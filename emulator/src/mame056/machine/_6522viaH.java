/**********************************************************************

	Rockwell 6522 VIA interface and emulation

	This function emulates all the functionality of up to 8 6522
	versatile interface adapters.

    This is based on the M6821 emulation in MAME.

	Written by Mathis Rosenhauer

**********************************************************************/

package mame056.machine;

import static arcadeflex056.fucPtr.*;
import static mame056.machine._6812piaH.irqfuncPtr;
import static mame056.timer.*;

public class _6522viaH {
    
    public static int  MAX_VIA      = 8;

    public static final int 	VIA_PB	    = 0;
    public static final int 	VIA_PA	    = 1;
    public static final int 	VIA_DDRB    = 2;
    public static final int 	VIA_DDRA    = 3;
    public static final int 	VIA_T1CL    = 4;
    public static final int 	VIA_T1CH    = 5;
    public static final int 	VIA_T1LL    = 6;
    public static final int 	VIA_T1LH    = 7;
    public static final int 	VIA_T2CL    = 8;
    public static final int 	VIA_T2CH    = 9;
    public static final int 	VIA_SR      = 10;
    public static final int 	VIA_ACR     = 11;
    public static final int 	VIA_PCR     = 12;
    public static final int 	VIA_IFR     = 13;
    public static final int 	VIA_IER     = 14;
    public static final int 	VIA_PANH    = 15;

    public static class via6522_interface
    {
            public ReadHandlerPtr in_a_func;
            public ReadHandlerPtr in_b_func;
            public ReadHandlerPtr in_ca1_func;
            public ReadHandlerPtr in_cb1_func;
            public ReadHandlerPtr in_ca2_func;
            public ReadHandlerPtr in_cb2_func;
            public WriteHandlerPtr out_a_func;
            public WriteHandlerPtr out_b_func;
            public WriteHandlerPtr out_ca2_func;
            public WriteHandlerPtr out_cb2_func;
            public irqfuncPtr irq_func;

        /* kludges for the Vectrex */
            /*TODO*///void (*out_shift_func)(int val);
            public timer_callback t2_callback;
        /* kludges for the Mac Plus (and 128k, 512k, 512ke) keyboard interface */
            /*TODO*///void (*out_shift_func2)(int val);	/* called when some data is shifted out in EXT sync mode */
            /*TODO*///void (*si_ready_func)(void);		/* called when the shift-in is enabled (EXT sync mode) */
            
            public via6522_interface(ReadHandlerPtr in_a_func,
                ReadHandlerPtr in_b_func,
                ReadHandlerPtr in_ca1_func,
                ReadHandlerPtr in_cb1_func,
                ReadHandlerPtr in_ca2_func,
                ReadHandlerPtr in_cb2_func,
                WriteHandlerPtr out_a_func,
                WriteHandlerPtr out_b_func,
                WriteHandlerPtr out_ca2_func,
                WriteHandlerPtr out_cb2_func,
                irqfuncPtr irq_func)
            {
                this.in_b_func = in_a_func;
                this.in_ca1_func = in_ca1_func;
                this.in_cb1_func = in_cb1_func;
                this.in_ca2_func = in_ca2_func;
                this.in_cb2_func = in_cb2_func;
                this.out_a_func = out_a_func;
                this.out_b_func = out_b_func;
                this.out_ca2_func = out_ca2_func;
                this.out_cb2_func = out_cb2_func;
                this.irq_func = irq_func;
            }
    };

}
