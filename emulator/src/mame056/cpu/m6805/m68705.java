package mame056.cpu.m6805;

import static mame056.cpuintrfH.*;
import static mame056.memoryH.*;
import static mame056.cpuexec.*;
import static mame056.cpu.m6805.m6805H.*;
import static mame056.memory.*;

public class m68705 extends m6805 {

    public m68705() {
        cpu_num = CPU_M68705;
        num_irqs = 1;
        default_vector = 0;
        icount = m6805_ICount;
        overclock = 1.00;
        irq_int = M68705_IRQ_LINE;
        databus_width = 8;
        pgm_memory_base = 0;
        address_shift = 0;
        address_bits = 11;
        endianess = CPU_IS_BE;
        align_unit = 1;
        max_inst_len = 3;
        icount[0] = 50000;
       
    }

    @Override
    public void reset(Object param) {
        //int p_amask = (int)param;
	super.reset(param);
	/* Overide default 6805 type */
	m6805.subtype = SUBTYPE_M68705;
	if (param!=null)
		m6805.amask = (int)param;
	else
		m6805.amask  = 0x7ff; /* default if no AMASK is specified */
	RM16( m6805.amask , m6805.pc );
        
        super.reset(param);
        /* Overide default 6805 type */
        m6805.subtype = SUBTYPE_M68705;
    }

    @Override
    public String cpu_info(Object context, int regnum) {
        switch (regnum) {
            case CPU_INFO_NAME:
                return "M68705";
            case CPU_INFO_VERSION:
                return "1.1";
        }
        return super.cpu_info(context, regnum);
    }
}
