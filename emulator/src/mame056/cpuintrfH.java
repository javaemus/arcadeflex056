/**
 * Ported to 0.56
 */
package mame056;

import static mame056.cpuintrf.*;

public class cpuintrfH {

    /**
     * ***********************************
     *
     * Enum listing all the CPUs
     *
     ************************************
     */
    public static final int CPU_DUMMY = 0;
    public static final int CPU_Z80 = 1;
    public static final int CPU_8080 = 2;
    public static final int CPU_8085A = 3;
    public static final int CPU_M6502 = 4;
    public static final int CPU_M65C02 = 5;
    public static final int CPU_M65SC02 = 6;
    public static final int CPU_M65CE02 = 7;
    public static final int CPU_M6509 = 8;
    public static final int CPU_M6510 = 9;
    public static final int CPU_M6510T = 10;
    public static final int CPU_M7501 = 11;
    public static final int CPU_M8502 = 12;
    public static final int CPU_N2A03 = 13;
    public static final int CPU_M4510 = 14;
    public static final int CPU_H6280 = 15;
    public static final int CPU_I86 = 16;
    public static final int CPU_I88 = 17;
    public static final int CPU_I186 = 18;
    public static final int CPU_I188 = 19;
    public static final int CPU_I286 = 20;
    public static final int CPU_V20 = 21;
    public static final int CPU_V30 = 22;
    public static final int CPU_V33 = 23;
    public static final int CPU_V60 = 24;
    public static final int CPU_I8035 = 25;
    public static final int CPU_I8039 = 26;
    public static final int CPU_I8048 = 27;
    public static final int CPU_N7751 = 28;
    public static final int CPU_I8X41 = 29;
    public static final int CPU_M6800 = 30;
    public static final int CPU_M6801 = 31;
    public static final int CPU_M6802 = 32;
    public static final int CPU_M6803 = 33;
    public static final int CPU_M6808 = 34;
    public static final int CPU_HD63701 = 35;
    public static final int CPU_NSC8105 = 36;
    public static final int CPU_M6805 = 37;
    public static final int CPU_M68705 = 38;
    public static final int CPU_HD63705 = 39;
    public static final int CPU_HD6309 = 40;
    public static final int CPU_M6809 = 41;
    public static final int CPU_KONAMI = 42;
    public static final int CPU_M68000 = 43;
    public static final int CPU_M68010 = 44;
    public static final int CPU_M68EC020 = 45;
    public static final int CPU_M68020 = 46;
    public static final int CPU_T11 = 47;
    public static final int CPU_S2650 = 48;
    public static final int CPU_TMS34010 = 49;
    public static final int CPU_TMS34020 = 50;
    public static final int CPU_TMS9900 = 51;
    public static final int CPU_TMS9940 = 52;
    public static final int CPU_TMS9980 = 53;
    public static final int CPU_TMS9985 = 54;
    public static final int CPU_TMS9989 = 55;
    public static final int CPU_TMS9995 = 56;
    public static final int CPU_TMS99105A = 57;
    public static final int CPU_TMS99110A = 58;
    public static final int CPU_Z8000 = 59;
    public static final int CPU_TMS320C10 = 60;
    public static final int CPU_CCPU = 61;
    public static final int CPU_ADSP2100 = 62;
    public static final int CPU_ADSP2105 = 63;
    public static final int CPU_PSXCPU = 64;
    public static final int CPU_ASAP = 65;
    public static final int CPU_UPD7810 = 66;

    public static final int CPU_COUNT = 67;

    /**
     * ***********************************
     *
     * Interrupt line constants
     *
     ************************************
     */
    /* line states */
    public static final int CLEAR_LINE = 0;/* clear (a fired, held or pulsed) line */
    public static final int ASSERT_LINE = 1;/* assert an interrupt immediately */
    public static final int HOLD_LINE = 2;/* hold interrupt line until acknowledged */
    public static final int PULSE_LINE = 3;/* pulse interrupt line for one instruction */

