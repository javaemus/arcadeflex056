/**
 *  ported to 0.56
 */
package mame056.sound;

import static common.libc.expressions.*;
import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import static common.ptr.*;

import static arcadeflex056.debug.mixerLog;

import static mame056.driverH.*;
import static mame056.sound.mixerH.*;
import static mame056.mame.*;
import static mame056.sndintrf.*;
import static mame056.sound.filter.*;
import static mame056.sound.filterH.*;
//to refactor
import static arcadeflex036.libc_old.*;
import static arcadeflex036.osdepend.logerror;
import static arcadeflex036.sound.*;

public class mixer {

    /*TODO*////***************************************************************************
/*TODO*///
/*TODO*///  mixer.c
/*TODO*///
/*TODO*///  Manage audio channels allocation, with volume and panning control
/*TODO*///
/*TODO*///***************************************************************************/
/*TODO*///
/*TODO*///#include "driver.h"
/*TODO*///#include "filter.h"
/*TODO*///
/*TODO*///#include <math.h>
/*TODO*///#include <limits.h>
/*TODO*///#include <assert.h>
/*TODO*///
/*TODO*////***************************************************************************/
/*TODO*////* Options */
/*TODO*///
/*TODO*////* Define it to enable the check of the flag options.use_filter as condition for the filter use */
/*TODO*///#define MIXER_USE_OPTION_FILTER
/*TODO*///
/*TODO*////* Undefine it to turn off clipping (helpful to find cases where we max out */
/*TODO*///#define MIXER_USE_CLIPPING
/*TODO*///
/*TODO*////* Define it to enable the logerror output */
/*TODO*////* #define MIXER_USE_LOGERROR */
/*TODO*///
/*TODO*////***************************************************************************/
    /* Config */
    static int mx_opened;

    public static void mixerlogerror(String string, Object... arguments) {
        if (mixerLog) {
            FILE f;

            f = fopen("mixer.log", (mx_opened++) != 0 ? "a" : "w");
            if (f != null) {
                fprintf(f, string, arguments);
                fclose(f);
            }
        }
    }

    /* accumulators have ACCUMULATOR_SAMPLES samples (must be a power of 2) */
    public static final int ACCUMULATOR_SAMPLES = 8192;
    public static final int ACCUMULATOR_MASK = (ACCUMULATOR_SAMPLES - 1);

    /* fractional numbers have FRACTION_BITS bits of resolution */
    public static final int FRACTION_BITS = 16;
    public static final int FRACTION_MASK = ((1 << FRACTION_BITS) - 1);

    /**
     * ************************************************************************
     */
    /* Static data */
    static int mixer_sound_enabled;

    /* holds all the data for the a mixer channel */
    public static class mixer_channel_data {

        String name;
        /* current volume, gain and pan */
        int left_volume;
        int right_volume;
        int gain;
        int pan;

        /* mixing levels */
        int/*unsigned*/ mixing_level;
        int/*unsigned*/ default_mixing_level;
        int/*unsigned*/ config_mixing_level;
        int/*unsigned*/ config_default_mixing_level;

        /* current playback positions */
        int/*unsigned*/ samples_available;

        /* resample state */
        int frac;
        /* resample fixed point state (used if filter is not active) */
        int pivot;
        /* resample brehesnam state (used if filter is active) */
        int step;
        /* fixed point increment */
        int/*unsigned*/ from_frequency;/* current source frequency */
        int/*unsigned*/ to_frequency;/* current destination frequency */
        int/*unsigned*/ lowpass_frequency;/* current lowpass arbitrary cut frequency, 0 if default */
        _filter filter;/* filter used, ==0 if none */
        filter_state left;/* state of the filter for the left/mono channel */
        filter_state right;/* state of the filter for the right channel */
        int is_reset_requested;
        /* state reset requested */

 /* lowpass filter request */
        int/*unsigned*/ request_lowpass_frequency;/* request for the lowpass arbitrary cut frequency, 0 if default */

 /* state of non-streamed playback */
        int is_stream;
        int is_playing;
        int is_looping;
        int is_16bit;

        BytePtr data_start_b;
        ShortPtr data_start_s;//void *		data_start;
        int data_end;//void *		data_end;
        int data_current;//void *		data_current;

    }

    /* channel data */
    static mixer_channel_data[] mixer_channel = new mixer_channel_data[MIXER_MAX_CHANNELS];
    static int[] /*unsigned*/ config_mixing_level = new int[MIXER_MAX_CHANNELS];
    static int[] /*unsigned*/ config_default_mixing_level = new int[MIXER_MAX_CHANNELS];

    static int first_free_channel = 0;
    static int is_config_invalid;
    static int is_stereo;

    /* 32-bit accumulators */
    static int/*unsigned*/ accum_base;

    static int[] left_accum = new int[ACCUMULATOR_SAMPLES];
    static int[] right_accum = new int[ACCUMULATOR_SAMPLES];

    /* 16-bit mix buffers */
    static short[] mix_buffer = new short[ACCUMULATOR_SAMPLES * 2];/* *2 for stereo */

 /* global sample tracking */
    public static int/*unsigned*/ samples_this_frame;

    /*TODO*///
/*TODO*////***************************************************************************
/*TODO*///	mixer_channel_resample
/*TODO*///***************************************************************************/
/*TODO*///
/*TODO*////* Window size of the FIR filter in samples (must be odd) */
/*TODO*////* Greater values are more precise, lesser values are faster. */
    public static final int FILTER_WIDTH = 31;

