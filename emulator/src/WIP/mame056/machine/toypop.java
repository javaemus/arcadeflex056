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

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.inptport.*;

public class toypop
{
	
	public static UBytePtr toypop_sound_sharedram=new UBytePtr(), toypop_m68000_sharedram=new UBytePtr(), toypop_customio=new UBytePtr();
	static int interrupt_enable_mainCPU, interrupt_enable_sound, interrupt_enable_68k;
	// variables used by the coinage of Libble Rabble
	static int credits, coinsA, coinsB;
	static int coinageA[][] = {{1,1},{2,1},{1,3},{3,1},{1,2},{2,3},{1,6},{3,2}};
	static int coinageB[][] = {{1,1},{1,7},{1,5},{2,1}};
	
	public static InitMachinePtr toypop_init_machine = new InitMachinePtr() { public void handler() 
	{
		credits = coinsA = coinsB = 0;
		interrupt_enable_mainCPU = 0;
		interrupt_enable_sound = 0;
		interrupt_enable_68k = 0;
	} };
	
	public static ReadHandlerPtr toypop_sound_sharedram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* to speed up emulation, we check for the loop the sound CPU sits in most of the time
		   and end the current iteration (things will start going again with the next IRQ) */
		if ((offset == (0xa1 - 0x40)) && (toypop_sound_sharedram.read(offset) == 0) && (cpu_get_pc() == 0xe4df))
			cpu_spinuntil_int();
		return toypop_sound_sharedram.read(offset);
	} };
	
	public static WriteHandlerPtr toypop_sound_sharedram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		toypop_sound_sharedram.write(offset, data);
	} };
	
	public static ReadHandlerPtr toypop_m68000_sharedram_r  = new ReadHandlerPtr() {
            public int handler(int offset) {
                return toypop_m68000_sharedram.read(offset);
            }
        };
	
	public static WriteHandlerPtr toypop_m68000_sharedram_w  = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*TODO*///if (ACCESSING_LSB)
			toypop_m68000_sharedram.write(offset, data & 0xff);
	}};
	
	public static WriteHandlerPtr toypop_main_interrupt_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		interrupt_enable_mainCPU = 1;
	} };
	
	public static WriteHandlerPtr toypop_main_interrupt_disable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		interrupt_enable_mainCPU = 0;
	} };
	
	public static WriteHandlerPtr toypop_sound_interrupt_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		interrupt_enable_sound = 1;
	} };
	
	public static WriteHandlerPtr toypop_sound_interrupt_disable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		interrupt_enable_sound = 0;
	} };
	
	public static WriteHandlerPtr toypop_m68000_interrupt_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		interrupt_enable_68k = 1;
	}};
	
	public static WriteHandlerPtr toypop_m68000_interrupt_disable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		interrupt_enable_68k = 0;
	}};
	
	public static InterruptPtr toypop_main_interrupt = new InterruptPtr() { public int handler() 
	{
		if (interrupt_enable_mainCPU != 0)
			return interrupt.handler();
		else
			return ignore_interrupt.handler();
	} };
	
	public static InterruptPtr toypop_sound_interrupt = new InterruptPtr() { public int handler() 
	{
		if (interrupt_enable_sound != 0)
			return interrupt.handler();
		else
			return ignore_interrupt.handler();
	} };
	
	public static InterruptPtr toypop_m68000_interrupt = new InterruptPtr() { public int handler() 
	{
		/*TODO*///if (interrupt_enable_68k != 0)
		/*TODO*///	return MC68000_IRQ_6;
		/*TODO*///else
			return ignore_interrupt.handler();
	} };
	
	public static ReadHandlerPtr toypop_customio_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int mode = toypop_customio.read(8);
	
		/* mode 5 values are actually checked against these numbers during power up */
		if (mode == 5)
			switch (offset) {
				case 2:
					return 15;
				case 6:
					return 12;
				case 16:
					return 6;
				case 17:
					return 9;
				case 32:
					return 6;
				case 33:
					return 9;
				default:
					return toypop_customio.read(offset);
			}
		else
			switch (offset) {
				case 4:
					return readinputport(0) & 0x0f;
				case 5:
					return readinputport(0) >> 4;
				case 6:
					return readinputport(1) & 0x0f;
				case 7:
					return readinputport(1) >> 4;
				case 16:
					return readinputport(2) & 0x0f;
				case 17:
					return readinputport(2) >> 4;
				case 18:
					return readinputport(3) & 0x0f;
				case 19:
					return readinputport(3) >> 4;
				default:
					return toypop_customio.read(offset);
			}
	} };
	
	public static ReadHandlerPtr liblrabl_customio_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int lastcoin=0, laststart=0;
		int val, tmp, mode = toypop_customio.read(24);
	
		/* mode 7 values are actually checked against these numbers during power up */
		if (mode == 7)
			switch (offset) {
				case 2:
					return 15;
				case 6:
					return 12;
				case 18:
					return 14;
				case 39:
					return 6;
				default:
					return toypop_customio.read(offset);
			}
		else if (mode == 1)
			switch (offset) {
				case 0:		// Coin slots
					val = readinputport(3) & 0x0f;
					// bit 0 is a trigger for the coin 1 slot
					if (((val & 1) !=0) && (lastcoin & 1)==0) {
						tmp = (readinputport(1) & 0xe0) >> 5;
						coinsA++;
						if (coinsA == coinageA[tmp][0]) {
							credits += coinageA[tmp][1];
							coinsA = 0;
						}
					}
					// bit 1 is a trigger for the coin 2 slot
					if (((val & 2) !=0) && ((lastcoin & 2)==0)) {
						tmp = (readinputport(0) & 0x18) >> 3;
						coinsB++;
						if (coinsB == coinageB[tmp][0]) {
							credits += coinageB[tmp][1];
							coinsB = 0;
						}
					}
					return lastcoin = val;
				case 1:		// Start buttons
					val = readinputport(3) >> 4;
					// bit 0 is a trigger for the 1 player start
					if (((val & 1)!=0) && ((laststart & 1)==0))
						credits--;
					// bit 1 is a trigger for the 2 player start
					if (((val & 2)!=0) && ((laststart & 2)==0)) {
						if (credits >= 2)
							credits -= 2;
						else
							val &= ~2;	// otherwise you can start with no credits
					}
					return laststart = val;
				case 2:		// High BCD of credits
					return credits / 10;
				case 3:		// Low BCD of credits
					return credits % 10;
	//			case 5:		// read, but unknown
	//				return readinputport(2) >> 4;
				case 4:		// Right joystick
					return readinputport(4) >> 4;
				case 6:		// Player 2 right joystick in cocktail mode
					return readinputport(5) >> 4;
				case 16:
					return readinputport(1) >> 4;
				case 17:
					return readinputport(0) & 0x0f;
				case 18:
					return readinputport(0) >> 4;
				case 19:
					return readinputport(1) & 0x0f;
				case 34:	// Left joystick
					return readinputport(4) & 0x0f;
				case 36:	// Player 2 left joystick in cocktail mode
					return readinputport(5) & 0x0f;
				case 39:
					return readinputport(2) & 0x0f;
				default:
					return toypop_customio.read(offset);
			}
		else
			return toypop_customio.read(offset);
	} };
	
	public static WriteHandlerPtr toypop_sound_clear_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_set_reset_line(1, CLEAR_LINE);
	} };
	
	public static WriteHandlerPtr toypop_sound_assert_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_set_reset_line(1, ASSERT_LINE);
	} };
	
	public static WriteHandlerPtr toypop_m68000_clear_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_set_reset_line(2, CLEAR_LINE);
	} };
	
	public static WriteHandlerPtr toypop_m68000_assert_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_set_reset_line(2, ASSERT_LINE);
	} };
}
