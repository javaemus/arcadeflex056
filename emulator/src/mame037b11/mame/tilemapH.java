/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mame037b11.mame;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.subArrays.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.mame.*;

import static mame037b11.mame.tilemapC.tile_info;

public class tilemapH {
    public static abstract interface GetTileInfoPtr {

        public abstract void handler(int memory_offset);
    }

    public static abstract interface GetMemoryOffsetPtr {

        public abstract /*UINT32*/ int handler(int u32_col, int u32_row, int u32_num_cols, int u32_num_rows);
    }

    public static final struct_tilemap ALL_TILEMAPS = null;
    /* ALL_TILEMAPS may be used with:
	tilemap_update, tilemap_render, tilemap_set_flip, tilemap_mark_all_pixels_dirty
     */
    public static final int TILEMAP_OPAQUE = 0x00;
    public static final int TILEMAP_TRANSPARENT = 0x01;
    public static final int TILEMAP_SPLIT = 0x02;
    public static final int TILEMAP_BITMASK = 0x04;
    public static final int TILEMAP_TRANSPARENT_COLOR = 0x08;
    /*
	TILEMAP_SPLIT should be used if the pixels from a single tile
	can appear in more than one plane.

	TILEMAP_BITMASK is needed for Namco SystemI
     */

    public static final int TILEMAP_IGNORE_TRANSPARENCY = 0x10;
    public static final int TILEMAP_BACK = 0x20;
    public static final int TILEMAP_FRONT = 0x40;

    /*
	when rendering a split layer, pass TILEMAP_FRONT or TILEMAP_BACK or'd with the
	tile_priority value to specify the part to draw.
     */

    public static final int TILEMAP_BITMASK_TRANSPARENT = (0);
    public static final int TILEMAP_BITMASK_OPAQUE = (~0);
    
    public static class cached_tile_info {

        public UBytePtr pen_data;
        public IntArray pal_data;
        public int u32_pen_usage;
        public int u32_flags;
    }

    public static class struct_tile_info {

        /*
		you must set tile_info.pen_data, tile_info.pal_data and tile_info.pen_usage
		in the callback.  You can use the SET_TILE_INFO() macro below to do this.
		tile_info.flags and tile_info.priority will be automatically preset to 0,
		games that don't need them don't need to explicitly set them to 0
         */
        public UBytePtr pen_data;
        public IntArray pal_data;
        public int u32_pen_usage;
        public int u32_flags;

        public int u32_priority;
        public UBytePtr mask_data;
    }

    public static void SET_TILE_INFO(int GFX, int CODE, int COLOR) {
        GfxElement gfx = Machine.gfx[(GFX)];
        int _code = (CODE) % gfx.total_elements;
        tile_info.pen_data = new UBytePtr(gfx.gfxdata, _code * gfx.char_modulo);
        tile_info.pal_data = new IntArray(gfx.colortable, gfx.color_granularity * (COLOR));
        tile_info.u32_pen_usage = gfx.pen_usage != null ? gfx.pen_usage[_code] : 0;
    }

    /* tile flags, set by get_tile_info callback */
    public static int TILE_FLIPX = 0x01;
    public static int TILE_FLIPY = 0x02;

    public static int TILE_SPLIT(int t) {
        return t << 2;
    }

    /* TILE_SPLIT is for use with TILEMAP_SPLIT layers.  It selects transparency type. */
    public static int TILE_IGNORE_TRANSPARENCY = 0x10;

    /* TILE_IGNORE_TRANSPARENCY is used if you need an opaque tile in a transparent layer */
    public static int TILE_FLIPYX(int YX) {
        return YX;
    }

    public static int TILE_FLIPXY(int XY) {
        return ((((XY) >>> 1) | ((XY) << 1)) & 3);
    }

    /*
	TILE_FLIPYX is a shortcut that can be used by approx 80% of games,
	since yflip frequently occurs one bit higher than xflip within a
	tile attributes byte.
     */
    public static final int TILE_LINE_DISABLED = 0x80000000;

    public static class tilemap_mask {

        mame_bitmap bitmask;
        int line_offset;
        char[]/*UINT8*/ u8_data;
        UBytePtr[] data_row;
    }

    public static class struct_tilemap {

        public struct_tilemap() {

        }
        public GetMemoryOffsetPtr get_memory_offset;
        public int[] memory_offset_to_cached_index;
        public int[] u32_cached_index_to_memory_offset;
        public int[] logical_flip_to_cached_flip = new int[4];

        /* callback to interpret video VRAM for the tilemap */
        public GetTileInfoPtr tile_get_info;

        public int u32_max_memory_offset;
        public int num_tiles;
        public int num_logical_rows, num_logical_cols;
        public int num_cached_rows, num_cached_cols;
        public int cached_tile_width, cached_tile_height, cached_width, cached_height;
        public cached_tile_info[] cached_tile_info;

        public int dx, dx_if_flipped;
        public int dy, dy_if_flipped;
        public int scrollx_delta, scrolly_delta;

        public int enable;
        public int attributes;
        public int type;
        public int transparent_pen;
        public int[] u32_transmask = new int[4];
        public WriteHandlerPtr draw;//void (*draw)( int, int );
        public WriteHandlerPtr draw_opaque;//void (*draw_opaque)( int, int );
        public char[] u8_priority;
        /* priority for each tile */
        public UBytePtr[] priority_row;

        public int[] u8_visible;
        /* boolean flag for each tile */

        public int[] u8_dirty_vram;
        /* boolean flag for each tile */
        public int[] u8_dirty_pixels;

        public int scroll_rows, scroll_cols;
        public int[] rowscroll;
        public int[] colscroll;

        public int orientation;
        public int clip_left, clip_right, clip_top, clip_bottom;

        /* cached color data */
        public mame_bitmap pixmap;
        public int pixmap_line_offset;

        public tilemap_mask foreground;
        /* for transparent layers, or the front half of a split layer */

        public tilemap_mask background;
        /* for the back half of a split layer */

        public struct_tilemap next;
        /* resource tracking */
    }

    public static final int TILEMAP_FLIPX = 0x1;
    public static final int TILEMAP_FLIPY = 0x2;

}
