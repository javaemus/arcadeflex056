/**
 * Ported to 0.56
 */
package mame056;

import static common.ptr.*;
import common.subArrays.IntArray;
import mame056.drawgfxH.*;
import mame056.driverH.GameDriver;
import mame056.driverH.MachineDriver;
import mame056.commonH.GameSamples;
import mame056.commonH.mame_bitmap;
import mame056.inptportH.InputPort;
import static common.libc.cstdio.*;

public class mameH {

    public static final int MAX_GFX_ELEMENTS = 32;
    public static final int MAX_MEMORY_REGIONS = 32;

    public static class RegionInfo {

        public UBytePtr base;
        public int length;
        public int/*UINT32*/ type;
        public int/*UINT32*/ flags;
    }

    public static class RunningMachine {

        public RegionInfo[] memory_region = new RegionInfo[MAX_MEMORY_REGIONS];
        public GfxElement gfx[] = new GfxElement[MAX_GFX_ELEMENTS];/* graphic sets (chars, sprites) */
        public mame_bitmap scrbitmap;/* bitmap to draw into */
        public rectangle visible_area;
        public /*UINT32 * */ int[] pens;/* remapped palette pen numbers. When you write */
 /* directly to a bitmap, never use absolute values, */
 /* use this array to get the pen number. For example, */
 /* if you want to use color #6 in the palette, use */
 /* pens[6] instead of just 6. */
        public /*UINT16 * */ char[] game_colortable;/* lookup table used to map gfx pen numbers to color numbers */
        public /*UINT32 * */ IntArray remapped_colortable;/* the above, already remapped through Machine->pens */
        public GameDriver gamedrv;/* contains the definition of the game machine */
        public MachineDriver drv;/* same as gamedrv->drv */
        public int color_depth;/* video color depth: 8, 16, 15 or 32 */
        public int sample_rate;/* the digital audio sample rate; 0 if sound is disabled. */
 /* This is set to a default value, or a value specified by */
 /* the user; osd_init() is allowed to change it to the actual */
 /* sample rate supported by the audio card. */
        public GameSamples samples;/* samples loaded from disk */
        public InputPort[] input_ports;/* the input ports definition from the driver */
 /* is copied here and modified (load settings from disk, */
 /* remove cheat commands, and so on) */
        public InputPort[] input_ports_default;/* original input_ports without modifications */
        public int orientation;/* see #defines in driver.h */
        public GfxElement uifont;/* font used by the user interface */
        public int uifontwidth, uifontheight;
        public int uixmin, uiymin;
        public int uiwidth, uiheight;
        public int ui_orientation;
        public rectangle absolute_visible_area;/* as passed to osd_set_visible_area() */

    }

    /* The host platform should fill these fields with the preferences specified in the GUI */
 /* or on the commandline. */
    public static class GameOptions {

        public FILE record;
        public FILE playback;
        public FILE language_file;/* LBO 042400 */

        public int mame_debug;
        public int cheat;
        public int gui_host;

        public int samplerate;
        public int use_samples;
        public int use_emulated_ym3812;
        public int use_filter;

        public int color_depth;/* 8 or 16, any other value means auto */
        public int vector_width;/* requested width for vector games; 0 means default (640) */
        public int vector_height;/* requested height for vector games; 0 means default (480) */
        public int debug_width;/* initial size of the debug_bitmap */
        public int debug_height;
        public int norotate;
        public int ror;
        public int rol;
        public int flipx;
        public int flipy;
        public int beam;
        public float vector_flicker;
        public int translucency;
        public int antialias;
        public int use_artwork;
        public int savegame;

    }
}
