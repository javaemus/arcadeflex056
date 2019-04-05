/**
 * Ported to 0.56
 */
package mame056;

import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import static common.libc.expressions.*;
import static common.subArrays.*;
import static common.ptr.*;

import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame037b7.cheat.*;
import static mame056.sndintrf.*;
import static mame056.driverH.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.inputH.*;
import static mame056.input.*;
import static mame056.inptportH.*;
import static mame056.mame.*;
import static mame056.mameH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrf.*;
import static mame056.ui_text.*;
import static mame056.ui_textH.*;
import static mame056.usrintrfH.*;
import static mame056.inptport.*;
import static mame056.version.*;
import static mame056.driver.*;
import static arcadeflex056.video.*;

//to refactor
import static arcadeflex036.sound.*;

public class usrintrf {

    static int setup_selected;
    static int osd_selected;
    static int jukebox_selected;
    static int single_step;
    static int trueorientation;
    static int orientation_count;

    public static void switch_ui_orientation() {
        if (orientation_count == 0) {
            trueorientation = Machine.orientation;
            Machine.orientation = Machine.ui_orientation;
            set_pixel_functions();
        }

        orientation_count++;
    }

    public static void switch_true_orientation() {
        orientation_count--;

        if (orientation_count == 0) {
            Machine.orientation = trueorientation;
            set_pixel_functions();
        }
    }

    public static void set_ui_visarea(int xmin, int ymin, int xmax, int ymax) {
        int temp, w, h;

        /* special case for vectors */
        if ((Machine.drv.video_attributes & VIDEO_TYPE_VECTOR) != 0) {
            if ((Machine.ui_orientation & ORIENTATION_SWAP_XY) != 0) {
                temp = xmin;
                xmin = ymin;
                ymin = temp;
                temp = xmax;
                xmax = ymax;
                ymax = temp;
            }
        } else {
            if ((Machine.orientation & ORIENTATION_SWAP_XY) != 0) {
                w = Machine.drv.screen_height;
                h = Machine.drv.screen_width;
            } else {
                w = Machine.drv.screen_width;
                h = Machine.drv.screen_height;
            }

            if ((Machine.ui_orientation & ORIENTATION_FLIP_X) != 0) {
                temp = w - xmin - 1;
                xmin = w - xmax - 1;
                xmax = temp;
            }

            if ((Machine.ui_orientation & ORIENTATION_FLIP_Y) != 0) {
                temp = h - ymin - 1;
                ymin = h - ymax - 1;
                ymax = temp;
            }

            if ((Machine.ui_orientation & ORIENTATION_SWAP_XY) != 0) {
                temp = xmin;
                xmin = ymin;
                ymin = temp;
                temp = xmax;
                xmax = ymax;
                ymax = temp;
            }

        }
        Machine.uiwidth = xmax - xmin + 1;
        Machine.uiheight = ymax - ymin + 1;
        Machine.uixmin = xmin;
        Machine.uiymin = ymin;
    }

    public static GfxElement builduifont() {
        char fontdata6x8[]
                = {
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x7c, 0x80, 0x98, 0x90, 0x80, 0xbc, 0x80, 0x7c, 0xf8, 0x04, 0x64, 0x44, 0x04, 0xf4, 0x04, 0xf8,
                    0x7c, 0x80, 0x98, 0x88, 0x80, 0xbc, 0x80, 0x7c, 0xf8, 0x04, 0x64, 0x24, 0x04, 0xf4, 0x04, 0xf8,
                    0x7c, 0x80, 0x88, 0x98, 0x80, 0xbc, 0x80, 0x7c, 0xf8, 0x04, 0x24, 0x64, 0x04, 0xf4, 0x04, 0xf8,
                    0x7c, 0x80, 0x90, 0x98, 0x80, 0xbc, 0x80, 0x7c, 0xf8, 0x04, 0x44, 0x64, 0x04, 0xf4, 0x04, 0xf8,
                    0x30, 0x48, 0x84, 0xb4, 0xb4, 0x84, 0x48, 0x30, 0x30, 0x48, 0x84, 0x84, 0x84, 0x84, 0x48, 0x30,
                    0x00, 0xfc, 0x84, 0x8c, 0xd4, 0xa4, 0xfc, 0x00, 0x00, 0xfc, 0x84, 0x84, 0x84, 0x84, 0xfc, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x30, 0x68, 0x78, 0x78, 0x30, 0x00, 0x00,
                    0x80, 0xc0, 0xe0, 0xf0, 0xe0, 0xc0, 0x80, 0x00, 0x04, 0x0c, 0x1c, 0x3c, 0x1c, 0x0c, 0x04, 0x00,
                    0x20, 0x70, 0xf8, 0x20, 0x20, 0xf8, 0x70, 0x20, 0x48, 0x48, 0x48, 0x48, 0x48, 0x00, 0x48, 0x00,
                    0x00, 0x00, 0x30, 0x68, 0x78, 0x30, 0x00, 0x00, 0x00, 0x30, 0x68, 0x78, 0x78, 0x30, 0x00, 0x00,
                    0x70, 0xd8, 0xe8, 0xe8, 0xf8, 0xf8, 0x70, 0x00, 0x1c, 0x7c, 0x74, 0x44, 0x44, 0x4c, 0xcc, 0xc0,
                    0x20, 0x70, 0xf8, 0x70, 0x70, 0x70, 0x70, 0x00, 0x70, 0x70, 0x70, 0x70, 0xf8, 0x70, 0x20, 0x00,
                    0x00, 0x10, 0xf8, 0xfc, 0xf8, 0x10, 0x00, 0x00, 0x00, 0x20, 0x7c, 0xfc, 0x7c, 0x20, 0x00, 0x00,
                    0xb0, 0x54, 0xb8, 0xb8, 0x54, 0xb0, 0x00, 0x00, 0x00, 0x28, 0x6c, 0xfc, 0x6c, 0x28, 0x00, 0x00,
                    0x00, 0x30, 0x30, 0x78, 0x78, 0xfc, 0x00, 0x00, 0xfc, 0x78, 0x78, 0x30, 0x30, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00, 0x20, 0x00,
                    0x50, 0x50, 0x50, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x50, 0xf8, 0x50, 0xf8, 0x50, 0x00, 0x00,
                    0x20, 0x70, 0xc0, 0x70, 0x18, 0xf0, 0x20, 0x00, 0x40, 0xa4, 0x48, 0x10, 0x20, 0x48, 0x94, 0x08,
                    0x60, 0x90, 0xa0, 0x40, 0xa8, 0x90, 0x68, 0x00, 0x10, 0x20, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x20, 0x40, 0x40, 0x40, 0x40, 0x40, 0x20, 0x00, 0x10, 0x08, 0x08, 0x08, 0x08, 0x08, 0x10, 0x00,
                    0x20, 0xa8, 0x70, 0xf8, 0x70, 0xa8, 0x20, 0x00, 0x00, 0x20, 0x20, 0xf8, 0x20, 0x20, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x30, 0x30, 0x60, 0x00, 0x00, 0x00, 0xf8, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x30, 0x30, 0x00, 0x00, 0x08, 0x10, 0x20, 0x40, 0x80, 0x00, 0x00,
                    0x70, 0x88, 0x88, 0x88, 0x88, 0x88, 0x70, 0x00, 0x10, 0x30, 0x10, 0x10, 0x10, 0x10, 0x10, 0x00,
                    0x70, 0x88, 0x08, 0x10, 0x20, 0x40, 0xf8, 0x00, 0x70, 0x88, 0x08, 0x30, 0x08, 0x88, 0x70, 0x00,
                    0x10, 0x30, 0x50, 0x90, 0xf8, 0x10, 0x10, 0x00, 0xf8, 0x80, 0xf0, 0x08, 0x08, 0x88, 0x70, 0x00,
                    0x70, 0x80, 0xf0, 0x88, 0x88, 0x88, 0x70, 0x00, 0xf8, 0x08, 0x08, 0x10, 0x20, 0x20, 0x20, 0x00,
                    0x70, 0x88, 0x88, 0x70, 0x88, 0x88, 0x70, 0x00, 0x70, 0x88, 0x88, 0x88, 0x78, 0x08, 0x70, 0x00,
                    0x00, 0x00, 0x30, 0x30, 0x00, 0x30, 0x30, 0x00, 0x00, 0x00, 0x30, 0x30, 0x00, 0x30, 0x30, 0x60,
                    0x10, 0x20, 0x40, 0x80, 0x40, 0x20, 0x10, 0x00, 0x00, 0x00, 0xf8, 0x00, 0xf8, 0x00, 0x00, 0x00,
                    0x40, 0x20, 0x10, 0x08, 0x10, 0x20, 0x40, 0x00, 0x70, 0x88, 0x08, 0x10, 0x20, 0x00, 0x20, 0x00,
                    0x30, 0x48, 0x94, 0xa4, 0xa4, 0x94, 0x48, 0x30, 0x70, 0x88, 0x88, 0xf8, 0x88, 0x88, 0x88, 0x00,
                    0xf0, 0x88, 0x88, 0xf0, 0x88, 0x88, 0xf0, 0x00, 0x70, 0x88, 0x80, 0x80, 0x80, 0x88, 0x70, 0x00,
                    0xf0, 0x88, 0x88, 0x88, 0x88, 0x88, 0xf0, 0x00, 0xf8, 0x80, 0x80, 0xf0, 0x80, 0x80, 0xf8, 0x00,
                    0xf8, 0x80, 0x80, 0xf0, 0x80, 0x80, 0x80, 0x00, 0x70, 0x88, 0x80, 0x98, 0x88, 0x88, 0x70, 0x00,
                    0x88, 0x88, 0x88, 0xf8, 0x88, 0x88, 0x88, 0x00, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00,
                    0x08, 0x08, 0x08, 0x08, 0x88, 0x88, 0x70, 0x00, 0x88, 0x90, 0xa0, 0xc0, 0xa0, 0x90, 0x88, 0x00,
                    0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0xf8, 0x00, 0x88, 0xd8, 0xa8, 0x88, 0x88, 0x88, 0x88, 0x00,
                    0x88, 0xc8, 0xa8, 0x98, 0x88, 0x88, 0x88, 0x00, 0x70, 0x88, 0x88, 0x88, 0x88, 0x88, 0x70, 0x00,
                    0xf0, 0x88, 0x88, 0xf0, 0x80, 0x80, 0x80, 0x00, 0x70, 0x88, 0x88, 0x88, 0x88, 0x88, 0x70, 0x08,
                    0xf0, 0x88, 0x88, 0xf0, 0x88, 0x88, 0x88, 0x00, 0x70, 0x88, 0x80, 0x70, 0x08, 0x88, 0x70, 0x00,
                    0xf8, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00, 0x88, 0x88, 0x88, 0x88, 0x88, 0x88, 0x70, 0x00,
                    0x88, 0x88, 0x88, 0x88, 0x88, 0x50, 0x20, 0x00, 0x88, 0x88, 0x88, 0x88, 0xa8, 0xd8, 0x88, 0x00,
                    0x88, 0x50, 0x20, 0x20, 0x20, 0x50, 0x88, 0x00, 0x88, 0x88, 0x88, 0x50, 0x20, 0x20, 0x20, 0x00,
                    0xf8, 0x08, 0x10, 0x20, 0x40, 0x80, 0xf8, 0x00, 0x30, 0x20, 0x20, 0x20, 0x20, 0x20, 0x30, 0x00,
                    0x40, 0x40, 0x20, 0x20, 0x10, 0x10, 0x08, 0x08, 0x30, 0x10, 0x10, 0x10, 0x10, 0x10, 0x30, 0x00,
                    0x20, 0x50, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xfc,
                    0x40, 0x20, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x70, 0x08, 0x78, 0x88, 0x78, 0x00,
                    0x80, 0x80, 0xf0, 0x88, 0x88, 0x88, 0xf0, 0x00, 0x00, 0x00, 0x70, 0x88, 0x80, 0x80, 0x78, 0x00,
                    0x08, 0x08, 0x78, 0x88, 0x88, 0x88, 0x78, 0x00, 0x00, 0x00, 0x70, 0x88, 0xf8, 0x80, 0x78, 0x00,
                    0x18, 0x20, 0x70, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x78, 0x88, 0x88, 0x78, 0x08, 0x70,
                    0x80, 0x80, 0xf0, 0x88, 0x88, 0x88, 0x88, 0x00, 0x20, 0x00, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00,
                    0x20, 0x00, 0x20, 0x20, 0x20, 0x20, 0x20, 0xc0, 0x80, 0x80, 0x90, 0xa0, 0xe0, 0x90, 0x88, 0x00,
                    0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0xf0, 0xa8, 0xa8, 0xa8, 0xa8, 0x00,
                    0x00, 0x00, 0xb0, 0xc8, 0x88, 0x88, 0x88, 0x00, 0x00, 0x00, 0x70, 0x88, 0x88, 0x88, 0x70, 0x00,
                    0x00, 0x00, 0xf0, 0x88, 0x88, 0xf0, 0x80, 0x80, 0x00, 0x00, 0x78, 0x88, 0x88, 0x78, 0x08, 0x08,
                    0x00, 0x00, 0xb0, 0xc8, 0x80, 0x80, 0x80, 0x00, 0x00, 0x00, 0x78, 0x80, 0x70, 0x08, 0xf0, 0x00,
                    0x20, 0x20, 0x70, 0x20, 0x20, 0x20, 0x18, 0x00, 0x00, 0x00, 0x88, 0x88, 0x88, 0x98, 0x68, 0x00,
                    0x00, 0x00, 0x88, 0x88, 0x88, 0x50, 0x20, 0x00, 0x00, 0x00, 0xa8, 0xa8, 0xa8, 0xa8, 0x50, 0x00,
                    0x00, 0x00, 0x88, 0x50, 0x20, 0x50, 0x88, 0x00, 0x00, 0x00, 0x88, 0x88, 0x88, 0x78, 0x08, 0x70,
                    0x00, 0x00, 0xf8, 0x10, 0x20, 0x40, 0xf8, 0x00, 0x08, 0x10, 0x10, 0x20, 0x10, 0x10, 0x08, 0x00,
                    0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x40, 0x20, 0x20, 0x10, 0x20, 0x20, 0x40, 0x00,
                    0x00, 0x68, 0xb0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x50, 0x20, 0x50, 0xa8, 0x50, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x20, 0x40, 0x0C, 0x10, 0x38, 0x10, 0x20, 0x20, 0xC0, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x28, 0x28, 0x50, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xA8, 0x00,
                    0x70, 0xA8, 0xF8, 0x20, 0x20, 0x20, 0x20, 0x00, 0x70, 0xA8, 0xF8, 0x20, 0x20, 0xF8, 0xA8, 0x70,
                    0x20, 0x50, 0x88, 0x00, 0x00, 0x00, 0x00, 0x00, 0x44, 0xA8, 0x50, 0x20, 0x68, 0xD4, 0x28, 0x00,
                    0x88, 0x70, 0x88, 0x60, 0x30, 0x88, 0x70, 0x00, 0x00, 0x10, 0x20, 0x40, 0x20, 0x10, 0x00, 0x00,
                    0x78, 0xA0, 0xA0, 0xB0, 0xA0, 0xA0, 0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x10, 0x10, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x28, 0x50, 0x50, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x28, 0x28, 0x50, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x30, 0x78, 0x78, 0x30, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFC, 0x00, 0x00, 0x00, 0x00,
                    0x68, 0xB0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xF4, 0x5C, 0x54, 0x54, 0x00, 0x00, 0x00, 0x00,
                    0x88, 0x70, 0x78, 0x80, 0x70, 0x08, 0xF0, 0x00, 0x00, 0x40, 0x20, 0x10, 0x20, 0x40, 0x00, 0x00,
                    0x00, 0x00, 0x70, 0xA8, 0xB8, 0xA0, 0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x50, 0x88, 0x88, 0x50, 0x20, 0x20, 0x20, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x00, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00,
                    0x00, 0x20, 0x70, 0xA8, 0xA0, 0xA8, 0x70, 0x20, 0x30, 0x48, 0x40, 0xE0, 0x40, 0x48, 0xF0, 0x00,
                    0x00, 0x48, 0x30, 0x48, 0x48, 0x30, 0x48, 0x00, 0x88, 0x88, 0x50, 0xF8, 0x20, 0xF8, 0x20, 0x00,
                    0x20, 0x20, 0x20, 0x00, 0x20, 0x20, 0x20, 0x00, 0x78, 0x80, 0x70, 0x88, 0x70, 0x08, 0xF0, 0x00,
                    0xD8, 0xD8, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x30, 0x48, 0x94, 0xA4, 0xA4, 0x94, 0x48, 0x30,
                    0x60, 0x10, 0x70, 0x90, 0x70, 0x00, 0x00, 0x00, 0x00, 0x28, 0x50, 0xA0, 0x50, 0x28, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0xF8, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x78, 0x00, 0x00, 0x00, 0x00,
                    0x30, 0x48, 0xB4, 0xB4, 0xA4, 0xB4, 0x48, 0x30, 0x7C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x60, 0x90, 0x90, 0x60, 0x00, 0x00, 0x00, 0x00, 0x20, 0x20, 0xF8, 0x20, 0x20, 0x00, 0xF8, 0x00,
                    0x60, 0x90, 0x20, 0x40, 0xF0, 0x00, 0x00, 0x00, 0x60, 0x90, 0x20, 0x90, 0x60, 0x00, 0x00, 0x00,
                    0x10, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x88, 0x88, 0x88, 0xC8, 0xB0, 0x80,
                    0x78, 0xD0, 0xD0, 0xD0, 0x50, 0x50, 0x50, 0x00, 0x00, 0x00, 0x00, 0x30, 0x30, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x20, 0x00, 0x20, 0x60, 0x20, 0x20, 0x70, 0x00, 0x00, 0x00,
                    0x20, 0x50, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xA0, 0x50, 0x28, 0x50, 0xA0, 0x00, 0x00,
                    0x40, 0x48, 0x50, 0x28, 0x58, 0xA8, 0x38, 0x08, 0x40, 0x48, 0x50, 0x28, 0x44, 0x98, 0x20, 0x3C,
                    0xC0, 0x28, 0xD0, 0x28, 0xD8, 0xA8, 0x38, 0x08, 0x20, 0x00, 0x20, 0x40, 0x80, 0x88, 0x70, 0x00,
                    0x40, 0x20, 0x70, 0x88, 0xF8, 0x88, 0x88, 0x00, 0x10, 0x20, 0x70, 0x88, 0xF8, 0x88, 0x88, 0x00,
                    0x70, 0x00, 0x70, 0x88, 0xF8, 0x88, 0x88, 0x00, 0x68, 0xB0, 0x70, 0x88, 0xF8, 0x88, 0x88, 0x00,
                    0x50, 0x00, 0x70, 0x88, 0xF8, 0x88, 0x88, 0x00, 0x20, 0x50, 0x70, 0x88, 0xF8, 0x88, 0x88, 0x00,
                    0x78, 0xA0, 0xA0, 0xF0, 0xA0, 0xA0, 0xB8, 0x00, 0x70, 0x88, 0x80, 0x80, 0x88, 0x70, 0x08, 0x70,
                    0x40, 0x20, 0xF8, 0x80, 0xF0, 0x80, 0xF8, 0x00, 0x10, 0x20, 0xF8, 0x80, 0xF0, 0x80, 0xF8, 0x00,
                    0x70, 0x00, 0xF8, 0x80, 0xF0, 0x80, 0xF8, 0x00, 0x50, 0x00, 0xF8, 0x80, 0xF0, 0x80, 0xF8, 0x00,
                    0x40, 0x20, 0x70, 0x20, 0x20, 0x20, 0x70, 0x00, 0x10, 0x20, 0x70, 0x20, 0x20, 0x20, 0x70, 0x00,
                    0x70, 0x00, 0x70, 0x20, 0x20, 0x20, 0x70, 0x00, 0x50, 0x00, 0x70, 0x20, 0x20, 0x20, 0x70, 0x00,
                    0x70, 0x48, 0x48, 0xE8, 0x48, 0x48, 0x70, 0x00, 0x68, 0xB0, 0x88, 0xC8, 0xA8, 0x98, 0x88, 0x00,
                    0x40, 0x20, 0x70, 0x88, 0x88, 0x88, 0x70, 0x00, 0x10, 0x20, 0x70, 0x88, 0x88, 0x88, 0x70, 0x00,
                    0x70, 0x00, 0x70, 0x88, 0x88, 0x88, 0x70, 0x00, 0x68, 0xB0, 0x70, 0x88, 0x88, 0x88, 0x70, 0x00,
                    0x50, 0x00, 0x70, 0x88, 0x88, 0x88, 0x70, 0x00, 0x00, 0x88, 0x50, 0x20, 0x50, 0x88, 0x00, 0x00,
                    0x00, 0x74, 0x88, 0x90, 0xA8, 0x48, 0xB0, 0x00, 0x40, 0x20, 0x88, 0x88, 0x88, 0x88, 0x70, 0x00,
                    0x10, 0x20, 0x88, 0x88, 0x88, 0x88, 0x70, 0x00, 0x70, 0x00, 0x88, 0x88, 0x88, 0x88, 0x70, 0x00,
                    0x50, 0x00, 0x88, 0x88, 0x88, 0x88, 0x70, 0x00, 0x10, 0xA8, 0x88, 0x50, 0x20, 0x20, 0x20, 0x00,
                    0x00, 0x80, 0xF0, 0x88, 0x88, 0xF0, 0x80, 0x80, 0x60, 0x90, 0x90, 0xB0, 0x88, 0x88, 0xB0, 0x00,
                    0x40, 0x20, 0x70, 0x08, 0x78, 0x88, 0x78, 0x00, 0x10, 0x20, 0x70, 0x08, 0x78, 0x88, 0x78, 0x00,
                    0x70, 0x00, 0x70, 0x08, 0x78, 0x88, 0x78, 0x00, 0x68, 0xB0, 0x70, 0x08, 0x78, 0x88, 0x78, 0x00,
                    0x50, 0x00, 0x70, 0x08, 0x78, 0x88, 0x78, 0x00, 0x20, 0x50, 0x70, 0x08, 0x78, 0x88, 0x78, 0x00,
                    0x00, 0x00, 0xF0, 0x28, 0x78, 0xA0, 0x78, 0x00, 0x00, 0x00, 0x70, 0x88, 0x80, 0x78, 0x08, 0x70,
                    0x40, 0x20, 0x70, 0x88, 0xF8, 0x80, 0x70, 0x00, 0x10, 0x20, 0x70, 0x88, 0xF8, 0x80, 0x70, 0x00,
                    0x70, 0x00, 0x70, 0x88, 0xF8, 0x80, 0x70, 0x00, 0x50, 0x00, 0x70, 0x88, 0xF8, 0x80, 0x70, 0x00,
                    0x40, 0x20, 0x00, 0x60, 0x20, 0x20, 0x70, 0x00, 0x10, 0x20, 0x00, 0x60, 0x20, 0x20, 0x70, 0x00,
                    0x20, 0x50, 0x00, 0x60, 0x20, 0x20, 0x70, 0x00, 0x50, 0x00, 0x00, 0x60, 0x20, 0x20, 0x70, 0x00,
                    0x50, 0x60, 0x10, 0x78, 0x88, 0x88, 0x70, 0x00, 0x68, 0xB0, 0x00, 0xF0, 0x88, 0x88, 0x88, 0x00,
                    0x40, 0x20, 0x00, 0x70, 0x88, 0x88, 0x70, 0x00, 0x10, 0x20, 0x00, 0x70, 0x88, 0x88, 0x70, 0x00,
                    0x20, 0x50, 0x00, 0x70, 0x88, 0x88, 0x70, 0x00, 0x68, 0xB0, 0x00, 0x70, 0x88, 0x88, 0x70, 0x00,
                    0x00, 0x50, 0x00, 0x70, 0x88, 0x88, 0x70, 0x00, 0x00, 0x20, 0x00, 0xF8, 0x00, 0x20, 0x00, 0x00,
                    0x00, 0x00, 0x68, 0x90, 0xA8, 0x48, 0xB0, 0x00, 0x40, 0x20, 0x88, 0x88, 0x88, 0x98, 0x68, 0x00,
                    0x10, 0x20, 0x88, 0x88, 0x88, 0x98, 0x68, 0x00, 0x70, 0x00, 0x88, 0x88, 0x88, 0x98, 0x68, 0x00,
                    0x50, 0x00, 0x88, 0x88, 0x88, 0x98, 0x68, 0x00, 0x10, 0x20, 0x88, 0x88, 0x88, 0x78, 0x08, 0x70,
                    0x80, 0xF0, 0x88, 0x88, 0xF0, 0x80, 0x80, 0x80, 0x50, 0x00, 0x88, 0x88, 0x88, 0x78, 0x08, 0x70
                };
        GfxLayout fontlayout6x8 = new GfxLayout(
                6, 8, /* 6*8 characters */
                256, /* 256 characters */
                1, /* 1 bit per pixel */
                new int[]{0},
                new int[]{0, 1, 2, 3, 4, 5, 6, 7}, /* straightforward layout */
                new int[]{0 * 8, 1 * 8, 2 * 8, 3 * 8, 4 * 8, 5 * 8, 6 * 8, 7 * 8},
                8 * 8 /* every char takes 8 consecutive bytes */
        );
        GfxLayout fontlayout12x8 = new GfxLayout(
                12, 8, /* 12*8 characters */
                256, /* 256 characters */
                1, /* 1 bit per pixel */
                new int[]{0},
                new int[]{0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7}, /* straightforward layout */
                new int[]{0 * 8, 1 * 8, 2 * 8, 3 * 8, 4 * 8, 5 * 8, 6 * 8, 7 * 8},
                8 * 8 /* every char takes 8 consecutive bytes */
        );
        GfxLayout fontlayout6x16 = new GfxLayout(
                6, 16, /* 6*8 characters */
                256, /* 256 characters */
                1, /* 1 bit per pixel */
                new int[]{0},
                new int[]{0, 1, 2, 3, 4, 5, 6, 7}, /* straightforward layout */
                new int[]{0 * 8, 0 * 8, 1 * 8, 1 * 8, 2 * 8, 2 * 8, 3 * 8, 3 * 8, 4 * 8, 4 * 8, 5 * 8, 5 * 8, 6 * 8, 6 * 8, 7 * 8, 7 * 8},
                8 * 8 /* every char takes 8 consecutive bytes */
        );
        GfxLayout fontlayout12x16 = new GfxLayout(
                12, 16, /* 12*16 characters */
                256, /* 256 characters */
                1, /* 1 bit per pixel */
                new int[]{0},
                new int[]{0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7}, /* straightforward layout */
                new int[]{0 * 8, 0 * 8, 1 * 8, 1 * 8, 2 * 8, 2 * 8, 3 * 8, 3 * 8, 4 * 8, 4 * 8, 5 * 8, 5 * 8, 6 * 8, 6 * 8, 7 * 8, 7 * 8},
                8 * 8 /* every char takes 8 consecutive bytes */
        );

        GfxElement font;
        int[] colortable = new int[2 * 2];/* ASG 980209 */


        switch_ui_orientation();

        if ((Machine.drv.video_attributes & VIDEO_PIXEL_ASPECT_RATIO_MASK)
                == VIDEO_PIXEL_ASPECT_RATIO_1_2) {
            if ((Machine.gamedrv.flags & ORIENTATION_SWAP_XY) != 0) {
                font = decodegfx(new UBytePtr(fontdata6x8), fontlayout6x16);
                Machine.uifontwidth = 6;
                Machine.uifontheight = 16;
            } else {
                font = decodegfx(new UBytePtr(fontdata6x8), fontlayout12x8);
                Machine.uifontwidth = 12;
                Machine.uifontheight = 8;
            }
        } else if (Machine.uiwidth >= 420 && Machine.uiheight >= 420) {
            font = decodegfx(new UBytePtr(fontdata6x8), fontlayout12x16);
            Machine.uifontwidth = 12;
            Machine.uifontheight = 16;
        } else {
            font = decodegfx(new UBytePtr(fontdata6x8), fontlayout6x8);
            Machine.uifontwidth = 6;
            Machine.uifontheight = 8;
        }

        if (font != null) {
            /* colortable will be set at run time */
            memset(colortable, 0, sizeof(colortable));
            font.colortable = new IntArray(colortable);
            font.total_colors = 2;
        }

        switch_true_orientation();

        return font;
    }

