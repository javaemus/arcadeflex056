/*****************************************************************************
 *
 *	 z8000ops.c
 *	 Portable Z8000(2) emulator
 *	 Opcode functions
 *
 *	 Copyright (c) 1998 Juergen Buchmueller, all rights reserved.
 *	 Bug fixes and MSB_FIRST compliance Ernesto Corvi.
 *
 *	 - This source code is released as freeware for non-commercial purposes.
 *	 - You are free to use and redistribute this code in modified or
 *	   unmodified form, provided you list me in the credits.
 *	 - If you modify this source code, you must add a notice to each modified
 *	   source file that it has been changed.  If you're a nice person, you
 *	   will clearly mark each change too.  :)
 *	 - If you wish to use this for commercial purposes, please contact me at
 *	   pullmoll@t-online.de
 *	 - The author of this copywritten work reserves the right to change the
 *     terms of its usage and license at any time, including retroactively
 *   - This entire notice must remain in the source code.
 *
 *****************************************************************************/

package mame056.cpu.z8000;

import mame056.cpu.z80.z80.opcode;
// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpu.z8000.z8000.Z;
import mame056.cpu.z8000.z8000cpuH.Z8000_init;

public class z8000ops {
    
    
    
/*TODO*////******************************************
/*TODO*/// helper functions
/*TODO*/// ******************************************/
/*TODO*///
/*TODO*////******************************************
/*TODO*/// check new fcw for switch to system mode
/*TODO*/// and swap stack pointer if needed
/*TODO*/// ******************************************/
/*TODO*///INLINE void CHANGE_FCW(UINT16 fcw)
/*TODO*///{
/*TODO*///	if (fcw & F_S_N) {			/* system mode now? */
/*TODO*///		if (!(FCW & F_S_N)) {	/* and not before? */
/*TODO*///			UINT16 tmp = RW(SP);
/*TODO*///			RW(SP) = NSP;
/*TODO*///			NSP = tmp;
/*TODO*///		}
/*TODO*///	} else {					/* user mode now */
/*TODO*///		if (FCW & F_S_N) {		/* and not before? */
/*TODO*///			UINT16 tmp = RW(SP);
/*TODO*///			RW(SP) = NSP;
/*TODO*///			NSP = tmp;
/*TODO*///        }
/*TODO*///    }
/*TODO*///    if (!(FCW & F_NVIE) && (fcw & F_NVIE) && (Z.irq_state[0] != CLEAR_LINE))
/*TODO*///		IRQ_REQ |= Z8000_NVI;
/*TODO*///	if (!(FCW & F_VIE) && (fcw & F_VIE) && (Z.irq_state[1] != CLEAR_LINE))
/*TODO*///		IRQ_REQ |= Z8000_VI;
/*TODO*///    FCW = fcw;  /* set new FCW */
/*TODO*///}
/*TODO*///
/*TODO*///INLINE void PUSHW(UINT8 dst, UINT16 value)
 /*TODO*///{
/*TODO*///    RW(dst) -= 2;
/*TODO*///	WRMEM_W( RW(dst), value );
/*TODO*///}
/*TODO*///
/*TODO*///INLINE UINT16 POPW(UINT8 src)
 /*TODO*///{
/*TODO*///	UINT16 result = RDMEM_W( RW(src) );
/*TODO*///    RW(src) += 2;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*///INLINE void PUSHL(UINT8 dst, UINT32 value)
 /*TODO*///{
/*TODO*///	RW(dst) -= 4;
/*TODO*///	WRMEM_L( RW(dst), value );
/*TODO*///}
/*TODO*///
/*TODO*///INLINE UINT32 POPL(UINT8 src)
 /*TODO*///{
/*TODO*///	UINT32 result = RDMEM_L( RW(src) );
/*TODO*///    RW(src) += 4;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////* check zero and sign flag for byte, word and long results */
/*TODO*///#define CHK_XXXB_ZS if (result == 0) SET_Z; else if ((INT8) result < 0) SET_S
/*TODO*///#define CHK_XXXW_ZS if (result == 0) SET_Z; else if ((INT16)result < 0) SET_S
/*TODO*///#define CHK_XXXL_ZS if (result == 0) SET_Z; else if ((INT32)result < 0) SET_S
/*TODO*///#define CHK_XXXQ_ZS if (result == 0) SET_Z; else if ((INT64)result < 0) SET_S
/*TODO*///
/*TODO*///#define CHK_XXXB_ZSP FCW |= z8000_zsp[result]
/*TODO*///
/*TODO*////* check carry for addition and subtraction */
/*TODO*///#define CHK_ADDX_C if (result < dest) SET_C
/*TODO*///#define CHK_ADCX_C if (result < dest || (result == dest && value)) SET_C
/*TODO*///
/*TODO*///#define CHK_SUBX_C if (result > dest) SET_C
/*TODO*///#define CHK_SBCX_C if (result > dest || (result == dest && value)) SET_C
/*TODO*///
/*TODO*////* check half carry for A addition and S subtraction */
/*TODO*///#define CHK_ADDB_H  if ((result & 15) < (dest & 15)) SET_H
/*TODO*///#define CHK_ADCB_H	if ((result & 15) < (dest & 15) || ((result & 15) == (dest & 15) && (value & 15))) SET_H
/*TODO*///
/*TODO*///#define CHK_SUBB_H  if ((result & 15) > (dest & 15)) SET_H
/*TODO*///#define CHK_SBCB_H	if ((result & 15) > (dest & 15) || ((result & 15) == (dest & 15) && (value & 15))) SET_H
/*TODO*///
/*TODO*////* check overflow for addition for byte, word and long */
/*TODO*///#define CHK_ADDB_V if (((value & dest & ~result) | (~value & ~dest & result)) & S08) SET_V
/*TODO*///#define CHK_ADDW_V if (((value & dest & ~result) | (~value & ~dest & result)) & S16) SET_V
/*TODO*///#define CHK_ADDL_V if (((value & dest & ~result) | (~value & ~dest & result)) & S32) SET_V
/*TODO*///
/*TODO*////* check overflow for subtraction for byte, word and long */
/*TODO*///#define CHK_SUBB_V if (((~value & dest & ~result) | (value & ~dest & result)) & S08) SET_V
/*TODO*///#define CHK_SUBW_V if (((~value & dest & ~result) | (value & ~dest & result)) & S16) SET_V
/*TODO*///#define CHK_SUBL_V if (((~value & dest & ~result) | (value & ~dest & result)) & S32) SET_V
/*TODO*///
/*TODO*///
/*TODO*////******************************************
/*TODO*/// add byte
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 ADDB(UINT8 dest, UINT8 value)
/*TODO*///{
/*TODO*///	UINT8 result = dest + value;
/*TODO*///    CLR_CZSVH;      /* first clear C, Z, S, P/V and H flags    */
/*TODO*///    CLR_DA;         /* clear DA (decimal adjust) flag for addb */
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	CHK_ADDX_C; 	/* set C if result overflowed			   */
/*TODO*///	CHK_ADDB_V; 	/* set V if result has incorrect sign	   */
/*TODO*///    CHK_ADDB_H;     /* set H if lower nibble overflowed        */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// add word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 ADDW(UINT16 dest, UINT16 value)
/*TODO*///{
/*TODO*///	UINT16 result = dest + value;
/*TODO*///    CLR_CZSV;       /* first clear C, Z, S, P/V flags          */
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	CHK_ADDX_C; 	/* set C if result overflowed			   */
/*TODO*///	CHK_ADDW_V; 	/* set V if result has incorrect sign	   */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// add long
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT32 ADDL(UINT32 dest, UINT32 value)
/*TODO*///{
/*TODO*///	UINT32 result = dest + value;
/*TODO*///    CLR_CZSV;       /* first clear C, Z, S, P/V flags          */
/*TODO*///    CHK_XXXL_ZS;    /* set Z and S flags for result long       */
/*TODO*///	CHK_ADDX_C; 	/* set C if result overflowed			   */
/*TODO*///	CHK_ADDL_V; 	/* set V if result has incorrect sign	   */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// add with carry byte
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 ADCB(UINT8 dest, UINT8 value)
/*TODO*///{
/*TODO*///	UINT8 result = dest + value + GET_C;
/*TODO*///    CLR_CZSVH;      /* first clear C, Z, S, P/V and H flags    */
/*TODO*///    CLR_DA;         /* clear DA (decimal adjust) flag for adcb */
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	CHK_ADCX_C; 	/* set C if result overflowed			   */
/*TODO*///	CHK_ADDB_V; 	/* set V if result has incorrect sign	   */
/*TODO*///	CHK_ADCB_H; 	/* set H if lower nibble overflowed 	   */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// add with carry word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 ADCW(UINT16 dest, UINT16 value)
/*TODO*///{
/*TODO*///	UINT16 result = dest + value + GET_C;
/*TODO*///    CLR_CZSV;       /* first clear C, Z, S, P/V flags          */
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	CHK_ADCX_C; 	/* set C if result overflowed			   */
/*TODO*///	CHK_ADDW_V; 	/* set V if result has incorrect sign	   */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// subtract byte
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 SUBB(UINT8 dest, UINT8 value)
/*TODO*///{
/*TODO*///	UINT8 result = dest - value;
/*TODO*///    CLR_CZSVH;      /* first clear C, Z, S, P/V and H flags    */
/*TODO*///    SET_DA;         /* set DA (decimal adjust) flag for subb   */
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	CHK_SUBX_C; 	/* set C if result underflowed			   */
/*TODO*///	CHK_SUBB_V; 	/* set V if result has incorrect sign	   */
/*TODO*///    CHK_SUBB_H;     /* set H if lower nibble underflowed       */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// subtract word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 SUBW(UINT16 dest, UINT16 value)
/*TODO*///{
/*TODO*///	UINT16 result = dest - value;
/*TODO*///    CLR_CZSV;       /* first clear C, Z, S, P/V flags          */
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	CHK_SUBX_C; 	/* set C if result underflowed			   */
/*TODO*///	CHK_SUBW_V; 	/* set V if result has incorrect sign	   */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// subtract long
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT32 SUBL(UINT32 dest, UINT32 value)
/*TODO*///{
/*TODO*///	UINT32 result = dest - value;
/*TODO*///    CLR_CZSV;       /* first clear C, Z, S, P/V flags          */
/*TODO*///    CHK_XXXL_ZS;    /* set Z and S flags for result long       */
/*TODO*///	CHK_SUBX_C; 	/* set C if result underflowed			   */
/*TODO*///	CHK_SUBL_V; 	/* set V if result has incorrect sign	   */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// subtract with carry byte
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 SBCB(UINT8 dest, UINT8 value)
/*TODO*///{
/*TODO*///	UINT8 result = dest - value - GET_C;
/*TODO*///    CLR_CZSVH;      /* first clear C, Z, S, P/V and H flags    */
/*TODO*///    SET_DA;         /* set DA (decimal adjust) flag for sbcb   */
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	CHK_SBCX_C; 	/* set C if result underflowed			   */
/*TODO*///	CHK_SUBB_V; 	/* set V if result has incorrect sign	   */
/*TODO*///	CHK_SBCB_H; 	/* set H if lower nibble underflowed	   */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// subtract with carry word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 SBCW(UINT16 dest, UINT16 value)
/*TODO*///{
/*TODO*///	UINT16 result = dest - value - GET_C;
/*TODO*///    CLR_CZSV;       /* first clear C, Z, S, P/V flags          */
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	CHK_SBCX_C; 	/* set C if result underflowed			   */
/*TODO*///	CHK_SUBW_V; 	/* set V if result has incorrect sign	   */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// logical or byte
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 ORB(UINT8 dest, UINT8 value)
/*TODO*///{
/*TODO*///	UINT8 result = dest | value;
/*TODO*///	CLR_ZSP;		/* first clear Z, S, P/V flags			   */
/*TODO*///	CHK_XXXB_ZSP;	/* set Z, S and P flags for result byte    */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// logical or word
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 ORW(UINT16 dest, UINT16 value)
/*TODO*///{
/*TODO*///	UINT16 result = dest | value;
/*TODO*///	CLR_ZS; 		/* first clear Z, and S flags			   */
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// logical and byte
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 ANDB(UINT8 dest, UINT8 value)
/*TODO*///{
/*TODO*///	UINT8 result = dest & value;
/*TODO*///    CLR_ZSP;        /* first clear Z,S and P/V flags           */
/*TODO*///	CHK_XXXB_ZSP;	/* set Z, S and P flags for result byte    */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// logical and word
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 ANDW(UINT16 dest, UINT16 value)
/*TODO*///{
/*TODO*///	UINT16 result = dest & value;
/*TODO*///    CLR_ZS;         /* first clear Z and S flags               */
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// logical exclusive or byte
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 XORB(UINT8 dest, UINT8 value)
/*TODO*///{
/*TODO*///	UINT8 result = dest ^ value;
/*TODO*///    CLR_ZSP;        /* first clear Z, S and P/V flags          */
/*TODO*///	CHK_XXXB_ZSP;	/* set Z, S and P flags for result byte    */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// logical exclusive or word
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 XORW(UINT16 dest, UINT16 value)
/*TODO*///{
/*TODO*///	UINT16 result = dest ^ value;
/*TODO*///    CLR_ZS;         /* first clear Z and S flags               */
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////******************************************
/*TODO*/// compare byte
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE void CPB(UINT8 dest, UINT8 value)
/*TODO*///{
/*TODO*///	UINT8 result = dest - value;
/*TODO*///    CLR_CZSV;       /* first clear C, Z, S and P/V flags       */
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	CHK_SUBX_C; 	/* set C if result underflowed			   */
/*TODO*///	CHK_SUBB_V;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// compare word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE void CPW(UINT16 dest, UINT16 value)
/*TODO*///{
/*TODO*///	UINT16 result = dest - value;
/*TODO*///	CLR_CZSV;
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	CHK_SUBX_C; 	/* set C if result underflowed			   */
/*TODO*///	CHK_SUBW_V;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// compare long
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE void CPL(UINT32 dest, UINT32 value)
/*TODO*///{
/*TODO*///	UINT32 result = dest - value;
/*TODO*///	CLR_CZSV;
/*TODO*///    CHK_XXXL_ZS;    /* set Z and S flags for result long       */
/*TODO*///	CHK_SUBX_C; 	/* set C if result underflowed			   */
/*TODO*///	CHK_SUBL_V;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// complement byte
/*TODO*/// flags: -ZSP--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 COMB(UINT8 dest)
/*TODO*///{
/*TODO*///	UINT8 result = ~dest;
/*TODO*///	CLR_ZSP;
/*TODO*///	CHK_XXXB_ZSP;	/* set Z, S and P flags for result byte    */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// complement word
/*TODO*/// flags: -ZS---
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 COMW(UINT16 dest)
/*TODO*///{
/*TODO*///	UINT16 result = ~dest;
/*TODO*///	CLR_ZS;
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// negate byte
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 NEGB(UINT8 dest)
/*TODO*///{
/*TODO*///	UINT8 result = (UINT8) -dest;
/*TODO*///	CLR_CZSV;
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	if (result > 0) SET_C;
/*TODO*///    if (result == S08) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// negate word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 NEGW(UINT16 dest)
/*TODO*///{
/*TODO*///	UINT16 result = (UINT16) -dest;
/*TODO*///	CLR_CZSV;
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	if (result > 0) SET_C;
/*TODO*///    if (result == S16) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// test byte
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
/*TODO*///INLINE void TESTB(UINT8 result)
/*TODO*///{
/*TODO*///	CLR_ZSP;
/*TODO*///	CHK_XXXB_ZSP;	/* set Z and S flags for result byte	   */
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// test word
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
/*TODO*///INLINE void TESTW(UINT16 dest)
/*TODO*///{
/*TODO*///	CLR_ZS;
/*TODO*///    if (dest == 0) SET_Z; else if (dest & S16) SET_S;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// test long
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
/*TODO*///INLINE void TESTL(UINT32 dest)
/*TODO*///{
/*TODO*///	CLR_ZS;
/*TODO*///	if (dest == 0) SET_Z; else if (dest & S32) SET_S;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// increment byte
/*TODO*/// flags: -ZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 INCB(UINT8 dest, UINT8 value)
/*TODO*///{
/*TODO*///    UINT8 result = dest + value;
/*TODO*///	CLR_ZSV;
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	CHK_ADDB_V; 	/* set V if result overflowed			   */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// increment word
/*TODO*/// flags: -ZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 INCW(UINT16 dest, UINT16 value)
/*TODO*///{
/*TODO*///    UINT16 result = dest + value;
/*TODO*///	CLR_ZSV;
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	CHK_ADDW_V; 	/* set V if result overflowed			   */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// decrement byte
/*TODO*/// flags: -ZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 DECB(UINT8 dest, UINT8 value)
/*TODO*///{
/*TODO*///    UINT8 result = dest - value;
/*TODO*///	CLR_ZSV;
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	CHK_SUBB_V; 	/* set V if result overflowed			   */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// decrement word
/*TODO*/// flags: -ZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 DECW(UINT16 dest, UINT16 value)
/*TODO*///{
/*TODO*///    UINT16 result = dest - value;
/*TODO*///	CLR_ZSV;
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	CHK_SUBW_V; 	/* set V if result overflowed			   */
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// multiply words
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT32 MULTW(UINT16 dest, UINT16 value)
/*TODO*///{
/*TODO*///	UINT32 result = (INT32)(INT16)dest * (INT16)value;
/*TODO*///	CLR_CZSV;
/*TODO*///    CHK_XXXL_ZS;
/*TODO*///	if (value == 0)
/*TODO*///	{
/*TODO*///		/* multiplication with zero is faster */
/*TODO*///        z8000_ICount += (70-18);
/*TODO*///	}
/*TODO*///	if( (INT32)result < -0x7fff || (INT32)result >= 0x7fff ) SET_C;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// multiply longs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT64 MULTL(UINT32 dest, UINT32 value)
/*TODO*///{
/*TODO*///	UINT64 result = (INT64)(INT32)dest * (INT32)value;
/*TODO*///    if (value == 0)
/*TODO*///	{
/*TODO*///		/* multiplication with zero is faster */
/*TODO*///		z8000_ICount += (282 - 30);
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		int n;
/*TODO*///		for( n = 0; n < 32; n++ )
/*TODO*///			if( dest & (1L << n) ) z8000_ICount -= 7;
/*TODO*///    }
/*TODO*///    CLR_CZSV;
/*TODO*///	CHK_XXXQ_ZS;
/*TODO*///	if( (INT64)result < -0x7fffffffL || (INT64)result >= 0x7fffffffL ) SET_C;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// divide long by word
/*TODO*/// flags: CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT32 DIVW(UINT32 dest, UINT16 value)
/*TODO*///{
/*TODO*///	UINT32 result = dest;
/*TODO*///	UINT16 remainder = 0;
/*TODO*///	CLR_CZSV;
/*TODO*///	if (value)
/*TODO*///	{
/*TODO*///		UINT16 qsign = ((dest >> 16) ^ value) & S16;
/*TODO*///		UINT16 rsign = (dest >> 16) & S16;
/*TODO*///		if ((INT32)dest < 0) dest = -dest;
/*TODO*///		if ((INT16)value < 0) value = -value;
/*TODO*///		result = dest / value;
/*TODO*///		remainder = dest % value;
/*TODO*///		if (qsign) result = -result;
/*TODO*///		if (rsign) remainder = -remainder;
/*TODO*///		if ((INT32)result < -0x8000 || (INT32)result > 0x7fff)
/*TODO*///		{
/*TODO*///			INT32 temp = (INT32)result >> 1;
/*TODO*///			SET_V;
/*TODO*///			if (temp >= -0x8000 && temp <= 0x7fff)
/*TODO*///			{
/*TODO*///				result = (temp < 0) ? -1 : 0;
/*TODO*///				CHK_XXXW_ZS;
/*TODO*///				SET_C;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			CHK_XXXW_ZS;
/*TODO*///		}
/*TODO*///		result = ((UINT32)remainder << 16) | (result & 0xffff);
/*TODO*///    }
/*TODO*///    else
/*TODO*///    {
/*TODO*///		SET_Z;
/*TODO*///        SET_V;
/*TODO*///    }
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// divide quad word by long
/*TODO*/// flags: CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT64 DIVL(UINT64 dest, UINT32 value)
/*TODO*///{
/*TODO*///	UINT64 result = dest;
/*TODO*///	UINT32 remainder = 0;
/*TODO*///	CLR_CZSV;
/*TODO*///	if (value)
/*TODO*///	{
/*TODO*///		UINT32 qsign = ((dest >> 32) ^ value) & S32;
/*TODO*///		UINT32 rsign = (dest >> 32) & S32;
/*TODO*///		if ((INT64)dest < 0) dest = -dest;
/*TODO*///		if ((INT32)value < 0) value = -value;
/*TODO*///		result = dest / value;
/*TODO*///		remainder = dest % value;
/*TODO*///		if (qsign) result = -result;
/*TODO*///		if (rsign) remainder = -remainder;
/*TODO*///		if ((INT64)result < -0x80000000 || (INT64)result > 0x7fffffff)
/*TODO*///		{
/*TODO*///			INT64 temp = (INT64)result >> 1;
/*TODO*///			SET_V;
/*TODO*///			if (temp >= -0x80000000 && temp <= 0x7fffffff)
/*TODO*///			{
/*TODO*///				result = (temp < 0) ? -1 : 0;
/*TODO*///				CHK_XXXL_ZS;
/*TODO*///				SET_C;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			CHK_XXXL_ZS;
/*TODO*///		}
/*TODO*///		result = ((UINT64)remainder << 32) | (result & 0xffffffff);
/*TODO*///    }
/*TODO*///    else
/*TODO*///    {
/*TODO*///		SET_Z;
/*TODO*///        SET_V;
/*TODO*///    }
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rotate left byte
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 RLB(UINT8 dest, UINT8 twice)
/*TODO*///{
/*TODO*///	UINT8 result = (dest << 1) | (dest >> 7);
/*TODO*///	CLR_CZSV;
/*TODO*///	if (twice) result = (result << 1) | (result >> 7);
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	if (result & 0x01) SET_C;
/*TODO*///    if ((result ^ dest) & S08) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rotate left word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 RLW(UINT16 dest, UINT8 twice)
/*TODO*///{
/*TODO*///	UINT16 result = (dest << 1) | (dest >> 15);
/*TODO*///	CLR_CZSV;
/*TODO*///	if (twice) result = (result << 1) | (result >> 15);
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	if (result & 0x0001) SET_C;
/*TODO*///    if ((result ^ dest) & S16) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rotate left through carry byte
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 RLCB(UINT8 dest, UINT8 twice)
/*TODO*///{
/*TODO*///    UINT8 c = dest & S08;
/*TODO*///	UINT8 result = (dest << 1) | GET_C;
/*TODO*///	CLR_CZSV;
/*TODO*///	if (twice) {
/*TODO*///		UINT8 c1 = c >> 7;
/*TODO*///        c = result & S08;
/*TODO*///		result = (result << 1) | c1;
/*TODO*///	}
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	if (c) SET_C;
/*TODO*///    if ((result ^ dest) & S08) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rotate left through carry word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 RLCW(UINT16 dest, UINT8 twice)
/*TODO*///{
/*TODO*///    UINT16 c = dest & S16;
/*TODO*///	UINT16 result = (dest << 1) | GET_C;
/*TODO*///	CLR_CZSV;
/*TODO*///	if (twice) {
/*TODO*///		UINT16 c1 = c >> 15;
/*TODO*///        c = result & S16;
/*TODO*///		result = (result << 1) | c1;
/*TODO*///    }
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	if (c) SET_C;
/*TODO*///    if ((result ^ dest) & S16) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rotate right byte
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 RRB(UINT8 dest, UINT8 twice)
/*TODO*///{
/*TODO*///	UINT8 result = (dest >> 1) | (dest << 7);
/*TODO*///	CLR_CZSV;
/*TODO*///	if (twice) result = (result >> 1) | (result << 7);
/*TODO*///    if (result == 0) SET_Z; else if (result & S08) SET_SC;
/*TODO*///    if ((result ^ dest) & S08) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rotate right word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 RRW(UINT16 dest, UINT8 twice)
/*TODO*///{
/*TODO*///	UINT16 result = (dest >> 1) | (dest << 15);
/*TODO*///	CLR_CZSV;
/*TODO*///	if (twice) result = (result >> 1) | (result << 15);
/*TODO*///    if (result == 0) SET_Z; else if (result & S16) SET_SC;
/*TODO*///    if ((result ^ dest) & S16) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rotate right through carry byte
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 RRCB(UINT8 dest, UINT8 twice)
/*TODO*///{
/*TODO*///	UINT8 c = dest & 1;
/*TODO*///	UINT8 result = (dest >> 1) | (GET_C << 7);
/*TODO*///	CLR_CZSV;
/*TODO*///	if (twice) {
/*TODO*///		UINT8 c1 = c << 7;
/*TODO*///		c = result & 1;
/*TODO*///		result = (result >> 1) | c1;
/*TODO*///	}
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	if (c) SET_C;
/*TODO*///    if ((result ^ dest) & S08) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rotate right through carry word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 RRCW(UINT16 dest, UINT8 twice)
/*TODO*///{
/*TODO*///	UINT16 c = dest & 1;
/*TODO*///	UINT16 result = (dest >> 1) | (GET_C << 15);
/*TODO*///	CLR_CZSV;
/*TODO*///	if (twice) {
/*TODO*///		UINT16 c1 = c << 15;
/*TODO*///		c = result & 1;
/*TODO*///		result = (result >> 1) | c1;
/*TODO*///    }
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	if (c) SET_C;
/*TODO*///    if ((result ^ dest) & S16) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift dynamic arithmetic byte
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 SDAB(UINT8 dest, INT8 count)
/*TODO*///{
/*TODO*///	INT8 result = (INT8) dest;
/*TODO*///	UINT8 c = 0;
/*TODO*///	CLR_CZSV;
/*TODO*///	while (count > 0) {
/*TODO*///        c = result & S08;
/*TODO*///		result <<= 1;
/*TODO*///		count--;
/*TODO*///	}
/*TODO*///	while (count < 0) {
/*TODO*///		c = result & 0x01;
/*TODO*///		result >>= 1;
/*TODO*///		count++;
/*TODO*///	}
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	if (c) SET_C;
/*TODO*///    if ((result ^ dest) & S08) SET_V;
/*TODO*///	return (UINT8)result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift dynamic arithmetic word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 SDAW(UINT16 dest, INT8 count)
/*TODO*///{
/*TODO*///	INT16 result = (INT16) dest;
/*TODO*///	UINT16 c = 0;
/*TODO*///	CLR_CZSV;
/*TODO*///	while (count > 0) {
/*TODO*///        c = result & S16;
/*TODO*///		result <<= 1;
/*TODO*///		count--;
/*TODO*///	}
/*TODO*///	while (count < 0) {
/*TODO*///		c = result & 0x0001;
/*TODO*///		result >>= 1;
/*TODO*///		count++;
/*TODO*///	}
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	if (c) SET_C;
/*TODO*///    if ((result ^ dest) & S16) SET_V;
/*TODO*///	return (UINT16)result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift dynamic arithmetic long
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT32 SDAL(UINT32 dest, INT8 count)
/*TODO*///{
/*TODO*///	INT32 result = (INT32) dest;
/*TODO*///	UINT32 c = 0;
/*TODO*///	CLR_CZSV;
/*TODO*///	while (count > 0) {
/*TODO*///        c = result & S32;
/*TODO*///		result <<= 1;
/*TODO*///		count--;
/*TODO*///	}
/*TODO*///	while (count < 0) {
/*TODO*///		c = result & 0x00000001;
/*TODO*///		result >>= 1;
/*TODO*///		count++;
/*TODO*///	}
/*TODO*///    CHK_XXXL_ZS;    /* set Z and S flags for result long       */
/*TODO*///	if (c) SET_C;
/*TODO*///    if ((result ^ dest) & S32) SET_V;
/*TODO*///	return (UINT32) result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift dynamic logic byte
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 SDLB(UINT8 dest, INT8 count)
/*TODO*///{
/*TODO*///	UINT8 result = dest;
/*TODO*///	UINT8 c = 0;
/*TODO*///	CLR_CZSV;
/*TODO*///	while (count > 0) {
/*TODO*///        c = result & S08;
/*TODO*///		result <<= 1;
/*TODO*///		count--;
/*TODO*///	}
/*TODO*///	while (count < 0) {
/*TODO*///		c = result & 0x01;
/*TODO*///		result >>= 1;
/*TODO*///		count++;
/*TODO*///	}
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	if (c) SET_C;
/*TODO*///    if ((result ^ dest) & S08) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift dynamic logic word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 SDLW(UINT16 dest, INT8 count)
/*TODO*///{
/*TODO*///	UINT16 result = dest;
/*TODO*///	UINT16 c = 0;
/*TODO*///    CLR_CZSV;
/*TODO*///	while (count > 0) {
/*TODO*///        c = result & S16;
/*TODO*///		result <<= 1;
/*TODO*///		count--;
/*TODO*///	}
/*TODO*///	while (count < 0) {
/*TODO*///		c = result & 0x0001;
/*TODO*///		result >>= 1;
/*TODO*///		count++;
/*TODO*///	}
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	if (c) SET_C;
/*TODO*///    if ((result ^ dest) & S16) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift dynamic logic long
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT32 SDLL(UINT32 dest, INT8 count)
/*TODO*///{
/*TODO*///	UINT32 result = dest;
/*TODO*///	UINT32 c = 0;
/*TODO*///    CLR_CZSV;
/*TODO*///	while (count > 0) {
/*TODO*///        c = result & S32;
/*TODO*///		result <<= 1;
/*TODO*///		count--;
/*TODO*///	}
/*TODO*///	while (count < 0) {
/*TODO*///		c = result & 0x00000001;
/*TODO*///		result >>= 1;
/*TODO*///		count++;
/*TODO*///	}
/*TODO*///    CHK_XXXL_ZS;    /* set Z and S flags for result long       */
/*TODO*///	if (c) SET_C;
/*TODO*///    if ((result ^ dest) & S32) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift left arithmetic byte
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 SLAB(UINT8 dest, UINT8 count)
/*TODO*///{
/*TODO*///    UINT8 c = (count) ? (dest << (count - 1)) & S08 : 0;
/*TODO*///	UINT8 result = (UINT8)((INT8)dest << count);
/*TODO*///	CLR_CZSV;
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	if (c) SET_C;
/*TODO*///    if ((result ^ dest) & S08) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift left arithmetic word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 SLAW(UINT16 dest, UINT8 count)
/*TODO*///{
/*TODO*///    UINT16 c = (count) ? (dest << (count - 1)) & S16 : 0;
/*TODO*///	UINT16 result = (UINT16)((INT16)dest << count);
/*TODO*///	CLR_CZSV;
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	if (c) SET_C;
/*TODO*///    if ((result ^ dest) & S16) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift left arithmetic long
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT32 SLAL(UINT32 dest, UINT8 count)
/*TODO*///{
/*TODO*///    UINT32 c = (count) ? (dest << (count - 1)) & S32 : 0;
/*TODO*///	UINT32 result = (UINT32)((INT32)dest << count);
/*TODO*///	CLR_CZSV;
/*TODO*///    CHK_XXXL_ZS;    /* set Z and S flags for result long       */
/*TODO*///	if (c) SET_C;
/*TODO*///    if ((result ^ dest) & S32) SET_V;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift left logic byte
/*TODO*/// flags:  CZS---
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 SLLB(UINT8 dest, UINT8 count)
/*TODO*///{
/*TODO*///    UINT8 c = (count) ? (dest << (count - 1)) & S08 : 0;
/*TODO*///	UINT8 result = dest << count;
/*TODO*///	CLR_CZS;
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	if (c) SET_C;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift left logic word
/*TODO*/// flags:  CZS---
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 SLLW(UINT16 dest, UINT8 count)
/*TODO*///{
/*TODO*///    UINT16 c = (count) ? (dest << (count - 1)) & S16 : 0;
/*TODO*///	UINT16 result = dest << count;
/*TODO*///	CLR_CZS;
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	if (c) SET_C;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift left logic long
/*TODO*/// flags:  CZS---
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT32 SLLL(UINT32 dest, UINT8 count)
/*TODO*///{
/*TODO*///    UINT32 c = (count) ? (dest << (count - 1)) & S32 : 0;
/*TODO*///	UINT32 result = dest << count;
/*TODO*///	CLR_CZS;
/*TODO*///    CHK_XXXL_ZS;    /* set Z and S flags for result long       */
/*TODO*///	if (c) SET_C;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift right arithmetic byte
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 SRAB(UINT8 dest, UINT8 count)
/*TODO*///{
/*TODO*///	UINT8 c = (count) ? ((INT8)dest >> (count - 1)) & 1 : 0;
/*TODO*///	UINT8 result = (UINT8)((INT8)dest >> count);
/*TODO*///	CLR_CZSV;
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	if (c) SET_C;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift right arithmetic word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 SRAW(UINT16 dest, UINT8 count)
/*TODO*///{
/*TODO*///	UINT8 c = (count) ? ((INT16)dest >> (count - 1)) & 1 : 0;
/*TODO*///	UINT16 result = (UINT16)((INT16)dest >> count);
/*TODO*///	CLR_CZSV;
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	if (c) SET_C;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift right arithmetic long
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT32 SRAL(UINT32 dest, UINT8 count)
/*TODO*///{
/*TODO*///	UINT8 c = (count) ? ((INT32)dest >> (count - 1)) & 1 : 0;
/*TODO*///	UINT32 result = (UINT32)((INT32)dest >> count);
/*TODO*///	CLR_CZSV;
/*TODO*///    CHK_XXXL_ZS;    /* set Z and S flags for result long       */
/*TODO*///	if (c) SET_C;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift right logic byte
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT8 SRLB(UINT8 dest, UINT8 count)
/*TODO*///{
/*TODO*///	UINT8 c = (count) ? (dest >> (count - 1)) & 1 : 0;
/*TODO*///	UINT8 result = dest >> count;
/*TODO*///	CLR_CZS;
/*TODO*///    CHK_XXXB_ZS;    /* set Z and S flags for result byte       */
/*TODO*///	if (c) SET_C;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift right logic word
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT16 SRLW(UINT16 dest, UINT8 count)
/*TODO*///{
/*TODO*///	UINT8 c = (count) ? (dest >> (count - 1)) & 1 : 0;
/*TODO*///	UINT16 result = dest >> count;
/*TODO*///	CLR_CZS;
/*TODO*///    CHK_XXXW_ZS;    /* set Z and S flags for result word       */
/*TODO*///	if (c) SET_C;
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*////******************************************
/*TODO*/// shift right logic long
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
/*TODO*///INLINE UINT32 SRLL(UINT32 dest, UINT8 count)
/*TODO*///{
/*TODO*///	UINT8 c = (count) ? (dest >> (count - 1)) & 1 : 0;
/*TODO*///	UINT32 result = dest >> count;
/*TODO*///	CLR_CZS;
/*TODO*///    CHK_XXXL_ZS;    /* set Z and S flags for result long       */
/*TODO*///	if (c) SET_C;
/*TODO*///	return result;
/*TODO*///}

