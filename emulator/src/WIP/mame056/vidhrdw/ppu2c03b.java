/**
 * ported to v0.56
 */
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;
import static mame056.usrintrf.*;
import static mame056.memoryH.*;
import static mame056.memory.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static arcadeflex036.osdepend.*;
import static common.libc.cstring.memcpy;
import static common.ptr.*;
import static common.subArrays.*;
import static mame056.common.*;
import static mame056.cpuexec.*;
import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.inptport.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;
import static WIP.mame056.vidhrdw.ppu2c03bH.*;

public class ppu2c03b {

    /* constant definitions */
    public static final int BOTTOM_VISIBLE_SCANLINE = 239;		/* The bottommost visible scanline */
    public static final int NMI_SCANLINE = 244;		/* 244 times Bayou Billy perfectly */
    public static final int SCANLINES_PER_FRAME = 262;		/* Total scanlines to account for per frame */
    public static final int VISIBLE_SCREEN_WIDTH = (32 * 8);	/* Visible screen width */
    public static final int VISIBLE_SCREEN_HEIGHT = (30 * 8);	/* Visible screen height */
    public static final int VIDEORAM_SIZE = 0x4000;	/* videoram size */
    public static final int SPRITERAM_SIZE = 0x100;/* spriteram size */
    public static final int CHARGEN_NUM_CHARS = 512;	/* max number of characters handled by the chargen */

    /* registers definition */
    public static final int PPU_CONTROL0 = 0;
    public static final int PPU_CONTROL1 = 1;
    public static final int PPU_STATUS = 2;
    public static final int PPU_SPRITE_ADDRESS = 3;
    public static final int PPU_SPRITE_DATA = 4;
    public static final int PPU_SCROLL = 5;
    public static final int PPU_ADDRESS = 6;
    public static final int PPU_DATA = 7;
    public static final int PPU_MAX_REG = 8;


    /* bit definitions for (some of) the registers */
    public static final int PPU_CONTROL0_INC = 0x04;
    public static final int PPU_CONTROL0_SPR_SELECT = 0x08;
    public static final int PPU_CONTROL0_CHR_SELECT = 0x10;
    public static final int PPU_CONTROL0_SPRITE_SIZE = 0x20;
    public static final int PPU_CONTROL0_NMI = 0x80;

    public static final int PPU_CONTROL1_DISPLAY_MONO = 0x01;
    public static final int PPU_CONTROL1_BACKGROUND_L8 = 0x02;
    public static final int PPU_CONTROL1_SPRITES_L8 = 0x04;
    public static final int PPU_CONTROL1_BACKGROUND = 0x08;
    public static final int PPU_CONTROL1_SPRITES = 0x10;

    public static final int PPU_STATUS_8SPRITES = 0x20;
    public static final int PPU_STATUS_SPRITE0_HIT = 0x40;
    public static final int PPU_STATUS_VBLANK = 0x80;

    /* default monochromatic colortable */
    static int default_colortable_mono[] =
            {
                    0, 1, 2, 3,
                    0, 1, 2, 3,
                    0, 1, 2, 3,
                    0, 1, 2, 3,
                    0, 1, 2, 3,
                    0, 1, 2, 3,
                    0, 1, 2, 3,
                    0, 1, 2, 3,
            };

    /* our chip state */
    public static class ppu2c03b_chip {
        mame_bitmap bitmap;				/* target bitmap */
        UBytePtr videoram;				/* video ram */
        UBytePtr spriteram;				/* sprite ram */
        int[] colortable_mono;		/* monochromatic color table modified at run time */
        char[] dirtychar;				/* an array flagging dirty characters */
        int chars_are_dirty;		/* master flag to check if theres any dirty character */
        Object scanline_timer;		/* scanline timer */
        int scanline;				/* scanline count */
        ppu2c03b_scanline_cb scanline_callback_proc;	/* optinal scanline callback */
        ppu2c03b_vidaccess_cb vidaccess_callback_proc;/* optinal video access callback */
        int has_videorom;			/* wether we access a video rom or not */
        int videorom_banks;			/* number of banks in the videorom (if available) */
        int[] regs = new int[PPU_MAX_REG];		/* registers */
        int refresh_data;			/* refresh-related */
        int refresh_latch;			/* refresh-related */
        int x_fine;					/* refresh-related */
        int toggle;					/* used to latch hi-lo scroll */
        int add;					/* vram increment amount */
        int videoram_addr;			/* videoram address pointer */
        int videoram_addr_latch;	/* videoram address latch */
        int videoram_data_latch;	/* latched videoram data */
        int tile_page;				/* current tile page */
        int sprite_page;			/* current sprite page */
        int back_color;				/* background color */
        UBytePtr[] ppu_page = new UBytePtr[4];			/* ppu pages */
        int[] nes_vram = new int[8];			/* keep track of 8 .5k vram pages to speed things up */
        int scan_scale;				/* scan scale */
    }

    /* our local copy of the interface */
    static ppu2c03b_interface intf;

    /* chips state - allocated at init time */
    static ppu2c03b_chip[] chips = null;


