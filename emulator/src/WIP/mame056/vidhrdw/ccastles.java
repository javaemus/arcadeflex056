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
import static common.libc.cstring.*;
import static mame056.cpuintrfH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.vidhrdw.generic.*;
import static mame056.palette.*;
// refactor
import static arcadeflex036.osdepend.logerror;

public class ccastles
{
	
	
	static mame_bitmap sprite_bm;
	static mame_bitmap maskbitmap;
	
	public static UBytePtr ccastles_screen_addr = new UBytePtr();
	public static UBytePtr ccastles_screen_inc = new UBytePtr();
	public static UBytePtr ccastles_screen_inc_enable = new UBytePtr();
	public static UBytePtr ccastles_sprite_bank = new UBytePtr();
	public static UBytePtr ccastles_scrollx = new UBytePtr();
	public static UBytePtr ccastles_scrolly = new UBytePtr();
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Crystal Castles doesn't have a color PROM. It uses RAM to dynamically
	  create the palette. The resolution is 9 bit (3 bits per gun). The palette
	  contains 32 entries, but it is accessed through a memory windows 64 bytes
	  long: writing to the first 32 bytes sets the msb of the red component to 0,
	  while writing to the last 32 bytes sets it to 1.
	  The first 16 entries are used for sprites; the last 16 for the background
	  bitmap.
	
	  I don't know the exact values of the resistors between the RAM and the
	  RGB output, I assumed the usual ones.
	  bit 8 -- inverter -- 220 ohm resistor  -- RED
	  bit 7 -- inverter -- 470 ohm resistor  -- RED
	        -- inverter -- 1  kohm resistor  -- RED
	        -- inverter -- 220 ohm resistor  -- BLUE
	        -- inverter -- 470 ohm resistor  -- BLUE
	        -- inverter -- 1  kohm resistor  -- BLUE
	        -- inverter -- 220 ohm resistor  -- GREEN
	        -- inverter -- 470 ohm resistor  -- GREEN
	  bit 0 -- inverter -- 1  kohm resistor  -- GREEN
	
	***************************************************************************/
	public static WriteHandlerPtr ccastles_paletteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int r,g,b;
		int bit0,bit1,bit2;
	
	
		r = (data & 0xC0) >> 6;
		b = (data & 0x38) >> 3;
		g = (data & 0x07);
		/* a write to offset 32-63 means to set the msb of the red component */
		if ((offset & 0x20)!=0) r += 4;
	
		/* bits are inverted */
		r = 7-r;
		g = 7-g;
		b = 7-b;
	
		bit0 = (r >> 0) & 0x01;
		bit1 = (r >> 1) & 0x01;
		bit2 = (r >> 2) & 0x01;
		r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		bit0 = (g >> 0) & 0x01;
		bit1 = (g >> 1) & 0x01;
		bit2 = (g >> 2) & 0x01;
		g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
		bit0 = (b >> 0) & 0x01;
		bit1 = (b >> 1) & 0x01;
		bit2 = (b >> 2) & 0x01;
		b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
	
