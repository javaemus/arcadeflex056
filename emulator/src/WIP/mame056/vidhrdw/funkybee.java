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

public class funkybee
{
	
	
	public static UBytePtr funkyb_row_scroll = new UBytePtr();
	
	static int gfx_bank;
	
	
	public static VhConvertColorPromPtr funkybee_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
                int _palette = 0;
	
		/* first, the character/sprite palette */
		for (i = 0;i < 32;i++)
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
            }
        };
	
	public static WriteHandlerPtr funkybee_gfx_bank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (data != (gfx_bank & 0x01))
		{
			gfx_bank = data & 0x01;
			memset(dirtybuffer, 1, videoram_size[0]);
		}
	} };
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	static void draw_chars(mame_bitmap _tmpbitmap, mame_bitmap bitmap)
	{
		int sx,sy;
	
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (sy = 0x1f;sy >= 0;sy--)
		{
			int offs;
	
	
			offs = (sy << 8) | 0x1f;
	
			for (sx = 0x1f;sx >= 0;sx--,offs--)
			{
				if (dirtybuffer[offs] != 0)
				{
					dirtybuffer[offs] = 0;
	
					drawgfx(_tmpbitmap,Machine.gfx[gfx_bank],
							videoram.read(offs),
							colorram.read(offs) & 0x03,
							0,0,
							8*sx,8*sy,
							null,TRANSPARENCY_NONE,0);
				}
			}
		}
	
	
		/* copy the temporary bitmap to the screen */
		{
			int offs;
                        int[] scroll=new int[32];
	
	
			for (offs = 0;offs < 28;offs++)
				scroll[offs] = -funkyb_row_scroll.read();
	
			for (;offs < 32;offs++)
				scroll[offs] = 0;
	
			copyscrollbitmap(bitmap,_tmpbitmap,32,scroll,0,new int[]{0},Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	}
	
	
	public static VhUpdatePtr funkybee_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		draw_chars(tmpbitmap, bitmap);
	
	
		/* draw the sprites */
		for (offs = 0x0f; offs >= 0; offs--)
		{
			int sx,sy,code,col,flipy,offs2;
	
	
			offs2 = 0x1e00 + offs;
	
			code  = videoram.read(offs2);
			sx    = videoram.read(offs2 + 0x10);
			sy    = 224 - colorram.read(offs2);
			col   = colorram.read(offs2 + 0x10);
			flipy = code & 0x01;
	
			drawgfx(bitmap,Machine.gfx[2+gfx_bank],
					(code >> 2) | ((code & 2) << 5),
					col,
					0,flipy,
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	
	
		/* draw the two variable position columns */
		for (offs = 0x1f;offs >= 0;offs--)
		{
			drawgfx(bitmap,Machine.gfx[gfx_bank],
					videoram.read(offs+0x1c00),
					colorram.read(0x1f10) & 0x03,
					0,0,
					videoram.read(0x1f10),8*offs,
					null,TRANSPARENCY_PEN,0);
	
			drawgfx(bitmap,Machine.gfx[gfx_bank],
					videoram.read(offs+0x1d00),
					colorram.read(0x1f11) & 0x03,
					0,0,
					videoram.read(0x1f11),8*offs,
					null,TRANSPARENCY_PEN,0);
		}
	} };
	
}
