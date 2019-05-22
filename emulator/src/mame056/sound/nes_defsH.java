/*****************************************************************************

  MAME/MESS NES APU CORE

  Based on the Nofrendo/Nosefart NES N2A03 sound emulation core written by
  Matthew Conte (matt@conte.com) and redesigned for use in MAME/MESS by
  Who Wants to Know? (wwtk@mail.com)

  This core is written with the advise and consent of Matthew Conte and is
  released under the GNU Public License.  This core is freely avaiable for
  use in any freeware project, subject to the following terms:

  Any modifications to this code must be duly noted in the source and
  approved by Matthew Conte and myself prior to public submission.

 *****************************************************************************

   NES_DEFS.H

   NES APU internal type definitions and constants.

 *****************************************************************************/

package mame056.sound;

import static common.ptr.*;
import common.subArrays.IntArray;

public class nes_defsH {
    /* QUEUE TYPES */
    public static int QUEUE_SIZE = 0x2000;
    public static int QUEUE_MAX  = (QUEUE_SIZE-1);

    public static class queue_t
    {
      public int pos;
      public int reg,val;
    };

    /* REGISTER DEFINITIONS */
    public static int  APU_WRA0     = 0x00;
    public static int  APU_WRA1     = 0x01;
    public static int  APU_WRA2     = 0x02;
    public static int  APU_WRA3     = 0x03;
    public static int  APU_WRB0     = 0x04;
    public static int  APU_WRB1     = 0x05;
    public static int  APU_WRB2     = 0x06;
    public static int  APU_WRB3     = 0x07;
    public static int  APU_WRC0     = 0x08;
    public static int  APU_WRC2     = 0x0A;
    public static int  APU_WRC3     = 0x0B;
    public static int  APU_WRD0     = 0x0C;
    public static int  APU_WRD2     = 0x0E;
    public static int  APU_WRD3     = 0x0F;
    public static int  APU_WRE0     = 0x10;
    public static int  APU_WRE1     = 0x11;
    public static int  APU_WRE2     = 0x12;
    public static int  APU_WRE3     = 0x13;

    public static int  APU_SMASK    = 0x15;

    public static int  NOISE_LONG   = 0x4000;
    public static int  NOISE_SHORT  = 93;

    /* CHANNEL TYPE DEFINITIONS */

    /* Square Wave */
    public static class square_t
    {
       public int[] regs=new int[4];
       public int vbl_length;
       public int freq;
       public float phaseacc;
       public float output_vol;
       public float env_phase;
       public float sweep_phase;
       public int adder;
       public int env_vol;
       public boolean enabled;
    };

    /* Triangle Wave */
    public static class triangle_t
    {
       public int[] regs=new int[4]; /* regs[1] unused */
       public int linear_length;
       public int vbl_length;
       public int write_latency;
       public float phaseacc;
       public float output_vol;
       public int adder;
       public boolean counter_started;
       public boolean enabled;
    };

    /* Noise Wave */
    public static class noise_t
    {
       public int[] regs=new int[4]; /* regs[1] unused */
       public int cur_pos;
       public int vbl_length;
       public float phaseacc;
       public float output_vol;
       public float env_phase;
       public int env_vol;
       public boolean enabled;
    };

    /* DPCM Wave */
    public static class dpcm_t
    {
       public int[] regs=new int[4];
       public int address;
       public int length;
       public int bits_left;
       public float phaseacc;
       public float output_vol;
       public int cur_byte;
       public boolean enabled;
       public boolean irq_occurred;
       public UBytePtr cpu_mem = new UBytePtr();
       public int vol;
    };

    /* APU type */
    public static class apu_t
    {
       /* Sound channels */
       public square_t[]   squ=new square_t[2];
       public triangle_t tri = new triangle_t();
       public noise_t    noi = new noise_t();
       public dpcm_t     dpcm = new dpcm_t();

       /* APU registers */
       public int[] regs=new int[22];

       /* Sound pointers */
       public IntArray buffer;

    //#ifdef USE_QUEUE

       /* Event queue */
       public queue_t[] queue = new queue_t[QUEUE_SIZE];
       public int head,tail;

    //#else

       int buf_pos;

    //#endif

    };

    /* CONSTANTS */

    /* vblank length table used for squares, triangle, noise */
    static int vbl_length[] =
    {
       5, 127, 10, 1, 19,  2, 40,  3, 80,  4, 30,  5, 7,  6, 13,  7,
       6,   8, 12, 9, 24, 10, 48, 11, 96, 12, 36, 13, 8, 14, 16, 15
    };

    /* frequency limit of square channels */
    static int freq_limit[] =
    {
       0x3FF, 0x555, 0x666, 0x71C, 0x787, 0x7C1, 0x7E0, 0x7F0,
    };

    /* table of noise frequencies */
    static int noise_freq[] =
    {
       4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 2046
    };

    /* dpcm transfer freqs */
    static int dpcm_clocks[] =
    {
       428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106, 85, 72, 54
    };

    /* ratios of pos/neg pulse for square waves */
    /* 2/16 = 12.5%, 4/16 = 25%, 8/16 = 50%, 12/16 = 75% */
    static int duty_lut[] =
    {
       2, 4, 8, 12
    };
}
