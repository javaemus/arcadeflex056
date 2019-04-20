/**
 * ported to v0.56
 * ported to v0.37b7
 */
/**
 * Changelog
 * ---------
 * 20/04/2019 - ported megazone vidhrdw to 0.56 (shadow)
 */
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.libc.cstring.*;
import static common.libc.expressions.*;
import static common.ptr.*;

import static mame056.common.*;

import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

public class megazone {

    public static UBytePtr megazone_scrollx = new UBytePtr();
    public static UBytePtr megazone_scrolly = new UBytePtr();
    static int flipscreen;

    public static UBytePtr megazone_videoram2 = new UBytePtr();
    public static UBytePtr megazone_colorram2 = new UBytePtr();
    public static int[] megazone_videoram2_size = new int[1];

    static int TOTAL_COLORS(int gfxn) {
        return Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity;
    }
    public static VhConvertColorPromPtr megazone_vh_convert_color_prom = new VhConvertColorPromPtr() {
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

            /* color_prom now points to the beginning of the lookup table */
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

    public static WriteHandlerPtr megazone_flipscreen_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            if (flipscreen != (data & 1)) {
                flipscreen = data & 1;
                memset(dirtybuffer, 1, videoram_size[0]);
            }
        }
    };

    public static VhStartPtr megazone_vh_start = new VhStartPtr() {
        public int handler() {
            dirtybuffer = null;
            tmpbitmap = null;

            if ((dirtybuffer = new char[videoram_size[0]]) == null) {
                return 1;
            }
            memset(dirtybuffer, 1, videoram_size[0]);

            if ((tmpbitmap = bitmap_alloc(256, 256)) == null) {
                dirtybuffer = null;
                return 1;
            }

            return 0;
        }
    };

    public static VhStopPtr megazone_vh_stop = new VhStopPtr() {
        public void handler() {
            dirtybuffer = null;
            bitmap_free(tmpbitmap);

            dirtybuffer = null;
            tmpbitmap = null;
        }
    };

    /**
     * *************************************************************************
     *
     * Draw the game screen in the given osd_bitmap. Do NOT call
     * osd_update_display() from this function, it will be called by the main
     * emulation engine.
     *
     **************************************************************************
     */
    public static VhUpdatePtr megazone_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            int offs;
            int x, y;

            /* for every character in the Video RAM, check if it has been modified */
 /* since last time and update it accordingly. */
            for (offs = videoram_size[0] - 1; offs >= 0; offs--) {
                if (dirtybuffer[offs] != 0) {
                    int sx, sy, flipx, flipy;

                    dirtybuffer[offs] = 0;

                    sx = offs % 32;
                    sy = offs / 32;
                    flipx = colorram.read(offs) & (1 << 6);
                    flipy = colorram.read(offs) & (1 << 5);
                    if (flipscreen != 0) {
                        sx = 31 - sx;
                        sy = 31 - sy;
                        flipx = NOT(flipx);
                        flipy = NOT(flipy);
                    }

                    drawgfx(tmpbitmap, Machine.gfx[0],
                            ((int) videoram.read(offs)) + (((colorram.read(offs) & (1 << 7)) != 0 ? 256 : 0)),
                            (colorram.read(offs) & 0x0f) + 0x10,
                            flipx, flipy,
                            8 * sx, 8 * sy,
                            null, TRANSPARENCY_NONE, 0);
                }
            }

            /* copy the temporary bitmap to the screen */
            {
                int scrollx;
                int scrolly;

                if (flipscreen != 0) {
                    scrollx = megazone_scrolly.read();
                    scrolly = megazone_scrollx.read();
                } else {
                    scrollx = -megazone_scrolly.read() + 4 * 8;// leave space for credit&score overlay
                    scrolly = -megazone_scrollx.read();
                }

                copyscrollbitmap(bitmap, tmpbitmap, 1, new int[]{scrollx}, 1, new int[]{scrolly}, Machine.visible_area, TRANSPARENCY_NONE, 0);
            }

            /* Draw the sprites. */
            {
                for (offs = spriteram_size[0] - 4; offs >= 0; offs -= 4) {
                    int sx, sy, flipx, flipy;

                    sx = spriteram.read(offs + 3);
                    if (flipscreen != 0) {
                        sx -= 11;
                    } else {
                        sx += 4 * 8;// Sprite y-position correction depending on screen flip
                    }
                    sy = 255 - ((spriteram.read(offs + 1) + 16) & 0xff);
                    if (flipscreen != 0) {
                        sy += 2;// Sprite x-position correction depending on screen flip
                    }
                    flipx = ~spriteram.read(offs + 0) & 0x40;
                    flipy = spriteram.read(offs + 0) & 0x80;

                    drawgfx(bitmap, Machine.gfx[1],
                            spriteram.read(offs + 2),
                            spriteram.read(offs + 0) & 0x0f,
                            flipx, flipy,
                            sx, sy,
                            Machine.visible_area, TRANSPARENCY_COLOR, 0);
                }
            }

            for (y = 0; y < 32; y++) {
                offs = y * 32;
                for (x = 0; x < 6; x++) {
                    int sx, sy, flipx, flipy;

                    sx = x;
                    sy = y;

                    flipx = megazone_colorram2.read(offs) & (1 << 6);
                    flipy = megazone_colorram2.read(offs) & (1 << 5);

                    if (flipscreen != 0) {
                        sx = 35 - sx;
                        sy = 31 - sy;
                        flipx = NOT(flipx);
                        flipy = NOT(flipy);
                    }

                    drawgfx(bitmap, Machine.gfx[0],
                            ((int) megazone_videoram2.read(offs)) + (((megazone_colorram2.read(offs) & (1 << 7)) != 0 ? 256 : 0)),
                            (megazone_colorram2.read(offs) & 0x0f) + 0x10,
                            flipx, flipy,
                            8 * sx, 8 * sy,
                            null, TRANSPARENCY_NONE, 0);
                    offs++;
                }
            }
        }
    };
}
