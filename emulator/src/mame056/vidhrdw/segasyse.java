/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
/**
 * Changelog
 * =========
 * 13/05/2019 ported to mame 0.56 (shadow)
 */
package mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static arcadeflex056.video.*;

import static common.libc.cstring.*;
import static common.ptr.*;
import static common.subArrays.*;

import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.palette.*;

import static mame056.drivers.segasyse.*;

public class segasyse {

    /*-- Variables --*/
    public static final int CHIPS = 2;/* There are 2 VDP Chips */

    public static int[]/*UINT8*/ u8_segae_vdp_cmdpart = new int[CHIPS];/* VDP Command Part Counter */
    public static int[]/*UINT16*/ u16_segae_vdp_command = new int[CHIPS];/* VDP Command Word */

    public static int[]/*UINT8*/ u8_segae_vdp_accessmode = new int[CHIPS];/* VDP Access Mode (VRAM, CRAM)	*/
    public static int[]/*UINT16*/ u16_segae_vdp_accessaddr = new int[CHIPS];/* VDP Access Address */
    public static int[]/*UINT8*/ u8_segae_vdp_readbuffer = new int[CHIPS];/* VDP Read Buffer */

    public static UBytePtr[] segae_vdp_vram = new UBytePtr[CHIPS];/* Pointer to VRAM */
    public static UBytePtr[] segae_vdp_cram = new UBytePtr[CHIPS];/* Pointer to the VDP's CRAM */
    public static UBytePtr[] segae_vdp_regs = new UBytePtr[CHIPS];/* Pointer to the VDP's Registers */

    public static int[]/*UINT8*/ u8_segae_vdp_vrambank = new int[CHIPS];/* Current VRAM Bank number (from writes to Port 0xf7) */

    public static UBytePtr cache_bitmap;/* 8bpp bitmap with raw pen values */


    /**
     * *****************************************************************************
     * vhstart, vhstop and vhrefresh functions
     * *****************************************************************************
     */
    public static VhStartPtr segae_vh_start = new VhStartPtr() {
        public int handler() {
            int/*UINT8*/ temp;

            for (temp = 0; temp < CHIPS; temp++) {
                if (segae_vdp_start(temp) != 0) {
                    return 1;
                }
            }

            cache_bitmap = new UBytePtr((16 + 256 + 16) * 192);/* 16 pixels either side to simplify drawing */

            if (cache_bitmap == null) {
                return 1;
            }

            return 0;
        }
    };

    public static VhStopPtr segae_vh_stop = new VhStopPtr() {
        public void handler() {
            int temp;

            for (temp = 0; temp < CHIPS; temp++) {
                segae_vdp_stop(temp);
            }

            cache_bitmap = null;
        }
    };

    public static VhUpdatePtr segae_vh_screenrefresh = new VhUpdatePtr() {
        public void handler(mame_bitmap bitmap, int full_refresh) {
            int i;

            /*- Draw from cache_bitmap to screen -*/
            for (i = 0; i < 192; i++) {
                draw_scanline8(bitmap, 0, i, 256, new UBytePtr(cache_bitmap, i * (16 + 256 + 16) + 16), new IntArray(Machine.pens), -1);
            }
        }
    };

