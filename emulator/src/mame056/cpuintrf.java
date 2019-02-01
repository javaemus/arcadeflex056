/**
 * Ported to 0.56
 */
package mame056;

import static arcadeflex.libc.cstring.strcmp;
import static old.arcadeflex.osdepend.*;

import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.cpuexecH.*;
import static mame056.memory.*;
//cpu imports
import mame056.cpu.dummy_cpu;
import mame056.cpu.z80.z80;

public class cpuintrf {

    /*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Debug logging
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///#define VERBOSE 0
/*TODO*///
/*TODO*///#if VERBOSE
/*TODO*///#define LOG(x)	logerror x
/*TODO*///#else
/*TODO*///#define LOG(x)
/*TODO*///#endif
/*TODO*///
/*TODO*///
    /**
     * ***********************************
     *
     * Macros to help verify active CPU
     *
     ************************************
     */
    public static int VERIFY_ACTIVECPU(int retval, String name) {
        if (activecpu < 0) {
            logerror(name + "() called with no active cpu!\n");
            return retval;
        }
        return -1;//??
    }

    public static void VERIFY_ACTIVECPU_VOID(String name) {
        if (activecpu < 0) {
            logerror(name + "() called with no active cpu!\n");

        }
    }

    /**
     * ***********************************
     *
     * Macros to help verify CPU index
     *
     ************************************
     */
    public static int VERIFY_CPUNUM(int cpunum, int retval, String name) {
        if (cpunum < 0 || cpunum >= totalcpu) {
            logerror(name + "() called for invalid cpu num!\n");
            return retval;
        }
        return -1;//??
    }

    /*TODO*///
/*TODO*///#define VERIFY_CPUNUM_VOID(name)							
/*TODO*///	if (cpunum < 0 || cpunum >= totalcpu)					
/*TODO*///	{														
/*TODO*///		logerror(#name "() called for invalid cpu num!\n");	
/*TODO*///		return;												
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Internal CPU info type
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
    public static class cpuinfo {

        public cpu_interface intf;/* copy of the interface data */
        public int cputype;/* type index of this CPU */
        public int family;/* family index of this CPU */
        public Object context;/* dynamically allocated context buffer */
    }
    /*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Macros to generate CPU entries
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////* most CPUs use this macro */
/*TODO*///#define CPU0(cpu,name,nirq,dirq,oc,i1,datawidth,mem,shift,bits,endian,align,maxinst) \
/*TODO*///	{																			   \
/*TODO*///		CPU_##cpu,																   \
/*TODO*///		name##_init, name##_reset, name##_exit, name##_execute, NULL,			   \
/*TODO*///		name##_get_context, name##_set_context, NULL, NULL, 					   \
/*TODO*///		name##_get_reg, name##_set_reg,			   \
/*TODO*///		name##_set_irq_line, name##_set_irq_callback,		   \
/*TODO*///		name##_info, name##_dasm, 										   \
/*TODO*///		nirq, dirq, &name##_ICount, oc, i1, 							   \
/*TODO*///		datawidth,																   \
/*TODO*///		(mem_read_handler)cpu_readmem##mem, (mem_write_handler)cpu_writemem##mem, NULL, NULL,						   \
/*TODO*///		0, cpu_setopbase##mem,													   \
/*TODO*///		shift, bits, CPU_IS_##endian, align, maxinst							   \
/*TODO*///	}
/*TODO*///
/*TODO*////* CPUs which have the _burn function */
/*TODO*///#define CPU1(cpu,name,nirq,dirq,oc,i1,datawidth,mem,shift,bits,endian,align,maxinst)	 \
/*TODO*///	{																			   \
/*TODO*///		CPU_##cpu,																   \
/*TODO*///		name##_init, name##_reset, name##_exit, name##_execute, 				   \
/*TODO*///		name##_burn,															   \
/*TODO*///		name##_get_context, name##_set_context, 								   \
/*TODO*///		name##_get_cycle_table, name##_set_cycle_table, 						   \
/*TODO*///		name##_get_reg, name##_set_reg,			   \
/*TODO*///		name##_set_irq_line, name##_set_irq_callback,		   \
/*TODO*///		name##_info, name##_dasm, 										   \
/*TODO*///		nirq, dirq, &name##_ICount, oc, i1, 							   \
/*TODO*///		datawidth,																   \
/*TODO*///		(mem_read_handler)cpu_readmem##mem, (mem_write_handler)cpu_writemem##mem, NULL, NULL,						   \
/*TODO*///		0, cpu_setopbase##mem,													   \
/*TODO*///		shift, bits, CPU_IS_##endian, align, maxinst							   \
/*TODO*///	}
/*TODO*///
/*TODO*////* like CPU0, but CPU has Harvard-architecture like program/data memory */
/*TODO*///#define CPU3(cpu,name,nirq,dirq,oc,i1,datawidth,mem,shift,bits,endian,align,maxinst) \
/*TODO*///	{																			   \
/*TODO*///		CPU_##cpu,																   \
/*TODO*///		name##_init, name##_reset, name##_exit, name##_execute, NULL,			   \
/*TODO*///		name##_get_context, name##_set_context, NULL, NULL, 					   \
/*TODO*///		name##_get_reg, name##_set_reg,			   \
/*TODO*///		name##_set_irq_line, name##_set_irq_callback,		   \
/*TODO*///		name##_info, name##_dasm, 										   \
/*TODO*///		nirq, dirq, &name##_icount, oc, i1, 							   \
/*TODO*///		datawidth,																   \
/*TODO*///		(mem_read_handler)cpu_readmem##mem, (mem_write_handler)cpu_writemem##mem, NULL, NULL,						   \
/*TODO*///		cpu##_PGM_OFFSET, cpu_setopbase##mem,									   \
/*TODO*///		shift, bits, CPU_IS_##endian, align, maxinst							   \
/*TODO*///	}
/*TODO*///
/*TODO*////* like CPU0, but CPU has internal memory (or I/O ports, timers or similiar) */
/*TODO*///#define CPU4(cpu,name,nirq,dirq,oc,i1,datawidth,mem,shift,bits,endian,align,maxinst) \
/*TODO*///	{																			   \
/*TODO*///		CPU_##cpu,																   \
/*TODO*///		name##_init, name##_reset, name##_exit, name##_execute, NULL,			   \
/*TODO*///		name##_get_context, name##_set_context, NULL, NULL, 					   \
/*TODO*///		name##_get_reg, name##_set_reg,			   \
/*TODO*///		name##_set_irq_line, name##_set_irq_callback,		   \
/*TODO*///		name##_info, name##_dasm, 										   \
/*TODO*///		nirq, dirq, &name##_icount, oc, i1, 							   \
/*TODO*///		datawidth,																   \
/*TODO*///		(mem_read_handler)cpu_readmem##mem, (mem_write_handler)cpu_writemem##mem, name##_internal_r, name##_internal_w, \
/*TODO*///		0, cpu_setopbase##mem,													   \
/*TODO*///		shift, bits, CPU_IS_##endian, align, maxinst							   \
/*TODO*///	}