    static void erase_screen(mame_bitmap bitmap) {
        fillbitmap(bitmap, Machine.uifont.colortable.read(0), null);
        schedule_full_refresh();
    }

    /**
     * *************************************************************************
     *
     * Display text on the screen. If erase is 0, it superimposes the text on
     * the last frame displayed.
     *
     **************************************************************************
     */
    public static void displaytext(mame_bitmap bitmap, DisplayText[] dt) {
        switch_ui_orientation();

        osd_mark_dirty(Machine.uixmin, Machine.uiymin, Machine.uixmin + Machine.uiwidth - 1, Machine.uiymin + Machine.uiheight - 1);
        int _ptr = 0;
        while (dt[_ptr].text != null) {
            int x, y;
            int c;

            x = dt[_ptr].x;
            y = dt[_ptr].y;
            c = 0;//dt.text;
            while (c < dt[_ptr].text.length() && dt[_ptr].text.charAt(c) != '\0')//while (*c)
            {
                boolean wrapped = false;

                if (dt[_ptr].text.charAt(c) == '\n') {
                    x = dt[_ptr].x;
                    y += Machine.uifontheight + 1;
                    wrapped = true;
                } else if (dt[_ptr].text.charAt(c) == ' ') {
                    /* don't try to word wrap at the beginning of a line (this would cause */
 /* an endless loop if a word is longer than a line) */
                    if (x != dt[_ptr].x) {
                        int nextlen = 0;
                        int nc;//const char *nc;

                        nc = c + 1;
                        while (nc < dt[_ptr].text.length() && dt[_ptr].text.charAt(nc) != '\0' && dt[_ptr].text.charAt(nc) != ' ' && dt[_ptr].text.charAt(nc) != '\n')//while (*nc && *nc != ' ' && *nc != '\n')
                        {
                            nextlen += Machine.uifontwidth;
                            nc++;
                        }

                        /* word wrap */
                        if (x + Machine.uifontwidth + nextlen > Machine.uiwidth) {
                            x = dt[_ptr].x;
                            y += Machine.uifontheight + 1;
                            wrapped = true;
                        }
                    }
                }

                if (!wrapped) {
                    drawgfx(bitmap, Machine.uifont, dt[_ptr].text.charAt(c), dt[_ptr].color, 0, 0, x + Machine.uixmin, y + Machine.uiymin, null, TRANSPARENCY_NONE, 0);
                    x += Machine.uifontwidth;
                }

                c++;
            }
            _ptr++;
        }
        switch_true_orientation();

    }

    /* Writes messages on the screen. */
    public static void ui_text_ex(mame_bitmap bitmap, String buf_begin, int buf_end, int x, int y, int color) {
        switch_ui_orientation();

        for (int i = 0; i < buf_end; ++i) {
            drawgfx(bitmap, Machine.uifont, buf_begin.charAt(i), color, 0, 0,
                    x + Machine.uixmin,
                    y + Machine.uiymin, null, TRANSPARENCY_NONE, 0);
            x += Machine.uifontwidth;
        }

        switch_true_orientation();
    }

    /* Writes messages on the screen. */
    public static void ui_text(mame_bitmap bitmap, String buf, int x, int y) {
        ui_text_ex(bitmap, buf, buf.length(), x, y, UI_COLOR_NORMAL);
    }

    public static void ui_drawbox(mame_bitmap bitmap, int leftx, int topy, int width, int height) {
        int black, white;

        switch_ui_orientation();

        if (leftx < 0) {
            leftx = 0;
        }
        if (topy < 0) {
            topy = 0;
        }
        if (width > Machine.uiwidth) {
            width = Machine.uiwidth;
        }
        if (height > Machine.uiheight) {
            height = Machine.uiheight;
        }

        leftx += Machine.uixmin;
        topy += Machine.uiymin;

        black = Machine.uifont.colortable.read(0);
        white = Machine.uifont.colortable.read(1);

        plot_box.handler(bitmap, leftx, topy, width, 1, white);
        plot_box.handler(bitmap, leftx, topy + height - 1, width, 1, white);
        plot_box.handler(bitmap, leftx, topy, 1, height, white);
        plot_box.handler(bitmap, leftx + width - 1, topy, 1, height, white);
        plot_box.handler(bitmap, leftx + 1, topy + 1, width - 2, height - 2, black);

        switch_true_orientation();
    }

    public static void drawbar(mame_bitmap bitmap, int leftx, int topy, int width, int height, int percentage, int default_percentage) {
        int black, white;

        switch_ui_orientation();

        if (leftx < 0) {
            leftx = 0;
        }
        if (topy < 0) {
            topy = 0;
        }
        if (width > Machine.uiwidth) {
            width = Machine.uiwidth;
        }
        if (height > Machine.uiheight) {
            height = Machine.uiheight;
        }

        leftx += Machine.uixmin;
        topy += Machine.uiymin;

        black = Machine.uifont.colortable.read(0);
        white = Machine.uifont.colortable.read(1);

        plot_box.handler(bitmap, leftx + (width - 1) * default_percentage / 100, topy, 1, height / 8, white);

        plot_box.handler(bitmap, leftx, topy + height / 8, width, 1, white);

        plot_box.handler(bitmap, leftx, topy + height / 8, 1 + (width - 1) * percentage / 100, height - 2 * (height / 8), white);

        plot_box.handler(bitmap, leftx, topy + height - height / 8 - 1, width, 1, white);

        plot_box.handler(bitmap, leftx + (width - 1) * default_percentage / 100, topy + height - height / 8, 1, height / 8, white);

        switch_true_orientation();
    }

