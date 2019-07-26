/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

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
import static mame056.artworkH.*;
import static mame056.artwork.*;
import static mame056.cpuexec.*;
import static mame056.cpuintrfH.*;
import static mame056.timerH.*;
import static mame056.timer.*;

import static WIP.mame056.machine.avalnche.*;

public class avalnche
{
	
	
	/* The first entry defines the color with which the bitmap is filled initially */
	/* The array is terminated with an entry with negative coordinates. */
	/* At least two entries are needed. */
	static artwork_element avalnche_ol[] =
	{
		new artwork_element(new rectangle(  0, 255,  16,  25), 0x20, 0xff, 0xff,   OVERLAY_DEFAULT_OPACITY),	/* cyan */
		new artwork_element(new rectangle(  0, 255,  26,  35), 0x20, 0x20, 0xff,   OVERLAY_DEFAULT_OPACITY),	/* blue */
		new artwork_element(new rectangle(  0, 255,  36,  44), 0xff, 0xff, 0x20,   OVERLAY_DEFAULT_OPACITY),	/* yellow */
		new artwork_element(new rectangle(  0, 255,  45,  55), 0xff, 0x80, 0x10,   OVERLAY_DEFAULT_OPACITY),	/* orange */
		new artwork_element(new rectangle(  0, 255,  56, 255), 0x20, 0xff, 0xff,   OVERLAY_DEFAULT_OPACITY)	/* cyan */
		//{{-1,-1,-1,-1},0,0,0,0}
	};
	
	
	public static VhStartPtr avalnche_vh_start = new VhStartPtr() { public int handler() 
	{
		int start_pen = 2;	/* leave space for black and white */
	
		if (generic_vh_start.handler()!=0)
			return 1;
	
		overlay_create(avalnche_ol, start_pen);
	
		return 0;
	} };
	
	public static WriteHandlerPtr avalnche_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		videoram.write(offset,data);
	
		if (offset >= 0x200)
		{
			int x,y,i;
	
			x = 8 * (offset % 32);
			y = offset / 32;
	
			for (i = 0;i < 8;i++)
				plot_pixel.handler(tmpbitmap,x+7-i,y,Machine.pens[(data >> i) & 1]);
		}
	} };
	
	public static VhUpdatePtr avalnche_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		if (full_refresh != 0)
		{
			int offs;
	
	
			for (offs = 0;offs < videoram_size[0]; offs++)
				avalnche_videoram_w.handler(offs,videoram.read(offs));
		}
	
		/* copy the character mapped graphics */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	} };
}