    /**
     * ***********************************
     *
     * The core list of CPU interfaces
     *
     ************************************
     */
    public static cpu_interface cpuintrf[]
            = {
                new dummy_cpu(),
                new z80()
            /*TODO*///#if (HAS_8080)
            /*TODO*///	CPU0(8080,	   i8080,	 4,255,1.00,I8080_INTR_LINE,8, 16,	  0,16,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_8085A)
            /*TODO*///	CPU0(8085A,    i8085,	 4,255,1.00,I8085_INTR_LINE,8, 16,	  0,16,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M6502)
            /*TODO*///	CPU0(M6502,    m6502,	 1,  0,1.00,M6502_IRQ_LINE, 8, 16,	  0,16,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M65C02)
            /*TODO*///	CPU0(M65C02,   m65c02,	 1,  0,1.00,M65C02_IRQ_LINE, 8, 16,	  0,16,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M65SC02)
            /*TODO*///	CPU0(M65SC02,  m65sc02,  1,  0,1.00,M65SC02_IRQ_LINE, 8, 16,	  0,16,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M65CE02)
            /*TODO*///	CPU0(M65CE02,  m65ce02,  1,  0,1.00,M65CE02_IRQ_LINE, 8, 16,	  0,16,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M6509)
            /*TODO*///	CPU0(M6509,    m6509,	 1,  0,1.00,M6509_IRQ_LINE, 8, 20,	  0,20,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M6510)
            /*TODO*///	CPU0(M6510,    m6510,	 1,  0,1.00,M6510_IRQ_LINE, 8, 16,	  0,16,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M6510T)
            /*TODO*///	CPU0(M6510T,   m6510t,	 1,  0,1.00,M6510T_IRQ_LINE, 8, 16,	  0,16,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M7501)
            /*TODO*///	CPU0(M7501,    m7501,	 1,  0,1.00,M7501_IRQ_LINE, 8, 16,	  0,16,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M8502)
            /*TODO*///	CPU0(M8502,    m8502,	 1,  0,1.00,M8502_IRQ_LINE, 8, 16,	  0,16,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_N2A03)
            /*TODO*///	CPU0(N2A03,    n2a03,	 1,  0,1.00,N2A03_IRQ_LINE, 8, 16,	  0,16,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M4510)
            /*TODO*///	CPU0(M4510,    m4510,	 1,  0,1.00,M4510_IRQ_LINE, 8, 20,	  0,20,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_H6280)
            /*TODO*///	CPU0(H6280,    h6280,	 3,  0,1.00,-1,			    8, 21,	  0,21,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_I86)
            /*TODO*///	CPU0(I86,	   i86, 	 1,  0,1.00,-1000,		    8, 20,	  0,20,LE,1, 5	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_I88)
            /*TODO*///	CPU0(I88,	   i88, 	 1,  0,1.00,-1000,		    8, 20,	  0,20,LE,1, 5	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_I186)
            /*TODO*///	CPU0(I186,	   i186,	 1,  0,1.00,-1000,		    8, 20,	  0,20,LE,1, 5	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_I188)
            /*TODO*///	CPU0(I188,	   i188,	 1,  0,1.00,-1000,		    8, 20,	  0,20,LE,1, 5	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_I286)
            /*TODO*///	CPU0(I286,	   i286,	 1,  0,1.00,-1000,		    8, 24,	  0,24,LE,1, 5	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_V20)
            /*TODO*///	CPU0(V20,	   v20, 	 1,  0,1.00,-1000,		    8, 20,	  0,20,LE,1, 5	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_V30)
            /*TODO*///	CPU0(V30,	   v30, 	 1,  0,1.00,-1000,		    8, 20,	  0,20,LE,1, 5	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_V33)
            /*TODO*///	CPU0(V33,	   v33, 	 1,  0,1.00,-1000,		    8, 20,	  0,20,LE,1, 5	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_V60)
            /*TODO*///	CPU0(V60,	   v60, 	 1,  0,1.00,-1000,		   16, 24lew, 0,24,LE,1, 11	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_I8035)
            /*TODO*///	CPU0(I8035,    i8035,	 1,  0,1.00,0,              8, 16,	  0,16,LE,1, 2	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_I8039)
            /*TODO*///	CPU0(I8039,    i8039,	 1,  0,1.00,0,              8, 16,	  0,16,LE,1, 2	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_I8048)
            /*TODO*///	CPU0(I8048,    i8048,	 1,  0,1.00,0,              8, 16,	  0,16,LE,1, 2	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_N7751)
            /*TODO*///	CPU0(N7751,    n7751,	 1,  0,1.00,0,              8, 16,	  0,16,LE,1, 2	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_I8X41)
            /*TODO*///	CPU0(I8X41,    i8x41,	 1,  0,1.00,I8X41_INT_IBF,  8, 16,	  0,16,LE,1, 2	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M6800)
            /*TODO*///	CPU0(M6800,    m6800,	 1,  0,1.00,M6800_IRQ_LINE, 8, 16,	  0,16,BE,1, 4	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M6801)
            /*TODO*///	CPU0(M6801,    m6801,	 1,  0,1.00,M6801_IRQ_LINE, 8, 16,	  0,16,BE,1, 4	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M6802)
            /*TODO*///	CPU0(M6802,    m6802,	 1,  0,1.00,M6802_IRQ_LINE, 8, 16,	  0,16,BE,1, 4	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M6803)
            /*TODO*///	CPU0(M6803,    m6803,	 1,  0,1.00,M6803_IRQ_LINE, 8, 16,	  0,16,BE,1, 4	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M6808)
            /*TODO*///	CPU0(M6808,    m6808,	 1,  0,1.00,M6808_IRQ_LINE, 8, 16,	  0,16,BE,1, 4	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_HD63701)
            /*TODO*///	CPU0(HD63701,  hd63701,  1,  0,1.00,HD63701_IRQ_LINE,8, 16,	  0,16,BE,1, 4	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_NSC8105)
            /*TODO*///	CPU0(NSC8105,  nsc8105,  1,  0,1.00,NSC8105_IRQ_LINE,8, 16,	  0,16,BE,1, 4	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M6805)
            /*TODO*///	CPU0(M6805,    m6805,	 1,  0,1.00,M6805_IRQ_LINE,  8, 16,	  0,11,BE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M68705)
            /*TODO*///	CPU0(M68705,   m68705,	 1,  0,1.00,M68705_IRQ_LINE, 8, 16,	  0,11,BE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_HD63705)
            /*TODO*///	CPU0(HD63705,  hd63705,  8,  0,1.00,HD63705_INT_IRQ1,8, 16,	  0,16,BE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_HD6309)
            /*TODO*///	CPU0(HD6309,   hd6309,	 2,  0,1.00,HD6309_IRQ_LINE, 8, 16,	  0,16,BE,1, 4	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M6809)
            /*TODO*///	CPU0(M6809,    m6809,	 2,  0,1.00,M6809_IRQ_LINE,  8, 16,	  0,16,BE,1, 4	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_KONAMI)
            /*TODO*///	CPU0(KONAMI,   konami,	 2,  0,1.00,KONAMI_IRQ_LINE, 8, 16,	  0,16,BE,1, 4	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M68000)
            /*TODO*///	CPU0(M68000,   m68000,	 8, -1,1.00,-1,			   16,24bew,  0,24,BE,2,10	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M68010)
            /*TODO*///	CPU0(M68010,   m68010,	 8, -1,1.00,-1,			   16,24bew,  0,24,BE,2,10	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M68EC020)
            /*TODO*///	CPU0(M68EC020, m68ec020, 8, -1,1.00,-1,			   32,24bedw, 0,24,BE,4,10	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_M68020)
            /*TODO*///	CPU0(M68020,   m68020,	 8, -1,1.00,-1, 		   32,32bedw, 0,32,BE,4,10	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_T11)
            /*TODO*///	CPU0(T11,	   t11, 	 4,  0,1.00,-1,			   16,16lew,  0,16,LE,2, 6	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_S2650)
            /*TODO*///	CPU0(S2650,    s2650,	 2,  0,1.00,-1,			    8, 16,	  0,15,LE,1, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_TMS34010)
            /*TODO*///	CPU0(TMS34010, tms34010, 2,  0,1.00,0,             16,29lew,  3,29,LE,2,10	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_TMS34020)
            /*TODO*///	CPU0(TMS34020, tms34020, 2,  0,1.00,0,             16,29lew,  3,29,LE,2,10	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_TMS9900)
            /*TODO*///	CPU0(TMS9900,  tms9900,  1,  0,1.00,-1,			   16,16bew,  0,16,BE,2, 6	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_TMS9940)
            /*TODO*///	CPU0(TMS9940,  tms9940,  1,  0,1.00,-1,			   16,16bew,  0,16,BE,2, 6	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_TMS9980)
            /*TODO*///	CPU0(TMS9980,  tms9980a, 1,  0,1.00,-1,			    8, 16,	  0,16,BE,1, 6	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_TMS9985)
            /*TODO*///	CPU0(TMS9985,  tms9985,  1,  0,1.00,-1,			    8, 16,	  0,16,BE,1, 6	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_TMS9989)
            /*TODO*///	CPU0(TMS9989,  tms9989,  1,  0,1.00,-1,			    8, 16,	  0,16,BE,1, 6	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_TMS9995)
            /*TODO*///	CPU0(TMS9995,  tms9995,  1,  0,1.00,-1,			    8, 16,	  0,16,BE,1, 6	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_TMS99105A)
            /*TODO*///	CPU0(TMS99105A,tms99105a,1,  0,1.00,-1,			   16,16bew,  0,16,BE,2, 6	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_TMS99110A)
            /*TODO*///	CPU0(TMS99110A,tms99110a,1,  0,1.00,-1,			   16,16bew,  0,16,BE,2, 6	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_Z8000)
            /*TODO*///	CPU0(Z8000,    z8000,	 2,  0,1.00,0,        	   16,16bew,  0,16,BE,2, 6	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_TMS320C10)
            /*TODO*///	CPU3(TMS320C10,tms320c10,2,  0,1.00,-1,			   16,16bew, -1,16,BE,2, 4	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_CCPU)
            /*TODO*///	CPU3(CCPU,	   ccpu,	 2,  0,1.00,-1,			   16,16bew,  0,15,BE,2, 3	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_ADSP2100)
            /*TODO*///	CPU3(ADSP2100, adsp2100, 4,  0,1.00,-1,			   16,17lew, -1,14,LE,2, 4	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_ADSP2105)
            /*TODO*///	CPU3(ADSP2105, adsp2105, 4,  0,1.00,-1,			   16,17lew, -1,14,LE,2, 4	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_PSXCPU)
            /*TODO*///	CPU0(PSXCPU,   mips,	 8, -1,1.00,0,             16,32lew,  0,32,LE,4, 4	),
            /*TODO*///#endif
            /*TODO*///#if (HAS_ASAP)
            /*TODO*///	#define asap_ICount asap_icount
            /*TODO*///	CPU0(ASAP,	   asap,	 1,  0,1.00,-1,			   32,32ledw, 0,32,LE,4, 12 ),
            /*TODO*///#endif
            /*TODO*///#if (HAS_UPD7810)
            /*TODO*///#define upd7810_ICount upd7810_icount
            /*TODO*///	CPU0(UPD7810,  upd7810,  2,  0,1.00,UPD7810_INTF1,  8, 16,	  0,16,LE,1, 4	),
            };
    /*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Default debugger window layout
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///UINT8 default_win_layout[] =
/*TODO*///{
/*TODO*///	 0, 0,80, 5,	/* register window (top rows) */
/*TODO*///	 0, 5,24,17,	/* disassembler window (left, middle columns) */
/*TODO*///	25, 5,55, 8,	/* memory #1 window (right, upper middle) */
/*TODO*///	25,14,55, 8,	/* memory #2 window (right, lower middle) */
/*TODO*///	 0,23,80, 1 	/* command line window (bottom row) */
/*TODO*///};
/*TODO*///
/*TODO*///
    /**
     * ***********************************
     *
     * Other variables we own
     *
     ************************************
     */
    static int activecpu;/* index of active CPU (or -1) */
    public static int totalcpu;/* total number of CPUs */

