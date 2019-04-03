/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;

import static common.ptr.*;
import common.subArrays.IntArray;
import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class kchamp
{
	
	/* prototypes */
	/*TODO*///void kchamp_vs_drawsprites( struct mame_bitmap *bitmap );
	/*TODO*///void kchamp_1p_drawsprites( struct mame_bitmap *bitmap );
	
	
	
	/*TODO*///typedef void (*kchamp_drawspritesproc)( struct mame_bitmap * );
	
	//static kchamp_drawspritesproc kchamp_drawsprites;
        static int kchamp_drawsprites_method = 0;
        static mame_bitmap[] kchamp_drawsprites;
        
        static final int kchamp_vs_drawsprites_method = 0;
        static final int kchamp_1p_drawsprites_method = 1;
	
	
	/***************************************************************************
	  Video hardware start.
	***************************************************************************/
	
	public static VhStartPtr kchampvs_vh_start = new VhStartPtr() { public int handler()  {
	
		kchamp_drawsprites_method = kchamp_vs_drawsprites_method;
	
		return generic_vh_start.handler();
	} };
	
	public static VhStartPtr kchamp1p_vh_start = new VhStartPtr() { public int handler()  {
            
            System.out.println("kchamp1p_vh_start");
	
		kchamp_drawsprites_method = kchamp_1p_drawsprites_method;
	
		return generic_vh_start.handler();
	} };
	
	/***************************************************************************
	  Convert color prom.
	***************************************************************************/
        public static int TOTAL_COLORS(int gfxn){
            return Machine.gfx[gfxn].total_colors * Machine.gfx[gfxn].color_granularity;
        };
	        
        public static void COLOR(char[] colortable, int gfxn, int offs, int value){
            colortable[Machine.drv.gfxdecodeinfo[gfxn].color_codes_start + offs] = (char) value;
        };
        
	public static VhConvertColorPromPtr kchamp_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                System.out.println("kchamp_vh_convert_color_prom");
                int i, red, green, blue;
	        int _palPos = 0;
                int _colTabPos = 0;
                
                System.out.println("Total Colors: "+Machine.drv.total_colors);
	
		for (i = 0;i < Machine.drv.total_colors;i++)
		{
	                red = color_prom.read(i);
	                green = color_prom.read(Machine.drv.total_colors+i);
	                blue = color_prom.read(2*Machine.drv.total_colors+i);
	
	
	                palette[_palPos++] = (char) (red*0x11);
	                palette[_palPos++] = (char) (green*0x11);
	                palette[_palPos++] = (char) (blue*0x11);
                        
                        System.out.println("Colors["+i+"]: RED "+red*0x11+" GREEN "+green*0x11+" BLUE "+blue*0x11);
	                colortable[_colTabPos++] = (char) i;
                        
                        palette_set_color(i,red*0x11,green*0x11,blue*0x11);
		}
                
                palette[0]=0x00;
                palette[1]=0x00;
                palette[2]=0x00;
                
                palette[3]=0xFF;
                palette[4]=0xFF;
                palette[5]=0xFF;
            }
        };
	
	public static void kchamp_vs_drawsprites( mame_bitmap bitmap ) {
	
		int offs;
		        /*
	                Sprites
	                -------
	                Offset          Encoding
	                  0             YYYYYYYY
	                  1             TTTTTTTT
	                  2             FGGTCCCC
	                  3             XXXXXXXX
	        */
	
	        for (offs = 0 ;offs < 0x100;offs+=4)
		{
	                int numtile = spriteram.read(offs+1) + ( ( spriteram.read(offs+2) & 0x10 ) << 4 );
	                int flipx = ( spriteram.read(offs+2) & 0x80 );
	                int sx, sy;
	                int gfx = 1 + ( ( spriteram.read(offs+2) & 0x60 ) >> 5 );
	                int color = ( spriteram.read(offs+2) & 0x0f );
	
	                sx = spriteram.read(offs+3);
	                sy = 240 - spriteram.read(offs);
	
	                drawgfx(bitmap,Machine.gfx[gfx],
	                                numtile,
	                                color,
	                                0, flipx,
	                                sx,sy,
	                                Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	}
	
	public static void kchamp_1p_drawsprites( mame_bitmap bitmap ) {
	
		int offs;
		        /*
	                Sprites
	                -------
	                Offset          Encoding
	                  0             YYYYYYYY
	                  1             TTTTTTTT
	                  2             FGGTCCCC
	                  3             XXXXXXXX
	        */
	
	        for (offs = 0 ;offs < 0x100;offs+=4)
		{
	                int numtile = spriteram.read(offs+1) + ( ( spriteram.read(offs+2) & 0x10 ) << 4 );
	                int flipx = ( spriteram.read(offs+2) & 0x80 );
	                int sx, sy;
	                int gfx = 1 + ( ( spriteram.read(offs+2) & 0x60 ) >> 5 );
	                int color = ( spriteram.read(offs+2) & 0x0f );
	
	                sx = spriteram.read(offs+3) - 8;
	                sy = 247 - spriteram.read(offs);
	
	                drawgfx(bitmap,Machine.gfx[gfx],
	                                numtile,
	                                color,
	                                0, flipx,
	                                sx,sy,
	                                Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	}
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	
	public static VhUpdatePtr kchamp_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
	        int offs;
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
	        for ( offs = videoram_size[0] - 1; offs >= 0; offs-- ) {
	                if ( dirtybuffer[offs] != 0 ) {
				int sx,sy,code;
	
				dirtybuffer[offs] = 0;
	
	                        sx = (offs % 32);
				sy = (offs / 32);
	
	                        code = videoram.read(offs) + ( ( colorram.read(offs) & 7 ) << 8 );
	
	                        drawgfx(tmpbitmap,Machine.gfx[0],
	                                        code,
	                                        ( colorram.read(offs) >> 3 ) & 0x1f,
	                                        0, /* flip x */
	                                        0, /* flip y */
						sx*8,sy*8,
						Machine.visible_area,TRANSPARENCY_NONE,0);
			}
		}
	
		/* copy the character mapped graphics */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
	
		kchamp_drawsprites(bitmap);
	}}; 

        public static void kchamp_drawsprites(mame_bitmap bitmap) {
            switch (kchamp_drawsprites_method) {
                case kchamp_vs_drawsprites_method:
                    kchamp_vs_drawsprites(bitmap);
                break;
                
                case kchamp_1p_drawsprites_method:
                    kchamp_1p_drawsprites(bitmap);
                break;
            }
        }
   
};
