/*****************************************************************************
 *
 *	 i8085.c
 *	 Portable I8085A emulator V1.2
 *
 *	 Copyright (c) 1999 Juergen Buchmueller, all rights reserved.
 *	 Partially based on information out of Z80Em by Marcel De Kogel
 *
 * changes in V1.2
 *	 - corrected cycle counts for these classes of opcodes
 *	   Thanks go to Jim Battle <frustum@pacbell.bet>
 *
 *					808x	 Z80
 *	   DEC A		   5	   4	\
 *	   INC A		   5	   4	 \
 *	   LD A,B		   5	   4	  >-- Z80 is faster
 *	   JP (HL)		   5	   4	 /
 *	   CALL cc,nnnn: 11/17	 10/17	/
 *
 *	   INC HL		   5	   6	\
 *	   DEC HL		   5	   6	 \
 *	   LD SP,HL 	   5	   6	  \
 *	   ADD HL,BC	  10	  11	   \
 *	   INC (HL) 	  10	  11		>-- 8080 is faster
 *	   DEC (HL) 	  10	  11	   /
 *	   IN A,(#) 	  10	  11	  /
 *	   OUT (#),A	  10	  11	 /
 *	   EX (SP),HL	  18	  19	/
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
 *	   terms of its usage and license at any time, including retroactively
 *	 - This entire notice must remain in the source code.
 *
 *****************************************************************************/

//int survival_prot = 0;

/*TODO*///#define VERBOSE 0

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.cpu.i8085;

import static mame056.cpu.i8085.i8085H.*;
import static mame056.cpu.i8085.i8085cpuH.*;
import static mame056.cpu.i8085.i8085daaH.*;
import mame056.cpu.m6502.m6502.PAIR;
import static mame056.cpuintrfH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;

public class i8085 extends cpu_interface {
    
    public static int[] i8085_ICount = new int[1];
    
    public static int I8085_INTR = 0xff;
	
    /* Layout of the registers in the debugger */
    /*TODO*///static UINT8 i8085_reg_layout[] = {
    /*TODO*///        I8085_PC,I8085_SP,I8085_AF,I8085_BC,I8085_DE,I8085_HL, -1,
    /*TODO*///        I8085_HALT,I8085_IM,I8085_IREQ,I8085_ISRV,I8085_VECTOR, -1,
    /*TODO*///        I8085_TRAP_STATE,I8085_INTR_STATE,I8085_RST55_STATE,I8085_RST65_STATE,I8085_RST75_STATE,
    /*TODO*///        0 };

    /* Layout of the debugger windows x,y,w,h */
    /*TODO*///static UINT8 i8085_win_layout[] = {
    /*TODO*///        25, 0,55, 3,	/* register window (top, right rows) */
    /*TODO*///         0, 0,24,22,	/* disassembler window (left colums) */
    /*TODO*///        25, 4,55, 9,	/* memory #1 window (right, upper middle) */
    /*TODO*///        25,14,55, 8,	/* memory #2 window (right, lower middle) */
    /*TODO*///         0,23,80, 1,	/* command line window (bottom rows) */
    ;
    
    public static class I8085_Regs {
            public int 	cputype;	/* 0 8080, 1 8085A */
            public PAIR	PC=new PAIR(),SP=new PAIR(),AF=new PAIR(),BC=new PAIR(),DE=new PAIR(),HL=new PAIR(),XX=new PAIR();
            public int	HALT;
            public int	IM; 		/* interrupt mask */
            public int	IREQ;		/* requested interrupts */
            public int	ISRV;		/* serviced interrupt */
            public int	INTR;		/* vector for INTR */
            public int	IRQ2;		/* scheduled interrupt address */
            public int	IRQ1;		/* executed interrupt address */
            public int	nmi_state;
            public int[] irq_state = new int[4];
            public int	filler; /* align on dword boundary */
            public irqcallbacksPtr irq_callback;
            public irqcallbacksPtr sod_callback;
    };
    
    public static I8085_Regs I = new I8085_Regs();
    
    public i8085() {
        cpu_num = CPU_8085A;
        num_irqs = 4;
        default_vector = 255;
        icount = i8085_ICount;
        overclock = 1.00;
        irq_int = I8085_INTR_LINE;
        databus_width = 8;
        pgm_memory_base = 0;
        address_shift = 0;
        address_bits = 16;
        endianess = CPU_IS_LE;
        align_unit = 1;
        max_inst_len = 3;
    }

    @Override
    public void init() {
        i8085_init();
    }

    @Override
    public void reset(Object param) {
        i8085_reset(param);
    }

    @Override
    public void exit() {
        i8085_exit();
    }

    @Override
    public int execute(int cycles) {
        return i8085_execute(cycles);
    }

    @Override
    public Object init_context() {
        Object reg = new I8085_Regs();
        return reg;
    }

    @Override
    public Object get_context() {
        System.out.println("get_context");
        I8085_Regs Regs = new I8085_Regs();
        
        Regs.AF = I.AF;
        Regs.BC = I.BC;
        Regs.cputype = I.cputype;	/* 0 8080, 1 8085A */
        Regs.PC=I.PC;
        Regs.SP=I.SP;
        Regs.DE=I.DE;
        Regs.HL=I.HL;
        Regs.XX=I.XX;
        Regs.HALT=I.HALT;
        Regs.IM=I.IM; 		/* interrupt mask */
        Regs.IREQ=I.IREQ;		/* requested interrupts */
        Regs.ISRV=I.ISRV;		/* serviced interrupt */
        Regs.INTR=I.INTR;		/* vector for INTR */
        Regs.IRQ2=I.IRQ2;		/* scheduled interrupt address */
        Regs.IRQ1=I.IRQ2;		/* executed interrupt address */
        Regs.nmi_state=I.nmi_state;
        Regs.irq_state = I.irq_state;
        Regs.filler=I.filler; /* align on dword boundary */
        Regs.irq_callback=I.irq_callback;
        Regs.sod_callback=I.sod_callback;

        return Regs;
    }

    @Override
    public void set_context(Object reg) {
        i8085_set_context(reg);
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
        return i8085_get_reg(regnum);
    }

    @Override
    public void set_reg(int regnum, int val) {
        i8085_set_reg(regnum, val);
    }

    @Override
    public void set_irq_line(int irqline, int linestate) {
        i8085_set_irq_line(irqline, linestate);
    }

    @Override
    public void set_irq_callback(irqcallbacksPtr callback) {
        i8085_set_irq_callback(callback);
    }
    
    public void i8085_set_irq_callback(irqcallbacksPtr callback) {
        I.irq_callback = callback;
    }

    /****************************************************************************
     * Return a formatted string for a register
     ****************************************************************************/
    @Override
    public String cpu_info(Object context, int regnum) {
        return i8085_info(context, regnum);
    }

