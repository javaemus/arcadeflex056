/*******************************************************************************

Raiders5 (c) 1985 Taito / UPL

Video hardware driver by Uki

	02/Jun/2001 -

*******************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;

import static common.ptr.*;

import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class raiders5
{
	
	public static UBytePtr raiders5_fgram = new UBytePtr();
	public static int[] raiders5_fgram_size = new int[1];
	
	static int raiders5_xscroll,raiders5_yscroll;
	static int flipscreen;
	
	
	public static WriteHandlerPtr raiders5_scroll_x_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		raiders5_xscroll = data;
	} };
	public static WriteHandlerPtr raiders5_scroll_y_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		raiders5_yscroll = data;
	} };
	
	public static WriteHandlerPtr raiders5_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		flipscreen = data & 0x01;
	} };
	
	public static ReadHandlerPtr raiders5_fgram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return raiders5_fgram.read(offset);
	} };
	public static WriteHandlerPtr raiders5_fgram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		raiders5_fgram.write(offset, data);
	} };
	
	public static WriteHandlerPtr raiders5_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int y = (offset + ((raiders5_yscroll & 0xf8) << 2) ) & 0x3e0;
		int x = (offset + (raiders5_xscroll >> 3) ) & 0x1f;
		int offs = x+y+(offset & 0x400);
	
		videoram.write(offs, data);
	} };
	public static ReadHandlerPtr raiders5_videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int y = (offset + ((raiders5_yscroll & 0xf8) << 2) ) & 0x3e0;
		int x = (offset + (raiders5_xscroll >> 3) ) & 0x1f;
		int offs = x+y+(offset & 0x400);
	
		return videoram.read(offs);
	} };
	
	public static WriteHandlerPtr raiders5_paletteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int i;
	
		paletteram_BBGGRRII_w.handler(offset,data);
	
		if (offset > 15)
			return;
	
		if (offset != 1)
		{
			for (i=0; i<16; i++)
			{
				paletteram_BBGGRRII_w.handler(0x200+offset+i*16,data);
			}
		}
		paletteram_BBGGRRII_w.handler(0x200+offset*16+1,data);
	} };
	
	/****************************************************************************/
	
	public static VhUpdatePtr raiders5_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
		int chr,col;
		int x,y,px,py,fx,fy,sx,sy;
		int b1,b2;
	
		int size = videoram_size[0]/2;
	
	/* draw BG layer */
	
		for (y=0; y<32; y++)
		{
			for (x=0; x<32; x++)
			{
				offs = y*0x20 + x;
	
				if (flipscreen!=0)
					offs = (size-1)-offs;
	
				px = x*8;
				py = y*8;
	
				chr = videoram.read( offs ) ;
				col = videoram.read( offs + size);
	
				b1 = (col >> 1) & 1; /* ? */
				b2 = col & 1;
	
				col = (col >> 4) & 0x0f;
				chr = chr | b2*0x100;
	
				drawgfx(tmpbitmap,Machine.gfx[b1+3],
					chr,
					col,
					flipscreen,flipscreen,
					px,py,
					null,TRANSPARENCY_NONE,0);
			}
		}
	
		if (flipscreen == 0)
		{
			sx = -raiders5_xscroll+7;
			sy = -raiders5_yscroll;
		}
		else
		{
			sx = raiders5_xscroll;
			sy = raiders5_yscroll;
		}
	
		copyscrollbitmap(bitmap,tmpbitmap,1,new int[]{sx},1,new int[]{sy},Machine.visible_area,TRANSPARENCY_NONE,0);
	
	/* draw sprites */
	
		for (offs=0; offs<spriteram_size[0]; offs +=32)
		{
			chr = spriteram.read(offs);
			col = spriteram.read(offs+3);
	
			b1 = (col >> 1) & 1;
			b2 = col & 0x01;
	
			fx = ((chr >> 0) & 1) ^ flipscreen;
			fy = ((chr >> 1) & 1) ^ flipscreen;
	
			x = spriteram.read(offs+1);
			y = spriteram.read(offs+2);
	
			col = (col >> 4) & 0x0f ;
			chr = (chr >> 2) | b2*0x40;
	
			if (flipscreen==0)
			{
				px = x;
				py = y;
			}
			else
			{
				px = 240-x;
				py = 240-y;
			}
	
			drawgfx(bitmap,Machine.gfx[b1],
				chr,
				col,
				fx,fy,
				px,py,
				Machine.visible_area,TRANSPARENCY_PEN,0);
	
			if (px>0xf0)
				drawgfx(bitmap,Machine.gfx[b1],
					chr,
					col,
					fx,fy,
					px-0x100,py,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	
	
	/* draw FG layer */
	
		for (y=4; y<28; y++)
		{
			for (x=0; x<32; x++)
			{
				offs = y*32+x;
				chr = raiders5_fgram.read(offs);
				col = raiders5_fgram.read(offs + 0x400) >> 4;
	
				if (flipscreen==0)
				{
					px = 8*x;
					py = 8*y;
				}
				else
				{
					px = 248-8*x;
					py = 248-8*y;
				}
	
				drawgfx(bitmap,Machine.gfx[2],
					chr,
					col,
					flipscreen,flipscreen,
					px,py,
					Machine.visible_area,TRANSPARENCY_PEN,0);
			}
		}
	} };
}
