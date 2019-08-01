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

public class pinbo
{
	
	
	static int[] flipscreen=new int[2];
	
	
	
	public static WriteHandlerPtr pinbo_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (flipscreen[0] != (data & 1))
		{
			flipscreen[0] = data & 1;
			memset(dirtybuffer,1,videoram_size[0]);
		}
		if (flipscreen[1] != (data & 2))
		{
			flipscreen[1] = data & 2;
			memset(dirtybuffer,1,videoram_size[0]);
		}
	} };
	
	public static VhUpdatePtr pinbo_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
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
				if (flipscreen[0] != 0) sx = 31 - sx;
				if (flipscreen[1] != 0) sy = 31 - sy;
	
				drawgfx(tmpbitmap,Machine.gfx[0],
						videoram.read(offs)+ ((colorram.read(offs)& 0x70) << 5),
						colorram.read(offs)& 0x0f,
						flipscreen[0],flipscreen[1],
						8*sx,8*sy,
						Machine.visible_area,TRANSPARENCY_NONE,0);
			}
		}
	
		/* copy the character mapped graphics */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	
		/* Draw the sprites. */
		for (offs = spriteram_size[0] - 4;offs >= 0;offs -= 4)
		{
			int sx,sy,flipx,flipy;
			int code,color;
	
	
			sx = spriteram.read(offs + 3);
			sy = 240 - spriteram.read(offs);
			flipx = spriteram.read(offs+1)& 0x40;
			flipy = spriteram.read(offs+1)& 0x80;
			if (flipscreen[0] != 0)
			{
				sx = 240 - sx;
				flipx = flipx!=0?0:1;
			}
			if (flipscreen[1] != 0)
			{
				sy = 240 - sy;
				flipy = flipy!=0?0:1;
			}
			code = (spriteram.read(offs+1)& 0x3f) | 0x40 | ((spriteram.read(offs+2)& 0x30) << 3);
			color = (spriteram.read(offs+2)& 0x0f);
	
			drawgfx(bitmap,Machine.gfx[1],
					code,
					color,
					flipx,flipy,
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	} };
}
