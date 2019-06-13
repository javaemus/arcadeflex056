/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;

import static common.ptr.*;

import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.inptport.*;
import static mame056.vidhrdw.generic.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame056.cpuexec.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static common.libc.cstring.*;
import static mame056.timerH.*;
import static mame056.timer.*;

public class exidy
{
	
	public static UBytePtr exidy_characterram = new UBytePtr();
	public static UBytePtr exidy_color_latch = new UBytePtr();
	public static UBytePtr exidy_sprite_no = new UBytePtr();
	public static UBytePtr exidy_sprite_enable = new UBytePtr();
	public static UBytePtr exidy_sprite1_xpos = new UBytePtr();
	public static UBytePtr exidy_sprite1_ypos = new UBytePtr();
	public static UBytePtr exidy_sprite2_xpos = new UBytePtr();
	public static UBytePtr exidy_sprite2_ypos = new UBytePtr();
	
	public static int exidy_collision_mask;
	public static int exidy_collision_invert;
	
	public static UBytePtr exidy_palette = new UBytePtr();
	public static UBytePtr exidy_colortable = new UBytePtr();
	
	static mame_bitmap motion_object_1_vid;
	static mame_bitmap motion_object_2_vid;
	
	static int[] chardirty = new int[256];
	static int update_complete;
	
	static int int_condition;
	
	
	
	/*************************************
	 *
	 *	Hard coded palettes
	 *
	 *************************************/
	
	/* Sidetrack/Targ/Spectar don't have a color PROM; colors are changed by the means of 8x3 */
	/* dip switches on the board. Here are the colors they map to. */
	public static char sidetrac_palette[] =
	{
		0x00,0x00,0x00,   /* BACKGND */
		0x00,0x00,0x00,   /* CSPACE0 */
		0x00,0xff,0x00,   /* CSPACE1 */
		0xff,0xff,0xff,   /* CSPACE2 */
		0xff,0xff,0xff,   /* CSPACE3 */
		0xff,0x00,0xff,   /* 5LINES (unused?) */
		0xff,0xff,0x00,   /* 5MO2VID  */
		0xff,0xff,0xff    /* 5MO1VID  */
	};
	
	/* Targ has different colors */
	public static char targ_palette[] =
	{
						/* color   use                */
		0x00,0x00,0xff, /* blue    background         */
		0x00,0xff,0xff, /* cyan    characters 192-255 */
		0xff,0xff,0x00, /* yellow  characters 128-191 */
		0xff,0xff,0xff, /* white   characters  64-127 */
		0xff,0x00,0x00, /* red     characters   0- 63 */
		0x00,0xff,0xff, /* cyan    not used           */
		0xff,0xff,0xff, /* white   bullet sprite      */
		0x00,0xff,0x00, /* green   wummel sprite      */
	};
	
	/* Spectar has different colors */
	public static char spectar_palette[] =
	{
						/* color   use                */
		0x00,0x00,0xff, /* blue    background         */
		0x00,0xff,0x00, /* green   characters 192-255 */
		0x00,0xff,0x00, /* green   characters 128-191 */
		0xff,0xff,0xff, /* white   characters  64-127 */
		0xff,0x00,0x00, /* red     characters   0- 63 */
		0x00,0xff,0x00, /* green   not used           */
		0xff,0xff,0x00, /* yellow  bullet sprite      */
		0x00,0xff,0x00, /* green   wummel sprite      */
	};
	
	
	
	/*************************************
	 *
	 *	Hard coded color tables
	 *
	 *************************************/
	
	public static char exidy_1bpp_colortable[] =
	{
		/* one-bit characters */
		0, 4,  /* chars 0x00-0x3F */
		0, 3,  /* chars 0x40-0x7F */
		0, 2,  /* chars 0x80-0xBF */
		0, 1,  /* chars 0xC0-0xFF */
	
		/* Motion Object 1 */
		0, 7,
	
		/* Motion Object 2 */
		0, 6,
	};
	
	public static char exidy_2bpp_colortable[] =
	{
		/* two-bit characters */
		/* (Because this is 2-bit color, the colorspace is only divided
			in half instead of in quarters.  That's why 00-3F = 40-7F and
			80-BF = C0-FF) */
		0, 0, 4, 3,  /* chars 0x00-0x3F */
		0, 0, 4, 3,  /* chars 0x40-0x7F */
		0, 0, 2, 1,  /* chars 0x80-0xBF */
		0, 0, 2, 1,  /* chars 0xC0-0xFF */
	
		/* Motion Object 1 */
		0, 7,
	
		/* Motion Object 2 */
		0, 6,
	};
	
	
	
	/*************************************
	 *
	 *	Palettes and colors
	 *
	 *************************************/
	
	/* also in driver/exidy.c */
	public static int PALETTE_LEN = 8;
	public static int COLORTABLE_LEN = 20;
	
