/****************************************************************************

	Gotya / The Hand driver by Zsolt Vasvari


TODO: Emulated sound

	  Hitachi HD38880BP
         	  HD38882PA06

	  I think HD38880 is a CPU/MCU, because the game just sends it a sound command (0-0x1a)

****************************************************************************/

/****************************************************************************
 About GotYa (from the board owner)

 I believe it is a prototype for several reasons.
 There were quite a few jumpers on the board, hand written labels with
 the dates on them. I also have the manual, the game name is clearly Got-Ya
 and is a Game-A-Tron game.  The game itself had a few flyers from GAT inside
 so I have a hard time believing it was a bootleg.

----

 so despite the fact that 'gotya' might look like its a bootleg of thehand,
 its more likely just a prototype / alternate version, its hard to tell
****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static WIP.mame056.vidhrdw.gotya.*;
import static WIP.mame056.sndhrdw.gotya.*;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.libc.cstring.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.inptportH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.memoryH.*;
import static mame056.inptport.*;
import static mame056.drawgfxH.*;
import static mame056.inputH.*;
import static mame056.sndintrfH.*;
import static mame056.sndintrf.*;
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;
import static mame056.sound.mixer.*;
import static mame056.sound.mixerH.*;
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;
import static mame056.sound.ay8910.*;
import static mame056.sound.ay8910H.*;
import static mame056.sound.pokey.*;
import static mame056.sound.pokeyH.*;

import static mame056.vidhrdw.generic.*;
import static mame056.timer.*;
import static mame056.timerH.*;

import static arcadeflex056.fileio.*;
import static mame056.palette.*;
// refactor
import static arcadeflex036.osdepend.logerror;

public class gotya
{
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x3fff, MRA_ROM ),
		new Memory_ReadAddress( 0x5000, 0x5fff, MRA_RAM ),
		new Memory_ReadAddress( 0x6000, 0x6000, input_port_0_r ),
		new Memory_ReadAddress( 0x6001, 0x6001, input_port_1_r ),
		new Memory_ReadAddress( 0x6002, 0x6002, input_port_2_r ),
		new Memory_ReadAddress( 0xc000, 0xd3ff, MRA_RAM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x3fff, MWA_ROM ),
		new Memory_WriteAddress( 0x5000, 0x5fff, MWA_RAM ),
		new Memory_WriteAddress( 0x6004, 0x6004, gotya_video_control_w ),
		new Memory_WriteAddress( 0x6005, 0x6005, gotya_soundlatch_w ),
		new Memory_WriteAddress( 0x6006, 0x6006, MWA_RAM, gotya_scroll ),
		new Memory_WriteAddress( 0x6007, 0x6007, watchdog_reset_w ),
		new Memory_WriteAddress( 0xc000, 0xc7ff, videoram_w, videoram, videoram_size ),
		new Memory_WriteAddress( 0xc800, 0xcfff, colorram_w, colorram ),
		new Memory_WriteAddress( 0xd000, 0xd3df, MWA_RAM, gotya_foregroundram ),
		new Memory_WriteAddress( 0xd3e0, 0xd3ff, MWA_RAM, spriteram ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	static InputPortPtr input_ports_gotya = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_PLAYER1 );
		PORT_SERVICE( 0x10, IP_ACTIVE_LOW );
		PORT_BITX(0x20, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER1, "P1 Paper", IP_KEY_DEFAULT, IP_JOY_DEFAULT );
		PORT_BITX(0x40, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1, "P1 Scissors", IP_KEY_DEFAULT, IP_JOY_DEFAULT );
		PORT_BITX(0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1, "P1 Rock", IP_KEY_DEFAULT, IP_JOY_DEFAULT );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_PLAYER2 );
		PORT_DIPNAME( 0x10, 0x10, "Sound Test" );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BITX(0x20, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2, "P2 Paper", IP_KEY_DEFAULT, IP_JOY_DEFAULT );
		PORT_BITX(0x40, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2, "P2 Scissors", IP_KEY_DEFAULT, IP_JOY_DEFAULT );
		PORT_BITX(0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2, "P2 Rock", IP_KEY_DEFAULT, IP_JOY_DEFAULT );
	
		PORT_START(); 	/* DSW1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_DIPNAME( 0x02, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Cocktail") );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START2 );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x00, "Easy" );
		PORT_DIPSETTING(    0x10, "Hard" );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x00, "None" );
		PORT_DIPSETTING(    0x20, "15000" );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "3" );
		PORT_DIPSETTING(    0x40, "5" );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
	INPUT_PORTS_END(); }}; 
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,	/* 8*8 characters */
		256,	/* 256 characters */
		2,	    /* 2 bits per pixel */
		new int[] { 0, 4 },	/* the bitplanes are packed in one byte */
		new int[] { 0, 1, 2, 3, 8*8+0, 8*8+1, 8*8+2, 8*8+3 },
		new int[] { 7*8, 6*8, 5*8, 4*8, 3*8, 2*8, 1*8, 0*8 },
		16*8	/* every char takes 16 consecutive bytes */
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,	/* 16*16 characters */
		64,		/* 64 characters */
		2,	    /* 2 bits per pixel */
		new int[] { 0, 4 },	/* the bitplanes are packed in one byte */
		new int[] { 0, 1, 2, 3, 24*8+0, 24*8+1, 24*8+2, 24*8+3,
		  16*8+0, 16*8+1, 16*8+2, 16*8+3, 8*8+0, 8*8+1, 8*8+2, 8*8+3 },
		new int[] { 39*8, 38*8, 37*8, 36*8, 35*8, 34*8, 33*8, 32*8,
		   7*8,  6*8,  5*8,  4*8,  3*8,  2*8,  1*8,  0*8 },
		64*8	/* every char takes 64 consecutive bytes */
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,   0, 16 ),
		new GfxDecodeInfo( REGION_GFX2, 0, spritelayout, 0, 16 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	static String sample_names[] =
	{												// Address triggered at
		"*thehand",
		"01.wav",	/* game start tune */			// 075f
		"02.wav",	/* coin in */					// 0074
		"03.wav",	/* eat dot */					// 0e45
		"05.wav",	/* eat dollar sign */			// 0e45
	
		"06.wav",	/* door open */					// 19e1
		"07.wav",	/* door close */				// 1965
	
		"08.wav",	/* theme song */				// 0821
		//"09.wav"									// 1569
	
		/* one of these two is played after eating the last dot */
		"0a.wav",	/* piccolo */					// 17af
		"0b.wav",	/* tune */						// 17af
	
		//"0f.wav"									// 08ee
		"10.wav",	/* 'We're even. Bye Bye!' */	// 162a
		"11.wav",	/* 'You got me!' */				// 1657
		"12.wav",	/* 'You have lost out' */		// 085e
	
		"13.wav",	/* 'Rock' */					// 14de
		"14.wav",	/* 'Scissors' */				// 14f3
		"15.wav",	/* 'Paper' */					// 1508
	
		/* one of these is played when going by the girl between levels */
		"16.wav",	/* 'Very good!' */				// 194a
		"17.wav",	/* 'Wonderful!' */				// 194a
		"18.wav",	/* 'Come on!' */				// 194a
		"19.wav",	/* 'I love you!' */				// 194a
		"1a.wav",	/* 'See you again!' */			// 194a
		null            /* end of array */
	};
	
	static Samplesinterface samples_interface = new Samplesinterface
        (
		4,	/* 4 channels */
		50,	/* volume */
		sample_names
        );
	
	
	static MachineDriver machine_driver_gotya = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				18432000/6,	/* 3.072 MHz ??? */
				readmem,writemem,null,null,
				interrupt,1
			)
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,
		1,
		null,
	
		/* video hardware */
		36*8, 32*8, new rectangle( 0, 36*8-1, 2*8, 30*8-1 ),
		gfxdecodeinfo,
		8, 16*4,
		gotya_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		gotya_vh_start,
		generic_vh_stop,
		gotya_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_SAMPLES,
				samples_interface
			)
		}
	);
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_thehand = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );    /* 64k for main CPU */
		ROM_LOAD( "hand6.bin",	0x0000, 0x1000, 0xa33b806c );
		ROM_LOAD( "hand5.bin",	0x1000, 0x1000, 0x89bcde82 );
		ROM_LOAD( "hand4.bin",	0x2000, 0x1000, 0xc6844a83 );
		ROM_LOAD( "gb-03.bin",	0x3000, 0x1000, 0xf34d90ab );
	
		ROM_REGION( 0x1000,  REGION_GFX1, ROMREGION_DISPOSE );/* characters */
		ROM_LOAD( "hand12.bin",	0x0000, 0x1000, 0x95773b46 );
	
		ROM_REGION( 0x1000,  REGION_GFX2, ROMREGION_DISPOSE );/* sprites */
		ROM_LOAD( "gb-11.bin",	0x0000, 0x1000, 0x5d5eca1b );
	
		ROM_REGION( 0x0120,  REGION_PROMS, 0 );
		ROM_LOAD( "prom.1a",    0x0000, 0x0020, 0x4864a5a0 );   /* color PROM */
		ROM_LOAD( "prom.4c",    0x0020, 0x0100, 0x4745b5f6 );   /* lookup table */
	
		ROM_REGION( 0x1000,  REGION_USER1, 0 );	/* no idea what these are */
		ROM_LOAD( "hand1.bin",	0x0000, 0x0800, 0xccc537e0 );
		ROM_LOAD( "gb-02.bin",	0x0800, 0x0800, 0x65a7e284 );
	
		ROM_REGION( 0x4000,  REGION_USER2, 0 );	/* HD38880 code? */
		ROM_LOAD( "gb-10.bin",	0x0000, 0x1000, 0x8101915f );
		ROM_LOAD( "gb-09.bin",	0x1000, 0x1000, 0x619bba76 );
		ROM_LOAD( "gb-08.bin",	0x2000, 0x1000, 0x82f59528 );
		ROM_LOAD( "hand7.bin",	0x3000, 0x1000, 0xfbf1c5de );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_gotya = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );    /* 64k for main CPU */
		ROM_LOAD( "gb-06.bin",	0x0000, 0x1000, 0x7793985a );
		ROM_LOAD( "gb-05.bin",	0x1000, 0x1000, 0x683d188b );
		ROM_LOAD( "gb-04.bin",	0x2000, 0x1000, 0x15b72f09 );
		ROM_LOAD( "gb-03.bin",	0x3000, 0x1000, 0xf34d90ab );   /* this is the only ROM that passes the ROM test */
	
		ROM_REGION( 0x1000,  REGION_GFX1, ROMREGION_DISPOSE );/* characters */
		ROM_LOAD( "gb-12.bin",	0x0000, 0x1000, 0x4993d735 );
	
		ROM_REGION( 0x1000,  REGION_GFX2, ROMREGION_DISPOSE );/* sprites */
		ROM_LOAD( "gb-11.bin",	0x0000, 0x1000, 0x5d5eca1b );
	
		ROM_REGION( 0x0120,  REGION_PROMS, 0 );
		ROM_LOAD( "prom.1a",    0x0000, 0x0020, 0x4864a5a0 );   /* color PROM */
		ROM_LOAD( "prom.4c",    0x0020, 0x0100, 0x4745b5f6 );   /* lookup table */
	
		ROM_REGION( 0x1000,  REGION_USER1, 0 );	/* no idea what these are */
		ROM_LOAD( "gb-01.bin",	0x0000, 0x0800, 0xc31dba64 );
		ROM_LOAD( "gb-02.bin",	0x0800, 0x0800, 0x65a7e284 );
	
		ROM_REGION( 0x4000,  REGION_USER2, 0 );	/* HD38880 code? */
		ROM_LOAD( "gb-10.bin",	0x0000, 0x1000, 0x8101915f );
		ROM_LOAD( "gb-09.bin",	0x1000, 0x1000, 0x619bba76 );
		ROM_LOAD( "gb-08.bin",	0x2000, 0x1000, 0x82f59528 );
		ROM_LOAD( "gb-07.bin",	0x3000, 0x1000, 0x92a9f8bf );
	ROM_END(); }}; 
	
	public static GameDriver driver_thehand	   = new GameDriver("1981"	,"thehand"	,"gotya.java"	,rom_thehand,null	,machine_driver_gotya	,input_ports_gotya	,null	,ROT270	,	"T.I.C."     , "The Hand" );
	public static GameDriver driver_gotya	   = new GameDriver("1981"	,"gotya"	,"gotya.java"	,rom_gotya,driver_thehand	,machine_driver_gotya	,input_ports_gotya	,null	,ROT270	,	"Game-A-Tron", "Got-Ya (12/24/1981, prototype?)" );
}
