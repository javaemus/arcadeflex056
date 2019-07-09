/****************************************************************************

  emu2413.c -- a YM2413 emulator : written by Mitsutaka Okazaki 2001

  2001 01-08 : Version 0.10 -- 1st version.
  2001 01-15 : Version 0.20 -- semi-public version.
  2001 01-16 : Version 0.30 -- 1st public version.
  2001 01-17 : Version 0.31 -- Fixed bassdrum problem.
             : Version 0.32 -- LPF implemented.
  2001 01-18 : Version 0.33 -- Fixed the drum problem, refine the mix-down method.
                            -- Fixed the LFO bug.
  2001 01-24 : Version 0.35 -- Fixed the drum problem,
                               support undocumented EG behavior.
  2001 02-02 : Version 0.38 -- Improved the performance.
                               Fixed the hi-hat and cymbal model.
                               Fixed the default percussive datas.
                               Noise reduction.
                               Fixed the feedback problem.
  References:
    fmopl.c        -- 1999,2000 written by Tatsuyuki Satoh (MAME development).
    fmopl.c(fixed) -- 2000 modified by mamiya (NEZplug development).
    fmgen.cpp      -- 1999,2000 written by cisc.
    fmpac.ill      -- 2000 created by NARUTO.
    MSX-Datapack
    YMU757 data sheet
    YM2143 data sheet

  http://www.angel.ne.jp/~okazaki/ym2413/index_en.html

*****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.sound;

import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame056.sound._2413intfH.*;
import static mame056.sound.ym2413H.*;

public class ym2413 {
    
    
    /*
        If you want to use the attack, decay and release time
        which are introduced on YM2413 specification sheet,
        please define USE_SPEC_ENV_SPEED(not recommended).
    */
    /*TODO*///  /*#define USE_SPEC_ENV_SPEED*/
