/**
 * ported to 0.56
 */
package arcadeflex056;

import static arcadeflex036.libc_old.strlen;
import arcadeflex036.osdepend;
import static arcadeflex036.ticker.TICKS_PER_SEC;
import static arcadeflex056.blit.*;
import static common.libc.cstdio.printf;
import static common.ptr.*;
import static mame056.driverH.*;

import static mame056.mame.Machine;

// refactor
import static arcadeflex036.osdepend.logerror;
import arcadeflex036.software_gfx;
import static arcadeflex036.sound.update_audio;
import static arcadeflex056.dirtyH.MARKDIRTY;
import static common.libc.cstdio.sprintf;
import common.libc.ctime;
import static common.libc.ctime.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import static java.lang.Math.pow;
import mame056.commonH.mame_bitmap;
import static mame056.inptportH.*;
import static mame056.input.*;
import static mame056.mame.Machine;
import static mame056.mame.schedule_full_refresh;
import static mame056.usrintrf.set_ui_visarea;
import static mame056.usrintrf.ui_text;
import static mame056.version.build_version;


public class video {
    /* old variables just to remember */
    
    static final int MEMORY = 10;
    static long[] prev1 = new long[10];
    static int clock_counter;
    static int speed = 100;
    
    public static final int MAX_X_MULTIPLY = 4;
    public static final int MAX_Y_MULTIPLY = 3;
    public static final int FRAMES_TO_SKIP = 20;
    public static long start_time, end_time;
/*TODO*///#include "mamalleg.h"
/*TODO*///#include "driver.h"
/*TODO*///#include "mamedbg.h"
/*TODO*///#include <pc.h>
/*TODO*///#include <sys/farptr.h>
/*TODO*///#include <go32.h>
/*TODO*///#include "TwkUser.c"
/*TODO*///#include <math.h>
/*TODO*///#include "vgafreq.h"
/*TODO*///#include "vidhrdw/vector.h"
/*TODO*///#include "dirty.h"
/*TODO*////*extra functions for 15.75KHz modes */
/*TODO*///#include "gen15khz.h"
/*TODO*///#include "ticker.h"
/*TODO*///
/*TODO*///
/*TODO*////* function to make scanline mode */
/*TODO*///Register *make_scanline_mode(Register *inreg,int entries);
/*TODO*///
/*TODO*////*15.75KHz SVGA driver (req. for 15.75KHz Arcade Monitor Modes)*/
/*TODO*///SVGA15KHZDRIVER *SVGA15KHzdriver;
/*TODO*///
/*TODO*///
/*TODO*////* from blit.c, for VGA triple buffering */
/*TODO*///extern int xpage_size;
/*TODO*///extern int no_xpages;
/*TODO*///void unchain_vga(Register *pReg);
/*TODO*///
    public static int warming_up;
/*TODO*///
/*TODO*////* tweak values for centering tweaked modes */
/*TODO*///int center_x;
/*TODO*///int center_y;
/*TODO*///
/*TODO*///BEGIN_GFX_DRIVER_LIST
/*TODO*///	GFX_DRIVER_VGA
/*TODO*///	GFX_DRIVER_VESA3
/*TODO*///	GFX_DRIVER_VESA2L
/*TODO*///	GFX_DRIVER_VESA2B
/*TODO*///	GFX_DRIVER_VESA1
/*TODO*///END_GFX_DRIVER_LIST
/*TODO*///
/*TODO*///BEGIN_COLOR_DEPTH_LIST
/*TODO*///	COLOR_DEPTH_8
/*TODO*///	COLOR_DEPTH_15
/*TODO*///	COLOR_DEPTH_16
/*TODO*///	COLOR_DEPTH_32
/*TODO*///END_COLOR_DEPTH_LIST
/*TODO*///
/*TODO*///
/*TODO*///dirtygrid grid1;
/*TODO*///char *dirty_new=grid1;
/*TODO*///
/*TODO*///void center_mode(Register *pReg);
/*TODO*///
/*TODO*////* in msdos/sound.c */
/*TODO*///int msdos_update_audio(void);
/*TODO*///
/*TODO*////* in msdos/input.c */
/*TODO*///void poll_joysticks(void);
/*TODO*///
/*TODO*///
/*TODO*////* specialized update_screen functions defined in blit.c */
/*TODO*///
/*TODO*////* dirty mode 1 (VIDEO_SUPPORTS_DIRTY) */
/*TODO*///void blitscreen_dirty1_vga(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_unchained_vga(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_1x_1x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_1x_2x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_1x_2xs_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_1x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_2x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_2xs_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_3x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_3xs_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_1x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_2x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_2xs_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_3x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_3xs_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_2x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_2xs_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_3x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_3xs_8bpp(struct mame_bitmap *bitmap);
/*TODO*///
/*TODO*///void blitscreen_dirty1_vesa_1x_1x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_1x_2x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_1x_2xs_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_1x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_2x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_2xs_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_3x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_3xs_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_1x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_2x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_2xs_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_3x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_3xs_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_2x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_2xs_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_3x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_3xs_16bpp(struct mame_bitmap *bitmap);
/*TODO*///
/*TODO*///void blitscreen_dirty1_vesa_1x_1x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_1x_2x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_1x_2xs_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_1x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_2x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_2xs_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_3x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_3xs_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_1x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_2x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_2xs_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_3x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_3xs_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_2x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_2xs_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_3x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_3xs_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///
/*TODO*///void blitscreen_dirty1_vesa_1x_1x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_1x_2x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_1x_2xs_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_1x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_2x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_2xs_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_3x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_2x_3xs_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_1x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_2x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_2xs_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_3x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_3x_3xs_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_2x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_2xs_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_3x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty1_vesa_4x_3xs_32bpp(struct mame_bitmap *bitmap);
/*TODO*///
/*TODO*///
/*TODO*////* dirty mode 0 (no osd_mark_dirty calls) */
/*TODO*///void blitscreen_dirty0_vga(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_unchained_vga(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_1x_1x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_1x_2x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_1x_2xs_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_1x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_2x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_2xs_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_3x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_3xs_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_1x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_2x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_2xs_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_3x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_3xs_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_2x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_2xs_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_3x_8bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_3xs_8bpp(struct mame_bitmap *bitmap);
/*TODO*///
/*TODO*///void blitscreen_dirty0_vesa_1x_1x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_1x_2x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_1x_2xs_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_1x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_2x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_2xs_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_3x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_3xs_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_1x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_2x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_2xs_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_3x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_3xs_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_2x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_2xs_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_3x_16bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_3xs_16bpp(struct mame_bitmap *bitmap);
/*TODO*///
/*TODO*///void blitscreen_dirty0_vesa_1x_1x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_1x_2x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_1x_2xs_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_1x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_2x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_2xs_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_3x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_3xs_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_1x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_2x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_2xs_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_3x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_3xs_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_2x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_2xs_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_3x_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_3xs_16bpp_palettized(struct mame_bitmap *bitmap);
/*TODO*///
/*TODO*///void blitscreen_dirty0_vesa_1x_1x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_1x_2x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_1x_2xs_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_1x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_2x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_2xs_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_3x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_2x_3xs_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_1x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_2x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_2xs_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_3x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_3x_3xs_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_2x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_2xs_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_3x_32bpp(struct mame_bitmap *bitmap);
/*TODO*///void blitscreen_dirty0_vesa_4x_3xs_32bpp(struct mame_bitmap *bitmap);
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///static void update_screen_dummy(struct mame_bitmap *bitmap);
/*TODO*///void (*update_screen)(struct mame_bitmap *bitmap) = update_screen_dummy;
/*TODO*///void (*update_screen_debugger)(struct mame_bitmap *bitmap) = update_screen_dummy;
/*TODO*///
/*TODO*///#define MAX_X_MULTIPLY 4
/*TODO*///#define MAX_Y_MULTIPLY 3
/*TODO*///
    
    public static class RGB {
        public int r, g, b;
    }
/*TODO*///static void (*updaters8[MAX_X_MULTIPLY][MAX_Y_MULTIPLY][2][2])(struct mame_bitmap *bitmap) =
/*TODO*///{			/* 1 x 1 */
/*TODO*///	{	{	{ blitscreen_dirty0_vesa_1x_1x_8bpp, blitscreen_dirty1_vesa_1x_1x_8bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_1x_1x_8bpp, blitscreen_dirty1_vesa_1x_1x_8bpp }
/*TODO*///		},	/* 1 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_1x_2x_8bpp,  blitscreen_dirty1_vesa_1x_2x_8bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_1x_2xs_8bpp, blitscreen_dirty1_vesa_1x_2xs_8bpp }
/*TODO*///		},	/* 1 x 3 */
/*TODO*///		{	{ update_screen_dummy, update_screen_dummy },
/*TODO*///			{ update_screen_dummy, update_screen_dummy },
/*TODO*///		}
/*TODO*///	},		/* 2 x 1 */
/*TODO*///	{	{	{ blitscreen_dirty0_vesa_2x_1x_8bpp, blitscreen_dirty1_vesa_2x_1x_8bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_2x_1x_8bpp, blitscreen_dirty1_vesa_2x_1x_8bpp }
/*TODO*///		},	/* 2 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_2x_2x_8bpp,  blitscreen_dirty1_vesa_2x_2x_8bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_2x_2xs_8bpp, blitscreen_dirty1_vesa_2x_2xs_8bpp }
/*TODO*///		},	/* 2 x 3 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_2x_3x_8bpp,  blitscreen_dirty1_vesa_2x_3x_8bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_2x_3xs_8bpp, blitscreen_dirty1_vesa_2x_3xs_8bpp }
/*TODO*///		}
/*TODO*///	},		/* 3 x 1 */
/*TODO*///	{	{	{ blitscreen_dirty0_vesa_3x_1x_8bpp,  blitscreen_dirty1_vesa_3x_1x_8bpp },
/*TODO*///			{ update_screen_dummy, update_screen_dummy }
/*TODO*///		},	/* 3 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_3x_2x_8bpp,  blitscreen_dirty1_vesa_3x_2x_8bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_3x_2xs_8bpp, blitscreen_dirty1_vesa_3x_2xs_8bpp }
/*TODO*///		},	/* 3 x 3 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_3x_3x_8bpp,  blitscreen_dirty1_vesa_3x_3x_8bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_3x_3xs_8bpp, blitscreen_dirty1_vesa_3x_3xs_8bpp }
/*TODO*///		}
/*TODO*///	},		/* 4 x 1 */
/*TODO*///	{	{	{ update_screen_dummy, update_screen_dummy },
/*TODO*///			{ update_screen_dummy, update_screen_dummy }
/*TODO*///		},	/* 4 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_4x_2x_8bpp,  blitscreen_dirty1_vesa_4x_2x_8bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_4x_2xs_8bpp, blitscreen_dirty1_vesa_4x_2xs_8bpp }
/*TODO*///		},	/* 4 x 3 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_4x_3x_8bpp,  blitscreen_dirty1_vesa_4x_3x_8bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_4x_3xs_8bpp, blitscreen_dirty1_vesa_4x_3xs_8bpp }
/*TODO*///		}
/*TODO*///	}
/*TODO*///};
/*TODO*///
/*TODO*///static void (*updaters16[MAX_X_MULTIPLY][MAX_Y_MULTIPLY][2][2])(struct mame_bitmap *bitmap) =
/*TODO*///{			/* 1 x 1 */
/*TODO*///	{	{	{ blitscreen_dirty0_vesa_1x_1x_16bpp, blitscreen_dirty1_vesa_1x_1x_16bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_1x_1x_16bpp, blitscreen_dirty1_vesa_1x_1x_16bpp }
/*TODO*///		},	/* 1 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_1x_2x_16bpp,  blitscreen_dirty1_vesa_1x_2x_16bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_1x_2xs_16bpp, blitscreen_dirty1_vesa_1x_2xs_16bpp }
/*TODO*///		},	/* 1 x 3 */
/*TODO*///		{	{ update_screen_dummy, update_screen_dummy },
/*TODO*///			{ update_screen_dummy, update_screen_dummy },
/*TODO*///		}
/*TODO*///	},		/* 2 x 1 */
/*TODO*///	{	{	{ blitscreen_dirty0_vesa_2x_1x_16bpp,  blitscreen_dirty1_vesa_2x_1x_16bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_2x_1x_16bpp,  blitscreen_dirty1_vesa_2x_1x_16bpp }
/*TODO*///		},	/* 2 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_2x_2x_16bpp,  blitscreen_dirty1_vesa_2x_2x_16bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_2x_2xs_16bpp, blitscreen_dirty1_vesa_2x_2xs_16bpp }
/*TODO*///		},	/* 2 x 3 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_2x_3x_16bpp,  blitscreen_dirty1_vesa_2x_3x_16bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_2x_3xs_16bpp, blitscreen_dirty1_vesa_2x_3xs_16bpp }
/*TODO*///		}
/*TODO*///	},		/* 3 x 1 */
/*TODO*///	{	{	{ blitscreen_dirty0_vesa_3x_1x_16bpp, blitscreen_dirty1_vesa_3x_1x_16bpp },
/*TODO*///			{ update_screen_dummy, update_screen_dummy }
/*TODO*///		},	/* 3 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_3x_2x_16bpp, blitscreen_dirty1_vesa_3x_2x_16bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_3x_2xs_16bpp, blitscreen_dirty1_vesa_3x_2xs_16bpp }
/*TODO*///		},	/* 3 x 3 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_3x_3x_16bpp,  blitscreen_dirty1_vesa_3x_3x_16bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_3x_3xs_16bpp, blitscreen_dirty1_vesa_3x_3xs_16bpp }
/*TODO*///		}
/*TODO*///	},		/* 4 x 1 */
/*TODO*///	{	{	{ update_screen_dummy, update_screen_dummy },
/*TODO*///			{ update_screen_dummy, update_screen_dummy }
/*TODO*///		},	/* 4 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_4x_2x_16bpp,  blitscreen_dirty1_vesa_4x_2x_16bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_4x_2xs_16bpp, blitscreen_dirty1_vesa_4x_2xs_16bpp }
/*TODO*///		},	/* 4 x 3 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_4x_3x_16bpp,  blitscreen_dirty1_vesa_4x_3x_16bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_4x_3xs_16bpp, blitscreen_dirty1_vesa_4x_3xs_16bpp }
/*TODO*///		}
/*TODO*///	}
/*TODO*///};
/*TODO*///
/*TODO*///static void (*updaters16_palettized[MAX_X_MULTIPLY][MAX_Y_MULTIPLY][2][2])(struct mame_bitmap *bitmap) =
/*TODO*///{			/* 1 x 1 */
/*TODO*///	{	{	{ blitscreen_dirty0_vesa_1x_1x_16bpp_palettized, blitscreen_dirty1_vesa_1x_1x_16bpp_palettized },
/*TODO*///			{ blitscreen_dirty0_vesa_1x_1x_16bpp_palettized, blitscreen_dirty1_vesa_1x_1x_16bpp_palettized }
/*TODO*///		},	/* 1 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_1x_2x_16bpp_palettized,  blitscreen_dirty1_vesa_1x_2x_16bpp_palettized },
/*TODO*///			{ blitscreen_dirty0_vesa_1x_2xs_16bpp_palettized, blitscreen_dirty1_vesa_1x_2xs_16bpp_palettized }
/*TODO*///		},	/* 1 x 3 */
/*TODO*///		{	{ update_screen_dummy, update_screen_dummy },
/*TODO*///			{ update_screen_dummy, update_screen_dummy },
/*TODO*///		}
/*TODO*///	},		/* 2 x 1 */
/*TODO*///	{	{	{ blitscreen_dirty0_vesa_2x_1x_16bpp_palettized,  blitscreen_dirty1_vesa_2x_1x_16bpp_palettized },
/*TODO*///			{ blitscreen_dirty0_vesa_2x_1x_16bpp_palettized,  blitscreen_dirty1_vesa_2x_1x_16bpp_palettized }
/*TODO*///		},	/* 2 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_2x_2x_16bpp_palettized,  blitscreen_dirty1_vesa_2x_2x_16bpp_palettized },
/*TODO*///			{ blitscreen_dirty0_vesa_2x_2xs_16bpp_palettized, blitscreen_dirty1_vesa_2x_2xs_16bpp_palettized }
/*TODO*///		},	/* 2 x 3 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_2x_3x_16bpp_palettized,  blitscreen_dirty1_vesa_2x_3x_16bpp_palettized },
/*TODO*///			{ blitscreen_dirty0_vesa_2x_3xs_16bpp_palettized, blitscreen_dirty1_vesa_2x_3xs_16bpp_palettized }
/*TODO*///		}
/*TODO*///	},		/* 3 x 1 */
/*TODO*///	{	{	{ blitscreen_dirty0_vesa_3x_1x_16bpp_palettized, blitscreen_dirty1_vesa_3x_1x_16bpp_palettized },
/*TODO*///			{ update_screen_dummy, update_screen_dummy }
/*TODO*///		},	/* 3 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_3x_2x_16bpp_palettized, blitscreen_dirty1_vesa_3x_2x_16bpp_palettized },
/*TODO*///			{ blitscreen_dirty0_vesa_3x_2xs_16bpp_palettized, blitscreen_dirty1_vesa_3x_2xs_16bpp_palettized }
/*TODO*///		},	/* 3 x 3 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_3x_3x_16bpp_palettized, blitscreen_dirty1_vesa_3x_3x_16bpp_palettized },
/*TODO*///			{ blitscreen_dirty0_vesa_3x_3xs_16bpp_palettized, blitscreen_dirty1_vesa_3x_3xs_16bpp_palettized }
/*TODO*///		}
/*TODO*///	},		/* 4 x 1 */
/*TODO*///	{	{	{ update_screen_dummy, update_screen_dummy },
/*TODO*///			{ update_screen_dummy, update_screen_dummy }
/*TODO*///		},	/* 4 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_4x_2x_16bpp_palettized,  blitscreen_dirty1_vesa_4x_2x_16bpp_palettized },
/*TODO*///			{ blitscreen_dirty0_vesa_4x_2xs_16bpp_palettized, blitscreen_dirty1_vesa_4x_2xs_16bpp_palettized }
/*TODO*///		},	/* 4 x 3 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_4x_3x_16bpp_palettized,  blitscreen_dirty1_vesa_4x_3x_16bpp_palettized },
/*TODO*///			{ blitscreen_dirty0_vesa_4x_3xs_16bpp_palettized, blitscreen_dirty1_vesa_4x_3xs_16bpp_palettized }
/*TODO*///		}
/*TODO*///	}
/*TODO*///};
/*TODO*///
/*TODO*///static void (*updaters32[MAX_X_MULTIPLY][MAX_Y_MULTIPLY][2][2])(struct mame_bitmap *bitmap) =
/*TODO*///{			/* 1 x 1 */
/*TODO*///	{	{	{ blitscreen_dirty0_vesa_1x_1x_32bpp, blitscreen_dirty1_vesa_1x_1x_32bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_1x_1x_32bpp, blitscreen_dirty1_vesa_1x_1x_32bpp }
/*TODO*///		},	/* 1 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_1x_2x_32bpp,  blitscreen_dirty1_vesa_1x_2x_32bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_1x_2xs_32bpp, blitscreen_dirty1_vesa_1x_2xs_32bpp }
/*TODO*///		},	/* 1 x 3 */
/*TODO*///		{	{ update_screen_dummy, update_screen_dummy },
/*TODO*///			{ update_screen_dummy, update_screen_dummy },
/*TODO*///		}
/*TODO*///	},		/* 2 x 1 */
/*TODO*///	{	{	{ blitscreen_dirty0_vesa_2x_1x_32bpp,  blitscreen_dirty1_vesa_2x_1x_32bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_2x_1x_32bpp,  blitscreen_dirty1_vesa_2x_1x_32bpp }
/*TODO*///		},	/* 2 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_2x_2x_32bpp,  blitscreen_dirty1_vesa_2x_2x_32bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_2x_2xs_32bpp, blitscreen_dirty1_vesa_2x_2xs_32bpp }
/*TODO*///		},	/* 2 x 3 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_2x_3x_32bpp,  blitscreen_dirty1_vesa_2x_3x_32bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_2x_3xs_32bpp, blitscreen_dirty1_vesa_2x_3xs_32bpp }
/*TODO*///		}
/*TODO*///	},		/* 3 x 1 */
/*TODO*///	{	{	{ blitscreen_dirty0_vesa_3x_1x_32bpp, blitscreen_dirty1_vesa_3x_1x_32bpp },
/*TODO*///			{ update_screen_dummy, update_screen_dummy }
/*TODO*///		},	/* 3 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_3x_2x_32bpp, blitscreen_dirty1_vesa_3x_2x_32bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_3x_2xs_32bpp, blitscreen_dirty1_vesa_3x_2xs_32bpp }
/*TODO*///		},	/* 3 x 3 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_3x_3x_32bpp, blitscreen_dirty1_vesa_3x_3x_32bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_3x_3xs_32bpp, blitscreen_dirty1_vesa_3x_3xs_32bpp }
/*TODO*///		}
/*TODO*///	},		/* 4 x 1 */
/*TODO*///	{	{	{ update_screen_dummy, update_screen_dummy },
/*TODO*///			{ update_screen_dummy, update_screen_dummy }
/*TODO*///		},	/* 4 x 2 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_4x_2x_32bpp,  blitscreen_dirty1_vesa_4x_2x_32bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_4x_2xs_32bpp, blitscreen_dirty1_vesa_4x_2xs_32bpp }
/*TODO*///		},	/* 4 x 3 */
/*TODO*///		{	{ blitscreen_dirty0_vesa_4x_3x_32bpp,  blitscreen_dirty1_vesa_4x_3x_32bpp },
/*TODO*///			{ blitscreen_dirty0_vesa_4x_3xs_32bpp, blitscreen_dirty1_vesa_4x_3xs_32bpp }
/*TODO*///		}
/*TODO*///	}
/*TODO*///};
    public static software_gfx screen; //for our screen creation
    public static int video_depth,video_fps,video_attributes,video_orientation;
/*TODO*///static int rgb_direct;
    public static int screen_colors;
    public static UBytePtr current_palette;
/*TODO*///static const UINT8 *dbg_palette;
    public static /*unsigned int * */ int[] dirtycolor;
    public static int dirtypalette;
    public static int dirty_bright;
    public static int[] bright_lookup = new int[128 * 1024];

