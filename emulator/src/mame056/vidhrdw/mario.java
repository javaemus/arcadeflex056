/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

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
import static mame056.memoryH.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.cpuexec.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.inptport.readinputport;


public class mario
{
	
	
	
	static int gfx_bank,palette_bank;
	
	public static UBytePtr mario_scrolly = new UBytePtr();
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Mario Bros. has a 512x8 palette PROM; interstingly, bytes 0-255 contain an
	  inverted palette, as other Nintendo games like Donkey Kong, while bytes
	  256-511 contain a non inverted palette. This was probably done to allow
	  connection to both the special Nintendo and a standard monitor.
	  The palette PROM is connected to the RGB output this way:
	
	  bit 7 -- 220 ohm resistor -- inverter  -- RED
	        -- 470 ohm resistor -- inverter  -- RED
	        -- 1  kohm resistor -- inverter  -- RED
	        -- 220 ohm resistor -- inverter  -- GREEN
	        -- 470 ohm resistor -- inverter  -- GREEN
	        -- 1  kohm resistor -- inverter  -- GREEN
	        -- 220 ohm resistor -- inverter  -- BLUE
	  bit 0 -- 470 ohm resistor -- inverter  -- BLUE
	
	***************************************************************************/
        public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
	
        public static void COLOR(char[] colortable, int gfxn, int offs, int value){
            colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs] = (char) value;
        }
        
	public static VhConvertColorPromPtr mario_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
                int _palette = 0;
		
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int bit0,bit1,bit2;
	
			/* red component */
			bit0 = (color_prom.read() >> 5) & 1;
			bit1 = (color_prom.read() >> 6) & 1;
			bit2 = (color_prom.read() >> 7) & 1;
			palette[_palette++] = (char) (255 - (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2));
			/* green component */
			bit0 = (color_prom.read() >> 2) & 1;
			bit1 = (color_prom.read() >> 3) & 1;
			bit2 = (color_prom.read() >> 4) & 1;
			palette[_palette++] = (char) (255 - (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2));
			/* blue component */
			bit0 = (color_prom.read() >> 0) & 1;
			bit1 = (color_prom.read() >> 1) & 1;
			palette[_palette++] = (char) (255 - (0x55 * bit0 + 0xaa * bit1));
	
			color_prom.inc();
		}
	
		/* characters use the same palette as sprites, however characters */
		/* use only colors 64-127 and 192-255. */
		for (i = 0;i < 8;i++)
		{
			COLOR(colortable,0,4*i, 8*i + 64);
			COLOR(colortable,0,4*i+1, 8*i+1 + 64);
			COLOR(colortable,0,4*i+2, 8*i+2 + 64);
			COLOR(colortable,0,4*i+3, 8*i+3 + 64);
		}
		for (i = 0;i < 8;i++)
		{
			COLOR(colortable,0,4*i+8*4, 8*i + 192);
			COLOR(colortable,0,4*i+8*4+1, 8*i+1 + 192);
			COLOR(colortable,0,4*i+8*4+2, 8*i+2 + 192);
			COLOR(colortable,0,4*i+8*4+3, 8*i+3 + 192);
		}
	
		/* sprites */
		for (i = 0;i < TOTAL_COLORS(1);i++)
			COLOR(colortable,1,i, i);
            }
        };
	
	public static WriteHandlerPtr mario_gfxbank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (gfx_bank != (data & 1))
		{
			memset(dirtybuffer,1,videoram_size[0]);
			gfx_bank = data & 1;
		}
	} };
	
	
	
	public static WriteHandlerPtr mario_palettebank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (palette_bank != (data & 1))
		{
			memset(dirtybuffer,1,videoram_size[0]);
			palette_bank = data & 1;
		}
	} };
	
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr mario_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
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
	
				drawgfx(tmpbitmap,Machine.gfx[0],
						videoram.read(offs) + 256 * gfx_bank,
						(videoram.read(offs) >> 5) + 8 * palette_bank,
						0,0,
						8*sx,8*sy,
						null,TRANSPARENCY_NONE,0);
			}
		}
	
	
		/* copy the temporary bitmap to the screen */
		{
			int scrolly;
	
			/* I'm not positive the scroll direction is right */
			scrolly = -mario_scrolly.read() - 17;
			copyscrollbitmap(bitmap,tmpbitmap,0,new int[]{0},1,new int[]{scrolly},Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	
		/* Draw the sprites. */
		for (offs = 0;offs < spriteram_size[0];offs += 4)
		{
			if (spriteram.read(offs) != 0)
			{
				drawgfx(bitmap,Machine.gfx[1],
						spriteram.read(offs + 2),
						(spriteram.read(offs + 1) & 0x0f) + 16 * palette_bank,
						spriteram.read(offs + 1) & 0x80,spriteram.read(offs + 1) & 0x40,
						spriteram.read(offs + 3) - 8,240 - spriteram.read(offs) + 8,
						Machine.visible_area,TRANSPARENCY_PEN,0);
			}
		}
	} };
}
