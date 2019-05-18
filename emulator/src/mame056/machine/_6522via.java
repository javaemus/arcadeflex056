/**********************************************************************

	Rockwell 6522 VIA interface and emulation

	This function emulates the functionality of up to 8 6522
	versatile interface adapters.

	This is based on the M6821 emulation in MAME.

	To do:

	T2 pulse counting mode
	Pulse mode handshake output
	Shift register

**********************************************************************/

/*
  1999-Dec-22 PeT
   vc20 random number generation only partly working
   (reads (uninitialized) timer 1 and timer 2 counter)
   timer init, reset, read changed
 */

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.machine;

import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.machine._6522viaH.*;
import static arcadeflex056.fucPtr.*;


public class _6522via
{
	
	
	/******************* internal VIA data structure *******************/
	
	public static class via6522
	{
		public static via6522_interface intf;
	
		public static int in_a;
		public static int in_ca1;
		public static int in_ca2;
		public static int out_a;
		public static int out_ca2;
		public static int ddr_a;
	
		public static int in_b;
		public static int in_cb1;
		public static int in_cb2;
		public static int out_b;
		public static int out_cb2;
		public static int ddr_b;
	
		public static int t1cl;
		public static int t1ch;
		public static int t1ll;
		public static int t1lh;
		public static int t2cl;
		public static int t2ch;
		public static int t2ll;
		public static int t2lh;
	
		public static int sr;
		public static int pcr;
		public static int acr;
		public static int ier;
		public static int ifr;
	
		public timer_entry t1 = new timer_entry();
		public static double time1;
		public timer_entry t2 = new timer_entry();
		public static double time2;
	
		public static double cycles_to_sec;
		public static double sec_to_cycles;
	};
/*TODO*///	
/*TODO*///	
/*TODO*///	/******************* convenince macros and defines *******************/
/*TODO*///	
/*TODO*///	#define V_CYCLES_TO_TIME(c) ((double)(c) * v.cycles_to_sec)
/*TODO*///	#define V_TIME_TO_CYCLES(t) ((int)((t) * v.sec_to_cycles))
/*TODO*///	
/*TODO*///	/* Macros for PCR */
/*TODO*///	#define CA1_LOW_TO_HIGH(c)		(c & 0x01)
/*TODO*///	#define CA1_HIGH_TO_LOW(c)		(!(c & 0x01))
/*TODO*///	
/*TODO*///	#define CB1_LOW_TO_HIGH(c)		(c & 0x10)
/*TODO*///	#define CB1_HIGH_TO_LOW(c)		(!(c & 0x10))
/*TODO*///	
/*TODO*///	#define CA2_INPUT(c)			(!(c & 0x08))
/*TODO*///	#define CA2_LOW_TO_HIGH(c)		((c & 0x0c) == 0x04)
/*TODO*///	#define CA2_HIGH_TO_LOW(c)		((c & 0x0c) == 0x00)
/*TODO*///	#define CA2_IND_IRQ(c)			((c & 0x0a) == 0x02)
/*TODO*///	
/*TODO*///	#define CA2_OUTPUT(c)			(c & 0x08)
/*TODO*///	#define CA2_AUTO_HS(c)			((c & 0x0c) == 0x08)
/*TODO*///	#define CA2_HS_OUTPUT(c)		((c & 0x0e) == 0x08)
/*TODO*///	#define CA2_PULSE_OUTPUT(c)		((c & 0x0e) == 0x0a)
/*TODO*///	#define CA2_FIX_OUTPUT(c)		((c & 0x0c) == 0x0c)
/*TODO*///	#define CA2_OUTPUT_LEVEL(c)		((c & 0x02) >> 1)
/*TODO*///	
/*TODO*///	#define CB2_INPUT(c)			(!(c & 0x80))
/*TODO*///	#define CB2_LOW_TO_HIGH(c)		((c & 0xc0) == 0x40)
/*TODO*///	#define CB2_HIGH_TO_LOW(c)		((c & 0xc0) == 0x00)
/*TODO*///	#define CB2_IND_IRQ(c)			((c & 0xa0) == 0x20)
/*TODO*///	
/*TODO*///	#define CB2_OUTPUT(c)			(c & 0x80)
/*TODO*///	#define CB2_AUTO_HS(c)			((c & 0xc0) == 0x80)
/*TODO*///	#define CB2_HS_OUTPUT(c)		((c & 0xe0) == 0x80)
/*TODO*///	#define CB2_PULSE_OUTPUT(c)		((c & 0xe0) == 0xa0)
/*TODO*///	#define CB2_FIX_OUTPUT(c)		((c & 0xc0) == 0xc0)
/*TODO*///	#define CB2_OUTPUT_LEVEL(c)		((c & 0x20) >> 5)
/*TODO*///	
/*TODO*///	/* Macros for ACR */
/*TODO*///	#define PA_LATCH_ENABLE(c)		(c & 0x01)
/*TODO*///	#define PB_LATCH_ENABLE(c)		(c & 0x02)
/*TODO*///	
/*TODO*///	#define SR_DISABLED(c)			(!(c & 0x1c))
/*TODO*///	#define SI_T2_CONTROL(c)		((c & 0x1c) == 0x04)
/*TODO*///	#define SI_O2_CONTROL(c)		((c & 0x1c) == 0x08)
/*TODO*///	#define SI_EXT_CONTROL(c)		((c & 0x1c) == 0x0c)
/*TODO*///	#define SO_T2_RATE(c)			((c & 0x1c) == 0x10)
/*TODO*///	#define SO_T2_CONTROL(c)		((c & 0x1c) == 0x14)
/*TODO*///	#define SO_O2_CONTROL(c)		((c & 0x1c) == 0x18)
/*TODO*///	#define SO_EXT_CONTROL(c)		((c & 0x1c) == 0x1c)
/*TODO*///	
/*TODO*///	#define T1_SET_PB7(c)			(c & 0x80)
/*TODO*///	#define T1_CONTINUOUS(c)		(c & 0x40)
/*TODO*///	#define T2_COUNT_PB6(c)			(c & 0x20)
/*TODO*///	
/*TODO*///	/* Interrupt flags */
/*TODO*///	#define INT_CA2	0x01
/*TODO*///	#define INT_CA1	0x02
/*TODO*///	#define INT_SR	0x04
/*TODO*///	#define INT_CB2	0x08
/*TODO*///	#define INT_CB1	0x10
/*TODO*///	#define INT_T2	0x20
/*TODO*///	#define INT_T1	0x40
/*TODO*///	#define INT_ANY	0x80
/*TODO*///	
/*TODO*///	#define CLR_PA_INT(v)	via_clear_int (v, INT_CA1 | ((!CA2_IND_IRQ(v.pcr)) ? INT_CA2: 0))
/*TODO*///	#define CLR_PB_INT(v)	via_clear_int (v, INT_CB1 | ((!CB2_IND_IRQ(v.pcr)) ? INT_CB2: 0))
/*TODO*///	
/*TODO*///	#define IFR_DELAY 3
/*TODO*///	
/*TODO*///	#define TIMER1_VALUE(v) (v.t1ll+(v.t1lh<<8))
/*TODO*///	#define TIMER2_VALUE(v) (v.t2ll+(v.t2lh<<8))
	