 /* internal flags (not for use by drivers!) */
    public static final int INTERNAL_CLEAR_LINE = 100 + CLEAR_LINE;
    public static final int INTERNAL_ASSERT_LINE = 100 + ASSERT_LINE;

    /* interrupt parameters */
    public static final int MAX_IRQ_LINES = 8;/* maximum number of IRQ lines per CPU */
    public static final int IRQ_LINE_NMI = 127;/* IRQ line for NMIs */


    /**
     * ***********************************
     *
     * CPU information constants
     *
     ************************************
     */

    /* get_reg/set_reg constants */
    public static final int MAX_REGS = 128;/* maximum number of register of any CPU */

 /* This value is passed to cpu_get_reg to retrieve the previous
	 * program counter value, ie. before a CPU emulation started
	 * to fetch opcodes and arguments for the current instrution. */
    public static final int REG_PREVIOUSPC = -1;

    /* This value is passed to cpu_get_reg to retrieve the current
	 * program counter value. */
    public static final int REG_PC = -2;

    /* This value is passed to cpu_get_reg to retrieve the current
	 * stack pointer value. */
    public static final int REG_SP = -3;

    /* This value is passed to cpu_get_reg/cpu_set_reg, instead of one of
	 * the names from the enum a CPU core defines for it's registers,
	 * to get or set the contents of the memory pointed to by a stack pointer.
	 * You can specify the n'th element on the stack by (REG_SP_CONTENTS-n),
	 * ie. lower negative values. The actual element size (UINT16 or UINT32)
	 * depends on the CPU core. */
    public static final int REG_SP_CONTENTS = -4;


    /* endianness constants */
    public static final int CPU_IS_LE = 0;/* emulated CPU is little endian */
    public static final int CPU_IS_BE = 1;/* emulated CPU is big endian */

 /* Values passed to the cpu_info function of a core to retrieve information */
    public static final int CPU_INFO_REG = 0;
    public static final int CPU_INFO_FLAGS = MAX_REGS;
    public static final int CPU_INFO_NAME = MAX_REGS + 1;
    public static final int CPU_INFO_FAMILY = MAX_REGS + 2;
    public static final int CPU_INFO_VERSION = MAX_REGS + 3;
    public static final int CPU_INFO_FILE = MAX_REGS + 4;
    public static final int CPU_INFO_CREDITS = MAX_REGS + 5;
    public static final int CPU_INFO_REG_LAYOUT = MAX_REGS + 6;
    public static final int CPU_INFO_WIN_LAYOUT = MAX_REGS + 7;

    /**
     * ***********************************
     *
     * Core CPU interface structure
     *
     ************************************
     */
    public static abstract interface burnPtr {

        public abstract void handler(int cycles);
    }

    public static abstract interface irqcallbacksPtr {

        public abstract int handler(int irqline);
    }

    public static abstract class cpu_interface {

        /* index (used to make sure we mach the enum above */
        public int cpu_num;

        /* table of core functions */
        public abstract void init();

        public abstract void reset(Object param);

        public abstract void exit();

        public abstract int execute(int cycles);

        public burnPtr burn;

        public abstract Object init_context();//not in mame , used specific for arcadeflex

        public abstract Object get_context();//different from mame returns reg object and not size since java doesn't support references

        public abstract void set_context(Object reg);

        public abstract int[] get_cycle_table(int which);

        public abstract void set_cycle_table(int which, int[] new_table);

        public abstract int get_reg(int regnum);

        public abstract void set_reg(int regnum, int val);

        public abstract void set_irq_line(int irqline, int linestate);

        public abstract void set_irq_callback(irqcallbacksPtr callback);

        public abstract String cpu_info(Object context, int regnum);

        public abstract String cpu_dasm(String buffer, int pc);

        /* IRQ and clock information */
        public int/*unsigned*/ num_irqs;
        public int default_vector;
        public int[] icount;
        public double overclock;
        public int irq_int;

        /* memory information */
        public int databus_width;

        public abstract int memory_read(int offset);

        public abstract void memory_write(int offset, int data);

