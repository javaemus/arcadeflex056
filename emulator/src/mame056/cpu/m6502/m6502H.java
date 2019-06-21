/**
 * ported to v0.56
 */
package mame056.cpu.m6502;

public class m6502H {

    /*TODO*///
/*TODO*////* set to 1 to test cur_mrhard/cur_wmhard to avoid calls */
/*TODO*///#define FAST_MEMORY 0
/*TODO*///
/*TODO*///#define SUBTYPE_6502	0
/*TODO*///#if (HAS_M65C02)
/*TODO*///#define SUBTYPE_65C02	1
/*TODO*///#endif
/*TODO*///#if (HAS_M6510)
/*TODO*///#define SUBTYPE_6510	2
/*TODO*///#endif
/*TODO*///#if (HAS_N2A03)
/*TODO*///#define SUBTYPE_2A03	3
/*TODO*///#endif
/*TODO*///#if (HAS_M65SC02)
/*TODO*///#define SUBTYPE_65SC02	4
/*TODO*///#endif
/*TODO*///
/*TODO*///enum {
/*TODO*///	M6502_PC=1, M6502_S, M6502_P, M6502_A, M6502_X, M6502_Y,
/*TODO*///	M6502_EA, M6502_ZP, M6502_NMI_STATE, M6502_IRQ_STATE, M6502_SO_STATE,
/*TODO*///	M6502_SUBTYPE
/*TODO*///};
/*TODO*///
    public static final int M6502_IRQ_LINE = 0;
    /* use cpu_set_irq_line(cpu, M6502_SET_OVERFLOW, level)
   to change level of the so input line
   positiv edge sets overflow flag */
    public static final int M6502_SET_OVERFLOW = 1;
    /*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * The 6510
/*TODO*/// ****************************************************************************/
/*TODO*///#if (HAS_M6510)
/*TODO*///#define M6510_A 						M6502_A
/*TODO*///#define M6510_X 						M6502_X
/*TODO*///#define M6510_Y 						M6502_Y
/*TODO*///#define M6510_S 						M6502_S
/*TODO*///#define M6510_PC						M6502_PC
/*TODO*///#define M6510_P 						M6502_P
/*TODO*///#define M6510_EA						M6502_EA
/*TODO*///#define M6510_ZP						M6502_ZP
/*TODO*///#define M6510_NMI_STATE 				M6502_NMI_STATE
/*TODO*///#define M6510_IRQ_STATE 				M6502_IRQ_STATE
/*TODO*///
/*TODO*///#define M6510_IRQ_LINE					M6502_IRQ_LINE
/*TODO*///
/*TODO*///#define m6510_ICount					m6502_ICount
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///#endif
/*TODO*///
/*TODO*///#ifdef HAS_M6510T
/*TODO*///#define M6510T_A						M6502_A
/*TODO*///#define M6510T_X						M6502_X
/*TODO*///#define M6510T_Y						M6502_Y
/*TODO*///#define M6510T_S						M6502_S
/*TODO*///#define M6510T_PC						M6502_PC
/*TODO*///#define M6510T_P						M6502_P
/*TODO*///#define M6510T_EA						M6502_EA
/*TODO*///#define M6510T_ZP						M6502_ZP
/*TODO*///#define M6510T_NMI_STATE				M6502_NMI_STATE
/*TODO*///#define M6510T_IRQ_STATE				M6502_IRQ_STATE
/*TODO*///
/*TODO*///#define M6510T_IRQ_LINE					M6502_IRQ_LINE
/*TODO*///
/*TODO*///#define m6510t_ICount                   m6502_ICount
/*TODO*///
/*TODO*///#define m6510t_init m6510_init
/*TODO*///#define m6510t_reset m6510_reset
/*TODO*///#define m6510t_exit m6510_exit
/*TODO*///#define m6510t_execute m6510_execute
/*TODO*///#define m6510t_get_context m6510_get_context
/*TODO*///#define m6510t_set_context m6510_set_context
/*TODO*///#define m6510t_get_reg m6510_get_reg
/*TODO*///#define m6510t_set_reg m6510_set_reg
/*TODO*///#define m6510t_set_irq_line m6510_set_irq_line
/*TODO*///#define m6510t_set_irq_callback m6510_set_irq_callback
/*TODO*///#define m6510t_state_save m6510_state_save
/*TODO*///#define m6510t_state_load m6510_state_load
/*TODO*///extern const char *m6510t_info(void *context, int regnum);
/*TODO*///#define m6510t_dasm m6510_dasm
/*TODO*///#endif
/*TODO*///
/*TODO*///#ifdef HAS_M7501
/*TODO*///#define M7501_A 						M6502_A
/*TODO*///#define M7501_X 						M6502_X
/*TODO*///#define M7501_Y 						M6502_Y
/*TODO*///#define M7501_S 						M6502_S
/*TODO*///#define M7501_PC						M6502_PC
/*TODO*///#define M7501_P 						M6502_P
/*TODO*///#define M7501_EA						M6502_EA
/*TODO*///#define M7501_ZP						M6502_ZP
/*TODO*///#define M7501_NMI_STATE 				M6502_NMI_STATE
/*TODO*///#define M7501_IRQ_STATE 				M6502_IRQ_STATE
/*TODO*///
/*TODO*///#define M7501_IRQ_LINE					M6502_IRQ_LINE
/*TODO*///
/*TODO*///#define m7501_ICount                    m6502_ICount
/*TODO*///
/*TODO*///#define m7501_init m6510_init
/*TODO*///#define m7501_reset m6510_reset
/*TODO*///#define m7501_exit m6510_exit
/*TODO*///#define m7501_execute m6510_execute
/*TODO*///#define m7501_get_context m6510_get_context
/*TODO*///#define m7501_set_context m6510_set_context
/*TODO*///#define m7501_get_reg m6510_get_reg
/*TODO*///#define m7501_set_reg m6510_set_reg
/*TODO*///#define m7501_set_irq_line m6510_set_irq_line
/*TODO*///#define m7501_set_irq_callback m6510_set_irq_callback
/*TODO*///#define m7501_state_save m6510_state_save
/*TODO*///#define m7501_state_load m6510_state_load
/*TODO*///extern const char *m7501_info(void *context, int regnum);
/*TODO*///#define m7501_dasm m6510_dasm
/*TODO*///#endif
/*TODO*///
/*TODO*///#ifdef HAS_M8502
/*TODO*///#define M8502_A 						M6502_A
/*TODO*///#define M8502_X 						M6502_X
/*TODO*///#define M8502_Y 						M6502_Y
/*TODO*///#define M8502_S 						M6502_S
/*TODO*///#define M8502_PC						M6502_PC
/*TODO*///#define M8502_P 						M6502_P
/*TODO*///#define M8502_EA						M6502_EA
/*TODO*///#define M8502_ZP						M6502_ZP
/*TODO*///#define M8502_NMI_STATE 				M6502_NMI_STATE
/*TODO*///#define M8502_IRQ_STATE 				M6502_IRQ_STATE
/*TODO*///
/*TODO*///#define M8502_IRQ_LINE					M6502_IRQ_LINE
/*TODO*///
/*TODO*///#define m8502_ICount                    m6502_ICount
/*TODO*///
/*TODO*///#define m8502_init m6510_init
/*TODO*///#define m8502_reset m6510_reset
/*TODO*///#define m8502_exit m6510_exit
/*TODO*///#define m8502_execute m6510_execute
/*TODO*///#define m8502_get_context m6510_get_context
/*TODO*///#define m8502_set_context m6510_set_context
/*TODO*///#define m8502_get_reg m6510_get_reg
/*TODO*///#define m8502_set_reg m6510_set_reg
/*TODO*///#define m8502_set_irq_line m6510_set_irq_line
/*TODO*///#define m8502_set_irq_callback m6510_set_irq_callback
/*TODO*///#define m8502_state_save m6510_state_save
/*TODO*///#define m8502_state_load m6510_state_load
/*TODO*///extern const char *m8502_info(void *context, int regnum);
/*TODO*///#define m8502_dasm m6510_dasm
/*TODO*///#endif
/*TODO*///
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * The 2A03 (NES 6502 without decimal mode ADC/SBC)
/*TODO*/// ****************************************************************************/
/*TODO*///#if (HAS_N2A03)
/*TODO*///#define N2A03_A 						M6502_A
/*TODO*///#define N2A03_X 						M6502_X
/*TODO*///#define N2A03_Y 						M6502_Y
/*TODO*///#define N2A03_S 						M6502_S
/*TODO*///#define N2A03_PC						M6502_PC
/*TODO*///#define N2A03_P 						M6502_P
/*TODO*///#define N2A03_EA						M6502_EA
/*TODO*///#define N2A03_ZP						M6502_ZP
/*TODO*///#define N2A03_NMI_STATE 				M6502_NMI_STATE
/*TODO*///#define N2A03_IRQ_STATE 				M6502_IRQ_STATE
/*TODO*///
/*TODO*///#define N2A03_IRQ_LINE					M6502_IRQ_LINE
/*TODO*///
/*TODO*///#define n2a03_ICount					m6502_ICount
/*TODO*///
/*TODO*///extern void n2a03_init(void);
/*TODO*///extern void n2a03_reset(void *param);
/*TODO*///extern void n2a03_exit(void);
/*TODO*///extern int	n2a03_execute(int cycles);
/*TODO*///extern unsigned n2a03_get_context(void *dst);
/*TODO*///extern void n2a03_set_context(void *src);
/*TODO*///extern unsigned n2a03_get_reg(int regnum);
/*TODO*///extern void n2a03_set_reg (int regnum, unsigned val);
/*TODO*///extern void n2a03_set_irq_line(int irqline, int state);
/*TODO*///extern void n2a03_set_irq_callback(int (*callback)(int irqline));
/*TODO*///extern const char *n2a03_info(void *context, int regnum);
/*TODO*///extern unsigned n2a03_dasm(char *buffer, unsigned pc);
/*TODO*///
/*TODO*///
    public static final double N2A03_DEFAULTCLOCK = (21477272.724 / 12);
    /*TODO*///
/*TODO*////* The N2A03 is integrally tied to its PSG (they're on the same die).
/*TODO*///   Bit 7 of address $4011 (the PSG's DPCM control register), when set,
/*TODO*///   causes an IRQ to be generated.  This function allows the IRQ to be called
/*TODO*///   from the PSG core when such an occasion arises. */
/*TODO*///extern void n2a03_irq(void);
/*TODO*///#endif
/*TODO*///
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * The 65C02
/*TODO*/// ****************************************************************************/
/*TODO*///#if (HAS_M65C02)
/*TODO*///#define M65C02_A						M6502_A
/*TODO*///#define M65C02_X						M6502_X
/*TODO*///#define M65C02_Y						M6502_Y
/*TODO*///#define M65C02_S						M6502_S
/*TODO*///#define M65C02_PC						M6502_PC
/*TODO*///#define M65C02_P						M6502_P
/*TODO*///#define M65C02_EA						M6502_EA
/*TODO*///#define M65C02_ZP						M6502_ZP
/*TODO*///#define M65C02_NMI_STATE				M6502_NMI_STATE
/*TODO*///#define M65C02_IRQ_STATE				M6502_IRQ_STATE
/*TODO*///
/*TODO*///#define M65C02_IRQ_LINE					M6502_IRQ_LINE
/*TODO*///
/*TODO*///#define m65c02_ICount					m6502_ICount
/*TODO*///
/*TODO*///extern void m65c02_init(void);
/*TODO*///extern void m65c02_reset(void *param);
/*TODO*///extern void m65c02_exit(void);
/*TODO*///extern int	m65c02_execute(int cycles);
/*TODO*///extern unsigned m65c02_get_context(void *dst);
/*TODO*///extern void m65c02_set_context(void *src);
/*TODO*///extern unsigned m65c02_get_reg(int regnum);
/*TODO*///extern void m65c02_set_reg(int regnum, unsigned val);
/*TODO*///extern void m65c02_set_irq_line(int irqline, int state);
/*TODO*///extern void m65c02_set_irq_callback(int (*callback)(int irqline));
/*TODO*///extern const char *m65c02_info(void *context, int regnum);
/*TODO*///extern unsigned m65c02_dasm(char *buffer, unsigned pc);
/*TODO*///#endif
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * The 65SC02
/*TODO*/// ****************************************************************************/
/*TODO*///#if (HAS_M65SC02)
/*TODO*///#define M65SC02_A						M6502_A
/*TODO*///#define M65SC02_X						M6502_X
/*TODO*///#define M65SC02_Y						M6502_Y
/*TODO*///#define M65SC02_S						M6502_S
/*TODO*///#define M65SC02_PC						M6502_PC
/*TODO*///#define M65SC02_P						M6502_P
/*TODO*///#define M65SC02_EA						M6502_EA
/*TODO*///#define M65SC02_ZP						M6502_ZP
/*TODO*///#define M65SC02_NMI_STATE				M6502_NMI_STATE
/*TODO*///#define M65SC02_IRQ_STATE				M6502_IRQ_STATE
/*TODO*///
/*TODO*///#define M65SC02_IRQ_LINE				M6502_IRQ_LINE
/*TODO*///
/*TODO*///#define m65sc02_ICount					m6502_ICount
/*TODO*///
/*TODO*///extern void m65sc02_init(void);
/*TODO*///extern void m65sc02_reset(void *param);
/*TODO*///extern void m65sc02_exit(void);
/*TODO*///extern int	m65sc02_execute(int cycles);
/*TODO*///extern unsigned m65sc02_get_context(void *dst);
/*TODO*///extern void m65sc02_set_context(void *src);
/*TODO*///extern unsigned m65sc02_get_reg(int regnum);
/*TODO*///extern void m65sc02_set_reg(int regnum, unsigned val);
/*TODO*///extern void m65sc02_set_irq_line(int irqline, int state);
/*TODO*///extern void m65sc02_set_irq_callback(int (*callback)(int irqline));
/*TODO*///extern const char *m65sc02_info(void *context, int regnum);
/*TODO*///extern unsigned m65sc02_dasm(char *buffer, unsigned pc);
/*TODO*///#endif
/*TODO*///
/*TODO*///#ifdef MAME_DEBUG
/*TODO*///extern unsigned Dasm6502( char *dst, unsigned pc );
/*TODO*///#endif
/*TODO*///
/*TODO*///#endif /* _M6502_H */
/*TODO*///
/*TODO*///    
}