    public String i8085_info(Object context, int regnum){
        /*TODO*///static char buffer[16][47+1];
        /*TODO*///static int which = 0;
        /*TODO*///i8085_Regs *r = context;

        /*TODO*///which = (which+1) % 16;
        /*TODO*///buffer[which][0] = '\0';
        /*TODO*///if (context == 0)
        /*TODO*///        r = &I;

        switch( regnum )
        {
                /*TODO*///case CPU_INFO_REG+I8085_AF: sprintf(buffer[which], "AF:%04X", r->AF.L); break;
                /*TODO*///case CPU_INFO_REG+I8085_BC: sprintf(buffer[which], "BC:%04X", r->BC.L); break;
                /*TODO*///case CPU_INFO_REG+I8085_DE: sprintf(buffer[which], "DE:%04X", r->DE.L); break;
                /*TODO*///case CPU_INFO_REG+I8085_HL: sprintf(buffer[which], "HL:%04X", r->HL.L); break;
                /*TODO*///case CPU_INFO_REG+I8085_SP: sprintf(buffer[which], "SP:%04X", r->SP.L); break;
                /*TODO*///case CPU_INFO_REG+I8085_PC: sprintf(buffer[which], "PC:%04X", r->PC.L); break;
                /*TODO*///case CPU_INFO_REG+I8085_IM: sprintf(buffer[which], "IM:%02X", r->IM); break;
                /*TODO*///case CPU_INFO_REG+I8085_HALT: sprintf(buffer[which], "HALT:%d", r->HALT); break;
                /*TODO*///case CPU_INFO_REG+I8085_IREQ: sprintf(buffer[which], "IREQ:%02X", I.IREQ); break;
                /*TODO*///case CPU_INFO_REG+I8085_ISRV: sprintf(buffer[which], "ISRV:%02X", I.ISRV); break;
                /*TODO*///case CPU_INFO_REG+I8085_VECTOR: sprintf(buffer[which], "VEC:%02X", I.INTR); break;
                /*TODO*///case CPU_INFO_REG+I8085_TRAP_STATE: sprintf(buffer[which], "TRAP:%X", I.nmi_state); break;
                /*TODO*///case CPU_INFO_REG+I8085_INTR_STATE: sprintf(buffer[which], "INTR:%X", I.irq_state[I8085_INTR_LINE]); break;
                /*TODO*///case CPU_INFO_REG+I8085_RST55_STATE: sprintf(buffer[which], "RST55:%X", I.irq_state[I8085_RST55_LINE]); break;
                /*TODO*///case CPU_INFO_REG+I8085_RST65_STATE: sprintf(buffer[which], "RST65:%X", I.irq_state[I8085_RST65_LINE]); break;
                /*TODO*///case CPU_INFO_REG+I8085_RST75_STATE: sprintf(buffer[which], "RST75:%X", I.irq_state[I8085_RST75_LINE]); break;
                /*TODO*///case CPU_INFO_FLAGS:
                /*TODO*///        sprintf(buffer[which], "%c%c%c%c%c%c%c%c",
                /*TODO*///                r->AF.L & 0x80 ? 'S':'.',
                /*TODO*///                r->AF.L & 0x40 ? 'Z':'.',
                /*TODO*///                r->AF.L & 0x20 ? '?':'.',
                /*TODO*///                r->AF.L & 0x10 ? 'H':'.',
                /*TODO*///                r->AF.L & 0x08 ? '?':'.',
                /*TODO*///                r->AF.L & 0x04 ? 'P':'.',
                /*TODO*///                r->AF.L & 0x02 ? 'N':'.',
                /*TODO*///                r->AF.L & 0x01 ? 'C':'.');
                /*TODO*///        break;
                case CPU_INFO_NAME: return "8085A";
                case CPU_INFO_FAMILY: return "Intel 8080";
                case CPU_INFO_VERSION: return "1.1";
                case CPU_INFO_FILE: return "i8085.java";
                case CPU_INFO_CREDITS: return "Copyright (c) 1999 Juergen Buchmueller, all rights reserved.";
                /*TODO*///case CPU_INFO_REG_LAYOUT: return (const char *)i8085_reg_layout;
                /*TODO*///case CPU_INFO_WIN_LAYOUT: return (const char *)i8085_win_layout;
        }
        /*TODO*///return buffer[which];
        throw new UnsupportedOperationException("Not supported yet.");    
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
        return 0; //doesn't exist in 8085 cpu
    }

    @Override
    public void internal_write(int offset, int data) {
        //doesn't exist in 8085 cpu
    }

    @Override
    public void set_op_base(int pc) {
        cpu_setOPbase16.handler(pc);
    }

    @Override
    public int mem_address_bits_of_cpu() {
        return 16;
    }
    
    public static int[] ZS = new int[256];
    public static int[] ZSP= new int[256];

    static int ROP()
    {
            return cpu_readop(I.PC.L++);
    }
	
    static int ARG()
    {
            return cpu_readop_arg(I.PC.L++);
    }

    static int ARG16()
    {
            int w;
            w  = cpu_readop_arg(I.PC.D);
            I.PC.L++;
            w += cpu_readop_arg(I.PC.D) << 8;
            I.PC.L++;
            return w;
    }

    public static int RM(int a)
    {
            return cpu_readmem16(a) & 0xFF;
    }

    public static void WM(int a, int v)
    {
            cpu_writemem16(a, v & 0xFF);
    }
	
	static	void illegal()
	{
/*TODO*///	#if VERBOSE
		int pc = I.PC.L - 1;
                System.out.println("i8085 illegal instruction "+pc+" "+cpu_readop(pc));
/*TODO*///		LOG(("i8085 illegal instruction %04X $%02X\n", pc, cpu_readop(pc)));
/*TODO*///	#endif
	}
	
