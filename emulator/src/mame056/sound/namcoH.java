/**
 * ported to v0.56
 * ported to v0.37b7
 */
package mame056.sound;

public class namcoH {

    public static class namco_interface {

        public namco_interface(int samplerate, int voices, int volume, int region, int stereo) {
            this.samplerate = samplerate;
            this.voices = voices;
            this.volume = volume;
            this.region = region;
            this.stereo = stereo;
        }

        //without stereo value
        public namco_interface(int samplerate, int voices, int volume, int region) {
            this.samplerate = samplerate;
            this.voices = voices;
            this.volume = volume;
            this.region = region;
            this.stereo = 0;
        }

        public int samplerate;/* sample rate */
        public int voices;/* number of voices */
        public int volume;/* playback volume */
        public int region;/* memory region; -1 to use RAM (pointed to by namco_wavedata) */
        public int stereo;/* set to 1 to indicate stereo (e.g., System 1) */
    };
}
//keep that as reference
//#define mappy_soundregs namco_soundregs
//#define pengo_soundregs namco_soundregs
//#define polepos_soundregs namco_soundregs
