/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mame037b11.mame;

//import static mame037b11.mame.tilemapH.*;
import static mame056.tilemapH.*;
import static mame056.common.*;
import static mame056.commonH.*;
// refactor
import static arcadeflex036.osdepend.logerror;
import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.*;
import static common.ptr.*;
import static common.subArrays.*;
import static mame056.drawgfxH.*;
import static mame056.driverH.*;
import static mame056.mame.*;

import static mame056.tilemapC.priority_bitmap;

public class tilemapC {
    /*TODO*///#define SWAP(X,Y) {UINT32 temp=X; X=Y; Y=temp; }
    /**
     * ********************************************************************************
     */
    /* some common mappings */
    public static GetMemoryOffsetPtr tilemap_scan_rows = new GetMemoryOffsetPtr() {
        public int handler(int u32_col, int u32_row, int u32_num_cols, int u32_num_rows) {
            /* logical (col,row) -> memory offset */
            return u32_row * u32_num_cols + u32_col;
        }
    };
    public static GetMemoryOffsetPtr tilemap_scan_cols = new GetMemoryOffsetPtr() {
        public int handler(int u32_col, int u32_row, int u32_num_cols, int u32_num_rows) {
            /* logical (col,row) -> memory offset */
            return u32_col * u32_num_rows + u32_row;
        }
    };

    /**
     * ******************************************************************************
     */
    static mame_bitmap create_tmpbitmap(int width, int height, int depth) {
        return bitmap_alloc(width * 2, height * 2);
    }

    static mame_bitmap create_bitmask(int width, int height) {
        width = (width + 7) / 8;
        /* 8 bits per byte */
        return bitmap_alloc(width, height);
    }

    /**
     * ********************************************************************************
     */
    static int mappings_create(struct_tilemap tilemap) {
        int max_memory_offset = 0;
        int /*UINT32*/ col, row;
        int /*UINT32*/ num_logical_rows = tilemap.num_logical_rows;
        int /*UINT32*/ num_logical_cols = tilemap.num_logical_cols;
        /* count offsets (might be larger than num_tiles) */
        for (row = 0; row < num_logical_rows; row++) {
            for (col = 0; col < num_logical_cols; col++) {
                int /*UINT32*/ memory_offset = tilemap.get_memory_offset.handler(col, row, num_logical_cols, num_logical_rows);
                if (memory_offset > max_memory_offset) {
                    max_memory_offset = memory_offset;
                }
            }
        }
        max_memory_offset++;
        tilemap.max_memory_offset = max_memory_offset;
        /* logical to cached (tilemap_mark_dirty) */
        tilemap.memory_offset_to_cached_index = new int[4 * max_memory_offset];//malloc( sizeof(int)*max_memory_offset );
        if (tilemap.memory_offset_to_cached_index != null) {
            /* cached to logical (get_tile_info) */
            tilemap.cached_indx_to_memory_offset = new int[4 * tilemap.num_tiles];//malloc( sizeof(UINT32)*tilemap->num_tiles );
            if (tilemap.cached_indx_to_memory_offset != null) {
                return 0;
                /* no error */
            }
            tilemap.memory_offset_to_cached_index = null;
        }
        return -1;
        /* error */
    }

    public static void mappings_dispose(struct_tilemap tilemap) {
        tilemap.cached_indx_to_memory_offset = null;
        tilemap.memory_offset_to_cached_index = null;
    }

    public static void mappings_update(struct_tilemap tilemap) {
        int logical_flip;
        int /*UINT32*/ logical_index, cached_index;
        int /*UINT32*/ num_cached_rows = tilemap.num_cached_rows;
        int /*UINT32*/ num_cached_cols = tilemap.num_cached_cols;
        int /*UINT32*/ num_logical_rows = tilemap.num_logical_rows;
        int /*UINT32*/ num_logical_cols = tilemap.num_logical_cols;
        for (logical_index = 0; logical_index < tilemap.max_memory_offset; logical_index++) {
            tilemap.memory_offset_to_cached_index[logical_index] = -1;
        }

        logerror("log size(%dx%d); cach size(%dx%d)\n",
                num_logical_cols, num_logical_rows,
                num_cached_cols, num_cached_rows);

        for (logical_index = 0; logical_index < tilemap.num_tiles; logical_index++) {
            int /*UINT32*/ logical_col = logical_index % num_logical_cols;
            int /*UINT32*/ logical_row = logical_index / num_logical_cols;
            int memory_offset = tilemap.get_memory_offset.handler(logical_col, logical_row, num_logical_cols, num_logical_rows);
            int /*UINT32*/ cached_col = logical_col;
            int /*UINT32*/ cached_row = logical_row;
            if ((tilemap.orientation & ORIENTATION_SWAP_XY) != 0) {
                //SWAP(cached_col, cached_row)
                int temp = cached_col;
                cached_col = cached_row;
                cached_row = temp;
            }
            if ((tilemap.orientation & ORIENTATION_FLIP_X) != 0) {
                cached_col = (num_cached_cols - 1) - cached_col;
            }
            if ((tilemap.orientation & ORIENTATION_FLIP_Y) != 0) {
                cached_row = (num_cached_rows - 1) - cached_row;
            }
            cached_index = cached_row * num_cached_cols + cached_col;
            tilemap.memory_offset_to_cached_index[memory_offset] = cached_index;
            tilemap.cached_indx_to_memory_offset[cached_index] = memory_offset;
        }
        for (logical_flip = 0; logical_flip < 4; logical_flip++) {
            int cached_flip = logical_flip;
            if ((tilemap.attributes & TILEMAP_FLIPX) != 0) {
                cached_flip ^= TILE_FLIPX;
            }
            if ((tilemap.attributes & TILEMAP_FLIPY) != 0) {
                cached_flip ^= TILE_FLIPY;
            }
//#ifndef PREROTATE_GFX
            if ((Machine.orientation & ORIENTATION_SWAP_XY) != 0) {
                if ((Machine.orientation & ORIENTATION_FLIP_X) != 0) {
                    cached_flip ^= TILE_FLIPY;
                }
                if ((Machine.orientation & ORIENTATION_FLIP_Y) != 0) {
                    cached_flip ^= TILE_FLIPX;
                }
            } else {
                if ((Machine.orientation & ORIENTATION_FLIP_X) != 0) {
                    cached_flip ^= TILE_FLIPX;
                }
                if ((Machine.orientation & ORIENTATION_FLIP_Y) != 0) {
                    cached_flip ^= TILE_FLIPY;
                }
            }
//#endif
            if ((tilemap.orientation & ORIENTATION_SWAP_XY) != 0) {
                cached_flip = ((cached_flip & 1) << 1) | ((cached_flip & 2) >>> 1);
            }
            tilemap.logical_flip_to_cached_flip[logical_flip] = cached_flip;
        }
    }

    /**
     * ********************************************************************************
     */
    //public static mame_bitmap priority_bitmap;
    /* priority buffer (corresponds to screen bitmap) */
    static int priority_bitmap_line_offset;

    static /*UINT8*/ int[] u8_flip_bit_table = new int[0x100];
    /* horizontal flip for 8 pixels */
    static struct_tilemap first_tilemap = null;

    /* resource tracking */
    static int screen_width, screen_height;

    public static struct_tile_info tile_info = new struct_tile_info();

    public static final int TILE_TRANSPARENT = 0;
    public static final int TILE_MASKED = 1;
    public static final int TILE_OPAQUE = 2;

    private static void memcpybitmask16(UShortPtr dest, UShortPtr source, UShortPtr bitmask, int count) {
        for (;;) {
            char/*UINT32*/ data = bitmask.read(bitmask.offset);
            bitmask.offset++;
            if ((data & 0x80) != 0) {
                dest.write(0, source.read(0));
            }
            if ((data & 0x40) != 0) {
                dest.write(1, source.read(1));
            }
            if ((data & 0x20) != 0) {
                dest.write(2, source.read(2));
            }
            if ((data & 0x10) != 0) {
                dest.write(3, source.read(3));
            }
            if ((data & 0x08) != 0) {
                dest.write(4, source.read(4));
            }
            if ((data & 0x04) != 0) {
                dest.write(5, source.read(5));
            }
            if ((data & 0x02) != 0) {
                dest.write(6, source.read(6));
            }
            if ((data & 0x01) != 0) {
                dest.write(7, source.read(7));
            }
            if (--count == 0) {
                break;
            }
            source.offset += 8;
            dest.offset += 8;
        }
    }

    private static void memsetbitmask16(UShortPtr dest, int value, UShortPtr bitmask, int count) {
        /* TBA: combine with memcpybitmask */
        for (;;) {
            int/*UINT32*/ data = bitmask.read(bitmask.offset);
            bitmask.offset++;
            if ((data & 0x80) != 0) {
                dest.write(0, (char) (dest.read(0) | value));
            }
            if ((data & 0x40) != 0) {
                dest.write(1, (char) (dest.read(1) | value));
            }
            if ((data & 0x20) != 0) {
                dest.write(2, (char) (dest.read(2) | value));
            }
            if ((data & 0x10) != 0) {
                dest.write(3, (char) (dest.read(3) | value));
            }
            if ((data & 0x08) != 0) {
                dest.write(4, (char) (dest.read(4) | value));
            }
            if ((data & 0x04) != 0) {
                dest.write(5, (char) (dest.read(5) | value));
            }
            if ((data & 0x02) != 0) {
                dest.write(6, (char) (dest.read(6) | value));
            }
            if ((data & 0x01) != 0) {
                dest.write(7, (char) (dest.read(7) | value));
            }
            if (--count == 0) {
                break;
            }
            dest.offset += 8;
        }
    }

    /* the following parameters are constant across tilemap_draw calls */
    static class _blit {

        int clip_left, clip_top, clip_right, clip_bottom;
        int source_width, source_height;
        int dest_line_offset, source_line_offset, mask_line_offset;
        int dest_row_offset, source_row_offset, mask_row_offset;
        mame_bitmap screen;
        mame_bitmap pixmap;
        mame_bitmap bitmask;
        public UBytePtr[] mask_data_row;
        public UBytePtr[] priority_data_row;
        int tile_priority;
        int tilemap_priority_code;
    }
    static _blit blit = new _blit();

    /*TODO*///#define MASKROWBYTES(W) (((W)+7)/8)
    static void memsetbitmask8(UBytePtr dest, int value, UBytePtr bitmask, int count) {
        /* TBA: combine with memcpybitmask */
        for (;;) {
            int/*UINT32*/ data = bitmask.readinc();
            if ((data & 0x80) != 0) {
                dest.write(0, dest.read(0) | value);
            }
            if ((data & 0x40) != 0) {
                dest.write(1, dest.read(1) | value);
            }
            if ((data & 0x20) != 0) {
                dest.write(2, dest.read(2) | value);
            }
            if ((data & 0x10) != 0) {
                dest.write(3, dest.read(3) | value);
            }
            if ((data & 0x08) != 0) {
                dest.write(4, dest.read(4) | value);
            }
            if ((data & 0x04) != 0) {
                dest.write(5, dest.read(5) | value);
            }
            if ((data & 0x02) != 0) {
                dest.write(6, dest.read(6) | value);
            }
            if ((data & 0x01) != 0) {
                dest.write(7, dest.read(7) | value);
            }
            if (--count == 0) {
                break;
            }
            dest.offset += 8;
        }
    }

