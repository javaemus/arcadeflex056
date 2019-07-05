/**
 * Ported to 0.56
 */
package mame056;

import arcadeflex056.fucPtr.*;

import static mame056.cpuexec.*;
import static mame056.cpuintrfH.*;

import static mame056.cpu.m6502.m6502H.*;
import static mame056.cpu.m6809.m6809H.*;
import static mame056.cpu.i8085.i8085H.*;

public class cpuexecH {

    /**
     * *************************************************************************
     *
     * cpuexec.h
     *
     * Core multi-CPU execution engine.
     *
     **************************************************************************
     */
    /**
     * ***********************************
     *
     * CPU description for drivers
     *
     ************************************
     */
    public static class MachineCPU {

        public MachineCPU(int cpu_type, int cpu_clock, Object memory_read, Object memory_write, Object port_read, Object port_write, InterruptPtr vblank_interrupt, int vblank_interrupts_per_frame, InterruptPtr timed_interrupt, int timed_interrupts_per_second, Object reset_param) {
            this.cpu_type = cpu_type;
            this.cpu_clock = cpu_clock;
            this.memory_read = memory_read;
            this.memory_write = memory_write;
            this.port_read = port_read;
            this.port_write = port_write;
            this.vblank_interrupt = vblank_interrupt;
            this.vblank_interrupts_per_frame = vblank_interrupts_per_frame;
            this.timed_interrupt = timed_interrupt;
            this.timed_interrupts_per_second = timed_interrupts_per_second;
            this.reset_param = reset_param;
        }

        public MachineCPU(int cpu_type, int cpu_clock, Object memory_read, Object memory_write, Object port_read, Object port_write, InterruptPtr vblank_interrupt, int vblank_interrupts_per_frame, InterruptPtr timed_interrupt, int timed_interrupts_per_second) {
            this.cpu_type = cpu_type;
            this.cpu_clock = cpu_clock;
            this.memory_read = memory_read;
            this.memory_write = memory_write;
            this.port_read = port_read;
            this.port_write = port_write;
            this.vblank_interrupt = vblank_interrupt;
            this.vblank_interrupts_per_frame = vblank_interrupts_per_frame;
            this.timed_interrupt = timed_interrupt;
            this.timed_interrupts_per_second = timed_interrupts_per_second;
            this.reset_param = null;
        }

        public MachineCPU(int cpu_type, int cpu_clock, Object memory_read, Object memory_write, Object port_read, Object port_write, InterruptPtr vblank_interrupt, int vblank_interrupts_per_frame) {
            this.cpu_type = cpu_type;
            this.cpu_clock = cpu_clock;
            this.memory_read = memory_read;
            this.memory_write = memory_write;
            this.port_read = port_read;
            this.port_write = port_write;
            this.vblank_interrupt = vblank_interrupt;
            this.vblank_interrupts_per_frame = vblank_interrupts_per_frame;
            this.timed_interrupt = null;
            this.timed_interrupts_per_second = 0;
            this.reset_param = null;
        }

        public MachineCPU() {
            this(0, 0, null, null, null, null, null, 0, null, 0, null);
        }

        public static MachineCPU[] create(int n) {
            MachineCPU[] a = new MachineCPU[n];
            for (int k = 0; k < n; k++) {
                a[k] = new MachineCPU();
            }
            return a;
        }
        public int cpu_type;/* see #defines below. */
        public int cpu_clock;/* in Hertz */
        public Object memory_read;/* struct Memory_ReadAddress */
        public Object memory_write;/* struct Memory_WriteAddress */
        public Object port_read;
        public Object port_write;
        public InterruptPtr vblank_interrupt;/* for interrupts tied to VBLANK */
        public int vblank_interrupts_per_frame;/* usually 1 */
        public InterruptPtr timed_interrupt;/* for interrupts not tied to VBLANK */
        public int timed_interrupts_per_second;
        public Object reset_param;/* parameter for cpu_reset */

    }

    /**
     * ***********************************
     *
     * CPU flag constants
     *
     ************************************
     */

    /* flags for CPU go into upper byte */
    public static final int CPU_FLAGS_MASK = 0xff00;

    /* set this if the CPU is used as a slave for audio. It will not be emulated if sound is disabled, therefore speeding up a lot the emulation. */
    public static final int CPU_AUDIO_CPU = 0x8000;

    /* the Z80 can be wired to use 16 bit addressing for I/O ports */
    public static final int CPU_16BIT_PORT = 0x4000;

    /**
     * ***********************************
     *
     * Interrupt constants
     *
     ************************************
     */

    /* generic "none" vector */
    public static final int INTERRUPT_NONE = 126;

    /* generic NMI vector */
    public static final int INTERRUPT_NMI = IRQ_LINE_NMI;

    /**
     * ***********************************
     *
     * Save/restore
     *
     ************************************
     */

