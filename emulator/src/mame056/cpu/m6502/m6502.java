/**
 * ported to v0.56
 */
package mame056.cpu.m6502;

import static mame056.cpu.m6502.m6502H.*;
import static mame056.cpu.m6502.ops02H.*;
import static mame056.cpu.m6502.t6502.*;
import static mame056.cpuintrfH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;

public class m6502 extends cpu_interface {

    public static int[] m6502_ICount = new int[1];

    public m6502() {
        cpu_num = CPU_M6502;
        num_irqs = 1;
        default_vector = 0;
        icount = m6502_ICount;
        overclock = 1.00;
        irq_int = M6502_IRQ_LINE;
        databus_width = 8;
        pgm_memory_base = 0;
        address_shift = 0;
        address_bits = 16;
        endianess = CPU_IS_LE;
        align_unit = 1;
        max_inst_len = 3;
    }

    public static final int M6502_NMI_VEC = 0xfffa;
    public static final int M6502_RST_VEC = 0xfffc;
    public static final int M6502_IRQ_VEC = 0xfffe;

    /**
     * **************************************************************************
     * The 6502 registers.
     * **************************************************************************
     */
    public static class PAIR {
        //L = low 8 bits
        //H = high 8 bits
        //D = whole 16 bits

        public int H, L, D;

        public void SetH(int val) {
            H = val & 0xFF;
            D = ((H << 8) | L) & 0xFFFF;
        }

        public void SetL(int val) {
            L = val & 0xFF;
            D = ((H << 8) | L) & 0xFFFF;
        }

        public void SetD(int val) {
            D = val & 0xFFFF;
            H = D >> 8 & 0xFF;
            L = D & 0xFF;
        }

        public void AddH(int val) {
            H = (H + val) & 0xFF;
            D = ((H << 8) | L) & 0xFFFF;
        }

        public void AddL(int val) {
            L = (L + val) & 0xFF;
            D = ((H << 8) | L) & 0xFFFF;
        }

        public void AddD(int val) {
            D = (D + val) & 0xFFFF;
            H = D >> 8 & 0xFF;
            L = D & 0xFF;
        }
    };

    public static class m6502_Regs {

        //int subtype;/* currently selected cpu sub type */
        //void	(**insn)(void); /* pointer to the function pointer table */
        PAIR ppc = new PAIR();/* previous program counter */
        PAIR pc = new PAIR();/* program counter */
        PAIR sp = new PAIR();/* stack pointer (always 100 - 1FF) */
        PAIR zp = new PAIR();/* zero page address */
        PAIR ea = new PAIR();/* effective address */
        int u8_a;/* Accumulator */
        int u8_x;/* X index register */
        int u8_y;/* Y index register */
        int u8_p;/* Processor status */
        int u8_pending_irq;/* nonzero if an IRQ is pending */
        int u8_after_cli;/* pending IRQ and last insn cleared I */
        int u8_nmi_state;
        int u8_irq_state;
        int u8_so_state;
        public irqcallbacksPtr irq_callback;/* IRQ callback */
    }
    public static m6502_Regs m6502 = new m6502_Regs();

    @Override
    public void init() {
        //m6502_state_register("m6502");
    }

    @Override
    public void reset(Object param) {
        /* wipe out the rest of the m6502 structure */
 /* read the reset vector into PC */
        m6502.pc.SetL(RDMEM(M6502_RST_VEC));
        m6502.pc.SetH(RDMEM(M6502_RST_VEC + 1));
        m6502.sp.SetD(0x01ff);/* stack pointer starts at page 1 offset FF */
        m6502.u8_p = F_T | F_I | F_Z | F_B | (m6502.u8_p & F_D);/* set T, I and Z flags */
        m6502.u8_pending_irq = 0;/* nonzero if an IRQ is pending */
        m6502.u8_after_cli = 0;/* pending IRQ and last insn cleared I */
        m6502.irq_callback = null;

        change_pc16(m6502.pc.D);
    }

    @Override
    public void exit() {
        //nothing
    }

