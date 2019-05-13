/***************************************************************************

	sndhrdw/mcr.c

	Functions to emulate general the various MCR sound cards.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import static mame056.inptport.*;
import static mame056.memoryH.*;
import static mame056.sound.ay8910.*;
import static mame056.sound.ay8910H.*;
import static mame056.sound.mixerH.*;
import static mame056.sound.mixer.*;
import static mame056.timerH.*;
import static mame056.timer.*;
import static WIP.mame056.sndhrdw.mcrH.*;
import static mame056.cpu.m6809.m6809H.M6809_IRQ_LINE;
import static mame056.cpuexec.*;
import static mame056.cpuintrfH.*;
import static mame056.machine._6812pia.*;
import static mame056.machine._6812piaH.*;
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpu.m6800.m6800H.M6808_IRQ_LINE;

public class mcr {
	/*************************************
	 *
	 *	Global variables
	 *
	 *************************************/
	
	public static int mcr_sound_config;
	
	
	
	/*************************************
	 *
	 *	Statics
	 *
	 *************************************/
	
	static int dacval;
	
	/* SSIO-specific globals */
	static int ssio_sound_cpu;
	static int[] ssio_data=new int[4];
	static int ssio_status;
	static int[][] ssio_duty_cycle=new int[2][3];
	
	/* Chip Squeak Deluxe-specific globals */
	static int csdeluxe_sound_cpu;
	static int csdeluxe_dac_index;
/*TODO*///	extern struct pia6821_interface csdeluxe_pia_intf;
	
	/* Turbo Chip Squeak-specific globals */
	static int turbocs_sound_cpu;
	static int turbocs_dac_index;
	static int turbocs_status;
/*TODO*///	extern struct pia6821_interface turbocs_pia_intf;
	
	/* Sounds Good-specific globals */
	static int soundsgood_sound_cpu;
	static int soundsgood_dac_index;
	static int soundsgood_status;
/*TODO*///	extern struct pia6821_interface soundsgood_pia_intf;
	
	/* Squawk n' Talk-specific globals */
	static int squawkntalk_sound_cpu;
	static int squawkntalk_tms_command;
	static int squawkntalk_tms_strobes;
