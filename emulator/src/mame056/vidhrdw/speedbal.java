/**
 * ported to v0.56
 * ported to v0.37b16
 * ported to v0.37b7
 *
 */
/**
 * Changelog
 * ---------
 * 11/05/2019 - added speedbal vidhrdw driver (shadow)
 */
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;
import static common.libc.cstring.*;

import static mame056.mame.*;
import static mame056.drawgfx.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.drivers.speedbal.*;

public class speedbal {

    public static final int SPRITE_X = 0;
    public static final int SPRITE_NUMBER = 1;
    public static final int SPRITE_PALETTE = 2;
    public static final int SPRITE_Y = 3;

    static char[] bg_dirtybuffer;/* background tiles */

    static char[] ch_dirtybuffer;/* foreground char  */

    static mame_bitmap bitmap_bg;/* background tiles */

    static mame_bitmap bitmap_ch;/* foreground char  */


    static int TOTAL_COLORS(int gfxn) {
        return Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity;
    }
    public static VhConvertColorPromPtr speedbal_vh_convert_color_prom = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
            int i;
            //#define TOTAL_COLORS(gfxn) (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity)
            //#define COLOR(gfxn,offs) (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs])

            int p_inc = 0;
            for (i = 0; i < Machine.drv.total_colors; i++) {
                int bit0, bit1, bit2;

                /* red component */
                bit0 = (color_prom.read() >> 0) & 0x01;
                bit1 = (color_prom.read() >> 1) & 0x01;
                bit2 = (color_prom.read() >> 2) & 0x01;
                palette[p_inc++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
                /* green component */
                bit0 = (color_prom.read() >> 3) & 0x01;
                bit1 = (color_prom.read() >> 4) & 0x01;
                bit2 = (color_prom.read() >> 5) & 0x01;
                palette[p_inc++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
                /* blue component */
                bit0 = 0;
                bit1 = (color_prom.read() >> 6) & 0x01;
                bit2 = (color_prom.read() >> 7) & 0x01;
                palette[p_inc++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);

                color_prom.inc();
            }

            /* characters */
            for (i = 0; i < TOTAL_COLORS(0); i++) {
                colortable[Machine.drv.gfxdecodeinfo[0].color_codes_start + i] = (char) ((color_prom.readinc()));
            }

            /* tiles */
            for (i = 0; i < TOTAL_COLORS(1); i++) {
                colortable[Machine.drv.gfxdecodeinfo[1].color_codes_start + i] = (char) ((color_prom.readinc()) & 0x0f);
            }

            /* sprites */
            for (i = 0; i < TOTAL_COLORS(2); i++) {
                colortable[Machine.drv.gfxdecodeinfo[2].color_codes_start + i] = (char) ((color_prom.readinc()) & 0x0f);
            }

        }
    };

