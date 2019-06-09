/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.libc.cstring.*;
import static common.ptr.*;

import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

public class mole {

    static int tile_bank;
    static UShortPtr tile_data;
    static final int NUM_ROWS = 25;
    static final int NUM_COLS = 40;
    static final int NUM_TILES = (NUM_ROWS * NUM_COLS);

    public static VhConvertColorPromPtr moleattack_vh_convert_color_prom = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
            int i;
            int p_ptr = 0;
            for (i = 0; i < 8; i++) {
                colortable[i] = (char) i;
                palette[p_ptr++] = (i & 1) != 0 ? 0xff : (char) 0x00;
                palette[p_ptr++] = (i & 4) != 0 ? 0xff : (char) 0x00;
                palette[p_ptr++] = (i & 2) != 0 ? 0xff : (char) 0x00;
            }
        }
    };

    public static VhStartPtr moleattack_vh_start = new VhStartPtr() {
        public int handler() {
            tile_data = new UShortPtr(NUM_TILES * 2*2);
            if (tile_data != null) {
                dirtybuffer = new char[NUM_TILES];
                if (dirtybuffer != null) {
                    memset(dirtybuffer, 1, NUM_TILES);
                    return 0;
                }
                tile_data = null;
            }
            return 1;
            /* error */
        }
    };

    public static VhStopPtr moleattack_vh_stop = new VhStopPtr() {
        public void handler() {
            dirtybuffer = null;
            tile_data = null;
        }
    };

    public static WriteHandlerPtr moleattack_videoram_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            if (offset < NUM_TILES) {
                if (tile_data.read(offset) != data) {
                    dirtybuffer[offset] = 1;
                    tile_data.write(offset, (char) (data | (tile_bank << 8)));
                }
            } else if (offset == 0x3ff) {
                /* hack!  erase screen */
                memset(dirtybuffer, 1, NUM_TILES);
                memset(tile_data, 0, NUM_TILES * 2);
            }
        }
    };

    public static WriteHandlerPtr moleattack_tilesetselector_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            tile_bank = data;
        }
    };

    public static VhUpdatePtr moleattack_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            int offs;

            if (full_refresh != 0) {
                memset(dirtybuffer, 1, NUM_TILES);
            }

            for (offs = 0; offs < NUM_TILES; offs++) {
                if (dirtybuffer[offs] != 0) {
                    char code = tile_data.read(offs);
                    drawgfx(bitmap, Machine.gfx[(code & 0x200) != 0 ? 1 : 0],
                            code & 0x1ff,
                            0, /* color */
                            0, 0, /* no flip */
                            (offs % NUM_COLS) * 8, /* xpos */
                            (offs / NUM_COLS) * 8, /* ypos */
                            null, /* no clip */
                            TRANSPARENCY_NONE, 0);

                    dirtybuffer[offs] = 0;
                }
            }
        }
    };
}
