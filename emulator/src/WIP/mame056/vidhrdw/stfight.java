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
import static common.ptr.*;
import static mame056.tilemapH.*;
//import static mame056.tilemapC.*;
import static mame037b11.mame.tilemapC.*;
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
import static WIP.mame056.machine.stfight.*;

public class stfight
{
	
	// Real stuff
	public static UBytePtr stfight_text_char_ram = new UBytePtr();
	public static UBytePtr stfight_text_attr_ram = new UBytePtr();
	public static UBytePtr stfight_vh_latch_ram = new UBytePtr();
	public static UBytePtr stfight_sprite_ram = new UBytePtr();
	
	static struct_tilemap fg_tilemap, bg_tilemap, tx_tilemap;
        
	static int stfight_sprite_base = 0;
	
	/*
			Graphics ROM Format
			===================
	
			Each tile is 8x8 pixels
			Each composite tile is 2x2 tiles, 16x16 pixels
			Each screen is 32x32 composite tiles, 64x64 tiles, 256x256 pixels
			Each layer is a 4-plane bitmap 8x16 screens, 2048x4096 pixels
	
			There are 4x256=1024 composite tiles defined for each layer
	
			Each layer is mapped using 2 bytes/composite tile
			- one byte for the tile
			- one byte for the tile bank, attribute
				- b7,b5     tile bank (0-3)
	
			Each pixel is 4 bits = 16 colours.
	
	 */
        
        public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
	
