/***************************************************************************

 Route 16/Stratovox memory map (preliminary)

 driver by Zsolt Vasvari

 Notes: Route 16 and Stratovox use identical hardware with the following
        exceptions: Stratovox has a DAC for voice.
        Route 16 has the added ability to turn off each bitplane indiviaually.
        This looks like an afterthought, as one of the same bits that control
        the palette selection is doubly utilized as the bitmap enable bit.

 CPU1

 0000-2fff ROM
 4000-43ff Shared RAM
 8000-bfff Video RAM

 I/O Read

 48xx IN0 - DIP Switches
 50xx IN1 - Input Port 1
 58xx IN2 - Input Port 2

 I/O Write

 48xx OUT0 - D0-D4 color select for VRAM 0
             D5    coin counter
 50xx OUT1 - D0-D4 color select for VRAM 1
             D5    VIDEO I/II (Flip Screen)

 I/O Port Write

 6800 AY-8910 Write Port
 6900 AY-8910 Control Port


 CPU2

 0000-1fff ROM
 4000-43ff Shared RAM
 8000-bfff Video RAM

 I/O Write

 2800      DAC output (Stratovox only)

 ***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static WIP.mame056.vidhrdw.route16.*;

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
import static mame056.sound.nes_apu.*;
import static mame056.sound.nes_apuH.*;
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;
import static mame056.sound.mixer.*;
import static mame056.sound.mixerH.*;
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;
import static mame056.sound.ay8910.*;
import static mame056.sound.ay8910H.*;
import static mame056.sound.sn76477.*;
import static mame056.sound.sn76477H.*;

import static mame056.sound._2151intf.*;
import static mame056.sound._2151intfH.*;
import static mame056.sound._2203intf.*;
import static mame056.sound._2203intfH.*;
import static mame056.vidhrdw.generic.*;

import static arcadeflex056.fileio.*;
import static mame056.palette.*;
import mame056.sound.sn76477H;

public class route16
{
	
	public static Memory_ReadAddress cpu1_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x2fff, MRA_ROM ),
		new Memory_ReadAddress( 0x4000, 0x43ff, route16_sharedram_r ),
		new Memory_ReadAddress( 0x4800, 0x4800, input_port_0_r ),
		new Memory_ReadAddress( 0x5000, 0x5000, input_port_1_r ),
		new Memory_ReadAddress( 0x5800, 0x5800, input_port_2_r ),
		new Memory_ReadAddress( 0x8000, 0xbfff, route16_videoram1_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress cpu1_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x2fff, MWA_ROM ),
		new Memory_WriteAddress( 0x4000, 0x43ff, route16_sharedram_w, route16_sharedram ),
		new Memory_WriteAddress( 0x4800, 0x4800, route16_out0_w ),
		new Memory_WriteAddress( 0x5000, 0x5000, route16_out1_w ),
		new Memory_WriteAddress( 0x8000, 0xbfff, route16_videoram1_w, route16_videoram1, route16_videoram_size ),
		new Memory_WriteAddress( 0xc000, 0xc000, MWA_RAM ), // Stratvox has an off by one error
	                                 // when clearing the screen
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static IO_WritePort cpu1_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x6800, 0x6800, AY8910_write_port_0_w ),
		new IO_WritePort( 0x6900, 0x6900, AY8910_control_port_0_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress cpu2_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x1fff, MRA_ROM ),
		new Memory_ReadAddress( 0x4000, 0x43ff, route16_sharedram_r ),
		new Memory_ReadAddress( 0x8000, 0xbfff, route16_videoram2_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress cpu2_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x1fff, MWA_ROM ),
		new Memory_WriteAddress( 0x2800, 0x2800, DAC_0_data_w ), // Not used by Route 16
		new Memory_WriteAddress( 0x4000, 0x43ff, route16_sharedram_w ),
		new Memory_WriteAddress( 0x8000, 0xbfff, route16_videoram2_w, route16_videoram2 ),
		new Memory_WriteAddress( 0xc000, 0xc1ff, MWA_NOP ), // Route 16 sometimes writes outside of
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	static InputPortPtr input_ports_route16 = new InputPortPtr(){ public void handler() { 
		PORT_START();       /* DSW 1 */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "3" );
		PORT_DIPSETTING(    0x01, "5" );
		PORT_DIPNAME( 0x02, 0x00, DEF_STR( "Unknown") ); // Doesn't seem to
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );                    // be referenced
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Unknown") ); // Doesn't seem to
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );                    // be referenced
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x18, 0x00, DEF_STR( "Coinage") );
		PORT_DIPSETTING(    0x08, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "1C_2C") );
	//	PORT_DIPSETTING(    0x18, DEF_STR( "2C_1C") ); // Same as 0x08
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		PORT_START();       /* Input Port 1 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY );
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_4WAY );
		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_4WAY );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_4WAY );
		PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_SERVICE( 0x40, IP_ACTIVE_HIGH );
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_COIN1 );
	
		PORT_START();       /* Input Port 2 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_4WAY | IPF_COCKTAIL );
		PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_START2 );
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_START1 );
	INPUT_PORTS_END(); }}; 
	
	
	
	static InputPortPtr input_ports_stratvox = new InputPortPtr(){ public void handler() { 
		PORT_START();       /* IN0 */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "3" );
		PORT_DIPSETTING(    0x01, "5" );
		PORT_DIPNAME( 0x02, 0x00, "Replenish Astronouts" );
		PORT_DIPSETTING(    0x00, DEF_STR( "No") );
		PORT_DIPSETTING(    0x02, DEF_STR( "Yes") );
		PORT_DIPNAME( 0x0c, 0x00, "2 Attackers At Wave" );
		PORT_DIPSETTING(    0x00, "2" );
		PORT_DIPSETTING(    0x04, "3" );
		PORT_DIPSETTING(    0x08, "4" );
		PORT_DIPSETTING(    0x0c, "5" );
		PORT_DIPNAME( 0x10, 0x00, "Astronauts Kidnapped" );
		PORT_DIPSETTING(    0x10, "Less Often" );
		PORT_DIPSETTING(    0x00, "More Often" );
		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "Demo Voices" );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_2WAY );
		PORT_BIT( 0x0c, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_COIN2 );
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_COIN1 );
	
		PORT_START();       /* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_2WAY | IPF_PLAYER2 );
		PORT_BIT( 0x0c, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_START2 );
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_START1 );
	INPUT_PORTS_END(); }}; 
	
	
	static AY8910interface ay8910_interface = new AY8910interface
	(
		1,	/* 1 chip */
		10000000/8,     /* 10MHz / 8 = 1.25MHz */
		new int[] { 50 },
		new ReadHandlerPtr[] { null },
		new ReadHandlerPtr[] { null },
		new WriteHandlerPtr[] { stratvox_sn76477_w },  /* SN76477 commands (not used in Route 16?) */
		new WriteHandlerPtr[] { null }
	);
	
	
	static SN76477interface sn76477_interface = new SN76477interface
	(
		1,	/* 1 chip */
		new int[]{ 50 },  /* mixing level   pin description		 */
		new double[]{ RES_K( 47)   },		/*	4  noise_res		 */
		new double[]{ RES_K(150)   },		/*	5  filter_res		 */
		new double[]{ CAP_U(0.001) },		/*	6  filter_cap		 */
		new double[]{ RES_M(3.3)   },		/*	7  decay_res		 */
		new double[]{ CAP_U(1.0)   },		/*	8  attack_decay_cap  */
		new double[]{ RES_K(4.7)   },		/* 10  attack_res		 */
		new double[]{ RES_K(200)   },		/* 11  amplitude_res	 */
		new double[]{ RES_K( 55)   },		/* 12  feedback_res 	 */
		new double[]{ 5.0*2/(2+10) },		/* 16  vco_voltage		 */
		new double[]{ CAP_U(0.022) },		/* 17  vco_cap			 */
		new double[]{ RES_K(100)   },		/* 18  vco_res			 */
		new double[]{ 5.0		   },		/* 19  pitch_voltage	 */
		new double[]{ RES_K( 75)   },		/* 20  slf_res			 */
		new double[]{ CAP_U(1.0)   },		/* 21  slf_cap			 */
		new double[]{ CAP_U(2.2)   },		/* 23  oneshot_cap		 */
		new double[]{ RES_K(4.7)   }		/* 24  oneshot_res		 */
	);
	
	
	static DACinterface dac_interface = new DACinterface
	(
		1,
		new int[] { 50 }
	);
	
	
        static MachineDriver machine_driver_route16 = new MachineDriver
	(															
		/* basic machine hardware */							
		new MachineCPU[] {														
			new MachineCPU(													
				CPU_Z80 | CPU_16BIT_PORT,						
				2500000,	/* 10MHz / 4 = 2.5MHz */			
				cpu1_readmem,cpu1_writemem,null,cpu1_writeport,	
				interrupt,1										
			),													
			new MachineCPU(													
				CPU_Z80,										
				2500000,	/* 10MHz / 4 = 2.5MHz */			
				cpu2_readmem,cpu2_writemem,null,null,					
				ignore_interrupt,0								
			)													
		},														
		57, DEFAULT_REAL_60HZ_VBLANK_DURATION,       /* frames per second, vblank duration */ 
		1,														
		null,														
																
		/* video hardware */									
		256, 256, new rectangle( 0, 256-1, 0, 256-1 ),						
		null,														
		8, 0,													
		route16_vh_convert_color_prom,							
																
		VIDEO_TYPE_RASTER | VIDEO_SUPPORTS_DIRTY , 
		null,														
		route16_vh_start,										
		route16_vh_stop,										
		route16_vh_screenrefresh,								
																
		/* sound hardware */									
		0,0,0,0,												
		new MachineSound[] {
                    new MachineSound(
			SOUND_AY8910,		 
			ay8910_interface									
                    )
		}														
	);
        
	
        static MachineDriver machine_driver_stratvox = new MachineDriver
	(															
		/* basic machine hardware */							
		new MachineCPU[] {														
			new MachineCPU(													
				CPU_Z80 | CPU_16BIT_PORT,						
				2500000,	/* 10MHz / 4 = 2.5MHz */			
				cpu1_readmem,cpu1_writemem,null,cpu1_writeport,	
				interrupt,1										
			),													
			new MachineCPU(													
				CPU_Z80,										
				2500000,	/* 10MHz / 4 = 2.5MHz */			
				cpu2_readmem,cpu2_writemem,null,null,					
				ignore_interrupt,0								
			)													
		},														
		57, DEFAULT_REAL_60HZ_VBLANK_DURATION,       /* frames per second, vblank duration */ 
		1,														
		null,														
																
		/* video hardware */									
		256, 256, new rectangle( 0, 256-1, 0, 256-1 ),						
		null,														
		8, 0,													
		route16_vh_convert_color_prom,							
																
		VIDEO_TYPE_RASTER | VIDEO_SUPPORTS_DIRTY , 
		null,														
		route16_vh_start,										
		route16_vh_stop,										
		route16_vh_screenrefresh,								
																
		/* sound hardware */									
		0,0,0,0,												
		new MachineSound[] {														
			new MachineSound(						 
				SOUND_AY8910,		 
				ay8910_interface	 
                        ),						 
			new MachineSound(						 
				SOUND_SN76477,		 
				sn76477_interface	 
                        ),                       
                        new MachineSound(                        
				SOUND_DAC,			 
				dac_interface		 
                        )									
		}														
	);
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_route16 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 ); // 64k for the first CPU
		ROM_LOAD( "route16.a0",   0x0000, 0x0800, 0x8f9101bd );
		ROM_LOAD( "route16.a1",   0x0800, 0x0800, 0x389bc077 );
		ROM_LOAD( "route16.a2",   0x1000, 0x0800, 0x1065a468 );
		ROM_LOAD( "route16.a3",   0x1800, 0x0800, 0x0b1987f3 );
		ROM_LOAD( "route16.a4",   0x2000, 0x0800, 0xf67d853a );
		ROM_LOAD( "route16.a5",   0x2800, 0x0800, 0xd85cf758 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 ); // 64k for the second CPU
		ROM_LOAD( "route16.b0",   0x0000, 0x0800, 0x0f9588a7 );
		ROM_LOAD( "route16.b1",   0x0800, 0x0800, 0x2b326cf9 );
		ROM_LOAD( "route16.b2",   0x1000, 0x0800, 0x529cad13 );
		ROM_LOAD( "route16.b3",   0x1800, 0x0800, 0x3bd8b899 );
	
		ROM_REGION( 0x0200, REGION_PROMS, 0 );
		/* The upper 128 bytes are 0's, used by the hardware to blank the display */
		ROM_LOAD( "pr09",         0x0000, 0x0100, 0x08793ef7 );/* top bitmap */
		ROM_LOAD( "pr10",         0x0100, 0x0100, 0x08793ef7 );/* bottom bitmap */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_route16b = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 ); // 64k for the first CPU
		ROM_LOAD( "rt16.0",       0x0000, 0x0800, 0xb1f0f636 );
		ROM_LOAD( "rt16.1",       0x0800, 0x0800, 0x3ec52fe5 );
		ROM_LOAD( "rt16.2",       0x1000, 0x0800, 0xa8e92871 );
		ROM_LOAD( "rt16.3",       0x1800, 0x0800, 0xa0fc9fc5 );
		ROM_LOAD( "rt16.4",       0x2000, 0x0800, 0x6dcaf8c4 );
		ROM_LOAD( "rt16.5",       0x2800, 0x0800, 0x63d7b05b );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 ); // 64k for the second CPU
		ROM_LOAD( "rt16.6",       0x0000, 0x0800, 0xfef605f3 );
		ROM_LOAD( "rt16.7",       0x0800, 0x0800, 0xd0d6c189 );
		ROM_LOAD( "rt16.8",       0x1000, 0x0800, 0xdefc5797 );
		ROM_LOAD( "rt16.9",       0x1800, 0x0800, 0x88d94a66 );
	
		ROM_REGION( 0x0200, REGION_PROMS, 0 );
		/* The upper 128 bytes are 0's, used by the hardware to blank the display */
		ROM_LOAD( "pr09",         0x0000, 0x0100, 0x08793ef7 );/* top bitmap */
		ROM_LOAD( "pr10",         0x0100, 0x0100, 0x08793ef7 );/* bottom bitmap */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_stratvox = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );    /* 64k for code */
		ROM_LOAD( "ls01.bin",     0x0000, 0x0800, 0xbf4d582e );
		ROM_LOAD( "ls02.bin",     0x0800, 0x0800, 0x16739dd4 );
		ROM_LOAD( "ls03.bin",     0x1000, 0x0800, 0x083c28de );
		ROM_LOAD( "ls04.bin",     0x1800, 0x0800, 0xb0927e3b );
		ROM_LOAD( "ls05.bin",     0x2000, 0x0800, 0xccd25c4e );
		ROM_LOAD( "ls06.bin",     0x2800, 0x0800, 0x07a907a7 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );    /* 64k for the second CPU */
		ROM_LOAD( "ls07.bin",     0x0000, 0x0800, 0x4d333985 );
		ROM_LOAD( "ls08.bin",     0x0800, 0x0800, 0x35b753fc );
	
		ROM_REGION( 0x0200, REGION_PROMS, 0 );
		/* The upper 128 bytes are 0's, used by the hardware to blank the display */
		ROM_LOAD( "pr09",         0x0000, 0x0100, 0x08793ef7 );/* top bitmap */
		ROM_LOAD( "pr10",         0x0100, 0x0100, 0x08793ef7 );/* bottom bitmap */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_speakres = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );    /* 64k for code */
		ROM_LOAD( "speakres.1",   0x0000, 0x0800, 0x6026e4ea );
		ROM_LOAD( "speakres.2",   0x0800, 0x0800, 0x93f0d4da );
		ROM_LOAD( "speakres.3",   0x1000, 0x0800, 0xa3874304 );
		ROM_LOAD( "speakres.4",   0x1800, 0x0800, 0xf484be3a );
		ROM_LOAD( "speakres.5",   0x2000, 0x0800, 0x61b12a67 );
		ROM_LOAD( "speakres.6",   0x2800, 0x0800, 0x220e0ab2 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );    /* 64k for the second CPU */
		ROM_LOAD( "speakres.7",   0x0000, 0x0800, 0xd417be13 );
		ROM_LOAD( "speakres.8",   0x0800, 0x0800, 0x52485d60 );
	
		ROM_REGION( 0x0200, REGION_PROMS, 0 );
		/* The upper 128 bytes are 0's, used by the hardware to blank the display */
		ROM_LOAD( "pr09",         0x0000, 0x0100, 0x08793ef7 );/* top bitmap */
		ROM_LOAD( "pr10",         0x0100, 0x0100, 0x08793ef7 );/* bottom bitmap */
	ROM_END(); }}; 
	
	
	
	public static GameDriver driver_route16	   = new GameDriver("1981"	,"route16"	,"route16.java"	,rom_route16,null	,machine_driver_route16	,input_ports_route16	,init_route16	,ROT270	,	"Tehkan/Sun (Centuri license)", "Route 16" );
	public static GameDriver driver_route16b	   = new GameDriver("1981"	,"route16b"	,"route16.java"	,rom_route16b,driver_route16	,machine_driver_route16	,input_ports_route16	,init_route16b	,ROT270	,	"bootleg", "Route 16 (bootleg)" );
	public static GameDriver driver_speakres	   = new GameDriver("1980"	,"speakres"	,"route16.java"	,rom_speakres,null	,machine_driver_stratvox	,input_ports_stratvox	,init_stratvox	,ROT270	,	"Sun Electronics", "Speak & Rescue" );
	public static GameDriver driver_stratvox	   = new GameDriver("1980"	,"stratvox"	,"route16.java"	,rom_stratvox,driver_speakres	,machine_driver_stratvox	,input_ports_stratvox	,init_stratvox	,ROT270	,	"[Sun Electronics] (Taito license)", "Stratovox" );
}