    /**
     * **********************************
     * <p>
     * PPU Palette Initialization
     * <p>
     * ***********************************
     */
    public static void ppu2c03b_init_palette(int ptr, char[] palette) {

		/* This routine builds a palette using a transformation from */
        /* the YUV (Y, B-Y, R-Y) to the RGB color space */

		/* The NES has a 64 color palette                        */
        /* 16 colors, with 4 luminance levels for each color     */
		/* The 16 colors circle around the YUV color space,      */

        int i, j;

        double R, G, B;

        double tint = .6;
        double hue = 332.0;
        double bright_adjust = 1.0;

        double brightness[][] =
                {
                        {0.50, 0.75, 1.0, 1.0},
                        {0.29, 0.45, 0.73, 0.9},
                        {0, 0.24, 0.47, 0.77}
                };

        double angle[] = {0, 240, 210, 180, 150, 120, 90, 60, 30, 0, 330, 300, 270, 0, 0, 0};
        int p_ptr = ptr;
		/* loop through the 4 intensities */
        for (i = 0; i < 4; i++) {
			/* loop through the 16 colors */
            for (j = 0; j < 16; j++) {
                double sat;
                double y;
                double rad;

                switch (j) {
                    case 0:
                        sat = 0;
                        y = brightness[0][i];
                        break;

                    case 13:
                        sat = 0;
                        y = brightness[2][i];
                        break;

                    case 14:
                    case 15:
                        sat = 0;
                        y = 0;
                        break;

                    default:
                        sat = tint;
                        y = brightness[1][i];
                        break;
                }

                rad = Math.PI * ((angle[j] + hue) / 180.0);

                y *= bright_adjust;

				/* Transform to RGB */
                R = (y + sat * Math.sin(rad)) * 255.0;
                G = (y - (27 / 53) * sat * Math.sin(rad) + (10 / 53) * sat * Math.cos(rad)) * 255.0;
                B = (y - sat * Math.cos(rad)) * 255.0;

				/* Clipping, in case of saturation */
                if (R < 0)
                    R = 0;
                if (R > 255)
                    R = 255;
                if (G < 0)
                    G = 0;
                if (G > 255)
                    G = 255;
                if (B < 0)
                    B = 0;
                if (B > 255)
                    B = 255;

				/* Round, and set the value */

                palette[p_ptr++]= (char) (((int) Math.floor(R + .5)) & 0xFF);
                palette[p_ptr++]= (char) (((int) Math.floor(G + .5)) & 0xFF);
                palette[p_ptr++]= (char) (((int) Math.floor(B + .5)) & 0xFF);
            }
        }
	
		/* color tables are modified at run-time, and are initialized on 'ppu2c03b_reset' */
    }

    /* the charlayout we use for the chargen */
    static GfxLayout ppu_charlayout = new GfxLayout
            (
                    8, 8,	/* 8*8 characters */
                    512,	/* 512 characters - modified at runtime */
                    2,		/* 2 bits per pixel */
                    new int[]{8 * 8, 0},	/* the two bitplanes are separated */
                    new int[]{0, 1, 2, 3, 4, 5, 6, 7},
                    new int[]{0 * 8, 1 * 8, 2 * 8, 3 * 8, 4 * 8, 5 * 8, 6 * 8, 7 * 8},
                    16 * 8	/* every char takes 16 consecutive bytes */
            );

    /**
     * **********************************
     * <p>
     * PPU Initialization and Disposal
     * <p>
     * ***********************************
     */
    public static int ppu2c03b_init(ppu2c03b_interface _interface) {
        int i;
	
		/* keep a local copy of the interface */
        intf = _interface;
	
		/* safety check */
        if (intf.num <= 0)
            return -1;

        chips = new ppu2c03b_chip[intf.num];//malloc( intf.num * sizeof( ppu2c03b_chip ) );

        if (chips == null)
            return -1;
	
		/* intialize our virtual chips */
        for (i = 0; i < intf.num; i++) {
			/* initialize the scanline handling portion */
            chips[i] = new ppu2c03b_chip();
            chips[i].scanline_timer = null;
            chips[i].scanline = 0;
            chips[i].scan_scale = 1;
	
			/* allocate a screen bitmap, videoram and spriteram, a dirtychar array and the monochromatic colortable */
            chips[i].bitmap = bitmap_alloc(VISIBLE_SCREEN_WIDTH, VISIBLE_SCREEN_HEIGHT);
            chips[i].videoram = new UBytePtr(VIDEORAM_SIZE);
            chips[i].spriteram = new UBytePtr(SPRITERAM_SIZE);
            chips[i].dirtychar = new char[CHARGEN_NUM_CHARS];
            chips[i].colortable_mono = new int[default_colortable_mono.length];//malloc( sizeof( default_colortable_mono ) );
	
			/* see if it failed */
            if (chips[i].bitmap == null || chips[i].videoram == null || chips[i].spriteram == null || chips[i].dirtychar == null || chips[i].colortable_mono == null) {
				/* if we did fail, release everything allocated so far and return */
                ppu2c03b_dispose();
                return -1;
            }
	
			/* clear videoram & spriteram */
            memset(chips[i].videoram, 0, VIDEORAM_SIZE);
            memset(chips[i].spriteram, 0, SPRITERAM_SIZE);
	
			/* set all characters dirty */
            memset(chips[i].dirtychar, 1, CHARGEN_NUM_CHARS);
	
			/* initialize the video ROM portion, if available */
            if ((intf.vrom_region[i] != REGION_INVALID) && (memory_region(intf.vrom_region[i]) != null)) {
				/* mark that we have a videorom */
                chips[i].has_videorom = 1;
	
				/* find out how many banks */
                chips[i].videorom_banks = memory_region_length(intf.vrom_region[i]) / 0x2000;
	
				/* tweak the layout accordingly */
                ppu_charlayout.total = chips[i].videorom_banks * CHARGEN_NUM_CHARS;
            } else {
                chips[i].has_videorom = chips[i].videorom_banks = 0;
	
				/* we need to reset this in case of mame running multisession */
                ppu_charlayout.total = CHARGEN_NUM_CHARS;
            }
	
			/* now create the gfx region */
            {
                UBytePtr src = chips[i].has_videorom != 0 ? memory_region(intf.vrom_region[i]) : chips[i].videoram;
                Machine.gfx[intf.gfx_layout_number[i]] = decodegfx(src, ppu_charlayout);

                if (Machine.gfx[intf.gfx_layout_number[i]] == null) {
					/* failed */
                    ppu2c03b_dispose();
                    return -1;
                }

                if (Machine.remapped_colortable != null)
                    Machine.gfx[intf.gfx_layout_number[i]].colortable = new IntArray(Machine.remapped_colortable, intf.color_base[i]);

                Machine.gfx[intf.gfx_layout_number[i]].total_colors = 8;
            }
	
			/* setup our videoram handlers based on mirroring */
            ppu2c03b_set_mirroring(i, intf.mirroring[i]);
        }
	
		/* success */
        return 0;
    }

