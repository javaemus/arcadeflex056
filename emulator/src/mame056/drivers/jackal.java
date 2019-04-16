/***************************************************************************

  jackal.c

  Written by Kenneth Lin (kenneth_lin@ai.vancouver.bc.ca)

Notes:
- This game uses two 005885 gfx chip in parallel. The unique thing about it is
  that the two 4bpp tilemaps from the two chips are merged to form a single
  8bpp tilemap.
- topgunbl is derived from a completely different version, which supports gun
  turret rotation. The copyright year is also deiffrent, but this doesn't
  necessarily mean anything.

TODO:
- The high score table colors are wrong, are there proms missing?
- Sprite lag
- Coin counters don't work correctly, because the register is overwritten by
  other routines and the coin counter bits rapidly toggle between 0 and 1.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.drivers;

import static arcadeflex056.fucPtr.*;
import common.ptr.UBytePtr;

import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.inptportH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.memoryH.*;
import static mame056.memory.*;
import static mame056.inptport.*;
import static mame056.drawgfxH.*;
import static mame056.inputH.*;
import static mame056.sndintrfH.*;
import static mame056.sndintrf.*;
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;

import static mame056.vidhrdw.generic.*;

import static arcadeflex056.fileio.*;
// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.palette.*;
import static mame056.machine.jackal.*;
import static mame056.palette.*;

import static mame056.vidhrdw.jackal.*;

public class jackal
{
	
	public static ReadHandlerPtr rotary_0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return (1 << (readinputport(5) * 8 / 256)) ^ 0xff;
	} };
	
	public static ReadHandlerPtr rotary_1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return (1 << (readinputport(6) * 8 / 256)) ^ 0xff;
	} };
	
	static int irq_enable;
	
	public static WriteHandlerPtr ctrl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		irq_enable = data & 0x02;
		flip_screen_set(data & 0x08);
	} };
	
	public static InterruptPtr jackal_interrupt = new InterruptPtr() { public int handler() 
	{
		if (irq_enable != 0)
			return interrupt.handler();
		else
			return ignore_interrupt.handler();
	} };
	
	
	
	public static Memory_ReadAddress jackal_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0010, 0x0010, input_port_0_r ),
		new Memory_ReadAddress( 0x0011, 0x0011, input_port_1_r ),
		new Memory_ReadAddress( 0x0012, 0x0012, input_port_2_r ),
		new Memory_ReadAddress( 0x0013, 0x0013, input_port_3_r ),
		new Memory_ReadAddress( 0x0014, 0x0014, rotary_0_r ),
		new Memory_ReadAddress( 0x0015, 0x0015, rotary_1_r ),
		new Memory_ReadAddress( 0x0018, 0x0018, input_port_4_r ),
		new Memory_ReadAddress( 0x0020, 0x005f, jackal_zram_r ),	/* MAIN   Z RAM,SUB    Z RAM */
		new Memory_ReadAddress( 0x0060, 0x1fff, jackal_commonram_r ),	/* M COMMON RAM,S COMMON RAM */
		new Memory_ReadAddress( 0x2000, 0x2fff, jackal_voram_r ),	/* MAIN V O RAM,SUB  V O RAM */
		new Memory_ReadAddress( 0x3000, 0x3fff, jackal_spriteram_r ),	/* MAIN V O RAM,SUB  V O RAM */
		new Memory_ReadAddress( 0x4000, 0xbfff, MRA_BANK1 ),
		new Memory_ReadAddress( 0xc000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress jackal_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x0003, MWA_RAM, jackal_videoctrl ),	/* scroll + other things */
		new Memory_WriteAddress( 0x0004, 0x0004, ctrl_w ),
		new Memory_WriteAddress( 0x0019, 0x0019, MWA_NOP ),	/* possibly watchdog reset */
		new Memory_WriteAddress( 0x001c, 0x001c, jackal_rambank_w ),
		new Memory_WriteAddress( 0x0020, 0x005f, jackal_zram_w ),
		new Memory_WriteAddress( 0x0060, 0x1fff, jackal_commonram_w ),
		new Memory_WriteAddress( 0x2000, 0x2fff, jackal_voram_w ),
		new Memory_WriteAddress( 0x3000, 0x3fff, jackal_spriteram_w ),
		new Memory_WriteAddress( 0x4000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress jackal_sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		/*TODO*///new Memory_ReadAddress( 0x2001, 0x2001, YM2151_status_port_0_r ),
		new Memory_ReadAddress( 0x4000, 0x43ff, MRA_RAM ),		/* COLOR RAM (Self test only check 0x4000-0x423f */
		new Memory_ReadAddress( 0x6000, 0x605f, MRA_RAM ),		/* SOUND RAM (Self test check 0x6000-605f, 0x7c00-0x7fff */
		new Memory_ReadAddress( 0x6060, 0x7fff, jackal_commonram1_r ), /* COMMON RAM */
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress jackal_sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		/*TODO*///new Memory_WriteAddress( 0x2000, 0x2000, YM2151_register_port_0_w ),
		/*TODO*///new Memory_WriteAddress( 0x2001, 0x2001, YM2151_data_port_0_w ),
		new Memory_WriteAddress( 0x4000, 0x43ff, paletteram_xBBBBBGGGGGRRRRR_w, paletteram ),
		new Memory_WriteAddress( 0x6000, 0x605f, MWA_RAM ),
		new Memory_WriteAddress( 0x6060, 0x7fff, jackal_commonram1_w ),
		new Memory_WriteAddress( 0x8000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	static InputPortPtr input_ports_jackal = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* DSW1 */
		PORT_DIPNAME( 0x0f, 0x0f, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x02, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x05, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "3C_2C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "4C_3C") );
		PORT_DIPSETTING(    0x0f, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "3C_4C") );
		PORT_DIPSETTING(    0x07, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0x0e, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x06, DEF_STR( "2C_5C") );
		PORT_DIPSETTING(    0x0d, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x0b, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x0a, DEF_STR( "1C_6C") );
		PORT_DIPSETTING(    0x09, DEF_STR( "1C_7C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Free_Play") );
		PORT_DIPNAME( 0xf0, 0xf0, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x20, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x50, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x80, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x40, DEF_STR( "3C_2C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "4C_3C") );
		PORT_DIPSETTING(    0xf0, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "3C_4C") );
		PORT_DIPSETTING(    0x70, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0xe0, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x60, DEF_STR( "2C_5C") );
		PORT_DIPSETTING(    0xd0, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0xc0, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0xb0, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0xa0, DEF_STR( "1C_6C") );
		PORT_DIPSETTING(    0x90, DEF_STR( "1C_7C") );
		PORT_DIPSETTING(    0x00, "Invalid" );
	
		PORT_START(); 	/* IN1 */
		/* note that button 3 for player 1 and 2 are exchanged */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	/* DSW2 */
		PORT_DIPNAME( 0x03, 0x02, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x03, "2" );
		PORT_DIPSETTING(    0x02, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPSETTING(    0x00, "7" );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x18, 0x18, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x18, "30000 150000" );
		PORT_DIPSETTING(    0x10, "50000 200000" );
		PORT_DIPSETTING(    0x08, "30000" );
		PORT_DIPSETTING(    0x00, "50000" );
		PORT_DIPNAME( 0x60, 0x60, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x60, "Easy" );
		PORT_DIPSETTING(    0x40, "Medium" );
		PORT_DIPSETTING(    0x20, "Hard" );
		PORT_DIPSETTING(    0x00, "Hardest" );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	/* identical, plus additional rotary controls */
	static InputPortPtr input_ports_topgunbl = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* DSW1 */
		PORT_DIPNAME( 0x0f, 0x0f, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x02, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x05, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "3C_2C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "4C_3C") );
		PORT_DIPSETTING(    0x0f, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "3C_4C") );
		PORT_DIPSETTING(    0x07, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0x0e, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x06, DEF_STR( "2C_5C") );
		PORT_DIPSETTING(    0x0d, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x0b, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x0a, DEF_STR( "1C_6C") );
		PORT_DIPSETTING(    0x09, DEF_STR( "1C_7C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Free_Play") );
		PORT_DIPNAME( 0xf0, 0xf0, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x20, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x50, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x80, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x40, DEF_STR( "3C_2C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "4C_3C") );
		PORT_DIPSETTING(    0xf0, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "3C_4C") );
		PORT_DIPSETTING(    0x70, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0xe0, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x60, DEF_STR( "2C_5C") );
		PORT_DIPSETTING(    0xd0, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0xc0, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0xb0, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0xa0, DEF_STR( "1C_6C") );
		PORT_DIPSETTING(    0x90, DEF_STR( "1C_7C") );
		PORT_DIPSETTING(    0x00, "Invalid" );
	
		PORT_START(); 	/* IN1 */
		/* note that button 3 for player 1 and 2 are exchanged */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	/* DSW2 */
		PORT_DIPNAME( 0x03, 0x02, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x03, "2" );
		PORT_DIPSETTING(    0x02, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPSETTING(    0x00, "7" );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x18, 0x18, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x18, "30000 150000" );
		PORT_DIPSETTING(    0x10, "50000 200000" );
		PORT_DIPSETTING(    0x08, "30000" );
		PORT_DIPSETTING(    0x00, "50000" );
		PORT_DIPNAME( 0x60, 0x60, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x60, "Easy" );
		PORT_DIPSETTING(    0x40, "Medium" );
		PORT_DIPSETTING(    0x20, "Hard" );
		PORT_DIPSETTING(    0x00, "Hardest" );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 	/* player 1 8-way rotary control - converted in rotary_0_r() */
		PORT_ANALOGX( 0xff, 0x00, IPT_DIAL, 25, 10, 0, 0, KEYCODE_Z, KEYCODE_X, 0, 0 );
	
		PORT_START(); 	/* player 2 8-way rotary control - converted in rotary_1_r() */
		PORT_ANALOGX( 0xff, 0x00, IPT_DIAL | IPF_PLAYER2, 25, 10, 0, 0, KEYCODE_N, KEYCODE_M, 0, 0 );
	INPUT_PORTS_END(); }}; 
	
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,4),
		8,	/* 8 bits per pixel (!) */
		new int[] { 0, 1, 2, 3, RGN_FRAC(1,2)+0, RGN_FRAC(1,2)+1, RGN_FRAC(1,2)+2, RGN_FRAC(1,2)+3 },
		new int[] { 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4 },
		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32 },
		32*8
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,4),
		4,
		new int[] { 0, 1, 2, 3 },
		new int[] { 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4,
				32*8+0*4, 32*8+1*4, 32*8+2*4, 32*8+3*4, 32*8+4*4, 32*8+5*4, 32*8+6*4, 32*8+7*4 },
		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32,
				16*32, 17*32, 18*32, 19*32, 20*32, 21*32, 22*32, 23*32 },
		32*32
	);
	
	static GfxLayout spritelayout8 = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,4),
		4,
		new int[] { 0, 1, 2, 3 },
		new int[] { 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4 },
		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32 },
		32*8
	);
	
	static GfxDecodeInfo jackal_gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0x00000, charlayout,               0, 16 ),	/* colors 256-511 with lookup */
		new GfxDecodeInfo( REGION_GFX1, 0x20000, spritelayout,        256*16, 16 ),	/* colors   0- 15 with lookup */
		new GfxDecodeInfo( REGION_GFX1, 0x20000, spritelayout8,       256*16, 16 ),	/* to handle 8x8 sprites */
		new GfxDecodeInfo( REGION_GFX1, 0x60000, spritelayout,  256*16+16*16, 16 ),	/* colors  16- 31 with lookup */
		new GfxDecodeInfo( REGION_GFX1, 0x60000, spritelayout8, 256*16+16*16, 16 ),	/* to handle 8x8 sprites */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	/*TODO*///static struct YM2151interface ym2151_interface =
	/*TODO*///{
	/*TODO*///	1,
	/*TODO*///	3580000,
	/*TODO*///	{ YM3012_VOL(50,MIXER_PAN_LEFT,50,MIXER_PAN_RIGHT) },
	/*TODO*///	{ 0 },
	/*TODO*///};
	
	
	static MachineDriver machine_driver_jackal = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6809,
				2000000,	/* 2 MHz???? */
				jackal_readmem,jackal_writemem,null,null,
				jackal_interrupt,1
			),
			new MachineCPU(
				CPU_M6809,
				2000000,	/* 2 MHz???? */
				jackal_sound_readmem,jackal_sound_writemem,null,null,
				ignore_interrupt,1
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		10,	/* 10 CPU slices per frame - seems enough to keep the CPUs in sync */
		jackal_init_machine,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 1*8, 31*8-1, 2*8, 30*8-1 ),
		jackal_gfxdecodeinfo,
		512, 256*16+16*16+16*16,
		jackal_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		jackal_vh_start,
		jackal_vh_stop,
		jackal_vh_screenrefresh,
	
		/* sound hardware */
		SOUND_SUPPORTS_STEREO,0,0,0,
		/*TODO*///new MachineSound[] {
		/*TODO*///	new MachineSound(
		/*TODO*///		SOUND_YM2151,
		/*TODO*///		ym2151_interface
		/*TODO*///	)
		/*TODO*///}
                null
	);
	
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_jackal = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x20000, REGION_CPU1, 0 );/* Banked 64k for 1st CPU */
		ROM_LOAD( "j-v02.rom",    0x04000, 0x8000, 0x0b7e0584 );
		ROM_CONTINUE(             0x14000, 0x8000 );
		ROM_LOAD( "j-v03.rom",    0x0c000, 0x4000, 0x3e0dfb83 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );    /* 64k for 2nd cpu (Graphics & Sound)*/
		ROM_LOAD( "631t01.bin",   0x8000, 0x8000, 0xb189af6a );
	
		ROM_REGION( 0x80000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD16_BYTE( "631t04.bin",   0x00000, 0x20000, 0x457f42f0 );
		ROM_LOAD16_BYTE( "631t05.bin",   0x00001, 0x20000, 0x732b3fc1 );
		ROM_LOAD16_BYTE( "631t06.bin",   0x40000, 0x20000, 0x2d10e56e );
		ROM_LOAD16_BYTE( "631t07.bin",   0x40001, 0x20000, 0x4961c397 );
	
		ROM_REGION( 0x0200, REGION_PROMS, 0 );/* color lookup tables */
		ROM_LOAD( "631r08.bpr",   0x0000, 0x0100, 0x7553a172 );
		ROM_LOAD( "631r09.bpr",   0x0100, 0x0100, 0xa74dd86c );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_topgunr = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x20000, REGION_CPU1, 0 );/* Banked 64k for 1st CPU */
		ROM_LOAD( "tgnr15d.bin",  0x04000, 0x8000, 0xf7e28426 );
		ROM_CONTINUE(             0x14000, 0x8000 );
		ROM_LOAD( "tgnr16d.bin",  0x0c000, 0x4000, 0xc086844e );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );    /* 64k for 2nd cpu (Graphics & Sound)*/
		ROM_LOAD( "631t01.bin",   0x8000, 0x8000, 0xb189af6a );
	
		ROM_REGION( 0x80000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD16_BYTE( "tgnr7h.bin",   0x00000, 0x20000, 0x50122a12 );
		ROM_LOAD16_BYTE( "tgnr8h.bin",   0x00001, 0x20000, 0x6943b1a4 );
		ROM_LOAD16_BYTE( "tgnr12h.bin",  0x40000, 0x20000, 0x37dbbdb0 );
		ROM_LOAD16_BYTE( "tgnr13h.bin",  0x40001, 0x20000, 0x22effcc8 );
	
		ROM_REGION( 0x0200, REGION_PROMS, 0 );/* color lookup tables */
		ROM_LOAD( "631r08.bpr",   0x0000, 0x0100, 0x7553a172 );
		ROM_LOAD( "631r09.bpr",   0x0100, 0x0100, 0xa74dd86c );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_jackalj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x20000, REGION_CPU1, 0 );/* Banked 64k for 1st CPU */
		ROM_LOAD( "631t02.bin",   0x04000, 0x8000, 0x14db6b1a );
		ROM_CONTINUE(             0x14000, 0x8000 );
		ROM_LOAD( "631t03.bin",   0x0c000, 0x4000, 0xfd5f9624 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );    /* 64k for 2nd cpu (Graphics & Sound)*/
		ROM_LOAD( "631t01.bin",   0x8000, 0x8000, 0xb189af6a );
	
		ROM_REGION( 0x80000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD16_BYTE( "631t04.bin",   0x00000, 0x20000, 0x457f42f0 );
		ROM_LOAD16_BYTE( "631t05.bin",   0x00001, 0x20000, 0x732b3fc1 );
		ROM_LOAD16_BYTE( "631t06.bin",   0x40000, 0x20000, 0x2d10e56e );
		ROM_LOAD16_BYTE( "631t07.bin",   0x40001, 0x20000, 0x4961c397 );
	
		ROM_REGION( 0x0200, REGION_PROMS, 0 );/* color lookup tables */
		ROM_LOAD( "631r08.bpr",   0x0000, 0x0100, 0x7553a172 );
		ROM_LOAD( "631r09.bpr",   0x0100, 0x0100, 0xa74dd86c );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_topgunbl = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x20000, REGION_CPU1, 0 );/* Banked 64k for 1st CPU */
		ROM_LOAD( "t-3.c5",       0x04000, 0x8000, 0x7826ad38 );
		ROM_LOAD( "t-4.c4",       0x14000, 0x8000, 0x976c8431 );
		ROM_LOAD( "t-2.c6",       0x0c000, 0x4000, 0xd53172e5 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );    /* 64k for 2nd cpu (Graphics & Sound)*/
		ROM_LOAD( "t-1.c14",      0x8000, 0x8000, 0x54aa2d29 );
	
		ROM_REGION( 0x80000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD16_BYTE( "tgnr7h.bin",   0x00000, 0x20000, 0x50122a12 );
		ROM_LOAD16_BYTE( "tgnr8h.bin",   0x00001, 0x20000, 0x6943b1a4 );
		ROM_LOAD16_BYTE( "tgnr12h.bin",  0x40000, 0x20000, 0x37dbbdb0 );
		ROM_LOAD16_BYTE( "tgnr13h.bin",  0x40001, 0x20000, 0x22effcc8 );
	
	/*TODO*///#if 0
	/*TODO*///	same data, different layout (and one bad ROM)
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-17.n12",     0x00000, 0x08000, 0xe8875110 )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-18.n13",     0x08000, 0x08000, 0xcf14471d )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-19.n14",     0x10000, 0x08000, 0x46ee5dd2 )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-20.n15",     0x18000, 0x08000, 0x3f472344 )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-6.n1",       0x20000, 0x08000, 0x539cc48c )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-5.m1",       0x28000, 0x08000, BADCRC( 0x2dd9a5e9 ) )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-7.n2",       0x30000, 0x08000, 0x0ecd31b1 )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-8.n3",       0x38000, 0x08000, 0xf946ada7 )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-13.n8",      0x40000, 0x08000, 0x5d669abb )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-14.n9",      0x48000, 0x08000, 0xf349369b )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-15.n10",     0x50000, 0x08000, 0x7c5a91dd )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-16.n11",     0x58000, 0x08000, 0x5ec46d8e )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-9.n4",       0x60000, 0x08000, 0x8269caca )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-10.n5",      0x68000, 0x08000, 0x25393e4f )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-11.n6",      0x70000, 0x08000, 0x7895c22d )
	/*TODO*///	ROM_LOAD16_WORD_SWAP( "t-12.n7",      0x78000, 0x08000, 0x15606dfc )
	/*TODO*///#endif
	
		ROM_REGION( 0x0200, REGION_PROMS, 0 );/* color lookup tables */
		ROM_LOAD( "631r08.bpr",   0x0000, 0x0100, 0x7553a172 );
		ROM_LOAD( "631r09.bpr",   0x0100, 0x0100, 0xa74dd86c );
	ROM_END(); }}; 
	
	
	
	public static GameDriver driver_jackal	   = new GameDriver("1986"	,"jackal"	,"jackal.java"	,rom_jackal,null	,machine_driver_jackal	,input_ports_jackal	,null	,ROT90	,	"Konami", "Jackal (World)", GAME_IMPERFECT_COLORS | GAME_NO_COCKTAIL );
	public static GameDriver driver_topgunr	   = new GameDriver("1986"	,"topgunr"	,"jackal.java"	,rom_topgunr,driver_jackal	,machine_driver_jackal	,input_ports_jackal	,null	,ROT90	,	"Konami", "Top Gunner (US)", GAME_IMPERFECT_COLORS | GAME_NO_COCKTAIL );
	public static GameDriver driver_jackalj	   = new GameDriver("1986"	,"jackalj"	,"jackal.java"	,rom_jackalj,driver_jackal	,machine_driver_jackal	,input_ports_jackal	,null	,ROT90	,	"Konami", "Tokushu Butai Jackal (Japan)", GAME_IMPERFECT_COLORS | GAME_NO_COCKTAIL );
	public static GameDriver driver_topgunbl	   = new GameDriver("1987"	,"topgunbl"	,"jackal.java"	,rom_topgunbl,driver_jackal	,machine_driver_jackal	,input_ports_topgunbl	,null	,ROT90	,	"bootleg", "Top Gunner (bootleg)", GAME_IMPERFECT_COLORS | GAME_NO_COCKTAIL );
}
