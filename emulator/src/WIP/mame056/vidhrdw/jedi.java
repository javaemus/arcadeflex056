/***************************************************************************

	Atari Return of the Jedi hardware

	driver by Dan Boris

***************************************************************************/

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
import common.subArrays.IntArray;

public class jedi
{
	
	
	/* globals */
	public static UBytePtr jedi_backgroundram = new UBytePtr();
	public static int[] jedi_backgroundram_size = new int[2];
	public static UBytePtr jedi_PIXIRAM = new UBytePtr();
	
	
	/* local variables */
	static int jedi_vscroll;
	static int jedi_hscroll;
	static int jedi_alpha_bank;
	static int video_off, smooth_table;
	public static UBytePtr fgdirty = new UBytePtr(), bgdirty = new UBytePtr();
	static mame_bitmap fgbitmap, mobitmap, bgbitmap, bgexbitmap;
	
	
	
	/*************************************
	 *
	 *	Video startup
	 *
	 *************************************/
	
	public static VhStartPtr jedi_vh_start = new VhStartPtr() { public int handler() 
	{
		/* allocate dirty buffer for the foreground characters */
		fgdirty = new UBytePtr(videoram_size[0]);
                dirtybuffer = new char[videoram_size[0]];
		if (fgdirty == null)
			return 1;
		memset(fgdirty, 1, videoram_size[0]);
	
		/* allocate an 8bpp bitmap for the raw foreground characters */
		fgbitmap = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height);
		if (fgbitmap == null)
		{
			fgdirty = null;
			return 1;
		}
	
		/* allocate an 8bpp bitmap for the motion objects */
		mobitmap = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height);
		if (mobitmap == null)
		{
			bitmap_free(fgbitmap);
			fgdirty = null;
			return 1;
		}
		fillbitmap(mobitmap, 0, Machine.visible_area);
	
		/* allocate dirty buffer for the background characters */
		bgdirty = new UBytePtr(jedi_backgroundram_size[0]);
		if (bgdirty == null)
		{
			bitmap_free(mobitmap);
			bitmap_free(fgbitmap);
			fgdirty = null;
			return 1;
		}
		memset(bgdirty, 1, jedi_backgroundram_size[0]);
	
		/* the background area is 256x256, doubled by the hardware*/
		bgbitmap = bitmap_alloc(256, 256);
		if (bgbitmap == null)
		{
			bitmap_free(fgbitmap);
			bitmap_free(mobitmap);
			fgdirty = null;
			bgdirty = null;
			return 1;
		}
	
		/* the expanded background area is 512x512 */
		bgexbitmap = bitmap_alloc(512, 512);
		if (bgexbitmap == null)
		{
			bitmap_free(fgbitmap);
			bitmap_free(mobitmap);
			bitmap_free(bgbitmap);
			fgdirty = null;
			bgdirty = null;
			return 1;
		}
	
