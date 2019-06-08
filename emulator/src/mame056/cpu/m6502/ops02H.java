/**
 * ported to v0.56
 */
package mame056.cpu.m6502;

import static mame056.memory.*;
import static mame056.cpu.m6502.m6502.*;
import static mame056.memoryH.*;

public class ops02H {

    /* 6502 flags */
    public static final int F_C = 0x01;
    public static final int F_Z = 0x02;
    public static final int F_I = 0x04;
    public static final int F_D = 0x08;
    public static final int F_B = 0x10;
    public static final int F_T = 0x20;
    public static final int F_V = 0x40;
    public static final int F_N = 0x80;

    /*TODO*///
/*TODO*////* some shortcuts for improved readability */
/*TODO*///#define A	m6502.a
/*TODO*///#define X	m6502.x
/*TODO*///#define Y	m6502.y
/*TODO*///#define P	m6502.p
/*TODO*///#define S	m6502.sp.b.l
/*TODO*///#define SPD m6502.sp.d
/*TODO*///
/*TODO*///#define NZ	m6502.nz
/*TODO*///
/*TODO*///#define SET_NZ(n)				\
/*TODO*///	if ((n) == 0) P = (P & ~F_N) | F_Z; else P = (P & ~(F_N | F_Z)) | ((n) & F_N)
/*TODO*///
/*TODO*///#define SET_Z(n)				\
/*TODO*///	if ((n) == 0) P |= F_Z; else P &= ~F_Z
/*TODO*///
/*TODO*///#define EAL m6502.ea.b.l
/*TODO*///#define EAH m6502.ea.b.h
/*TODO*///#define EAW m6502.ea.w.l
/*TODO*///#define EAD m6502.ea.d
/*TODO*///
/*TODO*///#define ZPL m6502.zp.b.l
/*TODO*///#define ZPH m6502.zp.b.h
/*TODO*///#define ZPW m6502.zp.w.l
/*TODO*///#define ZPD m6502.zp.d
/*TODO*///
/*TODO*///#define PCL m6502.pc.b.l
/*TODO*///#define PCH m6502.pc.b.h
/*TODO*///#define PCW m6502.pc.w.l
/*TODO*///#define PCD m6502.pc.d
/*TODO*///
/*TODO*///#define PPC m6502.ppc.d
/*TODO*///
/*TODO*///#if FAST_MEMORY
/*TODO*///extern	MHELE	*cur_mwhard;
/*TODO*///extern	MHELE	*cur_mrhard;
/*TODO*///extern	UINT8	*RAM;
/*TODO*///#endif
/*TODO*///
/*TODO*///#define CHANGE_PC change_pc16(PCD)
    /**
     * *************************************************************
     * RDOP	read an opcode
     * *************************************************************
     */
    public static int RDOP() {
        int r = cpu_readop(m6502.pc);
        m6502.pc = (m6502.pc + 1) & 0xFFFF;
        return r;
    }

    /*TODO*////***************************************************************
/*TODO*/// *	RDOPARG read an opcode argument
/*TODO*/// ***************************************************************/
/*TODO*///#define RDOPARG() cpu_readop_arg(PCW++)
/*TODO*///
    /**
     * *************************************************************
     * RDMEM	read memory
     * *************************************************************
     */
    public static int RDMEM(int addr) {
        return cpu_readmem16(addr) & 0xFF;
    }

    /**
     * *************************************************************
     * WRMEM	write memory
     * *************************************************************
     */
    public static void WRMEM(int addr, int data) {
        cpu_writemem16(addr, data & 0xFF);
    }

