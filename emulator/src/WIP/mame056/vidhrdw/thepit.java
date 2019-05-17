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
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.inptport.*;
import static mame056.sound.mixer.*;
import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;


public class thepit
{
	public static UBytePtr thepit_attributesram = new UBytePtr();
	
	static int graphics_bank = 0;
	
	static rectangle spritevisiblearea = new rectangle
	(
		2*8+1, 32*8-1,
		2*8, 30*8-1
        );
	
        static rectangle spritevisibleareaflipx = new rectangle
	(
		0*8, 30*8-2,
		2*8, 30*8-1
        );
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  bit 7 -- 220 ohm resistor  -- BLUE
	        -- 470 ohm resistor  -- BLUE
	        -- 220 ohm resistor  -- GREEN
	        -- 470 ohm resistor  -- GREEN
	        -- 1  kohm resistor  -- GREEN
	        -- 220 ohm resistor  -- RED
	        -- 470 ohm resistor  -- RED
	  bit 0 -- 1  kohm resistor  -- RED
	
	
	***************************************************************************/
	public static VhConvertColorPromPtr thepit_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
                int _palette = 0;
	
	
		/* first of all, allocate primary colors for the background and foreground */
		/* this is wrong, but I don't know where to pick the colors from */
		for (i = 0;i < 8;i++)
		{
			palette[_palette++] = (char) (0xff * ((i >> 2) & 1));
			palette[_palette++] = (char) (0xff * ((i >> 1) & 1));
			palette[_palette++] = (char) (0xff * ((i >> 0) & 1));
		}
	
		for (i = 0;i < Machine.drv.total_colors-8;i++)
		{
			int bit0,bit1,bit2;
	
	
			bit0 = (color_prom.read(i) >> 0) & 0x01;
			bit1 = (color_prom.read(i) >> 1) & 0x01;
			bit2 = (color_prom.read(i) >> 2) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			bit0 = (color_prom.read(i) >> 3) & 0x01;
			bit1 = (color_prom.read(i) >> 4) & 0x01;
			bit2 = (color_prom.read(i) >> 5) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			bit0 = 0;
			bit1 = (color_prom.read(i) >> 6) & 0x01;
			bit2 = (color_prom.read(i) >> 7) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
		}
	
