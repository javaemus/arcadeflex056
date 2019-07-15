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
import static mame056.inptport.*;
import static mame056.drawgfxH.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.vidhrdw.generic.*;
// refactor
import static arcadeflex036.osdepend.logerror;

public class phoenix
{
	
	
	/* in sndhrdw/pleiads.c */
	
	public static UBytePtr videoram_pg1 = new UBytePtr();
	public static UBytePtr videoram_pg2 = new UBytePtr();
	public static UBytePtr current_videoram_pg = new UBytePtr();
	static int current_videoram_pg_index;
	static int palette_bank;
	static int cocktail_mode;
	static int pleiads_protection_question;
	static int survival_protection_value;
	static struct_tilemap fg_tilemap, bg_tilemap;
	
	
	/***************************************************************************
	
	  Convert the color PROMs into a more useable format.
	
	  Phoenix has two 256x4 palette PROMs, one containing the high bits and the
	  other the low bits (2x2x2 color space).
	  The palette PROMs are connected to the RGB output this way:
	
	  bit 3 --
	        -- 270 ohm resistor  -- GREEN
	        -- 270 ohm resistor  -- BLUE
	  bit 0 -- 270 ohm resistor  -- RED
	
	  bit 3 --
	        -- GREEN
	        -- BLUE
	  bit 0 -- RED
	
	  plus 270 ohm pullup and pulldown resistors on all lines
	
	***************************************************************************/
        
