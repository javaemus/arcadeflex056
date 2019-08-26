/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.drivers.ddrible.ddrible_int_enable_0;
import static WIP.mame056.drivers.ddrible.ddrible_int_enable_1;

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

public class ddrible
{
	public static UBytePtr ddrible_fg_videoram = new UBytePtr();
	public static UBytePtr ddrible_bg_videoram = new UBytePtr();
	public static UBytePtr ddrible_spriteram_1 = new UBytePtr();
	public static UBytePtr ddrible_spriteram_2 = new UBytePtr();
	
	static int[][] ddribble_vregs = new int[2][5];
	static int[] charbank = new int[2];
	
	static struct_tilemap fg_tilemap,bg_tilemap;
	
	public static int TOTAL_COLORS(int gfxn){
            return (Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity);
        }
        
	public static void COLOR(char[] colortable, int gfxn, int offs, int value){
            (colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs]) = (char) value;
        }
        
	public static VhConvertColorPromPtr ddrible_vh_convert_color_prom = new VhConvertColorPromPtr() { public void handler(char []palette, char []colortable, UBytePtr color_prom) 
	{
		int i;
	
                /* build the lookup table for sprites. Palette is dynamic. */
		for (i = 0;i < TOTAL_COLORS(3);i++)
			COLOR(colortable,3,i,color_prom.readinc() & 0x0f);
	} };
	
	public static WriteHandlerPtr K005885_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		switch (offset){
			case 0x03:	/* char bank selection for set 1 */
				if ((data & 0x03) != charbank[0])
				{
					charbank[0] = data & 0x03;
					tilemap_mark_all_tiles_dirty( fg_tilemap );
				}
				break;
			case 0x04:	/* IRQ control, flipscreen */
				ddrible_int_enable_0 = data & 0x02;
				break;
		}
		ddribble_vregs[0][offset] = data;
	} };
	
	public static WriteHandlerPtr K005885_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		switch (offset){
			case 0x03:	/* char bank selection for set 2 */
				if ((data & 0x03) != charbank[1])
				{
					charbank[1] = data & 0x03;
					tilemap_mark_all_tiles_dirty( bg_tilemap );
				}
				break;
			case 0x04:	/* IRQ control, flipscreen */
				ddrible_int_enable_1 = data & 0x02;
				break;
		}
		ddribble_vregs[1][offset] = data;
	} };
	
	/***************************************************************************
	
		Callbacks for the TileMap code
	
	***************************************************************************/
	
	static GetMemoryOffsetPtr tilemap_scan = new GetMemoryOffsetPtr() {
            public int handler(int col,int row,int num_cols,int num_rows) {
                /* logical (col,row) . memory offset */
		return (col & 0x1f) + ((row & 0x1f) << 5) + ((col & 0x20) << 6);	/* skip 0x400 */
            }
        };
	
	static GetTileInfoPtr get_fg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
                int attr = ddrible_fg_videoram.read(tile_index);
		int num = ddrible_fg_videoram.read(tile_index + 0x400) +
				((attr & 0xc0) << 2) + ((attr & 0x20) << 5) + ((charbank[0] & 2) << 10);
		SET_TILE_INFO(
				0,
				num,
				0,
				TILE_FLIPYX((attr & 0x30) >> 4));
            }
        };
	
	static GetTileInfoPtr get_bg_tile_info = new GetTileInfoPtr() {
            public void handler(int tile_index) {
		int attr = ddrible_bg_videoram.read(tile_index);
		int num = ddrible_bg_videoram.read(tile_index + 0x400) +
				((attr & 0xc0) << 2) + ((attr & 0x20) << 5) + (charbank[1] << 11);
		SET_TILE_INFO(
				1,
				num,
				0,
				TILE_FLIPYX((attr & 0x30) >> 4));
            }
        };
	
	/***************************************************************************
	
		Start the video hardware emulation.
	
	***************************************************************************/
	
	public static VhStartPtr ddrible_vh_start = new VhStartPtr() { public int handler() 
	{
		fg_tilemap = tilemap_create(get_fg_tile_info,tilemap_scan,TILEMAP_TRANSPARENT,8,8,64,32);
		bg_tilemap = tilemap_create(get_bg_tile_info,tilemap_scan,TILEMAP_OPAQUE,     8,8,64,32);
	
		if (fg_tilemap==null || bg_tilemap==null)
			return 1;
	
		/*TODO*///tilemap_set_transparent_pen(fg_tilemap,0);
                fg_tilemap.transparent_pen = 0;
	
		return 0;
	} };
	
	/***************************************************************************
	
		Memory handlers
	
	***************************************************************************/
	
	public static WriteHandlerPtr ddrible_fg_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (ddrible_fg_videoram.read(offset) != data)
		{
			ddrible_fg_videoram.write(offset, data);
			tilemap_mark_tile_dirty(fg_tilemap,offset & 0xbff);
		}
	} };
	
	public static WriteHandlerPtr ddrible_bg_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (ddrible_bg_videoram.read(offset) != data)
		{
			ddrible_bg_videoram.write(offset, data);
			tilemap_mark_tile_dirty(bg_tilemap,offset & 0xbff);
		}
	} };
	
	/***************************************************************************
	
		Double Dribble sprites
	
	Each sprite has 5 bytes:
	byte #0:	sprite number
	byte #1:
		bits 0..2:	sprite bank #
		bit 3:		not used?
		bits 4..7:	sprite color
	byte #2:	y position
	byte #3:	x position
	byte #4:	attributes
		bit 0:		x position (high bit)
		bit 1:		???
		bits 2..4:	sprite size
		bit 5:		flip x
		bit 6:		flip y
		bit 7:		unused?
	
	***************************************************************************/
        
        static int x_offset[] = { 0x00, 0x01 };
	static int y_offset[] = { 0x00, 0x02 };
	
	static void ddribble_draw_sprites( mame_bitmap bitmap, UBytePtr source, int lenght, int gfxset, int flipscreen )
	{
		GfxElement gfx = Machine.gfx[gfxset];
		UBytePtr finish = new UBytePtr(source, lenght);
	
		while( source.offset < finish.offset )
		{
			int number = source.read(0) | ((source.read(1) & 0x07) << 8);	/* sprite number */
			int attr = source.read(4);								/* attributes */
			int sx = source.read(3) | ((attr & 0x01) << 8);			/* vertical position */
			int sy = source.read(2);									/* horizontal position */
			int flipx = attr & 0x20;							/* flip x */
			int flipy = attr & 0x40;							/* flip y */
			int color = (source.read(1) & 0xf0) >> 4;				/* color */
			int width,height;
	
			if (flipscreen != 0){
					flipx = flipx!=0?0:1;
					flipy = flipy!=0?0:1;
					sx = 240 - sx;
					sy = 240 - sy;
	
					if ((attr & 0x1c) == 0x10){	/* ???. needed for some sprites in flipped mode */
						sx -= 0x10;
						sy -= 0x10;
					}
			}
	
			switch (attr & 0x1c){
				case 0x10:	/* 32x32 */
					width = height = 2; number &= (~3); break;
				case 0x08:	/* 16x32 */
					width = 1; height = 2; number &= (~2); break;
				case 0x04:	/* 32x16 */
					width = 2; height = 1; number &= (~1); break;
				/* the hardware allow more sprite sizes, but ddribble doesn't use them */
				default:	/* 16x16 */
					width = height = 1; break;
			}
	
			{
				
				int x,y, ex, ey;
	
				for( y=0; y < height; y++ ){
					for( x=0; x < width; x++ ){
						ex = flipx!=0 ? (width-1-x) : x;
						ey = flipy!=0 ? (height-1-y) : y;
	
						drawgfx(bitmap,gfx,
							(number)+x_offset[ex]+y_offset[ey],
							color,
							flipx, flipy,
							sx+x*16,sy+y*16,
							Machine.visible_area,
							TRANSPARENCY_PEN, 0);
					}
				}
			}
			source.inc( 5 );
		}
	}
	
	/***************************************************************************
	
		Display Refresh
	
	***************************************************************************/
	
	public static VhUpdatePtr ddrible_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		tilemap_set_flip(fg_tilemap, (ddribble_vregs[0][4] & 0x08)!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
		tilemap_set_flip(bg_tilemap, (ddribble_vregs[1][4] & 0x08)!=0 ? (TILEMAP_FLIPY | TILEMAP_FLIPX) : 0);
	
		/* set scroll registers */
		tilemap_set_scrollx(fg_tilemap,0,ddribble_vregs[0][1] | ((ddribble_vregs[0][2] & 0x01) << 8));
		tilemap_set_scrollx(bg_tilemap,0,ddribble_vregs[1][1] | ((ddribble_vregs[1][2] & 0x01) << 8));
		tilemap_set_scrolly(fg_tilemap,0,ddribble_vregs[0][0]);
		tilemap_set_scrolly(bg_tilemap,0,ddribble_vregs[1][0]);
	
		/*TODO*///tilemap_draw(bitmap,bg_tilemap,0,0);
                tilemap_draw(bitmap,bg_tilemap,0);
                
		ddribble_draw_sprites(bitmap,ddrible_spriteram_1,0x07d,2,ddribble_vregs[0][4] & 0x08);
		ddribble_draw_sprites(bitmap,ddrible_spriteram_2,0x140,3,ddribble_vregs[1][4] & 0x08);
		/*TODO*///tilemap_draw(bitmap,fg_tilemap,0,0);
                tilemap_draw(bitmap,fg_tilemap,0);
	} };
}
