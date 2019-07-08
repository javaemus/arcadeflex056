/**
 * ported to v0.56
 *
 */
/**
 * Changelog
 * =========
 * 30/05/2019 ported to mame 0.56 (chusogar)
 */
package mame056.sound;

import static arcadeflex036.osdepend.logerror;
import static arcadeflex056.fucPtr.*;

import static common.libc.cstdio.*;
import static common.ptr.*;

import static mame056.common.*;
import static mame056.mame.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;

import static mame056.sound.oki6295H.*;
import static mame056.sound.streams.*;
import static mame056.sound.adpcm.*;

public class oki6295 extends snd_interface {
    
    /**********************************************************************************************
    *
    *	OKIM 6295 ADPCM chip:
    *
    *	Command bytes are sent:
    *
    *		1xxx xxxx = start of 2-byte command sequence, xxxxxxx is the sample number to trigger
    *		abcd vvvv = second half of command; one of the abcd bits is set to indicate which voice
    *		            the v bits seem to be volumed
    *
    *		0abc d000 = stop playing; one or more of the abcd bits is set to indicate which voice(s)
    *
    *	Status is read:
    *
    *		???? abcd = one bit per voice, set to 0 if nothing is playing, or 1 if it is active
    *
    ***********************************************************************************************/

    public static int OKIM6295_VOICES = 4;

    static int[] okim6295_command = new int[MAX_OKIM6295];
    static int[][] okim6295_base = new int[MAX_OKIM6295][OKIM6295_VOICES];


    
    public oki6295() {
        this.sound_num = SOUND_OKIM6295;
        this.name = "OKIM6295";
    }

    @Override
    public int chips_num(MachineSound msound) {
        return ((OKIM6295interface) msound.sound_interface).num;
    }

    @Override
    public int chips_clock(MachineSound msound) {
        return 0;//NO functionality expected
    }

    @Override
    public int start(MachineSound msound) {
        
        OKIM6295interface intf = (OKIM6295interface) msound.sound_interface;
        String stream_name;
        int i;

        /* reset the ADPCM system */
        num_voices = intf.num * OKIM6295_VOICES;
        compute_tables();

        /* initialize the voices */
        //memset(adpcm, 0, adpcm.length);
        for (i = 0; i < num_voices; i++)
        {
                int chip = i / OKIM6295_VOICES;
                int voice = i % OKIM6295_VOICES;

                /* reset the OKI-specific parameters */
                okim6295_command[chip] = -1;
                okim6295_base[chip][voice] = 0;

                /* generate the name and create the stream */
                stream_name = sprintf( "%s #%d (voice %d)", sound_name(msound), chip, voice);
                adpcm[i].stream = stream_init(stream_name, intf.mixing_level[chip], Machine.sample_rate, i, adpcm_update);
                if (adpcm[i].stream == -1)
                        return 1;

                /* initialize the rest of the structure */
                adpcm[i].region_base = new UBytePtr(memory_region(intf.region[chip]));
                adpcm[i].volume = 255;
                adpcm[i].signal = -2;
                if (Machine.sample_rate != 0)
                        adpcm[i].source_step = (int) ((double)intf.frequency[chip] * (double)FRAC_ONE / (double)Machine.sample_rate);
        }

        /*TODO*///okim6295_state_save_register();

        /* success */
        return 0;
    }

    @Override
    public void stop() {
        //no functionality expected
    }

    @Override
    public void update() {
        //no functionality expected
    }

    @Override
    public void reset() {
        //NO functionality expected
    }
    
    /**********************************************************************************************
	
	     OKIM6295_set_bank_base -- set the base of the bank for a given voice on a given chip
	
	***********************************************************************************************/
	
	public static void OKIM6295_set_bank_base(int which, int base)
	{
		int channel;
	
		for (channel = 0; channel < OKIM6295_VOICES; channel++)
		{
			ADPCMVoice voice = adpcm[which * OKIM6295_VOICES + channel];
	
			/* update the stream and set the new base */
			stream_update(voice.stream, 0);
			okim6295_base[which][channel] = base;
		}
	}
	
	
	
	/**********************************************************************************************
	
	     OKIM6295_set_frequency -- dynamically adjusts the frequency of a given ADPCM voice
	
	***********************************************************************************************/
	
	void OKIM6295_set_frequency(int which, int frequency)
	{
		int channel;
	
		for (channel = 0; channel < OKIM6295_VOICES; channel++)
		{
			ADPCMVoice voice = adpcm[which * OKIM6295_VOICES + channel];
	
			/* update the stream and set the new base */
			stream_update(voice.stream, 0);
			if (Machine.sample_rate != 0)
				voice.source_step = (int) ((double)frequency * (double)FRAC_ONE / (double)Machine.sample_rate);
		}
	}
	
	
	/**********************************************************************************************
	
	     OKIM6295_status_r -- read the status port of an OKIM6295-compatible chip
	
	***********************************************************************************************/
	
	static int OKIM6295_status_r(int num)
	{
		int i, result;
	
		/* range check the numbers */
		if (num >= num_voices / OKIM6295_VOICES)
		{
			logerror("error: OKIM6295_status_r() called with chip = %d, but only %d chips allocated\n",num, num_voices / OKIM6295_VOICES);
			return 0xff;
		}
	
		result = 0xf0;	/* naname expects bits 4-7 to be 1 */
		/* set the bit to 1 if something is playing on a given channel */
		for (i = 0; i < OKIM6295_VOICES; i++)
		{
			ADPCMVoice voice = adpcm[num * OKIM6295_VOICES + i];
	
			/* update the stream */
			stream_update(voice.stream, 0);
	
			/* set the bit if it's playing */
			if (voice.playing != 0)
				result |= 1 << i;
		}
	
		return result;
	}
	
	
	
