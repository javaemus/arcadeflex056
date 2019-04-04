/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcadeflex037b7;

import static arcadeflex037b7.blit.*;
import static common.libc.cstring.memset;
import static common.libc.cstdio.*;
import static common.ptr.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import static mame056.driverH.ORIENTATION_SWAP_XY;
import static mame056.driverH.VIDEO_DUAL_MONITOR;
import static mame056.driverH.VIDEO_PIXEL_ASPECT_RATIO_1_2;
import static mame056.driverH.VIDEO_PIXEL_ASPECT_RATIO_2_1;
import static mame056.driverH.VIDEO_PIXEL_ASPECT_RATIO_MASK;
import static mame056.driverH.VIDEO_TYPE_VECTOR;
import static mame056.version.build_version;
import static common.libc.cstdio.*;
import arcadeflex036.osdepend;
import static arcadeflex036.osdepend.logerror;
import arcadeflex036.software_gfx;
import static arcadeflex036.sound.update_audio;
import static arcadeflex036.ticker.TICKS_PER_SEC;
import static mame056.inptportH.IPT_UI_SHOW_FPS;
import static mame056.input.*;
import arcadeflex056.settings;
import static common.libc.ctime.*;

import static mame056.commonH.*;
import static mame056.mame.Machine;
import static mame056.mame.schedule_full_refresh;
import static mame056.usrintrf.set_ui_visarea;
import static mame056.usrintrf.ui_text;

import static arcadeflex056.video.*;

/**
 *
 * @author chusogar
 */
public class video {

    public static final int BACKGROUND = 0;
    
    static int framecount = 0;
    //TEMP HACK used old arcadeflex_old's sync. should be rewriten to new format
    static final int MEMORY = 10;
    static long[] prev1 = new long[10];
    static int clock_counter;
    static int speed = 100;
    public static long start_time, end_time;
    /* to calculate fps average on exit */

    public static final int FRAMES_TO_SKIP = 20;

    public static final int MAX_X_MULTIPLY = 4;
    public static final int MAX_Y_MULTIPLY = 3;
    public static final int MAX_X_MULTIPLY16 = 4;
    public static final int MAX_Y_MULTIPLY16 = 2;

    /* Create a bitmap. Also calls osd_clearbitmap() to appropriately initialize */
 /* it to the background color. */
 /* VERY IMPORTANT: the function must allocate also a "safety area" 16 pixels wide all */
 /* around the bitmap. This is required because, for performance reasons, some graphic */
 /* routines don't clip at boundaries of the bitmap. */
    public static int safety = 16;

    public static void osd_update_video_and_audio(mame_bitmap bitmap, int led) {
        osd_update_video_and_audio(bitmap);
    }

