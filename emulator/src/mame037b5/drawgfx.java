/*
 *  Ported to 0.37b5
 */
package mame037b5;


import static arcadeflex036.libc_old.IntPtr;
import static common.ptr.*;
import static common.subArrays.*;
import static common.libc.expressions.*;
import static arcadeflex037b7.video.osd_mark_dirty;
import static mame056.drawgfxH.*;
import static mame056.driverH.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.usrintrf.*;
import static mame037b7.drawgfx.*;
import static mame056.commonH.*;
import static mame056.drawgfx.decodechar;

public class drawgfx {

    /*TODO*///#ifndef DECLARE
/*TODO*///
/*TODO*///#include "driver.h"
/*TODO*///
/*TODO*///
/*TODO*////* LBO */
/*TODO*///#ifdef LSB_FIRST
/*TODO*///#define BL0 0
/*TODO*///#define BL1 1
/*TODO*///#define BL2 2
/*TODO*///#define BL3 3
/*TODO*///#define WL0 0
/*TODO*///#define WL1 1
/*TODO*///#else
/*TODO*///#define BL0 3
/*TODO*///#define BL1 2
/*TODO*///#define BL2 1
/*TODO*///#define BL3 0
/*TODO*///#define WL0 1
/*TODO*///#define WL1 0
/*TODO*///#endif
/*TODO*///
/*TODO*///
/*TODO*///UINT8 gfx_drawmode_table[256];
    public static plot_pixel_procPtr plot_pixel;
    public static read_pixel_procPtr read_pixel;
    public static plot_box_procPtr plot_box;

    static int[]/*UINT8*/ is_raw = new int[TRANSPARENCY_MODES];

    /*TODO*///
/*TODO*///#ifdef ALIGN_INTS /* GSL 980108 read/write nonaligned dword routine for ARM processor etc */
/*TODO*///
/*TODO*///INLINE UINT32 read_dword(void *address)
/*TODO*///{
/*TODO*///	if ((long)address & 3)
/*TODO*///	{
/*TODO*///#ifdef LSB_FIRST  /* little endian version */
/*TODO*///  		return ( *((UINT8 *)address) +
/*TODO*///				(*((UINT8 *)address+1) << 8)  +
/*TODO*///				(*((UINT8 *)address+2) << 16) +
/*TODO*///				(*((UINT8 *)address+3) << 24) );
/*TODO*///#else             /* big endian version */
/*TODO*///  		return ( *((UINT8 *)address+3) +
/*TODO*///				(*((UINT8 *)address+2) << 8)  +
/*TODO*///				(*((UINT8 *)address+1) << 16) +
/*TODO*///				(*((UINT8 *)address)   << 24) );
/*TODO*///#endif
/*TODO*///	}
/*TODO*///	else
/*TODO*///		return *(UINT32 *)address;
/*TODO*///}
/*TODO*///
/*TODO*///
    static void write_dword(UBytePtr address, int data)//TODO probably okay but might need to recheck this
    {
        address.write(0, data & 0xff);
        address.write(1, (data >> 8) & 0xff);
        address.write(2, (data >> 16) & 0xff);
        address.write(3, (data >> 24) & 0xff);
    }

    public static void blockmove_NtoN_transpen_noremap8(
            UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo,
            UBytePtr dstdata, int dstmodulo,
            int transpen) {
        int end;//UINT8 *end;
        int trans4;
        IntPtr sd4;//UINT32 *sd4;

        srcmodulo -= srcwidth;
        dstmodulo -= srcwidth;

        trans4 = transpen * 0x01010101;

        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while (((long) srcdata.offset & 3) != 0 && dstdata.offset < end) /* longword align */ {
                int col;

                col = srcdata.readinc();
                if (col != transpen) {
                    dstdata.write(0, col);
                }
                dstdata.inc();
            }
            sd4 = new IntPtr(srcdata);
            while (dstdata.offset <= end - 4) {
                int/*UINT32*/ col4;

                if ((col4 = (sd4.read(0))) != trans4) {
                    /*UINT32*/
                    int xod4;

                    xod4 = col4 ^ trans4;
                    if ((xod4 & 0x000000ff) != 0 && (xod4 & 0x0000ff00) != 0
                            && (xod4 & 0x00ff0000) != 0 && (xod4 & 0xff000000) != 0) {
                        write_dword(dstdata, (int) col4);//write_dword((UINT32 *)dstdata,col4);
                    } else {
                        if ((xod4 & 0xff000000) != 0) {
                            dstdata.write(3, (col4 >> 24) & 0xFF);
                        }
                        if ((xod4 & 0x00ff0000) != 0) {
                            dstdata.write(2, (col4 >> 16) & 0xFF);
                        }
                        if ((xod4 & 0x0000ff00) != 0) {
                            dstdata.write(1, (col4 >> 8) & 0xFF);
                        }
                        if ((xod4 & 0x000000ff) != 0) {
                            dstdata.write(0, (col4) & 0xFF);
                        }
                    }
                }
                sd4.base += 4;
                dstdata.offset += 4;
            }
            srcdata.set(sd4.readCA(), sd4.getBase());
            while (dstdata.offset < end) {
                int col;

                col = (srcdata.readinc());
                if (col != transpen) {
                    dstdata.write(0, col);
                }
                dstdata.inc();
            }

            srcdata.inc(srcmodulo);
            dstdata.inc(dstmodulo);
            srcheight--;
        }
    }

    public static void blockmove_NtoN_transpen_noremap_flipx8(
            UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo,
            UBytePtr dstdata, int dstmodulo,
            int transpen) {
        int end;//UINT8 *end;
        int trans4;
        IntPtr sd4;//UINT32 *sd4;

        srcmodulo += srcwidth;
        dstmodulo -= srcwidth;
        //srcdata += srcwidth-1;
        srcdata.offset -= 3;

        trans4 = transpen * 0x01010101;

        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while (((long) srcdata.offset & 3) != 0 && dstdata.offset < end) /* longword align */ {
                int col;

                col = srcdata.read(3);
                srcdata.offset--;
                if (col != transpen) {
                    dstdata.write(col);
                }
                dstdata.inc();
            }
            sd4 = new IntPtr(srcdata);
            while (dstdata.offset <= end - 4) {
                int col4;

                if ((col4 = sd4.read(0)) != trans4) {
                    int xod4;

                    xod4 = col4 ^ trans4;

                    if ((xod4 & 0x000000ff) != 0) {
                        dstdata.write(3, col4 & 0xFF);
                    }
                    if ((xod4 & 0x0000ff00) != 0) {
                        dstdata.write(2, (col4 >> 8) & 0xFF);
                    }
                    if ((xod4 & 0x00ff0000) != 0) {
                        dstdata.write(1, (col4 >> 16) & 0xFF);
                    }
                    if ((xod4 & 0xff000000) != 0) {
                        dstdata.write(0, (col4 >> 24) & 0xFF);
                    }
                }
                sd4.base -= 4;
                dstdata.offset += 4;
            }
            srcdata.set(sd4.readCA(), sd4.getBase());
            while (dstdata.offset < end) {
                int col;

                col = srcdata.read(3);
                srcdata.dec();
                if (col != transpen) {
                    dstdata.write(col);
                }
                dstdata.inc();
            }

            srcdata.inc(srcmodulo);
            dstdata.inc(dstmodulo);
            srcheight--;
        }
    }

    /*TODO*///
    /*TODO*///INLINE void blockmove_NtoN_transpen_noremap16(
    /*TODO*///		const UINT16 *srcdata,int srcwidth,int srcheight,int srcmodulo,
    /*TODO*///		UINT16 *dstdata,int dstmodulo,
    /*TODO*///		int transpen)
    /*TODO*///{
    /*TODO*///	UINT16 *end;
    /*TODO*///
    /*TODO*///	srcmodulo -= srcwidth;
    /*TODO*///	dstmodulo -= srcwidth;
    /*TODO*///
    /*TODO*///	while (srcheight)
    /*TODO*///	{
    /*TODO*///		end = dstdata + srcwidth;
    /*TODO*///		while (dstdata < end)
    /*TODO*///		{
    /*TODO*///			int col;
    /*TODO*///
    /*TODO*///			col = *(srcdata++);
    /*TODO*///			if (col != transpen) *dstdata = col;
    /*TODO*///			dstdata++;
    /*TODO*///		}
    /*TODO*///
    /*TODO*///		srcdata += srcmodulo;
    /*TODO*///		dstdata += dstmodulo;
    /*TODO*///		srcheight--;
    /*TODO*///	}
    /*TODO*///}
    /*TODO*///
    /*TODO*///INLINE void blockmove_NtoN_transpen_noremap_flipx16(
    /*TODO*///		const UINT16 *srcdata,int srcwidth,int srcheight,int srcmodulo,
    /*TODO*///		UINT16 *dstdata,int dstmodulo,
    /*TODO*///		int transpen)
    /*TODO*///{
    /*TODO*///	UINT16 *end;
    /*TODO*///
    /*TODO*///	srcmodulo += srcwidth;
    /*TODO*///	dstmodulo -= srcwidth;
    /*TODO*///	//srcdata += srcwidth-1;
    /*TODO*///
    /*TODO*///	while (srcheight)
    /*TODO*///	{
    /*TODO*///		end = dstdata + srcwidth;
    /*TODO*///		while (dstdata < end)
    /*TODO*///		{
    /*TODO*///			int col;
    /*TODO*///
    /*TODO*///			col = *(srcdata--);
    /*TODO*///			if (col != transpen) *dstdata = col;
    /*TODO*///			dstdata++;
    /*TODO*///		}
    /*TODO*///
    /*TODO*///		srcdata += srcmodulo;
    /*TODO*///		dstdata += dstmodulo;
    /*TODO*///		srcheight--;
    /*TODO*///	}
    /*TODO*///}
    /*TODO*///
    /*TODO*///
    /*TODO*///#define DATA_TYPE UINT8
    /*TODO*///#define DECLARE(function,args,body) INLINE void function##8 args body
    /*TODO*///#define BLOCKMOVE(function,flipx,args) \
    /*TODO*///	if (flipx) blockmove_##function##_flipx##8 args ; \
    /*TODO*///	else blockmove_##function##8 args
    /*TODO*///#include "drawgfx.c"
    /*TODO*///#undef DATA_TYPE
    /*TODO*///#undef DECLARE
    /*TODO*///#undef BLOCKMOVE
    /*TODO*///
    /*TODO*///#define DATA_TYPE UINT16
    /*TODO*///#define DECLARE(function,args,body) INLINE void function##16 args body
    /*TODO*///#define BLOCKMOVE(function,flipx,args) \
    /*TODO*///	if (flipx) blockmove_##function##_flipx##16 args ; \
    /*TODO*///	else blockmove_##function##16 args
    /*TODO*///#include "drawgfx.c"
    /*TODO*///#undef DATA_TYPE
    /*TODO*///#undef DECLARE
    /*TODO*///#undef BLOCKMOVE
    /*TODO*///
    /**
     * *************************************************************************
     * Draw graphic elements in the specified bitmap.
     * <p>
     * transparency == TRANSPARENCY_NONE - no transparency. transparency ==
     * TRANSPARENCY_PEN - bits whose _original_ value is == transparent_color
     * are transparent. This is the most common kind of transparency.
     * transparency == TRANSPARENCY_PENS - as above, but transparent_color is a
     * mask of transparent pens. transparency == TRANSPARENCY_COLOR - bits whose
     * _remapped_ palette index (taken from Machine->game_colortable) is ==
     * transparent_color transparency == TRANSPARENCY_THROUGH - if the
     * _destination_ pixel is == transparent_color, the source pixel is drawn
     * over it. This is used by e.g. Jr. Pac Man to draw the sprites when the
     * background has priority over them.
     * <p>
     * transparency == TRANSPARENCY_PEN_TABLE - the transparency condition is
     * same as TRANSPARENCY_PEN A special drawing is done according to
     * gfx_drawmode_table[source pixel]. DRAWMODE_NONE transparent
     * DRAWMODE_SOURCE normal, draw source pixel. DRAWMODE_SHADOW destination is
     * changed through palette_shadow_table[]
     * *************************************************************************
     */
    public static void common_drawgfx(mame_bitmap dest, GfxElement gfx,
            /*unsigned*/ int code,/*unsigned*/ int color, int flipx, int flipy, int sx, int sy,
            rectangle clip, int transparency, int transparent_color,
            mame_bitmap pri_buffer,/*UINT32*/ int pri_mask) {
        rectangle myclip = new rectangle();

        if (gfx == null) {
            usrintf_showmessage("drawgfx() gfx == 0");
            return;
        }
        if (gfx.colortable == null && is_raw[transparency] == 0) {
            usrintf_showmessage("drawgfx() gfx->colortable == 0");
            return;
        }

        code %= gfx.total_elements;
        if (is_raw[transparency] == 0) {
            color %= gfx.total_colors;
        }

        if (gfx.pen_usage != null && (transparency == TRANSPARENCY_PEN || transparency == TRANSPARENCY_PENS)) {
            int transmask = 0;

            if (transparency == TRANSPARENCY_PEN) {
                transmask = 1 << transparent_color;
            } else /* transparency == TRANSPARENCY_PENS */ {
                transmask = transparent_color;
            }

            if ((gfx.pen_usage[code] & ~transmask) == 0) /* character is totally transparent, no need to draw */ {
                return;
            } else if ((gfx.pen_usage[code] & transmask) == 0) /* character is totally opaque, can disable transparency */ {
                transparency = TRANSPARENCY_NONE;
            }
        }

        if ((Machine.orientation & ORIENTATION_SWAP_XY) != 0) {
            int temp;

            temp = sx;
            sx = sy;
            sy = temp;

            temp = flipx;
            flipx = flipy;
            flipy = temp;

            if (clip != null) {
                /* clip and myclip might be the same, so we need a temporary storage */
                temp = clip.min_x;
                myclip.min_x = clip.min_y;
                myclip.min_y = temp;
                temp = clip.max_x;
                myclip.max_x = clip.max_y;
                myclip.max_y = temp;
                clip = myclip;
            }
        }
        if ((Machine.orientation & ORIENTATION_FLIP_X) != 0) {
            sx = dest.width - gfx.width - sx;
            if (clip != null) {
                int temp;


                /* clip and myclip might be the same, so we need a temporary storage */
                temp = clip.min_x;
                myclip.min_x = dest.width - 1 - clip.max_x;
                myclip.max_x = dest.width - 1 - temp;
                myclip.min_y = clip.min_y;
                myclip.max_y = clip.max_y;
                clip = myclip;
            }
            flipx = NOT(flipx);
        }
        if ((Machine.orientation & ORIENTATION_FLIP_Y) != 0) {
            sy = dest.height - gfx.height - sy;
            if (clip != null) {
                int temp;

                myclip.min_x = clip.min_x;
                myclip.max_x = clip.max_x;
                /* clip and myclip might be the same, so we need a temporary storage */
                temp = clip.min_y;
                myclip.min_y = dest.height - 1 - clip.max_y;
                myclip.max_y = dest.height - 1 - temp;
                clip = myclip;
            }
            flipy = NOT(flipy);
        }

        if (dest.depth != 16) {
            drawgfx_core8(dest, gfx, code, color, flipx, flipy, sx, sy, clip, transparency, transparent_color, pri_buffer, pri_mask);
        } else {
//            throw new UnsupportedOperationException("Unsupported");
            /*TODO*///		drawgfx_core16(dest,gfx,code,color,flipx,flipy,sx,sy,clip,transparency,transparent_color,pri_buffer,pri_mask);
        }
    }

    public static void drawgfx(mame_bitmap dest, GfxElement gfx,
            /*unsigned*/ int code,/*unsigned*/ int color, int flipx, int flipy, int sx, int sy,
            rectangle clip, int transparency, int transparent_color) {
        common_drawgfx(dest, gfx, code, color, flipx, flipy, sx, sy, clip, transparency, transparent_color, null, 0);
    }

    /*TODO*///
/*TODO*///void pdrawgfx(struct osd_bitmap *dest,const struct GfxElement *gfx,
/*TODO*///		unsigned int code,unsigned int color,int flipx,int flipy,int sx,int sy,
/*TODO*///		const struct rectangle *clip,int transparency,int transparent_color,UINT32 priority_mask)
/*TODO*///{
/*TODO*///	profiler_mark(PROFILER_DRAWGFX);
/*TODO*///	common_drawgfx(dest,gfx,code,color,flipx,flipy,sx,sy,clip,transparency,transparent_color,priority_bitmap,priority_mask);
/*TODO*///	profiler_mark(PROFILER_END);
/*TODO*///}
    /**
     * *************************************************************************
     * Use drawgfx() to copy a bitmap onto another at the given position. This
     * function will very likely change in the future.
     * *************************************************************************
     */
    public static void copybitmap(mame_bitmap dest, mame_bitmap src, int flipx, int flipy, int sx, int sy,
            rectangle clip, int transparency, int transparent_color) {
        /* translate to proper transparency here */
        if (transparency == TRANSPARENCY_NONE) {
            transparency = TRANSPARENCY_NONE_RAW;
        } else if (transparency == TRANSPARENCY_PEN) {
            transparency = TRANSPARENCY_PEN_RAW;
        } else if (transparency == TRANSPARENCY_COLOR) {
            transparent_color = Machine.pens[transparent_color];
            transparency = TRANSPARENCY_PEN_RAW;
        } 

        copybitmap_remap(dest, src, flipx, flipy, sx, sy, clip, transparency, transparent_color);
    }

    public static void copybitmap_remap(mame_bitmap dest, mame_bitmap src, int flipx, int flipy, int sx, int sy,
            rectangle clip, int transparency, int transparent_color) {
        rectangle myclip = new rectangle();

        if ((Machine.orientation & ORIENTATION_SWAP_XY) != 0) {
            int temp;

            temp = sx;
            sx = sy;
            sy = temp;

            temp = flipx;
            flipx = flipy;
            flipy = temp;

            if (clip != null) {
                /* clip and myclip might be the same, so we need a temporary storage */
                temp = clip.min_x;
                myclip.min_x = clip.min_y;
                myclip.min_y = temp;
                temp = clip.max_x;
                myclip.max_x = clip.max_y;
                myclip.max_y = temp;
                clip = myclip;
            }
        }
        if ((Machine.orientation & ORIENTATION_FLIP_X) != 0) {
            sx = dest.width - src.width - sx;
            if (clip != null) {
                int temp;


                /* clip and myclip might be the same, so we need a temporary storage */
                temp = clip.min_x;
                myclip.min_x = dest.width - 1 - clip.max_x;
                myclip.max_x = dest.width - 1 - temp;
                myclip.min_y = clip.min_y;
                myclip.max_y = clip.max_y;
                clip = myclip;
            }
        }
        if ((Machine.orientation & ORIENTATION_FLIP_Y) != 0) {
            sy = dest.height - src.height - sy;
            if (clip != null) {
                int temp;

                myclip.min_x = clip.min_x;
                myclip.max_x = clip.max_x;
                /* clip and myclip might be the same, so we need a temporary storage */
                temp = clip.min_y;
                myclip.min_y = dest.height - 1 - clip.max_y;
                myclip.max_y = dest.height - 1 - temp;
                clip = myclip;
            }
        }

        if (dest.depth != 16) {
            copybitmap_core8(dest, src, flipx, flipy, sx, sy, clip, transparency, transparent_color);
        } else {
            /*TODO*///		copybitmap_core16(dest,src,flipx,flipy,sx,sy,clip,transparency,transparent_color);
            throw new UnsupportedOperationException("unsupported");
        }

    }

    /**
     * *************************************************************************
     * Copy a bitmap onto another with scroll and wraparound. This function
     * supports multiple independently scrolling rows/columns. "rows" is the
     * number of indepentently scrolling rows. "rowscroll" is an array of
     * integers telling how much to scroll each row. Same thing for "cols" and
     * "colscroll". If the bitmap cannot scroll in one direction, set rows or
     * columns to 0. If the bitmap scrolls as a whole, set rows and/or cols to
     * 1. Bidirectional scrolling is, of course, supported only if the bitmap
     * scrolls as a whole in at least one direction.
     * *************************************************************************
     */
    /*leave it here for compatibility  (shadow)*/
    public static void copyscrollbitmap(mame_bitmap dest, mame_bitmap src,
            int rows, int[] rowscroll, int cols, int[] colscroll,
            rectangle clip, int transparency, int transparent_color) {
        /* translate to proper transparency here */
        if (transparency == TRANSPARENCY_NONE) {
            transparency = TRANSPARENCY_NONE_RAW;
        } else if (transparency == TRANSPARENCY_PEN) {
            transparency = TRANSPARENCY_PEN_RAW;
        } else if (transparency == TRANSPARENCY_COLOR) {
            transparent_color = Machine.pens[transparent_color];
            transparency = TRANSPARENCY_PEN_RAW;
        } 

        copyscrollbitmap_remap(dest, src, rows, rowscroll, cols, colscroll, clip, transparency, transparent_color);
    }

    /*TODO*///
