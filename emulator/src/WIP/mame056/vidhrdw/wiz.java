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
import static common.libc.expressions.*;

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
/*TODO*///	
/*TODO*///	
/*TODO*///	static struct rectangle spritevisiblearea =
/*TODO*///	{
/*TODO*///		2*8, 32*8-1,
/*TODO*///		2*8, 30*8-1
/*TODO*///	};
/*TODO*///	
/*TODO*///	static struct rectangle spritevisibleareaflipx =
/*TODO*///	{
/*TODO*///		0*8, 30*8-1,
/*TODO*///		2*8, 30*8-1
/*TODO*///	};
/*TODO*///	
/*TODO*///	unsigned char *wiz_videoram2;
/*TODO*///	unsigned char *wiz_colorram2;
/*TODO*///	unsigned char *wiz_attributesram;
/*TODO*///	unsigned char *wiz_attributesram2;
/*TODO*///	
/*TODO*///	static int flipx, flipy;
/*TODO*///	static int bgpen;
/*TODO*///	
/*TODO*///	unsigned char *wiz_sprite_bank;
/*TODO*///	static unsigned char char_bank[2];
/*TODO*///	static unsigned char palbank[2];
/*TODO*///	static int palette_bank;
/*TODO*///	
/*TODO*///	
/*TODO*///	public static VhStartPtr wiz_vh_start = new VhStartPtr() { public int handler() 
/*TODO*///	{
/*TODO*///		if (generic_vh_start())
/*TODO*///			return 1;
/*TODO*///	
/*TODO*///		state_save_register_UINT8("wiz", 0, "char_bank",   char_bank,   2);
/*TODO*///		state_save_register_UINT8("wiz", 0, "palbank",	   palbank,     2);
/*TODO*///		state_save_register_int  ("wiz", 0, "flipx",       &flipx);
/*TODO*///		state_save_register_int  ("wiz", 0, "flipy",       &flipy);
/*TODO*///		state_save_register_int  ("wiz", 0, "bgpen",       &bgpen);
/*TODO*///	
/*TODO*///		return 0;
/*TODO*///	} };
/*TODO*///	
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
	
