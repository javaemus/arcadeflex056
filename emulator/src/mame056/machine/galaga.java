/***************************************************************************

  machine.c

  Functions to emulate general aspects of the machine (RAM, ROM, interrupts,
  I/O ports)

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.machine;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;
import static common.libc.cstring.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame056.inptport.*;
import static mame056.sound.samples.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;

import static mame056.vidhrdw.generic.*;
import static mame056.vidhrdw.galaga.*;

// refactor
import static arcadeflex036.osdepend.logerror;


public class galaga
{
	
	
	public static UBytePtr galaga_sharedram = new UBytePtr();
	static int interrupt_enable_1,interrupt_enable_2,interrupt_enable_3;
	
	static timer_entry nmi_timer;
	
	
	
	public static InitMachinePtr galaga_init_machine = new InitMachinePtr() { public void handler() 
	{
		nmi_timer = null;
		galaga_halt_w.handler(0, 0);
	} };
	
	
	
	public static ReadHandlerPtr galaga_sharedram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return galaga_sharedram.read(offset);
	} };
	
	
	
	public static WriteHandlerPtr galaga_sharedram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (offset < 0x800)		/* write to video RAM */
			dirtybuffer[offset & 0x3ff] = 1;
	
		galaga_sharedram.write(offset, data);
	} };
	
	
	
	public static ReadHandlerPtr galaga_dsw_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int bit0,bit1;
	
	
		bit0 = (input_port_0_r.handler(0) >> offset) & 1;
		bit1 = (input_port_1_r.handler(0) >> offset) & 1;
	
		return bit0 | (bit1 << 1);
	} };
	
	
	
	/***************************************************************************
	
	 Emulate the custom IO chip.
	
	***************************************************************************/
	static int customio_command;
	static int mode,credits;
	static int coinpercred,credpercoin;
	static int[] customio = new int[16];
	
	
	public static WriteHandlerPtr galaga_customio_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		customio[offset] = data;
	
	logerror("%04x: custom IO offset %02x data %02x\n",cpu_get_pc(),offset,data);
	
		switch (customio_command)
		{
			case 0xa8:
				if (offset == 3 && data == 0x20)	/* total hack */
			        sample_start(0,0,0);
				break;
	
			case 0xe1:
				if (offset == 7)
				{
					coinpercred = customio[1];
					credpercoin = customio[2];
				}
				break;
		}
	} };
	
	
	public static ReadHandlerPtr galaga_customio_data_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (customio_command != 0x71)
			logerror("%04x: custom IO read offset %02x\n",cpu_get_pc(),offset);
	
		switch (customio_command)
		{
			case 0x71:	/* read input */
			case 0xb1:	/* only issued after 0xe1 (go into credit mode) */
				if (offset == 0)
				{
					if (mode != 0)	/* switch mode */
					{
						/* bit 7 is the service switch */
						return readinputport(4);
					}
					else	/* credits mode: return number of credits in BCD format */
					{
						int in;
						int coininserted=0;
	
	
						in = readinputport(4);
	
						/* check if the user inserted a coin */
						if (coinpercred > 0)
						{
							if ((in & 0x70) != 0x70 && credits < 99)
							{
								coininserted++;
								if (coininserted >= coinpercred)
								{
									credits += credpercoin;
									coininserted = 0;
								}
							}
						}
						else credits = 100;	/* free play */
	
	
						/* check for 1 player start button */
						if ((in & 0x04) == 0)
							if (credits >= 1) credits--;
	
						/* check for 2 players start button */
						if ((in & 0x08) == 0)
							if (credits >= 2) credits -= 2;
	
						return (credits / 10) * 16 + credits % 10;
					}
				}
				else if (offset == 1)
					return readinputport(2);	/* player 1 input */
				else if (offset == 2)
					return readinputport(3);	/* player 2 input */
	
				break;
		}
	
		return -1;
	} };
	
	
	public static ReadHandlerPtr galaga_customio_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return customio_command;
	} };
	
	
	public static timer_callback galaga_nmi_generate = new timer_callback() {
            public void handler(int i) {
                cpu_cause_interrupt (0, Z80_NMI_INT);
            }
        };
		
	public static WriteHandlerPtr galaga_customio_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (data != 0x10 && data != 0x71)
			logerror("%04x: custom IO command %02x\n",cpu_get_pc(),data);
	
		customio_command = data;
	
		switch (data)
		{
			case 0x10:
				if (nmi_timer != null) timer_remove (nmi_timer);
				nmi_timer = null;
				return;
	
			case 0xa1:	/* go into switch mode */
				mode = 1;
				break;
	
			case 0xe1:	/* go into credit mode */
				credits = 0;	/* this is a good time to reset the credits counter */
				mode = 0;
				break;
		}
	
		nmi_timer = timer_pulse (TIME_IN_USEC (50), 0, galaga_nmi_generate);
	} };
	
	
	
	public static WriteHandlerPtr galaga_halt_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if ((data & 1) != 1)
		{
			cpu_set_reset_line(1,CLEAR_LINE);
			cpu_set_reset_line(2,CLEAR_LINE);
		}
		else if (data == 0)
		{
			cpu_set_reset_line(1,ASSERT_LINE);
			cpu_set_reset_line(2,ASSERT_LINE);
		}
	} };
	
	
	
	public static WriteHandlerPtr galaga_interrupt_enable_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		interrupt_enable_1 = data & 1;
	} };
	
	
	
	public static InterruptPtr galaga_interrupt_1 = new InterruptPtr() { public int handler() 
	{
		galaga_vh_interrupt();	/* update the background stars position */
	
		if (interrupt_enable_1 != 0) return interrupt.handler();
		else return ignore_interrupt.handler();
	} };
	
	
	
	public static WriteHandlerPtr galaga_interrupt_enable_2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		interrupt_enable_2 = data & 1;
	} };
	
	
	
	public static InterruptPtr galaga_interrupt_2 = new InterruptPtr() { public int handler() 
	{
		if (interrupt_enable_2 != 0) return interrupt.handler();
		else return ignore_interrupt.handler();
	} };
	
	
	
	public static WriteHandlerPtr galaga_interrupt_enable_3_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		interrupt_enable_3 = ((data & 1)!=0) ? 0 : 1;
	} };
	
	
	
	public static InterruptPtr galaga_interrupt_3 = new InterruptPtr() { public int handler() 
	{
		if (interrupt_enable_3 != 0) return nmi_interrupt.handler();
		else return ignore_interrupt.handler();
	} };
}
