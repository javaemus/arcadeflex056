/****************************************************************************
 *																			*
 *	Function prototypes and constants used by the TMS34061 emulator			*
 *																			*
 *  Created by Zsolt Vasvari on 5/26/1998.	                                *
 *	Updated by Aaron Giles on 11/21/2000.									*
 *																			*
 ****************************************************************************/

package WIP.mame056.vidhrdw;

import static mame056.machine._6812piaH.irqfuncPtr;
import static common.ptr.*;

public class tms34061H {
    /* register constants */
    public static final int TMS34061_HORENDSYNC   = 0;
    public static final int TMS34061_HORENDBLNK   = 1;
    public static final int TMS34061_HORSTARTBLNK = 2;
    public static final int TMS34061_HORTOTAL     = 3;
    public static final int TMS34061_VERENDSYNC   = 4;
    public static final int TMS34061_VERENDBLNK   = 5;
    public static final int TMS34061_VERSTARTBLNK = 6;
    public static final int TMS34061_VERTOTAL     = 7;
    public static final int TMS34061_DISPUPDATE   = 8;
    public static final int TMS34061_DISPSTART    = 9;
    public static final int TMS34061_VERINT       = 10;
    public static final int TMS34061_CONTROL1     = 11;
    public static final int TMS34061_CONTROL2     = 12;
    public static final int TMS34061_STATUS       = 13;
    public static final int TMS34061_XYOFFSET     = 14;
    public static final int TMS34061_XYADDRESS    = 15;
    public static final int TMS34061_DISPADDRESS  = 16;
    public static final int TMS34061_VERCOUNTER   = 17;
    public static final int TMS34061_REGCOUNT     = 18;
    

    /* interface structure */
    public static class tms34061_interface
    {
            public int			rowshift;					/* VRAM address is (row << rowshift) | col */
            public int			vramsize;					/* size of video RAM */
            public int			dirtychunk;					/* size of dirty chunks (must be power of 2) */
            public irqfuncPtr		interrupt;	/* interrupt gen callback */

        tms34061_interface(int rowshift, int vramsize, int dirtychunk, irqfuncPtr generate_interrupt) {
            this.rowshift = rowshift;
            this.vramsize = vramsize;
            this.dirtychunk = dirtychunk;
            this.interrupt = generate_interrupt;
        }
    };


    /* display state structure */
    public static class tms34061_display
    {
            public int			blanked;					/* true if blanked */
            public UBytePtr             vram = new UBytePtr();						/* base of VRAM */
            public UBytePtr             latchram=new UBytePtr();					/* base of latch RAM */
            public UBytePtr             dirty=new UBytePtr();						/* pointer to array of dirty rows */
            public int[]                regs;						/* pointer to array of registers */
            public int			dispstart;					/* display start */
    };
}