	/******************* static variables *******************/
	
	static via6522[] via = new via6522[MAX_VIA];
	
	/******************* configuration *******************/
/*TODO*///	
/*TODO*///	void via_set_clock(int which,int clock)
/*TODO*///	{
/*TODO*///		via[which].sec_to_cycles = clock;
/*TODO*///		via[which].cycles_to_sec = 1.0 / via[which].sec_to_cycles;
/*TODO*///	}
	
	public static void via_config(int which, via6522_interface intf)
	{
		if (which >= MAX_VIA) return;
		via[which].intf = intf;
		via[which].t1ll = 0xf3; /* via at 0x9110 in vic20 show these values */
		via[which].t1lh = 0xb5; /* ports are not written by kernel! */
		via[which].t2ll = 0xff; /* taken from vice */
		via[which].t2lh = 0xff;
		via[which].time2 = via[which].time1=timer_get_time();
	
		/* Default clock is from CPU1 */
		/*TODO*///via_set_clock (which, Machine.drv.cpu[0].cpu_clock);
	}
	
	
	/******************* reset *******************/
	
	public static void via_reset()
	{
		int i;
		via6522 v;
	
		v = new via6522();
	
		for (i = 0; i < MAX_VIA; i++)
	    {
			v.intf = via[i].intf;
			v.t1ll = via[i].t1ll;
			v.t1lh = via[i].t1lh;
			v.t2ll = via[i].t2ll;
			v.t2lh = via[i].t2lh;
			v.time1 = via[i].time1;
			v.time2 = via[i].time2;
			v.sec_to_cycles = via[i].sec_to_cycles;
			v.cycles_to_sec = via[i].cycles_to_sec;
	
			via[i] = v;
	    }
	}
	
/*TODO*///	/******************* external interrupt check *******************/
/*TODO*///	
/*TODO*///	static void via_set_int (via6522 v, int data)
/*TODO*///	{
/*TODO*///		v.ifr |= data;
/*TODO*///	
/*TODO*///		if ((v.ier & v.ifr) != 0)
/*TODO*///	    {
/*TODO*///			v.ifr |= INT_ANY;
/*TODO*///			if (v.intf.irq_func != null) (v.intf.irq_func).handler(ASSERT_LINE);
/*TODO*///	    }
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void via_clear_int (via6522 v, int data)
/*TODO*///	{
/*TODO*///		v.ifr = (v.ifr & ~data) & 0x7f;
/*TODO*///	
/*TODO*///		if ((v.ifr & v.ier) != 0)
/*TODO*///			v.ifr |= INT_ANY;
/*TODO*///		else
/*TODO*///			if (v.intf.irq_func != null) (v.intf.irq_func).handler(CLEAR_LINE);
/*TODO*///	}
/*TODO*///	
/*TODO*///	/******************* Timer timeouts *************************/
/*TODO*///	static void via_t1_timeout (int which)
/*TODO*///	{
/*TODO*///		via6522 v = via[which];
/*TODO*///	
/*TODO*///	
/*TODO*///		if (T1_CONTINUOUS (v.acr))
/*TODO*///	    {
/*TODO*///			if (T1_SET_PB7(v.acr))
/*TODO*///				v.out_b ^= 0x80;
/*TODO*///			timer_reset (v.t1, V_CYCLES_TO_TIME(TIMER1_VALUE(v) + IFR_DELAY));
/*TODO*///	    }
/*TODO*///		else
/*TODO*///	    {
/*TODO*///			if (T1_SET_PB7(v.acr))
/*TODO*///				v.out_b |= 0x80;
/*TODO*///			v.t1 = null;
/*TODO*///			v.time1=timer_get_time();
/*TODO*///	    }
/*TODO*///		if (v.intf.out_b_func && v.ddr_b)
/*TODO*///			v.intf.out_b_func(0, v.out_b & v.ddr_b);
/*TODO*///	
/*TODO*///		if (!(v.ifr & INT_T1))
/*TODO*///			via_set_int (v, INT_T1);
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void via_t2_timeout (int which)
/*TODO*///	{
/*TODO*///		struct via6522 *v = via + which;
/*TODO*///	
/*TODO*///		if (v.intf.t2_callback)
/*TODO*///			v.intf.t2_callback(timer_timeelapsed(v.t2));
/*TODO*///	
/*TODO*///		v.t2 = 0;
/*TODO*///		v.time2=timer_get_time();
/*TODO*///	
/*TODO*///		if (!(v.ifr & INT_T2))
/*TODO*///			via_set_int (v, INT_T2);
/*TODO*///	}
	
