/**
 * Ported to 0.56
 */
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;

import static common.libc.cstring.*;
import static common.ptr.*;

import static mame056.drawgfxH.*;
import static mame056.drawgfx.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.mame.*;

//to refactor
import static arcadeflex036.osdepend.*;


public class generic {

    public static UBytePtr videoram = new UBytePtr();
    public static UBytePtr videoram16 = new UBytePtr();
/*TODO*///	data32_t *videoram32;
    public static int[] videoram_size = new int[1];
    public static UBytePtr colorram = new UBytePtr();
    /*TODO*///	data16_t *colorram16;
/*TODO*///	data32_t *colorram32;
    public static UBytePtr spriteram = new UBytePtr();/* not used in this module... */
    public static UBytePtr spriteram16 = new UBytePtr();		/* ... */
/*TODO*///	data32_t *spriteram32;		/* ... */
    public static UBytePtr spriteram_2 = new UBytePtr();
    /*TODO*///	data16_t *spriteram16_2;
/*TODO*///	data32_t *spriteram32_2;
    public static UBytePtr spriteram_3 = new UBytePtr();
    /*TODO*///	data16_t *spriteram16_3;
/*TODO*///	data32_t *spriteram32_3;
    public static UBytePtr buffered_spriteram = new UBytePtr();
    public static UBytePtr buffered_spriteram16 = new UBytePtr(1024 * 128);
/*TODO*///	data32_t *buffered_spriteram32;
    public static UBytePtr buffered_spriteram_2 = new UBytePtr();
    /*TODO*///	data16_t *buffered_spriteram16_2;
/*TODO*///	data32_t *buffered_spriteram32_2;
    public static int[] spriteram_size = new int[1];/* ... here just for convenience */
    public static int[] spriteram_2_size = new int[1];/* ... here just for convenience */
    public static int[] spriteram_3_size = new int[1];/* ... here just for convenience */
    public static char[] dirtybuffer;
    /*TODO*///	data16_t *dirtybuffer16;
/*TODO*///	data32_t *dirtybuffer32;
    public static mame_bitmap tmpbitmap;
    /*TODO*///	
/*TODO*///	
/*TODO*///	void generic_vh_postload(void)
/*TODO*///	{
/*TODO*///		memset(dirtybuffer,1,videoram_size);
/*TODO*///	}

    /**
     * *************************************************************************
     *
     * Start the video hardware emulation.
     *
     **************************************************************************
     */
    public static VhStartPtr generic_vh_start = new VhStartPtr() {
        public int handler() {
            dirtybuffer = null;
            tmpbitmap = null;

            if (videoram_size[0] == 0) {
                logerror("Error: generic_vh_start() called but videoram_size not initialized\n");
                return 1;
            }
            if ((dirtybuffer = new char[videoram_size[0]]) == null) {
                return 1;
            }
            memset(dirtybuffer, 1, videoram_size[0]);

            if ((tmpbitmap = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height)) == null) {
                dirtybuffer = null;
                return 1;
            }
            /*TODO*///		state_save_register_func_postload(generic_vh_postload);
            return 0;
        }
    };

    public static VhStartPtr generic_bitmapped_vh_start = new VhStartPtr() {
        public int handler() {
            if ((tmpbitmap = bitmap_alloc(Machine.drv.screen_width, Machine.drv.screen_height)) == null) {
                return 1;
            }

            return 0;
        }
    };

    /**
     * *************************************************************************
     * Stop the video hardware emulation.
     * *************************************************************************
     */
    public static VhStopPtr generic_vh_stop = new VhStopPtr() {
        public void handler() {
            dirtybuffer = null;
            bitmap_free(tmpbitmap);

            dirtybuffer = null;
            tmpbitmap = null;
        }
    };

    public static VhStopPtr generic_bitmapped_vh_stop = new VhStopPtr() {
        public void handler() {
            bitmap_free(tmpbitmap);

            tmpbitmap = null;
        }
    };

    /**
     * *************************************************************************
     * Draw the game screen in the given mame_bitmap. To be used by bitmapped
     * games not using sprites.
     * *************************************************************************
     */
    public static VhUpdatePtr generic_bitmapped_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            if (full_refresh != 0) {
                copybitmap(bitmap, tmpbitmap, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_NONE, 0);
            }
        }
    };
    public static ReadHandlerPtr videoram_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return videoram.read(offset);
        }
    };

    public static ReadHandlerPtr colorram_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return colorram.read(offset);
        }
    };

    public static WriteHandlerPtr videoram_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            if (videoram.read(offset) != data) {
                dirtybuffer[offset] = 1;

                videoram.write(offset, data);
            }
        }
    };

    public static WriteHandlerPtr colorram_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            if (colorram.read(offset) != data) {
                dirtybuffer[offset] = 1;

                colorram.write(offset, data);
            }
        }
    };

    public static ReadHandlerPtr spriteram_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return spriteram.read(offset);
        }
    };

    public static WriteHandlerPtr spriteram_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            spriteram.write(offset, data);
        }
    };
    /*TODO*///	