    static cpuinfo[] cpu = new cpuinfo[MAX_CPU];

    static int[] cpu_active_context = new int[CPU_COUNT];
    static int[] cpu_context_stack = new int[4];
    static int cpu_context_stack_ptr;

    /**
     * ***********************************
     *
     * Set a new CPU context
     *
     ************************************
     */
    public static void set_cpu_context(int cpunum) {
        int newfamily = cpu[cpunum].family;
        int oldcontext = cpu_active_context[newfamily];

        /* if we need to change contexts, save the one that was there */
        if (oldcontext != cpunum && oldcontext != -1) {
            cpu[oldcontext].context = cpu[oldcontext].intf.get_context();
        }

        /* swap memory spaces */
        activecpu = cpunum;
        memory_set_context(cpunum);

        /* if the new CPU's context is not swapped in, do it now */
        if (oldcontext != cpunum) {
            cpu[cpunum].intf.set_context(cpu[cpunum].context);
            cpu_active_context[newfamily] = cpunum;
        }
    }

    /**
     * ***********************************
     *
     * Push/pop to a new CPU context
     *
     ************************************
     */
    public static void cpuintrf_push_context(int cpunum) {
        /* push the old context onto the stack */
        cpu_context_stack[cpu_context_stack_ptr++] = activecpu;

        /* do the rest only if this isn't the activecpu */
        if (cpunum != activecpu && cpunum != -1) {
            set_cpu_context(cpunum);
        }

        /* this is now the active CPU */
        activecpu = cpunum;
    }