    public static int frameskip,autoframeskip;
    public static final int FRAMESKIP_LEVELS = 12;

    public static int update_video_first_time;

/*TODO*////* type of monitor output- */
/*TODO*////* Standard PC, NTSC, PAL or Arcade */
/*TODO*///int monitor_type;
/*TODO*///
/*TODO*///int vgafreq;
/*TODO*///int always_synced;
    public static int video_sync;
    public static int wait_vsync;
/*TODO*///int use_triplebuf;
/*TODO*///int triplebuf_pos,triplebuf_page_width;
    public static int vsync_frame_rate;
    public static int skiplines;
    public static int skipcolumns;
    public static int scanlines;
    public static int stretch;
/*TODO*///int use_mmx;
/*TODO*///int mmxlfb;
/*TODO*///int use_tweaked;
    public static int use_vesa;
    public static int use_dirty;
    public static float osd_gamma_correction = 1.0f;
    public static int brightness;
    public static float brightness_paused_adjust;
/*TODO*///char *resolution;
/*TODO*///char *mode_desc;
/*TODO*///int gfx_mode;
    public static int gfx_width;
    public static int gfx_height;
    public static int vis_min_x,vis_max_x,vis_min_y,vis_max_y;
/*TODO*///
/*TODO*///
/*TODO*////*new 'half' flag (req. for 15.75KHz Arcade Monitor Modes)*/
/*TODO*///int half_yres=0;
/*TODO*////* indicates unchained video mode (req. for 15.75KHz Arcade Monitor Modes)*/
/*TODO*///int unchained;
/*TODO*////* flags for lowscanrate modes */
/*TODO*///int scanrate15KHz;

    public static int auto_resolution;
    public static int viswidth;
    public static int visheight;
    public static int skiplinesmax;
    public static int skipcolumnsmax;
    public static int skiplinesmin;
    public static int skipcolumnsmin;
/*TODO*///static int show_debugger,debugger_focus_changed;
/*TODO*///
    public static int vector_game;
/*TODO*///
/*TODO*///static Register *reg = 0;       /* for VGA modes */
/*TODO*///static int reglen = 0;  /* for VGA modes */
/*TODO*///static int videofreq;   /* for VGA modes */

    public static int gfx_xoffset;
    public static int gfx_yoffset;
    public static int gfx_display_lines;
    public static int gfx_display_columns;
    public static int xmultiply, ymultiply;
    public static int throttle = 1;/* toggled by F10 */

    public static int gone_to_gfx_mode;
    public static int frameskip_counter;
    public static int frames_displayed;
/*TODO*///static TICKER start_time,end_time;    /* to calculate fps average on exit */
/*TODO*///#define FRAMES_TO_SKIP 20       /* skip the first few frames from the FPS calculation */
/*TODO*///							/* to avoid counting the copyright and info screens */
/*TODO*///
/*TODO*///unsigned char tw224x288_h, tw224x288_v;
/*TODO*///unsigned char tw240x256_h, tw240x256_v;
/*TODO*///unsigned char tw256x240_h, tw256x240_v;
/*TODO*///unsigned char tw256x256_h, tw256x256_v;
/*TODO*///unsigned char tw256x256_hor_h, tw256x256_hor_v;
/*TODO*///unsigned char tw288x224_h, tw288x224_v;
/*TODO*///unsigned char tw240x320_h, tw240x320_v;
/*TODO*///unsigned char tw320x240_h, tw320x240_v;
/*TODO*///unsigned char tw336x240_h, tw336x240_v;
/*TODO*///unsigned char tw384x224_h, tw384x224_v;
/*TODO*///unsigned char tw384x240_h, tw384x240_v;
/*TODO*///unsigned char tw384x256_h, tw384x256_v;
/*TODO*///
/*TODO*///
/*TODO*///struct vga_tweak { int x, y; Register *reg; int reglen; int syncvgafreq; int unchained; int vertical_mode; };
/*TODO*///struct vga_tweak vga_tweaked[] = {
/*TODO*///	{ 240, 256, scr240x256, sizeof(scr240x256)/sizeof(Register),  1, 0, 1 },
/*TODO*///	{ 256, 240, scr256x240, sizeof(scr256x240)/sizeof(Register),  0, 0, 0 },
/*TODO*///	{ 256, 256, scr256x256, sizeof(scr256x256)/sizeof(Register),  1, 0, 1 },
/*TODO*///	{ 256, 256, scr256x256hor, sizeof(scr256x256hor)/sizeof(Register),  0, 0, 0 },
/*TODO*///	{ 224, 288, scr224x288, sizeof(scr224x288)/sizeof(Register),  1, 0, 1 },
/*TODO*///	{ 288, 224, scr288x224, sizeof(scr288x224)/sizeof(Register),  0, 0, 0 },
/*TODO*///	{ 240, 320, scr240x320, sizeof(scr240x320)/sizeof(Register),  1, 1, 1 },
/*TODO*///	{ 320, 240, scr320x240, sizeof(scr320x240)/sizeof(Register),  0, 1, 0 },
/*TODO*///	{ 336, 240, scr336x240, sizeof(scr336x240)/sizeof(Register),  0, 1, 0 },
/*TODO*///	{ 384, 224, scr384x224, sizeof(scr384x224)/sizeof(Register),  1, 1, 0 },
/*TODO*///	{ 384, 240, scr384x240, sizeof(scr384x240)/sizeof(Register),  1, 1, 0 },
/*TODO*///	{ 384, 256, scr384x256, sizeof(scr384x256)/sizeof(Register),  1, 1, 0 },
/*TODO*///	{ 0, 0 }
/*TODO*///};
/*TODO*///struct mode_adjust  {int x, y; unsigned char *hadjust; unsigned char *vadjust; int vertical_mode; };
/*TODO*///
/*TODO*////* horizontal and vertical total tweak values for above modes */
/*TODO*///struct mode_adjust  pc_adjust[] = {
/*TODO*///	{ 240, 256, &tw240x256_h, &tw240x256_v, 1 },
/*TODO*///	{ 256, 240, &tw256x240_h, &tw256x240_v, 0 },
/*TODO*///	{ 256, 256, &tw256x256_hor_h, &tw256x256_hor_v, 0 },
/*TODO*///	{ 256, 256, &tw256x256_h, &tw256x256_v, 1 },
/*TODO*///	{ 224, 288, &tw224x288_h, &tw224x288_v, 1 },
/*TODO*///	{ 288, 224, &tw288x224_h, &tw288x224_v, 0 },
/*TODO*///	{ 240, 320, &tw240x320_h, &tw240x320_v, 1 },
/*TODO*///	{ 320, 240, &tw320x240_h, &tw320x240_v, 0 },
/*TODO*///	{ 336, 240, &tw336x240_h, &tw336x240_v, 0 },
/*TODO*///	{ 384, 224, &tw384x224_h, &tw384x224_v, 0 },
/*TODO*///	{ 384, 240, &tw384x240_h, &tw384x240_v, 0 },
/*TODO*///	{ 384, 256, &tw384x256_h, &tw384x256_v, 0 },
/*TODO*///	{ 0, 0 }
/*TODO*///};
/*TODO*///
/*TODO*////* Tweak values for arcade/ntsc/pal modes */
/*TODO*///unsigned char tw224x288arc_h, tw224x288arc_v, tw288x224arc_h, tw288x224arc_v;
/*TODO*///unsigned char tw256x240arc_h, tw256x240arc_v, tw256x256arc_h, tw256x256arc_v;
/*TODO*///unsigned char tw320x240arc_h, tw320x240arc_v, tw320x256arc_h, tw320x256arc_v;
/*TODO*///unsigned char tw352x240arc_h, tw352x240arc_v, tw352x256arc_h, tw352x256arc_v;
/*TODO*///unsigned char tw368x224arc_h, tw368x224arc_v;
/*TODO*///unsigned char tw368x240arc_h, tw368x240arc_v, tw368x256arc_h, tw368x256arc_v;
/*TODO*///unsigned char tw512x224arc_h, tw512x224arc_v, tw512x256arc_h, tw512x256arc_v;
/*TODO*///unsigned char tw512x448arc_h, tw512x448arc_v, tw512x512arc_h, tw512x512arc_v;
/*TODO*///unsigned char tw640x480arc_h, tw640x480arc_v;
/*TODO*///
/*TODO*////* 15.75KHz Modes */
/*TODO*///struct vga_15KHz_tweak { int x, y; Register *reg; int reglen;
/*TODO*///			  int syncvgafreq; int vesa; int ntsc;
/*TODO*///			  int half_yres; int matchx; };
/*TODO*///struct vga_15KHz_tweak arcade_tweaked[] = {
/*TODO*///	{ 224, 288, scr224x288_15KHz, sizeof(scr224x288_15KHz)/sizeof(Register), 0, 0, 0, 0, 224 },
/*TODO*///	{ 256, 240, scr256x240_15KHz, sizeof(scr256x240_15KHz)/sizeof(Register), 0, 0, 1, 0, 256 },
/*TODO*///	{ 256, 256, scr256x256_15KHz, sizeof(scr256x256_15KHz)/sizeof(Register), 0, 0, 0, 0, 256 },
/*TODO*///	{ 288, 224, scr288x224_15KHz, sizeof(scr288x224_15KHz)/sizeof(Register), 0, 0, 1, 0, 288 },
/*TODO*///	{ 320, 240, scr320x240_15KHz, sizeof(scr320x240_15KHz)/sizeof(Register), 1, 0, 1, 0, 320 },
/*TODO*///	{ 320, 256, scr320x256_15KHz, sizeof(scr320x256_15KHz)/sizeof(Register), 1, 0, 0, 0, 320 },
/*TODO*///	{ 352, 240, scr352x240_15KHz, sizeof(scr352x240_15KHz)/sizeof(Register), 1, 0, 1, 0, 352 },
/*TODO*///	{ 352, 256, scr352x256_15KHz, sizeof(scr352x256_15KHz)/sizeof(Register), 1, 0, 0, 0, 352 },
/*TODO*////* force 384 games to match to 368 modes - the standard VGA clock speeds mean we can't go as wide as 384 */
/*TODO*///	{ 368, 224, scr368x224_15KHz, sizeof(scr368x224_15KHz)/sizeof(Register), 1, 0, 1, 0, 384 },
/*TODO*////* all VGA modes from now on are too big for triple buffering */
/*TODO*///	{ 368, 240, scr368x240_15KHz, sizeof(scr368x240_15KHz)/sizeof(Register), 1, 0, 1, 0, 384 },
/*TODO*///	{ 368, 256, scr368x256_15KHz, sizeof(scr368x256_15KHz)/sizeof(Register), 1, 0, 0, 0, 384 },
/*TODO*////* double monitor modes */
/*TODO*///	{ 512, 224, scr512x224_15KHz, sizeof(scr512x224_15KHz)/sizeof(Register), 0, 0, 1, 0, 512 },
/*TODO*///	{ 512, 256, scr512x256_15KHz, sizeof(scr512x256_15KHz)/sizeof(Register), 0, 0, 0, 0, 512 },
/*TODO*////* SVGA Mode (VGA register array not used) */
/*TODO*///	{ 640, 480, NULL            , 0                                        , 0, 1, 1, 0, 640 },
/*TODO*////* 'half y' VGA modes, used to fake hires if 'tweaked' is on */
/*TODO*///	{ 512, 448, scr512x224_15KHz, sizeof(scr512x224_15KHz)/sizeof(Register), 0, 0, 1, 1, 512 },
/*TODO*///	{ 512, 512, scr512x256_15KHz, sizeof(scr512x256_15KHz)/sizeof(Register), 0, 0, 0, 1, 512 },
/*TODO*///	{ 0, 0 }
/*TODO*///};
/*TODO*///
/*TODO*////* horizontal and vertical total tweak values for above modes */
/*TODO*///struct mode_adjust  arcade_adjust[] = {
/*TODO*///	{ 224, 288, &tw224x288arc_h, &tw224x288arc_v, 1 },
/*TODO*///	{ 256, 240, &tw256x240arc_h, &tw256x240arc_v, 0 },
/*TODO*///	{ 256, 256, &tw256x256arc_h, &tw256x256arc_v, 0 },
/*TODO*///	{ 288, 224, &tw288x224arc_h, &tw288x224arc_v, 0 },
/*TODO*///	{ 320, 240, &tw320x240arc_h, &tw320x240arc_v, 0 },
/*TODO*///	{ 352, 240, &tw352x240arc_h, &tw352x240arc_v, 0 },
/*TODO*///	{ 352, 256, &tw352x256arc_h, &tw352x256arc_v, 0 },
/*TODO*///	{ 368, 224, &tw368x224arc_h, &tw368x224arc_v, 0 },
/*TODO*///	{ 368, 240, &tw368x240arc_h, &tw368x240arc_v, 0 },
/*TODO*///	{ 368, 256, &tw368x256arc_h, &tw368x256arc_v, 0 },
/*TODO*///	{ 512, 224, &tw512x224arc_h, &tw512x224arc_v, 0 },
/*TODO*///	{ 512, 256, &tw512x256arc_h, &tw512x256arc_v, 0 },
/*TODO*///	{ 512, 448, &tw512x224arc_h, &tw512x224arc_v, 0 },
/*TODO*///	{ 512, 512, &tw512x256arc_h, &tw512x256arc_v, 0 },
/*TODO*///	{ 0, 0 }
/*TODO*///};
/*TODO*///
/*TODO*///
/*TODO*///static void mark_full_screen_dirty(void)
/*TODO*///{
/*TODO*///	osd_mark_dirty(0,0,65535,65535);
/*TODO*///}



