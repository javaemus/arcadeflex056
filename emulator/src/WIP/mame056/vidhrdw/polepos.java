/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;

import static common.ptr.*;
import static common.subArrays.*;
import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.memoryH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.inptport.readinputport;

public class polepos
{
	
	public static UBytePtr polepos_view16_memory = new UBytePtr();
	public static UBytePtr polepos_road16_memory = new UBytePtr();
	public static UBytePtr polepos_sprite16_memory = new UBytePtr();
	public static UBytePtr polepos_alpha16_memory = new UBytePtr();
	
	/* modified vertical position built from three nibbles (12 bit)
	 * of ROMs 136014-142, 136014-143, 136014-144
	 * The value RVP (road vertical position, lower 12 bits) is added
	 * to this value and the upper 10 bits of the result are used to
	 * address the playfield video memory (AB0 - AB9).
	 */
	static int[] polepos_vertical_position_modifier=new int[256];
	
	static int view16_hscroll;
	static int road16_vscroll;
	
	static UBytePtr road_control = new UBytePtr();
	static UBytePtr road_bits1 = new UBytePtr();
	static UBytePtr road_bits2 = new UBytePtr();
	
	static mame_bitmap view_bitmap;
	static UBytePtr view_dirty = new UBytePtr();
	
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Pole Position has three 256x4 palette PROMs (one per gun)
	  and a lot ;-) of 256x4 lookup table PROMs.
	  The palette PROMs are connected to the RGB output this way:
	
	  bit 3 -- 220 ohm resistor  -- RED/GREEN/BLUE
			-- 470 ohm resistor  -- RED/GREEN/BLUE
			-- 1  kohm resistor  -- RED/GREEN/BLUE
	  bit 0 -- 2.2kohm resistor  -- RED/GREEN/BLUE
	
	***************************************************************************/
	
	public static VhConvertColorPromPtr polepos_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i, j;
                int _palette = 0;
	
		/*******************************************************
		 * Color PROMs
		 * Sheet 15B: middle, 136014-137,138,139
		 * Inputs: MUX0 ... MUX3, ALPHA/BACK, SPRITE/BACK, 128V, COMPBLANK
		 *
		 * Note that we only decode the lower 128 colors because
		 * the upper 128 are all black and used during the
		 * horizontal and vertical blanking periods.
		 *******************************************************/
		for (i = 0; i < 128; i++)
		{
			int bit0,bit1,bit2,bit3;
	
			/* Sheet 15B: 136014-0137 red component */
			bit0 = (color_prom.read(0x000 + i) >> 0) & 1;
			bit1 = (color_prom.read(0x000 + i) >> 1) & 1;
			bit2 = (color_prom.read(0x000 + i) >> 2) & 1;
			bit3 = (color_prom.read(0x000 + i) >> 3) & 1;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
	
			/* Sheet 15B: 136014-0138 green component */
			bit0 = (color_prom.read(0x100 + i) >> 0) & 1;
			bit1 = (color_prom.read(0x100 + i) >> 1) & 1;
			bit2 = (color_prom.read(0x100 + i) >> 2) & 1;
			bit3 = (color_prom.read(0x100 + i) >> 3) & 1;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
	
			/* Sheet 15B: 136014-0139 blue component */
			bit0 = (color_prom.read(0x200 + i) >> 0) & 1;
			bit1 = (color_prom.read(0x200 + i) >> 1) & 1;
			bit2 = (color_prom.read(0x200 + i) >> 2) & 1;
			bit3 = (color_prom.read(0x200 + i) >> 3) & 1;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
		}
	
		/*******************************************************
		 * Alpha colors (colors 0x000-0x1ff)
		 * Sheet 15B: top left, 136014-140
		 * Inputs: SHFT0, SHFT1 and CHA8* ... CHA13*
		 *******************************************************/
		for (i = 0; i < 64*4; i++)
		{
			int color = color_prom.read(0x300 + i);
			colortable[0x0000 + i] = (char) ((color != 15) ? (0x020 + color) : 0);
			colortable[0x0100 + i] = (char) ((color != 15) ? (0x060 + color) : 0);
		}
	
		/*******************************************************
		 * View colors (colors 0x200-0x3ff)
		 * Sheet 13A: left, 136014-141
		 * Inputs: SHFT2, SHFT3 and CHA8 ... CHA13
		 *******************************************************/
		for (i = 0; i < 64*4; i++)
		{
			int color = color_prom.read(0x400 + i);
			colortable[0x0200 + i] = (char) (0x000 + color);
			colortable[0x0300 + i] = (char) (0x040 + color);
		}
	
		/*******************************************************
		 * Sprite colors (colors 0x400-0xbff)
		 * Sheet 14B: right, 136014-146
		 * Inputs: CUSTOM0 ... CUSTOM3 and DATA0 ... DATA5
		 *******************************************************/
		for (i = 0; i < 64*16; i++)
		{
			int color = color_prom.read(0xc00 + i);
			colortable[0x0400 + i] = (char) ((color != 15) ? (0x010 + color) : 0);
			colortable[0x0800 + i] = (char) ((color != 15) ? (0x050 + color) : 0);
		}
	
		/*******************************************************
		 * Road colors (colors 0xc00-0x13ff)
		 * Sheet 13A: bottom left, 136014-145
		 * Inputs: R1 ... R6 and CHA0 ... CHA3
		 *******************************************************/
		for (i = 0; i < 64*16; i++)
		{
			int color = color_prom.read(0x800 + i);
			colortable[0x0c00 + i] = (char) (0x000 + color);
			colortable[0x1000 + i] = (char) (0x040 + color);
		}
	
		/* 136014-142, 136014-143, 136014-144 Vertical position modifiers */
		for (i = 0; i < 256; i++)
		{
			j = color_prom.read(0x500 + i) + (color_prom.read(0x600 + i) << 4) + (color_prom.read(0x700 + i) << 8);
			polepos_vertical_position_modifier[i] = j;
		}
	
		road_control = new UBytePtr(color_prom, 0x2000);
		road_bits1 = new UBytePtr(color_prom, 0x4000);
		road_bits2 = new UBytePtr(color_prom, 0x6000);
            }
        };
	
	
	/***************************************************************************
	
	  Video initialization/shutdown
	
	***************************************************************************/
	
	public static VhStartPtr polepos_vh_start = new VhStartPtr() { public int handler() 
	{
		/* allocate view bitmap */
		view_bitmap = bitmap_alloc(64*8, 16*8);
		if (view_bitmap == null)
			return 1;
	
		/* allocate view dirty buffer */
		view_dirty = new UBytePtr(64*16);
		if (view_dirty == null)
		{
			bitmap_free(view_bitmap);
			return 1;
		}
	
		return 0;
	} };
	
	public static VhStopPtr polepos_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free(view_bitmap);
		view_dirty = null;
	} };
	
	
	/***************************************************************************
	
	  Sprite memory
	
	***************************************************************************/
	
        public static ReadHandlerPtr polepos_sprite16_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return polepos_sprite16_memory.read(offset);
            }
        };
	
	public static WriteHandlerPtr polepos_sprite16_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /*TODO*///COMBINE_DATA(polepos_sprite16_memory.read(offset));
            }
        };
	
	public static ReadHandlerPtr polepos_sprite_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return polepos_sprite16_memory.read(offset) & 0xff;
	} };
	
	public static WriteHandlerPtr polepos_sprite_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		polepos_sprite16_memory.write(offset, (polepos_sprite16_memory.read(offset) & 0xff00) | data);
	} };
	
	
	/***************************************************************************
	
	  Road memory
	
	***************************************************************************/
	
	public static ReadHandlerPtr polepos_road16_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return polepos_road16_memory.read(offset);
            }
        };
	
	public static WriteHandlerPtr polepos_road16_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /*TODO*///COMBINE_DATA(polepos_road16_memory.read(offset));
            }
        };
		
	public static ReadHandlerPtr polepos_road_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return polepos_road16_memory.read(offset) & 0xff;
	} };
	
	public static WriteHandlerPtr polepos_road_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		polepos_road16_memory.write(offset, (polepos_road16_memory.read(offset) & 0xff00) | data);
	} };
	
        public static WriteHandlerPtr polepos_road16_vscroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*TODO*///COMBINE_DATA(road16_vscroll);
	} };
	
	
	/***************************************************************************
	
	  View memory
	
	***************************************************************************/
	
	public static ReadHandlerPtr polepos_view16_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return polepos_view16_memory.read(offset);
	} };
	
	public static WriteHandlerPtr polepos_view16_w  = new WriteHandlerPtr() {
            public void handler(int offset, int data)
            {
		int oldword = polepos_view16_memory.read(offset);
		/*TODO*///COMBINE_DATA(polepos_view16_memory.read(offset);
		if (oldword != polepos_view16_memory.read(offset))
		{
			if (offset < 0x400)
				view_dirty.write(offset, 1);
		}
            }
        };
	
	public static ReadHandlerPtr polepos_view_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return polepos_view16_memory.read(offset) & 0xff;
	} };
	
	public static WriteHandlerPtr polepos_view_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int oldword = polepos_view16_memory.read(offset);
		polepos_view16_memory.write(offset, (polepos_view16_memory.read(offset) & 0xff00) | data);
		if (oldword != polepos_view16_memory.read(offset))
		{
			if (offset < 0x400)
				view_dirty.write(offset, 1);
		}
	} };
	
	public static WriteHandlerPtr polepos_view16_hscroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*TODO*///COMBINE_DATA(view16_hscroll);
	}};
	
	
	/***************************************************************************
	
	  Alpha memory
	
	***************************************************************************/
	
	public static ReadHandlerPtr polepos_alpha16_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return polepos_alpha16_memory.read(offset);
            }
        };
	
	public static WriteHandlerPtr polepos_alpha16_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /*TODO*///COMBINE_DATA(polepos_alpha16_memory.read(offset));
            }
        };
	
	public static ReadHandlerPtr polepos_alpha_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return polepos_alpha16_memory.read(offset) & 0xff;
	} };
	
	public static WriteHandlerPtr polepos_alpha_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		polepos_alpha16_memory.write(offset, (polepos_alpha16_memory.read(offset) & 0xff00) | data);
	} };
	
	
	/***************************************************************************
	
	  Internal draw routines
	
	***************************************************************************/
	
	static void draw_view(mame_bitmap bitmap)
	{
		rectangle clip = Machine.visible_area;
		int x, y, offs;
	
		/* look for dirty tiles */
		for (x = offs = 0; x < 64; x++)
			for (y = 0; y < 16; y++, offs++)
				if (view_dirty.read(offs) != 0)
				{
					int word = polepos_view16_memory.read(offs);
					int code = (word & 0xff) | ((word >> 6) & 0x100);
					int color = (word >> 8) & 0x3f;
	
					drawgfx(view_bitmap, Machine.gfx[1], code, color,
							0, 0, 8*x, 8*y, null, TRANSPARENCY_NONE, 0);
					view_dirty.write(offs, 0);
				}
	
		/* copy the bitmap */
		x = -view16_hscroll;
		clip.max_y = 127;
		copyscrollbitmap(bitmap, view_bitmap, 1, new int[]{x}, 0, new int[]{0}, clip, TRANSPARENCY_NONE, 0);
	}
	
	static void draw_road(mame_bitmap bitmap)
	{
		int x, y, i;
	
		/* loop over the lower half of the screen */
		for (y = 128; y < 256; y++)
		{
			int xoffs, yoffs, xscroll, roadpal;
			UBytePtr scanline = new UBytePtr(256 + 8);
			UBytePtr dest = scanline;
                        int _dest = 0;
			IntArray colortable;
	
			/* first add the vertical position modifier and the vertical scroll */
			yoffs = ((polepos_vertical_position_modifier[y] + road16_vscroll) >> 3) & 0x1ff;
	
			/* then use that as a lookup into the road memory */
			roadpal = polepos_road16_memory.read(yoffs) & 15;
	
			/* this becomes the palette base for the scanline */
			colortable = new IntArray(Machine.remapped_colortable, 0x1000 + (roadpal << 6));
	
			/* now fetch the horizontal scroll offset for this scanline */
			xoffs = polepos_road16_memory.read(0x380 + (y & 0x7f)) & 0x3ff;
	
			/* the road is drawn in 8-pixel chunks, so round downward and adjust the base */
			/* note that we assume there is at least 8 pixels of slop on the left/right */
			xscroll = xoffs & 7;
			xoffs &= ~7;
	
			/* loop over 8-pixel chunks */
			for (x = 0; x < 256 / 8 + 1; x++, xoffs += 8)
			{
				/* if the 0x200 bit of the xoffset is set, a special pin on the custom */
				/* chip is set and the /CE and /OE for the road chips is disabled */
				if ((xoffs & 0x200) != 0)
				{
					/* in this case, it looks like we just fill with 0 */
					for (i = 0; i < 8; i++)
						dest.write(_dest++, 0);
				}
	
				/* otherwise, we clock in the bits and compute the road value */
				else
				{
					/* the road ROM offset comes from the current scanline and the X offset */
					int romoffs = ((y & 0x07f) << 6) + ((xoffs & 0x1ff) >> 3);
	
					/* fetch the current data from the road ROMs */
					int control = road_control.read(romoffs);
					int bits1 = road_bits1.read(romoffs);
					int bits2 = road_bits2.read((romoffs & 0xfff) | ((romoffs >> 1) & 0x800));
	
					/* extract the road value and the carry-in bit */
					int roadval = control & 0x3f;
					int carin = control >> 7;
	
					/* draw this 8-pixel chunk */
					for (i = 0; i < 8; i++, bits1 <<= 1, bits2 <<= 1)
					{
						int bits = ((bits1 >> 7) & 1) + ((bits2 >> 6) & 2);
						if (carin==0 && bits==0) bits++;
						dest.write(_dest++, roadval & 0x3f);
						roadval += bits;
					}
				}
			}
	
			/* draw the scanline */
			draw_scanline8(bitmap, 0, y, 256, new UBytePtr(scanline, xscroll), colortable, -1);
		}
	}
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		UBytePtr posmem = new UBytePtr(polepos_sprite16_memory, 0x380);
		UBytePtr sizmem = new UBytePtr(polepos_sprite16_memory, 0x780);
		int i;
	
		for (i = 0; i < 64; i++, posmem.inc(2), sizmem.inc(2))
		{
                        GfxElement gfx = Machine.gfx[(sizmem.read(0) & 0x8000)!=0 ? 3 : 2];
			int vpos = (~posmem.read(0) & 0x1ff) + 4;
			int hpos = (posmem.read(1) & 0x3ff) - 0x40;
			int vsize = ((sizmem.read(0) >> 8) & 0x3f) + 1;
			int hsize = ((sizmem.read(1) >> 8) & 0x3f) + 1;
			int code = sizmem.read(0) & 0x7f;
			int hflip = sizmem.read(0) & 0x80;
			int color = sizmem.read(1) & 0x3f;
	
			if (vpos >= 128) color |= 0x40;
			/*TODO*///drawgfxzoom(bitmap, gfx,
			/*TODO*///		 code, color, hflip, 0, hpos, vpos,
			/*TODO*///		 Machine.visible_area, TRANSPARENCY_COLOR, 0, hsize << 11, vsize << 11);
		}
	}
	
	static void draw_alpha(mame_bitmap bitmap)
	{
		int x, y, offs, in;
	
		for (y = offs = 0; y < 32; y++)
			for (x = 0; x < 32; x++, offs++)
			{
				int word = polepos_alpha16_memory.read(offs);
				int code = (word & 0xff) | ((word >> 6) & 0x100);
				int color = (word >> 8) & 0x3f; /* 6 bits color */
	
				if (y >= 16) color |= 0x40;
				drawgfx(bitmap, Machine.gfx[0],
						 code, color, 0, 0, 8*x, 8*y,
						 Machine.visible_area, TRANSPARENCY_COLOR, 0);
			}
	
		/* Now draw the shift if selected on the fake dipswitch */
		in = readinputport( 0 );
	
		if (( in & 8 ) != 0) {
			if ( ( in & 2 ) == 0 ) {
				/* L */
				drawgfx(bitmap, Machine.gfx[0],
						 0x15, 0, 0, 0, 30*8-1, 29*8,
						 Machine.visible_area, TRANSPARENCY_PEN, 0);
				/* O */
				drawgfx(bitmap, Machine.gfx[0],
						 0x18, 0, 0, 0, 31*8-1, 29*8,
						 Machine.visible_area, TRANSPARENCY_PEN, 0);
			} else {
				/* H */
				drawgfx(bitmap, Machine.gfx[0],
						 0x11, 0, 0, 0, 30*8-1, 29*8,
						 Machine.visible_area, TRANSPARENCY_PEN, 0);
				/* I */
				drawgfx(bitmap, Machine.gfx[0],
						 0x12, 0, 0, 0, 31*8-1, 29*8,
						 Machine.visible_area, TRANSPARENCY_PEN, 0);
			}
		}
	}
	
	
	/***************************************************************************
	
	  Master refresh routine
	
	***************************************************************************/
	
	public static VhUpdatePtr polepos_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		draw_view(bitmap);
		draw_road(bitmap);
		draw_sprites(bitmap);
		draw_alpha(bitmap);
	} };
}
