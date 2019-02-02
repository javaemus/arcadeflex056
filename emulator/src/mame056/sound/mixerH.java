/**
 *  ported to 0.56
 */
package mame056.sound;

public class mixerH {

    public static final int MIXER_MAX_CHANNELS = 16;

    public static final int MIXER_PAN_CENTER = 0;
    public static final int MIXER_PAN_LEFT = 1;
    public static final int MIXER_PAN_RIGHT = 2;

    public static final int MIXER(int level, int pan) {
        return ((level & 0xff) | ((pan & 0x03) << 8));
    }

    public static final int MIXER_GAIN_1x = 0;
    public static final int MIXER_GAIN_2x = 1;
    public static final int MIXER_GAIN_4x = 2;
    public static final int MIXER_GAIN_8x = 3;

    public static final int MIXERG(int level, int gain, int pan) {
        return ((level & 0xff) | ((gain & 0x03) << 10) | ((pan & 0x03) << 8));
    }

    public static final int MIXER_GET_LEVEL(int mixing_level) {
        return ((mixing_level) & 0xff);
    }

    public static final int MIXER_GET_PAN(int mixing_level) {
        return (((mixing_level) >> 8) & 0x03);
    }

    public static final int MIXER_GET_GAIN(int mixing_level) {
        return (((mixing_level) >> 10) & 0x03);
    }
}
