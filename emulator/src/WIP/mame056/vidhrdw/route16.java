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

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.sound.sn76477.*;

public class route16
{
	
	public static UBytePtr route16_sharedram = new UBytePtr();
	public static UBytePtr route16_videoram1 = new UBytePtr();
	public static UBytePtr route16_videoram2 = new UBytePtr();
	public static int[] route16_videoram_size = new int[2];
	
	static mame_bitmap tmpbitmap1;
	static mame_bitmap tmpbitmap2;
	
	static int video_flip;
	static int video_color_select_1;
	static int video_color_select_2;
	static int video_disable_1 = 0;
	static int video_disable_2 = 0;
	static int video_remap_1;
	static int video_remap_2;
        static UBytePtr route16_color_prom = new UBytePtr();
	static int route16_hardware;
	
	
	public static VhConvertColorPromPtr route16_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		route16_color_prom = color_prom;	/* we'll need this later */
	} };
	
	
	/***************************************************************************
	
	  Set hardware dependent flag.
	
	***************************************************************************/
	public static InitDriverPtr init_route16b = new InitDriverPtr() { public void handler() 
	{
	    route16_hardware = 1;
	} };
	
	public static InitDriverPtr init_route16 = new InitDriverPtr() { public void handler() 
	{
		UBytePtr rom = new UBytePtr(memory_region(REGION_CPU1));
	
	
		/* patch the protection */
		rom.write(0x00e9, 0x3a);
	
		rom.write(0x0754, 0xc3);
		rom.write(0x0755, 0x63);
		rom.write(0x0756, 0x07);
	
		init_route16b.handler();
	} };
	
	public static InitDriverPtr init_stratvox = new InitDriverPtr() { public void handler() 
	{
	    route16_hardware = 0;
	} };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	public static VhStartPtr route16_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((tmpbitmap1 = bitmap_alloc(Machine.drv.screen_width,Machine.drv.screen_height)) == null)
		{
			return 1;
		}
	
		if ((tmpbitmap2 = bitmap_alloc(Machine.drv.screen_width,Machine.drv.screen_height)) == null)
		{
	
			bitmap_free(tmpbitmap1);
			tmpbitmap1 = null;
			return 1;
		}
	
		video_flip = 0;
		video_color_select_1 = 0;
		video_color_select_2 = 0;
		video_disable_1 = 0;
		video_disable_2 = 0;
		video_remap_1 = 1;
		video_remap_2 = 1;
	
		return 0;
	} };
	
	/***************************************************************************
	
	  Stop the video hardware emulation.
	
	***************************************************************************/
	public static VhStopPtr route16_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free(tmpbitmap1);
		bitmap_free(tmpbitmap2);
	} };
	
        static int last_write = 0;
	
	/***************************************************************************
	  route16_out0_w
	***************************************************************************/
	public static WriteHandlerPtr route16_out0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
	
		if (data == last_write) return;
	
		video_disable_1 = (((data & 0x02) << 6)!=0) && (route16_hardware!=0)?1:0;
		video_color_select_1 = ((data & 0x1f) << 2);
	
		/* Bit 5 is the coin counter. */
		coin_counter_w.handler(0, data & 0x20);
	
		video_remap_1 = 1;
		last_write = data;
	} };
	
	/***************************************************************************
	  route16_out1_w
	***************************************************************************/
	public static WriteHandlerPtr route16_out1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
		if (data == last_write) return;
	
		video_disable_2 = ((data & 0x02) << 6 )!=0 && route16_hardware!=0 ? 1: 0;
		video_color_select_2 = ((data & 0x1f) << 2);
	
		if (video_flip != ((data & 0x20) >> 5))
		{
			video_flip = (data & 0x20) >> 5;
		}
	
		video_remap_2 = 1;
		last_write = data;
	} };
	
	/***************************************************************************
	
	  Handle Stratovox's extra sound effects.
	
	***************************************************************************/
	public static WriteHandlerPtr stratvox_sn76477_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* get out for Route 16 */
		if (route16_hardware != 0) return;
	
	    /***************************************************************
	     * AY8910 output bits are connected to...
	     * 7    - direct: 5V * 30k/(100+30k) = 1.15V - via DAC??
	     * 6    - SN76477 mixer a
	     * 5    - SN76477 mixer b
	     * 4    - SN76477 mixer c
	     * 3    - SN76477 envelope 1
		 * 2	- SN76477 envelope 2
	     * 1    - SN76477 vco
	     * 0    - SN76477 enable
	     ***************************************************************/
	    SN76477_mixer_w(0,(data >> 4) & 7);
            SN76477_envelope_w(0,(data >> 2) & 3);
	    SN76477_vco_w(0,(data >> 1) & 1);
	    SN76477_enable_w(0,data & 1);
	} };
	
	/***************************************************************************
	  route16_sharedram_r
	***************************************************************************/
	public static ReadHandlerPtr route16_sharedram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return route16_sharedram.read(offset);
	} };
	
	/***************************************************************************
	  route16_sharedram_w
	***************************************************************************/
	public static WriteHandlerPtr route16_sharedram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		route16_sharedram.write(offset, data);
	
		// 4313-4319 are used in Route 16 as triggers to wake the other CPU
		if (offset >= 0x0313 && offset <= 0x0319 && data == 0xff && route16_hardware!=0)
		{
			// Let the other CPU run
			cpu_yield();
		}
	} };
	
	/***************************************************************************
	  route16_videoram1_r
	***************************************************************************/
	public static ReadHandlerPtr route16_videoram1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return route16_videoram1.read(offset);
	} };
	
	/***************************************************************************
	  route16_videoram2_r
	***************************************************************************/
	public static ReadHandlerPtr route16_videoram2_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return route16_videoram1.read(offset);
	} };
	
	/***************************************************************************
	  route16_videoram1_w
	***************************************************************************/
	public static WriteHandlerPtr route16_videoram1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		route16_videoram1.write(offset, data);
	
		common_videoram_w(offset, data, 0, tmpbitmap1);
	} };
	
	/***************************************************************************
	  route16_videoram2_w
	***************************************************************************/
	public static WriteHandlerPtr route16_videoram2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		route16_videoram2.write(offset, data);
	
		common_videoram_w(offset, data, 4, tmpbitmap2);
	} };
	
	/***************************************************************************
	  common_videoram_w
	***************************************************************************/
	static void common_videoram_w(int offset,int data,
	                              int coloroffset, mame_bitmap bitmap)
	{
		int x, y, color1, color2, color3, color4;
	
		x = ((offset & 0x3f) << 2);
		y = (offset & 0xffc0) >> 6;
	
		if (video_flip != 0)
		{
			x = 255 - x;
			y = 255 - y;
		}
	
		color4 = ((data & 0x80) >> 6) | ((data & 0x08) >> 3);
		color3 = ((data & 0x40) >> 5) | ((data & 0x04) >> 2);
		color2 = ((data & 0x20) >> 4) | ((data & 0x02) >> 1);
		color1 = ((data & 0x10) >> 3) | ((data & 0x01)     );
	
		if (video_flip != 0)
		{
			plot_pixel.handler(bitmap, x  , y, Machine.pens[color1 | coloroffset]);
			plot_pixel.handler(bitmap, x-1, y, Machine.pens[color2 | coloroffset]);
			plot_pixel.handler(bitmap, x-2, y, Machine.pens[color3 | coloroffset]);
			plot_pixel.handler(bitmap, x-3, y, Machine.pens[color4 | coloroffset]);
		}
		else
		{
			plot_pixel.handler(bitmap, x  , y, Machine.pens[color1 | coloroffset]);
			plot_pixel.handler(bitmap, x+1, y, Machine.pens[color2 | coloroffset]);
			plot_pixel.handler(bitmap, x+2, y, Machine.pens[color3 | coloroffset]);
			plot_pixel.handler(bitmap, x+3, y, Machine.pens[color4 | coloroffset]);
		}
	}
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr route16_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
	    if (video_remap_1 != 0)
		{
			modify_pen(0, video_color_select_1 + 0);
			modify_pen(1, video_color_select_1 + 1);
			modify_pen(2, video_color_select_1 + 2);
			modify_pen(3, video_color_select_1 + 3);
		}
	
		if (video_remap_2 != 0)
		{
			modify_pen(4, video_color_select_2 + 0);
			modify_pen(5, video_color_select_2 + 1);
			modify_pen(6, video_color_select_2 + 2);
			modify_pen(7, video_color_select_2 + 3);
		}
	
	
		if (full_refresh!=0 || video_remap_1!=0 || video_remap_2!=0)
		{
			int offs;
	
			// redraw bitmaps
			for (offs = 0; offs < route16_videoram_size[0]; offs++)
			{
				route16_videoram1_w.handler(offs, route16_videoram1.read(offs));
				route16_videoram2_w.handler(offs, route16_videoram2.read(offs));
			}
		}
	
		video_remap_1 = 0;
		video_remap_2 = 0;
	
	
		if (video_disable_2 == 0)
		{
			copybitmap(bitmap,tmpbitmap2,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	
		if (video_disable_1 == 0)
		{
			if (video_disable_2 != 0)
				copybitmap(bitmap,tmpbitmap1,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
			else
				copybitmap(bitmap,tmpbitmap1,0,0,0,0,Machine.visible_area,TRANSPARENCY_COLOR,0);
		}
	} };
	
	/***************************************************************************
	  mofify_pen
	 ***************************************************************************/
	static void modify_pen(int pen, int colorindex)
	{
		int r,g,b,color;
	
		color = route16_color_prom.read(colorindex);
	
		r = ((color & 1)!=0 ? 0xff : 0x00);
		g = ((color & 2)!=0 ? 0xff : 0x00);
		b = ((color & 4)!=0 ? 0xff : 0x00);
	
		palette_set_color(pen,r,g,b);
	}
}
