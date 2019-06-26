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

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.palette.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;

import static WIP.mame056.vidhrdw.ppu2c03b.*;
import static WIP.mame056.vidhrdw.ppu2c03bH.*;
import static WIP.mame056.machine.playch10.*;

public class playch10
{
	
	/* from machine */
	
	public static VhConvertColorPromPtr playch10_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
                int _palette = 0;
	
		for ( i = 0;i < 256; i++ )
		{
			int bit0,bit1,bit2,bit3;
	
			/* red component */
			bit0 = ~(color_prom.read(0) >> 0) & 0x01;
			bit1 = ~(color_prom.read(0) >> 1) & 0x01;
			bit2 = ~(color_prom.read(0) >> 2) & 0x01;
			bit3 = ~(color_prom.read(0) >> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
			/* green component */
			bit0 = ~(color_prom.read(256) >> 0) & 0x01;
			bit1 = ~(color_prom.read(256) >> 1) & 0x01;
			bit2 = ~(color_prom.read(256) >> 2) & 0x01;
			bit3 = ~(color_prom.read(256) >> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
			/* blue component */
			bit0 = ~(color_prom.read(2*256) >> 0) & 0x01;
			bit1 = ~(color_prom.read(2*256) >> 1) & 0x01;
			bit2 = ~(color_prom.read(2*256) >> 2) & 0x01;
			bit3 = ~(color_prom.read(2*256) >> 3) & 0x01;
			palette[_palette++] = (char) (0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3);
	
			color_prom.inc();
		}
	
		ppu2c03b_init_palette( _palette,palette );
            }
        };
	
	static ppu2c03b_irq_cb ppu_irq = new ppu2c03b_irq_cb() {
            public void handler(int num) {
                cpu_set_nmi_line( 1, PULSE_LINE );
		pc10_int_detect = 1;
            }
        };
	
	/* our ppu interface											*/
	/* things like mirroring and wether to use vrom or vram			*/
	/* can be set by calling 'ppu2c03b_override_hardware_options'	*/
	static ppu2c03b_interface ppu_interface = new ppu2c03b_interface
	(
		1,						/* num */
		new int[]{ REGION_GFX2 },		/* vrom gfx region */
		new int[]{ 1 },					/* gfxlayout num */
		new int[]{ 256 },				/* color base */
		new int[]{ PPU_MIRROR_NONE },	/* mirroring */
		new ppu2c03b_irq_cb[]{ ppu_irq }				/* irq */
	);
	
	public static VhStartPtr playch10_vh_start = new VhStartPtr() { public int handler() 
	{
		if ( ppu2c03b_init( ppu_interface ) != 0 )
			return 1;
	
		/* the bios uses the generic stuff */
		return generic_vh_start.handler();
	} };
	
	public static VhStopPtr playch10_vh_stop = new VhStopPtr() { public void handler() 
	{
		ppu2c03b_dispose();
		generic_vh_stop.handler();
	} };
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr playch10_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int offs;
	
		rectangle top_monitor = new rectangle(Machine.visible_area);
		rectangle bottom_monitor = new rectangle(Machine.visible_area);
	
		top_monitor.max_y = ( top_monitor.max_y - top_monitor.min_y ) / 2;
		bottom_monitor.min_y = ( bottom_monitor.max_y - bottom_monitor.min_y ) / 2;
	
		if ( full_refresh != 0 )
			memset( dirtybuffer, 1, videoram_size[0] );
	
		/* On Playchoice 10 single monitor, this bit toggles	*/
		/* between PPU and BIOS display.						*/
		/* We support the multi-monitor layout. In this case,	*/
		/* if the bit is not set, then we should display		*/
		/* the PPU portion.										*/
	
		if (pc10_dispmask == 0)
		{
			/* render the ppu */
			ppu2c03b_render( 0, bitmap, 0, 0, 0, 30*8 );
	
			/* if this is a gun game, draw a simple crosshair */
			if ( pc10_gun_controller != 0 )
			{
				int x_center = readinputport( 5 );
				int y_center = readinputport( 6 ) + 30*8;
	
				draw_crosshair(bitmap,x_center,y_center,Machine.visible_area);
	
			}
		}
		else
		{
			/* the ppu is masked, clear out the area */
			fillbitmap( bitmap, Machine.pens[0], bottom_monitor );
		}
	
		/* When the bios is accessing vram, the video circuitry cant access it */
		if ( pc10_sdcs != 0)
		{
			fillbitmap( bitmap, Machine.pens[0], top_monitor );
			return;
		}
	
		for( offs = videoram_size[0] - 2; offs >= 0; offs -= 2 )
		{
			if ( dirtybuffer[offs]!=0 || dirtybuffer[offs+1]!=0 )
			{
				int offs2 = offs / 2;
	
				int sx = offs2 % 32;
				int sy = offs2 / 32;
	
				int tilenum = videoram.read(offs) + ( ( videoram.read(offs+1) & 7 ) << 8 );
				int color = ( videoram.read(offs+1) >> 3 ) & 0x1f;
	
				dirtybuffer[offs] = dirtybuffer[offs+1] = 0;
	
				drawgfx( tmpbitmap, Machine.gfx[0],
						 tilenum,
						 color,
						 0, 0,
						 8 * sx, 8 * sy,
						 Machine.visible_area, TRANSPARENCY_NONE, 0 );
			}
		}
	
		/* copy the temporary bitmap to the screen */
		copybitmap( bitmap, tmpbitmap, 0, 0, 0, 0, top_monitor, TRANSPARENCY_NONE, 0 );
	} };
}
