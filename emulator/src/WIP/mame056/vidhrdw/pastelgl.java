/******************************************************************************

	Video Hardware for Nichibutsu Mahjong series.

	Driver by Takahiro Nogi 2000/06/07 -

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

public class pastelgl
{
	
	
	static int pastelgl_drawx, pastelgl_drawy;
	static int pastelgl_sizex, pastelgl_sizey;
	static int pastelgl_radrx, pastelgl_radry;
	static int pastelgl_gfxrom;
	static int pastelgl_dispflag;
	static int pastelgl_gfxflag;
	static int pastelgl_flipscreen;
	static int pastelgl_flipx, pastelgl_flipy;
	static int pastelgl_screen_refresh;
	static int pastelgl_palbank;
	
	static mame_bitmap pastelgl_tmpbitmap;
	public static UBytePtr pastelgl_videoram = new UBytePtr();
	public static UBytePtr pastelgl_paltbl = new UBytePtr();
	
	
	
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static VhConvertColorPromPtr pastelgl_init_palette = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
                int _palette = 0;
	
		for (i = 0; i < Machine.drv.total_colors; i++)
		{
			int bit0, bit1, bit2, bit3;
	
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
			bit0 = (color_prom.read(Machine.drv.total_colors)>> 0) & 0x01;
			bit1 = (color_prom.read(Machine.drv.total_colors)>> 1) & 0x01;
			bit2 = (color_prom.read(Machine.drv.total_colors)>> 2) & 0x01;
			bit3 = (color_prom.read(Machine.drv.total_colors)>> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
	
			color_prom.inc();
		}
	} };
	
	public static void pastelgl_paltbl_w(int offset, int data)
	{
		pastelgl_paltbl.write(offset, data);
	}
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static void pastelgl_radrx_w(int data)
	{
		pastelgl_radrx = data;
	}
	
	public static void pastelgl_radry_w(int data)
	{
		pastelgl_radry = data;
	}
	
	public static void pastelgl_sizex_w(int data)
	{
		pastelgl_sizex = data;
	}
	
	public static void pastelgl_sizey_w(int data)
	{
		pastelgl_sizey = data;
	
		pastelgl_gfxdraw_w();
	}
	
	public static void pastelgl_drawx_w(int data)
	{
		pastelgl_drawx = (data ^ 0xff);
	}
	
	public static void pastelgl_drawy_w(int data)
	{
		pastelgl_drawy = (data ^ 0xff);
	}
        
        static int pastelgl_flipscreen_old = -1;
	
	public static void pastelgl_dispflag_w(int data)
	{
		
	
		pastelgl_gfxflag = data;
	
		pastelgl_flipx = (data & 0x01)!=0 ? 1 : 0;
		pastelgl_flipy = (data & 0x02)!=0 ? 1 : 0;
		pastelgl_flipscreen = (data & 0x04)!=0 ? 0 : 1;
		pastelgl_dispflag = (data & 0x08)!=0 ? 0 : 1;		// unused ?
	//	if (data & 0xf0) usrintf_showmessage("Unknown GFXFLAG!! (%02X)", (data & 0xf0));
	
		if (nb1413m3_type == NB1413M3_PASTELGL)
		{
			pastelgl_flipscreen ^= 1;
		}
	
		if (pastelgl_flipscreen != pastelgl_flipscreen_old)
		{
			pastelgl_vramflip();
			pastelgl_screen_refresh = 1;
			pastelgl_flipscreen_old = pastelgl_flipscreen;
		}
	}
	
	public static void pastelgl_romsel_w(int data)
	{
		pastelgl_gfxrom = ((data & 0xc0) >> 6);
		pastelgl_palbank = ((data & 0x10) >> 4);
	
		if ((pastelgl_gfxrom << 16) > (memory_region_length(REGION_GFX1) - 1))
		{
	//#ifdef MAME_DEBUG
	//		usrintf_showmessage("GFXROM BANK OVER!!");
	//#endif
			pastelgl_gfxrom = 0;
		}
	}
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static void pastelgl_vramflip()
	{
		int x, y;
		int color1, color2;
	
		for (y = 0; y < Machine.drv.screen_height; y++)
		{
			for (x = 0; x < Machine.drv.screen_width; x++)
			{
				color1 = pastelgl_videoram.read((y * Machine.drv.screen_width) + x);
				color2 = pastelgl_videoram.read(((y ^ 0xff) * Machine.drv.screen_width) + (x ^ 0xff));
				pastelgl_videoram.write((y * Machine.drv.screen_width) + x, color2);
				pastelgl_videoram.write(((y ^ 0xff) * Machine.drv.screen_width) + (x ^ 0xff), color1);
			}
		}
	}
	
	public static void pastelgl_gfxdraw_w()
	{
		UBytePtr GFX = new UBytePtr(memory_region(REGION_GFX1));
	
		int x, y;
		int dx, dy;
		int startx, starty;
		int sizex, sizey;
		int skipx, skipy;
		int ctrx, ctry;
		int readflag;
		int tflag;
		int gfxaddr;
		int color;
		int drawcolor;
	
		if (pastelgl_flipx != 0)
		{
			pastelgl_drawx -= (pastelgl_sizex << 1);
			startx = pastelgl_sizex;
			sizex = ((pastelgl_sizex ^ 0xff) + 1);
			skipx = -1;
		}
		else
		{
			pastelgl_drawx = (pastelgl_drawx - pastelgl_sizex);
			startx = 0;
			sizex = (pastelgl_sizex + 1);
			skipx = 1;
		}
	
		if (pastelgl_flipy != 0)
		{
			pastelgl_drawy -= (pastelgl_sizey << 1);
			starty = pastelgl_sizey;
			sizey = ((pastelgl_sizey ^ 0xff) + 1);
			skipy = -1;
		}
		else
		{
			pastelgl_drawy = (pastelgl_drawy - pastelgl_sizey);
			starty = 0;
			sizey = (pastelgl_sizey + 1);
			skipy = 1;
		}
	
		gfxaddr = ((pastelgl_gfxrom << 16) + (pastelgl_radry << 8) + pastelgl_radrx);
	
		readflag = 0;
	
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
	
				color = GFX.read(gfxaddr);
	
				if (pastelgl_flipscreen != 0)
				{
					dx = (((pastelgl_drawx + x) ^ 0xff) & 0xff);
					dy = (((pastelgl_drawy + y) ^ 0xff) & 0xff);
				}
				else
				{
					dx = ((pastelgl_drawx + x) & 0xff);
					dy = ((pastelgl_drawy + y) & 0xff);
				}
	
				if (readflag == 0)
				{
					// 1st, 3rd, 5th, ... read
					color = (color & 0x0f);
				}
				else
				{
					// 2nd, 4th, 6th, ... read
					color = (color & 0xf0) >> 4;
					gfxaddr++;
				}
	
				readflag ^= 1;
	
				tflag = 1;
	
				if ((pastelgl_paltbl.read(color) & 0xf0) != 0)
				{
					if (color == 0) tflag = 0;
					drawcolor = ((pastelgl_palbank * 0x10) + color);
				}
				else
				{
					drawcolor = ((pastelgl_palbank * 0x10) + pastelgl_paltbl.read(color));
				}
	
				if (tflag != 0)
				{
					pastelgl_videoram.write((dy * Machine.drv.screen_width) + dx, drawcolor);
					plot_pixel.handler(pastelgl_tmpbitmap, dx, dy, Machine.pens[drawcolor]);
				}
	
				nb1413m3_busyctr++;
			}
		}
	
		nb1413m3_busyflag = (nb1413m3_busyctr > 7000) ? 0 : 1;
	}
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static VhStartPtr pastelgl_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((pastelgl_tmpbitmap = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height)) == null) return 1;
		if ((pastelgl_videoram = new UBytePtr(Machine.drv.screen_width * Machine.drv.screen_height)) == null) return 1;
		if ((pastelgl_paltbl = new UBytePtr(0x10)) == null) return 1;
		memset(pastelgl_videoram, 0x00, (Machine.drv.screen_width * Machine.drv.screen_height));
		return 0;
	} };
	
	public static VhStopPtr pastelgl_vh_stop = new VhStopPtr() { public void handler() 
	{
		pastelgl_paltbl = null;
		pastelgl_videoram = null;
		bitmap_free(pastelgl_tmpbitmap);
		pastelgl_videoram = null;
		pastelgl_tmpbitmap = null;
	} };
	
	/******************************************************************************
	
	
	******************************************************************************/
	public static VhUpdatePtr pastelgl_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int x, y;
		int color;
	
		if (full_refresh!=0 || pastelgl_screen_refresh!=0)
		{
			pastelgl_screen_refresh = 0;
			for (y = 0; y < Machine.drv.screen_height; y++)
			{
				for (x = 0; x < Machine.drv.screen_width; x++)
				{
					color = pastelgl_videoram.read((y * Machine.drv.screen_width) + x);
					plot_pixel.handler(pastelgl_tmpbitmap, x, y, Machine.pens[color]);
				}
			}
		}
	
		if (pastelgl_dispflag != 0)
		{
			copybitmap(bitmap, pastelgl_tmpbitmap, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_NONE, 0);
		}
		else
		{
			fillbitmap(bitmap, Machine.pens[0x00], null);
		}
	} };
}