    /*TODO*///
/*TODO*////* The number of samples that need to be played to flush the filter state */
/*TODO*////* For the FIR filters it's equal to the filter width */
/*TODO*///#define FILTER_FLUSH FILTER_WIDTH
/*TODO*///
/*TODO*////* Setup the resample information
/*TODO*///	from_frequency - input frequency
/*TODO*///	lowpass_frequency - lowpass frequency, use 0 to automatically compute it from the resample operation
/*TODO*///	restart - restart the resample state
/*TODO*///*/
    static void mixer_channel_resample_set(mixer_channel_data channel, int/*unsigned*/ from_frequency, int/*unsigned*/ lowpass_frequency, int restart) {
        int/*unsigned*/ to_frequency;
        to_frequency = Machine.sample_rate;

        mixerlogerror("Mixer:mixer_channel_resample_set(%s,%d,%d)\n", channel.name, from_frequency, lowpass_frequency, restart);

        if (restart != 0) {
            mixerlogerror(("\tpivot=0\n"));
            channel.pivot = 0;
            channel.frac = 0;
        }

        /* only if the filter change */
        if (from_frequency != channel.from_frequency
                || to_frequency != channel.to_frequency
                || lowpass_frequency != channel.lowpass_frequency) {
            /* delete the previous filter */
            if (channel.filter != null) {
                filter_free(channel.filter);
                channel.filter = null;
            }

            /* make a new filter */
            if (options.use_filter != 0) {
                if ((from_frequency != 0 && to_frequency != 0 && (from_frequency != to_frequency || lowpass_frequency != 0))) {
                    double cut;
                    int/*unsigned*/ cut_frequency;

                    if (from_frequency < to_frequency) {
                        /* upsampling */
                        cut_frequency = from_frequency / 2;
                        if (lowpass_frequency != 0 && cut_frequency > lowpass_frequency) {
                            cut_frequency = lowpass_frequency;
                        }
                        cut = (double) cut_frequency / to_frequency;
                    } else {
                        /* downsampling */
                        cut_frequency = to_frequency / 2;
                        if (lowpass_frequency != 0 && cut_frequency > lowpass_frequency) {
                            cut_frequency = lowpass_frequency;
                        }
                        cut = (double) cut_frequency / from_frequency;
                    }

                    channel.filter = filter_lp_fir_alloc(cut, FILTER_WIDTH);

                    mixerlogerror("\tfilter from %d Hz, to %d Hz, cut %f, cut %d Hz\n", from_frequency, to_frequency, cut, cut_frequency);
                }
            }
        }

        channel.lowpass_frequency = lowpass_frequency;
        channel.from_frequency = from_frequency;
        channel.to_frequency = to_frequency;
        channel.step = (int) ((double) from_frequency * (1 << FRACTION_BITS) / to_frequency);

        /* reset the filter state */
        if (channel.filter != null && channel.is_reset_requested != 0) {
            mixerlogerror(("\tstate clear\n"));
            channel.is_reset_requested = 0;
            filter_state_reset(channel.filter, channel.left);
            filter_state_reset(channel.filter, channel.right);
        }
    }

    /* Resample a channel
	channel - channel info
	state - filter state
	volume - volume (0-255)
	dst - destination vector
	dst_len - max number of destination samples
	src - source vector, (updated at the exit)
	src_len - max number of source samples
     */
    static int/*unsigned*/ mixer_channel_resample_16(mixer_channel_data channel, filter_state state, int volume, int[] dst, int/*unsigned*/ dst_len, ShortPtr psrc, int/*unsigned*/ src_len) {
        int /*unsigned*/ dst_base = (accum_base + channel.samples_available) & ACCUMULATOR_MASK;
        int /*unsigned*/ dst_pos = dst_base;

        ShortPtr src = new ShortPtr(psrc);//INT16* src = *psrc;
        int psrc_offset = src.offset;

        if (channel.filter == null) {
            if (channel.from_frequency == channel.to_frequency) {
                /* copy */
                int /*unsigned*/ len;

                if (src_len > dst_len) {
                    len = dst_len;
                } else {
                    len = src_len;
                }


                /* reference version */
                int src_end = (src.offset + len) * 2;
                while (src.offset != src_end) {
                    dst[dst_pos] += (src.read() * volume) >> 8;
                    dst_pos = (dst_pos + 1) & ACCUMULATOR_MASK;
                    src.inc();
                }
            } else {
                /* end address */
                int src_end = (src.offset + src_len) * 2;
                int/*unsigned*/ dst_pos_end = (dst_pos + dst_len) & ACCUMULATOR_MASK;

                int step = channel.step;
                int frac = channel.frac;
                src.inc(frac >> FRACTION_BITS);
                frac &= FRACTION_MASK;

                while (src.offset < src_end && dst_pos != dst_pos_end) {
                    dst[dst_pos] += (src.read() * volume) >> 8;
                    frac += step;
                    dst_pos = (dst_pos + 1) & ACCUMULATOR_MASK;
                    src.inc(frac >> FRACTION_BITS);
                    frac &= FRACTION_MASK;
                }

                /* adjust the end if it's too big */
                if (src.offset > src_end) {
                    frac += (src.offset - src_end) << FRACTION_BITS;
                    src.offset = src_end;
                }

                channel.frac = frac;
            }
        } else if (channel.from_frequency == 0) {
            dst_pos = (dst_pos + dst_len) & ACCUMULATOR_MASK;
        } else {
            int pivot = channel.pivot;

            /* end address */
            int src_end = (src.offset + src_len) * 2;
            int/*unsigned*/ dst_pos_end = (dst_pos + dst_len) & ACCUMULATOR_MASK;

            /* volume */
            int v = volume;

            if (channel.from_frequency < channel.to_frequency) {
                /* upsampling */
                while (src.offset != src_end && dst_pos != dst_pos_end) {
                    /* source */
                    filter_insert(channel.filter, state, (int) (src.read() * v / 256.0));
                    pivot += channel.from_frequency;
                    if (pivot > 0) {
                        pivot -= channel.to_frequency;
                        src.inc();
                    }
                    /* dest */
                    dst[dst_pos] += filter_compute(channel.filter, state);
                    dst_pos = (dst_pos + 1) & ACCUMULATOR_MASK;
                }
            } else {
                /* downsampling */
                while (src.offset != src_end && dst_pos != dst_pos_end) {
                    /* source */
                    filter_insert(channel.filter, state, (int) (src.read() * v / 256.0));
                    pivot -= channel.to_frequency;
                    src.inc();
                    /* dest */
                    if (pivot < 0) {
                        pivot += channel.from_frequency;
                        dst[dst_pos] += filter_compute(channel.filter, state);
                        dst_pos = (dst_pos + 1) & ACCUMULATOR_MASK;
                    }
                }
            }

            channel.pivot = pivot;
        }

        psrc.offset = psrc_offset;//*psrc = src;

        return (dst_pos - dst_base) & ACCUMULATOR_MASK;
    }

