/*****************************************************************************

Raiders5 (c) 1985 Taito / UPL

	Driver by Uki

	02/Jun/2001 -

******************************************************************************

Thanks to:

David Haywood for ninjakid driver.

Howie Cohen
Frank Palazzolo
Alex Pasadyn
	for nova2001 driver.

Notes:

"Free Play" does not work properly...?

*****************************************************************************/

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
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;

import static mame056.vidhrdw.generic.*;

import static arcadeflex056.fileio.*;
import static mame056.palette.game_palette;
import static mame056.inptport.*;

import static WIP.mame056.vidhrdw.raiders5.*;
import static mame056.palette.*;
import static mame056.sound.ay8910.*;
import static mame056.sound.ay8910H.*;

public class raiders5
{
	static UBytePtr raiders5_shared_workram = new UBytePtr();
        
	public static WriteHandlerPtr raiders5_shared_workram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		raiders5_shared_workram.write(offset, data);
	} };
	
	public static ReadHandlerPtr raiders5_shared_workram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return raiders5_shared_workram.read(offset);
	} };
	
	/****************************************************************************/
	
	public static Memory_ReadAddress readmem1[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
	
		new Memory_ReadAddress( 0x8000, 0x87ff, spriteram_r ),
		new Memory_ReadAddress( 0x8800, 0x8fff, raiders5_fgram_r ),
		new Memory_ReadAddress( 0x9000, 0x97ff, raiders5_videoram_r ),
	
		new Memory_ReadAddress( 0xc001, 0xc001, AY8910_read_port_0_r ),
		new Memory_ReadAddress( 0xc003, 0xc003, AY8910_read_port_1_r ),
	
		new Memory_ReadAddress( 0xd000, 0xd1ff, paletteram_r ),
	
		new Memory_ReadAddress( 0xe000, 0xe7ff, MRA_RAM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress writemem1[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
	
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0x8800, 0x8fff, raiders5_fgram_w, raiders5_fgram, raiders5_fgram_size ),
		new Memory_WriteAddress( 0x9000, 0x97ff, raiders5_videoram_w, videoram, videoram_size ),
	
		new Memory_WriteAddress( 0xa000, 0xa000, raiders5_scroll_x_w ),
		new Memory_WriteAddress( 0xa001, 0xa001, raiders5_scroll_y_w ),
		new Memory_WriteAddress( 0xa002, 0xa002, raiders5_flipscreen_w ),
	
		new Memory_WriteAddress( 0xc000, 0xc000, AY8910_control_port_0_w ),
		new Memory_WriteAddress( 0xc001, 0xc001, AY8910_write_port_0_w ),
		new Memory_WriteAddress( 0xc002, 0xc002, AY8910_control_port_1_w ),
		new Memory_WriteAddress( 0xc003, 0xc003, AY8910_write_port_1_w ),
	
		new Memory_WriteAddress( 0xd000, 0xd1ff, raiders5_paletteram_w, paletteram ),
	
		new Memory_WriteAddress( 0xe000, 0xe7ff, MWA_RAM, raiders5_shared_workram ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	public static IO_ReadPort  readport1[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x00, 0x00, IORP_NOP ), /* watchdog? */
                new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress readmem2[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x3fff, MRA_ROM ),
	
		new Memory_ReadAddress( 0x8001, 0x8001, AY8910_read_port_0_r ),
		new Memory_ReadAddress( 0x8003, 0x8003, AY8910_read_port_1_r ),
	
		new Memory_ReadAddress( 0x9000, 0x9000, MRA_NOP ), /* unknown */
	
		new Memory_ReadAddress( 0xa000, 0xa7ff, raiders5_shared_workram_r ),
	
		new Memory_ReadAddress( 0xc000, 0xc000, MRA_NOP ), /* unknown */
		new Memory_ReadAddress( 0xc800, 0xc800, MRA_NOP ), /* unknown */
		new Memory_ReadAddress( 0xd000, 0xd000, MRA_NOP ), /* unknown */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress writemem2[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x3fff, MWA_ROM ),
	
		new Memory_WriteAddress( 0x8000, 0x8000, AY8910_control_port_0_w ),
		new Memory_WriteAddress( 0x8001, 0x8001, AY8910_write_port_0_w ),
		new Memory_WriteAddress( 0x8002, 0x8002, AY8910_control_port_1_w ),
		new Memory_WriteAddress( 0x8003, 0x8003, AY8910_write_port_1_w ),
	
		new Memory_WriteAddress( 0xa000, 0xa7ff, raiders5_shared_workram_w ),
	
		new Memory_WriteAddress( 0xe000, 0xe000, raiders5_scroll_x_w ),
		new Memory_WriteAddress( 0xe001, 0xe001, raiders5_scroll_y_w ),
		new Memory_WriteAddress( 0xe002, 0xe002, raiders5_flipscreen_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	/****************************************************************************/
	
	static InputPortPtr input_ports_raiders5 = new InputPortPtr(){ public void handler() { 
	PORT_START(); 
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_VBLANK );	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		PORT_START(); 
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_LOW, IPT_COIN1, 5 );	PORT_SERVICE( 0x40, IP_ACTIVE_LOW );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );	PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
		PORT_START(); 	/* DSW1 */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x06, 0x06, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "2" );	
                PORT_DIPSETTING(    0x06, "3" );	
                PORT_DIPSETTING(    0x04, "4" );	
                PORT_DIPSETTING(    0x02, "5" );	
                PORT_DIPNAME( 0x08, 0x08, "1st Bonus" );	
                PORT_DIPSETTING(    0x08, "30000" );	
                PORT_DIPSETTING(    0x00, "40000" );	
                PORT_DIPNAME( 0x30, 0x30, "2nd Bonus" );	
                PORT_DIPSETTING(    0x30, "Every(?);50000" );
		PORT_DIPSETTING(    0x20, "Every(?);70000" );
		PORT_DIPSETTING(    0x10, "Every(?);90000" );
		PORT_DIPSETTING(    0x00, "None" );	PORT_DIPNAME( 0x40, 0x40, "Exercise" );	PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x80, "Normal" );	PORT_DIPSETTING(    0x00, "Hard" );
		PORT_START(); 	/* DSW2*/
		PORT_DIPNAME( 0x07, 0x07, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x04, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x05, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "4C_2C") );
		PORT_DIPSETTING(    0x06, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "3C_2C") );
		PORT_DIPSETTING(    0x02, DEF_STR( "2C_2C") );
		PORT_DIPSETTING(    0x07, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "1C_2C") );
	
		PORT_DIPNAME( 0x08, 0x08, "High Score Names" );	PORT_DIPSETTING(    0x00, "3 Letters" );	PORT_DIPSETTING(    0x08, "8 Letters" );	PORT_DIPNAME( 0x10, 0x10, "Allow Continue" );	PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x20, 0x20, "Unknown 2-6" );	PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Free_Play") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Endless Game (If Free Play)" );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
	INPUT_PORTS_END(); }}; 
	
	/****************************************************************************/
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,   /* 8*8 characters */
		512,   /* 512 characters */
		4,     /* 4 bits per pixel */
		new int[] {0,1,2,3},
		new int[] {0,4,8192*8+0,8192*8+4,8,12,8192*8+8,8192*8+12},
		new int[] {16*0, 16*1, 16*2, 16*3, 16*4, 16*5, 16*6, 16*7},
		16*8
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,    /* 16*16 characters */
		128,	  /* 128 sprites */
		4,        /* 4 bits per pixel */
		new int[] {0,1,2,3},
		new int[] {0,4,8192*8+0,8192*8+4,8,12,8192*8+8,8192*8+12,
		16*8+0,16*8+4,16*8+8192*8+0,16*8+8192*8+4,16*8+8,16*8+12,16*8+8192*8+8,16*8+8192*8+12},
		new int[] {16*0, 16*1, 16*2, 16*3, 16*4, 16*5, 16*6, 16*7,
		 16*16, 16*17, 16*18, 16*19, 16*20, 16*21, 16*22, 16*23},
		16*8*4
	);
	
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0x0000, spritelayout, 512, 16 ), /* sprite */
		new GfxDecodeInfo( REGION_GFX1, 0x4000, spritelayout, 512, 16 ), /* sprite */
		new GfxDecodeInfo( REGION_GFX1, 0x0000, charlayout,     0, 16 ), /* FG */
		new GfxDecodeInfo( REGION_GFX1, 0x8000, charlayout,   256, 16 ), /* BG */
		new GfxDecodeInfo( REGION_GFX1, 0x4000, charlayout,   256, 16 ), /* BG (?)*/
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	/****************************************************************************/
	
	static AY8910interface ay8910_interface = new AY8910interface
	(
		2,          /* 2 chips */
		12000000/8,    /* 1.5 MHz? */
		new int[] { 25, 25 },
		new ReadHandlerPtr[] { input_port_0_r , input_port_2_r },
		new ReadHandlerPtr[] { input_port_1_r , input_port_3_r },
		new WriteHandlerPtr[] { null, null },
		new WriteHandlerPtr[] { null, null }
	);
	
	static MachineDriver machine_driver_raiders5 = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				12000000/4,	/* 3.0MHz? */
				readmem1,writemem1,readport1,null,
				interrupt,1
			),
			new MachineCPU(
				CPU_Z80,
				12000000/4,	/* 3.0MHz? */
				readmem2,writemem2,null,null,
				interrupt,4
	
			)
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,
		400,
		null,
	
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 4*8, 28*8-1 ),
	
		gfxdecodeinfo,
		768, 0,
		null,
	
		VIDEO_TYPE_RASTER,
		null,
		generic_vh_start,
		generic_vh_stop,
		raiders5_vh_screenrefresh,
	
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_AY8910,
				ay8910_interface
			)
		}
	);
	
	/****************************************************************************/
	
	
	static RomLoadPtr rom_raiders5 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* CPU1 */
		ROM_LOAD( "raiders5.1", 0x0000,  0x4000, 0x47cea11f );	ROM_LOAD( "raiders5.2", 0x4000,  0x4000, 0xeb2ff410 );
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* CPU2 */
		ROM_LOAD( "raiders5.2", 0x0000,  0x4000, 0xeb2ff410 );
		ROM_REGION( 0x20000, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "raiders3.11f", 0x0000,  0x4000, 0x30041d58 );	ROM_LOAD( "raiders4.11g", 0x4000,  0x4000, 0xe441931c );	ROM_LOAD( "raiders5.11n", 0x8000,  0x4000, 0xc0895090 );ROM_END(); }}; 
	
	static RomLoadPtr rom_raidrs5t = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* CPU1 */
		ROM_LOAD( "raiders1.4c", 0x0000,  0x4000, 0x4e2d5679 );	ROM_LOAD( "raiders2.4d", 0x4000,  0x4000, 0xc8604be1 );
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* CPU2 */
		ROM_LOAD( "raiders2.4d", 0x0000,  0x4000, 0xc8604be1 );
		ROM_REGION( 0x20000, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "raiders3.11f", 0x0000,  0x4000, 0x30041d58 );	ROM_LOAD( "raiders4.11g", 0x4000,  0x4000, 0xe441931c );	ROM_LOAD( "raiders5.11n", 0x8000,  0x4000, 0xc0895090 );ROM_END(); }}; 
	
	
	public static GameDriver driver_raiders5	   = new GameDriver("1985"	,"raiders5"	,"raiders5.java"	,rom_raiders5,null	,machine_driver_raiders5	,input_ports_raiders5	,null	,ROT0	,	"UPL", "Raiders5" );
	public static GameDriver driver_raidrs5t	   = new GameDriver("1985"	,"raidrs5t"	,"raiders5.java"	,rom_raidrs5t,driver_raiders5	,machine_driver_raiders5	,input_ports_raiders5	,null	,ROT0	,	"UPL (Taito license)", "Raiders5 (Japan)" );
}