        public static void COLOR(char []colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs]) = (char) value;
        }
	
	public static VhConvertColorPromPtr stfight_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
                int _palette = 0;
		
		/* unique color for transparency */
		palette[256*3+0] = 0x04;
		palette[256*3+1] = 0x04;
		palette[256*3+2] = 0x04;
	
		/* text uses colors 192-207 */
		for (i = 0;i < TOTAL_COLORS(0);i++)
		{
			if ((color_prom.read() & 0x0f) == 0x0f) COLOR(colortable,0,i,256);	/* transparent */
			else COLOR(colortable,0,i,(color_prom.read() & 0x0f) + 0xc0);
			color_prom.inc();
		}
		color_prom.inc( 256 - TOTAL_COLORS(0));	/* rest of the PROM is unused */
	
		/* fg uses colors 64-127 */
		for (i = 0;i < TOTAL_COLORS(1);i++)
		{
			COLOR(colortable,1,i, (color_prom.read(256)& 0x0f) + 16 * (color_prom.read(0)& 0x03) + 0x40);
			color_prom.inc();
		}
		color_prom.inc( 256 );
	
		/* bg uses colors 0-63 */
		for (i = 0;i < TOTAL_COLORS(2);i++)
		{
			COLOR(colortable,2,i, (color_prom.read(256)& 0x0f) + 16 * (color_prom.read(0)& 0x03) + 0x00);
			color_prom.inc();
		}
		color_prom.inc( 256 );
	
		/* sprites use colors 128-191 */
		for (i = 0;i < TOTAL_COLORS(4);i++)
		{
			COLOR(colortable,4,i, (color_prom.read(256)& 0x0f) + 16 * (color_prom.read(0)& 0x03) + 0x80);
			color_prom.inc();
		}
		color_prom.inc( 256 );
	} };
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetMemoryOffsetPtr fg_scan = new GetMemoryOffsetPtr() {
            public int handler(int col,int row,int num_cols,int num_rows) {
		/* logical (col,row) . memory offset */
		return (col & 0x0f) + ((row & 0x0f) << 4) + ((col & 0x70) << 4) + ((row & 0xf0) << 7);
            }
        };
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		UBytePtr fgMap = new UBytePtr(memory_region(REGION_GFX5));
		int attr,tile_base;
	
		attr = fgMap.read(0x8000+tile_index);
		tile_base = ((attr & 0x80) << 2) | ((attr & 0x20) << 3);
	
		SET_TILE_INFO(
				1,
				tile_base + fgMap.read(tile_index),
				attr & 0x07,
				0);
            }
        };
	
	static GetMemoryOffsetPtr bg_scan = new GetMemoryOffsetPtr() {
            public int handler(int col,int row,int num_cols,int num_rows) {
                /* logical (col,row) . memory offset */
		return ((col & 0x0e) >> 1) + ((row & 0x0f) << 3) + ((col & 0x70) << 3) +
				((row & 0x80) << 3) + ((row & 0x10) << 7) + ((col & 0x01) << 12) +
				((row & 0x60) << 8);
            }
        };
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                UBytePtr bgMap = new UBytePtr(memory_region(REGION_GFX6));
		int attr,tile_bank,tile_base;
	
		attr = bgMap.read(0x8000+tile_index);
		tile_bank = (attr & 0x20) >> 5;
		tile_base = (attr & 0x80) << 1;
	
		SET_TILE_INFO(
				2+tile_bank,
				tile_base + bgMap.read(tile_index),
				attr & 0x07,
				0);
            }
        };
	
	static GetTileInfoPtr get_tx_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = stfight_text_attr_ram.read(tile_index);
	
		SET_TILE_INFO(
				0,
				stfight_text_char_ram.read(tile_index) + ((attr & 0x80) << 1),
				attr & 0x0f,
				TILE_FLIPYX((attr & 0x60) >> 5));
            }
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr stfight_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(get_bg_tile_info,bg_scan,TILEMAP_OPAQUE,     16,16,128,256);
		fg_tilemap = tilemap_create(get_fg_tile_info,fg_scan,TILEMAP_TRANSPARENT,16,16,128,256);
		tx_tilemap = tilemap_create(get_tx_tile_info,tilemap_scan_rows,
				TILEMAP_TRANSPARENT_COLOR,8,8,32,32);
	
		if (fg_tilemap==null || bg_tilemap==null || tx_tilemap==null)
			return 1;
	
		/*TODO*///tilemap_set_transparent_pen(fg_tilemap,0x0F);
                fg_tilemap.transparent_pen = 0x0F;
		/*TODO*///tilemap_set_transparent_pen(tx_tilemap,256);
                tx_tilemap.transparent_pen = 256;
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr stfight_text_char_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (stfight_text_char_ram.read(offset) != data)
		{
			stfight_text_char_ram.write(offset, data);
			tilemap_mark_tile_dirty(tx_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr stfight_text_attr_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (stfight_text_attr_ram.read(offset) != data)
		{
			stfight_text_attr_ram.write(offset, data);
			tilemap_mark_tile_dirty(tx_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr stfight_sprite_bank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		stfight_sprite_base = ( ( data & 0x04 ) << 7 ) |
					          ( ( data & 0x01 ) << 8 );
	} };
	
	public static WriteHandlerPtr stfight_vh_latch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int scroll;
	
	
		stfight_vh_latch_ram.write(offset, data);
	
		switch( offset )
		{
			case 0x00:
			case 0x01:
				scroll = (stfight_vh_latch_ram.read(1) << 8) | stfight_vh_latch_ram.read(0);
				tilemap_set_scrollx(fg_tilemap,0,scroll);
				break;
	
			case 0x02:
			case 0x03:
				scroll = (stfight_vh_latch_ram.read(3) << 8) | stfight_vh_latch_ram.read(2);
				tilemap_set_scrolly(fg_tilemap,0,scroll);
				break;
	
			case 0x04:
			case 0x05:
				scroll = (stfight_vh_latch_ram.read(5) << 8) | stfight_vh_latch_ram.read(4);
				tilemap_set_scrollx(bg_tilemap,0,scroll);
				break;
	
			case 0x06:
			case 0x08:
				scroll = (stfight_vh_latch_ram.read(8) << 8) | stfight_vh_latch_ram.read(6);
				tilemap_set_scrolly(bg_tilemap,0,scroll);
				break;
	
			case 0x07:
				tilemap_set_enable(tx_tilemap,data & 0x80);
				/* 0x40 = sprites */
				tilemap_set_enable(bg_tilemap,data & 0x20);
				tilemap_set_enable(fg_tilemap,data & 0x10);
				flip_screen_set(data & 0x01);
				break;
		}
	} };
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int offs,sx,sy;
	
		for (offs = 0;offs < 4096;offs += 32)
		{
			int code;
			int attr = stfight_sprite_ram.read(offs+1);
			int flipx = attr & 0x10;
			int color = attr & 0x0f;
			int pri = (attr & 0x20) >> 5;
	
			sy = stfight_sprite_ram.read(offs+2);
			sx = stfight_sprite_ram.read(offs+3);
	
			// non-active sprites have zero y coordinate value
			if( sy > 0 )
			{
				// sprites which wrap onto/off the screen have
				// a sign extension bit in the sprite attribute
				if( sx >= 0xf0 )
				{
					if ((attr & 0x80) != 0)
					    sx -= 0x100;
				}
	
				if ((flip_screen()) != 0)
				{
					sx = 240 - sx;
					sy = 240 - sy;
					flipx = flipx!=0?0:1;
				}
	
				code = stfight_sprite_base + stfight_sprite_ram.read(offs);
	
				pdrawgfx(bitmap,Machine.gfx[4],
					     code,
						 color,
						 flipx,flip_screen(),
						 sx,sy,
                                                 Machine.visible_area,TRANSPARENCY_PEN,0x0f,
						 pri!=0 ? 0x02 : 0);
			}
		}
	}
	
	
	public static VhUpdatePtr stfight_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		fillbitmap(priority_bitmap,0,null);
	
		fillbitmap(bitmap,Machine.pens[0],new rectangle(Machine.visible_area));	/* in case bg_tilemap is disabled */
                /*TODO*///tilemap_draw(bitmap,bg_tilemap,0,0);
                tilemap_draw(bitmap,bg_tilemap,0);
		/*TODO*///tilemap_draw(bitmap,fg_tilemap,0,1);
                tilemap_draw(bitmap,fg_tilemap,1);
	
		/* Draw sprites (may be obscured by foreground layer) */
		if ((stfight_vh_latch_ram.read(0x07) & 0x40) != 0)
			draw_sprites(bitmap);
	
		/*TODO*///tilemap_draw(bitmap,tx_tilemap,0,0);
                tilemap_draw(bitmap,tx_tilemap,0);
                
                // HACK - ONLY for tilemaps 0.37. REMOVE in 0.56
                tilemap_update(ALL_TILEMAPS);	
		tilemap_render(ALL_TILEMAPS);
                // END HACK
	} };
}