    /**
     * *****************************************************************************
     * VDP Start / Stop Functions
     * *******************************************************************************
     * note: we really should check after each allocation to make sure it was
     * successful then if one allocation fails we can free up the previous ones
     * *****************************************************************************
     */
    public static int segae_vdp_start(int chip) {
        int temp;

        /*- VRAM -*/
        segae_vdp_vram[chip] = new UBytePtr(0x8000);
        /* 32kb (2 banks) */
        u8_segae_vdp_vrambank[chip] = 0;

        /*- CRAM -*/
        segae_vdp_cram[chip] = new UBytePtr(0x20);

        /*- VDP Registers -*/
        segae_vdp_regs[chip] = new UBytePtr(0x20);

        /*- Check Allocation was Successful -*/
        if (segae_vdp_vram[chip] == null || segae_vdp_cram[chip] == null || segae_vdp_regs[chip] == null) {
            return 1;
        }

        /*- Clear Memory -*/
        memset(segae_vdp_vram[chip], 0, 0x8000);
        memset(segae_vdp_cram[chip], 0, 0x20);
        memset(segae_vdp_regs[chip], 0, 0x20);

        /*- Set Up Some Default Values */
        u16_segae_vdp_accessaddr[chip] = 0;
        u8_segae_vdp_accessmode[chip] = 0;
        u8_segae_vdp_cmdpart[chip] = 0;
        u16_segae_vdp_command[chip] = 0;

        /*- Black the Palette -*/
        for (temp = 0; temp < 32; temp++) {
            palette_set_color(temp + 32 * chip, 0, 0, 0);
        }

        /* Save State Stuff (based on vidhrdw/taitoic.c) */
 /*TODO*///	{
        /*TODO*///		char buf[20];	/* we need different labels for every item of save data */
        /*TODO*///			sprintf(buf,"SEGASYSE-VDP-%01x",chip);	/* so we add chip # as a suffix */
        /*TODO*///			state_save_register_UINT8 ( buf, 0, "Video RAM",		segae_vdp_vram[chip], 0x8000);
        /*TODO*///			state_save_register_UINT8 ( buf, 0, "Colour RAM",		segae_vdp_cram[chip], 0x20);
        /*TODO*///			state_save_register_UINT8 ( buf, 0, "Registers",		segae_vdp_regs[chip], 0x20);
        /*TODO*///			state_save_register_UINT8 ( buf, 0, "Command Part",		&segae_vdp_cmdpart[chip], 1);
        /*TODO*///			state_save_register_UINT16( buf, 0, "Command Word",		&segae_vdp_command[chip], 1);
        /*TODO*///			state_save_register_UINT8 ( buf, 0, "Access Mode",		&segae_vdp_accessmode[chip], 1);
        /*TODO*///			state_save_register_UINT16( buf, 0, "Access Address",	&segae_vdp_accessaddr[chip], 1);
        /*TODO*///			state_save_register_UINT8 ( buf, 0, "VRAM Bank",		&segae_vdp_vrambank[chip], 1);
        /*TODO*///	}
        return 0;
    }

    public static void segae_vdp_stop(int chip) {
        /*- Free Allocated Memory -*/

        segae_vdp_vram[chip] = null;
        segae_vdp_cram[chip] = null;
        segae_vdp_regs[chip] = null;
    }

    /**
     * *****************************************************************************
     * Core VDP Functions
     * *****************************************************************************
     */
    /*-- Reads --*/
    /**
     * *************************************
     * segae_vdp_ctrl_r ( UINT8 chip ) ***************************************
     * reading the vdp control port will return the following
     *
     * bit: 7	- vert int pending 6	- line int pending 5	- sprite collision (non
     * 0 pixels) *not currently emulated (not needed by these games)* 4 - always
     * 0 3 - always 0 2 - always 0 1 - always 0 0 - always 0
     *
     * bits 5,6,7 are cleared after a read *************************************
     */
    public static int /*unsigned char*/ segae_vdp_ctrl_r(int chip) {
        int/*UINT8*/ temp;

        temp = 0;

        temp = (temp | (u8_vintpending << 7)) & 0xFF;
        temp = (temp | (u8_hintpending << 6)) & 0xFF;

        u8_hintpending = u8_vintpending = 0;

        return temp;
    }

    public static int /*unsigned char*/ segae_vdp_data_r(int chip) {
        int/*UINT8*/ temp;

        u8_segae_vdp_cmdpart[chip] = 0;

        temp = u8_segae_vdp_readbuffer[chip] & 0xFF;

        if (u8_segae_vdp_accessmode[chip] == 0x03) {
            /* CRAM Access */
 /* error CRAM can't be read!! */
        } else {
            /* VRAM */
            u8_segae_vdp_readbuffer[chip] = segae_vdp_vram[chip].read(u8_segae_vdp_vrambank[chip] * 0x4000 + u16_segae_vdp_accessaddr[chip]);
            u16_segae_vdp_accessaddr[chip] = (u16_segae_vdp_accessaddr[chip] + 1) & 0xFFFF;
            u16_segae_vdp_accessaddr[chip] = (u16_segae_vdp_accessaddr[chip] & 0x3fff) & 0xFFFF;
        }
        return temp;
    }

