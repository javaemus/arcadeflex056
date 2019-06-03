/***************************************************************************
  Psychic 5

  Functions to emulate the video hardware of the machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;
import static mame056.usrintrf.*;
import static mame056.memoryH.*;
import static mame056.memory.*;

import static common.ptr.*;
import common.subArrays.IntArray;
import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.inptport.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.driverH.*;
import static mame056.palette.*;

public class psychic5
{
	
	
	public static final int	BG_SCROLLX_LSB          = 0x308;
	public static final int	BG_SCROLLX_MSB		= 0x309;
	public static final int	BG_SCROLLY_LSB		= 0x30a;
	public static final int	BG_SCROLLY_MSB		= 0x30b;
	public static final int	BG_SCREEN_MODE		= 0x30c;
	public static final int	BG_PAL_INTENSITY_RG	= 0x1fe;
	public static final int	BG_PAL_INTENSITY_BU	= 0x1ff;
	
	static int 		 ps5_vram_page = 0x0;
	static int		 bg_clip_mode;
	
	static mame_bitmap bitmap_bg;
	static UBytePtr bg_dirtybuffer = new UBytePtr();
	
	/* Paged RAM 0 */
	static UBytePtr ps5_background_videoram = new UBytePtr();
	static UBytePtr ps5_dummy_bg_ram = new UBytePtr();
	
	/* Paged RAM 1 */
	static UBytePtr ps5_io_ram = new UBytePtr();
	static UBytePtr ps5_palette_ram = new UBytePtr();
	static UBytePtr ps5_foreground_videoram = new UBytePtr();
	
	
	static int is_psychic5_title_mode()
	{
		if (ps5_foreground_videoram.read(0x7C6)=='H') return 0;
		return 1;
	}
	
	public static InitMachinePtr psychic5_init_machine = new InitMachinePtr() { public void handler() 
	{
		bg_clip_mode = -10;
	} };
	
	public static WriteHandlerPtr psychic5_vram_page_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		ps5_vram_page = data;
	} };
	
	public static ReadHandlerPtr psychic5_vram_page_select_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return ps5_vram_page;
	} };
	
	static void psychic5_paletteram_w(int color_offs, int offset, int data)
	{
		int bit0,bit1,bit2,bit3;
		int r,g,b,val;
	
		ps5_palette_ram.write(offset, data);
	
		/* red component */
		val  = ps5_palette_ram.read(offset & ~1) >> 4;
		bit0 = (val >> 0) & 0x01;
		bit1 = (val >> 1) & 0x01;
		bit2 = (val >> 2) & 0x01;
		bit3 = (val >> 3) & 0x01;
		r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
		/* green component */
		val = ps5_palette_ram.read(offset & ~1);
		bit0 = (val >> 0) & 0x01;
		bit1 = (val >> 1) & 0x01;
		bit2 = (val >> 2) & 0x01;
		bit3 = (val >> 3) & 0x01;
		g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
		/* blue component */
		val = ps5_palette_ram.read(offset | 1) >> 4;
		bit0 = (val >> 0) & 0x01;
		bit1 = (val >> 1) & 0x01;
		bit2 = (val >> 2) & 0x01;
		bit3 = (val >> 3) & 0x01;
		b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
		palette_set_color((offset / 2)-color_offs,r,g,b);
	}
	
	static void set_background_palette_intensity()
	{
		int i,r,g,b,val,lo,hi,ir,ig,ib,ix;
		int bit0,bit1,bit2,bit3;
	
		/* red,green,blue intensites */
		ir = 15 - (ps5_palette_ram.read(BG_PAL_INTENSITY_RG) >> 4);
		ig = 15 - (ps5_palette_ram.read(BG_PAL_INTENSITY_RG) & 15);
		ib = 15 - (ps5_palette_ram.read(BG_PAL_INTENSITY_BU) >> 4);
		/* unknow but assumes value 2 during the ride on the witches' broom */
		ix = ps5_palette_ram.read(0x1ff) & 15;
	
		for (i=0; i<256; i++)
		{
			lo = ps5_palette_ram.read(0x400+i*2);
			hi = ps5_palette_ram.read(0x400+i*2+1);
	
			val  =  lo >> 4;
			bit0 = (val >> 0) & 0x01;
			bit1 = (val >> 1) & 0x01;
			bit2 = (val >> 2) & 0x01;
			bit3 = (val >> 3) & 0x01;
	
			r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			/* green component */
			val = lo & 15;
			bit0 = (val >> 0) & 0x01;
			bit1 = (val >> 1) & 0x01;
			bit2 = (val >> 2) & 0x01;
			bit3 = (val >> 3) & 0x01;
	
			g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			/* blue component */
			val = hi >> 4;
			bit0 = (val >> 0) & 0x01;
			bit1 = (val >> 1) & 0x01;
			bit2 = (val >> 2) & 0x01;
			bit3 = (val >> 3) & 0x01;
	
			b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			/* grey background enable */
	
			if ((ps5_io_ram.read(BG_SCREEN_MODE) & 2) !=  0)
			{
				val = (int)(0.299*r + 0.587*g + 0.114*b);
	
	 			if (ix==2)		/* purple background enable */
				 palette_set_color(256+i,(int)(val*0.6),0,(int)(val*0.8));
				else			/* grey bg */
				 palette_set_color(256+i,val,val,val);
			}
			else
			{
	
				/* background intensity enable  TO DO BETTER !!! */
	
				if (is_psychic5_title_mode() == 0)
				{
					r = (r>>4) * ir;
		 		  	g = (g>>4) * ig;
	 			  	b = (b>>4) * ib;
				}
				palette_set_color(256+i,r,g,b);
			}
		}
	}
	
	
	public static WriteHandlerPtr psychic5_bgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (ps5_background_videoram.read(offset) != data)
		{
			bg_dirtybuffer.write(offset >> 1, 1);
			ps5_background_videoram.write(offset, data);
		}
	} };
	
	public static ReadHandlerPtr psychic5_paged_ram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int val;
	
		if (ps5_vram_page == 0)
		{
	
			if (offset < 0x1000)
				return ps5_background_videoram.read(offset);
			else
				return ps5_dummy_bg_ram.read(offset & 0xfff);
	
		} else
		{
			if (offset < 0x400)
			{
				val = 0;
				switch(offset)
				{
					case 0x00:
						val = input_port_0_r.handler(0);
						break;
					case 0x01:
						val = input_port_1_r.handler(0);
						break;
					case 0x02:
						val = input_port_2_r.handler(0);
						break;
					case 0x03:
						val = input_port_3_r.handler(0);
						break;
					case 0x04:
						val = input_port_4_r.handler(0);
						break;
					default:
						val = ps5_io_ram.read(offset);
				}
				return (val);
			}
			else if (offset < 0x1000)
			{
				return ps5_palette_ram.read(offset-0x400);
			}
			else
			{
				return ps5_foreground_videoram.read(offset & 0xfff);
			}
		}
		//return 0;
	} };
	
	public static WriteHandlerPtr psychic5_paged_ram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (ps5_vram_page == 0)
		{
			if (offset < 0x1000)
				psychic5_bgvideoram_w.handler(offset,data);
			else
				ps5_dummy_bg_ram.write(offset & 0xfff, data);
		} else
		{
			if (offset < 0x400)
			{
				ps5_io_ram.write(offset, data);
			}
			else if (offset < 0x600)
			{
				psychic5_paletteram_w(000, offset-0x400, data);
			}
			else if (offset > 0x5ff && offset< 0x800)
			{
				ps5_palette_ram.write(offset-0x400, data);
			}
			else if (offset > 0x7ff && offset < 0xa00)
			{
				psychic5_paletteram_w(256, offset-0x400, data);
			}
			else if (offset > 0x9ff && offset < 0xc00)
			{
				psychic5_paletteram_w(256, offset-0x400, data);
			}
			else if (offset < 0x1000)
			{
				ps5_palette_ram.write(offset-0x400, data);
			}
			else
			{
				ps5_foreground_videoram.write(offset & 0xfff, data);
			}
		}
	} };
	
	
	public static VhStartPtr psychic5_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((ps5_background_videoram = new UBytePtr(0x1000)) == null)
		{
			return 1;
		}
		if ((ps5_dummy_bg_ram = new UBytePtr(0x1000)) == null)
		{
			ps5_background_videoram = null;
			return 1;
		}
		if ((ps5_io_ram = new UBytePtr(0x400)) == null)
		{
			ps5_background_videoram = null;
			ps5_dummy_bg_ram = null;
			return 1;
		}
		if ((ps5_palette_ram = new UBytePtr(0xc00)) == null)
		{
			ps5_background_videoram = null;
			ps5_dummy_bg_ram = null;
			ps5_io_ram = null;
			return 1;
		}
		if ((ps5_foreground_videoram = new UBytePtr(0x1000)) == null)
		{
			ps5_background_videoram = null;
			ps5_dummy_bg_ram = null;
			ps5_io_ram = null;
			ps5_palette_ram = null;
			return 1;
		}
	        if ((bg_dirtybuffer = new UBytePtr(32*64)) == null)
		{
			ps5_background_videoram = null;
			ps5_dummy_bg_ram = null;
			ps5_io_ram = null;
			ps5_palette_ram = null;
			ps5_foreground_videoram = null;
	        	return 1;
		}
		if ((bitmap_bg = bitmap_alloc(Machine.drv.screen_width*4,Machine.drv.screen_height*2)) == null)
		{
			ps5_background_videoram = null;
			ps5_dummy_bg_ram = null;
			ps5_io_ram = null;
			ps5_palette_ram = null;
			ps5_foreground_videoram = null;
			bg_dirtybuffer = null;
			return 1;
		}
	        memset(bg_dirtybuffer,1,32*64);
		memset(ps5_background_videoram, 0,0x1000);
		memset(ps5_dummy_bg_ram,0,0x1000);
		memset(ps5_io_ram,0,0x400);
		memset(ps5_palette_ram,0,0xc00);
		memset(ps5_foreground_videoram,0,0x1000);
	        return 0;
	} };
	
	public static VhStopPtr psychic5_vh_stop = new VhStopPtr() { public void handler() 
	{
		ps5_background_videoram = null;
		ps5_dummy_bg_ram = null;
		ps5_io_ram = null;
		ps5_palette_ram = null;
		ps5_foreground_videoram = null;
		bg_dirtybuffer = null;
		bitmap_free(bitmap_bg);
	} };
	
	static void psychic5_draw_background(mame_bitmap bitmap)
	{
		int x,y,offs;
		int sx,sy,tile,palette,flipx,flipy,lo,hi;
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
	
		for (x = 31; x >=0; x--)
		{
			for (y = 0 ; y < 64; y++)
			{
				offs = y*64+(x*2);
				if (bg_dirtybuffer.read(offs >> 1) != 0)
				{
					sx = y << 4;
					sy = x << 4;
	
					bg_dirtybuffer.write(offs >> 1, 0);
	
					lo = ps5_background_videoram.read(offs);
					hi = ps5_background_videoram.read(offs+1);
					tile = ((hi & 0xc0) << 2) | lo;
					flipx = hi & 0x10;
					flipy = hi & 0x20;
					palette = hi & 0x0f;
					drawgfx(bitmap,Machine.gfx[1],
							  tile,
							  palette,
							  flipx,flipy,
							  sx,sy,
							  null,TRANSPARENCY_NONE,0);
				}
			}
		}
	}
	
	static void psychic5_draw_foreground(mame_bitmap bitmap)
	{
		int x,y,offs;
		int sx,sy,tile,palette,flipx,flipy,lo,hi;
	
		/* Draw the foreground text */
	
		for (x = 31; x >=0; x--)
		{
			for (y = 0; y < 32; y++)
			{
				offs = y*64+(x*2);
				if (ps5_foreground_videoram.read(offs+1)!=0xFF)
				{
					sx = y << 3;
					sy = x << 3;
	
					lo = ps5_foreground_videoram.read(offs);
					hi = ps5_foreground_videoram.read(offs+1);
					tile = ((hi & 0xc0) << 2) | lo;
					flipx = hi & 0x10;
					flipy = hi & 0x20;
					palette = hi & 0x0f;
	
					drawgfx(bitmap,Machine.gfx[2],
								tile,
								palette,
								flipx,flipy,
								sx,sy,
								Machine.visible_area,TRANSPARENCY_PEN, 15);
				}
			}
		}
	}
	
	static void psychic5_draw_sprites(mame_bitmap bitmap)
	{
		int offs,sx,sy,tile,palette,flipx,flipy;
		int size32,tileofs0,tileofs1,tileofs2,tileofs3,temp1,temp2;
	
		/* Draw the sprites */
	
		for (offs = 11 ;offs < spriteram_size[0]; offs+=16)
		{
			if (!(spriteram.read(offs+4)==0 && spriteram.read(offs)==0xf0))
			{
				sx = spriteram.read(offs+1);
				sy = spriteram.read(offs);
				if ((spriteram.read(offs+2) & 0x01)!=0) sx-=256;
				if ((spriteram.read(offs+2) & 0x04)!=0) sy-=256;
				tile	 = spriteram.read(offs+3)+((spriteram.read(offs+2) & 0xc0)<<2);
				size32	 = spriteram.read(offs+2) & 0x08;
				flipx	 = spriteram.read(offs+2) & 0x20;
				flipy	 = spriteram.read(offs+2) & 0x10;
				palette	 = spriteram.read(offs+4) & 0x0f;
				if (flipx != 0)
				{
					tileofs0 = 1;
					tileofs1 = 0;
					tileofs2 = 3;
					tileofs3 = 2;
				} else
				{
					tileofs0 = 0;
					tileofs1 = 1;
					tileofs2 = 2;
					tileofs3 = 3;
				}
				if (flipy != 0)
				{
					temp1	 = tileofs0;
					temp2	 = tileofs1;
					tileofs0 = tileofs2;
					tileofs1 = tileofs3;
					tileofs2 = temp1;
					tileofs3 = temp2;
				}
				if (size32 != 0)
				{
					drawgfx(bitmap,Machine.gfx[0],
								tile+tileofs0,
								palette,
								flipy,flipx,
								sx,sy,
								Machine.visible_area,
								TRANSPARENCY_PEN, 15);
					drawgfx(bitmap,Machine.gfx[0],
								tile+tileofs1,
								palette,
								flipy,flipx,
								sx,sy+16,
								Machine.visible_area,
								TRANSPARENCY_PEN, 15);
					drawgfx(bitmap,Machine.gfx[0],
								tile+tileofs2,
								palette,
								flipy,flipx,
								sx+16,sy,
								Machine.visible_area,
								TRANSPARENCY_PEN, 15);
					drawgfx(bitmap,Machine.gfx[0],
								tile+tileofs3,
								palette,
								flipy,flipx,
								sx+16,sy+16,
								Machine.visible_area,
								TRANSPARENCY_PEN, 15);
				}
				else
				{
					drawgfx(bitmap,Machine.gfx[0],
								tile,
								palette,
								flipy,flipx,
								sx,sy,
								Machine.visible_area,
								TRANSPARENCY_PEN, 15);
				}
			}
		}
	}
	
	
	
	
	static void psychic5_draw_sprites2(mame_bitmap bitmap)
	{
		int offs,sx,sy,tile,palette,flipx,flipy;
		int size32,tileofs0,tileofs1,tileofs2,tileofs3;
	
		for (offs = 11 ;offs < spriteram_size[0]; offs+=16)
		{
			if (!(spriteram.read(offs+4)==0 && spriteram.read(offs)==0xf0))
			{
				sx = spriteram.read(offs+1);
				sy = spriteram.read(offs);
				if ((spriteram.read(offs+2) & 0x01)!=0) sx-=256;
				if ((spriteram.read(offs+2) & 0x04)!=0) sy-=256;
				tile	 = spriteram.read(offs+3)+((spriteram.read(offs+2) & 0xc0)<<2);
				size32	 = spriteram.read(offs+2) & 0x08;
				flipx	 = spriteram.read(offs+2) & 0x20;
				flipy	 = spriteram.read(offs+2) & 0x10;
				palette	 = spriteram.read(offs+4) & 0x0;
				tileofs0 = 0;
				tileofs1 = 1;
				tileofs2 = 2;
				tileofs3 = 3;
				if (size32 != 0)
				{
					drawgfx(bitmap,Machine.gfx[0],
								tile+tileofs0,
								palette,
								flipy,flipx,
								sx,sy,
								Machine.visible_area,
								TRANSPARENCY_NONE, 0);
					drawgfx(bitmap,Machine.gfx[0],
								tile+tileofs1,
								palette,
								flipy,flipx,
								sx,sy+16,
								Machine.visible_area,
								TRANSPARENCY_NONE, 0);
					drawgfx(bitmap,Machine.gfx[0],
								tile+tileofs2,
								palette,
								flipy,flipx,
								sx+16,sy,
								Machine.visible_area,
								TRANSPARENCY_NONE, 0);
					drawgfx(bitmap,Machine.gfx[0],
								tile+tileofs3,
								palette,
								flipy,flipx,
								sx+16,sy+16,
								Machine.visible_area,
								TRANSPARENCY_NONE, 0);
				}
			}
		}
	}
	
	
	
	
	
	public static VhUpdatePtr psychic5_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int bg_scrollx,bg_scrolly;
	
		bg_scrollx = ((ps5_io_ram.read(BG_SCROLLX_LSB)+((ps5_io_ram.read(BG_SCROLLX_MSB) & 3)*256)) & 0x3FF) >> 4;
		bg_scrolly = ((ps5_io_ram.read(BG_SCROLLY_LSB)+((ps5_io_ram.read(BG_SCROLLY_MSB) & 1)*256)) & 0x1FF) >> 4;
	
		set_background_palette_intensity();
	
		bg_scrollx = -((ps5_io_ram.read(BG_SCROLLX_LSB)+((ps5_io_ram.read(BG_SCROLLX_MSB) & 3)*256)) & 0x3FF);
		bg_scrolly = -((ps5_io_ram.read(BG_SCROLLY_LSB)+((ps5_io_ram.read(BG_SCROLLY_MSB) & 1)*256)) & 0x1FF);
	
		if ((ps5_io_ram.read(BG_SCREEN_MODE) & 1)!=0)  /* background enable */
		{
			psychic5_draw_background(bitmap_bg);
	
			if (is_psychic5_title_mode() != 0)
			{
				rectangle clip=new rectangle();
				int sx1,sy1,sy2,tile;
	
				sx1  = spriteram.read(12);		/* sprite 0 */
				sy1  = spriteram.read(11);
				tile = spriteram.read(14);
				sy2 = spriteram.read(11+128);	/* sprite 8 */
	
				clip.min_x = Machine.visible_area.min_x;
				clip.min_y = Machine.visible_area.min_y;
				clip.max_x = Machine.visible_area.max_x;
				clip.max_y = Machine.visible_area.max_y;
	
				if (bg_clip_mode >=0 && bg_clip_mode < 3 && sy1==240) bg_clip_mode = 0;
				if (bg_clip_mode > 2 && bg_clip_mode < 5 && sy2==240) bg_clip_mode = -10;
				if (bg_clip_mode > 4 && bg_clip_mode < 7 && sx1==240) bg_clip_mode = 0;
				if (bg_clip_mode > 6 && bg_clip_mode < 9 && (sx1==240 || sx1==0)) bg_clip_mode = -10;
	
				if (sy1!=240 && sy1!=0 && bg_clip_mode<=0)
				{
					if (sy1>128)
					 bg_clip_mode=1;
					else
					 bg_clip_mode=2;
				}
				if (sy2!=240 && sy2!=0 && bg_clip_mode<=0)
				{
					if (sy2>128)
					 bg_clip_mode=3;
					else
					 bg_clip_mode=4;
				}
				if (sx1!=240 && sx1!=0 && bg_clip_mode<=0 && tile==0x3c)
				{
					if (sx1>128)
					 bg_clip_mode=5;
					else
					 bg_clip_mode=6;
				}
				if (sx1!=240 && sx1!=0 && bg_clip_mode<=0 && tile==0x1c)
				{
					if (sx1>128)
					 bg_clip_mode=7;
					else
					 bg_clip_mode=8;
				}
				if (bg_clip_mode != 0)
				{
					if (bg_clip_mode==1)
					 clip.min_y = sy1;
					else if (bg_clip_mode==2)
	 				 clip.max_y = sy1;
					else if (bg_clip_mode==3)
					 clip.max_y = sy2;
					else if (bg_clip_mode==4)
					 clip.min_y = sy2;
					else if (bg_clip_mode==5)
					 clip.min_x = sx1;
					else if (bg_clip_mode==6)
	 				 clip.max_x = sx1;
					else if (bg_clip_mode==7)
					 clip.max_x = sx1;
					else if (bg_clip_mode==8)
					 clip.min_x = sx1;
					else if (bg_clip_mode==-10)
					{
						clip.min_x = 0;
						clip.min_y = 0;
						clip.max_x = 0;
						clip.max_y = 0;
					}
					fillbitmap(bitmap,Machine.pens[0],Machine.visible_area);
					copyscrollbitmap(bitmap,bitmap_bg,1,new int[]{bg_scrollx},1,new int[]{bg_scrolly},clip,TRANSPARENCY_NONE,0);
				} else
					copyscrollbitmap(bitmap,bitmap_bg,1,new int[]{bg_scrollx},1,new int[]{bg_scrolly},Machine.visible_area,TRANSPARENCY_NONE,0);
	
			} else
				copyscrollbitmap(bitmap,bitmap_bg,1,new int[]{bg_scrollx},1,new int[]{bg_scrolly},Machine.visible_area,TRANSPARENCY_NONE,0);
		} else
		{
			fillbitmap(bitmap,Machine.pens[0],Machine.visible_area);
			memset(bg_dirtybuffer,1,64*32);
		}
	
		if (is_psychic5_title_mode() == 0)
		{
			psychic5_draw_sprites(bitmap);
			bg_clip_mode=-10;
		}
		psychic5_draw_foreground(bitmap);
	} };
}