	/******************* CPU interface for VIA read *******************/
	
	public static int via_read(int which, int offset)
	{
/*TODO*///		struct via6522 *v = via + which;
		int val = 0;
/*TODO*///	
/*TODO*///		offset &= 0xf;
/*TODO*///	
/*TODO*///		switch (offset)
/*TODO*///	    {
/*TODO*///	    case VIA_PB:
/*TODO*///			/* update the input */
/*TODO*///			if (PB_LATCH_ENABLE(v.acr) == 0)
/*TODO*///				if (v.intf.in_b_func) v.in_b = v.intf.in_b_func(0);
/*TODO*///	
/*TODO*///			CLR_PB_INT(v);
/*TODO*///	
/*TODO*///			/* combine input and output values, hold DDRB bit 7 high if T1_SET_PB7 */
/*TODO*///			if (T1_SET_PB7(v.acr))
/*TODO*///				val = (v.out_b & (v.ddr_b | 0x80)) | (v.in_b & ~(v.ddr_b | 0x80));
/*TODO*///			else
/*TODO*///				val = (v.out_b & v.ddr_b) + (v.in_b & ~v.ddr_b);
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_PA:
/*TODO*///			/* update the input */
/*TODO*///			if (PA_LATCH_ENABLE(v.acr) == 0)
/*TODO*///				if (v.intf.in_a_func) v.in_a = v.intf.in_a_func(0);
/*TODO*///	
/*TODO*///			/* combine input and output values */
/*TODO*///			val = (v.out_a & v.ddr_a) + (v.in_a & ~v.ddr_a);
/*TODO*///	
/*TODO*///			CLR_PA_INT(v);
/*TODO*///	
/*TODO*///			/* If CA2 is configured as output and in pulse or handshake mode,
/*TODO*///			   CA2 is set now */
/*TODO*///			if (CA2_AUTO_HS(v.pcr))
/*TODO*///			{
/*TODO*///				if (v.out_ca2)
/*TODO*///				{
/*TODO*///					/* set CA2 */
/*TODO*///					v.out_ca2 = 0;
/*TODO*///	
/*TODO*///					/* call the CA2 output function */
/*TODO*///					if (v.intf.out_ca2_func) v.intf.out_ca2_func(0, 0);
/*TODO*///				}
/*TODO*///			}
/*TODO*///	
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_PANH:
/*TODO*///			/* update the input */
/*TODO*///			if (PA_LATCH_ENABLE(v.acr) == 0)
/*TODO*///				if (v.intf.in_a_func) v.in_a = v.intf.in_a_func(0);
/*TODO*///	
/*TODO*///			/* combine input and output values */
/*TODO*///			val = (v.out_a & v.ddr_a) + (v.in_a & ~v.ddr_a);
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_DDRB:
/*TODO*///			val = v.ddr_b;
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_DDRA:
/*TODO*///			val = v.ddr_a;
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_T1CL:
/*TODO*///			via_clear_int (v, INT_T1);
/*TODO*///			if (v.t1)
/*TODO*///				val = V_TIME_TO_CYCLES(timer_timeleft(v.t1)) & 0xff;
/*TODO*///			else
/*TODO*///			{
/*TODO*///				if ( T1_CONTINUOUS(v.acr) )
/*TODO*///				{
/*TODO*///					val = (TIMER1_VALUE(v)-
/*TODO*///						   (V_TIME_TO_CYCLES(timer_get_time()-v.time1)
/*TODO*///							%TIMER1_VALUE(v))-1)&0xff;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					val = (0x10000-
/*TODO*///						   (V_TIME_TO_CYCLES(timer_get_time()-v.time1)&0xffff)
/*TODO*///						   -1)&0xff;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_T1CH:
/*TODO*///			if (v.t1)
/*TODO*///				val = V_TIME_TO_CYCLES(timer_timeleft(v.t1)) >> 8;
/*TODO*///			else
/*TODO*///			{
/*TODO*///				if ( T1_CONTINUOUS(v.acr) )
/*TODO*///				{
/*TODO*///					val = (TIMER1_VALUE(v)-
/*TODO*///						   (V_TIME_TO_CYCLES(timer_get_time()-v.time1)
/*TODO*///							%TIMER1_VALUE(v))-1)>>8;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					val = (0x10000-
/*TODO*///						   (V_TIME_TO_CYCLES(timer_get_time()-v.time1)&0xffff)
/*TODO*///						   -1)>>8;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_T1LL:
/*TODO*///			val = v.t1ll;
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_T1LH:
/*TODO*///			val = v.t1lh;
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_T2CL:
/*TODO*///			via_clear_int (v, INT_T2);
/*TODO*///			if (v.t2)
/*TODO*///				val = V_TIME_TO_CYCLES(timer_timeleft(v.t2)) & 0xff;
/*TODO*///			else
/*TODO*///			{
/*TODO*///				if (T2_COUNT_PB6(v.acr))
/*TODO*///				{
/*TODO*///					val = v.t2cl;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					val = (0x10000-
/*TODO*///						   (V_TIME_TO_CYCLES(timer_get_time()-v.time2)&0xffff)
/*TODO*///						   -1)&0xff;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_T2CH:
/*TODO*///			if (v.t2)
/*TODO*///				val = V_TIME_TO_CYCLES(timer_timeleft(v.t2)) >> 8;
/*TODO*///			else
/*TODO*///			{
/*TODO*///				if (T2_COUNT_PB6(v.acr))
/*TODO*///				{
/*TODO*///					val = v.t2ch;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					val = (0x10000-
/*TODO*///						   (V_TIME_TO_CYCLES(timer_get_time()-v.time2)&0xffff)
/*TODO*///						   -1)>>8;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_SR:
/*TODO*///			val = v.sr;
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_PCR:
/*TODO*///			val = v.pcr;
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_ACR:
/*TODO*///			val = v.acr;
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_IER:
/*TODO*///			val = v.ier | 0x80;
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_IFR:
/*TODO*///			val = v.ifr;
/*TODO*///			break;
/*TODO*///	    }
		return val;
	}
	
	
	/******************* CPU interface for VIA write *******************/
	
