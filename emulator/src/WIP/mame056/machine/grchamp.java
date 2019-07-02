/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static arcadeflex056.fucPtr.*;
import static mame056.common.*;
import static mame056.cpuintrfH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuexecH.*;
import static mame056.inptport.*;
import static mame056.sndintrf.*;

import static WIP.mame056.vidhrdw.grchamp.*;

public class grchamp
{
	
	public static int[] grchamp_cpu_irq_enable = new int[2];
	
	static int comm_latch;
	static int[] comm_latch2 = new int[4];
	
	/***************************************************************************
	
		Machine Init
	
	***************************************************************************/
	
	public static InitDriverPtr init_grchamp = new InitDriverPtr() { public void handler()  {
		/* clear the irq latches */
		grchamp_cpu_irq_enable[0] = grchamp_cpu_irq_enable[1] = 0;
	
		/* if the coin system is 1 way, lock Coin B (Page 40) */
		/*TODO*///if (( readinputport( 1 ) & 0x10 ) != 0)
		/*TODO*///	coin_lockout_w( 1, 1 );
	} };
	
	/*
		A note about port signals (note the preceding asterisk):
		*OUTxx = OUT port signals from CPU1
		OUTxx = OUT port signals from CPU2
	*/
	
	/***************************************************************************
	
		CPU 1
	
	***************************************************************************/
	
	public static ReadHandlerPtr grchamp_port_0_r  = new ReadHandlerPtr() { public int handler(int offset) {
		return comm_latch;
	} };
	
	public static WriteHandlerPtr grchamp_control0_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		/* *OUT0 - Page 42 */
		/* bit 0 = trigger irq on cpu1 (itself) when vblank arrives */
		/* bit 1 = enable PC3259 (10A), page 41, top-left. TODO */
		/* bit 2/3 = unused */
		/* bit 4 = HEAD LAMP (1-D5) */
		/* bit 5 = CHANGE (1-D5?) */
		/* bit 6 = FOG OUT (1-E4) */
		/* bit 7 = RADAR ON (S26) */
		grchamp_videoreg0.write( data );
		grchamp_cpu_irq_enable[0] = data & 1;	/* bit 0 */
	//	osd_led_w( 0, ( ~data >> 4 ) & 1 ); 	/* bit 4 */
	} };
	
	public static WriteHandlerPtr grchamp_coinled_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		/* *OUT9 - Page 40 */
		/* bit 0-3 = unused */
		/* bit 4 = Coin Lockout */
		/* bit 5 = Game Over lamp */
		/* bit 6/7 = unused */
	//	coin_lockout_global_w( 0, ( data >> 4 ) & 1 );	/* bit 4 */
	//	osd_led_w( 1, ( ~data >> 5 ) & 1 ); 			/* bit 5 */
	} };
	
	public static WriteHandlerPtr grchamp_sound_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		/* *OUT14 - Page 42 */
		soundlatch_w.handler( 0, data );
		cpu_set_nmi_line( 2, PULSE_LINE );
	} };
	
	public static WriteHandlerPtr grchamp_comm_w = new WriteHandlerPtr() {public void handler(int offset, int data){
		/* *OUT16 - Page 40 */
		comm_latch2[ offset & 3] = data;
	} };
	
	/***************************************************************************
	
		CPU 2
	
	***************************************************************************/
	
	public static ReadHandlerPtr grchamp_port_1_r  = new ReadHandlerPtr() { public int handler(int offset) {
		return comm_latch2[offset];
	} };
	
	public static WriteHandlerPtr grchamp_port_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		grchamp_vreg1[offset] = data;
	
		switch( offset ) { 	/* OUT0 - OUTF (Page 48) */
			/* OUT0 - Page 43: writes to 'Left Synk Bus' */		/* bg0 yscroll lsb */
			/* OUT1 - Page 43: writes to 'Left Synk Bus' */		/* bg0 yscroll msb */
			/* OUT2 - Page 43: writes to 'Left Synk Bus' */		/* bg0 xscroll? */
	
		case 3: /* OUT3 - Page 45 */
			/*	bit0-bit3 = Analog Tachometer output
				bit4 = Palette selector. (Goes to A4 on the color prom). I believe this
					select between colors 0-15 and colors 16-31.
				bit5 = Center Layer 256H line enable. This bit enables/disables the
			   		256H line (The extra higher bit for scroll) on the center (road) layer.
				bit 6 and 7 = unused.
			*/
			break;
	
		case 4: /* OUT4	- Page 46 */
			/* trigger irq on cpu2 when vblank arrives */
			grchamp_cpu_irq_enable[1] = data & 1;
			break;
	
			/* OUT5 - unused */									/* bg1 yscroll lsb */
			/* OUT6 - unused */									/* bg1 yscroll msb */
			/* OUT7 - Page 44: writes to 'Right Synk Bus' */	/* bg1 xscroll? */
	
		case 8: /* OUT8 - Page 47 */
			comm_latch = data;
			break;
	
			/* OUT9 - Page 47: writes to 'Center Synk Bus' */	/* bg2 yscroll lsb */
			/* OUTA - Page 47: writes to 'Center Synk Bus' */	/* bg2 yscroll msb? */
			/* OUTB - Page 47: writes to 'Center Synk Bus' */	/* bg2 xscroll? */
	
		default:
			/* OUTC - Page 48: goes to connector Q-23 */
			/* OUTD - Page 48: goes to connector Q-25 */
			/* OUTE - Page 48: goes to connector Q-27 */
			/* OUTF - unused */
			break;
		}
	} };
}