	public static void execute_one(int opcode)
	{
		switch (opcode)
		{
			case 0x00: i8085_ICount[0] -= 4;	/* NOP	*/
				/* no op */
				break;
			case 0x01: i8085_ICount[0] -= 10;	/* LXI	B,nnnn */
				I.BC.L = ARG16();
				break;
			case 0x02: i8085_ICount[0] -= 7;	/* STAX B */
				WM(I.BC.D, I.AF.H);
				break;
			case 0x03: i8085_ICount[0] -= 5;	/* INX	B */
				I.BC.L++;
				break;
			case 0x04: i8085_ICount[0] -= 5;	/* INR	B */
				M_INR(I.BC.H);
				break;
			case 0x05: i8085_ICount[0] -= 5;	/* DCR	B */
				M_DCR(I.BC.H);
				break;
			case 0x06: i8085_ICount[0] -= 7;	/* MVI	B,nn */
				M_MVI(I.BC.H);
				break;
			case 0x07: i8085_ICount[0] -= 4;	/* RLC	*/
				M_RLC();
				break;
	
			case 0x08: i8085_ICount[0] -= 4;	/* ???? */
				illegal();
				break;
			case 0x09: i8085_ICount[0] -= 10;	/* DAD	B */
				M_DAD(I.BC);
				break;
			case 0x0a: i8085_ICount[0] -= 7;	/* LDAX B */
				I.AF.H = RM(I.BC.D);
				break;
			case 0x0b: i8085_ICount[0] -= 5;	/* DCX	B */
				I.BC.L--;
				break;
			case 0x0c: i8085_ICount[0] -= 5;	/* INR	C */
				M_INR(I.BC.L);
				break;
			case 0x0d: i8085_ICount[0] -= 5;	/* DCR	C */
				M_DCR(I.BC.L);
				break;
			case 0x0e: i8085_ICount[0] -= 7;	/* MVI	C,nn */
				M_MVI(I.BC.L);
				break;
			case 0x0f: i8085_ICount[0] -= 4;	/* RRC	*/
				M_RRC();
				break;
	
			case 0x10: i8085_ICount[0] -= 8;	/* ????  */
				illegal();
				break;
			case 0x11: i8085_ICount[0] -= 10;	/* LXI	D,nnnn */
				I.DE.L = ARG16();
				break;
			case 0x12: i8085_ICount[0] -= 7;	/* STAX D */
				WM(I.DE.D, I.AF.H);
				break;
			case 0x13: i8085_ICount[0] -= 5;	/* INX	D */
				I.DE.L++;
				break;
			case 0x14: i8085_ICount[0] -= 5;	/* INR	D */
				M_INR(I.DE.H);
				break;
			case 0x15: i8085_ICount[0] -= 5;	/* DCR	D */
				M_DCR(I.DE.H);
				break;
			case 0x16: i8085_ICount[0] -= 7;	/* MVI	D,nn */
				M_MVI(I.DE.H);
				break;
			case 0x17: i8085_ICount[0] -= 4;	/* RAL	*/
				M_RAL();
				break;
	
			case 0x18: i8085_ICount[0] -= 7;	/* ????? */
				illegal();
				break;
			case 0x19: i8085_ICount[0] -= 10;	/* DAD	D */
				M_DAD(I.DE);
				break;
			case 0x1a: i8085_ICount[0] -= 7;	/* LDAX D */
				I.AF.H = RM(I.DE.D);
				break;
			case 0x1b: i8085_ICount[0] -= 5;	/* DCX	D */
				I.DE.L--;
				break;
			case 0x1c: i8085_ICount[0] -= 5;	/* INR	E */
				M_INR(I.DE.L);
				break;
			case 0x1d: i8085_ICount[0] -= 5;	/* DCR	E */
				M_DCR(I.DE.L);
				break;
			case 0x1e: i8085_ICount[0] -= 7;	/* MVI	E,nn */
				M_MVI(I.DE.L);
				break;
			case 0x1f: i8085_ICount[0] -= 4;	/* RAR	*/
				M_RAR();
				break;
	
			case 0x20:
				if( I.cputype != 0)
				{
					i8085_ICount[0] -= 7;		/* RIM	*/
					I.AF.H = I.IM;
	//				survival_prot ^= 0x01;
				}
				else
				{
					i8085_ICount[0] -= 7;		/* ???	*/
				}
				break;
			case 0x21: i8085_ICount[0] -= 10;	/* LXI	H,nnnn */
				I.HL.L = ARG16();
				break;
			case 0x22: i8085_ICount[0] -= 16;	/* SHLD nnnn */
				I.XX.L = ARG16();
				WM(I.XX.D, I.HL.L);
				I.XX.L++;
				WM(I.XX.D, I.HL.H);
				break;
			case 0x23: i8085_ICount[0] -= 5;	/* INX	H */
				I.HL.L++;
				break;
			case 0x24: i8085_ICount[0] -= 5;	/* INR	H */
				M_INR(I.HL.H);
				break;
			case 0x25: i8085_ICount[0] -= 5;	/* DCR	H */
				M_DCR(I.HL.H);
				break;
			case 0x26: i8085_ICount[0] -= 7;	/* MVI	H,nn */
				M_MVI(I.HL.H);
				break;
			case 0x27: i8085_ICount[0] -= 4;	/* DAA	*/
				I.XX.D = I.AF.H;
				if ((I.AF.L & CF)!=0) I.XX.D |= 0x100;
				if ((I.AF.L & HF)!=0) I.XX.D |= 0x200;
				if ((I.AF.L & NF)!=0) I.XX.D |= 0x400;
				I.AF.L = DAA[I.XX.D];
				break;
	
			case 0x28: i8085_ICount[0] -= 7;	/* ???? */
				illegal();
				break;
			case 0x29: i8085_ICount[0] -= 10;	/* DAD	H */
				M_DAD(I.HL);
				break;
			case 0x2a: i8085_ICount[0] -= 16;	/* LHLD nnnn */
				I.XX.D = ARG16();
				I.HL.L = RM(I.XX.D);
				I.XX.L++;
				I.HL.H = RM(I.XX.D);
				break;
			case 0x2b: i8085_ICount[0] -= 5;	/* DCX	H */
				I.HL.L--;
				break;
			case 0x2c: i8085_ICount[0] -= 5;	/* INR	L */
				M_INR(I.HL.L);
				break;
			case 0x2d: i8085_ICount[0] -= 5;	/* DCR	L */
				M_DCR(I.HL.L);
				break;
			case 0x2e: i8085_ICount[0] -= 7;	/* MVI	L,nn */
				M_MVI(I.HL.L);
				break;
			case 0x2f: i8085_ICount[0] -= 4;	/* CMA	*/
				I.AF.H ^= 0xff;
				I.AF.L |= HF + NF;
				break;
	
			case 0x30:
				if( I.cputype != 0 )
				{
					i8085_ICount[0] -= 7;		/* SIM	*/
					if (((I.IM ^ I.AF.H) & 0x80)!=0)
						if (I.sod_callback != null) I.sod_callback.handler(I.AF.H >> 7);
					I.IM &= (IM_SID + IM_IEN + IM_TRAP);
					I.IM |= (I.AF.H & ~(IM_SID + IM_SOD + IM_IEN + IM_TRAP));
					if ((I.AF.H & 0x80)!=0) I.IM |= IM_SOD;
				}
				else
				{
					i8085_ICount[0] -= 4;		/* ???	*/
				}
				break;
			case 0x31: i8085_ICount[0] -= 10;	/* LXI SP,nnnn */
				I.SP.L = ARG16();
				break;
			case 0x32: i8085_ICount[0] -= 13;	/* STAX nnnn */
				I.XX.D = ARG16();
				WM(I.XX.D, I.AF.H);
				break;
			case 0x33: i8085_ICount[0] -= 5;	/* INX	SP */
				I.SP.L++;
				break;
			case 0x34: i8085_ICount[0] -= 10;	/* INR	M */
				I.XX.L = RM(I.HL.D);
				M_INR(I.XX.L);
				WM(I.HL.D, I.XX.L);
				break;
			case 0x35: i8085_ICount[0] -= 10;	/* DCR	M */
				I.XX.L = RM(I.HL.D);
				M_DCR(I.XX.L);
				WM(I.HL.D, I.XX.L);
				break;
			case 0x36: i8085_ICount[0] -= 10;	/* MVI	M,nn */
				I.XX.L = ARG();
				WM(I.HL.D, I.XX.L);
				break;
			case 0x37: i8085_ICount[0] -= 4;	/* STC	*/
				I.AF.L = (I.AF.L & ~(HF + NF)) | CF;
				break;
	
			case 0x38: i8085_ICount[0] -= 7;	/* ???? */
				illegal();
				break;
			case 0x39: i8085_ICount[0] -= 10;	/* DAD SP */
				M_DAD(I.SP);
				break;
			case 0x3a: i8085_ICount[0] -= 13;	/* LDAX nnnn */
				I.XX.D = ARG16();
				I.AF.H = RM(I.XX.D);
				break;
			case 0x3b: i8085_ICount[0] -= 5;	/* DCX	SP */
				I.SP.L--;
				break;
			case 0x3c: i8085_ICount[0] -= 5;	/* INR	A */
				M_INR(I.AF.H);
				break;
			case 0x3d: i8085_ICount[0] -= 5;	/* DCR	A */
				M_DCR(I.AF.H);
				break;
			case 0x3e: i8085_ICount[0] -= 7;	/* MVI	A,nn */
				M_MVI(I.AF.H);
				break;
			case 0x3f: i8085_ICount[0] -= 4;	/* CMF	*/
				I.AF.L = ((I.AF.L & ~(HF + NF)) |
						   ((I.AF.L & CF) << 4)) ^ CF;
				break;
	
			case 0x40: i8085_ICount[0] -= 5;	/* MOV	B,B */
				/* no op */
				break;
			case 0x41: i8085_ICount[0] -= 5;	/* MOV	B,C */
				I.BC.H = I.BC.L;
				break;
			case 0x42: i8085_ICount[0] -= 5;	/* MOV	B,D */
				I.BC.H = I.DE.H;
				break;
			case 0x43: i8085_ICount[0] -= 5;	/* MOV	B,E */
				I.BC.H = I.DE.L;
				break;
			case 0x44: i8085_ICount[0] -= 5;	/* MOV	B,H */
				I.BC.H = I.HL.H;
				break;
			case 0x45: i8085_ICount[0] -= 5;	/* MOV	B,L */
				I.BC.H = I.HL.L;
				break;
			case 0x46: i8085_ICount[0] -= 7;	/* MOV	B,M */
				I.BC.H = RM(I.HL.D);
				break;
			case 0x47: i8085_ICount[0] -= 5;	/* MOV	B,A */
				I.BC.H = I.AF.H;
				break;
	
			case 0x48: i8085_ICount[0] -= 5;	/* MOV	C,B */
				I.BC.L = I.BC.H;
				break;
			case 0x49: i8085_ICount[0] -= 5;	/* MOV	C,C */
				/* no op */
				break;
			case 0x4a: i8085_ICount[0] -= 5;	/* MOV	C,D */
				I.BC.L = I.DE.H;
				break;
			case 0x4b: i8085_ICount[0] -= 5;	/* MOV	C,E */
				I.BC.L = I.DE.L;
				break;
			case 0x4c: i8085_ICount[0] -= 5;	/* MOV	C,H */
				I.BC.L = I.HL.H;
				break;
			case 0x4d: i8085_ICount[0] -= 5;	/* MOV	C,L */
				I.BC.L = I.HL.L;
				break;
			case 0x4e: i8085_ICount[0] -= 7;	/* MOV	C,M */
				I.BC.L = RM(I.HL.D);
				break;
			case 0x4f: i8085_ICount[0] -= 5;	/* MOV	C,A */
				I.BC.L = I.AF.H;
				break;
	
			case 0x50: i8085_ICount[0] -= 5;	/* MOV	D,B */
				I.DE.H = I.BC.H;
				break;
			case 0x51: i8085_ICount[0] -= 5;	/* MOV	D,C */
				I.DE.H = I.BC.L;
				break;
			case 0x52: i8085_ICount[0] -= 5;	/* MOV	D,D */
				/* no op */
				break;
			case 0x53: i8085_ICount[0] -= 5;	/* MOV	D,E */
				I.DE.H = I.DE.L;
				break;
			case 0x54: i8085_ICount[0] -= 5;	/* MOV	D,H */
				I.DE.H = I.HL.H;
				break;
			case 0x55: i8085_ICount[0] -= 5;	/* MOV	D,L */
				I.DE.H = I.HL.L;
				break;
			case 0x56: i8085_ICount[0] -= 7;	/* MOV	D,M */
				I.DE.H = RM(I.HL.D);
				break;
			case 0x57: i8085_ICount[0] -= 5;	/* MOV	D,A */
				I.DE.H = I.AF.H;
				break;
	
			case 0x58: i8085_ICount[0] -= 5;	/* MOV	E,B */
				I.DE.L = I.BC.H;
				break;
			case 0x59: i8085_ICount[0] -= 5;	/* MOV	E,C */
				I.DE.L = I.BC.L;
				break;
			case 0x5a: i8085_ICount[0] -= 5;	/* MOV	E,D */
				I.DE.L = I.DE.H;
				break;
			case 0x5b: i8085_ICount[0] -= 5;	/* MOV	E,E */
				/* no op */
				break;
			case 0x5c: i8085_ICount[0] -= 5;	/* MOV	E,H */
				I.DE.L = I.HL.H;
				break;
			case 0x5d: i8085_ICount[0] -= 5;	/* MOV	E,L */
				I.DE.L = I.HL.L;
				break;
			case 0x5e: i8085_ICount[0] -= 7;	/* MOV	E,M */
				I.DE.L = RM(I.HL.D);
				break;
			case 0x5f: i8085_ICount[0] -= 5;	/* MOV	E,A */
				I.DE.L = I.AF.H;
				break;
	
			case 0x60: i8085_ICount[0] -= 5;	/* MOV	H,B */
				I.HL.H = I.BC.H;
				break;
			case 0x61: i8085_ICount[0] -= 5;	/* MOV	H,C */
				I.HL.H = I.BC.L;
				break;
			case 0x62: i8085_ICount[0] -= 5;	/* MOV	H,D */
				I.HL.H = I.DE.H;
				break;
			case 0x63: i8085_ICount[0] -= 5;	/* MOV	H,E */
				I.HL.H = I.DE.L;
				break;
			case 0x64: i8085_ICount[0] -= 5;	/* MOV	H,H */
				/* no op */
				break;
			case 0x65: i8085_ICount[0] -= 5;	/* MOV	H,L */
				I.HL.H = I.HL.L;
				break;
			case 0x66: i8085_ICount[0] -= 7;	/* MOV	H,M */
				I.HL.H = RM(I.HL.D);
				break;
			case 0x67: i8085_ICount[0] -= 5;	/* MOV	H,A */
				I.HL.H = I.AF.H;
				break;
	
			case 0x68: i8085_ICount[0] -= 5;	/* MOV	L,B */
				I.HL.L = I.BC.H;
				break;
			case 0x69: i8085_ICount[0] -= 5;	/* MOV	L,C */
				I.HL.L = I.BC.L;
				break;
			case 0x6a: i8085_ICount[0] -= 5;	/* MOV	L,D */
				I.HL.L = I.DE.H;
				break;
			case 0x6b: i8085_ICount[0] -= 5;	/* MOV	L,E */
				I.HL.L = I.DE.L;
				break;
			case 0x6c: i8085_ICount[0] -= 5;	/* MOV	L,H */
				I.HL.L = I.HL.H;
				break;
			case 0x6d: i8085_ICount[0] -= 5;	/* MOV	L,L */
				/* no op */
				break;
			case 0x6e: i8085_ICount[0] -= 7;	/* MOV	L,M */
				I.HL.L = RM(I.HL.D);
				break;
			case 0x6f: i8085_ICount[0] -= 5;	/* MOV	L,A */
				I.HL.L = I.AF.H;
				break;
	
			case 0x70: i8085_ICount[0] -= 7;	/* MOV	M,B */
				WM(I.HL.D, I.BC.H);
				break;
			case 0x71: i8085_ICount[0] -= 7;	/* MOV	M,C */
				WM(I.HL.D, I.BC.L);
				break;
			case 0x72: i8085_ICount[0] -= 7;	/* MOV	M,D */
				WM(I.HL.D, I.DE.H);
				break;
			case 0x73: i8085_ICount[0] -= 7;	/* MOV	M,E */
				WM(I.HL.D, I.DE.L);
				break;
			case 0x74: i8085_ICount[0] -= 7;	/* MOV	M,H */
				WM(I.HL.D, I.HL.H);
				break;
			case 0x75: i8085_ICount[0] -= 7;	/* MOV	M,L */
				WM(I.HL.D, I.HL.L);
				break;
			case 0x76: i8085_ICount[0] -= 4;	/* HALT */
				I.PC.L--;
				I.HALT = 1;
				if (i8085_ICount[0] > 0) i8085_ICount[0] = 0;
				break;
			case 0x77: i8085_ICount[0] -= 7;	/* MOV	M,A */
				WM(I.HL.D, I.AF.H);
				break;
	
			case 0x78: i8085_ICount[0] -= 5;	/* MOV	A,B */
				I.AF.H = I.BC.H;
				break;
			case 0x79: i8085_ICount[0] -= 5;	/* MOV	A,C */
				I.AF.H = I.BC.L;
				break;
			case 0x7a: i8085_ICount[0] -= 5;	/* MOV	A,D */
				I.AF.H = I.DE.H;
				break;
			case 0x7b: i8085_ICount[0] -= 5;	/* MOV	A,E */
				I.AF.H = I.DE.L;
				break;
			case 0x7c: i8085_ICount[0] -= 5;	/* MOV	A,H */
				I.AF.H = I.HL.H;
				break;
			case 0x7d: i8085_ICount[0] -= 5;	/* MOV	A,L */
				I.AF.H = I.HL.L;
				break;
			case 0x7e: i8085_ICount[0] -= 7;	/* MOV	A,M */
				I.AF.H = RM(I.HL.D);
				break;
			case 0x7f: i8085_ICount[0] -= 5;	/* MOV	A,A */
				/* no op */
				break;
	
			case 0x80: i8085_ICount[0] -= 4;	/* ADD	B */
				M_ADD(I.BC.H);
				break;
			case 0x81: i8085_ICount[0] -= 4;	/* ADD	C */
				M_ADD(I.BC.L);
				break;
			case 0x82: i8085_ICount[0] -= 4;	/* ADD	D */
				M_ADD(I.DE.H);
				break;
			case 0x83: i8085_ICount[0] -= 4;	/* ADD	E */
				M_ADD(I.DE.L);
				break;
			case 0x84: i8085_ICount[0] -= 4;	/* ADD	H */
				M_ADD(I.HL.H);
				break;
			case 0x85: i8085_ICount[0] -= 4;	/* ADD	L */
				M_ADD(I.HL.L);
				break;
			case 0x86: i8085_ICount[0] -= 7;	/* ADD	M */
				M_ADD(RM(I.HL.D));
				break;
			case 0x87: i8085_ICount[0] -= 4;	/* ADD	A */
				M_ADD(I.AF.H);
				break;
	
			case 0x88: i8085_ICount[0] -= 4;	/* ADC	B */
				M_ADC(I.BC.H);
				break;
			case 0x89: i8085_ICount[0] -= 4;	/* ADC	C */
				M_ADC(I.BC.L);
				break;
			case 0x8a: i8085_ICount[0] -= 4;	/* ADC	D */
				M_ADC(I.DE.H);
				break;
			case 0x8b: i8085_ICount[0] -= 4;	/* ADC	E */
				M_ADC(I.DE.L);
				break;
			case 0x8c: i8085_ICount[0] -= 4;	/* ADC	H */
				M_ADC(I.HL.H);
				break;
			case 0x8d: i8085_ICount[0] -= 4;	/* ADC	L */
				M_ADC(I.HL.L);
				break;
			case 0x8e: i8085_ICount[0] -= 7;	/* ADC	M */
				M_ADC(RM(I.HL.D));
				break;
			case 0x8f: i8085_ICount[0] -= 4;	/* ADC	A */
				M_ADC(I.AF.H);
				break;
	
			case 0x90: i8085_ICount[0] -= 4;	/* SUB	B */
				M_SUB(I.BC.H);
				break;
			case 0x91: i8085_ICount[0] -= 4;	/* SUB	C */
				M_SUB(I.BC.L);
				break;
			case 0x92: i8085_ICount[0] -= 4;	/* SUB	D */
				M_SUB(I.DE.H);
				break;
			case 0x93: i8085_ICount[0] -= 4;	/* SUB	E */
				M_SUB(I.DE.L);
				break;
			case 0x94: i8085_ICount[0] -= 4;	/* SUB	H */
				M_SUB(I.HL.H);
				break;
			case 0x95: i8085_ICount[0] -= 4;	/* SUB	L */
				M_SUB(I.HL.L);
				break;
			case 0x96: i8085_ICount[0] -= 7;	/* SUB	M */
				M_SUB(RM(I.HL.D));
				break;
			case 0x97: i8085_ICount[0] -= 4;	/* SUB	A */
				M_SUB(I.AF.H);
				break;
	
			case 0x98: i8085_ICount[0] -= 4;	/* SBB	B */
				M_SBB(I.BC.H);
				break;
			case 0x99: i8085_ICount[0] -= 4;	/* SBB	C */
				M_SBB(I.BC.L);
				break;
			case 0x9a: i8085_ICount[0] -= 4;	/* SBB	D */
				M_SBB(I.DE.H);
				break;
			case 0x9b: i8085_ICount[0] -= 4;	/* SBB	E */
				M_SBB(I.DE.L);
				break;
			case 0x9c: i8085_ICount[0] -= 4;	/* SBB	H */
				M_SBB(I.HL.H);
				break;
			case 0x9d: i8085_ICount[0] -= 4;	/* SBB	L */
				M_SBB(I.HL.L);
				break;
			case 0x9e: i8085_ICount[0] -= 7;	/* SBB	M */
				M_SBB(RM(I.HL.D));
				break;
			case 0x9f: i8085_ICount[0] -= 4;	/* SBB	A */
				M_SBB(I.AF.H);
				break;
	
			case 0xa0: i8085_ICount[0] -= 4;	/* ANA	B */
				M_ANA(I.BC.H);
				break;
			case 0xa1: i8085_ICount[0] -= 4;	/* ANA	C */
				M_ANA(I.BC.L);
				break;
			case 0xa2: i8085_ICount[0] -= 4;	/* ANA	D */
				M_ANA(I.DE.H);
				break;
			case 0xa3: i8085_ICount[0] -= 4;	/* ANA	E */
				M_ANA(I.DE.L);
				break;
			case 0xa4: i8085_ICount[0] -= 4;	/* ANA	H */
				M_ANA(I.HL.H);
				break;
			case 0xa5: i8085_ICount[0] -= 4;	/* ANA	L */
				M_ANA(I.HL.L);
				break;
			case 0xa6: i8085_ICount[0] -= 7;	/* ANA	M */
				M_ANA(RM(I.HL.D));
				break;
			case 0xa7: i8085_ICount[0] -= 4;	/* ANA	A */
				M_ANA(I.AF.H);
				break;
	
			case 0xa8: i8085_ICount[0] -= 4;	/* XRA	B */
				M_XRA(I.BC.H);
				break;
			case 0xa9: i8085_ICount[0] -= 4;	/* XRA	C */
				M_XRA(I.BC.L);
				break;
			case 0xaa: i8085_ICount[0] -= 4;	/* XRA	D */
				M_XRA(I.DE.H);
				break;
			case 0xab: i8085_ICount[0] -= 4;	/* XRA	E */
				M_XRA(I.DE.L);
				break;
			case 0xac: i8085_ICount[0] -= 4;	/* XRA	H */
				M_XRA(I.HL.H);
				break;
			case 0xad: i8085_ICount[0] -= 4;	/* XRA	L */
				M_XRA(I.HL.L);
				break;
			case 0xae: i8085_ICount[0] -= 7;	/* XRA	M */
				M_XRA(RM(I.HL.D));
				break;
			case 0xaf: i8085_ICount[0] -= 4;	/* XRA	A */
				M_XRA(I.AF.H);
				break;
	
			case 0xb0: i8085_ICount[0] -= 4;	/* ORA	B */
				M_ORA(I.BC.H);
				break;
			case 0xb1: i8085_ICount[0] -= 4;	/* ORA	C */
				M_ORA(I.BC.L);
				break;
			case 0xb2: i8085_ICount[0] -= 4;	/* ORA	D */
				M_ORA(I.DE.H);
				break;
			case 0xb3: i8085_ICount[0] -= 4;	/* ORA	E */
				M_ORA(I.DE.L);
				break;
			case 0xb4: i8085_ICount[0] -= 4;	/* ORA	H */
				M_ORA(I.HL.H);
				break;
			case 0xb5: i8085_ICount[0] -= 4;	/* ORA	L */
				M_ORA(I.HL.L);
				break;
			case 0xb6: i8085_ICount[0] -= 7;	/* ORA	M */
				M_ORA(RM(I.HL.D));
				break;
			case 0xb7: i8085_ICount[0] -= 4;	/* ORA	A */
				M_ORA(I.AF.H);
				break;
	
			case 0xb8: i8085_ICount[0] -= 4;	/* CMP	B */
				M_CMP(I.BC.H);
				break;
			case 0xb9: i8085_ICount[0] -= 4;	/* CMP	C */
				M_CMP(I.BC.L);
				break;
			case 0xba: i8085_ICount[0] -= 4;	/* CMP	D */
				M_CMP(I.DE.H);
				break;
			case 0xbb: i8085_ICount[0] -= 4;	/* CMP	E */
				M_CMP(I.DE.L);
				break;
			case 0xbc: i8085_ICount[0] -= 4;	/* CMP	H */
				M_CMP(I.HL.H);
				break;
			case 0xbd: i8085_ICount[0] -= 4;	/* CMP	L */
				M_CMP(I.HL.L);
				break;
			case 0xbe: i8085_ICount[0] -= 7;	/* CMP	M */
				M_CMP(RM(I.HL.D));
				break;
			case 0xbf: i8085_ICount[0] -= 4;	/* CMP	A */
				M_CMP(I.AF.H);
				break;
	
			case 0xc0: i8085_ICount[0] -= 5;	/* RNZ	*/
				M_RET( (I.AF.L & ZF)!=0?0:1 );
				break;
			case 0xc1: i8085_ICount[0] -= 10;	/* POP	B */
				M_POP(I.BC);
				break;
			case 0xc2: i8085_ICount[0] -= 10;	/* JNZ	nnnn */
				M_JMP( (I.AF.L & ZF)!=0?0:1 );
				break;
			case 0xc3: i8085_ICount[0] -= 10;	/* JMP	nnnn */
				M_JMP(1);
				break;
			case 0xc4: i8085_ICount[0] -= 11;	/* CNZ	nnnn */
				M_CALL( (I.AF.L & ZF)!=0?0:1 );
				break;
			case 0xc5: i8085_ICount[0] -= 11;	/* PUSH B */
				M_PUSH(I.BC);
				break;
			case 0xc6: i8085_ICount[0] -= 7;	/* ADI	nn */
				I.XX.L = ARG();
				M_ADD(I.XX.L);
					break;
			case 0xc7: i8085_ICount[0] -= 11;	/* RST	0 */
				M_RST(0);
				break;
	
			case 0xc8: i8085_ICount[0] -= 5;	/* RZ	*/
				M_RET( I.AF.L & ZF );
				break;
			case 0xc9: i8085_ICount[0] -= 4;	/* RET	*/
				M_RET(1);
				break;
			case 0xca: i8085_ICount[0] -= 10;	/* JZ	nnnn */
				M_JMP( I.AF.L & ZF );
				break;
			case 0xcb: i8085_ICount[0] -= 4;	/* ???? */
				illegal();
				break;
			case 0xcc: i8085_ICount[0] -= 11;	/* CZ	nnnn */
				M_CALL( I.AF.L & ZF );
				break;
			case 0xcd: i8085_ICount[0] -= 11;	/* CALL nnnn */
				M_CALL(1);
				break;
			case 0xce: i8085_ICount[0] -= 7;	/* ACI	nn */
				I.XX.L = ARG();
				M_ADC(I.XX.L);
				break;
			case 0xcf: i8085_ICount[0] -= 11;	/* RST	1 */
				M_RST(1);
				break;
	
			case 0xd0: i8085_ICount[0] -= 5;	/* RNC	*/
				M_RET( (I.AF.L & CF)!=0?0:1 );
				break;
			case 0xd1: i8085_ICount[0] -= 10;	/* POP	D */
				M_POP(I.DE);
				break;
			case 0xd2: i8085_ICount[0] -= 10;	/* JNC	nnnn */
				M_JMP( (I.AF.L & CF)!=0?0:1 );
				break;
			case 0xd3: i8085_ICount[0] -= 10;	/* OUT	nn */
				M_OUT();
				break;
			case 0xd4: i8085_ICount[0] -= 11;	/* CNC	nnnn */
				M_CALL( (I.AF.L & CF)!=0?0:1 );
				break;
			case 0xd5: i8085_ICount[0] -= 11;	/* PUSH D */
				M_PUSH(I.DE);
				break;
			case 0xd6: i8085_ICount[0] -= 7;	/* SUI	nn */
				I.XX.L = ARG();
				M_SUB(I.XX.L);
				break;
			case 0xd7: i8085_ICount[0] -= 11;	/* RST	2 */
				M_RST(2);
				break;
	
			case 0xd8: i8085_ICount[0] -= 5;	/* RC	*/
				M_RET( I.AF.L & CF );
				break;
			case 0xd9: i8085_ICount[0] -= 4;	/* ???? */
				illegal();
				break;
			case 0xda: i8085_ICount[0] -= 10;	/* JC	nnnn */
				M_JMP( I.AF.L & CF );
				break;
			case 0xdb: i8085_ICount[0] -= 10;	/* IN	nn */
				M_IN();
				break;
			case 0xdc: i8085_ICount[0] -= 11;	/* CC	nnnn */
				M_CALL( I.AF.L & CF );
				break;
			case 0xdd: i8085_ICount[0] -= 4;	/* ???? */
				illegal();
				break;
			case 0xde: i8085_ICount[0] -= 7;	/* SBI	nn */
				I.XX.L = ARG();
				M_SBB(I.XX.L);
				break;
			case 0xdf: i8085_ICount[0] -= 11;	/* RST	3 */
				M_RST(3);
				break;
	
			case 0xe0: i8085_ICount[0] -= 5;	/* RPE	  */
				M_RET( (I.AF.L & VF)!=0?0:1 );
				break;
			case 0xe1: i8085_ICount[0] -= 10;	/* POP	H */
				M_POP(I.HL);
				break;
			case 0xe2: i8085_ICount[0] -= 10;	/* JPE	nnnn */
				M_JMP( (I.AF.L & VF)!=0?0:1 );
				break;
			case 0xe3: i8085_ICount[0] -= 18;	/* XTHL */
				M_POP(I.XX);
				M_PUSH(I.HL);
				I.HL.D = I.XX.D;
				break;
			case 0xe4: i8085_ICount[0] -= 11;	/* CPE	nnnn */
				M_CALL( (I.AF.L & VF)!=0?0:1 );
				break;
			case 0xe5: i8085_ICount[0] -= 11;	/* PUSH H */
				M_PUSH(I.HL);
				break;
			case 0xe6: i8085_ICount[0] -= 7;	/* ANI	nn */
				I.XX.L = ARG();
				M_ANA(I.XX.L);
				break;
			case 0xe7: i8085_ICount[0] -= 11;	/* RST	4 */
				M_RST(4);
				break;
	
			case 0xe8: i8085_ICount[0] -= 5;	/* RPO	*/
				M_RET( I.AF.L & VF );
				break;
			case 0xe9: i8085_ICount[0] -= 5;	/* PCHL */
				I.PC.D = I.HL.L;
				change_pc16(I.PC.D);
				break;
			case 0xea: i8085_ICount[0] -= 10;	/* JPO	nnnn */
				M_JMP( I.AF.L & VF );
				break;
			case 0xeb: i8085_ICount[0] -= 4;	/* XCHG */
				I.XX.D = I.DE.D;
				I.DE.D = I.HL.D;
				I.HL.D = I.XX.D;
				break;
			case 0xec: i8085_ICount[0] -= 11;	/* CPO	nnnn */
				M_CALL( I.AF.L & VF );
				break;
			case 0xed: i8085_ICount[0] -= 4;	/* ???? */
				illegal();
				break;
			case 0xee: i8085_ICount[0] -= 7;	/* XRI	nn */
				I.XX.L = ARG();
				M_XRA(I.XX.L);
				break;
			case 0xef: i8085_ICount[0] -= 11;	/* RST	5 */
				M_RST(5);
				break;
	
			case 0xf0: i8085_ICount[0] -= 5;	/* RP	*/
				M_RET( (I.AF.L&SF)!=0?0:1 );
				break;
			case 0xf1: i8085_ICount[0] -= 10;	/* POP	A */
				M_POP(I.AF);
				break;
			case 0xf2: i8085_ICount[0] -= 10;	/* JP	nnnn */
				M_JMP( (I.AF.L & SF)!=0?0:1 );
				break;
			case 0xf3: i8085_ICount[0] -= 4;	/* DI	*/
				/* remove interrupt enable */
				I.IM &= ~IM_IEN;
				break;
			case 0xf4: i8085_ICount[0] -= 11;	/* CP	nnnn */
				M_CALL( (I.AF.L & SF)!=0?0:1 );
				break;
			case 0xf5: i8085_ICount[0] -= 11;	/* PUSH A */
				M_PUSH(I.AF);
				break;
			case 0xf6: i8085_ICount[0] -= 7;	/* ORI	nn */
				I.XX.L = ARG();
				M_ORA(I.XX.L);
				break;
			case 0xf7: i8085_ICount[0] -= 11;	/* RST	6 */
				M_RST(6);
				break;
	
			case 0xf8: i8085_ICount[0] -= 5;	/* RM	*/
				M_RET( I.AF.L & SF );
				break;
			case 0xf9: i8085_ICount[0] -= 5;	/* SPHL */
				I.SP.D = I.HL.D;
				break;
			case 0xfa: i8085_ICount[0] -= 10;	/* JM	nnnn */
				M_JMP( I.AF.L & SF );
				break;
			case 0xfb: i8085_ICount[0] -= 4;	/* EI */
				/* set interrupt enable */
				I.IM |= IM_IEN;
				/* remove serviced IRQ flag */
				I.IREQ &= ~I.ISRV;
				/* reset serviced IRQ */
				I.ISRV = 0;
				if( I.irq_state[0] != CLEAR_LINE )
				{
/*TODO*///					LOG(("i8085 EI sets INTR\n"));
					I.IREQ |= IM_INTR;
					I.INTR = I8085_INTR;
				}
				if( I.cputype != 0 )
				{
					if( I.irq_state[1] != CLEAR_LINE )
					{
/*TODO*///						LOG(("i8085 EI sets RST5.5\n"));
						I.IREQ |= IM_RST55;
					}
					if( I.irq_state[2] != CLEAR_LINE )
					{
/*TODO*///						LOG(("i8085 EI sets RST6.5\n"));
						I.IREQ |= IM_RST65;
					}
					if( I.irq_state[3] != CLEAR_LINE )
					{
/*TODO*///						LOG(("i8085 EI sets RST7.5\n"));
						I.IREQ |= IM_RST75;
					}
					/* find highest priority IREQ flag with
					   IM enabled and schedule for execution */
					if( (I.IM & IM_RST75)==0 && (I.IREQ & IM_RST75)!=0 )
					{
						I.ISRV = IM_RST75;
						I.IRQ2 = ADDR_RST75;
					}
					else
					if( (I.IM & IM_RST65)==0 && (I.IREQ & IM_RST65)!=0 )
					{
						I.ISRV = IM_RST65;
						I.IRQ2 = ADDR_RST65;
					}
					else
					if( (I.IM & IM_RST55)==0 && (I.IREQ & IM_RST55)!=0 )
					{
						I.ISRV = IM_RST55;
						I.IRQ2 = ADDR_RST55;
					}
					else
					if( (I.IM & IM_INTR)==0 && (I.IREQ & IM_INTR)!=0 )
					{
						I.ISRV = IM_INTR;
						I.IRQ2 = I.INTR;
					}
				}
				else
				{
					if( (I.IM & IM_INTR)==0 && (I.IREQ & IM_INTR)!=0 )
					{
						I.ISRV = IM_INTR;
						I.IRQ2 = I.INTR;
					}
				}
				break;
			case 0xfc: i8085_ICount[0] -= 11;	/* CM	nnnn */
				M_CALL( I.AF.L & SF );
				break;
			case 0xfd: i8085_ICount[0] -= 4;	/* ???? */
				illegal();
				break;
			case 0xfe: i8085_ICount[0] -= 7;	/* CPI	nn */
				I.XX.L = ARG();
				M_CMP(I.XX.L);
				break;
			case 0xff: i8085_ICount[0] -= 11;	/* RST	7 */
				M_RST(7);
				break;
		}
	}
	
