/**
 * ported to v0.56
 * ported to v0.37b16
 * ported to v0.37b7
 *
 */
/**
 * Changelog
 * ---------
 * 12/05/2019 - ported polyplay vidhrdw to 0.56 (shadow)
 */
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;
import static common.libc.cstring.*;
import static common.libc.expressions.*;

import static mame056.mame.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.commonH.*;

import static mame056.vidhrdw.generic.*;

public class polyplay {

    public static UBytePtr polyplay_characterram = new UBytePtr();
    static char[] dirtycharacter = new char[256];

    public static VhConvertColorPromPtr polyplay_init_palette = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
            int i;

            char polyplay_palette[]
                    = {
                        0x00, 0x00, 0x00,
                        0xff, 0xff, 0xff,
                        0x00, 0x00, 0x00,
                        0xff, 0x00, 0x00,
                        0x00, 0xff, 0x00,
                        0xff, 0xff, 0x00,
                        0x00, 0x00, 0xff,
                        0xff, 0x00, 0xff,
                        0x00, 0xff, 0xff,
                        0xff, 0xff, 0xff,};

            memcpy(palette, polyplay_palette, sizeof(polyplay_palette));

        }
    };

    public static WriteHandlerPtr polyplay_characterram_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            if (polyplay_characterram.read(offset) != data) {
                dirtycharacter[((offset / 8) & 0x7f) + 0x80] = 1;

                polyplay_characterram.write(offset, data);
            }
        }
    };

    public static ReadHandlerPtr polyplay_characterram_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return polyplay_characterram.read(offset);
        }
    };

    public static VhUpdatePtr polyplay_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            int offs;

            if (full_refresh != 0) {
                memset(dirtybuffer, 1, videoram_size[0]);
            }

            /* for every character in the Video RAM, check if it has been modified */
 /* since last time and update it accordingly. */
            for (offs = videoram_size[0] - 1; offs >= 0; offs--) {
                int charcode;

                charcode = videoram.read(offs);

                if (dirtybuffer[offs] != 0 || dirtycharacter[charcode] != 0) {
                    int sx, sy;

                    /* index=0 . 1 bit chr; index=1 . 3 bit chr */
                    if (charcode < 0x80) {

                        /* ROM chr, no need for decoding */
                        dirtybuffer[offs] = 0;

                        sx = offs % 64;
                        sy = offs / 64;

                        drawgfx(bitmap, Machine.gfx[0],
                                charcode,
                                0x0,
                                0, 0,
                                8 * sx, 8 * sy,
                                Machine.visible_area, TRANSPARENCY_NONE, 0);

                    } else {
                        /* decode modified characters */
                        if (dirtycharacter[charcode] == 1) {
                            decodechar(Machine.gfx[1], charcode - 0x80, polyplay_characterram, Machine.drv.gfxdecodeinfo[1].gfxlayout);
                            dirtycharacter[charcode] = 2;
                        }

                        dirtybuffer[offs] = 0;

                        sx = offs % 64;
                        sy = offs / 64;

                        drawgfx(bitmap, Machine.gfx[1],
                                charcode,
                                0x0,
                                0, 0,
                                8 * sx, 8 * sy,
                                Machine.visible_area, TRANSPARENCY_NONE, 0);

                    }
                }
            }

            for (offs = 0; offs < 256; offs++) {
                if (dirtycharacter[offs] == 2) {
                    dirtycharacter[offs] = 0;
                }
            }
        }
    };
}
