/**
 * ported to v0.37b7
 *
 */
package mame037b7;

import static mame056.driverH.*;

import static mame056.drivers.bankp.*;
import static mame056.drivers.minivadr.*;

public class driver {

    public static GameDriver drivers[] = {
        driver_bankp,
        driver_minivadr,
        null
    };
}
