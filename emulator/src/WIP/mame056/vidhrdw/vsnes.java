/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package WIP.mame056.vidhrdw;

import static WIP.mame056.machine.vsnes.*;
import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;

import static common.ptr.*;

import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.sound.mixer.*;
import static mame056.sound.mixerH.*;
import static mame056.inptport.*;
import static mame056.vidhrdw.generic.*;
import static WIP.mame056.vidhrdw.ppu2c03b.*;
import static WIP.mame056.vidhrdw.ppu2c03bH.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;

public class vsnes {

    /* from machine */
    public static VhConvertColorPromPtr vsnes_vh_convert_color_prom = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
            ppu2c03b_init_palette(0, palette);
        }
    };

    public static VhConvertColorPromPtr vsdual_vh_convert_color_prom = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
            ppu2c03b_init_palette(3 * 64, palette);
            //ppu2c03b_init_palette( 0,palette );
            /*TODO*///ppu2c03b_init_palette( &palette[3*64] );
        }
    };

    static ppu2c03b_irq_cb ppu_irq = new ppu2c03b_irq_cb() {
        public void handler(int num) {
            cpu_set_nmi_line(num, PULSE_LINE);
        }
    };

    /* our ppu interface											*/
    static ppu2c03b_interface ppu_interface = new ppu2c03b_interface(
            1, /* num */
            new int[]{REGION_GFX1}, /* vrom gfx region */
            new int[]{0}, /* gfxlayout num */
            new int[]{0}, /* color base */
            new int[]{PPU_MIRROR_NONE}, /* mirroring */
            new ppu2c03b_irq_cb[]{ppu_irq} /* irq */
    );

    /* our ppu interface for dual games								*/
    static ppu2c03b_interface ppu_dual_interface = new ppu2c03b_interface(
            2, /* num */
            new int[]{REGION_GFX1, REGION_GFX2}, /* vrom gfx region */
            new int[]{0, 1}, /* gfxlayout num */
            new int[]{0, 64}, /* color base */
            new int[]{PPU_MIRROR_NONE, PPU_MIRROR_NONE}, /* mirroring */
            new ppu2c03b_irq_cb[]{ppu_irq, ppu_irq} /* irq */
    );

    public static VhStartPtr vsnes_vh_start = new VhStartPtr() {
        public int handler() {
            return ppu2c03b_init(ppu_interface);
        }
    };

    public static VhStartPtr vsdual_vh_start = new VhStartPtr() {
        public int handler() {
            return ppu2c03b_init(ppu_dual_interface);
        }
    };

    public static VhStopPtr vsnes_vh_stop = new VhStopPtr() {
        public void handler() {
            ppu2c03b_dispose();
        }
    };

    /**
     * *************************************************************************
     *
     * Display refresh
     *
     **************************************************************************
     */
    public static VhUpdatePtr vsnes_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            /* render the ppu */
            ppu2c03b_render(0, bitmap, 0, 0, 0, 0);

            /* if this is a gun game, draw a simple crosshair */
            if (vsnes_gun_controller != 0) {
                int x_center = readinputport(4);
                int y_center = readinputport(5);

                draw_crosshair(bitmap, x_center, y_center, Machine.visible_area);

            }

        }
    };

    public static VhUpdatePtr vsdual_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            /* render the ppu's */
            ppu2c03b_render(0, bitmap, 0, 0, 0, 0);
            ppu2c03b_render(1, bitmap, 0, 0, 32 * 8, 0);
        }
    };
}
