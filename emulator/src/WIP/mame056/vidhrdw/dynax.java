/***************************************************************************

						-= Dynax / Nakanihon Games =-

					driver by	Luca Elia (l.elia@tin.it)


Note:	if MAME_DEBUG is defined, pressing Z with:

				Q		Shows Layer 0
				W		Shows Layer 1
				E		Shows Layer 2

		Keys can be used together!


	There are three scrolling layers. Each layer consists of 2 frame
	buffers. The 2 images are blended together to form the final picture
	sent to the screen.

	The gfx roms do not contain tiles: the CPU controls a video blitter
	that can read data from them (instructions to draw pixel by pixel,
	in a compressed form) and write to the 6 frame buffers.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;
import static mame056.usrintrf.*;
import static mame056.memoryH.*;
import static mame056.memory.*;

import static common.ptr.*;
import common.subArrays.IntArray;
import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.inptport.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.cpuexec.*;
import static mame056.cpuintrfH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static WIP.mame056.drivers.dynax.*;

public class dynax
{
	
	// Log Blitter
	public static int VERBOSE = 0;
	
	
	public static WriteHandlerPtr dynax_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		flip_screen_set( data & 1 );
		if ((data & ~1) != 0)
			logerror("CPU#0 PC %06X: Warning, flip screen <- %02Xn", cpu_get_pc(), data);
	} };
        
        public static int BITSWAP5( int _x_ ){
            return ( (((_x_) & 0x01)!=0 ? 0x10 : 0) | 
                    (((_x_) & 0x02)!=0 ? 0x08 : 0) | 
                    (((_x_) & 0x04)!=0 ? 0x04 : 0) | 
                    (((_x_) & 0x08)!=0 ? 0x02 : 0) | 
                    (((_x_) & 0x10)!=0 ? 0x01 : 0) );
        }
	
	/* 0 B01234 G01234 R01234 */
	public static VhConvertColorPromPtr sprtmtch_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
                
		/* The bits are in reverse order */		
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int x =	(color_prom.read(i)<<8) + color_prom.read(0x200+i);
			int r = BITSWAP5((x >>  0) & 0x1f);
			int g = BITSWAP5((x >>  5) & 0x1f);
			int b = BITSWAP5((x >> 10) & 0x1f);
			palette[i * 3 + 0] =  (char) ((r << 3) | (r >> 2));
			palette[i * 3 + 1] =  (char) ((g << 3) | (g >> 2));
			palette[i * 3 + 2] =  (char) ((b << 3) | (b >> 2));
		}
	} };
	
	/***************************************************************************
	
	
									Video Blitter(s)
	
	
	***************************************************************************/
	
	static int dynax_blit_reg;
	
	static int dynax_blit_x;
	static int dynax_blit_y;
	static int dynax_blit_scroll_x;
	static int dynax_blit_scroll_y;
	
	public static int dynax_blit_address;
	static int dynax_blit_dest;
	
	static int dynax_blit_pen;
	static int dynax_blit_backpen;
	static int dynax_blit_palettes;
	static int dynax_blit_palbank;
	
	static int dynax_blit_enable;
	
	// 3 layers, 2 images per layer (blended on screen)
	static UBytePtr[][] dynax_pixmap = new UBytePtr[3][2];
	
	/* Destination X */
	public static WriteHandlerPtr dynax_blit_x_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
            dynax_blit_x = data;
            if (VERBOSE != 0)
                    logerror("X=%02X ",data);
	
	} };
	
	/* Destination Y */
	public static WriteHandlerPtr dynax_blit_y_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
            dynax_blit_y = data;
            if (VERBOSE != 0)
                    logerror("Y=%02X ",data);
	
	} };
	
	public static WriteHandlerPtr dynax_blit_scroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		// 0x800000 also used!
		if ((dynax_blit_address & 0x400000) != 0)
		{
			dynax_blit_scroll_y = data;
                if (VERBOSE != 0)
                        logerror("SY=%02X ",data);
	
		}
		else
		{
			dynax_blit_scroll_x = data;
                if (VERBOSE != 0)
                        logerror("SX=%02X ",data);
	
		}
	} };
	
	/* Source Address */
	public static WriteHandlerPtr dynax_blit_addr0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dynax_blit_address = (dynax_blit_address & ~0x0000ff) | (data<<0);
                if (VERBOSE != 0)
                        logerror("A0=%02X ",data);
	
	} };
	public static WriteHandlerPtr dynax_blit_addr1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dynax_blit_address = (dynax_blit_address & ~0x00ff00) | (data<<8);
                if (VERBOSE != 0)
                        logerror("A1=%02X ",data);
	
	} };
	public static WriteHandlerPtr dynax_blit_addr2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dynax_blit_address = (dynax_blit_address & ~0xff0000) | (data<<16);
                if (VERBOSE != 0)
                        logerror("A2=%02X ",data);

	} };
	
	/* Destination Layers */
	public static WriteHandlerPtr dynax_blit_dest_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dynax_blit_dest = data;
                if (VERBOSE != 0)
                        logerror("D=%02X ",data);
	
	} };
	
	/* Destination Pen */
	public static WriteHandlerPtr dynax_blit_pen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dynax_blit_pen = data;
                if (VERBOSE != 0)
                        logerror("P=%02X ",data);
	
	} };
	
	/* Background Color */
	public static WriteHandlerPtr dynax_blit_backpen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dynax_blit_backpen = data;
                if (VERBOSE != 0)
                        logerror("B=%02X ",data);
	
	} };
	
	/* Layers 0&1 Palettes (Low Bits) */
	public static WriteHandlerPtr dynax_blit_palette01_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dynax_blit_palettes = (dynax_blit_palettes & ~0xff) | data;
                if (VERBOSE != 0)
                        logerror("P1=%02X ",data);
	
	} };
	
	/* Layer 2 Palette (Low Bits) */
	public static WriteHandlerPtr dynax_blit_palette2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dynax_blit_palettes = (dynax_blit_palettes & ~0xff00) | (data<<8);
                if (VERBOSE != 0)
                        logerror("P2=%02X ",data);
	
	} };
	
	/* Layers Palettes (High Bits) */
	public static WriteHandlerPtr dynax_blit_palbank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dynax_blit_palbank = data;
                if (VERBOSE != 0)
                        logerror("PB=%02X ",data);

	} };
	
	/* Layers Enable */
	public static WriteHandlerPtr dynax_blit_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dynax_blit_enable = data;
                if (VERBOSE != 0)
                        logerror("E=%02X ",data);

	} };
	
	
	/***************************************************************************
	
								Blitter Data Format
	
		The blitter reads its commands from the gfx ROMs. They are
		instructions to draw an image pixel by pixel (in a compressed
		form) in a frame buffer.
	
		Fetch 1 Byte from the ROM:
	
		7654 ----	Pen to draw with
		---- 3210	Command
	
		Other bytes may follow, depending on the command
	
		Commands:
	
		0		Stop.
		1-b		Draw 1-b pixels along X.
		c		Followed by 1 byte (N): draw N pixels along X.
		d		Followed by 2 bytes (X,N): skip X pixels, draw N pixels along X.
		e		? unused
		f		Increment Y
	
	***************************************************************************/
	
	
	/* Plot a pixel (in the pixmaps specified by dynax_blit_dest) */
	public static void sprtmtch_plot_pixel(int x, int y, int pen, int flags)
	{
		/* "Flip Screen" just means complement the coordinates to 256 */
		if (flip_screen() != 0)	{	x = 0x100 - x;	y = 0x100 - y;	}
	
		/* Swap X with Y */
		if ((flags & 0x08) != 0)	{ int t = x; x = y; y = t;	}
	
		/* Ignore the pens specified in ROM, draw everything with the
		   supplied one instead */
		if ((flags & 0x02) != 0)	{ pen = (dynax_blit_pen >> 4) & 0xf;	}
	
		if (	(x >= 0) && (x <= 0xff) &&
				(y >= 0) && (y <= 0xff)	)
		{
			if ((dynax_blit_dest & 0x01) != 0)	dynax_pixmap[0][0].write((y<<8)|x, pen);
			if ((dynax_blit_dest & 0x02) != 0)	dynax_pixmap[0][1].write((y<<8)|x, pen);
			if ((dynax_blit_dest & 0x04) != 0)	dynax_pixmap[1][0].write((y<<8)|x, pen);
			if ((dynax_blit_dest & 0x08) != 0)	dynax_pixmap[1][1].write((y<<8)|x, pen);
			if ((dynax_blit_dest & 0x10) != 0)	dynax_pixmap[2][0].write((y<<8)|x, pen);
			if ((dynax_blit_dest & 0x20) != 0)	dynax_pixmap[2][1].write((y<<8)|x, pen);
		}
	}
	
	
	public static int sprtmtch_drawgfx( int i, int x, int y, int flags )
	{
		int cmd, pen, count;
	
		UBytePtr SRC		=	new UBytePtr(memory_region( REGION_GFX1 ));
		int   size_src	=	memory_region_length( REGION_GFX1 );
	
		int sx;
	
		if (( flags & 1 ) !=0)
		{
			/* Clear the buffer(s) starting from the given scanline and exit */
	
			if ((dynax_blit_dest & 0x01) != 0)	memset(new UBytePtr(dynax_pixmap[0][0], y<<8),0,(0x100-y)*0x100);
			if ((dynax_blit_dest & 0x02) != 0)	memset(new UBytePtr(dynax_pixmap[0][1], y<<8),0,(0x100-y)*0x100);
			if ((dynax_blit_dest & 0x04) != 0)	memset(new UBytePtr(dynax_pixmap[1][0], y<<8),0,(0x100-y)*0x100);
			if ((dynax_blit_dest & 0x08) != 0)	memset(new UBytePtr(dynax_pixmap[1][1], y<<8),0,(0x100-y)*0x100);
			if ((dynax_blit_dest & 0x10) != 0)	memset(new UBytePtr(dynax_pixmap[2][0], y<<8),0,(0x100-y)*0x100);
			if ((dynax_blit_dest & 0x20) != 0)	memset(new UBytePtr(dynax_pixmap[2][1], y<<8),0,(0x100-y)*0x100);
			return i;
		}
	
		sx = x;
	
		while ( y < Machine.drv.screen_height )
		{
			if (i >= size_src)	return i;
			cmd = SRC.read(i++);
			pen = (cmd & 0xf0)>>4;
			cmd = (cmd & 0x0f)>>0;
	
			switch (cmd)
			{
			case 0x0:	// Stop
				return i;
	
			case 0x1:	// Draw N pixels
			case 0x2:
			case 0x3:
			case 0x4:
			case 0x5:
			case 0x6:
			case 0x7:
			case 0x8:
			case 0x9:
			case 0xa:
			case 0xb:
				count = cmd;
				for ( ; count>0; count-- )
					sprtmtch_plot_pixel(x++, y, pen, flags);
				break;
	
			case 0xd:	// Skip X pixels
				if (i >= size_src)	return i;
				x = sx + SRC.read(i++);
			case 0xc:	// Draw N pixels
				if (i >= size_src)	return i;
				count = SRC.read(i++);
	
				for ( ; count>0; count-- )
					sprtmtch_plot_pixel(x++, y, pen, flags);
				break;
	
	//		case 0xe:	// ? unused
	
			case 0xf:	// Increment Y
				y++;
				x = sx;
				break;
	
			default:
				logerror("Blitter unknown command %06X: %02Xn", i-1, cmd);
			}
		}
		return i;
	}
	
	public static WriteHandlerPtr sprtmtch_blit_draw_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int i =
		sprtmtch_drawgfx(
			dynax_blit_address & 0x3fffff,
			dynax_blit_x, dynax_blit_y,
			data
		);
	
                if (VERBOSE != 0)
                        logerror("BLIT=%02Xn",data);
	
		dynax_blit_address	=	(dynax_blit_address & ~0x3fffff) |
								(i                  &  0x3fffff) ;
	
		/* Generate an IRQ */
		dynax_blitter_irq = 1;
		sprtmtch_update_irq();
	} };
	
	
	/***************************************************************************
	
	
									Video Init
	
	
	***************************************************************************/
	
	public static VhStartPtr dynax_vh_start = new VhStartPtr() { public int handler() 
	{
		return 0;
	} };
	
	public static VhStartPtr sprtmtch_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((dynax_pixmap[0][0] = new UBytePtr(256*256)) == null)	return 1;
		if ((dynax_pixmap[0][1] = new UBytePtr(256*256)) == null)	return 1;
		if ((dynax_pixmap[1][0] = new UBytePtr(256*256)) == null)	return 1;
		if ((dynax_pixmap[1][1] = new UBytePtr(256*256)) == null)	return 1;
		if ((dynax_pixmap[2][0] = new UBytePtr(256*256)) == null)	return 1;
		if ((dynax_pixmap[2][1] = new UBytePtr(256*256)) == null)	return 1;
		return 0;
	} };
	
	public static VhStopPtr dynax_vh_stop = new VhStopPtr() { public void handler() 
	{
	} };
	
	public static VhStopPtr sprtmtch_vh_stop = new VhStopPtr() { public void handler() 
	{
		/*TODO*///if (dynax_pixmap[0][0])	free(dynax_pixmap[0][0]);
		/*TODO*///if (dynax_pixmap[0][1])	free(dynax_pixmap[0][1]);
		/*TODO*///if (dynax_pixmap[1][0])	free(dynax_pixmap[1][0]);
		/*TODO*///if (dynax_pixmap[1][1])	free(dynax_pixmap[1][1]);
		/*TODO*///if (dynax_pixmap[2][0])	free(dynax_pixmap[2][0]);
		/*TODO*///if (dynax_pixmap[2][1])	free(dynax_pixmap[2][1]);
	
		dynax_pixmap[0][0] = null;	// multisession safety
		dynax_pixmap[0][1] = null;
		dynax_pixmap[1][0] = null;
		dynax_pixmap[1][1] = null;
		dynax_pixmap[2][0] = null;
		dynax_pixmap[2][1] = null;
	} };
	
	/***************************************************************************
	
	
									Screen Drawing
	
	
	***************************************************************************/
	
	public static VhUpdatePtr dynax_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
	//	fillbitmap(bitmap,Machine.pens[0],&Machine.visible_area);
	} };
	
	
	public static void sprtmtch_copylayer(mame_bitmap bitmap,int i)
	{
		GfxElement gfx=new GfxElement();
		int color;
		int sx,sy;
	
		switch ( i )
		{
			case 0:	color = (dynax_blit_palettes >> 0) & 0xf;	break;
			case 1:	color = (dynax_blit_palettes >> 4) & 0xf;	break;
			case 2:	color = (dynax_blit_palettes >> 8) & 0xf;	break;
			default:	return;
		}
	
		color += (dynax_blit_palbank & 1) * 16;
	
		gfx.width				=	256;
		gfx.height				=	256;
		gfx.total_elements		=	1;
		gfx.color_granularity	=	16;
		gfx.colortable			=	Machine.remapped_colortable;
		gfx.total_colors		=	32;
		gfx.pen_usage			=	null;
		gfx.gfxdata				= dynax_pixmap[i][0];
		gfx.line_modulo			=	gfx.width;
		gfx.char_modulo			=	0;
		gfx.flags				=	0;
	
		for ( sy = dynax_blit_scroll_y+8 - 0x100; sy < 0x100; sy += 0x100 )
		{
			for ( sx = dynax_blit_scroll_x - 0x100;	sx < 0x100; sx += 0x100 )
			{
				gfx.gfxdata = dynax_pixmap[i][0];
				drawgfx(	bitmap, gfx,
							0,
							color,
							0, 0,
							sx, sy,
							Machine.visible_area, TRANSPARENCY_PEN, 0);
	//			if (!keyboard_pressed(KEYCODE_Z))
				{
				gfx.gfxdata = dynax_pixmap[i][1];
				alpha_set_level(0x100/2);	// blend the 2 pictures (50%)
				drawgfx(	bitmap, gfx,
							0,
							color,
							0, 0,
							sx, sy,
							Machine.visible_area, TRANSPARENCY_ALPHA, 0);
				}
			}
		}
	}
	
	public static VhUpdatePtr sprtmtch_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
	//#ifdef MAME_DEBUG
	/*TODO*///#if 0
	/*	A primitive gfx viewer:
	
		T          -  Toggle viewer
		I,O        -  Change palette (-,+)
		J,K & N,M  -  Change "tile"  (-,+, slow & fast)
		R          -  "tile" = 0		*/
	
	/*TODO*///static int toggle;
	/*TODO*///if (keyboard_pressed_memory(KEYCODE_T))	toggle = 1-toggle;
	/*TODO*///if (toggle)	{
	/*TODO*///	data8_t *RAM = memory_region( REGION_GFX1 );
	/*TODO*///	static int i = 0, c = 0;
	/*TODO*///
	/*TODO*///	if (keyboard_pressed_memory(KEYCODE_I))	c = (c-1) & 0x1f;
	/*TODO*///	if (keyboard_pressed_memory(KEYCODE_O))	c = (c+1) & 0x1f;
	/*TODO*///	if (keyboard_pressed_memory(KEYCODE_R))	i = 0;
	/*TODO*///	if (keyboard_pressed(KEYCODE_M) | keyboard_pressed_memory(KEYCODE_K))	{
	/*TODO*///		while( RAM[i] )	i++;		i++;	}
	/*TODO*///	if (keyboard_pressed(KEYCODE_N) | keyboard_pressed_memory(KEYCODE_J))	{
	/*TODO*///		i-=2;	while( RAM[i] )	i--;		i++;	}
	/*TODO*///
	/*TODO*///	dynax_blit_palettes = (c & 0xf) * 0x111;
	/*TODO*///	dynax_blit_palbank  = (c >>  4) & 1;
	/*TODO*///	dynax_blit_dest = 3;
	/*TODO*///
	/*TODO*///	fillbitmap(bitmap,Machine.pens[0],&Machine.visible_area);
	/*TODO*///	memset(dynax_pixmap[0][0],0,sizeof(UINT8)*0x100*0x100);
	/*TODO*///	memset(dynax_pixmap[0][1],0,sizeof(UINT8)*0x100*0x100);
	/*TODO*///	sprtmtch_drawgfx(i, Machine.visible_area.min_x, Machine.visible_area.min_y, 0);
	/*TODO*///	sprtmtch_copylayer(bitmap, 0);
	/*TODO*///	usrintf_showmessage("%06X C%02X",i,c);
	/*TODO*///}
	/*TODO*///else
	/*TODO*///#endif
	/*TODO*///#endif
	{
		int layers_ctrl = ~dynax_blit_enable;
	
	/*TODO*///#ifdef MAME_DEBUG
	/*TODO*///if (keyboard_pressed(KEYCODE_Z))
	/*TODO*///{	int msk = 0;
	/*TODO*///	if (keyboard_pressed(KEYCODE_Q))	msk |= 1;
	/*TODO*///	if (keyboard_pressed(KEYCODE_W))	msk |= 2;
	/*TODO*///	if (keyboard_pressed(KEYCODE_E))	msk |= 4;
	/*TODO*///	if (msk != 0)	layers_ctrl &= msk;		}
	/*TODO*///#endif
	
		fillbitmap(
			bitmap,
			Machine.pens[(dynax_blit_backpen & 0xff) + (dynax_blit_palbank & 1) * 256],
			Machine.visible_area);
	
		if ((layers_ctrl & 1) != 0)	sprtmtch_copylayer( bitmap, 0 );
		if ((layers_ctrl & 2) != 0)	sprtmtch_copylayer( bitmap, 1 );
		if ((layers_ctrl & 4) != 0)	sprtmtch_copylayer( bitmap, 2 );
	}
	
	} };
}
