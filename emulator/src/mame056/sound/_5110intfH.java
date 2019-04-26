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

import static mame056.sound.tms5110H.*;

public class _5110intfH {

    public static abstract interface IrqPtr {

        public abstract void handler(int state);
    }

    public static class TMS5110interface {

        public TMS5110interface(int baseclock, int mixing_level, IrqPtr irq, M0_callbackPtr M0_callback) {
            this.baseclock = baseclock;
            this.mixing_level = mixing_level;
            this.irq = irq;
            this.M0_callback = M0_callback;
        }

        int baseclock;
        /* clock rate = 80 * output sample rate,     */
 /* usually 640000 for 8000 Hz sample rate or */
 /* usually 800000 for 10000 Hz sample rate.  */
        int mixing_level;
        IrqPtr irq;/* IRQ callback function */
        M0_callbackPtr M0_callback;/* function to be called when chip requests another bit*/
    }

}
