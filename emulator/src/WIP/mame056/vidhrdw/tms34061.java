/****************************************************************************
 *																			*
 *	Functions to emulate the TMS34061 video controller						*
 *																			*
 *  Created by Zsolt Vasvari on 5/26/1998.									*
 *	Updated by Aaron Giles on 11/21/2000.									*
 *																			*
 *  This is far from complete. See the TMS34061 User's Guide available on	*
 *  www.spies.com/arcade													*
 *																			*
 ****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.vidhrdw.tms34061H.*;
import static arcadeflex056.fucPtr.*;

import static common.ptr.*;
import static common.libc.cstring.*;

import static mame056.common.*;
import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.cpuintrfH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.inptport.*;
import static mame056.sound.mixer.*;
import static mame056.vidhrdw.generic.*;
import static mame056.timer.*;
import static mame056.timerH.*;

// refactor
import static arcadeflex036.osdepend.logerror;


public class tms34061
{
	
	
	
	/*************************************
	 *
	 *	Internal structure
	 *
	 *************************************/
	
	public static class tms34061_data
	{
		public UBytePtr			regs = new UBytePtr(TMS34061_REGCOUNT);
		public int			xmask;
		public int			yshift;
		public int			vrammask;
		public UBytePtr			vram = new UBytePtr();
		public UBytePtr			latchram = new UBytePtr();
		public int			latchdata;
		public UBytePtr			shiftreg = new UBytePtr();
		public UBytePtr			dirty = new UBytePtr();
		public int			dirtyshift;
		public timer_entry		timer;
		public tms34061_interface       intf;
	};
	
	
	
	/*************************************
	 *
	 *	Global variables
	 *
	 *************************************/
	
	static tms34061_data tms34061=new tms34061_data();
	
	
	
	/*************************************
	 *
	 *	Hardware startup
	 *
	 *************************************/
	
	public static int tms34061_start(tms34061_interface _interface)
	{
		int temp;
	
		/* reset the data */
		tms34061 = new tms34061_data();
		tms34061.intf = _interface;
		tms34061.vrammask = tms34061.intf.vramsize - 1;
	
		/* compute the dirty shift */
		temp = tms34061.intf.dirtychunk;
		while ((temp & 1) == 0){
			tms34061.dirtyshift++;
                        temp >>= 1;
                }
	
		/* allocate memory for VRAM */
		tms34061.vram = new UBytePtr(tms34061.intf.vramsize + 256 * 2);
		if (tms34061.vram == null)
			return 1;
		memset(tms34061.vram, 0, tms34061.intf.vramsize + 256 * 2);
	
		/* allocate memory for latch RAM */
		tms34061.latchram = new UBytePtr(tms34061.intf.vramsize + 256 * 2);
		if (tms34061.latchram == null)
		{
			tms34061.vram = null;
			return 1;
		}
		memset(tms34061.latchram, 0, tms34061.intf.vramsize + 256 * 2);
	
		/* allocate memory for dirty rows */
		tms34061.dirty = new UBytePtr(1 << (20 - tms34061.dirtyshift));
		if (tms34061.dirty==null)
		{
			tms34061.latchram = null;
			tms34061.vram = null;
			return 1;
		}
		memset(tms34061.dirty, 1, 1 << (20 - tms34061.dirtyshift));
	
		/* add some buffer space for VRAM and latch RAM */
		tms34061.vram.inc(25);
		tms34061.latchram.inc(256);
	
		/* point the shift register to the base of VRAM for now */
		tms34061.shiftreg = tms34061.vram;
	
		/* initialize registers to their default values from the manual */
		tms34061.regs.write(TMS34061_HORENDSYNC, 0x0010);
		tms34061.regs.write(TMS34061_HORENDBLNK, 0x0020);
		tms34061.regs.write(TMS34061_HORSTARTBLNK, 0x01f0);
		tms34061.regs.write(TMS34061_HORTOTAL, 0x0200);
		tms34061.regs.write(TMS34061_VERENDSYNC, 0x0004);
		tms34061.regs.write(TMS34061_VERENDBLNK, 0x0010);
		tms34061.regs.write(TMS34061_VERSTARTBLNK, 0x00f0);
		tms34061.regs.write(TMS34061_VERTOTAL, 0x0100);
		tms34061.regs.write(TMS34061_DISPUPDATE, 0x0000);
		tms34061.regs.write(TMS34061_DISPSTART, 0x0000);
		tms34061.regs.write(TMS34061_VERINT, 0x0000);
		tms34061.regs.write(TMS34061_CONTROL1, 0x7000);
		tms34061.regs.write(TMS34061_CONTROL2, 0x0600);
		tms34061.regs.write(TMS34061_STATUS, 0x0000);
		tms34061.regs.write(TMS34061_XYOFFSET, 0x0010);
		tms34061.regs.write(TMS34061_XYADDRESS, 0x0000);
		tms34061.regs.write(TMS34061_DISPADDRESS, 0x0000);
		tms34061.regs.write(TMS34061_VERCOUNTER, 0x0000);
	
		/* start vertical interrupt timer */
		tms34061.timer = null;
		return 0;
	}
	
	
	
	/*************************************
	 *
	 *	Hardware shutdown
	 *
	 *************************************/
	
	public static void tms34061_stop()
	{
		/* remove buffer space for VRAM and latch RAM */
		tms34061.vram.dec( 256 );
		tms34061.latchram.dec( 256 );
	
		tms34061.dirty = null;
		tms34061.latchram = null;
		tms34061.vram = null;
	}
	
	
	
	/*************************************
	 *
	 *	Interrupt handling
	 *
	 *************************************/
	
	public static void update_interrupts()
	{
		/* if we have a callback, process it */
		if (tms34061.intf.interrupt != null)
		{
			/* if the status bit is set, and ints are enabled, turn it on */
			if ((tms34061.regs.read(TMS34061_STATUS) & 0x0001)!=0 && (tms34061.regs.read(TMS34061_CONTROL1) & 0x0400)!=0)
				(tms34061.intf.interrupt).handler(ASSERT_LINE);
			else
				(tms34061.intf.interrupt).handler(CLEAR_LINE);
		}
	}
	
	
	static timer_callback tms34061_interrupt = new timer_callback() {
            public void handler(int param) {
                /* set timer for next frame */
		tms34061.timer = timer_set(cpu_getscanlinetime(tms34061.regs.read(TMS34061_VERINT)), 0, tms34061_interrupt);
	
		/* set the interrupt bit in the status reg */
		tms34061.regs.write(TMS34061_STATUS, tms34061.regs.read(TMS34061_STATUS) | 1);
	
		/* update the interrupt state */
		update_interrupts();
            }
        };
		
	/*************************************
	 *
	 *	Register writes
	 *
	 *************************************/
	
	public static WriteHandlerPtr register_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int regnum = offset >> 2;
		int oldval = tms34061.regs.read(regnum);
	
		/* store the hi/lo half */
		if ((offset & 0x02) != 0)
			tms34061.regs.write(regnum, (tms34061.regs.read(regnum) & 0x00ff) | (data << 8));
		else
			tms34061.regs.write(regnum, (tms34061.regs.read(regnum) & 0xff00) | data);
	
		/* update the state of things */
		switch (regnum)
		{
			/* vertical interrupt: adjust the timer */
			case TMS34061_VERINT:
				if (tms34061.timer != null)
					timer_remove(tms34061.timer);
				tms34061.timer = timer_set(cpu_getscanlinetime(tms34061.regs.read(TMS34061_VERINT)), 0, tms34061_interrupt);
				break;
	
			/* XY offset: set the X and Y masks */
			case TMS34061_XYOFFSET:
				switch (tms34061.regs.read(TMS34061_XYOFFSET) & 0x00ff)
				{
					case 0x01:	tms34061.yshift = 2;	break;
					case 0x02:	tms34061.yshift = 3;	break;
					case 0x04:	tms34061.yshift = 4;	break;
					case 0x08:	tms34061.yshift = 5;	break;
					case 0x10:	tms34061.yshift = 6;	break;
					case 0x20:	tms34061.yshift = 7;	break;
					case 0x40:	tms34061.yshift = 8;	break;
					case 0x80:	tms34061.yshift = 9;	break;
					default:	logerror("Invalid value for XYOFFSET = %04x\n", tms34061.regs.read(TMS34061_XYOFFSET));	break;
				}
				tms34061.xmask = (1 << tms34061.yshift) - 1;
				break;
	
			/* CONTROL1: they could have turned interrupts on */
			case TMS34061_CONTROL1:
				update_interrupts();
				break;
	
			/* CONTROL2: they could have blanked the display */
			case TMS34061_CONTROL2:
				if (((oldval ^ tms34061.regs.read(TMS34061_CONTROL2)) & 0x2000) != 0)
					memset(tms34061.dirty, 1, 1 << (20 - tms34061.dirtyshift));
				break;
	
			/* other supported registers */
			case TMS34061_XYADDRESS:
				break;
	
			/* report all others */
			default:
				logerror("Unsupported tms34061 write. Reg #%02X=%04X - PC: %04X\n",
						regnum, tms34061.regs.read(regnum), cpu_getpreviouspc());
				break;
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Register reads
	 *
	 *************************************/
	
	public static ReadHandlerPtr register_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int regnum = offset >> 2;
		int result;
	
		/* extract the correct portion of the register */
		if ((offset & 0x02) != 0)
			result = tms34061.regs.read(regnum) >> 8;
		else
			result = tms34061.regs.read(regnum);
	
		/* special cases: */
		switch (regnum)
		{
			/* status register: a read here clears it */
			case TMS34061_STATUS:
				tms34061.regs.write(TMS34061_STATUS, 0);
				update_interrupts();
				break;
	
			/* vertical count register: return the current scanline */
			case TMS34061_VERCOUNTER:
				if ((offset & 0x02) != 0)
					result = cpu_getscanline() >> 8;
				else
					result = cpu_getscanline();
				break;
	
			/* report all others */
			default:
				logerror("Unsupported tms34061 read.  Reg #%02X      - PC: %04X\n",
						regnum, cpu_getpreviouspc());
				break;
		}
		return result;
	} };
	
	
	
	/*************************************
	 *
	 *	XY addressing
	 *
	 *************************************/
	
	public static void adjust_xyaddress(int offset)
	{
		/* note that carries are allowed if the Y coordinate isn't being modified */
		switch (offset & 0x1e)
		{
			case 0x00:	/* no change */
				break;
	
			case 0x02:	/* X + 1 */
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS)+1);
				break;
	
			case 0x04:	/* X - 1 */
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS)-1);
				break;
	
			case 0x06:	/* X = 0 */
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS)& ~tms34061.xmask);
				break;
	
			case 0x08:	/* Y + 1 */
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS)+ (1 << tms34061.yshift));
				break;
	
			case 0x0a:	/* X + 1, Y + 1 */
				tms34061.regs.write(TMS34061_XYADDRESS, (tms34061.regs.read(TMS34061_XYADDRESS) & ~tms34061.xmask) |
						((tms34061.regs.read(TMS34061_XYADDRESS) + 1) & tms34061.xmask));
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS) + (1 << tms34061.yshift));
				break;
	
			case 0x0c:	/* X - 1, Y + 1 */
				tms34061.regs.write(TMS34061_XYADDRESS, (tms34061.regs.read(TMS34061_XYADDRESS) & ~tms34061.xmask) |
						((tms34061.regs.read(TMS34061_XYADDRESS) - 1) & tms34061.xmask));
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS) + (1 << tms34061.yshift));
				break;
	
			case 0x0e:	/* X = 0, Y + 1 */
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS) & ~tms34061.xmask);
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS) + (1 << tms34061.yshift));
				break;
	
			case 0x10:	/* Y - 1 */
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS)- (1 << tms34061.yshift));
				break;
	
			case 0x12:	/* X + 1, Y - 1 */
				tms34061.regs.write(TMS34061_XYADDRESS, (tms34061.regs.read(TMS34061_XYADDRESS) & ~tms34061.xmask) |
						((tms34061.regs.read(TMS34061_XYADDRESS) + 1) & tms34061.xmask));
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS) - (1 << tms34061.yshift));
				break;
	
			case 0x14:	/* X - 1, Y - 1 */
				tms34061.regs.write(TMS34061_XYADDRESS, (tms34061.regs.read(TMS34061_XYADDRESS) & ~tms34061.xmask) |
						((tms34061.regs.read(TMS34061_XYADDRESS) - 1) & tms34061.xmask));
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS) - (1 << tms34061.yshift));
				break;
	
			case 0x16:	/* X = 0, Y - 1 */
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS) & ~tms34061.xmask);
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS) - (1 << tms34061.yshift));
				break;
	
			case 0x18:	/* Y = 0 */
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS)& tms34061.xmask);
				break;
	
			case 0x1a:	/* X + 1, Y = 0 */
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS)+1);
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS) & tms34061.xmask);
				break;
	
			case 0x1c:	/* X - 1, Y = 0 */
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS)-1);
				tms34061.regs.write(TMS34061_XYADDRESS, tms34061.regs.read(TMS34061_XYADDRESS) & tms34061.xmask);
				break;
	
			case 0x1e:	/* X = 0, Y = 0 */
				tms34061.regs.write(TMS34061_XYADDRESS, 0);
				break;
		}
	}
	
	
	public static WriteHandlerPtr xypixel_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* determine the offset, then adjust it */
		int pixeloffs = tms34061.regs.read(TMS34061_XYADDRESS);
		if (offset != 0)
			adjust_xyaddress(offset);
	
		/* adjust for the upper bits */
		pixeloffs |= (tms34061.regs.read(TMS34061_XYOFFSET) & 0x0f00) << 8;
	
		/* mask to the VRAM size */
		pixeloffs &= tms34061.vrammask;
	
		/* set the pixel data */
		if (tms34061.vram.read(pixeloffs) != data || tms34061.latchram.read(pixeloffs) != tms34061.latchdata)
		{
			tms34061.vram.write(pixeloffs, data);
			tms34061.latchram.write(pixeloffs, tms34061.latchdata);
			tms34061.dirty.write(pixeloffs >> tms34061.dirtyshift, 1);
		}
	} };
	
	
	public static ReadHandlerPtr xypixel_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* determine the offset, then adjust it */
		int pixeloffs = tms34061.regs.read(TMS34061_XYADDRESS);
		if (offset != 0)
			adjust_xyaddress(offset);
	
		/* adjust for the upper bits */
		pixeloffs |= (tms34061.regs.read(TMS34061_XYOFFSET) & 0x0f00) << 8;
	
		/* mask to the VRAM size */
		pixeloffs &= tms34061.vrammask;
	
		/* return the result */
		return tms34061.vram.read(pixeloffs);
	} };
	
	
	
	/*************************************
	 *
	 *	Core writes
	 *
	 *************************************/
	
	public static void tms34061_w(int col, int row, int func, int data)
	{
		int offs;
	
		/* the function code determines what to do */
		switch (func)
		{
			/* both 0 and 2 map to register access */
			case 0:
			case 2:
				register_w.handler(col, data);
				break;
	
			/* function 1 maps to XY access; col is the address adjustment */
			case 1:
				xypixel_w.handler(col, data);
				break;
	
			/* function 3 maps to direct access */
			case 3:
				offs = ((row << tms34061.intf.rowshift) | col) & tms34061.vrammask;
				if (tms34061.vram.read(offs) != data || tms34061.latchram.read(offs) != tms34061.latchdata)
				{
					tms34061.vram.write(offs, data);
					tms34061.latchram.write(offs, tms34061.latchdata);
					tms34061.dirty.write(offs >> tms34061.dirtyshift, 1);
				}
				break;
	
			/* function 4 performs a shift reg transfer to VRAM */
			case 4:
				offs = col << tms34061.intf.rowshift;
				if ((tms34061.regs.read(TMS34061_CONTROL2) & 0x0040) != 0)
					offs |= (tms34061.regs.read(TMS34061_CONTROL2) & 3) << 16;
				offs &= tms34061.vrammask;
	
				memcpy(new UBytePtr(tms34061.vram, offs), tms34061.shiftreg, 1 << tms34061.intf.rowshift);
				memset(new UBytePtr(tms34061.latchram, offs), tms34061.latchdata, 1 << tms34061.intf.rowshift);
				tms34061.dirty.write(offs >> tms34061.dirtyshift, 1);
				break;
	
			/* function 5 performs a shift reg transfer from VRAM */
			case 5:
				offs = col << tms34061.intf.rowshift;
				if ((tms34061.regs.read(TMS34061_CONTROL2) & 0x0040) != 0)
					offs |= (tms34061.regs.read(TMS34061_CONTROL2) & 3) << 16;
				offs &= tms34061.vrammask;
	
				tms34061.shiftreg = new UBytePtr(tms34061.vram, offs);
				break;
	
			/* log anything else */
			default:
				logerror("Unsupported TMS34061 function %d - PC: %04X\n",
						func, cpu_get_pc());
				break;
		}
	}
	
	
	public static int tms34061_r(int col, int row, int func)
	{
		int result = 0;
		int offs;
	
		/* the function code determines what to do */
		switch (func)
		{
			/* both 0 and 2 map to register access */
			case 0:
			case 2:
				result = register_r.handler(col);
				break;
	
			/* function 1 maps to XY access; col is the address adjustment */
			case 1:
				result = xypixel_r.handler(col);
				break;
	
			/* funtion 3 maps to direct access */
			case 3:
				offs = ((row << tms34061.intf.rowshift) | col) & tms34061.vrammask;
				result = tms34061.vram.read(offs);
				break;
	
			/* function 4 performs a shift reg transfer to VRAM */
			case 4:
				offs = col << tms34061.intf.rowshift;
				if ((tms34061.regs.read(TMS34061_CONTROL2) & 0x0040) != 0)
					offs |= (tms34061.regs.read(TMS34061_CONTROL2) & 3) << 16;
				offs &= tms34061.vrammask;
	
				memcpy(new UBytePtr(tms34061.vram, offs), tms34061.shiftreg, 1 << tms34061.intf.rowshift);
				memset(new UBytePtr(tms34061.latchram, offs), tms34061.latchdata, 1 << tms34061.intf.rowshift);
				tms34061.dirty.write(offs >> tms34061.dirtyshift, 1);
				break;
	
			/* function 5 performs a shift reg transfer from VRAM */
			case 5:
				offs = col << tms34061.intf.rowshift;
				if ((tms34061.regs.read(TMS34061_CONTROL2) & 0x0040) != 0)
					offs |= (tms34061.regs.read(TMS34061_CONTROL2) & 3) << 16;
				offs &= tms34061.vrammask;
	
				tms34061.shiftreg = new UBytePtr(tms34061.vram, offs);
				break;
	
			/* log anything else */
			default:
				logerror("Unsupported TMS34061 function %d - PC: %04X\n",
						func, cpu_get_pc());
				break;
		}
	
		return result;
	}
	
	
	
	/*************************************
	 *
	 *	Misc functions
	 *
	 *************************************/
	
	public static ReadHandlerPtr tms34061_latch_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return tms34061.latchdata;
	} };
	
	
	public static WriteHandlerPtr tms34061_latch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		tms34061.latchdata = data;
	} };
	
	
	public static void tms34061_get_display_state(tms34061_display state)
	{
		state.blanked = (~tms34061.regs.read(TMS34061_CONTROL2) >> 13) & 1;
		state.vram = tms34061.vram;
		state.latchram = tms34061.latchram;
		state.dirty = tms34061.dirty;
		state.regs = tms34061.regs;
	
		/* compute the display start */
		state.dispstart = tms34061.regs.read(TMS34061_DISPSTART);
	
		/* if B6 of control reg 2 is set, upper bits of display start come from B0-B1 */
		if ((tms34061.regs.read(TMS34061_CONTROL2) & 0x0040) != 0)
			state.dispstart |= (tms34061.regs.read(TMS34061_CONTROL2) & 3) << 16;
	
		/* mask to actual VRAM size */
		state.dispstart &= tms34061.vrammask;
	}
}
