/*	video hardware for Pacific Novelty games:
**	Thief/Nato Defense
*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.libc.cstring.*;
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
import static mame056.palette.*;
// refactor
import static arcadeflex036.osdepend.logerror;

public class thief
{
	
	static mame_bitmap thief_page0;
	static mame_bitmap thief_page1;
	
	static int thief_read_mask, thief_write_mask;
	static int thief_video_control;
	
	public static class _thief_coprocessor {
		public UBytePtr context_ram = new UBytePtr();
		public int bank;
		public UBytePtr image_ram = new UBytePtr();
		public int[] param = new int[0x9];
	};
        
        public static _thief_coprocessor thief_coprocessor = new _thief_coprocessor();
	
	public static final int IMAGE_ADDR_LO=0;		//0xe000
	public static final int IMAGE_ADDR_HI=1;		//0xe001
	public static final int SCREEN_XPOS=2;		//0xe002
	public static final int SCREEN_YPOS=3;		//0xe003
	public static final int BLIT_WIDTH=4;			//0xe004
	public static final int BLIT_HEIGHT=5;		//0xe005
	public static final int GFX_PORT=6;			//0xe006
	public static final int BARL_PORT=7;			//0xe007
	public static final int BLIT_ATTRIBUTES=8;		//0xe008
	
	
	/***************************************************************************/
	
	public static ReadHandlerPtr thief_context_ram_r  = new ReadHandlerPtr() { public int handler(int offset){
		return thief_coprocessor.context_ram.read(0x40*thief_coprocessor.bank+offset);
	} };
	
	public static WriteHandlerPtr thief_context_ram_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		thief_coprocessor.context_ram.write(0x40*thief_coprocessor.bank+offset, data);
	} };
	
	public static WriteHandlerPtr thief_context_bank_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		thief_coprocessor.bank = data&0xf;
	} };
	
	/***************************************************************************/
	
	public static WriteHandlerPtr thief_video_control_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		if(( (data^thief_video_control)&1 ) != 0){
			/* screen flipped */
			memset( dirtybuffer, 0x00, 0x2000*2 );
		}
	
		thief_video_control = data;
	/*
		bit 0: screen flip
		bit 1: working page
		bit 2: visible page
		bit 3: mirrors bit 1
		bit 4: mirrors bit 2
	*/
	} };
	
	public static WriteHandlerPtr thief_vtcsel_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		/* TMS9927 VTAC registers */
	} };
	
	public static WriteHandlerPtr thief_color_map_w = new WriteHandlerPtr() {public void handler(int offset, int data){
	/*
		--xx----	blue
		----xx--	green
		------xx	red
	*/
		int intensity[] = {0x00,0x55,0xAA,0xFF};
		int r = intensity[(data & 0x03) >> 0];
                int g = intensity[(data & 0x0C) >> 2];
                int b = intensity[(data & 0x30) >> 4];
                
		palette_set_color( offset,r,g,b );
	} };
	
	/***************************************************************************/
	
	public static WriteHandlerPtr thief_color_plane_w = new WriteHandlerPtr() {public void handler(int offset, int data){
	/*
		--xx----	selects bitplane to read from (0..3)
		----xxxx	selects bitplane(s) to write to (0x0 = none, 0xf = all)
	*/
		thief_write_mask = data&0xf;
		thief_read_mask = (data>>4)&3;
	} };
	
	public static ReadHandlerPtr thief_videoram_r  = new ReadHandlerPtr() { public int handler(int offset){
		UBytePtr source = new UBytePtr(videoram, offset);
		if(( thief_video_control&0x02 )!=0) source.inc(0x2000*4); /* foreground/background */
		return source.read(thief_read_mask*0x2000);
	} };
	
	public static WriteHandlerPtr thief_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		UBytePtr dest = new UBytePtr(videoram, offset);
		if(( thief_video_control&0x02 ) != 0){
			dest.inc(0x2000*4); /* foreground/background */
			dirtybuffer[offset+0x2000] = 1;
		}
		else {
			dirtybuffer[offset] = 1;
		}
		if(( thief_write_mask&0x1 ) != 0) dest.write(0x2000*0, data);
		if(( thief_write_mask&0x2 ) != 0) dest.write(0x2000*1, data);
		if(( thief_write_mask&0x4 ) != 0) dest.write(0x2000*2, data);
		if(( thief_write_mask&0x8 ) != 0) dest.write(0x2000*3, data);
	} };
	
	/***************************************************************************/
	
	public static VhStopPtr thief_vh_stop = new VhStopPtr() { public void handler() {
		videoram = null;
		dirtybuffer = null;
		bitmap_free( thief_page1 );
		bitmap_free( thief_page0 );
		thief_coprocessor.context_ram = null;
		thief_coprocessor.image_ram = null;
	} };
	
	public static VhStartPtr thief_vh_start = new VhStartPtr() { public int handler() {
		//memset( thief_coprocessor, 0x00, sizeof(thief_coprocessor) );
                thief_coprocessor = new _thief_coprocessor();
	
		thief_page0	= bitmap_alloc( 256,256 );
		thief_page1	= bitmap_alloc( 256,256 );
		videoram = new UBytePtr( 0x2000*4*2 );
                for (int k=0 ; k<0x2000*4*2 ; k++)
                    videoram.write(k, 1);
                videoram.offset = 0;
                
		dirtybuffer = new char[ 0x2000*2 ];
	
		thief_coprocessor.image_ram = new UBytePtr( 0x2000 );
		thief_coprocessor.context_ram = new UBytePtr( 0x400 );
	
		if( thief_page0!=null && thief_page1!=null &&
			videoram!=null && dirtybuffer!=null &&
			thief_coprocessor.image_ram!=null &&
			thief_coprocessor.context_ram!=null )
		{
			memset( dirtybuffer, 1, 0x2000*2 );
			return 0;
		}
		thief_vh_stop.handler();
		return 1;
	} };
	
	public static VhUpdatePtr thief_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) {
		int offs;
		int flipscreen = thief_video_control&1;
		int[] pal_data = Machine.pens;
		UBytePtr dirty = new UBytePtr(dirtybuffer);
		UBytePtr source = new UBytePtr(videoram);
		mame_bitmap page;
	
		if(( thief_video_control&4 ) != 0){ /* visible page */
			dirty.inc( 0x2000 );
			source.inc( 0x2000*4 );
			page = thief_page1;
		}
		else {
			page = thief_page0;
		}
	
		for( offs=0; offs<0x2000; offs++ ){
			if( dirty.read(offs) != 0 ){
				int ypos = offs/32;
				int xpos = (offs%32)*8;
				int plane0 = source.read(0x2000*0+offs);
				int plane1 = source.read(0x2000*1+offs);
				int plane2 = source.read(0x2000*2+offs);
				int plane3 = source.read(0x2000*3+offs);
				int bit;
				if( flipscreen != 0 ){
					for( bit=0; bit<8; bit++ ){
						plot_pixel.handler( page, 0xff - (xpos+bit), 0xff - ypos,
							pal_data[
								(((plane0<<bit)&0x80)>>7) |
								(((plane1<<bit)&0x80)>>6) |
								(((plane2<<bit)&0x80)>>5) |
								(((plane3<<bit)&0x80)>>4)
							]
						);
					}
				}
				else {
					for( bit=0; bit<8; bit++ ){
						plot_pixel.handler( page, xpos+bit, ypos,
							pal_data[
								(((plane0<<bit)&0x80)>>7) |
								(((plane1<<bit)&0x80)>>6) |
								(((plane2<<bit)&0x80)>>5) |
								(((plane3<<bit)&0x80)>>4)
							]
						);
					}
				}
				dirty.write(offs, 0);
			}
		}
		copybitmap(bitmap,page,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	} };
	
	/***************************************************************************/
	
	static int fetch_image_addr(){
		int addr = thief_coprocessor.param[IMAGE_ADDR_LO]+256*thief_coprocessor.param[IMAGE_ADDR_HI];
		/* auto-increment */
		thief_coprocessor.param[IMAGE_ADDR_LO]++;
		if( thief_coprocessor.param[IMAGE_ADDR_LO]==0x00 ){
			thief_coprocessor.param[IMAGE_ADDR_HI]++;
		}
		return addr;
	}
	
	public static WriteHandlerPtr thief_blit_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		int i, offs, xoffset, dy;
		UBytePtr gfx_rom = new UBytePtr(memory_region( REGION_GFX1 ));
		int x = thief_coprocessor.param[SCREEN_XPOS];
		int y = thief_coprocessor.param[SCREEN_YPOS];
		int width = thief_coprocessor.param[BLIT_WIDTH];
		int height = thief_coprocessor.param[BLIT_HEIGHT];
		int attributes = thief_coprocessor.param[BLIT_ATTRIBUTES];
	
		int old_data;
		int xor_blit = data;
			/* making the xor behavior selectable fixes score display,
			but causes minor glitches on the playfield */
	
		x -= width*8;
		xoffset = x&7;
	
		if(( attributes&0x10 ) != 0 ){
			y += 7-height;
			dy = 1;
		}
		else {
			dy = -1;
		}
		height++;
		while( height-- !=0){
			for( i=0; i<=width; i++ ){
				int addr = fetch_image_addr();
				if( addr<0x2000 ){
					data = thief_coprocessor.image_ram.read(addr);
				}
				else {
					addr -= 0x2000;
					if(( addr<0x2000*3 )) data = gfx_rom.read(addr);
				}
				offs = (y*32+x/8+i)&0x1fff;
				old_data = thief_videoram_r.handler(offs );
				if( xor_blit != 0 ){
					thief_videoram_w.handler(offs, old_data^(data>>xoffset) );
				}
				else {
					thief_videoram_w.handler(offs,
						(old_data&(0xff00>>xoffset)) | (data>>xoffset)
					);
				}
				offs = (offs+1)&0x1fff;
				old_data = thief_videoram_r.handler(offs );
				if( xor_blit != 0 ){
					thief_videoram_w.handler(offs, old_data^((data<<(8-xoffset))&0xff) );
				}
				else {
					thief_videoram_w.handler(offs,
						(old_data&(0xff>>xoffset)) | ((data<<(8-xoffset))&0xff)
					);
				}
			}
			y+=dy;
		}
	} };
	
	public static ReadHandlerPtr thief_coprocessor_r  = new ReadHandlerPtr() { public int handler(int offset){
		switch( offset ){
	 	case SCREEN_XPOS: /* xpos */
		case SCREEN_YPOS: /* ypos */
			{
		 	/* XLAT: given (x,y) coordinate, return byte address in videoram */
				int addr = thief_coprocessor.param[SCREEN_XPOS]+256*thief_coprocessor.param[SCREEN_YPOS];
				int result = 0xc000 | (addr>>3);
				return (offset==0x03)?(result>>8):(result&0xff);
			}
			//break;
	
		case GFX_PORT:
			{
				int addr = fetch_image_addr();
				if( addr<0x2000 ){
					return thief_coprocessor.image_ram.read(addr);
				}
				else {
					UBytePtr gfx_rom = new UBytePtr(memory_region( REGION_GFX1 ));
					addr -= 0x2000;
					if( addr<0x6000 ) return gfx_rom.read(addr);
				}
			}
			break;
	
		case BARL_PORT:
			{
				/* return bitmask for addressed pixel */
				int dx = thief_coprocessor.param[SCREEN_XPOS]&0x7;
				if(( thief_coprocessor.param[BLIT_ATTRIBUTES]&0x01 )!=0){
					return 0x01<<dx; // flipx
				}
				else {
					return 0x80>>dx; // no flip
				}
			}
			//break;
		}
	
		return thief_coprocessor.param[offset];
	} };
	
	public static WriteHandlerPtr thief_coprocessor_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		switch( offset ){
		case GFX_PORT:
			{
				int addr = fetch_image_addr();
				if( addr<0x2000 ){
					thief_coprocessor.image_ram.write(addr, data);
				}
			}
			break;
	
		default:
			thief_coprocessor.param[offset] = data;
			break;
		}
	} };
}