    public static void osd_mark_dirty(int _x1,int _y1,int _x2,int _y2)
    {
            if (use_dirty != 0)
            {
                    int x, y;

    //        logerror("mark_dirty %3d,%3d - %3d,%3d\n", _x1,_y1, _x2,_y2);

                    _x1 -= skipcolumns;
                    _x2 -= skipcolumns;
                    _y1 -= skiplines;
                    _y2 -= skiplines;

            if (_y1 >= gfx_display_lines || _y2 < 0 || _x1 > gfx_display_columns || _x2 < 0) return;
                    if (_y1 < 0) _y1 = 0;
                    if (_y2 >= gfx_display_lines) _y2 = gfx_display_lines - 1;
                    if (_x1 < 0) _x1 = 0;
                    if (_x2 >= gfx_display_columns) _x2 = gfx_display_columns - 1;

                    for (y = _y1; y <= _y2 + 15; y += 16)
                            for (x = _x1; x <= _x2 + 15; x += 16)
                                    MARKDIRTY(x,y);
            }
    }

/*TODO*///static void init_dirty(char dirty)
/*TODO*///{
/*TODO*///	memset(dirty_new, dirty, MAX_GFX_WIDTH/16 * MAX_GFX_HEIGHT/16);
/*TODO*///}



    /*
     * This function tries to find the best display mode.
     */
    public static void select_display_mode(int width,int height,int depth,int attributes,int orientation)
    {
            int i;
    /*TODO*///	auto_resolution = 0;
    /*TODO*///	/* assume unchained video mode  */
    /*TODO*///	unchained = 0;
    /*TODO*///	/* see if it's a low scanrate mode */
    /*TODO*///	switch (monitor_type)
    /*TODO*///	{
    /*TODO*///		case MONITOR_TYPE_NTSC:
    /*TODO*///		case MONITOR_TYPE_PAL:
    /*TODO*///		case MONITOR_TYPE_ARCADE:
    /*TODO*///			scanrate15KHz = 1;
    /*TODO*///			break;
    /*TODO*///		default:
    /*TODO*///			scanrate15KHz = 0;
    /*TODO*///	}

            /* initialise quadring table [useful for *all* doubling modes */
            for (i = 0; i < screen_colors; i++)
            {
                    doublepixel[i] = i | (i<<8);
                    quadpixel[i] = i | (i<<8) | (i << 16) | (i << 24);
            }

            use_vesa = -1;

    /*TODO*///	/* 16 bit color is supported only by VESA modes */
    /*TODO*///	if (depth == 16 || depth == 32)
    /*TODO*///	{
    /*TODO*///		logerror("Game needs %d-bit colors. Using VESA\n",depth);
    /*TODO*///		use_tweaked = 0;
    /*TODO*///		/* only one 15.75KHz VESA mode, so force that */
    /*TODO*///		if (scanrate15KHz == 1)
    /*TODO*///		{
    /*TODO*///			gfx_width = 640;
    /*TODO*///			gfx_height = 480;
    /*TODO*///		}
    /*TODO*///	}
    /*TODO*///
    /*TODO*///
    /*TODO*///  /* Check for special 15.75KHz mode (req. for 15.75KHz Arcade Modes) */
    /*TODO*///	if (scanrate15KHz == 1)
    /*TODO*///	{
    /*TODO*///		switch (monitor_type)
    /*TODO*///		{
    /*TODO*///			case MONITOR_TYPE_NTSC:
    /*TODO*///				logerror("Using special NTSC video mode.\n");
    /*TODO*///				break;
    /*TODO*///			case MONITOR_TYPE_PAL:
    /*TODO*///				logerror("Using special PAL video mode.\n");
    /*TODO*///				break;
    /*TODO*///			case MONITOR_TYPE_ARCADE:
    /*TODO*///				logerror("Using special arcade monitor mode.\n");
    /*TODO*///				break;
    /*TODO*///		}
    /*TODO*///		scanlines = 0;
    /*TODO*///		/* if no width/height specified, pick one from our tweaked list */
    /*TODO*///		if (!gfx_width && !gfx_height)
    /*TODO*///		{
    /*TODO*///			for (i=0; arcade_tweaked[i].x != 0; i++)
    /*TODO*///			{
    /*TODO*///				/* find height/width fit */
    /*TODO*///				/* only allow VESA modes if vesa explicitly selected */
    /*TODO*///				/* only allow PAL / NTSC modes if explicitly selected */
    /*TODO*///				/* arcade modes cover 50-60Hz) */
    /*TODO*///				if ((use_tweaked == 0 ||!arcade_tweaked[i].vesa) &&
    /*TODO*///					(monitor_type == MONITOR_TYPE_ARCADE || /* handles all 15.75KHz modes */
    /*TODO*///					(arcade_tweaked[i].ntsc && monitor_type == MONITOR_TYPE_NTSC) ||  /* NTSC only */
    /*TODO*///					(!arcade_tweaked[i].ntsc && monitor_type == MONITOR_TYPE_PAL)) &&  /* PAL ONLY */
    /*TODO*///					width  <= arcade_tweaked[i].matchx &&
    /*TODO*///					height <= arcade_tweaked[i].y)
    /*TODO*///
    /*TODO*///				{
    /*TODO*///					gfx_width  = arcade_tweaked[i].x;
    /*TODO*///					gfx_height = arcade_tweaked[i].y;
    /*TODO*///					break;
    /*TODO*///				}
    /*TODO*///			}
    /*TODO*///			/* if it's a vector, and there's isn't an SVGA support we want to avoid the half modes */
    /*TODO*///			/* - so force default res. */
    /*TODO*///			if (vector_game && (use_vesa == 0 || monitor_type == MONITOR_TYPE_PAL))
    /*TODO*///				gfx_width = 0;
    /*TODO*///
    /*TODO*///			/* we didn't find a tweaked 15.75KHz mode to fit */
    /*TODO*///			if (gfx_width == 0)
    /*TODO*///			{
    /*TODO*///				/* pick a default resolution for the monitor type */
    /*TODO*///				/* something with the right refresh rate + an aspect ratio which can handle vectors */
    /*TODO*///				switch (monitor_type)
    /*TODO*///				{
    /*TODO*///					case MONITOR_TYPE_NTSC:
    /*TODO*///					case MONITOR_TYPE_ARCADE:
    /*TODO*///						gfx_width = 320; gfx_height = 240;
    /*TODO*///						break;
    /*TODO*///					case MONITOR_TYPE_PAL:
    /*TODO*///						gfx_width = 320; gfx_height = 256;
    /*TODO*///						break;
    /*TODO*///				}
    /*TODO*///
    /*TODO*///				use_vesa = 0;
    /*TODO*///			}
    /*TODO*///			else
    /*TODO*///				use_vesa = arcade_tweaked[i].vesa;
    /*TODO*///		}
    /*TODO*///
    /*TODO*///	}
    /*TODO*///
    /*TODO*///
    /*TODO*///	/* If using tweaked modes, check if there exists one to fit
    /*TODO*///	   the screen in, otherwise use VESA */
    /*TODO*///	if (use_tweaked && !gfx_width && !gfx_height)
    /*TODO*///	{
    /*TODO*///		for (i=0; vga_tweaked[i].x != 0; i++)
    /*TODO*///		{
    /*TODO*///			if (width <= vga_tweaked[i].x &&
    /*TODO*///				height <= vga_tweaked[i].y)
    /*TODO*///			{
    /*TODO*///				/*check for 57Hz modes which would fit into a 60Hz mode*/
    /*TODO*///				if (gfx_width <= 256 && gfx_height <= 256 &&
    /*TODO*///					video_sync && video_fps == 57)
    /*TODO*///				{
    /*TODO*///					gfx_width = 256;
    /*TODO*///					gfx_height = 256;
    /*TODO*///					use_vesa = 0;
    /*TODO*///					break;
    /*TODO*///				}
    /*TODO*///
    /*TODO*///				/* check for correct horizontal/vertical modes */
    /*TODO*///				if((!vga_tweaked[i].vertical_mode && !(orientation & ORIENTATION_SWAP_XY)) ||
    /*TODO*///					(vga_tweaked[i].vertical_mode && (orientation & ORIENTATION_SWAP_XY)))
    /*TODO*///				{
    /*TODO*///					gfx_width  = vga_tweaked[i].x;
    /*TODO*///					gfx_height = vga_tweaked[i].y;
    /*TODO*///					use_vesa = 0;
    /*TODO*///					/* leave the loop on match */
    /*TODO*///
    /*TODO*///if (gfx_width == 320 && gfx_height == 240 && scanlines == 0)
    /*TODO*///{
    /*TODO*///	use_vesa = 1;
    /*TODO*///	gfx_width = 0;
    /*TODO*///	gfx_height = 0;
    /*TODO*///}
    /*TODO*///					break;
    /*TODO*///				}
    /*TODO*///			}
    /*TODO*///		}
    /*TODO*///		/* If we didn't find a tweaked VGA mode, use VESA */
    /*TODO*///		if (gfx_width == 0)
    /*TODO*///		{
    /*TODO*///			logerror("Did not find a tweaked VGA mode. Using VESA.\n");
    /*TODO*///			use_vesa = 1;
    /*TODO*///		}
    /*TODO*///	}
    /*TODO*///
    /*TODO*///
            /* If no VESA resolution has been given, we choose a sensible one. */
            /* 640x480, 800x600 and 1024x768 are common to all VESA drivers. */
            if ((gfx_width==0) && (gfx_height==0))
            {
                    auto_resolution = 1;
                    use_vesa = 1;

                    /* vector games use 640x480 as default */
                    if (vector_game != 0)
                    {
                            gfx_width = 640;
                            gfx_height = 480;
                    }
                    else
                    {
                            int xm,ym;

                            xm = ym = 1;

                            if ((attributes & VIDEO_PIXEL_ASPECT_RATIO_MASK)
                                            == VIDEO_PIXEL_ASPECT_RATIO_1_2)
                            {
                                    if ((orientation & ORIENTATION_SWAP_XY) != 0)
                                            xm*=2;
                                    else ym*=2;
                            }
                            else if ((attributes & VIDEO_PIXEL_ASPECT_RATIO_MASK)
                                            == VIDEO_PIXEL_ASPECT_RATIO_2_1)
                            {
                                    if ((orientation & ORIENTATION_SWAP_XY) != 0)
                                            ym*=2;
                                    else xm*=2;
                            }

                            if ((scanlines != 0) && (stretch != 0))
                            {
                                    if (ym == 1)
                                    {
                                            xm *= 2;
                                            ym *= 2;
                                    }

                                    /* see if pixel doubling can be applied at 640x480 */
                                    if (ym*height <= 480 && xm*width <= 640 &&
                                                    (xm > 1 || (ym+1)*height > 768 || (xm+1)*width > 1024))
                                    {
                                            gfx_width = 640;
                                            gfx_height = 480;
                                    }
                                    /* see if pixel doubling can be applied at 800x600 */
                                    else if (ym*height <= 600 && xm*width <= 800 &&
                                                    (xm > 1 || (ym+1)*height > 768 || (xm+1)*width > 1024))
                                    {
                                            gfx_width = 800;
                                            gfx_height = 600;
                                    }
                                    /* don't use 1024x768 right away. If 512x384 is available, it */
                                    /* will provide hardware scanlines. */

                                    if (ym > 1 && xm > 1)
                                    {
                                            xm /= 2;
                                            ym /= 2;
                                    }
                            }

                            if ((gfx_width==0) && (gfx_height==0))
                            {
                                    if (ym*height <= 240 && xm*width <= 320)
                                    {
                                            gfx_width = 320;
                                            gfx_height = 240;
                                    }
                                    else if (ym*height <= 300 && xm*width <= 400)
                                    {
                                            gfx_width = 400;
                                            gfx_height = 300;
                                    }
                                    else if (ym*height <= 384 && xm*width <= 512)
                                    {
                                            gfx_width = 512;
                                            gfx_height = 384;
                                    }
                                    else if (ym*height <= 480 && xm*width <= 640 &&
                                                    ((stretch==0) || (ym+1)*height > 768 || (xm+1)*width > 1024))
                                    {
                                            gfx_width = 640;
                                            gfx_height = 480;
                                    }
                                    else if (ym*height <= 600 && xm*width <= 800 &&
                                                    ((stretch==0) || (ym+1)*height > 768 || (xm+1)*width > 1024))
                                    {
                                            gfx_width = 800;
                                            gfx_height = 600;
                                    }
                                    else
                                    {
                                            gfx_width = 1024;
                                            gfx_height = 768;
                                    }
                            }
                    }
            }            
    }



