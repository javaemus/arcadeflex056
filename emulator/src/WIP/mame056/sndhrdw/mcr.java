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

public class mcr {
	/*************************************
	 *
	 *	Global variables
	 *
	 *************************************/
	
	public static int mcr_sound_config;
	
	
	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Statics
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	static UINT16 dacval;
/*TODO*///	
/*TODO*///	/* SSIO-specific globals */
/*TODO*///	static UINT8 ssio_sound_cpu;
	static int[] ssio_data=new int[4];
	static int ssio_status;
	static int[][] ssio_duty_cycle=new int[2][3];
/*TODO*///	
/*TODO*///	/* Chip Squeak Deluxe-specific globals */
/*TODO*///	static UINT8 csdeluxe_sound_cpu;
/*TODO*///	static UINT8 csdeluxe_dac_index;
/*TODO*///	extern struct pia6821_interface csdeluxe_pia_intf;
/*TODO*///	
/*TODO*///	/* Turbo Chip Squeak-specific globals */
/*TODO*///	static UINT8 turbocs_sound_cpu;
/*TODO*///	static UINT8 turbocs_dac_index;
/*TODO*///	static UINT8 turbocs_status;
/*TODO*///	extern struct pia6821_interface turbocs_pia_intf;
/*TODO*///	
/*TODO*///	/* Sounds Good-specific globals */
/*TODO*///	static UINT8 soundsgood_sound_cpu;
/*TODO*///	static UINT8 soundsgood_dac_index;
/*TODO*///	static UINT8 soundsgood_status;
/*TODO*///	extern struct pia6821_interface soundsgood_pia_intf;
/*TODO*///	
/*TODO*///	/* Squawk n' Talk-specific globals */
/*TODO*///	static UINT8 squawkntalk_sound_cpu;
/*TODO*///	static UINT8 squawkntalk_tms_command;
/*TODO*///	static UINT8 squawkntalk_tms_strobes;
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
/*TODO*///		if (mcr_sound_config & MCR_SSIO)
/*TODO*///		{
/*TODO*///			ssio_sound_cpu = sound_cpu++;
/*TODO*///			ssio_reset_w(1);
/*TODO*///			ssio_reset_w(0);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* Turbo Chip Squeak */
/*TODO*///		if (mcr_sound_config & MCR_TURBO_CHIP_SQUEAK)
/*TODO*///		{
/*TODO*///			pia_config(0, PIA_ALTERNATE_ORDERING, &turbocs_pia_intf);
/*TODO*///			turbocs_dac_index = dac_index++;
/*TODO*///			turbocs_sound_cpu = sound_cpu++;
/*TODO*///			turbocs_reset_w(1);
/*TODO*///			turbocs_reset_w(0);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* Chip Squeak Deluxe */
/*TODO*///		if (mcr_sound_config & MCR_CHIP_SQUEAK_DELUXE)
/*TODO*///		{
/*TODO*///			pia_config(0, PIA_ALTERNATE_ORDERING, &csdeluxe_pia_intf);
/*TODO*///			csdeluxe_dac_index = dac_index++;
/*TODO*///			csdeluxe_sound_cpu = sound_cpu++;
/*TODO*///			csdeluxe_reset_w(1);
/*TODO*///			csdeluxe_reset_w(0);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* Sounds Good */
/*TODO*///		if (mcr_sound_config & MCR_SOUNDS_GOOD)
/*TODO*///		{
/*TODO*///			/* special case: Spy Hunter 2 has both Turbo CS and Sounds Good, so we use PIA slot 1 */
/*TODO*///			pia_config(1, PIA_ALTERNATE_ORDERING, &soundsgood_pia_intf);
/*TODO*///			soundsgood_dac_index = dac_index++;
/*TODO*///			soundsgood_sound_cpu = sound_cpu++;
/*TODO*///			soundsgood_reset_w(1);
/*TODO*///			soundsgood_reset_w(0);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* Squawk n Talk */
/*TODO*///		if (mcr_sound_config & MCR_SQUAWK_N_TALK)
/*TODO*///		{
/*TODO*///			pia_config(0, PIA_STANDARD_ORDERING, &squawkntalk_pia0_intf);
/*TODO*///			pia_config(1, PIA_STANDARD_ORDERING, &squawkntalk_pia1_intf);
/*TODO*///			squawkntalk_sound_cpu = sound_cpu++;
/*TODO*///			squawkntalk_reset_w(1);
/*TODO*///			squawkntalk_reset_w(0);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* Advanced Audio */
/*TODO*///		if (mcr_sound_config & MCR_WILLIAMS_SOUND)
/*TODO*///		{
/*TODO*///			williams_cvsd_init(sound_cpu++, 0);
/*TODO*///			dac_index++;
/*TODO*///			williams_cvsd_reset_w(1);
/*TODO*///			williams_cvsd_reset_w(0);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* reset any PIAs */
/*TODO*///		pia_reset();
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
	
/*TODO*///	void ssio_reset_w(int state)
/*TODO*///	{
/*TODO*///		/* going high halts the CPU */
/*TODO*///		if (state)
/*TODO*///		{
/*TODO*///			int i;
/*TODO*///	
/*TODO*///			cpu_set_reset_line(ssio_sound_cpu, ASSERT_LINE);
/*TODO*///	
/*TODO*///			/* latches also get reset */
/*TODO*///			for (i = 0; i < 4; i++)
/*TODO*///				ssio_data[i] = 0;
/*TODO*///			ssio_status = 0;
/*TODO*///		}
/*TODO*///		/* going low resets and reactivates the CPU */
/*TODO*///		else
/*TODO*///			cpu_set_reset_line(ssio_sound_cpu, CLEAR_LINE);
/*TODO*///	}
/*TODO*///	
	
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
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Chip Squeak Deluxe communications
/*TODO*///	 *
/*TODO*///	 *	MC68000, 1 PIA, 10-bit DAC
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	/********* internal interfaces ***********/
/*TODO*///	public static WriteHandlerPtr csdeluxe_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		dacval = (dacval & ~0x3fc) | (data << 2);
/*TODO*///		DAC_signed_data_16_w(csdeluxe_dac_index, dacval << 6);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr csdeluxe_portb_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		dacval = (dacval & ~0x003) | (data >> 6);
/*TODO*///		DAC_signed_data_16_w(csdeluxe_dac_index, dacval << 6);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	static void csdeluxe_irq(int state)
/*TODO*///	{
/*TODO*///	  	cpu_set_irq_line(csdeluxe_sound_cpu, 4, state ? ASSERT_LINE : CLEAR_LINE);
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void csdeluxe_delayed_data_w(int param)
/*TODO*///	{
/*TODO*///		pia_0_portb_w(0, param & 0x0f);
/*TODO*///		pia_0_ca1_w(0, ~param & 0x10);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/********* external interfaces ***********/
/*TODO*///	public static WriteHandlerPtr csdeluxe_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		timer_set(TIME_NOW, data, csdeluxe_delayed_data_w);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	void csdeluxe_reset_w(int state)
/*TODO*///	{
/*TODO*///		cpu_set_reset_line(csdeluxe_sound_cpu, state ? ASSERT_LINE : CLEAR_LINE);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/********* sound interfaces ***********/
/*TODO*///	static DACinterface mcr_dac_interface = new DACinterface
/*TODO*///	(
/*TODO*///		1,
/*TODO*///		new int[] { 100 }
/*TODO*///	);
/*TODO*///	
/*TODO*///	static DACinterface mcr_dual_dac_interface = new DACinterface
/*TODO*///	(
/*TODO*///		2,
/*TODO*///		new int[] { 75, 75 }
/*TODO*///	);
/*TODO*///	
/*TODO*///	
/*TODO*///	/********* memory interfaces ***********/
/*TODO*///	MEMORY_READ16_START( csdeluxe_readmem )
/*TODO*///		{ 0x000000, 0x007fff, MRA16_ROM },
/*TODO*///		{ 0x018000, 0x018007, pia_0_msb_r },
/*TODO*///		{ 0x01c000, 0x01cfff, MRA16_RAM },
/*TODO*///	MEMORY_END
/*TODO*///	
/*TODO*///	MEMORY_WRITE16_START( csdeluxe_writemem )
/*TODO*///		{ 0x000000, 0x007fff, MWA16_ROM },
/*TODO*///		{ 0x018000, 0x018007, pia_0_msb_w },
/*TODO*///		{ 0x01c000, 0x01cfff, MWA16_RAM },
/*TODO*///	MEMORY_END
/*TODO*///	
/*TODO*///	
/*TODO*///	/********* PIA interfaces ***********/
/*TODO*///	struct pia6821_interface csdeluxe_pia_intf =
/*TODO*///	{
/*TODO*///		/*inputs : A/B,CA/B1,CA/B2 */ 0, 0, 0, 0, 0, 0,
/*TODO*///		/*outputs: A/B,CA/B2       */ csdeluxe_porta_w, csdeluxe_portb_w, 0, 0,
/*TODO*///		/*irqs   : A/B             */ csdeluxe_irq, csdeluxe_irq
/*TODO*///	};
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	MCR Sounds Good communications
/*TODO*///	 *
/*TODO*///	 *	MC68000, 1 PIA, 10-bit DAC
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	/********* internal interfaces ***********/
/*TODO*///	public static WriteHandlerPtr soundsgood_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		dacval = (dacval & ~0x3fc) | (data << 2);
/*TODO*///		DAC_signed_data_16_w(soundsgood_dac_index, dacval << 6);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr soundsgood_portb_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		dacval = (dacval & ~0x003) | (data >> 6);
/*TODO*///		DAC_signed_data_16_w(soundsgood_dac_index, dacval << 6);
/*TODO*///		soundsgood_status = (data >> 4) & 3;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	static void soundsgood_irq(int state)
/*TODO*///	{
/*TODO*///	  	cpu_set_irq_line(soundsgood_sound_cpu, 4, state ? ASSERT_LINE : CLEAR_LINE);
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void soundsgood_delayed_data_w(int param)
/*TODO*///	{
/*TODO*///		pia_1_portb_w(0, (param >> 1) & 0x0f);
/*TODO*///		pia_1_ca1_w(0, ~param & 0x01);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/********* external interfaces ***********/
/*TODO*///	public static WriteHandlerPtr soundsgood_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		timer_set(TIME_NOW, data, soundsgood_delayed_data_w);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr soundsgood_status_r  = new ReadHandlerPtr() { public int handler(int offset)
/*TODO*///	{
/*TODO*///		return soundsgood_status;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	void soundsgood_reset_w(int state)
/*TODO*///	{
/*TODO*///		cpu_set_reset_line(soundsgood_sound_cpu, state ? ASSERT_LINE : CLEAR_LINE);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/********* sound interfaces ***********/
/*TODO*///	static DACinterface turbocs_plus_soundsgood_dac_interface = new DACinterface
/*TODO*///	(
/*TODO*///		2,
/*TODO*///		new int[] { 80, 80 }
/*TODO*///	);
/*TODO*///	
/*TODO*///	
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
/*TODO*///	
/*TODO*///	
/*TODO*///	/********* PIA interfaces ***********/
/*TODO*///	/* Note: we map this board to PIA #1. It is only used in Spy Hunter and Spy Hunter 2 */
/*TODO*///	/* For Spy Hunter 2, we also have a Turbo Chip Squeak in PIA slot 0, so we don't want */
/*TODO*///	/* to interfere */
/*TODO*///	struct pia6821_interface soundsgood_pia_intf =
/*TODO*///	{
/*TODO*///		/*inputs : A/B,CA/B1,CA/B2 */ 0, 0, 0, 0, 0, 0,
/*TODO*///		/*outputs: A/B,CA/B2       */ soundsgood_porta_w, soundsgood_portb_w, 0, 0,
/*TODO*///		/*irqs   : A/B             */ soundsgood_irq, soundsgood_irq
/*TODO*///	};
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	MCR Turbo Chip Squeak communications
/*TODO*///	 *
/*TODO*///	 *	MC6809, 1 PIA, 8-bit DAC
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	/********* internal interfaces ***********/
/*TODO*///	public static WriteHandlerPtr turbocs_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		dacval = (dacval & ~0x3fc) | (data << 2);
/*TODO*///		DAC_signed_data_16_w(turbocs_dac_index, dacval << 6);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr turbocs_portb_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		dacval = (dacval & ~0x003) | (data >> 6);
/*TODO*///		DAC_signed_data_16_w(turbocs_dac_index, dacval << 6);
/*TODO*///		turbocs_status = (data >> 4) & 3;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	static void turbocs_irq(int state)
/*TODO*///	{
/*TODO*///		cpu_set_irq_line(turbocs_sound_cpu, M6809_IRQ_LINE, state ? ASSERT_LINE : CLEAR_LINE);
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void turbocs_delayed_data_w(int param)
/*TODO*///	{
/*TODO*///		pia_0_portb_w(0, (param >> 1) & 0x0f);
/*TODO*///		pia_0_ca1_w(0, ~param & 0x01);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/********* external interfaces ***********/
/*TODO*///	public static WriteHandlerPtr turbocs_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		timer_set(TIME_NOW, data, turbocs_delayed_data_w);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr turbocs_status_r  = new ReadHandlerPtr() { public int handler(int offset)
/*TODO*///	{
/*TODO*///		return turbocs_status;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	void turbocs_reset_w(int state)
/*TODO*///	{
/*TODO*///		cpu_set_reset_line(turbocs_sound_cpu, state ? ASSERT_LINE : CLEAR_LINE);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
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
/*TODO*///	
/*TODO*///	/********* PIA interfaces ***********/
/*TODO*///	struct pia6821_interface turbocs_pia_intf =
/*TODO*///	{
/*TODO*///		/*inputs : A/B,CA/B1,CA/B2 */ 0, 0, 0, 0, 0, 0,
/*TODO*///		/*outputs: A/B,CA/B2       */ turbocs_porta_w, turbocs_portb_w, 0, 0,
/*TODO*///		/*irqs   : A/B             */ turbocs_irq, turbocs_irq
/*TODO*///	};
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	MCR Squawk n Talk communications
/*TODO*///	 *
/*TODO*///	 *	MC6802, 2 PIAs, TMS5220, AY8912 (not used), 8-bit DAC (not used)
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	/********* internal interfaces ***********/
/*TODO*///	public static WriteHandlerPtr squawkntalk_porta1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		logerror("Write to AY-8912\n");
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr squawkntalk_porta2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		squawkntalk_tms_command = data;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr squawkntalk_portb2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		/* bits 0-1 select read/write strobes on the TMS5220 */
/*TODO*///		data &= 0x03;
/*TODO*///	
/*TODO*///		/* write strobe -- pass the current command to the TMS5220 */
/*TODO*///		if (((data ^ squawkntalk_tms_strobes) & 0x02) && !(data & 0x02))
/*TODO*///		{
/*TODO*///			tms5220_data_w(offset, squawkntalk_tms_command);
/*TODO*///	
/*TODO*///			/* DoT expects the ready line to transition on a command/write here, so we oblige */
/*TODO*///			pia_1_ca2_w(0, 1);
/*TODO*///			pia_1_ca2_w(0, 0);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* read strobe -- read the current status from the TMS5220 */
/*TODO*///		else if (((data ^ squawkntalk_tms_strobes) & 0x01) && !(data & 0x01))
/*TODO*///		{
/*TODO*///			pia_1_porta_w(0, tms5220_status_r(offset));
/*TODO*///	
/*TODO*///			/* DoT expects the ready line to transition on a command/write here, so we oblige */
/*TODO*///			pia_1_ca2_w(0, 1);
/*TODO*///			pia_1_ca2_w(0, 0);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* remember the state */
/*TODO*///		squawkntalk_tms_strobes = data;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	static void squawkntalk_irq(int state)
/*TODO*///	{
/*TODO*///		cpu_set_irq_line(squawkntalk_sound_cpu, M6808_IRQ_LINE, state ? ASSERT_LINE : CLEAR_LINE);
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void squawkntalk_delayed_data_w(int param)
/*TODO*///	{
/*TODO*///		pia_0_porta_w(0, ~param & 0x0f);
/*TODO*///		pia_0_cb1_w(0, ~param & 0x10);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/********* external interfaces ***********/
/*TODO*///	public static WriteHandlerPtr squawkntalk_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		timer_set(TIME_NOW, data, squawkntalk_delayed_data_w);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	void squawkntalk_reset_w(int state)
/*TODO*///	{
/*TODO*///		cpu_set_reset_line(squawkntalk_sound_cpu, state ? ASSERT_LINE : CLEAR_LINE);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/********* sound interfaces ***********/
/*TODO*///	struct TMS5220interface squawkntalk_tms5220_interface =
/*TODO*///	{
/*TODO*///		640000,
/*TODO*///		MIXER(60,MIXER_PAN_LEFT),
/*TODO*///		0
/*TODO*///	};
/*TODO*///	
/*TODO*///	
/*TODO*///	/********* memory interfaces ***********/
/*TODO*///	MEMORY_READ_START( squawkntalk_readmem )
/*TODO*///		{ 0x0000, 0x007f, MRA_RAM },
/*TODO*///		{ 0x0080, 0x0083, pia_0_r },
/*TODO*///		{ 0x0090, 0x0093, pia_1_r },
/*TODO*///		{ 0xd000, 0xffff, MRA_ROM },
/*TODO*///	MEMORY_END
/*TODO*///	
/*TODO*///	MEMORY_WRITE_START( squawkntalk_writemem )
/*TODO*///		{ 0x0000, 0x007f, MWA_RAM },
/*TODO*///		{ 0x0080, 0x0083, pia_0_w },
/*TODO*///		{ 0x0090, 0x0093, pia_1_w },
/*TODO*///		{ 0xd000, 0xffff, MWA_ROM },
/*TODO*///	MEMORY_END
/*TODO*///	
/*TODO*///	
/*TODO*///	/********* PIA interfaces ***********/
/*TODO*///	struct pia6821_interface squawkntalk_pia0_intf =
/*TODO*///	{
/*TODO*///		/*inputs : A/B,CA/B1,CA/B2 */ 0, 0, 0, 0, 0, 0,
/*TODO*///		/*outputs: A/B,CA/B2       */ squawkntalk_porta1_w, 0, 0, 0,
/*TODO*///		/*irqs   : A/B             */ squawkntalk_irq, squawkntalk_irq
/*TODO*///	};
/*TODO*///	
/*TODO*///	struct pia6821_interface squawkntalk_pia1_intf =
/*TODO*///	{
/*TODO*///		/*inputs : A/B,CA/B1,CA/B2 */ 0, 0, 0, 0, 0, 0,
/*TODO*///		/*outputs: A/B,CA/B2       */ squawkntalk_porta2_w, squawkntalk_portb2_w, 0, 0,
/*TODO*///		/*irqs   : A/B             */ squawkntalk_irq, squawkntalk_irq
/*TODO*///	};    
}
