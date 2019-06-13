/***************************************************************************
Warlords Driver by Lee Taylor and John Clegg
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
import static mame056.inptport.*;
import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class warlord
{
	
	
	
	/***************************************************************************
	
	  Convert the color PROM into a more useable format.
	
	  The palette PROM are connected to the RGB output this way:
	
	  bit 2 -- RED
	        -- GREEN
	  bit 0 -- BLUE
	
	***************************************************************************/
        public static void COLOR(char[] colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs]) = (char) value;
        }
        
	public static VhConvertColorPromPtr warlord_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i, j;
		int _palette = 0;
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int r,g,b;
	
			r = ((color_prom.read() >> 2) & 0x01) * 0xff;
			g = ((color_prom.read() >> 1) & 0x01) * 0xff;
			b = ((color_prom.read() >> 0) & 0x01) * 0xff;
	
			/* Colors 0x40-0x7f are converted to grey scale as it's used on the
			   upright version that had an overlay */
			if (i >= Machine.drv.total_colors / 2)
			{
				int grey;
	
				/* Use the standard ratios: r = 30%, g = 59%, b = 11% */
				grey = (((r != 0)?1:0) * 0x4d) + (((g != 0)?1:0) * 0x96) + (((b != 0)?1:0) * 0x1c);
	
				r = g = b = grey;
			}
	
			palette[_palette++] = (char) r;
			palette[_palette++] = (char) g;
			palette[_palette++] = (char) b;
	
			color_prom.inc();
		}
	
		for (i = 0; i < 8; i++)
		{
			for (j = 0; j < 4; j++)
			{
				COLOR(colortable,0,i*4+j, i*16+j);
				COLOR(colortable,1,i*4+j, i*16+j*4);
			}
		}
            }
        };
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr warlord_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs, upright_mode, palette;
	
	
		/* Cocktail mode uses colors 0-3, upright 4-7 */
	
		upright_mode = input_port_0_r.handler(0) & 0x80;
		palette = ( upright_mode!=0 ? 4 : 0);
	
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			if (dirtybuffer[offs] != 0)
			{
				int sx,sy,color,flipx,flipy;
	
				dirtybuffer[offs] = 0;
	
				sy = (offs / 32);
				sx = (offs % 32);
	
				flipx = (videoram.read(offs) & 0x40)!=0?0:1;
				flipy =   videoram.read(offs) & 0x80;
	
				if (upright_mode != 0)
				{
					sx = 31 - sx;
					flipx = flipx!=0?0:1;
				}
	
				/* The four quadrants have different colors */
				color = ((sy & 0x10) >> 3) | ((sx & 0x10) >> 4) | palette;
	
				drawgfx(tmpbitmap,Machine.gfx[0],
						videoram.read(offs) & 0x3f,
						color,
						flipx, flipy,
						8*sx,8*sy,
						Machine.visible_area,TRANSPARENCY_NONE,0);
			}
		}
	
	
		/* copy the temporary bitmap to the screen */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	
	
		/* Draw the sprites */
		for (offs = 0;offs < 0x10;offs++)
		{
			int sx, sy, flipx, flipy, spritenum, color;
	
			sx = spriteram .read(offs + 0x20);
                        sy = 248 - spriteram.read(offs + 0x10);
	
			flipx = (spriteram.read(offs) & 0x40)!=0?0:1;
			flipy = spriteram.read(offs) & 0x80;
	
			if (upright_mode != 0)
			{
				sx = 248 - sx;
				flipx = flipx!=0?0:1;
			}
	
			spritenum = (spriteram.read(offs) & 0x3f);
	
			/* The four quadrants have different colors. This is not 100% accurate,
			   because right on the middle the sprite could actually have two or more
			   different color, but this is not noticable, as the color that
			   changes between the quadrants is mostly used on the paddle sprites */
			color = ((sy & 0x80) >> 6) | ((sx & 0x80) >> 7) | palette;
	
			drawgfx(bitmap,Machine.gfx[1],
					spritenum, color,
					flipx, flipy,
					sx, sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	} };
}
