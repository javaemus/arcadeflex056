/***************************************************************************
  Great Swordsman

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
import static mame056.memoryH.*;
import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;
import static common.libc.cstdio.sprintf;
import static mame056.usrintrf.usrintf_showmessage;

// refactor
import static arcadeflex036.osdepend.logerror;
import static common.libc.cstdlib.rand;

public class gsword
{
	
	public static int[] gs_videoram_size=new int[1];
	public static int[] gs_spritexy_size=new int[1];
	
	public static UBytePtr gs_videoram = new UBytePtr();
	public static UBytePtr gs_scrolly_ram = new UBytePtr();
	public static UBytePtr gs_spritexy_ram = new UBytePtr();
	public static UBytePtr gs_spritetile_ram = new UBytePtr();
	public static UBytePtr gs_spriteattrib_ram = new UBytePtr();
	
	static mame_bitmap 	bitmap_bg;
	public static UBytePtr dirtybuffer = new UBytePtr();
	static int charbank,charpalbank;
	static int flipscreen;
	
	public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
		
        public static void COLOR(char[] colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs])=(char) value;
        }
        
	public static VhConvertColorPromPtr josvolly_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                /* sprite lookup table is not original but it is almost 98% correct */
	
		int sprite_lookup_table[] = { 0x00,0x02,0x05,0x8C,0x49,0xDD,0xB7,0x06,
						0xD5,0x7A,0x85,0x8D,0x27,0x1A,0x03,0x0F };
		int i;
                int _palette = 0;
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int bit0,bit1,bit2,bit3;
	
	
			/* red component */
			bit0 = (color_prom.read(0) >> 0) & 0x01;
			bit1 = (color_prom.read(0) >> 1) & 0x01;
			bit2 = (color_prom.read(0) >> 2) & 0x01;
			bit3 = (color_prom.read(0) >> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
			/* green component */
			bit0 = (color_prom.read(Machine.drv.total_colors) >> 0) & 0x01;
			bit1 = (color_prom.read(Machine.drv.total_colors) >> 1) & 0x01;
			bit2 = (color_prom.read(Machine.drv.total_colors) >> 2) & 0x01;
			bit3 = (color_prom.read(Machine.drv.total_colors) >> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
			/* blue component */
			bit0 = (color_prom.read(2*Machine.drv.total_colors) >> 0) & 0x01;
			bit1 = (color_prom.read(2*Machine.drv.total_colors) >> 1) & 0x01;
			bit2 = (color_prom.read(2*Machine.drv.total_colors) >> 2) & 0x01;
			bit3 = (color_prom.read(2*Machine.drv.total_colors) >> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
	
			color_prom.inc();
		}
	
		color_prom.inc( 2*Machine.drv.total_colors );
		/* color_prom now points to the beginning of the sprite lookup table */
	
		/* characters */
		for (i = 0;i < TOTAL_COLORS(0);i++)
			COLOR(colortable,0,i,i);
	
		/* sprites */
		for (i = 0;i < TOTAL_COLORS(1);i++)
			COLOR(colortable,1,i,sprite_lookup_table[(color_prom.readinc())]);
            }
        };
	
	public static VhConvertColorPromPtr gsword_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                /* sprite lookup table is not original but it is almost 98% correct */
	
		int sprite_lookup_table[] = { 0x00,0x02,0x05,0x8C,0x49,0xDD,0xB7,0x06,
						0xD5,0x7A,0x85,0x8D,0x27,0x1A,0x03,0x0F };
		int i;
                int _palette = 0;
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int bit0,bit1,bit2;
	
			/* red component */
			bit0 = (color_prom.read(Machine.drv.total_colors) >> 0) & 1;
			bit1 = (color_prom.read(Machine.drv.total_colors) >> 1) & 1;
			bit2 = (color_prom.read(Machine.drv.total_colors) >> 2) & 1;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* green component */
			bit0 = (color_prom.read(Machine.drv.total_colors) >> 3) & 1;
			bit1 = (color_prom.read(0) >> 0) & 1;
			bit2 = (color_prom.read(0) >> 1) & 1;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* blue component */
			bit0 = 0;
			bit1 = (color_prom.read(0) >> 2) & 1;
			bit2 = (color_prom.read(0) >> 3) & 1;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
	
			color_prom.inc();
		}
	
		color_prom.inc(Machine.drv.total_colors);
		/* color_prom now points to the beginning of the sprite lookup table */
	
		/* characters */
		for (i = 0;i < TOTAL_COLORS(0);i++)
			COLOR(colortable,0,i,i);
	
		/* sprites */
		for (i = 0;i < TOTAL_COLORS(1);i++)
			COLOR(colortable,1,i,sprite_lookup_table[(color_prom.readinc())]);
            }
        };
	
	public static VhStartPtr gsword_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((dirtybuffer = new UBytePtr(gs_videoram_size[0])) == null) return 1;
		if ((bitmap_bg = bitmap_alloc(Machine.drv.screen_width,2*Machine.drv.screen_height)) == null)
		{
			dirtybuffer = null;
			return 1;
		}
		memset(dirtybuffer,1,gs_videoram_size[0]);
		return 0;
	} };
	
	public static VhStopPtr gsword_vh_stop = new VhStopPtr() { public void handler() 
	{
		dirtybuffer = null;
		bitmap_free(bitmap_bg);
	} };
	
	public static WriteHandlerPtr gs_charbank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (charbank != data)
		{
			charbank = data;
			memset(dirtybuffer,1,gs_videoram_size[0]);
		}
	} };
	
	public static WriteHandlerPtr gs_videoctrl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if ((data & 0x8f) != 0)
		{
			String baf;
			baf = sprintf("videoctrl %02x",data);
			usrintf_showmessage(baf);
		}
		/* bits 5-6 are char palette bank */
		if (charpalbank != ((data & 0x60) >> 5))
		{
			charpalbank = (data & 0x60) >> 5;
			memset(dirtybuffer,1,gs_videoram_size[0]);
		}
		/* bit 4 is flip screen */
		if (flipscreen != (data & 0x10))
		{
			flipscreen = data & 0x10;
		        memset(dirtybuffer,1,gs_videoram_size[0]);
		}
	
		/* bit 0 could be used but unknown */
	
		/* other bits unused */
	} };
	
	public static WriteHandlerPtr gs_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (gs_videoram.read(offset) != data)
		{
			dirtybuffer.write(offset, 1);
			gs_videoram.write(offset, data);
		}
	} };
	
	static void render_background(mame_bitmap bitmap)
	{
		int offs;
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
	
		for (offs = 0; offs < gs_videoram_size[0] ;offs++)
		{
			if (dirtybuffer.read(offs) != 0)
			{
				int sx,sy,tile,flipx,flipy;
	
				dirtybuffer.write(offs, 0);
	
				sx = offs % 32;
				sy = offs / 32;
				flipx = 0;
				flipy = 0;
	
				if (flipscreen != 0)
				{
					flipx = flipx!=0?0:1;
					flipy = flipy!=0?0:1;
				}
	
				tile = gs_videoram.read(offs) + ((charbank & 0x03) << 8);
	
				drawgfx(bitmap_bg,Machine.gfx[0],
						tile,
						((tile & 0x3c0) >> 6) + 16 * charpalbank,
						flipx,flipy,
						8*sx,8*sy,
						null,TRANSPARENCY_NONE,0);
			}
		}
	}
	
	
	public static void render_sprites(mame_bitmap bitmap)
	{
		int offs;
	
		for (offs = 0; offs < gs_spritexy_size[0] - 1; offs+=2)
		{
			int sx,sy,flipx,flipy,spritebank,tile;
	
			if (gs_spritexy_ram.read(offs)!=0xf1)
			{
				spritebank = 0;
				tile = gs_spritetile_ram.read(offs);
				sy = 241-gs_spritexy_ram.read(offs);
				sx = gs_spritexy_ram.read(offs+1)-56;
				flipx = gs_spriteattrib_ram.read(offs) & 0x02;
				flipy = gs_spriteattrib_ram.read(offs) & 0x01;
	
				// Adjust sprites that should be far far right!
				if (sx<0) sx+=256;
	
				// Adjuste for 32x32 tiles(#128-256)
				if (tile > 127)
				{
					spritebank = 1;
					tile -= 128;
					sy-=16;
				}
				if (flipscreen != 0)
				{
					flipx = flipx!=0?0:1;
					flipy = flipy!=0?0:1;
				}
				drawgfx(bitmap,Machine.gfx[1+spritebank],
						tile,
						gs_spritetile_ram.read(offs+1) & 0x3f,
						flipx,flipy,
						sx,sy,
						Machine.visible_area,TRANSPARENCY_COLOR, 15);
			}
		}
	}
	
	public static VhUpdatePtr gsword_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int scrollx=0, scrolly=-(gs_scrolly_ram.read());
	
		render_background(bitmap_bg);
		copyscrollbitmap(bitmap,bitmap_bg,1,new int[]{scrollx},1,new int[]{scrolly},Machine.visible_area,TRANSPARENCY_NONE,0);
		render_sprites(bitmap);
	} };
	
}
