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

import static common.ptr.*;
import static common.subArrays.*;
import static common.libc.cstring.*;

import static mame056.common.*;
import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.inptport.*;
import static mame056.sound.mixer.*;
import static mame056.vidhrdw.generic.*;

import static mame056.timer.*;
import static mame056.timerH.*;

// refactor
import static arcadeflex036.osdepend.logerror;



public class exerion
{
	
	//#define DEBUG_SPRITES
	
	public static int BACKGROUND_X_START        = 32;
	public static int BACKGROUND_X_START_FLIP   = 72;
	
	public static int VISIBLE_X_MIN             = (12*8);
	public static int VISIBLE_X_MAX             = (52*8);
	public static int VISIBLE_Y_MIN             = (2*8);
	public static int VISIBLE_Y_MAX             = (30*8);
	
	
	/*TODO*///#ifdef DEBUG_SPRITES
	/*TODO*///FILE	*sprite_log;
	/*TODO*///#endif
	
	public static int exerion_cocktail_flip;
	
	static int char_palette, sprite_palette;
	static int char_bank;
	
	static UBytePtr background_latches = new UBytePtr();
	static UBytePtr[] background_gfx=new UBytePtr[4];
	static UBytePtr current_latches=new UBytePtr(16);
	static int last_scanline_update;
	
	static UBytePtr background_mixer = new UBytePtr();
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  The palette PROM is connected to the RGB output this way:
	
	  bit 7 -- 220 ohm resistor  -- BLUE
	        -- 470 ohm resistor  -- BLUE
	        -- 220 ohm resistor  -- GREEN
	        -- 470 ohm resistor  -- GREEN
	        -- 1  kohm resistor  -- GREEN
	        -- 220 ohm resistor  -- RED
	        -- 470 ohm resistor  -- RED
	  bit 0 -- 1  kohm resistor  -- RED
	
	***************************************************************************/
	
	public static VhConvertColorPromPtr exerion_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
                int _palette = 0;
	
		for (i = 0; i < Machine.drv.total_colors; i++)
		{
			int bit0, bit1, bit2;
	
			/* red component */
			bit0 = (color_prom.read() >> 0) & 0x01;
			bit1 = (color_prom.read() >> 1) & 0x01;
			bit2 = (color_prom.read() >> 2) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* green component */
			bit0 = (color_prom.read() >> 3) & 0x01;
			bit1 = (color_prom.read() >> 4) & 0x01;
			bit2 = (color_prom.read() >> 5) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* blue component */
			bit0 = 0;
			bit1 = (color_prom.read() >> 6) & 0x01;
			bit2 = (color_prom.read() >> 7) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
	
			color_prom.inc();
		}
	
		/* color_prom now points to the beginning of the char lookup table */
	
		/* fg chars */
		for (i = 0; i < 256; i++)
			colortable[i + 0x000] = (char) (16 + (color_prom.read((i & 0xc0) | ((i & 3) << 4) | ((i >> 2) & 15)) & 15));
		color_prom.inc(256);
	
		/* color_prom now points to the beginning of the sprite lookup table */
	
		/* sprites */
		for (i = 0; i < 256; i++)
			colortable[i + 0x100] = (char) (16 + (color_prom.read((i & 0xc0) | ((i & 3) << 4) | ((i >> 2) & 15)) & 15));
		color_prom.inc(256);
	
