/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

  CHANGES:
  MAB 05 MAR 99 - changed overlay support to use artwork functions
***************************************************************************/

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

import static WIP.mame056.machine.sbrkout.*;

public class sbrkout
{
	
	public static UBytePtr sbrkout_horiz_ram = new UBytePtr();
	public static UBytePtr sbrkout_vert_ram = new UBytePtr();
	
	/* The first entry defines the color with which the bitmap is filled initially */
	/* The array is terminated with an entry with negative coordinates. */
	/* At least two entries are needed. */
	static artwork_element sbrkout_ol[] =
	{
		new artwork_element( new rectangle(208, 247,   8, 217), 0x20, 0x20, 0xff,   OVERLAY_DEFAULT_OPACITY),	/* blue */
		new artwork_element( new rectangle(176, 207,   8, 217), 0xff, 0x80, 0x10,   OVERLAY_DEFAULT_OPACITY),	/* orange */
		new artwork_element( new rectangle(144, 175,   8, 217), 0x20, 0xff, 0x20,   OVERLAY_DEFAULT_OPACITY),	/* green */
		new artwork_element( new rectangle(96, 143,   8, 217), 0xff, 0xff, 0x20,   OVERLAY_DEFAULT_OPACITY),	/* yellow */
		new artwork_element( new rectangle(16,	23,   8, 217), 0x20, 0x20, 0xff,   OVERLAY_DEFAULT_OPACITY)	/* blue */
		//{{-1,-1,-1,-1},0,0,0,0}
	};
	
	
	/***************************************************************************
	***************************************************************************/
	
	public static VhStartPtr sbrkout_vh_start = new VhStartPtr() { public int handler() 
	{
		int start_pen = 2;	/* leave space for black and white */
	
		if (generic_vh_start.handler()!=0)
			return 1;
	
		overlay_create(sbrkout_ol, start_pen);
	
		return 0;
	} };
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr sbrkout_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
		int ball;
	
	
		if (full_refresh != 0)
			memset(dirtybuffer,1,videoram_size[0]);
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			if (dirtybuffer[offs] != 0)
			{
				int code,sx,sy,color;
	
	
				dirtybuffer[offs]=0;
	
				code = videoram.read(offs)& 0x3f;
	
				sx = 8*(offs % 32);
				sy = 8*(offs / 32);
	
				/* Check the "draw" bit */
				color = ((videoram.read(offs)& 0x80)>>7);
	
				drawgfx(tmpbitmap,Machine.gfx[0],
						code, color,
						0,0,sx,sy,
						Machine.visible_area,TRANSPARENCY_NONE,0);
			}
		}
	
		/* copy the character mapped graphics */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	
		/* Draw each one of our three balls */
		for (ball=2;ball>=0;ball--)
		{
			int sx,sy,code;
	
	
			sx = 31*8-sbrkout_horiz_ram.read(ball*2);
			sy = 30*8-sbrkout_vert_ram.read(ball*2);
	
			code = ((sbrkout_vert_ram.read(ball*2+1) & 0x80) >> 7);
	
			drawgfx(bitmap,Machine.gfx[1],
					code,1,
					0,0,sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	} };
	
}