    static void memcpybitmask8(UBytePtr dest, UBytePtr source, UBytePtr bitmask, int count) {
        for (;;) {
            int/*UINT32*/ data = bitmask.readinc();
            if ((data & 0x80) != 0) {
                dest.write(0, source.read(0));
            }
            if ((data & 0x40) != 0) {
                dest.write(1, source.read(1));
            }
            if ((data & 0x20) != 0) {
                dest.write(2, source.read(2));
            }
            if ((data & 0x10) != 0) {
                dest.write(3, source.read(3));
            }
            if ((data & 0x08) != 0) {
                dest.write(4, source.read(4));
            }
            if ((data & 0x04) != 0) {
                dest.write(5, source.read(5));
            }
            if ((data & 0x02) != 0) {
                dest.write(6, source.read(6));
            }
            if ((data & 0x01) != 0) {
                dest.write(7, source.read(7));
            }
            if (--count == 0) {
                break;
            }
            source.offset += 8;
            dest.offset += 8;
        }
    }

    /*TODO*////***********************************************************************************/
/*TODO*///
/*TODO*///static void memcpybitmask16( UINT16 *dest, const UINT16 *source, const UINT8 *bitmask, int count ){
/*TODO*///	for(;;){
/*TODO*///		UINT32 data = *bitmask++;
/*TODO*///		if( data&0x80 ) dest[0] = source[0];
/*TODO*///		if( data&0x40 ) dest[1] = source[1];
/*TODO*///		if( data&0x20 ) dest[2] = source[2];
/*TODO*///		if( data&0x10 ) dest[3] = source[3];
/*TODO*///		if( data&0x08 ) dest[4] = source[4];
/*TODO*///		if( data&0x04 ) dest[5] = source[5];
/*TODO*///		if( data&0x02 ) dest[6] = source[6];
/*TODO*///		if( data&0x01 ) dest[7] = source[7];
/*TODO*///		if( --count == 0 ) break;
/*TODO*///		source+=8;
/*TODO*///		dest+=8;
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*////***********************************************************************************/
/*TODO*///
/*TODO*///#define TILE_WIDTH	8
/*TODO*///#define TILE_HEIGHT	8
/*TODO*///#define DATA_TYPE UINT8
/*TODO*///#define memcpybitmask memcpybitmask8
/*TODO*///#define DECLARE(function,args,body) static void function##8x8x8BPP args body
/*TODO*///#include "tilemap.c"
/*TODO*///
/*TODO*///#define TILE_WIDTH	16
/*TODO*///#define TILE_HEIGHT	16
/*TODO*///#define DATA_TYPE UINT8
/*TODO*///#define memcpybitmask memcpybitmask8
/*TODO*///#define DECLARE(function,args,body) static void function##16x16x8BPP args body
/*TODO*///#include "tilemap.c"
/*TODO*///
/*TODO*///#define TILE_WIDTH	32
/*TODO*///#define TILE_HEIGHT	32
/*TODO*///#define DATA_TYPE UINT8
/*TODO*///#define memcpybitmask memcpybitmask8
/*TODO*///#define DECLARE(function,args,body) static void function##32x32x8BPP args body
/*TODO*///#include "tilemap.c"
/*TODO*///
/*TODO*///#define TILE_WIDTH	8
/*TODO*///#define TILE_HEIGHT	8
/*TODO*///#define DATA_TYPE UINT16
/*TODO*///#define memcpybitmask memcpybitmask16
/*TODO*///#define DECLARE(function,args,body) static void function##8x8x16BPP args body
/*TODO*///#include "tilemap.c"
/*TODO*///
/*TODO*///#define TILE_WIDTH	16
/*TODO*///#define TILE_HEIGHT	16
/*TODO*///#define DATA_TYPE UINT16
/*TODO*///#define memcpybitmask memcpybitmask16
/*TODO*///#define DECLARE(function,args,body) static void function##16x16x16BPP args body
/*TODO*///#include "tilemap.c"
/*TODO*///
/*TODO*///#define TILE_WIDTH	32
/*TODO*///#define TILE_HEIGHT	32
/*TODO*///#define DATA_TYPE UINT16
/*TODO*///#define memcpybitmask memcpybitmask16
/*TODO*///#define DECLARE(function,args,body) static void function##32x32x16BPP args body
/*TODO*///#include "tilemap.c"
/*TODO*///
    /**
     * ******************************************************************************
     */
    public static void mask_dispose(tilemap_mask mask) {
        if (mask != null) {
            mask.data_row = null;
            mask.u8_data = null;
            bitmap_free(mask.bitmask);
            mask = null;
        }
    }

    public static tilemap_mask mask_create(struct_tilemap tilemap) {
        tilemap_mask mask = new tilemap_mask();
        if (mask != null) {
            mask.u8_data = new char[tilemap.num_tiles];
            mask.data_row = new UBytePtr[tilemap.num_cached_rows];
            mask.bitmask = //create_bitmask(tilemap.cached_width, tilemap.cached_height);
            create_tmpbitmap(tilemap.cached_width, tilemap.cached_height, Machine.scrbitmap.depth);
            if (mask.u8_data != null && mask.data_row != null && mask.bitmask != null) {
                int row;
                for (row = 0; row < tilemap.num_cached_rows; row++) {
                    mask.data_row[row] = new UBytePtr(mask.u8_data, row * tilemap.num_cached_cols);
                }
                mask.line_offset = mask.bitmask.line[1].offset - mask.bitmask.line[0].offset;
                return mask;
            }
        }
        mask_dispose(mask);
        return null;
    }

    /**
     * ********************************************************************************
     */
    static void install_draw_handlers(struct_tilemap tilemap) {
        int tile_width = tilemap.cached_tile_width;
        int tile_height = tilemap.cached_tile_height;
        tilemap.draw037 = tilemap.draw_opaque = null;
        /*TODO*///	if( Machine->scrbitmap->depth==16 ){
/*TODO*///		if( tile_width==8 && tile_height==8 ){
/*TODO*///			tilemap->draw = draw8x8x16BPP;
/*TODO*///			tilemap->draw_opaque = draw_opaque8x8x16BPP;
/*TODO*///		}
/*TODO*///		else if( tile_width==16 && tile_height==16 ){
/*TODO*///			tilemap->draw = draw16x16x16BPP;
/*TODO*///			tilemap->draw_opaque = draw_opaque16x16x16BPP;
/*TODO*///		}
/*TODO*///		else if( tile_width==32 && tile_height==32 ){
/*TODO*///			tilemap->draw = draw32x32x16BPP;
/*TODO*///			tilemap->draw_opaque = draw_opaque32x32x16BPP;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else {
        if (tile_width == 8 && tile_height == 8) {
            tilemap.draw037 = draw8x8x8BPP;
            tilemap.draw_opaque = draw_opaque8x8x8BPP;
        } else if (tile_width == 16 && tile_height == 16) {
            tilemap.draw037 = draw16x16x8BPP;
            tilemap.draw_opaque = draw_opaque16x16x8BPP;
        } else if (tile_width == 32 && tile_height == 32) {
            tilemap.draw037 = draw32x32x8BPP;
            tilemap.draw_opaque = draw_opaque32x32x8BPP;
        }
        /*TODO*///	}
    }

    /**
     * ********************************************************************************
     */
    public static int tilemap_init() {
        int/*UINT32*/ value, data, bit;
        for (value = 0; value < 0x100; value++) {
            data = 0;
            for (bit = 0; bit < 8; bit++) {
                if (((value >>> bit) & 1) != 0) {
                    data |= 0x80 >>> bit;
                }
            }
            u8_flip_bit_table[value] = data & 0xFF;
        }
        screen_width = Machine.scrbitmap.width;
        screen_height = Machine.scrbitmap.height;
        first_tilemap = null;
        priority_bitmap = create_tmpbitmap(screen_width, screen_height, 16);
        if (priority_bitmap != null) {
            priority_bitmap_line_offset = priority_bitmap.line[1].offset - priority_bitmap.line[0].offset;
            return 0;
        }
        return -1;
    }

    public static void tilemap_close() {
        /*TODO*///	while( first_tilemap ){
/*TODO*///		struct tilemap *next = first_tilemap->next;
/*TODO*///		tilemap_dispose( first_tilemap );
/*TODO*///		first_tilemap = next;
/*TODO*///	}
/*TODO*///	bitmap_free( priority_bitmap );
    }

    /**
     * ********************************************************************************
     */
    public static struct_tilemap tilemap_create(GetTileInfoPtr tile_get_info,
            GetMemoryOffsetPtr get_memory_offset,
            int type,
            int tile_width,
            int tile_height, /* in pixels */
            int num_cols,
            int num_rows /* in tiles */) {
        struct_tilemap tilemap = new struct_tilemap();
        if (tilemap != null) {
            int num_tiles = num_cols * num_rows;
            tilemap.num_logical_cols = num_cols;
            tilemap.num_logical_rows = num_rows;
            if ((Machine.orientation & ORIENTATION_SWAP_XY) != 0) {
                logerror("swap!!\n");
                //SWAP( tile_width, tile_height )
                int temp = tile_width;
                tile_width = tile_height;
                tile_height = temp;
                //SWAP( num_cols,num_rows )
                int temp2 = num_cols;
                num_cols = num_rows;
                num_rows = temp2;
            }
            tilemap.num_cached_cols = num_cols;
            tilemap.num_cached_rows = num_rows;
            tilemap.num_tiles = num_tiles;
            tilemap.cached_tile_width = tile_width;
            tilemap.cached_tile_height = tile_height;
            tilemap.cached_width = tile_width * num_cols;
            tilemap.cached_height = tile_height * num_rows;
            tilemap.tile_get_info = tile_get_info;
            tilemap.get_memory_offset = get_memory_offset;
            tilemap.orientation = Machine.orientation;
            tilemap.enable = 1;
            tilemap.type = type;
            tilemap.scroll_rows = 1;
            tilemap.scroll_cols = 1;
            tilemap.transparent_pen = -1;
            tilemap.cached_tile_info = new cached_tile_info[num_tiles];//calloc( num_tiles, sizeof(struct cached_tile_info) );
            for (int i = 0; i < num_tiles; i++) {
                tilemap.cached_tile_info[i] = new cached_tile_info();//init cache_tiles
            }
            tilemap.u8_priority = new char[num_tiles];
            tilemap.u8_visible = new int[num_tiles];
            tilemap.u8_dirty_vram = new int[num_tiles];
            tilemap.u8_dirty_pixels = new int[num_tiles];
            tilemap.rowscroll = new int[tilemap.cached_height];//calloc(tilemap->cached_height,sizeof(int));
            tilemap.colscroll = new int[tilemap.cached_width];//calloc(tilemap->cached_width,sizeof(int));
            tilemap.priority_row = new UBytePtr[num_rows];//malloc( sizeof(UINT8 *)*num_rows );
            tilemap.pixmap = create_tmpbitmap(tilemap.cached_width, tilemap.cached_height, Machine.scrbitmap.depth);
            tilemap.foreground = mask_create(tilemap);
            tilemap.background = (type & TILEMAP_SPLIT) != 0 ? mask_create(tilemap) : null;
            if (tilemap.cached_tile_info != null
                    && tilemap.u8_priority != null && tilemap.u8_visible != null
                    && tilemap.u8_dirty_vram != null && tilemap.u8_dirty_pixels != null
                    && tilemap.rowscroll != null && tilemap.colscroll != null
                    && tilemap.priority_row != null
                    && tilemap.pixmap != null && tilemap.foreground != null
                    && ((type & TILEMAP_SPLIT) == 0 || tilemap.background != null)
                    && (mappings_create(tilemap) == 0)) {
                int/*UINT32*/ row;
                for (row = 0; row < num_rows; row++) {
                    tilemap.priority_row[row] = new UBytePtr(tilemap.u8_priority, num_cols * row);
                }
                install_draw_handlers(tilemap);
                mappings_update(tilemap);
                tilemap_set_clip(tilemap, new rectangle(Machine.visible_area));
                memset(tilemap.u8_dirty_vram, 1, num_tiles);
                memset(tilemap.u8_dirty_pixels, 1, num_tiles);
                tilemap.pixmap_line_offset = tilemap.pixmap.line[1].offset - tilemap.pixmap.line[0].offset;
                tilemap.next = first_tilemap;
                first_tilemap = tilemap;
                return tilemap;
            }
            tilemap_dispose(tilemap);
        }
        return null;
    }

