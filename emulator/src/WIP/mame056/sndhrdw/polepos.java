/***************************************************************************
	polepos.c
	Sound handler
****************************************************************************/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import arcadeflex056.fucPtr.WriteHandlerPtr;
import static common.libc.cstring.*;
import static common.ptr.*;
import static common.subArrays.*;
import static mame037b11.sound.mixer.mixer_play_sample;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.mame.*;
import static mame056.sndintrfH.*;
import static mame056.sound.mixer.*;
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;
import static mame056.sound.streams.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.timer.*;

public class polepos
{
	
	static int sample_msb = 0;
	static int sample_lsb = 0;
	static int sample_enable = 0;
	
	static int current_position;
	static int sound_stream;
	
	/* speech section */
	static int channel;
	static byte[] speech;
	/* macro to convert 4-bit unsigned samples to 8-bit signed samples */
	public static int SAMPLE_CONV4(int a){ 
            return (0x11*((a&0x0f))-0x80);
        }
        
	public static int SAMPLE_SIZE = 0x8000;
	
	public static int AMP(int r){
            return (r*128/10100);
        }
        
	static int volume_table[] =
	{
		AMP(2200), AMP(3200), AMP(4400), AMP(5400),
		AMP(6900), AMP(7900), AMP(9100), AMP(10100)
	};
	static int[] sample_offsets = new int[5];
	
	/************************************/
	/* Stream updater                   */
	/************************************/
	public static StreamInitPtr engine_sound_update = new StreamInitPtr() {
            public void handler(int param, ShortPtr buffer, int length) {
                int current = current_position, step, clock, slot, volume;
		UBytePtr base;
                int _buffer = 0;
	
	
		/* if we're not enabled, just fill with 0 */
		if ((sample_enable == 0) || Machine.sample_rate == 0)
		{
			memset(buffer, 0, length);
			return;
		}
	
		/* determine the effective clock rate */
		clock = (Machine.drv.cpu[0].cpu_clock / 64) * ((sample_msb + 1) * 64 + sample_lsb + 1) / (16*64);
		step = (clock << 12) / Machine.sample_rate;
	
		/* determine the volume */
		slot = (sample_msb >> 3) & 7;
		volume = volume_table[slot];
		base = new UBytePtr(memory_region(REGION_SOUND1), 0x1000 + slot * 0x800);
	
		/* fill in the sample */
		while ((length--) != 0)
		{
			buffer.write(_buffer++, (short) (base.read((current >> 12) & 0x7ff) * volume));
			current += step;
		}
	
		current_position = current;
            }
        };
	
	/************************************/
	/* Sound handler start              */
	/************************************/
	public static ShStartPtr polepos_sh_start = new ShStartPtr() {
            public int handler(MachineSound msound) {
                int i, bits, last=0;
	
		channel = mixer_allocate_channel(25);
		mixer_set_name(channel,"Speech");
	
		speech = new byte[16*SAMPLE_SIZE];
		if (speech == null)
			return 1;
	
		/* decode the rom samples, interpolating to make it sound a little better */
		for (i = 0;i < SAMPLE_SIZE;i++)
		{
			bits = memory_region(REGION_SOUND1).read(0x5000+i) & 0x0f;
			bits = SAMPLE_CONV4(bits);
			speech[16*i+0] = (byte) (((7 * last + 1 * bits) / 8) & 0xFF);
			speech[16*i+1] = (byte) (((6 * last + 2 * bits) / 8) & 0xFF);
			speech[16*i+2] = (byte) (((5 * last + 3 * bits) / 8) & 0xFF);
			speech[16*i+3] = (byte) (((4 * last + 4 * bits) / 8) & 0xFF);
			speech[16*i+4] = (byte) (((3 * last + 5 * bits) / 8) & 0xFF);
			speech[16*i+5] = (byte) (((2 * last + 6 * bits) / 8) & 0xFF);
			speech[16*i+6] = (byte) (((1 * last + 7 * bits) / 8) & 0xFF);
			speech[16*i+7] = (byte) (bits & 0xFF);
			last = bits;
	
			bits = (memory_region(REGION_SOUND1).read(0x5000+i) & 0xf0) >> 4;
			bits = SAMPLE_CONV4(bits);
			speech[16*i+8] = (byte) (((7 * last + 1 * bits) / 8) & 0xFF);
			speech[16*i+9] = (byte) (((6 * last + 2 * bits) / 8) & 0xFF);
			speech[16*i+10] = (byte) (((5 * last + 3 * bits) / 8) & 0xFF);
			speech[16*i+11] = (byte) (((4 * last + 4 * bits) / 8) & 0xFF);
			speech[16*i+12] = (byte) (((3 * last + 5 * bits) / 8) & 0xFF);
			speech[16*i+13] = (byte) (((2 * last + 6 * bits) / 8) & 0xFF);
			speech[16*i+14] = (byte) (((1 * last + 7 * bits) / 8) & 0xFF);
			speech[16*i+15] = (byte) (bits & 0xFF);
			last = bits;
		}
	
		/* Japanese or US PROM? */
		if (memory_region(REGION_SOUND1).read(0x5000) == 0)
		{
			/* US */
			sample_offsets[0] = 0x0020;
			sample_offsets[1] = 0x0c00;
			sample_offsets[2] = 0x1c00;
			sample_offsets[3] = 0x2000;
			sample_offsets[4] = 0x2000;
		}
		else
		{
			/* Japan */
			sample_offsets[0] = 0x0020;
			sample_offsets[1] = 0x0900;
			sample_offsets[2] = 0x1f00;
			sample_offsets[3] = 0x4000;
			sample_offsets[4] = 0x6000;		/* How is this triggered? */
		}
	
		sound_stream = stream_init("Engine Sound", 50, Machine.sample_rate, 0, engine_sound_update);
		current_position = 0;
		sample_msb = sample_lsb = 0;
		sample_enable = 0;
                return 0;
            }
        };
	
	/************************************/
	/* Sound handler stop               */
	/************************************/
	public static ShStopPtr polepos_sh_stop = new ShStopPtr() {
            public void handler() {
                if (speech != null)			
                    speech = null;
            }
        };
	
	/************************************/
	/* Sound handler update 			*/
	/************************************/
	public static ShUpdatePtr polepos_sh_update = new ShUpdatePtr() {
            public void handler() {
            
            }
        };
	
	/************************************/
	/* Write LSB of engine sound		*/
	/************************************/
	public static WriteHandlerPtr polepos_engine_sound_lsb_w = new WriteHandlerPtr() {
            public void handler(int offset, int data)
            {
		stream_update(sound_stream, 0);
		sample_lsb = data & 62;
                sample_enable = data & 1;
            } 
        };
	
	/************************************/
	/* Write MSB of engine sound		*/
	/************************************/
	public static WriteHandlerPtr polepos_engine_sound_msb_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		stream_update(sound_stream, 0);
		sample_msb = data & 63;
	} };
	
	/************************************/
	/* Play speech sample				*/
	/************************************/
	public static void polepos_sample_play(int sample)
	{
		int start = sample_offsets[sample];
		int len = sample_offsets[sample + 1] - start;
	
		if (Machine.sample_rate == 0)
			return;
	
		mixer_play_sample(channel, new BytePtr(speech, start * 16), len * 16, 4000*8, 0);
	}
}
