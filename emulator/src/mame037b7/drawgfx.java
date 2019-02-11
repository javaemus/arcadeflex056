/*
 *  Ported to 0.37b7
 */
package mame037b7;

import static common.ptr.*;
import static mame056.mame.Machine;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.driverH.ORIENTATION_SWAP_XY;
import static mame037b5.drawgfx.*;
public class drawgfx {


    /**
     * *************************************************************************
     *
     * Copy a bitmap onto another with scroll and wraparound. This function
     * supports multiple independently scrolling rows/columns. "rows" is the
     * number of indepentently scrolling rows. "rowscroll" is an array of
     * integers telling how much to scroll each row. Same thing for "cols" and
     * "colscroll". If the bitmap cannot scroll in one direction, set rows or
     * columns to 0. If the bitmap scrolls as a whole, set rows and/or cols to
     * 1. Bidirectional scrolling is, of course, supported only if the bitmap
     * scrolls as a whole in at least one direction.
     *
     **************************************************************************
     */
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

    public static void copyscrollbitmap_remap(mame_bitmap dest, mame_bitmap src,
            int rows, int[] rowscroll, int cols, int[] colscroll,
            rectangle clip, int transparency, int transparent_color) {
        int srcwidth, srcheight, destwidth, destheight;
        rectangle orig_clip = new rectangle();

        if (clip != null) {
            orig_clip.min_x = clip.min_x;
            orig_clip.max_x = clip.max_x;
            orig_clip.min_y = clip.min_y;
            orig_clip.max_y = clip.max_y;
        } else {
            orig_clip.min_x = 0;
            orig_clip.max_x = dest.width - 1;
            orig_clip.min_y = 0;
            orig_clip.max_y = dest.height - 1;
        }
        clip = orig_clip;

        if (rows == 0 && cols == 0) {
            copybitmap(dest, src, 0, 0, 0, 0, clip, transparency, transparent_color);
            return;
        }

        if ((Machine.orientation & ORIENTATION_SWAP_XY) != 0) {
            srcwidth = src.height;
            srcheight = src.width;
            destwidth = dest.height;
            destheight = dest.width;
        } else {
            srcwidth = src.width;
            srcheight = src.height;
            destwidth = dest.width;
            destheight = dest.height;
        }
        if (rows == 0) {
            /* scrolling columns */
            int col, colwidth;
            rectangle myclip = new rectangle();

            colwidth = srcwidth / cols;

            myclip.min_y = clip.min_y;
            myclip.max_y = clip.max_y;

            col = 0;
            while (col < cols) {
                int cons, scroll;


                /* count consecutive columns scrolled by the same amount */
                scroll = colscroll[col];
                cons = 1;
                while (col + cons < cols && colscroll[col + cons] == scroll) {
                    cons++;
                }

                if (scroll < 0) {
                    scroll = srcheight - (-scroll) % srcheight;
                } else {
                    scroll %= srcheight;
                }

                myclip.min_x = col * colwidth;
                if (myclip.min_x < clip.min_x) {
                    myclip.min_x = clip.min_x;
                }
                myclip.max_x = (col + cons) * colwidth - 1;
                if (myclip.max_x > clip.max_x) {
                    myclip.max_x = clip.max_x;
                }

                copybitmap(dest, src, 0, 0, 0, scroll, myclip, transparency, transparent_color);
                copybitmap(dest, src, 0, 0, 0, scroll - srcheight, myclip, transparency, transparent_color);

                col += cons;
            }
        } else if (cols == 0) {
            /* scrolling rows */
            int row, rowheight;
            rectangle myclip = new rectangle();

            rowheight = srcheight / rows;

            myclip.min_x = clip.min_x;
            myclip.max_x = clip.max_x;

            row = 0;
            while (row < rows) {
                int cons, scroll;


                /* count consecutive rows scrolled by the same amount */
                scroll = rowscroll[row];
                cons = 1;
                while (row + cons < rows && rowscroll[row + cons] == scroll) {
                    cons++;
                }

                if (scroll < 0) {
                    scroll = srcwidth - (-scroll) % srcwidth;
                } else {
                    scroll %= srcwidth;
                }

                myclip.min_y = row * rowheight;
                if (myclip.min_y < clip.min_y) {
                    myclip.min_y = clip.min_y;
                }
                myclip.max_y = (row + cons) * rowheight - 1;
                if (myclip.max_y > clip.max_y) {
                    myclip.max_y = clip.max_y;
                }

                copybitmap(dest, src, 0, 0, scroll, 0, myclip, transparency, transparent_color);
                copybitmap(dest, src, 0, 0, scroll - srcwidth, 0, myclip, transparency, transparent_color);

                row += cons;
            }
        } else if (rows == 1 && cols == 1) {
            /* XY scrolling playfield */
            int scrollx, scrolly, sx, sy;

            if (rowscroll[0] < 0) {
                scrollx = srcwidth - (-rowscroll[0]) % srcwidth;
            } else {
                scrollx = rowscroll[0] % srcwidth;
            }

            if (colscroll[0] < 0) {
                scrolly = srcheight - (-colscroll[0]) % srcheight;
            } else {
                scrolly = colscroll[0] % srcheight;
            }

            for (sx = scrollx - srcwidth; sx < destwidth; sx += srcwidth) {
                for (sy = scrolly - srcheight; sy < destheight; sy += srcheight) {
                    copybitmap(dest, src, 0, 0, sx, sy, clip, transparency, transparent_color);
                }
            }
        } else if (rows == 1) {
            /* scrolling columns + horizontal scroll */
            int col, colwidth;
            int scrollx;
            rectangle myclip = new rectangle();

            if (rowscroll[0] < 0) {
                scrollx = srcwidth - (-rowscroll[0]) % srcwidth;
            } else {
                scrollx = rowscroll[0] % srcwidth;
            }

            colwidth = srcwidth / cols;

            myclip.min_y = clip.min_y;
            myclip.max_y = clip.max_y;

            col = 0;
            while (col < cols) {
                int cons, scroll;


                /* count consecutive columns scrolled by the same amount */
                scroll = colscroll[col];
                cons = 1;
                while (col + cons < cols && colscroll[col + cons] == scroll) {
                    cons++;
                }

                if (scroll < 0) {
                    scroll = srcheight - (-scroll) % srcheight;
                } else {
                    scroll %= srcheight;
                }

                myclip.min_x = col * colwidth + scrollx;
                if (myclip.min_x < clip.min_x) {
                    myclip.min_x = clip.min_x;
                }
                myclip.max_x = (col + cons) * colwidth - 1 + scrollx;
                if (myclip.max_x > clip.max_x) {
                    myclip.max_x = clip.max_x;
                }

                copybitmap(dest, src, 0, 0, scrollx, scroll, myclip, transparency, transparent_color);
                copybitmap(dest, src, 0, 0, scrollx, scroll - srcheight, myclip, transparency, transparent_color);

                myclip.min_x = col * colwidth + scrollx - srcwidth;
                if (myclip.min_x < clip.min_x) {
                    myclip.min_x = clip.min_x;
                }
                myclip.max_x = (col + cons) * colwidth - 1 + scrollx - srcwidth;
                if (myclip.max_x > clip.max_x) {
                    myclip.max_x = clip.max_x;
                }

                copybitmap(dest, src, 0, 0, scrollx - srcwidth, scroll, myclip, transparency, transparent_color);
                copybitmap(dest, src, 0, 0, scrollx - srcwidth, scroll - srcheight, myclip, transparency, transparent_color);

                col += cons;
            }
        } else if (cols == 1) {
            /* scrolling rows + vertical scroll */
            int row, rowheight;
            int scrolly;
            rectangle myclip = new rectangle();

            if (colscroll[0] < 0) {
                scrolly = srcheight - (-colscroll[0]) % srcheight;
            } else {
                scrolly = colscroll[0] % srcheight;
            }

            rowheight = srcheight / rows;

            myclip.min_x = clip.min_x;
            myclip.max_x = clip.max_x;

            row = 0;
            while (row < rows) {
                int cons, scroll;


                /* count consecutive rows scrolled by the same amount */
                scroll = rowscroll[row];
                cons = 1;
                while (row + cons < rows && rowscroll[row + cons] == scroll) {
                    cons++;
                }

                if (scroll < 0) {
                    scroll = srcwidth - (-scroll) % srcwidth;
                } else {
                    scroll %= srcwidth;
                }

                myclip.min_y = row * rowheight + scrolly;
                if (myclip.min_y < clip.min_y) {
                    myclip.min_y = clip.min_y;
                }
                myclip.max_y = (row + cons) * rowheight - 1 + scrolly;
                if (myclip.max_y > clip.max_y) {
                    myclip.max_y = clip.max_y;
                }

                copybitmap(dest, src, 0, 0, scroll, scrolly, myclip, transparency, transparent_color);
                copybitmap(dest, src, 0, 0, scroll - srcwidth, scrolly, myclip, transparency, transparent_color);

                myclip.min_y = row * rowheight + scrolly - srcheight;
                if (myclip.min_y < clip.min_y) {
                    myclip.min_y = clip.min_y;
                }
                myclip.max_y = (row + cons) * rowheight - 1 + scrolly - srcheight;
                if (myclip.max_y > clip.max_y) {
                    myclip.max_y = clip.max_y;
                }

                copybitmap(dest, src, 0, 0, scroll, scrolly - srcheight, myclip, transparency, transparent_color);
                copybitmap(dest, src, 0, 0, scroll - srcwidth, scrolly - srcheight, myclip, transparency, transparent_color);

                row += cons;
            }
        }

    }

    public static void blockmove_NtoN_opaque_noremap_flipx8(UBytePtr srcdata, int srcwidth, int srcheight, int srcmodulo, UBytePtr dstdata, int dstmodulo) {
        int end;

        srcmodulo += srcwidth;
        dstmodulo -= srcwidth;
        //srcdata += srcwidth-1;

        while (srcheight != 0) {
            end = dstdata.offset + srcwidth;
            while (dstdata.offset <= end - 8) {
                srcdata.offset -= 8;
                dstdata.write(0, srcdata.read(8));
                dstdata.write(1, srcdata.read(7));
                dstdata.write(2, srcdata.read(6));
                dstdata.write(3, srcdata.read(5));
                dstdata.write(4, srcdata.read(4));
                dstdata.write(5, srcdata.read(3));
                dstdata.write(6, srcdata.read(2));
                dstdata.write(7, srcdata.read(1));
                dstdata.offset += 8;
            }
            while (dstdata.offset < end) {
                dstdata.writeinc((srcdata.readdec()));
            }

            srcdata.offset += srcmodulo;
            dstdata.offset += dstmodulo;
            srcheight--;
        }
    }
}
