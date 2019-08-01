/******************************************************************

Shark Attack
(C) 1980 PACIFIC NOVELTY MFG. INC.

Thief
(C) 1981 PACIFIC NOVELTY MFG. INC.

NATO Defense
(C) 1982 PACIFIC NOVELTY MFG. INC.

Credits:
	Shark Driver by Victor Trucco and Mike Balfour
	Driver for Thief and NATO Defense by Phil Stroffolino

- 8255 emulation (ports 0x30..0x3f) could be better abstracted

- TMS9927 VTAC: do we need to emulate this?
	The video controller registers effect screen size (currently
	hard-coded on a per-game basis).

- minor blitting glitches in playfield of Thief (XOR vs copy?)

- Nato Defense gfx ROMs may be hooked up wrong;
	see screenshots from flyers

******************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static WIP.mame056.vidhrdw.thief.*;

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

public class thief
{
	
	static int thief_input_select;
	
	
	
	
	public static InterruptPtr thief_interrupt = new InterruptPtr() { public int handler() 
	{
		/* SLAM switch causes an NMI if it's pressed */
		if( (input_port_3_r.handler(0) & 0x10) == 0 )
			return nmi_interrupt.handler();
	
		return interrupt.handler();
	} };
	
	/**********************************************************/
	
	
	/*	Following is an attempt to simulate the behavior of the
	**	cassette tape used in several Pacific Novelty games.
	**
	**	It is a leaderless tape that is constructed so that it will
	**	loop continuously.  The IO controller can start and stop the
	**	tape player's motor, and enable/disable each of two audio
	**	tracks.
	*/
	
	//enum
	//{
		public static int kTalkTrack=0;
                public static int kCrashTrack=1;
	//};
	
	static void tape_set_audio( int track, int bOn )
	{
		sample_set_volume( track, bOn!=0?100:0 );
	}
	
	static void tape_set_motor( int bOn )
	{
		if( bOn != 0 )
		{
			sample_start( 0, 0, 1 );
			sample_start( 1, 1, 1 );
		}
		else
		{
			sample_stop( kTalkTrack );
			sample_stop( kCrashTrack );
		}
	}
	
	/***********************************************************/
	
	public static WriteHandlerPtr thief_input_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		thief_input_select = data;
	} };
	
	public static WriteHandlerPtr tape_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		switch( data )
		{
		case 0x02: /* coin meter on */
			break;
	
		case 0x03: /* nop */
			break;
	
		case 0x04: /* coin meter off */
			break;
	
		case 0x08: /* talk track on */
			tape_set_audio( kTalkTrack, 1 );
			break;
	
		case 0x09: /* talk track off */
			sample_set_volume( kTalkTrack, 0 );
			break;
	
		case 0x0a: /* motor on */
			tape_set_motor( 1 );
			break;
	
		case 0x0b: /* motor off */
			tape_set_motor( 0 );
			break;
	
		case 0x0c: /* crash track on */
			tape_set_audio( kCrashTrack, 1 );
			break;
	
		case 0x0d: /* crash track off */
			tape_set_audio( kCrashTrack, 0 );
			break;
		}
	} };
	
	public static ReadHandlerPtr thief_io_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		switch( thief_input_select )
		{
			case 0x01: return readinputport(0); /* dsw#1 */
			case 0x02: return readinputport(1); /* dsw#2 */
			case 0x04: return readinputport(2); /* inp#1 */
			case 0x08: return readinputport(3); /* inp#2 */
		}
		return 0x00;
	} };
	
	public static Memory_ReadAddress sharkatt_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x8fff, MRA_RAM ),			/* 2114 (working RAM) */
		new Memory_ReadAddress( 0xc000, 0xdfff, thief_videoram_r ),	/* 4116 */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sharkatt_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x8fff, MWA_RAM ),			/* 2114 */
		new Memory_WriteAddress( 0xc000, 0xdfff, thief_videoram_w ),	/* 4116 */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress thief_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x8fff, MRA_RAM ),			/* 2114 (working RAM) */
		new Memory_ReadAddress( 0xa000, 0xafff, MRA_ROM ),			/* NATO Defense diagnostic ROM */
		new Memory_ReadAddress( 0xc000, 0xdfff, thief_videoram_r ),	/* 4116 */
		new Memory_ReadAddress( 0xe000, 0xe008, thief_coprocessor_r ),
		new Memory_ReadAddress( 0xe010, 0xe02f, MRA_ROM ),
		new Memory_ReadAddress( 0xe080, 0xe0bf, thief_context_ram_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress thief_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x0000, thief_blit_w ),
		new Memory_WriteAddress( 0x0001, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x8fff, MWA_RAM ),			/* 2114 */
		new Memory_WriteAddress( 0xc000, 0xdfff, thief_videoram_w ),	/* 4116 */
		new Memory_WriteAddress( 0xe000, 0xe008, thief_coprocessor_w ),
		new Memory_WriteAddress( 0xe010, 0xe02f, MWA_ROM ),
		new Memory_WriteAddress( 0xe080, 0xe0bf, thief_context_ram_w ),
		new Memory_WriteAddress( 0xe0c0, 0xe0c0, thief_context_bank_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static IO_ReadPort readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x31, 0x31, thief_io_r ), // 8255
		new IO_ReadPort( 0x41, 0x41, AY8910_read_port_0_r ),
		new IO_ReadPort( 0x43, 0x43, AY8910_read_port_1_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static IO_WritePort writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x00, 0x00, MWA_NOP ), /* watchdog */
		new IO_WritePort( 0x10, 0x10, thief_video_control_w ),
		new IO_WritePort( 0x30, 0x30, thief_input_select_w ), // 8255
		new IO_WritePort( 0x33, 0x33, tape_control_w ),
		new IO_WritePort( 0x40, 0x40, AY8910_control_port_0_w ),
		new IO_WritePort( 0x41, 0x41, AY8910_write_port_0_w ),
		new IO_WritePort( 0x42, 0x42, AY8910_control_port_1_w ),
		new IO_WritePort( 0x43, 0x43, AY8910_write_port_1_w ),
		new IO_WritePort( 0x50, 0x50, thief_color_plane_w ),
		new IO_WritePort( 0x60, 0x6f, thief_vtcsel_w ),
		new IO_WritePort( 0x70, 0x7f, thief_color_map_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	
	/**********************************************************/
	
	static InputPortPtr input_ports_sharkatt = new InputPortPtr(){ public void handler() { 
		PORT_START();       /* IN0 */
		PORT_DIPNAME( 0x7f, 0x7f, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x00, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x7f, DEF_STR( "1C_1C") ); // if any are set
		PORT_SERVICE( 0x80, IP_ACTIVE_HIGH );
	
		PORT_START();       /* IN1 */
		PORT_DIPNAME( 0x03, 0x00, DEF_STR( "Lives") );
		PORT_DIPSETTING(	0x00, "3" );
		PORT_DIPSETTING(	0x01, "4" );
		PORT_DIPSETTING(	0x02, "5" );
	//	PORT_DIPSETTING(	0x03, "5" );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(	0x00, DEF_STR( "No") );
		PORT_DIPSETTING(	0x04, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(	0x00, DEF_STR( "No") );
		PORT_DIPSETTING(	0x08, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(	0x00, DEF_STR( "No") );
		PORT_DIPSETTING(	0x10, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x20, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(	0x00, DEF_STR( "No") );
		PORT_DIPSETTING(	0x20, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(	0x00, DEF_STR( "No") );
		PORT_DIPSETTING(	0x40, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(	0x00, DEF_STR( "No") );
		PORT_DIPSETTING(	0x80, DEF_STR( "Yes") );
	
		PORT_START();       /* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON2 );
	
		PORT_START();       /* IN3 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_TILT );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_thief = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_DIPNAME( 0x03, 0x00, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x01, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0x02, DEF_STR( "1C_2C") );
		PORT_DIPNAME( 0x0c, 0x000, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "3" );
		PORT_DIPSETTING(    0x04, "4" );
		PORT_DIPSETTING(    0x08, "5" );
		PORT_DIPSETTING(    0x0c, "7" );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x20, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Yes") );
	
		PORT_START(); 
		PORT_DIPNAME( 0x0f, 0x00, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x00|0x0c, "10K" );
		PORT_DIPSETTING(    0x01|0x0c, "20K" );
		PORT_DIPSETTING(    0x02|0x0c, "30K" );
		PORT_DIPSETTING(    0x03|0x0c, "40K" );
		PORT_DIPSETTING(    0x00|0x08, "10K 10K" );
		PORT_DIPSETTING(    0x01|0x08, "20K 20K" );
		PORT_DIPSETTING(    0x02|0x08, "30K 30K" );
		PORT_DIPSETTING(    0x03|0x08, "40K 40K" );
		PORT_DIPSETTING(    0x00,      "None" );
		PORT_DIPNAME( 0xf0, 0x00, "Mode" );
		PORT_DIPSETTING(    0x00, "Normal" );
		PORT_DIPSETTING(    0x70, "Display Options" );
		PORT_DIPSETTING(    0x80|0x00, "Burn-in Test" );
		PORT_DIPSETTING(    0x80|0x10, "Color Bar Test" );
		PORT_DIPSETTING(    0x80|0x20, "Cross Hatch" );
		PORT_DIPSETTING(    0x80|0x30, "Color Map" );
		PORT_DIPSETTING(    0x80|0x40, "VIDSEL Test" );
		PORT_DIPSETTING(    0x80|0x50, "VIDBIT Test" );
		PORT_DIPSETTING(    0x80|0x60, "I/O Board Test" );
		PORT_DIPSETTING(    0x80|0x70, "Reserved" );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_TILT );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_natodef = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_DIPNAME( 0x03, 0x00, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x01, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0x02, DEF_STR( "1C_2C") );
		PORT_DIPNAME( 0x0c, 0x000, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "3" );
		PORT_DIPSETTING(    0x04, "4" );
		PORT_DIPSETTING(    0x08, "5" );
		PORT_DIPSETTING(    0x0c, "7" );
		PORT_DIPNAME( 0x30, 0x00, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x00, "Easy" );
		PORT_DIPSETTING(    0x10, "Medium" );
		PORT_DIPSETTING(    0x20, "Hard" );
		PORT_DIPSETTING(    0x30, "Hardest" );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x80, 0x00, "Add a Coin?" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Yes") );
	
		PORT_START(); 
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x0b, 0x00, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x08, "10K" );
		PORT_DIPSETTING(    0x09, "20K" );
		PORT_DIPSETTING(    0x0a, "30K" );
		PORT_DIPSETTING(    0x0b, "40K" );
		PORT_DIPSETTING(    0x00, "None" );
		PORT_DIPNAME( 0xf0, 0x00, "Mode" );
		PORT_DIPSETTING(    0x00, "Normal" );
		PORT_DIPSETTING(    0x70, "Display Options" );
		PORT_DIPSETTING(    0x80|0x00, "Burn-in Test" );
		PORT_DIPSETTING(    0x80|0x10, "Color Bar Test" );
		PORT_DIPSETTING(    0x80|0x20, "Cross Hatch" );
		PORT_DIPSETTING(    0x80|0x30, "Color Map" );
		PORT_DIPSETTING(    0x80|0x40, "VIDSEL Test" );
		PORT_DIPSETTING(    0x80|0x50, "VIDBIT Test" );
		PORT_DIPSETTING(    0x80|0x60, "I/O Board Test" );
		PORT_DIPSETTING(    0x80|0x70, "Reserved" );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_TILT );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	/**********************************************************/
	
	static AY8910interface ay8910_interface = new AY8910interface
	(
		2,	/* 2 chips */
		4000000/4,	/* Z80 Clock / 4 */
		new int[] { 50, 50 },
		new ReadHandlerPtr[] { null, null },
		new ReadHandlerPtr[] { null, null },
		new WriteHandlerPtr[] { null, null },
		new WriteHandlerPtr[] { null, null }
	);
	
	/***********************************************************/
	
	static String sharkatt_sample_names[] =
	{
		"*sharkatt",
		"talk.wav",
		"crash.wav",
		null	/* end of array */
	};
	
	static Samplesinterface sharkatt_samples_interface = new Samplesinterface
        (
		2,	/* number of channels */
		50,	/* volume */
		sharkatt_sample_names
        );
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	static String thief_sample_names[] =
	{
		"*thief",
		"talk.wav",
		"crash.wav",
		null	/* end of array */
	};
	
	static Samplesinterface thief_samples_interface = new Samplesinterface
        (
		2,	/* number of channels */
		50,	/* volume */
		thief_sample_names
	);
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	static String natodef_sample_names[] =
	{
		"*natodef",
		"talk.wav",
		"crash.wav",
		null	/* end of array */
	};
	
	static Samplesinterface natodef_samples_interface = new Samplesinterface
        (
		2,	/* number of channels */
		50,	/* volume */
		natodef_sample_names
	);
	
	
	
	static MachineDriver machine_driver_sharkatt = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				4000000,        /* 4 MHz? */
				sharkatt_readmem,sharkatt_writemem,readport,writeport,
				thief_interrupt,1
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,
		1,      /* single CPU, no need for interleaving */
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 24*8-1 ),
		null,      /* no gfxdecodeinfo - bitmapped display */
		16, 0,
		null,
	
		VIDEO_TYPE_RASTER,
		null,
		thief_vh_start,
		thief_vh_stop,
		thief_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_AY8910,
				ay8910_interface
			),
			new MachineSound(
				SOUND_SAMPLES,
				sharkatt_samples_interface
			)
		}
	);
	
	static MachineDriver machine_driver_thief = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				4000000, /* 4 MHz? */
				thief_readmem,thief_writemem,readport,writeport,
				thief_interrupt,1
			),
		},
		60, DEFAULT_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
		1, /* single CPU, no need for interleaving */
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 32*8-1 ),
		null,
		16, 0,
		null,
	
		VIDEO_TYPE_RASTER,
		null,
		thief_vh_start,
		thief_vh_stop,
		thief_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_AY8910,
				ay8910_interface
			),
			new MachineSound(
				SOUND_SAMPLES,
				thief_samples_interface
			)
		}
	);
	
	static MachineDriver machine_driver_natodef = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				4000000, /* 4 MHz? */
				thief_readmem,thief_writemem,readport,writeport,
				thief_interrupt,1
			),
		},
		60, DEFAULT_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
		1, /* single CPU, no need for interleaving */
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 32*8-1 ),
		null,
		16, 0,
		null,
	
		VIDEO_TYPE_RASTER,
		null,
		thief_vh_start,
		thief_vh_stop,
		thief_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_AY8910,
				ay8910_interface
			),
			new MachineSound(
				SOUND_SAMPLES,
				natodef_samples_interface
			)
		}
	);
	
	/**********************************************************/
	
	static RomLoadPtr rom_sharkatt = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );    /* 64k for code */
		ROM_LOAD( "sharkatt.0",   0x0000, 0x800, 0xc71505e9 );
		ROM_LOAD( "sharkatt.1",   0x0800, 0x800, 0x3e3abf70 );
		ROM_LOAD( "sharkatt.2",   0x1000, 0x800, 0x96ded944 );
		ROM_LOAD( "sharkatt.3",   0x1800, 0x800, 0x007283ae );
		ROM_LOAD( "sharkatt.4a",  0x2000, 0x800, 0x5cb114a7 );
		ROM_LOAD( "sharkatt.5",   0x2800, 0x800, 0x1d88aaad );
		ROM_LOAD( "sharkatt.6",   0x3000, 0x800, 0xc164bad4 );
		ROM_LOAD( "sharkatt.7",   0x3800, 0x800, 0xd78c4b8b );
		ROM_LOAD( "sharkatt.8",   0x4000, 0x800, 0x5958476a );
		ROM_LOAD( "sharkatt.9",   0x4800, 0x800, 0x4915eb37 );
		ROM_LOAD( "sharkatt.10",  0x5000, 0x800, 0x9d07cb68 );
		ROM_LOAD( "sharkatt.11",  0x5800, 0x800, 0x21edc962 );
		ROM_LOAD( "sharkatt.12a", 0x6000, 0x800, 0x5dd8785a );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_thief = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* Z80 code */
		ROM_LOAD( "t8a0ah0a",	0x0000, 0x1000, 0xedbbf71c );
		ROM_LOAD( "t2662h2",	0x1000, 0x1000, 0x85b4f6ff );
		ROM_LOAD( "tc162h4",	0x2000, 0x1000, 0x70478a82 );
		ROM_LOAD( "t0cb4h6",	0x3000, 0x1000, 0x29de0425 );
		ROM_LOAD( "tc707h8",	0x4000, 0x1000, 0xea8dd847 );
		ROM_LOAD( "t857bh10",	0x5000, 0x1000, 0x403c33b7 );
		ROM_LOAD( "t606bh12",	0x6000, 0x1000, 0x4ca2748b );
		ROM_LOAD( "tae4bh14",	0x7000, 0x1000, 0x22e7dcc3 );/* diagnostics ROM */
	
		ROM_REGION( 0x400, REGION_CPU2, 0 );/* coprocessor */
		ROM_LOAD( "b8",			0x000, 0x0200, 0xfe865b2a );
		/* B8 is a function dispatch table for the coprocessor (unused) */
		ROM_LOAD( "c8", 		0x200, 0x0200, 0x7ed5c923 );
		/* C8 is mapped (banked) in CPU1's address space; it contains Z80 code */
	
		ROM_REGION( 0x6000, REGION_GFX1, 0 );/* image ROMs for coprocessor */
		ROM_LOAD16_BYTE( "t079ahd4" ,  0x0001, 0x1000, 0x928bd8ef );
		ROM_LOAD16_BYTE( "tdda7hh4" ,  0x0000, 0x1000, 0xb48f0862 );
		/* next 0x4000 bytes are unmapped (used by Nato Defense) */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_natodef = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* Z80 code */
		ROM_LOAD( "natodef.cp0",	0x0000, 0x1000, 0x8397c787 );
		ROM_LOAD( "natodef.cp2",	0x1000, 0x1000, 0x8cfbf26f );
		ROM_LOAD( "natodef.cp4",	0x2000, 0x1000, 0xb4c90fb2 );
		ROM_LOAD( "natodef.cp6",	0x3000, 0x1000, 0xc6d0d35e );
		ROM_LOAD( "natodef.cp8",	0x4000, 0x1000, 0xe4b6c21e );
		ROM_LOAD( "natodef.cpa",	0x5000, 0x1000, 0x888ecd42 );
		ROM_LOAD( "natodef.cpc",	0x6000, 0x1000, 0xcf713bc9 );
		ROM_LOAD( "natodef.cpe",	0x7000, 0x1000, 0x4eef6bf4 );
		ROM_LOAD( "natodef.cp5",	0xa000, 0x1000, 0x65c3601b );/* diagnostics ROM */
	
		ROM_REGION( 0x400, REGION_CPU2, 0 );/* coprocessor */
		ROM_LOAD( "b8",			0x000, 0x0200, 0xfe865b2a );
		ROM_LOAD( "c8", 		0x200, 0x0200, 0x7ed5c923 );
		/* C8 is mapped (banked) in CPU1's address space; it contains Z80 code */
	
		ROM_REGION( 0x6000, REGION_GFX1, 0 );/* image ROMs for coprocessor */
		ROM_LOAD16_BYTE( "natodef.o4",	0x0001, 0x1000, 0x39a868f8 );
		ROM_LOAD16_BYTE( "natodef.e1",	0x0000, 0x1000, 0xb6d1623d );
		ROM_LOAD16_BYTE( "natodef.o2",	0x2001, 0x1000, 0x77cc9cfd );
		ROM_LOAD16_BYTE( "natodef.e3",	0x2000, 0x1000, 0x5302410d );
		ROM_LOAD16_BYTE( "natodef.o3",	0x4001, 0x1000, 0xb217909a );
		ROM_LOAD16_BYTE( "natodef.e2",	0x4000, 0x1000, 0x886c3f05 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_natodefa = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* Z80 code */
		ROM_LOAD( "natodef.cp0",	0x0000, 0x1000, 0x8397c787 );
		ROM_LOAD( "natodef.cp2",	0x1000, 0x1000, 0x8cfbf26f );
		ROM_LOAD( "natodef.cp4",	0x2000, 0x1000, 0xb4c90fb2 );
		ROM_LOAD( "natodef.cp6",	0x3000, 0x1000, 0xc6d0d35e );
		ROM_LOAD( "natodef.cp8",	0x4000, 0x1000, 0xe4b6c21e );
		ROM_LOAD( "natodef.cpa",	0x5000, 0x1000, 0x888ecd42 );
		ROM_LOAD( "natodef.cpc",	0x6000, 0x1000, 0xcf713bc9 );
		ROM_LOAD( "natodef.cpe",	0x7000, 0x1000, 0x4eef6bf4 );
		ROM_LOAD( "natodef.cp5",	0xa000, 0x1000, 0x65c3601b );/* diagnostics ROM */
	
		ROM_REGION( 0x400, REGION_CPU2, 0 );/* coprocessor */
		ROM_LOAD( "b8",			0x000, 0x0200, 0xfe865b2a );
		ROM_LOAD( "c8", 		0x200, 0x0200, 0x7ed5c923 );
		/* C8 is mapped (banked) in CPU1's address space; it contains Z80 code */
	
		ROM_REGION( 0x6000, REGION_GFX1, 0 );/* image ROMs for coprocessor */
		ROM_LOAD16_BYTE( "natodef.o4",	0x0001, 0x1000, 0x39a868f8 );
		ROM_LOAD16_BYTE( "natodef.e1",	0x0000, 0x1000, 0xb6d1623d );
		ROM_LOAD16_BYTE( "natodef.o3",	0x2001, 0x1000, 0xb217909a );/* same ROMs as natodef, */
		ROM_LOAD16_BYTE( "natodef.e2",	0x2000, 0x1000, 0x886c3f05 );/* but in a different */
		ROM_LOAD16_BYTE( "natodef.o2",	0x4001, 0x1000, 0x77cc9cfd );/* order to give */
		ROM_LOAD16_BYTE( "natodef.e3",	0x4000, 0x1000, 0x5302410d );/* different mazes */
	ROM_END(); }}; 
	
	
	public static InitDriverPtr init_thief = new InitDriverPtr() { public void handler()
	{
		UBytePtr dest = new UBytePtr(memory_region( REGION_CPU1 ));
		UBytePtr source = new UBytePtr(memory_region( REGION_CPU2 ));
	
		/* C8 is mapped (banked) in CPU1's address space; it contains Z80 code */
		memcpy( new UBytePtr(dest, 0xe010), new UBytePtr(source, 0x290), 0x20 );
	} };
	
	
	public static GameDriver driver_sharkatt	   = new GameDriver("1980"	,"sharkatt"	,"thief.java"	,rom_sharkatt,null	,machine_driver_sharkatt	,input_ports_sharkatt	,null	,ROT0	,	"Pacific Novelty", "Shark Attack" );
	public static GameDriver driver_thief	   = new GameDriver("1981"	,"thief"	,"thief.java"	,rom_thief,null	,machine_driver_thief	,input_ports_thief	,init_thief	,ROT0	,	"Pacific Novelty", "Thief" );
	public static GameDriver driver_natodef	   = new GameDriver("1982"	,"natodef"	,"thief.java"	,rom_natodef,null	,machine_driver_natodef	,input_ports_natodef	,init_thief	,ROT0	,	"Pacific Novelty", "NATO Defense"  );
	public static GameDriver driver_natodefa	   = new GameDriver("1982"	,"natodefa"	,"thief.java"	,rom_natodefa,driver_natodef	,machine_driver_natodef	,input_ports_natodef	,init_thief	,ROT0	,	"Pacific Novelty", "NATO Defense (alternate mazes)"  );
}