    public static void tilemap_dispose(struct_tilemap tilemap) {
        /*TODO*///	if( tilemap==first_tilemap ){
/*TODO*///		first_tilemap = tilemap->next;
/*TODO*///	}
/*TODO*///	else {
/*TODO*///		struct tilemap *prev = first_tilemap;
/*TODO*///		while( prev->next != tilemap ) prev = prev->next;
/*TODO*///		prev->next =tilemap->next;
/*TODO*///	}
/*TODO*///
/*TODO*///	free( tilemap->cached_tile_info );
/*TODO*///	free( tilemap->priority );
/*TODO*///	free( tilemap->visible );
/*TODO*///	free( tilemap->dirty_vram );
/*TODO*///	free( tilemap->dirty_pixels );
/*TODO*///	free( tilemap->rowscroll );
/*TODO*///	free( tilemap->colscroll );
/*TODO*///	free( tilemap->priority_row );
/*TODO*///	bitmap_free( tilemap->pixmap );
/*TODO*///	mask_dispose( tilemap->foreground );
/*TODO*///	mask_dispose( tilemap->background );
/*TODO*///	mappings_dispose( tilemap );
/*TODO*///	free( tilemap );
    }

    /**
     * ********************************************************************************
     */
    public static void unregister_pens(struct_tilemap tilemap, int cache_ptr, int num_pens) {
        IntArray pal_data = tilemap.cached_tile_info[cache_ptr].pal_data;
        if (pal_data != null) {
            int /*UINT32*/ pen_usage = tilemap.cached_tile_info[cache_ptr].u32_pen_usage;
            if (pen_usage != 0) {
                /*TODO*///palette_decrease_usage_count(
                /*TODO*///        pal_data.offset - Machine.remapped_colortable.offset,
                /*TODO*///        pen_usage,
                /*TODO*///        PALETTE_COLOR_VISIBLE | PALETTE_COLOR_CACHED);
            } else {
                /*TODO*///palette_decrease_usage_countx(
                /*TODO*///        pal_data.offset - Machine.remapped_colortable.offset,
                /*TODO*///        num_pens,
                /*TODO*///        new UBytePtr(tilemap.cached_tile_info[cache_ptr].pen_data),
                /*TODO*///        PALETTE_COLOR_VISIBLE | PALETTE_COLOR_CACHED);
            }
            tilemap.cached_tile_info[cache_ptr].pal_data = null;
        }
    }

    public static void register_pens(struct_tilemap tilemap, int cache_ptr, int num_pens) {
        int /*UINT32*/ pen_usage = tilemap.cached_tile_info[cache_ptr].u32_pen_usage;
        if (pen_usage != 0) {
            /*TODO*///palette_increase_usage_count(
            /*TODO*///        tilemap.cached_tile_info[cache_ptr].pal_data.offset - Machine.remapped_colortable.offset,
            /*TODO*///        pen_usage,
            /*TODO*///        PALETTE_COLOR_VISIBLE | PALETTE_COLOR_CACHED);
        } else {
            /*TODO*///palette_increase_usage_countx(
            /*TODO*///        tilemap.cached_tile_info[cache_ptr].pal_data.offset - Machine.remapped_colortable.offset,
            /*TODO*///        num_pens,
            /*TODO*///        new UBytePtr(tilemap.cached_tile_info[cache_ptr].pen_data),
            /*TODO*///        PALETTE_COLOR_VISIBLE | PALETTE_COLOR_CACHED);
        }
    }

    /**
     * ********************************************************************************
     */
    public static void tilemap_set_enable(struct_tilemap tilemap, int enable) {
        tilemap.enable = enable;
    }

    public static void tilemap_set_flip(struct_tilemap tilemap, int attributes) {
        if (tilemap == ALL_TILEMAPS) {
            tilemap = first_tilemap;
            while (tilemap != null) {
                tilemap_set_flip(tilemap, attributes);
                tilemap = tilemap.next;
            }
        } else if (tilemap.attributes != attributes) {
            tilemap.attributes = attributes;
            tilemap.orientation = Machine.orientation;
            if ((attributes & TILEMAP_FLIPY) != 0) {
                tilemap.orientation ^= ORIENTATION_FLIP_Y;
                tilemap.scrolly_delta = tilemap.dy_if_flipped;
            } else {
                tilemap.scrolly_delta = tilemap.dy;
            }
            if ((attributes & TILEMAP_FLIPX) != 0) {
                tilemap.orientation ^= ORIENTATION_FLIP_X;
                tilemap.scrollx_delta = tilemap.dx_if_flipped;
            } else {
                tilemap.scrollx_delta = tilemap.dx;
            }

            mappings_update(tilemap);
            tilemap_mark_all_tiles_dirty(tilemap);
        }
    }

    public static void tilemap_set_clip(struct_tilemap tilemap, rectangle clip) {
        int left, top, right, bottom;
        if (clip != null) {
            left = clip.min_x;
            top = clip.min_y;
            right = clip.max_x + 1;
            bottom = clip.max_y + 1;
            if ((tilemap.orientation & ORIENTATION_SWAP_XY) != 0) {
                //SWAP(left, top)
                int temp = left;
                left = top;
                top = temp;
                //SWAP(right, bottom)
                int temp2 = right;
                right = bottom;
                bottom = temp2;
            }
            if ((tilemap.orientation & ORIENTATION_FLIP_X) != 0) {
                //SWAP(left, right)
                int temp = left;
                left = right;
                right = temp;
                left = screen_width - left;
                right = screen_width - right;
            }
            if ((tilemap.orientation & ORIENTATION_FLIP_Y) != 0) {
                //SWAP(top, bottom)
                int temp = top;
                top = bottom;
                bottom = temp;
                top = screen_height - top;
                bottom = screen_height - bottom;
            }
        } else {
            left = 0;
            top = 0;
            right = tilemap.cached_width;
            bottom = tilemap.cached_height;
        }
        tilemap.clip_left = left;
        tilemap.clip_right = right;
        tilemap.clip_top = top;
        tilemap.clip_bottom = bottom;
//	logerror("clip: %d,%d,%d,%d\n", left,top,right,bottom );
    }

    /**
     * ********************************************************************************
     */
    public static void tilemap_set_scroll_cols(struct_tilemap tilemap, int n) {
        if ((tilemap.orientation & ORIENTATION_SWAP_XY) != 0) {
            if (tilemap.scroll_rows != n) {
                tilemap.scroll_rows = n;
            }
        } else {
            if (tilemap.scroll_cols != n) {
                tilemap.scroll_cols = n;
            }
        }
    }

    public static void tilemap_set_scroll_rows(struct_tilemap tilemap, int n) {
        if ((tilemap.orientation & ORIENTATION_SWAP_XY) != 0) {
            if (tilemap.scroll_cols != n) {
                tilemap.scroll_cols = n;
            }
        } else {
            if (tilemap.scroll_rows != n) {
                tilemap.scroll_rows = n;
            }
        }
    }

    /**
     * ********************************************************************************
     */
    public static void tilemap_mark_tile_dirty(struct_tilemap tilemap, int memory_offset) {
        if (memory_offset < tilemap.max_memory_offset) {
            int cached_index = tilemap.memory_offset_to_cached_index[memory_offset];
            if (cached_index >= 0) {
                tilemap.u8_dirty_vram[cached_index] = 1;
            }
        }
    }

    public static void tilemap_mark_all_tiles_dirty(struct_tilemap tilemap) {
        if (tilemap == ALL_TILEMAPS) {
            tilemap = first_tilemap;
            while (tilemap != null) {
                tilemap_mark_all_tiles_dirty(tilemap);
                tilemap = tilemap.next;
            }
        } else {
            memset(tilemap.u8_dirty_vram, 1, tilemap.num_tiles);
        }
    }

    public static void tilemap_mark_all_pixels_dirty(struct_tilemap tilemap) {
        if (tilemap == ALL_TILEMAPS) {
            tilemap = first_tilemap;
            while (tilemap != null) {
                tilemap_mark_all_pixels_dirty(tilemap);
                tilemap = tilemap.next;
            }
        } else {
            /* invalidate all offscreen tiles */
            int/*UINT32*/ cached_tile_index;
            int/*UINT32*/ num_pens = tilemap.cached_tile_width * tilemap.cached_tile_height;
            for (cached_tile_index = 0; cached_tile_index < tilemap.num_tiles; cached_tile_index++) {
                if (tilemap.u8_visible[cached_tile_index] == 0) {
                    unregister_pens(tilemap, cached_tile_index, num_pens);
                    tilemap.u8_dirty_vram[cached_tile_index] = 1;
                }
            }
            memset(tilemap.u8_dirty_pixels, 1, tilemap.num_tiles);
        }
    }

    /**
     * ********************************************************************************
     */
    public static void draw_tile(
            struct_tilemap tilemap,
            int/*UINT32*/ cached_index,
            int/*UINT32*/ col, int/*UINT32*/ row
    ) {
        mame_bitmap pixmap = tilemap.pixmap;
        int /*UINT32*/ tile_width = tilemap.cached_tile_width;
        int /*UINT32*/ tile_height = tilemap.cached_tile_height;
        int cache_ptr = cached_index;//struct cached_tile_info *cached_tile_info = &tilemap->cached_tile_info[cached_index];
/*TOCHECK*/ UBytePtr pendata = new UBytePtr(tilemap.cached_tile_info[cache_ptr].pen_data);
        /*TOCHECK*/ IntArray paldata = tilemap.cached_tile_info[cache_ptr].pal_data;

        int /*UINT32*/ flags = tilemap.cached_tile_info[cache_ptr].u32_flags;
        int x, sx = tile_width * col;
        int sy, y1, y2, dy;

        if (Machine.scrbitmap.depth == 16) {
            //throw new UnsupportedOperationException("draw_tile in 16bit unimplemented");
                if(( flags&TILE_FLIPY ) != 0){ 
			y1 = tile_height*row+tile_height-1;
			y2 = y1-tile_height;
	 		dy = -1;
	 	}
	 	else {
			y1 = tile_height*row;
			y2 = y1+tile_height;
	 		dy = 1;
	 	}

		if(( flags&TILE_FLIPX ) != 0){
			tile_width--;
			for( sy=y1; sy!=y2; sy+=dy ){
				UShortPtr dest  = new UShortPtr(pixmap.line[sy], sx);
				for( x=tile_width; x>=0; x-- ) dest.write(x, (char)paldata.read(pendata.readinc()));
			}
		}
		else {
			for( sy=y1; sy!=y2; sy+=dy ){
				UShortPtr dest  = new UShortPtr(pixmap.line[sy], sx);
				for( x=0; x<tile_width; x++ ) dest.write(x, (char)paldata.read(pendata.readinc()));
			}
		}
        } else {
            if ((flags & TILE_FLIPY) != 0) {
                y1 = tile_height * row + tile_height - 1;
                y2 = y1 - tile_height;
                dy = -1;
            } else {
                y1 = tile_height * row;
                y2 = y1 + tile_height;
                dy = 1;
            }

            if ((flags & TILE_FLIPX) != 0) {
                tile_width--;
                for (sy = y1; sy != y2; sy += dy) {
                    UShortPtr dest  = new UShortPtr(pixmap.line[sy], sx);
                    for (x = tile_width; x >= 0; x--) {
                        dest.write(x, (char) paldata.read(pendata.readinc()));
                        
                    }
                }
            } else {
                for (sy = y1; sy != y2; sy += dy) {
                    UShortPtr dest = new UShortPtr(pixmap.line[sy], sx);
                    for (x = 0; x < tile_width; x++) {
                        dest.write(x, (char) paldata.read(pendata.readinc()));
                    }
                }
            }
        }
    }

