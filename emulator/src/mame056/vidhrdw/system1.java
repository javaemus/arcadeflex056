/*************************************************************************

  System1 / System 2.   By Jarek Parchanski & Mirko Buffoni.

  Many thanks to Roberto Ventura, for precious information about
  System 1 hardware.

*************************************************************************/

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
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.drivers.system1.*;

import static mame056.vidhrdw.generic.*;
import static mame056.vidhrdw.system1.*;
import static mame056.vidhrdw.system1H.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class system1
{
	
	public static UBytePtr system1_scroll_y=new UBytePtr();
	public static UBytePtr system1_scroll_x=new UBytePtr();
	public static UBytePtr system1_videoram=new UBytePtr();
	public static UBytePtr system1_backgroundram=new UBytePtr();
	public static UBytePtr system1_sprites_collisionram=new UBytePtr();
	public static UBytePtr system1_background_collisionram=new UBytePtr();
	public static UBytePtr system1_scrollx_ram=new UBytePtr();
	public static int[] system1_videoram_size=new int[1];
	public static int[] system1_backgroundram_size=new int[1];
	
	public static int[] sprite_onscreen_map;
	public static int background_scrollx,background_scrolly;
	public static int[] bg_dirtybuffer;
	
	public static int[] scrollx_row=new int[32];
	static mame_bitmap tmp_bitmap;
	
	//static int system1_pixel_mode = 0
	public static int system1_background_memory,system1_video_mode=0;
	
	public static UBytePtr system1_color_prom=new UBytePtr();
	
	static char[] wbml_paged_videoram;
	static int wbml_videoram_bank=0,wbml_videoram_bank_latch=0;
	
	/***************************************************************************
	
	  There are two kind of color handling: in the System 1 games, values in the
	  palette RAM are directly mapped to colors with the usual BBGGGRRR format;
	  in the System 2 ones (Choplifter, WBML, etc.), the value in the palette RAM
	  is a lookup offset for three palette PROMs in RRRRGGGGBBBB format.
	
	  It's hard to tell for sure because they use resistor packs, but here's
	  what I think the values are from measurment with a volt meter:
	
	  Blue: .250K ohms
	  Blue: .495K ohms
	  Green:.250K ohms
	  Green:.495K ohms
	  Green:.995K ohms
	  Red:� .495K ohms
	  Red:� .250K ohms
	  Red:� .995K ohms
	
	  accurate to +/- .003K ohms.
	
	***************************************************************************/
	public static VhConvertColorPromPtr system1_vh_convert_color_prom= new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                system1_color_prom = color_prom;
                
                /*for (int i=0 ; i<palette.length ; i++){
                    //paletteram.write(i, palette[i]);
                    int val, r, g, b;
                    int data = paletteram.read(i);
                    val = (data >> 0) & 0x07;
			r = (val << 5) | (val << 2) | (val >> 1);
	
			val = (data >> 3) & 0x07;
			g = (val << 5) | (val << 2) | (val >> 1);
	
			val = (data >> 6) & 0x03;
			//if (val != 0) val++;
			b = (val << 6) | (val << 4) | (val << 2);
		
	
		palette_set_color((i&0x7ff),r,g,b);
                }*/
            }
        };
	
	public static WriteHandlerPtr system1_paletteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int val,r,g,b;
	
	
		paletteram.write(offset, data);
	
		if (system1_color_prom != null)
		{
			int bit0,bit1,bit2,bit3;
	
			val = system1_color_prom.read(data+0*256);
			bit0 = (val >> 0) & 0x01;
			bit1 = (val >> 1) & 0x01;
			bit2 = (val >> 2) & 0x01;
			bit3 = (val >> 3) & 0x01;
			r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			val = system1_color_prom.read(data+1*256);
			bit0 = (val >> 0) & 0x01;
			bit1 = (val >> 1) & 0x01;
			bit2 = (val >> 2) & 0x01;
			bit3 = (val >> 3) & 0x01;
			g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			val = system1_color_prom.read(data+2*256);
			bit0 = (val >> 0) & 0x01;
			bit1 = (val >> 1) & 0x01;
			bit2 = (val >> 2) & 0x01;
			bit3 = (val >> 3) & 0x01;
			b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
		}
		else
		{
			val = (data >> 0) & 0x07;
			r = (val << 5) | (val << 2) | (val >> 1);
	
			val = (data >> 3) & 0x07;
			g = (val << 5) | (val << 2) | (val >> 1);
	
			val = (data >> 5) & 0x06;
			if (val!=0) val++;
			b = (val << 5) | (val << 2) | (val >> 1);
		}
	
		palette_set_color(offset,r,g,b);
	} };
	
	
	
	public static VhStartPtr system1_vh_start = new VhStartPtr() {
            public int handler() {
                if ((sprite_onscreen_map = new int[256*256]) == null)
			return 1;
		memset(sprite_onscreen_map,255,256*256);
	
		if ((bg_dirtybuffer = new int[1024]) == null)
		{
			//free(sprite_onscreen_map);
                        sprite_onscreen_map = null;
			return 1;
		}
		memset(bg_dirtybuffer,1,1024);
		if ((wbml_paged_videoram = new char[0x4000]) == null)			/* Allocate 16k for background banked ram */
		{
			//free(bg_dirtybuffer);
                        bg_dirtybuffer = null;
			//free(sprite_onscreen_map);
                        sprite_onscreen_map = null;
			return 1;
		}
		memset(wbml_paged_videoram,0,0x4000);
		if ((tmp_bitmap = bitmap_alloc(Machine.drv.screen_width,Machine.drv.screen_height)) == null)
		{
			//free(wbml_paged_videoram);
                        wbml_paged_videoram = null;
			//free(bg_dirtybuffer);
                        bg_dirtybuffer = null;
			//free(sprite_onscreen_map);
                        sprite_onscreen_map = null;
			return 1;
		}
	
		return 0;
            }
        };
		
	public static VhStopPtr system1_vh_stop = new VhStopPtr() {
            public void handler() {
                bitmap_free(tmp_bitmap);
		//free(wbml_paged_videoram);
                wbml_paged_videoram = null;
		//free(bg_dirtybuffer);
                bg_dirtybuffer = null;
		//free(sprite_onscreen_map);
                sprite_onscreen_map = null;
            }
        };
	
	public static WriteHandlerPtr system1_videomode_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	if ((data & 0x6e) != 0) logerror("videomode = %02x\n",data);
	
		/* bit 0 is coin counter */
		coin_counter_w.handler(0, data & 1);
	
		/* bit 4 is screen blank */
		system1_video_mode = data;
	
		/* bit 7 is flip screen */
		flip_screen_set(data & 0x80);
	} };
	
	public static ReadHandlerPtr system1_videomode_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return system1_video_mode;
	} };
	
	public static void system1_define_background_memory(int mode)
	{
		system1_background_memory = mode;
	}
	
	
	public static int get_sprite_bottom_y(int spr_number)
	{
		return  spriteram.read(0x10 * spr_number + SPR_Y_BOTTOM);
	}
	
	public static void draw_pixel( mame_bitmap bitmap,
					  	   int x,int y,int x_flipped,int y_flipped,
					  	   int spr_number,int color)
	{
		int xr,yr;
		int sprite_onscreen;
	
	
		if (x < 0 || x >= Machine.scrbitmap.width ||
			y < 0 || y >= Machine.scrbitmap.height)
			return;
	
		if (sprite_onscreen_map[256*y+x] != 255)
		{
			sprite_onscreen = sprite_onscreen_map[256*y+x];
			system1_sprites_collisionram.write(sprite_onscreen + 32 * spr_number, 0xff);
		}
	
		sprite_onscreen_map[256*y+x] = spr_number;
	
		if (x_flipped >= Machine.visible_area.min_x ||
			x_flipped <= Machine.visible_area.max_x ||
			y_flipped >= Machine.visible_area.min_y ||
			y_flipped <= Machine.visible_area.max_y)
		{
			plot_pixel.handler(bitmap, x_flipped, y_flipped, color);
		}
	
		xr = ((x - background_scrollx) & 0xff) / 8;
		yr = ((y - background_scrolly) & 0xff) / 8;
	
		/* TODO: bits 5 and 6 of backgroundram are also used (e.g. Pitfall2, Mr. Viking) */
		/* what's the difference? Bit 7 is used in Choplifter/WBML for extra char bank */
		/* selection, but it is also set in Pitfall2 */
	
		if (system1_background_memory == system1_BACKGROUND_MEMORY_SINGLE)
		{
			if ((system1_backgroundram.read(2 * (32 * yr + xr) + 1) & 0x10) != 0)
				system1_background_collisionram.write(0x20 + spr_number, 0xff);
		}
		else
		{
			/* TODO: I should handle the paged background memory here. */
			/* maybe collision detection is not used by the paged games */
			/* (wbml and tokisens), though tokisens doesn't play very well */
			/* (you can't seem to fit in gaps where you should fit) */
		}
	
		/* TODO: collision should probably be checked with the foreground as well */
		/* (TeddyBoy Blues, head of the tiger in girl bonus round) */
	}
	
	public static WriteHandlerPtr system1_background_collisionram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* to do the RAM check, Mister Viking writes 0xff and immediately */
		/* reads it back, expecting bit 0 to be NOT set. */
		system1_background_collisionram.write(offset, 0x7e);
	} };
	
	public static WriteHandlerPtr system1_sprites_collisionram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* to do the RAM check, Mister Viking write 0xff and immediately */
		/* reads it back, expecting bit 0 to be NOT set. */
		/* Up'n Down expects to find 0x7e at f800 before doing the whole */
		/* collision test */
		system1_sprites_collisionram.write(offset, 0x7e);
	} };
	
	
	
	/*TODO*///extern struct GameDriver driver_wbml;
	
	static void draw_sprite(mame_bitmap bitmap,int spr_number)
	{
		int sy,row,height,src,bank;
		UBytePtr sprite_base;
		IntArray sprite_palette;
		int skip;	/* bytes to skip before drawing each row (can be negative) */
		UBytePtr gfx;
	
	
		sprite_base	= new UBytePtr(spriteram, 0x10 * spr_number);
	
		src = sprite_base.read(SPR_GFXOFS_LO) + (sprite_base.read(SPR_GFXOFS_HI) << 8);
		bank = 0x8000 * (((sprite_base.read(SPR_X_HI) & 0x80) >> 7) + ((sprite_base.read(SPR_X_HI) & 0x40) >> 5));
		bank &= (memory_region_length(REGION_GFX2)-1);	/* limit to the range of available ROMs */
		skip = sprite_base.read(SPR_SKIP_LO) + (sprite_base.read(SPR_SKIP_HI) << 8);
	
		height = sprite_base.read(SPR_Y_BOTTOM) - sprite_base.read(SPR_Y_TOP);
		sprite_palette = new IntArray(Machine.remapped_colortable, 0x10 * spr_number);
	
		sy = sprite_base.read(SPR_Y_TOP) + 1;
	
		/* graphics region #2 contains the packed sprite data */
		/*TODO*///gfx = new memory_region(REGION_GFX2)[bank];
                gfx = new UBytePtr(memory_region(REGION_GFX2), bank);
	
		for (row = 0;row < height;row++)
		{
			int x,x_flipped;
			int y,y_flipped;
			int src2;
	
			src = src2 = src + skip;
	
			/* the +1 prevents sprite lag in Wonder Boy */
			x = sprite_base.read(SPR_X_LO) + ((sprite_base.read(SPR_X_HI) & 0x01) << 8) + 1;
			if (Machine.gamedrv == driver_wbml || Machine.gamedrv.clone_of == driver_wbml)
			{
				x += 7*2;
			}
			x_flipped = x;
			y = y_flipped = sy+row;
	
			if (flip_screen_x[0] != 0)
			{
				y_flipped = 258 - sy - height + row;
				x_flipped = (252*2) - x;
			}
	
			x /= 2;	/* the hardware has sub-pixel placement, it seems */
			x_flipped /= 2;
	
			while (true)
			{
				int color1,color2,data;
	
				data = gfx.read(src2 & 0x7fff);
	
				if ((src & 0x8000) != 0)
				{
					src2--;
	
					color1 = data & 0x0f;
					color2 = data >> 4;
				}
				else
				{
					src2++;
	
					color1 = data >> 4;
					color2 = data & 0x0f;
				}
	
				if (color1 == 15) break;
				if (color1 != 0)
					draw_pixel(bitmap,x,y,x_flipped,y_flipped,spr_number,sprite_palette.read(color1));
				x++;
				x_flipped += (flip_screen_x[0]!=0) ? -1 : 1;
	
				if (color2 == 15) break;
				if (color2 != 0)
					draw_pixel(bitmap,x,y,x_flipped,y_flipped,spr_number,sprite_palette.read(color2));
				x++;
				x_flipped += flip_screen_x[0]!=0 ? -1 : 1;
			}
		}
	}
	
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int spr_number,sprite_bottom_y,sprite_top_y;
		UBytePtr sprite_base;
	
	
		memset(sprite_onscreen_map,255,256*256);
	
		for (spr_number = 0;spr_number < 32;spr_number++)
		{
			sprite_base = new UBytePtr(spriteram, 0x10 * spr_number);
			sprite_top_y = sprite_base.read(SPR_Y_TOP);
			sprite_bottom_y = sprite_base.read(SPR_Y_BOTTOM);
			if ((sprite_bottom_y!=0) && (sprite_bottom_y-sprite_top_y > 0))
				draw_sprite(bitmap,spr_number);
		}
	}
	
	
	
	public static WriteHandlerPtr system1_backgroundram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		system1_backgroundram.write((offset&0xfff), data);
		bg_dirtybuffer[(offset&0xfff)>>1] = 1;
	} };
	
	
	static int system1_draw_fg(mame_bitmap bitmap,int priority)
	{
	int sx,sy,offs;
		int drawn = 0;
	
	
		priority <<= 3;
	
		for (offs = 0;offs < system1_videoram_size[0];offs += 2)
		{
			if ((system1_videoram.read(offs+1) & 0x08) == priority)
			{
				int code,color;
	
	
				code = (system1_videoram.read(offs) | (system1_videoram.read(offs+1) << 8));
				code = ((code >> 4) & 0x800) | (code & 0x7ff);	/* Heavy Metal only */
				color = ((code >> 5) & 0x3f);
				sx = (offs/2) % 32;
				sy = (offs/2) / 32;
	
				if (flip_screen() != 0)
				{
					sx = 31 - sx;
					sy = 31 - sy;
				}
	
				if ((Machine.gfx[0].pen_usage[code] & ~1) != 0)
				{
					drawn = 1;
	
					drawgfx(bitmap,Machine.gfx[0],
							code,
							color,
							flip_screen(),flip_screen(),
							8*sx,8*sy,
							Machine.visible_area,TRANSPARENCY_PEN,0);
				}
			}
		}
	
		return drawn;
	}
	
	static void system1_draw_bg(mame_bitmap bitmap,int priority)
	{
		int sx,sy,offs;
		int background_scrollx_flip, background_scrolly_flip;
	
	
		background_scrollx = ((system1_scroll_x.read(0) >> 1) + ((system1_scroll_x.read(1) & 1) << 7) + 14) & 0xff;
		background_scrolly = (-system1_scroll_y.read(0)) & 0xff;
	
		background_scrollx_flip = (275 - background_scrollx) & 0xff;
		background_scrolly_flip = (256 - background_scrolly) & 0xff;
	
		if (priority == -1)
		{
			/* optimized far background */
	
			/* for every character in the background video RAM, check if it has
			 * been modified since last time and update it accordingly.
			 */
	
			for (offs = 0;offs < system1_backgroundram_size[0];offs += 2)
			{
				if (bg_dirtybuffer[offs / 2] != 0)
				{
					int code,color;
	
	
					bg_dirtybuffer[offs / 2] = 0;
	
					code = (system1_backgroundram.read(offs) | (system1_backgroundram.read(offs+1) << 8));
					code = ((code >> 4) & 0x800) | (code & 0x7ff);	/* Heavy Metal only */
					color = ((code >> 5) & 0x3f) + 0x40;
					sx = (offs/2) % 32;
					sy = (offs/2) / 32;
	
					if (flip_screen() != 0)
					{
						sx = 31 - sx;
						sy = 31 - sy;
					}
	
					drawgfx(tmp_bitmap,Machine.gfx[0],
							code,
							color,
							flip_screen(),flip_screen(),
							8*sx,8*sy,
							null,TRANSPARENCY_NONE,0);
				}
			}
	
			/* copy the temporary bitmap to the screen */
			if (flip_screen() != 0)
				copyscrollbitmap(bitmap,tmp_bitmap,1,new int[]{background_scrollx_flip},1,new int[]{background_scrolly_flip},Machine.visible_area,TRANSPARENCY_NONE,0);
			else
				copyscrollbitmap(bitmap,tmp_bitmap,1,new int[]{background_scrollx},1,new int[]{background_scrolly},Machine.visible_area,TRANSPARENCY_NONE,0);
		}
		else
		{
			priority <<= 3;
	
			for (offs = 0;offs < system1_backgroundram_size[0];offs += 2)
			{
				if ((system1_backgroundram.read(offs+1) & 0x08) == priority)
				{
					int code,color;
	
	
					code = (system1_backgroundram.read(offs) | (system1_backgroundram.read(offs+1) << 8));
					code = ((code >> 4) & 0x800) | (code & 0x7ff);	/* Heavy Metal only */
					color = ((code >> 5) & 0x3f) + 0x40;
					sx = (offs/2) % 32;
					sy = (offs/2) / 32;
	
					if (flip_screen() != 0)
					{
						sx = 8*(31-sx) + background_scrollx_flip;
						sy = 8*(31-sy) + background_scrolly_flip;
					}
					else
					{
						sx = 8*sx + background_scrollx;
						sy = 8*sy + background_scrolly;
					}
	
					/* draw it 4 times because of possible wrap around */
					drawgfx(bitmap,Machine.gfx[0],
							code,
							color,
							flip_screen(),flip_screen(),
							sx,sy,
							Machine.visible_area,TRANSPARENCY_PEN,0);
					drawgfx(bitmap,Machine.gfx[0],
							code,
							color,
							flip_screen(),flip_screen(),
							sx-256,sy,
							Machine.visible_area,TRANSPARENCY_PEN,0);
					drawgfx(bitmap,Machine.gfx[0],
							code,
							color,
							flip_screen(),flip_screen(),
							sx,sy-256,
							Machine.visible_area,TRANSPARENCY_PEN,0);
					drawgfx(bitmap,Machine.gfx[0],
							code,
							color,
							flip_screen(),flip_screen(),
							sx-256,sy-256,
							Machine.visible_area,TRANSPARENCY_PEN,0);
				}
			}
		}
	}
	
	public static VhUpdatePtr system1_vh_screenrefresh = new VhUpdatePtr() {
            public void handler(mame_bitmap bitmap, int full_refresh) {
                int drawn;
	
	
		system1_draw_bg(bitmap,-1);
		drawn = system1_draw_fg(bitmap,0);
		/* redraw low priority bg tiles if necessary */
		if (drawn != 0) system1_draw_bg(bitmap,0);
		draw_sprites(bitmap);
		system1_draw_bg(bitmap,1);
		system1_draw_fg(bitmap,1);
	
		/* even if screen is off, sprites must still be drawn to update the collision table */
		if ((system1_video_mode & 0x10) != 0)  /* screen off */
			fillbitmap(bitmap,Machine.pens[0],Machine.visible_area);
            }
        };
	
	public static WriteHandlerPtr choplifter_scroll_x_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		system1_scrollx_ram.write(offset, data);
	
		scrollx_row[offset/2] = (system1_scrollx_ram.read(offset & ~1) >> 1) + ((system1_scrollx_ram.read(offset | 1) & 1) << 7);
	} };
	
	static void chplft_draw_bg(mame_bitmap bitmap, int priority)
	{
		int sx,sy,offs;
		int choplifter_scroll_x_on = (system1_scrollx_ram.read(0) == 0xe5 && system1_scrollx_ram.read(1) == 0xff) ? 0 : 1;
	
	
		if (priority == -1)
		{
			/* optimized far background */
	
			/* for every character in the background video RAM, check if it has
			 * been modified since last time and update it accordingly.
			 */
	
			for (offs = 0;offs < system1_backgroundram_size[0];offs += 2)
			{
				if (bg_dirtybuffer[offs / 2] != 0)
				{
					int code,color;
	
	
					bg_dirtybuffer[offs / 2] = 0;
	
					code = (system1_backgroundram.read(offs) | (system1_backgroundram.read(offs+1) << 8));
					code = ((code >> 4) & 0x800) | (code & 0x7ff);	/* Heavy Metal only */
					color = ((code >> 5) & 0x3f) + 0x40;
					sx = (offs/2) % 32;
					sy = (offs/2) / 32;
	
					/*TODO*///if (flip_screen)
                                        if (flip_screen_x[0] != 0)
					{
						sx = 31 - sx;
						sy = 31 - sy;
					}
	
					drawgfx(tmp_bitmap,Machine.gfx[0],
							code,
							color,
							flip_screen_x[0],flip_screen_x[0],
							8*sx,8*sy,
							null,TRANSPARENCY_NONE,0);
				}
			}
	
			/* copy the temporary bitmap to the screen */
			if (choplifter_scroll_x_on != 0)
			{
				if (flip_screen_x[0] != 0)
				{
					int[] scrollx_row_flip=new int[32];
                                        int i;
	
					for (i = 0; i < 32; i++)
						scrollx_row_flip[31-i] = (256-scrollx_row[i]) & 0xff;
	
					copyscrollbitmap(bitmap,tmp_bitmap,32,scrollx_row_flip,0,null,Machine.visible_area,TRANSPARENCY_NONE,0);
				}
				else
					copyscrollbitmap(bitmap,tmp_bitmap,32,scrollx_row,0,null,Machine.visible_area,TRANSPARENCY_NONE,0);
			}
			else
				copybitmap(bitmap,tmp_bitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
		}
		else
		{
			priority <<= 3;
	
			for (offs = 0;offs < system1_backgroundram_size[0];offs += 2)
			{
				if ((system1_backgroundram.read(offs+1) & 0x08) == priority)
				{
					int code,color;
	
	
					code = (system1_backgroundram.read(offs) | (system1_backgroundram.read(offs+1) << 8));
					code = ((code >> 4) & 0x800) | (code & 0x7ff);	/* Heavy Metal only */
					color = ((code >> 5) & 0x3f) + 0x40;
					sx = (offs/2) % 32;
					sy = (offs/2) / 32;
	
					if (flip_screen_x[0] != 0)
					{
						sx = 8*(31-sx);
	
						if (choplifter_scroll_x_on != 0)
							sx = (sx - scrollx_row[sy]) & 0xff;
	
						sy = 31 - sy;
					}
					else
					{
						sx = 8*sx;
	
						if (choplifter_scroll_x_on != 0)
							sx = (sx + scrollx_row[sy]) & 0xff;
					}
	
					drawgfx(bitmap,Machine.gfx[0],
							code,
							color,
							flip_screen_x[0],flip_screen_x[0],
							sx,8*sy,
							Machine.visible_area,TRANSPARENCY_PEN,0);
				}
			}
		}
	}
	
	public static VhUpdatePtr choplifter_vh_screenrefresh = new VhUpdatePtr() {
            public void handler(mame_bitmap bitmap, int full_refresh) {
                int drawn;
	
	
		chplft_draw_bg(bitmap,-1);
		drawn = system1_draw_fg(bitmap,0);
		/* redraw low priority bg tiles if necessary */
		if (drawn != 0) chplft_draw_bg(bitmap,0);
		draw_sprites(bitmap);
		chplft_draw_bg(bitmap,1);
		system1_draw_fg(bitmap,1);
	
		/* even if screen is off, sprites must still be drawn to update the collision table */
		if ((system1_video_mode & 0x10) != 0)  /* screen off */
			fillbitmap(bitmap,Machine.pens[0],Machine.visible_area);
	
	
	/*TODO*///#ifdef MAME_DEBUG
	/*TODO*///	if (keyboard_pressed(KEYCODE_SPACE))		// goto next level
	/*TODO*///	{
	/*TODO*///		memory_region(REGION_CPU1)[0xC085]=33;
	/*TODO*///	}
	/*TODO*///#endif
            }
        };
	
	public static ReadHandlerPtr wbml_videoram_bank_latch_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return wbml_videoram_bank_latch;
	} };
	
	public static WriteHandlerPtr wbml_videoram_bank_latch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		wbml_videoram_bank_latch = data;
		wbml_videoram_bank = (data >> 1) & 0x03;	/* Select 4 banks of 4k, bit 2,1 */
	} };
	
	public static ReadHandlerPtr wbml_paged_videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return wbml_paged_videoram[0x1000*wbml_videoram_bank + offset];
	} };
	
	public static WriteHandlerPtr wbml_paged_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		wbml_paged_videoram[0x1000*wbml_videoram_bank + offset] = (char) data;
	} };
	
	static void wbml_draw_bg(mame_bitmap bitmap, int trasp)
	{
		int page;
	
	
		int xscroll = (wbml_paged_videoram[0x7c0] >> 1) + ((wbml_paged_videoram[0x7c1] & 1) << 7) - 256 + 5;
		int yscroll = -wbml_paged_videoram[0x7ba];
	
		for (page=0; page < 4; page++)
		{
			UBytePtr source = new UBytePtr(wbml_paged_videoram, (wbml_paged_videoram[0x0740 + page*2] & 0x07)*0x800);
			int startx = (page&1)*256+xscroll;
			int starty = (page>>1)*256+yscroll;
			int row,col;
	
	
			for( row=0; row<32*8; row+=8 )
			{
				for( col=0; col<32*8; col+=8 )
				{
					int code,priority;
					int x = (startx+col) & 0x1ff;
					int y = (starty+row) & 0x1ff;
					if (x > 256) x -= 512;
					if (y > 224) y -= 512;
	
					if (flip_screen_x[0] != 0)
					{
						x = 248 - x;
						y = 248 - y;
					}
	
					code = source.read(0) + (source.read(1) << 8);
					priority = code & 0x800;
					code = ((code >> 4) & 0x800) | (code & 0x7ff);
	
					if (trasp == 0)
						drawgfx(bitmap,Machine.gfx[0],
								code,
								((code >> 5) & 0x3f) + 64,
								flip_screen_x[0],flip_screen_x[0],
								x,y,
								Machine.visible_area, TRANSPARENCY_NONE, 0);
					else if (priority != 0)
						drawgfx(bitmap,Machine.gfx[0],
								code,
								((code >> 5) & 0x3f) + 64,
								flip_screen_x[0],flip_screen_x[0],
								x,y,
								Machine.visible_area, TRANSPARENCY_PEN, 0);
	
					source.inc(2);
				}
			}
		} /* next page */
	}
	
	static void wbml_draw_fg(mame_bitmap bitmap)
	{
		int offs;
	
	
		for (offs = 0;offs < 0x700;offs += 2)
		{
			int sx,sy,code;
	
	
			sx = (offs/2) % 32;
			sy = (offs/2) / 32;
			code = wbml_paged_videoram[offs] | (wbml_paged_videoram[offs+1] << 8);
			code = ((code >> 4) & 0x800) | (code & 0x7ff);
	
			if (flip_screen_x[0] != 0)
			{
				sx = 31 - sx;
				sy = 31 - sy;
			}
	
			drawgfx(bitmap,Machine.gfx[0],
					code,
					(code >> 5) & 0x3f,
					flip_screen_x[0],flip_screen_x[0],
					8*sx,8*sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	}
	
	
	public static VhUpdatePtr wbml_vh_screenrefresh = new VhUpdatePtr() {
            public void handler(mame_bitmap bitmap, int full_refresh) {
                wbml_draw_bg(bitmap,0);
		draw_sprites(bitmap);
		wbml_draw_bg(bitmap,1);
		wbml_draw_fg(bitmap);
	
		/* even if screen is off, sprites must still be drawn to update the collision table */
		if ((system1_video_mode & 0x10) != 0)  /* screen off */
			fillbitmap(bitmap,Machine.pens[0],Machine.visible_area);
            }
        };

}
