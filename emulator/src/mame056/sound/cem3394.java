/***************************************************************************

	CEM3394 sound driver.

	This driver handles CEM-3394 analog synth chip. Very crudely.

	Still to do:
		- adjust the overall volume when multiple waves are being generated
		- filter internal sound
		- support resonance (don't understand how it works)

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.sound;

import static common.libc.cstdio.*;
import static common.ptr.*;
import static java.lang.Math.pow;
import static mame056.mame.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;

import static mame056.sound.cem3394H.*;
import static mame056.sound.streams.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class cem3394 extends snd_interface 
{
	
	public cem3394(){
            sound_num = SOUND_CEM3394;
            name = "CEM3394";
            
            /*for (int i=0 ; i<MAX_CEM3394 ; i++)
                chip_list[i] = new sound_chip();*/
        }
	
	/* waveform generation parameters */
	public static int ENABLE_PULSE      = 1;
	public static int ENABLE_TRIANGLE   = 1;
	public static int ENABLE_SAWTOOTH   = 1;
	public static int ENABLE_EXTERNAL   = 1;
	
	
	/* pulse shaping parameters */
	/* examples: */
	/*    hat trick - skidding ice sounds too loud if minimum width is too big */
	/*    snake pit - melody during first level too soft if minimum width is too small */
	/*    snake pit - bonus counter at the end of level */
	/*    snacks'n jaxson - laugh at end of level is too soft if minimum width is too small */
	public static double LIMIT_WIDTH    = 1.0;
	public static double MINIMUM_WIDTH  = 0.25;
	public static double MAXIMUM_WIDTH  = 0.75;
	
	public static int LIMIT_STEP        = 1;
	
	
	/********************************************************************************
	
		From the datasheet:
	
		CEM3394_VCO_FREQUENCY:
			-4.0 ... +4.0
			-0.75 V/octave
			f = exp(V) * 431.894
	
		CEM3394_MODULATION_AMOUNT
			 0.0 ... +3.5
			 0.0 == 0.01 x frequency
			 3.5 == 2.00 x frequency
	
		CEM3394_WAVE_SELECT
			-0.5 ... -0.2 == triangle
			+0.9 ... +1.5 == triangle + sawtooth
			+2.3 ... +3.9 == sawtooth
	
		CEM3394_PULSE_WIDTH
			 0.0 ... +2.0
			 0.0 ==   0% duty cycle
			+2.0 == 100% duty cycle
	
		CEM3394_MIXER_BALANCE
			-4.0 ... +4.0
			 0.0 both at -6dB
			 -20 dB/V
	
		CEM3394_FILTER_RESONANCE
			 0.0 ... +2.5
			 0.0 == no resonance
			+2.5 == oscillation
	
		CEM3394_FILTER_FREQENCY
			-3.0 ... +4.0
			-0.375 V/octave
			 0.0 == 1300Hz
	
		CEM3394_FINAL_GAIN
			 0.0 ... +4.0
			 -20 dB/V
			 0.0 == -90dB
			 4.0 == 0dB
	
		Square wave output = 160 (average is constant regardless of duty cycle)
		Sawtooth output = 200
		Triangle output = 250
		Sawtooth + triangle output = 330
		Maximum output = 400
	
	********************************************************************************/
	
	
	/* various waveforms */
	public static int WAVE_TRIANGLE = 1;
	public static int WAVE_SAWTOOTH = 2;
	public static int WAVE_PULSE    = 4;
	
	/* keep lots of fractional bits */
	public static int FRACTION_BITS     = 28;
	public static int FRACTION_ONE      = (1 << FRACTION_BITS);
	public static double FRACTION_ONE_D = ((double)(1 << FRACTION_BITS));
	public static int FRACTION_MASK     = (FRACTION_ONE - 1);
	public static int FRACTION_MULT(int a, int b){
            return (((a) >> (FRACTION_BITS / 2)) * ((b) >> (FRACTION_BITS - FRACTION_BITS / 2)));
        }

    @Override
    public int chips_num(MachineSound msound) {
        return ((cem3394_interface) msound.sound_interface).numchips;
    }

    @Override
    public int chips_clock(MachineSound msound) {
        return 0;
    }

    @Override
    public int start(MachineSound msound) {
        return cem3394_sh_start(msound);
    }

    @Override
    public void stop() {
        cem3394_sh_stop();
    }

    @Override
    public void update() {
        //no functionality expected
    }

    @Override
    public void reset() {
        //no functionality expected
    }
	
	
	/* this structure defines the parameters for a channel */
	public static class sound_chip
	{
		public int stream;			/* our stream */
		public ExternalSamplesHandlerPtr external;/* callback to generate external samples */
		public double vco_zero_freq;			/* frequency of VCO at 0.0V */
		public double filter_zero_freq;		/* frequency of filter at 0.0V */
	
		public double[] values=new double[8];				/* raw values of registers */
		public int wave_select;				/* flags which waveforms are enabled */
	
		public int volume;					/* linear overall volume (0-256) */
		public int mixer_internal;			/* linear internal volume (0-256) */
		public int mixer_external;			/* linear external volume (0-256) */
	
		public int position;				/* current VCO frequency position (0.FRACTION_BITS) */
		public int step;					/* per-sample VCO step (0.FRACTION_BITS) */
	
		public int filter_position;			/* current filter frequency position (0.FRACTION_BITS) */
		public int filter_step;				/* per-sample filter step (0.FRACTION_BITS) */
		public int modulation_depth;		/* fraction of total by which we modulate (0.FRACTION_BITS) */
		public int last_ext;					/* last external sample we read */
	
		public int pulse_width;				/* fractional pulse width (0.FRACTION_BITS) */
	};
	
	
	/* data about the sound system */
	static sound_chip[] chip_list = new sound_chip[MAX_CEM3394];
	
	/* global sound parameters */
	static double inv_sample_rate;
	static int sample_rate;
	
	static ShortPtr mixer_buffer;
	static ShortPtr external_buffer;
	
	
	/* generate sound to the mix buffer in mono */
	static StreamInitPtr cem3394_update = new StreamInitPtr() {
            public void handler(int ch, ShortPtr buffer, int length) {
                sound_chip chip = chip_list[ch];
		int int_volume = (chip.volume * chip.mixer_internal) / 256;
		int ext_volume = (chip.volume * chip.mixer_external) / 256;
		int step = chip.step, position, end_position = 0;
		ShortPtr mix, ext;
		int i;
	
		/* external volume is effectively 0 if no external function */
		if ((chip.external==null) || (ENABLE_EXTERNAL==0))
			ext_volume = 0;
	
		/* adjust the volume for the filter */
		if (step > chip.filter_step)
			int_volume /= step - chip.filter_step;
	
		/* bail if nothing's going on */
		if (int_volume == 0 && ext_volume == 0)
		{
			//memset(buffer, 0, sizeof(INT16) * length);
                        buffer = new ShortPtr(length*2);
			return;
		}
	
		/* if there's external stuff, fetch and process it now */
		if (ext_volume != 0)
		{
			int fposition = chip.filter_position, fstep = chip.filter_step, depth;
			int last_ext = chip.last_ext;
	
			/* fetch the external data */
			(chip.external).handler(ch, length, external_buffer);
	
			/* compute the modulation depth, and adjust fstep to the maximum frequency */
			/* we lop off 13 bits of depth so that we can multiply by stepadjust, below, */
			/* which has 13 bits of precision */
			depth = FRACTION_MULT(fstep, chip.modulation_depth);
			fstep += depth;
			depth >>= 13;
	
			/* "apply" the filter: note this is pretty cheesy; it basically just downsamples the
			   external sample to filter_freq by allowing only 2 transitions for every cycle */
			for (i = 0, ext = new ShortPtr(external_buffer), position = chip.position; i < length; i++, ext.inc())
			{
				int newposition;
				int stepadjust;
	
				/* update the position and compute the adjustment from a triangle wave */
				if ((position & (1 << (FRACTION_BITS - 1))) != 0)
					stepadjust = 0x2000 - ((position >> (FRACTION_BITS - 14)) & 0x1fff);
				else
					stepadjust = (position >> (FRACTION_BITS - 14)) & 0x1fff;
				position += step;
	
				/* if we cross a half-step boundary, allow the next byte of the external input */
				newposition = fposition + fstep - (stepadjust * depth);
				if (((newposition ^ fposition) & ~(FRACTION_MASK >> 1)) != 0)
					last_ext = ext.read();
				else
					ext.write((short) last_ext);
				fposition = newposition & FRACTION_MASK;
			}
	
			/* update the final filter values */
			chip.filter_position = fposition;
			chip.last_ext = last_ext;
		}
	
		/* if there's internal stuff, generate it */
		if (int_volume != 0)
		{
			if ((chip.wave_select == 0) && (ext_volume == 0))
				logerror("%f V didn't cut it\n", chip.values[CEM3394_WAVE_SELECT]);
	
			/* handle the pulse component; it maxes out at 0x1932, which is 27% smaller than */
			/* the sawtooth (since the value is constant, this is the best place to have an */
			/* odd value for volume) */
			if ((ENABLE_PULSE!=0) && ((chip.wave_select & WAVE_PULSE)!=0))
			{
				int pulse_width = chip.pulse_width;
	
				/* this is a kludge; Snake Pit uses very small pulse widths in the tune during */
				/* level 1; this makes the melody almost silent unless we enforce a minimum */
				/* pulse width; even then, it's pretty tinny */
				if (LIMIT_STEP != 0)
				{
					if (pulse_width <= step) pulse_width = step + 1;
					else if (pulse_width >= FRACTION_ONE - step) pulse_width = FRACTION_ONE - step - 1;
				}
                                
				/* if the width is wider than the step, we're guaranteed to hit it once per cycle */
				if (pulse_width >= step)
				{
					for (i = 0, mix = new ShortPtr(mixer_buffer), position = chip.position; i < length; i++, mix.inc())
					{
						if (position < pulse_width)
							mix.write((short)0x1932);
						else
							mix.write((short)0x0000);
						position = (position + step) & FRACTION_MASK;
					}
				}
	
				/* otherwise, we compute a volume and watch for cycle boundary crossings */
				else
				{
					int volume = 0x1932 * pulse_width / step;
					for (i = 0, mix = new ShortPtr(mixer_buffer), position = chip.position; i < length; i++, mix.inc())
					{
						int newposition = position + step;
						if (((newposition ^ position) & ~FRACTION_MASK) != 0)
							mix.write((short) volume);
						else
							mix.write((short)0x0000);
						position = newposition & FRACTION_MASK;
					}
				}
				end_position = position;
			} 	
			/* otherwise, clear the mixing buffer */
                        else {
                            //System.out.println("INIT "+length);
				//memset(mixer_buffer, 0, sizeof(INT16) * length);
                            mixer_buffer = new ShortPtr(length * 2);
                            
                            //if (length == 1)
                            //    mixer_buffer = new ShortPtr();
                        }
	
			/* handle the sawtooth component; it maxes out at 0x2000, which is 27% larger */
			/* than the pulse */
			if ((ENABLE_SAWTOOTH!=0) && ((chip.wave_select & WAVE_SAWTOOTH)!=0))
			{
				for (i = 0, mix = new ShortPtr(mixer_buffer), position = chip.position; i < length; i++, mix.inc())
				{
					mix.write((short)(mix.read() + (((position >> (FRACTION_BITS - 14)) & 0x3fff) - 0x2000)));
					position += step;
				}
				end_position = position & FRACTION_MASK;
			}
	
			/* handle the triangle component; it maxes out at 0x2800, which is 25% larger */
			/* than the sawtooth (should be 27% according to the specs, but 25% saves us */
			/* a multiplication) */
			if ((ENABLE_TRIANGLE!=0) && ((chip.wave_select & WAVE_TRIANGLE)!=0))
			{
				for (i = 0, mix = new ShortPtr(mixer_buffer), position = chip.position; i < length; i++, mix.inc())
				{
					int value;
					if ((position & (1 << (FRACTION_BITS - 1))) != 0)
						value = 0x2000 - ((position >> (FRACTION_BITS - 14)) & 0x1fff);
					else
						value = (position >> (FRACTION_BITS - 14)) & 0x1fff;
					mix.write((short)(mix.read() + value + (value >> 2)));
					position += step;
				}
				end_position = position & FRACTION_MASK;
			}
	
			/* update the final position */
			chip.position = end_position;
		}
	
		/* mix it down */
		mix = new ShortPtr(mixer_buffer);
		ext = new ShortPtr(external_buffer);
		{
                        int _buffer = 0;
			/* internal + external */
			if (ext_volume != 0 && int_volume != 0)
			{
				for (i = 0; i < length; i++, mix.inc(), ext.inc()){
					buffer.write(_buffer++,(short)((mix.read() * int_volume + ext.read() * ext_volume) / 128));
                                        //buffer.inc();
                                }
			}
			/* internal only */
			else if (int_volume != 0)
			{
				for (i = 0; i < length; i++, mix.inc()){
					buffer.write(_buffer++, (short)(mix.read() * int_volume / 128) );
                                        //buffer.inc();
                                }
			}
			/* external only */
			else
			{
				for (i = 0; i < length; i++, ext.inc()){
					buffer.write(_buffer++, (short)(ext.read() * ext_volume / 128));
                                        //buffer.inc();
                                }
			}
		}
            }
        };
	
	public static int cem3394_sh_start(MachineSound msound)
	{
		cem3394_interface intf = (cem3394_interface) msound.sound_interface;
		int i;
	
		/* bag on a 0 sample_rate */
		if (Machine.sample_rate == 0)
			return 0;
	
		/* copy global parameters */
		sample_rate = Machine.sample_rate;
		inv_sample_rate = 1.0 / (double)sample_rate;
	
		/* allocate stream channels, 1 per chip */
		for (i = 0; i < intf.numchips; i++)
		{
			String name_buffer="";
	
			chip_list[i] = new sound_chip();
                        
			sprintf(name_buffer, "CEM3394 #%d", i);
			chip_list[i].stream = stream_init(name_buffer, intf.volume[i], sample_rate, i, cem3394_update);
			chip_list[i].external = intf.external[i];
			chip_list[i].vco_zero_freq = intf.vco_zero_freq[i];
			chip_list[i].filter_zero_freq = intf.filter_zero_freq[i];
                        
		}
	
		/* allocate memory for a mixer buffer and external buffer (1 second should do it!) */
		mixer_buffer = new ShortPtr(2 * sample_rate * 2);
		if (mixer_buffer == null)
			return 1;
		external_buffer = new ShortPtr(mixer_buffer, sample_rate * 2);
	
		return 0;
	}
	
	
	void cem3394_sh_stop()
	{
		if (mixer_buffer != null)
                    mixer_buffer = external_buffer = null;
	}
	
	
	public static double compute_db(double voltage)
	{
		/* assumes 0.0 == full off, 4.0 == full on, with linear taper, as described in the datasheet */
	
		/* above 4.0, maximum volume */
		if (voltage >= 4.0)
			return 0.0;
	
		/* below 0.0, minimum volume */
		else if (voltage <= 0.0)
			return 90.0;
	
		/* between 2.5 and 4.0, linear from 20dB to 0dB */
		else if (voltage >= 2.5)
			return (4.0 - voltage) * (1.0 / 1.5) * 20.0;
	
		/* between 0.0 and 2.5, exponential to 20dB */
		else
		{
			double temp = 20.0 * pow(2.0, 2.5 - voltage);
			if (temp < 90.0) return 90.0;
			else return temp;
		}
	}
	
	
	public static int compute_db_volume(double voltage)
	{
		double temp;
	
		/* assumes 0.0 == full off, 4.0 == full on, with linear taper, as described in the datasheet */
	
		/* above 4.0, maximum volume */
		if (voltage >= 4.0)
			return 256;
	
		/* below 0.0, minimum volume */
		else if (voltage <= 0.0)
			return 0;
	
		/* between 2.5 and 4.0, linear from 20dB to 0dB */
		else if (voltage >= 2.5)
			temp = (4.0 - voltage) * (1.0 / 1.5) * 20.0;
	
		/* between 0.0 and 2.5, exponential to 20dB */
		else
		{
			temp = 20.0 * pow(2.0, 2.5 - voltage);
			if (temp < 50.0) return 0;
		}
	
		/* convert from dB to volume and return */
		return (int) (256.0 * pow(0.891251, temp));
	}
	
	
	public static void cem3394_set_voltage(int chipnum, int input, double voltage)
	{
		sound_chip chip = chip_list[chipnum];
		double temp;
	
		/* don't do anything if no change */
		if (voltage == chip.values[input])
			return;
		chip.values[input] = voltage;
	
		/* update the stream first */
		stream_update(chip.stream, 0);
	
		/* switch off the input */
		switch (input)
		{
			/* frequency varies from -4.0 to +4.0, at 0.75V/octave */
			case CEM3394_VCO_FREQUENCY:
				temp = chip.vco_zero_freq * pow(2.0, -voltage * (1.0 / 0.75));
				chip.step = (int) (temp * inv_sample_rate * FRACTION_ONE_D);
				break;
	
			/* wave select determines triangle/sawtooth enable */
			case CEM3394_WAVE_SELECT:
				chip.wave_select &= ~(WAVE_TRIANGLE | WAVE_SAWTOOTH);
				if (voltage >= -0.5 && voltage <= -0.2)
					chip.wave_select |= WAVE_TRIANGLE;
				else if (voltage >=  0.9 && voltage <=  1.5)
					chip.wave_select |= WAVE_TRIANGLE | WAVE_SAWTOOTH;
				else if (voltage >=  2.3 && voltage <=  3.9)
					chip.wave_select |= WAVE_SAWTOOTH;
				break;
	
			/* pulse width determines duty cycle; 0.0 means 0%, 2.0 means 100% */
			case CEM3394_PULSE_WIDTH:
				if (voltage < 0.0)
				{
					chip.pulse_width = 0;
					chip.wave_select &= ~WAVE_PULSE;
				}
				else
				{
					temp = voltage * 0.5;
					if (LIMIT_WIDTH != 0)
						temp = MINIMUM_WIDTH + (MAXIMUM_WIDTH - MINIMUM_WIDTH) * temp;
					chip.pulse_width = (int)(temp * FRACTION_ONE_D);
					chip.wave_select |= WAVE_PULSE;
				}
				break;
	
			/* final gain is pretty self-explanatory; 0.0 means ~90dB, 4.0 means 0dB */
			case CEM3394_FINAL_GAIN:
				chip.volume = compute_db_volume(voltage);
				break;
	
			/* mixer balance is a pan between the external input and the internal input */
			/* 0.0 is equal parts of both; positive values favor external, negative favor internal */
			case CEM3394_MIXER_BALANCE:
				if (voltage >= 0.0)
				{
					chip.mixer_internal = compute_db_volume(3.55 - voltage);
					chip.mixer_external = compute_db_volume(3.55 + 0.45 * (voltage * 0.25));
				}
				else
				{
					chip.mixer_internal = compute_db_volume(3.55 - 0.45 * (voltage * 0.25));
					chip.mixer_external = compute_db_volume(3.55 + voltage);
				}
				break;
	
			/* filter frequency varies from -4.0 to +4.0, at 0.375V/octave */
			case CEM3394_FILTER_FREQENCY:
				temp = chip.filter_zero_freq * pow(2.0, -voltage * (1.0 / 0.375));
				chip.filter_step = (int)(temp * inv_sample_rate * FRACTION_ONE_D);
				break;
	
			/* modulation depth is 0.01 at 0V and 2.0 at 3.5V; how it grows from one to the other */
			/* is still unclear at this point */
			case CEM3394_MODULATION_AMOUNT:
				if (voltage < 0.0)
					chip.modulation_depth = (int)(0.01 * FRACTION_ONE_D);
				else if (voltage > 3.5)
					chip.modulation_depth = (int)(2.00 * FRACTION_ONE_D);
				else
					chip.modulation_depth = (int)(((voltage * (1.0 / 3.5)) * 1.99 + 0.01) * FRACTION_ONE_D);
				break;
	
			/* this is not yet implemented */
			case CEM3394_FILTER_RESONANCE:
				break;
		}
	}
	
	
	public static double cem3394_get_parameter(int chipnum, int input)
	{
		sound_chip chip = chip_list[chipnum];
		double voltage = chip.values[input];
	
		switch (input)
		{
			case CEM3394_VCO_FREQUENCY:
				return chip.vco_zero_freq * pow(2.0, -voltage * (1.0 / 0.75));
	
			case CEM3394_WAVE_SELECT:
				return voltage;
	
			case CEM3394_PULSE_WIDTH:
				if (voltage <= 0.0)
					return 0.0;
				else if (voltage >= 2.0)
					return 1.0;
				else
					return voltage * 0.5;
	
			case CEM3394_FINAL_GAIN:
				return compute_db(voltage);
	
			case CEM3394_MIXER_BALANCE:
				return voltage * 0.25;
	
			case CEM3394_MODULATION_AMOUNT:
				if (voltage < 0.0)
					return 0.01;
				else if (voltage > 3.5)
					return 2.0;
				else
					return (voltage * (1.0 / 3.5)) * 1.99 + 0.01;
	
			case CEM3394_FILTER_RESONANCE:
				if (voltage < 0.0)
					return 0.0;
				else if (voltage > 2.5)
					return 1.0;
				else
					return voltage * (1.0 / 2.5);
	
			case CEM3394_FILTER_FREQENCY:
				return chip.filter_zero_freq * pow(2.0, -voltage * (1.0 / 0.375));
		}
		return 0.0;
	}
}
