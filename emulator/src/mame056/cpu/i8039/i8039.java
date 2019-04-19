/**
 * ported to v0.56
 */
package mame056.cpu.i8039;

import static mame056.cpu.i8039.i8039H.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static common.libc.cstdio.*;
import static arcadeflex036.osdepend.*;

public class i8039 extends cpu_interface {

    public static int[] i8039_ICount = new int[1];

    public i8039() {
        cpu_num = CPU_I8039;
        num_irqs = 1;
        default_vector = 0;
        icount = i8039_ICount;
        overclock = 1.00;
        irq_int = 0;
        databus_width = 8;
        pgm_memory_base = 0;
        address_shift = 0;
        address_bits = 16;
        endianess = CPU_IS_LE;
        align_unit = 1;
        max_inst_len = 2;
    }

    /* HJB 01/05/99 changed to positive values to use pending_irq as a flag */
    public static final int I8039_IGNORE_INT = 0;/* Ignore interrupt                     */
    public static final int I8039_EXT_INT = 1;/* Execute a normal extern interrupt	*/
    public static final int I8039_TIMER_INT = 2;/* Execute a Timer interrupt			*/
    public static final int I8039_COUNT_INT = 4;/* Execute a Counter interrupt			*/

    public static char M_RDMEM(int A) {
        return I8039_RDMEM(A);
    }

    public static char M_RDOP(int A) {
        return I8039_RDOP(A);
    }

    public static char M_RDOP_ARG(int A) {
        return I8039_RDOP_ARG(A);
    }

    public static char M_IN(int A) {
        return I8039_In(A);
    }

    public static void M_OUT(int A, int V) {
        I8039_Out(A, V);
    }

    public static char port_r(int A) {
        return I8039_In(I8039_p0 + A);
    }

    public static void port_w(int A, int V) {
        I8039_Out(I8039_p0 + A, V);
    }

    public static char test_r(int A) {
        return I8039_In(I8039_t0 + A);
    }

    public static void test_w(int A, int V) {
        I8039_Out(I8039_t0 + A, V);
    }

    public static char bus_r() {
        return I8039_In(I8039_bus);
    }

    public static void bus_w(int V) {
        I8039_Out(I8039_bus, V);
    }
    public static final int C_FLAG = 0x80;
    public static final int A_FLAG = 0x40;
    public static final int F_FLAG = 0x20;
    public static final int B_FLAG = 0x10;

    public static class I8039_Regs {

        public int PREPC;/* previous program counter */
        public int PC;/* program counter */
        public char u8_A, u8_SP, u8_PSW;
        public char[] u8_RAM = new char[128];
        public char u8_bus, u8_f1;/* Bus data, and flag1 */
        public char u8_P1, u8_P2;/* Internal Port 1 and 2 latched outputs */

        public int pending_irq, irq_executing, masterClock, regPtr;
        public char u8_t_flag, u8_timer, u8_timerON, u8_countON, u8_xirq_en, u8_tirq_en;
        public char A11, A11ff;
        public int irq_state;
        public irqcallbacksPtr irq_callback;
    }

    public static I8039_Regs R = new I8039_Regs();
    public static char u8_Old_T1;

    /* The opcode table now is a combination of cycle counts and function pointers */
    public abstract interface opcode {

        public abstract void handler();
    }

    static class s_opcode {

        public /*uint*/ int cycles;
        public opcode function;

        public s_opcode(int cycles, opcode function) {
            this.cycles = cycles;
            this.function = function;
        }
    }

    public static int POSITIVE_EDGE_T1(int T1) {
        return (((T1 - u8_Old_T1) > 0) ? 1 : 0);
    }

    public static int NEGATIVE_EDGE_T1(int T1) {
        return (((u8_Old_T1 - T1) > 0) ? 1 : 0);
    }

    public static char M_Cy() {
        return (char) ((((R.u8_PSW & C_FLAG) >>> 7)) & 0xFF);
    }

    public static boolean M_Cn() {
        return (M_Cy() == 0);
    }

    public static boolean M_Ay() {
        return ((R.u8_PSW & A_FLAG)) != 0;
    }

    public static boolean M_An() {
        return (!M_Ay());
    }

    public static boolean M_F0y() {
        return ((R.u8_PSW & F_FLAG)) != 0;
    }

    public static boolean M_F0n() {
        return (!M_F0y());
    }

    public static boolean M_By() {
        return ((R.u8_PSW & B_FLAG)) != 0;
    }

    public static boolean M_Bn() {
        return (!M_By());
    }

    public static void CLR(/*UINT8*/int flag) {
        R.u8_PSW = (char) ((R.u8_PSW & ~flag) & 0xFF);
    }

    public static void SET(/*UINT8*/int flag) {
        R.u8_PSW = (char) ((R.u8_PSW | flag) & 0xFF);
    }

