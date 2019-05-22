package mame056.cpu.m6805;

import static mame056.cpuintrfH.*;
import static mame056.memoryH.*;
import static mame056.cpuexec.*;
import static mame056.cpu.m6805.m6805H.*;
import static mame056.memory.*;

public class HD63705 extends m6805 {

    public HD63705() {
        cpu_num = CPU_HD63705;
        num_irqs = 8;
        default_vector = 0;
        icount = m6805_ICount;
        overclock = 1.00;
        irq_int = HD63705_INT_IRQ1;
        databus_width = 8;
        pgm_memory_base = 0;
        address_shift = 0;
        address_bits = 16;
        endianess = CPU_IS_BE;
        align_unit = 1;
        max_inst_len = 3;
        icount[0] = 50000;
    }

    @Override
    public void reset(Object param) {
        super.reset(param);
        /* Overide default 6805 type */
        m6805.subtype = SUBTYPE_HD63705;
        m6805.amask = 0xffff;
        m6805.sp_mask = 0x17f;
        m6805.sp_low = 0x100;
        RM16(0x1ffe, m6805.pc);
        m6805.s.SetD(0x17f);
    }

    @Override
    public void set_irq_line(int irqline, int state) {
        if (irqline == IRQ_LINE_NMI) {
            if (m6805.nmi_state == state) {
                return;
            }

            m6805.nmi_state = state;
            if (state != CLEAR_LINE) {
                m6805.u16_pending_interrupts |= 1 << HD63705_INT_NMI;
            }
        } else if (irqline <= HD63705_INT_ADCONV) {
            if (m6805.irq_state[irqline] == state) {
                return;
            }
            m6805.irq_state[irqline] = state;
            if (state != CLEAR_LINE) {
                m6805.u16_pending_interrupts |= 1 << irqline;
            }
        }
    }

    @Override
    public String cpu_info(Object context, int regnum) {
        /*TODO*///	static char buffer[8][47+1];
/*TODO*///	static int which = 0;
/*TODO*///	m6805_Regs *r = context;
/*TODO*///
/*TODO*///	which = ++which % 8;
/*TODO*///    buffer[which][0] = '\0';
/*TODO*///
/*TODO*///    if( !context )
/*TODO*///		r = &m6805;
/*TODO*///
        switch (regnum) {
            case CPU_INFO_NAME:
                return "HD63705";
            case CPU_INFO_VERSION:
                return "1.0";
            case CPU_INFO_CREDITS:
                return "Keith Wilkins, Juergen Buchmueller";
            /*TODO*///		case CPU_INFO_REG_LAYOUT: return (const char *)hd63705_reg_layout;
/*TODO*///		case CPU_INFO_WIN_LAYOUT: return (const char *)hd63705_win_layout;
/*TODO*///		case CPU_INFO_REG+HD63705_NMI_STATE: sprintf(buffer[which], "NMI:%X", r->nmi_state); return buffer[which];
/*TODO*///		case CPU_INFO_REG+HD63705_IRQ1_STATE: sprintf(buffer[which], "IRQ1:%X", r->irq_state[HD63705_INT_IRQ1]); return buffer[which];
/*TODO*///		case CPU_INFO_REG+HD63705_IRQ2_STATE: sprintf(buffer[which], "IRQ2:%X", r->irq_state[HD63705_INT_IRQ2]); return buffer[which];
/*TODO*///		case CPU_INFO_REG+HD63705_ADCONV_STATE: sprintf(buffer[which], "ADCONV:%X", r->irq_state[HD63705_INT_ADCONV]); return buffer[which];
        }
        return super.cpu_info(context, regnum);
    }
}
