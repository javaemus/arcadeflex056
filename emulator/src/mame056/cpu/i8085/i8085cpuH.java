/*******************************************************
 *
 *      Portable (hopefully ;-) 8085A emulator
 *
 *      Written by J. Buchmueller for use with MAME
 *
 *		Partially based on Z80Em by Marcel De Kogel
 *
 *      CPU related macros
 *
 *******************************************************/

package mame056.cpu.i8085;

import static mame056.cpu.i8085.i8085.ARG;
import static mame056.cpu.i8085.i8085.ARG16;
import static mame056.cpu.i8085.i8085.I;
import static mame056.cpu.i8085.i8085.RM;
import static mame056.cpu.i8085.i8085.WM;
import static mame056.cpu.i8085.i8085.ZS;
import static mame056.cpu.i8085.i8085.ZSP;
import static mame056.cpu.i8085.i8085.i8085_ICount;
import mame056.cpu.m6502.m6502.PAIR;
import static mame056.memory.cpu_readport16;
import static mame056.memory.cpu_writeport16;
import static mame056.memoryH.change_pc16;

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
public class i8085cpuH {

    public static final int SF              = 0x80;
    public static final int ZF              = 0x40;
    public static final int YF              = 0x20;
    public static final int HF              = 0x10;
    public static final int XF              = 0x08;
    public static final int VF              = 0x04;
    public static final int NF              = 0x02;
    public static final int CF              = 0x01;

    public static final int IM_SID          = 0x80;
    public static final int IM_SOD          = 0x40;
    public static final int IM_IEN          = 0x20;
    public static final int IM_TRAP         = 0x10;
    public static final int IM_INTR         = 0x08;
    public static final int IM_RST75        = 0x04;
    public static final int IM_RST65        = 0x02;
    public static final int IM_RST55        = 0x01;

    public static final int ADDR_TRAP       = 0x0024;
    public static final int ADDR_RST55      = 0x002c;
    public static final int ADDR_RST65      = 0x0034;
    public static final int ADDR_RST75      = 0x003c;
    public static final int ADDR_INTR       = 0x0038;

    public static void M_INR(int R){
        ++R;
        I.AF.L=(I.AF.L&CF)|ZS[R]|((R==0x80)?VF:0)|((R&0x0F)!=0?0:HF);
    }
    public static void M_DCR(int R){ 
        I.AF.L=(I.AF.L&CF)|NF|((R==0x80)?VF:0)|((R&0x0F)!=0?0:HF); 
        I.AF.L|=ZS[--R];
    }
    public static void M_MVI(int R){ 
        R=ARG();
    }

    public static void M_ANA(int R){
        I.AF.H&=R;    
        I.AF.L=ZSP[I.AF.H]|HF;
    }
    public static void M_ORA(int R){
        I.AF.H|=R;
        I.AF.L=ZSP[I.AF.H];
    }
    public static void M_XRA(int R){ 
        I.AF.H^=R;    
        I.AF.L=ZSP[I.AF.H];
    }

    public static void M_RLC() {
	I.AF.H = (I.AF.H << 1) | (I.AF.H >> 7);
	I.AF.L = (I.AF.L & ~(HF+NF+CF)) | (I.AF.H & CF);
    }

    public static void M_RRC() {
	I.AF.L = (I.AF.L & ~(HF+NF+CF)) | (I.AF.H & CF);
	I.AF.H = (I.AF.H >> 1) | (I.AF.H << 7);
    }

    public static void M_RAL() {
	int c = I.AF.L&CF;
	I.AF.L = (I.AF.L & ~(HF+NF+CF)) | (I.AF.H >> 7);
	I.AF.H = (I.AF.H << 1) | c;
    }

