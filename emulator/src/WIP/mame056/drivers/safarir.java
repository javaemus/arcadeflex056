/****************************************************************************

Safari Rally by SNK/Taito

Driver by Zsolt Vasvari


This hardware is a precursor to Phoenix.

----------------------------------

CPU board

76477        18MHz

              8080

Video board


 RL07  2114
       2114
       2114
       2114
       2114           RL01 RL02
       2114           RL03 RL04
       2114           RL05 RL06
 RL08  2114

11MHz

----------------------------------

TODO:

- SN76477 sound

****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

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
import static mame056.drawgfx.*;
import static mame056.inputH.*;
import static mame056.sndintrfH.*;
import static mame056.sndintrf.*;
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;
import static mame056.sound.mixer.*;
import static mame056.sound.mixerH.*;

import static mame056.vidhrdw.generic.*;
import static mame056.timer.*;
import static mame056.timerH.*;

import static arcadeflex056.fileio.*;
import static mame056.mame.*;
import static mame056.palette.*;

public class safarir
{
	
	
	public static UBytePtr safarir_ram1=new UBytePtr(), safarir_ram2=new UBytePtr();
	public static int[] safarir_ram_size=new int[2];
	
	public static UBytePtr safarir_ram=new UBytePtr();
	public static int[] safarir_scroll=new int[2];
	
	
	
	public static WriteHandlerPtr safarir_ram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		safarir_ram.write(offset, data);
	} };
	
	public static ReadHandlerPtr safarir_ram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return safarir_ram.read(offset);
	} };
	
	
	public static WriteHandlerPtr safarir_scroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		safarir_scroll[0] = data;
	} };
	
	public static WriteHandlerPtr safarir_ram_bank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		safarir_ram = data!=0 ? new UBytePtr(safarir_ram1) : new UBytePtr(safarir_ram2);
	} };
	
	
	public static VhUpdatePtr safarir_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		for (offs = safarir_ram_size[0]/2 - 1;offs >= 0;offs--)
		{
			int sx,sy;
			int code;
	
	
			sx = offs % 32;
			sy = offs / 32;
	
			code = safarir_ram.read(offs + safarir_ram_size[0]/2);
	
	
			drawgfx(bitmap,Machine.gfx[0],
					code & 0x7f,
					code >> 7,
					0,0,
					(8*sx - safarir_scroll[0]) & 0xff,8*sy,
					Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	
	
		/* draw the frontmost playfield. They are characters, but draw them as sprites */
	
		for (offs = safarir_ram_size[0]/2 - 1;offs >= 0;offs--)
		{
			int sx,sy,transparency;
			int code;
	
	
			sx = offs % 32;
			sy = offs / 32;
	
			code = safarir_ram.read(offs);
	
			transparency = (sx >= 3) ? TRANSPARENCY_PEN : TRANSPARENCY_NONE;
	
	
			drawgfx(bitmap,Machine.gfx[1],
					code & 0x7f,
					code >> 7,
					0,0,
					8*sx,8*sy,
					Machine.visible_area,transparency,0);
		}
	} };
	
	
	static char palette[] =
	{
		0x00,0x00,0x00, /* black */
		0x80,0x80,0x80, /* gray */
		0xff,0xff,0xff, /* white */
	};
	static char colortable[] =
	{
		0x00, 0x01,
		0x00, 0x02,
	};
	
	static VhConvertColorPromPtr init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] game_palette, char[] game_colortable, UBytePtr color_prom) {
                memcpy(game_palette,palette,palette.length);
		memcpy(game_colortable,colortable,colortable.length);
            }
        };
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x17ff, MRA_ROM ),
		new Memory_ReadAddress( 0x2000, 0x27ff, safarir_ram_r ),
		new Memory_ReadAddress( 0x3800, 0x38ff, input_port_0_r ),
		new Memory_ReadAddress( 0x3c00, 0x3cff, input_port_1_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x17ff, MWA_ROM ),
		new Memory_WriteAddress( 0x2000, 0x27ff, safarir_ram_w, safarir_ram1, safarir_ram_size ),
		new Memory_WriteAddress( 0x2800, 0x28ff, safarir_ram_bank_w ),
		new Memory_WriteAddress( 0x2c00, 0x2cff, safarir_scroll_w ),
		new Memory_WriteAddress( 0x3000, 0x30ff, MWA_NOP ),	/* goes to SN76477 */
	
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_NOP, safarir_ram2 ),	/* only here to initialize pointer */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	static InputPortPtr input_ports_safarir = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	/* DSW0 */
		PORT_DIPNAME( 0x03, 0x00, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPSETTING(    0x02, "5" );
		PORT_DIPSETTING(    0x03, "6" );
		PORT_DIPNAME( 0x0c, 0x04, "Acceleration Rate" );
		PORT_DIPSETTING(    0x00, "Slowest" );
		PORT_DIPSETTING(    0x04, "Slow" );
		PORT_DIPSETTING(    0x08, "Fast" );
		PORT_DIPSETTING(    0x0c, "Fastest" );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x10, DEF_STR( "On") );
		PORT_DIPNAME( 0x60, 0x00, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x00, "3000" );
		PORT_DIPSETTING(    0x20, "5000" );
		PORT_DIPSETTING(    0x40, "7000" );
		PORT_DIPSETTING(    0x60, "9000" );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_VBLANK );
	INPUT_PORTS_END(); }}; 
	
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,	/* 8*8 chars */
		128,	/* 128 characters */
		1,		/* 1 bit per pixel */
		new int[] { 0 },
		new int[] { 7, 6, 5, 4, 3, 2, 1, 0 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		8*8	/* every char takes 8 consecutive bytes */
	);
	
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout, 0, 2 ),
		new GfxDecodeInfo( REGION_GFX2, 0, charlayout, 0, 2 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	static MachineDriver machine_driver_safarir = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_8080,
				3072000,	/* 3 MHz ? */
				readmem,writemem,null,null,
				ignore_interrupt,1
			)
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,
		1,      /* single CPU, no need for interleaving */
		null,	/* init machine */
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 30*8-1, 0*8, 28*8-1 ),
		gfxdecodeinfo,
		3,2*2,
		init_palette,
	
		VIDEO_TYPE_RASTER,
		null,
		null,
		null,
		safarir_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
                null
	);
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_safarir = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );    /* 64k for main CPU */
		ROM_LOAD( "rl01",		0x0000, 0x0400, 0xcf7703c9 );
		ROM_LOAD( "rl02",		0x0400, 0x0400, 0x1013ecd3 );
		ROM_LOAD( "rl03",		0x0800, 0x0400, 0x84545894 );
		ROM_LOAD( "rl04",		0x0c00, 0x0400, 0x5dd12f96 );
		ROM_LOAD( "rl05",		0x1000, 0x0400, 0x935ed469 );
		ROM_LOAD( "rl06",		0x1400, 0x0400, 0x24c1cd42 );
	
		ROM_REGION( 0x0400, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "rl08",		0x0000, 0x0400, 0xd6a50aac );
	
		ROM_REGION( 0x0400, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "rl07",		0x0000, 0x0400, 0xba525203 );
	ROM_END(); }}; 
	
	
	public static GameDriver driver_safarir	   = new GameDriver("19??"	,"safarir"	,"safarir.java"	,rom_safarir,null	,machine_driver_safarir	,input_ports_safarir	,null	,ROT90	,	"SNK", "Safari Rally", GAME_NO_SOUND | GAME_IMPERFECT_COLORS );
}
