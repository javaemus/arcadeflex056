/*******************************************************************

Rolling Thunder Video Hardware

*******************************************************************/

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

public class namcos86
{
	
	public static int GFX_TILES1    = 0;
	public static int GFX_TILES2    = 1;
	public static int GFX_SPRITES   = 2;
	
	public static UBytePtr rthunder_videoram1=new UBytePtr(), rthunder_videoram2=new UBytePtr();
	
	static int tilebank;
	static int[] xscroll=new int[4], yscroll=new int[4];	/* scroll + priority */
	
	static struct_tilemap[] tilemap = new struct_tilemap[4];
	
	static int backcolor;
	static int flipscreen;
	static UBytePtr tile_address_prom = new UBytePtr();
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Rolling Thunder has two palette PROMs (512x8 and 512x4) and two 2048x8
	  lookup table PROMs.
	  The palette PROMs are connected to the RGB output this way:
	
	  bit 3 -- 220 ohm resistor  -- BLUE
	        -- 470 ohm resistor  -- BLUE
	        -- 1  kohm resistor  -- BLUE
	  bit 0 -- 2.2kohm resistor  -- BLUE
	
	  bit 7 -- 220 ohm resistor  -- GREEN
	        -- 470 ohm resistor  -- GREEN
	        -- 1  kohm resistor  -- GREEN
	        -- 2.2kohm resistor  -- GREEN
	        -- 220 ohm resistor  -- RED
	        -- 470 ohm resistor  -- RED
	        -- 1  kohm resistor  -- RED
	  bit 0 -- 2.2kohm resistor  -- RED
	
	***************************************************************************/
	
	public static VhConvertColorPromPtr namcos86_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
		int totcolors,totlookup;
                int _palette = 0;
	
		totcolors = Machine.drv.total_colors;
		totlookup = Machine.drv.color_table_len;
	
