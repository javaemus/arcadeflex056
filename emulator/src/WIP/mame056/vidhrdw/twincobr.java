/***************************************************************************
  vidhrdw.c

  Functions to emulate the video hardware of these machines.
  Video is 30x40 tiles. (200x320 for Twin Cobra/Flying shark)
  Video is 40x30 tiles. (320x200 for Wardner)

  Video has 3 scrolling tile layers (Background, Foreground and Text) and
  Sprites that have 4 (5?) priorities. Lowest priority is "Off".
  Wardner has an unusual sprite priority in the shop scenes, whereby a
  middle level priority Sprite appears over a high priority Sprite ?

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.vidhrdw.generic.*;
import static common.libc.cstring.*;
// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.mame.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;

public class twincobr {

    
	static UBytePtr twincobr_bgvideoram16 = new UBytePtr();
	static UBytePtr twincobr_fgvideoram16 = new UBytePtr();
	
	public static int wardner_sprite_hack = 0;	/* Required for weird sprite priority in wardner  */
									/* when hero is in shop. Hero should cover shop owner */
	
	
	static int[] twincobr_bgvideoram_size=new int[2],twincobr_fgvideoram_size=new int[2];
	public static int txscrollx = 0;
	public static int txscrolly = 0;
	static int fgscrollx = 0;
	static int fgscrolly = 0;
	static int bgscrollx = 0;
	static int bgscrolly = 0;
	public static int twincobr_fg_rom_bank = 0;
	public static int twincobr_bg_ram_bank = 0;
	public static int twincobr_display_on = 1;
	public static int twincobr_flip_screen = 0;
	public static int twincobr_flip_x_base = 0x37;	/* value to 0 the X scroll offsets (non-flip) */
	public static int twincobr_flip_y_base = 0x1e;	/* value to 0 the Y scroll offsets (non-flip) */
	
	public static int txoffs = 0;
	public static int bgoffs = 0;
	public static int fgoffs = 0;
	static int scroll_x = 0;
	static int scroll_y = 0;
	
	static int vidbaseaddr = 0;
	static int scroll_realign_x = 0;
	
	/************************* Wardner variables *******************************/
	
	public static VhStartPtr toaplan0_vh_start = new VhStartPtr() { public int handler() 
	{
		/* the video RAM is accessed via ports, it's not memory mapped */
		videoram_size[0] = 0x800;
		twincobr_bgvideoram_size[0] = 0x2000;	/* banked two times 0x1000 */
		twincobr_fgvideoram_size[0] = 0x1000;
                
                //buffer_spriteram16_w.handler(0, 0, 0);
                
	
		if ((videoram16 = new UBytePtr(videoram_size[0]*2)) == null)
			return 1;
		
                memset(videoram16,0,videoram_size[0]*2);
	
		if ((twincobr_fgvideoram16 = new UBytePtr(twincobr_fgvideoram_size[0]*2)) == null)
		{
                        videoram16 = null;
			return 1;
		}
		memset(twincobr_fgvideoram16,0,twincobr_fgvideoram_size[0]*2);
	
		if ((twincobr_bgvideoram16 = new UBytePtr(twincobr_bgvideoram_size[0]*2)) == null)
		{
			twincobr_fgvideoram16 = null;
			videoram16 = null;
			return 1;
		}
		memset(twincobr_bgvideoram16,0,twincobr_bgvideoram_size[0]*2);
	
		if ((dirtybuffer = new char[twincobr_bgvideoram_size[0]*2]) == null)
		{
			twincobr_bgvideoram16 = null;
			twincobr_fgvideoram16 = null;
			videoram16 = null;
			return 1;
		}
		memset(dirtybuffer,1,twincobr_bgvideoram_size[0]*2);
	
		if ((tmpbitmap = bitmap_alloc(Machine.drv.screen_width,2*Machine.drv.screen_height)) == null)
		{
			dirtybuffer = null;
			twincobr_bgvideoram16 = null;
			twincobr_fgvideoram16 = null;
			videoram16 = null;
			return 1;
		}
	
		return 0;
	} };
	
	public static VhStopPtr toaplan0_vh_stop = new VhStopPtr() { public void handler() 
	{
                tmpbitmap = null;
		dirtybuffer = null;
		twincobr_bgvideoram16 = null;
		twincobr_fgvideoram16 = null;
		videoram16 = null;
	} };
	
	
	
