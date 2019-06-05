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

public class punchout
{
	
	
	public static int TOP_MONITOR_ROWS = 28;
	public static int BOTTOM_MONITOR_ROWS = 28;
	
	public static int BIGSPRITE_WIDTH = 128;
	public static int BIGSPRITE_HEIGHT = 256;
	public static int ARMWREST_BIGSPRITE_WIDTH = 256;
	public static int ARMWREST_BIGSPRITE_HEIGHT = 128;
	
	public static UBytePtr punchout_videoram2 = new UBytePtr();
	public static int[] punchout_videoram2_size = new int[2];
	public static UBytePtr punchout_bigsprite1ram = new UBytePtr();
	public static int[] punchout_bigsprite1ram_size = new int[2];
	public static UBytePtr punchout_bigsprite2ram = new UBytePtr();
	public static int[] punchout_bigsprite2ram_size = new int[2];
	public static UBytePtr punchout_scroll = new UBytePtr();
	public static UBytePtr punchout_bigsprite1 = new UBytePtr();
	public static UBytePtr punchout_bigsprite2 = new UBytePtr();
	public static UBytePtr punchout_palettebank = new UBytePtr();
	public static UBytePtr dirtybuffer2 = new UBytePtr(), bs1dirtybuffer = new UBytePtr(), bs2dirtybuffer = new UBytePtr();
	static mame_bitmap bs1tmpbitmap, bs2tmpbitmap;
	
	static int top_palette_bank,bottom_palette_bank;
	
	static rectangle topvisiblearea = new rectangle
        (
		0*8, 32*8-1,
		0*8, TOP_MONITOR_ROWS*8-1
        );
        
	static rectangle bottomvisiblearea = new rectangle
        (
		0*8, 32*8-1,
		TOP_MONITOR_ROWS*8, (TOP_MONITOR_ROWS+BOTTOM_MONITOR_ROWS)*8-1
        );
        
	static rectangle backgroundvisiblearea = new rectangle
        (
		0*8, 64*8-1,
		TOP_MONITOR_ROWS*8, (TOP_MONITOR_ROWS+BOTTOM_MONITOR_ROWS)*8-1
        );
	
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Punch Out has a six 512x4 palette PROMs (one per gun; three for the top
	  monitor chars, three for everything else).
	  The PROMs are connected to the RGB output this way:
	
	  bit 3 -- 240 ohm resistor -- inverter  -- RED/GREEN/BLUE
	        -- 470 ohm resistor -- inverter  -- RED/GREEN/BLUE
	        -- 1  kohm resistor -- inverter  -- RED/GREEN/BLUE
	  bit 0 -- 2  kohm resistor -- inverter  -- RED/GREEN/BLUE
	
	***************************************************************************/
	static void convert_palette(char[] palette, UBytePtr color_prom) {
                int i;
                int _palette = 0;
	
		for (i = 0;i < 1024;i++)
		{
			int bit0,bit1,bit2,bit3;
	
	
			bit0 = (color_prom.read(0) >> 0) & 0x01;
			bit1 = (color_prom.read(0) >> 1) & 0x01;
			bit2 = (color_prom.read(0) >> 2) & 0x01;
			bit3 = (color_prom.read(0) >> 3) & 0x01;
			palette[_palette++] = (char) (255 - (0x10 * bit0 + 0x21 * bit1 + 0x46 * bit2 + 0x88 * bit3));
			bit0 = (color_prom.read(1024) >> 0) & 0x01;
			bit1 = (color_prom.read(1024) >> 1) & 0x01;
			bit2 = (color_prom.read(1024) >> 2) & 0x01;
			bit3 = (color_prom.read(1024) >> 3) & 0x01;
			palette[_palette++] = (char) (255 - (0x10 * bit0 + 0x21 * bit1 + 0x46 * bit2 + 0x88 * bit3));
			bit0 = (color_prom.read(2*1024) >> 0) & 0x01;
			bit1 = (color_prom.read(2*1024) >> 1) & 0x01;
			bit2 = (color_prom.read(2*1024) >> 2) & 0x01;
			bit3 = (color_prom.read(2*1024) >> 3) & 0x01;
			palette[_palette++] = (char) (255 - (0x10 * bit0 + 0x21 * bit1 + 0x46 * bit2 + 0x88 * bit3));
	
			color_prom.inc();
		}
	
		/* reserve the last color for the transparent pen (none of the game colors has */
		/* these RGB components) */
		palette[_palette++] = 240;
		palette[_palette++] = 240;
		palette[_palette++] = 240;
            
        };
	
