/**
 * Ported to v0.56
 */
package mame056.sound;

public class filterH {

    /* Max filter order */
    public static final int FILTER_ORDER_MAX = 51;

    public static final int FILTER_INT_FRACT = 15;/* fractional bits */

    public static class filter {

        public int[] xcoeffs = new int[(FILTER_ORDER_MAX + 1) / 2];
        int /*unsigned*/ order;
    }

    public static class filter_state {

        int/*unsigned*/ prev_mac;
        int[] xprev = new int[FILTER_ORDER_MAX];
    }

    /* Insert a value in the filter state */
    public static void filter_insert(filter f, filter_state s, int x) {
        /* next state */
        ++s.prev_mac;
        if (s.prev_mac >= f.order) {
            s.prev_mac = 0;
        }

        /* set x[0] */
        s.xprev[s.prev_mac] = x;
    }
}
