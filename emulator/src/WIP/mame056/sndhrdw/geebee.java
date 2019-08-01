/****************************************************************************
 *
 * geebee.c
 *
 * sound driver
 * juergen buchmueller <pullmoll@t-online.de>, jan 2000
 *
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

public class geebee
{
	
	static timer_entry volume_timer = null;
	static int[] decay = null;
	static int channel;
	static int sound_latch = 0;
	static int sound_signal = 0;
	static int volume = 0;
	static int noise = 0;
	
	static timer_callback volume_decay = new timer_callback() {
            public void handler(int param) {
                if( --volume < 0 )
			volume = 0;
            }
        };
	
	
	public static WriteHandlerPtr geebee_sound_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		stream_update(channel,0);
		sound_latch = data;
		volume = 0x7fff; /* set volume */
		noise = 0x0000;  /* reset noise shifter */
		/* faster decay enabled? */
		if(( sound_latch & 8 ) != 0)
		{
			/*
			 * R24 is 10k, Rb is 0, C57 is 1uF
			 * charge time t1 = 0.693 * (R24 + Rb) * C57 . 0.22176s
			 * discharge time t2 = 0.693 * (Rb) * C57 . 0
			 * Then C33 is only charged via D6 (1N914), not discharged!
			 * Decay:
			 * discharge C33 (1uF) through R50 (22k) . 0.14058s
			 */
			if( volume_timer != null )
				timer_remove(volume_timer);
			volume_timer = timer_pulse(TIME_IN_HZ(32768/0.14058), 0, volume_decay);
		}
		else
		{
			/*
			 * discharge only after R49 (100k) in the amplifier section,
			 * so the volume shouldn't very fast and only when the signal
			 * is gated through 6N (4066).
			 * I can only guess here that the decay should be slower,
			 * maybe half as fast?
			 */
			if( volume_timer != null )
				timer_remove(volume_timer);
			volume_timer = timer_pulse(TIME_IN_HZ(32768/0.2906), 0, volume_decay);
	    }
	} };
        
        static int vcarry = 0;
	static int vcount = 0;
	
	static StreamInitPtr geebee_sound_update = new StreamInitPtr() {
            public void handler(int param, ShortPtr buffer, int length) {
                while ((length--) != 0)
	    {
			buffer.inc(sound_signal);
			/* 1V = HSYNC = 18.432MHz / 3 / 2 / 384 = 8000Hz */
			vcarry -= 18432000 / 3 / 2 / 384;
	        while (vcarry < 0)
	        {
	            vcarry += Machine.sample_rate;
	            vcount++;
				/* noise clocked with raising edge of 2V */
				if ((vcount & 3) == 2)
				{
					/* bit0 = bit0 ^ !bit10 */
					if ((noise & 1) == ((noise >> 10) & 1))
						noise = ((noise << 1) & 0xfffe) | 1;
					else
						noise = (noise << 1) & 0xfffe;
				}
	            switch (sound_latch & 7)
	            {
	            case 0: /* 4V */
					sound_signal = (vcount & 0x04)!=0 ? decay[volume] : 0;
	                break;
	            case 1: /* 8V */
					sound_signal = (vcount & 0x08)!=0 ? decay[volume] : 0;
	                break;
	            case 2: /* 16V */
					sound_signal = (vcount & 0x10)!=0 ? decay[volume] : 0;
	                break;
	            case 3: /* 32V */
					sound_signal = (vcount & 0x20)!=0 ? decay[volume] : 0;
	                break;
	            case 4: /* TONE1 */
					sound_signal = (vcount & 0x01)==0 && (vcount & 0x10)==0 ? decay[volume] : 0;
	                break;
	            case 5: /* TONE2 */
					sound_signal = (vcount & 0x02)==0 && (vcount & 0x20)==0 ? decay[volume] : 0;
	                break;
	            case 6: /* TONE3 */
					sound_signal = (vcount & 0x04)==0 && (vcount & 0x40)==0 ? decay[volume] : 0;
	                break;
				default: /* NOISE */
					/* QH of 74164 #4V */
	                sound_signal = (noise & 0x8000)!=0 ? decay[volume] : 0;
	            }
	        }
	    }
            }
        };
	
	public static ShStartPtr geebee_sh_start = new ShStartPtr() {
            public int handler(MachineSound msound) {
                int i;
	
		decay = new int[32768];
		if (decay == null)
			return 1;
	
	    for( i = 0; i < 0x8000; i++ )
			decay[0x7fff-i] = (int) (0x7fff/Math.exp(1.0*i/4096));
	
		channel = stream_init("GeeBee", 100, Machine.sample_rate, 0, geebee_sound_update);
	    return 0;
            }
        };
	
	
	public static ShStopPtr geebee_sh_stop = new ShStopPtr() {
            public void handler() {
                if( volume_timer != null )
			timer_remove(volume_timer);
		volume_timer = null;
	    if( decay != null )
			decay = null;
            }
        };
	
	
	public static ShUpdatePtr geebee_sh_update = new ShUpdatePtr() {
            public void handler() {
                stream_update(channel,0);
            }
        };
	
}
