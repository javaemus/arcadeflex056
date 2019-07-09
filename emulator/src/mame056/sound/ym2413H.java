/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.sound;

import static common.subArrays.*;

public class ym2413H {
    /*#define PI 3.14159265358979*/

    /* voice data */
    public static class OPLL_PATCH {
      public int TL,FB,EG,ML,AR,DR,SL,RR,KR,KL,AM,PM,WF ;
      public int update ; /* for real-time update */
    };

    /* slot */
    public static class OPLL_SLOT {

      public OPLL_PATCH patch;

      public int type ;          /* 0 : modulator 1 : carrier */
      public int update ;

      /* OUTPUT */
      public int[] output = new int[2] ;      /* Output value of slot */

      /* for Phase Generator (PG) */
      public IntArray sintbl ;     /* Wavetable */
      public int phase ;      /* Phase */
      public int dphase ;     /* Phase increment amount */
      public int pgout ;      /* output */

      /* for Envelope Generator (EG) */
      public int fnum ;          /* F-Number */
      public int block ;         /* Block */
      public int volume ;        /* Current volume */
      public int sustine ;       /* Sustine 1 = ON, 0 = OFF */
      public int tll ;	      /* Total Level + Key scale level*/
      public int rks ;        /* Key scale offset (Rks) */
      public int eg_mode ;       /* Current state */
      public int eg_phase ;   /* Phase */
      public int eg_dphase ;  /* Phase increment amount */
      public int egout ;      /* output */

      /* LFO (refer to opll->*) */
      public IntArray plfo_am ;
      public IntArray plfo_pm ;

    };

    /* Channel */
    public static class OPLL_CH {

      public int patch_number ;
      public int key_status ;
      public OPLL_SLOT mod, car ;

    //#ifdef OPLL_ENABLE_DEBUG
    /*TODO*///  public int debug_keyonpatch[4] ;
    //#endif

    };

    /* opll */
    public static class OPLL {

      public int[] output = new int[4] ;

      /* Register */
      public char[] reg = new char[0x40] ;
      public int[] slot_on_flag = new int[18] ;

      /* Rythm Mode : 0 = OFF, 1 = ON */
      public int rythm_mode ;

      /* Pitch Modulator */
      public int pm_phase ;
      public int pm_dphase ;
      public int lfo_pm ;

      /* Amp Modulator */
      public int am_phase ;
      public int am_dphase ;
      public int lfo_am ;

      /* Noise Generator */
      public int whitenoise ;

      /* Channel & Slot */
      public OPLL_CH[] ch = new OPLL_CH[9] ;
      public OPLL_SLOT[] slot = new OPLL_SLOT[18] ;

      /* Voice Data */
      public OPLL_PATCH[] patch = new OPLL_PATCH[19] ;
      public int[] user_patch_update = new int[2] ; /* flag for check patch update */

      public int[] mask = new int[10] ; /* mask[9] = RYTHM */

      public int masterVolume ; /* 0min -- 64 -- 127 max (Liner) */
      public int rythmVolume ;  /* 0min -- 64 -- 127 max (Liner) */

    //#ifdef OPLL_ENABLE_DEBUG
      /*TODO*///public int debug_rythm_flag ;
      /*TODO*///public int debug_base_ml ;
      /*TODO*///public int feedback_type ;    /* feedback type select */
      /*TODO*///public FILE *logfile ;
    //#endif

    };

}
