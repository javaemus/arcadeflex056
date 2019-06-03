/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.drivers.zodiack.percuss_hardware;
import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;
import static mame056.usrintrf.*;
import static mame056.memoryH.*;
import static mame056.memory.*;

import static common.ptr.*;
import common.subArrays.IntArray;
import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.inptport.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.driverH.*;
import static mame056.palette.*;

public class zodiack
{
	
	public static UBytePtr zodiack_videoram2 = new UBytePtr();
	public static UBytePtr zodiack_attributesram = new UBytePtr();
	public static UBytePtr zodiack_bulletsram = new UBytePtr();
	public static int[] zodiack_bulletsram_size = new int[2];
	
	
	static int flipscreen;
        
        public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
		
        public static void COLOR(char[] colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs])=(char) value;
        }
	
	public static VhConvertColorPromPtr zodiack_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
		int _palette = 0;
	
	
		/* first, the character/sprite palette */
		for (i = 0;i < Machine.drv.total_colors-1; i++)
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
			bit0 = 0;
			bit1 = (color_prom.read() >> 6) & 0x01;
			bit2 = (color_prom.read() >> 7) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
	
			color_prom.inc();
		}
	
		/* white for bullets */
		palette[_palette++] = 0xff;
		palette[_palette++] = 0xff;
		palette[_palette++] = 0xff;
	
	
		for (i = 0;i < TOTAL_COLORS(0);i+=2)
		{
			COLOR(colortable, 0,i , (32 + (i / 2)));
			COLOR(colortable, 0,i+1, (40 + (i / 2)));
		}
	
		for (i = 0;i < TOTAL_COLORS(3);i++)
		{
			if ((i & 3) == 0)  COLOR(colortable, 3, i, 0);
		}
	
		/* bullet */
		COLOR(colortable, 2, 0, 0);
		COLOR(colortable, 2, 1, 48);
            }
        };
	
	public static WriteHandlerPtr zodiack_attributes_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if ((offset & 1)!=0 && zodiack_attributesram.read(offset) != data)
		{
			int i;
	
	
			for (i = offset / 2;i < videoram_size[0];i += 32)
				dirtybuffer[i] = 1;
		}
	
		zodiack_attributesram.write(offset, data);
	} };
	
	
	public static WriteHandlerPtr zodiac_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (flipscreen != (data!=0?0:1))
		{
			flipscreen = data!=0?0:1;
	
			memset(dirtybuffer, 1, videoram_size[0]);
		}
	} };
	
	
	public static WriteHandlerPtr zodiac_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* Bit 0-1 - coin counters */
		coin_counter_w.handler(0, data & 0x02);
		coin_counter_w.handler(1, data & 0x01);
	
		/* Bit 2 - ???? */
	} };
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	
	public static VhUpdatePtr zodiack_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		/* draw the background characters */
		for (offs = 0; offs < videoram_size[0]; offs++)
		{
			int code,sx,sy,col;
	
	
			if (dirtybuffer[offs]==0)  continue;
	
			dirtybuffer[offs] = 0;
	
	
			sy = offs / 32;
			sx = offs % 32;
	
			col = zodiack_attributesram.read(2 * sx + 1) & 0x07;
	
			if (flipscreen != 0)
			{
				sx = 31 - sx;
				sy = 31 - sy;
			}
	
			code = videoram.read(offs);
	
			drawgfx(tmpbitmap,Machine.gfx[3],
					code,
					col,
					flipscreen, flipscreen,
					8*sx, 8*sy,
					null,TRANSPARENCY_NONE,0);
		}
	
	
		/* draw the foreground characters */
		for (offs = 0; offs < videoram_size[0]; offs++)
		{
			int code,sx,sy,col;
	
	
			sy = offs / 32;
			sx = offs % 32;
	
			col = (zodiack_attributesram.read(2 * sx + 1) >> 4) & 0x07;
	
			if (flipscreen != 0)
			{
				sy = 31 - sy;
				sx = 31 - sx;
			}
	
			code = zodiack_videoram2.read(offs);
	
			drawgfx(bitmap,Machine.gfx[0],
					code,
					col,
					flipscreen, flipscreen,
					8*sx, 8*sy,
					Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	
	
		/* copy the temporary bitmap to the screen */
		{
			int i;
                        int [] scroll=new int[32];
	
	
			if (flipscreen != 0)
			{
				for (i = 0;i < 32;i++)
				{
					scroll[31-i] = zodiack_attributesram.read(2 * i);
				}
			}
			else
			{
				for (i = 0;i < 32;i++)
				{
					scroll[i] = -zodiack_attributesram.read(2 * i);
				}
			}
	
			copyscrollbitmap(bitmap,tmpbitmap,0,new int[]{0},32,scroll,Machine.visible_area,TRANSPARENCY_COLOR,0);
		}
	
	
		/* draw the bullets */
		for (offs = 0;offs < zodiack_bulletsram_size[0];offs += 4)
		{
			int x,y;
	
	
			x = zodiack_bulletsram.read(offs + 3) + Machine.drv.gfxdecodeinfo[2].gfxlayout.width;
			y = 255 - zodiack_bulletsram.read(offs + 1);
	
			if (flipscreen!=0 && percuss_hardware!=0)
			{
				y = 255 - y;
			}
	
			drawgfx(bitmap,Machine.gfx[2],
					0,	/* this is just a dot, generated by the hardware */
					0,
					0,0,
					x,y,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	
	
		/* draw the sprites */
		for (offs = spriteram_size[0] - 4;offs >= 0;offs -= 4)
		{
			int flipx,flipy,sx,sy,spritecode;
	
	
			sx = 240 - spriteram.read(offs + 3);
			sy = 240 - spriteram.read(offs);
			flipx = (spriteram.read(offs + 1) & 0x40)!=0?0:1;
			flipy =   spriteram.read(offs + 1) & 0x80;
			spritecode = spriteram.read(offs + 1) & 0x3f;
	
			if (flipscreen!=0 && percuss_hardware!=0)
			{
				sy = 240 - sy;
				flipy = flipy!=0?0:1;
			}
	
			drawgfx(bitmap,Machine.gfx[1],
					spritecode,
					spriteram.read(offs + 2) & 0x07,
					flipx,flipy,
					sx,sy,
					//flipscreen[0] ? &spritevisibleareaflipx : &spritevisiblearea,TRANSPARENCY_PEN,0);
					//&spritevisiblearea,TRANSPARENCY_PEN,0);
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	} };
}