/*TODO*///
/*TODO*////* notes:
/*TODO*///   - startx and starty MUST be UINT32 for calculations to work correctly
/*TODO*///   - srcbitmap->width and height are assumed to be a power of 2 to speed up wraparound
/*TODO*///   */
/*TODO*///void copyrozbitmap(struct osd_bitmap *dest,struct osd_bitmap *src,
/*TODO*///		UINT32 startx,UINT32 starty,int incxx,int incxy,int incyx,int incyy,int wraparound,
/*TODO*///		const struct rectangle *clip,int transparency,int transparent_color,UINT32 priority)
/*TODO*///{
/*TODO*///	profiler_mark(PROFILER_COPYBITMAP);
/*TODO*///
/*TODO*///	/* cheat, the core doesn't support TRANSPARENCY_NONE yet */
/*TODO*///	if (transparency == TRANSPARENCY_NONE)
/*TODO*///	{
/*TODO*///		transparency = TRANSPARENCY_PEN;
/*TODO*///		transparent_color = -1;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* if necessary, remap the transparent color */
/*TODO*///	if (transparency == TRANSPARENCY_COLOR)
/*TODO*///	{
/*TODO*///		transparency = TRANSPARENCY_PEN;
/*TODO*///		transparent_color = Machine->pens[transparent_color];
/*TODO*///	}
/*TODO*///
/*TODO*///	if (transparency != TRANSPARENCY_PEN)
/*TODO*///	{
/*TODO*///		usrintf_showmessage("copyrozbitmap unsupported trans %02x",transparency);
/*TODO*///		return;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (dest->depth != 16)
/*TODO*///		copyrozbitmap_core8(dest,src,startx,starty,incxx,incxy,incyx,incyy,wraparound,clip,transparency,transparent_color,priority);
/*TODO*///	else
/*TODO*///		copyrozbitmap_core16(dest,src,startx,starty,incxx,incxy,incyx,incyy,wraparound,clip,transparency,transparent_color,priority);
/*TODO*///
/*TODO*///	profiler_mark(PROFILER_END);
/*TODO*///}
/*TODO*///
/*TODO*///

    /* fill a bitmap using the specified pen */
    public static void fillbitmap(mame_bitmap dest, int pen, rectangle clip) {
        int sx, sy, ex, ey, y;
        rectangle myclip = new rectangle();

        if ((Machine.orientation & ORIENTATION_SWAP_XY) != 0) {
            if (clip != null) {
                myclip.min_x = clip.min_y;
                myclip.max_x = clip.max_y;
                myclip.min_y = clip.min_x;
                myclip.max_y = clip.max_x;
                clip = myclip;
            }
        }
        if ((Machine.orientation & ORIENTATION_FLIP_X) != 0) {
            if (clip != null) {
                int temp;

                temp = clip.min_x;
                myclip.min_x = dest.width - 1 - clip.max_x;
                myclip.max_x = dest.width - 1 - temp;
                myclip.min_y = clip.min_y;
                myclip.max_y = clip.max_y;
                clip = myclip;
            }
        }
        if ((Machine.orientation & ORIENTATION_FLIP_Y) != 0) {
            if (clip != null) {
                int temp;

                myclip.min_x = clip.min_x;
                myclip.max_x = clip.max_x;
                temp = clip.min_y;
                myclip.min_y = dest.height - 1 - clip.max_y;
                myclip.max_y = dest.height - 1 - temp;
                clip = myclip;
            }
        }

        sx = 0;
        ex = dest.width - 1;
        sy = 0;
        ey = dest.height - 1;

        if (clip != null && sx < clip.min_x) {
            sx = clip.min_x;
        }
        if (clip != null && ex > clip.max_x) {
            ex = clip.max_x;
        }
        if (sx > ex) {
            return;
        }
        if (clip != null && sy < clip.min_y) {
            sy = clip.min_y;
        }
        if (clip != null && ey > clip.max_y) {
            ey = clip.max_y;
        }
        if (sy > ey) {
            return;
        }

        osd_mark_dirty(sx, sy, ex, ey, 0);
        /* ASG 971011 */

 /* ASG 980211 */
        if (dest.depth == 16) {
//            throw new UnsupportedOperationException("unsupported");
            /*TODO*///		if ((pen >> 8) == (pen & 0xff))
/*TODO*///		{
/*TODO*///			for (y = sy;y <= ey;y++)
/*TODO*///				memset(&dest->line[y][sx*2],pen&0xff,(ex-sx+1)*2);
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			UINT16 *sp = (UINT16 *)dest->line[sy];
/*TODO*///			int x;
/*TODO*///
/*TODO*///			for (x = sx;x <= ex;x++)
/*TODO*///				sp[x] = pen;
/*TODO*///			sp+=sx;
/*TODO*///			for (y = sy+1;y <= ey;y++)
/*TODO*///				memcpy(&dest->line[y][sx*2],sp,(ex-sx+1)*2);
/*TODO*///		}
        } else {
            for (y = sy; y <= ey; y++) {
                //memset(&dest->line[y][sx],pen,ex-sx+1);
                for (int k = 0; k < ex - sx + 1; k++) {
                    dest.line[y].write(sx + k, pen);
                }
            }
        }
    }

    /*TODO*///
/*TODO*///INLINE void common_drawgfxzoom( struct osd_bitmap *dest_bmp,const struct GfxElement *gfx,
/*TODO*///		unsigned int code,unsigned int color,int flipx,int flipy,int sx,int sy,
/*TODO*///		const struct rectangle *clip,int transparency,int transparent_color,
/*TODO*///		int scalex, int scaley,struct osd_bitmap *pri_buffer,UINT32 pri_mask)
/*TODO*///{
/*TODO*///	struct rectangle myclip;
/*TODO*///
/*TODO*///
/*TODO*///	if (!scalex || !scaley) return;
/*TODO*///
/*TODO*///	if (scalex == 0x10000 && scaley == 0x10000)
/*TODO*///	{
/*TODO*///		common_drawgfx(dest_bmp,gfx,code,color,flipx,flipy,sx,sy,clip,transparency,transparent_color,pri_buffer,pri_mask);
/*TODO*///		return;
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///	pri_mask |= (1<<31);
/*TODO*///
/*TODO*///	if (transparency != TRANSPARENCY_PEN && transparency != TRANSPARENCY_PEN_RAW
/*TODO*///			&& transparency != TRANSPARENCY_PENS && transparency != TRANSPARENCY_COLOR
/*TODO*///			&& transparency != TRANSPARENCY_PEN_TABLE && transparency != TRANSPARENCY_PEN_TABLE_RAW)
/*TODO*///	{
/*TODO*///		usrintf_showmessage("drawgfxzoom unsupported trans %02x",transparency);
/*TODO*///		return;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (transparency == TRANSPARENCY_COLOR)
/*TODO*///		transparent_color = Machine->pens[transparent_color];
/*TODO*///
/*TODO*///
/*TODO*///	/*
/*TODO*///	scalex and scaley are 16.16 fixed point numbers
/*TODO*///	1<<15 : shrink to 50%
/*TODO*///	1<<16 : uniform scale
/*TODO*///	1<<17 : double to 200%
/*TODO*///	*/
/*TODO*///
/*TODO*///
/*TODO*///	if (Machine->orientation & ORIENTATION_SWAP_XY)
/*TODO*///	{
/*TODO*///		int temp;
/*TODO*///
/*TODO*///		temp = sx;
/*TODO*///		sx = sy;
/*TODO*///		sy = temp;
/*TODO*///
/*TODO*///		temp = flipx;
/*TODO*///		flipx = flipy;
/*TODO*///		flipy = temp;
/*TODO*///
/*TODO*///		temp = scalex;
/*TODO*///		scalex = scaley;
/*TODO*///		scaley = temp;
/*TODO*///
/*TODO*///		if (clip)
/*TODO*///		{
/*TODO*///			/* clip and myclip might be the same, so we need a temporary storage */
/*TODO*///			temp = clip->min_x;
/*TODO*///			myclip.min_x = clip->min_y;
/*TODO*///			myclip.min_y = temp;
/*TODO*///			temp = clip->max_x;
/*TODO*///			myclip.max_x = clip->max_y;
/*TODO*///			myclip.max_y = temp;
/*TODO*///			clip = &myclip;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	if (Machine->orientation & ORIENTATION_FLIP_X)
/*TODO*///	{
/*TODO*///		sx = dest_bmp->width - ((gfx->width * scalex + 0x7fff) >> 16) - sx;
/*TODO*///		if (clip)
/*TODO*///		{
/*TODO*///			int temp;
/*TODO*///
/*TODO*///
/*TODO*///			/* clip and myclip might be the same, so we need a temporary storage */
/*TODO*///			temp = clip->min_x;
/*TODO*///			myclip.min_x = dest_bmp->width-1 - clip->max_x;
/*TODO*///			myclip.max_x = dest_bmp->width-1 - temp;
/*TODO*///			myclip.min_y = clip->min_y;
/*TODO*///			myclip.max_y = clip->max_y;
/*TODO*///			clip = &myclip;
/*TODO*///		}
/*TODO*///#ifndef PREROTATE_GFX
/*TODO*///		flipx = !flipx;
/*TODO*///#endif
/*TODO*///	}
/*TODO*///	if (Machine->orientation & ORIENTATION_FLIP_Y)
/*TODO*///	{
/*TODO*///		sy = dest_bmp->height - ((gfx->height * scaley + 0x7fff) >> 16) - sy;
/*TODO*///		if (clip)
/*TODO*///		{
/*TODO*///			int temp;
/*TODO*///
/*TODO*///
/*TODO*///			myclip.min_x = clip->min_x;
/*TODO*///			myclip.max_x = clip->max_x;
/*TODO*///			/* clip and myclip might be the same, so we need a temporary storage */
/*TODO*///			temp = clip->min_y;
/*TODO*///			myclip.min_y = dest_bmp->height-1 - clip->max_y;
/*TODO*///			myclip.max_y = dest_bmp->height-1 - temp;
/*TODO*///			clip = &myclip;
/*TODO*///		}
/*TODO*///#ifndef PREROTATE_GFX
/*TODO*///		flipy = !flipy;
/*TODO*///#endif
/*TODO*///	}
/*TODO*///
/*TODO*///	/* KW 991012 -- Added code to force clip to bitmap boundary */
/*TODO*///	if(clip)
/*TODO*///	{
/*TODO*///		myclip.min_x = clip->min_x;
/*TODO*///		myclip.max_x = clip->max_x;
/*TODO*///		myclip.min_y = clip->min_y;
/*TODO*///		myclip.max_y = clip->max_y;
/*TODO*///
/*TODO*///		if (myclip.min_x < 0) myclip.min_x = 0;
/*TODO*///		if (myclip.max_x >= dest_bmp->width) myclip.max_x = dest_bmp->width-1;
/*TODO*///		if (myclip.min_y < 0) myclip.min_y = 0;
/*TODO*///		if (myclip.max_y >= dest_bmp->height) myclip.max_y = dest_bmp->height-1;
/*TODO*///
/*TODO*///		clip=&myclip;
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///	/* ASG 980209 -- added 16-bit version */
/*TODO*///	if (dest_bmp->depth != 16)
/*TODO*///	{
/*TODO*///		if( gfx && gfx->colortable )
/*TODO*///		{
/*TODO*///			const UINT16 *pal = &gfx->colortable[gfx->color_granularity * (color % gfx->total_colors)]; /* ASG 980209 */
/*TODO*///			int source_base = (code % gfx->total_elements) * gfx->height;
/*TODO*///
/*TODO*///			int sprite_screen_height = (scaley*gfx->height+0x8000)>>16;
/*TODO*///			int sprite_screen_width = (scalex*gfx->width+0x8000)>>16;
/*TODO*///
/*TODO*///			/* compute sprite increment per screen pixel */
/*TODO*///			int dx = (gfx->width<<16)/sprite_screen_width;
/*TODO*///			int dy = (gfx->height<<16)/sprite_screen_height;
/*TODO*///
/*TODO*///			int ex = sx+sprite_screen_width;
/*TODO*///			int ey = sy+sprite_screen_height;
/*TODO*///
/*TODO*///			int x_index_base;
/*TODO*///			int y_index;
/*TODO*///
/*TODO*///			if( flipx )
/*TODO*///			{
/*TODO*///				x_index_base = (sprite_screen_width-1)*dx;
/*TODO*///				dx = -dx;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				x_index_base = 0;
/*TODO*///			}
/*TODO*///
/*TODO*///			if( flipy )
/*TODO*///			{
/*TODO*///				y_index = (sprite_screen_height-1)*dy;
/*TODO*///				dy = -dy;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				y_index = 0;
/*TODO*///			}
/*TODO*///
/*TODO*///			if( clip )
/*TODO*///			{
/*TODO*///				if( sx < clip->min_x)
/*TODO*///				{ /* clip left */
/*TODO*///					int pixels = clip->min_x-sx;
/*TODO*///					sx += pixels;
/*TODO*///					x_index_base += pixels*dx;
/*TODO*///				}
/*TODO*///				if( sy < clip->min_y )
/*TODO*///				{ /* clip top */
/*TODO*///					int pixels = clip->min_y-sy;
/*TODO*///					sy += pixels;
/*TODO*///					y_index += pixels*dy;
/*TODO*///				}
/*TODO*///				/* NS 980211 - fixed incorrect clipping */
/*TODO*///				if( ex > clip->max_x+1 )
/*TODO*///				{ /* clip right */
/*TODO*///					int pixels = ex-clip->max_x-1;
/*TODO*///					ex -= pixels;
/*TODO*///				}
/*TODO*///				if( ey > clip->max_y+1 )
/*TODO*///				{ /* clip bottom */
/*TODO*///					int pixels = ey-clip->max_y-1;
/*TODO*///					ey -= pixels;
/*TODO*///				}
/*TODO*///			}
/*TODO*///
/*TODO*///			if( ex>sx )
/*TODO*///			{ /* skip if inner loop doesn't draw anything */
/*TODO*///				int y;
/*TODO*///
/*TODO*///				/* case 1: TRANSPARENCY_PEN */
/*TODO*///				if (transparency == TRANSPARENCY_PEN)
/*TODO*///				{
/*TODO*///					if (pri_buffer)
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT8 *dest = dest_bmp->line[y];
/*TODO*///							UINT8 *pri = pri_buffer->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color )
/*TODO*///								{
/*TODO*///									if (((1 << pri[x]) & pri_mask) == 0)
/*TODO*///										dest[x] = pal[c];
/*TODO*///									pri[x] = 31;
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT8 *dest = dest_bmp->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color ) dest[x] = pal[c];
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///				}
/*TODO*///
/*TODO*///				/* case 1b: TRANSPARENCY_PEN_RAW */
/*TODO*///				if (transparency == TRANSPARENCY_PEN_RAW)
/*TODO*///				{
/*TODO*///					if (pri_buffer)
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT8 *dest = dest_bmp->line[y];
/*TODO*///							UINT8 *pri = pri_buffer->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color )
/*TODO*///								{
/*TODO*///									if (((1 << pri[x]) & pri_mask) == 0)
/*TODO*///										dest[x] = color + c;
/*TODO*///									pri[x] = 31;
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT8 *dest = dest_bmp->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color ) dest[x] = color + c;
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///				}
/*TODO*///
/*TODO*///				/* case 2: TRANSPARENCY_PENS */
/*TODO*///				if (transparency == TRANSPARENCY_PENS)
/*TODO*///				{
/*TODO*///					if (pri_buffer)
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT8 *dest = dest_bmp->line[y];
/*TODO*///							UINT8 *pri = pri_buffer->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if (((1 << c) & transparent_color) == 0)
/*TODO*///								{
/*TODO*///									if (((1 << pri[x]) & pri_mask) == 0)
/*TODO*///										dest[x] = pal[c];
/*TODO*///									pri[x] = 31;
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT8 *dest = dest_bmp->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if (((1 << c) & transparent_color) == 0)
/*TODO*///									dest[x] = pal[c];
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///				}
/*TODO*///
/*TODO*///				/* case 3: TRANSPARENCY_COLOR */
/*TODO*///				else if (transparency == TRANSPARENCY_COLOR)
/*TODO*///				{
/*TODO*///					if (pri_buffer)
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT8 *dest = dest_bmp->line[y];
/*TODO*///							UINT8 *pri = pri_buffer->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = pal[source[x_index>>16]];
/*TODO*///								if( c != transparent_color )
/*TODO*///								{
/*TODO*///									if (((1 << pri[x]) & pri_mask) == 0)
/*TODO*///										dest[x] = c;
/*TODO*///									pri[x] = 31;
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT8 *dest = dest_bmp->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = pal[source[x_index>>16]];
/*TODO*///								if( c != transparent_color ) dest[x] = c;
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///				}
/*TODO*///
/*TODO*///				/* case 4: TRANSPARENCY_PEN_TABLE */
/*TODO*///				if (transparency == TRANSPARENCY_PEN_TABLE)
/*TODO*///				{
/*TODO*///					if (pri_buffer)
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT8 *dest = dest_bmp->line[y];
/*TODO*///							UINT8 *pri = pri_buffer->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color )
/*TODO*///								{
/*TODO*///									if (((1 << pri[x]) & pri_mask) == 0)
/*TODO*///									{
/*TODO*///										switch(gfx_drawmode_table[c])
/*TODO*///										{
/*TODO*///										case DRAWMODE_SOURCE:
/*TODO*///											dest[x] = pal[c];
/*TODO*///											break;
/*TODO*///										case DRAWMODE_SHADOW:
/*TODO*///											dest[x] = palette_shadow_table[dest[x]];
/*TODO*///											break;
/*TODO*///										}
/*TODO*///									}
/*TODO*///									pri[x] = 31;
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT8 *dest = dest_bmp->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color )
/*TODO*///								{
/*TODO*///									switch(gfx_drawmode_table[c])
/*TODO*///									{
/*TODO*///									case DRAWMODE_SOURCE:
/*TODO*///										dest[x] = pal[c];
/*TODO*///										break;
/*TODO*///									case DRAWMODE_SHADOW:
/*TODO*///										dest[x] = palette_shadow_table[dest[x]];
/*TODO*///										break;
/*TODO*///									}
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///				}
/*TODO*///
/*TODO*///				/* case 4b: TRANSPARENCY_PEN_TABLE_RAW */
/*TODO*///				if (transparency == TRANSPARENCY_PEN_TABLE_RAW)
/*TODO*///				{
/*TODO*///					if (pri_buffer)
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT8 *dest = dest_bmp->line[y];
/*TODO*///							UINT8 *pri = pri_buffer->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color )
/*TODO*///								{
/*TODO*///									if (((1 << pri[x]) & pri_mask) == 0)
/*TODO*///									{
/*TODO*///										switch(gfx_drawmode_table[c])
/*TODO*///										{
/*TODO*///										case DRAWMODE_SOURCE:
/*TODO*///											dest[x] = color + c;
/*TODO*///											break;
/*TODO*///										case DRAWMODE_SHADOW:
/*TODO*///											dest[x] = palette_shadow_table[dest[x]];
/*TODO*///											break;
/*TODO*///										}
/*TODO*///									}
/*TODO*///									pri[x] = 31;
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT8 *dest = dest_bmp->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color )
/*TODO*///								{
/*TODO*///									switch(gfx_drawmode_table[c])
/*TODO*///									{
/*TODO*///									case DRAWMODE_SOURCE:
/*TODO*///										dest[x] = color + c;
/*TODO*///										break;
/*TODO*///									case DRAWMODE_SHADOW:
/*TODO*///										dest[x] = palette_shadow_table[dest[x]];
/*TODO*///										break;
/*TODO*///									}
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* ASG 980209 -- new 16-bit part */
/*TODO*///	else
/*TODO*///	{
/*TODO*///		if( gfx && gfx->colortable )
/*TODO*///		{
/*TODO*///			const UINT16 *pal = &gfx->colortable[gfx->color_granularity * (color % gfx->total_colors)]; /* ASG 980209 */
/*TODO*///			int source_base = (code % gfx->total_elements) * gfx->height;
/*TODO*///
/*TODO*///			int sprite_screen_height = (scaley*gfx->height+0x8000)>>16;
/*TODO*///			int sprite_screen_width = (scalex*gfx->width+0x8000)>>16;
/*TODO*///
/*TODO*///			/* compute sprite increment per screen pixel */
/*TODO*///			int dx = (gfx->width<<16)/sprite_screen_width;
/*TODO*///			int dy = (gfx->height<<16)/sprite_screen_height;
/*TODO*///
/*TODO*///			int ex = sx+sprite_screen_width;
/*TODO*///			int ey = sy+sprite_screen_height;
/*TODO*///
/*TODO*///			int x_index_base;
/*TODO*///			int y_index;
/*TODO*///
/*TODO*///			if( flipx )
/*TODO*///			{
/*TODO*///				x_index_base = (sprite_screen_width-1)*dx;
/*TODO*///				dx = -dx;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				x_index_base = 0;
/*TODO*///			}
/*TODO*///
/*TODO*///			if( flipy )
/*TODO*///			{
/*TODO*///				y_index = (sprite_screen_height-1)*dy;
/*TODO*///				dy = -dy;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				y_index = 0;
/*TODO*///			}
/*TODO*///
/*TODO*///			if( clip )
/*TODO*///			{
/*TODO*///				if( sx < clip->min_x)
/*TODO*///				{ /* clip left */
/*TODO*///					int pixels = clip->min_x-sx;
/*TODO*///					sx += pixels;
/*TODO*///					x_index_base += pixels*dx;
/*TODO*///				}
/*TODO*///				if( sy < clip->min_y )
/*TODO*///				{ /* clip top */
/*TODO*///					int pixels = clip->min_y-sy;
/*TODO*///					sy += pixels;
/*TODO*///					y_index += pixels*dy;
/*TODO*///				}
/*TODO*///				/* NS 980211 - fixed incorrect clipping */
/*TODO*///				if( ex > clip->max_x+1 )
/*TODO*///				{ /* clip right */
/*TODO*///					int pixels = ex-clip->max_x-1;
/*TODO*///					ex -= pixels;
/*TODO*///				}
/*TODO*///				if( ey > clip->max_y+1 )
/*TODO*///				{ /* clip bottom */
/*TODO*///					int pixels = ey-clip->max_y-1;
/*TODO*///					ey -= pixels;
/*TODO*///				}
/*TODO*///			}
/*TODO*///
/*TODO*///			if( ex>sx )
/*TODO*///			{ /* skip if inner loop doesn't draw anything */
/*TODO*///				int y;
/*TODO*///
/*TODO*///				/* case 1: TRANSPARENCY_PEN */
/*TODO*///				if (transparency == TRANSPARENCY_PEN)
/*TODO*///				{
/*TODO*///					if (pri_buffer)
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT16 *dest = (UINT16 *)dest_bmp->line[y];
/*TODO*///							UINT8 *pri = pri_buffer->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color )
/*TODO*///								{
/*TODO*///									if (((1 << pri[x]) & pri_mask) == 0)
/*TODO*///										dest[x] = pal[c];
/*TODO*///									pri[x] = 31;
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT16 *dest = (UINT16 *)dest_bmp->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color ) dest[x] = pal[c];
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///				}
/*TODO*///
/*TODO*///				/* case 1b: TRANSPARENCY_PEN_RAW */
/*TODO*///				if (transparency == TRANSPARENCY_PEN_RAW)
/*TODO*///				{
/*TODO*///					if (pri_buffer)
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT16 *dest = (UINT16 *)dest_bmp->line[y];
/*TODO*///							UINT8 *pri = pri_buffer->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color )
/*TODO*///								{
/*TODO*///									if (((1 << pri[x]) & pri_mask) == 0)
/*TODO*///										dest[x] = color + c;
/*TODO*///									pri[x] = 31;
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT16 *dest = (UINT16 *)dest_bmp->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color ) dest[x] = color + c;
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///				}
/*TODO*///
/*TODO*///				/* case 2: TRANSPARENCY_PENS */
/*TODO*///				if (transparency == TRANSPARENCY_PENS)
/*TODO*///				{
/*TODO*///					if (pri_buffer)
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT16 *dest = (UINT16 *)dest_bmp->line[y];
/*TODO*///							UINT8 *pri = pri_buffer->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if (((1 << c) & transparent_color) == 0)
/*TODO*///								{
/*TODO*///									if (((1 << pri[x]) & pri_mask) == 0)
/*TODO*///										dest[x] = pal[c];
/*TODO*///									pri[x] = 31;
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT16 *dest = (UINT16 *)dest_bmp->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if (((1 << c) & transparent_color) == 0)
/*TODO*///									dest[x] = pal[c];
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///				}
/*TODO*///
/*TODO*///				/* case 3: TRANSPARENCY_COLOR */
/*TODO*///				else if (transparency == TRANSPARENCY_COLOR)
/*TODO*///				{
/*TODO*///					if (pri_buffer)
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT16 *dest = (UINT16 *)dest_bmp->line[y];
/*TODO*///							UINT8 *pri = pri_buffer->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = pal[source[x_index>>16]];
/*TODO*///								if( c != transparent_color )
/*TODO*///								{
/*TODO*///									if (((1 << pri[x]) & pri_mask) == 0)
/*TODO*///										dest[x] = c;
/*TODO*///									pri[x] = 31;
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT16 *dest = (UINT16 *)dest_bmp->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = pal[source[x_index>>16]];
/*TODO*///								if( c != transparent_color ) dest[x] = c;
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///				}
/*TODO*///
/*TODO*///				/* case 4: TRANSPARENCY_PEN_TABLE */
/*TODO*///				if (transparency == TRANSPARENCY_PEN_TABLE)
/*TODO*///				{
/*TODO*///					if (pri_buffer)
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT16 *dest = (UINT16 *)dest_bmp->line[y];
/*TODO*///							UINT8 *pri = pri_buffer->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color )
/*TODO*///								{
/*TODO*///									if (((1 << pri[x]) & pri_mask) == 0)
/*TODO*///									{
/*TODO*///										switch(gfx_drawmode_table[c])
/*TODO*///										{
/*TODO*///										case DRAWMODE_SOURCE:
/*TODO*///											dest[x] = pal[c];
/*TODO*///											break;
/*TODO*///										case DRAWMODE_SHADOW:
/*TODO*///											dest[x] = palette_shadow_table[dest[x]];
/*TODO*///											break;
/*TODO*///										}
/*TODO*///									}
/*TODO*///									pri[x] = 31;
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT16 *dest = (UINT16 *)dest_bmp->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color )
/*TODO*///								{
/*TODO*///									switch(gfx_drawmode_table[c])
/*TODO*///									{
/*TODO*///									case DRAWMODE_SOURCE:
/*TODO*///										dest[x] = pal[c];
/*TODO*///										break;
/*TODO*///									case DRAWMODE_SHADOW:
/*TODO*///										dest[x] = palette_shadow_table[dest[x]];
/*TODO*///										break;
/*TODO*///									}
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///				}
/*TODO*///
/*TODO*///				/* case 4b: TRANSPARENCY_PEN_TABLE_RAW */
/*TODO*///				if (transparency == TRANSPARENCY_PEN_TABLE_RAW)
/*TODO*///				{
/*TODO*///					if (pri_buffer)
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT16 *dest = (UINT16 *)dest_bmp->line[y];
/*TODO*///							UINT8 *pri = pri_buffer->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color )
/*TODO*///								{
/*TODO*///									if (((1 << pri[x]) & pri_mask) == 0)
/*TODO*///									{
/*TODO*///										switch(gfx_drawmode_table[c])
/*TODO*///										{
/*TODO*///										case DRAWMODE_SOURCE:
/*TODO*///											dest[x] = color + c;
/*TODO*///											break;
/*TODO*///										case DRAWMODE_SHADOW:
/*TODO*///											dest[x] = palette_shadow_table[dest[x]];
/*TODO*///											break;
/*TODO*///										}
/*TODO*///									}
/*TODO*///									pri[x] = 31;
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						for( y=sy; y<ey; y++ )
/*TODO*///						{
/*TODO*///							UINT8 *source = gfx->gfxdata + (source_base+(y_index>>16)) * gfx->line_modulo;
/*TODO*///							UINT16 *dest = (UINT16 *)dest_bmp->line[y];
/*TODO*///
/*TODO*///							int x, x_index = x_index_base;
/*TODO*///							for( x=sx; x<ex; x++ )
/*TODO*///							{
/*TODO*///								int c = source[x_index>>16];
/*TODO*///								if( c != transparent_color )
/*TODO*///								{
/*TODO*///									switch(gfx_drawmode_table[c])
/*TODO*///									{
/*TODO*///									case DRAWMODE_SOURCE:
/*TODO*///										dest[x] = color + c;
/*TODO*///										break;
/*TODO*///									case DRAWMODE_SHADOW:
/*TODO*///										dest[x] = palette_shadow_table[dest[x]];
/*TODO*///										break;
/*TODO*///									}
/*TODO*///								}
/*TODO*///								x_index += dx;
/*TODO*///							}
/*TODO*///
/*TODO*///							y_index += dy;
/*TODO*///						}
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///void drawgfxzoom( struct osd_bitmap *dest_bmp,const struct GfxElement *gfx,
/*TODO*///		unsigned int code,unsigned int color,int flipx,int flipy,int sx,int sy,
/*TODO*///		const struct rectangle *clip,int transparency,int transparent_color,int scalex, int scaley)
/*TODO*///{
/*TODO*///	profiler_mark(PROFILER_DRAWGFX);
/*TODO*///	common_drawgfxzoom(dest_bmp,gfx,code,color,flipx,flipy,sx,sy,
/*TODO*///			clip,transparency,transparent_color,scalex,scaley,NULL,0);
/*TODO*///	profiler_mark(PROFILER_END);
/*TODO*///}
/*TODO*///
/*TODO*///void pdrawgfxzoom( struct osd_bitmap *dest_bmp,const struct GfxElement *gfx,
/*TODO*///		unsigned int code,unsigned int color,int flipx,int flipy,int sx,int sy,
/*TODO*///		const struct rectangle *clip,int transparency,int transparent_color,int scalex, int scaley,
/*TODO*///		UINT32 priority_mask)
/*TODO*///{
/*TODO*///	profiler_mark(PROFILER_DRAWGFX);
/*TODO*///	common_drawgfxzoom(dest_bmp,gfx,code,color,flipx,flipy,sx,sy,
/*TODO*///			clip,transparency,transparent_color,scalex,scaley,priority_bitmap,priority_mask);
/*TODO*///	profiler_mark(PROFILER_END);
/*TODO*///}
/*TODO*///
/*TODO*///
    public static void plot_pixel2(mame_bitmap bitmap1, mame_bitmap bitmap2, int x, int y, int pen) {
        plot_pixel.handler(bitmap1, x, y, pen);
        plot_pixel.handler(bitmap2, x, y, pen);
    }

    public static plot_pixel_procPtr pp_8_nd = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            b.line[y].write(x, p);
        }
    };
    public static plot_pixel_procPtr pp_8_nd_fx = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            b.line[y].write(b.width - 1 - x, p);
        }
    };
    public static plot_pixel_procPtr pp_8_nd_fy = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            b.line[b.height - 1 - y].write(x, p);
        }
    };
    public static plot_pixel_procPtr pp_8_nd_fxy = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            b.line[b.height - 1 - y].write(b.width - 1 - x, p);
        }
    };
    public static plot_pixel_procPtr pp_8_nd_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            b.line[x].write(y, p);
        }
    };
    public static plot_pixel_procPtr pp_8_nd_fx_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            b.line[x].write(b.width - 1 - y, p);
        }
    };
    public static plot_pixel_procPtr pp_8_nd_fy_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            b.line[b.height - 1 - x].write(y, p);
        }
    };
    public static plot_pixel_procPtr pp_8_nd_fxy_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            b.line[b.height - 1 - x].write(b.width - 1 - y, p);
        }
    };

    public static plot_pixel_procPtr pp_8_d = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            b.line[y].write(x, p);
            osd_mark_dirty(x, y, x, y, 0);
        }
    };
    public static plot_pixel_procPtr pp_8_d_fx = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            x = b.width - 1 - x;
            b.line[y].write(x, p);
            osd_mark_dirty(x, y, x, y, 0);
        }
    };
    public static plot_pixel_procPtr pp_8_d_fy = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            y = b.height - 1 - y;
            b.line[y].write(x, p);
            osd_mark_dirty(x, y, x, y, 0);
        }
    };
    public static plot_pixel_procPtr pp_8_d_fxy = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            x = b.width - 1 - x;
            y = b.height - 1 - y;
            b.line[y].write(x, p);
            osd_mark_dirty(x, y, x, y, 0);
        }
    };
    public static plot_pixel_procPtr pp_8_d_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            b.line[x].write(y, p);
            osd_mark_dirty(y, x, y, x, 0);
        }
    };
    public static plot_pixel_procPtr pp_8_d_fx_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            y = b.width - 1 - y;
            b.line[x].write(y, p);
            osd_mark_dirty(y, x, y, x, 0);
        }
    };
    public static plot_pixel_procPtr pp_8_d_fy_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            x = b.height - 1 - x;
            b.line[x].write(y, p);
            osd_mark_dirty(y, x, y, x, 0);
        }
    };
    public static plot_pixel_procPtr pp_8_d_fxy_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            x = b.height - 1 - x;
            y = b.width - 1 - y;
            b.line[x].write(y, p);
            osd_mark_dirty(y, x, y, x, 0);
        }
    };
    public static plot_pixel_procPtr pp_16_nd = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///           ((UINT16 *) b.line[y])[x]=p;
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static plot_pixel_procPtr pp_16_nd_fx = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///            ((UINT16 *) b.line[y])[b.width - 1 - x]=p;
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static plot_pixel_procPtr pp_16_nd_fy = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///           ((UINT16 *) b.line[b.height - 1 - y])[x]=p;
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static plot_pixel_procPtr pp_16_nd_fxy = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///           ((UINT16 *) b.line[b.height - 1 - y])[b.width - 1 - x]=p;
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static plot_pixel_procPtr pp_16_nd_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///            ((UINT16 *) b.line[x])[y]=p;
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static plot_pixel_procPtr pp_16_nd_fx_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///            ((UINT16 *) b.line[x])[b.width - 1 - y]=p;
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static plot_pixel_procPtr pp_16_nd_fy_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///            ((UINT16 *) b.line[b.height - 1 - x])[y]=p;
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static plot_pixel_procPtr pp_16_nd_fxy_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///            ((UINT16 *) b.line[b.height - 1 - x])[b.width - 1 - y]=p;
            throw new UnsupportedOperationException("Unsupported");
        }
    };

    public static plot_pixel_procPtr pp_16_d = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///             ((UINT16 *) b.line[y])[x]=p;
