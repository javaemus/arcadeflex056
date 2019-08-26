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
import static mame056.tilemapH.*;
//import static mame056.tilemapC.*;
import static mame037b11.mame.tilemapC.*;
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

public class circusc
{
	
	
	
	public static UBytePtr circusc_videoram=new UBytePtr(), circusc_colorram=new UBytePtr();
	static struct_tilemap bg_tilemap;
	
	public static UBytePtr circusc_spritebank=new UBytePtr();
	public static UBytePtr circusc_scroll=new UBytePtr();
	
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Circus Charlie has one 32x8 palette PROM and two 256x4 lookup table PROMs
	  (one for characters, one for sprites).
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
        public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
	public static void COLOR(char[] colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs])=(char) value;
        }
        
	public static VhConvertColorPromPtr circusc_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
                int _palette = 0;
		
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int bit0,bit1,bit2;
	
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
	
		/* color_prom now points to the beginning of the lookup table */
	
		/* sprites */
		for (i = 0;i < TOTAL_COLORS(1);i++)
			COLOR(colortable,1,i,(color_prom.readinc()) & 0x0f);
	
		/* characters */
		for (i = 0;i < TOTAL_COLORS(0);i++)
			COLOR(colortable,0,i,(color_prom.readinc() & 0x0f) + 0x10);
	} };
	
	
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                int attr = circusc_colorram.read(tile_index);
		tile_info.priority = (attr & 0x10) >> 4;
		SET_TILE_INFO(
				0,
				circusc_videoram.read(tile_index) + ((attr & 0x20) << 3),
				attr & 0x0f,
				TILE_FLIPYX((attr & 0xc0) >> 6));
            }
        };
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr circusc_vh_start = new VhStartPtr() { public int handler() 
	{
		bg_tilemap = tilemap_create(get_tile_info,tilemap_scan_rows,TILEMAP_OPAQUE,8,8,32,32);
	
		if (bg_tilemap == null)
			return 1;
	
		tilemap_set_scroll_cols(bg_tilemap,32);
	
		return 0;
	} };
	
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr circusc_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (circusc_videoram.read(offset) != data)
		{
			circusc_videoram.write(offset, data);
			tilemap_mark_tile_dirty(bg_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr circusc_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (circusc_colorram.read(offset) != data)
		{
			circusc_colorram.write(offset, data);
			tilemap_mark_tile_dirty(bg_tilemap,offset);
		}
	} };
	
	public static WriteHandlerPtr circusc_flipscreen_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		flip_screen_set(data & 1);
	} };
	
	
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	static void draw_sprites(mame_bitmap bitmap)
	{
		int offs;
                UBytePtr sr = new UBytePtr();
	
	
		if ((circusc_spritebank.read() & 0x01) != 0)
			sr = spriteram;
		else sr = spriteram_2;
	
		for (offs = 0; offs < spriteram_size[0];offs += 4)
		{
			int sx,sy,flipx,flipy;
	
	
			sx = sr.read(offs + 2);
			sy = sr.read(offs + 3);
			flipx = sr.read(offs + 1) & 0x40;
			flipy = sr.read(offs + 1) & 0x80;
			if (flip_screen() != 0)
			{
				sx = 240 - sx;
				sy = 240 - sy;
				flipx = flipx!=0?0:1;
				flipy = flipy!=0?0:1;
			}
	
	
			drawgfx(bitmap,Machine.gfx[1],
					sr.read(offs + 0) + 8 * (sr.read(offs + 1) & 0x20),
					sr.read(offs + 1) & 0x0f,
					flipx,flipy,
					sx,sy,
					Machine.visible_area,TRANSPARENCY_COLOR,0);
	
		}
	}
	
	public static VhUpdatePtr circusc_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int i;
	
		for (i = 0;i < 10;i++)
			tilemap_set_scrolly(bg_tilemap,i,0);
		for (i = 10;i < 32;i++)
			tilemap_set_scrolly(bg_tilemap,i,circusc_scroll.read());
	
		/*TODO*///tilemap_draw(bitmap,bg_tilemap,1,0);
                tilemap_draw(bitmap,bg_tilemap,1);
		draw_sprites(bitmap);
		/*TODO*///tilemap_draw(bitmap,bg_tilemap,0,0);
                tilemap_draw(bitmap,bg_tilemap,0);
	} };
}