    /*TODO*///
/*TODO*///static unsigned mixer_channel_resample_8(struct mixer_channel_data *channel, filter_state* state, int volume, int* dst, unsigned dst_len, INT8** psrc, unsigned src_len)
/*TODO*///{
/*TODO*///	unsigned dst_base = (accum_base + channel->samples_available) & ACCUMULATOR_MASK;
/*TODO*///	unsigned dst_pos = dst_base;
/*TODO*///
/*TODO*///	INT8* src = *psrc;
/*TODO*///
/*TODO*///	assert( dst_len <= ACCUMULATOR_MASK );
/*TODO*///
/*TODO*///	if (!channel->filter)
/*TODO*///	{
/*TODO*///		if (channel->from_frequency == channel->to_frequency)
/*TODO*///		{
/*TODO*///			/* copy */
/*TODO*///			unsigned len;
/*TODO*///			INT8* src_end;
/*TODO*///			if (src_len > dst_len)
/*TODO*///				len = dst_len;
/*TODO*///			else
/*TODO*///				len = src_len;
/*TODO*///
/*TODO*///			src_end = src + len;
/*TODO*///			while (src != src_end)
/*TODO*///			{
/*TODO*///				dst[dst_pos] += *src * volume;
/*TODO*///				dst_pos = (dst_pos + 1) & ACCUMULATOR_MASK;
/*TODO*///				++src;
/*TODO*///			}
/*TODO*///		} else {
/*TODO*///			/* end address */
/*TODO*///			INT8* src_end = src + src_len;
/*TODO*///			unsigned dst_pos_end = (dst_pos + dst_len) & ACCUMULATOR_MASK;
/*TODO*///
/*TODO*///			int step = channel->step;
/*TODO*///			int frac = channel->frac;
/*TODO*///			src += frac >> FRACTION_BITS;
/*TODO*///			frac &= FRACTION_MASK;
/*TODO*///
/*TODO*///			while (src < src_end && dst_pos != dst_pos_end)
/*TODO*///			{
/*TODO*///				dst[dst_pos] += *src * volume;
/*TODO*///				dst_pos = (dst_pos + 1) & ACCUMULATOR_MASK;
/*TODO*///				frac += step;
/*TODO*///				src += frac >> FRACTION_BITS;
/*TODO*///				frac &= FRACTION_MASK;
/*TODO*///			}
/*TODO*///
/*TODO*///			/* adjust the end if it's too big */
/*TODO*///			if (src > src_end) {
/*TODO*///				frac += (src - src_end) << FRACTION_BITS;
/*TODO*///				src = src_end;
/*TODO*///			}
/*TODO*///
/*TODO*///			channel->frac = frac;
/*TODO*///		}
/*TODO*///	} else if (!channel->from_frequency) {
/*TODO*///		dst_pos = (dst_pos + dst_len) & ACCUMULATOR_MASK;
/*TODO*///	} else {
/*TODO*///		int pivot = channel->pivot;
/*TODO*///
/*TODO*///		/* end address */
/*TODO*///		INT8* src_end = src + src_len;
/*TODO*///		unsigned dst_pos_end = (dst_pos + dst_len) & ACCUMULATOR_MASK;
/*TODO*///
/*TODO*///		/* volume */
/*TODO*///		filter_real v = volume;
/*TODO*///
/*TODO*///		if (channel->from_frequency < channel->to_frequency)
/*TODO*///		{
/*TODO*///			/* upsampling */
/*TODO*///			while (src != src_end && dst_pos != dst_pos_end)
/*TODO*///			{
/*TODO*///				/* source */
/*TODO*///				filter_insert(channel->filter,state,*src * v);
/*TODO*///				pivot += channel->from_frequency;
/*TODO*///				if (pivot > 0)
/*TODO*///				{
/*TODO*///					pivot -= channel->to_frequency;
/*TODO*///					++src;
/*TODO*///				}
/*TODO*///				/* dest */
/*TODO*///				dst[dst_pos] += filter_compute(channel->filter,state);
/*TODO*///				dst_pos = (dst_pos + 1) & ACCUMULATOR_MASK;
/*TODO*///			}
/*TODO*///		} else {
/*TODO*///			/* downsampling */
/*TODO*///			while (src != src_end && dst_pos != dst_pos_end)
/*TODO*///			{
/*TODO*///				/* source */
/*TODO*///				filter_insert(channel->filter,state,*src * v);
/*TODO*///				pivot -= channel->to_frequency;
/*TODO*///				++src;
/*TODO*///				/* dest */
/*TODO*///				if (pivot < 0)
/*TODO*///				{
/*TODO*///					pivot += channel->from_frequency;
/*TODO*///					dst[dst_pos] += filter_compute(channel->filter,state);
/*TODO*///					dst_pos = (dst_pos + 1) & ACCUMULATOR_MASK;
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		channel->pivot = pivot;
/*TODO*///	}
/*TODO*///
/*TODO*///	*psrc = src;
/*TODO*///
/*TODO*///	return (dst_pos - dst_base) & ACCUMULATOR_MASK;
/*TODO*///}
/*TODO*///
/*TODO*////* Mix a 8 bit channel */
/*TODO*///static unsigned mixer_channel_resample_8_pan(struct mixer_channel_data *channel, int* volume, unsigned dst_len, INT8** src, unsigned src_len)
/*TODO*///{
/*TODO*///	unsigned count;
/*TODO*///
/*TODO*///	if (!is_stereo || channel->pan == MIXER_PAN_LEFT) {
/*TODO*///		count = mixer_channel_resample_8(channel, channel->left, volume[0], left_accum, dst_len, src, src_len);
/*TODO*///	} else if (channel->pan == MIXER_PAN_RIGHT) {
/*TODO*///		count = mixer_channel_resample_8(channel, channel->right, volume[1], right_accum, dst_len, src, src_len);
/*TODO*///	} else {
/*TODO*///		/* save */
/*TODO*///		unsigned save_pivot = channel->pivot;
/*TODO*///		unsigned save_frac = channel->frac;
/*TODO*///		INT8* save_src = *src;
/*TODO*///		count = mixer_channel_resample_8(channel, channel->left, volume[0], left_accum, dst_len, src, src_len);
/*TODO*///		/* restore */
/*TODO*///		channel->pivot = save_pivot;
/*TODO*///		channel->frac = save_frac;
/*TODO*///		*src = save_src;
/*TODO*///		mixer_channel_resample_8(channel, channel->right, volume[1], right_accum, dst_len, src, src_len);
/*TODO*///	}
/*TODO*///
/*TODO*///	channel->samples_available += count;
/*TODO*///	return count;
/*TODO*///}

