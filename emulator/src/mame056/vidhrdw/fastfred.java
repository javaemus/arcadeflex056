/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

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

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class fastfred
{
	
	
	public static UBytePtr fastfred_attributesram = new UBytePtr();
	static UBytePtr fastfred_color_prom = new UBytePtr();
	
	
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
	
	static int[] character_bank=new int[2];
	static int[] color_bank=new int[2];
	static int canspritesflipx = 0;
	
	public static InitMachinePtr jumpcoas_init_machine = new InitMachinePtr() { public void handler() 
	{
		canspritesflipx = 1;
	} };
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  bit 0 -- 1  kohm resistor  -- RED/GREEN/BLUE
	        -- 470 ohm resistor  -- RED/GREEN/BLUE
	        -- 220 ohm resistor  -- RED/GREEN/BLUE
	  bit 3 -- 100 ohm resistor  -- RED/GREEN/BLUE
	
	***************************************************************************/
	
	static void convert_color(int i, int r, int g, int b)
	{
		int bit0, bit1, bit2, bit3;
		UBytePtr prom = fastfred_color_prom;
		int total = Machine.drv.total_colors;
	
		bit0 = (prom.read(i + 0*total) >> 0) & 0x01;
		bit1 = (prom.read(i + 0*total) >> 1) & 0x01;
		bit2 = (prom.read(i + 0*total) >> 2) & 0x01;
		bit3 = (prom.read(i + 0*total) >> 3) & 0x01;
		r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
		bit0 = (prom.read(i + 1*total) >> 0) & 0x01;
		bit1 = (prom.read(i + 1*total) >> 1) & 0x01;
		bit2 = (prom.read(i + 1*total) >> 2) & 0x01;
		bit3 = (prom.read(i + 1*total) >> 3) & 0x01;
		g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
		bit0 = (prom.read(i + 2*total) >> 0) & 0x01;
		bit1 = (prom.read(i + 2*total) >> 1) & 0x01;
		bit2 = (prom.read(i + 2*total) >> 2) & 0x01;
		bit3 = (prom.read(i + 2*total) >> 3) & 0x01;
		b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	}
        
        public static int TOTAL_COLORS(int gfxn){
            return Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity;
        };
        
	public static void COLOR(char[] colortable, int gfxn, int offs, int value){
            colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs] = (char) value;
        };
	
	public static VhConvertColorPromPtr fastfred_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
		int i;
		int _palPos = 0;
	
	
		fastfred_color_prom = color_prom;	/* we'll need this later */
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int r=0,g=0,b=0;
	
			convert_color(i, r, g, b);
	
			palette[_palPos++] = (char) r;
			palette[_palPos++] = (char) g;
			palette[_palPos++] = (char) b;
		}
	
	
		/* characters and sprites use the same palette */
		for (i = 0;i < TOTAL_COLORS(0);i++)
		{
			int color;
	
			if ((i & 0x07)==0)
			{
				color = 0;
			}
			else
			{
				color = i;
			}
	
			COLOR(colortable,0,i,color);
                        COLOR(colortable,1,i,color);
		}
	}};
	
	
	public static WriteHandlerPtr fastfred_attributes_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (((offset & 1)!=0) && fastfred_attributesram.read(offset) != data)
		{
			int i;
	
	
			for (i = offset / 2;i < videoram_size[0];i += 32)
				dirtybuffer[i] = 1;
		}
	
		fastfred_attributesram.write(offset, data);
	} };
	
	
	public static WriteHandlerPtr fastfred_character_bank_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
                set_vh_global_attribute(new int[]{character_bank[offset]}, data & 0x01);
	
	} };
	
	
	public static WriteHandlerPtr fastfred_color_bank_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		set_vh_global_attribute(new int[]{color_bank[offset]}, data & 0x01);
	} };
	
	
	public static WriteHandlerPtr fastfred_background_color_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int r=0,g=0,b=0;
	
		//logerror("Background color = %02X\n", data);
	
		convert_color(data, r, g, b);
	
		palette_set_color(0,r,g,b);
	} };
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr fastfred_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs, charbank, colorbank;
	
	
		if (full_refresh != 0)
			memset(dirtybuffer,1,videoram_size[0]);
	
	
		charbank   = ((character_bank[1] << 9) | (character_bank[0] << 8));
		colorbank  = ((color_bank[1]     << 4) | (color_bank[0]     << 3));
	
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			int color;
	
			if (dirtybuffer[offs] != 0)
			{
				int sx,sy;
	
				dirtybuffer[offs] = 0;
	
				sx = offs % 32;
				sy = offs / 32;
	
				color = colorbank | (fastfred_attributesram.read(2 * sx + 1) & 0x07);
	
				if (flip_screen_x[0]!=0) sx = 31 - sx;
				if (flip_screen_y[0]!=0) sy = 31 - sy;
	
				drawgfx(tmpbitmap,Machine.gfx[0],
						charbank | videoram.read(offs),
						color,
						flip_screen_x[0],flip_screen_y[0],
						8*sx,8*sy,
						null,TRANSPARENCY_NONE,0);
			}
		}
	
	
		/* copy the temporary bitmap to the screen */
		{
			int i;
                        int[] scroll=new int[32];
	
			if (flip_screen_x[0]!=0)
			{
				for (i = 0;i < 32;i++)
				{
					scroll[31-i] = -fastfred_attributesram.read(2 * i);
					if (flip_screen_y[0]!=0) scroll[31-i] = -scroll[31-i];
				}
			}
			else
			{
				for (i = 0;i < 32;i++)
				{
					scroll[i] = -fastfred_attributesram.read(2 * i);
					if (flip_screen_y[0]!=0) scroll[i] = -scroll[i];
				}
			}
	
			copyscrollbitmap(bitmap,tmpbitmap,0,null,32,scroll,Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	
	
		/* draw the sprites */
		for (offs = spriteram_size[0] - 4;offs >= 0;offs -= 4)
		{
			int code,sx,sy,flipx,flipy;
	
			sx = spriteram.read(offs + 3);
			sy = 240 - spriteram.read(offs);
	
			if (canspritesflipx != 0)
			{
				// Jump Coaster
				code  =  spriteram.read(offs + 1) & 0x3f;
				flipx = ~spriteram.read(offs + 1) & 0x40;
				flipy =  spriteram.read(offs + 1) & 0x80;
			}
			else
			{
				// Fast Freddie
				code  =  spriteram.read(offs + 1) & 0x7f;
				flipx =  0;
				flipy = ~spriteram.read(offs + 1) & 0x80;
			}
	
	
			if (flip_screen_x[0]!=0)
			{
				sx = 240 - sx;
				flipx = (flipx!=0)?0:1;
			}
			if (flip_screen_y[0]!=0)
			{
				sy = 240 - sy;
				flipy = (flipy!=0)?0:1;
			}
	
			drawgfx(bitmap,Machine.gfx[1],
					code,
					colorbank | (spriteram.read(offs + 2) & 0x07),
					flipx,flipy,
					sx,sy,
					flip_screen_x[0]!=0 ? spritevisibleareaflipx : spritevisiblearea,TRANSPARENCY_PEN,0);
		}
	} };
}
