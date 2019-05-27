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


public class superqix
{
	
	
	
	static int gfxbank;
	public static UBytePtr superqix_bitmapram = new UBytePtr(), superqix_bitmapram2 = new UBytePtr(), superqix_bitmapram_dirty = new UBytePtr(), superqix_bitmapram2_dirty = new UBytePtr();
	static mame_bitmap tmpbitmap2;
	static int sqix_minx,sqix_maxx,sqix_miny,sqix_maxy;
	static int sqix_last_bitmap;
	static int sqix_current_bitmap;
	
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	public static VhStartPtr superqix_vh_start = new VhStartPtr() { public int handler() 
	{
		if (generic_vh_start.handler()!= 0)
			return 1;
	
		/* palette RAM is accessed thorough I/O ports, so we have to */
		/* allocate it ourselves */
		if ((paletteram = new UBytePtr(256)) == null)
		{
			generic_vh_stop.handler();
			return 1;
		}
	
		if ((superqix_bitmapram = new UBytePtr(0x7000)) == null)
		{
			paletteram = null;
			generic_vh_stop.handler();
			return 1;
		}
	
		if ((superqix_bitmapram2 = new UBytePtr(0x7000)) == null)
		{
			superqix_bitmapram = null;
			paletteram = null;
			generic_vh_stop.handler();
			return 1;
		}
	
		if ((superqix_bitmapram_dirty = new UBytePtr(0x7000)) == null)
		{
			superqix_bitmapram2 = null;
			superqix_bitmapram = null;
			paletteram = null;
			generic_vh_stop.handler();
			return 1;
		}
		memset(superqix_bitmapram_dirty,1,0x7000);
	
		if ((superqix_bitmapram2_dirty = new UBytePtr(0x7000)) == null)
		{
			superqix_bitmapram_dirty = null;
			superqix_bitmapram2 = null;
			superqix_bitmapram = null;
			paletteram = null;
			generic_vh_stop.handler();
			return 1;
		}
		memset(superqix_bitmapram2_dirty,1,0x7000);
	
		if ((tmpbitmap2 = bitmap_alloc(256, 256)) == null)
		{
			superqix_bitmapram2_dirty = null;
			superqix_bitmapram_dirty = null;
			superqix_bitmapram2 = null;
			superqix_bitmapram = null;
			paletteram = null;
			generic_vh_stop.handler();
			return 1;
		}
	
		sqix_minx=0;sqix_maxx=127;sqix_miny=0;sqix_maxy=223;
		sqix_last_bitmap=0;
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Stop the video hardware emulation.
	
	***************************************************************************/
	public static VhStopPtr superqix_vh_stop = new VhStopPtr() { public void handler() 
	{
		superqix_bitmapram2 = null;
		superqix_bitmapram = null;
		superqix_bitmapram_dirty = null;
		superqix_bitmapram2_dirty = null;
		bitmap_free (tmpbitmap2);
		paletteram = null;
		generic_vh_stop.handler();
	} };
	
	
	
	public static ReadHandlerPtr superqix_bitmapram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return superqix_bitmapram.read(offset);
	} };
	
	
	public static WriteHandlerPtr superqix_bitmapram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if(data != superqix_bitmapram.read(offset))
		{
			int x,y;
			superqix_bitmapram.write(offset, data);
			superqix_bitmapram_dirty.write(offset, 1);
			x=offset%128;
			y=offset/128;
			if(x<sqix_minx) sqix_minx=x;
			if(x>sqix_maxx) sqix_maxx=x;
			if(y<sqix_miny) sqix_miny=y;
			if(y>sqix_maxy) sqix_maxy=y;
		}
	} };
	
	public static ReadHandlerPtr superqix_bitmapram2_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return superqix_bitmapram2.read(offset);
	} };
	
	public static WriteHandlerPtr superqix_bitmapram2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if(data != superqix_bitmapram2.read(offset))
		{
			int x,y;
			superqix_bitmapram2.write(offset, data);
			superqix_bitmapram2_dirty.write(offset, 1);
			x=offset%128;
			y=offset/128;
			if(x<sqix_minx) sqix_minx=x;
			if(x>sqix_maxx) sqix_maxx=x;
			if(y<sqix_miny) sqix_miny=y;
			if(y>sqix_maxy) sqix_maxy=y;
		}
	} };
	
	
	
	public static WriteHandlerPtr superqix_0410_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int bankaddress;
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
	
		/* bits 0-1 select the tile bank */
		if (gfxbank != (data & 0x03))
		{
			gfxbank = data & 0x03;
			memset(dirtybuffer,1,videoram_size[0]);
		}
	
		/* bit 2 controls bitmap 1/2 */
		sqix_current_bitmap=data&4;
		if(sqix_current_bitmap !=sqix_last_bitmap)
		{
			sqix_last_bitmap=sqix_current_bitmap;
			memset(superqix_bitmapram_dirty,1,0x7000);
			memset(superqix_bitmapram2_dirty,1,0x7000);
			sqix_minx=0;sqix_maxx=127;sqix_miny=0;sqix_maxy=223;
		}
	
		/* bit 3 enables NMI */
		interrupt_enable_w.handler(offset,data & 0x08);
	
		/* bits 4-5 control ROM bank */
		bankaddress = 0x10000 + ((data & 0x30) >> 4) * 0x4000;
		cpu_setbank(1,new UBytePtr(RAM, bankaddress));
	} };
	
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr superqix_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs,i;
		int[] pens=new int[16];
	
	
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
	
				drawgfx(tmpbitmap,Machine.gfx[(colorram.read(offs) & 0x04)!=0 ? 0 : (1 + gfxbank)],
						videoram.read(offs) + 256 * (colorram.read(offs) & 0x03),
						(colorram.read(offs) & 0xf0) >> 4,
						0,0,
						8*sx,8*sy,
						Machine.visible_area,TRANSPARENCY_NONE,0);
			}
		}
	
	
		/* copy the character mapped graphics */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	
		for(i=1;i<16;i++)
			pens[i]=Machine.pens[i];
		pens[0]=0;
	
		if(sqix_current_bitmap==0)		/* Bitmap 1 */
		{
			int x,y;
	
			for (y = sqix_miny;y <= sqix_maxy;y++)
			{
				for (x = sqix_minx;x <= sqix_maxx;x++)
				{
					int sx,sy,d;
	
					if(superqix_bitmapram_dirty.read(y*128+x) != 0)
					{
						superqix_bitmapram_dirty.write(y*128+x, 0);
						d = superqix_bitmapram.read(y*128+x);
	
						sx = 2*x;
						sy = y+16;
	
						plot_pixel.handler(tmpbitmap2, sx    , sy, pens[d >> 4]);
						plot_pixel.handler(tmpbitmap2, sx + 1, sy, pens[d & 0x0f]);
					}
				}
			}
		}
		else		/* Bitmap 2 */
		{
			int x,y;
	
			for (y = sqix_miny;y <= sqix_maxy;y++)
			{
				for (x = sqix_minx;x <= sqix_maxx;x++)
				{
					int sx,sy,d;
	
					if(superqix_bitmapram2_dirty.read(y*128+x) != 0)
					{
						superqix_bitmapram2_dirty.write(y*128+x, 0);
						d = superqix_bitmapram2.read(y*128+x);
	
						sx = 2*x;
						sy = y+16;
	
						plot_pixel.handler(tmpbitmap2, sx    , sy, pens[d >> 4]);
						plot_pixel.handler(tmpbitmap2, sx + 1, sy, pens[d & 0x0f]);
					}
				}
			}
		}
		copybitmap(bitmap,tmpbitmap2,0,0,0,0,Machine.visible_area,TRANSPARENCY_PEN,0);
	
		/* Draw the sprites. Note that it is important to draw them exactly in this */
		/* order, to have the correct priorities. */
		for (offs = 0;offs < spriteram_size[0];offs += 4)
		{
			drawgfx(bitmap,Machine.gfx[5],
					spriteram.read(offs) + 256 * (spriteram.read(offs + 3) & 0x01),
					(spriteram.read(offs + 3) & 0xf0) >> 4,
					spriteram.read(offs + 3) & 0x04,spriteram.read(offs + 3) & 0x08,
					spriteram.read(offs + 1),spriteram.read(offs + 2),
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	
	
		/* redraw characters which have priority over the bitmap */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			if ((colorram.read(offs) & 0x08) != 0)
			{
				int sx,sy;
	
	
				sx = offs % 32;
				sy = offs / 32;
	
				drawgfx(bitmap,Machine.gfx[(colorram.read(offs) & 0x04)!=0 ? 0 : 1],
						videoram.read(offs) + 256 * (colorram.read(offs) & 0x03),
						(colorram.read(offs) & 0xf0) >> 4,
						0,0,
						8*sx,8*sy,
						Machine.visible_area,TRANSPARENCY_PEN,0);
			}
		}
	
		sqix_minx=1000;sqix_maxx=-1;sqix_miny=1000;sqix_maxy=-1;
	} };
}
