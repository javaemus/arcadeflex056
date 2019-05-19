/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
/**
 * Changelog
 * =========
 * 18/05/2019 ported to mame 0.56 (shadow)
 */
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;

import static mame056.common.*;
import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.drivers.nyny.*;

public class nyny {

    static mame_bitmap tmpbitmap1;
    static mame_bitmap tmpbitmap2;

    /* used by nyny and spiders */
    public static VhConvertColorPromPtr nyny_init_palette = new VhConvertColorPromPtr() {
        public void handler(char[] obsolete, char[] game_colortable, UBytePtr color_prom) {
            int i;

            for (i = 0; i < Machine.drv.total_colors; i++) {
                palette_set_color(i, ((i >> 0) & 1) * 0xff, ((i >> 1) & 1) * 0xff, ((i >> 2) & 1) * 0xff);
            }
        }
    };

    /**
     * *************************************************************************
     *
     * Start the video hardware emulation.
     *
     **************************************************************************
     */
    public static VhStartPtr nyny_vh_start = new VhStartPtr() {
        public int handler() {
            if ((tmpbitmap1 = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height)) == null) {
                return 1;
            }
            if ((tmpbitmap2 = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height)) == null) {
                bitmap_free(tmpbitmap1);
                return 1;
            }

            nyny_videoram = new UBytePtr(0x4000);
            nyny_colourram = new UBytePtr(0x4000);

            return 0;
        }
    };

    /**
     * *************************************************************************
     * Stop the video hardware emulation.
     * *************************************************************************
     */
    public static VhStopPtr nyny_vh_stop = new VhStopPtr() {
        public void handler() {
            nyny_videoram = null;
            nyny_colourram = null;

            bitmap_free(tmpbitmap1);
            bitmap_free(tmpbitmap2);
        }
    };

    public static WriteHandlerPtr nyny_flipscreen_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            flip_screen_set(data);
        }
    };

    public static ReadHandlerPtr nyny_videoram0_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return (nyny_videoram.read(offset));
        }
    };

    public static ReadHandlerPtr nyny_videoram1_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return (nyny_videoram.read(offset + 0x2000));
        }
    };

    public static ReadHandlerPtr nyny_colourram0_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return (nyny_colourram.read(offset));
        }
    };

    public static ReadHandlerPtr nyny_colourram1_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return (nyny_colourram.read(offset + 0x2000));
        }
    };

    public static WriteHandlerPtr nyny_colourram0_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int x, y, z, d, v, c;
            nyny_colourram.write(offset, data);
            v = nyny_videoram.read(offset);

            x = offset & 0x1f;
            y = offset >> 5;

            d = data & 7;
            for (z = 0; z < 8; z++) {
                c = v & 1;
                plot_pixel.handler(tmpbitmap1, x * 8 + z, y, Machine.pens[c * d]);
                v >>= 1;
            }
        }
    };

    public static WriteHandlerPtr nyny_videoram0_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int x, y, z, c, d;
            nyny_videoram.write(offset, data);
            d = nyny_colourram.read(offset) & 7;

            x = offset & 0x1f;
            y = offset >> 5;

            for (z = 0; z < 8; z++) {
                c = data & 1;
                plot_pixel.handler(tmpbitmap1, x * 8 + z, y, Machine.pens[c * d]);
                data >>= 1;
            }
        }
    };

    public static WriteHandlerPtr nyny_colourram1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int x, y, z, d, v, c;
            nyny_colourram.write(offset + 0x2000, data);
            v = nyny_videoram.read(offset + 0x2000);

            x = offset & 0x1f;
            y = offset >> 5;

            d = data & 7;
            for (z = 0; z < 8; z++) {
                c = v & 1;
                plot_pixel.handler(tmpbitmap2, x * 8 + z, y, Machine.pens[c * d]);
                v >>= 1;
            }

        }
    };

    public static WriteHandlerPtr nyny_videoram1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int x, y, z, c, d;
            nyny_videoram.write(offset + 0x2000, data);
            d = nyny_colourram.read(offset + 0x2000) & 7;

            x = offset & 0x1f;
            y = offset >> 5;

            for (z = 0; z < 8; z++) {
                c = data & 1;
                plot_pixel.handler(tmpbitmap2, x * 8 + z, y, Machine.pens[c * d]);
                data >>= 1;
            }
        }
    };

    public static VhUpdatePtr nyny_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            copybitmap(bitmap, tmpbitmap2, flip_screen(), flip_screen(), 0, 0, Machine.visible_area, TRANSPARENCY_NONE, 0);
            copybitmap(bitmap, tmpbitmap1, flip_screen(), flip_screen(), 0, 0, Machine.visible_area, TRANSPARENCY_COLOR, 0);
        }
    };
}