    /*-- Writes --*/
    public static void segae_vdp_ctrl_w(int chip, int u8_data) {
        if (u8_segae_vdp_cmdpart[chip] == 0) {
            u8_segae_vdp_cmdpart[chip] = 1;
            u16_segae_vdp_command[chip] = u8_data;
        } else {
            u8_segae_vdp_cmdpart[chip] = 0;
            u16_segae_vdp_command[chip] = (u16_segae_vdp_command[chip] | (u8_data << 8)) & 0xFFFF;
            segae_vdp_processcmd(chip, u16_segae_vdp_command[chip]);
        }
    }

    public static void segae_vdp_data_w(int chip, int u8_data) {
        u8_segae_vdp_cmdpart[chip] = 0;

        if (u8_segae_vdp_accessmode[chip] == 0x03) {
            /* CRAM Access */
            int/*UINT8*/ r, g, b, temp;

            temp = segae_vdp_cram[chip].read(u16_segae_vdp_accessaddr[chip]);

            segae_vdp_cram[chip].write(u16_segae_vdp_accessaddr[chip], u8_data);

            if (temp != u8_data) {
                r = (segae_vdp_cram[chip].read(u16_segae_vdp_accessaddr[chip]) & 0x03) << 6;
                g = (segae_vdp_cram[chip].read(u16_segae_vdp_accessaddr[chip]) & 0x0c) << 4;
                b = (segae_vdp_cram[chip].read(u16_segae_vdp_accessaddr[chip]) & 0x30) << 2;

                palette_set_color(u16_segae_vdp_accessaddr[chip] + 32 * chip, r, g, b);
            }

            u16_segae_vdp_accessaddr[chip] = (u16_segae_vdp_accessaddr[chip] + 1) & 0xFFFF;
            u16_segae_vdp_accessaddr[chip] = (u16_segae_vdp_accessaddr[chip] & 0x1f) & 0xFFFF;
        } else {
            /* VRAM Accesses */
            segae_vdp_vram[chip].write(u8_segae_vdp_vrambank[chip] * 0x4000 + u16_segae_vdp_accessaddr[chip], u8_data);
            u16_segae_vdp_accessaddr[chip] = (u16_segae_vdp_accessaddr[chip] + 1) & 0xFFFF;
            u16_segae_vdp_accessaddr[chip] = (u16_segae_vdp_accessaddr[chip] & 0x3fff) & 0xFFFF;
        }
    }

    /*-- Associated Functions --*/
    /**
     * *************************************
     * segae_vdp_processcmd ***************************************
     *
     * general command format
     *
     * M M A A A A A A A A A A A A A A M=Mode, A=Address
     *
     * the command will be one of 3 things according to the upper 4 bits
     *
     * 0 0 - - - - - - - - - - - - - - VRAM Acess Mode (Special Read)
     *
     * 0 1 - - - - - - - - - - - - - - VRAM Acesss Mode
     *
     * 1 0 0 0 - - - - - - - - - - - - VDP Register Set (current mode & address
     * _not_ changed)
     *
     * 1 0 x x - - - - - - - - - - - - VRAM Access Mode (0x1000 - 0x3FFF only, x
     * x is anything but 0 0)
     *
     * 1 1 - - - - - - - - - - - - - - CRAM Access Mode
     *
     **************************************
     */
    public static void segae_vdp_processcmd(int chip, int/*UINT16*/ u16_cmd) {
        if ((u16_cmd & 0xf000) == 0x8000) {
            /*  1 0 0 0 - - - - - - - - - - - -  VDP Register Set */
            segae_vdp_setregister(chip, u16_cmd);
        } else {
            /* Anything Else */
            u8_segae_vdp_accessmode[chip] = ((u16_cmd & 0xc000) >> 14) & 0xFF;
            u16_segae_vdp_accessaddr[chip] = (u16_cmd & 0x3fff) & 0xFFFF;

            if ((u8_segae_vdp_accessmode[chip] == 0x03) && (u16_segae_vdp_accessaddr[chip] > 0x1f)) {
                /* Check Address is valid for CRAM */
 /* Illegal, CRAM isn't this large! */
                u16_segae_vdp_accessaddr[chip] = (u16_segae_vdp_accessaddr[chip] & 0x1f) & 0xFFFF;
            }

            if (u8_segae_vdp_accessmode[chip] == 0x00) {
                /*  0 0 - - - - - - - - - - - - - -  VRAM Acess Mode (Special Read) */
                u8_segae_vdp_readbuffer[chip] = segae_vdp_vram[chip].read(u8_segae_vdp_vrambank[chip] * 0x4000 + u16_segae_vdp_accessaddr[chip]);
                u16_segae_vdp_accessaddr[chip] = (u16_segae_vdp_accessaddr[chip] + 1) & 0xFFFF;
                u16_segae_vdp_accessaddr[chip] = (u16_segae_vdp_accessaddr[chip] & 0x3fff) & 0xFFFF;
            }
        }
    }

