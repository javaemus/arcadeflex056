/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;
import static common.libc.expressions.*;

import static mame056.common.*;
import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;


public class rollrace
{
	
	
	static int ra_charbank[] = { 0,0 };
	static int ra_bkgpage = 0;
	static int ra_bkgflip = 0;
	static int ra_chrbank = 0;
	static int ra_bkgpen = 0;
	static int ra_bkgcol = 0;
	static int ra_flipy = 0;
	static int ra_flipx = 0;
	static int ra_spritebank =0 ;
	
	public static int	RA_FGCHAR_BASE 	= 0;
	public static int	RA_BGCHAR_BASE 	= 4;
	public static int	RA_SP_BASE	= 5;
	
	public static WriteHandlerPtr rollrace_charbank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	
		ra_charbank[offset&1] = data;
		ra_chrbank = ra_charbank[0] | (ra_charbank[1] << 1) ;
	} };
	
	
	public static WriteHandlerPtr rollrace_bkgpen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		ra_bkgpen = data;
	} };
	
	public static WriteHandlerPtr rollrace_spritebank_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                ra_spritebank = data;
            }
        };
	
	public static WriteHandlerPtr rollrace_backgroundpage_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                ra_bkgpage = data & 0x1f;
		ra_bkgflip = ( data & 0x80 ) >> 7;
	
		/* 0x80 flip vertical */
            }
        };
	
	public static WriteHandlerPtr rollrace_backgroundcolor_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		ra_bkgcol = data;
	} };
	
	public static WriteHandlerPtr rollrace_flipy_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		ra_flipy = data & 0x01;
	} };
	
	public static WriteHandlerPtr rollrace_flipx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		ra_flipx = data & 0x01;
	} };
	
	public static VhUpdatePtr rollrace_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
	
		int offs;
		int sx, sy;
		int scroll;
		int col;
	
		/* fill in background colour*/
		fillbitmap(bitmap,Machine.pens[ra_bkgpen],Machine.visible_area);
	
		/* draw road */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
			{
				if((ra_bkgflip)==0)
					{
					sy = ( 31 - offs / 32 ) ;
					}
				else
					sy = ( offs / 32 ) ;
	
				sx = ( offs%32 ) ;
	
				if(ra_flipx != 0)
					sx = 31-sx ;
	
				if(ra_flipy != 0)
					sy = 31-sy ;
	
				drawgfx(bitmap,
					Machine.gfx[RA_BGCHAR_BASE],
					memory_region(REGION_USER1).read(offs + ( ra_bkgpage * 1024 ))
					+ ((( memory_region(REGION_USER1).read(offs + 0x4000 + ( ra_bkgpage * 1024 )) & 0xc0 ) >> 6 ) * 256 ) ,
					ra_bkgcol,
					ra_flipx,(ra_bkgflip^ra_flipy),
					sx*8,sy*8,
					Machine.visible_area,TRANSPARENCY_PEN,0);
	
	
			}
	
	
	
	
		/* sprites */
		for ( offs = 0x80-4 ; offs >=0x0 ; offs -= 4)
		{
			int s_flipy = 0;
			int bank = 0;
	
			sy=spriteram.read(offs) - 16;
			sx=spriteram.read(offs+3) - 16;
	
			if(sx!=0 && sy!=0)
			{
	
			if(ra_flipx!=0)
				sx = 224 - sx;
			if(ra_flipy!=0)
				sy = 224 - sy;
	
			if((spriteram.read(offs+1) & 0x80) != 0)
				s_flipy = 1;
	
			bank = (( spriteram.read(offs+1) & 0x40 ) >> 6 ) ;
	
			if(bank != 0)
				bank += ra_spritebank;
	
			drawgfx(bitmap, Machine.gfx[ RA_SP_BASE + bank ],
				spriteram.read(offs+1) & 0x3f ,
				spriteram.read(offs+2) & 0x1f,
				ra_flipx,(s_flipy^ra_flipy)==0?1:0,
				sx,sy,
				Machine.visible_area,TRANSPARENCY_PEN,0);
			}
		}
	
	
	
	
		/* draw foreground characters */
		for (offs = videoram_size[0] - 1;offs >= 0;offs--)
		{
	
			sx =  offs % 32;
			sy =  offs / 32;
	
			scroll = ( 8 * sy + colorram.read(2 * sx) ) % 256;
			col = colorram.read( sx * 2 + 1 )&0x1f;
	
			if (ra_flipy == 0)
			{
			   scroll = (248 - scroll) % 256;
			}
	
			if (ra_flipx != 0) sx = 31 - sx;
	
			drawgfx(bitmap,Machine.gfx[RA_FGCHAR_BASE + ra_chrbank]  ,
				videoram.read( offs )  ,
				col,
				ra_flipx,ra_flipy,
				8*sx,scroll,
				Machine.visible_area,TRANSPARENCY_PEN,0);
	
		}
	
	
	
	} };
}
