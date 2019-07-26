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

import static WIP.mame056.machine.atarifb.*;
import static mame056.usrintrfH.UI_COLOR_NORMAL;
import static common.libc.cstdio.*;
import static WIP.mame056.drivers.atarifb.atarifb_game;
import static WIP.mame056.drivers.atarifb.atarifb_lamp1;
import static WIP.mame056.drivers.atarifb.atarifb_lamp2;

public class atarifb
{
	
	/* local */
	public static int[] atarifb_alphap1_vram_size=new int[2];
	public static int[] atarifb_alphap2_vram_size=new int[2];
	public static UBytePtr atarifb_alphap1_vram = new UBytePtr();
	public static UBytePtr atarifb_alphap2_vram = new UBytePtr();
	public static UBytePtr atarifb_scroll_register = new UBytePtr();
	public static UBytePtr alphap1_dirtybuffer = new UBytePtr();
	public static UBytePtr alphap2_dirtybuffer = new UBytePtr();
	
	
	
	static rectangle bigfield_area = new rectangle(  4*8, 34*8-1, 0*8, 32*8-1 );
	static rectangle left_area =     new rectangle(  0*8,  3*8-1, 0*8, 32*8-1 );
	static rectangle right_area =    new rectangle( 34*8, 38*8-1, 0*8, 32*8-1 );
	
