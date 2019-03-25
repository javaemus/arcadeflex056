/*
 *	Pulsar sound routines
 *
 *	TODO: change heart rate based on bit 7 of Port 1
 *
 */

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import static mame056.sound.samples.*;

public class pulsar
{
	
	
	/* output port 0x01 definitions - sound effect drive outputs */
	public static int OUT_PORT_1_CLANG		= 0x01;
	public static int OUT_PORT_1_KEY		= 0x02;
	public static int OUT_PORT_1_ALIENHIT		= 0x04;
	public static int OUT_PORT_1_PHIT		= 0x08;
	public static int OUT_PORT_1_ASHOOT		= 0x10;
	public static int OUT_PORT_1_PSHOOT		= 0x20;
	public static int OUT_PORT_1_BONUS		= 0x40;
	public static int OUT_PORT_1_HBEAT_RATE         = 0x80;	/* currently not used */
	
	/* output port 0x02 definitions - sound effect drive outputs */
        public static int OUT_PORT_2_SIZZLE		= 0x01;
	public static int OUT_PORT_2_GATE		= 0x02;
	public static int OUT_PORT_2_BIRTH		= 0x04;
	public static int OUT_PORT_2_HBEAT		= 0x08;
	public static int OUT_PORT_2_MOVMAZE		= 0x10;
	
	
	public static void PLAY(int id, int loop){
            sample_start( id, id, loop );
        }
        
	public static void STOP(int id){
            sample_stop( id );
        }
	
	
	/* sample file names */
	public static String pulsar_sample_names[] =
	{
		"*pulsar",
		"clang.wav",
		"key.wav",
		"alienhit.wav",
		"phit.wav",
		"ashoot.wav",
		"pshoot.wav",
		"bonus.wav",
		"sizzle.wav",
		"gate.wav",
		"birth.wav",
		"hbeat.wav",
		"movmaze.wav",
		null
	};
	
	/* sample sound IDs - must match sample file name table above */
	//enum
	//{
		public static int SND_CLANG     = 0;
		public static int SND_KEY       = 1;
		public static int SND_ALIENHIT  = 2;
		public static int SND_PHIT      = 3;
		public static int SND_ASHOOT    = 4;
		public static int SND_PSHOOT    = 5;
		public static int SND_BONUS     = 6;
		public static int SND_SIZZLE    = 7;
		public static int SND_GATE      = 8;
		public static int SND_BIRTH     = 9;
		public static int SND_HBEAT     = 10;
		public static int SND_MOVMAZE   = 11;
	//};
	
	
	static int port1State = 0;
	
	public static WriteHandlerPtr pulsar_sh_port1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int bitsChanged;
		int bitsGoneHigh;
		int bitsGoneLow;
	
	
		bitsChanged  = port1State ^ data;
		bitsGoneHigh = bitsChanged & data;
		bitsGoneLow  = bitsChanged & ~data;
	
		port1State = data;
	
		if (( bitsGoneLow & OUT_PORT_1_CLANG ) != 0)
		{
			PLAY( SND_CLANG, 0 );
		}
	
		if (( bitsGoneLow & OUT_PORT_1_KEY ) != 0)
		{
			PLAY( SND_KEY, 0 );
		}
	
		if (( bitsGoneLow & OUT_PORT_1_ALIENHIT ) != 0)
		{
			PLAY( SND_ALIENHIT, 0 );
		}
	
		if (( bitsGoneLow & OUT_PORT_1_PHIT ) != 0)
		{
			PLAY( SND_PHIT, 0 );
		}
	
		if (( bitsGoneLow & OUT_PORT_1_ASHOOT ) != 0)
		{
			PLAY( SND_ASHOOT, 0 );
		}
	
		if (( bitsGoneLow & OUT_PORT_1_PSHOOT ) != 0)
		{
			PLAY( SND_PSHOOT, 0 );
		}
	
		if (( bitsGoneLow & OUT_PORT_1_BONUS ) != 0)
		{
			PLAY( SND_BONUS, 0 );
		}
	} };
	
	
	public static WriteHandlerPtr pulsar_sh_port2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int port2State = 0;
		int bitsChanged;
		int bitsGoneHigh;
		int bitsGoneLow;
	
	
		bitsChanged  = port2State ^ data;
		bitsGoneHigh = bitsChanged & data;
		bitsGoneLow  = bitsChanged & ~data;
	
		port2State = data;
	
		if (( bitsGoneLow & OUT_PORT_2_SIZZLE ) != 0)
		{
			PLAY( SND_SIZZLE, 0 );
		}
	
		if (( bitsGoneLow & OUT_PORT_2_GATE ) != 0)
		{
			sample_start( SND_CLANG, SND_GATE, 0 );
		}
		if (( bitsGoneHigh & OUT_PORT_2_GATE ) != 0)
		{
			STOP( SND_CLANG );
		}
	
		if (( bitsGoneLow & OUT_PORT_2_BIRTH ) != 0)
		{
			PLAY( SND_BIRTH, 0 );
		}
	
		if (( bitsGoneLow & OUT_PORT_2_HBEAT ) != 0)
		{
			PLAY( SND_HBEAT, 1 );
		}
		if (( bitsGoneHigh & OUT_PORT_2_HBEAT ) != 0)
		{
			STOP( SND_HBEAT );
		}
	
		if (( bitsGoneLow & OUT_PORT_2_MOVMAZE ) != 0)
		{
			PLAY( SND_MOVMAZE, 1 );
		}
		if (( bitsGoneHigh & OUT_PORT_2_MOVMAZE ) != 0)
		{
			STOP( SND_MOVMAZE );
		}
	} };
}
