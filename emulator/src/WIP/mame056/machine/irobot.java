/***************************************************************************

  machine.c

  Functions to emulate general aspects of the machine (RAM, ROM, interrupts,
  I/O ports)

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.memoryH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;        
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame056.inptport.*;

import static common.ptr.*;
import static arcadeflex056.fucPtr.*;

import static WIP.mame056.vidhrdw.irobot.*;
import static mame056.commonH.*;
import static mame056.common.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpu.m6809.m6809H.M6809_IRQ_LINE;

public class irobot
{
	
	/* Note:
	 * There's probably something wrong with the way the Mathbox gets started.
	 * Try compiling with IR_TIMING=1, run with logging on and take a look at
	 * the resulting logilfe.
	 * The mathbox is started in short intervals (<10 scanlines) without (!)
	 * checking its idle status.
	 * It also seems that the mathbox in this emulation would have to cope with
	 * approx. 5000 instructions per scanline [look at the number of instructions
	 * and the number of scanlines to the next mathbox start]. This seems a bit
	 * too high.
	 */
	
	public static int IR_TIMING = 1;		/* try to emulate MB and VG running time */
	public static int DISASSEMBLE_MB_ROM = 0;		/* generate a disassembly of the mathbox ROMs */
	
	/*TODO*///#define IR_CPU_STATE \
	/*TODO*///	logerror(\
	/*TODO*///			"pc: %4x, scanline: %d\n", cpu_getpreviouspc(), cpu_getscanline())
	
	
	public static int irvg_clear;
	static int irvg_vblank;
	static int irvg_running;
	static int irmb_running;
	static timer_entry irscanline_timer;
	
	//#if IR_TIMING
	static timer_entry irvg_timer;
	static timer_entry irmb_timer;
	//#endif
	
	
	static UBytePtr comRAM=new UBytePtr(), mbRAM=new UBytePtr(), mbROM=new UBytePtr();
	static int irobot_control_num = 0;
	static int irobot_statwr;
	static int irobot_out0;
	static int irobot_outx,irobot_mpage;
	
	public static UBytePtr irobot_combase_mb = new UBytePtr();
	public static UBytePtr irobot_combase = new UBytePtr();
	public static int irobot_bufsel;
	public static int irobot_alphamap;
	

	
	/***********************************************************************/
	
	public static ReadHandlerPtr irobot_sharedmem_r  = new ReadHandlerPtr() { 
            public int handler(int offset){
		if (irobot_outx == 3)
			return mbRAM.read(BYTE_XOR_BE(offset));
	
		if (irobot_outx == 2)
			return irobot_combase.read(BYTE_XOR_BE(offset & 0xFFF));
	
		if (irobot_outx == 0)
			return mbROM.read(((irobot_mpage & 1) << 13) + BYTE_XOR_BE(offset));
	
		if (irobot_outx == 1)
			return mbROM.read(0x4000 + ((irobot_mpage & 3) << 13) + BYTE_XOR_BE(offset));
	
		return 0xFF;
	} };
	
	/* Comment out the mbRAM =, comRAM2 = or comRAM1 = and it will start working */
	public static WriteHandlerPtr irobot_sharedmem_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (irobot_outx == 3)
			mbRAM.write(BYTE_XOR_BE(offset), data);
	
		if (irobot_outx == 2)
			irobot_combase.write(BYTE_XOR_BE(offset & 0xFFF), data);
	} };
	
	public static timer_callback irvg_done_callback = new timer_callback() {
            public void handler(int param) {
                logerror("vg done. ");
		/*TODO*///IR_CPU_STATE();
		irvg_running = 0;
            }
        };
	
	public static WriteHandlerPtr irobot_statwr_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		logerror("write %2x ", data);
		/*TODO*///IR_CPU_STATE;
	
		irobot_combase = new UBytePtr(comRAM,(data >> 7));
		irobot_combase_mb = new UBytePtr(comRAM,((data >> 7) ^ 1));
		irobot_bufsel = data & 0x02;
		if (((data & 0x01) == 0x01) && (irvg_clear == 0))
			irobot_poly_clear();
	
		irvg_clear = data & 0x01;
	
		if (((data & 0x04)!=0) && ((irobot_statwr & 0x04)==0))
		{
			run_video();
	/*TODO*///#if IR_TIMING
			if (irvg_running == 0)
			{
				logerror("vg start ");
				/*TODO*///IR_CPU_STATE;
				irvg_timer = timer_set (TIME_IN_MSEC(10), 0, irvg_done_callback);
			}
			else
			{
				logerror("vg start [busy!] ");
				/*TODO*///IR_CPU_STATE;
				timer_reset (irvg_timer , TIME_IN_MSEC(10));
			}
	/*TODO*///#endif
			irvg_running=1;
		}
		if (((data & 0x10)!=0) && ((irobot_statwr & 0x10)==0)){
			/*TODO*///irmb_run();
                }
		irobot_statwr = data;
	} };
	
	public static WriteHandlerPtr irobot_out0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = memory_region(REGION_CPU1);
	
		irobot_out0 = data;
		switch (data & 0x60)
		{
			case 0:
				cpu_setbank(2, new UBytePtr(RAM,0x1C000));
				break;
			case 0x20:
				cpu_setbank(2, new UBytePtr(RAM,0x1C800));
				break;
			case 0x40:
				cpu_setbank(2, new UBytePtr(RAM,0x1D000));
				break;
		}
		irobot_outx = (data & 0x18) >> 3;
		irobot_mpage = (data & 0x06) >> 1;
		irobot_alphamap = (data & 0x80);
	} };
	
	public static WriteHandlerPtr irobot_rom_banksel_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = memory_region(REGION_CPU1);
	
		switch ((data & 0x0E) >> 1)
		{
			case 0:
				cpu_setbank(1, new UBytePtr(RAM,0x10000));
				break;
			case 1:
				cpu_setbank(1, new UBytePtr(RAM,0x12000));
				break;
			case 2:
				cpu_setbank(1, new UBytePtr(RAM,0x14000));
				break;
			case 3:
				cpu_setbank(1, new UBytePtr(RAM,0x16000));
				break;
			case 4:
				cpu_setbank(1, new UBytePtr(RAM,0x18000));
				break;
			case 5:
				cpu_setbank(1, new UBytePtr(RAM,0x1A000));
				break;
		}
		/*TODO*///set_led_status(0,data & 0x10);
		/*TODO*///set_led_status(1,data & 0x20);
	} };
	
	public static timer_callback scanline_callback = new timer_callback() {
            public void handler(int scanline) {
                if (scanline == 0) irvg_vblank=0;
	    if (scanline == 224) irvg_vblank=1;
	    logerror("SCANLINE CALLBACK %d\n",scanline);
	    /* set the IRQ line state based on the 32V line state */
	    cpu_set_irq_line(0, M6809_IRQ_LINE, (scanline & 32)!=0 ? ASSERT_LINE : CLEAR_LINE);
	
	    /* set a callback for the next 32-scanline increment */
	    scanline += 32;
	    if (scanline >= 256) scanline = 0;
	    irscanline_timer = timer_set(cpu_getscanlinetime(scanline), scanline, scanline_callback);
            }
        };
	
	public static InitMachinePtr irobot_init_machine = new InitMachinePtr() { public void handler() 
	{
		UBytePtr MB = memory_region(REGION_CPU2);
	
		/* initialize the memory regions */
		mbROM 		= new UBytePtr(MB, 0x00000);
		mbRAM 		= new UBytePtr(MB, 0x0c000);
		comRAM.write(0, MB.read() + 0x0e000);
		comRAM.write(1, MB.read() + 0x0f000);
	
		irvg_vblank=0;
		irvg_running = 0;
		irmb_running = 0;
	
		/* set an initial timer to go off on scanline 0 */
		irscanline_timer = timer_set(cpu_getscanlinetime(0), 0, scanline_callback);
	
		irobot_rom_banksel_w.handler(0,0);
		irobot_out0_w.handler(0,0);
		irobot_combase = new UBytePtr(comRAM,0);
		irobot_combase_mb = new UBytePtr(comRAM,1);
		irobot_outx = 0;
	} };
	
	public static WriteHandlerPtr irobot_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	
		irobot_control_num = offset & 0x03;
	} };
	
	public static ReadHandlerPtr irobot_control_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	
		if (irobot_control_num == 0)
			return readinputport (5);
		else if (irobot_control_num == 1)
			return readinputport (6);
		return 0;
	
	} };
	
	/*  we allow irmb_running and irvg_running to appear running before clearing
		them to simulate the mathbox and vector generator running in real time */
	public static ReadHandlerPtr irobot_status_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int d=0;
	
		logerror("status read. ");
		/*TODO*///IR_CPU_STATE;
	
		if (irmb_running == 0) d |= 0x20;
		if (irvg_running!=0) d |= 0x40;
	
		//        d = (irmb_running * 0x20) | (irvg_running * 0x40);
		if (irvg_vblank!=0) d = d | 0x80;
	/*TODO*///#if IR_TIMING
		/* flags are cleared by callbacks */
	/*TODO*///#else
		irmb_running=0;
		irvg_running=0;
	/*TODO*///#endif
		return d;
	} };
	
	
	/***********************************************************************
	
		I-Robot Mathbox
	
	    Based on 4 2901 chips slice processors connected to form a 16-bit ALU
	
	    Microcode roms:
	    6N: bits 0..3: Address of ALU A register
	    5P: bits 0..3: Address of ALU B register
	    6M: bits 0..3: ALU Function bits 5..8
	    7N: bits 0..3: ALU Function bits 1..4
	    8N: bits 0,1: Memory write timing
	        bit 2: Hardware multiply mode
	        bit 3: ALU Function bit 0
	    6P: bits 0,1: Direct addressing bits 0,1
	        bits 2,3: Jump address bits 0,1
	    8M: bits 0..3: Jump address bits 6..9
	    9N: bits 0..3: Jump address bits 2..5
	    8P: bits 0..3: Memory address bits 2..5
	    9M: bit 0: Shift control
	        bits 1..3: Jump type
	            0 = No Jump
	            1 = On carry
	            2 = On zero
	            3 = On positive
	            4 = On negative
	            5 = Unconditional
	            6 = Jump to Subroutine
	            7 = Return from Subroutine
	    7M: Bit 0: Mathbox memory enable
	        Bit 1: Latch data to address bus
	        Bit 2: Carry in select
	        Bit 3: Carry in value
	        (if 2,3 = 11 then mathbox is done)
	    9P: Bit 0: Hardware divide enable
	        Bits 1,2: Memory select
	        Bit 3: Memory R/W
	    7P: Bits 0,1: Direct addressing bits 6,7
	        Bits 2,3: Unused
	
	***********************************************************************/
	
	public static int FL_MULT       =   0x01;
	public static int FL_shift      =   0x02;
	public static int FL_MBMEMDEC   =   0x04;
	public static int FL_ADDEN      =   0x08;
	public static int FL_DPSEL      =   0x10;
	public static int FL_carry      =   0x20;
	public static int FL_DIV        =   0x40;
	public static int FL_MBRW       =   0x80;
	
	public static class irmb_ops
	{
		public irmb_ops nxtop;
		public int func;
		public int diradd;
		public int latchmask;
		public UShortPtr areg;
		public UShortPtr breg;
		public int cycles;
		public int diren;
		public int flags;
		public int ramsel;
	};
	
	static irmb_ops mbops;
	
	static irmb_ops[] irmb_stack=new irmb_ops[16];
	static int[] irmb_regs=new int[16];
	static int irmb_latch;
	
	/*TODO*///#if DISASSEMBLE_MB_ROM
