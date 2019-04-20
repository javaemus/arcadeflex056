/**
 * Ported to v0.56
 */
package mame056.cpu.i8039;

import static mame056.memory.*;
import static mame056.memoryH.*;

public class i8039H {

    public static final int I8039_PC = 1;
    /*TODO*///I8039_SP, I8039_PSW, I8039_A,  I8039_IRQ_STATE,
/*TODO*///	   I8039_R0,   I8039_R1, I8039_R2,  I8039_R3, I8039_R4,
/*TODO*///	   I8039_R5,   I8039_R6, I8039_R7,  I8039_P1, I8039_P2
/*TODO*///};
/*TODO*///

    /*   This handling of special I/O ports should be better for actual MAME
 *   architecture.  (i.e., define access to ports { I8039_p1, I8039_p1, dkong_out_w })
     */
    public static final int I8039_p0 = 0x100;/* Not used */
    public static final int I8039_p1 = 0x101;
    public static final int I8039_p2 = 0x102;
    public static final int I8039_p4 = 0x104;
    public static final int I8039_p5 = 0x105;
    public static final int I8039_p6 = 0x106;
    public static final int I8039_p7 = 0x107;
    public static final int I8039_t0 = 0x110;
    public static final int I8039_t1 = 0x111;
    public static final int I8039_bus = 0x120;

