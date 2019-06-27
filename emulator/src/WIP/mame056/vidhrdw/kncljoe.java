/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
/**
 * Changelog
 * ---------
 * 07/05/2019 - WIP kncljoe driver (not tilemaps) (shadow)
 */
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;
import static common.libc.expressions.*;

import static mame056.common.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.tilemapH.*;
/*TODO*///import static mame056.tilemapC.*;
import static mame037b11.mame.tilemapC.*;

import static mame056.vidhrdw.generic.*;

public class kncljoe {

    static struct_tilemap bg_tilemap;
    static int tile_bank, sprite_bank;
    static int flipscreen;

    /**
     * *************************************************************************
     *
     * Convert the color PROMs into a more useable format.
     *
     **************************************************************************
     */
    static int TOTAL_COLORS(int gfxn) {
        return Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity;
    }
    public static VhConvertColorPromPtr kncljoe_vh_convert_color_prom = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {

            int i;
            int p_inc = 0;
            for (i = 0; i < 128; i++) {
                int bit0, bit1, bit2, bit3;

                bit0 = (color_prom.read(0) >> 0) & 0x01;
                bit1 = (color_prom.read(0) >> 1) & 0x01;
                bit2 = (color_prom.read(0) >> 2) & 0x01;
                bit3 = (color_prom.read(0) >> 3) & 0x01;
                palette[p_inc++] = ((char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3));
                bit0 = (color_prom.read(0x100) >> 0) & 0x01;
                bit1 = (color_prom.read(0x100) >> 1) & 0x01;
                bit2 = (color_prom.read(0x100) >> 2) & 0x01;
                bit3 = (color_prom.read(0x100) >> 3) & 0x01;
                palette[p_inc++] = ((char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3));
                bit0 = (color_prom.read(0x200) >> 0) & 0x01;
                bit1 = (color_prom.read(0x200) >> 1) & 0x01;
                bit2 = (color_prom.read(0x200) >> 2) & 0x01;
                bit3 = (color_prom.read(0x200) >> 3) & 0x01;
                palette[p_inc++] = ((char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3));

                color_prom.inc();
            }

            color_prom.inc(2 * 256 + 128);
            /* bottom half is not used */

            for (i = 0; i < 16; i++) {
                int bit0, bit1, bit2;

                /* red component */
                bit0 = 0;
                bit1 = (color_prom.read() >> 6) & 0x01;
                bit2 = (color_prom.read() >> 7) & 0x01;
                palette[p_inc++] = ((char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2));
                /* green component */
                bit0 = (color_prom.read() >> 3) & 0x01;
                bit1 = (color_prom.read() >> 4) & 0x01;
                bit2 = (color_prom.read() >> 5) & 0x01;
                palette[p_inc++] = ((char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2));
                /* blue component */
                bit0 = (color_prom.read() >> 0) & 0x01;
                bit1 = (color_prom.read() >> 1) & 0x01;
                bit2 = (color_prom.read() >> 2) & 0x01;
                palette[p_inc++] = ((char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2));

                color_prom.inc();
            }

            color_prom.inc(16);
            /* bottom half is not used */

 /* sprite lookup table */
            for (i = 0; i < 128; i++) {
                colortable[Machine.drv.gfxdecodeinfo[1].color_codes_start + i] = (char) (128 + (color_prom.readinc() & 0x0f));
            }
        }
    };

    /**
     * *************************************************************************
     *
     * Callbacks for the TileMap code
     *
     **************************************************************************
     */
    	public static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                int attr = videoram.read(2*tile_index+1);
		int code = videoram.read(2*tile_index) + ((attr & 0xc0) << 2) + (tile_bank << 10);
	
		SET_TILE_INFO(
				0,
				code,
				attr & 0xf,
				TILE_FLIPXY((attr & 0x30) >> 4));
                tile_info.flags = TILE_FLIPXY((attr & 0x30) >> 4);
            }
        };
	
    /**
     * *************************************************************************
     *
     * Start the video hardware emulation.
     *
     **************************************************************************
     */
    public static VhStartPtr kncljoe_vh_start = new VhStartPtr() {
        public int handler() {
                bg_tilemap = tilemap_create(get_bg_tile_info,tilemap_scan_rows,TILEMAP_OPAQUE,8,8,64,32);
	
		if (bg_tilemap == null)
			return 1;

            		tilemap_set_scroll_rows(bg_tilemap,4);
            return 0;
        }
    };

    /**
     * *************************************************************************
     *
     * Memory handlers
     *
     **************************************************************************
     */
    public static WriteHandlerPtr kncljoe_videoram_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            if (videoram.read(offset) != data) {
                videoram.write(offset, data);
                tilemap_mark_tile_dirty(bg_tilemap,offset/2);
            }
        }
    };

    public static WriteHandlerPtr kncljoe_control_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /*	0x01	screen flip
		0x02	coin counter#1
		0x04	sprite bank
		0x10	character bank
		0x20	coin counter#2
             */
 /* coin counters:
			reset when IN0 - Coin 1 goes low (active)
			set after IN0 - Coin 1 goes high AND the credit has been added
             */

            flipscreen = data & 0x01;
            tilemap_set_flip(ALL_TILEMAPS,flipscreen!=0 ? TILEMAP_FLIPX : TILEMAP_FLIPY);

            coin_counter_w.handler(0, data & 0x02);
            coin_counter_w.handler(1, data & 0x20);

            if (tile_bank != ((data & 0x10) >> 4)) {
                tile_bank = (data & 0x10) >> 4;
                /*TODO*///			tilemap_mark_all_tiles_dirty(bg_tilemap);
            }

            sprite_bank = (data & 0x04) >> 2;
        }
    };

    public static WriteHandlerPtr kncljoe_scroll_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            	tilemap_set_scrollx(bg_tilemap,0,data);
		tilemap_set_scrollx(bg_tilemap,1,data);
		tilemap_set_scrollx(bg_tilemap,2,data);
		tilemap_set_scrollx(bg_tilemap,3,0);
        }
    };

    /**
     * *************************************************************************
     *
     * Display refresh
     *
     **************************************************************************
     */
    static void draw_sprites(mame_bitmap bitmap) {
        rectangle clip = new rectangle(Machine.visible_area);
        GfxElement gfx = Machine.gfx[1 + sprite_bank];
        int offs;

        /* score covers sprites */
        if (flipscreen != 0) {
            clip.max_y -= 64;
        } else {
            clip.min_y += 64;
        }

        for (offs = spriteram_size[0]; offs >= 0; offs -= 4) {
            int sy = spriteram.read(offs);
            int sx = spriteram.read(offs + 3);
            int code = spriteram.read(offs + 2);
            int attr = spriteram.read(offs + 1);
            int flipx = attr & 0x40;
            int flipy = NOT(attr & 0x80);
            int color = attr & 0x0f;
            if ((attr & 0x10) != 0) {
                code += 512;
            }
            if ((attr & 0x20) != 0) {
                code += 256;
            }

            if (flipscreen != 0) {
                flipx = NOT(flipx);
                flipy = NOT(flipy);
                sx = 240 - sx;
                sy = 240 - sy;
            }

            drawgfx(bitmap, gfx,
                    code,
                    color,
                    flipx, flipy,
                    sx, sy,
                    clip, TRANSPARENCY_PEN, 0);
        }
    }

    public static VhUpdatePtr kncljoe_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            tilemap_update(ALL_TILEMAPS);
	
            tilemap_render(ALL_TILEMAPS);
                
            tilemap_draw(bitmap, bg_tilemap, 0);
            draw_sprites(bitmap);
        }
    };
}