/*TODO*///	void disassemble_instruction(irmb_ops *op);
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	
/*TODO*///	UINT32 irmb_din(const irmb_ops *curop)
/*TODO*///	{
/*TODO*///		UINT32 d = 0;
/*TODO*///	
/*TODO*///		if (!(curop->flags & FL_MBMEMDEC) && (curop->flags & FL_MBRW))
/*TODO*///		{
/*TODO*///			UINT32 ad = curop->diradd | (irmb_latch & curop->latchmask);
/*TODO*///	
/*TODO*///			if (curop->diren || (irmb_latch & 0x6000) == 0)
/*TODO*///				d = ((UINT16 *)mbRAM)[ad & 0xfff];				/* MB RAM read */
/*TODO*///			else if (irmb_latch & 0x4000)
/*TODO*///				d = ((UINT16 *)mbROM)[ad + 0x2000];				/* MB ROM read, CEMATH = 1 */
/*TODO*///			else
/*TODO*///				d = ((UINT16 *)mbROM)[ad & 0x1fff];				/* MB ROM read, CEMATH = 0 */
/*TODO*///		}
/*TODO*///		return d;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	void irmb_dout(const irmb_ops *curop, UINT32 d)
/*TODO*///	{
/*TODO*///		/* Write to video com ram */
/*TODO*///		if (curop->ramsel == 3)
/*TODO*///			((UINT16 *)irobot_combase_mb)[irmb_latch & 0x7ff] = d;
/*TODO*///	
/*TODO*///	    /* Write to mathox ram */
/*TODO*///		if (!(curop->flags & FL_MBMEMDEC))
/*TODO*///		{
/*TODO*///			UINT32 ad = curop->diradd | (irmb_latch & curop->latchmask);
/*TODO*///	
/*TODO*///			if (curop->diren || (irmb_latch & 0x6000) == 0)
/*TODO*///				((UINT16 *)mbRAM)[ad & 0xfff] = d;				/* MB RAM write */
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/* Convert microcode roms to a more usable form */
/*TODO*///	void load_oproms()
/*TODO*///	{
/*TODO*///		UINT8 *MB = memory_region(REGION_PROMS) + 0x20;
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		/* allocate RAM */
/*TODO*///		mbops = malloc(sizeof(irmb_ops) * 1024);
/*TODO*///		if (mbops == 0) return;
/*TODO*///	
/*TODO*///		for (i = 0; i < 1024; i++)
/*TODO*///		{
/*TODO*///			int nxtadd, func, ramsel, diradd, latchmask, dirmask, time;
/*TODO*///	
/*TODO*///			mbops[i].areg = &irmb_regs[MB[0x0000 + i] & 0x0F];
/*TODO*///			mbops[i].breg = &irmb_regs[MB[0x0400 + i] & 0x0F];
/*TODO*///			func = (MB[0x0800 + i] & 0x0F) << 5;
/*TODO*///			func |= ((MB[0x0C00 +i] & 0x0F) << 1);
/*TODO*///			func |= (MB[0x1000 + i] & 0x08) >> 3;
/*TODO*///			time = MB[0x1000 + i] & 0x03;
/*TODO*///			mbops[i].flags = (MB[0x1000 + i] & 0x04) >> 2;
/*TODO*///			nxtadd = (MB[0x1400 + i] & 0x0C) >> 2;
/*TODO*///			diradd = MB[0x1400 + i] & 0x03;
/*TODO*///			nxtadd |= ((MB[0x1800 + i] & 0x0F) << 6);
/*TODO*///			nxtadd |= ((MB[0x1C00 + i] & 0x0F) << 2);
/*TODO*///			diradd |= (MB[0x2000 + i] & 0x0F) << 2;
/*TODO*///			func |= (MB[0x2400 + i] & 0x0E) << 9;
/*TODO*///			mbops[i].flags |= (MB[0x2400 + i] & 0x01) << 1;
/*TODO*///			mbops[i].flags |= (MB[0x2800 + i] & 0x0F) << 2;
/*TODO*///			mbops[i].flags |= ((MB[0x2C00 + i] & 0x01) << 6);
/*TODO*///			mbops[i].flags |= (MB[0x2C00 + i] & 0x08) << 4;
/*TODO*///			ramsel = (MB[0x2C00 + i] & 0x06) >> 1;
/*TODO*///			diradd |= (MB[0x3000 + i] & 0x03) << 6;
/*TODO*///	
/*TODO*///			if (mbops[i].flags & FL_shift) func |= 0x200;
/*TODO*///	
/*TODO*///			mbops[i].func = func;
/*TODO*///			mbops[i].nxtop = &mbops[nxtadd];
/*TODO*///	
/*TODO*///			/* determine the number of 12MHz cycles for this operation */
/*TODO*///			if (time == 3)
/*TODO*///				mbops[i].cycles = 2;
/*TODO*///			else
/*TODO*///				mbops[i].cycles = 3 + time;
/*TODO*///	
/*TODO*///			/* precompute the hardcoded address bits and the mask to be used on the latch value */
/*TODO*///			if (ramsel == 0)
/*TODO*///			{
/*TODO*///				dirmask = 0x00FC;
/*TODO*///				latchmask = 0x3000;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				dirmask = 0x0000;
/*TODO*///				latchmask = 0x3FFC;
/*TODO*///			}
/*TODO*///			if (ramsel & 2)
/*TODO*///				latchmask |= 0x0003;
/*TODO*///			else
/*TODO*///				dirmask |= 0x0003;
/*TODO*///	
/*TODO*///			mbops[i].ramsel = ramsel;
/*TODO*///			mbops[i].diradd = diradd & dirmask;
/*TODO*///			mbops[i].latchmask = latchmask;
/*TODO*///			mbops[i].diren = (ramsel == 0);
/*TODO*///	
/*TODO*///	#if DISASSEMBLE_MB_ROM
/*TODO*///			disassemble_instruction(&mbops[i]);
/*TODO*///	#endif
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	/* Init mathbox (only called once) */
/*TODO*///	public static InitDriverPtr init_irobot = new InitDriverPtr() { public void handler() 
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///		for (i = 0; i < 16; i++)
/*TODO*///		{
/*TODO*///			irmb_stack[i] = &mbops[0];
/*TODO*///			irmb_regs[i] = 0;
/*TODO*///		}
/*TODO*///		irmb_latch=0;
/*TODO*///		load_oproms();
/*TODO*///	} };
/*TODO*///	
/*TODO*///	static void irmb_done_callback (int param)
/*TODO*///	{
/*TODO*///	    logerror("mb done. ");
/*TODO*///		/*TODO*///IR_CPU_STATE;
/*TODO*///		irmb_running = 0;
/*TODO*///		cpu_set_irq_line(0, M6809_FIRQ_LINE, ASSERT_LINE);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	#define COMPUTE_CI \
/*TODO*///		CI = 0;\
/*TODO*///		if (curop->flags & FL_DPSEL)\
/*TODO*///			CI = cflag;\
/*TODO*///		else\
/*TODO*///		{\
/*TODO*///			if (curop->flags & FL_carry)\
/*TODO*///				CI = 1;\
/*TODO*///			if (!(prevop->flags & FL_DIV) && !nflag)\
/*TODO*///				CI = 1;\
/*TODO*///		}
/*TODO*///	
/*TODO*///	#define ADD(r,s) \
/*TODO*///		COMPUTE_CI;\
/*TODO*///		result = r + s + CI;\
/*TODO*///		cflag = (result >> 16) & 1;\
/*TODO*///		vflag = (((r & 0x7fff) + (s & 0x7fff) + CI) >> 15) ^ cflag
/*TODO*///	
/*TODO*///	#define SUBR(r,s) \
/*TODO*///		COMPUTE_CI;\
/*TODO*///		result = (r ^ 0xFFFF) + s + CI;         /*S - R + CI - 1*/ \
/*TODO*///		cflag = (result >> 16) & 1;\
/*TODO*///		vflag = (((s & 0x7fff) + ((r ^ 0xffff) & 0x7fff) + CI) >> 15) ^ cflag
/*TODO*///	
/*TODO*///	#define SUB(r,s) \
/*TODO*///		COMPUTE_CI;\
/*TODO*///		result = r + (s ^ 0xFFFF) + CI;      /*R - S + CI - 1*/ \
/*TODO*///		cflag = (result >> 16) & 1;\
/*TODO*///		vflag = (((r & 0x7fff) + ((s ^ 0xffff) & 0x7fff) + CI) >> 15) ^ cflag
/*TODO*///	
/*TODO*///	#define OR(r,s) \
/*TODO*///		result = r | s;\
/*TODO*///		vflag = cflag = 0
/*TODO*///	
/*TODO*///	#define AND(r,s) \
/*TODO*///		result = r & s;\
/*TODO*///		vflag = cflag = 0
/*TODO*///	
/*TODO*///	#define IAND(r,s) \
/*TODO*///		result = (r ^ 0xFFFF) & s;\
/*TODO*///		vflag = cflag = 0
/*TODO*///	
/*TODO*///	#define XOR(r,s) \
/*TODO*///		result = r ^ s;\
/*TODO*///		vflag = cflag = 0
/*TODO*///	
/*TODO*///	#define IXOR(r,s) \
/*TODO*///		result = (r ^ s) ^ 0xFFFF;\
/*TODO*///		vflag = cflag = 0
/*TODO*///	
/*TODO*///	
/*TODO*///	#define DEST0 \
/*TODO*///		Q = Y = zresult
/*TODO*///	
/*TODO*///	#define DEST1 \
/*TODO*///		Y = zresult
/*TODO*///	
/*TODO*///	#define DEST2 \
/*TODO*///		Y = *curop->areg;\
/*TODO*///		*curop->breg = zresult
/*TODO*///	
/*TODO*///	#define DEST3 \
/*TODO*///		*curop->breg = zresult;\
/*TODO*///		Y = zresult
/*TODO*///	
/*TODO*///	#define DEST4_NOSHIFT \
/*TODO*///		*curop->breg = (zresult >> 1) | ((curop->flags & 0x20) << 10);\
/*TODO*///		Q = (Q >> 1) | ((curop->flags & 0x20) << 10);\
/*TODO*///		Y = zresult
/*TODO*///	
/*TODO*///	#define DEST4_SHIFT \
/*TODO*///		*curop->breg = (zresult >> 1) | ((nflag ^ vflag) << 15);\
/*TODO*///		Q = (Q >> 1) | ((zresult & 0x01) << 15);\
/*TODO*///		Y = zresult
/*TODO*///	
/*TODO*///	#define DEST5_NOSHIFT \
/*TODO*///		*curop->breg = (zresult >> 1) | ((curop->flags & 0x20) << 10);\
/*TODO*///		Y = zresult
/*TODO*///	
/*TODO*///	#define DEST5_SHIFT \
/*TODO*///		*curop->breg = (zresult >> 1) | ((nflag ^ vflag) << 15);\
/*TODO*///		Y = zresult
/*TODO*///	
/*TODO*///	#define DEST6_NOSHIFT \
/*TODO*///		*curop->breg = zresult << 1;\
/*TODO*///		Q = ((Q << 1) & 0xffff) | (nflag ^ 1);\
/*TODO*///		Y = zresult
/*TODO*///	
/*TODO*///	#define DEST6_SHIFT \
/*TODO*///		*curop->breg = (zresult << 1) | ((Q & 0x8000) >> 15);\
/*TODO*///		Q = (Q << 1) & 0xffff;\
/*TODO*///		Y = zresult
/*TODO*///	
/*TODO*///	#define DEST7_NOSHIFT \
/*TODO*///		*curop->breg = zresult << 1;\
/*TODO*///		Y = zresult
/*TODO*///	
/*TODO*///	#define DEST7_SHIFT \
/*TODO*///		*curop->breg = (zresult << 1) | ((Q & 0x8000) >> 15);\
/*TODO*///		Y = zresult
/*TODO*///	
/*TODO*///	
/*TODO*///	#define JUMP0 	curop++;
/*TODO*///	#define JUMP1	if (cflag) curop = curop->nxtop; else curop++;
/*TODO*///	#define JUMP2	if (zresult == 0) curop = curop->nxtop; else curop++;
/*TODO*///	#define JUMP3	if (nflag == 0) curop = curop->nxtop; else curop++;
/*TODO*///	#define JUMP4	if (nflag) curop = curop->nxtop; else curop++;
/*TODO*///	#define JUMP5	curop = curop->nxtop;
/*TODO*///	#define JUMP6	irmb_stack[SP] = curop + 1; SP = (SP + 1) & 15; curop = curop->nxtop;
/*TODO*///	#define JUMP7	SP = (SP - 1) & 15; curop = irmb_stack[SP];
/*TODO*///	
/*TODO*///	
/*TODO*///	/* Run mathbox */
/*TODO*///	public static void irmb_run()
/*TODO*///	{
/*TODO*///		const irmb_ops *prevop = &mbops[0];
/*TODO*///		const irmb_ops *curop = &mbops[0];
/*TODO*///	
/*TODO*///		UINT32 Q = 0;
/*TODO*///		UINT32 Y = 0;
/*TODO*///		UINT32 nflag = 0;
/*TODO*///		UINT32 vflag = 0;
/*TODO*///		UINT32 cflag = 0;
/*TODO*///		UINT32 zresult = 1;
/*TODO*///		UINT32 CI = 0;
/*TODO*///		UINT32 SP = 0;
/*TODO*///		UINT32 icount = 0;
/*TODO*///	
/*TODO*///		profiler_mark(PROFILER_USER1);
/*TODO*///	
/*TODO*///		while ((prevop->flags & (FL_DPSEL | FL_carry)) != (FL_DPSEL | FL_carry))
/*TODO*///		{
/*TODO*///			UINT32 result;
/*TODO*///			UINT32 fu;
/*TODO*///			UINT32 tmp;
/*TODO*///	
/*TODO*///			icount += curop->cycles;
/*TODO*///	
/*TODO*///			/* Get function code */
/*TODO*///			fu = curop->func;
/*TODO*///	
/*TODO*///			/* Modify function for MULT */
/*TODO*///			if (!(prevop->flags & FL_MULT) || (Q & 1))
/*TODO*///				fu = fu ^ 0x02;
/*TODO*///			else
/*TODO*///				fu = fu | 0x02;
/*TODO*///	
/*TODO*///			/* Modify function for DIV */
/*TODO*///			if ((prevop->flags & FL_DIV) || nflag)
/*TODO*///				fu = fu ^ 0x08;
/*TODO*///			else
/*TODO*///				fu = fu | 0x08;
/*TODO*///	
/*TODO*///			/* Do source and operation */
/*TODO*///			switch (fu & 0x03f)
/*TODO*///			{
/*TODO*///				case 0x00:	ADD(*curop->areg, Q);								break;
/*TODO*///				case 0x01:	ADD(*curop->areg, *curop->breg);					break;
/*TODO*///				case 0x02:	ADD(0, Q);											break;
/*TODO*///				case 0x03:	ADD(0, *curop->breg);								break;
/*TODO*///				case 0x04:	ADD(0, *curop->areg);								break;
/*TODO*///				case 0x05:	tmp = irmb_din(curop); ADD(tmp, *curop->areg);		break;
/*TODO*///				case 0x06:	tmp = irmb_din(curop); ADD(tmp, Q);					break;
/*TODO*///				case 0x07:	tmp = irmb_din(curop); ADD(tmp, 0);					break;
/*TODO*///				case 0x08:	SUBR(*curop->areg, Q);								break;
/*TODO*///				case 0x09:	SUBR(*curop->areg, *curop->breg);					break;
/*TODO*///				case 0x0a:	SUBR(0, Q);											break;
/*TODO*///				case 0x0b:	SUBR(0, *curop->breg);								break;
/*TODO*///				case 0x0c:	SUBR(0, *curop->areg);								break;
/*TODO*///				case 0x0d:	tmp = irmb_din(curop); SUBR(tmp, *curop->areg);		break;
/*TODO*///				case 0x0e:	tmp = irmb_din(curop); SUBR(tmp, Q);				break;
/*TODO*///				case 0x0f:	tmp = irmb_din(curop); SUBR(tmp, 0);				break;
/*TODO*///				case 0x10:	SUB(*curop->areg, Q);								break;
/*TODO*///				case 0x11:	SUB(*curop->areg, *curop->breg);					break;
/*TODO*///				case 0x12:	SUB(0, Q);											break;
/*TODO*///				case 0x13:	SUB(0, *curop->breg);								break;
/*TODO*///				case 0x14:	SUB(0, *curop->areg);								break;
/*TODO*///				case 0x15:	tmp = irmb_din(curop); SUB(tmp, *curop->areg);		break;
/*TODO*///				case 0x16:	tmp = irmb_din(curop); SUB(tmp, Q);					break;
/*TODO*///				case 0x17:	tmp = irmb_din(curop); SUB(tmp, 0);					break;
/*TODO*///				case 0x18:	OR(*curop->areg, Q);								break;
/*TODO*///				case 0x19:	OR(*curop->areg, *curop->breg);						break;
/*TODO*///				case 0x1a:	OR(0, Q);											break;
/*TODO*///				case 0x1b:	OR(0, *curop->breg);								break;
/*TODO*///				case 0x1c:	OR(0, *curop->areg);								break;
/*TODO*///				case 0x1d:	OR(irmb_din(curop), *curop->areg);					break;
/*TODO*///				case 0x1e:	OR(irmb_din(curop), Q);								break;
/*TODO*///				case 0x1f:	OR(irmb_din(curop), 0);								break;
/*TODO*///				case 0x20:	AND(*curop->areg, Q);								break;
/*TODO*///				case 0x21:	AND(*curop->areg, *curop->breg);					break;
/*TODO*///				case 0x22:	AND(0, Q);											break;
/*TODO*///				case 0x23:	AND(0, *curop->breg);								break;
/*TODO*///				case 0x24:	AND(0, *curop->areg);								break;
/*TODO*///				case 0x25:	AND(irmb_din(curop), *curop->areg);					break;
/*TODO*///				case 0x26:	AND(irmb_din(curop), Q);							break;
/*TODO*///				case 0x27:	AND(irmb_din(curop), 0);							break;
/*TODO*///				case 0x28:	IAND(*curop->areg, Q);								break;
/*TODO*///				case 0x29:	IAND(*curop->areg, *curop->breg);					break;
/*TODO*///				case 0x2a:	IAND(0, Q);											break;
/*TODO*///				case 0x2b:	IAND(0, *curop->breg);								break;
/*TODO*///				case 0x2c:	IAND(0, *curop->areg);								break;
/*TODO*///				case 0x2d:	IAND(irmb_din(curop), *curop->areg);				break;
/*TODO*///				case 0x2e:	IAND(irmb_din(curop), Q);							break;
/*TODO*///				case 0x2f:	IAND(irmb_din(curop), 0);							break;
/*TODO*///				case 0x30:	XOR(*curop->areg, Q);								break;
/*TODO*///				case 0x31:	XOR(*curop->areg, *curop->breg);					break;
/*TODO*///				case 0x32:	XOR(0, Q);											break;
/*TODO*///				case 0x33:	XOR(0, *curop->breg);								break;
/*TODO*///				case 0x34:	XOR(0, *curop->areg);								break;
/*TODO*///				case 0x35:	XOR(irmb_din(curop), *curop->areg);					break;
/*TODO*///				case 0x36:	XOR(irmb_din(curop), Q);							break;
/*TODO*///				case 0x37:	XOR(irmb_din(curop), 0);							break;
/*TODO*///				case 0x38:	IXOR(*curop->areg, Q);								break;
/*TODO*///				case 0x39:	IXOR(*curop->areg, *curop->breg);					break;
/*TODO*///				case 0x3a:	IXOR(0, Q);											break;
/*TODO*///				case 0x3b:	IXOR(0, *curop->breg);								break;
/*TODO*///				case 0x3c:	IXOR(0, *curop->areg);								break;
/*TODO*///				case 0x3d:	IXOR(irmb_din(curop), *curop->areg);				break;
/*TODO*///				case 0x3e:	IXOR(irmb_din(curop), Q);							break;
/*TODO*///	default:	case 0x3f:	IXOR(irmb_din(curop), 0);							break;
/*TODO*///			}
/*TODO*///	
/*TODO*///			/* Evaluate flags */
/*TODO*///			zresult = result & 0xFFFF;
/*TODO*///			nflag = zresult >> 15;
/*TODO*///	
/*TODO*///			prevop = curop;
/*TODO*///	
/*TODO*///			/* Do destination and jump */
/*TODO*///			switch (fu >> 6)
/*TODO*///			{
/*TODO*///				case 0x00:
/*TODO*///				case 0x08:	DEST0;			JUMP0;	break;
/*TODO*///				case 0x01:
/*TODO*///				case 0x09:	DEST1;			JUMP0;	break;
/*TODO*///				case 0x02:
/*TODO*///				case 0x0a:	DEST2;			JUMP0;	break;
/*TODO*///				case 0x03:
/*TODO*///				case 0x0b:	DEST3;			JUMP0;	break;
/*TODO*///				case 0x04:	DEST4_NOSHIFT;	JUMP0;	break;
/*TODO*///				case 0x05:	DEST5_NOSHIFT;	JUMP0;	break;
/*TODO*///				case 0x06:	DEST6_NOSHIFT;	JUMP0;	break;
/*TODO*///				case 0x07:	DEST7_NOSHIFT;	JUMP0;	break;
/*TODO*///				case 0x0c:	DEST4_SHIFT;	JUMP0;	break;
/*TODO*///				case 0x0d:	DEST5_SHIFT;	JUMP0;	break;
/*TODO*///				case 0x0e:	DEST6_SHIFT;	JUMP0;	break;
/*TODO*///				case 0x0f:	DEST7_SHIFT;	JUMP0;	break;
/*TODO*///	
/*TODO*///				case 0x10:
/*TODO*///				case 0x18:	DEST0;			JUMP1;	break;
/*TODO*///				case 0x11:
/*TODO*///				case 0x19:	DEST1;			JUMP1;	break;
/*TODO*///				case 0x12:
/*TODO*///				case 0x1a:	DEST2;			JUMP1;	break;
/*TODO*///				case 0x13:
/*TODO*///				case 0x1b:	DEST3;			JUMP1;	break;
/*TODO*///				case 0x14:	DEST4_NOSHIFT;	JUMP1;	break;
/*TODO*///				case 0x15:	DEST5_NOSHIFT;	JUMP1;	break;
/*TODO*///				case 0x16:	DEST6_NOSHIFT;	JUMP1;	break;
/*TODO*///				case 0x17:	DEST7_NOSHIFT;	JUMP1;	break;
/*TODO*///				case 0x1c:	DEST4_SHIFT;	JUMP1;	break;
/*TODO*///				case 0x1d:	DEST5_SHIFT;	JUMP1;	break;
/*TODO*///				case 0x1e:	DEST6_SHIFT;	JUMP1;	break;
/*TODO*///				case 0x1f:	DEST7_SHIFT;	JUMP1;	break;
/*TODO*///	
/*TODO*///				case 0x20:
/*TODO*///				case 0x28:	DEST0;			JUMP2;	break;
/*TODO*///				case 0x21:
/*TODO*///				case 0x29:	DEST1;			JUMP2;	break;
/*TODO*///				case 0x22:
/*TODO*///				case 0x2a:	DEST2;			JUMP2;	break;
/*TODO*///				case 0x23:
/*TODO*///				case 0x2b:	DEST3;			JUMP2;	break;
/*TODO*///				case 0x24:	DEST4_NOSHIFT;	JUMP2;	break;
/*TODO*///				case 0x25:	DEST5_NOSHIFT;	JUMP2;	break;
/*TODO*///				case 0x26:	DEST6_NOSHIFT;	JUMP2;	break;
/*TODO*///				case 0x27:	DEST7_NOSHIFT;	JUMP2;	break;
/*TODO*///				case 0x2c:	DEST4_SHIFT;	JUMP2;	break;
/*TODO*///				case 0x2d:	DEST5_SHIFT;	JUMP2;	break;
/*TODO*///				case 0x2e:	DEST6_SHIFT;	JUMP2;	break;
/*TODO*///				case 0x2f:	DEST7_SHIFT;	JUMP2;	break;
/*TODO*///	
/*TODO*///				case 0x30:
/*TODO*///				case 0x38:	DEST0;			JUMP3;	break;
/*TODO*///				case 0x31:
/*TODO*///				case 0x39:	DEST1;			JUMP3;	break;
/*TODO*///				case 0x32:
/*TODO*///				case 0x3a:	DEST2;			JUMP3;	break;
/*TODO*///				case 0x33:
/*TODO*///				case 0x3b:	DEST3;			JUMP3;	break;
/*TODO*///				case 0x34:	DEST4_NOSHIFT;	JUMP3;	break;
/*TODO*///				case 0x35:	DEST5_NOSHIFT;	JUMP3;	break;
/*TODO*///				case 0x36:	DEST6_NOSHIFT;	JUMP3;	break;
/*TODO*///				case 0x37:	DEST7_NOSHIFT;	JUMP3;	break;
/*TODO*///				case 0x3c:	DEST4_SHIFT;	JUMP3;	break;
/*TODO*///				case 0x3d:	DEST5_SHIFT;	JUMP3;	break;
/*TODO*///				case 0x3e:	DEST6_SHIFT;	JUMP3;	break;
/*TODO*///				case 0x3f:	DEST7_SHIFT;	JUMP3;	break;
/*TODO*///	
/*TODO*///				case 0x40:
/*TODO*///				case 0x48:	DEST0;			JUMP4;	break;
/*TODO*///				case 0x41:
/*TODO*///				case 0x49:	DEST1;			JUMP4;	break;
/*TODO*///				case 0x42:
/*TODO*///				case 0x4a:	DEST2;			JUMP4;	break;
/*TODO*///				case 0x43:
/*TODO*///				case 0x4b:	DEST3;			JUMP4;	break;
/*TODO*///				case 0x44:	DEST4_NOSHIFT;	JUMP4;	break;
/*TODO*///				case 0x45:	DEST5_NOSHIFT;	JUMP4;	break;
/*TODO*///				case 0x46:	DEST6_NOSHIFT;	JUMP4;	break;
/*TODO*///				case 0x47:	DEST7_NOSHIFT;	JUMP4;	break;
/*TODO*///				case 0x4c:	DEST4_SHIFT;	JUMP4;	break;
/*TODO*///				case 0x4d:	DEST5_SHIFT;	JUMP4;	break;
/*TODO*///				case 0x4e:	DEST6_SHIFT;	JUMP4;	break;
/*TODO*///				case 0x4f:	DEST7_SHIFT;	JUMP4;	break;
/*TODO*///	
/*TODO*///				case 0x50:
/*TODO*///				case 0x58:	DEST0;			JUMP5;	break;
/*TODO*///				case 0x51:
/*TODO*///				case 0x59:	DEST1;			JUMP5;	break;
/*TODO*///				case 0x52:
/*TODO*///				case 0x5a:	DEST2;			JUMP5;	break;
/*TODO*///				case 0x53:
/*TODO*///				case 0x5b:	DEST3;			JUMP5;	break;
/*TODO*///				case 0x54:	DEST4_NOSHIFT;	JUMP5;	break;
/*TODO*///				case 0x55:	DEST5_NOSHIFT;	JUMP5;	break;
/*TODO*///				case 0x56:	DEST6_NOSHIFT;	JUMP5;	break;
/*TODO*///				case 0x57:	DEST7_NOSHIFT;	JUMP5;	break;
/*TODO*///				case 0x5c:	DEST4_SHIFT;	JUMP5;	break;
/*TODO*///				case 0x5d:	DEST5_SHIFT;	JUMP5;	break;
/*TODO*///				case 0x5e:	DEST6_SHIFT;	JUMP5;	break;
/*TODO*///				case 0x5f:	DEST7_SHIFT;	JUMP5;	break;
/*TODO*///	
/*TODO*///				case 0x60:
/*TODO*///				case 0x68:	DEST0;			JUMP6;	break;
/*TODO*///				case 0x61:
/*TODO*///				case 0x69:	DEST1;			JUMP6;	break;
/*TODO*///				case 0x62:
/*TODO*///				case 0x6a:	DEST2;			JUMP6;	break;
/*TODO*///				case 0x63:
/*TODO*///				case 0x6b:	DEST3;			JUMP6;	break;
/*TODO*///				case 0x64:	DEST4_NOSHIFT;	JUMP6;	break;
/*TODO*///				case 0x65:	DEST5_NOSHIFT;	JUMP6;	break;
/*TODO*///				case 0x66:	DEST6_NOSHIFT;	JUMP6;	break;
/*TODO*///				case 0x67:	DEST7_NOSHIFT;	JUMP6;	break;
/*TODO*///				case 0x6c:	DEST4_SHIFT;	JUMP6;	break;
/*TODO*///				case 0x6d:	DEST5_SHIFT;	JUMP6;	break;
/*TODO*///				case 0x6e:	DEST6_SHIFT;	JUMP6;	break;
/*TODO*///				case 0x6f:	DEST7_SHIFT;	JUMP6;	break;
/*TODO*///	
/*TODO*///				case 0x70:
/*TODO*///				case 0x78:	DEST0;			JUMP7;	break;
/*TODO*///				case 0x71:
/*TODO*///				case 0x79:	DEST1;			JUMP7;	break;
/*TODO*///				case 0x72:
/*TODO*///				case 0x7a:	DEST2;			JUMP7;	break;
/*TODO*///				case 0x73:
/*TODO*///				case 0x7b:	DEST3;			JUMP7;	break;
/*TODO*///				case 0x74:	DEST4_NOSHIFT;	JUMP7;	break;
/*TODO*///				case 0x75:	DEST5_NOSHIFT;	JUMP7;	break;
/*TODO*///				case 0x76:	DEST6_NOSHIFT;	JUMP7;	break;
/*TODO*///				case 0x77:	DEST7_NOSHIFT;	JUMP7;	break;
/*TODO*///				case 0x7c:	DEST4_SHIFT;	JUMP7;	break;
/*TODO*///				case 0x7d:	DEST5_SHIFT;	JUMP7;	break;
/*TODO*///				case 0x7e:	DEST6_SHIFT;	JUMP7;	break;
/*TODO*///				case 0x7f:	DEST7_SHIFT;	JUMP7;	break;
/*TODO*///			}
/*TODO*///	
/*TODO*///			/* Do write */
/*TODO*///			if (!(prevop->flags & FL_MBRW))
/*TODO*///				irmb_dout(prevop, Y);
/*TODO*///	
/*TODO*///			/* ADDEN */
/*TODO*///			if (!(prevop->flags & FL_ADDEN))
/*TODO*///			{
/*TODO*///				if (prevop->flags & FL_MBRW)
/*TODO*///					irmb_latch = irmb_din(prevop);
/*TODO*///				else
/*TODO*///					irmb_latch = Y;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		profiler_mark(PROFILER_END);
/*TODO*///	
/*TODO*///		logerror("%d instructions for Mathbox \n", icount);
/*TODO*///	
/*TODO*///	
/*TODO*///	/*TODO*///#if IR_TIMING
/*TODO*///		if (irmb_running == 0)
/*TODO*///		{
/*TODO*///			irmb_timer = timer_set (TIME_IN_HZ(12000000) * icount, 0, irmb_done_callback);
/*TODO*///			logerror("mb start ");
/*TODO*///			/*TODO*///IR_CPU_STATE;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			logerror("mb start [busy!] ");
/*TODO*///			/*TODO*///IR_CPU_STATE;
/*TODO*///			timer_reset (irmb_timer, TIME_IN_NSEC(200) * icount);
/*TODO*///		}
/*TODO*///	/*TODO*///#else
/*TODO*///		cpu_set_irq_line(0, M6809_FIRQ_LINE, ASSERT_LINE);
/*TODO*///	/*TODO*///#endif
/*TODO*///		irmb_running=1;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	#if DISASSEMBLE_MB_ROM
/*TODO*///	void disassemble_instruction(irmb_ops *op)
/*TODO*///	{
/*TODO*///		int lp;
/*TODO*///	
/*TODO*///		if (i==0)
/*TODO*///			logerror(" Address  a b func stor: Q :Y, R, S RDCSAESM da m rs\n");
/*TODO*///		logerror("%04X    : ",i);
/*TODO*///		logerror("%X ",op->areg);
/*TODO*///		logerror("%X ",op->breg);
/*TODO*///	
/*TODO*///		lp=(op->func & 0x38)>>3;
/*TODO*///		if ((lp&1)==0)
/*TODO*///			lp|=1;
/*TODO*///		else if((op->flags & FL_DIV) != 0)
/*TODO*///			lp&=6;
/*TODO*///		else
/*TODO*///			logerror("*");
/*TODO*///	
/*TODO*///		switch (lp)
/*TODO*///		{
/*TODO*///			case 0:
/*TODO*///				logerror("ADD  ");
/*TODO*///				break;
/*TODO*///			case 1:
/*TODO*///				logerror("SUBR ");
/*TODO*///				break;
/*TODO*///			case 2:
/*TODO*///				logerror("SUB  ");
/*TODO*///				break;
/*TODO*///			case 3:
/*TODO*///				logerror("OR   ");
/*TODO*///				break;
/*TODO*///			case 4:
/*TODO*///				logerror("AND  ");
/*TODO*///				break;
/*TODO*///			case 5:
/*TODO*///				logerror("AND  ");
/*TODO*///				break;
/*TODO*///			case 6:
/*TODO*///				logerror("XOR  ");
/*TODO*///				break;
/*TODO*///			case 7:
/*TODO*///				logerror("XNOR ");
/*TODO*///				break;
/*TODO*///		}
/*TODO*///	
/*TODO*///		switch ((op->func & 0x1c0)>>6)
/*TODO*///		{
/*TODO*///			case 0:
/*TODO*///				logerror("  - : Q :F,");
/*TODO*///				break;
/*TODO*///			case 1:
/*TODO*///				logerror("  - : - :F,");
/*TODO*///				break;
/*TODO*///			case 2:
/*TODO*///				logerror("  R%x: - :A,",op->breg);
/*TODO*///				break;
/*TODO*///			case 3:
/*TODO*///				logerror("  R%x: - :F,",op->breg);
/*TODO*///				break;
/*TODO*///			case 4:
/*TODO*///				logerror(">>R%x:>>Q:F,",op->breg);
/*TODO*///				break;
/*TODO*///			case 5:
/*TODO*///				logerror(">>R%x: - :F,",op->breg);
/*TODO*///				break;
/*TODO*///			case 6:
/*TODO*///				logerror("<<R%x:<<Q:F,",op->breg);
/*TODO*///				break;
/*TODO*///			case 7:
/*TODO*///				logerror("<<R%x: - :F,",op->breg);
/*TODO*///				break;
/*TODO*///		}
/*TODO*///	
/*TODO*///		lp=(op->func & 0x7);
/*TODO*///		if ((lp&2)==0)
/*TODO*///			lp|=2;
/*TODO*///		else if((op->flags & FL_MULT) == 0)
/*TODO*///			lp&=5;
/*TODO*///		else
/*TODO*///			logerror("*");
/*TODO*///	
/*TODO*///		switch (lp)
/*TODO*///		{
/*TODO*///			case 0:
/*TODO*///				logerror("R%x, Q ",op->areg);
/*TODO*///				break;
/*TODO*///			case 1:
/*TODO*///				logerror("R%x,R%x ",op->areg,op->breg);
/*TODO*///				break;
/*TODO*///			case 2:
/*TODO*///				logerror("00, Q ");
/*TODO*///				break;
/*TODO*///			case 3:
/*TODO*///				logerror("00,R%x ",op->breg);
/*TODO*///				break;
/*TODO*///			case 4:
/*TODO*///				logerror("00,R%x ",op->areg);
/*TODO*///				break;
/*TODO*///			case 5:
/*TODO*///				logerror(" D,R%x ",op->areg);
/*TODO*///				break;
/*TODO*///			case 6:
/*TODO*///				logerror(" D, Q ");
/*TODO*///				break;
/*TODO*///			case 7:
/*TODO*///				logerror(" D,00 ");
/*TODO*///				break;
/*TODO*///		}
/*TODO*///	
/*TODO*///		for (lp=0;lp<8;lp++)
/*TODO*///			if (op->flags & (0x80>>lp))
/*TODO*///				logerror("1");
/*TODO*///			else
/*TODO*///				logerror("0");
/*TODO*///	
/*TODO*///		logerror(" %02X ",op->diradd);
/*TODO*///		logerror("%X\n",op->ramsel);
/*TODO*///		if (op->jtype)
/*TODO*///		{
/*TODO*///			logerror("              ");
/*TODO*///			switch (op->jtype)
/*TODO*///			{
/*TODO*///				case 1:
/*TODO*///					logerror("BO ");
/*TODO*///					break;
/*TODO*///				case 2:
/*TODO*///					logerror("BZ ");
/*TODO*///					break;
/*TODO*///				case 3:
/*TODO*///					logerror("BH ");
/*TODO*///					break;
/*TODO*///				case 4:
/*TODO*///					logerror("BL ");
/*TODO*///					break;
/*TODO*///				case 5:
/*TODO*///					logerror("B  ");
/*TODO*///					break;
/*TODO*///				case 6:
/*TODO*///					logerror("Cl ");
/*TODO*///					break;
/*TODO*///				case 7:
/*TODO*///					logerror("Return\n\n");
/*TODO*///					break;
/*TODO*///			}
/*TODO*///			if (op->jtype != 7) logerror("  %04X    \n",op->nxtadd);
/*TODO*///			if (op->jtype == 5) logerror("\n");
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	#endif
}
