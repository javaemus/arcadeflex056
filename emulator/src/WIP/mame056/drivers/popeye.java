/***************************************************************************

Popeye  (c) 1982 Nintendo

driver by Marc Lafontaine

To enter service mode, reset keeping the service button pressed.

Notes:
- The main set has a protection device mapped at E000/E001. The second set
  (which is the same revision of the program code) has the protection disabled
  in a very clean way, so I don't know if it's an original (without the
  protection device to save costs), or a very well done bootleg.
- The bootleg derives from a different revision of the program code which we
  don't have.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.driverH.*;
import static mame056.timerH.*;

import static common.libc.cstdlib.*;
import static common.libc.cstring.*;

import static mame056.cpuintrfH.*;
import static mame056.cpuexecH.*;
import static mame056.mame.*;
import static mame056.palette.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.inputH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.sndintrfH.*;
import static mame056.cpuexec.*;

import static mame056.sndintrf.*;
import static mame056.sound.ay8910.*;
import static mame056.sound.ay8910H.*;
import static mame056.vidhrdw.generic.*;

import static WIP.mame056.vidhrdw.popeye.*;

public class popeye
{
	/* the protection device simply returns the last two values written shifted left */
	/* by a variable amount. */
	static int prot0,prot1,prot_shift;
	
	public static ReadHandlerPtr protection_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (offset == 0)
		{
			return ((prot1 << prot_shift) | (prot0 >> (8-prot_shift))) & 0xff;
		}
		else	/* offset == 1 */
		{
			/* the game just checks is bit 2 is clear. Returning 0 seems to be enough. */
			return 0;
		}
	} };
	
	public static WriteHandlerPtr protection_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (offset == 0)
		{
			/* this is the same as the level number (1-3) */
			prot_shift = data & 0x07;
		}
		else	/* offset == 1 */
		{
			prot0 = prot1;
			prot1 = data;
		}
	} };
	
	
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),
		new Memory_ReadAddress( 0x8800, 0x8bff, MRA_RAM ),
		new Memory_ReadAddress( 0x8c00, 0x8e7f, MRA_RAM ),
		new Memory_ReadAddress( 0x8e80, 0x8fff, MRA_RAM ),
		new Memory_ReadAddress( 0xe000, 0xe001, protection_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM ),
		new Memory_WriteAddress( 0x8800, 0x8bff, MWA_RAM ),
		new Memory_WriteAddress( 0x8c00, 0x8c01, MWA_RAM, popeye_background_pos ),
		new Memory_WriteAddress( 0x8c03, 0x8c03, MWA_RAM, popeye_palettebank ),
		new Memory_WriteAddress( 0x8c04, 0x8e7f, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0x8e80, 0x8fff, MWA_RAM ),
		new Memory_WriteAddress( 0xa000, 0xa7ff, MWA_RAM, popeye_textram ),
		new Memory_WriteAddress( 0xc000, 0xdfff, popeye_bitmap_w ),
		new Memory_WriteAddress( 0xe000, 0xe001, protection_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_ReadAddress popeyebl_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),
		new Memory_ReadAddress( 0x8c00, 0x8e7f, MRA_RAM ),
		new Memory_ReadAddress( 0x8e80, 0x8fff, MRA_RAM ),
		new Memory_ReadAddress( 0xe000, 0xe01f, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress popeyebl_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM ),
		new Memory_WriteAddress( 0x8c00, 0x8c01, MWA_RAM, popeye_background_pos ),
		new Memory_WriteAddress( 0x8c03, 0x8c03, MWA_RAM, popeye_palettebank ),
		new Memory_WriteAddress( 0x8c04, 0x8e7f, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0x8e80, 0x8fff, MWA_RAM ),
		new Memory_WriteAddress( 0xa000, 0xa7ff, MWA_RAM, popeye_textram ),
		new Memory_WriteAddress( 0xc000, 0xcfff, popeyebl_bitmap_w ),
		new Memory_WriteAddress( 0xe000, 0xe01f, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static IO_ReadPort readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_ReadPort( 0x00, 0x00, input_port_0_r ),
		new IO_ReadPort( 0x01, 0x01, input_port_1_r ),
		new IO_ReadPort( 0x02, 0x02, input_port_2_r ),
		new IO_ReadPort( 0x03, 0x03, AY8910_read_port_0_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	public static IO_WritePort writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_WritePort( 0x00, 0x00, AY8910_control_port_0_w ),
		new IO_WritePort( 0x01, 0x01, AY8910_write_port_0_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	static InputPortPtr input_ports_popeye = new InputPortPtr(){ public void handler() { 
	PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY );	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_4WAY );	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_4WAY );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_4WAY );	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_COCKTAIL );	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_START1 );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_START2 );	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_COIN2 );	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_SERVICE1 );	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_COIN1 );
		PORT_START(); 	/* DSW0 */
		PORT_DIPNAME( 0x0f, 0x0f, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x03, "A 3/1 B 1/2" );	PORT_DIPSETTING(    0x0e, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x01, "A 2/1 B 2/5" );	PORT_DIPSETTING(    0x04, "A 2/1 B 1/3" );	PORT_DIPSETTING(    0x07, "A 1/1 B 2/1" );	PORT_DIPSETTING(    0x0f, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x0c, "A 1/1 B 1/2" );	PORT_DIPSETTING(    0x0d, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x06, "A 1/2 B 1/4" );	PORT_DIPSETTING(    0x0b, "A 1/2 B 1/5" );	PORT_DIPSETTING(    0x02, "A 2/5 B 1/1" );	PORT_DIPSETTING(    0x0a, "A 1/3 B 1/1" );	PORT_DIPSETTING(    0x09, "A 1/4 B 1/1" );	PORT_DIPSETTING(    0x05, "A 1/5 B 1/1" );	PORT_DIPSETTING(    0x08, "A 1/6 B 1/1" );	PORT_DIPSETTING(    0x00, DEF_STR( "Free_Play") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x60, 0x40, "Copyright" );	PORT_DIPSETTING(    0x40, "Nintendo" );	PORT_DIPSETTING(    0x20, "Nintendo Co.,Ltd" );	PORT_DIPSETTING(    0x60, "Nintendo of America" );//	PORT_DIPSETTING(    0x00, "Nintendo of America" );	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_SPECIAL );/* scans DSW1 one bit at a time */
	
		PORT_START(); 	/* DSW1 (FAKE - appears as bit 7 of DSW0, see code below) */
		PORT_DIPNAME( 0x03, 0x01, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x03, "1" );	PORT_DIPSETTING(    0x02, "2" );	PORT_DIPSETTING(    0x01, "3" );	PORT_DIPSETTING(    0x00, "4" );	PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x0c, "Easy" );	PORT_DIPSETTING(    0x08, "Medium" );	PORT_DIPSETTING(    0x04, "Hard" );	PORT_DIPSETTING(    0x00, "Hardest" );	PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x30, "40000" );	PORT_DIPSETTING(    0x20, "60000" );	PORT_DIPSETTING(    0x10, "80000" );	PORT_DIPSETTING(    0x00, "None" );	PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Cocktail") );
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_popeyef = new InputPortPtr(){ public void handler() { 
	PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY );	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_4WAY );	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_4WAY );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_4WAY );	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_4WAY | IPF_COCKTAIL );	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_4WAY | IPF_COCKTAIL );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_4WAY | IPF_COCKTAIL );	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_COCKTAIL );	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_START1 );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_START2 );	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_COIN2 );	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_SERVICE1 );	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_COIN1 );
		PORT_START(); 	/* DSW0 */
		PORT_DIPNAME( 0x0f, 0x0f, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x03, "A 3/1 B 1/2" );	PORT_DIPSETTING(    0x0e, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x01, "A 2/1 B 2/5" );	PORT_DIPSETTING(    0x04, "A 2/1 B 1/3" );	PORT_DIPSETTING(    0x07, "A 1/1 B 2/1" );	PORT_DIPSETTING(    0x0f, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x0c, "A 1/1 B 1/2" );	PORT_DIPSETTING(    0x0d, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x06, "A 1/2 B 1/4" );	PORT_DIPSETTING(    0x0b, "A 1/2 B 1/5" );	PORT_DIPSETTING(    0x02, "A 2/5 B 1/1" );	PORT_DIPSETTING(    0x0a, "A 1/3 B 1/1" );	PORT_DIPSETTING(    0x09, "A 1/4 B 1/1" );	PORT_DIPSETTING(    0x05, "A 1/5 B 1/1" );	PORT_DIPSETTING(    0x08, "A 1/6 B 1/1" );	PORT_DIPSETTING(    0x00, DEF_STR( "Free_Play") );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x60, 0x40, "Copyright" );	PORT_DIPSETTING(    0x40, "Nintendo" );	PORT_DIPSETTING(    0x20, "Nintendo Co.,Ltd" );	PORT_DIPSETTING(    0x60, "Nintendo of America" );//	PORT_DIPSETTING(    0x00, "Nintendo of America" );	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_SPECIAL );/* scans DSW1 one bit at a time */
	
		PORT_START(); 	/* DSW1 (FAKE - appears as bit 7 of DSW0, see code below) */
		PORT_DIPNAME( 0x03, 0x01, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x03, "1" );	PORT_DIPSETTING(    0x02, "2" );	PORT_DIPSETTING(    0x01, "3" );	PORT_DIPSETTING(    0x00, "4" );	PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x0c, "Easy" );	PORT_DIPSETTING(    0x08, "Medium" );	PORT_DIPSETTING(    0x04, "Hard" );	PORT_DIPSETTING(    0x00, "Hardest" );	PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x30, "20000" );	PORT_DIPSETTING(    0x20, "30000" );	PORT_DIPSETTING(    0x10, "50000" );	PORT_DIPSETTING(    0x00, "None" );	PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Cocktail") );
	INPUT_PORTS_END(); }}; 
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		16,16,	/* 16*16 characters (8*8 doubled) */
		256,	/* 256 characters */
		1,	/* 1 bit per pixel (there are two bitplanes in the ROM, but only one is used) */
		new int[] { 0 },
		new int[] { 7,7, 6,6, 5,5, 4,4, 3,3, 2,2, 1,1, 0,0 },	/* pretty straightforward layout */
		new int[] { 0*8,0*8, 1*8,1*8, 2*8,2*8, 3*8,3*8, 4*8,4*8, 5*8,5*8, 6*8,6*8, 7*8,7*8 },
		8*8	/* every char takes 8 consecutive bytes */
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,	/* 16*16 sprites */
		512,	/* 512 sprites */
		2,	/* 2 bits per pixel */
		new int[] { 0, 0x4000*8 },	/* the two bitplanes are separated in different files */
		new int[] {7+(0x2000*8),6+(0x2000*8),5+(0x2000*8),4+(0x2000*8),
		 3+(0x2000*8),2+(0x2000*8),1+(0x2000*8),0+(0x2000*8),
		 7,6,5,4,3,2,1,0 },
		new int[] { 15*8, 14*8, 13*8, 12*8, 11*8, 10*8, 9*8, 8*8,
		  7*8, 6*8, 5*8, 4*8, 3*8, 2*8, 1*8, 0*8, },
		16*8	/* every sprite takes 16 consecutive bytes */
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0x0000, charlayout,      0, 16 ),	/* chars */
		new GfxDecodeInfo( REGION_GFX2, 0x0000, spritelayout, 16*2, 64 ),	/* sprites */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	static int dswbit;
	
	public static WriteHandlerPtr popeye_portB_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bit 0 does something - RV in the schematics */
	
		/* bits 1-3 select DSW1 bit to read */
		dswbit = (data & 0x0e) >> 1;
	} };
	
	public static ReadHandlerPtr popeye_portA_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int res;
	
	
		res = input_port_3_r.handler(offset);
		res |= (input_port_4_r.handler(offset) << (7-dswbit)) & 0x80;
	
		return res;
	} };
	
	static AY8910interface ay8910_interface = new AY8910interface
	(
		1,	/* 1 chip */
		2000000,	/* 2 MHz */
		new int[] { 40 },
		new ReadHandlerPtr[] { popeye_portA_r },
		new ReadHandlerPtr[] { null },
		new WriteHandlerPtr[] { null },
		new WriteHandlerPtr[] { popeye_portB_w }
	);
	
	
	
	static MachineDriver machine_driver_popeye = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				4000000,	/* 4 MHz */
				readmem,writemem,readport,writeport,
				nmi_interrupt,2
			)
		},
		30, DEFAULT_30HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,	/* single CPU, no need for interleaving */
		null,
	
		/* video hardware */
		32*16, 32*16, new rectangle( 0*16, 32*16-1, 2*16, 30*16-1 ),
		gfxdecodeinfo,
		16+16+256, 16*2+64*4,
		popeye_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		popeye_vh_start,
		popeye_vh_stop,
		popeye_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_AY8910,
				ay8910_interface
			)
		}
	);
	
	static MachineDriver machine_driver_popeyebl = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				4000000,	/* 4 MHz */
				popeyebl_readmem,popeyebl_writemem,readport,writeport,
				nmi_interrupt,2
			)
		},
		30, DEFAULT_30HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,	/* single CPU, no need for interleaving */
		null,
	
		/* video hardware */
		32*16, 32*16, new rectangle( 0*16, 32*16-1, 2*16, 30*16-1 ),
		gfxdecodeinfo,
		16+16+256, 16*2+64*4,
		popeyebl_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		popeye_vh_start,
		popeye_vh_stop,
		popeye_vh_screenrefresh,
	
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
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_popeye = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "c-7a",         0x0000, 0x2000, 0x9af7c821 );	ROM_LOAD( "c-7b",         0x2000, 0x2000, 0xc3704958 );	ROM_LOAD( "c-7c",         0x4000, 0x2000, 0x5882ebf9 );	ROM_LOAD( "c-7e",         0x6000, 0x2000, 0xef8649ca );
		ROM_REGION( 0x0800, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "v-5n",         0x0000, 0x0800, 0xcca61ddd );/* first half is empty */
		ROM_CONTINUE(             0x0000, 0x0800 );
		ROM_REGION( 0x8000, REGION_GFX2, ROMREGION_DISPOSE );	ROM_LOAD( "v-1e",         0x0000, 0x2000, 0x0f2cd853 );	ROM_LOAD( "v-1f",         0x2000, 0x2000, 0x888f3474 );	ROM_LOAD( "v-1j",         0x4000, 0x2000, 0x7e864668 );	ROM_LOAD( "v-1k",         0x6000, 0x2000, 0x49e1d170 );
		ROM_REGION( 0x0340, REGION_PROMS, 0 );	ROM_LOAD( "prom-cpu.4a",  0x0000, 0x0020, 0x375e1602 );/* background palette */
		ROM_LOAD( "prom-cpu.3a",  0x0020, 0x0020, 0xe950bea1 );/* char palette */
		ROM_LOAD( "prom-cpu.5b",  0x0040, 0x0100, 0xc5826883 );/* sprite palette - low 4 bits */
		ROM_LOAD( "prom-cpu.5a",  0x0140, 0x0100, 0xc576afba );/* sprite palette - high 4 bits */
		ROM_LOAD( "prom-vid.7j",  0x0240, 0x0100, 0xa4655e2e );/* timing for the protection ALU */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_popeyeu = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "7a",           0x0000, 0x2000, 0x0bd04389 );	ROM_LOAD( "7b",           0x2000, 0x2000, 0xefdf02c3 );	ROM_LOAD( "7c",           0x4000, 0x2000, 0x8eee859e );	ROM_LOAD( "7e",           0x6000, 0x2000, 0xb64aa314 );
		ROM_REGION( 0x0800, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "v-5n",         0x0000, 0x0800, 0xcca61ddd );/* first half is empty */
		ROM_CONTINUE(             0x0000, 0x0800 );
		ROM_REGION( 0x8000, REGION_GFX2, ROMREGION_DISPOSE );	ROM_LOAD( "v-1e",         0x0000, 0x2000, 0x0f2cd853 );	ROM_LOAD( "v-1f",         0x2000, 0x2000, 0x888f3474 );	ROM_LOAD( "v-1j",         0x4000, 0x2000, 0x7e864668 );	ROM_LOAD( "v-1k",         0x6000, 0x2000, 0x49e1d170 );
		ROM_REGION( 0x0340, REGION_PROMS, 0 );	ROM_LOAD( "prom-cpu.4a",  0x0000, 0x0020, 0x375e1602 );/* background palette */
		ROM_LOAD( "prom-cpu.3a",  0x0020, 0x0020, 0xe950bea1 );/* char palette */
		ROM_LOAD( "prom-cpu.5b",  0x0040, 0x0100, 0xc5826883 );/* sprite palette - low 4 bits */
		ROM_LOAD( "prom-cpu.5a",  0x0140, 0x0100, 0xc576afba );/* sprite palette - high 4 bits */
		ROM_LOAD( "prom-vid.7j",  0x0240, 0x0100, 0xa4655e2e );/* timing for the protection ALU */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_popeyef = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "tpp2-c_f.7a",  0x0000, 0x2000, 0x5fc5264d );	ROM_LOAD( "tpp2-c_f.7b",  0x2000, 0x2000, 0x51de48e8 );	ROM_LOAD( "tpp2-c_f.7c",  0x4000, 0x2000, 0x62df9647 );	ROM_LOAD( "tpp2-c_f.7e",  0x6000, 0x2000, 0xf31e7916 );
		ROM_REGION( 0x0800, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "v-5n",         0x0000, 0x0800, 0xcca61ddd );/* first half is empty */
		ROM_CONTINUE(             0x0000, 0x0800 );
		ROM_REGION( 0x8000, REGION_GFX2, ROMREGION_DISPOSE );	ROM_LOAD( "v-1e",         0x0000, 0x2000, 0x0f2cd853 );	ROM_LOAD( "v-1f",         0x2000, 0x2000, 0x888f3474 );	ROM_LOAD( "v-1j",         0x4000, 0x2000, 0x7e864668 );	ROM_LOAD( "v-1k",         0x6000, 0x2000, 0x49e1d170 );
		ROM_REGION( 0x0340, REGION_PROMS, 0 );	ROM_LOAD( "prom-cpu.4a",  0x0000, 0x0020, 0x375e1602 );/* background palette */
		ROM_LOAD( "prom-cpu.3a",  0x0020, 0x0020, 0xe950bea1 );/* char palette */
		ROM_LOAD( "prom-cpu.5b",  0x0040, 0x0100, 0xc5826883 );/* sprite palette - low 4 bits */
		ROM_LOAD( "prom-cpu.5a",  0x0140, 0x0100, 0xc576afba );/* sprite palette - high 4 bits */
		ROM_LOAD( "prom-vid.7j",  0x0240, 0x0100, 0xa4655e2e );/* timing for the protection ALU */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_popeyebl = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "po1",          0x0000, 0x2000, 0xb14a07ca );	ROM_LOAD( "po2",          0x2000, 0x2000, 0x995475ff );	ROM_LOAD( "po3",          0x4000, 0x2000, 0x99d6a04a );	ROM_LOAD( "po4",          0x6000, 0x2000, 0x548a6514 );	ROM_LOAD( "po_d1-e1.bin", 0xe000, 0x0020, 0x8de22998 );/* protection PROM */
	
		ROM_REGION( 0x0800, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "v-5n",         0x0000, 0x0800, 0xcca61ddd );/* first half is empty */
		ROM_CONTINUE(             0x0000, 0x0800 );
		ROM_REGION( 0x8000, REGION_GFX2, ROMREGION_DISPOSE );	ROM_LOAD( "v-1e",         0x0000, 0x2000, 0x0f2cd853 );	ROM_LOAD( "v-1f",         0x2000, 0x2000, 0x888f3474 );	ROM_LOAD( "v-1j",         0x4000, 0x2000, 0x7e864668 );	ROM_LOAD( "v-1k",         0x6000, 0x2000, 0x49e1d170 );
		ROM_REGION( 0x0240, REGION_PROMS, 0 );	ROM_LOAD( "popeye.pr1",   0x0000, 0x0020, 0xd138e8a4 );/* background palette */
		ROM_LOAD( "popeye.pr2",   0x0020, 0x0020, 0x0f364007 );/* char palette */
		ROM_LOAD( "popeye.pr3",   0x0040, 0x0100, 0xca4d7b6a );/* sprite palette - low 4 bits */
		ROM_LOAD( "popeye.pr4",   0x0140, 0x0100, 0xcab9bc53 );/* sprite palette - high 4 bits */
	ROM_END(); }}; 
	
	
	
	public static InitDriverPtr init_popeye = new InitDriverPtr() { public void handler() 
	{
		UBytePtr buffer;
		UBytePtr rom = new UBytePtr(memory_region(REGION_CPU1));
		int len = 0x10000;
	
		/* decrypt the program ROMs */
		if ((buffer = new UBytePtr(len)) != null)
		{
			int i;
			for (i = 0;i < len; i++)
				buffer.write(i, BITSWAP8(rom.read(BITSWAP16(i,15,14,13,12,11,10,8,7,6,3,9,5,4,2,1,0) ^ 0x3f),3,4,2,5,1,6,0,7));
			memcpy(rom,buffer,len);
			buffer = null;
		}
	} };
	
	
	public static GameDriver driver_popeye	   = new GameDriver("1982"	,"popeye"	,"popeye.java"	,rom_popeye,null	,machine_driver_popeye	,input_ports_popeye	,init_popeye	,ROT0	,	"Nintendo", "Popeye (revision D)", GAME_NO_COCKTAIL );
	public static GameDriver driver_popeyeu	   = new GameDriver("1982"	,"popeyeu"	,"popeye.java"	,rom_popeyeu,driver_popeye	,machine_driver_popeye	,input_ports_popeye	,init_popeye	,ROT0	,	"Nintendo", "Popeye (revision D not protected)", GAME_NO_COCKTAIL );
	public static GameDriver driver_popeyef	   = new GameDriver("1982"	,"popeyef"	,"popeye.java"	,rom_popeyef,driver_popeye	,machine_driver_popeye	,input_ports_popeyef	,init_popeye	,ROT0	,	"Nintendo", "Popeye (revision F)", GAME_NO_COCKTAIL );
	public static GameDriver driver_popeyebl	   = new GameDriver("1982"	,"popeyebl"	,"popeye.java"	,rom_popeyebl,driver_popeye	,machine_driver_popeyebl	,input_ports_popeye	,null	,ROT0	,	"bootleg",  "Popeye (bootleg)", GAME_NO_COCKTAIL );
	
}
