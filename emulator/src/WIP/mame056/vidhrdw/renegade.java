/***************************************************************************

	Renegade Video Hardware

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;

import static common.ptr.*;

import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
/*TODO*///import static mame056.tilemapC.*;
import static mame056.tilemapH.*;
import static mame037b11.mame.tilemapC.*;

public class renegade
{
	
	public static UBytePtr renegade_textram = new UBytePtr();
	static int renegade_scrollx;
	static struct_tilemap bg_tilemap;
	static struct_tilemap fg_tilemap;
	static int flipscreen;
	
	public static WriteHandlerPtr renegade_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( videoram.read(offset)!=data )
		{
			videoram.write(offset, data);
			offset = offset%(64*16);
			tilemap_mark_tile_dirty(bg_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr renegade_textram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( renegade_textram.read(offset)!=data )
		{
			renegade_textram.write(offset, data);
			offset = offset%(32*32);
			tilemap_mark_tile_dirty(fg_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr renegade_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		flipscreen = data!=0?0:1;
		tilemap_set_flip( ALL_TILEMAPS, flipscreen!=0?(TILEMAP_FLIPY|TILEMAP_FLIPX):0);
	} };
	
	public static WriteHandlerPtr renegade_scroll0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		renegade_scrollx = (renegade_scrollx&0xff00)|data;
	} };
	
	public static WriteHandlerPtr renegade_scroll1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		renegade_scrollx = (renegade_scrollx&0xFF)|(data<<8);
	} };
	
	static GetTileInfoPtr get_bg_tilemap_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                UBytePtr source = new UBytePtr(videoram, tile_index);
		int attributes = source.read(0x400); /* CCC??BBB */
		SET_TILE_INFO(
				1+(attributes&0x7),
				source.read(0),
				attributes>>5,
				0);
                tile_info.flags = 0;
            }
        };
	
	static GetTileInfoPtr get_fg_tilemap_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                UBytePtr source = new UBytePtr(renegade_textram, tile_index);
		int attributes = source.read(0x400);
		SET_TILE_INFO(
				0,
				(attributes&3)*256 + source.read(0),
				attributes>>6,
				0);
                tile_info.flags = 0;
            }
        };
	
	public static VhStartPtr renegade_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(get_bg_tilemap_info,tilemap_scan_rows,TILEMAP_OPAQUE,   16,16,64,16);
		fg_tilemap = tilemap_create(get_fg_tilemap_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
	
		if (bg_tilemap==null || fg_tilemap==null)
			return 1;
	
		/*TODO*///tilemap_set_transparent_pen(fg_tilemap,0);
                fg_tilemap.transparent_pen = 0;
		tilemap_set_scrolldx( bg_tilemap, 256, 0 );
	
		tilemap_set_scrolldy( fg_tilemap, 0, 16 );
		tilemap_set_scrolldy( bg_tilemap, 0, 16 );
		return 0;
	} };
	
	static void draw_sprites( mame_bitmap bitmap )
	{
		rectangle clip = Machine.visible_area;
	
		UBytePtr source = new UBytePtr(spriteram);
		UBytePtr finish = new UBytePtr(source, 96*4);
	
		while( source.offset<finish.offset )
		{
			int sy = 240-source.read(0);
			if( sy>=16 )
			{
			    int attributes = source.read(1); /* SFCCBBBB */
			    int sx = source.read(3);
			    int sprite_number = source.read(2);
			    int sprite_bank = 9 + (attributes&0xF);
			    int color = (attributes>>4)&0x3;
			    int xflip = attributes&0x40;
	
			    if( sx>248 ) sx -= 256;
	
			    if( (attributes&0x80) != 0){ /* big sprite */
			        drawgfx(bitmap,Machine.gfx[sprite_bank],
			            sprite_number+1,
			            color,
			            xflip,0,
			            sx,sy+16,
			            clip,TRANSPARENCY_PEN,0);
			    }
			    else
				{
			        sy += 16;
			    }
			    drawgfx(bitmap,Machine.gfx[sprite_bank],
			        sprite_number,
			        color,
			        xflip,0,
			        sx,sy,
			        clip,TRANSPARENCY_PEN,0);
			}
			source.inc(4);
		}
	}
	
	public static VhUpdatePtr renegade_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int fullrefresh) 
	{
                tilemap_update(ALL_TILEMAPS);
	
		tilemap_render(ALL_TILEMAPS);
                
		tilemap_set_scrollx( bg_tilemap, 0, renegade_scrollx );
		tilemap_set_scrolly( bg_tilemap, 0, 0 );
		tilemap_set_scrolly( fg_tilemap, 0, 0 );
	
		tilemap_draw( bitmap,bg_tilemap,0);
		draw_sprites( bitmap );
		tilemap_draw( bitmap,fg_tilemap,0);
	} };
}
