/****************************************************************************
 *	Twin Cobra																*
 *	Communications and memory functions between shared CPU memory spaces	*
 ****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.common.*;
import static mame056.cpuintrfH.*;
import static mame056.timerH.*;

import static WIP.mame056.vidhrdw.twincobr.*;
import static mame056.vidhrdw.generic.*;
import static mame056.palette.*;
// refactor
import static arcadeflex036.osdepend.logerror;

public class twincobr
{
	
/*TODO*///	#define LOG_DSP_CALLS 0
/*TODO*///	#define CLEAR  0
/*TODO*///	#define ASSERT 1
/*TODO*///	
/*TODO*///	
/*TODO*///	
        public static UShortPtr twincobr_68k_dsp_ram = new UShortPtr();
/*TODO*///	data8_t  *twincobr_sharedram;
	public static UBytePtr  wardner_mainram = new UBytePtr();
/*TODO*///	
/*TODO*///	
/*TODO*///	
	public static int dsp_execute;
	public static int dsp_addr_w, main_ram_seg;
	static int toaplan_main_cpu;	/* Main CPU type.  0 = 68000, 1 = Z80 */
/*TODO*///	#if LOG_DSP_CALLS
/*TODO*///	static char *toaplan_cpu_type[2] = { "68K"   , "Z80" };
/*TODO*///	static int  toaplan_port_type[2] = { 0x7800c , 0x5c  };
/*TODO*///	#endif
/*TODO*///	
	public static int twincobr_intenable;
/*TODO*///	int fsharkbt_8741;
/*TODO*///	
/*TODO*///	
/*TODO*///	void fsharkbt_reset_8741_mcu(void)
/*TODO*///	{
/*TODO*///		toaplan_main_cpu = 0;		/* 68000 */
/*TODO*///		twincobr_display_on = 0;
/*TODO*///		fsharkbt_8741 = -1;
/*TODO*///		twincobr_intenable = 0;
/*TODO*///		dsp_addr_w = dsp_execute = 0;
/*TODO*///		main_ram_seg = 0;
/*TODO*///	}
	
	public static InitMachinePtr wardner_reset = new InitMachinePtr() {
            public void handler() {
                toaplan_main_cpu = 1;		/* Z80 */
		twincobr_intenable = 0;
		twincobr_display_on = 1;
		dsp_addr_w = dsp_execute = 0;
		main_ram_seg = 0;
            }
        };
	
	public static ReadHandlerPtr twincobr_dsp_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                /* DSP can read data from main CPU RAM via DSP IO port 1 */
	
		int input_data = 0;
		switch (main_ram_seg) {
			case 0x30000:	input_data = twincobr_68k_dsp_ram.read(dsp_addr_w); break;
			case 0x40000:	input_data = spriteram16.read(dsp_addr_w); break;
			case 0x50000:	input_data = paletteram16.read(dsp_addr_w); break;
			case 0x7000:	input_data = wardner_mainram.read(dsp_addr_w*2) + (wardner_mainram.read(dsp_addr_w*2+1)<<8); break;
			case 0x8000:	input_data = spriteram16.read(dsp_addr_w); break;
			case 0xa000:	input_data = paletteram.read(dsp_addr_w*2)+ (paletteram.read(dsp_addr_w*2+1)<<8); break;
			default:		logerror("DSP PC:%04x Warning !!! IO reading from %08x (port 1)\n",cpu_getpreviouspc(),main_ram_seg + dsp_addr_w);
		}
/*TODO*///	#if LOG_DSP_CALLS
/*TODO*///		logerror("DSP PC:%04x IO read %04x at %08x (port 1)\n",cpu_getpreviouspc(),input_data,main_ram_seg + dsp_addr_w);
/*TODO*///	#endif
		return input_data;
            }
        };
	
