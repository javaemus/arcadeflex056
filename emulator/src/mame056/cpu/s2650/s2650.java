/**
 *
 * ported to v0.56
 * ported to v0.37b7
 *
 */
/**
 * Changelog
 * ---------
 * 29/05/2019 - initial work on s2650 (shadow)
 */
package mame056.cpu.s2650;

import static mame056.cpuintrfH.*;

public class s2650 extends cpu_interface {

    static int[] s2650_ICount = new int[1];

    public s2650() {
        cpu_num = CPU_S2650;
        num_irqs = 2;
        default_vector = 0;
        icount = s2650_ICount;
        overclock = 1.00;
        irq_int = -1;
        databus_width = 8;
        pgm_memory_base = 0;
        address_shift = 0;
        address_bits = 15;
        endianess = CPU_IS_LE;
        align_unit = 1;
        max_inst_len = 3;
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void reset(Object param) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void exit() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int execute(int cycles) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object init_context() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object get_context() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_context(Object reg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_reg(int regnum, int val) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_irq_line(int irqline, int linestate) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_irq_callback(irqcallbacksPtr callback) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String cpu_info(Object context, int regnum) {
        return s2650_info(context, regnum);
    }

    @Override
    public String cpu_dasm(String buffer, int pc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int memory_read(int offset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void memory_write(int offset, int data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int internal_read(int offset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void internal_write(int offset, int data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void set_op_base(int pc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int mem_address_bits_of_cpu() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /*TODO*////* define this to have some interrupt information logged */
/*TODO*///	#define VERBOSE 0
/*TODO*///	
/*TODO*///	#if VERBOSE
/*TODO*///	#define LOG(x) logerror x
/*TODO*///	#else
/*TODO*///	#define LOG(x)
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	/* define this to expand all EA calculations inline */
/*TODO*///	#define INLINE_EA	1
/*TODO*///	
/*TODO*///	static UINT8 s2650_reg_layout[] = {
/*TODO*///		S2650_PC, S2650_PS, S2650_R0, S2650_R1, S2650_R2, S2650_R3, -1,
/*TODO*///		S2650_SI, S2650_FO, S2650_R1A, S2650_R2A, S2650_R3A, -1,
/*TODO*///		S2650_HALT, S2650_IRQ_STATE, 0
/*TODO*///	};
/*TODO*///	
/*TODO*///	/* Layout of the debugger windows x,y,w,h */
/*TODO*///	static UINT8 s2650_win_layout[] = {
/*TODO*///		32, 0,48, 4,	/* register window (top rows) */
/*TODO*///		 0, 0,31,22,	/* disassembler window (left colums) */
/*TODO*///		32, 5,48, 8,	/* memory #1 window (right, upper middle) */
/*TODO*///		32,14,48, 8,	/* memory #2 window (right, lower middle) */
/*TODO*///	     0,23,80, 1,    /* command line window (bottom rows) */
/*TODO*///	};
/*TODO*///	
/*TODO*///	int s2650_ICount = 0;
/*TODO*///	
/*TODO*///	typedef struct {
/*TODO*///		UINT16	ppc;	/* previous program counter (page + iar) */
/*TODO*///	    UINT16  page;   /* 8K page select register (A14..A13) */
/*TODO*///	    UINT16  iar;    /* instruction address register (A12..A0) */
/*TODO*///	    UINT16  ea;     /* effective address (A14..A0) */
/*TODO*///	    UINT8   psl;    /* processor status lower */
/*TODO*///	    UINT8   psu;    /* processor status upper */
/*TODO*///	    UINT8   r;      /* absolute addressing dst/src register */
/*TODO*///	    UINT8   reg[7]; /* 7 general purpose registers */
/*TODO*///	    UINT8   halt;   /* 1 if cpu is halted */
/*TODO*///	    UINT8   ir;     /* instruction register */
/*TODO*///	    UINT16  ras[8]; /* 8 return address stack entries */
/*TODO*///		UINT8	irq_state;
/*TODO*///	    int     (*irq_callback)(int irqline);
/*TODO*///	}   s2650_Regs;
/*TODO*///	
/*TODO*///	static s2650_Regs S;
/*TODO*///	
/*TODO*///	/* condition code changes for a byte */
/*TODO*///	static	UINT8 ccc[0x200] = {
/*TODO*///		0x00,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,
/*TODO*///		0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,
/*TODO*///		0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,
/*TODO*///		0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,
/*TODO*///		0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,
/*TODO*///		0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,
/*TODO*///		0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,
/*TODO*///		0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,0x40,
/*TODO*///		0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,
/*TODO*///		0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,
/*TODO*///		0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,
/*TODO*///		0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,
/*TODO*///		0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,
/*TODO*///		0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,
/*TODO*///		0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,
/*TODO*///		0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,0x80,
/*TODO*///		0x04,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,
/*TODO*///		0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,
/*TODO*///		0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,
/*TODO*///		0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,
/*TODO*///		0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,
/*TODO*///		0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,
/*TODO*///		0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,
/*TODO*///		0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,
/*TODO*///		0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,
/*TODO*///		0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,
/*TODO*///		0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,
/*TODO*///		0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,
/*TODO*///		0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,
/*TODO*///		0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,
/*TODO*///		0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,
/*TODO*///		0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,0x84,
/*TODO*///	};
/*TODO*///	
/*TODO*///	#define CHECK_IRQ_LINE											\
/*TODO*///		if (S.irq_state != CLEAR_LINE)								\
/*TODO*///		{															\
/*TODO*///			if( (S.psu & II) == 0 ) 								\
/*TODO*///			{														\
/*TODO*///				int vector; 										\
/*TODO*///				if (S.halt) 										\
/*TODO*///				{													\
/*TODO*///					S.halt = 0; 									\
/*TODO*///					S.iar = (S.iar + 1) & PMSK; 					\
/*TODO*///				}													\
/*TODO*///				vector = (*S.irq_callback)(0) & 0xff;				\
/*TODO*///				/* build effective address within first 8K page */	\
/*TODO*///				S.ea = S2650_relative[vector] & PMSK;				\
/*TODO*///				if (vector & 0x80)		/* indirect bit set ? */	\
/*TODO*///				{													\
/*TODO*///					int addr = S.ea;								\
/*TODO*///					s2650_ICount -= 2;								\
/*TODO*///					/* build indirect 32K address */				\
/*TODO*///					S.ea = RDMEM(addr) << 8;						\
/*TODO*///					if (!(++addr & PMSK)) addr -= PLEN; 			\
/*TODO*///					S.ea = (S.ea + RDMEM(addr)) & AMSK; 			\
/*TODO*///				}													\
/*TODO*///				LOG(("S2650 interrupt to $%04x\n", S.ea));\
/*TODO*///				S.psu  = (S.psu & ~SP) | ((S.psu + 1) & SP) | II;	\
/*TODO*///				S.ras[S.psu & SP] = S.page + S.iar;					\
/*TODO*///				S.page = S.ea & PAGE;								\
/*TODO*///				S.iar  = S.ea & PMSK;								\
/*TODO*///			}														\
/*TODO*///		}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 *
/*TODO*///	 * set condition code (zero,plus,minus) from result
/*TODO*///	 ***************************************************************/
/*TODO*///	#define SET_CC(result)                                          \
/*TODO*///		S.psl = (S.psl & ~CC) | ccc[result]
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 *
/*TODO*///	 * set condition code (zero,plus,minus) and overflow
/*TODO*///	 ***************************************************************/
/*TODO*///	#define SET_CC_OVF(result,value)                                \
/*TODO*///		S.psl = (S.psl & ~(OVF+CC)) |								\
/*TODO*///			ccc[result + ( ( (result^value) << 1) & 256 )]
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * ROP
/*TODO*///	 * read next opcode
/*TODO*///	 ***************************************************************/
/*TODO*///	INLINE UINT8 ROP(void)
/*TODO*///	{
/*TODO*///		UINT8 result = cpu_readop(S.page + S.iar);
/*TODO*///		S.iar = (S.iar + 1) & PMSK;
/*TODO*///		return result;
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * ARG
/*TODO*///	 * read next opcode argument
/*TODO*///	 ***************************************************************/
/*TODO*///	INLINE UINT8 ARG(void)
/*TODO*///	{
/*TODO*///		UINT8 result = cpu_readop_arg(S.page + S.iar);
/*TODO*///		S.iar = (S.iar + 1) & PMSK;
/*TODO*///		return result;
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * RDMEM
/*TODO*///	 * read memory byte from addr
/*TODO*///	 ***************************************************************/
/*TODO*///	#define RDMEM(addr) cpu_readmem16(addr)
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * handy table to build PC relative offsets
/*TODO*///	 * from HR (holding register)
/*TODO*///	 ***************************************************************/
/*TODO*///	static	int 	S2650_relative[0x100] =
/*TODO*///	{
/*TODO*///		  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15,
/*TODO*///		 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
/*TODO*///		 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
/*TODO*///		 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,
/*TODO*///		-64,-63,-62,-61,-60,-59,-58,-57,-56,-55,-54,-53,-52,-51,-50,-49,
/*TODO*///		-48,-47,-46,-45,-44,-43,-42,-41,-40,-39,-38,-37,-36,-35,-34,-33,
/*TODO*///		-32,-31,-30,-29,-28,-27,-26,-25,-24,-23,-22,-21,-20,-19,-18,-17,
/*TODO*///		-16,-15,-14,-13,-12,-11,-10, -9, -8, -7, -6, -5, -4, -3, -2, -1,
/*TODO*///		  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15,
/*TODO*///		 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
/*TODO*///		 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
/*TODO*///		 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,
/*TODO*///		-64,-63,-62,-61,-60,-59,-58,-57,-56,-55,-54,-53,-52,-51,-50,-49,
/*TODO*///		-48,-47,-46,-45,-44,-43,-42,-41,-40,-39,-38,-37,-36,-35,-34,-33,
/*TODO*///		-32,-31,-30,-29,-28,-27,-26,-25,-24,-23,-22,-21,-20,-19,-18,-17,
/*TODO*///		-16,-15,-14,-13,-12,-11,-10, -9, -8, -7, -6, -5, -4, -3, -2, -1,
/*TODO*///	};
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * _REL_EA
/*TODO*///	 * build effective address with relative addressing
/*TODO*///	 ***************************************************************/
/*TODO*///	#define _REL_EA(page)											\
/*TODO*///	{																\
/*TODO*///		UINT8 hr = ARG();	/* get 'holding register' */            \
/*TODO*///		/* build effective address within current 8K page */		\
/*TODO*///		S.ea = page + ((S.iar + S2650_relative[hr]) & PMSK);		\
/*TODO*///		if (hr & 0x80) { /* indirect bit set ? */					\
/*TODO*///			int addr = S.ea;										\
/*TODO*///			s2650_ICount -= 2;										\
/*TODO*///			/* build indirect 32K address */						\
/*TODO*///			S.ea = RDMEM(addr) << 8;								\
/*TODO*///			if( (++addr & PMSK) == 0 ) addr -= PLEN; /* page wrap */\
/*TODO*///			S.ea = (S.ea + RDMEM(addr)) & AMSK; 					\
/*TODO*///		}															\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * _REL_ZERO
/*TODO*///	 * build effective address with zero relative addressing
/*TODO*///	 ***************************************************************/
/*TODO*///	#define _REL_ZERO(page)											\
/*TODO*///	{																\
/*TODO*///		UINT8 hr = ARG();	/* get 'holding register' */            \
/*TODO*///		/* build effective address from 0 */						\
/*TODO*///		S.ea = (S2650_relative[hr] & PMSK);							\
/*TODO*///		if (hr & 0x80) { /* indirect bit set ? */					\
/*TODO*///			int addr = S.ea;										\
/*TODO*///			s2650_ICount -= 2;										\
/*TODO*///			/* build indirect 32K address */						\
/*TODO*///			S.ea = RDMEM(addr) << 8;								\
/*TODO*///			if( (++addr & PMSK) == 0 ) addr -= PLEN; /* page wrap */\
/*TODO*///			S.ea = (S.ea + RDMEM(addr)) & AMSK; 					\
/*TODO*///		}															\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * _ABS_EA
/*TODO*///	 * build effective address with absolute addressing
/*TODO*///	 ***************************************************************/
/*TODO*///	#define _ABS_EA()												\
/*TODO*///	{																\
/*TODO*///		UINT8 hr, dr;												\
/*TODO*///		hr = ARG(); 	/* get 'holding register' */                \
/*TODO*///		dr = ARG(); 	/* get 'data bus register' */               \
/*TODO*///		/* build effective address within current 8K page */		\
/*TODO*///		S.ea = S.page + (((hr << 8) + dr) & PMSK);					\
/*TODO*///		/* indirect addressing ? */ 								\
/*TODO*///		if (hr & 0x80) {											\
/*TODO*///			int addr = S.ea;										\
/*TODO*///			s2650_ICount -= 2;										\
/*TODO*///			/* build indirect 32K address */						\
/*TODO*///			/* build indirect 32K address */						\
/*TODO*///			S.ea = RDMEM(addr) << 8;								\
/*TODO*///			if( (++addr & PMSK) == 0 ) addr -= PLEN; /* page wrap */\
/*TODO*///	        S.ea = (S.ea + RDMEM(addr)) & AMSK;                     \
/*TODO*///		}															\
/*TODO*///		/* check indexed addressing modes */						\
/*TODO*///		switch (hr & 0x60) {										\
/*TODO*///			case 0x00: /* not indexed */							\
/*TODO*///				break;												\
/*TODO*///			case 0x20: /* auto increment indexed */ 				\
/*TODO*///				S.reg[S.r] += 1;									\
/*TODO*///				S.ea = (S.ea & PAGE)+((S.ea+S.reg[S.r]) & PMSK);	\
/*TODO*///				S.r = 0; /* absolute addressing reg is R0 */		\
/*TODO*///				break;												\
/*TODO*///			case 0x40: /* auto decrement indexed */ 				\
/*TODO*///				S.reg[S.r] -= 1;									\
/*TODO*///				S.ea = (S.ea & PAGE)+((S.ea+S.reg[S.r]) & PMSK);	\
/*TODO*///				S.r = 0; /* absolute addressing reg is R0 */		\
/*TODO*///				break;												\
/*TODO*///			case 0x60: /* indexed */								\
/*TODO*///				S.ea = (S.ea & PAGE)+((S.ea+S.reg[S.r]) & PMSK);	\
/*TODO*///				S.r = 0; /* absolute addressing reg is R0 */		\
/*TODO*///				break;												\
/*TODO*///		}															\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * _BRA_EA
/*TODO*///	 * build effective address with absolute addressing (branch)
/*TODO*///	 ***************************************************************/
/*TODO*///	#define _BRA_EA()												\
/*TODO*///	{																\
/*TODO*///		UINT8 hr, dr;												\
/*TODO*///		hr = ARG(); 	/* get 'holding register' */                \
/*TODO*///		dr = ARG(); 	/* get 'data bus register' */               \
/*TODO*///		/* build address in 32K address space */					\
/*TODO*///		S.ea = ((hr << 8) + dr) & AMSK; 							\
/*TODO*///		/* indirect addressing ? */ 								\
/*TODO*///		if (hr & 0x80) {											\
/*TODO*///			int addr = S.ea;										\
/*TODO*///			s2650_ICount -= 2;										\
/*TODO*///			/* build indirect 32K address */						\
/*TODO*///			S.ea = RDMEM(addr) << 8;								\
/*TODO*///			if( (++addr & PMSK) == 0 ) addr -= PLEN; /* page wrap */\
/*TODO*///	        S.ea = (S.ea + RDMEM(addr)) & AMSK;                     \
/*TODO*///		}															\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * SWAP_REGS
/*TODO*///	 * Swap registers r1-r3 with r4-r6 (the second set)
/*TODO*///	 * This is done everytime the RS bit in PSL changes
/*TODO*///	 ***************************************************************/
/*TODO*///	#define SWAP_REGS												\
/*TODO*///	{																\
/*TODO*///		UINT8 tmp;													\
/*TODO*///		tmp = S.reg[1]; 											\
/*TODO*///		S.reg[1] = S.reg[4];										\
/*TODO*///		S.reg[4] = tmp; 											\
/*TODO*///		tmp = S.reg[2]; 											\
/*TODO*///		S.reg[2] = S.reg[5];										\
/*TODO*///		S.reg[5] = tmp; 											\
/*TODO*///		tmp = S.reg[3]; 											\
/*TODO*///		S.reg[3] = S.reg[6];										\
/*TODO*///		S.reg[6] = tmp; 											\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_BRR
/*TODO*///	 * Branch relative if cond is true
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_BRR(cond) 											\
/*TODO*///	{																\
/*TODO*///		if (cond)													\
/*TODO*///		{															\
/*TODO*///			REL_EA( S.page );										\
/*TODO*///			S.page = S.ea & PAGE;									\
/*TODO*///			S.iar  = S.ea & PMSK;									\
/*TODO*///			change_pc16(S.ea);										\
/*TODO*///		} else S.iar = (S.iar + 1) & PMSK;							\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_ZBRR
/*TODO*///	 * Branch relative to page zero
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_ZBRR()												\
/*TODO*///	{																\
/*TODO*///		REL_ZERO( 0 );												\
/*TODO*///		S.page = S.ea & PAGE;										\
/*TODO*///		S.iar  = S.ea & PMSK;										\
/*TODO*///		change_pc16(S.ea);											\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_BRA
/*TODO*///	 * Branch absolute if cond is true
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_BRA(cond) 											\
/*TODO*///	{																\
/*TODO*///		if( cond )													\
/*TODO*///		{															\
/*TODO*///			BRA_EA();												\
/*TODO*///			S.page = S.ea & PAGE;									\
/*TODO*///			S.iar  = S.ea & PMSK;									\
/*TODO*///			change_pc16(S.ea);										\
/*TODO*///		} else S.iar = (S.iar + 2) & PMSK;							\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_BXA
/*TODO*///	 * Branch indexed absolute (EA + R3)
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_BXA() 												\
/*TODO*///	{																\
/*TODO*///		BRA_EA();													\
/*TODO*///		S.ea   = (S.ea + S.reg[3]) & AMSK;							\
/*TODO*///		S.page = S.ea & PAGE;										\
/*TODO*///		S.iar  = S.ea & PMSK;										\
/*TODO*///		change_pc16(S.ea);											\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_BSR
/*TODO*///	 * Branch to subroutine relative if cond is true
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_BSR(cond) 											\
/*TODO*///	{																\
/*TODO*///		if( cond )													\
/*TODO*///		{															\
/*TODO*///			REL_EA(S.page); 										\
/*TODO*///			S.psu  = (S.psu & ~SP) | ((S.psu + 1) & SP);			\
/*TODO*///			S.ras[S.psu & SP] = S.page + S.iar;						\
/*TODO*///			S.page = S.ea & PAGE;									\
/*TODO*///			S.iar  = S.ea & PMSK;									\
/*TODO*///			change_pc16(S.ea);										\
/*TODO*///		} else	S.iar = (S.iar + 1) & PMSK; 						\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_ZBSR
/*TODO*///	 * Branch to subroutine relative to page zero
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_ZBSR()												\
/*TODO*///	{																\
/*TODO*///		REL_ZERO(0); 											    \
/*TODO*///		S.psu  = (S.psu & ~SP) | ((S.psu + 1) & SP);				\
/*TODO*///		S.ras[S.psu & SP] = S.page + S.iar;							\
/*TODO*///		S.page = S.ea & PAGE;										\
/*TODO*///		S.iar  = S.ea & PMSK;										\
/*TODO*///		change_pc16(S.ea);											\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_BSA
/*TODO*///	 * Branch to subroutine absolute
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_BSA(cond) 											\
/*TODO*///	{																\
/*TODO*///		if( cond )													\
/*TODO*///		{															\
/*TODO*///			BRA_EA();												\
/*TODO*///			S.psu = (S.psu & ~SP) | ((S.psu + 1) & SP); 			\
/*TODO*///			S.ras[S.psu & SP] = S.page + S.iar;						\
/*TODO*///			S.page = S.ea & PAGE;									\
/*TODO*///			S.iar  = S.ea & PMSK;									\
/*TODO*///			change_pc16(S.ea);										\
/*TODO*///		} else S.iar = (S.iar + 2) & PMSK;							\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_BSXA
/*TODO*///	 * Branch to subroutine indexed absolute (EA + R3)
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_BSXA()												\
/*TODO*///	{																\
/*TODO*///		BRA_EA();													\
/*TODO*///		S.ea  = (S.ea + S.reg[3]) & AMSK;							\
/*TODO*///		S.psu = (S.psu & ~SP) | ((S.psu + 1) & SP); 				\
/*TODO*///		S.ras[S.psu & SP] = S.page + S.iar;							\
/*TODO*///		S.page = S.ea & PAGE;										\
/*TODO*///		S.iar  = S.ea & PMSK;										\
/*TODO*///		change_pc16(S.ea);											\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_RET
/*TODO*///	 * Return from subroutine if cond is true
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_RET(cond) 											\
/*TODO*///	{																\
/*TODO*///		if( cond )													\
/*TODO*///		{															\
/*TODO*///			S.ea = S.ras[S.psu & SP];								\
/*TODO*///			S.psu = (S.psu & ~SP) | ((S.psu - 1) & SP); 			\
/*TODO*///			S.page = S.ea & PAGE;									\
/*TODO*///			S.iar  = S.ea & PMSK;									\
/*TODO*///			change_pc16(S.ea);										\
/*TODO*///		}															\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_RETE
/*TODO*///	 * Return from subroutine if cond is true
/*TODO*///	 * and enable interrupts; afterwards check IRQ line
/*TODO*///	 * state and eventually take next interrupt
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_RETE(cond)											\
/*TODO*///	{																\
/*TODO*///		if( cond )													\
/*TODO*///		{															\
/*TODO*///			S.ea = S.ras[S.psu & SP];								\
/*TODO*///			S.psu = (S.psu & ~SP) | ((S.psu - 1) & SP); 			\
/*TODO*///			S.page = S.ea & PAGE;									\
/*TODO*///			S.iar  = S.ea & PMSK;									\
/*TODO*///			change_pc16(S.ea);										\
/*TODO*///			S.psu &= ~II;											\
/*TODO*///			CHECK_IRQ_LINE; 										\
/*TODO*///		}															\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_LOD
/*TODO*///	 * Load destination with source register
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_LOD(dest,source)										\
/*TODO*///	{																\
/*TODO*///		dest = source;												\
/*TODO*///		SET_CC(dest);												\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_STR
/*TODO*///	 * Store source register to memory addr (CC unchanged)
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_STR(address,source)									\
/*TODO*///		cpu_writemem16(address, source)
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_AND
/*TODO*///	 * Logical and destination with source
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_AND(dest,source)										\
/*TODO*///	{																\
/*TODO*///		dest &= source; 											\
/*TODO*///		SET_CC(dest);												\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_IOR
/*TODO*///	 * Logical inclusive or destination with source
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_IOR(dest,source)										\
/*TODO*///	{																\
/*TODO*///		dest |= source; 											\
/*TODO*///		SET_CC(dest);												\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_EOR
/*TODO*///	 * Logical exclusive or destination with source
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_EOR(dest,source)										\
/*TODO*///	{																\
/*TODO*///		dest ^= source; 											\
/*TODO*///		SET_CC(dest);												\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_ADD
/*TODO*///	 * Add source to destination
/*TODO*///	 * Add with carry if WC flag of PSL is set
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_ADD(dest,source)										\
/*TODO*///	{																\
/*TODO*///		UINT8 before = dest;										\
/*TODO*///		/* add source; carry only if WC is set */					\
/*TODO*///		UINT16 res = dest + source + ((S.psl >> 3) & S.psl & C);	\
/*TODO*///		S.psl &= ~(C | OVF | IDC);									\
/*TODO*///		if(res & 0x100) S.psl |= C; 							    \
/*TODO*///	    dest = res & 0xff;                                          \
/*TODO*///		if( (dest & 15) < (before & 15) ) S.psl |= IDC; 			\
/*TODO*///		SET_CC_OVF(dest,before);									\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_SUB
/*TODO*///	 * Subtract source from destination
/*TODO*///	 * Subtract with borrow if WC flag of PSL is set
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_SUB(dest,source)										\
/*TODO*///	{																\
/*TODO*///		UINT8 before = dest;										\
/*TODO*///		/* subtract source; borrow only if WC is set */ 			\
/*TODO*///		UINT16 res = dest - source - ((S.psl >> 3) & (S.psl ^ C) & C);	\
/*TODO*///		S.psl &= ~(C | OVF | IDC);									\
/*TODO*///		if((res & 0x100)==0) S.psl |= C; 							\
/*TODO*///	    dest = res & 0xff;                                          \
/*TODO*///		if( (dest & 15) < (before & 15) ) S.psl |= IDC; 			\
/*TODO*///		SET_CC_OVF(dest,before);									\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_COM
/*TODO*///	 * Compare register against value. If COM of PSL is set,
/*TODO*///	 * use unsigned, else signed comparison
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_COM(reg,val)											\
/*TODO*///	{																\
/*TODO*///		int d;														\
/*TODO*///		S.psl &= ~CC;												\
/*TODO*///		if (S.psl & COM) d = (UINT8)reg - (UINT8)val;				\
/*TODO*///					else d = (INT8)reg - (INT8)val; 				\
/*TODO*///		if( d < 0 ) S.psl |= 0x80;									\
/*TODO*///		else														\
/*TODO*///		if( d > 0 ) S.psl |= 0x40;									\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_DAR
/*TODO*///	 * Decimal adjust register
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_DAR(dest)												\
/*TODO*///	{																\
/*TODO*///		if((S.psl & IDC) != 0)										\
/*TODO*///		{															\
/*TODO*///			if((S.psl & C) == 0) dest -= 0x60;						\
/*TODO*///		}															\
/*TODO*///		else														\
/*TODO*///		{															\
/*TODO*///			if( (S.psl & C) != 0 ) dest -= 0x06;					\
/*TODO*///			else dest -= 0x66;										\
/*TODO*///		}															\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_RRL
/*TODO*///	 * Rotate register left; If WC of PSL is set, rotate
/*TODO*///	 * through carry, else rotate circular
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_RRL(dest) 											\
/*TODO*///	{																\
/*TODO*///		UINT8 before = dest;										\
/*TODO*///		if( S.psl & WC )											\
/*TODO*///		{															\
/*TODO*///			UINT8 c = S.psl & C;									\
/*TODO*///			S.psl &= ~(C + IDC);									\
/*TODO*///			dest = (before << 1) | c;								\
/*TODO*///			S.psl |= (before >> 7) + (dest & IDC);					\
/*TODO*///		}															\
/*TODO*///		else														\
/*TODO*///		{															\
/*TODO*///			dest = (before << 1) | (before >> 7);					\
/*TODO*///		}															\
/*TODO*///		SET_CC_OVF(dest,before);									\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_RRR
/*TODO*///	 * Rotate register right; If WC of PSL is set, rotate
/*TODO*///	 * through carry, else rotate circular
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_RRR(dest) 											\
/*TODO*///	{																\
/*TODO*///		UINT8 before = dest;										\
/*TODO*///		if (S.psl & WC) 											\
/*TODO*///		{															\
/*TODO*///			UINT8 c = S.psl & C;									\
/*TODO*///			S.psl &= ~(C + IDC);									\
/*TODO*///			dest = (before >> 1) | (c << 7);						\
/*TODO*///			S.psl |= (before & C) + (dest & IDC);					\
/*TODO*///		} else	dest = (before >> 1) | (before << 7);				\
/*TODO*///		SET_CC_OVF(dest,before);									\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_SPSU
/*TODO*///	 * Store processor status upper (PSU) to register R0
/*TODO*///	 * Checks for External Sense IO port
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_SPSU()												\
/*TODO*///	{																\
/*TODO*///		R0 = ((S.psu & ~PSU34) | (cpu_readport16(S2650_SENSE_PORT) & SI)); \
/*TODO*///		SET_CC(R0); 												\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_SPSL
/*TODO*///	 * Store processor status lower (PSL) to register R0
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_SPSL()												\
/*TODO*///	{																\
/*TODO*///		R0 = S.psl; 												\
/*TODO*///		SET_CC(R0); 												\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_CPSU
/*TODO*///	 * Clear processor status upper (PSU), selective
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_CPSU()												\
/*TODO*///	{																\
/*TODO*///		UINT8 cpsu = ARG(); 										\
/*TODO*///		S.psu = S.psu & ~cpsu;										\
/*TODO*///		CHECK_IRQ_LINE; 											\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_CPSL
/*TODO*///	 * Clear processor status lower (PSL), selective
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_CPSL()												\
/*TODO*///	{																\
/*TODO*///		UINT8 cpsl = ARG(); 										\
/*TODO*///		/* select other register set now ? */						\
/*TODO*///		if( (cpsl & RS) && (S.psl & RS) )							\
/*TODO*///			SWAP_REGS;												\
/*TODO*///		S.psl = S.psl & ~cpsl;										\
/*TODO*///		CHECK_IRQ_LINE; 											\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_PPSU
/*TODO*///	 * Preset processor status upper (PSU), selective
/*TODO*///	 * Unused bits 3 and 4 can't be set
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_PPSU()												\
/*TODO*///	{																\
/*TODO*///		UINT8 ppsu = (ARG() & ~PSU34) & ~SI;						\
/*TODO*///		S.psu = S.psu | ppsu;										\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_PPSL
/*TODO*///	 * Preset processor status lower (PSL), selective
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_PPSL()												\
/*TODO*///	{																\
/*TODO*///		UINT8 ppsl = ARG(); 										\
/*TODO*///		/* select 2nd register set now ? */ 						\
/*TODO*///		if ((ppsl & RS) && !(S.psl & RS))							\
/*TODO*///			SWAP_REGS;												\
/*TODO*///		S.psl = S.psl | ppsl;										\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_TPSU
/*TODO*///	 * Test processor status upper (PSU)
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_TPSU()												\
/*TODO*///	{																\
/*TODO*///		UINT8 tpsu = ARG(); 										\
/*TODO*///	    UINT8 rpsu = (S.psu | (cpu_readport16(S2650_SENSE_PORT) & SI)); \
/*TODO*///		S.psl &= ~CC;												\
/*TODO*///		if( (rpsu & tpsu) != tpsu )									\
/*TODO*///			S.psl |= 0x80;											\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_TPSL
/*TODO*///	 * Test processor status lower (PSL)
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_TPSL()												\
/*TODO*///	{																\
/*TODO*///		UINT8 tpsl = ARG(); 										\
/*TODO*///		if( (S.psl & tpsl) != tpsl )								\
/*TODO*///			S.psl = (S.psl & ~CC) | 0x80;							\
/*TODO*///		else														\
/*TODO*///			S.psl &= ~CC;											\
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***************************************************************
/*TODO*///	 * M_TMI
/*TODO*///	 * Test under mask immediate
/*TODO*///	 ***************************************************************/
/*TODO*///	#define M_TMI(value)											\
/*TODO*///	{																\
/*TODO*///		UINT8 tmi = ARG();											\
/*TODO*///		S.psl &= ~CC;												\
/*TODO*///		if( (value & tmi) != tmi )									\
/*TODO*///			S.psl |= 0x80;											\
/*TODO*///	}
/*TODO*///	
/*TODO*///	#if INLINE_EA
/*TODO*///	#define REL_EA(page) _REL_EA(page)
/*TODO*///	#define REL_ZERO(page) _REL_ZERO(page)
/*TODO*///	#define ABS_EA() _ABS_EA()
/*TODO*///	#define BRA_EA() _BRA_EA()
/*TODO*///	#else
/*TODO*///	static void REL_EA(unsigned short page) _REL_EA(page)
/*TODO*///	static void REL_ZERO(unsigned short page) _REL_ZERO(page)
/*TODO*///	static void ABS_EA(void) _ABS_EA()
/*TODO*///	static void BRA_EA(void) _BRA_EA()
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	void s2650_init(void)
/*TODO*///	{
/*TODO*///	}
/*TODO*///	
/*TODO*///	void s2650_reset(void *param)
/*TODO*///	{
/*TODO*///		memset(&S, 0, sizeof(S));
/*TODO*///		S.psl = COM | WC;
/*TODO*///		S.psu = 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	void s2650_exit(void)
/*TODO*///	{
/*TODO*///		/* nothing to do */
/*TODO*///	}
/*TODO*///	
/*TODO*///	unsigned s2650_get_context(void *dst)
/*TODO*///	{
/*TODO*///		if( dst )
/*TODO*///			*(s2650_Regs*)dst = S;
/*TODO*///		return sizeof(s2650_Regs);
/*TODO*///	}
/*TODO*///	
/*TODO*///	void s2650_set_context(void *src)
/*TODO*///	{
/*TODO*///		if( src )
/*TODO*///		{
/*TODO*///			S = *(s2650_Regs*)src;
/*TODO*///			S.page = S.page & PAGE;
/*TODO*///			S.iar = S.iar & PMSK;
/*TODO*///	        change_pc16(S.page + S.iar);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	unsigned s2650_get_reg(int regnum)
/*TODO*///	{
/*TODO*///		switch( regnum )
/*TODO*///		{
/*TODO*///			case REG_PC:
/*TODO*///			case S2650_PC: return S.page + S.iar;
/*TODO*///			case REG_SP: return S.psu & SP;
/*TODO*///			case S2650_PS: return (S.psu << 8) | S.psl;
/*TODO*///			case S2650_R0: return S.reg[0];
/*TODO*///			case S2650_R1: return S.reg[1];
/*TODO*///			case S2650_R2: return S.reg[2];
/*TODO*///			case S2650_R3: return S.reg[3];
/*TODO*///			case S2650_R1A: return S.reg[4];
/*TODO*///			case S2650_R2A: return S.reg[5];
/*TODO*///			case S2650_R3A: return S.reg[6];
/*TODO*///			case S2650_HALT: return S.halt;
/*TODO*///			case S2650_IRQ_STATE: return S.irq_state;
/*TODO*///			case S2650_SI: return s2650_get_sense(); break;
/*TODO*///			case S2650_FO: return s2650_get_flag(); break;
/*TODO*///			case REG_PREVIOUSPC: return S.ppc; break;
/*TODO*///			default:
/*TODO*///				if( regnum <= REG_SP_CONTENTS )
/*TODO*///				{
/*TODO*///					unsigned offset = (REG_SP_CONTENTS - regnum);
/*TODO*///					if( offset < 8 )
/*TODO*///						return S.ras[offset];
/*TODO*///				}
/*TODO*///		}
/*TODO*///		return 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	void s2650_set_reg(int regnum, unsigned val)
/*TODO*///	{
/*TODO*///		switch( regnum )
/*TODO*///		{
/*TODO*///			case REG_PC:
/*TODO*///				S.page = val & PAGE;
/*TODO*///				S.iar = val & PMSK;
/*TODO*///				change_pc16(S.page + S.iar);
/*TODO*///				break;
/*TODO*///			case S2650_PC: S.page = val & PAGE; S.iar = val & PMSK; break;
/*TODO*///			case REG_SP: S.psu = (S.psu & ~SP) | (val & SP); break;
/*TODO*///			case S2650_PS: S.psl = val & 0xff; S.psu = val >> 8; break;
/*TODO*///			case S2650_R0: S.reg[0] = val; break;
/*TODO*///			case S2650_R1: S.reg[1] = val; break;
/*TODO*///			case S2650_R2: S.reg[2] = val; break;
/*TODO*///			case S2650_R3: S.reg[3] = val; break;
/*TODO*///			case S2650_R1A: S.reg[4] = val; break;
/*TODO*///			case S2650_R2A: S.reg[5] = val; break;
/*TODO*///			case S2650_R3A: S.reg[6] = val; break;
/*TODO*///			case S2650_HALT: S.halt = val; break;
/*TODO*///			case S2650_IRQ_STATE: s2650_set_irq_line(0, val); break;
/*TODO*///			case S2650_SI: s2650_set_sense(val); break;
/*TODO*///			case S2650_FO: s2650_set_flag(val); break;
/*TODO*///			default:
/*TODO*///				if( regnum <= REG_SP_CONTENTS )
/*TODO*///				{
/*TODO*///					unsigned offset = (REG_SP_CONTENTS - regnum);
/*TODO*///					if( offset < 8 )
/*TODO*///						S.ras[offset] = val;
/*TODO*///				}
/*TODO*///	    }
/*TODO*///	}
/*TODO*///	
/*TODO*///	void s2650_set_irq_line(int irqline, int state)
/*TODO*///	{
/*TODO*///		if (irqline == 1)
/*TODO*///		{
/*TODO*///			if (state == CLEAR_LINE)
/*TODO*///				s2650_set_sense(0);
/*TODO*///			else
/*TODO*///				s2650_set_sense(1);
/*TODO*///			return;
/*TODO*///		}
/*TODO*///	
/*TODO*///		S.irq_state = state;
/*TODO*///		CHECK_IRQ_LINE;
/*TODO*///	}
/*TODO*///	
/*TODO*///	void s2650_set_irq_callback(int (*callback)(int irqline))
/*TODO*///	{
/*TODO*///		S.irq_callback = callback;
/*TODO*///	}
/*TODO*///	
/*TODO*///	void s2650_set_flag(int state)
/*TODO*///	{
/*TODO*///	    if (state)
/*TODO*///	        S.psu |= FO;
/*TODO*///	    else
/*TODO*///	        S.psu &= ~FO;
/*TODO*///	}
/*TODO*///	
/*TODO*///	int s2650_get_flag(void)
/*TODO*///	{
/*TODO*///	    return (S.psu & FO) ? 1 : 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	void s2650_set_sense(int state)
/*TODO*///	{
/*TODO*///	    if (state)
/*TODO*///	        S.psu |= SI;
/*TODO*///	    else
/*TODO*///	        S.psu &= ~SI;
/*TODO*///	}
/*TODO*///	
/*TODO*///	int s2650_get_sense(void)
/*TODO*///	{
/*TODO*///		/* OR'd with Input to allow for external connections */
/*TODO*///	
/*TODO*///	    return (((S.psu & SI) ? 1 : 0) | ((cpu_readport16(S2650_SENSE_PORT) & SI) ? 1 : 0));
/*TODO*///	}
/*TODO*///	
/*TODO*///	static  int S2650_Cycles[0x100] = {
/*TODO*///		2,2,2,2, 2,2,2,2, 3,3,3,3, 4,4,4,4,
/*TODO*///		2,2,2,2, 2,2,2,2, 3,3,3,3, 3,3,3,3,
/*TODO*///		2,2,2,2, 2,2,2,2, 3,3,3,3, 4,4,4,4,
/*TODO*///		2,2,2,2, 3,3,3,3, 3,3,3,3, 3,3,3,3,
/*TODO*///		2,2,2,2, 2,2,2,2, 3,3,3,3, 4,4,4,4,
/*TODO*///		2,2,2,2, 3,3,3,3, 3,3,3,3, 3,3,3,3,
/*TODO*///		2,2,2,2, 2,2,2,2, 3,3,3,3, 4,4,4,4,
/*TODO*///		2,2,2,2, 2,2,2,2, 3,3,3,3, 3,3,3,3,
/*TODO*///		2,2,2,2, 2,2,2,2, 3,3,3,3, 4,4,4,4,
/*TODO*///		2,2,2,2, 2,2,2,2, 3,3,3,3, 3,3,3,3,
/*TODO*///		2,2,2,2, 2,2,2,2, 3,3,3,3, 4,4,4,4,
/*TODO*///		2,2,2,2, 2,2,2,2, 3,3,3,3, 3,3,3,3,
/*TODO*///		2,2,2,2, 2,2,2,2, 3,3,3,3, 4,4,4,4,
/*TODO*///		2,2,2,2, 3,3,3,3, 3,3,3,3, 3,3,3,3,
/*TODO*///		2,2,2,2, 2,2,2,2, 3,3,3,3, 4,4,4,4,
/*TODO*///		2,2,2,2, 3,3,3,3, 3,3,3,3, 3,3,3,3
/*TODO*///	};
/*TODO*///	
/*TODO*///	int s2650_execute(int cycles)
/*TODO*///	{
/*TODO*///		s2650_ICount = cycles;
/*TODO*///		do
/*TODO*///		{
/*TODO*///			S.ppc = S.page + S.iar;
/*TODO*///	
/*TODO*///			CALL_MAME_DEBUG;
/*TODO*///	
/*TODO*///			S.ir = ROP();
/*TODO*///			s2650_ICount -= S2650_Cycles[S.ir];
/*TODO*///			S.r = S.ir & 3; 		/* register / value */
/*TODO*///			switch (S.ir) {
/*TODO*///				case 0x00:		/* LODZ,0 */
/*TODO*///				case 0x01:		/* LODZ,1 */
/*TODO*///				case 0x02:		/* LODZ,2 */
/*TODO*///				case 0x03:		/* LODZ,3 */
/*TODO*///					M_LOD( R0, S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x04:		/* LODI,0 v */
/*TODO*///				case 0x05:		/* LODI,1 v */
/*TODO*///				case 0x06:		/* LODI,2 v */
/*TODO*///				case 0x07:		/* LODI,3 v */
/*TODO*///					M_LOD( S.reg[S.r], ARG() );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x08:		/* LODR,0 (*)a */
/*TODO*///				case 0x09:		/* LODR,1 (*)a */
/*TODO*///				case 0x0a:		/* LODR,2 (*)a */
/*TODO*///				case 0x0b:		/* LODR,3 (*)a */
/*TODO*///					REL_EA( S.page );
/*TODO*///					M_LOD( S.reg[S.r], RDMEM(S.ea) );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x0c:		/* LODA,0 (*)a(,X) */
/*TODO*///				case 0x0d:		/* LODA,1 (*)a(,X) */
/*TODO*///				case 0x0e:		/* LODA,2 (*)a(,X) */
/*TODO*///				case 0x0f:		/* LODA,3 (*)a(,X) */
/*TODO*///					ABS_EA();
/*TODO*///					M_LOD( S.reg[S.r], RDMEM(S.ea) );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x10:		/* illegal */
/*TODO*///				case 0x11:		/* illegal */
/*TODO*///					break;
/*TODO*///				case 0x12:		/* SPSU */
/*TODO*///					M_SPSU();
/*TODO*///					break;
/*TODO*///				case 0x13:		/* SPSL */
/*TODO*///					M_SPSL();
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x14:		/* RETC,0	(zero)	*/
/*TODO*///				case 0x15:		/* RETC,1	(plus)	*/
/*TODO*///				case 0x16:		/* RETC,2	(minus) */
/*TODO*///					M_RET( (S.psl >> 6) == S.r );
/*TODO*///					break;
/*TODO*///				case 0x17:		/* RETC,3	(always) */
/*TODO*///					M_RET( 1 );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x18:		/* BCTR,0  (*)a */
/*TODO*///				case 0x19:		/* BCTR,1  (*)a */
/*TODO*///				case 0x1a:		/* BCTR,2  (*)a */
/*TODO*///					M_BRR( (S.psl >> 6) == S.r );
/*TODO*///					break;
/*TODO*///				case 0x1b:		/* BCTR,3  (*)a */
/*TODO*///					M_BRR( 1 );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x1c:		/* BCTA,0  (*)a */
/*TODO*///				case 0x1d:		/* BCTA,1  (*)a */
/*TODO*///				case 0x1e:		/* BCTA,2  (*)a */
/*TODO*///					M_BRA( (S.psl >> 6) == S.r );
/*TODO*///					break;
/*TODO*///				case 0x1f:		/* BCTA,3  (*)a */
/*TODO*///					M_BRA( 1 );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x20:		/* EORZ,0 */
/*TODO*///				case 0x21:		/* EORZ,1 */
/*TODO*///				case 0x22:		/* EORZ,2 */
/*TODO*///				case 0x23:		/* EORZ,3 */
/*TODO*///					M_EOR( R0, S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x24:		/* EORI,0 v */
/*TODO*///				case 0x25:		/* EORI,1 v */
/*TODO*///				case 0x26:		/* EORI,2 v */
/*TODO*///				case 0x27:		/* EORI,3 v */
/*TODO*///					M_EOR( S.reg[S.r], ARG() );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x28:		/* EORR,0 (*)a */
/*TODO*///				case 0x29:		/* EORR,1 (*)a */
/*TODO*///				case 0x2a:		/* EORR,2 (*)a */
/*TODO*///				case 0x2b:		/* EORR,3 (*)a */
/*TODO*///					REL_EA( S.page );
/*TODO*///					M_EOR( S.reg[S.r], RDMEM(S.ea) );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x2c:		/* EORA,0 (*)a(,X) */
/*TODO*///				case 0x2d:		/* EORA,1 (*)a(,X) */
/*TODO*///				case 0x2e:		/* EORA,2 (*)a(,X) */
/*TODO*///				case 0x2f:		/* EORA,3 (*)a(,X) */
/*TODO*///					ABS_EA();
/*TODO*///					M_EOR( S.reg[S.r], RDMEM(S.ea) );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x30:		/* REDC,0 */
/*TODO*///				case 0x31:		/* REDC,1 */
/*TODO*///				case 0x32:		/* REDC,2 */
/*TODO*///				case 0x33:		/* REDC,3 */
/*TODO*///					S.reg[S.r] = cpu_readport16(S2650_CTRL_PORT);
/*TODO*///					SET_CC( S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x34:		/* RETE,0 */
/*TODO*///				case 0x35:		/* RETE,1 */
/*TODO*///				case 0x36:		/* RETE,2 */
/*TODO*///					M_RETE( (S.psl >> 6) == S.r );
/*TODO*///					break;
/*TODO*///				case 0x37:		/* RETE,3 */
/*TODO*///					M_RETE( 1 );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x38:		/* BSTR,0 (*)a */
/*TODO*///				case 0x39:		/* BSTR,1 (*)a */
/*TODO*///				case 0x3a:		/* BSTR,2 (*)a */
/*TODO*///					M_BSR( (S.psl >> 6) == S.r );
/*TODO*///					break;
/*TODO*///				case 0x3b:		/* BSTR,R3 (*)a */
/*TODO*///					M_BSR( 1 );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x3c:		/* BSTA,0 (*)a */
/*TODO*///				case 0x3d:		/* BSTA,1 (*)a */
/*TODO*///				case 0x3e:		/* BSTA,2 (*)a */
/*TODO*///					M_BSA( (S.psl >> 6) == S.r );
/*TODO*///					break;
/*TODO*///				case 0x3f:		/* BSTA,3 (*)a */
/*TODO*///					M_BSA( 1 );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x40:		/* HALT */
/*TODO*///					S.iar = (S.iar - 1) & PMSK;
/*TODO*///					S.halt = 1;
/*TODO*///					if (s2650_ICount > 0)
/*TODO*///						s2650_ICount = 0;
/*TODO*///					break;
/*TODO*///				case 0x41:		/* ANDZ,1 */
/*TODO*///				case 0x42:		/* ANDZ,2 */
/*TODO*///				case 0x43:		/* ANDZ,3 */
/*TODO*///					M_AND( R0, S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x44:		/* ANDI,0 v */
/*TODO*///				case 0x45:		/* ANDI,1 v */
/*TODO*///				case 0x46:		/* ANDI,2 v */
/*TODO*///				case 0x47:		/* ANDI,3 v */
/*TODO*///					M_AND( S.reg[S.r], ARG() );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x48:		/* ANDR,0 (*)a */
/*TODO*///				case 0x49:		/* ANDR,1 (*)a */
/*TODO*///				case 0x4a:		/* ANDR,2 (*)a */
/*TODO*///				case 0x4b:		/* ANDR,3 (*)a */
/*TODO*///					REL_EA( S.page );
/*TODO*///					M_AND( S.reg[S.r], RDMEM(S.ea) );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x4c:		/* ANDA,0 (*)a(,X) */
/*TODO*///				case 0x4d:		/* ANDA,1 (*)a(,X) */
/*TODO*///				case 0x4e:		/* ANDA,2 (*)a(,X) */
/*TODO*///				case 0x4f:		/* ANDA,3 (*)a(,X) */
/*TODO*///					ABS_EA();
/*TODO*///					M_AND( S.reg[S.r], RDMEM(S.ea) );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x50:		/* RRR,0 */
/*TODO*///				case 0x51:		/* RRR,1 */
/*TODO*///				case 0x52:		/* RRR,2 */
/*TODO*///				case 0x53:		/* RRR,3 */
/*TODO*///					M_RRR( S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x54:		/* REDE,0 v */
/*TODO*///				case 0x55:		/* REDE,1 v */
/*TODO*///				case 0x56:		/* REDE,2 v */
/*TODO*///				case 0x57:		/* REDE,3 v */
/*TODO*///					S.reg[S.r] = cpu_readport16( ARG() );
/*TODO*///					SET_CC(S.reg[S.r]);
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x58:		/* BRNR,0 (*)a */
/*TODO*///				case 0x59:		/* BRNR,1 (*)a */
/*TODO*///				case 0x5a:		/* BRNR,2 (*)a */
/*TODO*///				case 0x5b:		/* BRNR,3 (*)a */
/*TODO*///					M_BRR( S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x5c:		/* BRNA,0 (*)a */
/*TODO*///				case 0x5d:		/* BRNA,1 (*)a */
/*TODO*///				case 0x5e:		/* BRNA,2 (*)a */
/*TODO*///				case 0x5f:		/* BRNA,3 (*)a */
/*TODO*///					M_BRA( S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x60:		/* IORZ,0 */
/*TODO*///				case 0x61:		/* IORZ,1 */
/*TODO*///				case 0x62:		/* IORZ,2 */
/*TODO*///				case 0x63:		/* IORZ,3 */
/*TODO*///					M_IOR( R0, S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x64:		/* IORI,0 v */
/*TODO*///				case 0x65:		/* IORI,1 v */
/*TODO*///				case 0x66:		/* IORI,2 v */
/*TODO*///				case 0x67:		/* IORI,3 v */
/*TODO*///					M_IOR( S.reg[S.r], ARG() );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x68:		/* IORR,0 (*)a */
/*TODO*///				case 0x69:		/* IORR,1 (*)a */
/*TODO*///				case 0x6a:		/* IORR,2 (*)a */
/*TODO*///				case 0x6b:		/* IORR,3 (*)a */
/*TODO*///					REL_EA( S.page );
/*TODO*///					M_IOR( S.reg[S. r],RDMEM(S.ea) );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x6c:		/* IORA,0 (*)a(,X) */
/*TODO*///				case 0x6d:		/* IORA,1 (*)a(,X) */
/*TODO*///				case 0x6e:		/* IORA,2 (*)a(,X) */
/*TODO*///				case 0x6f:		/* IORA,3 (*)a(,X) */
/*TODO*///					ABS_EA();
/*TODO*///					M_IOR( S.reg[S.r], RDMEM(S.ea) );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x70:		/* REDD,0 */
/*TODO*///				case 0x71:		/* REDD,1 */
/*TODO*///				case 0x72:		/* REDD,2 */
/*TODO*///				case 0x73:		/* REDD,3 */
/*TODO*///					S.reg[S.r] = cpu_readport16(S2650_DATA_PORT);
/*TODO*///					SET_CC(S.reg[S.r]);
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x74:		/* CPSU */
/*TODO*///					M_CPSU();
/*TODO*///					break;
/*TODO*///				case 0x75:		/* CPSL */
/*TODO*///					M_CPSL();
/*TODO*///					break;
/*TODO*///				case 0x76:		/* PPSU */
/*TODO*///					M_PPSU();
/*TODO*///					break;
/*TODO*///				case 0x77:		/* PPSL */
/*TODO*///					M_PPSL();
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x78:		/* BSNR,0 (*)a */
/*TODO*///				case 0x79:		/* BSNR,1 (*)a */
/*TODO*///				case 0x7a:		/* BSNR,2 (*)a */
/*TODO*///				case 0x7b:		/* BSNR,3 (*)a */
/*TODO*///					M_BSR( S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x7c:		/* BSNA,0 (*)a */
/*TODO*///				case 0x7d:		/* BSNA,1 (*)a */
/*TODO*///				case 0x7e:		/* BSNA,2 (*)a */
/*TODO*///				case 0x7f:		/* BSNA,3 (*)a */
/*TODO*///					M_BSA( S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x80:		/* ADDZ,0 */
/*TODO*///				case 0x81:		/* ADDZ,1 */
/*TODO*///				case 0x82:		/* ADDZ,2 */
/*TODO*///				case 0x83:		/* ADDZ,3 */
/*TODO*///					M_ADD( R0,S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x84:		/* ADDI,0 v */
/*TODO*///				case 0x85:		/* ADDI,1 v */
/*TODO*///				case 0x86:		/* ADDI,2 v */
/*TODO*///				case 0x87:		/* ADDI,3 v */
/*TODO*///					M_ADD( S.reg[S.r], ARG() );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x88:		/* ADDR,0 (*)a */
/*TODO*///				case 0x89:		/* ADDR,1 (*)a */
/*TODO*///				case 0x8a:		/* ADDR,2 (*)a */
/*TODO*///				case 0x8b:		/* ADDR,3 (*)a */
/*TODO*///					REL_EA(S.page);
/*TODO*///					M_ADD( S.reg[S.r], RDMEM(S.ea) );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x8c:		/* ADDA,0 (*)a(,X) */
/*TODO*///				case 0x8d:		/* ADDA,1 (*)a(,X) */
/*TODO*///				case 0x8e:		/* ADDA,2 (*)a(,X) */
/*TODO*///				case 0x8f:		/* ADDA,3 (*)a(,X) */
/*TODO*///					ABS_EA();
/*TODO*///					M_ADD( S.reg[S.r], RDMEM(S.ea) );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x90:		/* illegal */
/*TODO*///				case 0x91:		/* illegal */
/*TODO*///					break;
/*TODO*///				case 0x92:		/* LPSU */
/*TODO*///					S.psu = R0 & ~PSU34;
/*TODO*///					break;
/*TODO*///				case 0x93:		/* LPSL */
/*TODO*///					/* change register set ? */
/*TODO*///					if ((S.psl ^ R0) & RS)
/*TODO*///						SWAP_REGS;
/*TODO*///					S.psl = R0;
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x94:		/* DAR,0 */
/*TODO*///				case 0x95:		/* DAR,1 */
/*TODO*///				case 0x96:		/* DAR,2 */
/*TODO*///				case 0x97:		/* DAR,3 */
/*TODO*///					M_DAR( S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x98:		/* BCFR,0 (*)a */
/*TODO*///				case 0x99:		/* BCFR,1 (*)a */
/*TODO*///				case 0x9a:		/* BCFR,2 (*)a */
/*TODO*///					M_BRR( (S.psl >> 6) != S.r );
/*TODO*///					break;
/*TODO*///				case 0x9b:		/* ZBRR    (*)a */
/*TODO*///					M_ZBRR();
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0x9c:		/* BCFA,0 (*)a */
/*TODO*///				case 0x9d:		/* BCFA,1 (*)a */
/*TODO*///				case 0x9e:		/* BCFA,2 (*)a */
/*TODO*///					M_BRA( (S.psl >> 6) != S.r );
/*TODO*///					break;
/*TODO*///				case 0x9f:		/* BXA	   (*)a */
/*TODO*///					M_BXA();
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xa0:		/* SUBZ,0 */
/*TODO*///				case 0xa1:		/* SUBZ,1 */
/*TODO*///				case 0xa2:		/* SUBZ,2 */
/*TODO*///				case 0xa3:		/* SUBZ,3 */
/*TODO*///					M_SUB( R0, S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xa4:		/* SUBI,0 v */
/*TODO*///				case 0xa5:		/* SUBI,1 v */
/*TODO*///				case 0xa6:		/* SUBI,2 v */
/*TODO*///				case 0xa7:		/* SUBI,3 v */
/*TODO*///					M_SUB( S.reg[S.r], ARG() );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xa8:		/* SUBR,0 (*)a */
/*TODO*///				case 0xa9:		/* SUBR,1 (*)a */
/*TODO*///				case 0xaa:		/* SUBR,2 (*)a */
/*TODO*///				case 0xab:		/* SUBR,3 (*)a */
/*TODO*///					REL_EA(S.page);
/*TODO*///					M_SUB( S.reg[S.r], RDMEM(S.ea) );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xac:		/* SUBA,0 (*)a(,X) */
/*TODO*///				case 0xad:		/* SUBA,1 (*)a(,X) */
/*TODO*///				case 0xae:		/* SUBA,2 (*)a(,X) */
/*TODO*///				case 0xaf:		/* SUBA,3 (*)a(,X) */
/*TODO*///					ABS_EA();
/*TODO*///					M_SUB( S.reg[S.r], RDMEM(S.ea) );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xb0:		/* WRTC,0 */
/*TODO*///				case 0xb1:		/* WRTC,1 */
/*TODO*///				case 0xb2:		/* WRTC,2 */
/*TODO*///				case 0xb3:		/* WRTC,3 */
/*TODO*///					cpu_writeport16(S2650_CTRL_PORT,S.reg[S.r]);
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xb4:		/* TPSU */
/*TODO*///					M_TPSU();
/*TODO*///					break;
/*TODO*///				case 0xb5:		/* TPSL */
/*TODO*///					M_TPSL();
/*TODO*///					break;
/*TODO*///				case 0xb6:		/* illegal */
/*TODO*///				case 0xb7:		/* illegal */
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xb8:		/* BSFR,0 (*)a */
/*TODO*///				case 0xb9:		/* BSFR,1 (*)a */
/*TODO*///				case 0xba:		/* BSFR,2 (*)a */
/*TODO*///					M_BSR( (S.psl >> 6) != S.r );
/*TODO*///					break;
/*TODO*///				case 0xbb:		/* ZBSR    (*)a */
/*TODO*///					M_ZBSR();
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xbc:		/* BSFA,0 (*)a */
/*TODO*///				case 0xbd:		/* BSFA,1 (*)a */
/*TODO*///				case 0xbe:		/* BSFA,2 (*)a */
/*TODO*///					M_BSA( (S.psl >> 6) != S.r );
/*TODO*///					break;
/*TODO*///				case 0xbf:		/* BSXA    (*)a */
/*TODO*///					M_BSXA();
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xc0:		/* NOP */
/*TODO*///					break;
/*TODO*///				case 0xc1:		/* STRZ,1 */
/*TODO*///				case 0xc2:		/* STRZ,2 */
/*TODO*///				case 0xc3:		/* STRZ,3 */
/*TODO*///					M_LOD( S.reg[S.r], R0 );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xc4:		/* illegal */
/*TODO*///				case 0xc5:		/* illegal */
/*TODO*///				case 0xc6:		/* illegal */
/*TODO*///				case 0xc7:		/* illegal */
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xc8:		/* STRR,0 (*)a */
/*TODO*///				case 0xc9:		/* STRR,1 (*)a */
/*TODO*///				case 0xca:		/* STRR,2 (*)a */
/*TODO*///				case 0xcb:		/* STRR,3 (*)a */
/*TODO*///					REL_EA(S.page);
/*TODO*///					M_STR( S.ea, S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xcc:		/* STRA,0 (*)a(,X) */
/*TODO*///				case 0xcd:		/* STRA,1 (*)a(,X) */
/*TODO*///				case 0xce:		/* STRA,2 (*)a(,X) */
/*TODO*///				case 0xcf:		/* STRA,3 (*)a(,X) */
/*TODO*///					ABS_EA();
/*TODO*///					M_STR( S.ea, S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xd0:		/* RRL,0 */
/*TODO*///				case 0xd1:		/* RRL,1 */
/*TODO*///				case 0xd2:		/* RRL,2 */
/*TODO*///				case 0xd3:		/* RRL,3 */
/*TODO*///					M_RRL( S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xd4:		/* WRTE,0 v */
/*TODO*///				case 0xd5:		/* WRTE,1 v */
/*TODO*///				case 0xd6:		/* WRTE,2 v */
/*TODO*///				case 0xd7:		/* WRTE,3 v */
/*TODO*///					cpu_writeport16( ARG(), S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xd8:		/* BIRR,0 (*)a */
/*TODO*///				case 0xd9:		/* BIRR,1 (*)a */
/*TODO*///				case 0xda:		/* BIRR,2 (*)a */
/*TODO*///				case 0xdb:		/* BIRR,3 (*)a */
/*TODO*///					M_BRR( ++S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xdc:		/* BIRA,0 (*)a */
/*TODO*///				case 0xdd:		/* BIRA,1 (*)a */
/*TODO*///				case 0xde:		/* BIRA,2 (*)a */
/*TODO*///				case 0xdf:		/* BIRA,3 (*)a */
/*TODO*///					M_BRA( ++S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xe0:		/* COMZ,0 */
/*TODO*///				case 0xe1:		/* COMZ,1 */
/*TODO*///				case 0xe2:		/* COMZ,2 */
/*TODO*///				case 0xe3:		/* COMZ,3 */
/*TODO*///					M_COM( R0, S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xe4:		/* COMI,0 v */
/*TODO*///				case 0xe5:		/* COMI,1 v */
/*TODO*///				case 0xe6:		/* COMI,2 v */
/*TODO*///				case 0xe7:		/* COMI,3 v */
/*TODO*///					M_COM( S.reg[S.r], ARG() );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xe8:		/* COMR,0 (*)a */
/*TODO*///				case 0xe9:		/* COMR,1 (*)a */
/*TODO*///				case 0xea:		/* COMR,2 (*)a */
/*TODO*///				case 0xeb:		/* COMR,3 (*)a */
/*TODO*///					REL_EA(S.page);
/*TODO*///					M_COM( S.reg[S.r], RDMEM(S.ea) );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xec:		/* COMA,0 (*)a(,X) */
/*TODO*///				case 0xed:		/* COMA,1 (*)a(,X) */
/*TODO*///				case 0xee:		/* COMA,2 (*)a(,X) */
/*TODO*///				case 0xef:		/* COMA,3 (*)a(,X) */
/*TODO*///					ABS_EA();
/*TODO*///					M_COM( S.reg[S.r], RDMEM(S.ea) );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xf0:		/* WRTD,0 */
/*TODO*///				case 0xf1:		/* WRTD,1 */
/*TODO*///				case 0xf2:		/* WRTD,2 */
/*TODO*///				case 0xf3:		/* WRTD,3 */
/*TODO*///					cpu_writeport16(S2650_DATA_PORT, S.reg[S.r]);
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xf4:		/* TMI,0  v */
/*TODO*///				case 0xf5:		/* TMI,1  v */
/*TODO*///				case 0xf6:		/* TMI,2  v */
/*TODO*///				case 0xf7:		/* TMI,3  v */
/*TODO*///					M_TMI( S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xf8:		/* BDRR,0 (*)a */
/*TODO*///				case 0xf9:		/* BDRR,1 (*)a */
/*TODO*///				case 0xfa:		/* BDRR,2 (*)a */
/*TODO*///				case 0xfb:		/* BDRR,3 (*)a */
/*TODO*///					M_BRR( --S.reg[S.r] );
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case 0xfc:		/* BDRA,0 (*)a */
/*TODO*///				case 0xfd:		/* BDRA,1 (*)a */
/*TODO*///				case 0xfe:		/* BDRA,2 (*)a */
/*TODO*///				case 0xff:		/* BDRA,3 (*)a */
/*TODO*///					M_BRA( --S.reg[S.r] );
/*TODO*///					break;
/*TODO*///			}
/*TODO*///		} while( s2650_ICount > 0 );
/*TODO*///	
/*TODO*///		return cycles - s2650_ICount;
/*TODO*///	}
/*TODO*///	#if 0
/*TODO*///	void s2650_state_save(void *file)
/*TODO*///	{
/*TODO*///		int cpu = cpu_getactivecpu();
/*TODO*///		state_save_UINT16(file,"s2650",cpu,"PAGE",&S.page,1);
/*TODO*///		state_save_UINT16(file,"s2650",cpu,"IAR",&S.iar,1);
/*TODO*///		state_save_UINT8(file,"s2650",cpu,"PSL",&S.psl,1);
/*TODO*///		state_save_UINT8(file,"s2650",cpu,"PSU",&S.psu,1);
/*TODO*///		state_save_UINT8(file,"s2650",cpu,"REG",S.reg,7);
/*TODO*///		state_save_UINT8(file,"s2650",cpu,"HALT",&S.halt,1);
/*TODO*///		state_save_UINT16(file,"s2650",cpu,"RAS",S.ras,8);
/*TODO*///		state_save_UINT8(file,"s2650",cpu,"IRQ_STATE",&S.irq_state,1);
/*TODO*///	}
/*TODO*///	
/*TODO*///	void s2650_state_load(void *file)
/*TODO*///	{
/*TODO*///		int cpu = cpu_getactivecpu();
/*TODO*///		state_load_UINT16(file,"s2650",cpu,"PAGE",&S.page,1);
/*TODO*///		state_load_UINT16(file,"s2650",cpu,"IAR",&S.iar,1);
/*TODO*///		state_load_UINT8(file,"s2650",cpu,"PSL",&S.psl,1);
/*TODO*///		state_load_UINT8(file,"s2650",cpu,"PSU",&S.psu,1);
/*TODO*///		state_load_UINT8(file,"s2650",cpu,"REG",S.reg,7);
/*TODO*///		state_load_UINT8(file,"s2650",cpu,"HALT",&S.halt,1);
/*TODO*///		state_load_UINT16(file,"s2650",cpu,"RAS",S.ras,8);
/*TODO*///		state_load_UINT8(file,"s2650",cpu,"IRQ_STATE",&S.irq_state,1);
/*TODO*///	}
/*TODO*///	#endif
/*TODO*///	/****************************************************************************
/*TODO*///	 * Return a formatted string for a register
/*TODO*///	 ****************************************************************************/
	public String s2650_info(Object context, int regnum)
	{
/*TODO*///		static char buffer[16][47+1];
/*TODO*///		static int which = 0;
/*TODO*///		s2650_Regs *r = context;
/*TODO*///	
/*TODO*///		which = (which+1) % 16;
/*TODO*///		buffer[which][0] = '\0';
/*TODO*///	
/*TODO*///	    if (context == 0)
/*TODO*///			r = &S;
/*TODO*///	
	    switch( regnum )
            {
/*TODO*///			case CPU_INFO_FLAGS:
/*TODO*///			case CPU_INFO_REG+S2650_PC: sprintf(buffer[which], "PC:%04X", r->page + r->iar); break;
/*TODO*///			case CPU_INFO_REG+S2650_PS: sprintf(buffer[which], "PS:%02X%02X", r->psu, r->psl); break;
/*TODO*///			case CPU_INFO_REG+S2650_R0: sprintf(buffer[which], "R0:%02X", r->reg[0]); break;
/*TODO*///			case CPU_INFO_REG+S2650_R1: sprintf(buffer[which], "R1:%02X", r->reg[1]); break;
/*TODO*///			case CPU_INFO_REG+S2650_R2: sprintf(buffer[which], "R2:%02X", r->reg[2]); break;
/*TODO*///			case CPU_INFO_REG+S2650_R3: sprintf(buffer[which], "R3:%02X", r->reg[3]); break;
/*TODO*///			case CPU_INFO_REG+S2650_R1A: sprintf(buffer[which], "R1'%02X", r->reg[4]); break;
/*TODO*///			case CPU_INFO_REG+S2650_R2A: sprintf(buffer[which], "R2'%02X", r->reg[5]); break;
/*TODO*///			case CPU_INFO_REG+S2650_R3A: sprintf(buffer[which], "R3'%02X", r->reg[6]); break;
/*TODO*///			case CPU_INFO_REG+S2650_HALT: sprintf(buffer[which], "HALT:%X", r->halt); break;
/*TODO*///			case CPU_INFO_REG+S2650_IRQ_STATE: sprintf(buffer[which], "IRQ:%X", r->irq_state); break;
/*TODO*///			case CPU_INFO_REG+S2650_SI: sprintf(buffer[which], "SI:%X", (r->psu & SI) ? 1 : 0); break;
/*TODO*///			case CPU_INFO_REG+S2650_FO: sprintf(buffer[which], "FO:%X", (r->psu & FO) ? 1 : 0); break;
/*TODO*///				sprintf(buffer[which], "%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c",
/*TODO*///					r->psu & 0x80 ? 'S':'.',
/*TODO*///					r->psu & 0x40 ? 'O':'.',
/*TODO*///					r->psu & 0x20 ? 'I':'.',
/*TODO*///					r->psu & 0x10 ? '?':'.',
/*TODO*///					r->psu & 0x08 ? '?':'.',
/*TODO*///					r->psu & 0x04 ? 's':'.',
/*TODO*///					r->psu & 0x02 ? 's':'.',
/*TODO*///					r->psu & 0x01 ? 's':'.',
/*TODO*///	                r->psl & 0x80 ? 'M':'.',
/*TODO*///					r->psl & 0x40 ? 'P':'.',
/*TODO*///					r->psl & 0x20 ? 'H':'.',
/*TODO*///					r->psl & 0x10 ? 'R':'.',
/*TODO*///					r->psl & 0x08 ? 'W':'.',
/*TODO*///					r->psl & 0x04 ? 'V':'.',
/*TODO*///					r->psl & 0x02 ? '2':'.',
/*TODO*///					r->psl & 0x01 ? 'C':'.');
/*TODO*///				break;
			case CPU_INFO_NAME: return "S2650";
			case CPU_INFO_FAMILY: return "Signetics 2650";
			case CPU_INFO_VERSION: return "1.1";
			case CPU_INFO_FILE: return "s2650.java";
			case CPU_INFO_CREDITS: return "Written by Juergen Buchmueller for use with MAME";
/*TODO*///			case CPU_INFO_REG_LAYOUT: return (const char *)s2650_reg_layout;
/*TODO*///			case CPU_INFO_WIN_LAYOUT: return (const char *)s2650_win_layout;
            }
/*TODO*///		return buffer[which];
            throw new UnsupportedOperationException("Not supported yet.");
	}
/*TODO*///	
/*TODO*///	unsigned s2650_dasm(char *buffer, unsigned pc)
/*TODO*///	{
/*TODO*///	#ifdef MAME_DEBUG
/*TODO*///	    return Dasm2650(buffer,pc);
/*TODO*///	#else
/*TODO*///		sprintf( buffer, "$%02X", cpu_readop(pc) );
/*TODO*///		return 1;
/*TODO*///	#endif
/*TODO*///	}

}
