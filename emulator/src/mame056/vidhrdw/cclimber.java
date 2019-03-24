/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;

import static common.ptr.*;
import common.subArrays.IntArray;
import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static common.libc.expressions.NOT;

public class cclimber {

    public static UBytePtr cclimber_bsvideoram = new UBytePtr();
    public static int[] cclimber_bsvideoram_size = new int[1];
    public static UBytePtr cclimber_bigspriteram = new UBytePtr();
    public static UBytePtr cclimber_column_scroll = new UBytePtr();
    static int[] palettebank = new int[1];
    static int[] sidepanel_enabled = new int[1];

    /**
     * *************************************************************************
     *
     * Convert the color PROMs into a more useable format.
     *
     * Crazy Climber has three 32x8 palette PROMs. The palette PROMs are
     * connected to the RGB output this way:
     *
     * bit 7 -- 220 ohm resistor -- BLUE -- 470 ohm resistor -- BLUE -- 220 ohm
     * resistor -- GREEN -- 470 ohm resistor -- GREEN -- 1 kohm resistor --
     * GREEN -- 220 ohm resistor -- RED -- 470 ohm resistor -- RED bit 0 -- 1
     * kohm resistor -- RED
     *
     **************************************************************************
     */
    public static int TOTAL_COLORS(int gfxn) {
        return Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity;
    }

