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

public class redalert
{
	
	public static UBytePtr redalert_backram = new UBytePtr();
	public static UBytePtr redalert_spriteram1 = new UBytePtr();
	public static UBytePtr redalert_spriteram2 = new UBytePtr();
	public static UBytePtr redalert_characterram = new UBytePtr();
	
	static char[] redalert_dirtyback=new char[0x400];
	static char[] redalert_dirtycharacter=new char[0x100];
	static char[] redalert_backcolor=new char[0x400];
	
	/* There might be a color PROM that dictates this? */
	/* These guesses are based on comparing the color bars on the test
	   screen with the picture in the manual */
	static char color_lookup[] = {
		1,1,1,1,1,1,1,1,1,1,1,1,3,3,3,3,
		1,1,1,1,1,1,1,1,3,3,3,3,3,3,3,3,
		3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
		1,1,1,1,1,1,1,1,1,1,3,3,3,3,3,3,
		1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
		1,1,1,1,1,1,1,1,1,1,3,3,3,3,3,3,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	
		1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,2,
		2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
		2,2,2,2,1,1,1,1,1,1,1,1,1,1,1,1,
		1,1,1,1,1,1,1,1,1,1,3,3,3,3,3,3,
		3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
		3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
		3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
		3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3
	};
	
	static int backcolor;
	
	public static WriteHandlerPtr redalert_c040_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* Only seems to load D0-D3 into a flip-flop. */
		/* D0/D1 seem to head off to unconnected circuits */
		/* D2 connects to a "NL" line, and NOTted to a "NH" line */
		/* D3 connects to a "YI" line */
	} };
	
	public static WriteHandlerPtr redalert_backcolor_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* Only seems to load D0-D2 into a flip-flop. */
		/* Outputs feed into RAM which seems to feed to RGB lines. */
		backcolor = data & 0x07;
	} };
	
	
	/***************************************************************************
	redalert_backram_w
	***************************************************************************/
	
	public static WriteHandlerPtr redalert_backram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int charnum;
	
		charnum = offset / 8 % 0x400;
	
		if ((redalert_backram.read(offset) != data) ||
			(redalert_backcolor[charnum] != backcolor))
		{
			redalert_dirtyback[charnum] = 1;
			dirtybuffer[charnum] = 1;
			redalert_backcolor[charnum] = (char) backcolor;
	
			redalert_backram.write(offset, data);
		}
	} };
	
	/***************************************************************************
	redalert_spriteram1_w
	***************************************************************************/
	
	public static WriteHandlerPtr redalert_spriteram1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (redalert_spriteram1.read(offset) != data)
		{
			redalert_dirtycharacter[((offset / 8) % 0x80) + 0x80] = 1;
	
			redalert_spriteram1.write(offset, data);
		}
	} };
	
	/***************************************************************************
	redalert_spriteram2_w
	***************************************************************************/
	
	public static WriteHandlerPtr redalert_spriteram2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (redalert_spriteram2.read(offset) != data)
		{
			redalert_dirtycharacter[((offset / 8) % 0x80) + 0x80] = 1;
	
			redalert_spriteram2.write(offset, data);
		}
	} };
	
	/***************************************************************************
	redalert_characterram_w
	***************************************************************************/
	
	public static WriteHandlerPtr redalert_characterram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (redalert_characterram.read(offset) != data)
		{
			redalert_dirtycharacter[((offset / 8) % 0x80)] = 1;
	
			redalert_characterram.write(offset, data);
		}
	} };
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr redalert_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs,i;
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			int charcode;
			int stat_transparent;
	
	
			charcode = videoram.read(offs);
	
			if (dirtybuffer[offs]!=0 || redalert_dirtycharacter[charcode]!=0)
			{
				int sx,sy,color;
	
	
				/* decode modified background */
				if (redalert_dirtyback[offs] == 1)
				{
					decodechar(Machine.gfx[0],offs,redalert_backram,
								Machine.drv.gfxdecodeinfo[0].gfxlayout);
					redalert_dirtyback[offs] = 2;
				}
	
				/* decode modified characters */
				if (redalert_dirtycharacter[charcode] == 1)
				{
					if (charcode < 0x80)
						decodechar(Machine.gfx[1],charcode,redalert_characterram,
									Machine.drv.gfxdecodeinfo[1].gfxlayout);
					else
						decodechar(Machine.gfx[2],charcode-0x80,redalert_spriteram1,
									Machine.drv.gfxdecodeinfo[2].gfxlayout);
					redalert_dirtycharacter[charcode] = 2;
				}
	
	
				dirtybuffer[offs] = 0;
	
				sx = 31 - offs / 32;
				sy = offs % 32;
	
				stat_transparent = TRANSPARENCY_NONE;
	
				/* First layer of color */
				if (charcode >= 0xC0)
				{
					stat_transparent = TRANSPARENCY_COLOR;
	
					color = color_lookup[charcode];
	
					drawgfx(tmpbitmap,Machine.gfx[2],
							charcode-0x80,
							color,
							0,0,
							8*sx,8*sy,
							Machine.visible_area,TRANSPARENCY_NONE,0);
				}
	
				/* Second layer - background */
				color = redalert_backcolor[offs];
				drawgfx(tmpbitmap,Machine.gfx[0],
						offs,
						color,
						0,0,
						8*sx,8*sy,
						Machine.visible_area,stat_transparent,0);
	
				/* Third layer - alphanumerics & sprites */
				if (charcode < 0x80)
				{
					color = color_lookup[charcode];
					drawgfx(tmpbitmap,Machine.gfx[1],
							charcode,
							color,
							0,0,
							8*sx,8*sy,
							Machine.visible_area,TRANSPARENCY_COLOR,0);
				}
				else if (charcode < 0xC0)
				{
					color = color_lookup[charcode];
					drawgfx(tmpbitmap,Machine.gfx[2],
							charcode-0x80,
							color,
							0,0,
							8*sx,8*sy,
							Machine.visible_area,TRANSPARENCY_COLOR,0);
				}
	
			}
		}
	
		for (i = 0;i < 256;i++)
		{
			if (redalert_dirtycharacter[i] == 2)
				redalert_dirtycharacter[i] = 0;
		}
	
		for (i = 0;i < 0x400;i++)
		{
			if (redalert_dirtyback[i] == 2)
				redalert_dirtyback[i] = 0;
		}
	
		/* copy the character mapped graphics */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	
	} };
	
}
