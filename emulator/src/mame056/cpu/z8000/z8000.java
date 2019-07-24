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
import static mame056.cpu.z8000.z8000cpuH.F_NVIE;
import static mame056.cpu.z8000.z8000cpuH.F_VIE;
import static mame056.cpu.z8000.z8000cpuH.RW;
import static mame056.cpu.z8000.z8000cpuH.*;
import static mame056.cpu.z8000.z8000tbl.*;

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
        z8000tbl tab = new z8000tbl();
        tab.z8000_init(); // in z8000tbl class
    }

    @Override
    public void reset(Object param) {
        z8000_reset(param);
    }

    @Override
    public void exit() {
        z8000_exit();
    }

    @Override
    public int execute(int cycles) {
        return z8000_execute(cycles);
    }

    @Override
    public Object init_context() {
        Object reg = new z8000_Regs();
        return reg;
    }

    @Override
    public Object get_context() {
        z8000_Regs Regs = new z8000_Regs();
        
        Regs.op=Z.op;
        Regs.ppc=Z.ppc;
	Regs.pc=Z.pc;
        Regs.psap=Z.psap;
        Regs.fcw=Z.fcw;
        Regs.refresh=Z.refresh;
        Regs.nsp=Z.nsp;
        Regs.irq_req=Z.irq_req;
        Regs.irq_srv=Z.irq_srv;
        Regs.irq_vec=Z.irq_vec;
	Regs.regs=Z.regs;
        Regs.nmi_state=Z.nmi_state;
        Regs.irq_state=Z.irq_state;
	Regs.irq_callback=Z.irq_callback;
        
        return Regs;
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
        return z8000_get_reg(regnum);
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
        cpu_setOPbase16.handler(pc);
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
	
	/* opcode execution table */
	public static Z8000_exec[] z8000_exec = new Z8000_exec[0x10000];
        
        static {
            for (int i=0 ; i<0x10000 ; i++)
                z8000_exec[i] = new Z8000_exec();
        }
	
	public class z8000_reg_file {
	    public int[]  B = new int[16]; /* RL0,RH0,RL1,RH1...RL7,RH7 */
	    public int[]  W = new int[16]; /* R0,R1,R2...R15 */
	    public int[]  L = new int[8];  /* RR0,RR2,RR4..RR14 */
	    public int[]  Q = new int[4];  /* RQ0,RQ4,..RQ12 */
	};
	
        public class z8000_Regs {
	    public int[]  op=new int[4];      /* opcodes/data of current instruction */
            public int	ppc;		/* previous program counter */
	    public int  pc;         /* program counter */
            public int  psap;       /* program status pointer */
            public int  fcw;        /* flags and control word */
            public int  refresh;    /* refresh timer/counter */
            public int  nsp;        /* system stack pointer */
            public int  irq_req;    /* CPU is halted, interrupt or trap request */
            public int  irq_srv;    /* serviced interrupt request */
            public int  irq_vec;    /* interrupt vector */
	    public z8000_reg_file regs = new z8000_reg_file();/* registers */
            public int nmi_state;		/* NMI line state */
            public int[] irq_state = new int[2];	/* IRQ line states (NVI, VI) */
	    public irqcallbacksPtr irq_callback;
	};

        public static int[] z8000_ICount = new int[1];
	
	/* current CPU context */
	static z8000_Regs Z;
	
	/* zero, sign and parity flags for logical byte operations */
	static int[] z8000_zsp = new int[256];

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
	
          public static int pRW(int _pos)
          {
              switch(_pos){
                  case 0:
                      return Z.regs.W[ 3];
                  case 1:
                      return Z.regs.W[ 2];
                  case 2:
                      return Z.regs.W[ 1];
                  case 3:
                      return Z.regs.W[ 0];
                  case 4:
                      return Z.regs.W[ 7];
                  case 5:
                      return Z.regs.W[ 6];
                  case 6:
                      return Z.regs.W[ 5];
                  case 7:
                      return Z.regs.W[ 4];
                  case 8:
                      return Z.regs.W[ 11];
                  case 9:
                      return Z.regs.W[ 10];
                  case 10:
                      return Z.regs.W[ 9];
                  case 11:
                      return Z.regs.W[ 8];
                  case 12:
                      return Z.regs.W[ 15];
                  case 13:
                      return Z.regs.W[ 14];
                  case 14:
                      return Z.regs.W[ 13];
                  case 15:
                      return Z.regs.W[ 12];
                  default:
                      return 0;
              }
	        
	    };
	
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
	
	public int RDOP()
	{
            /*TODO*///int res = cpu_readop16(Z.pc);
            int res = cpu_readop(Z.pc);
	    Z.pc += 2;
	    return res;
	}
	
/*TODO*///	INLINE UINT8 RDMEM_B(UINT16 addr)
/*TODO*///	{
/*TODO*///		return cpu_readmem16bew(addr);
/*TODO*///	}
	
	public int RDMEM_W(int addr)
	{
		addr &= ~1;
		/*TODO*///return cpu_readmem16bew_word(addr);
                return cpu_readmem16(addr);
	}
	
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
	
	
	public void set_irq(int type)
	{
	    switch ((type >> 8) & 255)
	    {
	        case Z8000_TRAP >> 8:
	            if (Z.irq_srv >= Z8000_TRAP)
	                return; /* double TRAP.. very bad :( */
	            Z.irq_req = type;
	            break;
	        case Z8000_NMI >> 8:
	            if (Z.irq_srv >= Z8000_NMI)
	                return; /* no NMIs inside trap */
	            Z.irq_req = type;
	            break;
	        case Z8000_SEGTRAP >> 8:
	            if (Z.irq_srv >= Z8000_SEGTRAP)
	                return; /* no SEGTRAPs inside NMI/TRAP */
	            Z.irq_req = type;
	            break;
	        case Z8000_NVI >> 8:
	            if (Z.irq_srv >= Z8000_NVI)
	                return; /* no NVIs inside SEGTRAP/NMI/TRAP */
	            Z.irq_req = type;
	            break;
	        case Z8000_VI >> 8:
	            if (Z.irq_srv >= Z8000_VI)
	                return; /* no VIs inside NVI/SEGTRAP/NMI/TRAP */
	            Z.irq_req = type;
	            break;
	        case Z8000_SYSCALL >> 8:
/*TODO*///	            LOG(("Z8K#%d SYSCALL $%02x\n", cpu_getactivecpu(), type & 0xff));
	            Z.irq_req = type;
	            break;
	        default:
	            logerror("Z8000 invalid Cause_Interrupt %04x\n", type);
	            return;
	    }
	    /* set interrupt request flag, reset HALT flag */
	    Z.irq_req = type & ~Z8000_HALT;
	}
	
	
	public void Interrupt()
	{
	    int fcw = Z.fcw;
	
	    if ((Z.irq_req & Z8000_NVI) != 0)
	    {
	        int type = Z.irq_callback.handler(0);
	        set_irq(type);
	    }
	
	    if ((Z.irq_req & Z8000_VI) != 0)
	    {
	        int type = Z.irq_callback.handler(1);
	        set_irq(type);
	    }
	
	   /* trap ? */
	   if ((Z.irq_req & Z8000_TRAP ) != 0)
	   {
/*TODO*///	        CHANGE_FCW(fcw | F_S_N);/* swap to system stack */
/*TODO*///	        PUSHW( SP, PC );        /* save current PC */
/*TODO*///	        PUSHW( SP, fcw );       /* save current FCW */
/*TODO*///	        PUSHW( SP, IRQ_REQ );   /* save interrupt/trap type tag */
/*TODO*///	        IRQ_SRV = IRQ_REQ;
/*TODO*///	        IRQ_REQ &= ~Z8000_TRAP;
/*TODO*///	        PC = TRAP;
/*TODO*///	        LOG(("Z8K#%d trap $%04x\n", cpu_getactivecpu(), PC ));
	   }
	   else
	   if ((Z.irq_req & Z8000_SYSCALL ) != 0)
	   {
/*TODO*///	        CHANGE_FCW(fcw | F_S_N);/* swap to system stack */
/*TODO*///	        PUSHW( SP, PC );        /* save current PC */
/*TODO*///	        PUSHW( SP, fcw );       /* save current FCW */
/*TODO*///	        PUSHW( SP, IRQ_REQ );   /* save interrupt/trap type tag */
/*TODO*///	        IRQ_SRV = IRQ_REQ;
/*TODO*///	        IRQ_REQ &= ~Z8000_SYSCALL;
/*TODO*///	        PC = SYSCALL;
/*TODO*///	        LOG(("Z8K#%d syscall $%04x\n", cpu_getactivecpu(), PC ));
	   }
	   else
	   if ((Z.irq_req & Z8000_SEGTRAP ) != 0)
	   {
/*TODO*///	        CHANGE_FCW(fcw | F_S_N);/* swap to system stack */
/*TODO*///	        PUSHW( SP, PC );        /* save current PC */
/*TODO*///	        PUSHW( SP, fcw );       /* save current FCW */
/*TODO*///	        PUSHW( SP, IRQ_REQ );   /* save interrupt/trap type tag */
/*TODO*///	        IRQ_SRV = IRQ_REQ;
/*TODO*///	        IRQ_REQ &= ~Z8000_SEGTRAP;
/*TODO*///	        PC = SEGTRAP;
/*TODO*///	        LOG(("Z8K#%d segtrap $%04x\n", cpu_getactivecpu(), PC ));
	   }
	   else
	   if ((Z.irq_req & Z8000_NMI ) != 0)
	   {
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
	    }
	    else
	    if ( ((Z.irq_req & Z8000_NVI)!=0) && (Z.fcw & F_NVIE)!=0 )
	    {
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
	    }
	    else
	    if ( ((Z.irq_req & Z8000_VI)!=0) && (Z.fcw & F_VIE)!=0 )
	    {
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
	    }
	}
	
	
	public void z8000_reset(Object param)
	{
/*TODO*///		memset(&Z, 0, sizeof(z8000_Regs));
/*TODO*///		FCW = RDMEM_W( 2 ); /* get reset FCW */
/*TODO*///		PC	= RDMEM_W( 4 ); /* get reset PC  */
/*TODO*///		change_pc16bew(PC);
	}
	
	public void z8000_exit()
	{
		z8000_deinit();
	}
	
	public int z8000_execute(int cycles)
	{
	    z8000_ICount[0] = cycles;
	
	    do
	    {
	        /* any interrupt request pending? */
	        if (Z.irq_req != 0)
				Interrupt();
	
/*TODO*///			CALL_MAME_DEBUG;
	
		if ((Z.irq_req & Z8000_HALT) != 0)
	        {
	            z8000_ICount[0] = 0;
	        }
	        else
	        {
	            Z8000_exec exec;
	            Z.op[0] = RDOP();
	            exec = z8000_exec[Z.op[0]];
	
	            if (exec.size > 1)
	                Z.op[1] = RDOP();
	            if (exec.size > 2)
	                Z.op[2] = RDOP();
	
	            z8000_ICount[0] -= exec.cycles;
                    /*TODO*///exec.opcode.handler(Z.pc);

	        }
	    } while (z8000_ICount[0] > 0);
	
	    return cycles - z8000_ICount[0];
	
	}
	
/*TODO*///	public Object z8000_get_context(Object dst)
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
	
	public int z8000_get_reg(int regnum)
	{
		switch( regnum )
		{
			case REG_PC:
			case Z8000_PC: return Z.pc;
			case REG_SP:
                        case Z8000_NSP: return Z.nsp;
                        case Z8000_FCW: return Z.fcw;
			case Z8000_PSAP: return Z.psap;
			case Z8000_REFRESH: return Z.refresh;
			case Z8000_IRQ_REQ: return Z.irq_req;
			case Z8000_IRQ_SRV: return Z.irq_srv;
			case Z8000_IRQ_VEC: return Z.irq_vec;
			case Z8000_R0: return RW( 0);
			case Z8000_R1: return RW( 1);
			case Z8000_R2: return RW( 2);
			case Z8000_R3: return RW( 3);
			case Z8000_R4: return RW( 4);
			case Z8000_R5: return RW( 5);
			case Z8000_R6: return RW( 6);
			case Z8000_R7: return RW( 7);
			case Z8000_R8: return RW( 8);
			case Z8000_R9: return RW( 9);
			case Z8000_R10: return RW(10);
			case Z8000_R11: return RW(11);
			case Z8000_R12: return RW(12);
			case Z8000_R13: return RW(13);
			case Z8000_R14: return RW(14);
			case Z8000_R15: return RW(15);
			case Z8000_NMI_STATE: return Z.nmi_state;
			case Z8000_NVI_STATE: return Z.irq_state[0];
			case Z8000_VI_STATE: return Z.irq_state[1];
			case REG_PREVIOUSPC: return Z.ppc;
			default:
				if( regnum <= REG_SP_CONTENTS )
				{
					int offset = Z.nsp + 2 * (REG_SP_CONTENTS - regnum);
					if( offset < 0xffff )
						return RDMEM_W( offset );
				}
		}
	    return 0;
	}
	
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
