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
import static mame056.memoryH.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.cpuexec.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.inptport.readinputport;


public class sidearms
{
	
	
	public static UBytePtr sidearms_bg_scrollx=new UBytePtr(),sidearms_bg_scrolly=new UBytePtr();
	public static UBytePtr sidearms_bg2_scrollx=new UBytePtr(),sidearms_bg2_scrolly=new UBytePtr();
	static mame_bitmap tmpbitmap2;
	static int flipscreen;
	static int bgon,objon;
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr sidearms_vh_start = new VhStartPtr() { public int handler() 
	{
		if (generic_vh_start.handler()!= 0)
			return 1;
	
		/* create a temporary bitmap slightly larger than the screen for the background */
		if ((tmpbitmap2 = bitmap_alloc(48*8 + 32,Machine.drv.screen_height + 32)) == null)
		{
			generic_vh_stop.handler();
			return 1;
		}
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Stop the video hardware emulation.
	
	***************************************************************************/
	public static VhStopPtr sidearms_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free(tmpbitmap2);
		generic_vh_stop.handler();
	} };
	
	
	
	public static WriteHandlerPtr sidearms_c804_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bits 0 and 1 are coin counters */
		coin_counter_w.handler(0,data & 0x01);
		coin_counter_w.handler(1,data & 0x02);
	
		/* bit 4 probably resets the sound CPU */
	
		/* TODO: I don't know about the other bits (all used) */
	
		/* bit 7 flips screen */
		if (flipscreen != (data & 0x80))
		{
			flipscreen = data & 0x80;
	/* TODO: support screen flip */
	//		memset(dirtybuffer,1,c1942_backgroundram_size);
		}
	} };
	
	public static WriteHandlerPtr sidearms_gfxctrl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		objon = data & 0x01;
		bgon = data & 0x02;
	} };
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
        static int lastoffs;
        
	public static VhUpdatePtr sidearms_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs, sx, sy;
		int scrollx,scrolly;
		
	
	
		/* There is a scrolling blinking star background behind the tile */
		/* background, but I have absolutely NO IDEA how to render it. */
		/* The scroll registers have a 64 pixels resolution. */
	//#ifdef IHAVETHEBACKGROUND
		scrollx = -(sidearms_bg2_scrollx.read() & 0x3f);
		scrolly = -(sidearms_bg2_scrolly.read() & 0x3f);
	//#endif
	
	
		if (bgon!=0)
		{
			scrollx = sidearms_bg_scrollx.read(0) + 256 * sidearms_bg_scrollx.read(1) + 64;
			scrolly = sidearms_bg_scrolly.read(0) + 256 * sidearms_bg_scrolly.read(1);
			offs = 2 * (scrollx >> 5) + 0x100 * (scrolly >> 5);
			scrollx = -(scrollx & 0x1f);
			scrolly = -(scrolly & 0x1f);
	
			if (offs != lastoffs)
			{
				UBytePtr p=memory_region(REGION_GFX4);
	
	
				lastoffs = offs;
	
				/* Draw the entire background scroll */
				for (sy = 0;sy < 9;sy++)
				{
					for (sx = 0; sx < 13; sx++)
					{
						int offset;
	
	
						offset = offs + 2 * sx;
	
						/* swap bits 1-7 and 8-10 of the address to compensate for the */
						/* funny layout of the ROM data */
						offset = (offset & 0xf801) | ((offset & 0x0700) >> 7) | ((offset & 0x00fe) << 3);
	
						drawgfx(tmpbitmap2,Machine.gfx[1],
								p.read(offset) + 256 * (p.read(offset+1) & 0x01),
								(p.read(offset+1) & 0xf8) >> 3,
								p.read(offset+1) & 0x02,p.read(offset+1) & 0x04,
								32*sx,32*sy,
								null,TRANSPARENCY_NONE,0);
					}
					offs += 0x100;
				}
			}
	
		scrollx += 64;
	
		copyscrollbitmap(bitmap,tmpbitmap2,1,new int[]{scrollx},1,new int[]{scrolly},Machine.visible_area,TRANSPARENCY_NONE,0);
		}
		else fillbitmap(bitmap,Machine.pens[0],Machine.visible_area);
	
		/* Draw the sprites. */
		if (objon!=0)
		{
			for (offs = spriteram_size[0] - 32;offs >= 0;offs -= 32)
			{
				sx = spriteram.read(offs + 3) + ((spriteram.read(offs + 1) & 0x10) << 4);
				sy = spriteram.read(offs + 2);
				if (flipscreen != 0)
				{
					sx = 496 - sx;
					sy = 240 - sy;
				}
	
				drawgfx(bitmap,Machine.gfx[2],
						spriteram.read(offs) + 8 * (spriteram.read(offs + 1) & 0xe0),
						spriteram.read(offs + 1) & 0x0f,
						flipscreen,flipscreen,
						sx,sy,
						Machine.visible_area,TRANSPARENCY_PEN,15);
			}
		}
	
	
		/* draw the frontmost playfield. They are characters, but draw them as sprites */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			sx = offs % 64;
			sy = offs / 64;
	
			if (flipscreen != 0)
			{
				sx = 63 - sx;
				sy = 31 - sy;
			}
	
			drawgfx(bitmap,Machine.gfx[0],
					videoram.read(offs) + 4 * (colorram.read(offs) & 0xc0),
					colorram.read(offs) & 0x3f,
					flipscreen,flipscreen,
					8*sx,8*sy,
					Machine.visible_area,TRANSPARENCY_PEN,3);
		}
	} };
}
