/***************************************************************************

  machine.c

  Functions to emulate general aspects of the machine (RAM, ROM, interrupts,
  I/O ports)

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static common.ptr.*;
import static arcadeflex056.fucPtr.*;
import static common.subArrays.*;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.machine._6812piaH.*;
import static mame056.machine._6812pia.*;
import static mame056.machine.ticket.*;
import static mame056.machine.ticketH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.cpuexec.*;
import static mame056.cpuintrfH.*;
import static mame056.palette.*;
import static common.libc.cstdio.sprintf;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpu.m6800.m6800H.M6800_IRQ_LINE;
import static mame056.cpu.m6809.m6809H.M6809_FIRQ_LINE;
import static mame056.cpu.m6809.m6809H.M6809_IRQ_LINE;
import static mame056.sound.dac.*;

import static WIP.mame056.vidhrdw.williams.*;

public class williams
{
	
	/* banking addresses set by the drivers */
	public static UBytePtr williams_bank_base = new UBytePtr();
	public static UBytePtr defender_bank_base = new UBytePtr();
	public static IntArray defender_bank_list;
	public static UBytePtr mayday_protection = new UBytePtr();
	
	/* internal bank switching tracking */
	static int blaster_bank;
	static int vram_bank;
	public static int williams2_bank;
	
	/* switches controlled by $c900 */
	public static int sinistar_clip;
	static int williams_cocktail;
	
	/* other stuff */
	static int joust2_current_sound_data;
	
	/* input port mapping */
	static int port_select;
	
	/* newer-Williams routines */
	
	/* Defender-specific code */
	
	/* Stargate-specific code */
	
	/* Lotto Fun-specific code */
	
	/* Turkey Shoot-specific code */
	
	/* Joust 2-specific code */
	
	/*************************************
	 *
	 *	Older Williams interrupts
	 *
	 *************************************/
	
	static timer_callback williams_va11_callback = new timer_callback() {
            public void handler(int scanline) {
                /* the IRQ signal comes into CB1, and is set to VA11 */
		pia_1_cb1_w.handler(0, scanline & 0x20);
	
		/* update the screen while we're here */
		williams_vh_update(scanline);
	
		/* set a timer for the next update */
		scanline += 8;
		if (scanline >= 256) scanline = 0;
		timer_set(cpu_getscanlinetime(scanline), scanline, williams_va11_callback);
            }
        };
	
	static timer_callback williams_count240_off_callback = new timer_callback() {
            public void handler(int param) {
                /* the COUNT240 signal comes into CA1, and is set to the logical AND of VA10-VA13 */
		pia_1_ca1_w.handler(0, 0);
            }
        };
	
	static timer_callback williams_count240_callback = new timer_callback() {
            public void handler(int param) {
		/* the COUNT240 signal comes into CA1, and is set to the logical AND of VA10-VA13 */
		pia_1_ca1_w.handler(0, 1);
	
		/* set a timer to turn it off once the scanline counter resets */
		timer_set(cpu_getscanlinetime(0), 0, williams_count240_off_callback);
	
		/* set a timer for next frame */
		timer_set(cpu_getscanlinetime(240), 0, williams_count240_callback);
	}};
	
	
	static irqfuncPtr williams_main_irq = new irqfuncPtr() {
            public void handler(int state) {
                /* IRQ to the main CPU */
		cpu_set_irq_line(0, M6809_IRQ_LINE, state!=0 ? ASSERT_LINE : CLEAR_LINE);
            }
        };
	
	static irqfuncPtr williams_main_firq = new irqfuncPtr() {
            public void handler(int state) {
		/* FIRQ to the main CPU */
		cpu_set_irq_line(0, M6809_FIRQ_LINE, state!=0 ? ASSERT_LINE : CLEAR_LINE);
            }
	};
	
	
	static irqfuncPtr williams_snd_irq = new irqfuncPtr() {
            public void handler(int state) {
		/* IRQ to the sound CPU */
		cpu_set_irq_line(1, M6800_IRQ_LINE, state!=0 ? ASSERT_LINE : CLEAR_LINE);
            }
	};
	
	
	
	/*************************************
	 *
	 *	Older Williams initialization
	 *
	 *************************************/
	
	public static InitMachinePtr williams_init_machine = new InitMachinePtr() { public void handler() 
	{
		/* reset the PIAs */
		pia_reset();
	
		/* reset the ticket dispenser (Lotto Fun) */
		ticket_dispenser_init(70, TICKET_MOTOR_ACTIVE_LOW, TICKET_STATUS_ACTIVE_HIGH);
	
		/* set a timer to go off every 16 scanlines, to toggle the VA11 line and update the screen */
		timer_set(cpu_getscanlinetime(0), 0, williams_va11_callback);
	
		/* also set a timer to go off on scanline 240 */
		timer_set(cpu_getscanlinetime(240), 0, williams_count240_callback);
	} };
	
	
	
	/*************************************
	 *
	 *	Older Williams VRAM/ROM banking
	 *
	 *************************************/
	
	public static WriteHandlerPtr williams_vram_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* VRAM/ROM banking from bit 0 */
		vram_bank = data & 0x01;
	
		/* cocktail flip from bit 1 */
		williams_cocktail = data & 0x02;
	
		/* sinistar clipping enable from bit 2 */
		sinistar_clip = (data & 0x04)!=0 ? 0x7400 : 0xffff;
	
		/* set the bank */
		if (vram_bank != 0)
		{
			cpu_setbank(1, williams_bank_base);
		}
		else
		{
			cpu_setbank(1, williams_videoram);
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Older Williams sound commands
	 *
	 *************************************/
	
	static timer_callback williams_deferred_snd_cmd_w = new timer_callback() {
            public void handler(int param) {
                pia_2_portb_w.handler(0, param);
		pia_2_cb1_w.handler(0, (param == 0xff) ? 0 : 1);
            }
        };
	
	public static WriteHandlerPtr williams_snd_cmd_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* the high two bits are set externally, and should be 1 */
		timer_set(TIME_NOW, data | 0xc0, williams_deferred_snd_cmd_w);
	} };
	
	
	
	/*************************************
	 *
	 *	General input port handlers
	 *
	 *************************************/
	
	public static WriteHandlerPtr williams_port_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		port_select = data;
	} };
	
	
	public static ReadHandlerPtr williams_input_port_0_3_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return readinputport(port_select!=0 ? 3 : 0);
	} };
	
	
	public static ReadHandlerPtr williams_input_port_1_4_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return readinputport(port_select!=0 ? 4 : 1);
	} };
	
	
	/*
	 *  Williams 49-way joystick
	 *
	 * The joystick has 48 positions + center.
	 *
	 * I'm not 100% sure but it looks like it's mapped this way:
	 *
	 *	xxxx1000 = up full
	 *	xxxx1100 = up 2/3
	 *	xxxx1110 = up 1/3
	 *	xxxx0111 = center
	 *	xxxx0011 = down 1/3
	 *	xxxx0001 = down 2/3
	 *	xxxx0000 = down full
	 *
	 *	1000xxxx = right full
	 *	1100xxxx = right 2/3
	 *	1110xxxx = right 1/3
	 *	0111xxxx = center
	 *	0011xxxx = left 1/3
	 *	0001xxxx = left 2/3
	 *	0000xxxx = left full
	 *
	 */
	
	public static ReadHandlerPtr williams_49way_port_0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int joy_x, joy_y;
		int bits_x, bits_y;
	
		joy_x = readinputport(3) >> 4;	/* 0 = left 3 = center 6 = right */
		joy_y = readinputport(4) >> 4;	/* 0 = down 3 = center 6 = up */
	
		bits_x = (0x70 >> (7 - joy_x)) & 0x0f;
		bits_y = (0x70 >> (7 - joy_y)) & 0x0f;
	
		return (bits_x << 4) | bits_y;
	} };
	
	
	
	/*************************************
	 *
	 *	Newer Williams interrupts
	 *
	 *************************************/
	
	static timer_callback williams2_va11_callback = new timer_callback() {
            public void handler(int scanline) {
                /* the IRQ signal comes into CB1, and is set to VA11 */
		pia_0_cb1_w.handler(0, scanline & 0x20);
		pia_1_ca1_w.handler(0, scanline & 0x20);
	
		/* update the screen while we're here */
		williams_vh_update(scanline);
	
		/* set a timer for the next update */
		scanline += 8;
		if (scanline >= 256) scanline = 0;
		timer_set(cpu_getscanlinetime(scanline), scanline, williams2_va11_callback);
            }
        };
	
	
	static timer_callback williams2_endscreen_off_callback = new timer_callback() {
            public void handler(int param) {
                /* the /ENDSCREEN signal comes into CA1 */
		pia_0_ca1_w.handler(0, 1);
            }
        };
	
	static timer_callback williams2_endscreen_callback = new timer_callback() {
            public void handler(int param) {
                /* the /ENDSCREEN signal comes into CA1 */
		pia_0_ca1_w.handler(0, 0);
	
		/* set a timer to turn it off once the scanline counter resets */
		timer_set(cpu_getscanlinetime(8), 0, williams2_endscreen_off_callback);
	
		/* set a timer for next frame */
		timer_set(cpu_getscanlinetime(254), 0, williams2_endscreen_callback);
            }
        };
	
	
	/*************************************
	 *
	 *	Newer Williams initialization
	 *
	 *************************************/
	
	public static InitMachinePtr williams2_init_machine = new InitMachinePtr() { public void handler() 
	{
		/* reset the PIAs */
		pia_reset();
	
		/* make sure our banking is reset */
		williams2_bank_select_w.handler(0, 0);
	
		/* set a timer to go off every 16 scanlines, to toggle the VA11 line and update the screen */
		timer_set(cpu_getscanlinetime(0), 0, williams2_va11_callback);
	
		/* also set a timer to go off on scanline 254 */
		timer_set(cpu_getscanlinetime(254), 0, williams2_endscreen_callback);
	} };
	
	
	
	/*************************************
	 *
	 *	Newer Williams ROM banking
	 *
	 *************************************/
	
	public static WriteHandlerPtr williams2_bank_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int bank[] = { 0, 0x10000, 0x20000, 0x10000, 0, 0x30000, 0x40000, 0x30000 };
	
		/* select bank index (only lower 3 bits used by IC56) */
		williams2_bank = data & 0x07;
	
		/* bank 0 references videoram */
		if (williams2_bank == 0)
		{
			cpu_setbank(1, williams_videoram);
			cpu_setbank(2, new UBytePtr(williams_videoram, 0x8000));
		}
	
		/* other banks reference ROM plus either palette RAM or the top of videoram */
		else
		{
			UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
			cpu_setbank(1, new UBytePtr(RAM, bank[williams2_bank]));
	
			if ((williams2_bank & 0x03) == 0x03)
			{
				cpu_setbank(2, williams2_paletteram);
			}
			else
			{
				cpu_setbank(2, new UBytePtr(williams_videoram, 0x8000));
			}
		}
	
		/* regardless, the top 2k references videoram */
		cpu_setbank(3, new UBytePtr(williams_videoram, 0x8800));
	} };
	
	
	
	/*************************************
	 *
	 *	Newer Williams sound commands
	 *
	 *************************************/
	
	static timer_callback williams2_deferred_snd_cmd_w = new timer_callback() {
            public void handler(int param) {
                pia_2_porta_w.handler(0, param);
            }
        };
	
	public static WriteHandlerPtr williams2_snd_cmd_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		timer_set(TIME_NOW, data, williams2_deferred_snd_cmd_w);
	} };
	
	
	
	/*************************************
	 *
	 *	Newer Williams other stuff
	 *
	 *************************************/
	
	public static WriteHandlerPtr williams2_7segment_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int n;
		char dot;
		String buffer;
	
		switch (data & 0x7F)
		{
			case 0x40:	n = 0; break;
			case 0x79:	n = 1; break;
			case 0x24:	n = 2; break;
			case 0x30:	n = 3; break;
			case 0x19:	n = 4; break;
			case 0x12:	n = 5; break;
			case 0x02:	n = 6; break;
			case 0x03:	n = 6; break;
			case 0x78:	n = 7; break;
			case 0x00:	n = 8; break;
			case 0x18:	n = 9; break;
			case 0x10:	n = 9; break;
			default:	n =-1; break;
		}
	
		if ((data & 0x80) == 0x00)
			dot = '.';
		else
			dot = ' ';
	
		if (n == -1)
			buffer=sprintf("[ %c]\n", dot);
		else
			buffer=sprintf("[%d%c]\n", n, dot);
	
		logerror(buffer);
	} };
	
	
	
	/*************************************
	 *
	 *	Defender-specific routines
	 *
	 *************************************/
	
	public static InitMachinePtr defender_init_machine = new InitMachinePtr() { public void handler() 
	{
		/* standard init */
		williams_init_machine.handler();
	
		/* make sure the banking is reset to 0 */
		defender_bank_select_w.handler(0, 0);
		cpu_setbank(1, williams_videoram);
	} };
	
	
	
	public static WriteHandlerPtr defender_bank_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int bank_offset = defender_bank_list.read(data & 7);
	
		/* set bank address */
		cpu_setbank(2, new UBytePtr(memory_region(REGION_CPU1), bank_offset));
	
		/* if the bank maps into normal RAM, it represents I/O space */
		if (bank_offset < 0x10000)
		{
			memory_set_bankhandler_r(2, 0, defender_io_r);
			memory_set_bankhandler_w(2, 0, defender_io_w);
		}
	
		/* otherwise, it's ROM space */
		else
		{
			memory_set_bankhandler_r(2, 0, MRA_BANK2);
			memory_set_bankhandler_w(2, 0, MWA_ROM);
		}
	} };
	
	
	public static ReadHandlerPtr defender_input_port_0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int keys, altkeys;
	
		/* read the standard keys and the cheat keys */
		keys = readinputport(0);
		altkeys = readinputport(3);
	
		/* modify the standard keys with the cheat keys */
		if (altkeys != 0)
		{
			keys |= altkeys;
			if (memory_region(REGION_CPU1).read(0xa0bb) == 0xfd)
			{
				if ((keys & 0x02) != 0)
					keys = (keys & 0xfd) | 0x40;
				else if ((keys & 0x40) != 0)
					keys = (keys & 0xbf) | 0x02;
			}
		}
	
		return keys;
	} };
	
	
	public static ReadHandlerPtr defender_io_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* PIAs */
		if (offset >= 0x0c00 && offset < 0x0c04)
			return pia_1_r.handler(offset & 3);
		else if (offset >= 0x0c04 && offset < 0x0c08)
			return pia_0_r.handler(offset & 3);
	
		/* video counter */
		else if (offset == 0x800)
			return williams_video_counter_r.handler(offset);
	
		/* If not bank 0 then return banked RAM */
		return defender_bank_base.read(offset);
	} };
	
	
	public static WriteHandlerPtr defender_io_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* write the data through */
		defender_bank_base.write(offset, data);
	
		/* watchdog */
		if (offset == 0x03fc)
			watchdog_reset_w.handler(offset, data);
	
		/* palette */
		else if (offset < 0x10)
			paletteram_BBGGGRRR_w.handler(offset, data);
	
		/* PIAs */
		else if (offset >= 0x0c00 && offset < 0x0c04)
			pia_1_w.handler(offset & 3, data);
		else if (offset >= 0x0c04 && offset < 0x0c08)
			pia_0_w.handler(offset & 3, data);
	} };
	
	
	
	/*************************************
	 *
	 *	Mayday-specific routines
	 *
	 *************************************/
	
	public static ReadHandlerPtr mayday_protection_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* Mayday does some kind of protection check that is not currently understood  */
		/* However, the results of that protection check are stored at $a190 and $a191 */
		/* These are compared against $a193 and $a194, respectively. Thus, to prevent  */
		/* the protection from resetting the machine, we just return $a193 for $a190,  */
		/* and $a194 for $a191. */
		return mayday_protection.read(offset + 3);
	} };
	
	
	
	/*************************************
	 *
	 *	Stargate-specific routines
	 *
	 *************************************/
	
	public static ReadHandlerPtr stargate_input_port_0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int keys, altkeys;
	
		/* read the standard keys and the cheat keys */
		keys = input_port_0_r.handler(0);
		altkeys = input_port_3_r.handler(0);
	
		/* modify the standard keys with the cheat keys */
		if (altkeys != 0)
		{
			keys |= altkeys;
			if (memory_region(REGION_CPU1).read(0x9c92) == 0xfd)
			{
				if ((keys & 0x02) != 0)
					keys = (keys & 0xfd) | 0x40;
				else if ((keys & 0x40) != 0)
					keys = (keys & 0xbf) | 0x02;
			}
		}
	
		return keys;
	} };
	
	
	
	/*************************************
	 *
	 *	Blaster-specific routines
	 *
	 *************************************/
	
	static int blaster_bank_offset[] =
	{
		0x00000, 0x10000, 0x14000, 0x18000, 0x1c000, 0x20000, 0x24000, 0x28000,
		0x2c000, 0x30000, 0x34000, 0x38000, 0x2c000, 0x30000, 0x34000, 0x38000
	};
	
	
	public static WriteHandlerPtr blaster_vram_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
		vram_bank = data;
	
		/* non-zero banks map to RAM and the currently-selected bank */
		if (vram_bank != 0)
		{
			cpu_setbank(1, new UBytePtr(RAM, blaster_bank_offset[blaster_bank]));
			cpu_setbank(2, new UBytePtr(williams_bank_base, 0x4000));
		}
	
		/* bank 0 maps to videoram */
		else
		{
			cpu_setbank(1, williams_videoram);
			cpu_setbank(2, new UBytePtr(williams_videoram, 0x4000));
		}
	} };
	
	
	public static WriteHandlerPtr blaster_bank_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
		blaster_bank = data & 15;
	
		/* only need to change anything if we're not pointing to VRAM */
		if (vram_bank != 0)
		{
			cpu_setbank(1, new UBytePtr(RAM, blaster_bank_offset[blaster_bank]));
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Lotto Fun-specific routines
	 *
	 *************************************/
	
	public static ReadHandlerPtr lottofun_input_port_0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* merge in the ticket dispenser status */
		return input_port_0_r.handler(offset) | ticket_dispenser_r.handler(offset);
	} };
	
	
	
	/*************************************
	 *
	 *	Turkey Shoot-specific routines
	 *
	 *************************************/
	
	public static ReadHandlerPtr tshoot_input_port_0_3_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* merge in the gun inputs with the standard data */
		int data = williams_input_port_0_3_r.handler(offset);
		int gun = (data & 0x3f) ^ ((data & 0x3f) >> 1);
		return (data & 0xc0) | gun;
	} };
	
	
	public static WriteHandlerPtr tshoot_maxvol_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* something to do with the sound volume */
		logerror("tshoot maxvol = %d (pc:%x)\n", data, cpu_get_pc());
	} };
	
	
	public static WriteHandlerPtr tshoot_lamp_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* set the grenade lamp */
		/*TODO*///set_led_status(0,data & 0x04);
	
		/* set the gun lamp */
		/*TODO*///set_led_status(1,data & 0x08);
	
	/*TODO*///#if 0
	/*TODO*///	/* gun coil */
	/*TODO*///	if (data & 0x10)
	/*TODO*///		printf("[gun coil] ");
	/*TODO*///	else
	/*TODO*///		printf("           ");
	/*TODO*///
	/*TODO*///	/* feather coil */
	/*TODO*///	if (data & 0x20)
	/*TODO*///		printf("[feather coil] ");
	/*TODO*///	else
	/*TODO*///		printf("               ");
	/*TODO*///
	/*TODO*///	printf("\n");
	/*TODO*///#endif
	} };
	
	
	
	/*************************************
	 *
	 *	Joust 2-specific routines
	 *
	 *************************************/
	
	public static InitMachinePtr joust2_init_machine = new InitMachinePtr() { public void handler() 
	{
		/* standard init */
		williams2_init_machine.handler();
	
		/* make sure sound board starts out in the reset state */
		/*TODO*///williams_cvsd_init(2, 3);
		pia_reset();
	} };
	
	
	static timer_callback joust2_deferred_snd_cmd_w = new timer_callback() {
            public void handler(int param) {
                pia_2_porta_w.handler(0, param & 0xff);
            }
        };
	
	public static WriteHandlerPtr joust2_pia_3_cb1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		joust2_current_sound_data = (joust2_current_sound_data & ~0x100) | ((data << 8) & 0x100);
		pia_3_cb1_w.handler(offset, data);
	} };
	
	
	public static WriteHandlerPtr joust2_snd_cmd_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		joust2_current_sound_data = (joust2_current_sound_data & ~0xff) | (data & 0xff);
		/*TODO*///williams_cvsd_data_w(joust2_current_sound_data);
		timer_set(TIME_NOW, joust2_current_sound_data, joust2_deferred_snd_cmd_w);
	} };

        /*************************************
	 *
	 *	Generic old-Williams PIA interfaces
	 *
	 *************************************/
	
	/* Generic PIA 0, maps to input ports 0 and 1 */
	public static pia6821_interface williams_pia_0_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ input_port_0_r, input_port_1_r, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, null, null, null,
		/*irqs   : A/B             */ null, null
        );
	
	/* Generic muxing PIA 0, maps to input ports 0/3 and 1; port select is CB2 */
	public static pia6821_interface williams_muxed_pia_0_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ williams_input_port_0_3_r, input_port_1_r, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, null, null, williams_port_select_w,
		/*irqs   : A/B             */ null, null
        );
	
	/* Generic dual muxing PIA 0, maps to input ports 0/3 and 1/4; port select is CB2 */
	public static pia6821_interface williams_dual_muxed_pia_0_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ williams_input_port_0_3_r, williams_input_port_1_4_r, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, null, null, williams_port_select_w,
		/*irqs   : A/B             */ null, null
	);
	
	/* Generic 49-way joystick PIA 0 for Sinistar/Blaster */
	public static pia6821_interface williams_49way_pia_0_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ williams_49way_port_0_r, input_port_1_r, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, null, null, null,
		/*irqs   : A/B             */ null, null
	);
	
	/* Generic PIA 1, maps to input port 2, sound command out, and IRQs */
	public static pia6821_interface williams_pia_1_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ input_port_2_r, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, williams_snd_cmd_w, null, null,
		/*irqs   : A/B             */ williams_main_irq, williams_main_irq
	);
	
	/* Generic PIA 2, maps to DAC data in and sound IRQs */
	public static pia6821_interface williams_snd_pia_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ null, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ DAC_0_data_w, null, null, null,
		/*irqs   : A/B             */ williams_snd_irq, williams_snd_irq
	);
        
        /*************************************
	 *
	 *	Game-specific old-Williams PIA interfaces
	 *
	 *************************************/
	
	/* Special PIA 0 for Defender, to handle the controls */
	public static pia6821_interface defender_pia_0_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ defender_input_port_0_r, input_port_1_r, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, null, null, null,
		/*irqs   : A/B             */ null, null
	);
	
	/* Special PIA 0 for Stargate, to handle the controls */
	public static pia6821_interface stargate_pia_0_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ stargate_input_port_0_r, input_port_1_r, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, null, null, null,
		/*irqs   : A/B             */ null, null
	);
	
	/* Special PIA 0 for Lotto Fun, to handle the controls and ticket dispenser */
	public static pia6821_interface lottofun_pia_0_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ lottofun_input_port_0_r, input_port_1_r, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, ticket_dispenser_w, null, null,
		/*irqs   : A/B             */ null, null
	);
	
	/* Special PIA 2 for Sinistar, to handle the CVSD */
	/*TODO*///public static pia6821_interface sinistar_snd_pia_intf = new pia6821_interface
        /*TODO*///(
	/*TODO*///	/*inputs : A/B,CA/B1,CA/B2 */ null, null, null, null, null, null,
	/*TODO*///	/*outputs: A/B,CA/B2       */ DAC_0_data_w, null, hc55516_0_digit_w, hc55516_0_clock_w,
	/*TODO*///	/*irqs   : A/B             */ williams_snd_irq, williams_snd_irq
	/*TODO*///);
	
	
	
	/*************************************
	 *
	 *	Generic later-Williams PIA interfaces
	 *
	 *************************************/
	
	/* Generic muxing PIA 0, maps to input ports 0/3 and 1; port select is CA2 */
	public static pia6821_interface williams2_muxed_pia_0_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ williams_input_port_0_3_r, input_port_1_r, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, null, williams_port_select_w, null,
		/*irqs   : A/B             */ null, null
	);
	
	/* Generic PIA 1, maps to input port 2, sound command out, and IRQs */
	public static pia6821_interface williams2_pia_1_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ input_port_2_r, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, williams2_snd_cmd_w, null, pia_2_ca1_w,
		/*irqs   : A/B             */ williams_main_irq, williams_main_irq
	);
	
	/* Generic PIA 2, maps to DAC data in and sound IRQs */
	public static pia6821_interface williams2_snd_pia_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ null, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ pia_1_portb_w, DAC_0_data_w, pia_1_cb1_w, null,
		/*irqs   : A/B             */ williams_snd_irq, williams_snd_irq
	);
	
	
	
	/*************************************
	 *
	 *	Game-specific later-Williams PIA interfaces
	 *
	 *************************************/
	
	/* Mystic Marathon PIA 0 */
	public static pia6821_interface mysticm_pia_0_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ input_port_0_r, input_port_1_r, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, null, null, null,
		/*irqs   : A/B             */ williams_main_firq, williams_main_irq
	);
	
	/* Turkey Shoot PIA 0 */
	public static pia6821_interface tshoot_pia_0_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ tshoot_input_port_0_3_r, input_port_1_r, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, tshoot_lamp_w, williams_port_select_w, null,
		/*irqs   : A/B             */ williams_main_irq, williams_main_irq
	);
	
	/* Turkey Shoot PIA 2 */
	public static pia6821_interface tshoot_snd_pia_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ null, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ pia_1_portb_w, DAC_0_data_w, pia_1_cb1_w, tshoot_maxvol_w,
		/*irqs   : A/B             */ williams_snd_irq, williams_snd_irq
	);
	
	/* Joust 2 PIA 1 */
	public static pia6821_interface joust2_pia_1_intf = new pia6821_interface
        (
		/*inputs : A/B,CA/B1,CA/B2 */ input_port_2_r, null, null, null, null, null,
		/*outputs: A/B,CA/B2       */ null, joust2_snd_cmd_w, joust2_pia_3_cb1_w, pia_2_ca1_w,
		/*irqs   : A/B             */ williams_main_irq, williams_main_irq
	);
}
