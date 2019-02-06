/**
 * Ported to 0.56
 */
package mame056;

import arcadeflex056.fucPtr.*;

public class sndintrfH {

    public static class MachineSound {

        public MachineSound(int sound_type, Object sound_interface) {
            this.sound_type = sound_type;
            this.sound_interface = sound_interface;
        }

        public MachineSound() {
            this(0, null);
        }

        public static MachineSound[] create(int n) {
            MachineSound[] a = new MachineSound[n];
            for (int k = 0; k < n; k++) {
                a[k] = new MachineSound();
            }
            return a;
        }

        public int sound_type;
        public Object sound_interface;
    }

    public static final int SOUND_DUMMY = 0;
    public static final int SOUND_CUSTOM = 1;
    public static final int SOUND_SAMPLES = 2;
    public static final int SOUND_DAC = 3;
    public static final int SOUND_DISCRETE = 4;
    public static final int SOUND_AY8910 = 5;
    public static final int SOUND_YM2203 = 6;
    public static final int SOUND_YM2151 = 7;
    public static final int SOUND_YM2608 = 8;
    public static final int SOUND_YM2610 = 9;
    public static final int SOUND_YM2610B = 10;
    public static final int SOUND_YM2612 = 11;
    public static final int SOUND_YM3438 = 12;
    public static final int SOUND_YM2413 = 13;
    public static final int SOUND_YM3812 = 14;
    public static final int SOUND_YM3526 = 15;
    public static final int SOUND_YMZ280B = 16;
    public static final int SOUND_Y8950 = 17;
    public static final int SOUND_SN76477 = 18;
    public static final int SOUND_SN76496 = 19;
    public static final int SOUND_POKEY = 20;
    public static final int SOUND_NES = 21;
    public static final int SOUND_ASTROCADE = 22;
    public static final int SOUND_NAMCO = 23;
    public static final int SOUND_TMS36XX = 24;
    public static final int SOUND_TMS5110 = 25;
    public static final int SOUND_TMS5220 = 26;
    public static final int SOUND_VLM5030 = 27;
    public static final int SOUND_ADPCM = 28;
    public static final int SOUND_OKIM6295 = 29;
    public static final int SOUND_MSM5205 = 30;
    public static final int SOUND_UPD7759 = 31;
    public static final int SOUND_HC55516 = 32;
    public static final int SOUND_K005289 = 33;
    public static final int SOUND_K007232 = 34;
    public static final int SOUND_K051649 = 35;
    public static final int SOUND_K053260 = 36;
    public static final int SOUND_K054539 = 37;
    public static final int SOUND_SEGAPCM = 38;
    public static final int SOUND_RF5C68 = 39;
    public static final int SOUND_CEM3394 = 40;
    public static final int SOUND_C140 = 41;
    public static final int SOUND_QSOUND = 42;
    public static final int SOUND_SAA1099 = 43;
    public static final int SOUND_IREMGA20 = 44;
    public static final int SOUND_ES5505 = 45;
    public static final int SOUND_ES5506 = 46;

    public static final int SOUND_COUNT = 47;

    /* structure for SOUND_CUSTOM sound drivers */
    public static class CustomSound_interface {

        public CustomSound_interface(ShStartPtr sh_start, ShStopPtr sh_stop, ShUpdatePtr sh_update) {
            this.sh_start = sh_start;
            this.sh_stop = sh_stop;
            this.sh_update = sh_update;
        }

        public ShStartPtr sh_start;
        public ShStopPtr sh_stop;
        public ShUpdatePtr sh_update;

    }
}
