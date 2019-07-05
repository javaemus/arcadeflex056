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
import static mame056.artworkH.*;
import static mame056.artwork.*;
import static mame056.drawgfxH.*;
import static mame056.artworkH.*;
import static mame056.driverH.*;
import static mame056.vidhrdw.generic.*;
import static mame056.common.*;
import static mame056.mame.*;
import static mame056.inptport.*;
import static mame056.drawgfx.*;
import static arcadeflex056.video.osd_mark_dirty;

// refactor
import static arcadeflex036.osdepend.logerror;
import static common.ptr.*;
import static mame056.commonH.*;

import static WIP.mame056.machine._8080bw.*;

public class _8080bw
{
	public static abstract interface plot_pixel_proc8080Ptr {
            public abstract void handler(int x, int y, /*UINT32*/ int pen);
        }
	
	static int use_tmpbitmap;
	static int sight_xs;
	static int sight_xc;
	static int sight_xe;
	static int sight_ys;
	static int sight_yc;
	static int sight_ye;
	static int screen_red;
	static int screen_red_enabled;		/* 1 for games that can turn the screen red */
	static int color_map_select;
	static int background_color;
	static int polaris_cloud_pos;
	
	static int artwork_type;
	static artwork_element[] init_artwork;
	
	static WriteHandlerPtr videoram_w_p;
	static VhUpdatePtr vh_screenrefresh_p;
	static plot_pixel_proc8080Ptr plot_pixel_p;
	
	
	//static plot_pixel_proc8080Ptr plot_pixel_8080;
	//static plot_pixel_proc8080Ptr plot_pixel_8080_tmpbitmap;
	
	/* smoothed pure colors, overlays are not so contrasted */
	
	public static artwork_color RED         = new artwork_color(0xff,0x20,0x20,OVERLAY_DEFAULT_OPACITY);
	public static artwork_color GREEN       = new artwork_color(0x20,0xff,0x20,OVERLAY_DEFAULT_OPACITY);
	public static artwork_color YELLOW      = new artwork_color(0xff,0xff,0x20,OVERLAY_DEFAULT_OPACITY);
	public static artwork_color CYAN        = new artwork_color(0x20,0xff,0xff,OVERLAY_DEFAULT_OPACITY);
	
	//#define	END  {{ -1, -1, -1, -1}, 0,0,0,0}
	
	
	static artwork_element invaders_overlay[]=
	{
		new artwork_element( new rectangle(16,  71,   0, 255), GREEN ),
		new artwork_element( new rectangle(0,  15,  16, 133), GREEN ),
		new artwork_element( new rectangle(192, 223,   0, 255), RED   )		
	};
	
	/*static const struct artwork_element invdpt2m_overlay[]= */
	/*{ */
	/*	{{  16,  71,   0, 255}, GREEN  }, */
	/*	{{   0,  15,  16, 133}, GREEN  }, */
	/*	{{  72, 191,   0, 255}, YELLOW }, */
	/*	{{ 192, 223,   0, 255}, RED    }, */
	/*	END */
	/*}; */
	
	static artwork_element invrvnge_overlay[]=
	{
		new artwork_element( new rectangle(0,  71,   0, 255), GREEN ),
		new artwork_element( new rectangle(192, 223,   0, 255), RED   )
	};
	
	static artwork_element invad2ct_overlay[]=
	{
		new artwork_element( new rectangle(0,  47,   0, 255), YELLOW ),
		new artwork_element( new rectangle(25,  70,   0, 255), GREEN  ),
		new artwork_element( new rectangle(48, 139,   0, 255), CYAN   ),
		new artwork_element( new rectangle(117, 185,   0, 255), GREEN  ),
		new artwork_element( new rectangle(163, 231,   0, 255), YELLOW ),
		new artwork_element( new rectangle(209, 255,   0, 255), RED    )
	};
	
	public static final int NO_ARTWORK = 0;
        public static final int SIMPLE_OVERLAY = 1;
        public static final int FILE_OVERLAY = 2;
        public static final int SIMPLE_BACKDROP = 3;
        public static final int FILE_BACKDROP = 4;
	
	public static InitDriverPtr init_8080bw = new InitDriverPtr() { public void handler() 
	{
		videoram_w_p = bw_videoram_w;
		vh_screenrefresh_p = vh_screenrefresh;
		use_tmpbitmap = 0;
		screen_red = 0;
		screen_red_enabled = 0;
		artwork_type = NO_ARTWORK;
		color_map_select = 0;
		flip_screen_set(0);
	} };
	
