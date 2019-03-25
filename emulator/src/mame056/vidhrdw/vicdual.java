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
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;

import static mame056.drivers.vicdual.*;


public class vicdual
{
	
	
	
	public static UBytePtr vicdual_characterram = new UBytePtr();
	static char[] dirtycharacter=new char[256];
	
	static int palette_bank;
	
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  The VIC dual game board has one 32x8 palette PROM. The color code is taken
	  from the three most significant bits of the character code, plus two
	  additional palette bank bits.
	  The palette PROM is connected to the RGB output this way:
	
	  bit 7 -- 22 ohm resistor  -- RED   \
	        -- 22 ohm resistor  -- BLUE  |  foreground
	        -- 22 ohm resistor  -- GREEN /
	        -- Unused
	        -- 22 ohm resistor  -- RED   \
	        -- 22 ohm resistor  -- BLUE  |  background
	        -- 22 ohm resistor  -- GREEN /
	  bit 0 -- Unused
	
	***************************************************************************/
        /* for b&w games we'll use the Head On PROM */
        static char bw_color_prom[] =
        {
                /* for b/w games, let's use the Head On PROM */
                0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,
                0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,0xE1,
        };
                
	public static VhConvertColorPromPtr vicdual_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
		
		if (color_prom == null) color_prom = new UBytePtr(bw_color_prom);
	
		for (i = 0;i < Machine.drv.total_colors / 2;i++)
		{
			int bit,r,g,b;
	
	
			/* background red component */
			bit = (color_prom.read(i) >> 3) & 0x01;
			r = 0xff * bit;
			/* background green component */
			bit = (color_prom.read(i) >> 1) & 0x01;
			g = 0xff * bit;
			/* background blue component */
			bit = (color_prom.read(i) >> 2) & 0x01;
			b = 0xff * bit;
	
			palette_set_color(2*i,r,g,b);
	
			/* foreground red component */
			bit = (color_prom.read(i) >> 7) & 0x01;
			r = 0xff * bit;
			/* foreground green component */
			bit = (color_prom.read(i) >> 5) & 0x01;
			g = 0xff * bit;
			/* foreground blue component */
			bit = (color_prom.read(i) >> 6) & 0x01;
			b = 0xff * bit;
	
			palette_set_color(2*i+1,r,g,b);
		}
	
		palette_bank = 0;
	
		{
			/* Heiankyo Alien doesn't write to port 0x40, it expects it to default to 3 */
			if (Machine.gamedrv == driver_heiankyo)
				palette_bank = 3;
	
			/* and many others expect it to default to 1 */
			if (Machine.gamedrv == driver_invinco ||
					Machine.gamedrv == driver_digger ||
					Machine.gamedrv == driver_tranqgun)
				palette_bank = 1;
		}
            }
        };
		
	public static WriteHandlerPtr vicdual_characterram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (vicdual_characterram.read(offset) != data)
		{
			dirtycharacter[(offset / 8) & 0xff] = 1;
	
			vicdual_characterram.write(offset, data);
		}
	} };
	
	public static ReadHandlerPtr vicdual_characterram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return vicdual_characterram.read(offset);
	} };
	
	public static WriteHandlerPtr vicdual_palette_bank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (palette_bank != (data & 3))
		{
			palette_bank = data & 3;
			memset(dirtybuffer,1,videoram_size[0]);
		}
	} };
	
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr vicdual_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		if (full_refresh != 0)
		{
			memset(dirtybuffer,1,videoram_size[0]);
		}
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			int charcode;
	
	
			charcode = videoram.read(offs);
	
			if ((dirtybuffer[offs] != 0) || (dirtycharacter[charcode] != 0))
			{
				int sx,sy;
	
	
				/* decode modified characters */
				if (dirtycharacter[charcode] == 1)
				{
					decodechar(Machine.gfx[0],charcode,vicdual_characterram,Machine.drv.gfxdecodeinfo[0].gfxlayout);
					dirtycharacter[charcode] = 2;
				}
	
	
				dirtybuffer[offs] = 0;
	
				sx = offs % 32;
				sy = offs / 32;
	
				drawgfx(bitmap,Machine.gfx[0],
						charcode,
						(charcode >> 5) + 8 * palette_bank,
						0,0,
						8*sx,8*sy,
						Machine.visible_area,TRANSPARENCY_NONE,0);
	
			}
		}
	
	
		for (offs = 0;offs < 256;offs++)
		{
			if (dirtycharacter[offs] == 2) dirtycharacter[offs] = 0;
		}
	} };
}
