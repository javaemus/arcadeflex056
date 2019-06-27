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

public class commando
{
	
	
	
	public static UBytePtr commando_fgvideoram=new UBytePtr(), commando_bgvideoram=new UBytePtr();
	
	static struct_tilemap fg_tilemap, bg_tilemap;
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	public static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                int code, color;
	
		code = commando_fgvideoram.read(tile_index);
		color = commando_fgvideoram.read(tile_index + 0x400);
		SET_TILE_INFO(
				0,
				code + ((color & 0xc0) << 2),
				color & 0x0f
                                ,TILE_FLIPYX((color & 0x30) >> 4)
                );
                tile_info.flags = TILE_FLIPYX((color & 0x30) >> 4);
            }
        };
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                int code, color;
	
		code = commando_bgvideoram.read(tile_index);
		color = commando_bgvideoram.read(tile_index + 0x400);
		SET_TILE_INFO(
				1,
				code + ((color & 0xc0) << 2),
				color & 0x0f
				,TILE_FLIPYX((color & 0x30) >> 4)
                );
                tile_info.flags = TILE_FLIPYX((color & 0x30) >> 4);
            }
        };
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr commando_vh_start = new VhStartPtr() { public int handler() 
	{
		fg_tilemap = tilemap_create(get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT, 8, 8,32,32);
		bg_tilemap = tilemap_create(get_bg_tile_info,tilemap_scan_cols,TILEMAP_OPAQUE,     16,16,32,32);
	
		if (fg_tilemap==null || bg_tilemap==null)
			return 1;
	
		//tilemap_set_transparent_pen(fg_tilemap,3);
                fg_tilemap.transparent_pen = 3;
	
		return 0;
	} };
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr commando_fgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		commando_fgvideoram.write(offset, data);
		tilemap_mark_tile_dirty(fg_tilemap,offset & 0x3ff);
	} };
	
	public static WriteHandlerPtr commando_bgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		commando_bgvideoram.write(offset, data);
		tilemap_mark_tile_dirty(bg_tilemap,offset & 0x3ff);
	} };
	
	static int[] scroll=new int[2];
        
	public static WriteHandlerPtr commando_scrollx_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll[offset] = data;
		tilemap_set_scrollx(bg_tilemap,0,scroll[0] | (scroll[1] << 8));
	} };
	
	public static WriteHandlerPtr commando_scrolly_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll[offset] = data;
		tilemap_set_scrolly(bg_tilemap,0,scroll[0] | (scroll[1] << 8));
	} };
	
	
	public static WriteHandlerPtr commando_c804_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bits 0 and 1 are coin counters */
		coin_counter_w.handler(0, data & 0x01);
		coin_counter_w.handler(1, data & 0x02);
	
		/* bit 4 resets the sound CPU */
		cpu_set_reset_line(1,(data & 0x10)!=0 ? ASSERT_LINE : CLEAR_LINE);
	
		/* bit 7 flips screen */
		flip_screen_set(~data & 0x80);
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
		for (offs = spriteram_size[0] - 4;offs >= 0;offs -= 4)
		{
			int sx,sy,flipx,flipy,bank,attr;
	
	
			/* bit 1 of attr is not used */
			attr = buffered_spriteram.read(offs + 1);
			sx = buffered_spriteram.read(offs + 3) - ((attr & 0x01) << 8);
			sy = buffered_spriteram.read(offs + 2);
			flipx = attr & 0x04;
			flipy = attr & 0x08;
			bank = (attr & 0xc0) >> 6;
	
			if (flip_screen() != 0)
			{
				sx = 240 - sx;
				sy = 240 - sy;
				flipx = flipx!=0?0:1;
				flipy = flipy!=0?0:1;
			}
	
			if (bank < 3)
				drawgfx(bitmap,Machine.gfx[2],
						buffered_spriteram.read(offs) + 256 * bank,
						(attr & 0x30) >> 4,
						flipx,flipy,
						sx,sy,
						Machine.visible_area,TRANSPARENCY_PEN,15);
		}
	}
	
	public static VhUpdatePtr commando_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		
                tilemap_draw(bitmap,bg_tilemap,0);
		draw_sprites(bitmap);
		tilemap_draw(bitmap,fg_tilemap,0);
                
                tilemap_update(ALL_TILEMAPS);	
		tilemap_render(ALL_TILEMAPS);
	} };
	
	public static VhEofCallbackPtr commando_eof_callback = new VhEofCallbackPtr() {
            public void handler() {
                buffer_spriteram_w.handler(0,0);
            }
        };
	
}

