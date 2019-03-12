/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.vidhrdw;

import common.ptr.UBytePtr;
import static mame056.tilemapH.*;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.commonH.*;
import static mame056.mame.*;
import static mame056.cpuexec.*;

public class gberet
{
	
	
	
	public static UBytePtr gberet_videoram=new UBytePtr(),gberet_colorram=new UBytePtr();
	public static UBytePtr gberet_spritebank=new UBytePtr();
	public static UBytePtr gberet_scrollram=new UBytePtr();
	/*TODO*///public static tilemap bg_tilemap;
	static int interruptenable;
	static int flipscreen;
	
        public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
		
        public static void COLOR(int gfxn, int offs, char[] colortable, int value){
            colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs]=(char) value;
         }
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Green Beret has a 32 bytes palette PROM and two 256 bytes color lookup table
	  PROMs (one for sprites, one for characters).
	  The palette PROM is connected to the RGB output, this way:
	
	  bit 7 -- 220 ohm resistor  -- BLUE
	        -- 470 ohm resistor  -- BLUE
	        -- 220 ohm resistor  -- GREEN
	        -- 470 ohm resistor  -- GREEN
	        -- 1  kohm resistor  -- GREEN
	        -- 220 ohm resistor  -- RED
	        -- 470 ohm resistor  -- RED
	  bit 0 -- 1  kohm resistor  -- RED
	
	***************************************************************************/
	
	public static VhConvertColorPromPtr gberet_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
		
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int bit0,bit1,bit2;
	
	
			/*TODO*///bit0 = (*color_prom >> 0) & 0x01;
			/*TODO*///bit1 = (*color_prom >> 1) & 0x01;
			/*TODO*///bit2 = (*color_prom >> 2) & 0x01;
			/*TODO*///*(palette++) = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/*TODO*///bit0 = (*color_prom >> 3) & 0x01;
			/*TODO*///bit1 = (*color_prom >> 4) & 0x01;
			/*TODO*///bit2 = (*color_prom >> 5) & 0x01;
			/*TODO*///*(palette++) = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
			/*TODO*///bit0 = 0;
			/*TODO*///bit1 = (*color_prom >> 6) & 0x01;
			/*TODO*///bit2 = (*color_prom >> 7) & 0x01;
			/*TODO*///*(palette++) = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
	
			/*TODO*///color_prom++;
		}
	
		for (i = 0;i < TOTAL_COLORS(1);i++)
		{
			/*TODO*///if (*color_prom & 0x0f) COLOR(1,i) = *color_prom & 0x0f;
			/*TODO*///else COLOR(1,i) = 0;
			/*TODO*///color_prom++;
		}
		for (i = 0;i < TOTAL_COLORS(0);i++)
		{
			/*TODO*///COLOR(0,i) = (*(color_prom++) & 0x0f) + 0x10;
		}
            }
        };

	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static void get_tile_info(int tile_index)
	{
		char attr = gberet_colorram.read(tile_index);
		/*TODO*///SET_TILE_INFO(
		/*TODO*///		0,
		/*TODO*///		gberet_videoram.read(tile_index) + ((attr & 0x40) << 2),
		/*TODO*///		attr & 0x0f,
		/*TODO*///		TILE_FLIPYX((attr & 0x30) >> 4));
		tile_info.priority = (attr & 0x80) >> 7;
	}
	
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr gberet_vh_start = new VhStartPtr() { public int handler() 
	{
		/*TODO*///bg_tilemap = tilemap_create(get_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT_COLOR,8,8,64,32);
	
		/*TODO*///if (bg_tilemap == null)
		/*TODO*///	return 0;
	
		/*TODO*///tilemap_set_transparent_pen(bg_tilemap,0x10);
		/*TODO*///tilemap_set_scroll_rows(bg_tilemap,32);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr gberet_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*TODO*///if (gberet_videoram[offset] != data)
		/*TODO*///{
		/*TODO*///	gberet_videoram[offset] = data;
		/*TODO*///	tilemap_mark_tile_dirty(bg_tilemap,offset);
		/*TODO*///}
	} };
	
	public static WriteHandlerPtr gberet_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*TODO*///if (gberet_colorram[offset] != data)
		/*TODO*///{
		/*TODO*///	gberet_colorram[offset] = data;
		/*TODO*///	tilemap_mark_tile_dirty(bg_tilemap,offset);
		/*TODO*///}
	} };
	
	public static WriteHandlerPtr gberet_e044_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* bit 0 enables interrupts */
		interruptenable = data & 1;
	
		/* bit 3 flips screen */
		flipscreen = data & 0x08;
		/*TODO*///tilemap_set_flip(ALL_TILEMAPS,flipscreen ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
	
		/* don't know about the other bits */
	} };
	
	public static WriteHandlerPtr gberet_scroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int scroll;
	
		/*TODO*///gberet_scrollram[offset] = data;
	
		/*TODO*///scroll = gberet_scrollram[offset & 0x1f] | (gberet_scrollram[offset | 0x20] << 8);
		/*TODO*///tilemap_set_scrollx(bg_tilemap,offset & 0x1f,scroll);
	} };
	
	public static WriteHandlerPtr gberetb_scroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int scroll;
	
		scroll = data;
		/*TODO*///if (offset) scroll |= 0x100;
	
		/*TODO*///for (offset = 6;offset < 29;offset++)
		/*TODO*///	tilemap_set_scrollx(bg_tilemap,offset,scroll + 64-8);
	} };
	
	
	public static InterruptPtr gberet_interrupt = new InterruptPtr() { public int handler() 
	{
		/*TODO*///if (cpu_getiloops() == 0) return interrupt();
		/*TODO*///else if (cpu_getiloops() % 2)
		/*TODO*///{
		/*TODO*///	if (interruptenable) return nmi_interrupt();
		/*TODO*///}
	
		return ignore_interrupt.handler();
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	/*TODO*///static void draw_sprites(struct mame_bitmap *bitmap)
	/*TODO*///{
	/*TODO*///	int offs;
	/*TODO*///	unsigned char *sr;
	/*TODO*///
	/*TODO*///	if (*gberet_spritebank & 0x08)
	/*TODO*///		sr = spriteram_2;
	/*TODO*///	else sr = spriteram;
	
	/*TODO*///	for (offs = 0;offs < spriteram_size;offs += 4)
	/*TODO*///	{
	/*TODO*///		if (sr[offs+3])
	/*TODO*///		{
	/*TODO*///			int sx,sy,flipx,flipy;
	
	
	/*TODO*///			sx = sr[offs+2] - 2*(sr[offs+1] & 0x80);
	/*TODO*///			sy = sr[offs+3];
	/*TODO*///			flipx = sr[offs+1] & 0x10;
	/*TODO*///			flipy = sr[offs+1] & 0x20;
	
	/*TODO*///			if (flipscreen)
	/*TODO*///			{
	/*TODO*///				sx = 240 - sx;
	/*TODO*///				sy = 240 - sy;
	/*TODO*///				flipx = !flipx;
	/*TODO*///				flipy = !flipy;
	/*TODO*///			}
	
	/*TODO*///			drawgfx(bitmap,Machine.gfx[1],
	/*TODO*///					sr[offs+0] + ((sr[offs+1] & 0x40) << 2),
	/*TODO*///					sr[offs+1] & 0x0f,
	/*TODO*///					flipx,flipy,
	/*TODO*///					sx,sy,
	/*TODO*///					&Machine.visible_area,TRANSPARENCY_COLOR,0);
	/*TODO*///		}
	/*TODO*///	}
	/*TODO*///}
	
	static void draw_sprites_bootleg(mame_bitmap bitmap)
	{
		int offs;
		/*TODO*///unsigned char *sr;
	
		/*TODO*///sr = spriteram;
	
		/*TODO*///for (offs = spriteram_size - 4;offs >= 0;offs -= 4)
		/*TODO*///{
		/*TODO*///	if (sr[offs+1])
		/*TODO*///	{
		/*TODO*///		int sx,sy,flipx,flipy;
	
	
		/*TODO*///		sx = sr[offs+2] - 2*(sr[offs+3] & 0x80);
		/*TODO*///		sy = sr[offs+1];
		/*TODO*///		sy = 240 - sy;
		/*TODO*///		flipx = sr[offs+3] & 0x10;
		/*TODO*///		flipy = sr[offs+3] & 0x20;
	
		/*TODO*///		if (flipscreen)
		/*TODO*///		{
		/*TODO*///			sx = 240 - sx;
		/*TODO*///			sy = 240 - sy;
		/*TODO*///			flipx = !flipx;
		/*TODO*///			flipy = !flipy;
		/*TODO*///		}
	
		/*TODO*///		drawgfx(bitmap,Machine.gfx[1],
		/*TODO*///				sr[offs+0] + ((sr[offs+3] & 0x40) << 2),
		/*TODO*///				sr[offs+3] & 0x0f,
		/*TODO*///				flipx,flipy,
		/*TODO*///				sx,sy,
		/*TODO*///				&Machine.visible_area,TRANSPARENCY_COLOR,0);
		/*TODO*///	}
		/*TODO*///}
	}
	
	
	public static VhUpdatePtr gberet_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/*TODO*///tilemap_draw(bitmap,bg_tilemap,TILEMAP_IGNORE_TRANSPARENCY|0,0);
		/*TODO*///tilemap_draw(bitmap,bg_tilemap,TILEMAP_IGNORE_TRANSPARENCY|1,0);
		/*TODO*///draw_sprites(bitmap);
		/*TODO*///tilemap_draw(bitmap,bg_tilemap,0,0);
	} };
	
	public static VhUpdatePtr gberetb_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/*TODO*///tilemap_draw(bitmap,bg_tilemap,TILEMAP_IGNORE_TRANSPARENCY|0,0);
		/*TODO*///tilemap_draw(bitmap,bg_tilemap,TILEMAP_IGNORE_TRANSPARENCY|1,0);
		draw_sprites_bootleg(bitmap);
		/*TODO*///tilemap_draw(bitmap,bg_tilemap,0,0);
	} };
}
