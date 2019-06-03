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
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.driverH.*;
import static mame056.palette.*;

public class dooyong
{
	public static UBytePtr lastday_txvideoram = new UBytePtr();
	public static UBytePtr lastday_bgscroll = new UBytePtr(), lastday_fgscroll = new UBytePtr(), bluehawk_fg2scroll = new UBytePtr();
	public static UBytePtr rshark_scroll1=new UBytePtr(2), rshark_scroll2=new UBytePtr(2), rshark_scroll4=new UBytePtr(2), rshark_scroll3=new UBytePtr(2);
	static int tx_pri;
	
	
	public static WriteHandlerPtr lastday_ctrl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bits 0 and 1 are coin counters */
		coin_counter_w.handler(0,data & 0x01);
		coin_counter_w.handler(1,data & 0x02);
	
		/* bit 3 is used but unknown */
	
		/* bit 4 is used but unknown */
	
		/* bit 6 is flip screen */
		flip_screen_set(data & 0x40);
	} };
	
	public static WriteHandlerPtr pollux_ctrl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bit 0 is flip screen */
		flip_screen_set(data & 0x01);
	
		/* bits 6 and 7 are coin counters */
		coin_counter_w.handler(0,data & 0x80);
		coin_counter_w.handler(1,data & 0x40);
	
		/* bit 1 is used but unknown */
	
		/* bit 2 is continuously toggled (unknown) */
	} };
	
	public static WriteHandlerPtr primella_ctrl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	 	int bankaddress;
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
		/* bits 0-2 select ROM bank */
		bankaddress = 0x10000 + (data & 0x07) * 0x4000;
		cpu_setbank(1, new UBytePtr(RAM, bankaddress));
	
		/* bit 3 disables tx layer */
		tx_pri = data & 0x08;
	
		/* bit 4 flips screen */
		flip_screen_set(data & 0x10);
	
		/* bit 5 used but unknown */
	
	//	logerror("%04x: bankswitch = %02x\n",cpu_get_pc(),data&0xe0);
	} };
	
	public static WriteHandlerPtr rshark_ctrl_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /*TODO*///if (ACCESSING_LSB)
		/*TODO*///{
			/* bit 0 flips screen */
			flip_screen_set(data & 0x01);
	
			/* bit 4 used but unknown */
	
			/* bit 5 used but unknown */
		/*TODO*///}
            }
        };                
	
	static void draw_layer(mame_bitmap bitmap,int gfx, UBytePtr scroll,
			UBytePtr tilemap, int transparency)
	{
		int offs;
		int scrollx,scrolly;
	
		scrollx = scroll.read(0) + (scroll.read(1) << 8);
		scrolly = scroll.read(3) + (scroll.read(4) << 8);
	
		for (offs = 0;offs < 0x100;offs += 2)
		{
			int sx,sy,code,color,attr,flipx,flipy;
			int toffs = offs+((scrollx&~0x1f)>>1);
	
			attr = tilemap.read(toffs);
			code = tilemap.read(toffs+1) | ((attr & 0x01) << 8) | ((attr & 0x80) << 2);
			color = (attr & 0x78) >> 3;
			sx = 32 * ((offs/2) / 8) - (scrollx & 0x1f);
			sy = (32 * ((offs/2) % 8) - scrolly) & 0xff;
			flipx = attr & 0x02;
			flipy = attr & 0x04;
			if (flip_screen() != 0)
			{
				sx = 512-32 - sx;
				sy = 256-32 - sy;
				flipx = flipx!=0?0:1;
				flipy = flipy!=0?0:1;
			}
	
			drawgfx(bitmap,Machine.gfx[gfx],
					code,
					color,
					flipx,flipy,
					sx,sy,
					Machine.visible_area,transparency,15);
			/* wraparound */
			if ((scrolly & 0x1f) != 0)
			{
				drawgfx(bitmap,Machine.gfx[gfx],
						code,
						color,
						flipx,flipy,
						sx,((sy + 0x20) & 0xff) - 0x20,
						Machine.visible_area,transparency,15);
			}
		}
	}
	
	static void bluehawk_draw_layer(mame_bitmap bitmap,int gfx, UBytePtr scroll,
			UBytePtr tilemap, int transparency)
	{
		int offs;
		int scrollx,scrolly;
	
		scrollx = scroll.read(0) + (scroll.read(1) << 8);
		scrolly = scroll.read(3) + (scroll.read(4) << 8);
	
		for (offs = 0;offs < 0x100;offs += 2)
		{
			int sx,sy,code,color,attr,flipx,flipy;
			int toffs = offs+((scrollx&~0x1f)>>1);
	
			attr = tilemap.read(toffs);
			code = tilemap.read(toffs+1) | ((attr & 0x03) << 8);
			color = (attr & 0x3c) >> 2;
			sx = 32 * ((offs/2) / 8) - (scrollx & 0x1f);
			sy = (32 * ((offs/2) % 8) - scrolly) & 0xff;
			flipx = attr & 0x40;
			flipy = attr & 0x80;
			if (flip_screen() != 0)
			{
				sx = 512-32 - sx;
				sy = 256-32 - sy;
				flipx = flipx!=0?0:1;
				flipy = flipy!=0?0:1;
			}
	
			drawgfx(bitmap,Machine.gfx[gfx],
					code,
					color,
					flipx,flipy,
					sx,sy,
					Machine.visible_area,transparency,15);
			/* wraparound */
			if ((scrolly & 0x1f) != 0)
			{
				drawgfx(bitmap,Machine.gfx[gfx],
						code,
						color,
						flipx,flipy,
						sx,((sy + 0x20) & 0xff) - 0x20,
						Machine.visible_area,transparency,15);
			}
		}
	}
	
	static void bluehawk_draw_layer2(mame_bitmap bitmap,int gfx, UBytePtr scroll,
			UBytePtr tilemap,int transparency)
	{
		int offs;
		int scrollx,scrolly;
	
		scrollx = scroll.read(0) + (scroll.read(1) << 8);
		scrolly = scroll.read(3) + (scroll.read(4) << 8);
	
		for (offs = 0;offs < 0x100;offs += 2)
		{
			int sx,sy,code,color,attr,flipx,flipy;
			int toffs = offs+((scrollx&~0x1f)>>1);
	
			attr = tilemap.read(toffs);
			code = tilemap.read(toffs+1) | ((attr & 0x01) << 8);
			color = (attr & 0x78) >> 3;
			sx = 32 * ((offs/2) / 8) - (scrollx & 0x1f);
			sy = (32 * ((offs/2) % 8) - scrolly) & 0xff;
			flipx = 0;
			flipy = attr & 0x04;
			if (flip_screen() != 0)
			{
				sx = 512-32 - sx;
				sy = 256-32 - sy;
				flipx = flipx!=0?0:1;
				flipy = flipy!=0?0:1;
			}
	
			drawgfx(bitmap,Machine.gfx[gfx],
					code,
					color,
					flipx,flipy,
					sx,sy,
					Machine.visible_area,transparency,15);
			/* wraparound */
			if ((scrolly & 0x1f) != 0)
			{
				drawgfx(bitmap,Machine.gfx[gfx],
						code,
						color,
						flipx,flipy,
						sx,((sy + 0x20) & 0xff) - 0x20,
						Machine.visible_area,transparency,15);
			}
		}
	}
	
	static void rshark_draw_layer(mame_bitmap bitmap,int gfx, UBytePtr scroll,
			UBytePtr tilemap, UBytePtr tilemap2,int transparency)
	{
		int offs;
		int scrollx,scrolly;
	
		scrollx = (scroll.read(0)&0xff) + ((scroll.read(1)&0xff) << 8);
		scrolly = (scroll.read(3)&0xff) + ((scroll.read(4)&0xff) << 8);
	
		for (offs = 0;offs < 0x800;offs += 2)
		{
			int sx,sy,code,color,attr,attr2,flipx,flipy;
			int toffs = offs+((scrollx&~0x0f)<<2);
	
			attr = tilemap.read(toffs);
			attr2 = tilemap2.read(toffs/2);
			code = tilemap.read(toffs+1) | ((attr & 0x1f) << 8);
			color = attr2 & 0x0f;
			sx = 16 * ((offs/2) / 32) - (scrollx & 0x0f);
			sy = (16 * ((offs/2) % 32) - scrolly) & 0x1ff;
			if (sy > 256) sy -= 512;
			flipx = attr & 0x40;
			flipy = attr & 0x80;
			if (flip_screen() != 0)
			{
				sx = 512-16 - sx;
				sy = 256-16 - sy;
				flipx = flipx!=0?0:1;
				flipy = flipy!=0?0:1;
			}
	
			drawgfx(bitmap,Machine.gfx[gfx],
					code,
					color,
					flipx,flipy,
					sx,sy,
					Machine.visible_area,transparency,15);
		}
	}
	
	static void draw_tx(mame_bitmap bitmap,int yoffset)
	{
		int offs;
	
		for (offs = 0;offs < 0x800;offs++)
		{
			int sx,sy,attr;
	
			attr = lastday_txvideoram.read(offs+0x800);
			sx = offs / 32;
			sy = offs % 32;
			if (flip_screen() != 0)
			{
				sx = 63 - sx;
				sy = 31 - sy;
			}
	
			drawgfx(bitmap,Machine.gfx[0],
					lastday_txvideoram.read(offs) | ((attr & 0x0f) << 8),
					(attr & 0xf0) >> 4,
					flip_screen(),flip_screen(),
					8*sx,8*(sy + yoffset),
					Machine.visible_area,TRANSPARENCY_PEN,15);
		}
	}
	
	static void bluehawk_draw_tx(mame_bitmap bitmap)
	{
		int offs;
	
		for (offs = 0;offs < 0x1000;offs += 2)
		{
			int sx,sy,attr;
	
			attr = lastday_txvideoram.read(offs+1);
			sx = (offs/2) / 32;
			sy = (offs/2) % 32;
			if (flip_screen() != 0)
			{
				sx = 63 - sx;
				sy = 31 - sy;
			}
	
			drawgfx(bitmap,Machine.gfx[0],
					lastday_txvideoram.read(offs) | ((attr & 0x0f) << 8),
					(attr & 0xf0) >> 4,
					flip_screen(),flip_screen(),
					8*sx,8*sy,
					Machine.visible_area,TRANSPARENCY_PEN,15);
		}
	}
	
	static void draw_sprites(mame_bitmap bitmap,int pollux_extensions)
	{
		int offs;
	
		for (offs = spriteram_size[0]-32;offs >= 0;offs -= 32)
		{
			int sx,sy,code,color;
			int flipx=0,flipy=0,height=0,y;
	
			sx = buffered_spriteram.read(offs+3) | ((buffered_spriteram.read(offs+1) & 0x10) << 4);
			sy = buffered_spriteram.read(offs+2);
			code = buffered_spriteram.read(offs) | ((buffered_spriteram.read(offs+1) & 0xe0) << 3);
			color = buffered_spriteram.read(offs+1) & 0x0f;
	
			if (pollux_extensions != 0)
			{
				/* gulfstrm, pollux, bluehawk */
				code |= ((buffered_spriteram.read(offs+0x1c) & 0x01) << 11);
	
				if (pollux_extensions >= 2)
				{
					/* pollux, bluehawk */
					height = (buffered_spriteram.read(offs+0x1c) & 0x70) >> 4;
					code &= ~height;
					if (pollux_extensions == 3)
					{
						/* bluehawk */
						sy += 6 - ((~buffered_spriteram.read(offs+0x1c) & 0x02) << 7);
						flipx = buffered_spriteram.read(offs+0x1c) & 0x08;
						flipy = buffered_spriteram.read(offs+0x1c) & 0x04;
					}
				}
			}
	
			if (flip_screen() != 0)
			{
				sx = 498 - sx;
				sy = 240-16*height - sy;
				flipx = flipx!=0?0:1;
				flipy = flipy!=0?0:1;
			}
	
			for (y = 0;y <= height;y++)
			{
				drawgfx(bitmap,Machine.gfx[1],
						code+y,
						color,
						flipx,flipy,
						sx,flipy!=0 ? sy + 16*(height-y) : sy + 16*y,
						Machine.visible_area,TRANSPARENCY_PEN,15);
			}
		}
	}
	
	static void rshark_draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
		for (offs = 0;offs < spriteram_size[0]/2;offs += 8)
		{
			if ((buffered_spriteram.read(offs) & 0x0001) != 0)	/* enable */
			{
				int sx,sy,code,color;
				int flipx=0,flipy=0,width,height,x,y;
	
				sx = buffered_spriteram.read(offs+4) & 0x01ff;
				sy = buffered_spriteram.read(offs+6);
				code = buffered_spriteram.read(offs+3);
				color = buffered_spriteram.read(offs+7);
				width = buffered_spriteram.read(offs+1) & 0x000f;
				height = (buffered_spriteram.read(offs+1) & 0x00f0) >> 4;
	
				if (flip_screen() != 0)
				{
					sx = 498-16*width - sx;
					sy = 240-16*height - sy;
					flipx = flipx!=0?0:1;
					flipy = flipy!=0?0:1;
				}
	
				for (y = 0;y <= height;y++)
				{
					for (x = 0;x <= width;x++)
					{
						drawgfx(bitmap,Machine.gfx[0],
								code,
								color,
								flipx,flipy,
								flipx!=0 ? sx + 16*(width-x) : sx + 16*x,
								flipy!=0 ? sy + 16*(height-y) : sy + 16*y,
								Machine.visible_area,TRANSPARENCY_PEN,15);
	
						code++;
					}
				}
			}
		}
	}
	
	
	public static VhUpdatePtr lastday_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		draw_layer(bitmap,2,lastday_bgscroll,memory_region(REGION_GFX5),TRANSPARENCY_NONE);
		draw_layer(bitmap,3,lastday_fgscroll,memory_region(REGION_GFX6),TRANSPARENCY_PEN);
		draw_sprites(bitmap,0);
		draw_tx(bitmap,-1);
	} };
	
	public static VhUpdatePtr gulfstrm_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		draw_layer(bitmap,2,lastday_bgscroll,memory_region(REGION_GFX5),TRANSPARENCY_NONE);
		draw_layer(bitmap,3,lastday_fgscroll,memory_region(REGION_GFX6),TRANSPARENCY_PEN);
		draw_sprites(bitmap,1);
		draw_tx(bitmap,-1);
	} };
	
	public static VhUpdatePtr pollux_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		draw_layer(bitmap,2,lastday_bgscroll,memory_region(REGION_GFX5),TRANSPARENCY_NONE);
		draw_layer(bitmap,3,lastday_fgscroll,memory_region(REGION_GFX6),TRANSPARENCY_PEN);
		draw_sprites(bitmap,2);
		draw_tx(bitmap,0);
	} };
	
	public static VhUpdatePtr bluehawk_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		bluehawk_draw_layer(bitmap,2,lastday_bgscroll,new UBytePtr(memory_region(REGION_GFX3), 0x78000),TRANSPARENCY_NONE);
		bluehawk_draw_layer(bitmap,3,lastday_fgscroll,new UBytePtr(memory_region(REGION_GFX4), 0x78000),TRANSPARENCY_PEN);
		draw_sprites(bitmap,3);
		bluehawk_draw_layer2(bitmap,4,bluehawk_fg2scroll,new UBytePtr(memory_region(REGION_GFX5), 0x38000),TRANSPARENCY_PEN);
		bluehawk_draw_tx(bitmap);
	} };
	
	public static VhUpdatePtr primella_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/*TODO*///bluehawk_draw_layer(bitmap,1,lastday_bgscroll,memory_region(REGION_GFX2)+memory_region_length(REGION_GFX2)-0x8000,TRANSPARENCY_NONE);
                bluehawk_draw_layer(bitmap,1,lastday_bgscroll,new UBytePtr(memory_region(REGION_GFX2), 0x8000),TRANSPARENCY_NONE);
		if (tx_pri != 0) bluehawk_draw_tx(bitmap);
		/*TODO*///bluehawk_draw_layer(bitmap,2,lastday_fgscroll,memory_region(REGION_GFX3)+memory_region_length(REGION_GFX3)-0x8000,TRANSPARENCY_PEN);
                bluehawk_draw_layer(bitmap,2,lastday_fgscroll,new UBytePtr(memory_region(REGION_GFX3), 0x8000),TRANSPARENCY_PEN);
		if (tx_pri == 0) bluehawk_draw_tx(bitmap);
	} };
	
	public static VhUpdatePtr rshark_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		rshark_draw_layer(bitmap,4,rshark_scroll4,new UBytePtr(memory_region(REGION_GFX5)),new UBytePtr(memory_region(REGION_GFX6), 0x60000),TRANSPARENCY_NONE);
		rshark_draw_layer(bitmap,3,rshark_scroll3,new UBytePtr(memory_region(REGION_GFX4)),new UBytePtr(memory_region(REGION_GFX6), 0x40000),TRANSPARENCY_PEN);
		rshark_draw_layer(bitmap,2,rshark_scroll2,new UBytePtr(memory_region(REGION_GFX3)),new UBytePtr(memory_region(REGION_GFX6), 0x20000),TRANSPARENCY_PEN);
		rshark_draw_layer(bitmap,1,rshark_scroll1,new UBytePtr(memory_region(REGION_GFX2)),new UBytePtr(memory_region(REGION_GFX6), 0x00000),TRANSPARENCY_PEN);
		rshark_draw_sprites(bitmap);
	} };
	
	public static VhEofCallbackPtr dooyong_eof_callback = new VhEofCallbackPtr() {
            public void handler() {
                buffer_spriteram_w.handler(0,0);
            }
        };
	
	public static VhEofCallbackPtr rshark_eof_callback = new VhEofCallbackPtr() {
            public void handler() {
		buffer_spriteram_w.handler(0,0);
            }
        };
}

