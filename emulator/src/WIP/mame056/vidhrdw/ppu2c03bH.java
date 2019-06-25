/**
 * ported to v0.56
 */
package WIP.mame056.vidhrdw;

public class ppu2c03bH {

    /* increment to use more chips */
    public static final int MAX_PPU = 2;

    /* mirroring types */
    public static final int PPU_MIRROR_NONE = 0;
    public static final int PPU_MIRROR_VERT = 1;
    public static final int PPU_MIRROR_HORZ = 2;
    public static final int PPU_MIRROR_HIGH = 3;
    public static final int PPU_MIRROR_LOW = 4;

    /* callback datatypes */
    public static abstract interface ppu2c03b_scanline_cb {

        public abstract void handler(int num, int scanline, int vblank, int blanked);
    }

    public static abstract interface ppu2c03b_irq_cb {

        public abstract void handler(int num);
    }

    public static abstract interface ppu2c03b_vidaccess_cb {

        public abstract int handler(int num, int address, int data);
    }

    public static class ppu2c03b_interface {

        public ppu2c03b_interface(int num, int[] vrom_region, int[] gfx_layout_number, int[] color_base, int[] mirroring, ppu2c03b_irq_cb[] handler) {
            this.num = num;
            this.vrom_region = vrom_region;
            this.gfx_layout_number = gfx_layout_number;
            this.color_base = color_base;
            this.mirroring = mirroring;
            this.handler = handler;
        }

        public int num;
        /* number of chips ( 1 to MAX_PPU ) */
        public int[] vrom_region;//[MAX_PPU];			/* region id of gfx vrom (or REGION_INVALID if none) */
        public int[] gfx_layout_number;//[MAX_PPU];		/* gfx layout number used by each chip */
        public int[] color_base;//[MAX_PPU];			/* color base to use per ppu */
        public int[] mirroring;//[MAX_PPU];				/* mirroring options (PPU_MIRROR_* flag) */
        public ppu2c03b_irq_cb[] handler;//[MAX_PPU];				/* IRQ handler */
    }
}
