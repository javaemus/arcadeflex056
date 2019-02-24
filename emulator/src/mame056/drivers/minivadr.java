/**
 * ported to v0.56
 * ported to v0.37b7
 */
package mame056.drivers;

import static arcadeflex056.fucPtr.*;

import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.inptportH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.memoryH.*;
import static mame056.inptport.*;
import static mame056.drawgfxH.*;
import static mame056.vidhrdw.minivadr.*;
import static mame056.vidhrdw.generic.*;

public class minivadr {

    public static Memory_ReadAddress readmem[] = {
        new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_ReadAddress(0x0000, 0x1fff, MRA_ROM),
        new Memory_ReadAddress(0xa000, 0xbfff, MRA_RAM),
        new Memory_ReadAddress(0xe008, 0xe008, input_port_0_r),
        new Memory_ReadAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_WriteAddress writemem[] = {
        new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_WriteAddress(0x0000, 0x1fff, MWA_ROM),
        new Memory_WriteAddress(0xa000, 0xbfff, minivadr_videoram_w, videoram, videoram_size),
        new Memory_WriteAddress(0xe008, 0xe008, MWA_NOP), // ???
        new Memory_WriteAddress(MEMPORT_MARKER, 0)
    };

    static InputPortPtr input_ports_minivadr = new InputPortPtr() {
        public void handler() {
            PORT_START();
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_BUTTON1);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_COIN1);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN);
            INPUT_PORTS_END();
        }
    };

    static MachineDriver machine_driver_minivadr = new MachineDriver(
            /* basic machine hardware */
            new MachineCPU[]{
                new MachineCPU(
                        CPU_Z80,
                        24000000 / 6, /* 4 MHz ? */
                        readmem, writemem, null, null,
                        interrupt, 1
                )
            },
            60, DEFAULT_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
            1, /* single CPU, no need for interleaving */
            null,
            /* video hardware */
            256, 256, new rectangle(0, 256 - 1, 16, 240 - 1),
            null,
            2, 0,
            minivadr_init_palette,
            VIDEO_TYPE_RASTER | VIDEO_SUPPORTS_DIRTY,
            null,
            null,
            null,
            minivadr_vh_screenrefresh,
            /* sound hardware */
            0, 0, 0, 0, null
    );

    /**
     * *************************************************************************
     *
     * Game driver(s)
     *
     **************************************************************************
     */
    static RomLoadPtr rom_minivadr = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("d26-01.bin", 0x0000, 0x2000, 0xa96c823d);
            ROM_END();
        }
    };

    public static GameDriver driver_minivadr = new GameDriver("1990", "minivadr", "minivadr.java", rom_minivadr, null, machine_driver_minivadr, input_ports_minivadr, null, ROT0, "Taito Corporation", "Minivader");
}