        public static void COLOR(char []colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs])=(char) value;
        }
        
        public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
	
	public static VhConvertColorPromPtr phoenix_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
		int _palette = 0;	
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int bit0,bit1;
	
	
			bit0 = (color_prom.read(0)>> 0) & 0x01;
			bit1 = (color_prom.read(Machine.drv.total_colors)>> 0) & 0x01;
                        palette[_palette++] = (char) (0x55 * bit0 + 0xaa * bit1);
			bit0 = (color_prom.read(0)>> 2) & 0x01;
			bit1 = (color_prom.read(Machine.drv.total_colors)>> 2) & 0x01;
			palette[_palette++] = (char) (0x55 * bit0 + 0xaa * bit1);
			bit0 = (color_prom.read(0)>> 1) & 0x01;
			bit1 = (color_prom.read(Machine.drv.total_colors)>> 1) & 0x01;
			palette[_palette++] = (char) (0x55 * bit0 + 0xaa * bit1);
	
			color_prom.inc();
		}
	
		/* first bank of characters use colors 0x00-0x1f and 0x40-0x5f */
		/* second bank of characters use colors 0x20-0x3f and 0x60-0x7f */
		for (i = 0;i < 0x40;i++)
		{
			int col;
	
	
			col = ((i & 0x1c) >> 2) | ((i & 0x03) << 3) | ((i & 0x20) << 1);
	
			COLOR(colortable,0,i,col);
			COLOR(colortable,1,i,col | 0x20);
		}
	} };
	
	public static VhConvertColorPromPtr pleiads_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
		int _palette = 0;
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
			int bit0,bit1;
	
	
			bit0 = (color_prom.read(0)>> 0) & 0x01;
			bit1 = (color_prom.read(Machine.drv.total_colors)>> 0) & 0x01;
			palette[_palette++] = (char) (0x55 * bit0 + 0xaa * bit1);
			bit0 = (color_prom.read(0)>> 2) & 0x01;
			bit1 = (color_prom.read(Machine.drv.total_colors)>> 2) & 0x01;
			palette[_palette++] = (char) (0x55 * bit0 + 0xaa * bit1);
			bit0 = (color_prom.read(0)>> 1) & 0x01;
			bit1 = (color_prom.read(Machine.drv.total_colors)>> 1) & 0x01;
			palette[_palette++] = (char) (0x55 * bit0 + 0xaa * bit1);
	
			color_prom.inc();
		}
	
		/* first bank of characters use colors 0x00-0x1f, 0x40-0x5f, 0x80-0x9f and 0xc0-0xdf */
		/* second bank of characters use colors 0x20-0x3f, 0x60-0x7f, 0xa0-0xbf and 0xe0-0xff */
		for (i = 0;i < 0x80;i++)
		{
			int col;
	
	
			col = ((i & 0x1c) >> 2) | ((i & 0x03) << 3) | ((i & 0x60) << 1);
	
			COLOR(colortable,0,i,col);
			COLOR(colortable,1,i,col | 0x20);
		}
	} };
	
	/***************************************************************************
	
	  Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                int code;
	
		code = current_videoram_pg.read(tile_index);
		SET_TILE_INFO(
				1,
				code,
				(code >> 5) | (palette_bank << 3),
				/*TODO*///(tile_index & 0x1f)!=0 ? 0 : TILE_IGNORE_TRANSPARENCY);	/* first row (column) is opaque */
                                0);
            }
        };
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int code;
	
		code = current_videoram_pg.read(tile_index + 0x800);
		SET_TILE_INFO(
				0,
				code,
				(code >> 5) | (palette_bank << 3),
				0);
            }
        };
	
	
	/***************************************************************************
	
	  Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr phoenix_vh_start = new VhStartPtr() { public int handler() 
	{
		if ((videoram_pg1 = new UBytePtr(0x1000)) == null)
			return 1;
	
		if ((videoram_pg2 = new UBytePtr(0x1000)) == null)
			return 1;
	
	    current_videoram_pg_index = -1;
		current_videoram_pg = videoram_pg1;		/* otherwise, hiscore loading crashes */
	
	
		fg_tilemap = tilemap_create(get_fg_tile_info,tilemap_scan_rows,TILEMAP_TRANSPARENT,8,8,32,32);
		bg_tilemap = tilemap_create(get_bg_tile_info,tilemap_scan_rows,TILEMAP_OPAQUE,     8,8,32,32);
	
		if (fg_tilemap==null || bg_tilemap==null)
			return 1;
	
		/*TODO*///tilemap_set_transparent_pen(fg_tilemap,0);
                fg_tilemap.transparent_pen = 0;
	
		return 0;
	} };
	
	
	/***************************************************************************
	
	  Stop the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStopPtr phoenix_vh_stop = new VhStopPtr() { public void handler() 
	{
		videoram_pg1 = null;
		videoram_pg2 = null;	
	} };
	
	
	/***************************************************************************
	
	  Memory handlers
	
	***************************************************************************/
	
	public static ReadHandlerPtr phoenix_videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return current_videoram_pg.read(offset);
	} };
	
	public static WriteHandlerPtr phoenix_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr rom = new UBytePtr(memory_region(REGION_CPU1));
	
		current_videoram_pg.write(offset, data);
	
		if ((offset & 0x7ff) < 0x340)
		{
			if ((offset & 0x800) != 0)
				tilemap_mark_tile_dirty(bg_tilemap,offset & 0x3ff);
			else
				tilemap_mark_tile_dirty(fg_tilemap,offset & 0x3ff);
		}
	
		/* as part of the protecion, Survival executes code from $43a4 */
		rom.write(offset + 0x4000, data);
	} };
	
	
	public static WriteHandlerPtr phoenix_videoreg_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    if (current_videoram_pg_index != (data & 1))
		{
			/* set memory bank */
			current_videoram_pg_index = data & 1;
			current_videoram_pg = current_videoram_pg_index!=0 ? videoram_pg2 : videoram_pg1;
	
			cocktail_mode = (current_videoram_pg_index!=0 && (input_port_3_r.handler(0) & 0x01)!=0)?1:0;
	
			tilemap_set_flip(ALL_TILEMAPS, cocktail_mode!=0 ? (TILEMAP_FLIPX | TILEMAP_FLIPY) : 0);
			tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
		}
	
		/* Phoenix has only one palette select effecting both layers */
		if (palette_bank != ((data >> 1) & 1))
		{
			palette_bank = (data >> 1) & 1;
	
			tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
		}
	} };
	
	public static WriteHandlerPtr pleiads_videoreg_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    if (current_videoram_pg_index != (data & 1))
		{
			/* set memory bank */
			current_videoram_pg_index = data & 1;
			current_videoram_pg = current_videoram_pg_index!=0 ? videoram_pg2 : videoram_pg1;
	
			cocktail_mode = (current_videoram_pg_index!=0 && (input_port_3_r.handler(0) & 0x01)!=0)?1:0;
	
			tilemap_set_flip(ALL_TILEMAPS, cocktail_mode!=0 ? (TILEMAP_FLIPX | TILEMAP_FLIPY) : 0);
			tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
		}
	
	
		/* the palette table is at $0420-$042f and is set by $06bc.
		   Four palette changes by level.  The palette selection is
		   wrong, but the same paletter is used for both layers. */
	
	    if (palette_bank != ((~data >> 1) & 3))
		{
			palette_bank = ((~data >> 1) & 3);
	
			tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
	
			logerror("Palette: %02X\n", (data & 0x06) >> 1);
		}
	
		pleiads_protection_question = data & 0xfc;
	
		/* send two bits to sound control C (not sure if they are there) */
		/*TODO*///pleiads_sound_control_c_w(offset, data);
	} };
	
	
	public static WriteHandlerPtr phoenix_scroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		tilemap_set_scrollx(bg_tilemap,0,data);
	} };
	
	
	public static ReadHandlerPtr phoenix_input_port_0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (cocktail_mode != 0)
			return (input_port_0_r.handler(0) & 0x07) | (input_port_1_r.handler(0) & 0xf8);
		else
			return input_port_0_r.handler(0);
	} };
	
	public static ReadHandlerPtr pleiads_input_port_0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int ret = phoenix_input_port_0_r.handler(0) & 0xf7;
	
		/* handle Pleiads protection */
		switch (pleiads_protection_question)
		{
		case 0x00:
		case 0x20:
			/* Bit 3 is 0 */
			break;
		case 0x0c:
		case 0x30:
			/* Bit 3 is 1 */
			ret	|= 0x08;
			break;
		default:
			logerror("Unknown protection question %02X at %04X\n", pleiads_protection_question, cpu_get_pc());
		}
	
		return ret;
	} };
	
	public static ReadHandlerPtr survival_input_port_0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int ret = phoenix_input_port_0_r.handler(0);
	
		if (survival_protection_value != 0)
		{
			ret ^= 0xf0;
		}
	
		return ret;
	} };
	
	public static ReadHandlerPtr survival_protection_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (cpu_get_pc() == 0x2017)
		{
			survival_protection_value ^= 1;
		}
	
		return survival_protection_value;
	} };
	
	/***************************************************************************
	
	  Display refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr phoenix_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		/*TODO*///tilemap_draw(bitmap,bg_tilemap,0,0);
                tilemap_draw(bitmap,bg_tilemap,0);
		/*TODO*///tilemap_draw(bitmap,fg_tilemap,0,0);
                tilemap_draw(bitmap,fg_tilemap,0);
                
                // HACK - ONLY for tilemaps 0.37. REMOVE in 0.56
                tilemap_update(ALL_TILEMAPS);	
		tilemap_render(ALL_TILEMAPS);
                // END HACK
	} };
}
