/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.subArrays.IntArray;
import static common.ptr.*;
import static common.libc.cstring.*;
import static common.libc.cstdio.sprintf;
import static mame056.usrintrf.usrintf_showmessage;

import static mame056.common.*;
import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.inptport.*;
import static mame056.sound.mixer.*;
import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpuintrfH.*;

public class solomon
{
	
	
	public static UBytePtr solomon_bgvideoram=new UBytePtr();
	public static UBytePtr solomon_bgcolorram=new UBytePtr();
	
	static mame_bitmap tmpbitmap2;
	public static UBytePtr dirtybuffer2=new UBytePtr();
	static int flipscreen;
	
	
	
	public static void solomon_dirty_all()
	{
		memset(dirtybuffer2,1,videoram_size[0]);
	}
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	public static VhStartPtr solomon_vh_start = new VhStartPtr() { public int handler() 
	{
		if (generic_vh_start.handler()!= 0)
			return 1;
	
		if ((tmpbitmap2 = bitmap_alloc(Machine.drv.screen_width,Machine.drv.screen_height)) == null)
		{
			generic_vh_stop.handler();
			return 1;
		}
	
		if ((dirtybuffer2 = new UBytePtr(videoram_size[0])) == null)
		{
			bitmap_free(tmpbitmap2);
			generic_vh_stop.handler();
			return 1;
		}
		memset(dirtybuffer2,1,videoram_size[0]);
	
		/*TODO*///state_save_register_int ("video", 0, "flipscreen", &flipscreen);
		/*TODO*///state_save_register_func_postload (solomon_dirty_all);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Stop the video hardware emulation.
	
	***************************************************************************/
	public static VhStopPtr solomon_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free(tmpbitmap2);
		dirtybuffer2 = null;
		generic_vh_stop.handler();
	} };
	
	
	public static WriteHandlerPtr solomon_bgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (solomon_bgvideoram.read(offset) != data)
		{
			dirtybuffer2.write(offset, 1);
	
			solomon_bgvideoram.write(offset, data);
		}
	} };
	
	public static WriteHandlerPtr solomon_bgcolorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (solomon_bgcolorram.read(offset) != data)
		{
			dirtybuffer2.write(offset, 1);
	
			solomon_bgcolorram.write(offset, data);
		}
	} };
	
	
	
	public static WriteHandlerPtr solomon_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (flipscreen != (data & 1))
		{
			flipscreen = data & 1;
			memset(dirtybuffer,1,videoram_size[0]);
			memset(dirtybuffer2,1,videoram_size[0]);
		}
	} };
	
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr solomon_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		for (offs = 0;offs < videoram_size[0];offs++)
		{
			if (dirtybuffer2.read(offs) != 0)
			{
				int sx,sy,flipx,flipy;
	
	
				dirtybuffer2.write(offs, 0);
				sx = offs % 32;
				sy = offs / 32;
				flipx = solomon_bgcolorram.read(offs) & 0x80;
				flipy = solomon_bgcolorram.read(offs) & 0x08;
				if (flipscreen != 0)
				{
					sx = 31 - sx;
					sy = 31 - sy;
					flipx = flipx!=0?0:1;
					flipy = flipy!=0?0:1;
				}
	
				drawgfx(tmpbitmap2,Machine.gfx[1],
						solomon_bgvideoram.read(offs) + 256 * (solomon_bgcolorram.read(offs) & 0x07),
						((solomon_bgcolorram.read(offs) & 0x70) >> 4),
						flipx,flipy,
						8*sx,8*sy,
						null,TRANSPARENCY_NONE,0);
			}
		}
	
		/* copy the character mapped graphics */
		copybitmap(bitmap,tmpbitmap2,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	
		/* draw the frontmost playfield */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
	//		if (dirtybuffer[offs])
			{
				int sx,sy;
	
	
				dirtybuffer[offs] = 0;
				sx = offs % 32;
				sy = offs / 32;
				if (flipscreen != 0)
				{
					sx = 31 - sx;
					sy = 31 - sy;
				}
	
				drawgfx(bitmap,Machine.gfx[0],
						videoram.read(offs) + 256 * (colorram.read(offs) & 0x07),
						(colorram.read(offs) & 0x70) >> 4,
						flipscreen,flipscreen,
						8*sx,8*sy,
						Machine.visible_area,TRANSPARENCY_PEN,0);
			}
		}
	
	
		/* draw sprites */
		for (offs = spriteram_size[0] - 4;offs >= 0;offs -= 4)
		{
			int sx,sy,flipx,flipy;
	
	
			sx = spriteram.read(offs+3);
			sy = 241-spriteram.read(offs+2);
			flipx = spriteram.read(offs+1) & 0x40;
			flipy =	spriteram.read(offs+1) & 0x80;
			if ((flipscreen & 1)!=0)
			{
				sx = 240 - sx;
				sy = 240 - sy;
				flipx = flipx!=0?0:1;
				flipy = flipy!=0?0:1;
			}
	
			drawgfx(bitmap,Machine.gfx[2],
					spriteram.read(offs) + 16*(spriteram.read(offs+1) & 0x10),
					(spriteram.read(offs + 1) & 0x0e) >> 1,
					flipx,flipy,
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	} };
}