    /* center image inside the display based on the visual area */
    public static void internal_set_visible_area(int min_x,int max_x,int min_y,int max_y,int debugger)
    {
            int act_width;

    logerror("set visible area %d-%d %d-%d\n",min_x,max_x,min_y,max_y);

    /*TODO*////* if it's a SVGA arcade monitor mode, get the memory width of the mode */
    /*TODO*////* this could be double the width of the actual mode set */
    /*TODO*///	if (scanrate15KHz && SVGA15KHzdriver && use_vesa == 1)
    /*TODO*///		act_width = SVGA15KHzdriver->getlogicalwidth (gfx_width);
    /*TODO*///	else
                    act_width = gfx_width;


            viswidth  = max_x - min_x + 1;
            visheight = max_y - min_y + 1;


    /*TODO*///	if (debugger)
    /*TODO*///	{
    /*TODO*///		xmultiply = ymultiply = 1;
    /*TODO*///	}
    /*TODO*///	else
    /*TODO*///	{
                    /* setup xmultiply to handle SVGA driver's (possible) double width */
                    xmultiply = act_width / gfx_width;
                    ymultiply = 1;

                    if ((use_vesa!=0) && (vector_game==0))
                    {
                            if (stretch != 0)
                            {
                                    if (((video_orientation & ORIENTATION_SWAP_XY)==0) &&
                                                    ((video_attributes & VIDEO_DUAL_MONITOR)==0))
                                    {
                                            /* horizontal, non dual monitor games may be stretched at will */
                                            while ((xmultiply+1) * viswidth <= act_width)
                                                    xmultiply++;
                                            while ((ymultiply+1) * visheight <= gfx_height)
                                                    ymultiply++;
                                    }
                                    else
                                    {
                                            int tw,th;

                                            tw = act_width;
                                            th = gfx_height;

                                            if ((video_attributes & VIDEO_PIXEL_ASPECT_RATIO_MASK)
                                                            == VIDEO_PIXEL_ASPECT_RATIO_1_2)
                                            {
                                                    if ((video_orientation & ORIENTATION_SWAP_XY) !=0 )
                                                            tw /= 2;
                                                    else th /= 2;
                                            }
                                            else if ((video_attributes & VIDEO_PIXEL_ASPECT_RATIO_MASK)
                                                            == VIDEO_PIXEL_ASPECT_RATIO_2_1)
                                            {
                                                    if ((video_orientation & ORIENTATION_SWAP_XY) != 0)
                                                            th /= 2;
                                                    else tw /= 2;
                                            }

                                            /* Hack for 320x480 and 400x600 "vmame" video modes */
                                            if ((gfx_width == 320 && gfx_height == 480) ||
                                                            (gfx_width == 400 && gfx_height == 600))
                                                    th /= 2;

                                            /* maintain aspect ratio for other games */
                                            while ((xmultiply+1) * viswidth <= tw &&
                                                            (ymultiply+1) * visheight <= th)
                                            {
                                                    xmultiply++;
                                                    ymultiply++;
                                            }

                                            if ((video_attributes & VIDEO_PIXEL_ASPECT_RATIO_MASK)
                                                            == VIDEO_PIXEL_ASPECT_RATIO_1_2)
                                            {
                                                    if ((video_orientation & ORIENTATION_SWAP_XY) != 0)
                                                            xmultiply *= 2;
                                                    else ymultiply *= 2;
                                            }
                                            else if ((video_attributes & VIDEO_PIXEL_ASPECT_RATIO_MASK)
                                                            == VIDEO_PIXEL_ASPECT_RATIO_2_1)
                                            {
                                                    if ((video_orientation & ORIENTATION_SWAP_XY) != 0)
                                                            ymultiply *= 2;
                                                    else xmultiply *= 2;
                                            }

                                            /* Hack for 320x480 and 400x600 "vmame" video modes */
                                            if ((gfx_width == 320 && gfx_height == 480) ||
                                                            (gfx_width == 400 && gfx_height == 600))
                                                    ymultiply *= 2;
                                    }
                            }
                            else
                            {
                                    if ((video_attributes & VIDEO_PIXEL_ASPECT_RATIO_MASK)
                                                    == VIDEO_PIXEL_ASPECT_RATIO_1_2)
                                    {
                                            if ((video_orientation & ORIENTATION_SWAP_XY) != 0)
                                                    xmultiply *= 2;
                                            else ymultiply *= 2;
                                    }
                                    else if ((video_attributes & VIDEO_PIXEL_ASPECT_RATIO_MASK)
                                                    == VIDEO_PIXEL_ASPECT_RATIO_2_1)
                                    {
                                            if ((video_orientation & ORIENTATION_SWAP_XY) != 0)
                                                    ymultiply *= 2;
                                            else xmultiply *= 2;
                                    }

                                    /* Hack for 320x480 and 400x600 "vmame" video modes */
                                    if ((gfx_width == 320 && gfx_height == 480) ||
                                                    (gfx_width == 400 && gfx_height == 600))
                                            ymultiply *= 2;
                            }
                    }

                    if (xmultiply > MAX_X_MULTIPLY) xmultiply = MAX_X_MULTIPLY;
                    if (ymultiply > MAX_Y_MULTIPLY) ymultiply = MAX_Y_MULTIPLY;
    /*TODO*///	}

            gfx_display_lines = visheight;
            gfx_display_columns = viswidth;

            gfx_xoffset = (act_width - viswidth * xmultiply) / 2;
            if (gfx_display_columns > act_width / xmultiply)
                    gfx_display_columns = act_width / xmultiply;

            gfx_yoffset = (gfx_height - visheight * ymultiply) / 2;
                    if (gfx_display_lines > gfx_height / ymultiply)
                            gfx_display_lines = gfx_height / ymultiply;


            skiplinesmin = min_y;
            skiplinesmax = visheight - gfx_display_lines + min_y;
            skipcolumnsmin = min_x;
            skipcolumnsmax = viswidth - gfx_display_columns + min_x;

            /* Align on a quadword !*/
            gfx_xoffset &= ~7;

            /* the skipcolumns from mame.cfg/cmdline is relative to the visible area */
            skipcolumns = min_x + skipcolumns;
            skiplines   = min_y + skiplines;

            /* Just in case the visual area doesn't fit */
            if (gfx_xoffset < 0)
            {
                    skipcolumns -= gfx_xoffset;
                    gfx_xoffset = 0;
            }
            if (gfx_yoffset < 0)
            {
                    skiplines   -= gfx_yoffset;
                    gfx_yoffset = 0;
            }

            /* Failsafe against silly parameters */
            if (skiplines < skiplinesmin)
                    skiplines = skiplinesmin;
            if (skipcolumns < skipcolumnsmin)
                    skipcolumns = skipcolumnsmin;
            if (skiplines > skiplinesmax)
                    skiplines = skiplinesmax;
            if (skipcolumns > skipcolumnsmax)
                    skipcolumns = skipcolumnsmax;

            logerror("gfx_width = %d gfx_height = %d\n"+
                                    "gfx_xoffset = %d gfx_yoffset = %d\n"+
                                    "xmin %d ymin %d xmax %d ymax %d\n"+
                                    "skiplines %d skipcolumns %d\n"+
                                    "gfx_display_lines %d gfx_display_columns %d\n"+
                                    "xmultiply %d ymultiply %d\n",
                                    gfx_width,gfx_height,
                                    gfx_xoffset,gfx_yoffset,
                                    min_x, min_y, max_x, max_y, skiplines, skipcolumns,gfx_display_lines,gfx_display_columns,xmultiply,ymultiply);

            set_ui_visarea(skipcolumns, skiplines, skipcolumns+gfx_display_columns-1, skiplines+gfx_display_lines-1);

            /* round to a multiple of 4 to avoid missing pixels on the right side */
            gfx_display_columns  = (gfx_display_columns + 3) & ~3;

            /*HACKISH*/
            tempCreation();
    }


    public static void osd_set_visible_area(int min_x,int max_x,int min_y,int max_y)
    {
            vis_min_x = min_x;
            vis_max_x = max_x;
            vis_min_y = min_y;
            vis_max_y = max_y;
            internal_set_visible_area(min_x,max_x,min_y,max_y,0);
    }


    /* set the actual display screen but don't allocate the screen bitmap */
    static int osd_set_display(int width,int height,int depth,int attributes,int orientation)
    {
/*TODO*///	struct mode_adjust *adjust_array;

            int     i;
            /* moved 'found' to here (req. for 15.75KHz Arcade Monitor Modes) */
            int     found;

            if ((gfx_height==0) || (gfx_width==0))
            {
                    printf("Please specify height AND width (e.g. -640x480)\n");
                    return 0;
            }


            /* Mark the dirty buffers as dirty */
            /*TODO*///if (use_dirty != 0) init_dirty(1);

            if (dirtycolor != null)
            {
                    for (i = 0;i < screen_colors;i++)
                            dirtycolor[i] = 1;
                    dirtypalette = 1;
            }
/*TODO*///	/* handle special 15.75KHz modes, these now include SVGA modes */
            found = 0;
/*TODO*///	/*move video freq set to here, as we need to set it explicitly for the 15.75KHz modes */
/*TODO*///	videofreq = vgafreq;
/*TODO*///
/*TODO*///	if (scanrate15KHz == 1)
/*TODO*///	{
/*TODO*///		/* pick the mode from our 15.75KHz tweaked modes */
/*TODO*///		for (i=0; ((arcade_tweaked[i].x != 0) && !found); i++)
/*TODO*///		{
/*TODO*///			if (gfx_width  == arcade_tweaked[i].x &&
/*TODO*///				gfx_height == arcade_tweaked[i].y)
/*TODO*///			{
/*TODO*///				/* check for SVGA mode with no vesa flag */
/*TODO*///				if (arcade_tweaked[i].vesa&& use_vesa == 0)
/*TODO*///				{
/*TODO*///					printf ("\n %dx%d SVGA 15.75KHz mode only available if tweaked flag is set to 0\n", gfx_width, gfx_height);
/*TODO*///					return 0;
/*TODO*///				}
/*TODO*///				/* check for a NTSC or PAL mode with no arcade flag */
/*TODO*///				if (monitor_type != MONITOR_TYPE_ARCADE)
/*TODO*///				{
/*TODO*///					if (arcade_tweaked[i].ntsc && monitor_type != MONITOR_TYPE_NTSC)
/*TODO*///					{
/*TODO*///						printf("\n %dx%d 15.75KHz mode only available if -monitor set to 'arcade' or 'ntsc' \n", gfx_width, gfx_height);
/*TODO*///						return 0;
/*TODO*///					}
/*TODO*///					if (!arcade_tweaked[i].ntsc && monitor_type != MONITOR_TYPE_PAL)
/*TODO*///					{
/*TODO*///						printf("\n %dx%d 15.75KHz mode only available if -monitor set to 'arcade' or 'pal' \n", gfx_width, gfx_height);
/*TODO*///						return 0;
/*TODO*///					}
/*TODO*///
/*TODO*///				}
/*TODO*///
/*TODO*///				reg = arcade_tweaked[i].reg;
/*TODO*///				reglen = arcade_tweaked[i].reglen;
/*TODO*///				use_vesa = arcade_tweaked[i].vesa;
/*TODO*///				half_yres = arcade_tweaked[i].half_yres;
/*TODO*///				/* all 15.75KHz VGA modes are unchained */
/*TODO*///				unchained = !use_vesa;
/*TODO*///
/*TODO*///				logerror("15.75KHz mode (%dx%d) vesa:%d half:%d unchained:%d\n",
/*TODO*///										gfx_width, gfx_height, use_vesa, half_yres, unchained);
/*TODO*///				/* always use the freq from the structure */
/*TODO*///				videofreq = arcade_tweaked[i].syncvgafreq;
/*TODO*///				found = 1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		/* explicitly asked for an 15.75KHz mode which doesn't exist , so inform and exit */
/*TODO*///		if (!found)
/*TODO*///		{
/*TODO*///			printf ("\nNo %dx%d 15.75KHz mode available.\n", gfx_width, gfx_height);
/*TODO*///			return 0;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (use_vesa != 1 && use_tweaked == 1)
/*TODO*///	{
/*TODO*///
/*TODO*///		/* setup tweaked modes */
/*TODO*///		/* handle 57Hz games which fit into 60Hz mode */
/*TODO*///		if (!found && gfx_width <= 256 && gfx_height <= 256 &&
/*TODO*///				video_sync && video_fps == 57)
/*TODO*///		{
/*TODO*///			found = 1;
/*TODO*///			if (!(orientation & ORIENTATION_SWAP_XY))
/*TODO*///			{
/*TODO*///				reg = scr256x256hor;
/*TODO*///				reglen = sizeof(scr256x256hor)/sizeof(Register);
/*TODO*///				videofreq = 0;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				reg = scr256x256;
/*TODO*///				reglen = sizeof(scr256x256)/sizeof(Register);
/*TODO*///				videofreq = 1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		/* find the matching tweaked mode */
/*TODO*///		for (i=0; ((vga_tweaked[i].x != 0) && !found); i++)
/*TODO*///		{
/*TODO*///			if (gfx_width  == vga_tweaked[i].x &&
/*TODO*///				gfx_height == vga_tweaked[i].y)
/*TODO*///			{
/*TODO*///				/* check for correct horizontal/vertical modes */
/*TODO*///
/*TODO*///				if((!vga_tweaked[i].vertical_mode && !(orientation & ORIENTATION_SWAP_XY)) ||
/*TODO*///					(vga_tweaked[i].vertical_mode && (orientation & ORIENTATION_SWAP_XY)))
/*TODO*///				{
/*TODO*///					reg = vga_tweaked[i].reg;
/*TODO*///					reglen = vga_tweaked[i].reglen;
/*TODO*///					if (videofreq == -1)
/*TODO*///						videofreq = vga_tweaked[i].syncvgafreq;
/*TODO*///					found = 1;
/*TODO*///					unchained = vga_tweaked[i].unchained;
/*TODO*///					if(unchained)
/*TODO*///					{
/*TODO*///						/* for unchained modes, turn off dirty updates */
/*TODO*///						/* as any speed gain is lost in the complex multi-page update needed */
/*TODO*///						/* plus - non-dirty updates remove unchained 'shearing' */
/*TODO*///						use_dirty = 0;
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///
/*TODO*///		/* can't find a VGA mode, use VESA */
/*TODO*///		if (found == 0)
/*TODO*///		{
/*TODO*///			use_vesa = 1;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			use_vesa = 0;
/*TODO*///			if (videofreq < 0) videofreq = 0;
/*TODO*///			else if (videofreq > 3) videofreq = 3;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (use_vesa != 0)
/*TODO*///	{
/*TODO*///		/*removed local 'found' */
/*TODO*///		int mode, bits, err;
/*TODO*///
/*TODO*///		mode = gfx_mode;
/*TODO*///		found = 0;
/*TODO*///		bits = depth;
/*TODO*///
/*TODO*///		/* Try the specified vesamode, 565 and 555 for 16 bit color modes, */
/*TODO*///		/* doubled resolution in case of noscanlines and if not succesful  */
/*TODO*///		/* repeat for all "lower" VESA modes. NS/BW 19980102 */
/*TODO*///
/*TODO*///		while (!found)
/*TODO*///		{
/*TODO*///			set_color_depth(bits);
/*TODO*///
/*TODO*///			/* allocate a wide enough virtual screen if possible */
/*TODO*///			/* we round the width (in dwords) to be an even multiple 256 - that */
/*TODO*///			/* way, during page flipping only one byte of the video RAM */
/*TODO*///			/* address changes, therefore preventing flickering. */
/*TODO*///			if (bits == 8)
/*TODO*///				triplebuf_page_width = (gfx_width + 0x3ff) & ~0x3ff;
/*TODO*///			else if (bits == 16)
/*TODO*///				triplebuf_page_width = (gfx_width + 0x1ff) & ~0x1ff;
/*TODO*///			else	/* bits == 32 */
/*TODO*///				triplebuf_page_width = (gfx_width + 0x1ff) & ~0x0ff;
/*TODO*///
/*TODO*///			/* don't ask for a larger screen if triplebuffer not requested - could */
/*TODO*///			/* cause problems in some cases. */
/*TODO*///			err = 1;
/*TODO*///			if (use_triplebuf)
/*TODO*///				err = set_gfx_mode(mode,gfx_width,gfx_height,3*triplebuf_page_width,0);
/*TODO*///			if (err)
/*TODO*///			{
/*TODO*///				/* if we're using a SVGA 15KHz driver - tell Allegro the virtual screen width */
/*TODO*///				if(SVGA15KHzdriver)
/*TODO*///					err = set_gfx_mode(mode,gfx_width,gfx_height,SVGA15KHzdriver->getlogicalwidth(gfx_width),0);
/*TODO*///				else
/*TODO*///					err = set_gfx_mode(mode,gfx_width,gfx_height,0,0);
/*TODO*///			}
/*TODO*///
/*TODO*///			logerror("Trying ");
/*TODO*///			if      (mode == GFX_VESA1)
/*TODO*///				logerror("VESA1");
/*TODO*///			else if (mode == GFX_VESA2B)
/*TODO*///				logerror("VESA2B");
/*TODO*///			else if (mode == GFX_VESA2L)
/*TODO*///				logerror("VESA2L");
/*TODO*///			else if (mode == GFX_VESA3)
/*TODO*///				logerror("VESA3");
/*TODO*///			logerror("  %dx%d, %d bit\n",
/*TODO*///					gfx_width, gfx_height, bits);
/*TODO*///
/*TODO*///			if (err == 0)
/*TODO*///			{
/*TODO*///				found = 1;
/*TODO*///				/* replace gfx_mode with found mode */
/*TODO*///				gfx_mode = mode;
/*TODO*///				continue;
/*TODO*///			}
/*TODO*///			else logerror("%s\n",allegro_error);
/*TODO*///
/*TODO*///			/* Now adjust parameters for the next loop */
/*TODO*///
/*TODO*///			/* try 5-5-5 in case there is no 5-6-5 16 bit color mode */
/*TODO*///			if (depth == 16)
/*TODO*///			{
/*TODO*///				if (bits == 16)
/*TODO*///				{
/*TODO*///					bits = 15;
/*TODO*///					continue;
/*TODO*///				}
/*TODO*///				else
/*TODO*///					bits = 16; /* reset to 5-6-5 */
/*TODO*///			}
/*TODO*///
/*TODO*///			/* try VESA modes in VESA3-VESA2L-VESA2B-VESA1 order */
/*TODO*///
/*TODO*///			if (mode == GFX_VESA3)
/*TODO*///			{
/*TODO*///				mode = GFX_VESA2L;
/*TODO*///				continue;
/*TODO*///			}
/*TODO*///			else if (mode == GFX_VESA2L)
/*TODO*///			{
/*TODO*///				mode = GFX_VESA2B;
/*TODO*///				continue;
/*TODO*///			}
/*TODO*///			else if (mode == GFX_VESA2B)
/*TODO*///			{
/*TODO*///				mode = GFX_VESA1;
/*TODO*///				continue;
/*TODO*///			}
/*TODO*///			else if (mode == GFX_VESA1)
/*TODO*///				mode = gfx_mode; /* restart with the mode given in mame.cfg */
/*TODO*///
/*TODO*///			/* try higher resolutions */
/*TODO*///			if (auto_resolution)
/*TODO*///			{
/*TODO*///				if (stretch && gfx_width <= 512)
/*TODO*///				{
/*TODO*///					/* low res VESA mode not available, try an high res one */
/*TODO*///					gfx_width *= 2;
/*TODO*///					gfx_height *= 2;
/*TODO*///					continue;
/*TODO*///				}
/*TODO*///
/*TODO*///				/* try next higher resolution */
/*TODO*///				if (gfx_height < 300 && gfx_width < 400)
/*TODO*///				{
/*TODO*///					gfx_width = 400;
/*TODO*///					gfx_height = 300;
/*TODO*///					continue;
/*TODO*///				}
/*TODO*///				else if (gfx_height < 384 && gfx_width < 512)
/*TODO*///				{
/*TODO*///					gfx_width = 512;
/*TODO*///					gfx_height = 384;
/*TODO*///					continue;
/*TODO*///				}
/*TODO*///				else if (gfx_height < 480 && gfx_width < 640)
/*TODO*///				{
/*TODO*///					gfx_width = 640;
/*TODO*///					gfx_height = 480;
/*TODO*///					continue;
/*TODO*///				}
/*TODO*///				else if (gfx_height < 600 && gfx_width < 800)
/*TODO*///				{
/*TODO*///					gfx_width = 800;
/*TODO*///					gfx_height = 600;
/*TODO*///					continue;
/*TODO*///				}
/*TODO*///				else if (gfx_height < 768 && gfx_width < 1024)
/*TODO*///				{
/*TODO*///					gfx_width = 1024;
/*TODO*///					gfx_height = 768;
/*TODO*///					continue;
/*TODO*///				}
/*TODO*///			}
/*TODO*///
/*TODO*///			/* If there was no continue up to this point, we give up */
/*TODO*///			break;
/*TODO*///		}
/*TODO*///
/*TODO*///		if (found == 0)
/*TODO*///		{
/*TODO*///			printf ("\nNo %d-bit %dx%d VESA mode available.\n",
/*TODO*///					depth,gfx_width,gfx_height);
/*TODO*///			printf ("\nPossible causes:\n"
/*TODO*///"1) Your video card does not support VESA modes at all. Almost all\n"
/*TODO*///"   video cards support VESA modes natively these days, so you probably\n"
/*TODO*///"   have an older card which needs some driver loaded first.\n"
/*TODO*///"   In case you can't find such a driver in the software that came with\n"
/*TODO*///"   your video card, Scitech Display Doctor or (for S3 cards) S3VBE\n"
/*TODO*///"   are good alternatives.\n"
/*TODO*///"2) Your VESA implementation does not support this resolution. For example,\n"
/*TODO*///"   '-320x240', '-400x300' and '-512x384' are only supported by a few\n"
/*TODO*///"   implementations.\n"
/*TODO*///"3) Your video card doesn't support this resolution at this color depth.\n"
/*TODO*///"   For example, 1024x768 in 16 bit colors requires 2MB video memory.\n"
/*TODO*///"   You can either force an 8 bit video mode ('-depth 8') or use a lower\n"
/*TODO*///"   resolution ('-640x480', '-800x600').\n");
/*TODO*///			return 0;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			logerror("Found matching %s mode\n", gfx_driver->desc);
/*TODO*///			gfx_mode = mode;
/*TODO*///			/* disable triple buffering if the screen is not large enough */
/*TODO*///			logerror("Virtual screen size %dx%d\n",VIRTUAL_W,VIRTUAL_H);
/*TODO*///			if (VIRTUAL_W < 3*triplebuf_page_width)
/*TODO*///			{
/*TODO*///				use_triplebuf = 0;
/*TODO*///				logerror("Triple buffer disabled\n");
/*TODO*///			}
/*TODO*///
/*TODO*///			/* if triple buffering is enabled, turn off vsync */
/*TODO*///			if (use_triplebuf)
/*TODO*///			{
/*TODO*///				wait_vsync = 0;
/*TODO*///				video_sync = 0;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///
/*TODO*///
/*TODO*///		/* set the VGA clock */
/*TODO*///		if (video_sync || always_synced || wait_vsync)
/*TODO*///			reg[0].value = (reg[0].value & 0xf3) | (videofreq << 2);
/*TODO*///
/*TODO*///		/* VGA triple buffering */
/*TODO*///		if(use_triplebuf)
/*TODO*///		{
/*TODO*///
/*TODO*///			int vga_page_size = (gfx_width * gfx_height);
/*TODO*///			/* see if it'll fit */
/*TODO*///			if ((vga_page_size * 3) > 0x40000)
/*TODO*///			{
/*TODO*///				/* too big */
/*TODO*///				logerror("tweaked mode %dx%d is too large to triple buffer\ntriple buffering disabled\n",gfx_width,gfx_height);
/*TODO*///				use_triplebuf = 0;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				/* it fits, so set up the 3 pages */
/*TODO*///				no_xpages = 3;
/*TODO*///				xpage_size = vga_page_size / 4;
/*TODO*///				logerror("unchained VGA triple buffering page size :%d\n",xpage_size);
/*TODO*///				/* and make sure the mode's unchained */
/*TODO*///				unchain_vga (reg);
/*TODO*///				/* triple buffering is enabled, turn off vsync */
/*TODO*///				wait_vsync = 0;
/*TODO*///				video_sync = 0;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		/* center the mode */
/*TODO*///		center_mode (reg);
/*TODO*///
/*TODO*///		/* set the horizontal and vertical total */
/*TODO*///		if (scanrate15KHz)
/*TODO*///			/* 15.75KHz modes */
/*TODO*///			adjust_array = arcade_adjust;
/*TODO*///		else
/*TODO*///			/* PC monitor modes */
/*TODO*///			adjust_array = pc_adjust;
/*TODO*///
/*TODO*///		for (i=0; adjust_array[i].x != 0; i++)
/*TODO*///		{
/*TODO*///			if ((gfx_width == adjust_array[i].x) && (gfx_height == adjust_array[i].y))
/*TODO*///			{
/*TODO*///				/* check for 'special vertical' modes */
/*TODO*///				if((!adjust_array[i].vertical_mode && !(orientation & ORIENTATION_SWAP_XY)) ||
/*TODO*///					(adjust_array[i].vertical_mode && (orientation & ORIENTATION_SWAP_XY)))
/*TODO*///				{
/*TODO*///					reg[H_TOTAL_INDEX].value = *adjust_array[i].hadjust;
/*TODO*///					reg[V_TOTAL_INDEX].value = *adjust_array[i].vadjust;
/*TODO*///					break;
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		/*if scanlines were requested - change the array values to get a scanline mode */
/*TODO*///		if (scanlines && !scanrate15KHz)
/*TODO*///			reg = make_scanline_mode(reg,reglen);
/*TODO*///
/*TODO*///		/* big hack: open a mode 13h screen using Allegro, then load the custom screen */
/*TODO*///		/* definition over it. */
/*TODO*///		if (set_gfx_mode(GFX_VGA,320,200,0,0) != 0)
/*TODO*///			return 0;
/*TODO*///
/*TODO*///		logerror("Generated Tweak Values :-\n");
/*TODO*///		for (i=0; i<reglen; i++)
/*TODO*///		{
/*TODO*///			logerror("{ 0x%02x, 0x%02x, 0x%02x},",reg[i].port,reg[i].index,reg[i].value);
/*TODO*///			if (!((i+1)%3))
/*TODO*///				logerror("\n");
/*TODO*///		}
/*TODO*///
/*TODO*///		/* tweak the mode */
/*TODO*///		outRegArray(reg,reglen);
/*TODO*///
/*TODO*///		/* check for unchained mode,  if unchained clear all pages */
/*TODO*///		if (unchained)
/*TODO*///		{
/*TODO*///			unsigned long address;
/*TODO*///			/* clear all 4 bit planes */
/*TODO*///			outportw (0x3c4, (0x02 | (0x0f << 0x08)));
/*TODO*///			for (address = 0xa0000; address < 0xb0000; address += 4)
/*TODO*///				_farpokel(screen->seg, address, 0);
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///	gone_to_gfx_mode = 1;


            vsync_frame_rate = video_fps;

/*TODO*///	if (video_sync)
/*TODO*///	{
/*TODO*///		TICKER a,b;
/*TODO*///		float rate;
/*TODO*///
/*TODO*///
/*TODO*///		/* wait some time to let everything stabilize */
/*TODO*///		for (i = 0;i < 60;i++)
/*TODO*///		{
/*TODO*///			vsync();
/*TODO*///			a = ticker();
/*TODO*///		}
/*TODO*///
/*TODO*///		/* small delay for really really fast machines */
/*TODO*///		for (i = 0;i < 100000;i++) ;
/*TODO*///
/*TODO*///		vsync();
/*TODO*///		b = ticker();
/*TODO*///
/*TODO*///		rate = ((float)TICKS_PER_SEC)/(b-a);
/*TODO*///
/*TODO*///		logerror("target frame rate = %dfps, video frame rate = %3.2fHz\n",video_fps,rate);
/*TODO*///
/*TODO*///		/* don't allow more than 8% difference between target and actual frame rate */
/*TODO*///		while (rate > video_fps * 108 / 100)
/*TODO*///			rate /= 2;
/*TODO*///
/*TODO*///		if (rate < video_fps * 92 / 100)
/*TODO*///		{
/*TODO*///			osd_close_display();
/*TODO*///			logerror("-vsync option cannot be used with this display mode:\n"
/*TODO*///						"video refresh frequency = %dHz, target frame rate = %ffps\n",
/*TODO*///						(int)(TICKS_PER_SEC/(b-a)),video_fps);
/*TODO*///			return 0;
/*TODO*///		}
/*TODO*///
/*TODO*///		logerror("adjusted video frame rate = %3.2fHz\n",rate);
/*TODO*///			vsync_frame_rate = rate;
/*TODO*///
/*TODO*///		if (Machine->sample_rate)
/*TODO*///		{
/*TODO*///			Machine->sample_rate = Machine->sample_rate * video_fps / rate;
/*TODO*///			logerror("sample rate adjusted to match video freq: %d\n",Machine->sample_rate);
/*TODO*///		}
/*TODO*///	}

	warming_up = 1;

	return 1;
}