    public static void ppu2c03b_dispose() {
        int i;
	
		/* clean up */
        if (chips != null) {
	
			/* iterate through the virtual chips and free storage */
            for (i = 0; i < intf.num; i++) {
				/* release the bitmap(s) */
                if (chips[i].bitmap != null)
                    bitmap_free(chips[i].bitmap);
                chips[i].bitmap = null;
	
				/* release the videoram */
                if (chips[i].videoram != null)
                    chips[i].videoram = null;
	
				/* release the spriteram */
                if (chips[i].spriteram != null)
                    chips[i].spriteram = null;
	
				/* release the dirtychar array */
                if (chips[i].dirtychar != null)
                    chips[i].dirtychar = null;
	
				/* release the colortable_mono array */
                if (chips[i].colortable_mono != null)
                    chips[i].colortable_mono = null;
	
				/* release the timer */
                if (chips[i].scanline_timer != null)
                    timer_remove(chips[i].scanline_timer);
                chips[i].scanline_timer = null;
            }
	
			/* dispose our chips states */
            chips = null;
        }
    }

    static void draw_background(int num, char[] line_priority) {
		/* cache some values locally */
        mame_bitmap bitmap = chips[num].bitmap;
        int[] ppu_regs = chips[num].regs;
        int scanline = chips[num].scanline;
        int refresh_data = chips[num].refresh_data;
        int gfx_bank = intf.gfx_layout_number[num];
        int total_elements = Machine.gfx[gfx_bank].total_elements;
        int[] nes_vram = chips[num].nes_vram;
        int tile_page = chips[num].tile_page;
        int char_modulo = Machine.gfx[gfx_bank].char_modulo;
        int line_modulo = Machine.gfx[gfx_bank].line_modulo;
        UBytePtr gfx_data = Machine.gfx[gfx_bank].gfxdata;
        UBytePtr[] ppu_page = chips[num].ppu_page;
        int start_x = (chips[num].x_fine ^ 0x07) - 7;
        int back_pen;

        int scroll_x_coarse, scroll_y_coarse, scroll_y_fine, color_mask;
        int x, tile_index, start, i;

        IntArray color_table;
        IntArray paldata;
        UBytePtr sd;
	
		/* setup the color mask and colortable to use */
        if ((ppu_regs[PPU_CONTROL1] & PPU_CONTROL1_DISPLAY_MONO) != 0) {
            color_mask = 0xf0;
            color_table = new IntArray(chips[num].colortable_mono);
        } else {
            color_mask = 0xff;
            color_table = Machine.gfx[gfx_bank].colortable;
        }
	
		/* cache the background pen */
        back_pen = Machine.pens[(chips[num].back_color & color_mask) + intf.color_base[num]];
	
		/* determine where in the nametable to start drawing from */
		/* based on the current scanline and scroll regs */
        scroll_x_coarse = refresh_data & 0x1f;
        scroll_y_coarse = (refresh_data & 0x3e0) >> 5;
        scroll_y_fine = (refresh_data & 0x7000) >> 12;

        x = scroll_x_coarse;
	
		/* get the tile index */
        tile_index = ((refresh_data & 0xc00) | 0x2000) + scroll_y_coarse * 32;
	
		/* draw the 32 or 33 tiles that make up a line */
        while (start_x < VISIBLE_SCREEN_WIDTH) {
            int color_byte;
            int color_bits;
            int pos;
            int index1;
            int page, page2, address;
            int index2;

            index1 = tile_index + x;
	
			/* Figure out which byte in the color table to use */
            pos = ((index1 & 0x380) >> 4) | ((index1 & 0x1f) >> 2);
            page = (index1 & 0x0c00) >> 10;
            address = 0x3c0 + pos;
            color_byte = ppu_page[page].read(address);
	
			/* figure out which bits in the color table to use */
            color_bits = ((index1 & 0x40) >> 4) + (index1 & 0x02);

            address = index1 & 0x3ff;
            page2 = ppu_page[page].read(address);
            index2 = nes_vram[(page2 >> 6) | tile_page] + (page2 & 0x3f);

            paldata = new IntArray(color_table, 4 * (((color_byte >> color_bits) & 0x03)));
            start = (index2 % total_elements) * char_modulo + scroll_y_fine * line_modulo;
            sd = new UBytePtr(gfx_data, start);
	
			/* render the pixel */
            for (i = 0; i < 8; i++) {
                if ((start_x + i) >= 0 && (start_x + i) < VISIBLE_SCREEN_WIDTH) {
                    if (sd.read(i) != 0) {
                        plot_pixel.handler(bitmap, start_x + i, scanline, paldata.read(sd.read(i)));
                        line_priority[start_x + i] |= 0x02;
                    } else {
                        plot_pixel.handler(bitmap, start_x + i, scanline, back_pen);
                    }
                }
            }

            start_x += 8;
	
			/* move to next tile over and toggle the horizontal name table if necessary */
            x++;
            if (x > 31) {
                x = 0;
                tile_index ^= 0x400;
            }
        }
	
		/* if the left 8 pixels for the background are off, blank 'em */
        if ((ppu_regs[PPU_CONTROL1] & PPU_CONTROL1_BACKGROUND_L8) == 0) {
            for (i = 0; i < 8; i++)
                plot_pixel.handler(bitmap, i, scanline, back_pen);
        }
	
		/* done updating, whew */
    }

