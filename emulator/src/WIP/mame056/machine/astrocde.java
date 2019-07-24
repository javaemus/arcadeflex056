/**************************************************************************

	Interrupt System Hardware for Bally/Midway games

 	Mike@Dissfulfils.co.uk

**************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.commonH.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.inptport.*;
import static mame056.cpuintrfH.*;
import static mame056.common.*;
// refactor
import static arcadeflex036.osdepend.logerror;

public class astrocde
{
	static int Skip=0;
	
	public static ReadHandlerPtr gorf_timer_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
	
	
		if ((RAM.read(0x5A93)==160) || (RAM.read(0x5A93)==4)) 	/* INVADERS AND    */
		{												/* GALAXIAN SCREEN */
			if (cpu_get_pc()==0x3086)
			{
			    if(--Skip==-1)
				{
					Skip=2;
				}
			}
	
		   	return Skip;
		}
		else
		{
			return RAM.read(0xD0A5);
		}
	
	} };
	
	
	/****************************************************************************
	 * Seawolf Controllers
	 ****************************************************************************/
	
	/*
	 * Seawolf2 uses rotary controllers on input ports 10 + 11
	 * each controller responds 0-63 for reading, with bit 7 as
	 * fire button.
	 *
	 * The controllers look like they returns Grays binary,
	 * so I use a table to translate my simple counter into it!
	 */
	
	static int ControllerTable[] = {
		0  , 1  , 3  , 2  , 6  , 7  , 5  , 4  ,
		12 , 13 , 15 , 14 , 10 , 11 , 9  , 8  ,
		24 , 25 , 27 , 26 , 30 , 31 , 29 , 28 ,
		20 , 21 , 23 , 22 , 18 , 19 , 17 , 16 ,
		48 , 49 , 51 , 50 , 54 , 55 , 53 , 52 ,
		60 , 61 , 63 , 62 , 58 , 59 , 57 , 56 ,
		40 , 41 , 43 , 42 , 46 , 47 , 45 , 44 ,
		36 , 37 , 39 , 38 , 34 , 35 , 33 , 32
	};
	
	public static ReadHandlerPtr seawolf2_controller1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return (input_port_0_r.handler(0) & 0xc0) + ControllerTable[input_port_0_r.handler(0) & 0x3f];
	} };
	
	public static ReadHandlerPtr seawolf2_controller2_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return (input_port_1_r.handler(0) & 0xc0) + ControllerTable[input_port_1_r.handler(0) & 0x3f];
	} };
	
	
	static int ebases_trackball_select = 0;
	
	public static WriteHandlerPtr ebases_trackball_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		ebases_trackball_select = data;
	} };
	
	public static ReadHandlerPtr ebases_trackball_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int ret = readinputport(3 + ebases_trackball_select);
		logerror("Port %d = %d\n", ebases_trackball_select, ret);
		return ret;
	} };
}