		for (i = 0;i < totcolors;i++)
		{
			int bit0,bit1,bit2,bit3;
	
	
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
			bit0 = (color_prom.read(totcolors)>> 0) & 0x01;
			bit1 = (color_prom.read(totcolors)>> 1) & 0x01;
			bit2 = (color_prom.read(totcolors)>> 2) & 0x01;
			bit3 = (color_prom.read(totcolors)>> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
	
			color_prom.inc();
		}
	
		color_prom.inc(totcolors);
		/* color_prom now points to the beginning of the lookup table */
                int _colortable = 0;
		/* tiles lookup table */
		for (i = 0;i < totlookup/2;i++)
			colortable[_colortable++] = color_prom.readinc();
	
		/* sprites lookup table */
		for (i = 0;i < totlookup/2;i++)
			colortable[_colortable++] = (char) (color_prom.readinc() + totcolors/2);
	
		/* color_prom now points to the beginning of the tile address decode PROM */
	
		tile_address_prom = color_prom;	/* we'll need this at run time */
            }
        };
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	public static void get_tile_info(int tile_index,int layer,UBytePtr vram)
	{
		int attr = vram.read(2*tile_index + 1);
		int tile_offs;
		if ((layer & 2) != 0)
			tile_offs = ((tile_address_prom.read(((layer & 1) << 4) + (attr & 0x03)) & 0xe0) >> 5) * 0x100;
		else
			tile_offs = ((tile_address_prom.read(((layer & 1) << 4) + ((attr & 0x03) << 2)) & 0x0e) >> 1) * 0x100 + tilebank * 0x800;
	
		SET_TILE_INFO(
				(layer & 2)!=0 ? GFX_TILES2 : GFX_TILES1,
				vram.read(2*tile_index) + tile_offs,
				attr,
				0);
	}
	
	static GetTileInfoPtr get_tile_info0 = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                get_tile_info(tile_index,0,new UBytePtr(rthunder_videoram1, 0x0000));
            }
        }; 
	static GetTileInfoPtr get_tile_info1 = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                get_tile_info(tile_index,1,new UBytePtr(rthunder_videoram1, 0x1000));
            }
        };
	static GetTileInfoPtr get_tile_info2 = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                get_tile_info(tile_index,2,new UBytePtr(rthunder_videoram2, 0x0000));
            } 
        };
	static GetTileInfoPtr get_tile_info3 = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                get_tile_info(tile_index,3,new UBytePtr(rthunder_videoram2, 0x1000));
            } 
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr namcos86_vh_start = new VhStartPtr() { public int handler() 
	{
		tilemap[0] = tilemap_create(get_tile_info0,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);
		tilemap[1] = tilemap_create(get_tile_info1,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);
		tilemap[2] = tilemap_create(get_tile_info2,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);
		tilemap[3] = tilemap_create(get_tile_info3,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,64,32);
	
		if (tilemap[0]==null || tilemap[1]==null || tilemap[2]==null || tilemap[3]==null)
			return 1;
	
		/*TODO*///tilemap_set_transparent_pen(tilemap[0],7);
                tilemap[0].transparent_pen=7;
		//tilemap_set_transparent_pen(tilemap[1],7);
                tilemap[1].transparent_pen=7;
		//tilemap_set_transparent_pen(tilemap[2],7);
                tilemap[2].transparent_pen=7;
		//tilemap_set_transparent_pen(tilemap[3],7);
                tilemap[3].transparent_pen=7;
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static ReadHandlerPtr rthunder_videoram1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return rthunder_videoram1.read(offset);
	} };
	
	public static WriteHandlerPtr rthunder_videoram1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (rthunder_videoram1.read(offset) != data)
		{
			rthunder_videoram1.write(offset, data);
			tilemap_mark_tile_dirty(tilemap[offset/0x1000],(offset & 0xfff)/2);
		}
	} };
	
	public static ReadHandlerPtr rthunder_videoram2_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return rthunder_videoram2.read(offset);
	} };
	
	public static WriteHandlerPtr rthunder_videoram2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (rthunder_videoram2.read(offset) != data)
		{
			rthunder_videoram2.write(offset, data);
			tilemap_mark_tile_dirty(tilemap[2+offset/0x1000],(offset & 0xfff)/2);
		}
	} };
	
	public static WriteHandlerPtr rthunder_tilebank_select_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (tilebank != 0)
		{
			tilebank = 0;
			tilemap_mark_all_tiles_dirty(tilemap[0]);
			tilemap_mark_all_tiles_dirty(tilemap[1]);
		}
	} };
	
	public static WriteHandlerPtr rthunder_tilebank_select_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (tilebank != 1)
		{
			tilebank = 1;
			tilemap_mark_all_tiles_dirty(tilemap[0]);
			tilemap_mark_all_tiles_dirty(tilemap[1]);
		}
	} };
	
	static void scroll_w(int layer,int offset,int data)
	{
		int xdisp[] = { 36,34,37,35 };
		int ydisp = 9;
		int scrollx,scrolly;
	
	
		switch (offset)
		{
			case 0:
				xscroll[layer] = (xscroll[layer]&0xff)|(data<<8);
				break;
			case 1:
				xscroll[layer] = (xscroll[layer]&0xff00)|data;
				break;
			case 2:
				yscroll[layer] = data;
				break;
		}
	
		scrollx = xscroll[layer]+xdisp[layer];
		scrolly = yscroll[layer]+ydisp;
		if (flipscreen != 0)
		{
			scrollx = -scrollx+256;
			scrolly = -scrolly;
		}
		tilemap_set_scrollx(tilemap[layer],0,scrollx-16);
		tilemap_set_scrolly(tilemap[layer],0,scrolly+16);
	}
	
	public static WriteHandlerPtr rthunder_scroll0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll_w(0,offset,data);
	} };
	public static WriteHandlerPtr rthunder_scroll1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll_w(1,offset,data);
	} };
	public static WriteHandlerPtr rthunder_scroll2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll_w(2,offset,data);
	} };
	public static WriteHandlerPtr rthunder_scroll3_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll_w(3,offset,data);
	} };
	
	
	public static WriteHandlerPtr rthunder_backcolor_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		backcolor = data;
	} };
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites( mame_bitmap bitmap, int sprite_priority )
	{
		/* note: sprites don't yet clip at the top of the screen properly */
		rectangle clip = new rectangle(Machine.visible_area);
	
		UBytePtr source = new UBytePtr (spriteram, 0x1400);
		UBytePtr finish = new UBytePtr (spriteram, 0x1c00-16);	/* the last is NOT a sprite */
	
		int sprite_xoffs = spriteram.read(0x1bf5)- 256 * (spriteram.read(0x1bf4)& 1);
		int sprite_yoffs = spriteram.read(0x1bf7)- 256 * (spriteram.read(0x1bf6)& 1);
	
		while( source.offset<finish.offset )
		{
	/*
		source[4]	S-FT -BBB
		source[5]	TTTT TTTT
		source[6]   CCCC CCCX
		source[7]	XXXX XXXX
		source[8]	PPPT -S-F
		source[9]   YYYY YYYY
	*/
			int priority = source.read(8);
			if( priority>>5 == sprite_priority )
			{
				int attrs = source.read(4);
				int color = source.read(6);
				int sx = source.read(7) + (color&1)*256; /* need adjust for left clip */
				int sy = -source.read(9);
				int flipx = attrs&0x20;
				int flipy = priority & 0x01;
				int tall = (priority&0x04)!=0?1:0;
				int wide = (attrs&0x80)!=0?1:0;
				int sprite_bank = attrs&7;
				int sprite_number = (source.read(5)&0xff)*4;
				int row,col;
	
				if ((attrs & 0x10)!=0 && wide==0) sprite_number += 1;
				if ((priority & 0x10)!=0 && tall==0) sprite_number += 2;
				color = color>>1;
	
				if (sx>512-32) sx -= 512;
				if (sy < -209-32) sy += 256;
	
				if (flipx!=0 && wide==0) sx-=16;
				if (tall == 0) sy+=16;
	//			if (flipy && !tall) sy+=16;
	
				sx += sprite_xoffs;
				sy -= sprite_yoffs;
	
				for( row=0; row<=tall; row++ )
				{
					for( col=0; col<=wide; col++ )
					{
						if (flipscreen != 0)
						{
							drawgfx( bitmap, Machine.gfx[GFX_SPRITES+sprite_bank],
								sprite_number+2*row+col,
								color,
								flipx!=0?0:1,flipy!=0?0:1,
								512-16-67 - (sx+16*(flipx!=0?1-col:col)),
								64-16+209 - (sy+16*(flipy!=0?1-row:row)),
								clip,
								TRANSPARENCY_PEN, 0xf );
						}
						else
						{
							drawgfx( bitmap, Machine.gfx[GFX_SPRITES+sprite_bank],
								sprite_number+2*row+col,
								color,
								flipx,flipy,
								-67 + (sx+16*(flipx!=0?1-col:col)),
								209 + (sy+16*(flipy!=0?1-row:row)),
								clip,
								TRANSPARENCY_PEN, 0xf );
						}
					}
				}
			}
			source.inc(16);
		}
	}
	
	
	
	public static VhUpdatePtr namcos86_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int layer;
	
		/* this is the global sprite Y offset, actually */
		flipscreen = spriteram.read(0x1bf6)& 1;
	
		tilemap_set_flip(ALL_TILEMAPS,flipscreen!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
	
		fillbitmap(bitmap,Machine.gfx[0].colortable.read(8*backcolor+7),Machine.visible_area);
	
		for (layer = 0;layer < 8;layer++)
		{
			int i;
	
			for (i = 3;i >= 0;i--)
			{
				if (((xscroll[i] & 0x0e00) >> 9) == layer){
					/*TODO*///tilemap_draw(bitmap,tilemap[i],0,0);
                                        tilemap_draw(bitmap,tilemap[i],0);
                                }
			}
	
			draw_sprites(bitmap,layer);
		}
                
                // HACK - ONLY for tilemaps 0.37. REMOVE in 0.56
                tilemap_update(ALL_TILEMAPS);	
		tilemap_render(ALL_TILEMAPS);
                // END HACK
	/*#if 0
	{
		char buf[80];
	int b=keyboard_pressed(KEYCODE_Y)?8:0;
		sprintf(buf,"%02x %02x %02x %02x %02x %02x %02x %02x",
				spriteram.read(0x1bf0+b),
				spriteram.read(0x1bf1+b),
				spriteram.read(0x1bf2+b),
				spriteram.read(0x1bf3+b),
				spriteram.read(0x1bf4+b),
				spriteram.read(0x1bf5+b),
				spriteram.read(0x1bf6+b),
				spriteram.read(0x1bf7+b));
		usrintf_showmessage(buf);
	}
	#endif*/
	} };
}
