/***************************************************************************

Tehkan World Cup - (c) Tehkan 1985


Ernesto Corvi
ernesto@imagina.com

Roberto Juan Fresca
robbiex@rocketmail.com

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

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;


public class tehkanwc
{
	
	public static UBytePtr tehkanwc_videoram1=new UBytePtr();
	public static int[] tehkanwc_videoram1_size=new int[1];
	static mame_bitmap tmpbitmap1 = null;
	public static UBytePtr dirtybuffer1=new UBytePtr();
	static int[] scroll_x=new int[2];
        static int scroll_y;
	static int led0,led1;
	
	
	public static VhStartPtr tehkanwc_vh_start = new VhStartPtr() { public int handler() 
	{
		if (generic_vh_start.handler() != 0)
			return 1;
	
		if ((tmpbitmap1 = bitmap_alloc(2 * Machine.drv.screen_width, Machine.drv.screen_height)) == null)
		{
			generic_vh_stop.handler();
			return 1;
		}
	
		if ((dirtybuffer1 = new UBytePtr(tehkanwc_videoram1_size[0])) == null)
		{
			bitmap_free(tmpbitmap1);
			generic_vh_stop.handler();
			return 1;
		}
		memset(dirtybuffer1,1,tehkanwc_videoram1_size[0]);
	
		return 0;
	} };
	
	public static VhStopPtr tehkanwc_vh_stop = new VhStopPtr() { public void handler() 
	{
		dirtybuffer1 = null;
		bitmap_free(tmpbitmap1);
		generic_vh_stop.handler();
	} };
	
	
	
	public static ReadHandlerPtr tehkanwc_videoram1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return tehkanwc_videoram1.read(offset);
	} };
	
	public static WriteHandlerPtr tehkanwc_videoram1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		tehkanwc_videoram1.write(offset, data);
		dirtybuffer1.write(offset, 1);
	} };
	
	public static ReadHandlerPtr tehkanwc_scroll_x_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return scroll_x[offset];
	} };
	
	public static ReadHandlerPtr tehkanwc_scroll_y_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return scroll_y;
	} };
	
	public static WriteHandlerPtr tehkanwc_scroll_x_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll_x[offset] = data;
	} };
	
	public static WriteHandlerPtr tehkanwc_scroll_y_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll_y = data;
	} };
	
	
	
	public static WriteHandlerPtr gridiron_led0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		led0 = data;
	} };
	public static WriteHandlerPtr gridiron_led1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		led1 = data;
	} };
	
	/*
	   Gridiron Fight has a LED display on the control panel, to let each player
	   choose the formation without letting the other know.
	   We emulate it by showing a character on the corner of the screen; the
	   association between the bits of the port and the led segments is:
	
	    ---0---
	   |       |
	   5       1
	   |       |
	    ---6---
	   |       |
	   4       2
	   |       |
	    ---3---
	
	   bit 7 = enable (0 = display off)
	 */
	
	static void gridiron_drawled(mame_bitmap bitmap, int led, int player)
	{
		int i;
	
	
		char ledvalues[] =
				{ 0x86, 0xdb, 0xcf, 0xe6, 0xed, 0xfd, 0x87, 0xff, 0xf3, 0xf1 };
	
	
		if ((led & 0x80) == 0) return;
	
		for (i = 0;i < 10;i++)
		{
			if (led == ledvalues[i] ) break;
		}
	
		if (i < 10)
		{
			if (player == 0)
				drawgfx(bitmap,Machine.gfx[0],
						0xc0 + i,
						0x0a,
						0,0,
						0,232,
						Machine.visible_area,TRANSPARENCY_NONE,0);
			else
				drawgfx(bitmap,Machine.gfx[0],
						0xc0 + i,
						0x03,
						1,1,
						0,16,
						Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	else logerror("unknown LED %02x for player %d\n",led,player);
	}
	
	
	
	public static VhUpdatePtr tehkanwc_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		/* draw the background */
		for (offs = tehkanwc_videoram1_size[0]-2;offs >= 0;offs -= 2 )
		{
			if (dirtybuffer1.read(offs)!=0 || dirtybuffer1.read(offs + 1) != 0)
			{
				int sx,sy;
	
	
				dirtybuffer1.write(offs, 0);
                                dirtybuffer1.write(offs + 1, 0);
	
				sx = offs % 64;
				sy = offs / 64;
	
				drawgfx(tmpbitmap1,Machine.gfx[2],
						tehkanwc_videoram1.read(offs) + ((tehkanwc_videoram1.read(offs+1) & 0x30) << 4),
						tehkanwc_videoram1.read(offs+1) & 0x0f,
						tehkanwc_videoram1.read(offs+1) & 0x40, tehkanwc_videoram1.read(offs+1) & 0x80,
						sx*8,sy*8,
						null,TRANSPARENCY_NONE,0);
			}
		}
		{
			int scrolly = -scroll_y;
			int scrollx = -(scroll_x[0] + 256 * scroll_x[1]);
			copyscrollbitmap(bitmap,tmpbitmap1,1,new int[]{scrollx},1,new int[]{scrolly},Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	
	
		/* draw the foreground chars which don't have priority over sprites */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			int sx,sy;
	
	
			dirtybuffer[offs] = 0;
	
			sx = offs % 32;
			sy = offs / 32;
	
			if ((colorram.read(offs) & 0x20) != 0)
				drawgfx(bitmap,Machine.gfx[0],
						videoram.read(offs) + ((colorram.read(offs) & 0x10) << 4),
						colorram.read(offs) & 0x0f,
						colorram.read(offs) & 0x40, colorram.read(offs) & 0x80,
						sx*8,sy*8,
						Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	
	
		/* draw sprites */
		for (offs = 0;offs < spriteram_size[0];offs += 4)
		{
			drawgfx(bitmap,Machine.gfx[1],
					spriteram.read(offs+0) + ((spriteram.read(offs+1) & 0x08) << 5),
					spriteram.read(offs+1) & 0x07,
					spriteram.read(offs+1) & 0x40,spriteram.read(offs+1) & 0x80,
					spriteram.read(offs+2) + ((spriteram.read(offs+1) & 0x20) << 3) - 0x80,spriteram.read(offs+3),
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	
	
		/* draw the foreground chars which have priority over sprites */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			int sx,sy;
	
	
			dirtybuffer[offs] = 0;
	
			sx = offs % 32;
			sy = offs / 32;
	
			if ((colorram.read(offs) & 0x20)==0)
				drawgfx(bitmap,Machine.gfx[0],
						videoram.read(offs) + ((colorram.read(offs) & 0x10) << 4),
						colorram.read(offs) & 0x0f,
						colorram.read(offs) & 0x40, colorram.read(offs) & 0x80,
						sx*8,sy*8,
						Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	
		gridiron_drawled(bitmap,led0,0);
		gridiron_drawled(bitmap,led1,1);
	} };
}
