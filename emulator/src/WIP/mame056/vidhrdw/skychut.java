/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

  (c) 12/2/1998 Lee Taylor

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
import static mame056.cpuexec.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static common.libc.cstring.*;

public class skychut
{
	
	
	
	static int flipscreen;
	
	
	public static WriteHandlerPtr skychut_vh_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	/*	if (flipscreen != (data & 0x8f))
		{
			flipscreen = (data & 0x8f);
			memset(dirtybuffer,1,videoram_size);
		}
	*/
	} };
	
	
	public static WriteHandlerPtr skychut_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (colorram.read(offset) != data)
		{
			dirtybuffer[offset] = 1;
	
			colorram.write(offset, data);
		}
	} };
	
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr skychut_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
		if (full_refresh != 0)
			memset (dirtybuffer, 1, videoram_size[0]);
	
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			if (dirtybuffer[offs] != 0)
			{
				int sx,sy;
	
	
				dirtybuffer[offs] = 0;
	
				sx = offs % 32;
				sy = offs / 32;
	
				drawgfx(bitmap,Machine.gfx[0],
						videoram.read(offs),
						colorram.read(offs),
						flipscreen,flipscreen,
						8*sx,8*sy,
						Machine.visible_area,TRANSPARENCY_NONE,0);
			}
		}
	
	} };
	
	public static UBytePtr iremm15_chargen = new UBytePtr();
	
	static void iremm15_drawgfx(mame_bitmap bitmap, int ch, int color, int back, int x, int y)
	{
		int mask;
		int i;
	
		for (i=0; i<8; i++, x++) {
			mask=iremm15_chargen.read(ch*8+i);
			plot_pixel.handler(bitmap,x,y+7,(mask&0x80)!=0?color:back);
			plot_pixel.handler(bitmap,x,y+6,(mask&0x40)!=0?color:back);
			plot_pixel.handler(bitmap,x,y+5,(mask&0x20)!=0?color:back);
			plot_pixel.handler(bitmap,x,y+4,(mask&0x10)!=0?color:back);
			plot_pixel.handler(bitmap,x,y+3,(mask&8)!=0?color:back);
			plot_pixel.handler(bitmap,x,y+2,(mask&4)!=0?color:back);
			plot_pixel.handler(bitmap,x,y+1,(mask&2)!=0?color:back);
			plot_pixel.handler(bitmap,x,y,(mask&1)!=0?color:back);
		}
	}
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr iremm15_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
		if (full_refresh != 0)
			memset (dirtybuffer, 1, videoram_size[0]);
	
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			if (dirtybuffer[offs] != 0)
			{
				int sx,sy;
	
	
				dirtybuffer[offs] = 0;
	
				sx = offs % 32;
				sy = offs / 32;
	
				iremm15_drawgfx(bitmap,
                                                videoram.read(offs),
                                                Machine.pens[colorram.read(offs)],
                                                Machine.pens[7], // space beam not color 0
                                                8*sx,8*sy);
			}
		}
	
	} };
	
}