    /* Mix a 16 bit channel */
    static int/*unsigned*/ mixer_channel_resample_16_pan(mixer_channel_data channel, int[] volume, int/*unsigned*/ dst_len, ShortPtr src, int/*unsigned*/ src_len) {
        int /*unsigned*/ count;

        if (is_stereo == 0 || channel.pan == MIXER_PAN_LEFT) {
            count = mixer_channel_resample_16(channel, channel.left, volume[0], left_accum, dst_len, src, src_len);
        } else if (channel.pan == MIXER_PAN_RIGHT) {
            count = mixer_channel_resample_16(channel, channel.right, volume[1], right_accum, dst_len, src, src_len);
        } else {
            throw new UnsupportedOperationException("Unsupported");
            /*TODO*///		/* save */
/*TODO*///		unsigned save_pivot = channel->pivot;
/*TODO*///		unsigned save_frac = channel->frac;
/*TODO*///		INT16* save_src = *src;
/*TODO*///		count = mixer_channel_resample_16(channel, channel->left, volume[0], left_accum, dst_len, src, src_len);
/*TODO*///		/* restore */
/*TODO*///		channel->pivot = save_pivot;
/*TODO*///		channel->frac = save_frac;
/*TODO*///		*src = save_src;
/*TODO*///		mixer_channel_resample_16(channel, channel->right, volume[1], right_accum, dst_len, src, src_len);
        }

        channel.samples_available += count;
        return count;
    }

    /**
     * *************************************************************************
     * mix_sample_8
     * *************************************************************************
     */
    public static void mix_sample_8(mixer_channel_data channel, int samples_to_generate) {
        throw new UnsupportedOperationException("unsupported");
        /*TODO*///	INT8 *source, *source_end;
/*TODO*///	int mixing_volume[2];
/*TODO*///
/*TODO*///	/* compute the overall mixing volume */
/*TODO*///	if (mixer_sound_enabled)
/*TODO*///	{
/*TODO*///		mixing_volume[0] = ((channel->left_volume * channel->mixing_level * 256) << channel->gain) / (100*100);
/*TODO*///		mixing_volume[1] = ((channel->right_volume * channel->mixing_level * 256) << channel->gain) / (100*100);
/*TODO*///	} else {
/*TODO*///		mixing_volume[0] = 0;
/*TODO*///		mixing_volume[1] = 0;
/*TODO*///	}
/*TODO*///	/* get the initial state */
/*TODO*///	source = channel->data_current;
/*TODO*///	source_end = channel->data_end;
/*TODO*///
/*TODO*///	/* an outer loop to handle looping samples */
/*TODO*///	while (samples_to_generate > 0)
/*TODO*///	{
/*TODO*///		samples_to_generate -= mixer_channel_resample_8_pan(channel,mixing_volume,samples_to_generate,&source,source_end - source);
/*TODO*///
/*TODO*///		assert( source <= source_end );
/*TODO*///
/*TODO*///		/* handle the end case */
/*TODO*///		if (source >= source_end)
/*TODO*///		{
/*TODO*///			/* if we're done, stop playing */
/*TODO*///			if (!channel->is_looping)
/*TODO*///			{
/*TODO*///				channel->is_playing = 0;
/*TODO*///				break;
/*TODO*///			}
/*TODO*///
/*TODO*///			/* if we're looping, wrap to the beginning */
/*TODO*///			else
/*TODO*///				source -= (INT8 *)source_end - (INT8 *)channel->data_start;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* update the final positions */
/*TODO*///	channel->data_current = source;
    }

    /*TODO*///
/*TODO*////***************************************************************************
/*TODO*///	mix_sample_16
/*TODO*///***************************************************************************/
/*TODO*///
    public static void mix_sample_16(mixer_channel_data channel, int samples_to_generate) {
        throw new UnsupportedOperationException("unsupported");
        /*TODO*///	INT16 *source, *source_end;
/*TODO*///	int mixing_volume[2];
/*TODO*///
/*TODO*///	/* compute the overall mixing volume */
/*TODO*///	if (mixer_sound_enabled)
/*TODO*///	{
/*TODO*///		mixing_volume[0] = ((channel->left_volume * channel->mixing_level * 256) << channel->gain) / (100*100);
/*TODO*///		mixing_volume[1] = ((channel->right_volume * channel->mixing_level * 256) << channel->gain) / (100*100);
/*TODO*///	} else {
/*TODO*///		mixing_volume[0] = 0;
/*TODO*///		mixing_volume[1] = 0;
/*TODO*///	}
/*TODO*///	/* get the initial state */
/*TODO*///	source = channel->data_current;
/*TODO*///	source_end = channel->data_end;
/*TODO*///
/*TODO*///	/* an outer loop to handle looping samples */
/*TODO*///	while (samples_to_generate > 0)
/*TODO*///	{
/*TODO*///		samples_to_generate -= mixer_channel_resample_16_pan(channel,mixing_volume,samples_to_generate,&source,source_end - source);
/*TODO*///
/*TODO*///		assert( source <= source_end );
/*TODO*///
/*TODO*///		/* handle the end case */
/*TODO*///		if (source >= source_end)
/*TODO*///		{
/*TODO*///			/* if we're done, stop playing */
/*TODO*///			if (!channel->is_looping)
/*TODO*///			{
/*TODO*///				channel->is_playing = 0;
/*TODO*///				break;
/*TODO*///			}
/*TODO*///
/*TODO*///			/* if we're looping, wrap to the beginning */
/*TODO*///			else
/*TODO*///				source -= (INT16 *)source_end - (INT16 *)channel->data_start;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* update the final positions */
/*TODO*///	channel->data_current = source;
    }

