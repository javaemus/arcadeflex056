/**
 * ported to v0.56
 *
 */
/**
 * Changelog
 * =========
 * 28/04/2019 ported to mame 0.56 (shadow)
 */
package mame056.sound;

public class adpcmH {

    public static final int MAX_ADPCM = 8;


    /* a generic ADPCM interface, for unknown chips */
    public static class ADPCMinterface {

        public ADPCMinterface(int num, int frequency, int region, int[] mixing_level) {
            this.num = num;
            this.frequency = frequency;
            this.region = region;
            this.mixing_level = mixing_level;
        }
        int num;/* total number of ADPCM decoders in the machine */
        int frequency;/* playback frequency */
        int region;/* memory region where the samples come from */
        int[] mixing_level;/* master volume */
    }

    
}
