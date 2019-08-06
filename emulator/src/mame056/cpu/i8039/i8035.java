/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mame056.cpu.i8039;

import static mame056.cpu.i8039.i8039H.*;
import static mame056.cpu.i8039.i8039.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static common.libc.cstdio.*;
import static arcadeflex036.osdepend.*;
import static common.libc.cstring.memset;

public class i8035 extends cpu_interface {
    
        public i8039 _cpu = new i8039();
    
        public i8035() {
            cpu_num = CPU_I8035;
            num_irqs = 1;
            default_vector = 0;
            icount = i8039_ICount;
            overclock = 1.00;
            irq_int = 0;
            databus_width = 8;
            pgm_memory_base = 0;
            address_shift = 0;
            address_bits = 16;
            endianess = CPU_IS_LE;
            align_unit = 1;
            max_inst_len = 2;
        }
    
        /**************************************************************************
	 * I8035 section
	 **************************************************************************/
	
	/* Layout of the registers in the debugger */
	/*TODO*///static UINT8 i8035_reg_layout[] = {
	/*TODO*///	I8035_PC, I8035_SP, I8035_PSW, I8035_A, I8035_IRQ_STATE,    I8035_P1, I8035_P2, -1,
	/*TODO*///	I8035_R0, I8035_R1, I8035_R2, I8035_R3, I8035_R4, I8035_R5, I8035_R6, I8035_R7, 0
	/*TODO*///};
	
	/* Layout of the debugger windows x,y,w,h */
	/*TODO*///static UINT8 i8035_win_layout[] = {
	/*TODO*///	 0, 0,80, 2,	/* register window (top rows) */
	/*TODO*///	 0, 3,24,19,	/* disassembler window (left colums) */
	/*TODO*///	25, 3,55, 9,	/* memory #1 window (right, upper middle) */
	/*TODO*///	25,13,55, 9,	/* memory #2 window (right, lower middle) */
	/*TODO*///	 0,23,80, 1,	/* command line window (bottom rows) */
	/*TODO*///};
	
	public void i8035_init() { }
	public void i8035_reset(Object param) { _cpu.reset(param); }
	public void i8035_exit() { _cpu.exit(); }
	public int i8035_execute(int cycles) { return _cpu.execute(cycles); }
	public Object i8035_get_context() { return _cpu.get_context(); }
	public void i8035_set_context(Object src)  { _cpu.set_context(src); }
	public int i8035_get_reg(int regnum) { return _cpu.get_reg(regnum); }
	public void i8035_set_reg(int regnum, int val) { _cpu.set_reg(regnum,val); }
	public void i8035_set_irq_line(int irqline, int state) { _cpu.set_irq_line(irqline,state); }
	public void i8035_set_irq_callback(irqcallbacksPtr callback) { _cpu.set_irq_callback(callback); }
	public String i8035_info(Object context, int regnum)
	{
		switch( regnum )
		{
			case CPU_INFO_NAME: return "I8035";
			case CPU_INFO_VERSION: return "1.1";
			/*TODO*///case CPU_INFO_REG_LAYOUT: return (const char*)i8035_reg_layout;
			/*TODO*///case CPU_INFO_WIN_LAYOUT: return (const char*)i8035_win_layout;
		}
		return _cpu.cpu_info(context,regnum);
	}
	
	/*TODO*///unsigned i8035_dasm(char *buffer, unsigned pc)
	/*TODO*///{
	/*TODO*///#ifdef MAME_DEBUG
	/*TODO*///	return Dasm8039(buffer,pc);
	/*TODO*///#else
	/*TODO*///	sprintf( buffer, "$%02X", cpu_readop(pc) );
	/*TODO*///	return 1;
	/*TODO*///#endif
	/*TODO*///}
	
	/*TODO*///#endif

    @Override
    public void init() {
        i8035_init();
    }

    @Override
    public void reset(Object param) {
        i8035_reset(param);
    }

    @Override
    public void exit() {
        i8035_exit();
    }

    @Override
    public int execute(int cycles) {
        return i8035_execute(cycles);
    }

    @Override
    public Object init_context() {
        return _cpu.init_context();
    }

    @Override
    public Object get_context() {
        return i8035_get_context();
    }

    @Override
    public void set_context(Object reg) {
        i8035_set_context(reg);
    }

    @Override
    public int[] get_cycle_table(int which) {
        return _cpu.get_cycle_table(which);
    }

    @Override
    public void set_cycle_table(int which, int[] new_table) {
        _cpu.set_cycle_table(which, new_table);
    }

    @Override
    public int get_reg(int regnum) {
        return i8035_get_reg(regnum);
    }

    @Override
    public void set_reg(int regnum, int val) {
        i8035_set_reg(regnum, val);
    }

    @Override
    public void set_irq_line(int irqline, int linestate) {
        i8035_set_irq_line(irqline, linestate);
    }

    @Override
    public void set_irq_callback(irqcallbacksPtr callback) {
        i8035_set_irq_callback(callback);
    }

    @Override
    public String cpu_info(Object context, int regnum) {
        return i8035_info(context, regnum);
    }

    @Override
    public String cpu_dasm(String buffer, int pc) {
        return _cpu.cpu_dasm(buffer, pc);
    }

    @Override
    public int memory_read(int offset) {
        return _cpu.memory_read(offset);
    }

    @Override
    public void memory_write(int offset, int data) {
        _cpu.memory_write(offset, data);
    }

    @Override
    public int internal_read(int offset) {
        return _cpu.internal_read(offset);
    }

    @Override
    public void internal_write(int offset, int data) {
        _cpu.internal_write(offset, data);
    }

    @Override
    public void set_op_base(int pc) {
        _cpu.set_op_base(pc);
    }

    @Override
    public int mem_address_bits_of_cpu() {
        return _cpu.mem_address_bits_of_cpu();
    }
    
}
