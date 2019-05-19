/***************************************************************************

	Taito Qix hardware

	driver by John Butler, Ed Mueller, Aaron Giles

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.cpu.m6809.m6809H.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame056.inptport.*;
import static mame056.machine._6812piaH.*;
import static mame056.machine._6812pia.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.cpuexec.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;
import static mame056.sound.sn76496.*;
import static mame056.sound.sn76496H.*;
import static WIP.mame056.vidhrdw.qix.*;
import static mame056.cpu.m6800.m6800H.*;

public class qix
{
	
	
	/* Globals */
	public static UBytePtr qix_sharedram = new UBytePtr();
	public static UBytePtr qix_68705_port_out = new UBytePtr();
	public static UBytePtr qix_68705_ddr = new UBytePtr();
	
	
	/* Local variables */
	static int[] qix_68705_port_in=new int[3];
	static int qix_coinctrl;
	
	
	/***************************************************************************
	
		Qix has 6 PIAs on board:
	
		From the ROM I/O schematic:
	
		PIA 0 = U11: (mapped to $9400 on the data CPU)
			port A = external input (input_port_0)
			port B = external input (input_port_1) (coin)
	
		PIA 1 = U20: (mapped to $9800/$9900 on the data CPU)
			port A = external input (???)
			port B = external input (???)
	
		PIA 2 = U30: (mapped to $9c00 on the data CPU)
			port A = external input (???)
			port B = external input (???)
	
	
		From the data/sound processor schematic:
	
		PIA 3 = U20: (mapped to $9000 on the data CPU)
			port A = data CPU to sound CPU communication
			port B = some kind of sound control, 2 4-bit values
			CA1 = interrupt signal from sound CPU
			CA2 = interrupt signal to sound CPU
			CB1 = VS input signal (vertical sync)
			CB2 = INV output signal (cocktail flip)
			IRQA = /DINT1 signal
			IRQB = /DINT1 signal
	
		PIA 4 = U8: (mapped to $4000 on the sound CPU)
			port A = sound CPU to data CPU communication
			port B = DAC value (port B)
			CA1 = interrupt signal from data CPU
			CA2 = interrupt signal to data CPU
			IRQA = /SINT1 signal
			IRQB = /SINT1 signal
	
		PIA 5 = U7: (never actually used, mapped to $2000 on the sound CPU)
			port A = unused
			port B = sound CPU to TMS5220 communication
			CA1 = interrupt signal from TMS5220
			CA2 = write signal to TMS5220
			CB1 = ready signal from TMS5220
			CB2 = read signal to TMS5220
			IRQA = /SINT2 signal
			IRQB = /SINT2 signal
	
	***************************************************************************/
	
	static pia6821_interface qix_pia_0_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ input_port_0_r, input_port_1_r, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, null, null, null,
		/*irqs   : A/B             */ null, null
        );
	
	static pia6821_interface qix_pia_1_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ input_port_2_r, input_port_3_r, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, null, null, null,
		/*irqs   : A/B             */ null, null
        );
        
        /*************************************
	 *
	 *	Coin I/O for games without coin CPU
	 *
	 *************************************/
	
	public static WriteHandlerPtr qix_coinctl_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                coin_lockout_w(0, (~data >> 2) & 1);
		coin_counter_w.handler(0, (data >> 1) & 1);
            }
        };
	
	static pia6821_interface qix_pia_2_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ input_port_4_r, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, qix_coinctl_w, null, null,
		/*irqs   : A/B             */ null, null
        );
        
        public static WriteHandlerPtr sync_pia_4_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* we need to synchronize this so the sound CPU doesn't drop anything important */
		timer_set(TIME_NOW, data, deferred_pia_4_porta_w);
	} };
        
        /*************************************
	 *
	 *	Cocktail flip
	 *
	 *************************************/
	
	public static WriteHandlerPtr qix_inv_flag_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		qix_cocktail_flip = data;
	} };
	
	
	
	
	
	public static WriteHandlerPtr slither_coinctl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		coin_lockout_w(0, (~data >> 6) & 1);
		coin_counter_w.handler(0, (data >> 5) & 1);
	} };
	
	/*************************************
	 *
	 *	IRQ generation
	 *
	 *************************************/
	
	static irqfuncPtr qix_pia_dint = new irqfuncPtr() {
            public void handler(int state) {
                /* DINT is connected to the data CPU's IRQ line */
		cpu_set_irq_line(0, M6809_IRQ_LINE, state!=0 ? ASSERT_LINE : CLEAR_LINE);
            }
        };
	
	
	static pia6821_interface qix_pia_3_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ null, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ sync_pia_4_porta_w, null, pia_4_ca1_w, qix_inv_flag_w,
		/*irqs   : A/B             */ qix_pia_dint, qix_pia_dint
        );
        
        public static WriteHandlerPtr qix_dac_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		DAC_data_w(0, data);
	} };
	
	static irqfuncPtr qix_pia_sint = new irqfuncPtr() {
            public void handler(int state) {
                /* SINT is connected to the sound CPU's IRQ line */
		cpu_set_irq_line(2, M6802_IRQ_LINE, state!=0 ? ASSERT_LINE : CLEAR_LINE);
            }
        };
	
	static pia6821_interface qix_pia_4_intf = new pia6821_interface
	(
		/*inputs : A/B,CA/B1,CA/B2 */ pia_4_porta_r, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ pia_3_porta_w, qix_dac_w, pia_3_ca1_w, null,
		/*irqs   : A/B             */ qix_pia_sint, qix_pia_sint
        );
	
	static pia6821_interface qix_pia_5_intf = new pia6821_interface
	(
		/*inputs : A/B,CA/B1,CA/B2 */ null, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ pia_3_porta_w, qix_dac_w, pia_3_ca1_w, null,
		/*irqs   : A/B             */ null, null
        );
	
	
	
	/***************************************************************************
	
		Games with an MCU need to handle coins differently, and provide
		communication with the MCU
	
	***************************************************************************/
        
        /*************************************
	 *
	 *	68705 Communication
	 *
	 *************************************/
	
	public static ReadHandlerPtr qixmcu_coin_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return qix_68705_port_out.read(0);
	} };
	
	
	public static WriteHandlerPtr qixmcu_coin_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* this is a callback called by pia_0_w(), so I don't need to synchronize */
		/* the CPUs - they have already been synchronized by qix_pia_0_w() */
		qix_68705_port_in[0] = data;
	} };
	
	
	public static WriteHandlerPtr qixmcu_coinctrl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if ((data & 0x04) != 0)
		{
			cpu_set_irq_line(3, M6809_IRQ_LINE, ASSERT_LINE);
			/* spin for a while to let the 68705 write the result */
			cpu_spinuntil_time(TIME_IN_USEC(50));
		}
		else
			cpu_set_irq_line(3, M6809_IRQ_LINE, CLEAR_LINE);
	
		/* this is a callback called by pia_0_w(), so I don't need to synchronize */
		/* the CPUs - they have already been synchronized by qix_pia_0_w() */
		qix_coinctrl = data;
	} };
	
	
	
	static pia6821_interface qixmcu_pia_0_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ input_port_0_r, qixmcu_coin_r, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, qixmcu_coin_w, null, null,
		/*irqs   : A/B             */ null, null
        );
	
	static pia6821_interface qixmcu_pia_2_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ input_port_4_r, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, qixmcu_coinctrl_w, null, null,
		/*irqs   : A/B             */ null, null
        );
	
	
	
	/***************************************************************************
	
		Slither uses 2 SN76489's for sound instead of the 6802+DAC; these
		are accessed via the PIAs.
	
	***************************************************************************/
        
        /*************************************
	 *
	 *	Slither trackball I/O
	 *
	 *************************************/
	
	public static ReadHandlerPtr slither_trak_lr_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return readinputport(qix_cocktail_flip!=0 ? 6 : 4);
	} };
	
	
	public static ReadHandlerPtr slither_trak_ud_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return readinputport(qix_cocktail_flip!=0 ? 5 : 3);
	} };
        
        /*************************************
	 *
	 *	Slither SN76489 I/O
	 *
	 *************************************/
	
	public static WriteHandlerPtr slither_76489_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* write to the sound chip */
		SN76496_0_w.handler(0, data);
	
		/* clock the ready line going back into CB1 */
		pia_1_cb1_w.handler(0, 0);
		pia_1_cb1_w.handler(0, 1);
	} };
	
	
	public static WriteHandlerPtr slither_76489_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* write to the sound chip */
		SN76496_1_w.handler(0, data);
	
		/* clock the ready line going back into CB1 */
		pia_2_cb1_w.handler(0, 0);
		pia_2_cb1_w.handler(0, 1);
	} };
	
	
	static pia6821_interface slither_pia_1_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ slither_trak_lr_r, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, slither_76489_0_w, null, null,
		/*irqs   : A/B             */ null, null
        );
	
	static pia6821_interface slither_pia_2_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ slither_trak_ud_r, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, slither_76489_1_w, null, null,
		/*irqs   : A/B             */ null, null
        );
	
	static pia6821_interface slither_pia_3_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ input_port_2_r, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, slither_coinctl_w, null, qix_inv_flag_w,
		/*irqs   : A/B             */ qix_pia_dint, qix_pia_dint
        );
	
	
	
	/*************************************
	 *
	 *	Machine initialization
	 *
	 *************************************/
	
	public static InitMachinePtr qix_init_machine = new InitMachinePtr() { public void handler() 
	{
		/* set a timer for the first scanline */
		timer_set(cpu_getscanlinetime(0), 0, qix_scanline_callback);
	
		/* configure and reset the PIAs */
		pia_unconfig();
		pia_config(0, PIA_STANDARD_ORDERING, qix_pia_0_intf);
		pia_config(1, PIA_STANDARD_ORDERING, qix_pia_1_intf);
		pia_config(2, PIA_STANDARD_ORDERING, qix_pia_2_intf);
		pia_config(3, PIA_STANDARD_ORDERING, qix_pia_3_intf);
		pia_config(4, PIA_STANDARD_ORDERING, qix_pia_4_intf);
		pia_config(5, PIA_STANDARD_ORDERING, qix_pia_5_intf);
		pia_reset();
	} };
	
	
	public static InitMachinePtr qixmcu_init_machine = new InitMachinePtr() { public void handler() 
	{
		/* set a timer for the first scanline */
		timer_set(cpu_getscanlinetime(0), 0, qix_scanline_callback);
	
		/* configure and reset the PIAs */
		pia_unconfig();
		pia_config(0, PIA_STANDARD_ORDERING, qixmcu_pia_0_intf);
		pia_config(1, PIA_STANDARD_ORDERING, qix_pia_1_intf);
		pia_config(2, PIA_STANDARD_ORDERING, qixmcu_pia_2_intf);
		pia_config(3, PIA_STANDARD_ORDERING, qix_pia_3_intf);
		pia_config(4, PIA_STANDARD_ORDERING, qix_pia_4_intf);
		pia_config(5, PIA_STANDARD_ORDERING, qix_pia_5_intf);
		pia_reset();
	
		/* reset the coin counter register */
		qix_coinctrl = 0x00;
	} };
	
	
	public static InitMachinePtr slither_init_machine = new InitMachinePtr() { public void handler() 
	{
		/* set a timer for the first scanline */
		timer_set(cpu_getscanlinetime(0), 0, qix_scanline_callback);
	
		/* configure and reset the PIAs */
		pia_unconfig();
		pia_config(0, PIA_STANDARD_ORDERING, qix_pia_0_intf);
		pia_config(1, PIA_STANDARD_ORDERING, slither_pia_1_intf);
		pia_config(2, PIA_STANDARD_ORDERING, slither_pia_2_intf);
		pia_config(3, PIA_STANDARD_ORDERING, slither_pia_3_intf);
		pia_reset();
	} };
	
	
	
	/*************************************
	 *
	 *	VSYNC interrupt handling
	 *
	 *************************************/
	
	static timer_callback vblank_stop = new timer_callback() {
            public void handler(int i) {
                pia_3_cb1_w.handler(0, 0);
            }
        };
	
	public static InterruptPtr qix_vblank_start = new InterruptPtr() {
            public int handler() {
                pia_3_cb1_w.handler(0, 1);
		timer_set(cpu_getscanlinetime(0), 0, vblank_stop);
		return ignore_interrupt.handler();
            }
        };
	
	
	/*************************************
	 *
	 *	Shared RAM
	 *
	 *************************************/
	
	public static ReadHandlerPtr qix_sharedram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return qix_sharedram.read(offset);
	} };
	
	
	public static WriteHandlerPtr qix_sharedram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		qix_sharedram.write(offset, data);
	} };
	
	
	
	/*************************************
	 *
	 *	Zoo Keeper bankswitching
	 *
	 *************************************/
	
	public static WriteHandlerPtr zoo_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU2));
	
		if ((data & 0x04)!=0)
			cpu_setbank(1, new UBytePtr(RAM,0x10000));
		else
			cpu_setbank(1, new UBytePtr(RAM, 0xa000));
	} };
	
	
	
	/*************************************
	 *
	 *	Data CPU FIRQ generation/ack
	 *
	 *************************************/
	
	public static WriteHandlerPtr qix_data_firq_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_set_irq_line(0, M6809_FIRQ_LINE, ASSERT_LINE);
	} };
	
	
	public static WriteHandlerPtr qix_data_firq_ack_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_set_irq_line(0, M6809_FIRQ_LINE, CLEAR_LINE);
	} };
	
	
	public static ReadHandlerPtr qix_data_firq_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		cpu_set_irq_line(0, M6809_FIRQ_LINE, ASSERT_LINE);
		return 0xff;
	} };
	
	
	public static ReadHandlerPtr qix_data_firq_ack_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		cpu_set_irq_line(0, M6809_FIRQ_LINE, CLEAR_LINE);
		return 0xff;
	} };
	
	
	
	/*************************************
	 *
	 *	Video CPU FIRQ generation/ack
	 *
	 *************************************/
	
	public static WriteHandlerPtr qix_video_firq_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_set_irq_line(1, M6809_FIRQ_LINE, ASSERT_LINE);
	} };
	
	
	public static WriteHandlerPtr qix_video_firq_ack_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_set_irq_line(1, M6809_FIRQ_LINE, CLEAR_LINE);
	} };
	
	
	public static ReadHandlerPtr qix_video_firq_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		cpu_set_irq_line(1, M6809_FIRQ_LINE, ASSERT_LINE);
		return 0xff;
	} };
	
	
	public static ReadHandlerPtr qix_video_firq_ack_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		cpu_set_irq_line(1, M6809_FIRQ_LINE, CLEAR_LINE);
		return 0xff;
	} };
	
	
	
	/*************************************
	 *
	 *	Sound PIA interfaces
	 *
	 *************************************/
	
	
	static timer_callback deferred_pia_4_porta_w = new timer_callback() {
            public void handler(int data) {
                pia_4_porta_w.handler(0, data);
            }
        };
	
	
	/*************************************
	 *
	 *	68705 Port Inputs
	 *
	 *************************************/
	
	public static ReadHandlerPtr qix_68705_portA_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int ddr = qix_68705_ddr.read(0);
		int out = qix_68705_port_out.read(0);
		int in = qix_68705_port_in[0];
		return (out & ddr) | (in & ~ddr);
	} };
	
	
	public static ReadHandlerPtr qix_68705_portB_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int ddr = qix_68705_ddr.read(1);
		int out = qix_68705_port_out.read(1);
		int in = (readinputport(1) & 0x0f) | ((readinputport(1) & 0x80) >> 3);
		return (out & ddr) | (in & ~ddr);
	} };
	
	
	public static ReadHandlerPtr qix_68705_portC_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int ddr = qix_68705_ddr.read(2);
		int out = qix_68705_port_out.read(2);
		int in = (~qix_coinctrl & 0x08) | ((readinputport(1) & 0x70) >> 4);
		return (out & ddr) | (in & ~ddr);
	} };
	
	
	
	/*************************************
	 *
	 *	68705 Port Outputs
	 *
	 *************************************/
	
	public static WriteHandlerPtr qix_68705_portA_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		qix_68705_port_out.write(0, data);
	} };
	
	
	public static WriteHandlerPtr qix_68705_portB_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		qix_68705_port_out.write(1, data);
		coin_lockout_w(0, (~data >> 6) & 1);
		coin_counter_w.handler(0, (data >> 7) & 1);
	} };
	
	
	public static WriteHandlerPtr qix_68705_portC_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		qix_68705_port_out.write(2, data);
	} };
	
	
	
	/*************************************
	 *
	 *	Data CPU PIA 0 synchronization
	 *
	 *************************************/
	
	static timer_callback pia_0_w_callback = new timer_callback() {
            public void handler(int param) {
                pia_0_w.handler(param >> 8, param & 0xff);
            }
        };
	
	public static WriteHandlerPtr qix_pia_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* make all the CPUs synchronize, and only AFTER that write the command to the PIA */
		/* otherwise the 68705 will miss commands */
		timer_set(TIME_NOW, data | (offset << 8), pia_0_w_callback);
	} };
	
	
	
	/*************************************
	 *
	 *	PIA/Protection(?) workarounds
	 *
	 *************************************/
	
	public static WriteHandlerPtr zookeep_pia_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* Hack: Kram and Zoo Keeper for some reason (protection?) leave the port A */
		/* DDR set to 0xff, so they cannot read the player 1 controls. Here we force */
		/* the DDR to 0, so the controls work correctly. */
		if (offset == 0)
			data = 0;
		qix_pia_0_w.handler(offset, data);
	} };
	
	
	public static WriteHandlerPtr zookeep_pia_2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* Hack: Zoo Keeper for some reason (protection?) leaves the port A */
		/* DDR set to 0xff, so they cannot read the player 2 controls. Here we force */
		/* the DDR to 0, so the controls work correctly. */
		if (offset == 0)
			data = 0;
		pia_2_w.handler(offset, data);
	} };
	
}
