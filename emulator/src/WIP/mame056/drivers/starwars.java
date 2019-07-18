/***************************************************************************

	Atari Star Wars hardware

	driver by Steve Baines (sulaco@ntlworld.com) and Frank Palazzolo

	This file is Copyright 1997, Steve Baines.
	Modified by Frank Palazzolo for sound support

	Games supported:
		* Star Wars
		* The Empire Strikes Back

	Known bugs:
		* none at this time

****************************************************************************

	Memory map (TBA)

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

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex056.fileio.*;
import static mame056.cpu.m6809.m6809H.M6809_IRQ_LINE;

import static mame056.machine.slapstic.*;
import static WIP.mame056.machine.starwars.*;
import static WIP.mame056.sndhrdw.starwars.*;
import static WIP.mame056.sound._5220intfH.*;
import static WIP.mame056.vidhrdw.vector.*;
import static WIP.mame056.vidhrdw.avgdvg.*;
import static mame056.sound.pokey.*;
import static mame056.sound.pokeyH.*;

public class starwars
{
	////
	
	/* Local variables */
	static UBytePtr nvram=new UBytePtr();
	static int[] nvram_size=new int[1];
	static UBytePtr slapstic_source;
	static UBytePtr slapstic_base;
	static int current_bank;
	static int is_esb;
	
	
	
	/*************************************
	 *
	 *	NVRAM handler
	 *
	 *************************************/
	
	public static nvramPtr nvram_handler = new nvramPtr() {
            public void handler(Object file, int read_or_write) {
                if (read_or_write != 0)
			osd_fwrite(file, nvram, nvram_size[0]);
		else if (file != null)
			osd_fread(file, nvram, nvram_size[0]);
		else
			memset(nvram, 0, nvram_size[0]);
            }
        };

        
	/*************************************
	 *
	 *	Machine init
	 *
	 *************************************/
	
	public static InitMachinePtr init_machine = new InitMachinePtr() { public void handler()
	{
		/* ESB-specific */
		if (is_esb != 0)
		{
			/* reset the slapstic */
			slapstic_reset();
			current_bank = slapstic_bank();
			memcpy(slapstic_base, new UBytePtr(slapstic_source, current_bank * 0x2000), 0x2000);
	
			/* reset all the banks */
			starwars_out_w.handler(4, 0);
		}
	
		/* reset the mathbox */
		swmathbox_reset();
	} };
	
	
	
	/*************************************
	 *
	 *	Interrupt generation
	 *
	 *************************************/
	
	public static InterruptPtr generate_irq = new InterruptPtr() { public int handler() 
	{
		cpu_set_irq_line(0, M6809_IRQ_LINE, ASSERT_LINE);
		return ignore_interrupt.handler();
	} };
	
	
	public static WriteHandlerPtr irq_ack_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_set_irq_line(0, M6809_IRQ_LINE, CLEAR_LINE);
	} };
	
	
	
	/*************************************
	 *
	 *	ESB Slapstic handler
	 *
	 *************************************/
	
	public static ReadHandlerPtr esb_slapstic_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int result = slapstic_base.read(offset);
		int new_bank = slapstic_tweak(offset);
	
		/* update for the new bank */
		if (new_bank != current_bank)
		{
			current_bank = new_bank;
			memcpy(slapstic_base, new UBytePtr(slapstic_source, current_bank * 0x2000), 0x2000);
		}
		return result;
	} };
	
	
	public static WriteHandlerPtr esb_slapstic_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int new_bank = slapstic_tweak(offset);
	
		/* update for the new bank */
		if (new_bank != current_bank)
		{
			current_bank = new_bank;
			memcpy(slapstic_base, new UBytePtr(slapstic_source, current_bank * 0x2000), 0x2000);
		}
	} };
	
	
	
	/*************************************
	 *
	 *	ESB Opcode base handler
	 *
	 *************************************/
	
	public static opbase_handlerPtr esb_setopbase = new opbase_handlerPtr() {
            public int handler(int address) {
                int prevpc = cpu_getpreviouspc();
	
		/*
		 *	This is a slightly ugly kludge for Empire Strikes Back because it jumps
		 *	directly to code in the slapstic.
		 */
	
		/* if we're jumping into the slapstic region, tweak the new PC */
		if ((address & 0xe000) == 0x8000)
		{
			esb_slapstic_r.handler(address & 0x1fff);
	
			/* make sure we catch the next branch as well */
			catch_nextBranch();
			return -1;
		}
	
		/* if we're jumping out of the slapstic region, tweak the previous PC */
		else if ((prevpc & 0xe000) == 0x8000)
		{
			if (prevpc != 0x8080 && prevpc != 0x8090 && prevpc != 0x80a0 && prevpc != 0x80b0)
				esb_slapstic_r.handler(prevpc & 0x1fff);
		}
	
		return address;
            }
        };
	
	
	/*************************************
	 *
	 *	Main CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress main_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x2fff, MRA_RAM ),			/* vector_ram */
		new Memory_ReadAddress( 0x3000, 0x3fff, MRA_ROM ),			/* vector_rom */
		new Memory_ReadAddress( 0x4300, 0x431f, input_port_0_r ),		/* Memory mapped input port 0 */
		new Memory_ReadAddress( 0x4320, 0x433f, starwars_input_1_r ),	/* Memory mapped input port 1 */
		new Memory_ReadAddress( 0x4340, 0x435f, input_port_2_r ),		/* DIP switches bank 0 */
		new Memory_ReadAddress( 0x4360, 0x437f, input_port_3_r ),		/* DIP switches bank 1 */
		new Memory_ReadAddress( 0x4380, 0x439f, starwars_adc_r ),		/* a-d control result */
		new Memory_ReadAddress( 0x4400, 0x4400, starwars_main_read_r ),
		new Memory_ReadAddress( 0x4401, 0x4401, starwars_main_ready_flag_r ),
		new Memory_ReadAddress( 0x4500, 0x45ff, MRA_RAM ),			/* nov_ram */
		new Memory_ReadAddress( 0x4700, 0x4700, swmathbx_reh_r ),
		new Memory_ReadAddress( 0x4701, 0x4701, swmathbx_rel_r ),
		new Memory_ReadAddress( 0x4703, 0x4703, swmathbx_prng_r ),	/* pseudo random number generator */
		new Memory_ReadAddress( 0x4800, 0x5fff, MRA_RAM ),			/* CPU and Math RAM */
		new Memory_ReadAddress( 0x6000, 0x7fff, MRA_BANK1 ),			/* banked ROM */
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),			/* rest of main_rom */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static Memory_WriteAddress main_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x2fff, MWA_RAM, vectorram, vectorram_size ),
		new Memory_WriteAddress( 0x3000, 0x3fff, MWA_ROM ),								/* vector_rom */
		new Memory_WriteAddress( 0x4400, 0x4400, starwars_main_wr_w ),
		new Memory_WriteAddress( 0x4500, 0x45ff, MWA_RAM, nvram, nvram_size ),
		new Memory_WriteAddress( 0x4600, 0x461f, avgdvg_go_w ),
		new Memory_WriteAddress( 0x4620, 0x463f, avgdvg_reset_w ),
		new Memory_WriteAddress( 0x4640, 0x465f, watchdog_reset_w ),
		new Memory_WriteAddress( 0x4660, 0x467f, irq_ack_w ),
		new Memory_WriteAddress( 0x4680, 0x4687, starwars_out_w ),
		new Memory_WriteAddress( 0x46a0, 0x46bf, MWA_NOP ),								/* nstore */
		new Memory_WriteAddress( 0x46c0, 0x46c2, starwars_adc_select_w ),
		new Memory_WriteAddress( 0x46e0, 0x46e0, starwars_soundrst_w ),
		new Memory_WriteAddress( 0x4700, 0x4707, swmathbx_w ),
		new Memory_WriteAddress( 0x4800, 0x5fff, MWA_RAM ),		/* CPU and Math RAM */
		new Memory_WriteAddress( 0x6000, 0xffff, MWA_ROM ),		/* main_rom */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	/*************************************
	 *
	 *	Sound CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0800, 0x0fff, starwars_sin_r ),		/* SIN Read */
		new Memory_ReadAddress( 0x1000, 0x107f, MRA_RAM ),	/* 6532 RAM */
		new Memory_ReadAddress( 0x1080, 0x109f, starwars_m6532_r ),
		new Memory_ReadAddress( 0x2000, 0x27ff, MRA_RAM ),	/* program RAM */
		new Memory_ReadAddress( 0x4000, 0xbfff, MRA_ROM ),	/* sound roms */
		new Memory_ReadAddress( 0xc000, 0xffff, MRA_ROM ),	/* load last rom twice */
										/* for proper int vec operation */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static Memory_WriteAddress sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x07ff, starwars_sout_w ),
		new Memory_WriteAddress( 0x1000, 0x107f, MWA_RAM ), /* 6532 ram */
		new Memory_WriteAddress( 0x1080, 0x109f, starwars_m6532_w ),
		new Memory_WriteAddress( 0x1800, 0x183f, quad_pokey_w ),
		new Memory_WriteAddress( 0x2000, 0x27ff, MWA_RAM ), /* program RAM */
		new Memory_WriteAddress( 0x4000, 0xbfff, MWA_ROM ), /* sound rom */
		new Memory_WriteAddress( 0xc000, 0xffff, MWA_ROM ), /* sound rom again, for intvecs */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	/*************************************
	 *
	 *	Port definitions
	 *
	 *************************************/
	
	static InputPortPtr input_ports_starwars = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT ( 0x01, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT ( 0x02, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT ( 0x04, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT ( 0x08, IP_ACTIVE_LOW, IPT_TILT );
		PORT_SERVICE( 0x10, IP_ACTIVE_LOW );
		PORT_BIT ( 0x20, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_BIT ( 0x40, IP_ACTIVE_LOW, IPT_BUTTON4 );
		PORT_BIT ( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT ( 0x01, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_BIT ( 0x02, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_BITX( 0x04, IP_ACTIVE_LOW, IPT_SERVICE, "Diagnostic Step", KEYCODE_F1, IP_JOY_NONE );
		PORT_BIT ( 0x08, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_BIT ( 0x10, IP_ACTIVE_LOW, IPT_BUTTON3 );
		PORT_BIT ( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		/* Bit 6 is MATH_RUN - see machine/starwars.c */
		PORT_BIT ( 0x40, IP_ACTIVE_HIGH, IPT_UNUSED );
		/* Bit 7 is VG_HALT - see machine/starwars.c */
		PORT_BIT ( 0x80, IP_ACTIVE_HIGH, IPT_UNUSED );
	
		PORT_START(); 	/* DSW0 */
		PORT_DIPNAME(0x03, 0x00, "Starting Shields" );
		PORT_DIPSETTING (  0x00, "6" );
		PORT_DIPSETTING (  0x01, "7" );
		PORT_DIPSETTING (  0x02, "8" );
		PORT_DIPSETTING (  0x03, "9" );
		PORT_DIPNAME(0x0c, 0x00, DEF_STR( "Difficulty") );
		PORT_DIPSETTING (  0x00, "Easy" );
		PORT_DIPSETTING (  0x04, "Moderate" );
		PORT_DIPSETTING (  0x08, "Hard" );
		PORT_DIPSETTING (  0x0c, "Hardest" );
		PORT_DIPNAME(0x30, 0x00, "Bonus Shields" );
		PORT_DIPSETTING (  0x00, "0" );
		PORT_DIPSETTING (  0x10, "1" );
		PORT_DIPSETTING (  0x20, "2" );
		PORT_DIPSETTING (  0x30, "3" );
		PORT_DIPNAME(0x40, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING (  0x40, DEF_STR( "Off") );
		PORT_DIPSETTING (  0x00, DEF_STR( "On") );
		PORT_DIPNAME(0x80, 0x80, "Freeze" );
		PORT_DIPSETTING (  0x80, DEF_STR( "Off") );
		PORT_DIPSETTING (  0x00, DEF_STR( "On") );
	
		PORT_START(); 	/* DSW1 */
		PORT_DIPNAME(0x03, 0x02, DEF_STR( "Coinage") );
		PORT_DIPSETTING (  0x03, DEF_STR( "2C_1C") );
		PORT_DIPSETTING (  0x02, DEF_STR( "1C_1C") );
		PORT_DIPSETTING (  0x01, DEF_STR( "1C_2C") );
		PORT_DIPSETTING (  0x00, DEF_STR( "Free_Play") );
		PORT_DIPNAME(0x0c, 0x00, DEF_STR( "Coin_B") );
		PORT_DIPSETTING (  0x00, "*1" );
		PORT_DIPSETTING (  0x04, "*4" );
		PORT_DIPSETTING (  0x08, "*5" );
		PORT_DIPSETTING (  0x0c, "*6" );
		PORT_DIPNAME(0x10, 0x00, DEF_STR( "Coin_A") );
		PORT_DIPSETTING (  0x00, "*1" );
		PORT_DIPSETTING (  0x10, "*2" );
		PORT_DIPNAME(0xe0, 0x00, "Bonus Coinage" );
		PORT_DIPSETTING (  0x20, "2 gives 1" );
		PORT_DIPSETTING (  0x60, "4 gives 2" );
		PORT_DIPSETTING (  0xa0, "3 gives 1" );
		PORT_DIPSETTING (  0x40, "4 gives 1" );
		PORT_DIPSETTING (  0x80, "5 gives 1" );
		PORT_DIPSETTING (  0x00, "None" );
	/* 0xc0 and 0xe0 None */
	
		PORT_START(); 	/* IN4 */
		PORT_ANALOG( 0xff, 0x80, IPT_AD_STICK_Y, 70, 30, 0, 255 );
	
		PORT_START(); 	/* IN5 */
		PORT_ANALOG( 0xff, 0x80, IPT_AD_STICK_X, 50, 30, 0, 255 );
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_esb = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 */
		PORT_BIT ( 0x01, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT ( 0x02, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT ( 0x04, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT ( 0x08, IP_ACTIVE_LOW, IPT_TILT );
		PORT_SERVICE( 0x10, IP_ACTIVE_LOW );
		PORT_BIT ( 0x20, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_BIT ( 0x40, IP_ACTIVE_LOW, IPT_BUTTON4 );
		PORT_BIT ( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 );
	
		PORT_START(); 	/* IN1 */
		PORT_BIT ( 0x01, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_BIT ( 0x02, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_BITX( 0x04, IP_ACTIVE_LOW, IPT_SERVICE, "Diagnostic Step", KEYCODE_F1, IP_JOY_NONE );
		PORT_BIT ( 0x08, IP_ACTIVE_HIGH, IPT_UNUSED );
		PORT_BIT ( 0x10, IP_ACTIVE_LOW, IPT_BUTTON3 );
		PORT_BIT ( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		/* Bit 6 is MATH_RUN - see machine/starwars.c */
		PORT_BIT ( 0x40, IP_ACTIVE_HIGH, IPT_UNUSED );
		/* Bit 7 is VG_HALT - see machine/starwars.c */
		PORT_BIT ( 0x80, IP_ACTIVE_HIGH, IPT_UNUSED );
	
		PORT_START(); 	/* DSW0 */
		PORT_DIPNAME(0x03, 0x03, "Starting Shields" );
		PORT_DIPSETTING (  0x01, "2" );
		PORT_DIPSETTING (  0x00, "3" );
		PORT_DIPSETTING (  0x03, "4" );
		PORT_DIPSETTING (  0x02, "5" );
		PORT_DIPNAME(0x0c, 0x0c, DEF_STR( "Difficulty") );
		PORT_DIPSETTING (  0x08, "Easy" );
		PORT_DIPSETTING (  0x0c, "Moderate" );
		PORT_DIPSETTING (  0x00, "Hard" );
		PORT_DIPSETTING (  0x04, "Hardest" );
		PORT_DIPNAME(0x30, 0x30, "Jedi-Letter Mode" );
		PORT_DIPSETTING (  0x00, "Level Only" );
		PORT_DIPSETTING (  0x10, "Level" );
		PORT_DIPSETTING (  0x20, "Increment Only" );
		PORT_DIPSETTING (  0x30, "Increment" );
		PORT_DIPNAME(0x40, 0x40, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING (  0x00, DEF_STR( "Off") );
		PORT_DIPSETTING (  0x40, DEF_STR( "On") );
		PORT_DIPNAME(0x80, 0x80, "Freeze" );
		PORT_DIPSETTING (  0x80, DEF_STR( "Off") );
		PORT_DIPSETTING (  0x00, DEF_STR( "On") );
	
		PORT_START(); 	/* DSW1 */
		PORT_DIPNAME(0x03, 0x02, DEF_STR( "Coinage") );
		PORT_DIPSETTING (  0x03, DEF_STR( "2C_1C") );
		PORT_DIPSETTING (  0x02, DEF_STR( "1C_1C") );
		PORT_DIPSETTING (  0x01, DEF_STR( "1C_2C") );
		PORT_DIPSETTING (  0x00, DEF_STR( "Free_Play") );
		PORT_DIPNAME(0x0c, 0x00, DEF_STR( "Coin_B") );
		PORT_DIPSETTING (  0x00, "*1" );
		PORT_DIPSETTING (  0x04, "*4" );
		PORT_DIPSETTING (  0x08, "*5" );
		PORT_DIPSETTING (  0x0c, "*6" );
		PORT_DIPNAME(0x10, 0x00, DEF_STR( "Coin_A") );
		PORT_DIPSETTING (  0x00, "*1" );
		PORT_DIPSETTING (  0x10, "*2" );
		PORT_DIPNAME(0xe0, 0xe0, "Bonus Coinage" );
		PORT_DIPSETTING (  0x20, "2 gives 1" );
		PORT_DIPSETTING (  0x60, "4 gives 2" );
		PORT_DIPSETTING (  0xa0, "3 gives 1" );
		PORT_DIPSETTING (  0x40, "4 gives 1" );
		PORT_DIPSETTING (  0x80, "5 gives 1" );
		PORT_DIPSETTING (  0xe0, "None" );
	/* 0xc0 and 0x00 None */
	
		PORT_START(); 	/* IN4 */
		PORT_ANALOG( 0xff, 0x80, IPT_AD_STICK_Y, 70, 30, 0, 255 );
	
		PORT_START(); 	/* IN5 */
		PORT_ANALOG( 0xff, 0x80, IPT_AD_STICK_X, 50, 30, 0, 255 );
	INPUT_PORTS_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	Sound definitions
	 *
	 *************************************/
	
	static POKEYinterface pokey_interface = new POKEYinterface
	(
		4,			/* 4 chips */
		1500000,	/* 1.5 MHz? */
		new int[]{ 20, 20, 20, 20 },	/* volume */
		/* The 8 pot handlers */
		new ReadHandlerPtr[]{ null, null, null, null },
		new ReadHandlerPtr[]{ null, null, null, null },
		new ReadHandlerPtr[]{ null, null, null, null },
		new ReadHandlerPtr[]{ null, null, null, null },
		new ReadHandlerPtr[]{ null, null, null, null },
		new ReadHandlerPtr[]{ null, null, null, null },
		new ReadHandlerPtr[]{ null, null, null, null },
		new ReadHandlerPtr[]{ null, null, null, null },
		/* The allpot handler */
		new ReadHandlerPtr[]{ null, null, null, null }
	);
	
	
	static TMS5220interface tms5220_interface = new TMS5220interface
        (
		640000,     /* clock speed (80*samplerate) */
		50,         /* volume */
		null           /* IRQ handler */
	);
	
	
	
	/*************************************
	 *
	 *	Machine driver
	 *
	 *************************************/
	
	static MachineDriver machine_driver_starwars = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6809,
				1500000,
				main_readmem,main_writemem,null,null,
				generate_irq,6 /* 183Hz ? */
			),
			new MachineCPU(
				CPU_M6809 | CPU_AUDIO_CPU,
				1500000,
				sound_readmem,sound_writemem,null,null,
				ignore_interrupt,0
			)
		},
		30, 0,
		1,
		init_machine,
	
		/* video hardware */
		400, 300, new rectangle( 0, 250, 0, 280 ),
		null,
		256, 0,
		avg_init_palette_swars,
	
		VIDEO_TYPE_VECTOR | VIDEO_SUPPORTS_DIRTY | VIDEO_RGB_DIRECT,
		null,
		avg_start_starwars,
		avg_stop,
		vector_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound( SOUND_POKEY, pokey_interface ),
			new MachineSound( SOUND_TMS5220, tms5220_interface )
		},
                
	
		nvram_handler
	);
	
	
	
	/*************************************
	 *
	 *	ROM definitions
	 *
	 *************************************/
	
	static RomLoadPtr rom_starwar1 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x12000, REGION_CPU1, 0 );    /* 2 64k ROM spaces */
		ROM_LOAD( "136021.105",   0x3000, 0x1000, 0x538e7d2f );/* 3000-3fff is 4k vector rom */
		ROM_LOAD( "136021.114",   0x6000, 0x2000, 0xe75ff867 );  /* ROM 0 bank pages 0 and 1 */
		ROM_CONTINUE(            0x10000, 0x2000 );
		ROM_LOAD( "136021.102",   0x8000, 0x2000, 0xf725e344 );/*  8k ROM 1 bank */
		ROM_LOAD( "136021.203",   0xa000, 0x2000, 0xf6da0a00 );/*  8k ROM 2 bank */
		ROM_LOAD( "136021.104",   0xc000, 0x2000, 0x7e406703 );/*  8k ROM 3 bank */
		ROM_LOAD( "136021.206",   0xe000, 0x2000, 0xc7e51237 );/*  8k ROM 4 bank */
	
		/* Sound ROMS */
		ROM_REGION( 0x10000, REGION_CPU2, 0 );    /* Really only 32k, but it looks like 64K */
		ROM_LOAD( "136021.107",   0x4000, 0x2000, 0xdbf3aea2 );/* Sound ROM 0 */
		ROM_RELOAD(               0xc000, 0x2000 );/* Copied again for */
		ROM_LOAD( "136021.208",   0x6000, 0x2000, 0xe38070a8 );/* Sound ROM 0 */
		ROM_RELOAD(               0xe000, 0x2000 );/* proper int vecs */
	
		/* Mathbox PROMs */
		ROM_REGION( 0x1000, REGION_PROMS, ROMREGION_DISPOSE );
		ROM_LOAD( "136021.110",   0x0000, 0x0400, 0x01061762 );/* PROM 0 */
		ROM_LOAD( "136021.111",   0x0400, 0x0400, 0x2e619b70 );/* PROM 1 */
		ROM_LOAD( "136021.112",   0x0800, 0x0400, 0x6cfa3544 );/* PROM 2 */
		ROM_LOAD( "136021.113",   0x0c00, 0x0400, 0x03f6acb2 );/* PROM 3 */
	ROM_END(); }}; 
	
	
	static RomLoadPtr rom_starwars = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x12000, REGION_CPU1, 0 );    /* 2 64k ROM spaces */
		ROM_LOAD( "136021.105",   0x3000, 0x1000, 0x538e7d2f );/* 3000-3fff is 4k vector rom */
		ROM_LOAD( "136021.214",   0x6000, 0x2000, 0x04f1876e );  /* ROM 0 bank pages 0 and 1 */
		ROM_CONTINUE(            0x10000, 0x2000 );
		ROM_LOAD( "136021.102",   0x8000, 0x2000, 0xf725e344 );/*  8k ROM 1 bank */
		ROM_LOAD( "136021.203",   0xa000, 0x2000, 0xf6da0a00 );/*  8k ROM 2 bank */
		ROM_LOAD( "136021.104",   0xc000, 0x2000, 0x7e406703 );/*  8k ROM 3 bank */
		ROM_LOAD( "136021.206",   0xe000, 0x2000, 0xc7e51237 );/*  8k ROM 4 bank */
	
		/* Sound ROMS */
		ROM_REGION( 0x10000, REGION_CPU2, 0 );    /* Really only 32k, but it looks like 64K */
		ROM_LOAD( "136021.107",   0x4000, 0x2000, 0xdbf3aea2 );/* Sound ROM 0 */
		ROM_RELOAD(               0xc000, 0x2000 );/* Copied again for */
		ROM_LOAD( "136021.208",   0x6000, 0x2000, 0xe38070a8 );/* Sound ROM 0 */
		ROM_RELOAD(               0xe000, 0x2000 );/* proper int vecs */
	
		/* Mathbox PROMs */
		ROM_REGION( 0x1000, REGION_PROMS, ROMREGION_DISPOSE );
		ROM_LOAD( "136021.110",   0x0000, 0x0400, 0x01061762 );/* PROM 0 */
		ROM_LOAD( "136021.111",   0x0400, 0x0400, 0x2e619b70 );/* PROM 1 */
		ROM_LOAD( "136021.112",   0x0800, 0x0400, 0x6cfa3544 );/* PROM 2 */
		ROM_LOAD( "136021.113",   0x0c00, 0x0400, 0x03f6acb2 );/* PROM 3 */
	ROM_END(); }}; 
	
	
	static RomLoadPtr rom_esb = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x22000, REGION_CPU1, 0 );    /* 64k for code and a buttload for the banked ROMs */
		ROM_LOAD( "136031.111",   0x03000, 0x1000, 0xb1f9bd12 );   /* 3000-3fff is 4k vector rom */
		ROM_LOAD( "136031.101",   0x06000, 0x2000, 0xef1e3ae5 );
		ROM_CONTINUE(             0x10000, 0x2000 );
		/* $8000 - $9fff : slapstic page */
		ROM_LOAD( "136031.102",   0x0a000, 0x2000, 0x62ce5c12 );
		ROM_CONTINUE(             0x1c000, 0x2000 );
		ROM_LOAD( "136031.203",   0x0c000, 0x2000, 0x27b0889b );
		ROM_CONTINUE(             0x1e000, 0x2000 );
		ROM_LOAD( "136031.104",   0x0e000, 0x2000, 0xfd5c725e );
		ROM_CONTINUE(             0x20000, 0x2000 );
	
		ROM_LOAD( "136031.105",   0x14000, 0x4000, 0xea9e4dce );/* slapstic 0, 1 */
		ROM_LOAD( "136031.106",   0x18000, 0x4000, 0x76d07f59 );/* slapstic 2, 3 */
	
		/* Sound ROMS */
		ROM_REGION( 0x10000, REGION_CPU2, 0 );
		ROM_LOAD( "136031.113",   0x4000, 0x2000, 0x24ae3815 );/* Sound ROM 0 */
		ROM_CONTINUE(             0xc000, 0x2000 );/* Copied again for */
		ROM_LOAD( "136031.112",   0x6000, 0x2000, 0xca72d341 );/* Sound ROM 1 */
		ROM_CONTINUE(             0xe000, 0x2000 );/* proper int vecs */
	
		/* Mathbox PROMs */
		ROM_REGION( 0x1000, REGION_PROMS, ROMREGION_DISPOSE );
		ROM_LOAD( "136031.110",   0x0000, 0x0400, 0xb8d0f69d );/* PROM 0 */
		ROM_LOAD( "136031.109",   0x0400, 0x0400, 0x6a2a4d98 );/* PROM 1 */
		ROM_LOAD( "136031.108",   0x0800, 0x0400, 0x6a76138f );/* PROM 2 */
		ROM_LOAD( "136031.107",   0x0c00, 0x0400, 0xafbf6e01 );/* PROM 3 */
	ROM_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	Driver init
	 *
	 *************************************/
	
	public static InitDriverPtr init_starwars = new InitDriverPtr() { public void handler()
	{
		/* prepare the mathbox */
		is_esb = 0;
		swmathbox_init();
	} };
	
	
	public static InitDriverPtr init_esb = new InitDriverPtr() { public void handler()
	{
		/* init the slapstic */
		slapstic_init(101);
		slapstic_source = new UBytePtr(memory_region(REGION_CPU1), 0x14000);
		slapstic_base = new UBytePtr(memory_region(REGION_CPU1), 0x08000);
	
		/* install an opcode base handler */
		memory_set_opbase_handler(0, esb_setopbase);
	
		/* install read/write handlers for it */
		install_mem_read_handler(0, 0x8000, 0x9fff, esb_slapstic_r);
		install_mem_write_handler(0, 0x8000, 0x9fff, esb_slapstic_w);
	
		/* install additional banking */
		install_mem_read_handler(0, 0xa000, 0xffff, MRA_BANK2);
	
		/* prepare the mathbox */
		is_esb = 1;
		swmathbox_init();
	} };
	
	
	
	/*************************************
	 *
	 *	Game drivers
	 *
	 *************************************/
	
	public static GameDriver driver_starwars	   = new GameDriver("1983"	,"starwars"	,"starwars.java"	,rom_starwars,null	,machine_driver_starwars	,input_ports_starwars	,init_starwars	,ROT0	,	"Atari", "Star Wars (rev 2)" );
	public static GameDriver driver_starwar1	   = new GameDriver("1983"	,"starwar1"	,"starwars.java"	,rom_starwar1,driver_starwars	,machine_driver_starwars	,input_ports_starwars	,init_starwars	,ROT0	,	"Atari", "Star Wars (rev 1)" );
	public static GameDriver driver_esb	   = new GameDriver("1985"	,"esb"	,"starwars.java"	,rom_esb,null	,machine_driver_starwars	,input_ports_esb	,init_esb	,ROT0	,	"Atari Games", "The Empire Strikes Back" );
}