		palette_set_color(offset & 0x1f,r,g,b);
	} };
	
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	public static VhStartPtr ccastles_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((tmpbitmap = bitmap_alloc(Machine.drv.screen_width,Machine.drv.screen_height)) == null)
			return 1;
	
		if ((maskbitmap = bitmap_alloc(Machine.drv.screen_width,Machine.drv.screen_height)) == null)
		{
			bitmap_free(tmpbitmap);
			return 1;
		}
	
		if ((sprite_bm = bitmap_alloc(8,16)) == null)
		{
			bitmap_free(maskbitmap);
			bitmap_free(tmpbitmap);
			return 1;
		}
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Stop the video hardware emulation.
	
	***************************************************************************/
	public static VhStopPtr ccastles_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free(sprite_bm);
		bitmap_free(maskbitmap);
		bitmap_free(tmpbitmap);
	} };
	
	
	
	public static ReadHandlerPtr ccastles_bitmode_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int addr;
	
		addr = (ccastles_screen_addr.read(1)<<7) | (ccastles_screen_addr.read(0)>>1);
	
		/* is the address in videoram? */
		if ((addr >= 0x0c00) && (addr < 0x8000))
		{
			/* auto increment in the x-direction if it's enabled */
			if (ccastles_screen_inc_enable.read(0)==0)
			{
				if (ccastles_screen_inc.read(0)==0)
					ccastles_screen_addr.write(0, ccastles_screen_addr.read(0)+1);
				else
					ccastles_screen_addr.write(0, ccastles_screen_addr.read(0)-1);
			}
	
			/* auto increment in the y-direction if it's enabled */
			if (ccastles_screen_inc_enable.read(1) == 0)
			{
				if (ccastles_screen_inc.read(1) != 0)
					ccastles_screen_addr.write(1, ccastles_screen_addr.read(1)+1);
				else
					ccastles_screen_addr.write(1, ccastles_screen_addr.read(1)-1);
			}
	
			addr -= 0xc00;
			if ((ccastles_screen_addr.read(0) & 0x01) != 0)
				return ((videoram.read(addr)& 0x0f) << 4);
			else
				return (videoram.read(addr)& 0xf0);
		}
	
		return 0;
	} };
	
	public static WriteHandlerPtr ccastles_bitmode_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int addr;
	
	
		addr = (ccastles_screen_addr.read(1) << 7) | (ccastles_screen_addr.read(0) >> 1);
	
		/* is the address in videoram? */
		if ((addr >= 0x0c00) && (addr < 0x8000))
		{
			int x,y,j;
			int mode;
	
			addr -= 0xc00;
	
			if ((ccastles_screen_addr.read(0) & 0x01) != 0)
			{
				mode = (data >> 4) & 0x0f;
				videoram.write(addr,(videoram.read(addr) & 0xf0) | mode);
			}
			else
			{
				mode = (data & 0xf0);
				videoram.write(addr,(videoram.read(addr) & 0x0f) | mode);
			}
	
			j = 2*addr;
			x = j%256;
			y = j/256;
			if (flip_screen() == 0)
			{
				plot_pixel.handler(tmpbitmap, x  , y, Machine.pens[16 + ((videoram.read(addr)& 0xf0) >> 4)]);
				plot_pixel.handler(tmpbitmap, x+1, y, Machine.pens[16 +  (videoram.read(addr)& 0x0f)      ]);
	
				/* if bit 3 of the pixel is set, background has priority over sprites when */
				/* the sprite has the priority bit set. We use a second bitmap to remember */
				/* which pixels have priority. */
				plot_pixel.handler(maskbitmap, x  , y, videoram.read(addr)& 0x80);
				plot_pixel.handler(maskbitmap, x+1, y, videoram.read(addr)& 0x08);
			}
			else
			{
				y = 231-y;
				x = 254-x;
				if (y >= 0)
				{
					plot_pixel.handler(tmpbitmap, x+1, y, Machine.pens[16 + ((videoram.read(addr)& 0xf0) >> 4)]);
					plot_pixel.handler(tmpbitmap, x  , y, Machine.pens[16 +  (videoram.read(addr)& 0x0f)      ]);
	
					/* if bit 3 of the pixel is set, background has priority over sprites when */
					/* the sprite has the priority bit set. We use a second bitmap to remember */
					/* which pixels have priority. */
					plot_pixel.handler(maskbitmap, x+1, y, videoram.read(addr)& 0x80);
					plot_pixel.handler(maskbitmap, x  , y, videoram.read(addr)& 0x08);
				}
			}
		}
	
		/* auto increment in the x-direction if it's enabled */
		if (ccastles_screen_inc_enable.read(0) == 0)
		{
			if (ccastles_screen_inc.read(0) == 0)
				ccastles_screen_addr.write(0, ccastles_screen_addr.read(0)+1);
			else
				ccastles_screen_addr.write(0, ccastles_screen_addr.read(0)-1);
		}
	
		/* auto increment in the y-direction if it's enabled */
		if (ccastles_screen_inc_enable.read(1) == 0)
		{
			if (ccastles_screen_inc.read(1) == 0)
				ccastles_screen_addr.write(1, ccastles_screen_addr.read(1)+1);
			else
				ccastles_screen_addr.write(1, ccastles_screen_addr.read(1)-1);
		}
	
	} };
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	static void redraw_bitmap()
	{
		int x, y;
		int screen_addr0_save, screen_addr1_save, screen_inc_enable0_save, screen_inc_enable1_save;
	
	
		/* save out registers */
		screen_addr0_save = ccastles_screen_addr.read(0);
		screen_addr1_save = ccastles_screen_addr.read(1);
	
		screen_inc_enable0_save = ccastles_screen_inc_enable.read(0);
		screen_inc_enable1_save = ccastles_screen_inc_enable.read(1);
	
		ccastles_screen_inc_enable.write(0, 1);
                ccastles_screen_inc_enable.write(1, 1);
	
	
		/* redraw bitmap */
		for (y = 0; y < 256; y++)
		{
			ccastles_screen_addr.write(1, y);
	
			for (x = 0; x < 256; x++)
			{
				ccastles_screen_addr.write(0, x);
	
				ccastles_bitmode_w.handler(0, ccastles_bitmode_r.handler(0));
			}
		}
	
	
		/* restore registers */
		ccastles_screen_addr.write(0, screen_addr0_save);
		ccastles_screen_addr.write(1, screen_addr1_save);
	
		ccastles_screen_inc_enable.write(0, screen_inc_enable0_save);
		ccastles_screen_inc_enable.write(1, screen_inc_enable1_save);
	}
	
	
	public static VhUpdatePtr ccastles_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
		UBytePtr spriteaddr=new UBytePtr();
		int scrollx,scrolly;
	
	
		if (full_refresh != 0)
		{
			redraw_bitmap();
		}
	
		scrollx = 255 - ccastles_scrollx.read();
		scrolly = 255 - ccastles_scrolly.read();
	
		if (flip_screen() != 0)
		{
			scrollx = 254 - scrollx;
			scrolly = 231 - scrolly;
		}
	
		copyscrollbitmap(bitmap,tmpbitmap,1,new int[]{scrollx},1,new int[]{scrolly},
					     Machine.visible_area,
			   			 TRANSPARENCY_NONE,0);
	
	
		if (ccastles_sprite_bank.read() != 0)
			spriteaddr = spriteram;
		else
			spriteaddr = spriteram_2;
	
	
		/* Draw the sprites */
		for (offs = 0; offs < spriteram_size[0]; offs += 4)
		{
			int i,j;
			int x,y;
	
			/* Get the X and Y coordinates from the MOB RAM */
			x = spriteaddr.read(offs+3);
			y = 216 - spriteaddr.read(offs+1);
	
			if ((spriteaddr.read(offs+2) & 0x80) != 0)	/* background can have priority over the sprite */
			{
				drawgfx(sprite_bm,Machine.gfx[0],
						spriteaddr.read(offs),
						0,
						flip_screen(),flip_screen(),
						0,0,
						null,TRANSPARENCY_NONE,0);
	
				for (j = 0;j < 16;j++)
				{
					if (y + j >= 0)	/* avoid accesses out of the bitmap boundaries */
					{
						for (i = 0;i < 8;i++)
						{
							int pixa,pixb;
	
							pixa = read_pixel.handler(sprite_bm, i, j);
							pixb = read_pixel.handler(maskbitmap, (x-scrollx+i+256)%256, (y-scrolly+j+232)%232);
	
							/* if background has priority over sprite, make the */
							/* temporary bitmap transparent */
							if (pixb != 0 && (pixa != Machine.pens[0]))
								plot_pixel.handler(sprite_bm, i, j, Machine.pens[7]);
						}
					}
				}
	
				copybitmap(bitmap,sprite_bm,0,0,x,y,Machine.visible_area,TRANSPARENCY_PEN,Machine.pens[7]);
			}
			else
			{
				drawgfx(bitmap,Machine.gfx[0],
						spriteaddr.read(offs),
						0,
						flip_screen(),flip_screen(),
						x,y,
						Machine.visible_area,TRANSPARENCY_PEN,7);
			}
		}
	} };
}