    public static void cpuintrf_pop_context() {
        /* push the old context onto the stack */
        int cpunum = cpu_context_stack[--cpu_context_stack_ptr];

        /* do the rest only if this isn't the activecpu */
        if (cpunum != activecpu && cpunum != -1) {
            set_cpu_context(cpunum);
        }

        /* this is now the active CPU */
        activecpu = cpunum;
    }

    /**
     * ***********************************
     *
     * Initialize a single CPU
     *
     ************************************
     */
    public static int cpuintrf_init() {
        int cputype;

        /* verify the order of entries in the cpuintrf[] array */
        for (cputype = 0; cputype < CPU_COUNT; cputype++) {
            /*TODO*///		/* make sure the index in the array matches the current index */
/*TODO*///		if (cpuintrf[cputype].cpu_num != cputype)
/*TODO*///		{
/*TODO*///			printf("CPU #%d [%s] wrong ID %d: check enum CPU_... in src/cpuintrf.h!\n", cputype, cputype_name(cputype), cpuintrf[cputype].cpu_num);
/*TODO*///			exit(1);
/*TODO*///		}

            /* also reset the active CPU context info */
            cpu_active_context[cputype] = -1;
        }

        /* zap the CPU data structure */
        for (int i = 0; i < MAX_CPU; i++) {
            cpu[i] = new cpuinfo();//memset(cpu, 0, sizeof(cpu));
        }
        totalcpu = 0;

        /* reset the context stack */
        for (int i = 0; i < 4; i++) {
            cpu_context_stack[i] = -1;//memset(cpu_context_stack, -1, sizeof(cpu_context_stack));
        }
        cpu_context_stack_ptr = 0;

        return 0;
    }

    /**
     * ***********************************
     *
     * Initialize a single CPU
     *
     ************************************
     */
    public static int cpuintrf_init_cpu(int cpunum, int cputype) {
        String familyname;
        int j, size;

        /* fill in the type and interface */
        cpu[cpunum].intf = cpuintrf[cputype];
        cpu[cpunum].cputype = cputype;

        /* determine the family index */
        familyname = cputype_core_file(cputype);
        for (j = 0; j < CPU_COUNT; j++) {
            if (strcmp(familyname, cputype_core_file(j)) == 0) {
                cpu[cpunum].family = j;
                break;
            }
        }

        cpu[cpunum].context = cpu[cpunum].intf.init_context();

        /* initialize the CPU and stash the context */
        activecpu = cpunum;
        cpu[cpunum].intf.init();

        activecpu = -1;

        /* clear out the registered CPU for this family */
        cpu_active_context[cpu[cpunum].family] = -1;

        /* make sure the total includes us */
        totalcpu = cpunum + 1;

        return 0;
    }

    /*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Exit/free a single CPU
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpuintrf_exit_cpu(int cpunum)
/*TODO*///{
/*TODO*///	/* if the CPU core defines an exit function, call it now */
/*TODO*///	if (cpu[cpunum].intf.exit)
/*TODO*///		(*cpu[cpunum].intf.exit)();
/*TODO*///
/*TODO*///	/* free the context buffer for that CPU */
/*TODO*///	if (cpu[cpunum].context)
/*TODO*///		free(cpu[cpunum].context);
/*TODO*///	cpu[cpunum].context = NULL;
/*TODO*///}
/*TODO*///
/*TODO*///
    /**
     * ***********************************
     *
     * Convert old-style interrupt number to an IRQ line
     *
     ************************************
     */
    public static int convert_type_to_irq_line(int cpunum, int num, int[] vector) {
        int irqline = num;

        /* default vector is the num */
        vector[0] = num;

        switch (cpu[cpunum].cputype) {
            case CPU_Z80:
                irqline = 0;
                break;
            /*TODO*///#if (HAS_I86)
/*TODO*///		case CPU_I86:				irqline = 0; break;
/*TODO*///#endif
/*TODO*///#if (HAS_I88)
/*TODO*///		case CPU_I88:				irqline = 0; break;
/*TODO*///#endif
/*TODO*///#if (HAS_I186)
/*TODO*///		case CPU_I186:				irqline = 0; break;
/*TODO*///#endif
/*TODO*///#if (HAS_I188)
/*TODO*///		case CPU_I188:				irqline = 0; break;
/*TODO*///#endif
/*TODO*///#if (HAS_I286)
/*TODO*///		case CPU_I286:				irqline = 0; break;
/*TODO*///#endif
/*TODO*///#if (HAS_V20)
/*TODO*///		case CPU_V20:				irqline = 0; break;
/*TODO*///#endif
/*TODO*///#if (HAS_V30)
/*TODO*///		case CPU_V30:				irqline = 0; break;
/*TODO*///#endif
/*TODO*///#if (HAS_V33)
/*TODO*///		case CPU_V33:				irqline = 0; break;
/*TODO*///#endif
/*TODO*///#if (HAS_8080)
/*TODO*///		case CPU_8080:				irqline = 0; if (*vector == 0) *vector = 0xff; break;
/*TODO*///#endif
/*TODO*///#if (HAS_S2650)
/*TODO*///		case CPU_S2650:				irqline = 0; break;
/*TODO*///#endif
/*TODO*///#if (HAS_M68000)
/*TODO*///		case CPU_M68000:			*vector = MC68000_INT_ACK_AUTOVECTOR; break;
/*TODO*///#endif
/*TODO*///#if (HAS_M68010)
/*TODO*///		case CPU_M68010:			*vector = MC68000_INT_ACK_AUTOVECTOR; break;
/*TODO*///#endif
/*TODO*///#if (HAS_M68EC020)
/*TODO*///		case CPU_M68EC020:			*vector = MC68000_INT_ACK_AUTOVECTOR; break;
/*TODO*///#endif
/*TODO*///#if (HAS_M68020)
/*TODO*///		case CPU_M68020:			*vector = MC68000_INT_ACK_AUTOVECTOR; break;
/*TODO*///#endif
        }
        return irqline;
    }