	public static void via_write(int which, int offset, int data)
	{
/*TODO*///		struct via6522 *v = via + which;
/*TODO*///	
/*TODO*///		offset &=0x0f;
/*TODO*///		switch (offset)
/*TODO*///	    {
/*TODO*///	    case VIA_PB:
/*TODO*///			if (T1_SET_PB7(v.acr))
/*TODO*///				v.out_b = (v.out_b & 0x80) | (data  & 0x7f);
/*TODO*///			else
/*TODO*///				v.out_b = data;
/*TODO*///	
/*TODO*///			if (v.intf.out_b_func && v.ddr_b)
/*TODO*///				v.intf.out_b_func(0, v.out_b & v.ddr_b);
/*TODO*///	
/*TODO*///			CLR_PB_INT(v);
/*TODO*///	
/*TODO*///			/* If CB2 is configured as output and in pulse or handshake mode,
/*TODO*///			   CB2 is set now */
/*TODO*///			if (CB2_AUTO_HS(v.pcr))
/*TODO*///			{
/*TODO*///				if (v.out_cb2)
/*TODO*///				{
/*TODO*///					/* set CB2 */
/*TODO*///					v.out_cb2 = 0;
/*TODO*///	
/*TODO*///					/* call the CB2 output function */
/*TODO*///					if (v.intf.out_cb2_func) v.intf.out_cb2_func(0, 0);
/*TODO*///				}
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_PA:
/*TODO*///			v.out_a = data;
/*TODO*///			if (v.intf.out_a_func && v.ddr_a)
/*TODO*///				v.intf.out_a_func(0, v.out_a & v.ddr_a);
/*TODO*///	
/*TODO*///			CLR_PA_INT(v);
/*TODO*///	
/*TODO*///			/* If CA2 is configured as output and in pulse or handshake mode,
/*TODO*///			   CA2 is set now */
/*TODO*///			if (CA2_AUTO_HS(v.pcr))
/*TODO*///			{
/*TODO*///				if (v.out_ca2)
/*TODO*///				{
/*TODO*///					/* set CA2 */
/*TODO*///					v.out_ca2 = 0;
/*TODO*///	
/*TODO*///					/* call the CA2 output function */
/*TODO*///					if (v.intf.out_ca2_func) v.intf.out_ca2_func(0, 0);
/*TODO*///				}
/*TODO*///			}
/*TODO*///	
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_PANH:
/*TODO*///			v.out_a = data;
/*TODO*///			if (v.intf.out_a_func && v.ddr_a)
/*TODO*///				v.intf.out_a_func(0, v.out_a & v.ddr_a);
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_DDRB:
/*TODO*///	    	/* EHC 03/04/2000 - If data direction changed, present output on the lines */
/*TODO*///	    	if ( data != v.ddr_b ) {
/*TODO*///				v.ddr_b = data;
/*TODO*///	
/*TODO*///				if (v.intf.out_b_func && v.ddr_b)
/*TODO*///					v.intf.out_b_func(0, v.out_b & v.ddr_b);
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_DDRA:
/*TODO*///	    	/* EHC 03/04/2000 - If data direction changed, present output on the lines */
/*TODO*///	    	if ( data != v.ddr_a ) {
/*TODO*///				v.ddr_a = data;
/*TODO*///	
/*TODO*///				if (v.intf.out_a_func && v.ddr_a)
/*TODO*///					v.intf.out_a_func(0, v.out_a & v.ddr_a);
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_T1CL:
/*TODO*///	    case VIA_T1LL:
/*TODO*///			v.t1ll = data;
/*TODO*///			break;
/*TODO*///	
/*TODO*///		case VIA_T1LH:
/*TODO*///		    v.t1lh = data;
/*TODO*///		    via_clear_int (v, INT_T1);
/*TODO*///		    break;
/*TODO*///	
/*TODO*///	    case VIA_T1CH:
/*TODO*///			v.t1ch = v.t1lh = data;
/*TODO*///			v.t1cl = v.t1ll;
/*TODO*///	
/*TODO*///			via_clear_int (v, INT_T1);
/*TODO*///	
/*TODO*///			if (T1_SET_PB7(v.acr))
/*TODO*///			{
/*TODO*///				v.out_b &= 0x7f;
/*TODO*///				if (v.intf.out_b_func && v.ddr_b)
/*TODO*///					v.intf.out_b_func(0, v.out_b & v.ddr_b);
/*TODO*///			}
/*TODO*///			if (v.t1)
/*TODO*///				timer_reset (v.t1, V_CYCLES_TO_TIME(TIMER1_VALUE(v) + IFR_DELAY));
/*TODO*///			else
/*TODO*///				v.t1 = timer_set (V_CYCLES_TO_TIME(TIMER1_VALUE(v) + IFR_DELAY), which, via_t1_timeout);
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_T2CL:
/*TODO*///			v.t2ll = data;
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_T2CH:
/*TODO*///			v.t2ch = v.t2lh = data;
/*TODO*///			v.t2cl = v.t2ll;
/*TODO*///	
/*TODO*///			via_clear_int (v, INT_T2);
/*TODO*///	
/*TODO*///			if (!T2_COUNT_PB6(v.acr))
/*TODO*///			{
/*TODO*///				if (v.t2)
/*TODO*///				{
/*TODO*///					if (v.intf.t2_callback)
/*TODO*///						v.intf.t2_callback(timer_timeelapsed(v.t2));
/*TODO*///					timer_reset (v.t2, V_CYCLES_TO_TIME(TIMER2_VALUE(v) + IFR_DELAY));
/*TODO*///				}
/*TODO*///				else
/*TODO*///					v.t2 = timer_set (V_CYCLES_TO_TIME(TIMER2_VALUE(v) + IFR_DELAY),
/*TODO*///									   which, via_t2_timeout);
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				v.time2=timer_get_time();
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_SR:
/*TODO*///			v.sr = data;
/*TODO*///			if (v.intf.out_shift_func && SO_O2_CONTROL(v.acr))
/*TODO*///				v.intf.out_shift_func(data);
/*TODO*///			/* kludge for Mac Plus (and 128k, 512k, 512ke) : */
/*TODO*///			if (v.intf.out_shift_func2 && SO_EXT_CONTROL(v.acr))
/*TODO*///			{
/*TODO*///				v.intf.out_shift_func2(data);
/*TODO*///				via_set_int(v, INT_SR);
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_PCR:
/*TODO*///			v.pcr = data;
/*TODO*///	
/*TODO*///			if (CA2_FIX_OUTPUT(data) && CA2_OUTPUT_LEVEL(data) ^ v.out_ca2)
/*TODO*///			{
/*TODO*///				v.out_ca2 = CA2_OUTPUT_LEVEL(data);
/*TODO*///				if (v.intf.out_ca2_func)
/*TODO*///					v.intf.out_ca2_func(0, v.out_ca2);
/*TODO*///			}
/*TODO*///	
/*TODO*///			if (CB2_FIX_OUTPUT(data) && CB2_OUTPUT_LEVEL(data) ^ v.out_cb2)
/*TODO*///			{
/*TODO*///				v.out_cb2 = CB2_OUTPUT_LEVEL(data);
/*TODO*///				if (v.intf.out_cb2_func)
/*TODO*///					v.intf.out_cb2_func(0, v.out_cb2);
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///	    case VIA_ACR:
/*TODO*///			v.acr = data;
/*TODO*///			if (T1_SET_PB7(v.acr))
/*TODO*///			{
/*TODO*///				if (v.t1)
/*TODO*///					v.out_b &= ~0x80;
/*TODO*///				else
/*TODO*///					v.out_b |= 0x80;
/*TODO*///	
/*TODO*///				if (v.intf.out_b_func && v.ddr_b)
/*TODO*///					v.intf.out_b_func(0, v.out_b & v.ddr_b);
/*TODO*///			}
/*TODO*///			if (T1_CONTINUOUS(data))
/*TODO*///			{
/*TODO*///				if (v.t1)
/*TODO*///					timer_reset (v.t1, V_CYCLES_TO_TIME(TIMER1_VALUE(v) + IFR_DELAY));
/*TODO*///				else
/*TODO*///					v.t1 = timer_set (V_CYCLES_TO_TIME(TIMER1_VALUE(v) + IFR_DELAY), which, via_t1_timeout);
/*TODO*///			}
/*TODO*///			/* kludge for Mac Plus (and 128k, 512k, 512ke) : */
/*TODO*///			if (v.intf.si_ready_func && SI_EXT_CONTROL(data))
/*TODO*///				v.intf.si_ready_func();
/*TODO*///			break;
/*TODO*///	
/*TODO*///		case VIA_IER:
/*TODO*///			if (data & 0x80)
/*TODO*///				v.ier |= data & 0x7f;
/*TODO*///			else
/*TODO*///				v.ier &= ~(data & 0x7f);
/*TODO*///	
/*TODO*///			if (v.ifr & INT_ANY)
/*TODO*///			{
/*TODO*///				if (((v.ifr & v.ier) & 0x7f) == 0)
/*TODO*///				{
/*TODO*///					v.ifr &= ~INT_ANY;
/*TODO*///					if (v.intf.irq_func)
/*TODO*///						(*v.intf.irq_func)(CLEAR_LINE);
/*TODO*///				}
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				if ((v.ier & v.ifr) & 0x7f)
/*TODO*///				{
/*TODO*///					v.ifr |= INT_ANY;
/*TODO*///					if (v.intf.irq_func)
/*TODO*///						(*v.intf.irq_func)(ASSERT_LINE);
/*TODO*///				}
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///		case VIA_IFR:
/*TODO*///			if (data & INT_ANY)
/*TODO*///				data = 0x7f;
/*TODO*///			via_clear_int (v, data);
/*TODO*///			break;
/*TODO*///	    }
	}
	
/*TODO*///	/******************* interface setting VIA port A input *******************/
/*TODO*///	
/*TODO*///	void via_set_input_a(int which, int data)
/*TODO*///	{
/*TODO*///		struct via6522 *v = via + which;
/*TODO*///	
/*TODO*///		/* set the input, what could be easier? */
/*TODO*///		v.in_a = data;
/*TODO*///	}
	