/*TODO*///	READ16_HANDLER( spriteram16_r )
/*TODO*///	{
/*TODO*///		return spriteram16[offset];
/*TODO*///	}
/*TODO*///	
/*TODO*///	WRITE16_HANDLER( spriteram16_w )
/*TODO*///	{
/*TODO*///		COMBINE_DATA(spriteram16+offset);
/*TODO*///	}
/*TODO*///	
    public static ReadHandlerPtr spriteram_2_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return spriteram_2.read(offset);
        }
    };

    public static WriteHandlerPtr spriteram_2_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            spriteram_2.write(offset, data);
        }
    };
    /*TODO*///	
/*TODO*///	/* Mish:  171099
/*TODO*///	
/*TODO*///		'Buffered spriteram' is where the graphics hardware draws the sprites
/*TODO*///	from private ram that the main CPU cannot access.  The main CPU typically
/*TODO*///	prepares sprites for the next frame in it's own sprite ram as the graphics
/*TODO*///	hardware renders sprites for the current frame from private ram.  Main CPU
/*TODO*///	sprite ram is usually copied across to private ram by setting some flag
/*TODO*///	in the VBL interrupt routine.
/*TODO*///	
/*TODO*///		The reason for this is to avoid sprite flicker or lag - if a game
/*TODO*///	is unable to prepare sprite ram within a frame (for example, lots of sprites
/*TODO*///	on screen) then it doesn't trigger the buffering hardware - instead the
/*TODO*///	graphics hardware will use the sprites from the last frame. An example is
/*TODO*///	Dark Seal - the buffer flag is only written to if the CPU is idle at the time
/*TODO*///	of the VBL interrupt.  If the buffering is not emulated the sprites flicker
/*TODO*///	at busy scenes.
/*TODO*///	
/*TODO*///		Some games seem to use buffering because of hardware constraints -
/*TODO*///	Capcom games (Cps1, Last Duel, etc) render spriteram _1 frame ahead_ and
/*TODO*///	buffer this spriteram at the end of a frame, so the _next_ frame must be drawn
/*TODO*///	from the buffer.  Presumably the graphics hardware and the main cpu cannot
/*TODO*///	share the same spriteram for whatever reason.
/*TODO*///	
/*TODO*///		Sprite buffering & Mame:
/*TODO*///	
/*TODO*///		To use sprite buffering in a driver use VIDEO_BUFFERS_SPRITERAM in the
/*TODO*///	machine driver.  This will automatically create an area for buffered spriteram
/*TODO*///	equal to the size of normal spriteram.
/*TODO*///	
/*TODO*///		Spriteram size _must_ be declared in the memory map:
/*TODO*///	
/*TODO*///		{ 0x120000, 0x1207ff, MWA_BANK2, &spriteram, &spriteram_size },
/*TODO*///	
/*TODO*///		Then the video driver must draw the sprites from the buffered_spriteram
/*TODO*///	pointer.  The function buffer_spriteram_w() is used to simulate hardware
/*TODO*///	which buffers the spriteram from a memory location write.  The function
/*TODO*///	buffer_spriteram(UBytePtr ptr, int length) can be used where
/*TODO*///	more control is needed over what is buffered.
/*TODO*///	
/*TODO*///		For examples see darkseal.c, contra.c, lastduel.c, bionicc.c etc.
/*TODO*///	
/*TODO*///	*/
/*TODO*///	
    public static WriteHandlerPtr buffer_spriteram_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            memcpy(buffered_spriteram, spriteram, spriteram_size[0]);
        }
    };
    	
    public static WriteHandlerPtr16 buffer_spriteram16_w = new WriteHandlerPtr16() {
        public void handler(int offset, int data, int d2) {
            //System.out.println("EOF!");
            memcpy(buffered_spriteram16,spriteram16,spriteram_size[0]);
        }
    };

	
/*TODO*///	WRITE32_HANDLER( buffer_spriteram32_w )
/*TODO*///	{
/*TODO*///		memcpy(buffered_spriteram32,spriteram32,spriteram_size);
/*TODO*///	}
/*TODO*///	
    public static WriteHandlerPtr buffer_spriteram_2_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            memcpy(buffered_spriteram_2, spriteram_2, spriteram_2_size[0]);
        }
    };

    /*TODO*///	
/*TODO*///	WRITE16_HANDLER( buffer_spriteram16_2_w )
/*TODO*///	{
/*TODO*///		memcpy(buffered_spriteram16_2,spriteram16_2,spriteram_2_size);
/*TODO*///	}
/*TODO*///	
/*TODO*///	WRITE32_HANDLER( buffer_spriteram32_2_w )
/*TODO*///	{
/*TODO*///		memcpy(buffered_spriteram32_2,spriteram32_2,spriteram_2_size);
/*TODO*///	}
/*TODO*///	
    public static void buffer_spriteram(UBytePtr ptr, int length) {
        memcpy(buffered_spriteram, ptr, length);
    }

    public static void buffer_spriteram_2(UBytePtr ptr, int length) {
        memcpy(buffered_spriteram_2, ptr, length);
    }
}
