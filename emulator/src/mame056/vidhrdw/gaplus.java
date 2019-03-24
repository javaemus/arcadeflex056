/**
 * Ported to v0.56
 * ported to v0.37b7
 *
 */
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.expressions.NOT;
import static common.ptr.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;
import static mame056.machine.gaplus.*;

public class gaplus {

    static int TOTAL_COLORS(int gfxn) {
        return Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity;
    }

    public static VhConvertColorPromPtr gaplus_vh_convert_color_prom = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
            int i;
            int p_inc = 0;
            for (i = 0; i < Machine.drv.total_colors; i++) {
                int bit0, bit1, bit2, bit3;

                /* red component */
                bit0 = (color_prom.read(0) >> 0) & 0x01;
                bit1 = (color_prom.read(0) >> 1) & 0x01;
                bit2 = (color_prom.read(0) >> 2) & 0x01;
                bit3 = (color_prom.read(0) >> 3) & 0x01;
                palette[p_inc++] = ((char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3));
                /* green component */
                bit0 = (color_prom.read(Machine.drv.total_colors) >> 0) & 0x01;
                bit1 = (color_prom.read(Machine.drv.total_colors) >> 1) & 0x01;
                bit2 = (color_prom.read(Machine.drv.total_colors) >> 2) & 0x01;
                bit3 = (color_prom.read(Machine.drv.total_colors) >> 3) & 0x01;
                palette[p_inc++] = ((char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3));
                /* blue component */
                bit0 = (color_prom.read(2 * Machine.drv.total_colors) >> 0) & 0x01;
                bit1 = (color_prom.read(2 * Machine.drv.total_colors) >> 1) & 0x01;
                bit2 = (color_prom.read(2 * Machine.drv.total_colors) >> 2) & 0x01;
                bit3 = (color_prom.read(2 * Machine.drv.total_colors) >> 3) & 0x01;
                palette[p_inc++] = ((char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3));

                color_prom.inc();
            }

            color_prom.inc(2 * Machine.drv.total_colors);
            /* color_prom now points to the beginning of the lookup table */

 /* characters use colors 240-255 */
            for (i = 0; i < TOTAL_COLORS(0); i++) {
                colortable[Machine.drv.gfxdecodeinfo[0].color_codes_start + i] = (char) (240 + ((color_prom.readinc()) & 0x0f));
            }