/*TODO*///	READ16_HANDLER( fsharkbt_dsp_r )
/*TODO*///	{
/*TODO*///		/* IO Port 2 used by Flying Shark bootleg */
/*TODO*///		/* DSP reads data from an extra MCU (8741) at IO port 2 */
/*TODO*///		/* Port is read three times during startup. First and last data */
/*TODO*///		/*	 read must equal, but second data read must be different */
/*TODO*///		fsharkbt_8741 += 1;
/*TODO*///	#if LOG_DSP_CALLS
/*TODO*///		logerror("DSP PC:%04x IO read %04x from 8741 MCU (port 2)\n",cpu_getpreviouspc(),(fsharkbt_8741 & 0x08));
/*TODO*///	#endif
/*TODO*///		return (fsharkbt_8741 & 1);
/*TODO*///	}
	
	public static WriteHandlerPtr twincobr_dsp_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                if (offset == 0) {
			/* This sets the main CPU RAM address the DSP should */
			/*		read/write, via the DSP IO port 0 */
			/* Top three bits of data need to be shifted left 3 places */
			/*		to select which memory bank from main CPU address */
			/*		space to use */
			/* Lower thirteen bits of this data is shifted left one position */
			/*		to move it to an even address word boundary */
	
			dsp_addr_w = data & 0x1fff;
			main_ram_seg = ((data & 0xe000) << 3);
			if (toaplan_main_cpu == 1) {		/* Z80 */
				dsp_addr_w &= 0x7ff;
				if (main_ram_seg == 0x30000) main_ram_seg = 0x7000;
				if (main_ram_seg == 0x40000) main_ram_seg = 0x8000;
				if (main_ram_seg == 0x50000) main_ram_seg = 0xa000;
			}
/*TODO*///	#if LOG_DSP_CALLS
/*TODO*///			logerror("DSP PC:%04x IO write %04x (%08x) at port 0\n",cpu_getpreviouspc(),data,main_ram_seg + dsp_addr_w);
/*TODO*///	#endif
		}
		if (offset == 1) {
			/* Data written to main CPU RAM via DSP IO port 1*/
			dsp_execute = 0;
			switch (main_ram_seg) {
				case 0x30000:	twincobr_68k_dsp_ram.write(dsp_addr_w, (char) data);
								if ((dsp_addr_w < 2) && (data == 0)) dsp_execute = 1; break;
				case 0x40000:	spriteram16.write(dsp_addr_w, data); break;
				case 0x50000:	paletteram16.write(dsp_addr_w, data); break;
				case 0x7000:	wardner_mainram.write(dsp_addr_w*2, data);
								wardner_mainram.write(dsp_addr_w*2 + 1, data >> 8);
								if ((dsp_addr_w < 2) && (data == 0)) dsp_execute = 1; break;
				case 0x8000:	spriteram16.write(dsp_addr_w, data); break;
				case 0xa000:	paletteram.write(dsp_addr_w*2,data & 0xff);
								paletteram.write(dsp_addr_w*2 + 1,(data >> 8) & 0xff); break;
				default:		logerror("DSP PC:%04x Warning !!! IO writing to %08x (port 1)\n",cpu_getpreviouspc(),main_ram_seg + dsp_addr_w);
			}
/*TODO*///	#if LOG_DSP_CALLS
/*TODO*///			logerror("DSP PC:%04x IO write %04x at %08x (port 1)\n",cpu_getpreviouspc(),data,main_ram_seg + dsp_addr_w);
/*TODO*///	#endif
		}
		if (offset == 2) {
			/* Flying Shark bootleg DSP writes data to an extra MCU (8741) at IO port 2 */
/*TODO*///	#if 0
/*TODO*///			logerror("DSP PC:%04x IO write from DSP RAM:%04x to 8741 MCU (port 2)\n",cpu_getpreviouspc(),fsharkbt_8741);
/*TODO*///	#endif
		}
		if (offset == 3) {
			/* data 0xffff	means inhibit BIO line to DSP and enable  */
			/*				communication to main processor */
			/*				Actually only DSP data bit 15 controls this */
			/* data 0x0000	means set DSP BIO line active and disable */
			/*				communication to main processor*/
/*TODO*///	#if LOG_DSP_CALLS
/*TODO*///			logerror("DSP PC:%04x IO write %04x at port 3\n",cpu_getpreviouspc(),data);
/*TODO*///	#endif
/*TODO*///			if (data & 0x8000) {
/*TODO*///				cpu_set_irq_line(2, TMS320C10_ACTIVE_BIO, CLEAR_LINE);
/*TODO*///			}
			if (data == 0) {
				if (dsp_execute != 0) {
/*TODO*///	#if LOG_DSP_CALLS
/*TODO*///					logerror("Turning %s on\n",toaplan_cpu_type[toaplan_main_cpu]);
/*TODO*///	#endif
/*TODO*///					timer_suspendcpu(0, CLEAR, SUSPEND_REASON_HALT);
					dsp_execute = 0;
				}
/*TODO*///				cpu_set_irq_line(2, TMS320C10_ACTIVE_BIO, ASSERT_LINE);
			}
		}
            }
        };
	