    /*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Interfaces to the active CPU
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////*--------------------------
/*TODO*/// 	Adjust/get icount
/*TODO*///--------------------------*/
/*TODO*///
/*TODO*///void activecpu_adjust_icount(int delta)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU_VOID(activecpu_adjust_icount);
/*TODO*///	*cpu[activecpu].intf.icount += delta;
/*TODO*///}
/*TODO*///
    public static int activecpu_get_icount() {
        if (activecpu < 0) {
            logerror("activecpu_get_icount() called with no active cpu!\n");
            return 0;
        }
        return cpu[activecpu].intf.icount[0];
    }

    /*TODO*///
/*TODO*////*--------------------------
/*TODO*/// 	Reset banking pointers
/*TODO*///--------------------------*/
/*TODO*///
/*TODO*///void activecpu_reset_banking(void)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU_VOID(activecpu_reset_banking);
/*TODO*///	(*cpu[activecpu].intf.set_op_base)(activecpu_get_pc_byte());
/*TODO*///}
/*TODO*///

    /*--------------------------
            IRQ line setting
    --------------------------*/
    public static void activecpu_set_irq_line(int irqline, int state) {
        if (activecpu < 0) {
            logerror("activecpu_set_irq_line() called with no active cpu!\n");

        }
        if (state != INTERNAL_CLEAR_LINE && state != INTERNAL_ASSERT_LINE) {
            logerror("activecpu_set_irq_line called when cpu_set_irq_line should have been used!\n");
            return;
        }
        cpu[activecpu].intf.set_irq_line(irqline, state - INTERNAL_CLEAR_LINE);
    }

    /*TODO*///
/*TODO*///
/*TODO*////*--------------------------
/*TODO*/// 	Get/set cycle table
/*TODO*///--------------------------*/
/*TODO*///
/*TODO*///void *activecpu_get_cycle_table(int which)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU(NULL, activecpu_get_cycle_table);
/*TODO*///	return (*cpu[activecpu].intf.get_cycle_table)(which);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///void activecpu_set_cycle_tbl(int which, void *new_table)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU_VOID(activecpu_set_cycle_tbl);
/*TODO*///	(*cpu[activecpu].intf.set_cycle_table)(which, new_table);
/*TODO*///}
    /*--------------------------
 	Get/set registers
    --------------------------*/
    public static int activecpu_get_reg(int regnum) {
        if (activecpu < 0) {
            logerror("activecpu_get_reg() called with no active cpu!\n");
            return 0;
        }
        return cpu[activecpu].intf.get_reg(regnum);
    }

    /*TODO*///
/*TODO*///void activecpu_set_reg(int regnum, unsigned val)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU_VOID(activecpu_set_reg);
/*TODO*///	(*cpu[activecpu].intf.set_reg)(regnum, val);
/*TODO*///}
/*TODO*///

    /*--------------------------
 	Get/set PC
    --------------------------*/
    public static int activecpu_get_pc_byte() {
        int base, pc;
        int shift;
        if (activecpu < 0) {
            logerror("activecpu_get_pc_byte() called with no active cpu!\n");
            return 0;
        }
        shift = cpu[activecpu].intf.address_shift;
        base = cpu[activecpu].intf.pgm_memory_base;
        pc = cpu[activecpu].intf.get_reg(REG_PC);
        if (shift != 0) {
            throw new UnsupportedOperationException("to be checked!");//z80 doesn't have shift so we can't check , leave that for future to not be forgotten
        }
        return base + ((shift < 0) ? (pc << -shift) : (pc >>> shift));
    }

    public static void activecpu_set_op_base(int val) {
        if (activecpu < 0) {
            logerror("activecpu_set_op_base() called with no active cpu!\n");

        }
        cpu[activecpu].intf.set_op_base(val);
    }

    /*TODO*///
/*TODO*///
/*TODO*////*--------------------------
/*TODO*/// 	Disassembly
/*TODO*///--------------------------*/
/*TODO*///
/*TODO*///unsigned activecpu_dasm(char *buffer, unsigned pc)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU(1, activecpu_dasm);
/*TODO*///	return (*cpu[activecpu].intf.cpu_dasm)(buffer, pc);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*--------------------------
/*TODO*/// 	Register dumps
/*TODO*///--------------------------*/
/*TODO*///
/*TODO*///const char *activecpu_flags(void)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU("", activecpu_flags);
/*TODO*///	return (*cpu[activecpu].intf.cpu_info)(NULL, CPU_INFO_FLAGS);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///const char *activecpu_dump_reg(int regnum)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU("", activecpu_dump_reg);
/*TODO*///	return (*cpu[activecpu].intf.cpu_info)(NULL, CPU_INFO_REG + regnum);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*--------------------------
/*TODO*/// 	State dumps
/*TODO*///--------------------------*/
/*TODO*///
/*TODO*///const char *activecpu_dump_state(void)
/*TODO*///{
/*TODO*///	static char buffer[1024+1];
/*TODO*///	unsigned addr_width = (activecpu_address_bits() + 3) / 4;
/*TODO*///	char *dst = buffer;
/*TODO*///	const char *src;
/*TODO*///	const INT8 *regs;
/*TODO*///	int width;
/*TODO*///
/*TODO*///	VERIFY_ACTIVECPU("", activecpu_dump_state);
/*TODO*///
/*TODO*///	dst += sprintf(dst, "CPU #%d [%s]\n", activecpu, activecpu_name());
/*TODO*///	width = 0;
/*TODO*///	regs = (INT8 *)activecpu_reg_layout();
/*TODO*///	while (*regs)
/*TODO*///	{
/*TODO*///		if (*regs == -1)
/*TODO*///		{
/*TODO*///			dst += sprintf(dst, "\n");
/*TODO*///			width = 0;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			src = activecpu_dump_reg(*regs);
/*TODO*///			if (*src)
/*TODO*///			{
/*TODO*///				if (width + strlen(src) + 1 >= 80)
/*TODO*///				{
/*TODO*///					dst += sprintf(dst, "\n");
/*TODO*///					width = 0;
/*TODO*///				}
/*TODO*///				dst += sprintf(dst, "%s ", src);
/*TODO*///				width += strlen(src) + 1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		regs++;
/*TODO*///	}
/*TODO*///	dst += sprintf(dst, "\n%0*X: ", addr_width, activecpu_get_pc());
/*TODO*///	activecpu_dasm(dst, activecpu_get_pc());
/*TODO*///	strcat(dst, "\n\n");
/*TODO*///
/*TODO*///	return buffer;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*--------------------------
/*TODO*/// 	Get/set static info
/*TODO*///--------------------------*/
/*TODO*///
/*TODO*///#define CPU_FUNC(rettype, name, defresult, result)			\
/*TODO*///rettype name(void)	 										\
/*TODO*///{ 															\
/*TODO*///	VERIFY_ACTIVECPU(defresult, name)						\
/*TODO*///	return result;											\
/*TODO*///}
    public static int activecpu_default_irq_line() {
        if (activecpu < 0) {
            logerror("activecpu_default_irq_line() called with no active cpu!\n");
            return 0;
        }
        return cpu[activecpu].intf.irq_int;
    }

