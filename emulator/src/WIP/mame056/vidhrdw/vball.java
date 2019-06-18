/***************************************************************************

  Video Hardware for Championship V'ball by Paul Hampson
  Generally copied from China Gate by Paul Hampson
  "Mainly copied from Vidhrdw of Double Dragon (bootleg) & Double Dragon II"

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.libc.cstring.*;
import static common.ptr.*;
import static common.libc.expressions.*;

import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.palette.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class vball
{
	
	public static int vb_scrollx_hi=0;
	public static UBytePtr vb_scrollx_lo = new UBytePtr();
	public static UBytePtr vb_videoram = new UBytePtr();
	//unsigned char *spriteram;
	public static UBytePtr vb_attribram = new UBytePtr();
	public static UBytePtr vb_fgattribram = new UBytePtr();
	public static int vball_gfxset;
	public static int vb_bgprombank=0xff;
	public static int vb_spprombank=0xff;
	
	public static VhStartPtr vb_vh_start = new VhStartPtr() { public int handler() 
	{
		dirtybuffer = new char[ 0x800 ];
		if( dirtybuffer != null)
		{
			memset(dirtybuffer,1, 0x800);
	
			tmpbitmap = bitmap_alloc(Machine.drv.screen_width*2,Machine.drv.screen_height*2);
	
			if( tmpbitmap != null ) return 0;
	
			dirtybuffer = null;
		}
	
		return 1;
	} };
	
	
	
	public static VhStopPtr vb_vh_stop = new VhStopPtr() { public void handler() 
	{
		bitmap_free( tmpbitmap );
		dirtybuffer = null;
	} };
	
	public static void vb_bgprombank_w( int bank )
	{
		int i;
	
		UBytePtr color_prom = new UBytePtr();
	
		if (bank==vb_bgprombank) return;
	
		color_prom = new UBytePtr(memory_region(REGION_PROMS), bank*0x80);
	
		logerror("BGPROM Bank:%x, bank offset:%x\n",bank, bank*0x80);
	
		for (i=0;i<128;i++, color_prom.inc())
		{
			palette_set_color(i,(color_prom.read(0) & 0x0f) << 4,(color_prom.read(0) & 0xf0) >> 0,
					       (color_prom.read(0x800) & 0x0f) << 4);
	//		logerror("\t%d: r:%d g:%d b:%d\n",i,(color_prom[0] & 0x0f) << 4,(color_prom[0] & 0xf0) >> 0,
	//				       (color_prom[0x800] & 0x0f) << 4);
		}
	
		vb_bgprombank=bank;
	
	}
	
	public static void vb_spprombank_w( int bank )
	{
	
		int i;
	
		UBytePtr color_prom = new UBytePtr();
	
		if (bank==vb_spprombank) return;
	
		color_prom = new UBytePtr(memory_region(REGION_PROMS), 0x400 + bank*0x80);
	
		logerror("SPPROM Bank:%x, bank offset:%x\n",bank, 0x400 + bank*0x80);
	
		for (i=128;i<256;i++,color_prom.inc())
		{
			palette_set_color(i,(color_prom.read(0) & 0x0f) << 4,(color_prom.read(0) & 0xf0) >> 0,
					       (color_prom.read(0x800) & 0x0f) << 4);
		}
	
		vb_spprombank=bank;
	
	}
	
	public static ReadHandlerPtr vb_foreground_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return vb_videoram.read(offset);
	} };
	
	
	public static WriteHandlerPtr vb_foreground_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( vb_videoram.read(offset) != data ){
			vb_videoram.write(offset, data);
			dirtybuffer[offset] = 1;
		}
	} };
	
	
	public static ReadHandlerPtr vb_fgattrib_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return vb_fgattribram.read(offset);
	} };
	
	
	public static WriteHandlerPtr vb_fgattrib_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( vb_fgattribram.read(offset) != data ){
			vb_fgattribram.write(offset, data);
			dirtybuffer[offset] = 1;
		}
	} };
	
	
	public static ReadHandlerPtr vb_attrib_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return vb_attribram.read(offset);
	} };
	
	
	public static WriteHandlerPtr vb_attrib_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( vb_attribram.read(offset) != data ){
			vb_attribram.write(offset, data);
			dirtybuffer[offset] = 1;
		}
	} };
	
	static void vb_draw_foreground( mame_bitmap bitmap )
	{
		GfxElement gfx = Machine.gfx[0];
		UBytePtr source = new UBytePtr(vb_videoram);
		UBytePtr attrib_source = new UBytePtr(vb_fgattribram);
	
		int sx,sy;
	
		for( sy=0; sy<256; sy+=8 ){
			for( sx=0; sx<256; sx+=8 ){
				int attributes = attrib_source.read(0);
				int tile_number = source.read(0) + 256*( attributes & 0x1f );
				int color = ( attributes >> 5 ) & 0x7;
				if (tile_number != 0)
					drawgfx( bitmap,gfx, tile_number + (vball_gfxset!=0?0:8192),
					color,
					0,0, /* no flip */
					sx,sy,
					null, /* no need to clip */
					TRANSPARENCY_PEN,0);
	
				source.inc( 1 );
				attrib_source.inc( 1 );
			}
		}
	}
	
	public static void DRAW_SPRITE( mame_bitmap bitmap, GfxElement gfx, int which, int order, int color, int flipx, int flipy, int sx, int sy, rectangle clip ){
            drawgfx( bitmap, gfx, (which+order),color,flipx,flipy,sx,sy, clip,TRANSPARENCY_PEN,0);
        }
	
	static void draw_sprites( mame_bitmap bitmap )
	{
		rectangle clip = Machine.visible_area;
		GfxElement gfx = Machine.gfx[1];
		UBytePtr src;
		int i;
	
		src = new UBytePtr(spriteram);
	
	/*	240-Y    S|X|CLR|WCH WHICH    240-X
		xxxxxxxx x|x|xxx|xxx xxxxxxxx xxxxxxxx
	*/
	
	
		for (i = 0;i < spriteram_size[0];i += 4)
		{
			int attr = src.read(i+1);
			int which = src.read(i+2)+((attr & 0x07)<<8);
			int sx = ((src.read(i+3) + 8) & 0xff) - 8;
			int sy = 240 - src.read(i);
			int size = (attr & 0x80) >> 7;
			int color = (attr & 0x38) >> 3;
			int flipx = ~attr & 0x40;
			int flipy = 0;
			int dy = -16;
	
			switch (size)
			{
				case 0: /* normal */
                                    DRAW_SPRITE(bitmap, gfx, which, 0, color, flipx, flipy, sx, sy, clip);
				break;
	
				case 1: /* double y */
                                    DRAW_SPRITE(bitmap, gfx, which, 0, color, flipx, flipy,sx,sy + dy,clip);
                                    DRAW_SPRITE(bitmap, gfx, which, 1, color, flipx, flipy, sx, sy, clip);
				break;
			}
		}
	}
	
	
	static void vb_draw_background( mame_bitmap bitmap )
	{
		GfxElement gfx = Machine.gfx[0];
		UBytePtr source = new UBytePtr(videoram);
		UBytePtr attrib_source = new UBytePtr(vb_attribram);
	
		int scrollx = vb_scrollx_hi - vb_scrollx_lo.read(0) -4;
		int i,sx,sy;
	
		for( i=0; i < 1; i++){
			for( sy=0; sy<256; sy+=8 ){
				for( sx=0; sx<256; sx+=8 ){
					if ( dirtybuffer[source.offset - videoram.offset] != 0 ) {
						int attributes = attrib_source.read(0);
						int tile_number = source.read(0) + 256*( attributes & 0x1f );
						int color = ( attributes >> 5 ) & 0x7;
	
						drawgfx( tmpbitmap,gfx, tile_number + (vball_gfxset!=0?8192:0),
						color,
						0,0, /* no flip */
						sx,sy,
						null, /* no need to clip */
						TRANSPARENCY_NONE,0);
	
						dirtybuffer[source.offset - videoram.offset] = 0;
	
					}
	
					if ( dirtybuffer[source.offset + 0x400 - videoram.offset] != 0) {
						int attributes = attrib_source.read(0x400);
						int tile_number = source.read(0x400) + 256*( attributes & 0x1f );
						int color = ( attributes >> 5 ) & 0x7;
	
						drawgfx( tmpbitmap,gfx, tile_number + (vball_gfxset!=0?8192:0),
						color,
						0,0, /* no flip */
						sx+256,sy,
						null, /* no need to clip */
						TRANSPARENCY_NONE,0);
	
						dirtybuffer[source.offset + 0x400 - videoram.offset] = 0;
	
					}
	
	
					source.inc( 1 );
					attrib_source.inc( 1 );
				}
			}
		}
	
		copyscrollbitmap(bitmap,tmpbitmap,
				1,new int[]{scrollx},0,new int[]{0},
				Machine.visible_area,
				TRANSPARENCY_NONE,0);
	
	}
	
	
	public static VhUpdatePtr vb_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
	//	Tripping the sprite funk-tastic. :-) PaulH
	/*	static int i=0;
	
		i++;
		i%=60;
	
		vb_spprombank_w(i/15);
	*/
		vb_draw_background( bitmap );
		draw_sprites( bitmap );
	//	vb_draw_foreground( bitmap ); /* So far just hides half the game screen... */
	} };
	
}
