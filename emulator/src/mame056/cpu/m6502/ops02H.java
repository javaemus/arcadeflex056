/**
 * ported to v0.56
 */
package mame056.cpu.m6502;

import static arcadeflex036.osdepend.logerror;
import static mame056.memory.*;
import static mame056.cpu.m6502.m6502.*;
import static mame056.cpuintrfH.*;
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
    public static void SET_NZ(int n) {
        if (n == 0) {
            m6502.u8_p = ((m6502.u8_p & ~F_N) | F_Z) & 0xFF;
        } else {
            m6502.u8_p = ((m6502.u8_p & ~(F_N | F_Z)) | ((n) & F_N)) & 0xFF;
        }
    }

    public static void SET_Z(int n) {
        if ((n) == 0) {
            m6502.u8_p = (m6502.u8_p | F_Z) & 0xFF;
        } else {
            m6502.u8_p = (m6502.u8_p & ~F_Z) & 0xFF;
        }
    }

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
        int r = cpu_readop(m6502.pc.D);
        m6502.pc.AddD(1);
        return r & 0xFF;
    }

    /**
     * *************************************************************
     * RDOPARG read an opcode argument
     * *************************************************************
     */
    public static int RDOPARG() {
        int tmp = cpu_readop_arg(m6502.pc.D);
        m6502.pc.AddD(1);
        return tmp & 0xFF;
    }

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

    /**
     * *************************************************************
     * BRA branch relative extra cycle if page boundary is crossed
     * *************************************************************
     */
    public static void BRA(boolean cond) {
        if (cond) {
            int tmp = RDOPARG();
            m6502.ea.SetD(m6502.pc.D + (byte) tmp);
            m6502_ICount[0] -= (m6502.pc.H == m6502.ea.H) ? 3 : 4;
            m6502.pc.SetD(m6502.ea.D);
            change_pc16(m6502.pc.D);
        } else {
            m6502.pc.AddD(1);//PCW++;
            m6502_ICount[0] -= 2;
        }
    }

    /**
     * *************************************************************
     *
     * Helper macros to build the effective address
     *
     **************************************************************
     */
    /**
     * *************************************************************
     * EA = zero page address
     * *************************************************************
     */
    public static void EA_ZPG() {
        m6502.zp.SetL(RDOPARG());
        m6502.ea.SetD(m6502.zp.D);
    }

    /**
     * *************************************************************
     * EA = zero page address + X
     * *************************************************************
     */
    public static void EA_ZPX() {
        m6502.zp.SetL(RDOPARG() + m6502.u8_x);
        m6502.ea.SetD(m6502.zp.D);
    }

    /**
     * *************************************************************
     * EA = zero page address + Y
     * *************************************************************
     */
    public static void EA_ZPY() {
        m6502.zp.SetL(RDOPARG() + m6502.u8_y);
        m6502.ea.SetD(m6502.zp.D);
    }

    /**
     * *************************************************************
     * EA = absolute address
     * *************************************************************
     */
    public static void EA_ABS() {
        m6502.ea.SetL(RDOPARG());
        m6502.ea.SetH(RDOPARG());
    }

    /**
     * *************************************************************
     * EA = absolute address + X
     * *************************************************************
     */
    public static void EA_ABX() {
        EA_ABS();
        m6502.ea.SetD(m6502.ea.D + m6502.u8_x);
    }

    /**
     * *************************************************************
     * EA = absolute address + Y
     * *************************************************************
     */
    public static void EA_ABY() {
        EA_ABS();
        m6502.ea.SetD(m6502.ea.D + m6502.u8_y);
    }

    /**
     * *************************************************************
     * EA = zero page + X indirect (pre indexed)
     * *************************************************************
     */
    public static void EA_IDX() {
        m6502.zp.SetL(RDOPARG() + m6502.u8_x);
        m6502.ea.SetL(RDMEM(m6502.zp.D));
        m6502.zp.AddL(1);
        m6502.ea.SetH(RDMEM(m6502.zp.D));
    }

    /**
     * *************************************************************
     * EA = zero page indirect + Y (post indexed) subtract 1 cycle if page
     * boundary is crossed
     * *************************************************************
     */
    public static void EA_IDY() {
        m6502.zp.SetL(RDOPARG());
        m6502.ea.SetL(RDMEM(m6502.zp.D));
        m6502.zp.AddL(1);
        m6502.ea.SetH(RDMEM(m6502.zp.D));
        if (m6502.ea.L + m6502.u8_y > 0xff) {
            m6502_ICount[0]--;
        }
        m6502.ea.SetD(m6502.ea.D + m6502.u8_y);

    }

    /**
     * *************************************************************
     * EA = indirect (only used by JMP)
     * *************************************************************
     */
    public static void EA_IND() {
        EA_ABS();
        int tmp = RDMEM(m6502.ea.D);
        m6502.ea.AddL(1);
        m6502.ea.SetH(RDMEM(m6502.ea.D));
        m6502.ea.SetL(tmp);
    }

    /* read a value into tmp */
    public static int RD_IMM() {
        return RDOPARG();
    }

    public static int RD_ACC() {
        return m6502.u8_a & 0xFF;
    }

    public static int RD_ZPG() {
        EA_ZPG();
        return RDMEM(m6502.ea.D);
    }

    public static int RD_ZPX() {
        EA_ZPX();
        return RDMEM(m6502.ea.D);
    }

    public static int RD_ZPY() {
        EA_ZPY();
        return RDMEM(m6502.ea.D);
    }

    public static int RD_ABS() {
        EA_ABS();
        return RDMEM(m6502.ea.D);
    }

    public static int RD_ABX() {
        EA_ABX();
        return RDMEM(m6502.ea.D);
    }

    public static int RD_ABY() {
        EA_ABY();
        return RDMEM(m6502.ea.D);
    }

    public static int RD_IDX() {
        EA_IDX();
        return RDMEM(m6502.ea.D);
    }

    public static int RD_IDY() {
        EA_IDY();
        return RDMEM(m6502.ea.D);
    }

    /* write a value from tmp */
    public static void WR_ZPG(int tmp) {
        EA_ZPG();
        WRMEM(m6502.ea.D, tmp);
    }

    public static void WR_ZPX(int tmp) {
        EA_ZPX();
        WRMEM(m6502.ea.D, tmp);
    }

    public static void WR_ZPY(int tmp) {
        EA_ZPY();
        WRMEM(m6502.ea.D, tmp);
    }

    public static void WR_ABS(int tmp) {
        EA_ABS();
        WRMEM(m6502.ea.D, tmp);
    }

    public static void WR_ABX(int tmp) {
        EA_ABX();
        WRMEM(m6502.ea.D, tmp);
    }

    public static void WR_ABY(int tmp) {
        EA_ABY();
        WRMEM(m6502.ea.D, tmp);
    }

    public static void WR_IDX(int tmp) {
        EA_IDX();
        WRMEM(m6502.ea.D, tmp);
    }

    public static void WR_IDY(int tmp) {
        EA_IDY();
        WRMEM(m6502.ea.D, tmp);
    }

    /* write back a value from tmp to the last EA */
    public static void WB_ACC(int tmp) {
        m6502.u8_a = tmp & 0xFF;
    }

    public static void WB_EA(int tmp) {
        WRMEM(m6502.ea.D, tmp);
    }

    /**
     * *************************************************************
     ***************************************************************
     * Macros to emulate the plain 6502 opcodes
     * **************************************************************
     * *************************************************************
     */
    /**
     * *************************************************************
     * push a register onto the stack
     * *************************************************************
     */
    public static void PUSH(int Rg) {
        WRMEM(m6502.sp.D, Rg);
        m6502.sp.AddL(-1);
    }

    /**
     * *************************************************************
     * pull a register from the stack
     * *************************************************************
     */
    public static int PULL() {
        m6502.sp.AddL(1);
        return RDMEM(m6502.sp.D);
    }

    /* 6502 ********************************************************
    *	ADC Add with carry
    ***************************************************************/
    public static void ADC(int tmp) {
        if ((m6502.u8_p & F_D) != 0) {
            int c = m6502.u8_p & F_C;
            int lo = (m6502.u8_a & 0xF) + (tmp & 0xF) + c;
            int hi = (m6502.u8_a & 0xF0) + (tmp & 0xF0);
            m6502.u8_p &= ((F_V | F_C | F_N | F_Z) ^ 0xFFFFFFFF);
            if ((lo + hi & 0xFF) == 0) {
                m6502.u8_p |= F_Z;
            }
            if (lo > 9) {
                hi += 16;
                lo += 6;
            }
            if ((hi & 0x80) != 0) {
                m6502.u8_p |= F_N;
            }
            if (((m6502.u8_a ^ tmp ^ 0xFFFFFFFF) & (m6502.u8_a ^ hi) & F_N) != 0) {
                m6502.u8_p |= F_V;
            }
            if (hi > 144) {
                hi += 96;
            }
            if ((hi & 0xFF00) != 0) {
                m6502.u8_p |= F_C;
            }
            m6502.u8_a = ((lo & 0xF) + (hi & 0xF0));
        } else {
            int c = m6502.u8_p & F_C;
            int sum = m6502.u8_a + tmp + c;
            m6502.u8_p &= ((F_V | F_C) ^ 0xFFFFFFFF);
            if (((m6502.u8_a ^ tmp ^ 0xFFFFFFFF) & (m6502.u8_a ^ sum) & F_N) != 0) {
                m6502.u8_p |= F_V;
            }
            if ((sum & 0xFF00) != 0) {
                m6502.u8_p |= F_C;
            }
            m6502.u8_a = (sum & 0xFF);
            SET_NZ(m6502.u8_a);
        }
    }

    /* 6502 ********************************************************
 *	AND Logical and
 ***************************************************************/
    public static void AND(int tmp) {
        m6502.u8_a = (m6502.u8_a & tmp) & 0xFF;
        SET_NZ(m6502.u8_a);
    }

    /* 6502 ********************************************************
 *	ASL Arithmetic shift left
 ***************************************************************/
    public static int ASL(int tmp) {
        m6502.u8_p = (m6502.u8_p & (F_C ^ 0xFFFFFFFF) | tmp >> 7 & F_C);
        tmp = tmp << 1 & 0xFF;
        SET_NZ(tmp);
        return tmp;
    }

    /* 6502 ********************************************************
     *	BCC Branch if carry clear
     ***************************************************************/
    public static void BCC() {
        BRA((m6502.u8_p & F_C) == 0);
    }

    /* 6502 ********************************************************
     *	BCS Branch if carry set
     ***************************************************************/
    public static void BCS() {
        BRA((m6502.u8_p & F_C) != 0);
    }

    /* 6502 ********************************************************
     *	BEQ Branch if equal
     ***************************************************************/
    public static void BEQ() {
        BRA((m6502.u8_p & F_Z) != 0);
    }

    /* 6502 ********************************************************
    *	BIT Bit test
    ***************************************************************/
    public static void BIT(int tmp) {
        m6502.u8_p &= ((F_N | F_V | F_Z) ^ 0xFFFFFFFF);
        m6502.u8_p |= tmp & (F_N | F_V);
        if ((tmp & m6502.u8_a) == 0) {
            m6502.u8_p |= F_Z;
        }
    }

    /* 6502 ********************************************************
    *	BMI Branch if minus
    ***************************************************************/
    public static void BMI() {
        BRA((m6502.u8_p & F_N) != 0);
    }

    /* 6502 ********************************************************
    *	BNE Branch if not equal
    ***************************************************************/
    public static void BNE() {
        BRA((m6502.u8_p & F_Z) == 0);
    }

    /* 6502 ********************************************************
    *	BPL Branch if plus
    ***************************************************************/
    public static void BPL() {
        BRA((m6502.u8_p & F_N) == 0);
    }

    /* 6502 ********************************************************
    *	BRK Break
    *	increment PC, push PC hi, PC lo, flags (with B bit set),
    *	set I flag, jump via IRQ vector
    ***************************************************************/
    public static void BRK() {
        m6502.pc.AddD(1);
        PUSH(m6502.pc.H);
        PUSH(m6502.pc.L);
        PUSH(m6502.u8_p | F_B);
        m6502.u8_p = (m6502.u8_p | F_I) & 0xFF;
        m6502.pc.SetL(RDMEM(M6502_IRQ_VEC));
        m6502.pc.SetH(RDMEM(M6502_IRQ_VEC + 1));
        change_pc16(m6502.pc.D);
    }

    /* 6502 ********************************************************
    * BVC	Branch if overflow clear
    ***************************************************************/
    public static void BVC() {
        BRA((m6502.u8_p & F_V) == 0);
    }

    /* 6502 ********************************************************
    * BVS	Branch if overflow set
    ***************************************************************/
    public static void BVS() {
        BRA((m6502.u8_p & F_V) != 0);
    }

    /* 6502 ********************************************************
    * CLC	Clear carry flag
    ***************************************************************/
    public static void CLC() {
        m6502.u8_p &= (F_C ^ 0xFFFFFFFF);
    }

    /* 6502 ********************************************************
    * CLD	Clear decimal flag
    ***************************************************************/
    public static void CLD() {
        m6502.u8_p &= (F_D ^ 0xFFFFFFFF);
    }

    /* 6502 ********************************************************
    * CLI	Clear interrupt flag
    ***************************************************************/
    public static void CLI() {
        if ((m6502.u8_irq_state != CLEAR_LINE) && (m6502.u8_p & F_I) != 0) {
            logerror("M6502#%d CLI sets after_cli\n", cpu_getactivecpu());
            m6502.u8_after_cli = 1;
        }
        m6502.u8_p = (m6502.u8_p & ~F_I) & 0xFF;
    }

    /* 6502 ********************************************************
    * CLV	Clear overflow flag
    ***************************************************************/
    public static void CLV() {
        m6502.u8_p &= (F_V ^ 0xFFFFFFFF);
    }

    /* 6502 ********************************************************
    *	CMP Compare accumulator
    ***************************************************************/
    public static void CMP(int tmp) {
        m6502.u8_p &= (F_C ^ 0xFFFFFFFF);
        if (m6502.u8_a >= tmp) {
            m6502.u8_p |= F_C;
        }
        SET_NZ((m6502.u8_a - tmp) & 0xFF);
    }

    /* 6502 ********************************************************
    *	CPX Compare index X
    ***************************************************************/
    public static void CPX(int tmp) {
        m6502.u8_p &= (F_C ^ 0xFFFFFFFF);
        if (m6502.u8_x >= tmp) {
            m6502.u8_p |= F_C;
        }
        SET_NZ((m6502.u8_x - tmp) & 0xFF);
    }

    /* 6502 ********************************************************
    *	CPY Compare index Y
    ***************************************************************/
    public static void CPY(int tmp) {
        m6502.u8_p &= (F_C ^ 0xFFFFFFFF);
        if (m6502.u8_y >= tmp) {
            m6502.u8_p |= F_C;
        }
        SET_NZ((m6502.u8_y - tmp) & 0xFF);
    }

    /* 6502 ********************************************************
    *	DEC Decrement memory
    ***************************************************************/
    public static int DEC(int tmp) {
        tmp = (tmp - 1) & 0xFF;
        SET_NZ(tmp);
        return tmp;
    }

    /* 6502 ********************************************************
    *	DEX Decrement index X
    ***************************************************************/
    public static void DEX() {
        m6502.u8_x = (m6502.u8_x - 1) & 0xFF;
        SET_NZ(m6502.u8_x);
    }

    /* 6502 ********************************************************
    *	DEY Decrement index Y
    ***************************************************************/
    public static void DEY() {
        m6502.u8_y = (m6502.u8_y - 1) & 0xFF;
        SET_NZ(m6502.u8_y);
    }

    /* 6502 ********************************************************
    *	EOR Logical exclusive or
    ***************************************************************/
    public static void EOR(int tmp) {
        m6502.u8_a = (m6502.u8_a ^ tmp) & 0xFF;
        SET_NZ(m6502.u8_a);
    }

    /* 6502 ********************************************************
    *	ILL Illegal opcode
    ***************************************************************/
    public static void ILL() {
        logerror("M6502 illegal opcode %04x: %02x\n", (m6502.pc.D - 1) & 0xffff, cpu_readop((m6502.pc.D - 1) & 0xffff));
    }

    /* 6502 ********************************************************
    *	INC Increment memory
    ***************************************************************/
    public static int INC(int tmp) {
        tmp = (tmp + 1) & 0xFF;
        SET_NZ(tmp);
        return tmp;
    }

    /* 6502 ********************************************************
    *	INX Increment index X
    ***************************************************************/
    public static void INX() {
        m6502.u8_x = (m6502.u8_x + 1) & 0xFF;
        SET_NZ(m6502.u8_x);
    }

    /* 6502 ********************************************************
    *	INY Increment index Y
    ***************************************************************/
    public static void INY() {
        m6502.u8_y = (m6502.u8_y + 1) & 0xFF;
        SET_NZ(m6502.u8_y);
    }

    /* 6502 ********************************************************
    *	JMP Jump to address
    *	set PC to the effective address
    ***************************************************************/
    public static void JMP() {
        if (m6502.ea == m6502.ppc && m6502.u8_pending_irq == 0 && m6502.u8_after_cli == 0) {
            if (m6502_ICount[0] > 0) {
                m6502_ICount[0] = 0;
            }
        }
        m6502.pc.SetD(m6502.ea.D);
        change_pc16(m6502.pc.D);
    }


    /* 6502 ********************************************************
    *	JSR Jump to subroutine
    *	decrement PC (sic!) push PC hi, push PC lo and set
    *	PC to the effective address
    ***************************************************************/
    public static void JSR() {
        m6502.ea.SetL(RDOPARG());
        PUSH(m6502.pc.H);
        PUSH(m6502.pc.L);
        m6502.ea.SetH(RDOPARG());
        m6502.pc.SetD(m6502.ea.D);
        change_pc16(m6502.pc.D);
    }

    /* 6502 ********************************************************
    *	LDA Load accumulator
    ***************************************************************/
    public static void LDA(int tmp) {
        m6502.u8_a = tmp & 0xFF;
        SET_NZ(m6502.u8_a);
    }

    /* 6502 ********************************************************
    *	LDX Load index X
    ***************************************************************/
    public static void LDX(int tmp) {
        m6502.u8_x = tmp & 0xFF;
        SET_NZ(m6502.u8_x);
    }

    /* 6502 ********************************************************
    *	LDY Load index Y
    ***************************************************************/
    public static void LDY(int tmp) {
        m6502.u8_y = tmp & 0xFF;
        SET_NZ(m6502.u8_y);
    }

    /* 6502 ********************************************************
     *	LSR Logic shift right
     *	0 -> [7][6][5][4][3][2][1][0] -> C
     ***************************************************************/
    public static int LSR(int tmp) {
        m6502.u8_p = (m6502.u8_p & (F_C ^ 0xFFFFFFFF) | tmp & F_C);
        tmp = tmp >> 1 & 0xFF;
        SET_NZ(tmp);
        return tmp;
    }

    /* 6502 ********************************************************
     *	NOP No operation
     ***************************************************************/
    public static void NOP() {
    }

    /* 6502 ********************************************************
     *	ORA Logical inclusive or
     ***************************************************************/
    public static void ORA(int tmp) {
        m6502.u8_a = (m6502.u8_a | tmp) & 0xFF;
        SET_NZ(m6502.u8_a);
    }

    /* 6502 ********************************************************
     *	PHA Push accumulator
     ***************************************************************/
    public static void PHA() {
        PUSH(m6502.u8_a);
    }

    /* 6502 ********************************************************
     *	PHP Push processor status (flags)
     ***************************************************************/
    public static void PHP() {
        PUSH(m6502.u8_p);
    }

    /* 6502 ********************************************************
     *	PLA Pull accumulator
     ***************************************************************/
    public static void PLA() {
        m6502.u8_a = PULL();
        SET_NZ(m6502.u8_a);
    }

    /* 6502 ********************************************************
    *	PLP Pull processor status (flags)
    ***************************************************************/
    public static void PLP() {
        if ((m6502.u8_p & F_I) != 0) {
            m6502.u8_p = PULL();
            if ((m6502.u8_irq_state != CLEAR_LINE) && (m6502.u8_p & F_I) == 0) {
                logerror("M6502#%d PLP sets after_cli\n", cpu_getactivecpu());
                m6502.u8_after_cli = 1;
            }
        } else {
            m6502.u8_p = PULL();
        }
        m6502.u8_p = (m6502.u8_p | (F_T | F_B)) & 0xFF;
    }

    /* 6502 ********************************************************
    * ROL	Rotate left
    *	new C <- [7][6][5][4][3][2][1][0] <- C
    ***************************************************************/
    public static int ROL(int tmp) {
        tmp = tmp << 1 | m6502.u8_p & F_C;
        m6502.u8_p = (m6502.u8_p & (F_C ^ 0xFFFFFFFF) | tmp >> 8 & F_C);
        tmp &= 0xFF;
        SET_NZ(tmp);
        return tmp;
    }

    /* 6502 ********************************************************
    * ROR	Rotate right
    *	C -> [7][6][5][4][3][2][1][0] -> new C
    ***************************************************************/
    public static int ROR(int tmp) {
        tmp |= (m6502.u8_p & F_C) << 8;
        m6502.u8_p = (m6502.u8_p & (F_C ^ 0xFFFFFFFF) | tmp & F_C);
        tmp = tmp >> 1 & 0xFF;
        SET_NZ(tmp);
        return tmp;
    }

    /* 6502 ********************************************************
    * RTI	Return from interrupt
    * pull flags, pull PC lo, pull PC hi and increment PC
    *	PCW++;
    ***************************************************************/
    public static void RTI() {
        m6502.u8_p = PULL();
        m6502.pc.SetL(PULL());
        m6502.pc.SetH(PULL());
        m6502.u8_p = (m6502.u8_p | (F_T | F_B)) & 0xFF;
        if ((m6502.u8_irq_state != CLEAR_LINE) && (m6502.u8_p & F_I) == 0) {
            logerror("M6502#%d RTI sets after_cli\n", cpu_getactivecpu());
            m6502.u8_after_cli = 1;
        }
        change_pc16(m6502.pc.D);
    }

    /* 6502 ********************************************************
    *	RTS Return from subroutine
    *	pull PC lo, PC hi and increment PC
    ***************************************************************/
    public static void RTS() {
        m6502.pc.SetL(PULL());
        m6502.pc.SetH(PULL());
        m6502.pc.AddD(1);
        change_pc16(m6502.pc.D);
    }

    /* 6502 ********************************************************
    *	SBC Subtract with carry
    ***************************************************************/
    public static void SBC(int tmp) {
        if ((m6502.u8_p & F_D) != 0) {
            int c = m6502.u8_p & F_C ^ F_C;
            int sum = m6502.u8_a - tmp - c;
            int lo = (m6502.u8_a & 0xF) - (tmp & 0xF) - c;
            int hi = (m6502.u8_a & 0xF0) - (tmp & 0xF0);
            if ((lo & 0x10) != 0) {
                lo -= 6;
                hi--;
            }
            m6502.u8_p &= ((F_V | F_C | F_Z | F_N) ^ 0xFFFFFFFF);
            if (((m6502.u8_a ^ tmp) & (m6502.u8_a ^ sum) & F_N) != 0) {
                m6502.u8_p |= F_V;
            }
            if ((hi & 0x100) != 0) {
                hi -= 96;
            }
            if ((sum & 0xFF00) == 0) {
                m6502.u8_p |= F_C;
            }
            if ((m6502.u8_a - tmp - c & 0xFF) == 0) {
                m6502.u8_p |= F_Z;
            }
            if ((m6502.u8_a - tmp - c & 0x80) != 0) {
                m6502.u8_p |= F_N;
            }
            m6502.u8_a = (lo & 0xF | hi & 0xF0);
        } else {
            int c = m6502.u8_p & F_C ^ F_C;
            int sum = m6502.u8_a - tmp - c;
            m6502.u8_p &= ((F_V | F_C) ^ 0xFFFFFFFF);
            if (((m6502.u8_a ^ tmp) & (m6502.u8_a ^ sum) & F_N) != 0) {
                m6502.u8_p |= F_V;
            }
            if ((sum & 0xFF00) == 0) {
                m6502.u8_p |= F_C;
            }
            m6502.u8_a = (sum & 0xFF);
            SET_NZ(m6502.u8_a);
        }
    }

    /* 6502 ********************************************************
    *	SEC Set carry flag
    ***************************************************************/
    public static void SEC() {
        m6502.u8_p |= F_C;
    }

    /* 6502 ********************************************************
    *	SED Set decimal flag
    ***************************************************************/
    public static void SED() {
        m6502.u8_p |= F_D;
    }

    /* 6502 ********************************************************
    *	SEI Set interrupt flag
    ***************************************************************/
    public static void SEI() {
        m6502.u8_p |= F_I;
    }


    /* 6502 ********************************************************
     * STA	Store accumulator
     ***************************************************************/
    public static int STA() {
        return m6502.u8_a & 0xFF;
    }

    /* 6502 ********************************************************
     * STX	Store index X
     ***************************************************************/
    public static int STX() {
        return m6502.u8_x & 0xFF;
    }

    /* 6502 ********************************************************
     * STY	Store index Y
     ***************************************************************/
    public static int STY() {
        return m6502.u8_y & 0xFF;
    }

    /* 6502 ********************************************************
     * TAX	Transfer accumulator to index X
     ***************************************************************/
    public static void TAX() {
        m6502.u8_x = m6502.u8_a & 0xFF;
        SET_NZ(m6502.u8_x);
    }

    /* 6502 ********************************************************
     * TAY	Transfer accumulator to index Y
     ***************************************************************/
    public static void TAY() {
        m6502.u8_y = m6502.u8_a & 0xFF;
        SET_NZ(m6502.u8_y);
    }

    /* 6502 ********************************************************
     * TSX	Transfer stack LSB to index X
     ***************************************************************/
    public static void TSX() {
        m6502.u8_x = m6502.sp.L;
        SET_NZ(m6502.u8_x);
    }

    /* 6502 ********************************************************
     * TXA	Transfer index X to accumulator
     ***************************************************************/
    public static void TXA() {
        m6502.u8_a = m6502.u8_x & 0xFF;
        SET_NZ(m6502.u8_a);
    }

    /* 6502 ********************************************************
     * TXS	Transfer index X to stack LSB
     * no flags changed (sic!)
     ***************************************************************/
    public static void TXS() {
        m6502.sp.SetL(m6502.u8_x);
    }

    /* 6502 ********************************************************
     * TYA	Transfer index Y to accumulator
     ***************************************************************/
    public static void TYA() {
        m6502.u8_a = m6502.u8_y & 0xFF;
        SET_NZ(m6502.u8_a);
    }
}
