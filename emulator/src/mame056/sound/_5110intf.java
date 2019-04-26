/**
 * ported to v0.56
 * ported to v0.37b7
 *
 */
/**
 * Changelog
 * ---------
 * 26/04/2019 - ported 5110 sound  to 0.56 (shadow)
 */
package mame056.sound;

import static arcadeflex056.fucPtr.*;
import static arcadeflex036.osdepend.*;

import static common.ptr.*;

import static mame056.mame.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;

import static mame056.sound._5110intfH.*;
import static mame056.sound.tms5110.*;
import static mame056.sound.streams.*;

public class _5110intf extends snd_interface {

    public static final int MAX_SAMPLE_CHUNK = 10000;
    public static final int FRAC_BITS = 14;
    public static final int FRAC_ONE = (1 << FRAC_BITS);
    public static final int FRAC_MASK = (FRAC_ONE - 1);


    /* the state of the streamed output */
    static TMS5110interface intf;
    static short last_sample, curr_sample;
    static /*UINT32*/ int source_step;
    static /*UINT32*/ int source_pos;
    static int stream;

    public _5110intf() {
        this.name = "TMS5110";
        this.sound_num = SOUND_TMS5110;
    }

    @Override
    public int chips_num(MachineSound msound) {
        return 0;//no functionality expected
    }

    @Override
    public int chips_clock(MachineSound msound) {
        return ((TMS5110interface) msound.sound_interface).baseclock;
    }

    /**
     * ****************************************************************************
     * tms5110_sh_start -- allocate buffers and reset the 5110
     * ****************************************************************************
     */
    @Override
    public int start(MachineSound msound) {
        intf = (TMS5110interface) msound.sound_interface;

        if (intf.M0_callback == null) {
            logerror("\n file: 5110intf.c, tms5110_sh_start(), line 53:\n  Missing _mandatory_ 'M0_callback' function pointer in the TMS5110 interface\n  This function is used by TMS5110 to call for a single bits\n  needed to generate the speech\n  Aborting startup...\n");
            return 1;
        }
        tms5110_set_M0_callback(intf.M0_callback);

        /* reset the 5110 */
        tms5110_reset();

        /* set the initial frequency */
        stream = -1;
        tms5110_set_frequency(intf.baseclock);
        source_pos = 0;
        last_sample = curr_sample = 0;

        /* initialize a stream */
        stream = stream_init("TMS5110", intf.mixing_level, Machine.sample_rate, 0, tms5110_update);
        if (stream == -1) {
            return 1;
        }

        /* request a sound channel */
        return 0;
    }

    @Override
    public void stop() {
        //no functionality expected
    }

    @Override
    public void update() {
        //no functionality expected
    }

    @Override
    public void reset() {
        //no functionality expected
    }

    /**
     * ****************************************************************************
     * tms5110_CTL_w -- write Control Command to the sound chip commands like
     * Speech, Reset, etc., are loaded into the chip via the CTL pins
     * ****************************************************************************
     */
    public static WriteHandlerPtr tms5110_CTL_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* bring up to date first */
            stream_update(stream, 0);
            tms5110_CTL_set(data);
        }
    };

    /**
     * ****************************************************************************
     * tms5110_PDC_w -- write to PDC pin on the sound chip
     * ****************************************************************************
     */
    public static WriteHandlerPtr tms5110_PDC_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* bring up to date first */
            stream_update(stream, 0);
            tms5110_PDC_set(data);
        }
    };

    /**
     * ****************************************************************************
     * tms5110_status_r -- read status from the sound chip
     * ****************************************************************************
     */
    public static ReadHandlerPtr tms5110_status_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            /* bring up to date first */
            stream_update(stream, 0);
            return tms5110_status_read();
        }
    };

    /**
     * ****************************************************************************
     * tms5110_ready_r -- return the not ready status from the sound chip
     * ****************************************************************************
     */
    public static int tms5110_ready_r() {
        /* bring up to date first */
        stream_update(stream, 0);
        return tms5110_ready_read();
    }

    /**
     * ****************************************************************************
     * tms5110_update -- update the sound chip so that it is in sync with CPU
     * execution
     * ****************************************************************************
     */
    public static StreamInitPtr tms5110_update = new StreamInitPtr() {
        public void handler(int ch, ShortPtr buffer, int length) {
            ShortPtr sample_data = new ShortPtr(MAX_SAMPLE_CHUNK);
            ShortPtr curr_data = new ShortPtr(sample_data);
            short prev = last_sample, curr = curr_sample;
            /*UINT32*/
            int final_pos;
            /*UINT32*/
            int new_samples;

            /* finish off the current sample */
            if (source_pos > 0) {
                /* interpolate */
                while (length > 0 && source_pos < FRAC_ONE) {
                    buffer.writeinc((short) ((((int) prev * (FRAC_ONE - source_pos)) + ((int) curr * source_pos)) >> FRAC_BITS));
                    source_pos += source_step;
                    length--;
                }

                /* if we're over, continue; otherwise, we're done */
                if (source_pos >= FRAC_ONE) {
                    source_pos -= FRAC_ONE;
                } else {
                    tms5110_process(sample_data, 0);
                    return;
                }
            }

            /* compute how many new samples we need */
            final_pos = source_pos + length * source_step;
            new_samples = (final_pos + FRAC_ONE - 1) >> FRAC_BITS;
            if (new_samples > MAX_SAMPLE_CHUNK) {
                new_samples = MAX_SAMPLE_CHUNK;
            }

            /* generate them into our buffer */
            tms5110_process(sample_data, new_samples);
            prev = curr;
            curr = curr_data.readinc();

            /* then sample-rate convert with linear interpolation */
            while (length > 0) {
                /* interpolate */
                while (length > 0 && source_pos < FRAC_ONE) {
                    buffer.writeinc((short) ((((int) prev * (FRAC_ONE - source_pos)) + ((int) curr * source_pos)) >> FRAC_BITS));
                    source_pos += source_step;
                    length--;
                }

                /* if we're over, grab the next samples */
                if (source_pos >= FRAC_ONE) {
                    source_pos -= FRAC_ONE;
                    prev = curr;
                    curr = curr_data.readinc();
                }
            }

            /* remember the last samples */
            last_sample = prev;
            curr_sample = curr;
        }
    };

    /**
     * ****************************************************************************
     * tms5110_set_frequency -- adjusts the playback frequency
     * ****************************************************************************
     */
    public static void tms5110_set_frequency(int frequency) {
        /* skip if output frequency is zero */
        if (Machine.sample_rate == 0) {
            return;
        }

        /* update the stream and compute a new step size */
        if (stream != -1) {
            stream_update(stream, 0);
        }
        source_step = (/*UINT32*/int) ((double) (frequency / 80) * (double) FRAC_ONE / (double) Machine.sample_rate);
    }
}
