/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.cpu.i8085;

import static mame056.cpu.i8085.i8085.I;
import static mame056.cpu.i8085.i8085.init_tables;
import static mame056.cpu.i8085.i8085H.I8080_INTR_LINE;
//import static mame056.cpu.i8085.i8085H.i8080_ICount;
import static mame056.cpu.i8085.i8085cpuH.IM_IEN;
import static mame056.cpuintrfH.*;
import static mame056.memory.cpu_readmem16;
import static mame056.memory.cpu_writemem16;



public class i8080 extends i8085 {
    
	
	/**************************************************************************
	 * 8080 section
	 **************************************************************************/
/*TODO*///	#if (HAS_8080)
/*TODO*///	/* Layout of the registers in the debugger */
/*TODO*///	static UINT8 i8080_reg_layout[] = {
/*TODO*///		I8080_AF, I8080_BC, I8080_DE, I8080_HL, I8080_SP, I8080_PC, -1,
/*TODO*///		I8080_HALT, I8080_IREQ, I8080_ISRV, I8080_VECTOR, I8080_TRAP_STATE, I8080_INTR_STATE,
/*TODO*///		0 };
/*TODO*///	
/*TODO*///	/* Layout of the debugger windows x,y,w,h */
/*TODO*///	static UINT8 i8080_win_layout[] = {
/*TODO*///		25, 0,55, 2,	/* register window (top, right rows) */
/*TODO*///		 0, 0,24,22,	/* disassembler window (left colums) */
/*TODO*///		25, 3,55,10,	/* memory #1 window (right, upper middle) */
/*TODO*///		25,14,55, 8,	/* memory #2 window (right, lower middle) */
/*TODO*///		 0,23,80, 1,	/* command line window (bottom rows) */
/*TODO*///	};
/*TODO*///	
        public i8080() {
            cpu_num = CPU_8080;
            num_irqs = 4;
        default_vector = 255;
        icount = i8085_ICount;
        overclock = 1.00;
        irq_int = I8080_INTR_LINE;
        databus_width = 8;
        pgm_memory_base = 0;
        address_shift = 0;
        address_bits = 16;
        endianess = CPU_IS_LE;
        align_unit = 1;
        max_inst_len = 3;
        }
	
        public void i8080_init()
	{
                i8085_init();
		
                int cpu = cpu_getactivecpu();
		init_tables();
		I.cputype = 0;
	
/*TODO*///		state_save_register_UINT16("i8080", cpu, "AF", &I.AF.L, 1);
/*TODO*///		state_save_register_UINT16("i8080", cpu, "BC", &I.BC.L, 1);
/*TODO*///		state_save_register_UINT16("i8080", cpu, "DE", &I.DE.L, 1);
/*TODO*///		state_save_register_UINT16("i8080", cpu, "HL", &I.HL.L, 1);
/*TODO*///		state_save_register_UINT16("i8080", cpu, "SP", &I.SP.L, 1);
/*TODO*///		state_save_register_UINT16("i8080", cpu, "PC", &I.PC.L, 1);
/*TODO*///		state_save_register_UINT8("i8080", cpu, "HALT", &I.HALT, 1);
/*TODO*///		state_save_register_UINT8("i8080", cpu, "IREQ", &I.IREQ, 1);
/*TODO*///		state_save_register_UINT8("i8080", cpu, "ISRV", &I.ISRV, 1);
/*TODO*///		state_save_register_UINT32("i8080", cpu, "INTR", &I.INTR, 1);
/*TODO*///		state_save_register_UINT32("i8080", cpu, "IRQ2", &I.IRQ2, 1);
/*TODO*///		state_save_register_UINT32("i8080", cpu, "IRQ1", &I.IRQ1, 1);
/*TODO*///		state_save_register_INT8("i8080", cpu, "nmi_state", &I.nmi_state, 1);
/*TODO*///		state_save_register_INT8("i8080", cpu, "irq_state", I.irq_state, 1);
	}
	
