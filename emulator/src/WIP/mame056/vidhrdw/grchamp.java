/*	video hardware for Taito Grand Champion */

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.tilemapH.*;
//import static mame056.tilemapC.*;
import static mame037b11.mame.tilemapC.*;
import static mame056.vidhrdw.generic.*;
import static common.libc.cstring.*;
import static mame056.drawgfxH.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;

public class grchamp
{
	
	public static UBytePtr grchamp_videoreg0 = new UBytePtr( 1024 );
	public static int grchamp_player_xpos;
	public static int grchamp_player_ypos;
	public static int grchamp_collision;
	
	static int grchamp_tile_number;
	static int grchamp_rain_xpos;
	static int grchamp_rain_ypos;
	static int palette_bank;
	static mame_bitmap headlight_bitmap;
	static mame_bitmap work_bitmap;
	
	public static int[] grchamp_vreg1 = new int[0x10];	/* background control registers */
	public static UBytePtr grchamp_videoram = new UBytePtr();	/* background tilemaps */
	public static UBytePtr grchamp_radar = new UBytePtr();		/* bitmap for radar */
	
	static struct_tilemap[] tilemap = new struct_tilemap[3];
        static {
            for (int i=0 ; i<3 ; i++)
                tilemap[i] = new struct_tilemap();
        }
	
