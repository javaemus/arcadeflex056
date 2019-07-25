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
import static common.libc.cstring.memset;
import static mame056.usrintrf.*;
import static mame056.memoryH.*;
import static mame056.memory.*;

import static common.ptr.*;
import common.subArrays.IntArray;
import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.inptport.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class canyon
{
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr canyon_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
	    int offs;
	
	    /* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			if (full_refresh!=0 || dirtybuffer[offs]!=0)
			{
				int charcode;
				int sx,sy;
	
				dirtybuffer[offs]=0;
	
				charcode = videoram.read(offs)& 0x3F;
	
				sx = 8 * (offs % 32);
				sy = 8 * (offs / 32);
				drawgfx(tmpbitmap,Machine.gfx[0],
						charcode, (videoram.read(offs)& 0x80)>>7,
						0,0,sx,sy,
						Machine.visible_area,TRANSPARENCY_NONE,0);
			}
		}
	
		/* copy the character mapped graphics */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	
		for (offs=0; offs<2; offs++)
		{
			int sx, sy;
			int pic;
			int flipx;
	
			sx = 27*8 - spriteram.read(offs*2+1);
			sy = 30*8 - spriteram.read(offs*2+8);
			pic = spriteram.read(offs*2+9);
			if ((pic & 0x80) != 0)
				flipx=0;
			else
				flipx=1;
	
	        drawgfx(bitmap,Machine.gfx[1],
	                (pic & 0x18) >> 3, offs,
					flipx,0,sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	
		/* Draw the bombs as 2x2 squares */
		for (offs=2; offs<4; offs++)
		{
			int sx, sy;
	
			sx = 31*8 - spriteram.read(offs*2+1);
			sy = 31*8 - spriteram.read(offs*2+8);
	
	        drawgfx(bitmap,Machine.gfx[2],
	                0, offs,
					0,0,sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	} };
}