    static void draw_sprites(int num, char[] line_priority) {
		/* cache some values locally */
        mame_bitmap bitmap = chips[num].bitmap;
        int scanline = chips[num].scanline;
        int gfx_bank = intf.gfx_layout_number[num];
        int total_elements = Machine.gfx[gfx_bank].total_elements;
        int sprite_page = chips[num].sprite_page;
        int char_modulo = Machine.gfx[gfx_bank].char_modulo;
        int line_modulo = Machine.gfx[gfx_bank].line_modulo;
        UBytePtr sprites = chips[num].spriteram;
        IntArray color_table = Machine.gfx[gfx_bank].colortable;
        UBytePtr gfx_data = Machine.gfx[gfx_bank].gfxdata;
        int[] ppu_regs = chips[num].regs;

        int x, y, i;
        int tile, index1, page;
        int pri;

        int flipx, flipy, color;
        int size;
        int spriteCount = 0;
        int sprite_line;
        int drawn;
        int start;

        IntArray paldata;
        UBytePtr sd;
	
		/* determine if the sprites are 8x8 or 8x16 */
        size = (ppu_regs[PPU_CONTROL0] & PPU_CONTROL0_SPRITE_SIZE) != 0 ? 16 : 8;

        for (i = 0; i < SPRITERAM_SIZE; i += 4) {
            y = sprites.read(i) + 1;
	
			/* if the sprite isn't visible, skip it */
            if ((y + size <= scanline) || (y > scanline))
                continue;
	
			/* clear our drawn flag */
            drawn = 0;

            x = sprites.read(i + 3);
            tile = sprites.read(i + 1);
            color = (sprites.read(i + 2) & 0x03) + 4;
            pri = sprites.read(i + 2) & 0x20;
            flipx = sprites.read(i + 2) & 0x40;
            flipy = sprites.read(i + 2) & 0x80;

            if (size == 16) {
				/* if it's 8x16 and odd-numbered, draw the other half instead */
                if ((tile & 0x01) != 0) {
                    tile &= ~0x01;
                    tile |= 0x100;
                }
				/* note that the sprite page value has no effect on 8x16 sprites */
                page = tile >> 6;
            } else
                page = (tile >> 6) | sprite_page;


            index1 = chips[num].nes_vram[page] + (tile & 0x3f);
	
			/* compute the character's line to draw */
            sprite_line = scanline - y;

            if (flipy != 0)
                sprite_line = (size - 1) - sprite_line;

            paldata = new IntArray(color_table, 4 * color);
            start = (index1 % total_elements) * char_modulo + sprite_line * line_modulo;
            sd = new UBytePtr(gfx_data, start);

            if (pri != 0) {
				/* draw the low-priority sprites */
                int j;

                if (flipx != 0) {
                    for (j = 0; j < 8; j++) {
						/* is this pixel non-transparent? */
                        if (sd.read(7 - j) != 0) {
							/* has the background (or another sprite) already been drawn here? */
                            if ((x + j) < VISIBLE_SCREEN_WIDTH) {
                                if (line_priority[x + j] == 0) {
								/* no, draw */
                                    if ((x + j) < VISIBLE_SCREEN_WIDTH)
                                        plot_pixel.handler(bitmap, x + j, scanline, paldata.read(sd.read(7 - j)));
                                    drawn = 1;
                                }
                            }
							/* indicate that a sprite was drawn at this location, even if it's not seen */
                            if ((x + j) < VISIBLE_SCREEN_WIDTH)
                                line_priority[x + j] |= 0x01;
	
							/* set the "sprite 0 hit" flag if appropriate */
                            if (i == 0)
                                ppu_regs[PPU_STATUS] |= PPU_STATUS_SPRITE0_HIT;
                        }
                    }
                } else {
                    for (j = 0; j < 8; j++) {
						/* is this pixel non-transparent? */
                        if (sd.read(j) != 0) {
							/* has the background (or another sprite) already been drawn here? */
                            if ((x + j) < VISIBLE_SCREEN_WIDTH) {
                                if (line_priority[x + j] == 0) {
                                    /* no, draw */
                                    if ((x + j) < VISIBLE_SCREEN_WIDTH)
                                        plot_pixel.handler(bitmap, x + j, scanline, paldata.read(sd.read(j)));
                                    drawn = 1;
                                }
                            }
							/* indicate that a sprite was drawn at this location, even if it's not seen */
                            if ((x + j) < VISIBLE_SCREEN_WIDTH)
                                line_priority[x + j] |= 0x01;
	
							/* set the "sprite 0 hit" flag if appropriate */
                            if (i == 0)
                                ppu_regs[PPU_STATUS] |= PPU_STATUS_SPRITE0_HIT;
                        }
                    }
                }
            } else {
				/* draw the high-priority sprites */
                int j;

                if (flipx != 0) {
                    for (j = 0; j < 8; j++) {
						/* is this pixel non-transparent? */
                        if (sd.read(7 - j) != 0) {
							/* has another sprite been drawn here? */
                            if ((x + j) < VISIBLE_SCREEN_WIDTH) {
                                if ((line_priority[x + j] & 0x01) == 0) {
								/* no, draw */
                                    if ((x + j) < VISIBLE_SCREEN_WIDTH) {
                                        plot_pixel.handler(bitmap, x + j, scanline, paldata.read(sd.read(7 - j)));
                                        line_priority[x + j] |= 0x01;
                                    }
                                    drawn = 1;
                                }
                            }
							/* set the "sprite 0 hit" flag if appropriate */
                            if ((i == 0) && (line_priority[x + j] & 0x02) != 0)
                                ppu_regs[PPU_STATUS] |= PPU_STATUS_SPRITE0_HIT;
                        }
                    }
                } else {
                    for (j = 0; j < 8; j++) {
						/* is this pixel non-transparent? */
                        if (sd.read(j) != 0) {
							/* has another sprite been drawn here? */
                            if ((x + j) < VISIBLE_SCREEN_WIDTH) {
                                if ((line_priority[x + j] & 0x01) == 0) {
								/* no, draw */
                                    if ((x + j) < VISIBLE_SCREEN_WIDTH) {
                                        plot_pixel.handler(bitmap, x + j, scanline, paldata.read(sd.read(j)));
                                        line_priority[x + j] |= 0x01;
                                    }
                                    drawn = 1;
                                }
                            }
	
							/* set the "sprite 0 hit" flag if appropriate */
                            if ((i == 0) && (line_priority[x + j] & 0x02) != 0)
                                ppu_regs[PPU_STATUS] |= PPU_STATUS_SPRITE0_HIT;
                        }
                    }
                }
            }

            if (drawn != 0) {
				/* if there are more than 8 sprites on this line, set the flag */
                spriteCount++;
                if (spriteCount == 8) {
                    ppu_regs[PPU_STATUS] |= PPU_STATUS_8SPRITES;
                    logerror("> 8 sprites, scanline: %d\n", scanline);
	
					/* the real NES only draws up to 8 sprites - the rest should be invisible */
                    break;
                }
            }
        }
    }

