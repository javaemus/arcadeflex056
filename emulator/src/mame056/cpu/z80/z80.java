/**
 * ported to v0.56
 * plus
 * Implemented DAA without lookup table
 * Fixed X/Y SCF,CCF flags
 * Fixed flags XF & YF in BIT instructions
 * Implemented MEMPTR (WZ) internal z80 register and fixed BIT_HL instructions
 */
package mame056.cpu.z80;

import static mame056.cpu.z80.z80H.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static arcadeflex036.osdepend.*;

public class z80 extends cpu_interface {

    public static int[] z80_ICount = new int[1];

    public z80() {
        cpu_num = CPU_Z80;
        burn = burn_function;
        num_irqs = 1;
        default_vector = 255;
        icount = z80_ICount;
        overclock = 1.00;
        irq_int = -1000;
        databus_width = 8;
        pgm_memory_base = 0;
        address_shift = 0;
        address_bits = 16;
        endianess = CPU_IS_LE;
        align_unit = 1;
        max_inst_len = 4;

    }

    /**
     * **************************************************************************
     * The Z80 registers. HALT is set to 1 when the CPU is halted, the refresh *
     * register is calculated as follows: refresh=(Regs.R&127)|(Regs.R2&128) *
     * **************************************************************************
     */
    public static class Z80_Regs {

        public int PREPC, PC, SP, A, F, B, C, D, E, H, L, IX, IY;
        public int A2, F2, B2, C2, D2, E2, H2, L2;
        public int WZ; //MEMPTR, esoteric register of the ZiLOG Z80 CPU
        public int R, R2, IFF1, IFF2, HALT, IM, I;
        public int irq_max;/* number of daisy chain devices        */
        public int request_irq;/* daisy chain next request device		*/
        public int service_irq;/* daisy chain next reti handling device */
        public int nmi_state;/* nmi line state */
        public int irq_state;/* irq line state */
        public int[] int_state = new int[Z80_MAXDAISY];
        public Z80_DaisyChain[] irq = new Z80_DaisyChain[Z80_MAXDAISY];
        public irqcallbacksPtr irq_callback;
        public int extra_cycles;/* extra cycles for interrupts */
    }

    private static int AF() {
        return ((Z80.A << 8) | Z80.F) & 0xFFFF;
    }

    private static int AF2() {
        return ((Z80.A2 << 8) | Z80.F2) & 0xFFFF;
    }

    public static int BC() {
        return ((Z80.B << 8) | Z80.C) & 0xFFFF;
    }

    public static int BC2() {
        return ((Z80.B2 << 8) | Z80.C2) & 0xFFFF;
    }

    public static int DE() {
        return ((Z80.D << 8) | Z80.E) & 0xFFFF;
    }

    public static int DE2() {
        return ((Z80.D2 << 8) | Z80.E2) & 0xFFFF;
    }

    public static int HL() {
        return ((Z80.H << 8) | Z80.L) & 0xFFFF;
    }

    public static int HL2() {
        return ((Z80.H2 << 8) | Z80.L2) & 0xFFFF;
    }

    private static void AF(int nn) {
        Z80.A = (nn >> 8) & 0xff;
        Z80.F = nn & 0xff;
    }

    private static void AF2(int nn) {
        Z80.A2 = (nn >> 8) & 0xff;
        Z80.F2 = nn & 0xff;
    }

    private static void BC(int nn) {
        Z80.B = (nn >> 8) & 0xff;
        Z80.C = nn & 0xff;
    }

    private static void BC2(int nn) {
        Z80.B2 = (nn >> 8) & 0xff;
        Z80.C2 = nn & 0xff;
    }

    private static void DE(int nn) {
        Z80.D = (nn >> 8) & 0xff;
        Z80.E = nn & 0xff;
    }

    private static void DE2(int nn) {
        Z80.D2 = (nn >> 8) & 0xff;
        Z80.E2 = nn & 0xff;
    }

    private static void HL(int nn) {
        Z80.H = (nn >> 8) & 0xff;
        Z80.L = nn & 0xff;
    }

    private static void HL2(int nn) {
        Z80.H2 = (nn >> 8) & 0xff;
        Z80.L2 = nn & 0xff;
    }

    public static final int CF = 0x01;
    public static final int NF = 0x02;
    public static final int PF = 0x04;
    public static final int VF = PF;
    public static final int XF = 0x08;
    public static final int HF = 0x10;
    public static final int YF = 0x20;
    public static final int ZF = 0x40;
    public static final int SF = 0x80;

    public static final int INT_IRQ = 0x01;
    public static final int NMI_IRQ = 0x02;

    static Z80_Regs Z80 = new Z80_Regs();
    static int/*UINT32*/ EA;
    static int after_EI = 0;

    private static int SZ[] = new int[256];/* zero and sign flags */
    private static int SZ_BIT[] = new int[256];/* zero, sign and parity/overflow (=zero) flags for BIT opcode */
    private static int SZP[] = new int[256];/* zero, sign and parity flags */
    private static int SZHV_inc[] = new int[256];/* zero, sign, half carry and overflow flags INC r8 */
    private static int SZHV_dec[] = new int[256];/* zero, sign, half carry and overflow flags DEC r8 */

    private static int SZHVC_Add[] = new int[2 * 256 * 256];
    private static int SZHVC_sub[] = new int[2 * 256 * 256];

    /* tmp1 value for ini/inir/outi/otir for [C.1-0][io.1-0] */
    static int irep_tmp1[][] = {
        {0, 0, 1, 0}, {0, 1, 0, 1}, {1, 0, 1, 1}, {0, 1, 1, 0}
    };

    /* tmp1 value for ind/indr/outd/otdr for [C.1-0][io.1-0] */
    static int drep_tmp1[][] = {
        {0, 1, 0, 0}, {1, 0, 0, 1}, {0, 0, 1, 0}, {0, 1, 0, 1}
    };

    /* tmp2 value for all in/out repeated opcodes for B.7-0 */
    static int breg_tmp2[] = {
        0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1,
        0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0,
        1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0,
        1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1,
        0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0,
        1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1,
        0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1,
        0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0,
        1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0,
        1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1,
        0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1,
        0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0,
        1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1,
        0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0,
        1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0,
        1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1
    };

    static int cc_op[] = {
        4, 10, 7, 6, 4, 4, 7, 4, 4, 11, 7, 6, 4, 4, 7, 4,
        8, 10, 7, 6, 4, 4, 7, 4, 12, 11, 7, 6, 4, 4, 7, 4,
        7, 10, 16, 6, 4, 4, 7, 4, 7, 11, 16, 6, 4, 4, 7, 4,
        7, 10, 13, 6, 11, 11, 10, 4, 7, 11, 13, 6, 4, 4, 7, 4,
        4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
        4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
        4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
        7, 7, 7, 7, 7, 7, 4, 7, 4, 4, 4, 4, 4, 4, 7, 4,
        4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
        4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
        4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
        4, 4, 4, 4, 4, 4, 7, 4, 4, 4, 4, 4, 4, 4, 7, 4,
        5, 10, 10, 10, 10, 11, 7, 11, 5, 10, 10, 0, 10, 17, 7, 11,
        5, 10, 10, 11, 10, 11, 7, 11, 5, 4, 10, 11, 10, 0, 7, 11,
        5, 10, 10, 19, 10, 11, 7, 11, 5, 4, 10, 4, 10, 0, 7, 11,
        5, 10, 10, 4, 10, 11, 7, 11, 5, 6, 10, 4, 10, 0, 7, 11};

    static int cc_cb[] = {
        8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
        8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
        8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
        8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
        8, 8, 8, 8, 8, 8, 12, 8, 8, 8, 8, 8, 8, 8, 12, 8,
        8, 8, 8, 8, 8, 8, 12, 8, 8, 8, 8, 8, 8, 8, 12, 8,
        8, 8, 8, 8, 8, 8, 12, 8, 8, 8, 8, 8, 8, 8, 12, 8,
        8, 8, 8, 8, 8, 8, 12, 8, 8, 8, 8, 8, 8, 8, 12, 8,
        8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
        8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
        8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
        8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
        8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
        8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
        8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8,
        8, 8, 8, 8, 8, 8, 15, 8, 8, 8, 8, 8, 8, 8, 15, 8};

    static int cc_ed[] = {
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
        12, 12, 15, 20, 8, 8, 8, 9, 12, 12, 15, 20, 8, 8, 8, 9,
        12, 12, 15, 20, 8, 8, 8, 9, 12, 12, 15, 20, 8, 8, 8, 9,
        12, 12, 15, 20, 8, 8, 8, 18, 12, 12, 15, 20, 8, 8, 8, 18,
        12, 12, 15, 20, 8, 8, 8, 8, 12, 12, 15, 20, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
        16, 16, 16, 16, 8, 8, 8, 8, 16, 16, 16, 16, 8, 8, 8, 8,
        16, 16, 16, 16, 8, 8, 8, 8, 16, 16, 16, 16, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8};

    static int cc_xy[] = {
        4, 4, 4, 4, 4, 4, 4, 4, 4, 15, 4, 4, 4, 4, 4, 4,
        4, 4, 4, 4, 4, 4, 4, 4, 4, 15, 4, 4, 4, 4, 4, 4,
        4, 14, 20, 10, 9, 9, 9, 4, 4, 15, 20, 10, 9, 9, 9, 4,
        4, 4, 4, 4, 23, 23, 19, 4, 4, 15, 4, 4, 4, 4, 4, 4,
        4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
        4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
        9, 9, 9, 9, 9, 9, 19, 9, 9, 9, 9, 9, 9, 9, 19, 9,
        19, 19, 19, 19, 19, 19, 4, 19, 4, 4, 4, 4, 9, 9, 19, 4,
        4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
        4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
        4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
        4, 4, 4, 4, 9, 9, 19, 4, 4, 4, 4, 4, 9, 9, 19, 4,
        4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4,
        4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
        4, 14, 4, 23, 4, 15, 4, 4, 4, 8, 4, 4, 4, 4, 4, 4,
        4, 4, 4, 4, 4, 4, 4, 4, 4, 10, 4, 4, 4, 4, 4, 4};

    static int cc_xycb[] = {
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
        20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
        20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
        20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
        20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23};

    /* extra cycles if jr/jp/call taken and 'interrupt latency' on rst 0-7 */
    static int cc_ex[] = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,/* DJNZ */
        5, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0,/* JR NZ/JR Z */
        5, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0,/* JR NC/JR C */
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        5, 5, 5, 5, 0, 0, 0, 0, 5, 5, 5, 5, 0, 0, 0, 0,/* LDIR/CPIR/INIR/OTIR LDDR/CPDR/INDR/OTDR */
        6, 0, 0, 0, 7, 0, 0, 2, 6, 0, 0, 0, 7, 0, 0, 2,
        6, 0, 0, 0, 7, 0, 0, 2, 6, 0, 0, 0, 7, 0, 0, 2,
        6, 0, 0, 0, 7, 0, 0, 2, 6, 0, 0, 0, 7, 0, 0, 2,
        6, 0, 0, 0, 7, 0, 0, 2, 6, 0, 0, 0, 7, 0, 0, 2};

    static int cc[][] = {cc_op, cc_cb, cc_ed, cc_xy, cc_xycb, cc_ex};

    /**
     * *************************************************************************
     * Burn an odd amount of cycles, that is instructions taking something
     * different from 4 T-states per opcode (and R increment)
     * *************************************************************************
     */
    public static void BURNODD(int cycles, int opcodes, int cyclesum) {
        if (cycles > 0) {
            Z80.R = (Z80.R + (cycles / cyclesum) * opcodes) & 0xFF;
            z80_ICount[0] -= (cycles / cyclesum) * cyclesum;
        }
    }

    /**
     * *************************************************************
     * Enter HALT state; write 1 to fake port on first execution
     * *************************************************************
     */
    public void ENTER_HALT() {
        Z80.PC = (Z80.PC - 1) & 0xFFFF;
        Z80.HALT = 1;
        if (after_EI == 0) {
            burn.handler(z80_ICount[0]);
        }
    }

    /**
     * *************************************************************
     * Leave HALT state; write 0 to fake port
     * *************************************************************
     */
    public static void LEAVE_HALT() {
        if (Z80.HALT != 0) {
            Z80.HALT = 0;
            Z80.PC = (Z80.PC + 1) & 0xFFFF;
        }
    }

    /**
     * *************************************************************
     * Input a byte from given I/O port
     * *************************************************************
     */
    public static int IN(int port) {
        return cpu_readport16(port) & 0xff;
    }

    /**
     * *************************************************************
     * Output a byte to given I/O port
     * *************************************************************
     */
    public static void OUT(int port, int value) {
        cpu_writeport16(port, value & 0xFF);
    }

    /**
     * *************************************************************
     * Read a byte from given memory location
     * *************************************************************
     */
    public static int RM(int addr) {
        return cpu_readmem16(addr) & 0xFF;
    }

    /**
     * *************************************************************
     * Read a word from given memory location
     * *************************************************************
     */
    public static int RM16(int addr) {
        return (RM(addr) | (RM((addr + 1) & 0xffff) << 8)) & 0xFFFF;
    }

    /**
     * *************************************************************
     * Write a byte to given memory location
     * *************************************************************
     */
    public static void WM(int addr, int value) {
        cpu_writemem16(addr, value & 0xFF);
    }

    /**
     * *************************************************************
     * Write a word to given memory location
     * *************************************************************
     */
    public static void WM16(int address, int data) {
        WM(address, data & 0xFF);
        WM((address + 1) & 0xffff, data >> 8);
    }

    /**
     * *************************************************************
     * ROP() is identical to RM() except it is used for reading opcodes. In case
     * of system with memory mapped I/O, this function can be used to greatly
     * speed up emulation
     * *************************************************************
     */
    public static /*UINT8*/ int ROP() {
        int pc = Z80.PC & 0xFFFF;
        Z80.PC = (Z80.PC + 1) & 0xFFFF;
        return cpu_readop(pc) & 0xFF;
    }

    /**
     * **************************************************************
     * ARG() is identical to ROP() except it is used for reading opcode
     * arguments. This difference can be used to support systems that use
     * different encoding mechanisms for opcodes and opcode arguments
     * *************************************************************
     */
    public static /*UINT8*/ int ARG() {
        int pc = Z80.PC & 0xFFFF;
        Z80.PC = (Z80.PC + 1 & 0xFFFF);
        return cpu_readop_arg(pc) & 0xFF;
    }

    public static int /*UINT32*/ ARG16() {
        int pc = Z80.PC & 0xFFFF;
        Z80.PC = (Z80.PC + 2) & 0xFFFF;
        return (cpu_readop_arg(pc) | (cpu_readop_arg((pc + 1) & 0xffff) << 8)) & 0xFFFF;
    }

    /**
     * *************************************************************
     * Calculate the effective address EA of an opcode using IX+offset resp.
     * IY+offset addressing.
     * *************************************************************
     */
    public static void EAX() {
        EA = (Z80.IX + (byte) ARG()) & 0xffff;
        Z80.WZ = EA;
    }

    public static void EAY() {
        EA = (Z80.IY + (byte) ARG()) & 0xffff;
        Z80.WZ = EA;
    }

    /**
     * *************************************************************
     * *********************POP ************************************
     */
    public static int POP() {
        int nn = RM16(Z80.SP);//RM16( _SPD, &Z80.DR );
        Z80.SP = (Z80.SP + 2) & 0xffff;
        return nn;
    }

    /**
     * *************************************************************
     * ********************PUSH ************************************
     */
    public static void PUSH(int nn) {
        Z80.SP = (Z80.SP - 2) & 0xffff;
        WM16(Z80.SP, nn);
    }

    /**
     * *************************************************************
     * JP *************************************************************
     */
    public static void JP() {
        Z80.PC = ARG16();
        Z80.WZ = Z80.PC;
        change_pc16(Z80.PC);
    }

    /**
     * *************************************************************
     * JP_COND *************************************************************
     */
    public static void JP_COND(boolean cond) {
        if (cond) {
            Z80.PC = ARG16();
            Z80.WZ = Z80.PC;
            change_pc16(Z80.PC);
        } else {
            Z80.WZ = ARG16();/* implicit do PC += 2 */
        }
    }

    /**
     * *************************************************************
     * JR *************************************************************
     */
    public static void JR() {
        byte arg = (byte) ARG();
        Z80.PC = (Z80.PC + arg) & 0xFFFF;
        Z80.WZ = Z80.PC;
        change_pc16(Z80.PC);
    }

    /*TODO*///#define JR()													\
/*TODO*///{																\
/*TODO*///	unsigned oldpc = _PCD-1;									\
/*TODO*///	INT8 arg = (INT8)ARG(); /* ARG() also increments _PC */ 	\
/*TODO*///	_PC += arg; 			/* so don't do _PC += ARG() */      \
/*TODO*///	change_pc16(_PCD);											\
/*TODO*///    /* speed up busy loop */                                    \
/*TODO*///	if( _PCD == oldpc ) 										\
/*TODO*///	{															\
/*TODO*///		if( !after_EI ) 										\
/*TODO*///			BURNODD( z80_ICount, 1, cc[Z80_TABLE_op][0x18] );	\
/*TODO*///	}															\
/*TODO*///	else														\
/*TODO*///	{															\
/*TODO*///		UINT8 op = cpu_readop(_PCD);							\
/*TODO*///		if( _PCD == oldpc-1 )									\
/*TODO*///		{														\
/*TODO*///			/* NOP - JR $-1 or EI - JR $-1 */					\
/*TODO*///			if ( op == 0x00 || op == 0xfb ) 					\
/*TODO*///			{													\
/*TODO*///				if( !after_EI ) 								\
/*TODO*///				   BURNODD( z80_ICount-cc[Z80_TABLE_op][0x00],	\
/*TODO*///					   2, cc[Z80_TABLE_op][0x00]+cc[Z80_TABLE_op][0x18]); \
/*TODO*///			}													\
/*TODO*///		}														\
/*TODO*///		else													\
/*TODO*///		/* LD SP,#xxxx - JR $-3 */								\
/*TODO*///		if( _PCD == oldpc-3 && op == 0x31 ) 					\
/*TODO*///		{														\
/*TODO*///			if( !after_EI ) 									\
/*TODO*///			   BURNODD( z80_ICount-cc[Z80_TABLE_op][0x31],		\
/*TODO*///				   2, cc[Z80_TABLE_op][0x31]+cc[Z80_TABLE_op][0x18]); \
/*TODO*///		}														\
/*TODO*///    }                                                           \
/*TODO*///}
    /**
     * *************************************************************
     * JR_COND *************************************************************
     */
    public static void JR_COND(boolean cond, int opcode) {
        if (cond) {
            byte arg = (byte) ARG();
            Z80.PC = (Z80.PC + arg) & 0xFFFF;
            z80_ICount[0] -= cc[Z80_TABLE_ex][opcode];//CC(ex,opcode);											
            change_pc16(Z80.PC);
        } else {
            Z80.PC = (Z80.PC + 1) & 0xFFFF;
        }
    }

    /**
     * *************************************************************
     * CALL *************************************************************
     */
    public static void CALL() {
        EA = ARG16();
        Z80.WZ = EA;
        PUSH(Z80.PC);
        Z80.PC = EA;
        change_pc16(Z80.PC);
    }

    /**
     * *************************************************************
     * CALL_COND *************************************************************
     */
    public static void CALL_COND(boolean cond, int opcode) {
        if (cond) {
            EA = ARG16();
            Z80.WZ = EA;
            PUSH(Z80.PC);
            Z80.PC = EA;
            z80_ICount[0] -= cc[Z80_TABLE_ex][opcode];
            change_pc16(Z80.PC);
        } else {
            Z80.WZ = ARG16();
            /* implicit call PC+=2; */
        }
    }

    /**
     * *************************************************************
     * RET_COND *************************************************************
     */
    public static void RET_COND(boolean cond, int opcode) {
        if (cond) {
            Z80.PC = POP();
            Z80.WZ = Z80.PC;
            change_pc16(Z80.PC);
            z80_ICount[0] -= cc[Z80_TABLE_ex][opcode];
        }
    }

    /**
     * *************************************************************
     * RETN / **************************************************************
     */
    public static void RETN() {
        //LOG(("Z80 #%d RETN IFF1:%d IFF2:%d\n", cpu_getactivecpu(), _IFF1, _IFF2)); 
        Z80.PC = POP();
        Z80.WZ = Z80.PC;
        change_pc16(Z80.PC);
        if (Z80.IFF1 == 0 && Z80.IFF2 == 1) {
            Z80.IFF1 = 1;
            if (Z80.irq_state != CLEAR_LINE || Z80.request_irq >= 0) {
                //LOG(("Z80 #%d RETN takes IRQ\n",cpu_getactivecpu()));							
                take_interrupt();
            }
        } else {
            Z80.IFF1 = Z80.IFF2;
        }
    }

    /**
     * *************************************************************
     * RETI *************************************************************
     */
    public static void RETI() {
        int device = Z80.service_irq;
        Z80.PC = POP();
        Z80.WZ = Z80.PC;
        change_pc16(Z80.PC);
        /* according to http://www.msxnet.org/tech/Z80/z80undoc.txt */
 /*	_IFF1 = _IFF2;	*/
        if (device >= 0) {
            //LOG(("Z80 #%d RETI device %d: $%02x\n",cpu_getactivecpu(), device, Z80.irq[device].irq_param)); 
            Z80.irq[device].interrupt_reti.handler(Z80.irq[device].irq_param);
        }
    }

    /**
     * *************************************************************
     * LD	R,A *************************************************************
     */
    public static void LD_R_A() {
        Z80.R = Z80.A & 0xFF;
        Z80.R2 = Z80.A & 0x80;
        /* keep bit 7 of R */
    }

    /**
     * *************************************************************
     * LD	A,R *************************************************************
     */
    public static void LD_A_R() {
        Z80.A = ((Z80.R & 0x7f) | Z80.R2) & 0xFF;
        Z80.F = (Z80.F & CF) | SZ[Z80.A] | (Z80.IFF2 << 2);
    }

    /**
     * *************************************************************
     * LD	I,A *************************************************************
     */
    public static void LD_I_A() {
        Z80.I = Z80.A & 0xFF;
    }

    /**
     * *************************************************************
     * LD	A,I *************************************************************
     */
    public static void LD_A_I() {
        Z80.A = Z80.I & 0xFF;
        Z80.F = (Z80.F & CF) | SZ[Z80.A] | (Z80.IFF2 << 2);
    }

    /**
     * *************************************************************
     * RST *************************************************************
     */
    public static void RST(int addr) {
        PUSH(Z80.PC);
        Z80.PC = addr & 0xFFFF;
        Z80.WZ = Z80.PC;
        change_pc16(Z80.PC);
    }

    /**
     * *************************************************************
     * INC	r8 *************************************************************
     */
    public static int INC(int value) {
        value = (value + 1) & 0xFF;
        Z80.F = (Z80.F & CF | SZHV_inc[value]);
        return value;
    }

    /**
     * *************************************************************
     * DEC	r8 *************************************************************
     */
    public static int DEC(int value) {
        value = (value - 1) & 0xFF;
        Z80.F = (Z80.F & CF | SZHV_dec[value]);
        return value;
    }

    /**
     * *************************************************************
     * RLCA *************************************************************
     */
    public static void RLCA() {
        Z80.A = ((Z80.A << 1) | (Z80.A >> 7)) & 0xFF;
        Z80.F = (Z80.F & (SF | ZF | PF)) | (Z80.A & (YF | XF | CF));
    }

    /**
     * *************************************************************
     * RRCA *************************************************************
     */
    public static void RRCA() {
        Z80.F = (Z80.F & (SF | ZF | PF)) | (Z80.A & CF);
        Z80.A = ((Z80.A >> 1) | (Z80.A << 7)) & 0xFF;
        Z80.F |= (Z80.A & (YF | XF));
    }

    /**
     * *************************************************************
     * RLA *************************************************************
     */
    public static void RLA() {
        int res = (Z80.A << 1 | Z80.F & CF) & 0xFF;
        int c = (Z80.A & 0x80) != 0 ? CF : 0;
        Z80.F = (Z80.F & (SF | ZF | PF)) | c | (res & (YF | XF));
        Z80.A = res;
    }

    /**
     * *************************************************************
     * RRA *************************************************************
     */
    public static void RRA() {
        int res = (Z80.A >> 1 | Z80.F << 7) & 0xFF;
        int c = (Z80.A & 0x1) != 0 ? CF : 0;
        Z80.F = (Z80.F & (SF | ZF | PF)) | c | (res & (YF | XF));
        Z80.A = res;
    }

    /**
     * *************************************************************
     * RRD *************************************************************
     */
    public static void RRD() {
        int n = RM(HL());
        Z80.WZ = (HL() + 1) & 0xFFFF;
        WM(HL(), ((n >> 4) | (Z80.A << 4)) & 0xFF);
        Z80.A = ((Z80.A & 0xf0) | (n & 0x0f)) & 0xFF;
        Z80.F = (Z80.F & CF) | SZP[Z80.A];
    }

    /**
     * *************************************************************
     * RLD *************************************************************
     */
    public static void RLD() {
        int n = RM(HL());
        Z80.WZ = (HL() + 1) & 0xFFFF;
        WM(HL(), ((n << 4) | (Z80.A & 0x0f)) & 0xFF);
        Z80.A = ((Z80.A & 0xf0) | (n >> 4)) & 0xFF;
        Z80.F = (Z80.F & CF) | SZP[Z80.A];
    }

    /**
     * *************************************************************
     * ADD	A,n *************************************************************
     */
    public static void ADD(int value) {
        int res = (Z80.A + value) & 0xFF;
        Z80.F = SZHVC_Add[(Z80.A << 8 | res)];
        Z80.A = res;
    }

    /**
     * *************************************************************
     * ADC	A,n *************************************************************
     */
    public static void ADC(int value) {
        int c = Z80.F & 0x1;
        int result = (Z80.A + value + c) & 0xFF;
        Z80.F = SZHVC_Add[(c << 16 | Z80.A << 8 | result)];
        Z80.A = result;
    }

    /**
     * *************************************************************
     * SUB	n *************************************************************
     */
    public static void SUB(int value) {
        int result = (Z80.A - value) & 0xFF;
        Z80.F = SZHVC_sub[(Z80.A << 8 | result)];
        Z80.A = result;
    }

    /**
     * *************************************************************
     * SBC	A,n *************************************************************
     */
    public static void SBC(int value) {
        int c = Z80.F & 1;
        int result = (Z80.A - value - c) & 0xff;
        Z80.F = SZHVC_sub[(c << 16) | (Z80.A << 8) | result];
        Z80.A = result;
    }

    /**
     * *************************************************************
     * NEG *************************************************************
     */
    public static void NEG() {
        int value = Z80.A & 0xFF;
        Z80.A = 0;
        SUB(value);
    }

    /**
     * *************************************************************
     * DAA *************************************************************
     */
    public static void DAA() {
        int a = Z80.A & 0xFF;
        if ((Z80.F & NF) != 0) {
            if ((Z80.F & HF) != 0 | ((Z80.A & 0xf) > 9)) {
                a = (a - 6) & 0xFF;
            }
            if ((Z80.F & CF) != 0 | (Z80.A > 0x99)) {
                a = (a - 0x60) & 0xFF;
            }
        } else {
            if ((Z80.F & HF) != 0 | ((Z80.A & 0xf) > 9)) {
                a = (a + 6) & 0xFF;
            }
            if ((Z80.F & CF) != 0 | (Z80.A > 0x99)) {
                a = (a + 0x60) & 0xFF;
            }
        }

        Z80.F = (Z80.F & (CF | NF)) | ((Z80.A > 0x99) ? 1 : 0) | ((Z80.A ^ a) & HF) | SZP[a];
        Z80.A = a & 0xFF;
    }

    /**
     * *************************************************************
     * AND	n *************************************************************
     */
    public static void AND(int value) {
        Z80.A = (Z80.A & value) & 0xff;
        Z80.F = SZP[Z80.A] | HF;
    }

    /**
     * *************************************************************
     * OR	n *************************************************************
     */
    public static void OR(int value) {
        Z80.A = (Z80.A | value) & 0xff;
        Z80.F = SZP[Z80.A];
    }

    /**
     * *************************************************************
     * XOR	n *************************************************************
     */
    public static void XOR(int value) {
        Z80.A = (Z80.A ^ value) & 0xff;
        Z80.F = SZP[Z80.A];
    }

    /**
     * *************************************************************
     * CP	n *************************************************************
     */
    public static void CP(int value) {
        int val = value & 0xFF;
        int result = (Z80.A - value) & 0xFF;
        Z80.F = (SZHVC_sub[(Z80.A << 8 | result)] & ~(YF | XF)) | (val & (YF | XF));
    }

    /**
     * *************************************************************
     * EX AF,AF' *************************************************************
     */
    public static void EX_AF() {
        int tmp = Z80.A;
        Z80.A = Z80.A2;
        Z80.A2 = tmp;
        tmp = Z80.F;
        Z80.F = Z80.F2;
        Z80.F2 = tmp;
    }

    /**
     * *************************************************************
     * EX DE,HL *************************************************************
     */
    public static void EX_DE_HL() {
        int tmp = Z80.D;
        Z80.D = Z80.H;
        Z80.H = tmp;
        tmp = Z80.E;
        Z80.E = Z80.L;
        Z80.L = tmp;
    }

    /**
     * *************************************************************
     * EXX *************************************************************
     */
    public static void EXX() {
        int tmp = Z80.B;
        Z80.B = Z80.B2;
        Z80.B2 = tmp;
        tmp = Z80.C;
        Z80.C = Z80.C2;
        Z80.C2 = tmp;
        tmp = Z80.D;
        Z80.D = Z80.D2;
        Z80.D2 = tmp;
        tmp = Z80.E;
        Z80.E = Z80.E2;
        Z80.E2 = tmp;
        tmp = Z80.H;
        Z80.H = Z80.H2;
        Z80.H2 = tmp;
        tmp = Z80.L;
        Z80.L = Z80.L2;
        Z80.L2 = tmp;
    }

    /**
     * *************************************************************
     * EX (SP),r16 *************************************************************
     */
    public static int EXSP(int DR) {
        int tmp = RM16(Z80.SP);
        WM16(Z80.SP, DR);
        Z80.WZ = tmp;
        /*TODO*///recheck
        return tmp;
    }

    /**
     * *************************************************************
     * ADD16 *************************************************************
     */
    public static int ADD16(int a, int b) {
        int result = a + b;
        Z80.WZ = (a + 1) & 0xFFFF;
        Z80.F = (Z80.F & (SF | ZF | VF)) | (((a ^ result ^ b) >> 8) & HF) | ((result >> 16) & CF) | ((result >> 8) & (YF | XF));
        return (result & 0xffff);
    }

    /**
     * *************************************************************
     * ADC	r16,r16 *************************************************************
     */
    public static void ADC16(int value) {
        int _HLD = HL();
        Z80.WZ = (HL() + 1) & 0xFFFF;
        int result = _HLD + value + (Z80.F & CF);
        Z80.F = (((_HLD ^ result ^ value) >> 8) & HF) | ((result >> 16) & CF) | ((result >> 8) & (SF | YF | XF))
                | (((result & 0xffff) != 0) ? 0 : ZF) | (((value ^ _HLD ^ 0x8000) & (value ^ result) & 0x8000) >> 13);
        Z80.H = (result >> 8) & 0xff;
        Z80.L = result & 0xff;
    }

    /**
     * *************************************************************
     * SBC	r16,r16 *************************************************************
     */
    public static void SBC16(int value) {
        int _HLD = HL();
        Z80.WZ = (HL() + 1) & 0xFFFF;
        int result = _HLD - value - (Z80.F & CF);
        Z80.F = (((_HLD ^ result ^ value) >> 8) & HF) | NF | ((result >> 16) & CF) | ((result >> 8) & (SF | YF | XF))
                | (((result & 0xffff) != 0) ? 0 : ZF) | (((value ^ _HLD) & (_HLD ^ result) & 0x8000) >> 13);
        Z80.H = (result >> 8) & 0xff;
        Z80.L = result & 0xff;
    }

    /**
     * *************************************************************
     * RLC	r8 *************************************************************
     */
    public static int RLC(int value) {
        int c = (value & 0x80) >> 7;
        value = (value << 1 | value >> 7) & 0xFF;
        Z80.F = (SZP[value] | c);
        return value;
    }

    /**
     * *************************************************************
     * RRC	r8 *************************************************************
     */
    public static int RRC(int value) {
        int res = value;
        int c = (res & 0x01) != 0 ? CF : 0;
        res = (res >> 1 | res << 7) & 0xFF;
        Z80.F = (SZP[res] | c);
        return res;
    }

    /**
     * *************************************************************
     * RL	r8 *************************************************************
     */
    public static int RL(int value) {
        int c = (value & 0x80) >> 7;
        value = (value << 1 | Z80.F & 0x1) & 0xFF;
        Z80.F = (SZP[value] | c);
        return value;
    }

    /**
     * *************************************************************
     * RR	r8 *************************************************************
     */
    public static int RR(int value) {
        int c = value & 0x1;
        value = (value >> 1 | Z80.F << 7) & 0xFF;
        Z80.F = (SZP[value] | c);
        return value;
    }

    /**
     * *************************************************************
     * SLA	r8 *************************************************************
     */
    public static int SLA(int value) {
        int c = (value & 0x80) >> 7;
        value = value << 1 & 0xFF;
        Z80.F = (SZP[value] | c);
        return value;
    }

    /**
     * *************************************************************
     * SRA	r8 *************************************************************
     */
    public static int SRA(int value) {
        int c = value & 0x1;
        value = value >> 1 | value & 0x80;
        Z80.F = (SZP[value] | c);
        return value;
    }

    /**
     * *************************************************************
     * SLL	r8 *************************************************************
     */
    public static int SLL(int value) {
        int c = (value & 0x80) >> 7;
        value = (value << 1 | 0x1) & 0xFF;
        Z80.F = (SZP[value] | c);
        return value;
    }

    /**
     * *************************************************************
     * SRL	r8 *************************************************************
     */
    public static int SRL(int value) {
        int c = value & 0x1;
        value = value >> 1 & 0xFF;
        Z80.F = (SZP[value] | c);
        return value;
    }

    /**
     * *************************************************************
     * BIT bit,r8 *************************************************************
     */
    private static final int[] bitSet = {1, 2, 4, 8, 16, 32, 64, 128};           // lookup table for setting a bit of an 8-bit value using OR
    private static final int[] bitRes = {254, 253, 251, 247, 239, 223, 191, 127}; // lookup table for resetting a bit of an 8-bit value using AND

    public static void BIT(int bitNumber, int value) {
        Z80.F = (Z80.F & CF) | HF | (SZ_BIT[value & bitSet[bitNumber]] & ~(YF | XF)) | (value & (YF | XF));
    }

    /**
     * *************************************************************
     * BIT bit,(HL)
     * *************************************************************
     */
    public static void BIT_HL(int bitNumber, int value) {
        Z80.F = (Z80.F & CF) | HF | (SZ_BIT[value & bitSet[bitNumber]] & ~(YF | XF)) | (((Z80.WZ >> 8) & 0xFF) & (YF | XF));
    }

    /**
     * *************************************************************
     * BIT	bit,(IX/Y+o)
     * *************************************************************
     */
    public static void BIT_XY(int bitNumber, int value) {
        Z80.F = (Z80.F & CF) | HF | (SZ_BIT[value & bitSet[bitNumber]] & ~(YF | XF)) | ((EA >> 8) & (YF | XF));
    }

    /**
     * *************************************************************
     * RES	bit,r8 *************************************************************
     */
    public static int RES(int bitNumber, int value) {
        value = value & bitRes[bitNumber];
        return value;
    }

    /**
     * *************************************************************
     * SET bit,r8 *************************************************************
     */
    public static int SET(int bitNumber, int value) {
        value = value | bitSet[bitNumber];
        return value;
    }

    /**
     * *************************************************************
     * LDI *************************************************************
     */
    public static void LDI() {
        int io = RM(HL());
        WM(DE(), io);
        Z80.F &= SF | ZF | CF;
        if (((Z80.A + io) & 0x02) != 0) {
            Z80.F |= YF;
            /* bit 1 -> flag 5 */
        }
        if (((Z80.A + io) & 0x08) != 0) {
            Z80.F |= XF;
            /* bit 3 -> flag 3 */
        }
        HL((HL() + 1) & 0xFFFF);
        DE((DE() + 1) & 0xFFFF);
        BC((BC() - 1) & 0xFFFF);
        if (BC() != 0) {
            Z80.F |= VF;
        }
    }

    /**
     * *************************************************************
     * CPI *************************************************************
     */
    public static void CPI() {
        int val = RM(HL());
        int res = (Z80.A - val) & 0xFF;
        Z80.WZ = (Z80.WZ + 1) & 0xFFFF;
        HL((HL() + 1) & 0xFFFF);
        BC((BC() - 1) & 0xFFFF);
        Z80.F = (Z80.F & CF) | (SZ[res] & ~(YF | XF)) | ((Z80.A ^ val ^ res) & HF) | NF;
        if ((Z80.F & HF) != 0) {
            res = (res - 1) & 0xff;
        }
        if ((res & 0x02) != 0) {
            Z80.F |= YF;
            /* bit 1 -> flag 5 */
        }
        if ((res & 0x08) != 0) {
            Z80.F |= XF;
            /* bit 3 -> flag 3 */
        }
        if (BC() != 0) {
            Z80.F |= VF;
        }
    }

    /**
     * *************************************************************
     * INI *************************************************************
     */
    public static void INI() {
        int io = IN(BC());
        Z80.WZ = (BC() + 1) & 0xFFFF;
        Z80.B = (Z80.B - 1) & 0xFF;
        WM(HL(), io);
        HL((HL() + 1) & 0xFFFF);
        Z80.F = SZ[Z80.B];
        if ((io & SF) != 0) {
            Z80.F |= NF;
        }
        if (((((Z80.C + 1) & 0xff) + io) & 0x100) != 0) {
            Z80.F |= HF | CF;
        }
        if (((irep_tmp1[Z80.C & 3][io & 3]
                ^ breg_tmp2[Z80.B]
                ^ (Z80.C >> 2)
                ^ (io >> 2)) & 1) != 0) {
            Z80.F |= PF;
        }
    }

    /**
     * *************************************************************
     * / * OUTI *************************************************************
     */
    public static void OUTI() {
        int io = RM(HL());
        Z80.B = (Z80.B - 1) & 0xFF;
        Z80.WZ = (BC() + 1) & 0xFFFF;
        OUT(BC(), io);
        HL((HL() + 1) & 0xFFFF);
        Z80.F = SZ[Z80.B];
        if ((io & SF) != 0) {
            Z80.F |= NF;
        }
        if (((((Z80.C + 1) & 0xff) + io) & 0x100) != 0) {
            Z80.F |= HF | CF;
        }
        if (((irep_tmp1[Z80.C & 3][io & 3]
                ^ breg_tmp2[Z80.B]
                ^ (Z80.C >> 2)
                ^ (io >> 2)) & 1) != 0) {
            Z80.F |= PF;
        }
    }

    /**
     * *************************************************************
     * LDD *************************************************************
     */
    public static void LDD() {
        int io = RM(HL());
        WM(DE(), io);
        Z80.F &= SF | ZF | CF;
        if (((Z80.A + io) & 0x02) != 0) {
            Z80.F |= YF;
            /* bit 1 -> flag 5 */
        }
        if (((Z80.A + io) & 0x08) != 0) {
            Z80.F |= XF;
            /* bit 3 -> flag 3 */
        }
        HL((HL() - 1) & 0xFFFF);
        DE((DE() - 1) & 0xFFFF);
        BC((BC() - 1) & 0xFFFF);
        if (BC() != 0) {
            Z80.F |= VF;
        }
    }

    /**
     * *************************************************************
     * CPD *************************************************************
     */
    public static void CPD() {
        int val = RM(HL());
        int res = (Z80.A - val) & 0xFF;
        Z80.WZ = (Z80.WZ - 1) & 0xFFFF;
        HL((HL() - 1) & 0xFFFF);
        BC((BC() - 1) & 0xFFFF);
        Z80.F = (Z80.F & CF) | (SZ[res] & ~(YF | XF)) | ((Z80.A ^ val ^ res) & HF) | NF;
        if ((Z80.F & HF) != 0) {
            res = (res - 1) & 0xff;
        }
        if ((res & 0x02) != 0) {
            Z80.F |= YF;
            /* bit 1 -> flag 5 */
        }
        if ((res & 0x08) != 0) {
            Z80.F |= XF;
            /* bit 3 -> flag 3 */
        }
        if (BC() != 0) {
            Z80.F |= VF;
        }
    }

    /**
     * *************************************************************
     * IND *************************************************************
     */
    public static void IND() {
        int io = IN(BC());
        Z80.WZ = (BC() - 1) & 0xFFFF;
        Z80.B = (Z80.B - 1) & 0xFF;
        WM(HL(), io);
        HL((HL() - 1) & 0xFFFF);
        Z80.F = SZ[Z80.B];
        if ((io & SF) != 0) {
            Z80.F |= NF;
        }
        if (((((Z80.C - 1) & 0xff) + io) & 0x100) != 0) {
            Z80.F |= HF | CF;
        }
        if (((drep_tmp1[Z80.C & 3][io & 3]
                ^ breg_tmp2[Z80.B]
                ^ (Z80.C >> 2)
                ^ (io >> 2)) & 1) != 0) {
            Z80.F |= PF;
        }
    }

    /**
     * *************************************************************
     * OUTD *************************************************************
     */
    public static void OUTD() {
        int io = RM(HL());
        Z80.B = (Z80.B - 1) & 0xFF;
        Z80.WZ = (BC() - 1) & 0xFFFF;
        OUT(BC(), io);
        HL((HL() - 1) & 0xFFFF);
        Z80.F = SZ[Z80.B];
        if ((io & SF) != 0) {
            Z80.F |= NF;
        }
        if (((((Z80.C - 1) & 0xff) + io) & 0x100) != 0) {
            Z80.F |= HF | CF;
        }
        if (((drep_tmp1[Z80.C & 3][io & 3]
                ^ breg_tmp2[Z80.B]
                ^ (Z80.C >> 2)
                ^ (io >> 2)) & 1) != 0) {
            Z80.F |= PF;
        }
    }

    /**
     * *************************************************************
     * LDIR *************************************************************
     */
    public static void LDIR() {
        LDI();
        if (BC() != 0) {
            Z80.PC = (Z80.PC - 2) & 0xFFFF;
            Z80.WZ = (Z80.PC + 1) & 0xFFFF;
            z80_ICount[0] -= cc[Z80_TABLE_ex][0xb0];//CC(ex,0xb0);											
        }
    }

    /**
     * *************************************************************
     * CPIR *************************************************************
     */
    public static void CPIR() {
        CPI();
        if (BC() != 0 && (Z80.F & ZF) == 0) {
            Z80.PC = (Z80.PC - 2) & 0xFFFF;
            Z80.WZ = (Z80.PC + 1) & 0xFFFF;
            z80_ICount[0] -= cc[Z80_TABLE_ex][0xb1];//CC(ex,0xb1);											
        }
    }

    /**
     * *************************************************************
     * INIR *************************************************************
     */
    public static void INIR() {
        INI();
        if (Z80.B != 0) {
            Z80.PC = (Z80.PC - 2) & 0xFFFF;
            z80_ICount[0] -= cc[Z80_TABLE_ex][0xb2];
        }
    }

    /**
     * *************************************************************
     * OTIR *************************************************************
     */
    public static void OTIR() {
        OUTI();
        if (Z80.B != 0) {
            Z80.PC = (Z80.PC - 2) & 0xFFFF;
            z80_ICount[0] -= cc[Z80_TABLE_ex][0xb3];
        }
    }

    /**
     * *************************************************************
     * LDDR *************************************************************
     */
    public static void LDDR() {
        LDD();
        if (BC() != 0) {
            Z80.PC = (Z80.PC - 2) & 0xFFFF;
            Z80.WZ = (Z80.PC + 1) & 0xFFFF;
            z80_ICount[0] -= cc[Z80_TABLE_ex][0xb8];
        }
    }

    /**
     * *************************************************************
     * CPDR *************************************************************
     */
    public static void CPDR() {
        CPD();
        if (BC() != 0 && (Z80.F & ZF) == 0) {
            Z80.PC = (Z80.PC - 2) & 0xFFFF;
            Z80.WZ = (Z80.PC + 1) & 0xFFFF;
            z80_ICount[0] -= cc[Z80_TABLE_ex][0xb9];
        }
    }

    /**
     * *************************************************************
     * INDR *************************************************************
     */
    public static void INDR() {
        IND();
        if (Z80.B != 0) {
            Z80.PC = (Z80.PC - 2) & 0xFFFF;
            z80_ICount[0] -= cc[Z80_TABLE_ex][0xba];
        }
    }

    /**
     * *************************************************************
     * OTDR *************************************************************
     */
    public static void OTDR() {
        OUTD();
        if (Z80.B != 0) {
            Z80.PC = (Z80.PC - 2) & 0xFFFF;
            z80_ICount[0] -= cc[Z80_TABLE_ex][0xbb];
        }
    }

    /**
     * *************************************************************
     * EI *************************************************************
     */
    public void EI() {
        /* If interrupts were disabled, execute one more			
         * instruction and check the IRQ line.                      
         * If not, simply set interrupt flip-flop 2                 
         */
        if (Z80.IFF1 == 0) {
            Z80.IFF1 = Z80.IFF2 = 1;
            Z80.PREPC = Z80.PC & 0xFFFF;
            Z80.R = (Z80.R + 1) & 0xFF;
            while (cpu_readop(Z80.PC) == 0xfb) /* more EIs? */ {
                //LOG(("Z80 #%d multiple EI opcodes at %04X\n",cpu_getactivecpu(), _PC));						
                z80_ICount[0] -= cc[Z80_TABLE_op][0xfb];//CC(op,0xfb);										
                Z80.PREPC = Z80.PC & 0xFFFF;
                Z80.PC = (Z80.PC + 1) & 0xFFFF;
                Z80.R = (Z80.R + 1) & 0xFF;
            }
            if (Z80.irq_state != CLEAR_LINE || Z80.request_irq >= 0) {
                after_EI = 1;
                /* avoid cycle skip hacks */
                int op = ROP();
                z80_ICount[0] -= cc[Z80_TABLE_op][op];
                Z80op[op].handler();//EXEC(op,ROP()); 									
                after_EI = 0;
                //LOG(("Z80 #%d EI takes irq\n", cpu_getactivecpu())); 
                take_interrupt();
            } else {
                int op = ROP();
                z80_ICount[0] -= cc[Z80_TABLE_op][op];
                Z80op[op].handler();//EXEC(op,ROP());
            }
        } else {
            Z80.IFF2 = 1;
        }
    }

    /*TODO*///
/*TODO*////**********************************************************
/*TODO*/// * opcodes with CB prefix
/*TODO*/// * rotate, shift and bit operations
/*TODO*/// **********************************************************/
/*TODO*///OP(cb,00) { _B = RLC(_B);											} /* RLC  B 		  */
/*TODO*///OP(cb,01) { _C = RLC(_C);											} /* RLC  C 		  */
/*TODO*///OP(cb,02) { _D = RLC(_D);											} /* RLC  D 		  */
/*TODO*///OP(cb,03) { _E = RLC(_E);											} /* RLC  E 		  */
/*TODO*///OP(cb,04) { _H = RLC(_H);											} /* RLC  H 		  */
/*TODO*///OP(cb,05) { _L = RLC(_L);											} /* RLC  L 		  */
/*TODO*///OP(cb,06) { WM( _HL, RLC(RM(_HL)) );								} /* RLC  (HL)		  */
/*TODO*///OP(cb,07) { _A = RLC(_A);											} /* RLC  A 		  */
/*TODO*///
/*TODO*///OP(cb,08) { _B = RRC(_B);											} /* RRC  B 		  */
/*TODO*///OP(cb,09) { _C = RRC(_C);											} /* RRC  C 		  */
/*TODO*///OP(cb,0a) { _D = RRC(_D);											} /* RRC  D 		  */
/*TODO*///OP(cb,0b) { _E = RRC(_E);											} /* RRC  E 		  */
/*TODO*///OP(cb,0c) { _H = RRC(_H);											} /* RRC  H 		  */
/*TODO*///OP(cb,0d) { _L = RRC(_L);											} /* RRC  L 		  */
/*TODO*///OP(cb,0e) { WM( _HL, RRC(RM(_HL)) );								} /* RRC  (HL)		  */
/*TODO*///OP(cb,0f) { _A = RRC(_A);											} /* RRC  A 		  */
/*TODO*///
/*TODO*///OP(cb,10) { _B = RL(_B);											} /* RL   B 		  */
/*TODO*///OP(cb,11) { _C = RL(_C);											} /* RL   C 		  */
/*TODO*///OP(cb,12) { _D = RL(_D);											} /* RL   D 		  */
/*TODO*///OP(cb,13) { _E = RL(_E);											} /* RL   E 		  */
/*TODO*///OP(cb,14) { _H = RL(_H);											} /* RL   H 		  */
/*TODO*///OP(cb,15) { _L = RL(_L);											} /* RL   L 		  */
/*TODO*///OP(cb,16) { WM( _HL, RL(RM(_HL)) ); 								} /* RL   (HL)		  */
/*TODO*///OP(cb,17) { _A = RL(_A);											} /* RL   A 		  */
/*TODO*///
/*TODO*///OP(cb,18) { _B = RR(_B);											} /* RR   B 		  */
/*TODO*///OP(cb,19) { _C = RR(_C);											} /* RR   C 		  */
/*TODO*///OP(cb,1a) { _D = RR(_D);											} /* RR   D 		  */
/*TODO*///OP(cb,1b) { _E = RR(_E);											} /* RR   E 		  */
/*TODO*///OP(cb,1c) { _H = RR(_H);											} /* RR   H 		  */
/*TODO*///OP(cb,1d) { _L = RR(_L);											} /* RR   L 		  */
/*TODO*///OP(cb,1e) { WM( _HL, RR(RM(_HL)) ); 								} /* RR   (HL)		  */
/*TODO*///OP(cb,1f) { _A = RR(_A);											} /* RR   A 		  */
/*TODO*///
/*TODO*///OP(cb,20) { _B = SLA(_B);											} /* SLA  B 		  */
/*TODO*///OP(cb,21) { _C = SLA(_C);											} /* SLA  C 		  */
/*TODO*///OP(cb,22) { _D = SLA(_D);											} /* SLA  D 		  */
/*TODO*///OP(cb,23) { _E = SLA(_E);											} /* SLA  E 		  */
/*TODO*///OP(cb,24) { _H = SLA(_H);											} /* SLA  H 		  */
/*TODO*///OP(cb,25) { _L = SLA(_L);											} /* SLA  L 		  */
/*TODO*///OP(cb,26) { WM( _HL, SLA(RM(_HL)) );								} /* SLA  (HL)		  */
/*TODO*///OP(cb,27) { _A = SLA(_A);											} /* SLA  A 		  */
/*TODO*///
/*TODO*///OP(cb,28) { _B = SRA(_B);											} /* SRA  B 		  */
/*TODO*///OP(cb,29) { _C = SRA(_C);											} /* SRA  C 		  */
/*TODO*///OP(cb,2a) { _D = SRA(_D);											} /* SRA  D 		  */
/*TODO*///OP(cb,2b) { _E = SRA(_E);											} /* SRA  E 		  */
/*TODO*///OP(cb,2c) { _H = SRA(_H);											} /* SRA  H 		  */
/*TODO*///OP(cb,2d) { _L = SRA(_L);											} /* SRA  L 		  */
/*TODO*///OP(cb,2e) { WM( _HL, SRA(RM(_HL)) );								} /* SRA  (HL)		  */
/*TODO*///OP(cb,2f) { _A = SRA(_A);											} /* SRA  A 		  */
/*TODO*///
/*TODO*///OP(cb,30) { _B = SLL(_B);											} /* SLL  B 		  */
/*TODO*///OP(cb,31) { _C = SLL(_C);											} /* SLL  C 		  */
/*TODO*///OP(cb,32) { _D = SLL(_D);											} /* SLL  D 		  */
/*TODO*///OP(cb,33) { _E = SLL(_E);											} /* SLL  E 		  */
/*TODO*///OP(cb,34) { _H = SLL(_H);											} /* SLL  H 		  */
/*TODO*///OP(cb,35) { _L = SLL(_L);											} /* SLL  L 		  */
/*TODO*///OP(cb,36) { WM( _HL, SLL(RM(_HL)) );								} /* SLL  (HL)		  */
/*TODO*///OP(cb,37) { _A = SLL(_A);											} /* SLL  A 		  */
/*TODO*///
/*TODO*///OP(cb,38) { _B = SRL(_B);											} /* SRL  B 		  */
/*TODO*///OP(cb,39) { _C = SRL(_C);											} /* SRL  C 		  */
/*TODO*///OP(cb,3a) { _D = SRL(_D);											} /* SRL  D 		  */
/*TODO*///OP(cb,3b) { _E = SRL(_E);											} /* SRL  E 		  */
/*TODO*///OP(cb,3c) { _H = SRL(_H);											} /* SRL  H 		  */
/*TODO*///OP(cb,3d) { _L = SRL(_L);											} /* SRL  L 		  */
/*TODO*///OP(cb,3e) { WM( _HL, SRL(RM(_HL)) );								} /* SRL  (HL)		  */
/*TODO*///OP(cb,3f) { _A = SRL(_A);											} /* SRL  A 		  */
/*TODO*///
/*TODO*///OP(cb,40) { BIT(0,_B);												} /* BIT  0,B		  */
/*TODO*///OP(cb,41) { BIT(0,_C);												} /* BIT  0,C		  */
/*TODO*///OP(cb,42) { BIT(0,_D);												} /* BIT  0,D		  */
/*TODO*///OP(cb,43) { BIT(0,_E);												} /* BIT  0,E		  */
/*TODO*///OP(cb,44) { BIT(0,_H);												} /* BIT  0,H		  */
/*TODO*///OP(cb,45) { BIT(0,_L);												} /* BIT  0,L		  */
/*TODO*///OP(cb,46) { BIT(0,RM(_HL)); 										} /* BIT  0,(HL)	  */
/*TODO*///OP(cb,47) { BIT(0,_A);												} /* BIT  0,A		  */
/*TODO*///
/*TODO*///OP(cb,48) { BIT(1,_B);												} /* BIT  1,B		  */
/*TODO*///OP(cb,49) { BIT(1,_C);												} /* BIT  1,C		  */
/*TODO*///OP(cb,4a) { BIT(1,_D);												} /* BIT  1,D		  */
/*TODO*///OP(cb,4b) { BIT(1,_E);												} /* BIT  1,E		  */
/*TODO*///OP(cb,4c) { BIT(1,_H);												} /* BIT  1,H		  */
/*TODO*///OP(cb,4d) { BIT(1,_L);												} /* BIT  1,L		  */
/*TODO*///OP(cb,4e) { BIT(1,RM(_HL)); 										} /* BIT  1,(HL)	  */
/*TODO*///OP(cb,4f) { BIT(1,_A);												} /* BIT  1,A		  */
/*TODO*///
/*TODO*///OP(cb,50) { BIT(2,_B);												} /* BIT  2,B		  */
/*TODO*///OP(cb,51) { BIT(2,_C);												} /* BIT  2,C		  */
/*TODO*///OP(cb,52) { BIT(2,_D);												} /* BIT  2,D		  */
/*TODO*///OP(cb,53) { BIT(2,_E);												} /* BIT  2,E		  */
/*TODO*///OP(cb,54) { BIT(2,_H);												} /* BIT  2,H		  */
/*TODO*///OP(cb,55) { BIT(2,_L);												} /* BIT  2,L		  */
/*TODO*///OP(cb,56) { BIT(2,RM(_HL)); 										} /* BIT  2,(HL)	  */
/*TODO*///OP(cb,57) { BIT(2,_A);												} /* BIT  2,A		  */
/*TODO*///
/*TODO*///OP(cb,58) { BIT(3,_B);												} /* BIT  3,B		  */
/*TODO*///OP(cb,59) { BIT(3,_C);												} /* BIT  3,C		  */
/*TODO*///OP(cb,5a) { BIT(3,_D);												} /* BIT  3,D		  */
/*TODO*///OP(cb,5b) { BIT(3,_E);												} /* BIT  3,E		  */
/*TODO*///OP(cb,5c) { BIT(3,_H);												} /* BIT  3,H		  */
/*TODO*///OP(cb,5d) { BIT(3,_L);												} /* BIT  3,L		  */
/*TODO*///OP(cb,5e) { BIT(3,RM(_HL)); 										} /* BIT  3,(HL)	  */
/*TODO*///OP(cb,5f) { BIT(3,_A);												} /* BIT  3,A		  */
/*TODO*///
/*TODO*///OP(cb,60) { BIT(4,_B);												} /* BIT  4,B		  */
/*TODO*///OP(cb,61) { BIT(4,_C);												} /* BIT  4,C		  */
/*TODO*///OP(cb,62) { BIT(4,_D);												} /* BIT  4,D		  */
/*TODO*///OP(cb,63) { BIT(4,_E);												} /* BIT  4,E		  */
/*TODO*///OP(cb,64) { BIT(4,_H);												} /* BIT  4,H		  */
/*TODO*///OP(cb,65) { BIT(4,_L);												} /* BIT  4,L		  */
/*TODO*///OP(cb,66) { BIT(4,RM(_HL)); 										} /* BIT  4,(HL)	  */
/*TODO*///OP(cb,67) { BIT(4,_A);												} /* BIT  4,A		  */
/*TODO*///
/*TODO*///OP(cb,68) { BIT(5,_B);												} /* BIT  5,B		  */
/*TODO*///OP(cb,69) { BIT(5,_C);												} /* BIT  5,C		  */
/*TODO*///OP(cb,6a) { BIT(5,_D);												} /* BIT  5,D		  */
/*TODO*///OP(cb,6b) { BIT(5,_E);												} /* BIT  5,E		  */
/*TODO*///OP(cb,6c) { BIT(5,_H);												} /* BIT  5,H		  */
/*TODO*///OP(cb,6d) { BIT(5,_L);												} /* BIT  5,L		  */
/*TODO*///OP(cb,6e) { BIT(5,RM(_HL)); 										} /* BIT  5,(HL)	  */
/*TODO*///OP(cb,6f) { BIT(5,_A);												} /* BIT  5,A		  */
/*TODO*///
/*TODO*///OP(cb,70) { BIT(6,_B);												} /* BIT  6,B		  */
/*TODO*///OP(cb,71) { BIT(6,_C);												} /* BIT  6,C		  */
/*TODO*///OP(cb,72) { BIT(6,_D);												} /* BIT  6,D		  */
/*TODO*///OP(cb,73) { BIT(6,_E);												} /* BIT  6,E		  */
/*TODO*///OP(cb,74) { BIT(6,_H);												} /* BIT  6,H		  */
/*TODO*///OP(cb,75) { BIT(6,_L);												} /* BIT  6,L		  */
/*TODO*///OP(cb,76) { BIT(6,RM(_HL)); 										} /* BIT  6,(HL)	  */
/*TODO*///OP(cb,77) { BIT(6,_A);												} /* BIT  6,A		  */
/*TODO*///
/*TODO*///OP(cb,78) { BIT(7,_B);												} /* BIT  7,B		  */
/*TODO*///OP(cb,79) { BIT(7,_C);												} /* BIT  7,C		  */
/*TODO*///OP(cb,7a) { BIT(7,_D);												} /* BIT  7,D		  */
/*TODO*///OP(cb,7b) { BIT(7,_E);												} /* BIT  7,E		  */
/*TODO*///OP(cb,7c) { BIT(7,_H);												} /* BIT  7,H		  */
/*TODO*///OP(cb,7d) { BIT(7,_L);												} /* BIT  7,L		  */
/*TODO*///OP(cb,7e) { BIT(7,RM(_HL)); 										} /* BIT  7,(HL)	  */
/*TODO*///OP(cb,7f) { BIT(7,_A);												} /* BIT  7,A		  */
/*TODO*///
/*TODO*///OP(cb,80) { _B = RES(0,_B); 										} /* RES  0,B		  */
/*TODO*///OP(cb,81) { _C = RES(0,_C); 										} /* RES  0,C		  */
/*TODO*///OP(cb,82) { _D = RES(0,_D); 										} /* RES  0,D		  */
/*TODO*///OP(cb,83) { _E = RES(0,_E); 										} /* RES  0,E		  */
/*TODO*///OP(cb,84) { _H = RES(0,_H); 										} /* RES  0,H		  */
/*TODO*///OP(cb,85) { _L = RES(0,_L); 										} /* RES  0,L		  */
/*TODO*///OP(cb,86) { WM( _HL, RES(0,RM(_HL)) );								} /* RES  0,(HL)	  */
/*TODO*///OP(cb,87) { _A = RES(0,_A); 										} /* RES  0,A		  */
/*TODO*///
/*TODO*///OP(cb,88) { _B = RES(1,_B); 										} /* RES  1,B		  */
/*TODO*///OP(cb,89) { _C = RES(1,_C); 										} /* RES  1,C		  */
/*TODO*///OP(cb,8a) { _D = RES(1,_D); 										} /* RES  1,D		  */
/*TODO*///OP(cb,8b) { _E = RES(1,_E); 										} /* RES  1,E		  */
/*TODO*///OP(cb,8c) { _H = RES(1,_H); 										} /* RES  1,H		  */
/*TODO*///OP(cb,8d) { _L = RES(1,_L); 										} /* RES  1,L		  */
/*TODO*///OP(cb,8e) { WM( _HL, RES(1,RM(_HL)) );								} /* RES  1,(HL)	  */
/*TODO*///OP(cb,8f) { _A = RES(1,_A); 										} /* RES  1,A		  */
/*TODO*///
/*TODO*///OP(cb,90) { _B = RES(2,_B); 										} /* RES  2,B		  */
/*TODO*///OP(cb,91) { _C = RES(2,_C); 										} /* RES  2,C		  */
/*TODO*///OP(cb,92) { _D = RES(2,_D); 										} /* RES  2,D		  */
/*TODO*///OP(cb,93) { _E = RES(2,_E); 										} /* RES  2,E		  */
/*TODO*///OP(cb,94) { _H = RES(2,_H); 										} /* RES  2,H		  */
/*TODO*///OP(cb,95) { _L = RES(2,_L); 										} /* RES  2,L		  */
/*TODO*///OP(cb,96) { WM( _HL, RES(2,RM(_HL)) );								} /* RES  2,(HL)	  */
/*TODO*///OP(cb,97) { _A = RES(2,_A); 										} /* RES  2,A		  */
/*TODO*///
/*TODO*///OP(cb,98) { _B = RES(3,_B); 										} /* RES  3,B		  */
/*TODO*///OP(cb,99) { _C = RES(3,_C); 										} /* RES  3,C		  */
/*TODO*///OP(cb,9a) { _D = RES(3,_D); 										} /* RES  3,D		  */
/*TODO*///OP(cb,9b) { _E = RES(3,_E); 										} /* RES  3,E		  */
/*TODO*///OP(cb,9c) { _H = RES(3,_H); 										} /* RES  3,H		  */
/*TODO*///OP(cb,9d) { _L = RES(3,_L); 										} /* RES  3,L		  */
/*TODO*///OP(cb,9e) { WM( _HL, RES(3,RM(_HL)) );								} /* RES  3,(HL)	  */
/*TODO*///OP(cb,9f) { _A = RES(3,_A); 										} /* RES  3,A		  */
/*TODO*///
/*TODO*///OP(cb,a0) { _B = RES(4,_B); 										} /* RES  4,B		  */
/*TODO*///OP(cb,a1) { _C = RES(4,_C); 										} /* RES  4,C		  */
/*TODO*///OP(cb,a2) { _D = RES(4,_D); 										} /* RES  4,D		  */
/*TODO*///OP(cb,a3) { _E = RES(4,_E); 										} /* RES  4,E		  */
/*TODO*///OP(cb,a4) { _H = RES(4,_H); 										} /* RES  4,H		  */
/*TODO*///OP(cb,a5) { _L = RES(4,_L); 										} /* RES  4,L		  */
/*TODO*///OP(cb,a6) { WM( _HL, RES(4,RM(_HL)) );								} /* RES  4,(HL)	  */
/*TODO*///OP(cb,a7) { _A = RES(4,_A); 										} /* RES  4,A		  */
/*TODO*///
/*TODO*///OP(cb,a8) { _B = RES(5,_B); 										} /* RES  5,B		  */
/*TODO*///OP(cb,a9) { _C = RES(5,_C); 										} /* RES  5,C		  */
/*TODO*///OP(cb,aa) { _D = RES(5,_D); 										} /* RES  5,D		  */
/*TODO*///OP(cb,ab) { _E = RES(5,_E); 										} /* RES  5,E		  */
/*TODO*///OP(cb,ac) { _H = RES(5,_H); 										} /* RES  5,H		  */
/*TODO*///OP(cb,ad) { _L = RES(5,_L); 										} /* RES  5,L		  */
/*TODO*///OP(cb,ae) { WM( _HL, RES(5,RM(_HL)) );								} /* RES  5,(HL)	  */
/*TODO*///OP(cb,af) { _A = RES(5,_A); 										} /* RES  5,A		  */
/*TODO*///
/*TODO*///OP(cb,b0) { _B = RES(6,_B); 										} /* RES  6,B		  */
/*TODO*///OP(cb,b1) { _C = RES(6,_C); 										} /* RES  6,C		  */
/*TODO*///OP(cb,b2) { _D = RES(6,_D); 										} /* RES  6,D		  */
/*TODO*///OP(cb,b3) { _E = RES(6,_E); 										} /* RES  6,E		  */
/*TODO*///OP(cb,b4) { _H = RES(6,_H); 										} /* RES  6,H		  */
/*TODO*///OP(cb,b5) { _L = RES(6,_L); 										} /* RES  6,L		  */
/*TODO*///OP(cb,b6) { WM( _HL, RES(6,RM(_HL)) );								} /* RES  6,(HL)	  */
/*TODO*///OP(cb,b7) { _A = RES(6,_A); 										} /* RES  6,A		  */
/*TODO*///
/*TODO*///OP(cb,b8) { _B = RES(7,_B); 										} /* RES  7,B		  */
/*TODO*///OP(cb,b9) { _C = RES(7,_C); 										} /* RES  7,C		  */
/*TODO*///OP(cb,ba) { _D = RES(7,_D); 										} /* RES  7,D		  */
/*TODO*///OP(cb,bb) { _E = RES(7,_E); 										} /* RES  7,E		  */
/*TODO*///OP(cb,bc) { _H = RES(7,_H); 										} /* RES  7,H		  */
/*TODO*///OP(cb,bd) { _L = RES(7,_L); 										} /* RES  7,L		  */
/*TODO*///OP(cb,be) { WM( _HL, RES(7,RM(_HL)) );								} /* RES  7,(HL)	  */
/*TODO*///OP(cb,bf) { _A = RES(7,_A); 										} /* RES  7,A		  */
/*TODO*///
/*TODO*///OP(cb,c0) { _B = SET(0,_B); 										} /* SET  0,B		  */
/*TODO*///OP(cb,c1) { _C = SET(0,_C); 										} /* SET  0,C		  */
/*TODO*///OP(cb,c2) { _D = SET(0,_D); 										} /* SET  0,D		  */
/*TODO*///OP(cb,c3) { _E = SET(0,_E); 										} /* SET  0,E		  */
/*TODO*///OP(cb,c4) { _H = SET(0,_H); 										} /* SET  0,H		  */
/*TODO*///OP(cb,c5) { _L = SET(0,_L); 										} /* SET  0,L		  */
/*TODO*///OP(cb,c6) { WM( _HL, SET(0,RM(_HL)) );								} /* SET  0,(HL)	  */
/*TODO*///OP(cb,c7) { _A = SET(0,_A); 										} /* SET  0,A		  */
/*TODO*///
/*TODO*///OP(cb,c8) { _B = SET(1,_B); 										} /* SET  1,B		  */
/*TODO*///OP(cb,c9) { _C = SET(1,_C); 										} /* SET  1,C		  */
/*TODO*///OP(cb,ca) { _D = SET(1,_D); 										} /* SET  1,D		  */
/*TODO*///OP(cb,cb) { _E = SET(1,_E); 										} /* SET  1,E		  */
/*TODO*///OP(cb,cc) { _H = SET(1,_H); 										} /* SET  1,H		  */
/*TODO*///OP(cb,cd) { _L = SET(1,_L); 										} /* SET  1,L		  */
/*TODO*///OP(cb,ce) { WM( _HL, SET(1,RM(_HL)) );								} /* SET  1,(HL)	  */
/*TODO*///OP(cb,cf) { _A = SET(1,_A); 										} /* SET  1,A		  */
/*TODO*///
/*TODO*///OP(cb,d0) { _B = SET(2,_B); 										} /* SET  2,B		  */
/*TODO*///OP(cb,d1) { _C = SET(2,_C); 										} /* SET  2,C		  */
/*TODO*///OP(cb,d2) { _D = SET(2,_D); 										} /* SET  2,D		  */
/*TODO*///OP(cb,d3) { _E = SET(2,_E); 										} /* SET  2,E		  */
/*TODO*///OP(cb,d4) { _H = SET(2,_H); 										} /* SET  2,H		  */
/*TODO*///OP(cb,d5) { _L = SET(2,_L); 										} /* SET  2,L		  */
/*TODO*///OP(cb,d6) { WM( _HL, SET(2,RM(_HL)) );								}/* SET  2,(HL) 	 */
/*TODO*///OP(cb,d7) { _A = SET(2,_A); 										} /* SET  2,A		  */
/*TODO*///
/*TODO*///OP(cb,d8) { _B = SET(3,_B); 										} /* SET  3,B		  */
/*TODO*///OP(cb,d9) { _C = SET(3,_C); 										} /* SET  3,C		  */
/*TODO*///OP(cb,da) { _D = SET(3,_D); 										} /* SET  3,D		  */
/*TODO*///OP(cb,db) { _E = SET(3,_E); 										} /* SET  3,E		  */
/*TODO*///OP(cb,dc) { _H = SET(3,_H); 										} /* SET  3,H		  */
/*TODO*///OP(cb,dd) { _L = SET(3,_L); 										} /* SET  3,L		  */
/*TODO*///OP(cb,de) { WM( _HL, SET(3,RM(_HL)) );								} /* SET  3,(HL)	  */
/*TODO*///OP(cb,df) { _A = SET(3,_A); 										} /* SET  3,A		  */
/*TODO*///
/*TODO*///OP(cb,e0) { _B = SET(4,_B); 										} /* SET  4,B		  */
/*TODO*///OP(cb,e1) { _C = SET(4,_C); 										} /* SET  4,C		  */
/*TODO*///OP(cb,e2) { _D = SET(4,_D); 										} /* SET  4,D		  */
/*TODO*///OP(cb,e3) { _E = SET(4,_E); 										} /* SET  4,E		  */
/*TODO*///OP(cb,e4) { _H = SET(4,_H); 										} /* SET  4,H		  */
/*TODO*///OP(cb,e5) { _L = SET(4,_L); 										} /* SET  4,L		  */
/*TODO*///OP(cb,e6) { WM( _HL, SET(4,RM(_HL)) );								} /* SET  4,(HL)	  */
/*TODO*///OP(cb,e7) { _A = SET(4,_A); 										} /* SET  4,A		  */
/*TODO*///
/*TODO*///OP(cb,e8) { _B = SET(5,_B); 										} /* SET  5,B		  */
/*TODO*///OP(cb,e9) { _C = SET(5,_C); 										} /* SET  5,C		  */
/*TODO*///OP(cb,ea) { _D = SET(5,_D); 										} /* SET  5,D		  */
/*TODO*///OP(cb,eb) { _E = SET(5,_E); 										} /* SET  5,E		  */
/*TODO*///OP(cb,ec) { _H = SET(5,_H); 										} /* SET  5,H		  */
/*TODO*///OP(cb,ed) { _L = SET(5,_L); 										} /* SET  5,L		  */
/*TODO*///OP(cb,ee) { WM( _HL, SET(5,RM(_HL)) );								} /* SET  5,(HL)	  */
/*TODO*///OP(cb,ef) { _A = SET(5,_A); 										} /* SET  5,A		  */
/*TODO*///
/*TODO*///OP(cb,f0) { _B = SET(6,_B); 										} /* SET  6,B		  */
/*TODO*///OP(cb,f1) { _C = SET(6,_C); 										} /* SET  6,C		  */
/*TODO*///OP(cb,f2) { _D = SET(6,_D); 										} /* SET  6,D		  */
/*TODO*///OP(cb,f3) { _E = SET(6,_E); 										} /* SET  6,E		  */
/*TODO*///OP(cb,f4) { _H = SET(6,_H); 										} /* SET  6,H		  */
/*TODO*///OP(cb,f5) { _L = SET(6,_L); 										} /* SET  6,L		  */
/*TODO*///OP(cb,f6) { WM( _HL, SET(6,RM(_HL)) );								} /* SET  6,(HL)	  */
/*TODO*///OP(cb,f7) { _A = SET(6,_A); 										} /* SET  6,A		  */
/*TODO*///
/*TODO*///OP(cb,f8) { _B = SET(7,_B); 										} /* SET  7,B		  */
/*TODO*///OP(cb,f9) { _C = SET(7,_C); 										} /* SET  7,C		  */
/*TODO*///OP(cb,fa) { _D = SET(7,_D); 										} /* SET  7,D		  */
/*TODO*///OP(cb,fb) { _E = SET(7,_E); 										} /* SET  7,E		  */
/*TODO*///OP(cb,fc) { _H = SET(7,_H); 										} /* SET  7,H		  */
/*TODO*///OP(cb,fd) { _L = SET(7,_L); 										} /* SET  7,L		  */
/*TODO*///OP(cb,fe) { WM( _HL, SET(7,RM(_HL)) );								} /* SET  7,(HL)	  */
/*TODO*///OP(cb,ff) { _A = SET(7,_A); 										} /* SET  7,A		  */
/*TODO*///
/*TODO*///
/*TODO*////**********************************************************
/*TODO*///* opcodes with DD/FD CB prefix
/*TODO*///* rotate, shift and bit operations with (IX+o)
/*TODO*///**********************************************************/
/*TODO*///OP(xycb,00) { _B = RLC( RM(EA) ); WM( EA,_B );						} /* RLC  B=(XY+o)	  */
/*TODO*///OP(xycb,01) { _C = RLC( RM(EA) ); WM( EA,_C );						} /* RLC  C=(XY+o)	  */
/*TODO*///OP(xycb,02) { _D = RLC( RM(EA) ); WM( EA,_D );						} /* RLC  D=(XY+o)	  */
/*TODO*///OP(xycb,03) { _E = RLC( RM(EA) ); WM( EA,_E );						} /* RLC  E=(XY+o)	  */
/*TODO*///OP(xycb,04) { _H = RLC( RM(EA) ); WM( EA,_H );						} /* RLC  H=(XY+o)	  */
/*TODO*///OP(xycb,05) { _L = RLC( RM(EA) ); WM( EA,_L );						} /* RLC  L=(XY+o)	  */
/*TODO*///OP(xycb,06) { WM( EA, RLC( RM(EA) ) );								} /* RLC  (XY+o)	  */
/*TODO*///OP(xycb,07) { _A = RLC( RM(EA) ); WM( EA,_A );						} /* RLC  A=(XY+o)	  */
/*TODO*///
/*TODO*///OP(xycb,08) { _B = RRC( RM(EA) ); WM( EA,_B );						} /* RRC  B=(XY+o)	  */
/*TODO*///OP(xycb,09) { _C = RRC( RM(EA) ); WM( EA,_C );						} /* RRC  C=(XY+o)	  */
/*TODO*///OP(xycb,0a) { _D = RRC( RM(EA) ); WM( EA,_D );						} /* RRC  D=(XY+o)	  */
/*TODO*///OP(xycb,0b) { _E = RRC( RM(EA) ); WM( EA,_E );						} /* RRC  E=(XY+o)	  */
/*TODO*///OP(xycb,0c) { _H = RRC( RM(EA) ); WM( EA,_H );						} /* RRC  H=(XY+o)	  */
/*TODO*///OP(xycb,0d) { _L = RRC( RM(EA) ); WM( EA,_L );						} /* RRC  L=(XY+o)	  */
/*TODO*///OP(xycb,0e) { WM( EA,RRC( RM(EA) ) );								} /* RRC  (XY+o)	  */
/*TODO*///OP(xycb,0f) { _A = RRC( RM(EA) ); WM( EA,_A );						} /* RRC  A=(XY+o)	  */
/*TODO*///
/*TODO*///OP(xycb,10) { _B = RL( RM(EA) ); WM( EA,_B );						} /* RL   B=(XY+o)	  */
/*TODO*///OP(xycb,11) { _C = RL( RM(EA) ); WM( EA,_C );						} /* RL   C=(XY+o)	  */
/*TODO*///OP(xycb,12) { _D = RL( RM(EA) ); WM( EA,_D );						} /* RL   D=(XY+o)	  */
/*TODO*///OP(xycb,13) { _E = RL( RM(EA) ); WM( EA,_E );						} /* RL   E=(XY+o)	  */
/*TODO*///OP(xycb,14) { _H = RL( RM(EA) ); WM( EA,_H );						} /* RL   H=(XY+o)	  */
/*TODO*///OP(xycb,15) { _L = RL( RM(EA) ); WM( EA,_L );						} /* RL   L=(XY+o)	  */
/*TODO*///OP(xycb,16) { WM( EA,RL( RM(EA) ) );								} /* RL   (XY+o)	  */
/*TODO*///OP(xycb,17) { _A = RL( RM(EA) ); WM( EA,_A );						} /* RL   A=(XY+o)	  */
/*TODO*///
/*TODO*///OP(xycb,18) { _B = RR( RM(EA) ); WM( EA,_B );						} /* RR   B=(XY+o)	  */
/*TODO*///OP(xycb,19) { _C = RR( RM(EA) ); WM( EA,_C );						} /* RR   C=(XY+o)	  */
/*TODO*///OP(xycb,1a) { _D = RR( RM(EA) ); WM( EA,_D );						} /* RR   D=(XY+o)	  */
/*TODO*///OP(xycb,1b) { _E = RR( RM(EA) ); WM( EA,_E );						} /* RR   E=(XY+o)	  */
/*TODO*///OP(xycb,1c) { _H = RR( RM(EA) ); WM( EA,_H );						} /* RR   H=(XY+o)	  */
/*TODO*///OP(xycb,1d) { _L = RR( RM(EA) ); WM( EA,_L );						} /* RR   L=(XY+o)	  */
/*TODO*///OP(xycb,1e) { WM( EA,RR( RM(EA) ) );								} /* RR   (XY+o)	  */
/*TODO*///OP(xycb,1f) { _A = RR( RM(EA) ); WM( EA,_A );						} /* RR   A=(XY+o)	  */
/*TODO*///
/*TODO*///OP(xycb,20) { _B = SLA( RM(EA) ); WM( EA,_B );						} /* SLA  B=(XY+o)	  */
/*TODO*///OP(xycb,21) { _C = SLA( RM(EA) ); WM( EA,_C );						} /* SLA  C=(XY+o)	  */
/*TODO*///OP(xycb,22) { _D = SLA( RM(EA) ); WM( EA,_D );						} /* SLA  D=(XY+o)	  */
/*TODO*///OP(xycb,23) { _E = SLA( RM(EA) ); WM( EA,_E );						} /* SLA  E=(XY+o)	  */
/*TODO*///OP(xycb,24) { _H = SLA( RM(EA) ); WM( EA,_H );						} /* SLA  H=(XY+o)	  */
/*TODO*///OP(xycb,25) { _L = SLA( RM(EA) ); WM( EA,_L );						} /* SLA  L=(XY+o)	  */
/*TODO*///OP(xycb,26) { WM( EA,SLA( RM(EA) ) );								} /* SLA  (XY+o)	  */
/*TODO*///OP(xycb,27) { _A = SLA( RM(EA) ); WM( EA,_A );						} /* SLA  A=(XY+o)	  */
/*TODO*///
/*TODO*///OP(xycb,28) { _B = SRA( RM(EA) ); WM( EA,_B );						} /* SRA  B=(XY+o)	  */
/*TODO*///OP(xycb,29) { _C = SRA( RM(EA) ); WM( EA,_C );						} /* SRA  C=(XY+o)	  */
/*TODO*///OP(xycb,2a) { _D = SRA( RM(EA) ); WM( EA,_D );						} /* SRA  D=(XY+o)	  */
/*TODO*///OP(xycb,2b) { _E = SRA( RM(EA) ); WM( EA,_E );						} /* SRA  E=(XY+o)	  */
/*TODO*///OP(xycb,2c) { _H = SRA( RM(EA) ); WM( EA,_H );						} /* SRA  H=(XY+o)	  */
/*TODO*///OP(xycb,2d) { _L = SRA( RM(EA) ); WM( EA,_L );						} /* SRA  L=(XY+o)	  */
/*TODO*///OP(xycb,2e) { WM( EA,SRA( RM(EA) ) );								} /* SRA  (XY+o)	  */
/*TODO*///OP(xycb,2f) { _A = SRA( RM(EA) ); WM( EA,_A );						} /* SRA  A=(XY+o)	  */
/*TODO*///
/*TODO*///OP(xycb,30) { _B = SLL( RM(EA) ); WM( EA,_B );						} /* SLL  B=(XY+o)	  */
/*TODO*///OP(xycb,31) { _C = SLL( RM(EA) ); WM( EA,_C );						} /* SLL  C=(XY+o)	  */
/*TODO*///OP(xycb,32) { _D = SLL( RM(EA) ); WM( EA,_D );						} /* SLL  D=(XY+o)	  */
/*TODO*///OP(xycb,33) { _E = SLL( RM(EA) ); WM( EA,_E );						} /* SLL  E=(XY+o)	  */
/*TODO*///OP(xycb,34) { _H = SLL( RM(EA) ); WM( EA,_H );						} /* SLL  H=(XY+o)	  */
/*TODO*///OP(xycb,35) { _L = SLL( RM(EA) ); WM( EA,_L );						} /* SLL  L=(XY+o)	  */
/*TODO*///OP(xycb,36) { WM( EA,SLL( RM(EA) ) );								} /* SLL  (XY+o)	  */
/*TODO*///OP(xycb,37) { _A = SLL( RM(EA) ); WM( EA,_A );						} /* SLL  A=(XY+o)	  */
/*TODO*///
/*TODO*///OP(xycb,38) { _B = SRL( RM(EA) ); WM( EA,_B );						} /* SRL  B=(XY+o)	  */
/*TODO*///OP(xycb,39) { _C = SRL( RM(EA) ); WM( EA,_C );						} /* SRL  C=(XY+o)	  */
/*TODO*///OP(xycb,3a) { _D = SRL( RM(EA) ); WM( EA,_D );						} /* SRL  D=(XY+o)	  */
/*TODO*///OP(xycb,3b) { _E = SRL( RM(EA) ); WM( EA,_E );						} /* SRL  E=(XY+o)	  */
/*TODO*///OP(xycb,3c) { _H = SRL( RM(EA) ); WM( EA,_H );						} /* SRL  H=(XY+o)	  */
/*TODO*///OP(xycb,3d) { _L = SRL( RM(EA) ); WM( EA,_L );						} /* SRL  L=(XY+o)	  */
/*TODO*///OP(xycb,3e) { WM( EA,SRL( RM(EA) ) );								} /* SRL  (XY+o)	  */
/*TODO*///OP(xycb,3f) { _A = SRL( RM(EA) ); WM( EA,_A );						} /* SRL  A=(XY+o)	  */
/*TODO*///
/*TODO*///OP(xycb,40) { xycb_46();											} /* BIT  0,B=(XY+o)  */
/*TODO*///OP(xycb,41) { xycb_46();													  } /* BIT	0,C=(XY+o)	*/
/*TODO*///OP(xycb,42) { xycb_46();											} /* BIT  0,D=(XY+o)  */
/*TODO*///OP(xycb,43) { xycb_46();											} /* BIT  0,E=(XY+o)  */
/*TODO*///OP(xycb,44) { xycb_46();											} /* BIT  0,H=(XY+o)  */
/*TODO*///OP(xycb,45) { xycb_46();											} /* BIT  0,L=(XY+o)  */
/*TODO*///OP(xycb,46) { BIT_XY(0,RM(EA)); 									} /* BIT  0,(XY+o)	  */
/*TODO*///OP(xycb,47) { xycb_46();											} /* BIT  0,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,48) { xycb_4e();											} /* BIT  1,B=(XY+o)  */
/*TODO*///OP(xycb,49) { xycb_4e();													  } /* BIT	1,C=(XY+o)	*/
/*TODO*///OP(xycb,4a) { xycb_4e();											} /* BIT  1,D=(XY+o)  */
/*TODO*///OP(xycb,4b) { xycb_4e();											} /* BIT  1,E=(XY+o)  */
/*TODO*///OP(xycb,4c) { xycb_4e();											} /* BIT  1,H=(XY+o)  */
/*TODO*///OP(xycb,4d) { xycb_4e();											} /* BIT  1,L=(XY+o)  */
/*TODO*///OP(xycb,4e) { BIT_XY(1,RM(EA)); 									} /* BIT  1,(XY+o)	  */
/*TODO*///OP(xycb,4f) { xycb_4e();											} /* BIT  1,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,50) { xycb_56();											} /* BIT  2,B=(XY+o)  */
/*TODO*///OP(xycb,51) { xycb_56();													  } /* BIT	2,C=(XY+o)	*/
/*TODO*///OP(xycb,52) { xycb_56();											} /* BIT  2,D=(XY+o)  */
/*TODO*///OP(xycb,53) { xycb_56();											} /* BIT  2,E=(XY+o)  */
/*TODO*///OP(xycb,54) { xycb_56();											} /* BIT  2,H=(XY+o)  */
/*TODO*///OP(xycb,55) { xycb_56();											} /* BIT  2,L=(XY+o)  */
/*TODO*///OP(xycb,56) { BIT_XY(2,RM(EA)); 									} /* BIT  2,(XY+o)	  */
/*TODO*///OP(xycb,57) { xycb_56();											} /* BIT  2,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,58) { xycb_5e();											} /* BIT  3,B=(XY+o)  */
/*TODO*///OP(xycb,59) { xycb_5e();													  } /* BIT	3,C=(XY+o)	*/
/*TODO*///OP(xycb,5a) { xycb_5e();											} /* BIT  3,D=(XY+o)  */
/*TODO*///OP(xycb,5b) { xycb_5e();											} /* BIT  3,E=(XY+o)  */
/*TODO*///OP(xycb,5c) { xycb_5e();											} /* BIT  3,H=(XY+o)  */
/*TODO*///OP(xycb,5d) { xycb_5e();											} /* BIT  3,L=(XY+o)  */
/*TODO*///OP(xycb,5e) { BIT_XY(3,RM(EA)); 									} /* BIT  3,(XY+o)	  */
/*TODO*///OP(xycb,5f) { xycb_5e();											} /* BIT  3,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,60) { xycb_66();											} /* BIT  4,B=(XY+o)  */
/*TODO*///OP(xycb,61) { xycb_66();													  } /* BIT	4,C=(XY+o)	*/
/*TODO*///OP(xycb,62) { xycb_66();											} /* BIT  4,D=(XY+o)  */
/*TODO*///OP(xycb,63) { xycb_66();											} /* BIT  4,E=(XY+o)  */
/*TODO*///OP(xycb,64) { xycb_66();											} /* BIT  4,H=(XY+o)  */
/*TODO*///OP(xycb,65) { xycb_66();											} /* BIT  4,L=(XY+o)  */
/*TODO*///OP(xycb,66) { BIT_XY(4,RM(EA)); 									} /* BIT  4,(XY+o)	  */
/*TODO*///OP(xycb,67) { xycb_66();											} /* BIT  4,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,68) { xycb_6e();											} /* BIT  5,B=(XY+o)  */
/*TODO*///OP(xycb,69) { xycb_6e();													  } /* BIT	5,C=(XY+o)	*/
/*TODO*///OP(xycb,6a) { xycb_6e();											} /* BIT  5,D=(XY+o)  */
/*TODO*///OP(xycb,6b) { xycb_6e();											} /* BIT  5,E=(XY+o)  */
/*TODO*///OP(xycb,6c) { xycb_6e();											} /* BIT  5,H=(XY+o)  */
/*TODO*///OP(xycb,6d) { xycb_6e();											} /* BIT  5,L=(XY+o)  */
/*TODO*///OP(xycb,6e) { BIT_XY(5,RM(EA)); 									} /* BIT  5,(XY+o)	  */
/*TODO*///OP(xycb,6f) { xycb_6e();											} /* BIT  5,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,70) { xycb_76();											} /* BIT  6,B=(XY+o)  */
/*TODO*///OP(xycb,71) { xycb_76();													  } /* BIT	6,C=(XY+o)	*/
/*TODO*///OP(xycb,72) { xycb_76();											} /* BIT  6,D=(XY+o)  */
/*TODO*///OP(xycb,73) { xycb_76();											} /* BIT  6,E=(XY+o)  */
/*TODO*///OP(xycb,74) { xycb_76();											} /* BIT  6,H=(XY+o)  */
/*TODO*///OP(xycb,75) { xycb_76();											} /* BIT  6,L=(XY+o)  */
/*TODO*///OP(xycb,76) { BIT_XY(6,RM(EA)); 									} /* BIT  6,(XY+o)	  */
/*TODO*///OP(xycb,77) { xycb_76();											} /* BIT  6,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,78) { xycb_7e();											} /* BIT  7,B=(XY+o)  */
/*TODO*///OP(xycb,79) { xycb_7e();													  } /* BIT	7,C=(XY+o)	*/
/*TODO*///OP(xycb,7a) { xycb_7e();											} /* BIT  7,D=(XY+o)  */
/*TODO*///OP(xycb,7b) { xycb_7e();											} /* BIT  7,E=(XY+o)  */
/*TODO*///OP(xycb,7c) { xycb_7e();											} /* BIT  7,H=(XY+o)  */
/*TODO*///OP(xycb,7d) { xycb_7e();											} /* BIT  7,L=(XY+o)  */
/*TODO*///OP(xycb,7e) { BIT_XY(7,RM(EA)); 									} /* BIT  7,(XY+o)	  */
/*TODO*///OP(xycb,7f) { xycb_7e();											} /* BIT  7,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,80) { _B = RES(0, RM(EA) ); WM( EA,_B );					} /* RES  0,B=(XY+o)  */
/*TODO*///OP(xycb,81) { _C = RES(0, RM(EA) ); WM( EA,_C );					} /* RES  0,C=(XY+o)  */
/*TODO*///OP(xycb,82) { _D = RES(0, RM(EA) ); WM( EA,_D );					} /* RES  0,D=(XY+o)  */
/*TODO*///OP(xycb,83) { _E = RES(0, RM(EA) ); WM( EA,_E );					} /* RES  0,E=(XY+o)  */
/*TODO*///OP(xycb,84) { _H = RES(0, RM(EA) ); WM( EA,_H );					} /* RES  0,H=(XY+o)  */
/*TODO*///OP(xycb,85) { _L = RES(0, RM(EA) ); WM( EA,_L );					} /* RES  0,L=(XY+o)  */
/*TODO*///OP(xycb,86) { WM( EA, RES(0,RM(EA)) );								} /* RES  0,(XY+o)	  */
/*TODO*///OP(xycb,87) { _A = RES(0, RM(EA) ); WM( EA,_A );					} /* RES  0,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,88) { _B = RES(1, RM(EA) ); WM( EA,_B );					} /* RES  1,B=(XY+o)  */
/*TODO*///OP(xycb,89) { _C = RES(1, RM(EA) ); WM( EA,_C );					} /* RES  1,C=(XY+o)  */
/*TODO*///OP(xycb,8a) { _D = RES(1, RM(EA) ); WM( EA,_D );					} /* RES  1,D=(XY+o)  */
/*TODO*///OP(xycb,8b) { _E = RES(1, RM(EA) ); WM( EA,_E );					} /* RES  1,E=(XY+o)  */
/*TODO*///OP(xycb,8c) { _H = RES(1, RM(EA) ); WM( EA,_H );					} /* RES  1,H=(XY+o)  */
/*TODO*///OP(xycb,8d) { _L = RES(1, RM(EA) ); WM( EA,_L );					} /* RES  1,L=(XY+o)  */
/*TODO*///OP(xycb,8e) { WM( EA, RES(1,RM(EA)) );								} /* RES  1,(XY+o)	  */
/*TODO*///OP(xycb,8f) { _A = RES(1, RM(EA) ); WM( EA,_A );					} /* RES  1,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,90) { _B = RES(2, RM(EA) ); WM( EA,_B );					} /* RES  2,B=(XY+o)  */
/*TODO*///OP(xycb,91) { _C = RES(2, RM(EA) ); WM( EA,_C );					} /* RES  2,C=(XY+o)  */
/*TODO*///OP(xycb,92) { _D = RES(2, RM(EA) ); WM( EA,_D );					} /* RES  2,D=(XY+o)  */
/*TODO*///OP(xycb,93) { _E = RES(2, RM(EA) ); WM( EA,_E );					} /* RES  2,E=(XY+o)  */
/*TODO*///OP(xycb,94) { _H = RES(2, RM(EA) ); WM( EA,_H );					} /* RES  2,H=(XY+o)  */
/*TODO*///OP(xycb,95) { _L = RES(2, RM(EA) ); WM( EA,_L );					} /* RES  2,L=(XY+o)  */
/*TODO*///OP(xycb,96) { WM( EA, RES(2,RM(EA)) );								} /* RES  2,(XY+o)	  */
/*TODO*///OP(xycb,97) { _A = RES(2, RM(EA) ); WM( EA,_A );					} /* RES  2,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,98) { _B = RES(3, RM(EA) ); WM( EA,_B );					} /* RES  3,B=(XY+o)  */
/*TODO*///OP(xycb,99) { _C = RES(3, RM(EA) ); WM( EA,_C );					} /* RES  3,C=(XY+o)  */
/*TODO*///OP(xycb,9a) { _D = RES(3, RM(EA) ); WM( EA,_D );					} /* RES  3,D=(XY+o)  */
/*TODO*///OP(xycb,9b) { _E = RES(3, RM(EA) ); WM( EA,_E );					} /* RES  3,E=(XY+o)  */
/*TODO*///OP(xycb,9c) { _H = RES(3, RM(EA) ); WM( EA,_H );					} /* RES  3,H=(XY+o)  */
/*TODO*///OP(xycb,9d) { _L = RES(3, RM(EA) ); WM( EA,_L );					} /* RES  3,L=(XY+o)  */
/*TODO*///OP(xycb,9e) { WM( EA, RES(3,RM(EA)) );								} /* RES  3,(XY+o)	  */
/*TODO*///OP(xycb,9f) { _A = RES(3, RM(EA) ); WM( EA,_A );					} /* RES  3,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,a0) { _B = RES(4, RM(EA) ); WM( EA,_B );					} /* RES  4,B=(XY+o)  */
/*TODO*///OP(xycb,a1) { _C = RES(4, RM(EA) ); WM( EA,_C );					} /* RES  4,C=(XY+o)  */
/*TODO*///OP(xycb,a2) { _D = RES(4, RM(EA) ); WM( EA,_D );					} /* RES  4,D=(XY+o)  */
/*TODO*///OP(xycb,a3) { _E = RES(4, RM(EA) ); WM( EA,_E );					} /* RES  4,E=(XY+o)  */
/*TODO*///OP(xycb,a4) { _H = RES(4, RM(EA) ); WM( EA,_H );					} /* RES  4,H=(XY+o)  */
/*TODO*///OP(xycb,a5) { _L = RES(4, RM(EA) ); WM( EA,_L );					} /* RES  4,L=(XY+o)  */
/*TODO*///OP(xycb,a6) { WM( EA, RES(4,RM(EA)) );								} /* RES  4,(XY+o)	  */
/*TODO*///OP(xycb,a7) { _A = RES(4, RM(EA) ); WM( EA,_A );					} /* RES  4,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,a8) { _B = RES(5, RM(EA) ); WM( EA,_B );					} /* RES  5,B=(XY+o)  */
/*TODO*///OP(xycb,a9) { _C = RES(5, RM(EA) ); WM( EA,_C );					} /* RES  5,C=(XY+o)  */
/*TODO*///OP(xycb,aa) { _D = RES(5, RM(EA) ); WM( EA,_D );					} /* RES  5,D=(XY+o)  */
/*TODO*///OP(xycb,ab) { _E = RES(5, RM(EA) ); WM( EA,_E );					} /* RES  5,E=(XY+o)  */
/*TODO*///OP(xycb,ac) { _H = RES(5, RM(EA) ); WM( EA,_H );					} /* RES  5,H=(XY+o)  */
/*TODO*///OP(xycb,ad) { _L = RES(5, RM(EA) ); WM( EA,_L );					} /* RES  5,L=(XY+o)  */
/*TODO*///OP(xycb,ae) { WM( EA, RES(5,RM(EA)) );								} /* RES  5,(XY+o)	  */
/*TODO*///OP(xycb,af) { _A = RES(5, RM(EA) ); WM( EA,_A );					} /* RES  5,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,b0) { _B = RES(6, RM(EA) ); WM( EA,_B );					} /* RES  6,B=(XY+o)  */
/*TODO*///OP(xycb,b1) { _C = RES(6, RM(EA) ); WM( EA,_C );					} /* RES  6,C=(XY+o)  */
/*TODO*///OP(xycb,b2) { _D = RES(6, RM(EA) ); WM( EA,_D );					} /* RES  6,D=(XY+o)  */
/*TODO*///OP(xycb,b3) { _E = RES(6, RM(EA) ); WM( EA,_E );					} /* RES  6,E=(XY+o)  */
/*TODO*///OP(xycb,b4) { _H = RES(6, RM(EA) ); WM( EA,_H );					} /* RES  6,H=(XY+o)  */
/*TODO*///OP(xycb,b5) { _L = RES(6, RM(EA) ); WM( EA,_L );					} /* RES  6,L=(XY+o)  */
/*TODO*///OP(xycb,b6) { WM( EA, RES(6,RM(EA)) );								} /* RES  6,(XY+o)	  */
/*TODO*///OP(xycb,b7) { _A = RES(6, RM(EA) ); WM( EA,_A );					} /* RES  6,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,b8) { _B = RES(7, RM(EA) ); WM( EA,_B );					} /* RES  7,B=(XY+o)  */
/*TODO*///OP(xycb,b9) { _C = RES(7, RM(EA) ); WM( EA,_C );					} /* RES  7,C=(XY+o)  */
/*TODO*///OP(xycb,ba) { _D = RES(7, RM(EA) ); WM( EA,_D );					} /* RES  7,D=(XY+o)  */
/*TODO*///OP(xycb,bb) { _E = RES(7, RM(EA) ); WM( EA,_E );					} /* RES  7,E=(XY+o)  */
/*TODO*///OP(xycb,bc) { _H = RES(7, RM(EA) ); WM( EA,_H );					} /* RES  7,H=(XY+o)  */
/*TODO*///OP(xycb,bd) { _L = RES(7, RM(EA) ); WM( EA,_L );					} /* RES  7,L=(XY+o)  */
/*TODO*///OP(xycb,be) { WM( EA, RES(7,RM(EA)) );								} /* RES  7,(XY+o)	  */
/*TODO*///OP(xycb,bf) { _A = RES(7, RM(EA) ); WM( EA,_A );					} /* RES  7,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,c0) { _B = SET(0, RM(EA) ); WM( EA,_B );					} /* SET  0,B=(XY+o)  */
/*TODO*///OP(xycb,c1) { _C = SET(0, RM(EA) ); WM( EA,_C );					} /* SET  0,C=(XY+o)  */
/*TODO*///OP(xycb,c2) { _D = SET(0, RM(EA) ); WM( EA,_D );					} /* SET  0,D=(XY+o)  */
/*TODO*///OP(xycb,c3) { _E = SET(0, RM(EA) ); WM( EA,_E );					} /* SET  0,E=(XY+o)  */
/*TODO*///OP(xycb,c4) { _H = SET(0, RM(EA) ); WM( EA,_H );					} /* SET  0,H=(XY+o)  */
/*TODO*///OP(xycb,c5) { _L = SET(0, RM(EA) ); WM( EA,_L );					} /* SET  0,L=(XY+o)  */
/*TODO*///OP(xycb,c6) { WM( EA, SET(0,RM(EA)) );								} /* SET  0,(XY+o)	  */
/*TODO*///OP(xycb,c7) { _A = SET(0, RM(EA) ); WM( EA,_A );					} /* SET  0,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,c8) { _B = SET(1, RM(EA) ); WM( EA,_B );					} /* SET  1,B=(XY+o)  */
/*TODO*///OP(xycb,c9) { _C = SET(1, RM(EA) ); WM( EA,_C );					} /* SET  1,C=(XY+o)  */
/*TODO*///OP(xycb,ca) { _D = SET(1, RM(EA) ); WM( EA,_D );					} /* SET  1,D=(XY+o)  */
/*TODO*///OP(xycb,cb) { _E = SET(1, RM(EA) ); WM( EA,_E );					} /* SET  1,E=(XY+o)  */
/*TODO*///OP(xycb,cc) { _H = SET(1, RM(EA) ); WM( EA,_H );					} /* SET  1,H=(XY+o)  */
/*TODO*///OP(xycb,cd) { _L = SET(1, RM(EA) ); WM( EA,_L );					} /* SET  1,L=(XY+o)  */
/*TODO*///OP(xycb,ce) { WM( EA, SET(1,RM(EA)) );								} /* SET  1,(XY+o)	  */
/*TODO*///OP(xycb,cf) { _A = SET(1, RM(EA) ); WM( EA,_A );					} /* SET  1,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,d0) { _B = SET(2, RM(EA) ); WM( EA,_B );					} /* SET  2,B=(XY+o)  */
/*TODO*///OP(xycb,d1) { _C = SET(2, RM(EA) ); WM( EA,_C );					} /* SET  2,C=(XY+o)  */
/*TODO*///OP(xycb,d2) { _D = SET(2, RM(EA) ); WM( EA,_D );					} /* SET  2,D=(XY+o)  */
/*TODO*///OP(xycb,d3) { _E = SET(2, RM(EA) ); WM( EA,_E );					} /* SET  2,E=(XY+o)  */
/*TODO*///OP(xycb,d4) { _H = SET(2, RM(EA) ); WM( EA,_H );					} /* SET  2,H=(XY+o)  */
/*TODO*///OP(xycb,d5) { _L = SET(2, RM(EA) ); WM( EA,_L );					} /* SET  2,L=(XY+o)  */
/*TODO*///OP(xycb,d6) { WM( EA, SET(2,RM(EA)) );								} /* SET  2,(XY+o)	  */
/*TODO*///OP(xycb,d7) { _A = SET(2, RM(EA) ); WM( EA,_A );					} /* SET  2,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,d8) { _B = SET(3, RM(EA) ); WM( EA,_B );					} /* SET  3,B=(XY+o)  */
/*TODO*///OP(xycb,d9) { _C = SET(3, RM(EA) ); WM( EA,_C );					} /* SET  3,C=(XY+o)  */
/*TODO*///OP(xycb,da) { _D = SET(3, RM(EA) ); WM( EA,_D );					} /* SET  3,D=(XY+o)  */
/*TODO*///OP(xycb,db) { _E = SET(3, RM(EA) ); WM( EA,_E );					} /* SET  3,E=(XY+o)  */
/*TODO*///OP(xycb,dc) { _H = SET(3, RM(EA) ); WM( EA,_H );					} /* SET  3,H=(XY+o)  */
/*TODO*///OP(xycb,dd) { _L = SET(3, RM(EA) ); WM( EA,_L );					} /* SET  3,L=(XY+o)  */
/*TODO*///OP(xycb,de) { WM( EA, SET(3,RM(EA)) );								} /* SET  3,(XY+o)	  */
/*TODO*///OP(xycb,df) { _A = SET(3, RM(EA) ); WM( EA,_A );					} /* SET  3,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,e0) { _B = SET(4, RM(EA) ); WM( EA,_B );					} /* SET  4,B=(XY+o)  */
/*TODO*///OP(xycb,e1) { _C = SET(4, RM(EA) ); WM( EA,_C );					} /* SET  4,C=(XY+o)  */
/*TODO*///OP(xycb,e2) { _D = SET(4, RM(EA) ); WM( EA,_D );					} /* SET  4,D=(XY+o)  */
/*TODO*///OP(xycb,e3) { _E = SET(4, RM(EA) ); WM( EA,_E );					} /* SET  4,E=(XY+o)  */
/*TODO*///OP(xycb,e4) { _H = SET(4, RM(EA) ); WM( EA,_H );					} /* SET  4,H=(XY+o)  */
/*TODO*///OP(xycb,e5) { _L = SET(4, RM(EA) ); WM( EA,_L );					} /* SET  4,L=(XY+o)  */
/*TODO*///OP(xycb,e6) { WM( EA, SET(4,RM(EA)) );								} /* SET  4,(XY+o)	  */
/*TODO*///OP(xycb,e7) { _A = SET(4, RM(EA) ); WM( EA,_A );					} /* SET  4,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,e8) { _B = SET(5, RM(EA) ); WM( EA,_B );					} /* SET  5,B=(XY+o)  */
/*TODO*///OP(xycb,e9) { _C = SET(5, RM(EA) ); WM( EA,_C );					} /* SET  5,C=(XY+o)  */
/*TODO*///OP(xycb,ea) { _D = SET(5, RM(EA) ); WM( EA,_D );					} /* SET  5,D=(XY+o)  */
/*TODO*///OP(xycb,eb) { _E = SET(5, RM(EA) ); WM( EA,_E );					} /* SET  5,E=(XY+o)  */
/*TODO*///OP(xycb,ec) { _H = SET(5, RM(EA) ); WM( EA,_H );					} /* SET  5,H=(XY+o)  */
/*TODO*///OP(xycb,ed) { _L = SET(5, RM(EA) ); WM( EA,_L );					} /* SET  5,L=(XY+o)  */
/*TODO*///OP(xycb,ee) { WM( EA, SET(5,RM(EA)) );								} /* SET  5,(XY+o)	  */
/*TODO*///OP(xycb,ef) { _A = SET(5, RM(EA) ); WM( EA,_A );					} /* SET  5,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,f0) { _B = SET(6, RM(EA) ); WM( EA,_B );					} /* SET  6,B=(XY+o)  */
/*TODO*///OP(xycb,f1) { _C = SET(6, RM(EA) ); WM( EA,_C );					} /* SET  6,C=(XY+o)  */
/*TODO*///OP(xycb,f2) { _D = SET(6, RM(EA) ); WM( EA,_D );					} /* SET  6,D=(XY+o)  */
/*TODO*///OP(xycb,f3) { _E = SET(6, RM(EA) ); WM( EA,_E );					} /* SET  6,E=(XY+o)  */
/*TODO*///OP(xycb,f4) { _H = SET(6, RM(EA) ); WM( EA,_H );					} /* SET  6,H=(XY+o)  */
/*TODO*///OP(xycb,f5) { _L = SET(6, RM(EA) ); WM( EA,_L );					} /* SET  6,L=(XY+o)  */
/*TODO*///OP(xycb,f6) { WM( EA, SET(6,RM(EA)) );								} /* SET  6,(XY+o)	  */
/*TODO*///OP(xycb,f7) { _A = SET(6, RM(EA) ); WM( EA,_A );					} /* SET  6,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(xycb,f8) { _B = SET(7, RM(EA) ); WM( EA,_B );					} /* SET  7,B=(XY+o)  */
/*TODO*///OP(xycb,f9) { _C = SET(7, RM(EA) ); WM( EA,_C );					} /* SET  7,C=(XY+o)  */
/*TODO*///OP(xycb,fa) { _D = SET(7, RM(EA) ); WM( EA,_D );					} /* SET  7,D=(XY+o)  */
/*TODO*///OP(xycb,fb) { _E = SET(7, RM(EA) ); WM( EA,_E );					} /* SET  7,E=(XY+o)  */
/*TODO*///OP(xycb,fc) { _H = SET(7, RM(EA) ); WM( EA,_H );					} /* SET  7,H=(XY+o)  */
/*TODO*///OP(xycb,fd) { _L = SET(7, RM(EA) ); WM( EA,_L );					} /* SET  7,L=(XY+o)  */
/*TODO*///OP(xycb,fe) { WM( EA, SET(7,RM(EA)) );								} /* SET  7,(XY+o)	  */
/*TODO*///OP(xycb,ff) { _A = SET(7, RM(EA) ); WM( EA,_A );					} /* SET  7,A=(XY+o)  */
/*TODO*///
/*TODO*///OP(illegal,1) {
/*TODO*///	logerror("Z80 #%d ill. opcode $%02x $%02x\n",
/*TODO*///			cpu_getactivecpu(), cpu_readop((_PCD-1)&0xffff), cpu_readop(_PCD));
/*TODO*///}
/*TODO*///
/*TODO*////**********************************************************
/*TODO*/// * IX register related opcodes (DD prefix)
/*TODO*/// **********************************************************/
/*TODO*///OP(dd,00) { illegal_1(); op_00();									} /* DB   DD		  */
/*TODO*///OP(dd,01) { illegal_1(); op_01();									} /* DB   DD		  */
/*TODO*///OP(dd,02) { illegal_1(); op_02();									} /* DB   DD		  */
/*TODO*///OP(dd,03) { illegal_1(); op_03();									} /* DB   DD		  */
/*TODO*///OP(dd,04) { illegal_1(); op_04();									} /* DB   DD		  */
/*TODO*///OP(dd,05) { illegal_1(); op_05();									} /* DB   DD		  */
/*TODO*///OP(dd,06) { illegal_1(); op_06();									} /* DB   DD		  */
/*TODO*///OP(dd,07) { illegal_1(); op_07();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,08) { illegal_1(); op_08();									} /* DB   DD		  */
/*TODO*///OP(dd,09) { _R++; ADD16(IX,BC); 									} /* ADD  IX,BC 	  */
/*TODO*///OP(dd,0a) { illegal_1(); op_0a();									} /* DB   DD		  */
/*TODO*///OP(dd,0b) { illegal_1(); op_0b();									} /* DB   DD		  */
/*TODO*///OP(dd,0c) { illegal_1(); op_0c();									} /* DB   DD		  */
/*TODO*///OP(dd,0d) { illegal_1(); op_0d();									} /* DB   DD		  */
/*TODO*///OP(dd,0e) { illegal_1(); op_0e();									} /* DB   DD		  */
/*TODO*///OP(dd,0f) { illegal_1(); op_0f();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,10) { illegal_1(); op_10();									} /* DB   DD		  */
/*TODO*///OP(dd,11) { illegal_1(); op_11();									} /* DB   DD		  */
/*TODO*///OP(dd,12) { illegal_1(); op_12();									} /* DB   DD		  */
/*TODO*///OP(dd,13) { illegal_1(); op_13();									} /* DB   DD		  */
/*TODO*///OP(dd,14) { illegal_1(); op_14();									} /* DB   DD		  */
/*TODO*///OP(dd,15) { illegal_1(); op_15();									} /* DB   DD		  */
/*TODO*///OP(dd,16) { illegal_1(); op_16();									} /* DB   DD		  */
/*TODO*///OP(dd,17) { illegal_1(); op_17();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,18) { illegal_1(); op_18();									} /* DB   DD		  */
/*TODO*///OP(dd,19) { _R++; ADD16(IX,DE); 									} /* ADD  IX,DE 	  */
/*TODO*///OP(dd,1a) { illegal_1(); op_1a();									} /* DB   DD		  */
/*TODO*///OP(dd,1b) { illegal_1(); op_1b();									} /* DB   DD		  */
/*TODO*///OP(dd,1c) { illegal_1(); op_1c();									} /* DB   DD		  */
/*TODO*///OP(dd,1d) { illegal_1(); op_1d();									} /* DB   DD		  */
/*TODO*///OP(dd,1e) { illegal_1(); op_1e();									} /* DB   DD		  */
/*TODO*///OP(dd,1f) { illegal_1(); op_1f();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,20) { illegal_1(); op_20();									} /* DB   DD		  */
/*TODO*///OP(dd,21) { _R++; _IX = ARG16();									} /* LD   IX,w		  */
/*TODO*///OP(dd,22) { _R++; EA = ARG16(); WM16( EA, &Z80.IX );				} /* LD   (w),IX	  */
/*TODO*///OP(dd,23) { _R++; _IX++;											} /* INC  IX		  */
/*TODO*///OP(dd,24) { _R++; _HX = INC(_HX);									} /* INC  HX		  */
/*TODO*///OP(dd,25) { _R++; _HX = DEC(_HX);									} /* DEC  HX		  */
/*TODO*///OP(dd,26) { _R++; _HX = ARG();										} /* LD   HX,n		  */
/*TODO*///OP(dd,27) { illegal_1(); op_27();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,28) { illegal_1(); op_28();									} /* DB   DD		  */
/*TODO*///OP(dd,29) { _R++; ADD16(IX,IX); 									} /* ADD  IX,IX 	  */
/*TODO*///OP(dd,2a) { _R++; EA = ARG16(); RM16( EA, &Z80.IX );				} /* LD   IX,(w)	  */
/*TODO*///OP(dd,2b) { _R++; _IX--;											} /* DEC  IX		  */
/*TODO*///OP(dd,2c) { _R++; _LX = INC(_LX);									} /* INC  LX		  */
/*TODO*///OP(dd,2d) { _R++; _LX = DEC(_LX);									} /* DEC  LX		  */
/*TODO*///OP(dd,2e) { _R++; _LX = ARG();										} /* LD   LX,n		  */
/*TODO*///OP(dd,2f) { illegal_1(); op_2f();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,30) { illegal_1(); op_30();									} /* DB   DD		  */
/*TODO*///OP(dd,31) { illegal_1(); op_31();									} /* DB   DD		  */
/*TODO*///OP(dd,32) { illegal_1(); op_32();									} /* DB   DD		  */
/*TODO*///OP(dd,33) { illegal_1(); op_33();									} /* DB   DD		  */
/*TODO*///OP(dd,34) { _R++; EAX; WM( EA, INC(RM(EA)) );						} /* INC  (IX+o)	  */
/*TODO*///OP(dd,35) { _R++; EAX; WM( EA, DEC(RM(EA)) );						} /* DEC  (IX+o)	  */
/*TODO*///OP(dd,36) { _R++; EAX; WM( EA, ARG() ); 							} /* LD   (IX+o),n	  */
/*TODO*///OP(dd,37) { illegal_1(); op_37();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,38) { illegal_1(); op_38();									} /* DB   DD		  */
/*TODO*///OP(dd,39) { _R++; ADD16(IX,SP); 									} /* ADD  IX,SP 	  */
/*TODO*///OP(dd,3a) { illegal_1(); op_3a();									} /* DB   DD		  */
/*TODO*///OP(dd,3b) { illegal_1(); op_3b();									} /* DB   DD		  */
/*TODO*///OP(dd,3c) { illegal_1(); op_3c();									} /* DB   DD		  */
/*TODO*///OP(dd,3d) { illegal_1(); op_3d();									} /* DB   DD		  */
/*TODO*///OP(dd,3e) { illegal_1(); op_3e();									} /* DB   DD		  */
/*TODO*///OP(dd,3f) { illegal_1(); op_3f();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,40) { illegal_1(); op_40();									} /* DB   DD		  */
/*TODO*///OP(dd,41) { illegal_1(); op_41();									} /* DB   DD		  */
/*TODO*///OP(dd,42) { illegal_1(); op_42();									} /* DB   DD		  */
/*TODO*///OP(dd,43) { illegal_1(); op_43();									} /* DB   DD		  */
/*TODO*///OP(dd,44) { _R++; _B = _HX; 										} /* LD   B,HX		  */
/*TODO*///OP(dd,45) { _R++; _B = _LX; 										} /* LD   B,LX		  */
/*TODO*///OP(dd,46) { _R++; EAX; _B = RM(EA); 								} /* LD   B,(IX+o)	  */
/*TODO*///OP(dd,47) { illegal_1(); op_47();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,48) { illegal_1(); op_48();									} /* DB   DD		  */
/*TODO*///OP(dd,49) { illegal_1(); op_49();									} /* DB   DD		  */
/*TODO*///OP(dd,4a) { illegal_1(); op_4a();									} /* DB   DD		  */
/*TODO*///OP(dd,4b) { illegal_1(); op_4b();									} /* DB   DD		  */
/*TODO*///OP(dd,4c) { _R++; _C = _HX; 										} /* LD   C,HX		  */
/*TODO*///OP(dd,4d) { _R++; _C = _LX; 										} /* LD   C,LX		  */
/*TODO*///OP(dd,4e) { _R++; EAX; _C = RM(EA); 								} /* LD   C,(IX+o)	  */
/*TODO*///OP(dd,4f) { illegal_1(); op_4f();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,50) { illegal_1(); op_50();									} /* DB   DD		  */
/*TODO*///OP(dd,51) { illegal_1(); op_51();									} /* DB   DD		  */
/*TODO*///OP(dd,52) { illegal_1(); op_52();									} /* DB   DD		  */
/*TODO*///OP(dd,53) { illegal_1(); op_53();									} /* DB   DD		  */
/*TODO*///OP(dd,54) { _R++; _D = _HX; 										} /* LD   D,HX		  */
/*TODO*///OP(dd,55) { _R++; _D = _LX; 										} /* LD   D,LX		  */
/*TODO*///OP(dd,56) { _R++; EAX; _D = RM(EA); 								} /* LD   D,(IX+o)	  */
/*TODO*///OP(dd,57) { illegal_1(); op_57();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,58) { illegal_1(); op_58();									} /* DB   DD		  */
/*TODO*///OP(dd,59) { illegal_1(); op_59();									} /* DB   DD		  */
/*TODO*///OP(dd,5a) { illegal_1(); op_5a();									} /* DB   DD		  */
/*TODO*///OP(dd,5b) { illegal_1(); op_5b();									} /* DB   DD		  */
/*TODO*///OP(dd,5c) { _R++; _E = _HX; 										} /* LD   E,HX		  */
/*TODO*///OP(dd,5d) { _R++; _E = _LX; 										} /* LD   E,LX		  */
/*TODO*///OP(dd,5e) { _R++; EAX; _E = RM(EA); 								} /* LD   E,(IX+o)	  */
/*TODO*///OP(dd,5f) { illegal_1(); op_5f();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,60) { _R++; _HX = _B; 										} /* LD   HX,B		  */
/*TODO*///OP(dd,61) { _R++; _HX = _C; 										} /* LD   HX,C		  */
/*TODO*///OP(dd,62) { _R++; _HX = _D; 										} /* LD   HX,D		  */
/*TODO*///OP(dd,63) { _R++; _HX = _E; 										} /* LD   HX,E		  */
/*TODO*///OP(dd,64) { 														} /* LD   HX,HX 	  */
/*TODO*///OP(dd,65) { _R++; _HX = _LX;										} /* LD   HX,LX 	  */
/*TODO*///OP(dd,66) { _R++; EAX; _H = RM(EA); 								} /* LD   H,(IX+o)	  */
/*TODO*///OP(dd,67) { _R++; _HX = _A; 										} /* LD   HX,A		  */
/*TODO*///
/*TODO*///OP(dd,68) { _R++; _LX = _B; 										} /* LD   LX,B		  */
/*TODO*///OP(dd,69) { _R++; _LX = _C; 										} /* LD   LX,C		  */
/*TODO*///OP(dd,6a) { _R++; _LX = _D; 										} /* LD   LX,D		  */
/*TODO*///OP(dd,6b) { _R++; _LX = _E; 										} /* LD   LX,E		  */
/*TODO*///OP(dd,6c) { _R++; _LX = _HX;										} /* LD   LX,HX 	  */
/*TODO*///OP(dd,6d) { 														} /* LD   LX,LX 	  */
/*TODO*///OP(dd,6e) { _R++; EAX; _L = RM(EA); 								} /* LD   L,(IX+o)	  */
/*TODO*///OP(dd,6f) { _R++; _LX = _A; 										} /* LD   LX,A		  */
/*TODO*///
/*TODO*///OP(dd,70) { _R++; EAX; WM( EA, _B );								} /* LD   (IX+o),B	  */
/*TODO*///OP(dd,71) { _R++; EAX; WM( EA, _C );								} /* LD   (IX+o),C	  */
/*TODO*///OP(dd,72) { _R++; EAX; WM( EA, _D );								} /* LD   (IX+o),D	  */
/*TODO*///OP(dd,73) { _R++; EAX; WM( EA, _E );								} /* LD   (IX+o),E	  */
/*TODO*///OP(dd,74) { _R++; EAX; WM( EA, _H );								} /* LD   (IX+o),H	  */
/*TODO*///OP(dd,75) { _R++; EAX; WM( EA, _L );								} /* LD   (IX+o),L	  */
/*TODO*///OP(dd,76) { illegal_1(); op_76();									}		  /* DB   DD		  */
/*TODO*///OP(dd,77) { _R++; EAX; WM( EA, _A );								} /* LD   (IX+o),A	  */
/*TODO*///
/*TODO*///OP(dd,78) { illegal_1(); op_78();									} /* DB   DD		  */
/*TODO*///OP(dd,79) { illegal_1(); op_79();									} /* DB   DD		  */
/*TODO*///OP(dd,7a) { illegal_1(); op_7a();									} /* DB   DD		  */
/*TODO*///OP(dd,7b) { illegal_1(); op_7b();									} /* DB   DD		  */
/*TODO*///OP(dd,7c) { _R++; _A = _HX; 										} /* LD   A,HX		  */
/*TODO*///OP(dd,7d) { _R++; _A = _LX; 										} /* LD   A,LX		  */
/*TODO*///OP(dd,7e) { _R++; EAX; _A = RM(EA); 								} /* LD   A,(IX+o)	  */
/*TODO*///OP(dd,7f) { illegal_1(); op_7f();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,80) { illegal_1(); op_80();									} /* DB   DD		  */
/*TODO*///OP(dd,81) { illegal_1(); op_81();									} /* DB   DD		  */
/*TODO*///OP(dd,82) { illegal_1(); op_82();									} /* DB   DD		  */
/*TODO*///OP(dd,83) { illegal_1(); op_83();									} /* DB   DD		  */
/*TODO*///OP(dd,84) { _R++; ADD(_HX); 										} /* ADD  A,HX		  */
/*TODO*///OP(dd,85) { _R++; ADD(_LX); 										} /* ADD  A,LX		  */
/*TODO*///OP(dd,86) { _R++; EAX; ADD(RM(EA)); 								} /* ADD  A,(IX+o)	  */
/*TODO*///OP(dd,87) { illegal_1(); op_87();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,88) { illegal_1(); op_88();									} /* DB   DD		  */
/*TODO*///OP(dd,89) { illegal_1(); op_89();									} /* DB   DD		  */
/*TODO*///OP(dd,8a) { illegal_1(); op_8a();									} /* DB   DD		  */
/*TODO*///OP(dd,8b) { illegal_1(); op_8b();									} /* DB   DD		  */
/*TODO*///OP(dd,8c) { _R++; ADC(_HX); 										} /* ADC  A,HX		  */
/*TODO*///OP(dd,8d) { _R++; ADC(_LX); 										} /* ADC  A,LX		  */
/*TODO*///OP(dd,8e) { _R++; EAX; ADC(RM(EA)); 								} /* ADC  A,(IX+o)	  */
/*TODO*///OP(dd,8f) { illegal_1(); op_8f();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,90) { illegal_1(); op_90();									} /* DB   DD		  */
/*TODO*///OP(dd,91) { illegal_1(); op_91();									} /* DB   DD		  */
/*TODO*///OP(dd,92) { illegal_1(); op_92();									} /* DB   DD		  */
/*TODO*///OP(dd,93) { illegal_1(); op_93();									} /* DB   DD		  */
/*TODO*///OP(dd,94) { _R++; SUB(_HX); 										} /* SUB  HX		  */
/*TODO*///OP(dd,95) { _R++; SUB(_LX); 										} /* SUB  LX		  */
/*TODO*///OP(dd,96) { _R++; EAX; SUB(RM(EA)); 								} /* SUB  (IX+o)	  */
/*TODO*///OP(dd,97) { illegal_1(); op_97();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,98) { illegal_1(); op_98();									} /* DB   DD		  */
/*TODO*///OP(dd,99) { illegal_1(); op_99();									} /* DB   DD		  */
/*TODO*///OP(dd,9a) { illegal_1(); op_9a();									} /* DB   DD		  */
/*TODO*///OP(dd,9b) { illegal_1(); op_9b();									} /* DB   DD		  */
/*TODO*///OP(dd,9c) { _R++; SBC(_HX); 										} /* SBC  A,HX		  */
/*TODO*///OP(dd,9d) { _R++; SBC(_LX); 										} /* SBC  A,LX		  */
/*TODO*///OP(dd,9e) { _R++; EAX; SBC(RM(EA)); 								} /* SBC  A,(IX+o)	  */
/*TODO*///OP(dd,9f) { illegal_1(); op_9f();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,a0) { illegal_1(); op_a0();									} /* DB   DD		  */
/*TODO*///OP(dd,a1) { illegal_1(); op_a1();									} /* DB   DD		  */
/*TODO*///OP(dd,a2) { illegal_1(); op_a2();									} /* DB   DD		  */
/*TODO*///OP(dd,a3) { illegal_1(); op_a3();									} /* DB   DD		  */
/*TODO*///OP(dd,a4) { _R++; AND(_HX); 										} /* AND  HX		  */
/*TODO*///OP(dd,a5) { _R++; AND(_LX); 										} /* AND  LX		  */
/*TODO*///OP(dd,a6) { _R++; EAX; AND(RM(EA)); 								} /* AND  (IX+o)	  */
/*TODO*///OP(dd,a7) { illegal_1(); op_a7();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,a8) { illegal_1(); op_a8();									} /* DB   DD		  */
/*TODO*///OP(dd,a9) { illegal_1(); op_a9();									} /* DB   DD		  */
/*TODO*///OP(dd,aa) { illegal_1(); op_aa();									} /* DB   DD		  */
/*TODO*///OP(dd,ab) { illegal_1(); op_ab();									} /* DB   DD		  */
/*TODO*///OP(dd,ac) { _R++; XOR(_HX); 										} /* XOR  HX		  */
/*TODO*///OP(dd,ad) { _R++; XOR(_LX); 										} /* XOR  LX		  */
/*TODO*///OP(dd,ae) { _R++; EAX; XOR(RM(EA)); 								} /* XOR  (IX+o)	  */
/*TODO*///OP(dd,af) { illegal_1(); op_af();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,b0) { illegal_1(); op_b0();									} /* DB   DD		  */
/*TODO*///OP(dd,b1) { illegal_1(); op_b1();									} /* DB   DD		  */
/*TODO*///OP(dd,b2) { illegal_1(); op_b2();									} /* DB   DD		  */
/*TODO*///OP(dd,b3) { illegal_1(); op_b3();									} /* DB   DD		  */
/*TODO*///OP(dd,b4) { _R++; OR(_HX);											} /* OR   HX		  */
/*TODO*///OP(dd,b5) { _R++; OR(_LX);											} /* OR   LX		  */
/*TODO*///OP(dd,b6) { _R++; EAX; OR(RM(EA));									} /* OR   (IX+o)	  */
/*TODO*///OP(dd,b7) { illegal_1(); op_b7();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,b8) { illegal_1(); op_b8();									} /* DB   DD		  */
/*TODO*///OP(dd,b9) { illegal_1(); op_b9();									} /* DB   DD		  */
/*TODO*///OP(dd,ba) { illegal_1(); op_ba();									} /* DB   DD		  */
/*TODO*///OP(dd,bb) { illegal_1(); op_bb();									} /* DB   DD		  */
/*TODO*///OP(dd,bc) { _R++; CP(_HX);											} /* CP   HX		  */
/*TODO*///OP(dd,bd) { _R++; CP(_LX);											} /* CP   LX		  */
/*TODO*///OP(dd,be) { _R++; EAX; CP(RM(EA));									} /* CP   (IX+o)	  */
/*TODO*///OP(dd,bf) { illegal_1(); op_bf();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,c0) { illegal_1(); op_c0();									} /* DB   DD		  */
/*TODO*///OP(dd,c1) { illegal_1(); op_c1();									} /* DB   DD		  */
/*TODO*///OP(dd,c2) { illegal_1(); op_c2();									} /* DB   DD		  */
/*TODO*///OP(dd,c3) { illegal_1(); op_c3();									} /* DB   DD		  */
/*TODO*///OP(dd,c4) { illegal_1(); op_c4();									} /* DB   DD		  */
/*TODO*///OP(dd,c5) { illegal_1(); op_c5();									} /* DB   DD		  */
/*TODO*///OP(dd,c6) { illegal_1(); op_c6();									} /* DB   DD		  */
/*TODO*///OP(dd,c7) { illegal_1(); op_c7();									}		  /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,c8) { illegal_1(); op_c8();									} /* DB   DD		  */
/*TODO*///OP(dd,c9) { illegal_1(); op_c9();									} /* DB   DD		  */
/*TODO*///OP(dd,ca) { illegal_1(); op_ca();									} /* DB   DD		  */
/*TODO*///OP(dd,cb) { _R++; EAX; EXEC(xycb,ARG());							} /* **   DD CB xx	  */
/*TODO*///OP(dd,cc) { illegal_1(); op_cc();									} /* DB   DD		  */
/*TODO*///OP(dd,cd) { illegal_1(); op_cd();									} /* DB   DD		  */
/*TODO*///OP(dd,ce) { illegal_1(); op_ce();									} /* DB   DD		  */
/*TODO*///OP(dd,cf) { illegal_1(); op_cf();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,d0) { illegal_1(); op_d0();									} /* DB   DD		  */
/*TODO*///OP(dd,d1) { illegal_1(); op_d1();									} /* DB   DD		  */
/*TODO*///OP(dd,d2) { illegal_1(); op_d2();									} /* DB   DD		  */
/*TODO*///OP(dd,d3) { illegal_1(); op_d3();									} /* DB   DD		  */
/*TODO*///OP(dd,d4) { illegal_1(); op_d4();									} /* DB   DD		  */
/*TODO*///OP(dd,d5) { illegal_1(); op_d5();									} /* DB   DD		  */
/*TODO*///OP(dd,d6) { illegal_1(); op_d6();									} /* DB   DD		  */
/*TODO*///OP(dd,d7) { illegal_1(); op_d7();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,d8) { illegal_1(); op_d8();									} /* DB   DD		  */
/*TODO*///OP(dd,d9) { illegal_1(); op_d9();									} /* DB   DD		  */
/*TODO*///OP(dd,da) { illegal_1(); op_da();									} /* DB   DD		  */
/*TODO*///OP(dd,db) { illegal_1(); op_db();									} /* DB   DD		  */
/*TODO*///OP(dd,dc) { illegal_1(); op_dc();									} /* DB   DD		  */
/*TODO*///OP(dd,dd) { illegal_1(); op_dd();									} /* DB   DD		  */
/*TODO*///OP(dd,de) { illegal_1(); op_de();									} /* DB   DD		  */
/*TODO*///OP(dd,df) { illegal_1(); op_df();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,e0) { illegal_1(); op_e0();									} /* DB   DD		  */
/*TODO*///OP(dd,e1) { _R++; POP(IX);											} /* POP  IX		  */
/*TODO*///OP(dd,e2) { illegal_1(); op_e2();									} /* DB   DD		  */
/*TODO*///OP(dd,e3) { _R++; EXSP(IX); 										} /* EX   (SP),IX	  */
/*TODO*///OP(dd,e4) { illegal_1(); op_e4();									} /* DB   DD		  */
/*TODO*///OP(dd,e5) { _R++; PUSH( IX );										} /* PUSH IX		  */
/*TODO*///OP(dd,e6) { illegal_1(); op_e6();									} /* DB   DD		  */
/*TODO*///OP(dd,e7) { illegal_1(); op_e7();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,e8) { illegal_1(); op_e8();									} /* DB   DD		  */
/*TODO*///OP(dd,e9) { _R++; _PC = _IX; change_pc16(_PCD); 					} /* JP   (IX)		  */
/*TODO*///OP(dd,ea) { illegal_1(); op_ea();									} /* DB   DD		  */
/*TODO*///OP(dd,eb) { illegal_1(); op_eb();									} /* DB   DD		  */
/*TODO*///OP(dd,ec) { illegal_1(); op_ec();									} /* DB   DD		  */
/*TODO*///OP(dd,ed) { illegal_1(); op_ed();									} /* DB   DD		  */
/*TODO*///OP(dd,ee) { illegal_1(); op_ee();									} /* DB   DD		  */
/*TODO*///OP(dd,ef) { illegal_1(); op_ef();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,f0) { illegal_1(); op_f0();									} /* DB   DD		  */
/*TODO*///OP(dd,f1) { illegal_1(); op_f1();									} /* DB   DD		  */
/*TODO*///OP(dd,f2) { illegal_1(); op_f2();									} /* DB   DD		  */
/*TODO*///OP(dd,f3) { illegal_1(); op_f3();									} /* DB   DD		  */
/*TODO*///OP(dd,f4) { illegal_1(); op_f4();									} /* DB   DD		  */
/*TODO*///OP(dd,f5) { illegal_1(); op_f5();									} /* DB   DD		  */
/*TODO*///OP(dd,f6) { illegal_1(); op_f6();									} /* DB   DD		  */
/*TODO*///OP(dd,f7) { illegal_1(); op_f7();									} /* DB   DD		  */
/*TODO*///
/*TODO*///OP(dd,f8) { illegal_1(); op_f8();									} /* DB   DD		  */
/*TODO*///OP(dd,f9) { _R++; _SP = _IX;										} /* LD   SP,IX 	  */
/*TODO*///OP(dd,fa) { illegal_1(); op_fa();									} /* DB   DD		  */
/*TODO*///OP(dd,fb) { illegal_1(); op_fb();									} /* DB   DD		  */
/*TODO*///OP(dd,fc) { illegal_1(); op_fc();									} /* DB   DD		  */
/*TODO*///OP(dd,fd) { illegal_1(); op_fd();									} /* DB   DD		  */
/*TODO*///OP(dd,fe) { illegal_1(); op_fe();									} /* DB   DD		  */
/*TODO*///OP(dd,ff) { illegal_1(); op_ff();									} /* DB   DD		  */
/*TODO*///
/*TODO*////**********************************************************
/*TODO*/// * IY register related opcodes (FD prefix)
/*TODO*/// **********************************************************/
/*TODO*///OP(fd,00) { illegal_1(); op_00();									} /* DB   FD		  */
/*TODO*///OP(fd,01) { illegal_1(); op_01();									} /* DB   FD		  */
/*TODO*///OP(fd,02) { illegal_1(); op_02();									} /* DB   FD		  */
/*TODO*///OP(fd,03) { illegal_1(); op_03();									} /* DB   FD		  */
/*TODO*///OP(fd,04) { illegal_1(); op_04();									} /* DB   FD		  */
/*TODO*///OP(fd,05) { illegal_1(); op_05();									} /* DB   FD		  */
/*TODO*///OP(fd,06) { illegal_1(); op_06();									} /* DB   FD		  */
/*TODO*///OP(fd,07) { illegal_1(); op_07();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,08) { illegal_1(); op_08();									} /* DB   FD		  */
/*TODO*///OP(fd,09) { _R++; ADD16(IY,BC); 									} /* ADD  IY,BC 	  */
/*TODO*///OP(fd,0a) { illegal_1(); op_0a();									} /* DB   FD		  */
/*TODO*///OP(fd,0b) { illegal_1(); op_0b();									} /* DB   FD		  */
/*TODO*///OP(fd,0c) { illegal_1(); op_0c();									} /* DB   FD		  */
/*TODO*///OP(fd,0d) { illegal_1(); op_0d();									} /* DB   FD		  */
/*TODO*///OP(fd,0e) { illegal_1(); op_0e();									} /* DB   FD		  */
/*TODO*///OP(fd,0f) { illegal_1(); op_0f();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,10) { illegal_1(); op_10();									} /* DB   FD		  */
/*TODO*///OP(fd,11) { illegal_1(); op_11();									} /* DB   FD		  */
/*TODO*///OP(fd,12) { illegal_1(); op_12();									} /* DB   FD		  */
/*TODO*///OP(fd,13) { illegal_1(); op_13();									} /* DB   FD		  */
/*TODO*///OP(fd,14) { illegal_1(); op_14();									} /* DB   FD		  */
/*TODO*///OP(fd,15) { illegal_1(); op_15();									} /* DB   FD		  */
/*TODO*///OP(fd,16) { illegal_1(); op_16();									} /* DB   FD		  */
/*TODO*///OP(fd,17) { illegal_1(); op_17();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,18) { illegal_1(); op_18();									} /* DB   FD		  */
/*TODO*///OP(fd,19) { _R++; ADD16(IY,DE); 									} /* ADD  IY,DE 	  */
/*TODO*///OP(fd,1a) { illegal_1(); op_1a();									} /* DB   FD		  */
/*TODO*///OP(fd,1b) { illegal_1(); op_1b();									} /* DB   FD		  */
/*TODO*///OP(fd,1c) { illegal_1(); op_1c();									} /* DB   FD		  */
/*TODO*///OP(fd,1d) { illegal_1(); op_1d();									} /* DB   FD		  */
/*TODO*///OP(fd,1e) { illegal_1(); op_1e();									} /* DB   FD		  */
/*TODO*///OP(fd,1f) { illegal_1(); op_1f();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,20) { illegal_1(); op_20();									} /* DB   FD		  */
/*TODO*///OP(fd,21) { _R++; _IY = ARG16();									} /* LD   IY,w		  */
/*TODO*///OP(fd,22) { _R++; EA = ARG16(); WM16( EA, &Z80.IY );				} /* LD   (w),IY	  */
/*TODO*///OP(fd,23) { _R++; _IY++;											} /* INC  IY		  */
/*TODO*///OP(fd,24) { _R++; _HY = INC(_HY);									} /* INC  HY		  */
/*TODO*///OP(fd,25) { _R++; _HY = DEC(_HY);									} /* DEC  HY		  */
/*TODO*///OP(fd,26) { _R++; _HY = ARG();										} /* LD   HY,n		  */
/*TODO*///OP(fd,27) { illegal_1(); op_27();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,28) { illegal_1(); op_28();									} /* DB   FD		  */
/*TODO*///OP(fd,29) { _R++; ADD16(IY,IY); 									} /* ADD  IY,IY 	  */
/*TODO*///OP(fd,2a) { _R++; EA = ARG16(); RM16( EA, &Z80.IY );				} /* LD   IY,(w)	  */
/*TODO*///OP(fd,2b) { _R++; _IY--;											} /* DEC  IY		  */
/*TODO*///OP(fd,2c) { _R++; _LY = INC(_LY);									} /* INC  LY		  */
/*TODO*///OP(fd,2d) { _R++; _LY = DEC(_LY);									} /* DEC  LY		  */
/*TODO*///OP(fd,2e) { _R++; _LY = ARG();										} /* LD   LY,n		  */
/*TODO*///OP(fd,2f) { illegal_1(); op_2f();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,30) { illegal_1(); op_30();									} /* DB   FD		  */
/*TODO*///OP(fd,31) { illegal_1(); op_31();									} /* DB   FD		  */
/*TODO*///OP(fd,32) { illegal_1(); op_32();									} /* DB   FD		  */
/*TODO*///OP(fd,33) { illegal_1(); op_33();									} /* DB   FD		  */
/*TODO*///OP(fd,34) { _R++; EAY; WM( EA, INC(RM(EA)) );						} /* INC  (IY+o)	  */
/*TODO*///OP(fd,35) { _R++; EAY; WM( EA, DEC(RM(EA)) );						} /* DEC  (IY+o)	  */
/*TODO*///OP(fd,36) { _R++; EAY; WM( EA, ARG() ); 							} /* LD   (IY+o),n	  */
/*TODO*///OP(fd,37) { illegal_1(); op_37();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,38) { illegal_1(); op_38();									} /* DB   FD		  */
/*TODO*///OP(fd,39) { _R++; ADD16(IY,SP); 									} /* ADD  IY,SP 	  */
/*TODO*///OP(fd,3a) { illegal_1(); op_3a();									} /* DB   FD		  */
/*TODO*///OP(fd,3b) { illegal_1(); op_3b();									} /* DB   FD		  */
/*TODO*///OP(fd,3c) { illegal_1(); op_3c();									} /* DB   FD		  */
/*TODO*///OP(fd,3d) { illegal_1(); op_3d();									} /* DB   FD		  */
/*TODO*///OP(fd,3e) { illegal_1(); op_3e();									} /* DB   FD		  */
/*TODO*///OP(fd,3f) { illegal_1(); op_3f();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,40) { illegal_1(); op_40();									} /* DB   FD		  */
/*TODO*///OP(fd,41) { illegal_1(); op_41();									} /* DB   FD		  */
/*TODO*///OP(fd,42) { illegal_1(); op_42();									} /* DB   FD		  */
/*TODO*///OP(fd,43) { illegal_1(); op_43();									} /* DB   FD		  */
/*TODO*///OP(fd,44) { _R++; _B = _HY; 										} /* LD   B,HY		  */
/*TODO*///OP(fd,45) { _R++; _B = _LY; 										} /* LD   B,LY		  */
/*TODO*///OP(fd,46) { _R++; EAY; _B = RM(EA); 								} /* LD   B,(IY+o)	  */
/*TODO*///OP(fd,47) { illegal_1(); op_47();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,48) { illegal_1(); op_48();									} /* DB   FD		  */
/*TODO*///OP(fd,49) { illegal_1(); op_49();									} /* DB   FD		  */
/*TODO*///OP(fd,4a) { illegal_1(); op_4a();									} /* DB   FD		  */
/*TODO*///OP(fd,4b) { illegal_1(); op_4b();									} /* DB   FD		  */
/*TODO*///OP(fd,4c) { _R++; _C = _HY; 										} /* LD   C,HY		  */
/*TODO*///OP(fd,4d) { _R++; _C = _LY; 										} /* LD   C,LY		  */
/*TODO*///OP(fd,4e) { _R++; EAY; _C = RM(EA); 								} /* LD   C,(IY+o)	  */
/*TODO*///OP(fd,4f) { illegal_1(); op_4f();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,50) { illegal_1(); op_50();									} /* DB   FD		  */
/*TODO*///OP(fd,51) { illegal_1(); op_51();									} /* DB   FD		  */
/*TODO*///OP(fd,52) { illegal_1(); op_52();									} /* DB   FD		  */
/*TODO*///OP(fd,53) { illegal_1(); op_53();									} /* DB   FD		  */
/*TODO*///OP(fd,54) { _R++; _D = _HY; 										} /* LD   D,HY		  */
/*TODO*///OP(fd,55) { _R++; _D = _LY; 										} /* LD   D,LY		  */
/*TODO*///OP(fd,56) { _R++; EAY; _D = RM(EA); 								} /* LD   D,(IY+o)	  */
/*TODO*///OP(fd,57) { illegal_1(); op_57();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,58) { illegal_1(); op_58();									} /* DB   FD		  */
/*TODO*///OP(fd,59) { illegal_1(); op_59();									} /* DB   FD		  */
/*TODO*///OP(fd,5a) { illegal_1(); op_5a();									} /* DB   FD		  */
/*TODO*///OP(fd,5b) { illegal_1(); op_5b();									} /* DB   FD		  */
/*TODO*///OP(fd,5c) { _R++; _E = _HY; 										} /* LD   E,HY		  */
/*TODO*///OP(fd,5d) { _R++; _E = _LY; 										} /* LD   E,LY		  */
/*TODO*///OP(fd,5e) { _R++; EAY; _E = RM(EA); 								} /* LD   E,(IY+o)	  */
/*TODO*///OP(fd,5f) { illegal_1(); op_5f();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,60) { _R++; _HY = _B; 										} /* LD   HY,B		  */
/*TODO*///OP(fd,61) { _R++; _HY = _C; 										} /* LD   HY,C		  */
/*TODO*///OP(fd,62) { _R++; _HY = _D; 										} /* LD   HY,D		  */
/*TODO*///OP(fd,63) { _R++; _HY = _E; 										} /* LD   HY,E		  */
/*TODO*///OP(fd,64) { _R++;													} /* LD   HY,HY 	  */
/*TODO*///OP(fd,65) { _R++; _HY = _LY;										} /* LD   HY,LY 	  */
/*TODO*///OP(fd,66) { _R++; EAY; _H = RM(EA); 								} /* LD   H,(IY+o)	  */
/*TODO*///OP(fd,67) { _R++; _HY = _A; 										} /* LD   HY,A		  */
/*TODO*///
/*TODO*///OP(fd,68) { _R++; _LY = _B; 										} /* LD   LY,B		  */
/*TODO*///OP(fd,69) { _R++; _LY = _C; 										} /* LD   LY,C		  */
/*TODO*///OP(fd,6a) { _R++; _LY = _D; 										} /* LD   LY,D		  */
/*TODO*///OP(fd,6b) { _R++; _LY = _E; 										} /* LD   LY,E		  */
/*TODO*///OP(fd,6c) { _R++; _LY = _HY;										} /* LD   LY,HY 	  */
/*TODO*///OP(fd,6d) { _R++;													} /* LD   LY,LY 	  */
/*TODO*///OP(fd,6e) { _R++; EAY; _L = RM(EA); 								} /* LD   L,(IY+o)	  */
/*TODO*///OP(fd,6f) { _R++; _LY = _A; 										} /* LD   LY,A		  */
/*TODO*///
/*TODO*///OP(fd,70) { _R++; EAY; WM( EA, _B );								} /* LD   (IY+o),B	  */
/*TODO*///OP(fd,71) { _R++; EAY; WM( EA, _C );								} /* LD   (IY+o),C	  */
/*TODO*///OP(fd,72) { _R++; EAY; WM( EA, _D );								} /* LD   (IY+o),D	  */
/*TODO*///OP(fd,73) { _R++; EAY; WM( EA, _E );								} /* LD   (IY+o),E	  */
/*TODO*///OP(fd,74) { _R++; EAY; WM( EA, _H );								} /* LD   (IY+o),H	  */
/*TODO*///OP(fd,75) { _R++; EAY; WM( EA, _L );								} /* LD   (IY+o),L	  */
/*TODO*///OP(fd,76) { illegal_1(); op_76();									}		  /* DB   FD		  */
/*TODO*///OP(fd,77) { _R++; EAY; WM( EA, _A );								} /* LD   (IY+o),A	  */
/*TODO*///
/*TODO*///OP(fd,78) { illegal_1(); op_78();									} /* DB   FD		  */
/*TODO*///OP(fd,79) { illegal_1(); op_79();									} /* DB   FD		  */
/*TODO*///OP(fd,7a) { illegal_1(); op_7a();									} /* DB   FD		  */
/*TODO*///OP(fd,7b) { illegal_1(); op_7b();									} /* DB   FD		  */
/*TODO*///OP(fd,7c) { _R++; _A = _HY; 										} /* LD   A,HY		  */
/*TODO*///OP(fd,7d) { _R++; _A = _LY; 										} /* LD   A,LY		  */
/*TODO*///OP(fd,7e) { _R++; EAY; _A = RM(EA); 								} /* LD   A,(IY+o)	  */
/*TODO*///OP(fd,7f) { illegal_1(); op_7f();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,80) { illegal_1(); op_80();									} /* DB   FD		  */
/*TODO*///OP(fd,81) { illegal_1(); op_81();									} /* DB   FD		  */
/*TODO*///OP(fd,82) { illegal_1(); op_82();									} /* DB   FD		  */
/*TODO*///OP(fd,83) { illegal_1(); op_83();									} /* DB   FD		  */
/*TODO*///OP(fd,84) { _R++; ADD(_HY); 										} /* ADD  A,HY		  */
/*TODO*///OP(fd,85) { _R++; ADD(_LY); 										} /* ADD  A,LY		  */
/*TODO*///OP(fd,86) { _R++; EAY; ADD(RM(EA)); 								} /* ADD  A,(IY+o)	  */
/*TODO*///OP(fd,87) { illegal_1(); op_87();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,88) { illegal_1(); op_88();									} /* DB   FD		  */
/*TODO*///OP(fd,89) { illegal_1(); op_89();									} /* DB   FD		  */
/*TODO*///OP(fd,8a) { illegal_1(); op_8a();									} /* DB   FD		  */
/*TODO*///OP(fd,8b) { illegal_1(); op_8b();									} /* DB   FD		  */
/*TODO*///OP(fd,8c) { _R++; ADC(_HY); 										} /* ADC  A,HY		  */
/*TODO*///OP(fd,8d) { _R++; ADC(_LY); 										} /* ADC  A,LY		  */
/*TODO*///OP(fd,8e) { _R++; EAY; ADC(RM(EA)); 								} /* ADC  A,(IY+o)	  */
/*TODO*///OP(fd,8f) { illegal_1(); op_8f();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,90) { illegal_1(); op_90();									} /* DB   FD		  */
/*TODO*///OP(fd,91) { illegal_1(); op_91();									} /* DB   FD		  */
/*TODO*///OP(fd,92) { illegal_1(); op_92();									} /* DB   FD		  */
/*TODO*///OP(fd,93) { illegal_1(); op_93();									} /* DB   FD		  */
/*TODO*///OP(fd,94) { _R++; SUB(_HY); 										} /* SUB  HY		  */
/*TODO*///OP(fd,95) { _R++; SUB(_LY); 										} /* SUB  LY		  */
/*TODO*///OP(fd,96) { _R++; EAY; SUB(RM(EA)); 								} /* SUB  (IY+o)	  */
/*TODO*///OP(fd,97) { illegal_1(); op_97();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,98) { illegal_1(); op_98();									} /* DB   FD		  */
/*TODO*///OP(fd,99) { illegal_1(); op_99();									} /* DB   FD		  */
/*TODO*///OP(fd,9a) { illegal_1(); op_9a();									} /* DB   FD		  */
/*TODO*///OP(fd,9b) { illegal_1(); op_9b();									} /* DB   FD		  */
/*TODO*///OP(fd,9c) { _R++; SBC(_HY); 										} /* SBC  A,HY		  */
/*TODO*///OP(fd,9d) { _R++; SBC(_LY); 										} /* SBC  A,LY		  */
/*TODO*///OP(fd,9e) { _R++; EAY; SBC(RM(EA)); 								} /* SBC  A,(IY+o)	  */
/*TODO*///OP(fd,9f) { illegal_1(); op_9f();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,a0) { illegal_1(); op_a0();									} /* DB   FD		  */
/*TODO*///OP(fd,a1) { illegal_1(); op_a1();									} /* DB   FD		  */
/*TODO*///OP(fd,a2) { illegal_1(); op_a2();									} /* DB   FD		  */
/*TODO*///OP(fd,a3) { illegal_1(); op_a3();									} /* DB   FD		  */
/*TODO*///OP(fd,a4) { _R++; AND(_HY); 										} /* AND  HY		  */
/*TODO*///OP(fd,a5) { _R++; AND(_LY); 										} /* AND  LY		  */
/*TODO*///OP(fd,a6) { _R++; EAY; AND(RM(EA)); 								} /* AND  (IY+o)	  */
/*TODO*///OP(fd,a7) { illegal_1(); op_a7();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,a8) { illegal_1(); op_a8();									} /* DB   FD		  */
/*TODO*///OP(fd,a9) { illegal_1(); op_a9();									} /* DB   FD		  */
/*TODO*///OP(fd,aa) { illegal_1(); op_aa();									} /* DB   FD		  */
/*TODO*///OP(fd,ab) { illegal_1(); op_ab();									} /* DB   FD		  */
/*TODO*///OP(fd,ac) { _R++; XOR(_HY); 										} /* XOR  HY		  */
/*TODO*///OP(fd,ad) { _R++; XOR(_LY); 										} /* XOR  LY		  */
/*TODO*///OP(fd,ae) { _R++; EAY; XOR(RM(EA)); 								} /* XOR  (IY+o)	  */
/*TODO*///OP(fd,af) { illegal_1(); op_af();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,b0) { illegal_1(); op_b0();									} /* DB   FD		  */
/*TODO*///OP(fd,b1) { illegal_1(); op_b1();									} /* DB   FD		  */
/*TODO*///OP(fd,b2) { illegal_1(); op_b2();									} /* DB   FD		  */
/*TODO*///OP(fd,b3) { illegal_1(); op_b3();									} /* DB   FD		  */
/*TODO*///OP(fd,b4) { _R++; OR(_HY);											} /* OR   HY		  */
/*TODO*///OP(fd,b5) { _R++; OR(_LY);											} /* OR   LY		  */
/*TODO*///OP(fd,b6) { _R++; EAY; OR(RM(EA));									} /* OR   (IY+o)	  */
/*TODO*///OP(fd,b7) { illegal_1(); op_b7();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,b8) { illegal_1(); op_b8();									} /* DB   FD		  */
/*TODO*///OP(fd,b9) { illegal_1(); op_b9();									} /* DB   FD		  */
/*TODO*///OP(fd,ba) { illegal_1(); op_ba();									} /* DB   FD		  */
/*TODO*///OP(fd,bb) { illegal_1(); op_bb();									} /* DB   FD		  */
/*TODO*///OP(fd,bc) { _R++; CP(_HY);											} /* CP   HY		  */
/*TODO*///OP(fd,bd) { _R++; CP(_LY);											} /* CP   LY		  */
/*TODO*///OP(fd,be) { _R++; EAY; CP(RM(EA));									} /* CP   (IY+o)	  */
/*TODO*///OP(fd,bf) { illegal_1(); op_bf();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,c0) { illegal_1(); op_c0();									} /* DB   FD		  */
/*TODO*///OP(fd,c1) { illegal_1(); op_c1();									} /* DB   FD		  */
/*TODO*///OP(fd,c2) { illegal_1(); op_c2();									} /* DB   FD		  */
/*TODO*///OP(fd,c3) { illegal_1(); op_c3();									} /* DB   FD		  */
/*TODO*///OP(fd,c4) { illegal_1(); op_c4();									} /* DB   FD		  */
/*TODO*///OP(fd,c5) { illegal_1(); op_c5();									} /* DB   FD		  */
/*TODO*///OP(fd,c6) { illegal_1(); op_c6();									} /* DB   FD		  */
/*TODO*///OP(fd,c7) { illegal_1(); op_c7();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,c8) { illegal_1(); op_c8();									} /* DB   FD		  */
/*TODO*///OP(fd,c9) { illegal_1(); op_c9();									} /* DB   FD		  */
/*TODO*///OP(fd,ca) { illegal_1(); op_ca();									} /* DB   FD		  */
/*TODO*///OP(fd,cb) { _R++; EAY; EXEC(xycb,ARG());							} /* **   FD CB xx	  */
/*TODO*///OP(fd,cc) { illegal_1(); op_cc();									} /* DB   FD		  */
/*TODO*///OP(fd,cd) { illegal_1(); op_cd();									} /* DB   FD		  */
/*TODO*///OP(fd,ce) { illegal_1(); op_ce();									} /* DB   FD		  */
/*TODO*///OP(fd,cf) { illegal_1(); op_cf();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,d0) { illegal_1(); op_d0();									} /* DB   FD		  */
/*TODO*///OP(fd,d1) { illegal_1(); op_d1();									} /* DB   FD		  */
/*TODO*///OP(fd,d2) { illegal_1(); op_d2();									} /* DB   FD		  */
/*TODO*///OP(fd,d3) { illegal_1(); op_d3();									} /* DB   FD		  */
/*TODO*///OP(fd,d4) { illegal_1(); op_d4();									} /* DB   FD		  */
/*TODO*///OP(fd,d5) { illegal_1(); op_d5();									} /* DB   FD		  */
/*TODO*///OP(fd,d6) { illegal_1(); op_d6();									} /* DB   FD		  */
/*TODO*///OP(fd,d7) { illegal_1(); op_d7();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,d8) { illegal_1(); op_d8();									} /* DB   FD		  */
/*TODO*///OP(fd,d9) { illegal_1(); op_d9();									} /* DB   FD		  */
/*TODO*///OP(fd,da) { illegal_1(); op_da();									} /* DB   FD		  */
/*TODO*///OP(fd,db) { illegal_1(); op_db();									} /* DB   FD		  */
/*TODO*///OP(fd,dc) { illegal_1(); op_dc();									} /* DB   FD		  */
/*TODO*///OP(fd,dd) { illegal_1(); op_dd();									} /* DB   FD		  */
/*TODO*///OP(fd,de) { illegal_1(); op_de();									} /* DB   FD		  */
/*TODO*///OP(fd,df) { illegal_1(); op_df();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,e0) { illegal_1(); op_e0();									} /* DB   FD		  */
/*TODO*///OP(fd,e1) { _R++; POP(IY);											} /* POP  IY		  */
/*TODO*///OP(fd,e2) { illegal_1(); op_e2();									} /* DB   FD		  */
/*TODO*///OP(fd,e3) { _R++; EXSP(IY); 										} /* EX   (SP),IY	  */
/*TODO*///OP(fd,e4) { illegal_1(); op_e4();									} /* DB   FD		  */
/*TODO*///OP(fd,e5) { _R++; PUSH( IY );										} /* PUSH IY		  */
/*TODO*///OP(fd,e6) { illegal_1(); op_e6();									} /* DB   FD		  */
/*TODO*///OP(fd,e7) { illegal_1(); op_e7();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,e8) { illegal_1(); op_e8();									} /* DB   FD		  */
/*TODO*///OP(fd,e9) { _R++; _PC = _IY; change_pc16(_PCD); 					} /* JP   (IY)		  */
/*TODO*///OP(fd,ea) { illegal_1(); op_ea();									} /* DB   FD		  */
/*TODO*///OP(fd,eb) { illegal_1(); op_eb();									} /* DB   FD		  */
/*TODO*///OP(fd,ec) { illegal_1(); op_ec();									} /* DB   FD		  */
/*TODO*///OP(fd,ed) { illegal_1(); op_ed();									} /* DB   FD		  */
/*TODO*///OP(fd,ee) { illegal_1(); op_ee();									} /* DB   FD		  */
/*TODO*///OP(fd,ef) { illegal_1(); op_ef();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,f0) { illegal_1(); op_f0();									} /* DB   FD		  */
/*TODO*///OP(fd,f1) { illegal_1(); op_f1();									} /* DB   FD		  */
/*TODO*///OP(fd,f2) { illegal_1(); op_f2();									} /* DB   FD		  */
/*TODO*///OP(fd,f3) { illegal_1(); op_f3();									} /* DB   FD		  */
/*TODO*///OP(fd,f4) { illegal_1(); op_f4();									} /* DB   FD		  */
/*TODO*///OP(fd,f5) { illegal_1(); op_f5();									} /* DB   FD		  */
/*TODO*///OP(fd,f6) { illegal_1(); op_f6();									} /* DB   FD		  */
/*TODO*///OP(fd,f7) { illegal_1(); op_f7();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(fd,f8) { illegal_1(); op_f8();									} /* DB   FD		  */
/*TODO*///OP(fd,f9) { _R++; _SP = _IY;										} /* LD   SP,IY 	  */
/*TODO*///OP(fd,fa) { illegal_1(); op_fa();									} /* DB   FD		  */
/*TODO*///OP(fd,fb) { illegal_1(); op_fb();									} /* DB   FD		  */
/*TODO*///OP(fd,fc) { illegal_1(); op_fc();									} /* DB   FD		  */
/*TODO*///OP(fd,fd) { illegal_1(); op_fd();									} /* DB   FD		  */
/*TODO*///OP(fd,fe) { illegal_1(); op_fe();									} /* DB   FD		  */
/*TODO*///OP(fd,ff) { illegal_1(); op_ff();									} /* DB   FD		  */
/*TODO*///
/*TODO*///OP(illegal,2)
/*TODO*///{
/*TODO*///	logerror("Z80 #%d ill. opcode $ed $%02x\n",
/*TODO*///			cpu_getactivecpu(), cpu_readop((_PCD-1)&0xffff));
/*TODO*///}
/*TODO*///
/*TODO*////**********************************************************
/*TODO*/// * special opcodes (ED prefix)
/*TODO*/// **********************************************************/
/*TODO*///OP(ed,00) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,01) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,02) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,03) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,04) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,05) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,06) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,07) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,08) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,09) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,0a) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,0b) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,0c) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,0d) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,0e) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,0f) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,10) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,11) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,12) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,13) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,14) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,15) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,16) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,17) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,18) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,19) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,1a) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,1b) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,1c) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,1d) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,1e) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,1f) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,20) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,21) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,22) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,23) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,24) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,25) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,26) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,27) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,28) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,29) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,2a) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,2b) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,2c) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,2d) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,2e) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,2f) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,30) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,31) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,32) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,33) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,34) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,35) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,36) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,37) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,38) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,39) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,3a) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,3b) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,3c) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,3d) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,3e) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,3f) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,40) { _B = IN(_BC); _F = (_F & CF) | SZP[_B]; 				} /* IN   B,(C) 	  */
/*TODO*///OP(ed,41) { OUT(_BC,_B);											} /* OUT  (C),B 	  */
/*TODO*///OP(ed,42) { SBC16( BC );											} /* SBC  HL,BC 	  */
/*TODO*///OP(ed,43) { EA = ARG16(); WM16( EA, &Z80.BC );						} /* LD   (w),BC	  */
/*TODO*///OP(ed,44) { NEG;													} /* NEG			  */
/*TODO*///OP(ed,45) { RETN;													} /* RETN;			  */
/*TODO*///OP(ed,46) { _IM = 0;												} /* IM   0 		  */
/*TODO*///OP(ed,47) { LD_I_A; 												} /* LD   I,A		  */
/*TODO*///
/*TODO*///OP(ed,48) { _C = IN(_BC); _F = (_F & CF) | SZP[_C]; 				} /* IN   C,(C) 	  */
/*TODO*///OP(ed,49) { OUT(_BC,_C);											} /* OUT  (C),C 	  */
/*TODO*///OP(ed,4a) { ADC16( BC );											} /* ADC  HL,BC 	  */
/*TODO*///OP(ed,4b) { EA = ARG16(); RM16( EA, &Z80.BC );						} /* LD   BC,(w)	  */
/*TODO*///OP(ed,4c) { NEG;													} /* NEG			  */
/*TODO*///OP(ed,4d) { RETI;													} /* RETI			  */
/*TODO*///OP(ed,4e) { _IM = 0;												} /* IM   0 		  */
/*TODO*///OP(ed,4f) { LD_R_A; 												} /* LD   R,A		  */
/*TODO*///
/*TODO*///OP(ed,50) { _D = IN(_BC); _F = (_F & CF) | SZP[_D]; 				} /* IN   D,(C) 	  */
/*TODO*///OP(ed,51) { OUT(_BC,_D);											} /* OUT  (C),D 	  */
/*TODO*///OP(ed,52) { SBC16( DE );											} /* SBC  HL,DE 	  */
/*TODO*///OP(ed,53) { EA = ARG16(); WM16( EA, &Z80.DE );						} /* LD   (w),DE	  */
/*TODO*///OP(ed,54) { NEG;													} /* NEG			  */
/*TODO*///OP(ed,55) { RETN;													} /* RETN;			  */
/*TODO*///OP(ed,56) { _IM = 1;												} /* IM   1 		  */
/*TODO*///OP(ed,57) { LD_A_I; 												} /* LD   A,I		  */
/*TODO*///
/*TODO*///OP(ed,58) { _E = IN(_BC); _F = (_F & CF) | SZP[_E]; 				} /* IN   E,(C) 	  */
/*TODO*///OP(ed,59) { OUT(_BC,_E);											} /* OUT  (C),E 	  */
/*TODO*///OP(ed,5a) { ADC16( DE );											} /* ADC  HL,DE 	  */
/*TODO*///OP(ed,5b) { EA = ARG16(); RM16( EA, &Z80.DE );						} /* LD   DE,(w)	  */
/*TODO*///OP(ed,5c) { NEG;													} /* NEG			  */
/*TODO*///OP(ed,5d) { RETI;													} /* RETI			  */
/*TODO*///OP(ed,5e) { _IM = 2;												} /* IM   2 		  */
/*TODO*///OP(ed,5f) { LD_A_R; 												} /* LD   A,R		  */
/*TODO*///
/*TODO*///OP(ed,60) { _H = IN(_BC); _F = (_F & CF) | SZP[_H]; 				} /* IN   H,(C) 	  */
/*TODO*///OP(ed,61) { OUT(_BC,_H);											} /* OUT  (C),H 	  */
/*TODO*///OP(ed,62) { SBC16( HL );											} /* SBC  HL,HL 	  */
/*TODO*///OP(ed,63) { EA = ARG16(); WM16( EA, &Z80.HL );						} /* LD   (w),HL	  */
/*TODO*///OP(ed,64) { NEG;													} /* NEG			  */
/*TODO*///OP(ed,65) { RETN;													} /* RETN;			  */
/*TODO*///OP(ed,66) { _IM = 0;												} /* IM   0 		  */
/*TODO*///OP(ed,67) { RRD;													} /* RRD  (HL)		  */
/*TODO*///
/*TODO*///OP(ed,68) { _L = IN(_BC); _F = (_F & CF) | SZP[_L]; 				} /* IN   L,(C) 	  */
/*TODO*///OP(ed,69) { OUT(_BC,_L);											} /* OUT  (C),L 	  */
/*TODO*///OP(ed,6a) { ADC16( HL );											} /* ADC  HL,HL 	  */
/*TODO*///OP(ed,6b) { EA = ARG16(); RM16( EA, &Z80.HL );						} /* LD   HL,(w)	  */
/*TODO*///OP(ed,6c) { NEG;													} /* NEG			  */
/*TODO*///OP(ed,6d) { RETI;													} /* RETI			  */
/*TODO*///OP(ed,6e) { _IM = 0;												} /* IM   0 		  */
/*TODO*///OP(ed,6f) { RLD;													} /* RLD  (HL)		  */
/*TODO*///
/*TODO*///OP(ed,70) { UINT8 res = IN(_BC); _F = (_F & CF) | SZP[res]; 		} /* IN   0,(C) 	  */
/*TODO*///OP(ed,71) { OUT(_BC,0); 											} /* OUT  (C),0 	  */
/*TODO*///OP(ed,72) { SBC16( SP );											} /* SBC  HL,SP 	  */
/*TODO*///OP(ed,73) { EA = ARG16(); WM16( EA, &Z80.SP );						} /* LD   (w),SP	  */
/*TODO*///OP(ed,74) { NEG;													} /* NEG			  */
/*TODO*///OP(ed,75) { RETN;													} /* RETN;			  */
/*TODO*///OP(ed,76) { _IM = 1;												} /* IM   1 		  */
/*TODO*///OP(ed,77) { illegal_2();											} /* DB   ED,77 	  */
/*TODO*///
/*TODO*///OP(ed,78) { _A = IN(_BC); _F = (_F & CF) | SZP[_A]; 				} /* IN   E,(C) 	  */
/*TODO*///OP(ed,79) { OUT(_BC,_A);											} /* OUT  (C),E 	  */
/*TODO*///OP(ed,7a) { ADC16( SP );											} /* ADC  HL,SP 	  */
/*TODO*///OP(ed,7b) { EA = ARG16(); RM16( EA, &Z80.SP );						} /* LD   SP,(w)	  */
/*TODO*///OP(ed,7c) { NEG;													} /* NEG			  */
/*TODO*///OP(ed,7d) { RETI;													} /* RETI			  */
/*TODO*///OP(ed,7e) { _IM = 2;												} /* IM   2 		  */
/*TODO*///OP(ed,7f) { illegal_2();											} /* DB   ED,7F 	  */
/*TODO*///
/*TODO*///OP(ed,80) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,81) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,82) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,83) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,84) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,85) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,86) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,87) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,88) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,89) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,8a) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,8b) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,8c) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,8d) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,8e) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,8f) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,90) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,91) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,92) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,93) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,94) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,95) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,96) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,97) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,98) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,99) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,9a) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,9b) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,9c) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,9d) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,9e) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,9f) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,a0) { LDI;													} /* LDI			  */
/*TODO*///OP(ed,a1) { CPI;													} /* CPI			  */
/*TODO*///OP(ed,a2) { INI;													} /* INI			  */
/*TODO*///OP(ed,a3) { OUTI;													} /* OUTI			  */
/*TODO*///OP(ed,a4) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,a5) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,a6) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,a7) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,a8) { LDD;													} /* LDD			  */
/*TODO*///OP(ed,a9) { CPD;													} /* CPD			  */
/*TODO*///OP(ed,aa) { IND;													} /* IND			  */
/*TODO*///OP(ed,ab) { OUTD;													} /* OUTD			  */
/*TODO*///OP(ed,ac) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,ad) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,ae) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,af) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,b0) { LDIR;													} /* LDIR			  */
/*TODO*///OP(ed,b1) { CPIR;													} /* CPIR			  */
/*TODO*///OP(ed,b2) { INIR;													} /* INIR			  */
/*TODO*///OP(ed,b3) { OTIR;													} /* OTIR			  */
/*TODO*///OP(ed,b4) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,b5) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,b6) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,b7) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,b8) { LDDR;													} /* LDDR			  */
/*TODO*///OP(ed,b9) { CPDR;													} /* CPDR			  */
/*TODO*///OP(ed,ba) { INDR;													} /* INDR			  */
/*TODO*///OP(ed,bb) { OTDR;													} /* OTDR			  */
/*TODO*///OP(ed,bc) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,bd) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,be) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,bf) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,c0) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,c1) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,c2) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,c3) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,c4) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,c5) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,c6) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,c7) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,c8) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,c9) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,ca) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,cb) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,cc) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,cd) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,ce) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,cf) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,d0) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,d1) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,d2) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,d3) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,d4) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,d5) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,d6) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,d7) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,d8) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,d9) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,da) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,db) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,dc) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,dd) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,de) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,df) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,e0) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,e1) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,e2) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,e3) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,e4) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,e5) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,e6) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,e7) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,e8) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,e9) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,ea) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,eb) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,ec) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,ed) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,ee) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,ef) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,f0) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,f1) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,f2) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,f3) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,f4) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,f5) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,f6) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,f7) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///OP(ed,f8) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,f9) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,fa) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,fb) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,fc) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,fd) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,fe) { illegal_2();											} /* DB   ED		  */
/*TODO*///OP(ed,ff) { illegal_2();											} /* DB   ED		  */
/*TODO*///
/*TODO*///#if TIME_LOOP_HACKS
/*TODO*///
/*TODO*///#define CHECK_BC_LOOP												\
/*TODO*///if( _BC > 1 && _PCD < 0xfffc ) {									\
/*TODO*///	UINT8 op1 = cpu_readop(_PCD);									\
/*TODO*///	UINT8 op2 = cpu_readop(_PCD+1); 								\
/*TODO*///	if( (op1==0x78 && op2==0xb1) || (op1==0x79 && op2==0xb0) )		\
/*TODO*///	{																\
/*TODO*///		UINT8 op3 = cpu_readop(_PCD+2); 							\
/*TODO*///		UINT8 op4 = cpu_readop(_PCD+3); 							\
/*TODO*///		if( op3==0x20 && op4==0xfb )								\
/*TODO*///		{															\
/*TODO*///			int cnt =												\
/*TODO*///				cc[Z80_TABLE_op][0x78] +							\
/*TODO*///				cc[Z80_TABLE_op][0xb1] +							\
/*TODO*///				cc[Z80_TABLE_op][0x20] +							\
/*TODO*///				cc[Z80_TABLE_ex][0x20]; 							\
/*TODO*///			while( _BC > 0 && Z80_ICOUNT > cnt )					\
/*TODO*///			{														\
/*TODO*///				BURNODD( cnt, 4, cnt ); 							\
/*TODO*///				_BC--;												\
/*TODO*///			}														\
/*TODO*///		}															\
/*TODO*///		else														\
/*TODO*///		if( op3 == 0xc2 )											\
/*TODO*///		{															\
/*TODO*///			UINT8 ad1 = cpu_readop_arg(_PCD+3); 					\
/*TODO*///			UINT8 ad2 = cpu_readop_arg(_PCD+4); 					\
/*TODO*///			if( (ad1 + 256 * ad2) == (_PCD - 1) )					\
/*TODO*///			{														\
/*TODO*///				int cnt =											\
/*TODO*///					cc[Z80_TABLE_op][0x78] +						\
/*TODO*///					cc[Z80_TABLE_op][0xb1] +						\
/*TODO*///					cc[Z80_TABLE_op][0xc2] +						\
/*TODO*///					cc[Z80_TABLE_ex][0xc2]; 						\
/*TODO*///				while( _BC > 0 && Z80_ICOUNT > cnt )				\
/*TODO*///				{													\
/*TODO*///					BURNODD( cnt, 4, cnt ); 						\
/*TODO*///					_BC--;											\
/*TODO*///				}													\
/*TODO*///			}														\
/*TODO*///		}															\
/*TODO*///	}																\
/*TODO*///}
/*TODO*///
/*TODO*///#define CHECK_DE_LOOP												\
/*TODO*///if( _DE > 1 && _PCD < 0xfffc ) {									\
/*TODO*///	UINT8 op1 = cpu_readop(_PCD);									\
/*TODO*///	UINT8 op2 = cpu_readop(_PCD+1); 								\
/*TODO*///	if( (op1==0x7a && op2==0xb3) || (op1==0x7b && op2==0xb2) )		\
/*TODO*///	{																\
/*TODO*///		UINT8 op3 = cpu_readop(_PCD+2); 							\
/*TODO*///		UINT8 op4 = cpu_readop(_PCD+3); 							\
/*TODO*///		if( op3==0x20 && op4==0xfb )								\
/*TODO*///		{															\
/*TODO*///			int cnt =												\
/*TODO*///				cc[Z80_TABLE_op][0x7a] +							\
/*TODO*///				cc[Z80_TABLE_op][0xb3] +							\
/*TODO*///				cc[Z80_TABLE_op][0x20] +							\
/*TODO*///				cc[Z80_TABLE_ex][0x20]; 							\
/*TODO*///			while( _DE > 0 && Z80_ICOUNT > cnt )					\
/*TODO*///			{														\
/*TODO*///				BURNODD( cnt, 4, cnt ); 							\
/*TODO*///				_DE--;												\
/*TODO*///			}														\
/*TODO*///		}															\
/*TODO*///		else														\
/*TODO*///		if( op3==0xc2 ) 											\
/*TODO*///		{															\
/*TODO*///			UINT8 ad1 = cpu_readop_arg(_PCD+3); 					\
/*TODO*///			UINT8 ad2 = cpu_readop_arg(_PCD+4); 					\
/*TODO*///			if( (ad1 + 256 * ad2) == (_PCD - 1) )					\
/*TODO*///			{														\
/*TODO*///				int cnt =											\
/*TODO*///					cc[Z80_TABLE_op][0x7a] +						\
/*TODO*///					cc[Z80_TABLE_op][0xb3] +						\
/*TODO*///					cc[Z80_TABLE_op][0xc2] +						\
/*TODO*///					cc[Z80_TABLE_ex][0xc2]; 						\
/*TODO*///				while( _DE > 0 && Z80_ICOUNT > cnt )				\
/*TODO*///				{													\
/*TODO*///					BURNODD( cnt, 4, cnt ); 						\
/*TODO*///					_DE--;											\
/*TODO*///				}													\
/*TODO*///			}														\
/*TODO*///		}															\
/*TODO*///	}																\
/*TODO*///}
/*TODO*///
/*TODO*///#define CHECK_HL_LOOP												\
/*TODO*///if( _HL > 1 && _PCD < 0xfffc ) {									\
/*TODO*///	UINT8 op1 = cpu_readop(_PCD);									\
/*TODO*///	UINT8 op2 = cpu_readop(_PCD+1); 								\
/*TODO*///	if( (op1==0x7c && op2==0xb5) || (op1==0x7d && op2==0xb4) )		\
/*TODO*///	{																\
/*TODO*///		UINT8 op3 = cpu_readop(_PCD+2); 							\
/*TODO*///		UINT8 op4 = cpu_readop(_PCD+3); 							\
/*TODO*///		if( op3==0x20 && op4==0xfb )								\
/*TODO*///		{															\
/*TODO*///			int cnt =												\
/*TODO*///				cc[Z80_TABLE_op][0x7c] +							\
/*TODO*///				cc[Z80_TABLE_op][0xb5] +							\
/*TODO*///				cc[Z80_TABLE_op][0x20] +							\
/*TODO*///				cc[Z80_TABLE_ex][0x20]; 							\
/*TODO*///			while( _HL > 0 && Z80_ICOUNT > cnt )					\
/*TODO*///			{														\
/*TODO*///				BURNODD( cnt, 4, cnt ); 							\
/*TODO*///				_HL--;												\
/*TODO*///			}														\
/*TODO*///		}															\
/*TODO*///		else														\
/*TODO*///		if( op3==0xc2 ) 											\
/*TODO*///		{															\
/*TODO*///			UINT8 ad1 = cpu_readop_arg(_PCD+3); 					\
/*TODO*///			UINT8 ad2 = cpu_readop_arg(_PCD+4); 					\
/*TODO*///			if( (ad1 + 256 * ad2) == (_PCD - 1) )					\
/*TODO*///			{														\
/*TODO*///				int cnt =											\
/*TODO*///					cc[Z80_TABLE_op][0x7c] +						\
/*TODO*///					cc[Z80_TABLE_op][0xb5] +						\
/*TODO*///					cc[Z80_TABLE_op][0xc2] +						\
/*TODO*///					cc[Z80_TABLE_ex][0xc2]; 						\
/*TODO*///				while( _HL > 0 && Z80_ICOUNT > cnt )				\
/*TODO*///				{													\
/*TODO*///					BURNODD( cnt, 4, cnt ); 						\
/*TODO*///					_HL--;											\
/*TODO*///				}													\
/*TODO*///			}														\
/*TODO*///		}															\
/*TODO*///	}																\
/*TODO*///}
/*TODO*///
/*TODO*///#else
/*TODO*///
/*TODO*///#define CHECK_BC_LOOP
/*TODO*///#define CHECK_DE_LOOP
/*TODO*///#define CHECK_HL_LOOP
/*TODO*///
/*TODO*///#endif
/*TODO*///
/*TODO*////**********************************************************
/*TODO*/// * main opcodes
/*TODO*/// **********************************************************/
/*TODO*///OP(op,00) { 														} /* NOP			  */
/*TODO*///OP(op,01) { _BC = ARG16();											} /* LD   BC,w		  */
/*TODO*///OP(op,02) { WM( _BC, _A );											} /* LD   (BC),A	  */
/*TODO*///OP(op,03) { _BC++;													} /* INC  BC		  */
/*TODO*///OP(op,04) { _B = INC(_B);											} /* INC  B 		  */
/*TODO*///OP(op,05) { _B = DEC(_B);											} /* DEC  B 		  */
/*TODO*///OP(op,06) { _B = ARG(); 											} /* LD   B,n		  */
/*TODO*///OP(op,07) { RLCA;													} /* RLCA			  */
/*TODO*///
/*TODO*///OP(op,08) { EX_AF;													} /* EX   AF,AF'      */
/*TODO*///OP(op,09) { ADD16(HL,BC);											} /* ADD  HL,BC 	  */
/*TODO*///OP(op,0a) { _A = RM(_BC);											} /* LD   A,(BC)	  */
/*TODO*///OP(op,0b) { _BC--; CHECK_BC_LOOP;									} /* DEC  BC		  */
/*TODO*///OP(op,0c) { _C = INC(_C);											} /* INC  C 		  */
/*TODO*///OP(op,0d) { _C = DEC(_C);											} /* DEC  C 		  */
/*TODO*///OP(op,0e) { _C = ARG(); 											} /* LD   C,n		  */
/*TODO*///OP(op,0f) { RRCA;													} /* RRCA			  */
/*TODO*///
/*TODO*///OP(op,10) { _B--; JR_COND( _B, 0x10 );								} /* DJNZ o 		  */
/*TODO*///OP(op,11) { _DE = ARG16();											} /* LD   DE,w		  */
/*TODO*///OP(op,12) { WM( _DE, _A );											} /* LD   (DE),A	  */
/*TODO*///OP(op,13) { _DE++;													} /* INC  DE		  */
/*TODO*///OP(op,14) { _D = INC(_D);											} /* INC  D 		  */
/*TODO*///OP(op,15) { _D = DEC(_D);											} /* DEC  D 		  */
/*TODO*///OP(op,16) { _D = ARG(); 											} /* LD   D,n		  */
/*TODO*///OP(op,17) { RLA;													} /* RLA			  */
/*TODO*///
/*TODO*///OP(op,18) { JR();													} /* JR   o 		  */
/*TODO*///OP(op,19) { ADD16(HL,DE);											} /* ADD  HL,DE 	  */
/*TODO*///OP(op,1a) { _A = RM(_DE);											} /* LD   A,(DE)	  */
/*TODO*///OP(op,1b) { _DE--; CHECK_DE_LOOP;									} /* DEC  DE		  */
/*TODO*///OP(op,1c) { _E = INC(_E);											} /* INC  E 		  */
/*TODO*///OP(op,1d) { _E = DEC(_E);											} /* DEC  E 		  */
/*TODO*///OP(op,1e) { _E = ARG(); 											} /* LD   E,n		  */
/*TODO*///OP(op,1f) { RRA;													} /* RRA			  */
/*TODO*///
/*TODO*///OP(op,20) { JR_COND( !(_F & ZF), 0x20 );							} /* JR   NZ,o		  */
/*TODO*///OP(op,21) { _HL = ARG16();											} /* LD   HL,w		  */
/*TODO*///OP(op,22) { EA = ARG16(); WM16( EA, &Z80.HL );						} /* LD   (w),HL	  */
/*TODO*///OP(op,23) { _HL++;													} /* INC  HL		  */
/*TODO*///OP(op,24) { _H = INC(_H);											} /* INC  H 		  */
/*TODO*///OP(op,25) { _H = DEC(_H);											} /* DEC  H 		  */
/*TODO*///OP(op,26) { _H = ARG(); 											} /* LD   H,n		  */
/*TODO*///OP(op,27) { DAA;													} /* DAA			  */
/*TODO*///
/*TODO*///OP(op,28) { JR_COND( _F & ZF, 0x28 );								} /* JR   Z,o		  */
/*TODO*///OP(op,29) { ADD16(HL,HL);											} /* ADD  HL,HL 	  */
/*TODO*///OP(op,2a) { EA = ARG16(); RM16( EA, &Z80.HL );						} /* LD   HL,(w)	  */
/*TODO*///OP(op,2b) { _HL--; CHECK_HL_LOOP;									} /* DEC  HL		  */
/*TODO*///OP(op,2c) { _L = INC(_L);											} /* INC  L 		  */
/*TODO*///OP(op,2d) { _L = DEC(_L);											} /* DEC  L 		  */
/*TODO*///OP(op,2e) { _L = ARG(); 											} /* LD   L,n		  */
/*TODO*///OP(op,2f) { _A ^= 0xff; _F = (_F&(SF|ZF|PF|CF))|HF|NF|(_A&(YF|XF)); } /* CPL			  */
/*TODO*///
/*TODO*///OP(op,30) { JR_COND( !(_F & CF), 0x30 );							} /* JR   NC,o		  */
/*TODO*///OP(op,31) { _SP = ARG16();											} /* LD   SP,w		  */
/*TODO*///OP(op,32) { EA = ARG16(); WM( EA, _A ); 							} /* LD   (w),A 	  */
/*TODO*///OP(op,33) { _SP++;													} /* INC  SP		  */
/*TODO*///OP(op,34) { WM( _HL, INC(RM(_HL)) );								} /* INC  (HL)		  */
/*TODO*///OP(op,35) { WM( _HL, DEC(RM(_HL)) );								} /* DEC  (HL)		  */
/*TODO*///OP(op,36) { WM( _HL, ARG() );										} /* LD   (HL),n	  */
/*TODO*///OP(op,37) { _F = (_F & (SF|ZF|PF)) | CF | (_A & (YF|XF));			} /* SCF			  */
/*TODO*///
/*TODO*///OP(op,38) { JR_COND( _F & CF, 0x38 );								} /* JR   C,o		  */
/*TODO*///OP(op,39) { ADD16(HL,SP);											} /* ADD  HL,SP 	  */
/*TODO*///OP(op,3a) { EA = ARG16(); _A = RM( EA );							} /* LD   A,(w) 	  */
/*TODO*///OP(op,3b) { _SP--;													} /* DEC  SP		  */
/*TODO*///OP(op,3c) { _A = INC(_A);											} /* INC  A 		  */
/*TODO*///OP(op,3d) { _A = DEC(_A);											} /* DEC  A 		  */
/*TODO*///OP(op,3e) { _A = ARG(); 											} /* LD   A,n		  */
/*TODO*///OP(op,3f) { _F = ((_F&(SF|ZF|PF|CF))|((_F&CF)<<4)|(_A&(YF|XF)))^CF; } /* CCF			  */
/*TODO*/////OP(op,3f) { _F = ((_F & ~(HF|NF)) | ((_F & CF)<<4)) ^ CF; 		  } /* CCF				*/
/*TODO*///
/*TODO*///OP(op,40) { 														} /* LD   B,B		  */
/*TODO*///OP(op,41) { _B = _C;												} /* LD   B,C		  */
/*TODO*///OP(op,42) { _B = _D;												} /* LD   B,D		  */
/*TODO*///OP(op,43) { _B = _E;												} /* LD   B,E		  */
/*TODO*///OP(op,44) { _B = _H;												} /* LD   B,H		  */
/*TODO*///OP(op,45) { _B = _L;												} /* LD   B,L		  */
/*TODO*///OP(op,46) { _B = RM(_HL);											} /* LD   B,(HL)	  */
/*TODO*///OP(op,47) { _B = _A;												} /* LD   B,A		  */
/*TODO*///
/*TODO*///OP(op,48) { _C = _B;												} /* LD   C,B		  */
/*TODO*///OP(op,49) { 														} /* LD   C,C		  */
/*TODO*///OP(op,4a) { _C = _D;												} /* LD   C,D		  */
/*TODO*///OP(op,4b) { _C = _E;												} /* LD   C,E		  */
/*TODO*///OP(op,4c) { _C = _H;												} /* LD   C,H		  */
/*TODO*///OP(op,4d) { _C = _L;												} /* LD   C,L		  */
/*TODO*///OP(op,4e) { _C = RM(_HL);											} /* LD   C,(HL)	  */
/*TODO*///OP(op,4f) { _C = _A;												} /* LD   C,A		  */
/*TODO*///
/*TODO*///OP(op,50) { _D = _B;												} /* LD   D,B		  */
/*TODO*///OP(op,51) { _D = _C;												} /* LD   D,C		  */
/*TODO*///OP(op,52) { 														} /* LD   D,D		  */
/*TODO*///OP(op,53) { _D = _E;												} /* LD   D,E		  */
/*TODO*///OP(op,54) { _D = _H;												} /* LD   D,H		  */
/*TODO*///OP(op,55) { _D = _L;												} /* LD   D,L		  */
/*TODO*///OP(op,56) { _D = RM(_HL);											} /* LD   D,(HL)	  */
/*TODO*///OP(op,57) { _D = _A;												} /* LD   D,A		  */
/*TODO*///
/*TODO*///OP(op,58) { _E = _B;												} /* LD   E,B		  */
/*TODO*///OP(op,59) { _E = _C;												} /* LD   E,C		  */
/*TODO*///OP(op,5a) { _E = _D;												} /* LD   E,D		  */
/*TODO*///OP(op,5b) { 														} /* LD   E,E		  */
/*TODO*///OP(op,5c) { _E = _H;												} /* LD   E,H		  */
/*TODO*///OP(op,5d) { _E = _L;												} /* LD   E,L		  */
/*TODO*///OP(op,5e) { _E = RM(_HL);											} /* LD   E,(HL)	  */
/*TODO*///OP(op,5f) { _E = _A;												} /* LD   E,A		  */
/*TODO*///
/*TODO*///OP(op,60) { _H = _B;												} /* LD   H,B		  */
/*TODO*///OP(op,61) { _H = _C;												} /* LD   H,C		  */
/*TODO*///OP(op,62) { _H = _D;												} /* LD   H,D		  */
/*TODO*///OP(op,63) { _H = _E;												} /* LD   H,E		  */
/*TODO*///OP(op,64) { 														} /* LD   H,H		  */
/*TODO*///OP(op,65) { _H = _L;												} /* LD   H,L		  */
/*TODO*///OP(op,66) { _H = RM(_HL);											} /* LD   H,(HL)	  */
/*TODO*///OP(op,67) { _H = _A;												} /* LD   H,A		  */
/*TODO*///
/*TODO*///OP(op,68) { _L = _B;												} /* LD   L,B		  */
/*TODO*///OP(op,69) { _L = _C;												} /* LD   L,C		  */
/*TODO*///OP(op,6a) { _L = _D;												} /* LD   L,D		  */
/*TODO*///OP(op,6b) { _L = _E;												} /* LD   L,E		  */
/*TODO*///OP(op,6c) { _L = _H;												} /* LD   L,H		  */
/*TODO*///OP(op,6d) { 														} /* LD   L,L		  */
/*TODO*///OP(op,6e) { _L = RM(_HL);											} /* LD   L,(HL)	  */
/*TODO*///OP(op,6f) { _L = _A;												} /* LD   L,A		  */
/*TODO*///
/*TODO*///OP(op,70) { WM( _HL, _B );											} /* LD   (HL),B	  */
/*TODO*///OP(op,71) { WM( _HL, _C );											} /* LD   (HL),C	  */
/*TODO*///OP(op,72) { WM( _HL, _D );											} /* LD   (HL),D	  */
/*TODO*///OP(op,73) { WM( _HL, _E );											} /* LD   (HL),E	  */
/*TODO*///OP(op,74) { WM( _HL, _H );											} /* LD   (HL),H	  */
/*TODO*///OP(op,75) { WM( _HL, _L );											} /* LD   (HL),L	  */
/*TODO*///OP(op,76) { ENTER_HALT; 											} /* HALT			  */
/*TODO*///OP(op,77) { WM( _HL, _A );											} /* LD   (HL),A	  */
/*TODO*///
/*TODO*///OP(op,78) { _A = _B;												} /* LD   A,B		  */
/*TODO*///OP(op,79) { _A = _C;												} /* LD   A,C		  */
/*TODO*///OP(op,7a) { _A = _D;												} /* LD   A,D		  */
/*TODO*///OP(op,7b) { _A = _E;												} /* LD   A,E		  */
/*TODO*///OP(op,7c) { _A = _H;												} /* LD   A,H		  */
/*TODO*///OP(op,7d) { _A = _L;												} /* LD   A,L		  */
/*TODO*///OP(op,7e) { _A = RM(_HL);											} /* LD   A,(HL)	  */
/*TODO*///OP(op,7f) { 														} /* LD   A,A		  */
/*TODO*///
/*TODO*///OP(op,80) { ADD(_B);												} /* ADD  A,B		  */
/*TODO*///OP(op,81) { ADD(_C);												} /* ADD  A,C		  */
/*TODO*///OP(op,82) { ADD(_D);												} /* ADD  A,D		  */
/*TODO*///OP(op,83) { ADD(_E);												} /* ADD  A,E		  */
/*TODO*///OP(op,84) { ADD(_H);												} /* ADD  A,H		  */
/*TODO*///OP(op,85) { ADD(_L);												} /* ADD  A,L		  */
/*TODO*///OP(op,86) { ADD(RM(_HL));											} /* ADD  A,(HL)	  */
/*TODO*///OP(op,87) { ADD(_A);												} /* ADD  A,A		  */
/*TODO*///
/*TODO*///OP(op,88) { ADC(_B);												} /* ADC  A,B		  */
/*TODO*///OP(op,89) { ADC(_C);												} /* ADC  A,C		  */
/*TODO*///OP(op,8a) { ADC(_D);												} /* ADC  A,D		  */
/*TODO*///OP(op,8b) { ADC(_E);												} /* ADC  A,E		  */
/*TODO*///OP(op,8c) { ADC(_H);												} /* ADC  A,H		  */
/*TODO*///OP(op,8d) { ADC(_L);												} /* ADC  A,L		  */
/*TODO*///OP(op,8e) { ADC(RM(_HL));											} /* ADC  A,(HL)	  */
/*TODO*///OP(op,8f) { ADC(_A);												} /* ADC  A,A		  */
/*TODO*///
/*TODO*///OP(op,90) { SUB(_B);												} /* SUB  B 		  */
/*TODO*///OP(op,91) { SUB(_C);												} /* SUB  C 		  */
/*TODO*///OP(op,92) { SUB(_D);												} /* SUB  D 		  */
/*TODO*///OP(op,93) { SUB(_E);												} /* SUB  E 		  */
/*TODO*///OP(op,94) { SUB(_H);												} /* SUB  H 		  */
/*TODO*///OP(op,95) { SUB(_L);												} /* SUB  L 		  */
/*TODO*///OP(op,96) { SUB(RM(_HL));											} /* SUB  (HL)		  */
/*TODO*///OP(op,97) { SUB(_A);												} /* SUB  A 		  */
/*TODO*///
/*TODO*///OP(op,98) { SBC(_B);												} /* SBC  A,B		  */
/*TODO*///OP(op,99) { SBC(_C);												} /* SBC  A,C		  */
/*TODO*///OP(op,9a) { SBC(_D);												} /* SBC  A,D		  */
/*TODO*///OP(op,9b) { SBC(_E);												} /* SBC  A,E		  */
/*TODO*///OP(op,9c) { SBC(_H);												} /* SBC  A,H		  */
/*TODO*///OP(op,9d) { SBC(_L);												} /* SBC  A,L		  */
/*TODO*///OP(op,9e) { SBC(RM(_HL));											} /* SBC  A,(HL)	  */
/*TODO*///OP(op,9f) { SBC(_A);												} /* SBC  A,A		  */
/*TODO*///
/*TODO*///OP(op,a0) { AND(_B);												} /* AND  B 		  */
/*TODO*///OP(op,a1) { AND(_C);												} /* AND  C 		  */
/*TODO*///OP(op,a2) { AND(_D);												} /* AND  D 		  */
/*TODO*///OP(op,a3) { AND(_E);												} /* AND  E 		  */
/*TODO*///OP(op,a4) { AND(_H);												} /* AND  H 		  */
/*TODO*///OP(op,a5) { AND(_L);												} /* AND  L 		  */
/*TODO*///OP(op,a6) { AND(RM(_HL));											} /* AND  (HL)		  */
/*TODO*///OP(op,a7) { AND(_A);												} /* AND  A 		  */
/*TODO*///
/*TODO*///OP(op,a8) { XOR(_B);												} /* XOR  B 		  */
/*TODO*///OP(op,a9) { XOR(_C);												} /* XOR  C 		  */
/*TODO*///OP(op,aa) { XOR(_D);												} /* XOR  D 		  */
/*TODO*///OP(op,ab) { XOR(_E);												} /* XOR  E 		  */
/*TODO*///OP(op,ac) { XOR(_H);												} /* XOR  H 		  */
/*TODO*///OP(op,ad) { XOR(_L);												} /* XOR  L 		  */
/*TODO*///OP(op,ae) { XOR(RM(_HL));											} /* XOR  (HL)		  */
/*TODO*///OP(op,af) { XOR(_A);												} /* XOR  A 		  */
/*TODO*///
/*TODO*///OP(op,b0) { OR(_B); 												} /* OR   B 		  */
/*TODO*///OP(op,b1) { OR(_C); 												} /* OR   C 		  */
/*TODO*///OP(op,b2) { OR(_D); 												} /* OR   D 		  */
/*TODO*///OP(op,b3) { OR(_E); 												} /* OR   E 		  */
/*TODO*///OP(op,b4) { OR(_H); 												} /* OR   H 		  */
/*TODO*///OP(op,b5) { OR(_L); 												} /* OR   L 		  */
/*TODO*///OP(op,b6) { OR(RM(_HL));											} /* OR   (HL)		  */
/*TODO*///OP(op,b7) { OR(_A); 												} /* OR   A 		  */
/*TODO*///
/*TODO*///OP(op,b8) { CP(_B); 												} /* CP   B 		  */
/*TODO*///OP(op,b9) { CP(_C); 												} /* CP   C 		  */
/*TODO*///OP(op,ba) { CP(_D); 												} /* CP   D 		  */
/*TODO*///OP(op,bb) { CP(_E); 												} /* CP   E 		  */
/*TODO*///OP(op,bc) { CP(_H); 												} /* CP   H 		  */
/*TODO*///OP(op,bd) { CP(_L); 												} /* CP   L 		  */
/*TODO*///OP(op,be) { CP(RM(_HL));											} /* CP   (HL)		  */
/*TODO*///OP(op,bf) { CP(_A); 												} /* CP   A 		  */
/*TODO*///
/*TODO*///OP(op,c0) { RET_COND( !(_F & ZF), 0xc0 );							} /* RET  NZ		  */
/*TODO*///OP(op,c1) { POP(BC);												} /* POP  BC		  */
/*TODO*///OP(op,c2) { JP_COND( !(_F & ZF) );									} /* JP   NZ,a		  */
/*TODO*///OP(op,c3) { JP; 													} /* JP   a 		  */
/*TODO*///OP(op,c4) { CALL_COND( !(_F & ZF), 0xc4 );							} /* CALL NZ,a		  */
/*TODO*///OP(op,c5) { PUSH( BC ); 											} /* PUSH BC		  */
/*TODO*///OP(op,c6) { ADD(ARG()); 											} /* ADD  A,n		  */
/*TODO*///OP(op,c7) { RST(0x00);												} /* RST  0 		  */
/*TODO*///
/*TODO*///OP(op,c8) { RET_COND( _F & ZF, 0xc8 );								} /* RET  Z 		  */
/*TODO*///OP(op,c9) { POP(PC); change_pc16(_PCD); 							} /* RET			  */
/*TODO*///OP(op,ca) { JP_COND( _F & ZF ); 									} /* JP   Z,a		  */
/*TODO*///OP(op,cb) { _R++; EXEC(cb,ROP());									} /* **** CB xx 	  */
/*TODO*///OP(op,cc) { CALL_COND( _F & ZF, 0xcc ); 							} /* CALL Z,a		  */
/*TODO*///OP(op,cd) { CALL(); 												} /* CALL a 		  */
/*TODO*///OP(op,ce) { ADC(ARG()); 											} /* ADC  A,n		  */
/*TODO*///OP(op,cf) { RST(0x08);												} /* RST  1 		  */
/*TODO*///
/*TODO*///OP(op,d0) { RET_COND( !(_F & CF), 0xd0 );							} /* RET  NC		  */
/*TODO*///OP(op,d1) { POP(DE);												} /* POP  DE		  */
/*TODO*///OP(op,d2) { JP_COND( !(_F & CF) );									} /* JP   NC,a		  */
/*TODO*///OP(op,d3) { unsigned n = ARG() | (_A << 8); OUT( n, _A );			} /* OUT  (n),A 	  */
/*TODO*///OP(op,d4) { CALL_COND( !(_F & CF), 0xd4 );							} /* CALL NC,a		  */
/*TODO*///OP(op,d5) { PUSH( DE ); 											} /* PUSH DE		  */
/*TODO*///OP(op,d6) { SUB(ARG()); 											} /* SUB  n 		  */
/*TODO*///OP(op,d7) { RST(0x10);												} /* RST  2 		  */
/*TODO*///
/*TODO*///OP(op,d8) { RET_COND( _F & CF, 0xd8 );								} /* RET  C 		  */
/*TODO*///OP(op,d9) { EXX;													} /* EXX			  */
/*TODO*///OP(op,da) { JP_COND( _F & CF ); 									} /* JP   C,a		  */
/*TODO*///OP(op,db) { unsigned n = ARG() | (_A << 8); _A = IN( n );			} /* IN   A,(n) 	  */
/*TODO*///OP(op,dc) { CALL_COND( _F & CF, 0xdc ); 							} /* CALL C,a		  */
/*TODO*///OP(op,dd) { _R++; EXEC(dd,ROP());									} /* **** DD xx 	  */
/*TODO*///OP(op,de) { SBC(ARG()); 											} /* SBC  A,n		  */
/*TODO*///OP(op,df) { RST(0x18);												} /* RST  3 		  */
/*TODO*///
/*TODO*///OP(op,e0) { RET_COND( !(_F & PF), 0xe0 );							} /* RET  PO		  */
/*TODO*///OP(op,e1) { POP(HL);												} /* POP  HL		  */
/*TODO*///OP(op,e2) { JP_COND( !(_F & PF) );									} /* JP   PO,a		  */
/*TODO*///OP(op,e3) { EXSP(HL);												} /* EX   HL,(SP)	  */
/*TODO*///OP(op,e4) { CALL_COND( !(_F & PF), 0xe4 );							} /* CALL PO,a		  */
/*TODO*///OP(op,e5) { PUSH( HL ); 											} /* PUSH HL		  */
/*TODO*///OP(op,e6) { AND(ARG()); 											} /* AND  n 		  */
/*TODO*///OP(op,e7) { RST(0x20);												} /* RST  4 		  */
/*TODO*///
/*TODO*///OP(op,e8) { RET_COND( _F & PF, 0xe8 );								} /* RET  PE		  */
/*TODO*///OP(op,e9) { _PC = _HL; change_pc16(_PCD);							} /* JP   (HL)		  */
/*TODO*///OP(op,ea) { JP_COND( _F & PF ); 									} /* JP   PE,a		  */
/*TODO*///OP(op,eb) { EX_DE_HL;												} /* EX   DE,HL 	  */
/*TODO*///OP(op,ec) { CALL_COND( _F & PF, 0xec ); 							} /* CALL PE,a		  */
/*TODO*///OP(op,ed) { _R++; EXEC(ed,ROP());									} /* **** ED xx 	  */
/*TODO*///OP(op,ee) { XOR(ARG()); 											} /* XOR  n 		  */
/*TODO*///OP(op,ef) { RST(0x28);												} /* RST  5 		  */
/*TODO*///
/*TODO*///OP(op,f0) { RET_COND( !(_F & SF), 0xf0 );							} /* RET  P 		  */
/*TODO*///OP(op,f1) { POP(AF);												} /* POP  AF		  */
/*TODO*///OP(op,f2) { JP_COND( !(_F & SF) );									} /* JP   P,a		  */
/*TODO*///OP(op,f3) { _IFF1 = _IFF2 = 0;										} /* DI 			  */
/*TODO*///OP(op,f4) { CALL_COND( !(_F & SF), 0xf4 );							} /* CALL P,a		  */
/*TODO*///OP(op,f5) { PUSH( AF ); 											} /* PUSH AF		  */
/*TODO*///OP(op,f6) { OR(ARG());												} /* OR   n 		  */
/*TODO*///OP(op,f7) { RST(0x30);												} /* RST  6 		  */
/*TODO*///
/*TODO*///OP(op,f8) { RET_COND( _F & SF, 0xf8 );								} /* RET  M 		  */
/*TODO*///OP(op,f9) { _SP = _HL;												} /* LD   SP,HL 	  */
/*TODO*///OP(op,fa) { JP_COND(_F & SF);										} /* JP   M,a		  */
/*TODO*///OP(op,fb) { EI; 													} /* EI 			  */
/*TODO*///OP(op,fc) { CALL_COND( _F & SF, 0xfc ); 							} /* CALL M,a		  */
/*TODO*///OP(op,fd) { _R++; EXEC(fd,ROP());									} /* **** FD xx 	  */
/*TODO*///OP(op,fe) { CP(ARG());												} /* CP   n 		  */
/*TODO*///OP(op,ff) { RST(0x38);												} /* RST  7 		  */
/*TODO*///
/*TODO*///
    static void take_interrupt() {
        if (Z80.IFF1 != 0) {

            int irq_vector;

            /* there isn't a valid previous program counter */
            Z80.PREPC = -1;

            /* Check if processor was halted */
            LEAVE_HALT();

            if (Z80.irq_max != 0) /* daisy chain mode */ {
                if (Z80.request_irq >= 0) {
                    /* Clear both interrupt flip flops */
                    Z80.IFF1 = Z80.IFF2 = 0;
                    irq_vector = Z80.irq[Z80.request_irq].interrupt_entry.handler(Z80.irq[Z80.request_irq].irq_param);
                    //LOG(("Z80 #%d daisy chain irq_vector $%02x\n", cpu_getactivecpu(), irq_vector));
                    Z80.request_irq = -1;
                } else {
                    return;
                }
            } else {
                /* Clear both interrupt flip flops */
                Z80.IFF1 = Z80.IFF2 = 0;
                /* call back the cpu interface to retrieve the vector */
                irq_vector = (Z80.irq_callback).handler(0);
                //LOG(("Z80 #%d single int. irq_vector $%02x\n", cpu_getactivecpu(), irq_vector));
            }

            /* Interrupt mode 2. Call [Z80.I:databyte] */
            if (Z80.IM == 2) {
                irq_vector = (irq_vector & 0xff) | (Z80.I << 8);
                PUSH(Z80.PC);
                Z80.PC = RM16(irq_vector);
                //LOG(("Z80 #%d IM2 [$%04x] = $%04x\n",cpu_getactivecpu() , irq_vector, _PCD));
                /* CALL opcode timing */
                Z80.extra_cycles += cc[Z80_TABLE_op][0xcd];
            } else /* Interrupt mode 1. RST 38h */ if (Z80.IM == 1) {
                //LOG(("Z80 #%d IM1 $0038\n",cpu_getactivecpu() ));
                PUSH(Z80.PC);
                Z80.PC = 0x0038;
                /* RST $38 + 'interrupt latency' cycles */
                Z80.extra_cycles += cc[Z80_TABLE_op][0xff] + cc[Z80_TABLE_ex][0xff];
            } else {
                /* Interrupt mode 0. We check for CALL and JP instructions, */
 /* if neither of these were found we assume a 1 byte opcode */
 /* was placed on the databus                                */
                //LOG(("Z80 #%d IM0 $%04x\n",cpu_getactivecpu() , irq_vector));
                switch (irq_vector & 0xff0000) {
                    case 0xcd0000:
                        /* call */
                        PUSH(Z80.PC);
                        Z80.PC = irq_vector & 0xffff;
                        /* CALL $xxxx + 'interrupt latency' cycles */
                        Z80.extra_cycles += cc[Z80_TABLE_op][0xcd] + cc[Z80_TABLE_ex][0xff];
                        break;
                    case 0xc30000:
                        /* jump */
                        Z80.PC = irq_vector & 0xffff;
                        /* JP $xxxx + 2 cycles */
                        Z80.extra_cycles += cc[Z80_TABLE_op][0xc3] + cc[Z80_TABLE_ex][0xff];
                        break;
                    default:
                        /* rst (or other opcodes?) */
                        PUSH(Z80.PC);
                        Z80.PC = irq_vector & 0x0038;
                        /* RST $xx + 2 cycles */
                        Z80.extra_cycles += cc[Z80_TABLE_op][Z80.PC] + cc[Z80_TABLE_ex][Z80.PC];
                        break;
                }
            }
            Z80.WZ = Z80.PC;//TODO check if it has to run always and not only if IFF1!=0
            change_pc16(Z80.PC);
        }
    }

    /**
     * **************************************************************************
     * Processor initialization
     * **************************************************************************
     */
    @Override
    public void init() {
        int cpu = cpu_getactivecpu();
        int i, p;
        int oldval, newval, val;
        int padd, padc, psub, psbc;
        padd = 0 * 256;
        padc = 256 * 256;
        psub = 0 * 256;
        psbc = 256 * 256;
        for (oldval = 0; oldval < 256; oldval++) {
            for (newval = 0; newval < 256; newval++) {
                /* add or adc w/o carry set */
                val = newval - oldval;

                if (newval != 0) {
                    if ((newval & 0x80) != 0) {
                        SZHVC_Add[padd] = SF;
                    } else {
                        SZHVC_Add[padd] = 0;
                    }
                } else {
                    SZHVC_Add[padd] = ZF;
                }

                SZHVC_Add[padd] |= (newval & (YF | XF));/* undocumented flag bits 5+3 */

                if ((newval & 0x0f) < (oldval & 0x0f)) {
                    SZHVC_Add[padd] |= HF;
                }
                if (newval < oldval) {
                    SZHVC_Add[padd] |= CF;
                }
                if (((val ^ oldval ^ 0x80) & (val ^ newval) & 0x80) != 0) {
                    SZHVC_Add[padd] |= VF;
                }
                padd++;

                /* adc with carry set */
                val = newval - oldval - 1;
                if (newval != 0) {
                    if ((newval & 0x80) != 0) {
                        SZHVC_Add[padc] = SF;
                    } else {
                        SZHVC_Add[padc] = 0;
                    }
                } else {
                    SZHVC_Add[padc] = ZF;
                }

                SZHVC_Add[padc] |= (newval & (YF | XF));/* undocumented flag bits 5+3 */

                if ((newval & 0x0f) <= (oldval & 0x0f)) {
                    SZHVC_Add[padc] |= HF;
                }
                if (newval <= oldval) {
                    SZHVC_Add[padc] |= CF;
                }
                if (((val ^ oldval ^ 0x80) & (val ^ newval) & 0x80) != 0) {
                    SZHVC_Add[padc] |= VF;
                }
                padc++;

                /* cp, sub or sbc w/o carry set */
                val = oldval - newval;
                if (newval != 0) {
                    if ((newval & 0x80) != 0) {
                        SZHVC_sub[psub] = NF | SF;
                    } else {
                        SZHVC_sub[psub] = NF;
                    }
                } else {
                    SZHVC_sub[psub] = NF | ZF;
                }

                SZHVC_sub[psub] |= (newval & (YF | XF));/* undocumented flag bits 5+3 */

                if ((newval & 0x0f) > (oldval & 0x0f)) {
                    SZHVC_sub[psub] |= HF;
                }
                if (newval > oldval) {
                    SZHVC_sub[psub] |= CF;
                }
                if (((val ^ oldval) & (oldval ^ newval) & 0x80) != 0) {
                    SZHVC_sub[psub] |= VF;
                }
                psub++;

                /* sbc with carry set */
                val = oldval - newval - 1;
                if (newval != 0) {
                    if ((newval & 0x80) != 0) {
                        SZHVC_sub[psbc] = NF | SF;
                    } else {
                        SZHVC_sub[psbc] = NF;
                    }
                } else {
                    SZHVC_sub[psbc] = NF | ZF;
                }

                SZHVC_sub[psbc] |= (newval & (YF | XF));/* undocumented flag bits 5+3 */

                if ((newval & 0x0f) >= (oldval & 0x0f)) {
                    SZHVC_sub[psbc] |= HF;
                }
                if (newval >= oldval) {
                    SZHVC_sub[psbc] |= CF;
                }
                if (((val ^ oldval) & (oldval ^ newval) & 0x80) != 0) {
                    SZHVC_sub[psbc] |= VF;
                }
                psbc++;
            }
        }
        for (i = 0; i < 256; i++) {
            p = 0;
            if ((i & 0x01) != 0) {
                ++p;
            }
            if ((i & 0x02) != 0) {
                ++p;
            }
            if ((i & 0x04) != 0) {
                ++p;
            }
            if ((i & 0x08) != 0) {
                ++p;
            }
            if ((i & 0x10) != 0) {
                ++p;
            }
            if ((i & 0x20) != 0) {
                ++p;
            }
            if ((i & 0x40) != 0) {
                ++p;
            }
            if ((i & 0x80) != 0) {
                ++p;
            }
            SZ[i] = (i != 0) ? i & 0x80 : 0x40;
            SZ[i] |= (i & (0x20 | 0x08));/* undocumented flag bits 5+3 */

            SZ_BIT[i] = (i != 0) ? i & 0x80 : 0x40 | 0x04;
            SZ_BIT[i] |= (i & (0x20 | 0x08));/* undocumented flag bits 5+3 */

            SZP[i] = SZ[i] | (((p & 1) != 0) ? 0 : 0x04);
            SZHV_inc[i] = SZ[i];
            if (i == 0x80) {
                SZHV_inc[i] |= 0x04;
            }
            if ((i & 0x0f) == 0x00) {
                SZHV_inc[i] |= 0x10;
            }
            SZHV_dec[i] = SZ[i] | 0x02;
            if (i == 0x7f) {
                SZHV_dec[i] |= 0x04;
            }
            if ((i & 0x0f) == 0x0f) {
                SZHV_dec[i] |= 0x10;
            }
        }
        /*TODO*///
/*TODO*///	state_save_register_UINT16("z80", cpu, "AF", &Z80.AF.w.l, 1);
/*TODO*///	state_save_register_UINT16("z80", cpu, "BC", &Z80.BC.w.l, 1);
/*TODO*///	state_save_register_UINT16("z80", cpu, "DE", &Z80.DE.w.l, 1);
/*TODO*///	state_save_register_UINT16("z80", cpu, "HL", &Z80.HL.w.l, 1);
/*TODO*///	state_save_register_UINT16("z80", cpu, "IX", &Z80.IX.w.l, 1);
/*TODO*///	state_save_register_UINT16("z80", cpu, "IY", &Z80.IY.w.l, 1);
/*TODO*///	state_save_register_UINT16("z80", cpu, "PC", &Z80.PC.w.l, 1);
/*TODO*///	state_save_register_UINT16("z80", cpu, "SP", &Z80.SP.w.l, 1);
/*TODO*///	state_save_register_UINT16("z80", cpu, "AF2", &Z80.AF2.w.l, 1);
/*TODO*///	state_save_register_UINT16("z80", cpu, "BC2", &Z80.BC2.w.l, 1);
/*TODO*///	state_save_register_UINT16("z80", cpu, "DE2", &Z80.DE2.w.l, 1);
/*TODO*///	state_save_register_UINT16("z80", cpu, "HL2", &Z80.HL2.w.l, 1);
/*TODO*///	state_save_register_UINT8("z80", cpu, "R", &Z80.R, 1);
/*TODO*///	state_save_register_UINT8("z80", cpu, "R2", &Z80.R2, 1);
/*TODO*///	state_save_register_UINT8("z80", cpu, "IFF1", &Z80.IFF1, 1);
/*TODO*///	state_save_register_UINT8("z80", cpu, "IFF2", &Z80.IFF2, 1);
/*TODO*///	state_save_register_UINT8("z80", cpu, "HALT", &Z80.HALT, 1);
/*TODO*///	state_save_register_UINT8("z80", cpu, "IM", &Z80.IM, 1);
/*TODO*///	state_save_register_UINT8("z80", cpu, "I", &Z80.I, 1);
/*TODO*///	state_save_register_UINT8("z80", cpu, "irq_max", &Z80.irq_max, 1);
/*TODO*///	state_save_register_INT8("z80", cpu, "request_irq", &Z80.request_irq, 1);
/*TODO*///	state_save_register_INT8("z80", cpu, "service_irq", &Z80.service_irq, 1);
/*TODO*///	state_save_register_UINT8("z80", cpu, "int_state", Z80.int_state, 4);
/*TODO*///	state_save_register_UINT8("z80", cpu, "nmi_state", &Z80.nmi_state, 1);
/*TODO*///	state_save_register_UINT8("z80", cpu, "irq_state", &Z80.irq_state, 1);
/*TODO*///	/* daisy chain needs to be saved by z80ctc.c somehow */
    }

    /**
     * **************************************************************************
     * Reset registers to their initial values
     * **************************************************************************
     */
    @Override
    public void reset(Object param) {
        Z80_DaisyChain[] daisy_chain = (Z80_DaisyChain[]) param;

        //memset(&Z80, 0, sizeof(Z80));
        Z80.PREPC = 0;
        Z80.PC = 0;
        Z80.SP = 0;
        Z80.A = 0;
        Z80.F = 0;
        Z80.B = 0;
        Z80.C = 0;
        Z80.D = 0;
        Z80.E = 0;
        Z80.H = 0;
        Z80.L = 0;
        Z80.IX = 0;
        Z80.IY = 0;
        Z80.A2 = 0;
        Z80.F2 = 0;
        Z80.B2 = 0;
        Z80.C2 = 0;
        Z80.D2 = 0;
        Z80.E2 = 0;
        Z80.H2 = 0;
        Z80.L2 = 0;
        Z80.WZ = 0;
        Z80.R = 0;
        Z80.R2 = 0;
        Z80.IFF1 = 0;
        Z80.IFF2 = 0;
        Z80.HALT = 0;
        Z80.IM = 0;
        Z80.I = 0;
        Z80.irq_max = 0;
        Z80.request_irq = 0;
        Z80.service_irq = 0;
        Z80.nmi_state = 0;
        Z80.irq_state = 0;
        Z80.int_state = new int[Z80_MAXDAISY];
        Z80.irq = new Z80_DaisyChain[Z80_MAXDAISY];
        Z80.irq_callback = null;
        Z80.extra_cycles = 0;

        Z80.IX = Z80.IY = 0xffff;/* IX and IY are FFFF after a reset! */
        Z80.F = ZF;/* Zero flag is set */
        Z80.request_irq = -1;
        Z80.service_irq = -1;
        Z80.nmi_state = CLEAR_LINE;
        Z80.irq_state = CLEAR_LINE;

        int dci = 0;
        if (daisy_chain != null) {
            while (daisy_chain[dci].irq_param != -1 && Z80.irq_max < Z80_MAXDAISY) {
                /* set callbackhandler after reti */
                Z80.irq[Z80.irq_max] = daisy_chain[dci];
                /* device reset */
                if (Z80.irq[Z80.irq_max].reset != null) {
                    Z80.irq[Z80.irq_max].reset.handler(Z80.irq[Z80.irq_max].irq_param);
                }
                Z80.irq_max++;
                dci++;
            }
        }
        change_pc16(Z80.PC);

    }

    @Override
    public void exit() {
        SZHVC_Add = null;
        SZHVC_sub = null;
    }

    /**
     * **************************************************************************
     * Execute 'cycles' T-states. Return number of T-states really executed
     * **************************************************************************
     */
    @Override
    public int execute(int cycles) {
        z80_ICount[0] = cycles - Z80.extra_cycles;
        Z80.extra_cycles = 0;

        do {
            Z80.PREPC = Z80.PC & 0xFFFF;
            Z80.R = (Z80.R + 1) & 0xFF;//_R++;
            int op = ROP();
            z80_ICount[0] -= cc[Z80_TABLE_op][op];
            Z80op[op].handler();//EXEC_INLINE(op, ROP());
        } while (z80_ICount[0] > 0);

        z80_ICount[0] -= Z80.extra_cycles;
        Z80.extra_cycles = 0;

        return cycles - z80_ICount[0];
    }

    /**
     * **************************************************************************
     * Burn 'cycles' T-states. Adjust R register for the lost time
     * **************************************************************************
     */
    public burnPtr burn_function = new burnPtr() {
        public void handler(int cycles) {
            if (cycles > 0) {
                /* NOP takes 4 cycles per instruction */
                int n = (cycles + 3) / 4;
                Z80.R = (Z80.R + n) & 0xFF;
                z80_ICount[0] -= 4 * n;
            }
        }
    };

    /**
     * **************************************************************************
     * Get all registers in given buffer
     * **************************************************************************
     */
    @Override
    public Object get_context() {
        Z80_Regs Regs = new Z80_Regs();
        Regs.PREPC = Z80.PREPC;
        Regs.PC = Z80.PC;
        Regs.SP = Z80.SP;

        Regs.A = Z80.A;
        Regs.F = Z80.F;
        Regs.B = Z80.B;
        Regs.C = Z80.C;
        Regs.D = Z80.D;
        Regs.E = Z80.E;
        Regs.H = Z80.H;
        Regs.L = Z80.L;
        Regs.IX = Z80.IX;
        Regs.IY = Z80.IY;
        Regs.A2 = Z80.A2;
        Regs.F2 = Z80.F2;
        Regs.B2 = Z80.B2;
        Regs.C2 = Z80.C2;
        Regs.D2 = Z80.D2;
        Regs.E2 = Z80.E2;
        Regs.H2 = Z80.H2;
        Regs.L2 = Z80.L2;
        Regs.WZ = Z80.WZ;
        Regs.R = Z80.R;
        Regs.R2 = Z80.R2;
        Regs.IFF1 = Z80.IFF1;
        Regs.IFF2 = Z80.IFF2;
        Regs.HALT = Z80.HALT;
        Regs.IM = Z80.IM;
        Regs.I = Z80.I;
        Regs.irq_max = Z80.irq_max;
        Regs.request_irq = Z80.request_irq;
        Regs.service_irq = Z80.service_irq;
        Regs.nmi_state = Z80.nmi_state;
        Regs.irq_state = Z80.irq_state;
        Regs.int_state[0] = Z80.int_state[0];
        Regs.int_state[1] = Z80.int_state[1];
        Regs.int_state[2] = Z80.int_state[2];
        Regs.int_state[3] = Z80.int_state[3];
        Regs.irq[0] = Z80.irq[0];
        Regs.irq[1] = Z80.irq[1];
        Regs.irq[2] = Z80.irq[2];
        Regs.irq[3] = Z80.irq[3];
        Regs.irq_callback = Z80.irq_callback;
        Regs.extra_cycles = Z80.extra_cycles;
        return Regs;
    }

    /**
     * **************************************************************************
     * Set all registers to given values
     * **************************************************************************
     */
    @Override
    public void set_context(Object reg) {
        Z80_Regs Regs = (Z80_Regs) reg;
        Z80.PREPC = Regs.PREPC;
        Z80.PC = Regs.PC;
        Z80.SP = Regs.SP;

        Z80.A = Regs.A;
        Z80.F = Regs.F;
        Z80.B = Regs.B;
        Z80.C = Regs.C;
        Z80.D = Regs.D;
        Z80.E = Regs.E;
        Z80.H = Regs.H;
        Z80.L = Regs.L;
        Z80.IX = Regs.IX;
        Z80.IY = Regs.IY;
        Z80.A2 = Regs.A2;
        Z80.F2 = Regs.F2;
        Z80.B2 = Regs.B2;
        Z80.C2 = Regs.C2;
        Z80.D2 = Regs.D2;
        Z80.E2 = Regs.E2;
        Z80.H2 = Regs.H2;
        Z80.L2 = Regs.L2;
        Z80.WZ = Regs.WZ;
        Z80.R = Regs.R;
        Z80.R2 = Regs.R2;
        Z80.IFF1 = Regs.IFF1;
        Z80.IFF2 = Regs.IFF2;
        Z80.HALT = Regs.HALT;
        Z80.IM = Regs.IM;
        Z80.I = Regs.I;
        Z80.irq_max = Regs.irq_max;
        Z80.request_irq = Regs.request_irq;
        Z80.service_irq = Regs.service_irq;
        Z80.nmi_state = Regs.nmi_state;
        Z80.irq_state = Regs.irq_state;
        Z80.int_state[0] = Regs.int_state[0];
        Z80.int_state[1] = Regs.int_state[1];
        Z80.int_state[2] = Regs.int_state[2];
        Z80.int_state[3] = Regs.int_state[3];
        Z80.irq[0] = Regs.irq[0];
        Z80.irq[1] = Regs.irq[1];
        Z80.irq[2] = Regs.irq[2];
        Z80.irq[3] = Regs.irq[3];
        Z80.irq_callback = Regs.irq_callback;
        Z80.extra_cycles = Regs.extra_cycles;
        change_pc16(Z80.PC);
    }

    /**
     * **************************************************************************
     * Get a pointer to a cycle count table
     * **************************************************************************
     */
    @Override
    public int[] get_cycle_table(int which) {
        if (which >= 0 && which <= Z80_TABLE_xycb) {
            return cc[which];
        }
        return null;
    }

    /**
     * **************************************************************************
     * Set a new cycle count table
     * **************************************************************************
     */
    @Override
    public void set_cycle_table(int which, int[] new_table) {
        if (which >= 0 && which <= Z80_TABLE_ex) {
            cc[which] = new_table;
        }
    }

    /**
     * **************************************************************************
     * Return a specific register
     * **************************************************************************
     */
    @Override
    public int get_reg(int regnum) {
        switch (regnum) {
            case REG_PC:
                return Z80.PC & 0xFFFF;
            case Z80_PC:
                return Z80.PC & 0xFFFF;
            case REG_SP:
                return Z80.SP & 0xFFFF;
            case Z80_SP:
                return Z80.SP & 0xFFFF;
            case Z80_AF:
                return AF();
            case Z80_BC:
                return BC();
            case Z80_DE:
                return DE();
            case Z80_HL:
                return HL();
            case Z80_IX:
                return Z80.IX & 0xFFFF;
            case Z80_IY:
                return Z80.IY & 0xFFFF;
            case Z80_R:
                return (Z80.R & 0x7f) | (Z80.R2 & 0x80);
            case Z80_I:
                return Z80.I;
            case Z80_AF2:
                return AF2();
            case Z80_BC2:
                return BC2();
            case Z80_DE2:
                return DE2();
            case Z80_HL2:
                return HL2();
            case Z80_IM:
                return Z80.IM;
            case Z80_IFF1:
                return Z80.IFF1;
            case Z80_IFF2:
                return Z80.IFF2;
            case Z80_HALT:
                return Z80.HALT;
            case Z80_NMI_STATE:
                return Z80.nmi_state;
            case Z80_IRQ_STATE:
                return Z80.irq_state;
            case Z80_DC0:
                return Z80.int_state[0];
            case Z80_DC1:
                return Z80.int_state[1];
            case Z80_DC2:
                return Z80.int_state[2];
            case Z80_DC3:
                return Z80.int_state[3];
            case REG_PREVIOUSPC:
                return Z80.PREPC & 0xFFFF;
            default:
                if (regnum <= REG_SP_CONTENTS) {
                    int offset = Z80.SP + 2 * (REG_SP_CONTENTS - regnum);
                    if (offset < 0xffff) {
                        return RM(offset) | (RM(offset + 1) << 8);
                    }
                }
        }
        return 0;
    }

    /**
     * **************************************************************************
     * Set a specific register
     * **************************************************************************
     */
    @Override
    public void set_reg(int regnum, int val) {
        switch (regnum) {
            case REG_PC:
                Z80.PC = val & 0xFFFF;
                change_pc16(Z80.PC);
                break;
            case Z80_PC:
                Z80.PC = val & 0xFFFF;
                break;
            case REG_SP:
                Z80.SP = val & 0xFFFF;
                break;
            case Z80_SP:
                Z80.SP = val & 0xFFFF;
                break;
            case Z80_AF:
                AF(val);
                break;
            case Z80_BC:
                BC(val);
                break;
            case Z80_DE:
                DE(val);
                break;
            case Z80_HL:
                HL(val);
                break;
            case Z80_IX:
                Z80.IX = val & 0xFFFF;
                break;
            case Z80_IY:
                Z80.IY = val & 0xFFFF;
                break;
            case Z80_R:
                Z80.R = val;
                Z80.R2 = val & 0x80;
                break;
            case Z80_I:
                Z80.I = val;
                break;
            case Z80_AF2:
                AF2(val);
                break;
            case Z80_BC2:
                BC2(val);
                break;
            case Z80_DE2:
                DE2(val);
                break;
            case Z80_HL2:
                HL2(val);
                break;
            case Z80_IM:
                Z80.IM = val;
                break;
            case Z80_IFF1:
                Z80.IFF1 = val;
                break;
            case Z80_IFF2:
                Z80.IFF2 = val;
                break;
            case Z80_HALT:
                Z80.HALT = val;
                break;
            case Z80_NMI_STATE:
                set_irq_line(IRQ_LINE_NMI, val);
                break;
            case Z80_IRQ_STATE:
                set_irq_line(0, val);
                break;
            case Z80_DC0:
                Z80.int_state[0] = val;
                break;
            case Z80_DC1:
                Z80.int_state[1] = val;
                break;
            case Z80_DC2:
                Z80.int_state[2] = val;
                break;
            case Z80_DC3:
                Z80.int_state[3] = val;
                break;
            default:
                if (regnum <= REG_SP_CONTENTS) {
                    int offset = Z80.SP + 2 * (REG_SP_CONTENTS - regnum);
                    if (offset < 0xffff) {
                        WM(offset, val & 0xff);
                        WM(offset + 1, (val >> 8) & 0xff);
                    }
                }
        }
    }

    /**
     * **************************************************************************
     * Set IRQ line state
     * **************************************************************************
     */
    @Override
    public void set_irq_line(int irqline, int state) {
        if (irqline == IRQ_LINE_NMI) {
            if (Z80.nmi_state == state) {
                return;
            }
            //LOG(("Z80 #%d set_nmi_line %d\n", cpu_getactivecpu(), state));
            Z80.nmi_state = state;
            if (state == CLEAR_LINE) {
                return;
            }
            //LOG(("Z80 #%d take NMI\n", cpu_getactivecpu()));
            Z80.PREPC = -1;/* there isn't a valid previous program counter */
            LEAVE_HALT();/* Check if processor was halted */

            Z80.IFF1 = 0;
            PUSH(Z80.PC);
            Z80.PC = 0x0066;
            Z80.WZ = Z80.PC;
            Z80.extra_cycles += 11;
        } else {
            //LOG(("Z80 #%d set_irq_line %d\n",cpu_getactivecpu() , state));
            Z80.irq_state = state;
            if (state == CLEAR_LINE) {
                return;
            }
            if (Z80.irq_max != 0) {
                int daisychain, device, int_state;
                daisychain = Z80.irq_callback.handler(irqline);
                device = daisychain >> 8;
                int_state = daisychain & 0xff;
                //LOG(("Z80 #%d daisy chain $%04x -> device %d, state $%02x",cpu_getactivecpu(), daisychain, device, int_state));

                if (Z80.int_state[device] != int_state) {
                    //LOG((" change\n"));
                    /* set new interrupt status */
                    Z80.int_state[device] = int_state;
                    /* check interrupt status */
                    Z80.request_irq = Z80.service_irq = -1;

                    /* search higher IRQ or IEO */
                    for (device = 0; device < Z80.irq_max; device++) {
                        /* IEO = disable ? */
                        if ((Z80.int_state[device] & Z80_INT_IEO) != 0) {
                            Z80.request_irq = -1;/* if IEO is disable , masking lower IRQ */
                            Z80.service_irq = device;/* set highest interrupt service device */
                        }
                        /* IRQ = request ? */
                        if ((Z80.int_state[device] & Z80_INT_REQ) != 0) {
                            Z80.request_irq = device;
                        }
                    }
                    //LOG(("Z80 #%d daisy chain service_irq $%02x, request_irq $%02x\n", cpu_getactivecpu(), Z80.service_irq, Z80.request_irq));
                    if (Z80.request_irq < 0) {
                        return;
                    }
                } else {
                    //LOG((" no change\n"));
                    return;
                }
            }
            take_interrupt();
        }
    }

    /**
     * **************************************************************************
     * Set IRQ vector callback
     * **************************************************************************
     */
    @Override
    public void set_irq_callback(irqcallbacksPtr callback) {
        //LOG(("Z80 #%d set_irq_callback $%08x\n",cpu_getactivecpu() , (int)callback));
        Z80.irq_callback = callback;
    }

    /**
     * **************************************************************************
     * Return a formatted string for a register
     * **************************************************************************
     */
    @Override
    public String cpu_info(Object context, int regnum) {
        /*TODO*///	static char buffer[32][47+1];
/*TODO*///	static int which = 0;
/*TODO*///	Z80_Regs *r = context;
/*TODO*///
/*TODO*///	which = (which+1) % 32;
/*TODO*///    buffer[which][0] = '\0';
/*TODO*///	if( !context )
/*TODO*///		r = &Z80;
/*TODO*///
        switch (regnum) {
            /*TODO*///		case CPU_INFO_REG+Z80_PC: sprintf(buffer[which], "PC:%04X", r->PC.w.l); break;
/*TODO*///		case CPU_INFO_REG+Z80_SP: sprintf(buffer[which], "SP:%04X", r->SP.w.l); break;
/*TODO*///		case CPU_INFO_REG+Z80_AF: sprintf(buffer[which], "AF:%04X", r->AF.w.l); break;
/*TODO*///		case CPU_INFO_REG+Z80_BC: sprintf(buffer[which], "BC:%04X", r->BC.w.l); break;
/*TODO*///		case CPU_INFO_REG+Z80_DE: sprintf(buffer[which], "DE:%04X", r->DE.w.l); break;
/*TODO*///		case CPU_INFO_REG+Z80_HL: sprintf(buffer[which], "HL:%04X", r->HL.w.l); break;
/*TODO*///		case CPU_INFO_REG+Z80_IX: sprintf(buffer[which], "IX:%04X", r->IX.w.l); break;
/*TODO*///		case CPU_INFO_REG+Z80_IY: sprintf(buffer[which], "IY:%04X", r->IY.w.l); break;
/*TODO*///		case CPU_INFO_REG+Z80_R: sprintf(buffer[which], "R:%02X", (r->R & 0x7f) | (r->R2 & 0x80)); break;
/*TODO*///		case CPU_INFO_REG+Z80_I: sprintf(buffer[which], "I:%02X", r->I); break;
/*TODO*///		case CPU_INFO_REG+Z80_AF2: sprintf(buffer[which], "AF'%04X", r->AF2.w.l); break;
/*TODO*///		case CPU_INFO_REG+Z80_BC2: sprintf(buffer[which], "BC'%04X", r->BC2.w.l); break;
/*TODO*///		case CPU_INFO_REG+Z80_DE2: sprintf(buffer[which], "DE'%04X", r->DE2.w.l); break;
/*TODO*///		case CPU_INFO_REG+Z80_HL2: sprintf(buffer[which], "HL'%04X", r->HL2.w.l); break;
/*TODO*///		case CPU_INFO_REG+Z80_IM: sprintf(buffer[which], "IM:%X", r->IM); break;
/*TODO*///		case CPU_INFO_REG+Z80_IFF1: sprintf(buffer[which], "IFF1:%X", r->IFF1); break;
/*TODO*///		case CPU_INFO_REG+Z80_IFF2: sprintf(buffer[which], "IFF2:%X", r->IFF2); break;
/*TODO*///		case CPU_INFO_REG+Z80_HALT: sprintf(buffer[which], "HALT:%X", r->HALT); break;
/*TODO*///		case CPU_INFO_REG+Z80_NMI_STATE: sprintf(buffer[which], "NMI:%X", r->nmi_state); break;
/*TODO*///		case CPU_INFO_REG+Z80_IRQ_STATE: sprintf(buffer[which], "IRQ:%X", r->irq_state); break;
/*TODO*///		case CPU_INFO_REG+Z80_DC0: if(Z80.irq_max >= 1) sprintf(buffer[which], "DC0:%X", r->int_state[0]); break;
/*TODO*///		case CPU_INFO_REG+Z80_DC1: if(Z80.irq_max >= 2) sprintf(buffer[which], "DC1:%X", r->int_state[1]); break;
/*TODO*///		case CPU_INFO_REG+Z80_DC2: if(Z80.irq_max >= 3) sprintf(buffer[which], "DC2:%X", r->int_state[2]); break;
/*TODO*///		case CPU_INFO_REG+Z80_DC3: if(Z80.irq_max >= 4) sprintf(buffer[which], "DC3:%X", r->int_state[3]); break;
/*TODO*///        case CPU_INFO_FLAGS:
/*TODO*///			sprintf(buffer[which], "%c%c%c%c%c%c%c%c",
/*TODO*///				r->AF.b.l & 0x80 ? 'S':'.',
/*TODO*///				r->AF.b.l & 0x40 ? 'Z':'.',
/*TODO*///				r->AF.b.l & 0x20 ? '5':'.',
/*TODO*///				r->AF.b.l & 0x10 ? 'H':'.',
/*TODO*///				r->AF.b.l & 0x08 ? '3':'.',
/*TODO*///				r->AF.b.l & 0x04 ? 'P':'.',
/*TODO*///				r->AF.b.l & 0x02 ? 'N':'.',
/*TODO*///				r->AF.b.l & 0x01 ? 'C':'.');
/*TODO*///			break;
            case CPU_INFO_NAME:
                return "Z80";
            case CPU_INFO_FAMILY:
                return "Zilog Z80";
            case CPU_INFO_VERSION:
                return "3.3";
            case CPU_INFO_FILE:
                return "z80.java";
            case CPU_INFO_CREDITS:
                return "Copyright (C) 1998,1999 Juergen Buchmueller, all rights reserved.";
            /*TODO*///		case CPU_INFO_REG_LAYOUT: return (const char *)z80_reg_layout;
/*TODO*///		case CPU_INFO_WIN_LAYOUT: return (const char *)z80_win_layout;
            }
        /*TODO*///	return buffer[which];
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String cpu_dasm(String buffer, int pc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        /*TODO*///#ifdef MAME_DEBUG
/*TODO*///	return DasmZ80( buffer, pc );
/*TODO*///#else
/*TODO*///	sprintf( buffer, "$%02X", cpu_readop(pc) );
/*TODO*///	return 1;
    }

    /**
     * *
     *
     * arcadeflex functions
     */
    @Override
    public Object init_context() {
        Object reg = new Z80_Regs();
        return reg;
    }

    @Override
    public void set_op_base(int pc) {
        cpu_setOPbase16.handler(pc);
    }

    @Override
    public int memory_read(int offset) {
        return cpu_readmem16(offset);
    }

    @Override
    public void memory_write(int offset, int data) {
        cpu_writemem16(offset, data);
    }

    @Override
    public int internal_read(int offset) {
        return 0; //doesn't exist in z80 cpu
    }

    @Override
    public void internal_write(int offset, int data) {
        //doesesn't exist in z80 cpu
    }
    @Override
    public int mem_address_bits_of_cpu() {
        return 16;
    }
    
    /**
     * Opcodes
     */
    opcode illegal_1 = new opcode() {
        public void handler() {
            logerror("Z80 #%d ill. opcode $%02x $%02x\n", cpu_getactivecpu(), cpu_readop((Z80.PC - 1) & 0xffff), cpu_readop(Z80.PC));
        }
    };

    opcode illegal_2 = new opcode() {
        public void handler() {
            logerror("Z80 #%d ill. opcode $ed $%02x\n", cpu_getactivecpu(), cpu_readop((Z80.PC - 1) & 0xffff));
        }
    };



    public abstract interface opcode {

        public abstract void handler();
    }
    /**********************************************************
    * Table opcodes
    **********************************************************/     
    opcode op_cb = new opcode() {
        public void handler() {
            Z80.R = (Z80.R + 1) & 0xFF;
            int op = ROP();
            z80_ICount[0] -= cc[Z80_TABLE_cb][op];
            Z80cb[op].handler();//EXEC(cb, ROP());
        }
    };
    opcode op_dd = new opcode() {
        public void handler() {
            Z80.R = (Z80.R + 1) & 0xFF;
            int op = ROP();
            z80_ICount[0] -= cc[Z80_TABLE_xy][op];
            Z80dd[op].handler();//EXEC(dd, ROP());
        }
    };
    opcode op_ed = new opcode() {
        public void handler() {
            Z80.R = (Z80.R + 1) & 0xFF;
            int op = ROP();
            z80_ICount[0] -= cc[Z80_TABLE_ed][op];
            Z80ed[op].handler();//EXEC(ed, ROP());
        }
    };
    opcode op_fd = new opcode() {
        public void handler() {
            Z80.R = (Z80.R + 1) & 0xFF;
            int op = ROP();
            z80_ICount[0] -= cc[Z80_TABLE_xy][op];
            Z80fd[op].handler();//EXEC(fd, ROP());
        }
    };
    opcode dd_cb = new opcode() {
        public void handler() {
            Z80.R = (Z80.R + 1) & 0xFF;
            EAX();
            int op = ARG();
            z80_ICount[0] -= cc[Z80_TABLE_xycb][op];
            Z80xycb[op].handler();//EXEC(xycb,ARG());
        }
    };
    opcode fd_cb = new opcode() {
        public void handler() {
            Z80.R = (Z80.R + 1) & 0xFF;
            EAY();
            int op = ARG();
            z80_ICount[0] -= cc[Z80_TABLE_xycb][op];
            Z80xycb[op].handler();
        }
    };
    /**********************************************************
    * Unorganized opcodes
    **********************************************************/ 
    opcode fd_21 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY = ARG16();									}}; /* LD   IY,w		  */
    opcode fd_22 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EA = ARG16(); WM16( EA, Z80.IY );	Z80.WZ = (EA + 1) & 0xFFFF;			}}; /* LD   (w),IY	  */
    opcode fd_2a = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EA = ARG16(); Z80.IY=RM16(EA); Z80.WZ = (EA + 1) & 0xFFFF;				}}; /* LD   IY,(w)	  */
    opcode fd_e9 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.PC = Z80.IY; change_pc16(Z80.PC); 					}}; /* JP   (IY)		  */
    opcode fd_f9 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.SP = Z80.IY;										}}; /* LD   SP,IY 	  */
    opcode dd_21 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX = ARG16();									}}; /* LD   IX,w		  */
    opcode dd_22 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EA = ARG16(); WM16( EA, Z80.IX );	Z80.WZ = (EA + 1) & 0xFFFF;			}}; /* LD   (w),IX	  */
    opcode dd_2a = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EA = ARG16(); Z80.IX=RM16(EA); Z80.WZ = (EA + 1) & 0xFFFF;				}}; /* LD   IX,(w)	  */
    opcode dd_e9 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.PC = Z80.IX; change_pc16(Z80.PC); 					}}; /* JP   (IX)		  */
    opcode dd_f9 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.SP = Z80.IX;										}}; /* LD   SP,IX 	  */
    opcode ed_43 = new opcode() { public void handler() { EA = ARG16(); WM16( EA, BC() ); Z80.WZ = (EA + 1) & 0xFFFF;						}}; /* LD   (w),BC	  */
    opcode ed_4b = new opcode() { public void handler() { EA = ARG16(); BC(RM16(EA)); Z80.WZ = (EA + 1) & 0xFFFF;						}}; /* LD   BC,(w)	  */
    opcode ed_53 = new opcode() { public void handler() { EA = ARG16(); WM16( EA, DE() ); Z80.WZ = (EA + 1) & 0xFFFF;						}}; /* LD   (w),DE	  */
    opcode ed_5b = new opcode() { public void handler() { EA = ARG16(); DE(RM16(EA)); Z80.WZ = (EA + 1) & 0xFFFF;						}}; /* LD   DE,(w)	  */
    opcode ed_63 = new opcode() { public void handler() { EA = ARG16(); WM16( EA, HL() ); Z80.WZ = (EA + 1) & 0xFFFF;						}}; /* LD   (w),HL	  */
    opcode ed_6b = new opcode() { public void handler() { EA = ARG16(); HL(RM16(EA)); Z80.WZ = (EA + 1) & 0xFFFF;						}}; /* LD   HL,(w)	  */
    opcode ed_73 = new opcode() { public void handler() { EA = ARG16(); WM16( EA, Z80.SP ); Z80.WZ = (EA + 1) & 0xFFFF;						}}; /* LD   (w),SP	  */
    opcode ed_7b = new opcode() { public void handler() { EA = ARG16(); Z80.SP=RM16(EA); Z80.WZ = (EA + 1) & 0xFFFF;						}}; /* LD   SP,(w)	  */
    opcode op_01 = new opcode() { public void handler() { BC(ARG16());											}}; /* LD   BC,w		  */
    opcode op_11 = new opcode() { public void handler() { DE(ARG16());											}}; /* LD   DE,w		  */
    opcode op_21 = new opcode() { public void handler() { HL(ARG16());											}}; /* LD   HL,w		  */
    opcode op_31 = new opcode() { public void handler() { Z80.SP = ARG16();											}}; /* LD   SP,w		  */
    opcode op_02 = new opcode() { public void handler() { WM( BC(), Z80.A ); Z80.WZ = (Z80.WZ & 0xff00) | ((BC() + 1) & 0xFF); Z80.WZ = ((Z80.WZ & 0x00ff) | Z80.A <<8);											}}; /* LD   (BC),A	  */
    opcode op_12 = new opcode() { public void handler() { WM( DE(), Z80.A ); Z80.WZ = (Z80.WZ & 0xff00) | ((DE() + 1) & 0xFF); Z80.WZ = ((Z80.WZ & 0x00ff) | Z80.A <<8);					}}; /* LD   (DE),A	  */    
    opcode op_22 = new opcode() { public void handler() { EA = ARG16(); WM16( EA, HL() ); Z80.WZ = (EA + 1) & 0xFFFF; 						}}; /* LD   (w),HL	  */
    opcode op_32 = new opcode() { public void handler() { EA = ARG16(); WM( EA, Z80.A ); Z80.WZ = (Z80.WZ & 0xff00) | ((EA + 1) & 0xFF); Z80.WZ = ((Z80.WZ & 0x00ff) | Z80.A <<8);							}}; /* LD   (w),A 	  */
    opcode op_2a = new opcode() { public void handler() { EA = ARG16(); HL(RM16(EA)); Z80.WZ = (EA + 1) & 0xFFFF;						}}; /* LD   HL,(w)	  */
    opcode dd_24 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX = ((Z80.IX & 0x00ff) | INC((Z80.IX >> 8) & 0xFF)<<8);									}}; /* INC  HX		  */
    opcode dd_25 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX = ((Z80.IX & 0x00ff) | DEC((Z80.IX >> 8) & 0xFF)<<8);									}}; /* DEC  HX		  */
    opcode dd_26 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX = ((Z80.IX & 0x00ff) | ARG()<<8);										}}; /* LD   HX,n		  */
    opcode dd_2c = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX = (Z80.IX & 0xff00) | INC(Z80.IX & 0xFF);									}}; /* INC  LX		  */
    opcode dd_2d = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX = (Z80.IX & 0xff00) | DEC(Z80.IX & 0xFF);									}}; /* DEC  LX		  */
    opcode dd_2e = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX = (Z80.IX & 0xff00) | ARG();										}}; /* LD   LX,n		  */
    opcode fd_24 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY = ((Z80.IY & 0x00ff) | INC((Z80.IY >> 8) & 0xFF)<<8);									}}; /* INC  HY		  */
    opcode fd_25 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY = ((Z80.IY & 0x00ff) | DEC((Z80.IY >> 8) & 0xFF)<<8);									}}; /* DEC  HY		  */
    opcode fd_26 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY = ((Z80.IY & 0x00ff) | ARG()<<8);										}}; /* LD   HY,n		  */
    opcode fd_2c = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY = (Z80.IY & 0xff00) | INC(Z80.IY & 0xFF);									}}; /* INC  LY		  */
    opcode fd_2d = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY = (Z80.IY & 0xff00) | DEC(Z80.IY & 0xFF);									}}; /* DEC  LY		  */
    opcode fd_2e = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY = (Z80.IY & 0xff00) | ARG();										}}; /* LD   LY,n		  */
    /**********************************************************
    * MISC opcodes
    **********************************************************/    
    opcode op_27 = new opcode() { public void handler() { DAA();													}}; /* DAA			  */
    opcode op_2f = new opcode() { public void handler() { Z80.A ^= 0xff; Z80.F = (Z80.F&(SF|ZF|PF|CF))|HF|NF|(Z80.A&(YF|XF)); }}; /* CPL			  */
    opcode op_37 = new opcode() { public void handler() { Z80.F = (Z80.F & (SF | ZF | YF | XF | PF)) | CF | (Z80.A & (YF | XF));			}}; /* SCF			  */
    opcode op_3f = new opcode() { public void handler() { Z80.F = ((Z80.F&(SF|ZF|YF|XF|PF|CF))|((Z80.F&CF)<<4)|(Z80.A&(YF|XF)))^CF; }}; /* CCF			  */
    opcode ed_67 = new opcode() { public void handler() { RRD();													}}; /* RRD  (HL)		  */
    opcode ed_6f = new opcode() { public void handler() { RLD();													}}; /* RLD  (HL)		  */
    opcode ed_a0 = new opcode() { public void handler() { LDI();													}}; /* LDI			  */
    opcode ed_a1 = new opcode() { public void handler() { CPI();													}}; /* CPI			  */
    opcode ed_a8 = new opcode() { public void handler() { LDD();													}}; /* LDD			  */
    opcode ed_a9 = new opcode() { public void handler() { CPD();													}}; /* CPD			  */
    opcode ed_b0 = new opcode() { public void handler() { LDIR();													}}; /* LDIR			  */
    opcode ed_b1 = new opcode() { public void handler() { CPIR();													}}; /* CPIR			  */
    opcode ed_b8 = new opcode() { public void handler() { LDDR();													}}; /* LDDR			  */
    opcode ed_b9 = new opcode() { public void handler() { CPDR();													}}; /* CPDR			  */
    opcode op_17 = new opcode() { public void handler() { RLA();													}}; /* RLA			  */
    opcode op_1f = new opcode() { public void handler() { RRA();													}}; /* RRA			  */
    opcode op_07 = new opcode() { public void handler() { RLCA();													}}; /* RLCA			  */
    opcode op_0f = new opcode() { public void handler() { RRCA();													}}; /* RRCA			  */
    opcode op_f3 = new opcode() { public void handler() { Z80.IFF1 = Z80.IFF2 = 0;										}}; /* DI 			  */
    opcode op_18 = new opcode() { public void handler() { JR();													}}; /* JR   o 		  */
    opcode op_c3 = new opcode() { public void handler() { JP(); 													}}; /* JP   a 		  */
    opcode ed_b3 = new opcode() { public void handler() { OTIR();													}}; /* OTIR			  */
    opcode op_fb = new opcode() { public void handler() { EI(); 													}}; /* EI 			  */
    opcode ed_a3 = new opcode() { public void handler() { OUTI();													}}; /* OUTI			  */
    opcode ed_47 = new opcode() { public void handler() { LD_I_A(); 												}}; /* LD   I,A		  */
    opcode ed_57 = new opcode() { public void handler() { LD_A_I(); 												}}; /* LD   A,I		  */
    opcode ed_5f = new opcode() { public void handler() { LD_A_R(); 												}}; /* LD   A,R		  */
    opcode ed_4f = new opcode() { public void handler() { LD_R_A(); 												}}; /* LD   R,A		  */
    opcode op_76 = new opcode() { public void handler() { ENTER_HALT(); 											}}; /* HALT			  */
    opcode ed_a2 = new opcode() { public void handler() { INI();													}}; /* INI			  */
    opcode ed_b2 = new opcode() { public void handler() { INIR();													}}; /* INIR			  */
    opcode ed_aa = new opcode() { public void handler() { IND();													}}; /* IND			  */
    opcode ed_ba = new opcode() { public void handler() { INDR();													}}; /* INDR			  */
    opcode ed_ab = new opcode() { public void handler() { OUTD();													}}; /* OUTD			  */
    opcode ed_bb = new opcode() { public void handler() { OTDR();													}}; /* OTDR			  */
    /**********************************************************
    * RETI/RETN opcodes
    **********************************************************/
    opcode ed_4d = new opcode() { public void handler() { RETI();													}}; /* RETI			  */
    opcode ed_5d = new opcode() { public void handler() { RETI();													}}; /* RETI			  */
    opcode ed_6d = new opcode() { public void handler() { RETI();													}}; /* RETI			  */
    opcode ed_7d = new opcode() { public void handler() { RETI();													}}; /* RETI			  */
    opcode ed_45 = new opcode() { public void handler() { RETN();													}}; /* RETN;			  */
    opcode ed_55 = new opcode() { public void handler() { RETN();													}}; /* RETN;			  */
    opcode ed_65 = new opcode() { public void handler() { RETN();													}}; /* RETN;			  */
    opcode ed_75 = new opcode() { public void handler() { RETN();													}}; /* RETN;			  */
    /**********************************************************
    * Exchange opcodes
    **********************************************************/
    opcode op_eb = new opcode() { public void handler() { EX_DE_HL();												}}; /* EX   DE,HL 	  */
    opcode dd_e3 = new opcode() { public void handler() { Z80.R = (Z80.R +1 ) & 0xFF; Z80.IX=EXSP(Z80.IX); 										}}; /* EX   (SP),IX	  */
    opcode fd_e3 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=EXSP(Z80.IY); 										}}; /* EX   (SP),IY	  */
    opcode op_e3 = new opcode() { public void handler() { HL(EXSP(HL()));												}}; /* EX   HL,(SP)	  */
    opcode op_08 = new opcode() { public void handler() { EX_AF();													}}; /* EX   AF,AF'      */
    opcode op_d9 = new opcode() { public void handler() { EXX();													}}; /* EXX			  */
    /**********************************************************
    * RST opcodes
    **********************************************************/
    opcode op_c7 = new opcode() { public void handler() { RST(0x00);												}}; /* RST  0 		  */
    opcode op_cf = new opcode() { public void handler() { RST(0x08);												}}; /* RST  1 		  */
    opcode op_d7 = new opcode() { public void handler() { RST(0x10);												}}; /* RST  2 		  */
    opcode op_df = new opcode() { public void handler() { RST(0x18);												}}; /* RST  3 		  */
    opcode op_e7 = new opcode() { public void handler() { RST(0x20);												}}; /* RST  4 		  */
    opcode op_ef = new opcode() { public void handler() { RST(0x28);												}}; /* RST  5 		  */
    opcode op_f7 = new opcode() { public void handler() { RST(0x30);												}}; /* RST  6 		  */
    opcode op_ff = new opcode() { public void handler() { RST(0x38);												}}; /* RST  7 		  */
    /**********************************************************
    * IN opcodes
    **********************************************************/
    opcode ed_40 = new opcode() { public void handler() { Z80.B = IN(BC()); Z80.F = (Z80.F & CF) | SZP[Z80.B]; 				}}; /* IN   B,(C) 	  */
    opcode ed_48 = new opcode() { public void handler() { Z80.C = IN(BC()); Z80.F = (Z80.F & CF) | SZP[Z80.C]; 				}}; /* IN   C,(C) 	  */
    opcode ed_50 = new opcode() { public void handler() { Z80.D = IN(BC()); Z80.F = (Z80.F & CF) | SZP[Z80.D]; 				}}; /* IN   D,(C) 	  */
    opcode ed_58 = new opcode() { public void handler() { Z80.E = IN(BC()); Z80.F = (Z80.F & CF) | SZP[Z80.E]; 				}}; /* IN   E,(C) 	  */
    opcode ed_60 = new opcode() { public void handler() { Z80.H = IN(BC()); Z80.F = (Z80.F & CF) | SZP[Z80.H]; 				}}; /* IN   H,(C) 	  */
    opcode ed_68 = new opcode() { public void handler() { Z80.L = IN(BC()); Z80.F = (Z80.F & CF) | SZP[Z80.L]; 				}}; /* IN   L,(C) 	  */
    opcode ed_70 = new opcode() { public void handler() { int res = IN(BC()); Z80.F = (Z80.F & CF) | SZP[res]; 		}}; /* IN   0,(C) 	  */
    opcode ed_78 = new opcode() { public void handler() { Z80.A = IN(BC()); Z80.F = (Z80.F & CF) | SZP[Z80.A]; 	Z80.WZ = (BC() + 1) &0xFFFF;			}}; /* IN   E,(C) 	  */
    opcode op_db = new opcode() { public void handler() { int n = (ARG() | (Z80.A << 8)) & 0xFFFF; Z80.A = IN(n); Z80.WZ = (n + 1)&0xFFFF;			}}; /* IN   A,(n) 	  */
    /**********************************************************
    * OUT opcodes
    **********************************************************/
    opcode ed_41 = new opcode() { public void handler() { OUT(BC(),Z80.B);											}}; /* OUT  (C),B 	  */
    opcode ed_49 = new opcode() { public void handler() { OUT(BC(),Z80.C);											}}; /* OUT  (C),C 	  */
    opcode ed_51 = new opcode() { public void handler() { OUT(BC(),Z80.D);											}}; /* OUT  (C),D 	  */
    opcode ed_59 = new opcode() { public void handler() { OUT(BC(),Z80.E);											}}; /* OUT  (C),E 	  */
    opcode ed_61 = new opcode() { public void handler() { OUT(BC(),Z80.H);											}}; /* OUT  (C),H 	  */
    opcode ed_69 = new opcode() { public void handler() { OUT(BC(),Z80.L);											}}; /* OUT  (C),L 	  */
    opcode ed_71 = new opcode() { public void handler() { OUT(BC(),0); 											}}; /* OUT  (C),0 	  */
    opcode ed_79 = new opcode() { public void handler() { OUT(BC(),Z80.A); Z80.WZ = (BC() + 1) &0xFFFF;											}}; /* OUT  (C),E 	  */
    opcode op_d3 = new opcode() { public void handler() { int n = (ARG() | (Z80.A << 8)) & 0xFFFF; OUT(n, Z80.A); Z80.WZ = (Z80.WZ & 0xff00) | (((n & 0xff) + 1) & 0xFF); Z80.WZ = ((Z80.WZ & 0x00ff) | Z80.A <<8);			}}; /* OUT  (n),A 	  */
    /**********************************************************
    * IM opcodes
    **********************************************************/
    opcode ed_46 = new opcode() { public void handler() { Z80.IM = 0;												}}; /* IM   0 		  */
    opcode ed_4e = new opcode() { public void handler() { Z80.IM = 0;												}}; /* IM   0 		  */
    opcode ed_56 = new opcode() { public void handler() { Z80.IM = 1;												}}; /* IM   1 		  */
    opcode ed_5e = new opcode() { public void handler() { Z80.IM = 2;												}}; /* IM   2 		  */
    opcode ed_66 = new opcode() { public void handler() { Z80.IM = 0;												}}; /* IM   0 		  */
    opcode ed_6e = new opcode() { public void handler() { Z80.IM = 0;												}}; /* IM   0 		  */
    opcode ed_76 = new opcode() { public void handler() { Z80.IM = 1;												}}; /* IM   1 		  */
    opcode ed_7e = new opcode() { public void handler() { Z80.IM = 2;												}}; /* IM   2 		  */
    /**********************************************************
    * RET_COND opcodes
    **********************************************************/
    opcode op_c0 = new opcode() { public void handler() { RET_COND( (Z80.F & ZF)==0, 0xc0 );							}}; /* RET  NZ		  */
    opcode op_c8 = new opcode() { public void handler() { RET_COND( (Z80.F & ZF)!=0, 0xc8 );								}}; /* RET  Z 		  */
    opcode op_d0 = new opcode() { public void handler() { RET_COND( (Z80.F & CF)==0, 0xd0 );							}}; /* RET  NC		  */
    opcode op_d8 = new opcode() { public void handler() { RET_COND( (Z80.F & CF)!=0, 0xd8 );								}}; /* RET  C 		  */
    opcode op_e0 = new opcode() { public void handler() { RET_COND( (Z80.F & PF)==0, 0xe0 );							}}; /* RET  PO		  */
    opcode op_e8 = new opcode() { public void handler() { RET_COND( (Z80.F & PF)!=0, 0xe8 );								}}; /* RET  PE		  */
    opcode op_f0 = new opcode() { public void handler() { RET_COND( (Z80.F & SF)==0, 0xf0 );							}}; /* RET  P 		  */
    opcode op_f8 = new opcode() { public void handler() { RET_COND( (Z80.F & SF)!=0, 0xf8 );								}}; /* RET  M 		  */
    /**********************************************************
    * CALL_COND opcodes
    **********************************************************/
    opcode op_c4 = new opcode() { public void handler() { CALL_COND( (Z80.F & ZF)==0, 0xc4 );							}}; /* CALL NZ,a		  */
    opcode op_cc = new opcode() { public void handler() { CALL_COND( (Z80.F & ZF)!=0, 0xcc ); 							}}; /* CALL Z,a		  */
    opcode op_d4 = new opcode() { public void handler() { CALL_COND( (Z80.F & CF)==0, 0xd4 );							}}; /* CALL NC,a		  */
    opcode op_dc = new opcode() { public void handler() { CALL_COND( (Z80.F & CF)!=0, 0xdc ); 							}}; /* CALL C,a		  */
    opcode op_e4 = new opcode() { public void handler() { CALL_COND( (Z80.F & PF)==0, 0xe4 );							}}; /* CALL PO,a		  */
    opcode op_ec = new opcode() { public void handler() { CALL_COND( (Z80.F & PF)!=0, 0xec ); 							}}; /* CALL PE,a		  */
    opcode op_f4 = new opcode() { public void handler() { CALL_COND( (Z80.F & SF)==0, 0xf4 );							}}; /* CALL P,a		  */
    opcode op_fc = new opcode() { public void handler() { CALL_COND( (Z80.F & SF)!=0, 0xfc ); 							}}; /* CALL M,a		  */
    opcode op_cd = new opcode() { public void handler() { CALL(); 												}}; /* CALL a 		  */
    /**********************************************************
    * JP_COND opcodes
    **********************************************************/
    opcode op_c2 = new opcode() { public void handler() { JP_COND( (Z80.F & ZF)==0 );									}}; /* JP   NZ,a		  */
    opcode op_ca = new opcode() { public void handler() { JP_COND( (Z80.F & ZF)!=0 ); 									}}; /* JP   Z,a		  */
    opcode op_d2 = new opcode() { public void handler() { JP_COND( (Z80.F & CF)==0 );									}}; /* JP   NC,a		  */
    opcode op_da = new opcode() { public void handler() { JP_COND( (Z80.F & CF)!=0 ); 									}}; /* JP   C,a		  */
    opcode op_e2 = new opcode() { public void handler() { JP_COND( (Z80.F & PF)==0 );									}}; /* JP   PO,a		  */
    opcode op_ea = new opcode() { public void handler() { JP_COND( (Z80.F & PF)!=0 ); 									}}; /* JP   PE,a		  */
    opcode op_f2 = new opcode() { public void handler() { JP_COND( (Z80.F & SF)==0 );									}}; /* JP   P,a		  */
    opcode op_fa = new opcode() { public void handler() { JP_COND( (Z80.F & SF)!=0 );										}}; /* JP   M,a		  */
    /**********************************************************
    * JR_COND opcodes
    **********************************************************/
    opcode op_10 = new opcode() { public void handler() { Z80.B = (Z80.B - 1) & 0xFF; JR_COND( Z80.B != 0, 0x10 );	}}; /* DJNZ o 		  */
    opcode op_20 = new opcode() { public void handler() { JR_COND( (Z80.F & ZF)==0, 0x20 );							}}; /* JR   NZ,o		  */
    opcode op_28 = new opcode() { public void handler() { JR_COND( (Z80.F & ZF)!=0, 0x28 );								}}; /* JR   Z,o		  */
    opcode op_30 = new opcode() { public void handler() { JR_COND( (Z80.F & CF)==0, 0x30 );							}}; /* JR   NC,o		  */
    opcode op_38 = new opcode() { public void handler() { JR_COND( (Z80.F & CF)!=0, 0x38 );								}}; /* JR   C,o		  */
    /**********************************************************
    * POP opcodes
    **********************************************************/
    opcode dd_e1 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=POP();											}}; /* POP  IX		  */
    opcode fd_e1 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=POP();											}}; /* POP  IY		  */
    opcode op_c1 = new opcode() { public void handler() { BC(POP());												}}; /* POP  BC		  */
    opcode op_c9 = new opcode() { public void handler() { Z80.PC=POP(); Z80.WZ=Z80.PC; change_pc16(Z80.PC); 							}}; /* RET			  */
    opcode op_d1 = new opcode() { public void handler() { DE(POP());												}}; /* POP  DE		  */
    opcode op_e1 = new opcode() { public void handler() { HL(POP());												}}; /* POP  HL		  */
    opcode op_f1 = new opcode() { public void handler() { AF(POP());												}}; /* POP  AF		  */
    /**********************************************************
    * PUSH opcodes
    **********************************************************/
    opcode dd_e5 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; PUSH( Z80.IX );										}}; /* PUSH IX		  */
    opcode fd_e5 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; PUSH( Z80.IY );										}}; /* PUSH IY		  */
    opcode op_c5 = new opcode() { public void handler() { PUSH( BC() ); 											}}; /* PUSH BC		  */
    opcode op_d5 = new opcode() { public void handler() { PUSH( DE() ); 											}}; /* PUSH DE		  */
    opcode op_e5 = new opcode() { public void handler() { PUSH( HL() ); 											}}; /* PUSH HL		  */
    opcode op_f5 = new opcode() { public void handler() { PUSH( AF() ); 											}}; /* PUSH AF		  */
    /**********************************************************
    * INC opcodes
    **********************************************************/
    opcode op_04 = new opcode() { public void handler() { Z80.B = INC(Z80.B);											}}; /* INC  B 		  */
    opcode op_0c = new opcode() { public void handler() { Z80.C = INC(Z80.C);											}}; /* INC  C 		  */
    opcode op_14 = new opcode() { public void handler() { Z80.D = INC(Z80.D);											}}; /* INC  D 		  */
    opcode op_1c = new opcode() { public void handler() { Z80.E = INC(Z80.E);											}}; /* INC  E 		  */
    opcode op_24 = new opcode() { public void handler() { Z80.H = INC(Z80.H);											}}; /* INC  H 		  */
    opcode op_2c = new opcode() { public void handler() { Z80.L = INC(Z80.L);											}}; /* INC  L 		  */
    opcode op_3c = new opcode() { public void handler() { Z80.A = INC(Z80.A);											}}; /* INC  A 		  */
    opcode dd_34 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); WM( EA, INC(RM(EA)) );						}}; /* INC  (IX+o)	  */
    opcode fd_34 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); WM( EA, INC(RM(EA)) );						}}; /* INC  (IY+o)	  */
    opcode op_34 = new opcode() { public void handler() { WM( HL(), INC(RM(HL())) );								}}; /* INC  (HL)		  */
    opcode op_03 = new opcode() { public void handler() { BC((BC() + 1) & 0xFFFF);													}}; /* INC  BC		  */
    opcode op_13 = new opcode() { public void handler() { DE((DE() + 1) & 0xFFFF);													}}; /* INC  DE		  */
    opcode op_23 = new opcode() { public void handler() { HL((HL() + 1) & 0xFFFF);													}}; /* INC  HL		  */
    opcode op_33 = new opcode() { public void handler() { Z80.SP = (Z80.SP + 1) & 0xFFFF;													}}; /* INC  SP		  */    
    opcode dd_23 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX = (Z80.IX + 1) & 0xFFFF;											}}; /* INC  IX		  */
    opcode fd_23 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY = (Z80.IY + 1) & 0xFFFF;											}}; /* INC  IY		  */
    /**********************************************************
    * DEC opcodes
    **********************************************************/
    opcode op_05 = new opcode() { public void handler() { Z80.B = DEC(Z80.B);											}}; /* DEC  B 		  */
    opcode op_0d = new opcode() { public void handler() { Z80.C = DEC(Z80.C);											}}; /* DEC  C 		  */
    opcode op_15 = new opcode() { public void handler() { Z80.D = DEC(Z80.D);											}}; /* DEC  D 		  */
    opcode op_1d = new opcode() { public void handler() { Z80.E = DEC(Z80.E);											}}; /* DEC  E 		  */
    opcode op_25 = new opcode() { public void handler() { Z80.H = DEC(Z80.H);											}}; /* DEC  H 		  */
    opcode op_2d = new opcode() { public void handler() { Z80.L = DEC(Z80.L);											}}; /* DEC  L 		  */
    opcode op_3d = new opcode() { public void handler() { Z80.A = DEC(Z80.A);											}}; /* DEC  A 		  */
    opcode dd_35 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); WM( EA, DEC(RM(EA)) );						}}; /* DEC  (IX+o)	  */
    opcode fd_35 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); WM( EA, DEC(RM(EA)) );						}}; /* DEC  (IY+o)	  */
    opcode op_35 = new opcode() { public void handler() { WM( HL(), DEC(RM(HL())) );								}}; /* DEC  (HL)		  */
    opcode op_0b = new opcode() { public void handler() { BC((BC() - 1) & 0xFFFF);									}}; /* DEC  BC		  */
    opcode op_1b = new opcode() { public void handler() { DE((DE() - 1) & 0xFFFF);									}}; /* DEC  DE		  */
    opcode op_2b = new opcode() { public void handler() { HL((HL() - 1) & 0xFFFF);									}}; /* DEC  HL		  */
    opcode op_3b = new opcode() { public void handler() { Z80.SP = (Z80.SP - 1) & 0xFFFF;													}}; /* DEC  SP		  */
    opcode dd_2b = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX = (Z80.IX - 1) & 0xFFFF;											}}; /* DEC  IX		  */
    opcode fd_2b = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY = (Z80.IY - 1) & 0xFFFF;											}}; /* DEC  IY		  */
    /**********************************************************
    * ARG() opcodes
    **********************************************************/
    opcode op_06 = new opcode() { public void handler() { Z80.B = ARG(); 											}}; /* LD   B,n		  */
    opcode op_0e = new opcode() { public void handler() { Z80.C = ARG(); 											}}; /* LD   C,n		  */
    opcode op_16 = new opcode() { public void handler() { Z80.D = ARG(); 											}}; /* LD   D,n		  */
    opcode op_1e = new opcode() { public void handler() { Z80.E = ARG(); 											}}; /* LD   E,n		  */
    opcode op_26 = new opcode() { public void handler() { Z80.H = ARG(); 											}}; /* LD   H,n		  */
    opcode op_2e = new opcode() { public void handler() { Z80.L = ARG(); 											}}; /* LD   L,n		  */
    opcode op_3e = new opcode() { public void handler() { Z80.A = ARG(); 											}}; /* LD   A,n		  */
    /**********************************************************
    * RM opcodes
    **********************************************************/
    opcode fd_46 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); Z80.B = RM(EA); 								}}; /* LD   B,(IY+o)	  */
    opcode fd_4e = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); Z80.C = RM(EA); 								}}; /* LD   C,(IY+o)	  */
    opcode fd_56 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); Z80.D = RM(EA); 								}}; /* LD   D,(IY+o)	  */
    opcode fd_5e = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); Z80.E = RM(EA); 								}}; /* LD   E,(IY+o)	  */
    opcode fd_66 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); Z80.H = RM(EA); 								}}; /* LD   H,(IY+o)	  */
    opcode fd_6e = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); Z80.L = RM(EA); 								}}; /* LD   L,(IY+o)	  */
    opcode fd_7e = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); Z80.A = RM(EA); 								}}; /* LD   A,(IY+o)	  */
    opcode dd_46 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); Z80.B = RM(EA); 								}}; /* LD   B,(IX+o)	  */
    opcode dd_4e = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); Z80.C = RM(EA); 								}}; /* LD   C,(IX+o)	  */
    opcode dd_56 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); Z80.D = RM(EA); 								}}; /* LD   D,(IX+o)	  */
    opcode dd_5e = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); Z80.E = RM(EA); 								}}; /* LD   E,(IX+o)	  */
    opcode dd_66 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); Z80.H = RM(EA); 								}}; /* LD   H,(IX+o)	  */
    opcode dd_6e = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); Z80.L = RM(EA); 								}}; /* LD   L,(IX+o)	  */
    opcode dd_7e = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); Z80.A = RM(EA); 								}}; /* LD   A,(IX+o)	  */
    opcode op_0a = new opcode() { public void handler() { Z80.A = RM(BC()); Z80.WZ=(BC()+1) & 0xFFFF;											}}; /* LD   A,(BC)	  */
    opcode op_1a = new opcode() { public void handler() { Z80.A = RM(DE()); Z80.WZ = (DE() + 1) & 0xFFFF;											}}; /* LD   A,(DE)	  */
    opcode op_3a = new opcode() { public void handler() { EA = ARG16(); Z80.A = RM( EA ); Z80.WZ = (EA + 1) & 0xFFFF;							}}; /* LD   A,(w) 	  */
    /**********************************************************
    * WM opcodes
    **********************************************************/
    opcode fd_36 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); WM( EA, ARG()); 							}}; /* LD   (IY+o),n	  */
    opcode fd_70 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); WM( EA, Z80.B);								}}; /* LD   (IY+o),B	  */
    opcode fd_71 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); WM( EA, Z80.C);								}}; /* LD   (IY+o),C	  */
    opcode fd_72 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); WM( EA, Z80.D);								}}; /* LD   (IY+o),D	  */
    opcode fd_73 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); WM( EA, Z80.E);								}}; /* LD   (IY+o),E	  */
    opcode fd_74 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); WM( EA, Z80.H);								}}; /* LD   (IY+o),H	  */
    opcode fd_75 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); WM( EA, Z80.L);								}}; /* LD   (IY+o),L	  */
    opcode fd_77 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); WM( EA, Z80.A);								}}; /* LD   (IY+o),A	  */
    opcode dd_36 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); WM( EA, ARG()); 							}}; /* LD   (IX+o),n	  */
    opcode dd_70 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); WM( EA, Z80.B);								}}; /* LD   (IX+o),B	  */
    opcode dd_71 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); WM( EA, Z80.C);								}}; /* LD   (IX+o),C	  */
    opcode dd_72 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); WM( EA, Z80.D);								}}; /* LD   (IX+o),D	  */
    opcode dd_73 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); WM( EA, Z80.E);								}}; /* LD   (IX+o),E	  */
    opcode dd_74 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); WM( EA, Z80.H);								}}; /* LD   (IX+o),H	  */
    opcode dd_75 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); WM( EA, Z80.L);								}}; /* LD   (IX+o),L	  */
    opcode dd_77 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); WM( EA, Z80.A);								}}; /* LD   (IX+o),A	  */
    opcode op_70 = new opcode() { public void handler() { WM( HL(), Z80.B);											}}; /* LD   (HL),B	  */
    opcode op_71 = new opcode() { public void handler() { WM( HL(), Z80.C);											}}; /* LD   (HL),C	  */
    opcode op_72 = new opcode() { public void handler() { WM( HL(), Z80.D);											}}; /* LD   (HL),D	  */
    opcode op_73 = new opcode() { public void handler() { WM( HL(), Z80.E);											}}; /* LD   (HL),E	  */
    opcode op_74 = new opcode() { public void handler() { WM( HL(), Z80.H);											}}; /* LD   (HL),H	  */
    opcode op_75 = new opcode() { public void handler() { WM( HL(), Z80.L);											}}; /* LD   (HL),L	  */
    opcode op_77 = new opcode() { public void handler() { WM( HL(), Z80.A);											}}; /* LD   (HL),A	  */
    opcode op_36 = new opcode() { public void handler() { WM( HL(), ARG() );										}}; /* LD   (HL),n	  */
    /**********************************************************
    * RLC opcodes
    **********************************************************/
    opcode cb_00 = new opcode() { public void handler() { Z80.B = RLC(Z80.B);											}}; /* RLC  B 		  */
    opcode cb_01 = new opcode() { public void handler() { Z80.C = RLC(Z80.C);											}}; /* RLC  C 		  */
    opcode cb_02 = new opcode() { public void handler() { Z80.D = RLC(Z80.D);											}}; /* RLC  D 		  */
    opcode cb_03 = new opcode() { public void handler() { Z80.E = RLC(Z80.E);											}}; /* RLC  E 		  */
    opcode cb_04 = new opcode() { public void handler() { Z80.H = RLC(Z80.H);											}}; /* RLC  H 		  */
    opcode cb_05 = new opcode() { public void handler() { Z80.L = RLC(Z80.L);											}}; /* RLC  L 		  */
    opcode cb_06 = new opcode() { public void handler() { WM( HL(), RLC(RM(HL())) );								}}; /* RLC  (HL)		  */
    opcode cb_07 = new opcode() { public void handler() { Z80.A = RLC(Z80.A);											}}; /* RLC  A 		  */
    opcode xycb_00 = new opcode() { public void handler() { Z80.B = RLC( RM(EA) ); WM( EA,Z80.B);						}}; /* RLC  B=(XY+o)	  */
    opcode xycb_01 = new opcode() { public void handler() { Z80.C = RLC( RM(EA) ); WM( EA,Z80.C);						}}; /* RLC  C=(XY+o)	  */
    opcode xycb_02 = new opcode() { public void handler() { Z80.D = RLC( RM(EA) ); WM( EA,Z80.D);						}}; /* RLC  D=(XY+o)	  */
    opcode xycb_03 = new opcode() { public void handler() { Z80.E = RLC( RM(EA) ); WM( EA,Z80.E);						}}; /* RLC  E=(XY+o)	  */
    opcode xycb_04 = new opcode() { public void handler() { Z80.H = RLC( RM(EA) ); WM( EA,Z80.H);						}}; /* RLC  H=(XY+o)	  */
    opcode xycb_05 = new opcode() { public void handler() { Z80.L = RLC( RM(EA) ); WM( EA,Z80.L);						}}; /* RLC  L=(XY+o)	  */
    opcode xycb_06 = new opcode() { public void handler() { WM( EA, RLC( RM(EA) ) );								}}; /* RLC  (XY+o)	  */
    opcode xycb_07 = new opcode() { public void handler() { Z80.A = RLC( RM(EA) ); WM( EA,Z80.A);						}}; /* RLC  A=(XY+o)	  */
    /**********************************************************
    * RRC opcodes
    **********************************************************/
    opcode cb_08 = new opcode() { public void handler() { Z80.B = RRC(Z80.B);											}}; /* RRC  B 		  */
    opcode cb_09 = new opcode() { public void handler() { Z80.C = RRC(Z80.C);											}}; /* RRC  C 		  */
    opcode cb_0a = new opcode() { public void handler() { Z80.D = RRC(Z80.D);											}}; /* RRC  D 		  */
    opcode cb_0b = new opcode() { public void handler() { Z80.E = RRC(Z80.E);											}}; /* RRC  E 		  */
    opcode cb_0c = new opcode() { public void handler() { Z80.H = RRC(Z80.H);											}}; /* RRC  H 		  */
    opcode cb_0d = new opcode() { public void handler() { Z80.L = RRC(Z80.L);											}}; /* RRC  L 		  */
    opcode cb_0e = new opcode() { public void handler() { WM( HL(), RRC(RM(HL())) );								}}; /* RRC  (HL)		  */
    opcode cb_0f = new opcode() { public void handler() { Z80.A = RRC(Z80.A);											}}; /* RRC  A 		  */
    opcode xycb_08 = new opcode() { public void handler() { Z80.B = RRC( RM(EA) ); WM( EA,Z80.B);						}}; /* RRC  B=(XY+o)	  */
    opcode xycb_09 = new opcode() { public void handler() { Z80.C = RRC( RM(EA) ); WM( EA,Z80.C);						}}; /* RRC  C=(XY+o)	  */
    opcode xycb_0a = new opcode() { public void handler() { Z80.D = RRC( RM(EA) ); WM( EA,Z80.D);						}}; /* RRC  D=(XY+o)	  */
    opcode xycb_0b = new opcode() { public void handler() { Z80.E = RRC( RM(EA) ); WM( EA,Z80.E);						}}; /* RRC  E=(XY+o)	  */
    opcode xycb_0c = new opcode() { public void handler() { Z80.H = RRC( RM(EA) ); WM( EA,Z80.H);						}}; /* RRC  H=(XY+o)	  */
    opcode xycb_0d = new opcode() { public void handler() { Z80.L = RRC( RM(EA) ); WM( EA,Z80.L);						}}; /* RRC  L=(XY+o)	  */
    opcode xycb_0e = new opcode() { public void handler() { WM( EA,RRC( RM(EA) ) );								}}; /* RRC  (XY+o)	  */
    opcode xycb_0f = new opcode() { public void handler() { Z80.A = RRC( RM(EA) ); WM( EA,Z80.A);						}}; /* RRC  A=(XY+o)	  */
    /**********************************************************
    * RL opcodes
    **********************************************************/
    opcode cb_10 = new opcode() { public void handler() { Z80.B = RL(Z80.B);											}}; /* RL   B 		  */
    opcode cb_11 = new opcode() { public void handler() { Z80.C = RL(Z80.C);											}}; /* RL   C 		  */
    opcode cb_12 = new opcode() { public void handler() { Z80.D = RL(Z80.D);											}}; /* RL   D 		  */
    opcode cb_13 = new opcode() { public void handler() { Z80.E = RL(Z80.E);											}}; /* RL   E 		  */
    opcode cb_14 = new opcode() { public void handler() { Z80.H = RL(Z80.H);											}}; /* RL   H 		  */
    opcode cb_15 = new opcode() { public void handler() { Z80.L = RL(Z80.L);											}}; /* RL   L 		  */
    opcode cb_16 = new opcode() { public void handler() { WM( HL(), RL(RM(HL())) ); 								}}; /* RL   (HL)		  */
    opcode cb_17 = new opcode() { public void handler() { Z80.A = RL(Z80.A);											}}; /* RL   A 		  */
    opcode xycb_10 = new opcode() { public void handler() { Z80.B = RL( RM(EA) ); WM( EA,Z80.B);						}}; /* RL   B=(XY+o)	  */
    opcode xycb_11 = new opcode() { public void handler() { Z80.C = RL( RM(EA) ); WM( EA,Z80.C);						}}; /* RL   C=(XY+o)	  */
    opcode xycb_12 = new opcode() { public void handler() { Z80.D = RL( RM(EA) ); WM( EA,Z80.D);						}}; /* RL   D=(XY+o)	  */
    opcode xycb_13 = new opcode() { public void handler() { Z80.E = RL( RM(EA) ); WM( EA,Z80.E);						}}; /* RL   E=(XY+o)	  */
    opcode xycb_14 = new opcode() { public void handler() { Z80.H = RL( RM(EA) ); WM( EA,Z80.H);						}}; /* RL   H=(XY+o)	  */
    opcode xycb_15 = new opcode() { public void handler() { Z80.L = RL( RM(EA) ); WM( EA,Z80.L);						}}; /* RL   L=(XY+o)	  */
    opcode xycb_16 = new opcode() { public void handler() { WM( EA,RL( RM(EA) ) );								}}; /* RL   (XY+o)	  */
    opcode xycb_17 = new opcode() { public void handler() { Z80.A = RL( RM(EA) ); WM( EA,Z80.A);						}}; /* RL   A=(XY+o)	  */
    /**********************************************************
    * RR opcodes
    **********************************************************/
    opcode cb_18 = new opcode() { public void handler() { Z80.B = RR(Z80.B);											}}; /* RR   B 		  */
    opcode cb_19 = new opcode() { public void handler() { Z80.C = RR(Z80.C);											}}; /* RR   C 		  */
    opcode cb_1a = new opcode() { public void handler() { Z80.D = RR(Z80.D);											}}; /* RR   D 		  */
    opcode cb_1b = new opcode() { public void handler() { Z80.E = RR(Z80.E);											}}; /* RR   E 		  */
    opcode cb_1c = new opcode() { public void handler() { Z80.H = RR(Z80.H);											}}; /* RR   H 		  */
    opcode cb_1d = new opcode() { public void handler() { Z80.L = RR(Z80.L);											}}; /* RR   L 		  */
    opcode cb_1e = new opcode() { public void handler() { WM( HL(), RR(RM(HL())) ); 								}}; /* RR   (HL)		  */
    opcode cb_1f = new opcode() { public void handler() { Z80.A = RR(Z80.A);											}}; /* RR   A 		  */
    opcode xycb_18 = new opcode() { public void handler() { Z80.B = RR( RM(EA) ); WM( EA,Z80.B);						}}; /* RR   B=(XY+o)	  */
    opcode xycb_19 = new opcode() { public void handler() { Z80.C = RR( RM(EA) ); WM( EA,Z80.C);						}}; /* RR   C=(XY+o)	  */
    opcode xycb_1a = new opcode() { public void handler() { Z80.D = RR( RM(EA) ); WM( EA,Z80.D);						}}; /* RR   D=(XY+o)	  */
    opcode xycb_1b = new opcode() { public void handler() { Z80.E = RR( RM(EA) ); WM( EA,Z80.E);						}}; /* RR   E=(XY+o)	  */
    opcode xycb_1c = new opcode() { public void handler() { Z80.H = RR( RM(EA) ); WM( EA,Z80.H);						}}; /* RR   H=(XY+o)	  */
    opcode xycb_1d = new opcode() { public void handler() { Z80.L = RR( RM(EA) ); WM( EA,Z80.L);						}}; /* RR   L=(XY+o)	  */
    opcode xycb_1e = new opcode() { public void handler() { WM( EA,RR( RM(EA) ) );								}}; /* RR   (XY+o)	  */
    opcode xycb_1f = new opcode() { public void handler() { Z80.A = RR( RM(EA) ); WM( EA,Z80.A);						}}; /* RR   A=(XY+o)	  */
    /**********************************************************
    * SLA opcodes
    **********************************************************/
    opcode cb_20 = new opcode() { public void handler() { Z80.B = SLA(Z80.B);											}}; /* SLA  B 		  */
    opcode cb_21 = new opcode() { public void handler() { Z80.C = SLA(Z80.C);											}}; /* SLA  C 		  */
    opcode cb_22 = new opcode() { public void handler() { Z80.D = SLA(Z80.D);											}}; /* SLA  D 		  */
    opcode cb_23 = new opcode() { public void handler() { Z80.E = SLA(Z80.E);											}}; /* SLA  E 		  */
    opcode cb_24 = new opcode() { public void handler() { Z80.H = SLA(Z80.H);											}}; /* SLA  H 		  */
    opcode cb_25 = new opcode() { public void handler() { Z80.L = SLA(Z80.L);											}}; /* SLA  L 		  */
    opcode cb_26 = new opcode() { public void handler() { WM( HL(), SLA(RM(HL())) );								}}; /* SLA  (HL)		  */
    opcode cb_27 = new opcode() { public void handler() { Z80.A = SLA(Z80.A);											}}; /* SLA  A 		  */
    opcode xycb_20 = new opcode() { public void handler() { Z80.B = SLA( RM(EA) ); WM( EA,Z80.B);						}}; /* SLA  B=(XY+o)	  */
    opcode xycb_21 = new opcode() { public void handler() { Z80.C = SLA( RM(EA) ); WM( EA,Z80.C);						}}; /* SLA  C=(XY+o)	  */
    opcode xycb_22 = new opcode() { public void handler() { Z80.D = SLA( RM(EA) ); WM( EA,Z80.D);						}}; /* SLA  D=(XY+o)	  */
    opcode xycb_23 = new opcode() { public void handler() { Z80.E = SLA( RM(EA) ); WM( EA,Z80.E);						}}; /* SLA  E=(XY+o)	  */
    opcode xycb_24 = new opcode() { public void handler() { Z80.H = SLA( RM(EA) ); WM( EA,Z80.H);						}}; /* SLA  H=(XY+o)	  */
    opcode xycb_25 = new opcode() { public void handler() { Z80.L = SLA( RM(EA) ); WM( EA,Z80.L);						}}; /* SLA  L=(XY+o)	  */
    opcode xycb_26 = new opcode() { public void handler() { WM( EA,SLA( RM(EA) ) );								}}; /* SLA  (XY+o)	  */
    opcode xycb_27 = new opcode() { public void handler() { Z80.A = SLA( RM(EA) ); WM( EA,Z80.A);						}}; /* SLA  A=(XY+o)	  */
    /**********************************************************
    * SRA opcodes
    **********************************************************/
    opcode cb_28 = new opcode() { public void handler() { Z80.B = SRA(Z80.B);											}}; /* SRA  B 		  */
    opcode cb_29 = new opcode() { public void handler() { Z80.C = SRA(Z80.C);											}}; /* SRA  C 		  */
    opcode cb_2a = new opcode() { public void handler() { Z80.D = SRA(Z80.D);											}}; /* SRA  D 		  */
    opcode cb_2b = new opcode() { public void handler() { Z80.E = SRA(Z80.E);											}}; /* SRA  E 		  */
    opcode cb_2c = new opcode() { public void handler() { Z80.H = SRA(Z80.H);											}}; /* SRA  H 		  */
    opcode cb_2d = new opcode() { public void handler() { Z80.L = SRA(Z80.L);											}}; /* SRA  L 		  */
    opcode cb_2e = new opcode() { public void handler() { WM( HL(), SRA(RM(HL())) );								}}; /* SRA  (HL)		  */
    opcode cb_2f = new opcode() { public void handler() { Z80.A = SRA(Z80.A);											}}; /* SRA  A 		  */
    opcode xycb_28 = new opcode() { public void handler() { Z80.B = SRA( RM(EA) ); WM( EA,Z80.B);						}}; /* SRA  B=(XY+o)	  */
    opcode xycb_29 = new opcode() { public void handler() { Z80.C = SRA( RM(EA) ); WM( EA,Z80.C);						}}; /* SRA  C=(XY+o)	  */
    opcode xycb_2a = new opcode() { public void handler() { Z80.D = SRA( RM(EA) ); WM( EA,Z80.D);						}}; /* SRA  D=(XY+o)	  */
    opcode xycb_2b = new opcode() { public void handler() { Z80.E = SRA( RM(EA) ); WM( EA,Z80.E);						}}; /* SRA  E=(XY+o)	  */
    opcode xycb_2c = new opcode() { public void handler() { Z80.H = SRA( RM(EA) ); WM( EA,Z80.H);						}}; /* SRA  H=(XY+o)	  */
    opcode xycb_2d = new opcode() { public void handler() { Z80.L = SRA( RM(EA) ); WM( EA,Z80.L);						}}; /* SRA  L=(XY+o)	  */
    opcode xycb_2e = new opcode() { public void handler() { WM( EA,SRA( RM(EA) ) );								}}; /* SRA  (XY+o)	  */
    opcode xycb_2f = new opcode() { public void handler() { Z80.A = SRA( RM(EA) ); WM( EA,Z80.A);						}}; /* SRA  A=(XY+o)	  */
    /**********************************************************
    * SLL opcodes
    **********************************************************/
    opcode cb_30 = new opcode() { public void handler() { Z80.B = SLL(Z80.B);											}}; /* SLL  B 		  */
    opcode cb_31 = new opcode() { public void handler() { Z80.C = SLL(Z80.C);											}}; /* SLL  C 		  */
    opcode cb_32 = new opcode() { public void handler() { Z80.D = SLL(Z80.D);											}}; /* SLL  D 		  */
    opcode cb_33 = new opcode() { public void handler() { Z80.E = SLL(Z80.E);											}}; /* SLL  E 		  */
    opcode cb_34 = new opcode() { public void handler() { Z80.H = SLL(Z80.H);											}}; /* SLL  H 		  */
    opcode cb_35 = new opcode() { public void handler() { Z80.L = SLL(Z80.L);											}}; /* SLL  L 		  */
    opcode cb_36 = new opcode() { public void handler() { WM( HL(), SLL(RM(HL())) );								}}; /* SLL  (HL)		  */
    opcode cb_37 = new opcode() { public void handler() { Z80.A = SLL(Z80.A);											}}; /* SLL  A 		  */
    opcode xycb_30 = new opcode() { public void handler() { Z80.B = SLL( RM(EA) ); WM( EA,Z80.B);						}}; /* SLL  B=(XY+o)	  */
    opcode xycb_31 = new opcode() { public void handler() { Z80.C = SLL( RM(EA) ); WM( EA,Z80.C);						}}; /* SLL  C=(XY+o)	  */
    opcode xycb_32 = new opcode() { public void handler() { Z80.D = SLL( RM(EA) ); WM( EA,Z80.D);						}}; /* SLL  D=(XY+o)	  */
    opcode xycb_33 = new opcode() { public void handler() { Z80.E = SLL( RM(EA) ); WM( EA,Z80.E);						}}; /* SLL  E=(XY+o)	  */
    opcode xycb_34 = new opcode() { public void handler() { Z80.H = SLL( RM(EA) ); WM( EA,Z80.H);						}}; /* SLL  H=(XY+o)	  */
    opcode xycb_35 = new opcode() { public void handler() { Z80.L = SLL( RM(EA) ); WM( EA,Z80.L);						}}; /* SLL  L=(XY+o)	  */
    opcode xycb_36 = new opcode() { public void handler() { WM( EA,SLL( RM(EA) ) );								}}; /* SLL  (XY+o)	  */
    opcode xycb_37 = new opcode() { public void handler() { Z80.A = SLL( RM(EA) ); WM( EA,Z80.A);						}}; /* SLL  A=(XY+o)	  */
    /**********************************************************
    * SRL opcodes
    **********************************************************/
    opcode cb_38 = new opcode() { public void handler() { Z80.B = SRL(Z80.B);											}}; /* SRL  B 		  */
    opcode cb_39 = new opcode() { public void handler() { Z80.C = SRL(Z80.C);											}}; /* SRL  C 		  */
    opcode cb_3a = new opcode() { public void handler() { Z80.D = SRL(Z80.D);											}}; /* SRL  D 		  */
    opcode cb_3b = new opcode() { public void handler() { Z80.E = SRL(Z80.E);											}}; /* SRL  E 		  */
    opcode cb_3c = new opcode() { public void handler() { Z80.H = SRL(Z80.H);											}}; /* SRL  H 		  */
    opcode cb_3d = new opcode() { public void handler() { Z80.L = SRL(Z80.L);											}}; /* SRL  L 		  */
    opcode cb_3e = new opcode() { public void handler() { WM( HL(), SRL(RM(HL())) );								}}; /* SRL  (HL)		  */
    opcode cb_3f = new opcode() { public void handler() { Z80.A = SRL(Z80.A);											}}; /* SRL  A 		  */
    opcode xycb_38 = new opcode() { public void handler() { Z80.B = SRL( RM(EA) ); WM( EA,Z80.B);						}}; /* SRL  B=(XY+o)	  */
    opcode xycb_39 = new opcode() { public void handler() { Z80.C = SRL( RM(EA) ); WM( EA,Z80.C);						}}; /* SRL  C=(XY+o)	  */
    opcode xycb_3a = new opcode() { public void handler() { Z80.D = SRL( RM(EA) ); WM( EA,Z80.D);						}}; /* SRL  D=(XY+o)	  */
    opcode xycb_3b = new opcode() { public void handler() { Z80.E = SRL( RM(EA) ); WM( EA,Z80.E);						}}; /* SRL  E=(XY+o)	  */
    opcode xycb_3c = new opcode() { public void handler() { Z80.H = SRL( RM(EA) ); WM( EA,Z80.H);						}}; /* SRL  H=(XY+o)	  */
    opcode xycb_3d = new opcode() { public void handler() { Z80.L = SRL( RM(EA) ); WM( EA,Z80.L);						}}; /* SRL  L=(XY+o)	  */
    opcode xycb_3e = new opcode() { public void handler() { WM( EA,SRL( RM(EA) ) );								}}; /* SRL  (XY+o)	  */
    opcode xycb_3f = new opcode() { public void handler() { Z80.A = SRL( RM(EA) ); WM( EA,Z80.A);						}}; /* SRL  A=(XY+o)	  */
    /**********************************************************
    * BIT_XY opcodes
    **********************************************************/
    opcode xycb_46 = new opcode() { public void handler() { BIT_XY(0,RM(EA)); 									}}; /* BIT  0,(XY+o)	  */
    opcode xycb_4e = new opcode() { public void handler() { BIT_XY(1,RM(EA)); 									}}; /* BIT  1,(XY+o)	  */
    opcode xycb_56 = new opcode() { public void handler() { BIT_XY(2,RM(EA)); 									}}; /* BIT  2,(XY+o)	  */
    opcode xycb_5e = new opcode() { public void handler() { BIT_XY(3,RM(EA)); 									}}; /* BIT  3,(XY+o)	  */
    opcode xycb_66 = new opcode() { public void handler() { BIT_XY(4,RM(EA)); 									}}; /* BIT  4,(XY+o)	  */
    opcode xycb_6e = new opcode() { public void handler() { BIT_XY(5,RM(EA)); 									}}; /* BIT  5,(XY+o)	  */
    opcode xycb_76 = new opcode() { public void handler() { BIT_XY(6,RM(EA)); 									}}; /* BIT  6,(XY+o)	  */
    opcode xycb_7e = new opcode() { public void handler() { BIT_XY(7,RM(EA)); 									}}; /* BIT  7,(XY+o)	  */
    /**********************************************************
    * BIT opcodes
    **********************************************************/
    opcode cb_40 = new opcode() { public void handler() { BIT(0,Z80.B);												}}; /* BIT  0,B		  */
    opcode cb_41 = new opcode() { public void handler() { BIT(0,Z80.C);												}}; /* BIT  0,C		  */
    opcode cb_42 = new opcode() { public void handler() { BIT(0,Z80.D);												}}; /* BIT  0,D		  */
    opcode cb_43 = new opcode() { public void handler() { BIT(0,Z80.E);												}}; /* BIT  0,E		  */
    opcode cb_44 = new opcode() { public void handler() { BIT(0,Z80.H);												}}; /* BIT  0,H		  */
    opcode cb_45 = new opcode() { public void handler() { BIT(0,Z80.L);												}}; /* BIT  0,L		  */
    opcode cb_46 = new opcode() { public void handler() { BIT_HL(0,RM(HL())); 										}}; /* BIT  0,(HL)	  */
    opcode cb_47 = new opcode() { public void handler() { BIT(0,Z80.A);												}}; /* BIT  0,A		  */
    opcode cb_48 = new opcode() { public void handler() { BIT(1,Z80.B);												}}; /* BIT  1,B		  */
    opcode cb_49 = new opcode() { public void handler() { BIT(1,Z80.C);												}}; /* BIT  1,C		  */
    opcode cb_4a = new opcode() { public void handler() { BIT(1,Z80.D);												}}; /* BIT  1,D		  */
    opcode cb_4b = new opcode() { public void handler() { BIT(1,Z80.E);												}}; /* BIT  1,E		  */
    opcode cb_4c = new opcode() { public void handler() { BIT(1,Z80.H);												}}; /* BIT  1,H		  */
    opcode cb_4d = new opcode() { public void handler() { BIT(1,Z80.L);												}}; /* BIT  1,L		  */
    opcode cb_4e = new opcode() { public void handler() { BIT_HL(1,RM(HL())); 										}}; /* BIT  1,(HL)	  */
    opcode cb_4f = new opcode() { public void handler() { BIT(1,Z80.A);												}}; /* BIT  1,A		  */
    opcode cb_50 = new opcode() { public void handler() { BIT(2,Z80.B);												}}; /* BIT  2,B		  */
    opcode cb_51 = new opcode() { public void handler() { BIT(2,Z80.C);												}}; /* BIT  2,C		  */
    opcode cb_52 = new opcode() { public void handler() { BIT(2,Z80.D);												}}; /* BIT  2,D		  */
    opcode cb_53 = new opcode() { public void handler() { BIT(2,Z80.E);												}}; /* BIT  2,E		  */
    opcode cb_54 = new opcode() { public void handler() { BIT(2,Z80.H);												}}; /* BIT  2,H		  */
    opcode cb_55 = new opcode() { public void handler() { BIT(2,Z80.L);												}}; /* BIT  2,L		  */
    opcode cb_56 = new opcode() { public void handler() { BIT_HL(2,RM(HL())); 										}}; /* BIT  2,(HL)	  */
    opcode cb_57 = new opcode() { public void handler() { BIT(2,Z80.A);												}}; /* BIT  2,A		  */
    opcode cb_58 = new opcode() { public void handler() { BIT(3,Z80.B);												}}; /* BIT  3,B		  */
    opcode cb_59 = new opcode() { public void handler() { BIT(3,Z80.C);												}}; /* BIT  3,C		  */
    opcode cb_5a = new opcode() { public void handler() { BIT(3,Z80.D);												}}; /* BIT  3,D		  */
    opcode cb_5b = new opcode() { public void handler() { BIT(3,Z80.E);												}}; /* BIT  3,E		  */
    opcode cb_5c = new opcode() { public void handler() { BIT(3,Z80.H);												}}; /* BIT  3,H		  */
    opcode cb_5d = new opcode() { public void handler() { BIT(3,Z80.L);												}}; /* BIT  3,L		  */
    opcode cb_5e = new opcode() { public void handler() { BIT_HL(3,RM(HL())); 										}}; /* BIT  3,(HL)	  */
    opcode cb_5f = new opcode() { public void handler() { BIT(3,Z80.A);												}}; /* BIT  3,A		  */
    opcode cb_60 = new opcode() { public void handler() { BIT(4,Z80.B);												}}; /* BIT  4,B		  */
    opcode cb_61 = new opcode() { public void handler() { BIT(4,Z80.C);												}}; /* BIT  4,C		  */
    opcode cb_62 = new opcode() { public void handler() { BIT(4,Z80.D);												}}; /* BIT  4,D		  */
    opcode cb_63 = new opcode() { public void handler() { BIT(4,Z80.E);												}}; /* BIT  4,E		  */
    opcode cb_64 = new opcode() { public void handler() { BIT(4,Z80.H);												}}; /* BIT  4,H		  */
    opcode cb_65 = new opcode() { public void handler() { BIT(4,Z80.L);												}}; /* BIT  4,L		  */
    opcode cb_66 = new opcode() { public void handler() { BIT_HL(4,RM(HL())); 										}}; /* BIT  4,(HL)	  */
    opcode cb_67 = new opcode() { public void handler() { BIT(4,Z80.A);												}}; /* BIT  4,A		  */
    opcode cb_68 = new opcode() { public void handler() { BIT(5,Z80.B);												}}; /* BIT  5,B		  */
    opcode cb_69 = new opcode() { public void handler() { BIT(5,Z80.C);												}}; /* BIT  5,C		  */
    opcode cb_6a = new opcode() { public void handler() { BIT(5,Z80.D);												}}; /* BIT  5,D		  */
    opcode cb_6b = new opcode() { public void handler() { BIT(5,Z80.E);												}}; /* BIT  5,E		  */
    opcode cb_6c = new opcode() { public void handler() { BIT(5,Z80.H);												}}; /* BIT  5,H		  */
    opcode cb_6d = new opcode() { public void handler() { BIT(5,Z80.L);												}}; /* BIT  5,L		  */
    opcode cb_6e = new opcode() { public void handler() { BIT_HL(5,RM(HL())); 										}}; /* BIT  5,(HL)	  */
    opcode cb_6f = new opcode() { public void handler() { BIT(5,Z80.A);												}}; /* BIT  5,A		  */
    opcode cb_70 = new opcode() { public void handler() { BIT(6,Z80.B);												}}; /* BIT  6,B		  */
    opcode cb_71 = new opcode() { public void handler() { BIT(6,Z80.C);												}}; /* BIT  6,C		  */
    opcode cb_72 = new opcode() { public void handler() { BIT(6,Z80.D);												}}; /* BIT  6,D		  */
    opcode cb_73 = new opcode() { public void handler() { BIT(6,Z80.E);												}}; /* BIT  6,E		  */
    opcode cb_74 = new opcode() { public void handler() { BIT(6,Z80.H);												}}; /* BIT  6,H		  */
    opcode cb_75 = new opcode() { public void handler() { BIT(6,Z80.L);												}}; /* BIT  6,L		  */
    opcode cb_76 = new opcode() { public void handler() { BIT_HL(6,RM(HL())); 										}}; /* BIT  6,(HL)	  */
    opcode cb_77 = new opcode() { public void handler() { BIT(6,Z80.A);												}}; /* BIT  6,A		  */
    opcode cb_78 = new opcode() { public void handler() { BIT(7,Z80.B);												}}; /* BIT  7,B		  */
    opcode cb_79 = new opcode() { public void handler() { BIT(7,Z80.C);												}}; /* BIT  7,C		  */
    opcode cb_7a = new opcode() { public void handler() { BIT(7,Z80.D);												}}; /* BIT  7,D		  */
    opcode cb_7b = new opcode() { public void handler() { BIT(7,Z80.E);												}}; /* BIT  7,E		  */
    opcode cb_7c = new opcode() { public void handler() { BIT(7,Z80.H);												}}; /* BIT  7,H		  */
    opcode cb_7d = new opcode() { public void handler() { BIT(7,Z80.L);												}}; /* BIT  7,L		  */
    opcode cb_7e = new opcode() { public void handler() { BIT_HL(7,RM(HL())); 										}}; /* BIT  7,(HL)	  */
    opcode cb_7f = new opcode() { public void handler() { BIT(7,Z80.A);												}}; /* BIT  7,A		  */
    /**********************************************************
    * RES opcodes
    **********************************************************/
    opcode cb_80 = new opcode() { public void handler() { Z80.B = RES(0,Z80.B); 										}}; /* RES  0,B		  */
    opcode cb_81 = new opcode() { public void handler() { Z80.C = RES(0,Z80.C); 										}}; /* RES  0,C		  */
    opcode cb_82 = new opcode() { public void handler() { Z80.D = RES(0,Z80.D); 										}}; /* RES  0,D		  */
    opcode cb_83 = new opcode() { public void handler() { Z80.E = RES(0,Z80.E); 										}}; /* RES  0,E		  */
    opcode cb_84 = new opcode() { public void handler() { Z80.H = RES(0,Z80.H); 										}}; /* RES  0,H		  */
    opcode cb_85 = new opcode() { public void handler() { Z80.L = RES(0,Z80.L); 										}}; /* RES  0,L		  */
    opcode cb_86 = new opcode() { public void handler() { WM( HL(), RES(0,RM(HL())) );								}}; /* RES  0,(HL)	  */
    opcode cb_87 = new opcode() { public void handler() { Z80.A = RES(0,Z80.A); 										}}; /* RES  0,A		  */
    opcode cb_88 = new opcode() { public void handler() { Z80.B = RES(1,Z80.B); 										}}; /* RES  1,B		  */
    opcode cb_89 = new opcode() { public void handler() { Z80.C = RES(1,Z80.C); 										}}; /* RES  1,C		  */
    opcode cb_8a = new opcode() { public void handler() { Z80.D = RES(1,Z80.D); 										}}; /* RES  1,D		  */
    opcode cb_8b = new opcode() { public void handler() { Z80.E = RES(1,Z80.E); 										}}; /* RES  1,E		  */
    opcode cb_8c = new opcode() { public void handler() { Z80.H = RES(1,Z80.H); 										}}; /* RES  1,H		  */
    opcode cb_8d = new opcode() { public void handler() { Z80.L = RES(1,Z80.L); 										}}; /* RES  1,L		  */
    opcode cb_8e = new opcode() { public void handler() { WM( HL(), RES(1,RM(HL())) );								}}; /* RES  1,(HL)	  */
    opcode cb_8f = new opcode() { public void handler() { Z80.A = RES(1,Z80.A); 										}}; /* RES  1,A		  */
    opcode cb_90 = new opcode() { public void handler() { Z80.B = RES(2,Z80.B); 										}}; /* RES  2,B		  */
    opcode cb_91 = new opcode() { public void handler() { Z80.C = RES(2,Z80.C); 										}}; /* RES  2,C		  */
    opcode cb_92 = new opcode() { public void handler() { Z80.D = RES(2,Z80.D); 										}}; /* RES  2,D		  */
    opcode cb_93 = new opcode() { public void handler() { Z80.E = RES(2,Z80.E); 										}}; /* RES  2,E		  */
    opcode cb_94 = new opcode() { public void handler() { Z80.H = RES(2,Z80.H); 										}}; /* RES  2,H		  */
    opcode cb_95 = new opcode() { public void handler() { Z80.L = RES(2,Z80.L); 										}}; /* RES  2,L		  */
    opcode cb_96 = new opcode() { public void handler() { WM( HL(), RES(2,RM(HL())) );								}}; /* RES  2,(HL)	  */
    opcode cb_97 = new opcode() { public void handler() { Z80.A = RES(2,Z80.A); 										}}; /* RES  2,A		  */
    opcode cb_98 = new opcode() { public void handler() { Z80.B = RES(3,Z80.B); 										}}; /* RES  3,B		  */
    opcode cb_99 = new opcode() { public void handler() { Z80.C = RES(3,Z80.C); 										}}; /* RES  3,C		  */
    opcode cb_9a = new opcode() { public void handler() { Z80.D = RES(3,Z80.D); 										}}; /* RES  3,D		  */
    opcode cb_9b = new opcode() { public void handler() { Z80.E = RES(3,Z80.E); 										}}; /* RES  3,E		  */
    opcode cb_9c = new opcode() { public void handler() { Z80.H = RES(3,Z80.H); 										}}; /* RES  3,H		  */
    opcode cb_9d = new opcode() { public void handler() { Z80.L = RES(3,Z80.L); 										}}; /* RES  3,L		  */
    opcode cb_9e = new opcode() { public void handler() { WM( HL(), RES(3,RM(HL())) );								}}; /* RES  3,(HL)	  */
    opcode cb_9f = new opcode() { public void handler() { Z80.A = RES(3,Z80.A); 										}}; /* RES  3,A		  */
    opcode cb_a0 = new opcode() { public void handler() { Z80.B = RES(4,Z80.B); 										}}; /* RES  4,B		  */
    opcode cb_a1 = new opcode() { public void handler() { Z80.C = RES(4,Z80.C); 										}}; /* RES  4,C		  */
    opcode cb_a2 = new opcode() { public void handler() { Z80.D = RES(4,Z80.D); 										}}; /* RES  4,D		  */
    opcode cb_a3 = new opcode() { public void handler() { Z80.E = RES(4,Z80.E); 										}}; /* RES  4,E		  */
    opcode cb_a4 = new opcode() { public void handler() { Z80.H = RES(4,Z80.H); 										}}; /* RES  4,H		  */
    opcode cb_a5 = new opcode() { public void handler() { Z80.L = RES(4,Z80.L); 										}}; /* RES  4,L		  */
    opcode cb_a6 = new opcode() { public void handler() { WM( HL(), RES(4,RM(HL())) );								}}; /* RES  4,(HL)	  */
    opcode cb_a7 = new opcode() { public void handler() { Z80.A = RES(4,Z80.A); 										}}; /* RES  4,A		  */
    opcode cb_a8 = new opcode() { public void handler() { Z80.B = RES(5,Z80.B); 										}}; /* RES  5,B		  */
    opcode cb_a9 = new opcode() { public void handler() { Z80.C = RES(5,Z80.C); 										}}; /* RES  5,C		  */
    opcode cb_aa = new opcode() { public void handler() { Z80.D = RES(5,Z80.D); 										}}; /* RES  5,D		  */
    opcode cb_ab = new opcode() { public void handler() { Z80.E = RES(5,Z80.E); 										}}; /* RES  5,E		  */
    opcode cb_ac = new opcode() { public void handler() { Z80.H = RES(5,Z80.H); 										}}; /* RES  5,H		  */
    opcode cb_ad = new opcode() { public void handler() { Z80.L = RES(5,Z80.L); 										}}; /* RES  5,L		  */
    opcode cb_ae = new opcode() { public void handler() { WM( HL(), RES(5,RM(HL())) );								}}; /* RES  5,(HL)	  */
    opcode cb_af = new opcode() { public void handler() { Z80.A = RES(5,Z80.A); 										}}; /* RES  5,A		  */
    opcode cb_b0 = new opcode() { public void handler() { Z80.B = RES(6,Z80.B); 										}}; /* RES  6,B		  */
    opcode cb_b1 = new opcode() { public void handler() { Z80.C = RES(6,Z80.C); 										}}; /* RES  6,C		  */
    opcode cb_b2 = new opcode() { public void handler() { Z80.D = RES(6,Z80.D); 										}}; /* RES  6,D		  */
    opcode cb_b3 = new opcode() { public void handler() { Z80.E = RES(6,Z80.E); 										}}; /* RES  6,E		  */
    opcode cb_b4 = new opcode() { public void handler() { Z80.H = RES(6,Z80.H); 										}}; /* RES  6,H		  */
    opcode cb_b5 = new opcode() { public void handler() { Z80.L = RES(6,Z80.L); 										}}; /* RES  6,L		  */
    opcode cb_b6 = new opcode() { public void handler() { WM( HL(), RES(6,RM(HL())) );								}}; /* RES  6,(HL)	  */
    opcode cb_b7 = new opcode() { public void handler() { Z80.A = RES(6,Z80.A); 										}}; /* RES  6,A		  */
    opcode cb_b8 = new opcode() { public void handler() { Z80.B = RES(7,Z80.B); 										}}; /* RES  7,B		  */
    opcode cb_b9 = new opcode() { public void handler() { Z80.C = RES(7,Z80.C); 										}}; /* RES  7,C		  */
    opcode cb_ba = new opcode() { public void handler() { Z80.D = RES(7,Z80.D); 										}}; /* RES  7,D		  */
    opcode cb_bb = new opcode() { public void handler() { Z80.E = RES(7,Z80.E); 										}}; /* RES  7,E		  */
    opcode cb_bc = new opcode() { public void handler() { Z80.H = RES(7,Z80.H); 										}}; /* RES  7,H		  */
    opcode cb_bd = new opcode() { public void handler() { Z80.L = RES(7,Z80.L); 										}}; /* RES  7,L		  */
    opcode cb_be = new opcode() { public void handler() { WM( HL(), RES(7,RM(HL())) );								}}; /* RES  7,(HL)	  */
    opcode cb_bf = new opcode() { public void handler() { Z80.A = RES(7,Z80.A); 										}}; /* RES  7,A		  */
    opcode xycb_80 = new opcode() { public void handler() { Z80.B = RES(0, RM(EA) ); WM( EA,Z80.B);					}}; /* RES  0,B=(XY+o)  */
    opcode xycb_81 = new opcode() { public void handler() { Z80.C = RES(0, RM(EA) ); WM( EA,Z80.C);					}}; /* RES  0,C=(XY+o)  */
    opcode xycb_82 = new opcode() { public void handler() { Z80.D = RES(0, RM(EA) ); WM( EA,Z80.D);					}}; /* RES  0,D=(XY+o)  */
    opcode xycb_83 = new opcode() { public void handler() { Z80.E = RES(0, RM(EA) ); WM( EA,Z80.E);					}}; /* RES  0,E=(XY+o)  */
    opcode xycb_84 = new opcode() { public void handler() { Z80.H = RES(0, RM(EA) ); WM( EA,Z80.H);					}}; /* RES  0,H=(XY+o)  */
    opcode xycb_85 = new opcode() { public void handler() { Z80.L = RES(0, RM(EA) ); WM( EA,Z80.L);					}}; /* RES  0,L=(XY+o)  */
    opcode xycb_86 = new opcode() { public void handler() { WM( EA, RES(0,RM(EA)) );								}}; /* RES  0,(XY+o)	  */
    opcode xycb_87 = new opcode() { public void handler() { Z80.A = RES(0, RM(EA) ); WM( EA,Z80.A);					}}; /* RES  0,A=(XY+o)  */
    opcode xycb_88 = new opcode() { public void handler() { Z80.B = RES(1, RM(EA) ); WM( EA,Z80.B);					}}; /* RES  1,B=(XY+o)  */
    opcode xycb_89 = new opcode() { public void handler() { Z80.C = RES(1, RM(EA) ); WM( EA,Z80.C);					}}; /* RES  1,C=(XY+o)  */
    opcode xycb_8a = new opcode() { public void handler() { Z80.D = RES(1, RM(EA) ); WM( EA,Z80.D);					}}; /* RES  1,D=(XY+o)  */
    opcode xycb_8b = new opcode() { public void handler() { Z80.E = RES(1, RM(EA) ); WM( EA,Z80.E);					}}; /* RES  1,E=(XY+o)  */
    opcode xycb_8c = new opcode() { public void handler() { Z80.H = RES(1, RM(EA) ); WM( EA,Z80.H);					}}; /* RES  1,H=(XY+o)  */
    opcode xycb_8d = new opcode() { public void handler() { Z80.L = RES(1, RM(EA) ); WM( EA,Z80.L);					}}; /* RES  1,L=(XY+o)  */
    opcode xycb_8e = new opcode() { public void handler() { WM( EA, RES(1,RM(EA)) );								}}; /* RES  1,(XY+o)	  */
    opcode xycb_8f = new opcode() { public void handler() { Z80.A = RES(1, RM(EA) ); WM( EA,Z80.A);					}}; /* RES  1,A=(XY+o)  */
    opcode xycb_90 = new opcode() { public void handler() { Z80.B = RES(2, RM(EA) ); WM( EA,Z80.B);					}}; /* RES  2,B=(XY+o)  */
    opcode xycb_91 = new opcode() { public void handler() { Z80.C = RES(2, RM(EA) ); WM( EA,Z80.C);					}}; /* RES  2,C=(XY+o)  */
    opcode xycb_92 = new opcode() { public void handler() { Z80.D = RES(2, RM(EA) ); WM( EA,Z80.D);					}}; /* RES  2,D=(XY+o)  */
    opcode xycb_93 = new opcode() { public void handler() { Z80.E = RES(2, RM(EA) ); WM( EA,Z80.E);					}}; /* RES  2,E=(XY+o)  */
    opcode xycb_94 = new opcode() { public void handler() { Z80.H = RES(2, RM(EA) ); WM( EA,Z80.H);					}}; /* RES  2,H=(XY+o)  */
    opcode xycb_95 = new opcode() { public void handler() { Z80.L = RES(2, RM(EA) ); WM( EA,Z80.L);					}}; /* RES  2,L=(XY+o)  */
    opcode xycb_96 = new opcode() { public void handler() { WM( EA, RES(2,RM(EA)) );								}}; /* RES  2,(XY+o)	  */
    opcode xycb_97 = new opcode() { public void handler() { Z80.A = RES(2, RM(EA) ); WM( EA,Z80.A);					}}; /* RES  2,A=(XY+o)  */
    opcode xycb_98 = new opcode() { public void handler() { Z80.B = RES(3, RM(EA) ); WM( EA,Z80.B);					}}; /* RES  3,B=(XY+o)  */
    opcode xycb_99 = new opcode() { public void handler() { Z80.C = RES(3, RM(EA) ); WM( EA,Z80.C);					}}; /* RES  3,C=(XY+o)  */
    opcode xycb_9a = new opcode() { public void handler() { Z80.D = RES(3, RM(EA) ); WM( EA,Z80.D);					}}; /* RES  3,D=(XY+o)  */
    opcode xycb_9b = new opcode() { public void handler() { Z80.E = RES(3, RM(EA) ); WM( EA,Z80.E);					}}; /* RES  3,E=(XY+o)  */
    opcode xycb_9c = new opcode() { public void handler() { Z80.H = RES(3, RM(EA) ); WM( EA,Z80.H);					}}; /* RES  3,H=(XY+o)  */
    opcode xycb_9d = new opcode() { public void handler() { Z80.L = RES(3, RM(EA) ); WM( EA,Z80.L);					}}; /* RES  3,L=(XY+o)  */
    opcode xycb_9e = new opcode() { public void handler() { WM( EA, RES(3,RM(EA)) );								}}; /* RES  3,(XY+o)	  */
    opcode xycb_9f = new opcode() { public void handler() { Z80.A = RES(3, RM(EA) ); WM( EA,Z80.A);					}}; /* RES  3,A=(XY+o)  */
    opcode xycb_a0 = new opcode() { public void handler() { Z80.B = RES(4, RM(EA) ); WM( EA,Z80.B);					}}; /* RES  4,B=(XY+o)  */
    opcode xycb_a1 = new opcode() { public void handler() { Z80.C = RES(4, RM(EA) ); WM( EA,Z80.C);					}}; /* RES  4,C=(XY+o)  */
    opcode xycb_a2 = new opcode() { public void handler() { Z80.D = RES(4, RM(EA) ); WM( EA,Z80.D);					}}; /* RES  4,D=(XY+o)  */
    opcode xycb_a3 = new opcode() { public void handler() { Z80.E = RES(4, RM(EA) ); WM( EA,Z80.E);					}}; /* RES  4,E=(XY+o)  */
    opcode xycb_a4 = new opcode() { public void handler() { Z80.H = RES(4, RM(EA) ); WM( EA,Z80.H);					}}; /* RES  4,H=(XY+o)  */
    opcode xycb_a5 = new opcode() { public void handler() { Z80.L = RES(4, RM(EA) ); WM( EA,Z80.L);					}}; /* RES  4,L=(XY+o)  */
    opcode xycb_a6 = new opcode() { public void handler() { WM( EA, RES(4,RM(EA)) );								}}; /* RES  4,(XY+o)	  */
    opcode xycb_a7 = new opcode() { public void handler() { Z80.A = RES(4, RM(EA) ); WM( EA,Z80.A);					}}; /* RES  4,A=(XY+o)  */
    opcode xycb_a8 = new opcode() { public void handler() { Z80.B = RES(5, RM(EA) ); WM( EA,Z80.B);					}}; /* RES  5,B=(XY+o)  */
    opcode xycb_a9 = new opcode() { public void handler() { Z80.C = RES(5, RM(EA) ); WM( EA,Z80.C);					}}; /* RES  5,C=(XY+o)  */
    opcode xycb_aa = new opcode() { public void handler() { Z80.D = RES(5, RM(EA) ); WM( EA,Z80.D);					}}; /* RES  5,D=(XY+o)  */
    opcode xycb_ab = new opcode() { public void handler() { Z80.E = RES(5, RM(EA) ); WM( EA,Z80.E);					}}; /* RES  5,E=(XY+o)  */
    opcode xycb_ac = new opcode() { public void handler() { Z80.H = RES(5, RM(EA) ); WM( EA,Z80.H);					}}; /* RES  5,H=(XY+o)  */
    opcode xycb_ad = new opcode() { public void handler() { Z80.L = RES(5, RM(EA) ); WM( EA,Z80.L);					}}; /* RES  5,L=(XY+o)  */
    opcode xycb_ae = new opcode() { public void handler() { WM( EA, RES(5,RM(EA)) );								}}; /* RES  5,(XY+o)	  */
    opcode xycb_af = new opcode() { public void handler() { Z80.A = RES(5, RM(EA) ); WM( EA,Z80.A);					}}; /* RES  5,A=(XY+o)  */
    opcode xycb_b0 = new opcode() { public void handler() { Z80.B = RES(6, RM(EA) ); WM( EA,Z80.B);					}}; /* RES  6,B=(XY+o)  */
    opcode xycb_b1 = new opcode() { public void handler() { Z80.C = RES(6, RM(EA) ); WM( EA,Z80.C);					}}; /* RES  6,C=(XY+o)  */
    opcode xycb_b2 = new opcode() { public void handler() { Z80.D = RES(6, RM(EA) ); WM( EA,Z80.D);					}}; /* RES  6,D=(XY+o)  */
    opcode xycb_b3 = new opcode() { public void handler() { Z80.E = RES(6, RM(EA) ); WM( EA,Z80.E);					}}; /* RES  6,E=(XY+o)  */
    opcode xycb_b4 = new opcode() { public void handler() { Z80.H = RES(6, RM(EA) ); WM( EA,Z80.H);					}}; /* RES  6,H=(XY+o)  */
    opcode xycb_b5 = new opcode() { public void handler() { Z80.L = RES(6, RM(EA) ); WM( EA,Z80.L);					}}; /* RES  6,L=(XY+o)  */
    opcode xycb_b6 = new opcode() { public void handler() { WM( EA, RES(6,RM(EA)) );								}}; /* RES  6,(XY+o)	  */
    opcode xycb_b7 = new opcode() { public void handler() { Z80.A = RES(6, RM(EA) ); WM( EA,Z80.A);					}}; /* RES  6,A=(XY+o)  */
    opcode xycb_b8 = new opcode() { public void handler() { Z80.B = RES(7, RM(EA) ); WM( EA,Z80.B);					}}; /* RES  7,B=(XY+o)  */
    opcode xycb_b9 = new opcode() { public void handler() { Z80.C = RES(7, RM(EA) ); WM( EA,Z80.C);					}}; /* RES  7,C=(XY+o)  */
    opcode xycb_ba = new opcode() { public void handler() { Z80.D = RES(7, RM(EA) ); WM( EA,Z80.D);					}}; /* RES  7,D=(XY+o)  */
    opcode xycb_bb = new opcode() { public void handler() { Z80.E = RES(7, RM(EA) ); WM( EA,Z80.E);					}}; /* RES  7,E=(XY+o)  */
    opcode xycb_bc = new opcode() { public void handler() { Z80.H = RES(7, RM(EA) ); WM( EA,Z80.H);					}}; /* RES  7,H=(XY+o)  */
    opcode xycb_bd = new opcode() { public void handler() { Z80.L = RES(7, RM(EA) ); WM( EA,Z80.L);					}}; /* RES  7,L=(XY+o)  */
    opcode xycb_be = new opcode() { public void handler() { WM( EA, RES(7,RM(EA)) );								}}; /* RES  7,(XY+o)	  */
    opcode xycb_bf = new opcode() { public void handler() { Z80.A = RES(7, RM(EA) ); WM( EA,Z80.A);					}}; /* RES  7,A=(XY+o)  */
    /**********************************************************
    * SET opcodes
    **********************************************************/
    opcode cb_c0 = new opcode() { public void handler() { Z80.B = SET(0,Z80.B); 										}}; /* SET  0,B		  */
    opcode cb_c1 = new opcode() { public void handler() { Z80.C = SET(0,Z80.C); 										}}; /* SET  0,C		  */
    opcode cb_c2 = new opcode() { public void handler() { Z80.D = SET(0,Z80.D); 										}}; /* SET  0,D		  */
    opcode cb_c3 = new opcode() { public void handler() { Z80.E = SET(0,Z80.E); 										}}; /* SET  0,E		  */
    opcode cb_c4 = new opcode() { public void handler() { Z80.H = SET(0,Z80.H); 										}}; /* SET  0,H		  */
    opcode cb_c5 = new opcode() { public void handler() { Z80.L = SET(0,Z80.L); 										}}; /* SET  0,L		  */
    opcode cb_c6 = new opcode() { public void handler() { WM( HL(), SET(0,RM(HL())) );								}}; /* SET  0,(HL)	  */
    opcode cb_c7 = new opcode() { public void handler() { Z80.A = SET(0,Z80.A); 										}}; /* SET  0,A		  */
    opcode cb_c8 = new opcode() { public void handler() { Z80.B = SET(1,Z80.B); 										}}; /* SET  1,B		  */
    opcode cb_c9 = new opcode() { public void handler() { Z80.C = SET(1,Z80.C); 										}}; /* SET  1,C		  */
    opcode cb_ca = new opcode() { public void handler() { Z80.D = SET(1,Z80.D); 										}}; /* SET  1,D		  */
    opcode cb_cb = new opcode() { public void handler() { Z80.E = SET(1,Z80.E); 										}}; /* SET  1,E		  */
    opcode cb_cc = new opcode() { public void handler() { Z80.H = SET(1,Z80.H); 										}}; /* SET  1,H		  */
    opcode cb_cd = new opcode() { public void handler() { Z80.L = SET(1,Z80.L); 										}}; /* SET  1,L		  */
    opcode cb_ce = new opcode() { public void handler() { WM( HL(), SET(1,RM(HL())) );								}}; /* SET  1,(HL)	  */
    opcode cb_cf = new opcode() { public void handler() { Z80.A = SET(1,Z80.A); 										}}; /* SET  1,A		  */
    opcode cb_d0 = new opcode() { public void handler() { Z80.B = SET(2,Z80.B); 										}}; /* SET  2,B		  */
    opcode cb_d1 = new opcode() { public void handler() { Z80.C = SET(2,Z80.C); 										}}; /* SET  2,C		  */
    opcode cb_d2 = new opcode() { public void handler() { Z80.D = SET(2,Z80.D); 										}}; /* SET  2,D		  */
    opcode cb_d3 = new opcode() { public void handler() { Z80.E = SET(2,Z80.E); 										}}; /* SET  2,E		  */
    opcode cb_d4 = new opcode() { public void handler() { Z80.H = SET(2,Z80.H); 										}}; /* SET  2,H		  */
    opcode cb_d5 = new opcode() { public void handler() { Z80.L = SET(2,Z80.L); 										}}; /* SET  2,L		  */
    opcode cb_d6 = new opcode() { public void handler() { WM( HL(), SET(2,RM(HL())) );								}};/* SET  2,(HL) 	 */
    opcode cb_d7 = new opcode() { public void handler() { Z80.A = SET(2,Z80.A); 										}}; /* SET  2,A		  */
    opcode cb_d8 = new opcode() { public void handler() { Z80.B = SET(3,Z80.B); 										}}; /* SET  3,B		  */
    opcode cb_d9 = new opcode() { public void handler() { Z80.C = SET(3,Z80.C); 										}}; /* SET  3,C		  */
    opcode cb_da = new opcode() { public void handler() { Z80.D = SET(3,Z80.D); 										}}; /* SET  3,D		  */
    opcode cb_db = new opcode() { public void handler() { Z80.E = SET(3,Z80.E); 										}}; /* SET  3,E		  */
    opcode cb_dc = new opcode() { public void handler() { Z80.H = SET(3,Z80.H); 										}}; /* SET  3,H		  */
    opcode cb_dd = new opcode() { public void handler() { Z80.L = SET(3,Z80.L); 										}}; /* SET  3,L		  */
    opcode cb_de = new opcode() { public void handler() { WM( HL(), SET(3,RM(HL())) );								}}; /* SET  3,(HL)	  */
    opcode cb_df = new opcode() { public void handler() { Z80.A = SET(3,Z80.A); 										}}; /* SET  3,A		  */
    opcode cb_e0 = new opcode() { public void handler() { Z80.B = SET(4,Z80.B); 										}}; /* SET  4,B		  */
    opcode cb_e1 = new opcode() { public void handler() { Z80.C = SET(4,Z80.C); 										}}; /* SET  4,C		  */
    opcode cb_e2 = new opcode() { public void handler() { Z80.D = SET(4,Z80.D); 										}}; /* SET  4,D		  */
    opcode cb_e3 = new opcode() { public void handler() { Z80.E = SET(4,Z80.E); 										}}; /* SET  4,E		  */
    opcode cb_e4 = new opcode() { public void handler() { Z80.H = SET(4,Z80.H); 										}}; /* SET  4,H		  */
    opcode cb_e5 = new opcode() { public void handler() { Z80.L = SET(4,Z80.L); 										}}; /* SET  4,L		  */
    opcode cb_e6 = new opcode() { public void handler() { WM( HL(), SET(4,RM(HL())) );								}}; /* SET  4,(HL)	  */
    opcode cb_e7 = new opcode() { public void handler() { Z80.A = SET(4,Z80.A); 										}}; /* SET  4,A		  */
    opcode cb_e8 = new opcode() { public void handler() { Z80.B = SET(5,Z80.B); 										}}; /* SET  5,B		  */
    opcode cb_e9 = new opcode() { public void handler() { Z80.C = SET(5,Z80.C); 										}}; /* SET  5,C		  */
    opcode cb_ea = new opcode() { public void handler() { Z80.D = SET(5,Z80.D); 										}}; /* SET  5,D		  */
    opcode cb_eb = new opcode() { public void handler() { Z80.E = SET(5,Z80.E); 										}}; /* SET  5,E		  */
    opcode cb_ec = new opcode() { public void handler() { Z80.H = SET(5,Z80.H); 										}}; /* SET  5,H		  */
    opcode cb_ed = new opcode() { public void handler() { Z80.L = SET(5,Z80.L); 										}}; /* SET  5,L		  */
    opcode cb_ee = new opcode() { public void handler() { WM( HL(), SET(5,RM(HL())) );								}}; /* SET  5,(HL)	  */
    opcode cb_ef = new opcode() { public void handler() { Z80.A = SET(5,Z80.A); 										}}; /* SET  5,A		  */
    opcode cb_f0 = new opcode() { public void handler() { Z80.B = SET(6,Z80.B); 										}}; /* SET  6,B		  */
    opcode cb_f1 = new opcode() { public void handler() { Z80.C = SET(6,Z80.C); 										}}; /* SET  6,C		  */
    opcode cb_f2 = new opcode() { public void handler() { Z80.D = SET(6,Z80.D); 										}}; /* SET  6,D		  */
    opcode cb_f3 = new opcode() { public void handler() { Z80.E = SET(6,Z80.E); 										}}; /* SET  6,E		  */
    opcode cb_f4 = new opcode() { public void handler() { Z80.H = SET(6,Z80.H); 										}}; /* SET  6,H		  */
    opcode cb_f5 = new opcode() { public void handler() { Z80.L = SET(6,Z80.L); 										}}; /* SET  6,L		  */
    opcode cb_f6 = new opcode() { public void handler() { WM( HL(), SET(6,RM(HL())) );								}}; /* SET  6,(HL)	  */
    opcode cb_f7 = new opcode() { public void handler() { Z80.A = SET(6,Z80.A); 										}}; /* SET  6,A		  */
    opcode cb_f8 = new opcode() { public void handler() { Z80.B = SET(7,Z80.B); 										}}; /* SET  7,B		  */
    opcode cb_f9 = new opcode() { public void handler() { Z80.C = SET(7,Z80.C); 										}}; /* SET  7,C		  */
    opcode cb_fa = new opcode() { public void handler() { Z80.D = SET(7,Z80.D); 										}}; /* SET  7,D		  */
    opcode cb_fb = new opcode() { public void handler() { Z80.E = SET(7,Z80.E); 										}}; /* SET  7,E		  */
    opcode cb_fc = new opcode() { public void handler() { Z80.H = SET(7,Z80.H); 										}}; /* SET  7,H		  */
    opcode cb_fd = new opcode() { public void handler() { Z80.L = SET(7,Z80.L); 										}}; /* SET  7,L		  */
    opcode cb_fe = new opcode() { public void handler() { WM( HL(), SET(7,RM(HL())) );								}}; /* SET  7,(HL)	  */
    opcode cb_ff = new opcode() { public void handler() { Z80.A = SET(7,Z80.A); 										}}; /* SET  7,A		  */
    opcode xycb_c0 = new opcode() { public void handler() { Z80.B = SET(0, RM(EA) ); WM( EA,Z80.B);					}}; /* SET  0,B=(XY+o)  */
    opcode xycb_c1 = new opcode() { public void handler() { Z80.C = SET(0, RM(EA) ); WM( EA,Z80.C);					}}; /* SET  0,C=(XY+o)  */
    opcode xycb_c2 = new opcode() { public void handler() { Z80.D = SET(0, RM(EA) ); WM( EA,Z80.D);					}}; /* SET  0,D=(XY+o)  */
    opcode xycb_c3 = new opcode() { public void handler() { Z80.E = SET(0, RM(EA) ); WM( EA,Z80.E);					}}; /* SET  0,E=(XY+o)  */
    opcode xycb_c4 = new opcode() { public void handler() { Z80.H = SET(0, RM(EA) ); WM( EA,Z80.H);					}}; /* SET  0,H=(XY+o)  */
    opcode xycb_c5 = new opcode() { public void handler() { Z80.L = SET(0, RM(EA) ); WM( EA,Z80.L);					}}; /* SET  0,L=(XY+o)  */
    opcode xycb_c6 = new opcode() { public void handler() { WM( EA, SET(0,RM(EA)) );								}}; /* SET  0,(XY+o)	  */
    opcode xycb_c7 = new opcode() { public void handler() { Z80.A = SET(0, RM(EA) ); WM( EA,Z80.A);					}}; /* SET  0,A=(XY+o)  */
    opcode xycb_c8 = new opcode() { public void handler() { Z80.B = SET(1, RM(EA) ); WM( EA,Z80.B);					}}; /* SET  1,B=(XY+o)  */
    opcode xycb_c9 = new opcode() { public void handler() { Z80.C = SET(1, RM(EA) ); WM( EA,Z80.C);					}}; /* SET  1,C=(XY+o)  */
    opcode xycb_ca = new opcode() { public void handler() { Z80.D = SET(1, RM(EA) ); WM( EA,Z80.D);					}}; /* SET  1,D=(XY+o)  */
    opcode xycb_cb = new opcode() { public void handler() { Z80.E = SET(1, RM(EA) ); WM( EA,Z80.E);					}}; /* SET  1,E=(XY+o)  */
    opcode xycb_cc = new opcode() { public void handler() { Z80.H = SET(1, RM(EA) ); WM( EA,Z80.H);					}}; /* SET  1,H=(XY+o)  */
    opcode xycb_cd = new opcode() { public void handler() { Z80.L = SET(1, RM(EA) ); WM( EA,Z80.L);					}}; /* SET  1,L=(XY+o)  */
    opcode xycb_ce = new opcode() { public void handler() { WM( EA, SET(1,RM(EA)) );								}}; /* SET  1,(XY+o)	  */
    opcode xycb_cf = new opcode() { public void handler() { Z80.A = SET(1, RM(EA) ); WM( EA,Z80.A);					}}; /* SET  1,A=(XY+o)  */
    opcode xycb_d0 = new opcode() { public void handler() { Z80.B = SET(2, RM(EA) ); WM( EA,Z80.B);					}}; /* SET  2,B=(XY+o)  */
    opcode xycb_d1 = new opcode() { public void handler() { Z80.C = SET(2, RM(EA) ); WM( EA,Z80.C);					}}; /* SET  2,C=(XY+o)  */
    opcode xycb_d2 = new opcode() { public void handler() { Z80.D = SET(2, RM(EA) ); WM( EA,Z80.D);					}}; /* SET  2,D=(XY+o)  */
    opcode xycb_d3 = new opcode() { public void handler() { Z80.E = SET(2, RM(EA) ); WM( EA,Z80.E);					}}; /* SET  2,E=(XY+o)  */
    opcode xycb_d4 = new opcode() { public void handler() { Z80.H = SET(2, RM(EA) ); WM( EA,Z80.H);					}}; /* SET  2,H=(XY+o)  */
    opcode xycb_d5 = new opcode() { public void handler() { Z80.L = SET(2, RM(EA) ); WM( EA,Z80.L);					}}; /* SET  2,L=(XY+o)  */
    opcode xycb_d6 = new opcode() { public void handler() { WM( EA, SET(2,RM(EA)) );								}}; /* SET  2,(XY+o)	  */
    opcode xycb_d7 = new opcode() { public void handler() { Z80.A = SET(2, RM(EA) ); WM( EA,Z80.A);					}}; /* SET  2,A=(XY+o)  */
    opcode xycb_d8 = new opcode() { public void handler() { Z80.B = SET(3, RM(EA) ); WM( EA,Z80.B);					}}; /* SET  3,B=(XY+o)  */
    opcode xycb_d9 = new opcode() { public void handler() { Z80.C = SET(3, RM(EA) ); WM( EA,Z80.C);					}}; /* SET  3,C=(XY+o)  */
    opcode xycb_da = new opcode() { public void handler() { Z80.D = SET(3, RM(EA) ); WM( EA,Z80.D);					}}; /* SET  3,D=(XY+o)  */
    opcode xycb_db = new opcode() { public void handler() { Z80.E = SET(3, RM(EA) ); WM( EA,Z80.E);					}}; /* SET  3,E=(XY+o)  */
    opcode xycb_dc = new opcode() { public void handler() { Z80.H = SET(3, RM(EA) ); WM( EA,Z80.H);					}}; /* SET  3,H=(XY+o)  */
    opcode xycb_dd = new opcode() { public void handler() { Z80.L = SET(3, RM(EA) ); WM( EA,Z80.L);					}}; /* SET  3,L=(XY+o)  */
    opcode xycb_de = new opcode() { public void handler() { WM( EA, SET(3,RM(EA)) );								}}; /* SET  3,(XY+o)	  */
    opcode xycb_df = new opcode() { public void handler() { Z80.A = SET(3, RM(EA) ); WM( EA,Z80.A);					}}; /* SET  3,A=(XY+o)  */
    opcode xycb_e0 = new opcode() { public void handler() { Z80.B = SET(4, RM(EA) ); WM( EA,Z80.B);					}}; /* SET  4,B=(XY+o)  */
    opcode xycb_e1 = new opcode() { public void handler() { Z80.C = SET(4, RM(EA) ); WM( EA,Z80.C);					}}; /* SET  4,C=(XY+o)  */
    opcode xycb_e2 = new opcode() { public void handler() { Z80.D = SET(4, RM(EA) ); WM( EA,Z80.D);					}}; /* SET  4,D=(XY+o)  */
    opcode xycb_e3 = new opcode() { public void handler() { Z80.E = SET(4, RM(EA) ); WM( EA,Z80.E);					}}; /* SET  4,E=(XY+o)  */
    opcode xycb_e4 = new opcode() { public void handler() { Z80.H = SET(4, RM(EA) ); WM( EA,Z80.H);					}}; /* SET  4,H=(XY+o)  */
    opcode xycb_e5 = new opcode() { public void handler() { Z80.L = SET(4, RM(EA) ); WM( EA,Z80.L);					}}; /* SET  4,L=(XY+o)  */
    opcode xycb_e6 = new opcode() { public void handler() { WM( EA, SET(4,RM(EA)) );								}}; /* SET  4,(XY+o)	  */
    opcode xycb_e7 = new opcode() { public void handler() { Z80.A = SET(4, RM(EA) ); WM( EA,Z80.A);					}}; /* SET  4,A=(XY+o)  */
    opcode xycb_e8 = new opcode() { public void handler() { Z80.B = SET(5, RM(EA) ); WM( EA,Z80.B);					}}; /* SET  5,B=(XY+o)  */
    opcode xycb_e9 = new opcode() { public void handler() { Z80.C = SET(5, RM(EA) ); WM( EA,Z80.C);					}}; /* SET  5,C=(XY+o)  */
    opcode xycb_ea = new opcode() { public void handler() { Z80.D = SET(5, RM(EA) ); WM( EA,Z80.D);					}}; /* SET  5,D=(XY+o)  */
    opcode xycb_eb = new opcode() { public void handler() { Z80.E = SET(5, RM(EA) ); WM( EA,Z80.E);					}}; /* SET  5,E=(XY+o)  */
    opcode xycb_ec = new opcode() { public void handler() { Z80.H = SET(5, RM(EA) ); WM( EA,Z80.H);					}}; /* SET  5,H=(XY+o)  */
    opcode xycb_ed = new opcode() { public void handler() { Z80.L = SET(5, RM(EA) ); WM( EA,Z80.L);					}}; /* SET  5,L=(XY+o)  */
    opcode xycb_ee = new opcode() { public void handler() { WM( EA, SET(5,RM(EA)) );								}}; /* SET  5,(XY+o)	  */
    opcode xycb_ef = new opcode() { public void handler() { Z80.A = SET(5, RM(EA) ); WM( EA,Z80.A);					}}; /* SET  5,A=(XY+o)  */
    opcode xycb_f0 = new opcode() { public void handler() { Z80.B = SET(6, RM(EA) ); WM( EA,Z80.B);					}}; /* SET  6,B=(XY+o)  */
    opcode xycb_f1 = new opcode() { public void handler() { Z80.C = SET(6, RM(EA) ); WM( EA,Z80.C);					}}; /* SET  6,C=(XY+o)  */
    opcode xycb_f2 = new opcode() { public void handler() { Z80.D = SET(6, RM(EA) ); WM( EA,Z80.D);					}}; /* SET  6,D=(XY+o)  */
    opcode xycb_f3 = new opcode() { public void handler() { Z80.E = SET(6, RM(EA) ); WM( EA,Z80.E);					}}; /* SET  6,E=(XY+o)  */
    opcode xycb_f4 = new opcode() { public void handler() { Z80.H = SET(6, RM(EA) ); WM( EA,Z80.H);					}}; /* SET  6,H=(XY+o)  */
    opcode xycb_f5 = new opcode() { public void handler() { Z80.L = SET(6, RM(EA) ); WM( EA,Z80.L);					}}; /* SET  6,L=(XY+o)  */
    opcode xycb_f6 = new opcode() { public void handler() { WM( EA, SET(6,RM(EA)) );								}}; /* SET  6,(XY+o)	  */
    opcode xycb_f7 = new opcode() { public void handler() { Z80.A = SET(6, RM(EA) ); WM( EA,Z80.A);					}}; /* SET  6,A=(XY+o)  */
    opcode xycb_f8 = new opcode() { public void handler() { Z80.B = SET(7, RM(EA) ); WM( EA,Z80.B);					}}; /* SET  7,B=(XY+o)  */
    opcode xycb_f9 = new opcode() { public void handler() { Z80.C = SET(7, RM(EA) ); WM( EA,Z80.C);					}}; /* SET  7,C=(XY+o)  */
    opcode xycb_fa = new opcode() { public void handler() { Z80.D = SET(7, RM(EA) ); WM( EA,Z80.D);					}}; /* SET  7,D=(XY+o)  */
    opcode xycb_fb = new opcode() { public void handler() { Z80.E = SET(7, RM(EA) ); WM( EA,Z80.E);					}}; /* SET  7,E=(XY+o)  */
    opcode xycb_fc = new opcode() { public void handler() { Z80.H = SET(7, RM(EA) ); WM( EA,Z80.H);					}}; /* SET  7,H=(XY+o)  */
    opcode xycb_fd = new opcode() { public void handler() { Z80.L = SET(7, RM(EA) ); WM( EA,Z80.L);					}}; /* SET  7,L=(XY+o)  */
    opcode xycb_fe = new opcode() { public void handler() { WM( EA, SET(7,RM(EA)) );								}}; /* SET  7,(XY+o)	  */
    opcode xycb_ff = new opcode() { public void handler() { Z80.A = SET(7, RM(EA) ); WM( EA,Z80.A);					}}; /* SET  7,A=(XY+o)  */
    /**********************************************************
    * MOV opcodes
    **********************************************************/
    opcode dd_44 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.B = ((Z80.IX>>8)&0xFF); 										}}; /* LD   B,HX		  */
    opcode dd_45 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.B = Z80.IX & 0xFF; 										}}; /* LD   B,LX		  */
    opcode dd_4c = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.C = ((Z80.IX>>8)&0xFF); 										}}; /* LD   C,HX		  */
    opcode dd_4d = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.C = Z80.IX & 0xFF; 										}}; /* LD   C,LX		  */
    opcode dd_54 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.D = ((Z80.IX>>8)&0xFF); 										}}; /* LD   D,HX		  */
    opcode dd_55 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.D = Z80.IX & 0xFF; 										}}; /* LD   D,LX		  */
    opcode dd_5c = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.E = ((Z80.IX>>8)&0xFF); 										}}; /* LD   E,HX		  */
    opcode dd_5d = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.E = Z80.IX & 0xFF; 										}}; /* LD   E,LX		  */
    opcode dd_7c = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.A = ((Z80.IX>>8)&0xFF); 										}}; /* LD   A,HX		  */
    opcode dd_7d = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.A = Z80.IX & 0xFF; 										}}; /* LD   A,LX		  */
    opcode fd_44 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.B = ((Z80.IY>>8)&0xFF); 										}}; /* LD   B,HY		  */
    opcode fd_45 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.B = Z80.IY & 0xFF; 										}}; /* LD   B,LY		  */
    opcode fd_4c = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.C = ((Z80.IY>>8)&0xFF); 										}}; /* LD   C,HY		  */
    opcode fd_4d = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.C = Z80.IY & 0xFF; 										}}; /* LD   C,LY		  */
    opcode fd_54 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.D = ((Z80.IY>>8)&0xFF); 										}}; /* LD   D,HY		  */
    opcode fd_55 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.D = Z80.IY & 0xFF; 										}}; /* LD   D,LY		  */
    opcode fd_5c = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.E = ((Z80.IY>>8)&0xFF); 										}}; /* LD   E,HY		  */
    opcode fd_5d = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.E = Z80.IY & 0xFF; 										}}; /* LD   E,LY		  */
    opcode fd_7c = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.A = ((Z80.IY>>8)&0xFF); 										}}; /* LD   A,HY		  */
    opcode fd_7d = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.A = Z80.IY & 0xFF; 										}}; /* LD   A,LY		  */
    opcode dd_60 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=((Z80.IX & 0x00ff) | Z80.B << 8);										}}; /* LD   HX,B		  */
    opcode dd_61 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=((Z80.IX & 0x00ff) | Z80.C << 8);								}}; /* LD   HX,C		  */
    opcode dd_62 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=((Z80.IX & 0x00ff) | Z80.D << 8); 										}}; /* LD   HX,D		  */
    opcode dd_63 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=((Z80.IX & 0x00ff) | Z80.E << 8); 										}}; /* LD   HX,E		  */
    opcode dd_65 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=((Z80.IX & 0x00ff) | (Z80.IX & 0xFF) << 8);										}}; /* LD   HX,LX 	  */
    opcode dd_67 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=((Z80.IX & 0x00ff) | Z80.A << 8); 										}}; /* LD   HX,A		  */
    opcode dd_68 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=(Z80.IX & 0xff00) | Z80.B; 										}}; /* LD   LX,B		  */
    opcode dd_69 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=(Z80.IX & 0xff00) | Z80.C; 										}}; /* LD   LX,C		  */
    opcode dd_6a = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=(Z80.IX & 0xff00) | Z80.D; 										}}; /* LD   LX,D		  */
    opcode dd_6b = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=(Z80.IX & 0xff00) | Z80.E; 										}}; /* LD   LX,E		  */
    opcode dd_6c = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=(Z80.IX & 0xff00) | ((Z80.IX>>8)&0xFF);										}}; /* LD   LX,HX 	  */
    opcode dd_6f = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=(Z80.IX & 0xff00) | Z80.A; 										}}; /* LD   LX,A		  */
    opcode fd_60 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=((Z80.IY & 0x00ff) | Z80.B<< 8); 										}}; /* LD   HY,B		  */
    opcode fd_61 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=((Z80.IY & 0x00ff) | Z80.C<< 8); 										}}; /* LD   HY,C		  */
    opcode fd_62 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=((Z80.IY & 0x00ff) | Z80.D<< 8); 										}}; /* LD   HY,D		  */
    opcode fd_63 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=((Z80.IY & 0x00ff) | Z80.E<< 8); 										}}; /* LD   HY,E		  */
    opcode fd_65 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=((Z80.IY & 0x00ff) | (Z80.IY & 0xFF)<< 8);										}}; /* LD   HY,LY 	  */
    opcode fd_67 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=((Z80.IY & 0x00ff) | Z80.A<< 8); 										}}; /* LD   HY,A		  */
    opcode fd_68 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=(Z80.IY & 0xff00) | Z80.B; 										}}; /* LD   LY,B		  */
    opcode fd_69 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=(Z80.IY & 0xff00) | Z80.C; 										}}; /* LD   LY,C		  */
    opcode fd_6a = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=(Z80.IY & 0xff00) | Z80.D; 										}}; /* LD   LY,D		  */
    opcode fd_6b = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=(Z80.IY & 0xff00) | Z80.E; 										}}; /* LD   LY,E		  */
    opcode fd_6c = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=(Z80.IY & 0xff00) | ((Z80.IY>>8)&0xFF);										}}; /* LD   LY,HY 	  */
    opcode fd_6f = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=(Z80.IY & 0xff00) | Z80.A; 										}}; /* LD   LY,A		  */    
    opcode op_41 = new opcode() { public void handler() { Z80.B = Z80.C;												}}; /* LD   B,C		  */
    opcode op_42 = new opcode() { public void handler() { Z80.B = Z80.D;												}}; /* LD   B,D		  */
    opcode op_43 = new opcode() { public void handler() { Z80.B = Z80.E;												}}; /* LD   B,E		  */
    opcode op_44 = new opcode() { public void handler() { Z80.B = Z80.H;												}}; /* LD   B,H		  */
    opcode op_45 = new opcode() { public void handler() { Z80.B = Z80.L;												}}; /* LD   B,L		  */
    opcode op_46 = new opcode() { public void handler() { Z80.B = RM(HL());											}}; /* LD   B,(HL)	  */
    opcode op_47 = new opcode() { public void handler() { Z80.B = Z80.A;												}}; /* LD   B,A		  */
    opcode op_48 = new opcode() { public void handler() { Z80.C = Z80.B;												}}; /* LD   C,B		  */
    opcode op_4a = new opcode() { public void handler() { Z80.C = Z80.D;												}}; /* LD   C,D		  */
    opcode op_4b = new opcode() { public void handler() { Z80.C = Z80.E;												}}; /* LD   C,E		  */
    opcode op_4c = new opcode() { public void handler() { Z80.C = Z80.H;												}}; /* LD   C,H		  */
    opcode op_4d = new opcode() { public void handler() { Z80.C = Z80.L;												}}; /* LD   C,L		  */
    opcode op_4e = new opcode() { public void handler() { Z80.C = RM(HL());											}}; /* LD   C,(HL)	  */
    opcode op_4f = new opcode() { public void handler() { Z80.C = Z80.A;												}}; /* LD   C,A		  */
    opcode op_50 = new opcode() { public void handler() { Z80.D = Z80.B;												}}; /* LD   D,B		  */
    opcode op_51 = new opcode() { public void handler() { Z80.D = Z80.C;												}}; /* LD   D,C		  */
    opcode op_53 = new opcode() { public void handler() { Z80.D = Z80.E;												}}; /* LD   D,E		  */
    opcode op_54 = new opcode() { public void handler() { Z80.D = Z80.H;												}}; /* LD   D,H		  */
    opcode op_55 = new opcode() { public void handler() { Z80.D = Z80.L;												}}; /* LD   D,L		  */
    opcode op_56 = new opcode() { public void handler() { Z80.D = RM(HL());											}}; /* LD   D,(HL)	  */
    opcode op_57 = new opcode() { public void handler() { Z80.D = Z80.A;												}}; /* LD   D,A		  */
    opcode op_58 = new opcode() { public void handler() { Z80.E = Z80.B;												}}; /* LD   E,B		  */
    opcode op_59 = new opcode() { public void handler() { Z80.E = Z80.C;												}}; /* LD   E,C		  */
    opcode op_5a = new opcode() { public void handler() { Z80.E = Z80.D;												}}; /* LD   E,D		  */
    opcode op_5c = new opcode() { public void handler() { Z80.E = Z80.H;												}}; /* LD   E,H		  */
    opcode op_5d = new opcode() { public void handler() { Z80.E = Z80.L;												}}; /* LD   E,L		  */
    opcode op_5e = new opcode() { public void handler() { Z80.E = RM(HL());											}}; /* LD   E,(HL)	  */
    opcode op_5f = new opcode() { public void handler() { Z80.E = Z80.A;												}}; /* LD   E,A		  */
    opcode op_60 = new opcode() { public void handler() { Z80.H = Z80.B;												}}; /* LD   H,B		  */
    opcode op_61 = new opcode() { public void handler() { Z80.H = Z80.C;												}}; /* LD   H,C		  */
    opcode op_62 = new opcode() { public void handler() { Z80.H = Z80.D;												}}; /* LD   H,D		  */
    opcode op_63 = new opcode() { public void handler() { Z80.H = Z80.E;												}}; /* LD   H,E		  */
    opcode op_65 = new opcode() { public void handler() { Z80.H = Z80.L;												}}; /* LD   H,L		  */
    opcode op_66 = new opcode() { public void handler() { Z80.H = RM(HL());											}}; /* LD   H,(HL)	  */
    opcode op_67 = new opcode() { public void handler() { Z80.H = Z80.A;												}}; /* LD   H,A		  */
    opcode op_68 = new opcode() { public void handler() { Z80.L = Z80.B;												}}; /* LD   L,B		  */
    opcode op_69 = new opcode() { public void handler() { Z80.L = Z80.C;												}}; /* LD   L,C		  */
    opcode op_6a = new opcode() { public void handler() { Z80.L = Z80.D;												}}; /* LD   L,D		  */
    opcode op_6b = new opcode() { public void handler() { Z80.L = Z80.E;												}}; /* LD   L,E		  */
    opcode op_6c = new opcode() { public void handler() { Z80.L = Z80.H;												}}; /* LD   L,H		  */
    opcode op_6e = new opcode() { public void handler() { Z80.L = RM(HL());											}}; /* LD   L,(HL)	  */
    opcode op_6f = new opcode() { public void handler() { Z80.L = Z80.A;												}}; /* LD   L,A		  */
    opcode op_78 = new opcode() { public void handler() { Z80.A = Z80.B;												}}; /* LD   A,B		  */
    opcode op_79 = new opcode() { public void handler() { Z80.A = Z80.C;												}}; /* LD   A,C		  */
    opcode op_7a = new opcode() { public void handler() { Z80.A = Z80.D;												}}; /* LD   A,D		  */
    opcode op_7b = new opcode() { public void handler() { Z80.A = Z80.E;												}}; /* LD   A,E		  */
    opcode op_7c = new opcode() { public void handler() { Z80.A = Z80.H;												}}; /* LD   A,H		  */
    opcode op_7d = new opcode() { public void handler() { Z80.A = Z80.L;												}}; /* LD   A,L		  */
    opcode op_7e = new opcode() { public void handler() { Z80.A = RM(HL());											}}; /* LD   A,(HL)	  */
    opcode op_e9 = new opcode() { public void handler() { Z80.PC = HL(); change_pc16(Z80.PC);							}}; /* JP   (HL)		  */
    opcode op_f9 = new opcode() { public void handler() { Z80.SP = HL();												}}; /* LD   SP,HL 	  */
    /**********************************************************
    * ADD opcodes
    **********************************************************/
    opcode op_80 = new opcode() { public void handler() { ADD(Z80.B);												}}; /* ADD  A,B		  */
    opcode op_81 = new opcode() { public void handler() { ADD(Z80.C);												}}; /* ADD  A,C		  */
    opcode op_82 = new opcode() { public void handler() { ADD(Z80.D);												}}; /* ADD  A,D		  */
    opcode op_83 = new opcode() { public void handler() { ADD(Z80.E);												}}; /* ADD  A,E		  */
    opcode op_84 = new opcode() { public void handler() { ADD(Z80.H);												}}; /* ADD  A,H		  */
    opcode op_85 = new opcode() { public void handler() { ADD(Z80.L);												}}; /* ADD  A,L		  */
    opcode op_86 = new opcode() { public void handler() { ADD(RM(HL()));											}}; /* ADD  A,(HL)	  */
    opcode op_87 = new opcode() { public void handler() { ADD(Z80.A);												}}; /* ADD  A,A		  */
    opcode op_c6 = new opcode() { public void handler() { ADD(ARG()); 											}}; /* ADD  A,n		  */
    opcode dd_84 = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; ADD((Z80.IX>>8)&0xFF); 										}}; /* ADD  A,HX		  */
    opcode dd_85 = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; ADD(Z80.IX&0xFF); 										}}; /* ADD  A,LX		  */
    opcode fd_84 = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; ADD((Z80.IY>>8)&0xFF); 										}}; /* ADD  A,HY		  */
    opcode fd_85 = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; ADD(Z80.IY&0xFF); 										}}; /* ADD  A,LY		  */
    opcode dd_86 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); ADD(RM(EA)); 								}}; /* ADD  A,(IX+o)	  */
    opcode fd_86 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); ADD(RM(EA)); 								}}; /* ADD  A,(IY+o)	  */
    /**********************************************************
    * ADC opcodes
    **********************************************************/
    opcode op_88 = new opcode() { public void handler() { ADC(Z80.B);												}}; /* ADC  A,B		  */
    opcode op_89 = new opcode() { public void handler() { ADC(Z80.C);												}}; /* ADC  A,C		  */
    opcode op_8a = new opcode() { public void handler() { ADC(Z80.D);												}}; /* ADC  A,D		  */
    opcode op_8b = new opcode() { public void handler() { ADC(Z80.E);												}}; /* ADC  A,E		  */
    opcode op_8c = new opcode() { public void handler() { ADC(Z80.H);												}}; /* ADC  A,H		  */
    opcode op_8d = new opcode() { public void handler() { ADC(Z80.L);												}}; /* ADC  A,L		  */
    opcode op_8e = new opcode() { public void handler() { ADC(RM(HL()));											}}; /* ADC  A,(HL)	  */
    opcode op_8f = new opcode() { public void handler() { ADC(Z80.A);												}}; /* ADC  A,A		  */
    opcode op_ce = new opcode() { public void handler() { ADC(ARG()); 											}}; /* ADC  A,n		  */
    opcode dd_8c = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; ADC((Z80.IX>>8)&0xFF); 										}}; /* ADC  A,HX		  */
    opcode dd_8d = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; ADC(Z80.IX&0xFF); 										}}; /* ADC  A,LX		  */
    opcode fd_8c = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; ADC((Z80.IY>>8)&0xFF); 										}}; /* ADC  A,HY		  */
    opcode fd_8d = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; ADC(Z80.IY&0xFF); 										}}; /* ADC  A,LY		  */
    opcode dd_8e = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); ADC(RM(EA)); 								}}; /* ADC  A,(IX+o)	  */
    opcode fd_8e = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); ADC(RM(EA)); 								}}; /* ADC  A,(IY+o)	  */
    /**********************************************************
    * CP opcodes
    **********************************************************/
    opcode dd_bc = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; CP((Z80.IX>>8)&0xFF);											}}; /* CP   HX		  */
    opcode dd_bd = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; CP(Z80.IX&0xFF);											}}; /* CP   LX		  */
    opcode fd_bc = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; CP((Z80.IY>>8)&0xFF);											}}; /* CP   HY		  */
    opcode fd_bd = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; CP(Z80.IY&0xFF);											}}; /* CP   LY		  */
    opcode dd_be = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); CP(RM(EA));									}}; /* CP   (IX+o)	  */
    opcode fd_be = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); CP(RM(EA));									}}; /* CP   (IY+o)	  */
    opcode op_b8 = new opcode() { public void handler() { CP(Z80.B); 												}}; /* CP   B 		  */
    opcode op_b9 = new opcode() { public void handler() { CP(Z80.C); 												}}; /* CP   C 		  */
    opcode op_ba = new opcode() { public void handler() { CP(Z80.D); 												}}; /* CP   D 		  */
    opcode op_bb = new opcode() { public void handler() { CP(Z80.E); 												}}; /* CP   E 		  */
    opcode op_bc = new opcode() { public void handler() { CP(Z80.H); 												}}; /* CP   H 		  */
    opcode op_bd = new opcode() { public void handler() { CP(Z80.L); 												}}; /* CP   L 		  */
    opcode op_be = new opcode() { public void handler() { CP(RM(HL()));											}}; /* CP   (HL)		  */
    opcode op_bf = new opcode() { public void handler() { CP(Z80.A); 												}}; /* CP   A 		  */
    opcode op_fe = new opcode() { public void handler() { CP(ARG());												}}; /* CP   n 		  */
    /**********************************************************
    * SBC opcodes
    **********************************************************/
    opcode op_98 = new opcode() { public void handler() { SBC(Z80.B);												}}; /* SBC  A,B		  */
    opcode op_99 = new opcode() { public void handler() { SBC(Z80.C);												}}; /* SBC  A,C		  */
    opcode op_9a = new opcode() { public void handler() { SBC(Z80.D);												}}; /* SBC  A,D		  */
    opcode op_9b = new opcode() { public void handler() { SBC(Z80.E);												}}; /* SBC  A,E		  */
    opcode op_9c = new opcode() { public void handler() { SBC(Z80.H);												}}; /* SBC  A,H		  */
    opcode op_9d = new opcode() { public void handler() { SBC(Z80.L);												}}; /* SBC  A,L		  */
    opcode op_9e = new opcode() { public void handler() { SBC(RM(HL()));											}}; /* SBC  A,(HL)	  */
    opcode op_9f = new opcode() { public void handler() { SBC(Z80.A);												}}; /* SBC  A,A		  */
    opcode op_de = new opcode() { public void handler() { SBC(ARG()); 											}}; /* SBC  A,n		  */
    opcode dd_9c = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; SBC((Z80.IX>>8)&0xFF); 										}}; /* SBC  A,HX		  */
    opcode dd_9d = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; SBC(Z80.IX&0xFF); 										}}; /* SBC  A,LX		  */
    opcode fd_9c = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; SBC((Z80.IY>>8)&0xFF); 										}}; /* SBC  A,HY		  */
    opcode fd_9d = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; SBC(Z80.IY&0xFF); 										}}; /* SBC  A,LY		  */
    opcode dd_9e = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); SBC(RM(EA)); 								}}; /* SBC  A,(IX+o)	  */
    opcode fd_9e = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); SBC(RM(EA)); 								}}; /* SBC  A,(IY+o)	  */
    /**********************************************************
    * SUB opcodes
    **********************************************************/
    opcode dd_94 = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; SUB((Z80.IX>>8)&0xFF); 										}}; /* SUB  HX		  */
    opcode dd_95 = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; SUB(Z80.IX&0xFF); 										}}; /* SUB  LX		  */
    opcode fd_94 = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; SUB((Z80.IY>>8)&0xFF); 										}}; /* SUB  HY		  */
    opcode fd_95 = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; SUB(Z80.IY&0xFF); 										}}; /* SUB  LY		  */
    opcode dd_96 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); SUB(RM(EA)); 								}}; /* SUB  (IX+o)	  */
    opcode fd_96 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); SUB(RM(EA)); 								}}; /* SUB  (IY+o)	  */
    opcode op_90 = new opcode() { public void handler() { SUB(Z80.B);												}}; /* SUB  B 		  */
    opcode op_91 = new opcode() { public void handler() { SUB(Z80.C);												}}; /* SUB  C 		  */
    opcode op_92 = new opcode() { public void handler() { SUB(Z80.D);												}}; /* SUB  D 		  */
    opcode op_93 = new opcode() { public void handler() { SUB(Z80.E);												}}; /* SUB  E 		  */
    opcode op_94 = new opcode() { public void handler() { SUB(Z80.H);												}}; /* SUB  H 		  */
    opcode op_95 = new opcode() { public void handler() { SUB(Z80.L);												}}; /* SUB  L 		  */
    opcode op_96 = new opcode() { public void handler() { SUB(RM(HL()));											}}; /* SUB  (HL)		  */
    opcode op_97 = new opcode() { public void handler() { SUB(Z80.A);												}}; /* SUB  A 		  */
    opcode op_d6 = new opcode() { public void handler() { SUB(ARG()); 											}}; /* SUB  n 		  */
    /**********************************************************
    * AND opcodes
    **********************************************************/
    opcode dd_a4 = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; AND((Z80.IX>>8)&0xFF); 										}}; /* AND  HX		  */
    opcode dd_a5 = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; AND(Z80.IX&0xFF); 										}}; /* AND  LX		  */
    opcode fd_a4 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; AND((Z80.IY>>8)&0xFF); 										}}; /* AND  HY		  */
    opcode fd_a5 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; AND(Z80.IY&0xFF); 										}}; /* AND  LY		  */
    opcode dd_a6 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); AND(RM(EA)); 								}}; /* AND  (IX+o)	  */
    opcode fd_a6 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); AND(RM(EA)); 								}}; /* AND  (IY+o)	  */
    opcode op_a0 = new opcode() { public void handler() { AND(Z80.B);												}}; /* AND  B 		  */
    opcode op_a1 = new opcode() { public void handler() { AND(Z80.C);												}}; /* AND  C 		  */
    opcode op_a2 = new opcode() { public void handler() { AND(Z80.D);												}}; /* AND  D 		  */
    opcode op_a3 = new opcode() { public void handler() { AND(Z80.E);												}}; /* AND  E 		  */
    opcode op_a4 = new opcode() { public void handler() { AND(Z80.H);												}}; /* AND  H 		  */
    opcode op_a5 = new opcode() { public void handler() { AND(Z80.L);												}}; /* AND  L 		  */
    opcode op_a6 = new opcode() { public void handler() { AND(RM(HL()));											}}; /* AND  (HL)		  */
    opcode op_a7 = new opcode() { public void handler() { AND(Z80.A);												}}; /* AND  A 		  */
    opcode op_e6 = new opcode() { public void handler() { AND(ARG()); 											}}; /* AND  n 		  */
    /**********************************************************
    * OR opcodes
    **********************************************************/
    opcode op_b0 = new opcode() { public void handler() { OR(Z80.B); 												}}; /* OR   B 		  */
    opcode op_b1 = new opcode() { public void handler() { OR(Z80.C); 												}}; /* OR   C 		  */
    opcode op_b2 = new opcode() { public void handler() { OR(Z80.D); 												}}; /* OR   D 		  */
    opcode op_b3 = new opcode() { public void handler() { OR(Z80.E); 												}}; /* OR   E 		  */
    opcode op_b4 = new opcode() { public void handler() { OR(Z80.H); 												}}; /* OR   H 		  */
    opcode op_b5 = new opcode() { public void handler() { OR(Z80.L); 												}}; /* OR   L 		  */
    opcode op_b6 = new opcode() { public void handler() { OR(RM(HL()));											}}; /* OR   (HL)		  */
    opcode op_b7 = new opcode() { public void handler() { OR(Z80.A); 												}}; /* OR   A 		  */
    opcode op_f6 = new opcode() { public void handler() { OR(ARG());												}}; /* OR   n 		  */
    opcode dd_b4 = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; OR((Z80.IX>>8)&0xFF);											}}; /* OR   HX		  */
    opcode dd_b5 = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; OR(Z80.IX&0xFF);											}}; /* OR   LX		  */
    opcode fd_b4 = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; OR((Z80.IY>>8)&0xFF);											}}; /* OR   HY		  */
    opcode fd_b5 = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; OR(Z80.IY&0xFF);											}}; /* OR   LY		  */
    opcode dd_b6 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); OR(RM(EA));									}}; /* OR   (IX+o)	  */
    opcode fd_b6 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); OR(RM(EA));									}}; /* OR   (IY+o)	  */
    /**********************************************************
    * XOR opcodes
    **********************************************************/
    opcode op_a8 = new opcode() { public void handler() { XOR(Z80.B);												}}; /* XOR  B 		  */
    opcode op_a9 = new opcode() { public void handler() { XOR(Z80.C);												}}; /* XOR  C 		  */
    opcode op_aa = new opcode() { public void handler() { XOR(Z80.D);												}}; /* XOR  D 		  */
    opcode op_ab = new opcode() { public void handler() { XOR(Z80.E);												}}; /* XOR  E 		  */
    opcode op_ac = new opcode() { public void handler() { XOR(Z80.H);												}}; /* XOR  H 		  */
    opcode op_ad = new opcode() { public void handler() { XOR(Z80.L);												}}; /* XOR  L 		  */
    opcode op_ae = new opcode() { public void handler() { XOR(RM(HL()));											}}; /* XOR  (HL)		  */
    opcode op_af = new opcode() { public void handler() { XOR(Z80.A);												}}; /* XOR  A 		  */
    opcode op_ee = new opcode() { public void handler() { XOR(ARG()); 											}}; /* XOR  n 		  */
    opcode dd_ac = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; XOR((Z80.IX>>8)&0xFF); 										}}; /* XOR  HX		  */
    opcode dd_ad = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; XOR(Z80.IX&0xFF); 										}}; /* XOR  LX		  */
    opcode fd_ac = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; XOR((Z80.IY>>8)&0xFF); 										}}; /* XOR  HY		  */
    opcode fd_ad = new opcode() { public void handler() { Z80.R = (Z80.R+1)&0xFF; XOR(Z80.IY&0xFF); 										}}; /* XOR  LY		  */
    opcode dd_ae = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAX(); XOR(RM(EA)); 								}}; /* XOR  (IX+o)	  */
    opcode fd_ae = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; EAY(); XOR(RM(EA)); 								}}; /* XOR  (IY+o)	  */
    /**********************************************************
    * ADD16 opcodes
    **********************************************************/
    opcode op_09 = new opcode() { public void handler() { HL(ADD16(HL(), BC()));											}}; /* ADD  HL,BC 	  */
    opcode op_19 = new opcode() { public void handler() { HL(ADD16(HL(), DE()));											}}; /* ADD  HL,DE 	  */
    opcode op_29 = new opcode() { public void handler() { HL(ADD16(HL(), HL()));											}}; /* ADD  HL,HL 	  */
    opcode op_39 = new opcode() { public void handler() { HL(ADD16(HL(), Z80.SP));											}}; /* ADD  HL,SP 	  */
    opcode dd_09 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=ADD16(Z80.IX,BC()); 									}}; /* ADD  IX,BC 	  */
    opcode dd_19 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=ADD16(Z80.IX,DE()); 									}}; /* ADD  IX,DE 	  */
    opcode dd_29 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=ADD16(Z80.IX,Z80.IX); 									}}; /* ADD  IX,IX 	  */
    opcode dd_39 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IX=ADD16(Z80.IX,Z80.SP); 									}}; /* ADD  IX,SP 	  */
    opcode fd_09 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=ADD16(Z80.IY,BC()); 									}}; /* ADD  IY,BC 	  */
    opcode fd_19 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=ADD16(Z80.IY,DE()); 									}}; /* ADD  IY,DE 	  */
    opcode fd_29 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=ADD16(Z80.IY,Z80.IY); 									}}; /* ADD  IY,IY 	  */
    opcode fd_39 = new opcode() { public void handler() { Z80.R = (Z80.R + 1) & 0xFF; Z80.IY=ADD16(Z80.IY,Z80.SP); 									}}; /* ADD  IY,SP 	  */
    /**********************************************************
    * ADC16 opcodes
    **********************************************************/
    opcode ed_4a = new opcode() { public void handler() { ADC16( BC() );											}}; /* ADC  HL,BC 	  */
    opcode ed_5a = new opcode() { public void handler() { ADC16( DE() );											}}; /* ADC  HL,DE 	  */
    opcode ed_6a = new opcode() { public void handler() { ADC16( HL() );											}}; /* ADC  HL,HL 	  */
    opcode ed_7a = new opcode() { public void handler() { ADC16( Z80.SP );											}}; /* ADC  HL,SP 	  */	
    /**********************************************************
    * SBC16 opcodes
    **********************************************************/
    opcode ed_42 = new opcode() { public void handler() { SBC16( BC() );											}}; /* SBC  HL,BC 	  */
    opcode ed_52 = new opcode() { public void handler() { SBC16( DE() );											}}; /* SBC  HL,DE 	  */
    opcode ed_62 = new opcode() { public void handler() { SBC16( HL() );											}}; /* SBC  HL,HL 	  */
    opcode ed_72 = new opcode() { public void handler() { SBC16( Z80.SP );											}}; /* SBC  HL,SP 	  */    
    /**********************************************************
    * NEG opcodes
    **********************************************************/
    opcode ed_44 = new opcode() { public void handler() { NEG();													}}; /* NEG			  */
    opcode ed_4c = new opcode() { public void handler() { NEG();													}}; /* NEG			  */
    opcode ed_54 = new opcode() { public void handler() { NEG();													}}; /* NEG			  */
    opcode ed_5c = new opcode() { public void handler() { NEG();													}}; /* NEG			  */
    opcode ed_64 = new opcode() { public void handler() { NEG();													}}; /* NEG			  */
    opcode ed_6c = new opcode() { public void handler() { NEG();													}}; /* NEG			  */
    opcode ed_74 = new opcode() { public void handler() { NEG();													}}; /* NEG			  */
    opcode ed_7c = new opcode() { public void handler() { NEG();													}}; /* NEG			  */
    /**********************************************************
    * No-op opcodes
    **********************************************************/
    opcode fd_64= new opcode() { public void handler(){ Z80.R = (Z80.R+1)&0xFF;													}}; /* LD   HY,HY 	  */
    opcode fd_6d= new opcode() { public void handler(){ Z80.R = (Z80.R+1)&0xFF;													}}; /* LD   LY,LY 	  */
    opcode dd_64= new opcode() { public void handler(){ 														}}; /* LD   HX,HX 	  */
    opcode dd_6d= new opcode() { public void handler(){ 														}}; /* LD   LX,LX 	  */
    opcode op_00= new opcode() { public void handler(){ 														}}; /* NOP			  */
    opcode op_40= new opcode() { public void handler(){ 														}}; /* LD   B,B		  */
    opcode op_49= new opcode() { public void handler(){ 														}}; /* LD   C,C		  */
    opcode op_52= new opcode() { public void handler(){ 														}}; /* LD   D,D		  */
    opcode op_5b= new opcode() { public void handler(){ 														}}; /* LD   E,E		  */
    opcode op_64= new opcode() { public void handler(){ 														}}; /* LD   H,H		  */
    opcode op_6d= new opcode() { public void handler(){ 														}}; /* LD   L,L		  */
    opcode op_7f= new opcode() { public void handler(){ 														}}; /* LD   A,A		  */
    /**********************************************************
    * Redirect opcodes
    **********************************************************/
    opcode xycb_40= new opcode() { public void handler(){ xycb_46.handler();											}}; /* BIT  0,B=(XY+o)  */
    opcode xycb_41= new opcode() { public void handler(){ xycb_46.handler();													  }}; /* BIT	0,C=(XY+o)	*/
    opcode xycb_42= new opcode() { public void handler(){ xycb_46.handler();											}}; /* BIT  0,D=(XY+o)  */
    opcode xycb_43= new opcode() { public void handler(){ xycb_46.handler();											}}; /* BIT  0,E=(XY+o)  */
    opcode xycb_44= new opcode() { public void handler(){ xycb_46.handler();											}}; /* BIT  0,H=(XY+o)  */
    opcode xycb_45= new opcode() { public void handler(){ xycb_46.handler();											}}; /* BIT  0,L=(XY+o)  */
    opcode xycb_47= new opcode() { public void handler(){ xycb_46.handler();											}}; /* BIT  0,A=(XY+o)  */
    opcode xycb_48= new opcode() { public void handler(){ xycb_4e.handler();											}}; /* BIT  1,B=(XY+o)  */
    opcode xycb_49= new opcode() { public void handler(){ xycb_4e.handler();													  }}; /* BIT	1,C=(XY+o)	*/
    opcode xycb_4a= new opcode() { public void handler(){ xycb_4e.handler();											}}; /* BIT  1,D=(XY+o)  */
    opcode xycb_4b= new opcode() { public void handler(){ xycb_4e.handler();											}}; /* BIT  1,E=(XY+o)  */
    opcode xycb_4c= new opcode() { public void handler(){ xycb_4e.handler();											}}; /* BIT  1,H=(XY+o)  */
    opcode xycb_4d= new opcode() { public void handler(){ xycb_4e.handler();											}}; /* BIT  1,L=(XY+o)  */
    opcode xycb_4f= new opcode() { public void handler(){ xycb_4e.handler();											}}; /* BIT  1,A=(XY+o)  */
    opcode xycb_50= new opcode() { public void handler(){ xycb_56.handler();											}}; /* BIT  2,B=(XY+o)  */
    opcode xycb_51= new opcode() { public void handler(){ xycb_56.handler();													  }}; /* BIT	2,C=(XY+o)	*/
    opcode xycb_52= new opcode() { public void handler(){ xycb_56.handler();											}}; /* BIT  2,D=(XY+o)  */
    opcode xycb_53= new opcode() { public void handler(){ xycb_56.handler();											}}; /* BIT  2,E=(XY+o)  */
    opcode xycb_54= new opcode() { public void handler(){ xycb_56.handler();											}}; /* BIT  2,H=(XY+o)  */
    opcode xycb_55= new opcode() { public void handler(){ xycb_56.handler();											}}; /* BIT  2,L=(XY+o)  */
    opcode xycb_57= new opcode() { public void handler(){ xycb_56.handler();											}}; /* BIT  2,A=(XY+o)  */
    opcode xycb_58= new opcode() { public void handler(){ xycb_5e.handler();											}}; /* BIT  3,B=(XY+o)  */
    opcode xycb_59= new opcode() { public void handler(){ xycb_5e.handler();													  }}; /* BIT	3,C=(XY+o)	*/
    opcode xycb_5a= new opcode() { public void handler(){ xycb_5e.handler();											}}; /* BIT  3,D=(XY+o)  */
    opcode xycb_5b= new opcode() { public void handler(){ xycb_5e.handler();											}}; /* BIT  3,E=(XY+o)  */
    opcode xycb_5c= new opcode() { public void handler(){ xycb_5e.handler();											}}; /* BIT  3,H=(XY+o)  */
    opcode xycb_5d= new opcode() { public void handler(){ xycb_5e.handler();											}}; /* BIT  3,L=(XY+o)  */
    opcode xycb_5f= new opcode() { public void handler(){ xycb_5e.handler();											}}; /* BIT  3,A=(XY+o)  */
    opcode xycb_60= new opcode() { public void handler(){ xycb_66.handler();											}}; /* BIT  4,B=(XY+o)  */
    opcode xycb_61= new opcode() { public void handler(){ xycb_66.handler();													  }}; /* BIT	4,C=(XY+o)	*/
    opcode xycb_62= new opcode() { public void handler(){ xycb_66.handler();											}}; /* BIT  4,D=(XY+o)  */
    opcode xycb_63= new opcode() { public void handler(){ xycb_66.handler();											}}; /* BIT  4,E=(XY+o)  */
    opcode xycb_64= new opcode() { public void handler(){ xycb_66.handler();											}}; /* BIT  4,H=(XY+o)  */
    opcode xycb_65= new opcode() { public void handler(){ xycb_66.handler();											}}; /* BIT  4,L=(XY+o)  */
    opcode xycb_67= new opcode() { public void handler(){ xycb_66.handler();											}}; /* BIT  4,A=(XY+o)  */
    opcode xycb_68= new opcode() { public void handler(){ xycb_6e.handler();											}}; /* BIT  5,B=(XY+o)  */
    opcode xycb_69= new opcode() { public void handler(){ xycb_6e.handler();													  }}; /* BIT	5,C=(XY+o)	*/
    opcode xycb_6a= new opcode() { public void handler(){ xycb_6e.handler();											}}; /* BIT  5,D=(XY+o)  */
    opcode xycb_6b= new opcode() { public void handler(){ xycb_6e.handler();											}}; /* BIT  5,E=(XY+o)  */
    opcode xycb_6c= new opcode() { public void handler(){ xycb_6e.handler();											}}; /* BIT  5,H=(XY+o)  */
    opcode xycb_6d= new opcode() { public void handler(){ xycb_6e.handler();											}}; /* BIT  5,L=(XY+o)  */
    opcode xycb_6f= new opcode() { public void handler(){ xycb_6e.handler();											}}; /* BIT  5,A=(XY+o)  */
    opcode xycb_70= new opcode() { public void handler(){ xycb_76.handler();											}}; /* BIT  6,B=(XY+o)  */
    opcode xycb_71= new opcode() { public void handler(){ xycb_76.handler();													  }}; /* BIT	6,C=(XY+o)	*/
    opcode xycb_72= new opcode() { public void handler(){ xycb_76.handler();											}}; /* BIT  6,D=(XY+o)  */
    opcode xycb_73= new opcode() { public void handler(){ xycb_76.handler();											}}; /* BIT  6,E=(XY+o)  */
    opcode xycb_74= new opcode() { public void handler(){ xycb_76.handler();											}}; /* BIT  6,H=(XY+o)  */
    opcode xycb_75= new opcode() { public void handler(){ xycb_76.handler();											}}; /* BIT  6,L=(XY+o)  */
    opcode xycb_77= new opcode() { public void handler(){ xycb_76.handler();											}}; /* BIT  6,A=(XY+o)  */
    opcode xycb_78= new opcode() { public void handler(){ xycb_7e.handler();											}}; /* BIT  7,B=(XY+o)  */
    opcode xycb_79= new opcode() { public void handler(){ xycb_7e.handler();													  }}; /* BIT	7,C=(XY+o)	*/
    opcode xycb_7a= new opcode() { public void handler(){ xycb_7e.handler();											}}; /* BIT  7,D=(XY+o)  */
    opcode xycb_7b= new opcode() { public void handler(){ xycb_7e.handler();											}}; /* BIT  7,E=(XY+o)  */
    opcode xycb_7c= new opcode() { public void handler(){ xycb_7e.handler();											}}; /* BIT  7,H=(XY+o)  */
    opcode xycb_7d= new opcode() { public void handler(){ xycb_7e.handler();											}}; /* BIT  7,L=(XY+o)  */
    opcode xycb_7f= new opcode() { public void handler(){ xycb_7e.handler();											}}; /* BIT  7,A=(XY+o)  */    
    /**********************************************************
    * illegal_1 opcodes
    **********************************************************/
    opcode dd_00= new opcode() { public void handler(){ illegal_1.handler(); op_00.handler();									}}; /* DB   DD		  */
    opcode dd_01= new opcode() { public void handler(){ illegal_1.handler(); op_01.handler();									}}; /* DB   DD		  */
    opcode dd_02= new opcode() { public void handler(){ illegal_1.handler(); op_02.handler();									}}; /* DB   DD		  */
    opcode dd_03= new opcode() { public void handler(){ illegal_1.handler(); op_03.handler();									}}; /* DB   DD		  */
    opcode dd_04= new opcode() { public void handler(){ illegal_1.handler(); op_04.handler();									}}; /* DB   DD		  */
    opcode dd_05= new opcode() { public void handler(){ illegal_1.handler(); op_05.handler();									}}; /* DB   DD		  */
    opcode dd_06= new opcode() { public void handler(){ illegal_1.handler(); op_06.handler();									}}; /* DB   DD		  */
    opcode dd_07= new opcode() { public void handler(){ illegal_1.handler(); op_07.handler();									}}; /* DB   DD		  */
    opcode dd_08= new opcode() { public void handler(){ illegal_1.handler(); op_08.handler();									}}; /* DB   DD		  */
    opcode dd_0a= new opcode() { public void handler(){ illegal_1.handler(); op_0a.handler();									}}; /* DB   DD		  */
    opcode dd_0b= new opcode() { public void handler(){ illegal_1.handler(); op_0b.handler();									}}; /* DB   DD		  */
    opcode dd_0c= new opcode() { public void handler(){ illegal_1.handler(); op_0c.handler();									}}; /* DB   DD		  */
    opcode dd_0d= new opcode() { public void handler(){ illegal_1.handler(); op_0d.handler();									}}; /* DB   DD		  */
    opcode dd_0e= new opcode() { public void handler(){ illegal_1.handler(); op_0e.handler();									}}; /* DB   DD		  */
    opcode dd_0f= new opcode() { public void handler(){ illegal_1.handler(); op_0f.handler();									}}; /* DB   DD		  */
    opcode dd_10= new opcode() { public void handler(){ illegal_1.handler(); op_10.handler();									}}; /* DB   DD		  */
    opcode dd_11= new opcode() { public void handler(){ illegal_1.handler(); op_11.handler();									}}; /* DB   DD		  */
    opcode dd_12= new opcode() { public void handler(){ illegal_1.handler(); op_12.handler();									}}; /* DB   DD		  */
    opcode dd_13= new opcode() { public void handler(){ illegal_1.handler(); op_13.handler();									}}; /* DB   DD		  */
    opcode dd_14= new opcode() { public void handler(){ illegal_1.handler(); op_14.handler();									}}; /* DB   DD		  */
    opcode dd_15= new opcode() { public void handler(){ illegal_1.handler(); op_15.handler();									}}; /* DB   DD		  */
    opcode dd_16= new opcode() { public void handler(){ illegal_1.handler(); op_16.handler();									}}; /* DB   DD		  */
    opcode dd_17= new opcode() { public void handler(){ illegal_1.handler(); op_17.handler();									}}; /* DB   DD		  */
    opcode dd_18= new opcode() { public void handler(){ illegal_1.handler(); op_18.handler();									}}; /* DB   DD		  */
    opcode dd_27= new opcode() { public void handler(){ illegal_1.handler(); op_27.handler();									}}; /* DB   DD		  */
    opcode dd_28= new opcode() { public void handler(){ illegal_1.handler(); op_28.handler();									}}; /* DB   DD		  */
    opcode dd_2f= new opcode() { public void handler(){ illegal_1.handler(); op_2f.handler();									}}; /* DB   DD		  */
    opcode dd_30= new opcode() { public void handler(){ illegal_1.handler(); op_30.handler();									}}; /* DB   DD		  */
    opcode dd_31= new opcode() { public void handler(){ illegal_1.handler(); op_31.handler();									}}; /* DB   DD		  */
    opcode dd_32= new opcode() { public void handler(){ illegal_1.handler(); op_32.handler();									}}; /* DB   DD		  */
    opcode dd_33= new opcode() { public void handler(){ illegal_1.handler(); op_33.handler();									}}; /* DB   DD		  */
    opcode dd_37= new opcode() { public void handler(){ illegal_1.handler(); op_37.handler();									}}; /* DB   DD		  */
    opcode dd_38= new opcode() { public void handler(){ illegal_1.handler(); op_38.handler();									}}; /* DB   DD		  */
    opcode dd_3a= new opcode() { public void handler(){ illegal_1.handler(); op_3a.handler();									}}; /* DB   DD		  */
    opcode dd_3b= new opcode() { public void handler(){ illegal_1.handler(); op_3b.handler();									}}; /* DB   DD		  */
    opcode dd_3c= new opcode() { public void handler(){ illegal_1.handler(); op_3c.handler();									}}; /* DB   DD		  */
    opcode dd_3d= new opcode() { public void handler(){ illegal_1.handler(); op_3d.handler();									}}; /* DB   DD		  */
    opcode dd_3e= new opcode() { public void handler(){ illegal_1.handler(); op_3e.handler();									}}; /* DB   DD		  */
    opcode dd_3f= new opcode() { public void handler(){ illegal_1.handler(); op_3f.handler();									}}; /* DB   DD		  */
    opcode dd_40= new opcode() { public void handler(){ illegal_1.handler(); op_40.handler();									}}; /* DB   DD		  */
    opcode dd_41= new opcode() { public void handler(){ illegal_1.handler(); op_41.handler();									}}; /* DB   DD		  */
    opcode dd_42= new opcode() { public void handler(){ illegal_1.handler(); op_42.handler();									}}; /* DB   DD		  */
    opcode dd_43= new opcode() { public void handler(){ illegal_1.handler(); op_43.handler();									}}; /* DB   DD		  */
    opcode dd_47= new opcode() { public void handler(){ illegal_1.handler(); op_47.handler();									}}; /* DB   DD		  */
    opcode dd_48= new opcode() { public void handler(){ illegal_1.handler(); op_48.handler();									}}; /* DB   DD		  */
    opcode dd_49= new opcode() { public void handler(){ illegal_1.handler(); op_49.handler();									}}; /* DB   DD		  */
    opcode dd_4a= new opcode() { public void handler(){ illegal_1.handler(); op_4a.handler();									}}; /* DB   DD		  */
    opcode dd_4b= new opcode() { public void handler(){ illegal_1.handler(); op_4b.handler();									}}; /* DB   DD		  */
    opcode dd_4f= new opcode() { public void handler(){ illegal_1.handler(); op_4f.handler();									}}; /* DB   DD		  */
    opcode dd_50= new opcode() { public void handler(){ illegal_1.handler(); op_50.handler();									}}; /* DB   DD		  */
    opcode dd_51= new opcode() { public void handler(){ illegal_1.handler(); op_51.handler();									}}; /* DB   DD		  */
    opcode dd_52= new opcode() { public void handler(){ illegal_1.handler(); op_52.handler();									}}; /* DB   DD		  */
    opcode dd_53= new opcode() { public void handler(){ illegal_1.handler(); op_53.handler();									}}; /* DB   DD		  */
    opcode dd_57= new opcode() { public void handler(){ illegal_1.handler(); op_57.handler();									}}; /* DB   DD		  */
    opcode dd_58= new opcode() { public void handler(){ illegal_1.handler(); op_58.handler();									}}; /* DB   DD		  */
    opcode dd_59= new opcode() { public void handler(){ illegal_1.handler(); op_59.handler();									}}; /* DB   DD		  */
    opcode dd_5a= new opcode() { public void handler(){ illegal_1.handler(); op_5a.handler();									}}; /* DB   DD		  */
    opcode dd_5b= new opcode() { public void handler(){ illegal_1.handler(); op_5b.handler();									}}; /* DB   DD		  */
    opcode dd_5f= new opcode() { public void handler(){ illegal_1.handler(); op_5f.handler();									}}; /* DB   DD		  */
    opcode dd_76= new opcode() { public void handler(){ illegal_1.handler(); op_76.handler();									}};		  /* DB   DD		  */
    opcode dd_78= new opcode() { public void handler(){ illegal_1.handler(); op_78.handler();									}}; /* DB   DD		  */
    opcode dd_79= new opcode() { public void handler(){ illegal_1.handler(); op_79.handler();									}}; /* DB   DD		  */
    opcode dd_7a= new opcode() { public void handler(){ illegal_1.handler(); op_7a.handler();									}}; /* DB   DD		  */
    opcode dd_7b= new opcode() { public void handler(){ illegal_1.handler(); op_7b.handler();									}}; /* DB   DD		  */
    opcode dd_7f= new opcode() { public void handler(){ illegal_1.handler(); op_7f.handler();									}}; /* DB   DD		  */
    opcode dd_80= new opcode() { public void handler(){ illegal_1.handler(); op_80.handler();									}}; /* DB   DD		  */
    opcode dd_81= new opcode() { public void handler(){ illegal_1.handler(); op_81.handler();									}}; /* DB   DD		  */
    opcode dd_82= new opcode() { public void handler(){ illegal_1.handler(); op_82.handler();									}}; /* DB   DD		  */
    opcode dd_83= new opcode() { public void handler(){ illegal_1.handler(); op_83.handler();									}}; /* DB   DD		  */
    opcode dd_87= new opcode() { public void handler(){ illegal_1.handler(); op_87.handler();									}}; /* DB   DD		  */
    opcode dd_88= new opcode() { public void handler(){ illegal_1.handler(); op_88.handler();									}}; /* DB   DD		  */
    opcode dd_89= new opcode() { public void handler(){ illegal_1.handler(); op_89.handler();									}}; /* DB   DD		  */
    opcode dd_8a= new opcode() { public void handler(){ illegal_1.handler(); op_8a.handler();									}}; /* DB   DD		  */
    opcode dd_8b= new opcode() { public void handler(){ illegal_1.handler(); op_8b.handler();									}}; /* DB   DD		  */
    opcode dd_8f= new opcode() { public void handler(){ illegal_1.handler(); op_8f.handler();									}}; /* DB   DD		  */
    opcode dd_90= new opcode() { public void handler(){ illegal_1.handler(); op_90.handler();									}}; /* DB   DD		  */
    opcode dd_91= new opcode() { public void handler(){ illegal_1.handler(); op_91.handler();									}}; /* DB   DD		  */
    opcode dd_92= new opcode() { public void handler(){ illegal_1.handler(); op_92.handler();									}}; /* DB   DD		  */
    opcode dd_93= new opcode() { public void handler(){ illegal_1.handler(); op_93.handler();									}}; /* DB   DD		  */
    opcode dd_97= new opcode() { public void handler(){ illegal_1.handler(); op_97.handler();									}}; /* DB   DD		  */
    opcode dd_98= new opcode() { public void handler(){ illegal_1.handler(); op_98.handler();									}}; /* DB   DD		  */
    opcode dd_99= new opcode() { public void handler(){ illegal_1.handler(); op_99.handler();									}}; /* DB   DD		  */
    opcode dd_9a= new opcode() { public void handler(){ illegal_1.handler(); op_9a.handler();									}}; /* DB   DD		  */
    opcode dd_9b= new opcode() { public void handler(){ illegal_1.handler(); op_9b.handler();									}}; /* DB   DD		  */
    opcode dd_9f= new opcode() { public void handler(){ illegal_1.handler(); op_9f.handler();									}}; /* DB   DD		  */
    opcode dd_a0= new opcode() { public void handler(){ illegal_1.handler(); op_a0.handler();									}}; /* DB   DD		  */
    opcode dd_a1= new opcode() { public void handler(){ illegal_1.handler(); op_a1.handler();									}}; /* DB   DD		  */
    opcode dd_a2= new opcode() { public void handler(){ illegal_1.handler(); op_a2.handler();									}}; /* DB   DD		  */
    opcode dd_a3= new opcode() { public void handler(){ illegal_1.handler(); op_a3.handler();									}}; /* DB   DD		  */
    opcode dd_a7= new opcode() { public void handler(){ illegal_1.handler(); op_a7.handler();									}}; /* DB   DD		  */
    opcode dd_a8= new opcode() { public void handler(){ illegal_1.handler(); op_a8.handler();									}}; /* DB   DD		  */
    opcode dd_a9= new opcode() { public void handler(){ illegal_1.handler(); op_a9.handler();									}}; /* DB   DD		  */
    opcode dd_aa= new opcode() { public void handler(){ illegal_1.handler(); op_aa.handler();									}}; /* DB   DD		  */
    opcode dd_ab= new opcode() { public void handler(){ illegal_1.handler(); op_ab.handler();									}}; /* DB   DD		  */
    opcode dd_af= new opcode() { public void handler(){ illegal_1.handler(); op_af.handler();									}}; /* DB   DD		  */
    opcode dd_b0= new opcode() { public void handler(){ illegal_1.handler(); op_b0.handler();									}}; /* DB   DD		  */
    opcode dd_b1= new opcode() { public void handler(){ illegal_1.handler(); op_b1.handler();									}}; /* DB   DD		  */
    opcode dd_b2= new opcode() { public void handler(){ illegal_1.handler(); op_b2.handler();									}}; /* DB   DD		  */
    opcode dd_b3= new opcode() { public void handler(){ illegal_1.handler(); op_b3.handler();									}}; /* DB   DD		  */
    opcode dd_b7= new opcode() { public void handler(){ illegal_1.handler(); op_b7.handler();									}}; /* DB   DD		  */
    opcode dd_b8= new opcode() { public void handler(){ illegal_1.handler(); op_b8.handler();									}}; /* DB   DD		  */
    opcode dd_b9= new opcode() { public void handler(){ illegal_1.handler(); op_b9.handler();									}}; /* DB   DD		  */
    opcode dd_ba= new opcode() { public void handler(){ illegal_1.handler(); op_ba.handler();									}}; /* DB   DD		  */
    opcode dd_bb= new opcode() { public void handler(){ illegal_1.handler(); op_bb.handler();									}}; /* DB   DD		  */
    opcode dd_bf= new opcode() { public void handler(){ illegal_1.handler(); op_bf.handler();									}}; /* DB   DD		  */
    opcode dd_c0= new opcode() { public void handler(){ illegal_1.handler(); op_c0.handler();									}}; /* DB   DD		  */
    opcode dd_c1= new opcode() { public void handler(){ illegal_1.handler(); op_c1.handler();									}}; /* DB   DD		  */
    opcode dd_c2= new opcode() { public void handler(){ illegal_1.handler(); op_c2.handler();									}}; /* DB   DD		  */
    opcode dd_c3= new opcode() { public void handler(){ illegal_1.handler(); op_c3.handler();									}}; /* DB   DD		  */
    opcode dd_c4= new opcode() { public void handler(){ illegal_1.handler(); op_c4.handler();									}}; /* DB   DD		  */
    opcode dd_c5= new opcode() { public void handler(){ illegal_1.handler(); op_c5.handler();									}}; /* DB   DD		  */
    opcode dd_c6= new opcode() { public void handler(){ illegal_1.handler(); op_c6.handler();									}}; /* DB   DD		  */
    opcode dd_c7= new opcode() { public void handler(){ illegal_1.handler(); op_c7.handler();									}};		  /* DB   DD		  */
    opcode dd_c8= new opcode() { public void handler(){ illegal_1.handler(); op_c8.handler();									}}; /* DB   DD		  */
    opcode dd_c9= new opcode() { public void handler(){ illegal_1.handler(); op_c9.handler();									}}; /* DB   DD		  */
    opcode dd_ca= new opcode() { public void handler(){ illegal_1.handler(); op_ca.handler();									}}; /* DB   DD		  */
    opcode dd_cc= new opcode() { public void handler(){ illegal_1.handler(); op_cc.handler();									}}; /* DB   DD		  */
    opcode dd_cd= new opcode() { public void handler(){ illegal_1.handler(); op_cd.handler();									}}; /* DB   DD		  */
    opcode dd_ce= new opcode() { public void handler(){ illegal_1.handler(); op_ce.handler();									}}; /* DB   DD		  */
    opcode dd_cf= new opcode() { public void handler(){ illegal_1.handler(); op_cf.handler();									}}; /* DB   DD		  */
    opcode dd_d0= new opcode() { public void handler(){ illegal_1.handler(); op_d0.handler();									}}; /* DB   DD		  */
    opcode dd_d1= new opcode() { public void handler(){ illegal_1.handler(); op_d1.handler();									}}; /* DB   DD		  */
    opcode dd_d2= new opcode() { public void handler(){ illegal_1.handler(); op_d2.handler();									}}; /* DB   DD		  */
    opcode dd_d3= new opcode() { public void handler(){ illegal_1.handler(); op_d3.handler();									}}; /* DB   DD		  */
    opcode dd_d4= new opcode() { public void handler(){ illegal_1.handler(); op_d4.handler();									}}; /* DB   DD		  */
    opcode dd_d5= new opcode() { public void handler(){ illegal_1.handler(); op_d5.handler();									}}; /* DB   DD		  */
    opcode dd_d6= new opcode() { public void handler(){ illegal_1.handler(); op_d6.handler();									}}; /* DB   DD		  */
    opcode dd_d7= new opcode() { public void handler(){ illegal_1.handler(); op_d7.handler();									}}; /* DB   DD		  */
    opcode dd_d8= new opcode() { public void handler(){ illegal_1.handler(); op_d8.handler();									}}; /* DB   DD		  */
    opcode dd_d9= new opcode() { public void handler(){ illegal_1.handler(); op_d9.handler();									}}; /* DB   DD		  */
    opcode dd_da= new opcode() { public void handler(){ illegal_1.handler(); op_da.handler();									}}; /* DB   DD		  */
    opcode dd_db= new opcode() { public void handler(){ illegal_1.handler(); op_db.handler();									}}; /* DB   DD		  */
    opcode dd_dc= new opcode() { public void handler(){ illegal_1.handler(); op_dc.handler();									}}; /* DB   DD		  */
    opcode dd_dd= new opcode() { public void handler(){ illegal_1.handler(); op_dd.handler();									}}; /* DB   DD		  */
    opcode dd_de= new opcode() { public void handler(){ illegal_1.handler(); op_de.handler();									}}; /* DB   DD		  */
    opcode dd_df= new opcode() { public void handler(){ illegal_1.handler(); op_df.handler();									}}; /* DB   DD		  */
    opcode dd_e0= new opcode() { public void handler(){ illegal_1.handler(); op_e0.handler();									}}; /* DB   DD		  */
    opcode dd_e2= new opcode() { public void handler(){ illegal_1.handler(); op_e2.handler();									}}; /* DB   DD		  */
    opcode dd_e4= new opcode() { public void handler(){ illegal_1.handler(); op_e4.handler();									}}; /* DB   DD		  */
    opcode dd_e6= new opcode() { public void handler(){ illegal_1.handler(); op_e6.handler();									}}; /* DB   DD		  */
    opcode dd_e7= new opcode() { public void handler(){ illegal_1.handler(); op_e7.handler();									}}; /* DB   DD		  */
    opcode dd_e8= new opcode() { public void handler(){ illegal_1.handler(); op_e8.handler();									}}; /* DB   DD		  */
    opcode dd_ea= new opcode() { public void handler(){ illegal_1.handler(); op_ea.handler();									}}; /* DB   DD		  */
    opcode dd_eb= new opcode() { public void handler(){ illegal_1.handler(); op_eb.handler();									}}; /* DB   DD		  */
    opcode dd_ec= new opcode() { public void handler(){ illegal_1.handler(); op_ec.handler();									}}; /* DB   DD		  */
    opcode dd_ed= new opcode() { public void handler(){ illegal_1.handler(); op_ed.handler();									}}; /* DB   DD		  */
    opcode dd_ee= new opcode() { public void handler(){ illegal_1.handler(); op_ee.handler();									}}; /* DB   DD		  */
    opcode dd_ef= new opcode() { public void handler(){ illegal_1.handler(); op_ef.handler();									}}; /* DB   DD		  */
    opcode dd_f0= new opcode() { public void handler(){ illegal_1.handler(); op_f0.handler();									}}; /* DB   DD		  */
    opcode dd_f1= new opcode() { public void handler(){ illegal_1.handler(); op_f1.handler();									}}; /* DB   DD		  */
    opcode dd_f2= new opcode() { public void handler(){ illegal_1.handler(); op_f2.handler();									}}; /* DB   DD		  */
    opcode dd_f3= new opcode() { public void handler(){ illegal_1.handler(); op_f3.handler();									}}; /* DB   DD		  */
    opcode dd_f4= new opcode() { public void handler(){ illegal_1.handler(); op_f4.handler();									}}; /* DB   DD		  */
    opcode dd_f5= new opcode() { public void handler(){ illegal_1.handler(); op_f5.handler();									}}; /* DB   DD		  */
    opcode dd_f6= new opcode() { public void handler(){ illegal_1.handler(); op_f6.handler();									}}; /* DB   DD		  */
    opcode dd_f7= new opcode() { public void handler(){ illegal_1.handler(); op_f7.handler();									}}; /* DB   DD		  */
    opcode dd_f8= new opcode() { public void handler(){ illegal_1.handler(); op_f8.handler();									}}; /* DB   DD		  */
    opcode dd_fa= new opcode() { public void handler(){ illegal_1.handler(); op_fa.handler();									}}; /* DB   DD		  */
    opcode dd_fb= new opcode() { public void handler(){ illegal_1.handler(); op_fb.handler();									}}; /* DB   DD		  */
    opcode dd_fc= new opcode() { public void handler(){ illegal_1.handler(); op_fc.handler();									}}; /* DB   DD		  */
    opcode dd_fd= new opcode() { public void handler(){ illegal_1.handler(); op_fd.handler();									}}; /* DB   DD		  */
    opcode dd_fe= new opcode() { public void handler(){ illegal_1.handler(); op_fe.handler();									}}; /* DB   DD		  */
    opcode dd_ff= new opcode() { public void handler(){ illegal_1.handler(); op_ff.handler();									}}; /* DB   DD		  */
    opcode fd_00= new opcode() { public void handler(){ illegal_1.handler(); op_00.handler();									}}; /* DB   FD		  */
    opcode fd_01= new opcode() { public void handler(){ illegal_1.handler(); op_01.handler();									}}; /* DB   FD		  */
    opcode fd_02= new opcode() { public void handler(){ illegal_1.handler(); op_02.handler();									}}; /* DB   FD		  */
    opcode fd_03= new opcode() { public void handler(){ illegal_1.handler(); op_03.handler();									}}; /* DB   FD		  */
    opcode fd_04= new opcode() { public void handler(){ illegal_1.handler(); op_04.handler();									}}; /* DB   FD		  */
    opcode fd_05= new opcode() { public void handler(){ illegal_1.handler(); op_05.handler();									}}; /* DB   FD		  */
    opcode fd_06= new opcode() { public void handler(){ illegal_1.handler(); op_06.handler();									}}; /* DB   FD		  */
    opcode fd_07= new opcode() { public void handler(){ illegal_1.handler(); op_07.handler();									}}; /* DB   FD		  */
    opcode fd_08= new opcode() { public void handler(){ illegal_1.handler(); op_08.handler();									}}; /* DB   FD		  */
    opcode fd_0a= new opcode() { public void handler(){ illegal_1.handler(); op_0a.handler();									}}; /* DB   FD		  */
    opcode fd_0b= new opcode() { public void handler(){ illegal_1.handler(); op_0b.handler();									}}; /* DB   FD		  */
    opcode fd_0c= new opcode() { public void handler(){ illegal_1.handler(); op_0c.handler();									}}; /* DB   FD		  */
    opcode fd_0d= new opcode() { public void handler(){ illegal_1.handler(); op_0d.handler();									}}; /* DB   FD		  */
    opcode fd_0e= new opcode() { public void handler(){ illegal_1.handler(); op_0e.handler();									}}; /* DB   FD		  */
    opcode fd_0f= new opcode() { public void handler(){ illegal_1.handler(); op_0f.handler();									}}; /* DB   FD		  */
    opcode fd_10= new opcode() { public void handler(){ illegal_1.handler(); op_10.handler();									}}; /* DB   FD		  */
    opcode fd_11= new opcode() { public void handler(){ illegal_1.handler(); op_11.handler();									}}; /* DB   FD		  */
    opcode fd_12= new opcode() { public void handler(){ illegal_1.handler(); op_12.handler();									}}; /* DB   FD		  */
    opcode fd_13= new opcode() { public void handler(){ illegal_1.handler(); op_13.handler();									}}; /* DB   FD		  */
    opcode fd_14= new opcode() { public void handler(){ illegal_1.handler(); op_14.handler();									}}; /* DB   FD		  */
    opcode fd_15= new opcode() { public void handler(){ illegal_1.handler(); op_15.handler();									}}; /* DB   FD		  */
    opcode fd_16= new opcode() { public void handler(){ illegal_1.handler(); op_16.handler();									}}; /* DB   FD		  */
    opcode fd_17= new opcode() { public void handler(){ illegal_1.handler(); op_17.handler();									}}; /* DB   FD		  */
    opcode fd_18= new opcode() { public void handler(){ illegal_1.handler(); op_18.handler();									}}; /* DB   FD		  */
    opcode fd_1a= new opcode() { public void handler(){ illegal_1.handler(); op_1a.handler();									}}; /* DB   FD		  */
    opcode fd_1b= new opcode() { public void handler(){ illegal_1.handler(); op_1b.handler();									}}; /* DB   FD		  */
    opcode fd_1c= new opcode() { public void handler(){ illegal_1.handler(); op_1c.handler();									}}; /* DB   FD		  */
    opcode fd_1d= new opcode() { public void handler(){ illegal_1.handler(); op_1d.handler();									}}; /* DB   FD		  */
    opcode fd_1e= new opcode() { public void handler(){ illegal_1.handler(); op_1e.handler();									}}; /* DB   FD		  */
    opcode fd_1f= new opcode() { public void handler(){ illegal_1.handler(); op_1f.handler();									}}; /* DB   FD		  */
    opcode fd_20= new opcode() { public void handler(){ illegal_1.handler(); op_20.handler();									}}; /* DB   FD		  */
    opcode fd_27= new opcode() { public void handler(){ illegal_1.handler(); op_27.handler();									}}; /* DB   FD		  */
    opcode fd_28= new opcode() { public void handler(){ illegal_1.handler(); op_28.handler();									}}; /* DB   FD		  */
    opcode fd_2f= new opcode() { public void handler(){ illegal_1.handler(); op_2f.handler();									}}; /* DB   FD		  */
    opcode fd_30= new opcode() { public void handler(){ illegal_1.handler(); op_30.handler();									}}; /* DB   FD		  */
    opcode fd_31= new opcode() { public void handler(){ illegal_1.handler(); op_31.handler();									}}; /* DB   FD		  */
    opcode fd_32= new opcode() { public void handler(){ illegal_1.handler(); op_32.handler();									}}; /* DB   FD		  */
    opcode fd_33= new opcode() { public void handler(){ illegal_1.handler(); op_33.handler();									}}; /* DB   FD		  */
    opcode fd_37= new opcode() { public void handler(){ illegal_1.handler(); op_37.handler();									}}; /* DB   FD		  */
    opcode fd_38= new opcode() { public void handler(){ illegal_1.handler(); op_38.handler();									}}; /* DB   FD		  */
    opcode fd_3a= new opcode() { public void handler(){ illegal_1.handler(); op_3a.handler();									}}; /* DB   FD		  */
    opcode fd_3b= new opcode() { public void handler(){ illegal_1.handler(); op_3b.handler();									}}; /* DB   FD		  */
    opcode fd_3c= new opcode() { public void handler(){ illegal_1.handler(); op_3c.handler();									}}; /* DB   FD		  */
    opcode fd_3d= new opcode() { public void handler(){ illegal_1.handler(); op_3d.handler();									}}; /* DB   FD		  */
    opcode fd_3e= new opcode() { public void handler(){ illegal_1.handler(); op_3e.handler();									}}; /* DB   FD		  */
    opcode fd_3f= new opcode() { public void handler(){ illegal_1.handler(); op_3f.handler();									}}; /* DB   FD		  */
    opcode fd_40= new opcode() { public void handler(){ illegal_1.handler(); op_40.handler();									}}; /* DB   FD		  */
    opcode fd_41= new opcode() { public void handler(){ illegal_1.handler(); op_41.handler();									}}; /* DB   FD		  */
    opcode fd_42= new opcode() { public void handler(){ illegal_1.handler(); op_42.handler();									}}; /* DB   FD		  */
    opcode fd_43= new opcode() { public void handler(){ illegal_1.handler(); op_43.handler();									}}; /* DB   FD		  */
    opcode fd_47= new opcode() { public void handler(){ illegal_1.handler(); op_47.handler();									}}; /* DB   FD		  */
    opcode fd_48= new opcode() { public void handler(){ illegal_1.handler(); op_48.handler();									}}; /* DB   FD		  */
    opcode fd_49= new opcode() { public void handler(){ illegal_1.handler(); op_49.handler();									}}; /* DB   FD		  */
    opcode fd_4a= new opcode() { public void handler(){ illegal_1.handler(); op_4a.handler();									}}; /* DB   FD		  */
    opcode fd_4b= new opcode() { public void handler(){ illegal_1.handler(); op_4b.handler();									}}; /* DB   FD		  */
    opcode fd_4f= new opcode() { public void handler(){ illegal_1.handler(); op_4f.handler();									}}; /* DB   FD		  */
    opcode fd_50= new opcode() { public void handler(){ illegal_1.handler(); op_50.handler();									}}; /* DB   FD		  */
    opcode fd_51= new opcode() { public void handler(){ illegal_1.handler(); op_51.handler();									}}; /* DB   FD		  */
    opcode fd_52= new opcode() { public void handler(){ illegal_1.handler(); op_52.handler();									}}; /* DB   FD		  */
    opcode fd_53= new opcode() { public void handler(){ illegal_1.handler(); op_53.handler();									}}; /* DB   FD		  */
    opcode fd_57= new opcode() { public void handler(){ illegal_1.handler(); op_57.handler();									}}; /* DB   FD		  */
    opcode fd_58= new opcode() { public void handler(){ illegal_1.handler(); op_58.handler();									}}; /* DB   FD		  */
    opcode fd_59= new opcode() { public void handler(){ illegal_1.handler(); op_59.handler();									}}; /* DB   FD		  */
    opcode fd_5a= new opcode() { public void handler(){ illegal_1.handler(); op_5a.handler();									}}; /* DB   FD		  */
    opcode fd_5b= new opcode() { public void handler(){ illegal_1.handler(); op_5b.handler();									}}; /* DB   FD		  */
    opcode fd_5f= new opcode() { public void handler(){ illegal_1.handler(); op_5f.handler();									}}; /* DB   FD		  */
    opcode fd_76= new opcode() { public void handler(){ illegal_1.handler(); op_76.handler();									}};		  /* DB   FD		  */
    opcode fd_78= new opcode() { public void handler(){ illegal_1.handler(); op_78.handler();									}}; /* DB   FD		  */
    opcode fd_79= new opcode() { public void handler(){ illegal_1.handler(); op_79.handler();									}}; /* DB   FD		  */
    opcode fd_7a= new opcode() { public void handler(){ illegal_1.handler(); op_7a.handler();									}}; /* DB   FD		  */
    opcode fd_7b= new opcode() { public void handler(){ illegal_1.handler(); op_7b.handler();									}}; /* DB   FD		  */
    opcode fd_7f= new opcode() { public void handler(){ illegal_1.handler(); op_7f.handler();									}}; /* DB   FD		  */
    opcode fd_80= new opcode() { public void handler(){ illegal_1.handler(); op_80.handler();									}}; /* DB   FD		  */
    opcode fd_81= new opcode() { public void handler(){ illegal_1.handler(); op_81.handler();									}}; /* DB   FD		  */
    opcode fd_82= new opcode() { public void handler(){ illegal_1.handler(); op_82.handler();									}}; /* DB   FD		  */
    opcode fd_83= new opcode() { public void handler(){ illegal_1.handler(); op_83.handler();									}}; /* DB   FD		  */
    opcode fd_87= new opcode() { public void handler(){ illegal_1.handler(); op_87.handler();									}}; /* DB   FD		  */
    opcode fd_88= new opcode() { public void handler(){ illegal_1.handler(); op_88.handler();									}}; /* DB   FD		  */
    opcode fd_89= new opcode() { public void handler(){ illegal_1.handler(); op_89.handler();									}}; /* DB   FD		  */
    opcode fd_8a= new opcode() { public void handler(){ illegal_1.handler(); op_8a.handler();									}}; /* DB   FD		  */
    opcode fd_8b= new opcode() { public void handler(){ illegal_1.handler(); op_8b.handler();									}}; /* DB   FD		  */
    opcode fd_8f= new opcode() { public void handler(){ illegal_1.handler(); op_8f.handler();									}}; /* DB   FD		  */
    opcode fd_90= new opcode() { public void handler(){ illegal_1.handler(); op_90.handler();									}}; /* DB   FD		  */
    opcode fd_91= new opcode() { public void handler(){ illegal_1.handler(); op_91.handler();									}}; /* DB   FD		  */
    opcode fd_92= new opcode() { public void handler(){ illegal_1.handler(); op_92.handler();									}}; /* DB   FD		  */
    opcode fd_93= new opcode() { public void handler(){ illegal_1.handler(); op_93.handler();									}}; /* DB   FD		  */
    opcode fd_97= new opcode() { public void handler(){ illegal_1.handler(); op_97.handler();									}}; /* DB   FD		  */
    opcode fd_98= new opcode() { public void handler(){ illegal_1.handler(); op_98.handler();									}}; /* DB   FD		  */
    opcode fd_99= new opcode() { public void handler(){ illegal_1.handler(); op_99.handler();									}}; /* DB   FD		  */
    opcode fd_9a= new opcode() { public void handler(){ illegal_1.handler(); op_9a.handler();									}}; /* DB   FD		  */
    opcode fd_9b= new opcode() { public void handler(){ illegal_1.handler(); op_9b.handler();									}}; /* DB   FD		  */
    opcode fd_9f= new opcode() { public void handler(){ illegal_1.handler(); op_9f.handler();									}}; /* DB   FD		  */
    opcode fd_a0= new opcode() { public void handler(){ illegal_1.handler(); op_a0.handler();									}}; /* DB   FD		  */
    opcode fd_a1= new opcode() { public void handler(){ illegal_1.handler(); op_a1.handler();									}}; /* DB   FD		  */
    opcode fd_a2= new opcode() { public void handler(){ illegal_1.handler(); op_a2.handler();									}}; /* DB   FD		  */
    opcode fd_a3= new opcode() { public void handler(){ illegal_1.handler(); op_a3.handler();									}}; /* DB   FD		  */
    opcode fd_a7= new opcode() { public void handler(){ illegal_1.handler(); op_a7.handler();									}}; /* DB   FD		  */
    opcode fd_a8= new opcode() { public void handler(){ illegal_1.handler(); op_a8.handler();									}}; /* DB   FD		  */
    opcode fd_a9= new opcode() { public void handler(){ illegal_1.handler(); op_a9.handler();									}}; /* DB   FD		  */
    opcode fd_aa= new opcode() { public void handler(){ illegal_1.handler(); op_aa.handler();									}}; /* DB   FD		  */
    opcode fd_ab= new opcode() { public void handler(){ illegal_1.handler(); op_ab.handler();									}}; /* DB   FD		  */
    opcode fd_af= new opcode() { public void handler(){ illegal_1.handler(); op_af.handler();									}}; /* DB   FD		  */
    opcode fd_b0= new opcode() { public void handler(){ illegal_1.handler(); op_b0.handler();									}}; /* DB   FD		  */
    opcode fd_b1= new opcode() { public void handler(){ illegal_1.handler(); op_b1.handler();									}}; /* DB   FD		  */
    opcode fd_b2= new opcode() { public void handler(){ illegal_1.handler(); op_b2.handler();									}}; /* DB   FD		  */
    opcode fd_b3= new opcode() { public void handler(){ illegal_1.handler(); op_b3.handler();									}}; /* DB   FD		  */
    opcode fd_b7= new opcode() { public void handler(){ illegal_1.handler(); op_b7.handler();									}}; /* DB   FD		  */
    opcode fd_b8= new opcode() { public void handler(){ illegal_1.handler(); op_b8.handler();									}}; /* DB   FD		  */
    opcode fd_b9= new opcode() { public void handler(){ illegal_1.handler(); op_b9.handler();									}}; /* DB   FD		  */
    opcode fd_ba= new opcode() { public void handler(){ illegal_1.handler(); op_ba.handler();									}}; /* DB   FD		  */
    opcode fd_bb= new opcode() { public void handler(){ illegal_1.handler(); op_bb.handler();									}}; /* DB   FD		  */
    opcode fd_bf= new opcode() { public void handler(){ illegal_1.handler(); op_bf.handler();									}}; /* DB   FD		  */
    opcode fd_c0= new opcode() { public void handler(){ illegal_1.handler(); op_c0.handler();									}}; /* DB   FD		  */
    opcode fd_c1= new opcode() { public void handler(){ illegal_1.handler(); op_c1.handler();									}}; /* DB   FD		  */
    opcode fd_c2= new opcode() { public void handler(){ illegal_1.handler(); op_c2.handler();									}}; /* DB   FD		  */
    opcode fd_c3= new opcode() { public void handler(){ illegal_1.handler(); op_c3.handler();									}}; /* DB   FD		  */
    opcode fd_c4= new opcode() { public void handler(){ illegal_1.handler(); op_c4.handler();									}}; /* DB   FD		  */
    opcode fd_c5= new opcode() { public void handler(){ illegal_1.handler(); op_c5.handler();									}}; /* DB   FD		  */
    opcode fd_c6= new opcode() { public void handler(){ illegal_1.handler(); op_c6.handler();									}}; /* DB   FD		  */
    opcode fd_c7= new opcode() { public void handler(){ illegal_1.handler(); op_c7.handler();									}}; /* DB   FD		  */
    opcode fd_c8= new opcode() { public void handler(){ illegal_1.handler(); op_c8.handler();									}}; /* DB   FD		  */
    opcode fd_c9= new opcode() { public void handler(){ illegal_1.handler(); op_c9.handler();									}}; /* DB   FD		  */
    opcode fd_ca= new opcode() { public void handler(){ illegal_1.handler(); op_ca.handler();									}}; /* DB   FD		  */
    opcode fd_cc= new opcode() { public void handler(){ illegal_1.handler(); op_cc.handler();									}}; /* DB   FD		  */
    opcode fd_cd= new opcode() { public void handler(){ illegal_1.handler(); op_cd.handler();									}}; /* DB   FD		  */
    opcode fd_ce= new opcode() { public void handler(){ illegal_1.handler(); op_ce.handler();									}}; /* DB   FD		  */
    opcode fd_cf= new opcode() { public void handler(){ illegal_1.handler(); op_cf.handler();									}}; /* DB   FD		  */
    opcode fd_d0= new opcode() { public void handler(){ illegal_1.handler(); op_d0.handler();									}}; /* DB   FD		  */
    opcode fd_d1= new opcode() { public void handler(){ illegal_1.handler(); op_d1.handler();									}}; /* DB   FD		  */
    opcode fd_d2= new opcode() { public void handler(){ illegal_1.handler(); op_d2.handler();									}}; /* DB   FD		  */
    opcode fd_d3= new opcode() { public void handler(){ illegal_1.handler(); op_d3.handler();									}}; /* DB   FD		  */
    opcode fd_d4= new opcode() { public void handler(){ illegal_1.handler(); op_d4.handler();									}}; /* DB   FD		  */
    opcode fd_d5= new opcode() { public void handler(){ illegal_1.handler(); op_d5.handler();									}}; /* DB   FD		  */
    opcode fd_d6= new opcode() { public void handler(){ illegal_1.handler(); op_d6.handler();									}}; /* DB   FD		  */
    opcode fd_d7= new opcode() { public void handler(){ illegal_1.handler(); op_d7.handler();									}}; /* DB   FD		  */
    opcode fd_d8= new opcode() { public void handler(){ illegal_1.handler(); op_d8.handler();									}}; /* DB   FD		  */
    opcode fd_d9= new opcode() { public void handler(){ illegal_1.handler(); op_d9.handler();									}}; /* DB   FD		  */
    opcode fd_da= new opcode() { public void handler(){ illegal_1.handler(); op_da.handler();									}}; /* DB   FD		  */
    opcode fd_db= new opcode() { public void handler(){ illegal_1.handler(); op_db.handler();									}}; /* DB   FD		  */
    opcode fd_dc= new opcode() { public void handler(){ illegal_1.handler(); op_dc.handler();									}}; /* DB   FD		  */
    opcode fd_dd= new opcode() { public void handler(){ illegal_1.handler(); op_dd.handler();									}}; /* DB   FD		  */
    opcode fd_de= new opcode() { public void handler(){ illegal_1.handler(); op_de.handler();									}}; /* DB   FD		  */
    opcode fd_df= new opcode() { public void handler(){ illegal_1.handler(); op_df.handler();									}}; /* DB   FD		  */
    opcode fd_e0= new opcode() { public void handler(){ illegal_1.handler(); op_e0.handler();									}}; /* DB   FD		  */
    opcode fd_e2= new opcode() { public void handler(){ illegal_1.handler(); op_e2.handler();									}}; /* DB   FD		  */
    opcode fd_e4= new opcode() { public void handler(){ illegal_1.handler(); op_e4.handler();									}}; /* DB   FD		  */
    opcode fd_e6= new opcode() { public void handler(){ illegal_1.handler(); op_e6.handler();									}}; /* DB   FD		  */
    opcode fd_e7= new opcode() { public void handler(){ illegal_1.handler(); op_e7.handler();									}}; /* DB   FD		  */
    opcode fd_e8= new opcode() { public void handler(){ illegal_1.handler(); op_e8.handler();									}}; /* DB   FD		  */
    opcode fd_ea= new opcode() { public void handler(){ illegal_1.handler(); op_ea.handler();									}}; /* DB   FD		  */
    opcode fd_eb= new opcode() { public void handler(){ illegal_1.handler(); op_eb.handler();									}}; /* DB   FD		  */
    opcode fd_ec= new opcode() { public void handler(){ illegal_1.handler(); op_ec.handler();									}}; /* DB   FD		  */
    opcode fd_ed= new opcode() { public void handler(){ illegal_1.handler(); op_ed.handler();									}}; /* DB   FD		  */
    opcode fd_ee= new opcode() { public void handler(){ illegal_1.handler(); op_ee.handler();									}}; /* DB   FD		  */
    opcode fd_ef= new opcode() { public void handler(){ illegal_1.handler(); op_ef.handler();									}}; /* DB   FD		  */
    opcode fd_f0= new opcode() { public void handler(){ illegal_1.handler(); op_f0.handler();									}}; /* DB   FD		  */
    opcode fd_f1= new opcode() { public void handler(){ illegal_1.handler(); op_f1.handler();									}}; /* DB   FD		  */
    opcode fd_f2= new opcode() { public void handler(){ illegal_1.handler(); op_f2.handler();									}}; /* DB   FD		  */
    opcode fd_f3= new opcode() { public void handler(){ illegal_1.handler(); op_f3.handler();									}}; /* DB   FD		  */
    opcode fd_f4= new opcode() { public void handler(){ illegal_1.handler(); op_f4.handler();									}}; /* DB   FD		  */
    opcode fd_f5= new opcode() { public void handler(){ illegal_1.handler(); op_f5.handler();									}}; /* DB   FD		  */
    opcode fd_f6= new opcode() { public void handler(){ illegal_1.handler(); op_f6.handler();									}}; /* DB   FD		  */
    opcode fd_f7= new opcode() { public void handler(){ illegal_1.handler(); op_f7.handler();									}}; /* DB   FD		  */
    opcode fd_f8= new opcode() { public void handler(){ illegal_1.handler(); op_f8.handler();									}}; /* DB   FD		  */
    opcode fd_fa= new opcode() { public void handler(){ illegal_1.handler(); op_fa.handler();									}}; /* DB   FD		  */
    opcode fd_fb= new opcode() { public void handler(){ illegal_1.handler(); op_fb.handler();									}}; /* DB   FD		  */
    opcode fd_fc= new opcode() { public void handler(){ illegal_1.handler(); op_fc.handler();									}}; /* DB   FD		  */
    opcode fd_fd= new opcode() { public void handler(){ illegal_1.handler(); op_fd.handler();									}}; /* DB   FD		  */
    opcode fd_fe= new opcode() { public void handler(){ illegal_1.handler(); op_fe.handler();									}}; /* DB   FD		  */
    opcode fd_ff= new opcode() { public void handler(){ illegal_1.handler(); op_ff.handler();									}}; /* DB   FD		  */  
    opcode dd_1a = new opcode(){ public void handler(){ illegal_1.handler(); op_1a.handler();									}}; /* DB   FD		  */
    opcode dd_1b = new opcode(){ public void handler(){ illegal_1.handler(); op_1b.handler();									}}; /* DB   FD		  */
    opcode dd_1c = new opcode(){ public void handler(){ illegal_1.handler(); op_1c.handler();									}}; /* DB   FD		  */
    opcode dd_1d = new opcode(){ public void handler(){ illegal_1.handler(); op_1d.handler();									}}; /* DB   FD		  */
    opcode dd_1e = new opcode(){ public void handler(){ illegal_1.handler(); op_1e.handler();									}}; /* DB   FD		  */
    opcode dd_1f = new opcode(){ public void handler(){ illegal_1.handler(); op_1f.handler();									}}; /* DB   FD		  */
    opcode dd_20 = new opcode(){ public void handler(){ illegal_1.handler(); op_20.handler();									}}; /* DB   FD		  */
    /**********************************************************
     * illegal_2 opcodes
     **********************************************************/
    opcode ed_00= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_01= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_02= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_03= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_04= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_05= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_06= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_07= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_08= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_09= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_0a= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_0b= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_0c= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_0d= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_0e= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_0f= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_10= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_11= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_12= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_13= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_14= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_15= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_16= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_17= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_18= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_19= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_1a= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_1b= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_1c= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_1d= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_1e= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_1f= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_20= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_21= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_22= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_23= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_24= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_25= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_26= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_27= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_28= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_29= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_2a= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_2b= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_2c= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_2d= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_2e= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_2f= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_30= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_31= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_32= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_33= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_34= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_35= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_36= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_37= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_38= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_39= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_3a= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_3b= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_3c= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_3d= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_3e= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_3f= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_77= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED,77 	  */
    opcode ed_7f= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED,7F 	  */
    opcode ed_80= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_81= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_82= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_83= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_84= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_85= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_86= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_87= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_88= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_89= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_8a= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_8b= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_8c= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_8d= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_8e= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_8f= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_90= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_91= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_92= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_a4= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_a5= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_a6= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_a7= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_93= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_94= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_95= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_96= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_97= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_98= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_99= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_9a= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_9b= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_9c= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_9d= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_9e= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_9f= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_ac= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_ad= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_ae= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_af= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_b4= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_b5= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_b6= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_b7= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_bc= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_bd= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_be= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_bf= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_c0= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_c1= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_c2= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_c3= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_c4= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_c5= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_c6= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_c7= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_c8= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_c9= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_ca= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_cb= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_cc= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_cd= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_ce= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_cf= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_d0= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_d1= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_d2= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_d3= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_d4= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_d5= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_d6= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_d7= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_d8= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_d9= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_da= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_db= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_dc= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_dd= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_de= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_df= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_e0= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_e1= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_e2= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_e3= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_e4= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_e5= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_e6= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_e7= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_e8= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_e9= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_ea= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_eb= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_ec= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_ed= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_ee= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_ef= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_f0= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_f1= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_f2= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_f3= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_f4= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_f5= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_f6= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_f7= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_f8= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_f9= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_fa= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_fb= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_fc= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_fd= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_fe= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
    opcode ed_ff= new opcode() { public void handler(){ illegal_2.handler();											}}; /* DB   ED		  */
  
    opcode[] Z80op = {
        op_00, op_01, op_02, op_03, op_04, op_05, op_06, op_07,
        op_08, op_09, op_0a, op_0b, op_0c, op_0d, op_0e, op_0f,
        op_10, op_11, op_12, op_13, op_14, op_15, op_16, op_17,
        op_18, op_19, op_1a, op_1b, op_1c, op_1d, op_1e, op_1f,
        op_20, op_21, op_22, op_23, op_24, op_25, op_26, op_27,
        op_28, op_29, op_2a, op_2b, op_2c, op_2d, op_2e, op_2f,
        op_30, op_31, op_32, op_33, op_34, op_35, op_36, op_37,
        op_38, op_39, op_3a, op_3b, op_3c, op_3d, op_3e, op_3f,
        op_40, op_41, op_42, op_43, op_44, op_45, op_46, op_47,
        op_48, op_49, op_4a, op_4b, op_4c, op_4d, op_4e, op_4f,
        op_50, op_51, op_52, op_53, op_54, op_55, op_56, op_57,
        op_58, op_59, op_5a, op_5b, op_5c, op_5d, op_5e, op_5f,
        op_60, op_61, op_62, op_63, op_64, op_65, op_66, op_67,
        op_68, op_69, op_6a, op_6b, op_6c, op_6d, op_6e, op_6f,
        op_70, op_71, op_72, op_73, op_74, op_75, op_76, op_77,
        op_78, op_79, op_7a, op_7b, op_7c, op_7d, op_7e, op_7f,
        op_80, op_81, op_82, op_83, op_84, op_85, op_86, op_87,
        op_88, op_89, op_8a, op_8b, op_8c, op_8d, op_8e, op_8f,
        op_90, op_91, op_92, op_93, op_94, op_95, op_96, op_97,
        op_98, op_99, op_9a, op_9b, op_9c, op_9d, op_9e, op_9f,
        op_a0, op_a1, op_a2, op_a3, op_a4, op_a5, op_a6, op_a7,
        op_a8, op_a9, op_aa, op_ab, op_ac, op_ad, op_ae, op_af,
        op_b0, op_b1, op_b2, op_b3, op_b4, op_b5, op_b6, op_b7,
        op_b8, op_b9, op_ba, op_bb, op_bc, op_bd, op_be, op_bf,
        op_c0, op_c1, op_c2, op_c3, op_c4, op_c5, op_c6, op_c7,
        op_c8, op_c9, op_ca, op_cb, op_cc, op_cd, op_ce, op_cf,
        op_d0, op_d1, op_d2, op_d3, op_d4, op_d5, op_d6, op_d7,
        op_d8, op_d9, op_da, op_db, op_dc, op_dd, op_de, op_df,
        op_e0, op_e1, op_e2, op_e3, op_e4, op_e5, op_e6, op_e7,
        op_e8, op_e9, op_ea, op_eb, op_ec, op_ed, op_ee, op_ef,
        op_f0, op_f1, op_f2, op_f3, op_f4, op_f5, op_f6, op_f7,
        op_f8, op_f9, op_fa, op_fb, op_fc, op_fd, op_fe, op_ff
    };
    opcode[] Z80cb = {
        cb_00, cb_01, cb_02, cb_03, cb_04, cb_05, cb_06, cb_07,
        cb_08, cb_09, cb_0a, cb_0b, cb_0c, cb_0d, cb_0e, cb_0f,
        cb_10, cb_11, cb_12, cb_13, cb_14, cb_15, cb_16, cb_17,
        cb_18, cb_19, cb_1a, cb_1b, cb_1c, cb_1d, cb_1e, cb_1f,
        cb_20, cb_21, cb_22, cb_23, cb_24, cb_25, cb_26, cb_27,
        cb_28, cb_29, cb_2a, cb_2b, cb_2c, cb_2d, cb_2e, cb_2f,
        cb_30, cb_31, cb_32, cb_33, cb_34, cb_35, cb_36, cb_37,
        cb_38, cb_39, cb_3a, cb_3b, cb_3c, cb_3d, cb_3e, cb_3f,
        cb_40, cb_41, cb_42, cb_43, cb_44, cb_45, cb_46, cb_47,
        cb_48, cb_49, cb_4a, cb_4b, cb_4c, cb_4d, cb_4e, cb_4f,
        cb_50, cb_51, cb_52, cb_53, cb_54, cb_55, cb_56, cb_57,
        cb_58, cb_59, cb_5a, cb_5b, cb_5c, cb_5d, cb_5e, cb_5f,
        cb_60, cb_61, cb_62, cb_63, cb_64, cb_65, cb_66, cb_67,
        cb_68, cb_69, cb_6a, cb_6b, cb_6c, cb_6d, cb_6e, cb_6f,
        cb_70, cb_71, cb_72, cb_73, cb_74, cb_75, cb_76, cb_77,
        cb_78, cb_79, cb_7a, cb_7b, cb_7c, cb_7d, cb_7e, cb_7f,
        cb_80, cb_81, cb_82, cb_83, cb_84, cb_85, cb_86, cb_87,
        cb_88, cb_89, cb_8a, cb_8b, cb_8c, cb_8d, cb_8e, cb_8f,
        cb_90, cb_91, cb_92, cb_93, cb_94, cb_95, cb_96, cb_97,
        cb_98, cb_99, cb_9a, cb_9b, cb_9c, cb_9d, cb_9e, cb_9f,
        cb_a0, cb_a1, cb_a2, cb_a3, cb_a4, cb_a5, cb_a6, cb_a7,
        cb_a8, cb_a9, cb_aa, cb_ab, cb_ac, cb_ad, cb_ae, cb_af,
        cb_b0, cb_b1, cb_b2, cb_b3, cb_b4, cb_b5, cb_b6, cb_b7,
        cb_b8, cb_b9, cb_ba, cb_bb, cb_bc, cb_bd, cb_be, cb_bf,
        cb_c0, cb_c1, cb_c2, cb_c3, cb_c4, cb_c5, cb_c6, cb_c7,
        cb_c8, cb_c9, cb_ca, cb_cb, cb_cc, cb_cd, cb_ce, cb_cf,
        cb_d0, cb_d1, cb_d2, cb_d3, cb_d4, cb_d5, cb_d6, cb_d7,
        cb_d8, cb_d9, cb_da, cb_db, cb_dc, cb_dd, cb_de, cb_df,
        cb_e0, cb_e1, cb_e2, cb_e3, cb_e4, cb_e5, cb_e6, cb_e7,
        cb_e8, cb_e9, cb_ea, cb_eb, cb_ec, cb_ed, cb_ee, cb_ef,
        cb_f0, cb_f1, cb_f2, cb_f3, cb_f4, cb_f5, cb_f6, cb_f7,
        cb_f8, cb_f9, cb_fa, cb_fb, cb_fc, cb_fd, cb_fe, cb_ff
    };

    opcode[] Z80dd = {
        dd_00, dd_01, dd_02, dd_03, dd_04, dd_05, dd_06, dd_07,
        dd_08, dd_09, dd_0a, dd_0b, dd_0c, dd_0d, dd_0e, dd_0f,
        dd_10, dd_11, dd_12, dd_13, dd_14, dd_15, dd_16, dd_17,
        dd_18, dd_19, dd_1a, dd_1b, dd_1c, dd_1d, dd_1e, dd_1f,
        dd_20, dd_21, dd_22, dd_23, dd_24, dd_25, dd_26, dd_27,
        dd_28, dd_29, dd_2a, dd_2b, dd_2c, dd_2d, dd_2e, dd_2f,
        dd_30, dd_31, dd_32, dd_33, dd_34, dd_35, dd_36, dd_37,
        dd_38, dd_39, dd_3a, dd_3b, dd_3c, dd_3d, dd_3e, dd_3f,
        dd_40, dd_41, dd_42, dd_43, dd_44, dd_45, dd_46, dd_47,
        dd_48, dd_49, dd_4a, dd_4b, dd_4c, dd_4d, dd_4e, dd_4f,
        dd_50, dd_51, dd_52, dd_53, dd_54, dd_55, dd_56, dd_57,
        dd_58, dd_59, dd_5a, dd_5b, dd_5c, dd_5d, dd_5e, dd_5f,
        dd_60, dd_61, dd_62, dd_63, dd_64, dd_65, dd_66, dd_67,
        dd_68, dd_69, dd_6a, dd_6b, dd_6c, dd_6d, dd_6e, dd_6f,
        dd_70, dd_71, dd_72, dd_73, dd_74, dd_75, dd_76, dd_77,
        dd_78, dd_79, dd_7a, dd_7b, dd_7c, dd_7d, dd_7e, dd_7f,
        dd_80, dd_81, dd_82, dd_83, dd_84, dd_85, dd_86, dd_87,
        dd_88, dd_89, dd_8a, dd_8b, dd_8c, dd_8d, dd_8e, dd_8f,
        dd_90, dd_91, dd_92, dd_93, dd_94, dd_95, dd_96, dd_97,
        dd_98, dd_99, dd_9a, dd_9b, dd_9c, dd_9d, dd_9e, dd_9f,
        dd_a0, dd_a1, dd_a2, dd_a3, dd_a4, dd_a5, dd_a6, dd_a7,
        dd_a8, dd_a9, dd_aa, dd_ab, dd_ac, dd_ad, dd_ae, dd_af,
        dd_b0, dd_b1, dd_b2, dd_b3, dd_b4, dd_b5, dd_b6, dd_b7,
        dd_b8, dd_b9, dd_ba, dd_bb, dd_bc, dd_bd, dd_be, dd_bf,
        dd_c0, dd_c1, dd_c2, dd_c3, dd_c4, dd_c5, dd_c6, dd_c7,
        dd_c8, dd_c9, dd_ca, dd_cb, dd_cc, dd_cd, dd_ce, dd_cf,
        dd_d0, dd_d1, dd_d2, dd_d3, dd_d4, dd_d5, dd_d6, dd_d7,
        dd_d8, dd_d9, dd_da, dd_db, dd_dc, dd_dd, dd_de, dd_df,
        dd_e0, dd_e1, dd_e2, dd_e3, dd_e4, dd_e5, dd_e6, dd_e7,
        dd_e8, dd_e9, dd_ea, dd_eb, dd_ec, dd_ed, dd_ee, dd_ef,
        dd_f0, dd_f1, dd_f2, dd_f3, dd_f4, dd_f5, dd_f6, dd_f7,
        dd_f8, dd_f9, dd_fa, dd_fb, dd_fc, dd_fd, dd_fe, dd_ff
    };
    opcode[] Z80ed = {
        ed_00, ed_01, ed_02, ed_03, ed_04, ed_05, ed_06, ed_07,
        ed_08, ed_09, ed_0a, ed_0b, ed_0c, ed_0d, ed_0e, ed_0f,
        ed_10, ed_11, ed_12, ed_13, ed_14, ed_15, ed_16, ed_17,
        ed_18, ed_19, ed_1a, ed_1b, ed_1c, ed_1d, ed_1e, ed_1f,
        ed_20, ed_21, ed_22, ed_23, ed_24, ed_25, ed_26, ed_27,
        ed_28, ed_29, ed_2a, ed_2b, ed_2c, ed_2d, ed_2e, ed_2f,
        ed_30, ed_31, ed_32, ed_33, ed_34, ed_35, ed_36, ed_37,
        ed_38, ed_39, ed_3a, ed_3b, ed_3c, ed_3d, ed_3e, ed_3f,
        ed_40, ed_41, ed_42, ed_43, ed_44, ed_45, ed_46, ed_47,
        ed_48, ed_49, ed_4a, ed_4b, ed_4c, ed_4d, ed_4e, ed_4f,
        ed_50, ed_51, ed_52, ed_53, ed_54, ed_55, ed_56, ed_57,
        ed_58, ed_59, ed_5a, ed_5b, ed_5c, ed_5d, ed_5e, ed_5f,
        ed_60, ed_61, ed_62, ed_63, ed_64, ed_65, ed_66, ed_67,
        ed_68, ed_69, ed_6a, ed_6b, ed_6c, ed_6d, ed_6e, ed_6f,
        ed_70, ed_71, ed_72, ed_73, ed_74, ed_75, ed_76, ed_77,
        ed_78, ed_79, ed_7a, ed_7b, ed_7c, ed_7d, ed_7e, ed_7f,
        ed_80, ed_81, ed_82, ed_83, ed_84, ed_85, ed_86, ed_87,
        ed_88, ed_89, ed_8a, ed_8b, ed_8c, ed_8d, ed_8e, ed_8f,
        ed_90, ed_91, ed_92, ed_93, ed_94, ed_95, ed_96, ed_97,
        ed_98, ed_99, ed_9a, ed_9b, ed_9c, ed_9d, ed_9e, ed_9f,
        ed_a0, ed_a1, ed_a2, ed_a3, ed_a4, ed_a5, ed_a6, ed_a7,
        ed_a8, ed_a9, ed_aa, ed_ab, ed_ac, ed_ad, ed_ae, ed_af,
        ed_b0, ed_b1, ed_b2, ed_b3, ed_b4, ed_b5, ed_b6, ed_b7,
        ed_b8, ed_b9, ed_ba, ed_bb, ed_bc, ed_bd, ed_be, ed_bf,
        ed_c0, ed_c1, ed_c2, ed_c3, ed_c4, ed_c5, ed_c6, ed_c7,
        ed_c8, ed_c9, ed_ca, ed_cb, ed_cc, ed_cd, ed_ce, ed_cf,
        ed_d0, ed_d1, ed_d2, ed_d3, ed_d4, ed_d5, ed_d6, ed_d7,
        ed_d8, ed_d9, ed_da, ed_db, ed_dc, ed_dd, ed_de, ed_df,
        ed_e0, ed_e1, ed_e2, ed_e3, ed_e4, ed_e5, ed_e6, ed_e7,
        ed_e8, ed_e9, ed_ea, ed_eb, ed_ec, ed_ed, ed_ee, ed_ef,
        ed_f0, ed_f1, ed_f2, ed_f3, ed_f4, ed_f5, ed_f6, ed_f7,
        ed_f8, ed_f9, ed_fa, ed_fb, ed_fc, ed_fd, ed_fe, ed_ff
    };
    opcode[] Z80fd = {
        fd_00, fd_01, fd_02, fd_03, fd_04, fd_05, fd_06, fd_07,
        fd_08, fd_09, fd_0a, fd_0b, fd_0c, fd_0d, fd_0e, fd_0f,
        fd_10, fd_11, fd_12, fd_13, fd_14, fd_15, fd_16, fd_17,
        fd_18, fd_19, fd_1a, fd_1b, fd_1c, fd_1d, fd_1e, fd_1f,
        fd_20, fd_21, fd_22, fd_23, fd_24, fd_25, fd_26, fd_27,
        fd_28, fd_29, fd_2a, fd_2b, fd_2c, fd_2d, fd_2e, fd_2f,
        fd_30, fd_31, fd_32, fd_33, fd_34, fd_35, fd_36, fd_37,
        fd_38, fd_39, fd_3a, fd_3b, fd_3c, fd_3d, fd_3e, fd_3f,
        fd_40, fd_41, fd_42, fd_43, fd_44, fd_45, fd_46, fd_47,
        fd_48, fd_49, fd_4a, fd_4b, fd_4c, fd_4d, fd_4e, fd_4f,
        fd_50, fd_51, fd_52, fd_53, fd_54, fd_55, fd_56, fd_57,
        fd_58, fd_59, fd_5a, fd_5b, fd_5c, fd_5d, fd_5e, fd_5f,
        fd_60, fd_61, fd_62, fd_63, fd_64, fd_65, fd_66, fd_67,
        fd_68, fd_69, fd_6a, fd_6b, fd_6c, fd_6d, fd_6e, fd_6f,
        fd_70, fd_71, fd_72, fd_73, fd_74, fd_75, fd_76, fd_77,
        fd_78, fd_79, fd_7a, fd_7b, fd_7c, fd_7d, fd_7e, fd_7f,
        fd_80, fd_81, fd_82, fd_83, fd_84, fd_85, fd_86, fd_87,
        fd_88, fd_89, fd_8a, fd_8b, fd_8c, fd_8d, fd_8e, fd_8f,
        fd_90, fd_91, fd_92, fd_93, fd_94, fd_95, fd_96, fd_97,
        fd_98, fd_99, fd_9a, fd_9b, fd_9c, fd_9d, fd_9e, fd_9f,
        fd_a0, fd_a1, fd_a2, fd_a3, fd_a4, fd_a5, fd_a6, fd_a7,
        fd_a8, fd_a9, fd_aa, fd_ab, fd_ac, fd_ad, fd_ae, fd_af,
        fd_b0, fd_b1, fd_b2, fd_b3, fd_b4, fd_b5, fd_b6, fd_b7,
        fd_b8, fd_b9, fd_ba, fd_bb, fd_bc, fd_bd, fd_be, fd_bf,
        fd_c0, fd_c1, fd_c2, fd_c3, fd_c4, fd_c5, fd_c6, fd_c7,
        fd_c8, fd_c9, fd_ca, fd_cb, fd_cc, fd_cd, fd_ce, fd_cf,
        fd_d0, fd_d1, fd_d2, fd_d3, fd_d4, fd_d5, fd_d6, fd_d7,
        fd_d8, fd_d9, fd_da, fd_db, fd_dc, fd_dd, fd_de, fd_df,
        fd_e0, fd_e1, fd_e2, fd_e3, fd_e4, fd_e5, fd_e6, fd_e7,
        fd_e8, fd_e9, fd_ea, fd_eb, fd_ec, fd_ed, fd_ee, fd_ef,
        fd_f0, fd_f1, fd_f2, fd_f3, fd_f4, fd_f5, fd_f6, fd_f7,
        fd_f8, fd_f9, fd_fa, fd_fb, fd_fc, fd_fd, fd_fe, fd_ff
    };
    opcode[] Z80xycb = {
        xycb_00, xycb_01, xycb_02, xycb_03, xycb_04, xycb_05, xycb_06, xycb_07,
        xycb_08, xycb_09, xycb_0a, xycb_0b, xycb_0c, xycb_0d, xycb_0e, xycb_0f,
        xycb_10, xycb_11, xycb_12, xycb_13, xycb_14, xycb_15, xycb_16, xycb_17,
        xycb_18, xycb_19, xycb_1a, xycb_1b, xycb_1c, xycb_1d, xycb_1e, xycb_1f,
        xycb_20, xycb_21, xycb_22, xycb_23, xycb_24, xycb_25, xycb_26, xycb_27,
        xycb_28, xycb_29, xycb_2a, xycb_2b, xycb_2c, xycb_2d, xycb_2e, xycb_2f,
        xycb_30, xycb_31, xycb_32, xycb_33, xycb_34, xycb_35, xycb_36, xycb_37,
        xycb_38, xycb_39, xycb_3a, xycb_3b, xycb_3c, xycb_3d, xycb_3e, xycb_3f,
        xycb_40, xycb_41, xycb_42, xycb_43, xycb_44, xycb_45, xycb_46, xycb_47,
        xycb_48, xycb_49, xycb_4a, xycb_4b, xycb_4c, xycb_4d, xycb_4e, xycb_4f,
        xycb_50, xycb_51, xycb_52, xycb_53, xycb_54, xycb_55, xycb_56, xycb_57,
        xycb_58, xycb_59, xycb_5a, xycb_5b, xycb_5c, xycb_5d, xycb_5e, xycb_5f,
        xycb_60, xycb_61, xycb_62, xycb_63, xycb_64, xycb_65, xycb_66, xycb_67,
        xycb_68, xycb_69, xycb_6a, xycb_6b, xycb_6c, xycb_6d, xycb_6e, xycb_6f,
        xycb_70, xycb_71, xycb_72, xycb_73, xycb_74, xycb_75, xycb_76, xycb_77,
        xycb_78, xycb_79, xycb_7a, xycb_7b, xycb_7c, xycb_7d, xycb_7e, xycb_7f,
        xycb_80, xycb_81, xycb_82, xycb_83, xycb_84, xycb_85, xycb_86, xycb_87,
        xycb_88, xycb_89, xycb_8a, xycb_8b, xycb_8c, xycb_8d, xycb_8e, xycb_8f,
        xycb_90, xycb_91, xycb_92, xycb_93, xycb_94, xycb_95, xycb_96, xycb_97,
        xycb_98, xycb_99, xycb_9a, xycb_9b, xycb_9c, xycb_9d, xycb_9e, xycb_9f,
        xycb_a0, xycb_a1, xycb_a2, xycb_a3, xycb_a4, xycb_a5, xycb_a6, xycb_a7,
        xycb_a8, xycb_a9, xycb_aa, xycb_ab, xycb_ac, xycb_ad, xycb_ae, xycb_af,
        xycb_b0, xycb_b1, xycb_b2, xycb_b3, xycb_b4, xycb_b5, xycb_b6, xycb_b7,
        xycb_b8, xycb_b9, xycb_ba, xycb_bb, xycb_bc, xycb_bd, xycb_be, xycb_bf,
        xycb_c0, xycb_c1, xycb_c2, xycb_c3, xycb_c4, xycb_c5, xycb_c6, xycb_c7,
        xycb_c8, xycb_c9, xycb_ca, xycb_cb, xycb_cc, xycb_cd, xycb_ce, xycb_cf,
        xycb_d0, xycb_d1, xycb_d2, xycb_d3, xycb_d4, xycb_d5, xycb_d6, xycb_d7,
        xycb_d8, xycb_d9, xycb_da, xycb_db, xycb_dc, xycb_dd, xycb_de, xycb_df,
        xycb_e0, xycb_e1, xycb_e2, xycb_e3, xycb_e4, xycb_e5, xycb_e6, xycb_e7,
        xycb_e8, xycb_e9, xycb_ea, xycb_eb, xycb_ec, xycb_ed, xycb_ee, xycb_ef,
        xycb_f0, xycb_f1, xycb_f2, xycb_f3, xycb_f4, xycb_f5, xycb_f6, xycb_f7,
        xycb_f8, xycb_f9, xycb_fa, xycb_fb, xycb_fc, xycb_fd, xycb_fe, xycb_ff
    };

}
