/***************************************************************************

  Qwak (prototype) driver.

  This driver is based *extremely* loosely on the Centipede driver.

  The following memory map is pure speculation:

  0000-01FF     R/W		RAM
  0200-025F     R/W		RAM?  ER2055 NOVRAM maybe?
  0300-03FF     R/W		RAM
  0400-07BF		R/W		Video RAM
  07C0-07FF		R/W		Sprite RAM
  1000			W		???
  2000			W		???
  2001			W		???
  2003          W		Start LED 1
  2004          W		Start LED 2
  3000			R		$40 = !UP			$80 = unused?
  3001			R		$40 = !DOWN			$80 = ???
  3002			R		$40 = !LEFT			$80 = ???
  3003			R		$40 = !RIGHT		$80 = unused?
  3004			R		$40 = !START1		$80 = ???
  3005			R		$40 = !START2		$80 = !COIN
  3006			R		$40 = !BUTTON1		$80 = !COIN
  3007			R		$40 = unused?		$80 = !COIN
  4000          R		???
  6000-600F		R/W		Pokey 1
  7000-700F		R/W		Pokey 2
  8000-BFFF		R		ROM

  TODO:
	- fix colors
	- coins seem to count twice instead of once?
	- find DIP switches (should be at $4000, I would think)
	- figure out what $1000, $2000, and $2001 are used for
	- figure out exactly what the unknown bits in the $3000 area do


If you have any questions about how this driver works, don't hesitate to
ask.  - Mike Balfour (mab22@po.cwru.edu)
***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static WIP.mame056.vidhrdw.qwakprot.*;

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

import static mame056.vidhrdw.generic.*;
import static mame056.timer.*;
import static mame056.timerH.*;

import static arcadeflex056.fileio.*;
import static mame056.machine.atari_vg.*;
import static mame056.palette.*;
import static mame056.sound.pokey.*;
import static mame056.sound.pokeyH.*;

public class qwakprot
{
	
	
	
	public static WriteHandlerPtr qwakprot_led_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*TODO*///set_led_status(offset,~data & 0x80);
	} };
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x01ff, MRA_RAM ),
		new Memory_ReadAddress( 0x0200, 0x025f, MRA_RAM ),
		new Memory_ReadAddress( 0x0300, 0x03ff, MRA_RAM ),
		new Memory_ReadAddress( 0x0400, 0x07ff, MRA_RAM ),
		new Memory_ReadAddress( 0x3000, 0x3000, input_port_0_r ),
		new Memory_ReadAddress( 0x3001, 0x3001, input_port_1_r ),
		new Memory_ReadAddress( 0x3002, 0x3002, input_port_2_r ),
		new Memory_ReadAddress( 0x3003, 0x3003, input_port_3_r ),
		new Memory_ReadAddress( 0x3004, 0x3004, input_port_4_r ),
		new Memory_ReadAddress( 0x3005, 0x3005, input_port_5_r ),
		new Memory_ReadAddress( 0x3006, 0x3006, input_port_6_r ),
		new Memory_ReadAddress( 0x3007, 0x3007, input_port_7_r ),
		new Memory_ReadAddress( 0x4000, 0x4000, input_port_8_r ),			/* just guessing */
		new Memory_ReadAddress( 0x6000, 0x600f, pokey1_r ),
		new Memory_ReadAddress( 0x7000, 0x700f, pokey2_r ),
		new Memory_ReadAddress( 0x8000, 0xbfff, MRA_ROM ),
		new Memory_ReadAddress( 0xf000, 0xffff, MRA_ROM ),	/* for the reset / interrupt vectors */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x01ff, MWA_RAM ),
		new Memory_WriteAddress( 0x0200, 0x025f, MWA_RAM ),
		new Memory_WriteAddress( 0x0300, 0x03ff, MWA_RAM ),
		new Memory_WriteAddress( 0x0400, 0x07bf, videoram_w, videoram, videoram_size ),
		new Memory_WriteAddress( 0x07c0, 0x07ff, MWA_RAM, spriteram ),
		new Memory_WriteAddress( 0x1c00, 0x1c0f, qwakprot_paletteram_w, paletteram ), /* just guessing */
	//	new Memory_WriteAddress( 0x2000, 0x2001, coin_counter_w ),
		new Memory_WriteAddress( 0x2003, 0x2004, qwakprot_led_w ),
		new Memory_WriteAddress( 0x6000, 0x600f, pokey1_w ),
		new Memory_WriteAddress( 0x7000, 0x700f, pokey2_w ),
		new Memory_WriteAddress( 0x8000, 0xbfff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	static InputPortPtr input_ports_qwakprot = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT ( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_UP );
		PORT_BIT ( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );		/* ??? */
	
		PORT_START();       /* IN1 */
		PORT_BIT ( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN );
		PORT_BIT ( 0x80, IP_ACTIVE_HIGH, IPT_VBLANK );		/* ??? */
	
		PORT_START();       /* IN2 */
		PORT_BIT ( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT );
		PORT_BIT ( 0x80, IP_ACTIVE_LOW, IPT_VBLANK );		/* ??? */
	
		PORT_START();       /* IN3 */
		PORT_BIT ( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT );
		PORT_BIT ( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );		/* ??? */
	
		PORT_START();       /* IN4 */
		PORT_BIT ( 0x40, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT ( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );		/* ??? */
	
		PORT_START();       /* IN5 */
		PORT_BIT ( 0x40, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT ( 0x80, IP_ACTIVE_LOW, IPT_COIN1 );
	
		PORT_START();       /* IN6 */
		PORT_BIT ( 0x40, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT ( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );
	
		PORT_START();       /* IN7 */
		PORT_BIT ( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );		/* ??? */
		PORT_BIT ( 0x80, IP_ACTIVE_LOW, IPT_COIN3 );
	
		PORT_START();       /* IN8 */
		PORT_DIPNAME( 0x01, 0x00, "DIP 1" );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, "DIP 2" );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, "DIP 3" );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "DIP 4" );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, "DIP 5" );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x00, "DIP 6" );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, "DIP 7" );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "DIP 8" );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,	/* 8*8 characters */
		128,	/* 128 characters */
		4,	/* 4 bits per pixel */
		new int[] { 0x3000*8, 0x2000*8, 0x1000*8, 0 },	/* the four bitplanes are separated */
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		16*8	/* every char takes 8 consecutive bytes, then skip 8 */
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		8,16,	/* 16*8 sprites */
		128,	/* 128 sprites */
		4,	/* 4 bits per pixel */
		new int[] { 0x3000*8, 0x2000*8, 0x1000*8, 0 },	/* the four bitplanes are separated */
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
				8*8, 9*8, 10*8, 11*8, 12*8, 13*8, 14*8, 15*8 },
		16*8	/* every sprite takes 16 consecutive bytes */
	);
	
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0x0800, charlayout,   0, 1 ),
		new GfxDecodeInfo( REGION_GFX1, 0x0808, charlayout,   0, 1 ),
		new GfxDecodeInfo( REGION_GFX1, 0x0000, spritelayout, 0, 1 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	static POKEYinterface pokey_interface = new POKEYinterface
	(
		2,	/* 2 chips */
		1500000,	/* 1.5 MHz??? */
		new int[] { 50, 50 },
		/* The 8 pot handlers */
		new ReadHandlerPtr[] { null, null },
		new ReadHandlerPtr[] { null, null },
		new ReadHandlerPtr[] { null, null },
		new ReadHandlerPtr[] { null, null },
		new ReadHandlerPtr[] { null, null },
		new ReadHandlerPtr[] { null, null },
		new ReadHandlerPtr[] { null, null },
		new ReadHandlerPtr[] { null, null },
		/* The allpot handler */
		new ReadHandlerPtr[] { null, null }
	);
	
	
	static MachineDriver machine_driver_qwakprot = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6502,
				12096000/8,	/* 1.512 MHz?? */
				readmem,writemem,null,null,
				interrupt,4
			)
		},
		60, 1460,	/* frames per second, vblank duration??? */
		1,	/* single CPU, no need for interleaving */
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 30*8-1 ),
		gfxdecodeinfo,
		16, 0,
		null,
	
		VIDEO_TYPE_RASTER|VIDEO_SUPPORTS_DIRTY,
		null,
		generic_vh_start,
		generic_vh_stop,
		qwakprot_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_POKEY,
				pokey_interface
			)
		}
	);
	
	
	/***************************************************************************
	
	  Game ROMs
	
	***************************************************************************/
	
	static RomLoadPtr rom_qwakprot = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "qwak8000.bin", 0x8000, 0x1000, 0x4d002d8a );
		ROM_LOAD( "qwak9000.bin", 0x9000, 0x1000, 0xe0c78fd7 );
		ROM_LOAD( "qwaka000.bin", 0xa000, 0x1000, 0xe5770fc9 );
		ROM_LOAD( "qwakb000.bin", 0xb000, 0x1000, 0x90771cc0 );
		ROM_RELOAD(               0xf000, 0x1000 );/* for the reset and interrupt vectors */
	
		ROM_REGION( 0x4000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "qwakgfx0.bin", 0x0000, 0x1000, 0xbed2c067 );
		ROM_LOAD( "qwakgfx1.bin", 0x1000, 0x1000, 0x73a31d28 );
		ROM_LOAD( "qwakgfx2.bin", 0x2000, 0x1000, 0x07fd9e80 );
		ROM_LOAD( "qwakgfx3.bin", 0x3000, 0x1000, 0xe8416f2b );
	ROM_END(); }}; 
	
	
	
	public static GameDriver driver_qwakprot	   = new GameDriver("1982"	,"qwakprot"	,"qwakprot.java"	,rom_qwakprot,null	,machine_driver_qwakprot	,input_ports_qwakprot	,null	,ROT270	,	"Atari", "Qwak (prototype)", GAME_NO_COCKTAIL );
}
