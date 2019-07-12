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
// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.tilemapH.*;
//import static mame056.tilemapC.*;
import static mame037b11.mame.tilemapC.*;

public class baraduke
{
	
	public static UBytePtr baraduke_textram=new UBytePtr(), baraduke_videoram=new UBytePtr();
	
	static struct_tilemap[] tilemap=new struct_tilemap[2];	/* backgrounds */
	static int[] xscroll=new int[2], yscroll=new int[2];	/* scroll registers */
	static int flipscreen;
	
	/***************************************************************************
	
		Convert the color PROMs into a more useable format.
	
		The palette PROMs are connected to the RGB output this way:
	
		bit 3	-- 220 ohm resistor  -- RED/GREEN/BLUE
				-- 470 ohm resistor  -- RED/GREEN/BLUE
				-- 1  kohm resistor  -- RED/GREEN/BLUE
		bit 0	-- 2.2kohm resistor  -- RED/GREEN/BLUE
	
	***************************************************************************/
	
	public static VhConvertColorPromPtr baraduke_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
		int bit0,bit1,bit2,bit3;
                int _palette = 0;
	
		for (i = 0; i < 2048; i++)
		{
			/* red component */
			bit0 = (color_prom.read(2048)>> 0) & 0x01;
			bit1 = (color_prom.read(2048)>> 1) & 0x01;
			bit2 = (color_prom.read(2048)>> 2) & 0x01;
			bit3 = (color_prom.read(2048)>> 3) & 0x01;
			palette[_palette++] = (char) (0x0e*bit0 + 0x1f*bit1 + 0x43*bit2 + 0x8f*bit3);
	
			/* green component */
			bit0 = (color_prom.read(0)>> 0) & 0x01;
			bit1 = (color_prom.read(0)>> 1) & 0x01;
			bit2 = (color_prom.read(0)>> 2) & 0x01;
			bit3 = (color_prom.read(0)>> 3) & 0x01;
			palette[_palette++] = (char) (0x0e*bit0 + 0x1f*bit1 + 0x43*bit2 + 0x8f*bit3);
	
			/* blue component */
			bit0 = (color_prom.read(0)>> 4) & 0x01;
			bit1 = (color_prom.read(0)>> 5) & 0x01;
			bit2 = (color_prom.read(0)>> 6) & 0x01;
			bit3 = (color_prom.read(0)>> 7) & 0x01;
			palette[_palette++] = (char) (0x0e*bit0 + 0x1f*bit1 + 0x43*bit2 + 0x8f*bit3);
	
			color_prom.inc();
		}
	} };
	
	/***************************************************************************
	
		Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_tile_info0 = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                int attr = baraduke_videoram.read(2*tile_index + 1);
		int code = baraduke_videoram.read(2*tile_index);
	
		SET_TILE_INFO(
				1 + ((attr & 0x02) >> 1),
				code | ((attr & 0x01) << 8),
				attr,
				0);
            }
        };
	
	static GetTileInfoPtr get_tile_info1 = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = baraduke_videoram.read(0x1000 + 2*tile_index + 1);
		int code = baraduke_videoram.read(0x1000 + 2*tile_index);
	
		SET_TILE_INFO(
				3 + ((attr & 0x02) >> 1),
				code | ((attr & 0x01) << 8),
				attr,
				0);
            }
        };
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr baraduke_vh_start = new VhStartPtr() { public int handler() 
	{
		tilemap[0] = tilemap_create(get_tile_info0,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);
		tilemap[1] = tilemap_create(get_tile_info1,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);
	
		if (tilemap[0]==null || tilemap[1]==null)
			return 1;
	
		/*TODO*///tilemap_set_transparent_pen(tilemap[0],7);
                tilemap[0].transparent_pen = 7;
		/*TODO*///tilemap_set_transparent_pen(tilemap[1],7);
                tilemap[1].transparent_pen = 7;
	
		return 0;
	} };
	
	/***************************************************************************
	
		Memory handlers
	
	***************************************************************************/
	
	public static ReadHandlerPtr baraduke_videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return baraduke_videoram.read(offset);
	} };
	
	public static WriteHandlerPtr baraduke_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (baraduke_videoram.read(offset) != data)
		{
			baraduke_videoram.write(offset, data);
			tilemap_mark_tile_dirty(tilemap[offset/0x1000],(offset&0xfff)/2);
		}
	} };
	
	static void scroll_w(int layer,int offset,int data)
	{
		int xdisp[] = { 26, 24 };
		int scrollx, scrolly;
	
		switch (offset)
		{
			case 0:	/* high scroll x */
				xscroll[layer] = (xscroll[layer] & 0xff) | (data << 8);
				break;
			case 1:	/* low scroll x */
				xscroll[layer] = (xscroll[layer] & 0xff00) | data;
				break;
			case 2:	/* scroll y */
				yscroll[layer] = data;
				break;
		}
	
		scrollx = xscroll[layer] + xdisp[layer];
		scrolly = yscroll[layer] + 25;
		if (flipscreen != 0)
		{
			scrollx = -scrollx + 227;
			scrolly = -scrolly + 32;
		}
	
		tilemap_set_scrollx(tilemap[layer], 0, scrollx);
		tilemap_set_scrolly(tilemap[layer], 0, scrolly);
	}
	
	public static WriteHandlerPtr baraduke_scroll0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll_w(0, offset, data);
	} };
	public static WriteHandlerPtr baraduke_scroll1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll_w(1, offset, data);
	} };
	
	/***************************************************************************
	
		Display Refresh
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap, int priority)
	{
		rectangle clip = new rectangle(Machine.visible_area);
	
		UBytePtr source = new UBytePtr(spriteram, 0);
		UBytePtr finish = new UBytePtr(spriteram, 0x0800-16);/* the last is NOT a sprite */
	
		int sprite_xoffs = spriteram.read(0x07f5)- 256 * (spriteram.read(0x07f4)& 1) + 16;
		int sprite_yoffs = spriteram.read(0x07f7)- 256 * (spriteram.read(0x07f6)& 1);
	
		while( source.offset<finish.offset )
		{
	/*
		source[4]	S-FT ---P
		source[5]	TTTT TTTT
		source[6]   CCCC CCCX
		source[7]	XXXX XXXX
		source[8]	---T -S-F
		source[9]   YYYY YYYY
	*/
			{
				int attrs = source.read(4);
				int attr2 = source.read(8);
				int color = source.read(6);
				int sx = source.read(7) + (color & 0x01)*256; /* need adjust for left clip */
				int sy = -source.read(9);
				int flipx = attrs & 0x20;
				int flipy = attr2 & 0x01;
				int tall = (attr2 & 0x04)!=0 ? 1 : 0;
				int wide = (attrs & 0x80)!=0 ? 1 : 0;
				int pri = attrs & 0x01;
				int sprite_number = (source.read(5) & 0xff)*4;
				int row,col;
	
				if (pri == priority)
				{
					if ((attrs & 0x10)!=0 && wide==0) sprite_number += 1;
					if ((attr2 & 0x10)!=0 && tall==0) sprite_number += 2;
					color = color >> 1;
	
					if( sx > 512 - 32 ) sx -= 512;
	
					if( flipx!=0 && wide==0 ) sx -= 16;
					if (tall == 0) sy += 16;
					if( tall==0 && (attr2 & 0x10)!=0 && flipy!=0 ) sy -= 16;
	
					sx += sprite_xoffs;
					sy -= sprite_yoffs;
	
					for( row=0; row<=tall; row++ )
					{
						for( col=0; col<=wide; col++ )
						{
							if (flipscreen != 0)
							{
								drawgfx( bitmap, Machine.gfx[5],
									sprite_number+2*row+col,
									color,
									flipx!=0?0:1,flipy!=0?0:1,
									512-67 - (sx+16*(flipx!=0 ? 1-col : col)),
									64-16-209 - (sy+16*(flipy!=0 ? 1-row : row)),
									clip,
									TRANSPARENCY_PEN, 0xf );
							}
							else
							{
								drawgfx( bitmap, Machine.gfx[5],
									sprite_number+2*row+col,
									color,
									flipx,flipy,
									-87 + (sx+16*(flipx!=0 ? 1-col : col)),
									209 + (sy+16*(flipy!=0 ? 1-row : row)),
									clip,
									TRANSPARENCY_PEN, 0x0f );
							}
						}
					}
				}
			}
			source.inc(16);
		}
	}
	
	public static VhUpdatePtr baraduke_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
		/* this is the global sprite Y offset, actually */
		flipscreen = spriteram.read(0x07f6)& 0x01;
		tilemap_set_flip(ALL_TILEMAPS,flipscreen!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
	
		/*TODO*///tilemap_draw(bitmap,tilemap[1],TILEMAP_IGNORE_TRANSPARENCY,0);
                tilemap_draw(bitmap,tilemap[1],TILEMAP_IGNORE_TRANSPARENCY);
		draw_sprites(bitmap,0);
		/*TODO*///tilemap_draw(bitmap,tilemap[0],0,0);
                tilemap_draw(bitmap,tilemap[0],0);
		draw_sprites(bitmap,1);
	
		for (offs = 0x400 - 1; offs > 0; offs--)
		{
			int mx,my,sx,sy;
	
	        mx = offs % 32;
			my = offs / 32;
	
			if (my < 2)
			{
				if (mx < 2 || mx >= 30) continue; /* not visible */
				sx = my + 34; sy = mx - 2;
			}
			else if (my >= 30)
			{
				if (mx < 2 || mx >= 30) continue; /* not visible */
				sx = my - 30; sy = mx - 2;
			}
			else
			{
				sx = mx + 2; sy = my - 2;
			}
			if (flipscreen != 0)
			{
					sx = 35 - sx; sy = 27 - sy;
			}
	
			drawgfx(bitmap,Machine.gfx[0],	baraduke_textram.read(offs),
					(baraduke_textram.read(offs+0x400) << 2) & 0x1ff,
					flipscreen,flipscreen,sx*8,sy*8,
					Machine.visible_area,TRANSPARENCY_PEN,3);
		}
	} };
	
	public static VhUpdatePtr metrocrs_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
		/* this is the global sprite Y offset, actually */
		flipscreen = spriteram.read(0x07f6)& 0x01;
		tilemap_set_flip(ALL_TILEMAPS,flipscreen!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
	
		/*TODO*///tilemap_draw(bitmap,tilemap[0],TILEMAP_IGNORE_TRANSPARENCY,0);
                tilemap_draw(bitmap,tilemap[0],TILEMAP_IGNORE_TRANSPARENCY);
		draw_sprites(bitmap,0);
		/*TODO*///tilemap_draw(bitmap,tilemap[1],0,0);
                tilemap_draw(bitmap,tilemap[1],0);
		draw_sprites(bitmap,1);
		for (offs = 0x400 - 1; offs > 0; offs--)
		{
			int mx,my,sx,sy;
	
	        mx = offs % 32;
			my = offs / 32;
	
			if (my < 2)
			{
				if (mx < 2 || mx >= 30) continue; /* not visible */
				sx = my + 34; sy = mx - 2;
			}
			else if (my >= 30)
			{
				if (mx < 2 || mx >= 30) continue; /* not visible */
				sx = my - 30; sy = mx - 2;
			}
			else
			{
				sx = mx + 2; sy = my - 2;
			}
			if (flipscreen != 0)
			{
					sx = 35 - sx; sy = 27 - sy;
			}
			drawgfx(bitmap,Machine.gfx[0],	baraduke_textram.read(offs),
					(baraduke_textram.read(offs+0x400) << 2) & 0x1ff,
					flipscreen,flipscreen,sx*8,sy*8,
					Machine.visible_area,TRANSPARENCY_PEN,3);
		}
	} };
}
