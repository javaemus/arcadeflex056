/***************************************************************************

  Z80 FMLY.C   Z80 FAMILY CHIP EMURATOR for MAME Ver.0.1 alpha

  Support chip :  Z80PIO , Z80CTC

  Copyright(C) 1997 Tatsuyuki Satoh.

  This version are tested starforce driver.

  8/21/97 -- Heavily modified by Aaron Giles to be much more accurate for the MCR games
  8/27/97 -- Rewritten a second time by Aaron Giles, with the datasheets in hand

pending:
	Z80CTC , Counter mode & Timer with Trigrt start :not support Triger level

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.machine;

import static arcadeflex056.fucPtr.*;
import static mame056.machine.z80fmlyH.*;
import static mame056.timer.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpuexecH.*;


public class z80fmly {
	public static class z80ctc
	{
		public int vector;                 /* interrupt vector */
		public int clock;                  /* system clock */
		public double invclock16;          /* 16/system clock */
		public double invclock256;         /* 256/system clock */
		public ReadHandlerPtr intr;    /* interrupt callback */
		public WriteHandlerPtr[] zc= new WriteHandlerPtr[4];    /* zero crossing callbacks */
		public int notimer;                /* no timer masks */
		public int[] mask=new int[4];                /* masked channel flags */
		public int[] mode=new int[4];                /* current mode */
		public int[] tconst=new int[4];              /* time constant */
		public int[] down=new int[4];                /* down counter (clock mode only) */
		public int[] extclk=new int[4];              /* current signal from the external clock */
		public timer_entry[] timer=new timer_entry[4];             /* array of active timers */
	
		public int[] int_state=new int[4];           /* interrupt status (for daisy chain) */
	};
	
	static z80ctc[] ctcs=new z80ctc[MAX_CTC];
	

	/* these are the bits of the incoming commands to the CTC */
        public static int INTERRUPT = 0x80;
        public static int INTERRUPT_ON = 0x80;
        public static int INTERRUPT_OFF = 0x00;

        public static int MODE = 0x40;
        public static int MODE_TIMER = 0x00;
        public static int MODE_COUNTER = 0x40;

        public static int PRESCALER = 0x20;
        public static int PRESCALER_256 = 0x20;
        public static int PRESCALER_16 = 0x00;

        public static int EDGE = 0x10;
        public static int EDGE_FALLING = 0x00;
        public static int EDGE_RISING = 0x10;

        public static int TRIGGER = 0x08;
        public static int TRIGGER_AUTO = 0x00;
        public static int TRIGGER_CLOCK = 0x08;

        public static int CONSTANT = 0x04;
        public static int CONSTANT_LOAD = 0x04;
        public static int CONSTANT_NONE = 0x00;;

        public static int RESET = 0x02;
        public static int RESET_CONTINUE = 0x00;
        public static int RESET_ACTIVE = 0x02;

        public static int CONTROL = 0x01;
        public static int CONTROL_VECTOR = 0x00;
        public static int CONTROL_WORD = 0x01;

        /* these extra bits help us keep things accurate */
        public static int WAITING_FOR_TRIG = 0x100;


/*TODO*///	static void z80ctc_timercallback (int param);
	
	
	public static void z80ctc_init (z80ctc_interface intf)
	{
		int i;
	
		/*TODO*///memset (ctcs, 0, sizeof (ctcs));
	
		for (i = 0; i < intf.num; i++)
		{
                        ctcs[i] = new z80ctc();
			ctcs[i].clock = intf.baseclock[i];
			ctcs[i].invclock16 = 16.0 / (double)intf.baseclock[i];
			ctcs[i].invclock256 = 256.0 / (double)intf.baseclock[i];
			ctcs[i].notimer = intf.notimer[i];
			ctcs[i].intr = intf.intr[i];
			ctcs[i].zc[0] = intf.zc0[i];
			ctcs[i].zc[1] = intf.zc1[i];
			ctcs[i].zc[2] = intf.zc2[i];
			ctcs[i].zc[3] = null;
			z80ctc_reset.handler(i);
		}
	}
	
	
