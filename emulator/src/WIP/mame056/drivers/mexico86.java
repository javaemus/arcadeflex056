/***************************************************************************

Kick & Run - (c) 1987 Taito

Ernesto Corvi
ernesto@imagina.com

Notes:
- 4 players mode is not emulated. THis involves some shared RAM and a subboard.
  There is additional code for a third Z80 in the bootleg version, I don't
  know if it's related or if its just a replacement for the 68705.

- kicknrun does a PS4 STOP ERROR short after boot, but works afterwards.
  PS4 is the mcu.

- kikikai sometimes crashes, might be a synchronization issue

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static WIP.mame056.machine.mexico86.*;
import static arcadeflex056.fucPtr.*;
import static common.ptr.*;

import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.mame.*;
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
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;
import static mame056.timer.*;
import static mame056.timerH.*;

import static mame056.vidhrdw.generic.*;

import static arcadeflex056.fileio.*;
import static mame056.palette.*;
import static mame056.inptport.*;
import static mame056.sound.sn76477H.*;
import static mame056.sound.sn76477.*;

import static WIP.mame056.vidhrdw.mexico86.*;
// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.sound._2203intf.*;
import static mame056.sound._2203intfH.*;

public class mexico86
{
	
	static UBytePtr shared = new UBytePtr();
	
	public static ReadHandlerPtr shared_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return shared.read(offset);
	} };
	
	public static WriteHandlerPtr shared_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		shared.write(offset, data);
	} };
	
	/*
	$f008 - write
	bit 7 = ? (unused?)
	bit 6 = ? (unused?)
	bit 5 = ? (unused?)
	bit 4 = ? (usually set in game)
	bit 3 = ? (usually set in game)
	bit 2 = sound cpu reset line
	bit 1 = microcontroller reset line
	bit 0 = ? (unused?)
	*/
	public static WriteHandlerPtr mexico86_f008_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_set_reset_line(1,(data & 4)!=0 ? CLEAR_LINE : ASSERT_LINE);
		cpu_set_reset_line(2,(data & 2)!=0 ? CLEAR_LINE : ASSERT_LINE);
	} };
	
	
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0xbfff, MRA_BANK1 ),	/* banked roms */
		new Memory_ReadAddress( 0xc000, 0xe7ff, shared_r ),	/* shared with sound cpu */
		new Memory_ReadAddress( 0xe800, 0xe8ff, MRA_RAM ),	/* protection ram */
		new Memory_ReadAddress( 0xe900, 0xefff, MRA_RAM ),
		new Memory_ReadAddress( 0xf010, 0xf010, input_port_5_r ),
		new Memory_ReadAddress( 0xf800, 0xffff, MRA_RAM ),	/* communication ram - to connect 4 players's subboard */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
        
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0xbfff, MWA_ROM ),
		new Memory_WriteAddress( 0xc000, 0xe7ff, shared_w, shared ),	/* shared with sound cpu */
		new Memory_WriteAddress( 0xc000, 0xcfff, MWA_RAM, mexico86_videoram ),
		new Memory_WriteAddress( 0xd500, 0xd7ff, MWA_RAM, mexico86_objectram, mexico86_objectram_size ),
		new Memory_WriteAddress( 0xe800, 0xe8ff, MWA_RAM, mexico86_protection_ram ),	/* shared with mcu */
		new Memory_WriteAddress( 0xe900, 0xefff, MWA_RAM ),
		new Memory_WriteAddress( 0xf000, 0xf000, mexico86_bankswitch_w ),	/* program and gfx ROM banks */
		new Memory_WriteAddress( 0xf008, 0xf008, mexico86_f008_w ),	/* cpu reset lines + other unknown stuff */
		new Memory_WriteAddress( 0xf018, 0xf018, MWA_NOP ),	// watchdog_reset_w },
		new Memory_WriteAddress( 0xf800, 0xffff, MWA_RAM ),	/* communication ram */
                new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0xa7ff, shared_r ),
		new Memory_ReadAddress( 0xa800, 0xbfff, MRA_RAM ),
		new Memory_ReadAddress( 0xc000, 0xc000, YM2203_status_port_0_r ),
		new Memory_ReadAddress( 0xc001, 0xc001, YM2203_read_port_0_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0xa7ff, shared_w ),
		new Memory_WriteAddress( 0xa800, 0xbfff, MWA_RAM ),
		new Memory_WriteAddress( 0xc000, 0xc000, YM2203_control_port_0_w ),
		new Memory_WriteAddress( 0xc001, 0xc001, YM2203_write_port_0_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_ReadAddress m68705_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x0000, mexico86_68705_portA_r ),
		new Memory_ReadAddress( 0x0001, 0x0001, mexico86_68705_portB_r ),
		new Memory_ReadAddress( 0x0002, 0x0002, input_port_0_r ),	/* COIN */
		new Memory_ReadAddress( 0x0010, 0x007f, MRA_RAM ),
		new Memory_ReadAddress( 0x0080, 0x07ff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress m68705_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x0000, mexico86_68705_portA_w ),
		new Memory_WriteAddress( 0x0001, 0x0001, mexico86_68705_portB_w ),
		new Memory_WriteAddress( 0x0004, 0x0004, mexico86_68705_ddrA_w ),
		new Memory_WriteAddress( 0x0005, 0x0005, mexico86_68705_ddrB_w ),
		new Memory_WriteAddress( 0x000a, 0x000a, MWA_NOP ),	/* looks like a bug in the code, writes to */
										/* 0x0a (=10dec) instead of 0x10 */
		new Memory_WriteAddress( 0x0010, 0x007f, MWA_RAM ),
		new Memory_WriteAddress( 0x0080, 0x07ff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	static InputPortPtr input_ports_mexico86 = new InputPortPtr(){ public void handler() { 
	PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_SERVICE );/* service 2 */
	
		PORT_START();       /* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_START(); 
		/* When Bit 1 is On, the machine waits a signal from another one */
		/* Seems like if you can join two cabinets, one as master */
		/* and the other as slave, probably to play four players */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );	PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x10, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "1C_2C") );
		PORT_DIPNAME( 0xc0, 0xc0, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x40, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0xc0, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0x80, DEF_STR( "1C_2C") );
	
		PORT_START(); 
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x0c, 0x08, "Timer" );	PORT_DIPSETTING(    0x04, "Slow" );	PORT_DIPSETTING(    0x08, "Normal" );	PORT_DIPSETTING(    0x0c, "Fast" );	PORT_DIPSETTING(    0x00, "Fastest" );	PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		/* The following dip seems to be related with the first one */
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Max Players" );	PORT_DIPSETTING(    0x80, "2" );	PORT_DIPSETTING(    0x00, "4" );
		PORT_START(); 
		/* the following is actually service coin 1 */
		PORT_BITX(0x01, IP_ACTIVE_LOW, IPT_SERVICE, "Advance", KEYCODE_F1, IP_JOY_NONE );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_TILT );	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_kikikai = new InputPortPtr(){ public void handler() { 
	PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_START();       /* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_COCKTAIL );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_COCKTAIL );	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_COCKTAIL );	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_START();       /* DSW0 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x01, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x04, IP_ACTIVE_LOW );	PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x08, DEF_STR( "On") );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x10, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "1C_2C") );
		PORT_DIPNAME( 0xc0, 0xc0, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x40, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0xc0, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0x80, DEF_STR( "1C_2C") );
	
		PORT_START();       /* DSW1 */
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x02, "Easy" );	PORT_DIPSETTING(    0x03, "Normal" );	PORT_DIPSETTING(    0x01, "Hard" );	PORT_DIPSETTING(    0x00, "Hardest" );	PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x00, "50000 100000" );	PORT_DIPSETTING(    0x0c, "70000 150000" );	PORT_DIPSETTING(    0x08, "70000 200000" );	PORT_DIPSETTING(    0x04, "100000 300000" );	PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "2" );	PORT_DIPSETTING(    0x30, "3" );	PORT_DIPSETTING(    0x20, "4" );	PORT_DIPSETTING(    0x10, "5" );	PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Number Match" );	PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN3 );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_TILT );	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );INPUT_PORTS_END(); }}; 
	
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,
		4*2048,
		4,
		new int[] { 0x20000*8, 0x20000*8+4, 0, 4 },
		new int[] { 3, 2, 1, 0, 8+3, 8+2, 8+1, 8+0 },
		new int[] { 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16 },
		16*8
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,   0, 16 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	static YM2203interface ym2203_interface = new YM2203interface
	(
		1,			/* 1 chip */
		3000000,	/* 3 MHz ??? */
		new int[]{ YM2203_VOL(40,40) },
		new ReadHandlerPtr[]{ input_port_3_r },
		new ReadHandlerPtr[]{ input_port_4_r },
		new WriteHandlerPtr[]{ null },
		new WriteHandlerPtr[]{ null }
        );
	
	
	
	
	//MACHINEDRIVER( mexico86 )
        static MachineDriver machine_driver_mexico86 = new MachineDriver
	(																					
		new MachineCPU[] {																				
			new MachineCPU(																			
				CPU_Z80,																
				6000000,		/* 6 MHz??? */											
				readmem,writemem,null,null,													
				ignore_interrupt,0	/* IRQs are triggered by the 68705 */				
			),																			
			new MachineCPU(																			
				CPU_Z80,																
				6000000,		/* 6 MHz??? */											
				sound_readmem,sound_writemem,null,null,										
				interrupt,1																
			),																			
			new MachineCPU(																			
				CPU_M68705,																
				4000000/2,	/* xtal is 4MHz (????) I think it's divided by 2 internally */	
				m68705_readmem,m68705_writemem,null,null,										
				mexico86_m68705_interrupt,2												
			)																			
		},																				
		60, DEFAULT_60HZ_VBLANK_DURATION,  /* frames per second, vblank duration */		
		100,	/* 100 CPU slices per frame - an high value to ensure proper */			
				/* synchronization of the CPUs */										
		null,																				
																						
		/* video hardware */															
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 2*8, 30*8-1 ),										
		gfxdecodeinfo,																	
		256, 0,																			
		palette_RRRR_GGGG_BBBB_convert_prom,											
																						
		VIDEO_TYPE_RASTER,																
		null,																				
		null,																				
		null,																				
		mexico86_vh_screenrefresh,														
																						
		/* sound hardware */															
		0,0,0,0,																		
		new MachineSound[] {																				
			new MachineSound(																			
				SOUND_YM2203,															
				ym2203_interface														
			)																			
		}																				
	);
	//MACHINEDRIVER( kikikai )
	static MachineDriver machine_driver_kikikai = new MachineDriver
	(																					
		new MachineCPU[] {																				
			new MachineCPU(																			
				CPU_Z80,																
				6000000,		/* 6 MHz??? */											
				readmem,writemem,null,null,													
				ignore_interrupt,0	/* IRQs are triggered by the 68705 */				
			),																			
			new MachineCPU(																			
				CPU_Z80,																
				6000000,		/* 6 MHz??? */											
				sound_readmem,sound_writemem,null,null,										
				interrupt,1																
			),																			
			new MachineCPU(																			
				CPU_M68705,																
				4000000/2,	/* xtal is 4MHz (????) I think it's divided by 2 internally */	
				m68705_readmem,m68705_writemem,null,null,										
				mexico86_m68705_interrupt,2												
			)																			
		},																				
		60, DEFAULT_60HZ_VBLANK_DURATION,  /* frames per second, vblank duration */		
		100,	/* 100 CPU slices per frame - an high value to ensure proper */			
				/* synchronization of the CPUs */										
		null,																				
																						
		/* video hardware */															
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 2*8, 30*8-1 ),										
		gfxdecodeinfo,																	
		256, 0,																			
		palette_RRRR_GGGG_BBBB_convert_prom,											
																						
		VIDEO_TYPE_RASTER,																
		null,																				
		null,																				
		null,																				
		kikikai_vh_screenrefresh,														
																						
		/* sound hardware */															
		0,0,0,0,																		
		new MachineSound[] {																				
			new MachineSound(																			
				SOUND_YM2203,															
				ym2203_interface														
			)																			
		}																				
	);
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_kikikai = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x28000, REGION_CPU1, 0 ); /* 196k for code */
		ROM_LOAD( "a85-17.rom", 0x00000, 0x08000, 0xc141d5ab );/* 1st half, main code		 */
		ROM_CONTINUE(           0x20000, 0x08000 );		   /* 2nd half, banked at 0x8000 */
		ROM_LOAD( "a85-16.rom", 0x10000, 0x10000, 0x4094d750 );/* banked at 0x8000			 */
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 ); /* 64k for the audio cpu */
		ROM_LOAD( "a85-11.rom", 0x0000, 0x8000, 0xcc3539db );
		ROM_REGION( 0x0800, REGION_CPU3, 0 );/* 2k for the microcontroller */
		ROM_LOAD( "knightb.uc", 0x0000, 0x0800, 0x3cc2bbe4 );
		ROM_REGION( 0x40000, REGION_GFX1, ROMREGION_DISPOSE | ROMREGION_INVERT );	ROM_LOAD( "a85-15.rom", 0x00000, 0x10000, 0xaebc8c32 );	ROM_LOAD( "a85-14.rom", 0x10000, 0x10000, 0xa9df0453 );	ROM_LOAD( "a85-13.rom", 0x20000, 0x10000, 0x3eeaf878 );	ROM_LOAD( "a85-12.rom", 0x30000, 0x10000, 0x91e58067 );
		ROM_REGION( 0x0300, REGION_PROMS, 0 );	ROM_LOAD( "a85-08.rom", 0x0000, 0x0100, 0xd15f61a8 );	ROM_LOAD( "a85-10.rom", 0x0100, 0x0100, 0x8fc3fa86 );	ROM_LOAD( "a85-09.rom", 0x0200, 0x0100, 0xb931c94d );ROM_END(); }}; 
	
	static RomLoadPtr rom_kicknrun = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x28000, REGION_CPU1, 0 ); /* 196k for code */
		ROM_LOAD( "a87-08.bin", 0x00000, 0x08000, 0x715e1b04 );/* 1st half, main code		 */
		ROM_CONTINUE(           0x20000, 0x08000 );		   /* 2nd half, banked at 0x8000 */
		ROM_LOAD( "a87-07.bin", 0x10000, 0x10000, 0x6cb6ebfe );/* banked at 0x8000			 */
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 ); /* 64k for the audio cpu */
		ROM_LOAD( "a87-06.bin", 0x0000, 0x8000, 0x1625b587 );
		ROM_REGION( 0x0800, REGION_CPU3, 0 );/* 2k for the microcontroller */
		ROM_LOAD( "knrmcu.bin",   0x0000, 0x0800, BADCRC(0x8e821fa0));	/* manually crafted from the Mexico '86 one */
	
		ROM_REGION( 0x40000, REGION_GFX1, ROMREGION_DISPOSE | ROMREGION_INVERT );	ROM_LOAD( "a87-05.bin", 0x08000, 0x08000, 0x4eee3a8a );	ROM_CONTINUE(           0x00000, 0x08000 );	ROM_LOAD( "a87-04.bin", 0x10000, 0x08000, 0x8b438d20 );	ROM_RELOAD(             0x18000, 0x08000 );	ROM_LOAD( "a87-03.bin", 0x28000, 0x08000, 0xf42e8a88 );	ROM_CONTINUE(           0x20000, 0x08000 );	ROM_LOAD( "a87-02.bin", 0x30000, 0x08000, 0x64f1a85f );	ROM_RELOAD(             0x38000, 0x08000 );
		ROM_REGION( 0x0300, REGION_PROMS, 0 );	ROM_LOAD( "a87-10.bin", 0x0000, 0x0100, 0xbe6eb1f0 );	ROM_LOAD( "a87-12.bin", 0x0100, 0x0100, 0x3e953444 );	ROM_LOAD( "a87-11.bin", 0x0200, 0x0100, 0x14f6c28d );ROM_END(); }}; 
	
	static RomLoadPtr rom_mexico86 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x28000, REGION_CPU1, 0 ); /* 196k for code */
		ROM_LOAD( "2_g.bin",    0x00000, 0x08000, 0x2bbfe0fb );/* 1st half, main code		 */
		ROM_CONTINUE(           0x20000, 0x08000 );		   /* 2nd half, banked at 0x8000 */
		ROM_LOAD( "1_f.bin",    0x10000, 0x10000, 0x0b93e68e );/* banked at 0x8000			 */
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 ); /* 64k for the audio cpu */
		ROM_LOAD( "a87-06.bin", 0x0000, 0x8000, 0x1625b587 );
		ROM_REGION( 0x0800, REGION_CPU3, 0 );/* 2k for the microcontroller */
		ROM_LOAD( "68_h.bin",   0x0000, 0x0800, 0xff92f816 );
		ROM_REGION( 0x40000, REGION_GFX1, ROMREGION_DISPOSE | ROMREGION_INVERT );	ROM_LOAD( "4_d.bin",    0x08000, 0x08000, 0x57cfdbca );	ROM_CONTINUE(           0x00000, 0x08000 );	ROM_LOAD( "5_c.bin",    0x10000, 0x08000, 0xe42fa143 );	ROM_RELOAD(             0x18000, 0x08000 );	ROM_LOAD( "6_b.bin",    0x28000, 0x08000, 0xa4607989 );	ROM_CONTINUE(           0x20000, 0x08000 );	ROM_LOAD( "7_a.bin",    0x30000, 0x08000, 0x245036b1 );	ROM_RELOAD(             0x38000, 0x08000 );
		ROM_REGION( 0x0300, REGION_PROMS, 0 );	ROM_LOAD( "a87-10.bin", 0x0000, 0x0100, 0xbe6eb1f0 );	ROM_LOAD( "a87-12.bin", 0x0100, 0x0100, 0x3e953444 );	ROM_LOAD( "a87-11.bin", 0x0200, 0x0100, 0x14f6c28d );ROM_END(); }}; 
	
	
	
	public static GameDriver driver_kikikai	   = new GameDriver("1986"	,"kikikai"	,"mexico86.java"	,rom_kikikai,null	,machine_driver_kikikai	,input_ports_kikikai	,null	,ROT90	,	"Taito Corporation", "KiKi KaiKai", GAME_NOT_WORKING );
	public static GameDriver driver_kicknrun	   = new GameDriver("1986"	,"kicknrun"	,"mexico86.java"	,rom_kicknrun,null	,machine_driver_mexico86	,input_ports_mexico86	,null	,ROT0	,	"Taito Corporation", "Kick and Run" );
	public static GameDriver driver_mexico86	   = new GameDriver("1986"	,"mexico86"	,"mexico86.java"	,rom_mexico86,driver_kicknrun	,machine_driver_mexico86	,input_ports_mexico86	,null	,ROT0	,	"bootleg", "Mexico 86" );
}