/*TODO*///	extern struct pia6821_interface squawkntalk_pia0_intf;
/*TODO*///	extern struct pia6821_interface squawkntalk_pia1_intf;
	
	
	
	/*************************************
	 *
	 *	Generic MCR sound initialization
	 *
	 *************************************/
	
	public static void mcr_sound_init()
	{
		int sound_cpu = 1;
		int dac_index = 0;
	
		/* SSIO */
		if ((mcr_sound_config & MCR_SSIO) != 0)
		{
			ssio_sound_cpu = sound_cpu++;
			ssio_reset_w(1);
			ssio_reset_w(0);
		}
	
		/* Turbo Chip Squeak */
		if ((mcr_sound_config & MCR_TURBO_CHIP_SQUEAK) != 0)
		{
			pia_config(0, PIA_ALTERNATE_ORDERING, turbocs_pia_intf);
			turbocs_dac_index = dac_index++;
			turbocs_sound_cpu = sound_cpu++;
			turbocs_reset_w(1);
			turbocs_reset_w(0);
		}
	
		/* Chip Squeak Deluxe */
		if ((mcr_sound_config & MCR_CHIP_SQUEAK_DELUXE) != 0)
		{
			pia_config(0, PIA_ALTERNATE_ORDERING, csdeluxe_pia_intf);
			csdeluxe_dac_index = dac_index++;
			csdeluxe_sound_cpu = sound_cpu++;
			csdeluxe_reset_w(1);
			csdeluxe_reset_w(0);
		}
	
		/* Sounds Good */
		if ((mcr_sound_config & MCR_SOUNDS_GOOD) != 0)
		{
			/* special case: Spy Hunter 2 has both Turbo CS and Sounds Good, so we use PIA slot 1 */
			pia_config(1, PIA_ALTERNATE_ORDERING, soundsgood_pia_intf);
			soundsgood_dac_index = dac_index++;
			soundsgood_sound_cpu = sound_cpu++;
			soundsgood_reset_w(1);
			soundsgood_reset_w(0);
		}
	
		/* Squawk n Talk */
		if ((mcr_sound_config & MCR_SQUAWK_N_TALK) != 0)
		{
			pia_config(0, PIA_STANDARD_ORDERING, squawkntalk_pia0_intf);
			pia_config(1, PIA_STANDARD_ORDERING, squawkntalk_pia1_intf);
			squawkntalk_sound_cpu = sound_cpu++;
			squawkntalk_reset_w(1);
			squawkntalk_reset_w(0);
		}
	
		/* Advanced Audio */
		if ((mcr_sound_config & MCR_WILLIAMS_SOUND) != 0)
		{
			/*TODO*///williams_cvsd_init(sound_cpu++, 0);
			dac_index++;
			/*TODO*///williams_cvsd_reset_w(1);
			/*TODO*///williams_cvsd_reset_w(0);
		}
	
		/* reset any PIAs */
		pia_reset();
	}
	
	
	
	/*************************************
	 *
	 *	MCR SSIO communications
	 *
	 *	Z80, 2 AY-3812
	 *
	 *************************************/
	
	/********* internal interfaces ***********/
	public static WriteHandlerPtr ssio_status_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		ssio_status = data;
	} };
	
	public static ReadHandlerPtr ssio_data_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return ssio_data[offset];
	} };
	
	static timer_callback ssio_delayed_data_w = new timer_callback() {
            public void handler(int param) {
                ssio_data[param >> 8] = param & 0xff;
            }
        };
	
	static void ssio_update_volumes()
	{
		int chip, chan;
		for (chip = 0; chip < 2; chip++)
			for (chan = 0; chan < 3; chan++)
				AY8910_set_volume(chip, chan, (ssio_duty_cycle[chip][chan] ^ 15) * 100 / 15);
	}
	
	public static WriteHandlerPtr ssio_porta0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		ssio_duty_cycle[0][0] = data & 15;
		ssio_duty_cycle[0][1] = data >> 4;
		ssio_update_volumes();
	} };
	
	public static WriteHandlerPtr ssio_portb0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		ssio_duty_cycle[0][2] = data & 15;
		ssio_update_volumes();
	} };
	
	public static WriteHandlerPtr ssio_porta1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		ssio_duty_cycle[1][0] = data & 15;
		ssio_duty_cycle[1][1] = data >> 4;
		ssio_update_volumes();
	} };
	
	public static WriteHandlerPtr ssio_portb1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		ssio_duty_cycle[1][2] = data & 15;
		mixer_sound_enable_global_w((data & 0x80)!=0?0:1);
		ssio_update_volumes();
	} };
	
	/********* external interfaces ***********/
	public static WriteHandlerPtr ssio_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		timer_set(TIME_NOW, (offset << 8) | (data & 0xff), ssio_delayed_data_w);
	} };
	
	public static ReadHandlerPtr ssio_status_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return ssio_status;
	} };
	
	public static void ssio_reset_w(int state)
	{
		/* going high halts the CPU */
		if (state != 0)
		{
			int i;
	
			cpu_set_reset_line(ssio_sound_cpu, ASSERT_LINE);
	
			/* latches also get reset */
			for (i = 0; i < 4; i++)
				ssio_data[i] = 0;
			ssio_status = 0;
		}
		/* going low resets and reactivates the CPU */
		else
			cpu_set_reset_line(ssio_sound_cpu, CLEAR_LINE);
	}
	
	
	/********* sound interfaces ***********/
	static AY8910interface ssio_ay8910_interface = new AY8910interface
	(
		2,			/* 2 chips */
		2000000,	/* 2 MHz ?? */
		new int[] { MIXER(33,MIXER_PAN_LEFT), MIXER(33,MIXER_PAN_RIGHT) },	/* dotron clips with anything higher */
		new ReadHandlerPtr[] { null, null },
		new ReadHandlerPtr[] { null, null },
		new WriteHandlerPtr[] { ssio_porta0_w, ssio_porta1_w },
		new WriteHandlerPtr[] { ssio_portb0_w, ssio_portb1_w }
	);
	
	
	/********* memory interfaces ***********/
        public static Memory_ReadAddress ssio_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x3fff, MRA_ROM ),
                new Memory_ReadAddress( 0x8000, 0x83ff, MRA_RAM ),
		new Memory_ReadAddress( 0x9000, 0x9003, ssio_data_r ),
		new Memory_ReadAddress( 0xa001, 0xa001, AY8910_read_port_0_r ),
		new Memory_ReadAddress( 0xb001, 0xb001, AY8910_read_port_1_r ),
		new Memory_ReadAddress( 0xe000, 0xe000, MRA_NOP ),
		new Memory_ReadAddress( 0xf000, 0xf000, input_port_5_r ),
                new Memory_ReadAddress(MEMPORT_MARKER, 0)
        };

        public static Memory_WriteAddress ssio_writemem []={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x3fff, MWA_ROM ),
                new Memory_WriteAddress( 0x8000, 0x83ff, MWA_RAM ),
                new Memory_WriteAddress( 0xa000, 0xa000, AY8910_control_port_0_w ),
                new Memory_WriteAddress( 0xa002, 0xa002, AY8910_write_port_0_w ),
                new Memory_WriteAddress( 0xb000, 0xb000, AY8910_control_port_1_w ),
                new Memory_WriteAddress( 0xb002, 0xb002, AY8910_write_port_1_w ),
                new Memory_WriteAddress( 0xc000, 0xc000, ssio_status_w ),
                new Memory_WriteAddress( 0xe000, 0xe000, MWA_NOP ),
                new Memory_WriteAddress(MEMPORT_MARKER, 0)
        };

	
	/*************************************
	 *
	 *	Chip Squeak Deluxe communications
	 *
	 *	MC68000, 1 PIA, 10-bit DAC
	 *
	 *************************************/
	
	/********* internal interfaces ***********/
	public static WriteHandlerPtr csdeluxe_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dacval = (dacval & ~0x3fc) | (data << 2);
		DAC_signed_data_16_w(csdeluxe_dac_index, dacval << 6);
	} };
	
	public static WriteHandlerPtr csdeluxe_portb_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dacval = (dacval & ~0x003) | (data >> 6);
		DAC_signed_data_16_w(csdeluxe_dac_index, dacval << 6);
	} };
	
	static irqfuncPtr csdeluxe_irq = new irqfuncPtr() {
            public void handler(int state) {
                cpu_set_irq_line(csdeluxe_sound_cpu, 4, state!=0 ? ASSERT_LINE : CLEAR_LINE);
            }
        };
	
	public static timer_callback csdeluxe_delayed_data_w = new timer_callback() {
            public void handler(int param) {
                pia_0_portb_w.handler(0, param & 0x0f);
		pia_0_ca1_w.handler(0, ~param & 0x10);
            }
        };
	
	/********* external interfaces ***********/
	public static WriteHandlerPtr csdeluxe_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		timer_set(TIME_NOW, data, csdeluxe_delayed_data_w);
	} };
	
	public static void csdeluxe_reset_w(int state)
	{
		cpu_set_reset_line(csdeluxe_sound_cpu, state!=0 ? ASSERT_LINE : CLEAR_LINE);
	}
	
	
	/********* sound interfaces ***********/
	static DACinterface mcr_dac_interface = new DACinterface
	(
		1,
		new int[] { 100 }
	);
	
	static DACinterface mcr_dual_dac_interface = new DACinterface
	(
		2,
		new int[] { 75, 75 }
	);
	
	
	/********* memory interfaces ***********/
        public static Memory_ReadAddress csdeluxe_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x000000, 0x007fff, MRA_ROM ), // MRA16_ROM
                /*TODO*///new Memory_ReadAddress( 0x018000, 0x018007, pia_0_msb_r ),
                new Memory_ReadAddress( 0x01c000, 0x01cfff, MRA_RAM ), // MRA16_RAM
                new Memory_ReadAddress(MEMPORT_MARKER, 0)
        };
        
        public static Memory_WriteAddress csdeluxe_writemem[]={new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x000000, 0x007fff, MWA_ROM ), // MWA16_ROM
                /*TODO*///new Memory_WriteAddress( 0x018000, 0x018007, pia_0_msb_w ),
                new Memory_WriteAddress( 0x01c000, 0x01cfff, MWA_RAM ), // MWA16_ROM
                new Memory_WriteAddress(MEMPORT_MARKER, 0)
        };
	
	
	/********* PIA interfaces ***********/
	static pia6821_interface csdeluxe_pia_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ null, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ csdeluxe_porta_w, csdeluxe_portb_w, null, null,
		/*irqs   : A/B             */ csdeluxe_irq, csdeluxe_irq
        );
	
	
	
	/*************************************
	 *
	 *	MCR Sounds Good communications
	 *
	 *	MC68000, 1 PIA, 10-bit DAC
	 *
	 *************************************/
	
	/********* internal interfaces ***********/
	public static WriteHandlerPtr soundsgood_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dacval = (dacval & ~0x3fc) | (data << 2);
		DAC_signed_data_16_w(soundsgood_dac_index, dacval << 6);
	} };
	
	public static WriteHandlerPtr soundsgood_portb_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dacval = (dacval & ~0x003) | (data >> 6);
		DAC_signed_data_16_w(soundsgood_dac_index, dacval << 6);
		soundsgood_status = (data >> 4) & 3;
	} };
	
	static irqfuncPtr soundsgood_irq = new irqfuncPtr() {
            public void handler(int state) {
                cpu_set_irq_line(soundsgood_sound_cpu, 4, state!=0 ? ASSERT_LINE : CLEAR_LINE);
            }
        };
	
	static timer_callback soundsgood_delayed_data_w = new timer_callback() {
            public void handler(int param) {
                pia_1_portb_w.handler(0, (param >> 1) & 0x0f);
		pia_1_ca1_w.handler(0, ~param & 0x01);
            }
        };
	
	/********* external interfaces ***********/
	public static WriteHandlerPtr soundsgood_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		timer_set(TIME_NOW, data, soundsgood_delayed_data_w);
	} };
	
	public static ReadHandlerPtr soundsgood_status_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return soundsgood_status;
	} };
	
	public static void soundsgood_reset_w(int state)
	{
		cpu_set_reset_line(soundsgood_sound_cpu, state!=0 ? ASSERT_LINE : CLEAR_LINE);
	}
	
	
	/********* sound interfaces ***********/
	static DACinterface turbocs_plus_soundsgood_dac_interface = new DACinterface
	(
		2,
		new int[] { 80, 80 }
	);
	
	
