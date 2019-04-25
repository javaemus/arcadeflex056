/***************************************************************************

  vidhrdw/balsente.c

  Functions to emulate the video hardware of the machine.

****************************************************************************/

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
import static mame056.drawgfx.*;
import static mame056.cpuexec.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static WIP.mame056.drivers.balsente.balsente_shooter;
import static WIP.mame056.drivers.balsente.balsente_shooter_x;
import static WIP.mame056.drivers.balsente.balsente_shooter_y;

public class balsente
{
	
	
	
	/*************************************
	 *
	 *	Statics
	 *
	 *************************************/
	
	static UBytePtr local_videoram = new UBytePtr();
	static UBytePtr scanline_dirty = new UBytePtr();
	static UBytePtr scanline_palette = new UBytePtr();
	static UBytePtr sprite_data = new UBytePtr();
	
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
	 *	Video system start
	 *
	 *************************************/
	
	public static VhStartPtr balsente_vh_start = new VhStartPtr() { public int handler() 
	{
		/* reset the system */
		palettebank_vis = 0;
	
		/* allocate a local copy of video RAM */
		local_videoram = new UBytePtr(256 * 256);
		if (local_videoram == null)
		{
			balsente_vh_stop.handler();
			return 1;
		}
	
		/* allocate a scanline dirty array */
		scanline_dirty = new UBytePtr(256);
		if (scanline_dirty == null)
		{
			balsente_vh_stop.handler();
			return 1;
		}
	
		/* allocate a scanline palette array */
		scanline_palette = new UBytePtr(256);
		if (scanline_palette == null)
		{
			balsente_vh_stop.handler();
			return 1;
		}
	
		/* mark everything dirty to start */
		memset(scanline_dirty, 1, 256);
	
		/* reset the scanline palette */
		memset(scanline_palette, 0, 256);
		last_scanline_palette = 0;
	
		sprite_data = memory_region(REGION_GFX1);
	
		return 0;
	} };
	
	
	
	/*************************************
	 *
	 *	Video system shutdown
	 *
	 *************************************/
	
	public static VhStopPtr balsente_vh_stop = new VhStopPtr() { public void handler() 
	{
		/* free the local video RAM array */
		if (local_videoram != null)
			local_videoram = null;
	
		/* free the scanline dirty array */
		if (scanline_dirty != null)
                    scanline_dirty = null;
	
		/* free the scanline dirty array */
		if (scanline_palette != null)
                    scanline_palette = null;
	} };
	
	
	
	/*************************************
	 *
	 *	Video RAM write
	 *
	 *************************************/
	
	public static WriteHandlerPtr balsente_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		videoram.write(offset, data);
	
		/* expand the two pixel values into two bytes */
		local_videoram.write(offset * 2 + 0, data >> 4);
		local_videoram.write(offset * 2 + 1, data & 15);
	//System.out.println("Write data "+data);
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
	
		/* special case: the scanline is the same as last time, but a screen refresh has occurred */
		if ((scanline == last_scanline_palette) && (screen_refresh_counter != 0))
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
	
	
	public static WriteHandlerPtr balsente_palette_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* only update if changed */
		if (palettebank_vis != (data & 3))
		{
			/* update the scanline palette */
			update_palette();
			palettebank_vis = data & 3;
		}
	
