/**
 * ported to v0.56
 */
package mame056.cpu.m6800;

public class m6800H {

    /*TODO*////*** m6800: Portable 6800 class emulator *************************************/
/*TODO*///
/*TODO*///#ifndef _M6800_H
/*TODO*///#define _M6800_H
/*TODO*///
/*TODO*///#include "osd_cpu.h"
/*TODO*///#include "memory.h"
/*TODO*///#include "cpuintrf.h"
/*TODO*///
/*TODO*///enum {
/*TODO*///	M6800_PC=1, M6800_S, M6800_A, M6800_B, M6800_X, M6800_CC,
/*TODO*///	M6800_WAI_STATE, M6800_NMI_STATE, M6800_IRQ_STATE };
/*TODO*///
    public static int M6800_WAI = 8;/* set when WAI is waiting for an interrupt */
    public static int M6800_SLP = 0x10;/* HD63701 only */

    public static final int M6800_IRQ_LINE = 0;/* IRQ line number */
    public static final int M6800_TIN_LINE = 1;/* P20/Tin Input Capture line (eddge sense)     */
 /* Active eddge is selecrable by internal reg.  */
 /* raise eddge : CLEAR_LINE  -> ASSERT_LINE     */
 /* fall  eddge : ASSERT_LINE -> CLEAR_LINE      */
 /* it is usuali to use PULSE_LINE state         */

 /*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * For now make the 6801 using the m6800 variables and functions
/*TODO*/// ****************************************************************************/
/*TODO*///#if (HAS_M6801)
/*TODO*///#define M6801_A 					M6800_A
/*TODO*///#define M6801_B 					M6800_B
/*TODO*///#define M6801_PC					M6800_PC
/*TODO*///#define M6801_S 					M6800_S
/*TODO*///#define M6801_X 					M6800_X
/*TODO*///#define M6801_CC					M6800_CC
/*TODO*///#define M6801_WAI_STATE 			M6800_WAI_STATE
/*TODO*///#define M6801_NMI_STATE 			M6800_NMI_STATE
/*TODO*///#define M6801_IRQ_STATE 			M6800_IRQ_STATE
/*TODO*///
/*TODO*///#define M6801_WAI					M6800_WAI
/*TODO*///#define M6801_IRQ_LINE				M6800_IRQ_LINE
/*TODO*///
/*TODO*///#define m6801_ICount				m6800_ICount
/*TODO*///
/*TODO*///#endif
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * For now make the 6802 using the m6800 variables and functions
/*TODO*/// ****************************************************************************/
/*TODO*///#if (HAS_M6802)
/*TODO*///#define M6802_A 					M6800_A
/*TODO*///#define M6802_B 					M6800_B
/*TODO*///#define M6802_PC					M6800_PC
/*TODO*///#define M6802_S 					M6800_S
/*TODO*///#define M6802_X 					M6800_X
/*TODO*///#define M6802_CC					M6800_CC
/*TODO*///    public static final int M6802_WAI_STATE = M6800_WAI_STATE;
/*TODO*///    public static final int M6802_NMI_STATE = M6800_NMI_STATE;
/*TODO*///    public static final int M6802_IRQ_STATE = M6800_IRQ_STATE;

    public static final int M6802_WAI = M6800_WAI;
    public static final int M6802_IRQ_LINE = M6800_IRQ_LINE;
    /*TODO*///
/*TODO*///#define m6802_ICount				m6800_ICount
/*TODO*///
/*TODO*///#endif
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * For now make the 6803 using the m6800 variables and functions
/*TODO*/// ****************************************************************************/
/*TODO*///#if (HAS_M6803)
/*TODO*///#define M6803_A 					M6800_A
/*TODO*///#define M6803_B 					M6800_B
/*TODO*///#define M6803_PC					M6800_PC
/*TODO*///#define M6803_S 					M6800_S
/*TODO*///#define M6803_X 					M6800_X
/*TODO*///#define M6803_CC					M6800_CC
/*TODO*///#define M6803_WAI_STATE 			M6800_WAI_STATE
/*TODO*///#define M6803_NMI_STATE 			M6800_NMI_STATE
/*TODO*///#define M6803_IRQ_STATE 			M6800_IRQ_STATE
/*TODO*///
/*TODO*///#define M6803_WAI					M6800_WAI
/*TODO*///#define M6803_IRQ_LINE				M6800_IRQ_LINE
/*TODO*///#define M6803_TIN_LINE				M6800_TIN_LINE
/*TODO*///
/*TODO*///#define m6803_ICount				m6800_ICount
/*TODO*///
/*TODO*///#endif
/*TODO*///

    public static final int M6803_DDR1 = 0x00;
    public static final int M6803_DDR2 = 0x01;

    public static final int M6803_PORT1 = 0x100;
    public static final int M6803_PORT2 = 0x101;

