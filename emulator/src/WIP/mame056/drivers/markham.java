/*****************************************************************************

Markham (c) 1983 Sun Electronics

	Driver by Uki

	17/Jun/2001 -

*****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static WIP.mame056.vidhrdw.markham.*;

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
import static mame056.vidhrdw.generic.*;

import static arcadeflex056.fileio.*;
import static mame056.palette.*;
import static mame056.sound.sn76496.*;
import static mame056.sound.sn76496H.*;

public class markham
{
	
	public static UBytePtr markham_sharedram = new UBytePtr();
	
	/****************************************************************************/
	
	
	public static WriteHandlerPtr markham_sharedram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		markham_sharedram.write(offset, data);
	} };
	
	public static ReadHandlerPtr markham_sharedram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return markham_sharedram.read(offset);
	} };
	
	public static ReadHandlerPtr markham_e004_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return 0;
	} };
	
	/****************************************************************************/
	
	public static Memory_ReadAddress readmem1[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x5fff, MRA_ROM ),
	
		new Memory_ReadAddress( 0xc000, 0xc7ff, MRA_RAM ),
		new Memory_ReadAddress( 0xc800, 0xcfff, spriteram_r ),
		new Memory_ReadAddress( 0xd000, 0xd7ff, MRA_RAM ),
		new Memory_ReadAddress( 0xd800, 0xdfff, markham_sharedram_r ),
	
		new Memory_ReadAddress( 0xe000, 0xe000, input_port_1_r ), /* dsw 1 */
		new Memory_ReadAddress( 0xe001, 0xe001, input_port_0_r ), /* dsw 2 */
		new Memory_ReadAddress( 0xe002, 0xe002, input_port_2_r ), /* player1 */
		new Memory_ReadAddress( 0xe003, 0xe003, input_port_3_r ), /* player2 */
	
		new Memory_ReadAddress( 0xe004, 0xe004, markham_e004_r ), /* from CPU2 busack */
	
		new Memory_ReadAddress( 0xe005, 0xe005, input_port_4_r ), /* other inputs */
	
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem1[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x5fff, MWA_ROM ),
	
		new Memory_WriteAddress( 0xc000, 0xc7ff, MWA_RAM ),
		new Memory_WriteAddress( 0xc800, 0xcfff, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0xd000, 0xd7ff, videoram_w, videoram, videoram_size ),
		new Memory_WriteAddress( 0xd800, 0xdfff, markham_sharedram_w ),
	
		new Memory_WriteAddress( 0xe008, 0xe008, MWA_NOP ), /* coin counter? */
	
		new Memory_WriteAddress( 0xe009, 0xe009, MWA_NOP ), /* to CPU2 busreq */
	
		new Memory_WriteAddress( 0xe00c, 0xe00d, markham_scroll_x_w ),
		new Memory_WriteAddress( 0xe00e, 0xe00e, markham_flipscreen_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress readmem2[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x5fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem2[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x5fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM, markham_sharedram ),
	
		new Memory_WriteAddress( 0xc000, 0xc000, SN76496_0_w ),
		new Memory_WriteAddress( 0xc001, 0xc001, SN76496_1_w ),
	
		new Memory_WriteAddress( 0xc002, 0xc002, MWA_NOP ), /* unknown */
		new Memory_WriteAddress( 0xc003, 0xc003, MWA_NOP ), /* unknown */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	/****************************************************************************/
	
	static InputPortPtr input_ports_markham = new InputPortPtr(){ public void handler() { 
		PORT_START();   /* dsw1 */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "3" );
		PORT_DIPSETTING(    0x01, "5" );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "Unknown 1-4" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0xf0, 0x00, "Coin1 / Coin2" );
		PORT_DIPSETTING(    0x00, "1C 1C / 1C 1C" );
		PORT_DIPSETTING(    0x10, "2C 1C / 2C 1C" );
		PORT_DIPSETTING(    0x20, "2C 1C / 1C 3C" );
		PORT_DIPSETTING(    0x30, "1C 1C / 1C 2C" );
		PORT_DIPSETTING(    0x40, "1C 1C / 1C 3C" );
		PORT_DIPSETTING(    0x50, "1C 1C / 1C 4C" );
		PORT_DIPSETTING(    0x60, "1C 1C / 1C 5C" );
		PORT_DIPSETTING(    0x70, "1C 1C / 1C 6C" );
		PORT_DIPSETTING(    0x80, "1C 2C / 1C 2C" );
		PORT_DIPSETTING(    0x90, "1C 2C / 1C 4C" );
		PORT_DIPSETTING(    0xa0, "1C 2C / 1C 5C" );
		PORT_DIPSETTING(    0xb0, "1C 2C / 1C 10C" );
		PORT_DIPSETTING(    0xc0, "1C 2C / 1C 11C" );
		PORT_DIPSETTING(    0xd0, "1C 2C / 1C 12C" );
		PORT_DIPSETTING(    0xe0, "1C 2C / 1C 6C" );
		PORT_DIPSETTING(    0xf0, DEF_STR( "Free_Play") );
	
		PORT_START();   /* dsw2 */
		PORT_DIPNAME( 0x03, 0x02, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x00, "None" );
		PORT_DIPSETTING(    0x01, "20000" );
		PORT_DIPSETTING(    0x02, "20000, Every 50000" );
		PORT_DIPSETTING(    0x03, "20000, Every 80000" );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "Unknown 2-4" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, "Unknown 2-5" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, "Unknown 2-6" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "Unknown 2-7" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "Freeze" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		PORT_START();  /* e002 */
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_BUTTON2 );
		PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_8WAY );
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_8WAY );
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_8WAY );
	
		PORT_START();  /* e003 */
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_COCKTAIL );
		PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
	
		PORT_START();  /* e005 */
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_SERVICE1 );
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		PORT_SERVICE( 0x10, IP_ACTIVE_HIGH );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_START2 );
		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_START1 );
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_COIN2 );
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_COIN1 );
	INPUT_PORTS_END(); }}; 
	
	
	/****************************************************************************/
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,    /* 8*8 characters */
		1024,   /* 1024 characters */
		3,      /* 3 bits per pixel */
		new int[] {0,8192*8,8192*8*2},
		new int[] {7,6,5,4,3,2,1,0},
		new int[] {8*0, 8*1, 8*2, 8*3, 8*4, 8*5, 8*6, 8*7},
		8*8
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,  /* 16*16 characters */
		256,    /* 256 characters */
		3,      /* 3 bits per pixel */
		new int[] {8192*8*2,8192*8,0},
		new int[] {7,6,5,4,3,2,1,0,
			8*16+7,8*16+6,8*16+5,8*16+4,8*16+3,8*16+2,8*16+1,8*16+0},
		new int[] {8*0, 8*1, 8*2, 8*3, 8*4, 8*5, 8*6, 8*7,
			8*8,8*9,8*10,8*11,8*12,8*13,8*14,8*15},
		8*8*4
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX2, 0x0000, charlayout,   512, 64 ),
		new GfxDecodeInfo( REGION_GFX1, 0x0000, spritelayout, 0,   64 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	static SN76496interface sn76496_interface = new SN76496interface
	(
		2,	/* 2 chips */
		new int[] { 8000000/2, 8000000/2 },
		new int[] { 75, 75 }
	);
	
	static MachineDriver machine_driver_markham = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				8000000/2, /* 4.000MHz */
				readmem1,writemem1,null,null,
				interrupt,1
			),
			new MachineCPU(
				CPU_Z80,
				8000000/2, /* 4.000MHz */
				readmem2,writemem2,null,null,
				interrupt,1
			),
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,
		100,
		null,
	
		32*8, 32*8, new rectangle( 1*8, 31*8-1, 2*8, 30*8-1 ),
	
		gfxdecodeinfo,
		256,1024,
		markham_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		generic_vh_start,
		generic_vh_stop,
		markham_vh_screenrefresh,
	
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_SN76496,
				sn76496_interface
			)
		}
	);
	
	/****************************************************************************/
	
	static RomLoadPtr rom_markham = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* main CPU */
		ROM_LOAD( "tv3.9",   0x0000,  0x2000, 0x59391637 );
		ROM_LOAD( "tvg4.10", 0x2000,  0x2000, 0x1837bcce );
		ROM_LOAD( "tvg5.11", 0x4000,  0x2000, 0x651da602 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* sub CPU */
		ROM_LOAD( "tvg1.5",  0x0000,  0x2000, 0xc5299766 );
		ROM_LOAD( "tvg2.6",  0x4000,  0x2000, 0xb216300a );
	
		ROM_REGION( 0x6000, REGION_GFX1, ROMREGION_DISPOSE );/* sprite */
		ROM_LOAD( "tvg6.84", 0x0000,  0x2000, 0xab933ae5 );
		ROM_LOAD( "tvg7.85", 0x2000,  0x2000, 0xce8edda7 );
		ROM_LOAD( "tvg8.86", 0x4000,  0x2000, 0x74d1536a );
	
		ROM_REGION( 0x6000, REGION_GFX2, ROMREGION_DISPOSE );/* bg */
		ROM_LOAD( "tvg9.87",  0x0000,  0x2000, 0x42168675 );
		ROM_LOAD( "tvg10.88", 0x2000,  0x2000, 0xfa9feb67 );
		ROM_LOAD( "tvg11.89", 0x4000,  0x2000, 0x71f3dd49 );
	
		ROM_REGION( 0x0700, REGION_PROMS, 0 );/* color PROMs */
		ROM_LOAD( "14-3.99",  0x0000,  0x0100, 0x89d09126 );/* R */
		ROM_LOAD( "14-4.100", 0x0100,  0x0100, 0xe1cafe6c );/* G */
		ROM_LOAD( "14-5.101", 0x0200,  0x0100, 0x2d444fa6 );/* B */
		ROM_LOAD( "14-1.61",  0x0300,  0x0200, 0x3ad8306d );/* sprite */
		ROM_LOAD( "14-2.115", 0x0500,  0x0200, 0x12a4f1ff );/* bg */
	ROM_END(); }}; 
	
	
	public static GameDriver driver_markham	   = new GameDriver("1983"	,"markham"	,"markham.java"	,rom_markham,null	,machine_driver_markham	,input_ports_markham	,null	,ROT0	,	"Sun Electronics", "Markham" );
}
