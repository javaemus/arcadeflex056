/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.sound;

import common.ptr.ShortPtr;
import common.ptr.UBytePtr;
import mame056.sndintrfH;


public class cem3394H {
    
    public static int MAX_CEM3394 = 6;
    
    public static abstract interface ExternalSamplesHandlerPtr {
        public abstract void handler(int channel, int length, ShortPtr ext_buffer);
    }

    /* interface */
    public static class cem3394_interface
    {
            public cem3394_interface(int numchips, int[] volume, double[] vco_zero_freq, double[] filter_zero_freq, ExternalSamplesHandlerPtr[] external){
                this.numchips = numchips;
                this.volume = volume;
                this.vco_zero_freq = vco_zero_freq;
                this.filter_zero_freq = filter_zero_freq;
                this.external = external;
            }
            
            public int numchips;					/* number of chips */
            public int[] volume = new int[MAX_CEM3394];			/* playback volume */
            public double[] vco_zero_freq = new double[MAX_CEM3394];	/* frequency at 0V for VCO */
            public double[] filter_zero_freq = new double[MAX_CEM3394];	/* frequency at 0V for filter */
            public ExternalSamplesHandlerPtr[] external = new ExternalSamplesHandlerPtr[MAX_CEM3394];    /* external input source (at Machine->sample_rate) */
    };

    /* inputs */
    public static final int CEM3394_VCO_FREQUENCY         = 0;
    public static final int CEM3394_MODULATION_AMOUNT     = 1;
    public static final int CEM3394_WAVE_SELECT           = 2;
    public static final int CEM3394_PULSE_WIDTH           = 3;
    public static final int CEM3394_MIXER_BALANCE         = 4;
    public static final int CEM3394_FILTER_RESONANCE      = 5;
    public static final int CEM3394_FILTER_FREQENCY       = 6;
    public static final int CEM3394_FINAL_GAIN            = 7;
    
    /*TODO*///int cem3394_sh_start(const struct MachineSound *msound);

    /* set the voltage going to a particular parameter */
    /*TODO*///void cem3394_set_voltage(int chip, int input, double voltage);

    /* get the translated parameter associated with the given input as follows:
            CEM3394_VCO_FREQUENCY:		frequency in Hz
            CEM3394_MODULATION_AMOUNT:	scale factor, 0.0 to 2.0
            CEM3394_WAVE_SELECT:		voltage from this line
            CEM3394_PULSE_WIDTH:		width fraction, from 0.0 to 1.0
            CEM3394_MIXER_BALANCE:		balance, from -1.0 to 1.0
            CEM3394_FILTER_RESONANCE:	resonance, from 0.0 to 1.0
            CEM3394_FILTER_FREQENCY:	frequency, in Hz
            CEM3394_FINAL_GAIN:			gain, in dB */
    /*TODO*///double cem3394_get_parameter(int chip, int input);

}