/*TODO*///	public static WriteHandlerPtr wiz_attributes_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		if ((offset & 1) && wiz_attributesram[offset] != data)
/*TODO*///		{
/*TODO*///			int i;
/*TODO*///	
/*TODO*///	
/*TODO*///			for (i = offset / 2;i < videoram_size;i += 32)
/*TODO*///			{
/*TODO*///				dirtybuffer[i] = 1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	
/*TODO*///		wiz_attributesram[offset] = data;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr wiz_palettebank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		if (palbank[offset] != (data & 1))
/*TODO*///		{
/*TODO*///			palbank[offset] = data & 1;
/*TODO*///			palette_bank = palbank[0] + 2 * palbank[1];
/*TODO*///	
/*TODO*///			memset(dirtybuffer,1,videoram_size);
/*TODO*///		}
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr wiz_bgcolor_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		bgpen = data;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr wiz_char_bank_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		if (char_bank[offset] != (data & 1))
/*TODO*///		{
/*TODO*///			char_bank[offset] = data & 1;
/*TODO*///			memset(dirtybuffer,1,videoram_size);
/*TODO*///		}
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr wiz_flipx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///	    if (flipx != data)
/*TODO*///	    {
/*TODO*///			flipx = data;
/*TODO*///	
/*TODO*///			memset(dirtybuffer, 1, videoram_size);
/*TODO*///	    }
/*TODO*///	} };
/*TODO*///	
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr wiz_flipy_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///	    if (flipy != data)
/*TODO*///	    {
/*TODO*///			flipy = data;
/*TODO*///	
/*TODO*///			memset(dirtybuffer, 1, videoram_size);
/*TODO*///	    }
/*TODO*///	} };
/*TODO*///	
/*TODO*///	static void draw_background(struct mame_bitmap *bitmap, int bank, int colortype)
/*TODO*///	{
/*TODO*///		int offs;
/*TODO*///	
/*TODO*///		/* for every character in the Video RAM, check if it has been modified */
/*TODO*///		/* since last time and update it accordingly. */
/*TODO*///	
/*TODO*///		for (offs = videoram_size - 1;offs >= 0;offs--)
/*TODO*///		{
/*TODO*///			int scroll,sx,sy,col;
/*TODO*///	
/*TODO*///			sx = offs % 32;
/*TODO*///			sy = offs / 32;
/*TODO*///	
/*TODO*///			if (colortype)
/*TODO*///			{
/*TODO*///				col = (wiz_attributesram[2 * sx + 1] & 0x07);
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				col = (wiz_attributesram[2 * (offs % 32) + 1] & 0x04) + (videoram[offs] & 3);
/*TODO*///			}
/*TODO*///	
/*TODO*///			scroll = (8*sy + 256 - wiz_attributesram[2 * sx]) % 256;
/*TODO*///			if (flipy)
/*TODO*///			{
/*TODO*///			   scroll = (248 - scroll) % 256;
/*TODO*///			}
/*TODO*///			if (flipx) sx = 31 - sx;
/*TODO*///	
/*TODO*///	
/*TODO*///			drawgfx(bitmap,Machine.gfx[bank],
/*TODO*///				videoram[offs],
/*TODO*///				col + 8 * palette_bank,
/*TODO*///				flipx,flipy,
/*TODO*///				8*sx,scroll,
/*TODO*///				&Machine.visible_area,TRANSPARENCY_PEN,0);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void draw_foreground(struct mame_bitmap *bitmap, int colortype)
/*TODO*///	{
/*TODO*///		int offs;
/*TODO*///	
/*TODO*///		/* draw the frontmost playfield. They are characters, but draw them as sprites. */
/*TODO*///		for (offs = videoram_size - 1;offs >= 0;offs--)
/*TODO*///		{
/*TODO*///			int scroll,sx,sy,col;
/*TODO*///	
/*TODO*///	
/*TODO*///			sx = offs % 32;
/*TODO*///			sy = offs / 32;
/*TODO*///	
/*TODO*///			if (colortype)
/*TODO*///			{
/*TODO*///				col = (wiz_attributesram2[2 * sx + 1] & 0x07);
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				col = (wiz_colorram2[offs] & 0x07);
/*TODO*///			}
/*TODO*///	
/*TODO*///			scroll = (8*sy + 256 - wiz_attributesram2[2 * sx]) % 256;
/*TODO*///			if (flipy)
/*TODO*///			{
/*TODO*///			   scroll = (248 - scroll) % 256;
/*TODO*///			}
/*TODO*///			if (flipx) sx = 31 - sx;
/*TODO*///	
/*TODO*///	
/*TODO*///			drawgfx(bitmap,Machine.gfx[char_bank[1]],
/*TODO*///				wiz_videoram2[offs],
/*TODO*///				col + 8 * palette_bank,
/*TODO*///				flipx,flipy,
/*TODO*///				8*sx,scroll,
/*TODO*///				&Machine.visible_area,TRANSPARENCY_PEN,0);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void draw_sprites(struct mame_bitmap *bitmap, unsigned char* sprite_ram,
/*TODO*///	                         int bank, const struct rectangle* visible_area)
/*TODO*///	{
/*TODO*///		int offs;
/*TODO*///	
/*TODO*///		for (offs = spriteram_size - 4;offs >= 0;offs -= 4)
/*TODO*///		{
/*TODO*///			int sx,sy;
/*TODO*///	
/*TODO*///	
/*TODO*///			sx = sprite_ram[offs + 3];
/*TODO*///			sy = sprite_ram[offs];
/*TODO*///	
/*TODO*///			if (!sx || !sy) continue;
/*TODO*///	
/*TODO*///			if ( flipx) sx = 240 - sx;
/*TODO*///			if (flipy == 0) sy = 240 - sy;
/*TODO*///	
/*TODO*///			drawgfx(bitmap,Machine.gfx[bank],
/*TODO*///					sprite_ram[offs + 1],
/*TODO*///					(sprite_ram[offs + 2] & 0x07) + 8 * palette_bank,
/*TODO*///					flipx,flipy,
/*TODO*///					sx,sy,
/*TODO*///					visible_area,TRANSPARENCY_PEN,0);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************************
/*TODO*///	
/*TODO*///	  Draw the game screen in the given mame_bitmap.
/*TODO*///	  Do NOT call osd_update_display() from this function, it will be called by
/*TODO*///	  the main emulation engine.
/*TODO*///	
/*TODO*///	***************************************************************************/
/*TODO*///	public static VhUpdatePtr wiz_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
/*TODO*///	{
/*TODO*///		int bank;
/*TODO*///		const struct rectangle* visible_area;
/*TODO*///	
/*TODO*///		fillbitmap(bitmap,Machine.pens[bgpen],&Machine.visible_area);
/*TODO*///		draw_background(bitmap, 2 + ((char_bank[0] << 1) | char_bank[1]), 0);
/*TODO*///		draw_foreground(bitmap, 0);
/*TODO*///	
/*TODO*///		visible_area = flipx ? &spritevisibleareaflipx : &spritevisiblearea;
/*TODO*///	
/*TODO*///	    bank = 7 + *wiz_sprite_bank;
/*TODO*///	
/*TODO*///		draw_sprites(bitmap, spriteram_2, 6,    visible_area);
/*TODO*///		draw_sprites(bitmap, spriteram  , bank, visible_area);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	
/*TODO*///	public static VhUpdatePtr stinger_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
/*TODO*///	{
/*TODO*///		fillbitmap(bitmap,Machine.pens[bgpen],&Machine.visible_area);
/*TODO*///		draw_background(bitmap, 2 + char_bank[0], 1);
/*TODO*///		draw_foreground(bitmap, 1);
/*TODO*///		draw_sprites(bitmap, spriteram_2, 4, &Machine.visible_area);
/*TODO*///		draw_sprites(bitmap, spriteram  , 5, &Machine.visible_area);
/*TODO*///	} };
}