/*TODO*///	/********* memory interfaces ***********/
/*TODO*///	MEMORY_READ16_START( soundsgood_readmem )
/*TODO*///		{ 0x000000, 0x03ffff, MRA16_ROM },
/*TODO*///		{ 0x060000, 0x060007, pia_1_msb_r },
/*TODO*///		{ 0x070000, 0x070fff, MRA16_RAM },
/*TODO*///	MEMORY_END
/*TODO*///	
/*TODO*///	MEMORY_WRITE16_START( soundsgood_writemem )
/*TODO*///		{ 0x000000, 0x03ffff, MWA16_ROM },
/*TODO*///		{ 0x060000, 0x060007, pia_1_msb_w },
/*TODO*///		{ 0x070000, 0x070fff, MWA16_RAM },
/*TODO*///	MEMORY_END
	
	
	/********* PIA interfaces ***********/
	/* Note: we map this board to PIA #1. It is only used in Spy Hunter and Spy Hunter 2 */
	/* For Spy Hunter 2, we also have a Turbo Chip Squeak in PIA slot 0, so we don't want */
	/* to interfere */
	static pia6821_interface soundsgood_pia_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ null, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ soundsgood_porta_w, soundsgood_portb_w, null, null,
		/*irqs   : A/B             */ soundsgood_irq, soundsgood_irq
        );
	
	
	
	/*************************************
	 *
	 *	MCR Turbo Chip Squeak communications
	 *
	 *	MC6809, 1 PIA, 8-bit DAC
	 *
	 *************************************/
	
	/********* internal interfaces ***********/
	public static WriteHandlerPtr turbocs_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dacval = (dacval & ~0x3fc) | (data << 2);
		DAC_signed_data_16_w(turbocs_dac_index, dacval << 6);
	} };
	
	public static WriteHandlerPtr turbocs_portb_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dacval = (dacval & ~0x003) | (data >> 6);
		DAC_signed_data_16_w(turbocs_dac_index, dacval << 6);
		turbocs_status = (data >> 4) & 3;
	} };
	
	static irqfuncPtr turbocs_irq = new irqfuncPtr() {
            public void handler(int state) {
                cpu_set_irq_line(turbocs_sound_cpu, M6809_IRQ_LINE, state!=0 ? ASSERT_LINE : CLEAR_LINE);
            }
        };
	
	public static timer_callback turbocs_delayed_data_w = new timer_callback() {
            public void handler(int param) {
		pia_0_portb_w.handler(0, (param >> 1) & 0x0f);
		pia_0_ca1_w.handler(0, ~param & 0x01);
            }
        };
	
	/********* external interfaces ***********/
	public static WriteHandlerPtr turbocs_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		timer_set(TIME_NOW, data, turbocs_delayed_data_w);
	} };
	
	public static ReadHandlerPtr turbocs_status_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return turbocs_status;
	} };
	
	public static void turbocs_reset_w(int state)
	{
		cpu_set_reset_line(turbocs_sound_cpu, state!=0 ? ASSERT_LINE : CLEAR_LINE);
	}
	
	