/*TODO*///            osd_mark_dirty(x, y, x, y, 0);
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static plot_pixel_procPtr pp_16_d_fx = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///            x = b.width - 1 - x;
/*TODO*///             ((UINT16 *) b.line[y])[x]=p;
/*TODO*///            osd_mark_dirty(x, y, x, y, 0);
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static plot_pixel_procPtr pp_16_d_fy = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///           y = b.height - 1 - y;
            /*TODO*///            ((UINT16 *) b.line[y])[x]=p;
            /*TODO*///           osd_mark_dirty(x, y, x, y, 0);
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static plot_pixel_procPtr pp_16_d_fxy = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///            x = b.width - 1 - x;
/*TODO*///            y = b.height - 1 - y;
/*TODO*///             ((UINT16 *) b.line[y])[x]=p;
/*TODO*///            osd_mark_dirty(x, y, x, y, 0);
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static plot_pixel_procPtr pp_16_d_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///            ((UINT16 *) b.line[x])[y]=p;
/*TODO*///            osd_mark_dirty(y, x, y, x, 0);
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static plot_pixel_procPtr pp_16_d_fx_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///           y = b.width - 1 - y;
/*TODO*///             ((UINT16 *) b.line[x])[y]=p;
/*TODO*///            osd_mark_dirty(y, x, y, x, 0);
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static plot_pixel_procPtr pp_16_d_fy_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///           x = b.height - 1 - x;
            /*TODO*///            ((UINT16 *) b.line[x])[y]=p;
            /*TODO*///           osd_mark_dirty(y, x, y, x, 0);
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static plot_pixel_procPtr pp_16_d_fxy_s = new plot_pixel_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int p) {
            /*TODO*///           x = b.height - 1 - x;
            /*TODO*///           y = b.width - 1 - y;
            /*TODO*///            ((UINT16 *) b.line[x])[y]=p;
            /*TODO*///           osd_mark_dirty(y, x, y, x, 0);
            throw new UnsupportedOperationException("Unsupported");
        }
    };
    public static read_pixel_procPtr rp_8 = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            return bitmap.line[y].read(x);
        }
    };
    public static read_pixel_procPtr rp_8_fx = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            return bitmap.line[y].read(bitmap.width - 1 - x);
        }
    };
    public static read_pixel_procPtr rp_8_fy = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            return bitmap.line[bitmap.height - 1 - y].read(x);
        }
    };
    public static read_pixel_procPtr rp_8_fxy = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            return bitmap.line[bitmap.height - 1 - y].read(bitmap.width - 1 - x);
        }
    };
    public static read_pixel_procPtr rp_8_s = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            return bitmap.line[x].read(y);
        }
    };
    public static read_pixel_procPtr rp_8_fx_s = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            return bitmap.line[x].read(bitmap.width - 1 - y);
        }
    };
    public static read_pixel_procPtr rp_8_fy_s = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            return bitmap.line[bitmap.height - 1 - x].read(y);
        }
    };
    public static read_pixel_procPtr rp_8_fxy_s = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            return bitmap.line[bitmap.height - 1 - x].read(bitmap.width - 1 - y);
        }
    };
    /*TODO*///static int rp_16(struct osd_bitmap *b,int x,int y)  { return ((unsigned short *)b->line[y])[x]; }
    public static read_pixel_procPtr rp_16 = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            throw new UnsupportedOperationException("Unsupported rp_16");
        }
    };
    /*TODO*///static int rp_16_fx(struct osd_bitmap *b,int x,int y)  { return ((unsigned short *)b->line[y])[b->width-1-x]; }
    public static read_pixel_procPtr rp_16_fx = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            throw new UnsupportedOperationException("Unsupported rp_16_fx");
        }
    };
    /*TODO*///static int rp_16_fy(struct osd_bitmap *b,int x,int y)  { return ((unsigned short *)b->line[b->height-1-y])[x]; }
    public static read_pixel_procPtr rp_16_fy = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            throw new UnsupportedOperationException("Unsupported rp_16_fy");
        }
    };
    /*TODO*///static int rp_16_fxy(struct osd_bitmap *b,int x,int y)  { return ((unsigned short *)b->line[b->height-1-y])[b->width-1-x]; }
    public static read_pixel_procPtr rp_16_fxy = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            throw new UnsupportedOperationException("Unsupported rp_16_fxy");
        }
    };
    /*TODO*///static int rp_16_s(struct osd_bitmap *b,int x,int y)  { return ((unsigned short *)b->line[x])[y]; }
    public static read_pixel_procPtr rp_16_s = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            throw new UnsupportedOperationException("Unsupported rp_16_s");
        }
    };
    /*TODO*///static int rp_16_fx_s(struct osd_bitmap *b,int x,int y)  { return ((unsigned short *)b->line[x])[b->width-1-y]; }
    public static read_pixel_procPtr rp_16_fx_s = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            throw new UnsupportedOperationException("Unsupported rp_16_fx_s");
        }
    };
    /*TODO*///static int rp_16_fy_s(struct osd_bitmap *b,int x,int y)  { return ((unsigned short *)b->line[b->height-1-x])[y]; }
    public static read_pixel_procPtr rp_16_fy_s = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            throw new UnsupportedOperationException("Unsupported rp_16_fy_s");
        }
    };
    /*TODO*///static int rp_16_fxy_s(struct osd_bitmap *b,int x,int y)  { return ((unsigned short *)b->line[b->height-1-x])[b->width-1-y]; }
    public static read_pixel_procPtr rp_16_fxy_s = new read_pixel_procPtr() {
        public int handler(mame_bitmap bitmap, int x, int y) {
            throw new UnsupportedOperationException("Unsupported rp_16_fxy_s");
        }
    };
    public static plot_box_procPtr pb_8_nd = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = x;
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[y].write(x, p);
                    x++;
                }
                y++;
            }
        }
    };
    public static plot_box_procPtr pb_8_nd_fx = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = b.width - 1 - x;
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[y].write(x, p);
                    x--;
                }
                y++;
            }
        }
    };
    public static plot_box_procPtr pb_8_nd_fy = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = x;
            y = b.height - 1 - y;
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[y].write(x, p);
                    x++;
                }
                y--;
            }
        }
    };
    public static plot_box_procPtr pb_8_nd_fxy = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = b.width - 1 - x;
            y = b.height - 1 - y;
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[y].write(x, p);
                    x--;
                }
                y--;
            }
        }
    };
    public static plot_box_procPtr pb_8_nd_s = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = x;
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[x].write(y, p);
                    x++;
                }
                y++;
            }
        }
    };
    public static plot_box_procPtr pb_8_nd_fx_s = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = x;
            y = b.width - 1 - y;
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[x].write(y, p);
                    x++;
                }
                y--;
            }
        }
    };
    public static plot_box_procPtr pb_8_nd_fy_s = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = b.height - 1 - x;
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[x].write(y, p);
                    x--;
                }
                y++;
            }
        }
    };
    public static plot_box_procPtr pb_8_nd_fxy_s = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = b.height - 1 - x;
            y = b.width - 1 - y;
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[x].write(y, p);
                    x--;
                }
                y--;
            }
        }
    };

    public static plot_box_procPtr pb_8_d = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = x;
            osd_mark_dirty(t, y, t + w - 1, y + h - 1, 0);
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[y].write(x, p);
                    x++;
                }
                y++;
            }
        }
    };
    public static plot_box_procPtr pb_8_d_fx = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = b.width - 1 - x;
            osd_mark_dirty(t - w + 1, y, t, y + h - 1, 0);
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[y].write(x, p);
                    x--;
                }
                y++;
            }
        }
    };
    public static plot_box_procPtr pb_8_d_fy = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = x;
            y = b.height - 1 - y;
            osd_mark_dirty(t, y - h + 1, t + w - 1, y, 0);
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[y].write(x, p);
                    x++;
                }
                y--;
            }
        }
    };
    public static plot_box_procPtr pb_8_d_fxy = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = b.width - 1 - x;
            y = b.height - 1 - y;
            osd_mark_dirty(t - w + 1, y - h + 1, t, y, 0);
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[y].write(x, p);
                    x--;
                }
                y--;
            }
        }
    };
    public static plot_box_procPtr pb_8_d_s = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = x;
            osd_mark_dirty(y, t, y + h - 1, t + w - 1, 0);
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[x].write(y, p);
                    x++;
                }
                y++;
            }
        }
    };
    public static plot_box_procPtr pb_8_d_fx_s = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = x;
            y = b.width - 1 - y;
            osd_mark_dirty(y - h + 1, t, y, t + w - 1, 0);
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[x].write(y, p);
                    x++;
                }
                y--;
            }
        }
    };
    public static plot_box_procPtr pb_8_d_fy_s = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = b.height - 1 - x;
            osd_mark_dirty(y, t - w + 1, y + h - 1, t, 0);
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[x].write(y, p);
                    x--;
                }
                y++;
            }
        }
    };
    public static plot_box_procPtr pb_8_d_fxy_s = new plot_box_procPtr() {
        public void handler(mame_bitmap b, int x, int y, int w, int h, int p) {
            int t = b.height - 1 - x;
            y = b.width - 1 - y;
            osd_mark_dirty(y - h + 1, t - w + 1, y, t, 0);
            while (h-- > 0) {
                int c = w;
                x = t;
                while (c-- > 0) {
                    b.line[x].write(y, p);
                    x--;
                }
                y--;
            }
        }
    };
    /*TODO*///static void pb_16_nd(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=x; while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[y])[x] = p; x++; } y++; } }
