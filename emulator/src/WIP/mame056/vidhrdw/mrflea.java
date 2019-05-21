/******************************************************************

Mr. F. Lea
(C) 1983 PACIFIC NOVELTY MFG. INC.

******************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;

import static common.ptr.*;

import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.sound.mixer.*;
import static mame056.sound.mixerH.*;
import static mame056.inptport.*;
import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;


public class mrflea
{
	
	static int mrflea_gfx_bank;
	
	public static WriteHandlerPtr mrflea_gfx_bank_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		mrflea_gfx_bank = data;
		if(( data & ~0x14 ) != 0){
			logerror( "unknown gfx bank: 0x%02x\n", data );
		}
	} };
	
	public static WriteHandlerPtr mrflea_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		int bank = offset/0x400;
		offset &= 0x3ff;
		videoram.write(offset, data);
		videoram.write(offset+0x400, bank);
		/*	the address range that tile data is written to sets one bit of
		**	the bank select.  The remaining bits are from a video register.
		*/
	} };
	
	public static WriteHandlerPtr mrflea_spriteram_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		if(( offset&2 ) != 0){ /* tile_number */
			spriteram.write(offset|1, offset&1);
			offset &= ~1;
		}
		spriteram.write(offset, data);
	} };
	
	static void draw_sprites( mame_bitmap bitmap ){
		GfxElement gfx = Machine.gfx[0];
		UBytePtr source = new UBytePtr(spriteram);
		UBytePtr finish = new UBytePtr(source, 0x100);
		rectangle clip = Machine.visible_area;
		clip.max_x -= 24;
		clip.min_x += 16;
		while( (source.offset)<(finish.offset) ){
			int xpos = source.read(1)-3;
			int ypos = source.read(0)-16+3;
			int tile_number = source.read(2)+source.read(3)*0x100;
	
			drawgfx( bitmap, gfx,
				tile_number,
				0, /* color */
				0,0, /* no flip */
				xpos,ypos,
				clip,TRANSPARENCY_PEN,0 );
			drawgfx( bitmap, gfx,
				tile_number,
				0, /* color */
				0,0, /* no flip */
				xpos,256+ypos,
				clip,TRANSPARENCY_PEN,0 );
			source.inc(4);
		}
	}
	
	static void draw_background( mame_bitmap bitmap ){
		UBytePtr source = new UBytePtr(videoram);
		GfxElement gfx = Machine.gfx[1];
		int sx,sy;
		int base = 0;
		if( (mrflea_gfx_bank&0x04) != 0) base |= 0x400;
		if( (mrflea_gfx_bank&0x10) != 0 ) base |= 0x200;
		for( sy=0; sy<256; sy+=8 ){
			for( sx=0; sx<256; sx+=8 ){
				int tile_number = base+source.read(0)+source.read(0x400)*0x100;
				source.inc();
				drawgfx( bitmap, gfx,
					tile_number,
					0, /* color */
					0,0, /* no flip */
					sx,sy,
					null, /* no clip */
					TRANSPARENCY_NONE,0 );
			}
		}
	}
	
	public static VhStartPtr mrflea_vh_start = new VhStartPtr() { public int handler() {
		return 0;
	} };
	
	public static VhUpdatePtr mrflea_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		draw_background( bitmap );
		draw_sprites( bitmap );
	} };
}
