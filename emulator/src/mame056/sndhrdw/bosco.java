/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.sndhrdw;

import static arcadeflex056.fucPtr.*;

import common.ptr.BytePtr;
import static common.ptr.UBytePtr;
import static mame037b11.sound.mixer.mixer_play_sample;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.mame.Machine;
import static mame056.sndintrfH.*;
import static mame056.sound.mixer.*;
import static mame056.sound.mixerH.*;

public class bosco
{
	
	/* macro to convert 4-bit unsigned samples to 8-bit signed samples */
	public static byte SAMPLE_CONV4(int a){
            return (byte) (0x11*((a&0x0f))-0x80);
        }
	
	static byte[] speech;	/* 24k for speech */
	static int channel;
	
	
	
	public static ShStartPtr bosco_sh_start = new ShStartPtr() {
            public int handler(MachineSound msound) {
                int i;
		int bits;
	
		channel = mixer_allocate_channel(25);
		mixer_set_name(channel,"Samples");
	
		speech = new byte[2*memory_region_length(REGION_SOUND2)];
		if (speech == null)
			return 1;
	
		/* decode the rom samples */
		for (i = 0;i < memory_region_length(REGION_SOUND2);i++)
		{
			bits = memory_region(REGION_SOUND2).read(i) & 0x0f;
			speech[2*i] = SAMPLE_CONV4(bits);
	
			bits = (memory_region(REGION_SOUND2).read(i) & 0xf0) >> 4;
			speech[2*i + 1] = SAMPLE_CONV4(bits);
		}
	
		return 0;
            }
        };
	
	public static ShStopPtr bosco_sh_stop = new ShStopPtr() {
            public void handler() {
                if (speech != null)		
                    speech = null;
            }
        };
	
	public static void bosco_sample_play(int offset, int length)
	{
		if (Machine.sample_rate == 0)
			return;
	
		mixer_play_sample(channel, new BytePtr(speech, offset),length,4000,0);
	}
}