	public static InitDriverPtr init_invaders = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		init_artwork = invaders_overlay;
		artwork_type = SIMPLE_OVERLAY;
	} };
	
	public static InitDriverPtr init_invaddlx = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		/*init_overlay = invdpt2m_overlay; */
		/*overlay_type = 1; */
	} };
	
	public static InitDriverPtr init_invrvnge = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		init_artwork = invrvnge_overlay;
		artwork_type = SIMPLE_OVERLAY;
	} };
	
	public static InitDriverPtr init_invad2ct = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		init_artwork = invad2ct_overlay;
		artwork_type = SIMPLE_OVERLAY;
	} };
	
	public static InitDriverPtr init_sstrngr2 = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		videoram_w_p = sstrngr2_videoram_w;
		screen_red_enabled = 1;
	} };
	
	public static InitDriverPtr init_schaser = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		videoram_w_p = schaser_videoram_w;
		background_color = 2;	/* blue */
	} };
	
	public static InitDriverPtr init_rollingc = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		videoram_w_p = schaser_videoram_w;
		background_color = 0;	/* black */
	} };
	
	public static InitDriverPtr init_helifire = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		videoram_w_p = helifire_videoram_w;
	} };
	
	public static InitDriverPtr init_polaris = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		videoram_w_p = polaris_videoram_w;
	} };
	
	public static InitDriverPtr init_lupin3 = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		videoram_w_p = lupin3_videoram_w;
		background_color = 0;	/* black */
	} };
	
	public static InitDriverPtr init_invadpt2 = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		videoram_w_p = invadpt2_videoram_w;
		screen_red_enabled = 1;
	} };
	
	public static InitDriverPtr init_seawolf = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		vh_screenrefresh_p = seawolf_vh_screenrefresh;
		use_tmpbitmap = 1;
	} };
	
	public static InitDriverPtr init_blueshrk = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		vh_screenrefresh_p = blueshrk_vh_screenrefresh;
		use_tmpbitmap = 1;
	} };
	
	public static InitDriverPtr init_desertgu = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		vh_screenrefresh_p = desertgu_vh_screenrefresh;
		use_tmpbitmap = 1;
	} };
	
	public static InitDriverPtr init_astinvad = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		videoram_w_p = astinvad_videoram_w;
		screen_red_enabled = 1;
	} };
	
	public static InitDriverPtr init_spaceint = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		videoram_w_p = spaceint_videoram_w;
	} };
	
	public static InitDriverPtr init_spcenctr = new InitDriverPtr() { public void handler() 
	{
		GameDriver driver_spcenctr;
	
		init_8080bw.handler();
		/*TODO*///init_artwork = driver_spcenctr.name;
		artwork_type = FILE_OVERLAY;
	} };
	
	public static InitDriverPtr init_phantom2 = new InitDriverPtr() { public void handler() 
	{
		init_8080bw.handler();
		vh_screenrefresh_p = phantom2_vh_screenrefresh;
		use_tmpbitmap = 1;
	} };
	
	public static InitDriverPtr init_boothill = new InitDriverPtr() { public void handler() 
	{
	/*	extern struct GameDriver driver_boothill; */
	
		init_8080bw.handler();
	/*	init_artwork = driver_boothill.name; */
	/*	artwork_type = FILE_BACKDROP; */
	} };
	
	public static VhStartPtr invaders_vh_start = new VhStartPtr() { public int handler() 
	{
		/* create overlay if one of was specified in init_X */
		if (artwork_type != NO_ARTWORK)
		{
			int start_pen;
	
			start_pen = 2;
	
			switch (artwork_type)
			{
			case SIMPLE_OVERLAY:
				overlay_create(init_artwork, start_pen);
				break;
			case FILE_OVERLAY:
				overlay_load(init_artwork[0].name, start_pen);
				break;
			case SIMPLE_BACKDROP:
				break;
			case FILE_BACKDROP:
				backdrop_load(init_artwork[0].name, start_pen);
				break;
			default:
				logerror("Unknown artwork type.\n");
				break;
			}
		}
	
		if (use_tmpbitmap!=0 && (generic_bitmapped_vh_start.handler()!= 0))
			return 1;
	
		if (use_tmpbitmap != 0)
		{
			plot_pixel_p = plot_pixel_8080_tmpbitmap;
		}
		else
		{
			plot_pixel_p = plot_pixel_8080;
		}
	
		/* make sure that the screen matches the videoram, this fixes invad2ct */
		schedule_full_refresh();
	
		return 0;
	} };
	
	
	public static VhStopPtr invaders_vh_stop = new VhStopPtr() { public void handler() 
	{
		if (use_tmpbitmap != 0)  generic_bitmapped_vh_stop.handler();
	} };
	
	
	public static void invaders_flip_screen_w(int data)
	{
		set_vh_global_attribute(new int[]{color_map_select}, data);
	
		if ((input_port_3_r.handler(0) & 0x01) != 0)
		{
			flip_screen_set(data);
		}
	}
	
	
	public static void invaders_screen_red_w(int data)
	{
		if (screen_red_enabled != 0)
		{
			set_vh_global_attribute(new int[]{screen_red}, data);
		}
	}
	
	static int cloud_speed;
        
	public static InterruptPtr polaris_interrupt = new InterruptPtr() { public int handler() 
	{
		cloud_speed++;
	
		if (cloud_speed >= 8)	/* every 4 frames - this was verified against real machine */
		{
			cloud_speed = 0;
	
			polaris_cloud_pos--;
	
			if (polaris_cloud_pos >= 0xe0)
			{
				polaris_cloud_pos = 0xdf;	/* no delay for invisible region */
			}
	
			schedule_full_refresh();
		}
	
		return invaders_interrupt.handler();
	} };
	
	
	static plot_pixel_proc8080Ptr plot_pixel_8080 = new plot_pixel_proc8080Ptr() {
            public void handler(int x, int y, int col) {
		if (flip_screen() != 0)
		{
			x = 255-x;
			y = 255-y;
		}
	
		plot_pixel.handler(Machine.scrbitmap,x,y,Machine.pens[col]);
            }
        };
	
	static plot_pixel_proc8080Ptr plot_pixel_8080_tmpbitmap = new plot_pixel_proc8080Ptr() {
            public void handler(int x, int y, int col) {
                if (flip_screen() != 0)
		{
			x = 255-x;
			y = 255-y;
		}
	
		plot_pixel.handler(tmpbitmap,x,y,Machine.pens[col]);
            }
        };
        
	public static void plot_byte(int x, int y, int data, int fore_color, int back_color)
	{
		int i;
	
		for (i = 0; i < 8; i++)
		{
			plot_pixel_p.handler(x, y, ((data & 0x01) != 0) ? fore_color : back_color);
	
			x++;
			data >>= 1;
		}
	}
	
	
	public static WriteHandlerPtr invaders_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		videoram_w_p.handler(offset, data);
	} };
	
	
	public static WriteHandlerPtr bw_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int x,y;
	
		videoram.write(offset,data);
	
		y = offset / 32;
		x = 8 * (offset % 32);
	
		plot_byte(x, y, data, 1, 0);
	} };
	
	public static WriteHandlerPtr schaser_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int x,y,col;
	
		videoram.write(offset,data);
	
		y = offset / 32;
		x = 8 * (offset % 32);
	
		col = colorram.read(offset & 0x1f1f)& 0x07;
	
		plot_byte(x, y, data, col, background_color);
	} };
	
	public static WriteHandlerPtr lupin3_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int x,y,col;
	
		videoram.write(offset,data);
	
		y = offset / 32;
		x = 8 * (offset % 32);
	
		col = ~colorram.read(offset & 0x1f1f)& 0x07;
	
		plot_byte(x, y, data, col, background_color);
	} };
	
	public static WriteHandlerPtr polaris_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int x,i,col,back_color,fore_color;
                UBytePtr color_map;
		int y, cloud_y;
	
		videoram.write(offset,data);
	
		y = offset / 32;
		x = 8 * (offset % 32);
	
		/* for the background color, bit 0 of the map PROM is connected to green gun.
		   red is 0 and blue is 1, giving cyan and blue for the background.  This
		   is different from what the schematics shows, but it's supported
		   by screenshots. */
	
		color_map = new UBytePtr(memory_region(REGION_PROMS), (((y+32)/8)*32) + (x/8));
		back_color = ((color_map.read() & 1)!=0) ? 6 : 2;
		fore_color = ~colorram.read(offset & 0x1f1f)& 0x07;
	
		/* bit 3 is connected to the cloud enable. bits 1 and 2 are marked 'not use' (sic)
		   on the schematics */
	
		if (y < polaris_cloud_pos)
		{
			cloud_y = y - polaris_cloud_pos - 0x20;
		}
		else
		{
			cloud_y = y - polaris_cloud_pos;
		}
	
		if (((color_map.read() & 0x08)!=0) || ((cloud_y > 64)))
		{
			plot_byte(x, y, data, fore_color, back_color);
		}
		else
		{
			/* cloud appears in this part of the screen */
			for (i = 0; i < 8; i++)
			{
				if ((data & 0x01) != 0)
				{
					col = fore_color;
				}
				else
				{
					int offs,bit;
	
					col = back_color;
	
					bit = 1 << (~x & 0x03);
					offs = ((x >> 2) & 0x03) | ((~cloud_y & 0x3f) << 2);
	
					col = ((new UBytePtr(memory_region(REGION_USER1),offs).read() & bit) != 0) ? 7 : back_color;
				}
	
				plot_pixel_p.handler(x, y, col);
	
				x++;
				data >>= 1;
			}
		}
	} };
	
	public static WriteHandlerPtr helifire_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int x,y,back_color,foreground_color;
	
		videoram.write(offset,data);
	
		y = offset / 32;
		x = 8 * (offset % 32);
	
		back_color = 0;
		foreground_color = colorram.read(offset)& 0x07;
	
		if (x < 0x78)
		{
			back_color = 4;	/* blue */
		}
	
		plot_byte(x, y, data, foreground_color, back_color);
	} };
	
	
	public static WriteHandlerPtr schaser_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int i;
	
	
		offset &= 0x1f1f;
	
		colorram.write(offset,data);
	
		/* redraw region with (possibly) changed color */
		for (i = 0; i < 8; i++, offset += 0x20)
		{
			videoram_w_p.handler(offset, videoram.read(offset));
		}
	} };
	
	public static ReadHandlerPtr schaser_colorram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return colorram.read(offset & 0x1f1f);
	} };
	
	
	public static WriteHandlerPtr helifire_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		colorram.write(offset,data);
	
		/* redraw region with (possibly) changed color */
		videoram_w_p.handler(offset, videoram.read(offset));
	} };
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr invaders_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		vh_screenrefresh_p.handler(bitmap, full_refresh);
	} };
	
	
	public static VhUpdatePtr vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		if (full_refresh != 0)
		{
			int offs;
	
			for (offs = 0;offs < videoram_size[0];offs++)
				videoram_w_p.handler(offs, videoram.read(offs));
		}
	
	
		if (use_tmpbitmap != 0)
		{
			copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
			osd_mark_dirty( sight_xc, sight_ys, sight_xc, sight_ye );
			osd_mark_dirty( sight_xs, sight_yc, sight_xe, sight_yc );
		}
	} };
	
	
	static void draw_sight(mame_bitmap bitmap,int x_center, int y_center)
	{
		int x,y;
	
		sight_xc = x_center;
		if( sight_xc < 2 )
		{
			sight_xc = 2;
		}
		else if( sight_xc > 253 )
		{
			sight_xc = 253;
		}
	
		sight_yc = y_center;
		if( sight_yc < 2 )
		{
			sight_yc = 2;
		}
		else if( sight_yc > 221 )
		{
			sight_yc = 221;
		}
	
		sight_xs = sight_xc - 20;
		if( sight_xs < 0 )
		{
			sight_xs = 0;
		}
		sight_xe = sight_xc + 20;
		if( sight_xe > 255 )
		{
			sight_xe = 255;
		}
	
		sight_ys = sight_yc - 20;
		if( sight_ys < 0 )
		{
			sight_ys = 0;
		}
		sight_ye = sight_yc + 20;
		if( sight_ye > 223 )
		{
			sight_ye = 223;
		}
	
		x = sight_xc;
		y = sight_yc;
		if (flip_screen() != 0)
		{
			x = 255-x;
			y = 255-y;
		}
	
		draw_crosshair(bitmap,x,y,Machine.visible_area);
	}
	
	
	public static VhUpdatePtr seawolf_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/* update the bitmap (and erase old cross) */
		vh_screenrefresh.handler(bitmap, full_refresh);
	
	    draw_sight(bitmap,((input_port_0_r.handler(0) & 0x1f) * 8) + 4, 31);
	} };
	
	public static VhUpdatePtr blueshrk_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/* update the bitmap (and erase old cross) */
		vh_screenrefresh.handler(bitmap, full_refresh);
	
	    draw_sight(bitmap,((input_port_0_r.handler(0) & 0x7f) * 2) - 12, 31);
	} };
	
	public static VhUpdatePtr desertgu_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/* update the bitmap (and erase old cross) */
		vh_screenrefresh.handler(bitmap, full_refresh);
	
		draw_sight(bitmap,((input_port_0_r.handler(0) & 0x7f) * 2) - 30,
				   ((input_port_2_r.handler(0) & 0x7f) * 2) - 30);
	} };
	
	public static VhUpdatePtr phantom2_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		UBytePtr clouds;
		int x, y;
	
	
		/* update the bitmap */
		vh_screenrefresh.handler(bitmap, full_refresh);
	
	
		/* draw the clouds */
		clouds = new UBytePtr(memory_region(REGION_PROMS));
	
		for (y = 0; y < 128; y++)
		{
			UBytePtr offs = new UBytePtr(memory_region(REGION_PROMS), y * 0x10);
	
			for (x = 0; x < 128; x++)
			{
				if ((offs.read(x >> 3) & (1 << (x & 0x07))) != 0)
				{
					plot_pixel_8080.handler(x*2,   y*2,   1);
					plot_pixel_8080.handler(x*2+1, y*2,   1);
					plot_pixel_8080.handler(x*2,   y*2+1, 1);
					plot_pixel_8080.handler(x*2+1, y*2+1, 1);
				}
			}
		}
	} };
	
	
	public static VhConvertColorPromPtr invadpt2_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
                int _palette = 0;
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			/* this bit arrangment is a little unusual but are confirmed by screen shots */
	
			palette[_palette++] = (char) (0xff * ((i >> 0) & 1));
			palette[_palette++] = (char) (0xff * ((i >> 2) & 1));
			palette[_palette++] = (char) (0xff * ((i >> 1) & 1));
		}
	} };
	
	public static VhConvertColorPromPtr helifire_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
                int _palette = 0;
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			palette[_palette++] = (char) (0xff * ((i >> 0) & 1));
			palette[_palette++] = (char) (0xff * ((i >> 1) & 1));
			palette[_palette++] = (char) (0xff * ((i >> 2) & 1));
		}
	} };
	
	
	public static WriteHandlerPtr invadpt2_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int x,y,col;
	
		videoram.write(offset,data);
	
		y = offset / 32;
		x = 8 * (offset % 32);
	
		/* 32 x 32 colormap */
		if (screen_red == 0)
		{
			int colbase;
	
			colbase = (color_map_select != 0) ? 0x400 : 0;
			col = new UBytePtr(memory_region(REGION_PROMS)).read(colbase + (((y+32)/8)*32) + (x/8)) & 7;
		}
		else
			col = 1;	/* red */
	
		plot_byte(x, y, data, col, 0);
	} };
	
	public static WriteHandlerPtr sstrngr2_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int x,y,col;
	
		videoram.write(offset,data);
	
		y = offset / 32;
		x = 8 * (offset % 32);
	
		/* 16 x 32 colormap */
		if (screen_red == 0)
		{
			int colbase;
	
			colbase = color_map_select!=0 ? 0 : 0x0200;
			col = new UBytePtr(memory_region(REGION_PROMS)).read(colbase + ((y/16+2) & 0x0f)*32 + (x/8)) & 0x0f;
		}
		else
			col = 1;	/* red */
	
		if (color_map_select != 0)
		{
			x = 240 - x;
			y = 223 - y;
		}
	
		plot_byte(x, y, data, col, 0);
	} };
	
	public static WriteHandlerPtr astinvad_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int x,y,col;
	
		videoram.write(offset,data);
	
		y = offset / 32;
		x = 8 * (offset % 32);
	
		if (screen_red == 0)
		{
			if (flip_screen() != 0)
				col = new UBytePtr(memory_region(REGION_PROMS)).read(((y+32)/8)*32 + (x/8)) >> 4;
			else
				col = new UBytePtr(memory_region(REGION_PROMS)).read((31-y/8)*32 + (31-x/8)) & 0x0f;
		}
		else
			col = 1; /* red */
	
		plot_byte(x, y, data, col, 0);
	} };
	
	public static WriteHandlerPtr spaceint_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int x,y,col;
		int i;
	
		videoram.write(offset,data);
	
		y = 8 * (offset / 256);
		x = offset % 256;
	
		/* this is wrong */
		col = new UBytePtr(memory_region(REGION_PROMS)).read((y/16)+16*((x+16)/32));
	
		for (i = 0; i < 8; i++)
		{
			plot_pixel_p.handler(x, y, (data & 0x01)!=0 ? col : 0);
	
			y++;
			data >>= 1;
		}
	} };
}
