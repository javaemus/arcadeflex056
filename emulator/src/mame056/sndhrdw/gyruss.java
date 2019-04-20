/**
 * ported to v0.56
 * ported to v0.37b7
 */
/**
 * Changelog
 * ---------
 * 20/04/2019 - ported file to 0.56 (shadow)
 */
package mame056.sndhrdw;

import static arcadeflex056.fucPtr.*;

import static mame056.cpuexec.*;

import static mame056.cpu.i8039.i8039.*;

import static mame056.sound.streams.*;

public class gyruss {

    static int gyruss_timer[]
            = {
                0x00, 0x01, 0x02, 0x03, 0x04, 0x09, 0x0a, 0x0b, 0x0a, 0x0d
            };
    /* need to protect from totalcycles overflow */
    static int last_totalcycles = 0;

    /* number of Z80 clock cycles to count */
    static int clock;
    public static ReadHandlerPtr gyruss_portA_r = new ReadHandlerPtr() {
        public int handler(int offset) {

            int current_totalcycles;

            current_totalcycles = cpu_gettotalcycles();
            clock = (clock + (current_totalcycles - last_totalcycles)) % 10240;

            last_totalcycles = current_totalcycles;

            return gyruss_timer[clock / 1024];
        }
    };

    static void filter_w(int chip, int data) {
        int i;

        for (i = 0; i < 3; i++) {
            int C;

            C = 0;
            if ((data & 1) != 0) {
                C += 47000;/* 47000pF = 0.047uF */
            }
            if ((data & 2) != 0) {
                C += 220000;/* 220000pF = 0.22uF */
            }
            data >>= 2;
            set_RC_filter(3 * chip + i, 1000, 2200, 200, C);
        }
    }

    public static WriteHandlerPtr gyruss_filter0_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            filter_w(0, data);
        }
    };

    public static WriteHandlerPtr gyruss_filter1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            filter_w(1, data);
        }
    };

    public static WriteHandlerPtr gyruss_sh_irqtrigger_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* writing to this register triggers IRQ on the sound CPU */
            cpu_cause_interrupt(2, 0xff);
        }
    };

    public static WriteHandlerPtr gyruss_i8039_irq_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_cause_interrupt(3, I8039_EXT_INT);
        }
    };
}
