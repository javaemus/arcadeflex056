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
import static mame056.cpuintrfH.*;
import static arcadeflex056.fucPtr.*;
import static mame056.mame.*;

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
	
	
	/******************* convenince macros and defines *******************/
	
        public static double V_CYCLES_TO_TIME(int c, via6522 v){
            return ((double)(c) * v.cycles_to_sec);
        }
        public static int V_TIME_TO_CYCLES(int t, via6522 v){
            return ((int)((t) * v.sec_to_cycles));
        }

	/* Macros for PCR */
        public static int CA1_LOW_TO_HIGH(int c){
            return (c & 0x01);
        }
        public static int CA1_HIGH_TO_LOW(int c){
            return ((c & 0x01))!=0?0:1;
        }

        public static int CB1_LOW_TO_HIGH(int c){
            return (c & 0x10);
        }
        public static int CB1_HIGH_TO_LOW(int c){
            return ((c & 0x10)==0)?1:0;
        }

        public static int CA2_INPUT(int c){
            return((c & 0x08)!=0)?1:0;
        }
        public static int CA2_LOW_TO_HIGH(int c){
            return ((c & 0x0c) == 0x04)?1:0;
        }
        public static int CA2_HIGH_TO_LOW(int c){
            return ((c & 0x0c) == 0x00)?1:0;
        }
        public static int CA2_IND_IRQ(int c){
            return ((c & 0x0a) == 0x02)?1:0;
        }

/*TODO*///	#define CA2_OUTPUT(c)			(c & 0x08)
    public static int CA2_AUTO_HS(int c){
        return ((c & 0x0c) == 0x08)?1:0;
    }
/*TODO*///	#define CA2_HS_OUTPUT(c)		((c & 0x0e) == 0x08)
/*TODO*///	#define CA2_PULSE_OUTPUT(c)		((c & 0x0e) == 0x0a)
    public static int CA2_FIX_OUTPUT(int c){
        return ((c & 0x0c) == 0x0c)?1:0;
    }
    public static int CA2_OUTPUT_LEVEL(int c){
        return ((c & 0x02) >> 1);
    }
/*TODO*///	
/*TODO*///	#define CB2_INPUT(c)			(!(c & 0x80))
/*TODO*///	#define CB2_LOW_TO_HIGH(c)		((c & 0xc0) == 0x40)
/*TODO*///	#define CB2_HIGH_TO_LOW(c)		((c & 0xc0) == 0x00)
    public static int CB2_IND_IRQ(int c){
        return ((c & 0xa0) == 0x20)?1:0;
    }

/*TODO*///	#define CB2_OUTPUT(c)			(c & 0x80)
    public static int CB2_AUTO_HS(int c){
        return ((c & 0xc0) == 0x80)?1:0;
    }
