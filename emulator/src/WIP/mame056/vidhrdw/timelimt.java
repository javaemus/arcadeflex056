/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;
import static common.libc.cstring.*;

import static mame056.common.*;
import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;



public class timelimt
{
	
	/* globals */
	public static UBytePtr timelimt_bg_videoram = new UBytePtr();
	public static int[] timelimt_bg_videoram_size = new int[1];
	
	/* locals */
	static int scrollx, scrolly;
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Time Limit has two 32 bytes palette PROM, connected to the RGB output this
	  way:
	
	  bit 7 -- 220 ohm resistor  -- BLUE
	        -- 470 ohm resistor  -- BLUE
	        -- 220 ohm resistor  -- GREEN
	        -- 470 ohm resistor  -- GREEN
	        -- 1  kohm resistor  -- GREEN
	        -- 220 ohm resistor  -- RED
	        -- 470 ohm resistor  -- RED
	  bit 0 -- 1  kohm resistor  -- RED
	
	***************************************************************************/
	
	public static VhConvertColorPromPtr timelimt_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
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
			bit0 = (color_prom.read() >> 6) & 0x01;
			bit1 = (color_prom.read() >> 7) & 0x01;
			palette[_palette++] = (char) (0x4f * bit0 + 0xa8 * bit1);
	
			color_prom.inc();
		}
            }
        };
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	
	public static VhStartPtr timelimt_vh_start = new VhStartPtr() { public int handler() 
	{
		dirtybuffer = null;
		tmpbitmap = null;
	
		if ( ( dirtybuffer = new char[timelimt_bg_videoram_size[0]] ) == null )
			return 1;
	
		memset( dirtybuffer, 1, timelimt_bg_videoram_size[0] );
	
		if ( ( tmpbitmap = bitmap_alloc( 64*8, 32*8 ) ) == null )
		{
			dirtybuffer = null;
			return 1;
		}
	
		return 0;
	} };
	
	/***************************************************************************/
	
	public static WriteHandlerPtr timelimt_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (videoram.read(offset) != data)
		{
			videoram.write(offset, data);
		}
	} };
	
	public static WriteHandlerPtr timelimt_bg_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (timelimt_bg_videoram.read(offset) != data)
		{
			timelimt_bg_videoram.write(offset, data);
			dirtybuffer[offset] = 1;
		}
	} };
	
	public static WriteHandlerPtr timelimt_scroll_x_lsb_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scrollx &= 0x100;
		scrollx |= data & 0xff;
	} };
	
	public static WriteHandlerPtr timelimt_scroll_x_msb_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scrollx &= 0xff;
		scrollx |= ( data & 1 ) << 8;
	} };
	
	public static WriteHandlerPtr timelimt_scroll_y_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scrolly = data;
	} };
	
	/***************************************************************************
	
		Draw the sprites
	
	***************************************************************************/
	static void drawsprites( mame_bitmap bitmap )
	{
		int offs;
	
		for( offs = spriteram_size[0]; offs >= 0; offs -= 4 )
		{
			int sy = 240 - spriteram.read(offs);
			int sx = spriteram.read(offs+3);
			int code = spriteram.read(offs+1) & 0x3f;
			int attr = spriteram.read(offs+2);
			int flipy = spriteram.read(offs+1) & 0x80;
			int flipx = spriteram.read(offs+1) & 0x40;
	
			code += ( attr & 0x80 )!=0 ? 0x40 : 0x00;
			code += ( attr & 0x40 )!=0 ? 0x80 : 0x00;
	
			drawgfx( bitmap, Machine.gfx[2],
					code,
					attr & 7,
					flipx,flipy,
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	}
	
	/***************************************************************************
	
		Draw the background layer
	
	***************************************************************************/
	
	static void draw_background( mame_bitmap bitmap )
	{
		int offs;
	
		for ( offs = 0; offs < timelimt_bg_videoram_size[0]; offs++ )
		{
			if ( dirtybuffer[offs] != 0 )
			{
				int sx, sy, code;
	
				sx = offs % 64;
				sy = offs / 64;
				code = timelimt_bg_videoram.read(offs);
	
				dirtybuffer[offs] = 0;
	
				drawgfx( tmpbitmap, Machine.gfx[1],
						code,
						0,
						0,0,
						8*sx,8*sy,
						null,TRANSPARENCY_NONE,0);
			}
		}
	
		{
			int tx = -scrollx;
			int ty = -scrolly;
	
			copyscrollbitmap( bitmap, tmpbitmap, 1, new int[]{tx}, 1, new int[]{ty}, Machine.visible_area, TRANSPARENCY_NONE, 0 );
		}
	}
	
	/***************************************************************************
	
		Draw the foreground layer
	
	***************************************************************************/
	
	static void draw_foreground( mame_bitmap bitmap )
	{
		int offs;
	
		for ( offs = 0; offs < videoram_size[0]; offs++ )
		{
			int sx, sy, code;
	
			sx = offs % 32;
			sy = offs / 32;
	
			code = videoram.read(offs);
	
			drawgfx( bitmap, Machine.gfx[0],
					 code,
					 0,
					 0,0,
					 8*sx,8*sy,
					 null,TRANSPARENCY_PEN,0);
		}
	}
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	
	public static VhUpdatePtr timelimt_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		if ( full_refresh != 0 )
		{
			memset( dirtybuffer, 1, timelimt_bg_videoram_size[0] );
		}
	
		draw_background( bitmap );
		drawsprites( bitmap );
		draw_foreground(  bitmap );
	} };
}