/*TODO*///static void pb_16_nd_fx(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=b->width-1-x; while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[y])[x] = p; x--; } y++; } }
/*TODO*///static void pb_16_nd_fy(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=x; y = b->height-1-y; while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[y])[x] = p; x++; } y--; } }
/*TODO*///static void pb_16_nd_fxy(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=b->width-1-x; y = b->height-1-y; while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[y])[x] = p; x--; } y--; } }
/*TODO*///static void pb_16_nd_s(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=x; while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[x])[y] = p; x++; } y++; } }
/*TODO*///static void pb_16_nd_fx_s(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=x; y = b->width-1-y; while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[x])[y] = p; x++; } y--; } }
/*TODO*///static void pb_16_nd_fy_s(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=b->height-1-x; while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[x])[y] = p; x--; } y++; } }
/*TODO*///static void pb_16_nd_fxy_s(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=b->height-1-x; y = b->width-1-y; while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[x])[y] = p; x--; } y--; } }
/*TODO*///
/*TODO*///static void pb_16_d(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=x; osd_mark_dirty (t,y,t+w-1,y+h-1,0); while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[y])[x] = p; x++; } y++; } }
/*TODO*///static void pb_16_d_fx(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=b->width-1-x;  osd_mark_dirty (t-w+1,y,t,y+h-1,0); while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[y])[x] = p; x--; } y++; } }
/*TODO*///static void pb_16_d_fy(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=x; y = b->height-1-y; osd_mark_dirty (t,y-h+1,t+w-1,y,0); while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[y])[x] = p; x++; } y--; } }
/*TODO*///static void pb_16_d_fxy(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=b->width-1-x; y = b->height-1-y; osd_mark_dirty (t-w+1,y-h+1,t,y,0); while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[y])[x] = p; x--; } y--; } }
/*TODO*///static void pb_16_d_s(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=x; osd_mark_dirty (y,t,y+h-1,t+w-1,0); while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[x])[y] = p; x++; } y++; } }
/*TODO*///static void pb_16_d_fx_s(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=x; y = b->width-1-y; osd_mark_dirty (y-h+1,t,y,t+w-1,0); while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[x])[y] = p; x++; } y--; } }
/*TODO*///static void pb_16_d_fy_s(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=b->height-1-x; osd_mark_dirty (y,t-w+1,y+h-1,t,0); while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[x])[y] = p; x--; } y++; } }
/*TODO*///static void pb_16_d_fxy_s(struct osd_bitmap *b,int x,int y,int w,int h,int p)  { int t=b->height-1-x; y = b->width-1-y; osd_mark_dirty (y-h+1,t-w+1,y,t,0); while(h-->0){ int c=w; x=t; while(c-->0){ ((UINT16 *)b->line[x])[y] = p; x--; } y--; } }
/*TODO*///

    static plot_pixel_procPtr pps_8_nd[]
            = {pp_8_nd, pp_8_nd_fx, pp_8_nd_fy, pp_8_nd_fxy,
                pp_8_nd_s, pp_8_nd_fx_s, pp_8_nd_fy_s, pp_8_nd_fxy_s};

    static plot_pixel_procPtr pps_8_d[]
            = {pp_8_d, pp_8_d_fx, pp_8_d_fy, pp_8_d_fxy,
                pp_8_d_s, pp_8_d_fx_s, pp_8_d_fy_s, pp_8_d_fxy_s};

    /*TODO*///static plot_pixel_proc pps_16_nd[] =
/*TODO*///		{ pp_16_nd,   pp_16_nd_fx,   pp_16_nd_fy, 	pp_16_nd_fxy,
/*TODO*///		  pp_16_nd_s, pp_16_nd_fx_s, pp_16_nd_fy_s, pp_16_nd_fxy_s };
/*TODO*///
/*TODO*///static plot_pixel_proc pps_16_d[] =
/*TODO*///		{ pp_16_d,   pp_16_d_fx,   pp_16_d_fy, 	 pp_16_d_fxy,
/*TODO*///		  pp_16_d_s, pp_16_d_fx_s, pp_16_d_fy_s, pp_16_d_fxy_s };
/*TODO*///
/*TODO*///
    static read_pixel_procPtr rps_8[]
            = {rp_8, rp_8_fx, rp_8_fy, rp_8_fxy,
                rp_8_s, rp_8_fx_s, rp_8_fy_s, rp_8_fxy_s};

    /*TODO*///static read_pixel_proc rps_16[] =
/*TODO*///		{ rp_16,   rp_16_fx,   rp_16_fy,   rp_16_fxy,
/*TODO*///		  rp_16_s, rp_16_fx_s, rp_16_fy_s, rp_16_fxy_s };
/*TODO*///
    static plot_box_procPtr pbs_8_nd[]
            = {pb_8_nd, pb_8_nd_fx, pb_8_nd_fy, pb_8_nd_fxy,
                pb_8_nd_s, pb_8_nd_fx_s, pb_8_nd_fy_s, pb_8_nd_fxy_s};

    static plot_box_procPtr pbs_8_d[]
            = {pb_8_d, pb_8_d_fx, pb_8_d_fy, pb_8_d_fxy,
                pb_8_d_s, pb_8_d_fx_s, pb_8_d_fy_s, pb_8_d_fxy_s};

    /*TODO*///static plot_box_proc pbs_16_nd[] =
/*TODO*///		{ pb_16_nd,   pb_16_nd_fx,   pb_16_nd_fy, 	pb_16_nd_fxy,
/*TODO*///		  pb_16_nd_s, pb_16_nd_fx_s, pb_16_nd_fy_s, pb_16_nd_fxy_s };
/*TODO*///
/*TODO*///static plot_box_proc pbs_16_d[] =
/*TODO*///		{ pb_16_d,   pb_16_d_fx,   pb_16_d_fy, 	 pb_16_d_fxy,
/*TODO*///		  pb_16_d_s, pb_16_d_fx_s, pb_16_d_fy_s, pb_16_d_fxy_s };
/*TODO*///
/*TODO*///
    public static void set_pixel_functions() {
        if (Machine.color_depth == 8) {
            read_pixel = rps_8[Machine.orientation];

            if ((Machine.drv.video_attributes & VIDEO_SUPPORTS_DIRTY) != 0) {
                plot_pixel = pps_8_d[Machine.orientation];
                plot_box = pbs_8_d[Machine.orientation];
            } else {
                plot_pixel = pps_8_nd[Machine.orientation];
                plot_box = pbs_8_nd[Machine.orientation];
            }
        } else {
            throw new UnsupportedOperationException("Unsupported");
            /*TODO*///		read_pixel = rps_16[Machine->orientation];
/*TODO*///
/*TODO*///		if (Machine->drv->video_attributes & VIDEO_SUPPORTS_DIRTY)
/*TODO*///		{
/*TODO*///			plot_pixel = pps_16_d[Machine->orientation];
/*TODO*///			plot_box = pbs_16_d[Machine->orientation];
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			plot_pixel = pps_16_nd[Machine->orientation];
/*TODO*///			plot_box = pbs_16_nd[Machine->orientation];
/*TODO*///		}
        }

        /* while we're here, fill in the raw drawing mode table as well */
        is_raw[TRANSPARENCY_NONE_RAW] = 1;
        is_raw[TRANSPARENCY_PEN_RAW] = 1;
        is_raw[TRANSPARENCY_PENS_RAW] = 1;
        is_raw[TRANSPARENCY_PEN_TABLE_RAW] = 1;
        is_raw[TRANSPARENCY_BLEND_RAW] = 1;
    }

    public static void blockmove_8toN_opaque8(UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo, UBytePtr dstdata, int dstmodulo, IntArray paldata) {
        int end;

        srcmodulo -= srcwidth;
        dstmodulo -= srcwidth;

        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while (dstdata.offset <= end - 8) {
                dstdata.write(0, paldata.read(srcdata.read(0)));
                dstdata.write(1, paldata.read(srcdata.read(1)));
                dstdata.write(2, paldata.read(srcdata.read(2)));
                dstdata.write(3, paldata.read(srcdata.read(3)));
                dstdata.write(4, paldata.read(srcdata.read(4)));
                dstdata.write(5, paldata.read(srcdata.read(5)));
                dstdata.write(6, paldata.read(srcdata.read(6)));
                dstdata.write(7, paldata.read(srcdata.read(7)));
                dstdata.inc(8);
                srcdata.inc(8);
            }
            while (dstdata.offset < end) {
                dstdata.writeinc(paldata.read(srcdata.readinc()));
            }

            srcdata.inc(srcmodulo);
            dstdata.inc(dstmodulo);
            srcheight--;
        }
    }

    /*TODO*///DECLARE(blockmove_8toN_opaque,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata <= end - 8)
/*TODO*///		{
/*TODO*///			dstdata[0] = paldata[srcdata[0]];
/*TODO*///			dstdata[1] = paldata[srcdata[1]];
/*TODO*///			dstdata[2] = paldata[srcdata[2]];
/*TODO*///			dstdata[3] = paldata[srcdata[3]];
/*TODO*///			dstdata[4] = paldata[srcdata[4]];
/*TODO*///			dstdata[5] = paldata[srcdata[5]];
/*TODO*///			dstdata[6] = paldata[srcdata[6]];
/*TODO*///			dstdata[7] = paldata[srcdata[7]];
/*TODO*///			dstdata += 8;
/*TODO*///			srcdata += 8;
/*TODO*///		}
/*TODO*///		while (dstdata < end)
/*TODO*///			*(dstdata++) = paldata[*(srcdata++)];
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
    public static void blockmove_8toN_opaque_flipx8(UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo, UBytePtr dstdata, int dstmodulo, IntArray paldata) {
        int end;

        srcmodulo += srcwidth;
        dstmodulo -= srcwidth;
        //srcdata += srcwidth-1;

        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while (dstdata.offset <= end - 8) {
                srcdata.dec(8);
                dstdata.write(0, paldata.read(srcdata.read(8)));
                dstdata.write(1, paldata.read(srcdata.read(7)));
                dstdata.write(2, paldata.read(srcdata.read(6)));
                dstdata.write(3, paldata.read(srcdata.read(5)));
                dstdata.write(4, paldata.read(srcdata.read(4)));
                dstdata.write(5, paldata.read(srcdata.read(3)));
                dstdata.write(6, paldata.read(srcdata.read(2)));
                dstdata.write(7, paldata.read(srcdata.read(1)));
                dstdata.inc(8);
            }
            while (dstdata.offset < end) {
                dstdata.writeinc(paldata.read(srcdata.readdec()));
            }

            srcdata.inc(srcmodulo);
            dstdata.inc(dstmodulo);
            srcheight--;
        }
    }

    /*TODO*///DECLARE(blockmove_8toN_opaque_flipx,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata <= end - 8)
/*TODO*///		{
/*TODO*///			srcdata -= 8;
/*TODO*///			dstdata[0] = paldata[srcdata[8]];
/*TODO*///			dstdata[1] = paldata[srcdata[7]];
/*TODO*///			dstdata[2] = paldata[srcdata[6]];
/*TODO*///			dstdata[3] = paldata[srcdata[5]];
/*TODO*///			dstdata[4] = paldata[srcdata[4]];
/*TODO*///			dstdata[5] = paldata[srcdata[3]];
/*TODO*///			dstdata[6] = paldata[srcdata[2]];
/*TODO*///			dstdata[7] = paldata[srcdata[1]];
/*TODO*///			dstdata += 8;
/*TODO*///		}
/*TODO*///		while (dstdata < end)
/*TODO*///			*(dstdata++) = paldata[*(srcdata--)];
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_opaque_pri,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,UINT8 *pridata,UINT32 pmask),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	pmask |= (1<<31);
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata <= end - 8)
/*TODO*///		{
/*TODO*///			if (((1 << pridata[0]) & pmask) == 0) dstdata[0] = paldata[srcdata[0]];
/*TODO*///			if (((1 << pridata[1]) & pmask) == 0) dstdata[1] = paldata[srcdata[1]];
/*TODO*///			if (((1 << pridata[2]) & pmask) == 0) dstdata[2] = paldata[srcdata[2]];
/*TODO*///			if (((1 << pridata[3]) & pmask) == 0) dstdata[3] = paldata[srcdata[3]];
/*TODO*///			if (((1 << pridata[4]) & pmask) == 0) dstdata[4] = paldata[srcdata[4]];
/*TODO*///			if (((1 << pridata[5]) & pmask) == 0) dstdata[5] = paldata[srcdata[5]];
/*TODO*///			if (((1 << pridata[6]) & pmask) == 0) dstdata[6] = paldata[srcdata[6]];
/*TODO*///			if (((1 << pridata[7]) & pmask) == 0) dstdata[7] = paldata[srcdata[7]];
/*TODO*///			memset(pridata,31,8);
/*TODO*///			srcdata += 8;
/*TODO*///			dstdata += 8;
/*TODO*///			pridata += 8;
/*TODO*///		}
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			if (((1 << *pridata) & pmask) == 0)
/*TODO*///				*dstdata = paldata[*srcdata];
/*TODO*///			*pridata = 31;
/*TODO*///			srcdata++;
/*TODO*///			dstdata++;
/*TODO*///			pridata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		pridata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_opaque_pri_flipx,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,UINT8 *pridata,UINT32 pmask),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	pmask |= (1<<31);
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata <= end - 8)
/*TODO*///		{
/*TODO*///			srcdata -= 8;
/*TODO*///			if (((1 << pridata[0]) & pmask) == 0) dstdata[0] = paldata[srcdata[8]];
/*TODO*///			if (((1 << pridata[1]) & pmask) == 0) dstdata[1] = paldata[srcdata[7]];
/*TODO*///			if (((1 << pridata[2]) & pmask) == 0) dstdata[2] = paldata[srcdata[6]];
/*TODO*///			if (((1 << pridata[3]) & pmask) == 0) dstdata[3] = paldata[srcdata[5]];
/*TODO*///			if (((1 << pridata[4]) & pmask) == 0) dstdata[4] = paldata[srcdata[4]];
/*TODO*///			if (((1 << pridata[5]) & pmask) == 0) dstdata[5] = paldata[srcdata[3]];
/*TODO*///			if (((1 << pridata[6]) & pmask) == 0) dstdata[6] = paldata[srcdata[2]];
/*TODO*///			if (((1 << pridata[7]) & pmask) == 0) dstdata[7] = paldata[srcdata[1]];
/*TODO*///			memset(pridata,31,8);
/*TODO*///			dstdata += 8;
/*TODO*///			pridata += 8;
/*TODO*///		}
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			if (((1 << *pridata) & pmask) == 0)
/*TODO*///				*dstdata = paldata[*srcdata];
/*TODO*///			*pridata = 31;
/*TODO*///			srcdata--;
/*TODO*///			dstdata++;
/*TODO*///			pridata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		pridata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///
    public static void blockmove_8toN_opaque_raw(
            UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo,
            UBytePtr dstdata, int dstmodulo,
            /*unsigned*/ int colorbase) {
        int/*DATA_TYPE **/ end;

        srcmodulo -= srcwidth;
        dstmodulo -= srcwidth;

        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while (dstdata.offset <= end - 8) {
                dstdata.write(0, colorbase + srcdata.read(0));
                dstdata.write(1, colorbase + srcdata.read(1));
                dstdata.write(2, colorbase + srcdata.read(2));
                dstdata.write(3, colorbase + srcdata.read(3));
                dstdata.write(4, colorbase + srcdata.read(4));
                dstdata.write(5, colorbase + srcdata.read(5));
                dstdata.write(6, colorbase + srcdata.read(6));
                dstdata.write(7, colorbase + srcdata.read(7));
                dstdata.offset += 8;
                srcdata.offset += 8;
            }
            while (dstdata.offset < end) {
                dstdata.writeinc(colorbase + srcdata.readinc());
            }

            srcdata.offset += srcmodulo;
            dstdata.offset += dstmodulo;
            srcheight--;
        }
    }

    /*TODO*///DECLARE(blockmove_8toN_opaque_raw,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		unsigned int colorbase),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata <= end - 8)
/*TODO*///		{
/*TODO*///			dstdata[0] = colorbase + srcdata[0];
/*TODO*///			dstdata[1] = colorbase + srcdata[1];
/*TODO*///			dstdata[2] = colorbase + srcdata[2];
/*TODO*///			dstdata[3] = colorbase + srcdata[3];
/*TODO*///			dstdata[4] = colorbase + srcdata[4];
/*TODO*///			dstdata[5] = colorbase + srcdata[5];
/*TODO*///			dstdata[6] = colorbase + srcdata[6];
/*TODO*///			dstdata[7] = colorbase + srcdata[7];
/*TODO*///			dstdata += 8;
/*TODO*///			srcdata += 8;
/*TODO*///		}
/*TODO*///		while (dstdata < end)
/*TODO*///			*(dstdata++) = colorbase + *(srcdata++);
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
    public static void blockmove_8toN_opaque_raw_flipx(
            UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo,
            UBytePtr dstdata, int dstmodulo,
            /*unsigned*/ int colorbase) {
        int end;

        srcmodulo += srcwidth;
        dstmodulo -= srcwidth;
        //srcdata += srcwidth-1;

        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while (dstdata.offset <= end - 8) {
                srcdata.offset -= 8;
                dstdata.write(0, colorbase + srcdata.read(8));
                dstdata.write(1, colorbase + srcdata.read(7));
                dstdata.write(2, colorbase + srcdata.read(6));
                dstdata.write(3, colorbase + srcdata.read(5));
                dstdata.write(4, colorbase + srcdata.read(4));
                dstdata.write(5, colorbase + srcdata.read(3));
                dstdata.write(6, colorbase + srcdata.read(2));
                dstdata.write(7, colorbase + srcdata.read(1));
                dstdata.offset += 8;
            }
            while (dstdata.offset < end) {
                dstdata.writeinc(colorbase + srcdata.readdec());
            }

            srcdata.offset += srcmodulo;
            dstdata.offset += dstmodulo;
            srcheight--;
        }
    }

    public static void blockmove_8toN_transpen8(UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo, UBytePtr dstdata, int dstmodulo, IntArray paldata, int transpen) {

        int end;
        int trans4;
        IntPtr sd4;//UINT32 *sd4;

        srcmodulo -= srcwidth;
        dstmodulo -= srcwidth;
        trans4 = transpen * 0x01010101;

        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while (((long) srcdata.offset & 3) != 0 && dstdata.offset < end) //while (((long)srcdata & 3) && dstdata < end)	/* longword align */
            {
                int col = srcdata.readinc();
                if (col != transpen) {
                    dstdata.write(0, paldata.read(col));
                }
                dstdata.offset++;
            }
            sd4 = new IntPtr(srcdata);//sd4 = (UINT32 *)srcdata;
            while (dstdata.offset <= end - 4) {
                int col4;
                if ((col4 = sd4.read(0)) != trans4) {
                    int xod4;
                    xod4 = col4 ^ trans4;
                    if ((xod4 & 0x000000ff) != 0) {
                        dstdata.write(0, paldata.read((col4) & 0xff));
                    }
                    if ((xod4 & 0x0000ff00) != 0) {
                        dstdata.write(1, paldata.read((col4 >> 8) & 0xff));
                    }
                    if ((xod4 & 0x00ff0000) != 0) {
                        dstdata.write(2, paldata.read((col4 >> 16) & 0xff));
                    }
                    if ((xod4 & 0xff000000) != 0) {
                        dstdata.write(3, paldata.read((col4 >> 24) & 0xff));
                    }
                }
                sd4.base += 4;
                dstdata.offset += 4;
            }
            srcdata.set(sd4.readCA(), sd4.getBase());//srcdata = (unsigned char *)sd4;
            while (dstdata.offset < end) {
                int col = srcdata.readinc();
                if (col != transpen) {
                    dstdata.write(0, paldata.read(col));
                }
                dstdata.offset++;
            }
            srcdata.offset += srcmodulo;
            dstdata.offset += dstmodulo;
            srcheight--;
        }
    }

    /*TODO*///DECLARE(blockmove_8toN_transpen,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transpen),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	int trans4;