    /*TODO*///
/*TODO*///
/*TODO*////* Get next opcode argument and increment program counter */
/*TODO*///INLINE unsigned M_RDMEM_OPCODE (void)
/*TODO*///{
/*TODO*///		unsigned retval;
/*TODO*///		retval=M_RDOP_ARG(R.PC.w.l);
/*TODO*///		R.PC.w.l++;
/*TODO*///		return retval;
/*TODO*///}
/*TODO*///
/*TODO*///INLINE void push(UINT8 d)
/*TODO*///{
/*TODO*///	intRAM[8+R.SP++] = d;
/*TODO*///	R.SP  = R.SP & 0x0f;
/*TODO*///	R.PSW = R.PSW & 0xf8;
/*TODO*///	R.PSW = R.PSW | (R.SP >> 1);
/*TODO*///}
/*TODO*///
/*TODO*///INLINE UINT8 pull(void) {
/*TODO*///	R.SP  = (R.SP + 15) & 0x0f;		/*  if (--R.SP < 0) R.SP = 15;  */
/*TODO*///	R.PSW = R.PSW & 0xf8;
/*TODO*///	R.PSW = R.PSW | (R.SP >> 1);
/*TODO*///	/* regPTR = ((M_By) ? 24 : 0);  regPTR should not change */
/*TODO*///	return intRAM[8+R.SP];
/*TODO*///}
/*TODO*///
/*TODO*///INLINE void daa_a(void)
/*TODO*///{
/*TODO*///	if ((R.A & 0x0f) > 0x09 || (R.PSW & A_FLAG))
/*TODO*///		R.A += 0x06;
/*TODO*///	if ((R.A & 0xf0) > 0x90 || (R.PSW & C_FLAG))
/*TODO*///	{
/*TODO*///		R.A += 0x60;
/*TODO*///		SET(C_FLAG);
/*TODO*///	} else CLR(C_FLAG);
/*TODO*///}
/*TODO*///
/*TODO*///INLINE void M_ADD(UINT8 dat)
/*TODO*///{
/*TODO*///	UINT16 temp;
/*TODO*///
/*TODO*///	CLR(C_FLAG | A_FLAG);
/*TODO*///	if ((R.A & 0xf) + (dat & 0xf) > 0xf) SET(A_FLAG);
/*TODO*///	temp = R.A + dat;
/*TODO*///	if (temp > 0xff) SET(C_FLAG);
/*TODO*///	R.A  = temp & 0xff;
/*TODO*///}
/*TODO*///
/*TODO*///INLINE void M_ADDC(UINT8 dat)
/*TODO*///{
/*TODO*///	UINT16 temp;
/*TODO*///
/*TODO*///	CLR(A_FLAG);
/*TODO*///	if ((R.A & 0xf) + (dat & 0xf) + M_Cy > 0xf) SET(A_FLAG);
/*TODO*///	temp = R.A + dat + M_Cy;
/*TODO*///	CLR(C_FLAG);
/*TODO*///	if (temp > 0xff) SET(C_FLAG);
/*TODO*///	R.A  = temp & 0xff;
/*TODO*///}
/*TODO*///
/*TODO*///INLINE void M_CALL(UINT16 addr)
/*TODO*///{
/*TODO*///	push(R.PC.b.l);
/*TODO*///	push((R.PC.b.h & 0x0f) | (R.PSW & 0xf0));
/*TODO*///	R.PC.w.l = addr;
/*TODO*///	#ifdef MESS
/*TODO*///		change_pc16(addr);
/*TODO*///	#endif
/*TODO*///
/*TODO*///}
/*TODO*///
/*TODO*///INLINE void M_XCHD(UINT8 addr)
/*TODO*///{
/*TODO*///	UINT8 dat = R.A & 0x0f;
/*TODO*///	R.A &= 0xf0;
/*TODO*///	R.A |= intRAM[addr] & 0x0f;
/*TODO*///	intRAM[addr] &= 0xf0;
/*TODO*///	intRAM[addr] |= dat;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///INLINE void M_ILLEGAL(void)
/*TODO*///{
/*TODO*///	logerror("I8039:  PC = %04x,  Illegal opcode = %02x\n", R.PC.w.l-1, M_RDMEM(R.PC.w.l-1));
/*TODO*///}
/*TODO*///
/*TODO*///INLINE void M_UNDEFINED(void)
/*TODO*///{
/*TODO*///	logerror("I8039:  PC = %04x,  Unimplemented opcode = %02x\n", R.PC.w.l-1, M_RDMEM(R.PC.w.l-1));
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///static void illegal(void)    { M_ILLEGAL(); }
/*TODO*///
/*TODO*///static void add_a_n(void)    { M_ADD(M_RDMEM_OPCODE()); }
/*TODO*///static void add_a_r0(void)   { M_ADD(R0); }
/*TODO*///static void add_a_r1(void)   { M_ADD(R1); }
/*TODO*///static void add_a_r2(void)   { M_ADD(R2); }
/*TODO*///static void add_a_r3(void)   { M_ADD(R3); }
/*TODO*///static void add_a_r4(void)   { M_ADD(R4); }
/*TODO*///static void add_a_r5(void)   { M_ADD(R5); }
/*TODO*///static void add_a_r6(void)   { M_ADD(R6); }
/*TODO*///static void add_a_r7(void)   { M_ADD(R7); }
/*TODO*///static void add_a_xr0(void)  { M_ADD(intRAM[R0 & 0x7f]); }
/*TODO*///static void add_a_xr1(void)  { M_ADD(intRAM[R1 & 0x7f]); }
/*TODO*///static void adc_a_n(void)    { M_ADDC(M_RDMEM_OPCODE()); }
/*TODO*///static void adc_a_r0(void)   { M_ADDC(R0); }
/*TODO*///static void adc_a_r1(void)   { M_ADDC(R1); }
/*TODO*///static void adc_a_r2(void)   { M_ADDC(R2); }
/*TODO*///static void adc_a_r3(void)   { M_ADDC(R3); }
/*TODO*///static void adc_a_r4(void)   { M_ADDC(R4); }
/*TODO*///static void adc_a_r5(void)   { M_ADDC(R5); }
/*TODO*///static void adc_a_r6(void)   { M_ADDC(R6); }
/*TODO*///static void adc_a_r7(void)   { M_ADDC(R7); }
/*TODO*///static void adc_a_xr0(void)  { M_ADDC(intRAM[R0 & 0x7f]); }
/*TODO*///static void adc_a_xr1(void)  { M_ADDC(intRAM[R1 & 0x7f]); }
/*TODO*///static void anl_a_n(void)    { R.A &= M_RDMEM_OPCODE(); }
/*TODO*///static void anl_a_r0(void)   { R.A &= R0; }
/*TODO*///static void anl_a_r1(void)   { R.A &= R1; }
/*TODO*///static void anl_a_r2(void)   { R.A &= R2; }
/*TODO*///static void anl_a_r3(void)   { R.A &= R3; }
/*TODO*///static void anl_a_r4(void)   { R.A &= R4; }
/*TODO*///static void anl_a_r5(void)   { R.A &= R5; }
/*TODO*///static void anl_a_r6(void)   { R.A &= R6; }
/*TODO*///static void anl_a_r7(void)   { R.A &= R7; }
/*TODO*///static void anl_a_xr0(void)  { R.A &= intRAM[R0 & 0x7f]; }
/*TODO*///static void anl_a_xr1(void)  { R.A &= intRAM[R1 & 0x7f]; }
/*TODO*///static void anl_bus_n(void)  { bus_w( bus_r() & M_RDMEM_OPCODE() ); }
/*TODO*///static void anl_p1_n(void)   { R.P1 &= M_RDMEM_OPCODE(); port_w( 1, R.P1); }
/*TODO*///static void anl_p2_n(void)   { R.P2 &= M_RDMEM_OPCODE(); port_w( 2, R.P2 ); }
/*TODO*///static void anld_p4_a(void)  { port_w( 4, port_r(4) & M_RDMEM_OPCODE() ); }
/*TODO*///static void anld_p5_a(void)  { port_w( 5, port_r(5) & M_RDMEM_OPCODE() ); }
/*TODO*///static void anld_p6_a(void)  { port_w( 6, port_r(6) & M_RDMEM_OPCODE() ); }
/*TODO*///static void anld_p7_a(void)  { port_w( 7, port_r(7) & M_RDMEM_OPCODE() ); }
/*TODO*///static void call(void)		 { UINT8 i=M_RDMEM_OPCODE(); M_CALL(i | R.A11); }
/*TODO*///static void call_1(void)	 { UINT8 i=M_RDMEM_OPCODE(); M_CALL(i | 0x100 | R.A11); }
/*TODO*///static void call_2(void)	 { UINT8 i=M_RDMEM_OPCODE(); M_CALL(i | 0x200 | R.A11); }
/*TODO*///static void call_3(void)	 { UINT8 i=M_RDMEM_OPCODE(); M_CALL(i | 0x300 | R.A11); }
/*TODO*///static void call_4(void)	 { UINT8 i=M_RDMEM_OPCODE(); M_CALL(i | 0x400 | R.A11); }
/*TODO*///static void call_5(void)	 { UINT8 i=M_RDMEM_OPCODE(); M_CALL(i | 0x500 | R.A11); }
/*TODO*///static void call_6(void)	 { UINT8 i=M_RDMEM_OPCODE(); M_CALL(i | 0x600 | R.A11); }
/*TODO*///static void call_7(void)	 { UINT8 i=M_RDMEM_OPCODE(); M_CALL(i | 0x700 | R.A11); }
/*TODO*///static void clr_a(void)      { R.A=0; }
/*TODO*///static void clr_c(void)      { CLR(C_FLAG); }
/*TODO*///static void clr_f0(void)     { CLR(F_FLAG); }
/*TODO*///static void clr_f1(void)     { R.f1 = 0; }
/*TODO*///static void cpl_a(void)      { R.A ^= 0xff; }
/*TODO*///static void cpl_c(void)      { R.PSW ^= C_FLAG; }
/*TODO*///static void cpl_f0(void)     { R.PSW ^= F_FLAG; }
/*TODO*///static void cpl_f1(void)     { R.f1 ^= 1; }
/*TODO*///static void dec_a(void)      { R.A--; }
/*TODO*///static void dec_r0(void)     { R0--; }
/*TODO*///static void dec_r1(void)     { R1--; }
/*TODO*///static void dec_r2(void)     { R2--; }
/*TODO*///static void dec_r3(void)     { R3--; }
/*TODO*///static void dec_r4(void)     { R4--; }
/*TODO*///static void dec_r5(void)     { R5--; }
/*TODO*///static void dec_r6(void)     { R6--; }
/*TODO*///static void dec_r7(void)     { R7--; }
/*TODO*///static void dis_i(void)      { R.xirq_en = 0; }
/*TODO*///static void dis_tcnti(void)  { R.tirq_en = 0; }
/*TODO*///#ifdef MESS
/*TODO*///	static void djnz_r0(void)	 { UINT8 i=M_RDMEM_OPCODE(); R0--; if (R0 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); }}
/*TODO*///	static void djnz_r1(void)	 { UINT8 i=M_RDMEM_OPCODE(); R1--; if (R1 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void djnz_r2(void)	 { UINT8 i=M_RDMEM_OPCODE(); R2--; if (R2 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void djnz_r3(void)	 { UINT8 i=M_RDMEM_OPCODE(); R3--; if (R3 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void djnz_r4(void)	 { UINT8 i=M_RDMEM_OPCODE(); R4--; if (R4 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void djnz_r5(void)	 { UINT8 i=M_RDMEM_OPCODE(); R5--; if (R5 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void djnz_r6(void)	 { UINT8 i=M_RDMEM_OPCODE(); R6--; if (R6 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void djnz_r7(void)	 { UINT8 i=M_RDMEM_OPCODE(); R7--; if (R7 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///#else
/*TODO*///	static void djnz_r0(void)	 { UINT8 i=M_RDMEM_OPCODE(); R0--; if (R0 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  }}
/*TODO*///	static void djnz_r1(void)	 { UINT8 i=M_RDMEM_OPCODE(); R1--; if (R1 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void djnz_r2(void)	 { UINT8 i=M_RDMEM_OPCODE(); R2--; if (R2 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void djnz_r3(void)	 { UINT8 i=M_RDMEM_OPCODE(); R3--; if (R3 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void djnz_r4(void)	 { UINT8 i=M_RDMEM_OPCODE(); R4--; if (R4 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void djnz_r5(void)	 { UINT8 i=M_RDMEM_OPCODE(); R5--; if (R5 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void djnz_r6(void)	 { UINT8 i=M_RDMEM_OPCODE(); R6--; if (R6 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void djnz_r7(void)	 { UINT8 i=M_RDMEM_OPCODE(); R7--; if (R7 != 0) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///#endif
/*TODO*///static void en_i(void)       { R.xirq_en = 1; if (R.irq_state != CLEAR_LINE) R.pending_irq |= I8039_EXT_INT; }
/*TODO*///static void en_tcnti(void)   { R.tirq_en = 1; }
/*TODO*///static void ento_clk(void)   { M_UNDEFINED(); }
/*TODO*///static void in_a_p1(void)    { R.A = port_r(1) & R.P1; }
/*TODO*///static void in_a_p2(void)    { R.A = port_r(2) & R.P2; }
/*TODO*///static void ins_a_bus(void)  { R.A = bus_r(); }
/*TODO*///static void inc_a(void)      { R.A++; }
/*TODO*///static void inc_r0(void)     { R0++; }
/*TODO*///static void inc_r1(void)     { R1++; }
/*TODO*///static void inc_r2(void)     { R2++; }
/*TODO*///static void inc_r3(void)     { R3++; }
/*TODO*///static void inc_r4(void)     { R4++; }
/*TODO*///static void inc_r5(void)     { R5++; }
/*TODO*///static void inc_r6(void)     { R6++; }
/*TODO*///static void inc_r7(void)     { R7++; }
/*TODO*///static void inc_xr0(void)    { intRAM[R0 & 0x7f]++; }
/*TODO*///static void inc_xr1(void)    { intRAM[R1 & 0x7f]++; }
/*TODO*///
/*TODO*////* static void jmp(void)		{ UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | R.A11; }
/*TODO*/// */
/*TODO*///
/*TODO*///static void jmp(void)
/*TODO*///{
/*TODO*///	UINT8 i=M_RDOP(R.PC.w.l);
/*TODO*///	UINT16 oldpc,newpc;
/*TODO*///
/*TODO*///	oldpc = R.PC.w.l-1;
/*TODO*///	R.PC.w.l = i | R.A11;
/*TODO*///#ifdef MESS
/*TODO*///	change_pc16(R.PC.w.l);
/*TODO*///#endif
/*TODO*///	newpc = R.PC.w.l;
/*TODO*///	if (newpc == oldpc) { if (i8039_ICount > 0) i8039_ICount = 0; } /* speed up busy loop */
/*TODO*///	else if (newpc == oldpc-1 && M_RDOP(newpc) == 0x00)	/* NOP - Gyruss */
/*TODO*///		{ if (i8039_ICount > 0) i8039_ICount = 0; }
/*TODO*///}
/*TODO*///
/*TODO*///#ifdef MESS
/*TODO*///	static void jmp_1(void) 	 { UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | 0x100 | R.A11; change_pc16(R.PC.w.l); }
/*TODO*///	static void jmp_2(void) 	 { UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | 0x200 | R.A11; change_pc16(R.PC.w.l); }
/*TODO*///	static void jmp_3(void) 	 { UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | 0x300 | R.A11; change_pc16(R.PC.w.l); }
/*TODO*///	static void jmp_4(void) 	 { UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | 0x400 | R.A11; change_pc16(R.PC.w.l); }
/*TODO*///	static void jmp_5(void) 	 { UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | 0x500 | R.A11; change_pc16(R.PC.w.l); }
/*TODO*///	static void jmp_6(void) 	 { UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | 0x600 | R.A11; change_pc16(R.PC.w.l); }
/*TODO*///	static void jmp_7(void) 	 { UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | 0x700 | R.A11; change_pc16(R.PC.w.l); }
/*TODO*///	static void jmpp_xa(void)	 { UINT16 addr = (R.PC.w.l & 0xf00) | R.A; R.PC.w.l = (R.PC.w.l & 0xf00) | M_RDMEM(addr); change_pc16(R.PC.w.l); }
/*TODO*///	static void jb_0(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x01) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jb_1(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x02) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jb_2(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x04) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jb_3(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x08) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jb_4(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x10) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jb_5(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x20) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jb_6(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x40) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jb_7(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x80) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jf0(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (M_F0y) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jf_1(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.f1)	{ R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jnc(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (M_Cn)	{ R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jc(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (M_Cy)	{ R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jni(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.irq_state != CLEAR_LINE) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jnt_0(void) 	 { UINT8 i=M_RDMEM_OPCODE(); if (!test_r(0)) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jt_0(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (test_r(0))  { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jnt_1(void) 	 { UINT8 i=M_RDMEM_OPCODE(); if (!test_r(1)) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jt_1(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (test_r(1))  { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jnz(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A != 0)	 { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jz(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A == 0)	 { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); } }
/*TODO*///	static void jtf(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.t_flag)	 { R.PC.w.l = (R.PC.w.l & 0xf00) | i; change_pc16(R.PC.w.l); R.t_flag = 0; } }
/*TODO*///#else
/*TODO*///	static void jmp_1(void) 	 { UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | 0x100 | R.A11;  }
/*TODO*///	static void jmp_2(void) 	 { UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | 0x200 | R.A11;  }
/*TODO*///	static void jmp_3(void) 	 { UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | 0x300 | R.A11;  }
/*TODO*///	static void jmp_4(void) 	 { UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | 0x400 | R.A11;  }
/*TODO*///	static void jmp_5(void) 	 { UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | 0x500 | R.A11;  }
/*TODO*///	static void jmp_6(void) 	 { UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | 0x600 | R.A11;  }
/*TODO*///	static void jmp_7(void) 	 { UINT8 i=M_RDOP(R.PC.w.l); R.PC.w.l = i | 0x700 | R.A11;  }
/*TODO*///	static void jmpp_xa(void)	 { UINT16 addr = (R.PC.w.l & 0xf00) | R.A; R.PC.w.l = (R.PC.w.l & 0xf00) | M_RDMEM(addr);  }
/*TODO*///	static void jb_0(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x01) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void jb_1(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x02) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void jb_2(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x04) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void jb_3(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x08) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void jb_4(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x10) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void jb_5(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x20) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void jb_6(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x40) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void jb_7(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A & 0x80) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void jf0(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (M_F0y) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; } }
/*TODO*///	static void jf_1(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.f1)	{ R.PC.w.l = (R.PC.w.l & 0xf00) | i; } }
/*TODO*///	static void jnc(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (M_Cn)	{ R.PC.w.l = (R.PC.w.l & 0xf00) | i; } }
/*TODO*///	static void jc(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (M_Cy)	{ R.PC.w.l = (R.PC.w.l & 0xf00) | i; } }
/*TODO*///	static void jni(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.irq_state != CLEAR_LINE) { R.PC.w.l = (R.PC.w.l & 0xf00) | i; } }
/*TODO*///	static void jnt_0(void) 	 { UINT8 i=M_RDMEM_OPCODE(); if (!test_r(0)) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void jt_0(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (test_r(0))  { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void jnt_1(void) 	 { UINT8 i=M_RDMEM_OPCODE(); if (!test_r(1)) { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void jt_1(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (test_r(1))  { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void jnz(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A != 0)	 { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void jz(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.A == 0)	 { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  } }
/*TODO*///	static void jtf(void)		 { UINT8 i=M_RDMEM_OPCODE(); if (R.t_flag)	 { R.PC.w.l = (R.PC.w.l & 0xf00) | i;  R.t_flag = 0; } }
/*TODO*///#endif
/*TODO*///
/*TODO*///static void mov_a_n(void)    { R.A = M_RDMEM_OPCODE(); }
/*TODO*///static void mov_a_r0(void)   { R.A = R0; }
/*TODO*///static void mov_a_r1(void)   { R.A = R1; }
/*TODO*///static void mov_a_r2(void)   { R.A = R2; }
/*TODO*///static void mov_a_r3(void)   { R.A = R3; }
/*TODO*///static void mov_a_r4(void)   { R.A = R4; }
/*TODO*///static void mov_a_r5(void)   { R.A = R5; }
/*TODO*///static void mov_a_r6(void)   { R.A = R6; }
/*TODO*///static void mov_a_r7(void)   { R.A = R7; }
/*TODO*///static void mov_a_psw(void)  { R.A = R.PSW; }
/*TODO*///static void mov_a_xr0(void)  { R.A = intRAM[R0 & 0x7f]; }
/*TODO*///static void mov_a_xr1(void)  { R.A = intRAM[R1 & 0x7f]; }
/*TODO*///static void mov_r0_a(void)   { R0 = R.A; }
/*TODO*///static void mov_r1_a(void)   { R1 = R.A; }
/*TODO*///static void mov_r2_a(void)   { R2 = R.A; }
/*TODO*///static void mov_r3_a(void)   { R3 = R.A; }
/*TODO*///static void mov_r4_a(void)   { R4 = R.A; }
/*TODO*///static void mov_r5_a(void)   { R5 = R.A; }
/*TODO*///static void mov_r6_a(void)   { R6 = R.A; }
/*TODO*///static void mov_r7_a(void)   { R7 = R.A; }
/*TODO*///static void mov_psw_a(void)  { R.PSW = R.A; regPTR = ((M_By) ? 24 : 0); R.SP = (R.PSW & 7) << 1; }
/*TODO*///static void mov_r0_n(void)   { R0 = M_RDMEM_OPCODE(); }
/*TODO*///static void mov_r1_n(void)   { R1 = M_RDMEM_OPCODE(); }
/*TODO*///static void mov_r2_n(void)   { R2 = M_RDMEM_OPCODE(); }
/*TODO*///static void mov_r3_n(void)   { R3 = M_RDMEM_OPCODE(); }
/*TODO*///static void mov_r4_n(void)   { R4 = M_RDMEM_OPCODE(); }
/*TODO*///static void mov_r5_n(void)   { R5 = M_RDMEM_OPCODE(); }
/*TODO*///static void mov_r6_n(void)   { R6 = M_RDMEM_OPCODE(); }
/*TODO*///static void mov_r7_n(void)   { R7 = M_RDMEM_OPCODE(); }
/*TODO*///static void mov_a_t(void)    { R.A = R.timer; }
/*TODO*///static void mov_t_a(void)    { R.timer = R.A; }
/*TODO*///static void mov_xr0_a(void)  { intRAM[R0 & 0x7f] = R.A; }
/*TODO*///static void mov_xr1_a(void)  { intRAM[R1 & 0x7f] = R.A; }
/*TODO*///static void mov_xr0_n(void)  { intRAM[R0 & 0x7f] = M_RDMEM_OPCODE(); }
/*TODO*///static void mov_xr1_n(void)  { intRAM[R1 & 0x7f] = M_RDMEM_OPCODE(); }
/*TODO*///static void movd_a_p4(void)  { R.A = port_r(4); }
/*TODO*///static void movd_a_p5(void)  { R.A = port_r(5); }
/*TODO*///static void movd_a_p6(void)  { R.A = port_r(6); }
/*TODO*///static void movd_a_p7(void)  { R.A = port_r(7); }
/*TODO*///static void movd_p4_a(void)  { port_w(4, R.A); }
/*TODO*///static void movd_p5_a(void)  { port_w(5, R.A); }
/*TODO*///static void movd_p6_a(void)  { port_w(6, R.A); }
/*TODO*///static void movd_p7_a(void)  { port_w(7, R.A); }
/*TODO*///static void movp_a_xa(void)  { R.A = M_RDMEM((R.PC.w.l & 0x0f00) | R.A); }
/*TODO*///static void movp3_a_xa(void) { R.A = M_RDMEM(0x300 | R.A); }
/*TODO*///static void movx_a_xr0(void) { R.A = M_IN(R0); }
/*TODO*///static void movx_a_xr1(void) { R.A = M_IN(R1); }
/*TODO*///static void movx_xr0_a(void) { M_OUT(R0, R.A); }
/*TODO*///static void movx_xr1_a(void) { M_OUT(R1, R.A); }
/*TODO*///static void nop(void) { }
/*TODO*///static void orl_a_n(void)    { R.A |= M_RDMEM_OPCODE(); }
/*TODO*///static void orl_a_r0(void)   { R.A |= R0; }
/*TODO*///static void orl_a_r1(void)   { R.A |= R1; }
/*TODO*///static void orl_a_r2(void)   { R.A |= R2; }
/*TODO*///static void orl_a_r3(void)   { R.A |= R3; }
/*TODO*///static void orl_a_r4(void)   { R.A |= R4; }
/*TODO*///static void orl_a_r5(void)   { R.A |= R5; }
/*TODO*///static void orl_a_r6(void)   { R.A |= R6; }
/*TODO*///static void orl_a_r7(void)   { R.A |= R7; }
/*TODO*///static void orl_a_xr0(void)  { R.A |= intRAM[R0 & 0x7f]; }
/*TODO*///static void orl_a_xr1(void)  { R.A |= intRAM[R1 & 0x7f]; }
/*TODO*///static void orl_bus_n(void)  { bus_w( bus_r() | M_RDMEM_OPCODE() ); }
/*TODO*///static void orl_p1_n(void)   { R.P1 |= M_RDMEM_OPCODE(); port_w(1, R.P1); }
/*TODO*///static void orl_p2_n(void)   { R.P2 |= M_RDMEM_OPCODE(); port_w(2, R.P2); }
/*TODO*///static void orld_p4_a(void)  { port_w(4, port_r(4) | R.A ); }
/*TODO*///static void orld_p5_a(void)  { port_w(5, port_r(5) | R.A ); }
/*TODO*///static void orld_p6_a(void)  { port_w(6, port_r(6) | R.A ); }
/*TODO*///static void orld_p7_a(void)  { port_w(7, port_r(7) | R.A ); }
/*TODO*///static void outl_bus_a(void) { bus_w(R.A); }
/*TODO*///static void outl_p1_a(void)  { port_w(1, R.A); R.P1 = R.A; }
/*TODO*///static void outl_p2_a(void)  { port_w(2, R.A); R.P2 = R.A; }
/*TODO*///#ifdef MESS
/*TODO*///	static void ret(void)		 { R.PC.w.l = ((pull() & 0x0f) << 8); R.PC.w.l |= pull(); change_pc16(R.PC.w.l); }
/*TODO*///#else
/*TODO*///	static void ret(void)		 { R.PC.w.l = ((pull() & 0x0f) << 8); R.PC.w.l |= pull();  }
/*TODO*///#endif
/*TODO*///
/*TODO*///static void retr(void)
/*TODO*///{
/*TODO*///	UINT8 i=pull();
/*TODO*///	R.PC.w.l = ((i & 0x0f) << 8) | pull();
/*TODO*///	#ifdef MESS
/*TODO*///		change_pc16(R.PC.w.l);
/*TODO*///	#endif
/*TODO*///	R.irq_executing = I8039_IGNORE_INT;
/*TODO*/////	R.A11 = R.A11ff;	/* NS990113 */
/*TODO*///	R.PSW = (R.PSW & 0x0f) | (i & 0xf0);   /* Stack is already changed by pull */
/*TODO*///	regPTR = ((M_By) ? 24 : 0);
/*TODO*///}
/*TODO*///static void rl_a(void)		 { UINT8 i=R.A & 0x80; R.A <<= 1; if (i) R.A |= 0x01; else R.A &= 0xfe; }
/*TODO*////* NS990113 */
/*TODO*///static void rlc_a(void) 	 { UINT8 i=M_Cy; if (R.A & 0x80) SET(C_FLAG); else CLR(C_FLAG); R.A <<= 1; if (i) R.A |= 0x01; else R.A &= 0xfe; }
/*TODO*///static void rr_a(void)		 { UINT8 i=R.A & 1; R.A >>= 1; if (i) R.A |= 0x80; else R.A &= 0x7f; }
/*TODO*////* NS990113 */
/*TODO*///static void rrc_a(void) 	 { UINT8 i=M_Cy; if (R.A & 1) SET(C_FLAG); else CLR(C_FLAG); R.A >>= 1; if (i) R.A |= 0x80; else R.A &= 0x7f; }
/*TODO*///static void sel_mb0(void)    { R.A11 = 0; R.A11ff = 0; }
/*TODO*///static void sel_mb1(void)    { R.A11ff = 0x800; if (R.irq_executing == I8039_IGNORE_INT) R.A11 = 0x800; }
/*TODO*///static void sel_rb0(void)    { CLR(B_FLAG); regPTR = 0;  }
/*TODO*///static void sel_rb1(void)    { SET(B_FLAG); regPTR = 24; }
/*TODO*///static void stop_tcnt(void)  { R.timerON = R.countON = 0; }
/*TODO*///static void strt_cnt(void)   { R.countON = 1; Old_T1 = test_r(1); }	/* NS990113 */
/*TODO*///static void strt_t(void)     { R.timerON = 1; R.masterClock = 0; }	/* NS990113 */
/*TODO*///static void swap_a(void)	 { UINT8 i=R.A >> 4; R.A <<= 4; R.A |= i; }
/*TODO*///static void xch_a_r0(void)	 { UINT8 i=R.A; R.A=R0; R0=i; }
/*TODO*///static void xch_a_r1(void)	 { UINT8 i=R.A; R.A=R1; R1=i; }
/*TODO*///static void xch_a_r2(void)	 { UINT8 i=R.A; R.A=R2; R2=i; }
/*TODO*///static void xch_a_r3(void)	 { UINT8 i=R.A; R.A=R3; R3=i; }
/*TODO*///static void xch_a_r4(void)	 { UINT8 i=R.A; R.A=R4; R4=i; }
/*TODO*///static void xch_a_r5(void)	 { UINT8 i=R.A; R.A=R5; R5=i; }
/*TODO*///static void xch_a_r6(void)	 { UINT8 i=R.A; R.A=R6; R6=i; }
/*TODO*///static void xch_a_r7(void)	 { UINT8 i=R.A; R.A=R7; R7=i; }
/*TODO*///static void xch_a_xr0(void)  { UINT8 i=R.A; R.A=intRAM[R0 & 0x7f]; intRAM[R0 & 0x7f]=i; }
/*TODO*///static void xch_a_xr1(void)  { UINT8 i=R.A; R.A=intRAM[R1 & 0x7f]; intRAM[R1 & 0x7f]=i; }
/*TODO*///static void xchd_a_xr0(void) { M_XCHD(R0 & 0x7f); }
/*TODO*///static void xchd_a_xr1(void) { M_XCHD(R1 & 0x7f); }
/*TODO*///static void xrl_a_n(void)    { R.A ^= M_RDMEM_OPCODE(); }
/*TODO*///static void xrl_a_r0(void)   { R.A ^= R0; }
/*TODO*///static void xrl_a_r1(void)   { R.A ^= R1; }
/*TODO*///static void xrl_a_r2(void)   { R.A ^= R2; }
/*TODO*///static void xrl_a_r3(void)   { R.A ^= R3; }
/*TODO*///static void xrl_a_r4(void)   { R.A ^= R4; }
/*TODO*///static void xrl_a_r5(void)   { R.A ^= R5; }
/*TODO*///static void xrl_a_r6(void)   { R.A ^= R6; }
/*TODO*///static void xrl_a_r7(void)   { R.A ^= R7; }
/*TODO*///static void xrl_a_xr0(void)  { R.A ^= intRAM[R0 & 0x7f]; }
/*TODO*///static void xrl_a_xr1(void)  { R.A ^= intRAM[R1 & 0x7f]; }
/*TODO*///
/*TODO*///static s_opcode opcode_main[256]=
/*TODO*///{
/*TODO*///	{1, nop 	   },{0, illegal	},{2, outl_bus_a },{2, add_a_n	  },{2, jmp 	   },{1, en_i		},{0, illegal	 },{1, dec_a	  },
/*TODO*///	{2, ins_a_bus  },{2, in_a_p1	},{2, in_a_p2	 },{0, illegal	  },{2, movd_a_p4  },{2, movd_a_p5	},{2, movd_a_p6  },{2, movd_a_p7  },
/*TODO*///	{1, inc_xr0    },{1, inc_xr1	},{2, jb_0		 },{2, adc_a_n	  },{2, call	   },{1, dis_i		},{2, jtf		 },{1, inc_a	  },
/*TODO*///	{1, inc_r0	   },{1, inc_r1 	},{1, inc_r2	 },{1, inc_r3	  },{1, inc_r4	   },{1, inc_r5 	},{1, inc_r6	 },{1, inc_r7	  },
/*TODO*///	{1, xch_a_xr0  },{1, xch_a_xr1	},{0, illegal	 },{2, mov_a_n	  },{2, jmp_1	   },{1, en_tcnti	},{2, jnt_0 	 },{1, clr_a	  },
/*TODO*///	{1, xch_a_r0   },{1, xch_a_r1	},{1, xch_a_r2	 },{1, xch_a_r3   },{1, xch_a_r4   },{1, xch_a_r5	},{1, xch_a_r6	 },{1, xch_a_r7   },
/*TODO*///	{1, xchd_a_xr0 },{1, xchd_a_xr1 },{2, jb_1		 },{0, illegal	  },{2, call_1	   },{1, dis_tcnti	},{2, jt_0		 },{1, cpl_a	  },
/*TODO*///	{0, illegal    },{2, outl_p1_a	},{2, outl_p2_a  },{0, illegal	  },{2, movd_p4_a  },{2, movd_p5_a	},{2, movd_p6_a  },{2, movd_p7_a  },
/*TODO*///	{1, orl_a_xr0  },{1, orl_a_xr1	},{1, mov_a_t	 },{2, orl_a_n	  },{2, jmp_2	   },{1, strt_cnt	},{2, jnt_1 	 },{1, swap_a	  },
/*TODO*///	{1, orl_a_r0   },{1, orl_a_r1	},{1, orl_a_r2	 },{1, orl_a_r3   },{1, orl_a_r4   },{1, orl_a_r5	},{1, orl_a_r6	 },{1, orl_a_r7   },
/*TODO*///	{1, anl_a_xr0  },{1, anl_a_xr1	},{2, jb_2		 },{2, anl_a_n	  },{2, call_2	   },{1, strt_t 	},{2, jt_1		 },{1, daa_a	  },
/*TODO*///	{1, anl_a_r0   },{1, anl_a_r1	},{1, anl_a_r2	 },{1, anl_a_r3   },{1, anl_a_r4   },{1, anl_a_r5	},{1, anl_a_r6	 },{1, anl_a_r7   },
/*TODO*///	{1, add_a_xr0  },{1, add_a_xr1	},{1, mov_t_a	 },{0, illegal	  },{2, jmp_3	   },{1, stop_tcnt	},{0, illegal	 },{1, rrc_a	  },
/*TODO*///	{1, add_a_r0   },{1, add_a_r1	},{1, add_a_r2	 },{1, add_a_r3   },{1, add_a_r4   },{1, add_a_r5	},{1, add_a_r6	 },{1, add_a_r7   },
/*TODO*///	{1, adc_a_xr0  },{1, adc_a_xr1	},{2, jb_3		 },{0, illegal	  },{2, call_3	   },{1, ento_clk	},{2, jf_1		 },{1, rr_a 	  },
/*TODO*///	{1, adc_a_r0   },{1, adc_a_r1	},{1, adc_a_r2	 },{1, adc_a_r3   },{1, adc_a_r4   },{1, adc_a_r5	},{1, adc_a_r6	 },{1, adc_a_r7   },
/*TODO*///	{2, movx_a_xr0 },{2, movx_a_xr1 },{0, illegal	 },{2, ret		  },{2, jmp_4	   },{1, clr_f0 	},{2, jni		 },{0, illegal	  },
/*TODO*///	{2, orl_bus_n  },{2, orl_p1_n	},{2, orl_p2_n	 },{0, illegal	  },{2, orld_p4_a  },{2, orld_p5_a	},{2, orld_p6_a  },{2, orld_p7_a  },
/*TODO*///	{2, movx_xr0_a },{2, movx_xr1_a },{2, jb_4		 },{2, retr 	  },{2, call_4	   },{1, cpl_f0 	},{2, jnz		 },{1, clr_c	  },
/*TODO*///	{2, anl_bus_n  },{2, anl_p1_n	},{2, anl_p2_n	 },{0, illegal	  },{2, anld_p4_a  },{2, anld_p5_a	},{2, anld_p6_a  },{2, anld_p7_a  },
/*TODO*///	{1, mov_xr0_a  },{1, mov_xr1_a	},{0, illegal	 },{2, movp_a_xa  },{2, jmp_5	   },{1, clr_f1 	},{0, illegal	 },{1, cpl_c	  },
/*TODO*///	{1, mov_r0_a   },{1, mov_r1_a	},{1, mov_r2_a	 },{1, mov_r3_a   },{1, mov_r4_a   },{1, mov_r5_a	},{1, mov_r6_a	 },{1, mov_r7_a   },
/*TODO*///	{2, mov_xr0_n  },{2, mov_xr1_n	},{2, jb_5		 },{2, jmpp_xa	  },{2, call_5	   },{1, cpl_f1 	},{2, jf0		 },{0, illegal	  },
/*TODO*///	{2, mov_r0_n   },{2, mov_r1_n	},{2, mov_r2_n	 },{2, mov_r3_n   },{2, mov_r4_n   },{2, mov_r5_n	},{2, mov_r6_n	 },{2, mov_r7_n   },
/*TODO*///	{0, illegal    },{0, illegal	},{0, illegal	 },{0, illegal	  },{2, jmp_6	   },{1, sel_rb0	},{2, jz		 },{1, mov_a_psw  },
/*TODO*///	{1, dec_r0	   },{1, dec_r1 	},{1, dec_r2	 },{1, dec_r3	  },{1, dec_r4	   },{1, dec_r5 	},{1, dec_r6	 },{1, dec_r7	  },
/*TODO*///	{1, xrl_a_xr0  },{1, xrl_a_xr1	},{2, jb_6		 },{2, xrl_a_n	  },{2, call_6	   },{1, sel_rb1	},{0, illegal	 },{1, mov_psw_a  },
/*TODO*///	{1, xrl_a_r0   },{1, xrl_a_r1	},{1, xrl_a_r2	 },{1, xrl_a_r3   },{1, xrl_a_r4   },{1, xrl_a_r5	},{1, xrl_a_r6	 },{1, xrl_a_r7   },
/*TODO*///	{0, illegal    },{0, illegal	},{0, illegal	 },{2, movp3_a_xa },{2, jmp_7	   },{1, sel_mb0	},{2, jnc		 },{1, rl_a 	  },
/*TODO*///	{2, djnz_r0    },{2, djnz_r1	},{2, djnz_r2	 },{2, djnz_r3	  },{2, djnz_r4    },{2, djnz_r5	},{2, djnz_r6	 },{2, djnz_r7	  },
/*TODO*///	{1, mov_a_xr0  },{1, mov_a_xr1	},{2, jb_7		 },{0, illegal	  },{2, call_7	   },{1, sel_mb1	},{2, jc		 },{1, rlc_a	  },
/*TODO*///	{1, mov_a_r0   },{1, mov_a_r1	},{1, mov_a_r2	 },{1, mov_a_r3   },{1, mov_a_r4   },{1, mov_a_r5	},{1, mov_a_r6	 },{1, mov_a_r7   }
/*TODO*///};
/*TODO*///
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * Issue an interrupt if necessary
/*TODO*/// ****************************************************************************/
/*TODO*///static int Timer_IRQ(void)
/*TODO*///{
/*TODO*///	if (R.tirq_en && !R.irq_executing)
/*TODO*///	{
/*TODO*///		logerror("I8039:  TIMER INTERRUPT\n");
/*TODO*///		R.irq_executing = I8039_TIMER_INT;
/*TODO*///		push(R.PC.b.l);
/*TODO*///		push((R.PC.b.h & 0x0f) | (R.PSW & 0xf0));
/*TODO*///		R.PC.w.l = 0x07;
/*TODO*///		#ifdef MESS
/*TODO*///			change_pc16(0x07);
/*TODO*///		#endif
/*TODO*///		R.A11ff = R.A11;
/*TODO*///		R.A11	= 0;
/*TODO*///		return 2;		/* 2 clock cycles used */
/*TODO*///	}
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*///static int Ext_IRQ(void)
/*TODO*///{
/*TODO*///	if (R.xirq_en) {
/*TODO*/////logerror("I8039:  EXT INTERRUPT\n");
/*TODO*///		R.irq_executing = I8039_EXT_INT;
/*TODO*///		push(R.PC.b.l);
/*TODO*///		push((R.PC.b.h & 0x0f) | (R.PSW & 0xf0));
/*TODO*///		R.PC.w.l = 0x03;
/*TODO*///		R.A11ff = R.A11;
/*TODO*///		R.A11   = 0;
/*TODO*///		return 2;		/* 2 clock cycles used */
/*TODO*///	}
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * Initialize emulation
/*TODO*/// ****************************************************************************/
/*TODO*///void i8039_init (void)
/*TODO*///{
/*TODO*///}
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * Reset registers to their initial values
/*TODO*/// ****************************************************************************/
/*TODO*///void i8039_reset (void *param)
/*TODO*///{
/*TODO*///	R.PC.w.l  = 0;
/*TODO*///	R.SP  = 0;
/*TODO*///	R.A   = 0;
/*TODO*///	R.PSW = 0x08;		/* Start with Carry SET, Bit 4 is always SET */
/*TODO*///	memset(R.RAM, 0x0, 128);
/*TODO*///	R.P1  = 0xff;
/*TODO*///	R.P2  = 0xff;
/*TODO*///	R.bus = 0;
/*TODO*///	R.irq_executing = I8039_IGNORE_INT;
/*TODO*///	R.pending_irq	= I8039_IGNORE_INT;
/*TODO*///
/*TODO*///	R.A11ff   = R.A11     = 0;
/*TODO*///	R.timerON = R.countON = 0;
/*TODO*///	R.tirq_en = R.xirq_en = 0;
/*TODO*///	R.xirq_en = 0;	/* NS990113 */
/*TODO*///	R.timerON = 1;	/* Mario Bros. doesn't work without this */
/*TODO*///	R.masterClock = 0;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * Shut down CPU emulation
/*TODO*/// ****************************************************************************/
/*TODO*///void i8039_exit (void)
/*TODO*///{
/*TODO*///	/* nothing to do ? */
/*TODO*///}
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * Execute cycles CPU cycles. Return number of cycles really executed
/*TODO*/// ****************************************************************************/
/*TODO*///int i8039_execute(int cycles)
/*TODO*///{
/*TODO*///	unsigned opcode, T1;
/*TODO*///	int count;
/*TODO*///
/*TODO*///	i8039_ICount=cycles;
/*TODO*///
/*TODO*///	do {
/*TODO*///		switch (R.pending_irq)
/*TODO*///		{
/*TODO*///			case I8039_COUNT_INT:
/*TODO*///			case I8039_TIMER_INT:
/*TODO*///				count = Timer_IRQ();
/*TODO*///				i8039_ICount -= count;
/*TODO*///				if (R.timerON)  /* NS990113 */
/*TODO*///					R.masterClock += count;
/*TODO*///				R.t_flag = 1;
/*TODO*///				break;
/*TODO*///			case I8039_EXT_INT:
/*TODO*///				if (R.irq_callback) (*R.irq_callback)(0);
/*TODO*///				count = Ext_IRQ();
/*TODO*///				i8039_ICount -= count;
/*TODO*///				if (R.timerON)  /* NS990113 */
/*TODO*///					R.masterClock += count;
/*TODO*///				break;
/*TODO*///		}
/*TODO*///		R.pending_irq = I8039_IGNORE_INT;
/*TODO*///
/*TODO*///		R.PREPC = R.PC;
/*TODO*///
/*TODO*///		CALL_MAME_DEBUG;
/*TODO*///
/*TODO*///		opcode=M_RDOP(R.PC.w.l);
/*TODO*///
/*TODO*////*		logerror("I8039:  PC = %04x,  opcode = %02x\n", R.PC.w.l, opcode); */
/*TODO*///
/*TODO*///		R.PC.w.l++;
/*TODO*///		i8039_ICount -= opcode_main[opcode].cycles;
/*TODO*///		(*(opcode_main[opcode].function))();
/*TODO*///
/*TODO*///		if (R.countON)  /* NS990113 */
/*TODO*///		{
/*TODO*///			T1 = test_r(1);
/*TODO*///			if (POSITIVE_EDGE_T1)
/*TODO*///			{   /* Handle COUNTER IRQs */
/*TODO*///				R.timer++;
/*TODO*///				if (R.timer == 0) R.pending_irq = I8039_COUNT_INT;
/*TODO*///
/*TODO*///				Old_T1 = T1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		if (R.timerON) {							/* Handle TIMER IRQs */
/*TODO*///			R.masterClock += opcode_main[opcode].cycles;
/*TODO*///			if (R.masterClock >= 32) {  /* NS990113 */
/*TODO*///				R.masterClock -= 32;
/*TODO*///				R.timer++;
/*TODO*///				if (R.timer == 0) R.pending_irq = I8039_TIMER_INT;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	} while (i8039_ICount>0);
/*TODO*///
/*TODO*///	return cycles - i8039_ICount;
/*TODO*///}
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * Get all registers in given buffer
/*TODO*/// ****************************************************************************/
/*TODO*///unsigned i8039_get_context (void *dst)
/*TODO*///{
/*TODO*///	if( dst )
/*TODO*///		*(I8039_Regs*)dst = R;
/*TODO*///	return sizeof(I8039_Regs);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * Set all registers to given values
/*TODO*/// ****************************************************************************/
/*TODO*///void i8039_set_context (void *src)
/*TODO*///{
/*TODO*///	if( src )
/*TODO*///	{
/*TODO*///		R = *(I8039_Regs*)src;
/*TODO*///		regPTR = ((M_By) ? 24 : 0);
/*TODO*///		R.SP = (R.PSW << 1) & 0x0f;
/*TODO*///		#ifdef MESS
/*TODO*///			change_pc16(R.PC.w.l);
/*TODO*///		#endif
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////****************************************************************************/
/*TODO*////* Get a specific register                                                  */
/*TODO*////****************************************************************************/
/*TODO*///unsigned i8039_get_reg (int regnum)
/*TODO*///{
/*TODO*///	switch( regnum )
/*TODO*///	{
/*TODO*///		case REG_PC:
/*TODO*///		case I8039_PC: return R.PC.w.l;
/*TODO*///		case REG_SP:
/*TODO*///		case I8039_SP: return R.SP;
/*TODO*///		case I8039_PSW: return R.PSW;
/*TODO*///		case I8039_A: return R.A;
/*TODO*///		case I8039_IRQ_STATE: return R.irq_state;
/*TODO*///		case I8039_R0: return R0;
/*TODO*///		case I8039_R1: return R1;
/*TODO*///		case I8039_R2: return R2;
/*TODO*///		case I8039_R3: return R3;
/*TODO*///		case I8039_R4: return R4;
/*TODO*///		case I8039_R5: return R5;
/*TODO*///		case I8039_R6: return R6;
/*TODO*///		case I8039_R7: return R7;
/*TODO*///		case I8039_P1: return R.P1;
/*TODO*///		case I8039_P2: return R.P2;
/*TODO*///		case REG_PREVIOUSPC: return R.PREPC.w.l;
/*TODO*///		default:
/*TODO*///			if( regnum <= REG_SP_CONTENTS )
/*TODO*///			{
/*TODO*///				unsigned offset = 8 + 2 * ((R.SP + REG_SP_CONTENTS - regnum) & 7);
/*TODO*///				return R.RAM[offset] + 256 * R.RAM[offset+1];
/*TODO*///			}
/*TODO*///	}
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////****************************************************************************/
/*TODO*////* Set a specific register                                                  */
/*TODO*////****************************************************************************/
/*TODO*///void i8039_set_reg (int regnum, unsigned val)
/*TODO*///{
/*TODO*///	switch( regnum )
/*TODO*///	{
/*TODO*///		case REG_PC:
/*TODO*///		case I8039_PC: R.PC.w.l = val; break;
/*TODO*///		case REG_SP:
/*TODO*///		case I8039_SP: R.SP = val; break;
/*TODO*///		case I8039_PSW: R.PSW = val; break;
/*TODO*///		case I8039_A: R.A = val; break;
/*TODO*///		case I8039_IRQ_STATE: i8039_set_irq_line( 0, val ); break;
/*TODO*///		case I8039_R0: R0 = val; break;
/*TODO*///		case I8039_R1: R1 = val; break;
/*TODO*///		case I8039_R2: R2 = val; break;
/*TODO*///		case I8039_R3: R3 = val; break;
/*TODO*///		case I8039_R4: R4 = val; break;
/*TODO*///		case I8039_R5: R5 = val; break;
/*TODO*///		case I8039_R6: R6 = val; break;
/*TODO*///		case I8039_R7: R7 = val; break;
/*TODO*///		case I8039_P1: R.P1 = val; break;
/*TODO*///		case I8039_P2: R.P2 = val; break;
/*TODO*///		default:
/*TODO*///			if( regnum <= REG_SP_CONTENTS )
/*TODO*///			{
/*TODO*///				unsigned offset = 8 + 2 * ((R.SP + REG_SP_CONTENTS - regnum) & 7);
/*TODO*///				R.RAM[offset] = val & 0xff;
/*TODO*///				R.RAM[offset+1] = val >> 8;
/*TODO*///			}
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////****************************************************************************/
/*TODO*////* Set IRQ line state														*/
/*TODO*////****************************************************************************/
/*TODO*///void i8039_set_irq_line(int irqline, int state)
/*TODO*///{
/*TODO*///	R.irq_state = state;
/*TODO*///	if (state == CLEAR_LINE)
/*TODO*///		R.pending_irq &= ~I8039_EXT_INT;
/*TODO*///	else
/*TODO*///		R.pending_irq |= I8039_EXT_INT;
/*TODO*///}
/*TODO*///
/*TODO*////****************************************************************************/
/*TODO*////* Set IRQ callback (interrupt acknowledge) 								*/
/*TODO*////****************************************************************************/
/*TODO*///void i8039_set_irq_callback(int (*callback)(int irqline))
/*TODO*///{
/*TODO*///	R.irq_callback = callback;
/*TODO*///}
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * Return a formatted string for a register
/*TODO*/// ****************************************************************************/
/*TODO*///const char *i8039_info(void *context, int regnum)
/*TODO*///{
/*TODO*///	static char buffer[8][47+1];
/*TODO*///	static int which = 0;
/*TODO*///	I8039_Regs *r = context;
/*TODO*///
/*TODO*///	which = (which+1) % 8;
/*TODO*///	buffer[which][0] = '\0';
/*TODO*///	if( !context )
/*TODO*///		r = &R;
/*TODO*///
/*TODO*///	switch( regnum )
/*TODO*///	{
/*TODO*///		case CPU_INFO_REG+I8039_PC: sprintf(buffer[which], "PC:%04X", r->PC.w.l); break;
/*TODO*///		case CPU_INFO_REG+I8039_SP: sprintf(buffer[which], "SP:%02X", r->SP); break;
/*TODO*///		case CPU_INFO_REG+I8039_PSW: sprintf(buffer[which], "PSW:%02X", r->PSW); break;
/*TODO*///		case CPU_INFO_REG+I8039_A: sprintf(buffer[which], "A:%02X", r->A); break;
/*TODO*///		case CPU_INFO_REG+I8039_IRQ_STATE: sprintf(buffer[which], "IRQ:%X", r->irq_state); break;
/*TODO*///		case CPU_INFO_REG+I8039_R0: sprintf(buffer[which], "R0:%02X", r->RAM[r->regPtr+0]); break;
/*TODO*///		case CPU_INFO_REG+I8039_R1: sprintf(buffer[which], "R1:%02X", r->RAM[r->regPtr+1]); break;
/*TODO*///		case CPU_INFO_REG+I8039_R2: sprintf(buffer[which], "R2:%02X", r->RAM[r->regPtr+2]); break;
/*TODO*///		case CPU_INFO_REG+I8039_R3: sprintf(buffer[which], "R3:%02X", r->RAM[r->regPtr+3]); break;
/*TODO*///		case CPU_INFO_REG+I8039_R4: sprintf(buffer[which], "R4:%02X", r->RAM[r->regPtr+4]); break;
/*TODO*///		case CPU_INFO_REG+I8039_R5: sprintf(buffer[which], "R5:%02X", r->RAM[r->regPtr+5]); break;
/*TODO*///		case CPU_INFO_REG+I8039_R6: sprintf(buffer[which], "R6:%02X", r->RAM[r->regPtr+6]); break;
/*TODO*///		case CPU_INFO_REG+I8039_R7: sprintf(buffer[which], "R7:%02X", r->RAM[r->regPtr+7]); break;
/*TODO*///		case CPU_INFO_REG+I8039_P1: sprintf(buffer[which], "P1:%02X", r->P1); break;
/*TODO*///		case CPU_INFO_REG+I8039_P2: sprintf(buffer[which], "P2:%02X", r->P2); break;
/*TODO*///		case CPU_INFO_FLAGS:
/*TODO*///			sprintf(buffer[which], "%c%c%c%c%c%c%c%c",
/*TODO*///				r->PSW & 0x80 ? 'C':'.',
/*TODO*///				r->PSW & 0x40 ? 'A':'.',
/*TODO*///				r->PSW & 0x20 ? 'F':'.',
/*TODO*///				r->PSW & 0x10 ? 'B':'.',
/*TODO*///				r->PSW & 0x08 ? '?':'.',
/*TODO*///				r->PSW & 0x04 ? '4':'.',
/*TODO*///				r->PSW & 0x02 ? '2':'.',
/*TODO*///				r->PSW & 0x01 ? '1':'.');
/*TODO*///			break;
/*TODO*///		case CPU_INFO_NAME: return "I8039";
/*TODO*///		case CPU_INFO_FAMILY: return "Intel 8039";
/*TODO*///		case CPU_INFO_VERSION: return "1.1";
/*TODO*///		case CPU_INFO_FILE: return __FILE__;
/*TODO*///		case CPU_INFO_CREDITS: return "Copyright (C) 1997 by Mirko Buffoni\nBased on the original work (C) 1997 by Dan Boris";
/*TODO*///		case CPU_INFO_REG_LAYOUT: return (const char*)i8039_reg_layout;
/*TODO*///		case CPU_INFO_WIN_LAYOUT: return (const char*)i8039_win_layout;
/*TODO*///	}
/*TODO*///	return buffer[which];
/*TODO*///}
/*TODO*///
/*TODO*///unsigned i8039_dasm(char *buffer, unsigned pc)
/*TODO*///{
/*TODO*///#ifdef	MAME_DEBUG
/*TODO*///    return Dasm8039(buffer,pc);
/*TODO*///#else
/*TODO*///	sprintf( buffer, "$%02X", cpu_readop(pc) );
/*TODO*///	return 1;
/*TODO*///#endif
/*TODO*///}
    @Override
    public void init() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void reset(Object param) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    public Object get_context() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_context(Object reg) {
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String cpu_info(Object context, int regnum) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * unused functions
     */
    @Override
    public int[] get_cycle_table(int which) {
        return null;
    }

    @Override
    public void set_cycle_table(int which, int[] new_table) {

    }

    @Override
    public String cpu_dasm(String buffer, int pc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int internal_read(int offset) {
        return 0; //doesn't exist in m6809 cpu
    }

    @Override
    public void internal_write(int offset, int data) {
        //doesesn't exist in m6809 cpu
    }

    /**
     * *
     *
     * arcadeflex functions
     */
    @Override
    public Object init_context() {
        Object reg = new I8039_Regs();
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
    public int mem_address_bits_of_cpu() {
        return 16;
    }
}
