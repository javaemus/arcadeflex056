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

public class popeye
{
	
	
	
	public static UBytePtr popeye_background_pos = new UBytePtr();
	public static UBytePtr popeye_palettebank=new UBytePtr();
	public static UBytePtr popeye_textram=new UBytePtr();
	
	static mame_bitmap tmpbitmap2;
	static int invertmask;
	
	public static int BGRAM_SIZE = 0x2000;
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Popeye has four color PROMS:
	  - 32x8 char palette
	  - 32x8 background palette
	  - two 256x4 sprite palette
	
	  The char and sprite PROMs are connected to the RGB output this way:
	
	  bit 7 -- 220 ohm resistor  -- BLUE (inverted)
	        -- 470 ohm resistor  -- BLUE (inverted)
	        -- 220 ohm resistor  -- GREEN (inverted)
	        -- 470 ohm resistor  -- GREEN (inverted)
	        -- 1  kohm resistor  -- GREEN (inverted)
	        -- 220 ohm resistor  -- RED (inverted)
	        -- 470 ohm resistor  -- RED (inverted)
	  bit 0 -- 1  kohm resistor  -- RED (inverted)
	
	  The background PROM is connected to the RGB output this way:
	
	  bit 7 -- 470 ohm resistor  -- BLUE (inverted)
	        -- 680 ohm resistor  -- BLUE (inverted)
	        -- 470 ohm resistor  -- GREEN (inverted)
	        -- 680 ohm resistor  -- GREEN (inverted)
	        -- 1.2kohm resistor  -- GREEN (inverted)
	        -- 470 ohm resistor  -- RED (inverted)
	        -- 680 ohm resistor  -- RED (inverted)
	  bit 0 -- 1.2kohm resistor  -- RED (inverted)
	
	  The bootleg is the same, but the outputs are not inverted.
	
	***************************************************************************/
        static int _palette = 0;
        static int _colortable = 0;
        
	public static void convert_color_prom(char[] palette, char[] colortable, UBytePtr color_prom)
	{
		int i;
                
	
		/* palette entries 0-15 are directly used by the background and changed at runtime */
		_palette += 3*16;
		color_prom.inc(32);
	
		/* characters */
		for (i = 0;i < 16;i++)
		{
			int bit0,bit1,bit2;
	
	
			/* red component */
			bit0 = ((color_prom.read() ^ invertmask) >> 0) & 0x01;
			bit1 = ((color_prom.read() ^ invertmask) >> 1) & 0x01;
			bit2 = ((color_prom.read() ^ invertmask) >> 2) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* green component */
			bit0 = ((color_prom.read() ^ invertmask) >> 3) & 0x01;
			bit1 = ((color_prom.read() ^ invertmask) >> 4) & 0x01;
			bit2 = ((color_prom.read() ^ invertmask) >> 5) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* blue component */
			bit0 = 0;
			bit1 = ((color_prom.read() ^ invertmask) >> 6) & 0x01;
			bit2 = ((color_prom.read() ^ invertmask) >> 7) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
	
			color_prom.inc();
		}
	
		color_prom.inc(16);	/* skip unused part of the PROM */
	
		/* sprites */
		for (i = 0;i < 256;i++)
		{
			int bit0,bit1,bit2;
	
	
			/* red component */
			bit0 = ((color_prom.read(0) ^ invertmask) >> 0) & 0x01;
			bit1 = ((color_prom.read(0) ^ invertmask) >> 1) & 0x01;
			bit2 = ((color_prom.read(0) ^ invertmask) >> 2) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* green component */
			bit0 = ((color_prom.read(0) ^ invertmask) >> 3) & 0x01;
			bit1 = ((color_prom.read(256) ^ invertmask) >> 0) & 0x01;
			bit2 = ((color_prom.read(256) ^ invertmask) >> 1) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* blue component */
			bit0 = 0;
			bit1 = ((color_prom.read(256) ^ invertmask) >> 2) & 0x01;
			bit2 = ((color_prom.read(256) ^ invertmask) >> 3) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
	
			color_prom.inc();
		}
	
	
		/* palette entries 0-15 are directly used by the background */
	
		for (i = 0;i < 16;i++)	/* characters */
		{
			colortable[_colortable++] = 0;	/* since chars are transparent, the PROM only */
							/* stores the non transparent color */
			colortable[_colortable++] = (char) (i + 16);
		}
		for (i = 0;i < 256;i++)	/* sprites */
		{
			colortable[_colortable++] = (char) (i + 16+16);
		}
	}
	
	public static VhConvertColorPromPtr popeye_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                invertmask = 0xff;
	