    /* Load or save the game state */
    public static final int LOADSAVE_NONE = 0;
    public static final int LOADSAVE_SAVE = 1;
    public static final int LOADSAVE_LOAD = 2;

    /**
     * ***********************************
     *
     * Interrupt handling
     *
     ************************************
     */
    /* macro for handling NMI lines */
    public static void cpu_set_nmi_line(int cpunum, int state) {
        cpu_set_irq_line(cpunum, IRQ_LINE_NMI, state);
    }

    /*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Obsolete interrupt handling
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////* OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE */
/*TODO*////* OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE */
/*TODO*////* OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE */
/*TODO*///
/*TODO*////* Obsolete functions: avoid to use them in new drivers if possible. */
/*TODO*///
/*TODO*///void cpu_cause_interrupt(int cpu,int type);
/*TODO*///void cpu_interrupt_enable(int cpu,int enabled);
/*TODO*///WRITE_HANDLER( interrupt_enable_w );
/*TODO*///WRITE_HANDLER( interrupt_vector_w );
/*TODO*///int interrupt(void);
/*TODO*///int nmi_interrupt(void);
/*TODO*///int ignore_interrupt(void);
/*TODO*///#if (HAS_M68000 || HAS_M68010 || HAS_M68020 || HAS_M68EC020)
/*TODO*///int m68_level1_irq(void);
/*TODO*///int m68_level2_irq(void);
/*TODO*///int m68_level3_irq(void);
/*TODO*///int m68_level4_irq(void);
/*TODO*///int m68_level5_irq(void);
/*TODO*///int m68_level6_irq(void);
/*TODO*///int m68_level7_irq(void);
/*TODO*///#endif
/*TODO*///
/* defines for backward compatibility */
    public static final int Z80_NMI_INT = INTERRUPT_NMI;
    public static final int Z80_IRQ_INT = -1000;

    public static final int M6502_INT_IRQ = M6502_IRQ_LINE;
    public static final int M6502_INT_NMI = INTERRUPT_NMI;
    public static final int M6809_INT_IRQ = M6809_IRQ_LINE;
    public static final int M6809_INT_FIRQ = M6809_FIRQ_LINE;
    public static final int M6809_INT_NMI = INTERRUPT_NMI;

    /*TODO*///#define HD6309_INT_IRQ		HD6309_IRQ_LINE
/*TODO*///#define HD6309_INT_FIRQ		HD6309_FIRQ_LINE
/*TODO*///#define HD63705_INT_IRQ		HD63705_INT_IRQ1
/*TODO*///#define M68705_INT_IRQ		M68705_IRQ_LINE
/*TODO*///#define KONAMI_INT_IRQ		KONAMI_IRQ_LINE
/*TODO*///#define KONAMI_INT_FIRQ		KONAMI_FIRQ_LINE
    public static final int I8035_EXT_INT   = 0;
/*TODO*///#define I8039_EXT_INT		0
/*TODO*///#define H6280_INT_IRQ1		0
/*TODO*///#define H6280_INT_IRQ2		1
/*TODO*///#define H6280_INT_NMI		INTERRUPT_NMI
/*TODO*///#define HD63701_INT_NMI 	INTERRUPT_NMI
    public static final int I8085_RST75 = I8085_RST75_LINE;
/*TODO*///
/*TODO*////* OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE */
/*TODO*////* OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE */
/*TODO*////* OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE */
/*TODO*///
    /**
     * ***********************************
     *
     * Z80 daisy chain
     *
     ************************************
     */

    /* daisy-chain link */
    public static abstract interface Interrupt_entryPtr {

        public abstract int handler(int i);
    }

    public static abstract interface ResetPtr {

        public abstract void handler(int i);
    }

    public static abstract interface Interrupt_retiPtr {

        public abstract void handler(int i);
    }

    /* daisy-chain link */
    public static class Z80_DaisyChain {

        public ResetPtr reset;/* reset callback     */
        public Interrupt_entryPtr interrupt_entry;/* entry callback     */
        public Interrupt_retiPtr interrupt_reti;/* reti callback      */
        public int irq_param;

        /* callback paramater */
        public Z80_DaisyChain(ResetPtr reset, Interrupt_entryPtr interrupt_entry, Interrupt_retiPtr interrupt_reti, int irq_param) {
            this.reset = reset;
            this.interrupt_entry = interrupt_entry;
            this.interrupt_reti = interrupt_reti;
            this.irq_param = irq_param;
        }
    }

    public static final int Z80_MAXDAISY = 4;/* maximum of daisy chan device */

    public static final int Z80_INT_REQ = 0x01;/* interrupt request mask       */
    public static final int Z80_INT_IEO = 0x02;/* interrupt disable mask(IEO)  */

    public static int Z80_VECTOR(int device, int state) {
        return (((device) << 8) & 0xFF | (state) & 0xFF);
    }
}