    /******************************************
     invalid
     flags:  ------
     ******************************************/
    public static opcode  zinvalid = new opcode() {
        public void handler() {
            logerror("Z8000 invalid opcode %04x: %04x\n", Z.pc, Z.op[0]);
        }
    };
    
    /******************************************
     addb	 rbd,imm8
     flags:  CZSVDH
     ******************************************/
    public static opcode Z00_0000_dddd_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM8(OP1);
/*TODO*///	RB(dst) = ADDB( RB(dst), imm8);
        }
    };

    /******************************************
     addb	 rbd,@rs
     flags:  CZSVDH
     ******************************************/
    public static opcode Z00_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RB(dst) = ADDB( RB(dst), RDMEM_B(RW(src)) );
        }
    };

/*TODO*////******************************************
/*TODO*/// add	 rd,imm16
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z01_0000_dddd_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	RW(dst) = ADDW( RW(dst), imm16 );
        }
    };
/*TODO*////******************************************
/*TODO*/// add	 rd,@rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z01_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = ADDW( RW(dst), RDMEM_W(RW(src)) );
        }
    };

/*TODO*////******************************************
/*TODO*/// subb	 rbd,imm8
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode Z02_0000_dddd_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM8(OP1);
/*TODO*///	RB(dst) = SUBB( RB(dst), imm8 );
        }
    };

