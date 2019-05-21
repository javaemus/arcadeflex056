/*******************************************************************************

XX Mission (c) 1986 UPL

Video hardware driver by Uki

	31/Mar/2001 -

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

public class xxmissio
{
	
	public static UBytePtr xxmissio_fgram = new UBytePtr();
	public static int[] xxmissio_fgram_size = new int[1];
	
	static int xxmissio_xscroll,xxmissio_yscroll;
	static int flipscreen;
	static int xxmissio_bg_redraw;
	
	
	public static WriteHandlerPtr xxmissio_scroll_x_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		xxmissio_xscroll = data;
	} };
	public static WriteHandlerPtr xxmissio_scroll_y_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		xxmissio_yscroll = data;
	} };
	
	public static WriteHandlerPtr xxmissio_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if ((data & 0x01) != flipscreen)
		{
			flipscreen = data & 0x01;
			xxmissio_bg_redraw = 1;
		}
	} };
	
	public static ReadHandlerPtr xxmissio_fgram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return xxmissio_fgram.read(offset);
	} };
	public static WriteHandlerPtr xxmissio_fgram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		xxmissio_fgram.write(offset, data);
	} };
	
	public static WriteHandlerPtr xxmissio_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int offs = offset & 0x7e0;
		int x = (offset + (xxmissio_xscroll >> 3) ) & 0x1f;
		offs |= x;
	
		videoram.write(offs, data);
		dirtybuffer[offs & 0x3ff] = 1;
	} };
	public static ReadHandlerPtr xxmissio_videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int offs = offset & 0x7e0;
		int x = (offset + (xxmissio_xscroll >> 3) ) & 0x1f;
		offs |= x;
	
		return videoram.read(offs);
	} };
	
	public static WriteHandlerPtr xxmissio_paletteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (paletteram.read(offset) != data)
		{
			paletteram_BBGGRRII_w.handler(offset,data);
	
			if (offset >= 0x200)
				xxmissio_bg_redraw = 1;
		}
	} };
	
	/****************************************************************************/
	
	public static VhUpdatePtr xxmissio_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
		int chr,col;
		int x,y,px,py,fx,fy,sx,sy;
	
		int size = videoram_size[0]/2;
	
		if (xxmissio_bg_redraw==1)
			memset(dirtybuffer,1,size);
	
	/* draw BG layer */
	
		for (y=0; y<32; y++)
		{
			for (x=0; x<32; x++)
			{
				offs = y*0x20 + x;
	
				if (flipscreen!=0)
					offs = (size-1)-offs;
	
				if (dirtybuffer[offs] != 0)
				{
					dirtybuffer[offs]=0;
	
					px = x*16;
					py = y*8;
	
					chr = videoram.read( offs );
					col = videoram.read( offs + size );
					chr = chr + ((col & 0xc0) << 2 );
					col = col & 0x0f;
	
					drawgfx(tmpbitmap,Machine.gfx[2],
						chr,
						col,
						flipscreen,flipscreen,
						px,py,
						Machine.visible_area,TRANSPARENCY_NONE,0);
				}
			}
		}
	
		if (flipscreen == 0)
		{
			sx = -xxmissio_xscroll*2+12;
			sy = -xxmissio_yscroll;
		}
		else
		{
			sx = xxmissio_xscroll*2+2;
			sy = xxmissio_yscroll;
		}
	
		copyscrollbitmap(bitmap,tmpbitmap,1,new int[]{sx},1,new int[]{sy},Machine.visible_area,TRANSPARENCY_NONE,0);
		xxmissio_bg_redraw = 0;
	
	/* draw sprites */
	
		for (offs=0; offs<spriteram_size[0]; offs +=32)
		{
			chr = spriteram.read(offs);
			col = spriteram.read(offs+3);
	
			fx = ((col & 0x10) >> 4) ^ flipscreen;
			fy = ((col & 0x20) >> 5) ^ flipscreen;
	
			x = spriteram.read(offs+1)*2;
			y = spriteram.read(offs+2);
	
			chr = chr + ((col & 0x40) << 2);
			col = col & 0x07;
	
			if (flipscreen==0)
			{
				px = x-8;
				py = y;
			}
			else
			{
				px = 480-x-8;
				py = 240-y;
			}
	
			px &= 0x1ff;
	
			drawgfx(bitmap,Machine.gfx[1],
				chr,
				col,
				fx,fy,
				px,py,
				Machine.visible_area,TRANSPARENCY_PEN,0);
			if (px>0x1e0)
				drawgfx(bitmap,Machine.gfx[1],
					chr,
					col,
					fx,fy,
					px-0x200,py,
					Machine.visible_area,TRANSPARENCY_PEN,0);
	
		}
	
	
	/* draw FG layer */
	
		for (y=4; y<28; y++)
		{
			for (x=0; x<32; x++)
			{
				offs = y*32+x;
				chr = xxmissio_fgram.read(offs);
				col = xxmissio_fgram.read(offs + 0x400) & 0x07;
	
				if (flipscreen==0)
				{
					px = 16*x;
					py = 8*y;
				}
				else
				{
					px = 496-16*x;
					py = 248-8*y;
				}
	
				drawgfx(bitmap,Machine.gfx[0],
					chr,
					col,
					flipscreen,flipscreen,
					px,py,
					Machine.visible_area,TRANSPARENCY_PEN,0);
			}
		}
	
	} };
}