/*TODO*///	/********* memory interfaces ***********/
/*TODO*///	MEMORY_READ_START( turbocs_readmem )
/*TODO*///		{ 0x0000, 0x07ff, MRA_RAM },
/*TODO*///		{ 0x4000, 0x4003, pia_0_r },	/* Max RPM accesses the PIA here */
/*TODO*///		{ 0x6000, 0x6003, pia_0_r },
/*TODO*///		{ 0x8000, 0xffff, MRA_ROM },
/*TODO*///	MEMORY_END
/*TODO*///	
/*TODO*///	MEMORY_WRITE_START( turbocs_writemem )
/*TODO*///		{ 0x0000, 0x07ff, MWA_RAM },
/*TODO*///		{ 0x4000, 0x4003, pia_0_w },	/* Max RPM accesses the PIA here */
/*TODO*///		{ 0x6000, 0x6003, pia_0_w },
/*TODO*///		{ 0x8000, 0xffff, MWA_ROM },
/*TODO*///	MEMORY_END
/*TODO*///	
	
	/********* PIA interfaces ***********/
	static pia6821_interface turbocs_pia_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ null, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ turbocs_porta_w, turbocs_portb_w, null, null,
		/*irqs   : A/B             */ turbocs_irq, turbocs_irq
        );
	
	
	
	/*************************************
	 *
	 *	MCR Squawk n Talk communications
	 *
	 *	MC6802, 2 PIAs, TMS5220, AY8912 (not used), 8-bit DAC (not used)
	 *
	 *************************************/
	
	/********* internal interfaces ***********/
	public static WriteHandlerPtr squawkntalk_porta1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		logerror("Write to AY-8912\n");
	} };
	
	public static WriteHandlerPtr squawkntalk_porta2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		squawkntalk_tms_command = data;
	} };
	
	public static WriteHandlerPtr squawkntalk_portb2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bits 0-1 select read/write strobes on the TMS5220 */
		data &= 0x03;
	
		/* write strobe -- pass the current command to the TMS5220 */
		if ((((data ^ squawkntalk_tms_strobes) & 0x02)!=0) && ((data & 0x02)==0))
		{
			/*TODO*///tms5220_data_w(offset, squawkntalk_tms_command);
	
			/* DoT expects the ready line to transition on a command/write here, so we oblige */
			pia_1_ca2_w.handler(0, 1);
			pia_1_ca2_w.handler(0, 0);
		}
	
		/* read strobe -- read the current status from the TMS5220 */
		else if ((((data ^ squawkntalk_tms_strobes) & 0x01)!=0) && ((data & 0x01)==0))
		{
			/*TODO*///pia_1_porta_w(0, tms5220_status_r(offset));
	
			/* DoT expects the ready line to transition on a command/write here, so we oblige */
			pia_1_ca2_w.handler(0, 1);
			pia_1_ca2_w.handler(0, 0);
		}
	
		/* remember the state */
		squawkntalk_tms_strobes = data;
	} };
	
	static irqfuncPtr squawkntalk_irq = new irqfuncPtr() {
            public void handler(int state) {
                cpu_set_irq_line(squawkntalk_sound_cpu, M6808_IRQ_LINE, state!=0 ? ASSERT_LINE : CLEAR_LINE);
            }
        };
	
	static timer_callback squawkntalk_delayed_data_w = new timer_callback() {
            public void handler(int param) {
                pia_0_porta_w.handler(0, ~param & 0x0f);
		pia_0_cb1_w.handler(0, ~param & 0x10);
            }
        };
	
	/********* external interfaces ***********/
	public static WriteHandlerPtr squawkntalk_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		timer_set(TIME_NOW, data, squawkntalk_delayed_data_w);
	} };
	
	public static void squawkntalk_reset_w(int state)
	{
		cpu_set_reset_line(squawkntalk_sound_cpu, state!=0 ? ASSERT_LINE : CLEAR_LINE);
	}
	
	
