/**
 * ported to 0.37b7
 */
package arcadeflex037b7;

import static common.util.*;
import arcadeflex036.osdepend;

public class Main {

    public static void main(String[] args) {
        ConvertArguments("arcadeflex", args);
        System.exit(osdepend.main(argc, argv));
    }
}
