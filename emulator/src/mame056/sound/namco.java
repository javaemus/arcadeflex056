/**
 * ported to v0.56
 * ported to v0.37b7
 *
 */
package mame056.sound;

import static arcadeflex056.fucPtr.*;

import static common.libc.cstring.*;
import static common.ptr.*;

import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame056.common.*;
import static mame056.mame.*;
import static mame056.sound.streams.*;
import static mame056.sound.namcoH.*;

import static arcadeflex036.osdepend.*;
import static mame056.sound.mixerH.*;
import static mame056.sound.mixer.*;


public class namco extends snd_interface {

    public namco() {
        sound_num = SOUND_NAMCO;
        name = "Namco";
    }

    @Override
    public int chips_num(MachineSound msound) {
        return 0;
    }

    @Override
    public int chips_clock(MachineSound msound) {
        return 0;
    }
    /* 8 voices max */
    public static final int MAX_VOICES = 8;

    /* this structure defines the parameters for a channel */
    public static class sound_channel {

        public int frequency;
        public int counter;
        public int[] volume = new int[2];
        public int noise_sw;
        public int noise_state;
        public int noise_seed;
        public int noise_counter;
        public UBytePtr wave;
    }

    /* globals available to everyone */
    public static UBytePtr namco_soundregs = new UBytePtr();//unsigned char *namco_soundregs;
    public static UBytePtr namco_wavedata = new UBytePtr();//unsigned char *namco_wavedata;

    /* data about the sound system */
    static sound_channel[] channel_list = new sound_channel[MAX_VOICES];
    static int last_channel;

    /* global sound parameters */
    static UBytePtr sound_prom;
    static int samples_per_byte;
    static int num_voices;
    static int sound_enable;
    static int stream;
    static int namco_clock;
    static int sample_rate;

    /* mixer tables and internal buffers */
    static short[] mixer_lookup;
    static ShortPtr mixer_buffer;
    static ShortPtr mixer_buffer_2;

    /* build a table to divide by the number of voices */
    static int mixer_lookup_middle;

    static int make_mixer_table(int voices) {
        int count = voices * 128;
        int i;
        int gain = 16;

        /* find the middle of the table */
        mixer_lookup = new short[256 * voices];
        mixer_lookup_middle = voices * 128;//mixer_lookup = mixer_table + (128 * voices);
        /* fill in the table - 16 bit case */
        for (i = 0; i < count; i++) {
            short val = (short) (i * gain * 16 / voices);
            if (val > 32767) {
                val = 32767;
            }
            mixer_lookup[mixer_lookup_middle + i] = val;
            mixer_lookup[mixer_lookup_middle - i] = (short) -val;
        }

        return 0;
    }