    /*TODO*///CPU_FUNC(int,          activecpu_default_irq_vector, 0,  cpu[activecpu].intf.default_vector)
    public static int activecpu_address_bits() {
        if (activecpu < 0) {
            logerror("activecpu_address_bits() called with no active cpu!\n");
            return 0;
        }
        return cpu[activecpu].intf.address_bits;
    }

    /*TODO*///CPU_FUNC(unsigned,     activecpu_address_mask,       0,  0xffffffffUL >> (32 - cpu[activecpu].intf.address_bits))
/*TODO*///CPU_FUNC(int,          activecpu_address_shift,      0,  cpu[activecpu].intf.address_shift)
/*TODO*///CPU_FUNC(unsigned,     activecpu_endianess,          0,  cpu[activecpu].intf.endianess)
/*TODO*///CPU_FUNC(unsigned,     activecpu_databus_width,      0,  cpu[activecpu].intf.databus_width)
/*TODO*///CPU_FUNC(unsigned,     activecpu_align_unit,         0,  cpu[activecpu].intf.align_unit)
/*TODO*///CPU_FUNC(unsigned,     activecpu_max_inst_len,       0,  cpu[activecpu].intf.max_inst_len)
/*TODO*///CPU_FUNC(const char *, activecpu_name,               "", (*cpu[activecpu].intf.cpu_info)(NULL, CPU_INFO_NAME))
/*TODO*///CPU_FUNC(const char *, activecpu_core_family,        "", (*cpu[activecpu].intf.cpu_info)(NULL, CPU_INFO_FAMILY))
/*TODO*///CPU_FUNC(const char *, activecpu_core_version,       "", (*cpu[activecpu].intf.cpu_info)(NULL, CPU_INFO_VERSION))
/*TODO*///CPU_FUNC(const char *, activecpu_core_file,          "", (*cpu[activecpu].intf.cpu_info)(NULL, CPU_INFO_FILE))
/*TODO*///CPU_FUNC(const char *, activecpu_core_credits,       "", (*cpu[activecpu].intf.cpu_info)(NULL, CPU_INFO_CREDITS))
/*TODO*///CPU_FUNC(const char *, activecpu_reg_layout,         "", (*cpu[activecpu].intf.cpu_info)(NULL, CPU_INFO_REG_LAYOUT))
/*TODO*///CPU_FUNC(const char *, activecpu_win_layout,         "", (*cpu[activecpu].intf.cpu_info)(NULL, CPU_INFO_WIN_LAYOUT))
/*TODO*///
/*TODO*///
    /**
     * ***********************************
     *
     * Interfaces to a specific CPU
     *
     ************************************
     */

    /*--------------------------
            Execute
    --------------------------*/
    public static int cpunum_execute(int cpunum, int cycles) {
        int ran;
        if (cpunum < 0 || cpunum >= totalcpu) {
            logerror("cpunum_execute() called for invalid cpu num!\n");
            return 0;
        }
        cpuintrf_push_context(cpunum);
        cpu[cpunum].intf.set_op_base(activecpu_get_pc_byte());
        ran = cpu[cpunum].intf.execute(cycles);
        cpuintrf_pop_context();
        return ran;
    }

    /*--------------------------
 	Reset and set IRQ ack
    --------------------------*/
    public static void cpunum_reset(int cpunum, Object param, irqcallbacksPtr irqack) {
        if (cpunum < 0 || cpunum >= totalcpu) {
            logerror("cpunum_reset() called for invalid cpu num!\n");
            return;
        }
        cpuintrf_push_context(cpunum);
        cpu[cpunum].intf.reset(param);
        if (irqack != null) {
            cpu[cpunum].intf.set_irq_callback(irqack);
        }
        cpuintrf_pop_context();
    }

    /*--------------------------
            Read a byte
    --------------------------*/
    public static int /*data8_t*/ cpunum_read_byte(int cpunum, int address) {
        int result;
        if (cpunum < 0 || cpunum >= totalcpu) {
            logerror("cpunum_read_byte() called for invalid cpu num!\n");
            return 0;
        }
        cpuintrf_push_context(cpunum);
        result = cpu[cpunum].intf.memory_read(address);
        cpuintrf_pop_context();
        return result & 0xFF;
    }


    /*--------------------------
 	Write a byte
    --------------------------*/
    public static void cpunum_write_byte(int cpunum, int address, int/*data8_t*/ data) {
        if (cpunum < 0 || cpunum >= totalcpu) {
            logerror("cpunum_write_byte() called for invalid cpu num!\n");
            return;
        }
        cpuintrf_push_context(cpunum);
        cpu[cpunum].intf.memory_write(address, data & 0xFF);
        cpuintrf_pop_context();
    }

    /*TODO*///
/*TODO*////*--------------------------
/*TODO*/// 	Get context pointer
/*TODO*///--------------------------*/
/*TODO*///
/*TODO*///void *cpunum_get_context_ptr(int cpunum)
/*TODO*///{
/*TODO*///	VERIFY_CPUNUM(NULL, cpunum_get_context_ptr);
/*TODO*///	return (cpu_active_context[cpu[cpunum].family] == cpunum) ? NULL : cpu[cpunum].context;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*--------------------------
/*TODO*/// 	Get/set cycle table
/*TODO*///--------------------------*/
/*TODO*///
/*TODO*///void *cpunum_get_cycle_table(int cpunum, int which)
/*TODO*///{
/*TODO*///	void *result;
/*TODO*///	VERIFY_CPUNUM(NULL, cpunum_get_cycle_table);
/*TODO*///	cpuintrf_push_context(cpunum);
/*TODO*///	result = (*cpu[cpunum].intf.get_cycle_table)(which);
/*TODO*///	cpuintrf_pop_context();
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///void cpunum_set_cycle_tbl(int cpunum, int which, void *new_table)
/*TODO*///{
/*TODO*///	VERIFY_CPUNUM_VOID(cpunum_set_cycle_tbl);
/*TODO*///	cpuintrf_push_context(cpunum);
/*TODO*///	(*cpu[cpunum].intf.set_cycle_table)(which, new_table);
/*TODO*///	cpuintrf_pop_context();
/*TODO*///}
    /*--------------------------
            Get/set registers
    --------------------------*/
    public static int cpunum_get_reg(int cpunum, int regnum) {
        if (cpunum < 0 || cpunum >= totalcpu) {
            logerror("cpunum_get_reg() called for invalid cpu num!\n");
            return 0;
        }
        int/*unsigned*/ result;
        cpuintrf_push_context(cpunum);
        result = cpu[cpunum].intf.get_reg(regnum);
        cpuintrf_pop_context();
        return result;
    }

