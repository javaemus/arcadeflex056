/**
 * ported to v0.56
 * ported to v0.37b7
 */
/**
 * Changelog
 * ---------
 * 20/04/2019 - ported file to 0.56 (shadow)
 */
package mame056.machine;

import static common.ptr.*;

import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.memory.*;

public class konami {

    static /*unsigned*/ char decodebyte(char/*unsigned char*/ opcode, char address) {
        /*unsigned*/ char xormask;

     	xormask = 0;
		if ((address & 0x02)!=0) xormask |= 0x80;
		else xormask |= 0x20;
		if ((address & 0x08)!=0) xormask |= 0x08;
		else xormask |= 0x02;
	
		return (char) (opcode ^ xormask);
    }

    static void decode(int cpu) {
        UBytePtr rom = new UBytePtr(memory_region(REGION_CPU1 + cpu));
        int diff = memory_region_length(REGION_CPU1 + cpu) / 2;
        int A;

        memory_set_opcode_base(cpu, new UBytePtr(rom, diff));

        for (A = 0; A < diff; A++) {
            rom.write(A + diff, decodebyte(rom.read(A), (char) A));
        }
    }

    public static void konami1_decode() {
        decode(0);
    }

    public static void konami1_decode_cpu2() {
        decode(1);
    }
}