    /**
     * *************************************
     * segae_vdp_setregister
     *
     * general command format
     *
     * 1 0 0 0 R R R R D D D D D D D D 1/0 = Fixed Values, R = Register # /
     * Address, D = Data
     *
     **************************************
     */
    public static void segae_vdp_setregister(int chip, int u16_cmd) {
        int/*UINT8*/ regnumber;
        int/*UINT8*/ regdata;

        regnumber = ((u16_cmd & 0x0f00) >> 8) & 0xFF;
        regdata = (u16_cmd & 0x00ff);

        if (regnumber < 11) {
            segae_vdp_regs[chip].write(regnumber, regdata);
        } else {
            /* Illegal, there aren't this many registers! */
        }
    }

    /**
     * *****************************************************************************
     * System E Drawing Capabilities Notes
     * *******************************************************************************
     * Display Consists of
     *
     * VDP0 Backdrop Color (?) VDP0 Tiles (Low) VDP0 Sprites VDP0 Tiles (High)
     * VDP1 Tiles (Low) VDP1 Sprites VDP1 Tiles (High)
     *
     * each vdp has its on vram, etc etc.
     *
     * the tilemaps are 256x224 in size, 256x192 of this is visible, the tiles
     * are 8x8 pixels, so 32x28 of 8x8 tiles make a 256x224 tilemap.
     *
     * the tiles are 4bpp (16 colours), video ram can hold upto 512 tile gfx
     *
     * tile references are 16 bits (3 bits unused, 1 bit priority, 1 bit
     * palette, 2 bits for flip, 9 bits for tile number)
     *
     * tilemaps can be scrolled horizontally, the top 16 lines of the display
     * can have horinzontal scrolling disabled
     *
     * tilemaps can be scrolled vertically, the right 64 lines of the display
     * can have the vertical scrolling disabled
     *
     * the leftmost 8 line of the display can be blanked
     *
     ******************************************************************************
     */
    public static void segae_drawscanline(int line) {

        UBytePtr dest;

        if (osd_skip_this_frame() != 0) {
            return;
        }

        dest = new UBytePtr(cache_bitmap, (16 + 256 + 16) * line);

        /* This should be cleared to bg colour, but which vdp determines that !, neither seems to be right, maybe its always the same col? */
        memset(dest, 0, 16 + 256 + 16);

        if ((segae_vdp_regs[0].read(1) & 0x40) != 0) {
            segae_drawtilesline(new UBytePtr(dest, 16), line, 0, 0);
            segae_drawspriteline(new UBytePtr(dest, 16), 0, line);
            segae_drawtilesline(new UBytePtr(dest, 16), line, 0, 1);
        }

        if ((segae_vdp_regs[1].read(1) & 0x40) != 0) {
            segae_drawtilesline(new UBytePtr(dest, 16), line, 1, 0);
            segae_drawspriteline(new UBytePtr(dest, 16), 1, line);
            segae_drawtilesline(new UBytePtr(dest, 16), line, 1, 1);
        }

        memset(dest, 16, 32 + 16, 8);
        /* Clear Leftmost column, there should be a register for this like on the SMS i imagine    */
 /* on the SMS this is bit 5 of register 0 (according to CMD's SMS docs) for system E this  */
 /* appears to be incorrect, most games need it blanked 99% of the time so we blank it      */

    }

