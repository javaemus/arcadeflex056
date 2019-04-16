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
import static mame056.cpuexec.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpuexecH.M6809_INT_NMI;
import static mame056.inptport.readinputport;


public class yiear
{
	
	
	static int nmi_enable;
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Yie Ar Kung-Fu has one 32x8 palette PROM, connected to the RGB output this
	  way:
	
	  bit 7 -- 220 ohm resistor  -- BLUE
	        -- 470 ohm resistor  -- BLUE
	        -- 220 ohm resistor  -- GREEN
	        -- 470 ohm resistor  -- GREEN
	        -- 1  kohm resistor  -- GREEN
	        -- 220 ohm resistor  -- RED
	        -- 470 ohm resistor  -- RED
	  bit 0 -- 1  kohm resistor  -- RED
	
	***************************************************************************/
	public static VhConvertColorPromPtr yiear_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
                int _palette = 0;
	
		for (i = 0;i < Machine.drv.total_colors;i++)
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
	
	public static WriteHandlerPtr yiear_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bit 0 flips screen */
		flip_screen_set(data & 1);
	
		/* bit 1 is NMI enable */
		nmi_enable = data & 0x02;
	
		/* bit 2 is IRQ enable */
		interrupt_enable_w.handler(0, data & 0x04);
	
		/* bits 3 and 4 are coin counters */
		coin_counter_w(0, (data >> 3) & 0x01);
		coin_counter_w(1, (data >> 4) & 0x01);
	} };
	
	
	public static InterruptPtr yiear_nmi_interrupt = new InterruptPtr() { public int handler() 
	{
		/* can't use nmi_interrupt() because interrupt_enable_w() effects it */
		return nmi_enable!=0 ? M6809_INT_NMI : ignore_interrupt.handler();
	} };
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr yiear_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		if (full_refresh!=0)
		{
			memset(dirtybuffer,1,videoram_size[0]);
		}
	
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 2;offs >= 0;offs -= 2)
		{
			if ((dirtybuffer[offs]!=0) || (dirtybuffer[offs + 1]!=0))
			{
				int sx,sy,flipx,flipy;
	
	
				dirtybuffer[offs] = dirtybuffer[offs + 1] = 0;
	
				sx = (offs/2) % 32;
				sy = (offs/2) / 32;
				flipx = videoram.read(offs) & 0x80;
				flipy = videoram.read(offs) & 0x40;
				if (flip_screen()!=0)
				{
					sx = 31 - sx;
					sy = 31 - sy;
					flipx = flipx!=0?0:1;
					flipy = flipy!=0?0:1;
				}
	
				drawgfx(tmpbitmap,Machine.gfx[0],
					videoram.read(offs + 1) | ((videoram.read(offs) & 0x10) << 4),
					0,
					flipx,flipy,
					8*sx,8*sy,
					null,TRANSPARENCY_NONE,0);
			}
		}
	
	
		/* copy the temporary bitmap to the screen */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	
	
		/* draw sprites */
		for (offs = spriteram_size[0] - 2;offs >= 0;offs -= 2)
		{
			int sx,sy,flipx,flipy;
	
	
			sy    =  240 - spriteram.read(offs + 1);
			sx    =  spriteram_2.read(offs);
			flipx = ~spriteram.read(offs) & 0x40;
			flipy =  spriteram.read(offs) & 0x80;
	
			if (flip_screen()!=0)
			{
				sy = 240 - sy;
				flipy = flipy!=0?0:1;
			}
	
			if (offs < 0x26)
			{
				sy++;	/* fix title screen & garbage at the bottom of the screen */
			}
	
			drawgfx(bitmap,Machine.gfx[1],
				spriteram_2.read(offs + 1) + 256 * (spriteram.read(offs) & 1),
				0,
				flipx,flipy,
				sx,sy,
				Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	} };
}