	public static VhConvertColorPromPtr exidy_vh_init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                if (exidy_palette != null)
			memcpy(new UBytePtr(palette), exidy_palette, 3 * PALETTE_LEN);
		memcpy(new UBytePtr(colortable), exidy_colortable, COLORTABLE_LEN );
            }
        };
	
	/*************************************
	 *
	 *	Video startup
	 *
	 *************************************/
	
	public static VhStartPtr exidy_vh_start = new VhStartPtr() { public int handler() 
	{
	    if (generic_vh_start.handler() != 0)
	        return 1;
	
		motion_object_1_vid = bitmap_alloc(16, 16);
	    if (motion_object_1_vid == null)
	    {
	        generic_vh_stop.handler();
	        return 1;
	    }
	
		motion_object_2_vid = bitmap_alloc(16, 16);
	    if (motion_object_2_vid == null)
	    {
	        bitmap_free(motion_object_1_vid);
	        generic_vh_stop.handler();
	        return 1;
	    }
	    return 0;
	} };
	
	
	
	/*************************************
	 *
	 *	Video shutdown
	 *
	 *************************************/
	
	public static VhStopPtr exidy_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free(motion_object_1_vid);
		bitmap_free(motion_object_2_vid);
		generic_vh_stop.handler();
	} };
	
	
	
	/*************************************
	 *
	 *	Interrupt generation
	 *
	 *************************************/
	
	public static void latch_condition(int collision)
	{
		collision ^= exidy_collision_invert;
		int_condition = (input_port_2_r.handler(0) & ~0x14) | (collision & exidy_collision_mask);
	}
	
	
	public static InterruptPtr exidy_vblank_interrupt = new InterruptPtr() { public int handler() 
	{
		/* latch the current condition */
		latch_condition(0);
		int_condition &= ~0x80;
	
		/* set the IRQ line */
		cpu_set_irq_line(0, 0, ASSERT_LINE);
		return ignore_interrupt.handler();
	} };
	
	
	public static ReadHandlerPtr exidy_interrupt_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* clear any interrupts */
		cpu_set_irq_line(0, 0, CLEAR_LINE);
	
		/* return the latched condition */
		return int_condition;
	} };
	
	
	
	/*************************************
	 *
	 *	Character RAM
	 *
	 *************************************/
	
	public static WriteHandlerPtr exidy_characterram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (exidy_characterram.read(offset) != data)
		{
			exidy_characterram.write(offset, data);
			chardirty[offset / 8 % 256] = 1;
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Palette RAM
	 *
	 *************************************/
	
	public static WriteHandlerPtr exidy_color_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int i;
	
		exidy_color_latch.write(offset, data);
	
		for (i = 0; i < 8; i++)
		{
			int b = ((exidy_color_latch.read(0) >> i) & 0x01) * 0xff;
			int g = ((exidy_color_latch.read(1) >> i) & 0x01) * 0xff;
			int r = ((exidy_color_latch.read(2) >> i) & 0x01) * 0xff;
			palette_set_color(i, r, g, b);
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Background update
	 *
	 *************************************/
	
	static void update_background()
	{
		int x, y, offs;
	
		/* update the background and any dirty characters in it */
		for (y = offs = 0; y < 32; y++)
			for (x = 0; x < 32; x++, offs++)
			{
				int code = videoram.read(offs);
	
				/* see if the character is dirty */
				if (chardirty[code] == 1)
				{
					decodechar(Machine.gfx[0], code, exidy_characterram, Machine.drv.gfxdecodeinfo[0].gfxlayout);
					chardirty[code] = 2;
				}
	
				/* see if the bitmap is dirty */
				if (dirtybuffer[offs]!=0 || chardirty[code]!=0)
				{
					int color = code >> 6;
					drawgfx(tmpbitmap, Machine.gfx[0], code, color, 0, 0, x * 8, y * 8, null, TRANSPARENCY_NONE, 0);
					dirtybuffer[offs] = 0;
				}
			}
	
		/* reset the char dirty array */
		for (y = 0; y < 256; y++)
			if (chardirty[y] == 2)
				chardirty[y] = 0;
	}
	
	
	
	/*************************************
	 *
	 *	Determine the time when the beam
	 *	will intersect a given pixel
	 *
	 *************************************/
	
	static double pixel_time(int x, int y)
	{
		/* assuming this is called at refresh time, compute how long until we
		 * hit the given x,y position */
		return cpu_getscanlinetime(y) + (cpu_getscanlineperiod() * (double)x * (1.0 / 256.0));
	}
	
	
	static timer_callback collision_irq_callback = new timer_callback() {
            public void handler(int param) {
                /* latch the collision bits */
		latch_condition(param);
	
		/* set the IRQ line */
		cpu_set_irq_line(0, 0, ASSERT_LINE);
            }
        };
	
	/*************************************
	 *
	 *	End-of-frame callback
	 *
	 *************************************/
	
	/***************************************************************************
	
		Exidy hardware checks for two types of collisions based on the video
		signals.  If the Motion Object 1 and Motion Object 2 signals are on at
		the same time, an M1M2 collision bit gets set.  If the Motion Object 1
		and Background Character signals are on at the same time, an M1CHAR
		collision bit gets set.  So effectively, there's a pixel-by-pixel
		collision check comparing Motion Object 1 (the player) to the
		background and to the other Motion Object (typically a bad guy).
	
	***************************************************************************/
	
	public static VhEofCallbackPtr exidy_vh_eof = new VhEofCallbackPtr() {
            public void handler() {
                int enable_set = ((exidy_sprite_enable.read() & 0x20) != 0)?1:0;
                rectangle clip = new rectangle( 0, 15, 0, 15 );
                int pen0 = Machine.pens[0];
                int sx, sy, org_x, org_y;
		int count = 0;
	
		/* if there is nothing to detect, bail */
		if (exidy_collision_mask == 0)
			return;
	
		/* if sprite 1 isn't enabled, we can't collide */
		if ((exidy_sprite_enable.read() & 0x80)!=0 && (exidy_sprite_enable.read() & 0x10)==0)
		{
			update_complete = 0;
			return;
		}
	
		/* update the background if necessary */
		if (update_complete == 0)
			update_background();
		update_complete = 0;
	
		/* draw sprite 1 */
		org_x = 236 - exidy_sprite1_xpos.read() - 4;
		org_y = 244 - exidy_sprite1_ypos.read() - 4;
		drawgfx(motion_object_1_vid, Machine.gfx[1],
			(exidy_sprite_no.read() & 0x0f) + 16 * enable_set, 0,
			0, 0, 0, 0, clip, TRANSPARENCY_NONE, 0);
	
	    /* draw sprite 2 clipped to sprite 1's location */
		fillbitmap(motion_object_2_vid, pen0, clip);
		if ((exidy_sprite_enable.read() & 0x40)==0)
		{
			sx = (236 - exidy_sprite2_xpos.read() - 4) - org_x;
			sy = (244 - exidy_sprite2_ypos.read() - 4) - org_y;
	
			drawgfx(motion_object_2_vid, Machine.gfx[1],
				((exidy_sprite_no.read() >> 4) & 0x0f) + 32, 1,
				0, 0, sx, sy, clip, TRANSPARENCY_NONE, 0);
		}
	
	    /* scan for collisions */
	    for (sy = 0; sy < 16; sy++)
		    for (sx = 0; sx < 16; sx++)
	    		if (read_pixel.handler(motion_object_1_vid, sx, sy) != pen0)
	    		{
	    			int collision_mask = 0;
	
	                /* check for background collision (M1CHAR) */
					if (read_pixel.handler(tmpbitmap, org_x + sx, org_y + sy) != pen0)
						collision_mask |= 0x04;
	
	                /* check for motion object collision (M1M2) */
					if (read_pixel.handler(motion_object_2_vid, sx, sy) != pen0)
						collision_mask |= 0x10;
	
					/* if we got one, trigger an interrupt */
					if ((collision_mask & exidy_collision_mask)!=0 && count++ < 128)
						timer_set(pixel_time(org_x + sx, org_y + sy), collision_mask, collision_irq_callback);
	            }
            }
        };
	
	/*************************************
	 *
	 *	Standard screen refresh callback
	 *
	 *************************************/
	
	public static VhUpdatePtr exidy_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int sx, sy;
	
		/* update the background and draw it */
		update_background();
		copybitmap(bitmap, tmpbitmap, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_NONE, 0);
	
		/* draw sprite 2 first */
		if ((exidy_sprite_enable.read() & 0x40)==0)
		{
			sx = 236 - exidy_sprite2_xpos.read() - 4;
			sy = 244 - exidy_sprite2_ypos.read() - 4;
	
			drawgfx(bitmap, Machine.gfx[1],
				((exidy_sprite_no.read() >> 4) & 0x0f) + 32, 1,
				0, 0, sx, sy, Machine.visible_area, TRANSPARENCY_PEN, 0);
		}
	
		/* draw sprite 1 next */
		if ((exidy_sprite_enable.read() & 0x80)==0 || (exidy_sprite_enable.read() & 0x10)==0)
		{
			int enable_set = ((exidy_sprite_enable.read() & 0x20) != 0)?1:0;
	
			sx = 236 - exidy_sprite1_xpos.read() - 4;
			sy = 244 - exidy_sprite1_ypos.read() - 4;
	
			if (sy < 0) sy = 0;
	
			drawgfx(bitmap, Machine.gfx[1],
				(exidy_sprite_no.read() & 0x0f) + 16 * enable_set, 0,
				0, 0, sx, sy, Machine.visible_area, TRANSPARENCY_PEN, 0);
		}
	
		/* indicate that we already updated the background */
		update_complete = 1;
	} };
}