    /*-- Drawing a line of tiles --*/
    public static void segae_drawtilesline(UBytePtr dest, int line, int chip, int pri) {
        /* todo: fix vscrolling (or is it something else causing the glitch on the hi-score screen of hangonjr, seems to be ..  */

        int/*UINT8*/ u8_hscroll;
        int/*UINT8*/ u8_vscroll;
        int/*UINT16*/ u16_tmbase;
        int/*UINT8*/ u8_tilesline, u8_tilesline2;
        int/*UINT8*/ u8_coloffset, u8_coloffset2;
        int/*UINT8*/ u8_loopcount;

        u8_hscroll = (256 - segae_vdp_regs[chip].read(8)) & 0xFF;
        u8_vscroll = segae_vdp_regs[chip].read(9);
        if (u8_vscroll > 224) {
            u8_vscroll = (u8_vscroll % 224) & 0xFF;
        }

        u16_tmbase = ((segae_vdp_regs[chip].read(2) & 0x0e) << 10) & 0xFFFF;
        u16_tmbase = (u16_tmbase + (u8_segae_vdp_vrambank[chip] * 0x4000)) & 0xFFFF;

        u8_tilesline = ((line + u8_vscroll) >> 3) & 0xFF;
        u8_tilesline2 = ((line + u8_vscroll) % 8) & 0xFF;

        u8_coloffset = (u8_hscroll >> 3) & 0xFF;
        u8_coloffset2 = (u8_hscroll % 8) & 0xFF;

        dest.dec(u8_coloffset2);

        for (u8_loopcount = 0; u8_loopcount < 33; u8_loopcount++) {

            int/*UINT16*/ vram_offset, vram_word;
            int/*UINT16*/ tile_no;
            int/*UINT8*/ palette, priority, flipx, flipy;

            vram_offset = (u16_tmbase + (2 * (32 * u8_tilesline + ((u8_coloffset + u8_loopcount) & 0x1f)))) & 0xFFFF;
            vram_word = (segae_vdp_vram[chip].read(vram_offset) | (segae_vdp_vram[chip].read(vram_offset + 1) << 8)) & 0xFFFF;

            tile_no = (vram_word & 0x01ff);
            flipx = ((vram_word & 0x0200) >> 9) & 0xFF;
            flipy = ((vram_word & 0x0400) >> 10) & 0xFF;
            palette = ((vram_word & 0x0800) >> 11) & 0xFF;
            priority = ((vram_word & 0x1000) >> 12) & 0xFF;

            if (priority == pri) {
                if (chip == 0) {
                    segae_draw8pix_solid16(dest, chip, tile_no, u8_tilesline2, flipx, palette);
                } else {
                    segae_draw8pix(dest, chip, tile_no, u8_tilesline2, flipx, palette);
                }
            }
            dest.inc(8);
        }
    }

    public static void segae_drawspriteline(UBytePtr dest, int chip, int/*UINT8*/ line) {
        /* todo: figure out what riddle of pythagoras hates about this */

        int nosprites;
        int loopcount;

        int/*UINT16*/ u16_spritebase;

        nosprites = 0;

        u16_spritebase = ((segae_vdp_regs[chip].read(5) & 0x7e) << 7) & 0xFFFF;
        u16_spritebase = (u16_spritebase + (u8_segae_vdp_vrambank[chip] * 0x4000)) & 0xFFFF;

        /*- find out how many sprites there are -*/
        for (loopcount = 0; loopcount < 64; loopcount++) {
            int/*UINT8*/ ypos;

            ypos = segae_vdp_vram[chip].read(u16_spritebase + loopcount);

            if (ypos == 208) {
                nosprites = loopcount;
                break;
            }
        }

        if (strcmp(Machine.gamedrv.name, "ridleofp") == 0) {
            nosprites = 63;
            /* why, there must be a bug elsewhere i guess ?! */
        }

        /*- draw sprites IN REVERSE ORDER -*/
        for (loopcount = nosprites; loopcount >= 0; loopcount--) {
            int ypos;
            int/*UINT8*/ sheight;

            ypos = segae_vdp_vram[chip].read(u16_spritebase + loopcount) + 1;

            if ((segae_vdp_regs[chip].read(1) & 0x02) != 0) {
                sheight = 16;
            } else {
                sheight = 8;
            }

            if ((line >= ypos) && (line < ypos + sheight)) {
                int xpos;
                int/*UINT8*/ sprnum;
                int/*UINT8*/ spline;

                spline = (line - ypos) & 0xFF;

                xpos = segae_vdp_vram[chip].read(u16_spritebase + 0x80 + (2 * loopcount));
                sprnum = segae_vdp_vram[chip].read(u16_spritebase + 0x81 + (2 * loopcount)) & 0xFF;

                if ((segae_vdp_regs[chip].read(6) & 0x04) != 0) {
                    sprnum = (sprnum + 0x100) & 0xFF;
                }

                segae_draw8pixsprite(new UBytePtr(dest, xpos), chip, sprnum, spline);

            }
        }
    }

