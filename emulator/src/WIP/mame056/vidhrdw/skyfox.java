/***************************************************************************

						-= Sky Fox / Exerizer =-

				driver by	Luca Elia (l.elia@tin.it)


							[ 1 Background ]

	The stars in the background are not tile based (I think!) and
	their rendering	is entirely guesswork for now..

	I draw a star for each horizontal line using 2 bytes in the
	background rom:

	- the first byte seems a color / shape info
	- the second byte seems a position info

	The rom holds 4 chunks of $2000 bytes. Most of the data does not
	change between chunks, while the remaining part (which is rendered
	to what seems a "milky way") pulsates in color and/or shape
	to simulate the shimmering of stars (?!) if we draw one chunk only
	and cycle through the four. Indeed, there's a register cycling
	through 4 values.

	Since the result kind of matches a screenshot we have, I feel the
	drawn result is not that far from reality. On the other hand we
	have a random arrangement of stars, so it's hard to tell for sure..

							[ 256 Sprites ]

	Sprites are 8 planes deep and can be 8x8, 16x16 or 32x32 pixels
	in size. They are stored as 32x32x8 tiles in the ROMs.


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
import static mame056.sndintrf.*;

public class skyfox
{
	
	/* Variables only used here: */
	
	static int[] vreg=new int[8];
	
	
	/* Variables that driver has access to: */
	
	public static int skyfox_bg_pos, skyfox_bg_ctrl;
	
	
	
	/***************************************************************************
	
								Memory Handlers
	
	***************************************************************************/
	
	public static ReadHandlerPtr skyfox_vregs_r  = new ReadHandlerPtr() { public int handler(int offset)	// for debug
	{
		return vreg[offset];
	} };
	
	public static WriteHandlerPtr skyfox_vregs_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		vreg[offset] = data;
	
		switch (offset)
		{
			case 0:	skyfox_bg_ctrl = data;	break;
			case 1:	soundlatch_w.handler(0,data);	break;
			case 2:	break;
			case 3:	break;
			case 4:	break;
			case 5:	break;
			case 6:	break;
			case 7:	break;
		}
	} };
	
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  There are three 256x4 palette PROMs (one per gun).
	  I don't know the exact values of the resistors between the RAM and the
	  RGB output. I assumed these values (the same as Commando)
	
	  bit 3 -- 220 ohm resistor  -- RED/GREEN/BLUE
	        -- 470 ohm resistor  -- RED/GREEN/BLUE
	        -- 1  kohm resistor  -- RED/GREEN/BLUE
	  bit 0 -- 2.2kohm resistor  -- RED/GREEN/BLUE
	
	***************************************************************************/
	
	public static VhConvertColorPromPtr skyfox_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
                int _palette = 0;
	
		for (i = 0;i < 256;i++)
		{
			int bit0,bit1,bit2,bit3,r,g,b;
	
			/* red component */
			bit0 = (color_prom.read(i)>> 0) & 0x01;
			bit1 = (color_prom.read(i)>> 1) & 0x01;
			bit2 = (color_prom.read(i)>> 2) & 0x01;
			bit3 = (color_prom.read(i)>> 3) & 0x01;
			r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
                        palette[_palette++]=(char) r;
			/* green component */
			bit0 = (color_prom.read(i + 256)>> 0) & 0x01;
			bit1 = (color_prom.read(i + 256)>> 1) & 0x01;
			bit2 = (color_prom.read(i + 256)>> 2) & 0x01;
			bit3 = (color_prom.read(i + 256)>> 3) & 0x01;
			g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
                        palette[_palette++]=(char) g;
			/* blue component */
			bit0 = (color_prom.read(i + 2*256)>> 0) & 0x01;
			bit1 = (color_prom.read(i + 2*256)>> 1) & 0x01;
			bit2 = (color_prom.read(i + 2*256)>> 2) & 0x01;
			bit3 = (color_prom.read(i + 2*256)>> 3) & 0x01;
			b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
                        palette[_palette++]=(char) b;
	
			palette_set_color(i,r,g,b);
		}
	
		/* Grey scale for the background??? */
		for (i = 0; i < 256; i++)
		{
			palette_set_color(i+256,i,i,i);
                        palette[_palette++]=(char) i;
                        palette[_palette++]=(char) i;
                        palette[_palette++]=(char) i;
		}
            }
        };
	
	
	/***************************************************************************
	
									Sprites Drawing
	
	Offset:			Value:
	
	03				Code: selects one of the 32x32 tiles in the ROMs.
					(Tiles $80-ff are bankswitched to cover $180 tiles)
	
	02				Code + Attr
	
						7654 ----	Code (low 4 bits)
									8x8   sprites use bits 7654	(since there are 16 8x8  tiles in the 32x32 one)
									16x16 sprites use bits --54 (since there are 4 16x16 tiles in the 32x32 one)
									32x32 sprites use no bits	(since the 32x32 tile is already selected)
	
						7--- 3---	Size
									1--- 1--- : 32x32 sprites
									0--- 1--- : 16x16 sprites
									8x8 sprites otherwise
	
						---- -2--	Flip Y
						---- --1-	Flip X
						---- ---0	X Low Bit
	
	00				Y
	
	01				X (High 8 Bits)
	
	***************************************************************************/
        
        public static void DRAW_SPRITE(mame_bitmap bitmap, int flipx, int flipy, int x, int y, int DX, int DY, int CODE){
			drawgfx(bitmap,Machine.gfx[0], 
					(CODE), 
					0, 
					flipx,flipy, 
					x + (DX),y + (DY), 
					Machine.visible_area,TRANSPARENCY_PEN, 0xff); 
        }
	
	public static void skyfox_draw_sprites(mame_bitmap bitmap)
	{
		int offs;
	
		int width	=	Machine.drv.screen_width;
		int height	=	Machine.drv.screen_height;
	
		/* The 32x32 tiles in the 80-ff range are bankswitched */
		int shift	=	(skyfox_bg_ctrl & 0x80)!=0 ? (4-1) : 4;
	
		for (offs = 0; offs < spriteram_size[0]; offs += 4)
		{
			int xstart, ystart, xend, yend;
			int xinc, yinc, dx, dy;
			int low_code, high_code, n;
	
			int y		=		spriteram.read(offs+0);
			int x		=		spriteram.read(offs+1);
			int code	=		spriteram.read(offs+2)+ spriteram.read(offs+3)* 256;
			int flipx	=		code & 0x2;
			int flipy	=		code & 0x4;
	
			x = x * 2 + (code & 1);	// add the least significant bit
	
			high_code = ((code >> 4) & 0x7f0) +
						((code & 0x8000) >> shift);
	
			switch( code & 0x88 )
			{
				case 0x88:	n = 4;	low_code = 0;										break;
				case 0x08:	n = 2;	low_code = ((code&0x20)!=0?8:0) + ((code&0x10)!=0?2:0);	break;
				default:	n = 1;	low_code = (code >> 4) & 0xf;
			}
	
	
	
			if ((skyfox_bg_ctrl & 1)!=0)	// flipscreen
			{
				x = width  - x - (n-1)*8;
				y = height - y - (n-1)*8;
				flipx = flipx!=0?0:1;
				flipy = flipy!=0?0:1;
			}
	
			if (flipx != 0)	{ xstart = n-1;  xend = -1;  xinc = -1; }
			else		{ xstart = 0;    xend = n;   xinc = +1; }
	
			if (flipy != 0)	{ ystart = n-1;  yend = -1;  yinc = -1; }
			else		{ ystart = 0;    yend = n;   yinc = +1; }
	
	
			code = low_code + high_code;
	
			for (dy = ystart; dy != yend; dy += yinc)
			{
				for (dx = xstart; dx != xend; dx += xinc)
					DRAW_SPRITE(bitmap, flipx, flipy, x, y, dx*8, dy*8, code++);
	
				if (n==2)	code+=2;
			}
		}
	}
	
	
	
	
	
	/***************************************************************************
	
								Background Rendering
	
	***************************************************************************/
	
	public static void skyfox_draw_background(mame_bitmap bitmap)
	{
		UBytePtr RAM	=	new UBytePtr(memory_region(REGION_GFX2));
		int x,y,i;
	
		/* The foreground stars (sprites) move at twice this speed when
		   the bg scroll rate [e.g. (skyfox_bg_reg >> 1) & 7] is 4 */
		int pos = (skyfox_bg_pos >> 4) & (512*2-1);
	
		for (i = 0 ; i < 0x1000; i++)
		{
			int pen,offs,j;
	
			offs	=	(i*2+((skyfox_bg_ctrl>>4)&0x3)*0x2000) % 0x8000;
	
			pen		=	RAM.read(offs);
			x		=	RAM.read(offs+1)*2 + (i&1) + pos + ((i & 8)!=0?512:0);
			y		=	((i/8)/2)*8 + (i%8);
	
			if ((skyfox_bg_ctrl & 1)!=0)	// flipscreen
			{
				x = 512 * 2 - (x%(512*2));
				y = 256     - (y%256);
			}
	
			for (j = 0 ; j <= ((pen&0x80)!=0?0:3); j++)
				plot_pixel.handler(bitmap,
							( (j&1)     + x ) % 512,
							( ((j/2)&1) + y ) % 256,
							Machine.pens[256+(pen&0x7f)] );
		}
	}
	
	
	/***************************************************************************
	
	
									Screen Drawing
	
	
	***************************************************************************/
	
	
	public static VhUpdatePtr skyfox_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		fillbitmap(bitmap,Machine.pens[255],Machine.visible_area);	// the bg is black
		skyfox_draw_background(bitmap);
		skyfox_draw_sprites(bitmap);
	} };
}