    public static void M_RAR() {
	int c = (I.AF.L&CF) << 7;
	I.AF.L = (I.AF.L & ~(HF+NF+CF)) | (I.AF.H & CF);
	I.AF.H = (I.AF.H >> 1) | c;
    }

/*TODO*///#ifdef X86_ASM
/*TODO*///#define M_ADD(R)												\
/*TODO*/// asm (															\
/*TODO*/// " addb %2,%0           \n"                                     \
/*TODO*/// " lahf                 \n"                                     \
/*TODO*/// " setob %%al           \n" /* al = 1 if overflow */            \
/*TODO*/// " shlb $2,%%al         \n" /* shift to P/V bit position */     \
/*TODO*/// " andb $0xd1,%%ah      \n" /* sign, zero, half carry, carry */ \
/*TODO*/// " orb %%ah,%%al        \n"                                     \
/*TODO*/// :"=g" (I.AF.b.h), "=a" (I.AF.b.l)                              \
/*TODO*/// :"r" (R), "0" (I.AF.b.h)                                       \
/*TODO*/// )
/*TODO*///#else
    public static void M_ADD(int R) {
        int q = I.AF.H+R;
	I.AF.L=ZS[q&255]|((q>>8)&CF)|
		((I.AF.H^q^R)&HF)|
		(((R^I.AF.H^SF)&(R^q)&SF)>>5);
	I.AF.H=q;
    }
/*TODO*///#endif
/*TODO*///
/*TODO*///#ifdef X86_ASM
/*TODO*///#define M_ADC(R)												\
/*TODO*/// asm (															\
/*TODO*/// " shrb $1,%%al         \n"                                     \
/*TODO*/// " adcb %2,%0           \n"                                     \
/*TODO*/// " lahf                 \n"                                     \
/*TODO*/// " setob %%al           \n" /* al = 1 if overflow */            \
/*TODO*/// " shlb $2,%%al         \n" /* shift to P/V bit position */     \
/*TODO*/// " andb $0xd1,%%ah      \n" /* sign, zero, half carry, carry */ \
/*TODO*/// " orb %%ah,%%al        \n" /* combine with P/V */              \
/*TODO*/// :"=g" (I.AF.b.h), "=a" (I.AF.b.l)                              \
/*TODO*/// :"r" (R), "a" (I.AF.b.l), "0" (I.AF.b.h)                       \
/*TODO*/// )
/*TODO*///#else
    public static void M_ADC(int R) {
	int q = I.AF.H+R+(I.AF.L&CF);
	I.AF.L=ZS[q&255]|((q>>8)&CF)|
		((I.AF.H^q^R)&HF)|
		(((R^I.AF.H^SF)&(R^q)&SF)>>5);
	I.AF.H=q;
    }
/*TODO*///#endif
/*TODO*///
/*TODO*///#ifdef X86_ASM
/*TODO*///#define M_SUB(R)												\
/*TODO*/// asm (															\
/*TODO*/// " subb %2,%0           \n"                                     \
/*TODO*/// " lahf                 \n"                                     \
/*TODO*/// " setob %%al           \n" /* al = 1 if overflow */            \
/*TODO*/// " shlb $2,%%al         \n" /* shift to P/V bit position */     \
/*TODO*/// " andb $0xd1,%%ah      \n" /* sign, zero, half carry, carry */ \
/*TODO*/// " orb $2,%%al          \n" /* set N flag */                    \
/*TODO*/// " orb %%ah,%%al        \n" /* combine with P/V */              \
/*TODO*/// :"=g" (I.AF.b.h), "=a" (I.AF.b.l)                              \
/*TODO*/// :"r" (R), "0" (I.AF.b.h)                                       \
/*TODO*/// )
/*TODO*///#else
    public static void M_SUB(int R) {
	int q = I.AF.H-R;
	I.AF.L=ZS[q&255]|((q>>8)&CF)|NF|
		((I.AF.H^q^R)&HF)|
		(((R^I.AF.H)&(I.AF.H^q)&SF)>>5);
	I.AF.H=q;
    }
/*TODO*///#endif
/*TODO*///
/*TODO*///#ifdef X86_ASM
/*TODO*///#define M_SBB(R)												\
/*TODO*/// asm (															\
/*TODO*/// " shrb $1,%%al         \n"                                     \
/*TODO*/// " sbbb %2,%0           \n"                                     \
/*TODO*/// " lahf                 \n"                                     \
/*TODO*/// " setob %%al           \n" /* al = 1 if overflow */            \
/*TODO*/// " shlb $2,%%al         \n" /* shift to P/V bit position */     \
/*TODO*/// " andb $0xd1,%%ah      \n" /* sign, zero, half carry, carry */ \
/*TODO*/// " orb $2,%%al          \n" /* set N flag */                    \
/*TODO*/// " orb %%ah,%%al        \n" /* combine with P/V */              \
/*TODO*/// :"=g" (I.AF.b.h), "=a" (I.AF.b.l)                              \
/*TODO*/// :"r" (R), "a" (I.AF.b.l), "0" (I.AF.b.h)                       \
/*TODO*/// )
/*TODO*///#else
    public static void M_SBB(int R) {
	int q = I.AF.H-R-(I.AF.L&CF);
	I.AF.L=ZS[q&255]|((q>>8)&CF)|NF|
		((I.AF.H^q^R)&HF)|
		(((R^I.AF.H)&(I.AF.H^q)&SF)>>5);
	I.AF.H=q;
    }
/*TODO*///#endif
/*TODO*///
/*TODO*///#ifdef X86_ASM
/*TODO*///#define M_CMP(R)												\
/*TODO*/// asm (															\
/*TODO*/// " cmpb %2,%0          \n"                                      \
/*TODO*/// " lahf                \n"                                      \
/*TODO*/// " setob %%al          \n" /* al = 1 if overflow */             \
/*TODO*/// " shlb $2,%%al        \n" /* shift to P/V bit position */      \
/*TODO*/// " andb $0xd1,%%ah     \n" /* sign, zero, half carry, carry */  \
/*TODO*/// " orb $2,%%al         \n" /* set N flag */                     \
/*TODO*/// " orb %%ah,%%al       \n" /* combine with P/V */               \
/*TODO*/// :"=g" (I.AF.b.h), "=a" (I.AF.b.l)                              \
/*TODO*/// :"r" (R), "0" (I.AF.b.h)                                       \
/*TODO*/// )
/*TODO*///#else
    public static void M_CMP(int R) {
	int q = I.AF.H-R;
	I.AF.L=ZS[q&255]|((q>>8)&CF)|NF|
		((I.AF.H^q^R)&HF)|
		(((R^I.AF.H)&(I.AF.H^q)&SF)>>5);
    }
/*TODO*///#endif

