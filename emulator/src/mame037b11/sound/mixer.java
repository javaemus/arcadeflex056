/*
 * ported to v0.37b11
 * 
 */
package mame037b11.sound;

import static common.ptr.*;

import static arcadeflex036.osdepend.logerror;
import static mame056.mame.Machine;
import static mame056.sndintrf.sound_scalebufferpos;
import mame056.sound.filterC;
import mame056.sound.filterH.filter;
import mame056.sound.filterH.filter_state;
import static mame056.sound.mixerH.*;
import static mame056.sound.mixer.*;

public class mixer {

    /* enable this to turn off clipping (helpful to find cases where we max out */
    public static final boolean DISABLE_CLIPPING = false;

    
    public static class mixer_channel_data {

        public String name;

        /* current volume, gain and pan */
        public int left_volume;
        public int right_volume;
        public int gain;
        public int pan;

        /* mixing levels */
        public char /*UINT8*/ mixing_level;
        public char /*UINT8*/ default_mixing_level;
        public char /*UINT8*/ config_mixing_level;
        public char /*UINT8*/ config_default_mixing_level;

        /* current playback positions */
        public int/*UINT32*/ input_frac;
        public int/*UINT32*/ samples_available;
        public int /*UINT32*/ frequency;
        public int/*UINT32*/ step_size;

        /* state of non-streamed playback */
        public int/*UINT8*/ is_stream;
        public int/*UINT8*/ is_playing;
        public int/*UINT8*/ is_looping;
        public int/*UINT8*/ is_16bit;

        public BytePtr data_start_b;
        public ShortPtr data_start_s;//void *		data_start;
        public int data_end;//void *		data_end;
        public int data_current;//void *		data_current;
        
        
        	/* resample state */
	public int frac; /* resample fixed point state (used if filter is not active) */
	public int pivot; /* resample brehesnam state (used if filter is active) */
	public int step; /* fixed point increment */
	public int from_frequency; /* current source frequency */
	public int to_frequency; /* current destination frequency */
	public int lowpass_frequency; /* current lowpass arbitrary cut frequency, 0 if default */
	public filter _filter; /* filter used, ==0 if none */
	public filter_state left; /* state of the filter for the left/mono channel */
	public filter_state right; /* state of the filter for the right channel */
	public int is_reset_requested; /* state reset requested */

	/* lowpass filter request */
	public int request_lowpass_frequency; /* request for the lowpass arbitrary cut frequency, 0 if default */
    }

