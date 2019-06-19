/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
/**
 * Changelog
 * ---------
 * 19/06/2019 - added centiped vidhrdw driver (shadow)
 */
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.libc.cstring.*;
import static common.ptr.*;

import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.drawgfxH.*;
import static mame056.drawgfx.*;
import static mame056.palette.*;
import static mame056.mame.*;

import static mame056.vidhrdw.generic.*;

public class centiped {

    static rectangle spritevisiblearea = new rectangle(
            1 * 8, 31 * 8 - 1,
            0 * 8, 30 * 8 - 1
    );

    static rectangle spritevisiblearea_flip = new rectangle(
            1 * 8, 31 * 8 - 1,
            2 * 8, 32 * 8 - 1
    );

    /**
     * *************************************************************************
     *
     * Centipede doesn't have a color PROM. Eight RAM locations control the
     * color of characters and sprites. The meanings of the four bits are (all
     * bits are inverted):
     *
     * bit 3 alternate blue green bit 0 red
     *
     * The alternate bit affects blue and green, not red. The way I weighted its
     * effect might not be perfectly accurate, but is reasonably close.
     *
     **************************************************************************
     */
    static int TOTAL_COLORS(int gfxn) {
        return Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity;
    }
    public static VhConvertColorPromPtr centiped_init_palette = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
            int i;
            //#define TOTAL_COLORS(gfxn) (Machine->gfx[gfxn]->total_colors * Machine->gfx[gfxn]->color_granularity)
            //#define COLOR(gfxn,offs) (colortable[Machine->drv->gfxdecodeinfo[gfxn].color_codes_start + offs])

            /* characters use colors 0-3, that become 0-15 due to raster effects */
            for (i = 0; i < TOTAL_COLORS(0); i++) {
                colortable[Machine.drv.gfxdecodeinfo[0].color_codes_start + i] = (char) (i);
            }

            /* Centipede is unusual because the sprite color code specifies the */
 /* colors to use one by one, instead of a combination code. */
 /* bit 5-4 = color to use for pen 11 */
 /* bit 3-2 = color to use for pen 10 */
 /* bit 1-0 = color to use for pen 01 */
 /* pen 00 is transparent */
            for (i = 0; i < TOTAL_COLORS(1); i += 4) {
                colortable[Machine.drv.gfxdecodeinfo[1].color_codes_start + i + 0] = (char) (16);
                colortable[Machine.drv.gfxdecodeinfo[1].color_codes_start + i + 1] = (char) (16 + ((i >> 2) & 3));
                colortable[Machine.drv.gfxdecodeinfo[1].color_codes_start + i + 2] = (char) (16 + ((i >> 4) & 3));
                colortable[Machine.drv.gfxdecodeinfo[1].color_codes_start + i + 3] = (char) (16 + ((i >> 6) & 3));
            }
        }
    };

    static void setcolor(int pen, int data) {
        int r, g, b;

        r = 0xff * ((~data >> 0) & 1);
        g = 0xff * ((~data >> 1) & 1);
        b = 0xff * ((~data >> 2) & 1);

        if ((~data & 0x08) != 0) /* alternate = 1 */ {
            /* when blue component is not 0, decrease it. When blue component is 0, */
 /* decrease green component. */
            if (b != 0) {
                b = 0xc0;
            } else if (g != 0) {
                g = 0xc0;
            }
        }

        palette_set_color(pen, r, g, b);
    }

    public static WriteHandlerPtr centiped_paletteram_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram.write(offset, data);

            /* the char palette will be effectively updated by the next interrupt handler */
            if (offset >= 12 && offset < 16) /* sprites palette */ {
                setcolor(16 + (offset - 12), data);
            }
        }
    };

    static int powerup_counter;

    public static InitMachinePtr centiped_init_machine = new InitMachinePtr() {
        public void handler() {
            powerup_counter = 10;
        }
    };

    public static InterruptPtr centiped_interrupt = new InterruptPtr() {
        public int handler() {
            int offset;
            int slice = 3 - cpu_getiloops();

            /* set the palette for the previous screen slice to properly support */
 /* midframe palette changes in test mode */
            for (offset = 4; offset < 8; offset++) {
                setcolor(4 * slice + (offset - 4), paletteram.read(offset));
            }

            /* Centipede doesn't like to receive interrupts just after a reset. */
 /* The only workaround I've found is to wait a little before starting */
 /* to generate them. */
            if (powerup_counter == 0) {
                return interrupt.handler();
            } else {
                powerup_counter--;
                return ignore_interrupt.handler();
            }
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
    public static VhUpdatePtr centiped_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            int offs;

            if (full_refresh != 0) {
                memset(dirtybuffer, 1, videoram_size[0]);
            }

            for (offs = videoram_size[0] - 1; offs >= 0; offs--) {
                if (dirtybuffer[offs] != 0) {
                    int sx, sy;

                    dirtybuffer[offs] = 0;

                    sx = offs % 32;
                    sy = offs / 32;

                    if (flip_screen() != 0) {
                        sy += 2;
                    }

                    drawgfx(tmpbitmap, Machine.gfx[0],
                            (videoram.read(offs) & 0x3f) + 0x40,
                            (sy + 1) / 8, /* support midframe palette changes in test mode */
                            flip_screen(), flip_screen(),
                            8 * sx, 8 * sy,
                            Machine.visible_area, TRANSPARENCY_NONE, 0);
                }
            }

            /* copy the temporary bitmap to the screen */
            copybitmap(bitmap, tmpbitmap, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_NONE, 0);

            /* Draw the sprites */
            for (offs = 0; offs < 0x10; offs++) {
                int code, color;
                int flipx;
                int x, y;

                code = ((spriteram.read(offs) & 0x3e) >> 1) | ((spriteram.read(offs) & 0x01) << 6);
                color = spriteram.read(offs + 0x30);
                flipx = (spriteram.read(offs) & 0x80);
                x = spriteram.read(offs + 0x20);
                y = 240 - spriteram.read(offs + 0x10);

                if (flip_screen() != 0) {
                    y += 16;
                }

                drawgfx(bitmap, Machine.gfx[1],
                        code,
                        color & 0x3f,
                        flip_screen(), flipx,
                        x, y,
                        flip_screen() != 0 ? spritevisiblearea_flip : spritevisiblearea,
                        TRANSPARENCY_PEN, 0);
            }
        }
    };
}
