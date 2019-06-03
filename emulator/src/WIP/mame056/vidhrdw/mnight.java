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

public class mnight
{
	
	/*TODO*///public static void COLORTABLE_START(int gfxn, int color){
        /*TODO*///    Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + 
	/*TODO*///					color * Machine.gfx[gfxn].color_granularity
	public static int GFX_COLOR_CODES(int gfxn){
            return Machine.gfx[gfxn].total_colors;
        }
	
        public static int GFX_ELEM_COLORS(int gfxn){
            return Machine.gfx[gfxn].color_granularity;
        }
	
	public static UBytePtr mnight_scrolly_ram = new UBytePtr();
	public static UBytePtr mnight_scrollx_ram = new UBytePtr();
	public static UBytePtr mnight_bgenable_ram = new UBytePtr();
	public static UBytePtr mnight_spoverdraw_ram = new UBytePtr();
	public static UBytePtr mnight_background_videoram = new UBytePtr();
	public static int[] mnight_backgroundram_size=new int[2];
	public static UBytePtr mnight_foreground_videoram = new UBytePtr();
	public static int[] mnight_foregroundram_size=new int[2];
	
	static mame_bitmap bitmap_bg;
	static mame_bitmap bitmap_sp;
	
	public static UBytePtr bg_dirtybuffer = new UBytePtr();
	static int       bg_enable = 1;
	static int       sp_overdraw = 0;
	
	public static VhStartPtr mnight_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((bg_dirtybuffer = new UBytePtr(1024)) == null)
		{
			return 1;
		}
		if ((bitmap_bg = bitmap_alloc (Machine.drv.screen_width*2,Machine.drv.screen_height*2)) == null)
		{
			bg_dirtybuffer = null;
			return 1;
		}
		if ((bitmap_sp = bitmap_alloc (Machine.drv.screen_width,Machine.drv.screen_height)) == null)
		{
			bg_dirtybuffer = null;
			bitmap_bg = null;
			return 1;
		}
		memset(bg_dirtybuffer,1,1024);
	
