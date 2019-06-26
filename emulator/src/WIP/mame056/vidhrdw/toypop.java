/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import common.ptr.UBytePtr;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import common.subArrays.IntArray;
import static mame056.commonH.*;
import static mame056.mame.*;
import static mame056.cpuexec.*;
import static mame056.drawgfxH.*;
import static mame056.drawgfx.*;
import static mame056.vidhrdw.generic.*;

public class toypop
{
	
	public static UBytePtr toypop_bg_image = new UBytePtr(128*1024);
	static int flipscreen, palettebank;
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  toypop has three 256x4 palette PROM and two 256x8 color lookup table PROMs
	  (one for characters, one for sprites).
	
	
	***************************************************************************/
	public static VhConvertColorPromPtr toypop_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
	
		for (i = 0;i < 256;i++)
		{
			int bit0,bit1,bit2,bit3;
	
			// red component
			bit0 = (color_prom.read(i) >> 0) & 0x01;
			bit1 = (color_prom.read(i) >> 1) & 0x01;
			bit2 = (color_prom.read(i) >> 2) & 0x01;
			bit3 = (color_prom.read(i) >> 3) & 0x01;
			palette[3*i] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
			// green component
			bit0 = (color_prom.read(i+0x100) >> 0) & 0x01;
			bit1 = (color_prom.read(i+0x100) >> 1) & 0x01;
			bit2 = (color_prom.read(i+0x100) >> 2) & 0x01;
			bit3 = (color_prom.read(i+0x100) >> 3) & 0x01;
			palette[3*i + 1] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
			// blue component
			bit0 = (color_prom.read(i+0x200) >> 0) & 0x01;
			bit1 = (color_prom.read(i+0x200) >> 1) & 0x01;
			bit2 = (color_prom.read(i+0x200) >> 2) & 0x01;
			bit3 = (color_prom.read(i+0x200) >> 3) & 0x01;
			palette[3*i + 2] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
		}
	
		// characters
		for (i = 0;i < 256;i++)
			colortable[i] = (char) (color_prom.read(i + 0x300) | 0x70);
	
