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

import static WIP.mame056.vidhrdw.missile.missile_video_mult_w;
import static WIP.mame056.vidhrdw.missile.missile_video_r;
import static WIP.mame056.vidhrdw.missile.missile_video_w;
import static arcadeflex056.fucPtr.*;
import static mame056.common.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.cpuintrf.*;
import static mame056.inptport.*;
import static mame056.commonH.*;
import static mame056.memoryH.*;
import static mame056.sound.pokey.*;
import static mame056.sound.pokeyH.*;
import static mame056.palette.*;
// refactor
import static arcadeflex036.osdepend.logerror;
import static common.ptr.*;

public class missile
{
	
	
	static int ctrld;
	static int h_pos, v_pos;
	
	
	
	/********************************************************************************************/
	public static ReadHandlerPtr missile_IN0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (ctrld != 0)	/* trackball */
		{
			if (flip_screen() == 0)
		  	    return ((readinputport(5) << 4) & 0xf0) | (readinputport(4) & 0x0f);
			else
		  	    return ((readinputport(7) << 4) & 0xf0) | (readinputport(6) & 0x0f);
		}
		else	/* buttons */
			return (readinputport(0));
	} };
	
	
	/********************************************************************************************/
	public static InitMachinePtr missile_init_machine = new InitMachinePtr() { public void handler() 
	{
		h_pos = v_pos = 0;
	} };
	
	
	/********************************************************************************************/
	public static WriteHandlerPtr missile_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int pc, opcode;
		offset = offset + 0x640;
	
		pc = cpu_getpreviouspc();
		opcode = cpu_readop(pc);
	
		/* 3 different ways to write to video ram - the third is caught by the core memory handler */
		if (opcode == 0x81)
		{
			/* 	STA ($00,X) */
			missile_video_w.handler(offset, data);
			return;
		}
		if (offset <= 0x3fff)
		{
			missile_video_mult_w.handler(offset, data);
			return;
		}
	
		/* $4c00 - watchdog */
		if (offset == 0x4c00)
		{
			watchdog_reset_w.handler(offset, data);
			return;
		}
	
		/* $4800 - various IO */
		if (offset == 0x4800)
		{
			flip_screen_set(~data & 0x40);
			coin_counter_w.handler(0,data & 0x20);
			coin_counter_w.handler(1,data & 0x10);
			coin_counter_w.handler(2,data & 0x08);
			/*TODO*///set_led_status(0,~data & 0x02);
			/*TODO*///set_led_status(1,~data & 0x04);
			ctrld = data & 1;
			return;
		}
	
		/* $4d00 - IRQ acknowledge */
		if (offset == 0x4d00)
		{
			return;
		}
	
		/* $4000 - $400f - Pokey */
		if (offset >= 0x4000 && offset <= 0x400f)
		{
			pokey1_w.handler(offset, data);
			return;
		}
	
		/* $4b00 - $4b07 - color RAM */
		if (offset >= 0x4b00 && offset <= 0x4b07)
		{
			int r,g,b;
	
	
			r = 0xff * ((~data >> 3) & 1);
			g = 0xff * ((~data >> 2) & 1);
			b = 0xff * ((~data >> 1) & 1);
	
			palette_set_color(offset - 0x4b00,r,g,b);
	
			return;
		}
	
		logerror("possible unmapped write, offset: %04x, data: %02x\n", offset, data);
	} };
	
	
	/********************************************************************************************/
	
	public static UBytePtr missile_video2ram = new UBytePtr();
	
	public static ReadHandlerPtr missile_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int pc, opcode;
		offset = offset + 0x1900;
	
		pc = cpu_getpreviouspc();
		opcode = cpu_readop(pc);
	
		if (opcode == 0xa1)
		{
			/* 	LDA ($00,X)  */
			return (missile_video_r.handler(offset));
		}
	
		if (offset >= 0x5000)
			return missile_video2ram.read(offset - 0x5000);
	
		if (offset == 0x4800)
			return (missile_IN0_r.handler(0));
		if (offset == 0x4900)
			return (readinputport (1));
		if (offset == 0x4a00)
			return (readinputport (2));
	
		if ((offset >= 0x4000) && (offset <= 0x400f))
			return (pokey1_r.handler(offset & 0x0f));
	
		logerror("possible unmapped read, offset: %04x\n", offset);
		return 0;
	} };
}