            /* sprites */
            for (i = 0; i < TOTAL_COLORS(2); i++) {
                colortable[Machine.drv.gfxdecodeinfo[2].color_codes_start + i] = (char) ((color_prom.read(0) & 0x0f) + ((color_prom.read(TOTAL_COLORS(2)) & 0x0f) << 4));
                color_prom.inc();
            }
        }
    };

    /**
     * *************************************************************************
     * Starfield information There's 3 sets of stars planes at different speeds.
     * <p>
     * a000 --. (bit 0 = 1) enable starfield. (bit 0 = 0) disable starfield.
     * a001 --. starfield plane 0 control a002 --. starfield plane 1 control
     * a003 --. starfield plane 2 control
     * *************************************************************************
     */
    public static int MAX_STARS = 250;

    /* starfield speed constants (bigger = faster) */
    public static double SPEED_1 = 0.5;
    public static double SPEED_2 = 1.0;
    public static double SPEED_3 = 2.0;

    static class star {

        public star() {
        }

        public int x, y, col, set;
    }

    static star stars[] = new star[MAX_STARS];

    static {
        for (int k = 0; k < MAX_STARS; k++) {
            stars[k] = new star();
        }
    }

    static /*unsigned*/ char[] gaplus_starfield_control = new char[4];
    static int total_stars;

    public static void starfield_init() {

        int generator = 0;
        int x, y;
        int set = 0;
        int width, height;

        width = Machine.drv.screen_width;
        height = Machine.drv.screen_height;

        total_stars = 0;

        /* precalculate the star background */
 /* this comes from the Galaxian hardware, Gaplus is probably different */
        for (y = 0; y < height; y++) {
            for (x = width * 2 - 1; x >= 0; x--) {
                int bit1, bit2;

                generator <<= 1;
                bit1 = (~generator >> 17) & 1;
                bit2 = (generator >> 5) & 1;

                if ((bit1 ^ bit2) != 0) {
                    generator |= 1;
                }

                if (((~generator >> 16) & 1) != 0 && (generator & 0xff) == 0xff) {
                    int color;

                    color = (~(generator >> 8)) & 0x3f;
                    if (color != 0 && total_stars < MAX_STARS) {
                        stars[total_stars].x = x;
                        stars[total_stars].y = y;
                        stars[total_stars].col = Machine.pens[color];
                        stars[total_stars].set = set++;

                        if (set == 3) {
                            set = 0;
                        }

                        total_stars++;
                    }
                }
            }
        }
    }

    public static void gaplus_starfield_update() {
        int i;
        int width, height;

        width = Machine.drv.screen_width;
        height = Machine.drv.screen_height;

        /* check if we're running */
        if ((gaplus_starfield_control[0] & 1) == 0) {
            return;
        }

        /* update the starfields */
        for (i = 0; i < total_stars; i++) {
            switch (gaplus_starfield_control[stars[i].set + 1]) {
                case 0x87:
                    /* stand still */
                    break;

                case 0x86:
                    /* scroll down (speed 1) */
                    stars[i].x += SPEED_1;
                    break;

                case 0x85:
                    /* scroll down (speed 2) */
                    stars[i].x += SPEED_2;
                    break;

                case 0x06:
                    /* scroll down (speed 3) */
                    stars[i].x += SPEED_3;
                    break;

                case 0x80:
                    /* scroll up (speed 1) */
                    stars[i].x -= SPEED_1;
                    break;

                case 0x82:
                    /* scroll up (speed 2) */
                    stars[i].x -= SPEED_2;
                    break;

                case 0x81:
                    /* scroll up (speed 3) */
                    stars[i].x -= SPEED_3;
                    break;

                case 0x9f:
                    /* scroll left (speed 2) */
                    stars[i].y += SPEED_2;
                    break;

                case 0xaf:
                    /* scroll left (speed 1) */
                    stars[i].y += SPEED_1;
                    break;
            }

            /* wrap */
            if (stars[i].x < 0) {
                stars[i].x = (int) ((float) (width * 2) + stars[i].x);
            }

            if (stars[i].x >= (float) (width * 2)) {
                stars[i].x -= (float) (width * 2);
            }

            if (stars[i].y < 0) {
                stars[i].y = (int) ((float) (height) + stars[i].y);
            }

            if (stars[i].y >= (float) (height)) {
                stars[i].y -= (float) (height);
            }
        }
    }

    static void starfield_render(mame_bitmap bitmap) {

        int i;
        int width, height;

        width = Machine.drv.screen_width;
        height = Machine.drv.screen_height;

        /* check if we're running */
        if ((gaplus_starfield_control[0] & 1) == 0) {
            return;
        }

        /* draw the starfields */
        for (i = 0; i < total_stars; i++) {
            int x, y;

            x = stars[i].x;
            y = stars[i].y;

            if (x >= 0 && x < width && y >= 0 && y < height) {
                plot_pixel.handler(bitmap, x, y, stars[i].col);
            }
        }
    }

    public static WriteHandlerPtr gaplus_starfield_control_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            gaplus_starfield_control[offset] = (char) (data & 0xFF);
        }
    };

    public static VhStartPtr gaplus_vh_start = new VhStartPtr() {
        public int handler() {

            /* set up spriteram area */
            spriteram_size[0] = 0x80;
            spriteram = new UBytePtr(gaplus_sharedram, 0x780);
            spriteram_2 = new UBytePtr(gaplus_sharedram, 0x780 + 0x800);
            spriteram_3 = new UBytePtr(gaplus_sharedram, 0x780 + 0x800 + 0x800);

            starfield_init();

            return generic_vh_start.handler();
        }
    };

    public static VhStopPtr gaplus_vh_stop = new VhStopPtr() {
        public void handler() {

            generic_vh_stop.handler();
        }
    };

    /**
     * *************************************************************************
     * Display Refresh
     * *************************************************************************
     */
    static void gaplus_draw_sprites(mame_bitmap bitmap) {
        int offs;

        for (offs = 0; offs < spriteram_size[0]; offs += 2) {
            if ((spriteram_3.read(offs + 1) & 2) == 0) {
                int number = spriteram.read(offs) + 4 * (spriteram_3.read(offs) & 0x40);
                int color = spriteram.read(offs + 1) & 0x3f;
                int sx = (spriteram_2.read(offs + 1) - 71) + 0x100 * (spriteram_3.read(offs + 1) & 1);
                int sy = (Machine.drv.screen_height) - spriteram_2.read(offs) - 24;
                int flipy = spriteram_3.read(offs) & 2;
                int flipx = spriteram_3.read(offs) & 1;
                int width, height;

                if (number >= 128 * 3) {
                    continue;
                }

                if (flip_screen() != 0) {
                    flipx = NOT(flipx);
                    flipy = NOT(flipy);
                }

                if ((spriteram_3.read(offs) & 0xa8) == 0xa0) {
                    /* draw the sprite twice in a row */
                    drawgfx(bitmap, Machine.gfx[2 + (number >> 7)],
                            number, color, flipx, flipy, sx, sy,
                            Machine.visible_area, TRANSPARENCY_COLOR, 255);
                    drawgfx(bitmap, Machine.gfx[2 + (number >> 7)],
                            number, color, flipx, flipy, sx, sy + 16,
                            Machine.visible_area, TRANSPARENCY_COLOR, 255);
                } else {
                    switch (spriteram_3.read(offs) & 0x28) {
                        case 0x28:
                            /* 2x both ways */
                            width = height = 2;
                            number &= (~3);
                            break;
                        case 0x20:
                            /* 2x vertical */
                            width = 1;
                            height = 2;
                            number &= (~2);
                            break;
                        case 0x08:
                            /* 2x horizontal */
                            width = 2;
                            height = 1;
                            number &= (~1);
                            sy += 16;
                            break;
                        default:
                            /* normal sprite */
                            width = height = 1;
                            sy += 16;
                            break;
                    }
                    {
                        int[] x_offset = {0x00, 0x01};
                        int[] y_offset = {0x00, 0x02};
                        int x, y, ex, ey;

                        for (y = 0; y < height; y++) {
                            for (x = 0; x < width; x++) {
                                ex = flipx != 0 ? (width - 1 - x) : x;
                                ey = flipy != 0 ? (height - 1 - y) : y;

                                drawgfx(bitmap, Machine.gfx[2 + (number >> 7)],
                                        (number) + x_offset[ex] + y_offset[ey],
                                        color,
                                        flipx, flipy,
                                        sx + x * 16, sy + y * 16,
                                        Machine.visible_area,
                                        TRANSPARENCY_COLOR, 255);
                            }
                        }
                    }
                }
            }
        }
    }

    public static VhUpdatePtr gaplus_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            int offs;

            fillbitmap(bitmap, Machine.pens[0], Machine.visible_area);

            starfield_render(bitmap);

            /* colorram layout: */
 /* bit 7 = bank */
 /* bit 6 = not used? */
 /* bit 5-0 = color */
 /* for every character in the Video RAM, check if it has been modified */
 /* since last time and update it accordingly. */
            for (offs = videoram_size[0] - 1; offs >= 0; offs--) {
                int sx, sy, mx, my, bank;

                /* Even if Gaplus screen is 28x36, the memory layout is 32x32. We therefore
	        have to convert the memory coordinates into screen coordinates.
	        Note that 32*32 = 1024, while 28*36 = 1008: therefore 16 bytes of Video RAM
	        don't map to a screen position. We don't check that here, however: range
	        checking is performed by drawgfx(). */
                mx = offs / 32;
                my = offs % 32;

                if (mx <= 1) /* bottom screen characters */ {
                    sx = 29 - my;
                    sy = mx + 34;
                } else if (mx >= 30) /* top screen characters */ {
                    sx = 29 - my;
                    sy = mx - 30;
                } else /* middle screen characters */ {
                    sx = 29 - mx;
                    sy = my + 2;
                }

                if (flip_screen() != 0) {
                    sx = 27 - sx;
                    sy = 35 - sy;
                }

                sx = ((Machine.drv.screen_height - 1) / 8) - sx;

                bank = (colorram.read(offs) & 0x80) != 0 ? 1 : 0;

                drawgfx(bitmap, Machine.gfx[bank],
                        videoram.read(offs),
                        colorram.read(offs) & 0x3f,
                        flip_screen(), flip_screen(), 8 * sy, 8 * sx,
                        Machine.visible_area, TRANSPARENCY_PEN, 0);
            }

            gaplus_draw_sprites(bitmap);
        }
    };
}
