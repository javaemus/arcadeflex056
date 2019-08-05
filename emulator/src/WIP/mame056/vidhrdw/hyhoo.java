/******************************************************************************

	Video Hardware for Nichibutsu Mahjong series.

	Driver by Takahiro Nogi <nogi@kt.rim.or.jp> 2000/01/28 -

******************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.machine.nb1413m3.*;
import static WIP.mame056.machine.nb1413m3H.*;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.libc.cstring.*;
import static mame056.cpuintrfH.*;
import static mame056.cpuintrf.*;
import static mame056.inptport.*;
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

import static mame056.artwork.*;
import static mame056.artworkH.*;

public class hyhoo
{
	
	
	static int hyhoo_scrolly;
	static int hyhoo_drawx, hyhoo_drawy;
	static int hyhoo_sizex, hyhoo_sizey;
	static int hyhoo_radrx, hyhoo_radry;
	static int hyhoo_gfxrom;
	static int hyhoo_gfxflag1;
	static int hyhoo_gfxflag2;
	static int hyhoo_dispflag;
	static int hyhoo_flipscreen;
	static int hyhoo_flipx, hyhoo_flipy;
	static int hyhoo_screen_refresh;
	
	static mame_bitmap hyhoo_tmpbitmap;
	static UBytePtr hyhoo_videoram = new UBytePtr();
	static UBytePtr hyhoo_videoworkram = new UBytePtr();
	static char[] hyhoo_palette;
	
	
	
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static VhConvertColorPromPtr hyhoo_init_palette = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
                int _palette = 0;
	
		/* initialize 655 RGB lookup */
		for (i = 0; i < 65536; i++)
		{
			int r, g, b;
	
			// bbbbbggg_ggrrrrrr
			r = ((i >>  0) & 0x3f);
			g = ((i >>  6) & 0x1f);
			b = ((i >> 11) & 0x1f);
	
			palette[_palette++] = (char) ((r << 2) | (r >> 3));
			palette[_palette++] = (char) ((g << 3) | (g >> 2));
			palette[_palette++] = (char) ((b << 3) | (b >> 2));
		}
	} };
	
	public static WriteHandlerPtr hyhoo_palette_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		hyhoo_palette[offset & 0x0f] = (char) (data ^ 0xff);
	} };
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static void hyhoo_radrx_w(int data)
	{
		hyhoo_radrx = data;
	}
	
	public static void hyhoo_radry_w(int data)
	{
		hyhoo_radry = data;
	}
	
	public static void hyhoo_sizex_w(int data)
	{
		hyhoo_sizex = data;
	}
	
	public static void hyhoo_sizey_w(int data)
	{
		hyhoo_sizey = data;
	
		hyhoo_gfxdraw();
	}
        
        static int hyhoo_flipscreen_old = -1;
	
	public static void hyhoo_gfxflag1_w(int data)
	{
		
	
		hyhoo_gfxflag1 = data;
	
		hyhoo_flipx = (data & 0x01)!=0 ? 1 : 0;
		hyhoo_flipy = (data & 0x02)!=0 ? 1 : 0;
		hyhoo_flipscreen = (data & 0x04)!=0 ? 0 : 1;
		hyhoo_dispflag = (data & 0x08)!=0 ? 0 : 1;
	
		if ((nb1413m3_type == NB1413M3_HYHOO) ||
		    (nb1413m3_type == NB1413M3_HYHOO2))
		{
			hyhoo_flipscreen ^= 1;
		}
	
		if (hyhoo_flipscreen != hyhoo_flipscreen_old)
		{
			hyhoo_vramflip();
			hyhoo_screen_refresh = 1;
			hyhoo_flipscreen_old = hyhoo_flipscreen;
		}
	}
	
	public static void hyhoo_gfxflag2_w(int data)
	{
		hyhoo_gfxflag2 = data;
	}
	
	public static void hyhoo_drawx_w(int data)
	{
		hyhoo_drawx = (data ^ 0xff);
	}
	
	public static void hyhoo_drawy_w(int data)
	{
		hyhoo_drawy = (data ^ 0xff);
	
		if (hyhoo_flipscreen != 0) hyhoo_scrolly = -2;
		else hyhoo_scrolly = 0;
	}
	
	public static void hyhoo_romsel_w(int data)
	{
		hyhoo_gfxrom = (((data & 0xc0) >> 4) + (data & 0x03));
	
		if ((hyhoo_gfxrom << 17) > (memory_region_length(REGION_GFX1) - 1))
		{
	//#ifdef MAME_DEBUG
	//		usrintf_showmessage("GFXROM BANK OVER!!");
	//#endif
			hyhoo_gfxrom = 0;
		}
	}
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static void hyhoo_vramflip()
	{
		int x, y;
		int color1, color2;
	
		for (y = 0; y < (Machine.drv.screen_height / 2); y++)
		{
			for (x = 0; x < Machine.drv.screen_width; x++)
			{
				color1 = hyhoo_videoram.read((y * Machine.drv.screen_width) + x);
				color2 = hyhoo_videoram.read(((y ^ 0xff) * Machine.drv.screen_width) + (x ^ 0x1ff));
				hyhoo_videoram.write((y * Machine.drv.screen_width) + x, color2);
				hyhoo_videoram.write(((y ^ 0xff) * Machine.drv.screen_width) + (x ^ 0x1ff), color1);
	
				color1 = hyhoo_videoworkram.read((y * Machine.drv.screen_width) + x);
				color2 = hyhoo_videoworkram.read(((y ^ 0xff) * Machine.drv.screen_width) + (x ^ 0x1ff));
				hyhoo_videoworkram.write((y * Machine.drv.screen_width) + x, color2);
				hyhoo_videoworkram.write(((y ^ 0xff) * Machine.drv.screen_width) + (x ^ 0x1ff), color1);
			}
		}
	}
	
	public static void hyhoo_gfxdraw()
	{
		UBytePtr GFX = new UBytePtr(memory_region(REGION_GFX1));
	
		int x, y;
		int dx1, dx2, dy;
		int startx, starty;
		int sizex, sizey;
		int skipx, skipy;
		int ctrx, ctry;
		int tflag1, tflag2;
		int gfxaddr;
		int r, g, b;
		int color, color1, color2;
		int drawcolor1, drawcolor2;
	
		hyhoo_gfxrom |= ((nb1413m3_sndrombank1 & 0x02) << 3);
	
		if (hyhoo_flipx != 0)
		{
			hyhoo_drawx -= (hyhoo_sizex << 1);
			startx = hyhoo_sizex;
			sizex = ((hyhoo_sizex ^ 0xff) + 1);
			skipx = -1;
		}
		else
		{
			hyhoo_drawx = (hyhoo_drawx - hyhoo_sizex);
			startx = 0;
			sizex = (hyhoo_sizex + 1);
			skipx = 1;
		}
	
		if (hyhoo_flipy != 0)
		{
			hyhoo_drawy -= ((hyhoo_sizey << 1) + 1);
			starty = hyhoo_sizey;
			sizey = ((hyhoo_sizey ^ 0xff) + 1);
			skipy = -1;
		}
		else
		{
			hyhoo_drawy = (hyhoo_drawy - hyhoo_sizey - 1);
			starty = 0;
			sizey = (hyhoo_sizey + 1);
			skipy = 1;
		}
	
		gfxaddr = ((hyhoo_gfxrom << 17) + (hyhoo_radry << 9) + (hyhoo_radrx << 1));
	
		for (y = starty, ctry = sizey; ctry > 0; y += skipy, ctry--)
		{
			for (x = startx, ctrx = sizex; ctrx > 0; x += skipx, ctrx--)
			{
				if ((gfxaddr > (memory_region_length(REGION_GFX1) - 1)))
				{
	//#ifdef MAME_DEBUG
	//				usrintf_showmessage("GFXROM ADDRESS OVER!!");
	//#endif
					gfxaddr = 0;
				}
	
				color = GFX.read(gfxaddr++);
	
				if (hyhoo_flipscreen != 0)
				{
					dx1 = (((((hyhoo_drawx + x) * 2) + 0) ^ 0x1ff) & 0x1ff);
					dx2 = (((((hyhoo_drawx + x) * 2) + 1) ^ 0x1ff) & 0x1ff);
					dy = (((hyhoo_drawy + y) ^ 0xff) & 0xff);
				}
				else
				{
					dx1 = ((((hyhoo_drawx + x) * 2) + 0) & 0x1ff);
					dx2 = ((((hyhoo_drawx + x) * 2) + 1) & 0x1ff);
					dy = ((hyhoo_drawy + y) & 0xff);
				}
	
				if ((hyhoo_gfxflag2 & 0x04) != 0)
				{
					// 65536 colors mode
	
					if ((hyhoo_gfxflag2 & 0x20) != 0)
					{
						// 65536 colors (lower)
	
						// src xxxxxxxx_bbbggrrr
						// dst xxbbbxxx_ggxxxrrr
						r = (((color & 0x07) >> 0) & 0x07);
						g = (((color & 0x18) >> 3) & 0x03);
						b = (((color & 0xe0) >> 5) & 0x07);
						drawcolor1 = drawcolor2 = ((b << (11 + 0)) | (g << (6 + 0)) | (r << (0 + 0)));
	
						drawcolor1 |= hyhoo_videoworkram.read((dy * Machine.drv.screen_width) + dx1);
						drawcolor2 |= hyhoo_videoworkram.read((dy * Machine.drv.screen_width) + dx2);
	
						tflag1 = (drawcolor1 != 0xffff) ? 1 : 0;
						tflag2 = (drawcolor2 != 0xffff) ? 1 : 0;
					}
					else
					{
						// 65536 colors (higher)
	
						tflag1 = tflag2 = 1;	// dummy
	
						// src xxxxxxxx_bbgggrrr
						// dst bbxxxggg_xxrrrxxx
						r = (((color & 0x07) >> 0) & 0x07);
						g = (((color & 0x38) >> 3) & 0x07);
						b = (((color & 0xc0) >> 6) & 0x03);
						drawcolor1 = drawcolor2 = ((b << (11 + 3)) | (g << (6 + 2)) | (r << (0 + 3)));
	
						hyhoo_videoworkram.write((dy * Machine.drv.screen_width) + dx1, drawcolor1);
						hyhoo_videoworkram.write((dy * Machine.drv.screen_width) + dx2, drawcolor2);
	
						continue;
					}
				}
				else
				{
					// Palettized picture mode
	
					if (hyhoo_flipx != 0)
					{
						// flip
						color1 = (color & 0xf0) >> 4;
						color2 = (color & 0x0f) >> 0;
					}
					else
					{
						// normal
						color1 = (color & 0x0f) >> 0;
						color2 = (color & 0xf0) >> 4;
					}
	
					tflag1 = (hyhoo_palette[color1] != 0xff) ? 1 : 0;
					tflag2 = (hyhoo_palette[color2] != 0xff) ? 1 : 0;
	
					// src xxxxxxxx_bbgggrrr
					// dst bbxxxggg_xxrrrxxx
	
					r = (hyhoo_palette[color1] & 0x07) >> 0;
					g = (hyhoo_palette[color1] & 0x38) >> 3;
					b = (hyhoo_palette[color1] & 0xc0) >> 6;
	
					drawcolor1 = ((b << (11 + 3)) | (g << (6 + 2)) | (r << (0 + 3)));
	
					// src xxxxxxxx_bbgggrrr
					// dst bbxxxggg_xxrrrxxx
	
					r = (hyhoo_palette[color2] & 0x07) >> 0;
					g = (hyhoo_palette[color2] & 0x38) >> 3;
					b = (hyhoo_palette[color2] & 0xc0) >> 6;
	
					drawcolor2 = ((b << (11 + 3)) | (g << (6 + 2)) | (r << (0 + 3)));
				}
	
				nb1413m3_busyctr++;
	
				if (tflag1 != 0)
				{
					hyhoo_videoram.write((dy * Machine.drv.screen_width) + dx1, drawcolor1);
					plot_pixel.handler(hyhoo_tmpbitmap, dx1, dy, Machine.pens[drawcolor1]);
				}
				if (tflag2 != 0)
				{
					hyhoo_videoram.write((dy * Machine.drv.screen_width) + dx2, drawcolor2);
					plot_pixel.handler(hyhoo_tmpbitmap, dx2, dy, Machine.pens[drawcolor2]);
				}
	
				nb1413m3_busyctr++;
			}
		}
	
		nb1413m3_busyflag = (nb1413m3_busyctr > 10000) ? 0 : 1;
	}
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static VhStartPtr hyhoo_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((hyhoo_tmpbitmap = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height)) == null) return 1;
		if ((hyhoo_videoram = new UBytePtr(Machine.drv.screen_width * Machine.drv.screen_height)) == null) return 1;
		if ((hyhoo_videoworkram = new UBytePtr(Machine.drv.screen_width * Machine.drv.screen_height)) == null) return 1;
		if ((hyhoo_palette = new char[0x10]) == null) return 1;
		memset(hyhoo_videoram, 0x0000, (Machine.drv.screen_width * Machine.drv.screen_height));
		return 0;
	} };
	
	public static VhStopPtr hyhoo_vh_stop = new VhStopPtr() { public void handler() 
	{
		hyhoo_palette = null;
		hyhoo_videoworkram = null;
		hyhoo_videoram = null;
		bitmap_free(hyhoo_tmpbitmap);
		
		hyhoo_tmpbitmap = null;
	} };
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static VhUpdatePtr hyhoo_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int x, y;
		int color;
	
		if (full_refresh!=0 || hyhoo_screen_refresh!=0)
		{
			hyhoo_screen_refresh = 0;
			for (y = 0; y < Machine.drv.screen_height; y++)
			{
				for (x = 0; x < Machine.drv.screen_width; x++)
				{
					color = hyhoo_videoram.read((y * Machine.drv.screen_width) + x);
					plot_pixel.handler(hyhoo_tmpbitmap, x, y, Machine.pens[color]);
				}
			}
		}
	
		if (hyhoo_dispflag != 0)
		{
			copyscrollbitmap(bitmap, hyhoo_tmpbitmap, 0, new int[]{0}, 1, new int[]{hyhoo_scrolly}, Machine.visible_area, TRANSPARENCY_NONE, 0);
		}
		else
		{
			fillbitmap(bitmap, Machine.pens[0x0000], null);
		}
	} };
}
