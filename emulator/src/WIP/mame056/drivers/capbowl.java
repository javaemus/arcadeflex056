/***************************************************************************

  Coors Light Bowling/Bowl-O-Rama memory map

  driver by Zsolt Vasvari

  CPU Board:

  0000-3fff     3 Graphics ROMS mapped in using 0x4800 (Coors Light Bowling only)
  0000-001f		Turbo board area (Bowl-O-Rama only) See Below.
  4000          Display row selected
  4800          Graphics ROM select
  5000-57ff     Battery backed up RAM (Saves machine state after shut off)
                Enter setup menu by holding down the F2 key on the
                high score screen
  5800-5fff		TMS34061 area

                First 0x20 bytes of each row provide a 16 color palette for this
                row. 2 bytes per color: 0000RRRR GGGGBBBB.

                Remaining 0xe0 bytes contain 2 pixels each for a total of
                448 pixels, but only 360 seem to be displayed.
                (Each row appears vertically because the monitor is rotated)

  6000          Sound command
  6800			Trackball Reset. Double duties as a watchdog.
  7000          Input port 1    Bit 0-3 Trackball Vertical Position
							  	Bit 4   Player 2 Hook Left
								Bit 5   Player 2 Hook Right
								Bit 6   Upright/Cocktail DIP Switch
                                Bit 7   Coin 2
  7800          Input port 2    Bit 0-3 Trackball Horizontal Positon
                                Bit 4   Player 1 Hook Left
                                Bit 5   Player 1 Hook Right
                                Bit 6   Start
                                Bit 7   Coin 1
  8000-ffff		ROM


  Sound Board:

  0000-07ff		RAM
  1000-1001		YM2203
			  	Port A D7 Read  is ticket sensor
				Port B D7 Write is ticket dispenser enable
				Port B D6 looks like a diagnostics LED to indicate that
				          the PCB is operating. It's pulsated by the
						  sound CPU. It is kind of pointless to emulate it.
  2000			Not hooked up according to the schematics
  6000			DAC write
  7000			Sound command read (0x34 is used to dispense a ticket)
  8000-ffff		ROM


  Turbo Board Layout (Plugs in place of GR0):

  Bowl-O-Rama	Copyright 1991 P&P Marketing
				Marquee says "EXIT Entertainment"

				This portion: Mike Appolo with the help of Andrew Pines.
				Andrew was one of the game designers for Capcom Bowling,
				Coors Light Bowling, Strata Bowling, and Bowl-O-Rama.

				This game was an upgrade for Capcom Bowling and included a
				"Turbo PCB" that had a GAL address decoder / data mask

  Memory Map for turbo board (where GR0 is on Capcom Bowling PCBs:

  0000   		Read Mask
  0001-0003		Unused
  0004  		Read Data
  0005-0007		Unused
  0008  		GR Address High Byte (GR17-16)
  0009-0016		Unused
  0017			GR Address Middle Byte (GR15-0 written as a word to 0017-0018)
  0018  		GR address Low byte
  0019-001f		Unused

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static WIP.mame056.vidhrdw.capbowl.*;
import static WIP.mame056.machine.capbowl.*;
import static mame056.machine.ticket.*;
import static mame056.machine.ticketH.*;

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

import static mame056.sound._2203intf.*;
import static mame056.sound._2203intfH.*;
import static mame056.vidhrdw.generic.*;

import static arcadeflex056.fileio.*;
import static mame056.palette.*;

public class capbowl
{
	
	/*************************************
	 *
	 *	NVRAM
	 *
	 *************************************/
	
	static UBytePtr nvram = new UBytePtr();
	static int[] nvram_size = new int[1];
	
	static nvramPtr nvram_handler = new nvramPtr() {
            public void handler(Object file, int read_or_write) {
                if (read_or_write != 0)
			osd_fwrite(file,nvram,nvram_size[0]);
		else
		{
			if (file != null)
				osd_fread(file,nvram,nvram_size[0]);
			else
			{
				/* invalidate nvram to make the game initialize it.
				   A 0xff fill will cause the game to malfunction, so we use a
				   0x01 fill which seems OK */
				memset(nvram,0x01,nvram_size[0]);
			}
		}
            }
        };
	
	
	/*************************************
	 *
	 *	Sound commands
	 *
	 *************************************/
	
	public static WriteHandlerPtr capbowl_sndcmd_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_cause_interrupt(1, M6809_INT_IRQ);
	
		soundlatch_w.handler(offset, data);
	} };
	
	
	
	/*************************************
	 *
	 *	Handler called by the 2203 emulator
	 *	when the internal timers cause an IRQ
	 *
	 *************************************/
	
	static WriteYmHandlerPtr firqhandler = new WriteYmHandlerPtr() {
            public void handler(int irq) {
                cpu_set_irq_line(1, 1, irq!=0 ? ASSERT_LINE : CLEAR_LINE);
            }
        };
	
	
	/*************************************
	 *
	 *	NMI is to trigger the self test.
	 *	We use a fake input port to tie
	 *	that event to a keypress
	 *
	 *************************************/
	
	public static InterruptPtr capbowl_interrupt = new InterruptPtr() { public int handler() 
	{
		if ((readinputport(4) & 1) != 0)	/* get status of the F2 key */
			return nmi_interrupt.handler();	/* trigger self test */
	
		return ignore_interrupt.handler();
	} };
	
	
	
	/*************************************
	 *
	 *	Trackball input handlers
	 *
	 *************************************/
	
	static int[] track=new int[2];
	
	public static ReadHandlerPtr track_0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return (input_port_0_r.handler(offset) & 0xf0) | ((input_port_2_r.handler(offset) - track[0]) & 0x0f);
	} };
	
	public static ReadHandlerPtr track_1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return (input_port_1_r.handler(offset) & 0xf0) | ((input_port_3_r.handler(offset) - track[1]) & 0x0f);
	} };
	
	public static WriteHandlerPtr track_reset_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* reset the trackball counters */
		track[0] = input_port_2_r.handler(offset);
		track[1] = input_port_3_r.handler(offset);
	
		watchdog_reset_w.handler(offset,data);
	} };
	
	
	
	/*************************************
	 *
	 *	Main CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress capbowl_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x3fff, MRA_BANK1 ),
		new Memory_ReadAddress( 0x5000, 0x57ff, MRA_RAM ),
		new Memory_ReadAddress( 0x5800, 0x5fff, capbowl_tms34061_r ),
		new Memory_ReadAddress( 0x7000, 0x7000, track_0_r ),	/* + other inputs */
		new Memory_ReadAddress( 0x7800, 0x7800, track_1_r ),	/* + other inputs */
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress bowlrama_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	
                new Memory_ReadAddress( 0x0000, 0x001f, bowlrama_turbo_r ),
		new Memory_ReadAddress( 0x5000, 0x57ff, MRA_RAM ),
		new Memory_ReadAddress( 0x5800, 0x5fff, capbowl_tms34061_r ),
		new Memory_ReadAddress( 0x7000, 0x7000, track_0_r ),	/* + other inputs */
		new Memory_ReadAddress( 0x7800, 0x7800, track_1_r ),	/* + other inputs */
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	
                new Memory_WriteAddress( 0x0000, 0x001f, bowlrama_turbo_w ),	/* Bowl-O-Rama only */
		new Memory_WriteAddress( 0x4000, 0x4000, MWA_RAM, capbowl_rowaddress ),
		new Memory_WriteAddress( 0x4800, 0x4800, capbowl_rom_select_w ),
		new Memory_WriteAddress( 0x5000, 0x57ff, MWA_RAM, nvram, nvram_size ),
		new Memory_WriteAddress( 0x5800, 0x5fff, capbowl_tms34061_w ),
		new Memory_WriteAddress( 0x6000, 0x6000, capbowl_sndcmd_w ),
		new Memory_WriteAddress( 0x6800, 0x6800, track_reset_w ),	/* + watchdog */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	/*************************************
	 *
	 *	Sound CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x07ff, MRA_RAM ),
		new Memory_ReadAddress( 0x1000, 0x1000, YM2203_status_port_0_r ),
		new Memory_ReadAddress( 0x1001, 0x1001, YM2203_read_port_0_r ),
		new Memory_ReadAddress( 0x7000, 0x7000, soundlatch_r ),
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x07ff, MWA_RAM),
		new Memory_WriteAddress( 0x1000, 0x1000, YM2203_control_port_0_w ),
		new Memory_WriteAddress( 0x1001, 0x1001, YM2203_write_port_0_w ),
		new Memory_WriteAddress( 0x2000, 0x2000, MWA_NOP ),  /* Not hooked up according to the schematics */
		new Memory_WriteAddress( 0x6000, 0x6000, DAC_0_data_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	/*************************************
	 *
	 *	Port definitions
	 *
	 *************************************/
	
	static InputPortPtr input_ports_capbowl = new InputPortPtr(){ public void handler() { 
	PORT_START(); 	/* IN0 */
		/* low 4 bits are for the trackball */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );	PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Cabinet") ); /* This version of Bowl-O-Rama */
		PORT_DIPSETTING(    0x40, DEF_STR( "Upright") );			   /* is Upright only */
		PORT_DIPSETTING(    0x00, DEF_STR( "Cocktail") );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_START(); 	/* IN1 */
		/* low 4 bits are for the trackball */
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START1 );	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_START(); 	/* FAKE */
		PORT_ANALOG( 0xff, 0x00, IPT_TRACKBALL_Y | IPF_REVERSE, 20, 40, 0, 0 );
		PORT_START(); 	/* FAKE */
		PORT_ANALOG( 0xff, 0x00, IPT_TRACKBALL_X, 20, 40, 0, 0 );
		PORT_START(); 	/* FAKE */
		/* This fake input port is used to get the status of the F2 key, */
		/* and activate the test mode, which is triggered by a NMI */
		PORT_BITX(0x01, IP_ACTIVE_HIGH, IPT_SERVICE, DEF_STR( "Service_Mode"), KEYCODE_F2, IP_JOY_NONE );
	INPUT_PORTS_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	Sound definitions
	 *
	 *************************************/
	
	static YM2203interface ym2203_interface = new YM2203interface
	(
		1,			/* 1 chip */
		4000000,	/* 4 MHz */
		new int[]{ YM2203_VOL(40,40) },
		new ReadHandlerPtr[]{ ticket_dispenser_r },
		new ReadHandlerPtr[]{ null },
		new WriteHandlerPtr[]{ null },
		new WriteHandlerPtr[]{ ticket_dispenser_w },  /* Also a status LED. See memory map above */
		new WriteYmHandlerPtr[]{ firqhandler }
	);
	
	
	static DACinterface dac_interface = new DACinterface
	(
		1,
		new int[] { 100 }
	);
	
	
	
	/*************************************
	 *
	 *	Machine driver
	 *
	 *************************************/
	
	//#define MACHINEDRIVER(NAME, VISIBLE_Y)						
	//MACHINEDRIVER(capbowl,  244)															
	static MachineDriver machine_driver_capbowl = new MachineDriver
	(															
		/* basic machine hardware */   							
		new MachineCPU[] {														
			new MachineCPU(													
				CPU_M6809,										
				2000000,										
				capbowl_readmem,writemem,null,null,					
				capbowl_interrupt,1							
			),													
			new MachineCPU(													
				CPU_M6809 | CPU_AUDIO_CPU,						
				2000000,										
				sound_readmem,sound_writemem,null,null,				
				ignore_interrupt,1								
			)													
		},														
		57, 5000,												
		1,														
		capbowl_init_machine,									
																
		/* video hardware */									
		360, 256, new rectangle( 0, 359, 0, 244 ),						
		null,														
		4096, 0,												
		null,														
																
		VIDEO_TYPE_RASTER,				
		null,														
		capbowl_vh_start,										
		capbowl_vh_stop,										
		capbowl_vh_screenrefresh,								
																
		/* sound hardware */									
		0,0,0,0,												
		new MachineSound[] {														
			new MachineSound(													
				SOUND_YM2203,									
				ym2203_interface								
			),													
			new MachineSound(													
				SOUND_DAC,										
				dac_interface									
			)													
		},														
																
		nvram_handler											
	);
	
	//MACHINEDRIVER(bowlrama, 239)
	static MachineDriver machine_driver_bowlrama = new MachineDriver
	(															
		/* basic machine hardware */   							
		new MachineCPU[] {														
			new MachineCPU(													
				CPU_M6809,										
				2000000,										
				bowlrama_readmem,writemem,null,null,					
				capbowl_interrupt,1							
			),													
			new MachineCPU(													
				CPU_M6809 | CPU_AUDIO_CPU,						
				2000000,										
				sound_readmem,sound_writemem,null,null,				
				ignore_interrupt,1								
			)													
		},														
		57, 5000,												
		1,														
		capbowl_init_machine,									
																
		/* video hardware */									
		360, 256, new rectangle( 0, 359, 0, 239 ),						
		null,														
		4096, 0,												
		null,														
																
		VIDEO_TYPE_RASTER,				
		null,														
		capbowl_vh_start,										
		capbowl_vh_stop,										
		capbowl_vh_screenrefresh,								
																
		/* sound hardware */									
		0,0,0,0,												
		new MachineSound[] {														
			new MachineSound(													
				SOUND_YM2203,									
				ym2203_interface								
			),													
			new MachineSound(													
				SOUND_DAC,										
				dac_interface									
			)													
		},														
																
		nvram_handler											
	);
	
	
	/*************************************
	 *
	 *	ROM definitions
	 *
	 *************************************/
	
	static RomLoadPtr rom_capbowl = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x28000, REGION_CPU1, 0 );	ROM_LOAD( "u6",           0x08000, 0x8000, 0x14924c96 );	ROM_LOAD( "gr0",          0x10000, 0x8000, 0xef53ca7a );	ROM_LOAD( "gr1",          0x18000, 0x8000, 0x27ede6ce );	ROM_LOAD( "gr2",          0x20000, 0x8000, 0xe49238f4 );
		ROM_REGION( 0x10000, REGION_CPU2, 0 );	ROM_LOAD( "sound",        0x8000, 0x8000, 0x8c9c3b8a );ROM_END(); }}; 
	
	
	static RomLoadPtr rom_capbowl2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x28000, REGION_CPU1, 0 );	ROM_LOAD( "progrev3.u6",  0x08000, 0x8000, 0x9162934a );	ROM_LOAD( "gr0",          0x10000, 0x8000, 0xef53ca7a );	ROM_LOAD( "gr1",          0x18000, 0x8000, 0x27ede6ce );	ROM_LOAD( "gr2",          0x20000, 0x8000, 0xe49238f4 );
		ROM_REGION( 0x10000, REGION_CPU2, 0 );	ROM_LOAD( "sound",        0x8000, 0x8000, 0x8c9c3b8a );ROM_END(); }}; 
	
	
	static RomLoadPtr rom_clbowl = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x28000, REGION_CPU1, 0 );	ROM_LOAD( "u6.cl",        0x08000, 0x8000, 0x91e06bc4 );	ROM_LOAD( "gr0.cl",       0x10000, 0x8000, 0x899c8f15 );	ROM_LOAD( "gr1.cl",       0x18000, 0x8000, 0x0ac0dc4c );	ROM_LOAD( "gr2.cl",       0x20000, 0x8000, 0x251f5da5 );
		ROM_REGION( 0x10000, REGION_CPU2, 0 );	ROM_LOAD( "sound.cl",     0x8000, 0x8000, 0x1eba501e );ROM_END(); }}; 
	
	
	static RomLoadPtr rom_bowlrama = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );	ROM_LOAD( "u6",           0x08000, 0x08000, 0x7103ad55 );
		ROM_REGION( 0x10000, REGION_CPU2, 0 );	ROM_LOAD( "u30",          0x8000, 0x8000, 0xf3168834 );
		ROM_REGION( 0x40000, REGION_GFX1, 0 );	ROM_LOAD( "ux7",          0x00000, 0x40000, 0x8727432a );ROM_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	Game drivers
	 *
	 *************************************/
	
	public static GameDriver driver_capbowl	   = new GameDriver("1988"	,"capbowl"	,"capbowl.java"	,rom_capbowl,null	,machine_driver_capbowl	,input_ports_capbowl	,null	,ROT270	,	"Incredible Technologies", "Capcom Bowling (set 1)" );
	public static GameDriver driver_capbowl2	   = new GameDriver("1988"	,"capbowl2"	,"capbowl.java"	,rom_capbowl2,driver_capbowl	,machine_driver_capbowl	,input_ports_capbowl	,null	,ROT270	,	"Incredible Technologies", "Capcom Bowling (set 2)" );
	public static GameDriver driver_clbowl	   = new GameDriver("1989"	,"clbowl"	,"capbowl.java"	,rom_clbowl,driver_capbowl	,machine_driver_capbowl	,input_ports_capbowl	,null	,ROT270	,	"Incredible Technologies", "Coors Light Bowling" );
	public static GameDriver driver_bowlrama	   = new GameDriver("1991"	,"bowlrama"	,"capbowl.java"	,rom_bowlrama,null	,machine_driver_bowlrama	,input_ports_capbowl	,null	,ROT270	,	"P & P Marketing", "Bowl-O-Rama" );
}
