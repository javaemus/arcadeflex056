/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.cpu.i8085;

import static mame056.cpu.i8085.i8085.i8085_ICount;

public class i8085H {
    
        public static final int I8085_PC=1;
        public static final int I8085_SP=2;
        public static final int I8085_AF=3;
        public static final int I8085_BC=4;
        public static final int I8085_DE=5;
        public static final int I8085_HL=6;
        public static final int I8085_HALT=7;
        public static final int I8085_IM=8;
        public static final int I8085_IREQ=9;
        public static final int I8085_ISRV=10;
        public static final int I8085_VECTOR=11;
        public static final int I8085_TRAP_STATE=12;
        public static final int I8085_INTR_STATE=13;
        public static final int I8085_RST55_STATE=14;
        public static final int I8085_RST65_STATE=15;
        public static final int I8085_RST75_STATE=16;
	
	public static final int I8085_INTR_LINE = 0;
	public static final int I8085_RST55_LINE = 1;
	public static final int I8085_RST65_LINE = 2;
	public static final int I8085_RST75_LINE = 3;
	
	
	/*TODO*///extern unsigned i8085_get_context(void *dst);
	/*TODO*///extern unsigned i8085_get_reg(int regnum);
	/*TODO*///extern const char *i8085_info(void *context, int regnum);
	/*TODO*///extern unsigned i8085_dasm(char *buffer, unsigned pc);
	
	/**************************************************************************
	 * I8080 section
	 **************************************************************************/
	/*TODO*///#if (HAS_8080)
	public static final int I8080_PC                = I8085_PC;
	public static final int I8080_SP		= I8085_SP;
	public static final int I8080_BC                = I8085_BC;
	public static final int I8080_DE                = I8085_DE;
	public static final int I8080_HL                = I8085_HL;
        public static final int I8080_AF                = I8085_AF;
	public static final int I8080_HALT              = I8085_HALT;
	public static final int I8080_IREQ              = I8085_IREQ;
	public static final int I8080_ISRV              = I8085_ISRV;
	public static final int I8080_VECTOR            = I8085_VECTOR;
	public static final int I8080_TRAP_STATE        = I8085_TRAP_STATE;
	public static final int I8080_INTR_STATE        = I8085_INTR_STATE;
	
	/*TODO*///#define I8080_REG_LAYOUT \
	/*TODO*///{	CPU_8080, \
	/*TODO*///	I8080_AF,I8080_BC,I8080_DE,I8080_HL,I8080_SP,I8080_PC, DBG_ROW, \
	/*TODO*///	I8080_HALT,I8080_IREQ,I8080_ISRV,I8080_VECTOR, I8080_TRAP_STATE,I8080_INTR_STATE, \
	/*TODO*///    DBG_END }
	
	public static final int I8080_INTR_LINE         = I8085_INTR_LINE;
	
        /*TODO*///public static int[] i8080_ICount = i8085_ICount;
	/*TODO*///extern unsigned i8080_get_context(void *dst);
	/*TODO*///extern unsigned i8080_get_reg(int regnum);
	/*TODO*///extern const char *i8080_info(void *context, int regnum);
	/*TODO*///extern unsigned i8080_dasm(char *buffer, unsigned pc);
	/*TODO*///#endif
	
	/*TODO*///#ifdef	MAME_DEBUG
	/*TODO*///extern unsigned Dasm8085(char *buffer, unsigned pc);
	/*TODO*///#endif
	
	/*TODO*///#endif
    
}
