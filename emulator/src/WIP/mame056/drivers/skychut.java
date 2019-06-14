/***************************************************************************
  ? (M10 m10 hardware)
  Sky Chuter By IREM
  Space Beam (M15 m15 hardware)
  Green Beret (?M15 ?m15 hardware)

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

  (c) 12/2/1998 Lee Taylor

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;

import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.inptportH.*;
import static mame056.inptport.*;
import static mame056.cpuexecH.*;
import static mame056.cpuexec.*;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.memoryH.*;
import static mame056.memory.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.inptport.*;
import static mame056.drawgfxH.*;
import static mame056.palette.*;
import static mame056.inputH.*;
import static mame056.sndintrfH.*;
import static mame056.sndintrf.*;
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;

// refactor
import static arcadeflex036.osdepend.logerror;

import static mame056.vidhrdw.generic.*;

import static arcadeflex056.fileio.*;
import static mame056.palette.game_palette;
import static mame056.inptport.*;

import static WIP.mame056.vidhrdw.skychut.*;
import static common.libc.cstring.*;

public class skychut
{
	
	static UBytePtr memory = new UBytePtr();
	
	static char palette[] = /* V.V */ /* Smoothed pure colors, overlays are not so contrasted */
	{
		0x00,0x00,0x00, /* BLACK */
		0xff,0x20,0x20, /* RED */
		0x20,0xff,0x20, /* GREEN */
		0xff,0xff,0x20, /* YELLOW */
		0x20,0xff,0xff, /* CYAN */
		0xff,0x20,0xff,  /* PURPLE */
		0xff,0xff,0xff /* WHITE */
	};
	
	static char spacebeam_palette[] = /* total estimation */
	{
		0xff,0xff,0xff, /* WHITE */
		0xff,0x20,0x20, /* RED */
		0x20,0xff,0x20, /* GREEN */
		0xff,0xff,0x20, /* YELLOW */
		0x20,0xff,0xff, /* CYAN */
		0xff,0x20,0xff,  /* PURPLE */
		0x00,0x00,0xf0, /* blue */
		0x00,0x00,0x00 /* BLACK */
	};
	
	static char colortable[] =
	{
		0,1,0,2,0,3,0,4,0,5,0,6
	};
	
	static VhConvertColorPromPtr init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] game_palette, char[] game_colortable, UBytePtr color_prom) {
                memcpy(game_palette,palette,palette.length);
		memcpy(game_colortable,colortable,colortable.length);
            }
        };
	
	static VhConvertColorPromPtr init_greenber_palette = new VhConvertColorPromPtr() {
            public void handler(char[] game_palette, char[] game_colortable, UBytePtr color_prom) {
		memcpy(game_palette,spacebeam_palette,spacebeam_palette.length);
            }
        };
	
	public static Memory_ReadAddress skychut_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x02ff, MRA_RAM ), /* scratch ram */
		new Memory_ReadAddress( 0x1000, 0x2fff, MRA_ROM ),
		new Memory_ReadAddress( 0x4000, 0x4400, MRA_RAM ),
		new Memory_ReadAddress( 0x4800, 0x4bff, MRA_RAM ), /* Foreground colour  */
		new Memory_ReadAddress( 0x5000, 0x53ff, MRA_RAM ), /* BKgrnd colour ??? */
		new Memory_ReadAddress( 0xa200, 0xa200, input_port_1_r ),
		new Memory_ReadAddress( 0xa300, 0xa300, input_port_0_r ),
	/*	new Memory_ReadAddress( 0xa700, 0xa700, input_port_2_r ), */
		new Memory_ReadAddress( 0xfC00, 0xffff, MRA_ROM ),	/* for the reset / interrupt vectors */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static Memory_WriteAddress skychut_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x02ff, MWA_RAM, memory ),
		new Memory_WriteAddress( 0x1000, 0x2fff, MWA_ROM ),
		new Memory_WriteAddress( 0x4000, 0x4400, videoram_w, videoram, videoram_size ),
		new Memory_WriteAddress( 0x4800, 0x4bff, skychut_colorram_w,colorram ), /* foreground colour  */
		new Memory_WriteAddress( 0x5000, 0x53ff, MWA_RAM, iremm15_chargen ), /* background ????? */
		new Memory_WriteAddress( 0xa100, 0xa1ff, MWA_RAM ), /* Sound writes????? */
		new Memory_WriteAddress( 0Xa400, 0xa400, skychut_vh_flipscreen_w ),
		new Memory_WriteAddress( 0xfc00, 0xffff, MWA_ROM ),	/* for the reset / interrupt vectors */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress greenberet_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x02ff, MRA_RAM ), /* scratch ram */
		new Memory_ReadAddress( 0x1000, 0x33ff, MRA_ROM ),
		new Memory_ReadAddress( 0x4000, 0x4400, MRA_RAM ),
		new Memory_ReadAddress( 0x4800, 0x4bff, MRA_RAM ), /* Foreground colour  */
		new Memory_ReadAddress( 0x5000, 0x57ff, MRA_RAM ),
		new Memory_ReadAddress( 0xa000, 0xa000, input_port_3_r ),
		new Memory_ReadAddress( 0xa200, 0xa200, input_port_1_r ),
		new Memory_ReadAddress( 0xa300, 0xa300, input_port_0_r ),
		new Memory_ReadAddress( 0xfC00, 0xffff, MRA_ROM ),	/* for the reset / interrupt vectors */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress greenberet_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x02ff, MWA_RAM, memory ),
		new Memory_WriteAddress( 0x1000, 0x33ff, MWA_ROM ),
		new Memory_WriteAddress( 0x4000, 0x4400, videoram_w, videoram, videoram_size ),
		new Memory_WriteAddress( 0x4800, 0x4bff, skychut_colorram_w,colorram ), /* foreground colour  */
		new Memory_WriteAddress( 0x5000, 0x57ff, MWA_RAM, iremm15_chargen ),
		new Memory_WriteAddress( 0xa100, 0xa1ff, MWA_RAM ), /* Sound writes????? */
		new Memory_WriteAddress( 0Xa400, 0xa400, skychut_vh_flipscreen_w ),
		new Memory_WriteAddress( 0xfc00, 0xffff, MWA_ROM ),	/* for the reset / interrupt vectors */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static InterruptPtr skychut_interrupt = new InterruptPtr() { public int handler() 
	{
		if ((readinputport(2) & 1) != 0)	/* Left Coin */
	            return nmi_interrupt.handler();
	        else
	            return interrupt.handler();
	} };
	
	static InputPortPtr input_ports_skychut = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_START1 );
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_START2);
		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_COCKTAIL);
		PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT  );
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT );
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_COCKTAIL );
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_COCKTAIL );
		PORT_START(); 	/* IN1 */
		PORT_DIPNAME(0x03, 0x00, DEF_STR( "Lives") );
		PORT_DIPSETTING (   0x00, "3" );
		PORT_DIPSETTING (   0x01, "4" );
		PORT_DIPSETTING (   0x02, "5" );
		PORT_START(); 	/* FAKE */
		PORT_BIT_IMPULSE( 0x01, IP_ACTIVE_HIGH, IPT_COIN1, 1 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_spacebeam = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_START1 );
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_START2);
		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT );
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT );
		PORT_START(); 	/* IN1 */
		PORT_DIPNAME(0x03, 0x01, DEF_STR( "Lives") );
		PORT_DIPSETTING (   0x00, "2" );
		PORT_DIPSETTING (   0x01, "3" );
		PORT_DIPSETTING (   0x02, "4" );
		PORT_DIPSETTING (   0x03, "5" );
		PORT_DIPNAME(0x08, 0x00, "?" );
		PORT_DIPSETTING (   0x00, DEF_STR("Off"));
		PORT_DIPSETTING (   0x08, DEF_STR("On"));
		PORT_DIPNAME(0x30, 0x10, DEF_STR("Coinage"));
		PORT_DIPSETTING (   0x00, "Testmode" );
		PORT_DIPSETTING (   0x10, "1 Coin 1 Play" );
		PORT_DIPSETTING (   0x20, "1 Coin 2 Plays" );
		PORT_START(); 	/* FAKE */
		PORT_BIT_IMPULSE( 0x01, IP_ACTIVE_HIGH, IPT_COIN1, 1 );
		PORT_START(); 	/* IN3 */
		PORT_BIT( 0x03, 0, IPT_UNUSED );
		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_BUTTON1|IPF_COCKTAIL );
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT |IPF_COCKTAIL );
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT|IPF_COCKTAIL );
	INPUT_PORTS_END(); }}; 
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,	/* 8*8 characters */
		256,	/* 256 characters */
		1,	/* 1 bits per pixel */
		new int[] { 0 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		new int[] { 7, 6, 5, 4, 3, 2, 1, 0 },
		8*8	/* every char takes 8 consecutive bytes */
	);
	
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0x0000, charlayout,   0, 7 ),	/* 4 color codes to support midframe */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	static MachineDriver machine_driver_skychut = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6502,
				20000000/8,
				skychut_readmem,skychut_writemem,null,null,
				skychut_interrupt,1
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,       /* frames per second, vblank duration */
		1,      /* single CPU, no need for interleaving */
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 2*8, 30*8-1 ),
		gfxdecodeinfo,
		palette.length / 3, colortable.length,
		init_palette,
	
		VIDEO_TYPE_RASTER|VIDEO_SUPPORTS_DIRTY,
		null,
		generic_vh_start,
		generic_vh_stop,
	    skychut_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
                null
	);
	
	static MachineDriver machine_driver_greenberet = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6502,
				20000000/8,
				greenberet_readmem,greenberet_writemem,null,null,
				skychut_interrupt,1
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,       /* frames per second, vblank duration */
		1,      /* single CPU, no need for interleaving */
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 2*8, 30*8-1 ),
		null,
		spacebeam_palette.length / 3,
		0,
		init_greenber_palette,
	
		VIDEO_TYPE_RASTER|VIDEO_SUPPORTS_DIRTY,
		null,
		generic_vh_start,
		generic_vh_stop,
	    iremm15_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
                null
	);
	
	
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_iremm10 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );
		ROM_LOAD( "b1r",  0x1000, 0x0400, 0xf9a7eb9b );// code, ok
		ROM_LOAD( "b2r",  0x1400, 0x0400, 0xaf11c1aa );// code ok
		ROM_LOAD( "b3r",  0x1800, 0x0400, 0xed49e481 );// code, ok
		ROM_LOAD( "b4r",  0x1c00, 0x0400, 0x6d5db95b );// code, vectors ok,
		ROM_RELOAD(       0xfc00, 0x0400 );/* for the reset and interrupt vectors */
		ROM_LOAD( "b5r",  0x2000, 0x0400, 0xeabba7aa );// graphic
		ROM_LOAD( "b6r",  0x2400, 0x0400, BADCRC( 0x9b7d6e77 )); // ?? bad dump
		ROM_LOAD( "b7r",  0x2800, 0x0400, 0x32045580 );// code, graphic ok,
	
		ROM_REGION( 0x0800, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "b9r",  0x0000, 0x0400, 0x56942cab );// ok
		ROM_LOAD( "b10r", 0x0400, 0x0400, 0xbe4b8585 );// ok
	ROM_END(); }}; 
	
	static RomLoadPtr rom_skychut = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "sc1d",  0x1000, 0x0400, 0x30b5ded1 );
		ROM_LOAD( "sc2d",  0x1400, 0x0400, 0xfd1f4b9e );
		ROM_LOAD( "sc3d",  0x1800, 0x0400, 0x67ed201e );
		ROM_LOAD( "sc4d",  0x1c00, 0x0400, 0x9b23a679 );
		ROM_RELOAD(        0xfc00, 0x0400 );/* for the reset and interrupt vectors */
		ROM_LOAD( "sc5a",  0x2000, 0x0400, 0x51d975e6 );
		ROM_LOAD( "sc6e",  0x2400, 0x0400, 0x617f302f );
		ROM_LOAD( "sc7",   0x2800, 0x0400, 0xdd4c8e1a );
		ROM_LOAD( "sc8d",  0x2c00, 0x0400, 0xaca8b798 );
	
		ROM_REGION( 0x0800, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "sc9d",  0x0000, 0x0400, 0x2101029e );
		ROM_LOAD( "sc10d", 0x0400, 0x0400, 0x2f81c70c );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_spacbeam = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );
		ROM_LOAD( "m1b", 0x1000, 0x0400, 0x5a1c3e0b );
		ROM_LOAD( "m2b", 0x1400, 0x0400, 0xa02bd9d7 );
		ROM_LOAD( "m3b", 0x1800, 0x0400, 0x78040843 );
		ROM_LOAD( "m4b", 0x1c00, 0x0400, 0x74705a44 );
		ROM_RELOAD(      0xfc00, 0x0400 );/* for the reset and interrupt vectors */
		ROM_LOAD( "m5b", 0x2000, 0x0400, 0xafdf1242 );
		ROM_LOAD( "m6b", 0x2400, 0x0400, 0x12afb0c2 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_greenber = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );
		ROM_LOAD( "gb1", 0x1000, 0x0400, 0x018ff672 );// ok
		ROM_LOAD( "gb2", 0x1400, 0x0400, 0xea8f2267 );// ok
		ROM_LOAD( "gb3", 0x1800, 0x0400, 0x8f337920 );// ok
		ROM_LOAD( "gb4", 0x1c00, 0x0400, 0x7eeac4eb );// ok
		ROM_RELOAD(      0xfc00, 0x0400 );/* for the reset and interrupt vectors */
		ROM_LOAD( "gb5", 0x2000, 0x0400, 0xb2f8e69a );
		ROM_LOAD( "gb6", 0x2400, 0x0400, 0x50ea8bd3 );
		ROM_LOAD( "gb7", 0x2800, 0x0400, 0x00000000 );// 2be8 entry
		ROM_LOAD( "gb8", 0x2c00, 0x0400, 0x34700b31 );
		ROM_LOAD( "gb9", 0x3000, 0x0400, 0xc27b9ba3 );// ok ?
	ROM_END(); }}; 
	
	
	
	public static GameDriver driver_iremm10	   = new GameDriver("19??"	,"iremm10"	,"skychut.java"	,rom_iremm10,null	,machine_driver_skychut	,input_ports_skychut	,null	,ROT0	,	"Irem", "? (irem m10 hardware)", GAME_NO_SOUND | GAME_WRONG_COLORS | GAME_NOT_WORKING );
	public static GameDriver driver_skychut	   = new GameDriver("1980"	,"skychut"	,"skychut.java"	,rom_skychut,null	,machine_driver_skychut	,input_ports_skychut	,null	,ROT0	,	"Irem", "Sky Chuter", GAME_NO_SOUND | GAME_WRONG_COLORS );
	public static GameDriver driver_spacbeam	   = new GameDriver("19??"	,"spacbeam"	,"skychut.java"	,rom_spacbeam,null	,machine_driver_greenberet	,input_ports_spacebeam	,null	,ROT0	,	"Irem", "Space Beam", GAME_NO_SOUND | GAME_WRONG_COLORS );
	public static GameDriver driver_greenber	   = new GameDriver("1980"	,"greenber"	,"skychut.java"	,rom_greenber,null	,machine_driver_greenberet	,input_ports_spacebeam	,null	,ROT0	,	"Irem", "Green Beret (IREM)", GAME_NO_SOUND | GAME_WRONG_COLORS | GAME_NOT_WORKING );
}
