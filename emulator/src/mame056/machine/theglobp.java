/**
 * Ported to v0.56
 */
package mame056.machine;

import static arcadeflex056.fucPtr.*;
import static arcadeflex036.osdepend.*;

import static common.ptr.*;

import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.memoryH.*;

import static mame056.drivers.pacman.*;

public class theglobp {

    static int counter_glob = 0;

    static void theglobp_decrypt_rom_8() {
        int oldbyte, inverted_oldbyte, newbyte;
        int mem;
        UBytePtr RAM;

        RAM = memory_region(REGION_CPU1);

        for (mem = 0; mem < 0x4000; mem++) {
            oldbyte = RAM.read(mem);
            inverted_oldbyte = ~oldbyte;

            /*	Note: D2 is inverted and connected to D1, D5 is inverted and
			connected to D0.  The other six data bits are converted by a
			PAL10H8 driven by the counter. */
            newbyte = 0;

            /* Direct inversion */
            newbyte = (inverted_oldbyte & 0x04) >> 1;
            newbyte |= (inverted_oldbyte & 0x20) >> 5;
            /* PAL */
            newbyte |= (oldbyte & 0x01) << 5;
            newbyte |= (oldbyte & 0x02) << 1;
            newbyte |= (inverted_oldbyte & 0x08) << 4;
            newbyte |= (inverted_oldbyte & 0x10) >> 1;
            newbyte |= (inverted_oldbyte & 0x40) >> 2;
            newbyte |= (inverted_oldbyte & 0x80) >> 1;

            RAM.write(mem + 0x10000, newbyte);
        }

        return;
    }

    static void theglobp_decrypt_rom_9() {
        int oldbyte, inverted_oldbyte, newbyte;
        int mem;
        UBytePtr RAM;

        RAM = memory_region(REGION_CPU1);

        for (mem = 0; mem < 0x4000; mem++) {
            oldbyte = RAM.read(mem);
            inverted_oldbyte = ~oldbyte;

            /*	Note: D2 is inverted and connected to D1, D5 is inverted and
			connected to D0.  The other six data bits are converted by a
			PAL10H8 driven by the counter. */
            newbyte = 0;

            /* Direct inversion */
            newbyte = (inverted_oldbyte & 0x04) >> 1;
            newbyte |= (inverted_oldbyte & 0x20) >> 5;
            /* PAL */
            newbyte |= (oldbyte & 0x01) << 5;
            newbyte |= (inverted_oldbyte & 0x02) << 6;
            newbyte |= (oldbyte & 0x08) << 1;
            newbyte |= (inverted_oldbyte & 0x10) >> 1;
            newbyte |= (inverted_oldbyte & 0x40) >> 4;
            newbyte |= (inverted_oldbyte & 0x80) >> 1;

            RAM.write(mem + 0x14000, newbyte);
        }

        return;
    }

    static void theglobp_decrypt_rom_A() {
        int oldbyte, inverted_oldbyte, newbyte;
        int mem;
        UBytePtr RAM;

        RAM = memory_region(REGION_CPU1);

        for (mem = 0; mem < 0x4000; mem++) {
            oldbyte = RAM.read(mem);
            inverted_oldbyte = ~oldbyte;

            /*	Note: D2 is inverted and connected to D1, D5 is inverted and
			connected to D0.  The other six data bits are converted by a
			PAL10H8 driven by the counter. */
            newbyte = 0;

            /* Direct inversion */
            newbyte = (inverted_oldbyte & 0x04) >> 1;
            newbyte |= (inverted_oldbyte & 0x20) >> 5;
            /* PAL */
            newbyte |= (inverted_oldbyte & 0x01) << 6;
            newbyte |= (oldbyte & 0x02) << 1;
            newbyte |= (inverted_oldbyte & 0x08) << 4;
            newbyte |= (inverted_oldbyte & 0x10) << 1;
            newbyte |= (inverted_oldbyte & 0x40) >> 2;
            newbyte |= (oldbyte & 0x80) >> 4;

            RAM.write(mem + 0x18000, newbyte);
        }

        return;
    }

    static void theglobp_decrypt_rom_B() {
        int oldbyte, inverted_oldbyte, newbyte;
        int mem;
        UBytePtr RAM;

        RAM = memory_region(REGION_CPU1);

        for (mem = 0; mem < 0x4000; mem++) {
            oldbyte = RAM.read(mem);
            inverted_oldbyte = ~oldbyte;

            /*	Note: D2 is inverted and connected to D1, D5 is inverted and
			connected to D0.  The other six data bits are converted by a
			PAL10H8 driven by the counter. */
            newbyte = 0;

            /* Direct inversion */
            newbyte = (inverted_oldbyte & 0x04) >> 1;
            newbyte |= (inverted_oldbyte & 0x20) >> 5;
            /* PAL */
            newbyte |= (inverted_oldbyte & 0x01) << 6;
            newbyte |= (inverted_oldbyte & 0x02) << 6;
            newbyte |= (oldbyte & 0x08) << 1;
            newbyte |= (inverted_oldbyte & 0x10) << 1;
            newbyte |= (inverted_oldbyte & 0x40) >> 4;
            newbyte |= (oldbyte & 0x80) >> 4;

            RAM.write(mem + 0x1C000, newbyte);
        }

        return;
    }

    public static ReadHandlerPtr theglobp_decrypt_rom = new ReadHandlerPtr() {
        public int handler(int offset) {
            UBytePtr RAM = memory_region(REGION_CPU1);

            if ((offset & 0x01) != 0) {
                counter_glob = counter_glob - 1;
                if (counter_glob < 0) {
                    counter_glob = 0x0F;
                }
            } else {
                counter_glob = (counter_glob + 1) & 0x0F;
            }

            switch (counter_glob) {
                case 0x08:
                    cpu_setbank(1, new UBytePtr(RAM, 0x10000));
                    break;
                case 0x09:
                    cpu_setbank(1, new UBytePtr(RAM,0x14000));
                    break;
                case 0x0A:
                    cpu_setbank(1, new UBytePtr(RAM,0x18000));
                    break;
                case 0x0B:
                    cpu_setbank(1, new UBytePtr(RAM,0x1C000));
                    break;
                default:
                    logerror("Invalid counter = %02X\n", counter_glob);
                    break;
            }

            return 0;
        }
    };

    public static InitMachinePtr theglobp_init_machine = new InitMachinePtr() {
        public void handler() {

            UBytePtr RAM = memory_region(REGION_CPU1);

            /* While the PAL supports up to 16 decryption methods, only four
		are actually used in the PAL.  Therefore, we'll take a little
		memory overhead and decrypt the ROMs using each method in advance. */
            theglobp_decrypt_rom_8();
            theglobp_decrypt_rom_9();
            theglobp_decrypt_rom_A();
            theglobp_decrypt_rom_B();

            /* The initial state of the counter is 0x0A */
            counter_glob = 0x0A;
            cpu_setbank(1, new UBytePtr(RAM, 0x18000));

            pacman_init_machine.handler();
        }
    };

}