    public static void COLOR(char[] colortable, int gfxn, int offs, int value) {
        colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + (offs)] = (char) value;
    }

    public static VhConvertColorPromPtr cclimber_vh_convert_color_prom = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
            int p_ptr = 0;
            for (i = 0; i < Machine.drv.total_colors; i++) {
                int bit0, bit1, bit2;

                /* red component */
                bit0 = (color_prom.read() >> 0) & 0x01;
                bit1 = (color_prom.read() >> 1) & 0x01;
                bit2 = (color_prom.read() >> 2) & 0x01;
                palette[p_ptr++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
                /* green component */
                bit0 = (color_prom.read() >> 3) & 0x01;
                bit1 = (color_prom.read() >> 4) & 0x01;
                bit2 = (color_prom.read() >> 5) & 0x01;
                palette[p_ptr++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
                /* blue component */
                bit0 = 0;
                bit1 = (color_prom.read() >> 6) & 0x01;
                bit2 = (color_prom.read() >> 7) & 0x01;
                palette[p_ptr++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);

                color_prom.inc();
            }

            /* character and sprite lookup table */
 /* they use colors 0-63 */
            for (i = 0; i < TOTAL_COLORS(0); i++) {
                /* pen 0 always uses color 0 (background in River Patrol and Silver Land) */
                if (i % 4 == 0) {
                    colortable[Machine.drv.gfxdecodeinfo[0].color_codes_start + i] = (char) (0);
                } else {
                    colortable[Machine.drv.gfxdecodeinfo[0].color_codes_start + i] = (char) (i);
                }
            }

            /* big sprite lookup table */
 /* it uses colors 64-95 */
            for (i = 0; i < TOTAL_COLORS(2); i++) {
                if (i % 4 == 0) {
                    colortable[Machine.drv.gfxdecodeinfo[2].color_codes_start + i] = (char) (0);
                } else {
                    colortable[Machine.drv.gfxdecodeinfo[2].color_codes_start + i] = (char) (i + 64);
                }

            }

        }
    };

    /**
     * *************************************************************************
     *
     * Convert the color PROMs into a more useable format.
     *
     * Swimmer has two 256x4 char/sprite palette PROMs and one 32x8 big sprite
     * palette PROM. The palette PROMs are connected to the RGB output this way:
     * (the 500 and 250 ohm resistors are made of 1 kohm resistors in parallel)
     *
     * bit 3 -- 250 ohm resistor -- BLUE -- 500 ohm resistor -- BLUE -- 250 ohm
     * resistor -- GREEN bit 0 -- 500 ohm resistor -- GREEN bit 3 -- 1 kohm
     * resistor -- GREEN -- 250 ohm resistor -- RED -- 500 ohm resistor -- RED
     * bit 0 -- 1 kohm resistor -- RED
     *
     * bit 7 -- 250 ohm resistor -- BLUE -- 500 ohm resistor -- BLUE -- 250 ohm
     * resistor -- GREEN -- 500 ohm resistor -- GREEN -- 1 kohm resistor --
     * GREEN -- 250 ohm resistor -- RED -- 500 ohm resistor -- RED bit 0 -- 1
     * kohm resistor -- RED
     *
     * Additionally, the background color of the score panel is determined by
     * these resistors:
     *
     * /--- tri-state -- 470 -- BLUE +5V -- 1kohm ------- tri-state -- 390 --
     * GREEN \--- tri-state -- 1000 -- RED
     *
     **************************************************************************
     */
    public static int BGPEN = (256 + 32);
    public static int SIDEPEN = (256 + 32 + 1);

    /*TODO*///#define TOTAL_COLORS(gfxn) (Machine->gfx[gfxn]->total_colors * Machine->gfx[gfxn]->color_granularity)
    /*TODO*///#define COLOR(gfxn,offs) (colortable[Machine->drv->gfxdecodeinfo[gfxn].color_codes_start + (offs)])
    public static VhConvertColorPromPtr swimmer_vh_convert_color_prom = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
            int i;
            int p_ptr = 0;
            for (i = 0; i < 256; i++) {
                int bit0, bit1, bit2;

                /* red component */
                bit0 = (color_prom.read(i) >> 0) & 0x01;
                bit1 = (color_prom.read(i) >> 1) & 0x01;
                bit2 = (color_prom.read(i) >> 2) & 0x01;
                palette[p_ptr++] = (char) (0x20 * bit0 + 0x40 * bit1 + 0x80 * bit2);
                /* green component */
                bit0 = (color_prom.read(i) >> 3) & 0x01;
                bit1 = (color_prom.read(i + 256) >> 0) & 0x01;
                bit2 = (color_prom.read(i + 256) >> 1) & 0x01;
                palette[p_ptr++] = (char) (0x20 * bit0 + 0x40 * bit1 + 0x80 * bit2);
                /* blue component */
                bit0 = 0;
                bit1 = (color_prom.read(i + 256) >> 2) & 0x01;
                bit2 = (color_prom.read(i + 256) >> 3) & 0x01;
                palette[p_ptr++] = (char) (0x20 * bit0 + 0x40 * bit1 + 0x80 * bit2);

                /* side panel */
                if ((i % 8) != 0) {
                    colortable[Machine.drv.gfxdecodeinfo[0].color_codes_start + i] = (char) (i);
                    colortable[Machine.drv.gfxdecodeinfo[0].color_codes_start + i + 256] = (char) (i);
                } else {
                    /* background */
                    colortable[Machine.drv.gfxdecodeinfo[0].color_codes_start + i] = (char) (BGPEN);
                    colortable[Machine.drv.gfxdecodeinfo[0].color_codes_start + i + 256] = (char) (SIDEPEN);
                }
            }

            color_prom.inc(2 * 256);

            /* big sprite */
            for (i = 0; i < 32; i++) {
                int bit0, bit1, bit2;

                /* red component */
                bit0 = (color_prom.read(i) >> 0) & 0x01;
                bit1 = (color_prom.read(i) >> 1) & 0x01;
                bit2 = (color_prom.read(i) >> 2) & 0x01;
                palette[p_ptr++] = (char) (0x20 * bit0 + 0x40 * bit1 + 0x80 * bit2);
                /* green component */
                bit0 = (color_prom.read(i) >> 3) & 0x01;
                bit1 = (color_prom.read(i) >> 4) & 0x01;
                bit2 = (color_prom.read(i) >> 5) & 0x01;
                palette[p_ptr++] = (char) (0x20 * bit0 + 0x40 * bit1 + 0x80 * bit2);
                /* blue component */
                bit0 = 0;
                bit1 = (color_prom.read(i) >> 6) & 0x01;
                bit2 = (color_prom.read(i) >> 7) & 0x01;
                palette[p_ptr++] = (char) (0x20 * bit0 + 0x40 * bit1 + 0x80 * bit2);

                if (i % 8 == 0) {
                    colortable[Machine.drv.gfxdecodeinfo[2].color_codes_start + i] = (char) (BGPEN);
                } /* enforce transparency */ else {
                    colortable[Machine.drv.gfxdecodeinfo[2].color_codes_start + i] = (char) (i + 256);
                }
            }

            /* background */
            palette[p_ptr++] = (char) (0);
            palette[p_ptr++] = (char) (0);
            palette[p_ptr++] = (char) (0);
            /* side panel background color */
            palette[p_ptr++] = (char) (0x20);
            palette[p_ptr++] = (char) (0x98);
            palette[p_ptr++] = (char) (0x79);

        }
    };

    /**
     * *************************************************************************
     *
     * Swimmer can directly set the background color. The latch is connected to
     * the RGB output this way: (the 500 and 250 ohm resistors are made of 1
     * kohm resistors in parallel)
     *
     * bit 7 -- 250 ohm resistor -- RED -- 500 ohm resistor -- RED -- 250 ohm
     * resistor -- GREEN -- 500 ohm resistor -- GREEN -- 1 kohm resistor --
     * GREEN -- 250 ohm resistor -- BLUE -- 500 ohm resistor -- BLUE bit 0 -- 1
     * kohm resistor -- BLUE
     *
     **************************************************************************
     */
    public static WriteHandlerPtr swimmer_bgcolor_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int bit0, bit1, bit2;
            int r, g, b;

            /* red component */
            bit0 = 0;
            bit1 = (data >> 6) & 0x01;
            bit2 = (data >> 7) & 0x01;
            r = 0x20 * bit0 + 0x40 * bit1 + 0x80 * bit2;

            /* green component */
            bit0 = (data >> 3) & 0x01;
            bit1 = (data >> 4) & 0x01;
            bit2 = (data >> 5) & 0x01;
            g = 0x20 * bit0 + 0x40 * bit1 + 0x80 * bit2;

            /* blue component */
            bit0 = (data >> 0) & 0x01;
            bit1 = (data >> 1) & 0x01;
            bit2 = (data >> 2) & 0x01;
            b = 0x20 * bit0 + 0x40 * bit1 + 0x80 * bit2;

            palette_set_color(BGPEN, r, g, b);
        }
    };

    /**
     * *************************************************************************
     *
     * Start the video hardware emulation.
     *
     **************************************************************************
     */
    public static VhStartPtr cclimber_vh_start = new VhStartPtr() {
        public int handler() {
            if (generic_vh_start.handler() != 0) {
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
    public static VhStopPtr cclimber_vh_stop = new VhStopPtr() {
        public void handler() {
            generic_vh_stop.handler();
        }
    };

    public static WriteHandlerPtr cclimber_colorram_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            if (colorram.read(offset) != data) {
                /* bit 5 of the address is not used for color memory. There is just */
 /* 512 bytes of memory; every two consecutive rows share the same memory */
 /* region. */
                offset &= 0xffdf;

                dirtybuffer[offset] = 1;
                dirtybuffer[offset + 0x20] = 1;

                colorram.write(offset, data);
                colorram.write(offset + 0x20, data);
            }
        }
    };

    public static WriteHandlerPtr cclimber_bigsprite_videoram_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cclimber_bsvideoram.write(offset, data);
        }
    };

    public static WriteHandlerPtr swimmer_palettebank_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            set_vh_global_attribute(palettebank, data & 1);
        }
    };

    public static WriteHandlerPtr swimmer_sidepanel_enable_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            set_vh_global_attribute(sidepanel_enabled, data);
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
    static void drawbigsprite(mame_bitmap bitmap) {
        int offs;
        int ox, oy, sx, sy, flipx, flipy;
        int color;

        ox = 136 - cclimber_bigspriteram.read(3);
        oy = 128 - cclimber_bigspriteram.read(2);
        flipx = cclimber_bigspriteram.read(1) & 0x10;
        flipy = cclimber_bigspriteram.read(1) & 0x20;
        if (flip_screen_y[0] != 0) /* only the Y direction has to be flipped */ {
            oy = 128 - oy;
            flipy = NOT(flipy);
        }
        color = cclimber_bigspriteram.read(1) & 0x07;
        /* cclimber */
        //	color = cclimber_bigspriteram[1] & 0x03;	/* swimmer */

        for (offs = cclimber_bsvideoram_size[0] - 1; offs >= 0; offs--) {
            sx = offs % 16;
            sy = offs / 16;
            if (flipx != 0) {
                sx = 15 - sx;
            }
            if (flipy != 0) {
                sy = 15 - sy;
            }

            drawgfx(bitmap, Machine.gfx[2],
                    //				cclimber_bsvideoram[offs],	/* cclimber */
                    cclimber_bsvideoram.read(offs) + ((cclimber_bigspriteram.read(1) & 0x08) << 5), /* swimmer */
                    color,
                    flipx, flipy,
                    (ox + 8 * sx) & 0xff, (oy + 8 * sy) & 0xff,
                    null, TRANSPARENCY_PEN, 0);

            /* wraparound */
            drawgfx(bitmap, Machine.gfx[2],
                    //				cclimber_bsvideoram[offs],	/* cclimber */
                    cclimber_bsvideoram.read(offs) + ((cclimber_bigspriteram.read(1) & 0x08) << 5), /* swimmer */
                    color,
                    flipx, flipy,
                    ((ox + 8 * sx) & 0xff) - 256, (oy + 8 * sy) & 0xff,
                    null, TRANSPARENCY_PEN, 0);
        }
    }

    public static VhUpdatePtr cclimber_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            int offs;

            if (full_refresh != 0) {
                memset(dirtybuffer, 1, videoram_size[0]);
            }

            /* for every character in the Video RAM, check if it has been modified */
 /* since last time and update it accordingly. */
            for (offs = videoram_size[0] - 1; offs >= 0; offs--) {
                if (dirtybuffer[offs] != 0) {
                    int sx, sy, flipx, flipy;

                    dirtybuffer[offs] = 0;

                    sx = offs % 32;
                    sy = offs / 32;
                    flipx = colorram.read(offs) & 0x40;
                    flipy = colorram.read(offs) & 0x80;
                    /* vertical flipping flips two adjacent characters */
                    if (flipy != 0) {
                        sy ^= 1;
                    }

                    if (flip_screen_x[0] != 0) {
                        sx = 31 - sx;
                        flipx = NOT(flipx);
                    }
                    if (flip_screen_y[0] != 0) {
                        sy = 31 - sy;
                        flipy = NOT(flipy);
                    }

                    drawgfx(tmpbitmap, Machine.gfx[(colorram.read(offs) & 0x10) != 0 ? 1 : 0],
                            videoram.read(offs) + 8 * (colorram.read(offs) & 0x20),
                            colorram.read(offs) & 0x0f,
                            flipx, flipy,
                            8 * sx, 8 * sy,
                            null, TRANSPARENCY_NONE, 0);
                }
            }

            /* copy the temporary bitmap to the screen */
            {
                int[] scroll = new int[32];

                if (flip_screen_x[0] != 0) {
                    for (offs = 0; offs < 32; offs++) {
                        scroll[offs] = -cclimber_column_scroll.read(31 - offs);
                        if (flip_screen_y[0] != 0) {
                            scroll[offs] = -scroll[offs];
                        }
                    }
                } else {
                    for (offs = 0; offs < 32; offs++) {
                        scroll[offs] = -cclimber_column_scroll.read(offs);
                        if (flip_screen_y[0] != 0) {
                            scroll[offs] = -scroll[offs];
                        }
                    }
                }

                copyscrollbitmap(bitmap, tmpbitmap, 0, null, 32, scroll, Machine.visible_area, TRANSPARENCY_NONE, 0);
            }

            if ((cclimber_bigspriteram.read(0) & 1) != 0) /* draw the "big sprite" below sprites */ {
                drawbigsprite(bitmap);
            }

            /* Draw the sprites. Note that it is important to draw them exactly in this */
 /* order, to have the correct priorities. */
            for (offs = spriteram_size[0] - 4; offs >= 0; offs -= 4) {
                int sx, sy, flipx, flipy;

                sx = spriteram.read(offs + 3);
                sy = 240 - spriteram.read(offs + 2);
                flipx = spriteram.read(offs) & 0x40;
                flipy = spriteram.read(offs) & 0x80;
                if (flip_screen_x[0] != 0) {
                    sx = 240 - sx;
                    flipx = NOT(flipx);
                }
                if (flip_screen_y[0] != 0) {
                    sy = 240 - sy;
                    flipy = NOT(flipy);
                }

                drawgfx(bitmap, Machine.gfx[(spriteram.read(offs + 1) & 0x10) != 0 ? 4 : 3],
                        (spriteram.read(offs) & 0x3f) + 2 * (spriteram.read(offs + 1) & 0x20),
                        spriteram.read(offs + 1) & 0x0f,
                        flipx, flipy,
                        sx, sy,
                        Machine.visible_area, TRANSPARENCY_PEN, 0);
            }

            if ((cclimber_bigspriteram.read(0) & 1) == 0) /* draw the "big sprite" over sprites */ {
                drawbigsprite(bitmap);
            }
        }
    };

    public static VhUpdatePtr swimmer_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            int offs;

            if (full_refresh != 0) {
                memset(dirtybuffer, 1, videoram_size[0]);
            }

            /* for every character in the Video RAM, check if it has been modified */
 /* since last time and update it accordingly. */
            for (offs = videoram_size[0] - 1; offs >= 0; offs--) {
                if (dirtybuffer[offs] != 0) {
                    int sx, sy, flipx, flipy, color;

                    dirtybuffer[offs] = 0;

                    sx = offs % 32;
                    sy = offs / 32;
                    flipx = colorram.read(offs) & 0x40;
                    flipy = colorram.read(offs) & 0x80;
                    /* vertical flipping flips two adjacent characters */
                    if (flipy != 0) {
                        sy ^= 1;
                    }

                    color = (colorram.read(offs) & 0x0f) + 0x10 * palettebank[0];
                    if ((sx >= 24) && (sidepanel_enabled[0] != 0)) {
                        color += 32;
                    }

                    if (flip_screen_x[0] != 0) {
                        sx = 31 - sx;
                        flipx = (flipx == 0) ? 1 : 0;
                    }
                    if (flip_screen_y[0] != 0) {
                        sy = 31 - sy;
                        flipy = (flipy == 0) ? 1 : 0;
                    }

                    drawgfx(tmpbitmap, Machine.gfx[0],
                            videoram.read(offs) + ((colorram.read(offs) & 0x10) << 4),
                            color,
                            flipx, flipy,
                            8 * sx, 8 * sy,
                            null, TRANSPARENCY_NONE, 0);
                }
            }

            /* copy the temporary bitmap to the screen */
            {
                int[] scroll = new int[32];

                if (flip_screen_y[0] != 0) {
                    for (offs = 0; offs < 32; offs++) {
                        scroll[offs] = cclimber_column_scroll.read(31 - offs);
                    }
                } else {
                    for (offs = 0; offs < 32; offs++) {
                        scroll[offs] = -cclimber_column_scroll.read(offs);
                    }
                }

                copyscrollbitmap(bitmap, tmpbitmap, 0, null, 32, scroll, Machine.visible_area, TRANSPARENCY_NONE, 0);
            }

            if ((cclimber_bigspriteram.read(0) & 1) != 0) /* draw the "big sprite" below sprites */ {
                drawbigsprite(bitmap);
            }

            /* Draw the sprites. Note that it is important to draw them exactly in this */
 /* order, to have the correct priorities. */
            for (offs = spriteram_size[0] - 4; offs >= 0; offs -= 4) {
                int sx, sy, flipx, flipy;

                sx = spriteram.read(offs + 3);
                sy = 240 - spriteram.read(offs + 2);
                flipx = spriteram.read(offs) & 0x40;
                flipy = spriteram.read(offs) & 0x80;
                if (flip_screen_x[0] != 0) {
                    sx = 240 - sx;
                    flipx = (flipx != 0) ? 0 : 1;
                }
                if (flip_screen_y[0] != 0) {
                    sy = 240 - sy;
                    flipy = (flipy != 0) ? 0 : 1;
                }

                drawgfx(bitmap, Machine.gfx[1],
                        (spriteram.read(offs) & 0x3f) | (spriteram.read(offs + 1) & 0x10) << 2,
                        (spriteram.read(offs + 1) & 0x0f) + 0x10 * palettebank[0],
                        flipx, flipy,
                        sx, sy,
                        Machine.visible_area, TRANSPARENCY_PEN, 0);
            }

            if ((cclimber_bigspriteram.read(0) & 1) == 0) /* draw the "big sprite" over sprites */ {
                drawbigsprite(bitmap);
            }
        }
    };
}