    public static void tilemap_render(struct_tilemap tilemap) {
        if (tilemap == ALL_TILEMAPS) {
            tilemap = first_tilemap;
            while (tilemap != null) {
                tilemap_render(tilemap);
                tilemap = tilemap.next;
            }
        } else if (tilemap.enable != 0) {
            int[] u8_dirty_pixels = tilemap.u8_dirty_pixels;
            int[] u8_visible = tilemap.u8_visible;
            int/*UINT32*/ cached_index = 0;
            int/*UINT32*/ row, col;

            /* walk over cached rows/cols (better to walk screen coords) */
            for (row = 0; row < tilemap.num_cached_rows; row++) {
                for (col = 0; col < tilemap.num_cached_cols; col++) {
                    if (u8_visible[cached_index] != 0 && u8_dirty_pixels[cached_index] != 0) {
                        draw_tile(tilemap, cached_index, col, row);
                        u8_dirty_pixels[cached_index] = 0;
                    }
                    cached_index++;
                }
                /* next col */
            }
            /* next row */
        }
    }

    /*TODO*///
/*TODO*////***********************************************************************************/
/*TODO*///
/*TODO*///static int draw_bitmask(
/*TODO*///		struct osd_bitmap *mask,
/*TODO*///		UINT32 col, UINT32 row,
/*TODO*///		UINT32 tile_width, UINT32 tile_height,
/*TODO*///		const UINT8 *maskdata,
/*TODO*///		UINT32 flags )
/*TODO*///{
/*TODO*///	int is_opaque = 1, is_transparent = 1;
/*TODO*///	int x,sx = tile_width*col;
/*TODO*///	int sy,y1,y2,dy;
/*TODO*///
/*TODO*///	if( maskdata==TILEMAP_BITMASK_TRANSPARENT ) return TILE_TRANSPARENT;
/*TODO*///	if( maskdata==TILEMAP_BITMASK_OPAQUE) return TILE_OPAQUE;
/*TODO*///
/*TODO*///	if( flags&TILE_FLIPY ){
/*TODO*///		y1 = tile_height*row+tile_height-1;
/*TODO*///		y2 = y1-tile_height;
/*TODO*/// 		dy = -1;
/*TODO*/// 	}
/*TODO*/// 	else {
/*TODO*///		y1 = tile_height*row;
/*TODO*///		y2 = y1+tile_height;
/*TODO*/// 		dy = 1;
/*TODO*/// 	}
/*TODO*///
/*TODO*///	if( flags&TILE_FLIPX ){
/*TODO*///		tile_width--;
/*TODO*///		for( sy=y1; sy!=y2; sy+=dy ){
/*TODO*///			UINT8 *mask_dest  = mask->line[sy]+sx/8;
/*TODO*///			for( x=tile_width/8; x>=0; x-- ){
/*TODO*///				UINT8 data = flip_bit_table[*maskdata++];
/*TODO*///				if( data!=0x00 ) is_transparent = 0;
/*TODO*///				if( data!=0xff ) is_opaque = 0;
/*TODO*///				mask_dest[x] = data;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else {
/*TODO*///		for( sy=y1; sy!=y2; sy+=dy ){
/*TODO*///			UINT8 *mask_dest  = mask->line[sy]+sx/8;
/*TODO*///			for( x=0; x<tile_width/8; x++ ){
/*TODO*///				UINT8 data = *maskdata++;
/*TODO*///				if( data!=0x00 ) is_transparent = 0;
/*TODO*///				if( data!=0xff ) is_opaque = 0;
/*TODO*///				mask_dest[x] = data;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if( is_transparent ) return TILE_TRANSPARENT;
/*TODO*///	if( is_opaque ) return TILE_OPAQUE;
/*TODO*///	return TILE_MASKED;
/*TODO*///}
/*TODO*///
    static int draw_color_mask(
            mame_bitmap mask,
            int/*UINT32*/ col, int/*UINT32*/ row,
            int/*UINT32*/ tile_width, int/*UINT32*/ tile_height,
            UBytePtr pendata,
            UShortArray clut,
            int transparent_color,
            int/*UINT32*/ flags) {
        int is_opaque = 1, is_transparent = 1;

        int x, bit, sx = tile_width * col;
        int sy, y1, y2, dy;

        if ((flags & TILE_FLIPY) != 0) {
            y1 = tile_height * row + tile_height - 1;
            y2 = y1 - tile_height;
            dy = -1;
        } else {
            y1 = tile_height * row;
            y2 = y1 + tile_height;
            dy = 1;
        }

        if ((flags & TILE_FLIPX) != 0) {
            tile_width--;
            for (sy = y1; sy != y2; sy += dy) {
                UBytePtr mask_dest = new UBytePtr(mask.line[sy], sx / 8);
                for (x = tile_width / 8; x >= 0; x--) {
                    int/*UINT32*/ data = 0;
                    for (bit = 0; bit < 8; bit++) {
                        int/*UINT32*/ pen = pendata.readinc();
                        data = data >> 1;
                        if (clut.read(pen) != transparent_color) {
                            data |= 0x80;
                        }
                    }
                    if (data != 0x00) {
                        is_transparent = 0;
                    }
                    if (data != 0xff) {
                        is_opaque = 0;
                    }
                    mask_dest.write(x, data);
                }
            }
        } else {
            for (sy = y1; sy != y2; sy += dy) {
                UBytePtr mask_dest = new UBytePtr(mask.line[sy], sx / 8);
                for (x = 0; x < tile_width / 8; x++) {
                    int/*UINT32*/ data = 0;
                    for (bit = 0; bit < 8; bit++) {
                        int/*UINT32*/ pen = pendata.readinc();
                        data = data << 1;
                        if (clut.read(pen) != transparent_color) {
                            data |= 0x01;
                        }
                    }
                    if (data != 0x00) {
                        is_transparent = 0;
                    }
                    if (data != 0xff) {
                        is_opaque = 0;
                    }
                    mask_dest.write(x, data);
                }
            }
        }
        if (is_transparent != 0) {
            return TILE_TRANSPARENT;
        }
        if (is_opaque != 0) {
            return TILE_OPAQUE;
        }
        return TILE_MASKED;
    }

    /*TODO*///
/*TODO*///static int draw_pen_mask(
/*TODO*///	struct osd_bitmap *mask,
/*TODO*///	UINT32 col, UINT32 row,
/*TODO*///	UINT32 tile_width, UINT32 tile_height,
/*TODO*///	const UINT8 *pendata,
/*TODO*///	int transparent_pen,
/*TODO*///	UINT32 flags )
/*TODO*///{
/*TODO*///	int is_opaque = 1, is_transparent = 1;
/*TODO*///
/*TODO*///	int x,bit,sx = tile_width*col;
/*TODO*///	int sy,y1,y2,dy;
/*TODO*///
/*TODO*///	if( flags&TILE_FLIPY ){
/*TODO*///		y1 = tile_height*row+tile_height-1;
/*TODO*///		y2 = y1-tile_height;
/*TODO*/// 		dy = -1;
/*TODO*/// 	}
/*TODO*/// 	else {
/*TODO*///		y1 = tile_height*row;
/*TODO*///		y2 = y1+tile_height;
/*TODO*/// 		dy = 1;
/*TODO*/// 	}
/*TODO*///
/*TODO*///	if( flags&TILE_FLIPX ){
/*TODO*///		tile_width--;
/*TODO*///		for( sy=y1; sy!=y2; sy+=dy ){
/*TODO*///			UINT8 *mask_dest  = mask->line[sy]+sx/8;
/*TODO*///			for( x=tile_width/8; x>=0; x-- ){
/*TODO*///				UINT32 data = 0;
/*TODO*///				for( bit=0; bit<8; bit++ ){
/*TODO*///					UINT32 pen = *pendata++;
/*TODO*///					data = data>>1;
/*TODO*///					if( pen!=transparent_pen ) data |=0x80;
/*TODO*///				}
/*TODO*///				if( data!=0x00 ) is_transparent = 0;
/*TODO*///				if( data!=0xff ) is_opaque = 0;
/*TODO*///				mask_dest[x] = data;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else {
/*TODO*///		for( sy=y1; sy!=y2; sy+=dy ){
/*TODO*///			UINT8 *mask_dest  = mask->line[sy]+sx/8;
/*TODO*///			for( x=0; x<tile_width/8; x++ ){
/*TODO*///				UINT32 data = 0;
/*TODO*///				for( bit=0; bit<8; bit++ ){
/*TODO*///					UINT32 pen = *pendata++;
/*TODO*///					data = data<<1;
/*TODO*///					if( pen!=transparent_pen ) data |=0x01;
/*TODO*///				}
/*TODO*///				if( data!=0x00 ) is_transparent = 0;
/*TODO*///				if( data!=0xff ) is_opaque = 0;
/*TODO*///				mask_dest[x] = data;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	if( is_transparent ) return TILE_TRANSPARENT;
/*TODO*///	if( is_opaque ) return TILE_OPAQUE;
/*TODO*///	return TILE_MASKED;
/*TODO*///}
/*TODO*///
    public static void draw_mask(
            mame_bitmap mask,
            int/*UINT32*/ col, int/*UINT32*/ row,
            int/*UINT32*/ tile_width, int/*UINT32*/ tile_height,
            UBytePtr pendata,
            long/*UINT32*/ transmask,
            int/*UINT32*/ flags) {
        int x, bit, sx = tile_width * col;
        int sy, y1, y2, dy;

        if ((flags & TILE_FLIPY) != 0) {
            y1 = tile_height * row + tile_height - 1;
            y2 = y1 - tile_height;
            dy = -1;
        } else {
            y1 = tile_height * row;
            y2 = y1 + tile_height;
            dy = 1;
        }

        if ((flags & TILE_FLIPX) != 0) {
            tile_width--;
            for (sy = y1; sy != y2; sy += dy) {
                UBytePtr mask_dest = new UBytePtr(mask.line[sy], sx / 8);
                for (x = tile_width / 8; x >= 0; x--) {
                    int/*UINT32*/ data = 0;
                    for (bit = 0; bit < 8; bit++) {
                        int/*UINT32*/ pen = pendata.readinc();
                        data = data >>> 1;
                        if (((1 << pen) & transmask) == 0) {
                            data |= 0x80;
                        }
                    }
                    mask_dest.write(x, data);
                }
            }
        } else {
            for (sy = y1; sy != y2; sy += dy) {
                UBytePtr mask_dest = new UBytePtr(mask.line[sy], sx / 8);
                for (x = 0; x < tile_width / 8; x++) {
                    int/*UINT32*/ data = 0;
                    for (bit = 0; bit < 8; bit++) {
                        int/*UINT32*/ pen = pendata.readinc();
                        data = (data << 1);
                        if (((1 << pen) & transmask) == 0) {
                            data |= 0x01;
                        }
                    }
                    mask_dest.write(x, (char) data);
                }
            }
        }
    }