		for (i = 0;i < Machine.drv.color_table_len;i++)
		{
			colortable[i] = (char) (i + 8);
		}
            }
        };
	
	
	/***************************************************************************
	
	 Super Mouse has 5 bits per gun (maybe 6 for green), exact weights are
	 unknown.
	
	***************************************************************************/
	public static VhConvertColorPromPtr suprmous_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
                int _palette = 0;
	
	
		/* first of all, allocate primary colors for the background and foreground */
		/* this is wrong, but I don't know where to pick the colors from */
		for (i = 0;i < 8;i++)
		{
			palette[_palette++] = (char) (0xff * ((i >> 2) & 1));
			palette[_palette++] = (char) (0xff * ((i >> 1) & 1));
			palette[_palette++] = (char) (0xff * ((i >> 0) & 1));
		}
	
		for (i = 0;i < Machine.drv.total_colors-8;i++)
		{
			int bit0,bit1,bit2,bit3,bit4;
	
	
			bit0 = (color_prom.read(i+0x20) >> 6) & 0x01;
			bit1 = (color_prom.read(i+0x20) >> 7) & 0x01;
			bit2 = (color_prom.read(i) >> 0) & 0x01;
			bit3 = (color_prom.read(i) >> 1) & 0x01;
			bit4 = (color_prom.read(i) >> 2) & 0x01;
			palette[_palette++] = (char) (0x10 * bit0 + 0x20 * bit1 + 0x30 * bit2 + 0x40 * bit3 + 0x50 * bit4);
			bit0 = (color_prom.read(i+0x20) >> 1) & 0x01;
			bit1 = (color_prom.read(i+0x20) >> 2) & 0x01;
			bit2 = (color_prom.read(i+0x20) >> 3) & 0x01;
			bit3 = (color_prom.read(i+0x20) >> 4) & 0x01;
			bit4 = (color_prom.read(i+0x20) >> 5) & 0x01;
			palette[_palette++] = (char) (0x50 * bit0 + 0x40 * bit1 + 0x30 * bit2 + 0x20 * bit3 + 0x10 * bit4);
			bit0 = (color_prom.read(i) >> 3) & 0x01;
			bit1 = (color_prom.read(i) >> 4) & 0x01;
			bit2 = (color_prom.read(i) >> 5) & 0x01;
			bit3 = (color_prom.read(i) >> 6) & 0x01;
			bit4 = (color_prom.read(i) >> 7) & 0x01;
			palette[_palette++] = (char) (0x50 * bit0 + 0x40 * bit1 + 0x30 * bit2 + 0x20 * bit3 + 0x10 * bit4);
		}
	
		for (i = 0;i < Machine.drv.color_table_len;i++)
		{
			colortable[i] = (char) (i + 8);
		}
            }
        };
	
	public static WriteHandlerPtr thepit_attributes_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if ((offset & 1)!=0 && thepit_attributesram.read(offset) != data)
		{
			int i;
	
	
			for (i = offset / 2;i < videoram_size[0];i += 32)
				dirtybuffer[i] = 1;
		}
	
		thepit_attributesram.write(offset, data);
	} };
	
	
	public static WriteHandlerPtr intrepid_graphics_bank_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		set_vh_global_attribute(new int[]{graphics_bank}, data << 1);
	} };
	
	
	public static ReadHandlerPtr thepit_input_port_0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* Read either the real or the fake input ports depending on the
		   horizontal flip switch. (This is how the real PCB does it) */
		if (flip_screen_x[0] != 0)
		{
			return input_port_3_r.handler(offset);
		}
		else
		{
			return input_port_0_r.handler(offset);
		}
	} };
	
	
	public static WriteHandlerPtr thepit_sound_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		mixer_sound_enable_global_w(data);
	} };
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	static void drawtiles(mame_bitmap bitmap,int priority)
	{
		int offs,spacechar=0;
	
	
		if (priority == 1)
		{
			/* find the space character */
			while ((Machine.gfx[0].pen_usage[spacechar] & ~1)!=0) spacechar++;
		}
	
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			int bgcolor;
	
	
			bgcolor = (colorram.read(offs) & 0x70) >> 4;
	
			if ((priority == 0 && dirtybuffer[offs]!=0) ||
					(priority == 1 && bgcolor != 0 && (colorram.read(offs) & 0x80) == 0))
			{
				int sx,sy,code,bank,color;
	
	
				dirtybuffer[offs] = 0;
	
				sx = (offs % 32);
				sy = 8*(offs / 32);
	
				if (priority == 0)
				{
					code = videoram.read(offs);
					bank = graphics_bank;
				}
				else
				{
					code = spacechar;
					bank = 0;
	
					sy = (sy - thepit_attributesram.read(2 * sx)) & 0xff;
				}
	
				if (flip_screen_x[0] != 0) sx = 31 - sx;
				if (flip_screen_y[0] != 0) sy = 248 - sy;
	
				color = colorram.read(offs) & (Machine.drv.gfxdecodeinfo[bank].total_color_codes - 1);
	
				/* set up the background color */
				Machine.gfx[bank].
						colortable.write(color * Machine.gfx[bank].color_granularity,
						Machine.pens[bgcolor]);
	
				drawgfx(priority == 0 ? tmpbitmap : bitmap,Machine.gfx[bank],
						code,
						color,
						flip_screen_x[0],flip_screen_y[0],
						8*sx,sy,
						null,TRANSPARENCY_NONE,0);
			}
		}
	
	
		/* copy the temporary bitmap to the screen */
		if (priority == 0)
		{
			int i; 
                        int[] scroll = new int[32];
	
			if (flip_screen_x[0] != 0)
			{
				for (i = 0;i < 32;i++)
				{
					scroll[31-i] = -thepit_attributesram.read(2 * i);
					if (flip_screen_y[0] != 0) scroll[31-i] = -scroll[31-i];
				}
			}
			else
			{
				for (i = 0;i < 32;i++)
				{
					scroll[i] = -thepit_attributesram.read(2 * i);
					if (flip_screen_y[0] != 0) scroll[i] = -scroll[i];
				}
			}
	
			copyscrollbitmap(bitmap,tmpbitmap,0,new int[]{0},32,scroll,Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	}
	
	static void drawsprites(mame_bitmap bitmap,int priority)
	{
		int offs;
	
	
		/* draw low priority sprites */
		for (offs = spriteram_size[0] - 4;offs >= 0;offs -= 4)
		{
			if (((spriteram.read(offs + 2) & 0x08) >> 3) == priority)
			{
				int sx,sy,flipx,flipy;
	
	
				if (spriteram.read(offs + 0) == 0 ||
					spriteram.read(offs + 3) == 0)
				{
					continue;
				}
	
				sx = (spriteram.read(offs+3) + 1) & 0xff;
				sy = 240 - spriteram.read(offs);
	
				flipx = spriteram.read(offs + 1) & 0x40;
				flipy = spriteram.read(offs + 1) & 0x80;
	
				if (flip_screen_x[0] != 0)
				{
					sx = 242 - sx;
					flipx = flipx!=0?0:1;
				}
	
				if (flip_screen_y[0] != 0)
				{
					sy = 240 - sy;
					flipy = flipy!=0?0:1;
				}
	
				/* Sprites 0-3 are drawn one pixel to the left */
				if (offs <= 3*4) sy++;
	
				drawgfx(bitmap,Machine.gfx[graphics_bank | 1],
						spriteram.read(offs + 1) & 0x3f,
						spriteram.read(offs + 2) & 0x07,
						flipx,flipy,
						sx,sy,
						(flip_screen_x[0] & 1) !=0? spritevisibleareaflipx : spritevisiblearea,TRANSPARENCY_PEN,0);
			}
		}
	}
	
	
	public static VhUpdatePtr thepit_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		if (full_refresh != 0)
		{
			memset(dirtybuffer, 1, videoram_size[0]);
		}
	
	
		/* low priority tiles */
		drawtiles(bitmap,0);
	
		/* low priority sprites */
		drawsprites(bitmap,0);
	
		/* high priority tiles */
		drawtiles(bitmap,1);
	
		/* high priority sprites */
		drawsprites(bitmap,1);
	} };
}
