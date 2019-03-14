/***************************************************************************

  The Konami_1 CPU is a 6809 with opcodes scrambled. Here is how to
  descramble them.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.machine;

import common.ptr.UBytePtr;
import static mame056.common.memory_region;
import static mame056.common.memory_region_length;
import static mame056.commonH.REGION_CPU1;
import static mame056.memory.memory_set_opcode_base;

public class konami
{
	
	
	
	public static char decodebyte( char opcode, short address )
	{
	/*
	>
	> CPU_D7 = (EPROM_D7 & ~ADDRESS_1) | (~EPROM_D7 & ADDRESS_1)  >
	> CPU_D6 = EPROM_D6
	>
	> CPU_D5 = (EPROM_D5 & ADDRESS_1) | (~EPROM_D5 & ~ADDRESS_1) >
	> CPU_D4 = EPROM_D4
	>
	> CPU_D3 = (EPROM_D3 & ~ADDRESS_3) | (~EPROM_D3 & ADDRESS_3) >
	> CPU_D2 = EPROM_D2
	>
	> CPU_D1 = (EPROM_D1 & ADDRESS_3) | (~EPROM_D1 & ~ADDRESS_3) >
	> CPU_D0 = EPROM_D0
	>
	*/
		char xormask;
	
	
		xormask = 0;
		if ((address & 0x02) != 0) xormask |= 0x80;
		else xormask |= 0x20;
		if ((address & 0x08) != 0) xormask |= 0x08;
		else xormask |= 0x02;
	
		return (char) (opcode ^ xormask);
	}
	
	
	
	public static void decode(int cpu)
	{
		UBytePtr rom = memory_region(REGION_CPU1+cpu);
		int diff = memory_region_length(REGION_CPU1+cpu) / 2;
		int A;
	
	
		memory_set_opcode_base(cpu, new UBytePtr(rom, diff));
	
		for (A = 0;A < diff;A++)
		{
			rom.write(A+diff, decodebyte(rom.read(A), (short) A));
		}
	}
	
	public static void konami1_decode()
	{
		decode(0);
	}
	
	public static void konami1_decode_cpu2()
	{
		decode(1);
	}
}
