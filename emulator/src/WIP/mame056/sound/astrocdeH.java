/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.sound;

public class astrocdeH {
    
    public static final int MAX_ASTROCADE_CHIPS = 2;   /* max number of emulated chips */

    public static class astrocade_interface
    {
            public int num;			/* total number of sound chips in the machine */
            public int baseclock;			/* astrocade clock rate  */
            public int[] volume = new int[MAX_ASTROCADE_CHIPS];			/* master volume */
            
            public astrocade_interface(int num, int baseclock, int[] volume){
                this.num = num;			/* total number of sound chips in the machine */
                this.baseclock = baseclock;			/* astrocade clock rate  */
                this.volume = volume;
            }
    };
    
}