	/***************************************************************************
	***************************************************************************/
	public static WriteHandlerPtr atarifb_alphap1_vram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (atarifb_alphap1_vram.read(offset) != data)
		{
			atarifb_alphap1_vram.write(offset, data);
	
			alphap1_dirtybuffer.write(offset, 1);
		}
	} };
	
	public static WriteHandlerPtr atarifb_alphap2_vram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (atarifb_alphap2_vram.read(offset) != data)
		{
			atarifb_alphap2_vram.write(offset, data);
	
			alphap2_dirtybuffer.write(offset, 1);
		}
	} };
	
	/***************************************************************************
	***************************************************************************/
	public static WriteHandlerPtr atarifb_scroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (data - 8 != atarifb_scroll_register.read())
		{
			atarifb_scroll_register.write( data - 8 );
			memset(dirtybuffer,1,videoram_size[0]);
		}
	} };
	
	/***************************************************************************
	***************************************************************************/
	
	public static VhStartPtr atarifb_vh_start = new VhStartPtr() { public int handler() 
	{
		if (generic_vh_start.handler()!=0)
			return 1;
	
		alphap1_dirtybuffer = new UBytePtr (atarifb_alphap1_vram_size[0]);
		alphap2_dirtybuffer = new UBytePtr (atarifb_alphap2_vram_size[0]);
		if ((alphap1_dirtybuffer==null) || (alphap2_dirtybuffer==null))
		{
			generic_vh_stop.handler();
			return 1;
		}
	
		memset(alphap1_dirtybuffer, 1, atarifb_alphap1_vram_size[0]);
		memset(alphap2_dirtybuffer, 1, atarifb_alphap2_vram_size[0]);
		memset(dirtybuffer, 1, videoram_size[0]);
	
		return 0;
	} };
	
	/***************************************************************************
	***************************************************************************/
	
	public static VhStopPtr atarifb_vh_stop = new VhStopPtr() { public void handler() 
	{
		generic_vh_stop.handler();
		alphap1_dirtybuffer = null;
		alphap2_dirtybuffer = null;
	} };
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr atarifb_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs,obj;
		int sprite_bank;
	
		if (full_refresh != 0)
		{
			memset(alphap1_dirtybuffer, 1, atarifb_alphap1_vram_size[0]);
			memset(alphap2_dirtybuffer, 1, atarifb_alphap2_vram_size[0]);
			memset(dirtybuffer,1,videoram_size[0]);
		}
	
		/* Soccer uses a different graphics set for sprites */
		if (atarifb_game == 4)
			sprite_bank = 2;
		else
			sprite_bank = 1;
	
		/* for every character in the Player 1 Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = atarifb_alphap1_vram_size[0] - 1;offs >= 0;offs--)
		{
			if (alphap1_dirtybuffer.read(offs) != 0)
			{
				int charcode;
				int flipbit;
				int disable;
				int sx,sy;
	
				alphap1_dirtybuffer.write(offs, 0);
	
				sx = 8 * (offs / 32) + 35*8;
				sy = 8 * (offs % 32) + 8;
	
				charcode = atarifb_alphap1_vram.read(offs) & 0x3f;
				flipbit = (atarifb_alphap1_vram.read(offs) & 0x40) >> 6;
				disable = (atarifb_alphap1_vram.read(offs) & 0x80) >> 7;
	
				if (disable == 0)
				{
					drawgfx(bitmap,Machine.gfx[0],
						charcode, 0,
						flipbit,flipbit,sx,sy,
						right_area,TRANSPARENCY_NONE,0);
				}
			}
		}
	
		/* for every character in the Player 2 Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = atarifb_alphap2_vram_size[0] - 1;offs >= 0;offs--)
		{
			if (alphap2_dirtybuffer.read(offs) != 0)
			{
				int charcode;
				int flipbit;
				int disable;
				int sx,sy;
	
				alphap2_dirtybuffer.write(offs, 0);
	
				sx = 8 * (offs / 32);
				sy = 8 * (offs % 32) + 8;
	
				charcode = atarifb_alphap2_vram.read(offs) & 0x3f;
				flipbit = (atarifb_alphap2_vram.read(offs) & 0x40) >> 6;
				disable = (atarifb_alphap2_vram.read(offs) & 0x80) >> 7;
	
				if (disable == 0)
				{
					drawgfx(bitmap,Machine.gfx[0],
						charcode, 0,
						flipbit,flipbit,sx,sy,
						left_area,TRANSPARENCY_NONE,0);
				}
			}
		}
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
			if (dirtybuffer[offs] != 0)
			{
				int charcode;
				int flipx,flipy;
				int sx,sy;
	
				dirtybuffer[offs]=0;
	
				charcode = videoram.read(offs)& 0x3f;
				flipx = (videoram.read(offs)& 0x40) >> 6;
				flipy = (videoram.read(offs)& 0x80) >> 7;
	
				sx = (8 * (offs % 32) - atarifb_scroll_register.read());
				sy = 8 * (offs / 32) + 8;
	
				/* Soccer hack */
				if (atarifb_game == 4)
				{
					sy += 8;
				}
	
				/* Baseball hack */
				if (atarifb_game == 0x03) sx -= 8;
	
				if (sx < 0) sx += 256;
				if (sx > 255) sx -= 256;
	
				drawgfx(tmpbitmap,Machine.gfx[1],
						charcode, 0,
						flipx,flipy,sx,sy,
						null,TRANSPARENCY_NONE,0);
			}
		}
	
		/* copy the character mapped graphics */
		copybitmap(bitmap,tmpbitmap,0,0,8*3,0,bigfield_area,TRANSPARENCY_NONE,0);
	
		/* Draw our motion objects */
		for (obj=0;obj<16;obj++)
		{
			int charcode;
			int flipx,flipy;
			int sx,sy;
			int shade = 0;
	
			sy = 255 - spriteram.read(obj*2 + 1);
			if (sy == 255) continue;
	
			charcode = spriteram.read(obj*2)& 0x3f;
			flipx = (spriteram.read(obj*2)& 0x40);
			flipy = (spriteram.read(obj*2)& 0x80);
			sx = spriteram.read(obj*2 + 0x20)+ 8*3;
	
			/* Note on Atari Soccer: */
			/* There are 3 sets of 2 bits each, where the 2 bits represent */
			/* black, dk grey, grey and white. I think the 3 sets determine the */
			/* color of each bit in the sprite, but I haven't implemented it that way. */
			if (atarifb_game == 4)
			{
				shade = ((spriteram.read(obj*2+1 + 0x20)) & 0x07);
	
				drawgfx(bitmap,Machine.gfx[sprite_bank+1],
					charcode, shade,
					flipx,flipy,sx,sy,
					bigfield_area,TRANSPARENCY_PEN,0);
	
				shade = ((spriteram.read(obj*2+1 + 0x20)) & 0x08) >> 3;
			}
	
			drawgfx(bitmap,Machine.gfx[sprite_bank],
					charcode, shade,
					flipx,flipy,sx,sy,
					bigfield_area,TRANSPARENCY_PEN,0);
	
			/* If this isn't soccer, handle the multiplexed sprites */
			if (atarifb_game != 4)
			{
				/* The down markers are multiplexed by altering the y location during */
				/* mid-screen. We'll fake it by essentially doing the same thing here. */
				if ((charcode == 0x11) && (sy == 0x07))
				{
					sy = 0xf1; /* When multiplexed, it's 0x10...why? */
					drawgfx(bitmap,Machine.gfx[sprite_bank],
						charcode, 0,
						flipx,flipy,sx,sy,
						bigfield_area,TRANSPARENCY_PEN,0);
				}
			}
		}
	
	/* If this isn't Soccer, print the plays at the top of the screen */
	if (atarifb_game != 4)
	{
		int x;
		String buf1="                    ", buf2="                    ";
	
		switch (atarifb_game)
		{
			case 0x01: /* 2-player football */
				switch (atarifb_lamp1)
				{
					case 0x00:
						buf1="                    ";
						break;
					case 0x01:
						buf1="SWEEP               ";
						break;
					case 0x02:
						buf1="KEEPER              ";
						break;
					case 0x04:
						buf1="BOMB                ";
						break;
					case 0x08:
						buf1="DOWN & OUT          ";
						break;
				}
				switch (atarifb_lamp2)
				{
					case 0x00:
						buf2="                    ";
						break;
					case 0x01:
						buf2="SWEEP               ";
						break;
					case 0x02:
						buf2="KEEPER              ";
						break;
					case 0x04:
						buf2="BOMB                ";
						break;
					case 0x08:
						buf2="DOWN & OUT          ";
						break;
				}
				break;
			case 0x02: /* 4-player football */
				switch (atarifb_lamp1 & 0x1f)
				{
					case 0x01:
						buf1="SLANT OUT           ";
						break;
					case 0x02:
						buf1="SLANT IN            ";
						break;
					case 0x04:
						buf1="BOMB                ";
						break;
					case 0x08:
						buf1="DOWN & OUT          ";
						break;
					case 0x10:
						buf1="KICK                ";
						break;
					default:
						buf1="                    ";
						break;
				}
				switch (atarifb_lamp2 & 0x1f)
				{
					case 0x01:
						buf2="SLANT OUT           ";
						break;
					case 0x02:
						buf2="SLANT IN            ";
						break;
					case 0x04:
						buf2="BOMB                ";
						break;
					case 0x08:
						buf2="DOWN & OUT          ";
						break;
					case 0x10:
						buf2="KICK                ";
						break;
					default:
						buf2="                    ";
						break;
				}
				break;
			case 0x03: /* 2-player baseball */
				switch (atarifb_lamp1 & 0x0f)
				{
					case 0x01:
						buf1="RT SWING/FASTBALL   ";
						break;
					case 0x02:
						buf1="LT SWING/CHANGE-UP  ";
						break;
					case 0x04:
						buf1="RT BUNT/CURVE BALL  ";
						break;
					case 0x08:
						buf1="LT BUNT/KNUCKLE BALL";
						break;
					default:
						buf1="                    ";
						break;
				}
				switch (atarifb_lamp2 & 0x0f)
				{
					case 0x01:
						buf2="RT SWING/FASTBALL   ";
						break;
					case 0x02:
						buf2="LT SWING/CHANGE-UP  ";
						break;
					case 0x04:
						buf2="RT BUNT/CURVE BALL  ";
						break;
					case 0x08:
						buf2="LT BUNT/KNUCKLE BALL";
						break;
					default:
						buf2="                    ";
						break;
				}
				break;
			default:
				buf1="                    ";
				buf2="                    ";
				break;
		}
                
		for (x = 0;x < 20;x++)
				drawgfx(bitmap,Machine.uifont,buf1.charAt(x),UI_COLOR_NORMAL,0,0,6*x + 24*8,0,null,TRANSPARENCY_NONE,0);
	
		for (x = 0;x < 20;x++)
				drawgfx(bitmap,Machine.uifont,buf2.charAt(x),UI_COLOR_NORMAL,0,0,6*x,0,null,TRANSPARENCY_NONE,0);
	}
	} };
}