    public static void m6502_take_irq() {
        if ((m6502.u8_p & F_I) == 0) {
            m6502.ea.SetD(M6502_IRQ_VEC);
            m6502_ICount[0] -= 7;
            PUSH(m6502.pc.H);
            PUSH(m6502.pc.L);
            PUSH(m6502.u8_p & ~F_B);
            m6502.u8_p = (m6502.u8_p | F_I) & 0xFF;/* set I flag */
            m6502.pc.SetL(RDMEM(m6502.ea.D));
            m6502.ea.AddD(1);
            m6502.pc.SetH(RDMEM(m6502.ea.D));
            //logerror("M6502#%d takes IRQ ($%04x)\n", cpu_getactivecpu(), m6502.pc.D);
            /* call back the cpuintrf to let it clear the line */
            if (m6502.irq_callback != null) {
                (m6502.irq_callback).handler(0);
            }
            change_pc16(m6502.pc.D);
        }
        m6502.u8_pending_irq = 0;
    }

    @Override
    public int execute(int cycles) {
        m6502_ICount[0] = cycles;

        change_pc16(m6502.pc.D);

        do {
            int/*UINT8*/ op;
            m6502.ppc.SetD(m6502.pc.D);

            /* if an irq is pending, take it now */
            if (m6502.u8_pending_irq != 0) {
                m6502_take_irq();
            }

            op = RDOP();
            insn6502[op].handler();

            /* check if the I flag was just reset (interrupts enabled) */
            if (m6502.u8_after_cli != 0) {
                //LOG(("M6502#%d after_cli was >0", cpu_getactivecpu()));
                m6502.u8_after_cli = 0;
                if (m6502.u8_irq_state != CLEAR_LINE) {
                    //LOG((": irq line is asserted: set pending IRQ\n"));
                    m6502.u8_pending_irq = 1;
                } else {
                    //LOG((": irq line is clear\n"));
                }
            } else if (m6502.u8_pending_irq != 0) {
                m6502_take_irq();
            }

        } while (m6502_ICount[0] > 0);

        return cycles - m6502_ICount[0];
    }

    @Override
    public Object get_context() {
        m6502_Regs regs = new m6502_Regs();
        regs.ppc.SetD(m6502.ppc.D);
        regs.pc.SetD(m6502.pc.D);
        regs.zp.SetD(m6502.zp.D);
        regs.sp.SetD(m6502.sp.D);
        regs.ea.SetD(m6502.ea.D);
        regs.u8_a = m6502.u8_a;
        regs.u8_x = m6502.u8_x;
        regs.u8_y = m6502.u8_y;
        regs.u8_p = m6502.u8_p;
        regs.u8_pending_irq = m6502.u8_pending_irq;
        regs.u8_after_cli = m6502.u8_after_cli;
        regs.u8_nmi_state = m6502.u8_nmi_state;
        regs.u8_irq_state = m6502.u8_irq_state;
        regs.u8_so_state = m6502.u8_so_state;
        regs.irq_callback = m6502.irq_callback;
        return regs;
    }

    @Override
    public void set_context(Object reg) {
        m6502_Regs regs = (m6502_Regs) reg;
        m6502.ppc.SetD(regs.ppc.D);
        m6502.pc.SetD(regs.pc.D);
        m6502.zp.SetD(regs.zp.D);
        m6502.sp.SetD(regs.sp.D);
        m6502.ea.SetD(regs.ea.D);
        m6502.u8_a = regs.u8_a;
        m6502.u8_x = regs.u8_x;
        m6502.u8_y = regs.u8_y;
        m6502.u8_p = regs.u8_p;
        m6502.u8_pending_irq = regs.u8_pending_irq;
        m6502.u8_after_cli = regs.u8_after_cli;
        m6502.u8_nmi_state = regs.u8_nmi_state;
        m6502.u8_irq_state = regs.u8_irq_state;
        m6502.u8_so_state = regs.u8_so_state;
        m6502.irq_callback = regs.irq_callback;

        change_pc16(m6502.pc.D);
    }