    /**
     * **********************************
     * <p>
     * Scanline Rendering and Update
     * <p>
     * ***********************************
     */
    static void render_scanline(int num) {
        char[] line_priority = new char[VISIBLE_SCREEN_WIDTH];
        int[] ppu_regs = chips[num].regs;
        int i;
        int refresh_data = chips[num].refresh_data;

	
		/* clear the line priority for this scanline */
        memset(line_priority, 0, VISIBLE_SCREEN_WIDTH);
	
		/* clear the sprite count for this line */
        ppu_regs[PPU_STATUS] &= ~PPU_STATUS_8SPRITES;
	
		/* see if we need to render the background */
        if ((ppu_regs[PPU_CONTROL1] & PPU_CONTROL1_BACKGROUND) != 0)
            draw_background(num, line_priority);
	
		/* if sprites are hidden in the leftmost column, fake a priority flag to mask them */
        if ((ppu_regs[PPU_CONTROL1] & PPU_CONTROL1_SPRITES_L8) == 0) {
            for (i = 0; i < 8; i++)
                line_priority[i] |= 0x01;
        }
	
		/* if sprites are on, draw them */
        if ((ppu_regs[PPU_CONTROL1] & PPU_CONTROL1_SPRITES) != 0)
            draw_sprites(num, line_priority);
	
		/* increment the fine y-scroll */
        refresh_data += 0x1000;
	
		/* if it's rolled, increment the coarse y-scroll */
        if ((refresh_data & 0x8000) != 0) {
            char tmp;
            tmp = (char) ((refresh_data & 0x03e0) + 0x20);
            refresh_data &= 0x7c1f;
			/* handle bizarro scrolling rollover at the 30th (not 32nd) vertical tile */
            if (tmp == 0x03c0) {
                refresh_data ^= 0x0800;
            } else {
                refresh_data |= (tmp & 0x03e0);
            }
        }

        chips[num].refresh_data = refresh_data;
	
	 	/* done updating, whew */
    }

    static void update_scanline(int num) {
        int scanline = chips[num].scanline;
        int[] ppu_regs = chips[num].regs;

        if (scanline <= BOTTOM_VISIBLE_SCANLINE) {
			/* If background or sprites are enabled, copy the ppu address latch */
            if ((ppu_regs[PPU_CONTROL1] & (PPU_CONTROL1_BACKGROUND | PPU_CONTROL1_SPRITES)) != 0) {
				/* Copy only the scroll x-coarse and the x-overflow bit */
                chips[num].refresh_data &= ~0x041f;
                chips[num].refresh_data |= (chips[num].refresh_latch & 0x041f);
            }

			/* Render this scanline if appropriate */
            if ((ppu_regs[PPU_CONTROL1] & (PPU_CONTROL1_BACKGROUND | PPU_CONTROL1_SPRITES)) != 0)
                render_scanline(num);

            return;
        }

        if (scanline == BOTTOM_VISIBLE_SCANLINE + 1) {
			/* We just entered VBLANK */
            ppu_regs[PPU_STATUS] |= PPU_STATUS_VBLANK;
            return;
        }
	
		/* If NMI's are set to be triggered, go for it */
        if ((scanline == NMI_SCANLINE) && (ppu_regs[PPU_CONTROL0] & PPU_CONTROL0_NMI) != 0) {
            if (intf.handler[num] != null)
                (intf.handler[num]).handler(num);
        }
    }

    public static timer_callback scanline_callback = new timer_callback() {
        public void handler(int num) {

            int[] ppu_regs = chips[num].regs;
            int i;
            int blanked = (ppu_regs[PPU_CONTROL1] & (PPU_CONTROL1_BACKGROUND | PPU_CONTROL1_SPRITES)) == 0 ? 1 : 0;
            int vblank = (ppu_regs[PPU_STATUS] & PPU_STATUS_VBLANK) != 0 ? 1 : 0;
	
	/*	logerror("SCANLINE CALLBACK %d\n",chips[num].scanline); */
	
		/* if a callback is available, call it */
            if (chips[num].scanline_callback_proc != null)
                (chips[num].scanline_callback_proc).handler(num, chips[num].scanline, vblank, blanked);
	
		/* update the scanline that just went by */
            update_scanline(num);
	
		/* increment our scanline count */
            chips[num].scanline++;
	
		/* decode any dirty chars if we're using vram */
	
		/* first, check the master dirty char flag */
            if (chips[num].has_videorom == 0 && chips[num].chars_are_dirty != 0) {
			/* cache some values */
                char[] dirtyarray = chips[num].dirtychar;
                UBytePtr vram = chips[num].videoram;
                GfxElement gfx = Machine.gfx[intf.gfx_layout_number[num]];
	
			/* then iterate and decode */
                for (i = 0; i < CHARGEN_NUM_CHARS; i++) {
                    if (dirtyarray[i] != 0) {
                        decodechar(gfx, i, vram, ppu_charlayout);
                        dirtyarray[i] = 0;
                    }
                }

                chips[num].chars_are_dirty = 0;
            }
	
		/* see if we rolled */
            if (chips[num].scanline >= SCANLINES_PER_FRAME) {
			/* clear the vblank & sprite hit flag */
                ppu_regs[PPU_STATUS] &= ~(PPU_STATUS_VBLANK | PPU_STATUS_SPRITE0_HIT);
	
			/* if background or sprites are enabled, copy the ppu address latch */
                if (blanked == 0)
                    chips[num].refresh_data = chips[num].refresh_latch;
	
			/* reset the scanline count */
                chips[num].scanline = 0;
            }
	
		/* setup our next stop here */
            chips[num].scanline_timer = timer_set(cpu_getscanlinetime(chips[num].scanline * chips[num].scan_scale), num, scanline_callback);
        }
    };

