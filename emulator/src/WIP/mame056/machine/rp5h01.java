/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static WIP.mame056.machine.rp5h01H.*;
// refactor
import static arcadeflex036.osdepend.logerror;
import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.common.*;
import static mame056.commonH.*;

public class rp5h01
{
	
	/****************************************************************************/
	
	/* local copy of the interface pointer */
	public static RP5H01_interface intf;
	
	/* these also work as the address masks */
	public static int COUNTER_MODE_6_BITS = 0x3f;
	public static int COUNTER_MODE_7_BITS = 0x7f;
	
	
	public static class RP5H01 {
		public int counter;
		public int counter_mode;	/* test pin */
		public int enabled;		/* chip enable */
		public int old_reset;		/* reset pin state (level-triggered) */
		public int old_clock;		/* clock pin state (level-triggered) */
		public UBytePtr data = new UBytePtr();
	};
	
	static RP5H01[] RP5H01_state=new RP5H01[MAX_RP5H01];
	
	/****************************************************************************/
	
	public static int RP5H01_init( RP5H01_interface _interface ) {
		int i;
	
		/* setup our local copy of the interface */
		intf = _interface;
	
		if ( intf.num > MAX_RP5H01 ) {
			logerror( "Requested number of RP5H01's is bigger than the supported amount\n" );
			return -1;
		}
	
		/* initialize the state */
		for( i = 0; i < intf.num; i++ ) {
                        RP5H01_state[i] = new RP5H01();
			RP5H01_state[i].counter = 0;
			RP5H01_state[i].counter_mode = COUNTER_MODE_6_BITS;
			RP5H01_state[i].data = new UBytePtr( memory_region( intf.region[i] ),  intf.offset[i] );
			RP5H01_state[i].enabled = 0;
			RP5H01_state[i].old_reset = -1;
			RP5H01_state[i].old_clock = -1;
		}
	
		return 0;
	}
	
	/****************************************************************************/
	
	public static void RP5H01_enable_w( int which, int data ) {
		RP5H01	chip;
	
		if ( which >= intf.num ) {
			logerror( "RP5H01_enable: trying to access an unmapped chip\n" );
			return;
		}
	
		/* get the chip */
		chip = RP5H01_state[which];
	
		/* process the /CE signal and enable/disable the IC */
		chip.enabled = ( data == 0 ) ? 1 : 0;
	}
	
	public static void RP5H01_reset_w( int which, int data ) {
		RP5H01	chip;
		int		newstate = ( data == 0 ) ? 0 : 1;
	
		if ( which >= intf.num ) {
			logerror( "RP5H01_enable: trying to access an unmapped chip\n" );
			return;
		}
	
		/* get the chip */
		chip = RP5H01_state[which];
	
		/* if it's not enabled, ignore */
		if ( chip.enabled == 0 )
			return;
	
		/* now look for a 0.1 transition */
		if ( chip.old_reset == 0 && newstate == 1 ) {
			/* reset the counter */
			chip.counter = 0;
		}
	
		/* update the pin */
		chip.old_reset = newstate;
	}
	
	public static void RP5H01_clock_w( int which, int data ) {
		RP5H01	chip;
		int		newstate = ( data == 0 ) ? 0 : 1;
	
		if ( which >= intf.num ) {
			logerror( "RP5H01_enable: trying to access an unmapped chip\n" );
			return;
		}
	
		/* get the chip */
		chip = RP5H01_state[which];
	
		/* if it's not enabled, ignore */
		if ( chip.enabled == 0 )
			return;
	
		/* now look for a 1.0 transition */
		if ( chip.old_clock == 1 && newstate == 0 ) {
			/* increment the counter, and mask it with the mode */
			chip.counter++;
		}
	
		/* update the pin */
		chip.old_clock = newstate;
	}
	
	public static void RP5H01_test_w( int which, int data ) {
		RP5H01	chip;
	
		if ( which >= intf.num ) {
			logerror( "RP5H01_enable: trying to access an unmapped chip\n" );
			return;
		}
	
		/* get the chip */
		chip = RP5H01_state[which];
	
		/* if it's not enabled, ignore */
		if ( chip.enabled == 0)
			return;
	
		/* process the test signal and change the counter mode */
		chip.counter_mode = ( data == 0 ) ? COUNTER_MODE_6_BITS : COUNTER_MODE_7_BITS;
	}
	
	public static int RP5H01_counter_r( int which ) {
		RP5H01	chip;
	
		if ( which >= intf.num ) {
			logerror( "RP5H01_enable: trying to access an unmapped chip\n" );
			return 0;
		}
	
		/* get the chip */
		chip = RP5H01_state[which];
	
		/* if it's not enabled, ignore */
		if ( chip.enabled == 0)
			return 0; /* ? (should be high impedance) */
	
		/* return A5 */
		return ( chip.counter >> 5 ) & 1;
	}
	
	public static int RP5H01_data_r( int which ) {
		RP5H01	chip;
		int		_byte, bit;
	
		if ( which >= intf.num ) {
			logerror( "RP5H01_enable: trying to access an unmapped chip\n" );
			return 0;
		}
	
		/* get the chip */
		chip = RP5H01_state[which];
	
		/* if it's not enabled, ignore */
		if ( chip.enabled == 0)
			return 0; /* ? (should be high impedance) */
	
		/* get the byte offset and bit offset */
		_byte = ( chip.counter & chip.counter_mode) >> 3;
		bit = 7 - ( chip.counter & 7 );
	
		/* return the data */
		return ( chip.data.read(_byte) >> bit ) & 1;
	}
	
	/****************************************************************************/
	
	public static WriteHandlerPtr RP5H01_0_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		RP5H01_enable_w( 0, data );
	} };
	
	public static WriteHandlerPtr RP5H01_0_reset_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		RP5H01_reset_w( 0, data );
	} };
	
	public static WriteHandlerPtr RP5H01_0_clock_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		RP5H01_clock_w( 0, data );
	} };
	
	public static WriteHandlerPtr RP5H01_0_test_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		RP5H01_test_w( 0, data );
	} };
	
	public static ReadHandlerPtr RP5H01_0_counter_r  = new ReadHandlerPtr() { public int handler(int offset) {
		return RP5H01_counter_r( 0 );
	} };
	
	public static ReadHandlerPtr RP5H01_0_data_r  = new ReadHandlerPtr() { public int handler(int offset) {
		return RP5H01_data_r( 0 );
	} };
}