        public abstract int internal_read(int offset);

        public abstract void internal_write(int offset, int data);

        public int pgm_memory_base;

        public abstract void set_op_base(int pc);

        public int address_shift;
        public int/*unsigned*/ address_bits;
        public int/*unsigned*/ endianess;
        public int/*unsigned*/ align_unit;
        public int/*unsigned*/ max_inst_len;

        public abstract int mem_address_bits_of_cpu();//arcadeflex function (based on the above table)
        /*	{ 16, cpu_readmem16 },
    	{ 20, cpu_readmem20 },
    	{ 21, cpu_readmem21 },
    	{ 24, cpu_readmem24 },
    
    	{ 16, cpu_readmem16bew },
    	{ 24, cpu_readmem24bew },
    	{ 32, cpu_readmem32bew },
    
    	{ 16, cpu_readmem16lew },
    	{ 17, cpu_readmem17lew },
    	{ 24, cpu_readmem24lew },
    	{ 29, cpu_readmem29lew },
    	{ 32, cpu_readmem32lew },
    
    	{ 24, cpu_readmem24bedw },
    	{ 29, cpu_readmem29bedw },
    	{ 32, cpu_readmem32bedw },
    
    	{ 26, cpu_readmem26ledw },
    	{ 29, cpu_readmem29ledw },
    	{ 32, cpu_readmem32ledw },
    
    	{ 18, cpu_readmem18bedw }*/
    }

    /**
     * ***********************************
     *
     * Macros
     *
     ************************************
     */
    public static int activecpu_get_previouspc() {
        return activecpu_get_reg(REG_PREVIOUSPC);
    }

    public static int activecpu_get_pc() {
        return activecpu_get_reg(REG_PC);
    }

    /*TODO*///#define		activecpu_get_sp()			activecpu_get_reg(REG_SP)
/*TODO*///#define		activecpu_set_pc(val)		activecpu_set_reg(REG_PC, val)
/*TODO*///#define		activecpu_set_sp(val)		activecpu_set_reg(REG_SP, val)
/*TODO*///
/*TODO*///#define		cpunum_get_previouspc(cpu)	cpunum_get_reg(cpu, REG_PREVIOUSPC)
/*TODO*///#define		cpunum_get_pc(cpu)			cpunum_get_reg(cpu, REG_PC)
/*TODO*///#define		cpunum_get_sp(cpu)			cpunum_get_reg(cpu, REG_SP)
/*TODO*///#define		cpunum_set_pc(cpu, val)		cpunum_set_reg(cpu, REG_PC, val)
/*TODO*///#define		cpunum_set_sp(cpu, val)		cpunum_set_reg(cpu, REG_SP, val)

    /* this is kind of gross - is it necessary */
    public static int cpu_geturnpc(){
        return activecpu_get_reg(REG_SP_CONTENTS);
    }

    /* map older cpu_* functions to activecpu_* */
    public static int cpu_get_pc() {
        return activecpu_get_pc();
    }

    /*TODO*///#define		cpu_get_sp					activecpu_get_sp
    public static int cpu_get_reg(int regnum) {
        return activecpu_get_reg(regnum);
    }

    /*TODO*///#define		cpu_set_reg					activecpu_set_reg
    public static int cpu_getpreviouspc() {
        return activecpu_get_previouspc();
    }

    public static void cpu_set_op_base(int val) {
        activecpu_set_op_base(val);
    }

    public static int cpu_get_pc_byte() {
        return activecpu_get_pc_byte();
    }

    /**
     * ***********************************
     *
     * CPU interface accessors
     *
     ************************************
     */

    /* return a pointer to the interface struct for a given CPU type */
    public static cpu_interface cputype_get_interface(int cputype) {
        return cpuintrf[cputype];
    }

    /* return a the index of the active CPU */
    public static int cpu_getactivecpu() {
        return activecpu;
    }

    /* return a the total number of registered CPUs */
    public static int cpu_gettotalcpu() {
        return totalcpu;
    }
}