/*TODO*////******************************************
/*TODO*/// subb	 rbd,@rs
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode Z02_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RB(dst) = SUBB( RB(dst), RDMEM_B(RW(src)) ); /* EHC */
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sub	 rd,imm16
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z03_0000_dddd_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	RW(dst) = SUBW( RW(dst), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sub	 rd,@rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z03_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = SUBW( RW(dst), RDMEM_W(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// orb	 rbd,imm8
/*TODO*/// flags:  CZSP--
/*TODO*/// ******************************************/
    public static opcode Z04_0000_dddd_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM8(OP1);
/*TODO*///	RB(dst) = ORB( RB(dst), imm8 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// orb	 rbd,@rs
/*TODO*/// flags:  CZSP--
/*TODO*/// ******************************************/
    public static opcode Z04_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RB(dst) = ORB( RB(dst), RDMEM_B(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// or 	 rd,imm16
/*TODO*/// flags:  CZS---
/*TODO*/// ******************************************/
    public static opcode Z05_0000_dddd_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	RW(dst) = ORW( RW(dst), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// or 	 rd,@rs
/*TODO*/// flags:  CZS---
/*TODO*/// ******************************************/
    public static opcode Z05_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = ORW( RW(dst), RDMEM_W(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// andb	 rbd,imm8
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z06_0000_dddd_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM8(OP1);
/*TODO*///	RB(dst) = ANDB( RB(dst), imm8 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// andb	 rbd,@rs
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z06_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RB(dst) = ANDB( RB(dst), RDMEM_B(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// and	 rd,imm16
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z07_0000_dddd_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	RW(dst) = ANDW( RW(dst), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// and	 rd,@rs
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z07_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = ANDW( RW(dst), RDMEM_W(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// xorb	 rbd,imm8
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z08_0000_dddd_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM8(OP1);
/*TODO*///	RB(dst) = XORB(RB(dst), imm8);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// xorb	 rbd,@rs
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z08_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RB(dst) = XORB( RB(dst), RDMEM_B(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// xor	 rd,imm16
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z09_0000_dddd_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	RW(dst) = XORW( RW(dst), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// xor	 rd,@rs
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z09_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = XORW( RW(dst), RDMEM_W(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpb	 rbd,imm8
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z0A_0000_dddd_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM8(OP1);
/*TODO*///	CPB(RB(dst), imm8);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpb	 rbd,@rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z0A_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	CPB( RB(dst), RDMEM_B(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cp 	 rd,imm16
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z0B_0000_dddd_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	CPW( RW(dst), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cp 	 rd,@rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z0B_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	CPW( RW(dst), RDMEM_W(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// comb	 @rd
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z0C_ddN0_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	WRMEM_B( RW(dst), COMB(RDMEM_B(RW(dst))) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpb	 @rd,imm8
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z0C_ddN0_0001_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM8(OP1);
/*TODO*///	CPB( RB(dst), imm8 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// negb	 @rd
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z0C_ddN0_0010 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_B( RW(dst), NEGB(RDMEM_B(RW(dst))) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// testb	 @rd
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z0C_ddN0_0100 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	TESTB(RDMEM_B(RW(dst)));
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 @rd,imm8
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z0C_ddN0_0101_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM8(OP1);
/*TODO*///	WRMEM_B( RW(dst), imm8 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// tsetb	 @rd
/*TODO*/// flags:  --S---
/*TODO*/// ******************************************/
    public static opcode Z0C_ddN0_0110 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///    if (RDMEM_B(RW(dst)) & S08) SET_S; else CLR_S;
/*TODO*///    WRMEM_B(RW(dst), 0xff);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// clrb	 @rd
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z0C_ddN0_1000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_B( RW(dst), 0 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// com	 @rd
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z0D_ddN0_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_W( RW(dst), COMW(RDMEM_W(RW(dst))) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cp 	 @rd,imm16
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z0D_ddN0_0001_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	CPW( RDMEM_W(RW(dst)), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// neg	 @rd
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z0D_ddN0_0010 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_W( RW(dst), NEGW(RDMEM_W(RW(dst))) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// test	 @rd
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z0D_ddN0_0100 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	TESTW( RDMEM_W(RW(dst)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 @rd,imm16
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z0D_ddN0_0101_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	WRMEM_W( RW(dst), imm16);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// tset	 @rd
/*TODO*/// flags:  --S---
/*TODO*/// ******************************************/
    public static opcode Z0D_ddN0_0110 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///    if (RDMEM_W(RW(dst)) & S16) SET_S; else CLR_S;
/*TODO*///    WRMEM_W(RW(dst), 0xffff);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// clr	 @rd
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z0D_ddN0_1000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_W( RDMEM_W(RW(dst)), 0 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// push	 @rd,imm16
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z0D_ddN0_1001_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	PUSHW( dst, imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ext0e	 imm8
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z0E_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM8(0);
/*TODO*///	LOG(("Z8K#%d %04x: ext0e  $%02x\n", cpu_getactivecpu(), PC, imm8));
/*TODO*///    if (FCW & F_EPU) {
/*TODO*///		/* Z8001 EPU code goes here */
/*TODO*///		 = new opcode() {imm8;
/*TODO*///	}
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ext0f	 imm8
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z0F_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM8(0);
/*TODO*///	LOG(("Z8K#%d %04x: ext0f  $%02x\n", cpu_getactivecpu(), PC, imm8));
/*TODO*///    if (FCW & F_EPU) {
/*TODO*///		/* Z8001 EPU code goes here */
/*TODO*///		 = new opcode() {imm8;
/*TODO*///    }
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpl	 rrd,imm32
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z10_0000_dddd_imm32 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM32;
/*TODO*///	CPL( RL(dst), imm32 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpl	 rrd,@rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z10_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	CPL( RL(dst), RDMEM_L(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// pushl	 @rd,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z11_ddN0_ssN0 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	PUSHL( dst, RDMEM_L(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// subl	 rrd,imm32
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z12_0000_dddd_imm32 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM32;
/*TODO*///	RL(dst) = SUBL( RL(dst), imm32 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// subl	 rrd,@rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z12_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RL(dst) = SUBL( RL(dst), RDMEM_L(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// push	 @rd,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z13_ddN0_ssN0 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	PUSHW( dst, RDMEM_W(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldl	 rrd,imm32
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z14_0000_dddd_imm32 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM32;
/*TODO*///	RL(dst) = imm32;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldl	 rrd,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z14_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RL(dst) = RDMEM_L( RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// popl	 @rd,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z15_ssN0_ddN0 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RL(dst) = POPL( src );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// addl	 rrd,imm32
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z16_0000_dddd_imm32 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM32;
/*TODO*///	RL(dst) = ADDL( RL(dst), imm32 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// addl	 rrd,@rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z16_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RL(dst) = ADDL( RL(dst), RDMEM_L(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// pop	 @rd,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z17_ssN0_ddN0 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = POPW( src );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// multl	 rqd,@rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z18_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RQ(dst) = MULTL( RQ(dst), RL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// mult	 rrd,imm16
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z19_0000_dddd_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	RL(dst) = MULTW( RL(dst), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// mult	 rrd,@rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z19_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RL(dst) = MULTW( RL(dst), RDMEM_W(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// divl	 rqd,imm32
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z1A_0000_dddd_imm32 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM32;
/*TODO*///	RQ(dst) = DIVL( RQ(dst), imm32 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// divl	 rqd,@rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z1A_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RQ(dst) = DIVL( RQ(dst), RDMEM_L(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// div	 rrd,imm16
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z1B_0000_dddd_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	RL(dst) = DIVW( RL(dst), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// div	 rrd,@rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z1B_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RL(dst) = DIVW( RL(dst), RDMEM_W(RW(src)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// testl	 @rd
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z1C_ddN0_1000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	TESTL( RDMEM_L(RW(dst)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldm     @rd,rs,n
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z1C_ddN0_1001_0000_ssss_0000_nmin1 = new opcode() {
        public void handler() {
/*TODO*///    GET_DST(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB3);
/*TODO*///    GET_SRC(OP1,NIB1);
/*TODO*///	UINT16 idx = RW(dst);
/*TODO*///    while (cnt-- >= 0) {
/*TODO*///        WRMEM_W( idx, RW(src) );
/*TODO*///		idx = (idx + 2) & 0xffff;
/*TODO*///		src = (src+1) & 15;
/*TODO*///    }
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldm	 rd,@rs,n
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z1C_ssN0_0001_0000_dddd_0000_nmin1 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CNT(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB1);
/*TODO*///	UINT16 idx = RW(src);
/*TODO*///	while (cnt-- >= 0) {
/*TODO*///		RW(dst) = RDMEM_W( idx );
/*TODO*///		idx = (idx + 2) & 0xffff;
/*TODO*///		dst = (dst+1) & 15;
/*TODO*///    }
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldl	 @rd,rrs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z1D_ddN0_ssss = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_L( RW(dst), RL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// jp      cc,rd
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z1E_ddN0_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_CCC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) PC = RW(dst); break;
/*TODO*///		case  1: if (CC1) PC = RW(dst); break;
/*TODO*///		case  2: if (CC2) PC = RW(dst); break;
/*TODO*///		case  3: if (CC3) PC = RW(dst); break;
/*TODO*///		case  4: if (CC4) PC = RW(dst); break;
/*TODO*///		case  5: if (CC5) PC = RW(dst); break;
/*TODO*///		case  6: if (CC6) PC = RW(dst); break;
/*TODO*///		case  7: if (CC7) PC = RW(dst); break;
/*TODO*///		case  8: if (CC8) PC = RW(dst); break;
/*TODO*///		case  9: if (CC9) PC = RW(dst); break;
/*TODO*///		case 10: if (CCA) PC = RW(dst); break;
/*TODO*///		case 11: if (CCB) PC = RW(dst); break;
/*TODO*///		case 12: if (CCC) PC = RW(dst); break;
/*TODO*///		case 13: if (CCD) PC = RW(dst); break;
/*TODO*///		case 14: if (CCE) PC = RW(dst); break;
/*TODO*///		case 15: if (CCF) PC = RW(dst); break;
/*TODO*///	}
/*TODO*///	change_pc16bew(PC);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// call	 @rd
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z1F_ddN0_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	PUSHW( SP, PC );
/*TODO*///    PC = RW(dst);
/*TODO*///	change_pc16bew(PC);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 rbd,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z20_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RB(dst) = RDMEM_B( RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 rd,imm16
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z21_0000_dddd_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	RW(dst) = imm16;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 rd,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z21_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = RDMEM_W( RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// resb	 rbd,rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z22_0000_ssss_0000_dddd_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP1,NIB1);
/*TODO*///	RB(dst) = RB(dst) & ~(1 << (RW(src) & 7));
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// resb	 @rd,imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z22_ddN0_imm4 = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_B(RW(dst), RDMEM_B(RW(dst)) & ~bit);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// result 	rd,rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z23_0000_ssss_0000_dddd_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP1,NIB1);
/*TODO*///	RW(dst) = RW(dst) & ~(1 << (RW(src) & 15));
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// res	 @rd,imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z23_ddN0_imm4 = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_W(RW(dst), RDMEM_W(RW(dst)) & ~bit);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// setb	 rbd,rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z24_0000_ssss_0000_dddd_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP1,NIB1);
/*TODO*///	RB(dst) = RB(dst) | (1 << (RW(src) & 7));
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// setb	 @rd,imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z24_ddN0_imm4 = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_B(RW(dst), RDMEM_B(RW(dst)) | bit);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// set	 rd,rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z25_0000_ssss_0000_dddd_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP1,NIB1);
/*TODO*///	RW(dst) = RW(dst) | (1 << (RW(src) & 15));
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// set	 @rd,imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z25_ddN0_imm4 = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_W(RW(dst), RDMEM_W(RW(dst)) | bit);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// bitb	 rbd,rs
/*TODO*/// flags:  -Z----
/*TODO*/// ******************************************/
    public static opcode Z26_0000_ssss_0000_dddd_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP1,NIB1);
/*TODO*///	if (RB(dst) & (1 << (RW(src) & 7))) CLR_Z; else SET_Z;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// bitb	 @rd,imm4
/*TODO*/// flags:  -Z----
/*TODO*/// ******************************************/
    public static opcode Z26_ddN0_imm4 = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	if (RDMEM_B(RW(dst)) & bit) CLR_Z; else SET_Z;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// bit	 rd,rs
/*TODO*/// flags:  -Z----
/*TODO*/// ******************************************/
    public static opcode Z27_0000_ssss_0000_dddd_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP1,NIB1);
/*TODO*///	if (RW(dst) & (1 << (RW(src) & 15))) CLR_Z; else SET_Z;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// bit	 @rd,imm4
/*TODO*/// flags:  -Z----
/*TODO*/// ******************************************/
    public static opcode Z27_ddN0_imm4 = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	if (RDMEM_W(RW(dst)) & bit) CLR_Z; else SET_Z;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// incb	 @rd,imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode Z28_ddN0_imm4m1 = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_B( RW(dst), INCB( RDMEM_B(RW(dst)), i4p1) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// inc     @rd,imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode Z29_ddN0_imm4m1 = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_W( RW(dst), INCW( RDMEM_W(RW(dst)), i4p1 ) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// decb	 @rd,imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode Z2A_ddN0_imm4m1 = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_B( RW(dst), DECB( RDMEM_B(RW(dst)), i4p1 ) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// dec     @rd,imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode Z2B_ddN0_imm4m1 = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_W( RW(dst), DECW( RDMEM_W(RW(dst)), i4p1 ) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// exb	 rbd,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z2C_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	UINT8 tmp = RDMEM_B( RW(src) );
/*TODO*///	WRMEM_B( RW(src), RB(dst) );
/*TODO*///	RB(dst) = tmp;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ex 	 rd,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z2D_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	UINT16 tmp = RDMEM_W( RW(src) );
/*TODO*///	WRMEM_W( RW(src), RW(dst) );
/*TODO*///	RW(dst) = tmp;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 @rd,rbs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z2E_ddN0_ssss = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_B( RW(dst), RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 @rd,rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z2F_ddN0_ssss = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	WRMEM_W( RW(dst), RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldrb	 rbd,dsp16
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z30_0000_dddd_dsp16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_DSP16;
/*TODO*///	RB(dst) = RDMEM_B(dsp16);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 rbd,rs(imm16)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z30_ssN0_dddd_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	imm16 += RW(src);
/*TODO*///	RB(dst) = RDMEM_B( imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldr	 rd,dsp16
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z31_0000_dddd_dsp16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_DSP16;
/*TODO*///	RW(dst) = RDMEM_W(dsp16);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 rd,rs(imm16)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z31_ssN0_dddd_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	imm16 += RW(src);
/*TODO*///	RW(dst) = RDMEM_W( imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldrb	 dsp16,rbs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z32_0000_ssss_dsp16 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DSP16;
/*TODO*///	WRMEM_B( dsp16, RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 rd(imm16),rbs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z32_ddN0_ssss_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	imm16 += RW(dst);
/*TODO*///	WRMEM_B( imm16, RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldr	 dsp16,rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z33_0000_ssss_dsp16 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DSP16;
/*TODO*///	WRMEM_W( dsp16, RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 rd(imm16),rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z33_ddN0_ssss_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	imm16 += RW(dst);
/*TODO*///	WRMEM_W( imm16, RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldar	 prd,dsp16
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z34_0000_dddd_dsp16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_DSP16;
/*TODO*///	RW(dst) = dsp16;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// lda	 prd,rs(imm16)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z34_ssN0_dddd_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	imm16 += RW(src);
/*TODO*///	RW(dst) = imm16;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldrl	 rrd,dsp16
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z35_0000_dddd_dsp16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_DSP16;
/*TODO*///	RL(dst) = RDMEM_L( dsp16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldl	 rrd,rs(imm16)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z35_ssN0_dddd_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	imm16 += RW(src);
/*TODO*///	RL(dst) = RDMEM_L( imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// bpt
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z36_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	/* execute break point trap IRQ_REQ */
/*TODO*///	IRQ_REQ = Z8000_TRAP;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rsvd36
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z36_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM8(0);
/*TODO*///	LOG(("Z8K#%d %04x: rsvd36 $%02x\n", cpu_getactivecpu(), PC, imm8));
/*TODO*///    if (FCW & F_EPU) {
/*TODO*///		/* Z8001 EPU code goes here */
/*TODO*///		 = new opcode() {imm8;
/*TODO*///    }
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldrl	 dsp16,rrs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z37_0000_ssss_dsp16 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DSP16;
/*TODO*///	WRMEM_L( dsp16, RL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldl	 rd(imm16),rrs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z37_ddN0_ssss_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	imm16 += RW(dst);
/*TODO*///	WRMEM_L( imm16, RL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rsvd38
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z38_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM8(0);
/*TODO*///	LOG(("Z8K#%d %04x: rsvd38 $%02x\n", cpu_getactivecpu(), PC, imm8));
/*TODO*///    if (FCW & F_EPU) {
/*TODO*///		/* Z8001 EPU code goes here */
/*TODO*///		 = new opcode() {imm8;
/*TODO*///    }
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldps	 @rs
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode Z39_ssN0_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	UINT16 fcw;
/*TODO*///	fcw = RDMEM_W( RW(src) );
/*TODO*///	PC	= RDMEM_W( (UINT16)(RW(src) + 2) );
/*TODO*///	CHANGE_FCW(fcw); /* check for user/system mode change */
/*TODO*///    change_pc16bew(PC);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// inib(r) @rd,@rs,ra
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode Z3A_ssss_0000_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///    GET_DST(OP1,NIB2);
/*TODO*///    GET_CCC(OP1,NIB3);
/*TODO*///    WRMEM_B( RW(dst), RDPORT_B( 0, RW(src) ) );
/*TODO*///    RW(dst)++;
/*TODO*///    RW(src)++;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sinib	 @rd,@rs,ra
/*TODO*/// sinibr  @rd,@rs,ra
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3A_ssss_0001_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///    GET_DST(OP1,NIB2);
/*TODO*///    GET_CCC(OP1,NIB3);
/*TODO*///    WRMEM_B( RW(dst), RDPORT_B( 1, RW(src) ) );
/*TODO*///    RW(dst)++;
/*TODO*///    RW(src)++;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// outib	 @rd,@rs,ra
/*TODO*/// outibr  @rd,@rs,ra
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode Z3A_ssss_0010_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///    GET_DST(OP1,NIB2);
/*TODO*///    GET_CCC(OP1,NIB3);
/*TODO*///    WRPORT_B( 0, RW(dst), RDMEM_B( RW(src) ) );
/*TODO*///    RW(dst)++;
/*TODO*///    RW(src)++;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// soutib  @rd,@rs,ra
/*TODO*/// soutibr @rd,@rs,ra
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3A_ssss_0011_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///    GET_DST(OP1,NIB2);
/*TODO*///    GET_CCC(OP1,NIB3);
/*TODO*///    WRPORT_B( 1, RW(dst), RDMEM_B( RW(src) ) );
/*TODO*///    RW(dst)++;
/*TODO*///    RW(src)++;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// inb     rbd,imm16
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3A_dddd_0100_imm16 = new opcode() {
        public void handler() {
/*TODO*///    GET_DST(OP0,NIB2);
/*TODO*///    GET_IMM16(OP1);
/*TODO*///    RB(dst) = RDPORT_B( 0, imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sinb    rbd,imm16
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3A_dddd_0101_imm16 = new opcode() {
        public void handler() {
/*TODO*///    GET_DST(OP0,NIB2);
/*TODO*///    GET_IMM16(OP1);
/*TODO*///    RB(dst) = RDPORT_B( 1, imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// outb    imm16,rbs
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode Z3A_ssss_0110_imm16 = new opcode() {
        public void handler() {
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    GET_IMM16(OP1);
/*TODO*///    WRPORT_B( 0, imm16, RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// soutb   imm16,rbs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3A_ssss_0111_imm16 = new opcode() {
        public void handler() {
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    GET_IMM16(OP1);
/*TODO*///    WRPORT_B( 1, imm16, RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// indb	 @rd,@rs,rba
/*TODO*/// indbr	 @rd,@rs,rba
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode Z3A_ssss_1000_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	WRMEM_B( RW(dst), RDPORT_B( 0, RW(src) ) );
/*TODO*///	RW(dst)--;
/*TODO*///	RW(src)--;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sindb	 @rd,@rs,rba
/*TODO*/// sindbr  @rd,@rs,rba
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3A_ssss_1001_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	WRMEM_B( RW(dst), RDPORT_B( 1, RW(src) ) );
/*TODO*///	RW(dst)--;
/*TODO*///	RW(src)--;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// outdb	 @rd,@rs,rba
/*TODO*/// outdbr  @rd,@rs,rba
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode Z3A_ssss_1010_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	WRPORT_B( 0, RW(dst), RDMEM_B( RW(src) ) );
/*TODO*///	RW(dst)--;
/*TODO*///	RW(src)--;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// soutdb  @rd,@rs,rba
/*TODO*/// soutdbr @rd,@rs,rba
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3A_ssss_1011_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	WRPORT_B( 1, RW(dst), RDMEM_B( RW(src) ) );
/*TODO*///	RW(dst)--;
/*TODO*///	RW(src)--;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ini	 @rd,@rs,ra
/*TODO*/// inir	 @rd,@rs,ra
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode Z3B_ssss_0000_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	WRMEM_W( RW(dst), RDPORT_W( 0, RW(src) ) );
/*TODO*///	RW(dst) += 2;
/*TODO*///	RW(src) += 2;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sini	 @rd,@rs,ra
/*TODO*/// sinir	 @rd,@rs,ra
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3B_ssss_0001_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	WRMEM_W( RW(dst), RDPORT_W( 1, RW(src) ) );
/*TODO*///	RW(dst) += 2;
/*TODO*///	RW(src) += 2;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// outi	 @rd,@rs,ra
/*TODO*/// outir	 @rd,@rs,ra
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode Z3B_ssss_0010_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	WRPORT_W( 0, RW(dst), RDMEM_W( RW(src) ) );
/*TODO*///	RW(dst) += 2;
/*TODO*///	RW(src) += 2;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// souti	 @rd,@rs,ra
/*TODO*/// soutir  @rd,@rs,ra
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3B_ssss_0011_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	WRPORT_W( 1, RW(dst), RDMEM_W( RW(src) ) );
/*TODO*///	RW(dst) += 2;
/*TODO*///	RW(src) += 2;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// in 	 rd,imm16
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3B_dddd_0100_imm16 = new opcode() {
        public void handler() {
/*TODO*///    GET_DST(OP0,NIB2);
/*TODO*///    GET_IMM16(OP1);
/*TODO*///	RW(dst) = RDPORT_W( 0, imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sin	 rd,imm16
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3B_dddd_0101_imm16 = new opcode() {
        public void handler() {
/*TODO*///    GET_DST(OP0,NIB2);
/*TODO*///    GET_IMM16(OP1);
/*TODO*///	RW(dst) = RDPORT_W( 1, imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// out	 imm16,rs
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode Z3B_ssss_0110_imm16 = new opcode() {
        public void handler() {
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    GET_IMM16(OP1);
/*TODO*///	WRPORT_W( 0, imm16, RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sout	 imm16,rbs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3B_ssss_0111_imm16 = new opcode() {
        public void handler() {
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    GET_IMM16(OP1);
/*TODO*///	WRPORT_W( 1, imm16, RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ind	 @rd,@rs,ra
/*TODO*/// indr	 @rd,@rs,ra
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode Z3B_ssss_1000_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	WRMEM_W( RW(dst), RDPORT_W( 0, RW(src) ) );
/*TODO*///	RW(dst) -= 2;
/*TODO*///	RW(src) -= 2;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sind	 @rd,@rs,ra
/*TODO*/// sindr	 @rd,@rs,ra
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3B_ssss_1001_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	WRMEM_W( RW(dst), RDPORT_W( 1, RW(src) ) );
/*TODO*///	RW(dst) -= 2;
/*TODO*///	RW(src) -= 2;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// outd	 @rd,@rs,ra
/*TODO*/// outdr	 @rd,@rs,ra
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode Z3B_ssss_1010_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	WRPORT_W( 0, RW(dst), RDMEM_W( RW(src) ) );
/*TODO*///	RW(dst) -= 2;
/*TODO*///	RW(src) -= 2;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// soutd	 @rd,@rs,ra
/*TODO*/// soutdr  @rd,@rs,ra
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3B_ssss_1011_0000_aaaa_dddd_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	WRPORT_W( 1, RW(dst), RDMEM_W( RW(src) ) );
/*TODO*///	RW(dst) -= 2;
/*TODO*///	RW(src) -= 2;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// inb	 rbd,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3C_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	RB(dst) = RDPORT_B( 0, RDMEM_W( RW(src) ) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// in 	 rd,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z3D_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	RW(dst) = RDPORT_W( 0, RDMEM_W( RW(src) ) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// outb	 @rd,rbs
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode Z3E_dddd_ssss = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	WRPORT_B( 0, RDMEM_W( RW(dst) ), RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// out	 @rd,rs
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode Z3F_dddd_ssss = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	WRPORT_W( 0, RDMEM_W( RW(dst) ), RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// addb	 rbd,addr
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode Z40_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RB(dst) = ADDB( RB(dst), RDMEM_B(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// addb	 rbd,addr(rs)
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode Z40_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RB(dst) = ADDB( RB(dst), RDMEM_B(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// add	 rd,addr
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z41_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RW(dst) = ADDW( RW(dst), RDMEM_W(addr)); /* EHC */
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// add	 rd,addr(rs)
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z41_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RW(dst) = ADDW( RW(dst), RDMEM_W(addr) );	/* ASG */
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// subb	 rbd,addr
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode Z42_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RB(dst) = SUBB( RB(dst), RDMEM_B(addr)); /* EHC */
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// subb	 rbd,addr(rs)
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode Z42_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RB(dst) = SUBB( RB(dst), RDMEM_B(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sub	 rd,addr
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z43_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RW(dst) = SUBW( RW(dst), RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sub	 rd,addr(rs)
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z43_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RW(dst) = SUBW( RW(dst), RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// orb	 rbd,addr
/*TODO*/// flags:  CZSP--
/*TODO*/// ******************************************/
    public static opcode Z44_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RB(dst) = ORB( RB(dst), RDMEM_B(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// orb	 rbd,addr(rs)
/*TODO*/// flags:  CZSP--
/*TODO*/// ******************************************/
    public static opcode Z44_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RB(dst) = ORB( RB(dst), RDMEM_B(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// or 	 rd,addr
/*TODO*/// flags:  CZS---
/*TODO*/// ******************************************/
    public static opcode Z45_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RW(dst) = ORW( RW(dst), RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// or 	 rd,addr(rs)
/*TODO*/// flags:  CZS---
/*TODO*/// ******************************************/
    public static opcode Z45_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RW(dst) = ORW( RW(dst), RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// andb	 rbd,addr
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z46_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RB(dst) = ANDB( RB(dst), RDMEM_B(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// andb	 rbd,addr(rs)
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z46_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RB(dst) = ANDB( RB(dst), RDMEM_B(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// and	 rd,addr
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z47_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RW(dst) = ANDW( RW(dst), RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// and	 rd,addr(rs)
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z47_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RW(dst) = ANDW( RW(dst), RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// xorb	 rbd,addr
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z48_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RB(dst) = XORB( RB(dst), RDMEM_B(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// xorb	 rbd,addr(rs)
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z48_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RB(dst) = XORB( RB(dst), RDMEM_B(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// xor	 rd,addr
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z49_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RW(dst) = XORW( RW(dst), RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// xor	 rd,addr(rs)
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z49_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RW(dst) = XORW( RW(dst), RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpb	 rbd,addr
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z4A_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	CPB( RB(dst), RDMEM_B(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpb	 rbd,addr(rs)
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z4A_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	CPB( RB(dst), RDMEM_B(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cp 	 rd,addr
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z4B_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	CPW( RW(dst), RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cp 	 rd,addr(rs)
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z4B_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	CPW( RW(dst), RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// comb	 addr
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z4C_0000_0000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_B( addr, COMB(RDMEM_W(addr)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpb	 addr,imm8
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z4C_0000_0001_addr_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	GET_IMM8(OP2);
/*TODO*///	CPB( RDMEM_B(addr), imm8 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// negb	 addr
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z4C_0000_0010_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_B( addr, NEGB(RDMEM_B(addr)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// testb	 addr
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z4C_0000_0100_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	TESTB(RDMEM_B(addr));
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 addr,imm8
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z4C_0000_0101_addr_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	GET_IMM8(OP2);
/*TODO*///	WRMEM_B( addr, imm8 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// tsetb	 addr
/*TODO*/// flags:  --S---
/*TODO*/// ******************************************/
    public static opcode Z4C_0000_0110_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///    if (RDMEM_B(addr) & S08) SET_S; else CLR_S;
/*TODO*///    WRMEM_B(addr, 0xff);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// clrb	 addr
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z4C_0000_1000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_B( addr, 0 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// comb	 addr(rd)
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z4C_ddN0_0000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_B( addr, COMB(RDMEM_B(addr)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpb	 addr(rd),imm8
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z4C_ddN0_0001_addr_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	GET_IMM8(OP2);
/*TODO*///	addr += RW(dst);
/*TODO*///	CPB( RDMEM_B(addr), imm8 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// negb	 addr(rd)
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z4C_ddN0_0010_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_B( addr, NEGB(RDMEM_B(addr)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// testb	 addr(rd)
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z4C_ddN0_0100_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	TESTB( RDMEM_B(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 addr(rd),imm8
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z4C_ddN0_0101_addr_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	GET_IMM8(OP2);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_B( addr, imm8 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// tsetb	 addr(rd)
/*TODO*/// flags:  --S---
/*TODO*/// ******************************************/
    public static opcode Z4C_ddN0_0110_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///    if (RDMEM_B(addr) & S08) SET_S; else CLR_S;
/*TODO*///    WRMEM_B(addr, 0xff);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// clrb	 addr(rd)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z4C_ddN0_1000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_B( addr, 0 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// com	 addr
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z4D_0000_0000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_W( addr, COMW(RDMEM_W(addr)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cp 	 addr,imm16
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z4D_0000_0001_addr_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	GET_IMM16(OP2);
/*TODO*///	CPW( RDMEM_W(addr), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// neg	 addr
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z4D_0000_0010_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_W( addr, NEGW(RDMEM_W(addr)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// test	 addr
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z4D_0000_0100_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	TESTW( RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 addr,imm16
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z4D_0000_0101_addr_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	GET_IMM16(OP2);
/*TODO*///	WRMEM_W( addr, imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// tset	 addr
/*TODO*/// flags:  --S---
/*TODO*/// ******************************************/
    public static opcode Z4D_0000_0110_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///    if (RDMEM_W(addr) & S16) SET_S; else CLR_S;
/*TODO*///    WRMEM_W(addr, 0xffff);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// clr	 addr
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z4D_0000_1000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_W( addr, 0 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// com	 addr(rd)
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z4D_ddN0_0000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_W( addr, COMW(RDMEM_W(addr)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cp 	 addr(rd),imm16
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z4D_ddN0_0001_addr_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	GET_IMM16(OP2);
/*TODO*///	addr += RW(dst);
/*TODO*///	CPW( RDMEM_W(addr), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// neg	 addr(rd)
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z4D_ddN0_0010_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_W( addr, NEGW(RDMEM_W(addr)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// test	 addr(rd)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z4D_ddN0_0100_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	TESTW( RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 addr(rd),imm16
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z4D_ddN0_0101_addr_imm16 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	GET_IMM16(OP2);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_W( addr, imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// tset	 addr(rd)
/*TODO*/// flags:  --S---
/*TODO*/// ******************************************/
    public static opcode Z4D_ddN0_0110_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///    if (RDMEM_W(addr) & S16) SET_S; else CLR_S;
/*TODO*///    WRMEM_W(addr, 0xffff);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// clr	 addr(rd)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z4D_ddN0_1000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_W( addr, 0 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 addr(rd),rbs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z4E_ddN0_ssN0_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_B( addr, RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpl	 rrd,addr
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z50_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	CPL( RL(dst), RDMEM_L(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpl	 rrd,addr(rs)
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z50_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	CPL( RL(dst), RDMEM_L(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// pushl	 @rd,addr
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z51_ddN0_0000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	PUSHL( dst, RDMEM_L(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// pushl	 @rd,addr(rs)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z51_ddN0_ssN0_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	PUSHL( dst, RDMEM_L(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// subl	 rrd,addr
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z52_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RL(dst) = SUBL( RL(dst), RDMEM_L(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// subl	 rrd,addr(rs)
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z52_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RL(dst) = SUBL( RL(dst), RDMEM_L(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// push	 @rd,addr
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z53_ddN0_0000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	PUSHW( dst, RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// push	 @rd,addr(rs)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z53_ddN0_ssN0_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	PUSHW( dst, RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldl	 rrd,addr
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z54_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RL(dst) = RDMEM_L( addr );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldl	 rrd,addr(rs)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z54_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RL(dst) = RDMEM_L( addr );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// popl	 addr,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z55_ssN0_0000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_L( addr, POPL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// popl	 addr(rd),@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z55_ssN0_ddN0_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_L( addr, POPL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// addl	 rrd,addr
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z56_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RL(dst) = ADDL( RL(dst), RDMEM_L(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// addl	 rrd,addr(rs)
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z56_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RL(dst) = ADDL( RL(dst), RDMEM_L(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// pop	 addr,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z57_ssN0_0000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_W( addr, POPW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// pop	 addr(rd),@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z57_ssN0_ddN0_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_W( addr, POPW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// multl	 rqd,addr
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z58_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RQ(dst) = MULTL( RQ(dst), RDMEM_L(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// multl	 rqd,addr(rs)
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z58_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RQ(dst) = MULTL( RQ(dst), RDMEM_L(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// mult	 rrd,addr
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z59_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RL(dst) = MULTW( RL(dst), RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// mult	 rrd,addr(rs)
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z59_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RL(dst) = MULTW( RL(dst), RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// divl	 rqd,addr
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z5A_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RQ(dst) = DIVL( RQ(dst), RDMEM_L(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// divl	 rqd,addr(rs)
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z5A_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RQ(dst) = DIVL( RQ(dst), RDMEM_L(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// div	 rrd,addr
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z5B_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RL(dst) = DIVW( RL(dst), RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// div	 rrd,addr(rs)
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z5B_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RL(dst) = DIVW( RL(dst), RDMEM_W(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldm	 rd,addr,n
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z5C_0000_0001_0000_dddd_0000_nmin1_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP1,NIB1);
/*TODO*///	GET_CNT(OP1,NIB3);
/*TODO*///	GET_ADDR(OP2);
/*TODO*///	while (cnt-- >= 0) {
/*TODO*///		RW(dst) = RDMEM_W(addr);
/*TODO*///		dst = (dst+1) & 15;
/*TODO*///		addr = (addr + 2) & 0xffff;
/*TODO*///	}
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// testl	 addr
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z5C_0000_1000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	TESTL( RDMEM_L(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldm	 addr,rs,n
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z5C_0000_1001_0000_ssss_0000_nmin1_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP1,NIB1);
/*TODO*///	GET_CNT(OP1,NIB3);
/*TODO*///	GET_ADDR(OP2);
/*TODO*///	while (cnt-- >= 0) {
/*TODO*///		WRMEM_W( addr, RW(src) );
/*TODO*///		src = (src+1) & 15;
/*TODO*///		addr = (addr + 2) & 0xffff;
/*TODO*///	}
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// testl	 addr(rd)
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z5C_ddN0_1000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	TESTL( RDMEM_L(addr) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldm	 addr(rd),rs,n
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z5C_ddN0_1001_0000_ssN0_0000_nmin1_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB1);
/*TODO*///	GET_CNT(OP1,NIB3);
/*TODO*///	GET_ADDR(OP2);
/*TODO*///	addr += RW(dst);
/*TODO*///	while (cnt-- >= 0) {
/*TODO*///		WRMEM_W( addr, RW(src) );
/*TODO*///		src = (src+1) & 15;
/*TODO*///		addr = (addr + 2) & 0xffff;
/*TODO*///	}
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldm	 rd,addr(rs),n
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z5C_ssN0_0001_0000_dddd_0000_nmin1_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_DST(OP1,NIB1);
/*TODO*///	GET_CNT(OP1,NIB3);
/*TODO*///	GET_ADDR(OP2);
/*TODO*///	addr += RW(src);
/*TODO*///	while (cnt-- >= 0) {
/*TODO*///		RW(dst) = RDMEM_W(addr);
/*TODO*///		dst = (dst+1) & 15;
/*TODO*///		addr = (addr + 2) & 0xffff;
/*TODO*///	}
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldl	 addr,rrs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z5D_0000_ssss_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_L( addr, RL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldl	 addr(rd),rrs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z5D_ddN0_ssss_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_L( addr, RL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// jp 	 cc,addr
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z5E_0000_cccc_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_CCC(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) PC = addr; break;
/*TODO*///		case  1: if (CC1) PC = addr; break;
/*TODO*///		case  2: if (CC2) PC = addr; break;
/*TODO*///		case  3: if (CC3) PC = addr; break;
/*TODO*///		case  4: if (CC4) PC = addr; break;
/*TODO*///		case  5: if (CC5) PC = addr; break;
/*TODO*///		case  6: if (CC6) PC = addr; break;
/*TODO*///		case  7: if (CC7) PC = addr; break;
/*TODO*///		case  8: if (CC8) PC = addr; break;
/*TODO*///		case  9: if (CC9) PC = addr; break;
/*TODO*///		case 10: if (CCA) PC = addr; break;
/*TODO*///		case 11: if (CCB) PC = addr; break;
/*TODO*///		case 12: if (CCC) PC = addr; break;
/*TODO*///		case 13: if (CCD) PC = addr; break;
/*TODO*///		case 14: if (CCE) PC = addr; break;
/*TODO*///		case 15: if (CCF) PC = addr; break;
/*TODO*///	}
/*TODO*///	change_pc16bew(PC);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// jp 	 cc,addr(rd)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z5E_ddN0_cccc_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_CCC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) PC = addr; break;
/*TODO*///		case  1: if (CC1) PC = addr; break;
/*TODO*///		case  2: if (CC2) PC = addr; break;
/*TODO*///		case  3: if (CC3) PC = addr; break;
/*TODO*///		case  4: if (CC4) PC = addr; break;
/*TODO*///		case  5: if (CC5) PC = addr; break;
/*TODO*///		case  6: if (CC6) PC = addr; break;
/*TODO*///		case  7: if (CC7) PC = addr; break;
/*TODO*///		case  8: if (CC8) PC = addr; break;
/*TODO*///		case  9: if (CC9) PC = addr; break;
/*TODO*///		case 10: if (CCA) PC = addr; break;
/*TODO*///		case 11: if (CCB) PC = addr; break;
/*TODO*///		case 12: if (CCC) PC = addr; break;
/*TODO*///		case 13: if (CCD) PC = addr; break;
/*TODO*///		case 14: if (CCE) PC = addr; break;
/*TODO*///		case 15: if (CCF) PC = addr; break;
/*TODO*///	}
/*TODO*///	change_pc16bew(PC);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// call	 addr
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z5F_0000_0000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	PUSHW( SP, PC );
/*TODO*///	PC = addr;
/*TODO*///	change_pc16bew(PC);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// call	 addr(rd)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z5F_ddN0_0000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	PUSHW( SP, PC );
/*TODO*///	addr += RW(dst);
/*TODO*///	PC = addr;
/*TODO*///	change_pc16bew(PC);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 rbd,addr
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z60_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RB(dst) = RDMEM_B(addr);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 rbd,addr(rs)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z60_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RB(dst) = RDMEM_B(addr);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 rd,addr
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z61_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RW(dst) = RDMEM_W(addr);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 rd,addr(rs)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z61_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///	RW(dst) = RDMEM_W(addr);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// resb	 addr,imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z62_0000_imm4_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_B( addr, RDMEM_B(addr) & ~bit );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// resb	 addr(rd),imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z62_ddN0_imm4_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_B( addr, RDMEM_B(addr) & ~bit );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// res	 addr,imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z63_0000_imm4_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_W( addr, RDMEM_W(addr) & ~bit );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// res	 addr(rd),imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z63_ddN0_imm4_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_W( addr, RDMEM_W(addr) & ~bit );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// setb	 addr,imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z64_0000_imm4_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_B( addr, RDMEM_B(addr) | bit );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// setb	 addr(rd),imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z64_ddN0_imm4_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_B( addr, RDMEM_B(addr) | bit );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// set	 addr,imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z65_0000_imm4_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_W( addr, RDMEM_W(addr) | bit );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// set	 addr(rd),imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z65_ddN0_imm4_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_W( addr, RDMEM_W(addr) | bit );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// bitb	 addr,imm4
/*TODO*/// flags:  -Z----
/*TODO*/// ******************************************/
    public static opcode Z66_0000_imm4_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	if ( RDMEM_B(addr) & bit) CLR_Z; else SET_Z;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// bitb	 addr(rd),imm4
/*TODO*/// flags:  -Z----
/*TODO*/// ******************************************/
    public static opcode Z66_ddN0_imm4_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///    addr += RW(dst);
/*TODO*///    if ( RDMEM_B(addr) & bit) CLR_Z; else SET_Z;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// bit	 addr,imm4
/*TODO*/// flags:  -Z----
/*TODO*/// ******************************************/
    public static opcode Z67_0000_imm4_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	if ( RDMEM_W(addr) & bit) CLR_Z; else SET_Z;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// bit	 addr(rd),imm4
/*TODO*/// flags:  -Z----
/*TODO*/// ******************************************/
    public static opcode Z67_ddN0_imm4_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///    addr += RW(dst);
/*TODO*///	if ( RDMEM_W(addr) & bit) CLR_Z; else SET_Z;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// incb	 addr,imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode Z68_0000_imm4m1_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_B( addr, INCB(RDMEM_B(addr), i4p1) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// incb	 addr(rd),imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode Z68_ddN0_imm4m1_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_B( addr, INCB(RDMEM_B(addr), i4p1) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// inc	 addr,imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode Z69_0000_imm4m1_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_W( addr, INCW(RDMEM_W(addr), i4p1) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// inc	 addr(rd),imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode Z69_ddN0_imm4m1_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_W( addr, INCW(RDMEM_W(addr), i4p1) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// decb	 addr,imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode Z6A_0000_imm4m1_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_B( addr, DECB(RDMEM_B(addr), i4p1) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// decb	 addr(rd),imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode Z6A_ddN0_imm4m1_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_B( addr, DECB(RDMEM_B(addr), i4p1) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// dec	 addr,imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode Z6B_0000_imm4m1_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_W( addr, DECW(RDMEM_W(addr), i4p1) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// dec	 addr(rd),imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode Z6B_ddN0_imm4m1_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_W( addr, DECW(RDMEM_W(addr), i4p1) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// exb	 rbd,addr
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z6C_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	UINT8 tmp = RDMEM_B(addr);
/*TODO*///	WRMEM_B(addr, RB(dst));
/*TODO*///	RB(dst) = tmp;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// exb	 rbd,addr(rs)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z6C_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	UINT8 tmp;
/*TODO*///	addr += RW(src);
/*TODO*///	tmp = RDMEM_B(addr);
/*TODO*///	WRMEM_B(addr, RB(dst));
/*TODO*///    RB(dst) = tmp;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ex 	 rd,addr
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z6D_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	UINT16 tmp = RDMEM_W(addr);
/*TODO*///	WRMEM_W( addr, RW(dst) );
/*TODO*///	RW(dst) = tmp;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ex 	 rd,addr(rs)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z6D_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	UINT16 tmp;
/*TODO*///	addr += RW(src);
/*TODO*///	tmp = RDMEM_W(addr);
/*TODO*///	WRMEM_W( addr, RW(dst) );
/*TODO*///    RW(dst) = tmp;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 addr,rbs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z6E_0000_ssss_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_B( addr, RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 addr(rd),rbs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z6E_ddN0_ssss_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_B( addr, RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 addr,rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z6F_0000_ssss_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	WRMEM_W( addr, RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 addr(rd),rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z6F_ddN0_ssss_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(dst);
/*TODO*///	WRMEM_W( addr, RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 rbd,rs(rx)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z70_ssN0_dddd_0000_xxxx_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_IDX(OP1,NIB1);
/*TODO*///	RB(dst) = RDMEM_B( (UINT16)(RW(src) + RW(idx)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 rd,rs(rx)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z71_ssN0_dddd_0000_xxxx_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_IDX(OP1,NIB1);
/*TODO*///	RW(dst) = RDMEM_W( (UINT16)(RW(src) + RW(idx)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 rd(rx),rbs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z72_ddN0_ssss_0000_xxxx_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IDX(OP1,NIB1);
/*TODO*///	WRMEM_B( (UINT16)(RW(dst) + RW(idx)), RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 rd(rx),rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z73_ddN0_ssss_0000_xxxx_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IDX(OP1,NIB1);
/*TODO*///	WRMEM_W( (UINT16)(RW(dst) + RW(idx)), RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// lda	 prd,rs(rx)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z74_ssN0_dddd_0000_xxxx_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_IDX(OP1,NIB1);
/*TODO*///	RW(dst) = (UINT16)(RW(src) + RW(idx));
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldl	 rrd,rs(rx)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z75_ssN0_dddd_0000_xxxx_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_IDX(OP1,NIB1);
/*TODO*///	RL(dst) = RDMEM_L( (UINT16)(RW(src) + RW(idx)) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// lda	 prd,addr
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z76_0000_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	RW(dst) = addr;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// lda	 prd,addr(rs)
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z76_ssN0_dddd_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	addr += RW(src);
/*TODO*///    RW(dst) = addr;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldl	 rd(rx),rrs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z77_ddN0_ssss_0000_xxxx_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IDX(OP1,NIB1);
/*TODO*///	WRMEM_L( (UINT16)(RW(dst) + RW(idx)), RL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rsvd78
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z78_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM8(0);
/*TODO*///	LOG(("Z8K#%d %04x: rsvd78 $%02x\n", cpu_getactivecpu(), PC, imm8));
/*TODO*///    if (FCW & F_EPU) {
/*TODO*///		/* Z8001 EPU code goes here */
/*TODO*///		 = new opcode() {imm8;
/*TODO*///    }
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldps	 addr
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode Z79_0000_0000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	UINT16 fcw;
/*TODO*///	fcw = RDMEM_W(addr);
/*TODO*///	PC	= RDMEM_W((UINT16)(addr + 2));
/*TODO*///	CHANGE_FCW(fcw); /* check for user/system mode change */
/*TODO*///    change_pc16bew(PC);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldps	 addr(rs)
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode Z79_ssN0_0000_addr = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_ADDR(OP1);
/*TODO*///	UINT16 fcw;
/*TODO*///	addr += RW(src);
/*TODO*///	fcw = RDMEM_W(addr);
/*TODO*///	PC	= RDMEM_W((UINT16)(addr + 2));
/*TODO*///	CHANGE_FCW(fcw); /* check for user/system mode change */
/*TODO*///    change_pc16bew(PC);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// halt
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z7A_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	IRQ_REQ |= Z8000_HALT;
/*TODO*///	if (z8000_ICount > 0) z8000_ICount = 0;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// iret
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode Z7B_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	UINT16 tag, fcw;
/*TODO*///	tag = POPW( SP );	/* get type tag */
/*TODO*///	fcw = POPW( SP );	/* get FCW	*/
/*TODO*///	PC	= POPW( SP );	/* get PC	*/
/*TODO*///    IRQ_SRV &= ~tag;    /* remove IRQ serviced flag */
/*TODO*///	CHANGE_FCW(fcw);		 /* check for user/system mode change */
/*TODO*///    change_pc16bew(PC);
/*TODO*///	LOG(("Z8K#%d IRET tag $%04x, fcw $%04x, pc $%04x\n", cpu_getactivecpu(), tag, fcw, PC));
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// mset
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z7B_0000_1000 = new opcode() {
        public void handler() {
/*TODO*///	/* set mu-0 line */
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// mres
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z7B_0000_1001 = new opcode() {
        public void handler() {
/*TODO*///	/* reset mu-0 line */
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// mbit
/*TODO*/// flags:  CZS---
/*TODO*/// ******************************************/
    public static opcode Z7B_0000_1010 = new opcode() {
        public void handler() {
/*TODO*///	/* test mu-I line */
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// mreq	 rd
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z7B_dddd_1101 = new opcode() {
        public void handler() {
/*TODO*///	/* test mu-I line, invert cascade to mu-0  */
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// di 	 i2
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z7C_0000_00ii = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM2(OP0,NIB3);
/*TODO*///	UINT16 fcw = FCW;
/*TODO*///	fcw &= ~(imm2 << 11);
/*TODO*///	CHANGE_FCW(fcw);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ei 	 i2
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z7C_0000_01ii = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM2(OP0,NIB3);
/*TODO*///	UINT16 fcw = FCW;
/*TODO*///	fcw |= imm2 << 11;
/*TODO*///	CHANGE_FCW(fcw);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldctl	 rd,ctrl
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z7D_dddd_0ccc = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM3(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	switch (imm3) {
/*TODO*///		case 0:
/*TODO*///			RW(dst) = FCW;
/*TODO*///			break;
/*TODO*///		case 3:
/*TODO*///			RW(dst) = REFRESH;
/*TODO*///			break;
/*TODO*///		case 5:
/*TODO*///			RW(dst) = PSAP;
/*TODO*///			break;
/*TODO*///		case 7:
/*TODO*///			RW(dst) = NSP;
/*TODO*///			break;
/*TODO*///		default:
/*TODO*///			LOG(("Z8K#%d LDCTL R%d,%d\n", cpu_getactivecpu(), dst, imm3));
/*TODO*///    }
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldctl	 ctrl,rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z7D_ssss_1ccc = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM3(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	switch (imm3) {
/*TODO*///		case 0:
/*TODO*///			{
/*TODO*///				UINT16 fcw;
/*TODO*///				fcw = RW(src);
/*TODO*///				CHANGE_FCW(fcw); /* check for user/system mode change */
/*TODO*///			}
/*TODO*///            break;
/*TODO*///		case 3:
/*TODO*///			REFRESH = RW(src);
/*TODO*///			break;
/*TODO*///		case 5:
/*TODO*///			PSAP = RW(src);
/*TODO*///			break;
/*TODO*///		case 7:
/*TODO*///			NSP = RW(src);
/*TODO*///			break;
/*TODO*///		default:
/*TODO*///			LOG(("Z8K#%d LDCTL %d,R%d\n", cpu_getactivecpu(), imm3, src));
/*TODO*///    }
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rsvd7e
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z7E_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM8(0);
/*TODO*///	LOG(("Z8K#%d %04x: rsvd7e $%02x\n", cpu_getactivecpu(), PC, imm8));
/*TODO*///    if (FCW & F_EPU) {
/*TODO*///		/* Z8001 EPU code goes here */
/*TODO*///		 = new opcode() {imm8;
/*TODO*///    }
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sc 	 imm8
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode Z7F_imm8 = new opcode() {
        public void handler() {
/*TODO*///    GET_IMM8(0);
/*TODO*///	/* execute system call via IRQ */
/*TODO*///	IRQ_REQ = Z8000_SYSCALL | imm8;
/*TODO*///
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// addb	 rbd,rbs
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode Z80_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RB(dst) = ADDB( RB(dst), RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// add	 rd,rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z81_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = ADDW( RW(dst), RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// subb	 rbd,rbs
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode Z82_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RB(dst) = SUBB( RB(dst), RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sub	 rd,rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z83_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = SUBW( RW(dst), RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// orb	 rbd,rbs
/*TODO*/// flags:  CZSP--
/*TODO*/// ******************************************/
    public static opcode Z84_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RB(dst) = ORB( RB(dst), RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// or 	 rd,rs
/*TODO*/// flags:  CZS---
/*TODO*/// ******************************************/
    public static opcode Z85_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = ORW( RW(dst), RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// andb	 rbd,rbs
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z86_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RB(dst) = ANDB( RB(dst), RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// and	 rd,rs
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z87_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = ANDW( RW(dst), RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// xorb	 rbd,rbs
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z88_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RB(dst) = XORB( RB(dst), RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// xor	 rd,rs
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z89_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = XORW( RW(dst), RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpb	 rbd,rbs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z8A_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	CPB( RB(dst), RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cp 	 rd,rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z8B_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	CPW( RW(dst), RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// comb	 rbd
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z8C_dddd_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RB(dst) = COMB( RB(dst) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// negb	 rbd
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z8C_dddd_0010 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RB(dst) = NEGB( RB(dst) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// testb	 rbd
/*TODO*/// flags:  -ZSP--
/*TODO*/// ******************************************/
    public static opcode Z8C_dddd_0100 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	TESTB( RB(dst) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// tsetb	 rbd
/*TODO*/// flags:  --S---
/*TODO*/// ******************************************/
    public static opcode Z8C_dddd_0110 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///    if (RB(dst) & S08) SET_S; else CLR_S;
/*TODO*///    RB(dst) = 0xff;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// clrb	 rbd
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z8C_dddd_1000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RB(dst) = 0;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// nop
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z8D_0000_0111 = new opcode() {
        public void handler() {
/*TODO*///	/* nothing */
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// com	 rd
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z8D_dddd_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RW(dst) = COMW( RW(dst) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// neg	 rd
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z8D_dddd_0010 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RW(dst) = NEGW( RW(dst) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// test	 rd
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z8D_dddd_0100 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	TESTW( RW(dst) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// tset	 rd
/*TODO*/// flags:  --S---
/*TODO*/// ******************************************/
    public static opcode Z8D_dddd_0110 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///    if (RW(dst) & S16) SET_S; else CLR_S;
/*TODO*///    RW(dst) = 0xffff;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// clr	 rd
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z8D_dddd_1000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RW(dst) = 0;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// setflg  imm4
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z8D_imm4_0001 = new opcode() {
        public void handler() {
/*TODO*///	FCW |= Z.op[0] & 0x00f0;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// resflg  imm4
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z8D_imm4_0011 = new opcode() {
        public void handler() {
/*TODO*///	FCW &= ~(Z.op[0] & 0x00f0);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// comflg  flags
/*TODO*/// flags:  CZSP--
/*TODO*/// ******************************************/
    public static opcode Z8D_imm4_0101 = new opcode() {
        public void handler() {
/*TODO*///	FCW ^= (Z.op[0] & 0x00f0);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ext8e	 imm8
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z8E_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM8(0);
/*TODO*///	LOG(("Z8K#%d %04x: ext8e  $%02x\n", cpu_getactivecpu(), PC, imm8));
/*TODO*///    if (FCW & F_EPU) {
/*TODO*///		/* Z8001 EPU code goes here */
/*TODO*///		 = new opcode() {imm8;
/*TODO*///    }
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ext8f	 imm8
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z8F_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM8(0);
/*TODO*///	LOG(("Z8K#%d %04x: ext8f  $%02x\n", cpu_getactivecpu(), PC, imm8));
/*TODO*///    if (FCW & F_EPU) {
/*TODO*///		/* Z8001 EPU code goes here */
/*TODO*///		 = new opcode() {imm8;
/*TODO*///    }
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpl	 rrd,rrs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z90_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///    GET_DST(OP0,NIB3);
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    CPL( RL(dst), RL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// pushl	 @rd,rrs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z91_ddN0_ssss = new opcode() {
        public void handler() {
/*TODO*///    GET_SRC(OP0,NIB3);
/*TODO*///    GET_DST(OP0,NIB2);
/*TODO*///    PUSHL( dst, RL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// subl	 rrd,rrs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z92_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///    GET_DST(OP0,NIB3);
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    RL(dst) = SUBL( RL(dst), RL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// push	 @rd,rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z93_ddN0_ssss = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	PUSHW(dst, RW(src));
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldl	 rrd,rrs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z94_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RL(dst) = RL(src);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// popl	 rrd,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z95_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///    GET_DST(OP0,NIB3);
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    RL(dst) = POPL( src );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// addl	 rrd,rrs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z96_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RL(dst) = ADDL( RL(dst), RL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// pop	 rd,@rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z97_ssN0_dddd = new opcode() {
        public void handler() {
/*TODO*///    GET_DST(OP0,NIB3);
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    RW(dst) = POPW( src );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// multl	 rqd,rrs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z98_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///    GET_DST(OP0,NIB3);
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///	RQ(dst) = MULTL( RQ(dst), RL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// mult	 rrd,rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z99_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///    GET_DST(OP0,NIB3);
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///	RL(dst) = MULTW( RL(dst), RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// divl	 rqd,rrs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z9A_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///    GET_DST(OP0,NIB3);
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    RQ(dst) = DIVL( RQ(dst), RL(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// div	 rrd,rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode Z9B_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///    GET_DST(OP0,NIB3);
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    RL(dst) = DIVW( RL(dst), RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// testl	 rrd
/*TODO*/// flags:  -ZS---
/*TODO*/// ******************************************/
    public static opcode Z9C_dddd_1000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	CLR_ZS;
/*TODO*///	if (!RL(dst)) SET_Z;
/*TODO*///    else if (RL(dst) & S32) SET_S;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rsvd9d
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z9D_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM8(0);
/*TODO*///	LOG(("Z8K#%d %04x: rsvd9d $%02x\n", cpu_getactivecpu(), PC, imm8));
/*TODO*///    if (FCW & F_EPU) {
/*TODO*///		/* Z8001 EPU code goes here */
/*TODO*///		 = new opcode() {imm8;
/*TODO*///    }
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ret	 cc
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z9E_0000_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_CCC(OP0,NIB3);
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) PC = POPW( SP ); break;
/*TODO*///		case  1: if (CC1) PC = POPW( SP ); break;
/*TODO*///		case  2: if (CC2) PC = POPW( SP ); break;
/*TODO*///		case  3: if (CC3) PC = POPW( SP ); break;
/*TODO*///		case  4: if (CC4) PC = POPW( SP ); break;
/*TODO*///		case  5: if (CC5) PC = POPW( SP ); break;
/*TODO*///		case  6: if (CC6) PC = POPW( SP ); break;
/*TODO*///		case  7: if (CC7) PC = POPW( SP ); break;
/*TODO*///		case  8: if (CC8) PC = POPW( SP ); break;
/*TODO*///		case  9: if (CC9) PC = POPW( SP ); break;
/*TODO*///		case 10: if (CCA) PC = POPW( SP ); break;
/*TODO*///		case 11: if (CCB) PC = POPW( SP ); break;
/*TODO*///		case 12: if (CCC) PC = POPW( SP ); break;
/*TODO*///		case 13: if (CCD) PC = POPW( SP ); break;
/*TODO*///		case 14: if (CCE) PC = POPW( SP ); break;
/*TODO*///		case 15: if (CCF) PC = POPW( SP ); break;
/*TODO*///	}
/*TODO*///	change_pc16bew(PC);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rsvd9f
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode Z9F_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM8(0);
/*TODO*///	LOG(("Z8K#%d %04x: rsvd9f $%02x\n", cpu_getactivecpu(), PC, imm8));
/*TODO*///    if (FCW & F_EPU) {
/*TODO*///		/* Z8001 EPU code goes here */
/*TODO*///		 = new opcode() {imm8;
/*TODO*///    }
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 rbd,rbs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZA0_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RB(dst) = RB(src);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ld 	 rd,rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZA1_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = RW(src);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// resb	 rbd,imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZA2_dddd_imm4 = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RB(dst) &= ~bit;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// res	 rd,imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZA3_dddd_imm4 = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RW(dst) &= ~bit;
         }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// setb	 rbd,imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZA4_dddd_imm4 = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RB(dst) |= bit;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// set	 rd,imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZA5_dddd_imm4 = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RW(dst) |= bit;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// bitb	 rbd,imm4
/*TODO*/// flags:  -Z----
/*TODO*/// ******************************************/
    public static opcode ZA6_dddd_imm4 = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	if (RB(dst) & bit) CLR_Z; else SET_Z;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// bit	 rd,imm4
/*TODO*/// flags:  -Z----
/*TODO*/// ******************************************/
    public static opcode ZA7_dddd_imm4 = new opcode() {
        public void handler() {
/*TODO*///	GET_BIT(OP0);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	if (RW(dst) & bit) CLR_Z; else SET_Z;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// incb	 rbd,imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode ZA8_dddd_imm4m1 = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RB(dst) = INCB( RB(dst), i4p1);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// inc	 rd,imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode ZA9_dddd_imm4m1 = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RW(dst) = INCW( RW(dst), i4p1 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// decb	 rbd,imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode ZAA_dddd_imm4m1 = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RB(dst) = DECB( RB(dst), i4p1 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// dec	 rd,imm4m1
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode ZAB_dddd_imm4m1 = new opcode() {
        public void handler() {
/*TODO*///	GET_I4M1(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RW(dst) = DECW( RW(dst), i4p1 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// exb	 rbd,rbs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZAC_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	UINT8 tmp = RB(src);
/*TODO*///	RB(src) = RB(dst);
/*TODO*///	RB(dst) = tmp;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ex 	 rd,rs
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZAD_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	UINT16 tmp = RW(src);
/*TODO*///	RW(src) = RW(dst);
/*TODO*///	RW(dst) = tmp;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// tccb	 cc,rbd
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZAE_dddd_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_CCC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	UINT8 tmp = RB(dst) & ~1;
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) tmp |= 1; break;
/*TODO*///		case  1: if (CC1) tmp |= 1; break;
/*TODO*///		case  2: if (CC2) tmp |= 1; break;
/*TODO*///		case  3: if (CC3) tmp |= 1; break;
/*TODO*///		case  4: if (CC4) tmp |= 1; break;
/*TODO*///		case  5: if (CC5) tmp |= 1; break;
/*TODO*///		case  6: if (CC6) tmp |= 1; break;
/*TODO*///		case  7: if (CC7) tmp |= 1; break;
/*TODO*///		case  8: if (CC8) tmp |= 1; break;
/*TODO*///		case  9: if (CC9) tmp |= 1; break;
/*TODO*///		case 10: if (CCA) tmp |= 1; break;
/*TODO*///		case 11: if (CCB) tmp |= 1; break;
/*TODO*///		case 12: if (CCC) tmp |= 1; break;
/*TODO*///		case 13: if (CCD) tmp |= 1; break;
/*TODO*///		case 14: if (CCE) tmp |= 1; break;
/*TODO*///		case 15: if (CCF) tmp |= 1; break;
/*TODO*///    }
/*TODO*///	RB(dst) = tmp;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// tcc	 cc,rd
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZAF_dddd_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_CCC(OP0,NIB3);
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	UINT16 tmp = RW(dst) & ~1;
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) tmp |= 1; break;
/*TODO*///		case  1: if (CC1) tmp |= 1; break;
/*TODO*///		case  2: if (CC2) tmp |= 1; break;
/*TODO*///		case  3: if (CC3) tmp |= 1; break;
/*TODO*///		case  4: if (CC4) tmp |= 1; break;
/*TODO*///		case  5: if (CC5) tmp |= 1; break;
/*TODO*///		case  6: if (CC6) tmp |= 1; break;
/*TODO*///		case  7: if (CC7) tmp |= 1; break;
/*TODO*///		case  8: if (CC8) tmp |= 1; break;
/*TODO*///		case  9: if (CC9) tmp |= 1; break;
/*TODO*///		case 10: if (CCA) tmp |= 1; break;
/*TODO*///		case 11: if (CCB) tmp |= 1; break;
/*TODO*///		case 12: if (CCC) tmp |= 1; break;
/*TODO*///		case 13: if (CCD) tmp |= 1; break;
/*TODO*///		case 14: if (CCE) tmp |= 1; break;
/*TODO*///		case 15: if (CCF) tmp |= 1; break;
/*TODO*///    }
/*TODO*///	RW(dst) = tmp;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// dab	 rbd
/*TODO*/// flags:  CZS---
/*TODO*/// ******************************************/
    public static opcode ZB0_dddd_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	UINT8 result;
/*TODO*///	UINT16 idx = RB(dst);
/*TODO*///	if (FCW & F_C)	idx |= 0x100;
/*TODO*///	if (FCW & F_H)	idx |= 0x200;
/*TODO*///	if (FCW & F_DA) idx |= 0x400;
/*TODO*///	result = Z8000_dab[idx];
/*TODO*///	CLR_CZS;
/*TODO*///	CHK_XXXB_ZS;
/*TODO*///	if (Z8000_dab[idx] & 0x100) SET_C;
/*TODO*///	RB(dst) = result;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// extsb	 rd
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZB1_dddd_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///    RW(dst) = (RW(dst) & 0xff) | ((RW(dst) & S08) ? 0xff00 : 0x0000);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// extsl	 rqd
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZB1_dddd_0111 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	RQ(dst) = COMBINE_U64_U32_U32( (RQ(dst) & S32) ?
/*TODO*///		0xfffffffful : 0, LO32_U32_U64(RQ(dst)));
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// exts	 rrd
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZB1_dddd_1010 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///    RL(dst) = (RL(dst) & 0xffff) | ((RL(dst) & S16) ?
/*TODO*///		0xffff0000ul : 0x00000000ul);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sllb	 rbd,imm8
/*TODO*/// flags:  CZS---
/*TODO*/// srlb	 rbd,imm8
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB2_dddd_0001_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	if (imm16 & S16)
/*TODO*///		RB(dst) = SRLB( RB(dst), -(INT16)imm16 );
/*TODO*///	else
/*TODO*///		RB(dst) = SLLB( RB(dst), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sdlb	 rbd,rs
/*TODO*/// flags:  CZS---
/*TODO*/// ******************************************/
    public static opcode ZB2_dddd_0011_0000_ssss_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB1);
/*TODO*///	RB(dst) = SRLB( RB(dst), (INT8)RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rlb	 rbd,imm1or2
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB2_dddd_00I0 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM1(OP0,NIB3);
/*TODO*///	RB(dst) = RLB( RB(dst), imm1 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rrb	 rbd,imm1or2
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB2_dddd_01I0 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM1(OP0,NIB3);
/*TODO*///	RB(dst) = RRB( RB(dst), imm1 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// slab	 rbd,imm8
/*TODO*/// flags:  CZSV--
/*TODO*/// srab	 rbd,imm8
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB2_dddd_1001_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	if (imm16 & S16)
/*TODO*///		RB(dst) = SRAB( RB(dst), -(INT16)imm16 );
/*TODO*///	else
/*TODO*///		RB(dst) = SLAB( RB(dst), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sdab	 rbd,rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB2_dddd_1011_0000_ssss_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB1);
/*TODO*///	RB(dst) = SDAB( RB(dst), (INT8) RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rlcb	 rbd,imm1or2
/*TODO*/// flags:  -Z----
/*TODO*/// ******************************************/
    public static opcode ZB2_dddd_10I0 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM1(OP0,NIB3);
/*TODO*///	RB(dst) = RLCB( RB(dst), imm1 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rrcb	 rbd,imm1or2
/*TODO*/// flags:  -Z----
/*TODO*/// ******************************************/
    public static opcode ZB2_dddd_11I0 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM1(OP0,NIB3);
/*TODO*///	RB(dst) = RRCB( RB(dst), imm1 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sll	 rd,imm8
/*TODO*/// flags:  CZS---
/*TODO*/// srl	 rd,imm8
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB3_dddd_0001_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	if (imm16 & S16)
/*TODO*///		RW(dst) = SRLW( RW(dst), -(INT16)imm16 );
/*TODO*///	else
/*TODO*///        RW(dst) = SLLW( RW(dst), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sdl	 rd,rs
/*TODO*/// flags:  CZS---
/*TODO*/// ******************************************/
    public static opcode ZB3_dddd_0011_0000_ssss_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB1);
/*TODO*///	RW(dst) = SDLW( RW(dst), (INT8)RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rl 	 rd,imm1or2
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB3_dddd_00I0 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM1(OP0,NIB3);
/*TODO*///	RW(dst) = RLW( RW(dst), imm1 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// slll	 rrd,imm8
/*TODO*/// flags:  CZS---
/*TODO*/// srll	 rrd,imm8
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB3_dddd_0101_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	if (imm16 & S16)
/*TODO*///		RL(dst) = SRLL( RL(dst), -(INT16)imm16 );
/*TODO*///	else
/*TODO*///		RL(dst) = SLLL( RL(dst), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sdll	 rrd,rs
/*TODO*/// flags:  CZS---
/*TODO*/// ******************************************/
    public static opcode ZB3_dddd_0111_0000_ssss_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB1);
/*TODO*///	RL(dst) = SDLL( RL(dst), RW(src) & 0xff );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rr 	 rd,imm1or2
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB3_dddd_01I0 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM1(OP0,NIB3);
/*TODO*///	RW(dst) = RRW( RW(dst), imm1 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sla	 rd,imm8
/*TODO*/// flags:  CZSV--
/*TODO*/// sra	 rd,imm8
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB3_dddd_1001_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	if (imm16 & S16)
/*TODO*///		RW(dst) = SRAW( RW(dst), -(INT16)imm16 );
/*TODO*///	else
/*TODO*///        RW(dst) = SLAW( RW(dst), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sda	 rd,rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB3_dddd_1011_0000_ssss_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB1);
/*TODO*///	RW(dst) = SDAW( RW(dst), (INT8)RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rlc	 rd,imm1or2
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB3_dddd_10I0 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM1(OP0,NIB3);
/*TODO*///	RW(dst) = RLCW( RW(dst), imm1 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// slal	 rrd,imm8
/*TODO*/// flags:  CZSV--
/*TODO*/// sral	 rrd,imm8
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB3_dddd_1101_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM16(OP1);
/*TODO*///	if (imm16 & S16)
/*TODO*///		RL(dst) = SRAL( RL(dst), -(INT16)imm16 );
/*TODO*///	else
/*TODO*///		RL(dst) = SLAL( RL(dst), imm16 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sdal	 rrd,rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB3_dddd_1111_0000_ssss_0000_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB1);
/*TODO*///	RL(dst) = SDAL( RL(dst), RW(src) & 0xff );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rrc	 rd,imm1or2
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB3_dddd_11I0 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM1(OP0,NIB3);
/*TODO*///	RW(dst) = RRCW( RW(dst), imm1 );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// adcb	 rbd,rbs
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode ZB4_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RB(dst) = ADCB( RB(dst), RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// adc	 rd,rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB5_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = ADCW( RW(dst), RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sbcb	 rbd,rbs
/*TODO*/// flags:  CZSVDH
/*TODO*/// ******************************************/
    public static opcode ZB6_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RB(dst) = SBCB( RB(dst), RB(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// sbc	 rd,rs
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZB7_ssss_dddd = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB3);
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	RW(dst) = SBCW( RW(dst), RW(src) );
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// trtib	 @rd,@rs,rr
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode ZB8_ddN0_0010_0000_rrrr_ssN0_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	UINT8 xlt = RDMEM_B( (UINT16)(RW(src) + RDMEM_B(RW(dst))) );
/*TODO*///	RB(2) = xlt;
/*TODO*///	if (xlt) CLR_Z; else SET_Z;
/*TODO*///	RW(dst)++;
/*TODO*///	if (--RW(cnt)) CLR_V; else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// trtirb  @rd,@rs,rbr
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode ZB8_ddN0_0110_0000_rrrr_ssN0_1110 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	UINT8 xlt = RDMEM_B( (UINT16)(RW(src) + RDMEM_B(RW(dst))) );
/*TODO*///	RB(2) = xlt;
/*TODO*///	if (xlt) CLR_Z; else SET_Z;
/*TODO*///	RW(dst)++;
/*TODO*///	if (--RW(cnt)) { CLR_V; PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// trtdb	 @rd,@rs,rbr
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode ZB8_ddN0_1010_0000_rrrr_ssN0_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	UINT8 xlt = RDMEM_B( (UINT16)(RW(src) + RDMEM_B(RW(dst))) );
/*TODO*///	RB(2) = xlt;
/*TODO*///	if (xlt) CLR_Z; else SET_Z;
/*TODO*///    RW(dst)--;
/*TODO*///	if (--RW(cnt)) CLR_V; else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// trtdrb  @rd,@rs,rbr
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode ZB8_ddN0_1110_0000_rrrr_ssN0_1110 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	UINT8 xlt = RDMEM_B( (UINT16)(RW(src) + RDMEM_B(RW(dst))) );
/*TODO*///	RB(2) = xlt;
/*TODO*///	if (xlt) CLR_Z; else SET_Z;
/*TODO*///    RW(dst)--;
/*TODO*///	if (--RW(cnt)) { CLR_V; PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// trib	 @rd,@rs,rbr
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode ZB8_ddN0_0000_0000_rrrr_ssN0_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	UINT8 xlt = RDMEM_B( (UINT16)(RW(src) + RDMEM_B(RW(dst))) );
/*TODO*///	WRMEM_B( RW(dst), xlt );
/*TODO*///	RW(dst)++;
/*TODO*///	if (--RW(cnt)) CLR_V; else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// trirb	 @rd,@rs,rbr
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode ZB8_ddN0_0100_0000_rrrr_ssN0_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	UINT8 xlt = RDMEM_B( (UINT16)(RW(src) + RDMEM_B(RW(dst))) );
/*TODO*///	WRMEM_B( RW(dst), xlt );
/*TODO*///	RW(dst)++;
/*TODO*///	if (--RW(cnt)) { CLR_V; PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// trdb	 @rd,@rs,rbr
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode ZB8_ddN0_1000_0000_rrrr_ssN0_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	UINT8 xlt = RDMEM_B( (UINT16)(RW(src) + RDMEM_B(RW(dst))) );
/*TODO*///	WRMEM_B( RW(dst), xlt );
/*TODO*///    RW(dst)--;
/*TODO*///	if (--RW(cnt)) CLR_V; else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// trdrb	 @rd,@rs,rbr
/*TODO*/// flags:  -ZSV--
/*TODO*/// ******************************************/
    public static opcode ZB8_ddN0_1100_0000_rrrr_ssN0_0000 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_SRC(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	UINT8 xlt = RDMEM_B( (UINT16)(RW(src) + RDMEM_B(RW(dst))) );
/*TODO*///	WRMEM_B( RW(dst), xlt );
/*TODO*///    RW(dst)--;
/*TODO*///	if (--RW(cnt)) { CLR_V; PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rsvdb9
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZB9_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM8(0);
/*TODO*///	LOG(("Z8K#%d %04x: rsvdb9 $%02x\n", cpu_getactivecpu(), PC, imm8));
/*TODO*///    if (FCW & F_EPU) {
/*TODO*///		/* Z8001 EPU code goes here */
/*TODO*///		 = new opcode() {imm8;
/*TODO*///    }
/*TODO*///	 = new opcode() {imm8;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpib	 rbd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBA_ssN0_0000_0000_rrrr_dddd_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	CPB( RB(dst), RDMEM_B(RW(src)) );
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///	RW(src)++;
/*TODO*///	if (--RW(cnt)) CLR_V; else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldib	 @rd,@rs,rr
/*TODO*/// ldibr	 @rd,@rs,rr
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode ZBA_ssN0_0001_0000_rrrr_ddN0_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);	/* repeat? */
/*TODO*///    WRMEM_B( RW(dst), RDMEM_B(RW(src)) );
/*TODO*///	RW(dst)++;
/*TODO*///	RW(src)++;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpsib	 @rd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBA_ssN0_0010_0000_rrrr_ddN0_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	CPB( RDMEM_B(RW(dst)), RDMEM_B(RW(src)) );
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///	RW(dst)++;
/*TODO*///	RW(src)++;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (!(FCW & F_Z)) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpirb	 rbd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBA_ssN0_0100_0000_rrrr_dddd_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	CPB( RB(dst), RDMEM_B(RW(src)) );
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///	RW(src)++;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (!(FCW & F_Z)) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpsirb  @rd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBA_ssN0_0110_0000_rrrr_ddN0_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	CPB( RDMEM_B(RW(dst)), RDMEM_B(RW(src)) );
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///	RW(dst)++;
/*TODO*///	RW(src)++;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (!(FCW & F_Z)) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpdb	 rbd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBA_ssN0_1000_0000_rrrr_dddd_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///    CPB( RB(dst), RDMEM_B(RW(src)) );
/*TODO*///    switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///    RW(src)--;
/*TODO*///	if (--RW(cnt)) CLR_V; else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// lddb	 @rs,@rd,rr
/*TODO*/// lddbr	 @rs,@rd,rr
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode ZBA_ssN0_1001_0000_rrrr_ddN0_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	WRMEM_B( RW(dst), RDMEM_B(RW(src)) );
/*TODO*///	RW(dst)--;
/*TODO*///	RW(src)--;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpsdb	 @rd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBA_ssN0_1010_0000_rrrr_ddN0_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///    CPB( RDMEM_B(RW(dst)), RDMEM_B(RW(src)) );
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///	RW(dst)--;
/*TODO*///	RW(src)--;
/*TODO*///	if (--RW(cnt)) CLR_V; else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpdrb	 rbd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBA_ssN0_1100_0000_rrrr_dddd_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	CPB( RB(dst), RDMEM_B(RW(src)) );
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///    RW(src)--;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (!(FCW & F_Z)) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpsdrb  @rd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBA_ssN0_1110_0000_rrrr_ddN0_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///    CPB( RDMEM_B(RW(dst)), RDMEM_B(RW(src)) );
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///	}
/*TODO*///	RW(dst)--;
/*TODO*///	RW(src)--;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (!(FCW & F_Z)) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpi	 rd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBB_ssN0_0000_0000_rrrr_dddd_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	CPW( RW(dst), RDMEM_W(RW(src)) );
/*TODO*///    switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///    RW(src) += 2;
/*TODO*///	if (--RW(cnt)) CLR_V; else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldi	 @rd,@rs,rr
/*TODO*/// ldir	 @rd,@rs,rr
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode ZBB_ssN0_0001_0000_rrrr_ddN0_x000 = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	WRMEM_W( RW(dst), RDMEM_W(RW(src)) );
/*TODO*///	RW(dst) += 2;
/*TODO*///	RW(src) += 2;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpsi	 @rd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBB_ssN0_0010_0000_rrrr_ddN0_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	CPW( RDMEM_W(RW(dst)), RDMEM_W(RW(src)) );
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///	RW(dst) += 2;
/*TODO*///	RW(src) += 2;
/*TODO*///	if (--RW(cnt)) CLR_V; else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpir	 rd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBB_ssN0_0100_0000_rrrr_dddd_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	CPW( RW(dst), RDMEM_W(RW(src)) );
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///	RW(src) += 2;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (!(FCW & F_Z)) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpsir	 @rd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBB_ssN0_0110_0000_rrrr_ddN0_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	CPW( RDMEM_W(RW(dst)), RDMEM_W(RW(src)) );
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///	RW(dst) += 2;
/*TODO*///    RW(src) += 2;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (!(FCW & F_Z)) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpd	 rd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBB_ssN0_1000_0000_rrrr_dddd_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	CPW( RW(dst), RDMEM_W(RW(src)) );
/*TODO*///    switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///    RW(src) -= 2;
/*TODO*///	if (--RW(cnt)) CLR_V; else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldd     @rs,@rd,rr
/*TODO*/// lddr	 @rs,@rd,rr
/*TODO*/// flags:  ---V--
/*TODO*/// ******************************************/
    public static opcode ZBB_ssN0_1001_0000_rrrr_ddN0_x000 = new opcode() {
        public void handler() {
/*TODO*///    GET_SRC(OP0,NIB2);
/*TODO*///    GET_CNT(OP1,NIB1);
/*TODO*///    GET_DST(OP1,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///    WRMEM_W( RW(dst), RDMEM_W(RW(src)) );
/*TODO*///    RW(dst) -= 2;
/*TODO*///    RW(src) -= 2;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (cc == 0) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpsd	 @rd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBB_ssN0_1010_0000_rrrr_ddN0_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	CPW( RDMEM_W(RW(dst)), RDMEM_W(RW(src)) );
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///    RW(dst) -= 2;
/*TODO*///    RW(src) -= 2;
/*TODO*///	if (--RW(cnt)) CLR_V; else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpdr	 rd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBB_ssN0_1100_0000_rrrr_dddd_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	CPW( RW(dst), RDMEM_W(RW(src)) );
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///    RW(src) -= 2;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (!(FCW & F_Z)) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// cpsdr	 @rd,@rs,rr,cc
/*TODO*/// flags:  CZSV--
/*TODO*/// ******************************************/
    public static opcode ZBB_ssN0_1110_0000_rrrr_ddN0_cccc = new opcode() {
        public void handler() {
/*TODO*///	GET_SRC(OP0,NIB2);
/*TODO*///	GET_CCC(OP1,NIB3);
/*TODO*///	GET_DST(OP1,NIB2);
/*TODO*///	GET_CNT(OP1,NIB1);
/*TODO*///	CPW( RDMEM_W(RW(dst)), RDMEM_W(RW(src)) );
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) SET_Z; else CLR_Z; break;
/*TODO*///		case  1: if (CC1) SET_Z; else CLR_Z; break;
/*TODO*///		case  2: if (CC2) SET_Z; else CLR_Z; break;
/*TODO*///		case  3: if (CC3) SET_Z; else CLR_Z; break;
/*TODO*///		case  4: if (CC4) SET_Z; else CLR_Z; break;
/*TODO*///		case  5: if (CC5) SET_Z; else CLR_Z; break;
/*TODO*///		case  6: if (CC6) SET_Z; else CLR_Z; break;
/*TODO*///		case  7: if (CC7) SET_Z; else CLR_Z; break;
/*TODO*///		case  8: if (CC8) SET_Z; else CLR_Z; break;
/*TODO*///		case  9: if (CC9) SET_Z; else CLR_Z; break;
/*TODO*///		case 10: if (CCA) SET_Z; else CLR_Z; break;
/*TODO*///		case 11: if (CCB) SET_Z; else CLR_Z; break;
/*TODO*///		case 12: if (CCC) SET_Z; else CLR_Z; break;
/*TODO*///		case 13: if (CCD) SET_Z; else CLR_Z; break;
/*TODO*///		case 14: if (CCE) SET_Z; else CLR_Z; break;
/*TODO*///		case 15: if (CCF) SET_Z; else CLR_Z; break;
/*TODO*///    }
/*TODO*///    RW(dst) -= 2;
/*TODO*///    RW(src) -= 2;
/*TODO*///	if (--RW(cnt)) { CLR_V; if (!(FCW & F_Z)) PC -= 4; } else SET_V;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rrdb	 rbb,rba
/*TODO*/// flags:  -Z----
/*TODO*/// ******************************************/
    public static opcode ZBC_aaaa_bbbb = new opcode() {
        public void handler() {
/*TODO*///	UINT8 b = Z.op[0] & 15;
/*TODO*///	UINT8 a = (Z.op[0] >> 4) & 15;
/*TODO*///	UINT8 tmp = RB(b);
/*TODO*///	RB(a) = (RB(a) >> 4) | (RB(b) << 4);
/*TODO*///	RB(b) = (RB(b) & 0xf0) | (tmp & 0x0f);
/*TODO*///    if (RB(b)) CLR_Z; else SET_Z;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldk	 rd,imm4
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZBD_dddd_imm4 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB2);
/*TODO*///	GET_IMM4(OP0,NIB3);
/*TODO*///	RW(dst) = imm4;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rldb	 rbb,rba
/*TODO*/// flags:  -Z----
/*TODO*/// ******************************************/
    public static opcode ZBE_aaaa_bbbb = new opcode() {
        public void handler() {
/*TODO*///	UINT8 b = Z.op[0] & 15;
/*TODO*///	UINT8 a = (Z.op[0] >> 4) & 15;
/*TODO*///	UINT8 tmp = RB(a);
/*TODO*///	RB(a) = (RB(a) << 4) | (RB(b) & 0x0f);
/*TODO*///	RB(b) = (RB(b) & 0xf0) | (tmp >> 4);
/*TODO*///	if (RB(b)) CLR_Z; else SET_Z;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// rsvdbf
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZBF_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_IMM8(0);
/*TODO*///	LOG(("Z8K#%d %04x: rsvdbf $%02x\n", cpu_getactivecpu(), PC, imm8));
/*TODO*///    if (FCW & F_EPU) {
/*TODO*///		/* Z8001 EPU code goes here */
/*TODO*///		 = new opcode() {imm8;
/*TODO*///    }
/*TODO*///	 = new opcode() {imm8;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// ldb	 rbd,imm8
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZC_dddd_imm8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB1);
/*TODO*///	GET_IMM8(0);
/*TODO*///	RB(dst) = imm8;
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// calr	 dsp12
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZD_dsp12 = new opcode() {
        public void handler() {
/*TODO*///	INT16 dsp12 = Z.op[0] & 0xfff;
/*TODO*///	PUSHW( SP, PC );
/*TODO*///	dsp12 = (dsp12 & 2048) ? 4096 -2 * (dsp12 & 2047) : -2 * (dsp12 & 2047);
/*TODO*///	PC += dsp12;
/*TODO*///	change_pc16bew(PC);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// jr 	 cc,dsp8
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZE_cccc_dsp8 = new opcode() {
        public void handler() {
/*TODO*///	GET_DSP8;
/*TODO*///	GET_CCC(OP0,NIB1);
/*TODO*///	switch (cc) {
/*TODO*///		case  0: if (CC0) PC += dsp8 * 2; break;
/*TODO*///		case  1: if (CC1) PC += dsp8 * 2; break;
/*TODO*///		case  2: if (CC2) PC += dsp8 * 2; break;
/*TODO*///		case  3: if (CC3) PC += dsp8 * 2; break;
/*TODO*///		case  4: if (CC4) PC += dsp8 * 2; break;
/*TODO*///		case  5: if (CC5) PC += dsp8 * 2; break;
/*TODO*///		case  6: if (CC6) PC += dsp8 * 2; break;
/*TODO*///		case  7: if (CC7) PC += dsp8 * 2; break;
/*TODO*///		case  8: if (CC8) PC += dsp8 * 2; break;
/*TODO*///		case  9: if (CC9) PC += dsp8 * 2; break;
/*TODO*///		case 10: if (CCA) PC += dsp8 * 2; break;
/*TODO*///		case 11: if (CCB) PC += dsp8 * 2; break;
/*TODO*///		case 12: if (CCC) PC += dsp8 * 2; break;
/*TODO*///		case 13: if (CCD) PC += dsp8 * 2; break;
/*TODO*///		case 14: if (CCE) PC += dsp8 * 2; break;
/*TODO*///		case 15: if (CCF) PC += dsp8 * 2; break;
/*TODO*///    }
/*TODO*///	change_pc16bew(PC);
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// dbjnz   rbd,dsp7
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZF_dddd_0dsp7 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB1);
/*TODO*///    GET_DSP7;
/*TODO*///    RB(dst) -= 1;
/*TODO*///    if (RB(dst)) {
/*TODO*///        PC = PC - 2 * dsp7;
/*TODO*///        change_pc16bew(PC);
/*TODO*///    }
        }
    };
/*TODO*///
/*TODO*////******************************************
/*TODO*/// djnz	 rd,dsp7
/*TODO*/// flags:  ------
/*TODO*/// ******************************************/
    public static opcode ZF_dddd_1dsp7 = new opcode() {
        public void handler() {
/*TODO*///	GET_DST(OP0,NIB1);
/*TODO*///	GET_DSP7;
/*TODO*///	RW(dst) -= 1;
/*TODO*///	if (RW(dst)) {
/*TODO*///		PC = PC - 2 * dsp7;
/*TODO*///		change_pc16bew(PC);
/*TODO*///	}
        }
    };
/*TODO*///
/*TODO*///

}
