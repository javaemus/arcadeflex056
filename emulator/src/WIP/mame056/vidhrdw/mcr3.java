/***************************************************************************

  vidhrdw/mcr3.c

	Functions to emulate the video hardware of an mcr3-style machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.machine.mcr.*;
import static common.ptr.*;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.mame.*;
import static mame056.cpuexec.*;
import static mame056.drawgfxH.*;
import static mame056.drawgfx.*;
import static mame056.palette.*;
import static mame056.tilemapC.priority_bitmap;
import static mame056.vidhrdw.generic.*;
import static common.libc.cstring.*;
import static common.libc.cstdio.*;

public class mcr3 {
	
	
	public static int MIN(int x, int y){
            return (x)<(y)?(x):(y);
        }
	
	/* These are used to align Discs of Tron with the backdrop */
	public static int DOTRON_X_START = 90;
	public static int DOTRON_Y_START = 118;
	/*TODO*///static struct artwork_info *backdrop[2];
	
	
	
	/*************************************
	 *
	 *	Global variables
	 *
	 *************************************/
	
	/* Spy Hunter hardware extras */
	public static int spyhunt_sprite_color_mask;
	public static int spyhunt_scrollx, spyhunt_scrolly;
	public static int spyhunt_scroll_offset;
	public static int spyhunt_draw_lamps;
	public static int[] spyhunt_lamp=new int[30];
	
	public static UBytePtr spyhunt_alpharam = new UBytePtr();
	public static int[] spyhunt_alpharam_size = new int[1];
	
	
	
	/*************************************
	 *
	 *	Local variables
	 *
	 *************************************/
	
	/* Spy Hunter-specific scrolling background */
	static mame_bitmap spyhunt_backbitmap;
	
	static int last_cocktail_flip;
	
	
	
	/*************************************
	 *
	 *	Palette RAM writes
	 *
	 *************************************/
	
	public static WriteHandlerPtr mcr3_paletteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int r, g, b;
	
		paletteram.write(offset, data);
		offset &= 0x7f;
	
		/* high bit of red comes from low bit of address */
		r = ((offset & 1) << 2) + (data >> 6);
		g = (data >> 0) & 7;
		b = (data >> 3) & 7;
	
		/* up to 8 bits */
		r = (r << 5) | (r << 2) | (r >> 1);
		g = (g << 5) | (g << 2) | (g >> 1);
		b = (b << 5) | (b << 2) | (b >> 1);
	
		palette_set_color(offset / 2, r, g, b);
	} };
	
	
	
	/*************************************
	 *
	 *	Video RAM writes
	 *
	 *************************************/
	
	public static WriteHandlerPtr mcr3_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (videoram.read(offset) != data)
		{
			dirtybuffer[offset & ~1] = 1;
			videoram.write(offset, data);
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Background update
	 *
	 *************************************/
	
	static void mcr3_update_background(mame_bitmap bitmap, int color_xor)
	{
		int offs;
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 2; offs >= 0; offs -= 2)
			if (dirtybuffer[offs] != 0)
			{
				int mx = (offs / 2) % 32;
				int my = (offs / 2) / 32;
				int attr = videoram.read(offs + 1);
				int color = ((attr & 0x30) >> 4) ^ color_xor;
				int code = videoram.read(offs) | ((attr & 0x03) << 8) | ((attr & 0x40) << 4);
	
				if (mcr_cocktail_flip == 0)
					drawgfx(bitmap, Machine.gfx[0], code, color, attr & 0x04, attr & 0x08,
							16 * mx, 16 * my, Machine.visible_area, TRANSPARENCY_NONE, 0);
				else
					drawgfx(bitmap, Machine.gfx[0], code, color, (attr & 0x04)!=0?0:1, (attr & 0x08)!=0?0:1,
							16 * (31 - mx), 16 * (29 - my), Machine.visible_area, TRANSPARENCY_NONE, 0);
	
				dirtybuffer[offs] = 0;
			}
	}
	
	
	
	/*************************************
	 *
	 *	Sprite update
	 *
	 *************************************/
	
	public static void mcr3_update_sprites(mame_bitmap bitmap, int color_mask, int code_xor, int dx, int dy)
	{
		int offs;
	
		fillbitmap(priority_bitmap, 1, null);
	
		/* loop over sprite RAM */
		for (offs = spriteram_size[0] - 4; offs >= 0; offs -= 4)
		{
			int code, color, flipx, flipy, sx, sy, flags;
	
			/* skip if zero */
			if (spriteram.read(offs) == 0)
				continue;
	
			/* extract the bits of information */
			flags = spriteram.read(offs + 1);
			code = spriteram.read(offs + 2) + 256 * ((flags >> 3) & 0x01);
			color = ~flags & color_mask;
			flipx = flags & 0x10;
			flipy = flags & 0x20;
			sx = (spriteram.read(offs + 3) - 3) * 2;
			sy = (241 - spriteram.read(offs)) * 2;
	
			code ^= code_xor;
	
			sx += dx;
			sy += dy;
	
			/* sprites use color 0 for background pen and 8 for the 'under tile' pen.
				The color 8 is used to cover over other sprites. */
			if (mcr_cocktail_flip == 0)
			{
				/* first draw the sprite, visible */
				pdrawgfx(bitmap, Machine.gfx[1], code, color, flipx, flipy, sx, sy,
						Machine.visible_area, TRANSPARENCY_PENS, 0x0101, 0x00);
	
				/* then draw the mask, behind the background but obscuring following sprites */
				pdrawgfx(bitmap, Machine.gfx[1], code, color, flipx, flipy, sx, sy,
						Machine.visible_area, TRANSPARENCY_PENS, 0xfeff, 0x02);
			}
			else
			{
				/* first draw the sprite, visible */
				pdrawgfx(bitmap, Machine.gfx[1], code, color, flipx!=0?0:1, flipy!=0?0:1, 480 - sx, 452 - sy,
						Machine.visible_area, TRANSPARENCY_PENS, 0x0101, 0x00);
	
				/* then draw the mask, behind the background but obscuring following sprites */
				pdrawgfx(bitmap, Machine.gfx[1], code, color, flipx!=0?0:1, flipy!=0?0:1, 480 - sx, 452 - sy,
						Machine.visible_area, TRANSPARENCY_PENS, 0xfeff, 0x02);
			}
		}
	}
	
	
	
	/*************************************
	 *
	 *	Generic MCR3 redraw
	 *
	 *************************************/
	
	public static VhUpdatePtr mcr3_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/* mark everything dirty on a cocktail flip change */
		if (last_cocktail_flip != mcr_cocktail_flip)
			memset(dirtybuffer, 1, videoram_size[0]);
		last_cocktail_flip = mcr_cocktail_flip;
	
		/* redraw the background */
		mcr3_update_background(tmpbitmap, 0);
	
		/* copy it to the destination */
		copybitmap(bitmap, tmpbitmap, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_NONE, 0);
	
		/* draw the sprites */
		mcr3_update_sprites(bitmap, 0x03, 0, 0, 0);
	} };
	
	
	
	/*************************************
	 *
	 *	MCR monoboard-specific redraw
	 *
	 *************************************/
	
	public static VhUpdatePtr mcrmono_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/* mark everything dirty on a cocktail flip change */
		if (last_cocktail_flip != mcr_cocktail_flip)
			memset(dirtybuffer, 1, videoram_size[0]);
		last_cocktail_flip = mcr_cocktail_flip;
	
		/* redraw the background */
		mcr3_update_background(tmpbitmap, 3);
	
		/* copy it to the destination */
		copybitmap(bitmap, tmpbitmap, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_NONE, 0);
	
		/* draw the sprites */
		mcr3_update_sprites(bitmap, 0x03, 0, 0, 0);
	} };
	
	
	
	/*************************************
	 *
	 *	Spy Hunter-specific color PROM decoder
	 *
	 *************************************/
	
	public static VhConvertColorPromPtr spyhunt_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                /* add some colors for the alpha RAM */
		palette[(4*16)*3+0] = 0;
		palette[(4*16)*3+1] = 0;
		palette[(4*16)*3+2] = 0;
		palette[(4*16+1)*3+0] = 0;
		palette[(4*16+1)*3+1] = 255;
		palette[(4*16+1)*3+2] = 0;
		palette[(4*16+2)*3+0] = 0;
		palette[(4*16+2)*3+1] = 0;
		palette[(4*16+2)*3+2] = 255;
		palette[(4*16+3)*3+0] = 255;
		palette[(4*16+3)*3+1] = 255;
		palette[(4*16+3)*3+2] = 255;
            }
        };
	
	
	/*************************************
	 *
	 *	Spy Hunter-specific video startup
	 *
	 *************************************/
	
	public static VhStartPtr spyhunt_vh_start = new VhStartPtr() { public int handler() 
	{
		/* allocate our own dirty buffer */
		dirtybuffer = new char[videoram_size[0]];
		if (dirtybuffer == null)
			return 1;
		memset(dirtybuffer, 1, videoram_size[0]);
	
		/* allocate a bitmap for the background */
		spyhunt_backbitmap = bitmap_alloc(64*64, 32*32);
		if (spyhunt_backbitmap == null)
		{
			dirtybuffer = null;
			return 1;
		}
	
		/* reset the scrolling */
		spyhunt_scrollx = spyhunt_scrolly = 0;
	
		return 0;
	} };
	
	
	
	/*************************************
	 *
	 *	Spy Hunter-specific video shutdown
	 *
	 *************************************/
	
	public static VhStopPtr spyhunt_vh_stop = new VhStopPtr() { public void handler() 
	{
		/* free the buffers */
		bitmap_free(spyhunt_backbitmap);
		dirtybuffer = null;
	} };
	
	
	/*************************************
	 *
	 *	Spy Hunter-specific redraw
	 *
	 *************************************/
	
	public static VhUpdatePtr spyhunt_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		rectangle clip = new rectangle( 0, 30*16-1, 0, 30*16-1 );
		int offs, scrollx, scrolly;
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 1; offs >= 0; offs--)
		{
			if (dirtybuffer[offs] != 0)
			{
				int code = videoram.read(offs);
				int vflip = code & 0x40;
				int mx = (offs >> 4) & 0x3f;
				int my = (offs & 0x0f) | ((offs >> 6) & 0x10);
	
				code = (code & 0x3f) | ((code & 0x80) >> 1);
	
				drawgfx(spyhunt_backbitmap, Machine.gfx[0], code, 0, 0, vflip,
						64 * mx, 32 * my, null, TRANSPARENCY_NONE, 0);
	
				dirtybuffer[offs] = 0;
			}
		}
	
		/* copy it to the destination */
		scrollx = -spyhunt_scrollx * 2 + spyhunt_scroll_offset;
		scrolly = -spyhunt_scrolly * 2;
		copyscrollbitmap(bitmap, spyhunt_backbitmap, 1, new int[]{scrollx}, 1, new int[]{scrolly}, clip, TRANSPARENCY_NONE, 0);
	
		/* draw the sprites */
		mcr3_update_sprites(bitmap, spyhunt_sprite_color_mask, 0x80, -12, 0);
	
		/* render any characters on top */
		for (offs = spyhunt_alpharam_size[0] - 1; offs >= 0; offs--)
		{
			int ch = spyhunt_alpharam.read(offs);
			if (ch != 0)
			{
				int mx = offs / 32;
				int my = offs % 32;
	
				drawgfx(bitmap, Machine.gfx[2], ch, 0, 0, 0,
						16 * mx - 16, 16 * my, clip, TRANSPARENCY_PEN, 0);
			}
		}
	
		/* lamp indicators */
		if (spyhunt_draw_lamps != 0)
		{
			String buffer="";
	
			buffer = sprintf("%s  %s  %s  %s  %s",
					(spyhunt_lamp[0]!=0 ? "OIL" : "   "),
					(spyhunt_lamp[1]!=0 ? "MISSILE" : "       "),
					(spyhunt_lamp[2]!=0 ? "VAN" : "   "),
					(spyhunt_lamp[3]!=0 ? "SMOKE" : "     "),
					(spyhunt_lamp[4]!=0 ? "GUNS" : "    "));
                        //System.out.println(buffer);
			for (offs = 0; offs < 30; offs++)
				drawgfx(bitmap, Machine.gfx[2], (buffer.charAt(offs)), 0, 0, 0,
						30 * 16, (29 - offs) * 16, Machine.visible_area, TRANSPARENCY_NONE, 0);
		}
	} };
	
	/*************************************
	 *
	 *	Discs of Tron-specific artwork loading
	 *
	 *************************************/
	
	static int dotron_artwork_start()
	{
		/*TODO*///backdrop_load("dotron1.png", 64);
		/*TODO*///if (artwork_backdrop)
		/*TODO*///{
		/*TODO*///	backdrop[0] = artwork_backdrop;
		/*TODO*///	artwork_load (&backdrop[1], "dotron2.png", 64);
		/*TODO*///}
		/*TODO*///else backdrop[0] = backdrop[1] = null;
	
		/* need to clear the border outside the game display */
		/*TODO*///fillbitmap(tmpbitmap,Machine.pens[64],Machine.visible_area);	/* artwork's black */
	
		/*TODO*///if (artwork_backdrop == 0)
		/*TODO*///{
			/* if no artwork available, reduce visible area to the game display */
		/*TODO*///	set_visible_area(DOTRON_X_START,DOTRON_X_START + 32*16-1,DOTRON_Y_START,DOTRON_Y_START + 30*16-1);
		/*TODO*///}
	
		return 0;
	}
	
	/*************************************
	 *
	 *	Discs of Tron-specific video startup
	 *
	 *************************************/
	
	public static VhStartPtr dotron_vh_start = new VhStartPtr() { public int handler() 
	{
		/* do generic initialization to start */
		if (generic_vh_start.handler()!= 0)
			return 1;
	
		return dotron_artwork_start();
	} };
	
	/*************************************
	 *
	 *	Discs of Tron light management
	 *
	 *************************************/
	
	public static void dotron_change_light(int light)
	{
		set_led_status(0,light & 1);	/* background light */
		set_led_status(1,light & 2);	/* strobe */
	
		/*TODO*///if (backdrop[light & 1])
		/*TODO*///	artwork_backdrop = backdrop[light & 1];
	}
	
	/*************************************
	 *
	 *	Discs of Tron-specific redraw
	 *
	 *************************************/
	
	public static VhUpdatePtr dotron_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		rectangle sclip=new rectangle();
		int offs;
	
		if (full_refresh != 0)
			memset(dirtybuffer, 1 ,videoram_size[0]);
	
		/* Screen clip, because our backdrop is a different resolution than the game */
		sclip.min_x = DOTRON_X_START;
		sclip.max_x = DOTRON_X_START + 32*16-1;
		sclip.min_y = DOTRON_Y_START;
		sclip.max_y = DOTRON_Y_START + 30*16-1;
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 2; offs >= 0; offs -= 2)
		{
			if (dirtybuffer[offs] != 0)
			{
				int attr = videoram.read(offs+1);
				int code = videoram.read(offs) + 256 * (attr & 0x03);
				int color = (attr & 0x30) >> 4;
				int mx = ((offs / 2) % 32) * 16;
				int my = ((offs / 2) / 32) * 16;
	
				/* center for the backdrop */
				mx += DOTRON_X_START;
				my += DOTRON_Y_START;
	
				drawgfx(tmpbitmap, Machine.gfx[0], code, color, attr & 0x04, attr & 0x08,
						mx, my, sclip, TRANSPARENCY_NONE, 0);
	
				dirtybuffer[offs] = 0;
			}
		}
	
		/* copy the resulting bitmap to the screen */
		copybitmap(bitmap, tmpbitmap, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_NONE, 0);
	
		/* draw the sprites */
		mcr3_update_sprites(bitmap, 0x03, 0, DOTRON_X_START, DOTRON_Y_START);
	} };
	
	/*************************************
	 *
	 *	Discs of Tron-specific video shutdown
	 *
	 *************************************/
	
	public static VhStopPtr dotron_vh_stop = new VhStopPtr() { public void handler() 
	{
		/*TODO*///if (artwork_backdrop != NULL)
		/*TODO*///{
			/* 0 is freeed by the core */
		/*TODO*///	artwork_backdrop = backdrop[0];
		/*TODO*///	artwork_free(&backdrop[1]);
		/*TODO*///}
	} };    
}
