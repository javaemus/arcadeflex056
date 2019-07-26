/***************************************************************************

Atari Basketball machine

If you have any questions about how this driver works, don't hesitate to
ask.  - Mike Balfour (mab22@po.cwru.edu)
***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static arcadeflex056.fucPtr.*;
import static mame056.cpuexec.*;
import static mame056.inptport.*;

public class bsktball
{
	
	static int LD1=0;
	static int LD2=0;
	static int NMION = 0;
	
	/***************************************************************************
	bsktball_nmion_w
	***************************************************************************/
	public static WriteHandlerPtr bsktball_nmion_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		NMION = offset & 0x01;
	} };
	
	/***************************************************************************
	bsktball_interrupt
	***************************************************************************/
        static int i256V=0;
	/* NMI every 32V, IRQ every VBLANK */
	public static InterruptPtr bsktball_interrupt = new InterruptPtr() { public int handler() 
	{
		
	
		/* We mod by 8 because we're interrupting 8x per frame, 1 per 32V */
		i256V=(i256V+1) % 8;
	
		if (i256V==0)
			return interrupt.handler();
		else if (NMION != 0)
			return nmi_interrupt.handler();
		else
			return ignore_interrupt.handler();
	} };
	
	/***************************************************************************
	bsktball_ld_w
	***************************************************************************/
	
	public static WriteHandlerPtr bsktball_ld1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		LD1 = (offset & 0x01);
	} };
	
	public static WriteHandlerPtr bsktball_ld2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		LD2 = (offset & 0x01);
	} };
	
	
	/***************************************************************************
	bsktball_in0_r
	***************************************************************************/
	static int DR0=0;		/* PL2 H DIR */
        static int DR1=0;		/* PL2 V DIR */
        static int DR2=0;		/* PL1 H DIR */
        static int DR3=0;		/* PL1 V DIR */

        static int last_p1_horiz=0;
        static int last_p1_vert=0;
        static int last_p2_horiz=0;
        static int last_p2_vert=0;
        
	public static ReadHandlerPtr bsktball_in0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		
		int p1_horiz;
		int p1_vert;
		int p2_horiz;
		int p2_vert;
		int temp;
	
		p1_horiz = input_port_0_r.handler(offset);
		p1_vert  = input_port_1_r.handler(offset);
		p2_horiz = input_port_2_r.handler(offset);
		p2_vert  = input_port_3_r.handler(offset);
	
		/* Set direction bits */
	
		/* P1 H DIR */
		if (p1_horiz > last_p1_horiz)
		{
			if ((p1_horiz-last_p1_horiz) > 128)		DR2=0x40;
			else									DR2=0;
		}
		else if (p1_horiz < last_p1_horiz)
		{
			if ((last_p1_horiz-p1_horiz) > 128)		DR2=0;
			else									DR2=0x40;
		}
	
		/* P1 V DIR */
		if (p1_vert > last_p1_vert)
		{
			if ((p1_vert-last_p1_vert) > 128)		DR3=0;
			else									DR3=0x80;
		}
		else if (p1_vert < last_p1_vert)
		{
			if ((last_p1_vert-p1_vert) > 128)		DR3=0x80;
			else									DR3=0;
		}
	
		/* P2 H DIR */
		if (p2_horiz > last_p2_horiz)
		{
			if ((p2_horiz-last_p2_horiz) > 128)		DR0=0x10;
			else									DR0=0;
		}
		else if (p2_horiz < last_p2_horiz)
		{
			if ((last_p2_horiz-p2_horiz) > 128)		DR0=0;
			else									DR0=0x10;
		}
	
		/* P2 V DIR */
		if (p2_vert > last_p2_vert)
		{
			if ((p2_vert-last_p2_vert) > 128)		DR1=0;
			else									DR1=0x20;
		}
		else if (p2_vert < last_p2_vert)
		{
			if ((last_p2_vert-p2_vert) > 128)		DR1=0x20;
			else									DR1=0;
		}
	
	
		last_p1_horiz = p1_horiz;
		last_p1_vert  = p1_vert;
		last_p2_horiz = p2_horiz;
		last_p2_vert  = p2_vert;
	
		/* D0-D3 = Plyr 1 Horiz, D4-D7 = Plyr 1 Vert */
		if (((LD1) & (LD2)) != 0)
		{
			return ((p1_horiz & 0x0F) | ((p1_vert << 4) & 0xF0));
		}
		/* D0-D3 = Plyr 2 Horiz, D4-D7 = Plyr 2 Vert */
		else if (LD2 != 0)
		{
			return ((p2_horiz & 0x0F) | ((p2_vert << 4) & 0xF0));
		}
		else
		{
			temp = input_port_4_r.handler(offset) & 0x0F;
			/* Remap button 1 back to the Start button */
			/* NOTE:  This is an ADDED feature, not a part of the original hardware! */
			temp = (temp) & (temp>>2);
	
			return (temp | DR0 | DR1 | DR2 | DR3);
		}
	} };
	
	/***************************************************************************
	bsktball_led_w
	***************************************************************************/
	public static WriteHandlerPtr bsktball_led1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*TODO*///set_led_status(0,offset & 0x01);
	} };
	
	public static WriteHandlerPtr bsktball_led2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*TODO*///set_led_status(1,offset & 0x01);
	} };
	
}