    /*TODO*///
/*TODO*////***************************************************************************
/*TODO*///	mixer_flush
/*TODO*///***************************************************************************/
/*TODO*///
/*TODO*////* Silence samples */
/*TODO*///static unsigned char silence_data[FILTER_FLUSH];
/*TODO*///
/*TODO*////* Flush the state of the filter playing some 0 samples */
    static void mixer_flush(mixer_channel_data channel) {
        throw new UnsupportedOperationException("Unsupported");
        /*TODO*///	INT8 *source_begin, *source_end;
/*TODO*///	int mixing_volume[2];
/*TODO*///	unsigned save_available;
/*TODO*///
/*TODO*///	mixerlogerror(("Mixer:mixer_flush(%s)\n",channel->name));
/*TODO*///
/*TODO*///	/* filter reset request */
/*TODO*///	channel->is_reset_requested = 1;
/*TODO*///
/*TODO*///	/* null volume */
/*TODO*///	mixing_volume[0] = 0;
/*TODO*///	mixing_volume[1] = 0;
/*TODO*///
/*TODO*///	/* null data */
/*TODO*///	source_begin = (INT8*)silence_data;
/*TODO*///	source_end = (INT8*)silence_data + FILTER_FLUSH;
/*TODO*///
/*TODO*///	/* save the number of samples availables */
/*TODO*///	save_available = channel->samples_available;
/*TODO*///
/*TODO*///	/* mix the silence */
/*TODO*///	mixer_channel_resample_8_pan(channel,mixing_volume,ACCUMULATOR_MASK,&source_begin,source_end - source_begin);
/*TODO*///
/*TODO*///	/* restore the number of samples availables */
/*TODO*///	channel->samples_available = save_available;
    }

    /**
     * *************************************************************************
     * mixer_sh_start
     * *************************************************************************
     */
    public static int mixer_sh_start() {

        /* reset all channels to their defaults */
        for (int i = 0; i < mixer_channel.length; i++) {
            mixer_channel[i] = new mixer_channel_data();
            mixer_channel[i].mixing_level = 0xff;
            mixer_channel[i].default_mixing_level = 0xff;
            mixer_channel[i].config_mixing_level = config_mixing_level[i];
            mixer_channel[i].config_default_mixing_level = config_default_mixing_level[i];
            mixer_channel[i].left = filter_state_alloc();
            mixer_channel[i].right = filter_state_alloc();
        }

        /* determine if we're playing in stereo or not */
        first_free_channel = 0;
        is_stereo = ((Machine.drv.sound_attributes & SOUND_SUPPORTS_STEREO) != 0) ? 1 : 0;

        /* clear the accumulators */
        accum_base = 0;
        memset(left_accum, 0, sizeof(left_accum));
        memset(right_accum, 0, sizeof(right_accum));

        samples_this_frame = osd_start_audio_stream(is_stereo);

        mixer_sound_enabled = 1;

        return 0;
    }

    /**
     * *************************************************************************
     * mixer_sh_stop
     * *************************************************************************
     */
    public static void mixer_sh_stop() {
        osd_stop_audio_stream();
        /*TODO*///
/*TODO*///	for (i = 0, channel = mixer_channel; i < MIXER_MAX_CHANNELS; i++, channel++)
/*TODO*///	{
/*TODO*///		if (channel->filter)
/*TODO*///			filter_free(channel->filter);
/*TODO*///		filter_state_free(channel->left);
/*TODO*///		filter_state_free(channel->right);
/*TODO*///	}
    }

    /**
     * *************************************************************************
     * mixer_update_channel
     * *************************************************************************
     */
    public static void mixer_update_channel(mixer_channel_data channel, int total_sample_count) {
        int samples_to_generate = (int) (total_sample_count - channel.samples_available);

        /* don't do anything for streaming channels */
        if (channel.is_stream != 0) {
            return;
        }

        /* if we're all caught up, just return */
        if (samples_to_generate <= 0) {
            return;
        }

        /* if we're playing, mix in the data */
        if (channel.is_playing != 0) {
            if (channel.is_16bit != 0) {
                mix_sample_16(channel, samples_to_generate);
            } else {
                mix_sample_8(channel, samples_to_generate);
            }

            if (channel.is_playing == 0) {
                mixer_flush(channel);
            }
        }
    }

    /**
     * *************************************************************************
     * mixer_sh_update
     * *************************************************************************
     */
    public static void mixer_sh_update() {
        int/*UINT32*/ accum_pos = accum_base;

        int sample;
        /* update all channels (for streams this is a no-op) */
        for (int i = 0; i < first_free_channel; i++) {
            mixer_update_channel(mixer_channel[i], (int) samples_this_frame);

            /* if we needed more than they could give, adjust their pointers */
            if (samples_this_frame > mixer_channel[i].samples_available) {
                mixer_channel[i].samples_available = 0;
            } else {
                mixer_channel[i].samples_available -= samples_this_frame;
            }
        }

        /* copy the mono 32-bit data to a 16-bit buffer, clipping along the way */
        if (is_stereo == 0) {
            int mix = 0;
            for (int i = 0; i < samples_this_frame; i++) {
                /* fetch and clip the sample */
                sample = left_accum[accum_pos];
                if (sample < -32768) {
                    sample = -32768;
                } else if (sample > 32767) {
                    sample = 32767;
                }
                /* store and zero out behind us */
                mix_buffer[mix++] = (short) sample;
                left_accum[accum_pos] = 0;

                /* advance to the next sample */
                accum_pos = (accum_pos + 1) & ACCUMULATOR_MASK;
            }
        } /* copy the stereo 32-bit data to a 16-bit buffer, clipping along the way */ else {
            throw new UnsupportedOperationException("Unsupported");
            /*TODO*///		mix = mix_buffer;
/*TODO*///		for (i = 0; i < samples_this_frame; i++)
/*TODO*///		{
/*TODO*///			/* fetch and clip the left sample */
/*TODO*///			sample = left_accum[accum_pos];
/*TODO*///#ifdef MIXER_USE_CLIPPING
/*TODO*///			if (sample < -32768)
/*TODO*///				sample = -32768;
/*TODO*///			else if (sample > 32767)
/*TODO*///				sample = 32767;
/*TODO*///#endif
/*TODO*///
/*TODO*///			/* store and zero out behind us */
/*TODO*///			*mix++ = sample;
/*TODO*///			left_accum[accum_pos] = 0;
/*TODO*///
/*TODO*///			/* fetch and clip the right sample */
/*TODO*///			sample = right_accum[accum_pos];
/*TODO*///#ifdef MIXER_USE_CLIPPING
/*TODO*///			if (sample < -32768)
/*TODO*///				sample = -32768;
/*TODO*///			else if (sample > 32767)
/*TODO*///				sample = 32767;
/*TODO*///#endif
/*TODO*///
/*TODO*///			/* store and zero out behind us */
/*TODO*///			*mix++ = sample;
/*TODO*///			right_accum[accum_pos] = 0;
/*TODO*///
/*TODO*///			/* advance to the next sample */
/*TODO*///			accum_pos = (accum_pos + 1) & ACCUMULATOR_MASK;
/*TODO*///		}
        }
        /* play the result */
        samples_this_frame = osd_update_audio_stream(mix_buffer);

        accum_base = accum_pos;
    }

