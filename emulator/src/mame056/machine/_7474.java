/*****************************************************************************

  7474 positive-edge-triggered D-type flip-flop with preset, clear and
       complementary outputs.  There are 2 flip-flops per chips


  7474 pin layout:

	[ 1] /1CLR		   VCC [14]
	[ 2]  1D		 /2CLR [13]
  	[ 3]  1CLK		    2D [12]
	[ 4] /1PR		  2CLK [11]
	[ 5]  1Q	      /2PR [10]
	[ 6] /1Q	        2Q [9]
	[ 7]  GND	       /2Q [8]


  Truth table (logical states):

	PR	CLR	CLK D | Q  /Q
	--------------|-------
	H	L	X   X | H	L
	L   H   X   X | L   H
	H   H   X   X | H   H  (Note 1)
	L   L  _-   X | D  /D
	L   L   L   X | Q0 /Q0
	----------------------
	L	= lo (0)
	H	= hi (1)
	X	= any state
	_-	= raising edge
	Q0  = previous state

	Note 1: Non-stable configuration

*****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.machine;

import static mame056.machine._7474H.*;

public class _7474
{
	
	
	public static class TTL7474
	{
		public TTL7474_interface output_changed_cb;
		public int clear;			/* pin 1/13 */
		public int preset;			/* pin 4/10 */
		public int clock;			/* pin 3/11 */
		public int d;				/* pin 2/12 */
		public int output;			/* pin 5/9 */
		public int output_comp;	/* pin 6/8 */
	};
	
	public static TTL7474[] chips = new TTL7474[MAX_TTL7474];
	
        static {
            for (int i=0 ; i<MAX_TTL7474 ; i++)
                chips[i] = new TTL7474();
        }
	
	public static void TTL7474_set_inputs(int which, int clear, int preset, int clock, int d)
	{
		TTL7474 chip;
		int new_output, new_output_comp;
	
	
		chip = chips[which];
	
	
		new_output = chip.output;
		new_output_comp = chip.output_comp;
	
	
		if (clear != -1)	chip.clear = clear;
		if (preset != -1)	chip.preset = preset;
		if (d != -1)		chip.d = d;
															/*	PR	CLR	CLK D | Q  /Q */
	
		if ((chip.preset!=0) && (chip.clear==0))					/* 	H	L	X   X | H	L */
		{
			new_output 		= 1;
			new_output_comp = 0;
		}
		else if ((chip.preset==0) && (chip.clear!=0))				/*  L   H   X   X | L   H */
		{
			new_output 		= 0;
			new_output_comp = 1;
		}
		else if ((chip.preset!=0) && (chip.clear!=0))				/*  H   H   X   X | H   H */
		{
			new_output 		= 1;
			new_output_comp = 1;
		}
		else
		{
			if ((clock != -1) && (chip.clock==0) && (clock!=0))		/*  L   L  _-   X | D  /D */
			{
				new_output 		=  chip.d;
				new_output_comp = (chip.d==0)? 1 : 0;
			}
	
			/* otherwise, the output is not changed */
		}
	
		if (clock != -1)  chip.clock = clock;
	
	
		if ((new_output != chip.output) || (new_output_comp != chip.output_comp))
		{
			chip.output = new_output;
			chip.output_comp = new_output_comp;
	
			chip.output_changed_cb.output_changed_cb.handler(0);
		}
	}
	
	
	public static void TTL7474_config(int which, TTL7474_interface intf)
        {
		TTL7474 chip;
	
	
		if (which >= MAX_TTL7474) return;
	
	
		chip = chips[which];
	
		chip.output_changed_cb = intf;
	
		/* all inputs are open first - not sure if this is correct */
	    chip.clear = 0;
	    chip.preset = 0;
	    chip.clock = 1;
	    chip.d = 1;
	    chip.output = -1;
	    chip.output_comp = -1;
	}
	
	
	public static int TTL7474_output_r(int which)
	{
		return chips[which].output;
	}
	
	public static int TTL7474_output_comp_r(int which)
	{
		return chips[which].output_comp;
	}
}