		// sprites
		for (i = 256;i < Machine.drv.color_table_len;i++)
			colortable[i] = color_prom.read(i + 0x400);	// 0x500-5ff
            }
        };
	
	public static WriteHandlerPtr toypop_palettebank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (offset != 0)
			palettebank = 0xe0;
		else
			palettebank = 0x60;
	} };
	
	public static WriteHandlerPtr toypop_flipscreen_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		flipscreen = offset;
	}};
	
	public static ReadHandlerPtr toypop_merged_background_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                int data1, data2;
	
		// 0x0a0b0c0d is read as 0xabcd
		data1 = toypop_bg_image.read(2*offset);
		data2 = toypop_bg_image.read(2*offset + 1);
		return ((data1 & 0xf00) << 4) | ((data1 & 0xf) << 8) | ((data2 & 0xf00) >> 4) | (data2 & 0xf);
            }
        };
	
	public static WriteHandlerPtr toypop_merged_background_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		// 0xabcd is written as 0x0a0b0c0d in the background image
		//if (ACCESSING_MSB)
			toypop_bg_image.write(2*offset, ((data & 0xf00) >> 8) | ((data & 0xf000) >> 4));
	
		//if (ACCESSING_LSB)
			toypop_bg_image.write(2*offset+1, (data & 0xf) | ((data & 0xf0) << 4));
	}};
	
	public static void toypop_draw_sprite(mame_bitmap dest, int code, int color,
		int flipx,int flipy,int sx,int sy)
	{
		drawgfx(dest,Machine.gfx[1],code,color,flipx,flipy,sx,sy,Machine.visible_area,TRANSPARENCY_COLOR,0xff);
	}
	
	public static void draw_background_and_characters(mame_bitmap bitmap)
	{
		int offs, x, y;
		char[] scanline=new char[288];
                //scanline.offset=0;
                //toypop_bg_image.offset=0;
	
		// copy the background image from RAM (0x190200-0x19FDFF) to bitmap
		if (flipscreen != 0)
		{
			offs = 0xFDFE/2;
			for (y = 0; y < 224; y++)
			{
				for (x = 0; x < 288; x+=2)
				{
					int data = toypop_bg_image.read(offs);
					scanline[x]   = (char) data;
					scanline[x+1] = (char) (data >> 8);
					offs--;
				}
				draw_scanline8(bitmap, 0, y, 288, new UBytePtr(scanline), new IntArray(Machine.pens, palettebank), -1);
			}
		}
		else
		{
                        //toypop_bg_image.offset=0;
			offs = 0x200/2;
			for (y = 0; y < 224; y++)
			{
                            //toypop_bg_image.offset=0;
				for (x = 0; x < 288; x+=2)
				{
                                    
					int data = toypop_bg_image.read(offs);
					scanline[x]   = (char) (data >> 8);
					scanline[x+1] = (char) data;
					offs++;
				}
				draw_scanline8(bitmap, 0, y, 288, new UBytePtr(scanline), new IntArray(Machine.pens, palettebank), -1);
			}
		}
	
		// draw every character in the Video RAM (videoram_size = 1024)
		for (offs = 1021; offs >= 2; offs--) {
			if (offs >= 960) {
				// Draw the 2 columns at left
				x = ((offs >> 5) - 30) << 3;
				y = ((offs & 0x1f) - 2) << 3;
			} else if (offs < 64) {
				// Draw the 2 columns at right
				x = ((offs >> 5) + 34) << 3;
				y = ((offs & 0x1f) - 2) << 3;
			} else {
				// draw the rest of the screen
				x = ((offs & 0x1f) + 2) << 3;
				y = ((offs >> 5) - 2) << 3;
			}
			if (flipscreen != 0) {
				x = 280 - x;
				y = 216 - y;
			}
			drawgfx(bitmap,Machine.gfx[0],videoram.read(offs),colorram.read(offs),flipscreen,flipscreen,x,y,null,TRANSPARENCY_PEN,0);
		}
	}
	
	public static VhUpdatePtr toypop_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs, x, y;
	
		draw_background_and_characters(bitmap);
	
		// Draw the sprites
		for (offs = 0;offs < spriteram_size[0];offs += 2) {
			// is it on?
			if ((spriteram_2.read(offs)) != 0xe9) {
				int sprite = spriteram.read(offs);
				int color = spriteram.read(offs+1);
				int flipx = spriteram_3.read(offs) & 1;
				int flipy = spriteram_3.read(offs) & 2;
	
				x = (spriteram_2.read(offs+1) | ((spriteram_3.read(offs+1) & 1) << 8)) - 71;
				y = 217 - spriteram_2.read(offs);
				if (flipscreen != 0) {
					flipx = flipx!=0?0:1;
					flipy = flipy!=0?0:1;
				}
	
				switch (spriteram_3.read(offs) & 0x0c)
				{
					case 0:		/* normal size */
						toypop_draw_sprite(bitmap,sprite,color,flipx,flipy,x,y);
						break;
					case 4:		/* 2x horizontal */
						sprite &= ~1;
						if (flipx == 0) {
							toypop_draw_sprite(bitmap,1+sprite,color,0,flipy,x+16,y);
							toypop_draw_sprite(bitmap,sprite,color,0,flipy,x,y);
						} else {
							toypop_draw_sprite(bitmap,sprite,color,1,flipy,x+16,y);
							toypop_draw_sprite(bitmap,1+sprite,color,1,flipy,x,y);
						}
						break;
					case 8:		/* 2x vertical */
						sprite &= ~2;
						if (flipy == 0) {
							toypop_draw_sprite(bitmap,sprite,color,flipx,0,x,y-16);
							toypop_draw_sprite(bitmap,2+sprite,color,flipx,0,x,y);
						} else {
							toypop_draw_sprite(bitmap,2+sprite,color,flipx,1,x,y-16);
							toypop_draw_sprite(bitmap,sprite,color,flipx,1,x,y);
						}
						break;
					case 12:		/* 2x both ways */
						sprite &= ~3;
						if ((flipy==0) && (flipx==0)) {
							toypop_draw_sprite(bitmap,2+sprite,color,0,0,x,y);
							toypop_draw_sprite(bitmap,3+sprite,color,0,0,x+16,y);
							toypop_draw_sprite(bitmap,sprite,color,0,0,x,y-16);
							toypop_draw_sprite(bitmap,1+sprite,color,0,0,x+16,y-16);
						} else if (flipy!=0 && flipx!=0) {
							toypop_draw_sprite(bitmap,1+sprite,color,1,1,x,y);
							toypop_draw_sprite(bitmap,sprite,color,1,1,x+16,y);
							toypop_draw_sprite(bitmap,3+sprite,color,1,1,x,y-16);
							toypop_draw_sprite(bitmap,2+sprite,color,1,1,x+16,y-16);
						} else if (flipx!=0) {
							toypop_draw_sprite(bitmap,3+sprite,color,1,0,x,y);
							toypop_draw_sprite(bitmap,2+sprite,color,1,0,x+16,y);
							toypop_draw_sprite(bitmap,1+sprite,color,1,0,x,y-16);
							toypop_draw_sprite(bitmap,sprite,color,1,0,x+16,y-16);
						} else {	// flipy
							toypop_draw_sprite(bitmap,sprite,color,0,1,x,y);
							toypop_draw_sprite(bitmap,1+sprite,color,0,1,x+16,y);
							toypop_draw_sprite(bitmap,2+sprite,color,0,1,x,y-16);
							toypop_draw_sprite(bitmap,3+sprite,color,0,1,x+16,y-16);
						}
						break;
				}
			}
		}
	} };
}
