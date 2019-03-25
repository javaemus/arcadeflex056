/***************************************************************************

VIC Dual Game board

Seems like there are 2 types of boards. One uses an 8080A and 2 control PROMs
and the other is a Z80 with 1 control PROM.


0000-3fff ROM
4000-7fff ROM mirror image (used in most games)

the following have mirror images throughout the address space
e000-e3ff Video RAM + work RAM
e400-e7ff RAM
e800-efff Character generator RAM

I/O ports:

The memory map is the same for many games, but the I/O ports change. The
following ones are for Carnival, and apply to many other games as well.

read:
00        IN0
          bit 0 = connector
          bit 1 = connector
          bit 2 = dsw
          bit 3 = dsw
          bit 4 = connector
          bit 5 = connector
          bit 6 = seems unused
          bit 7 = seems unused

01        IN1
          bit 0 = connector
          bit 1 = connector
          bit 2 = dsw
          bit 3 = vblank
          bit 4 = connector
          bit 5 = connector
          bit 6 = seems unused
          bit 7 = seems unused

02        IN2
          bit 0 = connector
          bit 1 = connector
          bit 2 = dsw
          bit 3 = timer? is this used?
          bit 4 = connector
          bit 5 = connector
          bit 6 = seems unused
          bit 7 = seems unused

03        IN3
          bit 0 = connector
          bit 1 = connector
          bit 2 = dsw
          bit 3 = COIN (must reset the CPU to make the game acknowledge it)
          bit 4 = connector
          bit 5 = connector
          bit 6 = seems unused
          bit 7 = seems unused

write:
	(ports 1 and 2: see definitions in sound driver)

08        ?

40        palette bank

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.drivers;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memcpy;
import common.ptr.UBytePtr;
import static mame056.common.memory_region;

import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.inptportH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuexec.*;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.memoryH.*;
import static mame056.memory.*;
import static mame056.inptport.*;
import static mame056.drawgfxH.*;
import static mame056.drivers.sega.*;
import static mame056.inputH.*;
import static mame056.sndintrfH.*;
import static mame056.timerH.*;
import static mame056.timer.*;
import static mame056.palette.*;
import static mame056.vidhrdw.generic.*;
import static mame056.vidhrdw.vicdual.*;
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;

import static mame056.sndhrdw.invinco.*;
import static mame056.sndhrdw.carnival.*;
import static mame056.sndhrdw.depthch.*;
import static mame056.sndhrdw.pulsar.*;
import mame056.sound.ay8910H.AY8910interface;

public class vicdual
{
	
	
	
	public static int PSG_CLOCK_CARNIVAL = ( 3579545 / 3 );	/* Hz */
	
	
	static UBytePtr vicdual_ram = new UBytePtr();
	
	
	static int protection_data;
	
	public static WriteHandlerPtr samurai_protection_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		protection_data = data;
	} };
	
	public static ReadHandlerPtr samurai_input_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int answer = 0;
	
		if (protection_data == 0xab) answer = 0x02;
		else if (protection_data == 0x1d) answer = 0x0c;
	
		return (readinputport(1 + offset) & 0xfd) | ((answer >> offset) & 0x02);
	} };
	
	
	public static WriteHandlerPtr vicdual_ram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		vicdual_ram.write(offset, data);
	} };
	
	public static ReadHandlerPtr vicdual_ram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return vicdual_ram.read(offset);
	} };
	
	
	public static ReadHandlerPtr depthch_input_port_1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* bit 0 is 64V according to the schematics */
		return (input_port_1_r.handler(0) & 0xfe) | ((cpu_getscanline() >> 6) & 0x01);
	} };
	
	
	public static Memory_ReadAddress vicdual_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x83ff, videoram_r ),
		new Memory_ReadAddress( 0x8400, 0x87ff, vicdual_ram_r ),
		new Memory_ReadAddress( 0x8800, 0x8fff, vicdual_characterram_r ),
		new Memory_ReadAddress( 0x9000, 0x93ff, videoram_r ),
		new Memory_ReadAddress( 0x9400, 0x97ff, vicdual_ram_r ),
		new Memory_ReadAddress( 0x9800, 0x9fff, vicdual_characterram_r ),
		new Memory_ReadAddress( 0xa000, 0xa3ff, videoram_r ),
		new Memory_ReadAddress( 0xa400, 0xa7ff, vicdual_ram_r ),
		new Memory_ReadAddress( 0xa800, 0xafff, vicdual_characterram_r ),
		new Memory_ReadAddress( 0xb000, 0xb3ff, videoram_r ),
		new Memory_ReadAddress( 0xb400, 0xb7ff, vicdual_ram_r ),
		new Memory_ReadAddress( 0xb800, 0xbfff, vicdual_characterram_r ),
		new Memory_ReadAddress( 0xc000, 0xc3ff, videoram_r ),
		new Memory_ReadAddress( 0xc400, 0xc7ff, vicdual_ram_r ),
		new Memory_ReadAddress( 0xc800, 0xcfff, vicdual_characterram_r ),
		new Memory_ReadAddress( 0xd000, 0xd3ff, videoram_r ),
		new Memory_ReadAddress( 0xd400, 0xd7ff, vicdual_ram_r ),
		new Memory_ReadAddress( 0xd800, 0xdfff, vicdual_characterram_r ),
		new Memory_ReadAddress( 0xe000, 0xe3ff, videoram_r ),
		new Memory_ReadAddress( 0xe400, 0xe7ff, vicdual_ram_r ),
		new Memory_ReadAddress( 0xe800, 0xefff, vicdual_characterram_r ),
		new Memory_ReadAddress( 0xf000, 0xf3ff, videoram_r ),
		new Memory_ReadAddress( 0xf400, 0xf7ff, vicdual_ram_r ),
		new Memory_ReadAddress( 0xf800, 0xffff, vicdual_characterram_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress vicdual_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x83ff, videoram_w, videoram, videoram_size ),
		new Memory_WriteAddress( 0x8400, 0x87ff, vicdual_ram_w, vicdual_ram ),
		new Memory_WriteAddress( 0x8800, 0x8fff, vicdual_characterram_w, vicdual_characterram ),
		new Memory_WriteAddress( 0x9000, 0x93ff, videoram_w, videoram, videoram_size ),
		new Memory_WriteAddress( 0x9400, 0x97ff, vicdual_ram_w, vicdual_ram ),
		new Memory_WriteAddress( 0x9800, 0x9fff, vicdual_characterram_w, vicdual_characterram),
		new Memory_WriteAddress( 0xa000, 0xa3ff, videoram_w ),
		new Memory_WriteAddress( 0xa400, 0xa7ff, vicdual_ram_w ),
		new Memory_WriteAddress( 0xa800, 0xafff, vicdual_characterram_w ),
		new Memory_WriteAddress( 0xb000, 0xb3ff, videoram_w ),
		new Memory_WriteAddress( 0xb400, 0xb7ff, vicdual_ram_w ),
		new Memory_WriteAddress( 0xb800, 0xbfff, vicdual_characterram_w ),
		new Memory_WriteAddress( 0xc000, 0xc3ff, videoram_w ),
		new Memory_WriteAddress( 0xc400, 0xc7ff, vicdual_ram_w ),
		new Memory_WriteAddress( 0xc800, 0xcfff, vicdual_characterram_w ),
		new Memory_WriteAddress( 0xd000, 0xd3ff, videoram_w ),
		new Memory_WriteAddress( 0xd400, 0xd7ff, vicdual_ram_w ),
		new Memory_WriteAddress( 0xd800, 0xdfff, vicdual_characterram_w ),
		new Memory_WriteAddress( 0xe000, 0xe3ff, videoram_w ),
		new Memory_WriteAddress( 0xe400, 0xe7ff, vicdual_ram_w ),
		new Memory_WriteAddress( 0xe800, 0xefff, vicdual_characterram_w ),
		new Memory_WriteAddress( 0xf000, 0xf3ff, videoram_w ),
		new Memory_WriteAddress( 0xf400, 0xf7ff, vicdual_ram_w ),
		new Memory_WriteAddress( 0xf800, 0xffff, vicdual_characterram_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	/* Safari has extra RAM */
	public static Memory_ReadAddress safari_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),
		new Memory_ReadAddress( 0xe000, 0xe3ff, videoram_r ),
		new Memory_ReadAddress( 0xe400, 0xe7ff, vicdual_ram_r ),
		new Memory_ReadAddress( 0xe800, 0xefff, vicdual_characterram_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress safari_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM ),
		new Memory_WriteAddress( 0xe000, 0xe3ff, videoram_w, videoram, videoram_size ),
		new Memory_WriteAddress( 0xe400, 0xe7ff, vicdual_ram_w, vicdual_ram ),
		new Memory_WriteAddress( 0xe800, 0xefff, vicdual_characterram_w, vicdual_characterram ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_ReadPort readport_2ports[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x01, 0x01, input_port_0_r ),
		new IO_ReadPort( 0x08, 0x08, input_port_1_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static IO_ReadPort readport_3ports[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x01, 0x01, input_port_0_r ),
		new IO_ReadPort( 0x04, 0x04, input_port_1_r ),
		new IO_ReadPort( 0x08, 0x08, input_port_2_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static IO_ReadPort readport_4ports[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x00, 0x00, input_port_0_r ),
		new IO_ReadPort( 0x01, 0x01, input_port_1_r ),
		new IO_ReadPort( 0x02, 0x02, input_port_2_r ),
		new IO_ReadPort( 0x03, 0x03, input_port_3_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static IO_ReadPort readport_safari[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x03, 0x03, input_port_0_r ),
		new IO_ReadPort( 0x08, 0x08, input_port_1_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_WritePort writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x40, 0x40, vicdual_palette_bank_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	public static Memory_ReadAddress i8039_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x07ff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress i8039_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x07ff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_ReadPort i8039_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		/*TODO*///new IO_ReadPort( I8039_t1, I8039_t1, carnival_music_port_t1_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static IO_WritePort i8039_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		/*TODO*///new IO_WritePort( I8039_p1, I8039_p1, carnival_music_port_1_w ),
		/*TODO*///new IO_WritePort( I8039_p2, I8039_p2, carnival_music_port_2_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	
	static InputPortPtr input_ports_depthch = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		PORT_DIPNAME (0x30, 0x30, DEF_STR( "Coinage") );
		PORT_DIPSETTING (   0x00, DEF_STR( "4C_1C") );
		PORT_DIPSETTING (   0x10, DEF_STR( "3C_1C") );
		PORT_DIPSETTING (   0x20, DEF_STR( "2C_1C") );
		PORT_DIPSETTING (   0x30, DEF_STR( "1C_1C") );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_UNUSED ); /* 64V */
		PORT_BIT( 0x7e, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_safari = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );
		PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_BUTTON2, "Aim Up", KEYCODE_A, IP_JOY_DEFAULT );
		PORT_BITX(0x20, IP_ACTIVE_LOW, IPT_BUTTON3, "Aim Down", KEYCODE_Z, IP_JOY_DEFAULT );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x0e, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME (0x30, 0x30, DEF_STR( "Coinage") );
		PORT_DIPSETTING (   0x00, DEF_STR( "4C_1C") );
		PORT_DIPSETTING (   0x10, DEF_STR( "3C_1C") );
		PORT_DIPSETTING (   0x20, DEF_STR( "2C_1C") );
		PORT_DIPSETTING (   0x30, DEF_STR( "1C_1C") );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_frogs = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, "Allow Free Game" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x20, 0x20, "Time" );
		PORT_DIPSETTING(    0x00, "60" );
		PORT_DIPSETTING(    0x20, "90" );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x00, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x40, DEF_STR( "1C_1C") );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x7e, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_sspaceat = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_COCKTAIL );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_COCKTAIL );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY );
	
		PORT_START(); 	/* IN1 */
		PORT_DIPNAME( 0x01, 0x00, "Bonus Life After 1st Stage" );
		PORT_DIPSETTING(    0x01, DEF_STR( "No") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x0e, 0x0e, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x0e, "3" );
		PORT_DIPSETTING(    0x0c, "4" );
		PORT_DIPSETTING(    0x0a, "5" );
		PORT_DIPSETTING(    0x06, "6" );
	/* the following are duplicates
		PORT_DIPSETTING(    0x00, "4" );
		PORT_DIPSETTING(    0x04, "4" );
		PORT_DIPSETTING(    0x08, "4" );
		PORT_DIPSETTING(    0x02, "5" );*/
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x00, "10000" );
		PORT_DIPSETTING(    0x10, "15000" );
		PORT_BIT( 0x60, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x80, 0x00, "Credits Display" );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x7e, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_headon = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_DIPNAME( 0x03, 0x00, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPSETTING(    0x02, "5" );
		PORT_DIPSETTING(    0x03, "6" );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x7e, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_headon2 = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x07, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x18, 0x18, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x18, "4" );
		PORT_DIPSETTING(    0x10, "5" );
		PORT_DIPSETTING(    0x00, "6" );
	/*	PORT_DIPSETTING(    0x08, "5" );/
		PORT_BIT( 0xe0, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_BIT( 0x7c, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_invho2 = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x04, "Head On Lives (1/2)" );
		PORT_DIPSETTING(    0x04, "+0" );
		PORT_DIPSETTING(    0x00, "+1" );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, "Head On Lives (2/2)" );
		PORT_DIPSETTING(    0x04, "+0" );
		PORT_DIPSETTING(    0x00, "+1" );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, "Invinco Lives" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPSETTING(    0x04, "6" );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );/* timer - unused */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );/* probably unused */
	
		PORT_START(); 	/* IN3 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		/* There's probably a bug in the code: this would likely be the second */
		/* bit of the Invinco Lives setting, but the game reads bit 3 instead */
		/* of bit 2. */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT_IMPULSE( 0x08, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
		PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_TOGGLE, "Game Select", IP_KEY_DEFAULT, IP_JOY_DEFAULT );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_samurai = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x04, "3" );
		PORT_DIPSETTING(    0x00, "4" );
		PORT_BITX(    0x08, 0x08, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Infinite Lives", IP_KEY_NONE, IP_JOY_NONE );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* protection, see samurai_input_r() */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unknown") ); /* unknown, but used */
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_VBLANK );/* seems to be on port 2 instead */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* protection, see samurai_input_r() */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_VBLANK );/* either vblank, or a timer. In the */
	                                            /* Carnival schematics, it's a timer. */
	//	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );/* timer */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN3 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* protection, see samurai_input_r() */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT_IMPULSE( 0x08, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_invinco = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
	
		PORT_START(); 	/* IN1 */
		PORT_DIPNAME( 0x03, 0x00, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "3" );
		PORT_DIPSETTING(    0x01, "4" );
		PORT_DIPSETTING(    0x02, "5" );
		PORT_DIPSETTING(    0x03, "6" );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x60, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x7e, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_invds = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, "Invinco Lives (1/2)" );
		PORT_DIPSETTING(    0x00, "+0" );
		PORT_DIPSETTING(    0x04, "+1" );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, "Invinco Lives (2/2)" );
		PORT_DIPSETTING(    0x00, "+0" );
		PORT_DIPSETTING(    0x04, "+2" );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, "Deep Scan Lives (1/2)" );
		PORT_DIPSETTING(    0x00, "+0" );
		PORT_DIPSETTING(    0x04, "+1" );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );/* timer - unused */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN3 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		/* +1 and +2 gives 2 lives instead of 6 */
		PORT_DIPNAME( 0x04, 0x00, "Deep Scan Lives (2/2)" );
		PORT_DIPSETTING(    0x04, "+0" );
		PORT_DIPSETTING(    0x00, "+2" );
		PORT_BIT_IMPULSE( 0x08, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
		PORT_BITX(0x10, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_TOGGLE, "Game Select", IP_KEY_DEFAULT, IP_JOY_DEFAULT );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_tranqgun = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );/* timer */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN3 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT_IMPULSE( 0x08, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_spacetrk = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x04, "3" );
		PORT_DIPSETTING(    0x00, "4" );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unknown") ); /* unknown, but used */
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* must be high for bonus life to work */
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );/* timer - unused */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN3 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* must be high for bonus life to work */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT_IMPULSE( 0x08, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_sptrekct = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x04, "3" );
		PORT_DIPSETTING(    0x00, "4" );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unknown") ); /* unknown, but used */
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* must be high for bonus life to work */
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );/* timer - unused */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN3 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* must be high for bonus life to work */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT_IMPULSE( 0x08, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_carnival = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );/* timer - unused */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN3 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT_IMPULSE( 0x08, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_carnvckt = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT    | IPF_2WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );/* timer - unused */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN3 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT_IMPULSE( 0x08, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_digger = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
	
		PORT_START(); 	/* IN1 */
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x03, "3" );
		PORT_DIPSETTING(    0x02, "4" );
		PORT_DIPSETTING(    0x01, "5" );
		PORT_DIPSETTING(    0x00, "6" );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x60, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x7e, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_pulsar = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x04, "Lives (1/2)" );
		PORT_DIPSETTING(    0x04, "+0" );
		PORT_DIPSETTING(    0x00, "+2" );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_4WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x04, "Lives (2/2)" );
		PORT_DIPSETTING(    0x04, "+0" );
		PORT_DIPSETTING(    0x00, "+1" );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );/* timer - unused */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN3 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT_IMPULSE( 0x08, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );/* probably unused */
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_heiankyo = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") ); /* bonus life? */
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, "2 Players Mode" );
		PORT_DIPSETTING(    0x08, "Alternating" );
		PORT_DIPSETTING(    0x00, "Simultaneous" );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") ); /* bonus life? */
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") ); /* bonus life? */
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );/* timer - unused */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );/* probably unused */
	
		PORT_START(); 	/* IN3 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START2 );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "3" );
		PORT_DIPSETTING(    0x04, "5" );
		PORT_BIT_IMPULSE( 0x08, IP_ACTIVE_LOW, IPT_COIN1 | IPF_RESETCPU, 30 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,	/* 8*8 characters */
		256,	/* 256 characters */
		1,	/* 1 bit per pixel */
		new int[] { 0 },
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },	/* pretty straightforward layout */
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		8*8	/* every char takes 8 consecutive bytes */
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( 0, 0xe800, charlayout, 0, 32 ),	/* the game dynamically modifies this */
		new GfxDecodeInfo( -1 )	/* end of array */
	};
	
	
	
	static Samplesinterface samples_interface_carnival = new Samplesinterface
        (
	 	12,	/* 12 channels */
	 	50,	/* volume */
		carnival_sample_names
        );
	
	static Samplesinterface samples_interface_depthch = new Samplesinterface
        (
		12,	/* 12 channels */
		50,	/* volume */
		depthch_sample_names
        );
	
	static Samplesinterface samples_interface_invinco3 = new Samplesinterface
        (
		12,	/* 12 channels */
		50,	/* volume */
		invinco_sample_names
        );
	
	//#define samples_interface_invinco4 samples_interface_invinco3
	
	static Samplesinterface samples_interface_pulsar = new Samplesinterface
        (
		12,	/* 12 channels */
		50,	/* volume */
		pulsar_sample_names
        );
	
	public static Samplesinterface samples_interface_2ports = samples_interface_carnival;	/* not used */
	public static Samplesinterface samples_interface_3ports = samples_interface_carnival;	/* not used */
	public static Samplesinterface samples_interface_4ports = samples_interface_carnival;	/* not used */
	public static Samplesinterface samples_interface_safari = samples_interface_carnival;	/* not used */
	
	
	
	//MACHINEDRIVER(        2ports,     vicdual,    2ports, 0 )
        static MachineDriver machine_driver_2ports = new MachineDriver
	(													
		/* basic machine hardware */					
		new MachineCPU[] {												
			new MachineCPU(											
				CPU_Z80,								
				15468480/8,								
				vicdual_readmem,vicdual_writemem,readport_2ports,writeport,	
				ignore_interrupt,1						
			)											
		},												
		60, 5000,	/* frames per second, vblank duration */	
		1,	/* single CPU, no need for interleaving */	
		null,												
														
		/* video hardware */							
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 28*8-1 ),		
		gfxdecodeinfo,									
		64, 0,											
		vicdual_vh_convert_color_prom,					
														
		VIDEO_TYPE_RASTER|VIDEO_SUPPORTS_DIRTY,			
		null,												
		generic_vh_start,								
		generic_vh_stop,								
		vicdual_vh_screenrefresh,						
														
		/* sound hardware */							
		0,0,0,0,										
		new MachineSound[] {												
			new MachineSound(											
				0 * SOUND_SAMPLES,				
				samples_interface_2ports				
			)											
		}                
	);
	
        //MACHINEDRIVER( 3ports,   vicdual, 3ports, 0 )
        static MachineDriver machine_driver_3ports = new MachineDriver
	(													
		/* basic machine hardware */					
		new MachineCPU[] {												
			new MachineCPU(											
				CPU_Z80,								
				15468480/8,								
				vicdual_readmem,vicdual_writemem,readport_3ports,writeport,	
				ignore_interrupt,1						
			)											
		},												
		60, 5000,	/* frames per second, vblank duration */	
		1,	/* single CPU, no need for interleaving */	
		null,												
														
		/* video hardware */							
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 28*8-1 ),		
		gfxdecodeinfo,									
		64, 0,											
		vicdual_vh_convert_color_prom,					
														
		VIDEO_TYPE_RASTER|VIDEO_SUPPORTS_DIRTY,			
		null,												
		generic_vh_start,								
		generic_vh_stop,								
		vicdual_vh_screenrefresh,						
														
		/* sound hardware */							
		0,0,0,0,										
		new MachineSound[] {												
			new MachineSound(											
				0 * SOUND_SAMPLES,				
				samples_interface_3ports				
			)											
		}               
	);
        
	//MACHINEDRIVER( 4ports,   vicdual, 4ports, 0 )
        static MachineDriver machine_driver_4ports = new MachineDriver
	(													
		/* basic machine hardware */					
		new MachineCPU[] {												
			new MachineCPU(											
				CPU_Z80,								
				15468480/8,								
				vicdual_readmem,vicdual_writemem,readport_4ports,writeport,	
				ignore_interrupt,1						
			)											
		},												
		60, 5000,	/* frames per second, vblank duration */	
		1,	/* single CPU, no need for interleaving */	
		null,												
														
		/* video hardware */							
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 28*8-1 ),		
		gfxdecodeinfo,									
		64, 0,											
		vicdual_vh_convert_color_prom,					
														
		VIDEO_TYPE_RASTER|VIDEO_SUPPORTS_DIRTY,			
		null,												
		generic_vh_start,								
		generic_vh_stop,								
		vicdual_vh_screenrefresh,						
														
		/* sound hardware */							
		0,0,0,0,										
		new MachineSound[] {												
			new MachineSound(											
				0 * SOUND_SAMPLES,				
				samples_interface_4ports				
			)											
		}                                
	);
        
	//MACHINEDRIVER( safari,   safari,  safari, 0 )
	static MachineDriver machine_driver_safari = new MachineDriver
	(													
		/* basic machine hardware */					
		new MachineCPU[] {												
			new MachineCPU(											
				CPU_Z80,								
				15468480/8,								
				safari_readmem,safari_writemem,readport_safari,writeport,	
				ignore_interrupt,1						
			)											
		},												
		60, 5000,	/* frames per second, vblank duration */	
		1,	/* single CPU, no need for interleaving */	
		null,												
														
		/* video hardware */							
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 28*8-1 ),		
		gfxdecodeinfo,									
		64, 0,											
		vicdual_vh_convert_color_prom,					
														
		VIDEO_TYPE_RASTER|VIDEO_SUPPORTS_DIRTY,			
		null,												
		generic_vh_start,								
		generic_vh_stop,								
		vicdual_vh_screenrefresh,						
														
		/* sound hardware */							
		0,0,0,0,										
		new MachineSound[] {												
			new MachineSound(											
				0 * SOUND_SAMPLES,				
				samples_interface_safari				
			)											
		}
	);
        
        //MACHINEDRIVER( depthch,  vicdual, 2ports, 1 )
        static MachineDriver machine_driver_depthch = new MachineDriver
	(													
		/* basic machine hardware */					
		new MachineCPU[] {												
			new MachineCPU(											
				CPU_Z80,								
				15468480/8,								
				vicdual_readmem,vicdual_writemem,readport_2ports,writeport,	
				ignore_interrupt,1						
			)											
		},												
		60, 5000,	/* frames per second, vblank duration */	
		1,	/* single CPU, no need for interleaving */	
		null,												
														
		/* video hardware */							
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 28*8-1 ),		
		gfxdecodeinfo,									
		64, 0,											
		vicdual_vh_convert_color_prom,					
														
		VIDEO_TYPE_RASTER|VIDEO_SUPPORTS_DIRTY,			
		null,												
		generic_vh_start,								
		generic_vh_stop,								
		vicdual_vh_screenrefresh,						
														
		/* sound hardware */							
		0,0,0,0,										
		new MachineSound[] {												
			new MachineSound(											
				1 * SOUND_SAMPLES,				
				samples_interface_depthch				
			)											
		}
	);
        
	//MACHINEDRIVER( invinco3, vicdual, 3ports, 1 )
        static MachineDriver machine_driver_invinco3 = new MachineDriver
	(													
		/* basic machine hardware */					
		new MachineCPU[] {												
			new MachineCPU(											
				CPU_Z80,								
				15468480/8,								
				vicdual_readmem,vicdual_writemem,readport_3ports,writeport,	
				ignore_interrupt,1						
			)											
		},												
		60, 5000,	/* frames per second, vblank duration */	
		1,	/* single CPU, no need for interleaving */	
		null,												
														
		/* video hardware */							
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 28*8-1 ),		
		gfxdecodeinfo,									
		64, 0,											
		vicdual_vh_convert_color_prom,					
														
		VIDEO_TYPE_RASTER|VIDEO_SUPPORTS_DIRTY,			
		null,												
		generic_vh_start,								
		generic_vh_stop,								
		vicdual_vh_screenrefresh,						
														
		/* sound hardware */							
		0,0,0,0,										
		new MachineSound[] {												
			new MachineSound(											
				1 * SOUND_SAMPLES,				
				samples_interface_invinco3				
			)
                }
	);
        
	//MACHINEDRIVER( invinco4, vicdual, 4ports, 1 )
        static MachineDriver machine_driver_invinco4 = new MachineDriver
	(													
		/* basic machine hardware */					
		new MachineCPU[] {												
			new MachineCPU(											
				CPU_Z80,								
				15468480/8,								
				vicdual_readmem,vicdual_writemem,readport_4ports,writeport,	
				ignore_interrupt,1						
			)											
		},												
		60, 5000,	/* frames per second, vblank duration */	
		1,	/* single CPU, no need for interleaving */	
		null,												
														
		/* video hardware */							
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 28*8-1 ),		
		gfxdecodeinfo,									
		64, 0,											
		vicdual_vh_convert_color_prom,					
														
		VIDEO_TYPE_RASTER|VIDEO_SUPPORTS_DIRTY,			
		null,												
		generic_vh_start,								
		generic_vh_stop,								
		vicdual_vh_screenrefresh,						
														
		/* sound hardware */							
		0,0,0,0,										
		new MachineSound[] {												
			new MachineSound(											
				1 * SOUND_SAMPLES,				
				samples_interface_invinco3				
			)											
		}
	);
        
	////#define MACHINEDRIVER(NAME,       MEM,        PORT, SAMPLES)
        //MACHINEDRIVER( pulsar,   vicdual, 4ports, 1 )
	static MachineDriver machine_driver_pulsar = new MachineDriver
	(													
		/* basic machine hardware */					
		new MachineCPU[] {												
			new MachineCPU(											
				CPU_Z80,								
				15468480/8,								
				vicdual_readmem,vicdual_writemem,readport_4ports,writeport,	
				ignore_interrupt,1						
			)											
		},												
		60, 5000,	/* frames per second, vblank duration */	
		1,	/* single CPU, no need for interleaving */	
		null,												
														
		/* video hardware */							
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 28*8-1 ),		
		gfxdecodeinfo,									
		64, 0,											
		vicdual_vh_convert_color_prom,					
														
		VIDEO_TYPE_RASTER|VIDEO_SUPPORTS_DIRTY,			
		null,												
		generic_vh_start,								
		generic_vh_stop,								
		vicdual_vh_screenrefresh,						
														
		/* sound hardware */							
		0,0,0,0,										
		new MachineSound[] {												
			new MachineSound(											
				1 * SOUND_SAMPLES,				
				samples_interface_pulsar				
			)											
		}
	);
	
	static AY8910interface carnival_ay8910_interface = new AY8910interface
	(
		1,	/* 1 chips */
		PSG_CLOCK_CARNIVAL,
		new int[] { 35 },
		new ReadHandlerPtr[] { null },
		new ReadHandlerPtr[] { null },
		new WriteHandlerPtr[] { null },
		new WriteHandlerPtr[] { null }
	);
	
	/* don't know if any of the other games use the 8048 music board */
	/* so, we won't burden those drivers with the extra music handling */
	static MachineDriver machine_driver_carnival = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				15468480/8,
				vicdual_readmem,vicdual_writemem,readport_4ports,writeport,
				ignore_interrupt,1
			),
			new MachineCPU(
				CPU_I8039 | CPU_AUDIO_CPU,
				( ( 3579545 / 5 ) / 3 ),
				i8039_readmem,i8039_writemem,i8039_readport,i8039_writeport,
				ignore_interrupt,1
			)
		},
		60, 5000,	/* frames per second, vblank duration */
		10,
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 28*8-1 ),
		gfxdecodeinfo,
		64, 0,
		vicdual_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER|VIDEO_SUPPORTS_DIRTY,
		null,
		generic_vh_start,
		generic_vh_stop,
		vicdual_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_AY8910,
				carnival_ay8910_interface
			),
			new MachineSound(
				SOUND_SAMPLES,
				samples_interface_carnival
			)
		}
	);
	
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_depthch = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "50a",          0x0000, 0x0400, 0x56c5ffed );
		ROM_LOAD( "51a",          0x0400, 0x0400, 0x695eb81f );
		ROM_LOAD( "52",           0x0800, 0x0400, 0xaed0ba1b );
		ROM_LOAD( "53",           0x0c00, 0x0400, 0x2ccbd2d0 );
		ROM_LOAD( "54a",          0x1000, 0x0400, 0x1b7f6a43 );
		ROM_LOAD( "55a",          0x1400, 0x0400, 0x9fc2eb41 );
	
		ROM_REGION( 0x0040, REGION_USER1, 0 );/* misc PROMs, but no color so don't use REGION_PROMS! */
		ROM_LOAD( "316-0043.u87", 0x0000, 0x0020, 0xe60a7960 );/* control PROM */
		ROM_LOAD( "316-0042.u88", 0x0020, 0x0020, 0xa1506b9d );/* sequence PROM */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_safari = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "3160066.u48",  0x0000, 0x0400, 0x2a26b098 );
		ROM_LOAD( "3160065.u47",  0x0400, 0x0400, 0xb776f7db );
		ROM_LOAD( "3160064.u46",  0x0800, 0x0400, 0x19d8c196 );
		ROM_LOAD( "3160063.u45",  0x0c00, 0x0400, 0x028bad25 );
		ROM_LOAD( "3160062.u44",  0x1000, 0x0400, 0x504e0575 );
		ROM_LOAD( "3160061.u43",  0x1400, 0x0400, 0xd4c528e0 );
		ROM_LOAD( "3160060.u42",  0x1800, 0x0400, 0x48c7b0cc );
		ROM_LOAD( "3160059.u41",  0x1c00, 0x0400, 0x3f7baaff );
		ROM_LOAD( "3160058.u40",  0x2000, 0x0400, 0x0d5058f1 );
		ROM_LOAD( "3160057.u39",  0x2400, 0x0400, 0x298e8c41 );
	
		ROM_REGION( 0x0040, REGION_USER1, 0 );/* misc PROMs, but no color so don't use REGION_PROMS! */
		ROM_LOAD( "316-0043.u87", 0x0000, 0x0020, 0xe60a7960 );/* control PROM */
		ROM_LOAD( "316-0042.u88", 0x0020, 0x0020, 0xa1506b9d );/* sequence PROM */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_frogs = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "119a.u48",     0x0000, 0x0400, 0xb1d1fce4 );
		ROM_LOAD( "118a.u47",     0x0400, 0x0400, 0x12fdcc05 );
		ROM_LOAD( "117a.u46",     0x0800, 0x0400, 0x8a5be424 );
		ROM_LOAD( "116b.u45",     0x0c00, 0x0400, 0x09b82619 );
		ROM_LOAD( "115a.u44",     0x1000, 0x0400, 0x3d4e4fa8 );
		ROM_LOAD( "114a.u43",     0x1400, 0x0400, 0x04a21853 );
		ROM_LOAD( "113a.u42",     0x1800, 0x0400, 0x02786692 );
		ROM_LOAD( "112a.u41",     0x1c00, 0x0400, 0x0be2a058 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sspaceat = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "155.u27",      0x0000, 0x0400, 0xba7bb86f );
		ROM_LOAD( "156.u26",      0x0400, 0x0400, 0x0b3a491c );
		ROM_LOAD( "157.u25",      0x0800, 0x0400, 0x3d3fac3b );
		ROM_LOAD( "158.u24",      0x0c00, 0x0400, 0x843b80f6 );
		ROM_LOAD( "159.u23",      0x1000, 0x0400, 0x1eacf60d );
		ROM_LOAD( "160.u22",      0x1400, 0x0400, 0xe61d482f );
		ROM_LOAD( "161.u21",      0x1800, 0x0400, 0xeb5e0993 );
		ROM_LOAD( "162.u20",      0x1c00, 0x0400, 0x5f84d550 );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "316-0138.u44", 0x0000, 0x0020, 0x67104ea9 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sspacat2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "81.u48",       0x0000, 0x0400, 0x3e4b29f6 );
		ROM_LOAD( "58.u47",       0x0400, 0x0400, 0x176adb80 );
		ROM_LOAD( "59.u46",       0x0800, 0x0400, 0xb2400d05 );
		ROM_LOAD( "150.u45",      0x0c00, 0x0400, 0xcf9bfa65 );
		ROM_LOAD( "151.u44",      0x1000, 0x0400, 0x064530f1 );
		ROM_LOAD( "152.u43",      0x1400, 0x0400, 0xc65c30fe );
		ROM_LOAD( "153.u42",      0x1800, 0x0400, 0xea70c7f6 );
		ROM_LOAD( "156a.u41",     0x1c00, 0x0400, 0x9029d2ce );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "316-0138.u44", 0x0000, 0x0020, 0x67104ea9 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sspacatc = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "139.u27",      0x0000, 0x0400, 0x9f2112fc );
		ROM_LOAD( "140.u26",      0x0400, 0x0400, 0xddbeed35 );
		ROM_LOAD( "141.u25",      0x0800, 0x0400, 0xb159924d );
		ROM_LOAD( "142.u24",      0x0c00, 0x0400, 0xf2ebfce9 );
		ROM_LOAD( "143.u23",      0x1000, 0x0400, 0xbff34a66 );
		ROM_LOAD( "144.u22",      0x1400, 0x0400, 0xfa062d58 );
		ROM_LOAD( "145.u21",      0x1800, 0x0400, 0x7e950614 );
		ROM_LOAD( "146.u20",      0x1c00, 0x0400, 0x8ba94fbc );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "316-0138.u44", 0x0000, 0x0020, 0x67104ea9 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_headon = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "163a",         0x0000, 0x0400, 0x4bb51259 );
		ROM_LOAD( "164a",         0x0400, 0x0400, 0xaeac8c5f );
		ROM_LOAD( "165a",         0x0800, 0x0400, 0xf1a0cb72 );
		ROM_LOAD( "166c",         0x0c00, 0x0400, 0x65d12951 );
		ROM_LOAD( "167c",         0x1000, 0x0400, 0x2280831e );
		ROM_LOAD( "192a",         0x1400, 0x0400, 0xed4666f2 );
		ROM_LOAD( "193a",         0x1800, 0x0400, 0x37a1df4c );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "316-0138.u44", 0x0000, 0x0020, 0x67104ea9 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_headonb = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "163a",         0x0000, 0x0400, 0x4bb51259 );
		ROM_LOAD( "164a",         0x0400, 0x0400, 0xaeac8c5f );
		ROM_LOAD( "165a",         0x0800, 0x0400, 0xf1a0cb72 );
		ROM_LOAD( "166b",         0x0c00, 0x0400, 0x1c59008a );
		ROM_LOAD( "167a",         0x1000, 0x0400, 0x069e839e );
		ROM_LOAD( "192a",         0x1400, 0x0400, 0xed4666f2 );
		ROM_LOAD( "193a-1",       0x1800, 0x0400, 0xd3782c1d );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "316-0138.u44", 0x0000, 0x0020, 0x67104ea9 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_headon2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "u27.bin",      0x0000, 0x0400, 0xfa47d2fb );
		ROM_LOAD( "u26.bin",      0x0400, 0x0400, 0x61c47b15 );
		ROM_LOAD( "u25.bin",      0x0800, 0x0400, 0xbb16db92 );
		ROM_LOAD( "u24.bin",      0x0c00, 0x0400, 0x17a09f24 );
		ROM_LOAD( "u23.bin",      0x1000, 0x0400, 0x0024895e );
		ROM_LOAD( "u22.bin",      0x1400, 0x0400, 0xf798304d );
		ROM_LOAD( "u21.bin",      0x1800, 0x0400, 0x4c19dd40 );
		ROM_LOAD( "u20.bin",      0x1c00, 0x0400, 0x25887ff2 );
	
		ROM_REGION( 0x0060, REGION_PROMS, 0 );
		ROM_LOAD( "316-0138.u44", 0x0000, 0x0020, 0x67104ea9 );
		ROM_LOAD( "u65.bin",      0x0020, 0x0020, 0xe60a7960 );/* control PROM */
		ROM_LOAD( "u66.bin",      0x0040, 0x0020, 0xa1506b9d );/* sequence PROM */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_invho2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "271b.u33",     0x0000, 0x0400, 0x44356a73 );
		ROM_LOAD( "272b.u32",     0x0400, 0x0400, 0xbd251265 );
		ROM_LOAD( "273b.u31",     0x0800, 0x0400, 0x2fc80cd9 );
		ROM_LOAD( "274b.u30",     0x0c00, 0x0400, 0x4fac4210 );
		ROM_LOAD( "275b.u29",     0x1000, 0x0400, 0x85af508e );
		ROM_LOAD( "276b.u28",     0x1400, 0x0400, 0xe305843a );
		ROM_LOAD( "277b.u27",     0x1800, 0x0400, 0xb6b4221e );
		ROM_LOAD( "278b.u26",     0x1c00, 0x0400, 0x74d42250 );
		ROM_LOAD( "279b.u8",      0x2000, 0x0400, 0x8d30a3e0 );
		ROM_LOAD( "280b.u7",      0x2400, 0x0400, 0xb5ee60ec );
		ROM_LOAD( "281b.u6",      0x2800, 0x0400, 0x21a6d4f2 );
		ROM_LOAD( "282b.u5",      0x2c00, 0x0400, 0x07d54f8a );
		ROM_LOAD( "283b.u4",      0x3000, 0x0400, 0xbdbe7ec1 );
		ROM_LOAD( "284b.u3",      0x3400, 0x0400, 0xae9e9f16 );
		ROM_LOAD( "285b.u2",      0x3800, 0x0400, 0x8dc3ec34 );
		ROM_LOAD( "286b.u1",      0x3c00, 0x0400, 0x4bab9ba2 );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "316-0287.u49", 0x0000, 0x0020, 0xd4374b01 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_samurai = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "epr289.u33",   0x0000, 0x0400, 0xa1a9cb03 );
		ROM_LOAD( "epr290.u32",   0x0400, 0x0400, 0x49fede51 );
		ROM_LOAD( "epr291.u31",   0x0800, 0x0400, 0x6503dd72 );
		ROM_LOAD( "epr292.u30",   0x0c00, 0x0400, 0x179c224f );
		ROM_LOAD( "epr366.u29",   0x1000, 0x0400, 0x3df2abec );
		ROM_LOAD( "epr355.u28",   0x1400, 0x0400, 0xb24517a4 );
		ROM_LOAD( "epr367.u27",   0x1800, 0x0400, 0x992a6e5a );
		ROM_LOAD( "epr368.u26",   0x1c00, 0x0400, 0x403c72ce );
		ROM_LOAD( "epr369.u8",    0x2000, 0x0400, 0x3cfd115b );
		ROM_LOAD( "epr370.u7",    0x2400, 0x0400, 0x2c30db12 );
		ROM_LOAD( "epr299.u6",    0x2800, 0x0400, 0x87c71139 );
		ROM_LOAD( "epr371.u5",    0x2c00, 0x0400, 0x761f56cf );
		ROM_LOAD( "epr301.u4",    0x3000, 0x0400, 0x23de1ff7 );
		ROM_LOAD( "epr372.u3",    0x3400, 0x0400, 0x292cfd89 );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "pr55.clr",     0x0000, 0x0020, 0x975f5fb0 );
	
		ROM_REGION( 0x0040, REGION_USER1, 0 );/* misc PROMs */
		ROM_LOAD( "316-0043.u87", 0x0000, 0x0020, 0xe60a7960 );/* control PROM */
		ROM_LOAD( "316-0042.u88", 0x0020, 0x0020, 0xa1506b9d );/* sequence PROM */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_invinco = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "310a.u27",     0x0000, 0x0400, 0xe3931365 );
		ROM_LOAD( "311a.u26",     0x0400, 0x0400, 0xde1a6c4a );
		ROM_LOAD( "312a.u25",     0x0800, 0x0400, 0xe3c08f39 );
		ROM_LOAD( "313a.u24",     0x0c00, 0x0400, 0xb680b306 );
		ROM_LOAD( "314a.u23",     0x1000, 0x0400, 0x790f07d9 );
		ROM_LOAD( "315a.u22",     0x1400, 0x0400, 0x0d13bed2 );
		ROM_LOAD( "316a.u21",     0x1800, 0x0400, 0x88d7eab8 );
		ROM_LOAD( "317a.u20",     0x1c00, 0x0400, 0x75389463 );
		ROM_LOAD( "318a.uxx",     0x2000, 0x0400, 0x0780721d );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "316-246.u44",  0x0000, 0x0020, 0xfe4406cb );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_invds = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "367.u33",      0x0000, 0x0400, 0xe6a33eae );
		ROM_LOAD( "368.u32",      0x0400, 0x0400, 0x421554a8 );
		ROM_LOAD( "369.u31",      0x0800, 0x0400, 0x531e917a );
		ROM_LOAD( "370.u30",      0x0c00, 0x0400, 0x2ad68f8c );
		ROM_LOAD( "371.u29",      0x1000, 0x0400, 0x1b98dc5c );
		ROM_LOAD( "372.u28",      0x1400, 0x0400, 0x3a72190a );
		ROM_LOAD( "373.u27",      0x1800, 0x0400, 0x3d361520 );
		ROM_LOAD( "374.u26",      0x1c00, 0x0400, 0xe606e7d9 );
		ROM_LOAD( "375.u8",       0x2000, 0x0400, 0xadbe8d32 );
		ROM_LOAD( "376.u7",       0x2400, 0x0400, 0x79409a46 );
		ROM_LOAD( "377.u6",       0x2800, 0x0400, 0x3f021a71 );
		ROM_LOAD( "378.u5",       0x2c00, 0x0400, 0x49a542b0 );
		ROM_LOAD( "379.u4",       0x3000, 0x0400, 0xee140e49 );
		ROM_LOAD( "380.u3",       0x3400, 0x0400, 0x688ba831 );
		ROM_LOAD( "381.u2",       0x3800, 0x0400, 0x798ba0c7 );
		ROM_LOAD( "382.u1",       0x3c00, 0x0400, 0x8d195c24 );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "316-246",      0x0000, 0x0020, 0xfe4406cb );
	
		ROM_REGION( 0x0020, REGION_USER1, 0 );/* misc PROM */
		ROM_LOAD( "316-0206.u14", 0x0000, 0x0020, 0x9617d796 );/* control PROM */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_tranqgun = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "u33.bin",      0x0000, 0x0400, 0x6d50e902 );
		ROM_LOAD( "u32.bin",      0x0400, 0x0400, 0xf0ba0e60 );
		ROM_LOAD( "u31.bin",      0x0800, 0x0400, 0x9fe440d3 );
		ROM_LOAD( "u30.bin",      0x0c00, 0x0400, 0x1041608e );
		ROM_LOAD( "u29.bin",      0x1000, 0x0400, 0xfb5de95f );
		ROM_LOAD( "u28.bin",      0x1400, 0x0400, 0x03fd8727 );
		ROM_LOAD( "u27.bin",      0x1800, 0x0400, 0x3d93239b );
		ROM_LOAD( "u26.bin",      0x1c00, 0x0400, 0x20f64a7f );
		ROM_LOAD( "u8.bin",       0x2000, 0x0400, 0x5121c695 );
		ROM_LOAD( "u7.bin",       0x2400, 0x0400, 0xb13d21f7 );
		ROM_LOAD( "u6.bin",       0x2800, 0x0400, 0x603cee59 );
		ROM_LOAD( "u5.bin",       0x2c00, 0x0400, 0x7f25475f );
		ROM_LOAD( "u4.bin",       0x3000, 0x0400, 0x57dc3123 );
		ROM_LOAD( "u3.bin",       0x3400, 0x0400, 0x7aa7829b );
		ROM_LOAD( "u2.bin",       0x3800, 0x0400, 0xa9b10df5 );
		ROM_LOAD( "u1.bin",       0x3c00, 0x0400, 0x431a7449 );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "u49.bin",      0x0000, 0x0020, 0x6481445b );
	
		ROM_REGION( 0x0040, REGION_USER1, 0 );/* misc PROMs */
		ROM_LOAD( "316-0043.u87", 0x0000, 0x0020, 0xe60a7960 );/* control PROM */
		ROM_LOAD( "316-0042.u88", 0x0020, 0x0020, 0xa1506b9d );/* sequence PROM */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_spacetrk = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "u33.bin",      0x0000, 0x0400, 0x9033fe50 );
		ROM_LOAD( "u32.bin",      0x0400, 0x0400, 0x08f61f0d );
		ROM_LOAD( "u31.bin",      0x0800, 0x0400, 0x1088a8c4 );
		ROM_LOAD( "u30.bin",      0x0c00, 0x0400, 0x55560cc8 );
		ROM_LOAD( "u29.bin",      0x1000, 0x0400, 0x71713958 );
		ROM_LOAD( "u28.bin",      0x1400, 0x0400, 0x7bcf5ca3 );
		ROM_LOAD( "u27.bin",      0x1800, 0x0400, 0xad7a2065 );
		ROM_LOAD( "u26.bin",      0x1c00, 0x0400, 0x6060fe77 );
		ROM_LOAD( "u8.bin",       0x2000, 0x0400, 0x75a90624 );
		ROM_LOAD( "u7.bin",       0x2400, 0x0400, 0x7b31a2ab );
		ROM_LOAD( "u6.bin",       0x2800, 0x0400, 0x94135b33 );
		ROM_LOAD( "u5.bin",       0x2c00, 0x0400, 0xcfbf2538 );
		ROM_LOAD( "u4.bin",       0x3000, 0x0400, 0xb4b95129 );
		ROM_LOAD( "u3.bin",       0x3400, 0x0400, 0x03ca1d70 );
		ROM_LOAD( "u2.bin",       0x3800, 0x0400, 0xa968584b );
		ROM_LOAD( "u1.bin",       0x3c00, 0x0400, 0xe6e300e8 );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "u49.bin",      0x0000, 0x0020, 0xaabae4cd );
	
		ROM_REGION( 0x0040, REGION_USER1, 0 );/* misc PROMs */
		ROM_LOAD( "316-0043.u87", 0x0000, 0x0020, 0xe60a7960 );/* control PROM */
		ROM_LOAD( "316-0042.u88", 0x0020, 0x0020, 0xa1506b9d );/* sequence PROM */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_sptrekct = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "u33c.bin",     0x0000, 0x0400, 0xb056b928 );
		ROM_LOAD( "u32c.bin",     0x0400, 0x0400, 0xdffb11d9 );
		ROM_LOAD( "u31c.bin",     0x0800, 0x0400, 0x9b25d46f );
		ROM_LOAD( "u30c.bin",     0x0c00, 0x0400, 0x3a612bfe );
		ROM_LOAD( "u29c.bin",     0x1000, 0x0400, 0xd8bb6e0c );
		ROM_LOAD( "u28c.bin",     0x1400, 0x0400, 0x0e367740 );
		ROM_LOAD( "u27c.bin",     0x1800, 0x0400, 0xd59fec86 );
		ROM_LOAD( "u26c.bin",     0x1c00, 0x0400, 0x9deefa0f );
		ROM_LOAD( "u8c.bin",      0x2000, 0x0400, 0x613116c5 );
		ROM_LOAD( "u7c.bin",      0x2400, 0x0400, 0x3bdf2464 );
		ROM_LOAD( "u6c.bin",      0x2800, 0x0400, 0x039d73fa );
		ROM_LOAD( "u5c.bin",      0x2c00, 0x0400, 0x1638344f );
		ROM_LOAD( "u4c.bin",      0x3000, 0x0400, 0xe34443cd );
		ROM_LOAD( "u3c.bin",      0x3400, 0x0400, 0x6f16cbd7 );
		ROM_LOAD( "u2c.bin",      0x3800, 0x0400, 0x94da3cdc );
		ROM_LOAD( "u1c.bin",      0x3c00, 0x0400, 0x2a228bf4 );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "u49.bin",      0x0000, 0x0020, 0xaabae4cd );
	
		ROM_REGION( 0x0040, REGION_USER1, 0 );/* misc PROMs */
		ROM_LOAD( "316-0043.u87", 0x0000, 0x0020, 0xe60a7960 );/* control PROM */
		ROM_LOAD( "316-0042.u88", 0x0020, 0x0020, 0xa1506b9d );/* sequence PROM */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_carnival = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "651u33.cpu",   0x0000, 0x0400, 0x9f2736e6 );
		ROM_LOAD( "652u32.cpu",   0x0400, 0x0400, 0xa1f58beb );
		ROM_LOAD( "653u31.cpu",   0x0800, 0x0400, 0x67b17922 );
		ROM_LOAD( "654u30.cpu",   0x0c00, 0x0400, 0xbefb09a5 );
		ROM_LOAD( "655u29.cpu",   0x1000, 0x0400, 0x623fcdad );
		ROM_LOAD( "656u28.cpu",   0x1400, 0x0400, 0x53040332 );
		ROM_LOAD( "657u27.cpu",   0x1800, 0x0400, 0xf2537467 );
		ROM_LOAD( "658u26.cpu",   0x1c00, 0x0400, 0xfcc3854e );
		ROM_LOAD( "659u8.cpu",    0x2000, 0x0400, 0x28be8d69 );
		ROM_LOAD( "660u7.cpu",    0x2400, 0x0400, 0x3873ccdb );
		ROM_LOAD( "661u6.cpu",    0x2800, 0x0400, 0xd9a96dff );
		ROM_LOAD( "662u5.cpu",    0x2c00, 0x0400, 0xd893ca72 );
		ROM_LOAD( "663u4.cpu",    0x3000, 0x0400, 0xdf8c63c5 );
		ROM_LOAD( "664u3.cpu",    0x3400, 0x0400, 0x689a73e8 );
		ROM_LOAD( "665u2.cpu",    0x3800, 0x0400, 0x28e7b2b6 );
		ROM_LOAD( "666u1.cpu",    0x3c00, 0x0400, 0x4eec7fae );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "316-633",      0x0000, 0x0020, 0xf0084d80 );
	
		ROM_REGION( 0x0800, REGION_CPU2, 0 );/* sound ROM */
		ROM_LOAD( "crvl.snd",     0x0000, 0x0400, 0x0dbaa2b0 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_carnvckt = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "epr501",       0x0000, 0x0400, 0x688503d2 );
		ROM_LOAD( "652u32.cpu",   0x0400, 0x0400, 0xa1f58beb );
		ROM_LOAD( "653u31.cpu",   0x0800, 0x0400, 0x67b17922 );
		ROM_LOAD( "654u30.cpu",   0x0c00, 0x0400, 0xbefb09a5 );
		ROM_LOAD( "655u29.cpu",   0x1000, 0x0400, 0x623fcdad );
		ROM_LOAD( "epr506",       0x1400, 0x0400, 0xba916e97 );
		ROM_LOAD( "epr507",       0x1800, 0x0400, 0xd0bda4a5 );
		ROM_LOAD( "epr508",       0x1c00, 0x0400, 0xf0258cad );
		ROM_LOAD( "epr509",       0x2000, 0x0400, 0xdcc8a530 );
		ROM_LOAD( "epr510",       0x2400, 0x0400, 0x92c2ba51 );
		ROM_LOAD( "epr511",       0x2800, 0x0400, 0x3af899a0 );
		ROM_LOAD( "epr512",       0x2c00, 0x0400, 0x09f7b3e6 );
		ROM_LOAD( "epr513",       0x3000, 0x0400, 0x8f41974c );
		ROM_LOAD( "epr514",       0x3400, 0x0400, 0x2788d140 );
		ROM_LOAD( "epr515",       0x3800, 0x0400, 0x10decaa9 );
		ROM_LOAD( "epr516",       0x3c00, 0x0400, 0x7c32b352 );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "316-633",      0x0000, 0x0020, 0xf0084d80 );
	
		ROM_REGION( 0x0800, REGION_CPU2, 0 );/* sound ROM */
		ROM_LOAD( "crvl.snd",     0x0000, 0x0400, 0x0dbaa2b0 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_digger = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "684.u27",      0x0000, 0x0400, 0xbba0d7c2 );
		ROM_LOAD( "685.u26",      0x0400, 0x0400, 0x85210d8b );
		ROM_LOAD( "686.u25",      0x0800, 0x0400, 0x2d87238c );
		ROM_LOAD( "687.u24",      0x0c00, 0x0400, 0x0dd0604e );
		ROM_LOAD( "688.u23",      0x1000, 0x0400, 0x2f649667 );
		ROM_LOAD( "689.u22",      0x1400, 0x0400, 0x89fd63d9 );
		ROM_LOAD( "690.u21",      0x1800, 0x0400, 0xa86622a6 );
		ROM_LOAD( "691.u20",      0x1c00, 0x0400, 0x8aca72d8 );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "316-507",      0x0000, 0x0020, 0xfdb22e8f );
	
		ROM_REGION( 0x0020, REGION_USER1, 0 );/* misc PROM */
		ROM_LOAD( "316-0206.u14", 0x0000, 0x0020, 0x9617d796 );/* control PROM */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_pulsar = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "790.u33",      0x0000, 0x0400, 0x5e3816da );
		ROM_LOAD( "791.u32",      0x0400, 0x0400, 0xce0aee83 );
		ROM_LOAD( "792.u31",      0x0800, 0x0400, 0x72d78cf1 );
		ROM_LOAD( "793.u30",      0x0c00, 0x0400, 0x42155dd4 );
		ROM_LOAD( "794.u29",      0x1000, 0x0400, 0x11c7213a );
		ROM_LOAD( "795.u28",      0x1400, 0x0400, 0xd2f02e29 );
		ROM_LOAD( "796.u27",      0x1800, 0x0400, 0x67737a2e );
		ROM_LOAD( "797.u26",      0x1c00, 0x0400, 0xec250b24 );
		ROM_LOAD( "798.u8",       0x2000, 0x0400, 0x1d34912d );
		ROM_LOAD( "799.u7",       0x2400, 0x0400, 0xf5695e4c );
		ROM_LOAD( "800.u6",       0x2800, 0x0400, 0xbf91ad92 );
		ROM_LOAD( "801.u5",       0x2c00, 0x0400, 0x1e9721dc );
		ROM_LOAD( "802.u4",       0x3000, 0x0400, 0xd32d2192 );
		ROM_LOAD( "803.u3",       0x3400, 0x0400, 0x3ede44d5 );
		ROM_LOAD( "804.u2",       0x3800, 0x0400, 0x62847b01 );
		ROM_LOAD( "805.u1",       0x3c00, 0x0400, 0xab418e86 );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "316-0789.u49", 0x0000, 0x0020, 0x7fc1861f );
	
		ROM_REGION( 0x0020, REGION_USER1, 0 );/* misc PROM */
		ROM_LOAD( "316-0206.u14", 0x0000, 0x0020, 0x9617d796 );/* control PROM */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_heiankyo = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "ha16.u33",     0x0000, 0x0400, 0x1eec8b36 );
		ROM_LOAD( "ha15.u32",     0x0400, 0x0400, 0xc1b9a1a5 );
		ROM_LOAD( "ha14.u31",     0x0800, 0x0400, 0x5b7b582e );
		ROM_LOAD( "ha13.u30",     0x0c00, 0x0400, 0x4aa67e01 );
		ROM_LOAD( "ha12.u29",     0x1000, 0x0400, 0x75889ca6 );
		ROM_LOAD( "ha11.u28",     0x1400, 0x0400, 0xd469226a );
		ROM_LOAD( "ha10.u27",     0x1800, 0x0400, 0x4e203074 );
		ROM_LOAD( "ha9.u26",      0x1c00, 0x0400, 0x9c3a3dd2 );
		ROM_LOAD( "ha8.u8",       0x2000, 0x0400, 0x6cc64878 );
		ROM_LOAD( "ha7.u7",       0x2400, 0x0400, 0x6d2f9527 );
		ROM_LOAD( "ha6.u6",       0x2800, 0x0400, 0xe467c353 );
		ROM_LOAD( "ha3.u3",       0x2c00, 0x0400, 0x6a55eda8 );
		/* 3000-37ff empty */
		ROM_LOAD( "ha2.u2",       0x3800, 0x0400, 0x056b3b8b );
		ROM_LOAD( "ha1.u1",       0x3c00, 0x0400, 0xb8da2b5e );
	
		ROM_REGION( 0x0020, REGION_PROMS, 0 );
		ROM_LOAD( "316-138.u49",  0x0000, 0x0020, 0x67104ea9 );
	
		ROM_REGION( 0x0040, REGION_USER1, 0 );/* misc PROMs */
		ROM_LOAD( "316-0043.u87", 0x0000, 0x0020, 0xe60a7960 );/* control PROM */
		ROM_LOAD( "316-0042.u88", 0x0020, 0x0020, 0xa1506b9d );/* sequence PROM */
	ROM_END(); }}; 
	
	
	
	static void vicdual_decode()
	{
		UBytePtr RAM = memory_region(REGION_CPU1);
	
	
		/* copy the ROMs to the mirror image */
		memcpy(new UBytePtr(RAM,0x4000),new UBytePtr(RAM,0x0000),0x4000);
	}
	
	
	
	public static InitDriverPtr init_nosamples = new InitDriverPtr() { public void handler()
	{
		vicdual_decode();
	} };
	
	public static InitDriverPtr init_depthch = new InitDriverPtr() { public void handler()
	{
		install_port_read_handler(0, 0x08, 0x08, depthch_input_port_1_r);
	
		/* install sample trigger */
		/*TODO*///install_port_write_handler(0, 0x04, 0x04, depthch_sh_port1_w);
	
		vicdual_decode();
	} };
	
	public static InitDriverPtr init_samurai = new InitDriverPtr() { public void handler()
	{
		/* install protection handlers */
		install_mem_write_handler(0, 0x7f00, 0x7f00, samurai_protection_w);
		install_port_read_handler(0, 0x01, 0x03, samurai_input_r);
	
		vicdual_decode();
	} };
	
	public static InitDriverPtr init_carnival = new InitDriverPtr() { public void handler()
	{
		/* install sample triggers */
		/*TODO*///install_port_write_handler(0, 0x01, 0x01, carnival_sh_port1_w);
		/*TODO*///install_port_write_handler(0, 0x02, 0x02, carnival_sh_port2_w);
	
		vicdual_decode();
	} };
	
	public static InitDriverPtr init_invinco = new InitDriverPtr() { public void handler()
	{
		/* install sample trigger */
		/*TODO*///install_port_write_handler(0, 0x02, 0x02, invinco_sh_port2_w);
	
		vicdual_decode();
	} };
	
	public static InitDriverPtr init_invho2 = new InitDriverPtr() { public void handler()
	{
		/* install sample trigger */
		/*TODO*///install_port_write_handler(0, 0x02, 0x02, invinco_sh_port2_w);
	
		vicdual_decode();
	} };
	
	public static InitDriverPtr init_invds = new InitDriverPtr() { public void handler()
	{
		/* install sample trigger */
		/*TODO*///install_port_write_handler(0, 0x01, 0x01, invinco_sh_port2_w);
	
		vicdual_decode();
	} };
	
	public static InitDriverPtr init_pulsar = new InitDriverPtr() { public void handler()
	{
		/* install sample triggers */
		/*TODO*///install_port_write_handler(0, 0x01, 0x01, pulsar_sh_port1_w);
		/*TODO*///install_port_write_handler(0, 0x02, 0x02, pulsar_sh_port2_w);
	
		vicdual_decode();
	} };
	
	
	public static GameDriver driver_depthch	   = new GameDriver("1977"	,"depthch"	,"vicdual.java"	,rom_depthch,null	,machine_driver_depthch	,input_ports_depthch	,init_depthch	,ROT0	,	"Gremlin", "Depthcharge" );
	public static GameDriver driver_safari	   = new GameDriver("1977"	,"safari"	,"vicdual.java"	,rom_safari,null	,machine_driver_safari	,input_ports_safari	,init_nosamples	,ROT0	,	"Gremlin", "Safari", GAME_NO_SOUND );
	public static GameDriver driver_frogs	   = new GameDriver("1978"	,"frogs"	,"vicdual.java"	,rom_frogs,null	,machine_driver_2ports	,input_ports_frogs	,init_nosamples	,ROT0	,	"Gremlin", "Frogs", GAME_NO_SOUND );
	public static GameDriver driver_sspaceat	   = new GameDriver("1979"	,"sspaceat"	,"vicdual.java"	,rom_sspaceat,null	,machine_driver_3ports	,input_ports_sspaceat	,init_nosamples	,ROT270	,	"Sega", "Space Attack (upright)", GAME_NO_SOUND );
	public static GameDriver driver_sspacat2	   = new GameDriver("1979"	,"sspacat2"	,"vicdual.java"	,rom_sspacat2,driver_sspaceat	,machine_driver_3ports	,input_ports_sspaceat	,init_nosamples	,ROT270	,	"Sega", "Space Attack (upright, older)", GAME_NO_SOUND );
	public static GameDriver driver_sspacatc	   = new GameDriver("1979"	,"sspacatc"	,"vicdual.java"	,rom_sspacatc,driver_sspaceat	,machine_driver_3ports	,input_ports_sspaceat	,init_nosamples	,ROT270	,	"Sega", "Space Attack (cocktail)", GAME_NO_SOUND );
	public static GameDriver driver_headon	   = new GameDriver("1979"	,"headon"	,"vicdual.java"	,rom_headon,null	,machine_driver_2ports	,input_ports_headon	,init_nosamples	,ROT0	,	"Gremlin", "Head On (2 players)", GAME_NO_SOUND );
	public static GameDriver driver_headonb	   = new GameDriver("1979"	,"headonb"	,"vicdual.java"	,rom_headonb,driver_headon	,machine_driver_2ports	,input_ports_headon	,init_nosamples	,ROT0	,	"Gremlin", "Head On (1 player)", GAME_NO_SOUND );
	public static GameDriver driver_headon2	   = new GameDriver("1979"	,"headon2"	,"vicdual.java"	,rom_headon2,null	,machine_driver_3ports	,input_ports_headon2	,init_nosamples	,ROT0	,	"Sega", "Head On 2", GAME_NO_SOUND );
	public static GameDriver driver_invho2	   = new GameDriver("1979"	,"invho2"	,"vicdual.java"	,rom_invho2,null	,machine_driver_invinco4	,input_ports_invho2	,init_invho2	,ROT270	,	"Sega", "Invinco / Head On 2" );
	public static GameDriver driver_samurai	   = new GameDriver("1980"	,"samurai"	,"vicdual.java"	,rom_samurai,null	,machine_driver_4ports	,input_ports_samurai	,init_samurai	,ROT270	,	"Sega", "Samurai (Sega)", GAME_NO_SOUND );
	public static GameDriver driver_invinco	   = new GameDriver("1979"	,"invinco"	,"vicdual.java"	,rom_invinco,null	,machine_driver_invinco3	,input_ports_invinco	,init_invinco	,ROT270	,	"Sega", "Invinco" );
	public static GameDriver driver_invds	   = new GameDriver("1979"	,"invds"	,"vicdual.java"	,rom_invds,null	,machine_driver_invinco4	,input_ports_invds	,init_invds	,ROT270	,	"Sega", "Invinco / Deep Scan" );
	public static GameDriver driver_tranqgun	   = new GameDriver("1980"	,"tranqgun"	,"vicdual.java"	,rom_tranqgun,null	,machine_driver_4ports	,input_ports_tranqgun	,init_nosamples	,ROT270	,	"Sega", "Tranquilizer Gun", GAME_NO_SOUND );
	public static GameDriver driver_spacetrk	   = new GameDriver("1980"	,"spacetrk"	,"vicdual.java"	,rom_spacetrk,null	,machine_driver_4ports	,input_ports_spacetrk	,init_nosamples	,ROT270	,	"Sega", "Space Trek (upright)", GAME_NO_SOUND );
	public static GameDriver driver_sptrekct	   = new GameDriver("1980"	,"sptrekct"	,"vicdual.java"	,rom_sptrekct,driver_spacetrk	,machine_driver_4ports	,input_ports_sptrekct	,init_nosamples	,ROT270	,	"Sega", "Space Trek (cocktail)", GAME_NO_SOUND );
	public static GameDriver driver_carnival	   = new GameDriver("1980"	,"carnival"	,"vicdual.java"	,rom_carnival,null	,machine_driver_carnival	,input_ports_carnival	,init_carnival	,ROT270	,	"Sega", "Carnival (upright)" );
	public static GameDriver driver_carnvckt	   = new GameDriver("1980"	,"carnvckt"	,"vicdual.java"	,rom_carnvckt,driver_carnival	,machine_driver_carnival	,input_ports_carnvckt	,init_carnival	,ROT270	,	"Sega", "Carnival (cocktail)" );
	public static GameDriver driver_digger	   = new GameDriver("1980"	,"digger"	,"vicdual.java"	,rom_digger,null	,machine_driver_3ports	,input_ports_digger	,init_nosamples	,ROT270	,	"Sega", "Digger", GAME_NO_SOUND );
	public static GameDriver driver_pulsar	   = new GameDriver("1981"	,"pulsar"	,"vicdual.java"	,rom_pulsar,null	,machine_driver_pulsar	,input_ports_pulsar	,init_pulsar	,ROT270	,	"Sega", "Pulsar" );
	public static GameDriver driver_heiankyo	   = new GameDriver("1979"	,"heiankyo"	,"vicdual.java"	,rom_heiankyo,null	,machine_driver_4ports	,input_ports_heiankyo	,init_nosamples	,ROT270	,	"Denki Onkyo", "Heiankyo Alien", GAME_NO_SOUND );
}