    /*TODO*///
/*TODO*////* Extract one line from a multiline buffer */
/*TODO*////* Return the characters number of the line, pbegin point to the start of the next line */
/*TODO*///static unsigned multiline_extract(const char** pbegin, const char* end, unsigned max)
/*TODO*///{
/*TODO*///	unsigned mac = 0;
/*TODO*///	const char* begin = *pbegin;
/*TODO*///	while (begin != end && mac < max)
/*TODO*///	{
/*TODO*///		if (*begin == '\n')
/*TODO*///		{
/*TODO*///			*pbegin = begin + 1; /* strip final space */
/*TODO*///			return mac;
/*TODO*///		}
/*TODO*///		else if (*begin == ' ')
/*TODO*///		{
/*TODO*///			const char* word_end = begin + 1;
/*TODO*///			while (word_end != end && *word_end != ' ' && *word_end != '\n')
/*TODO*///				++word_end;
/*TODO*///			if (mac + word_end - begin > max)
/*TODO*///			{
/*TODO*///				if (mac)
/*TODO*///				{
/*TODO*///					*pbegin = begin + 1;
/*TODO*///					return mac; /* strip final space */
/*TODO*///				} else {
/*TODO*///					*pbegin = begin + max;
/*TODO*///					return max;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			mac += word_end - begin;
/*TODO*///			begin = word_end;
/*TODO*///		} else {
/*TODO*///			++mac;
/*TODO*///			++begin;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	if (begin != end && (*begin == '\n' || *begin == ' '))
/*TODO*///		++begin;
/*TODO*///	*pbegin = begin;
/*TODO*///	return mac;
/*TODO*///}
/*TODO*///
/*TODO*////* Compute the output size of a multiline string */
/*TODO*///static void multiline_size(int* dx, int* dy, const char* begin, const char* end, unsigned max)
/*TODO*///{
/*TODO*///	unsigned rows = 0;
/*TODO*///	unsigned cols = 0;
/*TODO*///	while (begin != end)
/*TODO*///	{
/*TODO*///		unsigned len;
/*TODO*///		len = multiline_extract(&begin,end,max);
/*TODO*///		if (len > cols)
/*TODO*///			cols = len;
/*TODO*///		++rows;
/*TODO*///	}
/*TODO*///	*dx = cols * Machine->uifontwidth;
/*TODO*///	*dy = (rows-1) * 3*Machine->uifontheight/2 + Machine->uifontheight;
/*TODO*///}
/*TODO*///
/*TODO*////* Compute the output size of a multiline string with box */
/*TODO*///static void multilinebox_size(int* dx, int* dy, const char* begin, const char* end, unsigned max)
/*TODO*///{
/*TODO*///	multiline_size(dx,dy,begin,end,max);
/*TODO*///	*dx += Machine->uifontwidth;
/*TODO*///	*dy += Machine->uifontheight;
/*TODO*///}
/*TODO*///
/*TODO*////* Display a multiline string */
/*TODO*///static void ui_multitext_ex(struct mame_bitmap *bitmap, const char* begin, const char* end, unsigned max, int x, int y, int color)
/*TODO*///{
/*TODO*///	while (begin != end)
/*TODO*///	{
/*TODO*///		const char* line_begin = begin;
/*TODO*///		unsigned len = multiline_extract(&begin,end,max);
/*TODO*///		ui_text_ex(bitmap, line_begin, line_begin + len,x,y,color);
/*TODO*///		y += 3*Machine->uifontheight/2;
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*////* Display a multiline string with box */
/*TODO*///static void ui_multitextbox_ex(struct mame_bitmap *bitmap,const char* begin, const char* end, unsigned max, int x, int y, int dx, int dy, int color)
/*TODO*///{
/*TODO*///	ui_drawbox(bitmap,x,y,dx,dy);
/*TODO*///	x += Machine->uifontwidth/2;
/*TODO*///	y += Machine->uifontheight/2;
/*TODO*///	ui_multitext_ex(bitmap,begin,end,max,x,y,color);
/*TODO*///}
/*TODO*///
    public static void ui_displaymenu(mame_bitmap bitmap, String[] items, String[] subitems, char[] flag, int selected, int arrowize_subitem) {
        DisplayText[] dt = DisplayText.create(256);
        int curr_dt;
        String lefthilight = ui_getstring(UI_lefthilight);
        String righthilight = ui_getstring(UI_righthilight);
        String uparrow = ui_getstring(UI_uparrow);
        String downarrow = ui_getstring(UI_downarrow);
        String leftarrow = ui_getstring(UI_leftarrow);
        String rightarrow = ui_getstring(UI_rightarrow);
        int i, count, len, maxlen, highlen;
        int leftoffs, topoffs, visible, topitem;
        int selected_long;

        i = 0;
        maxlen = 0;
        highlen = Machine.uiwidth / Machine.uifontwidth;
        while (items[i] != null) {
            len = 3 + strlen(items[i]);
            if (subitems != null && subitems[i] != null) {
                len += 2 + strlen(subitems[i]);
            }
            if (len > maxlen && len <= highlen) {
                maxlen = len;
            }
            i++;
        }
        count = i;

        visible = Machine.uiheight / (3 * Machine.uifontheight / 2) - 1;
        topitem = 0;
        if (visible > count) {
            visible = count;
        } else {
            topitem = selected - visible / 2;
            if (topitem < 0) {
                topitem = 0;
            }
            if (topitem > count - visible) {
                topitem = count - visible;
            }
        }

        leftoffs = (Machine.uiwidth - maxlen * Machine.uifontwidth) / 2;
        topoffs = (Machine.uiheight - (3 * visible + 1) * Machine.uifontheight / 2) / 2;

        /* black background */
        ui_drawbox(bitmap, leftoffs, topoffs, maxlen * Machine.uifontwidth, (3 * visible + 1) * Machine.uifontheight / 2);

        selected_long = 0;
        curr_dt = 0;
        for (i = 0; i < visible; i++) {
            int item = i + topitem;

            if (i == 0 && item > 0) {
                dt[curr_dt].text = uparrow;
                dt[curr_dt].color = UI_COLOR_NORMAL;
                dt[curr_dt].x = (Machine.uiwidth - Machine.uifontwidth * strlen(uparrow)) / 2;
                dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
                curr_dt++;
            } else if (i == visible - 1 && item < count - 1) {
                dt[curr_dt].text = downarrow;
                dt[curr_dt].color = UI_COLOR_NORMAL;
                dt[curr_dt].x = (Machine.uiwidth - Machine.uifontwidth * strlen(downarrow)) / 2;
                dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
                curr_dt++;
            } else {
                if (subitems != null && subitems[item] != null) {
                    int sublen;
                    len = strlen(items[item]);
                    dt[curr_dt].text = items[item];
                    dt[curr_dt].color = UI_COLOR_NORMAL;
                    dt[curr_dt].x = leftoffs + 3 * Machine.uifontwidth / 2;
                    dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
                    curr_dt++;
                    sublen = strlen(subitems[item]);
                    if (sublen > maxlen - 5 - len) {
                        dt[curr_dt].text = "...";
                        sublen = strlen(dt[curr_dt].text);
                        if (item == selected) {
                            selected_long = 1;
                        }
                    } else {
                        dt[curr_dt].text = subitems[item];
                    }
                    /* If this item is flagged, draw it in inverse print */
                    dt[curr_dt].color = (flag != null && flag[item] != 0) ? UI_COLOR_INVERSE : UI_COLOR_NORMAL;
                    dt[curr_dt].x = leftoffs + Machine.uifontwidth * (maxlen - 1 - sublen) - Machine.uifontwidth / 2;
                    dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
                    curr_dt++;
                } else {
                    dt[curr_dt].text = items[item];
                    dt[curr_dt].color = UI_COLOR_NORMAL;
                    dt[curr_dt].x = (Machine.uiwidth - Machine.uifontwidth * strlen(items[item])) / 2;
                    dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
                    curr_dt++;
                }
            }
        }

        i = selected - topitem;
        if (subitems != null && subitems[selected] != null && arrowize_subitem != 0) {
            if ((arrowize_subitem & 1) != 0) {
                int sublen;
                len = strlen(items[selected]);

                dt[curr_dt].text = leftarrow;
                dt[curr_dt].color = UI_COLOR_NORMAL;

                sublen = strlen(subitems[selected]);
                if (sublen > maxlen - 5 - len) {
                    sublen = strlen("...");
                }

                dt[curr_dt].x = leftoffs + Machine.uifontwidth * (maxlen - 2 - sublen) - Machine.uifontwidth / 2 - 1;
                dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
                curr_dt++;
            }
            if ((arrowize_subitem & 2) != 0) {
                dt[curr_dt].text = rightarrow;
                dt[curr_dt].color = UI_COLOR_NORMAL;
                dt[curr_dt].x = leftoffs + Machine.uifontwidth * (maxlen - 1) - Machine.uifontwidth / 2;
                dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
                curr_dt++;
            }
        } else {
            dt[curr_dt].text = righthilight;
            dt[curr_dt].color = UI_COLOR_NORMAL;
            dt[curr_dt].x = leftoffs + Machine.uifontwidth * (maxlen - 1) - Machine.uifontwidth / 2;
            dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
            curr_dt++;
        }
        dt[curr_dt].text = lefthilight;
        dt[curr_dt].color = UI_COLOR_NORMAL;
        dt[curr_dt].x = leftoffs + Machine.uifontwidth / 2;
        dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
        curr_dt++;

        dt[curr_dt].text = null;
        /* terminate array */

        displaytext(bitmap, dt);

        if (selected_long != 0) {
            throw new UnsupportedOperationException("unimplemented");
            /*TODO*///		int long_dx;
/*TODO*///		int long_dy;
/*TODO*///		int long_x;
/*TODO*///		int long_y;
/*TODO*///		unsigned long_max;
/*TODO*///
/*TODO*///		long_max = (Machine->uiwidth / Machine->uifontwidth) - 2;
/*TODO*///		multilinebox_size(&long_dx,&long_dy,subitems[selected],subitems[selected] + strlen(subitems[selected]), long_max);
/*TODO*///
/*TODO*///		long_x = Machine->uiwidth - long_dx;
/*TODO*///		long_y = topoffs + (i+1) * 3*Machine->uifontheight/2;
/*TODO*///
/*TODO*///		/* if too low display up */
/*TODO*///		if (long_y + long_dy > Machine->uiheight)
/*TODO*///			long_y = topoffs + i * 3*Machine->uifontheight/2 - long_dy;
/*TODO*///
/*TODO*///		ui_multitextbox_ex(bitmap,subitems[selected],subitems[selected] + strlen(subitems[selected]), long_max, long_x,long_y,long_dx,long_dy, UI_COLOR_NORMAL);
        }
    }

    public static void ui_displaymessagewindow(mame_bitmap bitmap, String text) {
        DisplayText[] dt = DisplayText.create(256);
        int curr_dt;
        int c, c2;
        char[] textcopy = new char[2048];
        int i, len, maxlen, lines;
        int leftoffs, topoffs;
        int maxcols, maxrows;

        maxcols = (Machine.uiwidth / Machine.uifontwidth) - 1;
        maxrows = (2 * Machine.uiheight - Machine.uifontheight) / (3 * Machine.uifontheight);

        /* copy text, calculate max len, count lines, wrap long lines and crop height to fit */
        maxlen = 0;
        lines = 0;
        c = 0;//(char *)text;
        c2 = 0;//textcopy;
        while (c < text.length() && text.charAt(c) != '\0')//while (*c)
        {
            len = 0;
            while (c < text.length() && text.charAt(c) != '\0' && text.charAt(c) != '\n')//while (*c && *c != '\n')
            {
                textcopy[c2++] = text.charAt(c++);
                len++;
                if (len == maxcols && text.charAt(c) != '\n') {
                    /* attempt word wrap */
                    int csave = c, c2save = c2;
                    int lensave = len;

                    /* back up to last space or beginning of line */
                    while (text.charAt(c) != ' ' && text.charAt(c) != '\n' && c > 0)//while (*c != ' ' && *c != '\n' && c > text)
                    {
                        --c;
                        --c2;
                        --len;
                    }
                    /* if no space was found, hard wrap instead */
                    if (text.charAt(c) != ' ') {
                        c = csave;
                        c2 = c2save;
                        len = lensave;
                    } else {
                        c++;
                    }

                    textcopy[c2++] = '\n';
                    /* insert wrap */
                    break;
                }
            }
            if (c < text.length() && text.charAt(c) == '\n')//if (*c == '\n')
            {
                textcopy[c2++] = text.charAt(c++);
            }

            if (len > maxlen) {
                maxlen = len;
            }

            lines++;
            if (lines == maxrows) {
                break;
            }
        }
        textcopy[c2] = '\0';

        maxlen += 1;
        leftoffs = (Machine.uiwidth - Machine.uifontwidth * maxlen) / 2;
        if (leftoffs < 0) {
            leftoffs = 0;
        }
        topoffs = (Machine.uiheight - (3 * lines + 1) * Machine.uifontheight / 2) / 2;

        /* black background */
        ui_drawbox(bitmap, leftoffs, topoffs, maxlen * Machine.uifontwidth, (3 * lines + 1) * Machine.uifontheight / 2);

        curr_dt = 0;
        c = 0;//textcopy;
        i = 0;
        while (c < textcopy.length && textcopy[c] != '\0')//while (*c)
        {
            c2 = c;
            while (c < textcopy.length && textcopy[c] != '\0' && textcopy[c] != '\n')//while (*c && *c != '\n')
            {
                c++;
            }

            if (textcopy[c] == '\n') {
                textcopy[c] = '\0';
                c++;
            }
            if (textcopy[c2] == '\t') /* center text */ {
                c2++;
                dt[curr_dt].x = (Machine.uiwidth - Machine.uifontwidth * (c - c2)) / 2;
            } else {
                dt[curr_dt].x = leftoffs + Machine.uifontwidth / 2;
            }

            dt[curr_dt].text = new String(textcopy).substring(c2);//dt[curr_dt].text = c2;
            dt[curr_dt].color = UI_COLOR_NORMAL;
            dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
            curr_dt++;

            i++;
        }
        dt[curr_dt].text = null;
        /* terminate array */

        displaytext(bitmap, dt);
    }