    /*TODO*////****************************************************************************
/*TODO*/// * For now make the 6808 using the m6800 variables and functions
/*TODO*/// ****************************************************************************/
/*TODO*///#if (HAS_M6808)
/*TODO*///#define M6808_A 					M6800_A
/*TODO*///#define M6808_B 					M6800_B
/*TODO*///#define M6808_PC					M6800_PC
/*TODO*///#define M6808_S 					M6800_S
/*TODO*///#define M6808_X 					M6800_X
/*TODO*///#define M6808_CC					M6800_CC
/*TODO*///#define M6808_WAI_STATE 			M6800_WAI_STATE
/*TODO*///#define M6808_NMI_STATE 			M6800_NMI_STATE
/*TODO*///#define M6808_IRQ_STATE 			M6800_IRQ_STATE
/*TODO*///
/*TODO*///#define M6808_WAI                   M6800_WAI
    public static final int M6808_IRQ_LINE = M6800_IRQ_LINE;
    /*TODO*///
/*TODO*///#define m6808_ICount                m6800_ICount
/*TODO*///
/*TODO*///#endif
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * For now make the HD63701 using the m6800 variables and functions
/*TODO*/// ****************************************************************************/
/*TODO*///#if (HAS_HD63701)
/*TODO*///#define HD63701_A					 M6800_A
/*TODO*///#define HD63701_B					 M6800_B
/*TODO*///#define HD63701_PC					 M6800_PC
/*TODO*///#define HD63701_S					 M6800_S
/*TODO*///#define HD63701_X					 M6800_X
/*TODO*///#define HD63701_CC					 M6800_CC
/*TODO*///#define HD63701_WAI_STATE			 M6800_WAI_STATE
/*TODO*///#define HD63701_NMI_STATE			 M6800_NMI_STATE
/*TODO*///#define HD63701_IRQ_STATE			 M6800_IRQ_STATE
/*TODO*///
/*TODO*///#define HD63701_WAI 				 M6800_WAI
/*TODO*///#define HD63701_SLP 				 M6800_SLP
    public static final int HD63701_IRQ_LINE = M6800_IRQ_LINE;
    /*TODO*///#define HD63701_TIN_LINE			 M6800_TIN_LINE
/*TODO*///
/*TODO*///#define hd63701_ICount				 m6800_ICount
/*TODO*///
/*TODO*///
    public static final int HD63701_DDR1 = M6803_DDR1;
    public static final int HD63701_DDR2 = M6803_DDR2;

    public static final int HD63701_PORT1 = M6803_PORT1;
    public static final int HD63701_PORT2 = M6803_PORT2;
    /*TODO*///
/*TODO*///READ_HANDLER( hd63701_internal_registers_r );
/*TODO*///WRITE_HANDLER( hd63701_internal_registers_w );
/*TODO*///
/*TODO*///#endif
/*TODO*///
/*TODO*////****************************************************************************
/*TODO*/// * For now make the NSC8105 using the m6800 variables and functions
/*TODO*/// ****************************************************************************/
/*TODO*///#if (HAS_NSC8105)
/*TODO*///#define NSC8105_A					 M6800_A
/*TODO*///#define NSC8105_B					 M6800_B
/*TODO*///#define NSC8105_PC					 M6800_PC
/*TODO*///#define NSC8105_S					 M6800_S
/*TODO*///#define NSC8105_X					 M6800_X
/*TODO*///#define NSC8105_CC					 M6800_CC
/*TODO*///#define NSC8105_WAI_STATE			 M6800_WAI_STATE
/*TODO*///#define NSC8105_NMI_STATE			 M6800_NMI_STATE
/*TODO*///#define NSC8105_IRQ_STATE			 M6800_IRQ_STATE
/*TODO*///
/*TODO*///#define NSC8105_WAI 				 M6800_WAI
/*TODO*///#define NSC8105_IRQ_LINE			 M6800_IRQ_LINE
/*TODO*///#define NSC8105_TIN_LINE			 M6800_TIN_LINE
/*TODO*///
/*TODO*///#define nsc8105_ICount				 m6800_ICount
/*TODO*///
/*TODO*///#endif
/*TODO*///
/*TODO*////****************************************************************************/
/*TODO*////* Read a byte from given memory location									*/
/*TODO*////****************************************************************************/
/*TODO*////* ASG 971005 -- changed to cpu_readmem16/cpu_writemem16 */
/*TODO*///#define M6800_RDMEM(Addr) ((unsigned)cpu_readmem16(Addr))
/*TODO*///
/*TODO*////****************************************************************************/
/*TODO*////* Write a byte to given memory location                                    */
/*TODO*////****************************************************************************/
/*TODO*///#define M6800_WRMEM(Addr,Value) (cpu_writemem16(Addr,Value))
/*TODO*///
/*TODO*////****************************************************************************/
/*TODO*////* M6800_RDOP() is identical to M6800_RDMEM() except it is used for reading */
/*TODO*////* opcodes. In case of system with memory mapped I/O, this function can be  */
/*TODO*////* used to greatly speed up emulation                                       */
/*TODO*////****************************************************************************/
/*TODO*///#define M6800_RDOP(Addr) ((unsigned)cpu_readop(Addr))
/*TODO*///
/*TODO*////****************************************************************************/
/*TODO*////* M6800_RDOP_ARG() is identical to M6800_RDOP() but it's used for reading  */
/*TODO*////* opcode arguments. This difference can be used to support systems that    */
/*TODO*////* use different encoding mechanisms for opcodes and opcode arguments       */
/*TODO*////****************************************************************************/
/*TODO*///#define M6800_RDOP_ARG(Addr) ((unsigned)cpu_readop_arg(Addr))
/*TODO*///
/*TODO*///#ifndef FALSE
/*TODO*///#    define FALSE 0
/*TODO*///#endif
/*TODO*///#ifndef TRUE
/*TODO*///#    define TRUE (!FALSE)
/*TODO*///#endif
/*TODO*///
/*TODO*///#ifdef	MAME_DEBUG
/*TODO*///unsigned Dasm680x(int subtype, char *buf, unsigned pc);
/*TODO*///#endif
/*TODO*///
/*TODO*///#endif /* _M6800_H */
/*TODO*///
}