    /**
     * *************************************************************************
     * mixer_allocate_channel
     * *************************************************************************
     */
    public static int mixer_allocate_channel(int default_mixing_level) {
        /* this is just a degenerate case of the multi-channel mixer allocate */
        return mixer_allocate_channels(1, new int[]{default_mixing_level});
    }

    /**
     * *************************************************************************
     * mixer_allocate_channels
     * *************************************************************************
     */
    public static int mixer_allocate_channels(int channels, int[] default_mixing_levels) {
        mixerlogerror("Mixer:mixer_allocate_channels(%d)\n", channels);

        /* make sure we didn't overrun the number of available channels */
        if (first_free_channel + channels > MIXER_MAX_CHANNELS) {
            logerror("Too many mixer channels (requested %d, available %d)\n", first_free_channel + channels, MIXER_MAX_CHANNELS);
            throw new UnsupportedOperationException("ERROR");
        }
        /* loop over channels requested */
        for (int i = 0; i < channels; i++) {
            /* extract the basic data */
            mixer_channel[first_free_channel + i].default_mixing_level = (char) MIXER_GET_LEVEL(default_mixing_levels[i]);
            mixer_channel[first_free_channel + i].pan = MIXER_GET_PAN(default_mixing_levels[i]);
            mixer_channel[first_free_channel + i].gain = MIXER_GET_GAIN(default_mixing_levels[i]);
            mixer_channel[first_free_channel + i].left_volume = 100;
            mixer_channel[first_free_channel + i].right_volume = 100;

            /* backwards compatibility with old 0-255 volume range */
            if (mixer_channel[first_free_channel + i].default_mixing_level > 100) {
                mixer_channel[first_free_channel + i].default_mixing_level = (char) (mixer_channel[first_free_channel + i].default_mixing_level * 25 / 255);
            }

            /* attempt to load in the configuration data for this channel */
            mixer_channel[first_free_channel + i].mixing_level = mixer_channel[first_free_channel + i].default_mixing_level;
            if (is_config_invalid == 0) {
                /* if the defaults match, set the mixing level from the config */
                if (mixer_channel[first_free_channel + i].default_mixing_level == mixer_channel[first_free_channel + i].config_default_mixing_level) {
                    mixer_channel[first_free_channel + i].mixing_level = mixer_channel[first_free_channel + i].config_mixing_level;
                } /* otherwise, invalidate all channels that have been created so far */ else {
                    is_config_invalid = 1;
                    for (int j = 0; j < first_free_channel + i; j++) {
                        mixer_set_mixing_level(j, mixer_channel[j].default_mixing_level);
                    }
                }
            }
            /* set the default name */
            mixer_set_name(first_free_channel + i, null);
        }
        /* increment the counter and return the first one */
        first_free_channel += (char) channels;
        return first_free_channel - channels;

    }

    /**
     * *************************************************************************
     * mixer_set_name
     * *************************************************************************
     */
    public static void mixer_set_name(int ch, String name) {
        /* either copy the name or create a default one */
        if (name != null) {
            mixer_channel[ch].name = name;
        } else {
            mixer_channel[ch].name = sprintf("<channel #%d>", ch);
        }

        /* append left/right onto the channel as appropriate */
        if (mixer_channel[ch].pan == MIXER_PAN_LEFT) {
            mixer_channel[ch].name += " (Lt)";
        } else if (mixer_channel[ch].pan == MIXER_PAN_RIGHT) {
            mixer_channel[ch].name += " (Rt)";
        }
    }

    /*TODO*///
/*TODO*///
/*TODO*////***************************************************************************
/*TODO*///	mixer_get_name
/*TODO*///***************************************************************************/
/*TODO*///
/*TODO*///const char *mixer_get_name(int ch)
/*TODO*///{
/*TODO*///	struct mixer_channel_data *channel = &mixer_channel[ch];
/*TODO*///
/*TODO*///	/* return a pointer to the name or a NULL for an unused channel */
/*TODO*///	if (channel->name[0] != 0)
/*TODO*///		return channel->name;
/*TODO*///	else
/*TODO*///		return NULL;
/*TODO*///}
    /**
     * *************************************************************************
     * mixer_set_volume
     * *************************************************************************
     */
    public static void mixer_set_volume(int ch, int volume) {
        mixer_update_channel(mixer_channel[ch], sound_scalebufferpos((int) samples_this_frame));
        mixer_channel[ch].left_volume = volume;
        mixer_channel[ch].right_volume = volume;
    }

    /**
     * *************************************************************************
     * mixer_set_mixing_level
     * *************************************************************************
     */
    public static void mixer_set_mixing_level(int ch, int level) {
        mixer_update_channel(mixer_channel[ch], sound_scalebufferpos((int) samples_this_frame));
        mixer_channel[ch].mixing_level = (char) level;
    }

