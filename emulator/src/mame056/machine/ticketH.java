/***************************************************************************

	Function prototypes and constants for the ticket dispenser emulator

***************************************************************************/

package mame056.machine;

public class ticketH {


    public static int TICKET_MOTOR_ACTIVE_LOW    = 0;    /* Ticket motor is triggered by D7=0 */
    public static int TICKET_MOTOR_ACTIVE_HIGH   = 1;    /* Ticket motor is triggered by D7=1 */

    public static int TICKET_STATUS_ACTIVE_LOW   = 0;    /* Ticket is done dispensing when D7=0 */
    public static int TICKET_STATUS_ACTIVE_HIGH  = 1;    /* Ticket is done dispensing when D7=1 */
    
}