/*TODO*///	UINT32 *sd4;
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	trans4 = transpen * 0x01010101;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (((long)srcdata & 3) && dstdata < end)	/* longword align */
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = *(srcdata++);
/*TODO*///			if (col != transpen) *dstdata = paldata[col];
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///		sd4 = (UINT32 *)srcdata;
/*TODO*///		while (dstdata <= end - 4)
/*TODO*///		{
/*TODO*///			UINT32 col4;
/*TODO*///
/*TODO*///			if ((col4 = *(sd4++)) != trans4)
/*TODO*///			{
/*TODO*///				UINT32 xod4;
/*TODO*///
/*TODO*///				xod4 = col4 ^ trans4;
/*TODO*///				if (xod4 & 0x000000ff) dstdata[BL0] = paldata[(col4) & 0xff];
/*TODO*///				if (xod4 & 0x0000ff00) dstdata[BL1] = paldata[(col4 >>  8) & 0xff];
/*TODO*///				if (xod4 & 0x00ff0000) dstdata[BL2] = paldata[(col4 >> 16) & 0xff];
/*TODO*///				if (xod4 & 0xff000000) dstdata[BL3] = paldata[col4 >> 24];
/*TODO*///			}
/*TODO*///			dstdata += 4;
/*TODO*///		}
/*TODO*///		srcdata = (UINT8 *)sd4;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = *(srcdata++);
/*TODO*///			if (col != transpen) *dstdata = paldata[col];
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
    public static void blockmove_8toN_transpen_flipx8(UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo, UBytePtr dstdata, int dstmodulo, IntArray paldata, int transpen) {
        int end;
        IntPtr sd4;//UINT32 *sd4;
        srcmodulo += srcwidth;
        dstmodulo -= srcwidth;
        //srcdata += srcwidth-1;
        srcdata.offset -= 3;

        int trans4 = transpen * 0x01010101;

        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while (((long) srcdata.offset & 3) != 0 && dstdata.offset < end) //while (((long)srcdata & 3) && dstdata < end)	/* longword align */
            {
                int col = srcdata.read(3);
                srcdata.offset--;
                if (col != transpen) {
                    dstdata.write(0, paldata.read(col));
                }
                dstdata.offset++;
            }
            sd4 = new IntPtr(srcdata);
            while (dstdata.offset <= end - 4) {
                int col4;//UINT32 col4
                if ((col4 = sd4.read(0)) != trans4)//if ((col4 = *(sd4--)) != trans4)
                {
                    int xod4; //UINT32 xod4;

                    xod4 = col4 ^ trans4;
                    if ((xod4 & 0xff000000) != 0) {
                        dstdata.write(0, paldata.read((col4 >> 24) & 0xff));//is 0xFF neccesary here???
                    }
                    if ((xod4 & 0x00ff0000) != 0) {
                        dstdata.write(1, paldata.read((col4 >> 16) & 0xff));
                    }
                    if ((xod4 & 0x0000ff00) != 0) {
                        dstdata.write(2, paldata.read((col4 >> 8) & 0xff));
                    }
                    if ((xod4 & 0x000000ff) != 0) {
                        dstdata.write(3, paldata.read(col4 & 0xff));
                    }
                }
                sd4.base -= 4;
                dstdata.offset += 4;
            }
            srcdata.set(sd4.readCA(), sd4.getBase());
            while (dstdata.offset < end) {
                int col = srcdata.read(3);
                srcdata.offset--;
                if (col != transpen) {
                    dstdata.write(0, paldata.read(col));
                }
                dstdata.offset++;

            }
            srcdata.offset += srcmodulo;
            dstdata.offset += dstmodulo;
            srcheight--;
        }
    }

    /*TODO*///DECLARE(blockmove_8toN_transpen_flipx,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transpen),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	int trans4;
/*TODO*///	UINT32 *sd4;
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///	srcdata -= 3;
/*TODO*///
/*TODO*///	trans4 = transpen * 0x01010101;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (((long)srcdata & 3) && dstdata < end)	/* longword align */
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = srcdata[3];
/*TODO*///			srcdata--;
/*TODO*///			if (col != transpen) *dstdata = paldata[col];
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///		sd4 = (UINT32 *)srcdata;
/*TODO*///		while (dstdata <= end - 4)
/*TODO*///		{
/*TODO*///			UINT32 col4;
/*TODO*///
/*TODO*///			if ((col4 = *(sd4--)) != trans4)
/*TODO*///			{
/*TODO*///				UINT32 xod4;
/*TODO*///
/*TODO*///				xod4 = col4 ^ trans4;
/*TODO*///				if (xod4 & 0xff000000) dstdata[BL0] = paldata[col4 >> 24];
/*TODO*///				if (xod4 & 0x00ff0000) dstdata[BL1] = paldata[(col4 >> 16) & 0xff];
/*TODO*///				if (xod4 & 0x0000ff00) dstdata[BL2] = paldata[(col4 >>  8) & 0xff];
/*TODO*///				if (xod4 & 0x000000ff) dstdata[BL3] = paldata[col4 & 0xff];
/*TODO*///			}
/*TODO*///			dstdata += 4;
/*TODO*///		}
/*TODO*///		srcdata = (UINT8 *)sd4;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = srcdata[3];
/*TODO*///			srcdata--;
/*TODO*///			if (col != transpen) *dstdata = paldata[col];
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_transpen_pri,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transpen,UINT8 *pridata,UINT32 pmask),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	int trans4;
/*TODO*///	UINT32 *sd4;
/*TODO*///
/*TODO*///	pmask |= (1<<31);
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	trans4 = transpen * 0x01010101;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (((long)srcdata & 3) && dstdata < end)	/* longword align */
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = *(srcdata++);
/*TODO*///			if (col != transpen)
/*TODO*///			{
/*TODO*///				if (((1 << *pridata) & pmask) == 0)
/*TODO*///					*dstdata = paldata[col];
/*TODO*///				*pridata = 31;
/*TODO*///			}
/*TODO*///			dstdata++;
/*TODO*///			pridata++;
/*TODO*///		}
/*TODO*///		sd4 = (UINT32 *)srcdata;
/*TODO*///		while (dstdata <= end - 4)
/*TODO*///		{
/*TODO*///			UINT32 col4;
/*TODO*///
/*TODO*///			if ((col4 = *(sd4++)) != trans4)
/*TODO*///			{
/*TODO*///				UINT32 xod4;
/*TODO*///
/*TODO*///				xod4 = col4 ^ trans4;
/*TODO*///				if (xod4 & 0x000000ff)
/*TODO*///				{
/*TODO*///					if (((1 << pridata[BL0]) & pmask) == 0)
/*TODO*///						dstdata[BL0] = paldata[(col4) & 0xff];
/*TODO*///					pridata[BL0] = 31;
/*TODO*///				}
/*TODO*///				if (xod4 & 0x0000ff00)
/*TODO*///				{
/*TODO*///					if (((1 << pridata[BL1]) & pmask) == 0)
/*TODO*///						dstdata[BL1] = paldata[(col4 >>  8) & 0xff];
/*TODO*///					pridata[BL1] = 31;
/*TODO*///				}
/*TODO*///				if (xod4 & 0x00ff0000)
/*TODO*///				{
/*TODO*///					if (((1 << pridata[BL2]) & pmask) == 0)
/*TODO*///						dstdata[BL2] = paldata[(col4 >> 16) & 0xff];
/*TODO*///					pridata[BL2] = 31;
/*TODO*///				}
/*TODO*///				if (xod4 & 0xff000000)
/*TODO*///				{
/*TODO*///					if (((1 << pridata[BL3]) & pmask) == 0)
/*TODO*///						dstdata[BL3] = paldata[col4 >> 24];
/*TODO*///					pridata[BL3] = 31;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			dstdata += 4;
/*TODO*///			pridata += 4;
/*TODO*///		}
/*TODO*///		srcdata = (UINT8 *)sd4;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = *(srcdata++);
/*TODO*///			if (col != transpen)
/*TODO*///			{
/*TODO*///				if (((1 << *pridata) & pmask) == 0)
/*TODO*///					*dstdata = paldata[col];
/*TODO*///				*pridata = 31;
/*TODO*///			}
/*TODO*///			dstdata++;
/*TODO*///			pridata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		pridata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_transpen_pri_flipx,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transpen,UINT8 *pridata,UINT32 pmask),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	int trans4;
/*TODO*///	UINT32 *sd4;
/*TODO*///
/*TODO*///	pmask |= (1<<31);
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///	srcdata -= 3;
/*TODO*///
/*TODO*///	trans4 = transpen * 0x01010101;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (((long)srcdata & 3) && dstdata < end)	/* longword align */
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = srcdata[3];
/*TODO*///			srcdata--;
/*TODO*///			if (col != transpen)
/*TODO*///			{
/*TODO*///				if (((1 << *pridata) & pmask) == 0)
/*TODO*///					*dstdata = paldata[col];
/*TODO*///				*pridata = 31;
/*TODO*///			}
/*TODO*///			dstdata++;
/*TODO*///			pridata++;
/*TODO*///		}
/*TODO*///		sd4 = (UINT32 *)srcdata;
/*TODO*///		while (dstdata <= end - 4)
/*TODO*///		{
/*TODO*///			UINT32 col4;
/*TODO*///
/*TODO*///			if ((col4 = *(sd4--)) != trans4)
/*TODO*///			{
/*TODO*///				UINT32 xod4;
/*TODO*///
/*TODO*///				xod4 = col4 ^ trans4;
/*TODO*///				if (xod4 & 0xff000000)
/*TODO*///				{
/*TODO*///					if (((1 << pridata[BL0]) & pmask) == 0)
/*TODO*///						dstdata[BL0] = paldata[col4 >> 24];
/*TODO*///					pridata[BL0] = 31;
/*TODO*///				}
/*TODO*///				if (xod4 & 0x00ff0000)
/*TODO*///				{
/*TODO*///					if (((1 << pridata[BL1]) & pmask) == 0)
/*TODO*///						dstdata[BL1] = paldata[(col4 >> 16) & 0xff];
/*TODO*///					pridata[BL1] = 31;
/*TODO*///				}
/*TODO*///				if (xod4 & 0x0000ff00)
/*TODO*///				{
/*TODO*///					if (((1 << pridata[BL2]) & pmask) == 0)
/*TODO*///						dstdata[BL2] = paldata[(col4 >>  8) & 0xff];
/*TODO*///					pridata[BL2] = 31;
/*TODO*///				}
/*TODO*///				if (xod4 & 0x000000ff)
/*TODO*///				{
/*TODO*///					if (((1 << pridata[BL3]) & pmask) == 0)
/*TODO*///						dstdata[BL3] = paldata[col4 & 0xff];
/*TODO*///					pridata[BL3] = 31;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			dstdata += 4;
/*TODO*///			pridata += 4;
/*TODO*///		}
/*TODO*///		srcdata = (UINT8 *)sd4;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = srcdata[3];
/*TODO*///			srcdata--;
/*TODO*///			if (col != transpen)
/*TODO*///			{
/*TODO*///				if (((1 << *pridata) & pmask) == 0)
/*TODO*///					*dstdata = paldata[col];
/*TODO*///				*pridata = 31;
/*TODO*///			}
/*TODO*///			dstdata++;
/*TODO*///			pridata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		pridata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
    public static void blockmove_8toN_transpen_raw(
            UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo,
            UBytePtr dstdata, int dstmodulo,
            /*unsigned*/ int colorbase, int transpen) {

        int end;
        int trans4;
        IntPtr sd4;//UINT32 *sd4;

        srcmodulo -= srcwidth;
        dstmodulo -= srcwidth;
        trans4 = transpen * 0x01010101;
        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while (((long) srcdata.offset & 3) != 0 && dstdata.offset < end) //while (((long)srcdata & 3) && dstdata < end)	/* longword align */
            {
                int col = srcdata.readinc();
                if (col != transpen) {
                    dstdata.write(colorbase + col);
                }
                dstdata.offset++;
            }
            sd4 = new IntPtr(srcdata);//sd4 = (UINT32 *)srcdata;
            while (dstdata.offset <= end - 4) {
                int col4;
                if ((col4 = sd4.read(0)) != trans4) {
                    int xod4;
                    xod4 = col4 ^ trans4;
                    if ((xod4 & 0x000000ff) != 0) {
                        dstdata.write(0, colorbase + ((col4) & 0xff));
                    }
                    if ((xod4 & 0x0000ff00) != 0) {
                        dstdata.write(1, colorbase + ((col4 >> 8) & 0xff));
                    }
                    if ((xod4 & 0x00ff0000) != 0) {
                        dstdata.write(2, colorbase + ((col4 >> 16) & 0xff));
                    }
                    if ((xod4 & 0xff000000) != 0) {
                        dstdata.write(3, colorbase + ((col4 >> 24) & 0xff));
                    }
                }
                sd4.base += 4;
                dstdata.offset += 4;
            }
            srcdata.set(sd4.readCA(), sd4.getBase());//srcdata = (unsigned char *)sd4;
            while (dstdata.offset < end) {
                int col = srcdata.readinc();
                if (col != transpen) {
                    dstdata.write(0, colorbase + col);
                }
                dstdata.offset++;
            }
            srcdata.offset += srcmodulo;
            dstdata.offset += dstmodulo;
            srcheight--;
        }

    }

    /*TODO*///