    /**
     * **********************************
     * <p>
     * PPU Reset
     * <p>
     * ***********************************
     */
    public static void ppu2c03b_reset(int num, int scan_scale) {
        int i;
	
		/* check bounds */
        if (num >= intf.num) {
            logerror("PPU(reset): Attempting to access an unmapped chip\n");
            return;
        }
	
		/* check if we have a previously allocated scanline timer */
        if (chips[num].scanline_timer != null)
            timer_remove(chips[num].scanline_timer);
	
		/* reset the scanline count */
        chips[num].scanline = 0;
	
		/* set the scan scale (this is for dual monitor vertical setups) */
        chips[num].scan_scale = scan_scale;
	
		/* allocate the scanline timer - start at scanline 0 */
        chips[num].scanline_timer = timer_set(cpu_getscanlinetime(0), num, scanline_callback);
	
		/* reset the callbacks */
        chips[num].scanline_callback_proc = null;
        chips[num].vidaccess_callback_proc = null;

        for (i = 0; i < PPU_MAX_REG; i++)
            chips[num].regs[i] = 0;
	
		/* initialize the rest of the members */
        chips[num].refresh_data = 0;
        chips[num].refresh_latch = 0;
        chips[num].x_fine = 0;
        chips[num].toggle = 0;
        chips[num].add = 1;
        chips[num].videoram_addr = 0;
        chips[num].videoram_addr_latch = 0;
        chips[num].videoram_data_latch = 0;
        chips[num].tile_page = 0;
        chips[num].sprite_page = 0;
        chips[num].back_color = 0;
        chips[num].chars_are_dirty = 1;
	
		/* initialize the color tables */
        {
            int color_base = intf.color_base[num];

            for (i = 0; i < default_colortable_mono.length; i++)//for( i = 0; i < ( sizeof( default_colortable_mono ) / sizeof( default_colortable_mono[0] ) ); i++ )
            {
				/* monochromatic table */
                chips[num].colortable_mono[i] = Machine.pens[default_colortable_mono[i] + color_base];
	
				/* color table */
                Machine.gfx[intf.gfx_layout_number[num]].colortable.write(i, Machine.pens[default_colortable_mono[i] + color_base]);
            }
        }
	
		/* set the vram bank-switch values to the default */
        for (i = 0; i < 8; i++)
            chips[num].nes_vram[i] = i * 64;

        if (chips[num].has_videorom != 0)
            ppu2c03b_set_videorom_bank(num, 0, 8, 0, 512);
    }


    /**
     * **********************************
     * <p>
     * PPU Registers Read
     * <p>
     * ***********************************
     */
    public static int ppu2c03b_r(int num, int offset) {
        int ret = 0;
	
		/* check bounds */
        if (num >= intf.num) {
            logerror("PPU %d(r): Attempting to access an unmapped chip\n", num);
            return 0;
        }

        if (offset >= PPU_MAX_REG) {
            logerror("PPU %d(r): Attempting to read past the chip\n", num);

            offset &= PPU_MAX_REG - 1;
	/*		return 0; */
        }
	
		/* now, see wich register to read */
        switch (offset) {
            case PPU_STATUS:
                ret = chips[num].regs[PPU_STATUS];
	
				/* this is necessary */
                chips[num].toggle = 0;
	
				/* note that we don't clear the vblank flag - this is correct. */
                break;

            case PPU_SPRITE_DATA:
                ret = chips[num].spriteram.read(chips[num].regs[PPU_SPRITE_ADDRESS]);
                break;

            case PPU_DATA:
                ret = chips[num].videoram_data_latch;

                if ((chips[num].videoram_addr >= 0x2000) && (chips[num].videoram_addr <= 0x3fef))
                    chips[num].videoram_data_latch = chips[num].ppu_page[(chips[num].videoram_addr & 0xc00) >> 10].read(chips[num].videoram_addr & 0x3ff);
                else
                    chips[num].videoram_data_latch = chips[num].videoram.read(chips[num].videoram_addr & 0x3fff);

                chips[num].videoram_addr += chips[num].add;
                break;

            default:
				/* ignore other register reads */
                break;
        }

        return ret;
    }


