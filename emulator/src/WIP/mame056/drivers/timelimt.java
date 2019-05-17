/***************************************************************************

Time Limit (c) 1983 Chuo

driver by Ernesto Corvi

Notes:
- Sprite colors are wrong (missing colortable?)

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstdlib.rand;
import static common.ptr.*;
import static common.libc.cstring.*;
import static arcadeflex056.fileio.*;
import static mame056.common.*;
import static mame056.mame.*;
import static mame056.sndintrf.*;
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
import static mame056.memory.*;
import static mame056.sound.ay8910H.*;
import static mame056.sound.ay8910.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;

import static mame056.vidhrdw.generic.*;
import static WIP.mame056.vidhrdw.timelimt.*;

// refactor
import static arcadeflex036.osdepend.logerror;

import static mame056.sound.sn76496.*;
import static mame056.sound.sn76496H.*;

public class timelimt
{
	
	/***************************************************************************/
	
	static int nmi_enabled = 0;
	
	static InitMachinePtr timelimt_init = new InitMachinePtr() {
            public void handler() {
                soundlatch_setclearedvalue( 0 );
		nmi_enabled = 0;
            }
        };
	
	public static WriteHandlerPtr nmi_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		nmi_enabled = data & 1;	/* bit 0 = nmi enable */
	} };
	
	public static WriteHandlerPtr sound_reset_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (( data & 1 ) != 0)
			cpu_set_reset_line( 1, PULSE_LINE );
	} };
	
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),		/* rom */
		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),		/* ram */
		new Memory_ReadAddress( 0x8800, 0x8bff, MRA_RAM ),		/* video ram */
		new Memory_ReadAddress( 0x9000, 0x97ff, MRA_RAM ),		/* background ram */
		new Memory_ReadAddress( 0x9800, 0x98ff, MRA_RAM ),		/* sprite ram */
		new Memory_ReadAddress( 0xa000, 0xa000, input_port_0_r ), /* input port */
		new Memory_ReadAddress( 0xa800, 0xa800, input_port_1_r ),	/* input port */
		new Memory_ReadAddress( 0xb000, 0xb000, input_port_2_r ),	/* DSW */
		new Memory_ReadAddress( 0xb800, 0xb800, MRA_NOP ),		/* NMI ack? */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),		/* rom */
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM ),		/* ram */
		new Memory_WriteAddress( 0x8800, 0x8bff, timelimt_videoram_w, videoram, videoram_size ),	/* video ram */
		new Memory_WriteAddress( 0x9000, 0x97ff, timelimt_bg_videoram_w, timelimt_bg_videoram, timelimt_bg_videoram_size ),/* background ram */
		new Memory_WriteAddress( 0x9800, 0x98ff, MWA_RAM, spriteram, spriteram_size ),	/* sprite ram */
		new Memory_WriteAddress( 0xb000, 0xb000, nmi_enable_w ),	/* nmi enable */
		new Memory_WriteAddress( 0xb003, 0xb003, sound_reset_w ),	/* sound reset ? */
		new Memory_WriteAddress( 0xb800, 0xb800, soundlatch_w ), 	/* sound write */
		new Memory_WriteAddress( 0xc800, 0xc800, timelimt_scroll_x_lsb_w ),
		new Memory_WriteAddress( 0xc801, 0xc801, timelimt_scroll_x_msb_w ),
		new Memory_WriteAddress( 0xc802, 0xc802, timelimt_scroll_y_w ),
		new Memory_WriteAddress( 0xc803, 0xc803, MWA_NOP ),		/* ???? bit 0 used only */
		new Memory_WriteAddress( 0xc804, 0xc804, MWA_NOP ),		/* ???? not used */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	public static IO_ReadPort readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_ReadPort( 0x00, 0x00, watchdog_reset_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	public static Memory_ReadAddress readmem_sound[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x1fff, MRA_ROM ),	/* rom */
		new Memory_ReadAddress( 0x3800, 0x3bff, MRA_RAM ),	/* ram */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress writemem_sound[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x1fff, MWA_ROM ),	/* rom */
		new Memory_WriteAddress( 0x3800, 0x3bff, MWA_RAM ),	/* ram */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	public static IO_ReadPort readport_sound[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_ReadPort( 0x8c, 0x8d, AY8910_read_port_0_r ),
		new IO_ReadPort( 0x8e, 0x8f, AY8910_read_port_1_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	public static IO_WritePort writeport_sound[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_WritePort( 0x00, 0x00, soundlatch_clear_w ),
		new IO_WritePort( 0x8c, 0x8c, AY8910_control_port_0_w ),
		new IO_WritePort( 0x8d, 0x8d, AY8910_write_port_0_w ),
		new IO_WritePort( 0x8e, 0x8e, AY8910_control_port_1_w ),
		new IO_WritePort( 0x8f, 0x8f, AY8910_write_port_1_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	/***************************************************************************/
	
	static InputPortPtr input_ports_timelimt = new InputPortPtr(){ public void handler() { 
	PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_4WAY );	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY );	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_4WAY );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_4WAY );	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_START1 );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_START2 );	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
	
		PORT_START(); 	/* DSW1 */
		PORT_DIPNAME( 0x07, 0x01, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x00, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x02, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x05, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x06, DEF_STR( "1C_6C") );
		PORT_DIPSETTING(    0x07, DEF_STR( "1C_7C") );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "3" );	PORT_DIPSETTING(    0x10, "5" );	PORT_DIPNAME( 0x20, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x20, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_BITX(    0x80, 0x00, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Invincibility", IP_KEY_NONE, IP_JOY_NONE );	PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	/***************************************************************************/
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,2),
		4,
		new int[] { RGN_FRAC(0,2)+0, RGN_FRAC(0,2)+4, RGN_FRAC(1,2)+0, RGN_FRAC(1,2)+4 },
		new int[] { 0, 1, 2, 3, 8+0, 8+1, 8+2, 8+3 },
		new int[] { 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16 },
		16*8
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,3),
		3,
		new int[] { RGN_FRAC(0,3), RGN_FRAC(1,3), RGN_FRAC(2,3) },
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7,
				8*8+0, 8*8+1, 8*8+2, 8*8+3, 8*8+4, 8*8+5, 8*8+6, 8*8+7 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
				16*8, 17*8, 18*8, 19*8, 20*8, 21*8, 22*8, 23*8 },
		32*8
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,   32, 1 ),	/* seems correct */
		new GfxDecodeInfo( REGION_GFX2, 0, charlayout,    0, 1 ),	/* seems correct */
		new GfxDecodeInfo( REGION_GFX3, 0, spritelayout,  0, 8 ),	/* ?? */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	/***************************************************************************/
	
	static AY8910interface ay8910_interface = new AY8910interface
	(
		2,	/* 2 chips */
		18432000/12,
		new int[] { 25, 25 },
		new ReadHandlerPtr[] { null, soundlatch_r },
		new ReadHandlerPtr[] { null, null },
		new WriteHandlerPtr[] { null, null },
		new WriteHandlerPtr[] { null, null }
	);
	
	public static InterruptPtr timelimt_irq = new InterruptPtr() { public int handler()  {
		if ( nmi_enabled != 0 )
			return nmi_interrupt.handler();
	
		return ignore_interrupt.handler();
	} };
	
	/***************************************************************************/
	
	static MachineDriver machine_driver_timelimt = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				5000000,	/* 5.000 MHz */
				readmem,writemem,readport,null,
				timelimt_irq,1
			),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				18432000/6,	/* 3.072 MHz */
				readmem_sound,writemem_sound,readport_sound,writeport_sound,
				interrupt,1 /* ? */
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		50,
		timelimt_init,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 2*8, 30*8-1 ),
		gfxdecodeinfo,
		64, 64,
		timelimt_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		timelimt_vh_start,
		generic_vh_stop,
		timelimt_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_AY8910,
				ay8910_interface
			)
		}
	);
	
	
	/***************************************************************************
	
	  Game ROM(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_timelimt = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* ROMs */
		ROM_LOAD( "t8",     0x0000, 0x2000, 0x006767ca );	ROM_LOAD( "t7",     0x2000, 0x2000, 0xcbe7cd86 );	ROM_LOAD( "t6",     0x4000, 0x2000, 0xf5f17e39 );	ROM_LOAD( "t9",     0x6000, 0x2000, 0x2d72ab45 );
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* ROMs */
		ROM_LOAD( "tl5",    0x0000, 0x1000, 0x5b782e4a );	ROM_LOAD( "tl4",    0x1000, 0x1000, 0xa32883a9 );
		ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );/* tiles */
		ROM_LOAD( "tl11",   0x0000, 0x1000, 0x46676307 );	ROM_LOAD( "tl10",   0x1000, 0x1000, 0x2336908a );
		ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );/* tiles */
		ROM_LOAD( "tl13",   0x0000, 0x1000, 0x072e4053 );	ROM_LOAD( "tl12",   0x1000, 0x1000, 0xce960389 );
		ROM_REGION( 0x6000, REGION_GFX3, ROMREGION_DISPOSE );/* sprites */
		ROM_LOAD( "tl3",    0x0000, 0x2000, 0x01a9fd95 );	ROM_LOAD( "tl2",    0x2000, 0x2000, 0x4693b849 );	ROM_LOAD( "tl1",    0x4000, 0x2000, 0xc4007caf );
		ROM_REGION( 0x0040, REGION_PROMS, 0 );	ROM_LOAD( "clr.35", 0x0000, 0x0020, 0x9c9e6073 );	ROM_LOAD( "clr.48", 0x0020, 0x0020, BADCRC( 0xa0bcac59 ));	/* FIXED BITS (xxxxxx1x) */
	ROM_END(); }}; 
	
	
	
	public static GameDriver driver_timelimt	   = new GameDriver("1983"	,"timelimt"	,"timelimt.java"	,rom_timelimt,null	,machine_driver_timelimt	,input_ports_timelimt	,null	,ROT90	,	"Chuo Co. Ltd", "Time Limit", GAME_IMPERFECT_COLORS );
}