    /*
    Create a display screen, or window, of the given dimensions (or larger).
    Attributes are the ones defined in driver.h.
    Returns 0 on success.
    */
    public static int osd_create_display(int width,int height,int depth,int fps,int attributes,int orientation)
    {
            logerror("width %d, height %d depth %d\n",width,height,depth);
            if ((Machine.drv.video_attributes & VIDEO_TYPE_VECTOR) == 0) {//temp hack
                width = Machine.drv.screen_width;
                height = Machine.drv.screen_height;//TODO workaround to get it work for now
            }

            video_depth = depth;
            if (video_depth == 15) video_depth = 16;
            video_fps = fps;
            video_attributes = attributes;
            video_orientation = orientation;

    /*TODO*///	show_debugger = 0;

            brightness = 100;
            brightness_paused_adjust = 1.0f;
            dirty_bright = 1;

            if (frameskip < 0) frameskip = 0;
            if (frameskip >= FRAMESKIP_LEVELS) frameskip = FRAMESKIP_LEVELS-1;



            gone_to_gfx_mode = 0;

            /* Look if this is a vector game */
            if ((attributes & VIDEO_TYPE_VECTOR) != 0)
                    vector_game = 1;
            else
                    vector_game = 0;


            /*TODO*///if (use_dirty == -1)	/* dirty=auto in mame.cfg? */
            /*TODO*///{
            /*TODO*///        /* Is the game using a dirty system? */
            /*TODO*///        if ((attributes & VIDEO_SUPPORTS_DIRTY)!=0)
            /*TODO*///                use_dirty = 1;
            /*TODO*///        else
            /*TODO*///                use_dirty = 0;
            /*TODO*///}

            select_display_mode(width,height,video_depth,attributes,orientation);

    /*TODO*////* find a VESA driver for 15KHz modes just in case we need it later on */
    /*TODO*///	if (scanrate15KHz)
    /*TODO*///		getSVGA15KHzdriver (&SVGA15KHzdriver);
    /*TODO*///	else
    /*TODO*///		SVGA15KHzdriver = 0;


            if (osd_set_display(width,height,video_depth,attributes,orientation) == 0)
                    return 1;

            /* set visible area to nothing just to initialize it - it will be set by the core */
            /*TODO*///osd_set_visible_area(0,0,0,0);

    /*TODO*///   /*Check for SVGA 15.75KHz mode (req. for 15.75KHz Arcade Monitor Modes)
    /*TODO*///     need to do this here, as the double params will be set up correctly */
    /*TODO*///	if (use_vesa == 1 && scanrate15KHz)
    /*TODO*///	{
    /*TODO*///		int dbl;
    /*TODO*///		dbl = (ymultiply >= 2);
    /*TODO*///		/* check that we found a driver */
    /*TODO*///		if (!SVGA15KHzdriver)
    /*TODO*///		{
    /*TODO*///			printf ("\nUnable to find 15.75KHz SVGA driver for %dx%d\n", gfx_width, gfx_height);
    /*TODO*///			return 1;
    /*TODO*///		}
    /*TODO*///		logerror("Using %s 15.75KHz SVGA driver\n", SVGA15KHzdriver->name);
    /*TODO*///		/*and try to set the mode */
    /*TODO*///		if (!SVGA15KHzdriver->setSVGA15KHzmode (dbl, gfx_width, gfx_height))
    /*TODO*///		{
    /*TODO*///			printf ("\nUnable to set SVGA 15.75KHz mode %dx%d (driver: %s)\n", gfx_width, gfx_height, SVGA15KHzdriver->name);
    /*TODO*///			return 1;
    /*TODO*///		}
    /*TODO*///		/* if we're doubling, we might as well have scanlines */
    /*TODO*///		/* the 15.75KHz driver is going to drop every other line anyway -
    /*TODO*///			so we can avoid drawing them and save some time */
    /*TODO*///		if(dbl)
    /*TODO*///			scanlines=1;
    /*TODO*///	}

            update_video_first_time = 1;

            return 0;
    }

    
    /* shut up the display */
    public static void osd_close_display()
    {
/*TODO*///	if (gone_to_gfx_mode != 0)
/*TODO*///	{
/*TODO*///		/* tidy up if 15.75KHz SVGA mode used */
/*TODO*///		if (scanrate15KHz && use_vesa == 1)
/*TODO*///		{
/*TODO*///			/* check we've got a valid driver before calling it */
/*TODO*///			if (SVGA15KHzdriver != NULL)
/*TODO*///				SVGA15KHzdriver->resetSVGA15KHzmode();
/*TODO*///		}
/*TODO*///
/*TODO*///		set_gfx_mode (GFX_TEXT,0,0,0,0);
/*TODO*///
		if (frames_displayed > FRAMES_TO_SKIP)
			printf("Average FPS: %f\n",(double)TICKS_PER_SEC/(end_time-start_time)*(frames_displayed-FRAMES_TO_SKIP));
/*TODO*///	}
/*TODO*///
/*TODO*///	free(dirtycolor);
/*TODO*///	dirtycolor = 0;
/*TODO*///	free(current_palette);
/*TODO*///	current_palette = 0;
/*TODO*///	free(palette_16bit_lookup);
/*TODO*///	palette_16bit_lookup = 0;
    }


/*TODO*///void osd_debugger_focus(int debugger_has_focus)
/*TODO*///{
/*TODO*///    if (show_debugger != debugger_has_focus)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///		show_debugger = debugger_has_focus;
/*TODO*///		debugger_focus_changed = 1;
/*TODO*///		for (i = 0;i < screen_colors;i++)
/*TODO*///			dirtycolor[i] = 1;
/*TODO*///		dirtypalette = 1;
/*TODO*///
/*TODO*///		if (!show_debugger)
/*TODO*///		{
/*TODO*///			/* silly way to clear the screen */
/*TODO*///			struct mame_bitmap *clrbitmap;
/*TODO*///			clrbitmap = bitmap_alloc_depth(gfx_display_columns,gfx_display_lines,-video_depth);
/*TODO*///			if (clrbitmap)
/*TODO*///			{
/*TODO*///				fillbitmap(clrbitmap,Machine->uifont->colortable[0],NULL);
/*TODO*///				/* three times to handle triple buffering */
/*TODO*///				update_screen_debugger(clrbitmap);
/*TODO*///				update_screen_debugger(clrbitmap);
/*TODO*///				update_screen_debugger(clrbitmap);
/*TODO*///				bitmap_free(clrbitmap);
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///static int init_direct_mapped(unsigned int totalcolors,
/*TODO*///		const UINT8 *palette,UINT32 *rgb_components,
/*TODO*///		const UINT8 *debug_palette,pen_t *debug_pens)
/*TODO*///{
/*TODO*///	if (video_depth == 16)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///		int r,g,b;
/*TODO*///		unsigned char *pal;
/*TODO*///
/*TODO*///		screen_colors = 32768;
/*TODO*///
/*TODO*///		dirtycolor = malloc(screen_colors * sizeof(int));
/*TODO*///		current_palette = malloc(3 * screen_colors * sizeof(unsigned char));
/*TODO*///		palette_16bit_lookup = malloc(screen_colors * sizeof(palette_16bit_lookup[0]));
/*TODO*///		if (dirtycolor == 0 || current_palette == 0 || palette_16bit_lookup == 0)
/*TODO*///			return 1;
/*TODO*///
/*TODO*///		for (i = 0;i < screen_colors;i++)
/*TODO*///			dirtycolor[i] = 1;
/*TODO*///		dirtypalette = 1;
/*TODO*///
/*TODO*///		pal = current_palette;
/*TODO*///		for (r = 0;r < 32;r++)
/*TODO*///		{
/*TODO*///			for (g = 0;g < 32;g++)
/*TODO*///			{
/*TODO*///				for (b = 0;b < 32;b++)
/*TODO*///				{
/*TODO*///					*(pal++) = (r << 3) | (r >> 2);
/*TODO*///					*(pal++) = (g << 3) | (g >> 2);
/*TODO*///					*(pal++) = (b << 3) | (b >> 2);
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///#if 0
/*TODO*///		for (i = 0;i < totalcolors;i++)
/*TODO*///		{
/*TODO*///			r = 255 * brightness * pow(palette[3*i+0] / 255.0, 1 / osd_gamma_correction) / 100;
/*TODO*///			g = 255 * brightness * pow(palette[3*i+1] / 255.0, 1 / osd_gamma_correction) / 100;
/*TODO*///			b = 255 * brightness * pow(palette[3*i+2] / 255.0, 1 / osd_gamma_correction) / 100;
/*TODO*///			*rgb_components++ = makecol(r,g,b);
/*TODO*///		}
/*TODO*///#endif
/*TODO*///		rgb_components[0] = 0x7c00;
/*TODO*///		rgb_components[1] = 0x03e0;
/*TODO*///		rgb_components[2] = 0x001f;
/*TODO*///
/*TODO*///		Machine->uifont->colortable[0] = 0x0000;
/*TODO*///		Machine->uifont->colortable[1] = 0x7fff;
/*TODO*///		Machine->uifont->colortable[2] = 0x7fff;
/*TODO*///		Machine->uifont->colortable[3] = 0x0000;
/*TODO*///
/*TODO*///#if 0
/*TODO*///		if (debug_pens)
/*TODO*///		{
/*TODO*///			for (i = 0;i < DEBUGGER_TOTAL_COLORS;i++)
/*TODO*///			{
/*TODO*///				r = debug_palette[3*i+0];
/*TODO*///				g = debug_palette[3*i+1];
/*TODO*///				b = debug_palette[3*i+2];
/*TODO*///				*debug_pens++ = makecol(r,g,b);
/*TODO*///			}
/*TODO*///		}
/*TODO*///#endif
/*TODO*///		if (debug_pens)
/*TODO*///		{
/*TODO*///			for (i = 0;i < DEBUGGER_TOTAL_COLORS;i++)
/*TODO*///			{
/*TODO*///				r = debug_palette[3*i+0];
/*TODO*///				g = debug_palette[3*i+1];
/*TODO*///				b = debug_palette[3*i+2];
/*TODO*///				*debug_pens++ = r * rgb_components[0] / 0xff + g * rgb_components[1] / 0xff + b * rgb_components[2] / 0xff;
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///
/*TODO*///		if (use_mmx == -1) /* mmx=auto: can new mmx blitters be applied? */
/*TODO*///		{
/*TODO*///			/* impossible cases follow */
/*TODO*///			if (!cpu_mmx)
/*TODO*///				mmxlfb = 0;
/*TODO*///			else if ((gfx_mode != GFX_VESA2L) && (gfx_mode != GFX_VESA3))
/*TODO*///				mmxlfb = 0;
/*TODO*///			/* not yet implemented cases follow */
/*TODO*///			else if ((xmultiply > 2) || (ymultiply > 2))
/*TODO*///				mmxlfb = 0;
/*TODO*///			else
/*TODO*///				mmxlfb = 1;
/*TODO*///		}
/*TODO*///		else /* use forced mmx= setting from mame.cfg at own risk!!! */
/*TODO*///			mmxlfb = use_mmx;
/*TODO*///
/*TODO*///		update_screen = updaters16_palettized[xmultiply-1][ymultiply-1][scanlines?1:0][use_dirty?1:0];
/*TODO*///		update_screen_debugger = updaters16_palettized[0][0][0][0];
/*TODO*///	}
/*TODO*///	else if (video_depth == 32)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///		int r,g,b;
/*TODO*///
/*TODO*///
/*TODO*///		screen_colors = 2;
/*TODO*///
/*TODO*///		dirtycolor = malloc(screen_colors * sizeof(int));
/*TODO*///		current_palette = malloc(3 * screen_colors * sizeof(unsigned char));
/*TODO*///		if (dirtycolor == 0 || current_palette == 0)
/*TODO*///			return 1;
/*TODO*///
/*TODO*///		for (i = 0;i < screen_colors;i++)
/*TODO*///			dirtycolor[i] = 1;
/*TODO*///		dirtypalette = 1;
/*TODO*///
/*TODO*///		for (i = 0;i < totalcolors;i++)
/*TODO*///		{
/*TODO*///			r = 255 * brightness * pow(palette[3*i+0] / 255.0, 1 / osd_gamma_correction) / 100;
/*TODO*///			g = 255 * brightness * pow(palette[3*i+1] / 255.0, 1 / osd_gamma_correction) / 100;
/*TODO*///			b = 255 * brightness * pow(palette[3*i+2] / 255.0, 1 / osd_gamma_correction) / 100;
/*TODO*///			*rgb_components++ = makecol(r,g,b);
/*TODO*///		}
/*TODO*///
/*TODO*///		Machine->uifont->colortable[0] = makecol(0x00,0x00,0x00);
/*TODO*///		Machine->uifont->colortable[1] = makecol(0xff,0xff,0xff);
/*TODO*///		Machine->uifont->colortable[2] = makecol(0xff,0xff,0xff);
/*TODO*///		Machine->uifont->colortable[3] = makecol(0x00,0x00,0x00);
/*TODO*///
/*TODO*///		if (debug_pens)
/*TODO*///		{
/*TODO*///			for (i = 0;i < DEBUGGER_TOTAL_COLORS;i++)
/*TODO*///			{
/*TODO*///				r = debug_palette[3*i+0];
/*TODO*///				g = debug_palette[3*i+1];
/*TODO*///				b = debug_palette[3*i+2];
/*TODO*///				*debug_pens++ = makecol(r,g,b);
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		if (use_mmx == -1) /* mmx=auto: can new mmx blitters be applied? */
/*TODO*///		{
/*TODO*///			/* impossible cases follow */
/*TODO*///			if (!cpu_mmx)
/*TODO*///				mmxlfb = 0;
/*TODO*///			else if ((gfx_mode != GFX_VESA2L) && (gfx_mode != GFX_VESA3))
/*TODO*///				mmxlfb = 0;
/*TODO*///			/* not yet implemented cases follow */
/*TODO*///			else if ((xmultiply > 2) || (ymultiply > 2))
/*TODO*///				mmxlfb = 0;
/*TODO*///			else
/*TODO*///				mmxlfb = 1;
/*TODO*///		}
/*TODO*///		else /* use forced mmx= setting from mame.cfg at own risk!!! */
/*TODO*///			mmxlfb = use_mmx;
/*TODO*///
/*TODO*///		update_screen = updaters32[xmultiply-1][ymultiply-1][scanlines?1:0][use_dirty?1:0];
/*TODO*///		update_screen_debugger = updaters32[0][0][0][0];
/*TODO*///	}
/*TODO*///
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*///
public static int osd_allocate_colors(int totalcolors,char[] palette,int[] rgb_components)
{
        /*TODO*///TEMPHACK*/
        video_depth = Machine.scrbitmap.depth;
        
        System.out.println("Total colors="+totalcolors);
        System.out.println("Video depth="+video_depth);
        
	int i;
/*TODO*///
/*TODO*///	if (video_attributes & VIDEO_RGB_DIRECT)
/*TODO*///	{
/*TODO*///		rgb_direct = 1;
/*TODO*///		return init_direct_mapped(totalcolors,palette,rgb_components,debug_palette,debug_pens);
/*TODO*///	}
/*TODO*///
/*TODO*///	rgb_direct = 0;

	screen_colors = totalcolors;
	if (video_depth != 8)
		screen_colors += 2;
	else screen_colors = 256;

	dirtycolor = new int[screen_colors];
	current_palette = new UBytePtr(3 * screen_colors);
	palette_16bit_lookup = new UShortPtr(screen_colors*2);
	if (dirtycolor == null || current_palette == null || palette_16bit_lookup == null)
		return 1;

	for (i = 0;i < screen_colors;i++)
		dirtycolor[i] = 1;
	dirtypalette = 1;
	for (i = 0;i < screen_colors;i++){
		current_palette.write(3*i+0, 0);
                current_palette.write(3*i+1, 0);
                current_palette.write(3*i+2, 0);
        }
        
        current_palette.offset=0;
        Machine.uifont.colortable.offset=0;

	// reserve color totalcolors+1 for the user interface text */
	if (totalcolors < 65535)
	{
		current_palette.write((totalcolors+1)*3+0, 0xff);
                current_palette.write((totalcolors+1)*3+1, 0xff);
                current_palette.write((totalcolors+1)*3+2, 0xff);
		Machine.uifont.colortable.write(0, totalcolors);
		Machine.uifont.colortable.write(1, totalcolors + 1);
		Machine.uifont.colortable.write(2, totalcolors + 1);
		Machine.uifont.colortable.write(3, totalcolors);
	}
	else
	{
		Machine.uifont.colortable.write(0, 0);
		Machine.uifont.colortable.write(1, 65535);
		Machine.uifont.colortable.write(2, 65535);
		Machine.uifont.colortable.write(3, 0);
	}
        
        current_palette.offset=0;

	for (i = 0;i < totalcolors;i++)
	{
		current_palette.write(3*i+0, palette[3*i]);
		current_palette.write(3*i+1, palette[3*i+1]);
		current_palette.write(3*i+2, palette[3*i+2]);
	}
        
        /*Machine.uifont.colortable.offset = 0;
        Machine.uifont.colortable.write(0, totalcolors);
	Machine.uifont.colortable.write(1, totalcolors+1);
	Machine.uifont.colortable.write(2, totalcolors+1);
	Machine.uifont.colortable.write(3, 0);
        current_palette.offset=0;        
        current_palette.write((totalcolors)*3+0, 0x00);
        current_palette.write((totalcolors)*3+1, 0x00);
        current_palette.write((totalcolors)*3+2, 0x00);
        
        current_palette.write((totalcolors+1)*3+0, 0xff);
        current_palette.write((totalcolors+1)*3+1, 0xff);
        current_palette.write((totalcolors+1)*3+2, 0xff);*/

/*TODO*///	if (debug_pens)
/*TODO*///	{
/*TODO*///		for (i = 0;i < DEBUGGER_TOTAL_COLORS;i++)
/*TODO*///			debug_pens[i] = i;
/*TODO*///	}
/*TODO*///
/*TODO*///	dbg_palette = debug_palette;
/*TODO*///
/*TODO*///
/*TODO*///	if (use_vesa == 0)
/*TODO*///	{
/*TODO*///		if (use_dirty) /* supports dirty ? */
/*TODO*///		{
/*TODO*///			if (unchained)
/*TODO*///			{
/*TODO*///				update_screen = update_screen_debugger = blitscreen_dirty1_unchained_vga;
/*TODO*///				logerror("blitscreen_dirty1_unchained_vga\n");
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				update_screen = update_screen_debugger = blitscreen_dirty1_vga;
/*TODO*///				logerror("blitscreen_dirty1_vga\n");
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			/* check for unchained modes */
/*TODO*///			if (unchained)
/*TODO*///			{
/*TODO*///				update_screen = update_screen_debugger = blitscreen_dirty0_unchained_vga;
/*TODO*///				logerror("blitscreen_dirty0_unchained_vga\n");
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				update_screen = update_screen_debugger = blitscreen_dirty0_vga;
/*TODO*///				logerror("blitscreen_dirty0_vga\n");
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		if (use_mmx == -1) /* mmx=auto: can new mmx blitters be applied? */
/*TODO*///		{
/*TODO*///			/* impossible cases follow */
/*TODO*///			if (!cpu_mmx)
/*TODO*///				mmxlfb = 0;
/*TODO*///			else if ((gfx_mode != GFX_VESA2L) && (gfx_mode != GFX_VESA3))
/*TODO*///				mmxlfb = 0;
/*TODO*///			/* not yet implemented cases follow */
/*TODO*///			else if ((xmultiply > 2) || (ymultiply > 2))
/*TODO*///				mmxlfb = 0;
/*TODO*///			else
/*TODO*///				mmxlfb = 1;
/*TODO*///		}
/*TODO*///		else /* use forced mmx= setting from mame.cfg at own risk!!! */
/*TODO*///			mmxlfb = use_mmx;
/*TODO*///
/*TODO*///		if (video_depth == 16)
/*TODO*///		{
/*TODO*///			if (rgb_direct)
/*TODO*///			{
/*TODO*///				update_screen = updaters16[xmultiply-1][ymultiply-1][scanlines?1:0][use_dirty?1:0];
/*TODO*///				update_screen_debugger = updaters16[0][0][0][0];
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				update_screen = updaters16_palettized[xmultiply-1][ymultiply-1][scanlines?1:0][use_dirty?1:0];
/*TODO*///				update_screen_debugger = updaters16_palettized[0][0][0][0];
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			update_screen = updaters8[xmultiply-1][ymultiply-1][scanlines?1:0][use_dirty?1:0];
/*TODO*///			update_screen_debugger = updaters8[0][0][0][0];
/*TODO*///		}
/*TODO*///	}
/*TODO*///
	back_buffer = new char[Machine.scrbitmap.line[0].memory.length];
        for (int ii = 0 ; ii<back_buffer.length ; ii++)
            back_buffer[ii] = (char) Machine.uifont.colortable.read(0);
        
        return 0;
}



