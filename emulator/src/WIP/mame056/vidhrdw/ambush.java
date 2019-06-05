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

public class ambush
{
	
	
	public static UBytePtr ambush_scrollram = new UBytePtr();
	public static UBytePtr ambush_colorbank = new UBytePtr();
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  I'm not sure about the resistor value, I'm using the Galaxian ones.
	
	***************************************************************************/
	public static VhConvertColorPromPtr ambush_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
	
		for (i = 0;i < Machine.drv.total_colors; i++)
		{
			int bit0,bit1,bit2,r,g,b;
	
			/* red component */
			bit0 = (color_prom.read(i) >> 0) & 0x01;
			bit1 = (color_prom.read(i) >> 1) & 0x01;
			bit2 = (color_prom.read(i) >> 2) & 0x01;
			r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/* green component */
			bit0 = (color_prom.read(i) >> 3) & 0x01;
			bit1 = (color_prom.read(i) >> 4) & 0x01;
			bit2 = (color_prom.read(i) >> 5) & 0x01;
			g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/* blue component */
			bit0 = 0;
			bit1 = (color_prom.read(i) >> 6) & 0x01;
			bit2 = (color_prom.read(i) >> 7) & 0x01;
			b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
	
			palette_set_color(i,r,g,b);
		}
            }
        };
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	
	static void draw_chars(mame_bitmap bitmap, int priority)
	{
		int offs, transparency;
	
	
		transparency = (priority == 0) ? TRANSPARENCY_NONE : TRANSPARENCY_PEN;
	
		for (offs = 0; offs < videoram_size[0]; offs++)
		{
			int code,sx,sy,col;
			int scroll;
	
	
			sy = (offs / 32);
			sx = (offs % 32);
	
			col = colorram.read(((sy & 0x1c) << 3) + sx);
	
			if ((col & 0x10) != priority)  continue;
	
			scroll = ~ambush_scrollram.read(sx);
	
			code = videoram.read(offs) | ((col & 0x60) << 3);
	
			if (flip_screen() != 0)
			{
				sx = 31 - sx;
				sy = 31 - sy;
				scroll = ~scroll - 1;
			}
	
			drawgfx(bitmap,Machine.gfx[0],
					code,
					(col & 0x0f) | ((ambush_colorbank.read() & 0x03) << 4),
					flip_screen(),flip_screen(),
					8*sx, (8*sy + scroll) & 0xff,
					Machine.visible_area,transparency,0);
		}
	}
	
	
	public static VhUpdatePtr ambush_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		fillbitmap(bitmap,Machine.pens[0],Machine.visible_area);
	
	
		/* Draw the background priority characters */
		draw_chars(bitmap, 0x00);
	
	
		/* Draw the sprites. */
		for (offs = spriteram_size[0] - 4;offs >= 0;offs -= 4)
		{
			int code,col,sx,sy,flipx,flipy,gfx;
	
	
			sy = spriteram.read(offs + 0);
			sx = spriteram.read(offs + 3);
	
			if ( (sy == 0) ||
				 (sy == 0xff) ||
				((sx <  0x40) && (  spriteram.read(offs + 2) & 0x10)!=0) ||
				((sx >= 0xc0) && ((spriteram.read(offs + 2) & 0x10)==0)))  continue;  /* prevent wraparound */
	
	
			code = (spriteram.read(offs + 1) & 0x3f) | ((spriteram.read(offs + 2) & 0x60) << 1);
	
			if ((spriteram.read(offs + 2) & 0x80) != 0)
			{
				/* 16x16 sprites */
				gfx = 1;
	
				if (flip_screen() == 0)
				{
					sy = 240 - sy;
				}
				else
				{
					sx = 240 - sx;
				}
			}
			else
			{
				/* 8x8 sprites */
				gfx = 0;
				code <<= 2;
	
				if (flip_screen() == 0)
				{
					sy = 248 - sy;
				}
				else
				{
					sx = 248 - sx;
				}
			}
	
			col   = spriteram.read(offs + 2) & 0x0f;
			flipx = spriteram.read(offs + 1) & 0x40;
			flipy = spriteram.read(offs + 1) & 0x80;
	
			if (flip_screen() != 0)
			{
				flipx = flipx!=0?0:1;
				flipy = flipy!=0?0:1;
			}
	
			drawgfx(bitmap,Machine.gfx[gfx],
					code, col | ((ambush_colorbank.read() & 0x03) << 4),
					flipx, flipy,
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	
	
		/* Draw the foreground priority characters */
		draw_chars(bitmap, 0x10);
	} };
}