    /**
     * **********************************
     * <p>
     * PPU Registers Write
     * <p>
     * ***********************************
     */
    public static void ppu2c03b_w(int num, int offset, int data) {
	
		/* check bounds */
        if (num >= intf.num) {
            logerror("PPU(w): Attempting to access an unmapped chip\n");
            return;
        }

        if (offset >= PPU_MAX_REG) {
            logerror("PPU: Attempting to write past the chip\n");

            offset &= PPU_MAX_REG - 1;
	/*		return; */
        }

        switch (offset) {
            case PPU_CONTROL0:
                chips[num].regs[PPU_CONTROL0] = data;
	
				/* update the name table number on our refresh latches */
                chips[num].refresh_latch &= ~0x0c00;
                chips[num].refresh_latch |= (data & 3) << 10;
	
				/* the char ram bank points either 0x0000 or 0x1000 (page 0 or page 4) */
                chips[num].tile_page = (data & PPU_CONTROL0_CHR_SELECT) >> 2;
                chips[num].sprite_page = (data & PPU_CONTROL0_SPR_SELECT) >> 1;

                chips[num].add = (data & PPU_CONTROL0_INC) != 0 ? 32 : 1;
                break;

            case PPU_CONTROL1:
				/* if color intensity has changed, change all the pens */
                if ((data & 0xe0) != (chips[num].regs[PPU_CONTROL1] & 0xe0)) {
					/* TODO? : maybe add support for COLOR_INTENSITY later? */
                }

                chips[num].regs[PPU_CONTROL1] = data;
                break;

            case PPU_SPRITE_ADDRESS:
                chips[num].regs[PPU_SPRITE_ADDRESS] = data & 0xff;
                break;

            case PPU_SPRITE_DATA:
                chips[num].spriteram.write(chips[num].regs[PPU_SPRITE_ADDRESS], data);
                chips[num].regs[PPU_SPRITE_ADDRESS] = (chips[num].regs[PPU_SPRITE_ADDRESS] + 1) & 0xff;
                break;

            case PPU_SCROLL:
                if (chips[num].toggle != 0) {
					/* second write */
                    chips[num].refresh_latch &= ~0x03e0;
                    chips[num].refresh_latch |= (data & 0xf8) << 2;

                    chips[num].refresh_latch &= ~0x7000;
                    chips[num].refresh_latch |= (data & 0x07) << 12;
                } else {
					/* first write */
                    chips[num].refresh_latch &= ~0x1f;
                    chips[num].refresh_latch |= (data & 0xf8) >> 3;

                    chips[num].x_fine = data & 7;
                }

                chips[num].toggle ^= 1;
                break;

            case PPU_ADDRESS:
                if (chips[num].toggle != 0) {
					/* second write */
                    chips[num].videoram_addr = (chips[num].videoram_addr_latch << 8) | (data & 0xff);

                    chips[num].refresh_latch &= ~0x00ff;
                    chips[num].refresh_latch |= data & 0xff;
                    chips[num].refresh_data = chips[num].refresh_latch;
                } else {
					/* first write */
                    chips[num].videoram_addr_latch = data & 0xff;

                    if (data != 0x3f) /* TODO: remove this hack! */ {
                        chips[num].refresh_latch &= ~0xff00;
                        chips[num].refresh_latch |= (data & 0x3f) << 8;
                    }
                }

                chips[num].toggle ^= 1;
                break;

            case PPU_DATA: {
                int tempAddr = chips[num].videoram_addr & 0x3fff;
	
					/* if there's a callback, call it now */
                if (chips[num].vidaccess_callback_proc != null)
                    data = chips[num].vidaccess_callback_proc.handler(num, tempAddr, data);
	
					/* see if it's on the chargen portion */
                if (tempAddr < 0x2000) {
						/* if we have a videorom mapped there, dont write and log the problem */
                    if (chips[num].has_videorom != 0) {
							/* if there is a vidaccess callback, assume it coped up with it */
                        if (chips[num].vidaccess_callback_proc == null)
                            logerror("PPU: Attempting to write to the chargen, when there's a ROM there!\n");
                    } else {
							/* store the data */
                        chips[num].videoram.write(tempAddr, data);
	
							/* setup the master dirty switch */
                        chips[num].chars_are_dirty = 1;
	
							/* mark the char dirty */
                        chips[num].dirtychar[tempAddr >> 4] = 1;
                    }
	
						/* increment the address */
                    chips[num].videoram_addr += chips[num].add;
	
						/* and be gone */
                    break;
                }
	
					/* the only valid background colors are writes to 0x3f00 and 0x3f10			*/
					/* and even then, they are mirrors of each other. as usual, some games		*/
					/* attempt to write values > the number of colors so we must mask the data. */
                if (tempAddr >= 0x3f00) {
                    int color_base = intf.color_base[num];
	
						/* store the data */
                    chips[num].videoram.write(tempAddr, data);

                    data &= 0x3f;

                    if ((tempAddr & 0x03) != 0) {
                        Machine.gfx[intf.gfx_layout_number[num]].colortable.write(tempAddr & 0x1f, Machine.pens[color_base + data]);
                        chips[num].colortable_mono[tempAddr & 0x1f] = Machine.pens[color_base + (data & 0xf0)];
                    }

                    if ((tempAddr & 0x0f) == 0) {
                        int i;

                        chips[num].back_color = data;
                        for (i = 0; i < 32; i += 4) {
                            Machine.gfx[intf.gfx_layout_number[num]].colortable.write(i, Machine.pens[color_base + data]);
                            chips[num].colortable_mono[i] = Machine.pens[color_base + (data & 0xf0)];
                        }
                    }
	
						/* increment the address */
                    chips[num].videoram_addr += chips[num].add;
	
						/* and be gone */
                    break;
                }
	
					/* everything else */
					/* writes to $3000-$3eff are mirrors of $2000-$2eff */
                {
                    int page = (tempAddr & 0x0c00) >> 10;
                    int address = tempAddr & 0x3ff;

                    chips[num].ppu_page[page].write(address, data);
	
						/* increment the address */
                    chips[num].videoram_addr += chips[num].add;
	
						/* fall through */
                }
            }
            break;

            default:
				/* ignore other registers writes */
                break;
        }
    }

    /**
     * **********************************
     * <p>
     * Sprite DMA
     * <p>
     * ***********************************
     */
    public static void ppu2c03b_spriteram_dma(int num, UBytePtr source) {
		/* check bounds */
        if (num >= intf.num) {
            logerror("PPU(render): Attempting to access an unmapped chip\n");
            return;
        }

        memcpy(chips[num].spriteram, source, SPRITERAM_SIZE);
    }

    /**
     * **********************************
     * <p>
     * PPU Rendering
     * <p>
     * ***********************************
     */
    public static void ppu2c03b_render(int num, mame_bitmap bitmap, int flipx, int flipy, int sx, int sy) {
		/* check bounds */
        if (num >= intf.num) {
            logerror("PPU(render): Attempting to access an unmapped chip\n");
            return;
        }
	
	/*	logerror("PPU %d:VBLANK HIT (scanline %d)\n",num, chips[num].scanline); */

        copybitmap(bitmap, chips[num].bitmap, flipx, flipy, sx, sy, null, TRANSPARENCY_NONE, 0);
    }