	public void i8080_reset(Object param) { i8085_reset(param); }
	public void i8080_exit() { i8085_exit(); }
	public int i8080_execute(int cycles) { return i8085_execute(cycles); }
	public Object i8080_get_context(Object dst) { return i8085_get_context(dst); }
	public void i8080_set_context(Object src) { i8085_set_context(src); }
	public int i8080_get_reg(int regnum) { return i8085_get_reg(regnum); }
	public void i8080_set_reg(int regnum, int val)  { i8085_set_reg(regnum,val); }
        
	public void i8080_set_irq_line(int irqline, int state)
	{
            //irqline = irqline & 3;
            
            //System.out.println("i8080_set_irq_line "+irqline);
            
		if (irqline == IRQ_LINE_NMI)
		{
			i8085_set_irq_line(irqline, state);
		}
                else /*HACK*/  //if (irqline < 4)
		{
			I.irq_state[irqline] = state;
			if (state == CLEAR_LINE)
			{
				if ((I.IM & IM_IEN) == 0)
					i8085_set_INTR(0);
			}
			else
			{
				if ((I.IM & IM_IEN) != 0)
					i8085_set_INTR(1);
			}
		}
	}
        
	public void i8080_set_irq_callback(irqcallbacksPtr callback) { 
            System.out.println("i8080_set_irq_callback");
            i8085_set_irq_callback(callback); 
        }
        
	public String i8080_info(Object context, int regnum)
	{
		switch( regnum )
		{
			case CPU_INFO_NAME: return "8080";
			case CPU_INFO_VERSION: return "1.2";
/*TODO*///			case CPU_INFO_REG_LAYOUT: return (const char *)i8080_reg_layout;
/*TODO*///			case CPU_INFO_WIN_LAYOUT: return (const char *)i8080_win_layout;
		}
		return i8085_info(context,regnum);
	}
	
/*TODO*///	unsigned i8080_dasm(char *buffer, unsigned pc)
/*TODO*///	{
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///		return Dasm8085(buffer,pc);
/*TODO*///	#else
/*TODO*///		sprintf( buffer, "$%02X", cpu_readop(pc) );
/*TODO*///		return 1;
/*TODO*///	#endif
/*TODO*///	}
/*TODO*///	#endif

    @Override
    public void init() {
        i8080_init();
    }

    @Override
    public void reset(Object param) {
        i8080_reset(param);
    }

    @Override
    public void exit() {
        i8080_exit();
    }

    @Override
    public int execute(int cycles) {
        return i8080_execute(cycles);
    }

    /*
    @Override
    public Object init_context() {
        Object reg = new I8085_Regs();
        return reg;
    }
    */

    @Override
    public Object get_context() {
        return i8085_get_context(null);
    }

    @Override
    public void set_context(Object reg) {
        i8080_set_context(reg);
    }

    @Override
    public int[] get_cycle_table(int which) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_cycle_table(int which, int[] new_table) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int get_reg(int regnum) {
        return i8080_get_reg(regnum);
    }

    @Override
    public void set_reg(int regnum, int val) {
        i8080_set_reg(regnum, val);
    }

    @Override
    public void set_irq_line(int irqline, int linestate) {
        i8080_set_irq_line(irqline, linestate);
    }

    @Override
    public void set_irq_callback(irqcallbacksPtr callback) {
        i8080_set_irq_callback(callback);
    }

    @Override
    public String cpu_info(Object context, int regnum) {
        return i8080_info(context, regnum);
    }

    @Override
    public String cpu_dasm(String buffer, int pc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int memory_read(int offset) {
        return cpu_readmem16(offset);
    }

    @Override
    public void memory_write(int offset, int data) {
        cpu_writemem16(offset, data);
    }

    @Override
    public int internal_read(int offset) {
        return 0;
    }

    @Override
    public void internal_write(int offset, int data) {
        
    }    
    
    /*
    @Override
    public void set_op_base(int pc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /*
    @Override
    public int mem_address_bits_of_cpu() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    */
}