    static void segae_draw8pix_solid16(UBytePtr dest, int chip, /*UINT16*/ int tile, int/*UINT8*/ line, int/*UINT8*/ flipx, int/*UINT8*/ col) {

        int/*UINT32*/ pix8 = segae_vdp_vram[chip].read32((32) * tile + (4) * line + (0x4000) * u8_segae_vdp_vrambank[chip]);
        int/*UINT8*/ pix, coladd;

        if (pix8 == 0 && col == 0) {
            return;
            /*note only the colour 0 of each vdp is transparent NOT colour 16???, fixes sky in HangonJr */
        }

        coladd = 16 * col;

        if (flipx != 0) {
            pix = ((pix8 >> 0) & 0x01) | ((pix8 >> 7) & 0x02) | ((pix8 >> 14) & 0x04) | ((pix8 >> 21) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(0, pix + 32 * chip);
            }
            pix = ((pix8 >> 1) & 0x01) | ((pix8 >> 8) & 0x02) | ((pix8 >> 15) & 0x04) | ((pix8 >> 22) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(1, pix + 32 * chip);
            }
            pix = ((pix8 >> 2) & 0x01) | ((pix8 >> 9) & 0x02) | ((pix8 >> 16) & 0x04) | ((pix8 >> 23) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(2, pix + 32 * chip);
            }
            pix = ((pix8 >> 3) & 0x01) | ((pix8 >> 10) & 0x02) | ((pix8 >> 17) & 0x04) | ((pix8 >> 24) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(3, pix + 32 * chip);
            }
            pix = ((pix8 >> 4) & 0x01) | ((pix8 >> 11) & 0x02) | ((pix8 >> 18) & 0x04) | ((pix8 >> 25) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(4, pix + 32 * chip);
            }
            pix = ((pix8 >> 5) & 0x01) | ((pix8 >> 12) & 0x02) | ((pix8 >> 19) & 0x04) | ((pix8 >> 26) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(5, pix + 32 * chip);
            }
            pix = ((pix8 >> 6) & 0x01) | ((pix8 >> 13) & 0x02) | ((pix8 >> 20) & 0x04) | ((pix8 >> 27) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(6, pix + 32 * chip);
            }
            pix = ((pix8 >> 7) & 0x01) | ((pix8 >> 14) & 0x02) | ((pix8 >> 21) & 0x04) | ((pix8 >> 28) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(7, pix + 32 * chip);
            }
        } else {
            pix = ((pix8 >> 7) & 0x01) | ((pix8 >> 14) & 0x02) | ((pix8 >> 21) & 0x04) | ((pix8 >> 28) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(0, pix + 32 * chip);
            }
            pix = ((pix8 >> 6) & 0x01) | ((pix8 >> 13) & 0x02) | ((pix8 >> 20) & 0x04) | ((pix8 >> 27) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(1, pix + 32 * chip);
            }
            pix = ((pix8 >> 5) & 0x01) | ((pix8 >> 12) & 0x02) | ((pix8 >> 19) & 0x04) | ((pix8 >> 26) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(2, pix + 32 * chip);
            }
            pix = ((pix8 >> 4) & 0x01) | ((pix8 >> 11) & 0x02) | ((pix8 >> 18) & 0x04) | ((pix8 >> 25) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(3, pix + 32 * chip);
            }
            pix = ((pix8 >> 3) & 0x01) | ((pix8 >> 10) & 0x02) | ((pix8 >> 17) & 0x04) | ((pix8 >> 24) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(4, pix + 32 * chip);
            }
            pix = ((pix8 >> 2) & 0x01) | ((pix8 >> 9) & 0x02) | ((pix8 >> 16) & 0x04) | ((pix8 >> 23) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(5, pix + 32 * chip);
            }
            pix = ((pix8 >> 1) & 0x01) | ((pix8 >> 8) & 0x02) | ((pix8 >> 15) & 0x04) | ((pix8 >> 22) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(6, pix + 32 * chip);
            }
            pix = ((pix8 >> 0) & 0x01) | ((pix8 >> 7) & 0x02) | ((pix8 >> 14) & 0x04) | ((pix8 >> 21) & 0x08);
            pix = (pix + coladd) & 0xFF;;
            if (pix != 0) {
                dest.write(7, pix + 32 * chip);
            }
        }
    }

