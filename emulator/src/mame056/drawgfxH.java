/**
 * Ported to 0.56
 */
package mame056;

import static common.ptr.*;
import java.util.Arrays;
import mame056.commonH.mame_bitmap;
import common.subArrays.IntArray;
import static mame056.drawgfx.alpha_cache;

public class drawgfxH {

    public static final int MAX_GFX_PLANES = 8;
    public static final int MAX_GFX_SIZE = 64;

    public static int RGN_FRAC(int num, int den) {
        return (0x80000000 | ((num & 0x0f) << 27) | ((den & 0x0f) << 23));
    }

    public static int IS_FRAC(int offset) {
        return ((offset) & 0x80000000);
    }

    public static int FRAC_NUM(int offset) {
        return (((offset) >> 27) & 0x0f);
    }

    public static int FRAC_DEN(int offset) {
        return (((offset) >> 23) & 0x0f);
    }

    public static int FRAC_OFFSET(int offset) {
        return ((offset) & 0x007fffff);
    }

    
    public static int STEP4(int offset, int START, int STEP){
        switch(offset){
            case 0:
                return START;
            case 1:
                return (START)+1*(STEP);
            case 2:
                return (START)+2*(STEP);
            case 3: 
                return (START)+3*(STEP);
        }
        return 0;
    };
    
    public static int STEP8(int offset, int START, int STEP){
        switch (offset){
            case 0:
            case 1:
            case 2:
            case 3:
                return STEP4(offset, START,STEP);
                
            case 4:
            case 5:
            case 6:
            case 7:
                return STEP4(offset, (START)+4*(STEP),STEP);                
        }
        
        return 0;
    };
/*TODO*///#define STEP16(START,STEP) STEP8(START,STEP),STEP8((START)+8*(STEP),STEP)
/*TODO*///
/*TODO*///
    public static class GfxLayout {

        public GfxLayout() {
        }

        public GfxLayout(int width, int height, int total, int planes, int planeoffset[], int xoffset[], int yoffset[], int charincrement) {
            this.width = width;
            this.height = height;
            this.total = total;
            this.planes = planes;
            this.planeoffset = planeoffset;
            this.xoffset = xoffset;
            this.yoffset = yoffset;
            this.charincrement = charincrement;
        }

        public GfxLayout(GfxLayout c) {
            width = c.width;
            height = c.height;
            total = c.total;
            planes = c.planes;
            planeoffset = Arrays.copyOf(c.planeoffset, c.planeoffset.length);
            xoffset = Arrays.copyOf(c.xoffset, c.xoffset.length);
            yoffset = Arrays.copyOf(c.yoffset, c.yoffset.length);
            charincrement = c.charincrement;
        }

        public /*UNINT16*/ int width, height;/* width and height of chars/sprites */
        public /*UNINT32*/ int total;/* total numer of chars/sprites in the rom */
        public /*UNINT16*/ int planes;/* number of bitplanes */
        public /*UNINT32*/ int planeoffset[];/* start of every bitplane */
        public /*UNINT32*/ int xoffset[];/* coordinates of the bit corresponding to the pixel */
        public /*UNINT32*/ int yoffset[];/* of the given coordinates */
        public /*UNINT16*/ int charincrement;/* distance between two consecutive characters/sprites */
    }

    public static final int GFX_RAW = 0x12345678;

    /* When planeoffset[0] is set to GFX_RAW, the gfx data is left as-is, with no conversion.
   No buffer is allocated for the decoded data, and gfxdata is set to point to the source
   data; therefore, you must not use ROMREGION_DISPOSE.
   xoffset[0] is an optional displacement (*8) from the beginning of the source data, while
   yoffset[0] is the line modulo (*8) and charincrement the char modulo (*8). They are *8
   for consistency with the usual behaviour, but the bottom 3 bits are not used.
   GFX_PACKED is automatically set if planes is <= 4.

   This special mode can be used to save memory in games that require several different
   handlings of the same ROM data (e.g. metro.c can use both 4bpp and 8bpp tiles, and both
   8x8 and 16x16; cps.c has 8x8, 16x16 and 32x32 tiles all fetched from the same ROMs).
   Note, however, that performance will suffer in rotated games, since the gfx data will
   not be prerotated and will rely on GFX_SWAPXY.
     */
    public static class GfxElement {

        public int width, height;
        public /*unsigned */ int total_elements;/* total number of characters/sprites */
        public int color_granularity;/* number of colors for each color code (for example, 4 for 2 bitplanes gfx) */
        public IntArray colortable;/* map color codes to screen pens */ /* if this is 0, the function does a verbatim copy */
        public int total_colors;
        public /*unsigned */ int[] pen_usage;/* an array of total_elements ints. */
        public UBytePtr gfxdata;/* pixel data */
        public int line_modulo;/* amount to add to get to the next line (usually = width) */
        public int char_modulo;/* = line_modulo * height */
        public int flags;
    }

    public static final int GFX_PACKED = 1;/* two 4bpp pixels are packed in one byte of gfxdata */
    public static final int GFX_SWAPXY = 2;/* characters are mirrored along the top-left/bottom-right diagonal */
    public static final int GFX_DONT_FREE_GFXDATA = 4;/* gfxdata was not malloc()ed, so don't free it on exit */

    public static class GfxDecodeInfo {