    @Override
    public int get_reg(int regnum) {
        switch (regnum) {
            case REG_PC:
                return m6502.pc.D;
            /*TODO*///		case M6502_PC: return m6502.pc.w.l;
/*TODO*///		case REG_SP: return S;
/*TODO*///		case M6502_S: return m6502.sp.b.l;
/*TODO*///		case M6502_P: return m6502.p;
/*TODO*///		case M6502_A: return m6502.a;
/*TODO*///		case M6502_X: return m6502.x;
/*TODO*///		case M6502_Y: return m6502.y;
/*TODO*///		case M6502_EA: return m6502.ea.w.l;
/*TODO*///		case M6502_ZP: return m6502.zp.w.l;
/*TODO*///		case M6502_NMI_STATE: return m6502.nmi_state;
/*TODO*///		case M6502_IRQ_STATE: return m6502.irq_state;
/*TODO*///		case M6502_SO_STATE: return m6502.so_state;
/*TODO*///		case M6502_SUBTYPE: return m6502.subtype;
            case REG_PREVIOUSPC:
                return m6502.ppc.D;
            /*TODO*///		default:
/*TODO*///			if( regnum <= REG_SP_CONTENTS )
/*TODO*///			{
/*TODO*///				unsigned offset = S + 2 * (REG_SP_CONTENTS - regnum);
/*TODO*///				if( offset < 0x1ff )
/*TODO*///					return RDMEM( offset ) | ( RDMEM( offset + 1 ) << 8 );
/*TODO*///			}
        }
        /*TODO*///	return 0;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_reg(int regnum, int val) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_irq_line(int irqline, int state) {
        if (irqline == IRQ_LINE_NMI) {
            if (m6502.u8_nmi_state == state) {
                return;
            }
            m6502.u8_nmi_state = state;
            if (state != CLEAR_LINE) {
                //LOG(( "M6502#%d set_nmi_line(ASSERT)\n", cpu_getactivecpu()));
                m6502.ea.SetD(M6502_NMI_VEC);
                m6502_ICount[0] -= 7;
                PUSH(m6502.pc.H);
                PUSH(m6502.pc.L);
                PUSH(m6502.u8_p & ~F_B);
                m6502.u8_p = (m6502.u8_p | F_I) & 0xFF;/* set I flag */
                m6502.pc.SetL(RDMEM(m6502.ea.D));
                m6502.ea.AddD(1);
                m6502.pc.SetH(RDMEM(m6502.ea.D));
                //LOG(("M6502#%d takes NMI ($%04x)\n", cpu_getactivecpu(), PCD));
                change_pc16(m6502.pc.D);
            }
        } else {
            if (irqline == M6502_SET_OVERFLOW) {
                if (m6502.u8_so_state != 0 && state == 0) {
                    //LOG(( "M6502#%d set overflow\n", cpu_getactivecpu()));
                    m6502.u8_p = (m6502.u8_p | F_V) & 0xFF;
                }
                m6502.u8_so_state = state;
                return;
            }
            m6502.u8_irq_state = state;
            if (state != CLEAR_LINE) {
                //LOG(( "M6502#%d set_irq_line(ASSERT)\n", cpu_getactivecpu()));
                m6502.u8_pending_irq = 1;
            }
        }
    }

    @Override
    public void set_irq_callback(irqcallbacksPtr callback) {
        m6502.irq_callback = callback;
    }

