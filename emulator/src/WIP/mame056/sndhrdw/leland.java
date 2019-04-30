/***************************************************************************

	Cinemat/Leland driver

	Leland sound hardware
	driver by Aaron Giles and Paul Leaman

	-------------------------------------------------------------------

	1st generation sound hardware was controlled by the master Z80.
	It drove an AY-8910/AY-8912 pair for music. It also had two DACs
	that were driven by the video refresh. At the end of each scanline
	there are 8-bit DAC samples that can be enabled via the output
	ports on the AY-8910. The DACs run at a fixed frequency of 15.3kHz,
	since they are clocked once each scanline.

	-------------------------------------------------------------------

	2nd generation sound hardware was used in Redline Racer. It
	consisted of an 80186 microcontroller driving 8 8-bit DACs. The
	frequency of the DACs were controlled by one of 3 Intel 8254
	programmable interval timers (PITs):

		DAC number	Clock source
		----------	-----------------
			0		8254 PIT 1 output 0
			1		8254 PIT 1 output 1
			2		8254 PIT 1 output 2
			3		8254 PIT 2 output 0
			4		8254 PIT 2 output 1
			5-7		8254 PIT 3 output 0

	The clock outputs for each DAC can be read, and are polled to
	determine when data should be updated on the chips. The 80186's
	two DMA channels are generally used to drive the first two DACs,
	with the remaining 6 DACs being fed manually via polling.

	-------------------------------------------------------------------

	3rd generation sound hardware appeared in the football games
	(Quarterback, AAFB) and the later games up through Pigout. This
	variant is closely based on the Redline Racer sound system, but
	they took out two of the DACs and replaced them with a higher
	resolution (10-bit) DAC. The driving clocks have been rearranged
	a bit, and the number of PITs reduced from 3 to 2:

		DAC number	Clock source
		----------	-----------------
			0		8254 PIT 1 output 0
			1		8254 PIT 1 output 1
			2		8254 PIT 1 output 2
			3		8254 PIT 2 output 0
			4		8254 PIT 2 output 1
			5		8254 PIT 2 output 2
			10-bit	80186 timer 0

	Like the 2nd generation board, the first two DACs are driven via
	the DMA channels, and the remaining 5 DACs are polled.

	-------------------------------------------------------------------

	4th generation sound hardware showed up in Ataxx, Indy Heat, and
	World Soccer Finals. For this variant, they removed one more PIT
	and 3 of the 8-bit DACs, and added a YM2151 music chip and an
	externally-fed 8-bit DAC.

		DAC number	Clock source
		----------	-----------------
			0		8254 PIT 1 output 0
			1		8254 PIT 1 output 1
			2		8254 PIT 1 output 2
			10-bit	80186 timer 0
			ext		80186 timer 1

	The externally driven DACs have registers for a start/stop address
	and triggers to control the clocking.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.sndhrdw;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;

import static mame056.sndintrfH.*;
import static mame056.mame.Machine;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.timerH.*;
import static mame056.timer.*;
import static mame056.cpuintrf.*;

import static mame056.sound.samples.*;
import static mame056.sound.streams.*;
import static mame056.sound.samplesH.*;
import static mame056.sound.mixer.*;
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;
import static mame037b11.sound.mixer.*;
import static mame056.usrintrf.usrintf_showmessage;

import static mame056.memory.*;
import static mame056.memoryH.*;

// refactor
import static arcadeflex036.osdepend.logerror;

import static common.libc.cstring.memset;
import static mame056.cpu.z80.z80H.*;
import static mame056.sound.streams.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.sound._2151intf.*;

public class leland
{
	
	
	/*************************************
	 *
	 *	1st generation sound
	 *
	 *************************************/
	
	public static int DAC_BUFFER_SIZE = 1024;
	public static int DAC_BUFFER_MASK = (DAC_BUFFER_SIZE - 1);
	
	static UBytePtr[] dac_buffer = new UBytePtr[2];
        static {
            for (int i=0 ; i<2 ; i++)
                dac_buffer[i] = new UBytePtr();
        }
	static int[] dac_bufin=new int[2];
	static int[] dac_bufout=new int[2];
	
	static int dac_stream;
	
	public static StreamInitPtr leland_update = new StreamInitPtr() {
            public void handler(int param, ShortPtr buffer, int length) {
                int dacnum;
	
		/* reset the buffer */
		memset(buffer, 0, length);
		for (dacnum = 0; dacnum < 2; dacnum++)
		{
			int bufout = dac_bufout[dacnum];
			int count = (dac_bufin[dacnum] - bufout) & DAC_BUFFER_MASK;
	
			if (count > 300)
			{
				UBytePtr base = new UBytePtr(dac_buffer[dacnum]);
				int i;
	
				for (i = 0; i < length && count > 0; i++, count--)
				{
					buffer.write(i, (short) (buffer.read(i) + (base.read(bufout) - 0x80) * 0x40));
					bufout = (bufout + 1) & DAC_BUFFER_MASK;
				}
				dac_bufout[dacnum] = bufout;
			}
		}
            }
        };
	
	public static ShStartPtr leland_sh_start = new ShStartPtr() {
            public int handler(MachineSound msound) {
                /* reset globals */
		dac_buffer[0] = new UBytePtr();
                dac_buffer[1] = new UBytePtr();
		dac_bufin[0]  = dac_bufin[1]  = 0;
		dac_bufout[0] = dac_bufout[1] = 0;
	
		/* skip if no sound */
		if (Machine.sample_rate == 0)
			return 0;
	
		/* allocate the stream */
		dac_stream = stream_init("Onboard DACs", 50, 256*60, 0, leland_update);
	
		/* allocate memory */
		dac_buffer[0] = new UBytePtr(DAC_BUFFER_SIZE);
		dac_buffer[1] = new UBytePtr(DAC_BUFFER_SIZE);
		if ((dac_buffer[0]==null) || (dac_buffer[1]==null))
		{
			dac_buffer[0] = dac_buffer[1] = null;
			return 1;
		}
	
		return 0;
            }
        };
	
	
	public static ShStopPtr leland_sh_stop = new ShStopPtr() {
            public void handler() {
                if (dac_buffer[0] != null)
			dac_buffer[0]=null;
		if (dac_buffer[1] != null)
			dac_buffer[1]=null;
		dac_buffer[0] = dac_buffer[1] = null;
            }
        };
	
	public static void leland_dac_update(int dacnum, int sample)
	{
		UBytePtr buffer = new UBytePtr(dac_buffer[dacnum]);
                System.out.println(dac_buffer[dacnum]);
		int bufin = dac_bufin[dacnum];
	
		/* skip if nothing */
		if (buffer == null)
			return;
	
		/* copy data from VRAM */
		buffer.write(bufin, sample);
		bufin = (bufin + 1) & DAC_BUFFER_MASK;
	
		/* update the buffer */
		dac_bufin[dacnum] = bufin;
	}
	
	
	
	/*************************************
	 *
	 *	2nd-4th generation sound
	 *
	 *************************************/
	
	/*
        #define LOG_INTERRUPTS		0
	#define LOG_DMA				0
	#define LOG_SHORTAGES		0
	#define LOG_TIMER			0
	#define LOG_COMM			0
	#define LOG_PORTS			0
	#define LOG_DAC				0
	#define LOG_EXTERN			0
	#define LOG_PIT				0
	#define LOG_OPTIMIZATION	0
	*/
	
	/* according to the Intel manual, external interrupts are not latched */
	/* however, I cannot get this system to work without latching them */
	public static int LATCH_INTS = 1;
	
	public static int DAC_VOLUME_SCALE = 4;
	public static int CPU_RESUME_TRIGGER = 7123;
	
	
	static int dma_stream;
	static int nondma_stream;
	static int extern_stream;
	
	static UBytePtr ram_base=new UBytePtr();
	static int has_ym2151;
	static int is_redline;
	
	static int last_control;
	static int clock_active;
	static int clock_tick;
	
	static int[] sound_command=new int[2];
	static int sound_response;
	
	static int ext_start;
	static int ext_stop;
	static int ext_active;
	static UBytePtr ext_base=new UBytePtr();
	
	static UBytePtr  active_mask = new UBytePtr();
	static int total_reads;
	
	public static class mem_state
	{
		public int	lower;
		public int	upper;
		public int	middle;
		public int	middle_size;
		public int	peripheral;
	};
	
	public static class timer_state
	{
		public int	control;
		public int	maxA;
		public int	maxB;
		public int	count;
		public timer_entry int_timer;
		public timer_entry time_timer;
		public double	last_time;
	};
	
	public static class dma_state
	{
		public int	source;
		public int	dest;
		public int	count;
		public int	control;
		public int	finished;
		public timer_entry finish_timer;
	};
	
	public static class intr_state
	{
		public int	pending;
		public int	ack_mask;
		public int	priority_mask;
		public int	in_service;
		public int	request;
		public int	status;
		public int	poll_status;
		public int	timer;
		public int[]	dma=new int[2];
		public int[]	ext=new int[4];
	};
	
	public static class i186_state
	{
		public timer_state[]	timer   = new timer_state[3];
		public dma_state[]	dma     = new dma_state[2];
		public intr_state	intr    = new intr_state();
		public mem_state	mem     = new mem_state();
	};
        
        public static i186_state i186 = new i186_state();
        
        static {
            for (int i=0 ; i<2 ; i++)
                i186.dma[i] = new dma_state();
        }
	
	
	//public static final int  DAC_BUFFER_SIZE = 1024;
	public static final int  DAC_BUFFER_SIZE_MASK = (DAC_BUFFER_SIZE - 1);
        
	public static class dac_state
	{
		public int	value;
		public int	volume;
		public int	frequency;
		public int	step;
		public int	fraction;
	
		public short[]	buffer=new short[DAC_BUFFER_SIZE];
		public int	bufin;
		public int	bufout;
		public int	buftarget;
	};
        
        static dac_state[] dac = new dac_state[8];
        
        static {
            for (int i=0 ; i<8 ; i++)
                dac[i] = new dac_state();
        }
	
	public static class counter_state
	{
		public timer_entry timer;
		public int count;
		public int mode;
		public int readbyte;
		public int writebyte;
	};
        
        static counter_state[] counter = new counter_state[9];
	
	//static void set_dac_frequency(int which, int frequency);
	
	
	
	
	/*************************************
	 *
	 *	Manual DAC sound generation
	 *
	 *************************************/
	
	public static StreamInitPtr leland_i186_dac_update = new StreamInitPtr() {
            public void handler(int param, ShortPtr buffer, int length) {
                int i, j, start, stop;
	
		/*TODO*///if (LOG_SHORTAGES) logerror("----\n");
	
		/* reset the buffer */
		//memset(buffer, 0, length * sizeof(INT16));
                buffer = new ShortPtr(length * 2);
	
		/* if we're redline racer, we have more DACs */
		if (is_redline == 0){
			start = 2; 
                        stop = 7;
                } else {
			start = 0; 
                        stop = 8;
                }
	
		/* loop over manual DAC channels */
		for (i = start; i < stop; i++)
		{
			dac_state d = dac[i];
			int count = (d.bufin - d.bufout) & DAC_BUFFER_SIZE_MASK;
	
			/* if we have data, process it */
			if (count > 0)
			{
				ShortPtr base = new ShortPtr(d.buffer);
				int source = d.bufout;
				int frac = d.fraction;
				int step = d.step;
	
				/* sample-rate convert to the output frequency */
				for (j = 0; j < length && count > 0; j++)
				{
					buffer.write(j, (short) (buffer.read(j)+ base.read(source)));
					frac += step;
					source += frac >> 24;
					count -= frac >> 24;
					frac &= 0xffffff;
					source &= DAC_BUFFER_SIZE_MASK;
				}
	
				/*TODO*///if (LOG_SHORTAGES && j < length)
				/*TODO*///	logerror("DAC #%d short by %d/%d samples\n", i, length - j, length);
	
				/* update the DAC state */
				d.fraction = frac;
				d.bufout = source;
			}
	
			/* update the clock status */
			if (count < d.buftarget)
			{
				/*TODO*///if (LOG_OPTIMIZATION) logerror("  - trigger due to clock active in update\n");
				cpu_trigger.handler(CPU_RESUME_TRIGGER);
				clock_active |= 1 << i;
			}
		}
            }
        };
		
	
	/*************************************
	 *
	 *	DMA-based DAC sound generation
	 *
	 *************************************/
	
	public static StreamInitPtr leland_i186_dma_update = new StreamInitPtr() {
            public void handler(int param, ShortPtr buffer, int length) {
                int i, j;
	
		/* reset the buffer */
		memset(buffer, 0, length);
	
		/* loop over DMA buffers */
		for (i = 0; i < 2; i++)
		{
			dma_state d = i186.dma[i];
	
			/* check for enabled DMA */
			if ((d.control & 0x0002) != 0)
			{
				/* make sure the parameters meet our expectations */
				if ((d.control & 0xfe00) != 0x1600)
				{
					logerror("Unexpected DMA control %02X\n", d.control);
				}
				else if ((is_redline==0) && (((d.dest & 1)!=0) || (d.dest & 0x3f) > 0x0b))
				{
					logerror("Unexpected DMA destination %02X\n", d.dest);
				}
				else if ((is_redline!=0) && (d.dest & 0xf000) != 0x4000 && (d.dest & 0xf000) != 0x5000)
				{
					logerror("Unexpected DMA destination %02X\n", d.dest);
				}
	
				/* otherwise, we're ready for liftoff */
				else
				{
					UBytePtr base = new UBytePtr(memory_region(REGION_CPU3));
					int source = d.source;
					int count = d.count;
					int which, frac, step, volume;
	
					/* adjust for redline racer */
					if (is_redline == 0)
						which = (d.dest & 0x3f) / 2;
					else
						which = (d.dest >> 9) & 7;
	
					frac = dac[which].fraction;
					step = dac[which].step;
					volume = dac[which].volume;
	
					/* sample-rate convert to the output frequency */
					for (j = 0; j < length && count > 0; j++)
					{
						buffer.write(j, (short) (buffer.read(j)+ ((int)base.read(source) - 0x80) * volume));
						frac += step;
						source += frac >> 24;
						count -= frac >> 24;
						frac &= 0xffffff;
					}
	
					/* update the DMA state */
					if (count > 0)
					{
						d.source = source;
						d.count = count;
					}
					else
					{
						/* let the timer callback actually mark the transfer finished */
						d.source = source + count - 1;
						d.count = 1;
						d.finished = 1;
					}
	
					/*TODO*///if (LOG_DMA) logerror("DMA Generated %d samples - new count = %04X, source = %04X\n", j, d.count, d.source);
	
					/* update the DAC state */
					dac[which].fraction = frac;
				}
			}
		}
            }
        };
		
	
	/*************************************
	 *
	 *	Externally-driven DAC sound generation
	 *
	 *************************************/
	
	public static StreamInitPtr leland_i186_extern_update = new StreamInitPtr() {
            public void handler(int param, ShortPtr buffer, int length) {
                dac_state d = dac[7];
		int count = ext_stop - ext_start;
		int j;
	
		/* reset the buffer */
		memset(buffer, 0, length * 2);
	
		/* if we have data, process it */
		if (count > 0 && ext_active!=0)
		{
			int source = ext_start;
			int frac = d.fraction;
			int step = d.step;
	
			/* sample-rate convert to the output frequency */
			for (j = 0; j < length && count > 0; j++)
			{
				buffer.write(j, (short) (buffer.read(j)+ (ext_base.read(source) - 0x80) * d.volume));
				frac += step;
				source += frac >> 24;
				count -= frac >> 24;
				frac &= 0xffffff;
			}
	
			/* update the DAC state */
			d.fraction = frac;
			ext_start = source;
		}
            }
        };
	
	
	/*************************************
	 *
	 *	Sound initialization
	 *
	 *************************************/
	
	public static ShStartPtr leland_i186_sh_start = new ShStartPtr() {
            public int handler(MachineSound msound) {
                int i;
	
		/* bail if nothing to play */
		if (Machine.sample_rate == 0)
			return 0;
	
		/* determine which sound hardware is installed */
		has_ym2151 = 0;
		for (i = 0; i < MAX_SOUND; i++)
			if (Machine.drv.sound[i].sound_type == SOUND_YM2151)
				has_ym2151 = 1;
	
		/* allocate separate streams for the DMA and non-DMA DACs */
		dma_stream = stream_init("80186 DMA-driven DACs", 100, Machine.sample_rate, 0, leland_i186_dma_update);
		nondma_stream = stream_init("80186 manually-driven DACs", 100, Machine.sample_rate, 0, leland_i186_dac_update);
	
		/* if we have a 2151, install an externally driven DAC stream */
		if (has_ym2151 != 0)
		{
			ext_base = memory_region(REGION_SOUND1);
			extern_stream = stream_init("80186 externally-driven DACs", 100, Machine.sample_rate, 0, leland_i186_extern_update);
		}
	
		/* by default, we're not redline racer */
		is_redline = 0;
		return 0;
            }
        };
	
	
	public static ShStartPtr redline_i186_sh_start = new ShStartPtr() {
            public int handler(MachineSound msound) {
                int result = leland_i186_sh_start.handler(msound);
		is_redline = 1;
		return result;
            }
        };
	
	
	static void leland_i186_reset()
	{
		/* kill any live timers */
		if (i186.timer[0].int_timer != null) timer_remove(i186.timer[0].int_timer);
		if (i186.timer[1].int_timer != null) timer_remove(i186.timer[1].int_timer);
		if (i186.timer[2].int_timer != null) timer_remove(i186.timer[2].int_timer);
		if (i186.timer[0].time_timer != null) timer_remove(i186.timer[0].time_timer);
		if (i186.timer[1].time_timer != null) timer_remove(i186.timer[1].time_timer);
		if (i186.timer[2].time_timer != null) timer_remove(i186.timer[2].time_timer);
		if (i186.dma[0].finish_timer != null) timer_remove(i186.dma[0].finish_timer);
		if (i186.dma[1].finish_timer != null) timer_remove(i186.dma[1].finish_timer);
	
		/* reset the i186 state */
		//memset(i186, 0, sizeof(i186));
                i186 = new i186_state();
	
		/* reset the interrupt state */
		i186.intr.priority_mask	= 0x0007;
		i186.intr.timer 		= 0x000f;
		i186.intr.dma[0]		= 0x000f;
		i186.intr.dma[1]		= 0x000f;
		i186.intr.ext[0]		= 0x000f;
		i186.intr.ext[1]		= 0x000f;
		i186.intr.ext[2]		= 0x000f;
		i186.intr.ext[3]		= 0x000f;
	
		/* reset the DAC and counter states as well */
		//memset(&dac, 0, sizeof(dac));
                dac = new dac_state[8];
		//memset(&counter, 0, sizeof(counter));
                counter = new counter_state[9];
	
		/* send a trigger in case we're suspended */
		/*TODO*///if (LOG_OPTIMIZATION) logerror("  - trigger due to reset\n");
		cpu_trigger.handler(CPU_RESUME_TRIGGER);
		total_reads = 0;
	}
	
	
	public static void leland_i186_sound_init()
	{
		/* RAM is multiply mapped in the first 128k of address space */
		cpu_setbank(6, ram_base);
		cpu_setbank(7, ram_base);
	
		/* reset the I86 registers */
		//memset(&i186, 0, sizeof(i186));
                i186 = null;
		leland_i186_reset();
	
		/* reset our internal stuff */
		last_control = 0xf8;
		clock_active = 0;
	
		/* reset the external DAC */
		ext_start = 0;
		ext_stop = 0;
		ext_active = 0;
	}
	
	
	
	/*************************************
	 *
	 *	80186 interrupt controller
	 *
	 *************************************/
	
	static int int_callback(int line)
	{
		/*TODO*///if (LOG_INTERRUPTS) logerror("(%f) **** Acknowledged interrupt vector %02X\n", timer_get_time(), i186.intr.poll_status & 0x1f);
	
		/* clear the interrupt */
		/*TODO*///i86_set_irq_line(0, CLEAR_LINE);
		i186.intr.pending = 0;
	
		/* clear the request and set the in-service bit */
	if (LATCH_INTS != 0)
		i186.intr.request &= ~i186.intr.ack_mask;
	else
		i186.intr.request &= ~(i186.intr.ack_mask & 0x0f);
	
		i186.intr.in_service |= i186.intr.ack_mask;
		if (i186.intr.ack_mask == 0x0001)
		{
			switch (i186.intr.poll_status & 0x1f)
			{
				case 0x08:	i186.intr.status &= ~0x01;	break;
				case 0x12:	i186.intr.status &= ~0x02;	break;
				case 0x13:	i186.intr.status &= ~0x04;	break;
			}
		}
		i186.intr.ack_mask = 0;
	
		/* a request no longer pending */
		i186.intr.poll_status &= ~0x8000;
	
		/* return the vector */
		return i186.intr.poll_status & 0x1f;
	}
	
	
	static void update_interrupt_state()
	{
                int generate_int = 0;
		int i, j, new_vector = 0;
	
		/*TODO*///if (LOG_INTERRUPTS) logerror("update_interrupt_status: req=%02X stat=%02X serv=%02X\n", i186.intr.request, i186.intr.status, i186.intr.in_service);
	
		/* loop over priorities */
		for (i = 0; i <= i186.intr.priority_mask; i++)
		{
			/* note: by checking 4 bits, we also verify that the mask is off */
			if ((i186.intr.timer & 15) == i)
			{
				/* if we're already servicing something at this level, don't generate anything new */
				if ((i186.intr.in_service & 0x01) != 0)
					return;
	
				/* if there's something pending, generate an interrupt */
				if ((i186.intr.status & 0x07) != 0)
				{
					if ((i186.intr.status & 1) != 0)
						new_vector = 0x08;
					else if ((i186.intr.status & 2) != 0)
						new_vector = 0x12;
					else if ((i186.intr.status & 4) != 0)
						new_vector = 0x13;
					else
						usrintf_showmessage("Invalid timer interrupt!");
	
					/* set the clear mask and generate the int */
					i186.intr.ack_mask = 0x0001;
					generate_int = 1; break;
				}
			}
	
			/* check DMA interrupts */
			for (j = 0; j < 2; j++)
				if ((i186.intr.dma[j] & 15) == i)
				{
					/* if we're already servicing something at this level, don't generate anything new */
					if ((i186.intr.in_service & (0x04 << j)) != 0)
						return;
	
					/* if there's something pending, generate an interrupt */
					if ((i186.intr.request & (0x04 << j)) != 0)
					{
						new_vector = 0x0a + j;
	
						/* set the clear mask and generate the int */
						i186.intr.ack_mask = 0x0004 << j;
						generate_int = 1; break;
					}
				}
	
			/* check external interrupts */
			for (j = 0; j < 4; j++)
				if ((i186.intr.ext[j] & 15) == i)
				{
					/* if we're already servicing something at this level, don't generate anything new */
					if ((i186.intr.in_service & (0x10 << j)) != 0)
						return;
	
					/* if there's something pending, generate an interrupt */
					if ((i186.intr.request & (0x10 << j)) != 0)
					{
						/* otherwise, generate an interrupt for this request */
						new_vector = 0x0c + j;
	
						/* set the clear mask and generate the int */
						i186.intr.ack_mask = 0x0010 << j;
						generate_int = 1; break;
					}
				}
		}
		if (generate_int == 0)
                    return;
	
	
		/* generate the appropriate interrupt */
		i186.intr.poll_status = 0x8000 | new_vector;
		if (i186.intr.pending == 0)
			cpu_set_irq_line(2, 0, ASSERT_LINE);
		i186.intr.pending = 1;
		cpu_trigger.handler(CPU_RESUME_TRIGGER);
		/*TODO*///if (LOG_OPTIMIZATION) logerror("  - trigger due to interrupt pending\n");
		/*TODO*///if (LOG_INTERRUPTS) logerror("(%f) **** Requesting interrupt vector %02X\n", timer_get_time(), new_vector);
	}
	
	
	static void handle_eoi(int data)
	{
		int i, j;
	
		/* specific case */
		if ((data & 0x8000) == 0)
		{
			/* turn off the appropriate in-service bit */
			switch (data & 0x1f)
			{
				case 0x08:	i186.intr.in_service &= ~0x01;	break;
				case 0x12:	i186.intr.in_service &= ~0x01;	break;
				case 0x13:	i186.intr.in_service &= ~0x01;	break;
				case 0x0a:	i186.intr.in_service &= ~0x04;	break;
				case 0x0b:	i186.intr.in_service &= ~0x08;	break;
				case 0x0c:	i186.intr.in_service &= ~0x10;	break;
				case 0x0d:	i186.intr.in_service &= ~0x20;	break;
				case 0x0e:	i186.intr.in_service &= ~0x40;	break;
				case 0x0f:	i186.intr.in_service &= ~0x80;	break;
				default:	logerror("%05X:ERROR - 80186 EOI with unknown vector %02X\n", cpu_get_pc(), data & 0x1f);
			}
			/*TODO*///if (LOG_INTERRUPTS) logerror("(%f) **** Got EOI for vector %02X\n", timer_get_time(), data & 0x1f);
		}
	
		/* non-specific case */
		else
		{
			/* loop over priorities */
			for (i = 0; i <= 7; i++)
			{
				/* check for in-service timers */
				if ((i186.intr.timer & 7) == i && (i186.intr.in_service & 0x01)!=0)
				{
					i186.intr.in_service &= ~0x01;
					/*TODO*///if (LOG_INTERRUPTS) logerror("(%f) **** Got EOI for timer\n", timer_get_time());
					return;
				}
	
				/* check for in-service DMA interrupts */
				for (j = 0; j < 2; j++)
					if ((i186.intr.dma[j] & 7) == i && (i186.intr.in_service & (0x04 << j))!=0)
					{
						i186.intr.in_service &= ~(0x04 << j);
						/*TODO*///if (LOG_INTERRUPTS) logerror("(%f) **** Got EOI for DMA%d\n", timer_get_time(), j);
						return;
					}
	
				/* check external interrupts */
				for (j = 0; j < 4; j++)
					if ((i186.intr.ext[j] & 7) == i && (i186.intr.in_service & (0x10 << j))!=0)
					{
						i186.intr.in_service &= ~(0x10 << j);
						/*TODO*///if (LOG_INTERRUPTS) logerror("(%f) **** Got EOI for INT%d\n", timer_get_time(), j);
						return;
					}
			}
		}
	}
	
	
	
	/*************************************
	 *
	 *	80186 internal timers
	 *
	 *************************************/
	
	public static timer_callback internal_timer_int = new timer_callback() {
            public void handler(int which) {
                timer_state t = i186.timer[which];
	
		/*TODO*///if (LOG_TIMER) logerror("Hit interrupt callback for timer %d\n", which);
	
		/* set the max count bit */
		t.control |= 0x0020;
	
		/* request an interrupt */
		if ((t.control & 0x2000) != 0)
		{
			i186.intr.status |= 0x01 << which;
			update_interrupt_state();
			/*TODO*///if (LOG_TIMER) logerror("  Generating timer interrupt\n");
		}
	
		/* if we're continuous, reset */
		if ((t.control & 0x0001) != 0)
		{
			int count = t.maxA != 0 ? t.maxA : 0x10000;
			t.int_timer = timer_set((double)count * TIME_IN_HZ(2000000), which, internal_timer_int);
			/*TODO*///if (LOG_TIMER) logerror("  Repriming interrupt\n");
		}
		else
			t.int_timer = null;
            }
        };
	
	
	static void internal_timer_sync(int which)
	{
		timer_state t = i186.timer[which];
	
		/* if we have a timing timer running, adjust the count */
		if (t.time_timer != null)
		{
			double current_time = timer_timeelapsed(t.time_timer);
			int net_clocks = (int)((current_time - t.last_time) * 2000000.);
			t.last_time = current_time;
	
			/* set the max count bit if we passed the max */
			if ((int)t.count + net_clocks >= t.maxA)
				t.control |= 0x0020;
	
			/* set the new count */
			if (t.maxA != 0)
				t.count = (t.count + net_clocks) % t.maxA;
			else
				t.count = t.count + net_clocks;
		}
	}
	
	
	static void internal_timer_update(int which, int new_count, int new_maxA, int new_maxB, int new_control)
	{
		timer_state t = i186.timer[which];
		int update_int_timer = 0;
	
		/* if we have a new count and we're on, update things */
		if (new_count != -1)
		{
			if ((t.control & 0x8000) != 0)
			{
				internal_timer_sync(which);
				update_int_timer = 1;
			}
			t.count = new_count;
		}
	
		/* if we have a new max and we're on, update things */
		if (new_maxA != -1 && new_maxA != t.maxA)
		{
			if ((t.control & 0x8000) != 0)
			{
				internal_timer_sync(which);
				update_int_timer = 1;
			}
			t.maxA = new_maxA;
			if (new_maxA == 0) new_maxA = 0x10000;
	
			/* redline racer controls nothing externally? */
			if (is_redline !=0)
				;
	
			/* on the common board, timer 0 controls the 10-bit DAC frequency */
			else if (which == 0)
				set_dac_frequency(6, 2000000 / new_maxA);
	
			/* timer 1 controls the externally driven DAC on Indy Heat/WSF */
			else if (which == 1 && has_ym2151!=0)
				set_dac_frequency(7, 2000000 / (new_maxA * 2));
		}
	
		/* if we have a new max and we're on, update things */
		if (new_maxB != -1 && new_maxB != t.maxB)
		{
			if ((t.control & 0x8000) != 0)
			{
				internal_timer_sync(which);
				update_int_timer = 1;
			}
			t.maxB = new_maxB;
			if (new_maxB == 0) new_maxB = 0x10000;
	
			/* timer 1 controls the externally driven DAC on Indy Heat/WSF */
			/* they alternate the use of maxA and maxB in a way that makes no */
			/* sense according to the 80186 documentation! */
			if (which == 1 && has_ym2151!=0)
				set_dac_frequency(7, 2000000 / (new_maxB * 2));
		}
	
		/* handle control changes */
		if (new_control != -1)
		{
			int diff;
	
			/* merge back in the bits we don't modify */
			new_control = (new_control & ~0x1fc0) | (t.control & 0x1fc0);
	
			/* handle the /INH bit */
			if ((new_control & 0x4000)==0)
				new_control = (new_control & ~0x8000) | (t.control & 0x8000);
			new_control &= ~0x4000;
	
			/* check for control bits we don't handle */
			diff = new_control ^ t.control;
			if ((diff & 0x001c) != 0)
				logerror("%05X:ERROR! - unsupported timer mode %04X\n", new_control);
	
			/* if we have real changes, update things */
			if (diff != 0)
			{
				/* if we're going off, make sure our timers are gone */
				if (((diff & 0x8000)!=0) && ((new_control & 0x8000)==0))
				{
					/* compute the final count */
					internal_timer_sync(which);
	
					/* nuke the timer and force the interrupt timer to be recomputed */
					if (t.time_timer != null)
						timer_remove(t.time_timer);
					t.time_timer = null;
					update_int_timer = 1;
				}
	
				/* if we're going on, start the timers running */
				else if (((diff & 0x8000)!=0) && ((new_control & 0x8000)!=0))
				{
					/* start the timing */
					t.time_timer = timer_set(TIME_NEVER, 0, null);
					update_int_timer = 1;
				}
	
				/* if something about the interrupt timer changed, force an update */
				if (((diff & 0x8000)==0) && ((diff & 0x2000)!=0))
				{
					internal_timer_sync(which);
					update_int_timer = 1;
				}
			}
	
			/* set the new control register */
			t.control = new_control;
		}
	
		/* update the interrupt timer */
	
		/* kludge: the YM2151 games sometimes crank timer 1 really high, and leave interrupts */
		/* enabled, even though the handler for timer 1 does nothing. To alleviate this, we */
		/* just ignore it */
		if (has_ym2151==0 || which != 1)
			if (update_int_timer != 0)
			{
				if (t.int_timer != null)
					timer_remove(t.int_timer);
				if (((t.control & 0x8000)!=0) && ((t.control & 0x2000)!=0))
				{
					int diff = t.maxA - t.count;
					if (diff <= 0) diff += 0x10000;
					t.int_timer = timer_set((double)diff * TIME_IN_HZ(2000000), which, internal_timer_int);
					/*TODO*///if (LOG_TIMER) logerror("Set interrupt timer for %d\n", which);
				}
				else
					t.int_timer = null;
			}
	}
	
	
	
	/*************************************
	 *
	 *	80186 internal DMA
	 *
	 *************************************/
	
	public static timer_callback dma_timer_callback = new timer_callback() {
            public void handler(int which) {
                dma_state d = i186.dma[which];
	
		/* force an update and see if we're really done */
		stream_update(dma_stream, 0);
	
		/* complete the status update */
		d.control &= ~0x0002;
		d.source += d.count;
		d.count = 0;
	
		/* check for interrupt generation */
		if ((d.control & 0x0100) != 0)
		{
			/*TODO*///if (LOG_DMA) logerror("DMA%d timer callback - requesting interrupt: count = %04X, source = %04X\n", which, d.count, d.source);
			i186.intr.request |= 0x04 << which;
			update_interrupt_state();
		}
		d.finish_timer = null;
            }
        };
	
	static void update_dma_control(int which, int new_control)
	{
		dma_state d = i186.dma[which];
		int diff;
	
		/* handle the CHG bit */
		if ((new_control & 0x0004)==0)
			new_control = (new_control & ~0x0002) | (d.control & 0x0002);
		new_control &= ~0x0004;
	
		/* check for control bits we don't handle */
		diff = new_control ^ d.control;
		if ((diff & 0x6811) != 0)
			logerror("%05X:ERROR! - unsupported DMA mode %04X\n", new_control);
	
		/* if we're going live, set a timer */
		if (((diff & 0x0002) != 0) && ((new_control & 0x0002)!=0))
		{
			/* make sure the parameters meet our expectations */
			if ((new_control & 0xfe00) != 0x1600)
			{
				logerror("Unexpected DMA control %02X\n", new_control);
			}
			else if ((is_redline==0) && (((d.dest & 1)!=0) || (d.dest & 0x3f) > 0x0b))
			{
				logerror("Unexpected DMA destination %02X\n", d.dest);
			}
			else if (is_redline!=0 && (d.dest & 0xf000) != 0x4000 && (d.dest & 0xf000) != 0x5000)
			{
				logerror("Unexpected DMA destination %02X\n", d.dest);
			}
	
			/* otherwise, set a timer */
			else
			{
				int count = d.count;
				int dacnum;
	
				/* adjust for redline racer */
				if (is_redline == 0)
					dacnum = (d.dest & 0x3f) / 2;
				else
				{
					dacnum = (d.dest >> 9) & 7;
					dac[dacnum].volume = (d.dest & 0x1fe) / 2 / DAC_VOLUME_SCALE;
				}
	
				/*TODO*///if (LOG_DMA) logerror("Initiated DMA %d - count = %04X, source = %04X, dest = %04X\n", which, d.count, d.source, d.dest);
	
				if (d.finish_timer != null)
					timer_remove(d.finish_timer);
				d.finished = 0;
				d.finish_timer = timer_set(TIME_IN_HZ(dac[dacnum].frequency) * (double)count, which, dma_timer_callback);
			}
		}
	
		/* set the new control register */
		d.control = new_control;
	}
	
	
	
	/*************************************
	 *
	 *	80186 internal I/O reads
	 *
	 *************************************/
	
	public static ReadHandlerPtr i186_internal_port_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int shift = 8 * (offset & 1);
		int temp, which;
	
		switch (offset & ~1)
		{
			case 0x22:
				logerror("%05X:ERROR - read from 80186 EOI\n", cpu_get_pc());
				break;
	
			case 0x24:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 interrupt poll\n", cpu_get_pc());
				if ((i186.intr.poll_status & 0x8000) != 0)
					int_callback(0);
				return (i186.intr.poll_status >> shift) & 0xff;
	
			case 0x26:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 interrupt poll status\n", cpu_get_pc());
				return (i186.intr.poll_status >> shift) & 0xff;
	
			case 0x28:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 interrupt mask\n", cpu_get_pc());
				temp  = (i186.intr.timer  >> 3) & 0x01;
				temp |= (i186.intr.dma[0] >> 1) & 0x04;
				temp |= (i186.intr.dma[1] >> 0) & 0x08;
				temp |= (i186.intr.ext[0] << 1) & 0x10;
				temp |= (i186.intr.ext[1] << 2) & 0x20;
				temp |= (i186.intr.ext[2] << 3) & 0x40;
				temp |= (i186.intr.ext[3] << 4) & 0x80;
				return (temp >> shift) & 0xff;
	
			case 0x2a:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 interrupt priority mask\n", cpu_get_pc());
				return (i186.intr.priority_mask >> shift) & 0xff;
	
			case 0x2c:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 interrupt in-service\n", cpu_get_pc());
				return (i186.intr.in_service >> shift) & 0xff;
	
			case 0x2e:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 interrupt request\n", cpu_get_pc());
				temp = i186.intr.request & ~0x0001;
				if ((i186.intr.status & 0x0007) != 0)
					temp |= 1;
				return (temp >> shift) & 0xff;
	
			case 0x30:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 interrupt status\n", cpu_get_pc());
				return (i186.intr.status >> shift) & 0xff;
	
			case 0x32:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 timer interrupt control\n", cpu_get_pc());
				return (i186.intr.timer >> shift) & 0xff;
	
			case 0x34:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 DMA 0 interrupt control\n", cpu_get_pc());
				return (i186.intr.dma[0] >> shift) & 0xff;
	
			case 0x36:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 DMA 1 interrupt control\n", cpu_get_pc());
				return (i186.intr.dma[1] >> shift) & 0xff;
	
			case 0x38:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 INT 0 interrupt control\n", cpu_get_pc());
				return (i186.intr.ext[0] >> shift) & 0xff;
	
			case 0x3a:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 INT 1 interrupt control\n", cpu_get_pc());
				return (i186.intr.ext[1] >> shift) & 0xff;
	
			case 0x3c:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 INT 2 interrupt control\n", cpu_get_pc());
				return (i186.intr.ext[2] >> shift) & 0xff;
	
			case 0x3e:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 INT 3 interrupt control\n", cpu_get_pc());
				return (i186.intr.ext[3] >> shift) & 0xff;
	
			case 0x50:
			case 0x58:
			case 0x60:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 Timer %d count\n", cpu_get_pc(), (offset - 0x50) / 8);
				which = (offset - 0x50) / 8;
				if ((offset & 1)==0)
					internal_timer_sync(which);
				return (i186.timer[which].count >> shift) & 0xff;
	
			case 0x52:
			case 0x5a:
			case 0x62:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 Timer %d max A\n", cpu_get_pc(), (offset - 0x50) / 8);
				which = (offset - 0x50) / 8;
				return (i186.timer[which].maxA >> shift) & 0xff;
	
			case 0x54:
			case 0x5c:
				logerror("%05X:read 80186 Timer %d max B\n", cpu_get_pc(), (offset - 0x50) / 8);
				which = (offset - 0x50) / 8;
				return (i186.timer[which].maxB >> shift) & 0xff;
	
			case 0x56:
			case 0x5e:
			case 0x66:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 Timer %d control\n", cpu_get_pc(), (offset - 0x50) / 8);
				which = (offset - 0x50) / 8;
				return (i186.timer[which].control >> shift) & 0xff;
	
			case 0xa0:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 upper chip select\n", cpu_get_pc());
				return (i186.mem.upper >> shift) & 0xff;
	
			case 0xa2:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 lower chip select\n", cpu_get_pc());
				return (i186.mem.lower >> shift) & 0xff;
	
			case 0xa4:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 peripheral chip select\n", cpu_get_pc());
				return (i186.mem.peripheral >> shift) & 0xff;
	
			case 0xa6:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 middle chip select\n", cpu_get_pc());
				return (i186.mem.middle >> shift) & 0xff;
	
			case 0xa8:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 middle P chip select\n", cpu_get_pc());
				return (i186.mem.middle_size >> shift) & 0xff;
	
			case 0xc0:
			case 0xd0:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 DMA%d lower source address\n", cpu_get_pc(), (offset - 0xc0) / 0x10);
				which = (offset - 0xc0) / 0x10;
				stream_update(dma_stream, 0);
				return (i186.dma[which].source >> shift) & 0xff;
	
			case 0xc2:
			case 0xd2:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 DMA%d upper source address\n", cpu_get_pc(), (offset - 0xc0) / 0x10);
				which = (offset - 0xc0) / 0x10;
				stream_update(dma_stream, 0);
				return (i186.dma[which].source >> (shift + 16)) & 0xff;
	
			case 0xc4:
			case 0xd4:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 DMA%d lower dest address\n", cpu_get_pc(), (offset - 0xc0) / 0x10);
				which = (offset - 0xc0) / 0x10;
				stream_update(dma_stream, 0);
				return (i186.dma[which].dest >> shift) & 0xff;
	
			case 0xc6:
			case 0xd6:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 DMA%d upper dest address\n", cpu_get_pc(), (offset - 0xc0) / 0x10);
				which = (offset - 0xc0) / 0x10;
				stream_update(dma_stream, 0);
				return (i186.dma[which].dest >> (shift + 16)) & 0xff;
	
			case 0xc8:
			case 0xd8:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 DMA%d transfer count\n", cpu_get_pc(), (offset - 0xc0) / 0x10);
				which = (offset - 0xc0) / 0x10;
				stream_update(dma_stream, 0);
				return (i186.dma[which].count >> shift) & 0xff;
	
			case 0xca:
			case 0xda:
				/*TODO*///if (LOG_PORTS) logerror("%05X:read 80186 DMA%d control\n", cpu_get_pc(), (offset - 0xc0) / 0x10);
				which = (offset - 0xc0) / 0x10;
				stream_update(dma_stream, 0);
				return (i186.dma[which].control >> shift) & 0xff;
	
			default:
				logerror("%05X:read 80186 port %02X\n", cpu_get_pc(), offset);
				break;
		}
		return 0x00;
	} };
	
	
	
	/*************************************
	 *
	 *	80186 internal I/O writes
	 *
	 *************************************/
	
	public static WriteHandlerPtr i186_internal_port_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int even_byte=0;
		int temp, which, data16;
	
		/* warning: this assumes all port writes here are word-sized */
		if ((offset & 1)==0)
		{
			even_byte = data;
			return;
		}
		data16 = (data << 8) | even_byte;
	
		switch (offset & ~1)
		{
			case 0x22:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 EOI = %04X\n", cpu_get_pc(), data16);
				handle_eoi(0x8000);
				update_interrupt_state();
				break;
	
			case 0x24:
				logerror("%05X:ERROR - write to 80186 interrupt poll = %04X\n", cpu_get_pc(), data16);
				break;
	
			case 0x26:
				logerror("%05X:ERROR - write to 80186 interrupt poll status = %04X\n", cpu_get_pc(), data16);
				break;
	
			case 0x28:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 interrupt mask = %04X\n", cpu_get_pc(), data16);
				i186.intr.timer  = (i186.intr.timer  & ~0x08) | ((data16 << 3) & 0x08);
				i186.intr.dma[0] = (i186.intr.dma[0] & ~0x08) | ((data16 << 1) & 0x08);
				i186.intr.dma[1] = (i186.intr.dma[1] & ~0x08) | ((data16 << 0) & 0x08);
				i186.intr.ext[0] = (i186.intr.ext[0] & ~0x08) | ((data16 >> 1) & 0x08);
				i186.intr.ext[1] = (i186.intr.ext[1] & ~0x08) | ((data16 >> 2) & 0x08);
				i186.intr.ext[2] = (i186.intr.ext[2] & ~0x08) | ((data16 >> 3) & 0x08);
				i186.intr.ext[3] = (i186.intr.ext[3] & ~0x08) | ((data16 >> 4) & 0x08);
				update_interrupt_state();
				break;
	
			case 0x2a:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 interrupt priority mask = %04X\n", cpu_get_pc(), data16);
				i186.intr.priority_mask = data16 & 0x0007;
				update_interrupt_state();
				break;
	
			case 0x2c:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 interrupt in-service = %04X\n", cpu_get_pc(), data16);
				i186.intr.in_service = data16 & 0x00ff;
				update_interrupt_state();
				break;
	
			case 0x2e:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 interrupt request = %04X\n", cpu_get_pc(), data16);
				i186.intr.request = (i186.intr.request & ~0x00c0) | (data16 & 0x00c0);
				update_interrupt_state();
				break;
	
			case 0x30:
				/*TODO*///if (LOG_PORTS) logerror("%05X:WARNING - wrote to 80186 interrupt status = %04X\n", cpu_get_pc(), data16);
				i186.intr.status = (i186.intr.status & ~0x8007) | (data16 & 0x8007);
				update_interrupt_state();
				break;
	
			case 0x32:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 timer interrupt contol = %04X\n", cpu_get_pc(), data16);
				i186.intr.timer = data16 & 0x000f;
				break;
	
			case 0x34:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 DMA 0 interrupt control = %04X\n", cpu_get_pc(), data16);
				i186.intr.dma[0] = data16 & 0x000f;
				break;
	
			case 0x36:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 DMA 1 interrupt control = %04X\n", cpu_get_pc(), data16);
				i186.intr.dma[1] = data16 & 0x000f;
				break;
	
			case 0x38:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 INT 0 interrupt control = %04X\n", cpu_get_pc(), data16);
				i186.intr.ext[0] = data16 & 0x007f;
				break;
	
			case 0x3a:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 INT 1 interrupt control = %04X\n", cpu_get_pc(), data16);
				i186.intr.ext[1] = data16 & 0x007f;
				break;
	
			case 0x3c:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 INT 2 interrupt control = %04X\n", cpu_get_pc(), data16);
				i186.intr.ext[2] = data16 & 0x001f;
				break;
	
			case 0x3e:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 INT 3 interrupt control = %04X\n", cpu_get_pc(), data16);
				i186.intr.ext[3] = data16 & 0x001f;
				break;
	
			case 0x50:
			case 0x58:
			case 0x60:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 Timer %d count = %04X\n", cpu_get_pc(), (offset - 0x50) / 8, data16);
				which = (offset - 0x50) / 8;
				internal_timer_update(which, data16, -1, -1, -1);
				break;
	
			case 0x52:
			case 0x5a:
			case 0x62:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 Timer %d max A = %04X\n", cpu_get_pc(), (offset - 0x50) / 8, data16);
				which = (offset - 0x50) / 8;
				internal_timer_update(which, -1, data16, -1, -1);
				break;
	
			case 0x54:
			case 0x5c:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 Timer %d max B = %04X\n", cpu_get_pc(), (offset - 0x50) / 8, data16);
				which = (offset - 0x50) / 8;
				internal_timer_update(which, -1, -1, data16, -1);
				break;
	
			case 0x56:
			case 0x5e:
			case 0x66:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 Timer %d control = %04X\n", cpu_get_pc(), (offset - 0x50) / 8, data16);
				which = (offset - 0x50) / 8;
				internal_timer_update(which, -1, -1, -1, data16);
				break;
	
			case 0xa0:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 upper chip select = %04X\n", cpu_get_pc(), data16);
				i186.mem.upper = data16 | 0xc038;
				break;
	
			case 0xa2:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 lower chip select = %04X\n", cpu_get_pc(), data16);
				i186.mem.lower = (data16 & 0x3fff) | 0x0038;
				break;
	
			case 0xa4:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 peripheral chip select = %04X\n", cpu_get_pc(), data16);
				i186.mem.peripheral = data16 | 0x0038;
				break;
	
			case 0xa6:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 middle chip select = %04X\n", cpu_get_pc(), data16);
				i186.mem.middle = data16 | 0x01f8;
				break;
	
			case 0xa8:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 middle P chip select = %04X\n", cpu_get_pc(), data16);
				i186.mem.middle_size = data16 | 0x8038;
	
				temp = (i186.mem.peripheral & 0xffc0) << 4;
				if ((i186.mem.middle_size & 0x0040) != 0)
				{
					install_mem_read_handler(2, temp, temp + 0x2ff, peripheral_r);
					install_mem_write_handler(2, temp, temp + 0x2ff, peripheral_w);
				}
				else
				{
					temp &= 0xffff;
					install_port_read_handler(2, temp, temp + 0x2ff, peripheral_r);
					install_port_write_handler(2, temp, temp + 0x2ff, peripheral_w);
				}
	
				/* we need to do this at a time when the I86 context is swapped in */
				/* this register is generally set once at startup and never again, so it's a good */
				/* time to set it up */
				/*TODO*///i86_set_irq_callback(int_callback);
				break;
	
			case 0xc0:
			case 0xd0:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 DMA%d lower source address = %04X\n", cpu_get_pc(), (offset - 0xc0) / 0x10, data16);
				which = (offset - 0xc0) / 0x10;
				stream_update(dma_stream, 0);
				i186.dma[which].source = (i186.dma[which].source & ~0x0ffff) | (data16 & 0x0ffff);
				break;
	
			case 0xc2:
			case 0xd2:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 DMA%d upper source address = %04X\n", cpu_get_pc(), (offset - 0xc0) / 0x10, data16);
				which = (offset - 0xc0) / 0x10;
				stream_update(dma_stream, 0);
				i186.dma[which].source = (i186.dma[which].source & ~0xf0000) | ((data16 << 16) & 0xf0000);
				break;
	
			case 0xc4:
			case 0xd4:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 DMA%d lower dest address = %04X\n", cpu_get_pc(), (offset - 0xc0) / 0x10, data16);
				which = (offset - 0xc0) / 0x10;
				stream_update(dma_stream, 0);
				i186.dma[which].dest = (i186.dma[which].dest & ~0x0ffff) | (data16 & 0x0ffff);
				break;
	
			case 0xc6:
			case 0xd6:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 DMA%d upper dest address = %04X\n", cpu_get_pc(), (offset - 0xc0) / 0x10, data16);
				which = (offset - 0xc0) / 0x10;
				stream_update(dma_stream, 0);
				i186.dma[which].dest = (i186.dma[which].dest & ~0xf0000) | ((data16 << 16) & 0xf0000);
				break;
	
			case 0xc8:
			case 0xd8:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 DMA%d transfer count = %04X\n", cpu_get_pc(), (offset - 0xc0) / 0x10, data16);
				which = (offset - 0xc0) / 0x10;
				stream_update(dma_stream, 0);
				i186.dma[which].count = data16;
				break;
	
			case 0xca:
			case 0xda:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 DMA%d control = %04X\n", cpu_get_pc(), (offset - 0xc0) / 0x10, data16);
				which = (offset - 0xc0) / 0x10;
				stream_update(dma_stream, 0);
				update_dma_control(which, data16);
				break;
	
			case 0xfe:
				/*TODO*///if (LOG_PORTS) logerror("%05X:80186 relocation register = %04X\n", cpu_get_pc(), data16);
	
				/* we assume here there that this doesn't happen too often */
				/* plus, we can't really remove the old memory range, so we also assume that it's */
				/* okay to leave us mapped where we were */
				temp = (data16 & 0x0fff) << 8;
				if ((data16 & 0x1000) != 0)
				{
					install_mem_read_handler(2, temp, temp + 0xff, i186_internal_port_r);
					install_mem_write_handler(2, temp, temp + 0xff, i186_internal_port_w);
				}
				else
				{
					temp &= 0xffff;
					install_port_read_handler(2, temp, temp + 0xff, i186_internal_port_r);
					install_port_write_handler(2, temp, temp + 0xff, i186_internal_port_w);
				}
	/*			usrintf_showmessage("Sound CPU reset");*/
				break;
	
			default:
				logerror("%05X:80186 port %02X = %04X\n", cpu_get_pc(), offset, data16);
				break;
		}
	} };
	
	
	
	/*************************************
	 *
	 *	8254 PIT accesses
	 *
	 *************************************/
	
	public static void counter_update_count(int which)
	{
		/* only update if the timer is running */
		if (counter[which].timer != null)
		{
			/* determine how many 2MHz cycles are remaining */
			int count = (int)(timer_timeleft(counter[which].timer) / TIME_IN_HZ(2000000));
			counter[which].count = (count < 0) ? 0 : count;
		}
	}
	
	
	public static ReadHandlerPtr pit8254_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		counter_state ctr;
		int which = offset / 0x80;
		int reg = (offset / 2) & 3;
	
		/* ignore odd offsets */
		if ((offset & 1) != 0)
			return 0;
	
		/* switch off the register */
		switch (offset & 3)
		{
			case 0:
			case 1:
			case 2:
				/* warning: assumes LSB/MSB addressing and no latching! */
				which = (which * 3) + reg;
				ctr = counter[which];
	
				/* update the count */
				counter_update_count(which);
	
				/* return the LSB */
				if (counter[which].readbyte == 0)
				{
					counter[which].readbyte = 1;
					return counter[which].count & 0xff;
				}
	
				/* write the MSB and reset the counter */
				else
				{
					counter[which].readbyte = 0;
					return (counter[which].count >> 8) & 0xff;
				}
				//break;
		}
		return 0;
	} };
	
	
	public static WriteHandlerPtr pit8254_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		counter_state ctr;
		int which = offset / 0x80;
		int reg = (offset / 2) & 3;
	
		/* ignore odd offsets */
		if ((offset & 1) != 0)
			return;
	
		/* switch off the register */
		switch (reg)
		{
			case 0:
			case 1:
			case 2:
				/* warning: assumes LSB/MSB addressing and no latching! */
				which = (which * 3) + reg;
				ctr = counter[which];
	
				/* write the LSB */
				if (ctr.writebyte == 0)
				{
					ctr.count = (ctr.count & 0xff00) | (data & 0x00ff);
					ctr.writebyte = 1;
				}
	
				/* write the MSB and reset the counter */
				else
				{
					ctr.count = (ctr.count & 0x00ff) | ((data << 8) & 0xff00);
					ctr.writebyte = 0;
	
					/* treat 0 as $10000 */
					if (ctr.count == 0) ctr.count = 0x10000;
	
					/* reset/start the timer */
					if (ctr.timer != null)
						timer_reset(ctr.timer, TIME_NEVER);
					else
						ctr.timer = timer_set(TIME_NEVER, 0, null);
	
					/*TODO*///if (LOG_PIT) logerror("PIT counter %d set to %d (%d Hz)\n", which, ctr.count, 4000000 / ctr.count);
	
					/* set the frequency of the associated DAC */
					if (is_redline == 0)
						set_dac_frequency(which, 4000000 / ctr.count);
					else
					{
						if (which < 5)
							set_dac_frequency(which, 7000000 / ctr.count);
						else if (which == 6)
						{
							set_dac_frequency(5, 7000000 / ctr.count);
							set_dac_frequency(6, 7000000 / ctr.count);
							set_dac_frequency(7, 7000000 / ctr.count);
						}
					}
				}
				break;
	
			case 3:
				/* determine which counter */
				if ((data & 0xc0) == 0xc0) break;
				which = (which * 3) + (data >> 6);
				ctr = counter[which];
	
				/* set the mode */
				ctr.mode = (data >> 1) & 7;
				break;
		}
	} };
	
	
	
	/*************************************
	 *
	 *	External 80186 control
	 *
	 *************************************/
	
	public static WriteHandlerPtr leland_i86_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* see if anything changed */
		int diff = (last_control ^ data) & 0xf8;
		if (diff == 0)
			return;
		last_control = data;
	
		/*if (LOG_COMM)
		{
			logerror("%04X:I86 control = %02X", cpu_getpreviouspc(), data);
			if (!(data & 0x80)) logerror("  /RESET");
			if (!(data & 0x40)) logerror("  ZNMI");
			if (!(data & 0x20)) logerror("  INT0");
			if (!(data & 0x10)) logerror("  /TEST");
			if (!(data & 0x08)) logerror("  INT1");
			logerror("\n");
		}*/
	
		/* /RESET */
		cpu_set_reset_line(2, (data & 0x80)!=0  ? CLEAR_LINE : ASSERT_LINE);
	
		/* /NMI */
	/* 	If the master CPU doesn't get a response by the time it's ready to send
		the next command, it uses an NMI to force the issue; unfortunately, this
		seems to really screw up the sound system. It turns out it's better to
		just wait for the original interrupt to occur naturally */
	/*	cpu_set_nmi_line  (2, data & 0x40  ? CLEAR_LINE : ASSERT_LINE);*/
	
		/* INT0 */
		if ((data & 0x20) != 0)
		{
			if (LATCH_INTS == 0) i186.intr.request &= ~0x10;
		}
		else if ((i186.intr.ext[0] & 0x10) != 0)
			i186.intr.request |= 0x10;
		else if ((diff & 0x20) != 0)
			i186.intr.request |= 0x10;
	
		/* INT1 */
		if ((data & 0x08) != 0)
		{
			if (LATCH_INTS == 0) i186.intr.request &= ~0x20;
		}
		else if ((i186.intr.ext[1] & 0x10) != 0)
			i186.intr.request |= 0x20;
		else if ((diff & 0x08) != 0)
			i186.intr.request |= 0x20;
	
		/* handle reset here */
		if (((diff & 0x80)  != 0) && ((data & 0x80)  != 0))
			leland_i186_reset();
	
		update_interrupt_state();
	} };
	
	
	
	/*************************************
	 *
	 *	Sound command handling
	 *
	 *************************************/
	
	public static timer_callback command_lo_sync = new timer_callback() {
            public void handler(int data) {
                /*TODO*///if (LOG_COMM) logerror("%04X:Write sound command latch lo = %02X\n", cpu_getpreviouspc(), data);
		sound_command[0] = data;
            }
        };
	
	
	public static WriteHandlerPtr leland_i86_command_lo_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		timer_set(TIME_NOW, data, command_lo_sync);
	} };
	
	
	public static WriteHandlerPtr leland_i86_command_hi_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*TODO*///if (LOG_COMM) logerror("%04X:Write sound command latch hi = %02X\n", cpu_getpreviouspc(), data);
		sound_command[1] = data;
	} };
	
	
	public static ReadHandlerPtr main_to_sound_comm_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if ((offset & 1)==0)
		{
			/*TODO*///if (LOG_COMM) logerror("%05X:Read sound command latch lo = %02X\n", cpu_get_pc(), sound_command[0]);
			return sound_command[0];
		}
		else
		{
			/*TODO*///if (LOG_COMM) logerror("%05X:Read sound command latch hi = %02X\n", cpu_get_pc(), sound_command[1]);
			return sound_command[1];
		}
	} };
	
	
	
	
	/*************************************
	 *
	 *	Sound response handling
	 *
	 *************************************/
	
	public static timer_callback delayed_response_r = new timer_callback() {
            public void handler(int checkpc) {
                int pc = cpunum_get_reg(0, Z80_PC);
		int oldaf = cpunum_get_reg(0, Z80_AF);
	
		/* This is pretty cheesy, but necessary. Since the CPUs run in round-robin order,
		   synchronizing on the write to this register from the slave side does nothing.
		   In order to make sure the master CPU get the real response, we synchronize on
		   the read. However, the value we returned the first time around may not be
		   accurate, so after the system has synced up, we go back into the master CPUs
		   state and put the proper value into the A register. */
		if (pc == checkpc)
		{
			/*TODO*///if (LOG_COMM) logerror("(Updated sound response latch to %02X)\n", sound_response);
	
			oldaf = (oldaf & 0x00ff) | (sound_response << 8);
			cpunum_set_reg(0, Z80_AF, oldaf);
		}
		else
			logerror("ERROR: delayed_response_r - current PC = %04X, checkPC = %04X\n", pc, checkpc);
            }
        };
	
	
	public static ReadHandlerPtr leland_i86_response_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/*TODO*///if (LOG_COMM) logerror("%04X:Read sound response latch = %02X\n", cpu_getpreviouspc(), sound_response);
	
		/* if sound is disabled, toggle between FF and 00 */
		if (Machine.sample_rate == 0)
			return sound_response ^= 0xff;
		else
		{
			/* synchronize the response */
			timer_set(TIME_NOW, cpu_getpreviouspc() + 2, delayed_response_r);
			return sound_response;
		}
	} };
	
	
	public static WriteHandlerPtr sound_to_main_comm_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*TODO*///if (LOG_COMM) logerror("%05X:Write sound response latch = %02X\n", cpu_get_pc(), data);
		sound_response = data;
	} };
	
	
	
	/*************************************
	 *
	 *	Low-level DAC I/O
	 *
	 *************************************/
	
	public static void set_dac_frequency(int which, int frequency)
	{
		dac_state d = dac[which];
		int count = (d.bufin - d.bufout) & DAC_BUFFER_SIZE_MASK;
	
		/* set the frequency of the associated DAC */
		d.frequency = frequency;
		d.step = (int)((double)frequency * (double)(1 << 24) / (double)Machine.sample_rate);
	
		/* also determine the target buffer size */
		d.buftarget = dac[which].frequency / 60 + 50;
		if (d.buftarget > DAC_BUFFER_SIZE - 1)
			d.buftarget = DAC_BUFFER_SIZE - 1;
	
		/* reevaluate the count */
		if (count > d.buftarget)
			clock_active &= ~(1 << which);
		else if (count < d.buftarget)
		{
			/*TODO*///if (LOG_OPTIMIZATION) logerror("  - trigger due to clock active in set_dac_frequency\n");
			cpu_trigger.handler(CPU_RESUME_TRIGGER);
			clock_active |= 1 << which;
		}
	
		/*TODO*///if (LOG_DAC) logerror("DAC %d frequency = %d, step = %08X\n", which, d.frequency, d.step);
	}
	
	
	public static WriteHandlerPtr dac_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int which = offset / 2;
		dac_state d = dac[which];
	
		/* handle value changes */
		if ((offset & 1)==0)
		{
			int count = (d.bufin - d.bufout) & DAC_BUFFER_SIZE_MASK;
	
			/* set the new value */
			d.value = data - 0x80;
			/*TODO*///if (LOG_DAC) logerror("%05X:DAC %d value = %02X\n", cpu_get_pc(), offset / 2, data);
	
			/* if we haven't overflowed the buffer, add the value value to it */
			if (count < DAC_BUFFER_SIZE - 1)
			{
				/* if this is the first byte, sync the stream */
				if (count == 0)
					stream_update(nondma_stream, 0);
	
				/* prescale by the volume */
				d.buffer[d.bufin] = (short) (d.value * d.volume);
				d.bufin = (d.bufin + 1) & DAC_BUFFER_SIZE_MASK;
	
				/* update the clock status */
				if (++count > d.buftarget)
					clock_active &= ~(1 << which);
			}
		}
	
		/* handle volume changes */
		else
		{
			d.volume = (data ^ 0x00) / DAC_VOLUME_SCALE;
			/*TODO*///if (LOG_DAC) logerror("%05X:DAC %d volume = %02X\n", cpu_get_pc(), offset / 2, data);
		}
	} };
	
	
	public static WriteHandlerPtr redline_dac_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int which = offset / 0x200;
		dac_state d = dac[which];
		int count = (d.bufin - d.bufout) & DAC_BUFFER_SIZE_MASK;
	
		/* set the new value */
		d.value = data - 0x80;
	
		/* if we haven't overflowed the buffer, add the value value to it */
		if (count < DAC_BUFFER_SIZE - 1)
		{
			/* if this is the first byte, sync the stream */
			if (count == 0)
				stream_update(nondma_stream, 0);
	
			/* prescale by the volume */
			d.buffer[d.bufin] = (short) (d.value * d.volume);
			d.bufin = (d.bufin + 1) & DAC_BUFFER_SIZE_MASK;
	
			/* update the clock status */
			if (++count > d.buftarget)
				clock_active &= ~(1 << which);
		}
	
		/* update the volume */
		d.volume = (offset & 0x1fe) / 2 / DAC_VOLUME_SCALE;
		/*TODO*///if (LOG_DAC) logerror("%05X:DAC %d value = %02X, volume = %02X\n", cpu_get_pc(), which, data, (offset & 0x1fe) / 2);
	} };
	
	
	public static WriteHandlerPtr dac_10bit_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int even_byte=0;
		dac_state d = dac[6];
		int count = (d.bufin - d.bufout) & DAC_BUFFER_SIZE_MASK;
		int data16;
	
		/* warning: this assumes all port writes here are word-sized */
		/* if the offset is even, just stash the value */
		if ((offset & 1)==0)
		{
			even_byte = data;
			return;
		}
		data16 = (data << 8) | even_byte;
	
		/* set the new value */
		d.value = data16 - 0x200;
		/*TODO*///if (LOG_DAC) logerror("%05X:DAC 10-bit value = %02X\n", cpu_get_pc(), data16);
	
		/* if we haven't overflowed the buffer, add the value value to it */
		if (count < DAC_BUFFER_SIZE - 1)
		{
			/* if this is the first byte, sync the stream */
			if (count == 0)
				stream_update(nondma_stream, 0);
	
			/* prescale by the volume */
			d.buffer[d.bufin] = (short) (d.value * (0xff / DAC_VOLUME_SCALE / 2));
			d.bufin = (d.bufin + 1) & DAC_BUFFER_SIZE_MASK;
	
			/* update the clock status */
			if (++count > d.buftarget)
				clock_active &= ~0x40;
		}
	} };
	
	
	public static WriteHandlerPtr ataxx_dac_control = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* handle common offsets */
		switch (offset)
		{
			case 0x00:
			case 0x02:
			case 0x04:
				dac_w.handler(offset, data);
				return;
	
			case 0x06:
				dac_w.handler(1, ((data << 5) & 0xe0) | ((data << 2) & 0x1c) | (data & 0x03));
				dac_w.handler(3, ((data << 2) & 0xe0) | ((data >> 1) & 0x1c) | ((data >> 4) & 0x03));
				dac_w.handler(5, (data & 0xc0) | ((data >> 2) & 0x30) | ((data >> 4) & 0x0c) | ((data >> 6) & 0x03));
				return;
		}
	
		/* if we have a YM2151 (and an external DAC), handle those offsets */
		if (has_ym2151 != 0)
		{
			stream_update(extern_stream, 0);
			switch (offset)
			{
				case 0x08:
				case 0x09:
					ext_active = 1;
					/*TODO*///if (LOG_EXTERN) logerror("External DAC active\n");
					return;
	
				case 0x0a:
				case 0x0b:
					ext_active = 0;
					/*TODO*///if (LOG_EXTERN) logerror("External DAC inactive\n");
					return;
	
				case 0x0c:
					ext_start = (ext_start & 0xff00f) | ((data << 4) & 0x00ff0);
					/*TODO*///if (LOG_EXTERN) logerror("External DAC start = %05X\n", ext_start);
					return;
	
				case 0x0d:
					ext_start = (ext_start & 0x00fff) | ((data << 12) & 0xff000);
					/*TODO*///if (LOG_EXTERN) logerror("External DAC start = %05X\n", ext_start);
					return;
	
				case 0x0e:
					ext_stop = (ext_stop & 0xff00f) | ((data << 4) & 0x00ff0);
					/*TODO*///if (LOG_EXTERN) logerror("External DAC stop = %05X\n", ext_stop);
					return;
	
				case 0x0f:
					ext_stop = (ext_stop & 0x00fff) | ((data << 12) & 0xff000);
					/*TODO*///if (LOG_EXTERN) logerror("External DAC stop = %05X\n", ext_stop);
					return;
	
				case 0x42:
				case 0x43:
					dac_w.handler(offset - 0x42 + 14, data);
					return;
			}
		}
		logerror("%05X:Unexpected peripheral write %d/%02X = %02X\n", cpu_get_pc(), 5, offset, data);
	} };
	
	
	
	/*************************************
	 *
	 *	Peripheral chip dispatcher
	 *
	 *************************************/
	
	public static ReadHandlerPtr peripheral_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int select = offset / 0x80;
		offset &= 0x7f;
	
		switch (select)
		{
			case 0:
				if ((offset & 1)!=0)
					return 0;
	
				/* we have to return 0 periodically so that they handle interrupts */
				if ((++clock_tick & 7) == 0)
					return 0;
	
				/* if we've filled up all the active channels, we can give this CPU a reset */
				/* until the next interrupt */
				{
					int result;
	
					if (is_redline == 0)
						result = ((clock_active >> 1) & 0x3e);
					else
						result = ((clock_active << 1) & 0x7e);
	
					if ((i186.intr.pending==0) && (active_mask!=null) 
                                                && ((active_mask.read() & result) == 0) 
                                                && ((++total_reads) > 100))
					{
						/*TODO*///if (LOG_OPTIMIZATION) logerror("Suspended CPU: active_mask = %02X, result = %02X\n", *active_mask, result);
						cpu_spinuntil_trigger(CPU_RESUME_TRIGGER);
					}
					/*TODO*///else if (LOG_OPTIMIZATION)
					/*TODO*///{
					/*TODO*///	if (i186.intr.pending) logerror("(can't suspend - interrupt pending)\n");
					/*TODO*///	else if (active_mask && (*active_mask & result) != 0) logerror("(can't suspend: mask=%02X result=%02X\n", *active_mask, result);
					/*TODO*///}
	
					return result;
				}
	
			case 1:
				return main_to_sound_comm_r.handler(offset);
	
			case 2:
				return pit8254_r.handler(offset);
	
			case 3:
				if (has_ym2151 == 0)
					return pit8254_r.handler(offset | 0x80);
				else
					return (offset & 1)!=0 ? 0 : YM2151_status_port_0_r.handler(offset);
	
			case 4:
				if (is_redline != 0)
					return pit8254_r.handler(offset | 0x100);
				else
					logerror("%05X:Unexpected peripheral read %d/%02X\n", cpu_get_pc(), select, offset);
				break;
	
			default:
				logerror("%05X:Unexpected peripheral read %d/%02X\n", cpu_get_pc(), select, offset);
				break;
		}
		return 0xff;
	} };
	
	
	public static WriteHandlerPtr peripheral_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int select = offset / 0x80;
		offset &= 0x7f;
	
		switch (select)
		{
			case 1:
				sound_to_main_comm_w.handler(offset, data);
				break;
	
			case 2:
				pit8254_w.handler(offset, data);
				break;
	
			case 3:
				if (has_ym2151 == 0)
					pit8254_w.handler(offset | 0x80, data);
				else if (offset == 0)
					YM2151_register_port_0_w.handler(offset, data);
				else if (offset == 2)
					YM2151_data_port_0_w.handler(offset, data);
				break;
	
			case 4:
				if (is_redline != 0)
					pit8254_w.handler(offset | 0x100, data);
				else
					dac_10bit_w.handler(offset, data);
				break;
	
			case 5:	/* Ataxx/WSF/Indy Heat only */
				ataxx_dac_control.handler(offset, data);
				break;
	
			default:
				logerror("%05X:Unexpected peripheral write %d/%02X = %02X\n", cpu_get_pc(), select, offset, data);
				break;
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Optimizations
	 *
	 *************************************/
	
	public static void leland_i86_optimize_address(int offset)
	{
		if (offset != 0)
			active_mask = new UBytePtr(memory_region(REGION_CPU3), offset);
		else
			active_mask = null;
	}
	
	
	
	/*************************************
	 *
	 *	Game-specific handlers
	 *
	 *************************************/
	
	public static WriteHandlerPtr ataxx_i86_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* compute the bit-shuffled variants of the bits and then write them */
		int modified = 	((data & 0x01) << 7) |
						((data & 0x02) << 5) |
						((data & 0x04) << 3) |
						((data & 0x08) << 1);
		leland_i86_control_w.handler(offset, modified);
	} };
	
	
	
	/*************************************
	 *
	 *	Sound CPU memory handlers
	 *
	 *************************************/
	public static Memory_ReadAddress leland_i86_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x1fff, MRA_ROM ),
		new Memory_ReadAddress( 0x00000, 0x03fff, MRA_RAM  ),
		new Memory_ReadAddress( 0x0c000, 0x0ffff, MRA_BANK6 ), /* used by Ataxx */
		new Memory_ReadAddress( 0x1c000, 0x1ffff, MRA_BANK7 ), /* used by Super Offroad */
		new Memory_ReadAddress( 0x20000, 0xfffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress leland_i86_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x9fff, MWA_ROM ),
		new Memory_WriteAddress( 0x00000, 0x03fff, MWA_RAM, ram_base ),
		new Memory_WriteAddress( 0x0c000, 0x0ffff, MWA_BANK6 ),
		new Memory_WriteAddress( 0x1c000, 0x1ffff, MWA_BANK7 ),
		new Memory_WriteAddress( 0x20000, 0xfffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static IO_ReadPort leland_i86_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),    new IO_ReadPort( 0xf2, 0xf2, leland_i86_response_r ),
                new IO_ReadPort( 0xff00, 0xffff, i186_internal_port_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
        
        public static IO_WritePort redline_i86_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x6000, 0x6fff, redline_dac_w ),
		new IO_WritePort( 0xff00, 0xffff, i186_internal_port_w ),
                new IO_WritePort(MEMPORT_MARKER, 0)
	};
		
	public static IO_WritePort leland_i86_writeport []={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x0000, 0x000b, dac_w ),
		new IO_WritePort( 0x0080, 0x008b, dac_w ),
		new IO_WritePort( 0x00c0, 0x00cb, dac_w ),
		new IO_WritePort( 0xff00, 0xffff, i186_internal_port_w ),
                new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	public static IO_WritePort ataxx_i86_writeport []={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0xff00, 0xffff, i186_internal_port_w ),
                new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	/************************************************************************
	
	Memory configurations:
	
		Redline Racer:
			FFDF7:80186 upper chip select = E03C		. E0000-FFFFF, 128k long
			FFDF7:80186 lower chip select = 00FC		. 00000-00FFF, 4k long
			FFDF7:80186 peripheral chip select = 013C	. 01000, 01080, 01100, 01180, 01200, 01280, 01300
			FFDF7:80186 middle chip select = 81FC		. 80000-C0000, 64k chunks, 256k total
			FFDF7:80186 middle P chip select = A0FC
	
		Quarterback, Team Quarterback, AAFB, Super Offroad, Track Pack, Pigout, Viper:
			FFDFA:80186 upper chip select = E03C		. E0000-FFFFF, 128k long
			FFDFA:80186 peripheral chip select = 203C	. 20000, 20080, 20100, 20180, 20200, 20280, 20300
			FFDFA:80186 middle chip select = 01FC		. 00000-7FFFF, 128k chunks, 512k total
			FFDFA:80186 middle P chip select = C0FC
	
		Ataxx, Indy Heat, World Soccer Finals:
			FFD9D:80186 upper chip select = E03C		. E0000-FFFFF, 128k long
			FFD9D:80186 peripheral chip select = 043C	. 04000, 04080, 04100, 04180, 04200, 04280, 04300
			FFD9D:80186 middle chip select = 01FC		. 00000-7FFFF, 128k chunks, 512k total
			FFD9D:80186 middle P chip select = C0BC
	
	************************************************************************/
}