/*TODO*///DECLARE(blockmove_8toN_transpen_raw_flipx,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		unsigned int colorbase, int transpen),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	int trans4;
/*TODO*///	UINT32 *sd4;
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///	srcdata -= 3;
/*TODO*///
/*TODO*///	trans4 = transpen * 0x01010101;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (((long)srcdata & 3) && dstdata < end)	/* longword align */
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = srcdata[3];
/*TODO*///			srcdata--;
/*TODO*///			if (col != transpen) *dstdata = colorbase + col;
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///		sd4 = (UINT32 *)srcdata;
/*TODO*///		while (dstdata <= end - 4)
/*TODO*///		{
/*TODO*///			UINT32 col4;
/*TODO*///
/*TODO*///			if ((col4 = *(sd4--)) != trans4)
/*TODO*///			{
/*TODO*///				UINT32 xod4;
/*TODO*///
/*TODO*///				xod4 = col4 ^ trans4;
/*TODO*///				if (xod4 & 0xff000000) dstdata[BL0] = colorbase + (col4 >> 24);
/*TODO*///				if (xod4 & 0x00ff0000) dstdata[BL1] = colorbase + ((col4 >> 16) & 0xff);
/*TODO*///				if (xod4 & 0x0000ff00) dstdata[BL2] = colorbase + ((col4 >>  8) & 0xff);
/*TODO*///				if (xod4 & 0x000000ff) dstdata[BL3] = colorbase + (col4 & 0xff);
/*TODO*///			}
/*TODO*///			dstdata += 4;
/*TODO*///		}
/*TODO*///		srcdata = (UINT8 *)sd4;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = srcdata[3];
/*TODO*///			srcdata--;
/*TODO*///			if (col != transpen) *dstdata = colorbase + col;
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///#define PEN_IS_OPAQUE ((1<<col)&transmask) == 0
/*TODO*///
    static void blockmove_transmask8(UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo, UBytePtr dstdata, int dstmodulo, IntArray paldata, int transmask) {
        int end;
        IntPtr sd4;//UINT32 *sd4;

        srcmodulo -= srcwidth;
        dstmodulo -= srcwidth;

        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while ((srcdata.offset & 3) != 0 && dstdata.offset < end)//while (((long)srcdata & 3) && dstdata < end)	/* longword align */
            {
                int col;

                col = srcdata.read(0);
                srcdata.offset++;
                if (((1 << col) & transmask) == 0) {
                    dstdata.write(0, paldata.read(col)); //*dstdata = paldata[col];
                }
                dstdata.offset++;
            }
            sd4 = new IntPtr(srcdata);//sd4 = (UINT32 *)srcdata;
            while (dstdata.offset <= end - 4) {
                int col;
                int col4;

                col4 = sd4.read(0);
                col = (col4 >> 0) & 0xff;
                if (((1 << col) & transmask) == 0) {
                    dstdata.write(0, paldata.read(col));
                }
                col = (col4 >> 8) & 0xff;
                if (((1 << col) & transmask) == 0) {
                    dstdata.write(1, paldata.read(col));
                }
                col = (col4 >> 16) & 0xff;
                if (((1 << col) & transmask) == 0) {
                    dstdata.write(2, paldata.read(col));
                }
                col = (col4 >> 24) & 0xff;
                if (((1 << col) & transmask) == 0) {
                    dstdata.write(3, paldata.read(col));
                }
                sd4.base += 4;
                dstdata.offset += 4;
            }
            srcdata.set(sd4.readCA(), sd4.getBase());//srcdata = (unsigned char *)sd4;

            while (dstdata.offset < end) {
                int col;

                col = srcdata.read(0);
                srcdata.offset++;
                if (((1 << col) & transmask) == 0) {
                    dstdata.write(0, paldata.read(col));//*dstdata = paldata[col];
                }
                dstdata.offset++;
            }

            srcdata.offset += srcmodulo;
            dstdata.offset += dstmodulo;
            srcheight--;
        }
    }

    /*TODO*///DECLARE(blockmove_8toN_transmask,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transmask),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	UINT32 *sd4;
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (((long)srcdata & 3) && dstdata < end)	/* longword align */
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = *(srcdata++);
/*TODO*///			if (PEN_IS_OPAQUE) *dstdata = paldata[col];
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///		sd4 = (UINT32 *)srcdata;
/*TODO*///		while (dstdata <= end - 4)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///			UINT32 col4;
/*TODO*///
/*TODO*///			col4 = *(sd4++);
/*TODO*///			col = (col4 >>  0) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL0] = paldata[col];
/*TODO*///			col = (col4 >>  8) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL1] = paldata[col];
/*TODO*///			col = (col4 >> 16) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL2] = paldata[col];
/*TODO*///			col = (col4 >> 24) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL3] = paldata[col];
/*TODO*///			dstdata += 4;
/*TODO*///		}
/*TODO*///		srcdata = (UINT8 *)sd4;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = *(srcdata++);
/*TODO*///			if (PEN_IS_OPAQUE) *dstdata = paldata[col];
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
    static void blockmove_transmask_flipx8(UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo, UBytePtr dstdata, int dstmodulo, IntArray paldata, int transmask) {
        int end;
        IntPtr sd4;//UINT32 *sd4;

        srcmodulo += srcwidth;
        dstmodulo -= srcwidth;
        //srcdata += srcwidth-1;
        srcdata.offset -= 3;

        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while ((srcdata.offset & 3) != 0 && dstdata.offset < end)//while (((long)srcdata & 3) && dstdata < end)	/* longword align */
            {
                int col;

                col = srcdata.read(3);
                srcdata.offset--;
                if (((1 << col) & transmask) == 0) {
                    dstdata.write(0, paldata.read(col)); //*dstdata = paldata[col];
                }
                dstdata.offset++;
            }
            sd4 = new IntPtr(srcdata);//sd4 = (UINT32 *)srcdata;
            while (dstdata.offset <= end - 4) {
                int col;
                int col4;
                col4 = sd4.read(0);//col4 = *(sd4--);
                col = (col4 >> 24) & 0xff;
                if (((1 << col) & transmask) == 0) {
                    dstdata.write(0, paldata.read(col));
                }
                col = (col4 >> 16) & 0xff;
                if (((1 << col) & transmask) == 0) {
                    dstdata.write(1, paldata.read(col));
                }
                col = (col4 >> 8) & 0xff;
                if (((1 << col) & transmask) == 0) {
                    dstdata.write(2, paldata.read(col));
                }
                col = (col4 >> 0) & 0xff;
                if (((1 << col) & transmask) == 0) {
                    dstdata.write(3, paldata.read(col));
                }
                sd4.base -= 4;
                dstdata.offset += 4;
            }
            srcdata.set(sd4.readCA(), sd4.getBase());//srcdata = (unsigned char *)sd4;

            while (dstdata.offset < end) {
                int col;

                col = srcdata.read(3);
                srcdata.offset--;
                if (((1 << col) & transmask) == 0) {
                    dstdata.write(0, paldata.read(col));//*dstdata = paldata[col];
                }
                dstdata.offset++;
            }

            srcdata.offset += srcmodulo;
            dstdata.offset += dstmodulo;
            srcheight--;
        }
    }

    /*TODO*///DECLARE(blockmove_8toN_transmask_flipx,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transmask),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	UINT32 *sd4;
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///	srcdata -= 3;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (((long)srcdata & 3) && dstdata < end)	/* longword align */
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = srcdata[3];
/*TODO*///			srcdata--;
/*TODO*///			if (PEN_IS_OPAQUE) *dstdata = paldata[col];
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///		sd4 = (UINT32 *)srcdata;
/*TODO*///		while (dstdata <= end - 4)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///			UINT32 col4;
/*TODO*///
/*TODO*///			col4 = *(sd4--);
/*TODO*///			col = (col4 >> 24) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL0] = paldata[col];
/*TODO*///			col = (col4 >> 16) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL1] = paldata[col];
/*TODO*///			col = (col4 >>  8) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL2] = paldata[col];
/*TODO*///			col = (col4 >>  0) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL3] = paldata[col];
/*TODO*///			dstdata += 4;
/*TODO*///		}
/*TODO*///		srcdata = (UINT8 *)sd4;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = srcdata[3];
/*TODO*///			srcdata--;
/*TODO*///			if (PEN_IS_OPAQUE) *dstdata = paldata[col];
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_transmask_pri,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transmask,UINT8 *pridata,UINT32 pmask),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	UINT32 *sd4;
/*TODO*///
/*TODO*///	pmask |= (1<<31);
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (((long)srcdata & 3) && dstdata < end)	/* longword align */
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = *(srcdata++);
/*TODO*///			if (PEN_IS_OPAQUE)
/*TODO*///			{
/*TODO*///				if (((1 << *pridata) & pmask) == 0)
/*TODO*///					*dstdata = paldata[col];
/*TODO*///				*pridata = 31;
/*TODO*///			}
/*TODO*///			dstdata++;
/*TODO*///			pridata++;
/*TODO*///		}
/*TODO*///		sd4 = (UINT32 *)srcdata;
/*TODO*///		while (dstdata <= end - 4)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///			UINT32 col4;
/*TODO*///
/*TODO*///			col4 = *(sd4++);
/*TODO*///			col = (col4 >>  0) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE)
/*TODO*///			{
/*TODO*///				if (((1 << pridata[BL0]) & pmask) == 0)
/*TODO*///					dstdata[BL0] = paldata[col];
/*TODO*///				pridata[BL0] = 31;
/*TODO*///			}
/*TODO*///			col = (col4 >>  8) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE)
/*TODO*///			{
/*TODO*///				if (((1 << pridata[BL1]) & pmask) == 0)
/*TODO*///					dstdata[BL1] = paldata[col];
/*TODO*///				pridata[BL1] = 31;
/*TODO*///			}
/*TODO*///			col = (col4 >> 16) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE)
/*TODO*///			{
/*TODO*///				if (((1 << pridata[BL2]) & pmask) == 0)
/*TODO*///					dstdata[BL2] = paldata[col];
/*TODO*///				pridata[BL2] = 31;
/*TODO*///			}
/*TODO*///			col = (col4 >> 24) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE)
/*TODO*///			{
/*TODO*///				if (((1 << pridata[BL3]) & pmask) == 0)
/*TODO*///					dstdata[BL3] = paldata[col];
/*TODO*///				pridata[BL3] = 31;
/*TODO*///			}
/*TODO*///			dstdata += 4;
/*TODO*///			pridata += 4;
/*TODO*///		}
/*TODO*///		srcdata = (UINT8 *)sd4;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = *(srcdata++);
/*TODO*///			if (PEN_IS_OPAQUE)
/*TODO*///			{
/*TODO*///				if (((1 << *pridata) & pmask) == 0)
/*TODO*///					*dstdata = paldata[col];
/*TODO*///				*pridata = 31;
/*TODO*///			}
/*TODO*///			dstdata++;
/*TODO*///			pridata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		pridata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_transmask_pri_flipx,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transmask,UINT8 *pridata,UINT32 pmask),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	UINT32 *sd4;
/*TODO*///
/*TODO*///	pmask |= (1<<31);
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///	srcdata -= 3;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (((long)srcdata & 3) && dstdata < end)	/* longword align */
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = srcdata[3];
/*TODO*///			srcdata--;
/*TODO*///			if (PEN_IS_OPAQUE)
/*TODO*///			{
/*TODO*///				if (((1 << *pridata) & pmask) == 0)
/*TODO*///					*dstdata = paldata[col];
/*TODO*///				*pridata = 31;
/*TODO*///			}
/*TODO*///			dstdata++;
/*TODO*///			pridata++;
/*TODO*///		}
/*TODO*///		sd4 = (UINT32 *)srcdata;
/*TODO*///		while (dstdata <= end - 4)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///			UINT32 col4;
/*TODO*///
/*TODO*///			col4 = *(sd4--);
/*TODO*///			col = (col4 >> 24) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE)
/*TODO*///			{
/*TODO*///				if (((1 << pridata[BL0]) & pmask) == 0)
/*TODO*///					dstdata[BL0] = paldata[col];
/*TODO*///				pridata[BL0] = 31;
/*TODO*///			}
/*TODO*///			col = (col4 >> 16) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE)
/*TODO*///			{
/*TODO*///				if (((1 << pridata[BL1]) & pmask) == 0)
/*TODO*///					dstdata[BL1] = paldata[col];
/*TODO*///				pridata[BL1] = 31;
/*TODO*///			}
/*TODO*///			col = (col4 >>  8) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE)
/*TODO*///			{
/*TODO*///				if (((1 << pridata[BL2]) & pmask) == 0)
/*TODO*///					dstdata[BL2] = paldata[col];
/*TODO*///				pridata[BL2] = 31;
/*TODO*///			}
/*TODO*///			col = (col4 >>  0) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE)
/*TODO*///			{
/*TODO*///				if (((1 << pridata[BL3]) & pmask) == 0)
/*TODO*///					dstdata[BL3] = paldata[col];
/*TODO*///				pridata[BL3] = 31;
/*TODO*///			}
/*TODO*///			dstdata += 4;
/*TODO*///			pridata += 4;
/*TODO*///		}
/*TODO*///		srcdata = (UINT8 *)sd4;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = srcdata[3];
/*TODO*///			srcdata--;
/*TODO*///			if (PEN_IS_OPAQUE)
/*TODO*///			{
/*TODO*///				if (((1 << *pridata) & pmask) == 0)
/*TODO*///					*dstdata = paldata[col];
/*TODO*///				*pridata = 31;
/*TODO*///			}
/*TODO*///			dstdata++;
/*TODO*///			pridata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		pridata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_transmask_raw,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		unsigned int colorbase,int transmask),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	UINT32 *sd4;
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (((long)srcdata & 3) && dstdata < end)	/* longword align */
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = *(srcdata++);
/*TODO*///			if (PEN_IS_OPAQUE) *dstdata = colorbase + col;
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///		sd4 = (UINT32 *)srcdata;
/*TODO*///		while (dstdata <= end - 4)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///			UINT32 col4;
/*TODO*///
/*TODO*///			col4 = *(sd4++);
/*TODO*///			col = (col4 >>  0) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL0] = colorbase + col;
/*TODO*///			col = (col4 >>  8) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL1] = colorbase + col;
/*TODO*///			col = (col4 >> 16) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL2] = colorbase + col;
/*TODO*///			col = (col4 >> 24) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL3] = colorbase + col;
/*TODO*///			dstdata += 4;
/*TODO*///		}
/*TODO*///		srcdata = (UINT8 *)sd4;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = *(srcdata++);
/*TODO*///			if (PEN_IS_OPAQUE) *dstdata = colorbase + col;
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_transmask_raw_flipx,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		unsigned int colorbase,int transmask),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	UINT32 *sd4;
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///	srcdata -= 3;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (((long)srcdata & 3) && dstdata < end)	/* longword align */
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = srcdata[3];
/*TODO*///			srcdata--;
/*TODO*///			if (PEN_IS_OPAQUE) *dstdata = colorbase + col;
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///		sd4 = (UINT32 *)srcdata;
/*TODO*///		while (dstdata <= end - 4)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///			UINT32 col4;
/*TODO*///
/*TODO*///			col4 = *(sd4--);
/*TODO*///			col = (col4 >> 24) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL0] = colorbase + col;
/*TODO*///			col = (col4 >> 16) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL1] = colorbase + col;
/*TODO*///			col = (col4 >>  8) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL2] = colorbase + col;
/*TODO*///			col = (col4 >>  0) & 0xff;
/*TODO*///			if (PEN_IS_OPAQUE) dstdata[BL3] = colorbase + col;
/*TODO*///			dstdata += 4;
/*TODO*///		}
/*TODO*///		srcdata = (UINT8 *)sd4;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = srcdata[3];
/*TODO*///			srcdata--;
/*TODO*///			if (PEN_IS_OPAQUE) *dstdata = colorbase + col;
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
    public static void blockmove_8toN_transcolor8(
            UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo,
            UBytePtr dstdata, int dstmodulo,
            IntArray paldata, int transcolor) {
        int end;
        //const UINT16 *lookupdata = Machine->game_colortable + (paldata - Machine->remapped_colortable);
        UShortArray lookupdata = new UShortArray(Machine.game_colortable, (paldata.offset - Machine.remapped_colortable.offset));

        srcmodulo -= srcwidth;
        dstmodulo -= srcwidth;

        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while (dstdata.offset < end) {
                //if (lookupdata[*srcdata] != transcolor) *dstdata = paldata[*srcdata];
                if (lookupdata.read(srcdata.read()) != transcolor) {
                    dstdata.write(paldata.read(srcdata.read()));
                }

                srcdata.inc();
                dstdata.inc();
            }

            srcdata.inc(srcmodulo);
            dstdata.inc(dstmodulo);
            srcheight--;
        }
    }

    /*TODO*///
/*TODO*///DECLARE(blockmove_8toN_transcolor,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transcolor),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	const UINT16 *lookupdata = Machine->game_colortable + (paldata - Machine->remapped_colortable);
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			if (lookupdata[*srcdata] != transcolor) *dstdata = paldata[*srcdata];
/*TODO*///			srcdata++;
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
    public static void blockmove_8toN_transcolor_flipx8(
            UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo,
            UBytePtr dstdata, int dstmodulo,
            IntArray paldata, int transcolor) {
        int end;
        //const UINT16 *lookupdata = Machine->game_colortable + (paldata - Machine->remapped_colortable);
        UShortArray lookupdata = new UShortArray(Machine.game_colortable, (paldata.offset - Machine.remapped_colortable.offset));

        srcmodulo += srcwidth;
        dstmodulo -= srcwidth;
        //srcdata += srcwidth-1;

        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while (dstdata.offset < end) {
                //if (lookupdata[*srcdata] != transcolor) *dstdata = paldata[*srcdata];
                if (lookupdata.read(srcdata.read()) != transcolor) {
                    dstdata.write(paldata.read(srcdata.read()));
                }
                srcdata.dec();
                dstdata.inc();
            }

            srcdata.inc(srcmodulo);
            dstdata.inc(dstmodulo);
            srcheight--;
        }
    }

    /*TODO*///DECLARE(blockmove_8toN_transcolor_flipx,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transcolor),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	const UINT16 *lookupdata = Machine->game_colortable + (paldata - Machine->remapped_colortable);
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			if (lookupdata[*srcdata] != transcolor) *dstdata = paldata[*srcdata];
/*TODO*///			srcdata--;
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_transcolor_pri,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transcolor,UINT8 *pridata,UINT32 pmask),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	const UINT16 *lookupdata = Machine->game_colortable + (paldata - Machine->remapped_colortable);
/*TODO*///
/*TODO*///	pmask |= (1<<31);
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			if (lookupdata[*srcdata] != transcolor)
/*TODO*///			{
/*TODO*///				if (((1 << *pridata) & pmask) == 0)
/*TODO*///					*dstdata = paldata[*srcdata];
/*TODO*///				*pridata = 31;
/*TODO*///			}
/*TODO*///			srcdata++;
/*TODO*///			dstdata++;
/*TODO*///			pridata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		pridata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_transcolor_pri_flipx,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transcolor,UINT8 *pridata,UINT32 pmask),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///	const UINT16 *lookupdata = Machine->game_colortable + (paldata - Machine->remapped_colortable);
/*TODO*///
/*TODO*///	pmask |= (1<<31);
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			if (lookupdata[*srcdata] != transcolor)
/*TODO*///			{
/*TODO*///				if (((1 << *pridata) & pmask) == 0)
/*TODO*///					*dstdata = paldata[*srcdata];
/*TODO*///				*pridata = 31;
/*TODO*///			}
/*TODO*///			srcdata--;
/*TODO*///			dstdata++;
/*TODO*///			pridata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		pridata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_transthrough,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transcolor),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			if (*dstdata == transcolor) *dstdata = paldata[*srcdata];
/*TODO*///			srcdata++;
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_transthrough_flipx,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transcolor),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			if (*dstdata == transcolor) *dstdata = paldata[*srcdata];
/*TODO*///			srcdata--;
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_transthrough_raw,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		unsigned int colorbase,int transcolor),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			if (*dstdata == transcolor) *dstdata = colorbase + *srcdata;
/*TODO*///			srcdata++;
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_transthrough_raw_flipx,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		unsigned int colorbase,int transcolor),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			if (*dstdata == transcolor) *dstdata = colorbase + *srcdata;
/*TODO*///			srcdata--;
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_pen_table,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transcolor),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = *(srcdata++);
/*TODO*///			if (col != transcolor)
/*TODO*///			{
/*TODO*///				switch(gfx_drawmode_table[col])
/*TODO*///				{
/*TODO*///				case DRAWMODE_SOURCE:
/*TODO*///					*dstdata = paldata[col];
/*TODO*///					break;
/*TODO*///				case DRAWMODE_SHADOW:
/*TODO*///					*dstdata = palette_shadow_table[*dstdata];
/*TODO*///					break;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_pen_table_flipx,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int transcolor),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = *(srcdata--);
/*TODO*///			if (col != transcolor)
/*TODO*///			{
/*TODO*///				switch(gfx_drawmode_table[col])
/*TODO*///				{
/*TODO*///				case DRAWMODE_SOURCE:
/*TODO*///					*dstdata = paldata[col];
/*TODO*///					break;
/*TODO*///				case DRAWMODE_SHADOW:
/*TODO*///					*dstdata = palette_shadow_table[*dstdata];
/*TODO*///					break;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_pen_table_raw,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		unsigned int colorbase,int transcolor),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = *(srcdata++);
/*TODO*///			if (col != transcolor)
/*TODO*///			{
/*TODO*///				switch(gfx_drawmode_table[col])
/*TODO*///				{
/*TODO*///				case DRAWMODE_SOURCE:
/*TODO*///					*dstdata = colorbase + col;
/*TODO*///					break;
/*TODO*///				case DRAWMODE_SHADOW:
/*TODO*///					*dstdata = palette_shadow_table[*dstdata];
/*TODO*///					break;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_8toN_pen_table_raw_flipx,(
/*TODO*///		const UINT8 *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		unsigned int colorbase,int transcolor),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			int col;
/*TODO*///
/*TODO*///			col = *(srcdata--);
/*TODO*///			if (col != transcolor)
/*TODO*///			{
/*TODO*///				switch(gfx_drawmode_table[col])
/*TODO*///				{
/*TODO*///				case DRAWMODE_SOURCE:
/*TODO*///					*dstdata = colorbase + col;
/*TODO*///					break;
/*TODO*///				case DRAWMODE_SHADOW:
/*TODO*///					*dstdata = palette_shadow_table[*dstdata];
/*TODO*///					break;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
    public static void blockmove_NtoN_opaque_noremap8(UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo, UBytePtr dstdata, int dstmodulo) {

        while (srcheight != 0) {
            System.arraycopy(srcdata.memory, (int) srcdata.offset, dstdata.memory, (int) dstdata.offset, srcwidth);
            srcdata.offset += srcmodulo;
            dstdata.offset += dstmodulo;
            srcheight--;
        }
    }

    /*TODO*///DECLARE(blockmove_NtoN_opaque_noremap,(
/*TODO*///		const DATA_TYPE *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo),
/*TODO*///{
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		memcpy(dstdata,srcdata,srcwidth * sizeof(DATA_TYPE));
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_NtoN_opaque_noremap_flipx,(
/*TODO*///		const DATA_TYPE *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata <= end - 8)
/*TODO*///		{
/*TODO*///			srcdata -= 8;
/*TODO*///			dstdata[0] = srcdata[8];
/*TODO*///			dstdata[1] = srcdata[7];
/*TODO*///			dstdata[2] = srcdata[6];
/*TODO*///			dstdata[3] = srcdata[5];
/*TODO*///			dstdata[4] = srcdata[4];
/*TODO*///			dstdata[5] = srcdata[3];
/*TODO*///			dstdata[6] = srcdata[2];
/*TODO*///			dstdata[7] = srcdata[1];
/*TODO*///			dstdata += 8;
/*TODO*///		}
/*TODO*///		while (dstdata < end)
/*TODO*///			*(dstdata++) = *(srcdata--);
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
    public static void blockmove_NtoN_opaque_remap8(
            UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo,
            UBytePtr dstdata, int dstmodulo,
            int[] paldata) {
        int end;

        srcmodulo -= srcwidth;
        dstmodulo -= srcwidth;

        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while (dstdata.offset <= end - 8) {
                dstdata.write(0, paldata[srcdata.read(0)]);
                dstdata.write(1, paldata[srcdata.read(1)]);
                dstdata.write(2, paldata[srcdata.read(2)]);
                dstdata.write(3, paldata[srcdata.read(3)]);
                dstdata.write(4, paldata[srcdata.read(4)]);
                dstdata.write(5, paldata[srcdata.read(5)]);
                dstdata.write(6, paldata[srcdata.read(6)]);
                dstdata.write(7, paldata[srcdata.read(7)]);
                dstdata.inc(8);
                srcdata.inc(8);
            }
            while (dstdata.offset < end) {
                dstdata.writeinc(paldata[srcdata.readinc()]);
            }

            srcdata.inc(srcmodulo);
            dstdata.inc(dstmodulo);
            srcheight--;
        }
    }

    /*TODO*///