		convert_color_prom(palette,colortable,color_prom);
            }
        };
		
	public static VhConvertColorPromPtr popeyebl_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                invertmask = 0x00;
	
		convert_color_prom(palette,colortable,color_prom);
            }
        };
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	public static VhStartPtr popeye_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((tmpbitmap2 = bitmap_alloc(512,512)) == null)
			return 1;
	
		return 0;
	} };
	
	public static VhStopPtr popeye_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free(tmpbitmap2);
	} };
	
	
	
	public static WriteHandlerPtr popeye_bitmap_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int sx,sy,x,y,colour;
	
		sx = 8 * (offset % 64);
		sy = 4 * (offset / 64);
	
		colour = Machine.pens[data & 0x0f];
		for (y = 0; y < 4; y++)
		{
			for (x = 0; x < 8; x++)
			{
				plot_pixel.handler(tmpbitmap2, sx+x, sy+y, colour);
			}
		}
	} };
	
	public static WriteHandlerPtr popeyebl_bitmap_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		offset = ((offset & 0xfc0) << 1) | (offset & 0x03f);
		if ((data & 0x80) != 0)
			offset |= 0x40;
	
		popeye_bitmap_w.handler(offset,data);
	} };
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	
	static void set_background_palette(int bank)
	{
		int i;
		UBytePtr color_prom = new UBytePtr( memory_region(REGION_PROMS), 16 * bank);
                
	
		for (i = 0;i < 16;i++)
		{
			int bit0,bit1,bit2;
			int r,g,b;
	
			/* red component */
			bit0 = ((color_prom.read() ^ invertmask) >> 0) & 0x01;
			bit1 = ((color_prom.read() ^ invertmask) >> 1) & 0x01;
			bit2 = ((color_prom.read() ^ invertmask) >> 2) & 0x01;
			r = 0x1c * bit0 + 0x31 * bit1 + 0x47 * bit2;
			/* green component */
			bit0 = ((color_prom.read() ^ invertmask) >> 3) & 0x01;
			bit1 = ((color_prom.read() ^ invertmask) >> 4) & 0x01;
			bit2 = ((color_prom.read() ^ invertmask) >> 5) & 0x01;
			g = 0x1c * bit0 + 0x31 * bit1 + 0x47 * bit2;
			/* blue component */
			bit0 = 0;
			bit1 = ((color_prom.read() ^ invertmask) >> 6) & 0x01;
			bit2 = ((color_prom.read() ^ invertmask) >> 7) & 0x01;
			b = 0x1c * bit0 + 0x31 * bit1 + 0x47 * bit2;
	
			palette_set_color(i,r,g,b);
	
			color_prom.inc();
		}
	}
	
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
		for (offs = 0;offs < spriteram_size[0];offs += 4)
		{
			int code,color;
	
			/*
			 * offs+3:
			 * bit 7 ?
			 * bit 6 ?
			 * bit 5 ?
			 * bit 4 MSB of sprite code
			 * bit 3 vertical flip
			 * bit 2 sprite bank
			 * bit 1 \ color (with bit 2 as well)
			 * bit 0 /
			 */
	
			code = (spriteram.read(offs + 2) & 0x7f) + ((spriteram.read(offs + 3) & 0x10) << 3)
								+ ((spriteram.read(offs + 3) & 0x04) << 6);
			color = (spriteram.read(offs + 3) & 0x07) + 8*(popeye_palettebank.read() & 0x07);
	
			if (spriteram.read(offs) != 0)
				drawgfx(bitmap,Machine.gfx[1],
						code ^ 0x1ff,
						color,
						spriteram.read(offs + 2) & 0x80,spriteram.read(offs + 3) & 0x08,
						2*(spriteram.read(offs))-8,2*(256-spriteram.read(offs + 1)),
						Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	}
	
	public static VhUpdatePtr popeye_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		set_background_palette((popeye_palettebank.read() & 0x08) >> 3);
	
		if (popeye_background_pos.read(0) != 0)	/* no background */
		{
			fillbitmap(bitmap,Machine.pens[0],Machine.visible_area);
		}
		else
		{
			/* copy the background graphics */
	
	       	int scrollx = 199 - popeye_background_pos.read(0);	/* ??? */
	        int scrolly = 2 * (256 - popeye_background_pos.read(1));
			copyscrollbitmap(bitmap,tmpbitmap2,1,new int[]{scrollx},1,new int[]{scrolly},Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	
	
		draw_sprites(bitmap);
	
	
		for (offs = 0;offs < 0x400;offs++)
		{
			int sx,sy;
	
			sx = 16 * (offs % 32);
			sy = 16 * (offs / 32);
	
			drawgfx(bitmap,Machine.gfx[0],
					popeye_textram.read(offs),
					popeye_textram.read(offs + 0x400),
					0,0,
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	} };
}
