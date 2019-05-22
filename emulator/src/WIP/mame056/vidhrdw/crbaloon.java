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

import static common.ptr.*;

import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.sound.mixer.*;
import static mame056.sound.mixerH.*;
import static mame056.inptport.*;
import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class crbaloon
{
	
	static int[] spritectrl = new int[3];
	
	public static int crbaloon_collision;
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Crazy Balloon has no PROMs, the color code directly maps to a color:
	  all bits are inverted
	  bit 3 HALF (intensity)
	  bit 2 BLUE
	  bit 1 GREEN
	  bit 0 RED
	
	***************************************************************************/
        public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
        
	public static void COLOR(char[] colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs]) = (char) value;
        }
        
	public static VhConvertColorPromPtr crbaloon_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
		int _palette = 0;
	
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int intensity;
	
	
			intensity = (~i & 0x08)!=0 ? 0xff : 0x55;
	
			/* red component */
			palette[_palette++] = (char) (intensity * ((~i >> 0) & 1));
			/* green component */
			palette[_palette++] = (char) (intensity * ((~i >> 1) & 1));
			/* blue component */
			palette[_palette++] = (char) (intensity * ((~i >> 2) & 1));
		}
	
		for (i = 0;i < TOTAL_COLORS(0);i += 2)
		{
			COLOR(colortable,0,i,15);		/* black background */
			COLOR(colortable,0,i + 1,i / 2);	/* colored foreground */
		}
                
                palette[0]=0x00;
                palette[1]=0x00;
                palette[2]=0x00;
                
                palette[3]=0xFF;
                palette[4]=0xFF;
                palette[5]=0xFF;
            }
        };
	
	public static WriteHandlerPtr crbaloon_spritectrl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		spritectrl[offset] = data;
	} };
	
	
	
	public static WriteHandlerPtr crbaloon_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		flip_screen_set(data & 1);
	} };
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	 ***************************************************************************/
	
	public static VhUpdatePtr crbaloon_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs,x,y;
		int bx,by;
	
	
		if (full_refresh != 0)
			memset(dirtybuffer,1,videoram_size[0]);
	
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			if (dirtybuffer[offs] != 0)
			{
				int sx,sy;
	
	
				dirtybuffer[offs] = 0;
	
				sx = offs % 32;
				sy = offs / 32;
				if (flip_screen() == 0)
				{
					sx = 31 - sx;
					sy = 31 - sy;
				}
	
				drawgfx(tmpbitmap,Machine.gfx[0],
						videoram.read(offs),
						colorram.read(offs) & 0x0f,
						flip_screen(),flip_screen(),
						8*sx,8*sy,
						Machine.visible_area,TRANSPARENCY_NONE,0);
			}
		}
	
		/* copy the character mapped graphics */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	
	
	    /* Check Collision - Draw balloon in background colour, if no */
	    /* collision occured, bitmap will be same as tmpbitmap        */
	
		bx = spritectrl[1];
		by = spritectrl[2] - 32;
	
		if (flip_screen() != 0)
		{
			by += 32;
		}
	
		drawgfx(bitmap,Machine.gfx[1],
				spritectrl[0] & 0x0f,
				15,
				0,0,
				bx,by,
				Machine.visible_area,TRANSPARENCY_PEN,0);
	
	    crbaloon_collision = 0;
	
		for (x = bx; x < bx + Machine.gfx[1].width; x++)
		{
			for (y = by; y < by + Machine.gfx[1].height; y++)
	        {
				if ((x < Machine.visible_area.min_x) ||
				    (x > Machine.visible_area.max_x) ||
				    (y < Machine.visible_area.min_y) ||
				    (y > Machine.visible_area.max_y))
				{
					continue;
				}
	
	        	if (read_pixel.handler(bitmap, x, y) != read_pixel.handler(tmpbitmap, x, y))
	        	{
					crbaloon_collision = -1;
					break;
				}
	        }
		}
	
	
		/* actually draw the balloon */
	
		drawgfx(bitmap,Machine.gfx[1],
				spritectrl[0] & 0x0f,
				(spritectrl[0] & 0xf0) >> 4,
				0,0,
				bx,by,
				Machine.visible_area,TRANSPARENCY_PEN,0);
	} };
}
