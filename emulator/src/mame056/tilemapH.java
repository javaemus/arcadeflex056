/**
 * ported to v0.56
 */
package mame056;

import static common.ptr.*;
import static common.subArrays.*;
import static mame056.drawgfxH.*;
import static mame056.mame.*;
import static mame056.tilemapC.*;

public class tilemapH {

    public static abstract interface GetTileInfoPtr {

        public abstract void handler(int memory_offset);
    }

    public static abstract interface GetMemoryOffsetPtr {

        public abstract /*UINT32*/ int handler(int u32_col, int u32_row, int u32_num_cols, int u32_num_rows);
    }

    public static final struct_tilemap ALL_TILEMAPS = null;
    /* ALL_TILEMAPS may be used with:
	tilemap_set_flip, tilemap_mark_all_tiles_dirty
     */
    public static final int TILEMAP_OPAQUE = 0x00;
    public static final int TILEMAP_TRANSPARENT = 0x01;
    public static final int TILEMAP_SPLIT = 0x02;
    public static final int TILEMAP_BITMASK = 0x04;
    public static final int TILEMAP_TRANSPARENT_COLOR = 0x08;

    /* Set transparency_pen to a mask.  pen&mask determines whether each pixel is in front or back half */
    public static final int TILEMAP_SPLIT_PENBIT = 0x10;
    /*
	TILEMAP_SPLIT should be used if the pixels from a single tile
	can appear in more than one plane.

	TILEMAP_BITMASK is used by Namco System1, Namco System2, NamcoNA1/2, Namco NB1
     */

    public static final int TILEMAP_IGNORE_TRANSPARENCY = 0x10;

    public static final int TILEMAP_BACK                = 0x20;
    public static final int TILEMAP_FRONT               = 0x40;
    public static final int TILEMAP_ALPHA               = 0x80;

/*TODO*////*
/*TODO*///	when rendering a split layer, pass TILEMAP_FRONT or TILEMAP_BACK or'd with the
/*TODO*///	tile_priority value to specify the part to draw.
/*TODO*///
/*TODO*///	when rendering a layer in alpha mode, the priority parameter
/*TODO*///	becomes the alpha parameter (0..255).  Split mode is still
/*TODO*///	available in alpha mode, ignore_transparency isn't.
/*TODO*///*/
/*TODO*///
    public static class struct_tile_info {

        /*
		you must set tile_info.pen_data, tile_info.pal_data and tile_info.pen_usage
		in the callback.  You can use the SET_TILE_INFO() macro below to do this.
		tile_info.flags and tile_info.priority will be automatically preset to 0,
		games that don't need them don't need to explicitly set them to 0
         */
        public UBytePtr pen_data;
        public IntArray pal_data;
        public int/*UINT32*/ flags;
        public int skip;
        public int/*UINT32*/ tile_number;/* needed for tilemap_mark_gfxdata_dirty */
        public int/*UINT32*/ pen_usage;/* TBR */
        public int/*UINT32*/ priority;/* tile priority */
        public UBytePtr mask_data;/* for TILEMAP_BITMASK */
    }

    public static void SET_TILE_INFO(int GFX, int CODE, int COLOR, int FLAGS) {
        GfxElement gfx = Machine.gfx[(GFX)];
        int _code = (CODE) % gfx.total_elements;
        tile_info.tile_number = _code;
        tile_info.pen_data = new UBytePtr(gfx.gfxdata, _code * gfx.char_modulo);
        tile_info.pal_data = new IntArray(gfx.colortable, gfx.color_granularity * (COLOR));
        tile_info.pen_usage = gfx.pen_usage != null ? gfx.pen_usage[_code] : 0;
        tile_info.flags = FLAGS;
        if ((gfx.flags & GFX_PACKED) != 0) {
            tile_info.flags |= TILE_4BPP;
        }
        if ((gfx.flags & GFX_SWAPXY) != 0) {
            tile_info.flags |= TILE_SWAPXY;
        }
    }

    
    /* tile flags, set by get_tile_info callback */
    /* TILE_IGNORE_TRANSPARENCY is used if you need an opaque tile in a transparent layer. */
    public static final int TILE_FLIPX = 0x01;
    public static final int TILE_FLIPY = 0x02;
    public static final int TILE_SWAPXY = 0x04;
    public static final int TILE_IGNORE_TRANSPARENCY = 0x08;
    public static final int TILE_4BPP = 0x10;
    public static final int TILE_SPLIT = 0x60;

    
    /* TILE_SPLIT is for use with TILEMAP_SPLIT layers.  It selects transparency type. */
    public static int TILE_SPLIT_OFFSET			= 5;
    public static int TILE_SPLIT(int T){
        return ((T)<<TILE_SPLIT_OFFSET);
    }

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

 /*TODO*///#define TILE_LINE_DISABLED 0x80000000
/*TODO*///
/*TODO*///extern struct mame_bitmap *priority_bitmap;
/*TODO*///
/*TODO*////* don't call these from drivers - they are called from mame.c */
/*TODO*///int tilemap_init( void );
/*TODO*///void tilemap_close( void );
/*TODO*///void tilemap_dispose( struct tilemap *tilemap );
/*TODO*///
    public static final int TILEMAP_FLIPX = 0x1;
    public static final int TILEMAP_FLIPY = 0x2;

    public static final int TILE_FLAG_TILE_PRIORITY = (0x0f);
    public static final int TILE_FLAG_FG_OPAQUE = (0x10);
    public static final int TILE_FLAG_BG_OPAQUE = (0x20);

}
