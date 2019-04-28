/***************************************************************************

	Exidy 440 video system

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.drivers.exidy440.exidy440_bank;
import static WIP.mame056.drivers.exidy440.exidy440_topsecret;
import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;
import static common.libc.cstring.memcpy;

import static common.ptr.*;
import static common.subArrays.*;
import static mame056.common.*;
import static mame056.inptport.*;
import static mame056.drawgfx.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.timerH.*;
import static mame056.timer.*;
import static mame056.cpuexec.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.cpuexecH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpuintrfH.*;


public class exidy440
{
	
	public static int SPRITE_COUNT      =   40;
	public static int SPRITERAM_SIZE    =   (SPRITE_COUNT * 4);
	public static int CHUNK_SIZE        =   8;
	public static int MAX_SCANLINE      =   240;
	public static int TOTAL_CHUNKS      =   (MAX_SCANLINE / CHUNK_SIZE);
	
	/* globals */
	public static UBytePtr exidy440_scanline = new UBytePtr();
	public static UBytePtr exidy440_imageram = new UBytePtr();
	public static int exidy440_firq_vblank;
	public static int exidy440_firq_beam;
	public static int topsecex_yscroll;
	
	/* local allocated storage */
	public static int exidy440_latched_x;
	public static UBytePtr local_videoram = new UBytePtr();
	public static UBytePtr local_paletteram = new UBytePtr();
	public static UBytePtr scanline_dirty = new UBytePtr();
	public static UBytePtr spriteram_buffer = new UBytePtr();
	
	/* local variables */
	static int firq_enable;
	static int firq_select;
	static int palettebank_io;
	static int palettebank_vis;
	static int topsecex_last_yscroll;
	
	
	/*************************************
	 *
	 *	Initialize the video system
	 *
	 *************************************/
	
	public static VhStartPtr exidy440_vh_start = new VhStartPtr() { public int handler() 
	{
		/* reset the system */
		firq_enable = 0;
		firq_select = 0;
		palettebank_io = 0;
		palettebank_vis = 0;
		exidy440_firq_vblank = 0;
		exidy440_firq_beam = 0;
	
		/* reset Top Secret variables */
		topsecex_yscroll = 0;
		topsecex_last_yscroll = 0;
	
		/* allocate a buffer for VRAM */
		local_videoram = new UBytePtr(256 * 256 * 2);
		if (local_videoram == null)
		{
			exidy440_vh_stop.handler();
			return 1;
		}
	
		/* clear it */
		memset(local_videoram, 0, 256 * 256 * 2);
	
		/* allocate a buffer for palette RAM */
		local_paletteram = new UBytePtr(512 * 2);
		if (local_paletteram == null)
		{
			exidy440_vh_stop.handler();
			return 1;
		}
	
		/* clear it */
		memset(local_paletteram, 0, 512 * 2);
	
		/* allocate a scanline dirty array */
		scanline_dirty = new UBytePtr(256);
		if (scanline_dirty == null)
		{
			exidy440_vh_stop.handler();
			return 1;
		}
	
		/* mark everything dirty to start */
		memset(scanline_dirty, 1, 256);
	
		/* allocate a sprite cache */
		spriteram_buffer = new UBytePtr(SPRITERAM_SIZE * TOTAL_CHUNKS);
		if (spriteram_buffer == null)
		{
			exidy440_vh_stop.handler();
			return 1;
		}
	
		/* start the scanline timer */
		timer_set(TIME_NOW, 0, scanline_callback);
	
		return 0;
	} };
	
	
	
	/*************************************
	 *
	 *	Tear down the video system
	 *
	 *************************************/
	
	public static VhStopPtr exidy440_vh_stop = new VhStopPtr() { public void handler() 
	{
		/* free VRAM */
		if (local_videoram != null)
			local_videoram = null;
	
		/* free palette RAM */
		if (local_paletteram != null)
			local_paletteram = null;
	
		/* free the scanline dirty array */
		if (scanline_dirty != null)
                        scanline_dirty = null;
	
		/* free the sprite cache */
		if (spriteram_buffer != null)
                        spriteram_buffer = null;
	} };
	
	
	
	/*************************************
	 *
	 *	Periodic scanline update
	 *
	 *************************************/
	
	public static timer_callback scanline_callback = new timer_callback() {
            public void handler(int scanline) {
                /* copy the spriteram */
		memcpy(new UBytePtr(spriteram_buffer, SPRITERAM_SIZE * (scanline / CHUNK_SIZE)), spriteram, SPRITERAM_SIZE);
	
		/* fire after the next 8 scanlines */
		scanline += CHUNK_SIZE;
		if (scanline >= MAX_SCANLINE)
			scanline = 0;
		timer_set(cpu_getscanlinetime(scanline), scanline, scanline_callback);
            }
        };
	
	
	/*************************************
	 *
	 *	Video RAM read/write
	 *
	 *************************************/
	
	public static ReadHandlerPtr exidy440_videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		UBytePtr base = new UBytePtr(local_videoram,(exidy440_scanline.read() * 256 + offset) * 2);
	
		/* combine the two pixel values into one byte */
		return (base.read(0) << 4) | base.read(1);
	} };
	
	
	public static WriteHandlerPtr exidy440_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr base = new UBytePtr(local_videoram, (exidy440_scanline.read() * 256 + offset) * 2);
	
		/* expand the two pixel values into two bytes */
		base.write(0, (data >> 4) & 15);
		base.write(1, data & 15);
	
		/* mark the scanline dirty */
		scanline_dirty.write(exidy440_scanline.read(), 1);
	} };
	
	
	
	/*************************************
	 *
	 *	Palette RAM read/write
	 *
	 *************************************/
	
	public static ReadHandlerPtr exidy440_paletteram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return local_paletteram.read(palettebank_io * 512 + offset);
	} };
	
	
	public static WriteHandlerPtr exidy440_paletteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* update palette ram in the I/O bank */
		local_paletteram.write(palettebank_io * 512 + offset, data);
	
		/* if we're modifying the active palette, change the color immediately */
		if (palettebank_io == palettebank_vis)
		{
			int word;
	
			/* combine two bytes into a word */
			offset = palettebank_vis * 512 + (offset & 0x1fe);
			word = (local_paletteram.read(offset) << 8) + local_paletteram.read(offset + 1);
	
			/* extract the 5-5-5 RGB colors */
			palette_set_color(offset / 2, ((word >> 10) & 31) << 3, ((word >> 5) & 31) << 3, (word & 31) << 3);
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Horizontal/vertical positions
	 *
	 *************************************/
	
	public static ReadHandlerPtr exidy440_horizontal_pos_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* clear the FIRQ on a read here */
		exidy440_firq_beam = 0;
		exidy440_update_firq();
	
		/* according to the schems, this value is only latched on an FIRQ
		 * caused by collision or beam */
		return exidy440_latched_x;
	} };
	
	
	public static ReadHandlerPtr exidy440_vertical_pos_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int result;
	
		/* according to the schems, this value is latched on any FIRQ
		 * caused by collision or beam, ORed together with CHRCLK,
		 * which probably goes off once per scanline; for now, we just
		 * always return the current scanline */
		result = cpu_getscanline();
		return (result < 255) ? result : 255;
	} };
	
	
	
	/*************************************
	 *
	 *	Interrupt and I/O control regs
	 *
	 *************************************/
	
	public static WriteHandlerPtr exidy440_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int oldvis = palettebank_vis;
	
		/* extract the various bits */
		exidy440_bank = data >> 4;
		firq_enable = (data >> 3) & 1;
		firq_select = (data >> 2) & 1;
		palettebank_io = (data >> 1) & 1;
		palettebank_vis = data & 1;
	
		/* set the memory bank for the main CPU from the upper 4 bits */
		cpu_setbank(1, new UBytePtr(memory_region(REGION_CPU1), 0x10000 + exidy440_bank * 0x4000));
	
		/* update the FIRQ in case we enabled something */
		exidy440_update_firq();
	
		/* if we're swapping palettes, change all the colors */
		if (oldvis != palettebank_vis)
		{
			int i;
	
			/* pick colors from the visible bank */
			offset = palettebank_vis * 512;
			for (i = 0; i < 256; i++, offset += 2)
			{
				/* extract a word and the 5-5-5 RGB components */
				int word = (local_paletteram.read(offset) << 8) + local_paletteram.read(offset + 1);
				palette_set_color(i, ((word >> 10) & 31) << 3, ((word >> 5) & 31) << 3, (word & 31) << 3);
			}
		}
	} };
	
	
	public static WriteHandlerPtr exidy440_interrupt_clear_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* clear the VBLANK FIRQ on a write here */
		exidy440_firq_vblank = 0;
		exidy440_update_firq();
	} };
	
	
	
	/*************************************
	 *
	 *	Interrupt generators
	 *
	 *************************************/
	
	public static void exidy440_update_firq()
	{
		if ((exidy440_firq_vblank!=0) || ((firq_enable!=0) && (exidy440_firq_beam!=0)))
			cpu_set_irq_line(0, 1, ASSERT_LINE);
		else
			cpu_set_irq_line(0, 1, CLEAR_LINE);
	}
	
	
	public static InterruptPtr exidy440_vblank_interrupt = new InterruptPtr() { public int handler() 
	{
		/* set the FIRQ line on a VBLANK */
		exidy440_firq_vblank = 1;
		exidy440_update_firq();
	
		/* allocate a timer to go off just before the refresh (but not for Top Secret */
		if (exidy440_topsecret == 0)
			timer_set(TIME_IN_USEC(Machine.drv.vblank_duration - 50), 0, exidy440_update_callback);
	
		return ignore_interrupt.handler();
	} };
	
	
	
	/*************************************
	 *
	 *	IRQ callback handlers
	 *
	 *************************************/
	
	public static timer_callback beam_firq_callback = new timer_callback() {
            public void handler(int param) {
                /* generate the interrupt, if we're selected */
		if (firq_select!=0 && firq_enable!=0)
		{
			exidy440_firq_beam = 1;
			exidy440_update_firq();
		}
	
		/* round the x value to the nearest byte */
		param = (param + 1) / 2;
	
		/* latch the x value; this convolution comes from the read routine */
		exidy440_latched_x = (param + 3) ^ 2;
            }
        };
	
	
	public static timer_callback collide_firq_callback = new timer_callback() {
            public void handler(int param) {
                /* generate the interrupt, if we're selected */
		if ((firq_select==0) && (firq_enable!=0))
		{
			exidy440_firq_beam = 1;
			exidy440_update_firq();
		}
	
		/* round the x value to the nearest byte */
		param = (param + 1) / 2;
	
		/* latch the x value; this convolution comes from the read routine */
		exidy440_latched_x = (param + 3) ^ 2;
            }
        };
	
	
	/*************************************
	 *
	 *	Determine the time when the beam
	 *	will intersect a given pixel
	 *
	 *************************************/
	
	public static double compute_pixel_time(int x, int y)
	{
		/* assuming this is called at refresh time, compute how long until we
		 * hit the given x,y position */
		return cpu_getscanlinetime(y) + (cpu_getscanlineperiod() * (double)x * (1.0 / 320.0));
	}
	
	
	
	/*************************************
	 *
	 *	Sprite drawing
	 *
	 *************************************/
	
	static void draw_sprites(mame_bitmap bitmap, int scroll_offset)
	{
		int scanline, i;
	
		/* get a pointer to the palette to look for collision flags */
		UBytePtr palette = new UBytePtr(local_paletteram, palettebank_vis * 512);
	
		/* start the count high for topsecret, which doesn't use collision flags */
		int count = exidy440_topsecret!=0 ? 128 : 0;
	
		/* draw the sprite images, checking for collisions along the way */
		for (scanline = 0; scanline < MAX_SCANLINE; scanline += CHUNK_SIZE)
		{
			UBytePtr sprite = new UBytePtr(spriteram_buffer, SPRITERAM_SIZE * (scanline / CHUNK_SIZE) + (SPRITE_COUNT - 1) * 4);
			for (i = 0; i < SPRITE_COUNT; i++, sprite.dec(4) )
			{
				int image = (~sprite.read(3) & 0x3f);
				int xoffs = (~((sprite.read(1) << 8) | sprite.read(2)) & 0x1ff);
				int yoffs = (~sprite.read(0) & 0xff) + 1;
				int x, y, sy;
				UBytePtr src;
	
				/* skip if out of range */
				if (yoffs < scanline || yoffs >= scanline + 16 + CHUNK_SIZE - 1)
					continue;
	
				/* get a pointer to the source image */
				src = new UBytePtr(exidy440_imageram, image * 128);
	
				/* account for large positive offsets meaning small negative values */
				if (xoffs >= 0x1ff - 16)
					xoffs -= 0x1ff;
	
				/* loop over y */
				sy = yoffs + scroll_offset;
				for (y = 0; y < 16; y++, yoffs--, sy--)
				{
					/* wrap at the top and bottom of the screen */
					if (sy >= 240)
						sy -= 240;
					else if (sy < 0)
						sy += 240;
	
					/* stop if we get before the current scanline */
					if (yoffs < scanline)
						break;
	
					/* only draw scanlines that are in this chunk */
					if (yoffs < scanline + CHUNK_SIZE)
					{
						UBytePtr old = new UBytePtr(local_videoram, sy * 512 + xoffs);
						int currx = xoffs;
	
						/* mark this scanline dirty */
						scanline_dirty.write(sy, 1);
	
						/* loop over x */
						for (x = 0; x < 8; x++, old.inc(2))
						{
							int ipixel = src.read();
                                                        src.inc();
							int left = ipixel & 0xf0;
							int right = (ipixel << 4) & 0xf0;
							int pen;
	
							/* left pixel */
							if (left!=0 && currx >= 0 && currx < 320)
							{
								/* combine with the background */
								pen = left | old.read(0);
								plot_pixel.handler(bitmap, currx, yoffs, Machine.pens[pen]);
	
								/* check the collisions bit */
								if (((palette.read(2 * pen) & 0x80)!=0) && count++ < 128)
									timer_set(compute_pixel_time(currx, yoffs), currx, collide_firq_callback);
							}
							currx++;
	
							/* right pixel */
							if (right!=0 && currx >= 0 && currx < 320)
							{
								/* combine with the background */
								pen = right | old.read(1);
								plot_pixel.handler(bitmap, currx, yoffs, Machine.pens[pen]);
	
								/* check the collisions bit */
								if ((palette.read(2 * pen) & 0x80)!=0 && count++ < 128)
									timer_set(compute_pixel_time(currx, yoffs), currx, collide_firq_callback);
							}
							currx++;
						}
					}
					else
						src.inc(8);
				}
			}
		}
	}
	
	
	
	/*************************************
	 *
	 *	Core refresh routine
	 *
	 *************************************/
	
	public static void update_screen(mame_bitmap bitmap, int scroll_offset)
	{
		int y, sy;
		int beamx, beamy;
	
		/* draw any dirty scanlines from the VRAM directly */
		sy = scroll_offset;
		for (y = 0; y < 240; y++, sy++)
		{
			/* wrap at the bottom of the screen */
			if (sy >= 240)
				sy -= 240;
	
			/* only redraw if dirty */
			if (scanline_dirty.read(sy) != 0)
			{
				draw_scanline8(bitmap, 0, y, 320, new UBytePtr(local_videoram, sy * 512), new IntArray(Machine.pens), -1);
				scanline_dirty.write(sy, 0);
			}
		}
	
		/* draw the sprites */
		draw_sprites(bitmap, scroll_offset);
	
		/* draw the crosshair (but not for topsecret) */
		if (exidy440_topsecret == 0)
		{
			beamx = ((input_port_4_r.handler(0) & 0xff) * 320) >> 8;
			beamy = ((input_port_5_r.handler(0) & 0xff) * 240) >> 8;
	
			draw_crosshair(bitmap, beamx, beamy, Machine.visible_area);
	
			/* dirty scanlines */
			/* we can ignore scroll (topsecret is the only game which uses scroll)  */
			for(y = beamy - 6; y <= beamy + 6; y++)
				if((y >= 0) && (y < 256))
					scanline_dirty.write(y, 1);
		}
	}
	
	
	
	/*************************************
	 *
	 *	Update handling for shooters
	 *
	 *************************************/
	
	public static timer_callback exidy440_update_callback = new timer_callback() {
            public void handler(int param) {
                /* note: we do most of the work here, because collision detection and beam detection need
			to happen at 60Hz, whether or not we're frameskipping; in order to do those, we pretty
			much need to do all the update handling */
	
		mame_bitmap bitmap = Machine.scrbitmap;
	
		int i;
		double time, increment;
		int beamx, beamy;
	
		/* redraw the screen */
		update_screen(bitmap, 0);
	
		/* update the analog x,y values */
		beamx = ((input_port_4_r.handler(0) & 0xff) * 320) >> 8;
		beamy = ((input_port_5_r.handler(0) & 0xff) * 240) >> 8;
	
		/* The timing of this FIRQ is very important. The games look for an FIRQ
			and then wait about 650 cycles, clear the old FIRQ, and wait a
			very short period of time (~130 cycles) for another one to come in.
			From this, it appears that they are expecting to get beams over
			a 12 scanline period, and trying to pick roughly the middle one.
			This is how it is implemented. */
		increment = cpu_getscanlineperiod();
		time = compute_pixel_time(beamx, beamy) - increment * 6;
		for (i = 0; i <= 12; i++, time += increment)
			timer_set(time, beamx, beam_firq_callback);
            }
        };
	
	
	/*************************************
	 *
	 *	Standard screen refresh callback
	 *
	 *************************************/
	
	public static VhUpdatePtr exidy440_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/* if we need a full refresh, mark all scanlines dirty */
		if (full_refresh != 0)
			memset(scanline_dirty, 1, 256);
	
		/* if we're Top Secret, do our refresh here; others are done in the update function above */
		/* unless we're doing a full refresh (eg. when the driver is paused) */
		if (exidy440_topsecret != 0)
		{
			/* if the scroll changed, mark everything dirty */
			if (topsecex_yscroll != topsecex_last_yscroll)
			{
				topsecex_last_yscroll = topsecex_yscroll;
				memset(scanline_dirty, 1, 256);
			}
	
			/* redraw the screen */
			update_screen(bitmap, topsecex_yscroll);
		}
		else
		{
			if(full_refresh != 0)
			{
				update_screen(bitmap, 0);
			}
		}
	} };
}
