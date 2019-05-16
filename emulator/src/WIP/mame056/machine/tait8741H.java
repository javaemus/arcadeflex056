package WIP.mame056.machine;

import arcadeflex056.fucPtr.ReadHandlerPtr;

public class tait8741H {
    public static int MAX_TAITO8741 = 4;

    /* NEC 8741 program mode */
    public static final int TAITO8741_MASTER = 0;
    public static final int TAITO8741_SLAVE  = 1;
    public static final int TAITO8741_PORT   = 2;

    public static class TAITO8741interface
    {
            public int num;
            public int[] mode=new int[MAX_TAITO8741];            /* program select */
            public int[] serial_connect=new int[MAX_TAITO8741];	/* serial port connection */
            public ReadHandlerPtr[] portHandler_r=new ReadHandlerPtr[MAX_TAITO8741]; /* parallel port handler */

        public TAITO8741interface(int num, int[] mode, int[] serial_connect, ReadHandlerPtr[] portHandler_r) {
            this.num = num;
            this.mode = mode;
            this.serial_connect = serial_connect;
            this.portHandler_r = portHandler_r;
        }
    };

}
