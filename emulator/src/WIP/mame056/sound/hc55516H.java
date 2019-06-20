/**
 * ported to v0.56
 */
package WIP.mame056.sound;

public class hc55516H {

    public static final int MAX_HC55516 = 4;

    public static class hc55516_interface {

        public hc55516_interface(int num, int[] volume) {
            this.num = num;
            this.volume = volume;
        }

        public int num;
        public int[] volume;
    }
}