	/******************* interface setting VIA port CA1 input *******************/
	
	public static void via_set_input_ca1(int which, int data)
	{
/*TODO*///		struct via6522 *v = via + which;
/*TODO*///	
/*TODO*///		/* limit the data to 0 or 1 */
/*TODO*///		data = data ? 1 : 0;
/*TODO*///	
/*TODO*///		/* handle the active transition */
/*TODO*///		if (data != v.in_ca1)
/*TODO*///	    {
/*TODO*///			if ((CA1_LOW_TO_HIGH(v.pcr) && data) || (CA1_HIGH_TO_LOW(v.pcr) && !data))
/*TODO*///			{
/*TODO*///				if (PA_LATCH_ENABLE(v.acr))
/*TODO*///					if (v.intf.in_a_func) v.in_a = v.intf.in_a_func(0);
/*TODO*///				via_set_int (v, INT_CA1);
/*TODO*///	
/*TODO*///				/* CA2 is configured as output and in pulse or handshake mode,
/*TODO*///				   CA2 is cleared now */
/*TODO*///				if (CA2_AUTO_HS(v.pcr))
/*TODO*///				{
/*TODO*///					if (!v.out_ca2)
/*TODO*///					{
/*TODO*///						/* clear CA2 */
/*TODO*///						v.out_ca2 = 1;
/*TODO*///	
/*TODO*///						/* call the CA2 output function */
/*TODO*///						if (v.intf.out_ca2_func) v.intf.out_ca2_func(0, 1);
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///			v.in_ca1 = data;
/*TODO*///	    }
	}
	
