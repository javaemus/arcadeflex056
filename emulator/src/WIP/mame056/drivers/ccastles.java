/***************************************************************************

I'm in no mood to write documentation now, so if you have any questions
about this driver, please address them to Pat Lawrence <pjl@ns.net>.  I'll
be happy to help you out any way I can.

Crystal Castles memory map.

 Address  A A A A A A A A A A A A A A A A  R  D D D D D D D D  Function
          1 1 1 1 1 1 9 8 7 6 5 4 3 2 1 0  /  7 6 5 4 3 2 1 0
          5 4 3 2 1 0                      W
-------------------------------------------------------------------------------
0000      X X X X X X X X X X X X X X X X  W  X X X X X X X X  X Coordinate
0001      0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1  W  D D D D D D D D  Y Coordinate
0002      0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 R/W D D D D          Bit Mode
0003-0BFF 0 0 0 0 A A A A A A A A A A A A R/W D D D D D D D D  RAM (DRAM)
0C00-7FFF 0 A A A A A A A A A A A A A A A R/W D D D D D D D D  Screen RAM
8000-8DFF 1 0 0 0 A A A A A A A A A A A A R/W D D D D D D D D  RAM (STATIC)
8E00-8EFF 1 0 0 0 1 1 1 0 A A A A A A A A R/W D D D D D D D D  MOB BUF 2
-------------------------------------------------------------------------------
8F00-8FFF 1 0 0 0 1 1 1 1 A A A A A A A A R/W D D D D D D D D  MOB BUF 1
                                      0 0 R/W D D D D D D D D  MOB Picture
                                      0 1 R/W D D D D D D D D  MOB Vertical
                                      1 0 R/W D D D D D D D D  MOB Priority
                                      1 1 R/W D D D D D D D D  MOB Horizontal
-------------------------------------------------------------------------------
9000-90FF 1 0 0 1 0 0 X X A A A A A A A A R/W D D D D D D D D  NOVRAM
9400-9401 1 0 0 1 0 1 0 X X X X X X X 0 A  R                   TRAK-BALL 1
9402-9403 1 0 0 1 0 1 0 X X X X X X X 1 A  R                   TRAK-BALL 2
9500-9501 1 0 0 1 0 1 0 X X X X X X X X A  R                   TRAK-BALL 1 mirror
9600      1 0 0 1 0 1 1 X X X X X X X X X  R                   IN0
                                           R                D  COIN R
                                           R              D    COIN L
                                           R            D      COIN AUX
                                           R          D        SLAM
                                           R        D          SELF TEST
                                           R      D            VBLANK
                                           R    D              JMP1
                                           R  D                JMP2
-------------------------------------------------------------------------------
9800-980F 1 0 0 1 1 0 0 X X X X X A A A A R/W D D D D D D D D  CI/O 0
9A00-9A0F 1 0 0 1 1 0 1 X X X X X A A A A R/W D D D D D D D D  CI/O 1
9A08                                                    D D D  Option SW
                                                      D        SPARE
                                                    D          SPARE
                                                  D            SPARE
9C00      1 0 0 1 1 1 0 0 0 X X X X X X X  W                   RECALL
-------------------------------------------------------------------------------
9C80      1 0 0 1 1 1 0 0 1 X X X X X X X  W  D D D D D D D D  H Scr Ctr Load
9D00      1 0 0 1 1 1 0 1 0 X X X X X X X  W  D D D D D D D D  V Scr Ctr Load
9D80      1 0 0 1 1 1 0 1 1 X X X X X X X  W                   Int. Acknowledge
9E00      1 0 0 1 1 1 1 0 0 X X X X X X X  W                   WDOG
          1 0 0 1 1 1 1 0 1 X X X X A A A  W                D  OUT0
9E80                                0 0 0  W                D  Trak Ball Light P1
9E81                                0 0 1  W                D  Trak Ball Light P2
9E82                                0 1 0  W                D  Store Low
9E83                                0 1 1  W                D  Store High
9E84                                1 0 0  W                D  Spare
9E85                                1 0 1  W                D  Coin Counter R
9E86                                1 1 0  W                D  Coin Counter L
9E87                                1 1 1  W                D  BANK0-BANK1
          1 0 0 1 1 1 1 1 0 X X X X A A A  W          D        OUT1
9F00                                0 0 0  W          D        ^AX
9F01                                0 0 1  W          D        ^AY
9F02                                0 1 0  W          D        ^XINC
9F03                                0 1 1  W          D        ^YINC
9F04                                1 0 0  W          D        PLAYER2 (flip screen)
9F05                                1 0 1  W          D        ^SIRE
9F06                                1 1 0  W          D        BOTHRAM
9F07                                1 1 1  W          D        BUF1/^BUF2 (sprite bank)
9F80-9FBF 1 0 0 1 1 1 1 1 1 X A A A A A A  W  D D D D D D D D  COLORAM
A000-FFFF 1 A A A A A A A A A A A A A A A  R  D D D D D D D D  Program ROM

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static WIP.mame056.vidhrdw.ccastles.*;

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

public class ccastles
{
	
	
	static UBytePtr nvram = new UBytePtr();
	static int[] nvram_size = new int[2];
	
	static nvramPtr nvram_handler = new nvramPtr() {
            public void handler(Object file, int read_or_write) {
                if (read_or_write != 0)
			osd_fwrite(file,nvram,nvram_size[0]);
		else
		{
			if (file != null)
				osd_fread(file,nvram,nvram_size[0]);
			else
				memset(nvram,0,nvram_size[0]);
		}
            }
        };
	
	public static WriteHandlerPtr ccastles_led_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*TODO*///set_led_status(offset,~data & 1);
	} };
	
	public static WriteHandlerPtr ccastles_coin_counter_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* this is not working, haven't investigated why */
		coin_counter_w.handler(offset^1, ~data);
	} };
	
	public static WriteHandlerPtr ccastles_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
	
		if (data!=0) { cpu_setbank(1,new UBytePtr(RAM, 0x10000)); }
		else { cpu_setbank(1,new UBytePtr(RAM, 0xa000)); }
	} };
	
	public static WriteHandlerPtr flip_screen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		flip_screen_set(data);
	} };
	
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0002, 0x0002, ccastles_bitmode_r ),
		new Memory_ReadAddress( 0x0000, 0x8fff, MRA_RAM ),
		new Memory_ReadAddress( 0x9000, 0x90ff, MRA_RAM ),
		new Memory_ReadAddress( 0x9400, 0x9400, input_port_2_r ),	/* trackball y - player 1 */
		new Memory_ReadAddress( 0x9402, 0x9402, input_port_2_r ),	/* trackball y - player 2 */
		new Memory_ReadAddress( 0x9500, 0x9500, input_port_2_r ),	/* trackball y - player 1 mirror */
		new Memory_ReadAddress( 0x9401, 0x9401, input_port_3_r ),	/* trackball x - player 1 */
		new Memory_ReadAddress( 0x9403, 0x9403, input_port_3_r ),	/* trackball x - player 2 */
		new Memory_ReadAddress( 0x9501, 0x9501, input_port_3_r ),	/* trackball x - player 1 mirror */
		new Memory_ReadAddress( 0x9600, 0x9600, input_port_0_r ),	/* IN0 */
		new Memory_ReadAddress( 0x9800, 0x980f, pokey1_r ), /* Random # generator on a Pokey */
		new Memory_ReadAddress( 0x9a00, 0x9a0f, pokey2_r ), /* Random #, IN1 */
		new Memory_ReadAddress( 0xa000, 0xdfff, MRA_BANK1 ),
		new Memory_ReadAddress( 0xe000, 0xffff, MRA_ROM ),	/* ROMs/interrupt vectors */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x0001, MWA_RAM, ccastles_screen_addr ),
		new Memory_WriteAddress( 0x0002, 0x0002, ccastles_bitmode_w ),
		new Memory_WriteAddress( 0x0003, 0x0bff, MWA_RAM ),
		new Memory_WriteAddress( 0x0c00, 0x7fff, MWA_RAM, videoram ),
		new Memory_WriteAddress( 0x8000, 0x8dff, MWA_RAM ),
		new Memory_WriteAddress( 0x8e00, 0x8eff, MWA_RAM, spriteram_2, spriteram_size ),
		new Memory_WriteAddress( 0x8f00, 0x8fff, MWA_RAM, spriteram ),
		new Memory_WriteAddress( 0x9000, 0x90ff, MWA_RAM, nvram, nvram_size ),
		new Memory_WriteAddress( 0x9800, 0x980f, pokey1_w ),
		new Memory_WriteAddress( 0x9a00, 0x9a0f, pokey2_w ),
		new Memory_WriteAddress( 0x9c80, 0x9c80, MWA_RAM, ccastles_scrollx ),
		new Memory_WriteAddress( 0x9d00, 0x9d00, MWA_RAM, ccastles_scrolly ),
		new Memory_WriteAddress( 0x9d80, 0x9d80, MWA_NOP ),
		new Memory_WriteAddress( 0x9e00, 0x9e00, watchdog_reset_w ),
		new Memory_WriteAddress( 0x9e80, 0x9e81, ccastles_led_w ),
		new Memory_WriteAddress( 0x9e85, 0x9e86, ccastles_coin_counter_w ),
		new Memory_WriteAddress( 0x9e87, 0x9e87, ccastles_bankswitch_w ),
		new Memory_WriteAddress( 0x9f00, 0x9f01, MWA_RAM, ccastles_screen_inc_enable ),
		new Memory_WriteAddress( 0x9f02, 0x9f03, MWA_RAM, ccastles_screen_inc ),
		new Memory_WriteAddress( 0x9f04, 0x9f04, flip_screen_w ),
		new Memory_WriteAddress( 0x9f05, 0x9f06, MWA_RAM ),
		new Memory_WriteAddress( 0x9f07, 0x9f07, MWA_RAM, ccastles_sprite_bank ),
		new Memory_WriteAddress( 0x9f80, 0x9fbf, ccastles_paletteram_w ),
		new Memory_WriteAddress( 0xa000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	static InputPortPtr input_ports_ccastles = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT ( 0x01, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT ( 0x02, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT ( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );
		PORT_BIT ( 0x08, IP_ACTIVE_LOW, IPT_TILT );
		PORT_SERVICE( 0x10, IP_ACTIVE_LOW );
		PORT_BIT ( 0x20, IP_ACTIVE_HIGH, IPT_VBLANK );
		PORT_BIT ( 0x40, IP_ACTIVE_LOW, IPT_BUTTON1 );			/* 1p Jump, non-cocktail start1 */
		PORT_BIT ( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );/* 2p Jump, non-cocktail start2 */
	
		PORT_START(); 	/* IN1 */
		PORT_BIT ( 0x07, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT ( 0x08, IP_ACTIVE_LOW, IPT_START1 );			/* cocktail only */
		PORT_BIT ( 0x10, IP_ACTIVE_LOW, IPT_START2 );			/* cocktail only */
		PORT_DIPNAME(0x20, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING (   0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING (   0x20, DEF_STR( "Cocktail") );
		PORT_BIT ( 0xc0, IP_ACTIVE_HIGH, IPT_UNKNOWN );
	
		PORT_START(); 	/* IN2 */
		PORT_ANALOG( 0xff, 0x7f, IPT_TRACKBALL_Y | IPF_REVERSE, 10, 30, 0, 0 );
	
		PORT_START(); 	/* IN3 */
		PORT_ANALOG( 0xff, 0x7f, IPT_TRACKBALL_X, 10, 30, 0, 0 );
	INPUT_PORTS_END(); }}; 
	
	static GfxLayout ccastles_spritelayout = new GfxLayout
	(
		8,16,	/* 8*16 sprites */
		256,	/* 256 sprites */
		4,	/* 4 bits per pixel (the most significant bit is always 0) */
		new int[] { 0x2000*8+0, 0x2000*8+4, 0, 4 },	/* the three bitplanes are separated */
		new int[] { 0, 1, 2, 3, 8+0, 8+1, 8+2, 8+3 },
		new int[] { 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16,
				8*16, 9*16, 10*16, 11*16, 12*16, 13*16, 14*16, 15*16 },
		32*8	/* every sprite takes 32 consecutive bytes */
	);
	
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0x0000, ccastles_spritelayout,  0, 1 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	
	static POKEYinterface pokey_interface = new POKEYinterface
	(
		2,	/* 2 chips */
		1250000,	/* 1.25 MHz??? */
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
		new ReadHandlerPtr[] { null, input_port_1_r }
	);
	
	
	
	static MachineDriver machine_driver_ccastles = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6502,
				1500000,	/* 1.5 MHz */
				readmem,writemem,null,null,
				interrupt,4
			)
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,	/* single CPU, no need for interleaving */
	
		null,
		256, 232, new rectangle( 0, 255, 0, 231 ),
		gfxdecodeinfo,
		32, 0,
		null,
	
		VIDEO_TYPE_RASTER,
		null,
		ccastles_vh_start,
		ccastles_vh_stop,
		ccastles_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_POKEY,
				pokey_interface
			)
		},
	
		nvram_handler
	);
	
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_ccastles = new RomLoadPtr(){ public void handler(){ 
	     ROM_REGION( 0x14000, REGION_CPU1, 0 );/* 64k for code */
	     ROM_LOAD( "ccastles.303", 0x0a000, 0x2000, 0x10e39fce );
	     ROM_LOAD( "ccastles.304", 0x0c000, 0x2000, 0x74510f72 );
	     ROM_LOAD( "ccastles.305", 0x0e000, 0x2000, 0x9418cf8a );
	     ROM_LOAD( "ccastles.102", 0x10000, 0x2000, 0xf6ccfbd4 );/* Bank switched ROMs */
	     ROM_LOAD( "ccastles.101", 0x12000, 0x2000, 0xe2e17236 );/* containing level data. */
	
	     ROM_REGION( 0x4000, REGION_GFX1, ROMREGION_DISPOSE );
	     ROM_LOAD( "ccastles.107", 0x0000, 0x2000, 0x39960b7d );
	     ROM_LOAD( "ccastles.106", 0x2000, 0x2000, 0x9d1d89fc );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ccastle2 = new RomLoadPtr(){ public void handler(){ 
	     ROM_REGION( 0x14000, REGION_CPU1, 0 );/* 64k for code */
	     ROM_LOAD( "ccastles.203", 0x0a000, 0x2000, 0x348a96f0 );
	     ROM_LOAD( "ccastles.204", 0x0c000, 0x2000, 0xd48d8c1f );
	     ROM_LOAD( "ccastles.205", 0x0e000, 0x2000, 0x0e4883cc );
	     ROM_LOAD( "ccastles.102", 0x10000, 0x2000, 0xf6ccfbd4 );/* Bank switched ROMs */
	     ROM_LOAD( "ccastles.101", 0x12000, 0x2000, 0xe2e17236 );/* containing level data. */
	
	     ROM_REGION( 0x4000, REGION_GFX1, ROMREGION_DISPOSE );
	     ROM_LOAD( "ccastles.107", 0x0000, 0x2000, 0x39960b7d );
	     ROM_LOAD( "ccastles.106", 0x2000, 0x2000, 0x9d1d89fc );
	ROM_END(); }}; 
	
	
	
	public static GameDriver driver_ccastles	   = new GameDriver("1983"	,"ccastles"	,"ccastles.java"	,rom_ccastles,null	,machine_driver_ccastles	,input_ports_ccastles	,null	,ROT0	,	"Atari", "Crystal Castles (set 1)" );
	public static GameDriver driver_ccastle2	   = new GameDriver("1983"	,"ccastle2"	,"ccastles.java"	,rom_ccastle2,driver_ccastles	,machine_driver_ccastles	,input_ports_ccastles	,null	,ROT0	,	"Atari", "Crystal Castles (set 2)" );
}