    public static void render_mask(struct_tilemap tilemap, int/*UINT32*/ cached_index) {
        int cache_ptr = cached_index;
        int/*UINT32*/ col = cached_index % tilemap.num_cached_cols;
        int/*UINT32*/ row = cached_index / tilemap.num_cached_cols;
        int/*UINT32*/ type = tilemap.type;

        int/*UINT32*/ transparent_pen = tilemap.transparent_pen;
        int[] transmask = tilemap.u32_transmask;
        int/*UINT32*/ tile_width = tilemap.cached_tile_width;
        int/*UINT32*/ tile_height = tilemap.cached_tile_height;

        int/*UINT32*/ pen_usage = tilemap.cached_tile_info[cache_ptr].u32_pen_usage;
        /*TOCHECK*/ UBytePtr pen_data = new UBytePtr(tilemap.cached_tile_info[cache_ptr].pen_data);
        int/*UINT32*/ flags = tilemap.cached_tile_info[cache_ptr].u32_flags;

        if ((type & TILEMAP_BITMASK) != 0) {
            //throw new UnsupportedOperationException();
            System.out.println("TILEMAP_BITMASK unimplemented");
            /*TODO*///		tilemap->foreground->data_row[row][col] =
/*TODO*///			draw_bitmask( tilemap->foreground->bitmask,col, row,
/*TODO*///				tile_width, tile_height,tile_info.mask_data, flags );
        } else if ((type & TILEMAP_SPLIT) != 0) {

            int/*UINT32*/ pen_mask = (transparent_pen < 0) ? 0 : (1 << transparent_pen);
            if ((flags & TILE_IGNORE_TRANSPARENCY) != 0) {
                throw new UnsupportedOperationException();
                /*TODO*///			tilemap->foreground->data_row[row][col] = TILE_OPAQUE;
/*TODO*///			tilemap->background->data_row[row][col] = TILE_OPAQUE;
            } else if (pen_mask == pen_usage) {
                tilemap.foreground.data_row[row].write(col, TILE_TRANSPARENT);
                tilemap.background.data_row[row].write(col, TILE_TRANSPARENT);
            } else {

                long fg_transmask = transmask[(flags >> 2) & 3] & 0xFFFFFFFFL;
                long /*UINT32*/ bg_transmask = (~fg_transmask) | pen_mask;
                if ((pen_usage & fg_transmask) == 0) {
                    /* foreground totally opaque */
                    tilemap.foreground.data_row[row].write(col, TILE_OPAQUE);
                    tilemap.background.data_row[row].write(col, TILE_TRANSPARENT);
                } else if ((pen_usage & bg_transmask) == 0) {
                    /* background totally opaque */
                    tilemap.foreground.data_row[row].write(col, TILE_TRANSPARENT);
                    tilemap.background.data_row[row].write(col, TILE_OPAQUE);
                } else if ((pen_usage & ~bg_transmask) == 0) {
                    /* background transparent */
                    draw_mask(tilemap.foreground.bitmask,
                            col, row, tile_width, tile_height,
                            new UBytePtr(pen_data), fg_transmask, flags);
                    tilemap.foreground.data_row[row].write(col, TILE_MASKED);
                    tilemap.background.data_row[row].write(col, TILE_TRANSPARENT);
                } else if ((pen_usage & ~fg_transmask) == 0) {
                    /* foreground transparent */
                    draw_mask(tilemap.background.bitmask,
                            col, row, tile_width, tile_height,
                            new UBytePtr(pen_data), bg_transmask, flags);
                    tilemap.foreground.data_row[row].write(col, TILE_TRANSPARENT);
                    tilemap.background.data_row[row].write(col, TILE_MASKED);
                } else {
                    /* split tile - opacity in both foreground and background */
                    draw_mask(tilemap.foreground.bitmask,
                            col, row, tile_width, tile_height,
                            new UBytePtr(pen_data), fg_transmask, flags);
                    draw_mask(tilemap.background.bitmask,
                            col, row, tile_width, tile_height,
                            new UBytePtr(pen_data), bg_transmask, flags);
                    tilemap.foreground.data_row[row].write(col, TILE_MASKED);
                    tilemap.background.data_row[row].write(col, TILE_MASKED);
                }
            }
        } else if (type == TILEMAP_TRANSPARENT) {

            if (pen_usage != 0) {
                int/*UINT32*/ fg_transmask = 1 << transparent_pen;
                if ((flags & TILE_IGNORE_TRANSPARENCY) != 0) {
                    fg_transmask = 0;
                }
                if (pen_usage == fg_transmask) {
                    tilemap.foreground.data_row[row].write(col, TILE_TRANSPARENT);
                } else if ((pen_usage & fg_transmask) != 0) {
                    draw_mask(tilemap.foreground.bitmask,
                            col, row, tile_width, tile_height,
                            new UBytePtr(pen_data), fg_transmask, flags);
                    tilemap.foreground.data_row[row].write(col, TILE_MASKED);
                } else {
                    tilemap.foreground.data_row[row].write(col, TILE_OPAQUE);
                }
            } else {
                throw new UnsupportedOperationException();
                /*TODO*///			tilemap->foreground->data_row[row][col] =
/*TODO*///				draw_pen_mask(
/*TODO*///					tilemap->foreground->bitmask,
/*TODO*///					col, row, tile_width, tile_height,
/*TODO*///					pen_data,
/*TODO*///					transparent_pen,
/*TODO*///					flags
/*TODO*///				);
            }
        } else if (type == TILEMAP_TRANSPARENT_COLOR) {
            tilemap.foreground.data_row[row].write(col,
                    draw_color_mask(
                            tilemap.foreground.bitmask,
                            col, row, tile_width, tile_height,
                            new UBytePtr(pen_data),
                            new UShortArray(Machine.game_colortable,
                                    (tilemap.cached_tile_info[cache_ptr].pal_data.offset - Machine.remapped_colortable.offset)),
                            transparent_pen,
                            flags
                    ));
        } else {
            tilemap.foreground.data_row[row].write(col, TILE_OPAQUE);
        }
    }

    public static void update_tile_info(struct_tilemap tilemap) {
        int[] logical_flip_to_cached_flip = tilemap.logical_flip_to_cached_flip;
        int /*UINT32*/ num_pens = tilemap.cached_tile_width * tilemap.cached_tile_height;
        int /*UINT32*/ num_tiles = tilemap.num_tiles;
        int /*UINT32*/ cached_index;
        int[] u8_visible = tilemap.u8_visible;
        int[] u8_dirty_vram = tilemap.u8_dirty_vram;
        int[] u8_dirty_pixels = tilemap.u8_dirty_pixels;
        tile_info.flags = 0;
        tile_info.priority = 0;
        for (cached_index = 0; cached_index < num_tiles; cached_index++) {
            if (u8_visible[cached_index] != 0 && u8_dirty_vram[cached_index] != 0) {
                int cache_ptr = cached_index;
                int /*UINT32*/ memory_offset = tilemap.cached_indx_to_memory_offset[cached_index];
                unregister_pens(tilemap, cache_ptr, num_pens);
                tilemap.tile_get_info.handler(memory_offset);
                {
                    int /*UINT32*/ flags = tile_info.flags;
                    tilemap.cached_tile_info[cache_ptr].u32_flags = (flags & 0xfc) | logical_flip_to_cached_flip[flags & 0x3];
                }
                tilemap.cached_tile_info[cache_ptr].u32_pen_usage = tile_info.pen_usage;
                /*TOCHECK*/ tilemap.cached_tile_info[cache_ptr].pen_data = tile_info.pen_data;
                /*TOCHECK*/ tilemap.cached_tile_info[cache_ptr].pal_data = tile_info.pal_data;
                tilemap.u8_priority[cached_index] = (char) tile_info.priority;
                register_pens(tilemap, cache_ptr, num_pens);
                u8_dirty_pixels[cached_index] = 1;
                u8_dirty_vram[cached_index] = 0;
                render_mask(tilemap, cached_index);
            }
        }
    }

    public static void update_visible(struct_tilemap tilemap) {
        // temporary hack
        memset(tilemap.u8_visible, 1, tilemap.num_tiles);
    }

    public static void tilemap_update(struct_tilemap tilemap) {
        if (tilemap == ALL_TILEMAPS) {
            tilemap = first_tilemap;
            while (tilemap != null) {
                tilemap_update(tilemap);
                tilemap = tilemap.next;
            }
        } else if (tilemap.enable != 0) {
            update_visible(tilemap);
            update_tile_info(tilemap);
        }
    }

    /**
     * ********************************************************************************
     */
    public static void tilemap_set_scrolldx(struct_tilemap tilemap, int dx, int dx_if_flipped) {
        tilemap.dx = dx;
        tilemap.dx_if_flipped = dx_if_flipped;
        tilemap.scrollx_delta = (tilemap.attributes & TILEMAP_FLIPX) != 0 ? dx_if_flipped : dx;
    }

    public static void tilemap_set_scrolldy(struct_tilemap tilemap, int dy, int dy_if_flipped) {
        tilemap.dy = dy;
        tilemap.dy_if_flipped = dy_if_flipped;
        tilemap.scrolly_delta = (tilemap.attributes & TILEMAP_FLIPY) != 0 ? dy_if_flipped : dy;
    }

    public static void tilemap_set_scrollx(struct_tilemap tilemap, int which, int value) {
        value = tilemap.scrollx_delta - value;

        if ((tilemap.orientation & ORIENTATION_SWAP_XY) != 0) {
            if ((tilemap.orientation & ORIENTATION_FLIP_X) != 0) {
                which = tilemap.scroll_cols - 1 - which;
            }
            if ((tilemap.orientation & ORIENTATION_FLIP_Y) != 0) {
                value = screen_height - tilemap.cached_height - value;
            }
            if (tilemap.colscroll[which] != value) {
                tilemap.colscroll[which] = value;
            }
        } else {
            if ((tilemap.orientation & ORIENTATION_FLIP_Y) != 0) {
                which = tilemap.scroll_rows - 1 - which;
            }
            if ((tilemap.orientation & ORIENTATION_FLIP_X) != 0) {
                value = screen_width - tilemap.cached_width - value;
            }
            if (tilemap.rowscroll[which] != value) {
                tilemap.rowscroll[which] = value;
            }
        }
    }