    public static void showcharset(mame_bitmap bitmap) {
        int i;
        String buf = "";
        int bank, color, firstdrawn;
        int palpage;
        int changed;
        int total_colors = 0;
        IntArray colortable = null;

        bank = -2;
        color = 0;
        firstdrawn = 0;
        palpage = 0;

        changed = 1;

        do {
            int cpx, cpy, skip_chars;

            if (bank >= 0) {
                cpx = Machine.uiwidth / Machine.gfx[bank].width;
                cpy = (Machine.uiheight - Machine.uifontheight) / Machine.gfx[bank].height;
                skip_chars = cpx * cpy;
            } else {
                cpx = cpy = skip_chars = 0;

                if (bank == -2) /* palette */ {
                    total_colors = Machine.drv.total_colors;
                    colortable = new IntArray(Machine.pens);
                } else if (bank == -1) /* clut */ {
                    total_colors = Machine.drv.color_table_len;
                    colortable = Machine.remapped_colortable;
                }
            }

            if (changed != 0) {
                int lastdrawn = 0;

                erase_screen(bitmap);

                /* validity check after char bank change */
                if (bank >= 0) {
                    if (firstdrawn >= Machine.gfx[bank].total_elements) {
                        firstdrawn = Machine.gfx[bank].total_elements - skip_chars;
                        if (firstdrawn < 0) {
                            firstdrawn = 0;
                        }
                    }
                }

                switch_ui_orientation();

                if (bank >= 0) {
                    int flipx, flipy;

                    flipx = (Machine.orientation ^ trueorientation) & ORIENTATION_FLIP_X;
                    flipy = (Machine.orientation ^ trueorientation) & ORIENTATION_FLIP_Y;

                    if ((Machine.orientation & ORIENTATION_SWAP_XY) != 0) {
                        int t;
                        t = flipx;
                        flipx = flipy;
                        flipy = t;
                    }

                    for (i = 0; i + firstdrawn < Machine.gfx[bank].total_elements && i < cpx * cpy; i++) {
                        drawgfx(bitmap, Machine.gfx[bank],
                                i + firstdrawn, color, /*sprite num, color*/
                                flipx, flipy,
                                (i % cpx) * Machine.gfx[bank].width + Machine.uixmin,
                                Machine.uifontheight + (i / cpx) * Machine.gfx[bank].height + Machine.uiymin,
                                null, Machine.gfx[bank].colortable != null ? TRANSPARENCY_NONE : TRANSPARENCY_NONE_RAW, 0);

                        lastdrawn = i + firstdrawn;
                    }
                } else {
                    if (total_colors != 0) {
                        int sx, sy, colors;

                        colors = total_colors - 256 * palpage;
                        if (colors > 256) {
                            colors = 256;
                        }

                        for (i = 0; i < 16; i++) {
                            String bf = "";

                            sx = 3 * Machine.uifontwidth + (Machine.uifontwidth * 4 / 3) * (i % 16);
                            bf = sprintf("%X", i);
                            ui_text(bitmap, bf, sx, 2 * Machine.uifontheight);
                            if (16 * i < colors) {
                                sy = 3 * Machine.uifontheight + (Machine.uifontheight) * (i % 16);
                                bf = sprintf("%3X", i + 16 * palpage);
                                ui_text(bitmap, bf, 0, sy);
                            }
                        }

                        for (i = 0; i < colors; i++) {
                            sx = Machine.uixmin + 3 * Machine.uifontwidth + (Machine.uifontwidth * 4 / 3) * (i % 16);
                            sy = Machine.uiymin + 2 * Machine.uifontheight + (Machine.uifontheight) * (i / 16) + Machine.uifontheight;
                            plot_box.handler(bitmap, sx, sy, Machine.uifontwidth * 4 / 3, Machine.uifontheight, colortable.read(i + 256 * palpage));
                        }
                    } else {
                        ui_text(bitmap, "N/A", 3 * Machine.uifontwidth, 2 * Machine.uifontheight);
                    }
                }

                switch_true_orientation();

                if (bank >= 0) {
                    sprintf(buf, "GFXSET %d COLOR %2X CODE %X-%X", bank, color, firstdrawn, lastdrawn);
                } else if (bank == -2) {
                    buf = "PALETTE";
                } else if (bank == -1) {
                    buf = "CLUT";
                }
                ui_text(bitmap, buf, 0, 0);

                changed = 0;
            }

            update_video_and_audio();

            if (code_pressed(KEYCODE_LCONTROL) != 0 || code_pressed(KEYCODE_RCONTROL) != 0) {
                skip_chars = cpx;
            }
            if (code_pressed(KEYCODE_LSHIFT) != 0 || code_pressed(KEYCODE_RSHIFT) != 0) {
                skip_chars = 1;
            }

            if (input_ui_pressed_repeat(IPT_UI_RIGHT, 8) != 0) {
                int next;

                next = bank + 1;
                if (next == -1 && Machine.drv.color_table_len == 0) {
                    next++;
                }

                if (next < 0 || (next < MAX_GFX_ELEMENTS && Machine.gfx[next] != null)) {
                    bank = next;
//				firstdrawn = 0;
                    changed = 1;
                }
            }

            if (input_ui_pressed_repeat(IPT_UI_LEFT, 8) != 0) {
                if (bank > -2) {
                    bank--;
                    if (bank == -1 && Machine.drv.color_table_len == 0) {
                        bank--;
                    }
//				firstdrawn = 0;
                    changed = 1;
                }
            }

            if (code_pressed_memory_repeat(KEYCODE_PGDN, 4) != 0) {
                if (bank >= 0) {
                    if (firstdrawn + skip_chars < Machine.gfx[bank].total_elements) {
                        firstdrawn += skip_chars;
                        changed = 1;
                    }
                } else {
                    if (256 * (palpage + 1) < total_colors) {
                        palpage++;
                        changed = 1;
                    }
                }
            }

            if (code_pressed_memory_repeat(KEYCODE_PGUP, 4) != 0) {
                if (bank >= 0) {
                    firstdrawn -= skip_chars;
                    if (firstdrawn < 0) {
                        firstdrawn = 0;
                    }
                    changed = 1;
                } else {
                    if (palpage > 0) {
                        palpage--;
                        changed = 1;
                    }
                }
            }

            if (input_ui_pressed_repeat(IPT_UI_UP, 6) != 0) {
                if (bank >= 0) {
                    if (color < Machine.gfx[bank].total_colors - 1) {
                        color++;
                        changed = 1;
                    }
                }
            }

            if (input_ui_pressed_repeat(IPT_UI_DOWN, 6) != 0) {
                if (bank >= 0) {
                    if (color > 0) {
                        color--;
                        changed = 1;
                    }
                }
            }

            if (input_ui_pressed(IPT_UI_SNAPSHOT) != 0) {
                osd_save_snapshot(bitmap);
            }
        } while (input_ui_pressed(IPT_UI_SHOW_GFX) == 0
                && input_ui_pressed(IPT_UI_CANCEL) == 0);

        schedule_full_refresh();
    }

