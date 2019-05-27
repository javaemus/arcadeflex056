/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
/**
 * Changelog
 * ---------
 * 26/05/2019 - rewrote docastle to 0.56 (shadow)
 */
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.libc.cstring.*;
import static common.ptr.*;
import static common.libc.expressions.*;

import static mame056.common.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.tilemapC.*;

import static mame056.vidhrdw.generic.*;


public class docastle {

    static mame_bitmap tmpbitmap1;

    static int TOTAL_COLORS(int gfxn) {
        return Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity;
    }

    static void convert_color_prom(char[] palette, char[] colortable, UBytePtr color_prom, int priority) {
        int i, j;
        int p_ptr = 0;
        for (i = 0; i < 256; i++) {
            int bit0, bit1, bit2;

            /* red component */
            bit0 = (color_prom.read() >> 5) & 0x01;
            bit1 = (color_prom.read() >> 6) & 0x01;
            bit2 = (color_prom.read() >> 7) & 0x01;
            palette[p_ptr++] = (char) (0x23 * bit0 + 0x4b * bit1 + 0x91 * bit2);
            /* green component */
            bit0 = (color_prom.read() >> 2) & 0x01;
            bit1 = (color_prom.read() >> 3) & 0x01;
            bit2 = (color_prom.read() >> 4) & 0x01;
            palette[p_ptr++] = (char) (0x23 * bit0 + 0x4b * bit1 + 0x91 * bit2);
            /* blue component */
            bit0 = 0;
            bit1 = (color_prom.read() >> 0) & 0x01;
            bit2 = (color_prom.read() >> 1) & 0x01;
            palette[p_ptr++] = (char) (0x23 * bit0 + 0x4b * bit1 + 0x91 * bit2);

            color_prom.inc();
        }

        /* reserve one color for the transparent pen (none of the game colors can have */
 /* these RGB components) */
        palette[p_ptr++] = (char) (1);
        palette[p_ptr++] = (char) (1);
        palette[p_ptr++] = (char) (1);
        /* and the last color for the sprite covering pen */
        palette[p_ptr++] = (char) (2);
        palette[p_ptr++] = (char) (2);
        palette[p_ptr++] = (char) (2);

        /* characters */
 /* characters have 4 bitplanes, but they actually have only 8 colors. The fourth */
 /* plane is used to select priority over sprites. The meaning of the high bit is */
 /* reversed in Do's Castle wrt the other games. */
 /* first create a table with all colors, used to draw the background */
        for (i = 0; i < 32; i++) {
            for (j = 0; j < 8; j++) {
                colortable[16 * i + j] = (char) (8 * i + j);
                colortable[16 * i + j + 8] = (char) (8 * i + j);
            }
        }
        /* now create a table with only the colors which have priority over sprites, used */
 /* to draw the foreground. */
        for (i = 0; i < 32; i++) {
            for (j = 0; j < 8; j++) {
                if (priority == 0) /* Do's Castle */ {
                    colortable[32 * 16 + 16 * i + j] = 256;
                    /* high bit clear means less priority than sprites */
                    colortable[32 * 16 + 16 * i + j + 8] = (char) (8 * i + j);
                } else /* Do Wild Ride, Do Run Run, Kick Rider */ {
                    colortable[32 * 16 + 16 * i + j] = (char) (8 * i + j);
                    colortable[32 * 16 + 16 * i + j + 8] = 256;
                    /* high bit set means less priority than sprites */
                }
            }
        }

        /* sprites */
 /* sprites have 4 bitplanes, but they actually have only 8 colors. The fourth */
 /* plane is used for transparency. */
        for (i = 0; i < 32; i++) {
            /* build two versions of the colortable, one with the covering color
			   mapped to transparent, and one with all colors but the covering one
			   mapped to transparent. */
            for (j = 0; j < 16; j++) {
                if (j < 8) {
                    colortable[64 * 16 + 16 * i + j] = 256;
                    /* high bit clear means transparent */
                } else if (j == 15) {
                    colortable[64 * 16 + 16 * i + j] = 256;
                    /* sprite covering color */
                } else {
                    colortable[64 * 16 + 16 * i + j] = (char) (8 * i + (j & 7));
                }
            }
            for (j = 0; j < 16; j++) {
                if (j == 15) {
                    colortable[64 * 16 + 32 * 16 + 16 * i + j] = 257;
                    /* sprite covering color */
                } else {
                    colortable[64 * 16 + 32 * 16 + 16 * i + j] = 256;
                }
            }
        }
    }

