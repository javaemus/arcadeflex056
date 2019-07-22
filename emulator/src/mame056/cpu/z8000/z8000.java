/*****************************************************************************
 *
 *	 z8000.c
 *	 Portable Z8000(2) emulator
 *	 Z8000 MAME interface
 *
 *	 Copyright (C) 1998,1999 Juergen Buchmueller, all rights reserved.
 *	 Bug fixes and MSB_FIRST compliance Ernesto Corvi.
 *
 *	 - This source code is released as freeware for non-commercial purposes.
 *	 - You are free to use and redistribute this code in modified or
 *	   unmodified form, provided you list me in the credits.
 *	 - If you modify this source code, you must add a notice to each modified
 *	   source file that it has been changed.  If you're a nice person, you
 *	   will clearly mark each change too.  :)
 *	 - If you wish to use this for commercial purposes, please contact me at
 *	   pullmoll@t-online.de
 *	 - The author of this copywritten work reserves the right to change the
 *     terms of its usage and license at any time, including retroactively
 *   - This entire notice must remain in the source code.
 *
 *****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.cpu.z8000;

import static mame056.cpu.z8000.z8000H.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static arcadeflex036.osdepend.*;

public class z8000 extends cpu_interface
{
    
    public z8000() {
        cpu_num = CPU_Z8000;
        num_irqs = 2;
        default_vector = 0;
        icount = z8000_ICount;
        overclock = 1.00;
        irq_int = -1000;
        databus_width = 8;
        pgm_memory_base = 0;
        address_shift = 0;
        address_bits = 16;
        endianess = CPU_IS_BE;
        align_unit = 2;
        max_inst_len = 6;

    }

    @Override
    public void init() {
        // nothing to do
    }

    @Override
    public void reset(Object param) {
        z8000_reset(param);
    }

    @Override
    public void exit() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int execute(int cycles) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object init_context() {
        Object reg = new z8000_Regs();
        return reg;
    }

    @Override
    public Object get_context() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_context(Object reg) {
        z8000_set_context(reg);
    }

    @Override
    public int[] get_cycle_table(int which) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_cycle_table(int which, int[] new_table) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int get_reg(int regnum) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_reg(int regnum, int val) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_irq_line(int irqline, int linestate) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_irq_callback(irqcallbacksPtr callback) {
        z8000_set_irq_callback(callback);
    }

    @Override
    public String cpu_info(Object context, int regnum) {
        return z8000_info(context, regnum);
    }

    @Override
    public String cpu_dasm(String buffer, int pc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int memory_read(int offset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void memory_write(int offset, int data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int internal_read(int offset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void internal_write(int offset, int data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_op_base(int pc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int mem_address_bits_of_cpu() {
        return 16;
    }
	
/*TODO*///	#define VERBOSE 0
/*TODO*///	
/*TODO*///	
/*TODO*///	#if VERBOSE
/*TODO*///	#define LOG(x)	logerror x
/*TODO*///	#else
/*TODO*///	#define LOG(x)
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	static UINT8 z8000_reg_layout[] = {
/*TODO*///		Z8000_PC, Z8000_NSP, Z8000_FCW, Z8000_PSAP, Z8000_REFRESH, -1,
/*TODO*///		Z8000_R0, Z8000_R1, Z8000_R2, Z8000_R3, Z8000_R4, Z8000_R5, Z8000_R6, Z8000_R7, -1,
/*TODO*///		Z8000_R8, Z8000_R9, Z8000_R10,Z8000_R11,Z8000_R12,Z8000_R13,Z8000_R14,Z8000_R15,-1,
/*TODO*///		Z8000_IRQ_REQ, Z8000_IRQ_SRV, Z8000_IRQ_VEC, Z8000_NMI_STATE, Z8000_NVI_STATE, Z8000_VI_STATE, 0
/*TODO*///	};
/*TODO*///	
/*TODO*///	static UINT8 z8000_win_layout[] = {
/*TODO*///		 0, 0,80, 4,	/* register window (top rows) */
/*TODO*///		 0, 5,26,17,	/* disassembler window (left colums) */
/*TODO*///		27, 5,53, 8,	/* memory #1 window (right, upper middle) */
/*TODO*///		27,14,53, 8,	/* memory #2 window (right, lower middle) */
/*TODO*///		 0,23,80, 1,	/* command line window (bottom rows) */
/*TODO*///	};
/*TODO*///	
/*TODO*///	/* opcode execution table */
/*TODO*///	Z8000_exec *z8000_exec = NULL;
/*TODO*///	
/*TODO*///	typedef union {
/*TODO*///	    UINT8   B[16]; /* RL0,RH0,RL1,RH1...RL7,RH7 */
/*TODO*///	    UINT16  W[16]; /* R0,R1,R2...R15 */
/*TODO*///	    UINT32  L[8];  /* RR0,RR2,RR4..RR14 */
/*TODO*///	    UINT64  Q[4];  /* RQ0,RQ4,..RQ12 */
/*TODO*///	}   z8000_reg_file;
/*TODO*///	
        public class z8000_Regs {
/*TODO*///	    UINT16  op[4];      /* opcodes/data of current instruction */
/*TODO*///		UINT16	ppc;		/* previous program counter */
	    public int  pc;         /* program counter */
/*TODO*///	    UINT16  psap;       /* program status pointer */
/*TODO*///	    UINT16  fcw;        /* flags and control word */
/*TODO*///	    UINT16  refresh;    /* refresh timer/counter */
/*TODO*///	    UINT16  nsp;        /* system stack pointer */
/*TODO*///	    UINT16  irq_req;    /* CPU is halted, interrupt or trap request */
/*TODO*///	    UINT16  irq_srv;    /* serviced interrupt request */
/*TODO*///	    UINT16  irq_vec;    /* interrupt vector */
/*TODO*///	    z8000_reg_file regs;/* registers */
/*TODO*///		int nmi_state;		/* NMI line state */
/*TODO*///		int irq_state[2];	/* IRQ line states (NVI, VI) */
	    public irqcallbacksPtr irq_callback;
	};

        public static int[] z8000_ICount = new int[1];
	
	/* current CPU context */
	static z8000_Regs Z;
	
/*TODO*///	/* zero, sign and parity flags for logical byte operations */
/*TODO*///	static UINT8 z8000_zsp[256];
/*TODO*///	
/*TODO*///	/* conversion table for Z8000 DAB opcode */
/*TODO*///	
/*TODO*///	/**************************************************************************
/*TODO*///	 * This is the register file layout:
/*TODO*///	 *
/*TODO*///	 * BYTE 	   WORD 		LONG		   QUAD
/*TODO*///	 * msb	 lsb	   bits 		  bits			 bits
/*TODO*///	 * RH0 - RL0   R 0 15- 0	RR 0  31-16    RQ 0  63-48
/*TODO*///	 * RH1 - RL1   R 1 15- 0		  15- 0 		 47-32
/*TODO*///	 * RH2 - RL2   R 2 15- 0	RR 2  31-16 		 31-16
/*TODO*///	 * RH3 - RL3   R 3 15- 0		  15- 0 		 15- 0
/*TODO*///	 * RH4 - RL4   R 4 15- 0	RR 4  31-16    RQ 4  63-48
/*TODO*///	 * RH5 - RL5   R 5 15- 0		  15- 0 		 47-32
/*TODO*///	 * RH6 - RL6   R 6 15- 0	RR 6  31-16 		 31-16
/*TODO*///	 * RH7 - RL7   R 7 15- 0		  15- 0 		 15- 0
/*TODO*///	 *			   R 8 15- 0	RR 8  31-16    RQ 8  63-48
/*TODO*///	 *			   R 9 15- 0		  15- 0 		 47-32
/*TODO*///	 *			   R10 15- 0	RR10  31-16 		 31-16
/*TODO*///	 *			   R11 15- 0		  15- 0 		 15- 0
/*TODO*///	 *			   R12 15- 0	RR12  31-16    RQ12  63-48
/*TODO*///	 *			   R13 15- 0		  15- 0 		 47-32
/*TODO*///	 *			   R14 15- 0	RR14  31-16 		 31-16
/*TODO*///	 *			   R15 15- 0		  15- 0 		 15- 0
/*TODO*///	 *
/*TODO*///	 * Note that for LSB_FIRST machines we have the case that the RR registers
/*TODO*///	 * use the lower numbered R registers in the higher bit positions.
/*TODO*///	 * And also the RQ registers use the lower numbered RR registers in the
/*TODO*///	 * higher bit positions.
/*TODO*///	 * That's the reason for the ordering in the following pointer table.
/*TODO*///	 **************************************************************************/
/*TODO*///	#ifdef	LSB_FIRST
/*TODO*///		/* pointers to byte (8bit) registers */
/*TODO*///		static UINT8	*pRB[16] =
/*TODO*///		{
/*TODO*///			&Z.regs.B[ 7],&Z.regs.B[ 5],&Z.regs.B[ 3],&Z.regs.B[ 1],
/*TODO*///			&Z.regs.B[15],&Z.regs.B[13],&Z.regs.B[11],&Z.regs.B[ 9],
/*TODO*///			&Z.regs.B[ 6],&Z.regs.B[ 4],&Z.regs.B[ 2],&Z.regs.B[ 0],
/*TODO*///			&Z.regs.B[14],&Z.regs.B[12],&Z.regs.B[10],&Z.regs.B[ 8]
/*TODO*///		};
/*TODO*///	
/*TODO*///		static UINT16	*pRW[16] =
/*TODO*///		{
/*TODO*///	        &Z.regs.W[ 3],&Z.regs.W[ 2],&Z.regs.W[ 1],&Z.regs.W[ 0],
/*TODO*///	        &Z.regs.W[ 7],&Z.regs.W[ 6],&Z.regs.W[ 5],&Z.regs.W[ 4],
/*TODO*///	        &Z.regs.W[11],&Z.regs.W[10],&Z.regs.W[ 9],&Z.regs.W[ 8],
/*TODO*///	        &Z.regs.W[15],&Z.regs.W[14],&Z.regs.W[13],&Z.regs.W[12]
/*TODO*///	    };
/*TODO*///	
/*TODO*///	    /* pointers to long (32bit) registers */
/*TODO*///		static UINT32	*pRL[16] =
/*TODO*///		{
/*TODO*///			&Z.regs.L[ 1],&Z.regs.L[ 1],&Z.regs.L[ 0],&Z.regs.L[ 0],
/*TODO*///			&Z.regs.L[ 3],&Z.regs.L[ 3],&Z.regs.L[ 2],&Z.regs.L[ 2],
/*TODO*///			&Z.regs.L[ 5],&Z.regs.L[ 5],&Z.regs.L[ 4],&Z.regs.L[ 4],
/*TODO*///			&Z.regs.L[ 7],&Z.regs.L[ 7],&Z.regs.L[ 6],&Z.regs.L[ 6]
/*TODO*///	    };
/*TODO*///	
/*TODO*///	#else	/* MSB_FIRST */
/*TODO*///	
/*TODO*///	    /* pointers to byte (8bit) registers */
/*TODO*///		static UINT8	*pRB[16] =
/*TODO*///		{
/*TODO*///			&Z.regs.B[ 0],&Z.regs.B[ 2],&Z.regs.B[ 4],&Z.regs.B[ 6],
/*TODO*///			&Z.regs.B[ 8],&Z.regs.B[10],&Z.regs.B[12],&Z.regs.B[14],
/*TODO*///			&Z.regs.B[ 1],&Z.regs.B[ 3],&Z.regs.B[ 5],&Z.regs.B[ 7],
/*TODO*///			&Z.regs.B[ 9],&Z.regs.B[11],&Z.regs.B[13],&Z.regs.B[15]
/*TODO*///		};
/*TODO*///	
/*TODO*///		/* pointers to word (16bit) registers */
/*TODO*///		static UINT16	*pRW[16] =
/*TODO*///		{
/*TODO*///			&Z.regs.W[ 0],&Z.regs.W[ 1],&Z.regs.W[ 2],&Z.regs.W[ 3],
/*TODO*///			&Z.regs.W[ 4],&Z.regs.W[ 5],&Z.regs.W[ 6],&Z.regs.W[ 7],
/*TODO*///			&Z.regs.W[ 8],&Z.regs.W[ 9],&Z.regs.W[10],&Z.regs.W[11],
/*TODO*///			&Z.regs.W[12],&Z.regs.W[13],&Z.regs.W[14],&Z.regs.W[15]
/*TODO*///		};
/*TODO*///	
/*TODO*///		/* pointers to long (32bit) registers */
/*TODO*///		static UINT32	*pRL[16] =
/*TODO*///		{
/*TODO*///			&Z.regs.L[ 0],&Z.regs.L[ 0],&Z.regs.L[ 1],&Z.regs.L[ 1],
/*TODO*///			&Z.regs.L[ 2],&Z.regs.L[ 2],&Z.regs.L[ 3],&Z.regs.L[ 3],
/*TODO*///			&Z.regs.L[ 4],&Z.regs.L[ 4],&Z.regs.L[ 5],&Z.regs.L[ 5],
/*TODO*///			&Z.regs.L[ 6],&Z.regs.L[ 6],&Z.regs.L[ 7],&Z.regs.L[ 7]
/*TODO*///		};
/*TODO*///	
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	/* pointers to quad word (64bit) registers */
/*TODO*///	static UINT64   *pRQ[16] = {
/*TODO*///	    &Z.regs.Q[ 0],&Z.regs.Q[ 0],&Z.regs.Q[ 0],&Z.regs.Q[ 0],
/*TODO*///	    &Z.regs.Q[ 1],&Z.regs.Q[ 1],&Z.regs.Q[ 1],&Z.regs.Q[ 1],
/*TODO*///	    &Z.regs.Q[ 2],&Z.regs.Q[ 2],&Z.regs.Q[ 2],&Z.regs.Q[ 2],
/*TODO*///	    &Z.regs.Q[ 3],&Z.regs.Q[ 3],&Z.regs.Q[ 3],&Z.regs.Q[ 3]};
/*TODO*///	
/*TODO*///	INLINE UINT16 RDOP(void)
/*TODO*///	{
/*TODO*///		UINT16 res = cpu_readop16(PC);
/*TODO*///	    PC += 2;
/*TODO*///	    return res;
/*TODO*///	}
/*TODO*///	
/*TODO*///	INLINE UINT8 RDMEM_B(UINT16 addr)
/*TODO*///	{
/*TODO*///		return cpu_readmem16bew(addr);
/*TODO*///	}
/*TODO*///	
/*TODO*///	INLINE UINT16 RDMEM_W(UINT16 addr)
/*TODO*///	{
/*TODO*///		addr &= ~1;
/*TODO*///		return cpu_readmem16bew_word(addr);
/*TODO*///	}
/*TODO*///	
/*TODO*///	INLINE UINT32 RDMEM_L(UINT16 addr)
/*TODO*///	{
/*TODO*///		UINT32 result;
/*TODO*///		addr &= ~1;
/*TODO*///		result = cpu_readmem16bew_word(addr) << 16;
/*TODO*///		return result + cpu_readmem16bew_word(addr + 2);
/*TODO*///	}
/*TODO*///	
/*TODO*///	INLINE void WRMEM_B(UINT16 addr, UINT8 value)
/*TODO*///	{
/*TODO*///		cpu_writemem16bew(addr, value);
/*TODO*///	}
/*TODO*///	
/*TODO*///	INLINE void WRMEM_W(UINT16 addr, UINT16 value)
/*TODO*///	{
/*TODO*///		addr &= ~1;
/*TODO*///		cpu_writemem16bew_word(addr, value);
/*TODO*///	}
/*TODO*///	
/*TODO*///	INLINE void WRMEM_L(UINT16 addr, UINT32 value)
/*TODO*///	{
/*TODO*///		addr &= ~1;
/*TODO*///		cpu_writemem16bew_word(addr, value >> 16);
/*TODO*///		cpu_writemem16bew_word((UINT16)(addr + 2), value & 0xffff);
/*TODO*///	}
/*TODO*///	
/*TODO*///	INLINE UINT8 RDPORT_B(int mode, UINT16 addr)
/*TODO*///	{
/*TODO*///		if( mode == 0 )
/*TODO*///		{
/*TODO*///			return cpu_readport16(addr);
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			/* how to handle MMU reads? */
/*TODO*///			return 0x00;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	INLINE UINT16 RDPORT_W(int mode, UINT16 addr)
/*TODO*///	{
/*TODO*///		if( mode == 0 )
/*TODO*///		{
/*TODO*///			return cpu_readport16((UINT16)(addr)) +
/*TODO*///				  (cpu_readport16((UINT16)(addr+1)) << 8);
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			/* how to handle MMU reads? */
/*TODO*///			return 0x0000;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	INLINE UINT32 RDPORT_L(int mode, UINT16 addr)
/*TODO*///	{
/*TODO*///		if( mode == 0 )
/*TODO*///		{
/*TODO*///			return	cpu_readport16((UINT16)(addr)) +
/*TODO*///				   (cpu_readport16((UINT16)(addr+1)) <<  8) +
/*TODO*///				   (cpu_readport16((UINT16)(addr+2)) << 16) +
/*TODO*///				   (cpu_readport16((UINT16)(addr+3)) << 24);
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			/* how to handle MMU reads? */
/*TODO*///			return 0x00000000;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	INLINE void WRPORT_B(int mode, UINT16 addr, UINT8 value)
/*TODO*///	{
/*TODO*///		if( mode == 0 )
/*TODO*///		{
/*TODO*///	        cpu_writeport16(addr,value);
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			/* how to handle MMU writes? */
/*TODO*///	    }
/*TODO*///	}
/*TODO*///	
/*TODO*///	INLINE void WRPORT_W(int mode, UINT16 addr, UINT16 value)
/*TODO*///	{
/*TODO*///		if( mode == 0 )
/*TODO*///		{
/*TODO*///			cpu_writeport16((UINT16)(addr),value & 0xff);
/*TODO*///			cpu_writeport16((UINT16)(addr+1),(value >> 8) & 0xff);
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			/* how to handle MMU writes? */
/*TODO*///	    }
/*TODO*///	}
/*TODO*///	
/*TODO*///	INLINE void WRPORT_L(int mode, UINT16 addr, UINT32 value)
/*TODO*///	{
/*TODO*///		if( mode == 0 )
/*TODO*///		{
/*TODO*///			cpu_writeport16((UINT16)(addr),value & 0xff);
/*TODO*///			cpu_writeport16((UINT16)(addr+1),(value >> 8) & 0xff);
/*TODO*///			cpu_writeport16((UINT16)(addr+2),(value >> 16) & 0xff);
/*TODO*///			cpu_writeport16((UINT16)(addr+3),(value >> 24) & 0xff);
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			/* how to handle MMU writes? */
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	INLINE void set_irq(int type)
/*TODO*///	{
/*TODO*///	    switch ((type >> 8) & 255)
/*TODO*///	    {
/*TODO*///	        case Z8000_TRAP >> 8:
/*TODO*///	            if (IRQ_SRV >= Z8000_TRAP)
/*TODO*///	                return; /* double TRAP.. very bad :( */
/*TODO*///	            IRQ_REQ = type;
/*TODO*///	            break;
/*TODO*///	        case Z8000_NMI >> 8:
/*TODO*///	            if (IRQ_SRV >= Z8000_NMI)
/*TODO*///	                return; /* no NMIs inside trap */
/*TODO*///	            IRQ_REQ = type;
/*TODO*///	            break;
/*TODO*///	        case Z8000_SEGTRAP >> 8:
/*TODO*///	            if (IRQ_SRV >= Z8000_SEGTRAP)
/*TODO*///	                return; /* no SEGTRAPs inside NMI/TRAP */
/*TODO*///	            IRQ_REQ = type;
/*TODO*///	            break;
/*TODO*///	        case Z8000_NVI >> 8:
/*TODO*///	            if (IRQ_SRV >= Z8000_NVI)
/*TODO*///	                return; /* no NVIs inside SEGTRAP/NMI/TRAP */
/*TODO*///	            IRQ_REQ = type;
/*TODO*///	            break;
/*TODO*///	        case Z8000_VI >> 8:
/*TODO*///	            if (IRQ_SRV >= Z8000_VI)
/*TODO*///	                return; /* no VIs inside NVI/SEGTRAP/NMI/TRAP */
/*TODO*///	            IRQ_REQ = type;
/*TODO*///	            break;
/*TODO*///	        case Z8000_SYSCALL >> 8:
/*TODO*///	            LOG(("Z8K#%d SYSCALL $%02x\n", cpu_getactivecpu(), type & 0xff));
/*TODO*///	            IRQ_REQ = type;
/*TODO*///	            break;
/*TODO*///	        default:
/*TODO*///	            logerror("Z8000 invalid Cause_Interrupt %04x\n", type);
/*TODO*///	            return;
/*TODO*///	    }
/*TODO*///	    /* set interrupt request flag, reset HALT flag */
/*TODO*///	    IRQ_REQ = type & ~Z8000_HALT;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	INLINE void Interrupt(void)
/*TODO*///	{
/*TODO*///	    UINT16 fcw = FCW;
/*TODO*///	
/*TODO*///	    if (IRQ_REQ & Z8000_NVI)
/*TODO*///	    {
/*TODO*///	        int type = (*Z.irq_callback)(0);
/*TODO*///	        set_irq(type);
/*TODO*///	    }
/*TODO*///	
/*TODO*///	    if (IRQ_REQ & Z8000_VI)
/*TODO*///	    {
/*TODO*///	        int type = (*Z.irq_callback)(1);
/*TODO*///	        set_irq(type);
/*TODO*///	    }
/*TODO*///	
/*TODO*///	   /* trap ? */
/*TODO*///	   if ( IRQ_REQ & Z8000_TRAP )
/*TODO*///	   {
/*TODO*///	        CHANGE_FCW(fcw | F_S_N);/* swap to system stack */
/*TODO*///	        PUSHW( SP, PC );        /* save current PC */
/*TODO*///	        PUSHW( SP, fcw );       /* save current FCW */
/*TODO*///	        PUSHW( SP, IRQ_REQ );   /* save interrupt/trap type tag */
/*TODO*///	        IRQ_SRV = IRQ_REQ;
/*TODO*///	        IRQ_REQ &= ~Z8000_TRAP;
/*TODO*///	        PC = TRAP;
/*TODO*///	        LOG(("Z8K#%d trap $%04x\n", cpu_getactivecpu(), PC ));
/*TODO*///	   }
/*TODO*///	   else
/*TODO*///	   if ( IRQ_REQ & Z8000_SYSCALL )
/*TODO*///	   {
/*TODO*///	        CHANGE_FCW(fcw | F_S_N);/* swap to system stack */
/*TODO*///	        PUSHW( SP, PC );        /* save current PC */
/*TODO*///	        PUSHW( SP, fcw );       /* save current FCW */
/*TODO*///	        PUSHW( SP, IRQ_REQ );   /* save interrupt/trap type tag */
/*TODO*///	        IRQ_SRV = IRQ_REQ;
/*TODO*///	        IRQ_REQ &= ~Z8000_SYSCALL;
/*TODO*///	        PC = SYSCALL;
/*TODO*///	        LOG(("Z8K#%d syscall $%04x\n", cpu_getactivecpu(), PC ));
/*TODO*///	   }
/*TODO*///	   else
/*TODO*///	   if ( IRQ_REQ & Z8000_SEGTRAP )
/*TODO*///	   {
/*TODO*///	        CHANGE_FCW(fcw | F_S_N);/* swap to system stack */
/*TODO*///	        PUSHW( SP, PC );        /* save current PC */
/*TODO*///	        PUSHW( SP, fcw );       /* save current FCW */
/*TODO*///	        PUSHW( SP, IRQ_REQ );   /* save interrupt/trap type tag */
/*TODO*///	        IRQ_SRV = IRQ_REQ;
/*TODO*///	        IRQ_REQ &= ~Z8000_SEGTRAP;
/*TODO*///	        PC = SEGTRAP;
/*TODO*///	        LOG(("Z8K#%d segtrap $%04x\n", cpu_getactivecpu(), PC ));
/*TODO*///	   }
/*TODO*///	   else
/*TODO*///	   if ( IRQ_REQ & Z8000_NMI )
/*TODO*///	   {
/*TODO*///	        CHANGE_FCW(fcw | F_S_N);/* swap to system stack */
/*TODO*///	        PUSHW( SP, PC );        /* save current PC */
/*TODO*///	        PUSHW( SP, fcw );       /* save current FCW */
/*TODO*///	        PUSHW( SP, IRQ_REQ );   /* save interrupt/trap type tag */
/*TODO*///	        IRQ_SRV = IRQ_REQ;
/*TODO*///	        fcw = RDMEM_W( NMI );
/*TODO*///	        PC = RDMEM_W( NMI + 2 );
/*TODO*///	        IRQ_REQ &= ~Z8000_NMI;
/*TODO*///	        CHANGE_FCW(fcw);
/*TODO*///	        PC = NMI;
/*TODO*///	        LOG(("Z8K#%d NMI $%04x\n", cpu_getactivecpu(), PC ));
/*TODO*///	    }
/*TODO*///	    else
/*TODO*///	    if ( (IRQ_REQ & Z8000_NVI) && (FCW & F_NVIE) )
/*TODO*///	    {
/*TODO*///	        CHANGE_FCW(fcw | F_S_N);/* swap to system stack */
/*TODO*///	        PUSHW( SP, PC );        /* save current PC */
/*TODO*///	        PUSHW( SP, fcw );       /* save current FCW */
/*TODO*///	        PUSHW( SP, IRQ_REQ );   /* save interrupt/trap type tag */
/*TODO*///	        IRQ_SRV = IRQ_REQ;
/*TODO*///	        fcw = RDMEM_W( NVI );
/*TODO*///	        PC = RDMEM_W( NVI + 2 );
/*TODO*///	        IRQ_REQ &= ~Z8000_NVI;
/*TODO*///	        CHANGE_FCW(fcw);
/*TODO*///	        LOG(("Z8K#%d NVI $%04x\n", cpu_getactivecpu(), PC ));
/*TODO*///	    }
/*TODO*///	    else
/*TODO*///	    if ( (IRQ_REQ & Z8000_VI) && (FCW & F_VIE) )
/*TODO*///	    {
/*TODO*///	        CHANGE_FCW(fcw | F_S_N);/* swap to system stack */
/*TODO*///	        PUSHW( SP, PC );        /* save current PC */
/*TODO*///	        PUSHW( SP, fcw );       /* save current FCW */
/*TODO*///	        PUSHW( SP, IRQ_REQ );   /* save interrupt/trap type tag */
/*TODO*///	        IRQ_SRV = IRQ_REQ;
/*TODO*///	        fcw = RDMEM_W( IRQ_VEC );
/*TODO*///	        PC = RDMEM_W( VEC00 + 2 * (IRQ_REQ & 0xff) );
/*TODO*///	        IRQ_REQ &= ~Z8000_VI;
/*TODO*///	        CHANGE_FCW(fcw);
/*TODO*///	        LOG(("Z8K#%d VI [$%04x/$%04x] fcw $%04x, pc $%04x\n", cpu_getactivecpu(), IRQ_VEC, VEC00 + VEC00 + 2 * (IRQ_REQ & 0xff), FCW, PC ));
/*TODO*///	    }
/*TODO*///	}
/*TODO*///	
/*TODO*///	
	public void z8000_reset(Object param)
	{
/*TODO*///		memset(&Z, 0, sizeof(z8000_Regs));
/*TODO*///		FCW = RDMEM_W( 2 ); /* get reset FCW */
/*TODO*///		PC	= RDMEM_W( 4 ); /* get reset PC  */
/*TODO*///		change_pc16bew(PC);
	}
	
/*TODO*///	void z8000_exit(void)
/*TODO*///	{
/*TODO*///		z8000_deinit();
/*TODO*///	}
/*TODO*///	
/*TODO*///	int z8000_execute(int cycles)
/*TODO*///	{
/*TODO*///	    z8000_ICount = cycles;
/*TODO*///	
/*TODO*///	    do
/*TODO*///	    {
/*TODO*///	        /* any interrupt request pending? */
/*TODO*///	        if (IRQ_REQ)
/*TODO*///				Interrupt();
/*TODO*///	
/*TODO*///			CALL_MAME_DEBUG;
/*TODO*///	
/*TODO*///			if (IRQ_REQ & Z8000_HALT)
/*TODO*///	        {
/*TODO*///	            z8000_ICount = 0;
/*TODO*///	        }
/*TODO*///	        else
/*TODO*///	        {
/*TODO*///	            Z8000_exec *exec;
/*TODO*///	            Z.op[0] = RDOP();
/*TODO*///	            exec = &z8000_exec[Z.op[0]];
/*TODO*///	
/*TODO*///	            if (exec->size > 1)
/*TODO*///	                Z.op[1] = RDOP();
/*TODO*///	            if (exec->size > 2)
/*TODO*///	                Z.op[2] = RDOP();
/*TODO*///	
/*TODO*///	            z8000_ICount -= exec->cycles;
/*TODO*///	            (*exec->opcode)();
/*TODO*///	
/*TODO*///	        }
/*TODO*///	    } while (z8000_ICount > 0);
/*TODO*///	
/*TODO*///	    return cycles - z8000_ICount;
/*TODO*///	
/*TODO*///	}
/*TODO*///	
/*TODO*///	unsigned z8000_get_context(void *dst)
/*TODO*///	{
/*TODO*///		if( dst )
/*TODO*///			*(z8000_Regs*)dst = Z;
/*TODO*///	    return sizeof(z8000_Regs);
/*TODO*///	}
/*TODO*///	
	public void z8000_set_context(Object src)
	{
		if( src != null )
		{
			Z = (z8000_Regs)src;
			change_pc16bew(Z.pc);
		}
	}
	
/*TODO*///	unsigned z8000_get_reg(int regnum)
/*TODO*///	{
/*TODO*///		switch( regnum )
/*TODO*///		{
/*TODO*///			case REG_PC:
/*TODO*///			case Z8000_PC: return PC;
/*TODO*///			case REG_SP:
/*TODO*///	        case Z8000_NSP: return NSP;
/*TODO*///	        case Z8000_FCW: return FCW;
/*TODO*///			case Z8000_PSAP: return PSAP;
/*TODO*///			case Z8000_REFRESH: return REFRESH;
/*TODO*///			case Z8000_IRQ_REQ: return IRQ_REQ;
/*TODO*///			case Z8000_IRQ_SRV: return IRQ_SRV;
/*TODO*///			case Z8000_IRQ_VEC: return IRQ_VEC;
/*TODO*///			case Z8000_R0: return RW( 0);
/*TODO*///			case Z8000_R1: return RW( 1);
/*TODO*///			case Z8000_R2: return RW( 2);
/*TODO*///			case Z8000_R3: return RW( 3);
/*TODO*///			case Z8000_R4: return RW( 4);
/*TODO*///			case Z8000_R5: return RW( 5);
/*TODO*///			case Z8000_R6: return RW( 6);
/*TODO*///			case Z8000_R7: return RW( 7);
/*TODO*///			case Z8000_R8: return RW( 8);
/*TODO*///			case Z8000_R9: return RW( 9);
/*TODO*///			case Z8000_R10: return RW(10);
/*TODO*///			case Z8000_R11: return RW(11);
/*TODO*///			case Z8000_R12: return RW(12);
/*TODO*///			case Z8000_R13: return RW(13);
/*TODO*///			case Z8000_R14: return RW(14);
/*TODO*///			case Z8000_R15: return RW(15);
/*TODO*///			case Z8000_NMI_STATE: return Z.nmi_state;
/*TODO*///			case Z8000_NVI_STATE: return Z.irq_state[0];
/*TODO*///			case Z8000_VI_STATE: return Z.irq_state[1];
/*TODO*///			case REG_PREVIOUSPC: return PPC;
/*TODO*///			default:
/*TODO*///				if( regnum <= REG_SP_CONTENTS )
/*TODO*///				{
/*TODO*///					unsigned offset = NSP + 2 * (REG_SP_CONTENTS - regnum);
/*TODO*///					if( offset < 0xffff )
/*TODO*///						return RDMEM_W( offset );
/*TODO*///				}
/*TODO*///		}
/*TODO*///	    return 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	void z8000_set_reg(int regnum, unsigned val)
/*TODO*///	{
/*TODO*///		switch( regnum )
/*TODO*///		{
/*TODO*///			case REG_PC: PC = val; change_pc16bew(PC); break;
/*TODO*///			case Z8000_PC: PC = val; break;
/*TODO*///			case REG_SP:
/*TODO*///			case Z8000_NSP: NSP = val; break;
/*TODO*///			case Z8000_FCW: FCW = val; break;
/*TODO*///			case Z8000_PSAP: PSAP = val; break;
/*TODO*///			case Z8000_REFRESH: REFRESH = val; break;
/*TODO*///			case Z8000_IRQ_REQ: IRQ_REQ = val; break;
/*TODO*///			case Z8000_IRQ_SRV: IRQ_SRV = val; break;
/*TODO*///			case Z8000_IRQ_VEC: IRQ_VEC = val; break;
/*TODO*///			case Z8000_R0: RW( 0) = val; break;
/*TODO*///			case Z8000_R1: RW( 1) = val; break;
/*TODO*///			case Z8000_R2: RW( 2) = val; break;
/*TODO*///			case Z8000_R3: RW( 3) = val; break;
/*TODO*///			case Z8000_R4: RW( 4) = val; break;
/*TODO*///			case Z8000_R5: RW( 5) = val; break;
/*TODO*///			case Z8000_R6: RW( 6) = val; break;
/*TODO*///			case Z8000_R7: RW( 7) = val; break;
/*TODO*///			case Z8000_R8: RW( 8) = val; break;
/*TODO*///			case Z8000_R9: RW( 9) = val; break;
/*TODO*///			case Z8000_R10: RW(10) = val; break;
/*TODO*///			case Z8000_R11: RW(11) = val; break;
/*TODO*///			case Z8000_R12: RW(12) = val; break;
/*TODO*///			case Z8000_R13: RW(13) = val; break;
/*TODO*///			case Z8000_R14: RW(14) = val; break;
/*TODO*///			case Z8000_R15: RW(15) = val; break;
/*TODO*///			case Z8000_NMI_STATE: Z.nmi_state = val; break;
/*TODO*///			case Z8000_NVI_STATE: Z.irq_state[0] = val; break;
/*TODO*///			case Z8000_VI_STATE: Z.irq_state[1] = val; break;
/*TODO*///			default:
/*TODO*///				if( regnum < REG_SP_CONTENTS )
/*TODO*///				{
/*TODO*///					unsigned offset = NSP + 2 * (REG_SP_CONTENTS - regnum);
/*TODO*///					if( offset < 0xffff )
/*TODO*///						WRMEM_W( offset, val & 0xffff );
/*TODO*///				}
/*TODO*///	    }
/*TODO*///	}
/*TODO*///	
/*TODO*///	void z8000_set_irq_line(int irqline, int state)
/*TODO*///	{
/*TODO*///		if (irqline == IRQ_LINE_NMI)
/*TODO*///		{
/*TODO*///			if (Z.nmi_state == state)
/*TODO*///				return;
/*TODO*///	
/*TODO*///		    Z.nmi_state = state;
/*TODO*///	
/*TODO*///		    if (state != CLEAR_LINE)
/*TODO*///			{
/*TODO*///				if (IRQ_SRV >= Z8000_NMI)	/* no NMIs inside trap */
/*TODO*///					return;
/*TODO*///				IRQ_REQ = Z8000_NMI;
/*TODO*///				IRQ_VEC = NMI;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else if (irqline < 2)
/*TODO*///		{
/*TODO*///			Z.irq_state[irqline] = state;
/*TODO*///			if (irqline == 0)
/*TODO*///			{
/*TODO*///				if (state == CLEAR_LINE)
/*TODO*///				{
/*TODO*///					if (!(FCW & F_NVIE))
/*TODO*///						IRQ_REQ &= ~Z8000_NVI;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					if (FCW & F_NVIE)
/*TODO*///						IRQ_REQ |= Z8000_NVI;
/*TODO*///		        }
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				if (state == CLEAR_LINE)
/*TODO*///				{
/*TODO*///					if (!(FCW & F_VIE))
/*TODO*///						IRQ_REQ &= ~Z8000_VI;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					if (FCW & F_VIE)
/*TODO*///						IRQ_REQ |= Z8000_VI;
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
	
	public void z8000_set_irq_callback(irqcallbacksPtr callback)
	{
		Z.irq_callback = callback;
	}
	
	/****************************************************************************
	 * Return a formatted string for a register
	 ****************************************************************************/
	public String z8000_info(Object context, int regnum)
	{
/*TODO*///		static char buffer[32][47+1];
/*TODO*///		static int which = 0;
/*TODO*///		z8000_Regs *r = (z8000_Regs *)context;
/*TODO*///	
/*TODO*///		which = (which+1) % 32;
/*TODO*///	    buffer[which][0] = '\0';
/*TODO*///		if (context == 0)
/*TODO*///			r = &Z;
	
	    switch( regnum )
            {
			case CPU_INFO_NAME: return "Z8002";
			case CPU_INFO_FAMILY: return "Zilog Z8000";
			case CPU_INFO_VERSION: return "1.1";
			case CPU_INFO_FILE: return "z8000.java";
			case CPU_INFO_CREDITS: return "Copyright (C) 1998,1999 Juergen Buchmueller, all rights reserved.";
/*TODO*///	
/*TODO*///			case CPU_INFO_REG_LAYOUT: return (const char*)z8000_reg_layout;
/*TODO*///			case CPU_INFO_WIN_LAYOUT: return (const char*)z8000_win_layout;
/*TODO*///	
/*TODO*///	        case CPU_INFO_FLAGS:
/*TODO*///				sprintf(buffer[which], "%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c",
/*TODO*///					r->fcw & 0x8000 ? 's':'.',
/*TODO*///					r->fcw & 0x4000 ? 'n':'.',
/*TODO*///					r->fcw & 0x2000 ? 'e':'.',
/*TODO*///					r->fcw & 0x1000 ? '2':'.',
/*TODO*///					r->fcw & 0x0800 ? '1':'.',
/*TODO*///					r->fcw & 0x0400 ? '?':'.',
/*TODO*///					r->fcw & 0x0200 ? '?':'.',
/*TODO*///					r->fcw & 0x0100 ? '?':'.',
/*TODO*///					r->fcw & 0x0080 ? 'C':'.',
/*TODO*///					r->fcw & 0x0040 ? 'Z':'.',
/*TODO*///					r->fcw & 0x0020 ? 'S':'.',
/*TODO*///					r->fcw & 0x0010 ? 'V':'.',
/*TODO*///					r->fcw & 0x0008 ? 'D':'.',
/*TODO*///					r->fcw & 0x0004 ? 'H':'.',
/*TODO*///					r->fcw & 0x0002 ? '?':'.',
/*TODO*///					r->fcw & 0x0001 ? '?':'.');
/*TODO*///	            break;
/*TODO*///			case CPU_INFO_REG+Z8000_PC: sprintf(buffer[which], "PC :%04X", r->pc); break;
/*TODO*///			case CPU_INFO_REG+Z8000_NSP: sprintf(buffer[which], "SP :%04X", r->nsp); break;
/*TODO*///			case CPU_INFO_REG+Z8000_FCW: sprintf(buffer[which], "FCW:%04X", r->fcw); break;
/*TODO*///			case CPU_INFO_REG+Z8000_PSAP: sprintf(buffer[which], "NSP:%04X", r->psap); break;
/*TODO*///			case CPU_INFO_REG+Z8000_REFRESH: sprintf(buffer[which], "REFR:%04X", r->refresh); break;
/*TODO*///			case CPU_INFO_REG+Z8000_IRQ_REQ: sprintf(buffer[which], "IRQR:%04X", r->irq_req); break;
/*TODO*///			case CPU_INFO_REG+Z8000_IRQ_SRV: sprintf(buffer[which], "IRQS:%04X", r->irq_srv); break;
/*TODO*///			case CPU_INFO_REG+Z8000_IRQ_VEC: sprintf(buffer[which], "IRQV:%04X", r->irq_vec); break;
/*TODO*///	#ifdef	LSB_FIRST
/*TODO*///	#define REG_XOR 3
/*TODO*///	#else
/*TODO*///	#define REG_XOR 0
/*TODO*///	#endif
/*TODO*///			case CPU_INFO_REG+Z8000_R0: sprintf(buffer[which], "R0 :%04X", r->regs.W[ 0^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R1: sprintf(buffer[which], "R1 :%04X", r->regs.W[ 1^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R2: sprintf(buffer[which], "R2 :%04X", r->regs.W[ 2^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R3: sprintf(buffer[which], "R3 :%04X", r->regs.W[ 3^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R4: sprintf(buffer[which], "R4 :%04X", r->regs.W[ 4^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R5: sprintf(buffer[which], "R5 :%04X", r->regs.W[ 5^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R6: sprintf(buffer[which], "R6 :%04X", r->regs.W[ 6^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R7: sprintf(buffer[which], "R7 :%04X", r->regs.W[ 7^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R8: sprintf(buffer[which], "R8 :%04X", r->regs.W[ 8^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R9: sprintf(buffer[which], "R9 :%04X", r->regs.W[ 9^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R10: sprintf(buffer[which], "R10:%04X", r->regs.W[10^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R11: sprintf(buffer[which], "R11:%04X", r->regs.W[11^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R12: sprintf(buffer[which], "R12:%04X", r->regs.W[12^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R13: sprintf(buffer[which], "R13:%04X", r->regs.W[13^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R14: sprintf(buffer[which], "R14:%04X", r->regs.W[14^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_R15: sprintf(buffer[which], "R15:%04X", r->regs.W[15^REG_XOR]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_NMI_STATE: sprintf(buffer[which], "NMI:%X", r->nmi_state); break;
/*TODO*///			case CPU_INFO_REG+Z8000_NVI_STATE: sprintf(buffer[which], "NVI:%X", r->irq_state[0]); break;
/*TODO*///			case CPU_INFO_REG+Z8000_VI_STATE: sprintf(buffer[which], "VI :%X", r->irq_state[1]); break;
	    }
/*TODO*///		return buffer[which];
            throw new UnsupportedOperationException("Not supported yet.");
	}
/*TODO*///	
/*TODO*///	
/*TODO*///	unsigned z8000_dasm(char *buffer, unsigned pc)
/*TODO*///	{
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///	    return DasmZ8000(buffer,pc);
/*TODO*///	#else
/*TODO*///		sprintf( buffer, "$%04X", cpu_readop16(pc) );
/*TODO*///		return 2;
/*TODO*///	#endif
/*TODO*///	}
/*TODO*///	
}