/*TODO*///DECLARE(blockmove_NtoN_opaque_remap,(
/*TODO*///		const DATA_TYPE *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata <= end - 8)
/*TODO*///		{
/*TODO*///			dstdata[0] = paldata[srcdata[0]];
/*TODO*///			dstdata[1] = paldata[srcdata[1]];
/*TODO*///			dstdata[2] = paldata[srcdata[2]];
/*TODO*///			dstdata[3] = paldata[srcdata[3]];
/*TODO*///			dstdata[4] = paldata[srcdata[4]];
/*TODO*///			dstdata[5] = paldata[srcdata[5]];
/*TODO*///			dstdata[6] = paldata[srcdata[6]];
/*TODO*///			dstdata[7] = paldata[srcdata[7]];
/*TODO*///			dstdata += 8;
/*TODO*///			srcdata += 8;
/*TODO*///		}
/*TODO*///		while (dstdata < end)
/*TODO*///			*(dstdata++) = paldata[*(srcdata++)];
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_NtoN_opaque_remap_flipx,(
/*TODO*///		const DATA_TYPE *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata <= end - 8)
/*TODO*///		{
/*TODO*///			srcdata -= 8;
/*TODO*///			dstdata[0] = paldata[srcdata[8]];
/*TODO*///			dstdata[1] = paldata[srcdata[7]];
/*TODO*///			dstdata[2] = paldata[srcdata[6]];
/*TODO*///			dstdata[3] = paldata[srcdata[5]];
/*TODO*///			dstdata[4] = paldata[srcdata[4]];
/*TODO*///			dstdata[5] = paldata[srcdata[3]];
/*TODO*///			dstdata[6] = paldata[srcdata[2]];
/*TODO*///			dstdata[7] = paldata[srcdata[1]];
/*TODO*///			dstdata += 8;
/*TODO*///		}
/*TODO*///		while (dstdata < end)
/*TODO*///			*(dstdata++) = paldata[*(srcdata--)];
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///
/*TODO*///DECLARE(blockmove_NtoN_transthrough_noremap,(
/*TODO*///		const DATA_TYPE *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		int transcolor),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			if (*dstdata == transcolor) *dstdata = *srcdata;
/*TODO*///			srcdata++;
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_NtoN_transthrough_noremap_flipx,(
/*TODO*///		const DATA_TYPE *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		int transcolor),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			if (*dstdata == transcolor) *dstdata = *srcdata;
/*TODO*///			srcdata--;
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_NtoN_blend_noremap,(
/*TODO*///		const DATA_TYPE *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		int srcshift),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata <= end - 8)
/*TODO*///		{
/*TODO*///			dstdata[0] |= srcdata[0] << srcshift;
/*TODO*///			dstdata[1] |= srcdata[1] << srcshift;
/*TODO*///			dstdata[2] |= srcdata[2] << srcshift;
/*TODO*///			dstdata[3] |= srcdata[3] << srcshift;
/*TODO*///			dstdata[4] |= srcdata[4] << srcshift;
/*TODO*///			dstdata[5] |= srcdata[5] << srcshift;
/*TODO*///			dstdata[6] |= srcdata[6] << srcshift;
/*TODO*///			dstdata[7] |= srcdata[7] << srcshift;
/*TODO*///			dstdata += 8;
/*TODO*///			srcdata += 8;
/*TODO*///		}
/*TODO*///		while (dstdata < end)
/*TODO*///			*(dstdata++) |= *(srcdata++) << srcshift;
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_NtoN_blend_noremap_flipx,(
/*TODO*///		const DATA_TYPE *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		int srcshift),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata <= end - 8)
/*TODO*///		{
/*TODO*///			srcdata -= 8;
/*TODO*///			dstdata[0] |= srcdata[8] << srcshift;
/*TODO*///			dstdata[1] |= srcdata[7] << srcshift;
/*TODO*///			dstdata[2] |= srcdata[6] << srcshift;
/*TODO*///			dstdata[3] |= srcdata[5] << srcshift;
/*TODO*///			dstdata[4] |= srcdata[4] << srcshift;
/*TODO*///			dstdata[5] |= srcdata[3] << srcshift;
/*TODO*///			dstdata[6] |= srcdata[2] << srcshift;
/*TODO*///			dstdata[7] |= srcdata[1] << srcshift;
/*TODO*///			dstdata += 8;
/*TODO*///		}
/*TODO*///		while (dstdata < end)
/*TODO*///			*(dstdata++) |= *(srcdata--) << srcshift;
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_NtoN_blend_remap,(
/*TODO*///		const DATA_TYPE *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int srcshift),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo -= srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata <= end - 8)
/*TODO*///		{
/*TODO*///			dstdata[0] = paldata[dstdata[0] | (srcdata[0] << srcshift)];
/*TODO*///			dstdata[1] = paldata[dstdata[1] | (srcdata[1] << srcshift)];
/*TODO*///			dstdata[2] = paldata[dstdata[2] | (srcdata[2] << srcshift)];
/*TODO*///			dstdata[3] = paldata[dstdata[3] | (srcdata[3] << srcshift)];
/*TODO*///			dstdata[4] = paldata[dstdata[4] | (srcdata[4] << srcshift)];
/*TODO*///			dstdata[5] = paldata[dstdata[5] | (srcdata[5] << srcshift)];
/*TODO*///			dstdata[6] = paldata[dstdata[6] | (srcdata[6] << srcshift)];
/*TODO*///			dstdata[7] = paldata[dstdata[7] | (srcdata[7] << srcshift)];
/*TODO*///			dstdata += 8;
/*TODO*///			srcdata += 8;
/*TODO*///		}
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			*dstdata = paldata[*dstdata | (*(srcdata++) << srcshift)];
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(blockmove_NtoN_blend_remap_flipx,(
/*TODO*///		const DATA_TYPE *srcdata,int srcwidth,int srcheight,int srcmodulo,
/*TODO*///		DATA_TYPE *dstdata,int dstmodulo,
/*TODO*///		const UINT16 *paldata,int srcshift),
/*TODO*///{
/*TODO*///	DATA_TYPE *end;
/*TODO*///
/*TODO*///	srcmodulo += srcwidth;
/*TODO*///	dstmodulo -= srcwidth;
/*TODO*///	//srcdata += srcwidth-1;
/*TODO*///
/*TODO*///	while (srcheight)
/*TODO*///	{
/*TODO*///		end = dstdata + srcwidth;
/*TODO*///		while (dstdata <= end - 8)
/*TODO*///		{
/*TODO*///			srcdata -= 8;
/*TODO*///			dstdata[0] = paldata[dstdata[0] | (srcdata[8] << srcshift)];
/*TODO*///			dstdata[1] = paldata[dstdata[1] | (srcdata[7] << srcshift)];
/*TODO*///			dstdata[2] = paldata[dstdata[2] | (srcdata[6] << srcshift)];
/*TODO*///			dstdata[3] = paldata[dstdata[3] | (srcdata[5] << srcshift)];
/*TODO*///			dstdata[4] = paldata[dstdata[4] | (srcdata[4] << srcshift)];
/*TODO*///			dstdata[5] = paldata[dstdata[5] | (srcdata[3] << srcshift)];
/*TODO*///			dstdata[6] = paldata[dstdata[6] | (srcdata[2] << srcshift)];
/*TODO*///			dstdata[7] = paldata[dstdata[7] | (srcdata[1] << srcshift)];
/*TODO*///			dstdata += 8;
/*TODO*///		}
/*TODO*///		while (dstdata < end)
/*TODO*///		{
/*TODO*///			*dstdata = paldata[*dstdata | (*(srcdata--) << srcshift)];
/*TODO*///			dstdata++;
/*TODO*///		}
/*TODO*///
/*TODO*///		srcdata += srcmodulo;
/*TODO*///		dstdata += dstmodulo;
/*TODO*///		srcheight--;
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///
    public static void drawgfx_core8(
            mame_bitmap dest, GfxElement gfx,
            /*unsigned*/ int code,/*unsigned*/ int color, int flipx, int flipy, int sx, int sy,
            rectangle clip, int transparency, int transparent_color,
            mame_bitmap pri_buffer,/*UINT32*/ int pri_mask) {
        int ox;
        int oy;
        int ex;
        int ey;


        /* check bounds */
        ox = sx;
        oy = sy;

        ex = sx + gfx.width - 1;
        if (sx < 0) {
            sx = 0;
        }
        if (clip != null && sx < clip.min_x) {
            sx = clip.min_x;
        }
        if (ex >= dest.width) {
            ex = dest.width - 1;
        }
        if (clip != null && ex > clip.max_x) {
            ex = clip.max_x;
        }
        if (sx > ex) {
            return;
        }

        ey = sy + gfx.height - 1;
        if (sy < 0) {
            sy = 0;
        }
        if (clip != null && sy < clip.min_y) {
            sy = clip.min_y;
        }
        if (ey >= dest.height) {
            ey = dest.height - 1;
        }
        if (clip != null && ey > clip.max_y) {
            ey = clip.max_y;
        }
        if (sy > ey) {
            return;
        }

        osd_mark_dirty(sx, sy, ex, ey, 0);
        /* ASG 971011 */

        UBytePtr sd = new UBytePtr(gfx.gfxdata, code * gfx.char_modulo);
        /* source data */
        int sw = ex - sx + 1;
        /* source width */
        int sh = ey - sy + 1;
        /* source height */
        int sm = gfx.line_modulo;
        /* source modulo */
        UBytePtr dd = new UBytePtr(dest.line[sy], sx);
        /* dest data */
        int dm = (int) ((dest.line[1].offset) - (dest.line[0].offset));/* dest modulo */
        IntArray paldata = null;
        if (gfx.colortable != null) {
            paldata = new IntArray(gfx.colortable, gfx.color_granularity * color);
        }
        UBytePtr pribuf = (pri_buffer != null) ? new UBytePtr(pri_buffer.line[sy], sx) : null;

        if (flipx != 0) {
            //if ((sx-ox) == 0) sd += gfx.width - sw;
            sd.inc(gfx.width - 1 - (sx - ox));
        } else {
            sd.inc(sx - ox);
        }

        if (flipy != 0) {
            //if ((sy-oy) == 0) sd += sm * (gfx.height - sh);
            //dd += dm * (sh - 1);
            //dm = -dm;
            sd.inc(sm * (gfx.height - 1 - (sy - oy)));
            sm = -sm;
        } else {
            sd.inc(sm * (sy - oy));
        }

        switch (transparency) {
            case TRANSPARENCY_NONE:
                if (pribuf != null) {
                    if (flipx != 0) {
                        throw new UnsupportedOperationException("Unsupported");//blockmove_8toN_opaque_pri_flipx(sd, sw, sh, sm, dd, dm, paldata, pribuf, pri_mask);
                    } else {
                        throw new UnsupportedOperationException("Unsupported");//blockmove_8toN_opaque_pri(sd, sw, sh, sm, dd, dm, paldata, pribuf, pri_mask);
                    }
                } else if (flipx != 0) {
                    blockmove_8toN_opaque_flipx8(sd, sw, sh, sm, dd, dm, paldata);
                } else {
                    blockmove_8toN_opaque8(sd, sw, sh, sm, dd, dm, paldata);
                }

                break;
            case TRANSPARENCY_PEN:
                if (pribuf != null) {
                    if (flipx != 0) {
                        throw new UnsupportedOperationException("Unsupported");//blockmove_8toN_transpen_pri_flipx(sd, sw, sh, sm, dd, dm, paldata, transparent_color, pribuf, pri_mask);
                    } else {
                        throw new UnsupportedOperationException("Unsupported");//blockmove_8toN_transpen_pri(sd, sw, sh, sm, dd, dm, paldata, transparent_color, pribuf, pri_mask);
                    }
                } else if (flipx != 0) {
                    blockmove_8toN_transpen_flipx8(sd, sw, sh, sm, dd, dm, paldata, transparent_color);
                } else {
                    blockmove_8toN_transpen8(sd, sw, sh, sm, dd, dm, paldata, transparent_color);
                }
                break;
            case TRANSPARENCY_PENS:

                if (pribuf != null) {
                    /*TODO*///                    BLOCKMOVE(8toN_transmask_pri,flipx,(sd,sw,sh,sm,dd,dm,paldata,transparent_color,pribuf,pri_mask));				
                    throw new UnsupportedOperationException("unsupported");
                } else if (flipx != 0) {
                    blockmove_transmask_flipx8(sd, sw, sh, sm, dd, dm, paldata, transparent_color);
                } else {
                    blockmove_transmask8(sd, sw, sh, sm, dd, dm, paldata, transparent_color);
                }
                break;
            case TRANSPARENCY_COLOR:
                if (pribuf != null) {
                    /*TODO*///					BLOCKMOVE(8toN_transcolor_pri,flipx,(sd,sw,sh,sm,dd,dm,paldata,transparent_color,pribuf,pri_mask));
                    throw new UnsupportedOperationException("unsupported");
                } else if (flipx != 0) {
                    blockmove_8toN_transcolor_flipx8(sd, sw, sh, sm, dd, dm, paldata, transparent_color);
                } else {
                    blockmove_8toN_transcolor8(sd, sw, sh, sm, dd, dm, paldata, transparent_color);
                }
                break;
            /*case TRANSPARENCY_THROUGH:
                if (pribuf != null) {
                    usrintf_showmessage("pdrawgfx TRANS_THROUGH not supported");
                } else {
                    if (flipx != 0) {
                        blockmove_8toN_transthrough_flipx(sd, sw, sh, sm, dd, dm, paldata, transparent_color);
                    } else {
                        blockmove_8toN_transthrough(sd, sw, sh, sm, dd, dm, paldata, transparent_color);
                    }
                }

                break;*/

            case TRANSPARENCY_PEN_TABLE:
                throw new UnsupportedOperationException("unsupported");
            /*TODO*///				if (pribuf)
/*TODO*///usrintf_showmessage("pdrawgfx TRANS_PEN_TABLE not supported");
/*TODO*/////					BLOCKMOVE(8toN_pen_table_pri,flipx,(sd,sw,sh,sm,dd,dm,paldata,transparent_color));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_pen_table,flipx,(sd,sw,sh,sm,dd,dm,paldata,transparent_color));
/*TODO*///				break;
/*TODO*///
            case TRANSPARENCY_PEN_TABLE_RAW:
                throw new UnsupportedOperationException("unsupported");
            /*TODO*///				if (pribuf)
/*TODO*///usrintf_showmessage("pdrawgfx TRANS_PEN_TABLE_RAW not supported");
/*TODO*/////					BLOCKMOVE(8toN_pen_table_pri_raw,flipx,(sd,sw,sh,sm,dd,dm,color,transparent_color));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_pen_table_raw,flipx,(sd,sw,sh,sm,dd,dm,color,transparent_color));
/*TODO*///				break;
/*TODO*///
            case TRANSPARENCY_NONE_RAW:
                if (pribuf != null) {
                    throw new UnsupportedOperationException("unsupported");
                    /*TODO*///                    usrintf_showmessage("pdrawgfx TRANS_NONE_RAW not supported");
//					BLOCKMOVE(8toN_opaque_pri_raw,flipx,(sd,sw,sh,sm,dd,dm,paldata,pribuf,pri_mask));
                } else if (flipx != 0) {
                    blockmove_8toN_opaque_raw_flipx(sd, sw, sh, sm, dd, dm, color);
                } else {
                    //BLOCKMOVE(8toN_opaque_raw, flipx, (sd, sw, sh, sm, dd, dm, color));
                    blockmove_8toN_opaque_raw(sd, sw, sh, sm, dd, dm, color);
                }
                break;

            case TRANSPARENCY_PEN_RAW:
                if (pribuf != null) {
                    throw new UnsupportedOperationException("unsupported");
                } else if (flipx != 0) {
                    throw new UnsupportedOperationException("unsupported");
                } else {
                    blockmove_8toN_transpen_raw(sd, sw, sh, sm, dd, dm, color, transparent_color);
                }
//                throw new UnsupportedOperationException("unsupported");
                /*TODO*///				if (pribuf)
/*TODO*///usrintf_showmessage("pdrawgfx TRANS_PEN_RAW not supported");
/*TODO*/////					BLOCKMOVE(8toN_transpen_pri_raw,flipx,(sd,sw,sh,sm,dd,dm,paldata,pribuf,pri_mask));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_transpen_raw,flipx,(sd,sw,sh,sm,dd,dm,color,transparent_color));
/*TODO*///				break;
/*TODO*///
                break;
            case TRANSPARENCY_PENS_RAW:
                throw new UnsupportedOperationException("unsupported");
            /*TODO*///				if (pribuf)
/*TODO*///usrintf_showmessage("pdrawgfx TRANS_PENS_RAW not supported");
/*TODO*/////					BLOCKMOVE(8toN_transmask_pri_raw,flipx,(sd,sw,sh,sm,dd,dm,paldata,pribuf,pri_mask));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_transmask_raw,flipx,(sd,sw,sh,sm,dd,dm,color,transparent_color));
/*TODO*///				break;
/*TODO*///
          //  case TRANSPARENCY_THROUGH_RAW:
            //    throw new UnsupportedOperationException("unsupported");
            /*TODO*///				if (pribuf)
/*TODO*///usrintf_showmessage("pdrawgfx TRANS_PEN_RAW not supported");
/*TODO*/////					BLOCKMOVE(8toN_transpen_pri_raw,flipx,(sd,sw,sh,sm,dd,dm,paldata,pribuf,pri_mask));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_transthrough_raw,flipx,(sd,sw,sh,sm,dd,dm,color,transparent_color));
/*TODO*///				break;
/*TODO*///
            default:
                if (pribuf != null) {
                    usrintf_showmessage("pdrawgfx pen mode not supported");
                } else {
                    usrintf_showmessage("drawgfx pen mode not supported");
                }
                break;
        }
    }

    /*TODO*///DECLARE(drawgfx_core,(
/*TODO*///		struct osd_bitmap *dest,const struct GfxElement *gfx,
/*TODO*///		unsigned int code,unsigned int color,int flipx,int flipy,int sx,int sy,
/*TODO*///		const struct rectangle *clip,int transparency,int transparent_color,
/*TODO*///		struct osd_bitmap *pri_buffer,UINT32 pri_mask),
/*TODO*///{
/*TODO*///	int ox;
/*TODO*///	int oy;
/*TODO*///	int ex;
/*TODO*///	int ey;
/*TODO*///
/*TODO*///
/*TODO*///	/* check bounds */
/*TODO*///	ox = sx;
/*TODO*///	oy = sy;
/*TODO*///
/*TODO*///	ex = sx + gfx->width-1;
/*TODO*///	if (sx < 0) sx = 0;
/*TODO*///	if (clip && sx < clip->min_x) sx = clip->min_x;
/*TODO*///	if (ex >= dest->width) ex = dest->width-1;
/*TODO*///	if (clip && ex > clip->max_x) ex = clip->max_x;
/*TODO*///	if (sx > ex) return;
/*TODO*///
/*TODO*///	ey = sy + gfx->height-1;
/*TODO*///	if (sy < 0) sy = 0;
/*TODO*///	if (clip && sy < clip->min_y) sy = clip->min_y;
/*TODO*///	if (ey >= dest->height) ey = dest->height-1;
/*TODO*///	if (clip && ey > clip->max_y) ey = clip->max_y;
/*TODO*///	if (sy > ey) return;
/*TODO*///
/*TODO*///	osd_mark_dirty (sx,sy,ex,ey,0);	/* ASG 971011 */
/*TODO*///
/*TODO*///	{
/*TODO*///		UINT8 *sd = gfx->gfxdata + code * gfx->char_modulo;		/* source data */
/*TODO*///		int sw = ex-sx+1;										/* source width */
/*TODO*///		int sh = ey-sy+1;										/* source height */
/*TODO*///		int sm = gfx->line_modulo;								/* source modulo */
/*TODO*///		DATA_TYPE *dd = ((DATA_TYPE *)dest->line[sy]) + sx;		/* dest data */
/*TODO*///		int dm = ((DATA_TYPE *)dest->line[1])-((DATA_TYPE *)dest->line[0]);	/* dest modulo */
/*TODO*///		const UINT16 *paldata = &gfx->colortable[gfx->color_granularity * color];
/*TODO*///		UINT8 *pribuf = (pri_buffer) ? pri_buffer->line[sy] + sx : NULL;
/*TODO*///
/*TODO*///		if (flipx)
/*TODO*///		{
/*TODO*///			//if ((sx-ox) == 0) sd += gfx->width - sw;
/*TODO*///			sd += gfx->width -1 -(sx-ox);
/*TODO*///		}
/*TODO*///		else
/*TODO*///			sd += (sx-ox);
/*TODO*///
/*TODO*///		if (flipy)
/*TODO*///		{
/*TODO*///			//if ((sy-oy) == 0) sd += sm * (gfx->height - sh);
/*TODO*///			//dd += dm * (sh - 1);
/*TODO*///			//dm = -dm;
/*TODO*///			sd += sm * (gfx->height -1 -(sy-oy));
/*TODO*///			sm = -sm;
/*TODO*///		}
/*TODO*///		else
/*TODO*///			sd += sm * (sy-oy);
/*TODO*///
/*TODO*///		switch (transparency)
/*TODO*///		{
/*TODO*///			case TRANSPARENCY_NONE:
/*TODO*///				if (pribuf)
/*TODO*///					BLOCKMOVE(8toN_opaque_pri,flipx,(sd,sw,sh,sm,dd,dm,paldata,pribuf,pri_mask));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_opaque,flipx,(sd,sw,sh,sm,dd,dm,paldata));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_PEN:
/*TODO*///				if (pribuf)
/*TODO*///					BLOCKMOVE(8toN_transpen_pri,flipx,(sd,sw,sh,sm,dd,dm,paldata,transparent_color,pribuf,pri_mask));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_transpen,flipx,(sd,sw,sh,sm,dd,dm,paldata,transparent_color));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_PENS:
/*TODO*///				if (pribuf)
/*TODO*///					BLOCKMOVE(8toN_transmask_pri,flipx,(sd,sw,sh,sm,dd,dm,paldata,transparent_color,pribuf,pri_mask));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_transmask,flipx,(sd,sw,sh,sm,dd,dm,paldata,transparent_color));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_COLOR:
/*TODO*///				if (pribuf)
/*TODO*///					BLOCKMOVE(8toN_transcolor_pri,flipx,(sd,sw,sh,sm,dd,dm,paldata,transparent_color,pribuf,pri_mask));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_transcolor,flipx,(sd,sw,sh,sm,dd,dm,paldata,transparent_color));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_THROUGH:
/*TODO*///				if (pribuf)
/*TODO*///usrintf_showmessage("pdrawgfx TRANS_THROUGH not supported");
/*TODO*/////					BLOCKMOVE(8toN_transthrough,flipx,(sd,sw,sh,sm,dd,dm,paldata,transparent_color));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_transthrough,flipx,(sd,sw,sh,sm,dd,dm,paldata,transparent_color));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_PEN_TABLE:
/*TODO*///				if (pribuf)
/*TODO*///usrintf_showmessage("pdrawgfx TRANS_PEN_TABLE not supported");
/*TODO*/////					BLOCKMOVE(8toN_pen_table_pri,flipx,(sd,sw,sh,sm,dd,dm,paldata,transparent_color));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_pen_table,flipx,(sd,sw,sh,sm,dd,dm,paldata,transparent_color));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_PEN_TABLE_RAW:
/*TODO*///				if (pribuf)
/*TODO*///usrintf_showmessage("pdrawgfx TRANS_PEN_TABLE_RAW not supported");
/*TODO*/////					BLOCKMOVE(8toN_pen_table_pri_raw,flipx,(sd,sw,sh,sm,dd,dm,color,transparent_color));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_pen_table_raw,flipx,(sd,sw,sh,sm,dd,dm,color,transparent_color));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_NONE_RAW:
/*TODO*///				if (pribuf)
/*TODO*///usrintf_showmessage("pdrawgfx TRANS_NONE_RAW not supported");
/*TODO*/////					BLOCKMOVE(8toN_opaque_pri_raw,flipx,(sd,sw,sh,sm,dd,dm,paldata,pribuf,pri_mask));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_opaque_raw,flipx,(sd,sw,sh,sm,dd,dm,color));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_PEN_RAW:
/*TODO*///				if (pribuf)
/*TODO*///usrintf_showmessage("pdrawgfx TRANS_PEN_RAW not supported");
/*TODO*/////					BLOCKMOVE(8toN_transpen_pri_raw,flipx,(sd,sw,sh,sm,dd,dm,paldata,pribuf,pri_mask));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_transpen_raw,flipx,(sd,sw,sh,sm,dd,dm,color,transparent_color));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_PENS_RAW:
/*TODO*///				if (pribuf)
/*TODO*///usrintf_showmessage("pdrawgfx TRANS_PENS_RAW not supported");
/*TODO*/////					BLOCKMOVE(8toN_transmask_pri_raw,flipx,(sd,sw,sh,sm,dd,dm,paldata,pribuf,pri_mask));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_transmask_raw,flipx,(sd,sw,sh,sm,dd,dm,color,transparent_color));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_THROUGH_RAW:
/*TODO*///				if (pribuf)
/*TODO*///usrintf_showmessage("pdrawgfx TRANS_PEN_RAW not supported");
/*TODO*/////					BLOCKMOVE(8toN_transpen_pri_raw,flipx,(sd,sw,sh,sm,dd,dm,paldata,pribuf,pri_mask));
/*TODO*///				else
/*TODO*///					BLOCKMOVE(8toN_transthrough_raw,flipx,(sd,sw,sh,sm,dd,dm,color,transparent_color));
/*TODO*///				break;
/*TODO*///
/*TODO*///			default:
/*TODO*///				if (pribuf)
/*TODO*///					usrintf_showmessage("pdrawgfx pen mode not supported");
/*TODO*///				else
/*TODO*///					usrintf_showmessage("drawgfx pen mode not supported");
/*TODO*///				break;
/*TODO*///		}
/*TODO*///	}
/*TODO*///})
/*TODO*///
    public static void copybitmap_core8(
            mame_bitmap dest, mame_bitmap src,
            int flipx, int flipy, int sx, int sy,
            rectangle clip, int transparency, int transparent_color) {
        int ox;
        int oy;
        int ex;
        int ey;


        /* check bounds */
        ox = sx;
        oy = sy;

        ex = sx + src.width - 1;
        if (sx < 0) {
            sx = 0;
        }
        if (clip != null && sx < clip.min_x) {
            sx = clip.min_x;
        }
        if (ex >= dest.width) {
            ex = dest.width - 1;
        }
        if (clip != null && ex > clip.max_x) {
            ex = clip.max_x;
        }
        if (sx > ex) {
            return;
        }

        ey = sy + src.height - 1;
        if (sy < 0) {
            sy = 0;
        }
        if (clip != null && sy < clip.min_y) {
            sy = clip.min_y;
        }
        if (ey >= dest.height) {
            ey = dest.height - 1;
        }
        if (clip != null && ey > clip.max_y) {
            ey = clip.max_y;
        }
        if (sy > ey) {
            return;
        }

        {
            UBytePtr sd = new UBytePtr(src.line[0]);
            /* source data */
            int sw = ex - sx + 1;
            /* source width */
            int sh = ey - sy + 1;
            /* source height */
            int sm = (src.line[1].offset) - (src.line[0].offset);
            /* source modulo */
            UBytePtr dd = new UBytePtr(dest.line[sy], sx);
            /* dest data */
            int dm = (dest.line[1].offset) - (dest.line[0].offset);
            /* dest modulo */

            if (flipx != 0) {
                //if ((sx-ox) == 0) sd += gfx.width - sw;
                sd.inc(src.width - 1 - (sx - ox));
            } else {
                sd.inc(sx - ox);
            }

            if (flipy != 0) {
                //if ((sy-oy) == 0) sd += sm * (gfx.height - sh);
                //dd += dm * (sh - 1);
                //dm = -dm;
                sd.inc(sm * (src.height - 1 - (sy - oy)));
                sm = -sm;
            } else {
                sd.inc(sm * (sy - oy));
            }

            switch (transparency) {
                case TRANSPARENCY_NONE:
                    if (flipx != 0) {
                        throw new UnsupportedOperationException("unimplemented");//BLOCKMOVE(NtoN_opaque_remap,flipx,(sd,sw,sh,sm,dd,dm,Machine.pens));
                    } else {
                        blockmove_NtoN_opaque_remap8(sd, sw, sh, sm, dd, dm, Machine.pens);
                    }
                    break;
                case TRANSPARENCY_NONE_RAW:
                    if (flipx != 0) {
                        blockmove_NtoN_opaque_noremap_flipx8(sd, sw, sh, sm, dd, dm);
                    } else {
                        blockmove_NtoN_opaque_noremap8(sd, sw, sh, sm, dd, dm);
                    }
                    break;

                case TRANSPARENCY_PEN_RAW:
                    if (flipx != 0) {
                        blockmove_NtoN_transpen_noremap_flipx8(sd, sw, sh, sm, dd, dm, transparent_color);
                    } else {
                        blockmove_NtoN_transpen_noremap8(sd, sw, sh, sm, dd, dm, transparent_color);
                    }
                    break;
                /*case TRANSPARENCY_THROUGH_RAW:
                    if (flipx != 0) {
                        blockmove_NtoN_transthrough_noremap_flipx8(sd, sw, sh, sm, dd, dm, transparent_color);
                    } else {
                        blockmove_NtoN_transthrough_noremap8(sd, sw, sh, sm, dd, dm, transparent_color);
                    }
                    break;*/
                case TRANSPARENCY_BLEND:
                    throw new UnsupportedOperationException("unimplemented");
                /*TODO*///				BLOCKMOVE(NtoN_blend_remap,flipx,(sd,sw,sh,sm,dd,dm,Machine.pens,transparent_color));
/*TODO*///				break;
/*TODO*///
                case TRANSPARENCY_BLEND_RAW:
                    throw new UnsupportedOperationException("unimplemented");
                /*TODO*///				BLOCKMOVE(NtoN_blend_noremap,flipx,(sd,sw,sh,sm,dd,dm,transparent_color));
/*TODO*///				break;
                default:
                    usrintf_showmessage("copybitmap pen mode not supported");
                    break;
            }
        }
    }
    /*TODO*///DECLARE(copybitmap_core,(
/*TODO*///		struct osd_bitmap *dest,struct osd_bitmap *src,
/*TODO*///		int flipx,int flipy,int sx,int sy,
/*TODO*///		const struct rectangle *clip,int transparency,int transparent_color),
/*TODO*///{
/*TODO*///	int ox;
/*TODO*///	int oy;
/*TODO*///	int ex;
/*TODO*///	int ey;
/*TODO*///
/*TODO*///
/*TODO*///	/* check bounds */
/*TODO*///	ox = sx;
/*TODO*///	oy = sy;
/*TODO*///
/*TODO*///	ex = sx + src->width-1;
/*TODO*///	if (sx < 0) sx = 0;
/*TODO*///	if (clip && sx < clip->min_x) sx = clip->min_x;
/*TODO*///	if (ex >= dest->width) ex = dest->width-1;
/*TODO*///	if (clip && ex > clip->max_x) ex = clip->max_x;
/*TODO*///	if (sx > ex) return;
/*TODO*///
/*TODO*///	ey = sy + src->height-1;
/*TODO*///	if (sy < 0) sy = 0;
/*TODO*///	if (clip && sy < clip->min_y) sy = clip->min_y;
/*TODO*///	if (ey >= dest->height) ey = dest->height-1;
/*TODO*///	if (clip && ey > clip->max_y) ey = clip->max_y;
/*TODO*///	if (sy > ey) return;
/*TODO*///
/*TODO*///	{
/*TODO*///		DATA_TYPE *sd = ((DATA_TYPE *)src->line[0]);							/* source data */
/*TODO*///		int sw = ex-sx+1;														/* source width */
/*TODO*///		int sh = ey-sy+1;														/* source height */
/*TODO*///		int sm = ((DATA_TYPE *)src->line[1])-((DATA_TYPE *)src->line[0]);		/* source modulo */
/*TODO*///		DATA_TYPE *dd = ((DATA_TYPE *)dest->line[sy]) + sx;						/* dest data */
/*TODO*///		int dm = ((DATA_TYPE *)dest->line[1])-((DATA_TYPE *)dest->line[0]);		/* dest modulo */
/*TODO*///
/*TODO*///		if (flipx)
/*TODO*///		{
/*TODO*///			//if ((sx-ox) == 0) sd += gfx->width - sw;
/*TODO*///			sd += src->width -1 -(sx-ox);
/*TODO*///		}
/*TODO*///		else
/*TODO*///			sd += (sx-ox);
/*TODO*///
/*TODO*///		if (flipy)
/*TODO*///		{
/*TODO*///			//if ((sy-oy) == 0) sd += sm * (gfx->height - sh);
/*TODO*///			//dd += dm * (sh - 1);
/*TODO*///			//dm = -dm;
/*TODO*///			sd += sm * (src->height -1 -(sy-oy));
/*TODO*///			sm = -sm;
/*TODO*///		}
/*TODO*///		else
/*TODO*///			sd += sm * (sy-oy);
/*TODO*///
/*TODO*///		switch (transparency)
/*TODO*///		{
/*TODO*///			case TRANSPARENCY_NONE:
/*TODO*///				BLOCKMOVE(NtoN_opaque_remap,flipx,(sd,sw,sh,sm,dd,dm,Machine->pens));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_NONE_RAW:
/*TODO*///				BLOCKMOVE(NtoN_opaque_noremap,flipx,(sd,sw,sh,sm,dd,dm));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_PEN_RAW:
/*TODO*///				BLOCKMOVE(NtoN_transpen_noremap,flipx,(sd,sw,sh,sm,dd,dm,transparent_color));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_THROUGH_RAW:
/*TODO*///				BLOCKMOVE(NtoN_transthrough_noremap,flipx,(sd,sw,sh,sm,dd,dm,transparent_color));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_BLEND:
/*TODO*///				BLOCKMOVE(NtoN_blend_remap,flipx,(sd,sw,sh,sm,dd,dm,Machine->pens,transparent_color));
/*TODO*///				break;
/*TODO*///
/*TODO*///			case TRANSPARENCY_BLEND_RAW:
/*TODO*///				BLOCKMOVE(NtoN_blend_noremap,flipx,(sd,sw,sh,sm,dd,dm,transparent_color));
/*TODO*///				break;
/*TODO*///
/*TODO*///			default:
/*TODO*///				usrintf_showmessage("copybitmap pen mode not supported");
/*TODO*///				break;
/*TODO*///		}
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///DECLARE(copyrozbitmap_core,(struct osd_bitmap *bitmap,struct osd_bitmap *srcbitmap,
/*TODO*///		UINT32 startx,UINT32 starty,int incxx,int incxy,int incyx,int incyy,int wraparound,
/*TODO*///		const struct rectangle *clip,int transparency,int transparent_color,UINT32 priority),
/*TODO*///{
/*TODO*///	UINT32 cx;
/*TODO*///	UINT32 cy;
/*TODO*///	int x;
/*TODO*///	int sx;
/*TODO*///	int sy;
/*TODO*///	int ex;
/*TODO*///	int ey;
/*TODO*///	const int xmask = srcbitmap->width-1;
/*TODO*///	const int ymask = srcbitmap->height-1;
/*TODO*///	const int widthshifted = srcbitmap->width << 16;
/*TODO*///	const int heightshifted = srcbitmap->height << 16;
/*TODO*///	DATA_TYPE *dest;
/*TODO*///
/*TODO*///
/*TODO*///	if (clip)
/*TODO*///	{
/*TODO*///		startx += clip->min_x * incxx + clip->min_y * incyx;
/*TODO*///		starty += clip->min_x * incxy + clip->min_y * incyy;
/*TODO*///
/*TODO*///		sx = clip->min_x;
/*TODO*///		sy = clip->min_y;
/*TODO*///		ex = clip->max_x;
/*TODO*///		ey = clip->max_y;
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		sx = 0;
/*TODO*///		sy = 0;
/*TODO*///		ex = bitmap->width-1;
/*TODO*///		ey = bitmap->height-1;
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///	if (Machine->orientation & ORIENTATION_SWAP_XY)
/*TODO*///	{
/*TODO*///		int t;
/*TODO*///
/*TODO*///		t = startx; startx = starty; starty = t;
/*TODO*///		t = sx; sx = sy; sy = t;
/*TODO*///		t = ex; ex = ey; ey = t;
/*TODO*///		t = incxx; incxx = incyy; incyy = t;
/*TODO*///		t = incxy; incxy = incyx; incyx = t;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (Machine->orientation & ORIENTATION_FLIP_X)
/*TODO*///	{
/*TODO*///		int w = ex - sx;
/*TODO*///
/*TODO*///		incxy = -incxy;
/*TODO*///		incyx = -incyx;
/*TODO*///		startx = widthshifted - startx - 1;
/*TODO*///		startx -= incxx * w;
/*TODO*///		starty -= incxy * w;
/*TODO*///
/*TODO*///		w = sx;
/*TODO*///		sx = bitmap->width-1 - ex;
/*TODO*///		ex = bitmap->width-1 - w;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (Machine->orientation & ORIENTATION_FLIP_Y)
/*TODO*///	{
/*TODO*///		int h = ey - sy;
/*TODO*///
/*TODO*///		incxy = -incxy;
/*TODO*///		incyx = -incyx;
/*TODO*///		starty = heightshifted - starty - 1;
/*TODO*///		startx -= incyx * h;
/*TODO*///		starty -= incyy * h;
/*TODO*///
/*TODO*///		h = sy;
/*TODO*///		sy = bitmap->height-1 - ey;
/*TODO*///		ey = bitmap->height-1 - h;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (incxy == 0 && incyx == 0 && !wraparound)
/*TODO*///	{
/*TODO*///		/* optimized loop for the not rotated case */
/*TODO*///
/*TODO*///		if (incxx == 0x10000)
/*TODO*///		{
/*TODO*///			/* optimized loop for the not zoomed case */
/*TODO*///
/*TODO*///			/* startx is unsigned */
/*TODO*///			startx = ((INT32)startx) >> 16;
/*TODO*///
/*TODO*///			if (startx >= srcbitmap->width)
/*TODO*///			{
/*TODO*///				sx += -startx;
/*TODO*///				startx = 0;
/*TODO*///			}
/*TODO*///
/*TODO*///			if (sx <= ex)
/*TODO*///			{
/*TODO*///				while (sy <= ey)
/*TODO*///				{
/*TODO*///					if (starty < heightshifted)
/*TODO*///					{
/*TODO*///						x = sx;
/*TODO*///						cx = startx;
/*TODO*///						cy = starty >> 16;
/*TODO*///						dest = ((DATA_TYPE *)bitmap->line[sy]) + sx;
/*TODO*///						if (priority)
/*TODO*///						{
/*TODO*///							UINT8 *pri = &priority_bitmap->line[sy][sx];
/*TODO*///							DATA_TYPE *src = (DATA_TYPE *)srcbitmap->line[cy];
/*TODO*///
/*TODO*///							while (x <= ex && cx < srcbitmap->width)
/*TODO*///							{
/*TODO*///								int c = src[cx];
/*TODO*///
/*TODO*///								if (c != transparent_color)
/*TODO*///								{
/*TODO*///									*dest = c;
/*TODO*///									*pri |= priority;
/*TODO*///								}
/*TODO*///
/*TODO*///								cx++;
/*TODO*///								x++;
/*TODO*///								dest++;
/*TODO*///								pri++;
/*TODO*///							}
/*TODO*///						}
/*TODO*///						else
/*TODO*///						{
/*TODO*///							DATA_TYPE *src = (DATA_TYPE *)srcbitmap->line[cy];
/*TODO*///
/*TODO*///							while (x <= ex && cx < srcbitmap->width)
/*TODO*///							{
/*TODO*///								int c = src[cx];
/*TODO*///
/*TODO*///								if (c != transparent_color)
/*TODO*///									*dest = c;
/*TODO*///
/*TODO*///								cx++;
/*TODO*///								x++;
/*TODO*///								dest++;
/*TODO*///							}
/*TODO*///						}
/*TODO*///					}
/*TODO*///					starty += incyy;
/*TODO*///					sy++;
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			while (startx >= widthshifted && sx <= ex)
/*TODO*///			{
/*TODO*///				startx += incxx;
/*TODO*///				sx++;
/*TODO*///			}
/*TODO*///
/*TODO*///			if (sx <= ex)
/*TODO*///			{
/*TODO*///				while (sy <= ey)
/*TODO*///				{
/*TODO*///					if (starty < heightshifted)
/*TODO*///					{
/*TODO*///						x = sx;
/*TODO*///						cx = startx;
/*TODO*///						cy = starty >> 16;
/*TODO*///						dest = ((DATA_TYPE *)bitmap->line[sy]) + sx;
/*TODO*///						if (priority)
/*TODO*///						{
/*TODO*///							UINT8 *pri = &priority_bitmap->line[sy][sx];
/*TODO*///							DATA_TYPE *src = (DATA_TYPE *)srcbitmap->line[cy];
/*TODO*///
/*TODO*///							while (x <= ex && cx < widthshifted)
/*TODO*///							{
/*TODO*///								int c = src[cx >> 16];
/*TODO*///
/*TODO*///								if (c != transparent_color)
/*TODO*///								{
/*TODO*///									*dest = c;
/*TODO*///									*pri |= priority;
/*TODO*///								}
/*TODO*///
/*TODO*///								cx += incxx;
/*TODO*///								x++;
/*TODO*///								dest++;
/*TODO*///								pri++;
/*TODO*///							}
/*TODO*///						}
/*TODO*///						else
/*TODO*///						{
/*TODO*///							DATA_TYPE *src = (DATA_TYPE *)srcbitmap->line[cy];
/*TODO*///
/*TODO*///							while (x <= ex && cx < widthshifted)
/*TODO*///							{
/*TODO*///								int c = src[cx >> 16];
/*TODO*///
/*TODO*///								if (c != transparent_color)
/*TODO*///									*dest = c;
/*TODO*///
/*TODO*///								cx += incxx;
/*TODO*///								x++;
/*TODO*///								dest++;
/*TODO*///							}
/*TODO*///						}
/*TODO*///					}
/*TODO*///					starty += incyy;
/*TODO*///					sy++;
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		if (wraparound)
/*TODO*///		{
/*TODO*///			/* plot with wraparound */
/*TODO*///			while (sy <= ey)
/*TODO*///			{
/*TODO*///				x = sx;
/*TODO*///				cx = startx;
/*TODO*///				cy = starty;
/*TODO*///				dest = ((DATA_TYPE *)bitmap->line[sy]) + sx;
/*TODO*///				if (priority)
/*TODO*///				{
/*TODO*///					UINT8 *pri = &priority_bitmap->line[sy][sx];
/*TODO*///
/*TODO*///					while (x <= ex)
/*TODO*///					{
/*TODO*///						int c = ((DATA_TYPE *)srcbitmap->line[(cy >> 16) & xmask])[(cx >> 16) & ymask];
/*TODO*///
/*TODO*///						if (c != transparent_color)
/*TODO*///						{
/*TODO*///							*dest = c;
/*TODO*///							*pri |= priority;
/*TODO*///						}
/*TODO*///
/*TODO*///						cx += incxx;
/*TODO*///						cy += incxy;
/*TODO*///						x++;
/*TODO*///						dest++;
/*TODO*///						pri++;
/*TODO*///					}
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					while (x <= ex)
/*TODO*///					{
/*TODO*///						int c = ((DATA_TYPE *)srcbitmap->line[(cy >> 16) & xmask])[(cx >> 16) & ymask];
/*TODO*///
/*TODO*///						if (c != transparent_color)
/*TODO*///							*dest = c;
/*TODO*///
/*TODO*///						cx += incxx;
/*TODO*///						cy += incxy;
/*TODO*///						x++;
/*TODO*///						dest++;
/*TODO*///					}
/*TODO*///				}
/*TODO*///				startx += incyx;
/*TODO*///				starty += incyy;
/*TODO*///				sy++;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			while (sy <= ey)
/*TODO*///			{
/*TODO*///				x = sx;
/*TODO*///				cx = startx;
/*TODO*///				cy = starty;
/*TODO*///				dest = ((DATA_TYPE *)bitmap->line[sy]) + sx;
/*TODO*///				if (priority)
/*TODO*///				{
/*TODO*///					UINT8 *pri = &priority_bitmap->line[sy][sx];
/*TODO*///
/*TODO*///					while (x <= ex)
/*TODO*///					{
/*TODO*///						if (cx < widthshifted && cy < heightshifted)
/*TODO*///						{
/*TODO*///							int c = ((DATA_TYPE *)srcbitmap->line[cy >> 16])[cx >> 16];
/*TODO*///
/*TODO*///							if (c != transparent_color)
/*TODO*///							{
/*TODO*///								*dest = c;
/*TODO*///								*pri |= priority;
/*TODO*///							}
/*TODO*///						}
/*TODO*///
/*TODO*///						cx += incxx;
/*TODO*///						cy += incxy;
/*TODO*///						x++;
/*TODO*///						dest++;
/*TODO*///						pri++;
/*TODO*///					}
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					while (x <= ex)
/*TODO*///					{
/*TODO*///						if (cx < widthshifted && cy < heightshifted)
/*TODO*///						{
/*TODO*///							int c = ((DATA_TYPE *)srcbitmap->line[cy >> 16])[cx >> 16];
/*TODO*///
/*TODO*///							if (c != transparent_color)
/*TODO*///								*dest = c;
/*TODO*///						}
/*TODO*///
/*TODO*///						cx += incxx;
/*TODO*///						cy += incxy;
/*TODO*///						x++;
/*TODO*///						dest++;
/*TODO*///					}
/*TODO*///				}
/*TODO*///				startx += incyx;
/*TODO*///				starty += incyy;
/*TODO*///				sy++;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///})
/*TODO*///
/*TODO*///#endif /* DECLARE */
/*TODO*///
}