    /* generate sound to the mix buffer in mono */
    public static StreamInitPtr namco_update_mono = new StreamInitPtr() {
        public void handler(int chip, ShortPtr buffer, int length) {
            ShortPtr mix;

            /* if no sound, we're done */
            if (sound_enable == 0) {
                memset(buffer, 0, length * 2);
                return;
            }

            /* zap the contents of the mixer buffer */
            memset(mixer_buffer, 0, length * 2);

            /* loop over each voice and add its contribution */
            for (int voice = 0; voice < last_channel; voice++)//; voice < last_channel; voice++)
            {
                int f = channel_list[voice].frequency;
                int v = channel_list[voice].volume[0];
                mix = new ShortPtr(mixer_buffer);

                if (channel_list[voice].noise_sw != 0) {
                    /* only update if we have non-zero volume and frequency */
                    if (v != 0 && (f & 0xff) != 0) {

                        float fbase = (float) sample_rate / (float) namco_clock;
                        int delta = (int) ((float) ((f & 0xff) << 4) * fbase);
                        int c = channel_list[voice].noise_counter;
                        /* add our contribution */
                        for (int i = 0; i < length; i++) {
                            int noise_data;
                            int cnt;

                            if (channel_list[voice].noise_state != 0) {
                                noise_data = 0x07;
                            } else {
                                noise_data = -0x07;
                            }
                            mix.writeinc((short) (mix.read(0) + noise_data * (v >> 1)));
                            c += delta;
                            cnt = (c >> 12);
                            c &= (1 << 12) - 1;
                            for (; cnt > 0; cnt--) {
                                if (((channel_list[voice].noise_seed + 1) & 2) != 0) {
                                    channel_list[voice].noise_state ^= 1;
                                }
                                if ((channel_list[voice].noise_seed & 1) != 0) {
                                    channel_list[voice].noise_seed ^= 0x28000;
                                }
                                channel_list[voice].noise_seed >>= 1;
                            }
                        }
                        /* update the counter for this voice */
                        channel_list[voice].noise_counter = c;
                    }
                } else {
                    /* only update if we have non-zero volume and frequency */
                    if (v != 0 && f != 0) {
                        int c = channel_list[voice].counter;
                        /* add our contribution */
                        for (int i = 0; i < length; i++) {
                            c += f;
                            int offs = (c >> 15) & 0x1f;
                            if (samples_per_byte == 1)/* use only low 4 bits */ {
                                mix.writeinc((short) (mix.read(0) + ((channel_list[voice].wave.read(offs) & 0x0f) - 8) * v));
                            } else /* use full byte, first the high 4 bits, then the low 4 bits */ {
                                if ((offs & 1) != 0) {
                                    mix.writeinc((short) (mix.read(0) + ((channel_list[voice].wave.read(offs >> 1) & 0x0f) - 8) * v));
                                } else {
                                    mix.writeinc((short) (mix.read(0) + (((channel_list[voice].wave.read(offs >> 1) >> 4) & 0x0f) - 8) * v));
                                }
                            }
                        }

                        /* update the counter for this voice */
                        channel_list[voice].counter = c;
                    }
                }

            }
            /* mix it down */
            mix = new ShortPtr(mixer_buffer);
            for (int i = 0; i < length; i++) {
                buffer.writeinc(mixer_lookup[mixer_lookup_middle + (short) mix.read(0)]);
                mix.inc();
            }
        }
    };

    
    