/*TODO*///
/*TODO*///      /* Size of Sintable ( 1 -- 18 can be used, but 7 -- 14 recommended.)*/
/*TODO*///      #define PG_BITS 9
/*TODO*///      #define PG_WIDTH (1<<PG_BITS)
/*TODO*///
/*TODO*///      /* Phase increment counter */
/*TODO*///      #define DP_BITS 18
/*TODO*///      #define DP_WIDTH (1<<DP_BITS)
/*TODO*///      #define DP_BASE_BITS (DP_BITS - PG_BITS)
/*TODO*///
/*TODO*///      /* Bits number for 48dB (7:0.375dB/step, 8:0.1875dB/step) */
/*TODO*///      #define DB_BITS 8
/*TODO*///      /* Resolution */
/*TODO*///      #define DB_STEP ((double)48.0/(1<<DB_BITS))
/*TODO*///      /* Resolution of sine table */
/*TODO*///      #define SIN_STEP (DB_STEP/16)
/*TODO*///
/*TODO*///      #define DB_MUTE ((1<<DB_BITS)-1)
/*TODO*///
/*TODO*///      /* Volume of Noise (dB) */
/*TODO*///      #define DB_NOISE 6
/*TODO*///
/*TODO*///      /* Bits for envelope */
/*TODO*///      #define EG_BITS 7
/*TODO*///
/*TODO*///      /* Bits for total level */
/*TODO*///      #define TL_BITS   6
/*TODO*///
/*TODO*///      /* Bits for sustine level */
/*TODO*///      #define SL_BITS   4
/*TODO*///
/*TODO*///      /* Bits for liner value */
/*TODO*///      #define DB2LIN_AMP_BITS 9
/*TODO*///      #define SLOT_AMP_BITS (DB2LIN_AMP_BITS)
/*TODO*///
/*TODO*///      /* Bits for envelope phase incremental counter */
/*TODO*///      #define EG_DP_BITS 22
/*TODO*///      #define EG_DP_WIDTH (1<<EG_DP_BITS)
/*TODO*///
/*TODO*///      /* Bits for Pitch and Amp modulator */
/*TODO*///      #define PM_PG_BITS 8
/*TODO*///      #define PM_PG_WIDTH (1<<PM_PG_BITS)
/*TODO*///      #define PM_DP_BITS 16
/*TODO*///      #define PM_DP_WIDTH (1<<PM_DP_BITS)
/*TODO*///      #define AM_PG_BITS 8
/*TODO*///      #define AM_PG_WIDTH (1<<AM_PG_BITS)
/*TODO*///      #define AM_DP_BITS 16
/*TODO*///      #define AM_DP_WIDTH (1<<AM_DP_BITS)
/*TODO*///
/*TODO*///      /* PM table is calcurated by PM_AMP * pow(2,PM_DEPTH*sin(x)/1200) */
/*TODO*///      #define PM_AMP_BITS 8
/*TODO*///      #define PM_AMP (1<<PM_AMP_BITS)
/*TODO*///
/*TODO*///      /* PM speed(Hz) and depth(cent) */
/*TODO*///      #define PM_SPEED 6.4
/*TODO*///      #define PM_DEPTH 13.75
/*TODO*///
/*TODO*///      /* AM speed(Hz) and depth(dB) */
/*TODO*///      #define AM_SPEED 3.7
/*TODO*///      #define AM_DEPTH 4.8
/*TODO*///
/*TODO*///      /* Cut the lower b bit(s) off. */
/*TODO*///      #define HIGHBITS(c,b) ((c)>>(b))
/*TODO*///
/*TODO*///      /* Leave the lower b bit(s). */
/*TODO*///      #define LOWBITS(c,b) ((c)&((1<<(b))-1))
/*TODO*///
/*TODO*///      /* Expand x which is s bits to d bits. */
/*TODO*///      #define EXPAND_BITS(x,s,d) ((x)<<((d)-(s)))
/*TODO*///
/*TODO*///      /* Expand x which is s bits to d bits and fill expanded bits '1' */
/*TODO*///      #define EXPAND_BITS_X(x,s,d) (((x)<<((d)-(s)))|((1<<((d)-(s)))-1))
/*TODO*///
/*TODO*///      /* Adjust envelope speed which depends on sampling rate. */
/*TODO*///      #define rate_adjust(x) (UINT32)((double)(x)*clk/72/rate + 0.5) /* +0.5 for round */
/*TODO*///
/*TODO*///      #define MOD(x) slot[x*2]
/*TODO*///      #define CAR(x) slot[x*2+1]
/*TODO*///      /*
/*TODO*///      #define MOD(x) ch[x].mod
/*TODO*///      #define CAR(x) ch[x].car
/*TODO*///      */
/*TODO*///
/*TODO*///      /* Sampling rate */
/*TODO*///      static INT32 rate ;
/*TODO*///
/*TODO*///      /* Input clock */
/*TODO*///      static double clk ;
/*TODO*///
/*TODO*///      /* WaveTable for each envelope amp */
/*TODO*///      static INT32 fullsintable[PG_WIDTH] ;
/*TODO*///      static INT32 halfsintable[PG_WIDTH] ;
/*TODO*///
/*TODO*///      static INT32 *waveform[2] = {fullsintable,halfsintable} ;
/*TODO*///
/*TODO*///      /* LFO Table */
/*TODO*///      static INT32 pmtable[PM_PG_WIDTH] ;
/*TODO*///      static INT32 amtable[AM_PG_WIDTH] ;
/*TODO*///
/*TODO*///      /* dB to Liner table */
/*TODO*///      static INT32 DB2LIN_TABLE[DB_MUTE+1] ;
/*TODO*///
/*TODO*///      /* Liner to Log curve conversion table (for Attack rate). */
/*TODO*///      static UINT32 AR_ADJUST_TABLE[1<<EG_BITS] ;
/*TODO*///
/*TODO*///      /* Empty voice data */
/*TODO*///      static OPLL_PATCH null_patch = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } ;
/*TODO*///
/*TODO*///      /* Basic voice Data */
/*TODO*///      static OPLL_PATCH default_patch[(16+3)*2] ;
/*TODO*///
/*TODO*///      static unsigned char default_inst[(16+3)*16]={
/*TODO*///      0x49, 0x4C, 0x4C, 0x32, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x61, 0x61, 0x1E, 0x17, 0xF0, 0x7F, 0x07, 0x17, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x13, 0x41, 0x0F, 0x0D, 0xCE, 0xD2, 0x43, 0x13, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x03, 0x01, 0x99, 0x04, 0xFF, 0xC3, 0x03, 0x73, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x21, 0x61, 0x1B, 0x07, 0xAF, 0x63, 0x40, 0x28, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x22, 0x21, 0x1E, 0x06, 0xF0, 0x76, 0x08, 0x28, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x31, 0x22, 0x16, 0x05, 0x90, 0x7F, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x21, 0x61, 0x1D, 0x07, 0x82, 0x81, 0x10, 0x17, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x23, 0x21, 0x2D, 0x16, 0xC0, 0x70, 0x07, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x61, 0x21, 0x1B, 0x06, 0x64, 0x65, 0x18, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x61, 0x61, 0x0C, 0x18, 0x85, 0xA0, 0x79, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x23, 0x21, 0x87, 0x11, 0xF0, 0xA4, 0x00, 0xF7, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x97, 0xE1, 0x28, 0x07, 0xFF, 0xF3, 0x02, 0xF8, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x61, 0x10, 0x0C, 0x05, 0xF2, 0xC4, 0x40, 0xC8, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x01, 0x01, 0x56, 0x03, 0xB4, 0xB2, 0x23, 0x58, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x61, 0x41, 0x89, 0x03, 0xF1, 0xF4, 0xF0, 0x13, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x08, 0x21, 0x28, 0x00, 0xDF, 0xF8, 0xFF, 0xF8, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x23, 0x22, 0x00, 0x00, 0xA8, 0xF8, 0xF8, 0xF8, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
/*TODO*///      0x35, 0x18, 0x00, 0x00, 0xF7, 0xA9, 0xF7, 0x55, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
/*TODO*///      } ;
/*TODO*///
/*TODO*///      /* Definition of envelope mode */
/*TODO*///      public static final int SETTLE = 0;
/*TODO*///      public static final int ATTACK = 1;
/*TODO*///      public static final int DECAY = 2;
/*TODO*///      public static final int SUSHOLD = 3;
/*TODO*///      public static final int SUSTINE = 4;
/*TODO*///      public static final int RELEASE = 5;
/*TODO*///      public static final int FINISH = 6;
/*TODO*///
/*TODO*///
/*TODO*///      #ifdef USE_SPEC_ENV_SPEED
/*TODO*///      static double attacktime[16][4] = {
/*TODO*///        {0,0,0,0},
/*TODO*///        {1730.15, 1400.60, 1153.43, 988.66},
/*TODO*///        {865.08, 700.30, 576.72, 494.33},
/*TODO*///        {432.54, 350.15, 288.36, 247.16},
/*TODO*///        {216.27, 175.07, 144.18, 123.58},
/*TODO*///        {108.13, 87.54, 72.09, 61.79},
/*TODO*///        {54.07, 43.77, 36.04, 30.90},
/*TODO*///        {27.03, 21.88, 18.02, 15.45},
/*TODO*///        {13.52, 10.94, 9.01, 7.72},
/*TODO*///        {6.76, 5.47, 4.51, 3.86},
/*TODO*///        {3.38, 2.74, 2.25, 1.93},
/*TODO*///        {1.69, 1.37, 1.13, 0.97},
/*TODO*///        {0.84, 0.70, 0.60, 0.54},
/*TODO*///        {0.50, 0.42, 0.34, 0.30},
/*TODO*///        {0.28, 0.22, 0.18, 0.14},
/*TODO*///        {0.00, 0.00, 0.00, 0.00}
/*TODO*///      } ;
/*TODO*///
/*TODO*///      static double decaytime[16][4] = {
/*TODO*///        {0,0,0,0},
/*TODO*///        {20926.60,16807.20,14006.00,12028.60},
/*TODO*///        {10463.30,8403.58,7002.98,6014.32},
/*TODO*///        {5231.64,4201.79,3501.49,3007.16},
/*TODO*///        {2615.82,2100.89,1750.75,1503.58},
/*TODO*///        {1307.91,1050.45,875.37,751.79},
/*TODO*///        {653.95,525.22,437.69,375.90},
/*TODO*///        {326.98,262.61,218.84,187.95},
/*TODO*///        {163.49,131.31,109.42,93.97},
/*TODO*///        {81.74,65.65,54.71,46.99},
/*TODO*///        {40.87,32.83,27.36,23.49},
/*TODO*///        {20.44,16.41,13.68,11.75},
/*TODO*///        {10.22,8.21,6.84,5.87},
/*TODO*///        {5.11,4.10,3.42,2.94},
/*TODO*///        {2.55,2.05,1.71,1.47},
/*TODO*///        {1.27,1.27,1.27,1.27}
/*TODO*///      } ;
/*TODO*///      static UINT32 attacktable[16][4] ;
/*TODO*///      static UINT32 decaytable[16][4] ;
/*TODO*///      #endif
/*TODO*///
/*TODO*///      /* Phase incr table for Attack */
/*TODO*///      static UINT32 dphaseARTable[16][16] ;
/*TODO*///      /* Phase incr table for Decay and Release */
/*TODO*///      static UINT32 dphaseDRTable[16][16] ;
/*TODO*///
/*TODO*///      /* KSL + TL Table */
/*TODO*///      static UINT32 tllTable[16][8][1<<TL_BITS][4] ;
/*TODO*///      static INT32 rksTable[2][8][2] ;
/*TODO*///
/*TODO*///      /* Phase incr table for PG */
/*TODO*///      static UINT32 dphaseTable[512][8][16] ;
/*TODO*///
/*TODO*///
/*TODO*///      /***************************************************
/*TODO*///
/*TODO*///                        Create tables
/*TODO*///
/*TODO*///      ****************************************************/
/*TODO*///      INLINE INT32 Max(INT32 i,INT32 j){
/*TODO*///
/*TODO*///        if(i>j) return i ; else return j ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      INLINE INT32 Min(INT32 i,INT32 j){
/*TODO*///
/*TODO*///        if(i<j) return i ; else return j ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Table for AR to LogCurve. */
/*TODO*///      static void makeAdjustTable(void){
/*TODO*///
/*TODO*///        int i ;
/*TODO*///
/*TODO*///        AR_ADJUST_TABLE[0] = (1<<EG_BITS) ;
/*TODO*///        for ( i=1 ; i < 128 ; i++)
/*TODO*///          AR_ADJUST_TABLE[i] = (UINT32)((double)(1<<EG_BITS) - 1 - (1<<EG_BITS) * log(i) / log(128)) ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///
/*TODO*///      /* Table for dB(0 -- (1<<DB_BITS)) to Liner(0 -- DB2LIN_AMP_WIDTH) */
/*TODO*///      static void makeDB2LinTable(void){
/*TODO*///
/*TODO*///        int i ;
/*TODO*///
/*TODO*///        for( i=0 ; i < DB_MUTE ; i++)
/*TODO*///          DB2LIN_TABLE[i] = (INT32)((double)(1<<DB2LIN_AMP_BITS) * pow(10,-(double)i*DB_STEP/20)) ;
/*TODO*///
/*TODO*///        DB2LIN_TABLE[DB_MUTE] = 0 ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Liner(+0.0 - +1.0) to dB((1<<DB_BITS) - 1 -- 0) */
/*TODO*///      static INT32 lin2db(double d){
/*TODO*///
/*TODO*///        if(d == 0) return (1<<DB_BITS) - 1 ;
/*TODO*///        else return (INT32)(-((INT32)((double)20*log10(d)/SIN_STEP)) * (SIN_STEP/DB_STEP)) ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /*
/*TODO*///         Sin Table
/*TODO*///         Plus(minus) 1.0dB to positive(negative) area to distinguish +0 and -0dB)
/*TODO*///       */
/*TODO*///      static void makeSinTable(void){
/*TODO*///
/*TODO*///        int i ;
/*TODO*///
/*TODO*///        for( i = 0 ; i < PG_WIDTH/4 ; i++ )
/*TODO*///          fullsintable[i] = lin2db(sin(2.0*PI*i/PG_WIDTH)) + 1 ;
/*TODO*///
/*TODO*///        for( i = 0 ; i < PG_WIDTH/4 ; i++ )
/*TODO*///          fullsintable[PG_WIDTH/2 - 1 - i] = fullsintable[i] ;
/*TODO*///
/*TODO*///        for( i = 0 ; i < PG_WIDTH/2 ; i++ )
/*TODO*///          fullsintable[PG_WIDTH/2+i] = -fullsintable[i] ;
/*TODO*///
/*TODO*///        for( i = 0 ; i < PG_WIDTH/2 ; i++ )
/*TODO*///          halfsintable[i] = fullsintable[i] ;
/*TODO*///
/*TODO*///        for( i = PG_WIDTH/2 ; i< PG_WIDTH ; i++ )
/*TODO*///          halfsintable[i] = fullsintable[0] ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Table for Pitch Modulator */
/*TODO*///      static void makePmTable(void){
/*TODO*///
/*TODO*///        int i ;
/*TODO*///
/*TODO*///        for(i = 0 ; i < PM_PG_WIDTH ; i++ )
/*TODO*///          pmtable[i] = (INT32)((double)PM_AMP * pow(2,(double)PM_DEPTH*sin(2.0*PI*i/PM_PG_WIDTH)/1200)) ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Table for Amp Modulator */
/*TODO*///      static void makeAmTable(void){
/*TODO*///
/*TODO*///        int i ;
/*TODO*///
/*TODO*///        for(i = 0 ; i < AM_PG_WIDTH ; i++ )
/*TODO*///          amtable[i] = (INT32)((double)AM_DEPTH/2/DB_STEP * (1.0 + sin(2.0*PI*i/PM_PG_WIDTH))) ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Phase increment counter table */
/*TODO*///      static void makeDphaseTable(void){
/*TODO*///
/*TODO*///        UINT32 fnum, block , ML ;
/*TODO*///        UINT32 mltable[16]={ 1,1*2,2*2,3*2,4*2,5*2,6*2,7*2,8*2,9*2,10*2,10*2,12*2,12*2,15*2,15*2 } ;
/*TODO*///
/*TODO*///        for(fnum=0; fnum<512; fnum++)
/*TODO*///          for(block=0; block<8; block++)
/*TODO*///            for(ML=0; ML<16; ML++)
/*TODO*///              dphaseTable[fnum][block][ML] = rate_adjust(((fnum * mltable[ML]) << block ) >> (20 - DP_BITS)) ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      static void makeTllTalbe(void){
/*TODO*///
/*TODO*///      #if DB_BITS == 7
/*TODO*///      #define dB2(x) (UINT32)(((UINT32)(x/0.375)) * 0.375 * 2)
/*TODO*///      #else
/*TODO*///      #define dB2(x) (UINT32)((x)*2)
/*TODO*///      #endif
/*TODO*///
/*TODO*///              static UINT32 kltable[16] = {
/*TODO*///          dB2( 0.000),dB2( 9.000),dB2(12.000),dB2(13.875),dB2(15.000),dB2(16.125),dB2(16.875),dB2(17.625),
/*TODO*///          dB2(18.000),dB2(18.750),dB2(19.125),dB2(19.500),dB2(19.875),dB2(20.250),dB2(20.625),dB2(21.000)
/*TODO*///              } ;
/*TODO*///
/*TODO*///        INT32 tmp ;
/*TODO*///        int fnum, block ,TL , KL ;
/*TODO*///
/*TODO*///        for(fnum=0; fnum<16; fnum++)
/*TODO*///          for(block=0; block<8; block++)
/*TODO*///            for(TL=0; TL<64; TL++)
/*TODO*///              for(KL=0; KL<4; KL++){
/*TODO*///
/*TODO*///                if(KL==0){
/*TODO*///                  tllTable[fnum][block][TL][KL] = EXPAND_BITS(TL,TL_BITS,DB_BITS) ;
/*TODO*///                }else{
/*TODO*///                      tmp = kltable[fnum] - dB2(3.000) * (7 - block) ;
/*TODO*///                  if(tmp <= 0)
/*TODO*///                    tllTable[fnum][block][TL][KL] = EXPAND_BITS(TL,TL_BITS,DB_BITS) ;
/*TODO*///                  else
/*TODO*///                    tllTable[fnum][block][TL][KL] = (UINT32)((tmp >> ( 3 - KL ))/DB_STEP) + EXPAND_BITS(TL,TL_BITS,DB_BITS) ;
/*TODO*///                }
/*TODO*///
/*TODO*///             }
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Rate Table for Attack */
/*TODO*///      static void makeDphaseARTable(void){
/*TODO*///
/*TODO*///        int AR,Rks,RM,RL ;
/*TODO*///
/*TODO*///      #ifdef USE_SPEC_ENV_SPEED
/*TODO*///        for(RM=0; RM<16; RM++)
/*TODO*///          for(RL=0; RL<4 ; RL++) {
/*TODO*///            if(RM == 0)
/*TODO*///              attacktable[RM][RL] = 0 ;
/*TODO*///            else if(RM == 15)
/*TODO*///              attacktable[RM][RL] = EG_DP_WIDTH ;
/*TODO*///            else
/*TODO*///              attacktable[RM][RL] = (UINT32)((double)(1<<EG_DP_BITS)/(attacktime[RM][RL]*clk/72/1000));
/*TODO*///
/*TODO*///          }
/*TODO*///      #endif
/*TODO*///
/*TODO*///        for(AR=0; AR<16; AR++)
/*TODO*///          for(Rks=0; Rks<16; Rks++){
/*TODO*///            RM = AR + (Rks>>2) ;
/*TODO*///            if(RM>15) RM = 15 ;
/*TODO*///            RL = Rks&3 ;
/*TODO*///            switch(AR){
/*TODO*///             case 0:
/*TODO*///              dphaseARTable[AR][Rks] = 0 ;
/*TODO*///              break ;
/*TODO*///            case 15:
/*TODO*///              dphaseARTable[AR][Rks] = EG_DP_WIDTH ;
/*TODO*///              break ;
/*TODO*///            default:
/*TODO*///      #ifdef USE_SPEC_ENV_SPEED
/*TODO*///              dphaseARTable[AR][Rks] = rate_adjust(attacktable[RM][RL]);
/*TODO*///      #else
/*TODO*///              dphaseARTable[AR][Rks] = rate_adjust(( 3 * (RL + 4) << (RM + 1))) ;
/*TODO*///      #endif
/*TODO*///              break ;
/*TODO*///            }
/*TODO*///
/*TODO*///          }
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      static void makeDphaseDRTable(void){
/*TODO*///
/*TODO*///        int DR,Rks,RM,RL ;
/*TODO*///
/*TODO*///      #ifdef USE_SPEC_ENV_SPEED
/*TODO*///        for(RM=0; RM<16; RM++)
/*TODO*///          for(RL=0; RL<4 ; RL++)
/*TODO*///            if(RM == 0)
/*TODO*///              decaytable[RM][RL] = 0 ;
/*TODO*///            else
/*TODO*///              decaytable[RM][RL] = (UINT32)((double)(1<<EG_DP_BITS)/(decaytime[RM][RL]*clk/72/1000)) ;
/*TODO*///      #endif
/*TODO*///
/*TODO*///        for(DR=0; DR<16; DR++)
/*TODO*///          for(Rks=0; Rks<16; Rks++){
/*TODO*///            RM = DR + (Rks>>2) ;
/*TODO*///            RL = Rks&3 ;
/*TODO*///            if(RM>15) RM = 15 ;
/*TODO*///            switch(DR){
/*TODO*///            case 0:
/*TODO*///              dphaseDRTable[DR][Rks] = 0 ;
/*TODO*///              break ;
/*TODO*///            default:
/*TODO*///      #ifdef USE_SPEC_ENV_SPEED
/*TODO*///              dphaseDRTable[DR][Rks] = rate_adjust(decaytable[RM][RL]) ;
/*TODO*///      #else
/*TODO*///              dphaseDRTable[DR][Rks] = rate_adjust((RL + 4) << (RM - 1));
/*TODO*///      #endif
/*TODO*///              break ;
/*TODO*///            }
/*TODO*///
/*TODO*///          }
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      static void makeRksTable(void){
/*TODO*///
/*TODO*///        int fnum8, block, KR ;
/*TODO*///
/*TODO*///        for(fnum8 = 0 ; fnum8 < 2 ; fnum8++)
/*TODO*///          for(block = 0 ; block < 8 ; block++)
/*TODO*///            for(KR = 0; KR < 2 ; KR++){
/*TODO*///              if(KR!=0)
/*TODO*///                rksTable[fnum8][block][KR] = ( block << 1 ) + fnum8 ;
/*TODO*///              else
/*TODO*///                rksTable[fnum8][block][KR] = block >> 1 ;
/*TODO*///            }
/*TODO*///
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///
/*TODO*///      void dump2patch(unsigned char *dump, OPLL_PATCH *patch){
/*TODO*///
/*TODO*///        patch[0].AM = (dump[0]>>7)&1 ;
/*TODO*///        patch[1].AM = (dump[1]>>7)&1 ;
/*TODO*///        patch[0].PM = (dump[0]>>6)&1 ;
/*TODO*///        patch[1].PM = (dump[1]>>6)&1 ;
/*TODO*///        patch[0].EG = (dump[0]>>5)&1 ;
/*TODO*///        patch[1].EG = (dump[1]>>5)&1 ;
/*TODO*///        patch[0].KR = (dump[0]>>4)&1 ;
/*TODO*///        patch[1].KR = (dump[1]>>4)&1 ;
/*TODO*///        patch[0].ML = (dump[0])&15 ;
/*TODO*///        patch[1].ML = (dump[1])&15 ;
/*TODO*///        patch[0].KL = (dump[2]>>6)&3 ;
/*TODO*///        patch[1].KL = (dump[3]>>6)&3 ;
/*TODO*///        patch[0].TL = (dump[2])&63 ;
/*TODO*///        patch[0].FB = (dump[3])&7 ;
/*TODO*///        patch[0].WF = (dump[3]>>3)&1 ;
/*TODO*///        patch[1].WF = (dump[3]>>4)&1 ;
/*TODO*///        patch[0].AR = (dump[4]>>4)&15 ;
/*TODO*///        patch[1].AR = (dump[5]>>4)&15 ;
/*TODO*///        patch[0].DR = (dump[4])&15 ;
/*TODO*///        patch[1].DR = (dump[5])&15 ;
/*TODO*///        patch[0].SL = (dump[6]>>4)&15 ;
/*TODO*///        patch[1].SL = (dump[7]>>4)&15 ;
/*TODO*///        patch[0].RR = (dump[6])&15 ;
/*TODO*///        patch[1].RR = (dump[7])&15 ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      static void makeDefaultPatch(void){
/*TODO*///
/*TODO*///        int i ;
/*TODO*///
/*TODO*///        for(i=1;i<19;i++)
/*TODO*///          dump2patch(default_inst+(i*16),&default_patch[i*2]) ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /************************************************************
/*TODO*///
/*TODO*///                            Calc Parameters
/*TODO*///
/*TODO*///      ************************************************************/
/*TODO*///
/*TODO*///      INLINE UINT32 calc_eg_dphase(OPLL_SLOT *slot){
/*TODO*///
/*TODO*///        switch(slot.eg_mode){
/*TODO*///
/*TODO*///        case ATTACK:
/*TODO*///          return dphaseARTable[slot.patch.AR][slot.rks] ;
/*TODO*///
/*TODO*///        case DECAY:
/*TODO*///          return dphaseDRTable[slot.patch.DR][slot.rks] ;
/*TODO*///
/*TODO*///        case SUSHOLD:
/*TODO*///          return 0 ;
/*TODO*///
/*TODO*///        case SUSTINE:
/*TODO*///          return dphaseDRTable[slot.patch.RR][slot.rks] ;
/*TODO*///
/*TODO*///        case RELEASE:
/*TODO*///          if(slot.sustine)
/*TODO*///            return dphaseDRTable[5][slot.rks] ;
/*TODO*///          else if(slot.patch.EG)
/*TODO*///            return dphaseDRTable[slot.patch.RR][slot.rks] ;
/*TODO*///          else
/*TODO*///            return dphaseDRTable[7][slot.rks] ;
/*TODO*///
/*TODO*///        case FINISH:
/*TODO*///          return 0 ;
/*TODO*///
/*TODO*///        default:
/*TODO*///          return 0 ;
/*TODO*///
/*TODO*///        }
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /*************************************************************
/*TODO*///
/*TODO*///                          OPLL internal interfaces
/*TODO*///
/*TODO*///      *************************************************************/
/*TODO*///      #define SLOT_BD1 12
/*TODO*///      #define SLOT_BD2 13
/*TODO*///      #define SLOT_HH 14
/*TODO*///      #define SLOT_SD 15
/*TODO*///      #define SLOT_TOM 16
/*TODO*///      #define SLOT_CYM 17
/*TODO*///
/*TODO*///      #define UPDATE_PG 1
/*TODO*///      #define UPDATE_EG 2
/*TODO*///      #define UPDATE_TLL 4
/*TODO*///      #define UPDATE_RKS 8
/*TODO*///      #define UPDATE_WF 16
/*TODO*///      #define UPDATE_ALL (UPDATE_PG|UPDATE_EG|UPDATE_TLL|UPDATE_RKS|UPDATE_WF)
/*TODO*///
/*TODO*///      /* Force Refresh (When external program changes some parameters). */
/*TODO*///      void OPLL_forceRefresh(OPLL *opll){
/*TODO*///
/*TODO*///        int i ;
/*TODO*///
/*TODO*///        if(opll==NULL) return ;
/*TODO*///        for(i=0; i<18 ;i++)
/*TODO*///          opll.slot[i].update |= UPDATE_ALL ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Refresh slot parameters */
/*TODO*///      INLINE void refresh(OPLL_SLOT *slot){
/*TODO*///
/*TODO*///        if(slot.update&UPDATE_PG)
/*TODO*///          slot.dphase = dphaseTable[slot.fnum][slot.block][slot.patch.ML] ;
/*TODO*///
/*TODO*///        if(slot.update&UPDATE_EG)
/*TODO*///          slot.eg_dphase = calc_eg_dphase(slot) ;
/*TODO*///
/*TODO*///        if(slot.update&UPDATE_TLL){
/*TODO*///          if(slot.type == 0)
/*TODO*///            slot.tll = tllTable[slot.fnum>>5][slot.block][slot.patch.TL][slot.patch.KL] ;
/*TODO*///          else
/*TODO*///            slot.tll = tllTable[slot.fnum>>5][slot.block][slot.volume][slot.patch.KL] ;
/*TODO*///        }
/*TODO*///
/*TODO*///        if(slot.update&UPDATE_RKS)
/*TODO*///          slot.rks = rksTable[slot.fnum>>8][slot.block][slot.patch.KR] ;
/*TODO*///
/*TODO*///        if(slot.update&UPDATE_WF)
/*TODO*///          slot.sintbl = waveform[slot.patch.WF] ;
/*TODO*///
/*TODO*///        slot.update = 0 ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Slot key on  */
/*TODO*///      INLINE void slotOn(OPLL_SLOT *slot){
/*TODO*///
/*TODO*///        slot.eg_mode = ATTACK ;
/*TODO*///        slot.phase = 0 ;
/*TODO*///        slot.eg_phase = 0 ;
/*TODO*///        slot.update |= UPDATE_EG ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Slot key off */
/*TODO*///      INLINE void slotOff(OPLL_SLOT *slot){
/*TODO*///
/*TODO*///        if(slot.eg_mode == ATTACK)
/*TODO*///          slot.eg_phase = EXPAND_BITS(AR_ADJUST_TABLE[HIGHBITS(slot.eg_phase, EG_DP_BITS - EG_BITS)],EG_BITS,EG_DP_BITS) ;
/*TODO*///
/*TODO*///        slot.eg_mode = RELEASE ;
/*TODO*///        slot.update |= UPDATE_EG ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Channel key on */
/*TODO*///      INLINE void keyOn(OPLL *opll, int i){
/*TODO*///
/*TODO*///        if(!opll.slot_on_flag[i*2]) slotOn(opll.MOD(i)) ;
/*TODO*///        if(!opll.slot_on_flag[i*2+1]) slotOn(opll.CAR(i)) ;
/*TODO*///        opll.ch[i].key_status = 1 ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Channel key off */
/*TODO*///      INLINE void keyOff(OPLL *opll, int i){
/*TODO*///
/*TODO*///        if(opll.slot_on_flag[i*2+1]) slotOff(opll.CAR(i)) ;
/*TODO*///        opll.ch[i].key_status = 0 ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Drum key on */
/*TODO*///      #ifdef OPLL_ENABLE_DEBUG
/*TODO*///      INLINE void keyOn_BD(OPLL *opll){
/*TODO*///        if(!opll.slot_on_flag[SLOT_BD1]){
/*TODO*///          slotOn(opll.MOD(6)) ;
/*TODO*///        }
/*TODO*///        if(!opll.slot_on_flag[SLOT_BD2]){
/*TODO*///          slotOn(opll.CAR(6)) ;
/*TODO*///          opll.debug_rythm_flag |= 16 ;
/*TODO*///        }
/*TODO*///      }
/*TODO*///      INLINE void keyOn_SD(OPLL *opll){
/*TODO*///        if(!opll.slot_on_flag[SLOT_SD]){
/*TODO*///          slotOn(opll.CAR(7)) ;
/*TODO*///          opll.debug_rythm_flag |= 8 ;
/*TODO*///        }
/*TODO*///      }
/*TODO*///      INLINE void keyOn_TOM(OPLL *opll){
/*TODO*///        if(!opll.slot_on_flag[SLOT_TOM]){
/*TODO*///          slotOn(opll.MOD(8)) ;
/*TODO*///          opll.debug_rythm_flag |= 4 ;
/*TODO*///        }
/*TODO*///      }
/*TODO*///      INLINE void keyOn_HH(OPLL *opll){
/*TODO*///        if(!opll.slot_on_flag[SLOT_HH]){
/*TODO*///          slotOn(opll.MOD(7)) ;
/*TODO*///          opll.debug_rythm_flag |= 2 ;
/*TODO*///        }
/*TODO*///      }
/*TODO*///      INLINE void keyOn_CYM(OPLL *opll){
/*TODO*///        if(!opll.slot_on_flag[SLOT_CYM]){
/*TODO*///          slotOn(opll.CAR(8)) ;
/*TODO*///          opll.debug_rythm_flag |= 1 ;
/*TODO*///        }
/*TODO*///      }
/*TODO*///      #else
/*TODO*///      INLINE void keyOn_BD(OPLL *opll){ keyOn(opll,6) ; }
/*TODO*///      INLINE void keyOn_SD(OPLL *opll){ if(!opll.slot_on_flag[SLOT_SD]) slotOn(opll.CAR(7)) ; }
/*TODO*///      INLINE void keyOn_TOM(OPLL *opll){ if(!opll.slot_on_flag[SLOT_TOM]) slotOn(opll.MOD(8)) ; }
/*TODO*///      INLINE void keyOn_HH(OPLL *opll){ if(!opll.slot_on_flag[SLOT_HH]) slotOn(opll.MOD(7)) ; }
/*TODO*///      INLINE void keyOn_CYM(OPLL *opll){ if(!opll.slot_on_flag[SLOT_CYM]) slotOn(opll.CAR(8)) ; }
/*TODO*///      #endif
/*TODO*///
/*TODO*///      /* Drum key off */
/*TODO*///      INLINE void keyOff_BD(OPLL *opll){ keyOff(opll,6) ; }
/*TODO*///      INLINE void keyOff_SD(OPLL *opll){ if(opll.slot_on_flag[SLOT_SD]) slotOff(opll.CAR(7)) ; }
/*TODO*///      INLINE void keyOff_TOM(OPLL *opll){ if(opll.slot_on_flag[SLOT_TOM]) slotOff(opll.MOD(8)) ; }
/*TODO*///      INLINE void keyOff_HH(OPLL *opll){ if(opll.slot_on_flag[SLOT_HH]) slotOff(opll.MOD(7)) ; }
/*TODO*///      INLINE void keyOff_CYM(OPLL *opll){ if(opll.slot_on_flag[SLOT_CYM]) slotOff(opll.CAR(8)) ; }
/*TODO*///
/*TODO*///      /* Change a voice */
/*TODO*///      INLINE void setPatch(OPLL *opll, int i, int num){
/*TODO*///
/*TODO*///      #ifdef OPLL_ENABLE_DEBUG
/*TODO*///        if(opll.ch[i].key_status) {
/*TODO*///          if(opll.ch[i].debug_keyonpatch[0] != opll.ch[i].patch_number + 1){
/*TODO*///            opll.ch[i].debug_keyonpatch[2] = opll.ch[i].debug_keyonpatch[1] ;
/*TODO*///            opll.ch[i].debug_keyonpatch[1] = opll.ch[i].debug_keyonpatch[0] ;
/*TODO*///            opll.ch[i].debug_keyonpatch[0] = opll.ch[i].patch_number + 1 ;
/*TODO*///          }
/*TODO*///        }else{
/*TODO*///          opll.ch[i].debug_keyonpatch[2] = 0 ;
/*TODO*///          opll.ch[i].debug_keyonpatch[1] = 0 ;
/*TODO*///          opll.ch[i].debug_keyonpatch[0] = 0 ;
/*TODO*///        }
/*TODO*///      #endif
/*TODO*///
/*TODO*///        opll.ch[i].patch_number = num ;
/*TODO*///        opll.MOD(i).patch = &(opll.patch[num][0]) ;
/*TODO*///        opll.CAR(i).patch = &(opll.patch[num][1]) ;
/*TODO*///
/*TODO*///        opll.MOD(i).update |= UPDATE_ALL ;
/*TODO*///        opll.CAR(i).update |= UPDATE_ALL ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Change a rythm voice */
/*TODO*///      INLINE void setSlotPatch(OPLL_SLOT *slot, OPLL_PATCH *patch){
/*TODO*///
/*TODO*///        slot.patch = patch ;
/*TODO*///        slot.update |= UPDATE_ALL ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Set sustine parameter */
/*TODO*///      INLINE void setSustine(OPLL *opll, int c, int sustine){
/*TODO*///
/*TODO*///        opll.CAR(c).sustine = sustine ;
/*TODO*///        opll.MOD(c).sustine = sustine ;
/*TODO*///        opll.CAR(c).update |= UPDATE_EG ;
/*TODO*///        opll.MOD(c).update |= UPDATE_EG ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /*
/*TODO*///        Volume setting
/*TODO*///        volume : 6bit ( Volume register << 2 )
/*TODO*///      */
/*TODO*///      INLINE void setVolume(OPLL *opll, int c, int volume){
/*TODO*///
/*TODO*///        opll.CAR(c).volume = volume ;
/*TODO*///        opll.CAR(c).update |= UPDATE_TLL ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      INLINE void setSlotVolume(OPLL_SLOT *slot, int volume){
/*TODO*///
/*TODO*///        slot.volume = volume ;
/*TODO*///        slot.update |= UPDATE_TLL ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Volume setting for Drum */
/*TODO*///      INLINE void setVolume_BD(OPLL *opll, int volume){ setVolume(opll,6,volume) ; }
/*TODO*///      INLINE void setVolume_HH(OPLL *opll, int volume){ setSlotVolume(opll.MOD(7),volume) ; }
/*TODO*///      INLINE void setVolume_SD(OPLL *opll, int volume){ setSlotVolume(opll.CAR(7),volume) ; }
/*TODO*///      INLINE void setVolume_TOM(OPLL *opll, int volume){ setSlotVolume(opll.MOD(8),volume) ; }
/*TODO*///      INLINE void setVolume_CYM(OPLL *opll, int volume){ setSlotVolume(opll.CAR(8),volume) ; }
/*TODO*///
/*TODO*///      /* Set F-Number ( fnum : 9bit ) */
/*TODO*///      INLINE void setFnumber(OPLL *opll, int c, int fnum){
/*TODO*///
/*TODO*///        opll.CAR(c).fnum = fnum ;
/*TODO*///        opll.MOD(c).fnum = fnum ;
/*TODO*///        opll.CAR(c).update |= UPDATE_PG | UPDATE_TLL | UPDATE_RKS ;
/*TODO*///        opll.MOD(c).update |= UPDATE_PG | UPDATE_TLL | UPDATE_RKS ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Set Block data (block : 3bit ) */
/*TODO*///      INLINE void setBlock(OPLL *opll, int c, int block){
/*TODO*///
/*TODO*///        opll.CAR(c).block = block ;
/*TODO*///        opll.MOD(c).block = block ;
/*TODO*///        opll.CAR(c).update |= UPDATE_PG | UPDATE_TLL | UPDATE_RKS ;
/*TODO*///        opll.MOD(c).update |= UPDATE_PG | UPDATE_TLL | UPDATE_RKS ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Change Rythm Mode */
/*TODO*///      INLINE void setRythmMode(OPLL *opll, int mode){
/*TODO*///
/*TODO*///        opll.rythm_mode = mode ;
/*TODO*///
/*TODO*///        if(mode){
/*TODO*///
/*TODO*///          opll.ch[6].patch_number = 16 ;
/*TODO*///          opll.ch[7].patch_number = 17 ;
/*TODO*///          opll.ch[8].patch_number = 18 ;
/*TODO*///          setSlotPatch(opll.MOD(6), &(opll.patch[16][0])) ;
/*TODO*///          setSlotPatch(opll.CAR(6), &(opll.patch[16][1])) ;
/*TODO*///          setSlotPatch(opll.MOD(7), &(opll.patch[17][0])) ;
/*TODO*///          setSlotPatch(opll.CAR(7), &(opll.patch[17][1])) ;
/*TODO*///          opll.MOD(7).type = 1 ;
/*TODO*///          setSlotPatch(opll.MOD(8), &(opll.patch[18][0])) ;
/*TODO*///          setSlotPatch(opll.CAR(8), &(opll.patch[18][1])) ;
/*TODO*///          opll.MOD(8).type = 1 ;
/*TODO*///
/*TODO*///        }else{
/*TODO*///
/*TODO*///          setPatch(opll, 6, opll.reg[0x36]>>4) ;
/*TODO*///          setPatch(opll, 7, opll.reg[0x37]>>4) ;
/*TODO*///          opll.MOD(7).type = 0 ;
/*TODO*///          setPatch(opll, 8, opll.reg[0x38]>>4) ;
/*TODO*///          opll.MOD(8).type = 0 ;
/*TODO*///
/*TODO*///        }
/*TODO*///
/*TODO*///        slotOff(opll.MOD(6));
/*TODO*///        slotOff(opll.MOD(7));
/*TODO*///        slotOff(opll.MOD(8));
/*TODO*///        slotOff(opll.CAR(6));
/*TODO*///        slotOff(opll.CAR(7));
/*TODO*///        slotOff(opll.CAR(8));
/*TODO*///        opll.MOD(6).update |= UPDATE_ALL ;
/*TODO*///        opll.MOD(7).update |= UPDATE_ALL ;
/*TODO*///        opll.MOD(8).update |= UPDATE_ALL ;
/*TODO*///        opll.CAR(6).update |= UPDATE_ALL ;
/*TODO*///        opll.CAR(7).update |= UPDATE_ALL ;
/*TODO*///        opll.CAR(8).update |= UPDATE_ALL ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      void OPLL_copyPatch(OPLL *opll, int num, OPLL_PATCH *patch){
/*TODO*///
/*TODO*///        memcpy(opll.patch[num],patch,sizeof(OPLL_PATCH)*2) ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /***********************************************************
/*TODO*///
/*TODO*///                            Initializing
/*TODO*///
/*TODO*///      ***********************************************************/
/*TODO*///
/*TODO*///      static void OPLL_SLOT_reset(OPLL_SLOT *slot){
/*TODO*///
/*TODO*///        slot.sintbl = waveform[0] ;
/*TODO*///        slot.phase = 0 ;
/*TODO*///        slot.dphase = 0 ;
/*TODO*///        slot.output[0] = 0 ;
/*TODO*///        slot.output[1] = 0 ;
/*TODO*///        slot.eg_mode = SETTLE ;
/*TODO*///        slot.eg_phase = EG_DP_WIDTH ;
/*TODO*///        slot.eg_dphase = 0 ;
/*TODO*///        slot.rks = 0 ;
/*TODO*///        slot.tll = 0 ;
/*TODO*///        slot.sustine = 0 ;
/*TODO*///        slot.patch = &null_patch ;
/*TODO*///        slot.fnum = 0 ;
/*TODO*///        slot.block = 0 ;
/*TODO*///        slot.volume = 0 ;
/*TODO*///        slot.pgout = 0 ;
/*TODO*///        slot.egout = 0 ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      static OPLL_SLOT *OPLL_SLOT_new(void){
/*TODO*///
/*TODO*///        OPLL_SLOT *slot ;
/*TODO*///
/*TODO*///        slot = malloc(sizeof(OPLL_SLOT)) ;
/*TODO*///        if(slot == NULL) return NULL ;
/*TODO*///
/*TODO*///        return slot ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      static void OPLL_SLOT_delete(OPLL_SLOT *slot){
/*TODO*///
/*TODO*///        free(slot) ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      static void OPLL_CH_reset(OPLL_CH *ch){
/*TODO*///
/*TODO*///        if(ch.mod!=NULL) OPLL_SLOT_reset(ch.mod) ;
/*TODO*///        if(ch.car!=NULL) OPLL_SLOT_reset(ch.car) ;
/*TODO*///        ch.key_status = 0 ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      static OPLL_CH *OPLL_CH_new(void){
/*TODO*///
/*TODO*///        OPLL_CH *ch ;
/*TODO*///        OPLL_SLOT *mod, *car ;
/*TODO*///
/*TODO*///        mod = OPLL_SLOT_new() ;
/*TODO*///        if(mod == NULL) return NULL ;
/*TODO*///
/*TODO*///        car = OPLL_SLOT_new() ;
/*TODO*///        if(car == NULL){
/*TODO*///          OPLL_SLOT_delete(mod) ;
/*TODO*///          return NULL ;
/*TODO*///        }
/*TODO*///
/*TODO*///        ch = malloc(sizeof(OPLL_CH)) ;
/*TODO*///        if(ch == NULL){
/*TODO*///          OPLL_SLOT_delete(mod) ;
/*TODO*///          OPLL_SLOT_delete(car) ;
/*TODO*///          return NULL ;
/*TODO*///        }
/*TODO*///
/*TODO*///        mod.type = 0 ;
/*TODO*///        car.type = 1 ;
/*TODO*///        ch.mod = mod ;
/*TODO*///        ch.car = car ;
/*TODO*///
/*TODO*///        return ch ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///
/*TODO*///      static void OPLL_CH_delete(OPLL_CH *ch){
/*TODO*///
/*TODO*///        OPLL_SLOT_delete(ch.mod) ;
/*TODO*///        OPLL_SLOT_delete(ch.car) ;
/*TODO*///        free(ch) ;
/*TODO*///
/*TODO*///      }
/*TODO*///
    public static OPLL OPLL_new(){

        OPLL opll = null;
/*TODO*///        OPLL_CH *ch[9] ;
/*TODO*///        OPLL_PATCH *patch[19] ;
/*TODO*///        int i, j ;
/*TODO*///
/*TODO*///        for( i = 0 ; i < 19 ; i++ ){
/*TODO*///          patch[i] = calloc(sizeof(OPLL_PATCH),2) ;
/*TODO*///          if(patch[i] == NULL){
/*TODO*///            for ( j = i ; i > 0 ; i++ ) free(patch[j-1]) ;
/*TODO*///            return NULL ;
/*TODO*///          }
/*TODO*///        }
/*TODO*///
/*TODO*///        for( i = 0 ; i < 9 ; i++ ){
/*TODO*///          ch[i] = OPLL_CH_new() ;
/*TODO*///          if(ch[i]==NULL){
/*TODO*///            for ( j = i ; i > 0 ; i++ ) OPLL_CH_delete(ch[j-1]) ;
/*TODO*///            for ( j = 0 ; j < 19 ; j++ ) free(patch[j]) ;
/*TODO*///            return NULL ;
/*TODO*///          }
/*TODO*///        }
/*TODO*///
/*TODO*///
/*TODO*///        opll = malloc(sizeof(OPLL)) ;
/*TODO*///        if(opll == NULL) return NULL ;
/*TODO*///
/*TODO*///        for ( i = 0 ; i < 19 ; i++ ) opll.patch[i] = patch[i] ;
/*TODO*///        for ( i = 0 ; i < 9 ; i++ ) opll.ch[i] = ch[i] ;
/*TODO*///
/*TODO*///
/*TODO*///        /* Slot aliases for sequential access. */
/*TODO*///        for ( i = 0 ; i <9 ; i++){
/*TODO*///            opll.ch[i].mod.plfo_am = &(opll.lfo_am) ;
/*TODO*///            opll.ch[i].mod.plfo_pm = &(opll.lfo_pm) ;
/*TODO*///            opll.slot[i*2+0] = opll.ch[i].mod ;
/*TODO*///            opll.ch[i].car.plfo_am = &(opll.lfo_am) ;
/*TODO*///            opll.ch[i].car.plfo_pm = &(opll.lfo_pm) ;
/*TODO*///            opll.slot[i*2+1] = opll.ch[i].car ;
/*TODO*///        }
/*TODO*///
/*TODO*///        for( i = 0 ; i < 10 ; i++ ) opll.mask[i] = 0 ;
/*TODO*///
/*TODO*///        OPLL_reset(opll) ;
/*TODO*///
/*TODO*///        opll.masterVolume = 64 ;
/*TODO*///        opll.rythmVolume = 64 ;
/*TODO*///
        return opll ;

      }

      public static void OPLL_delete(OPLL opll){
/*TODO*///
/*TODO*///        int i ;
/*TODO*///
/*TODO*///        for ( i = 0 ; i < 9 ; i++ )
/*TODO*///          OPLL_CH_delete(opll.ch[i]) ;
/*TODO*///
/*TODO*///        for ( i = 0 ; i < 19 ; i++ )
/*TODO*///          free(opll.patch[i]) ;
/*TODO*///
/*TODO*///      #ifdef OPLL_LOGFILE
/*TODO*///
/*TODO*///        fclose(opll.logfile) ;
/*TODO*///
/*TODO*///      #endif
/*TODO*///
/*TODO*///        free(opll) ;

      }

      /* Reset patch datas by system default. */
      public static void OPLL_reset_patch(OPLL opll){

/*TODO*///        int i ;
/*TODO*///
/*TODO*///        for ( i = 0 ; i < 19 ; i++ )
/*TODO*///          OPLL_copyPatch(opll, i, &default_patch[i*2]) ;
/*TODO*///
      }

      /* Reset whole of OPLL except patch datas. */
      public static void OPLL_reset(OPLL opll){

/*TODO*///        int i ;
/*TODO*///
/*TODO*///        if (opll == 0) return ;
/*TODO*///
/*TODO*///        opll.output[0] = 0 ;
/*TODO*///        opll.output[1] = 0 ;
/*TODO*///
/*TODO*///        opll.rythm_mode = 0 ;
/*TODO*///        opll.pm_phase = 0 ;
/*TODO*///        opll.pm_dphase = (UINT32)rate_adjust(PM_SPEED * PM_DP_WIDTH / (clk/72) ) ;
/*TODO*///        opll.am_phase = 0 ;
/*TODO*///        opll.am_dphase = (UINT32)rate_adjust(AM_SPEED * AM_DP_WIDTH / (clk/72) ) ;
/*TODO*///
/*TODO*///        for ( i = 0 ; i < 0x38 ; i++ ) opll.reg[i] = 0 ;
/*TODO*///
/*TODO*///        for ( i = 0 ; i < 9 ; i++ ){
/*TODO*///            OPLL_CH_reset(opll.ch[i]) ;
/*TODO*///            setPatch(opll,i,0) ;
/*TODO*///        }
/*TODO*///
/*TODO*///      #ifdef OPLL_LOGFILE
/*TODO*///
/*TODO*///        opll.logfile = fopen("opll.log","w") ;
/*TODO*///
/*TODO*///      #endif

      }

      public static void OPLL_init(int c, int r){
/*TODO*///
/*TODO*///
/*TODO*///        clk = c ;
/*TODO*///        rate = r ;
/*TODO*///        makePmTable() ;
/*TODO*///        makeAmTable() ;
/*TODO*///        makeDB2LinTable() ;
/*TODO*///        makeAdjustTable() ;
/*TODO*///        makeDphaseTable() ;
/*TODO*///        makeTllTalbe() ;
/*TODO*///        makeSinTable() ;
/*TODO*///        makeDphaseARTable() ;
/*TODO*///        makeDphaseDRTable() ;
/*TODO*///        makeRksTable() ;
/*TODO*///        makeDefaultPatch() ;
/*TODO*///
      }

      public static void OPLL_close(){

      }

/*TODO*///      /*********************************************************
/*TODO*///
/*TODO*///                       Generate wave data
/*TODO*///
/*TODO*///      *********************************************************/
/*TODO*///      /* Convert Amp(0 to EG_HEIGHT) to Phase(0 to 2PI). */
/*TODO*///      #if ( SLOT_AMP_BITS - PG_BITS ) > 0
/*TODO*///      #define wave2_2pi(e)  ( (e) >> ( SLOT_AMP_BITS - PG_BITS ))
/*TODO*///      #else
/*TODO*///      #define wave2_2pi(e)  ( (e) << ( PG_BITS - SLOT_AMP_BITS ))
/*TODO*///      #endif
/*TODO*///
/*TODO*///      /* Convert Amp(0 to EG_HEIGHT) to Phase(0 to 4PI). */
/*TODO*///      #if ( SLOT_AMP_BITS - PG_BITS - 1 ) > 0
/*TODO*///      #define wave2_4pi(e)  ( (e) >> ( SLOT_AMP_BITS - PG_BITS - 1 ))
/*TODO*///      #else
/*TODO*///      #define wave2_4pi(e)  ( (e) << ( 1 + PG_BITS - SLOT_AMP_BITS ))
/*TODO*///      #endif
/*TODO*///
/*TODO*///      /* Convert Amp(0 to EG_HEIGHT) to Phase(0 to 8PI). */
/*TODO*///      #if ( SLOT_AMP_BITS - PG_BITS - 2 ) > 0
/*TODO*///      #define wave2_8pi(e)  ( (e) >> ( SLOT_AMP_BITS - PG_BITS - 2 ))
/*TODO*///      #else
/*TODO*///      #define wave2_8pi(e)  ( (e) << ( 2 + PG_BITS - SLOT_AMP_BITS ))
/*TODO*///      #endif
/*TODO*///
/*TODO*///      INLINE UINT32 mrand(void){
/*TODO*///
/*TODO*///        static unsigned int seed = 0xffff ;
/*TODO*///
/*TODO*///        seed = ((seed>>15)^((seed>>12)&1)) | (( seed << 1 ) & 0xffff) ;
/*TODO*///
/*TODO*///        return seed ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Update Noise unit */
/*TODO*///      INLINE void update_noise(OPLL *opll){
/*TODO*///
/*TODO*///        opll.whitenoise = (((UINT32)(DB_NOISE/DB_STEP)) * mrand()) >> 16 ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* Update AM, PM unit */
/*TODO*///      INLINE void update_ampm(OPLL *opll){
/*TODO*///
/*TODO*///        opll.pm_phase = (opll.pm_phase + opll.pm_dphase)&(PM_DP_WIDTH - 1) ;
/*TODO*///        opll.am_phase = (opll.am_phase + opll.am_dphase)&(AM_DP_WIDTH - 1) ;
/*TODO*///        opll.lfo_am = amtable[HIGHBITS(opll.am_phase, AM_DP_BITS - AM_PG_BITS)] ;
/*TODO*///        opll.lfo_pm = pmtable[HIGHBITS(opll.pm_phase, PM_DP_BITS - PM_PG_BITS)] ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* PG */
/*TODO*///      INLINE UINT32 calc_phase(OPLL_SLOT *slot, INT32 lfo_pm){
/*TODO*///
/*TODO*///        if(slot.patch.PM)
/*TODO*///          slot.phase += (slot.dphase * lfo_pm) >> PM_AMP_BITS ;
/*TODO*///        else
/*TODO*///          slot.phase += slot.dphase ;
/*TODO*///
/*TODO*///        slot.phase &= (DP_WIDTH - 1) ;
/*TODO*///
/*TODO*///        return HIGHBITS(slot.phase, DP_BASE_BITS) ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* EG */
/*TODO*///      INLINE UINT32 calc_envelope(OPLL_SLOT *slot, UINT32 lfo_am){
/*TODO*///
/*TODO*///        #define SL(x) EXPAND_BITS((UINT32)(x/DB_STEP),DB_BITS,EG_DP_BITS)
/*TODO*///        static UINT32 SLtable[16] = {
/*TODO*///          SL( 0), SL( 3), SL( 6), SL( 9), SL(12), SL(15), SL(18), SL(21),
/*TODO*///          SL(24), SL(27), SL(30), SL(33), SL(36), SL(39), SL(42), SL(48)
/*TODO*///        } ;
/*TODO*///
/*TODO*///        UINT32 egout ;
/*TODO*///
/*TODO*///        switch(slot.eg_mode){
/*TODO*///
/*TODO*///        case ATTACK:
/*TODO*///          slot.eg_phase += slot.eg_dphase ;
/*TODO*///          if(EG_DP_WIDTH & slot.eg_phase){ /* (it is as same meaning as EG_DP_WIDTH <= slot.eg_phase) */
/*TODO*///            egout = 0 ;
/*TODO*///            slot.eg_phase= 0 ;
/*TODO*///            slot.eg_mode = DECAY ;
/*TODO*///            slot.update |= UPDATE_EG ;
/*TODO*///          }else
/*TODO*///            egout = AR_ADJUST_TABLE[HIGHBITS(slot.eg_phase, EG_DP_BITS - EG_BITS)] ;
/*TODO*///          break;
/*TODO*///
/*TODO*///        case DECAY:
/*TODO*///          slot.eg_phase += slot.eg_dphase ;
/*TODO*///          egout = HIGHBITS(slot.eg_phase, EG_DP_BITS - EG_BITS) ;
/*TODO*///          if(slot.eg_phase >= SLtable[slot.patch.SL]){
/*TODO*///            if(slot.patch.EG){
/*TODO*///              slot.eg_phase = SLtable[slot.patch.SL] ;
/*TODO*///                    slot.eg_mode = SUSHOLD ;
/*TODO*///              slot.update |= UPDATE_EG ;
/*TODO*///            }else{
/*TODO*///              slot.eg_phase = SLtable[slot.patch.SL] ;
/*TODO*///              slot.eg_mode = SUSTINE ;
/*TODO*///              slot.update |= UPDATE_EG ;
/*TODO*///            }
/*TODO*///            egout = HIGHBITS(slot.eg_phase, EG_DP_BITS - EG_BITS) ;
/*TODO*///          }
/*TODO*///
/*TODO*///          break;
/*TODO*///
/*TODO*///        case SUSHOLD:
/*TODO*///          egout = HIGHBITS(slot.eg_phase, EG_DP_BITS - EG_BITS) ;
/*TODO*///          if(slot.patch.EG == 0){
/*TODO*///            slot.eg_mode = SUSTINE ;
/*TODO*///            slot.update |= UPDATE_EG ;
/*TODO*///          }
/*TODO*///          break;
/*TODO*///
/*TODO*///        case SUSTINE:
/*TODO*///        case RELEASE:
/*TODO*///          slot.eg_phase += slot.eg_dphase ;
/*TODO*///          egout = HIGHBITS(slot.eg_phase, EG_DP_BITS - EG_BITS) ;
/*TODO*///          if(egout >= (1<<EG_BITS)){
/*TODO*///            slot.eg_mode = FINISH ;
/*TODO*///            egout = (1<<EG_BITS) - 1 ;
/*TODO*///          }
/*TODO*///          break;
/*TODO*///
/*TODO*///        case FINISH:
/*TODO*///          egout = (1<<EG_BITS) - 1 ;
/*TODO*///          break ;
/*TODO*///
/*TODO*///        default:
/*TODO*///          egout = (1<<EG_BITS) - 1 ;
/*TODO*///          break;
/*TODO*///        }
/*TODO*///
/*TODO*///        if(slot.patch.AM)
/*TODO*///          egout = EXPAND_BITS_X(egout,EG_BITS,DB_BITS) + slot.tll + lfo_am ;
/*TODO*///        else
/*TODO*///          egout = EXPAND_BITS_X(egout,EG_BITS,DB_BITS) + slot.tll ;
/*TODO*///
/*TODO*///        if(egout >= (int)(48/DB_STEP)) egout = DB_MUTE ;
/*TODO*///
/*TODO*///        return egout ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      INLINE INT32 calc_slot_car(OPLL_SLOT *slot, INT32 fm){
/*TODO*///
/*TODO*///        INT32 tmp ;
/*TODO*///
/*TODO*///        slot.update |= slot.patch.update ;
/*TODO*///        if(slot.update) refresh(slot) ;
/*TODO*///        slot.egout = calc_envelope(slot,*(slot.plfo_am)) ;
/*TODO*///        slot.pgout = calc_phase(slot,*(slot.plfo_pm)) ;
/*TODO*///        if(slot.egout>=DB_MUTE) return 0 ;
/*TODO*///
/*TODO*///        tmp = slot.sintbl[(slot.pgout+wave2_8pi(fm))&(PG_WIDTH-1)] ;
/*TODO*///
/*TODO*///        if(tmp>0)
/*TODO*///          return DB2LIN_TABLE[Min(DB_MUTE, tmp-1+slot.egout)] ;
/*TODO*///        else
/*TODO*///          return -DB2LIN_TABLE[Min(DB_MUTE, 1-tmp+slot.egout)] ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///
/*TODO*///      INLINE INT32 calc_slot_mod(OPLL_SLOT *slot){
/*TODO*///
/*TODO*///        INT32 tmp, fm ;
/*TODO*///
/*TODO*///        slot.update |= slot.patch.update ;
/*TODO*///        if(slot.update) refresh(slot) ;
/*TODO*///        slot.egout = calc_envelope(slot,*(slot.plfo_am)) ;
/*TODO*///        slot.pgout = calc_phase(slot,*(slot.plfo_pm)) ;
/*TODO*///        if(slot.egout>=DB_MUTE){
/*TODO*///          slot.output[1] = slot.output[0] ;
/*TODO*///          slot.output[0] = 0 ;
/*TODO*///          return 0 ;
/*TODO*///        }
/*TODO*///
/*TODO*///        if(slot.patch.FB!=0){
/*TODO*///          fm = wave2_4pi((slot.output[0]+slot.output[1])>>1) >> (7 - slot.patch.FB) ;
/*TODO*///          tmp = slot.sintbl[(slot.pgout+fm)&(PG_WIDTH-1)] ;
/*TODO*///        }else
/*TODO*///          tmp = slot.sintbl[slot.pgout] ;
/*TODO*///
/*TODO*///        slot.output[1] = slot.output[0] ;
/*TODO*///        if(tmp>0)
/*TODO*///          slot.output[0] =  DB2LIN_TABLE[Min(DB_MUTE, tmp-1+slot.egout)] ;
/*TODO*///        else
/*TODO*///          slot.output[0] = -DB2LIN_TABLE[Min(DB_MUTE, 1-tmp+slot.egout)] ;
/*TODO*///
/*TODO*///        return slot.output[0] ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      INLINE INT32 calc_slot_tom(OPLL_SLOT *slot){
/*TODO*///
/*TODO*///        INT32 tmp ;
/*TODO*///
/*TODO*///        if(slot.update) refresh(slot) ;
/*TODO*///        slot.egout = calc_envelope(slot,0) ;
/*TODO*///        slot.pgout = calc_phase(slot,0) ;
/*TODO*///        if(slot.egout>=DB_MUTE) return 0 ;
/*TODO*///
/*TODO*///        tmp = slot.sintbl[slot.pgout] ;
/*TODO*///        if(tmp>0) return DB2LIN_TABLE[Min(DB_MUTE, tmp-1+slot.egout+4)] ;
/*TODO*///        else return -DB2LIN_TABLE[Min(DB_MUTE, 1-tmp+slot.egout+4)] ;
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      /* calc SNARE slot */
/*TODO*///      INLINE INT32 calc_slot_snare(OPLL_SLOT *slot, UINT32 whitenoise){
/*TODO*///
/*TODO*///        INT32 tmp ;
/*TODO*///
/*TODO*///        if(slot.update) refresh(slot) ;
/*TODO*///        slot.egout = calc_envelope(slot,0) ;
/*TODO*///        slot.pgout = calc_phase(slot,0) ;
/*TODO*///        if(slot.egout>=DB_MUTE) return 0 ;
/*TODO*///
/*TODO*///        tmp = fullsintable[slot.pgout] ;
/*TODO*///
/*TODO*///        if(tmp > 0){
/*TODO*///            tmp = ((tmp - 1)>>2) + slot.egout + whitenoise ;
/*TODO*///          return DB2LIN_TABLE[Min(DB_MUTE, tmp)] ;
/*TODO*///        }else{
/*TODO*///            tmp = (((1 - tmp)>>2) + slot.egout + whitenoise) ;
/*TODO*///          return -DB2LIN_TABLE[Min(DB_MUTE,tmp)] ;
/*TODO*///        }
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      INLINE INT32 calc_slot_hat(OPLL_SLOT *slot, INT32 fm, INT32 whitenoise){
/*TODO*///
/*TODO*///        INT32 tmp ;
/*TODO*///
/*TODO*///        if(slot.update) refresh(slot) ;
/*TODO*///        slot.egout = calc_envelope(slot,0) ;
/*TODO*///        slot.pgout = calc_phase(slot,0) ;
/*TODO*///        if(slot.egout>=DB_MUTE) return 0 ;
/*TODO*///
/*TODO*///        tmp = fullsintable[(slot.pgout + wave2_8pi(fm))&(PG_WIDTH - 1)] ;
/*TODO*///
/*TODO*///        if(tmp > 0){
/*TODO*///          tmp = tmp + ((int)(6/DB_STEP)) + slot.egout + whitenoise ;
/*TODO*///          return DB2LIN_TABLE[Min(DB_MUTE, slot.egout<<2)] + DB2LIN_TABLE[Min(DB_MUTE,tmp-1)] ;
/*TODO*///        }else{
/*TODO*///          tmp = tmp - ((int)(6/DB_STEP)) - slot.egout - whitenoise ;
/*TODO*///          return DB2LIN_TABLE[Min(DB_MUTE, slot.egout<<2)] - DB2LIN_TABLE[Min(DB_MUTE,1-tmp)] ;
/*TODO*///        }
/*TODO*///
/*TODO*///      }
/*TODO*///
/*TODO*///      #ifdef OPLL_ENABLE_DEBUG
/*TODO*///        static UINT32 tick = 0 ;
/*TODO*///      #endif
/*TODO*///
      public static int OPLL_calc(OPLL opll){

        int inst = 0 , perc = 0 , out = 0 ;
        int basesin ;
        int i ;
/*TODO*///
/*TODO*///      #ifdef OPLL_ENABLE_DEBUG
/*TODO*///        tick++ ;
/*TODO*///      #endif
/*TODO*///
/*TODO*///        update_ampm(opll) ;
/*TODO*///        update_noise(opll) ;
/*TODO*///
/*TODO*///        for(i=0 ; i < 6 ; i++)
/*TODO*///          if((!opll.mask[i])&&(opll.CAR(i).eg_mode!=FINISH))
/*TODO*///            inst += calc_slot_car(opll.CAR(i),calc_slot_mod(opll.MOD(i))) ;
/*TODO*///
/*TODO*///        if(!opll.rythm_mode){
/*TODO*///
/*TODO*///          for(i=6 ; i < 9 ; i++)
/*TODO*///            if((!opll.mask[i])&&(opll.CAR(i).eg_mode!=FINISH))
/*TODO*///              inst += calc_slot_car(opll.CAR(i),calc_slot_mod(opll.MOD(i))) ;
/*TODO*///
/*TODO*///        }else if(!opll.mask[9]){
/*TODO*///
/*TODO*///      #ifdef OPLL_ENABLE_DEBUG
/*TODO*///          basesin = fullsintable[(opll.MOD(8).pgout*opll.debug_base_ml)&(PG_WIDTH-1)] ;
/*TODO*///      #else
/*TODO*///          basesin = fullsintable[(opll.MOD(8).pgout<<3)&(PG_WIDTH-1)] ;
/*TODO*///      #endif
/*TODO*///
/*TODO*///          if((!opll.mask[6])&&(opll.CAR(6).eg_mode!=FINISH)){
/*TODO*///            perc += calc_slot_car(opll.CAR(6),calc_slot_mod(opll.MOD(6))) ;
/*TODO*///          }
/*TODO*///
/*TODO*///          if(!opll.mask[7]){
/*TODO*///            if(opll.MOD(7).eg_mode!=FINISH)
/*TODO*///              perc += calc_slot_hat(opll.MOD(7),basesin,opll.whitenoise<<2) ;
/*TODO*///            if(opll.CAR(7).eg_mode!=FINISH)
/*TODO*///              perc += calc_slot_snare(opll.CAR(7),opll.whitenoise<<1) ;
/*TODO*///          }
/*TODO*///
/*TODO*///          if(!opll.mask[8]){
/*TODO*///            if(opll.MOD(8).eg_mode!=FINISH)
/*TODO*///              perc += calc_slot_tom(opll.MOD(8)) ;
/*TODO*///            else{
/*TODO*///              if(opll.MOD(8).update) refresh(opll.MOD(8)) ;
/*TODO*///              opll.MOD(8).pgout = calc_phase(opll.MOD(8),0) ;
/*TODO*///            }
/*TODO*///            if(opll.CAR(8).eg_mode!=FINISH)
/*TODO*///              perc += calc_slot_hat(opll.CAR(8),basesin,opll.whitenoise<<1) ;
/*TODO*///          }else{
/*TODO*///            if(opll.MOD(8).update) refresh(opll.MOD(8)) ;
/*TODO*///            opll.MOD(8).pgout = calc_phase(opll.MOD(8),0) ;
/*TODO*///          }
/*TODO*///        }
/*TODO*///
/*TODO*///        perc = perc << 1 ;
/*TODO*///
/*TODO*///      #if SLOT_AMP_BITS > 8
/*TODO*///        inst = (inst >> (SLOT_AMP_BITS - 8)) ;
/*TODO*///        perc = (perc >> (SLOT_AMP_BITS - 9)) ;
/*TODO*///      #else
/*TODO*///        inst = (inst << (8 - SLOT_AMP_BITS)) ;
/*TODO*///        perc = (perc << (9 - SLOT_AMP_BITS)) ;
/*TODO*///      #endif
/*TODO*///
/*TODO*///        out = inst + ((perc * opll.rythmVolume) >> 6) ;
/*TODO*///        out = ( out  * opll.masterVolume ) >> 2 ;
/*TODO*///
/*TODO*///        if(out>32767) out = 32767 ;
/*TODO*///        else if(out<-32768) out = -32768 ;
/*TODO*///
/*TODO*///        opll.patch[0][0].update = 0 ;
/*TODO*///        opll.patch[0][1].update = 0 ;
/*TODO*///
        return out ;

      }

      /****************************************************

                Interfaces for through I/O port.

      *****************************************************/
      public static void OPLL_writeReg(OPLL opll, int reg, int data){

/*TODO*///        OPLL_PATCH *p ;
/*TODO*///        int i,v ;
/*TODO*///
/*TODO*///        p = opll.patch[0] ;
/*TODO*///        data = data&0xff ;
/*TODO*///
/*TODO*///        switch(reg){
/*TODO*///        case 0x00:
/*TODO*///        case 0x01:
/*TODO*///          p[reg%2].AM = (data>>7)&1 ;
/*TODO*///          p[reg%2].PM = (data>>6)&1 ;
/*TODO*///          p[reg%2].EG = (data>>5)&1 ;
/*TODO*///          p[reg%2].KR = (data>>4)&1 ;
/*TODO*///          p[reg%2].ML = (data)&15 ;
/*TODO*///          p[reg%2].update |= UPDATE_ALL ;
/*TODO*///          break;
/*TODO*///
/*TODO*///        case 0x02:
/*TODO*///          p[0].KL = (data>>6)&3 ;
/*TODO*///          p[0].TL = (data)&63 ;
/*TODO*///          p[reg%2].update |= UPDATE_TLL ;
/*TODO*///          break ;
/*TODO*///
/*TODO*///        case 0x03:
/*TODO*///          p[1].KL = (data>>6)&3 ;
/*TODO*///          p[1].WF = (data>>4)&1 ;
/*TODO*///          p[0].WF = (data>>3)&1 ;
/*TODO*///          p[0].FB = (data)&7 ;
/*TODO*///          p[reg%2].update |= UPDATE_TLL | UPDATE_WF ;
/*TODO*///          break ;
/*TODO*///
/*TODO*///        case 0x04:
/*TODO*///        case 0x05:
/*TODO*///          p[reg%2].AR = (data>>4)&15 ;
/*TODO*///          p[reg%2].DR = (data)&15 ;
/*TODO*///          p[reg%2].update |= UPDATE_EG ;
/*TODO*///          break ;
/*TODO*///
/*TODO*///        case 0x06:
/*TODO*///        case 0x07:
/*TODO*///          p[reg%2].SL = (data>>4)&15 ;
/*TODO*///          p[reg%2].RR = (data)&15 ;
/*TODO*///          p[reg%2].update |= UPDATE_EG ;
/*TODO*///          break ;
/*TODO*///
/*TODO*///        case 0x0e:
/*TODO*///          if(((data>>5)&1)^(opll.rythm_mode)) setRythmMode(opll,(data&32)>>5) ;
/*TODO*///
/*TODO*///          if(opll.rythm_mode){
/*TODO*///            opll.slot_on_flag[SLOT_BD1] = (opll.reg[0x0e]&0x10) | (opll.reg[0x26]&0x10) ;
/*TODO*///            opll.slot_on_flag[SLOT_BD2] = (opll.reg[0x0e]&0x10) | (opll.reg[0x26]&0x10) ;
/*TODO*///            opll.slot_on_flag[SLOT_SD]  = (opll.reg[0x0e]&0x08) | (opll.reg[0x27]&0x10) ;
/*TODO*///            opll.slot_on_flag[SLOT_HH]  = (opll.reg[0x0e]&0x01) | (opll.reg[0x27]&0x10) ;
/*TODO*///            opll.slot_on_flag[SLOT_TOM] = (opll.reg[0x0e]&0x04) | (opll.reg[0x28]&0x10) ;
/*TODO*///            opll.slot_on_flag[SLOT_CYM] = (opll.reg[0x0e]&0x02) | (opll.reg[0x28]&0x10) ;
/*TODO*///            if(data&0x10) keyOn_BD(opll) ; else keyOff_BD(opll) ;
/*TODO*///            if(data&0x8) keyOn_SD(opll) ; else keyOff_SD(opll) ;
/*TODO*///            if(data&0x4) keyOn_TOM(opll) ; else keyOff_TOM(opll) ;
/*TODO*///            if(data&0x2) keyOn_CYM(opll) ; else keyOff_CYM(opll) ;
/*TODO*///            if(data&0x1) keyOn_HH(opll) ; else keyOff_HH(opll) ;
/*TODO*///          }
/*TODO*///          break ;
/*TODO*///
/*TODO*///        case 0x0f:
/*TODO*///          break ;
/*TODO*///
/*TODO*///        case 0x10:  case 0x11:  case 0x12:  case 0x13:
/*TODO*///        case 0x14:  case 0x15:  case 0x16:  case 0x17:
/*TODO*///        case 0x18:
/*TODO*///          setFnumber(opll, reg-0x10, data + ((opll.reg[reg+0x10]&1)<<8)) ;
/*TODO*///          break ;
/*TODO*///
/*TODO*///        case 0x20:  case 0x21:  case 0x22:  case 0x23:
/*TODO*///        case 0x24:  case 0x25:
/*TODO*///          setFnumber(opll, reg-0x20, ((data&1)<<8) + opll.reg[reg-0x10]) ;
/*TODO*///          setBlock(opll, reg-0x20, (data>>1)&7 ) ;
/*TODO*///          opll.slot_on_flag[(reg-0x20)*2] = opll.slot_on_flag[(reg-0x20)*2+1] = (opll.reg[reg])&0x10 ;
/*TODO*///          if(data&0x10) keyOn(opll, reg-0x20) ;
/*TODO*///          else keyOff(opll, reg-0x20) ;
/*TODO*///          if((opll.reg[reg]^data)&0x20)
/*TODO*///            setSustine(opll, reg-0x20, (data>>5)&1) ;
/*TODO*///          break ;
/*TODO*///
/*TODO*///        case 0x26:
/*TODO*///          setFnumber(opll, 6, ((data&1)<<8) + opll.reg[0x16]) ;
/*TODO*///          setBlock(opll, 6, (data>>1)&7 ) ;
/*TODO*///          opll.slot_on_flag[SLOT_BD1] = opll.slot_on_flag[SLOT_BD2] = (opll.reg[0x26])&0x10 ;
/*TODO*///          if(opll.reg[0x0e]&32){
/*TODO*///            opll.slot_on_flag[SLOT_BD1] |= (opll.reg[0x0e])&0x10 ;
/*TODO*///            opll.slot_on_flag[SLOT_BD2] |= (opll.reg[0x0e])&0x10 ;
/*TODO*///          }
/*TODO*///          if(data&0x10) keyOn(opll, 6) ; else keyOff(opll, 6) ;
/*TODO*///          if((opll.reg[0x26]^data)&0x20) setSustine(opll, 6, (data>>5)&1) ;
/*TODO*///          break ;
/*TODO*///
/*TODO*///        case 0x27:
/*TODO*///          setFnumber(opll, 7, ((data&1)<<8) + opll.reg[0x17]) ;
/*TODO*///          setBlock(opll, 7, (data>>1)&7 ) ;
/*TODO*///          opll.slot_on_flag[SLOT_SD] = opll.slot_on_flag[SLOT_HH] = (opll.reg[0x27])&0x10 ;
/*TODO*///          if(opll.reg[0x0e]&32){
/*TODO*///            opll.slot_on_flag[SLOT_SD]  |= (opll.reg[0x0e])&0x08 ;
/*TODO*///            opll.slot_on_flag[SLOT_HH]  |= (opll.reg[0x0e])&0x01 ;
/*TODO*///          }
/*TODO*///          if(data&0x10) keyOn(opll, 7) ; else keyOff(opll, 7) ;
/*TODO*///          if((opll.reg[0x27]^data)&0x20) setSustine(opll, 7, (data>>5)&1) ;
/*TODO*///          break ;
/*TODO*///
/*TODO*///        case 0x28:
/*TODO*///          setFnumber(opll, 8, ((data&1)<<8) + opll.reg[0x18]) ;
/*TODO*///          setBlock(opll, 8, (data>>1)&7 ) ;
/*TODO*///          opll.slot_on_flag[SLOT_TOM] = opll.slot_on_flag[SLOT_CYM] = (opll.reg[0x28])&0x10 ;
/*TODO*///          if(opll.reg[0x0e]&32){
/*TODO*///            opll.slot_on_flag[SLOT_TOM] |= (opll.reg[0x0e])&0x04 ;
/*TODO*///            opll.slot_on_flag[SLOT_CYM] |= (opll.reg[0x0e])&0x02 ;
/*TODO*///          }
/*TODO*///          if(data&0x10) keyOn(opll, 8) ; else keyOff(opll, 8) ;
/*TODO*///          if((opll.reg[reg]^data)&0x20) setSustine(opll, 8, (data>>5)&1) ;
/*TODO*///          break ;
/*TODO*///
/*TODO*///        case 0x30: case 0x31: case 0x32: case 0x33: case 0x34:
/*TODO*///        case 0x35: case 0x36: case 0x37: case 0x38:
/*TODO*///          i = (data>>4)&15 ;
/*TODO*///          v = data&15 ;
/*TODO*///          if((opll.rythm_mode)&&(reg>=0x36)) {
/*TODO*///            switch(reg){
/*TODO*///            case 0x37 :
/*TODO*///              setSlotVolume(opll.MOD(7), i<<2) ;
/*TODO*///              break ;
/*TODO*///            case 0x38 :
/*TODO*///              setSlotVolume(opll.MOD(8), i<<2) ;
/*TODO*///              break ;
/*TODO*///            }
/*TODO*///          }else{
/*TODO*///            setPatch(opll, reg-0x30, i) ;
/*TODO*///          }
/*TODO*///
/*TODO*///          setVolume(opll, reg-0x30, v<<2) ;
/*TODO*///          break ;
/*TODO*///
/*TODO*///        default:
/*TODO*///          break ;
/*TODO*///
/*TODO*///        }
/*TODO*///
/*TODO*///        opll.reg[reg] = (unsigned char)data ;

      }

/*TODO*///      /****************************************************************
/*TODO*///
/*TODO*///                           debug controller
/*TODO*///
/*TODO*///      ****************************************************************/
/*TODO*///      #ifdef OPLL_ENABLE_DEBUG
/*TODO*///      void debug_base_ml_ctrl(OPLL *opll,int i){
/*TODO*///
/*TODO*///        opll.debug_base_ml += i ;
/*TODO*///        if(opll.debug_base_ml>31) opll.debug_base_ml = 31 ;
/*TODO*///        if(opll.debug_base_ml<1) opll.debug_base_ml = 1 ;
/*TODO*///
/*TODO*///      }
/*TODO*///      #endif
}