    /*TODO*///
/*TODO*///
/*TODO*///void cpunum_set_reg(int cpunum, int regnum, unsigned val)
/*TODO*///{
/*TODO*///	VERIFY_CPUNUM_VOID(cpunum_set_reg);
/*TODO*///	cpuintrf_push_context(cpunum);
/*TODO*///	(*cpu[cpunum].intf.set_reg)(regnum, val);
/*TODO*///	cpuintrf_pop_context();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*--------------------------
/*TODO*/// 	Get/set PC
/*TODO*///--------------------------*/
/*TODO*///
/*TODO*///offs_t cpunum_get_pc_byte(int cpunum)
/*TODO*///{
/*TODO*///	offs_t base, pc;
/*TODO*///	int shift;
/*TODO*///
/*TODO*///	VERIFY_CPUNUM(0, cpunum_get_pc_byte);
/*TODO*///	shift = cpu[cpunum].intf.address_shift;
/*TODO*///	base = cpu[cpunum].intf.pgm_memory_base;
/*TODO*///	cpuintrf_push_context(cpunum);
/*TODO*///	pc = (*cpu[cpunum].intf.get_reg)(REG_PC);
/*TODO*///	cpuintrf_pop_context();
/*TODO*///	return base + ((shift < 0) ? (pc << -shift) : (pc >> shift));
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///void cpunum_set_op_base(int cpunum, unsigned val)
/*TODO*///{
/*TODO*///	VERIFY_CPUNUM_VOID(cpunum_set_op_base);
/*TODO*///	cpuintrf_push_context(cpunum);
/*TODO*///	(*cpu[cpunum].intf.set_op_base)(val);
/*TODO*///	cpuintrf_pop_context();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*--------------------------
/*TODO*/// 	Disassembly
/*TODO*///--------------------------*/
/*TODO*///
/*TODO*///unsigned cpunum_dasm(int cpunum, char *buffer, unsigned pc)
/*TODO*///{
/*TODO*///	unsigned result;
/*TODO*///	VERIFY_CPUNUM(1, cpunum_dasm);
/*TODO*///	cpuintrf_push_context(cpunum);
/*TODO*///	result = (*cpu[cpunum].intf.cpu_dasm)(buffer, pc);
/*TODO*///	cpuintrf_pop_context();
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*--------------------------
/*TODO*/// 	Register dumps
/*TODO*///--------------------------*/
/*TODO*///
/*TODO*///const char *cpunum_flags(int cpunum)
/*TODO*///{
/*TODO*///	const char *result;
/*TODO*///	VERIFY_CPUNUM("", cpunum_flags);
/*TODO*///	cpuintrf_push_context(cpunum);
/*TODO*///	result = (*cpu[cpunum].intf.cpu_info)(NULL, CPU_INFO_FLAGS);
/*TODO*///	cpuintrf_pop_context();
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///const char *cpunum_dump_reg(int cpunum, int regnum)
/*TODO*///{
/*TODO*///	const char *result;
/*TODO*///	VERIFY_CPUNUM("", cpunum_dump_reg);
/*TODO*///	cpuintrf_push_context(cpunum);
/*TODO*///	result = (*cpu[cpunum].intf.cpu_info)(NULL, CPU_INFO_REG + regnum);
/*TODO*///	cpuintrf_pop_context();
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*--------------------------
/*TODO*/// 	State dumps
/*TODO*///--------------------------*/
/*TODO*///
/*TODO*///const char *cpunum_dump_state(int cpunum)
/*TODO*///{
/*TODO*///	static char buffer[1024+1];
/*TODO*///	VERIFY_CPUNUM("", cpunum_dump_state);
/*TODO*///	cpuintrf_push_context(cpunum);
/*TODO*///	strcpy(buffer, activecpu_dump_state());
/*TODO*///	cpuintrf_pop_context();
/*TODO*///	return buffer;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*--------------------------
/*TODO*/// 	Get/set static info
/*TODO*///--------------------------*/
/*TODO*///
/*TODO*///#define CPUNUM_FUNC(rettype, name, defresult, result)		\
/*TODO*///rettype name(int cpunum)									\
/*TODO*///{ 															\
/*TODO*///	VERIFY_CPUNUM(defresult, name)							\
/*TODO*///	return result;											\
/*TODO*///}
/*TODO*///
/*TODO*///CPUNUM_FUNC(int,          cpunum_default_irq_line,   0,  cpu[cpunum].intf.irq_int)
    public static int cpunum_default_irq_vector(int cpunum) {
        if (cpunum < 0 || cpunum >= totalcpu) {
            logerror("cpunum_default_irq_vector() called for invalid cpu num!\n");
            return 0;
        }
        return cpu[cpunum].intf.default_vector;
    }

    /*TODO*///CPUNUM_FUNC(unsigned,     cpunum_address_bits,       0,  cpu[cpunum].intf.address_bits)
    public static int cpunum_address_mask(int cpunum) {
        if (cpunum < 0 || cpunum >= totalcpu) {
            logerror("cpunum_address_mask() called for invalid cpu num!\n");
            return 0;
        }
        return (0xffffffff >>> (32 - cpu[cpunum].intf.address_bits));
    }

    /*TODO*///CPUNUM_FUNC(int,          cpunum_address_shift,      0,  cpu[cpunum].intf.address_shift)
    /*TODO*///CPUNUM_FUNC(unsigned,     cpunum_endianess,          0,  cpu[cpunum].intf.endianess)
    public static int cpunum_databus_width(int cpunum) {
        if (cpunum < 0 || cpunum >= totalcpu) {
            logerror("cpunum_databus_width() called for invalid cpu num!\n");
            return 0;
        }
        return cpu[cpunum].intf.databus_width;
    }

