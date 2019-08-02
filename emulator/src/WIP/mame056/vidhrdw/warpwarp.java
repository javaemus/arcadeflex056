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
import static mame056.cpuintrfH.*;
import static mame056.cpuintrf.*;
import static mame056.inptport.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.vidhrdw.generic.*;
import static mame056.palette.*;
// refactor
import static arcadeflex036.osdepend.logerror;

public class warpwarp
{
	
	
	
	public static UBytePtr warpwarp_bulletsram = new UBytePtr();
	
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Warp Warp doesn't use PROMs - the 8-bit code is directly converted into a
	  color.
	
	  The color RAM is connected to the RGB output this way (I think - schematics
	  are fuzzy):
	
	  bit 7 -- 300 ohm resistor  -- BLUE
	        -- 820 ohm resistor  -- BLUE
	        -- 300 ohm resistor  -- GREEN
	        -- 820 ohm resistor  -- GREEN
	        -- 1.6kohm resistor  -- GREEN
	        -- 300 ohm resistor  -- RED
	        -- 820 ohm resistor  -- RED
	  bit 0 -- 1.6kohm resistor  -- RED
	
	  Moreover, the bullet is pure white, obtained with three 220 ohm resistors.
	
	***************************************************************************/
        public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
		
        public static void COLOR(char[] colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs])=(char) value;
        }
        
	public static VhConvertColorPromPtr warpwarp_init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
		
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int bit0,bit1,bit2,r,g,b;
	
	
			/* red component */
			bit0 = (i >> 0) & 0x01;
			bit1 = (i >> 1) & 0x01;
			bit2 = (i >> 2) & 0x01;
			r = 0x1f * bit0 + 0x3c * bit1 + 0xa4 * bit2;
			/* green component */
			bit0 = (i >> 3) & 0x01;
			bit1 = (i >> 4) & 0x01;
			bit2 = (i >> 5) & 0x01;
			g = 0x1f * bit0 + 0x3c * bit1 + 0xa4 * bit2;
			/* blue component */
			bit0 = 0;
			bit1 = (i >> 6) & 0x01;
			bit2 = (i >> 7) & 0x01;
			b = 0x1f * bit0 + 0x3c * bit1 + 0xa4 * bit2;
	
			palette_set_color(i,r,g,b);
		}
	
		for (i = 0;i < TOTAL_COLORS(0);i += 2)
		{
			COLOR(colortable,0,i, 0);			/* black background */
			COLOR(colortable,0,i + 1, i / 2);	/* colored foreground */
		}
            }
        };
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr warpwarp_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			if (dirtybuffer[offs] != 0)
			{
				int mx,my,sx,sy;
	
				mx = offs % 32;
				my = offs / 32;
	
				if (my == 0)
				{
					sx = 33;
					sy = mx;
				}
				else if (my == 1)
				{
					sx = 0;
					sy = mx;
				}
				else
				{
					sx = mx + 1;
					sy = my;
				}
	
				drawgfx(tmpbitmap,Machine.gfx[0],
						videoram.read(offs),
						colorram.read(offs),
						0,0,
						8*sx,8*sy,
						Machine.visible_area,TRANSPARENCY_NONE,0);
	
				dirtybuffer[offs] = 0;
			}
		}
	
	
		/* copy the character mapped graphics */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	
	
		if (warpwarp_bulletsram.read(0) > 1)
		{
			int x,y;
	
	
			x = 260 - warpwarp_bulletsram.read(0);
			y = 252 - warpwarp_bulletsram.read(1);
			if (x >= Machine.visible_area.min_x && x+3 <= Machine.visible_area.max_x &&
				y >= Machine.visible_area.min_y && y+3 <= Machine.visible_area.max_y)
			{
				int colour;
				int i,j;
	
	
				colour = Machine.pens[0xf6];	/* white */
	
				for (i = 0;i < 4;i++)
				{
					for (j = 0;j < 4;j++)
					{
						plot_pixel.handler(bitmap, x+j, y+i, colour);
					}
				}
			}
		}
	} };
}
