/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/


/*  Stuff that work only in MS DOS (Color cycling)
 */

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.vidhrdw;

import static mame056.usrintrf.*;
import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;

import static common.ptr.*;
import common.subArrays.IntArray;
import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.memoryH.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.cpuexec.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.inptport.readinputport;


public class tutankhm
{
	
	
	public static UBytePtr tutankhm_scrollx = new UBytePtr();
	
	
	
	static void videowrite(int offset,int data)
	{
		int x1,x2,y1,y2;
	
	
		x1 = ( offset & 0x7f ) << 1;
		y1 = ( offset >> 7 );
		x2 = x1 + 1;
		y2 = y1;
	
		if (flip_screen_x[0] != 0)
		{
			x1 = 255 - x1;
			x2 = 255 - x2;
		}
		if (flip_screen_y[0] != 0)
		{
			y1 = 255 - y1;
			y2 = 255 - y2;
		}
	
		plot_pixel.handler(tmpbitmap,x1,y1,Machine.pens[data & 0x0f]);
		plot_pixel.handler(tmpbitmap,x2,y2,Machine.pens[data >> 4]);
	}
	
	
	
	public static WriteHandlerPtr tutankhm_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		videoram.write(offset, data);
		videowrite(offset,data);
	} };
	
	
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	public static VhUpdatePtr tutankhm_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		if (full_refresh != 0)
		{
			int offs;

                    for (offs = 0; offs < videoram_size[0]; offs++) {
                        tutankhm_videoram_w.handler(offs, videoram.read(offs));
                    }
		}
	
		/* copy the temporary bitmap to the screen */
		{
			int[] scroll = new int[32];
                int i;

                if (flip_screen_x[0] != 0) {
                    for (i = 0; i < 8; i++) {
                        scroll[i] = 0;
                    }
                    for (i = 8; i < 32; i++) {
                        scroll[i] = -tutankhm_scrollx.read();
                        if (flip_screen_y[0] != 0) {
                            scroll[i] = -scroll[i];
                        }
                    }
                } else {
                    for (i = 0; i < 24; i++) {
                        scroll[i] = -tutankhm_scrollx.read();
                        if (flip_screen_y[0] != 0) {
                            scroll[i] = -scroll[i];
                        }
                    }
                    for (i = 24; i < 32; i++) {
                        scroll[i] = 0;
                    }
                }

                copyscrollbitmap(bitmap, tmpbitmap, 0, null, 32, scroll, Machine.visible_area, TRANSPARENCY_NONE, 0);
		}
	} };
	
	
	
	/* Juno First Blitter Hardware emulation
	
		Juno First can blit a 16x16 graphics which comes from un-memory mapped graphics roms
	
		$8070.$8071 specifies the destination NIBBLE address
		$8072.$8073 specifies the source NIBBLE address
	
		Depending on bit 0 of the source address either the source pixels will be copied to
		the destination address, or a zero will be written.
		This allows the game to quickly clear the sprites from the screen
	
		A lookup table is used to swap the source nibbles as they are the wrong way round in the
		source data.
	
		Bugs -
	
			Currently only the even pixels will be written to. This is to speed up the blit routine
			as it does not have to worry about shifting the source data.
			This means that all destination X values will be rounded to even values.
			In practice no one actaully notices this.
	
			The clear works properly.
	*/
        
        static UBytePtr blitterdata = new UBytePtr(1024);
        
        public static void JUNOBLITPIXEL(UBytePtr JunoBLTRom, int x, long srcaddress, long destaddress){
		if (JunoBLTRom.read((int) (srcaddress+x)) !=0)
			tutankhm_videoram_w.handler((int) (destaddress+x),					
				((JunoBLTRom.read((int) (srcaddress+x)) & 0xf0) >> 4)		
				| ((JunoBLTRom.read((int) (srcaddress+x)) & 0x0f) << 4));
        }
        
        public static void JUNOCLEARPIXEL(UBytePtr JunoBLTRom, int x, int srcaddress, int destaddress){
		if ((JunoBLTRom.read( (srcaddress+x)) & 0xF0) != 0)
			tutankhm_videoram_w.handler( (destaddress+x),		
				videoram.read( (destaddress+x)) & 0xF0);	
		if ((JunoBLTRom.read((int) (srcaddress+x)) & 0x0F) != 0)
			tutankhm_videoram_w.handler( (destaddress+x),		
				videoram.read( (destaddress+x)) & 0x0F);
        }
	
	public static WriteHandlerPtr junofrst_blitter_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
	
	
		blitterdata.write(offset, (char)(data&0xFF));
	
		/* Blitter is triggered by $8073 */
		if (offset==3)
		{
			int i;
			/*unsigned*/ int srcaddress;
			/*unsigned*/ int destaddress;
			/*unsigned*/ int srcflag;
			/*unsigned*/ int destflag;
			UBytePtr JunoBLTRom = new UBytePtr(memory_region(REGION_GFX1));
	
			srcaddress = (blitterdata.read(0x2)<<8) | (blitterdata.read(0x3));
			srcflag = (srcaddress & 1);
			srcaddress >>= 1;
			srcaddress &= 0x7FFE;
			destaddress = (blitterdata.read(0x0)<<8)  | (blitterdata.read(0x1));
	
			destflag = (destaddress & 1);
	
			destaddress >>= 1;
			destaddress &= 0x7fff;
	
			if (srcflag != 0) {
				for (i=0;i<16;i++) {
										
		if (JunoBLTRom.read(srcaddress+0)!=0)							
			tutankhm_videoram_w.handler(destaddress+0,((JunoBLTRom.read(srcaddress+0) & 0xf0) >> 4)| ((JunoBLTRom.read(srcaddress+0) & 0x0f) << 4));
		if (JunoBLTRom.read(srcaddress+1)!=0)							
			tutankhm_videoram_w.handler(destaddress+1,((JunoBLTRom.read(srcaddress+1) & 0xf0) >> 4)| ((JunoBLTRom.read(srcaddress+1) & 0x0f) << 4));
		if (JunoBLTRom.read(srcaddress+2)!=0)							
			tutankhm_videoram_w.handler(destaddress+2,((JunoBLTRom.read(srcaddress+2) & 0xf0) >> 4)| ((JunoBLTRom.read(srcaddress+2) & 0x0f) << 4));
		if (JunoBLTRom.read(srcaddress+3)!=0)							
			tutankhm_videoram_w.handler(destaddress+3,((JunoBLTRom.read(srcaddress+3) & 0xf0) >> 4)| ((JunoBLTRom.read(srcaddress+3) & 0x0f) << 4));
		if (JunoBLTRom.read(srcaddress+4)!=0)							
			tutankhm_videoram_w.handler(destaddress+4,((JunoBLTRom.read(srcaddress+4) & 0xf0) >> 4)| ((JunoBLTRom.read(srcaddress+4) & 0x0f) << 4));
		if (JunoBLTRom.read(srcaddress+5)!=0)							
			tutankhm_videoram_w.handler(destaddress+5,((JunoBLTRom.read(srcaddress+5) & 0xf0) >> 4)| ((JunoBLTRom.read(srcaddress+5) & 0x0f) << 4));
		if (JunoBLTRom.read(srcaddress+6)!=0)							
			tutankhm_videoram_w.handler(destaddress+6,((JunoBLTRom.read(srcaddress+6) & 0xf0) >> 4)| ((JunoBLTRom.read(srcaddress+6) & 0x0f) << 4));
		if (JunoBLTRom.read(srcaddress+7)!=0)							
			tutankhm_videoram_w.handler(destaddress+7,((JunoBLTRom.read(srcaddress+7) & 0xf0) >> 4)| ((JunoBLTRom.read(srcaddress+7) & 0x0f) << 4));							
					destaddress += 128;
					srcaddress += 8;
				}
			} else {
				for (i=0;i<16;i++) {
					
		if ((JunoBLTRom.read(srcaddress+0) & 0xF0)!=0) 		
			tutankhm_videoram_w.handler(destaddress+0,videoram.read(destaddress+0) & 0xF0);	
		if ((JunoBLTRom.read(srcaddress+0) & 0x0F)!=0)		
			tutankhm_videoram_w.handler(destaddress+0,videoram.read(destaddress+0) & 0x0F);
		if ((JunoBLTRom.read(srcaddress+1) & 0xF0)!=0) 		
			tutankhm_videoram_w.handler(destaddress+1,videoram.read(destaddress+1) & 0xF0);	
		if ((JunoBLTRom.read(srcaddress+1) & 0x0F)!=0)		
			tutankhm_videoram_w.handler(destaddress+1,videoram.read(destaddress+1) & 0x0F);
		if ((JunoBLTRom.read(srcaddress+2) & 0xF0)!=0) 		
			tutankhm_videoram_w.handler(destaddress+2,videoram.read(destaddress+2) & 0xF0);	
		if ((JunoBLTRom.read(srcaddress+2) & 0x0F)!=0)		
			tutankhm_videoram_w.handler(destaddress+2,videoram.read(destaddress+2) & 0x0F);
		if ((JunoBLTRom.read(srcaddress+3) & 0xF0)!=0) 		
			tutankhm_videoram_w.handler(destaddress+3,videoram.read(destaddress+3) & 0xF0);	
		if ((JunoBLTRom.read(srcaddress+3) & 0x0F)!=0)		
			tutankhm_videoram_w.handler(destaddress+3,videoram.read(destaddress+3) & 0x0F);
		if ((JunoBLTRom.read(srcaddress+4) & 0xF0)!=0) 		
			tutankhm_videoram_w.handler(destaddress+4,videoram.read(destaddress+4) & 0xF0);	
		if ((JunoBLTRom.read(srcaddress+4) & 0x0F)!=0)		
			tutankhm_videoram_w.handler(destaddress+4,videoram.read(destaddress+4) & 0x0F);
		if ((JunoBLTRom.read(srcaddress+5) & 0xF0)!=0) 		
			tutankhm_videoram_w.handler(destaddress+5,videoram.read(destaddress+5) & 0xF0);	
		if ((JunoBLTRom.read(srcaddress+5) & 0x0F)!=0)		
			tutankhm_videoram_w.handler(destaddress+5,videoram.read(destaddress+5) & 0x0F);
		if ((JunoBLTRom.read(srcaddress+6) & 0xF0)!=0) 		
			tutankhm_videoram_w.handler(destaddress+6,videoram.read(destaddress+6) & 0xF0);	
		if ((JunoBLTRom.read(srcaddress+6) & 0x0F)!=0)		
			tutankhm_videoram_w.handler(destaddress+6,videoram.read(destaddress+6) & 0x0F);
		if ((JunoBLTRom.read(srcaddress+7) & 0xF0)!=0) 		
			tutankhm_videoram_w.handler(destaddress+7,videoram.read(destaddress+7) & 0xF0);	
		if ((JunoBLTRom.read(srcaddress+7) & 0x0F)!=0)		
			tutankhm_videoram_w.handler(destaddress+7,videoram.read(destaddress+7) & 0x0F);
					destaddress += 128;
					srcaddress+= 8;
				}
			}
		}
        } };
}