	/******************* interface setting VIA port CA2 input *******************/
	
	public static void via_set_input_ca2(int which, int data)
	{
/*TODO*///		struct via6522 *v = via + which;
/*TODO*///	
/*TODO*///		/* limit the data to 0 or 1 */
/*TODO*///		data = data ? 1 : 0;
/*TODO*///	
/*TODO*///		/* CA2 is in input mode */
/*TODO*///		if (CA2_INPUT(v.pcr))
/*TODO*///	    {
/*TODO*///			/* the new state has caused a transition */
/*TODO*///			if (v.in_ca2 != data)
/*TODO*///			{
/*TODO*///				/* handle the active transition */
/*TODO*///				if ((data && CA2_LOW_TO_HIGH(v.pcr)) || (!data && CA2_HIGH_TO_LOW(v.pcr)))
/*TODO*///				{
/*TODO*///					/* mark the IRQ */
/*TODO*///					via_set_int (v, INT_CA2);
/*TODO*///				}
/*TODO*///				/* set the new value for CA2 */
/*TODO*///				v.in_ca2 = data;
/*TODO*///			}
/*TODO*///	    }
/*TODO*///	
/*TODO*///	
	}
	
	
	
/*TODO*///	/******************* interface setting VIA port B input *******************/
/*TODO*///	
/*TODO*///	void via_set_input_b(int which, int data)
/*TODO*///	{
/*TODO*///		struct via6522 *v = via + which;
/*TODO*///	
/*TODO*///		/* set the input, what could be easier? */
/*TODO*///		v.in_b = data;
/*TODO*///	}
	
	
	
	/******************* interface setting VIA port CB1 input *******************/
	
	public static void via_set_input_cb1(int which, int data)
	{
/*TODO*///		struct via6522 *v = via + which;
/*TODO*///	
/*TODO*///		/* limit the data to 0 or 1 */
/*TODO*///		data = data ? 1 : 0;
/*TODO*///	
/*TODO*///		/* handle the active transition */
/*TODO*///		if (data != v.in_cb1)
/*TODO*///	    {
/*TODO*///			if ((CB1_LOW_TO_HIGH(v.pcr) && data) || (CB1_HIGH_TO_LOW(v.pcr) && !data))
/*TODO*///			{
/*TODO*///				if (PB_LATCH_ENABLE(v.acr))
/*TODO*///					if (v.intf.in_b_func) v.in_b = v.intf.in_b_func(0);
/*TODO*///				via_set_int (v, INT_CB1);
/*TODO*///	
/*TODO*///				/* CB2 is configured as output and in pulse or handshake mode,
/*TODO*///				   CB2 is cleared now */
/*TODO*///				if (CB2_AUTO_HS(v.pcr))
/*TODO*///				{
/*TODO*///					if (!v.out_cb2)
/*TODO*///					{
/*TODO*///						/* clear CB2 */
/*TODO*///						v.out_cb2 = 1;
/*TODO*///	
/*TODO*///						/* call the CB2 output function */
/*TODO*///						if (v.intf.out_cb2_func) v.intf.out_cb2_func(0, 1);
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///			v.in_cb1 = data;
/*TODO*///	    }
	}
	
/*TODO*///	/******************* interface setting VIA port CB2 input *******************/
/*TODO*///	
/*TODO*///	void via_set_input_cb2(int which, int data)
/*TODO*///	{
/*TODO*///		struct via6522 *v = via + which;
/*TODO*///	
/*TODO*///		/* limit the data to 0 or 1 */
/*TODO*///		data = data ? 1 : 0;
/*TODO*///	
/*TODO*///		/* CB2 is in input mode */
/*TODO*///		if (CB2_INPUT(v.pcr))
/*TODO*///	    {
/*TODO*///			/* the new state has caused a transition */
/*TODO*///			if (v.in_cb2 != data)
/*TODO*///			{
/*TODO*///				/* handle the active transition */
/*TODO*///				if ((data && CB2_LOW_TO_HIGH(v.pcr)) || (!data && CB2_HIGH_TO_LOW(v.pcr)))
/*TODO*///				{
/*TODO*///					/* mark the IRQ */
/*TODO*///					via_set_int (v, INT_CB2);
/*TODO*///				}
/*TODO*///				/* set the new value for CB2 */
/*TODO*///				v.in_cb2 = data;
/*TODO*///			}
/*TODO*///	    }
/*TODO*///	}
/*TODO*///	
/*TODO*///	/******************* interface to shift data into VIA ***********************/
/*TODO*///	
/*TODO*///	/* kludge for Mac Plus (and 128k, 512k, 512ke) : */
/*TODO*///	
/*TODO*///	void via_set_input_si(int which, int data)
/*TODO*///	{
/*TODO*///		struct via6522 *v = via + which;
/*TODO*///	
/*TODO*///		via_set_int(v, INT_SR);
/*TODO*///		v.sr = data;
/*TODO*///	}
	