	static void Interrupt()
	{
	
		if( I.HALT != 0 )		/* if the CPU was halted */
		{
			I.PC.L++; 	/* skip HALT instr */
			I.HALT = 0;
		}
		I.IM &= ~IM_IEN;		/* remove general interrupt enable bit */
	
		if( I.ISRV == IM_INTR )
		{
/*TODO*///			LOG(("Interrupt get INTR vector\n"));
			I.IRQ1 = (I.irq_callback.handler(0));
		}
	
		if( I.cputype != 0 )
		{
			if( I.ISRV == IM_RST55 )
			{
/*TODO*///				LOG(("Interrupt get RST5.5 vector\n"));
				I.IRQ1 = I.irq_callback.handler(1);
			}
	
			if( I.ISRV == IM_RST65	)
			{
/*TODO*///				LOG(("Interrupt get RST6.5 vector\n"));
				I.IRQ1 = I.irq_callback.handler(2);
			}
	
			if( I.ISRV == IM_RST75 )
			{
/*TODO*///				LOG(("Interrupt get RST7.5 vector\n"));
				I.IRQ1 = I.irq_callback.handler(3);
			}
		}
	
		switch( I.IRQ1 & 0xff0000 )
		{
			case 0xcd0000:	/* CALL nnnn */
				i8085_ICount[0] -= 7;
				M_PUSH(I.PC);
			case 0xc30000:	/* JMP	nnnn */
				i8085_ICount[0] -= 10;
				I.PC.D = I.IRQ1 & 0xffff;
				change_pc16(I.PC.D);
				break;
			default:
				switch( I.ISRV )
				{
					case IM_TRAP:
					case IM_RST75:
					case IM_RST65:
					case IM_RST55:
						M_PUSH(I.PC);
						if (I.IRQ1 != (1 << I8085_RST75_LINE))
							I.PC.D = I.IRQ1;
						else
							I.PC.D = 0x3c;
						change_pc16(I.PC.D);
						break;
					default:
/*TODO*///						LOG(("i8085 take int $%02x\n", I.IRQ1));
						execute_one(I.IRQ1 & 0xff);
				}
		}
	}
	
