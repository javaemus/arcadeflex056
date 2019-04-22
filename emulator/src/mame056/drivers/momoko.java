/**
 * ported to v0.56
 */
/**
 * Changelog
 * ---------
 * 22/04/2019 - added momoko driver (shadow)
 */
package mame056.drivers;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;

import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.inptportH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.common.*;
import static mame056.driverH.*;
import static mame056.memoryH.*;
import static mame056.inptport.*;
import static mame056.drawgfxH.*;
import static mame056.sndintrfH.*;
import static mame056.sndintrf.*;
import static mame056.palette.*;

import static mame056.sound._2203intf.*;
import static mame056.sound._2203intfH.*;

import static mame056.vidhrdw.generic.*;
import static mame056.vidhrdw.momoko.*;

public class momoko {

    public static WriteHandlerPtr momoko_bg_read_bank_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            UBytePtr BG_MAP = memory_region(REGION_USER1);
            int bank_address = (data & 0x1f) * 0x1000;
            cpu_setbank(1, new UBytePtr(BG_MAP, bank_address));
        }
    };

    /**
     * *************************************************************************
     */
    public static Memory_ReadAddress readmem[] = {
        new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_ReadAddress(0x0000, 0xbfff, MRA_ROM),
        new Memory_ReadAddress(0xc000, 0xcfff, MRA_RAM),
        new Memory_ReadAddress(0xd064, 0xd0ff, MRA_RAM), /* sprite ram */
        new Memory_ReadAddress(0xd400, 0xd400, input_port_0_r),
        new Memory_ReadAddress(0xd402, 0xd402, input_port_1_r),
        new Memory_ReadAddress(0xd406, 0xd406, input_port_2_r),
        new Memory_ReadAddress(0xd407, 0xd407, input_port_3_r),
        new Memory_ReadAddress(0xd800, 0xdbff, paletteram_r),
        new Memory_ReadAddress(0xe000, 0xe3ff, MRA_RAM), /* text */
        new Memory_ReadAddress(0xf000, 0xffff, MRA_BANK1),
        new Memory_ReadAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_WriteAddress writemem[] = {
        new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_WriteAddress(0x0000, 0xbfff, MWA_ROM),
        new Memory_WriteAddress(0xc000, 0xcfff, MWA_RAM),
        new Memory_WriteAddress(0xd064, 0xd0ff, MWA_RAM, spriteram, spriteram_size),
        new Memory_WriteAddress(0xd400, 0xd400, MWA_NOP), /* interrupt ack? */
        new Memory_WriteAddress(0xd402, 0xd402, momoko_flipscreen_w),
        new Memory_WriteAddress(0xd404, 0xd404, watchdog_reset_w),
        new Memory_WriteAddress(0xd406, 0xd406, soundlatch_w),
        new Memory_WriteAddress(0xd800, 0xdbff, paletteram_xxxxRRRRGGGGBBBB_swap_w, paletteram),
        new Memory_WriteAddress(0xdc00, 0xdc00, momoko_fg_scrolly_w),
        new Memory_WriteAddress(0xdc01, 0xdc01, momoko_fg_scrollx_w),
        new Memory_WriteAddress(0xdc02, 0xdc02, momoko_fg_select_w),
        new Memory_WriteAddress(0xe000, 0xe3ff, videoram_w, videoram, videoram_size),
        new Memory_WriteAddress(0xe800, 0xe800, momoko_text_scrolly_w),
        new Memory_WriteAddress(0xe801, 0xe801, momoko_text_mode_w),
        new Memory_WriteAddress(0xf000, 0xf001, momoko_bg_scrolly_w, momoko_bg_scrolly),
        new Memory_WriteAddress(0xf002, 0xf003, momoko_bg_scrollx_w, momoko_bg_scrollx),
        new Memory_WriteAddress(0xf004, 0xf004, momoko_bg_read_bank_w),
        new Memory_WriteAddress(0xf006, 0xf006, momoko_bg_select_w),
        new Memory_WriteAddress(0xf007, 0xf007, momoko_bg_priority_w),
        new Memory_WriteAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_ReadAddress readmem_sound[] = {
        new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_ReadAddress(0x0000, 0x7fff, MRA_ROM),
        new Memory_ReadAddress(0x8000, 0x87ff, MRA_RAM),
        new Memory_ReadAddress(0xa000, 0xa000, YM2203_status_port_0_r),
        new Memory_ReadAddress(0xa001, 0xa001, YM2203_read_port_0_r),
        new Memory_ReadAddress(0xc000, 0xc000, YM2203_status_port_1_r),
        new Memory_ReadAddress(0xc001, 0xc001, YM2203_read_port_1_r),
        new Memory_ReadAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_WriteAddress writemem_sound[] = {
        new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_WriteAddress(0x0000, 0x7fff, MWA_ROM),
        new Memory_WriteAddress(0x8000, 0x87ff, MWA_RAM),
        new Memory_WriteAddress(0x9000, 0x9000, MWA_NOP), /* unknown */
        new Memory_WriteAddress(0xa000, 0xa000, YM2203_control_port_0_w),
        new Memory_WriteAddress(0xa001, 0xa001, YM2203_write_port_0_w),
        new Memory_WriteAddress(0xb000, 0xb000, MWA_NOP), /* unknown */
        new Memory_WriteAddress(0xc000, 0xc000, YM2203_control_port_1_w),
        new Memory_WriteAddress(0xc001, 0xc001, YM2203_write_port_1_w),
        new Memory_WriteAddress(MEMPORT_MARKER, 0)
    };

    /**
     * *************************************************************************
     */
    static InputPortPtr input_ports_momoko = new InputPortPtr() {
        public void handler() {
            PORT_START();
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_START2);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_START1);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_BUTTON2);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_BUTTON1);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY);
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY);

            PORT_START();
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_COCKTAIL);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_COCKTAIL);
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_COCKTAIL);

            PORT_START();
            /* dsw0 */
            PORT_DIPNAME(0x03, 0x03, DEF_STR("Lives"));
            PORT_DIPSETTING(0x03, "3");
            PORT_DIPSETTING(0x02, "4");
            PORT_DIPSETTING(0x01, "5");
            PORT_BITX(0, 0x00, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "256", IP_KEY_NONE, IP_JOY_NONE);
            PORT_DIPNAME(0x1c, 0x1c, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x10, DEF_STR("5C_1C"));
            PORT_DIPSETTING(0x14, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x18, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x1c, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x0c, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x04, DEF_STR("2C_5C"));
            PORT_DIPSETTING(0x08, DEF_STR("1C_5C"));
            PORT_DIPSETTING(0x00, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x60, 0x60, DEF_STR("Difficulty"));
            PORT_DIPSETTING(0x40, "Easy");
            PORT_DIPSETTING(0x60, "Normal");
            PORT_DIPSETTING(0x20, "Difficult");
            PORT_DIPSETTING(0x00, "Very difficult");
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_COIN1);

            PORT_START();
            /* dsw1 */
            PORT_DIPNAME(0x03, 0x03, DEF_STR("Bonus_Life"));
            PORT_DIPSETTING(0x01, "20000");
            PORT_DIPSETTING(0x03, "30000");
            PORT_DIPSETTING(0x02, "50000");
            PORT_DIPSETTING(0x00, "100000");
            PORT_DIPNAME(0x04, 0x04, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x04, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x08, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x08, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Cabinet"));
            PORT_DIPSETTING(0x00, DEF_STR("Upright"));
            PORT_DIPSETTING(0x10, DEF_STR("Cocktail"));
            PORT_DIPNAME(0x20, 0x20, DEF_STR("Demo_Sounds"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x40, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x40, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x80, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x80, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));

            PORT_START();
            /* fake */
            PORT_DIPNAME(0x01, 0x00, DEF_STR("Flip_Screen"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));

            INPUT_PORTS_END();
        }
    };

    /**
     * *************************************************************************
     */
    static GfxLayout charlayout = new GfxLayout(
            8, 8, /* 8*8 characters */
            256, /* 256 characters */
            2, /* 2 bits per pixel */
            new int[]{4, 0},
            new int[]{0, 1, 2, 3, 256 * 8 * 8 + 0, 256 * 8 * 8 + 1, 256 * 8 * 8 + 2, 256 * 8 * 8 + 3},
            new int[]{8 * 0, 8 * 1, 8 * 2, 8 * 3, 8 * 4, 8 * 5, 8 * 6, 8 * 7},
            8 * 8
    );

    static GfxLayout spritelayout = new GfxLayout(
            8, 16, /* 8*16 characters */
            2048 - 128, /* 1024 sprites ( ccc 0ccccccc ) */
            4, /* 4 bits per pixel */
            new int[]{12, 8, 4, 0},
            new int[]{0, 1, 2, 3, 4096 * 8 + 0, 4096 * 8 + 1, 4096 * 8 + 2, 4096 * 8 + 3},
            new int[]{0, 1 * 16, 2 * 16, 3 * 16, 4 * 16, 5 * 16, 6 * 16, 7 * 16,
                8 * 16, 9 * 16, 10 * 16, 11 * 16, 12 * 16, 13 * 16, 14 * 16, 15 * 16},
            8 * 32
    );

    static GfxLayout tilelayout = new GfxLayout(
            8, 8, /* 8*8 characters */
            8192 - 256, /* 4096 tiles ( cccc0 cccccccc ) */
            4, /* 4 bits per pixel */
            new int[]{4, 0, 12, 8},
            new int[]{0, 1, 2, 3, 4096 * 8 + 0, 4096 * 8 + 1, 4096 * 8 + 2, 4096 * 8 + 3},
            new int[]{0, 1 * 16, 2 * 16, 3 * 16, 4 * 16, 5 * 16, 6 * 16, 7 * 16},
            8 * 16
    );

    static GfxLayout charlayout1 = new GfxLayout(
            8, 1, /* 8*1 characters */
            256 * 8, /* 2048 characters */
            2, /* 2 bits per pixel */
            new int[]{4, 0},
            new int[]{0, 1, 2, 3, 256 * 8 * 8 + 0, 256 * 8 * 8 + 1, 256 * 8 * 8 + 2, 256 * 8 * 8 + 3},
            new int[]{8 * 0},
            8 * 1
    );

    static GfxDecodeInfo gfxdecodeinfo[]
            = {
                new GfxDecodeInfo(REGION_GFX1, 0x0000, charlayout1, 0, 24), /* TEXT */
                new GfxDecodeInfo(REGION_GFX2, 0x0000, tilelayout, 256, 16), /* BG */
                new GfxDecodeInfo(REGION_GFX3, 0x0000, charlayout, 0, 1), /* FG */
                new GfxDecodeInfo(REGION_GFX4, 0x0000, spritelayout, 128, 8), /* sprite */
                new GfxDecodeInfo(-1) /* end of array */};

    /**
     * *************************************************************************
     */
    static YM2203interface ym2203_interface = new YM2203interface(
            2, /* 2 chips */
            1250000, /* 1.25 MHz */
            new int[]{YM2203_VOL(40, 15), YM2203_VOL(40, 15)},
            new ReadHandlerPtr[]{null, soundlatch_r},
            new ReadHandlerPtr[]{null, null},
            new WriteHandlerPtr[]{null, null},
            new WriteHandlerPtr[]{null, null}
    );

    static MachineDriver machine_driver_momoko = new MachineDriver(
            new MachineCPU[]{
                new MachineCPU(
                        CPU_Z80,
                        5000000, /* 5.0MHz */
                        readmem, writemem, null, null,
                        interrupt, 1
                ),
                new MachineCPU(
                        CPU_Z80 | CPU_AUDIO_CPU,
                        2500000, /* 2.5MHz */
                        readmem_sound, writemem_sound, null, null,
                        ignore_interrupt, 0
                ),},
            60, DEFAULT_REAL_60HZ_VBLANK_DURATION,
            1,
            null,
            32 * 8, 32 * 8, new rectangle(1 * 8, 31 * 8 - 1, 2 * 8, 29 * 8 - 1),
            gfxdecodeinfo,
            512, 0,
            null,
            VIDEO_TYPE_RASTER,
            null,
            generic_vh_start,
            generic_vh_stop,
            momoko_vh_screenrefresh,
            0, 0, 0, 0,
            new MachineSound[]{
                new MachineSound(
                        SOUND_YM2203,
                        ym2203_interface
                )
            }
    );

    /**
     * *************************************************************************
     */
    static RomLoadPtr rom_momoko = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* main CPU */
            ROM_LOAD("momoko03.bin", 0x0000, 0x8000, 0x386e26ed);
            ROM_LOAD("momoko02.bin", 0x8000, 0x4000, 0x4255e351);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* sound CPU */
            ROM_LOAD("momoko01.bin", 0x0000, 0x8000, 0xe8a6673c);

            ROM_REGION(0x2000, REGION_GFX1, ROMREGION_DISPOSE);/* text */
            ROM_LOAD("momoko13.bin", 0x0000, 0x2000, 0x2745cf5a);

            ROM_REGION(0x2000, REGION_GFX3, ROMREGION_DISPOSE);/* FG */
            ROM_LOAD("momoko14.bin", 0x0000, 0x2000, 0xcfccca05);

            ROM_REGION(0x10000, REGION_GFX4, ROMREGION_DISPOSE);/* sprite */
            ROM_LOAD16_BYTE("momoko16.bin", 0x0000, 0x8000, 0xfc6876fc);
            ROM_LOAD16_BYTE("momoko17.bin", 0x0001, 0x8000, 0x45dc0247);

            ROM_REGION(0x20000, REGION_GFX2, 0);/* BG */
            ROM_LOAD16_BYTE("momoko09.bin", 0x00000, 0x8000, 0x9f5847c7);
            ROM_LOAD16_BYTE("momoko11.bin", 0x00001, 0x8000, 0x9c9fbd43);
            ROM_LOAD16_BYTE("momoko10.bin", 0x10000, 0x8000, 0xae17e74b);
            ROM_LOAD16_BYTE("momoko12.bin", 0x10001, 0x8000, 0x1e29c9c4);

            ROM_REGION(0x20000, REGION_USER1, 0);/* BG map */
            ROM_LOAD("momoko04.bin", 0x0000, 0x8000, 0x3ab3c2c3);
            ROM_LOAD("momoko05.bin", 0x8000, 0x8000, 0x757cdd2b);
            ROM_LOAD("momoko06.bin", 0x10000, 0x8000, 0x20cacf8b);
            ROM_LOAD("momoko07.bin", 0x18000, 0x8000, 0xb94b38db);

            ROM_REGION(0x2000, REGION_USER2, 0);/* BG color/priority table */
            ROM_LOAD("momoko08.bin", 0x0000, 0x2000, 0x69b41702);

            ROM_REGION(0x4000, REGION_USER3, 0);/* FG map */
            ROM_LOAD("momoko15.bin", 0x0000, 0x4000, 0x8028f806);

            ROM_REGION(0x0120, REGION_PROMS, 0);/* TEXT color */
            ROM_LOAD("momoko-c.bin", 0x0000, 0x0100, 0xf35ccae0);
            ROM_LOAD("momoko-b.bin", 0x0100, 0x0020, 0x427b0e5c);
            ROM_END();
        }
    };

    public static GameDriver driver_momoko = new GameDriver("1986", "momoko", "momoko.java", rom_momoko, null, machine_driver_momoko, input_ports_momoko, null, ROT0, "Jaleco", "Momoko 120%");
}
