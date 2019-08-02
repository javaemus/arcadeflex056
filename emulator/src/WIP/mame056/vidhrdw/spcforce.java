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
import static common.ptr.*;
import static common.libc.cstring.*;
import static mame056.cpuintrfH.*;
import static mame056.cpuintrf.*;
import static mame056.inptport.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.vidhrdw.generic.*;
import static mame056.palette.*;
// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.vidhrdw.crtc6845H.*;

public class spcforce
{
	
	
	public static UBytePtr spcforce_scrollram = new UBytePtr();
	
	
	public static WriteHandlerPtr spcforce_flip_screen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		flip_screen_set(~data & 0x01);
	} };
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr spcforce_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		/* draw the characters as sprites because they could be overlapping */
	
		fillbitmap(bitmap,Machine.pens[0],Machine.visible_area);
	
	
		for (offs = 0; offs < videoram_size[0]; offs++)
		{
			int code,sx,sy,col;
	
	
			sy = 8 * (offs / 32) -  (spcforce_scrollram.read(offs)       & 0x0f);
			sx = 8 * (offs % 32) + ((spcforce_scrollram.read(offs) >> 4) & 0x0f);
	
			code = videoram.read(offs)+ ((colorram.read(offs)& 0x01) << 8);
			col  = (~colorram.read(offs)>> 4) & 0x07;
	
			if (flip_screen() != 0)
			{
				sx = 248 - sx;
				sy = 248 - sy;
			}
	
			drawgfx(bitmap,Machine.gfx[0],
					code, col,
					flip_screen(), flip_screen(),
					sx, sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	} };
}