		logerror("balsente_palette_select_w(%d) scanline=%d\n", data & 3, cpu_getscanline());
	} };
	
	
	
	/*************************************
	 *
	 *	Palette RAM write
	 *
	 *************************************/
	
	public static WriteHandlerPtr balsente_paletteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int r, g, b;
	
		paletteram.write(offset, data & 0x0f);
	
		r = paletteram.read((offset & ~3) + 0);
		g = paletteram.read((offset & ~3) + 1);
		b = paletteram.read((offset & ~3) + 2);
		palette_set_color(offset / 4, (r << 4) | r, (g << 4) | g, (b << 4) | b);
	} };
	
	
	
	/*************************************
	 *
	 *	Sprite drawing
	 *
	 *************************************/
	
	public static void draw_one_sprite(mame_bitmap bitmap, UBytePtr sprite)
	{
		int flags = sprite.read(0);
		int image = sprite.read(1) | ((flags & 3) << 8);
		int ypos = sprite.read(2) + 17;
		int xpos = sprite.read(3);
		UBytePtr src;
		int x, y;
	
		/* get a pointer to the source image */
		src = new UBytePtr(sprite_data,64 * image);
		if ((flags & 0x80)!=0) src.inc( 4 * 15 );
	
		/* loop over y */
		for (y = 0; y < 16; y++, ypos = (ypos + 1) & 255)
		{
			if (ypos >= 16 && ypos < 240)
			{
                                int _pens_offset = scanline_palette.read(y) * 256;
				int[] pens = Machine.pens;
				UBytePtr old = new UBytePtr(local_videoram, ypos * 256 + xpos);
				int currx = xpos;
	
				/* mark this scanline dirty */
				scanline_dirty.write(ypos, 1);
	
				/* standard case */
				if ((flags & 0x40) == 0)
				{
					/* loop over x */
					for (x = 0; x < 4; x++, old.inc(2) )
					{
						int ipixel = src.read();
                                                src.inc();
                                                
						int left = ipixel & 0xf0;
						int right = (ipixel << 4) & 0xf0;
	
						/* left pixel, combine with the background */
						if (left!=0 && currx >= 0 && currx < 256)
							plot_pixel.handler(bitmap, currx, ypos, pens[_pens_offset+(left | old.read(0))]);
						currx++;
	
						/* right pixel, combine with the background */
						if (right!=0 && currx >= 0 && currx < 256)
							plot_pixel.handler(bitmap, currx, ypos, pens[_pens_offset+(right | old.read(1))]);
						currx++;
					}
				}
	
				/* hflip case */
				else
				{
					src.inc(4);
	
					/* loop over x */
					for (x = 0; x < 4; x++, old.inc(2))
					{
						int ipixel = src.read();
                                                src.dec();
                                                
						int left = (ipixel << 4) & 0xf0;
						int right = ipixel & 0xf0;
	
						/* left pixel, combine with the background */
						if (left!=0 && currx >= 0 && currx < 256)
							plot_pixel.handler(bitmap, currx, ypos, pens[_pens_offset+(left | old.read(0))]);
						currx++;
	
						/* right pixel, combine with the background */
						if (right!=0 && currx >= 0 && currx < 256)
							plot_pixel.handler(bitmap, currx, ypos, pens[_pens_offset+(right | old.read(1))]);
						currx++;
					}
					src.inc(4);
				}
			}
			else
				src.inc(4);
			if ((flags & 0x80)!=0) src.dec(2 * 4);
		}
	}
	
	
	
	/*************************************
	 *
	 *		Main screen refresh
	 *
	 *************************************/
	
	public static VhUpdatePtr balsente_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int y, i;
	
		/* update the remaining scanlines */
		screen_refresh_counter++;
		update_palette();
	
		/* draw any dirty scanlines from the VRAM directly */
		for (y = 0; y < 240; y++)
			if (scanline_dirty.read(y) !=0 || full_refresh!=0)
			{
				int _pens_offset = scanline_palette.read(y) * 256;
                                IntArray pens = new IntArray(Machine.pens, _pens_offset);
                                
				draw_scanline8(bitmap, 0, y, 256, new UBytePtr(local_videoram,y * 256), pens, -1);
				scanline_dirty.write(y, 0);
			}
	
		/* draw the sprite images */
		for (i = 0; i < 40; i++)
			draw_one_sprite(bitmap, new UBytePtr(spriteram,(0xe0 + i * 4) & 0xff));
	
		/* draw a crosshair */
		if (balsente_shooter != 0)
		{
			int beamx = balsente_shooter_x;
			int beamy = balsente_shooter_y - 10;
	
			/*TODO*///draw_crosshair(bitmap,beamx,beamy,Machine.visible_area);
			for (y = -6; y <= 6; y++)
			{
				int yoffs = beamy + y;
				if (yoffs >= 0 && yoffs < 240)
					scanline_dirty.write(yoffs, 1);
			}
		}
	} };
}