    @Override
    public String cpu_info(Object context, int regnum) {
        /*TODO*///	static char buffer[16][47+1];
/*TODO*///	static int which = 0;
/*TODO*///	m6502_Regs *r = context;
/*TODO*///
/*TODO*///	which = (which+1) % 16;
/*TODO*///	buffer[which][0] = '\0';
/*TODO*///	if( !context )
/*TODO*///		r = &m6502;
/*TODO*///
        switch (regnum) {
            /*TODO*///		case CPU_INFO_REG+M6502_PC: sprintf(buffer[which], "PC:%04X", r->pc.w.l); break;
/*TODO*///		case CPU_INFO_REG+M6502_S: sprintf(buffer[which], "S:%02X", r->sp.b.l); break;
/*TODO*///		case CPU_INFO_REG+M6502_P: sprintf(buffer[which], "P:%02X", r->p); break;
/*TODO*///		case CPU_INFO_REG+M6502_A: sprintf(buffer[which], "A:%02X", r->a); break;
/*TODO*///		case CPU_INFO_REG+M6502_X: sprintf(buffer[which], "X:%02X", r->x); break;
/*TODO*///		case CPU_INFO_REG+M6502_Y: sprintf(buffer[which], "Y:%02X", r->y); break;
/*TODO*///		case CPU_INFO_REG+M6502_EA: sprintf(buffer[which], "EA:%04X", r->ea.w.l); break;
/*TODO*///		case CPU_INFO_REG+M6502_ZP: sprintf(buffer[which], "ZP:%03X", r->zp.w.l); break;
/*TODO*///		case CPU_INFO_REG+M6502_NMI_STATE: sprintf(buffer[which], "NMI:%X", r->nmi_state); break;
/*TODO*///		case CPU_INFO_REG+M6502_IRQ_STATE: sprintf(buffer[which], "IRQ:%X", r->irq_state); break;
/*TODO*///		case CPU_INFO_REG+M6502_SO_STATE: sprintf(buffer[which], "SO:%X", r->so_state); break;
/*TODO*///		case CPU_INFO_FLAGS:
/*TODO*///			sprintf(buffer[which], "%c%c%c%c%c%c%c%c",
/*TODO*///				r->p & 0x80 ? 'N':'.',
/*TODO*///				r->p & 0x40 ? 'V':'.',
/*TODO*///				r->p & 0x20 ? 'R':'.',
/*TODO*///				r->p & 0x10 ? 'B':'.',
/*TODO*///				r->p & 0x08 ? 'D':'.',
/*TODO*///				r->p & 0x04 ? 'I':'.',
/*TODO*///				r->p & 0x02 ? 'Z':'.',
/*TODO*///				r->p & 0x01 ? 'C':'.');
/*TODO*///			break;
            case CPU_INFO_NAME:
                return "M6502";
            case CPU_INFO_FAMILY:
                return "Motorola 6502";
            case CPU_INFO_VERSION:
                return "1.2";
            case CPU_INFO_FILE:
                return "m6502.java";
            case CPU_INFO_CREDITS:
                return "Copyright (c) 1998 Juergen Buchmueller, all rights reserved.";
            /*TODO*///		case CPU_INFO_REG_LAYOUT: return (const char*)m6502_reg_layout;
/*TODO*///		case CPU_INFO_WIN_LAYOUT: return (const char*)m6502_win_layout;
        }
        /*TODO*///	return buffer[which];
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String cpu_dasm(String buffer, int pc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates./*TODO*///#ifdef MAME_DEBUG
/*TODO*///	return Dasm6502( buffer, pc );
/*TODO*///#else
/*TODO*///	sprintf( buffer, "$%02X", cpu_readop(pc) );
/*TODO*///	return 1;
/*TODO*///#endif
    }

    /**
     * Not needed functions
     */
    @Override
    public int[] get_cycle_table(int which) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_cycle_table(int which, int[] new_table) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * *
     *
     * arcadeflex functions
     */
    @Override
    public Object init_context() {
        Object reg = new m6502_Regs();
        return reg;
    }

    @Override
    public void set_op_base(int pc
    ) {
        cpu_setOPbase16.handler(pc);
    }

    @Override
    public int memory_read(int offset
    ) {
        return cpu_readmem16(offset);
    }

    @Override
    public void memory_write(int offset, int data
    ) {
        cpu_writemem16(offset, data);
    }

    @Override
    public int internal_read(int offset
    ) {
        return 0; //doesn't exist in 6502 cpu
    }

    @Override
    public void internal_write(int offset, int data
    ) {
        //doesesn't exist in 6502 cpu
    }

    @Override
    public int mem_address_bits_of_cpu() {
        return 16;
    }
}