    public static void tilemap_set_scrolly(struct_tilemap tilemap, int which, int value) {
        value = tilemap.scrolly_delta - value;

        if ((tilemap.orientation & ORIENTATION_SWAP_XY) != 0) {
            if ((tilemap.orientation & ORIENTATION_FLIP_Y) != 0) {
                which = tilemap.scroll_rows - 1 - which;
            }
            if ((tilemap.orientation & ORIENTATION_FLIP_X) != 0) {
                value = screen_width - tilemap.cached_width - value;
            }
            if (tilemap.rowscroll[which] != value) {
                tilemap.rowscroll[which] = value;
            }
        } else {
            if ((tilemap.orientation & ORIENTATION_FLIP_X) != 0) {
                which = tilemap.scroll_cols - 1 - which;
            }
            if ((tilemap.orientation & ORIENTATION_FLIP_Y) != 0) {
                value = screen_height - tilemap.cached_height - value;
            }
            if (tilemap.colscroll[which] != value) {
                tilemap.colscroll[which] = value;
            }
        }
    }

    /**
     * ********************************************************************************
     */
    public static void tilemap_draw(mame_bitmap dest, struct_tilemap tilemap, int priority) {
        int xpos, ypos;
        if (tilemap.enable != 0) {
            WriteHandlerPtr draw;
            int rows = tilemap.scroll_rows;
            int[] rowscroll = tilemap.rowscroll;
            int cols = tilemap.scroll_cols;
            int[] colscroll = tilemap.colscroll;

            int left = tilemap.clip_left;
            int right = tilemap.clip_right;
            int top = tilemap.clip_top;
            int bottom = tilemap.clip_bottom;

            int tile_height = tilemap.cached_tile_height;

            blit.screen = dest;
            blit.dest_line_offset = dest.line[1].offset - dest.line[0].offset;

            blit.pixmap = tilemap.pixmap;
            blit.source_line_offset = tilemap.pixmap_line_offset;

            if (tilemap.type == TILEMAP_OPAQUE || (priority & TILEMAP_IGNORE_TRANSPARENCY) != 0) {
                draw = tilemap.draw_opaque;
            } else {
                draw = tilemap.draw037;
                if ((priority & TILEMAP_BACK) != 0) {
                    blit.bitmask = tilemap.background.bitmask;
                    blit.mask_line_offset = tilemap.background.line_offset;
                    blit.mask_data_row = tilemap.background.data_row;
                } else {
                    blit.bitmask = tilemap.foreground.bitmask;
                    blit.mask_line_offset = tilemap.foreground.line_offset;
                    blit.mask_data_row = tilemap.foreground.data_row;
                }

                blit.mask_row_offset = tile_height * blit.mask_line_offset;
            }

            if (dest.depth == 16) {
                blit.dest_line_offset /= 2;
                blit.source_line_offset /= 2;
            }

            blit.source_row_offset = tile_height * blit.source_line_offset;
            blit.dest_row_offset = tile_height * blit.dest_line_offset;

            blit.priority_data_row = tilemap.priority_row;
            blit.source_width = tilemap.cached_width;
            blit.source_height = tilemap.cached_height;
            blit.tile_priority = priority & 0xf;
            blit.tilemap_priority_code = priority >>> 16;

            if (rows == 1 && cols == 1) {
                /* XY scrolling playfield */
                int scrollx = rowscroll[0];
                int scrolly = colscroll[0];

                if (scrollx < 0) {
                    scrollx = blit.source_width - (-scrollx) % blit.source_width;
                } else {
                    scrollx = scrollx % blit.source_width;
                }

                if (scrolly < 0) {
                    scrolly = blit.source_height - (-scrolly) % blit.source_height;
                } else {
                    scrolly = scrolly % blit.source_height;
                }

                blit.clip_left = left;
                blit.clip_top = top;
                blit.clip_right = right;
                blit.clip_bottom = bottom;

                for (ypos = scrolly - blit.source_height;
                        ypos < blit.clip_bottom;
                        ypos += blit.source_height) {
                    for (xpos = scrollx - blit.source_width;
                            xpos < blit.clip_right;
                            xpos += blit.source_width) {
                        draw.handler(xpos, ypos);
                    }
                }
            } else if (rows == 1) {
                /* scrolling columns + horizontal scroll */
                int col = 0;
                int colwidth = blit.source_width / cols;
                int scrollx = rowscroll[0];

                if (scrollx < 0) {
                    scrollx = blit.source_width - (-scrollx) % blit.source_width;
                } else {
                    scrollx = scrollx % blit.source_width;
                }

                blit.clip_top = top;
                blit.clip_bottom = bottom;

                while (col < cols) {
                    int cons = 1;
                    int scrolly = colscroll[col];

                    /* count consecutive columns scrolled by the same amount */
                    if (scrolly != TILE_LINE_DISABLED) {
                        while (col + cons < cols && colscroll[col + cons] == scrolly) {
                            cons++;
                        }

                        if (scrolly < 0) {
                            scrolly = blit.source_height - (-scrolly) % blit.source_height;
                        } else {
                            scrolly %= blit.source_height;
                        }

                        blit.clip_left = col * colwidth + scrollx;
                        if (blit.clip_left < left) {
                            blit.clip_left = left;
                        }
                        blit.clip_right = (col + cons) * colwidth + scrollx;
                        if (blit.clip_right > right) {
                            blit.clip_right = right;
                        }

                        for (ypos = scrolly - blit.source_height;
                                ypos < blit.clip_bottom;
                                ypos += blit.source_height) {
                            draw.handler(scrollx, ypos);
                        }

                        blit.clip_left = col * colwidth + scrollx - blit.source_width;
                        if (blit.clip_left < left) {
                            blit.clip_left = left;
                        }
                        blit.clip_right = (col + cons) * colwidth + scrollx - blit.source_width;
                        if (blit.clip_right > right) {
                            blit.clip_right = right;
                        }

                        for (ypos = scrolly - blit.source_height;
                                ypos < blit.clip_bottom;
                                ypos += blit.source_height) {
                            draw.handler(scrollx - blit.source_width, ypos);
                        }
                    }
                    col += cons;
                }
            } else if (cols == 1) {
                /* scrolling rows + vertical scroll */
                int row = 0;
                int rowheight = blit.source_height / rows;
                int scrolly = colscroll[0];
                if (scrolly < 0) {
                    scrolly = blit.source_height - (-scrolly) % blit.source_height;
                } else {
                    scrolly = scrolly % blit.source_height;
                }
                blit.clip_left = left;
                blit.clip_right = right;
                while (row < rows) {
                    int cons = 1;
                    int scrollx = rowscroll[row];
                    /* count consecutive rows scrolled by the same amount */
                    if (scrollx != TILE_LINE_DISABLED) {
                        while (row + cons < rows && rowscroll[row + cons] == scrollx) {
                            cons++;
                        }
                        if (scrollx < 0) {
                            scrollx = blit.source_width - (-scrollx) % blit.source_width;
                        } else {
                            scrollx %= blit.source_width;
                        }
                        blit.clip_top = row * rowheight + scrolly;
                        if (blit.clip_top < top) {
                            blit.clip_top = top;
                        }
                        blit.clip_bottom = (row + cons) * rowheight + scrolly;
                        if (blit.clip_bottom > bottom) {
                            blit.clip_bottom = bottom;
                        }
                        for (xpos = scrollx - blit.source_width;
                                xpos < blit.clip_right;
                                xpos += blit.source_width) {
                            draw.handler(xpos, scrolly);
                        }
                        blit.clip_top = row * rowheight + scrolly - blit.source_height;
                        if (blit.clip_top < top) {
                            blit.clip_top = top;
                        }
                        blit.clip_bottom = (row + cons) * rowheight + scrolly - blit.source_height;
                        if (blit.clip_bottom > bottom) {
                            blit.clip_bottom = bottom;
                        }
                        for (xpos = scrollx - blit.source_width;
                                xpos < blit.clip_right;
                                xpos += blit.source_width) {
                            draw.handler(xpos, scrolly - blit.source_height);
                        }
                    }
                    row += cons;
                }
            }
        }

    }

    /**
     * ********************************************************************************
     */
    public static WriteHandlerPtr draw8x8x8BPP = new WriteHandlerPtr() {
        public void handler(int xpos, int ypos) {
            generic8draw(xpos, ypos, 8, 8);
        }
    };
    public static WriteHandlerPtr draw16x16x8BPP = new WriteHandlerPtr() {
        public void handler(int xpos, int ypos) {
            generic8draw(xpos, ypos, 16, 16);
        }
    };
    public static WriteHandlerPtr draw32x32x8BPP = new WriteHandlerPtr() {
        public void handler(int xpos, int ypos) {
            generic8draw(xpos, ypos, 32, 32);
        }
    };

