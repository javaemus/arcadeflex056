/**
 * ported to v0.56
 */
package WIP.mame056.sound;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstdio.*;
import static common.ptr.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static WIP.mame056.sound.hc55516H.*;
import static mame056.mame.Machine;
import static mame056.sound.streams.*;

public class hc55516 extends snd_interface {

    public static final double INTEGRATOR_LEAK_TC = 0.001;
    public static final double FILTER_DECAY_TC = 0.004;
    public static final double FILTER_CHARGE_TC = 0.004;
    public static final double FILTER_MIN = 0.0416;
    public static final double FILTER_MAX = 1.0954;
    public static final double SAMPLE_GAIN = 10000.0;

    public static class hc55516_data {

        int/*INT8*/ channel;
        int/*UINT8*/ last_clock;
        int/*UINT8*/ databit;
        int/*UINT8*/ shiftreg;

        short curr_value;
        short next_value;

        int/*UINT32*/ update_count;

        double filter;
        double integrator;
    }

    static hc55516_data[] hc55516 = new hc55516_data[MAX_HC55516];
    static double charge, decay, leak;

    public hc55516() {
        sound_num = SOUND_HC55516;
        name = "HC55516";
    }

    @Override
    public int chips_num(MachineSound msound) {
        return ((hc55516_interface) msound.sound_interface).num;
    }

    @Override
    public int chips_clock(MachineSound msound) {
        return 0;//NO FUNCTIONAL CODE IS NECCESARY
    }

    public static StreamInitPtr hc55516_update = new StreamInitPtr() {
        public void handler(int num, ShortPtr buffer, int length) {
            hc55516_data chip = hc55516[num];
            int data, slope;
            int i;

            /* zero-length? bail */
            if (length == 0) {
                return;
            }

            /* track how many samples we've updated without a clock */
            chip.update_count += length;
            if (chip.update_count > Machine.sample_rate / 32) {
                chip.update_count = Machine.sample_rate;
                chip.next_value = 0;
            }

            /* compute the interpolation slope */
            data = chip.curr_value;
            slope = ((int) chip.next_value - data) / length;
            chip.curr_value = chip.next_value;

            /* reset the sample count */
            for (i = 0; i < length; i++, data += slope) {
                buffer.writeinc((short) data);
            }
        }
    };

    @Override
    public int start(MachineSound msound) {
        hc55516_interface intf = ((hc55516_interface) msound.sound_interface);
        int i;

        /* compute the fixed charge, decay, and leak time constants */
        charge = Math.pow(Math.exp(-1), 1.0 / (FILTER_CHARGE_TC * 16000.0));
        decay = Math.pow(Math.exp(-1), 1.0 / (FILTER_DECAY_TC * 16000.0));
        leak = Math.pow(Math.exp(-1), 1.0 / (INTEGRATOR_LEAK_TC * 16000.0));

        /* loop over HC55516 chips */
        for (i = 0; i < intf.num; i++) {
            //hc55516_data chip = hc55516[i];
            String name = "";

            /* reset the channel */
            hc55516[i] = new hc55516_data();//memset(chip, 0, sizeof(*chip));

            /* create the stream */
            name = sprintf("HC55516 #%d", i);
            hc55516[i].channel = stream_init(name, intf.volume[i] & 0xff, Machine.sample_rate, i, hc55516_update);

            /* bail on fail */
            if (hc55516[i].channel == -1) {
                return 1;
            }
        }

        /* success */
        return 0;
    }

    @Override
    public void stop() {
        //NO FUNCTIONAL CODE IS NECCESARY
    }

    @Override
    public void update() {
        //NO FUNCTIONAL CODE IS NECCESARY
    }

    @Override
    public void reset() {
        //NO FUNCTIONAL CODE IS NECCESARY
    }

    public static void hc55516_clock_w(int num, int state) {
        hc55516_data chip = hc55516[num];
        int clock = state & 1, diffclock;

        /* update the clock */
        diffclock = clock ^ chip.last_clock;
        chip.last_clock = clock & 0xFF;

        /* speech clock changing (active on rising edge) */
        if (diffclock != 0 && clock != 0) {
            double integrator = chip.integrator, temp;

            /* clear the update count */
            chip.update_count = 0;

            /* move the estimator up or down a step based on the bit */
            if (chip.databit != 0) {
                chip.shiftreg = ((chip.shiftreg << 1) | 1) & 7;
                integrator += chip.filter;
            } else {
                chip.shiftreg = (chip.shiftreg << 1) & 7;
                integrator -= chip.filter;
            }

            /* simulate leakage */
            integrator *= leak;

            /* if we got all 0's or all 1's in the last n bits, bump the step up */
            if (chip.shiftreg == 0 || chip.shiftreg == 7) {
                chip.filter = FILTER_MAX - ((FILTER_MAX - chip.filter) * charge);
                if (chip.filter > FILTER_MAX) {
                    chip.filter = FILTER_MAX;
                }
            } /* simulate decay */ else {
                chip.filter *= decay;
                if (chip.filter < FILTER_MIN) {
                    chip.filter = FILTER_MIN;
                }
            }

            /* compute the sample as a 32-bit word */
            temp = integrator * SAMPLE_GAIN;
            chip.integrator = integrator;

            /* compress the sample range to fit better in a 16-bit word */
            if (temp < 0) {
                chip.next_value = (short) ((int) (temp / (-temp * (1.0 / 32768.0) + 1.0)));
            } else {
                chip.next_value = (short) ((int) (temp / (temp * (1.0 / 32768.0) + 1.0)));
            }

            /* update the output buffer before changing the registers */
            stream_update(chip.channel, 0);
        }
    }

    public static void hc55516_digit_w(int num, int data) {
        hc55516[num].databit = data & 1;
    }

    public static void hc55516_clock_clear_w(int num, int data) {
        hc55516_clock_w(num, 0);
    }

    public static void hc55516_clock_set_w(int num, int data) {
        hc55516_clock_w(num, 1);
    }

    public static void hc55516_digit_clock_clear_w(int num, int data) {
        hc55516[num].databit = data & 1;
        hc55516_clock_w(num, 0);
    }

    public static WriteHandlerPtr hc55516_0_digit_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            hc55516_digit_w(0, data);
        }
    };
    public static WriteHandlerPtr hc55516_0_clock_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            hc55516_clock_w(0, data);
        }
    };
    public static WriteHandlerPtr hc55516_0_clock_clear_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            hc55516_clock_clear_w(0, data);
        }
    };
    public static WriteHandlerPtr hc55516_0_clock_set_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            hc55516_clock_set_w(0, data);
        }
    };
    public static WriteHandlerPtr hc55516_0_digit_clock_clear_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            hc55516_digit_clock_clear_w(0, data);
        }
    };
}