    /* generate sound to the mix buffer in stereo */
    public static StreamInitMultiPtr namco_update_stereo = new StreamInitMultiPtr() {
        public void handler(int ch, ShortPtr[] buffer, int length) {
            sound_channel voice;
            ShortPtr lmix, rmix;
    	int i;
        int _voice = 0;
    
    	/* if no sound, we're done */
    	if (sound_enable == 0)
    	{
    		memset(buffer[0], 0, length);
    		memset(buffer[1], 0, length);
    		return;
    	}
    
    	/* zap the contents of the mixer buffer */
    	memset(mixer_buffer, 0, length);
    	memset(mixer_buffer_2, 0, length);
    
    	/* loop over each voice and add its contribution */
    	for (voice = channel_list[_voice]; _voice < last_channel; _voice++)
    	{
    		int f = voice.frequency;
    		int lv = voice.volume[0];
    		int rv = voice.volume[1];
    
    		lmix = new ShortPtr(mixer_buffer);
    		rmix = new ShortPtr(mixer_buffer_2);
    
    		if (voice.noise_sw != 0)
    		{
    			/* only update if we have non-zero volume and frequency */
    			if ((lv!=0 || rv!=0) && (f & 0xff)!=0)
    			{
    				float fbase = (float)sample_rate / (float)namco_clock;
    				int delta = (int) (((f & 0xff) << 4) * fbase);
    				int c = voice.noise_counter;
    
    				/* add our contribution */
    				for (i = 0; i < length; i++)
    				{
    					int noise_data;
    					int cnt;
    
    					if (voice.noise_state!=0)	noise_data = 0x07;
    					else noise_data = -0x07;
    					lmix.writeinc((short) (lmix.read() + noise_data * (lv >> 1)));
    					rmix.writeinc((short) (rmix.read() + noise_data * (rv >> 1)));
    
    					c += delta;
    					cnt = (c >> 12);
    					c &= (1 << 12) - 1;
    					for( ;cnt > 0; cnt--)
    					{
    						if (((voice.noise_seed + 1) & 2)!=0) voice.noise_state ^= 1;
    						if ((voice.noise_seed & 1)!=0) voice.noise_seed ^= 0x28000;
    						voice.noise_seed >>= 1;
    					}
    				}
    
    				/* update the counter for this voice */
    				voice.noise_counter = c;
    			}
    		}
    		else
    		{
    			/* only update if we have non-zero volume and frequency */
    			if ((lv!=0 || rv!=0) && f!=0)
    			{
    				UBytePtr w = new UBytePtr(voice.wave);
    				int c = voice.counter;
    
    				/* add our contribution */
    				for (i = 0; i < length; i++)
    				{
    					int offs;
    
    					c += f;
    					offs = (c >> 15) & 0x1f;
    					if (samples_per_byte == 1)	/* use only low 4 bits */
    					{
    						lmix.writeinc((short) (lmix.read() + ((w.read(offs) & 0x0f) - 8) * lv));
    						rmix.writeinc((short) (rmix.read() + ((w.read(offs) & 0x0f) - 8) * rv));
    					}
    					else	/* use full byte, first the high 4 bits, then the low 4 bits */
    					{
    						if ((offs & 1) != 0)
    						{
    							lmix.writeinc((short) (lmix.read() + ((w.read(offs>>1) & 0x0f) - 8) * lv));
    							rmix.writeinc((short) (rmix.read() + ((w.read(offs>>1) & 0x0f) - 8) * rv));
    						}
    						else
    						{
    							lmix.writeinc((short) (lmix.read() + (((w.read(offs>>1)>>4) & 0x0f) - 8) * lv));
    							rmix.writeinc((short) (rmix.read() + (((w.read(offs>>1)>>4) & 0x0f) - 8) * rv));
    						}
    					}
    				}
    
    				/* update the counter for this voice */
    				voice.counter = c;
    			}
    		}
    	}
    
    	/* mix it down */
    	lmix = mixer_buffer;
    	rmix = mixer_buffer_2;
    	{
    		ShortPtr dest1 = buffer[0];
    		ShortPtr dest2 = buffer[1];
    		for (i = 0; i < length; i++)
    		{
    			dest1.writeinc( mixer_lookup[lmix.readinc()]);
    			dest2.writeinc( mixer_lookup[rmix.readinc()]);
    		}
    	}
        }
    };
  
    public int start(MachineSound msound) {
        String mono_name = "NAMCO sound";
        String[] stereo_names = {"NAMCO sound left", "NAMCO sound right"};

        namco_interface intf = (namco_interface) msound.sound_interface;

        namco_clock = intf.samplerate;
        sample_rate = Machine.sample_rate;
        /* get stream channels */
        System.out.println("NAMCO Sound "+intf.stereo);
        if (intf.stereo != 0) {            
            		int[] vol=new int[2];
            
            		vol[0] = MIXER(intf.volume,MIXER_PAN_LEFT);
            		vol[1] = MIXER(intf.volume,MIXER_PAN_RIGHT);
            		stream = stream_init_multi(2, stereo_names, vol, intf.samplerate, 0, namco_update_stereo);
        } else {
            stream = stream_init(mono_name, intf.volume, intf.samplerate, 0, namco_update_mono);
        }
        /* allocate a pair of buffers to mix into - 1 second's worth should be more than enough */
        mixer_buffer = new ShortPtr(2 * intf.samplerate * 2);
        mixer_buffer_2 = new ShortPtr(mixer_buffer, intf.samplerate * 2);

        /* build the mixer table */
        make_mixer_table(intf.voices);
        /* extract globals from the interface */
        num_voices = intf.voices;
        last_channel = num_voices;

        if (intf.region == -1) {
            sound_prom = namco_wavedata;
            samples_per_byte = 2;/* first 4 high bits, then low 4 bits */

        } else {
            sound_prom = memory_region(intf.region);
            samples_per_byte = 1;/* use only low 4 bits */

        }

        /* start with sound enabled, many games don't have a sound enable register */
        sound_enable = 1;
        /* reset all the voices */
        for (int i = 0; i < last_channel; i++) //for (voice = channel_list; voice < last_channel; voice++)
        {
            channel_list[i] = new sound_channel();
            channel_list[i].frequency = 0;
            channel_list[i].volume[0] = channel_list[i].volume[1] = 0;
            channel_list[i].wave = sound_prom;
            channel_list[i].counter = 0;
            channel_list[i].noise_sw = 0;
            channel_list[i].noise_state = 0;
            channel_list[i].noise_seed = 1;
            channel_list[i].noise_counter = 0;
        }

        return 0;

    }