	/******************* Standard 8-bit CPU interfaces, D0-D7 *******************/
	
	public static ReadHandlerPtr via_0_r  = new ReadHandlerPtr() { public int handler(int offset) { return via_read(0, offset); } };
	public static ReadHandlerPtr via_1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via_read(1, offset); } };
	public static ReadHandlerPtr via_2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via_read(2, offset); } };
	public static ReadHandlerPtr via_3_r  = new ReadHandlerPtr() { public int handler(int offset) { return via_read(3, offset); } };
	public static ReadHandlerPtr via_4_r  = new ReadHandlerPtr() { public int handler(int offset) { return via_read(4, offset); } };
	public static ReadHandlerPtr via_5_r  = new ReadHandlerPtr() { public int handler(int offset) { return via_read(5, offset); } };
	public static ReadHandlerPtr via_6_r  = new ReadHandlerPtr() { public int handler(int offset) { return via_read(6, offset); } };
	public static ReadHandlerPtr via_7_r  = new ReadHandlerPtr() { public int handler(int offset) { return via_read(7, offset); } };
	
	public static WriteHandlerPtr via_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_write(0, offset, data); } };
	public static WriteHandlerPtr via_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_write(1, offset, data); } };
	public static WriteHandlerPtr via_2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_write(2, offset, data); } };
	public static WriteHandlerPtr via_3_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_write(3, offset, data); } };
	public static WriteHandlerPtr via_4_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_write(4, offset, data); } };
	public static WriteHandlerPtr via_5_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_write(5, offset, data); } };
	public static WriteHandlerPtr via_6_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_write(6, offset, data); } };
	public static WriteHandlerPtr via_7_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_write(7, offset, data); } };
	
/*TODO*///	/******************* 8-bit A/B port interfaces *******************/
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr via_0_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_a(0, data); } };
/*TODO*///	public static WriteHandlerPtr via_1_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_a(1, data); } };
/*TODO*///	public static WriteHandlerPtr via_2_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_a(2, data); } };
/*TODO*///	public static WriteHandlerPtr via_3_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_a(3, data); } };
/*TODO*///	public static WriteHandlerPtr via_4_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_a(4, data); } };
/*TODO*///	public static WriteHandlerPtr via_5_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_a(5, data); } };
/*TODO*///	public static WriteHandlerPtr via_6_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_a(6, data); } };
/*TODO*///	public static WriteHandlerPtr via_7_porta_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_a(7, data); } };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr via_0_portb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_b(0, data); } };
/*TODO*///	public static WriteHandlerPtr via_1_portb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_b(1, data); } };
/*TODO*///	public static WriteHandlerPtr via_2_portb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_b(2, data); } };
/*TODO*///	public static WriteHandlerPtr via_3_portb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_b(3, data); } };
/*TODO*///	public static WriteHandlerPtr via_4_portb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_b(4, data); } };
/*TODO*///	public static WriteHandlerPtr via_5_portb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_b(5, data); } };
/*TODO*///	public static WriteHandlerPtr via_6_portb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_b(6, data); } };
/*TODO*///	public static WriteHandlerPtr via_7_portb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_b(7, data); } };
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr via_0_porta_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[0].in_a; } };
/*TODO*///	public static ReadHandlerPtr via_1_porta_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[1].in_a; } };
/*TODO*///	public static ReadHandlerPtr via_2_porta_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[2].in_a; } };
/*TODO*///	public static ReadHandlerPtr via_3_porta_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[3].in_a; } };
/*TODO*///	public static ReadHandlerPtr via_4_porta_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[4].in_a; } };
/*TODO*///	public static ReadHandlerPtr via_5_porta_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[5].in_a; } };
/*TODO*///	public static ReadHandlerPtr via_6_porta_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[6].in_a; } };
/*TODO*///	public static ReadHandlerPtr via_7_porta_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[7].in_a; } };
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr via_0_portb_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[0].in_b; } };
/*TODO*///	public static ReadHandlerPtr via_1_portb_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[1].in_b; } };
/*TODO*///	public static ReadHandlerPtr via_2_portb_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[2].in_b; } };
/*TODO*///	public static ReadHandlerPtr via_3_portb_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[3].in_b; } };
/*TODO*///	public static ReadHandlerPtr via_4_portb_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[4].in_b; } };
/*TODO*///	public static ReadHandlerPtr via_5_portb_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[5].in_b; } };
/*TODO*///	public static ReadHandlerPtr via_6_portb_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[6].in_b; } };
/*TODO*///	public static ReadHandlerPtr via_7_portb_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[7].in_b; } };
/*TODO*///	
	/******************* 1-bit CA1/CA2/CB1/CB2 port interfaces *******************/
	
	public static WriteHandlerPtr via_0_ca1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca1(0, data); } };
	public static WriteHandlerPtr via_1_ca1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca1(1, data); } };
	public static WriteHandlerPtr via_2_ca1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca1(2, data); } };
	public static WriteHandlerPtr via_3_ca1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca1(3, data); } };
	public static WriteHandlerPtr via_4_ca1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca1(4, data); } };
	public static WriteHandlerPtr via_5_ca1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca1(5, data); } };
	public static WriteHandlerPtr via_6_ca1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca1(6, data); } };
	public static WriteHandlerPtr via_7_ca1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca1(7, data); } };
	public static WriteHandlerPtr via_0_ca2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca2(0, data); } };
	public static WriteHandlerPtr via_1_ca2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca2(1, data); } };
	public static WriteHandlerPtr via_2_ca2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca2(2, data); } };
	public static WriteHandlerPtr via_3_ca2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca2(3, data); } };
	public static WriteHandlerPtr via_4_ca2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca2(4, data); } };
	public static WriteHandlerPtr via_5_ca2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca2(5, data); } };
	public static WriteHandlerPtr via_6_ca2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca2(6, data); } };
	public static WriteHandlerPtr via_7_ca2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_ca2(7, data); } };
	
	public static WriteHandlerPtr via_0_cb1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb1(0, data); } };