/*TODO*///	READ16_HANDLER( twincobr_68k_dsp_r )
/*TODO*///	{
/*TODO*///		return twincobr_68k_dsp_ram[offset];
/*TODO*///	}
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( twincobr_68k_dsp_w )
/*TODO*///	{
/*TODO*///	#if LOG_DSP_CALLS
/*TODO*///		if (offset < 5) logerror("%s:%08x write %08x at %08x\n",toaplan_cpu_type[toaplan_main_cpu],cpu_get_pc(),data,0x30000+offset);
/*TODO*///	#endif
/*TODO*///		COMBINE_DATA(&twincobr_68k_dsp_ram[offset]);
/*TODO*///	}
	
	
	public static WriteHandlerPtr wardner_mainram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
/*TODO*///	#if 0
/*TODO*///		if ((offset == 4) && (data != 4)) logerror("CPU #0:%04x  Writing %02x to %04x of main RAM (DSP command number)\n",cpu_get_pc(),data, offset + 0x7000);
/*TODO*///	#endif
		wardner_mainram.write(offset, data);
	
	} };
	public static ReadHandlerPtr wardner_mainram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return wardner_mainram.read(offset);
	} };
	
	
	static void toaplan0_control_w(int offset, int data)
	{
/*TODO*///	#if 0
/*TODO*///		logerror("%s:%08x  Writing %08x to %08x.\n",toaplan_cpu_type[toaplan_main_cpu],cpu_get_pc(),data,toaplan_port_type[toaplan_main_cpu] - offset);
/*TODO*///	#endif
	
		if (toaplan_main_cpu == 1) {
			if (data == 0x0c) { data = 0x1c; wardner_sprite_hack=0; }	/* Z80 ? */
			if (data == 0x0d) { data = 0x1d; wardner_sprite_hack=1; }	/* Z80 ? */
		}
	
		switch (data) {
			case 0x0004: twincobr_intenable = 0; break;
			case 0x0005: twincobr_intenable = 1; break;
			case 0x0006: twincobr_flip_screen = 0; twincobr_flip_x_base=0x037; twincobr_flip_y_base=0x01e; break;
			case 0x0007: twincobr_flip_screen = 1; twincobr_flip_x_base=0x085; twincobr_flip_y_base=0x0f2; break;
			case 0x0008: twincobr_bg_ram_bank = 0x0000; break;
			case 0x0009: twincobr_bg_ram_bank = 0x1000; break;
			case 0x000a: twincobr_fg_rom_bank = 0x0000; break;
			case 0x000b: twincobr_fg_rom_bank = 0x1000; break;
			case 0x000e: twincobr_display_on  = 0x0000; break; /* Turn display off */
			case 0x000f: twincobr_display_on  = 0x0001; break; /* Turn display on */
			case 0x000c: if (twincobr_display_on != 0) {
							/* This means assert the INT line to the DSP */
/*TODO*///	#if LOG_DSP_CALLS
/*TODO*///							logerror("Turning DSP on and %s off\n",toaplan_cpu_type[toaplan_main_cpu]);
/*TODO*///	#endif
/*TODO*///							timer_suspendcpu(2, CLEAR, SUSPEND_REASON_HALT);
/*TODO*///							cpu_set_irq_line(2, TMS320C10_ACTIVE_INT, ASSERT_LINE);
/*TODO*///							timer_suspendcpu(0, ASSERT, SUSPEND_REASON_HALT);
						} break;
			case 0x000d: if (twincobr_display_on != 0) {
							/* This means inhibit the INT line to the DSP */
/*TODO*///	#if LOG_DSP_CALLS
/*TODO*///							logerror("Turning DSP off\n");
/*TODO*///	#endif
/*TODO*///							cpu_set_irq_line(2, TMS320C10_ACTIVE_INT, CLEAR_LINE);
/*TODO*///							timer_suspendcpu(2, ASSERT, SUSPEND_REASON_HALT);
						} break;
		}
	}
