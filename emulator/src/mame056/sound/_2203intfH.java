/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.sound;

import static arcadeflex056.fucPtr.*;
import static mame056.driverH.*;
import static mame056.sound.ay8910H.*;

public class _2203intfH {
    
    public static final int MAX_2203 = 4;
    
    /* volume level for YM2203 */
    public static int YM2203_VOL(int FM_VOLUME, int SSG_VOLUME) {
        return (((FM_VOLUME) << 16) + (SSG_VOLUME));
    }
    
    public static class YM2203interface extends AY8910interface {
        public YM2203interface(int num, int baseclock, int[] mixing_level, ReadHandlerPtr[] pAr, ReadHandlerPtr[] pBr, WriteHandlerPtr[] pAw, WriteHandlerPtr[] pBw, WriteYmHandlerPtr[] ym_handler) {
            super(num, baseclock, mixing_level, pAr, pBr, pAw, pBw, ym_handler);
        }

        public YM2203interface(int num, int baseclock, int[] mixing_level, ReadHandlerPtr[] pAr, ReadHandlerPtr[] pBr, WriteHandlerPtr[] pAw, WriteHandlerPtr[] pBw) {
            super(num, baseclock, mixing_level, pAr, pBr, pAw, pBw);
        }
        
        /*TODO*///int YM2203_sh_start(const struct MachineSound *msound);
	
	/*TODO*///void YM2203UpdateRequest(int chip);
    }
}
