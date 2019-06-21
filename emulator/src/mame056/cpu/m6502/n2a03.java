/**
 * ported to v0.56
 */
package mame056.cpu.m6502;

import static mame056.cpu.m6502.m6502.*;
import static mame056.cpu.m6502.m6502H.*;
import static mame056.cpu.m6502.ops02H.*;
import static mame056.cpu.m6502.tn2a03.*;
import static mame056.cpuintrfH.*;
import static mame056.memoryH.*;

public class n2a03 extends m6502 {

    public n2a03() {
        cpu_num = CPU_N2A03;
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

    @Override
    public void init() {
        //m6502_state_register("n2a03");
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
            insn2a03[op].handler();

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

    public String cpu_info(Object context, int regnum) {
        switch (regnum) {
            case CPU_INFO_NAME:
                return "N2A03";
            case CPU_INFO_VERSION:
                return "1.0";
        }
        return super.cpu_info(context, regnum);
    }

    /* The N2A03 is integrally tied to its PSG (they're on the same die).
   Bit 7 of address $4011 (the PSG's DPCM control register), when set,
   causes an IRQ to be generated.  This function allows the IRQ to be called
   from the PSG core when such an occasion arises. */
    public static void n2a03_irq() {
        m6502_take_irq();
    }
}
