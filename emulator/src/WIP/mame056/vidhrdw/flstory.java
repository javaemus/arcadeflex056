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
import static mame056.memoryH.*;
import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static common.libc.cstdlib.rand;


public class flstory
{
	
	
	static int char_bank,palette_bank;
	
	
	public static VhStartPtr flstory_vh_start = new VhStartPtr() { public int handler() 
	{
		paletteram = new UBytePtr(0x200);
		paletteram_2 = new UBytePtr(0x200);
		return generic_vh_start.handler();
	} };
	
	public static VhStopPtr flstory_vh_stop = new VhStopPtr() { public void handler() 
	{
		paletteram = null;		
		paletteram_2 = null;
		
		generic_vh_stop.handler();
	} };
	
	
	
	public static WriteHandlerPtr flstory_palette_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if ((offset & 0x100) != 0)
			paletteram_xxxxBBBBGGGGRRRR_split2_w.handler((offset & 0xff) + (palette_bank << 8),data);
		else
			paletteram_xxxxBBBBGGGGRRRR_split1_w.handler((offset & 0xff) + (palette_bank << 8),data);
	} };
	
	public static WriteHandlerPtr flstory_gfxctrl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		char_bank = (data & 0x10) >> 4;
		palette_bank = (data & 0x20) >> 5;
	//usrintf_showmessage("%04x: gfxctrl = %02x\n",cpu_get_pc(),data);
	} };
	
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr flstory_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		for (offs = videoram_size[0] - 2;offs >= 0;offs -= 2)
		{
			if (dirtybuffer[offs]!=0 || dirtybuffer[offs+1]!=0)
			{
				int sx,sy;
	
	
				dirtybuffer[offs] = 0;
				dirtybuffer[offs+1] = 0;
	
				sx = (offs/2)%32;
				sy = (offs/2)/32;
	
				drawgfx(tmpbitmap,Machine.gfx[0],
						videoram.read(offs) + ((videoram.read(offs + 1) & 0xc0) << 2) + 0x400 + 0x800 * char_bank,
						videoram.read(offs + 1) & 0x07,
						videoram.read(offs + 1) & 0x08, videoram.read(offs + 1) & 0x10,
						8*sx,8*sy,
						Machine.visible_area,TRANSPARENCY_NONE,0);
			}
		}
	
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	
		for (offs = spriteram_size[0]-4;offs >= 0;offs -= 4)
		{
			int code,sx,sy,flipx,flipy;
	
	
			code = spriteram.read(offs+2) + ((spriteram.read(offs+1) & 0x30) << 4);
			sx = spriteram.read(offs+3);
			sy = 240 - spriteram.read(offs+0) - 1;
			flipx = spriteram.read(offs+1)&0x40;
			flipy = spriteram.read(offs+1)&0x80;
	
			drawgfx(bitmap,Machine.gfx[1],
					code,
					spriteram.read(offs+1) & 0x0f,
					flipx,flipy,
					sx,sy,
					Machine.visible_area,TRANSPARENCY_PEN,15);
			/* wrap around */
			if (sx > 240)
				drawgfx(bitmap,Machine.gfx[1],
						code,
						spriteram.read(offs+1) & 0x0f,
						flipx,flipy,
						sx-256,sy,
						Machine.visible_area,TRANSPARENCY_PEN,15);
		}
	
		/* redraw chars with priority over sprites */
		for (offs = videoram_size[0] - 2;offs >= 0;offs -= 2)
		{
			if ((videoram.read(offs + 1) & 0x20) !=0)
			{
				int sx,sy;
	
	
				sx = (offs/2)%32;
				sy = (offs/2)/32;
	
				drawgfx(bitmap,Machine.gfx[0],
						videoram.read(offs) + ((videoram.read(offs + 1) & 0xc0) << 2) + 0x400 + 0x800 * char_bank,
						videoram.read(offs + 1) & 0x07,
						videoram.read(offs + 1) & 0x08, videoram.read(offs + 1) & 0x10,
						8*sx,8*sy,
						Machine.visible_area,TRANSPARENCY_PEN,15);
			}
		}
	} };
}
