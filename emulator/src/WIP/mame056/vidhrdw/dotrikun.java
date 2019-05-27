/***************************************************************************

Dottori Kun (Head On's mini game)
(c)1990 SEGA

Driver by Takahiro Nogi (nogi@kt.rim.or.jp) 1999/12/15 -

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
import static mame056.cpu.m6809.m6809H.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.inptport.*;
import static mame056.sound.mixer.*;
import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpuintrfH.*;

public class dotrikun
{
	
	
	
	/*******************************************************************
	
		Palette Setting.
	
	*******************************************************************/
	public static WriteHandlerPtr dotrikun_color_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int r, g, b;
	
		r = ((data & 0x08)!=0 ? 0xff : 0x00);
		g = ((data & 0x10)!=0 ? 0xff : 0x00);
		b = ((data & 0x20)!=0 ? 0xff : 0x00);
		palette_set_color(0, r, g, b);		// BG color
	
		r = ((data & 0x01)!=0 ? 0xff : 0x00);
		g = ((data & 0x02)!=0 ? 0xff : 0x00);
		b = ((data & 0x04)!=0 ? 0xff : 0x00);
		palette_set_color(1, r, g, b);		// DOT color
	} };
	
	
	/*******************************************************************
	
		Draw Pixel.
	
	*******************************************************************/
	public static WriteHandlerPtr dotrikun_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int i;
		int x, y;
		int color;
	
	
		videoram.write(offset, data);
	
		x = 2 * (((offset % 16) * 8));
		y = 2 * ((offset / 16));
	
		if (x >= Machine.visible_area.min_x &&
				x <= Machine.visible_area.max_x &&
				y >= Machine.visible_area.min_y &&
				y <= Machine.visible_area.max_y)
		{
			for (i = 0; i < 8; i++)
			{
				color = Machine.pens[((data >> i) & 0x01)];
	
				/* I think the video hardware doubles pixels, screen would be too small otherwise */
				plot_pixel.handler(Machine.scrbitmap, x + 2*(7 - i),   y,   color);
				plot_pixel.handler(Machine.scrbitmap, x + 2*(7 - i)+1, y,   color);
				plot_pixel.handler(Machine.scrbitmap, x + 2*(7 - i),   y+1, color);
				plot_pixel.handler(Machine.scrbitmap, x + 2*(7 - i)+1, y+1, color);
			}
		}
	} };
	
	
	public static VhUpdatePtr dotrikun_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		if (full_refresh != 0)
		{
			int offs;
	
			/* redraw bitmap */
	
			for (offs = 0; offs < videoram_size[0]; offs++)
				dotrikun_videoram_w.handler(offs,videoram.read(offs));
		}
	} };
}
