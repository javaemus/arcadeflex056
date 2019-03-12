/**
 * Ported to v0.56
 */
package mame056.sound;

import static mame056.sound.filterH.*;

public class filterC {

    static filter filter_alloc() {
        filter f = new filter();
        return f;
    }

    public static void filter_free(filter f) {
        f = null;
    }

    public static void filter_state_reset(filter f, filter_state s) {
        int i;
        s.prev_mac = 0;
        for (i = 0; i < f.order; ++i) {
            s.xprev[i] = 0;
        }
    }

    public static filter_state filter_state_alloc() {
        int i;
        filter_state s = new filter_state();
        s.prev_mac = 0;
        for (i = 0; i < FILTER_ORDER_MAX; ++i) {
            s.xprev[i] = 0;
        }
        return s;
    }

    public static void filter_state_free(filter_state s) {
        s = null;
    }

    /**
     * *************************************************************************
     */
    /* FIR */
    public static int filter_compute(filter f, filter_state s) {
        int/*unsigned*/ order = f.order;
        int/*unsigned*/ midorder = f.order / 2;
        int y = 0;
        int/*unsigned*/ i, j, k;

        /* i == [0] */
 /* j == [-2*midorder] */
        i = s.prev_mac;
        j = i + 1;
        if (j == order) {
            j = 0;
        }

        /* x */
        for (k = 0; k < midorder; ++k) {
            y += f.xcoeffs[midorder - k] * (s.xprev[i] + s.xprev[j]);
            ++j;
            if (j == order) {
                j = 0;
            }
            if (i == 0) {
                i = order - 1;
            } else {
                --i;
            }
        }
        y += f.xcoeffs[0] * s.xprev[i];

        return y >> FILTER_INT_FRACT;
    }

    public static filter filter_lp_fir_alloc(double freq, int order) {
        filter f = filter_alloc();
        int/*unsigned*/ midorder = (order - 1) / 2;
        int/*unsigned*/ i;
        double gain;

        /* Compute the antitrasform of the perfect low pass filter */
        gain = 2 * freq;
        f.xcoeffs[0] = (int) (gain * (1 << FILTER_INT_FRACT));
        for (i = 1; i <= midorder; ++i) {
            /* number of the sample starting from 0 to (order-1) included */
            int/*unsigned*/ n = i + midorder;

            /* sample value */
            double c = Math.sin(2 * Math.PI * freq * i) / (Math.PI * i);

            /* apply only one window or none */
 /* double w = 2 - 2*n/(order-1); */ /* Bartlett (triangular) */
 /* double w = 0.5 * (1 - cos(2*M_PI*n/(order-1))); */ /* Hanning */
            double w = 0.54 - 0.46 * Math.cos(2 * Math.PI * n / (order - 1));
            /* Hamming */
 /* double w = 0.42 - 0.5 * cos(2*M_PI*n/(order-1)) + 0.08 * cos(4*M_PI*n/(order-1)); */ /* Blackman */

 /* apply the window */
            c *= w;

            /* update the gain */
            gain += 2 * c;

            /* insert the coeff */
            f.xcoeffs[i] = (int) (c * (1 << FILTER_INT_FRACT));

        }

        /* adjust the gain to be exact 1.0 */
        for (i = 0; i <= midorder; ++i) {
            f.xcoeffs[i] /= gain;

        }

        /* decrease the order if the last coeffs are 0 */
        i = midorder;
        while (i > 0 && f.xcoeffs[i] == 0.0) {
            --i;
        }

        f.order = i * 2 + 1;

        return f;
    }
}
