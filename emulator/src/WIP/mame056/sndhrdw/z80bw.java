/* z80bw.c *********************************
 updated: 1997-04-09 08:46 TT
 updated  20-3-1998 LT Added colour changes on base explosion
 updated  02-6-1998 HJB copied from 8080bw and removed unneeded code
 *
 * Author      : Tormod Tjaberg
 * Created     : 1997-04-09
 * Description : Sound routines for the 'astinvad' games
 *
 * Note:
 * The samples were taken from Michael Strutt's (mstrutt@pixie.co.za)
 * excellent space invader emulator and converted to signed samples so
 * they would work under SEAL. The port info was also gleaned from
 * his emulator. These sounds should also work on all the invader games.
 *
 * The sounds are generated using output port 3 and 5
 *
 * Port 4:
 * bit 0=UFO  (repeats)       0.raw
 * bit 1=Shot                 1.raw
 * bit 2=Base hit             2.raw
 * bit 3=Invader hit          3.raw
 * bit 5=global enable?????
 *
 * Port 5:
 * bit 0=Fleet movement 1     4.raw
 * bit 1=Fleet movement 2     5.raw
 * bit 2=Fleet movement 3     6.raw
 * bit 3=Fleet movement 4     7.raw
 * bit 4=UFO 2                8.raw
 * but 5=screen flip		   n/a
 */

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;
import static WIP.mame056.vidhrdw._8080bw.*;
// refactor
import static arcadeflex036.osdepend.logerror;

public class z80bw
{
	
	/* output port 0x04 definitions - sound effect drive outputs */
	public static int OUT_PORT_4_UFO            = 0x01;
	public static int OUT_PORT_4_SHOT           = 0x02;
	public static int OUT_PORT_4_BASEHIT        = 0x04;
	public static int OUT_PORT_4_INVADERHIT     = 0x08;
	public static int OUT_PORT_4_ENABLE_SNDS    = 0x20;
	public static int OUT_PORT_4_UNUSED         = (~(0x2f));
	
	/* output port 0x05 definitions - sound effect drive outputs */
	public static int OUT_PORT_5_FLEET1         = 0x01;
	public static int OUT_PORT_5_FLEET2         = 0x02;
	public static int OUT_PORT_5_FLEET3         = 0x04;
	public static int OUT_PORT_5_FLEET4         = 0x08;
	public static int OUT_PORT_5_UFO2           = 0x10;
	public static int OUT_PORT_5_FLIP           = 0x20;
	public static int OUT_PORT_5_UNUSED         = 0xc0;
	
	
	static int astinvad_snds = 0;
	public static void PLAY(int id, int loop){
            if (astinvad_snds != 0) sample_start( id, id, loop );
        }
        
	public static void STOP(int id){
            sample_stop( id );
        }
	
	
	static String astinvad_sample_names[] =
	{
		"*invaders",
		"0.wav",
		"1.wav",
		"2.wav",
		"3.wav",
		"4.wav",
		"5.wav",
		"6.wav",
		"7.wav",
		"8.wav",
		null       /* end of array */
	};
	
	public static Samplesinterface astinvad_samples_interface = new Samplesinterface
	(
		9,	/* 9 channels */
		25,	/* volume */
		astinvad_sample_names
	);
	
	
	/* sample sound IDs - must match sample file name table above */
	public static int SND_UFO           = 0;
	public static int SND_SHOT          = 1;
	public static int SND_BASEHIT       = 2;
	public static int SND_INVADERHIT    = 3;
	public static int SND_FLEET1        = 4;
	public static int SND_FLEET2        = 5;
	public static int SND_FLEET3        = 6;
	public static int SND_FLEET4        = 7;
	public static int SND_UFO2          = 8;
	
        static int port4State;
	
	/* LT 20-3-1998 */
	public static WriteHandlerPtr astinvad_sh_port_4_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
		int bitsChanged;
		int bitsGoneHigh;
		int bitsGoneLow;
	
	
		bitsChanged  = port4State ^ data;
		bitsGoneHigh = bitsChanged & data;
		bitsGoneLow  = bitsChanged & ~data;
	
		port4State = data;
	
		if (( bitsGoneHigh & OUT_PORT_4_ENABLE_SNDS ) != 0) astinvad_snds = 1;
		if (( bitsGoneLow & OUT_PORT_4_ENABLE_SNDS ) != 0) astinvad_snds = 0;
	
		if (( bitsGoneHigh & OUT_PORT_4_UFO ) != 0)  PLAY( SND_UFO, 1 );
		if (( bitsGoneLow  & OUT_PORT_4_UFO ) != 0)  STOP( SND_UFO );
	
		if (( bitsGoneHigh & OUT_PORT_4_SHOT ) != 0)  PLAY( SND_SHOT, 0 );
		if (( bitsGoneLow  & OUT_PORT_4_SHOT ) != 0)  STOP( SND_SHOT );
	
		if (( bitsGoneHigh & OUT_PORT_4_BASEHIT ) != 0)
		{
			PLAY( SND_BASEHIT, 0 );
	    	/* turn all colours red here */
	    	invaders_screen_red_w(1);
	    }
		if (( bitsGoneLow & OUT_PORT_4_BASEHIT ) != 0)
		{
			STOP( SND_BASEHIT );
	    	/* restore colours here */
	    	invaders_screen_red_w(0);
	    }
	
		if (( bitsGoneHigh & OUT_PORT_4_INVADERHIT ) != 0)  PLAY( SND_INVADERHIT, 0 );
		if (( bitsGoneLow  & OUT_PORT_4_INVADERHIT ) != 0)  STOP( SND_INVADERHIT );
	
		if (( bitsChanged & OUT_PORT_4_UNUSED ) != 0) logerror("Snd Port 4 = %02X\n", data & OUT_PORT_4_UNUSED);
	} };
	
	static int port5State;
	
	public static WriteHandlerPtr astinvad_sh_port_5_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
		int bitsChanged;
		int bitsGoneHigh;
		int bitsGoneLow;
	
	
		bitsChanged  = port5State ^ data;
		bitsGoneHigh = bitsChanged & data;
		bitsGoneLow  = bitsChanged & ~data;
	
		port5State = data;
	
	
		if (( bitsGoneHigh & OUT_PORT_5_FLEET1 ) != 0)  PLAY( SND_FLEET1, 0 );
	
		if (( bitsGoneHigh & OUT_PORT_5_FLEET2 ) != 0)  PLAY( SND_FLEET2, 0 );
	
		if (( bitsGoneHigh & OUT_PORT_5_FLEET3 ) != 0)  PLAY( SND_FLEET3, 0 );
	
		if (( bitsGoneHigh & OUT_PORT_5_FLEET4 ) != 0)  PLAY( SND_FLEET4, 0 );
	
		if (( bitsGoneHigh & OUT_PORT_5_UFO2 ) != 0)  PLAY( SND_UFO2, 0 );
		if (( bitsGoneLow  & OUT_PORT_5_UFO2 ) != 0)  STOP( SND_UFO2 );
	
		if (( bitsChanged  & OUT_PORT_5_FLIP ) != 0)  invaders_flip_screen_w(data & 0x20);
	
		if (( bitsChanged  & OUT_PORT_5_UNUSED ) != 0) logerror("Snd Port 5 = %02X\n", data & OUT_PORT_5_UNUSED);
	} };
	
}