	int i8085_execute(int cycles)
	{
	
		i8085_ICount[0] = cycles;
		do
		{
/*TODO*///			CALL_MAME_DEBUG;
			/* interrupts enabled or TRAP pending ? */
			if ( (I.IM & IM_IEN)!=0 || (I.IREQ & IM_TRAP)!=0 )
			{
				/* copy scheduled to executed interrupt request */
				I.IRQ1 = I.IRQ2;
				/* reset scheduled interrupt request */
				I.IRQ2 = 0;
				/* interrupt now ? */
				if (I.IRQ1 != 0) Interrupt();
			}
	
			/* here we go... */
			execute_one(ROP());
	
		} while (i8085_ICount[0] > 0);
	
		return cycles - i8085_ICount[0];
	}
	
	/****************************************************************************
	 * Initialise the various lookup tables used by the emulation code
	 ****************************************************************************/
	static void init_tables ()
	{
		int zs;
		int i, p;
		for (i = 0; i < 256; i++)
		{
			zs = 0;
			if (i==0) zs |= ZF;
			if ((i&128)!=0) zs |= SF;
			p = 0;
			if ((i&1)!=0) ++p;
			if ((i&2)!=0) ++p;
			if ((i&4)!=0) ++p;
			if ((i&8)!=0) ++p;
			if ((i&16)!=0) ++p;
			if ((i&32)!=0) ++p;
			if ((i&64)!=0) ++p;
			if ((i&128)!=0) ++p;
			ZS[i] = zs;
			ZSP[i] = zs | ((p&1)!=0 ? 0 : VF);
		}
	}
	