/*TODO*///	double z80ctc_getperiod (int which, int ch)
/*TODO*///	{
/*TODO*///		z80ctc *ctc = ctcs + which;
/*TODO*///		double clock;
/*TODO*///		int mode;
/*TODO*///	
/*TODO*///		/* keep channel within range, and get the current mode */
/*TODO*///		ch &= 3;
/*TODO*///		mode = ctc->mode[ch];
/*TODO*///	
/*TODO*///		/* if reset active */
/*TODO*///		if( (mode & RESET) == RESET_ACTIVE) return 0;
/*TODO*///		/* if counter mode */
/*TODO*///		if( (mode & MODE) == MODE_COUNTER)
/*TODO*///		{
/*TODO*///			logerror("CTC %d is CounterMode : Can't calcrate period\n", ch );
/*TODO*///			return 0;
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* compute the period */
/*TODO*///		clock = ((mode & PRESCALER) == PRESCALER_16) ? ctc->invclock16 : ctc->invclock256;
/*TODO*///		return clock * (double)ctc->tconst[ch];
/*TODO*///	}
	
	/* interrupt request callback with daisy-chain circuit */
	public static void z80ctc_interrupt_check( z80ctc ctc )
	{
		int state = 0;
		int ch;
	
		for( ch = 3 ; ch >= 0 ; ch-- )
		{
			/* if IEO disable , same and lower IRQ is masking */
	/* ASG: changed this line because this state could have an interrupt pending as well! */
	/*		if( ctc->int_state[ch] & Z80_INT_IEO ) state  = Z80_INT_IEO;*/
			if(( ctc.int_state[ch] & Z80_INT_IEO )!=0 ) state  = ctc.int_state[ch];
			else                                   state |= ctc.int_state[ch];
		}
		/* change interrupt status */
		/*TODO*///if (ctc.intr != null) (*ctc.intr)(state);
	}
	
	
	public static ResetPtr z80ctc_reset = new ResetPtr() {
            public void handler(int which) {
                z80ctc ctc = ctcs[which];
		int i;
	
		/* set up defaults */
		for (i = 0; i < 4; i++)
		{
			ctc.mode[i] = RESET_ACTIVE;
			ctc.tconst[i] = 0x100;
			if (ctc.timer[i] != null)
				timer_remove (ctc.timer[i]);
			ctc.timer[i] = null;
			ctc.int_state[i] = 0;
		}
		z80ctc_interrupt_check( ctc );
            }
        };
	
	