    public static int setdipswitches(mame_bitmap bitmap, int selected) {
        String[] menu_item = new String[128];
        String[] menu_subitem = new String[128];
        InputPort[] entry = new InputPort[128];
        int[] entry_ptr = new int[128];
        char[] flag = new char[40];
        int i, sel;
        InputPort[] _in;
        int total;
        int arrowize;

        sel = selected - 1;

        int in_ptr = 0;
        _in = Machine.input_ports;

        total = 0;
        while (_in[in_ptr].type != IPT_END) {
            if ((_in[in_ptr].type & ~IPF_MASK) == IPT_DIPSWITCH_NAME && input_port_name(_in, in_ptr) != null
                    && (_in[in_ptr].type & IPF_UNUSED) == 0
                    && !(options.cheat == 0 && (_in[in_ptr].type & IPF_CHEAT) != 0)) {
                entry[total] = _in[in_ptr];
                entry_ptr[total] = in_ptr;
                menu_item[total] = input_port_name(_in, in_ptr);

                total++;
            }

            in_ptr++;
        }

        if (total == 0) {
            return 0;
        }

        menu_item[total] = ui_getstring(UI_returntomain);
        menu_item[total + 1] = null;/* terminate array */
        total++;

        for (i = 0; i < total; i++) {
            flag[i] = '\0';/* TODO: flag the dip if it's not the real default */
            if (i < total - 1) {
                in_ptr = entry_ptr[i] + 1;//in = entry[i] + 1;
                while ((_in[in_ptr].type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING
                        && _in[in_ptr].default_value != entry[i].default_value) {
                    in_ptr++;
                }

                if ((_in[in_ptr].type & ~IPF_MASK) != IPT_DIPSWITCH_SETTING) {
                    menu_subitem[i] = ui_getstring(UI_INVALID);
                } else {
                    menu_subitem[i] = input_port_name(_in, in_ptr);
                }
            } else {
                menu_subitem[i] = null;/* no subitem */
            }
        }
        arrowize = 0;
        if (sel < total - 1) {
            in_ptr = entry_ptr[sel] + 1;//in = entry[sel] + 1;
            while ((_in[in_ptr].type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING
                    && _in[in_ptr].default_value != entry[sel].default_value) {
                in_ptr++;
            }

            if ((_in[in_ptr].type & ~IPF_MASK) != IPT_DIPSWITCH_SETTING) {
                /* invalid setting: revert to a valid one */
                arrowize |= 1;
            } else {
                if ((_in[in_ptr - 1].type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING
                        && !(options.cheat == 0 && (_in[in_ptr - 1].type & IPF_CHEAT) != 0)) {
                    arrowize |= 1;
                }
            }
        }
        if (sel < total - 1) {
            in_ptr = entry_ptr[sel] + 1;//in = entry[sel] + 1;
            while ((_in[in_ptr].type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING
                    && _in[in_ptr].default_value != entry[sel].default_value) {
                in_ptr++;
            }

            if ((_in[in_ptr].type & ~IPF_MASK) != IPT_DIPSWITCH_SETTING) /* invalid setting: revert to a valid one */ {
                arrowize |= 2;
            } else {
                if ((_in[in_ptr + 1].type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING
                        && !(options.cheat == 0 && (_in[in_ptr + 1].type & IPF_CHEAT) != 0)) {
                    arrowize |= 2;
                }
            }
        }

        ui_displaymenu(bitmap, menu_item, menu_subitem, flag, sel, arrowize);

        if (input_ui_pressed_repeat(IPT_UI_DOWN, 8) != 0) {
            sel = (sel + 1) % total;
        }

        if (input_ui_pressed_repeat(IPT_UI_UP, 8) != 0) {
            sel = (sel + total - 1) % total;
        }

        if (input_ui_pressed_repeat(IPT_UI_RIGHT, 8) != 0) {
            if (sel < total - 1) {
                in_ptr = entry_ptr[sel] + 1;//in = entry[sel] + 1;
                while ((_in[in_ptr].type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING
                        && _in[in_ptr].default_value != entry[sel].default_value) {
                    in_ptr++;
                }

                if ((_in[in_ptr].type & ~IPF_MASK) != IPT_DIPSWITCH_SETTING) /* invalid setting: revert to a valid one */ {
                    entry[sel].default_value = _in[entry_ptr[sel] + 1].default_value & entry[sel].mask; //(entry[sel]+1)->default_value & entry[sel]->mask;
                } else {
                    if ((_in[in_ptr + 1].type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING
                            && !(options.cheat == 0 && (_in[in_ptr + 1].type & IPF_CHEAT) != 0)) {
                        entry[sel].default_value = _in[in_ptr + 1].default_value & entry[sel].mask;//(in+1)->default_value & entry[sel]->mask;
                    }
                }

                /* tell updatescreen() to clean after us (in case the window changes size) */
                schedule_full_refresh();
            }
        }
        if (input_ui_pressed_repeat(IPT_UI_LEFT, 8) != 0) {
            if (sel < total - 1) {
                in_ptr = entry_ptr[sel] + 1;//in = entry[sel] + 1;
                while ((_in[in_ptr].type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING
                        && _in[in_ptr].default_value != entry[sel].default_value) {
                    in_ptr++;
                }

                if ((_in[in_ptr].type & ~IPF_MASK) != IPT_DIPSWITCH_SETTING) /* invalid setting: revert to a valid one */ {
                    entry[sel].default_value = _in[entry_ptr[sel] + 1].default_value & entry[sel].mask; //(entry[sel]+1)->default_value & entry[sel]->mask;
                } else {
                    if ((_in[in_ptr - 1].type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING
                            && !(options.cheat == 0 && (_in[in_ptr - 1].type & IPF_CHEAT) != 0)) {
                        entry[sel].default_value = _in[in_ptr - 1].default_value & entry[sel].mask;//(in-1)->default_value & entry[sel]->mask;
                    }
                }

                /* tell updatescreen() to clean after us (in case the window changes size) */
                schedule_full_refresh();
            }
        }

        if (input_ui_pressed(IPT_UI_SELECT) != 0) {
            if (sel == total - 1) {
                sel = -1;
            }
        }

        if (input_ui_pressed(IPT_UI_CANCEL) != 0) {
            sel = -1;
        }

        if (input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
            sel = -2;
        }

        if (sel == -1 || sel == -2) {
            /* tell updatescreen() to clean after us */
            schedule_full_refresh();
        }

        return sel + 1;

    }

    /* This flag is used for record OR sequence of key/joy */
 /* when is !=0 the first sequence is record, otherwise the first free */
 /* it's used byt setdefkeysettings, setdefjoysettings, setkeysettings, setjoysettings */
    static int record_first_insert = 1;

    static String[] menu_subitem_buffer = new String[400];//static char menu_subitem_buffer[400][96];

    public static int setdefcodesettings(mame_bitmap bitmap, int selected) {
        String[] menu_item = new String[400];
        String[] menu_subitem = new String[400];
        ipd[] entry = new ipd[400];
        char[] flag = new char[400];
        int i, sel;
        int in_ptr;
        ipd[] in;
        int total;

        sel = selected - 1;

        if (Machine.input_ports == null) {
            return 0;
        }

        in = inputport_defaults;
        in_ptr = 0;

        total = 0;
        while (in[in_ptr].type != IPT_END) {
            if (in[in_ptr].name != null && (in[in_ptr].type & ~IPF_MASK) != IPT_UNKNOWN && (in[in_ptr].type & IPF_UNUSED) == 0
                    && !(options.cheat == 0 && (in[in_ptr].type & IPF_CHEAT) != 0)) {
                entry[total] = in[in_ptr];
                menu_item[total] = in[in_ptr].name;

                total++;
            }

            in_ptr++;
        }
        if (total == 0) {
            return 0;
        }

        menu_item[total] = ui_getstring(UI_returntomain);
        menu_item[total + 1] = null;
        /* terminate array */
        total++;

        for (i = 0; i < total; i++) {
            if (i < total - 1) {
                menu_subitem_buffer[i] = seq_name(entry[i].seq, 100);//seq_name(&entry[i]->seq,menu_subitem_buffer[i],sizeof(menu_subitem_buffer[0]));
                menu_subitem[i] = menu_subitem_buffer[i];
            } else {
                menu_subitem[i] = null;
                /* no subitem */
            }
            flag[i] = '\0';
        }
        if (sel > SEL_MASK) /* are we waiting for a new key? */ {
            int ret;

            menu_subitem[sel & SEL_MASK] = "    ";
            ui_displaymenu(bitmap, menu_item, menu_subitem, flag, sel & SEL_MASK, 3);

            ret = seq_read_async(entry[sel & SEL_MASK].seq, record_first_insert);
            if (ret >= 0) {
                sel &= 0xff;

                if (ret > 0 || seq_get_1(entry[sel].seq) == CODE_NONE) {
                    seq_set_1(entry[sel].seq, CODE_NONE);
                    ret = 1;
                }

                /* tell updatescreen() to clean after us (in case the window changes size) */
                schedule_full_refresh();

                record_first_insert = ret != 0 ? 1 : 0;
            }

            return sel + 1;
        }

        ui_displaymenu(bitmap, menu_item, menu_subitem, flag, sel, 0);

        if (input_ui_pressed_repeat(IPT_UI_DOWN, 8) != 0) {
            sel = (sel + 1) % total;
            record_first_insert = 1;
        }
        if (input_ui_pressed_repeat(IPT_UI_UP, 8) != 0) {
            sel = (sel + total - 1) % total;
            record_first_insert = 1;
        }

        if (input_ui_pressed(IPT_UI_SELECT) != 0) {
            if (sel == total - 1) {
                sel = -1;
            } else {
                seq_read_async_start();

                sel |= 1 << SEL_BITS;
                /* we'll ask for a key */

 /* tell updatescreen() to clean after us (in case the window changes size) */
                schedule_full_refresh();
            }
        }
        if (input_ui_pressed(IPT_UI_CANCEL) != 0) {
            sel = -1;
        }

        if (input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
            sel = -2;
        }

        if (sel == -1 || sel == -2) {
            /* tell updatescreen() to clean after us */
            schedule_full_refresh();

            record_first_insert = 1;
        }

        return sel + 1;
    }

    public static int setcodesettings(mame_bitmap bitmap, int selected) {
        String[] menu_item = new String[400];
        String[] menu_subitem = new String[400];
        InputPort[] entry = new InputPort[400];
        char[] flag = new char[400];
        int i, sel;
        InputPort[] in;
        int in_ptr = 0;
        int total;

        sel = selected - 1;

        if (Machine.input_ports == null) {
            return 0;
        }

        in = Machine.input_ports;

        total = 0;
        while (in[in_ptr].type != IPT_END) {
            if (input_port_name(in, in_ptr) != null && seq_get_1(in[in_ptr].seq) != CODE_NONE && (in[in_ptr].type & ~IPF_MASK) != IPT_UNKNOWN) {
                entry[total] = in[in_ptr];
                menu_item[total] = input_port_name(in, in_ptr);

                total++;
            }

            in_ptr++;
        }

        if (total == 0) {
            return 0;
        }

        menu_item[total] = ui_getstring(UI_returntomain);
        menu_item[total + 1] = null;
        /* terminate array */
        total++;

        for (i = 0; i < total; i++) {
            if (i < total - 1) {
                menu_subitem_buffer[i] = seq_name(input_port_seq(entry, i), 100);//seq_name(input_port_seq(entry[i]),menu_subitem_buffer[i],sizeof(menu_subitem_buffer[0]));
                menu_subitem[i] = menu_subitem_buffer[i];

                /* If the key isn't the default, flag it */
                if (seq_get_1(entry[i].seq) != CODE_DEFAULT) {
                    flag[i] = 1;
                } else {
                    flag[i] = '\0';
                }

            } else {
                menu_subitem[i] = null;
                /* no subitem */
            }
        }
        if (sel > SEL_MASK) /* are we waiting for a new key? */ {
            int ret;

            menu_subitem[sel & SEL_MASK] = "    ";
            ui_displaymenu(bitmap, menu_item, menu_subitem, flag, sel & SEL_MASK, 3);

            ret = seq_read_async(entry[sel & SEL_MASK].seq, record_first_insert);

            if (ret >= 0) {
                sel &= 0xff;

                if (ret > 0 || seq_get_1(entry[sel].seq) == CODE_NONE) {
                    seq_set_1(entry[sel].seq, CODE_DEFAULT);
                    ret = 1;
                }

                /* tell updatescreen() to clean after us (in case the window changes size) */
                schedule_full_refresh();

                record_first_insert = ret != 0 ? 1 : 0;
            }

            return sel + 1;
        }

        ui_displaymenu(bitmap, menu_item, menu_subitem, flag, sel, 0);

        if (input_ui_pressed_repeat(IPT_UI_DOWN, 8) != 0) {
            sel = (sel + 1) % total;
            record_first_insert = 1;
        }

        if (input_ui_pressed_repeat(IPT_UI_UP, 8) != 0) {
            sel = (sel + total - 1) % total;
            record_first_insert = 1;
        }
        if (input_ui_pressed(IPT_UI_SELECT) != 0) {
            if (sel == total - 1) {
                sel = -1;
            } else {
                seq_read_async_start();

                sel |= 1 << SEL_BITS;
                /* we'll ask for a key */

 /* tell updatescreen() to clean after us (in case the window changes size) */
                schedule_full_refresh();
            }
        }
        if (input_ui_pressed(IPT_UI_CANCEL) != 0) {
            sel = -1;
        }

        if (input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
            sel = -2;
        }

        if (sel == -1 || sel == -2) {
            /* tell updatescreen() to clean after us */
            schedule_full_refresh();

            record_first_insert = 1;
        }

        return sel + 1;
    }

    /*TODO*///static int calibratejoysticks(struct mame_bitmap *bitmap,int selected)
/*TODO*///{
/*TODO*///	const char *msg;
/*TODO*///	static char buf[2048];
/*TODO*///	int sel;
/*TODO*///	static int calibration_started = 0;
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///	if (calibration_started == 0)
/*TODO*///	{
/*TODO*///		osd_joystick_start_calibration();
/*TODO*///		calibration_started = 1;
/*TODO*///		strcpy (buf, "");
/*TODO*///	}
/*TODO*///
/*TODO*///	if (sel > SEL_MASK) /* Waiting for the user to acknowledge joystick movement */
/*TODO*///	{
/*TODO*///		if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		{
/*TODO*///			calibration_started = 0;
/*TODO*///			sel = -1;
/*TODO*///		}
/*TODO*///		else if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///		{
/*TODO*///			osd_joystick_calibrate();
/*TODO*///			sel &= 0xff;
/*TODO*///		}
/*TODO*///
/*TODO*///		ui_displaymessagewindow(bitmap,buf);
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		msg = osd_joystick_calibrate_next();
/*TODO*///		schedule_full_refresh();
/*TODO*///		if (msg == 0)
/*TODO*///		{
/*TODO*///			calibration_started = 0;
/*TODO*///			osd_joystick_end_calibration();
/*TODO*///			sel = -1;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			strcpy (buf, msg);
/*TODO*///			ui_displaymessagewindow(bitmap,buf);
/*TODO*///			sel |= 1 << SEL_BITS;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///		sel = -2;
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		schedule_full_refresh();
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///static int settraksettings(struct mame_bitmap *bitmap,int selected)
/*TODO*///{
/*TODO*///	const char *menu_item[40];
/*TODO*///	const char *menu_subitem[40];
/*TODO*///	struct InputPort *entry[40];
/*TODO*///	int i,sel;
/*TODO*///	struct InputPort *in;
/*TODO*///	int total,total2;
/*TODO*///	int arrowize;
/*TODO*///
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///
/*TODO*///	if (Machine->input_ports == 0)
/*TODO*///		return 0;
/*TODO*///
/*TODO*///	in = Machine->input_ports;
/*TODO*///
/*TODO*///	/* Count the total number of analog controls */
/*TODO*///	total = 0;
/*TODO*///	while (in->type != IPT_END)
/*TODO*///	{
/*TODO*///		if (((in->type & 0xff) > IPT_ANALOG_START) && ((in->type & 0xff) < IPT_ANALOG_END)
/*TODO*///				&& !(!options.cheat && (in->type & IPF_CHEAT)))
/*TODO*///		{
/*TODO*///			entry[total] = in;
/*TODO*///			total++;
/*TODO*///		}
/*TODO*///		in++;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (total == 0) return 0;
/*TODO*///
/*TODO*///	/* Each analog control has 3 entries - key & joy delta, reverse, sensitivity */
/*TODO*///
/*TODO*///#define ENTRIES 3
/*TODO*///
/*TODO*///	total2 = total * ENTRIES;
/*TODO*///
/*TODO*///	menu_item[total2] = ui_getstring (UI_returntomain);
/*TODO*///	menu_item[total2 + 1] = 0;	/* terminate array */
/*TODO*///	total2++;
/*TODO*///
/*TODO*///	arrowize = 0;
/*TODO*///	for (i = 0;i < total2;i++)
/*TODO*///	{
/*TODO*///		if (i < total2 - 1)
/*TODO*///		{
/*TODO*///			char label[30][40];
/*TODO*///			char setting[30][40];
/*TODO*///			int sensitivity,delta;
/*TODO*///			int reverse;
/*TODO*///
/*TODO*///			strcpy (label[i], input_port_name(entry[i/ENTRIES]));
/*TODO*///			sensitivity = IP_GET_SENSITIVITY(entry[i/ENTRIES]);
/*TODO*///			delta = IP_GET_DELTA(entry[i/ENTRIES]);
/*TODO*///			reverse = (entry[i/ENTRIES]->type & IPF_REVERSE);
/*TODO*///
/*TODO*///			strcat (label[i], " ");
/*TODO*///			switch (i%ENTRIES)
/*TODO*///			{
/*TODO*///				case 0:
/*TODO*///					strcat (label[i], ui_getstring (UI_keyjoyspeed));
/*TODO*///					sprintf(setting[i],"%d",delta);
/*TODO*///					if (i == sel) arrowize = 3;
/*TODO*///					break;
/*TODO*///				case 1:
/*TODO*///					strcat (label[i], ui_getstring (UI_reverse));
/*TODO*///					if (reverse)
/*TODO*///						sprintf(setting[i],ui_getstring (UI_on));
/*TODO*///					else
/*TODO*///						sprintf(setting[i],ui_getstring (UI_off));
/*TODO*///					if (i == sel) arrowize = 3;
/*TODO*///					break;
/*TODO*///				case 2:
/*TODO*///					strcat (label[i], ui_getstring (UI_sensitivity));
/*TODO*///					sprintf(setting[i],"%3d%%",sensitivity);
/*TODO*///					if (i == sel) arrowize = 3;
/*TODO*///					break;
/*TODO*///			}
/*TODO*///
/*TODO*///			menu_item[i] = label[i];
/*TODO*///			menu_subitem[i] = setting[i];
/*TODO*///
/*TODO*///			in++;
/*TODO*///		}
/*TODO*///		else menu_subitem[i] = 0;	/* no subitem */
/*TODO*///	}
/*TODO*///
/*TODO*///	ui_displaymenu(bitmap,menu_item,menu_subitem,0,sel,arrowize);
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///		sel = (sel + 1) % total2;
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///		sel = (sel + total2 - 1) % total2;
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_LEFT,8))
/*TODO*///	{
/*TODO*///		if ((sel % ENTRIES) == 0)
/*TODO*///		/* keyboard/joystick delta */
/*TODO*///		{
/*TODO*///			int val = IP_GET_DELTA(entry[sel/ENTRIES]);
/*TODO*///
/*TODO*///			val --;
/*TODO*///			if (val < 1) val = 1;
/*TODO*///			IP_SET_DELTA(entry[sel/ENTRIES],val);
/*TODO*///		}
/*TODO*///		else if ((sel % ENTRIES) == 1)
/*TODO*///		/* reverse */
/*TODO*///		{
/*TODO*///			int reverse = entry[sel/ENTRIES]->type & IPF_REVERSE;
/*TODO*///			if (reverse)
/*TODO*///				reverse=0;
/*TODO*///			else
/*TODO*///				reverse=IPF_REVERSE;
/*TODO*///			entry[sel/ENTRIES]->type &= ~IPF_REVERSE;
/*TODO*///			entry[sel/ENTRIES]->type |= reverse;
/*TODO*///		}
/*TODO*///		else if ((sel % ENTRIES) == 2)
/*TODO*///		/* sensitivity */
/*TODO*///		{
/*TODO*///			int val = IP_GET_SENSITIVITY(entry[sel/ENTRIES]);
/*TODO*///
/*TODO*///			val --;
/*TODO*///			if (val < 1) val = 1;
/*TODO*///			IP_SET_SENSITIVITY(entry[sel/ENTRIES],val);
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_RIGHT,8))
/*TODO*///	{
/*TODO*///		if ((sel % ENTRIES) == 0)
/*TODO*///		/* keyboard/joystick delta */
/*TODO*///		{
/*TODO*///			int val = IP_GET_DELTA(entry[sel/ENTRIES]);
/*TODO*///
/*TODO*///			val ++;
/*TODO*///			if (val > 255) val = 255;
/*TODO*///			IP_SET_DELTA(entry[sel/ENTRIES],val);
/*TODO*///		}
/*TODO*///		else if ((sel % ENTRIES) == 1)
/*TODO*///		/* reverse */
/*TODO*///		{
/*TODO*///			int reverse = entry[sel/ENTRIES]->type & IPF_REVERSE;
/*TODO*///			if (reverse)
/*TODO*///				reverse=0;
/*TODO*///			else
/*TODO*///				reverse=IPF_REVERSE;
/*TODO*///			entry[sel/ENTRIES]->type &= ~IPF_REVERSE;
/*TODO*///			entry[sel/ENTRIES]->type |= reverse;
/*TODO*///		}
/*TODO*///		else if ((sel % ENTRIES) == 2)
/*TODO*///		/* sensitivity */
/*TODO*///		{
/*TODO*///			int val = IP_GET_SENSITIVITY(entry[sel/ENTRIES]);
/*TODO*///
/*TODO*///			val ++;
/*TODO*///			if (val > 255) val = 255;
/*TODO*///			IP_SET_SENSITIVITY(entry[sel/ENTRIES],val);
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///	{
/*TODO*///		if (sel == total2 - 1) sel = -1;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		sel = -1;
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///		sel = -2;
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		schedule_full_refresh();
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
    public static int mame_stats(mame_bitmap bitmap, int selected) {
        String temp = "";
        String buf = "";
        int sel, i;

        sel = selected - 1;

        if (dispensed_tickets != 0) {
            buf += ui_getstring(UI_tickets);
            buf += ": ";
            temp = sprintf("%d\n\n", dispensed_tickets);
            buf += temp;
        }

        for (i = 0; i < COIN_COUNTERS; i++) {
            buf += ui_getstring(UI_coin);
            temp = sprintf(" %c: ", i + 'A');
            buf += temp;
            if (coins[i] == 0) {
                buf += ui_getstring(UI_NA);
            } else {
                temp = sprintf("%d", coins[i]);
                buf += temp;
            }
            if (coinlockedout[i] != 0) {
                buf += " ";
                buf += ui_getstring(UI_locked);
                buf += "\n";
            } else {
                buf += "\n";
            }
        }

        {
            /* menu system, use the normal menu keys */
            buf += "\n\t";
            buf += ui_getstring(UI_lefthilight);
            buf += " ";
            buf += ui_getstring(UI_returntomain);
            buf += " ";
            buf += ui_getstring(UI_righthilight);

            ui_displaymessagewindow(bitmap, buf);

            if (input_ui_pressed(IPT_UI_SELECT) != 0) {
                sel = -1;
            }

            if (input_ui_pressed(IPT_UI_CANCEL) != 0) {
                sel = -1;
            }

            if (input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
                sel = -2;
            }
        }

        if (sel == -1 || sel == -2) {
            /* tell updatescreen() to clean after us */
            schedule_full_refresh();
        }

        return sel + 1;
    }

    public static int showcopyright(mame_bitmap bitmap) {
        int done;
        String buf = "";
        String buf2 = "";

        buf = ui_getstring(UI_copyright1);
        buf += "\n\n";
        buf2 = sprintf(ui_getstring(UI_copyright2), Machine.gamedrv.description);
        buf += buf2;
        buf += "\n\n";
        buf += ui_getstring(UI_copyright3);

        erase_screen(bitmap);
        ui_displaymessagewindow(bitmap, buf);

        setup_selected = -1;////
        done = 0;
        do {
            update_video_and_audio();
            if (input_ui_pressed(IPT_UI_CANCEL) != 0) {
                setup_selected = 0;////
                return 1;
            }
            if (keyboard_pressed_memory(KEYCODE_O) != 0
                    || input_ui_pressed(IPT_UI_LEFT) != 0) {
                done = 1;
            }
            if (done == 1 && (keyboard_pressed_memory(KEYCODE_K) != 0
                    || input_ui_pressed(IPT_UI_RIGHT) != 0)) {
                done = 2;
            }
        } while (done < 2);

        setup_selected = 0;////
        erase_screen(bitmap);
        update_video_and_audio();

        return 0;
    }

    public static int displaygameinfo(mame_bitmap bitmap, int selected) {
        int i;
        String buf = "";
        String buf2 = "";
        int sel;

        sel = selected - 1;

        buf = sprintf("%s\n%s %s\n\n%s:\n", Machine.gamedrv.description, Machine.gamedrv.year, Machine.gamedrv.manufacturer,
                ui_getstring(UI_cpu));
        i = 0;
        while (i < MAX_CPU && Machine.drv.cpu[i].cpu_type != 0) {

            if (Machine.drv.cpu[i].cpu_clock >= 1000000) {
                buf += sprintf("%s %d.%06d MHz",
                        cputype_name(Machine.drv.cpu[i].cpu_type),
                        Machine.drv.cpu[i].cpu_clock / 1000000,
                        Machine.drv.cpu[i].cpu_clock % 1000000);
            } else {
                buf += sprintf("%s %d.%03d kHz",
                        cputype_name(Machine.drv.cpu[i].cpu_type),
                        Machine.drv.cpu[i].cpu_clock / 1000,
                        Machine.drv.cpu[i].cpu_clock % 1000);
            }

            if ((Machine.drv.cpu[i].cpu_type & CPU_AUDIO_CPU) != 0) {
                buf2 = sprintf(" (%s)", ui_getstring(UI_sound_lc));
                buf += buf2;
            }

            buf += "\n";

            i++;
        }

        buf2 = sprintf("\n%s", ui_getstring(UI_sound));
        buf += buf2;
        if ((Machine.drv.sound_attributes & SOUND_SUPPORTS_STEREO) != 0) {
            buf += sprintf(" (%s)", ui_getstring(UI_stereo));
        }
        buf += ":\n";

        i = 0;
        while (i < MAX_SOUND && Machine.drv.sound[i].sound_type != 0) {
            if (sound_num(Machine.drv.sound[i]) != 0) {
                buf += sprintf("%dx", sound_num(Machine.drv.sound[i]));
            }

            buf += sprintf("%s", sound_name(Machine.drv.sound[i]));

            if (sound_clock(Machine.drv.sound[i]) != 0) {
                if (sound_clock(Machine.drv.sound[i]) >= 1000000) {
                    buf += sprintf(" %d.%06d MHz",
                            sound_clock(Machine.drv.sound[i]) / 1000000,
                            sound_clock(Machine.drv.sound[i]) % 1000000);
                } else {
                    buf += sprintf(" %d.%03d kHz",
                            sound_clock(Machine.drv.sound[i]) / 1000,
                            sound_clock(Machine.drv.sound[i]) % 1000);
                }
            }
            buf += "\n";

            i++;
        }
        if ((Machine.drv.video_attributes & VIDEO_TYPE_VECTOR) != 0) {
            buf += sprintf("\n%s\n", ui_getstring(UI_vectorgame));
        } else {
            buf += sprintf("\n%s:\n", ui_getstring(UI_screenres));
            buf += sprintf("%d x %d (%s) %f Hz\n",
                    Machine.visible_area.max_x - Machine.visible_area.min_x + 1,
                    Machine.visible_area.max_y - Machine.visible_area.min_y + 1,
                    (Machine.gamedrv.flags & ORIENTATION_SWAP_XY) != 0 ? "V" : "H",
                    Machine.drv.frames_per_second);
        }
        if (sel == -1) {
            /* startup info, print MAME version and ask for any key */

            buf2 = sprintf("\n\t%s ", "Arcadeflex"/*ui_getstring (UI_mame)*/);
            /* \t means that the line will be centered */
            buf += buf2;

            buf += build_version;
            buf2 = sprintf("\n\t%s", ui_getstring(UI_anykey));
            buf += buf2;
            ui_drawbox(bitmap, 0, 0, Machine.uiwidth, Machine.uiheight);
            ui_displaymessagewindow(bitmap, buf);

            sel = 0;
            if (code_read_async() != CODE_NONE) {
                sel = -1;
            }
        } else {
            /* menu system, use the normal menu keys */
            buf += "\n\t";
            buf += ui_getstring(UI_lefthilight);
            buf += " ";
            buf += ui_getstring(UI_returntomain);
            buf += " ";
            buf += ui_getstring(UI_righthilight);

            ui_displaymessagewindow(bitmap, buf);

            if (input_ui_pressed(IPT_UI_SELECT) != 0) {
                sel = -1;
            }

            if (input_ui_pressed(IPT_UI_CANCEL) != 0) {
                sel = -1;
            }

            if (input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
                sel = -2;
            }
        }

        if (sel == -1 || sel == -2) {
            schedule_full_refresh();
        }

        return sel + 1;
    }

    public static int showgamewarnings(mame_bitmap bitmap) {
        int i;
        String buf = "";

        if ((Machine.gamedrv.flags
                & (GAME_NOT_WORKING | GAME_UNEMULATED_PROTECTION | GAME_WRONG_COLORS | GAME_IMPERFECT_COLORS
                | GAME_NO_SOUND | GAME_IMPERFECT_SOUND | GAME_IMPERFECT_GRAPHICS | GAME_NO_COCKTAIL)) != 0) {
            int done;

            buf = ui_getstring(UI_knownproblems);
            buf += "\n\n";

            if ((Machine.gamedrv.flags & GAME_IMPERFECT_COLORS) != 0) {
                buf += ui_getstring(UI_imperfectcolors);
                buf += "\n";
            }

            if ((Machine.gamedrv.flags & GAME_WRONG_COLORS) != 0) {
                buf += ui_getstring(UI_wrongcolors);
                buf += "\n";
            }

            if ((Machine.gamedrv.flags & GAME_IMPERFECT_GRAPHICS) != 0) {
                buf += ui_getstring(UI_imperfectgraphics);
                buf += "\n";
            }
            if ((Machine.gamedrv.flags & GAME_IMPERFECT_SOUND) != 0) {
                buf += ui_getstring(UI_imperfectsound);
                buf += "\n";
            }

            if ((Machine.gamedrv.flags & GAME_NO_SOUND) != 0) {
                buf += ui_getstring(UI_nosound);
                buf += "\n";
            }

            if ((Machine.gamedrv.flags & GAME_NO_COCKTAIL) != 0) {
                buf += ui_getstring(UI_nococktail);
                buf += "\n";
            }

            if ((Machine.gamedrv.flags & (GAME_NOT_WORKING | GAME_UNEMULATED_PROTECTION)) != 0) {
                GameDriver maindrv;
                int foundworking;

                if ((Machine.gamedrv.flags & GAME_NOT_WORKING) != 0) {
                    buf += ui_getstring(UI_brokengame);
                    buf += "\n";
                }
                if ((Machine.gamedrv.flags & GAME_UNEMULATED_PROTECTION) != 0) {
                    buf += ui_getstring(UI_brokenprotection);
                    buf += "\n";
                }
                if (Machine.gamedrv.clone_of != null && (Machine.gamedrv.clone_of.flags & NOT_A_DRIVER) == 0) {
                    maindrv = Machine.gamedrv.clone_of;
                } else {
                    maindrv = Machine.gamedrv;
                }

                foundworking = 0;
                i = 0;
                while (drivers[i] != null) {
                    if (drivers[i] == maindrv || drivers[i].clone_of == maindrv) {
                        if ((drivers[i].flags & (GAME_NOT_WORKING | GAME_UNEMULATED_PROTECTION)) == 0) {
                            if (foundworking == 0) {
                                buf += "\n\n";
                                buf += ui_getstring(UI_workingclones);
                                buf += "\n\n";
                            }
                            foundworking = 1;

                            buf += sprintf("%s\n", drivers[i].name);
                        }
                    }
                    i++;
                }
            }

            buf += "\n\n";
            buf += ui_getstring(UI_typeok);

            erase_screen(bitmap);
            ui_displaymessagewindow(bitmap, buf);

            done = 0;
            do {
                update_video_and_audio();
                if (input_ui_pressed(IPT_UI_CANCEL) != 0) {
                    return 1;
                }
                if (code_pressed_memory(KEYCODE_O) != 0
                        || input_ui_pressed(IPT_UI_LEFT) != 0) {
                    done = 1;
                }
                if (done == 1 && (code_pressed_memory(KEYCODE_K) != 0
                        || input_ui_pressed(IPT_UI_RIGHT) != 0)) {
                    done = 2;
                }
            } while (done < 2);
        }

        erase_screen(bitmap);

        /* clear the input memory */
        while (code_read_async() != CODE_NONE) {
        }

        while (displaygameinfo(bitmap, 0) == 1) {
            update_video_and_audio();
            /*TODO*///      osd_poll_joysticks();
        }

        erase_screen(bitmap);
        /* make sure that the screen is really cleared, in case autoframeskip kicked in */
        update_video_and_audio();
        update_video_and_audio();
        update_video_and_audio();
        update_video_and_audio();

        return 0;
    }
    /*TODO*///
/*TODO*////* Word-wraps the text in the specified buffer to fit in maxwidth characters per line.
/*TODO*///   The contents of the buffer are modified.
/*TODO*///   Known limitations: Words longer than maxwidth cause the function to fail. */
/*TODO*///static void wordwrap_text_buffer (char *buffer, int maxwidth)
/*TODO*///{
/*TODO*///	int width = 0;
/*TODO*///
/*TODO*///	while (*buffer)
/*TODO*///	{
/*TODO*///		if (*buffer == '\n')
/*TODO*///		{
/*TODO*///			buffer++;
/*TODO*///			width = 0;
/*TODO*///			continue;
/*TODO*///		}
/*TODO*///
/*TODO*///		width++;
/*TODO*///
/*TODO*///		if (width > maxwidth)
/*TODO*///		{
/*TODO*///			/* backtrack until a space is found */
/*TODO*///			while (*buffer != ' ')
/*TODO*///			{
/*TODO*///				buffer--;
/*TODO*///				width--;
/*TODO*///			}
/*TODO*///			if (width < 1) return;	/* word too long */
/*TODO*///
/*TODO*///			/* replace space with a newline */
/*TODO*///			*buffer = '\n';
/*TODO*///		}
/*TODO*///		else
/*TODO*///			buffer++;
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///static int count_lines_in_buffer (char *buffer)
/*TODO*///{
/*TODO*///	int lines = 0;
/*TODO*///	char c;
/*TODO*///
/*TODO*///	while ( (c = *buffer++) )
/*TODO*///		if (c == '\n') lines++;
/*TODO*///
/*TODO*///	return lines;
/*TODO*///}
/*TODO*///
/*TODO*////* Display lines from buffer, starting with line 'scroll', in a width x height text window */
/*TODO*///static void display_scroll_message (struct mame_bitmap *bitmap, int *scroll, int width, int height, char *buf)
/*TODO*///{
/*TODO*///	struct DisplayText dt[256];
/*TODO*///	int curr_dt = 0;
/*TODO*///	const char *uparrow = ui_getstring (UI_uparrow);
/*TODO*///	const char *downarrow = ui_getstring (UI_downarrow);
/*TODO*///	char textcopy[2048];
/*TODO*///	char *copy;
/*TODO*///	int leftoffs,topoffs;
/*TODO*///	int first = *scroll;
/*TODO*///	int buflines,showlines;
/*TODO*///	int i;
/*TODO*///
/*TODO*///
/*TODO*///	/* draw box */
/*TODO*///	leftoffs = (Machine->uiwidth - Machine->uifontwidth * (width + 1)) / 2;
/*TODO*///	if (leftoffs < 0) leftoffs = 0;
/*TODO*///	topoffs = (Machine->uiheight - (3 * height + 1) * Machine->uifontheight / 2) / 2;
/*TODO*///	ui_drawbox(bitmap,leftoffs,topoffs,(width + 1) * Machine->uifontwidth,(3 * height + 1) * Machine->uifontheight / 2);
/*TODO*///
/*TODO*///	buflines = count_lines_in_buffer (buf);
/*TODO*///	if (first > 0)
/*TODO*///	{
/*TODO*///		if (buflines <= height)
/*TODO*///			first = 0;
/*TODO*///		else
/*TODO*///		{
/*TODO*///			height--;
/*TODO*///			if (first > (buflines - height))
/*TODO*///				first = buflines - height;
/*TODO*///		}
/*TODO*///		*scroll = first;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (first != 0)
/*TODO*///	{
/*TODO*///		/* indicate that scrolling upward is possible */
/*TODO*///		dt[curr_dt].text = uparrow;
/*TODO*///		dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///		dt[curr_dt].x = (Machine->uiwidth - Machine->uifontwidth * strlen(uparrow)) / 2;
/*TODO*///		dt[curr_dt].y = topoffs + (3*curr_dt+1)*Machine->uifontheight/2;
/*TODO*///		curr_dt++;
/*TODO*///	}
/*TODO*///
/*TODO*///	if ((buflines - first) > height)
/*TODO*///		showlines = height - 1;
/*TODO*///	else
/*TODO*///		showlines = height;
/*TODO*///
/*TODO*///	/* skip to first line */
/*TODO*///	while (first > 0)
/*TODO*///	{
/*TODO*///		char c;
/*TODO*///
/*TODO*///		while ( (c = *buf++) )
/*TODO*///		{
/*TODO*///			if (c == '\n')
/*TODO*///			{
/*TODO*///				first--;
/*TODO*///				break;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* copy 'showlines' lines from buffer, starting with line 'first' */
/*TODO*///	copy = textcopy;
/*TODO*///	for (i = 0; i < showlines; i++)
/*TODO*///	{
/*TODO*///		char *copystart = copy;
/*TODO*///
/*TODO*///		while (*buf && *buf != '\n')
/*TODO*///		{
/*TODO*///			*copy = *buf;
/*TODO*///			copy++;
/*TODO*///			buf++;
/*TODO*///		}
/*TODO*///		*copy = '\0';
/*TODO*///		copy++;
/*TODO*///		if (*buf == '\n')
/*TODO*///			buf++;
/*TODO*///
/*TODO*///		if (*copystart == '\t') /* center text */
/*TODO*///		{
/*TODO*///			copystart++;
/*TODO*///			dt[curr_dt].x = (Machine->uiwidth - Machine->uifontwidth * (copy - copystart)) / 2;
/*TODO*///		}
/*TODO*///		else
/*TODO*///			dt[curr_dt].x = leftoffs + Machine->uifontwidth/2;
/*TODO*///
/*TODO*///		dt[curr_dt].text = copystart;
/*TODO*///		dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///		dt[curr_dt].y = topoffs + (3*curr_dt+1)*Machine->uifontheight/2;
/*TODO*///		curr_dt++;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (showlines == (height - 1))
/*TODO*///	{
/*TODO*///		/* indicate that scrolling downward is possible */
/*TODO*///		dt[curr_dt].text = downarrow;
/*TODO*///		dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///		dt[curr_dt].x = (Machine->uiwidth - Machine->uifontwidth * strlen(downarrow)) / 2;
/*TODO*///		dt[curr_dt].y = topoffs + (3*curr_dt+1)*Machine->uifontheight/2;
/*TODO*///		curr_dt++;
/*TODO*///	}
/*TODO*///
/*TODO*///	dt[curr_dt].text = 0;	/* terminate array */
/*TODO*///
/*TODO*///	displaytext(bitmap,dt);
/*TODO*///}
/*TODO*///
/*TODO*///
    static int hist_scroll = 0;

    /* Display text entry for current driver from history.dat and mameinfo.dat. */
    public static int displayhistory(mame_bitmap bitmap, int selected) {

        /*TODO*///	static char *buf = 0;
        int maxcols, maxrows;
        int sel;

        sel = selected - 1;

        maxcols = (Machine.uiwidth / Machine.uifontwidth) - 1;
        maxrows = (2 * Machine.uiheight - Machine.uifontheight) / (3 * Machine.uifontheight);
        maxcols -= 2;
        maxrows -= 8;
        /*TODO*///
/*TODO*///	if (!buf)
/*TODO*///	{
/*TODO*///		/* allocate a buffer for the text */
/*TODO*///		buf = malloc (8192);
/*TODO*///		if (buf)
/*TODO*///		{
/*TODO*///			/* try to load entry */
/*TODO*///			if (load_driver_history (Machine->gamedrv, buf, 8192) == 0)
/*TODO*///			{
/*TODO*///				scroll = 0;
/*TODO*///				wordwrap_text_buffer (buf, maxcols);
/*TODO*///				strcat(buf,"\n\t");
/*TODO*///				strcat(buf,ui_getstring (UI_lefthilight));
/*TODO*///				strcat(buf," ");
/*TODO*///				strcat(buf,ui_getstring (UI_returntomain));
/*TODO*///				strcat(buf," ");
/*TODO*///				strcat(buf,ui_getstring (UI_righthilight));
/*TODO*///				strcat(buf,"\n");
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				free (buf);
/*TODO*///				buf = 0;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
        {
            /*TODO*///		if (buf)
/*TODO*///			display_scroll_message (bitmap, &scroll, maxcols, maxrows, buf);
/*TODO*///		else
/*TODO*///		{
            String msg = "";

            msg += "\t";
            msg += ui_getstring(UI_historymissing);
            msg += "\n\n\t";
            msg += ui_getstring(UI_lefthilight);
            msg += " ";
            msg += ui_getstring(UI_returntomain);
            msg += " ";
            msg += ui_getstring(UI_righthilight);
            ui_displaymessagewindow(bitmap, msg);
            /*TODO*///		}

            if ((hist_scroll > 0) && input_ui_pressed_repeat(IPT_UI_UP, 4) != 0) {
                if (hist_scroll == 2) {
                    hist_scroll = 0;
                    /* 1 would be the same as 0, but with arrow on top */
                } else {
                    hist_scroll--;
                }
            }

            if (input_ui_pressed_repeat(IPT_UI_DOWN, 4) != 0) {
                if (hist_scroll == 0) {
                    hist_scroll = 2;
                    /* 1 would be the same as 0, but with arrow on top */
                } else {
                    hist_scroll++;
                }
            }

            if (input_ui_pressed(IPT_UI_SELECT) != 0) {
                sel = -1;
            }

            if (input_ui_pressed(IPT_UI_CANCEL) != 0) {
                sel = -1;
            }

            if (input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
                sel = -2;
            }
        }
        if (sel == -1 || sel == -2) {
            /* tell updatescreen() to clean after us */
            schedule_full_refresh();
            /*TODO*///
/*TODO*///		/* force buffer to be recreated */
/*TODO*///		if (buf)
/*TODO*///		{
/*TODO*///			free (buf);
/*TODO*///			buf = 0;
/*TODO*///        }
        }

        return sel + 1;
    }
    /*TODO*////* Display text entry for current driver from history.dat and mameinfo.dat. */
/*TODO*///static int displayhistory (struct mame_bitmap *bitmap, int selected)
/*TODO*///{
/*TODO*///	static int scroll = 0;
/*TODO*///	static char *buf = 0;
/*TODO*///	int maxcols,maxrows;
/*TODO*///	int sel;
/*TODO*///
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///
/*TODO*///	maxcols = (Machine->uiwidth / Machine->uifontwidth) - 1;
/*TODO*///	maxrows = (2 * Machine->uiheight - Machine->uifontheight) / (3 * Machine->uifontheight);
/*TODO*///	maxcols -= 2;
/*TODO*///	maxrows -= 8;
/*TODO*///
/*TODO*///	if (!buf)
/*TODO*///	{
/*TODO*///		/* allocate a buffer for the text */
/*TODO*///		#ifndef MESS
/*TODO*///		buf = malloc (8192);
/*TODO*///		#else
/*TODO*///		buf = malloc (200*1024);
/*TODO*///		#endif
/*TODO*///		if (buf)
/*TODO*///		{
/*TODO*///			/* try to load entry */
/*TODO*///			#ifndef MESS
/*TODO*///			if (load_driver_history (Machine->gamedrv, buf, 8192) == 0)
/*TODO*///			#else
/*TODO*///			if (load_driver_history (Machine->gamedrv, buf, 200*1024) == 0)
/*TODO*///			#endif
/*TODO*///			{
/*TODO*///				scroll = 0;
/*TODO*///				wordwrap_text_buffer (buf, maxcols);
/*TODO*///				strcat(buf,"\n\t");
/*TODO*///				strcat(buf,ui_getstring (UI_lefthilight));
/*TODO*///				strcat(buf," ");
/*TODO*///				strcat(buf,ui_getstring (UI_returntomain));
/*TODO*///				strcat(buf," ");
/*TODO*///				strcat(buf,ui_getstring (UI_righthilight));
/*TODO*///				strcat(buf,"\n");
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				free (buf);
/*TODO*///				buf = 0;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	{
/*TODO*///		if (buf)
/*TODO*///			display_scroll_message (bitmap, &scroll, maxcols, maxrows, buf);
/*TODO*///		else
/*TODO*///		{
/*TODO*///			char msg[80];
/*TODO*///
/*TODO*///			strcpy(msg,"\t");
/*TODO*///			strcat(msg,ui_getstring(UI_historymissing));
/*TODO*///			strcat(msg,"\n\n\t");
/*TODO*///			strcat(msg,ui_getstring (UI_lefthilight));
/*TODO*///			strcat(msg," ");
/*TODO*///			strcat(msg,ui_getstring (UI_returntomain));
/*TODO*///			strcat(msg," ");
/*TODO*///			strcat(msg,ui_getstring (UI_righthilight));
/*TODO*///			ui_displaymessagewindow(bitmap,msg);
/*TODO*///		}
/*TODO*///
/*TODO*///		if ((scroll > 0) && input_ui_pressed_repeat(IPT_UI_UP,4))
/*TODO*///		{
/*TODO*///			if (scroll == 2) scroll = 0;	/* 1 would be the same as 0, but with arrow on top */
/*TODO*///			else scroll--;
/*TODO*///		}
/*TODO*///
/*TODO*///		if (input_ui_pressed_repeat(IPT_UI_DOWN,4))
/*TODO*///		{
/*TODO*///			if (scroll == 0) scroll = 2;	/* 1 would be the same as 0, but with arrow on top */
/*TODO*///			else scroll++;
/*TODO*///		}
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///			sel = -1;
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///			sel = -1;
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///			sel = -2;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		schedule_full_refresh();
/*TODO*///
/*TODO*///		/* force buffer to be recreated */
/*TODO*///		if (buf)
/*TODO*///		{
/*TODO*///			free (buf);
/*TODO*///			buf = 0;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///#ifndef MESS
/*TODO*///#ifndef TINY_COMPILE
/*TODO*///#ifndef CPSMAME
/*TODO*///int memcard_menu(struct mame_bitmap *bitmap, int selection)
/*TODO*///{
/*TODO*///	int sel;
/*TODO*///	int menutotal = 0;
/*TODO*///	const char *menuitem[10];
/*TODO*///	char buf[256];
/*TODO*///	char buf2[256];
/*TODO*///
/*TODO*///	sel = selection - 1 ;
/*TODO*///
/*TODO*///	sprintf(buf, "%s %03d", ui_getstring (UI_loadcard), mcd_number);
/*TODO*///	menuitem[menutotal++] = buf;
/*TODO*///	menuitem[menutotal++] = ui_getstring (UI_ejectcard);
/*TODO*///	menuitem[menutotal++] = ui_getstring (UI_createcard);
/*TODO*///	menuitem[menutotal++] = ui_getstring (UI_resetcard);
/*TODO*///	menuitem[menutotal++] = ui_getstring (UI_returntomain);
/*TODO*///	menuitem[menutotal] = 0;
/*TODO*///
/*TODO*///	if (mcd_action!=0)
/*TODO*///	{
/*TODO*///		strcpy (buf2, "\n");
/*TODO*///
/*TODO*///		switch(mcd_action)
/*TODO*///		{
/*TODO*///			case 1:
/*TODO*///				strcat (buf2, ui_getstring (UI_loadfailed));
/*TODO*///				break;
/*TODO*///			case 2:
/*TODO*///				strcat (buf2, ui_getstring (UI_loadok));
/*TODO*///				break;
/*TODO*///			case 3:
/*TODO*///				strcat (buf2, ui_getstring (UI_cardejected));
/*TODO*///				break;
/*TODO*///			case 4:
/*TODO*///				strcat (buf2, ui_getstring (UI_cardcreated));
/*TODO*///				break;
/*TODO*///			case 5:
/*TODO*///				strcat (buf2, ui_getstring (UI_cardcreatedfailed));
/*TODO*///				strcat (buf2, "\n");
/*TODO*///				strcat (buf2, ui_getstring (UI_cardcreatedfailed2));
/*TODO*///				break;
/*TODO*///			default:
/*TODO*///				strcat (buf2, ui_getstring (UI_carderror));
/*TODO*///				break;
/*TODO*///		}
/*TODO*///
/*TODO*///		strcat (buf2, "\n\n");
/*TODO*///		ui_displaymessagewindow(bitmap,buf2);
/*TODO*///		if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///			mcd_action = 0;
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		ui_displaymenu(bitmap,menuitem,0,0,sel,0);
/*TODO*///
/*TODO*///		if (input_ui_pressed_repeat(IPT_UI_RIGHT,8))
/*TODO*///			mcd_number = (mcd_number + 1) % 1000;
/*TODO*///
/*TODO*///		if (input_ui_pressed_repeat(IPT_UI_LEFT,8))
/*TODO*///			mcd_number = (mcd_number + 999) % 1000;
/*TODO*///
/*TODO*///		if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///			sel = (sel + 1) % menutotal;
/*TODO*///
/*TODO*///		if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///			sel = (sel + menutotal - 1) % menutotal;
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///		{
/*TODO*///			switch(sel)
/*TODO*///			{
/*TODO*///			case 0:
/*TODO*///				neogeo_memcard_eject();
/*TODO*///				if (neogeo_memcard_load(mcd_number))
/*TODO*///				{
/*TODO*///					memcard_status=1;
/*TODO*///					memcard_number=mcd_number;
/*TODO*///					mcd_action = 2;
/*TODO*///				}
/*TODO*///				else
/*TODO*///					mcd_action = 1;
/*TODO*///				break;
/*TODO*///			case 1:
/*TODO*///				neogeo_memcard_eject();
/*TODO*///				mcd_action = 3;
/*TODO*///				break;
/*TODO*///			case 2:
/*TODO*///				if (neogeo_memcard_create(mcd_number))
/*TODO*///					mcd_action = 4;
/*TODO*///				else
/*TODO*///					mcd_action = 5;
/*TODO*///				break;
/*TODO*///			case 3:
/*TODO*///				memcard_manager=1;
/*TODO*///				sel=-2;
/*TODO*///				machine_reset();
/*TODO*///				break;
/*TODO*///			case 4:
/*TODO*///				sel=-1;
/*TODO*///				break;
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///			sel = -1;
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///			sel = -2;
/*TODO*///
/*TODO*///		if (sel == -1 || sel == -2)
/*TODO*///		{
/*TODO*///			schedule_full_refresh();
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///#endif
/*TODO*///#endif
/*TODO*///#endif
/*TODO*///
    public static final int UI_SWITCH = 0;
    public static final int UI_DEFCODE = 1;
    public static final int UI_CODE = 2;
    public static final int UI_ANALOG = 3;
    public static final int UI_CALIBRATE = 4;
    public static final int UI_STATS = 5;
    public static final int UI_GAMEINFO = 6;
    public static final int UI_HISTORY = 7;
    public static final int UI_CHEAT = 8;
    public static final int UI_RESET = 9;
    public static final int UI_MEMCARD = 10;
    public static final int UI_EXIT = 11;

    public static final int MAX_SETUPMENU_ITEMS = 20;
    static String[] menu_item = new String[MAX_SETUPMENU_ITEMS];
    static int[] menu_action = new int[MAX_SETUPMENU_ITEMS];
    static int menu_total;

    static void setup_menu_init() {
        menu_total = 0;

        menu_item[menu_total] = ui_getstring(UI_inputgeneral);
        menu_action[menu_total++] = UI_DEFCODE;
        menu_item[menu_total] = ui_getstring(UI_inputspecific);
        menu_action[menu_total++] = UI_CODE;
        menu_item[menu_total] = ui_getstring(UI_dipswitches);
        menu_action[menu_total++] = UI_SWITCH;

        /*TODO*///	/* Determine if there are any analog controls */
/*TODO*///	{
/*TODO*///		struct InputPort *in;
/*TODO*///		int num;
/*TODO*///
/*TODO*///		in = Machine->input_ports;
/*TODO*///
/*TODO*///		num = 0;
/*TODO*///		while (in->type != IPT_END)
/*TODO*///		{
/*TODO*///			if (((in->type & 0xff) > IPT_ANALOG_START) && ((in->type & 0xff) < IPT_ANALOG_END)
/*TODO*///					&& !(!options.cheat && (in->type & IPF_CHEAT)))
/*TODO*///				num++;
/*TODO*///			in++;
/*TODO*///		}
/*TODO*///
/*TODO*///		if (num != 0)
/*TODO*///		{
/*TODO*///			menu_item[menu_total] = ui_getstring (UI_analogcontrols); menu_action[menu_total++] = UI_ANALOG;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* Joystick calibration possible? */
/*TODO*///	if ((osd_joystick_needs_calibration()) != 0)
/*TODO*///	{
/*TODO*///		menu_item[menu_total] = ui_getstring (UI_calibrate); menu_action[menu_total++] = UI_CALIBRATE;
/*TODO*///	}
        menu_item[menu_total] = ui_getstring(UI_bookkeeping);
        menu_action[menu_total++] = UI_STATS;
        menu_item[menu_total] = ui_getstring(UI_gameinfo);
        menu_action[menu_total++] = UI_GAMEINFO;
        menu_item[menu_total] = ui_getstring(UI_history);
        menu_action[menu_total++] = UI_HISTORY;

        if (options.cheat != 0) {
            menu_item[menu_total] = ui_getstring(UI_cheat);
            menu_action[menu_total++] = UI_CHEAT;
        }
        /*TODO*///	if (Machine->gamedrv->clone_of == &driver_neogeo ||
/*TODO*///			(Machine->gamedrv->clone_of &&
/*TODO*///				Machine->gamedrv->clone_of->clone_of == &driver_neogeo))
/*TODO*///	{
/*TODO*///		menu_item[menu_total] = ui_getstring (UI_memorycard); menu_action[menu_total++] = UI_MEMCARD;
/*TODO*///	}
        menu_item[menu_total] = ui_getstring(UI_resetgame);
        menu_action[menu_total++] = UI_RESET;
        menu_item[menu_total] = ui_getstring(UI_returntogame);
        menu_action[menu_total++] = UI_EXIT;
        menu_item[menu_total] = null;/* terminate array */
    }

    static int menu_lastselected = 0;

    public static int setup_menu(mame_bitmap bitmap, int selected) {
        int sel, res = -1;

        if (selected == -1) {
            sel = menu_lastselected;
        } else {
            sel = selected - 1;
        }

        if (sel > SEL_MASK) {
            switch (menu_action[sel & SEL_MASK]) {
                case UI_SWITCH:
                    res = setdipswitches(bitmap, sel >> SEL_BITS);
                    break;
                case UI_DEFCODE:
                    res = setdefcodesettings(bitmap, sel >> SEL_BITS);
                    break;
                case UI_CODE:
                    res = setcodesettings(bitmap, sel >> SEL_BITS);
                    break;
                /*TODO*///			case UI_ANALOG:
/*TODO*///				res = settraksettings(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
/*TODO*///			case UI_CALIBRATE:
/*TODO*///				res = calibratejoysticks(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
                case UI_STATS:
                    res = mame_stats(bitmap, sel >> SEL_BITS);
                    break;
                case UI_GAMEINFO:
                    res = displaygameinfo(bitmap, sel >> SEL_BITS);
                    break;
                case UI_HISTORY:
                    res = displayhistory(bitmap, sel >> SEL_BITS);
                    break;
                case UI_CHEAT:
                    res = cheat_menu(bitmap, sel >> SEL_BITS);
                    break;
                /*TODO*///			case UI_MEMCARD:
/*TODO*///				res = memcard_menu(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
            }

            if (res == -1) {
                menu_lastselected = sel;
                sel = -1;
            } else {
                sel = (sel & SEL_MASK) | (res << SEL_BITS);
            }

            return sel + 1;
        }

        ui_displaymenu(bitmap, menu_item, null, null, sel, 0);

        if (input_ui_pressed_repeat(IPT_UI_DOWN, 8) != 0) {
            sel = (sel + 1) % menu_total;
        }

        if (input_ui_pressed_repeat(IPT_UI_UP, 8) != 0) {
            sel = (sel + menu_total - 1) % menu_total;
        }

        if (input_ui_pressed(IPT_UI_SELECT) != 0) {
            switch (menu_action[sel]) {
                case UI_SWITCH:
                case UI_DEFCODE:
                case UI_CODE:
                case UI_ANALOG:
                case UI_CALIBRATE:
                case UI_STATS:
                case UI_GAMEINFO:
                case UI_HISTORY:
                case UI_CHEAT:
                case UI_MEMCARD:
                    sel |= 1 << SEL_BITS;
                    schedule_full_refresh();
                    break;

                case UI_RESET:
                    machine_reset();
                    break;

                case UI_EXIT:
                    menu_lastselected = 0;
                    sel = -1;
                    break;
            }
        }

        if (input_ui_pressed(IPT_UI_CANCEL) != 0
                || input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
            menu_lastselected = sel;
            sel = -1;
        }

        if (sel == -1) {
            schedule_full_refresh();
        }

        return sel + 1;
    }

    /**
     * ******************************************************************
     * <p>
     * start of On Screen Display handling
     * <p>
     * *******************************************************************
     */
    public static void displayosd(mame_bitmap bitmap, String text, int percentage, int default_percentage) {
        DisplayText[] dt = DisplayText.create(2);
        int avail;

        avail = (Machine.uiwidth / Machine.uifontwidth) * 19 / 20;

        ui_drawbox(bitmap, (Machine.uiwidth - Machine.uifontwidth * avail) / 2,
                (Machine.uiheight - 7 * Machine.uifontheight / 2),
                avail * Machine.uifontwidth,
                3 * Machine.uifontheight);

        avail--;

        drawbar(bitmap, (Machine.uiwidth - Machine.uifontwidth * avail) / 2,
                (Machine.uiheight - 3 * Machine.uifontheight),
                avail * Machine.uifontwidth,
                Machine.uifontheight,
                percentage, default_percentage);

        dt[0].text = text;
        dt[0].color = UI_COLOR_NORMAL;
        dt[0].x = (Machine.uiwidth - Machine.uifontwidth * strlen(text)) / 2;
        dt[0].y = (Machine.uiheight - 2 * Machine.uifontheight) + 2;
        dt[1].text = null;/* terminate array */
        displaytext(bitmap, dt);
    }

    public static onscrd_fncPtr onscrd_volume = new onscrd_fncPtr() {
        public void handler(mame_bitmap bitmap, int increment, int arg) {
            String buf;
            int attenuation;

            if (increment != 0) {
                attenuation = osd_get_mastervolume();
                attenuation += increment;
                if (attenuation > 0) {
                    attenuation = 0;
                }
                if (attenuation < -32) {
                    attenuation = -32;
                }
                osd_set_mastervolume(attenuation);
            }
            attenuation = osd_get_mastervolume();

            buf = sprintf("%s %3ddB", ui_getstring(UI_volume), attenuation);
            displayosd(bitmap, buf, 100 * (attenuation + 32) / 32, 100);
        }
    };

    /*TODO*///static void onscrd_mixervol(struct mame_bitmap *bitmap,int increment,int arg)
/*TODO*///{
/*TODO*///	static void *driver = 0;
/*TODO*///	char buf[40];
/*TODO*///	int volume,ch;
/*TODO*///	int doallchannels = 0;
/*TODO*///	int proportional = 0;
/*TODO*///
/*TODO*///
/*TODO*///	if (code_pressed(KEYCODE_LSHIFT) || code_pressed(KEYCODE_RSHIFT))
/*TODO*///		doallchannels = 1;
/*TODO*///	if (!code_pressed(KEYCODE_LCONTROL) && !code_pressed(KEYCODE_RCONTROL))
/*TODO*///		increment *= 5;
/*TODO*///	if (code_pressed(KEYCODE_LALT) || code_pressed(KEYCODE_RALT))
/*TODO*///		proportional = 1;
/*TODO*///
/*TODO*///	if (increment)
/*TODO*///	{
/*TODO*///		if (proportional)
/*TODO*///		{
/*TODO*///			static int old_vol[MIXER_MAX_CHANNELS];
/*TODO*///			float ratio = 1.0;
/*TODO*///			int overflow = 0;
/*TODO*///
/*TODO*///			if (driver != Machine->drv)
/*TODO*///			{
/*TODO*///				driver = (void *)Machine->drv;
/*TODO*///				for (ch = 0; ch < MIXER_MAX_CHANNELS; ch++)
/*TODO*///					old_vol[ch] = mixer_get_mixing_level(ch);
/*TODO*///			}
/*TODO*///
/*TODO*///			volume = mixer_get_mixing_level(arg);
/*TODO*///			if (old_vol[arg])
/*TODO*///				ratio = (float)(volume + increment) / (float)old_vol[arg];
/*TODO*///
/*TODO*///			for (ch = 0; ch < MIXER_MAX_CHANNELS; ch++)
/*TODO*///			{
/*TODO*///				if (mixer_get_name(ch) != 0)
/*TODO*///				{
/*TODO*///					volume = ratio * old_vol[ch];
/*TODO*///					if (volume < 0 || volume > 100)
/*TODO*///						overflow = 1;
/*TODO*///				}
/*TODO*///			}
/*TODO*///
/*TODO*///			if (!overflow)
/*TODO*///			{
/*TODO*///				for (ch = 0; ch < MIXER_MAX_CHANNELS; ch++)
/*TODO*///				{
/*TODO*///					volume = ratio * old_vol[ch];
/*TODO*///					mixer_set_mixing_level(ch,volume);
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			driver = 0; /* force reset of saved volumes */
/*TODO*///
/*TODO*///			volume = mixer_get_mixing_level(arg);
/*TODO*///			volume += increment;
/*TODO*///			if (volume > 100) volume = 100;
/*TODO*///			if (volume < 0) volume = 0;
/*TODO*///
/*TODO*///			if (doallchannels)
/*TODO*///			{
/*TODO*///				for (ch = 0;ch < MIXER_MAX_CHANNELS;ch++)
/*TODO*///					mixer_set_mixing_level(ch,volume);
/*TODO*///			}
/*TODO*///			else
/*TODO*///				mixer_set_mixing_level(arg,volume);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	volume = mixer_get_mixing_level(arg);
/*TODO*///
/*TODO*///	if (proportional)
/*TODO*///		sprintf(buf,"%s %s %3d%%", ui_getstring (UI_allchannels), ui_getstring (UI_relative), volume);
/*TODO*///	else if (doallchannels)
/*TODO*///		sprintf(buf,"%s %s %3d%%", ui_getstring (UI_allchannels), ui_getstring (UI_volume), volume);
/*TODO*///	else
/*TODO*///		sprintf(buf,"%s %s %3d%%",mixer_get_name(arg), ui_getstring (UI_volume), volume);
/*TODO*///	displayosd(bitmap,buf,volume,mixer_get_default_mixing_level(arg));
/*TODO*///}
    public static onscrd_fncPtr onscrd_brightness = new onscrd_fncPtr() {
        public void handler(mame_bitmap bitmap, int increment, int arg) {
            String buf;
            int brightness;

            if (increment != 0) {
                brightness = osd_get_brightness();
                brightness += 5 * increment;
                if (brightness < 0) {
                    brightness = 0;
                }
                if (brightness > 100) {
                    brightness = 100;
                }
                osd_set_brightness(brightness);
            }
            brightness = osd_get_brightness();
            buf = sprintf("%s %3d%%", ui_getstring(UI_brightness), brightness);
            displayosd(bitmap, buf, brightness, 100);
        }
    };

    /*TODO*///static void onscrd_gamma(struct mame_bitmap *bitmap,int increment,int arg)
/*TODO*///{
/*TODO*///	char buf[20];
/*TODO*///	float gamma_correction;
/*TODO*///
/*TODO*///	if (increment)
/*TODO*///	{
/*TODO*///		gamma_correction = osd_get_gamma();
/*TODO*///
/*TODO*///		gamma_correction += 0.05 * increment;
/*TODO*///		if (gamma_correction < 0.5) gamma_correction = 0.5;
/*TODO*///		if (gamma_correction > 2.0) gamma_correction = 2.0;
/*TODO*///
/*TODO*///		osd_set_gamma(gamma_correction);
/*TODO*///	}
/*TODO*///	gamma_correction = osd_get_gamma();
/*TODO*///
/*TODO*///	sprintf(buf,"%s %1.2f", ui_getstring (UI_gamma), gamma_correction);
/*TODO*///	displayosd(bitmap,buf,100*(gamma_correction-0.5)/(2.0-0.5),100*(1.0-0.5)/(2.0-0.5));
/*TODO*///}
/*TODO*///
/*TODO*///static void onscrd_vector_flicker(struct mame_bitmap *bitmap,int increment,int arg)
/*TODO*///{
/*TODO*///	char buf[1000];
/*TODO*///	float flicker_correction;
/*TODO*///
/*TODO*///	if (!code_pressed(KEYCODE_LCONTROL) && !code_pressed(KEYCODE_RCONTROL))
/*TODO*///		increment *= 5;
/*TODO*///
/*TODO*///	if (increment)
/*TODO*///	{
/*TODO*///		flicker_correction = vector_get_flicker();
/*TODO*///
/*TODO*///		flicker_correction += increment;
/*TODO*///		if (flicker_correction < 0.0) flicker_correction = 0.0;
/*TODO*///		if (flicker_correction > 100.0) flicker_correction = 100.0;
/*TODO*///
/*TODO*///		vector_set_flicker(flicker_correction);
/*TODO*///	}
/*TODO*///	flicker_correction = vector_get_flicker();
/*TODO*///
/*TODO*///	sprintf(buf,"%s %1.2f", ui_getstring (UI_vectorflicker), flicker_correction);
/*TODO*///	displayosd(bitmap,buf,flicker_correction,0);
/*TODO*///}
/*TODO*///
/*TODO*///static void onscrd_vector_intensity(struct mame_bitmap *bitmap,int increment,int arg)
/*TODO*///{
/*TODO*///	char buf[30];
/*TODO*///	float intensity_correction;
/*TODO*///
/*TODO*///	if (increment)
/*TODO*///	{
/*TODO*///		intensity_correction = vector_get_intensity();
/*TODO*///
/*TODO*///		intensity_correction += 0.05 * increment;
/*TODO*///		if (intensity_correction < 0.5) intensity_correction = 0.5;
/*TODO*///		if (intensity_correction > 3.0) intensity_correction = 3.0;
/*TODO*///
/*TODO*///		vector_set_intensity(intensity_correction);
/*TODO*///	}
/*TODO*///	intensity_correction = vector_get_intensity();
/*TODO*///
/*TODO*///	sprintf(buf,"%s %1.2f", ui_getstring (UI_vectorintensity), intensity_correction);
/*TODO*///	displayosd(bitmap,buf,100*(intensity_correction-0.5)/(3.0-0.5),100*(1.5-0.5)/(3.0-0.5));
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///static void onscrd_overclock(struct mame_bitmap *bitmap,int increment,int arg)
/*TODO*///{
/*TODO*///	char buf[30];
/*TODO*///	double overclock;
/*TODO*///	int cpu, doallcpus = 0, oc;
/*TODO*///
/*TODO*///	if (code_pressed(KEYCODE_LSHIFT) || code_pressed(KEYCODE_RSHIFT))
/*TODO*///		doallcpus = 1;
/*TODO*///	if (!code_pressed(KEYCODE_LCONTROL) && !code_pressed(KEYCODE_RCONTROL))
/*TODO*///		increment *= 5;
/*TODO*///	if( increment )
/*TODO*///	{
/*TODO*///		overclock = timer_get_overclock(arg);
/*TODO*///		overclock += 0.01 * increment;
/*TODO*///		if (overclock < 0.01) overclock = 0.01;
/*TODO*///		if (overclock > 2.0) overclock = 2.0;
/*TODO*///		if( doallcpus )
/*TODO*///			for( cpu = 0; cpu < cpu_gettotalcpu(); cpu++ )
/*TODO*///				timer_set_overclock(cpu, overclock);
/*TODO*///		else
/*TODO*///			timer_set_overclock(arg, overclock);
/*TODO*///	}
/*TODO*///
/*TODO*///	oc = 100 * timer_get_overclock(arg) + 0.5;
/*TODO*///
/*TODO*///	if( doallcpus )
/*TODO*///		sprintf(buf,"%s %s %3d%%", ui_getstring (UI_allcpus), ui_getstring (UI_overclock), oc);
/*TODO*///	else
/*TODO*///		sprintf(buf,"%s %s%d %3d%%", ui_getstring (UI_overclock), ui_getstring (UI_cpu), arg, oc);
/*TODO*///	displayosd(bitmap,buf,oc/2,100/2);
/*TODO*///}
/*TODO*///
    public static final int MAX_OSD_ITEMS = 30;

    public static abstract interface onscrd_fncPtr {

        public abstract void handler(mame_bitmap bitmap, int increment, int arg);

    }

    public static onscrd_fncPtr[] onscrd_fnc = new onscrd_fncPtr[MAX_OSD_ITEMS];
    public static int[] onscrd_arg = new int[MAX_OSD_ITEMS];
    static int onscrd_total_items;

    public static void onscrd_init() {
        int item, ch;

        item = 0;

        if (Machine.sample_rate != 0) {
            onscrd_fnc[item] = onscrd_volume;
            onscrd_arg[item] = 0;
            item++;
            /*TODO*///
/*TODO*///		for (ch = 0;ch < MIXER_MAX_CHANNELS;ch++)
/*TODO*///		{
/*TODO*///			if (mixer_get_name(ch) != 0)
/*TODO*///			{
/*TODO*///				onscrd_fnc[item] = onscrd_mixervol;
/*TODO*///				onscrd_arg[item] = ch;
/*TODO*///				item++;
/*TODO*///			}
/*TODO*///		}
        }
        /*TODO*///
/*TODO*///	if (options.cheat)
/*TODO*///	{
/*TODO*///		for (ch = 0;ch < cpu_gettotalcpu();ch++)
/*TODO*///		{
/*TODO*///			onscrd_fnc[item] = onscrd_overclock;
/*TODO*///			onscrd_arg[item] = ch;
/*TODO*///			item++;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
        onscrd_fnc[item] = onscrd_brightness;
        onscrd_arg[item] = 0;
        item++;
        /*TODO*///
/*TODO*///	onscrd_fnc[item] = onscrd_gamma;
/*TODO*///	onscrd_arg[item] = 0;
/*TODO*///	item++;
/*TODO*///
/*TODO*///	if (Machine->drv->video_attributes & VIDEO_TYPE_VECTOR)
/*TODO*///	{
/*TODO*///		onscrd_fnc[item] = onscrd_vector_flicker;
/*TODO*///		onscrd_arg[item] = 0;
/*TODO*///		item++;
/*TODO*///
/*TODO*///		onscrd_fnc[item] = onscrd_vector_intensity;
/*TODO*///		onscrd_arg[item] = 0;
/*TODO*///		item++;
/*TODO*///	}
/*TODO*///
        onscrd_total_items = item;
    }
    static int lastselected = 0;

    public static int on_screen_display(mame_bitmap bitmap, int selected) {
        int increment, sel;

        if (selected == -1) {
            sel = lastselected;
        } else {
            sel = selected - 1;
        }

        increment = 0;
        if (input_ui_pressed_repeat(IPT_UI_LEFT, 8) != 0) {
            increment = -1;
        }
        if (input_ui_pressed_repeat(IPT_UI_RIGHT, 8) != 0) {
            increment = 1;
        }
        if (input_ui_pressed_repeat(IPT_UI_DOWN, 8) != 0) {
            sel = (sel + 1) % onscrd_total_items;
        }
        if (input_ui_pressed_repeat(IPT_UI_UP, 8) != 0) {
            sel = (sel + onscrd_total_items - 1) % onscrd_total_items;
        }

        onscrd_fnc[sel].handler(bitmap, increment, onscrd_arg[sel]);

        lastselected = sel;

        if (input_ui_pressed(IPT_UI_ON_SCREEN_DISPLAY) != 0) {
            sel = -1;

            schedule_full_refresh();
        }

        return sel + 1;
    }

    /**
     * ******************************************************************
     * <p>
     * end of On Screen Display handling
     * <p>
     * *******************************************************************
     */
    public static void displaymessage(mame_bitmap bitmap, String text) {
        DisplayText[] dt = DisplayText.create(2);
        int avail;

        if (Machine.uiwidth < Machine.uifontwidth * strlen(text)) {
            ui_displaymessagewindow(bitmap, text);
            return;
        }

        avail = strlen(text) + 2;

        ui_drawbox(bitmap, (Machine.uiwidth - Machine.uifontwidth * avail) / 2,
                Machine.uiheight - 3 * Machine.uifontheight,
                avail * Machine.uifontwidth,
                2 * Machine.uifontheight);

        dt[0].text = text;
        dt[0].color = UI_COLOR_NORMAL;
        dt[0].x = (Machine.uiwidth - Machine.uifontwidth * strlen(text)) / 2;
        dt[0].y = Machine.uiheight - 5 * Machine.uifontheight / 2;
        dt[1].text = null;/* terminate array */
        displaytext(bitmap, dt);
    }

    public static String messagetext;
    public static int messagecounter;

    public static void usrintf_showmessage(String text, Object... arg) {
        messagetext = sprintf(text, arg);
        messagecounter = (int) (2 * Machine.drv.frames_per_second);
    }

    public static void usrintf_showmessage_secs(int seconds, String text, Object... arg) {
        messagetext = sprintf(text, arg);
        messagecounter = (int) (seconds * Machine.drv.frames_per_second);
    }

    public static int handle_user_interface(mame_bitmap bitmap) {
        /*TODO*///	int request_loadsave = LOADSAVE_NONE;
/*TODO*///

        /* if the user pressed F12, save the screen to a file */
        if (input_ui_pressed(IPT_UI_SNAPSHOT) != 0) {
            osd_save_snapshot(bitmap);
        }

        /* This call is for the cheat, it must be called once a frame */
        if (options.cheat != 0) {
            DoCheat(bitmap);
        }

        /* if the user pressed ESC, stop the emulation */
 /* but don't quit if the setup menu is on screen */
        if (setup_selected == 0 && input_ui_pressed(IPT_UI_CANCEL) != 0) {
            return 1;
        }

        if (setup_selected == 0 && input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
            setup_selected = -1;
            if (osd_selected != 0) {
                osd_selected = 0;/* disable on screen display */
                schedule_full_refresh();
            }
        }
        if (setup_selected != 0) {
            setup_selected = setup_menu(bitmap, setup_selected);
        }

        if (osd_selected == 0 && input_ui_pressed(IPT_UI_ON_SCREEN_DISPLAY) != 0) {
            osd_selected = -1;
            if (setup_selected != 0) {
                setup_selected = 0;
                /* disable setup menu */
                schedule_full_refresh();
            }
        }
        if (osd_selected != 0) {
            osd_selected = on_screen_display(bitmap, osd_selected);
        }

        /* if the user pressed F3, reset the emulation */
        if (input_ui_pressed(IPT_UI_RESET_MACHINE) != 0) {
            machine_reset();
        }

        /*TODO*///	if (input_ui_pressed(IPT_UI_SAVE_STATE))
/*TODO*///		request_loadsave = LOADSAVE_SAVE;
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_LOAD_STATE))
/*TODO*///		request_loadsave = LOADSAVE_LOAD;
/*TODO*///
/*TODO*///	if (request_loadsave != LOADSAVE_NONE)
/*TODO*///	{
/*TODO*///		int file = 0;
/*TODO*///
/*TODO*///		osd_sound_enable(0);
/*TODO*///		osd_pause(1);
/*TODO*///
/*TODO*///		do
/*TODO*///		{
/*TODO*///			InputCode code;
/*TODO*///
/*TODO*///			if (request_loadsave == LOADSAVE_SAVE)
/*TODO*///				displaymessage(bitmap, "Select position to save to");
/*TODO*///			else
/*TODO*///				displaymessage(bitmap, "Select position to load from");
/*TODO*///
/*TODO*///			update_video_and_audio();
/*TODO*///
/*TODO*///			if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///				break;
/*TODO*///
/*TODO*///			code = code_read_async();
/*TODO*///			if (code != CODE_NONE)
/*TODO*///			{
/*TODO*///				if (code >= KEYCODE_A && code <= KEYCODE_Z)
/*TODO*///					file = 'a' + (code - KEYCODE_A);
/*TODO*///				else if (code >= KEYCODE_0 && code <= KEYCODE_9)
/*TODO*///					file = '0' + (code - KEYCODE_0);
/*TODO*///				else if (code >= KEYCODE_0_PAD && code <= KEYCODE_9_PAD)
/*TODO*///					file = '0' + (code - KEYCODE_0);
/*TODO*///			}
/*TODO*///		}
/*TODO*///		while (!file);
/*TODO*///
/*TODO*///		osd_pause(0);
/*TODO*///		osd_sound_enable(1);
/*TODO*///
/*TODO*///		if (file > 0)
/*TODO*///		{
/*TODO*///			if (request_loadsave == LOADSAVE_SAVE)
/*TODO*///				usrintf_showmessage("Save to position %c", file);
/*TODO*///			else
/*TODO*///				usrintf_showmessage("Load from position %c", file);
/*TODO*///			cpu_loadsave_schedule(request_loadsave, file);
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			if (request_loadsave == LOADSAVE_SAVE)
/*TODO*///				usrintf_showmessage("Save cancelled");
/*TODO*///			else
/*TODO*///				usrintf_showmessage("Load cancelled");
/*TODO*///		}
/*TODO*///	}
        if (single_step != 0 || input_ui_pressed(IPT_UI_PAUSE) != 0) /* pause the game */ {
            /*		osd_selected = 0;	   disable on screen display, since we are going   */
 /* to change parameters affected by it */

            if (single_step == 0) {
                osd_sound_enable(0);
                osd_pause(1);
            }

            while (input_ui_pressed(IPT_UI_PAUSE) == 0) {
                if (osd_skip_this_frame() == 0) {
                    /* keep calling vh_screenrefresh() while paused so we can stuff */
 /* debug code in there */
                    draw_screen();
                }

                if (input_ui_pressed(IPT_UI_SNAPSHOT) != 0) {
                    osd_save_snapshot(bitmap);
                }

                if (setup_selected == 0 && input_ui_pressed(IPT_UI_CANCEL) != 0) {
                    return 1;
                }

                if (setup_selected == 0 && input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
                    setup_selected = -1;
                    if (osd_selected != 0) {
                        osd_selected = 0;
                        /* disable on screen display */
                        schedule_full_refresh();
                    }
                }
                if (setup_selected != 0) {
                    setup_selected = setup_menu(bitmap, setup_selected);
                }

                if (osd_selected == 0 && input_ui_pressed(IPT_UI_ON_SCREEN_DISPLAY) != 0) {
                    osd_selected = -1;
                    if (setup_selected != 0) {
                        setup_selected = 0;
                        /* disable setup menu */
                        schedule_full_refresh();
                    }
                }
                if (osd_selected != 0) {
                    osd_selected = on_screen_display(bitmap, osd_selected);
                }

                if (options.cheat != 0) {
                    DisplayWatches(bitmap);
                }

                /* show popup message if any */
                if (messagecounter > 0) {
                    displaymessage(bitmap, messagetext);
                }

                update_video_and_audio();
            }

            if (code_pressed(KEYCODE_LSHIFT) != 0 || code_pressed(KEYCODE_RSHIFT) != 0) {
                single_step = 1;
            } else {
                single_step = 0;
                osd_pause(0);
                osd_sound_enable(1);
            }
        }


        /* show popup message if any */
        if (messagecounter > 0) {
            displaymessage(bitmap, messagetext);

            if (--messagecounter == 0) {
                schedule_full_refresh();
            }
        }

        /* if the user pressed F4, show the character set */
        if (input_ui_pressed(IPT_UI_SHOW_GFX) != 0) {
            osd_sound_enable(0);

            showcharset(bitmap);

            osd_sound_enable(1);
        }

        return 0;
    }

    public static void init_user_interface() {
        snapno = 0;/* reset snapshot counter */

        setup_menu_init();
        setup_selected = 0;

        onscrd_init();
        osd_selected = 0;

        jukebox_selected = -1;

        single_step = 0;

        orientation_count = 0;
    }

    public static int onscrd_active() {
        return osd_selected;
    }

    public static int setup_active() {
        return setup_selected;
    }
}
