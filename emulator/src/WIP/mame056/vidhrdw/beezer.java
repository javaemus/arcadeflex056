/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;
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

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpu.m6809.m6809H.M6809_FIRQ_LINE;
import static mame056.cpuintrfH.*;
import static mame056.machine._6522via.*;

public class beezer
{
	
	public static UBytePtr beezer_ram = new UBytePtr();
	static int scanline=0;
	
	public static InterruptPtr beezer_interrupt = new InterruptPtr() { public int handler() 
	{
		scanline = (scanline + 1) % 0x80;
		via_0_ca2_w.handler(0, scanline & 0x10);
		if ((scanline & 0x78) == 0x78)
			cpu_set_irq_line(0, M6809_FIRQ_LINE, ASSERT_LINE);
		else
			cpu_set_irq_line(0, M6809_FIRQ_LINE, CLEAR_LINE);
		return ignore_interrupt.handler();
	} };
	
	public static VhUpdatePtr beezer_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int x, y;
		UBytePtr ram = new UBytePtr(memory_region(REGION_CPU1));
	
		if (full_refresh != 0)
			for (y = Machine.visible_area.min_y; y <= Machine.visible_area.max_y; y+=2)
			{
				for (x = Machine.visible_area.min_x; x <= Machine.visible_area.max_x; x++)
				{
					plot_pixel.handler(bitmap, x, y+1, Machine.pens[ram.read(0x80*y+x) & 0x0f]);
					plot_pixel.handler(bitmap, x, y, Machine.pens[(ram.read(0x80*y+x) >> 4)& 0x0f]);
				}
			}
	} };
	
	public static WriteHandlerPtr beezer_map_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*
		  bit 7 -- 330  ohm resistor  -- BLUE
		        -- 560  ohm resistor  -- BLUE
				-- 330	ohm resistor  -- GREEN
				-- 560	ohm resistor  -- GREEN
				-- 1.2 kohm resistor  -- GREEN
				-- 330	ohm resistor  -- RED
				-- 560	ohm resistor  -- RED
		  bit 0 -- 1.2 kohm resistor  -- RED
		*/
	
		int r, g, b, bit0, bit1, bit2;;
	
		/* red component */
		bit0 = (data >> 0) & 0x01;
		bit1 = (data >> 1) & 0x01;
		bit2 = (data >> 2) & 0x01;
		r = 0x26 * bit0 + 0x50 * bit1 + 0x89 * bit2;
		/* green component */
		bit0 = (data >> 3) & 0x01;
		bit1 = (data >> 4) & 0x01;
		bit2 = (data >> 5) & 0x01;
		g = 0x26 * bit0 + 0x50 * bit1 + 0x89 * bit2;
		/* blue component */
		bit0 = (data >> 6) & 0x01;
		bit1 = (data >> 7) & 0x01;
		b = 0x5f * bit0 + 0xa0 * bit1;
	
		palette_set_color(offset, r, g, b);
	} };
	
	public static WriteHandlerPtr beezer_ram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int x, y;
		x = offset % 0x100;
		y = (offset / 0x100) * 2;
	
		if( (y >= Machine.visible_area.min_y) && (y <= Machine.visible_area.max_y) )
		{
			plot_pixel.handler(Machine.scrbitmap, x, y+1, Machine.pens[data & 0x0f]);
			plot_pixel.handler(Machine.scrbitmap, x, y, Machine.pens[(data >> 4)& 0x0f]);
		}
	
		beezer_ram.write(offset, data);
	} };
	
	public static ReadHandlerPtr beezer_line_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return (scanline & 0xfe) << 1;
	} };
	
}