    /**
     * **********************************
     * <p>
     * PPU VideoROM banking
     * <p>
     * ***********************************
     */
    public static void ppu2c03b_set_videorom_bank(int num, int start_page, int num_pages, int bank, int bank_size) {
        int i;
	
		/* check bounds */
        if (num >= intf.num) {
            logerror("PPU(set vrom bank): Attempting to access an unmapped chip\n");
            return;
        }

        if (chips[num].has_videorom == 0) {
            logerror("PPU(set vrom bank): Attempting to switch videorom banks and no rom is mapped\n");
            return;
        }

        bank &= (chips[num].videorom_banks * (CHARGEN_NUM_CHARS / bank_size)) - 1;

        for (i = start_page; i < (start_page + num_pages); i++)
            chips[num].nes_vram[i] = bank * bank_size + 64 * (i - start_page);

        {
            int vram_start = start_page * 0x400;
            int count = num_pages * 0x400;
            int rom_start = bank * bank_size * 16;

            memcpy(chips[num].videoram, vram_start, memory_region(intf.vrom_region[num]), rom_start, count);
        }
    }

    /**
     * **********************************
     * <p>
     * Utility functions
     * <p>
     * ***********************************
     */
    public static int ppu2c03b_get_pixel(int num, int x, int y) {
		/* check bounds */
        if (num >= intf.num) {
            logerror("PPU(get_pixel): Attempting to access an unmapped chip\n");
            return 0;
        }

        if (x >= VISIBLE_SCREEN_WIDTH)
            x = VISIBLE_SCREEN_WIDTH - 1;

        if (y >= VISIBLE_SCREEN_HEIGHT)
            y = VISIBLE_SCREEN_HEIGHT - 1;

        return chips[num].bitmap.line[y].read(x);
    }

    public static int ppu2c03b_get_colorbase(int num) {
		/* check bounds */
        if (num >= intf.num) {
            logerror("PPU(get_colorbase): Attempting to access an unmapped chip\n");
            return 0;
        }

        return intf.color_base[num];
    }

    public static void ppu2c03b_set_mirroring(int num, int mirroring) {
		/* check bounds */
        if (num >= intf.num) {
            logerror("PPU(get_colorbase): Attempting to access an unmapped chip\n");
            return;
        }
	
		/* setup our videoram handlers based on mirroring */
        switch (mirroring) {
            case PPU_MIRROR_VERT:
                chips[num].ppu_page[0] = new UBytePtr(chips[num].videoram, 0x2000);
                chips[num].ppu_page[1] = new UBytePtr(chips[num].videoram, 0x2400);
                chips[num].ppu_page[2] = new UBytePtr(chips[num].videoram, 0x2000);
                chips[num].ppu_page[3] = new UBytePtr(chips[num].videoram, 0x2400);
                break;

            case PPU_MIRROR_HORZ:
                chips[num].ppu_page[0] = new UBytePtr(chips[num].videoram, 0x2000);
                chips[num].ppu_page[1] = new UBytePtr(chips[num].videoram, 0x2000);
                chips[num].ppu_page[2] = new UBytePtr(chips[num].videoram, 0x2400);
                chips[num].ppu_page[3] = new UBytePtr(chips[num].videoram, 0x2400);
                break;

            case PPU_MIRROR_HIGH:
                chips[num].ppu_page[0] = new UBytePtr(chips[num].videoram, 0x2400);
                chips[num].ppu_page[1] = new UBytePtr(chips[num].videoram, 0x2400);
                chips[num].ppu_page[2] = new UBytePtr(chips[num].videoram, 0x2400);
                chips[num].ppu_page[3] = new UBytePtr(chips[num].videoram, 0x2400);
                break;

            case PPU_MIRROR_LOW:
                chips[num].ppu_page[0] = new UBytePtr(chips[num].videoram, 0x2000);
                chips[num].ppu_page[1] = new UBytePtr(chips[num].videoram, 0x2000);
                chips[num].ppu_page[2] = new UBytePtr(chips[num].videoram, 0x2000);
                chips[num].ppu_page[3] = new UBytePtr(chips[num].videoram, 0x2000);
                break;

            case PPU_MIRROR_NONE:
            default:
                chips[num].ppu_page[0] = new UBytePtr(chips[num].videoram, 0x2000);
                chips[num].ppu_page[1] = new UBytePtr(chips[num].videoram, 0x2400);
                chips[num].ppu_page[2] = new UBytePtr(chips[num].videoram, 0x2800);
                chips[num].ppu_page[3] = new UBytePtr(chips[num].videoram, 0x2c00);
                break;
        }
    }

    public static void ppu2c03b_set_irq_callback(int num, ppu2c03b_irq_cb cb) {
		/* check bounds */
        if (num >= intf.num) {
            logerror("PPU(set_irq_callback): Attempting to access an unmapped chip\n");
            return;
        }

        intf.handler[num] = cb;
    }

    public static void ppu2c03b_set_scanline_callback(int num, ppu2c03b_scanline_cb cb) {
		/* check bounds */
        if (num >= intf.num) {
            logerror("PPU(set_scanline_callback): Attempting to access an unmapped chip\n");
            return;
        }

        chips[num].scanline_callback_proc = cb;
    }

    public static void ppu2c03b_set_vidaccess_callback(int num, ppu2c03b_vidaccess_cb cb) {
		/* check bounds */
        if (num >= intf.num) {
            logerror("PPU(set_vidaccess_callback): Attempting to access an unmapped chip\n");
            return;
        }

        chips[num].vidaccess_callback_proc = cb;
    }

    /**
     * **********************************
     * <p>
     * Accesors
     * <p>
     * ***********************************
     */

    public static ReadHandlerPtr ppu2c03b_0_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return ppu2c03b_r(0, offset);
        }
    };

    public static ReadHandlerPtr ppu2c03b_1_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return ppu2c03b_r(1, offset);
        }
    };

    public static WriteHandlerPtr ppu2c03b_0_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            ppu2c03b_w(0, offset, data);
        }
    };

    public static WriteHandlerPtr ppu2c03b_1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            ppu2c03b_w(1, offset, data);
        }
    };
}