    /*TODO*///CPUNUM_FUNC(unsigned,     cpunum_align_unit,         0,  cpu[cpunum].intf.align_unit)
/*TODO*///CPUNUM_FUNC(unsigned,     cpunum_max_inst_len,       0,  cpu[cpunum].intf.max_inst_len)
/*TODO*///CPUNUM_FUNC(const char *, cpunum_name,               "", (*cpu[cpunum].intf.cpu_info)(NULL, CPU_INFO_NAME))
/*TODO*///CPUNUM_FUNC(const char *, cpunum_core_family,        "", (*cpu[cpunum].intf.cpu_info)(NULL, CPU_INFO_FAMILY))
/*TODO*///CPUNUM_FUNC(const char *, cpunum_core_version,       "", (*cpu[cpunum].intf.cpu_info)(NULL, CPU_INFO_VERSION))
/*TODO*///CPUNUM_FUNC(const char *, cpunum_core_file,          "", (*cpu[cpunum].intf.cpu_info)(NULL, CPU_INFO_FILE))
/*TODO*///CPUNUM_FUNC(const char *, cpunum_core_credits,       "", (*cpu[cpunum].intf.cpu_info)(NULL, CPU_INFO_CREDITS))
/*TODO*///CPUNUM_FUNC(const char *, cpunum_reg_layout,         "", (*cpu[cpunum].intf.cpu_info)(NULL, CPU_INFO_REG_LAYOUT))
/*TODO*///CPUNUM_FUNC(const char *, cpunum_win_layout,         "", (*cpu[cpunum].intf.cpu_info)(NULL, CPU_INFO_WIN_LAYOUT))
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Static info about a type of CPU
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///#define CPUTYPE_FUNC(rettype, name, defresult, result)		\
/*TODO*///rettype name(int cputype)									\
/*TODO*///{ 															\
/*TODO*///	cputype &= ~CPU_FLAGS_MASK;								\
/*TODO*///	if (cputype >= 0 && cputype < CPU_COUNT)				\
/*TODO*///		return result;										\
/*TODO*///	else													\
/*TODO*///		logerror(#name "() called with invalid cpu type!\n");\
/*TODO*///	return defresult;										\
/*TODO*///}
/*TODO*///
/*TODO*///CPUTYPE_FUNC(int,          cputype_default_irq_line,   0,  cpuintrf[cputype].irq_int)
/*TODO*///CPUTYPE_FUNC(int,          cputype_default_irq_vector, 0,  cpuintrf[cputype].default_vector)
/*TODO*///CPUTYPE_FUNC(unsigned,     cputype_address_bits,       0,  cpuintrf[cputype].address_bits)
/*TODO*///CPUTYPE_FUNC(unsigned,     cputype_address_mask,       0,  0xffffffffUL >> (32 - cpuintrf[cputype].address_bits))
/*TODO*///CPUTYPE_FUNC(int,          cputype_address_shift,      0,  cpuintrf[cputype].address_shift)
    public static int cputype_endianess(int cputype) {
        cputype &= ~CPU_FLAGS_MASK;
        if (cputype >= 0 && cputype < CPU_COUNT) {
            return cpuintrf[cputype].endianess;
        } else {
            logerror("cputype_endianess() called with invalid cpu type!\n");
        }
        return 0;
    }

    public static int cputype_databus_width(int cputype) {
        cputype &= ~CPU_FLAGS_MASK;
        if (cputype >= 0 && cputype < CPU_COUNT) {
            return cpuintrf[cputype].databus_width;
        } else {
            logerror("cputype_databus_width() called with invalid cpu type!\n");
        }
        return 0;
    }

    /*TODO*///CPUTYPE_FUNC(unsigned,     cputype_align_unit,         0,  cpuintrf[cputype].align_unit)
/*TODO*///CPUTYPE_FUNC(unsigned,     cputype_max_inst_len,       0,  cpuintrf[cputype].max_inst_len)
    public static String cputype_name(int cputype) {
        cputype &= ~CPU_FLAGS_MASK;
        if (cputype >= 0 && cputype < CPU_COUNT) {
            return cpuintrf[cputype].cpu_info(null, CPU_INFO_NAME);
        } else {
            logerror("cputype_name() called with invalid cpu type!\n");
        }
        return "";
    }

    /*TODO*///CPUTYPE_FUNC(const char *, cputype_core_family,        "", (*cpuintrf[cputype].cpu_info)(NULL, CPU_INFO_FAMILY))
/*TODO*///CPUTYPE_FUNC(const char *, cputype_core_version,       "", (*cpuintrf[cputype].cpu_info)(NULL, CPU_INFO_VERSION))
    public static String cputype_core_file(int cputype) {
        cputype &= ~CPU_FLAGS_MASK;
        if (cputype >= 0 && cputype < CPU_COUNT) {
            return cpuintrf[cputype].cpu_info(null, CPU_INFO_FILE);
        } else {
            logerror("cputype_core_file() called with invalid cpu type!\n");
        }
        return "";
    }
    /*TODO*///CPUTYPE_FUNC(const char *, cputype_core_credits,       "", (*cpuintrf[cputype].cpu_info)(NULL, CPU_INFO_CREDITS))
/*TODO*///CPUTYPE_FUNC(const char *, cputype_reg_layout,         "", (*cpuintrf[cputype].cpu_info)(NULL, CPU_INFO_REG_LAYOUT))
/*TODO*///CPUTYPE_FUNC(const char *, cputype_win_layout,         "", (*cpuintrf[cputype].cpu_info)(NULL, CPU_INFO_WIN_LAYOUT))
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Dump states of all CPUs
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_dump_states(void)
/*TODO*///{
/*TODO*///	int cpunum;
/*TODO*///
/*TODO*///	for (cpunum = 0; cpunum < totalcpu; cpunum++)
/*TODO*///		puts(cpunum_dump_state(cpunum));
/*TODO*///	fflush(stdout);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	68000 reset kludge
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///#if (HAS_M68000 || HAS_M68010 || HAS_M68020 || HAS_M68EC020)
/*TODO*///void cpu_set_m68k_reset(int cpunum, void (*resetfn)(void))
/*TODO*///{
/*TODO*///	void m68k_set_reset_instr_callback(void (*callback)(void));
/*TODO*///
/*TODO*///	if ( 1
/*TODO*///#if (HAS_M68000)
/*TODO*///		&& cpu[cpunum].cputype != CPU_M68000
/*TODO*///#endif
/*TODO*///#if (HAS_M68010)
/*TODO*///		&& cpu[cpunum].cputype != CPU_M68010
/*TODO*///#endif
/*TODO*///#if (HAS_M68020)
/*TODO*///		&& cpu[cpunum].cputype != CPU_M68020
/*TODO*///#endif
/*TODO*///#if (HAS_M68EC020)
/*TODO*///		&& cpu[cpunum].cputype != CPU_M68EC020
/*TODO*///#endif
/*TODO*///		)
/*TODO*///	{
/*TODO*///		logerror("Trying to set m68k reset vector on non-68k cpu\n");
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	cpuintrf_push_context(cpunum);
/*TODO*///	m68k_set_reset_instr_callback(resetfn);
/*TODO*///	cpuintrf_pop_context();
/*TODO*///}
/*TODO*///#endif
/*TODO*///    
}