	/****************************************************************************
	 * Init the 8085 emulation
	 ****************************************************************************/
	void i8085_init()
	{
		int cpu = cpu_getactivecpu();
		init_tables();
		I.cputype = 1;
	
/*TODO*///		state_save_register_UINT16("i8085", cpu, "AF", &I.AF.L, 1);
/*TODO*///		state_save_register_UINT16("i8085", cpu, "BC", &I.BC.L, 1);
/*TODO*///		state_save_register_UINT16("i8085", cpu, "DE", &I.DE.L, 1);
/*TODO*///		state_save_register_UINT16("i8085", cpu, "HL", &I.HL.L, 1);
/*TODO*///		state_save_register_UINT16("i8085", cpu, "SP", &I.SP.L, 1);
/*TODO*///		state_save_register_UINT16("i8085", cpu, "PC", &I.PC.L, 1);
/*TODO*///		state_save_register_UINT8("i8085", cpu, "HALT", &I.HALT, 1);
/*TODO*///		state_save_register_UINT8("i8085", cpu, "IM", &I.IM, 1);
/*TODO*///		state_save_register_UINT8("i8085", cpu, "IREQ", &I.IREQ, 1);
/*TODO*///		state_save_register_UINT8("i8085", cpu, "ISRV", &I.ISRV, 1);
/*TODO*///		state_save_register_UINT32("i8085", cpu, "INTR", &I.INTR, 1);
/*TODO*///		state_save_register_UINT32("i8085", cpu, "IRQ2", &I.IRQ2, 1);
/*TODO*///		state_save_register_UINT32("i8085", cpu, "IRQ1", &I.IRQ1, 1);
/*TODO*///		state_save_register_INT8("i8085", cpu, "NMI_STATE", &I.nmi_state, 1);
/*TODO*///		state_save_register_INT8("i8085", cpu, "IRQ_STATE", I.irq_state, 4);
	}
	
