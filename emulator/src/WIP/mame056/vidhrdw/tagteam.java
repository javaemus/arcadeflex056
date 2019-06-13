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
import static mame056.inptport.*;
import static mame056.vidhrdw.generic.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class tagteam
{
	
	static int flipscreen = 0;
	static int palettebank;
	
	
	
	public static VhConvertColorPromPtr tagteam_vh_convert_color_prom = new VhConvertColorPromPtr() {
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
	
	public static ReadHandlerPtr tagteam_mirrorvideoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int x,y;
	
		/* swap x and y coordinates */
		x = offset / 32;
		y = offset % 32;
		offset = 32 * y + x;
	
		return videoram_r.handler(offset);
	} };
	
	public static ReadHandlerPtr tagteam_mirrorcolorram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int x,y;
	
		/* swap x and y coordinates */
		x = offset / 32;
		y = offset % 32;
		offset = 32 * y + x;
	
		return colorram_r.handler(offset);
	} };
	
	public static WriteHandlerPtr tagteam_mirrorvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int x,y;
	
		/* swap x and y coordinates */
		x = offset / 32;
		y = offset % 32;
		offset = 32 * y + x;
	
		videoram_w.handler(offset,data);
	} };
	
	public static WriteHandlerPtr tagteam_mirrorcolorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int x,y;
	
		/* swap x and y coordinates */
		x = offset / 32;
		y = offset % 32;
		offset = 32 * y + x;
	
		colorram_w.handler(offset,data);
	} };
	
	public static WriteHandlerPtr tagteam_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	logerror("%04x: control = %02x\n",cpu_get_pc(),data);
	
		/* bit 7 is the palette bank */
		palettebank = (data & 0x80) >> 7;
	} };
	
	
	/***************************************************************************
	
	Draw the game screen in the given mame_bitmap.
	Do NOT call osd_update_display() from this function, it will be called by
	the main emulation engine.
	
	***************************************************************************/
	static void drawchars(mame_bitmap bitmap,int color)
	{
		int offs;
	
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. If the background is on, */
		/* draw characters as sprites */
	
	
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			int sx,sy;
	
			if (dirtybuffer[offs] != 0)
			{
				dirtybuffer[offs] = 0;
	
				sx = 31 - offs % 32;
				sy = offs / 32;
	
				if (flipscreen != 0)
				{
					sx = 31 - sx;
					sy = 31 - sy;
				}
	
				/*Someday when the proms are properly figured out, we can remove
				the color hack*/
				drawgfx(tmpbitmap,Machine.gfx[0],
						videoram.read(offs) + 256 * colorram.read(offs),
						2*color,	/* guess */
						flipscreen,flipscreen,
						8*sx,8*sy,
						Machine.visible_area,TRANSPARENCY_NONE,0);
			}
		}
	
		/* copy the temporary bitmap to the screen */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	}
	
	static void drawsprites(mame_bitmap bitmap,int color)
	{
		int offs;
	
		/* Draw the sprites */
		for (offs = 0;offs < 0x20;offs += 4)
		{
			int sx,sy,flipx,flipy;
			int spritebank;
	
			if ((videoram.read(offs + 0) & 0x01)==0) continue;
	
			sx = 240 - videoram.read(offs + 3);
			sy = 240 - videoram.read(offs + 2);
	
			flipx = videoram.read(offs + 0) & 0x04;
			flipy = videoram.read(offs + 0) & 0x02;
			spritebank = (videoram.read(offs) & 0x30) << 4;
	
			if (flipscreen != 0)
			{
				sx = 240 - sx;
				sy = 240 - sy;
	
				flipx = flipx!=0 ? 0:1;
				flipy = flipy!=0 ? 0:1;
			}
	
			drawgfx(bitmap,Machine.gfx[1],
					videoram.read(offs + 1) + 256 * spritebank,
					1+2*color,	/* guess */
					flipx,flipy,
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
	
			sy += (flipscreen!=0 ? -256 : 256);
	
			// Wrap around
			drawgfx(bitmap,Machine.gfx[1],
					videoram.read(offs + 0x20) + 256 * spritebank,
					color,
					flipx,flipy,
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	}
	
	public static VhUpdatePtr tagteam_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		drawchars(bitmap,palettebank);
	
		drawsprites(bitmap,palettebank);
	} };
	
}