    public static void osd_modify_pen(int pen, int red, int green, int blue)
    {
            /*TODO*///if (rgb_direct != 0)
            /*TODO*///{
            /*TODO*///        logerror("error: osd_modify_pen() called with rgb direct mode\n");
            /*TODO*///        return;
            /*TODO*///}


            if (current_palette.read(3*pen+0) != red ||
                            current_palette.read(3*pen+1) != green ||
                            current_palette.read(3*pen+2) != blue)
            {
                    current_palette.write(3*pen+0, red);
                    current_palette.write(3*pen+1, green);
                    current_palette.write(3*pen+2, blue);

                    dirtycolor[pen] = 1;
                    dirtypalette = 1;
            }
    }



/*TODO*///static void update_screen_dummy(struct mame_bitmap *bitmap)
/*TODO*///{
/*TODO*///	logerror("msdos/video.c: undefined update_screen() function for %d x %d!\n",xmultiply,ymultiply);
/*TODO*///}
/*TODO*///
/*TODO*///INLINE void pan_display(void)
/*TODO*///{
/*TODO*///	int pan_changed = 0;
/*TODO*///
/*TODO*///	/* horizontal panning */
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_PAN_LEFT,1))
/*TODO*///		if (skipcolumns < skipcolumnsmax)
/*TODO*///		{
/*TODO*///			skipcolumns++;
/*TODO*///			mark_full_screen_dirty();
/*TODO*///			pan_changed = 1;
/*TODO*///		}
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_PAN_RIGHT,1))
/*TODO*///		if (skipcolumns > skipcolumnsmin)
/*TODO*///		{
/*TODO*///			skipcolumns--;
/*TODO*///			mark_full_screen_dirty();
/*TODO*///			pan_changed = 1;
/*TODO*///		}
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_PAN_DOWN,1))
/*TODO*///		if (skiplines < skiplinesmax)
/*TODO*///		{
/*TODO*///			skiplines++;
/*TODO*///			mark_full_screen_dirty();
/*TODO*///			pan_changed = 1;
/*TODO*///		}
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_PAN_UP,1))
/*TODO*///		if (skiplines > skiplinesmin)
/*TODO*///		{
/*TODO*///			skiplines--;
/*TODO*///			mark_full_screen_dirty();
/*TODO*///			pan_changed = 1;
/*TODO*///		}
/*TODO*///
/*TODO*///	if (pan_changed)
/*TODO*///	{
/*TODO*///		if (use_dirty) init_dirty(1);
/*TODO*///
/*TODO*///		set_ui_visarea (skipcolumns, skiplines, skipcolumns+gfx_display_columns-1, skiplines+gfx_display_lines-1);
/*TODO*///	}
/*TODO*///}

    static int[][] skiptable =
	{
		{ 0,0,0,0,0,0,0,0,0,0,0,0 },
		{ 0,0,0,0,0,0,0,0,0,0,0,1 },
		{ 0,0,0,0,0,1,0,0,0,0,0,1 },
		{ 0,0,0,1,0,0,0,1,0,0,0,1 },
		{ 0,0,1,0,0,1,0,0,1,0,0,1 },
		{ 0,1,0,0,1,0,1,0,0,1,0,1 },
		{ 0,1,0,1,0,1,0,1,0,1,0,1 },
		{ 0,1,0,1,1,0,1,0,1,1,0,1 },
		{ 0,1,1,0,1,1,0,1,1,0,1,1 },
		{ 0,1,1,1,0,1,1,1,0,1,1,1 },
		{ 0,1,1,1,1,1,0,1,1,1,1,1 },
		{ 0,1,1,1,1,1,1,1,1,1,1,1 }
	};


        public static int osd_skip_this_frame()
        {
            return skiptable[frameskip][frameskip_counter];
        }
        
        static int waittable[][] =
	{
		{ 1,1,1,1,1,1,1,1,1,1,1,1 },
		{ 2,1,1,1,1,1,1,1,1,1,1,0 },
		{ 2,1,1,1,1,0,2,1,1,1,1,0 },
		{ 2,1,1,0,2,1,1,0,2,1,1,0 },
		{ 2,1,0,2,1,0,2,1,0,2,1,0 },
		{ 2,0,2,1,0,2,0,2,1,0,2,0 },
		{ 2,0,2,0,2,0,2,0,2,0,2,0 },
		{ 2,0,2,0,0,3,0,2,0,0,3,0 },
		{ 3,0,0,3,0,0,3,0,0,3,0,0 },
		{ 4,0,0,0,4,0,0,0,4,0,0,0 },
		{ 6,0,0,0,0,0,6,0,0,0,0,0 },
		{12,0,0,0,0,0,0,0,0,0,0,0 }
	};
	
	public static int showfps,showfpstemp;
	/*TODO*///TICKER curr;
        public static long curr;
	/*TODO*///static TICKER prev_measure,this_frame_base,prev;
        static long prev_measure, this_frame_base, prev;
	public static int vups,vfcount;
	static int already_synced;
	
	static int leds_old;
        public static final int KB_NUMLOCK_FLAG = 0;
        public static final int KB_CAPSLOCK_FLAG = 1;
        public static final int KB_SCROLOCK_FLAG = 2;
        
        static int led_flags[] =
        {
                KB_NUMLOCK_FLAG,
                KB_CAPSLOCK_FLAG,
                KB_SCROLOCK_FLAG
        };

    static long last=0;
    static int frameskipadjust;
    /* Update the display. */
    public static void osd_update_video_and_audio(mame_bitmap game_bitmap, /*mame_bitmap debug_bitmap,*/ int leds_status)
    {
        /*TODO*///mame_bitmap bitmap;
        //int i;
	
	/*TODO*///if ((update_video_first_time!=0) || leds_old != leds_status)
	/*TODO*///{
		/*TODO*///update_video_first_time = 0;
		/*TODO*///leds_old = leds_status;

		/*TODO*///i = 0;
		/*TODO*///if ((leds_status & 1)!=0) i |= led_flags[0];
		/*TODO*///if ((leds_status & 2)!=0) i |= led_flags[1];
		/*TODO*///if ((leds_status & 4)!=0) i |= led_flags[2];
		/*TODO*///set_leds(i);
        /*TODO*///}


/*TODO*///	if (debug_bitmap && input_ui_pressed(IPT_UI_TOGGLE_DEBUG))
/*TODO*///	{
/*TODO*///		osd_debugger_focus(show_debugger ^ 1);
/*TODO*///	}
/*TODO*///
/*TODO*///	if (debugger_focus_changed)
/*TODO*///	{
/*TODO*///		debugger_focus_changed = 0;
/*TODO*///
/*TODO*///		if (show_debugger)
/*TODO*///			internal_set_visible_area(0,debug_bitmap->width-1,0,debug_bitmap->height-1,1);
/*TODO*///		else
/*TODO*///			internal_set_visible_area(vis_min_x,vis_max_x,vis_min_y,vis_max_y,0);
/*TODO*///	}
/*TODO*///
/*TODO*///	if (show_debugger && debug_bitmap) bitmap = debug_bitmap;
/*TODO*///	else 
/*TODO*///        bitmap = game_bitmap;
/*TODO*///
/*TODO*///	if (warming_up)
/*TODO*///	{
/*TODO*///		/* first time through, initialize timer */
/*TODO*///		prev_measure = ticker() - FRAMESKIP_LEVELS * TICKS_PER_SEC/video_fps;
/*TODO*///		warming_up = 0;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (frameskip_counter == 0)
/*TODO*///		this_frame_base = prev_measure + FRAMESKIP_LEVELS * TICKS_PER_SEC/video_fps;
/*TODO*///
/*TODO*///	if (throttle)
/*TODO*///	{
/*TODO*///		static TICKER last;
/*TODO*///
/*TODO*///		/* if too much time has passed since last sound update, disable throttling */
/*TODO*///		/* temporarily - we wouldn't be able to keep synch anyway. */
/*TODO*///		curr = ticker();
/*TODO*///		if ((curr - last) > 2*TICKS_PER_SEC / video_fps)
/*TODO*///			throttle = 0;
/*TODO*///		last = curr;
/*TODO*///
/*TODO*///		already_synced = msdos_update_audio();
/*TODO*///
/*TODO*///		throttle = 1;
/*TODO*///	}
/*TODO*///	else
/*TODO*///		already_synced = msdos_update_audio();
/*TODO*///
/*TODO*///
/*TODO*///	if (osd_skip_this_frame() == 0)
/*TODO*///	{
/*TODO*///		if (showfpstemp)
/*TODO*///		{
/*TODO*///			showfpstemp--;
/*TODO*///			if (showfps == 0 && showfpstemp == 0)
/*TODO*///				schedule_full_refresh();
/*TODO*///		}


		if (input_ui_pressed(IPT_UI_SHOW_FPS) != 0)
		{
			if (showfpstemp != 0)
			{
				showfpstemp = 0;
				schedule_full_refresh();
			}
			else
			{
				showfps ^= 1;
				if (showfps == 0)
					schedule_full_refresh();
			}
		}

                long curr;
                
/*TODO*///		/* now wait until it's time to update the screen */
/*TODO*///		if (throttle)
/*TODO*///		{
/*TODO*///			profiler_mark(PROFILER_IDLE);
/*TODO*///			if (video_sync)
/*TODO*///			{
/*TODO*///				static TICKER last;
                                
/*TODO*///
/*TODO*///
				do
				{
/*TODO*///					vsync();
/*TODO*///					curr = ticker();
                                    curr = uclock();

                                } while ((throttle != 0) && (curr - last < (frameskip + 1) * 1000000000 / Machine.drv.frames_per_second));
                                //while (TICKS_PER_SEC / (curr - last) > video_fps * 11 /10);
/*TODO*///
				last = curr;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				TICKER target;

				/* wait for video sync but use normal throttling */
/*TODO*///				if (wait_vsync != 0)
/*TODO*///                                  vsync();

/*TODO*///				curr = ticker();


/*TODO*///				if (already_synced == 0)
/*TODO*///				{
				/* wait only if the audio update hasn't synced us already */

/*TODO*///					target = this_frame_base +
/*TODO*///							frameskip_counter * TICKS_PER_SEC/video_fps;

/*TODO*///					if (curr - target < 0)
/*TODO*///					{
/*TODO*///						do
/*TODO*///						{
/*TODO*///							curr = ticker();
/*TODO*///                                                    curr = uclock();
/*TODO*///						} while (curr - target < 0);
/*TODO*///					}
/*TODO*///				}

/*TODO*///			}
/*TODO*///			profiler_mark(PROFILER_END);
/*TODO*///		}
/*TODO*///		else curr = ticker();


		/* for the FPS average calculation */
/*TODO*///		if (++frames_displayed == FRAMES_TO_SKIP)
/*TODO*///			start_time = curr;
/*TODO*///		else
/*TODO*///			end_time = curr;


/*TODO*///		if (frameskip_counter == 0)
/*TODO*///		{
/*TODO*///			int divdr;


/*TODO*///			divdr = (int) (video_fps * (curr - prev_measure) / (100 * FRAMESKIP_LEVELS));
/*TODO*///			speed = (int) ((TICKS_PER_SEC + divdr/2) / divdr);

/*TODO*///			prev_measure = curr;
/*TODO*///		}

/*TODO*///		prev = curr;

/*TODO*///		vfcount += waittable[frameskip][frameskip_counter];
/*TODO*///		if (vfcount >= video_fps)
/*TODO*///		{
/*TODO*///			extern int vector_updates; /* avgdvg_go_w()'s per Mame frame, should be 1 */
/*TODO*///
/*TODO*///
/*TODO*///			vfcount = 0;
/*TODO*///			vups = vector_updates;
/*TODO*///			vector_updates = 0;
/*TODO*///		}
/*TODO*///
//		if (!show_debugger && (showfps || showfpstemp))
                if (showfps != 0 || showfpstemp != 0)
		{
			int fps;
			String buf;
			int divdr;


			divdr = 100 * FRAMESKIP_LEVELS;
			fps = (video_fps * (FRAMESKIP_LEVELS - frameskip) * speed + (divdr / 2)) / divdr;
			buf = sprintf("%s%2d%4d%%%4d/%d fps", autoframeskip != 0 ? "auto" : "fskp", frameskip, speed, fps, (int) (Machine.drv.frames_per_second + 0.5));
			ui_text(Machine.scrbitmap, buf, Machine.uiwidth - buf.length() * Machine.uifontwidth, 0);
			if (vector_game != 0)
			{
				sprintf(buf," %d vector updates",vups);
				ui_text(Machine.scrbitmap, buf, Machine.uiwidth - buf.length() * Machine.uifontwidth, Machine.uifontheight);
			}
		}

		if (Machine.scrbitmap.depth == 8)
		{
			if (dirty_bright!=0)
			{
				dirty_bright = 0;
				for (int i = 0;i < screen_colors;i++)
				{
					float rate = (float) (brightness * brightness_paused_adjust * pow(i / 255.0, 1 / osd_gamma_correction) / 100);
					//bright_lookup[i] = (int) (63 * rate + 0.5);
                                        bright_lookup[i] = (int) (255 * rate + 0.5);
				}
			}
			if (dirtypalette != 0)
			{
				dirtypalette = 0;

			/*TODO*///	if (show_debugger)
			/*TODO*///	{
			/*TODO*///		for (i = 0;i < DEBUGGER_TOTAL_COLORS;i++)
			/*TODO*///		{
			/*TODO*///			RGB adjusted_palette;
			/*TODO*///			adjusted_palette.r = dbg_palette[3*i+0] >> 2;
			/*TODO*///			adjusted_palette.g = dbg_palette[3*i+1] >> 2;
			/*TODO*///			adjusted_palette.b = dbg_palette[3*i+2] >> 2;
			/*TODO*///			set_color(i,&adjusted_palette);
			/*TODO*///		}
			/*TODO*///	}
			/*TODO*///	else
/*TODO*///				{
					for (int i = 0;i < screen_colors;i++)
					{
						if (dirtycolor[i] != 0)
						{
							RGB adjusted_palette=new RGB();

							dirtycolor[i] = 0;

							adjusted_palette.r = current_palette.read(3*i+0);
							adjusted_palette.g = current_palette.read(3*i+1);
							adjusted_palette.b = current_palette.read(3*i+2);
							if (i != Machine.uifont.colortable.read(1))	/* don't adjust the user interface text */
							{
								adjusted_palette.r = bright_lookup[adjusted_palette.r];
								adjusted_palette.g = bright_lookup[adjusted_palette.g];
								adjusted_palette.b = bright_lookup[adjusted_palette.b];
							}
							else
							{
								adjusted_palette.r >>= 2;
								adjusted_palette.g >>= 2;
								adjusted_palette.b >>= 2;
							}
							set_color(i,adjusted_palette);
						}
					}
				}
			}
/*TODO*///		}
		else if (Machine.scrbitmap.depth == 15 || Machine.scrbitmap.depth == 16)
                {
			if (dirty_bright!=0)
			{
				dirty_bright = 0;
				for (int i = 0;i < screen_colors;i++)
				{
					float rate = (float) (brightness * brightness_paused_adjust * pow(i / 255.0, 1 / osd_gamma_correction) / 100);
					bright_lookup[i] = (int) (255 * rate + 0.5);
				}
			}
			if (dirtypalette != 0)
			{
				/*TODO*///if (use_dirty != 0) init_dirty(1);	/* have to redraw the whole screen */

				dirtypalette = 0;

				/*TODO*///if (show_debugger)
				/*TODO*///{
				/*TODO*///	for (i = 0;i < DEBUGGER_TOTAL_COLORS;i++)
				/*TODO*///	{
				/*TODO*///		int r,g,b;
/*TODO*///
				/*TODO*///		r = dbg_palette[3*i+0];
				/*TODO*///		g = dbg_palette[3*i+1];
				/*TODO*///		b = dbg_palette[3*i+2];
				/*TODO*///		palette_16bit_lookup[i] = makecol(r,g,b) * 0x10001;
				/*TODO*///	}
				/*TODO*///}
				/*TODO*///else
/*TODO*///				{
					for (int i = 0; i < screen_colors; i++) {
                                            if (dirtycolor[i] != 0) {
                                                int r, g, b;

                                                dirtycolor[i] = 0;

                                                r = current_palette.read(3 * i + 0);
                                                g = current_palette.read(3 * i + 1);
                                                b = current_palette.read(3 * i + 2);
                                                //if (i != Machine.uifont.colortable.read(1) && i != Machine.uifont.colortable.read(0)) /* don't adjust the user interface text */ {
                                                //    r = bright_lookup[r];
                                                //    g = bright_lookup[g];
                                                //    b = bright_lookup[b];
                                                //}
                                                palette_16bit_lookup.write(i,  (char)(makecol(r, g, b)));// * 0x10001);
                                                RGB p = new RGB();
                                                p.r = r;
                                                p.g = g;
                                                p.b = b;
                                                set_color(i, p);
                                            }
					}
				}
/*TODO*///			}
		}

/*TODO*///		if (show_debugger)
/*TODO*///		{
/*TODO*///			update_screen_debugger(bitmap);
/*TODO*///        }
/*TODO*///		else
/*TODO*///		{
/*TODO*///			/* copy the bitmap to screen memory */
/*TODO*///			profiler_mark(PROFILER_BLIT);
/*TODO*///			update_screen(game_bitmap);
/*TODO*///			profiler_mark(PROFILER_END);
/*TODO*///		}
/*TODO*///
/*TODO*///		/* see if we need to give the card enough time to draw both odd/even fields of the interlaced display
/*TODO*///			(req. for 15.75KHz Arcade Monitor Modes */
/*TODO*///		interlace_sync();
/*TODO*///
/*TODO*///
/*TODO*///		if (use_dirty) init_dirty(0);
/*TODO*///

		if ((throttle!=0) && (autoframeskip!=0) && frameskip_counter == 0)
		{
/*TODO*///			static int frameskipadjust;
			int adjspeed;

			/* adjust speed to video refresh rate if vsync is on */
			adjspeed = speed * video_fps / vsync_frame_rate;

			if (adjspeed >= 100)
			{
				frameskipadjust++;
				if (frameskipadjust >= 3)
				{
					frameskipadjust = 0;
					if (frameskip > 0) frameskip--;
				}
			}
			else
			{
				if (adjspeed < 80)
					frameskipadjust -= (90 - adjspeed) / 5;
				else
				{
					/* don't push frameskip too far if we are close to 100% speed */
					if (frameskip < 8)
						frameskipadjust--;
				}

				while (frameskipadjust <= -2)
				{
					frameskipadjust += 2;
					if (frameskip < FRAMESKIP_LEVELS-1) frameskip++;
				}
			}
		}
/*TODO*///	}

/*TODO*///	/* Check for PGUP, PGDN and pan screen */
/*TODO*///	pan_display();

/*TODO*///	if (input_ui_pressed(IPT_UI_FRAMESKIP_INC) != 0)
/*TODO*///	{
/*TODO*///		if (autoframeskip != 0)
/*TODO*///		{
/*TODO*///			autoframeskip = 0;
/*TODO*///			frameskip = 0;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			if (frameskip == FRAMESKIP_LEVELS-1)
/*TODO*///			{
/*TODO*///				frameskip = 0;
/*TODO*///				autoframeskip = 1;
/*TODO*///			}
/*TODO*///			else
/*TODO*///				frameskip++;
/*TODO*///		}

/*TODO*///		if (showfps == 0)
/*TODO*///			showfpstemp = 2*video_fps;

		/* reset the frame counter every time the frameskip key is pressed, so */
		/* we'll measure the average FPS on a consistent status. */
/*TODO*///		frames_displayed = 0;
/*TODO*///	}

	/*TODO*///if (input_ui_pressed(IPT_UI_FRAMESKIP_DEC) != 0)
	/*TODO*///{
	/*TODO*///	if (autoframeskip != 0)
	/*TODO*///	{
	/*TODO*///		autoframeskip = 0;
	/*TODO*///		frameskip = FRAMESKIP_LEVELS-1;
	/*TODO*///	}
	/*TODO*///	else
	/*TODO*///	{
	/*TODO*///		if (frameskip == 0)
	/*TODO*///			autoframeskip = 1;
	/*TODO*///		else
	/*TODO*///			frameskip--;
	/*TODO*///	}

	/*TODO*///	if (showfps == 0)
	/*TODO*///		showfpstemp = 2*video_fps;

		/* reset the frame counter every time the frameskip key is pressed, so */
		/* we'll measure the average FPS on a consistent status. */
	/*TODO*///	frames_displayed = 0;
	/*TODO*///}

	/*TODO*///if (input_ui_pressed(IPT_UI_THROTTLE) != 0)
	/*TODO*///{
	/*TODO*///	throttle ^= 1;

		/* reset the frame counter every time the throttle key is pressed, so */
		/* we'll measure the average FPS on a consistent status. */
	/*TODO*///	frames_displayed = 0;
	/*TODO*///}


	frameskip_counter = (frameskip_counter + 1) % FRAMESKIP_LEVELS;

/*TODO*///	poll_joysticks();

         blitscreen_dirty1_vga();
         update_audio();
         
    }



