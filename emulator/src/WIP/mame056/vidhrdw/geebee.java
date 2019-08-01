/****************************************************************************
 *
 * geebee.c
 *
 * video driver
 * juergen buchmueller <pullmoll@t-online.de>, jan 2000
 *
 * TODO:
 * backdrop support for lamps? (player1, player2 and serve)
 * what is the counter output anyway?
 * add overlay colors for Navalone and Kaitei Takara Sagashi
 *
 ****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.machine.geebee.*;

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

public class geebee
{
	
	/* from machine/geebee.c */
	
	//#ifdef MAME_DEBUG
	//char geebee_msg[32+1];
	//int geebee_cnt;
	//#endif
	
	
	static char palette[] =
	{
		0x00,0x00,0x00, /* black */
		0xff,0xff,0xff, /* white */
		0x7f,0x7f,0x7f  /* grey  */
	};
	
	static char geebee_colortable[] =
	{
		 0, 1,
		 0, 2,
		 1, 0,
		 2, 0
	};
	
	static char navalone_colortable[] =
	{
		 0, 1,
		 0, 2,
		 0, 1,
		 0, 2
	};
	
	
	public static artwork_color PINK1 = new artwork_color(0xa0,0x00,0xe0,OVERLAY_DEFAULT_OPACITY);
	public static artwork_color PINK2 = new artwork_color(0xe0,0x00,0xf0,OVERLAY_DEFAULT_OPACITY);
	public static artwork_color ORANGE = new artwork_color(0xff,0xd0,0x00,OVERLAY_DEFAULT_OPACITY);
	public static artwork_color BLUE = new artwork_color(0x00,0x00,0xff,OVERLAY_DEFAULT_OPACITY);
	
	
	
	static artwork_element geebee_overlay[]=
	{
		new artwork_element( new rectangle(  1*8,  4*8-1,    0,32*8-1 ), PINK2  ),
		new artwork_element( new rectangle(  4*8,  5*8-1,    0, 6*8-1 ), PINK1  ),
		new artwork_element( new rectangle(  4*8,  5*8-1, 26*8,32*8-1 ), PINK1  ),
		new artwork_element( new rectangle(  4*8,  5*8-1,  6*8,26*8-1 ), ORANGE ),
		new artwork_element( new rectangle(  5*8, 28*8-1,    0, 3*8-1 ), PINK1  ),
		new artwork_element( new rectangle(  5*8, 28*8-1, 29*8,32*8-1 ), PINK1  ),
		new artwork_element( new rectangle(  5*8, 28*8-1,  3*8, 6*8-1 ), BLUE   ),
		new artwork_element( new rectangle(  5*8, 28*8-1, 26*8,29*8-1 ), BLUE   ),
		new artwork_element( new rectangle(  12*8, 13*8-1, 15*8,17*8-1 ), BLUE   ),
		new artwork_element( new rectangle(  21*8, 23*8-1, 12*8,14*8-1 ), BLUE   ),
		new artwork_element( new rectangle(  21*8, 23*8-1, 18*8,20*8-1 ), BLUE   ),
		new artwork_element( new rectangle(  28*8, 29*8-1,    0,32*8-1 ), PINK2  ),
		new artwork_element( new rectangle(  29*8, 32*8-1,    0,32*8-1 ), PINK1  )
		
	};
	
	public static VhStartPtr geebee_vh_start = new VhStartPtr() { public int handler() 
	{
		if( generic_vh_start.handler()!= 0 )
			return 1;
	
		/* use an overlay only in upright mode */
	
		if( (readinputport(2) & 0x01) == 0 )
		{
			overlay_create(geebee_overlay, 3);
		}
	
		return 0;
	} };
	
	public static VhStartPtr navalone_vh_start = new VhStartPtr() { public int handler() 
	{
		if( generic_vh_start.handler()!= 0 )
			return 1;
	
	    /* overlay? */
	
		return 0;
	} };
	
	public static VhStartPtr sos_vh_start = new VhStartPtr() { public int handler() 
	{
		if( generic_vh_start.handler() != 0 )
			return 1;
	
	    /* overlay? */
	
		return 0;
	} };
	
	public static VhStartPtr kaitei_vh_start = new VhStartPtr() { public int handler() 
	{
		if( generic_vh_start.handler() != 0 )
		return 1;
	
	    /* overlay? */
	
		return 0;
	} };
	
	/* Initialise the palette */
	public static VhConvertColorPromPtr geebee_init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] sys_palette, char[] sys_colortable, UBytePtr color_prom) {
                memcpy(sys_palette, palette, palette.length);
		memcpy(sys_colortable, geebee_colortable, geebee_colortable.length);
            }
        };
	
	/* Initialise the palette */
	public static VhConvertColorPromPtr navalone_init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] sys_palette, char[] sys_colortable, UBytePtr color_prom) {
		memcpy(sys_palette, palette, palette.length);
		memcpy(sys_colortable, navalone_colortable, navalone_colortable.length);
            }
        };
	
	
	public static void geebee_plot(mame_bitmap bitmap, int x, int y)
	{
		rectangle r = Machine.visible_area;
		if (x >= r.min_x && x <= r.max_x && y >= r.min_y && y <= r.max_y)
			plot_pixel.handler(bitmap,x,y,Machine.pens[1]);
	}
	
	public static void geebee_mark_dirty(int x, int y)
	{
		int cx, cy, offs;
		cy = y / 8;
		cx = x / 8;
	    if (geebee_inv != 0)
		{
			offs = (32 - cx) + (31 - cy) * 32;
			dirtybuffer[offs % videoram_size[0]] = 1;
			dirtybuffer[(offs - 1) & (videoram_size[0] - 1)] = 1;
			dirtybuffer[(offs - 32) & (videoram_size[0] - 1)] = 1;
			dirtybuffer[(offs - 32 - 1) & (videoram_size[0] - 1)] = 1;
		}
		else
		{
			offs = (cx - 1) + cy * 32;
			dirtybuffer[offs & (videoram_size[0] - 1)] = 1;
			dirtybuffer[(offs + 1) & (videoram_size[0] - 1)] = 1;
			dirtybuffer[(offs + 32) & (videoram_size[0] - 1)] = 1;
			dirtybuffer[(offs + 32 + 1) & (videoram_size[0] - 1)] = 1;
		}
	}
	
	public static VhUpdatePtr geebee_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
	//#ifdef MAME_DEBUG
	//	if( geebee_cnt > 0 )
	//	{
	//		ui_text(Machine.scrbitmap, geebee_msg, Machine.visible_area.min_y, Machine.visible_area.max_x - 8);
	//		if( --geebee_cnt == 0 )
	//			full_refresh = 1;
	//    }
	//#endif
	
		if (full_refresh != 0)
	        memset(dirtybuffer, 1, videoram_size[0]);
	
		for( offs = 0; offs < videoram_size[0]; offs++ )
		{
			if( dirtybuffer[offs] != 0 )
			{
				int mx,my,sx,sy,code,color;
	
				dirtybuffer[offs] = 0;
	
				mx = offs % 32;
				my = offs / 32;
	
				if (my == 0)
				{
					sx = 8*33;
					sy = 8*mx;
				}
				else if (my == 1)
				{
					sx = 0;
					sy = 8*mx;
				}
				else
				{
					sx = 8*(mx+1);
					sy = 8*my;
				}
	
				if (geebee_inv != 0)
				{
					sx = 33*8 - sx;
					sy = 31*8 - sy;
				}
	
				code = videoram.read(offs);
				color = ((geebee_bgw & 1) << 1) | ((code & 0x80) >> 7);
				drawgfx(bitmap,Machine.gfx[0],
						code,color,
						geebee_inv,geebee_inv,sx,sy,
						Machine.visible_area,TRANSPARENCY_NONE,0);
			}
		}
	
		if( geebee_ball_on != 0)
		{
			int x, y;
	
			geebee_mark_dirty(geebee_ball_h+5,geebee_ball_v-2);
			for( y = 0; y < 4; y++ )
				for( x = 0; x < 4; x++ )
					geebee_plot(bitmap,geebee_ball_h+x+5,geebee_ball_v+y-2);
		}
	} };
}
