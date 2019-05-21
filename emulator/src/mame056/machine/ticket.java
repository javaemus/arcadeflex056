/***************************************************************************

  machine.c

  Functions to emulate a prototypical ticket dispenser hardware.

  Right now, this is an *extremely* basic ticket dispenser.
  TODO:	Active Bit may not be Bit 7 in all applications.
  	    Add a ticket dispenser interface instead of passing a bunch
		of arguments to ticket_dispenser_init.
		Add sound, graphical output?
***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.machine;

import static mame056.machine.ticketH.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.common.*;
import static arcadeflex056.fucPtr.*;
import static common.ptr.*;

public class ticket
{
	
	static int status;
	static int power;
	static int time_msec;
	static int motoron;
	static int ticketdispensed;
	static int ticketnotdispensed;
	static timer_entry timer;
	
	static int active_bit = 0x80;
	
	
	/***************************************************************************
	  ticket_dispenser_init
	
	***************************************************************************/
	public static void ticket_dispenser_init(int msec, int motoronhigh, int statusactivehigh)
	{
		time_msec          = msec;
		motoron            = motoronhigh!=0  ? active_bit : 0;
		ticketdispensed    = statusactivehigh!=0 ? active_bit : 0;
		ticketnotdispensed = ticketdispensed ^ active_bit;
	
		status = ticketnotdispensed;
		dispensed_tickets = 0;
		power  = 0x00;
	}
	
	/***************************************************************************
	  ticket_dispenser_r
	***************************************************************************/
	public static ReadHandlerPtr ticket_dispenser_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	/*TODO*///#ifdef DEBUG_TICKET
	/*TODO*///	logerror("PC: %04X  Ticket Status Read = %02X\n", cpu_get_pc(), status);
	/*TODO*///#endif
		return status;
	} };
	
	/***************************************************************************
	  ticket_dispenser_w
	***************************************************************************/
	public static WriteHandlerPtr ticket_dispenser_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* On an activate signal, start dispensing! */
		if ((data & active_bit) == motoron)
		{
			if (power == 0)
			{
	/*TODO*///#ifdef DEBUG_TICKET
	/*TODO*///			logerror("PC: %04X  Ticket Power On\n", cpu_get_pc());
	/*TODO*///#endif
				timer = timer_set (TIME_IN_MSEC(time_msec), 0, ticket_dispenser_toggle);
				power = 1;
	
				status = ticketnotdispensed;
			}
		}
		else
		{
			if (power != 0)
			{
	/*TODO*///#ifdef DEBUG_TICKET
	/*TODO*///			logerror("PC: %04X  Ticket Power Off\n", cpu_get_pc());
	/*TODO*///#endif
				timer_remove(timer);
	/*TODO*///			set_led_status(2,0);
				power = 0;
			}
		}
	} };
	
	
	/***************************************************************************
	  ticket_dispenser_toggle
	
	  How I think this works:
	  When a ticket dispenses, there is N milliseconds of status = high,
	  and N milliseconds of status = low (a wait cycle?).
	***************************************************************************/
	static timer_callback ticket_dispenser_toggle = new timer_callback() {
            public void handler(int param) {
                
		/* If we still have power, keep toggling ticket states. */
		if (power != 0)
		{
			status ^= active_bit;
	/*TODO*///#ifdef DEBUG_TICKET
	/*TODO*///		logerror("Ticket Status Changed to %02X\n", status);
	/*TODO*///#endif
			timer = timer_set (TIME_IN_MSEC(time_msec), 0, ticket_dispenser_toggle);
		}
	
		if (status == ticketdispensed)
		{
	/*TODO*///		set_led_status(2,1);
			dispensed_tickets++;
	
	/*TODO*///#ifdef DEBUG_TICKET
	/*TODO*///		logerror("Ticket Dispensed\n");
	/*TODO*///#endif
		}
		else
		{
	/*TODO*///		set_led_status(2,0);
		}
            }
        };
	
}
