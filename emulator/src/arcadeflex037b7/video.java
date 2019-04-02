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

    
    

    /*
     * This function tries to find the best display mode.
     */
    public static void select_display_mode(int width, int height, int depth, int attributes, int orientation) {
        int i;
        /*TODO*///
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
/*TODO*///
/*TODO*///	/* initialise quadring table [useful for *all* doubling modes */
/*TODO*///	for (i = 0; i < 256; i++)
/*TODO*///	{
/*TODO*///		doublepixel[i] = i | (i<<8);
/*TODO*///		quadpixel[i] = i | (i<<8) | (i << 16) | (i << 24);
/*TODO*///	}
/*TODO*///
/*TODO*///	use_vesa = -1;
/*TODO*///
/*TODO*///	/* 16 bit color is supported only by VESA modes */
/*TODO*///	if (depth == 16)
/*TODO*///	{
/*TODO*///		logerror("Game needs 16-bit colors. Using VESA\n");
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
                                if (vector_game != 0)
                                    gfx_width = 0;
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
/*TODO*///	/* If no VESA resolution has been given, we choose a sensible one. */
/*TODO*///	/* 640x480, 800x600 and 1024x768 are common to all VESA drivers_old. */
        if (gfx_width == 0 && gfx_height == 0) {
            auto_resolution = 1;
            use_vesa = 1;

            /* vector games use 640x480 as default */
            if (vector_game != 0) {
                gfx_width = 640;
                gfx_height = 480;
            } else {
                int xm, ym;

                xm = ym = 1;

                if ((attributes & VIDEO_PIXEL_ASPECT_RATIO_MASK)
                        == VIDEO_PIXEL_ASPECT_RATIO_1_2) {
                    if ((orientation & ORIENTATION_SWAP_XY) != 0) {
                        xm++;
                    } else {
                        ym++;
                    }
                }

                if (scanlines != 0 && stretch != 0) {
                    if (ym == 1) {
                        xm *= 2;
                        ym *= 2;
                    }

                    /* see if pixel doubling can be applied at 640x480 */
                    if (ym * height <= 480 && xm * width <= 640
                            && (xm > 1 || (ym + 1) * height > 768 || (xm + 1) * width > 1024)) {
                        gfx_width = 640;
                        gfx_height = 480;
                    } /* see if pixel doubling can be applied at 800x600 */ else if (ym * height <= 600 && xm * width <= 800
                            && (xm > 1 || (ym + 1) * height > 768 || (xm + 1) * width > 1024)) {
                        gfx_width = 800;
                        gfx_height = 600;
                    }
                    /* don't use 1024x768 right away. If 512x384 is available, it */
 /* will provide hardware scanlines. */

                    if (ym > 1 && xm > 1) {
                        xm /= 2;
                        ym /= 2;
                    }
                }

                if (gfx_width == 0 && gfx_height == 0) {
                    if (ym * height <= 240 && xm * width <= 320) {
                        gfx_width = 320;
                        gfx_height = 240;
                    } else if (ym * height <= 300 && xm * width <= 400) {
                        gfx_width = 400;
                        gfx_height = 300;
                    } else if (ym * height <= 384 && xm * width <= 512) {
                        gfx_width = 512;
                        gfx_height = 384;
                    } else if (ym * height <= 480 && xm * width <= 640
                            && (stretch == 0 || (ym + 1) * height > 768 || (xm + 1) * width > 1024)) {
                        gfx_width = 640;
                        gfx_height = 480;
                    } else if (ym * height <= 600 && xm * width <= 800
                            && (stretch == 0 || (ym + 1) * height > 768 || (xm + 1) * width > 1024)) {
                        gfx_width = 800;
                        gfx_height = 600;
                    } else {
                        gfx_width = 1024;
                        gfx_height = 768;
                    }
                }
            }
        }
    }

    /* set the actual display screen but don't allocate the screen bitmap */
    public static int osd_set_display(int width, int height, int depth, int attributes, int orientation) {
        /*TODO*///	struct mode_adjust *adjust_array;
/*TODO*///
        int i;
        /* moved 'found' to here (req. for 15.75KHz Arcade Monitor Modes) */
        int found;

        if (gfx_height == 0 || gfx_width == 0) {
            printf("Please specify height AND width (e.g. -640x480)\n");
            return 0;
        }


        /* Mark the dirty buffers as dirty */

 /*TODO*///	if (use_dirty)
/*TODO*///	{
/*TODO*///		if (vector_game)
/*TODO*///			/* vector games only use one dirty buffer */
/*TODO*///			init_dirty (0);
/*TODO*///		else
/*TODO*///			init_dirty(1);
/*TODO*///		swap_dirty();
/*TODO*///		init_dirty(1);
/*TODO*///	}
        if (dirtycolor != null) {
            for (i = 0; i < screen_colors; i++) {
                dirtycolor[i] = 1;
            }
            dirtypalette = 1;
        }
        /* handle special 15.75KHz modes, these now include SVGA modes */
        found = 0;
        /*move video freq set to here, as we need to set it explicitly for the 15.75KHz modes */
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
/*TODO*///			else
/*TODO*///				triplebuf_page_width = (gfx_width + 0x1ff) & ~0x1ff;
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
/*TODO*///				mode = gfx_mode; /* restart with the mode given in mame_old.cfg */
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
/*TODO*///
/*TODO*///
        vsync_frame_rate = video_fps;
        /*TODO*///
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
/*TODO*///		logerror("target frame rate = %ffps, video frame rate = %3.2fHz\n",video_fps,rate);
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
/*TODO*///
        warming_up = 1;

        return 1;
    }

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

    
    public static void osd_free_bitmap(mame_bitmap bitmap) {
        if (bitmap != null) {
            //bitmap->line -= safety;
            bitmap.line = null;
//            bitmap._private = null;
            bitmap = null;
        }
    }

    public static void osd_save_snapshot(mame_bitmap bitmap) {

    }

}
