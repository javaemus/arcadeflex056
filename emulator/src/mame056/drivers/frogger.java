/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.drivers;

import static arcadeflex056.fucPtr.*;

import static mame056.drivers.galaxian.*;

import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.inptportH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.memoryH.*;
import static mame056.inptport.*;
import static mame056.drawgfxH.*;
import static mame056.sndintrfH.*;
import static mame056.sndintrf.*;
import static mame056.sound.ay8910.*;
import static mame056.sound.ay8910H.*;
import static mame056.sound.mixerH.*;

import static mame056.machine.scramble.*;

import static mame056.sndhrdw.scramble.*;

import static mame056.vidhrdw.galaxian.*;

public class frogger {

    public static WriteHandlerPtr frogger_coin_counter_0_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            coin_counter_w.handler(0, data);
        }
    };

    public static WriteHandlerPtr frogger_coin_counter_1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            coin_counter_w.handler(1, data);
        }
    };

    public static Memory_ReadAddress readmem[] = {
        new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_ReadAddress(0x0000, 0x3fff, MRA_ROM),
        new Memory_ReadAddress(0x8000, 0x87ff, MRA_RAM),
        new Memory_ReadAddress(0x8800, 0x8800, watchdog_reset_r),
        new Memory_ReadAddress(0xa800, 0xabff, MRA_RAM),
        new Memory_ReadAddress(0xb000, 0xb0ff, MRA_RAM),
        new Memory_ReadAddress(0xd000, 0xd007, frogger_ppi8255_1_r),
        new Memory_ReadAddress(0xe000, 0xe007, frogger_ppi8255_0_r),
        new Memory_ReadAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_WriteAddress writemem[] = {
        new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_WriteAddress(0x0000, 0x3fff, MWA_ROM),
        new Memory_WriteAddress(0x8000, 0x87ff, MWA_RAM),
        new Memory_WriteAddress(0xa800, 0xabff, MWA_RAM, galaxian_videoram),
        new Memory_WriteAddress(0xb000, 0xb03f, MWA_RAM, galaxian_attributesram),
        new Memory_WriteAddress(0xb040, 0xb05f, MWA_RAM, galaxian_spriteram, galaxian_spriteram_size),
        new Memory_WriteAddress(0xb060, 0xb0ff, MWA_RAM),
        new Memory_WriteAddress(0xb808, 0xb808, interrupt_enable_w),
        new Memory_WriteAddress(0xb80c, 0xb80c, galaxian_flip_screen_y_w),
        new Memory_WriteAddress(0xb810, 0xb810, galaxian_flip_screen_x_w),
        new Memory_WriteAddress(0xb818, 0xb818, frogger_coin_counter_0_w),
        new Memory_WriteAddress(0xb81c, 0xb81c, frogger_coin_counter_1_w),
        new Memory_WriteAddress(0xd000, 0xd007, frogger_ppi8255_1_w),
        new Memory_WriteAddress(0xe000, 0xe007, frogger_ppi8255_0_w),
        new Memory_WriteAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_ReadAddress froggrmc_readmem[] = {
        new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_ReadAddress(0x0000, 0x3fff, MRA_ROM),
        new Memory_ReadAddress(0x8000, 0x87ff, MRA_RAM),
        new Memory_ReadAddress(0x9000, 0x93ff, MRA_RAM),
        new Memory_ReadAddress(0x9800, 0x98ff, MRA_RAM),
        new Memory_ReadAddress(0xa000, 0xa000, input_port_0_r),
        new Memory_ReadAddress(0xa800, 0xa800, input_port_1_r),
        new Memory_ReadAddress(0xb000, 0xb000, input_port_2_r),
        new Memory_ReadAddress(0xb800, 0xb800, watchdog_reset_r),
        new Memory_ReadAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_WriteAddress froggrmc_writemem[] = {
        new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_WriteAddress(0x0000, 0x3fff, MWA_ROM),
        new Memory_WriteAddress(0x8000, 0x87ff, MWA_RAM),
        new Memory_WriteAddress(0x9000, 0x93ff, MWA_RAM, galaxian_videoram),
        new Memory_WriteAddress(0x9800, 0x983f, MWA_RAM, galaxian_attributesram),
        new Memory_WriteAddress(0x9840, 0x985f, MWA_RAM, galaxian_spriteram, galaxian_spriteram_size),
        new Memory_WriteAddress(0x9860, 0x98ff, MWA_RAM),
        new Memory_WriteAddress(0xa800, 0xa800, soundlatch_w),
        new Memory_WriteAddress(0xb000, 0xb000, interrupt_enable_w),
        new Memory_WriteAddress(0xb001, 0xb001, froggrmc_sh_irqtrigger_w),
        new Memory_WriteAddress(0xb006, 0xb006, galaxian_flip_screen_x_w),
        new Memory_WriteAddress(0xb007, 0xb007, galaxian_flip_screen_y_w),
        new Memory_WriteAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_ReadAddress frogger_sound_readmem[] = {
        new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_ReadAddress(0x0000, 0x1fff, MRA_ROM),
        new Memory_ReadAddress(0x4000, 0x43ff, MRA_RAM),
        new Memory_ReadAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_WriteAddress frogger_sound_writemem[] = {
        new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_WriteAddress(0x0000, 0x1fff, MWA_ROM),
        new Memory_WriteAddress(0x4000, 0x43ff, MWA_RAM),
        new Memory_WriteAddress(0x6000, 0x6fff, frogger_filter_w),
        new Memory_WriteAddress(MEMPORT_MARKER, 0)
    };

    public static IO_ReadPort frogger_sound_readport[] = {
        new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
        new IO_ReadPort(0x40, 0x40, AY8910_read_port_0_r),
        new IO_ReadPort(MEMPORT_MARKER, 0)
    };

    public static IO_WritePort frogger_sound_writeport[] = {
        new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
        new IO_WritePort(0x40, 0x40, AY8910_write_port_0_w),
        new IO_WritePort(0x80, 0x80, AY8910_control_port_0_w),
        new IO_WritePort(MEMPORT_MARKER, 0)
    };

    static InputPortPtr input_ports_frogger = new InputPortPtr() {
        public void handler() {
            PORT_START();
            /* IN0 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);/* 1P shoot2 - unused */
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_SERVICE1);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);/* 1P shoot1 - unused */
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_COIN2);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_COIN1);

            PORT_START();
            /* IN1 */
            PORT_DIPNAME(0x03, 0x00, DEF_STR("Lives"));
            PORT_DIPSETTING(0x00, "3");
            PORT_DIPSETTING(0x01, "5");
            PORT_DIPSETTING(0x02, "7");
            PORT_BITX(0, 0x03, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "256", IP_KEY_NONE, IP_JOY_NONE);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);/* 2P shoot2 - unused */
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);/* 2P shoot1 - unused */
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_START2);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_START1);

            PORT_START();
            /* IN2 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY | IPF_COCKTAIL);
            PORT_DIPNAME(0x06, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x02, "A 2/1 B 2/1 C 2/1");
            PORT_DIPSETTING(0x04, "A 2/1 B 1/3 C 2/1");
            PORT_DIPSETTING(0x00, "A 1/1 B 1/1 C 1/1");
            PORT_DIPSETTING(0x06, "A 1/1 B 1/6 C 1/1");
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Cabinet"));
            PORT_DIPSETTING(0x00, DEF_STR("Upright"));
            PORT_DIPSETTING(0x08, DEF_STR("Cocktail"));
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNUSED);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNUSED);
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_froggrmc = new InputPortPtr() {
        public void handler() {
            PORT_START();
            /* IN0 */
            PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_COIN1);
            PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_COIN2);
            PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_4WAY);
            PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY);
            PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_4WAY);
            PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_4WAY);
            PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN);
            PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_SERVICE1);

            PORT_START();
            /* IN1 */
            PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_START1);
            PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_START2);
            PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_4WAY | IPF_COCKTAIL);
            PORT_DIPNAME(0xc0, 0xc0, DEF_STR("Lives"));
            PORT_DIPSETTING(0xc0, "3");
            PORT_DIPSETTING(0x80, "5");
            PORT_DIPSETTING(0x40, "7");
            PORT_BITX(0, 0x00, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "256", IP_KEY_NONE, IP_JOY_NONE);

            PORT_START();
            /* IN2 */
            PORT_DIPNAME(0x01, 0x01, DEF_STR("Cabinet"));
            PORT_DIPSETTING(0x01, DEF_STR("Upright"));
            PORT_DIPSETTING(0x00, DEF_STR("Cocktail"));
            PORT_DIPNAME(0x06, 0x06, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x02, "A 2/1 B 2/1 C 2/1");
            PORT_DIPSETTING(0x04, "A 2/1 B 1/3 C 2/1");
            PORT_DIPSETTING(0x06, "A 1/1 B 1/1 C 1/1");
            PORT_DIPSETTING(0x00, "A 1/1 B 1/6 C 1/1");
            PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN);
            PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN);
            PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN);
            PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN);
            PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN);
            INPUT_PORTS_END();
        }
    };

    static AY8910interface frogger_ay8910_interface = new AY8910interface(
            1, /* 1 chip */
            14318000 / 8, /* 1.78975 MHz */
            new int[]{MIXERG(80, MIXER_GAIN_2x, MIXER_PAN_CENTER)},
            new ReadHandlerPtr[]{soundlatch_r},
            new ReadHandlerPtr[]{frogger_portB_r},
            new WriteHandlerPtr[]{null},
            new WriteHandlerPtr[]{null}
    );

    static MachineDriver machine_driver_frogger = new MachineDriver(
            /* basic machine hardware */
            new MachineCPU[]{
                new MachineCPU(
                        CPU_Z80,
                        18432000 / 6, /* 3.072 MHz */
                        readmem, writemem, null, null,
                        nmi_interrupt, 1
                ),
                new MachineCPU(
                        CPU_Z80 | CPU_AUDIO_CPU,
                        14318000 / 8, /* 1.78975 MHz */
                        frogger_sound_readmem, frogger_sound_writemem, frogger_sound_readport, frogger_sound_writeport,
                        ignore_interrupt, 1 /* interrupts are triggered by the main CPU */
                )
            },
            16000 / 132 / 2, 2500, /* frames per second, vblank duration */
            1, /* 1 CPU slice per frame - interleaving is forced when a sound command is written */
            scramble_init_machine,
            /* video hardware */
            32 * 8, 32 * 8, new rectangle(0 * 8, 32 * 8 - 1, 2 * 8, 30 * 8 - 1),
            galaxian_gfxdecodeinfo,
            32 + 64 + 2 + 1, 8 * 4, /* 32 for characters, 64 for stars, 2 for bullets, 1 for background */
            frogger_vh_convert_color_prom,
            VIDEO_TYPE_RASTER,
            null,
            frogger_vh_start,
            null,
            galaxian_vh_screenrefresh,
            /* sound hardware */
            0, 0, 0, 0,
            new MachineSound[]{
                new MachineSound(
                        SOUND_AY8910,
                        frogger_ay8910_interface
                )
            }
    );

    static MachineDriver machine_driver_froggrmc = new MachineDriver(
            /* basic machine hardware */
            new MachineCPU[]{
                new MachineCPU(
                        CPU_Z80,
                        18432000 / 6, /* 3.072 MHz */
                        froggrmc_readmem, froggrmc_writemem, null, null,
                        nmi_interrupt, 1
                ),
                new MachineCPU(
                        CPU_Z80 | CPU_AUDIO_CPU,
                        14318000 / 8, /* 1.78975 MHz */
                        frogger_sound_readmem, frogger_sound_writemem, frogger_sound_readport, frogger_sound_writeport,
                        ignore_interrupt, 1 /* interrupts are triggered by the main CPU */
                )
            },
            16000 / 132 / 2, 2500, /* frames per second, vblank duration */
            1, /* 1 CPU slice per frame - interleaving is forced when a sound command is written */
            scramble_init_machine,
            /* video hardware */
            32 * 8, 32 * 8, new rectangle(0 * 8, 32 * 8 - 1, 2 * 8, 30 * 8 - 1),
            galaxian_gfxdecodeinfo,
            32 + 64 + 2 + 1, 8 * 4, /* 32 for characters, 64 for stars, 2 for bullets, 1 for background */
            frogger_vh_convert_color_prom,
            VIDEO_TYPE_RASTER,
            null,
            froggrmc_vh_start,
            null,
            galaxian_vh_screenrefresh,
            /* sound hardware */
            0, 0, 0, 0,
            new MachineSound[]{
                new MachineSound(
                        SOUND_AY8910,
                        frogger_ay8910_interface
                )
            }
    );

    /**
     * *************************************************************************
     *
     * Game driver(s)
     *
     **************************************************************************
     */
    static RomLoadPtr rom_frogger = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("frogger.26", 0x0000, 0x1000, 0x597696d6);
            ROM_LOAD("frogger.27", 0x1000, 0x1000, 0xb6e6fcc3);
            ROM_LOAD("frsm3.7", 0x2000, 0x1000, 0xaca22ae0);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 64k for the audio CPU */
            ROM_LOAD("frogger.608", 0x0000, 0x0800, 0xe8ab0256);
            ROM_LOAD("frogger.609", 0x0800, 0x0800, 0x7380a48f);
            ROM_LOAD("frogger.610", 0x1000, 0x0800, 0x31d7eb27);

            ROM_REGION(0x1000, REGION_GFX1, ROMREGION_DISPOSE);
            ROM_LOAD("frogger.607", 0x0000, 0x0800, 0x05f7d883);
            ROM_LOAD("frogger.606", 0x0800, 0x0800, 0xf524ee30);

            ROM_REGION(0x0020, REGION_PROMS, 0);
            ROM_LOAD("pr-91.6l", 0x0000, 0x0020, 0x413703bf);
            ROM_END();
        }
    };

    static RomLoadPtr rom_frogseg1 = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("frogger.26", 0x0000, 0x1000, 0x597696d6);
            ROM_LOAD("frogger.27", 0x1000, 0x1000, 0xb6e6fcc3);
            ROM_LOAD("frogger.34", 0x2000, 0x1000, 0xed866bab);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 64k for the audio CPU */
            ROM_LOAD("frogger.608", 0x0000, 0x0800, 0xe8ab0256);
            ROM_LOAD("frogger.609", 0x0800, 0x0800, 0x7380a48f);
            ROM_LOAD("frogger.610", 0x1000, 0x0800, 0x31d7eb27);

            ROM_REGION(0x1000, REGION_GFX1, ROMREGION_DISPOSE);
            ROM_LOAD("frogger.607", 0x0000, 0x0800, 0x05f7d883);
            ROM_LOAD("frogger.606", 0x0800, 0x0800, 0xf524ee30);

            ROM_REGION(0x0020, REGION_PROMS, 0);
            ROM_LOAD("pr-91.6l", 0x0000, 0x0020, 0x413703bf);
            ROM_END();
        }
    };

    static RomLoadPtr rom_frogseg2 = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("frogger.ic5", 0x0000, 0x1000, 0xefab0c79);
            ROM_LOAD("frogger.ic6", 0x1000, 0x1000, 0xaeca9c13);
            ROM_LOAD("frogger.ic7", 0x2000, 0x1000, 0xdd251066);
            ROM_LOAD("frogger.ic8", 0x3000, 0x1000, 0xbf293a02);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 64k for the audio CPU */
            ROM_LOAD("frogger.608", 0x0000, 0x0800, 0xe8ab0256);
            ROM_LOAD("frogger.609", 0x0800, 0x0800, 0x7380a48f);
            ROM_LOAD("frogger.610", 0x1000, 0x0800, 0x31d7eb27);

            ROM_REGION(0x1000, REGION_GFX1, ROMREGION_DISPOSE);
            ROM_LOAD("frogger.607", 0x0000, 0x0800, 0x05f7d883);
            ROM_LOAD("frogger.606", 0x0800, 0x0800, 0xf524ee30);

            ROM_REGION(0x0020, REGION_PROMS, 0);
            ROM_LOAD("pr-91.6l", 0x0000, 0x0020, 0x413703bf);
            ROM_END();
        }
    };

    static RomLoadPtr rom_froggrmc = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("epr-1031.15", 0x0000, 0x1000, 0x4b7c8d11);
            ROM_LOAD("epr-1032.16", 0x1000, 0x1000, 0xac00b9d9);
            ROM_LOAD("epr-1033.33", 0x2000, 0x1000, 0xbc1d6fbc);
            ROM_LOAD("epr-1034.34", 0x3000, 0x1000, 0x9efe7399);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 64k for the audio CPU */
            ROM_LOAD("epr-1082.42", 0x0000, 0x1000, 0x802843c2);
            ROM_LOAD("epr-1035.43", 0x1000, 0x0800, 0x14e74148);

            ROM_REGION(0x1000, REGION_GFX1, ROMREGION_DISPOSE);
            ROM_LOAD("frogger.607", 0x0000, 0x0800, 0x05f7d883);
            ROM_LOAD("epr-1036.1k", 0x0800, 0x0800, 0x658745f8);

            ROM_REGION(0x0020, REGION_PROMS, 0);
            ROM_LOAD("pr-91.6l", 0x0000, 0x0020, 0x413703bf);
            ROM_END();
        }
    };

    public static GameDriver driver_frogger = new GameDriver("1981", "frogger", "frogger.java", rom_frogger, null, machine_driver_frogger, input_ports_frogger, init_frogger, ROT90, "Konami", "Frogger");
    public static GameDriver driver_frogseg1 = new GameDriver("1981", "frogseg1", "frogger.java", rom_frogseg1, driver_frogger, machine_driver_frogger, input_ports_frogger, init_frogger, ROT90, "[Konami] (Sega license)", "Frogger (Sega set 1)");
    public static GameDriver driver_frogseg2 = new GameDriver("1981", "frogseg2", "frogger.java", rom_frogseg2, driver_frogger, machine_driver_frogger, input_ports_frogger, init_frogger, ROT90, "[Konami] (Sega license)", "Frogger (Sega set 2)");
    public static GameDriver driver_froggrmc = new GameDriver("1981", "froggrmc", "frogger.java", rom_froggrmc, driver_frogger, machine_driver_froggrmc, input_ports_froggrmc, init_froggers, ROT90, "bootleg?", "Frogger (modified Moon Cresta hardware)");
}