/*TODO*///	WRITE16_HANDLER( twincobr_crtc_reg_sel_w )
/*TODO*///	{
/*TODO*///		crtc6845_address_w(offset, data);
/*TODO*///	}
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( twincobr_crtc_data_w )
/*TODO*///	{
/*TODO*///		crtc6845_register_w(offset, data);
/*TODO*///	}
/*TODO*///	
	public static WriteHandlerPtr16 twincobr_txoffs_w = new WriteHandlerPtr16() {
            public void handler(int offset, int data, int d2) {
                /*TODO*///COMBINE_DATA(&txoffs);
		txoffs %= videoram_size[0];
            }
        };
	
	public static ReadHandlerPtr twincobr_txram_r = new ReadHandlerPtr() {
            public int handler(int offs) {
                return videoram16.read(txoffs);
            }
        };
	
	public static WriteHandlerPtr16 twincobr_txram_w = new WriteHandlerPtr16() {
            public void handler(int offset, int data, int d2) {
		/*TODO*///COMBINE_DATA(&videoram16[txoffs]);
            }
        };
	
	public static WriteHandlerPtr16 twincobr_bgoffs_w = new WriteHandlerPtr16() {
            public void handler(int offset, int data, int d2) {
                /*TODO*///COMBINE_DATA(&bgoffs);
		bgoffs %= (twincobr_bgvideoram_size[0] >> 1);
            }
        };
	
	public static ReadHandlerPtr twincobr_bgram_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return twincobr_bgvideoram16.read(bgoffs+twincobr_bg_ram_bank);
            }
        };
	
	public static WriteHandlerPtr16 twincobr_bgram_w = new WriteHandlerPtr16() {
            public void handler(int offset, int data, int d2) {
		/*TODO*///COMBINE_DATA(&twincobr_bgvideoram16[bgoffs+twincobr_bg_ram_bank]);
		dirtybuffer[bgoffs] = 1;
            }
        };
	
	public static WriteHandlerPtr16 twincobr_fgoffs_w = new WriteHandlerPtr16() {
            public void handler(int offset, int data, int d2) {
                /*TODO*///COMBINE_DATA(&fgoffs);
		fgoffs %= twincobr_fgvideoram_size[0];
            }
        };
	{
		
	}
	public static ReadHandlerPtr twincobr_fgram_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return twincobr_fgvideoram16.read(fgoffs);
            }
        };
	
	public static WriteHandlerPtr16 twincobr_fgram_w = new WriteHandlerPtr16() {
            public void handler(int offset, int data, int d2) {
		/*TODO*///COMBINE_DATA(&twincobr_fgvideoram16[fgoffs]);
            }
        };
	
	public static WriteHandlerPtr16 twincobr_txscroll_w = new WriteHandlerPtr16() {
            public void handler(int offset, int data, int d2) {
                /*TODO*///if (offset == 0) COMBINE_DATA(txscrollx);
		/*TODO*///else COMBINE_DATA(txscrolly);
            }
        };
	
	
	public static WriteHandlerPtr16 twincobr_bgscroll_w = new WriteHandlerPtr16() {
            public void handler(int offset, int data, int d2) {
                /*TODO*///if (offset == 0) COMBINE_DATA(&bgscrollx);
		/*TODO*///else COMBINE_DATA(&bgscrolly);
            }
        };
	
	public static WriteHandlerPtr16 twincobr_fgscroll_w = new WriteHandlerPtr16() {
            public void handler(int offset, int data, int d2) {
                /*TODO*///if (offset == 0) COMBINE_DATA(&fgscrollx);
		/*TODO*///else COMBINE_DATA(&fgscrolly);
            }
        };
	