        public GfxDecodeInfo(int mr, int s, GfxLayout g, int ccs, int tcc) {
            memory_region = mr;
            start = s;
            if (g != null) {
                gfxlayout = new GfxLayout(g);
            } else {
                gfxlayout = null;
            }
            color_codes_start = ccs;
            total_color_codes = tcc;
        }

        public GfxDecodeInfo(int s, GfxLayout g, int ccs, int tcc) {
            start = s;
            if (g != null) {
                gfxlayout = new GfxLayout(g);
            } else {
                gfxlayout = null;
            }
            color_codes_start = ccs;
            total_color_codes = tcc;
        }

        public GfxDecodeInfo(int s) {
            this(s, s, null, 0, 0);
        }

        public int memory_region;/* memory region where the data resides (usually 1)  -1 marks the end of the array */
        public int start;/* beginning of data data to decode (offset in RAM[]) */
        public GfxLayout gfxlayout;
        public int color_codes_start;/* offset in the color lookup table where color codes start */
        public int total_color_codes;/* total number of color codes */
    }

    public static class rectangle {

        public rectangle() {
        }

        public rectangle(int min_x, int max_x, int min_y, int max_y) {
            this.min_x = min_x;
            this.max_x = max_x;
            this.min_y = min_y;
            this.max_y = max_y;
        }

        public rectangle(rectangle rec) {
            min_x = rec.min_x;
            max_x = rec.max_x;
            min_y = rec.min_y;
            max_y = rec.max_y;
        }

        public int min_x, max_x;
        public int min_y, max_y;
    }
    
    public static class _alpha_cache {
            public int[] alphas;
            public int[] alphad;
            public int[][] alpha=new int[0x101][0x100];
    };

/*TODO*///extern struct _alpha_cache alpha_cache;

    public static final int TRANSPARENCY_NONE = 0;/* opaque with remapping */
    public static final int TRANSPARENCY_NONE_RAW = 1;/* opaque with no remapping */
    public static final int TRANSPARENCY_PEN = 2;/* single pen transparency with remapping */
    public static final int TRANSPARENCY_PEN_RAW = 3;/* single pen transparency with no remapping */
    public static final int TRANSPARENCY_PENS = 4;/* multiple pen transparency with remapping */
    public static final int TRANSPARENCY_PENS_RAW = 5;/* multiple pen transparency with no remapping */
    public static final int TRANSPARENCY_COLOR = 6;/* single remapped pen transparency with remapping */
    public static final int TRANSPARENCY_PEN_TABLE = 7;/* special pen remapping modes (see DRAWMODE_xxx below) with remapping */
    public static final int TRANSPARENCY_PEN_TABLE_RAW = 8;/* special pen remapping modes (see DRAWMODE_xxx below) with no remapping */
    public static final int TRANSPARENCY_BLEND = 9;/* blend two bitmaps, shifting the source and ORing to the dest with remapping */
    public static final int TRANSPARENCY_BLEND_RAW = 10;/* blend two bitmaps, shifting the source and ORing to the dest with no remapping */
    public static final int TRANSPARENCY_ALPHAONE = 11;/* single pen transparency, single pen alpha */
    public static final int TRANSPARENCY_ALPHA = 12;/* single pen transparency, other pens alpha */
    public static final int TRANSPARENCY_MODES = 13;/* total number of modes; must be last */

 /* drawing mode case TRANSPARENCY_PEN_TABLE */
    public static final int DRAWMODE_NONE = 0;
    public static final int DRAWMODE_SOURCE = 1;
    public static final int DRAWMODE_SHADOW = 2;

    public static abstract interface plot_pixel_procPtr {

        public abstract void handler(mame_bitmap bitmap, int x, int y, /*UINT32*/ int pen);
    }

    public static abstract interface read_pixel_procPtr {

        public abstract int handler(mame_bitmap bitmap, int x, int y);
    }

    public static abstract interface plot_box_procPtr {

        public abstract void handler(mame_bitmap bitmap, int x, int y, int width, int height, /*UINT32*/ int pen);
    }

    public static abstract interface mark_dirty_procPtr {

        public abstract void handler(int sx, int sy, int ex, int ey);
    }

    /*TODO*////* Alpha blending functions */
/*TODO*///extern int alpha_active;
/*TODO*///void alpha_init(void);
    public static void alpha_set_level(int level) {
            if(level == 0)
                    level = -1;
            alpha_cache.alphas = alpha_cache.alpha[level+1];
            alpha_cache.alphad = alpha_cache.alpha[255-level];
    }

    public static int alpha_blend16( int d, int s )
    {
            int[] alphas = alpha_cache.alphas;
            int[] alphad = alpha_cache.alphad;
            return (alphas[s & 0x1f] | (alphas[(s>>5) & 0x1f] << 5) | (alphas[(s>>10) & 0x1f] << 10))
                    + (alphad[d & 0x1f] | (alphad[(d>>5) & 0x1f] << 5) | (alphad[(d>>10) & 0x1f] << 10));
    }


    public static int alpha_blend32( int d, int s )
    {
            int[] alphas = alpha_cache.alphas;
            int[] alphad = alpha_cache.alphad;
            return (alphas[s & 0xff] | (alphas[(s>>8) & 0xff] << 8) | (alphas[(s>>16) & 0xff] << 16))
                    + (alphad[d & 0xff] | (alphad[(d>>8) & 0xff] << 8) | (alphad[(d>>16) & 0xff] << 16));
    }
}