	/****************************************************************************
	 * Reset the 8085 emulation
	 ****************************************************************************/
	public void i8085_reset(Object param)
	{
		init_tables();
		I = new I8085_Regs();
		change_pc16(I.PC.D);
	}
	
	/****************************************************************************
	 * Shut down the CPU emulation
	 ****************************************************************************/
	public void i8085_exit()
	{
		/* nothing to do */
	}
	
	/****************************************************************************
	 * Get the current 8085 context
	 ****************************************************************************/
	public static Object i8085_get_context(Object dst)
	{
		if( dst != null )
			dst = (I8085_Regs)I;
		return dst;
	}
	
	/****************************************************************************
	 * Set the current 8085 context
	 ****************************************************************************/
	public static void i8085_set_context(Object src)
	{
		if( src != null )
		{
			I = (I8085_Regs)src;
			change_pc16(I.PC.D);
		}
	}
	
	/****************************************************************************
	 * Get a specific register
	 ****************************************************************************/
	public int i8085_get_reg(int regnum)
	{
		switch( regnum )
		{
			case REG_PC: return I.PC.D;
			case I8085_PC: return I.PC.L;
			case REG_SP: return I.SP.D;
			case I8085_SP: return I.SP.L;
			case I8085_AF: return I.AF.L;
			case I8085_BC: return I.BC.L;
			case I8085_DE: return I.DE.L;
			case I8085_HL: return I.HL.L;
			case I8085_IM: return I.IM;
			case I8085_HALT: return I.HALT;
			case I8085_IREQ: return I.IREQ;
			case I8085_ISRV: return I.ISRV;
			case I8085_VECTOR: return I.INTR;
			case I8085_TRAP_STATE: return I.nmi_state;
			case I8085_INTR_STATE: return I.irq_state[I8085_INTR_LINE];
			case I8085_RST55_STATE: return I.irq_state[I8085_RST55_LINE];
			case I8085_RST65_STATE: return I.irq_state[I8085_RST65_LINE];
			case I8085_RST75_STATE: return I.irq_state[I8085_RST75_LINE];
			case REG_PREVIOUSPC: return 0; /* previous pc not supported */
			default:
				if( regnum <= REG_SP_CONTENTS )
				{
					int offset = I.SP.L + 2 * (REG_SP_CONTENTS - regnum);
					if( offset < 0xffff )
						return RM( offset ) + ( RM( offset+1 ) << 8 );
				}
		}
		return 0;
	}
	