    public static void generic8draw(int xpos, int ypos, int TILE_WIDTH, int TILE_HEIGHT) {
        int tilemap_priority_code = blit.tilemap_priority_code;
        int x1 = xpos;
        int y1 = ypos;
        int x2 = xpos + blit.source_width;
        int y2 = ypos + blit.source_height;

        /* clip source coordinates */
        if (x1 < blit.clip_left) {
            x1 = blit.clip_left;
        }
        if (x2 > blit.clip_right) {
            x2 = blit.clip_right;
        }
        if (y1 < blit.clip_top) {
            y1 = blit.clip_top;
        }
        if (y2 > blit.clip_bottom) {
            y2 = blit.clip_bottom;
        }
        
        //System.out.println("priority_bitmap_line_offset="+priority_bitmap_line_offset);
        //System.out.println("TILE_HEIGHT="+TILE_HEIGHT);
      
        if (x1 < x2 && y1 < y2) {
            /* do nothing if totally clipped */
            UBytePtr dest_baseaddr = new UBytePtr(blit.screen.line[y1], xpos);
            UBytePtr dest_next;

            int priority_bitmap_row_offset = priority_bitmap_line_offset * TILE_HEIGHT;
            UBytePtr priority_bitmap_baseaddr = new UBytePtr(priority_bitmap.line[y1], xpos);
            UBytePtr priority_bitmap_next;

            int priority = blit.tile_priority;
            UBytePtr source_baseaddr;
            UBytePtr source_next;
            UBytePtr mask_baseaddr;
            UBytePtr mask_next;

            int c1;
            int c2;
            /* leftmost and rightmost visible columns in source tilemap */
            int y;
            /* current screen line to render */
            int y_next;

            /* convert screen coordinates to source tilemap coordinates */
            x1 -= xpos;
            y1 -= ypos;
            x2 -= xpos;
            y2 -= ypos;

            source_baseaddr = new UBytePtr(blit.pixmap.line[y1]);
            mask_baseaddr = new UBytePtr(blit.bitmask.line[y1]);

            c1 = x1 / TILE_WIDTH;
            /* round down */
            c2 = (x2 + TILE_WIDTH - 1) / TILE_WIDTH;
            /* round up */

            y = y1;
            y_next = TILE_HEIGHT * (y1 / TILE_HEIGHT) + TILE_HEIGHT;
            if (y_next > y2) {
                y_next = y2;
            }

            {
                int dy = y_next - y;
                dest_next = new UBytePtr(dest_baseaddr, dy * blit.dest_line_offset);
                priority_bitmap_next = new UBytePtr(priority_bitmap_baseaddr, dy * priority_bitmap_line_offset);
                source_next = new UBytePtr(source_baseaddr, dy * blit.source_line_offset);
                mask_next = new UBytePtr(mask_baseaddr, dy * blit.mask_line_offset);
            }

            for (;;) {
                int row = y / TILE_HEIGHT;
                UBytePtr mask_data = new UBytePtr(blit.mask_data_row[row]);
                UBytePtr priority_data = new UBytePtr(blit.priority_data_row[row]);

                int tile_type;
                int prev_tile_type = TILE_TRANSPARENT;

                int x_start = x1;
                int x_end;

                int column;
                for (column = c1; column <= c2; column++) {
                    if (column == c2 || priority_data.read(column) != priority) {
                        tile_type = TILE_TRANSPARENT;
                    } else {
                        tile_type = mask_data.read(column);
                    }

                    if (tile_type != prev_tile_type) {
                        x_end = column * TILE_WIDTH;
                        if (x_end < x1) {
                            x_end = x1;
                        }
                        if (x_end > x2) {
                            x_end = x2;
                        }

                        if (prev_tile_type != TILE_TRANSPARENT) {
                            if (prev_tile_type == TILE_MASKED) {
                                //System.out.println("A");
                                int count = (x_end + 7) / 8 - x_start / 8;
                                UBytePtr mask0 = new UBytePtr(mask_baseaddr, x_start / 8);
                                UBytePtr source0 = new UBytePtr(source_baseaddr, (x_start & 0xfff8));
                                UBytePtr dest0 = new UBytePtr(dest_baseaddr, (x_start & 0xfff8));
                                UBytePtr pmap0 = new UBytePtr(priority_bitmap_baseaddr, (x_start & 0xfff8));
                                int i = y;
                                for (;;) {
                                    memcpybitmask8(new UBytePtr(dest0), new UBytePtr(source0), new UBytePtr(mask0), count);
                                    memsetbitmask8(new UBytePtr(pmap0), tilemap_priority_code, new UBytePtr(mask0), count);
                                    if (++i == y_next) {
                                        break;
                                    }

                                    dest0.offset += blit.dest_line_offset;
                                    source0.offset += blit.source_line_offset;
                                    mask0.offset += blit.mask_line_offset;
                                    pmap0.offset += priority_bitmap_line_offset;
                                }
                            } else {
                                //System.out.println("B");
                                /* TILE_OPAQUE */
                                int num_pixels = x_end - x_start;
                                UBytePtr dest0 = new UBytePtr(dest_baseaddr, x_start);
                                UBytePtr source0 = new UBytePtr(source_baseaddr, x_start);
                                UBytePtr pmap0 = new UBytePtr(priority_bitmap_baseaddr, x_start);
                                int i = y;
                                for (;;) {
                                    memcpy(dest0, source0, num_pixels);
                                    memset(pmap0, tilemap_priority_code, num_pixels);
                                    if (++i == y_next) {
                                        break;
                                    }

                                    dest0.offset += blit.dest_line_offset;
                                    source0.offset += blit.source_line_offset;
                                    pmap0.offset += priority_bitmap_line_offset;
                                }
                            }
                        }
                        x_start = x_end;
                    }

                    prev_tile_type = tile_type;
                }

                if (y_next == y2) {
                    break;
                    /* we are done! */
                }

                priority_bitmap_baseaddr = new UBytePtr(priority_bitmap_next);
                dest_baseaddr = new UBytePtr(dest_next);
                source_baseaddr = new UBytePtr(source_next);
                mask_baseaddr = new UBytePtr(mask_next);

                y = y_next;
                y_next += TILE_HEIGHT;

                if (y_next >= y2) {
                    y_next = y2;
                } else {
                    dest_next.offset += blit.dest_row_offset;
                    priority_bitmap_next.offset += priority_bitmap_row_offset;
                    source_next.offset += blit.source_row_offset;
                    mask_next.offset += blit.mask_row_offset;
                }
            }
            /* process next row */
        }
        /* not totally clipped */
    }
    /*TODO*///DECLARE( draw, (int xpos, int ypos),
/*TODO*///{
/*TODO*///	int tilemap_priority_code = blit.tilemap_priority_code;
/*TODO*///	int x1 = xpos;
/*TODO*///	int y1 = ypos;
/*TODO*///	int x2 = xpos+blit.source_width;
/*TODO*///	int y2 = ypos+blit.source_height;
/*TODO*///
/*TODO*///	/* clip source coordinates */
/*TODO*///	if( x1<blit.clip_left ) x1 = blit.clip_left;
/*TODO*///	if( x2>blit.clip_right ) x2 = blit.clip_right;
/*TODO*///	if( y1<blit.clip_top ) y1 = blit.clip_top;
/*TODO*///	if( y2>blit.clip_bottom ) y2 = blit.clip_bottom;
/*TODO*///
/*TODO*///	if( x1<x2 && y1<y2 ){ /* do nothing if totally clipped */
/*TODO*///		DATA_TYPE *dest_baseaddr = xpos + (DATA_TYPE *)blit.screen->line[y1];
/*TODO*///		DATA_TYPE *dest_next;
/*TODO*///
/*TODO*///		int priority_bitmap_row_offset = priority_bitmap_line_offset*TILE_HEIGHT;
/*TODO*///		UINT8 *priority_bitmap_baseaddr = xpos + (UINT8 *)priority_bitmap->line[y1];
/*TODO*///		UINT8 *priority_bitmap_next;
/*TODO*///
/*TODO*///		int priority = blit.tile_priority;
/*TODO*///		const DATA_TYPE *source_baseaddr;
/*TODO*///		const DATA_TYPE *source_next;
/*TODO*///		const UINT8 *mask_baseaddr;
/*TODO*///		const UINT8 *mask_next;
/*TODO*///
/*TODO*///		int c1;
/*TODO*///		int c2; /* leftmost and rightmost visible columns in source tilemap */
/*TODO*///		int y; /* current screen line to render */
/*TODO*///		int y_next;
/*TODO*///
/*TODO*///		/* convert screen coordinates to source tilemap coordinates */
/*TODO*///		x1 -= xpos;
/*TODO*///		y1 -= ypos;
/*TODO*///		x2 -= xpos;
/*TODO*///		y2 -= ypos;
/*TODO*///
/*TODO*///		source_baseaddr = (DATA_TYPE *)blit.pixmap->line[y1];
/*TODO*///		mask_baseaddr = blit.bitmask->line[y1];
/*TODO*///
/*TODO*///		c1 = x1/TILE_WIDTH; /* round down */
/*TODO*///		c2 = (x2+TILE_WIDTH-1)/TILE_WIDTH; /* round up */
/*TODO*///
/*TODO*///		y = y1;
/*TODO*///		y_next = TILE_HEIGHT*(y1/TILE_HEIGHT) + TILE_HEIGHT;
/*TODO*///		if( y_next>y2 ) y_next = y2;
/*TODO*///
/*TODO*///		{
/*TODO*///			int dy = y_next-y;
/*TODO*///			dest_next = dest_baseaddr + dy*blit.dest_line_offset;
/*TODO*///			priority_bitmap_next = priority_bitmap_baseaddr + dy*priority_bitmap_line_offset;
/*TODO*///			source_next = source_baseaddr + dy*blit.source_line_offset;
/*TODO*///			mask_next = mask_baseaddr + dy*blit.mask_line_offset;
/*TODO*///		}
/*TODO*///
/*TODO*///		for(;;){
/*TODO*///			int row = y/TILE_HEIGHT;
/*TODO*///			UINT8 *mask_data = blit.mask_data_row[row];
/*TODO*///			UINT8 *priority_data = blit.priority_data_row[row];
/*TODO*///
/*TODO*///			int tile_type;
/*TODO*///			int prev_tile_type = TILE_TRANSPARENT;
/*TODO*///
/*TODO*///			int x_start = x1;
/*TODO*///			int x_end;
/*TODO*///
/*TODO*///			int column;
/*TODO*///			for( column=c1; column<=c2; column++ ){
/*TODO*///				if( column==c2 || priority_data[column]!=priority )
/*TODO*///					tile_type = TILE_TRANSPARENT;
/*TODO*///				else
/*TODO*///					tile_type = mask_data[column];
/*TODO*///
/*TODO*///				if( tile_type!=prev_tile_type ){
/*TODO*///					x_end = column*TILE_WIDTH;
/*TODO*///					if( x_end<x1 ) x_end = x1;
/*TODO*///					if( x_end>x2 ) x_end = x2;
/*TODO*///
/*TODO*///					if( prev_tile_type != TILE_TRANSPARENT ){
/*TODO*///						if( prev_tile_type == TILE_MASKED ){
/*TODO*///							int count = (x_end+7)/8 - x_start/8;
/*TODO*///							const UINT8 *mask0 = mask_baseaddr + x_start/8;
/*TODO*///							const DATA_TYPE *source0 = source_baseaddr + (x_start&0xfff8);
/*TODO*///							DATA_TYPE *dest0 = dest_baseaddr + (x_start&0xfff8);
/*TODO*///							UINT8 *pmap0 = priority_bitmap_baseaddr + (x_start&0xfff8);
/*TODO*///							int i = y;
/*TODO*///							for(;;){
/*TODO*///								memcpybitmask( dest0, source0, mask0, count );
/*TODO*///								memsetbitmask8( pmap0, tilemap_priority_code, mask0, count );
/*TODO*///								if( ++i == y_next ) break;
/*TODO*///
/*TODO*///								dest0 += blit.dest_line_offset;
/*TODO*///								source0 += blit.source_line_offset;
/*TODO*///								mask0 += blit.mask_line_offset;
/*TODO*///								pmap0 += priority_bitmap_line_offset;
/*TODO*///							}
/*TODO*///						}
/*TODO*///						else { /* TILE_OPAQUE */
/*TODO*///							int num_pixels = x_end - x_start;
/*TODO*///							DATA_TYPE *dest0 = dest_baseaddr+x_start;
/*TODO*///							const DATA_TYPE *source0 = source_baseaddr+x_start;
/*TODO*///							UINT8 *pmap0 = priority_bitmap_baseaddr + x_start;
/*TODO*///							int i = y;
/*TODO*///							for(;;){
/*TODO*///								memcpy( dest0, source0, num_pixels*sizeof(DATA_TYPE) );
/*TODO*///								memset( pmap0, tilemap_priority_code, num_pixels );
/*TODO*///								if( ++i == y_next ) break;
/*TODO*///
/*TODO*///								dest0 += blit.dest_line_offset;
/*TODO*///								source0 += blit.source_line_offset;
/*TODO*///								pmap0 += priority_bitmap_line_offset;
/*TODO*///							}
/*TODO*///						}
/*TODO*///					}
/*TODO*///					x_start = x_end;
/*TODO*///				}
/*TODO*///
/*TODO*///				prev_tile_type = tile_type;
/*TODO*///			}
/*TODO*///
/*TODO*///			if( y_next==y2 ) break; /* we are done! */
/*TODO*///
/*TODO*///			priority_bitmap_baseaddr = priority_bitmap_next;
/*TODO*///			dest_baseaddr = dest_next;
/*TODO*///			source_baseaddr = source_next;
/*TODO*///			mask_baseaddr = mask_next;
/*TODO*///
/*TODO*///			y = y_next;
/*TODO*///			y_next += TILE_HEIGHT;
/*TODO*///
/*TODO*///			if( y_next>=y2 ){
/*TODO*///				y_next = y2;
/*TODO*///			}
/*TODO*///			else {
/*TODO*///				dest_next += blit.dest_row_offset;
/*TODO*///				priority_bitmap_next += priority_bitmap_row_offset;
/*TODO*///				source_next += blit.source_row_offset;
/*TODO*///				mask_next += blit.mask_row_offset;
/*TODO*///			}
/*TODO*///		} /* process next row */
/*TODO*///	} /* not totally clipped */
/*TODO*///})
/*TODO*///
    public static WriteHandlerPtr draw_opaque8x8x8BPP = new WriteHandlerPtr() {
        public void handler(int xpos, int ypos) {
            generic8draw_opaque(xpos, ypos, 8, 8);
        }
    };
    public static WriteHandlerPtr draw_opaque16x16x8BPP = new WriteHandlerPtr() {
        public void handler(int xpos, int ypos) {
            generic8draw_opaque(xpos, ypos, 16, 16);
        }
    };
    public static WriteHandlerPtr draw_opaque32x32x8BPP = new WriteHandlerPtr() {
        public void handler(int xpos, int ypos) {
            generic8draw_opaque(xpos, ypos, 32, 32);
        }
    };