    /*TODO*////***************************************************************************
/*TODO*///	mixer_set_stereo_volume
/*TODO*///***************************************************************************/
/*TODO*///void mixer_set_stereo_volume(int ch, int l_vol, int r_vol )
/*TODO*///{
/*TODO*///	struct mixer_channel_data *channel = &mixer_channel[ch];
/*TODO*///
/*TODO*///	mixer_update_channel(channel, sound_scalebufferpos(samples_this_frame));
/*TODO*///	channel->left_volume  = l_vol;
/*TODO*///	channel->right_volume = r_vol;
/*TODO*///}
/*TODO*///
/*TODO*////***************************************************************************
/*TODO*///	mixer_get_mixing_level
/*TODO*///***************************************************************************/
/*TODO*///
/*TODO*///int mixer_get_mixing_level(int ch)
/*TODO*///{
/*TODO*///	struct mixer_channel_data *channel = &mixer_channel[ch];
/*TODO*///	return channel->mixing_level;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////***************************************************************************
/*TODO*///	mixer_get_default_mixing_level
/*TODO*///***************************************************************************/
/*TODO*///
/*TODO*///int mixer_get_default_mixing_level(int ch)
/*TODO*///{
/*TODO*///	struct mixer_channel_data *channel = &mixer_channel[ch];
/*TODO*///	return channel->default_mixing_level;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////***************************************************************************
/*TODO*///	mixer_read_config
/*TODO*///***************************************************************************/
/*TODO*///
/*TODO*///void mixer_read_config(void *f)
/*TODO*///{
/*TODO*///	UINT8 default_levels[MIXER_MAX_CHANNELS];
/*TODO*///	UINT8 mixing_levels[MIXER_MAX_CHANNELS];
/*TODO*///	int i;
/*TODO*///
/*TODO*///	memset(default_levels, 0xff, sizeof(default_levels));
/*TODO*///	memset(mixing_levels, 0xff, sizeof(mixing_levels));
/*TODO*///	osd_fread(f, default_levels, MIXER_MAX_CHANNELS);
/*TODO*///	osd_fread(f, mixing_levels, MIXER_MAX_CHANNELS);
/*TODO*///	for (i = 0; i < MIXER_MAX_CHANNELS; i++)
/*TODO*///	{
/*TODO*///		config_default_mixing_level[i] = default_levels[i];
/*TODO*///		config_mixing_level[i] = mixing_levels[i];
/*TODO*///	}
/*TODO*///	is_config_invalid = 0;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////***************************************************************************
/*TODO*///	mixer_write_config
/*TODO*///***************************************************************************/
/*TODO*///
/*TODO*///void mixer_write_config(void *f)
/*TODO*///{
/*TODO*///	UINT8 default_levels[MIXER_MAX_CHANNELS];
/*TODO*///	UINT8 mixing_levels[MIXER_MAX_CHANNELS];
/*TODO*///	int i;
/*TODO*///
/*TODO*///	for (i = 0; i < MIXER_MAX_CHANNELS; i++)
/*TODO*///	{
/*TODO*///		default_levels[i] = mixer_channel[i].default_mixing_level;
/*TODO*///		mixing_levels[i] = mixer_channel[i].mixing_level;
/*TODO*///	}
/*TODO*///	osd_fwrite(f, default_levels, MIXER_MAX_CHANNELS);
/*TODO*///	osd_fwrite(f, mixing_levels, MIXER_MAX_CHANNELS);
/*TODO*///}
/*TODO*///
    /**
     * *************************************************************************
     * mixer_play_streamed_sample_16
     * *************************************************************************
     */
    public static void mixer_play_streamed_sample_16(int ch, ShortPtr data, int len, int freq) {
        int[] mixing_volume = new int[2];

        mixerlogerror("Mixer:mixer_play_streamed_sample_16(%s,,%d,%d)\n", mixer_channel[ch].name, len / 2, freq);

        /* skip if sound is off */
        if (Machine.sample_rate == 0) {
            return;
        }
        mixer_channel[ch].is_stream = 1;
        /* compute the overall mixing volume */
        if (mixer_sound_enabled != 0) {
            mixing_volume[0] = ((mixer_channel[ch].left_volume * mixer_channel[ch].mixing_level * 256) << mixer_channel[ch].gain) / (100 * 100);
            mixing_volume[1] = ((mixer_channel[ch].right_volume * mixer_channel[ch].mixing_level * 256) << mixer_channel[ch].gain) / (100 * 100);
        } else {
            mixing_volume[0] = 0;
            mixing_volume[1] = 0;
        }

        mixer_channel_resample_set(mixer_channel[ch], freq, mixer_channel[ch].request_lowpass_frequency, 0);

        /* compute the length in fractional form */
        len = len / 2;
        /* convert len from byte to word */

        mixer_channel_resample_16_pan(mixer_channel[ch], mixing_volume, ACCUMULATOR_MASK, data, len);
    }

    /*TODO*///
/*TODO*///
/*TODO*////***************************************************************************
/*TODO*///	mixer_samples_this_frame
/*TODO*///***************************************************************************/
/*TODO*///
/*TODO*///int mixer_samples_this_frame(void)
/*TODO*///{
/*TODO*///	return samples_this_frame;
/*TODO*///}
/*TODO*///
    /**
     * *************************************************************************
     * mixer_need_samples_this_frame
     * *************************************************************************
     */
    public static final int EXTRA_SAMPLES = 1;// safety margin for sampling rate conversion

    public static int mixer_need_samples_this_frame(int channel, int freq) {
        return (samples_this_frame - mixer_channel[channel].samples_available)
                * freq / Machine.sample_rate + EXTRA_SAMPLES;
    }

