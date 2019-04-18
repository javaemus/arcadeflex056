/*************************************************************************

	 Turbo - Sega - 1981

	 Video Hardware

*************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;

import static common.ptr.*;
import common.subArrays.IntArray;
import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.inptport.readinputport;


public class turbo
{
	
	/* constants */
	public static int VIEW_WIDTH    =   (32*8);
	public static int VIEW_HEIGHT   =   (28*8);
	
	/* external definitions */
	static UBytePtr turbo_sprite_position = new UBytePtr();
	static int turbo_collision;
	
	/* internal data */
	static UBytePtr sprite_gfxdata=new UBytePtr(), sprite_priority=new UBytePtr();
	static UBytePtr road_gfxdata=new UBytePtr(), road_palette=new UBytePtr(), road_enable_collide=new UBytePtr();
	static UBytePtr back_gfxdata=new UBytePtr(), back_palette=new UBytePtr();
	static UBytePtr overall_priority=new UBytePtr(), collision_map=new UBytePtr();
	
	/* sprite tracking */
	public static class sprite_params_data
	{
		public /*UINT32*/ IntArray base;
		public int offset, rowbytes;
		public int yscale, miny, maxy;
		public int xscale, xoffs;
	};
	static sprite_params_data[] sprite_params=new sprite_params_data[16];
	static /*UINT32*/ IntArray sprite_expanded_data;
	
	/* misc other stuff */
	static UShortPtr back_expanded_data=new UShortPtr();
	static UShortPtr road_expanded_palette=new UShortPtr();
	static int drew_frame;
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	***************************************************************************/
	
	public static VhConvertColorPromPtr turbo_vh_convert_color_prom= new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
                int _color_prom = 0;
	
		for (i = 0; i < 512; i++, _color_prom++)
		{
			int bit0, bit1, bit2;
	
			/* bits 4,5,6 of the index are inverted before being used as addresses */
			/* to save ourselves lots of trouble, we will undo the inversion when */
			/* generating the palette */
			int adjusted_index = i ^ 0x70;
	
			/* red component */
			bit0 = (color_prom.read() >> 0) & 1;
			bit1 = (color_prom.read() >> 1) & 1;
			bit2 = (color_prom.read() >> 2) & 1;
			palette[adjusted_index * 3 + 0] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
	
			/* green component */
			bit0 = (color_prom.read() >> 3) & 1;
			bit1 = (color_prom.read() >> 4) & 1;
			bit2 = (color_prom.read() >> 5) & 1;
			palette[adjusted_index * 3 + 1] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
	
			/* blue component */
			bit0 = 0;
			bit1 = (color_prom.read() >> 6) & 1;
			bit2 = (color_prom.read() >> 7) & 1;
			palette[adjusted_index * 3 + 2] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
		}
	
		/* LED segments colors: black and red */
		palette[512 * 3 + 0] = 0x00;
		palette[512 * 3 + 1] = 0x00;
		palette[512 * 3 + 2] = 0x00;
		palette[513 * 3 + 0] = 0xff;
		palette[513 * 3 + 1] = 0x00;
		palette[513 * 3 + 2] = 0x00;
		/* Tachometer colors: Led colors + yellow and green */
		palette[514 * 3 + 0] = 0x00;
		palette[514 * 3 + 1] = 0x00;
		palette[514 * 3 + 2] = 0x00;
		palette[515 * 3 + 0] = 0xff;
		palette[515 * 3 + 1] = 0xff;
		palette[515 * 3 + 2] = 0x00;
		palette[516 * 3 + 0] = 0x00;
		palette[516 * 3 + 1] = 0x00;
		palette[516 * 3 + 2] = 0x00;
		palette[517 * 3 + 0] = 0x00;
		palette[517 * 3 + 1] = 0xff;
		palette[517 * 3 + 2] = 0x00;
            }
        };
	
	/***************************************************************************
	
	  Video startup/shutdown
	
	***************************************************************************/
	
	public static VhStartPtr turbo_vh_start = new VhStartPtr() { public int handler() 
	{
		int i, j, sprite_length, sprite_bank_size, back_length;
		IntArray sprite_expand=new IntArray(16);
		IntArray dst;
		UShortPtr bdst=new UShortPtr();
		UBytePtr src=new UBytePtr();
	
		/* allocate the expanded sprite data */
		sprite_length = memory_region_length(REGION_GFX1);
		sprite_bank_size = sprite_length / 8;
		sprite_expanded_data = new IntArray(sprite_length * 2);
		if (sprite_expanded_data == null)
			return 1;
	
		/* allocate the expanded background data */
		back_length = memory_region_length(REGION_GFX3);
		back_expanded_data = new UShortPtr(back_length);
		if (back_expanded_data == null)
		{
			sprite_expanded_data = null;
			return 1;
		}
	
		/* allocate the expanded road palette */
		road_expanded_palette = new UShortPtr(0x40);
		if (road_expanded_palette == null)
		{
			back_expanded_data = null;
			sprite_expanded_data = null;
			return 1;
		}
	
		/* determine ROM/PROM addresses */
		sprite_gfxdata = memory_region(REGION_GFX1);
		sprite_priority = new UBytePtr(memory_region(REGION_PROMS), 0x0200);
	
		road_gfxdata = memory_region(REGION_GFX2);
		road_palette = new UBytePtr(memory_region(REGION_PROMS), 0x0b00);
		road_enable_collide = new UBytePtr(memory_region(REGION_PROMS), 0x0b40);
	
		back_gfxdata = memory_region(REGION_GFX3);
		back_palette = new UBytePtr(memory_region(REGION_PROMS), 0x0a00);
	
		overall_priority = new UBytePtr(memory_region(REGION_PROMS), 0x0600);
		collision_map = new UBytePtr(memory_region(REGION_PROMS), 0x0b60);
	
		/* compute the sprite expansion array */
		for (i = 0; i < 16; i++)
		{
			int value = 0;
			if ((i & 1)!=0) value |= 0x00000001;
			if ((i & 2)!=0) value |= 0x00000100;
			if ((i & 4)!=0) value |= 0x00010000;
			if ((i & 8)!=0) value |= 0x01000000;
	
			/* special value for the end-of-row */
			if ((i & 0x0c) == 0x04) value = 0x12345678;
	
			sprite_expand.write(i, value);
		}
	
		/* expand the sprite ROMs */
		src = sprite_gfxdata;
		dst = sprite_expanded_data;
		for (i = 0; i < 8; i++)
		{
			/* expand this bank */
			for (j = 0; j < sprite_bank_size; j++)
			{
				dst.writeinc(sprite_expand.read(src.read() >> 4));
                                src.inc();
                                dst.writeinc(sprite_expand.read(src.read() & 15));                                
			}
	
			/* shift for the next bank */
			for (j = 0; j < 16; j++)
				if (sprite_expand.read(j) != 0x12345678) sprite_expand.write(j, sprite_expand.read(j)<< 1);
		}
	
		/* expand the background ROMs */
		src = back_gfxdata;
		bdst = back_expanded_data;
		for (i = 0; i < back_length / 2; i++, src.inc())
		{
			int bits1 = src.read(0);
			int bits2 = src.read(back_length / 2);
			int newbits = 0;
	
			for (j = 0; j < 8; j++)
			{
				newbits |= ((bits1 >> (j ^ 7)) & 1) << (j * 2);
				newbits |= ((bits2 >> (j ^ 7)) & 1) << (j * 2 + 1);
			}
			bdst.inc( newbits );
		}
	
		/* expand the road palette */
		src = road_palette;
		bdst = road_expanded_palette;
		for (i = 0; i < 0x20; i++, src.inc()){
			bdst.write((char) (src.read(0) | (src.read(0x20) << 8)));
                        bdst.inc();
                }
		/* other stuff */
		drew_frame = 0;
	
		/* return success */
		return 0;
	} };
	
	
	public static VhStopPtr turbo_vh_stop = new VhStopPtr() { public void handler() 
	{
		sprite_expanded_data = null;
		back_expanded_data = null;
		road_expanded_palette = null;
	} };
	
	
	/***************************************************************************
	
	  Sprite information
	
	***************************************************************************/
	
	static void update_sprite_info()
	{
		sprite_params_data[] data = sprite_params;
                int _data = 0;
		int i;
	
		/* first loop over all sprites and update those whose scanlines intersect ours */
		for (i = 0; i < 16; i++, _data++)
		{
			UBytePtr sprite_base = new UBytePtr(spriteram, 16 * i);
	
			/* snarf all the data */
			data[_data].base = new IntArray(sprite_expanded_data, (i & 7) * 0x8000);
			data[_data].offset = (sprite_base.read(6) + 256 * sprite_base.read(7)) & 0x7fff;
			data[_data].rowbytes = (sprite_base.read(4) + 256 * sprite_base.read(5));
			data[_data].miny = sprite_base.read(0);
			data[_data].maxy = sprite_base.read(1);
			data[_data].xscale = ((5 * 256 - 4 * sprite_base.read(2)) << 16) / (5 * 256);
			data[_data].yscale = (4 << 16) / (sprite_base.read(3) + 4);
			data[_data].xoffs = -1;
		}
	
		/* now find the X positions */
		for (i = 0; i < 0x200; i++)
		{
			int value = turbo_sprite_position.read(i);
			if (value != 0)
			{
				int base = (i & 0x100) >> 5;
				int which;
				for (which = 0; which < 8; which++)
					if ((value & (1 << which)) != 0)
						sprite_params[base + which].xoffs = i & 0xff;
			}
		}
	}
	
	
	/***************************************************************************
	
	  Internal draw routines
	
	***************************************************************************/
	
	static void draw_one_sprite(sprite_params_data data, IntArray dest, int xclip, int scanline)
	{
		int xstep = data.xscale;
		int xoffs = data.xoffs;
		int xcurr, offset;
		IntArray src;
	
		/* xoffs of -1 means don't draw */
		if (xoffs == -1) return;
	
		/* clip to the road */
		xcurr = 0;
		if (xoffs < xclip)
		{
			/* the pixel clock starts on xoffs regardless of clipping; take this into account */
			xcurr = ((xclip - xoffs) * xstep) & 0xffff;
			xoffs = xclip;
		}
	
		/* compute the current data offset */
		scanline = ((scanline - data.miny) * data.yscale) >> 16;
		offset = data.offset + (scanline + 1) * data.rowbytes;
	
		/* determine the bitmap location */
		src = new IntArray(data.base,offset & 0x7fff);
	
		/* loop over columns */
		while (xoffs < VIEW_WIDTH)
		{
			IntArray srcval = new IntArray(src, xcurr >> 16);
	
			/* stop on the end-of-row signal */
			if (srcval.read() == 0x12345678) break;
			dest.write(xoffs++, (dest.read(xoffs)| srcval.read()));
			xcurr += xstep;
		}
	}
	
        static void draw_road_sprites(IntArray dest, int scanline)
	{
                sprite_params_data[] param_list =
                {
                        sprite_params[0], sprite_params[8],
                        sprite_params[1], sprite_params[9],
                        sprite_params[2], sprite_params[10]
                };
                
		int i;
	
		/* loop over the road sprites */
		for (i = 0; i < 6; i++)
		{
			sprite_params_data data = param_list[i];
	
			/* if the sprite intersects this scanline, draw it */
			if (scanline >= data.miny && scanline < data.maxy)
				draw_one_sprite(data, dest, 0, scanline);
		}
	}
	
	
	static void draw_offroad_sprites(IntArray dest, int road_column, int scanline)
	{
		sprite_params_data param_list[] =
		{
			sprite_params[3], sprite_params[11],
			sprite_params[4], sprite_params[12],
			sprite_params[5], sprite_params[13],
			sprite_params[6], sprite_params[14],
			sprite_params[7], sprite_params[15]
		};
		int i;
	
		/* loop over the offroad sprites */
		for (i = 0; i < 10; i++)
		{
			sprite_params_data data = param_list[i];
	
			/* if the sprite intersects this scanline, draw it */
			if (scanline >= data.miny && scanline < data.maxy)
				draw_one_sprite(data, dest, road_column, scanline);
		}
	}
	
	
	static void draw_scores(mame_bitmap bitmap)
	{
		rectangle clip;
		int offs, x, y;
	
		/* current score */
		offs = 31;
		/*TODO*///for (y = 0; y < 5; y++, offs--)
			/*TODO*///drawgfx(bitmap, Machine.gfx[0],
			/*TODO*///		turbo_segment_data[offs],
			/*TODO*///		0,
			/*TODO*///		0, 0,
			/*TODO*///		14*8, (2 + y) * 8,
			/*TODO*///		Machine.visible_area, TRANSPARENCY_NONE, 0);
	
		/* high scores */
		for (x = 0; x < 5; x++)
		{
			offs = 6 + x * 5;
			/*TODO*///for (y = 0; y < 5; y++, offs--)
			/*TODO*///	drawgfx(bitmap, Machine.gfx[0],
			/*TODO*///			turbo_segment_data[offs],
			/*TODO*///			0,
			/*TODO*///			0, 0,
			/*TODO*///			(20 + 2 * x) * 8, (2 + y) * 8,
			/*TODO*///			Machine.visible_area, TRANSPARENCY_NONE, 0);
		}
	
		/* tachometer */
		clip = Machine.visible_area;
		clip.min_x = 5*8;
		clip.max_x = clip.min_x + 1;
		for (y = 0; y < 22; y++)
		{
			int led_color[] = { 2, 2, 2, 2, 1, 1, 0, 0, 0, 0, 0 };
			/*TODO*///int code = ((y / 2) <= turbo_speed) ? 0 : 1;
	
			/*TODO*///drawgfx(bitmap, Machine.gfx[1],
			/*TODO*///		code,
			/*TODO*///		led_color[y / 2],
			/*TODO*///		0,0,
			/*TODO*///		5*8, y*2+8,
			/*TODO*///		clip, TRANSPARENCY_NONE, 0);
			if (y % 3 == 2)
				clip.max_x++;
		}
	
		/* shifter status */
		if ((readinputport(0) & 0x04) != 0)
		{
			drawgfx(bitmap, Machine.gfx[2], 'H', 0, 0,0, 2*8,3*8, Machine.visible_area, TRANSPARENCY_NONE, 0);
			drawgfx(bitmap, Machine.gfx[2], 'I', 0, 0,0, 2*8,4*8, Machine.visible_area, TRANSPARENCY_NONE, 0);
		}
		else
		{
			drawgfx(bitmap, Machine.gfx[2], 'L', 0, 0,0, 2*8,3*8, Machine.visible_area, TRANSPARENCY_NONE, 0);
			drawgfx(bitmap, Machine.gfx[2], 'O', 0, 0,0, 2*8,4*8, Machine.visible_area, TRANSPARENCY_NONE, 0);
		}
	}
	
	
	
	/***************************************************************************
	
		Core drawing routine
	
	***************************************************************************/
	
	/*TODO*///static void draw_everything(mame_bitmap bitmap, int yoffs)