		return 0;
	} };
	
	public static VhStopPtr mnight_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free(bitmap_bg);
		bitmap_free(bitmap_sp);
		bg_dirtybuffer = null;
	} };
	
	
	public static WriteHandlerPtr mnight_bgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (mnight_background_videoram.read(offset) != data)
		{
			bg_dirtybuffer.write(offset >> 1, 1);
			mnight_background_videoram.write(offset, data);
		}
	} };
	
	public static WriteHandlerPtr mnight_fgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (mnight_foreground_videoram.read(offset) != data)
			mnight_foreground_videoram.write(offset, data);
	} };
	
	public static WriteHandlerPtr mnight_background_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (bg_enable!=data)
		{
			mnight_bgenable_ram.write(offset, data);
			bg_enable = data;
			if (bg_enable != 0)
				memset(bg_dirtybuffer, 1, mnight_backgroundram_size[0] / 2);
			else
				fillbitmap(bitmap_bg, Machine.pens[0],null);
		}
	} };
	
	public static WriteHandlerPtr mnight_sprite_overdraw_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (sp_overdraw != (data&1))
		{
			mnight_spoverdraw_ram.write(offset, data);
			fillbitmap(bitmap_sp,15,Machine.visible_area);
			sp_overdraw = data & 1;
		}
	} };
	
	static void mnight_draw_foreground(mame_bitmap bitmap)
	{
		int offs;
	
		/* Draw the foreground text */
	
		for (offs = 0 ;offs < mnight_foregroundram_size[0] / 2; offs++)
		{
			int sx,sy,tile,palette,flipx,flipy,lo,hi;
	
			if ((mnight_foreground_videoram.read(offs*2) | mnight_foreground_videoram.read(offs*2+1)) != 0)
			{
				sx = (offs % 32) << 3;
				sy = (offs >> 5) << 3;
	
				lo = mnight_foreground_videoram.read(offs*2);
				hi = mnight_foreground_videoram.read(offs*2+1);
				tile = ((hi & 0xc0) << 2) | lo;
				flipx = hi & 0x10;
				flipy = hi & 0x20;
				palette = hi & 0x0f;
	
				drawgfx(bitmap,Machine.gfx[3],
						tile,
						palette,
						flipx,flipy,
						sx,sy,
						Machine.visible_area,TRANSPARENCY_PEN, 15);
			}
	
		}
	}
	
	
	static void mnight_draw_background(mame_bitmap bitmap)
	{
		int offs;
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
	
		for (offs = 0 ;offs < mnight_backgroundram_size[0] / 2; offs++)
		{
			int sx,sy,tile,palette,flipy,lo,hi;
	
			if (bg_dirtybuffer.read(offs) != 0)
			{
				sx = (offs % 32) << 4;
				sy = (offs >> 5) << 4;
	
				bg_dirtybuffer.write(offs, 0);
	
				lo = mnight_background_videoram.read(offs*2);
				hi = mnight_background_videoram.read(offs*2+1);
				tile = ((hi & 0x10) << 6) | ((hi & 0xc0) << 2) | lo;
				flipy = hi & 0x20;
				palette = hi & 0x0f;
				drawgfx(bitmap,Machine.gfx[0],
						tile,
						palette,
						0,flipy,
						sx,sy,
						null,TRANSPARENCY_NONE,0);
			}
	
		}
	}
	
	static void mnight_draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
		/* Draw the sprites */
	
		for (offs = 11 ;offs < spriteram_size[0]; offs+=16)
		{
			int sx,sy,tile,palette,flipx,flipy,big;
	
			if ((spriteram.read(offs+2) & 2) != 0)
			{
				sx = spriteram.read(offs+1);
				sy = spriteram.read(offs);
				if ((spriteram.read(offs+2) & 1)!=0) sx-=256;
				tile = spriteram.read(offs+3)+((spriteram.read(offs+2) & 0xc0)<<2) + ((spriteram.read(offs+2) & 0x08)<<7);
				big  = spriteram.read(offs+2) & 4;
				if (big != 0) tile /= 4;
				flipx = spriteram.read(offs+2) & 0x10;
				flipy = spriteram.read(offs+2) & 0x20;
				palette = spriteram.read(offs+4) & 0x0f;
				drawgfx(bitmap,Machine.gfx[(big!=0)?2:1],
						tile,
						palette,
						flipx,flipy,
						sx,sy,
						Machine.visible_area,
						TRANSPARENCY_PEN, 15);
	
				/* kludge to clear shots */
				if (((spriteram.read(offs+2)==2) || (spriteram.read(offs+2)==0x12)) && (((tile>=0xd0) && (tile<=0xd5)) || ((tile>=0x20) && (tile<=0x25))))
					spriteram.write(offs+2, 0);
			}
		}
	}
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr mnight_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int scrollx,scrolly;
	
	
		if (bg_enable != 0)
			mnight_draw_background(bitmap_bg);
	
		scrollx = -((mnight_scrollx_ram.read(0)+mnight_scrollx_ram.read(1)*256) & 0x1FF);
		scrolly = -((mnight_scrolly_ram.read(0)+mnight_scrolly_ram.read(1)*256) & 0x1FF);
	
		if (sp_overdraw != 0)	/* overdraw sprite mode */
		{
			copyscrollbitmap(bitmap,bitmap_bg,1,new int[]{scrollx},1,new int[]{scrolly},Machine.visible_area,TRANSPARENCY_NONE,0);
			mnight_draw_sprites(bitmap_sp);
			mnight_draw_foreground(bitmap_sp);
			copybitmap(bitmap,bitmap_sp,0,0,0,0,Machine.visible_area,TRANSPARENCY_PEN, 15);
		}
		else			/* normal sprite mode */
		{
			copyscrollbitmap(bitmap,bitmap_bg,1,new int[]{scrollx},1,new int[]{scrolly},Machine.visible_area,TRANSPARENCY_NONE,0);
			mnight_draw_sprites(bitmap);
			mnight_draw_foreground(bitmap);
		}
	
	} };
}