/*TODO*///void osd_set_gamma(float _gamma)
/*TODO*///{
/*TODO*///	int i;
/*TODO*///
/*TODO*///	osd_gamma_correction = _gamma;
/*TODO*///
/*TODO*///	for (i = 0;i < screen_colors;i++)
/*TODO*///		dirtycolor[i] = 1;
/*TODO*///	dirtypalette = 1;
/*TODO*///	dirty_bright = 1;
/*TODO*///}
/*TODO*///
/*TODO*///float osd_get_gamma(void)
/*TODO*///{
/*TODO*///	return osd_gamma_correction;
/*TODO*///}

    /* brightess = percentage 0-100% */
    public static void osd_set_brightness(int _brightness)
    {
            int i;

            brightness = _brightness;

            for (i = 0;i < screen_colors;i++)
                    dirtycolor[i] = 1;
            dirtypalette = 1;
            dirty_bright = 1;
    }

    public static int osd_get_brightness()
    {
            return brightness;
    }


    public static void osd_save_snapshot(mame_bitmap bitmap)
    {
            /*TODO*///save_screen_snapshot(bitmap);
    }

    public static void osd_pause(int paused)
    {
            int i;

            if (paused != 0) brightness_paused_adjust = 0.65f;
            else brightness_paused_adjust = 1.0f;

            for (i = 0;i < screen_colors;i++)
                    dirtycolor[i] = 1;
            dirtypalette = 1;
            dirty_bright = 1;
    }

