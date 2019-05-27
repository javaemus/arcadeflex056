/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.subArrays.IntArray;
import static common.ptr.*;
import static common.libc.cstring.*;
import static common.libc.cstdio.sprintf;
import static mame056.usrintrf.usrintf_showmessage;

import static mame056.common.*;
import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpu.m6809.m6809H.M6809_FIRQ_LINE;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.inptport.*;
import static mame056.sound.mixer.*;
import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpuintrfH.*;
import static WIP.mame056.vidhrdw.tms34061H.*;
import static WIP.mame056.vidhrdw.tms34061.*;
import static mame056.machine._6812piaH.irqfuncPtr;

public class capbowl
{
	
	public static UBytePtr capbowl_rowaddress = new UBytePtr();
	
	
	/*************************************
	 *
	 *	TMS34061 interfacing
	 *
	 *************************************/
	
	static irqfuncPtr generate_interrupt = new irqfuncPtr() {
            public void handler(int state) {
                cpu_set_irq_line(0, M6809_FIRQ_LINE, state);
            }
        };
	
	static tms34061_interface tms34061intf = new tms34061_interface
        (
		8,						/* VRAM address is (row << rowshift) | col */
		0x10000,				/* size of video RAM */
		0x100,					/* size of dirty chunks (must be power of 2) */
		generate_interrupt		/* interrupt gen callback */
	);
	
	
	
	/*************************************
	 *
	 *	Video start
	 *
	 *************************************/
	
	public static VhStartPtr capbowl_vh_start = new VhStartPtr() { public int handler() 
	{
		/* initialize TMS34061 emulation */
	    if (tms34061_start(tms34061intf) != 0)
			return 1;
	
		return 0;
	} };
	
	
	
	/*************************************
	 *
	 *	Video stop
	 *
	 *************************************/
	
	public static VhStopPtr capbowl_vh_stop = new VhStopPtr() { public void handler() 
	{
		tms34061_stop();
	} };
	
	
	/*************************************
	 *
	 *	TMS34061 I/O
	 *
	 *************************************/
	
	public static WriteHandlerPtr capbowl_tms34061_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int func = (offset >> 8) & 3;
		int col = offset & 0xff;
	
		/* Column address (CA0-CA8) is hooked up the A0-A7, with A1 being inverted
		   during register access. CA8 is ignored */
		if (func == 0 || func == 2)
			col ^= 2;
	
		/* Row address (RA0-RA8) is not dependent on the offset */
		tms34061_w(col, capbowl_rowaddress.read(), func, data);
	} };
	
	
	public static ReadHandlerPtr capbowl_tms34061_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int func = (offset >> 8) & 3;
		int col = offset & 0xff;
	
		/* Column address (CA0-CA8) is hooked up the A0-A7, with A1 being inverted
		   during register access. CA8 is ignored */
		if (func == 0 || func == 2)
			col ^= 2;
	
		/* Row address (RA0-RA8) is not dependent on the offset */
		return tms34061_r(col, capbowl_rowaddress.read(), func);
	} };
	
	
	/*************************************
	 *
	 *	Main refresh
	 *
	 *************************************/
	
	public static VhUpdatePtr capbowl_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int halfwidth = (Machine.visible_area.max_x - Machine.visible_area.min_x + 1) / 2;
		tms34061_display state=new tms34061_display();
		int x, y;
	
		/* first get the current display state */
		tms34061_get_display_state(state);
	
		/* if we're blanked, just fill with black */
		if (state.blanked != 0)
		{
			fillbitmap(bitmap, Machine.pens[0], Machine.visible_area);
			return;
		}
	
		/* update the palette and color usage */
		for (y = Machine.visible_area.min_y; y <= Machine.visible_area.max_y; y++)
			if (state.dirty.read(y) != 0)
			{
				UBytePtr src = new UBytePtr(state.vram, 256 * y);
	
				/* update the palette */
				for (x = 0; x < 16; x++)
				{
					int r = src.readinc() & 0x0f;
					int g = src.read() >> 4;
					int b = src.readinc() & 0x0f;
	
					palette_set_color(y * 16 + x, (r << 4) | r, (g << 4) | g, (b << 4) | b);
				}
			}
	
		/* now regenerate the bitmap */
		for (y = Machine.visible_area.min_y; y <= Machine.visible_area.max_y; y++)
			if (full_refresh!=0 || state.dirty.read(y)!=0)
			{
				UBytePtr src = new UBytePtr(state.vram, 256 * y + 32);
				UBytePtr scanline = new UBytePtr(400);
				UBytePtr dst = new UBytePtr(scanline);
	
				/* expand row to 8bpp */
				for (x = 0; x < halfwidth; x++)
				{
					int pix = src.readinc();
					dst.writeinc( pix >> 4 );
					dst.writeinc( pix & 0x0f );
				}
	
				/* redraw the scanline and mark it no longer dirty */
				draw_scanline8(bitmap, Machine.visible_area.min_x, y, halfwidth * 2, scanline, new IntArray(Machine.pens, 16 * y), -1);
				state.dirty.write(y, 0);
			}
	} };
}
