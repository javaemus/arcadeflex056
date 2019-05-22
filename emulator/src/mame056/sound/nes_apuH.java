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

   NES_APU.H

   NES APU external interface.

 *****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.sound;

public class nes_apuH {
    
        public static int MAX_NESPSG = 2;
        
        public static double N2A03_DEFAULTCLOCK = (21477272.724 / 12);
	
	/* AN EXPLANATION
	 *
	 * The NES APU is actually integrated into the Nintendo processor.
	 * You must supply the same number of APUs as you do processors.
	 * Also make sure to correspond the memory regions to those used in the
	 * processor, as each is shared.
	 */
	public static class NESinterface
	{
	   public int num;                 /* total number of chips in the machine */
	   public int[] region = new int[MAX_NESPSG];  /* DMC regions */
	   public int[] volume = new int[MAX_NESPSG];

            public NESinterface(int num, int[] region, int[] volume) {
                this.num = num;
                this.region = region;
                this.volume = volume;
            }
            
	};
}
