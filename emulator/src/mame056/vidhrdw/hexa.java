/**
 * ported to v0.56
 * ported to v0.37b16
 * ported to v0.37b7
 *
 */
/**
 * Changelog
 * ---------
 * 12/05/2019 - ported hexa vidhrdw to 0.56 (shadow)
 */
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.vidhrdw.generic.*;
import static mame056.mame.*;
import static mame056.drawgfx.*;
import static common.libc.cstring.*;
import static mame056.common.*;
import static mame056.drawgfxH.*;
import static mame056.memoryH.*;
import static mame056.commonH.*;

public class hexa {

    static int charbank;
    static int flipx, flipy;

    public static WriteHandlerPtr hexa_d008_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int bankaddress;
            UBytePtr RAM = memory_region(REGION_CPU1);

            /* bit 0 = flipx (or y?) */
            if (flipx != (data & 0x01)) {
                flipx = data & 0x01;
                memset(dirtybuffer, 1, videoram_size[0]);
            }

            /* bit 1 = flipy (or x?) */
            if (flipy != (data & 0x02)) {
                flipy = data & 0x02;
                memset(dirtybuffer, 1, videoram_size[0]);
            }

            /* bit 2 - 3 unknown */
 /* bit 4 could be the ROM bank selector for 8000-bfff (not sure) */
            bankaddress = 0x10000 + ((data & 0x10) >> 4) * 0x4000;
            cpu_setbank(1, new UBytePtr(RAM, bankaddress));

            /* bit 5 = char bank */
            if (charbank != ((data & 0x20) >> 5)) {
                charbank = (data & 0x20) >> 5;
                memset(dirtybuffer, 1, videoram_size[0]);
            }

            /* bit 6 - 7 unknown */
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
    public static VhUpdatePtr hexa_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            int offs;

            /* for every character in the Video RAM, check if it has been modified */
 /* since last time and update it accordingly. */
            for (offs = videoram_size[0] - 2; offs >= 0; offs -= 2) {
                if (dirtybuffer[offs] != 0 || dirtybuffer[offs + 1] != 0) {
                    int sx, sy;

                    dirtybuffer[offs] = 0;
                    dirtybuffer[offs + 1] = 0;

                    sx = (offs / 2) % 32;
                    sy = (offs / 2) / 32;
                    if (flipx != 0) {
                        sx = 31 - sx;
                    }
                    if (flipy != 0) {
                        sy = 31 - sy;
                    }

                    drawgfx(tmpbitmap, Machine.gfx[0],
                            videoram.read(offs + 1) + ((videoram.read(offs) & 0x07) << 8) + (charbank << 11),
                            (videoram.read(offs) & 0xf8) >> 3,
                            flipx, flipy,
                            8 * sx, 8 * sy,
                            Machine.visible_area, TRANSPARENCY_NONE, 0);
                }
            }

            copybitmap(bitmap, tmpbitmap, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_NONE, 0);
        }
    };
}
