/***************************************************************************

  video hardware for Tecmo games

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

public class tecmo
{
	
	public static UBytePtr tecmo_txvideoram = new UBytePtr(), tecmo_fgvideoram = new UBytePtr(), tecmo_bgvideoram = new UBytePtr();
	
	public static int tecmo_video_type = 0;
	/*
	   video_type is used to distinguish Rygar, Silkworm and Gemini Wing.
	   This is needed because there is a difference in the tile and sprite indexing.
	*/
	
	static struct_tilemap tx_tilemap, fg_tilemap, bg_tilemap;
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                int attr = tecmo_bgvideoram.read(tile_index+0x200);
		SET_TILE_INFO(
				3,
				tecmo_bgvideoram.read(tile_index) + ((attr & 0x07) << 8),
				attr >> 4,
				0);
            }
        };
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = tecmo_fgvideoram.read(tile_index+0x200);
		SET_TILE_INFO(
				2,
				tecmo_fgvideoram.read(tile_index) + ((attr & 0x07) << 8),
				attr >> 4,
				0);
	}};
	
	static GetTileInfoPtr gemini_get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = tecmo_bgvideoram.read(tile_index+0x200);
		SET_TILE_INFO(
				3,
				tecmo_bgvideoram.read(tile_index) + ((attr & 0x70) << 4),
				attr & 0x0f,
				0);
	}};
	
	static GetTileInfoPtr gemini_get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = tecmo_fgvideoram.read(tile_index+0x200);
		SET_TILE_INFO(
				2,
				tecmo_fgvideoram.read(tile_index) + ((attr & 0x70) << 4),
				attr & 0x0f,
				0);
	}};
	
	static GetTileInfoPtr get_tx_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = tecmo_txvideoram.read(tile_index+0x400);
		SET_TILE_INFO(
				0,
				tecmo_txvideoram.read(tile_index) + ((attr & 0x03) << 8),
				attr >> 4,
				0);
	}};
	
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr tecmo_vh_start = new VhStartPtr() { public int handler() 
	{
		if (tecmo_video_type == 2)	/* gemini */
		{
			bg_tilemap = tilemap_create(gemini_get_bg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,16,16,32,16);
			fg_tilemap = tilemap_create(gemini_get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,16,16,32,16);
		}
		else	/* rygar, silkworm */
		{
			bg_tilemap = tilemap_create(get_bg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,16,16,32,16);
			fg_tilemap = tilemap_create(get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,16,16,32,16);
		}
		tx_tilemap = tilemap_create(get_tx_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT, 8, 8,32,32);
	
		if (bg_tilemap==null || fg_tilemap==null || tx_tilemap==null)
			return 1;
	
		/*TODO*///tilemap_set_transparent_pen(bg_tilemap,0);
                bg_tilemap.transparent_pen = 0;
		/*TODO*///tilemap_set_transparent_pen(fg_tilemap,0);
                fg_tilemap.transparent_pen = 0;
		/*TODO*///tilemap_set_transparent_pen(tx_tilemap,0);
                tx_tilemap.transparent_pen = 0;
	
		tilemap_set_scrolldx(bg_tilemap,-48,256+48);
		tilemap_set_scrolldx(fg_tilemap,-48,256+48);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr tecmo_txvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (tecmo_txvideoram.read(offset) != data)
		{
			tecmo_txvideoram.write(offset, data);
			tilemap_mark_tile_dirty(tx_tilemap,offset & 0x3ff);
		}
	} };
	
	public static WriteHandlerPtr tecmo_fgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (tecmo_fgvideoram.read(offset) != data)
		{
			tecmo_fgvideoram.write(offset, data);
			tilemap_mark_tile_dirty(fg_tilemap,offset & 0x1ff);
		}
	} };
	
	public static WriteHandlerPtr tecmo_bgvideoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (tecmo_bgvideoram.read(offset) != data)
		{
			tecmo_bgvideoram.write(offset, data);
			tilemap_mark_tile_dirty(bg_tilemap,offset & 0x1ff);
		}
	} };
        
        static int[] scroll = new int[3];
	
	public static WriteHandlerPtr tecmo_fgscroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll[offset] = data;
	
		tilemap_set_scrollx(fg_tilemap,0,scroll[0] + 256 * scroll[1]);
		tilemap_set_scrolly(fg_tilemap,0,scroll[2]);
	} };
	
	public static WriteHandlerPtr tecmo_bgscroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		scroll[offset] = data;
	
		tilemap_set_scrollx(bg_tilemap,0,scroll[0] + 256 * scroll[1]);
		tilemap_set_scrolly(bg_tilemap,0,scroll[2]);
	} };
	
	public static WriteHandlerPtr tecmo_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		flip_screen_set(data & 1);
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int offs;
		int layout[][] =
		{
			{0,1,4,5,16,17,20,21},
			{2,3,6,7,18,19,22,23},
			{8,9,12,13,24,25,28,29},
			{10,11,14,15,26,27,30,31},
			{32,33,36,37,48,49,52,53},
			{34,35,38,39,50,51,54,55},
			{40,41,44,45,56,57,60,61},
			{42,43,46,47,58,59,62,63}
		};
	
		for (offs = spriteram_size[0]-8;offs >= 0;offs -= 8)
		{
			int flags = spriteram.read(offs+3);
			int priority = flags>>6;
			int bank = spriteram.read(offs+0);
			if ((bank & 4) != 0)
			{ /* visible */
				int which = spriteram.read(offs+1);
				int code,xpos,ypos,flipx,flipy,priority_mask,x,y;
				int size = spriteram.read(offs + 2)& 3;
	
				if (tecmo_video_type != 0)	/* gemini, silkworm */
				  code = which + ((bank & 0xf8) << 5);
				else						/* rygar */
				  code = which + ((bank & 0xf0) << 4);
	
				code &= ~((1 << (size*2)) - 1);
				size = 1 << size;
	
				xpos = spriteram.read(offs + 5)- ((flags & 0x10) << 4);
				ypos = spriteram.read(offs + 4)- ((flags & 0x20) << 3);
				flipx = bank & 1;
				flipy = bank & 2;
	
				if (flip_screen() != 0)
				{
					xpos = 256 - (8 * size) - xpos;
					ypos = 256 - (8 * size) - ypos;
					flipx = flipx!=0?0:1;
					flipy = flipy!=0?0:1;
				}
	
				/* bg: 1; fg:2; text: 4 */
				switch (priority)
				{
					default:
					case 0x0: priority_mask = 0; break;
					case 0x1: priority_mask = 0xf0; break; /* obscured by text layer */
					case 0x2: priority_mask = 0xf0|0xcc; break;	/* obscured by foreground */
					case 0x3: priority_mask = 0xf0|0xcc|0xaa; break; /* obscured by bg and fg */
				}
	
				for (y = 0;y < size;y++)
				{
					for (x = 0;x < size;x++)
					{
						int sx = xpos + 8*(flipx!=0?(size-1-x):x);
						int sy = ypos + 8*(flipy!=0?(size-1-y):y);
						pdrawgfx(bitmap,Machine.gfx[1],
								code + layout[y][x],
								flags & 0xf,
								flipx,flipy,
								sx,sy,
								Machine.visible_area,TRANSPARENCY_PEN,0,
								priority_mask);
					}
				}
			}
		}
	}
	
	
	public static VhUpdatePtr tecmo_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		fillbitmap(priority_bitmap,0,null);
		fillbitmap(bitmap,Machine.pens[0x100],Machine.visible_area);
		/*TODO*///tilemap_draw(bitmap,bg_tilemap,0,1);
                tilemap_draw(bitmap,bg_tilemap,1);
		/*TODO*///tilemap_draw(bitmap,fg_tilemap,0,2);
                tilemap_draw(bitmap,fg_tilemap,2);
		/*TODO*///tilemap_draw(bitmap,tx_tilemap,0,4);
                tilemap_draw(bitmap,tx_tilemap,4);
	
		draw_sprites(bitmap);
                
                // HACK - ONLY for tilemaps 0.37. REMOVE in 0.56
                tilemap_update(ALL_TILEMAPS);	
		tilemap_render(ALL_TILEMAPS);
                // END HACK
	} };
}
