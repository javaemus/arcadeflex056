/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;

import static common.ptr.*;
import common.subArrays.IntArray;
import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;
import static mame056.drivers.sauro.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class sauro
{
	
	static int scroll1;
	static int scroll2;
	
	public static WriteHandlerPtr sauro_scroll1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll1 = data;
	} };
	
	
	static int scroll2_map     [] = {2, 1, 4, 3, 6, 5, 0, 7};
	static int scroll2_map_flip[] = {0, 7, 2, 1, 4, 3, 6, 5};
	
	public static WriteHandlerPtr sauro_scroll2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int[] map = (flip_screen()!=0 ? scroll2_map_flip : scroll2_map);
	
		scroll2 = (data & 0xf8) | map[data & 7];
	} };
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr sauro_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs,code,sx,sy,color,flipx;
	
	
		if (full_refresh != 0)
		{
			memset(dirtybuffer,1,videoram_size[0]);
		}
	
	
		/* for every character in the backround RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = 0; offs < videoram_size[0]; offs ++)
		{
			if (dirtybuffer[offs]==0) continue;
	
			dirtybuffer[offs] = 0;
	
			code = videoram.read(offs) + ((colorram.read(offs) & 0x07) << 8);
			sx = 8 * (offs / 32);
			sy = 8 * (offs % 32);
			color = (colorram.read(offs) >> 4) & 0x0f;
	
			flipx = colorram.read(offs) & 0x08;
	
			if (flip_screen() != 0 )
			{
				flipx = flipx!=0?0:1;
				sx = 248 - sx;
				sy = 248 - sy;
			}
	
			drawgfx(tmpbitmap,Machine.gfx[1],
					code,
					color,
					flipx,flip_screen(),
					sx,sy,
					null,TRANSPARENCY_NONE,0);
		}
	
		if (flip_screen() == 0)
		{
			int scroll = -scroll1;
			copyscrollbitmap(bitmap,tmpbitmap,1,new int[]{scroll} ,0,new int[]{0},Machine.visible_area,TRANSPARENCY_NONE,0);
		}
		else
		{
			copyscrollbitmap(bitmap,tmpbitmap,1,new int[]{scroll1},0,new int[]{0},Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	
	
		/* draw the frontmost playfield. They are characters, but draw them as sprites */
		for (offs = 0; offs < videoram_size[0]; offs++)
		{
			code = sauro_videoram2.read(offs) + ((sauro_colorram2.read(offs) & 0x07) << 8);
	
			/* Skip spaces */
			if (code == 0x19) continue;
	
			sx = 8 * (offs / 32);
			sy = 8 * (offs % 32);
			color = (sauro_colorram2.read(offs) >> 4) & 0x0f;
	
			flipx = sauro_colorram2.read(offs) & 0x08;
	
			sx = (sx - scroll2) & 0xff;
	
			if (flip_screen() != 0)
			{
				flipx = flipx!=0?0:1;
				sx = 248 - sx;
				sy = 248 - sy;
			}
	
			drawgfx(bitmap,Machine.gfx[0],
					code,
					color,
					flipx,flip_screen(),
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		};
	
		/* Draw the sprites. The order is important for correct priorities */
	
		/* Weird, sprites entries don't start on DWORD boundary */
		for (offs = 3;offs < spriteram_size[0] - 1;offs += 4)
		{
			sy = spriteram.read(offs);
			if (sy == 0xf8) continue;
	
			code = spriteram.read(offs+1) + ((spriteram.read(offs+3) & 0x03) << 8);
			sx = spriteram.read(offs+2);
			sy = 236 - sy;
			color = (spriteram.read(offs+3) >> 4) & 0x0f;
	
			/* I'm not really sure how this bit works */
			if ((spriteram.read(offs+3) & 0x08) != 0)
			{
				if (sx > 0xc0)
				{
					/* Sign extend */
					sx = (int)(char)sx;
				}
			}
			else
			{
				if (sx < 0x40) continue;
			}
	
			flipx = spriteram.read(offs+3) & 0x04;
	
			if (flip_screen() != 0)
			{
				flipx = flipx!=0?0:1;
				sx = (235 - sx) & 0xff;  /* The &0xff is not 100% percent correct */
				sy = 240 - sy;
			}
	
			drawgfx(bitmap, Machine.gfx[2],
					code,
					color,
					flipx,flip_screen(),
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	} };
}