    static void segae_draw8pix(UBytePtr dest, int chip, int/*UINT16*/ tile, int/*UINT8*/ line, int/*UINT8*/ flipx, int/*UINT8*/ col) {

        int/*UINT32*/ pix8 = segae_vdp_vram[chip].read32((32) * tile + (4) * line + (0x4000) * u8_segae_vdp_vrambank[chip]);
        int/*UINT8*/ pix, coladd;

        if (pix8 == 0) {
            return;
        }

        coladd = (16 * col + 32 * chip) & 0xFF;

        if (flipx != 0) {
            pix = ((pix8 >> 0) & 0x01) | ((pix8 >> 7) & 0x02) | ((pix8 >> 14) & 0x04) | ((pix8 >> 21) & 0x08);
            if (pix != 0) {
                dest.write(0, pix + coladd);
            }
            pix = ((pix8 >> 1) & 0x01) | ((pix8 >> 8) & 0x02) | ((pix8 >> 15) & 0x04) | ((pix8 >> 22) & 0x08);
            if (pix != 0) {
                dest.write(1, pix + coladd);
            }
            pix = ((pix8 >> 2) & 0x01) | ((pix8 >> 9) & 0x02) | ((pix8 >> 16) & 0x04) | ((pix8 >> 23) & 0x08);
            if (pix != 0) {
                dest.write(2, pix + coladd);
            }
            pix = ((pix8 >> 3) & 0x01) | ((pix8 >> 10) & 0x02) | ((pix8 >> 17) & 0x04) | ((pix8 >> 24) & 0x08);
            if (pix != 0) {
                dest.write(3, pix + coladd);
            }
            pix = ((pix8 >> 4) & 0x01) | ((pix8 >> 11) & 0x02) | ((pix8 >> 18) & 0x04) | ((pix8 >> 25) & 0x08);
            if (pix != 0) {
                dest.write(4, pix + coladd);
            }
            pix = ((pix8 >> 5) & 0x01) | ((pix8 >> 12) & 0x02) | ((pix8 >> 19) & 0x04) | ((pix8 >> 26) & 0x08);
            if (pix != 0) {
                dest.write(5, pix + coladd);
            }
            pix = ((pix8 >> 6) & 0x01) | ((pix8 >> 13) & 0x02) | ((pix8 >> 20) & 0x04) | ((pix8 >> 27) & 0x08);
            if (pix != 0) {
                dest.write(6, pix + coladd);
            }
            pix = ((pix8 >> 7) & 0x01) | ((pix8 >> 14) & 0x02) | ((pix8 >> 21) & 0x04) | ((pix8 >> 28) & 0x08);
            if (pix != 0) {
                dest.write(7, pix + coladd);
            }
        } else {
            pix = ((pix8 >> 7) & 0x01) | ((pix8 >> 14) & 0x02) | ((pix8 >> 21) & 0x04) | ((pix8 >> 28) & 0x08);
            if (pix != 0) {
                dest.write(0, pix + coladd);
            }
            pix = ((pix8 >> 6) & 0x01) | ((pix8 >> 13) & 0x02) | ((pix8 >> 20) & 0x04) | ((pix8 >> 27) & 0x08);
            if (pix != 0) {
                dest.write(1, pix + coladd);
            }
            pix = ((pix8 >> 5) & 0x01) | ((pix8 >> 12) & 0x02) | ((pix8 >> 19) & 0x04) | ((pix8 >> 26) & 0x08);
            if (pix != 0) {
                dest.write(2, pix + coladd);
            }
            pix = ((pix8 >> 4) & 0x01) | ((pix8 >> 11) & 0x02) | ((pix8 >> 18) & 0x04) | ((pix8 >> 25) & 0x08);
            if (pix != 0) {
                dest.write(3, pix + coladd);
            }
            pix = ((pix8 >> 3) & 0x01) | ((pix8 >> 10) & 0x02) | ((pix8 >> 17) & 0x04) | ((pix8 >> 24) & 0x08);
            if (pix != 0) {
                dest.write(4, pix + coladd);
            }
            pix = ((pix8 >> 2) & 0x01) | ((pix8 >> 9) & 0x02) | ((pix8 >> 16) & 0x04) | ((pix8 >> 23) & 0x08);
            if (pix != 0) {
                dest.write(5, pix + coladd);
            }
            pix = ((pix8 >> 1) & 0x01) | ((pix8 >> 8) & 0x02) | ((pix8 >> 15) & 0x04) | ((pix8 >> 22) & 0x08);
            if (pix != 0) {
                dest.write(6, pix + coladd);
            }
            pix = ((pix8 >> 0) & 0x01) | ((pix8 >> 7) & 0x02) | ((pix8 >> 14) & 0x04) | ((pix8 >> 21) & 0x08);
            if (pix != 0) {
                dest.write(7, pix + coladd);
            }
        }
    }