    /**
     * ***********************************
     *				   *
     * Start-Stop	* * ***********************************
     */
    public static VhStartPtr speedbal_vh_start = new VhStartPtr() {
        public int handler() {
            if ((bg_dirtybuffer = new char[speedbal_background_videoram_size[0]]) == null) {
                return 1;
            }
            if ((ch_dirtybuffer = new char[speedbal_foreground_videoram_size[0]]) == null) {
                bg_dirtybuffer = null;
                return 1;
            }

            /* foreground bitmap */
            if ((bitmap_ch = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height)) == null) {
                bg_dirtybuffer = null;
                ch_dirtybuffer = null;
                return 1;
            }

            /* background bitmap */
            if ((bitmap_bg = bitmap_alloc(Machine.drv.screen_width * 2, Machine.drv.screen_height * 2)) == null) {
                bg_dirtybuffer = null;
                ch_dirtybuffer = null;
                bitmap_free(bitmap_ch);
                return 1;
            }

            memset(ch_dirtybuffer, 1, speedbal_foreground_videoram_size[0] / 2);
            memset(bg_dirtybuffer, 1, speedbal_background_videoram_size[0] / 2);
            return 0;

        }
    };

    public static VhStopPtr speedbal_vh_stop = new VhStopPtr() {
        public void handler() {
            bitmap_free(bitmap_ch);
            bitmap_free(bitmap_bg);
            bg_dirtybuffer = null;
            ch_dirtybuffer = null;
        }
    };

    /**
     * ***********************************
     *				   *
     * Foreground characters RAM * * ***********************************
     */
    public static WriteHandlerPtr speedbal_foreground_videoram_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            ch_dirtybuffer[offset] = 1;
            speedbal_foreground_videoram.write(offset, data);
        }
    };

    public static ReadHandlerPtr speedbal_foreground_videoram_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return speedbal_foreground_videoram.read(offset);
        }
    };

    /**
     * ***********************************
     *				   *
     * Background tiles RAM * * ***********************************
     */
    public static WriteHandlerPtr speedbal_background_videoram_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            bg_dirtybuffer[offset] = 1;
            speedbal_background_videoram.write(offset, data);
        }
    };

    public static ReadHandlerPtr speedbal_background_videoram_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return speedbal_background_videoram.read(offset);
        }
    };

    /**
     * ***********************************
     *				   *
     * Sprite drawing	* * ***********************************
     */
    public static void speedbal_draw_sprites(mame_bitmap bitmap) {
        int SPTX, SPTY, SPTTile, SPTColor, offset, f;
        char carac;
        UBytePtr SPTRegs;

        /* Drawing sprites: 64 in total */
        for (offset = 0; offset < speedbal_sprites_dataram_size[0]; offset += 4) {
            SPTRegs = new UBytePtr(speedbal_sprites_dataram, offset);

            SPTX = 243 - SPTRegs.read(SPRITE_Y);
            SPTY = 239 - SPTRegs.read(SPRITE_X);

            carac = SPTRegs.read(SPRITE_NUMBER);
            SPTTile = 0;
            for (f = 0; f < 8; f++) {
                SPTTile += ((carac >> f) & 1) << (7 - f);
            }
            SPTColor = (SPTRegs.read(SPRITE_PALETTE) & 0x0f);

            if ((SPTRegs.read(SPRITE_PALETTE) & 0x40) == 0) {
                SPTTile += 256;
            }

            drawgfx(bitmap, Machine.gfx[2],
                    SPTTile,
                    SPTColor,
                    0, 0,
                    SPTX, SPTY,
                    Machine.visible_area, TRANSPARENCY_PEN, 0);
        }
    }

    /**
     * ***********************************
     *				   *
     * Background drawing: Tiles * * ***********************************
     */
    public static void speedbal_draw_background(mame_bitmap bitmap) {
        int sx, sy, code, tile, offset, color;

        for (offset = 0; offset < speedbal_background_videoram_size[0]; offset += 2) {
            if (bg_dirtybuffer[offset] != 0) {
                bg_dirtybuffer[offset] = 0;

                tile = speedbal_background_videoram.read(offset + 0);
                code = speedbal_background_videoram.read(offset + 1);
                tile += (code & 0x30) << 4;
                color = (code & 0x0f);

                sx = 15 - (offset / 2) / 16;
                sy = (offset / 2) % 16;

                drawgfx(bitmap_bg, Machine.gfx[1],
                        tile,
                        color,
                        0, 0,
                        16 * sx, 16 * sy,
                        null, TRANSPARENCY_NONE, 0);
            }
        }
        copybitmap(bitmap, bitmap_bg, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_NONE, 0);
    }

    /**
     * ***********************************
     *				   *
     * Foreground drawing: 8x8 graphs * * ***********************************
     */
    public static void speedbal_draw_foreground1(mame_bitmap bitmap) {
        int sx, sy, code, caracter, color, offset;

        for (offset = 0; offset < speedbal_foreground_videoram_size[0]; offset += 2) {
            //if (ch_dirtybuffer[offset] != 0) {
            caracter = speedbal_foreground_videoram.read(offset);
            code = speedbal_foreground_videoram.read(offset + 1);
            caracter += (code & 0x30) << 4;

            color = (code & 0x0f);

            sx = 31 - (offset / 2) / 32;
            sy = (offset / 2) % 32;

            drawgfx(bitmap, Machine.gfx[0],
                    caracter,
                    color,
                    0, 0,
                    8 * sx, 8 * sy,
                    Machine.visible_area, TRANSPARENCY_PEN, 0);

            ch_dirtybuffer[offset] = 0;
            // }

        }
    }

    /**
     * ***********************************
     *				   *
     * Refresh screen	* * ***********************************
     */
    public static VhUpdatePtr speedbal_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            // first background
            speedbal_draw_background(bitmap);

            // second characters (general)
            speedbal_draw_foreground1(bitmap);

            // thirth sprites
            speedbal_draw_sprites(bitmap);
        }
    };
}
