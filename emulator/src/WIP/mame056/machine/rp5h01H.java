/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;


public class rp5h01H {
    /* max simultaneous chips supported. change if you need more */
    public static int MAX_RP5H01	= 1;

    public static class RP5H01_interface {
        public int num;					/* number of chips */
        public int[] region = new int[MAX_RP5H01];		/* memory region where data resides */
        public int[] offset = new int[MAX_RP5H01];		/* memory offset within the above region where data resides */

        public RP5H01_interface(int num, int[] region, int[] offset) {
            this.num = num;
            this.region = region;
            this.offset = offset;
        }
    };
}