    public static VhConvertColorPromPtr docastle_vh_convert_color_prom = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
            convert_color_prom(palette, colortable, color_prom, 0);
        }
    };

    public static VhConvertColorPromPtr dorunrun_vh_convert_color_prom = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
            convert_color_prom(palette, colortable, color_prom, 1);
        }
    };

    /**
     * *************************************************************************
     *
     * Start the video hardware emulation.
     *
     **************************************************************************
     */
    public static VhStartPtr docastle_vh_start = new VhStartPtr() {
        public int handler() {
            if (generic_vh_start.handler() != 0) {
                return 1;
            }

            if ((tmpbitmap1 = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height)) == null) {
                generic_vh_stop.handler();
                return 1;
            }

            return 0;
        }
    };

    /**
     * *************************************************************************
     *
     * Stop the video hardware emulation.
     *
     **************************************************************************
     */
    public static VhStopPtr docastle_vh_stop = new VhStopPtr() {
        public void handler() {
            bitmap_free(tmpbitmap1);
            generic_vh_stop.handler();
        }
    };

    public static ReadHandlerPtr docastle_flipscreen_off_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            flip_screen_set(0);
            return 0;
        }
    };

    public static ReadHandlerPtr docastle_flipscreen_on_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            flip_screen_set(1);
            return 0;
        }
    };

    public static WriteHandlerPtr docastle_flipscreen_off_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            flip_screen_set(0);
        }
    };

    public static WriteHandlerPtr docastle_flipscreen_on_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            flip_screen_set(1);
        }
    };

    /**
     * *************************************************************************
     *
     * Draw the game screen in the given mame_bitmap. Do NOT call
     * osd_update_display() from this function, it will be called by the main
     * emulation engine.
     *
     **************************************************************************
     */
    public static VhUpdatePtr docastle_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            int offs;

            if (full_refresh != 0) {
                memset(dirtybuffer, 1, videoram_size[0]);
            }

            /* for every character in the Video RAM, check if it has been modified */
 /* since last time and update it accordingly. */
            for (offs = videoram_size[0] - 1; offs >= 0; offs--) {
                if (dirtybuffer[offs] != 0) {
                    int sx, sy;

                    dirtybuffer[offs] = 0;

                    sx = offs % 32;
                    sy = offs / 32;

                    if (flip_screen() != 0) {
                        sx = 31 - sx;
                        sy = 31 - sy;
                    }

                    drawgfx(tmpbitmap, Machine.gfx[0],
                            videoram.read(offs) + 8 * (colorram.read(offs) & 0x20),
                            colorram.read(offs) & 0x1f,
                            flip_screen(), flip_screen(),
                            8 * sx, 8 * sy,
                            Machine.visible_area, TRANSPARENCY_NONE, 0);

                    /* also draw the part of the character which has priority over the */
 /* sprites in another bitmap */
                    drawgfx(tmpbitmap1, Machine.gfx[0],
                            videoram.read(offs) + 8 * (colorram.read(offs) & 0x20),
                            32 + (colorram.read(offs) & 0x1f),
                            flip_screen(), flip_screen(),
                            8 * sx, 8 * sy,
                            Machine.visible_area, TRANSPARENCY_NONE, 0);
                }
            }

            /* copy the character mapped graphics */
            copybitmap(bitmap, tmpbitmap, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_NONE, 0);

            fillbitmap(priority_bitmap, 1, null);

            /* Draw the sprites */
            for (offs = spriteram_size[0] - 4; offs >= 0; offs -= 4) {
                int sx, sy, flipx, flipy, code, color;

                code = spriteram.read(offs + 3);
                color = spriteram.read(offs + 2) & 0x1f;
                sx = spriteram.read(offs + 1);
                sy = spriteram.read(offs);
                flipx = spriteram.read(offs + 2) & 0x40;
                flipy = spriteram.read(offs + 2) & 0x80;

                if (flip_screen() != 0) {
                    sx = 240 - sx;
                    sy = 240 - sy;
                    flipx = NOT(flipx);
                    flipy = NOT(flipy);
                }

                /* first draw the sprite, visible */
                pdrawgfx(bitmap, Machine.gfx[1],
                        code,
                        color,
                        flipx, flipy,
                        sx, sy,
                        Machine.visible_area, TRANSPARENCY_COLOR, 256,
                        0x00);

                /* then draw the mask, behind the background but obscuring following sprites */
                pdrawgfx(bitmap, Machine.gfx[1],
                        code,
                        color + 32,
                        flipx, flipy,
                        sx, sy,
                        Machine.visible_area, TRANSPARENCY_COLOR, 256,
                        0x02);
            }

            /* now redraw the portions of the background which have priority over sprites */
            copybitmap(bitmap, tmpbitmap1, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_COLOR, 256);
        }
    };
}