/*TODO*///	WRITE16_HANDLER( twincobr_exscroll_w )	/* Extra unused video layer */
/*TODO*///	{
/*TODO*///		if (offset == 0) logerror("PC - write %04x to unknown video scroll Y register\n",data);
/*TODO*///		else logerror("PC - write %04x to unknown video scroll X register\n",data);
/*TODO*///	}
	
	/******************** Wardner interface to this hardware ********************/
	public static WriteHandlerPtr wardner_txlayer_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int shift = 8 * (offset & 1);
		twincobr_txoffs_w.handler(offset / 2, data << shift, 0xff00 >> shift);
	} };
	public static WriteHandlerPtr wardner_bglayer_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int shift = 8 * (offset & 1);
		twincobr_bgoffs_w.handler(offset / 2, data << shift, 0xff00 >> shift);
	} };
	public static WriteHandlerPtr wardner_fglayer_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int shift = 8 * (offset & 1);
		twincobr_fgoffs_w.handler(offset / 2, data << shift, 0xff00 >> shift);
	} };
	
	public static WriteHandlerPtr wardner_txscroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int shift = 8 * (offset & 1);
		twincobr_txscroll_w.handler(offset / 2, data << shift, 0xff00 >> shift);
	} };
	public static WriteHandlerPtr wardner_bgscroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int shift = 8 * (offset & 1);
		twincobr_bgscroll_w.handler(offset / 2, data << shift, 0xff00 >> shift);
	} };
	public static WriteHandlerPtr wardner_fgscroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int shift = 8 * (offset & 1);
		twincobr_fgscroll_w.handler(offset / 2, data << shift, 0xff00 >> shift);
	} };
	
	public static WriteHandlerPtr wardner_exscroll_w = new WriteHandlerPtr() {public void handler(int offset, int data)	/* Extra unused video layer */
	{
		if (offset == 0) logerror("PC - write %04x to unknown video scroll Y register\n",data);
		else logerror("PC - write %04x to unknown video scroll X register\n",data);
	} };
	
	public static ReadHandlerPtr wardner_videoram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int shift = 8 * (offset & 1);
		switch (offset/2) {
			case 0: return twincobr_txram_r.handler(0) >> shift; //break;
			case 1: return twincobr_bgram_r.handler(0) >> shift; //break;
			case 2: return twincobr_fgram_r.handler(0) >> shift; //break;
		}
		return 0;
	} };
	
	public static WriteHandlerPtr wardner_videoram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int shift = 8 * (offset & 1);
		switch (offset/2) {
			case 0: twincobr_txram_w.handler(0,data << shift, 0xff00 >> shift); break;
			case 1: twincobr_bgram_w.handler(0,data << shift, 0xff00 >> shift); break;
			case 2: twincobr_fgram_w.handler(0,data << shift, 0xff00 >> shift); break;
		}
	} };
	
	static void twincobr_draw_sprites(mame_bitmap bitmap, int priority)
	{
		int offs;
	
		for (offs = 0;offs < spriteram_size[0]/2;offs += 4)
		{
			int attribute,sx,sy,flipx,flipy;
			int sprite, color;
	
			attribute = buffered_spriteram16.read(offs + 1);
			if ((attribute & 0x0c00) == priority) {	/* low priority */
				sy = buffered_spriteram16.read(offs + 3) >> 7;
				if (sy != 0x0100) {		/* sx = 0x01a0 or 0x0040*/
					sprite = buffered_spriteram16.read(offs) & 0x7ff;
					color  = attribute & 0x3f;
					sx = buffered_spriteram16.read(offs + 2) >> 7;
					flipx = attribute & 0x100;
					if (flipx != 0) sx -= 14;		/* should really be 15 */
					flipy = attribute & 0x200;
					drawgfx(bitmap,Machine.gfx[3],
						sprite,
						color,
						flipx,flipy,
						sx-32,sy-16,
						Machine.visible_area,TRANSPARENCY_PEN,0);
				}
			}
		}
	}
	
	static int offs,code,tile,color;
	
	public static VhUpdatePtr toaplan0_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		
	
		if (twincobr_display_on == 0)
		{
			fillbitmap(bitmap,Machine.pens[0],Machine.visible_area);
			return;
		}
	
	
		/* draw the background */
		for (offs = twincobr_bgvideoram_size[0] - 1;offs >= 0;offs--)
		{
			if (dirtybuffer[offs] != 0)
			{
				int sx,sy;
	
				dirtybuffer[offs] = 0;
	
				sx = offs % 64;
				sy = offs / 64;
	
				code = twincobr_bgvideoram16.read(offs+twincobr_bg_ram_bank);
				tile  = (code & 0x0fff);
				color = (code & 0xf000) >> 12;
				if (twincobr_flip_screen != 0) { sx=63-sx; sy=63-sy; }
				drawgfx(tmpbitmap,Machine.gfx[2],
					tile,
					color,
					twincobr_flip_screen,twincobr_flip_screen,
					8*sx,8*sy,
					null,TRANSPARENCY_NONE,0);
			}
		}
	
		/* copy the background graphics */
		{
			if (twincobr_flip_screen != 0) {
				scroll_x = (twincobr_flip_x_base + bgscrollx + 0x141) & 0x1ff;
				scroll_y = (twincobr_flip_y_base + bgscrolly + 0xf1) & 0x1ff;
			}
			else {
				scroll_x = (0x1c9 - bgscrollx) & 0x1ff;
				scroll_y = (- 0x1e - bgscrolly) & 0x1ff;
			}
			copyscrollbitmap(bitmap,tmpbitmap,1,new int[]{scroll_x},1,new int[]{scroll_y},Machine.visible_area,TRANSPARENCY_NONE,0);
		}
	
	
		/* draw the sprites in low priority (Twin Cobra tanks under roofs) */
		twincobr_draw_sprites (bitmap, 0x0400);
	
		/* draw the foreground */
		scroll_x = (twincobr_flip_x_base + fgscrollx) & 0x01ff;
		scroll_y = (twincobr_flip_y_base + fgscrolly) & 0x01ff;
		vidbaseaddr = ((scroll_y>>3)*64) + (scroll_x>>3);
		scroll_realign_x = scroll_x >> 3;		/* realign video ram pointer */
		for (offs = (31*41)-1; offs >= 0; offs-- )
		{
			int xpos,ypos;
			int sx,sy;
			int vidramaddr = 0;
	
			sx = offs % 41;
			sy = offs / 41;
	
			vidramaddr = vidbaseaddr + (sy*64) + sx;
			if ((scroll_realign_x + sx) > 63) vidramaddr -= 64;
	
			code  = twincobr_fgvideoram16.read(vidramaddr & 0xfff);
			tile  = (code & 0x0fff) | twincobr_fg_rom_bank;
			color = (code & 0xf000) >> 12;
			if (twincobr_flip_screen != 0) { sx=40-sx; sy=30-sy; xpos=(sx*8) - (7-(scroll_x&7)); ypos=(sy*8) - (7-(scroll_y&7)); }
			else { xpos=(sx*8) - (scroll_x&7); ypos=(sy*8) - (scroll_y&7); }
			drawgfx(bitmap,Machine.gfx[1],
				tile,
				color,
				twincobr_flip_screen,twincobr_flip_screen,
				xpos,ypos,
				Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	
	/*********  Begin ugly sprite hack for Wardner when hero is in shop *********/
		if ((wardner_sprite_hack != 0) && (fgscrollx != bgscrollx)) {	/* Wardner ? */
			if ((fgscrollx==0x1c9) || (twincobr_flip_screen!=0 && (fgscrollx==0x17a))) {	/* in the shop ? */
				int wardner_hack = buffered_spriteram16.read(0x0b04/2);
			/* sprite position 0x6300 to 0x8700 -- hero on shop keeper (normal) */
			/* sprite position 0x3900 to 0x5e00 -- hero on shop keeper (flip) */
				if ((wardner_hack > 0x3900) && (wardner_hack < 0x8700)) {	/* hero at shop keeper ? */
					wardner_hack = buffered_spriteram16.read(0x0b02/2);
					wardner_hack |= 0x0400;			/* make hero top priority */
					buffered_spriteram16.write(0x0b02/2, wardner_hack);
					wardner_hack = buffered_spriteram16.read(0x0b0a/2);
					wardner_hack |= 0x0400;
					buffered_spriteram16.write(0x0b0a/2, wardner_hack);
					wardner_hack = buffered_spriteram16.read(0x0b12/2);
					wardner_hack |= 0x0400;
					buffered_spriteram16.write(0x0b12/2, wardner_hack);
					wardner_hack = buffered_spriteram16.read(0x0b1a/2);
					wardner_hack |= 0x0400;
					buffered_spriteram16.write(0x0b1a/2, wardner_hack);
				}
			}
		}
	/**********  End ugly sprite hack for Wardner when hero is in shop **********/
	
		/* draw the sprites in normal priority */
		twincobr_draw_sprites (bitmap, 0x0800);
	
		/* draw the top layer */
		scroll_x = (twincobr_flip_x_base + txscrollx) & 0x01ff;
		scroll_y = (twincobr_flip_y_base + txscrolly) & 0x00ff;
		vidbaseaddr = ((scroll_y>>3)*64) + (scroll_x>>3);
		scroll_realign_x = scroll_x >> 3;
		for (offs = (31*41)-1; offs >= 0; offs-- )
		{
			int xpos,ypos;
			int sx,sy;
			int vidramaddr = 0;
	
			sx = offs % 41;
			sy = offs / 41;
	
			vidramaddr = vidbaseaddr + (sy*64) + sx;
			if ((scroll_realign_x + sx) > 63) vidramaddr -= 64;
	
			code  = videoram16.read(vidramaddr & 0x7ff);
			tile  = (code & 0x07ff);
			color = (code & 0xf800) >> 11;
			if (twincobr_flip_screen != 0) { sx=40-sx; sy=30-sy; xpos=(sx*8) - (7-(scroll_x&7)); ypos=(sy*8) - (7-(scroll_y&7)); }
			else { xpos=(sx*8) - (scroll_x&7); ypos=(sy*8) - (scroll_y&7); }
			drawgfx(bitmap,Machine.gfx[0],
				tile,
				color,
				twincobr_flip_screen,twincobr_flip_screen,
				xpos,ypos,
				Machine.visible_area,TRANSPARENCY_PEN,0);
		}
	
		/* draw the sprites in high priority */
		twincobr_draw_sprites (bitmap, 0x0c00);
	} };
	
	public static VhEofCallbackPtr toaplan0_eof_callback = new VhEofCallbackPtr() {
            public void handler() {
                /*  Spriteram is always 1 frame ahead, suggesting spriteram buffering.
			There are no CPU output registers that control this so we
			assume it happens automatically every frame, at the end of vblank */
		buffer_spriteram16_w.handler(0,0,0);
            }
        };
	
}
