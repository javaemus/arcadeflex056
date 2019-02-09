package common.libc;

/**
 *
 * @author shadow
 */
public class ctime {

    public static final int UCLOCKS_PER_SEC = 1000000000;

    /*
     *   return system's timer
     */
    public static long uclock() {
        return System.nanoTime();
    }
}
