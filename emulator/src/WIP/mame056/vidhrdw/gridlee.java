/***************************************************************************

	Videa Gridlee hardware

    driver by Aaron Giles

	Based on the Bally/Sente SAC system

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

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

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpuexec.*;

public class gridlee
{
	
	
	/*************************************
	 *
	 *	Globals
	 *
	 *************************************/
	
	public static int gridlee_cocktail_flip;
	
	
	
	/*************************************
	 *
	 *	Statics
	 *
	 *************************************/
	
	static UBytePtr local_videoram = new UBytePtr();
	static UBytePtr scanline_dirty = new UBytePtr();
	static UBytePtr scanline_palette = new UBytePtr();
	
	static int last_scanline_palette;
	static int screen_refresh_counter;
	static int palettebank_vis;
	
	
	
	/*************************************
	 *
	 *	Prototypes
	 *
	 *************************************/
	
	
	
	
	/*************************************
	 *
	 *	Color PROM conversion
	 *
	 *************************************/
	
	public static VhConvertColorPromPtr gridlee_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
                int _palette = 0;
                int _colortable = 0;
	
		for (i = 0; i < Machine.drv.total_colors; i++)
		{
			palette[_palette++] = (char) (color_prom.read(0x0000) | (color_prom.read(0x0000) << 4));
			palette[_palette++] = (char) (color_prom.read(0x0800) | (color_prom.read(0x0800) << 4));
			palette[_palette++] = (char) (color_prom.read(0x1000) | (color_prom.read(0x1000) << 4));
			colortable[_colortable++] = (char) i;
			color_prom.inc();
		}
            }
        };
	
	/*************************************
	 *
	 *	Video system start
	 *
	 *************************************/
	
	public static VhStartPtr gridlee_vh_start = new VhStartPtr() { public int handler() 
	{
		/* allocate a local copy of video RAM */
		local_videoram = new UBytePtr(256 * 256);
		if (local_videoram == null)
		{
			gridlee_vh_stop.handler();
			return 1;
		}
	
		/* allocate a scanline dirty array */
		scanline_dirty = new UBytePtr(256);
		if (scanline_dirty == null)
		{
			gridlee_vh_stop.handler();
			return 1;
		}
	
		/* allocate a scanline palette array */
		scanline_palette = new UBytePtr(256);
		if (scanline_palette == null)
		{
			gridlee_vh_stop.handler();
			return 1;
		}
	
		/* mark everything dirty to start */
		memset(scanline_dirty, 1, 256);
	
		/* reset the scanline palette */
		memset(scanline_palette, 0, 256);
		last_scanline_palette = 0;
		palettebank_vis = -1;
	
		return 0;
	} };
	
	
	
	/*************************************
	 *
	 *	Video system shutdown
	 *
	 *************************************/
	
	public static VhStopPtr gridlee_vh_stop = new VhStopPtr() { public void handler() 
	{
		/* free the local video RAM array */
		if (local_videoram != null)
                    local_videoram = null;
	
		/* free the scanline dirty array */
		if (scanline_dirty != null)
                    scanline_dirty = null;
	
		/* free the scanline palette array */
		if (scanline_palette != null)
                    scanline_palette = null;
	} };
	
	
	
	/*************************************
	 *
	 *	Cocktail flip
	 *
	 *************************************/
	
	public static WriteHandlerPtr gridlee_cocktail_flip_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		gridlee_cocktail_flip = data & 1;
		memset(scanline_dirty, 1, 256);
	} };
	
	
	
	/*************************************
	 *
	 *	Video RAM write
	 *
	 *************************************/
	
	public static WriteHandlerPtr gridlee_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		videoram.write(offset, data);
	
		/* expand the two pixel values into two bytes */
		local_videoram.write(offset * 2 + 0, data >> 4);
		local_videoram.write(offset * 2 + 1, data & 15);
	
		/* mark the scanline dirty */
		scanline_dirty.write(offset / 128, 1);
	} };
	
	
	
	/*************************************
	 *
	 *	Palette banking
	 *
	 *************************************/
	
	static void update_palette()
	{
		int scanline = cpu_getscanline(), i;
		if (scanline > 255) scanline = 0;
	
	logerror("update_palette: %d-%d (%02x)\n", last_scanline_palette, scanline, palettebank_vis);
	
		/* special case: the scanline is the same as last time, but a screen refresh has occurred */
		if (scanline == last_scanline_palette && (screen_refresh_counter != 0))
		{
			for (i = 0; i < 256; i++)
			{
				/* mark the scanline dirty if it was a different palette */
				if (scanline_palette.read(i) != palettebank_vis)
					scanline_dirty.write(i, 1);
				scanline_palette.write(i, palettebank_vis);
			}
		}
	
		/* fill in the scanlines up till now */
		else
		{
			for (i = last_scanline_palette; i != scanline; i = (i + 1) & 255)
			{
				/* mark the scanline dirty if it was a different palette */
				if (scanline_palette.read(i) != palettebank_vis)
					scanline_dirty.write(i, 1);
				scanline_palette.write(i, palettebank_vis);
			}
	
			/* remember where we left off */
			last_scanline_palette = scanline;
		}
	
		/* reset the screen refresh counter */
		screen_refresh_counter = 0;
	}
	
	
	public static WriteHandlerPtr gridlee_palette_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* update the scanline palette */
		update_palette();
		palettebank_vis = data & 0x3f;
	} };
	
	
	
	/*************************************
	 *
	 *	Main screen refresh
	 *
	 *************************************/
	
	public static VhUpdatePtr gridlee_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int x, y, i;
	
	logerror("-------------------------------\n");
	
		/* update the remaining scanlines */
		screen_refresh_counter++;
		update_palette();
	
		/* full refresh dirties all */
		if (full_refresh != 0)
			memset(scanline_dirty, 1, 240);
	
		/* draw any dirty scanlines from the VRAM directly */
		for (y = 0; y < 240; y++)
			if (scanline_dirty.read(y) != 0)
			{
				IntArray pens = new IntArray(Machine.pens, scanline_palette.read(y) * 32 + 16);
	
				/* non-flipped: draw directly from the bitmap */
				if (gridlee_cocktail_flip == 0)
					draw_scanline8(bitmap, 0, y, 256, new UBytePtr(local_videoram, y * 25), pens, -1);
	
				/* flipped: x-flip the scanline into a temp buffer and draw that */
				else
				{
					UBytePtr temp=new UBytePtr(256);
					int xx;
	
					for (xx = 0; xx < 256; xx++)
						temp.write(xx, local_videoram.read(y * 256 + 255 - xx));
					draw_scanline8(bitmap, 0, 239 - y, 256, temp, pens, -1);
				}
	
				scanline_dirty.write(y, 0);
			}
	
		/* draw the sprite images */
		for (i = 0; i < 32; i++)
		{
			UBytePtr sprite = new UBytePtr(spriteram, i * 4);
			UBytePtr src;
			int image = sprite.read(0);
			int ypos = sprite.read(2) + 17;
			int xpos = sprite.read(3);
	
			/* get a pointer to the source image */
			src = new UBytePtr(memory_region(REGION_GFX1), 64 * image);
	
			/* loop over y */
			for (y = 0; y < 16; y++, ypos = (ypos + 1) & 255)
			{
				if (ypos >= 16 && ypos < 240)
				{
					IntArray pens = new IntArray(Machine.pens, scanline_palette.read(ypos) * 32);
					int currx = xpos, currxor = 0;
	
					/* mark this scanline dirty */
					scanline_dirty.write(ypos, 1);
	
					/* adjust for flip */
					if (gridlee_cocktail_flip != 0)
					{
						ypos = 239 - ypos;
						currxor = 0xff;
					}
	
					/* loop over x */
					for (x = 0; x < 4; x++)
					{
						int ipixel = src.read();
                                                src.inc();
						int left = ipixel >> 4;
						int right = ipixel & 0x0f;
	
						/* left pixel */
						if (left!=0 && currx >= 0 && currx < 256)
							plot_pixel.handler(bitmap, currx ^ currxor, ypos, pens.read(left));
						currx++;
	
						/* right pixel */
						if (right!=0 && currx >= 0 && currx < 256)
							plot_pixel.handler(bitmap, currx ^ currxor, ypos, pens.read(right));
						currx++;
					}
	
					/* adjust for flip */
					if (gridlee_cocktail_flip != 0)
						ypos = 239 - ypos;
				}
				else
					src.inc(4);
			}
		}
	} };
}