    /*TODO*////**************************************************************************
/*TODO*/// * I8035 section
/*TODO*/// **************************************************************************/
/*TODO*///#if (HAS_I8035)
/*TODO*///#define I8035_PC				I8039_PC
/*TODO*///#define I8035_SP				I8039_SP
/*TODO*///#define I8035_PSW				I8039_PSW
/*TODO*///#define I8035_A 				I8039_A
/*TODO*///#define I8035_IRQ_STATE 		I8039_IRQ_STATE
/*TODO*///#define I8035_R0				I8039_R0
/*TODO*///#define I8035_R1				I8039_R1
/*TODO*///#define I8035_R2				I8039_R2
/*TODO*///#define I8035_R3				I8039_R3
/*TODO*///#define I8035_R4				I8039_R4
/*TODO*///#define I8035_R5				I8039_R5
/*TODO*///#define I8035_R6				I8039_R6
/*TODO*///#define I8035_R7				I8039_R7
/*TODO*///#define I8035_P1				I8039_P1
/*TODO*///#define I8035_P2				I8039_P2
/*TODO*///
/*TODO*///#define i8035_ICount            i8039_ICount
/*TODO*///
/*TODO*///extern void i8035_init(void);
/*TODO*///extern void i8035_reset(void *param);
/*TODO*///extern void i8035_exit(void);
/*TODO*///extern int i8035_execute(int cycles);
/*TODO*///extern unsigned i8035_get_context(void *dst);
/*TODO*///extern void i8035_set_context(void *src);
/*TODO*///extern unsigned i8035_get_reg(int regnum);
/*TODO*///extern void i8035_set_reg(int regnum, unsigned val);
/*TODO*///extern void i8035_set_irq_line(int irqline, int state);
/*TODO*///extern void i8035_set_irq_callback(int (*callback)(int irqline));
/*TODO*///extern const char *i8035_info(void *context, int regnum);
/*TODO*///extern unsigned i8035_dasm(char *buffer, unsigned pc);
/*TODO*///#endif
/*TODO*///
/*TODO*////**************************************************************************
/*TODO*/// * I8048 section
/*TODO*/// **************************************************************************/
/*TODO*///#if (HAS_I8048)
/*TODO*///#define I8048_PC				I8039_PC
/*TODO*///#define I8048_SP				I8039_SP
/*TODO*///#define I8048_PSW				I8039_PSW
/*TODO*///#define I8048_A 				I8039_A
/*TODO*///#define I8048_IRQ_STATE 		I8039_IRQ_STATE
/*TODO*///#define I8048_R0				I8039_R0
/*TODO*///#define I8048_R1				I8039_R1
/*TODO*///#define I8048_R2				I8039_R2
/*TODO*///#define I8048_R3				I8039_R3
/*TODO*///#define I8048_R4				I8039_R4
/*TODO*///#define I8048_R5				I8039_R5
/*TODO*///#define I8048_R6				I8039_R6
/*TODO*///#define I8048_R7				I8039_R7
/*TODO*///#define I8048_P1				I8039_P1
/*TODO*///#define I8048_P2				I8039_P2
/*TODO*///
/*TODO*///#define i8048_ICount            i8039_ICount
/*TODO*///
/*TODO*///extern void i8048_init(void);
/*TODO*///extern void i8048_reset(void *param);
/*TODO*///extern void i8048_exit(void);
/*TODO*///extern int i8048_execute(int cycles);
/*TODO*///extern unsigned i8048_get_context(void *dst);
/*TODO*///extern void i8048_set_context(void *src);
/*TODO*///extern unsigned i8048_get_reg(int regnum);
/*TODO*///extern void i8048_set_reg(int regnum, unsigned val);
/*TODO*///extern void i8048_set_irq_line(int irqline, int state);
/*TODO*///extern void i8048_set_irq_callback(int (*callback)(int irqline));
/*TODO*///const char *i8048_info(void *context, int regnum);
/*TODO*///extern unsigned i8048_dasm(char *buffer, unsigned pc);
/*TODO*///#endif
/*TODO*///
/*TODO*////**************************************************************************
/*TODO*/// * N7751 section
/*TODO*/// **************************************************************************/
/*TODO*///#if (HAS_N7751)
/*TODO*///#define N7751_PC				I8039_PC
/*TODO*///#define N7751_SP				I8039_SP
/*TODO*///#define N7751_PSW				I8039_PSW
/*TODO*///#define N7751_A 				I8039_A
/*TODO*///#define N7751_IRQ_STATE 		I8039_IRQ_STATE
/*TODO*///#define N7751_R0				I8039_R0
/*TODO*///#define N7751_R1				I8039_R1
/*TODO*///#define N7751_R2				I8039_R2
/*TODO*///#define N7751_R3				I8039_R3
/*TODO*///#define N7751_R4				I8039_R4
/*TODO*///#define N7751_R5				I8039_R5
/*TODO*///#define N7751_R6				I8039_R6
/*TODO*///#define N7751_R7				I8039_R7
/*TODO*///#define N7751_P1				I8039_P1
/*TODO*///#define N7751_P2				I8039_P2
/*TODO*///
/*TODO*///#define n7751_ICount            i8039_ICount
/*TODO*///
/*TODO*///extern void n7751_init(void);
/*TODO*///extern void n7751_reset(void *param);
/*TODO*///extern void n7751_exit(void);
/*TODO*///extern int n7751_execute(int cycles);
/*TODO*///extern unsigned n7751_get_context(void *dst);
/*TODO*///extern void n7751_set_context(void *src);
/*TODO*///extern unsigned n7751_get_reg(int regnum);
/*TODO*///extern void n7751_set_reg(int regnum, unsigned val);
/*TODO*///extern void n7751_set_irq_line(int irqline, int state);
/*TODO*///extern void n7751_set_irq_callback(int (*callback)(int irqline));
/*TODO*///extern const char *n7751_info(void *context, int regnum);
/*TODO*///extern unsigned n7751_dasm(char *buffer, unsigned pc);
/*TODO*///#endif
/*TODO*///
    /*
     *	 Input a UINT8 from given I/O port
     */
    public static char I8039_In(int Port) {
        return (char) ((cpu_readport16(Port) & 0xFF));
    }

    /*
     *	 Output a UINT8 to given I/O port
     */
    public static void I8039_Out(int Port, int Value) {
        cpu_writeport16(Port, Value & 0xFF);
    }

    /*
     *	 Read a UINT8 from given memory location
     */
    public static char I8039_RDMEM(int addr) {
        return (char) ((cpu_readmem16(addr) & 0xFF));
    }


    /*
     *	 Write a UINT8 to given memory location
     */
    public static void I8039_WRMEM(int addr, int value) {
        cpu_writemem16(addr & 0xFFFF, value & 0xFF);
    }


    /*
     *   I8039_RDOP() is identical to I8039_RDMEM() except it is used for reading
     *   opcodes. In case of system with memory mapped I/O, this function can be
     *   used to greatly speed up emulation
     */
    public static char I8039_RDOP(int addr) {
        return cpu_readop(addr);
    }


    /*
     *   I8039_RDOP_ARG() is identical to I8039_RDOP() except it is used for reading
     *   opcode arguments. This difference can be used to support systems that
     *   use different encoding mechanisms for opcodes and opcode arguments
     */
    public static char I8039_RDOP_ARG(int addr) {
        return cpu_readop_arg(addr);
    }
}