/*TODO*///	#define CB2_HS_OUTPUT(c)		((c & 0xe0) == 0x80)
/*TODO*///	#define CB2_PULSE_OUTPUT(c)		((c & 0xe0) == 0xa0)
    public static int CB2_FIX_OUTPUT(int c){
        return ((c & 0xc0) == 0xc0)?1:0;
    }
    public static int CB2_OUTPUT_LEVEL(int c){
        return ((c & 0x20) >> 5);
    }
	
	/* Macros for ACR */
    public static int PA_LATCH_ENABLE(int c){
        return (c & 0x01);
    }
    public static int PB_LATCH_ENABLE(int c){
        return (c & 0x02);
    }
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
    public static int T1_SET_PB7(int c){
        return (c & 0x80);
    }
    public static int T1_CONTINUOUS(int c){
        return (c & 0x40);
    }
    public static int T2_COUNT_PB6(int c){
        return (c & 0x20);
    }

    /* Interrupt flags */
    public static int INT_CA2	= 0x01;
    public static int INT_CA1	= 0x02;
    public static int INT_SR	= 0x04;
    public static int INT_CB2	= 0x08;
    public static int INT_CB1	= 0x10;
    public static int INT_T2	= 0x20;
    public static int INT_T1	= 0x40;
    public static int INT_ANY	= 0x80;

    public static void CLR_PA_INT(via6522 v){
        via_clear_int (v, INT_CA1 | ((CA2_IND_IRQ(v.pcr)==0) ? INT_CA2: 0));
    }
    public static void CLR_PB_INT(via6522 v){
        via_clear_int (v, INT_CB1 | ((CB2_IND_IRQ(v.pcr)==0) ? INT_CB2: 0));
    }

    public static int IFR_DELAY = 3;

    public static int TIMER1_VALUE(via6522 v){
        return (v.t1ll+(v.t1lh<<8));
    }
    public static int TIMER2_VALUE(via6522 v){
        return (v.t2ll+(v.t2lh<<8));
    }
	
	/******************* static variables *******************/
	
	static via6522[] via = new via6522[MAX_VIA];
        static int _via = 0;
	
	/******************* configuration *******************/
	
	static void via_set_clock(int which,int clock)
	{
		via[which].sec_to_cycles = clock;
		via[which].cycles_to_sec = 1.0 / via[which].sec_to_cycles;
	}
	
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
		via_set_clock (which, Machine.drv.cpu[0].cpu_clock);
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
	
	/******************* external interrupt check *******************/
	
	static void via_set_int (via6522 v, int data)
	{
		v.ifr |= data;
	
		if ((v.ier & v.ifr) != 0)
	    {
			v.ifr |= INT_ANY;
			if (v.intf.irq_func != null) (v.intf.irq_func).handler(ASSERT_LINE);
	    }
	}
	
	static void via_clear_int (via6522 v, int data)
	{
		v.ifr = (v.ifr & ~data) & 0x7f;
	
		if ((v.ifr & v.ier) != 0)
			v.ifr |= INT_ANY;
		else
			if (v.intf.irq_func != null) (v.intf.irq_func).handler(CLEAR_LINE);
	}
	
	/******************* Timer timeouts *************************/
	public static timer_callback via_t1_timeout = new timer_callback() {
            public void handler (int which) {
                via6522 v = via[which];
	
	
		if (T1_CONTINUOUS (v.acr) != 0)
	    {
			if (T1_SET_PB7(v.acr) != 0)
				v.out_b ^= 0x80;
			timer_reset (v.t1, V_CYCLES_TO_TIME(TIMER1_VALUE(v) + IFR_DELAY, v));
	    }
		else
	    {
			if (T1_SET_PB7(v.acr) != 0)
				v.out_b |= 0x80;
			v.t1 = null;
			v.time1=timer_get_time();
	    }
		if (v.intf.out_b_func!= null && v.ddr_b!=0)
			v.intf.out_b_func.handler(0, v.out_b & v.ddr_b);
	
		if ((v.ifr & INT_T1)==0)
			via_set_int (v, INT_T1);
            }
        };
	
	static timer_callback via_t2_timeout = new timer_callback() {
            public void handler(int which) {
                via6522 v = via[_via + which];
	
		if (v.intf.t2_callback != null)
			v.intf.t2_callback.handler((int) timer_timeelapsed(v.t2));
	
		v.t2 = null;
		v.time2=timer_get_time();
	
		if ((v.ifr & INT_T2)==0)
			via_set_int (v, INT_T2);
            }
        };
	
	/******************* CPU interface for VIA read *******************/
	
	public static int via_read(int which, int offset)
	{
		via6522 v = via[_via + which];
		int val = 0;
	
		offset &= 0xf;
	
		switch (offset)
	    {
	    case VIA_PB:
			/* update the input */
			if (PB_LATCH_ENABLE(v.acr) == 0)
				if (v.intf.in_b_func != null) v.in_b = v.intf.in_b_func.handler(0);
	
			CLR_PB_INT(v);
	
			/* combine input and output values, hold DDRB bit 7 high if T1_SET_PB7 */
			if (T1_SET_PB7(v.acr) != 0)
				val = (v.out_b & (v.ddr_b | 0x80)) | (v.in_b & ~(v.ddr_b | 0x80));
			else
				val = (v.out_b & v.ddr_b) + (v.in_b & ~v.ddr_b);
			break;
	
	    case VIA_PA:
			/* update the input */
			if (PA_LATCH_ENABLE(v.acr) == 0)
				if (v.intf.in_a_func != null) v.in_a = v.intf.in_a_func.handler(0);
	
			/* combine input and output values */
			val = (v.out_a & v.ddr_a) + (v.in_a & ~v.ddr_a);
	
			CLR_PA_INT(v);
	
			/* If CA2 is configured as output and in pulse or handshake mode,
			   CA2 is set now */
			if (CA2_AUTO_HS(v.pcr) != 0)
			{
				if (v.out_ca2 != 0)
				{
					/* set CA2 */
					v.out_ca2 = 0;
	
					/* call the CA2 output function */
					if (v.intf.out_ca2_func != null) v.intf.out_ca2_func.handler(0, 0);
				}
			}
	
			break;
	
	    case VIA_PANH:
			/* update the input */
			if (PA_LATCH_ENABLE(v.acr) == 0)
				if (v.intf.in_a_func != null) v.in_a = v.intf.in_a_func.handler(0);
	
			/* combine input and output values */
			val = (v.out_a & v.ddr_a) + (v.in_a & ~v.ddr_a);
			break;
	
	    case VIA_DDRB:
			val = v.ddr_b;
			break;
	
	    case VIA_DDRA:
			val = v.ddr_a;
			break;
	
	    case VIA_T1CL:
			via_clear_int (v, INT_T1);
			if (v.t1 != null)
				val = V_TIME_TO_CYCLES((int) timer_timeleft(v.t1), v) & 0xff;
			else
			{
				if ( T1_CONTINUOUS(v.acr) != 0)
				{
					val = (TIMER1_VALUE(v)-
						   (V_TIME_TO_CYCLES((int) (timer_get_time()-v.time1), v)
							%TIMER1_VALUE(v))-1)&0xff;
				}
				else
				{
					val = (0x10000-
						   (V_TIME_TO_CYCLES((int) (timer_get_time()-v.time1), v)&0xffff)
						   -1)&0xff;
				}
			}
			break;
	
	    case VIA_T1CH:
			if (v.t1 != null)
				val = V_TIME_TO_CYCLES((int) timer_timeleft(v.t1), v) >> 8;
			else
			{
				if ( T1_CONTINUOUS(v.acr) != 0)
				{
					val = (TIMER1_VALUE(v)-
						   (V_TIME_TO_CYCLES((int) (timer_get_time()-v.time1), v)
							%TIMER1_VALUE(v))-1)>>8;
				}
				else
				{
					val = (0x10000-
						   (V_TIME_TO_CYCLES((int) (timer_get_time()-v.time1), v)&0xffff)
						   -1)>>8;
				}
			}
			break;
	
	    case VIA_T1LL:
			val = v.t1ll;
			break;
	
	    case VIA_T1LH:
			val = v.t1lh;
			break;
	
	    case VIA_T2CL:
			via_clear_int (v, INT_T2);
			if (v.t2 != null)
				val = V_TIME_TO_CYCLES((int) timer_timeleft(v.t2), v) & 0xff;
			else
			{
				if (T2_COUNT_PB6(v.acr) != 0)
				{
					val = v.t2cl;
				}
				else
				{
					val = (0x10000-
						   (V_TIME_TO_CYCLES((int) (timer_get_time()-v.time2), v)&0xffff)
						   -1)&0xff;
				}
			}
			break;
	
	    case VIA_T2CH:
			if (v.t2 != null)
				val = V_TIME_TO_CYCLES((int) timer_timeleft(v.t2), v) >> 8;
			else
			{
				if (T2_COUNT_PB6(v.acr) != 0)
				{
					val = v.t2ch;
				}
				else
				{
					val = (0x10000-
						   (V_TIME_TO_CYCLES((int) (timer_get_time()-v.time2), v)&0xffff)
						   -1)>>8;
				}
			}
			break;
	
	    case VIA_SR:
			val = v.sr;
			break;
	
	    case VIA_PCR:
			val = v.pcr;
			break;
	
	    case VIA_ACR:
			val = v.acr;
			break;
	
	    case VIA_IER:
			val = v.ier | 0x80;
			break;
	
	    case VIA_IFR:
			val = v.ifr;
			break;
	    }
		return val;
	}
	
	
	/******************* CPU interface for VIA write *******************/
	
	public static void via_write(int which, int offset, int data)
	{
		via6522 v = via[_via + which];
	
		offset &=0x0f;
		switch (offset)
	    {
	    case VIA_PB:
			if (T1_SET_PB7(v.acr) != 0)
				v.out_b = (v.out_b & 0x80) | (data  & 0x7f);
			else
				v.out_b = data;
	
			if (v.intf.out_b_func!=null && v.ddr_b!=0)
				v.intf.out_b_func.handler(0, v.out_b & v.ddr_b);
	
			CLR_PB_INT(v);
	
			/* If CB2 is configured as output and in pulse or handshake mode,
			   CB2 is set now */
			if (CB2_AUTO_HS(v.pcr) != 0)
			{
				if (v.out_cb2 != 0)
				{
					/* set CB2 */
					v.out_cb2 = 0;
	
					/* call the CB2 output function */
					if (v.intf.out_cb2_func != null) v.intf.out_cb2_func.handler(0, 0);
				}
			}
			break;
	
	    case VIA_PA:
			v.out_a = data;
			if (v.intf.out_a_func!= null && v.ddr_a!=0 )
				v.intf.out_a_func.handler(0, v.out_a & v.ddr_a);
	
			CLR_PA_INT(v);
	
			/* If CA2 is configured as output and in pulse or handshake mode,
			   CA2 is set now */
			if (CA2_AUTO_HS(v.pcr) != 0)
			{
				if (v.out_ca2 != 0)
				{
					/* set CA2 */
					v.out_ca2 = 0;
	
					/* call the CA2 output function */
					if (v.intf.out_ca2_func != null) v.intf.out_ca2_func.handler(0, 0);
				}
			}
	
			break;
	
	    case VIA_PANH:
			v.out_a = data;
			if (v.intf.out_a_func!=null && v.ddr_a!=0)
				v.intf.out_a_func.handler(0, v.out_a & v.ddr_a);
			break;
	
	    case VIA_DDRB:
	    	/* EHC 03/04/2000 - If data direction changed, present output on the lines */
	    	if ( data != v.ddr_b ) {
				v.ddr_b = data;
	
				if (v.intf.out_b_func!=null && v.ddr_b!=0)
					v.intf.out_b_func.handler(0, v.out_b & v.ddr_b);
			}
			break;
	
	    case VIA_DDRA:
	    	/* EHC 03/04/2000 - If data direction changed, present output on the lines */
	    	if ( data != v.ddr_a ) {
				v.ddr_a = data;
	
				if (v.intf.out_a_func!=null && v.ddr_a!=0)
					v.intf.out_a_func.handler(0, v.out_a & v.ddr_a);
			}
			break;
	
	    case VIA_T1CL:
	    case VIA_T1LL:
			v.t1ll = data;
			break;
	
		case VIA_T1LH:
		    v.t1lh = data;
		    via_clear_int (v, INT_T1);
		    break;
	
	    case VIA_T1CH:
			v.t1ch = v.t1lh = data;
			v.t1cl = v.t1ll;
	
			via_clear_int (v, INT_T1);
	
			if (T1_SET_PB7(v.acr) != 0)
			{
				v.out_b &= 0x7f;
				if (v.intf.out_b_func!=null && v.ddr_b!=0)
					v.intf.out_b_func.handler(0, v.out_b & v.ddr_b);
			}
			if (v.t1 != null)
				timer_reset (v.t1, V_CYCLES_TO_TIME(TIMER1_VALUE(v) + IFR_DELAY, v));
			else
				v.t1 = timer_set (V_CYCLES_TO_TIME(TIMER1_VALUE(v) + IFR_DELAY, v), which, via_t1_timeout);
			break;
	
	    case VIA_T2CL:
			v.t2ll = data;
			break;
	
	    case VIA_T2CH:
			v.t2ch = v.t2lh = data;
			v.t2cl = v.t2ll;
	
			via_clear_int (v, INT_T2);
	
			if (T2_COUNT_PB6(v.acr)==0)
			{
				if (v.t2 != null)
				{
					if (v.intf.t2_callback != null)
						v.intf.t2_callback.handler((int) timer_timeelapsed(v.t2));
					timer_reset (v.t2, V_CYCLES_TO_TIME(TIMER2_VALUE(v) + IFR_DELAY, v));
				}
				else
					v.t2 = timer_set (V_CYCLES_TO_TIME(TIMER2_VALUE(v) + IFR_DELAY, v),
									   which, via_t2_timeout);
			}
			else
			{
				v.time2=timer_get_time();
			}
			break;
	
	    case VIA_SR:
		/*TODO*///	v.sr = data;
		/*TODO*///	if (v.intf.out_shift_func && SO_O2_CONTROL(v.acr))
		/*TODO*///		v.intf.out_shift_func(data);
		/*TODO*///	/* kludge for Mac Plus (and 128k, 512k, 512ke) : */
		/*TODO*///	if (v.intf.out_shift_func2 && SO_EXT_CONTROL(v.acr))
		/*TODO*///	{
		/*TODO*///		v.intf.out_shift_func2(data);
		/*TODO*///		via_set_int(v, INT_SR);
		/*TODO*///	}
			break;
	
	    case VIA_PCR:
			v.pcr = data;
	
			if (CA2_FIX_OUTPUT(data)!=0 && (CA2_OUTPUT_LEVEL(data) ^ v.out_ca2) !=0)
			{
				v.out_ca2 = CA2_OUTPUT_LEVEL(data);
				if (v.intf.out_ca2_func!=null)
					v.intf.out_ca2_func.handler(0, v.out_ca2);
			}
	
			if (CB2_FIX_OUTPUT(data)!=0 && (CB2_OUTPUT_LEVEL(data) ^ v.out_cb2)!=0)
			{
				v.out_cb2 = CB2_OUTPUT_LEVEL(data);
				if (v.intf.out_cb2_func != null)
					v.intf.out_cb2_func.handler(0, v.out_cb2);
			}
			break;
	
	    case VIA_ACR:
			v.acr = data;
			if (T1_SET_PB7(v.acr) != 0)
			{
				if (v.t1 != null)
					v.out_b &= ~0x80;
				else
					v.out_b |= 0x80;
	
				if (v.intf.out_b_func!= null && v.ddr_b!=0)
					v.intf.out_b_func.handler(0, v.out_b & v.ddr_b);
			}
			if (T1_CONTINUOUS(data) != 0)
			{
				if (v.t1 != null)
					timer_reset (v.t1, V_CYCLES_TO_TIME(TIMER1_VALUE(v) + IFR_DELAY, v));
				else
					v.t1 = timer_set (V_CYCLES_TO_TIME(TIMER1_VALUE(v) + IFR_DELAY, v), which, via_t1_timeout);
			}
			/* kludge for Mac Plus (and 128k, 512k, 512ke) : */
			/*TODO*///if (v.intf.si_ready_func && SI_EXT_CONTROL(data))
			/*TODO*///	v.intf.si_ready_func();
			break;
	
		case VIA_IER:
			if ((data & 0x80)!=0)
				v.ier |= data & 0x7f;
			else
				v.ier &= ~(data & 0x7f);
	
			if ((v.ifr & INT_ANY)!=0)
			{
				if (((v.ifr & v.ier) & 0x7f) == 0)
				{
					v.ifr &= ~INT_ANY;
					if (v.intf.irq_func != null)
						(v.intf.irq_func).handler(CLEAR_LINE);
				}
			}
			else
			{
				if (((v.ier & v.ifr) & 0x7f)!=0)
				{
					v.ifr |= INT_ANY;
					if (v.intf.irq_func!=null)
						(v.intf.irq_func).handler(ASSERT_LINE);
				}
			}
			break;
	
		case VIA_IFR:
			if ((data & INT_ANY)!=0)
				data = 0x7f;
			via_clear_int (v, data);
			break;
	    }
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
		via6522 v = via[ _via + which];
	
		/* limit the data to 0 or 1 */
		data = data!=0 ? 1 : 0;
	
		/* handle the active transition */
		if (data != v.in_ca1)
	    {
			if ((CA1_LOW_TO_HIGH(v.pcr)!=0 && data!=0) || (CA1_HIGH_TO_LOW(v.pcr)!=0 && data==0))
			{
				if (PA_LATCH_ENABLE(v.acr) != 0)
					if (v.intf.in_a_func !=null) v.in_a = v.intf.in_a_func.handler(0);
				via_set_int (v, INT_CA1);
	
				/* CA2 is configured as output and in pulse or handshake mode,
				   CA2 is cleared now */
				if (CA2_AUTO_HS(v.pcr) != 0)
				{
					if (v.out_ca2==0)
					{
						/* clear CA2 */
						v.out_ca2 = 1;
	
						/* call the CA2 output function */
						if (v.intf.out_ca2_func != null) v.intf.out_ca2_func.handler(0, 1);
					}
				}
			}
			v.in_ca1 = data;
	    }
	}
	
	/******************* interface setting VIA port CA2 input *******************/
	
	public static void via_set_input_ca2(int which, int data)
	{
		via6522 v = via[_via + which];
	
		/* limit the data to 0 or 1 */
		data = data!=0 ? 1 : 0;
	
		/* CA2 is in input mode */
		if (CA2_INPUT(v.pcr) != 0)
	    {
			/* the new state has caused a transition */
			if (v.in_ca2 != data)
			{
				/* handle the active transition */
				if ((data!=0 && CA2_LOW_TO_HIGH(v.pcr)!=0) || (data==0 && CA2_HIGH_TO_LOW(v.pcr)!=0))
				{
					/* mark the IRQ */
					via_set_int (v, INT_CA2);
				}
				/* set the new value for CA2 */
				v.in_ca2 = data;
			}
	    }
	
	
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
		via6522 v = via[_via + which];
	
		/* limit the data to 0 or 1 */
		data = data!=0 ? 1 : 0;
	
		/* handle the active transition */
		if (data != v.in_cb1)
	    {
			if ((CB1_LOW_TO_HIGH(v.pcr)!=0 && data!=0) || (CB1_HIGH_TO_LOW(v.pcr)!=0 && data==0))
			{
				if (PB_LATCH_ENABLE(v.acr) !=0)
					if (v.intf.in_b_func != null) v.in_b = v.intf.in_b_func.handler(0);
				via_set_int (v, INT_CB1);
	
				/* CB2 is configured as output and in pulse or handshake mode,
				   CB2 is cleared now */
				if (CB2_AUTO_HS(v.pcr) !=0)
				{
					if (v.out_cb2 == 0)
					{
						/* clear CB2 */
						v.out_cb2 = 1;
	
						/* call the CB2 output function */
						if (v.intf.out_cb2_func != null) v.intf.out_cb2_func.handler(0, 1);
					}
				}
			}
			v.in_cb1 = data;
	    }
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