    /*TODO*///
/*TODO*///
/*TODO*////***************************************************************************
/*TODO*///	mixer_play_sample
/*TODO*///***************************************************************************/
/*TODO*///
    public static void mixer_play_sample(int ch, BytePtr data, int len, int freq, int loop) {
        throw new UnsupportedOperationException("Unsupported");
        /*TODO*///void mixer_play_sample(int ch, INT8 *data, int len, int freq, int loop)
/*TODO*///{
/*TODO*///	struct mixer_channel_data *channel = &mixer_channel[ch];
/*TODO*///
/*TODO*///	mixerlogerror(("Mixer:mixer_play_sample_8(%s,,%d,%d,%s)\n",channel->name,len,freq,loop ? "loop" : "single"));
/*TODO*///
/*TODO*///	/* skip if sound is off, or if this channel is a stream */
/*TODO*///	if (Machine->sample_rate == 0 || channel->is_stream)
/*TODO*///		return;
/*TODO*///
/*TODO*///	/* update the state of this channel */
/*TODO*///	mixer_update_channel(channel, sound_scalebufferpos(samples_this_frame));
/*TODO*///
/*TODO*///	mixer_channel_resample_set(channel,freq,channel->request_lowpass_frequency,1);
/*TODO*///
/*TODO*///	/* now determine where to mix it */
/*TODO*///	channel->data_start = data;
/*TODO*///	channel->data_current = data;
/*TODO*///	channel->data_end = (UINT8 *)data + len;
/*TODO*///	channel->is_playing = 1;
/*TODO*///	channel->is_looping = loop;
/*TODO*///	channel->is_16bit = 0;
    }

    /*TODO*///
/*TODO*///
/*TODO*////***************************************************************************
/*TODO*///	mixer_play_sample_16
/*TODO*///***************************************************************************/
/*TODO*///
    public static void mixer_play_sample_16(int ch, ShortPtr data, int len, int freq, int loop) {
        throw new UnsupportedOperationException("Unsupported");
        /*TODO*///	struct mixer_channel_data *channel = &mixer_channel[ch];
/*TODO*///
/*TODO*///	mixerlogerror(("Mixer:mixer_play_sample_16(%s,,%d,%d,%s)\n",channel->name,len/2,freq,loop ? "loop" : "single"));
/*TODO*///
/*TODO*///	/* skip if sound is off, or if this channel is a stream */
/*TODO*///	if (Machine->sample_rate == 0 || channel->is_stream)
/*TODO*///		return;
/*TODO*///
/*TODO*///	/* update the state of this channel */
/*TODO*///	mixer_update_channel(channel, sound_scalebufferpos(samples_this_frame));
/*TODO*///
/*TODO*///	mixer_channel_resample_set(channel,freq,channel->request_lowpass_frequency,1);
/*TODO*///
/*TODO*///	/* now determine where to mix it */
/*TODO*///	channel->data_start = data;
/*TODO*///	channel->data_current = data;
/*TODO*///	channel->data_end = (UINT8 *)data + len;
/*TODO*///	channel->is_playing = 1;
/*TODO*///	channel->is_looping = loop;
/*TODO*///	channel->is_16bit = 1;
    }

    /*TODO*///
    /**
     * *************************************************************************
     * mixer_stop_sample
     * *************************************************************************
     */
    public static void mixer_stop_sample(int ch) {
        throw new UnsupportedOperationException("Unsupported");
        /*TODO*///	struct mixer_channel_data *channel = &mixer_channel[ch];
/*TODO*///
/*TODO*///	mixerlogerror(("Mixer:mixer_stop_sample(%s)\n",channel->name));
/*TODO*///
/*TODO*///	mixer_update_channel(channel, sound_scalebufferpos(samples_this_frame));
/*TODO*///
/*TODO*///	if (channel->is_playing) {
/*TODO*///		channel->is_playing = 0;
/*TODO*///		mixer_flush(channel);
/*TODO*///	}
    }

    /**
     * *************************************************************************
     * mixer_is_sample_playing
     * *************************************************************************
     */
    public static int mixer_is_sample_playing(int ch) {
        mixer_update_channel(mixer_channel[ch], sound_scalebufferpos((int) samples_this_frame));
        return mixer_channel[ch].is_playing;
    }

    /**
     * *************************************************************************
     * mixer_set_sample_frequency
     * *************************************************************************
     */
    public static void mixer_set_sample_frequency(int ch, int freq) {
        throw new UnsupportedOperationException("Unsupported");
        /*TODO*///	struct mixer_channel_data *channel = &mixer_channel[ch];
/*TODO*///
/*TODO*///	assert( !channel->is_stream );
/*TODO*///
/*TODO*///	if (channel->is_playing) {
/*TODO*///		mixerlogerror(("Mixer:mixer_set_sample_frequency(%s,%d)\n",channel->name,freq));
/*TODO*///
/*TODO*///		mixer_update_channel(channel, sound_scalebufferpos(samples_this_frame));
/*TODO*///
/*TODO*///		mixer_channel_resample_set(channel,freq,channel->request_lowpass_frequency,0);
/*TODO*///	}
    }

    /*TODO*///
/*TODO*////***************************************************************************
/*TODO*///	mixer_set_lowpass_frequency
/*TODO*///***************************************************************************/
/*TODO*///
/*TODO*////* Set the desidered lowpass cut frequency.
/*TODO*///This function should be called immeditially after the mixer_allocate() and
/*TODO*///before the first play() call. Otherwise the lowpass frequency may be
/*TODO*///unused until the next filter recompute.
/*TODO*///	ch - channel
/*TODO*///	freq - frequency in Hz. Use 0 to disable
/*TODO*///*/
/*TODO*///void mixer_set_lowpass_frequency(int ch, int freq)
/*TODO*///{
/*TODO*///	struct mixer_channel_data *channel = &mixer_channel[ch];
/*TODO*///
/*TODO*///	assert(!channel->is_playing && !channel->is_stream);
/*TODO*///
/*TODO*///	mixerlogerror(("Mixer:mixer_set_lowpass_frequency(%s,%d)\n",channel->name,freq));
/*TODO*///
/*TODO*///	channel->request_lowpass_frequency = freq;
/*TODO*///}
    /**
     * *************************************************************************
     * mixer_sound_enable_global_w
     * *************************************************************************
     */
    public static void mixer_sound_enable_global_w(int enable) {
        /*TODO*///	int i;
        /*TODO*///	struct mixer_channel_data *channel;
        /*TODO*///
        /*TODO*///	/* update all channels (for streams this is a no-op) */
        /*TODO*///	for (i = 0, channel = mixer_channel; i < first_free_channel; i++, channel++)
        /*TODO*///	{
        /*TODO*///		mixer_update_channel(channel, sound_scalebufferpos(samples_this_frame));
        /*TODO*///	}
        /*TODO*///
        /*TODO*///	mixer_sound_enabled = enable;
    }
}