	/****************************************************************************
	 * Set a specific register
	 ****************************************************************************/
	void i8085_set_reg(int regnum, int val)
	{
		switch( regnum )
		{
			case REG_PC: I.PC.L = val; change_pc16(I.PC.D); break;
			case I8085_PC: I.PC.L = val; break;
			case REG_SP: I.SP.L = val; break;
			case I8085_SP: I.SP.L = val; break;
			case I8085_AF: I.AF.L = val; break;
			case I8085_BC: I.BC.L = val; break;
			case I8085_DE: I.DE.L = val; break;
			case I8085_HL: I.HL.L = val; break;
			case I8085_IM: I.IM = val; break;
			case I8085_HALT: I.HALT = val; break;
			case I8085_IREQ: I.IREQ = val; break;
			case I8085_ISRV: I.ISRV = val; break;
			case I8085_VECTOR: I.INTR = val; break;
			case I8085_TRAP_STATE: I.nmi_state = val; break;
			case I8085_INTR_STATE: I.irq_state[I8085_INTR_LINE] = val; break;
			case I8085_RST55_STATE: I.irq_state[I8085_RST55_LINE] = val; break;
			case I8085_RST65_STATE: I.irq_state[I8085_RST65_LINE] = val; break;
			case I8085_RST75_STATE: I.irq_state[I8085_RST75_LINE] = val; break;
			default:
				if( regnum <= REG_SP_CONTENTS )
				{
					int offset = I.SP.L + 2 * (REG_SP_CONTENTS - regnum);
					if( offset < 0xffff )
					{
						WM( offset, val&0xff );
						WM( offset+1, (val>>8)&0xff );
					}
				}
		}
	}
	
/*TODO*///	/****************************************************************************/
/*TODO*///	/* Set the 8085 SID input signal state										*/
/*TODO*///	/****************************************************************************/
/*TODO*///	void i8085_set_SID(int state)
/*TODO*///	{
/*TODO*///		LOG(("i8085: SID %d\n", state));
/*TODO*///		if (state)
/*TODO*///			I.IM |= IM_SID;
/*TODO*///		else
/*TODO*///			I.IM &= ~IM_SID;
/*TODO*///	}
/*TODO*///	
/*TODO*///	/****************************************************************************/
/*TODO*///	/* Set a callback to be called at SOD output change 						*/
/*TODO*///	/****************************************************************************/
/*TODO*///	void i8085_set_sod_callback(void (*callback)(int state))
/*TODO*///	{
/*TODO*///		I.sod_callback = callback;
/*TODO*///	}
	
	/****************************************************************************/
	/* Set TRAP signal state													*/
	/****************************************************************************/
	void i8085_set_TRAP(int state)
	{
/*TODO*///		LOG(("i8085: TRAP %d\n", state));
		if (state != 0)
		{
			I.IREQ |= IM_TRAP;
			if(( I.ISRV & IM_TRAP )!=0) return;	/* already servicing TRAP ? */
			I.ISRV = IM_TRAP;				/* service TRAP */
			I.IRQ2 = ADDR_TRAP;
		}
		else
		{
			I.IREQ &= ~IM_TRAP; 			/* remove request for TRAP */
		}
	}
	
	/****************************************************************************/
	/* Set RST7.5 signal state													*/
	/****************************************************************************/
	void i8085_set_RST75(int state)
	{
/*TODO*///		LOG(("i8085: RST7.5 %d\n", state));
		if( state != 0 )
		{
	
			I.IREQ |= IM_RST75; 			/* request RST7.5 */
			if(( I.IM & IM_RST75 )!=0) return;	/* if masked, ignore it for now */
			if( I.ISRV == 0)					/* if no higher priority IREQ is serviced */
			{
				I.ISRV = IM_RST75;			/* service RST7.5 */
				I.IRQ2 = ADDR_RST75;
			}
		}
		/* RST7.5 is reset only by SIM or end of service routine ! */
	}
	
	/****************************************************************************/
	/* Set RST6.5 signal state													*/
	/****************************************************************************/
	void i8085_set_RST65(int state)
	{
/*TODO*///		LOG(("i8085: RST6.5 %d\n", state));
		if( state != 0 )
		{
			I.IREQ |= IM_RST65; 			/* request RST6.5 */
			if(( I.IM & IM_RST65 )!=0) return;	/* if masked, ignore it for now */
			if( I.ISRV == 0 )					/* if no higher priority IREQ is serviced */
			{
				I.ISRV = IM_RST65;			/* service RST6.5 */
				I.IRQ2 = ADDR_RST65;
			}
		}
		else
		{
			I.IREQ &= ~IM_RST65;			/* remove request for RST6.5 */
		}
	}
	
	/****************************************************************************/
	/* Set RST5.5 signal state													*/
	/****************************************************************************/
	void i8085_set_RST55(int state)
	{
/*TODO*///		LOG(("i8085: RST5.5 %d\n", state));
		if( state != 0 )
		{
			I.IREQ |= IM_RST55; 			/* request RST5.5 */
			if(( I.IM & IM_RST55 ) != 0) return;	/* if masked, ignore it for now */
			if( I.ISRV == 0 )					/* if no higher priority IREQ is serviced */
			{
				I.ISRV = IM_RST55;			/* service RST5.5 */
				I.IRQ2 = ADDR_RST55;
			}
		}
		else
		{
			I.IREQ &= ~IM_RST55;			/* remove request for RST5.5 */
		}
	}
	
	/****************************************************************************/
	/* Set INTR signal															*/
	/****************************************************************************/
	void i8085_set_INTR(int state)
	{
/*TODO*///		LOG(("i8085: INTR %d\n", state));
		if( state != 0 )
		{
			I.IREQ |= IM_INTR;				/* request INTR */
			I.INTR = state;
			if(( I.IM & IM_INTR ) != 0) return;	/* if masked, ignore it for now */
			if( I.ISRV == 0 )					/* if no higher priority IREQ is serviced */
			{
				I.ISRV = IM_INTR;			/* service INTR */
				I.IRQ2 = I.INTR;
			}
		}
		else
		{
			I.IREQ &= ~IM_INTR; 			/* remove request for INTR */
		}
	}
	
	public void i8085_set_irq_line(int irqline, int state)
	{
		if (irqline == IRQ_LINE_NMI)
		{
			I.nmi_state = state;
			if( state != CLEAR_LINE )
				i8085_set_TRAP(1);
		}
		else if (irqline < 4)
		{
			I.irq_state[irqline] = state;
			if (state == CLEAR_LINE)
			{
				if( (I.IM & IM_IEN)!=0 )
				{
					switch (irqline)
					{
						case I8085_INTR_LINE: i8085_set_INTR(0); break;
						case I8085_RST55_LINE: i8085_set_RST55(0); break;
						case I8085_RST65_LINE: i8085_set_RST65(0); break;
						case I8085_RST75_LINE: i8085_set_RST75(0); break;
					}
				}
			}
			else
			{
				if(( I.IM & IM_IEN ) != 0)
				{
					switch( irqline )
					{
						case I8085_INTR_LINE: i8085_set_INTR(1); break;
						case I8085_RST55_LINE: i8085_set_RST55(1); break;
						case I8085_RST65_LINE: i8085_set_RST65(1); break;
						case I8085_RST75_LINE: i8085_set_RST75(1); break;
					}
				}
			}
		}
	}
/*TODO*///	unsigned i8085_dasm(char *buffer, unsigned pc)
/*TODO*///	{
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///		return Dasm8085(buffer,pc);
/*TODO*///	#else
/*TODO*///		sprintf( buffer, "$%02X", cpu_readop(pc) );
/*TODO*///		return 1;
/*TODO*///	#endif
/*TODO*///	}

}