	/**********************************************************************************************
	
	     OKIM6295_data_w -- write to the data port of an OKIM6295-compatible chip
	
	***********************************************************************************************/
	
	static void OKIM6295_data_w(int num, int data)
	{
		/* range check the numbers */
		if (num >= num_voices / OKIM6295_VOICES)
		{
			logerror("error: OKIM6295_data_w() called with chip = %d, but only %d chips allocated\n", num, num_voices / OKIM6295_VOICES);
			return;
		}
	
		/* if a command is pending, process the second half */
		if (okim6295_command[num] != -1)
		{
			int temp = data >> 4, i, start, stop;
			UBytePtr base = new UBytePtr();
	
			/* determine which voice(s) (voice is set by a 1 bit in the upper 4 bits of the second byte) */
			for (i = 0; i < OKIM6295_VOICES; i++, temp >>= 1)
			{
				if ((temp & 1) != 0)
				{
					ADPCMVoice voice = adpcm[num * OKIM6295_VOICES + i];
	
					/* update the stream */
					stream_update(voice.stream, 0);
	
					if (Machine.sample_rate == 0) return;
	
					/* determine the start/stop positions */
					base = new UBytePtr(voice.region_base, ( okim6295_base[num][i] + okim6295_command[num] * 8));
					start = (base.read(0) << 16) + (base.read(1) << 8) + base.read(2);
					stop = (base.read(3) << 16) + (base.read(4) << 8) + base.read(5);
	
					/* set up the voice to play this sample */
					if (start < 0x40000 && stop < 0x40000)
					{
						voice.playing = 1;
						voice.base = new UBytePtr(voice.region_base, okim6295_base[num][i] + start);
						voice.sample = 0;
						voice.count = 2 * (stop - start + 1);
	
						/* also reset the ADPCM parameters */
						voice.signal = -2;
						voice.step = 0;
						voice.volume = volume_table[data & 0x0f];
					}
	
					/* invalid samples go here */
					else
					{
						logerror("OKIM6295: requested to play invalid sample %02x\n",okim6295_command[num]);
						voice.playing = 0;
					}
				}
			}
	
			/* reset the command */
			okim6295_command[num] = -1;
		}
	
		/* if this is the start of a command, remember the sample number for next time */
		else if ((data & 0x80) != 0)
		{
			okim6295_command[num] = data & 0x7f;
		}
	
		/* otherwise, see if this is a silence command */
		else
		{
			int temp = data >> 3, i;
	
			/* determine which voice(s) (voice is set by a 1 bit in bits 3-6 of the command */
			for (i = 0; i < 4; i++, temp >>= 1)
			{
				if ((temp & 1) != 0)
				{
					ADPCMVoice voice = adpcm[num * OKIM6295_VOICES + i];
	
					/* update the stream, then turn it off */
					stream_update(voice.stream, 0);
					voice.playing = 0;
				}
			}
		}
	}
	
	
	
	/**********************************************************************************************
	
	     OKIM6295_status_0_r -- generic status read functions
	     OKIM6295_status_1_r
	
	***********************************************************************************************/
	
	public static ReadHandlerPtr OKIM6295_status_0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return OKIM6295_status_r(0);
	} };
	
	public static ReadHandlerPtr OKIM6295_status_1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return OKIM6295_status_r(1);
	} };
	
	public static ReadHandlerPtr OKIM6295_status_0_lsb_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return OKIM6295_status_r(0);
	} };
	
	public static ReadHandlerPtr OKIM6295_status_1_lsb_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return OKIM6295_status_r(1);
	} };
	
	public static ReadHandlerPtr OKIM6295_status_0_msb_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return OKIM6295_status_r(0) << 8;
	} };
	
	public static ReadHandlerPtr OKIM6295_status_1_msb_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return OKIM6295_status_r(1) << 8;
	} };
	
	
	
	/**********************************************************************************************
	
	     OKIM6295_data_0_w -- generic data write functions
	     OKIM6295_data_1_w
	
	***********************************************************************************************/
	
	public static WriteHandlerPtr OKIM6295_data_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		OKIM6295_data_w(0, data);
	} };
	
	public static WriteHandlerPtr OKIM6295_data_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		OKIM6295_data_w(1, data);
	} };
	
	public static WriteHandlerPtr OKIM6295_data_0_lsb_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		//if (ACCESSING_LSB)
			OKIM6295_data_w(0, data & 0xff);
	} };
	
	public static WriteHandlerPtr OKIM6295_data_1_lsb_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		//if (ACCESSING_LSB)
			OKIM6295_data_w(1, data & 0xff);
	} };
	
	public static WriteHandlerPtr OKIM6295_data_0_msb_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		//if (ACCESSING_MSB)
			OKIM6295_data_w(0, data >> 8);
	} };
	
	public static WriteHandlerPtr OKIM6295_data_1_msb_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		//if (ACCESSING_MSB)
			OKIM6295_data_w(1, data >> 8);
	} };
}
