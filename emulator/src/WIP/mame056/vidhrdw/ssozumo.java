/***************************************************************************

Syusse Oozumou
(c) 1984 Technos Japan (Licensed by Data East)

Driver by Takahiro Nogi (nogi@kt.rim.or.jp) 1999/10/04

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.libc.cstring.*;
import static mame056.cpuintrfH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.vidhrdw.generic.*;
import static mame056.palette.*;
// refactor
import static arcadeflex036.osdepend.logerror;

public class ssozumo
{
	
	public static UBytePtr ssozumo_videoram2=new UBytePtr(), ssozumo_colorram2=new UBytePtr();
	public static int[] ssozumo_videoram2_size=new int[2];
	public static UBytePtr ssozumo_scroll=new UBytePtr();
	
	//#define TOTAL_COLORS(gfxn)	(Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity)
	//#define COLOR(gfxn,offs)	(colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs])
	
	/**************************************************************************/
	
	public static VhConvertColorPromPtr ssozumo_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int	bit0, bit1, bit2, bit3;
		int	i;
                int     _palette = 0;
	
		for (i = 0 ; i < 64 ; i++)
		{
			bit0 = (color_prom.read(0)>> 0) & 0x01;
			bit1 = (color_prom.read(0)>> 1) & 0x01;
			bit2 = (color_prom.read(0)>> 2) & 0x01;
			bit3 = (color_prom.read(0)>> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
			bit0 = (color_prom.read(0)>> 4) & 0x01;
			bit1 = (color_prom.read(0)>> 5) & 0x01;
			bit2 = (color_prom.read(0)>> 6) & 0x01;
			bit3 = (color_prom.read(0)>> 7) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
			bit0 = (color_prom.read(64)>> 0) & 0x01;
			bit1 = (color_prom.read(64)>> 1) & 0x01;
			bit2 = (color_prom.read(64)>> 2) & 0x01;
			bit3 = (color_prom.read(64)>> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
	
			color_prom.inc();
		}
	} };
	
	
	public static WriteHandlerPtr ssozumo_paletteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int	bit0, bit1, bit2, bit3, val;
		int	r, g, b;
		int	offs2;
	
		paletteram.write(offset,data);
		offs2 = offset & 0x0f;
	
		val = paletteram.read(offs2);
		bit0 = (val >> 0) & 0x01;
		bit1 = (val >> 1) & 0x01;
		bit2 = (val >> 2) & 0x01;
		bit3 = (val >> 3) & 0x01;
		r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
		val = paletteram.read(offs2 | 0x10);
		bit0 = (val >> 0) & 0x01;
		bit1 = (val >> 1) & 0x01;
		bit2 = (val >> 2) & 0x01;
		bit3 = (val >> 3) & 0x01;
		g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
		val = paletteram.read(offs2 | 0x20);
		bit0 = (val >> 0) & 0x01;
		bit1 = (val >> 1) & 0x01;
		bit2 = (val >> 2) & 0x01;
		bit3 = (val >> 3) & 0x01;
		b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
		palette_set_color(offs2 + 64, r, g, b);
	} };
	
	
	public static VhStartPtr ssozumo_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((dirtybuffer = new char[videoram_size[0]]) == null)
		{
			return 1;
		}
		memset(dirtybuffer, 1, videoram_size[0]);
	
		if ((tmpbitmap = bitmap_alloc(Machine.drv.screen_width, 2 * Machine.drv.screen_height)) == null)
		{
                        dirtybuffer = null;
			return 1;
		}
	
		return 0;
	} };
	
	
	public static VhStopPtr ssozumo_vh_stop = new VhStopPtr() { public void handler() 
	{
		dirtybuffer = null;
		bitmap_free(tmpbitmap);
	} };
	
	
	public static VhUpdatePtr ssozumo_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int	offs;
		int	sx, sy;
		int	scrolly;
	
	
		/* Draw the background layer*/
		for (offs = (videoram_size[0] - 1) ; offs >= 0 ; offs--)
		{
			if (dirtybuffer[offs] != 0)
			{
				dirtybuffer[offs] = 0;
	
				sx = (15 - offs / 32);
				sy = (offs % 32);
	
				drawgfx(tmpbitmap, Machine.gfx[1],
						videoram.read(offs)+ ((colorram.read(offs)& 0x08) << 5),
						(colorram.read(offs)& 0x30) >> 4,
						0, sy >= 16 ? 1:0,
						(16 * sx), (16 * sy),
						null, TRANSPARENCY_NONE, 0);
			}
		}
	
		/* Draw the front layer */
		scrolly = -ssozumo_scroll.read();
		copyscrollbitmap(bitmap, tmpbitmap, 0, new int[]{0}, 1, new int[]{scrolly}, Machine.visible_area, TRANSPARENCY_NONE, 0);
	
		for (offs = (ssozumo_videoram2_size[0] - 1) ; offs >= 0 ; offs--)
		{
			sx = (31 - offs / 32);
			sy = (offs % 32);
	
			drawgfx(bitmap,Machine.gfx[0],
					ssozumo_videoram2.read(offs) + 256 * (ssozumo_colorram2.read(offs) & 0x07),
					(ssozumo_colorram2.read(offs) & 0x30) >> 4,
					0, 0,
					(8 * sx), (8 * sy),
					Machine.visible_area, TRANSPARENCY_PEN, 0);
		}
	
		/* Draw the sprites layer */
		for (offs = 0 ; offs < spriteram_size[0] ; offs += 4)
		{
			if ((spriteram.read(offs)& 0x01) != 0)
			{
				drawgfx(bitmap, Machine.gfx[2],
						spriteram.read(offs+1)+ ((spriteram.read(offs)& 0xf0) << 4),
						(spriteram.read(offs)& 0x08) >> 3,
						spriteram.read(offs)& 0x04,spriteram.read(offs)& 0x02,
						239 - spriteram.read(offs+3), (240 - spriteram.read(offs+2)) & 0xff,
						Machine.visible_area, TRANSPARENCY_PEN, 0);
			}
		}
	} };
}
