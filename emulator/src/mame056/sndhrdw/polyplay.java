/**
 * Ported to 0.56
 * Ported to 0.37b7
 */
/**
 * Changelog
 * =========
 * 28/04/2019 ported to mame 0.56 (shadow)
 */
package mame056.sndhrdw;

import static arcadeflex056.fucPtr.*;

import static common.ptr.ShortPtr;
import static common.libc.expressions.*;

import static mame056.sndintrfH.*;

import static mame056.sound.mixer.*;
import static mame037b11.sound.mixer.*;

public class polyplay {

    public static final int LFO_VOLUME = 25;
    public static final int SAMPLE_LENGTH = 32;
    public static final int SAMPLE_AMPLITUDE = 0x4000;

    public static int freq1, freq2, channellfo, channel_playing1, channel_playing2;
    static int lfovol[] = {LFO_VOLUME, LFO_VOLUME};

    static short[] backgroundwave = new short[SAMPLE_LENGTH];

    public static ShStartPtr polyplay_sh_start = new ShStartPtr() {
        public int handler(MachineSound msound) {
            int i;

            for (i = 0; i < SAMPLE_LENGTH / 2; i++) {
                backgroundwave[i] = +SAMPLE_AMPLITUDE;
            }
            for (i = SAMPLE_LENGTH / 2; i < SAMPLE_LENGTH; i++) {
                backgroundwave[i] = -SAMPLE_AMPLITUDE;
            }
            freq1 = freq2 = 110;
            channellfo = mixer_allocate_channels(2, lfovol);
            mixer_set_name(channellfo + 0, "Polyplay #0");
            mixer_set_name(channellfo + 1, "Polyplay #1");
            mixer_set_volume(channellfo + 0, 0);
            mixer_set_volume(channellfo + 1, 0);

            channel_playing1 = 0;
            channel_playing2 = 0;
            return 0;
        }
    };

    public static ShStopPtr polyplay_sh_stop = new ShStopPtr() {
        public void handler() {
            mixer_stop_sample(channellfo + 0);
            mixer_stop_sample(channellfo + 1);
        }
    };

    public static ShUpdatePtr polyplay_sh_update = new ShUpdatePtr() {
        public void handler() {
            mixer_set_sample_frequency(channellfo + 0, sizeof(backgroundwave) * freq1);
            mixer_set_sample_frequency(channellfo + 1, sizeof(backgroundwave) * freq2);
        }
    };

    public static void set_channel1(int active) {
        channel_playing1 = active;
    }

    public static void set_channel2(int active) {
        channel_playing2 = active;
    }

    public static void play_channel1(int data) {
        if (data != 0) {
            freq1 = 2457600 / 16 / data / 8;
            mixer_set_volume(channellfo + 0, channel_playing1 * 100);
            mixer_play_sample_16(channellfo + 0, new ShortPtr(backgroundwave), sizeof(backgroundwave) * 2, sizeof(backgroundwave) * freq1, 1);
        } else {
            polyplay_sh_stop.handler();
        }
    }

    public static void play_channel2(int data) {
        if (data != 0) {
            freq2 = 2457600 / 16 / data / 8;
            mixer_set_volume(channellfo + 1, channel_playing2 * 100);
            mixer_play_sample_16(channellfo + 1, new ShortPtr(backgroundwave), sizeof(backgroundwave) * 2, sizeof(backgroundwave) * freq2, 1);
        } else {
            polyplay_sh_stop.handler();
        }
    }
}