    public static void M_IN(){
	I.XX.D=ARG();
	I.AF.H=cpu_readport16(I.XX.D);
    }

    public static void M_OUT(){
    	I.XX.D=ARG();
    	cpu_writeport16(I.XX.D,I.AF.H);
    }

/*TODO*///#ifdef	X86_ASM
/*TODO*///#define M_DAD(R)												\
/*TODO*/// asm (															\
/*TODO*/// " andb $0xc4,%1        \n"                                     \
/*TODO*/// " addb %%al,%%cl       \n"                                     \
/*TODO*/// " adcb %%ah,%%ch       \n"                                     \
/*TODO*/// " lahf                 \n"                                     \
/*TODO*/// " andb $0x11,%%ah      \n"                                     \
/*TODO*/// " orb %%ah,%1          \n"                                     \
/*TODO*/// :"=c" (I.HL.d), "=g" (I.AF.b.l)                                \
/*TODO*/// :"0" (I.HL.d), "1" (I.AF.b.l), "a" (I.R.d)                     \
/*TODO*/// )
/*TODO*///#else
    public static void M_DAD(PAIR R) {
	int q = I.HL.D + R.D;
	I.AF.L = ( I.AF.L & ~(HF+CF) ) |
		( ((I.HL.D^q^R.D) >> 8) & HF ) |
		( (q>>16) & CF );
	I.HL.L = q;
    }
/*TODO*///#endif

    public static void M_PUSH(PAIR R) {
	WM(--I.SP.L, R.H);									
	WM(--I.SP.L, R.L);									
    }

    public static void M_POP(PAIR R) {
	R.L = RM(I.SP.L++);
	R.H = RM(I.SP.L++);
    }

    public static void M_RET(int cc)
    {
	if (cc != 0)
	{
		i8085_ICount[0] -= 6;
		M_POP(I.PC);
		change_pc16(I.PC.D);
	}
    }

    public static void M_JMP(int cc) {
	if (cc != 0) {
		I.PC.L = ARG16();
		change_pc16(I.PC.D);
	} else I.PC.L += 2;
    }

    public static void M_CALL(int cc)
    {
	if (cc != 0)
	{
		int a = ARG16();
		i8085_ICount[0] -= 6;
		M_PUSH(I.PC);
		I.PC.D = a;
		change_pc16(I.PC.D);
	} else I.PC.L += 2;
    }

    public static void M_RST(int nn) {
	M_PUSH(I.PC);
	I.PC.D = 8 * nn;
	change_pc16(I.PC.D);
    }

}