/*TODO*///	void z80ctc_0_reset (void) { z80ctc_reset (0); }
/*TODO*///	void z80ctc_1_reset (void) { z80ctc_reset (1); }
	
	
	public static void z80ctc_w (int which, int offset, int data)
	{
		//z80ctc *ctc = ctcs + which;
                z80ctc ctc = ctcs[which];
		int mode, ch;
	
		/* keep channel within range, and get the current mode */
		ch = offset & 3;
		mode = ctc.mode[ch];
	
		/* if we're waiting for a time constant, this is it */
		if ((mode & CONSTANT) == CONSTANT_LOAD)
		{
			/* set the time constant (0 -> 0x100) */
			ctc.tconst[ch] = data!=0 ? data : 0x100;
	
			/* clear the internal mode -- we're no longer waiting */
			ctc.mode[ch] &= ~CONSTANT;
	
			/* also clear the reset, since the constant gets it going again */
			ctc.mode[ch] &= ~RESET;
	
			/* if we're in timer mode.... */
			if ((mode & MODE) == MODE_TIMER)
			{
				/* if we're triggering on the time constant, reset the down counter now */
				if ((mode & TRIGGER) == TRIGGER_AUTO)
				{
					double clock = ((mode & PRESCALER) == PRESCALER_16) ? ctc.invclock16 : ctc.invclock256;
					if (ctc.timer[ch] != null)
						timer_remove (ctc.timer[ch]);
					if ((ctc.notimer & (1<<ch)) == 0)
						ctc.timer[ch] = timer_pulse(clock * (double)ctc.tconst[ch], (which << 2) + ch, z80ctc_timercallback);
				}
	
				/* else set the bit indicating that we're waiting for the appropriate trigger */
				else
					ctc.mode[ch] |= WAITING_FOR_TRIG;
			}
	
			/* also set the down counter in case we're clocking externally */
			ctc.down[ch] = ctc.tconst[ch];
	
			/* all done here */
			return;
		}
	
/*TODO*///		/* if we're writing the interrupt vector, handle it specially */
/*TODO*///	#if 0	/* Tatsuyuki Satoh changes */
/*TODO*///		/* The 'Z80family handbook' wrote,                            */
/*TODO*///		/* interrupt vector is able to set for even channel (0 or 2)  */
/*TODO*///		if ((data & CONTROL) == CONTROL_VECTOR && (ch&1) == 0)
/*TODO*///	#else
		if ((data & CONTROL) == CONTROL_VECTOR && ch == 0)
/*TODO*///	#endif
		{
			ctc.vector = data & 0xf8;
			logerror("CTC Vector = %02x\n", ctc.vector);
			return;
		}
	
		/* this must be a control word */
		if ((data & CONTROL) == CONTROL_WORD)
		{
			/* set the new mode */
			ctc.mode[ch] = data;
			logerror("CTC ch.%d mode = %02x\n", ch, data);
	
			/* if we're being reset, clear out any pending timers for this channel */
			if ((data & RESET) == RESET_ACTIVE)
			{
				if (ctc.timer[ch] != null)
					timer_remove (ctc.timer[ch]);
				ctc.timer[ch] = null;
	
				if( ctc.int_state[ch] != 0 )
				{
					/* clear interrupt service , request */
					ctc.int_state[ch] = 0;
					z80ctc_interrupt_check( ctc );
				}
			}
	
			/* all done here */
			return;
		}
	}
	
	public static WriteHandlerPtr z80ctc_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { z80ctc_w (0, offset, data); } };
	public static WriteHandlerPtr z80ctc_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { z80ctc_w (1, offset, data); } };
	
	
	public static int z80ctc_r (int which, int ch)
	{
		z80ctc ctc = ctcs[which];
		int mode;
	
		/* keep channel within range */
		ch &= 3;
		mode = ctc.mode[ch];
	
		/* if we're in counter mode, just return the count */
		if ((mode & MODE) == MODE_COUNTER)
			return ctc.down[ch];
	
		/* else compute the down counter value */
		else
		{
			double clock = ((mode & PRESCALER) == PRESCALER_16) ? ctc.invclock16 : ctc.invclock256;
	
                        logerror("CTC clock %f\n",1.0/clock);
	
	
			if (ctc.timer[ch] != null)
				return ((int)(timer_timeleft (ctc.timer[ch]) / clock) + 1) & 0xff;
			else
				return 0;
		}
	}
	
	public static ReadHandlerPtr z80ctc_0_r  = new ReadHandlerPtr() { public int handler(int offset) { return z80ctc_r (0, offset); } };
	public static ReadHandlerPtr z80ctc_1_r  = new ReadHandlerPtr() { public int handler(int offset) { return z80ctc_r (1, offset); } };
	
	
	public static Interrupt_entryPtr z80ctc_interrupt = new Interrupt_entryPtr() {
            public int handler(int which) {
                z80ctc ctc = ctcs[which];
		int ch;
	
		for( ch = 0 ; ch < 4 ; ch++ )
		{
			if( ctc.int_state[ch] != 0)
			{
				if( ctc.int_state[ch] == Z80_INT_REQ)
					ctc.int_state[ch] = Z80_INT_IEO;
				break;
			}
		}
		if( ch > 3 )
		{
			logerror("CTC entry INT : non IRQ\n");
			ch = 0;
		}
		z80ctc_interrupt_check( ctc );
		return ctc.vector + ch * 2;
            }
        };
	
	/* when operate RETI , soud be call this function for request pending interrupt */
	public static Interrupt_retiPtr z80ctc_reti = new Interrupt_retiPtr() {
            public void handler(int which) {
                z80ctc ctc = ctcs[which];
		int ch;
	
		for( ch = 0 ; ch < 4 ; ch++ )
		{
			if(( ctc.int_state[ch] & Z80_INT_IEO ) != 0)
			{
				/* highest served interrupt found */
				/* clear interrupt status */
				ctc.int_state[ch] &= ~Z80_INT_IEO;
				/* search next interrupt */
				break;
			}
		}
		/* set next interrupt stattus */
		z80ctc_interrupt_check( ctc );
            }
        };
	
	public static timer_callback z80ctc_timercallback = new timer_callback() {
            public void handler(int param) {

		int which = param >> 2;
		int ch = param & 3;
		z80ctc ctc = ctcs[which];
	
		/* down counter has reached zero - see if we should interrupt */
		if ((ctc.mode[ch] & INTERRUPT) == INTERRUPT_ON)
		{
			if( (ctc.int_state[ch] & Z80_INT_REQ) == 0 )
			{
				ctc.int_state[ch] |= Z80_INT_REQ;
				z80ctc_interrupt_check( ctc );
			}
		}
		/* generate the clock pulse */
		if (ctc.zc[ch] != null)
		{
			(ctc.zc[ch]).handler(0,1);
			(ctc.zc[ch]).handler(0,0);
		}
	
		/* reset the down counter */
		ctc.down[ch] = ctc.tconst[ch];
            }
        };


	public static void z80ctc_trg_w (int which, int trg, int offset, int data)
	{
		z80ctc ctc = ctcs[which];
		int ch = trg & 3;
		int mode;
	
		data = data!=0 ? 1 : 0;
		mode = ctc.mode[ch];
	
		/* see if the trigger value has changed */
		if (data != ctc.extclk[ch])
		{
			ctc.extclk[ch] = data;
	
			/* see if this is the active edge of the trigger */
			if (((mode & EDGE) == EDGE_RISING && data!=0) || ((mode & EDGE) == EDGE_FALLING && (data==0)))
			{
				/* if we're waiting for a trigger, start the timer */
				if (((mode & WAITING_FOR_TRIG)!=0) && (mode & MODE) == MODE_TIMER)
				{
					double clock = ((mode & PRESCALER) == PRESCALER_16) ? ctc.invclock16 : ctc.invclock256;
	
                                        logerror("CTC clock %f\n",1.0/clock);
	
	
					if (ctc.timer[ch] != null)
						timer_remove (ctc.timer[ch]);
					if ((ctc.notimer & (1<<ch)) == 0)
						ctc.timer[ch] = timer_pulse (clock * (double)ctc.tconst[ch], (which << 2) + ch, z80ctc_timercallback);
				}
	
				/* we're no longer waiting */
				ctc.mode[ch] &= ~WAITING_FOR_TRIG;
	
				/* if we're clocking externally, decrement the count */
				if ((mode & MODE) == MODE_COUNTER)
				{
					ctc.down[ch]--;
	
					/* if we hit zero, do the same thing as for a timer interrupt */
					if (ctc.down[ch] == 0)
						z80ctc_timercallback.handler((which << 2) + ch);
				}
			}
		}
	}
	
