/**
 * ported to v0.56
 *
 */
/**
 * Changelog
 * ---------
 * 22/05/2019 - ported lsasquad vidhrdw to 0.56 (shadow)
 */
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.libc.expressions.*;
import static common.ptr.*;


import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

public class lsasquad {

    public static UBytePtr lsasquad_scrollram = new UBytePtr();

    static void draw_layer(mame_bitmap bitmap, UBytePtr scrollram) {
        int offs, scrollx, scrolly;

        scrollx = scrollram.read(3);
        scrolly = -scrollram.read(0);

        for (offs = 0; offs < 0x080; offs += 4) {
            int base, y, sx, sy, code, color;

            base = 64 * scrollram.read(offs + 1);
            sx = 8 * (offs / 4) + scrollx;
            if (flip_screen() != 0) {
                sx = 248 - sx;
            }
            sx &= 0xff;

            for (y = 0; y < 32; y++) {
                int attr;

                sy = 8 * y + scrolly;
                if (flip_screen() != 0) {
                    sy = 248 - sy;
                }
                sy &= 0xff;

                attr = videoram.read(base + 2 * y + 1);
                code = videoram.read(base + 2 * y) + ((attr & 0x0f) << 8);
                color = attr >> 4;

                drawgfx(bitmap, Machine.gfx[0],
                        code,
                        color,
                        flip_screen(), flip_screen(),
                        sx, sy,
                        Machine.visible_area, TRANSPARENCY_PEN, 15);
                if (sx > 248) /* wraparound */ {
                    drawgfx(bitmap, Machine.gfx[0],
                            code,
                            color,
                            flip_screen(), flip_screen(),
                            sx - 256, sy,
                            Machine.visible_area, TRANSPARENCY_PEN, 15);
                }
            }
        }
    }

    static void draw_sprites(mame_bitmap bitmap) {
        int offs;

        for (offs = spriteram_size[0] - 4; offs >= 0; offs -= 4) {
            int sx, sy, attr, code, color, flipx, flipy;

            sx = spriteram.read(offs + 3);
            sy = 240 - spriteram.read(offs);
            attr = spriteram.read(offs + 1);
            code = spriteram.read(offs + 2) + ((attr & 0x30) << 4);
            color = attr & 0x0f;
            flipx = attr & 0x40;
            flipy = attr & 0x80;

            if (flip_screen() != 0) {
                sx = 240 - sx;
                sy = 240 - sy;
                flipx = NOT(flipx);
                flipy = NOT(flipy);
            }

            drawgfx(bitmap, Machine.gfx[1],
                    code,
                    color,
                    flipx, flipy,
                    sx, sy,
                    Machine.visible_area, TRANSPARENCY_PEN, 15);
            /* wraparound */
            drawgfx(bitmap, Machine.gfx[1],
                    code,
                    color,
                    flipx, flipy,
                    sx - 256, sy,
                    Machine.visible_area, TRANSPARENCY_PEN, 15);
        }
    }

    public static VhUpdatePtr lsasquad_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            fillbitmap(bitmap, Machine.pens[511], Machine.visible_area);

            draw_layer(bitmap, new UBytePtr(lsasquad_scrollram, 0x000));
            draw_layer(bitmap, new UBytePtr(lsasquad_scrollram, 0x080));
            draw_sprites(bitmap);
            draw_layer(bitmap, new UBytePtr(lsasquad_scrollram, 0x100));
        }
    };
}
