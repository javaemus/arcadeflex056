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

import static mame056.common.*;
import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class wiz
{
	
	
	static rectangle spritevisiblearea = new rectangle
	(
		2*8, 32*8-1,
		2*8, 30*8-1
        );
	
	static rectangle spritevisibleareaflipx = new rectangle
	(
		0*8, 30*8-1,
		2*8, 30*8-1
        );
	
	public static UBytePtr wiz_videoram2 = new UBytePtr();
	public static UBytePtr wiz_colorram2 = new UBytePtr();
	public static UBytePtr wiz_attributesram = new UBytePtr();
	public static UBytePtr wiz_attributesram2 = new UBytePtr();
	
	static int flipx, flipy;
	static int bgpen;
	
	public static UBytePtr wiz_sprite_bank = new UBytePtr();
	static int[] char_bank=new int[2];
	static int[] palbank=new int[2];
	static int palette_bank;
	
	
	public static VhStartPtr wiz_vh_start = new VhStartPtr() { public int handler() 
	{
		if (generic_vh_start.handler()!= 0)
			return 1;
	
/*TODO*///		state_save_register_UINT8("wiz", 0, "char_bank",   char_bank,   2);
/*TODO*///		state_save_register_UINT8("wiz", 0, "palbank",	   palbank,     2);
/*TODO*///		state_save_register_int  ("wiz", 0, "flipx",       &flipx);
/*TODO*///		state_save_register_int  ("wiz", 0, "flipy",       &flipy);
/*TODO*///		state_save_register_int  ("wiz", 0, "bgpen",       &bgpen);
	
		return 0;
	} };
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Stinger has three 256x4 palette PROMs (one per gun).
	  The palette PROMs are connected to the RGB output this way:
	
	  bit 3 -- 100 ohm resistor  -- RED/GREEN/BLUE
	        -- 220 ohm resistor  -- RED/GREEN/BLUE
	        -- 470 ohm resistor  -- RED/GREEN/BLUE
	  bit 0 -- 1  kohm resistor  -- RED/GREEN/BLUE
	
	***************************************************************************/
	public static VhConvertColorPromPtr wiz_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
                int _palette = 0;
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int bit0,bit1,bit2,bit3;
	
	
			bit0 = (color_prom.read(0) >> 0) & 0x01;
			bit1 = (color_prom.read(0) >> 1) & 0x01;
			bit2 = (color_prom.read(0) >> 2) & 0x01;
			bit3 = (color_prom.read(0) >> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x42 * bit2 + 0x90 * bit3);
			bit0 = (color_prom.read(Machine.drv.total_colors) >> 0) & 0x01;
			bit1 = (color_prom.read(Machine.drv.total_colors) >> 1) & 0x01;
			bit2 = (color_prom.read(Machine.drv.total_colors) >> 2) & 0x01;
			bit3 = (color_prom.read(Machine.drv.total_colors) >> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x42 * bit2 + 0x90 * bit3);
			bit0 = (color_prom.read(2*Machine.drv.total_colors) >> 0) & 0x01;
			bit1 = (color_prom.read(2*Machine.drv.total_colors) >> 1) & 0x01;
			bit2 = (color_prom.read(2*Machine.drv.total_colors) >> 2) & 0x01;
			bit3 = (color_prom.read(2*Machine.drv.total_colors) >> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x42 * bit2 + 0x90 * bit3);
	
			color_prom.inc();
		}
            }
        };
	
	public static WriteHandlerPtr wiz_attributes_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (((offset & 1)!=0) && wiz_attributesram.read(offset) != data)
		{
			int i;
	
	
			for (i = offset / 2;i < videoram_size[0];i += 32)
			{
				dirtybuffer[i] = 1;
			}
		}
	
		wiz_attributesram.write(offset, data);
	} };
	
	public static WriteHandlerPtr wiz_palettebank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (palbank[offset] != (data & 1))
		{
			palbank[offset] = data & 1;
			palette_bank = palbank[0] + 2 * palbank[1];
	
			memset(dirtybuffer,1,videoram_size[0]);
		}
	} };
	
	public static WriteHandlerPtr wiz_bgcolor_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		bgpen = data;
	} };
	
	public static WriteHandlerPtr wiz_char_bank_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (char_bank[offset] != (data & 1))
		{
			char_bank[offset] = data & 1;
			memset(dirtybuffer,1,videoram_size[0]);
		}
	} };
	
	public static WriteHandlerPtr wiz_flipx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    if (flipx != data)
	    {
			flipx = data;
	
			memset(dirtybuffer, 1, videoram_size[0]);
	    }
	} };
	
	
	public static WriteHandlerPtr wiz_flipy_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    if (flipy != data)
	    {
			flipy = data;
	
			memset(dirtybuffer, 1, videoram_size[0]);
	    }
	} };
	
	static void draw_background(mame_bitmap bitmap, int bank, int colortype)
	{
		int offs;
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
	
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			int scroll,sx,sy,col;
	
			sx = offs % 32;
			sy = offs / 32;
	
			if (colortype != 0)
			{
				col = (wiz_attributesram.read(2 * sx + 1) & 0x07);
			}
			else
			{
				col = (wiz_attributesram.read(2 * (offs % 32) + 1) & 0x04) + (videoram.read(offs) & 3);
			}
	
			scroll = (8*sy + 256 - wiz_attributesram.read(2 * sx)) % 256;
			if (flipy != 0)
			{
			   scroll = (248 - scroll) % 256;
			}
			if (flipx != 0) sx = 31 - sx;
	
	
			drawgfx(bitmap,Machine.gfx[bank],
				videoram.read(offs),
				col + 8 * palette_bank,
				flipx,flipy,
				8*sx,scroll,
				Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	}
	
	static void draw_foreground(mame_bitmap bitmap, int colortype)
	{
		int offs;
	
		/* draw the frontmost playfield. They are characters, but draw them as sprites. */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			int scroll,sx,sy,col;
	
	
			sx = offs % 32;
			sy = offs / 32;
	
			if (colortype != 0)
			{
				col = (wiz_attributesram2.read(2 * sx + 1) & 0x07);
			}
			else
			{
				col = (wiz_colorram2.read(offs) & 0x07);
			}
	
			scroll = (8*sy + 256 - wiz_attributesram2.read(2 * sx)) % 256;
			if (flipy != 0)
			{
			   scroll = (248 - scroll) % 256;
			}
			if (flipx != 0) sx = 31 - sx;
	
	
			drawgfx(bitmap,Machine.gfx[char_bank[1]],
				wiz_videoram2.read(offs),
				col + 8 * palette_bank,
				flipx,flipy,
				8*sx,scroll,
				Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	}
	
	static void draw_sprites(mame_bitmap bitmap, UBytePtr sprite_ram,
	                         int bank, rectangle visible_area)
	{
		int offs;
	
		for (offs = spriteram_size[0] - 4;offs >= 0;offs -= 4)
		{
			int sx,sy;
	
	
			sx = sprite_ram.read(offs + 3);
			sy = sprite_ram.read(offs);
	
			if (sx==0 || sy==0) continue;
	
			if (flipx != 0) sx = 240 - sx;
			if (flipy == 0) sy = 240 - sy;
	
			drawgfx(bitmap,Machine.gfx[bank],
					sprite_ram.read(offs + 1),
					(sprite_ram.read(offs + 2) & 0x07) + 8 * palette_bank,
					flipx,flipy,
					sx,sy,
					visible_area,TRANSPARENCY_PEN,0);
		}
	}
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr wiz_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int bank;
		rectangle visible_area;
	
		fillbitmap(bitmap,Machine.pens[bgpen],Machine.visible_area);
		draw_background(bitmap, 2 + ((char_bank[0] << 1) | char_bank[1]), 0);
		draw_foreground(bitmap, 0);
	
		visible_area = flipx!=0 ? spritevisibleareaflipx : spritevisiblearea;
	
                bank = 7 + wiz_sprite_bank.read();
	
		draw_sprites(bitmap, spriteram_2, 6,    visible_area);
		draw_sprites(bitmap, spriteram  , bank, visible_area);
	} };
	
	
	public static VhUpdatePtr stinger_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		fillbitmap(bitmap,Machine.pens[bgpen],Machine.visible_area);
		draw_background(bitmap, 2 + char_bank[0], 1);
		draw_foreground(bitmap, 1);
		draw_sprites(bitmap, spriteram_2, 4, Machine.visible_area);
		draw_sprites(bitmap, spriteram  , 5, Machine.visible_area);
	} };
}
