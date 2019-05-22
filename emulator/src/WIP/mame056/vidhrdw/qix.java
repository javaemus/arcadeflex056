/***************************************************************************

	Taito Qix hardware

	driver by John Butler, Ed Mueller, Aaron Giles

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;
import static common.libc.expressions.*;
import static common.libc.cstring.*;

import static mame056.common.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static common.libc.cstring.memset;
import common.subArrays.IntArray;
import static mame056.cpuexec.*;
import static mame056.palette.*;
import static mame056.vidhrdw.generic.*;
import static mame056.timer.*;

public class qix
{
	
	
	/* Constants */
	public static int SCANLINE_INCREMENT = 4;
	
	
	/* Globals */
	public static UBytePtr qix_palettebank = new UBytePtr();
	public static UBytePtr qix_videoaddress = new UBytePtr();
	public static int qix_cocktail_flip;
	
	
	/* Local variables */
	static int vram_mask;
	public static UBytePtr videoram_cache = new UBytePtr();
	public static UBytePtr palette_cache = new UBytePtr();
	
	
	
	/*************************************
	 *
	 *	Video startup
	 *
	 *************************************/
	
	public static VhStartPtr qix_vh_start = new VhStartPtr() { public int handler() 
	{
		/* allocate memory for the full video RAM */
		videoram = new UBytePtr(256 * 256);
		if (videoram == null)
			return 1;
	
		/* allocate memory for the cached video RAM */
		videoram_cache = new UBytePtr(256 * 256);
		if (videoram_cache == null)
		{
			videoram = null;
			return 1;
		}
	
		/* allocate memory for the cached palette banks */
		palette_cache = new UBytePtr(256);
		if (palette_cache == null)
		{
			videoram = videoram_cache = null;
			return 1;
		}
	
		/* initialize the mask for games that don't use it */
		vram_mask = 0xff;
		return 0;
	} };
	
	
	
	/*************************************
	 *
	 *	Video shutdown
	 *
	 *************************************/
	
	public static VhStopPtr qix_vh_stop = new VhStopPtr() { public void handler() 
	{
		/* free memory */
		palette_cache = null;
		videoram_cache = null;
		videoram = null;
	
		/* reset the pointers */
		videoram = videoram_cache = null;
		palette_cache = null;
	} };
	
	
	
	/*************************************
	 *
	 *	Scanline caching
	 *
	 *************************************/
	
	public static timer_callback qix_scanline_callback = new timer_callback() {
            public void handler(int scanline) {
                /* for non-zero scanlines, cache the previous data and the palette bank */
		if (scanline != 0)
		{
			int offset = (scanline - SCANLINE_INCREMENT) * 256;
			int count = SCANLINE_INCREMENT * 256;
			UBytePtr src, dst = new UBytePtr(videoram_cache, offset);
	
			/* copy the data forwards or backwards, based on the cocktail flip */
			if (qix_cocktail_flip == 0)
			{
				src = new UBytePtr(videoram, offset);
				memcpy(dst, src, count);
			}
			else
			{
				src = new UBytePtr(videoram, offset ^ 0xffff);
				while ((count--) != 0)
					dst.writeinc(src.readdec());
			}
	
			/* cache the palette bank as well */
			memset(new UBytePtr(palette_cache, scanline - SCANLINE_INCREMENT), qix_palettebank.read(), SCANLINE_INCREMENT);
		}
	
		/* set a timer for the next increment */
		scanline += SCANLINE_INCREMENT;
		if (scanline > 256)
			scanline = SCANLINE_INCREMENT;
		timer_set(cpu_getscanlinetime(scanline), scanline, qix_scanline_callback);
            }
        };
	
	
	/*************************************
	 *
	 *	Current scanline read
	 *
	 *************************************/
	
	public static ReadHandlerPtr qix_scanline_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int scanline = cpu_getscanline();
		return (scanline <= 0xff) ? scanline : 0;
	} };
	
	
	
	/*************************************
	 *
	 *	Video RAM mask
	 *
	 *************************************/
	
	public static WriteHandlerPtr slither_vram_mask_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* Slither appears to extend the basic hardware by providing */
		/* a mask register which controls which data bits get written */
		/* to video RAM */
		vram_mask = data;
	} };
	
	
	
	/*************************************
	 *
	 *	Direct video RAM read/write
	 *
	 *	The screen is 256x256 with eight
	 *	bit pixels (64K).  The screen is
	 *	divided into two halves each half
	 *	mapped by the video CPU at
	 *	$0000-$7FFF.  The high order bit
	 *	of the address latch at $9402
	 *	specifies which half of the screen
	 *	is being accessed.
	 *
	 *************************************/
	
	public static ReadHandlerPtr qix_videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* add in the upper bit of the address latch */
		offset += (qix_videoaddress.read(0) & 0x80) << 8;
		return videoram.read(offset);
	} };
	
	
	public static WriteHandlerPtr qix_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* add in the upper bit of the address latch */
		offset += (qix_videoaddress.read(0) & 0x80) << 8;
	
		/* blend the data */
		videoram.write(offset, (videoram.read(offset) & ~vram_mask) | (data & vram_mask));
	} };
	
	
	
	/*************************************
	 *
	 *	Latched video RAM read/write
	 *
	 *	The address latch works as follows.
	 *	When the video CPU accesses $9400,
	 *	the screen address is computed by
	 *	using the values at $9402 (high
	 *	byte) and $9403 (low byte) to get
	 *	a value between $0000-$FFFF.  The
	 *	value at that location is either
	 *	returned or written.
	 *
	 *************************************/
	
	public static ReadHandlerPtr qix_addresslatch_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* compute the value at the address latch */
		offset = (qix_videoaddress.read(0) << 8) | qix_videoaddress.read(1);
		return videoram.read(offset);
	} };
	
	
	
	public static WriteHandlerPtr qix_addresslatch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* compute the value at the address latch */
		offset = (qix_videoaddress.read(0) << 8) | qix_videoaddress.read(1);
	
		/* blend the data */
		videoram.write(offset, (videoram.read(offset) & ~vram_mask) | (data & vram_mask));
	} };
	
	
	
	/*************************************
	 *
	 *	Palette RAM
	 *
	 *************************************/
	
	public static WriteHandlerPtr qix_paletteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* this conversion table should be about right. It gives a reasonable */
		/* gray scale in the test screen, and the red, green and blue squares */
		/* in the same screen are barely visible, as the manual requires. */
		int table[] =
		{
			0x00,	/* value = 0, intensity = 0 */
			0x12,	/* value = 0, intensity = 1 */
			0x24,	/* value = 0, intensity = 2 */
			0x49,	/* value = 0, intensity = 3 */
			0x12,	/* value = 1, intensity = 0 */
			0x24,	/* value = 1, intensity = 1 */
			0x49,	/* value = 1, intensity = 2 */
			0x92,	/* value = 1, intensity = 3 */
			0x5b,	/* value = 2, intensity = 0 */
			0x6d,	/* value = 2, intensity = 1 */
			0x92,	/* value = 2, intensity = 2 */
			0xdb,	/* value = 2, intensity = 3 */
			0x7f,	/* value = 3, intensity = 0 */
			0x91,	/* value = 3, intensity = 1 */
			0xb6,	/* value = 3, intensity = 2 */
			0xff	/* value = 3, intensity = 3 */
		};
		int bits, intensity, red, green, blue;
	
		/* set the palette RAM value */
		paletteram.write(offset, data);
	
		/* compute R, G, B from the table */
		intensity = (data >> 0) & 0x03;
		bits = (data >> 6) & 0x03;
		red = table[(bits << 2) | intensity];
		bits = (data >> 4) & 0x03;
		green = table[(bits << 2) | intensity];
		bits = (data >> 2) & 0x03;
		blue = table[(bits << 2) | intensity];
	
		/* update the palette */
		palette_set_color(offset, red, green, blue);
	} };
	
	
	public static WriteHandlerPtr qix_palettebank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* set the bank value; this is cached per-scanline above */
		qix_palettebank.write( data );
	
		/* LEDs are in the upper 6 bits */
	} };
	
	
	
	/*************************************
	 *
	 *	Core video refresh
	 *
	 *************************************/
	
	public static VhUpdatePtr qix_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int y;
	
		/* draw the bitmap */
		for (y = 0; y < 256; y++)
		{
			IntArray pens = new IntArray(Machine.pens, (palette_cache.read(y) & 3) * 256);
			draw_scanline8(bitmap, 0, y, 256, new UBytePtr(videoram_cache, y * 256), pens, -1);
		}
	} };
}