		/* bg chars (this is not the full story... there are four layers mixed */
		/* using another PROM */
		for (i = 0; i < 256; i++)
			colortable[i + 0x200] = (char) (color_prom.readinc() & 15);
            }
        };
	
	/*************************************
	 *
	 *		Video system startup
	 *
	 *************************************/
	
	public static VhStartPtr exerion_vh_start = new VhStartPtr() { public int handler() 
	{
		UShortPtr dst=new UShortPtr();
		UBytePtr src = new UBytePtr();
		int i, x, y;
	
	/*TODO*///#ifdef DEBUG_SPRITES
	/*TODO*///	sprite_log = fopen ("sprite.log","w");
	/*TODO*///#endif
	
		/* get pointers to the mixing and lookup PROMs */
		background_mixer = new UBytePtr(memory_region(REGION_PROMS), 0x320);
	
		/* allocate memory to track the background latches */
		background_latches = new UBytePtr(Machine.drv.screen_height * 16);
		if (background_latches == null)
			return 1;
	
		/* allocate memory for the decoded background graphics */
		//background_gfx[0] = malloc(2 * 256 * 256 * 4);
                background_gfx[0] = new UBytePtr(2 * 256 * 256 * 4);
		background_gfx[1] = new UBytePtr(background_gfx[0], 256 * 256);
		background_gfx[2] = new UBytePtr(background_gfx[1], 256 * 256);
		background_gfx[3] = new UBytePtr(background_gfx[2], 256 * 256);
		if (background_gfx==null)
		{
			background_latches = null;
			
			return 1;
		}
	
		/*---------------------------------
		 * Decode the background graphics
		 *
		 * We decode the 4 background layers separately, but shuffle the bits so that
		 * we can OR all four layers together. Each layer has 2 bits per pixel. Each
		 * layer is decoded into the following bit patterns:
		 *
		 *	000a 0000 00AA
		 *  00b0 0000 BB00
		 *  0c00 00CC 0000
		 *  d000 DD00 0000
		 *
		 * Where AA,BB,CC,DD are the 2bpp data for the pixel,and a,b,c,d are the OR
		 * of these two bits together.
		 */
		for (i = 0; i < 4; i++)
		{
			src = new UBytePtr(memory_region(REGION_GFX3), i * 0x2000);
			dst = new UShortPtr(background_gfx[i]);
	
			for (y = 0; y < 256; y++)
			{
				for (x = 0; x < 128; x += 4)
				{
					int data = src.readinc();
					int val;
	
					val = ((data >> 3) & 2) | ((data >> 0) & 1);
					if (val!=0) val |= 0x100 >> i;
					dst.write( (char)(val << (2 * i)));
                                        dst.offset++;
	
					val = ((data >> 4) & 2) | ((data >> 1) & 1);
					if (val!=0) val |= 0x100 >> i;
					dst.write( (char)(val << (2 * i)));
                                        dst.offset++;
	
					val = ((data >> 5) & 2) | ((data >> 2) & 1);
					if (val!=0) val |= 0x100 >> i;
					dst.write( (char)(val << (2 * i)));
                                        dst.offset++;
	
					val = ((data >> 6) & 2) | ((data >> 3) & 1);
					if (val!=0) val |= 0x100 >> i;
					dst.write( (char)(val << (2 * i)));
                                        dst.offset++;
				}
				for (x = 0; x < 128; x++){
					dst.write( (char)0 );
                                        dst.offset++;
                                }
			}
		}
	
		return generic_vh_start.handler();
	} };
	
	
	/*************************************
	 *
	 *		Video system shutdown
	 *
	 *************************************/
	
	public static VhStopPtr exerion_vh_stop = new VhStopPtr() { public void handler() 
	{
	/*TODO*///#ifdef DEBUG_SPRITES
	/*TODO*///	fclose (sprite_log);
	/*TODO*///#endif
	
		/* free the background graphics data */
		if (background_gfx != null)
			background_gfx = null;
		
		/*TODO*///background_gfx[1] = null;
		/*TODO*///background_gfx[2] = null;
		/*TODO*///background_gfx[3] = null;
	
		/* free the background latches data */
		if (background_latches != null)
			background_latches = null;
		
	
		generic_vh_stop.handler();
	} };
	
	
	
	/*************************************
	 *
	 *		Video register I/O
	 *
	 *************************************/
	
	public static WriteHandlerPtr exerion_videoreg_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bit 0 = flip screen and joystick input multiplexor */
		exerion_cocktail_flip = data & 1;
	
		/* bits 1-2 char lookup table bank */
		char_palette = (data & 0x06) >> 1;
	
		/* bits 3 char bank */
		char_bank = (data & 0x08) >> 3;
	
		/* bits 4-5 unused */
	
		/* bits 6-7 sprite lookup table bank */
		sprite_palette = (data & 0xc0) >> 6;
	} };
	
	
	public static WriteHandlerPtr exerion_video_latch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int ybeam = cpu_getscanline();
	
		if (ybeam >= Machine.drv.screen_height)
			ybeam = Machine.drv.screen_height - 1;
	
		/* copy data up to and including the current scanline */
		while (ybeam != last_scanline_update)
		{
			last_scanline_update = (last_scanline_update + 1) % Machine.drv.screen_height;
			memcpy(new UBytePtr(background_latches, last_scanline_update * 16), current_latches, 16);
		}
	
		/* modify data on the current scanline */
		if (offset != -1)
			current_latches.write(offset, data);
	} };
	
	
	public static ReadHandlerPtr exerion_video_timing_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* bit 1 is VBLANK */
		/* bit 0 is the SNMI signal, which is low for H >= 0x1c0 and /VBLANK */
	
		int xbeam = cpu_gethorzbeampos();
		int ybeam = cpu_getscanline();
		int result = 0;
	
		if (ybeam >= VISIBLE_Y_MAX)
			result |= 2;
		if (xbeam < 0x1c0 && ybeam < VISIBLE_Y_MAX)
			result |= 1;
	
		return result;
	} };
	
	
	/*************************************
	 *
	 *		Background rendering
	 *
	 *************************************/
	
	public static void draw_background(mame_bitmap bitmap)
	{
		UBytePtr latches = new UBytePtr(background_latches, VISIBLE_Y_MIN * 16);
		int x, y;
	
		/* loop over all visible scanlines */
		for (y = VISIBLE_Y_MIN; y < VISIBLE_Y_MAX; y++, latches.inc(16))
		{
			UShortPtr src0 = new UShortPtr(background_gfx[0], latches.read(1) * 256);
			UShortPtr src1 = new UShortPtr(background_gfx[1], latches.read(3) * 256);
			UShortPtr src2 = new UShortPtr(background_gfx[2], latches.read(5) * 256);
			UShortPtr src3 = new UShortPtr(background_gfx[3], latches.read(7) * 256);
			int xoffs0 = latches.read(0);
			int xoffs1 = latches.read(2);
			int xoffs2 = latches.read(4);
			int xoffs3 = latches.read(6);
			int start0 = latches.read(8) & 0x0f;
			int start1 = latches.read(9) & 0x0f;
			int start2 = latches.read(10) & 0x0f;
			int start3 = latches.read(11) & 0x0f;
			int stop0 = latches.read(8) >> 4;
			int stop1 = latches.read(9) >> 4;
			int stop2 = latches.read(10) >> 4;
			int stop3 = latches.read(11) >> 4;
			UBytePtr mixer = new UBytePtr(background_mixer, (latches.read(12) << 4) & 0xf0);
			UBytePtr scanline=new UBytePtr(VISIBLE_X_MAX);
			IntArray pens;
	
			/* the cocktail flip flag controls whether we count up or down in X */
			if (exerion_cocktail_flip == 0)
			{
				/* skip processing anything that's not visible */
				for (x = BACKGROUND_X_START; x < VISIBLE_X_MIN; x++)
				{
					if ((++xoffs0 & 0x1f)==0){ start0++; stop0++;};
					if ((++xoffs1 & 0x1f)==0){ start1++; stop1++;};
					if ((++xoffs2 & 0x1f)==0){ start2++; stop2++;};
					if ((++xoffs3 & 0x1f)==0){ start3++; stop3++;};
				}
	
				/* draw the rest of the scanline fully */
				for (x = VISIBLE_X_MIN; x < VISIBLE_X_MAX; x++)
				{
					int combined = 0;
					int lookupval;
	
					/* the output enable is controlled by the carries on the start/stop counters */
					/* they are only active when the start has carried but the stop hasn't */
					if (((start0 ^ stop0) & 0x10)!=0) combined |= src0.read(xoffs0 & 0xff);
					if (((start1 ^ stop1) & 0x10)!=0) combined |= src1.read(xoffs1 & 0xff);
					if (((start2 ^ stop2) & 0x10)!=0) combined |= src2.read(xoffs2 & 0xff);
					if (((start3 ^ stop3) & 0x10)!=0) combined |= src3.read(xoffs3 & 0xff);
	
					/* bits 8-11 of the combined value contains the lookup for the mixer PROM */
					lookupval = mixer.read(combined >> 8) & 3;
	
					/* the color index comes from the looked up value combined with the pixel data */
					scanline.write(x, (lookupval << 2) | ((combined >> (2 * lookupval)) & 3));
	
					/* the start/stop counters are clocked when the low 5 bits of the X counter overflow */
					if ((++xoffs0 & 0x1f)==0) {start0++; stop0++;};
					if ((++xoffs1 & 0x1f)==0) {start1++; stop1++;}
					if ((++xoffs2 & 0x1f)==0) {start2++; stop2++;}
					if ((++xoffs3 & 0x1f)==0) {start3++; stop3++;}
				}
			}
			else
			{
				/* skip processing anything that's not visible */
				for (x = BACKGROUND_X_START; x < VISIBLE_X_MIN; x++)
				{
					if ((xoffs0-- & 0x1f)==0){ start0++; stop0++;}
					if ((xoffs1-- & 0x1f)==0){ start1++; stop1++;}
					if ((xoffs2-- & 0x1f)==0){ start2++; stop2++;}
					if ((xoffs3-- & 0x1f)==0){ start3++; stop3++;}
				}
	
				/* draw the rest of the scanline fully */
				for (x = VISIBLE_X_MIN; x < VISIBLE_X_MAX; x++)
				{
					int combined = 0;
					int lookupval;
	
					/* the output enable is controlled by the carries on the start/stop counters */
					/* they are only active when the start has carried but the stop hasn't */
					if (((start0 ^ stop0) & 0x10) != 0) combined |= src0.read(xoffs0 & 0xff);
					if (((start1 ^ stop1) & 0x10) != 0) combined |= src1.read(xoffs1 & 0xff);
					if (((start2 ^ stop2) & 0x10) != 0) combined |= src2.read(xoffs2 & 0xff);
					if (((start3 ^ stop3) & 0x10) != 0) combined |= src3.read(xoffs3 & 0xff);
	
					/* bits 8-11 of the combined value contains the lookup for the mixer PROM */
					lookupval = mixer.read(combined >> 8) & 3;
	
					/* the color index comes from the looked up value combined with the pixel data */
					scanline.write(x, (lookupval << 2) | ((combined >> (2 * lookupval)) & 3));
	
					/* the start/stop counters are clocked when the low 5 bits of the X counter overflow */
					if ((xoffs0-- & 0x1f)==0){ start0++; stop0++;}
					if ((xoffs1-- & 0x1f)==0){ start1++; stop1++;}
					if ((xoffs2-- & 0x1f)==0){ start2++; stop2++;}
					if ((xoffs3-- & 0x1f)==0){ start3++; stop3++;}
				}
			}
	
			/* draw the scanline */
			pens = new IntArray(Machine.remapped_colortable, 0x200 + (latches.read(12) >> 4) * 16);
			draw_scanline8(bitmap, VISIBLE_X_MIN, y, VISIBLE_X_MAX - VISIBLE_X_MIN, new UBytePtr(scanline, VISIBLE_X_MIN), pens, -1);
		}
	}
	
	
	/*************************************
	 *
	 *		Core refresh routine
	 *
	 *************************************/
	
	public static VhUpdatePtr exerion_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int sx, sy, offs, i;
	
		/* finish updating the scanlines */
		exerion_video_latch_w.handler(-1, 0);
	
		/* draw background */
		draw_background(bitmap);
	
	/*TODO*///#ifdef DEBUG_SPRITES
	/*TODO*///	if (sprite_log)
	/*TODO*///	{
	/*TODO*///		int i;
	/*TODO*///
	/*TODO*///		for (i = 0; i < spriteram_size; i+= 4)
	/*TODO*///		{
	/*TODO*///			if (spriteram[i+2] == 0x02)
	/*TODO*///			{
	/*TODO*///				fprintf (sprite_log, "%02x %02x %02x %02x\n",spriteram[i], spriteram[i+1], spriteram[i+2], spriteram[i+3]);
	/*TODO*///			}
	/*TODO*///		}
	/*TODO*///	}
	/*TODO*///#endif
	
		/* draw sprites */
		for (i = 0; i < spriteram_size[0]; i += 4)
		{
			int flags = spriteram.read(i + 0);
			int y = spriteram.read(i + 1) ^ 255;
			int code = spriteram.read(i + 2);
			int x = spriteram.read(i + 3) * 2 + 72;
	
			int xflip = flags & 0x80;
			int yflip = flags & 0x40;
			int doubled = flags & 0x10;
			int wide = flags & 0x08;
			int code2 = code;
	
			int color = ((flags >> 1) & 0x03) | ((code >> 5) & 0x04) | (code & 0x08) | (sprite_palette * 16);
			GfxElement gfx = doubled!=0 ? Machine.gfx[2] : Machine.gfx[1];
	
			if (exerion_cocktail_flip != 0)
			{
				x = 64*8 - gfx.width - x;
				y = 32*8 - gfx.height - y;
				if (wide!=0) y -= gfx.height;
				xflip = xflip!=0?0:1;
				yflip = yflip!=0?0:1;
			}
	
			if (wide != 0)
			{
				if (yflip != 0){
					code |= 0x10; code2 &= ~0x10;
                                } else {
					code &= ~0x10; code2 |= 0x10;
                                }
	
				drawgfx(bitmap, gfx, code2, color, xflip, yflip, x, y + gfx.height,
				        Machine.visible_area, TRANSPARENCY_COLOR, 16);
			}
	
			drawgfx(bitmap, gfx, code, color, xflip, yflip, x, y,
			        Machine.visible_area, TRANSPARENCY_COLOR, 16);
	
			if (doubled != 0) i += 4;
		}
	
		/* draw the visible text layer */
		for (sy = VISIBLE_Y_MIN/8; sy < VISIBLE_Y_MAX/8; sy++)
			for (sx = VISIBLE_X_MIN/8; sx < VISIBLE_X_MAX/8; sx++)
			{
				int x = exerion_cocktail_flip!=0 ? (63*8 - 8*sx) : 8*sx;
				int y = exerion_cocktail_flip!=0 ? (31*8 - 8*sy) : 8*sy;
	
				offs = sx + sy * 64;
				drawgfx(bitmap, Machine.gfx[0],
					videoram.read(offs) + 256 * char_bank,
					((videoram.read(offs) & 0xf0) >> 4) + char_palette * 16,
					exerion_cocktail_flip, exerion_cocktail_flip, x, y,
					Machine.visible_area, TRANSPARENCY_PEN, 0);
			}
	} };
}