		/* reserve color 1024 for black (disabled display) */
		palette_set_color(1024, 0, 0, 0);
		return 0;
	} };
	
	
	
	/*************************************
	 *
	 *	Video shutdown
	 *
	 *************************************/
	
	public static VhStopPtr jedi_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free(fgbitmap);
		bitmap_free(mobitmap);
		bitmap_free(bgbitmap);
		bitmap_free(bgexbitmap);
		fgdirty = null;
		bgdirty = null;
	} };
	
	
	
	/*************************************
	 *
	 *	Palette RAM
	 *
	 *************************************
	 *
	 *	Color RAM format
	 *	Color RAM is 1024x12
	 *
	 *	RAM address: A0..A3 = Playfield color code
	 *		A4..A7 = Motion object color code
	 *		A8..A9 = Alphanumeric color code
	 *
	 *	RAM data:
	 *		0..2 = Blue
	 *		3..5 = Green
	 *		6..8 = Blue
	 *		9..11 = Intensity
	 *
	 *	Output resistor values:
	 *		bit 0 = 22K
	 *		bit 1 = 10K
	 *		bit 2 = 4.7K
	 *
	 *************************************/
	
	public static WriteHandlerPtr jedi_paletteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    int r, g, b, bits, intensity;
	    int color;
	
		paletteram.write(offset, data);
		color = paletteram.read(offset & 0x3FF) | (paletteram.read(offset | 0x400) << 8);
	
		intensity = (color >> 9) & 7;
		bits = (color >> 6) & 7;
		r = 5 * bits * intensity;
		bits = (color >> 3) & 7;
		g = 5 * bits * intensity;
		bits = (color >> 0) & 7;
		b = 5 * bits * intensity;
	
		palette_set_color(offset & 0x3ff, r, g, b);
	} };
	
	
	
	/*************************************
	 *
	 *	Background access
	 *
	 *************************************/
	
	public static WriteHandlerPtr jedi_backgroundram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (jedi_backgroundram.read(offset) != data)
		{
			bgdirty.write(offset, 1);
			jedi_backgroundram.write(offset, data);
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Foreground banking
	 *
	 *************************************/
	
	public static WriteHandlerPtr jedi_alpha_banksel_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (jedi_alpha_bank != 2 * (data & 0x80))
		{
			jedi_alpha_bank = 2 * (data & 0x80);
			memset(fgdirty, 1, videoram_size[0]);
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Scroll offsets
	 *
	 *************************************/
	
	public static WriteHandlerPtr jedi_vscroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    jedi_vscroll = data | (offset << 8);
	} };
	
	
	public static WriteHandlerPtr jedi_hscroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    jedi_hscroll = data | (offset << 8);
	} };
	
	
	
	/*************************************
	 *
	 *	Video control
	 *
	 *************************************/
	
	public static WriteHandlerPtr jedi_video_off_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		video_off = data;
	} };
	
	
	public static WriteHandlerPtr jedi_PIXIRAM_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		smooth_table = data & 0x03;
		memset(bgdirty, 1, jedi_backgroundram_size[0]);
	} };
	
	
	
	/*************************************
	 *
	 *	Background smoothing
	 *
	 *************************************/
	
	static void update_smoothing(int bgtilerow, int first, int last)
	{
		UBytePtr prom = new UBytePtr(memory_region(REGION_PROMS), smooth_table * 0x100);
		int[][] bgscan = new int[2][256];
		IntArray bgcurr = new IntArray(bgscan[0]);
                IntArray bglast = new IntArray(bgscan[1]);
		int xstart, xstop, x, y;
	
		/*
			smoothing notes:
				* even scanlines blend the previous (Y-1) and current (Y) line
				* odd scanlines are just taken from the current line (Y)
				* therefore, if we modify source scanlines 8-15, we must update dest scanlines 16-32
	
				* even pixels are just taken from the current pixel (X)
				* odd pixels blend the current (X) and next (X+1) pixels
				* therefore, if we modify source pixels 8-15, we must update dest pixels 15-31
		*/
	
		/* compute x start/stop in destination coordinates */
		xstart = first * 16 - 1;
		xstop = last * 16 + 15;
	
		/* extract the previous bg scanline */
		/*TODO*///extract_scanline8(bgbitmap, 0, ((bgtilerow * 16 - 1) & 0x1ff) / 2, 256, bgcurr);
	
		/* loop over height */
		for (y = 0; y <= 16; y++)
		{
			int curry = (bgtilerow * 16 + y) & 0x1ff;
	
			/* swap background buffers */
			IntArray bgtemp = new IntArray(bgcurr);
			bgcurr = new IntArray(bglast);
			bglast = new IntArray(bgtemp);
	
			/* extract current bg scanline */
			/*TODO*///extract_scanline8(bgbitmap, 0, curry / 2, 256, bgcurr);
	
			/* loop over columns */
			for (x = xstart; x <= xstop; x++)
			{
				int tr = bglast.read(((x + 1) & 0x1ff) / 2);
				int br = bgcurr.read(((x + 1) & 0x1ff) / 2);
	
				/* smooth pixels */
				if ((x & 1) != 0)
				{
					int tl = bglast.read((x & 0x1ff) / 2);
					int bl = bgcurr.read((x & 0x1ff) / 2);
					int mixt = prom.read(16 * tl + tr);
					int mixb = prom.read(16 * bl + br);
					plot_pixel.handler(bgexbitmap, x & 0x1ff, curry, prom.read(0x400 + 16 * mixt + mixb));
				}
				else
					plot_pixel.handler(bgexbitmap, x & 0x1ff, curry, prom.read(0x400 + 16 * tr + br));
			}
		}
	}
	
	
	
	/*************************************
	 *
	 *	Core video refresh
	 *
	 *************************************/
	
	public static VhUpdatePtr jedi_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int[][] bgexdirty=new int[32][2];
		int offs;
	
	
		/* if no video, clear it all to black */
		if (video_off != 0)
		{
			fillbitmap(bitmap, Machine.pens[1024], Machine.visible_area);
			return;
		}
	
		/* Return of the Jedi has a peculiar playfield/motion object priority system. That */
		/* is, there is no priority system ;-) The color of the pixel which appears on */
		/* screen depends on all three of the foreground, background and motion objects. The */
		/* 1024 colors palette is appropriately set up by the program to "emulate" a */
		/* priority system, but it can also be used to display completely different */
		/* colors (see the palette test in service mode) */
	
	    /* update foreground bitmap as a raw bitmap*/
	    for (offs = videoram_size[0] - 1; offs >= 0; offs--)
			if (fgdirty.read(offs) != 0)
			{
				int sx = offs % 64;
				int sy = offs / 64;
	
				fgdirty.write(offs, 0);
	
				drawgfx(fgbitmap, Machine.gfx[0], videoram.read(offs) + jedi_alpha_bank,
						0, 0, 0, 8*sx, 8*sy, Machine.visible_area, TRANSPARENCY_NONE_RAW, 0);
			}
	
		/* reset the expanded dirty array */
		for (offs = 0; offs < 32; offs++)
			bgexdirty[offs][0] = bgexdirty[offs][1] = -1;
	
	    /* update background bitmap as a raw bitmap */
		for (offs = jedi_backgroundram_size[0] / 2 - 1; offs >= 0; offs--)
			if (bgdirty.read(offs)!=0 || bgdirty.read(offs + 0x400)!=0)
			{
				int sx = offs % 32;
				int sy = offs / 32;
				int code = (jedi_backgroundram.read(offs) & 0xFF);
				int bank = (jedi_backgroundram.read(offs + 0x400) & 0x0F);
	
				/* shuffle the bank bits in */
				code |= (bank & 0x01) << 8;
				code |= (bank & 0x08) << 6;
				code |= (bank & 0x02) << 9;
	
				bgdirty.write(offs, 0);
                                bgdirty.write(offs + 0x400, 0);
	
				/* update expanded dirty status (assumes we go right-to-left) */
				if (bgexdirty[sy][1] == -1)
					bgexdirty[sy][1] = sx;
				bgexdirty[sy][0] = sx;
	
				drawgfx(bgbitmap, Machine.gfx[1], code,
						0, bank & 0x04, 0, 8*sx, 8*sy, null, TRANSPARENCY_NONE_RAW, 0);
			}
	
		/* update smoothed version of background */
		for (offs = 0; offs < 32; offs++)
			if (bgexdirty[offs][1] != -1)
				update_smoothing(offs, bgexdirty[offs][0], bgexdirty[offs][1]);
	
		/* draw the motion objects */
	    for (offs = 0; offs < 0x30; offs++)
		{
			/* coordinates adjustments made to match screenshot */
			int x = spriteram.read(offs + 0x100) + ((spriteram.read(offs + 0x40) & 0x01) << 8) - 2;
			int y = 240 - spriteram.read(offs + 0x80) + 1;
			int flipx = spriteram.read(offs + 0x40) & 0x10;
			int flipy = spriteram.read(offs + 0x40) & 0x20;
			int tall = spriteram.read(offs + 0x40) & 0x08;
			int code, bank;
	
			/* shuffle the bank bits in */
			bank  = ((spriteram.read(offs + 0x40) & 0x02) >> 1);
			bank |= ((spriteram.read(offs + 0x40) & 0x40) >> 5);
			bank |=  (spriteram.read(offs + 0x40) & 0x04);
			code = spriteram.read(offs) + (bank * 256);
	
			/* adjust for double-height */
			if (tall != 0)
				code |= 1;
	
			/* draw motion object */
			drawgfx(mobitmap, Machine.gfx[2], code,
					0, flipx, flipy, x, y, Machine.visible_area, TRANSPARENCY_PEN_RAW, 0);
	
			/* handle double-height */
			if (tall != 0)
				drawgfx(mobitmap, Machine.gfx[2], code - 1,
						0, flipx, flipy, x, y - 16, Machine.visible_area, TRANSPARENCY_PEN_RAW, 0);
	    }
	
		/* compose the three layers */
		{
			int xscroll = -jedi_hscroll;
			int yscroll = -jedi_vscroll;
			copyscrollbitmap(bitmap, bgexbitmap, 1, new int[]{xscroll}, 1, new int[]{yscroll}, Machine.visible_area, TRANSPARENCY_NONE, 0);
			copybitmap(bitmap, mobitmap, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_BLEND_RAW, 4);
			copybitmap(bitmap, fgbitmap, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_BLEND, 8);
		}
	
		/* erase the motion objects */
	    for (offs = 0; offs < 0x30; offs++)
		{
			/* coordinates adjustments made to match screenshot */
			int x = spriteram.read(offs + 0x100) + ((spriteram.read(offs + 0x40) & 0x01) << 8) - 2;
			int y = 240 - spriteram.read(offs + 0x80) + 1;
			int tall = spriteram.read(offs + 0x40) & 0x08;
			rectangle bounds = new rectangle();
	
			/* compute the bounds */
			bounds.min_x = x;
			bounds.max_x = x + 15;
			bounds.min_y = tall != 0 ? (y - 16) : y;
			bounds.max_y = y + (tall != 0 ? 31 : 15);
			fillbitmap(mobitmap, 0, bounds);
	    }
	} };
}
