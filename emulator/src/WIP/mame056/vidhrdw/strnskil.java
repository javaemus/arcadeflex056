/******************************************************************************

Strength & Skill (c) 1984 Sun Electronics

Video hardware driver by Uki

	19/Jun/2001 -

******************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;
import static common.libc.cstring.*;

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
import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;


public class strnskil
{
	
	static int strnskil_flipscreen, strnskil_scrl_ctrl;
        static int[] strnskil_xscroll=new int[2];
	
	public static VhConvertColorPromPtr strnskil_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
                int _palette = 0;
                int _colortable = 0;
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			palette[_palette++] = (char) (color_prom.read(0)*0x11);
			palette[_palette++] = (char) (color_prom.read(Machine.drv.total_colors)*0x11);
			palette[_palette++] = (char) (color_prom.read(2*Machine.drv.total_colors)*0x11);
	
			color_prom.inc();
		}
	
		color_prom.inc( 2*Machine.drv.total_colors );
	
		/* color_prom now points to the beginning of the lookup table */
	
		/* sprites lookup table */
		for (i=0; i<512; i++)
			colortable[_colortable++] = color_prom.readinc();
	
		/* bg lookup table */
		for (i=0; i<512; i++){
			colortable[_colortable++] = color_prom.readinc();
                }
	
            }
        };
	
	public static WriteHandlerPtr strnskil_scroll_x_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		strnskil_xscroll[offset] = data;
	} };
	
	public static WriteHandlerPtr strnskil_scrl_ctrl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		strnskil_scrl_ctrl = data >> 5;
		strnskil_flipscreen = (data >> 3) & 1;
	} };
	
	public static VhUpdatePtr strnskil_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
	
		int offs,chr,col,x,y,px,py,fx,fy,bank,d ;
		UBytePtr SCROLLX = new UBytePtr(memory_region( REGION_USER1 ));
	
		/* draw bg layer */
	
		for (offs=0; offs<(videoram_size[0]/2); offs++)
		{
			int sx,sy;
	
			sx = offs / 32;
			sy = offs % 32;
	
			py = sy*8;
			px = sx*8;
	
			if (strnskil_scrl_ctrl != 7)
			{
				d = SCROLLX.read( strnskil_scrl_ctrl*32 + sy );
				switch (d)
				{
					case 2:
						px = (sx*8 + ~strnskil_xscroll[1]) & 0xff;
						break;
					case 4:
						px = (sx*8 + ~strnskil_xscroll[0]) & 0xff;
						break;
				}
			}
	
			if (strnskil_flipscreen != 0)
			{
				px = 248-px;
				py = 248-py;
			}
	
			col = videoram.read(offs*2);
			fx = strnskil_flipscreen;
			fy = strnskil_flipscreen;
			bank = (col & 0x60) << 3;
			col = ((col & 0x1f)<<0) | ((col & 0x80) >> 2);
	
			drawgfx(bitmap,Machine.gfx[0],
				videoram.read(offs*2+1) + bank,
				col,
				fx,fy,
				px,py,
				Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	
	/* draw sprites */
	
		/* c060 - c0ff */
		for (offs=0x60; offs<0x100; offs +=4)
		{
			chr = spriteram.read(offs+1);
			col = spriteram.read(offs+2);
	
			fx = strnskil_flipscreen;
			fy = strnskil_flipscreen;
	
			x = spriteram.read(offs+3);
			y = spriteram.read(offs+0);
	
			col &= 0x3f ;
	
			if (strnskil_flipscreen==0)
			{
				px = x-2;
				py = 240-y;
			}
			else
			{
				px = 240-x  +0; /* +2 or +0 ? */
				py = y;
			}
	
			px = px & 0xff;
	
			if (px>248)
				px = px-256;
	
			drawgfx(bitmap,Machine.gfx[1],
				chr,
				col,
				fx,fy,
				px,py,
				Machine.visible_area,TRANSPARENCY_COLOR,0);
		}
	
	} };
}
