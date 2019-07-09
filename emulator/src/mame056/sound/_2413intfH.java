/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.sound;

import static mame056.sound._3812intfH.*;

public class _2413intfH {
    public static int MAX_2413 	= (4);

    public static class YM2413interface
    {
            int num;
            int baseclock;
            int[] mixing_level = new int[MAX_2413];

        public YM2413interface(int num, int baseclock, int[] mixing_level) {
            this.num = num;
            this.baseclock = baseclock;
            this.mixing_level = mixing_level;
        }
    };
    
}
