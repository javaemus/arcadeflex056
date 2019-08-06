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

public class rockola
{
	
	
	public static UBytePtr rockola_videoram2 = new UBytePtr();
	public static UBytePtr rockola_characterram = new UBytePtr();
	public static UBytePtr rockola_scrollx = new UBytePtr(), rockola_scrolly = new UBytePtr();
	static char[] dirtycharacter = new char[256];
	static int charbank;
	static int backcolor;
	
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Zarzon has a different PROM layout from the others.
	
	***************************************************************************/
        public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
        
	public static void COLOR(char []colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs]) = (char) value;
        }
        
	public static VhConvertColorPromPtr rockola_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
		int _palette = 0;
	
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int bit0,bit1,bit2;
	
	
			/* red component */
                        bit0 = (color_prom.read() >> 0) & 0x01;
                        bit1 = (color_prom.read() >> 1) & 0x01;
                        bit2 = (color_prom.read() >> 2) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* green component */
                        bit0 = (color_prom.read() >> 3) & 0x01;
                        bit1 = (color_prom.read() >> 4) & 0x01;
                        bit2 = (color_prom.read() >> 5) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* blue component */
			bit0 = 0;
                        bit1 = (color_prom.read() >> 6) & 0x01;
                        bit2 = (color_prom.read() >> 7) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
	
			color_prom.inc();
		}
	
	
		backcolor = 0;	/* background color can be changed by the game */
	
		for (i = 0;i < TOTAL_COLORS(0);i++)
			COLOR(colortable,0,i, i);
	
		for (i = 0;i < TOTAL_COLORS(1);i++)
		{
			if (i % 4 == 0) COLOR(colortable,1,i, 4 * backcolor + 0x20);
			else COLOR(colortable,1,i, i + 0x20);
		}
	} };
	
	public static VhConvertColorPromPtr satansat_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
		int _palette = 0;
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int bit0,bit1,bit2;
	
	
			/* red component */
                        bit0 = (color_prom.read() >> 0) & 0x01;
                        bit1 = (color_prom.read() >> 1) & 0x01;
                        bit2 = (color_prom.read() >> 2) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* green component */
                        bit0 = (color_prom.read() >> 3) & 0x01;
                        bit1 = (color_prom.read() >> 4) & 0x01;
                        bit2 = (color_prom.read() >> 5) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* blue component */
			bit0 = 0;
                        bit1 = (color_prom.read() >> 6) & 0x01;
                        bit2 = (color_prom.read() >> 7) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
	
			color_prom.inc();
		}
	
	
		backcolor = 0;	/* background color can be changed by the game */
	
		for (i = 0;i < TOTAL_COLORS(0);i++)
			COLOR(colortable,0,i, 4 * (i % 4) + (i / 4));
	
		for (i = 0;i < TOTAL_COLORS(1);i++)
		{
			if (i % 4 == 0) COLOR(colortable,1,i, backcolor + 0x10);
			else COLOR(colortable,1,i, 4 * (i % 4) + (i / 4) + 0x10);
		}
	} };
	
	
	
	public static WriteHandlerPtr rockola_characterram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (rockola_characterram.read(offset) != data)
		{
			dirtycharacter[(offset / 8) & 0xff] = 1;
			rockola_characterram.write(offset, data);
		}
	} };
	
	
	
	public static WriteHandlerPtr rockola_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bits 0-2 select background color */
		if (backcolor != (data & 7))
		{
			int i;
	
	
			backcolor = data & 7;
	
			for (i = 0;i < 32;i += 4)
				Machine.gfx[1].colortable.write(i, Machine.pens[4 * backcolor + 0x20]);
	
			schedule_full_refresh();
		}
	
		/* bit 3 selects char bank */
		set_vh_global_attribute(new int[]{charbank},(~data & 0x08) >> 3);
                charbank = (~data & 0x08) >> 3;
	
		/* bit 7 flips screen */
		flip_screen_set(data & 0x80);
	} };
	
	
	public static WriteHandlerPtr satansat_b002_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bit 0 flips screen */
		flip_screen_set(data & 0x01);
	
		/* bit 1 enables interrups */
		/* it controls only IRQs, not NMIs. Here I am affecting both, which */
		/* is wrong. */
		interrupt_enable_w.handler(0,data & 0x02);
	
		/* other bits unused */
	} };
	
	
	
	public static WriteHandlerPtr satansat_backcolor_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bits 0-1 select background color. Other bits unused. */
		if (backcolor != (data & 3))
		{
			int i;
	
	
			backcolor = data & 3;
	
			for (i = 0;i < 16;i += 4)
				Machine.gfx[1].colortable.write(i, Machine.pens[backcolor + 0x10]);
	
			schedule_full_refresh();
		}
	} };
	
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr rockola_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		if (full_refresh != 0)
			memset(dirtybuffer,1,videoram_size[0]);
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			if (dirtybuffer[offs] != 0)
			{
				int sx,sy;
	
	
				dirtybuffer[offs] = 0;
	
				sx = offs % 32;
				sy = offs / 32;
				if (flip_screen() != 0)
				{
					sx = 31 - sx;
					sy = 31 - sy;
				}
	
				drawgfx(tmpbitmap,Machine.gfx[1],
						videoram.read(offs)+ 256 * charbank,
						(colorram.read(offs)& 0x38) >> 3,
						flip_screen(),flip_screen(),
						8*sx,8*sy,
						null,TRANSPARENCY_NONE,0);
			}
		}
	
	
		/* copy the background graphics */
		{
			int scrollx,scrolly;
	
	
			scrollx = -rockola_scrolly.read();
			scrolly = -rockola_scrollx.read();
	
			if (flip_screen() != 0)
			{
				scrollx = -scrollx;
				scrolly = -scrolly;
			}
	
			copyscrollbitmap(bitmap,tmpbitmap,1,new int[]{scrollx},1,new int[]{scrolly},Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	
	
		/* draw the frontmost playfield. They are characters, but draw them as sprites */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			int charcode;
			int sx,sy;
	
	
			charcode = rockola_videoram2.read(offs);
	
			/* decode modified characters */
			if (dirtycharacter[charcode] != 0)
			{
				decodechar(Machine.gfx[0],charcode,rockola_characterram,
						   Machine.drv.gfxdecodeinfo[0].gfxlayout);
				dirtycharacter[charcode] = 0;
			}
	
			sx = offs % 32;
			sy = offs / 32;
			if (flip_screen() != 0)
			{
				sx = 31 - sx;
				sy = 31 - sy;
			}
	
			drawgfx(bitmap,Machine.gfx[0],
					charcode,
					colorram.read(offs)& 0x07,
					flip_screen(),flip_screen(),
					8*sx,8*sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	} };
	
	
	/* Zarzon's background doesn't scroll, and the color code selection is different. */
	public static VhUpdatePtr satansat_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		if (full_refresh != 0)
			memset(dirtybuffer,1,videoram_size[0]);
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
	    for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			if (dirtybuffer[offs] != 0)
			{
		        int sx,sy;
	
	
				dirtybuffer[offs] = 0;
	
				sx = offs % 32;
				sy = offs / 32;
				if (flip_screen() != 0)
				{
					sx = 31 - sx;
					sy = 31 - sy;
				}
	
				drawgfx(tmpbitmap,Machine.gfx[1],
						videoram.read(offs),
						(colorram.read(offs)& 0x0c) >> 2,
						flip_screen(),flip_screen(),
						8*sx,8*sy,
						Machine.visible_area,TRANSPARENCY_NONE,0);
			}
	    }
	
		/* copy the temporary bitmap to the screen */
                copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	
		/* draw the frontmost playfield. They are characters, but draw them as sprites */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			int charcode;
			int sx,sy;
	
	
			charcode = rockola_videoram2.read(offs);
	
			/* decode modified characters */
			if (dirtycharacter[charcode] != 0)
			{
				decodechar(Machine.gfx[0],charcode,rockola_characterram,
						   Machine.drv.gfxdecodeinfo[0].gfxlayout);
				dirtycharacter[charcode] = 0;
			}
	
			sx = offs % 32;
			sy = offs / 32;
			if (flip_screen() != 0)
			{
				sx = 31 - sx;
				sy = 31 - sy;
			}
	
			drawgfx(bitmap,Machine.gfx[0],
					charcode,
					colorram.read(offs)& 0x03,
					flip_screen(),flip_screen(),
					8*sx,8*sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	} };
}
