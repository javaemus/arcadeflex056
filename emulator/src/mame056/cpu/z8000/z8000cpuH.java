/*****************************************************************************
 *
 *   z8000cpu.h
 *	 Portable Z8000(2) emulator
 *	 Macros and types used in z8000.c / z8000ops.c / z8000tbl.c
 *
 *	 Copyright (c) 1998,1999 Juergen Buchmueller, all rights reserved.
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

import arcadeflex056.fucPtr.ReadHandlerPtr;
import mame056.cpu.z80.z80.opcode;
import static mame056.cpu.z8000.z8000.pRW;


public class z8000cpuH {
/*TODO*////* pointers to the registers inside the Z8000_Regs struct Z */
/*TODO*///#define RB(n)   (*pRB[n])
    public static int RW(int n){
        return pRW(n);
    }
/*TODO*///#define RL(n)   (*pRL[n])
/*TODO*///#define RQ(n)   (*pRQ[n])
/*TODO*///
/*TODO*////* the register used as stack pointer */
/*TODO*///#define SP      15
/*TODO*///
/*TODO*////* programm status */
/*TODO*///#define PPC     Z.ppc
/*TODO*///#define PC		Z.pc
/*TODO*///#define PSAP    Z.psap
/*TODO*///#define FCW     Z.fcw
/*TODO*///#define REFRESH Z.refresh
/*TODO*///#define NSP 	Z.nsp
/*TODO*///#define IRQ_REQ Z.irq_req
/*TODO*///#define IRQ_SRV Z.irq_srv
/*TODO*///#define IRQ_VEC Z.irq_vec
/*TODO*///
/*TODO*////* these vectors are based on PSAP */
/*TODO*///#define RST 	(Z.psap + 0x0000)	/* start up FCW and PC */
/*TODO*///#define EPU 	(Z.psap + 0x0004)	/* extension processor unit? trap */
/*TODO*///#define TRAP	(Z.psap + 0x0008)	/* privilege violation trap */
/*TODO*///#define SYSCALL (Z.psap + 0x000c)	/* system call SC */
/*TODO*///#define SEGTRAP (Z.psap + 0x0010)	/* segment trap */
/*TODO*///#define NMI 	(Z.psap + 0x0014)	/* non maskable interrupt */
/*TODO*///#define NVI 	(Z.psap + 0x0018)	/* non vectored interrupt */
/*TODO*///#define VI		(Z.psap + 0x001c)	/* vectored interrupt */
/*TODO*///#define VEC00	(Z.psap + 0x001e)	/* vector n PC value */
/*TODO*///
/*TODO*////* bits of the FCW */
/*TODO*///#define F_SEG	0x8000				/* segmented mode (Z8001 only) */
/*TODO*///#define F_S_N	0x4000				/* system / normal mode */
/*TODO*///#define F_EPU	0x2000				/* extension processor unit? */
    public static final int F_NVIE	= 0x1000;				/* non vectored interrupt enable */
    public static final int F_VIE	= 0x0800;				/* vectored interrupt enable */
/*TODO*///#define F_10	0x0400				/* unused */
/*TODO*///#define F_9 	0x0200				/* unused */
/*TODO*///#define F_8 	0x0100				/* unused */
/*TODO*///#define F_C 	0x0080				/* carry flag */
    public static final int F_Z 	= 0x0040;				/* zero flag */
    public static final int F_S 	= 0x0020;				/* sign flag */
    public static final int F_PV	= 0x0010;				/* parity/overflow flag */