/*TODO*///	public static WriteHandlerPtr z80ctc_0_trg0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { z80ctc_trg_w (0, 0, offset, data); } };
	public static WriteHandlerPtr z80ctc_0_trg1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { z80ctc_trg_w (0, 1, offset, data); } };
	public static WriteHandlerPtr z80ctc_0_trg2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { z80ctc_trg_w (0, 2, offset, data); } };
	public static WriteHandlerPtr z80ctc_0_trg3_w = new WriteHandlerPtr() {public void handler(int offset, int data) { z80ctc_trg_w (0, 3, offset, data); } };
/*TODO*///	public static WriteHandlerPtr z80ctc_1_trg0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { z80ctc_trg_w (1, 0, offset, data); } };
/*TODO*///	public static WriteHandlerPtr z80ctc_1_trg1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { z80ctc_trg_w (1, 1, offset, data); } };
/*TODO*///	public static WriteHandlerPtr z80ctc_1_trg2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { z80ctc_trg_w (1, 2, offset, data); } };
/*TODO*///	public static WriteHandlerPtr z80ctc_1_trg3_w = new WriteHandlerPtr() {public void handler(int offset, int data) { z80ctc_trg_w (1, 3, offset, data); } };
/*TODO*///	
/*TODO*///	
/*TODO*///	/*---------------------- Z80 PIO ---------------------------------*/
/*TODO*///	
/*TODO*///	/* starforce emurate Z80PIO subset */
/*TODO*///	/* ch.A mode 1 input handshake mode from sound command */
/*TODO*///	/* ch.b not use */
/*TODO*///	
/*TODO*///	
/*TODO*///	#define PIO_MODE0 0x00		/* output mode */
/*TODO*///	#define PIO_MODE1 0x01		/* input  mode */
/*TODO*///	#define PIO_MODE2 0x02		/* i/o    mode */
/*TODO*///	#define PIO_MODE3 0x03		/* bit    mode */
/*TODO*///	/* pio controll port operation (bit 0-3) */
/*TODO*///	#define PIO_OP_MODE 0x0f	/* mode select        */
/*TODO*///	#define PIO_OP_INTC 0x07	/* interrupt controll */
/*TODO*///	#define PIO_OP_INTE 0x03	/* interrupt enable   */
/*TODO*///	#define PIO_OP_INTE 0x03	/* interrupt enable   */
/*TODO*///	/* pio interrupt controll nit */
/*TODO*///	#define PIO_INT_ENABLE 0x80  /* ENABLE : 0=disable , 1=enable */
/*TODO*///	#define PIO_INT_AND    0x40  /* LOGIC  : 0=OR      , 1=AND    */
/*TODO*///	#define PIO_INT_HIGH   0x20  /* LEVEL  : 0=low     , 1=high   */
/*TODO*///	#define PIO_INT_MASK   0x10  /* MASK   : 0=off     , 1=on     */
/*TODO*///	
/*TODO*///	typedef struct
/*TODO*///	{
/*TODO*///		int vector[2];                        /* interrupt vector               */
/*TODO*///		void (*intr)(int which);              /* interrupt callbacks            */
/*TODO*///		void (*rdyr[2])(int data);            /* RDY active callback            */
/*TODO*///		int mode[2];                          /* mode 00=in,01=out,02=i/o,03=bit*/
/*TODO*///		int enable[2];                        /* interrupt enable               */
/*TODO*///		int mask[2];                          /* mask folowers                  */
/*TODO*///		int dir[2];                           /* direction (bit mode)           */
/*TODO*///		int rdy[2];                           /* ready pin level                */
/*TODO*///		int in[2];                            /* input port data                */
/*TODO*///		int out[2];                           /* output port                    */
/*TODO*///		int strobe[2];							/* strobe inputs */
/*TODO*///		int int_state[2];                     /* interrupt status (daisy chain) */
/*TODO*///	} z80pio;
/*TODO*///	
/*TODO*///	static z80pio pios[MAX_PIO];
/*TODO*///	
/*TODO*///	static void	z80pio_set_rdy(z80pio *pio, int ch, int state)
/*TODO*///	{
/*TODO*///		/* set state */
/*TODO*///		pio->rdy[ch] = state;
/*TODO*///	
/*TODO*///		/* call callback with state */
/*TODO*///		if (pio->rdyr[ch]!=0)
/*TODO*///			pio->rdyr[ch](pio->rdy[ch]);
/*TODO*///	}
/*TODO*///	
/*TODO*///	/* initialize pio emurator */
/*TODO*///	void z80pio_init (z80pio_interface *intf)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		memset (pios, 0, sizeof (pios));
/*TODO*///	
/*TODO*///		for (i = 0; i < intf->num; i++)
/*TODO*///		{
/*TODO*///			pios[i].intr = intf->intr[i];
/*TODO*///			pios[i].rdyr[0] = intf->rdyA[i];
/*TODO*///			pios[i].rdyr[1] = intf->rdyB[i];
/*TODO*///			z80pio_reset (i);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void z80pio_interrupt_check( z80pio *pio )
/*TODO*///	{
/*TODO*///		int state;
/*TODO*///	
/*TODO*///		if( pio->int_state[1] & Z80_INT_IEO ) state  = Z80_INT_IEO;
/*TODO*///		else                                  state  = pio->int_state[1];
/*TODO*///		if( pio->int_state[0] & Z80_INT_IEO ) state  = Z80_INT_IEO;
/*TODO*///		else                                  state |= pio->int_state[0];
/*TODO*///		/* change daisy chain status */
/*TODO*///		if (pio->intr) (*pio->intr)(state);
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void z80pio_check_irq( z80pio *pio , int ch )
/*TODO*///	{
/*TODO*///		int irq = 0;
/*TODO*///		int data;
/*TODO*///		int old_state;
/*TODO*///	
/*TODO*///		if( pio->enable[ch] & PIO_INT_ENABLE )
/*TODO*///		{
/*TODO*///			if( pio->mode[ch] == PIO_MODE3 )
/*TODO*///			{
/*TODO*///				data  =  pio->in[ch] & pio->dir[ch]; /* input data only */
/*TODO*///				data &= ~pio->mask[ch];              /* mask follow     */
/*TODO*///				if( !(pio->enable[ch]&PIO_INT_HIGH) )/* active level    */
/*TODO*///					data ^= pio->mask[ch];             /* active low  */
/*TODO*///				if( pio->enable[ch]&PIO_INT_AND )    /* logic      */
/*TODO*///				     { if( data == pio->mask[ch] ) irq = 1; }
/*TODO*///				else { if( data == 0             ) irq = 1; }
/*TODO*///				/* if portB , portA mode 2 check */
/*TODO*///				if( ch && (pio->mode[0]==PIO_MODE2) )
/*TODO*///				{
/*TODO*///					if( pio->rdy[ch] == 0 ) irq = 1;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			else if( pio->rdy[ch] == 0 ) irq = 1;
/*TODO*///		}
/*TODO*///		old_state = pio->int_state[ch];
/*TODO*///		if( irq ) pio->int_state[ch] |=  Z80_INT_REQ;
/*TODO*///		else      pio->int_state[ch] &= ~Z80_INT_REQ;
/*TODO*///	
/*TODO*///		if( old_state != pio->int_state[ch] )
/*TODO*///			z80pio_interrupt_check( pio );
/*TODO*///	}
/*TODO*///	
/*TODO*///	void z80pio_reset (int which)
/*TODO*///	{
/*TODO*///		z80pio *pio = pios + which;
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		for( i = 0 ; i <= 1 ; i++){
/*TODO*///			pio->mask[i]   = 0xff;	/* mask all on */
/*TODO*///			pio->enable[i] = 0x00;	/* disable     */
/*TODO*///			pio->mode[i]   = 0x01;	/* mode input  */
/*TODO*///			pio->dir[i]    = 0x01;	/* dir  input  */
/*TODO*///			z80pio_set_rdy(pio,i,0);	/* RDY = low   */
/*TODO*///			pio->out[i]    = 0x00;	/* outdata = 0 */
/*TODO*///			pio->int_state[i] = 0;
/*TODO*///			pio->strobe[i] = 0;
/*TODO*///		}
/*TODO*///		z80pio_interrupt_check( pio );
/*TODO*///	}
/*TODO*///	
/*TODO*///	/* pio data register write */
/*TODO*///	void z80pio_d_w( int which , int ch , int data )
/*TODO*///	{
/*TODO*///		z80pio *pio = pios + which;
/*TODO*///		if( ch ) ch = 1;
/*TODO*///	
/*TODO*///		pio->out[ch] = data;	/* latch out data */
/*TODO*///		switch( pio->mode[ch] ){
/*TODO*///		case PIO_MODE0:			/* mode 0 output */
/*TODO*///		case PIO_MODE2:			/* mode 2 i/o */
/*TODO*///			z80pio_set_rdy(pio, ch,1); /* ready = H */
/*TODO*///			z80pio_check_irq( pio , ch );
/*TODO*///			return;
/*TODO*///		case PIO_MODE1:			/* mode 1 intput */
/*TODO*///		case PIO_MODE3:			/* mode 0 bit */
/*TODO*///			return;
/*TODO*///		default:
/*TODO*///			logerror("PIO-%c data write,bad mode\n",'A'+ch );
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	/* pio controll register write */
/*TODO*///	void z80pio_c_w( int which , int ch , int data )
/*TODO*///	{
/*TODO*///		z80pio *pio = pios + which;
/*TODO*///		if( ch ) ch = 1;
/*TODO*///	
/*TODO*///		/* load direction phase ? */
/*TODO*///		if( pio->mode[ch] == 0x13 ){
/*TODO*///			pio->dir[ch] = data;
/*TODO*///			pio->mode[ch] = 0x03;
/*TODO*///			return;
/*TODO*///		}
/*TODO*///		/* load mask folows phase ? */
/*TODO*///		if( pio->enable[ch] & PIO_INT_MASK ){	/* load mask folows */
/*TODO*///			pio->mask[ch] = data;
/*TODO*///			pio->enable[ch] &= ~PIO_INT_MASK;
/*TODO*///			logerror("PIO-%c interrupt mask %02x\n",'A'+ch,data );
/*TODO*///			return;
/*TODO*///		}
/*TODO*///		switch( data & 0x0f ){
/*TODO*///		case PIO_OP_MODE:	/* mode select 0=out,1=in,2=i/o,3=bit */
/*TODO*///			pio->mode[ch] = (data >> 6 );
/*TODO*///			if( pio->mode[ch] == 0x03 ) pio->mode[ch] = 0x13;
/*TODO*///			logerror("PIO-%c Mode %x\n",'A'+ch,pio->mode[ch] );
/*TODO*///			break;
/*TODO*///		case PIO_OP_INTC:		/* interrupt control */
/*TODO*///			pio->enable[ch] = data & 0xf0;
/*TODO*///			pio->mask[ch]   = 0x00;
/*TODO*///			/* when interrupt enable , set vector request flag */
/*TODO*///			logerror("PIO-%c Controll %02x\n",'A'+ch,data );
/*TODO*///			break;
/*TODO*///		case PIO_OP_INTE:		/* interrupt enable controll */
/*TODO*///			pio->enable[ch] &= ~PIO_INT_ENABLE;
/*TODO*///			pio->enable[ch] |= (data & PIO_INT_ENABLE);
/*TODO*///			logerror("PIO-%c enable %02x\n",'A'+ch,data&0x80 );
/*TODO*///			break;
/*TODO*///		default:
/*TODO*///				if( !(data&1) )
/*TODO*///				{
/*TODO*///					pio->vector[ch] = data;
/*TODO*///					logerror("PIO-%c vector %02x\n",'A'+ch,data);
/*TODO*///				}
/*TODO*///				else logerror("PIO-%c illegal command %02x\n",'A'+ch,data );
/*TODO*///		}
/*TODO*///		/* interrupt check */
/*TODO*///		z80pio_check_irq( pio , ch );
/*TODO*///	}
/*TODO*///	
/*TODO*///	/* pio controll register read */
/*TODO*///	int z80pio_c_r( int which , int ch )
/*TODO*///	{
/*TODO*///		if( ch ) ch = 1;
/*TODO*///	
/*TODO*///		logerror("PIO-%c controll read\n",'A'+ch );
/*TODO*///		return 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	/* pio data register read */
/*TODO*///	int z80pio_d_r( int which , int ch )
/*TODO*///	{
/*TODO*///		z80pio *pio = pios + which;
/*TODO*///		if( ch ) ch = 1;
/*TODO*///	
/*TODO*///		switch( pio->mode[ch] ){
/*TODO*///		case PIO_MODE0:			/* mode 0 output */
/*TODO*///			return pio->out[ch];
/*TODO*///		case PIO_MODE1:			/* mode 1 intput */
/*TODO*///			z80pio_set_rdy(pio, ch, 1);	/* ready = H */
/*TODO*///			z80pio_check_irq( pio , ch );
/*TODO*///			return pio->in[ch];
/*TODO*///		case PIO_MODE2:			/* mode 2 i/o */
/*TODO*///			if( ch ) logerror("PIO-B mode 2 \n");
/*TODO*///			z80pio_set_rdy(pio, 1, 1); /* brdy = H */
/*TODO*///			z80pio_check_irq( pio , ch );
/*TODO*///			return pio->in[ch];
/*TODO*///		case PIO_MODE3:			/* mode 3 bit */
/*TODO*///			return (pio->in[ch]&pio->dir[ch])|(pio->out[ch]&~pio->dir[ch]);
/*TODO*///		}
/*TODO*///		logerror("PIO-%c data read,bad mode\n",'A'+ch );
/*TODO*///		return 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	int z80pio_interrupt( int which )
/*TODO*///	{
/*TODO*///		z80pio *pio = pios + which;
/*TODO*///		int ch = 0;
/*TODO*///	
/*TODO*///		/* port A */
/*TODO*///		if( pio->int_state[0] == Z80_INT_REQ )
/*TODO*///		{
/*TODO*///			pio->int_state[0] |= Z80_INT_IEO;
/*TODO*///		} if( pio->int_state[0] == 0 )
/*TODO*///		{
/*TODO*///			/* port B */
/*TODO*///			ch = 1;
/*TODO*///			if( pio->int_state[1] == Z80_INT_REQ )
/*TODO*///			{
/*TODO*///				pio->int_state[1] |= Z80_INT_IEO;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				logerror("PIO entry INT : non IRQ\n");
/*TODO*///				ch = 0;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		z80pio_interrupt_check( pio );
/*TODO*///		return pio->vector[ch];
/*TODO*///	}
/*TODO*///	
/*TODO*///	void z80pio_reti( int which )
/*TODO*///	{
/*TODO*///		z80pio *pio = pios + which;
/*TODO*///	
/*TODO*///		if( pio->int_state[0] & Z80_INT_IEO )
/*TODO*///		{
/*TODO*///			pio->int_state[0] &= ~Z80_INT_IEO;
/*TODO*///		} else if( pio->int_state[1] & Z80_INT_IEO )
/*TODO*///		{
/*TODO*///			pio->int_state[1] &= ~Z80_INT_IEO;
/*TODO*///		}
/*TODO*///		/* set next interrupt stattus */
/*TODO*///		z80pio_interrupt_check( pio );
/*TODO*///	}
/*TODO*///	
/*TODO*///	/* z80pio port write */
/*TODO*///	void z80pio_p_w( int which , int ch , int data )
/*TODO*///	{
/*TODO*///		z80pio *pio = pios + which;
/*TODO*///	
/*TODO*///		if( ch ) ch = 1;
/*TODO*///	
/*TODO*///		pio->in[ch]  = data;
/*TODO*///		switch( pio->mode[ch] ){
/*TODO*///		case PIO_MODE0:
/*TODO*///			logerror("PIO-%c OUTPUT mode and data write\n",'A'+ch );
/*TODO*///			break;
/*TODO*///		case PIO_MODE2:	/* only port A */
/*TODO*///			ch = 1;		/* handshake and IRQ is use portB */
/*TODO*///		case PIO_MODE1:
/*TODO*///			z80pio_set_rdy(pio, ch, 0);
/*TODO*///			z80pio_check_irq( pio , ch );
/*TODO*///			break;
/*TODO*///		case PIO_MODE3:
/*TODO*///			/* irq check */
/*TODO*///			z80pio_check_irq( pio , ch );
/*TODO*///			break;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	/* z80pio port read */
/*TODO*///	int z80pio_p_r( int which , int ch )
/*TODO*///	{
/*TODO*///		z80pio *pio = pios + which;
/*TODO*///	
/*TODO*///		if( ch ) ch = 1;
/*TODO*///	
/*TODO*///		switch( pio->mode[ch] ){
/*TODO*///		case PIO_MODE2:		/* port A only */
/*TODO*///		case PIO_MODE0:
/*TODO*///			z80pio_set_rdy(pio, ch, 0);
/*TODO*///			z80pio_check_irq( pio , ch );
/*TODO*///			break;
/*TODO*///		case PIO_MODE1:
/*TODO*///			logerror("PIO-%c INPUT mode and data read\n",'A'+ch );
/*TODO*///			break;
/*TODO*///		case PIO_MODE3:
/*TODO*///			/*     input bits                , output bits                */
/*TODO*///			return (pio->in[ch]&pio->dir[ch])|(pio->out[ch]&~pio->dir[ch]);
/*TODO*///		}
/*TODO*///		return pio->out[ch];
/*TODO*///	}
/*TODO*///	
/*TODO*///	/* for mame interface */
/*TODO*///	
/*TODO*///	void z80pio_0_reset (void) { z80pio_reset (0); }
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr z80pio_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		if(offset&1) z80pio_c_w(0,(offset/2)&1,data);
/*TODO*///		else         z80pio_d_w(0,(offset/2)&1,data);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr z80pio_0_r  = new ReadHandlerPtr() { public int handler(int offset)
/*TODO*///	{
/*TODO*///		return (offset&1) ? z80pio_c_r(0,(offset/2)&1) : z80pio_d_r(0,(offset/2)&1);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr z80pioA_0_p_w = new WriteHandlerPtr() {public void handler(int offset, int data) { z80pio_p_w(0,0,data);   } };
/*TODO*///	public static WriteHandlerPtr z80pioB_0_p_w = new WriteHandlerPtr() {public void handler(int offset, int data) { z80pio_p_w(0,1,data);   } };
/*TODO*///	public static ReadHandlerPtr z80pioA_0_p_r  = new ReadHandlerPtr() { public int handler(int offset)           { return z80pio_p_r(0,0); } };
/*TODO*///	public static ReadHandlerPtr z80pioB_0_p_r  = new ReadHandlerPtr() { public int handler(int offset)           { return z80pio_p_r(0,1); } };
/*TODO*///	
/*TODO*///	static void z80pio_update_strobe(int which, int ch, int state)
/*TODO*///	{
/*TODO*///		z80pio *pio = pios + which;
/*TODO*///	
/*TODO*///		if (ch) ch=1;
/*TODO*///	
/*TODO*///		switch (pio->mode[ch])
/*TODO*///		{
/*TODO*///			/* output mode */
/*TODO*///			case PIO_MODE0:
/*TODO*///			{
/*TODO*///				/* ensure valid */
/*TODO*///				state = state & 0x01;
/*TODO*///	
/*TODO*///				/* strobe changed state? */
/*TODO*///				if ((pio->strobe[ch]^state)!=0)
/*TODO*///				{
/*TODO*///					/* yes */
/*TODO*///					if (state!=0)
/*TODO*///					{
/*TODO*///						/* positive edge */
/*TODO*///						logerror("PIO-%c positive strobe\n",'A'+ch );
/*TODO*///						/* ready is now inactive */
/*TODO*///						z80pio_set_rdy(pio, ch, 0);
/*TODO*///	
/*TODO*///						/* int enabled? */
/*TODO*///						if (pio->enable[ch] & PIO_INT_ENABLE)
/*TODO*///						{
/*TODO*///							/* trigger an int request */
/*TODO*///							pio->int_state[ch] |= Z80_INT_REQ;
/*TODO*///						}
/*TODO*///					}
/*TODO*///				}
/*TODO*///	
/*TODO*///				/* store strobe state */
/*TODO*///				pio->strobe[ch] = state;
/*TODO*///	
/*TODO*///				/* check interrupt */
/*TODO*///				z80pio_interrupt_check( pio );
/*TODO*///			}
/*TODO*///			break;
/*TODO*///	
/*TODO*///			default:
/*TODO*///				break;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	/* set /astb input */
/*TODO*///	/* /astb is active low */
/*TODO*///	/* output mode: a positive edge is used by peripheral to acknowledge
/*TODO*///	the receipt of data */
/*TODO*///	/* input mode: strobe is used by peripheral to load data from the peripheral
/*TODO*///	into port a input register, data loaded into pio when signal is active */
/*TODO*///	void	z80pio_astb_w(int which, int state)
/*TODO*///	{
/*TODO*///		z80pio_update_strobe(which, 0, state);
/*TODO*///	}
/*TODO*///	
/*TODO*///	/* set bstb input */
/*TODO*///	void	z80pio_bstb_w(int which, int state)
/*TODO*///	{
/*TODO*///		z80pio_update_strobe(which, 1, state);
/*TODO*///	}
}