	public static WriteHandlerPtr grchamp_player_xpos_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		grchamp_player_xpos = data;
	} };
	
	public static WriteHandlerPtr grchamp_player_ypos_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		grchamp_player_ypos = data;
	} };
	
	public static WriteHandlerPtr grchamp_tile_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* tile select: bits 4..7:rain; bits 0..3:player car */
		grchamp_tile_number = data;
	} };
	
	public static WriteHandlerPtr grchamp_rain_xpos_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		grchamp_rain_xpos = data;
	} };
	
	public static WriteHandlerPtr grchamp_rain_ypos_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		grchamp_rain_ypos = data;
	} };
	
	public static VhConvertColorPromPtr grchamp_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
                int _palette = 0;
                
		for( i=0; i<0x20; i++ )
		{
			int data = color_prom.readinc();
			int bit0,bit1,bit2;
			/* red component */
			bit0 = (data >> 0) & 0x01;
			bit1 = (data >> 1) & 0x01;
			bit2 = (data >> 2) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* green component */
			bit0 = (data >> 3) & 0x01;
			bit1 = (data >> 4) & 0x01;
			bit2 = (data >> 5) & 0x01;
			palette[_palette++] = (char) (0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2);
			/* blue component */
			bit0 = (data >> 6) & 0x01;
			bit1 = (data >> 7) & 0x01;
			palette[_palette++] = (char) (0x4f * bit0 + 0xa8 * bit1);
		}
	
		for( i=0; i<0x20; i++ )
		{
			int data = color_prom.readinc();
			int r = (data&4)!=0?0:1;
			int g = (data&2)!=0?0:1;
			int b = (data&1)!=0?0:1;
			int intensity = (data&0x08)!=0?0x55:0xff;
			palette[_palette++] = (char) (r*intensity);
			palette[_palette++] = (char) (g*intensity);
			palette[_palette++] = (char) (b*intensity);
		}
		/* add a fake entry for fog */
		palette[_palette++] = 0x55;
		palette[_palette++] = 0x55;
		palette[_palette++] = 0x55;
	
		memset( palette, 0x00, 3*3 );
            }
        };
	
	public static WriteHandlerPtr grchamp_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( grchamp_videoram.read(offset)!=data )
		{
			grchamp_videoram.write(offset, data);
			tilemap_mark_tile_dirty( tilemap[offset/0x800], offset%0x800 );
		}
	} };
	
	public static GetTileInfoPtr get_bg0_tile_info = new GetTileInfoPtr() {
            public void handler(int offset) {
                int tile_number = grchamp_videoram.read(offset);
		SET_TILE_INFO(
				1,
				tile_number,
				palette_bank,
				0);
            }
        };
	
	static GetTileInfoPtr get_bg1_tile_info = new GetTileInfoPtr() {
            public void handler(int offset) {
		int tile_number = grchamp_videoram.read(offset+0x800)+256;
		SET_TILE_INFO(
				1,
				tile_number,
				palette_bank,
				0);
            }
        };
	
	static GetTileInfoPtr get_bg2_tile_info = new GetTileInfoPtr() {
            public void handler(int offset) {
		int tile_number = grchamp_videoram.read(offset+0x800*2)+256*2;
		SET_TILE_INFO(
				1,
				tile_number,
				0,
				0);
            }
        };
	
	static GetMemoryOffsetPtr get_memory_offset = new GetMemoryOffsetPtr() {
            public int handler( int col, int row, int num_cols, int num_rows ) {
                int offset = (31-row)*32;
		offset += 31-(col%32);
		if(( col/32 )!=0) offset += 0x400;
		return offset;
            }
        };
	
	public static VhStartPtr grchamp_vh_start = new VhStartPtr() { public int handler() 
	{
		headlight_bitmap = bitmap_alloc( 64,128 );
		if( headlight_bitmap != null ){
			work_bitmap = bitmap_alloc( 32,32 );
			if( work_bitmap != null ){
                                tilemap = new struct_tilemap[3];
				tilemap[0] = tilemap_create(get_bg0_tile_info,get_memory_offset,TILEMAP_OPAQUE,8,8,64,32);
				/*TODO*///tilemap[1] = tilemap_create(get_bg1_tile_info,get_memory_offset,TILEMAP_TRANSPARENT,8,8,64,32);
                                tilemap[1] = tilemap_create(get_bg1_tile_info,get_memory_offset,TILEMAP_OPAQUE,8,8,64,32);
				/*TODO*///tilemap[2] = tilemap_create(get_bg2_tile_info,get_memory_offset,TILEMAP_TRANSPARENT,8,8,64,32);
                                tilemap[2] = tilemap_create(get_bg2_tile_info,get_memory_offset,TILEMAP_OPAQUE,8,8,64,32);
				if( tilemap[0]!=null && tilemap[1]!=null && tilemap[2]!=null )
				{
					/*TODO*///tilemap_set_transparent_pen( tilemap[1], 0 );
                                        tilemap[1].transparent_pen = 0;
					/*TODO*///tilemap_set_transparent_pen( tilemap[2], 0 );
                                        tilemap[2].transparent_pen = 0;
                                        
					return 0;
				}
				bitmap_free( work_bitmap );
			}
			bitmap_free( headlight_bitmap );
		}
		return 1;
	} };
	
	public static VhStopPtr grchamp_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free( headlight_bitmap );
		bitmap_free( work_bitmap );
	} };
	
	static void draw_text( mame_bitmap bitmap )
	{
		rectangle clip = new rectangle(Machine.visible_area);
		GfxElement gfx = Machine.gfx[0];
		UBytePtr source = new UBytePtr(videoram);
		int bank = (grchamp_videoreg0.read()&0x20)!=0?256:0;
		int offs;
		for( offs=0; offs<0x400; offs++ )
		{
			int col = offs%32;
			int row = offs/32;
			int scroll = colorram.read(col*2)-1;
			int attributes = colorram.read(col*2+1);
			int tile_number = source.read(offs);
	
			drawgfx( bitmap, gfx,
				bank + tile_number,
				attributes,
				0,0, /* no flip */
				8*col,
				(8*row-scroll)&0xff,
				clip,
				TRANSPARENCY_PEN, 0 );
		}
	}
	
	static void draw_background( mame_bitmap bitmap )
	{
		int dx = -48;
		int dy = 16;
		int attributes = grchamp_vreg1[0x3];
			/*	----xxxx	Analog Tachometer output
			**	---x----	palette select
			**	--x-----	enables msb of bg#3 xscroll
			**	xx------	unused
			*/
	
		int color = (attributes&0x10)!=0?1:0;
		if( color!=palette_bank )
		{
			palette_bank = color;
			tilemap_mark_all_tiles_dirty( ALL_TILEMAPS );
		}
	
		tilemap_set_scrollx( tilemap[0], 0, dx-(grchamp_vreg1[0x0]+grchamp_vreg1[0x1]*256) );
		tilemap_set_scrolly( tilemap[0], 0, dy - grchamp_vreg1[0x2] );
		tilemap_set_scrollx( tilemap[1], 0, dx-(grchamp_vreg1[0x5]+grchamp_vreg1[0x6]*256) );
		tilemap_set_scrolly( tilemap[1], 0, dy - grchamp_vreg1[0x7] );
		tilemap_set_scrollx( tilemap[2], 0, dx-(grchamp_vreg1[0x9]+ ((attributes&0x20)!=0?256:(grchamp_vreg1[0xa]*256))));
		tilemap_set_scrolly( tilemap[2], 0, dy - grchamp_vreg1[0xb] );
	
		/*TODO*///tilemap_draw(bitmap,tilemap[0],0,0);
                tilemap_draw(bitmap,tilemap[0],0);
		/*TODO*///tilemap_draw(bitmap,tilemap[1],0,0);
                tilemap_draw(bitmap,tilemap[1],0);
		/*TODO*///tilemap_draw(bitmap,tilemap[2],0,0);
                tilemap_draw(bitmap,tilemap[2],0);
	}
	
	static void draw_player_car( mame_bitmap bitmap )
	{
		drawgfx( bitmap,
			Machine.gfx[2],
			grchamp_tile_number&0xf,
			1, /* color = red */
			0,0, /* flip */
			256-grchamp_player_xpos,
			240-grchamp_player_ypos,
			Machine.visible_area,
			TRANSPARENCY_PEN, 0 );
	}
	
	static int collision_check( mame_bitmap bitmap, int which )
	{
		int bgcolor = Machine.pens[0];
		int sprite_transp = Machine.pens[0x24];
		rectangle clip = new rectangle(Machine.visible_area);
		int y0 = 240-grchamp_player_ypos;
		int x0 = 256-grchamp_player_xpos;
		int x,y;
		int result = 0;
	
		if( which==0 )
		{
			/* draw the current player sprite into a work bitmap */
			drawgfx( work_bitmap, Machine.gfx[2],
				grchamp_tile_number&0xf,
				1, /* color */
				0,0,
				0,0,
				null,
				TRANSPARENCY_NONE, 0 );
		}
	
		for( y = 0; y <32; y++ )
		{
			for( x = 0; x<32; x++ )
			{
				if( read_pixel.handler(work_bitmap,x,y) != sprite_transp ){
					int sx = x+x0;
					int sy = y+y0;
					if( sx >= clip.min_x && sx <= clip.max_x &&
						sy >= clip.min_y && sy <= clip.max_y )
					{
						if( read_pixel.handler(bitmap, sx, sy) != bgcolor )
						{
							result = 1; /* flag collision */
	
							/*	wipe this pixel, so collision checks with the
							**	next layer work */
							plot_pixel.handler(bitmap, sx, sy, bgcolor );
						}
					}
				}
	        }
		}
		return result!=0?(1<<which):0;
	}
	
	static void draw_rain( mame_bitmap bitmap ){
		GfxElement gfx = Machine.gfx[4];
		rectangle clip = new rectangle(Machine.visible_area);
		int tile_number = grchamp_tile_number>>4;
		if( tile_number != 0 ){
			int scrollx = grchamp_rain_xpos;
			int scrolly = grchamp_rain_ypos;
			int sx,sy;
			for( sy=0; sy<256; sy+=16 ){
				for( sx=0; sx<256; sx+=16 ){
					drawgfx( bitmap, gfx,
						tile_number, 0,
						0,0,
						(sx+scrollx)&0xff,(sy+scrolly)&0xff,
						clip,
						TRANSPARENCY_PEN, 0 );
				}
			}
		}
	}
	
	static void draw_fog( mame_bitmap bitmap, int bFog ){
		int x0 = 256-grchamp_player_xpos-64;
		int y0 = 240-grchamp_player_ypos-64;
		int color = Machine.pens[bFog!=0?0x40:0x00];
	
		copybitmap(
			headlight_bitmap, /* dest */
			bitmap, /* source */
			0,0, /* flipx,flipy */
			-x0,-y0, /* sx,sy */
			null, /* clip */
			TRANSPARENCY_NONE,0 );
	
		fillbitmap( bitmap,color,null );
	}
	
	static void draw_headlights( mame_bitmap bitmap, int bFog )
	{
		int sx, sy, color;
		int x0 = 256-grchamp_player_xpos-64;
		int y0 = 240-grchamp_player_ypos-64;
		UBytePtr source = new UBytePtr(memory_region( REGION_GFX4 ));
		int x,y,bit;
		if (bFog == 0) source.inc( 0x400 );
		for( y=0; y<128; y++ )
		{
			for( x=0; x<64; x+=8 )
			{
				int data = source.readinc();
				if( data != 0 )
				{
					for( bit=0; bit<8; bit++ )
					{
						if(( data&0x80 ) != 0){
							sx = x0+x+bit;
							sy = y0+y;
							if( sx>=0 && sy>=0 && sx<=255 && sy<=255 )
							{
								color = read_pixel.handler(headlight_bitmap, x+bit, y );
								plot_pixel.handler(bitmap, sx,sy, color );
							}
						}
						data <<= 1;
					}
				}
			}
		}
	}
	
	static void draw_radar( mame_bitmap bitmap ){
		UBytePtr source = new UBytePtr(grchamp_radar);
		int color = Machine.pens[3];
		int offs;
		for( offs=0; offs<0x400; offs++ ){
			int data = source.read(offs);
			if( data != 0 ){
				int x = (offs%32)*8;
				int y = (offs/32)+16;
				int bit;
				for( bit=0; bit<8; bit++ ){
					if( (data&0x80) != 0 ) plot_pixel.handler(bitmap, x+bit, y, color );
					data <<= 1;
				}
			}
		}
	}
	
	static void draw_tachometer( mame_bitmap bitmap ){
	/*
		int value = grchamp_vreg1[0x03]&0xf;
		int i;
		for( i=0; i<value; i++ ){
			drawgfx( bitmap, Machine.uifont,
				'*',
				0,
				0,0,
				i*6+32,64,
				0,
				TRANSPARENCY_NONE, 0 );
		}
	*/
	}
	
	static void draw_sprites( mame_bitmap bitmap, int bFog ){
		GfxElement gfx = Machine.gfx[3];
		rectangle clip = new rectangle(Machine.visible_area);
		int bank = (grchamp_videoreg0.read()&0x20)!=0?0x40:0x00;
		UBytePtr source = new UBytePtr(spriteram);
		UBytePtr finish = new UBytePtr(source, 0x40);
		while( source.offset<finish.offset ){
			int sx = source.read(3);
			int sy = 240-source.read(0);
			int color = bFog!=0?8:source.read(2);
			int code = source.read(1);
			drawgfx( bitmap, gfx,
				bank + (code&0x3f),
				color,
				code&0x40,code&0x80,
				sx,sy,
				clip,
				TRANSPARENCY_PEN, 0 );
			source.inc( 4 );
		}
	}
	
	public static VhUpdatePtr grchamp_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) {
		int bFog = grchamp_videoreg0.read()&0x40;
	
		draw_background( bitmap ); /* 3 layers */
	
		grchamp_collision = collision_check( bitmap,0 );
		draw_sprites( bitmap, 0 ); /* computer cars */
		grchamp_collision |= collision_check( bitmap,1 );
	
		draw_player_car( bitmap );
	
		if(( grchamp_videoreg0.read()&(0x10|0x40) ) != 0){
			draw_fog( bitmap,bFog ); /* grey fog / black tunnel darkness */
		}
	
		/* fog covered sprites look like black shadows */
		if( bFog != 0 ) draw_sprites( bitmap, bFog );
	
		/* paint the visible area exposed by headlights shape */
		if(( grchamp_videoreg0.read()&(0x10|0x40) ) != 0){
			draw_headlights( bitmap,bFog );
		}
	
		draw_rain( bitmap );
		draw_text( bitmap );
		if(( grchamp_videoreg0.read()&0x80 ) != 0) draw_radar( bitmap );
		draw_tachometer( bitmap );
                
                // HACK - ONLY for tilemaps 0.37. REMOVE in 0.56
                tilemap_update(ALL_TILEMAPS);	
		tilemap_render(ALL_TILEMAPS);
                // END HACK
	} };
}
