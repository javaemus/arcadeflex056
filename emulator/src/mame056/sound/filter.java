/**
 * Ported to v0.56
 */
package mame056.sound;

import static mame056.sound.filterH.*;

public class filter {

    static _filter filter_alloc() {
        _filter f = new _filter();
        return f;
    }

    public static void filter_free(_filter f) {
        f = null;
    }

    public static void filter_state_reset(_filter f, filter_state s) {
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

    /*TODO*///
/*TODO*///void filter_state_free(filter_state* s) {
/*TODO*///	free(s);
/*TODO*///}
/*TODO*///
/*TODO*////****************************************************************************/
/*TODO*////* FIR */
/*TODO*///
/*TODO*///filter_real filter_compute(filter* f, filter_state* s) {
/*TODO*///	unsigned order = f->order;
/*TODO*///	unsigned midorder = f->order / 2;
/*TODO*///	filter_real y = 0;
/*TODO*///	unsigned i,j,k;
/*TODO*///
/*TODO*///	/* i == [0] */
/*TODO*///	/* j == [-2*midorder] */
/*TODO*///	i = s->prev_mac;
/*TODO*///	j = i + 1;
/*TODO*///	if (j == order)
/*TODO*///		j = 0;
/*TODO*///
/*TODO*///	/* x */
/*TODO*///	for(k=0;k<midorder;++k) {
/*TODO*///		y += f->xcoeffs[midorder-k] * (s->xprev[i] + s->xprev[j]);
/*TODO*///		++j;
/*TODO*///		if (j == order)
/*TODO*///			j = 0;
/*TODO*///		if (i == 0)
/*TODO*///			i = order - 1;
/*TODO*///		else
/*TODO*///			--i;
/*TODO*///	}
/*TODO*///	y += f->xcoeffs[0] * s->xprev[i];
/*TODO*///
/*TODO*///#ifdef FILTER_USE_INT
/*TODO*///	return y >> FILTER_INT_FRACT;
/*TODO*///#else
/*TODO*///	return y;
/*TODO*///#endif
/*TODO*///}
/*TODO*///
    public static _filter filter_lp_fir_alloc(double freq, int order) {
        _filter f = filter_alloc();
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