    static void segae_draw8pixsprite(UBytePtr dest, int/*UINT8*/ chip, int/*UINT16*/ tile, int/*UINT8*/ line) {

        int/*UINT32*/ pix8 = segae_vdp_vram[chip].read32((32) * tile + (4) * line + (0x4000) * u8_segae_vdp_vrambank[chip]);
        int/*UINT8*/ pix;

        if (pix8 == 0) {
            return;
            /*note only the colour 0 of each vdp is transparent NOT colour 16, fixes sky in HangonJr */
        }

        pix = ((pix8 >> 7) & 0x01) | ((pix8 >> 14) & 0x02) | ((pix8 >> 21) & 0x04) | ((pix8 >> 28) & 0x08);
        if (pix != 0) {
            dest.write(0, pix + 16 + 32 * chip);
        }
        pix = ((pix8 >> 6) & 0x01) | ((pix8 >> 13) & 0x02) | ((pix8 >> 20) & 0x04) | ((pix8 >> 27) & 0x08);
        if (pix != 0) {
            dest.write(1, pix + 16 + 32 * chip);
        }
        pix = ((pix8 >> 5) & 0x01) | ((pix8 >> 12) & 0x02) | ((pix8 >> 19) & 0x04) | ((pix8 >> 26) & 0x08);
        if (pix != 0) {
            dest.write(2, pix + 16 + 32 * chip);
        }
        pix = ((pix8 >> 4) & 0x01) | ((pix8 >> 11) & 0x02) | ((pix8 >> 18) & 0x04) | ((pix8 >> 25) & 0x08);
        if (pix != 0) {
            dest.write(3, pix + 16 + 32 * chip);
        }
        pix = ((pix8 >> 3) & 0x01) | ((pix8 >> 10) & 0x02) | ((pix8 >> 17) & 0x04) | ((pix8 >> 24) & 0x08);
        if (pix != 0) {
            dest.write(4, pix + 16 + 32 * chip);
        }
        pix = ((pix8 >> 2) & 0x01) | ((pix8 >> 9) & 0x02) | ((pix8 >> 16) & 0x04) | ((pix8 >> 23) & 0x08);
        if (pix != 0) {
            dest.write(5, pix + 16 + 32 * chip);
        }
        pix = ((pix8 >> 1) & 0x01) | ((pix8 >> 8) & 0x02) | ((pix8 >> 15) & 0x04) | ((pix8 >> 22) & 0x08);
        if (pix != 0) {
            dest.write(6, pix + 16 + 32 * chip);
        }
        pix = ((pix8 >> 0) & 0x01) | ((pix8 >> 7) & 0x02) | ((pix8 >> 14) & 0x04) | ((pix8 >> 21) & 0x08);
        if (pix != 0) {
            dest.write(7, pix + 16 + 32 * chip);
        }

    }
}