/*TODO*///#define F_DA	0x0008				/* decimal adjust flag (0 add/adc, 1 sub/sbc) */
/*TODO*///#define F_H 	0x0004				/* half carry flag (byte arithmetic only) */
/*TODO*///#define F_1 	0x0002				/* unused */
/*TODO*///#define F_0 	0x0001				/* unused */
/*TODO*///
/*TODO*////* opcode word numbers in Z.op[] array */
/*TODO*///#define OP0 	0
/*TODO*///#define OP1     1
/*TODO*///#define OP2     2
/*TODO*///
/*TODO*////* nibble shift factors for an opcode word */
/*TODO*////* left to right: 0x1340 -> NIB0=1, NIB1=3, NIB2=4, NIB3=0 */
/*TODO*///#define NIB0    12
/*TODO*///#define NIB1	8
/*TODO*///#define NIB2	4
/*TODO*///#define NIB3	0
/*TODO*///
/*TODO*////* sign bit masks for byte, word and long */
/*TODO*///#define S08 0x80
/*TODO*///#define S16 0x8000
/*TODO*///#define S32 0x80000000
/*TODO*///
/*TODO*////* get a single flag bit 0/1 */
/*TODO*///#define GET_C       ((FCW >> 7) & 1)
/*TODO*///#define GET_Z		((FCW >> 6) & 1)
/*TODO*///#define GET_S		((FCW >> 5) & 1)
/*TODO*///#define GET_PV		((FCW >> 4) & 1)
/*TODO*///#define GET_DA		((FCW >> 3) & 1)
/*TODO*///#define GET_H		((FCW >> 2) & 1)
/*TODO*///
/*TODO*////* clear a single flag bit */
/*TODO*///#define CLR_C       FCW &= ~F_C
/*TODO*///#define CLR_Z		FCW &= ~F_Z
/*TODO*///#define CLR_S		FCW &= ~F_S
/*TODO*///#define CLR_P		FCW &= ~F_PV
/*TODO*///#define CLR_V		FCW &= ~F_PV
/*TODO*///#define CLR_DA		FCW &= ~F_DA
/*TODO*///#define CLR_H		FCW &= ~F_H
/*TODO*///
/*TODO*////* clear a flag bit combination */
/*TODO*///#define CLR_CZS     FCW &= ~(F_C|F_Z|F_S)
/*TODO*///#define CLR_CZSP	FCW &= ~(F_C|F_Z|F_S|F_PV)
/*TODO*///#define CLR_CZSV	FCW &= ~(F_C|F_Z|F_S|F_PV)
/*TODO*///#define CLR_CZSVH	FCW &= ~(F_C|F_Z|F_S|F_PV|F_H)
/*TODO*///#define CLR_ZS		FCW &= ~(F_Z|F_S)
/*TODO*///#define CLR_ZSV 	FCW &= ~(F_Z|F_S|F_PV)
/*TODO*///#define CLR_ZSP 	FCW &= ~(F_Z|F_S|F_PV)
/*TODO*///
/*TODO*////* set a single flag bit */
/*TODO*///#define SET_C       FCW |= F_C
/*TODO*///#define SET_Z		FCW |= F_Z
/*TODO*///#define SET_S		FCW |= F_S
/*TODO*///#define SET_P		FCW |= F_PV
/*TODO*///#define SET_V		FCW |= F_PV
/*TODO*///#define SET_DA		FCW |= F_DA
/*TODO*///#define SET_H		FCW |= F_H
/*TODO*///
/*TODO*////* set a flag bit combination */
/*TODO*///#define SET_SC      FCW |= F_C | F_S
/*TODO*///
/*TODO*////* check condition codes */
/*TODO*///#define CC0 (0) 						/* always false */
/*TODO*///#define CC1 (GET_PV^GET_S)				/* less than */
/*TODO*///#define CC2 (GET_Z|(GET_PV^GET_S))		/* less than or equal */
/*TODO*///#define CC3 (GET_Z|GET_C)				/* unsigned less than or equal */
/*TODO*///#define CC4 GET_PV						/* parity even / overflow */
/*TODO*///#define CC5 GET_S						/* minus (signed) */
/*TODO*///#define CC6 GET_Z						/* zero / equal */
/*TODO*///#define CC7 GET_C						/* carry / unsigned less than */
/*TODO*///
/*TODO*///#define CC8 (1) 						/* always true */
/*TODO*///#define CC9 !(GET_PV^GET_S) 			/* greater than or equal */
/*TODO*///#define CCA !(GET_Z|(GET_PV^GET_S)) 	/* greater than */
/*TODO*///#define CCB !(GET_Z|GET_C)				/* unsigned greater than */
/*TODO*///#define CCC !GET_PV 					/* parity odd / no overflow */
/*TODO*///#define CCD !GET_S						/* plus (not signed) */
/*TODO*///#define CCE !GET_Z						/* not zero / not equal */
/*TODO*///#define CCF !GET_C						/* not carry / unsigned greater than */
/*TODO*///
/*TODO*////* get data from the opcode words */
/*TODO*////* o is the opcode word offset	  */
/*TODO*////* s is a nibble shift factor	  */
/*TODO*///#define GET_BIT(o)      UINT16 bit = 1 << (Z.op[o] & 15)
/*TODO*///#define GET_CCC(o,s)	UINT8 cc = (Z.op[o] >> (s)) & 15
/*TODO*///
/*TODO*///#define GET_DST(o,s)	UINT8 dst = (Z.op[o] >> (s)) & 15
/*TODO*///#define GET_SRC(o,s)	UINT8 src = (Z.op[o] >> (s)) & 15
/*TODO*///#define GET_IDX(o,s)	UINT8 idx = (Z.op[o] >> (s)) & 15
/*TODO*///#define GET_CNT(o,s)	INT8 cnt = (Z.op[o] >> (s)) & 15
/*TODO*///#define GET_IMM4(o,s)	UINT8 imm4 = (Z.op[o] >> (s)) & 15
/*TODO*///
/*TODO*///#define GET_I4M1(o,s)	UINT8 i4p1 = ((Z.op[o] >> (s)) & 15) + 1
/*TODO*///#define GET_IMM1(o,s)	UINT8 imm1 = (Z.op[o] >> (s)) & 2
/*TODO*///#define GET_IMM2(o,s)	UINT8 imm2 = (Z.op[o] >> (s)) & 3
/*TODO*///#define GET_IMM3(o,s)	UINT8 imm3 = (Z.op[o] >> (s)) & 7
/*TODO*///
/*TODO*///#define GET_IMM8(o) 	UINT8 imm8 = (UINT8)Z.op[o]
/*TODO*///
/*TODO*///#define GET_IMM16(o)	UINT16 imm16 = Z.op[o]
/*TODO*///#define GET_IMM32		UINT32 imm32 = Z.op[2] + (Z.op[1] << 16)
/*TODO*///#define GET_DSP7		UINT8 dsp7 = Z.op[0] & 127
/*TODO*///#define GET_DSP8		INT8 dsp8 = (INT8)Z.op[0]
/*TODO*///#define GET_DSP16		UINT16 dsp16 = PC + (INT16)Z.op[1]
/*TODO*///#define GET_ADDR(o) 	UINT16 addr = (UINT16)Z.op[o]

    /* structure for the opcode definition table */
    public static class Z8000_init {
        public int 	beg, end, step;
        public int 	size, cycles;
        public opcode   opcode;
        public String   dasm;
        
        public Z8000_init(int beg, int end, int step, int size, int cycles, opcode opcode, String dasm){
           this.beg = beg;
           this.end = end;
           this.step = step;
           this.size = size;
           this.cycles = cycles;
           this.opcode = opcode;
           this.dasm = dasm;
        }
    };

    /* structure for the opcode execution table / disassembler */
    public static class Z8000_exec {
        public opcode           opcode=null;
        public int              cycles=0;
    	public int              size=0;
        public String           dasm=null;
        
        public Z8000_exec(){}
    };

/*TODO*////* opcode execution table */
/*TODO*///extern Z8000_exec *z8000_exec;
    
}
