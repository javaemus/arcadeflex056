/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.sndhrdw;

import static arcadeflex056.fucPtr.*;

import static mame056.cpuexec.*;
import static mame056.cpuintrfH.*;

import static mame056.machine._7474.*;
import static mame056.machine._7474H.*;

import static mame056.sound.mixer.*;
import static mame056.sound.streams.*;

public class scramble {

    /* The timer clock in Scramble which feeds the upper 4 bits of    		*/
 /* AY-3-8910 port A is based on the same clock        					*/
 /* feeding the sound CPU Z80.  It is a divide by      					*/
 /* 5120, formed by a standard divide by 512,        					*/
 /* followed by a divide by 10 using a 4 bit           					*/
 /* bi-quinary count sequence. (See LS90 data sheet    					*/
 /* for an example).                                   					*/
 /*																		*/
 /* Bit 4 comes from the output of the divide by 1024  					*/
 /*       0, 1, 0, 1, 0, 1, 0, 1, 0, 1									*/
 /* Bit 5 comes from the QC output of the LS90 producing a sequence of	*/
 /* 		 0, 0, 1, 1, 0, 0, 1, 1, 1, 0									*/
 /* Bit 6 comes from the QD output of the LS90 producing a sequence of	*/
 /*		 0, 0, 0, 0, 1, 0, 0, 0, 0, 1									*/
 /* Bit 7 comes from the QA output of the LS90 producing a sequence of	*/
 /*		 0, 0, 0, 0, 0, 1, 1, 1, 1, 1			 						*/
    static int scramble_timer[]
            = {
                0x00, 0x10, 0x20, 0x30, 0x40, 0x90, 0xa0, 0xb0, 0xa0, 0xd0
            };

    /* need to protect from totalcycles overflow */
    static int last_totalcycles = 0;

    /* number of Z80 clock cycles to count */
    static int clock = 0;

    public static ReadHandlerPtr scramble_portB_r = new ReadHandlerPtr() {
        public int handler(int offset) {

            int current_totalcycles;

            current_totalcycles = cpu_gettotalcycles();
            clock = (clock + (current_totalcycles - last_totalcycles)) % 5120;

            last_totalcycles = current_totalcycles;

            return scramble_timer[clock / 512];
        }
    };

    /* The timer clock in Frogger which feeds the upper 4 bits of    		*/
 /* AY-3-8910 port A is based on the same clock        					*/
 /* feeding the sound CPU Z80.  It is a divide by      					*/
 /* 5120, formed by a standard divide by 512,        					*/
 /* followed by a divide by 10 using a 4 bit           					*/
 /* bi-quinary count sequence. (See LS90 data sheet    					*/
 /* for an example).                                   					*/
 /*																		*/
 /* Bit 4 comes from the output of the divide by 1024  					*/
 /*       0, 1, 0, 1, 0, 1, 0, 1, 0, 1									*/
 /* Bit 3 comes from the QC output of the LS90 producing a sequence of	*/
 /* 		 0, 0, 1, 1, 0, 0, 1, 1, 1, 0									*/
 /* Bit 6 comes from the QD output of the LS90 producing a sequence of	*/
 /*		 0, 0, 0, 0, 1, 0, 0, 0, 0, 1									*/
 /* Bit 7 comes from the QA output of the LS90 producing a sequence of	*/
 /*		 0, 0, 0, 0, 0, 1, 1, 1, 1, 1			 						*/
    static int frogger_timer[]
            = {
                0x00, 0x10, 0x08, 0x18, 0x40, 0x90, 0x88, 0x98, 0x88, 0xd0
            };

    public static ReadHandlerPtr frogger_portB_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            int current_totalcycles;

            current_totalcycles = cpu_gettotalcycles();
            clock = (clock + (current_totalcycles - last_totalcycles)) % 5120;

            last_totalcycles = current_totalcycles;

            return frogger_timer[clock / 512];
        }
    };

    public static WriteHandlerPtr scramble_sh_irqtrigger_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* the complement of bit 3 is connected to the flip-flop's clock */
            TTL7474_set_inputs(0, -1, -1, ~data & 0x08, -1);

            /* bit 4 is sound disable */
            mixer_sound_enable_global_w(~data & 0x10);
        }
    };

    public static WriteHandlerPtr froggrmc_sh_irqtrigger_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* the complement of bit 0 is connected to the flip-flop's clock */
            TTL7474_set_inputs(0, -1, -1, ~data & 0x01, -1);
        }
    };

    public static irqcallbacksPtr scramble_sh_irq_callback = new irqcallbacksPtr() {
        public int handler(int irqline) {
            /* interrupt acknowledge clears the flip-flop --
		   we need to pulse the CLR line because MAME's core never clears this
		   line, only asserts it */
            TTL7474_set_inputs(0, 1, -1, -1, -1);
            TTL7474_set_inputs(0, 0, -1, -1, -1);

            return 0xff;
        }
    };

    public static irqcallbacksPtr scramble_sh_7474_callback = new irqcallbacksPtr() {
        public int handler(int irqline) {
            /* the Q bar is connected to the Z80's INT line.  But since INT is complemented, */
 /* we need to complement Q bar */
            cpu_set_irq_line(1, 0, (TTL7474_output_comp_r(0) == 0) ? ASSERT_LINE : CLEAR_LINE);

            return 1;
        }
    };

    public static WriteHandlerPtr hotshock_sh_irqtrigger_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_set_irq_line(1, 0, PULSE_LINE);
        }
    };

    static void filter_w(int chip, int channel, int data) {
        int C;

        C = 0;
        if ((data & 1) != 0) {
            C += 220000;
            /* 220000pF = 0.220uF */
        }
        if ((data & 2) != 0) {
            C += 47000;
            /*  47000pF = 0.047uF */
        }
        set_RC_filter(3 * chip + channel, 1000, 5100, 0, C);
    }

    public static WriteHandlerPtr scramble_filter_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            filter_w(1, 0, (offset >> 0) & 3);
            filter_w(1, 1, (offset >> 2) & 3);
            filter_w(1, 2, (offset >> 4) & 3);
            filter_w(0, 0, (offset >> 6) & 3);
            filter_w(0, 1, (offset >> 8) & 3);
            filter_w(0, 2, (offset >> 10) & 3);
        }
    };

    public static WriteHandlerPtr frogger_filter_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            filter_w(0, 0, (offset >> 6) & 3);
            filter_w(0, 1, (offset >> 8) & 3);
            filter_w(0, 2, (offset >> 10) & 3);
        }
    };

    public static TTL7474_interface scramble_sh_7474_intf = new TTL7474_interface(scramble_sh_7474_callback);

    public static void scramble_sh_init() {
        cpu_set_irq_callback(1, scramble_sh_irq_callback);

        TTL7474_config(0, scramble_sh_7474_intf);

        /* PR is always 0, D is always 1 */
        TTL7474_set_inputs(0, 0, 0, -1, 1);
    }
}
