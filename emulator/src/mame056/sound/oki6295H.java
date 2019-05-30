/**
 * ported to v0.56
 *
 */
/**
 * Changelog
 * =========
 * 30/05/2019 ported to mame 0.56 (chusogar)
 */
package mame056.sound;

public class oki6295H {
    
    /* an interface for the OKIM6295 and similar chips */

    public static final int MAX_OKIM6295 = 2;

    /*
      Note about the playback frequency: the external clock is internally divided,
      depending on pin 7, by 132 (high) or 165 (low). This isn't handled by the
      emulation, so you have to provide the didvided internal clock instead of the
      external clock.
    */
    public static class OKIM6295interface
    {
            public int num;                  		/* total number of chips */
            public int[] frequency = new int[MAX_OKIM6295];	/* playback frequency */
            public int[] region = new int[MAX_OKIM6295];		/* memory region where the sample ROM lives */
            public int[] mixing_level = new int[MAX_OKIM6295];	/* master volume */
            
            public OKIM6295interface (int num, int[] frequency, int[] region, int[] mixing_level){
                this.num = num;
                this.frequency = frequency;
                this.region = region;
                this.mixing_level = mixing_level;
            }
    };
}