/*TODO*///Register *make_scanline_mode(Register *inreg,int entries)
/*TODO*///{
/*TODO*///	static Register outreg[32];
/*TODO*///	int maxscan,maxscanout;
/*TODO*///	int overflow,overflowout;
/*TODO*///	int ytotalin,ytotalout;
/*TODO*///	int ydispin,ydispout;
/*TODO*///	int vrsin,vrsout,vreout,vblksout,vblkeout;
/*TODO*////* first - check's it not already a 'non doubled' line mode */
/*TODO*///	maxscan = inreg[MAXIMUM_SCANLINE_INDEX].value;
/*TODO*///	if ((maxscan & 1) == 0)
/*TODO*///	/* it is, so just return the array as is */
/*TODO*///  		return inreg;
/*TODO*////* copy across our standard display array */
/*TODO*///	memcpy (&outreg, inreg, entries * sizeof(Register));
/*TODO*////* keep hold of the overflow register - as we'll need to refer to it a lot */
/*TODO*///	overflow = inreg[OVERFLOW_INDEX].value;
/*TODO*////* set a large line compare value  - as we won't be doing any split window scrolling etc.*/
/*TODO*///	maxscanout = 0x40;
/*TODO*////* half all the y values */
/*TODO*////* total */
/*TODO*///	ytotalin = inreg[V_TOTAL_INDEX].value;
/*TODO*///	ytotalin |= ((overflow & 1)<<0x08) | ((overflow & 0x20)<<0x04);
/*TODO*///    ytotalout = ytotalin >> 1;
/*TODO*////* display enable end */
/*TODO*///	ydispin = inreg[13].value | ((overflow & 0x02)<< 0x07) | ((overflow & 0x040) << 0x03);
/*TODO*///	ydispin ++;
/*TODO*///	ydispout = ydispin >> 1;
/*TODO*///	ydispout --;
/*TODO*///	overflowout = ((ydispout & 0x100) >> 0x07) | ((ydispout && 0x200) >> 0x03);
/*TODO*///	outreg[V_END_INDEX].value = (ydispout & 0xff);
/*TODO*////* avoid top over scan */
/*TODO*///	if ((ytotalin - ydispin) < 40 && !center_y)
/*TODO*///	{
/*TODO*///  		vrsout = ydispout;
/*TODO*///		/* give ourselves a scanline cushion */
/*TODO*///		ytotalout += 2;
/*TODO*///	}
/*TODO*///  	else
/*TODO*///	{
/*TODO*////* vertical retrace start */
/*TODO*///		vrsin = inreg[V_RETRACE_START_INDEX].value | ((overflow & 0x04)<<0x06) | ((overflow & 0x80)<<0x02);
/*TODO*///		vrsout = vrsin >> 1;
/*TODO*///	}
/*TODO*////* check it's legal */
/*TODO*///	if (vrsout < ydispout)
/*TODO*///		vrsout = ydispout;
/*TODO*////*update our output overflow */
/*TODO*///	overflowout |= (((vrsout & 0x100) >> 0x06) | ((vrsout & 0x200) >> 0x02));
/*TODO*///	outreg[V_RETRACE_START_INDEX].value = (vrsout & 0xff);
/*TODO*////* vertical retrace end */
/*TODO*///	vreout = vrsout + 2;
/*TODO*////* make sure the retrace fits into our adjusted display size */
/*TODO*///	if (vreout > (ytotalout - 9))
/*TODO*///		ytotalout = vreout + 9;
/*TODO*////* write out the vertical retrace end */
/*TODO*///	outreg[V_RETRACE_END_INDEX].value &= ~0x0f;
/*TODO*///	outreg[V_RETRACE_END_INDEX].value |= (vreout & 0x0f);
/*TODO*////* vertical blanking start */
/*TODO*///	vblksout = ydispout + 1;
/*TODO*////* check it's legal */
/*TODO*///	if(vblksout > vreout)
/*TODO*///		vblksout = vreout;
/*TODO*////* save the overflow value */
/*TODO*///	overflowout |= ((vblksout & 0x100) >> 0x05);
/*TODO*///	maxscanout |= ((vblksout & 0x200) >> 0x04);
/*TODO*////* write the v blank value out */
/*TODO*///	outreg[V_BLANKING_START_INDEX].value = (vblksout & 0xff);
/*TODO*////* vertical blanking end */
/*TODO*///	vblkeout = vreout + 1;
/*TODO*////* make sure the blanking fits into our adjusted display size */
/*TODO*///	if (vblkeout > (ytotalout - 9))
/*TODO*///		ytotalout = vblkeout + 9;
/*TODO*////* write out the vertical blanking total */
/*TODO*///	outreg[V_BLANKING_END_INDEX].value = (vblkeout & 0xff);
/*TODO*////* update our output overflow */
/*TODO*///	overflowout |= ((ytotalout & 0x100) >> 0x08) | ((ytotalout & 0x200) >> 0x04);
/*TODO*////* write out the new vertical total */
/*TODO*///	outreg[V_TOTAL_INDEX].value = (ytotalout & 0xff);
/*TODO*///
/*TODO*////* write out our over flows */
/*TODO*///	outreg[OVERFLOW_INDEX].value = overflowout;
/*TODO*////* finally the max scan line */
/*TODO*///	outreg[MAXIMUM_SCANLINE_INDEX].value = maxscanout;
/*TODO*////* and we're done */
/*TODO*///	return outreg;
/*TODO*///
/*TODO*///}
/*TODO*///
/*TODO*///void center_mode(Register *pReg)
/*TODO*///{
/*TODO*///	int center;
/*TODO*///	int hrt_start, hrt_end, hrt, hblnk_start, hblnk_end;
/*TODO*///	int vrt_start, vrt_end, vert_total, vert_display, vblnk_start, vrt, vblnk_end;
/*TODO*////* check for empty array */
/*TODO*///	if (!pReg)
/*TODO*///		return;
/*TODO*////* vertical retrace width */
/*TODO*///	vrt = 2;
/*TODO*////* check the clock speed, to work out the retrace width */
/*TODO*///	if (pReg[CLOCK_INDEX].value == 0xe7)
/*TODO*///		hrt = 11;
/*TODO*///	else
/*TODO*///		hrt = 10;
/*TODO*////* our center x tweak value */
/*TODO*///	center = center_x;
/*TODO*////* check for double width scanline rather than half clock (15.75kHz modes) */
/*TODO*///	if( pReg[H_TOTAL_INDEX].value > 0x96)
/*TODO*///	{
/*TODO*///		center<<=1;
/*TODO*///		hrt<<=1;
/*TODO*///	}
/*TODO*////* set the hz retrace */
/*TODO*///	hrt_start = pReg[H_RETRACE_START_INDEX].value;
/*TODO*///	hrt_start += center;
/*TODO*////* make sure it's legal */
/*TODO*///	if (hrt_start <= pReg[H_DISPLAY_INDEX].value)
/*TODO*///		hrt_start = pReg[H_DISPLAY_INDEX].value + 1;
/*TODO*///	pReg[H_RETRACE_START_INDEX].value = hrt_start;
/*TODO*////* set hz retrace end */
/*TODO*///	hrt_end = hrt_start + hrt;
/*TODO*////* make sure it's legal */
/*TODO*///	if( hrt_end > pReg[H_TOTAL_INDEX].value)
/*TODO*///		hrt_end = pReg[H_TOTAL_INDEX].value;
/*TODO*///
/*TODO*////* set the hz blanking */
/*TODO*///	hblnk_start = pReg[H_DISPLAY_INDEX].value + 1;
/*TODO*////* make sure it's legal */
/*TODO*///	if (hblnk_start > hrt_start)
/*TODO*///		hblnk_start = pReg[H_RETRACE_START_INDEX].value;
/*TODO*///
/*TODO*///	pReg[H_BLANKING_START_INDEX].value = hblnk_start;
/*TODO*////* the horizontal blanking end */
/*TODO*///	hblnk_end = hrt_end + 2;
/*TODO*////* make sure it's legal */
/*TODO*///	if( hblnk_end > pReg[H_TOTAL_INDEX].value)
/*TODO*///		hblnk_end = pReg[H_TOTAL_INDEX].value;
/*TODO*////* write horizontal blanking - include 7th test bit (always 1) */
/*TODO*///	pReg[H_BLANKING_END_INDEX].value = (hblnk_end & 0x1f) | 0x80;
/*TODO*////* include the 5th bit of the horizontal blanking in the horizontal retrace reg. */
/*TODO*///	hrt_end = ((hrt_end & 0x1f) | ((hblnk_end & 0x20) << 2));
/*TODO*///	pReg[H_RETRACE_END_INDEX].value = hrt_end;
/*TODO*///
/*TODO*///
/*TODO*////* get the vt retrace */
/*TODO*///	vrt_start = pReg[V_RETRACE_START_INDEX].value | ((pReg[OVERFLOW_INDEX].value & 0x04) << 6) |
/*TODO*///				((pReg[OVERFLOW_INDEX].value & 0x80) << 2);
/*TODO*///
/*TODO*////* set the new retrace start */
/*TODO*///	vrt_start += center_y;
/*TODO*////* check it's legal, get the display line count */
/*TODO*///	vert_display = (pReg[V_END_INDEX].value | ((pReg[OVERFLOW_INDEX].value & 0x02) << 7) |
/*TODO*///				((pReg[OVERFLOW_INDEX].value & 0x40) << 3)) + 1;
/*TODO*///
/*TODO*///	if (vrt_start < vert_display)
/*TODO*///		vrt_start = vert_display;
/*TODO*///
/*TODO*////* and get the vertical line count */
/*TODO*///	vert_total = pReg[V_TOTAL_INDEX].value | ((pReg[OVERFLOW_INDEX].value & 0x01) << 8) |
/*TODO*///				((pReg[OVERFLOW_INDEX].value & 0x20) << 4);
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///	pReg[V_RETRACE_START_INDEX].value = (vrt_start & 0xff);
/*TODO*///	pReg[OVERFLOW_INDEX].value &= ~0x84;
/*TODO*///	pReg[OVERFLOW_INDEX].value |= ((vrt_start & 0x100) >> 6);
/*TODO*///	pReg[OVERFLOW_INDEX].value |= ((vrt_start & 0x200) >> 2);
/*TODO*///	vrt_end = vrt_start + vrt;
/*TODO*///
/*TODO*///
/*TODO*///	if (vrt_end > vert_total)
/*TODO*///		vrt_end = vert_total;
/*TODO*///
/*TODO*////* write retrace end, include CRT protection and IRQ2 bits */
/*TODO*///	pReg[V_RETRACE_END_INDEX].value = (vrt_end  & 0x0f) | 0x80 | 0x20;
/*TODO*///
/*TODO*////* get the start of vt blanking */
/*TODO*///	vblnk_start = vert_display + 1;
/*TODO*////* check it's legal */
/*TODO*///	if (vblnk_start > vrt_start)
/*TODO*///		vblnk_start = vrt_start;
/*TODO*////* and the end */
/*TODO*///	vblnk_end = vrt_end + 2;
/*TODO*////* check it's legal */
/*TODO*///	if (vblnk_end > vert_total)
/*TODO*///		vblnk_end = vert_total;
/*TODO*////* set vblank start */
/*TODO*///	pReg[V_BLANKING_START_INDEX].value = (vblnk_start & 0xff);
/*TODO*////* write out any overflows */
/*TODO*///	pReg[OVERFLOW_INDEX].value &= ~0x08;
/*TODO*///	pReg[OVERFLOW_INDEX].value |= ((vblnk_start & 0x100) >> 5);
/*TODO*///	pReg[MAXIMUM_SCANLINE_INDEX].value &= ~0x20;
/*TODO*///	pReg[MAXIMUM_SCANLINE_INDEX].value |= ((vblnk_start &0x200) >> 4);
/*TODO*////* set the vblank end */
/*TODO*///	pReg[V_BLANKING_END_INDEX].value = (vblnk_end & 0xff);
/*TODO*///}
/*TODO*///
/*TODO*///    
    
    public static int makecol(int r, int g, int b) {//makecol16 from allegro src
        /*  Color c = new Color(r, g, b);
        int cl = c.getRGB();
        //Red shift from 24 to 16, masking but 5 MSBs 
        char o = (char) ((cl >> 8) & 0xf800);

        /* Green shift from 16 to 11, masking 6 MSBs */
 /*    o |= (char) ((cl >> 5) & 0x07e0);

        /* Blue shift from 8 to 5, masking 5 MSBs */
 /*   o |= (char) ((cl >> 3) & 0x001f);
        System.out.println((int) o);*/
        int o = ((r >> 3) << 11) | ((g >> 2) << 5) | ((b >> 3) << 0);
        return o;
        //return (((r >> 3) << 11)
        //        | ((g >> 2) << 11)
        //      | ((b >> 3) << 11));
    }
    
    static int onlyone = 0;
    
    public static void tempCreation() {
        /*part of the old arcadeflex_old emulator probably need refactoring */
        Dimension localDimension = Toolkit.getDefaultToolkit().getScreenSize();
        if (onlyone == 0) {
            //kill loading window
            osdepend.dlprogress.setVisible(false);
            screen = new software_gfx(settings.version + " (based on mame v" + build_version + ")");
            screen.pack();
            //screen.setSize((scanlines==1),gfx_width,gfx_height);//this???
            //screen.setSize((scanlines==1),width,height);//this???
            
            screen.setSize((scanlines == 0), Machine.scrbitmap.width, Machine.scrbitmap.height);
            screen.setBackground(Color.black);
            screen.start();
            screen.run();
            screen.setLocation((int) ((localDimension.getWidth() - screen.getWidth()) / 2.0D), (int) ((localDimension.getHeight() - screen.getHeight()) / 2.0D));
            screen.setVisible(true);
            screen.setResizable((scanlines == 1));

            screen.addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent evt) {
                    screen.readkey = KeyEvent.VK_ESCAPE;
                    screen.key[KeyEvent.VK_ESCAPE] = true;
                    osd_refresh();
                    if (screen != null) {
                        screen.key[KeyEvent.VK_ESCAPE] = false;
                    }
                }
            });

            screen.addComponentListener(new ComponentAdapter() {

                public void componentResized(ComponentEvent evt) {
                    screen.resizeVideo();
                }
            });

            screen.addKeyListener(screen);
            screen.setFocusTraversalKeysEnabled(false);
            screen.requestFocus();
            onlyone = 1;//big hack!!
        }
    }

    public static void osd_refresh() {
        /*function from old arcadeflex_old */

        if (screen != null) {
            screen.blit();
        }
        try {
            Thread.sleep(100L);
        } catch (InterruptedException localInterruptedException) {
        }
    }
}
