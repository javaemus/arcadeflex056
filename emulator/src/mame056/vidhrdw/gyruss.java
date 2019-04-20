/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
/**
 * Changelog
 * ---------
 * 20/04/2019 - added gyruss vidhrdw driver (shadow)
 */
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;
import static common.libc.cstring.*;
import static common.libc.expressions.*;

import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.cpuexec.*;

import static mame056.vidhrdw.generic.*;

public class gyruss {

    static int flipscreen;

    /*
	sprites are multiplexed, so we have to buffer the spriteram
	scanline by scanline.
     */
    static UBytePtr sprite_mux_buffer;
    static int scanline;

    static int TOTAL_COLORS(int gfxn) {
        return Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity;
    }
    public static VhConvertColorPromPtr gyruss_vh_convert_color_prom = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
            int i;
            int p_inc = 0;
            for (i = 0; i < Machine.drv.total_colors; i++) {
                int bit0, bit1, bit2;

                /* red component */
                bit0 = (color_prom.read() >> 0) & 0x01;
                bit1 = (color_prom.read() >> 1) & 0x01;
                bit2 = (color_prom.read() >> 2) & 0x01;
                palette[p_inc++] = ((char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2));
                /* green component */
                bit0 = (color_prom.read() >> 3) & 0x01;
                bit1 = (color_prom.read() >> 4) & 0x01;
                bit2 = (color_prom.read() >> 5) & 0x01;
                palette[p_inc++] = ((char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2));
                /* blue component */
                bit0 = 0;
                bit1 = (color_prom.read() >> 6) & 0x01;
                bit2 = (color_prom.read() >> 7) & 0x01;
                palette[p_inc++] = ((char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2));

                color_prom.inc();
            }

            /* sprites */
            for (i = 0; i < TOTAL_COLORS(1); i++) {
                colortable[Machine.drv.gfxdecodeinfo[1].color_codes_start + i] = (char) ((color_prom.readinc()) & 0x0f);
            }

            /* characters */
            for (i = 0; i < TOTAL_COLORS(0); i++) {
                colortable[Machine.drv.gfxdecodeinfo[0].color_codes_start + i] = (char) (((color_prom.readinc()) & 0x0f) + 0x10);
            }
        }
    };

    public static VhStopPtr gyruss_vh_stop = new VhStopPtr() {
        public void handler() {
            sprite_mux_buffer = null;
            generic_vh_stop.handler();
        }
    };

    public static VhStartPtr gyruss_vh_start = new VhStartPtr() {
        public int handler() {
            sprite_mux_buffer = new UBytePtr(256 * spriteram_size[0]);
            return generic_vh_start.handler();
        }
    };

    public static WriteHandlerPtr gyruss_flipscreen_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            if (flipscreen != (data & 1)) {
                flipscreen = data & 1;
                memset(dirtybuffer, 1, videoram_size[0]);
            }
        }
    };

    /* Return the current video scan line */
    public static ReadHandlerPtr gyruss_scanline_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return scanline;
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
    static void draw_sprites(mame_bitmap bitmap) {
        rectangle clip = new rectangle(Machine.visible_area);
        int offs;
        int line;

        for (line = 0; line < 256; line++) {
            if (line >= Machine.visible_area.min_y && line <= Machine.visible_area.max_y) {
                UBytePtr sr;

                sr = new UBytePtr(sprite_mux_buffer, line * spriteram_size[0]);
                clip.min_y = clip.max_y = line;

                for (offs = spriteram_size[0] - 4; offs >= 0; offs -= 4) {
                    int sx, sy;

                    sx = sr.read(offs);
                    sy = 241 - sr.read(offs + 3);
                    if (sy > line - 16 && sy <= line) {
                        drawgfx(bitmap, Machine.gfx[1 + (sr.read(offs + 1) & 1)],
                                sr.read(offs + 1) / 2 + 4 * (sr.read(offs + 2) & 0x20),
                                sr.read(offs + 2) & 0x0f,
                                NOT(sr.read(offs + 2) & 0x40), sr.read(offs + 2) & 0x80,
                                sx, sy,
                                clip, TRANSPARENCY_PEN, 0);
                    }
                }
            }
        }
    }

    public static VhUpdatePtr gyruss_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            int offs;

            /* for every character in the Video RAM, check if it has been modified since last time and update it accordingly. */
            for (offs = videoram_size[0] - 1; offs >= 0; offs--) {
                if (dirtybuffer[offs] != 0) {
                    int sx, sy, flipx, flipy;

                    dirtybuffer[offs] = 0;

                    sx = offs % 32;
                    sy = offs / 32;
                    flipx = colorram.read(offs) & 0x40;
                    flipy = colorram.read(offs) & 0x80;
                    if (flipscreen != 0) {
                        sx = 31 - sx;
                        sy = 31 - sy;
                        flipx = NOT(flipx);
                        flipy = NOT(flipy);
                    }

                    drawgfx(tmpbitmap, Machine.gfx[0],
                            videoram.read(offs) + 8 * (colorram.read(offs) & 0x20),
                            colorram.read(offs) & 0x0f,
                            flipx, flipy,
                            8 * sx, 8 * sy,
                            Machine.visible_area, TRANSPARENCY_NONE, 0);
                }
            }

            /* copy the character mapped graphics */
            copybitmap(bitmap, tmpbitmap, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_NONE, 0);

            draw_sprites(bitmap);

            /* redraw the characters which have priority over sprites */
            for (offs = videoram_size[0] - 1; offs >= 0; offs--) {
                int sx, sy, flipx, flipy;

                sx = offs % 32;
                sy = offs / 32;
                flipx = colorram.read(offs) & 0x40;
                flipy = colorram.read(offs) & 0x80;
                if (flipscreen != 0) {
                    sx = 31 - sx;
                    sy = 31 - sy;
                    flipx = NOT(flipx);
                    flipy = NOT(flipy);
                }

                if ((colorram.read(offs) & 0x10) != 0) {
                    drawgfx(bitmap, Machine.gfx[0],
                            videoram.read(offs) + 8 * (colorram.read(offs) & 0x20),
                            colorram.read(offs) & 0x0f,
                            flipx, flipy,
                            8 * sx, 8 * sy,
                            Machine.visible_area, TRANSPARENCY_NONE, 0);
                }
            }
        }
    };

    public static InterruptPtr gyruss_6809_interrupt = new InterruptPtr() {
        public int handler() {
            scanline = 255 - cpu_getiloops();

            memcpy(sprite_mux_buffer, scanline * spriteram_size[0], spriteram, spriteram_size[0]);

            if (scanline == 255) {
                return interrupt.handler();
            } else {
                return ignore_interrupt.handler();
            }
        }
    };
}