	/* these depend on jumpers on the board and change from game to game */
	static int gfx0inv,gfx1inv,gfx2inv,gfx3inv;
        
        public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
		
        public static void COLOR(char[] colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + (offs)]) = (char) value;
        }	
	
	public static VhConvertColorPromPtr punchout_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
		
	
		convert_palette(palette,color_prom);
	
	
		/* top monitor chars */
		for (i = 0;i < TOTAL_COLORS(0);i++)
			COLOR(colortable,0,i ^ gfx0inv, i);
	
		/* bottom monitor chars */
		for (i = 0;i < TOTAL_COLORS(1);i++)
			COLOR(colortable,1,i ^ gfx1inv, i + 512);
	
		/* big sprite #1 */
		for (i = 0;i < TOTAL_COLORS(2);i++)
		{
			if (i % 8 == 0) COLOR(colortable,2,i ^ gfx2inv, 1024);	/* transparent */
			else COLOR(colortable,2,i ^ gfx2inv, i + 512);
		}
	
		/* big sprite #2 */
		for (i = 0;i < TOTAL_COLORS(3);i++)
		{
			if (i % 4 == 0) COLOR(colortable,3,i ^ gfx3inv, 1024);	/* transparent */
			else COLOR(colortable,3,i ^ gfx3inv, i + 512);
		}
            }
        };
	
	public static VhConvertColorPromPtr armwrest_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
		//#define TOTAL_COLORS(gfxn) (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity)
		//#define COLOR(gfxn,offs) (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + (offs)])
	
	
		convert_palette(palette,color_prom);
	
	
		/* top monitor / bottom monitor backround chars */
		for (i = 0;i < TOTAL_COLORS(0);i++)
			COLOR(colortable,0,i, i);
	
		/* bottom monitor foreground chars */
		for (i = 0;i < TOTAL_COLORS(1);i++)
			COLOR(colortable,1,i, i + 512);
	
		/* big sprite #1 */
		for (i = 0;i < TOTAL_COLORS(2);i++)
		{
			if (i % 8 == 7) COLOR(colortable,2,i, 1024);	/* transparent */
			else COLOR(colortable,2,i, i + 512);
		}
	
		/* big sprite #2 - pen order is inverted */
		for (i = 0;i < TOTAL_COLORS(3);i++)
		{
			if (i % 4 == 3) COLOR(colortable,3,i ^ 3, 1024);	/* transparent */
			else COLOR(colortable,3,i ^ 3, i + 512);
		}
            }
        };
	
	
	public static void gfx_fix()
	{
		/* one graphics ROM (4v) doesn't */
		/* exist but must be seen as a 0xff fill for colors to come out properly */
		memset(new UBytePtr(memory_region(REGION_GFX3), 0x2c000),0xff,0x4000);
	}
	
	public static InitDriverPtr init_punchout = new InitDriverPtr() { public void handler() 
	{
		gfx_fix();
	
		gfx0inv = 0x03;
		gfx1inv = 0xfc;
		gfx2inv = 0xff;
		gfx3inv = 0xfc;
	} };
	
	public static InitDriverPtr init_spnchout = new InitDriverPtr() { public void handler() 
	{
		gfx_fix();
	
		gfx0inv = 0x00;
		gfx1inv = 0xff;
		gfx2inv = 0xff;
		gfx3inv = 0xff;
	} };
	
	public static InitDriverPtr init_spnchotj = new InitDriverPtr() { public void handler() 
	{
		gfx_fix();
	
		gfx0inv = 0xfc;
		gfx1inv = 0xff;
		gfx2inv = 0xff;
		gfx3inv = 0xff;
	} };
	
	public static InitDriverPtr init_armwrest = new InitDriverPtr() { public void handler() 
	{
		gfx_fix();
	
		/* also, ROM 2k is enabled only when its top half is accessed. The other half must */
		/* be seen as a 0xff fill for colors to come out properly */
		memset(new UBytePtr(memory_region(REGION_GFX2), 0x08000),0xff,0x2000);
	} };
	
	
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	public static VhStartPtr punchout_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((dirtybuffer = new char[videoram_size[0]]) == null)
			return 1;
		memset(dirtybuffer,1,videoram_size[0]);
	
		if ((dirtybuffer2 = new UBytePtr(punchout_videoram2_size[0])) == null)
		{
			dirtybuffer = null;
			return 1;
		}
		memset(dirtybuffer2,1,punchout_videoram2_size[0]);
	
		if ((tmpbitmap = bitmap_alloc(512,480)) == null)
		{
			dirtybuffer = null;
			dirtybuffer2 = null;
			return 1;
		}
	
		if ((bs1dirtybuffer = new UBytePtr(punchout_bigsprite1ram_size[0])) == null)
		{
			bitmap_free(tmpbitmap);
			dirtybuffer = null;
			dirtybuffer2 = null;
			return 1;
		}
		memset(bs1dirtybuffer,1,punchout_bigsprite1ram_size[0]);
	
		if ((bs1tmpbitmap = bitmap_alloc(BIGSPRITE_WIDTH,BIGSPRITE_HEIGHT)) == null)
		{
			bitmap_free(tmpbitmap);
			dirtybuffer = null;
			dirtybuffer2 = null;
			bs1dirtybuffer = null;
			return 1;
		}
	
		if ((bs2dirtybuffer = new UBytePtr(punchout_bigsprite2ram_size[0])) == null)
		{
			bitmap_free(tmpbitmap);
			bitmap_free(bs1tmpbitmap);
			dirtybuffer = null;
			dirtybuffer2 = null;
			bs1dirtybuffer = null;
			return 1;
		}
		memset(bs2dirtybuffer,1,punchout_bigsprite2ram_size[0]);
	
		if ((bs2tmpbitmap = bitmap_alloc(BIGSPRITE_WIDTH,BIGSPRITE_HEIGHT)) == null)
		{
			bitmap_free(tmpbitmap);
			bitmap_free(bs1tmpbitmap);
			dirtybuffer = null;
			dirtybuffer2 = null;
			bs1dirtybuffer = null;
			bs2dirtybuffer = null;
			return 1;
		}
	
		return 0;
	} };
	
	public static VhStartPtr armwrest_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((dirtybuffer = new char[videoram_size[0]]) == null)
			return 1;
		memset(dirtybuffer,1,videoram_size[0]);
	
		if ((dirtybuffer2 = new UBytePtr(punchout_videoram2_size[0])) == null)
		{
			dirtybuffer = null;
			return 1;
		}
		memset(dirtybuffer2,1,punchout_videoram2_size[0]);
	
		if ((tmpbitmap = bitmap_alloc(512,480)) == null)
		{
			dirtybuffer = null;
			dirtybuffer2 = null;
			return 1;
		}
	
		if ((bs1dirtybuffer = new UBytePtr(punchout_bigsprite1ram_size[0])) == null)
		{
			bitmap_free(tmpbitmap);
			dirtybuffer = null;
			dirtybuffer2 = null;
			return 1;
		}
		memset(bs1dirtybuffer,1,punchout_bigsprite1ram_size[0]);
	
		if ((bs1tmpbitmap = bitmap_alloc(ARMWREST_BIGSPRITE_WIDTH,ARMWREST_BIGSPRITE_HEIGHT)) == null)
		{
			bitmap_free(tmpbitmap);
			dirtybuffer = null;
			dirtybuffer2 = null;
			bs1dirtybuffer = null;
			return 1;
		}
	
		if ((bs2dirtybuffer = new UBytePtr(punchout_bigsprite2ram_size[0])) == null)
		{
			bitmap_free(tmpbitmap);
			bitmap_free(bs1tmpbitmap);
			dirtybuffer = null;
			dirtybuffer2 = null;
			bs1dirtybuffer = null;
			return 1;
		}
		memset(bs2dirtybuffer,1,punchout_bigsprite2ram_size[0]);
	
		if ((bs2tmpbitmap = bitmap_alloc(BIGSPRITE_WIDTH,BIGSPRITE_HEIGHT)) == null)
		{
			bitmap_free(tmpbitmap);
			bitmap_free(bs1tmpbitmap);
			dirtybuffer = null;
			dirtybuffer2 = null;
			bs1dirtybuffer = null;
			bs2dirtybuffer = null;
			return 1;
		}
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Stop the video hardware emulation.
	
	***************************************************************************/
	public static VhStopPtr punchout_vh_stop = new VhStopPtr() { public void handler() 
	{
		dirtybuffer = null;
		dirtybuffer2 = null;
		bs1dirtybuffer = null;
		bs2dirtybuffer = null;
		bitmap_free(tmpbitmap);
		bitmap_free(bs1tmpbitmap);
		bitmap_free(bs2tmpbitmap);
	} };
	
	
	
	public static WriteHandlerPtr punchout_videoram2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (punchout_videoram2.read(offset) != data)
		{
			dirtybuffer2.write(offset, 1);
	
			punchout_videoram2.write(offset, data);
		}
	} };
	
	public static WriteHandlerPtr punchout_bigsprite1ram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (punchout_bigsprite1ram.read(offset) != data)
		{
			bs1dirtybuffer.write(offset, 1);
	
			punchout_bigsprite1ram.write(offset, data);
		}
	} };
	
	public static WriteHandlerPtr punchout_bigsprite2ram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (punchout_bigsprite2ram.read(offset) != data)
		{
			bs2dirtybuffer.write(offset, 1);
	
			punchout_bigsprite2ram.write(offset, data);
		}
	} };
	
	
	
	public static WriteHandlerPtr punchout_palettebank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		punchout_palettebank.write( data );
	
		if (top_palette_bank != ((data >> 1) & 0x01))
		{
			top_palette_bank = (data >> 1) & 0x01;
			memset(dirtybuffer,1,videoram_size[0]);
		}
		if (bottom_palette_bank != ((data >> 0) & 0x01))
		{
			bottom_palette_bank = (data >> 0) & 0x01;
			memset(dirtybuffer2,1,punchout_videoram2_size[0]);
			memset(bs1dirtybuffer,1,punchout_bigsprite1ram_size[0]);
			memset(bs2dirtybuffer,1,punchout_bigsprite2ram_size[0]);
		}
	} };
	
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr punchout_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = videoram_size[0] - 2;offs >= 0;offs -= 2)
		{
			if (dirtybuffer[offs]!=0 || dirtybuffer[offs + 1]!=0)
			{
				int sx,sy;
	
	
				dirtybuffer[offs] = 0;
				dirtybuffer[offs + 1] = 0;
	
				sx = offs/2 % 32;
				sy = offs/2 / 32;
	
				drawgfx(tmpbitmap,Machine.gfx[0],
						videoram.read(offs) + 256 * (videoram.read(offs + 1) & 0x03),
						((videoram.read(offs + 1) & 0x7c) >> 2) + 64 * top_palette_bank,
						videoram.read(offs + 1) & 0x80,0,
						8*sx,8*sy - 16,
						topvisiblearea,TRANSPARENCY_NONE,0);
			}
		}
	
		for (offs = punchout_videoram2_size[0] - 2;offs >= 0;offs -= 2)
		{
			if ((dirtybuffer2.read(offs) | dirtybuffer2.read(offs + 1)) != 0)
			{
				int sx,sy;
	
	
				dirtybuffer2.write(offs, 0);
				dirtybuffer2.write(offs + 1, 0);
	
				sx = offs/2 % 64;
				sy = offs/2 / 64;
	
				drawgfx(tmpbitmap,Machine.gfx[1],
						punchout_videoram2.read(offs) + 256 * (punchout_videoram2.read(offs + 1) & 0x03),
						((punchout_videoram2.read(offs + 1) & 0x7c) >> 2) + 64 * bottom_palette_bank,
						punchout_videoram2.read(offs + 1) & 0x80,0,
						8*sx,8*sy + 8*TOP_MONITOR_ROWS - 16,
						backgroundvisiblearea,TRANSPARENCY_NONE,0);
			}
		}
	
		for (offs = punchout_bigsprite1ram_size[0] - 4;offs >= 0;offs -= 4)
		{
			if ((bs1dirtybuffer.read(offs) | bs1dirtybuffer.read(offs + 1) | bs1dirtybuffer.read(offs + 3)) != 0)
			{
				int sx,sy;
	
	
				bs1dirtybuffer.write(offs, 0);
				bs1dirtybuffer.write(offs + 1, 0);
				bs1dirtybuffer.write(offs + 3, 0);
	
				sx = offs/4 % 16;
				sy = offs/4 / 16;
	
				drawgfx(bs1tmpbitmap,Machine.gfx[2],
						punchout_bigsprite1ram.read(offs) + 256 * (punchout_bigsprite1ram.read(offs + 1) & 0x1f),
						(punchout_bigsprite1ram.read(offs + 3) & 0x1f) + 32 * bottom_palette_bank,
						punchout_bigsprite1ram.read(offs + 3) & 0x80,0,
						8*sx,8*sy,
						null,TRANSPARENCY_NONE,0);
			}
		}
	
		for (offs = punchout_bigsprite2ram_size[0] - 4;offs >= 0;offs -= 4)
		{
			if ((bs2dirtybuffer.read(offs) | bs2dirtybuffer.read(offs + 1) | bs2dirtybuffer.read(offs + 3)) != 0)
			{
				int sx,sy;
	
	
				bs2dirtybuffer.write(offs, 0);
				bs2dirtybuffer.write(offs + 1, 0);
				bs2dirtybuffer.write(offs + 3, 0);
	
				sx = offs/4 % 16;
				sy = offs/4 / 16;
	
				drawgfx(bs2tmpbitmap,Machine.gfx[3],
						punchout_bigsprite2ram.read(offs) + 256 * (punchout_bigsprite2ram.read(offs + 1) & 0x0f),
						(punchout_bigsprite2ram.read(offs + 3) & 0x3f) + 64 * bottom_palette_bank,
						punchout_bigsprite2ram.read(offs + 3) & 0x80,0,
						8*sx,8*sy,
						null,TRANSPARENCY_NONE,0);
			}
		}
	
	
		/* copy the character mapped graphics */
		{
			int[] scroll=new int[64];
	
	
			for (offs = 0;offs < TOP_MONITOR_ROWS;offs++)
				scroll[offs] = 0;
			for (offs = 0;offs < BOTTOM_MONITOR_ROWS;offs++)
				scroll[TOP_MONITOR_ROWS + offs] = -(58 + punchout_scroll.read(2*(offs+2)) + 256 * (punchout_scroll.read(2*(offs+2) + 1) & 0x01));
	
			copyscrollbitmap(bitmap,tmpbitmap,TOP_MONITOR_ROWS + BOTTOM_MONITOR_ROWS,scroll,0,new int[]{0},Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	
		/* copy the two big sprites */
		{
			int zoom;
	
			zoom = punchout_bigsprite1.read(0) + 256 * (punchout_bigsprite1.read(1) & 0x0f);
			if (zoom != 0)
			{
				int sx,sy;
				int startx,starty;
				int incxx,incyy;
	
				sx = 4096 - (punchout_bigsprite1.read(2) + 256 * (punchout_bigsprite1.read(3) & 0x0f));
				if (sx > 4096-4*127) sx -= 4096;
	
				sy = -(punchout_bigsprite1.read(4) + 256 * (punchout_bigsprite1.read(5) & 1));
				if (sy <= -256 + zoom/0x40) sy += 512;
	
				incxx = zoom << 6;
				incyy = zoom << 6;
	
				startx = -sx * 0x4000;
				starty = -sy * 0x10000;
				startx += 3740 * zoom;	/* adjustment to match the screen shots */
				starty -= 178 * zoom;	/* and make the hall of fame picture nice */
	
				if ((punchout_bigsprite1.read(6) & 1) != 0)	/* flip x */
				{
					startx = (bs1tmpbitmap.width << 16) - startx - 1;
					incxx = -incxx;
				}
	
				if ((punchout_bigsprite1.read(7) & 1) != 0)	/* display in top monitor */
				{
					copyrozbitmap(bitmap,bs1tmpbitmap,
						startx,starty + 0x200*(2) * zoom,
						incxx,0,0,incyy,	/* zoom, no rotation */
						0,	/* no wraparound */
						topvisiblearea,TRANSPARENCY_COLOR,1024,0);
				}
				if ((punchout_bigsprite1.read(7) & 2) != 0)	/* display in bottom monitor */
				{
					copyrozbitmap(bitmap,bs1tmpbitmap,
						startx,starty - 0x200*TOP_MONITOR_ROWS * zoom,
						incxx,0,0,incyy,	/* zoom, no rotation */
						0,	/* no wraparound */
						bottomvisiblearea,TRANSPARENCY_COLOR,1024,0);
				}
			}
		}
		{
			int sx,sy;
	
	
			sx = 512 - (punchout_bigsprite2.read(0) + 256 * (punchout_bigsprite2.read(1) & 1));
			if (sx > 512-127) sx -= 512;
			sx -= 55;	/* adjustment to match the screen shots */
	
			sy = -punchout_bigsprite2.read(2) + 256 * (punchout_bigsprite2.read(3) & 1);
			sy += 3;	/* adjustment to match the screen shots */
	
			copybitmap(bitmap,bs2tmpbitmap,
					punchout_bigsprite2.read(4) & 1,0,
					sx,sy + 8*TOP_MONITOR_ROWS - 16,
					bottomvisiblearea,TRANSPARENCY_COLOR,1024);
		}
	} };
	
	
	public static VhUpdatePtr armwrest_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for (offs = punchout_videoram2_size[0] - 2;offs >= 0;offs -= 2)
		{
			if ((dirtybuffer2.read(offs) | dirtybuffer2.read(offs + 1)) != 0)
			{
				int sx,sy;
	
	
				dirtybuffer2.write(offs, 0);
				dirtybuffer2.write(offs + 1, 0);
	
				sx = offs/2 % 32;
				sy = offs/2 / 32;
	
				if (sy >= 32)
				{
					/* top screen */
					sy -= 32;
					drawgfx(tmpbitmap,Machine.gfx[0],
							punchout_videoram2.read(offs) + 256 * (punchout_videoram2.read(offs + 1) & 0x03) +
							8 * (punchout_videoram2.read(offs + 1) & 0x80),
							((punchout_videoram2.read(offs + 1) & 0x7c) >> 2) + 64 * top_palette_bank,
							0,0,
							8*sx,8*sy - 16,
							topvisiblearea,TRANSPARENCY_NONE,0);
				}
				else
					/* bottom screen background */
					drawgfx(tmpbitmap,Machine.gfx[0],
							punchout_videoram2.read(offs) + 256 * (punchout_videoram2.read(offs + 1) & 0x03),
							128 + ((punchout_videoram2.read(offs + 1) & 0x7c) >> 2) + 64 * bottom_palette_bank,
							punchout_videoram2.read(offs + 1) & 0x80,0,
							8*sx,8*sy + 8*TOP_MONITOR_ROWS - 16,
							backgroundvisiblearea,TRANSPARENCY_NONE,0);
			}
		}
	
		for (offs = punchout_bigsprite1ram_size[0] - 4;offs >= 0;offs -= 4)
		{
			if ((bs1dirtybuffer.read(offs) | bs1dirtybuffer.read(offs + 1) | bs1dirtybuffer.read(offs + 3)) != 0)
			{
				int sx,sy;
	
	
				bs1dirtybuffer.write(offs, 0);
				bs1dirtybuffer.write(offs + 1, 0);
				bs1dirtybuffer.write(offs + 3, 0);
	
				sx = offs/4 % 16;
				sy = offs/4 / 16;
				if (sy >= 16)
				{
					sy -= 16;
					sx += 16;
				}
	
				drawgfx(bs1tmpbitmap,Machine.gfx[2],
						punchout_bigsprite1ram.read(offs) + 256 * (punchout_bigsprite1ram.read(offs + 1) & 0x1f),
						(punchout_bigsprite1ram.read(offs + 3) & 0x1f) + 32 * bottom_palette_bank,
						punchout_bigsprite1ram.read(offs + 3) & 0x80,0,
						8*sx,8*sy,
						null,TRANSPARENCY_NONE,0);
			}
		}
	
		for (offs = punchout_bigsprite2ram_size[0] - 4;offs >= 0;offs -= 4)
		{
			if ((bs2dirtybuffer.read(offs) | bs2dirtybuffer.read(offs + 1) | bs2dirtybuffer.read(offs + 3)) != 0)
			{
				int sx,sy;
	
	
				bs2dirtybuffer.write(offs, 0);
				bs2dirtybuffer.write(offs + 1, 0);
				bs2dirtybuffer.write(offs + 3, 0);
	
				sx = offs/4 % 16;
				sy = offs/4 / 16;
	
				drawgfx(bs2tmpbitmap,Machine.gfx[3],
						punchout_bigsprite2ram.read(offs) + 256 * (punchout_bigsprite2ram.read(offs + 1) & 0x0f),
						(punchout_bigsprite2ram.read(offs + 3) & 0x3f) + 64 * bottom_palette_bank,
						punchout_bigsprite2ram.read(offs + 3) & 0x80,0,
						8*sx,8*sy,
						null,TRANSPARENCY_NONE,0);
			}
		}
	
	
		/* copy the character mapped graphics */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	
	
		/* copy the two big sprites */
		{
			int zoom;
	
			zoom = punchout_bigsprite1.read(0) + 256 * (punchout_bigsprite1.read(1) & 0x0f);
			if (zoom != 0)
			{
				int sx,sy;
				int startx,starty;
				int incxx,incyy;
	
				sx = 4096 - (punchout_bigsprite1.read(2) + 256 * (punchout_bigsprite1.read(3) & 0x0f));
				if (sx > 4096-4*127) sx -= 4096;
	
				sy = -(punchout_bigsprite1.read(4) + 256 * (punchout_bigsprite1.read(5) & 1));
				if (sy <= -256 + zoom/0x40) sy += 512;
	
				incxx = zoom << 6;
				incyy = zoom << 6;
	
				startx = -sx * 0x4000;
				starty = -sy * 0x10000;
				startx += 3740 * zoom;	/* adjustment to match the screen shots */
				starty -= 178 * zoom;	/* and make the hall of fame picture nice */
	
				if ((punchout_bigsprite1.read(6) & 1) != 0)	/* flip x */
				{
					startx = (bs1tmpbitmap.width << 16) - startx - 1;
					incxx = -incxx;
				}
	
				if ((punchout_bigsprite1.read(7) & 1) != 0)	/* display in top monitor */
				{
					copyrozbitmap(bitmap,bs1tmpbitmap,
						startx,starty + 0x200*(2) * zoom,
						incxx,0,0,incyy,	/* zoom, no rotation */
						0,	/* no wraparound */
						topvisiblearea,TRANSPARENCY_COLOR,1024,0);
				}
				if ((punchout_bigsprite1.read(7) & 2) != 0)	/* display in bottom monitor */
				{
					copyrozbitmap(bitmap,bs1tmpbitmap,
						startx,starty - 0x200*TOP_MONITOR_ROWS * zoom,
						incxx,0,0,incyy,	/* zoom, no rotation */
						0,	/* no wraparound */
						bottomvisiblearea,TRANSPARENCY_COLOR,1024,0);
				}
			}
		}
		{
			int sx,sy;
	
	
			sx = 512 - (punchout_bigsprite2.read(0) + 256 * (punchout_bigsprite2.read(1) & 1));
			if (sx > 512-127) sx -= 512;
			sx -= 55;	/* adjustment to match the screen shots */
	
			sy = -punchout_bigsprite2.read(2) + 256 * (punchout_bigsprite2.read(3) & 1);
			sy += 3;	/* adjustment to match the screen shots */
	
			copybitmap(bitmap,bs2tmpbitmap,
					punchout_bigsprite2.read(4) & 1,0,
					sx,sy + 8*TOP_MONITOR_ROWS - 16,
					bottomvisiblearea,TRANSPARENCY_COLOR,1024);
		}
	
	
		/* draw the foregound chars */
		for (offs = videoram_size[0] - 2;offs >= 0;offs -= 2)
		{
			int sx,sy;
	
	
			dirtybuffer[offs] = 0;
			dirtybuffer[offs + 1] = 0;
	
			sx = offs/2 % 32;
			sy = offs/2 / 32;
	
			drawgfx(bitmap,Machine.gfx[1],
					videoram.read(offs) + 256 * (videoram.read(offs + 1) & 0x07),
					((videoram.read(offs + 1) & 0xf8) >> 3) + 32 * bottom_palette_bank,
					videoram.read(offs + 1) & 0x80,0,
					8*sx,8*sy + 8*TOP_MONITOR_ROWS - 16,
					backgroundvisiblearea,TRANSPARENCY_PEN,7);
		}
	} };
}