/*TODO*///	WRITE16_HANDLER( twincobr_control_w )
/*TODO*///	{
/*TODO*///		if (ACCESSING_LSB)
/*TODO*///		{
/*TODO*///			toaplan0_control_w(offset, data & 0xff);
/*TODO*///		}
/*TODO*///	}
	public static WriteHandlerPtr wardner_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		toaplan0_control_w(offset, data);
	} };
	
	
/*TODO*///	READ16_HANDLER( twincobr_sharedram_r )
/*TODO*///	{
/*TODO*///		return twincobr_sharedram[offset];
/*TODO*///	}
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( twincobr_sharedram_w )
/*TODO*///	{
/*TODO*///		if (ACCESSING_LSB)
/*TODO*///		{
/*TODO*///			twincobr_sharedram[offset] = data & 0xff;
/*TODO*///		}
/*TODO*///	}
	
	static void toaplan0_coin_dsp_w(int offset, int data)
	{
/*TODO*///	#if 0
/*TODO*///		if (data > 1)
/*TODO*///			logerror("%s:%08x  Writing %08x to %08x.\n",toaplan_cpu_type[toaplan_main_cpu],cpu_get_pc(),data,toaplan_port_type[toaplan_main_cpu] - offset);
/*TODO*///	#endif
		switch (data) {
			case 0x08: coin_counter_w.handler(0,0); break;
			case 0x09: coin_counter_w.handler(0,1); break;
			case 0x0a: coin_counter_w.handler(1,0); break;
			case 0x0b: coin_counter_w.handler(1,1); break;
			case 0x0c: coin_lockout_w(0,1); break;
			case 0x0d: coin_lockout_w(0,0); break;
			case 0x0e: coin_lockout_w(1,1); break;
			case 0x0f: coin_lockout_w(1,0); break;
			/****** The following apply to Flying Shark/Wardner only ******/
			case 0x00:	/* This means assert the INT line to the DSP */
/*TODO*///	#if LOG_DSP_CALLS
/*TODO*///						logerror("Turning DSP on and %s off\n",toaplan_cpu_type[toaplan_main_cpu]);
/*TODO*///	#endif
/*TODO*///						timer_suspendcpu(2, CLEAR, SUSPEND_REASON_HALT);
/*TODO*///						cpu_set_irq_line(2, TMS320C10_ACTIVE_INT, ASSERT_LINE);
/*TODO*///						timer_suspendcpu(0, ASSERT, SUSPEND_REASON_HALT);
						break;
			case 0x01:	/* This means inhibit the INT line to the DSP */
/*TODO*///	#if LOG_DSP_CALLS
/*TODO*///						logerror("Turning DSP off\n");
/*TODO*///	#endif
/*TODO*///						cpu_set_irq_line(2, TMS320C10_ACTIVE_INT, CLEAR_LINE);
/*TODO*///						timer_suspendcpu(2, ASSERT, SUSPEND_REASON_HALT);
						break;
		}
	}
/*TODO*///	WRITE16_HANDLER( fshark_coin_dsp_w )
/*TODO*///	{
/*TODO*///		if (ACCESSING_LSB)
/*TODO*///		{
/*TODO*///			toaplan0_coin_dsp_w(offset, data & 0xff);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	public static WriteHandlerPtr twincobr_coin_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		toaplan0_coin_dsp_w(offset, data);
/*TODO*///	} };
	public static WriteHandlerPtr wardner_coin_dsp_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		toaplan0_coin_dsp_w(offset, data);
	} };
}