    public static void osd_update_video_and_audio(mame_bitmap bitmap) {
        if (++framecount > frameskip) {
            framecount = 0;

            if (input_ui_pressed(IPT_UI_SHOW_FPS) != 0) {
                if (showfpstemp != 0) {
                    showfpstemp = 0;
                    schedule_full_refresh();
                } else {
                    showfps ^= 1;
                    if (showfps == 0) {
                        schedule_full_refresh();
                    }
                }
            }

            long curr;
            /* now wait until it's time to trigger the interrupt */
            do {

                curr = uclock();
            } while ((throttle != 0) && (curr - prev1[clock_counter] < (frameskip + 1) * 1000000000 / Machine.drv.frames_per_second));
            //while (throttle != 0 && video_sync == 0 && (curr - prev[i]) < (frameskip+1) * UCLOCKS_PER_SEC/drv.frames_per_second);
            if (showfps != 0 || showfpstemp != 0) {
                int fps;
                String buf;
                int divdr;

                divdr = 100 * FRAMESKIP_LEVELS;
                fps = ((int) Machine.drv.frames_per_second * (FRAMESKIP_LEVELS - frameskip) * speed + (divdr / 2)) / divdr;
                buf = sprintf("%s%2d%4d%%%4d/%d fps", autoframeskip != 0 ? "auto" : "fskp", frameskip, speed, fps, (int) (Machine.drv.frames_per_second + 0.5));
                ui_text(Machine.scrbitmap, buf, Machine.uiwidth - buf.length() * Machine.uifontwidth, 0);
                if (vector_game != 0) {
                    sprintf(buf, " %d vector updates", vups);
                    ui_text(Machine.scrbitmap, buf, Machine.uiwidth - buf.length() * Machine.uifontwidth, Machine.uifontheight);
                }
            }
            if (Machine.scrbitmap.depth == 8) {
                if (dirty_bright != 0) {
                    dirty_bright = 0;
                    for (int i = 0; i < 256; i++) {
                        float rate = (float) (brightness * brightness_paused_adjust * Math.pow(i / 255.0, 1 / osd_gamma_correction) / 100);
                        /*bright_lookup[i] = 63 * rate + 0.5;*/
                        bright_lookup[i] = (int) (255 * rate + 0.5);

                    }
                }
                if (dirtypalette != 0) {
                    dirtypalette = 0;
                    for (int i = 0; i < screen_colors; i++) {
                        if (dirtycolor[i] != 0) {
                            RGB adjusted_palette = new RGB();

                            dirtycolor[i] = 0;

                            adjusted_palette.r = current_palette.read(3 * i + 0);
                            adjusted_palette.g = current_palette.read(3 * i + 1);
                            adjusted_palette.b = current_palette.read(3 * i + 2);
                            if (i != Machine.uifont.colortable.read(1)) /* don't adjust the user interface text */ {
                                adjusted_palette.r = (char) bright_lookup[adjusted_palette.r];
                                adjusted_palette.g = (char) bright_lookup[adjusted_palette.g];
                                adjusted_palette.b = (char) bright_lookup[adjusted_palette.b];
                            } else {

                                /*TODO*///							adjusted_palette.r >>= 2;
                                /*TODO*///							adjusted_palette.g >>= 2;
                                /*TODO*///							adjusted_palette.b >>= 2;
                            }
                            set_color(i, adjusted_palette);
                        }
                    }
                }
            } else {
                if (dirty_bright != 0) {
                    dirty_bright = 0;
                    for (int i = 0; i < 256; i++) {
                        float rate = (float) (brightness * brightness_paused_adjust * Math.pow(i / 255.0, 1 / osd_gamma_correction) / 100);
                        bright_lookup[i] = (int) (255 * rate + 0.5);
                    }
                }
                if (dirtypalette != 0) {
                    // if (use_dirty != 0) init_dirty(1);	/* have to redraw the whole screen */

                    dirtypalette = 0;
                    for (int i = 0; i < screen_colors; i++) {
                        if (dirtycolor[i] != 0) {
                            int r, g, b;

                            dirtycolor[i] = 0;

                            r = current_palette.read(3 * i + 0);
                            g = current_palette.read(3 * i + 1);
                            b = current_palette.read(3 * i + 2);
                            if (i != Machine.uifont.colortable.read(1)) /* don't adjust the user interface text */ {
                                r = bright_lookup[r];
                                g = bright_lookup[g];
                                b = bright_lookup[b];
                            }
                            palette_16bit_lookup.write(i,  (char)(makecol(r, g, b)));// * 0x10001);
                            RGB p = new RGB();
                            p.r = r;
                            p.g = g;
                            p.b = b;
                            set_color(i, p);
                        }
                    }
                }
            }
            /*TODO*///		else
            /*TODO*///		{
            /*TODO*///			if (dirty_bright)
            /*TODO*///			{
            /*TODO*///				dirty_bright = 0;
            /*TODO*///				for (i = 0;i < 256;i++)
            /*TODO*///				{
            /*TODO*///					float rate = brightness * brightness_paused_adjust * pow(i / 255.0, 1 / osd_gamma_correction) / 100;
            /*TODO*///					bright_lookup[i] = 255 * rate + 0.5;
            /*TODO*///				}
            /*TODO*///			}
            /*TODO*///			if (dirtypalette)
            /*TODO*///			{
            /*TODO*///				if (use_dirty) init_dirty(1);	/* have to redraw the whole screen */
            /*TODO*///
            /*TODO*///				dirtypalette = 0;
            /*TODO*///				for (i = 0;i < screen_colors;i++)
            /*TODO*///				{
            /*TODO*///					if (dirtycolor[i])
            /*TODO*///					{
            /*TODO*///						int r,g,b;
            /*TODO*///
            /*TODO*///						dirtycolor[i] = 0;
            /*TODO*///
            /*TODO*///						r = current_palette[3*i+0];
            /*TODO*///						g = current_palette[3*i+1];
            /*TODO*///						b = current_palette[3*i+2];
            /*TODO*///						if (i != Machine->uifont->colortable[1])	/* don't adjust the user interface text */
            /*TODO*///						{
            /*TODO*///							r = bright_lookup[r];
            /*TODO*///							g = bright_lookup[g];
            /*TODO*///							b = bright_lookup[b];
            /*TODO*///						}
            /*TODO*///						palette_16bit_lookup[i] = makecol(r,g,b) * 0x10001;
            /*TODO*///					}
            /*TODO*///				}
            /*TODO*///			}
            /*TODO*///		}
            blitscreen_dirty1_vga();
            update_audio();

            clock_counter = (clock_counter + 1) % MEMORY;
            if ((curr - prev1[clock_counter]) != 0) {
                long divdr = (int) Machine.drv.frames_per_second * (curr - prev1[clock_counter]) / (100L * MEMORY);

                speed = (int) ((UCLOCKS_PER_SEC * (frameskip + 1) + divdr / 2L) / divdr);
            }

            prev1[clock_counter] = curr;
        }
    }

   
    
    

    
    
    
  
}