    /*TODO*///
/*TODO*////***************************************************************
/*TODO*/// *	BRA  branch relative
/*TODO*/// *	extra cycle if page boundary is crossed
/*TODO*/// ***************************************************************/
/*TODO*///#define BRA(cond)												\
/*TODO*///	if (cond)													\
/*TODO*///	{															\
/*TODO*///		tmp = RDOPARG();										\
/*TODO*///		EAW = PCW + (signed char)tmp;							\
/*TODO*///		m6502_ICount -= (PCH == EAH) ? 3 : 4;					\
/*TODO*///		PCD = EAD;												\
/*TODO*///		CHANGE_PC;												\
/*TODO*///	}															\
/*TODO*///	else														\
/*TODO*///	{															\
/*TODO*///		PCW++;													\
/*TODO*///		m6502_ICount -= 2;										\
/*TODO*///	}
/*TODO*///
/*TODO*////***************************************************************
/*TODO*/// *
/*TODO*/// * Helper macros to build the effective address
/*TODO*/// *
/*TODO*/// ***************************************************************/
/*TODO*///
/*TODO*////***************************************************************
/*TODO*/// *	EA = zero page address
/*TODO*/// ***************************************************************/
/*TODO*///#define EA_ZPG													\
/*TODO*///	ZPL = RDOPARG();											\
/*TODO*///	EAD = ZPD
/*TODO*///
/*TODO*////***************************************************************
/*TODO*/// *	EA = zero page address + X
/*TODO*/// ***************************************************************/
/*TODO*///#define EA_ZPX													\
/*TODO*///	ZPL = RDOPARG() + X;										\
/*TODO*///	EAD = ZPD
/*TODO*///
/*TODO*////***************************************************************
/*TODO*/// *	EA = zero page address + Y
/*TODO*/// ***************************************************************/
/*TODO*///#define EA_ZPY													\
/*TODO*///	ZPL = RDOPARG() + Y;										\
/*TODO*///	EAD = ZPD
/*TODO*///
/*TODO*////***************************************************************
/*TODO*/// *	EA = absolute address
/*TODO*/// ***************************************************************/
/*TODO*///#define EA_ABS													\
/*TODO*///	EAL = RDOPARG();											\
/*TODO*///	EAH = RDOPARG()
/*TODO*///
/*TODO*////***************************************************************
/*TODO*/// *	EA = absolute address + X
/*TODO*/// ***************************************************************/
/*TODO*///#define EA_ABX													\
/*TODO*///	EA_ABS; 													\
/*TODO*///	EAW += X
/*TODO*///
/*TODO*////***************************************************************
/*TODO*/// *	EA = absolute address + Y
/*TODO*/// ***************************************************************/
/*TODO*///#define EA_ABY													\
/*TODO*///	EA_ABS; 													\
/*TODO*///	EAW += Y
/*TODO*///
/*TODO*////***************************************************************
/*TODO*/// *	EA = zero page + X indirect (pre indexed)
/*TODO*/// ***************************************************************/
/*TODO*///#define EA_IDX													\
/*TODO*///	ZPL = RDOPARG() + X;										\
/*TODO*///	EAL = RDMEM(ZPD);											\
/*TODO*///	ZPL++;														\
/*TODO*///	EAH = RDMEM(ZPD)
/*TODO*///
/*TODO*////***************************************************************
/*TODO*/// *	EA = zero page indirect + Y (post indexed)
/*TODO*/// *	subtract 1 cycle if page boundary is crossed
/*TODO*/// ***************************************************************/
/*TODO*///#define EA_IDY													\
/*TODO*///	ZPL = RDOPARG();											\
/*TODO*///	EAL = RDMEM(ZPD);											\
/*TODO*///	ZPL++;														\
/*TODO*///	EAH = RDMEM(ZPD);											\
/*TODO*///	if (EAL + Y > 0xff) 										\
/*TODO*///		m6502_ICount--; 										\
/*TODO*///	EAW += Y
/*TODO*///
/*TODO*////***************************************************************
/*TODO*/// *	EA = indirect (only used by JMP)
/*TODO*/// ***************************************************************/
/*TODO*///#define EA_IND													\
/*TODO*///	EA_ABS; 													\
/*TODO*///	tmp = RDMEM(EAD);											\
/*TODO*///	EAL++;	/* booby trap: stay in same page! ;-) */			\
/*TODO*///	EAH = RDMEM(EAD);											\
/*TODO*///	EAL = tmp
/*TODO*///
/*TODO*////* read a value into tmp */
/*TODO*///#define RD_IMM	tmp = RDOPARG()
/*TODO*///#define RD_ACC	tmp = A
/*TODO*///#define RD_ZPG	EA_ZPG; tmp = RDMEM(EAD)
/*TODO*///#define RD_ZPX	EA_ZPX; tmp = RDMEM(EAD)
/*TODO*///#define RD_ZPY	EA_ZPY; tmp = RDMEM(EAD)
/*TODO*///#define RD_ABS	EA_ABS; tmp = RDMEM(EAD)
/*TODO*///#define RD_ABX	EA_ABX; tmp = RDMEM(EAD)
/*TODO*///#define RD_ABY	EA_ABY; tmp = RDMEM(EAD)
/*TODO*///#define RD_ZPI	EA_ZPI; tmp = RDMEM(EAD)
/*TODO*///#define RD_IDX	EA_IDX; tmp = RDMEM(EAD)
/*TODO*///#define RD_IDY	EA_IDY; tmp = RDMEM(EAD)
/*TODO*///
/*TODO*////* write a value from tmp */
/*TODO*///#define WR_ZPG	EA_ZPG; WRMEM(EAD, tmp)
/*TODO*///#define WR_ZPX	EA_ZPX; WRMEM(EAD, tmp)
/*TODO*///#define WR_ZPY	EA_ZPY; WRMEM(EAD, tmp)
/*TODO*///#define WR_ABS	EA_ABS; WRMEM(EAD, tmp)
/*TODO*///#define WR_ABX	EA_ABX; WRMEM(EAD, tmp)
/*TODO*///#define WR_ABY	EA_ABY; WRMEM(EAD, tmp)
/*TODO*///#define WR_ZPI	EA_ZPI; WRMEM(EAD, tmp)
/*TODO*///#define WR_IDX	EA_IDX; WRMEM(EAD, tmp)
/*TODO*///#define WR_IDY	EA_IDY; WRMEM(EAD, tmp)
/*TODO*///
/*TODO*////* write back a value from tmp to the last EA */
/*TODO*///#define WB_ACC	A = (UINT8)tmp;
/*TODO*///#define WB_EA	WRMEM(EAD, tmp)
/*TODO*///
/*TODO*////***************************************************************
/*TODO*/// ***************************************************************
/*TODO*/// *			Macros to emulate the plain 6502 opcodes
/*TODO*/// ***************************************************************
/*TODO*/// ***************************************************************/
/*TODO*///
/*TODO*////***************************************************************
/*TODO*/// * push a register onto the stack
/*TODO*/// ***************************************************************/
/*TODO*///#define PUSH(Rg) WRMEM(SPD, Rg); S--
/*TODO*///
/*TODO*////***************************************************************
/*TODO*/// * pull a register from the stack
/*TODO*/// ***************************************************************/
/*TODO*///#define PULL(Rg) S++; Rg = RDMEM(SPD)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	ADC Add with carry
/*TODO*/// ***************************************************************/
/*TODO*///#define ADC 													\
/*TODO*///	if (P & F_D)												\
/*TODO*///	{															\
/*TODO*///	int c = (P & F_C);											\
/*TODO*///	int lo = (A & 0x0f) + (tmp & 0x0f) + c; 					\
/*TODO*///	int hi = (A & 0xf0) + (tmp & 0xf0); 						\
/*TODO*///		P &= ~(F_V | F_C|F_N|F_Z);								\
/*TODO*///		if (!((lo+hi)&0xff)) P|=F_Z;							\
/*TODO*///		if (lo > 0x09)											\
/*TODO*///		{														\
/*TODO*///			hi += 0x10; 										\
/*TODO*///			lo += 0x06; 										\
/*TODO*///		}														\
/*TODO*///		if (hi&0x80) P|=F_N;									\
/*TODO*///		if (~(A^tmp) & (A^hi) & F_N)							\
/*TODO*///			P |= F_V;											\
/*TODO*///		if (hi > 0x90)											\
/*TODO*///			hi += 0x60; 										\
/*TODO*///		if (hi & 0xff00)										\
/*TODO*///			P |= F_C;											\
/*TODO*///		A = (lo & 0x0f) + (hi & 0xf0);							\
/*TODO*///	}															\
/*TODO*///	else														\
/*TODO*///	{															\
/*TODO*///		int c = (P & F_C);										\
/*TODO*///		int sum = A + tmp + c;									\
/*TODO*///		P &= ~(F_V | F_C);										\
/*TODO*///		if (~(A^tmp) & (A^sum) & F_N)							\
/*TODO*///			P |= F_V;											\
/*TODO*///		if (sum & 0xff00)										\
/*TODO*///			P |= F_C;											\
/*TODO*///		A = (UINT8) sum;										\
/*TODO*///		SET_NZ(A); \
/*TODO*///	}
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	AND Logical and
/*TODO*/// ***************************************************************/
/*TODO*///#define AND 													\
/*TODO*///	A = (UINT8)(A & tmp);										\
/*TODO*///	SET_NZ(A)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	ASL Arithmetic shift left
/*TODO*/// ***************************************************************/
/*TODO*///#define ASL 													\
/*TODO*///	P = (P & ~F_C) | ((tmp >> 7) & F_C);						\
/*TODO*///	tmp = (UINT8)(tmp << 1);									\
/*TODO*///	SET_NZ(tmp)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	BCC Branch if carry clear
/*TODO*/// ***************************************************************/
/*TODO*///#define BCC BRA(!(P & F_C))
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	BCS Branch if carry set
/*TODO*/// ***************************************************************/
/*TODO*///#define BCS BRA(P & F_C)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	BEQ Branch if equal
/*TODO*/// ***************************************************************/
/*TODO*///#define BEQ BRA(P & F_Z)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	BIT Bit test
/*TODO*/// ***************************************************************/
/*TODO*///#define BIT 													\
/*TODO*///	P &= ~(F_N|F_V|F_Z);										\
/*TODO*///	P |= tmp & (F_N|F_V);										\
/*TODO*///	if ((tmp & A) == 0) 										\
/*TODO*///		P |= F_Z
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	BMI Branch if minus
/*TODO*/// ***************************************************************/
/*TODO*///#define BMI BRA(P & F_N)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	BNE Branch if not equal
/*TODO*/// ***************************************************************/
/*TODO*///#define BNE BRA(!(P & F_Z))
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	BPL Branch if plus
/*TODO*/// ***************************************************************/
/*TODO*///#define BPL BRA(!(P & F_N))
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	BRK Break
/*TODO*/// *	increment PC, push PC hi, PC lo, flags (with B bit set),
/*TODO*/// *	set I flag, jump via IRQ vector
/*TODO*/// ***************************************************************/
/*TODO*///#define BRK 													\
/*TODO*///	PCW++;														\
/*TODO*///	PUSH(PCH);													\
/*TODO*///	PUSH(PCL);													\
/*TODO*///	PUSH(P | F_B);												\
/*TODO*///	P = (P | F_I);												\
/*TODO*///	PCL = RDMEM(M6502_IRQ_VEC); 								\
/*TODO*///	PCH = RDMEM(M6502_IRQ_VEC+1);								\
/*TODO*///	CHANGE_PC
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * BVC	Branch if overflow clear
/*TODO*/// ***************************************************************/
/*TODO*///#define BVC BRA(!(P & F_V))
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * BVS	Branch if overflow set
/*TODO*/// ***************************************************************/
/*TODO*///#define BVS BRA(P & F_V)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * CLC	Clear carry flag
/*TODO*/// ***************************************************************/
/*TODO*///#define CLC 													\
/*TODO*///	P &= ~F_C
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * CLD	Clear decimal flag
/*TODO*/// ***************************************************************/
/*TODO*///#define CLD 													\
/*TODO*///	P &= ~F_D
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * CLI	Clear interrupt flag
/*TODO*/// ***************************************************************/
/*TODO*///#define CLI 													\
/*TODO*///	if ((m6502.irq_state != CLEAR_LINE) && (P & F_I)) { 		\
/*TODO*///		m6502.after_cli = 1;									\
/*TODO*///	}															\
/*TODO*///	P &= ~F_I
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * CLV	Clear overflow flag
/*TODO*/// ***************************************************************/
/*TODO*///#define CLV 													\
/*TODO*///	P &= ~F_V
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	CMP Compare accumulator
/*TODO*/// ***************************************************************/
/*TODO*///#define CMP 													\
/*TODO*///	P &= ~F_C;													\
/*TODO*///	if (A >= tmp)												\
/*TODO*///		P |= F_C;												\
/*TODO*///	SET_NZ((UINT8)(A - tmp))
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	CPX Compare index X
/*TODO*/// ***************************************************************/
/*TODO*///#define CPX 													\
/*TODO*///	P &= ~F_C;													\
/*TODO*///	if (X >= tmp)												\
/*TODO*///		P |= F_C;												\
/*TODO*///	SET_NZ((UINT8)(X - tmp))
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	CPY Compare index Y
/*TODO*/// ***************************************************************/
/*TODO*///#define CPY 													\
/*TODO*///	P &= ~F_C;													\
/*TODO*///	if (Y >= tmp)												\
/*TODO*///		P |= F_C;												\
/*TODO*///	SET_NZ((UINT8)(Y - tmp))
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	DEC Decrement memory
/*TODO*/// ***************************************************************/
/*TODO*///#define DEC 													\
/*TODO*///	tmp = (UINT8)(tmp-1); 										\
/*TODO*///	SET_NZ(tmp)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	DEX Decrement index X
/*TODO*/// ***************************************************************/
/*TODO*///#define DEX 													\
/*TODO*///	X = (UINT8)(X-1); 											\
/*TODO*///	SET_NZ(X)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	DEY Decrement index Y
/*TODO*/// ***************************************************************/
/*TODO*///#define DEY 													\
/*TODO*///	Y = (UINT8)(Y-1); 											\
/*TODO*///	SET_NZ(Y)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	EOR Logical exclusive or
/*TODO*/// ***************************************************************/
/*TODO*///#define EOR 													\
/*TODO*///	A = (UINT8)(A ^ tmp);										\
/*TODO*///	SET_NZ(A)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	ILL Illegal opcode
/*TODO*/// ***************************************************************/
/*TODO*///#define ILL 													\
/*TODO*///	logerror("M6502 illegal opcode %04x: %02x\n",(PCW-1)&0xffff, cpu_readop((PCW-1)&0xffff))
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	INC Increment memory
/*TODO*/// ***************************************************************/
/*TODO*///#define INC 													\
/*TODO*///	tmp = (UINT8)(tmp+1); 										\
/*TODO*///	SET_NZ(tmp)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	INX Increment index X
/*TODO*/// ***************************************************************/
/*TODO*///#define INX 													\
/*TODO*///	X = (UINT8)(X+1); 											\
/*TODO*///	SET_NZ(X)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	INY Increment index Y
/*TODO*/// ***************************************************************/
/*TODO*///#define INY 													\
/*TODO*///	Y = (UINT8)(Y+1); 											\
/*TODO*///	SET_NZ(Y)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	JMP Jump to address
/*TODO*/// *	set PC to the effective address
/*TODO*/// ***************************************************************/
/*TODO*///#define JMP 													\
/*TODO*///	if( EAD == PPC && !m6502.pending_irq && !m6502.after_cli )	\
/*TODO*///		if( m6502_ICount > 0 ) m6502_ICount = 0;				\
/*TODO*///	PCD = EAD;													\
/*TODO*///	CHANGE_PC
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	JSR Jump to subroutine
/*TODO*/// *	decrement PC (sic!) push PC hi, push PC lo and set
/*TODO*/// *	PC to the effective address
/*TODO*/// ***************************************************************/
/*TODO*///#define JSR 													\
/*TODO*///	EAL = RDOPARG();											\
/*TODO*///	PUSH(PCH);													\
/*TODO*///	PUSH(PCL);													\
/*TODO*///	EAH = RDOPARG();											\
/*TODO*///	PCD = EAD;													\
/*TODO*///	CHANGE_PC
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	LDA Load accumulator
/*TODO*/// ***************************************************************/
/*TODO*///#define LDA 													\
/*TODO*///	A = (UINT8)tmp; 											\
/*TODO*///	SET_NZ(A)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	LDX Load index X
/*TODO*/// ***************************************************************/
/*TODO*///#define LDX 													\
/*TODO*///	X = (UINT8)tmp; 											\
/*TODO*///	SET_NZ(X)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	LDY Load index Y
/*TODO*/// ***************************************************************/
/*TODO*///#define LDY 													\
/*TODO*///	Y = (UINT8)tmp; 											\
/*TODO*///	SET_NZ(Y)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	LSR Logic shift right
/*TODO*/// *	0 -> [7][6][5][4][3][2][1][0] -> C
/*TODO*/// ***************************************************************/
/*TODO*///#define LSR 													\
/*TODO*///	P = (P & ~F_C) | (tmp & F_C);								\
/*TODO*///	tmp = (UINT8)tmp >> 1;										\
/*TODO*///	SET_NZ(tmp)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	NOP No operation
/*TODO*/// ***************************************************************/
/*TODO*///#define NOP
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	ORA Logical inclusive or
/*TODO*/// ***************************************************************/
/*TODO*///#define ORA 													\
/*TODO*///	A = (UINT8)(A | tmp);										\
/*TODO*///	SET_NZ(A)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	PHA Push accumulator
/*TODO*/// ***************************************************************/
/*TODO*///#define PHA 													\
/*TODO*///	PUSH(A)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	PHP Push processor status (flags)
/*TODO*/// ***************************************************************/
/*TODO*///#define PHP 													\
/*TODO*///	PUSH(P)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	PLA Pull accumulator
/*TODO*/// ***************************************************************/
/*TODO*///#define PLA 													\
/*TODO*///	PULL(A);													\
/*TODO*///	SET_NZ(A)
/*TODO*///
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	PLP Pull processor status (flags)
/*TODO*/// ***************************************************************/
/*TODO*///#define PLP 													\
/*TODO*///	if ( P & F_I ) {											\
/*TODO*///		PULL(P);												\
/*TODO*///		if ((m6502.irq_state != CLEAR_LINE) && !(P & F_I)) {	\
/*TODO*///			LOG(("M6502#%d PLP sets after_cli\n",cpu_getactivecpu())); \
/*TODO*///			m6502.after_cli = 1;								\
/*TODO*///		}														\
/*TODO*///	} else {													\
/*TODO*///		PULL(P);												\
/*TODO*///	}															\
/*TODO*///	P |= (F_T|F_B);
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * ROL	Rotate left
/*TODO*/// *	new C <- [7][6][5][4][3][2][1][0] <- C
/*TODO*/// ***************************************************************/
/*TODO*///#define ROL 													\
/*TODO*///	tmp = (tmp << 1) | (P & F_C);								\
/*TODO*///	P = (P & ~F_C) | ((tmp >> 8) & F_C);						\
/*TODO*///	tmp = (UINT8)tmp;											\
/*TODO*///	SET_NZ(tmp)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * ROR	Rotate right
/*TODO*/// *	C -> [7][6][5][4][3][2][1][0] -> new C
/*TODO*/// ***************************************************************/
/*TODO*///#define ROR 													\
/*TODO*///	tmp |= (P & F_C) << 8;										\
/*TODO*///	P = (P & ~F_C) | (tmp & F_C);								\
/*TODO*///	tmp = (UINT8)(tmp >> 1);									\
/*TODO*///	SET_NZ(tmp)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * RTI	Return from interrupt
/*TODO*/// * pull flags, pull PC lo, pull PC hi and increment PC
/*TODO*/// *	PCW++;
/*TODO*/// ***************************************************************/
/*TODO*///#define RTI 													\
/*TODO*///	PULL(P);													\
/*TODO*///	PULL(PCL);													\
/*TODO*///	PULL(PCH);													\
/*TODO*///	P |= F_T | F_B; 											\
/*TODO*///	if( (m6502.irq_state != CLEAR_LINE) && !(P & F_I) ) 		\
/*TODO*///	{															\
/*TODO*///		LOG(("M6502#%d RTI sets after_cli\n",cpu_getactivecpu())); \
/*TODO*///		m6502.after_cli = 1;									\
/*TODO*///	}															\
/*TODO*///	CHANGE_PC
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	RTS Return from subroutine
/*TODO*/// *	pull PC lo, PC hi and increment PC
/*TODO*/// ***************************************************************/
/*TODO*///#define RTS 													\
/*TODO*///	PULL(PCL);													\
/*TODO*///	PULL(PCH);													\
/*TODO*///	PCW++;														\
/*TODO*///	CHANGE_PC
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	SBC Subtract with carry
/*TODO*/// ***************************************************************/
/*TODO*///#define SBC 													\
/*TODO*///	if (P & F_D)												\
/*TODO*///	{															\
/*TODO*///		int c = (P & F_C) ^ F_C;								\
/*TODO*///		int sum = A - tmp - c;									\
/*TODO*///		int lo = (A & 0x0f) - (tmp & 0x0f) - c; 				\
/*TODO*///		int hi = (A & 0xf0) - (tmp & 0xf0); 					\
/*TODO*///		if (lo & 0x10)											\
/*TODO*///		{														\
/*TODO*///			lo -= 6;											\
/*TODO*///			hi--;												\
/*TODO*///		}														\
/*TODO*///		P &= ~(F_V | F_C|F_Z|F_N);								\
/*TODO*///		if( (A^tmp) & (A^sum) & F_N )							\
/*TODO*///			P |= F_V;											\
/*TODO*///		if( hi & 0x0100 )										\
/*TODO*///			hi -= 0x60; 										\
/*TODO*///		if( (sum & 0xff00) == 0 )								\
/*TODO*///			P |= F_C;											\
/*TODO*///		if( !((A-tmp-c) & 0xff) )								\
/*TODO*///			P |= F_Z;											\
/*TODO*///		if( (A-tmp-c) & 0x80 )									\
/*TODO*///			P |= F_N;											\
/*TODO*///		A = (lo & 0x0f) | (hi & 0xf0);							\
/*TODO*///	}															\
/*TODO*///	else														\
/*TODO*///	{															\
/*TODO*///		int c = (P & F_C) ^ F_C;								\
/*TODO*///		int sum = A - tmp - c;									\
/*TODO*///		P &= ~(F_V | F_C);										\
/*TODO*///		if( (A^tmp) & (A^sum) & F_N )							\
/*TODO*///			P |= F_V;											\
/*TODO*///		if( (sum & 0xff00) == 0 )								\
/*TODO*///			P |= F_C;											\
/*TODO*///		A = (UINT8) sum;										\
/*TODO*///		SET_NZ(A);												\
/*TODO*///	}
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	SEC Set carry flag
/*TODO*/// ***************************************************************/
/*TODO*///#define SEC 													\
/*TODO*///	P |= F_C
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	SED Set decimal flag
/*TODO*/// ***************************************************************/
/*TODO*///#define SED 													\
/*TODO*///	P |= F_D
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// *	SEI Set interrupt flag
/*TODO*/// ***************************************************************/
/*TODO*///#define SEI 													\
/*TODO*///	P |= F_I
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * STA	Store accumulator
/*TODO*/// ***************************************************************/
/*TODO*///#define STA 													\
/*TODO*///	tmp = A
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * STX	Store index X
/*TODO*/// ***************************************************************/
/*TODO*///#define STX 													\
/*TODO*///	tmp = X
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * STY	Store index Y
/*TODO*/// ***************************************************************/
/*TODO*///#define STY 													\
/*TODO*///	tmp = Y
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * TAX	Transfer accumulator to index X
/*TODO*/// ***************************************************************/
/*TODO*///#define TAX 													\
/*TODO*///	X = A;														\
/*TODO*///	SET_NZ(X)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * TAY	Transfer accumulator to index Y
/*TODO*/// ***************************************************************/
/*TODO*///#define TAY 													\
/*TODO*///	Y = A;														\
/*TODO*///	SET_NZ(Y)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * TSX	Transfer stack LSB to index X
/*TODO*/// ***************************************************************/
/*TODO*///#define TSX 													\
/*TODO*///	X = S;														\
/*TODO*///	SET_NZ(X)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * TXA	Transfer index X to accumulator
/*TODO*/// ***************************************************************/
/*TODO*///#define TXA 													\
/*TODO*///	A = X;														\
/*TODO*///	SET_NZ(A)
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * TXS	Transfer index X to stack LSB
/*TODO*/// * no flags changed (sic!)
/*TODO*/// ***************************************************************/
/*TODO*///#define TXS 													\
/*TODO*///	S = X
/*TODO*///
/*TODO*////* 6502 ********************************************************
/*TODO*/// * TYA	Transfer index Y to accumulator
/*TODO*/// ***************************************************************/
/*TODO*///#define TYA 													\
/*TODO*///	A = Y;														\
/*TODO*///	SET_NZ(A)
/*TODO*///
/*TODO*///    
}