    public static void generic8draw_opaque(int xpos, int ypos, int TILE_WIDTH, int TILE_HEIGHT) {
        int tilemap_priority_code = blit.tilemap_priority_code;
        int x1 = xpos;
        int y1 = ypos;
        int x2 = xpos + blit.source_width;
        int y2 = ypos + blit.source_height;
        /* clip source coordinates */
        if (x1 < blit.clip_left) {
            x1 = blit.clip_left;
        }
        if (x2 > blit.clip_right) {
            x2 = blit.clip_right;
        }
        if (y1 < blit.clip_top) {
            y1 = blit.clip_top;
        }
        if (y2 > blit.clip_bottom) {
            y2 = blit.clip_bottom;
        }

        if (x1 < x2 && y1 < y2) {
            /* do nothing if totally clipped */
            UBytePtr priority_bitmap_baseaddr = new UBytePtr(priority_bitmap.line[y1], xpos);
            int priority_bitmap_row_offset = priority_bitmap_line_offset * TILE_HEIGHT;

            int priority = blit.tile_priority;
            UBytePtr dest_baseaddr = new UBytePtr(blit.screen.line[y1], xpos);
            UBytePtr dest_next;
            UBytePtr source_baseaddr;
            UBytePtr source_next;

            int c1;
            int c2;
            /* leftmost and rightmost visible columns in source tilemap */
            int y;
            /* current screen line to render */
            int y_next;

            /* convert screen coordinates to source tilemap coordinates */
            x1 -= xpos;
            y1 -= ypos;
            x2 -= xpos;
            y2 -= ypos;

            source_baseaddr = new UBytePtr(blit.pixmap.line[y1]);

            c1 = x1 / TILE_WIDTH;
            /* round down */
            c2 = (x2 + TILE_WIDTH - 1) / TILE_WIDTH;
            /* round up */

            y = y1;
            y_next = TILE_HEIGHT * (y1 / TILE_HEIGHT) + TILE_HEIGHT;
            if (y_next > y2) {
                y_next = y2;
            }

            {
                int dy = y_next - y;
                dest_next = new UBytePtr(dest_baseaddr, dy * blit.dest_line_offset);
                source_next = new UBytePtr(source_baseaddr, dy * blit.source_line_offset);
            }

            for (;;) {
                int row = y / TILE_HEIGHT;
                UBytePtr priority_data = new UBytePtr(blit.priority_data_row[row]);

                int tile_type;
                int prev_tile_type = TILE_TRANSPARENT;

                int x_start = x1;
                int x_end;

                int column;
                for (column = c1; column <= c2; column++) {
                    if (column == c2 || priority_data.read(column) != priority) {
                        tile_type = TILE_TRANSPARENT;
                    } else {
                        tile_type = TILE_OPAQUE;
                    }

                    if (tile_type != prev_tile_type) {
                        x_end = column * TILE_WIDTH;
                        if (x_end < x1) {
                            x_end = x1;
                        }
                        if (x_end > x2) {
                            x_end = x2;
                        }

                        if (prev_tile_type != TILE_TRANSPARENT) {
                            /* TILE_OPAQUE */
                            int num_pixels = x_end - x_start;
                            UBytePtr dest0 = new UBytePtr(dest_baseaddr, x_start);
                            UBytePtr pmap0 = new UBytePtr(priority_bitmap_baseaddr, x_start);
                            UBytePtr source0 = new UBytePtr(source_baseaddr, x_start);
                            int i = y;
                            for (;;) {
                                memcpy(new UBytePtr(dest0), new UBytePtr(source0), num_pixels);
                                memset(new UBytePtr(pmap0), tilemap_priority_code, num_pixels);
                                if (++i == y_next) {
                                    break;
                                }

                                dest0.offset += blit.dest_line_offset;
                                pmap0.offset += priority_bitmap_line_offset;
                                source0.offset += blit.source_line_offset;
                            }
                        }
                        x_start = x_end;
                    }

                    prev_tile_type = tile_type;
                }

                if (y_next == y2) {
                    break;
                    /* we are done! */
                }

                priority_bitmap_baseaddr.offset += priority_bitmap_row_offset;
                dest_baseaddr = new UBytePtr(dest_next);
                source_baseaddr = new UBytePtr(source_next);

                y = y_next;
                y_next += TILE_HEIGHT;

                if (y_next >= y2) {
                    y_next = y2;
                } else {
                    dest_next.offset += blit.dest_row_offset;
                    source_next.offset += blit.source_row_offset;
                }
            }
            /* process next row */
        }
        /* not totally clipped */
    }
    /*TODO*///DECLARE( draw_opaque, (int xpos, int ypos),
/*TODO*///{
/*TODO*///	int tilemap_priority_code = blit.tilemap_priority_code;
/*TODO*///	int x1 = xpos;
/*TODO*///	int y1 = ypos;
/*TODO*///	int x2 = xpos+blit.source_width;
/*TODO*///	int y2 = ypos+blit.source_height;
/*TODO*///	/* clip source coordinates */
/*TODO*///	if( x1<blit.clip_left ) x1 = blit.clip_left;
/*TODO*///	if( x2>blit.clip_right ) x2 = blit.clip_right;
/*TODO*///	if( y1<blit.clip_top ) y1 = blit.clip_top;
/*TODO*///	if( y2>blit.clip_bottom ) y2 = blit.clip_bottom;
/*TODO*///
/*TODO*///	if( x1<x2 && y1<y2 ){ /* do nothing if totally clipped */
/*TODO*///		UINT8 *priority_bitmap_baseaddr = xpos + (UINT8 *)priority_bitmap->line[y1];
/*TODO*///		int priority_bitmap_row_offset = priority_bitmap_line_offset*TILE_HEIGHT;
/*TODO*///
/*TODO*///		int priority = blit.tile_priority;
/*TODO*///		DATA_TYPE *dest_baseaddr = xpos + (DATA_TYPE *)blit.screen->line[y1];
/*TODO*///		DATA_TYPE *dest_next;
/*TODO*///		const DATA_TYPE *source_baseaddr;
/*TODO*///		const DATA_TYPE *source_next;
/*TODO*///
/*TODO*///		int c1;
/*TODO*///		int c2; /* leftmost and rightmost visible columns in source tilemap */
/*TODO*///		int y; /* current screen line to render */
/*TODO*///		int y_next;
/*TODO*///
/*TODO*///		/* convert screen coordinates to source tilemap coordinates */
/*TODO*///		x1 -= xpos;
/*TODO*///		y1 -= ypos;
/*TODO*///		x2 -= xpos;
/*TODO*///		y2 -= ypos;
/*TODO*///
/*TODO*///		source_baseaddr = (DATA_TYPE *)blit.pixmap->line[y1];
/*TODO*///
/*TODO*///		c1 = x1/TILE_WIDTH; /* round down */
/*TODO*///		c2 = (x2+TILE_WIDTH-1)/TILE_WIDTH; /* round up */
/*TODO*///
/*TODO*///		y = y1;
/*TODO*///		y_next = TILE_HEIGHT*(y1/TILE_HEIGHT) + TILE_HEIGHT;
/*TODO*///		if( y_next>y2 ) y_next = y2;
/*TODO*///
/*TODO*///		{
/*TODO*///			int dy = y_next-y;
/*TODO*///			dest_next = dest_baseaddr + dy*blit.dest_line_offset;
/*TODO*///			source_next = source_baseaddr + dy*blit.source_line_offset;
/*TODO*///		}
/*TODO*///
/*TODO*///		for(;;){
/*TODO*///			int row = y/TILE_HEIGHT;
/*TODO*///			UINT8 *priority_data = blit.priority_data_row[row];
/*TODO*///
/*TODO*///			int tile_type;
/*TODO*///			int prev_tile_type = TILE_TRANSPARENT;
/*TODO*///
/*TODO*///			int x_start = x1;
/*TODO*///			int x_end;
/*TODO*///
/*TODO*///			int column;
/*TODO*///			for( column=c1; column<=c2; column++ ){
/*TODO*///				if( column==c2 || priority_data[column]!=priority )
/*TODO*///					tile_type = TILE_TRANSPARENT;
/*TODO*///				else
/*TODO*///					tile_type = TILE_OPAQUE;
/*TODO*///
/*TODO*///				if( tile_type!=prev_tile_type ){
/*TODO*///					x_end = column*TILE_WIDTH;
/*TODO*///					if( x_end<x1 ) x_end = x1;
/*TODO*///					if( x_end>x2 ) x_end = x2;
/*TODO*///
/*TODO*///					if( prev_tile_type != TILE_TRANSPARENT ){
/*TODO*///						/* TILE_OPAQUE */
/*TODO*///						int num_pixels = x_end - x_start;
/*TODO*///						DATA_TYPE *dest0 = dest_baseaddr+x_start;
/*TODO*///						UINT8 *pmap0 = priority_bitmap_baseaddr+x_start;
/*TODO*///						const DATA_TYPE *source0 = source_baseaddr+x_start;
/*TODO*///						int i = y;
/*TODO*///						for(;;){
/*TODO*///							memcpy( dest0, source0, num_pixels*sizeof(DATA_TYPE) );
/*TODO*///							memset( pmap0, tilemap_priority_code, num_pixels );
/*TODO*///							if( ++i == y_next ) break;
/*TODO*///
/*TODO*///							dest0 += blit.dest_line_offset;
/*TODO*///							pmap0 += priority_bitmap_line_offset;
/*TODO*///							source0 += blit.source_line_offset;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					x_start = x_end;
/*TODO*///				}
/*TODO*///
/*TODO*///				prev_tile_type = tile_type;
/*TODO*///			}
/*TODO*///
/*TODO*///			if( y_next==y2 ) break; /* we are done! */
/*TODO*///
/*TODO*///			priority_bitmap_baseaddr += priority_bitmap_row_offset;
/*TODO*///			dest_baseaddr = dest_next;
/*TODO*///			source_baseaddr = source_next;
/*TODO*///
/*TODO*///			y = y_next;
/*TODO*///			y_next += TILE_HEIGHT;
/*TODO*///
/*TODO*///			if( y_next>=y2 ){
/*TODO*///				y_next = y2;
/*TODO*///			}
/*TODO*///			else {
/*TODO*///				dest_next += blit.dest_row_offset;
/*TODO*///				source_next += blit.source_row_offset;
/*TODO*///			}
/*TODO*///		} /* process next row */
/*TODO*///	} /* not totally clipped */
/*TODO*///})
/*TODO*///
/*TODO*///#undef TILE_WIDTH
/*TODO*///#undef TILE_HEIGHT
/*TODO*///#undef DATA_TYPE
/*TODO*///#undef memcpybitmask
/*TODO*///#undef DECLARE
/*TODO*///
/*TODO*///#endif /* DECLARE */
/*TODO*///    
}