/*TODO*///	/********* sound interfaces ***********/
/*TODO*///	struct TMS5220interface squawkntalk_tms5220_interface =
/*TODO*///	{
/*TODO*///		640000,
/*TODO*///		MIXER(60,MIXER_PAN_LEFT),
/*TODO*///		0
/*TODO*///	};
	
	
	/********* memory interfaces ***********/
        public static Memory_ReadAddress squawkntalk_readmem[]={
            new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
            new Memory_ReadAddress( 0x0000, 0x007f, MRA_RAM ),
            new Memory_ReadAddress( 0x0080, 0x0083, pia_0_r ),
            new Memory_ReadAddress( 0x0090, 0x0093, pia_1_r ),
            new Memory_ReadAddress( 0xd000, 0xffff, MRA_ROM ),
            new Memory_ReadAddress(MEMPORT_MARKER, 0)

        };
    
        public static Memory_WriteAddress squawkntalk_writemem []={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x007f, MWA_RAM ),
                new Memory_WriteAddress( 0x0080, 0x0083, pia_0_w ),
                new Memory_WriteAddress( 0x0090, 0x0093, pia_1_w ),
                new Memory_WriteAddress( 0xd000, 0xffff, MWA_ROM ),
                new Memory_WriteAddress(MEMPORT_MARKER, 0)
        };
	
	
	/********* PIA interfaces ***********/
	static pia6821_interface squawkntalk_pia0_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ null, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ squawkntalk_porta1_w, null, null, null,
		/*irqs   : A/B             */ squawkntalk_irq, squawkntalk_irq
        );
	
	static pia6821_interface squawkntalk_pia1_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ null, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ squawkntalk_porta2_w, squawkntalk_portb2_w, null, null,
		/*irqs   : A/B             */ squawkntalk_irq, squawkntalk_irq
        );    
}
