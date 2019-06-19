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

public class hanaawas
{
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	***************************************************************************/
	public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
        
	public static void COLOR(char []colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs]) = (char) value;
        }
        
	public static VhConvertColorPromPtr hanaawas_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
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
	
		color_prom.inc( 0x10 );
		/* color_prom now points to the beginning of the lookup table */
	
	
		/* character lookup table.  The 1bpp tiles really only use colors 0-0x0f and the
		   3bpp ones 0x10-0x1f */
	
		for (i = 0;i < TOTAL_COLORS(0)/8 ;i++)
		{
			COLOR(colortable,0,i*8+0, color_prom.read(i*4+0x00)& 0x0f);
			COLOR(colortable,0,i*8+1, color_prom.read(i*4+0x01)& 0x0f);
			COLOR(colortable,0,i*8+2, color_prom.read(i*4+0x02)& 0x0f);
			COLOR(colortable,0,i*8+3, color_prom.read(i*4+0x03)& 0x0f);
			COLOR(colortable,0,i*8+4, color_prom.read(i*4+0x80)& 0x0f);
			COLOR(colortable,0,i*8+5, color_prom.read(i*4+0x81)& 0x0f);
			COLOR(colortable,0,i*8+6, color_prom.read(i*4+0x82)& 0x0f);
			COLOR(colortable,0,i*8+7, color_prom.read(i*4+0x83)& 0x0f);
		}
	} };
	
	
	public static WriteHandlerPtr hanaawas_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int offs2;
	
	
		colorram.write(offset,data);
	
		/* dirty both current and next offsets */
		offs2 = (offset + (flip_screen()!=0 ? -1 : 1)) & 0x03ff;
	
		dirtybuffer[offset] = 1;
		dirtybuffer[offs2 ] = 1;
	} };
	
	
	public static WriteHandlerPtr hanaawas_portB_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bit 7 is flip screen */
		flip_screen_set(~data & 0x80);
	} };
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	
	public static VhUpdatePtr hanaawas_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs,offs_adj;
	
	
		if (full_refresh != 0)
		{
			memset(dirtybuffer, 1, videoram_size[0]);
		}
	
	
	
		offs_adj = flip_screen()!=0 ? 1 : -1;
	
		for (offs = videoram_size[0] - 1; offs >= 0; offs--)
		{
			if (dirtybuffer[offs] != 0)
			{
				int sx,sy,col,code,bank,offs2;
	
				dirtybuffer[offs] = 0;
	            sx = offs % 32;
				sy = offs / 32;
	
				if (flip_screen() != 0)
				{
					sx = 31 - sx;
					sy = 31 - sy;
				}
	
				/* the color is determined by the current color byte, but the bank is via the
				   previous one!!! */
				offs2 = (offs + offs_adj) & 0x03ff;
	
				col  = colorram.read(offs)& 0x1f;
				code = videoram.read(offs)+ ((colorram.read(offs2)& 0x20) << 3);
				bank = (colorram.read(offs2)& 0x40) >> 6;
	
				drawgfx(bitmap,Machine.gfx[bank],
						code,col,
						flip_screen(),flip_screen(),
						sx*8,sy*8,
						Machine.visible_area,TRANSPARENCY_NONE,0);
	        }
		}
	} };
}