    @Override
    public void stop() {
        mixer_buffer = null;
    }

    /**
     * *****************************************************************************
     */
    public static WriteHandlerPtr pengo_sound_enable_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            sound_enable = data;
        }
    };
    public static WriteHandlerPtr pengo_sound_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int voice;
            int _base;

            /* update the streams */
            stream_update(stream, 0);

            /* set the register */
            namco_soundregs.write(offset, data & 0x0f);

            /* recompute all the voice parameters */
            for (_base = 0, voice = 0; voice < last_channel; voice++, _base += 5) {
                channel_list[voice].frequency = namco_soundregs.read(0x14 + _base);/* always 0 */

                channel_list[voice].frequency = channel_list[voice].frequency * 16 + namco_soundregs.read(0x13 + _base);
                channel_list[voice].frequency = channel_list[voice].frequency * 16 + namco_soundregs.read(0x12 + _base);
                channel_list[voice].frequency = channel_list[voice].frequency * 16 + namco_soundregs.read(0x11 + _base);
                if (_base == 0) /* the first voice has extra frequency bits */ {
                    channel_list[voice].frequency = channel_list[voice].frequency * 16 + namco_soundregs.read(0x10 + _base);
                } else {
                    channel_list[voice].frequency = channel_list[voice].frequency * 16;
                }

                channel_list[voice].volume[0] = namco_soundregs.read(0x15 + _base) & 0x0f;
                channel_list[voice].wave = new UBytePtr(sound_prom, 32 * (namco_soundregs.read(0x05 + _base) & 7));
            }
        }
    };

    
    /********************************************************************************/
    
    public static WriteHandlerPtr polepos_sound_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            sound_channel[] voice;
            int base;
            int _voice = 0;

            /* update the streams */
            stream_update(stream, 0);

            /* set the register */
            namco_soundregs.write(offset, data);

            /* recompute all the voice parameters */
            for (base = 8, voice = channel_list; _voice < last_channel; _voice++, base += 4)
            {
                    voice[_voice].frequency = namco_soundregs.read(0x01 + base);
                    voice[_voice].frequency = voice[_voice].frequency * 256 + namco_soundregs.read(0x00 + base);

                    /* the volume seems to vary between one of these five places */
                    /* it's likely that only 3 or 4 are valid; for now, we just */
                    /* take the maximum volume and that seems to do the trick */
                    /* volume[0] = left speaker ?, volume[1] = right speaker ? */
                    voice[_voice].volume[0] = voice[_voice].volume[1] = 0;
                    // front speaker ?
                    voice[_voice].volume[1] |= namco_soundregs.read(0x02 + base) & 0x0f;
                    voice[_voice].volume[0] |= namco_soundregs.read(0x02 + base) >> 4;
                    // rear speaker ?
                    voice[_voice].volume[1] |= namco_soundregs.read(0x03 + base) & 0x0f;
                    voice[_voice].volume[0] |= namco_soundregs.read(0x03 + base) >> 4;
                    voice[_voice].volume[1] |= namco_soundregs.read(0x23 + base) >> 4;
                    voice[_voice].wave = new UBytePtr(sound_prom, 32 * (namco_soundregs.read(0x23 + base) & 7));
    	}
    }};
    
    /********************************************************************************/
    public static WriteHandlerPtr mappy_sound_enable_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            sound_enable = offset;
        }
    };
    public static WriteHandlerPtr mappy_sound_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int voice;
            int _base;

            /* update the streams */
            stream_update(stream, 0);

            /* set the register */
            namco_soundregs.write(offset, data);

            /* recompute all the voice parameters */
            for (_base = 0, voice = 0; voice < last_channel; voice++, _base += 8) {
                channel_list[voice].frequency = namco_soundregs.read(0x06 + _base) & 15;/* high bits are from here */

                channel_list[voice].frequency = channel_list[voice].frequency * 256 + namco_soundregs.read(0x05 + _base);
                channel_list[voice].frequency = channel_list[voice].frequency * 256 + namco_soundregs.read(0x04 + _base);

                channel_list[voice].volume[0] = namco_soundregs.read(0x03 + _base) & 0x0f;
                channel_list[voice].wave = new UBytePtr(sound_prom, 32 * ((namco_soundregs.read(0x06 + _base) >> 4) & 7));
            }
        }
    };
    /**
     * *****************************************************************************
     */
    static int nssw;
    public static WriteHandlerPtr namcos1_sound_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int voice;
            int _base;

            /* verify the offset */
            if (offset > 63) {
                logerror("NAMCOS1 sound: Attempting to write past the 64 registers segment\n");
                return;
            }

            /* update the streams */
            stream_update(stream, 0);

            /* set the register */
            namco_soundregs.write(offset, data);

            /* recompute all the voice parameters */
            for (_base = 0, voice = 0; voice < last_channel; voice++, _base += 8) {
                channel_list[voice].frequency = namco_soundregs.read(0x01 + _base) & 15;/* high bits are from here */

                channel_list[voice].frequency = channel_list[voice].frequency * 256 + namco_soundregs.read(0x02 + _base);
                channel_list[voice].frequency = channel_list[voice].frequency * 256 + namco_soundregs.read(0x03 + _base);

                channel_list[voice].volume[0] = namco_soundregs.read(0x00 + _base) & 0x0f;
                channel_list[voice].volume[1] = namco_soundregs.read(0x04 + _base) & 0x0f;
                channel_list[voice].wave = new UBytePtr(sound_prom, 32 / samples_per_byte * ((namco_soundregs.read(0x01 + _base) >> 4) & 15));

                nssw = ((namco_soundregs.read(0x04 + _base) & 0x80) >> 7);
                if ((voice + 1) < last_channel) {
                    channel_list[voice + 1].noise_sw = nssw;
                }
            }
            //voice = 0;
            channel_list[0].noise_sw = nssw;
        }
    };
    public static ReadHandlerPtr namcos1_sound_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return namco_soundregs.read(offset);
        }
    };
    public static WriteHandlerPtr namcos1_wavedata_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* update the streams */
            stream_update(stream, 0);

            namco_wavedata.write(offset, data);
        }
    };
    public static ReadHandlerPtr namcos1_wavedata_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return namco_wavedata.read(offset);
        }
    };

    /**
     * *****************************************************************************
     */
    public static WriteHandlerPtr snkwave_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {

            int freq0 = 0xff;
            sound_channel voice = channel_list[0];//sound_channel *voice = channel_list
            if (offset == 0) {
                freq0 = data;
            }
            if (offset == 1) {
                stream_update(stream, 0);
                if (data == 0xff || freq0 == 0) {
                    voice.volume[0] = 0x0;
                } else {
                    voice.volume[0] = 0x8;
                    voice.frequency = (data << 16) / freq0;
                }
            }
        }
    };

    @Override
    public void update() {
        //no functionality expected
    }

    @Override
    public void reset() {
        //no functionality expected
    }

}
