/**
 * ported to v056
 * ported to v0.37b7
 *
 */
/**
 * Changelog
 * ---------
 * 26/05/2019 - rewrote docastle machine to 0.56 (shadow)
 */
package WIP.mame056.machine;

import static arcadeflex056.fucPtr.*;
import static arcadeflex036.osdepend.*;

import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;

public class docastle {

    static /*unsigned*/ char[] u8_buffer0 = new char[9];
    static /*unsigned*/ char[] u8_buffer1 = new char[9];

    public static ReadHandlerPtr docastle_shared0_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            if (offset == 8) {
                logerror("CPU #0 shared0r  clock = %d\n", cpu_gettotalcycles());
            }

            /* this shouldn't be done, however it's the only way I've found */
 /* to make dip switches work in Do Run Run. */
            if (offset == 8) {
                cpu_cause_interrupt(1, Z80_NMI_INT);
                cpu_spinuntil_trigger(500);
            }

            return u8_buffer0[offset] & 0xFF;
        }
    };

    public static ReadHandlerPtr docastle_shared1_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            if (offset == 8) {
                logerror("CPU #1 shared1r  clock = %d\n", cpu_gettotalcycles());
            }
            return u8_buffer1[offset] & 0xFF;
        }
    };

    public static WriteHandlerPtr docastle_shared0_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            if (offset == 8) {
                logerror("CPU #1 shared0w %02x %02x %02x %02x %02x %02x %02x %02x %02x clock = %d\n",
                        u8_buffer0[0], u8_buffer0[1], u8_buffer0[2], u8_buffer0[3], u8_buffer0[4], u8_buffer0[5], u8_buffer0[6], u8_buffer0[7], data, cpu_gettotalcycles());
            }

            u8_buffer0[offset] = (char) (data & 0xFF);

            if (offset == 8) /* awake the master CPU */ {
                cpu_trigger.handler(500);
            }
        }
    };

    public static WriteHandlerPtr docastle_shared1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            u8_buffer1[offset] = (char) (data & 0xFF);

            if (offset == 8) {
                logerror("CPU #0 shared1w %02x %02x %02x %02x %02x %02x %02x %02x %02x clock = %d\n",
                        u8_buffer1[0], u8_buffer1[1], u8_buffer1[2], u8_buffer1[3], u8_buffer1[4], u8_buffer1[5], u8_buffer1[6], u8_buffer1[7], data, cpu_gettotalcycles());

                /* freeze execution of the master CPU until the slave has used the shared memory */
                cpu_spinuntil_trigger(500);
            }
        }
    };

    public static WriteHandlerPtr docastle_nmitrigger_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_cause_interrupt(1, Z80_NMI_INT);
        }
    };
}