        public static mixer_channel_data[] mixer_channel = new mixer_channel_data[MIXER_MAX_CHANNELS];


    

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
        }

        /* just eat the rest */
        channel.samples_available += (int) samples_to_generate;
    }

    /**
     * *************************************************************************
     * mixer_play_streamed_sample_16
     * *************************************************************************
     */
    public static void mixer_play_streamed_sample_16(int ch, ShortPtr data, int len, int freq) {

        int/*UINT32*/ step_size, input_pos, output_pos, samples_mixed;
        int[] mixing_volume = new int[2];

        /* skip if sound_old is off */
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
        /* compute the step size for sample rate conversion */
        if (freq != mixer_channel[ch].frequency) {
            /*RECHECK*/
            mixer_channel[ch].frequency = /*uint32)*/ (int) freq;
            /*RECHECK*/
            mixer_channel[ch].step_size = (/*UINT32*/int) ((double) freq * (double) (1 << FRACTION_BITS) / (double) Machine.sample_rate);
        }

        step_size = mixer_channel[ch].step_size;

        /* now determine where to mix it */
        input_pos = mixer_channel[ch].input_frac;
        output_pos = (accum_base + mixer_channel[ch].samples_available) & ACCUMULATOR_MASK;

        /* compute the length in fractional form */
        len = (len / 2) << FRACTION_BITS;
        samples_mixed = 0;


        /* if we're mono or left panning, just mix to the left channel */
        if (is_stereo == 0 || mixer_channel[ch].pan == MIXER_PAN_LEFT) {
            while (input_pos < len) {
                left_accum[output_pos] += ((short) data.read((int) (input_pos >> FRACTION_BITS)) * mixing_volume[0]) >> 8;
                input_pos += step_size;
                output_pos = (output_pos + 1) & ACCUMULATOR_MASK;
                samples_mixed++;
            }
        } /* if we're right panning, just mix to the right channel */ else if (mixer_channel[ch].pan == MIXER_PAN_RIGHT) {
            while (input_pos < len) {
                right_accum[output_pos] += ((short) data.read((int) (input_pos >> FRACTION_BITS)) * mixing_volume[1]) >> 8;
                input_pos += step_size;
                output_pos = (output_pos + 1) & ACCUMULATOR_MASK;
                samples_mixed++;
            }
        } /* if we're stereo center, mix to both channels */ else {
            while (input_pos < len) {
                left_accum[output_pos] += (data.read(input_pos >> FRACTION_BITS) * mixing_volume[0]) >> 8;
                right_accum[output_pos] += (data.read(input_pos >> FRACTION_BITS) * mixing_volume[1]) >> 8;

                input_pos += step_size;
                output_pos = (output_pos + 1) & ACCUMULATOR_MASK;
                samples_mixed++;
            }
        }
        /* update the final positions */
        mixer_channel[ch].input_frac = input_pos & FRACTION_MASK;
        mixer_channel[ch].samples_available += samples_mixed;
    }




    /**
     * *************************************************************************
     * mixer_play_sample
     * *************************************************************************
     */
    public static void mixer_play_sample(int ch, BytePtr data, int len, int freq, int loop) {
        //struct mixer_channel_data *channel = &mixer_channel[ch];

        /* skip if sound_old is off, or if this channel is a stream */
        if (Machine.sample_rate == 0 || mixer_channel[ch].is_stream != 0) {
            return;
        }

        /* update the state of this channel */
        mixer_update_channel(mixer_channel[ch], sound_scalebufferpos((int) samples_this_frame));

        /* compute the step size for sample rate conversion */
        if (freq != mixer_channel[ch].frequency) {
            mixer_channel[ch].frequency = freq;
            mixer_channel[ch].step_size = (/*UINT32*/int) ((double) freq * (double) (1 << FRACTION_BITS) / (double) Machine.sample_rate);
        }

        /* now determine where to mix it */
        mixer_channel[ch].input_frac = 0;
        mixer_channel[ch].data_start_b = data;
        mixer_channel[ch].data_current = 0;
        mixer_channel[ch].data_end = /*(UINT8 *)data +*/ len;
        mixer_channel[ch].is_playing = 1;
        mixer_channel[ch].is_looping = loop;
        mixer_channel[ch].is_16bit = 0;
    }

    /**
     * *************************************************************************
     * mixer_play_sample_16
     * *************************************************************************
     */
    public static void mixer_play_sample_16(int ch, ShortPtr data, int len, int freq, int loop) {
        /* skip if sound_old is off, or if this channel is a stream */
        if (Machine.sample_rate == 0 || mixer_channel[ch].is_stream != 0) {
            return;
        }

        /* update the state of this channel */
        mixer_update_channel(mixer_channel[ch], sound_scalebufferpos((int) samples_this_frame));

        /* compute the step size for sample rate conversion */
        if (freq != mixer_channel[ch].frequency) {
            mixer_channel[ch].frequency = (int) freq;
            mixer_channel[ch].step_size = (int) ((double) freq * (double) (1 << FRACTION_BITS) / (double) Machine.sample_rate);
        }

        /* now determine where to mix it */
        mixer_channel[ch].input_frac = 0;
        mixer_channel[ch].data_start_s = data;
        mixer_channel[ch].data_current = 0;//data;
        mixer_channel[ch].data_end = /*(UINT8 *)data*/ len;
        mixer_channel[ch].is_playing = 1;
        mixer_channel[ch].is_looping = loop;
        mixer_channel[ch].is_16bit = 1;
    }

    /**
     * *************************************************************************
     * mixer_set_sample_frequency
     * *************************************************************************
     */
    public static void mixer_set_sample_frequency(int ch, int freq) {
        //struct mixer_channel_data *channel = &mixer_channel[ch];

        mixer_update_channel(mixer_channel[ch], sound_scalebufferpos(samples_this_frame));

        /* compute the step size for sample rate conversion */
        if (freq != mixer_channel[ch].frequency) {
            mixer_channel[ch].frequency = freq;
            mixer_channel[ch].step_size = (int) ((double) freq * (double) (1 << FRACTION_BITS) / (double) Machine.sample_rate);
        }
    }

    

    /**
     * *************************************************************************
     * mix_sample_8
     * *************************************************************************
     */
    public static void mix_sample_8(mixer_channel_data channel, int samples_to_generate) {
        int/*UINT32*/ step_size, input_frac, output_pos;
        BytePtr source;
        int source_end;
        int[] mixing_volume=new int[2];

        /* compute the overall mixing volume */
        if (mixer_sound_enabled!=0){
	  mixing_volume[0] = ((channel.left_volume * channel.mixing_level * 256) << channel.gain) / (100*100);
	  mixing_volume[1] = ((channel.right_volume * channel.mixing_level * 256) << channel.gain) / (100*100);
	}
	else{
		mixing_volume[0] = 0;
		mixing_volume[1] = 0;
	}


        /* get the initial state */
        step_size = channel.step_size;
        source = new BytePtr(channel.data_start_b, channel.data_current);//source = channel->data_current;
        source_end = channel.data_end;
        input_frac = channel.input_frac;
        output_pos = (accum_base + channel.samples_available) & ACCUMULATOR_MASK;

        /* an outer loop to handle looping samples */
        while (samples_to_generate > 0) {
            /* if we're mono or left panning, just mix to the left channel */
            if (is_stereo == 0 || channel.pan == MIXER_PAN_LEFT) {
                while (source.offset < source_end && samples_to_generate > 0) {
                    left_accum[output_pos] += source.read(0) * mixing_volume[0];
                    input_frac += step_size;
                    source.offset += (int) (input_frac >> FRACTION_BITS);
                    input_frac &= FRACTION_MASK;
                    output_pos = (output_pos + 1) & ACCUMULATOR_MASK;
                    samples_to_generate--;
                }
            } /* if we're right panning, just mix to the right channel */ else if (channel.pan == MIXER_PAN_RIGHT) {
                while (source.offset < source_end && samples_to_generate > 0) {
                    right_accum[output_pos] += source.read(0) * mixing_volume[1];
                    input_frac += step_size;
                    source.offset += (int) (input_frac >> FRACTION_BITS);
                    input_frac &= FRACTION_MASK;
                    output_pos = (output_pos + 1) & ACCUMULATOR_MASK;
                    samples_to_generate--;
                }
            } /* if we're stereo center, mix to both channels */ else {
                throw new UnsupportedOperationException("unsupported");
                /*TODO*///				while (source < source_end && samples_to_generate > 0)
/*TODO*///				{
/*TODO*///					left_accum[output_pos] +=  *source * mixing_volume[0];
/*TODO*///				right_accum[output_pos] +=  *source * mixing_volume[1];
/*TODO*///					input_frac += step_size;
/*TODO*///					source += input_frac >> FRACTION_BITS;
/*TODO*///					input_frac &= FRACTION_MASK;
/*TODO*///					output_pos = (output_pos + 1) & ACCUMULATOR_MASK;
/*TODO*///					samples_to_generate--;
/*TODO*///				}
            }
            /* handle the end case */
            if (source.offset >= source_end) {
                /* if we're done, stop playing */
                if (channel.is_looping == 0) {
                    channel.is_playing = 0;
                    break;
                } /* if we're looping, wrap to the beginning */ else {
                    source.offset -= source_end;// -(INT8*)channel.data_start;
                }
            }
        }
        /* update the final positions */
        channel.input_frac = input_frac;
        channel.data_current = source.offset;
    }

    /**
     * *************************************************************************
     * mix_sample_16
     * *************************************************************************
     */
    static void mix_sample_16(mixer_channel_data channel, int samples_to_generate) {
        int step_size, input_frac, output_pos;
        ShortPtr source;
        int source_end;
        int[] mixing_volume=new int[2];

        if (mixer_sound_enabled!=0){
	  mixing_volume[0] = ((channel.left_volume * channel.mixing_level * 256) << channel.gain) / (100*100);
	  mixing_volume[1] = ((channel.right_volume * channel.mixing_level * 256) << channel.gain) / (100*100);
	}
	else{
		mixing_volume[0] = 0;
		mixing_volume[1] = 0;
	}

        /* get the initial state */
        step_size = channel.step_size;
        source = new ShortPtr(channel.data_start_s, channel.data_current);
        source_end = channel.data_end;
        input_frac = channel.input_frac;
        output_pos = (accum_base + channel.samples_available) & ACCUMULATOR_MASK;

        /* an outer loop to handle looping samples */
        while (samples_to_generate > 0) {
            /* if we're mono or left panning, just mix to the left channel */
            if (is_stereo == 0 || channel.pan == MIXER_PAN_LEFT) {
                while (source.offset < source_end && samples_to_generate > 0) {
                    left_accum[output_pos] += (source.read() * mixing_volume[0]) >> 8;

                    input_frac += step_size;
                    source.inc((int) (input_frac >> FRACTION_BITS));
                    input_frac &= FRACTION_MASK;

                    output_pos = (output_pos + 1) & ACCUMULATOR_MASK;
                    samples_to_generate--;
                }
            } /* if we're right panning, just mix to the right channel */ else if (channel.pan == MIXER_PAN_RIGHT) {
                while (source.offset < source_end && samples_to_generate > 0) {
                    right_accum[output_pos] += (source.read() * mixing_volume[1]) >> 8;

                    input_frac += step_size;
                    source.inc((int) (input_frac >> FRACTION_BITS));
                    input_frac &= FRACTION_MASK;

                    output_pos = (output_pos + 1) & ACCUMULATOR_MASK;
                    samples_to_generate--;
                }
            } /* if we're stereo center, mix to both channels */ else {
                throw new UnsupportedOperationException("unsupported");
                /*TODO*///				while (source < source_end && samples_to_generate > 0)
/*TODO*///				{
/*TODO*///					left_accum[output_pos] += (*source * mixing_volume[0]) >> 8;
/*TODO*///				right_accum[output_pos] += (*source * mixing_volume[1]) >> 8;
/*TODO*///
/*TODO*///
/*TODO*///					input_frac += step_size;
/*TODO*///					source += input_frac >> FRACTION_BITS;
/*TODO*///					input_frac &= FRACTION_MASK;
/*TODO*///
/*TODO*///					output_pos = (output_pos + 1) & ACCUMULATOR_MASK;
/*TODO*///					samples_to_generate--;
/*TODO*///				}
            }

            /* handle the end case */
            if (source.offset >= source_end) {
                /* if we're done, stop playing */
                if (channel.is_looping == 0) {
                    channel.is_playing = 0;
                    break;
                } /* if we're looping, wrap to the beginning */ else {
                    source.offset -= source_end;// source.offset -= (INT16*)source_end - (INT16*)channel.data_start;
                }
            }
        }

        /* update the final positions */
        channel.input_frac = input_frac;
        channel.data_current = source.offset;
    }

}