/*TODO*///	{
/*TODO*///		UBytePtr overall_priority_base = new UBytePtr(overall_priority, (turbo_fbpla & 8) << 6);
/*TODO*///		UINT8 *sprite_priority_base = &sprite_priority[(turbo_fbpla & 7) << 7];
/*TODO*///		UINT8 *road_gfxdata_base = &road_gfxdata[(turbo_opc << 5) & 0x7e0];
/*TODO*///		UINT16 *road_palette_base = &road_expanded_palette[(turbo_fbcol & 1) << 4];
/*TODO*///		pen_t *colortable;
/*TODO*///		int x, y, i;
/*TODO*///	
/*TODO*///		/* determine the color offset */
/*TODO*///		colortable = &Machine.pens[(turbo_fbcol & 6) << 6];
/*TODO*///	
/*TODO*///		/* loop over rows */
/*TODO*///		for (y = 4; y < VIEW_HEIGHT - 4; y++)
/*TODO*///		{
/*TODO*///			int sel, coch, babit, slipar_acciar, area, area1, area2, area3, area4, area5, road = 0;
/*TODO*///			UINT32 sprite_buffer[VIEW_WIDTH + 256];
/*TODO*///			UINT32 *sprite_data = sprite_buffer;
/*TODO*///			UINT8 scanline[VIEW_WIDTH];
/*TODO*///	
/*TODO*///			/* compute the Y sum between opa and the current scanline (p. 141) */
/*TODO*///			int va = (y + turbo_opa) & 0xff;
/*TODO*///	
/*TODO*///			/* the upper bit of OPC inverts the road */
/*TODO*///			if (!(turbo_opc & 0x80)) va ^= 0xff;
/*TODO*///	
/*TODO*///			/* clear the sprite buffer and draw the road sprites */
/*TODO*///			memset(sprite_buffer, 0, VIEW_WIDTH * sizeof(UINT32));
/*TODO*///			draw_road_sprites(sprite_buffer, y);
/*TODO*///	
/*TODO*///			/* loop over 8-pixel chunks */
/*TODO*///			sprite_data += 8;
/*TODO*///			for (x = 8; x < VIEW_WIDTH; x += 8)
/*TODO*///			{
/*TODO*///				int area5_buffer = road_gfxdata_base[0x4000 + (x >> 3)];
/*TODO*///				UINT8 back_data = videoram[(y / 8) * 32 + (x / 8) - 33];
/*TODO*///				UINT16 backbits_buffer = back_expanded_data[(back_data << 3) | (y & 7)];
/*TODO*///	
/*TODO*///				/* loop over columns */
/*TODO*///				for (i = 0; i < 8; i++)
/*TODO*///				{
/*TODO*///					UINT32 sprite = *sprite_data++;
/*TODO*///	
/*TODO*///					/* compute the X sum between opb and the current column; only the carry matters (p. 141) */
/*TODO*///					int carry = (x + i + turbo_opb) >> 8;
/*TODO*///	
/*TODO*///					/* the carry selects which inputs to use (p. 141) */
/*TODO*///					if (carry)
/*TODO*///					{
/*TODO*///						sel	 = turbo_ipb;
/*TODO*///						coch = turbo_ipc >> 4;
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						sel	 = turbo_ipa;
/*TODO*///						coch = turbo_ipc & 15;
/*TODO*///					}
/*TODO*///	
/*TODO*///					/* at this point we also compute area5 (p. 141) */
/*TODO*///					area5 = (area5_buffer >> 3) & 0x10;
/*TODO*///					area5_buffer <<= 1;
/*TODO*///	
/*TODO*///					/* now look up the rest of the road bits (p. 142) */
/*TODO*///					area1 = road_gfxdata[0x0000 | ((sel & 15) << 8) | va];
/*TODO*///					area1 = ((area1 + x + i) >> 8) & 0x01;
/*TODO*///					area2 = road_gfxdata[0x1000 | ((sel & 15) << 8) | va];
/*TODO*///					area2 = ((area2 + x + i) >> 7) & 0x02;
/*TODO*///					area3 = road_gfxdata[0x2000 | ((sel >> 4) << 8) | va];
/*TODO*///					area3 = ((area3 + x + i) >> 6) & 0x04;
/*TODO*///					area4 = road_gfxdata[0x3000 | ((sel >> 4) << 8) | va];
/*TODO*///					area4 = ((area4 + x + i) >> 5) & 0x08;
/*TODO*///	
/*TODO*///					/* compute the final area value and look it up in IC18/PR1115 (p. 144) */
/*TODO*///					area = area5 | area4 | area3 | area2 | area1;
/*TODO*///					babit = road_enable_collide[area] & 0x07;
/*TODO*///	
/*TODO*///					/* note: SLIPAR is 0 on the road surface only */
/*TODO*///					/*		 ACCIAR is 0 on the road surface and the striped edges only */
/*TODO*///					slipar_acciar = road_enable_collide[area] & 0x30;
/*TODO*///					if (!road && (slipar_acciar & 0x20))
/*TODO*///					{
/*TODO*///						road = 1;
/*TODO*///						draw_offroad_sprites(sprite_buffer, x + i + 2, y);
/*TODO*///					}
/*TODO*///	
/*TODO*///					/* perform collision detection here */
/*TODO*///					turbo_collision |= collision_map[((sprite >> 24) & 7) | (slipar_acciar >> 1)];
/*TODO*///	
/*TODO*///					/* we only need to continue if we're actually drawing */
/*TODO*///					if (bitmap)
/*TODO*///					{
/*TODO*///						int bacol, red, grn, blu, priority, backbits, mx;
/*TODO*///	
/*TODO*///						/* also use the coch value to look up color info in IC13/PR1114 and IC21/PR1117 (p. 144) */
/*TODO*///						bacol = road_palette_base[coch & 15];
/*TODO*///	
/*TODO*///						/* at this point, do the character lookup */
/*TODO*///						backbits = backbits_buffer & 3;
/*TODO*///						backbits_buffer >>= 2;
/*TODO*///						backbits = back_palette[backbits | (back_data & 0xfc)];
/*TODO*///	
/*TODO*///						/* look up the sprite priority in IC11/PR1122 */
/*TODO*///						priority = sprite_priority_base[sprite >> 25];
/*TODO*///	
/*TODO*///						/* use that to look up the overall priority in IC12/PR1123 */
/*TODO*///						mx = overall_priority_base[(priority & 7) | ((sprite >> 21) & 8) | ((back_data >> 3) & 0x10) | ((backbits << 2) & 0x20) | (babit << 6)];
/*TODO*///	
/*TODO*///						/* the input colors consist of a mix of sprite, road and 1's & 0's */
/*TODO*///						red = 0x040000 | ((bacol & 0x001f) << 13) | ((backbits & 1) << 12) | ((sprite <<  4) & 0x0ff0);
/*TODO*///						grn = 0x080000 | ((bacol & 0x03e0) <<  9) | ((backbits & 2) << 12) | ((sprite >>  3) & 0x1fe0);
/*TODO*///						blu = 0x100000 | ((bacol & 0x7c00) <<  5) | ((backbits & 4) << 12) | ((sprite >> 10) & 0x3fc0);
/*TODO*///	
/*TODO*///						/* we then go through a muxer; normally these values are inverted, but */
/*TODO*///						/* we've already taken care of that when we generated the palette */
/*TODO*///						red = (red >> mx) & 0x10;
/*TODO*///						grn = (grn >> mx) & 0x20;
/*TODO*///						blu = (blu >> mx) & 0x40;
/*TODO*///						scanline[x + i] = mx | red | grn | blu;
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///	
/*TODO*///			/* render the scanline */
/*TODO*///			if (bitmap)
/*TODO*///				draw_scanline8(bitmap, 8, y + yoffs, VIEW_WIDTH - 8, &scanline[8], colortable, -1);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/***************************************************************************
/*TODO*///	
/*TODO*///	  Master refresh routine
/*TODO*///	
/*TODO*///	***************************************************************************/
/*TODO*///	
/*TODO*///	void turbo_vh_eof(void)
/*TODO*///	{
/*TODO*///		/* only do collision checking if we didn't draw */
/*TODO*///		if (drew_frame == 0)
/*TODO*///		{
/*TODO*///			update_sprite_info();
/*TODO*///			draw_everything(NULL, 0);
/*TODO*///		}
/*TODO*///		drew_frame = 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	public static VhUpdatePtr turbo_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
/*TODO*///	{
/*TODO*///		/* update the sprite data */
/*TODO*///		update_sprite_info();
/*TODO*///	
/*TODO*///		/* perform the actual drawing */
/*TODO*///		draw_everything(bitmap, 64);
/*TODO*///	
/*TODO*///		/* draw the LEDs for the scores */
/*TODO*///		draw_scores(bitmap);
/*TODO*///	
/*TODO*///		/* indicate that we drew this frame, so that the eof callback doesn't bother doing anything */
/*TODO*///		drew_frame = 1;
/*TODO*///	} };
}