/*TODO*///	public static WriteHandlerPtr via_1_cb1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb1(1, data); } };
/*TODO*///	public static WriteHandlerPtr via_2_cb1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb1(2, data); } };
/*TODO*///	public static WriteHandlerPtr via_3_cb1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb1(3, data); } };
/*TODO*///	public static WriteHandlerPtr via_4_cb1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb1(4, data); } };
/*TODO*///	public static WriteHandlerPtr via_5_cb1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb1(5, data); } };
/*TODO*///	public static WriteHandlerPtr via_6_cb1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb1(6, data); } };
/*TODO*///	public static WriteHandlerPtr via_7_cb1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb1(7, data); } };
/*TODO*///	public static WriteHandlerPtr via_0_cb2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb2(0, data); } };
/*TODO*///	public static WriteHandlerPtr via_1_cb2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb2(1, data); } };
/*TODO*///	public static WriteHandlerPtr via_2_cb2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb2(2, data); } };
/*TODO*///	public static WriteHandlerPtr via_3_cb2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb2(3, data); } };
/*TODO*///	public static WriteHandlerPtr via_4_cb2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb2(4, data); } };
/*TODO*///	public static WriteHandlerPtr via_5_cb2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb2(5, data); } };
/*TODO*///	public static WriteHandlerPtr via_6_cb2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb2(6, data); } };
/*TODO*///	public static WriteHandlerPtr via_7_cb2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { via_set_input_cb2(7, data); } };
	
	public static ReadHandlerPtr via_0_ca1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[0].in_ca1; } };
	public static ReadHandlerPtr via_1_ca1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[1].in_ca1; } };
	public static ReadHandlerPtr via_2_ca1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[2].in_ca1; } };
	public static ReadHandlerPtr via_3_ca1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[3].in_ca1; } };
	public static ReadHandlerPtr via_4_ca1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[4].in_ca1; } };
	public static ReadHandlerPtr via_5_ca1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[5].in_ca1; } };
	public static ReadHandlerPtr via_6_ca1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[6].in_ca1; } };
	public static ReadHandlerPtr via_7_ca1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[7].in_ca1; } };
	public static ReadHandlerPtr via_0_ca2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[0].in_ca2; } };
	public static ReadHandlerPtr via_1_ca2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[1].in_ca2; } };
	public static ReadHandlerPtr via_2_ca2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[2].in_ca2; } };
	public static ReadHandlerPtr via_3_ca2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[3].in_ca2; } };
	public static ReadHandlerPtr via_4_ca2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[4].in_ca2; } };
	public static ReadHandlerPtr via_5_ca2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[5].in_ca2; } };
	public static ReadHandlerPtr via_6_ca2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[6].in_ca2; } };
	public static ReadHandlerPtr via_7_ca2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[7].in_ca2; } };
	
	public static ReadHandlerPtr via_0_cb1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[0].in_cb1; } };
	public static ReadHandlerPtr via_1_cb1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[1].in_cb1; } };
	public static ReadHandlerPtr via_2_cb1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[2].in_cb1; } };
	public static ReadHandlerPtr via_3_cb1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[3].in_cb1; } };
	public static ReadHandlerPtr via_4_cb1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[4].in_cb1; } };
	public static ReadHandlerPtr via_5_cb1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[5].in_cb1; } };
	public static ReadHandlerPtr via_6_cb1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[6].in_cb1; } };
	public static ReadHandlerPtr via_7_cb1_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[7].in_cb1; } };
	public static ReadHandlerPtr via_0_cb2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[0].in_cb2; } };
	public static ReadHandlerPtr via_1_cb2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[1].in_cb2; } };
	public static ReadHandlerPtr via_2_cb2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[2].in_cb2; } };
	public static ReadHandlerPtr via_3_cb2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[3].in_cb2; } };
	public static ReadHandlerPtr via_4_cb2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[4].in_cb2; } };
	public static ReadHandlerPtr via_5_cb2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[5].in_cb2; } };
	public static ReadHandlerPtr via_6_cb2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[6].in_cb2; } };
	public static ReadHandlerPtr via_7_cb2_r  = new ReadHandlerPtr() { public int handler(int offset) { return via[7].in_cb2; } };
}
