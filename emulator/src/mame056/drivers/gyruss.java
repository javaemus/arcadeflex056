/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
/**
 * Changelog
 * ---------
 * 20/04/2019 - added gyruss driver (shadow)
 */
package mame056.drivers;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;
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

import static mame056.cpu.i8039.i8039H.*;

import static mame056.machine.konami.*;

import static mame056.sndhrdw.gyruss.*;

import static mame056.sound.mixerH.*;
import static mame056.sound.ay8910.*;
import static mame056.sound.ay8910H.*;
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;

import static mame056.vidhrdw.generic.*;
import static mame056.vidhrdw.gyruss.*;

public class gyruss {

    public static UBytePtr gyruss_sharedram = new UBytePtr();

    public static ReadHandlerPtr gyruss_sharedram_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return gyruss_sharedram.read(offset);
        }
    };

    public static WriteHandlerPtr gyruss_sharedram_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            gyruss_sharedram.write(offset, data);
        }
    };

    public static Memory_ReadAddress readmem[] = {
        new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_ReadAddress(0x0000, 0x7fff, MRA_ROM),
        new Memory_ReadAddress(0x8000, 0x87ff, MRA_RAM),
        new Memory_ReadAddress(0x9000, 0x9fff, MRA_RAM),
        new Memory_ReadAddress(0xa000, 0xa7ff, gyruss_sharedram_r),
        new Memory_ReadAddress(0xc000, 0xc000, input_port_4_r), /* DSW1 */
        new Memory_ReadAddress(0xc080, 0xc080, input_port_0_r), /* IN0 */
        new Memory_ReadAddress(0xc0a0, 0xc0a0, input_port_1_r), /* IN1 */
        new Memory_ReadAddress(0xc0c0, 0xc0c0, input_port_2_r), /* IN2 */
        new Memory_ReadAddress(0xc0e0, 0xc0e0, input_port_3_r), /* DSW0 */
        new Memory_ReadAddress(0xc100, 0xc100, input_port_5_r), /* DSW2 */
        new Memory_ReadAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_WriteAddress writemem[] = {
        new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_WriteAddress(0x0000, 0x7fff, MWA_ROM), /* rom space+1        */
        new Memory_WriteAddress(0x8000, 0x83ff, colorram_w, colorram),
        new Memory_WriteAddress(0x8400, 0x87ff, videoram_w, videoram, videoram_size),
        new Memory_WriteAddress(0x9000, 0x9fff, MWA_RAM),
        new Memory_WriteAddress(0xa000, 0xa7ff, gyruss_sharedram_w, gyruss_sharedram),
        new Memory_WriteAddress(0xc000, 0xc000, MWA_NOP), /* watchdog reset */
        new Memory_WriteAddress(0xc080, 0xc080, gyruss_sh_irqtrigger_w),
        new Memory_WriteAddress(0xc100, 0xc100, soundlatch_w), /* command to soundb  */
        new Memory_WriteAddress(0xc180, 0xc180, interrupt_enable_w), /* NMI enable         */
        new Memory_WriteAddress(0xc185, 0xc185, gyruss_flipscreen_w),
        new Memory_WriteAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_ReadAddress m6809_readmem[] = {
        new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_ReadAddress(0x0000, 0x0000, gyruss_scanline_r),
        new Memory_ReadAddress(0x4000, 0x47ff, MRA_RAM),
        new Memory_ReadAddress(0x6000, 0x67ff, gyruss_sharedram_r),
        new Memory_ReadAddress(0xe000, 0xffff, MRA_ROM),
        new Memory_ReadAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_WriteAddress m6809_writemem[] = {
        new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_WriteAddress(0x2000, 0x2000, interrupt_enable_w),
        new Memory_WriteAddress(0x4000, 0x47ff, MWA_RAM),
        new Memory_WriteAddress(0x4040, 0x40ff, MWA_RAM, spriteram, spriteram_size),
        new Memory_WriteAddress(0x6000, 0x67ff, gyruss_sharedram_w),
        new Memory_WriteAddress(0xe000, 0xffff, MWA_ROM),
        new Memory_WriteAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_ReadAddress sound_readmem[] = {
        new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_ReadAddress(0x0000, 0x5fff, MRA_ROM), /* rom soundboard     */
        new Memory_ReadAddress(0x6000, 0x63ff, MRA_RAM), /* ram soundboard     */
        new Memory_ReadAddress(0x8000, 0x8000, soundlatch_r),
        new Memory_ReadAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_WriteAddress sound_writemem[] = {
        new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_WriteAddress(0x0000, 0x5fff, MWA_ROM), /* rom soundboard     */
        new Memory_WriteAddress(0x6000, 0x63ff, MWA_RAM), /* ram soundboard     */
        new Memory_WriteAddress(MEMPORT_MARKER, 0)
    };

    public static IO_ReadPort sound_readport[] = {
        new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
        new IO_ReadPort(0x01, 0x01, AY8910_read_port_0_r),
        new IO_ReadPort(0x05, 0x05, AY8910_read_port_1_r),
        new IO_ReadPort(0x09, 0x09, AY8910_read_port_2_r),
        new IO_ReadPort(0x0d, 0x0d, AY8910_read_port_3_r),
        new IO_ReadPort(0x11, 0x11, AY8910_read_port_4_r),
        new IO_ReadPort(MEMPORT_MARKER, 0)
    };

    public static IO_WritePort sound_writeport[] = {
        new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
        new IO_WritePort(0x00, 0x00, AY8910_control_port_0_w),
        new IO_WritePort(0x02, 0x02, AY8910_write_port_0_w),
        new IO_WritePort(0x04, 0x04, AY8910_control_port_1_w),
        new IO_WritePort(0x06, 0x06, AY8910_write_port_1_w),
        new IO_WritePort(0x08, 0x08, AY8910_control_port_2_w),
        new IO_WritePort(0x0a, 0x0a, AY8910_write_port_2_w),
        new IO_WritePort(0x0c, 0x0c, AY8910_control_port_3_w),
        new IO_WritePort(0x0e, 0x0e, AY8910_write_port_3_w),
        new IO_WritePort(0x10, 0x10, AY8910_control_port_4_w),
        new IO_WritePort(0x12, 0x12, AY8910_write_port_4_w),
        new IO_WritePort(0x14, 0x14, gyruss_i8039_irq_w),
        new IO_WritePort(0x18, 0x18, soundlatch2_w),
        new IO_WritePort(MEMPORT_MARKER, 0)
    };

    public static Memory_ReadAddress i8039_readmem[] = {
        new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_ReadAddress(0x0000, 0x0fff, MRA_ROM),
        new Memory_ReadAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_WriteAddress i8039_writemem[] = {
        new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_WriteAddress(0x0000, 0x0fff, MWA_ROM),
        new Memory_WriteAddress(MEMPORT_MARKER, 0)
    };

    public static IO_ReadPort i8039_readport[] = {
        new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
        new IO_ReadPort(0x00, 0xff, soundlatch2_r),
        new IO_ReadPort(MEMPORT_MARKER, 0)
    };

    public static IO_WritePort i8039_writeport[] = {
        new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
        new IO_WritePort(I8039_p1, I8039_p1, DAC_0_data_w),
        new IO_WritePort(I8039_p2, I8039_p2, IOWP_NOP),
        new IO_WritePort(MEMPORT_MARKER, 0)
    };

    static InputPortPtr input_ports_gyruss = new InputPortPtr() {
        public void handler() {
            PORT_START();
            /* IN0 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_COIN1);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_COIN2);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_SERVICE1);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_START1);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_START2);
            PORT_BIT(0xe0, IP_ACTIVE_LOW, IPT_UNUSED);

            PORT_START();
            /* IN1 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_2WAY);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_2WAY);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_BUTTON1);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);/* 1p shoot 2 - unused */
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);/* 2p shoot 3 - unused */
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNUSED);

            PORT_START();
            /* IN2 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY | IPF_COCKTAIL);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_COCKTAIL);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_2WAY | IPF_COCKTAIL);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_2WAY | IPF_COCKTAIL);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);/* 2p shoot 2 - unused */
            PORT_BIT(0xc0, IP_ACTIVE_LOW, IPT_UNUSED);

            PORT_START();
            /* DSW0 */
            PORT_DIPNAME(0xf0, 0xf0, DEF_STR("Coin_B"));
            PORT_DIPSETTING(0x20, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x50, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x80, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x40, DEF_STR("3C_2C"));
            PORT_DIPSETTING(0x10, DEF_STR("4C_3C"));
            PORT_DIPSETTING(0xf0, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x30, DEF_STR("3C_4C"));
            PORT_DIPSETTING(0x70, DEF_STR("2C_3C"));
            PORT_DIPSETTING(0xe0, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x60, DEF_STR("2C_5C"));
            PORT_DIPSETTING(0xd0, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0xc0, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0xb0, DEF_STR("1C_5C"));
            PORT_DIPSETTING(0xa0, DEF_STR("1C_6C"));
            PORT_DIPSETTING(0x90, DEF_STR("1C_7C"));
            PORT_DIPSETTING(0x00, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x0f, 0x0f, DEF_STR("Coin_A"));
            PORT_DIPSETTING(0x02, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x05, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x08, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("3C_2C"));
            PORT_DIPSETTING(0x01, DEF_STR("4C_3C"));
            PORT_DIPSETTING(0x0f, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x03, DEF_STR("3C_4C"));
            PORT_DIPSETTING(0x07, DEF_STR("2C_3C"));
            PORT_DIPSETTING(0x0e, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x06, DEF_STR("2C_5C"));
            PORT_DIPSETTING(0x0d, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x0c, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0x0b, DEF_STR("1C_5C"));
            PORT_DIPSETTING(0x0a, DEF_STR("1C_6C"));
            PORT_DIPSETTING(0x09, DEF_STR("1C_7C"));
            PORT_DIPSETTING(0x00, DEF_STR("Free_Play"));

            PORT_START();
            /* DSW1 */
            PORT_DIPNAME(0x03, 0x03, DEF_STR("Lives"));
            PORT_DIPSETTING(0x03, "3");
            PORT_DIPSETTING(0x02, "4");
            PORT_DIPSETTING(0x01, "5");
            PORT_BITX(0, 0x00, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "255", IP_KEY_NONE, IP_JOY_NONE);
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Cabinet"));
            PORT_DIPSETTING(0x00, DEF_STR("Upright"));
            PORT_DIPSETTING(0x04, DEF_STR("Cocktail"));
            PORT_DIPNAME(0x08, 0x08, DEF_STR("Bonus_Life"));
            PORT_DIPSETTING(0x08, "30000 60000");
            PORT_DIPSETTING(0x00, "40000 70000");
            PORT_DIPNAME(0x70, 0x70, DEF_STR("Difficulty"));
            PORT_DIPSETTING(0x70, "1 (Easiest)");
            PORT_DIPSETTING(0x60, "2");
            PORT_DIPSETTING(0x50, "3");
            PORT_DIPSETTING(0x40, "4");
            PORT_DIPSETTING(0x30, "5 (Average)");
            PORT_DIPSETTING(0x20, "6");
            PORT_DIPSETTING(0x10, "7");
            PORT_DIPSETTING(0x00, "8 (Hardest)");
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Demo_Sounds"));
            PORT_DIPSETTING(0x80, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));

            PORT_START();
            /* DSW2 */
            PORT_DIPNAME(0x01, 0x00, "Demo Music");
            PORT_DIPSETTING(0x01, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            /* other bits probably unused */
            INPUT_PORTS_END();
        }
    };

    /* This is identical to gyruss except for the bonus that has different
	   values */
    static InputPortPtr input_ports_gyrussce = new InputPortPtr() {
        public void handler() {
            PORT_START();
            /* IN0 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_COIN1);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_COIN2);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_SERVICE1);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_START1);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_START2);
            PORT_BIT(0xe0, IP_ACTIVE_LOW, IPT_UNUSED);

            PORT_START();
            /* IN1 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_2WAY);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_2WAY);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_BUTTON1);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);/* 1p shoot 2 - unused */
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);/* 2p shoot 3 - unused */
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNUSED);

            PORT_START();
            /* IN2 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_2WAY | IPF_COCKTAIL);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_COCKTAIL);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_2WAY | IPF_COCKTAIL);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_2WAY | IPF_COCKTAIL);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);/* 2p shoot 2 - unused */
            PORT_BIT(0xc0, IP_ACTIVE_LOW, IPT_UNUSED);

            PORT_START();
            /* DSW0 */
            PORT_DIPNAME(0xf0, 0xf0, DEF_STR("Coin_B"));
            PORT_DIPSETTING(0x20, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x50, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x80, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x40, DEF_STR("3C_2C"));
            PORT_DIPSETTING(0x10, DEF_STR("4C_3C"));
            PORT_DIPSETTING(0xf0, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x30, DEF_STR("3C_4C"));
            PORT_DIPSETTING(0x70, DEF_STR("2C_3C"));
            PORT_DIPSETTING(0xe0, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x60, DEF_STR("2C_5C"));
            PORT_DIPSETTING(0xd0, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0xc0, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0xb0, DEF_STR("1C_5C"));
            PORT_DIPSETTING(0xa0, DEF_STR("1C_6C"));
            PORT_DIPSETTING(0x90, DEF_STR("1C_7C"));
            PORT_DIPSETTING(0x00, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x0f, 0x0f, DEF_STR("Coin_A"));
            PORT_DIPSETTING(0x02, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x05, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x08, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("3C_2C"));
            PORT_DIPSETTING(0x01, DEF_STR("4C_3C"));
            PORT_DIPSETTING(0x0f, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x03, DEF_STR("3C_4C"));
            PORT_DIPSETTING(0x07, DEF_STR("2C_3C"));
            PORT_DIPSETTING(0x0e, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x06, DEF_STR("2C_5C"));
            PORT_DIPSETTING(0x0d, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x0c, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0x0b, DEF_STR("1C_5C"));
            PORT_DIPSETTING(0x0a, DEF_STR("1C_6C"));
            PORT_DIPSETTING(0x09, DEF_STR("1C_7C"));
            PORT_DIPSETTING(0x00, DEF_STR("Free_Play"));

            PORT_START();
            /* DSW1 */
            PORT_DIPNAME(0x03, 0x03, DEF_STR("Lives"));
            PORT_DIPSETTING(0x03, "3");
            PORT_DIPSETTING(0x02, "4");
            PORT_DIPSETTING(0x01, "5");
            PORT_BITX(0, 0x00, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "255", IP_KEY_NONE, IP_JOY_NONE);
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Cabinet"));
            PORT_DIPSETTING(0x00, DEF_STR("Upright"));
            PORT_DIPSETTING(0x04, DEF_STR("Cocktail"));
            PORT_DIPNAME(0x08, 0x08, DEF_STR("Bonus_Life"));
            PORT_DIPSETTING(0x08, "50000 70000");
            PORT_DIPSETTING(0x00, "60000 80000");
            PORT_DIPNAME(0x70, 0x70, DEF_STR("Difficulty"));
            PORT_DIPSETTING(0x70, "1 (Easiest");
            PORT_DIPSETTING(0x60, "2");
            PORT_DIPSETTING(0x50, "3");
            PORT_DIPSETTING(0x40, "4");
            PORT_DIPSETTING(0x30, "5 (Average)");
            PORT_DIPSETTING(0x20, "6");
            PORT_DIPSETTING(0x10, "7");
            PORT_DIPSETTING(0x00, "8 (Hardest)");
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Demo_Sounds"));
            PORT_DIPSETTING(0x80, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));

            PORT_START();
            /* DSW2 */
            PORT_DIPNAME(0x01, 0x00, "Demo Music");
            PORT_DIPSETTING(0x01, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            /* other bits probably unused */
            INPUT_PORTS_END();
        }
    };

    static GfxLayout charlayout = new GfxLayout(
            8, 8, /* 8*8 characters */
            512, /* 512 characters */
            2, /* 2 bits per pixel */
            new int[]{4, 0},
            new int[]{0, 1, 2, 3, 8 * 8 + 0, 8 * 8 + 1, 8 * 8 + 2, 8 * 8 + 3},
            new int[]{0 * 8, 1 * 8, 2 * 8, 3 * 8, 4 * 8, 5 * 8, 6 * 8, 7 * 8},
            16 * 8 /* every char takes 16 consecutive bytes */
    );
    static GfxLayout spritelayout = new GfxLayout(
            8, 16, /* 8*16 sprites */
            256, /* 256 sprites */
            4, /* 4 bits per pixel */
            new int[]{0x4000 * 8 + 4, 0x4000 * 8 + 0, 4, 0},
            new int[]{0, 1, 2, 3, 8 * 8, 8 * 8 + 1, 8 * 8 + 2, 8 * 8 + 3},
            new int[]{0 * 8, 1 * 8, 2 * 8, 3 * 8, 4 * 8, 5 * 8, 6 * 8, 7 * 8,
                32 * 8, 33 * 8, 34 * 8, 35 * 8, 36 * 8, 37 * 8, 38 * 8, 39 * 8},
            64 * 8 /* every sprite takes 64 consecutive bytes */
    );

    static GfxDecodeInfo gfxdecodeinfo[]
            = {
                new GfxDecodeInfo(REGION_GFX1, 0x0000, charlayout, 0, 16),
                new GfxDecodeInfo(REGION_GFX2, 0x0000, spritelayout, 16 * 4, 16), /* upper half */
                new GfxDecodeInfo(REGION_GFX2, 0x0010, spritelayout, 16 * 4, 16), /* lower half */
                new GfxDecodeInfo(-1) /* end of array */};

    static AY8910interface ay8910_interface = new AY8910interface(
            5, /* 5 chips */
            14318180 / 8, /* 1.789772727 MHz */
            new int[]{MIXERG(10, MIXER_GAIN_4x, MIXER_PAN_RIGHT), MIXERG(10, MIXER_GAIN_4x, MIXER_PAN_LEFT),
                MIXERG(20, MIXER_GAIN_4x, MIXER_PAN_RIGHT), MIXERG(20, MIXER_GAIN_4x, MIXER_PAN_RIGHT), MIXERG(20, MIXER_GAIN_4x, MIXER_PAN_LEFT)},
            /*  R       L   |   R       R       L */
            /*   effects    |         music       */
            new ReadHandlerPtr[]{null, null, gyruss_portA_r, null, null},
            new ReadHandlerPtr[]{null, null, null, null, null},
            new WriteHandlerPtr[]{null, null, null, null, null},
            new WriteHandlerPtr[]{gyruss_filter0_w, gyruss_filter1_w, null, null, null}
    );

    static DACinterface dac_interface = new DACinterface(
            1,
            new int[]{MIXER(50, MIXER_PAN_LEFT)}
    );

    static MachineDriver machine_driver_gyruss = new MachineDriver(
            /* basic machine hardware */
            new MachineCPU[]{
                new MachineCPU(
                        CPU_Z80,
                        3072000, /* 3.072 MHz (?) */
                        readmem, writemem, null, null,
                        nmi_interrupt, 1
                ),
                new MachineCPU(
                        CPU_M6809,
                        2000000, /* 2 MHz ??? */
                        m6809_readmem, m6809_writemem, null, null,
                        gyruss_6809_interrupt, 256
                ),
                new MachineCPU(
                        CPU_Z80 | CPU_AUDIO_CPU,
                        14318180 / 4, /* 3.579545 MHz */
                        sound_readmem, sound_writemem, sound_readport, sound_writeport,
                        ignore_interrupt, 1 /* interrupts are triggered by the main CPU */
                ),
                new MachineCPU(
                        CPU_I8039 | CPU_AUDIO_CPU,
                        8000000 / 15, /* 8MHz crystal */
                        i8039_readmem, i8039_writemem, i8039_readport, i8039_writeport,
                        ignore_interrupt, 1
                )
            },
            60, DEFAULT_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
            1, /* 1 CPU slice per frame - interleaving is forced by the 6809 interrupts anyway */
            null,
            /* video hardware */
            32 * 8, 32 * 8, new rectangle(0 * 8, 32 * 8 - 1, 2 * 8, 30 * 8 - 1),
            gfxdecodeinfo,
            32, 16 * 4 + 16 * 16,
            gyruss_vh_convert_color_prom,
            VIDEO_TYPE_RASTER,
            null,
            gyruss_vh_start,
            gyruss_vh_stop,
            gyruss_vh_screenrefresh,
            /* sound hardware */
            SOUND_SUPPORTS_STEREO, 0, 0, 0,
            new MachineSound[]{
                new MachineSound(
                        SOUND_AY8910,
                        ay8910_interface
                ),
                new MachineSound(
                        SOUND_DAC,
                        dac_interface
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
    static RomLoadPtr rom_gyruss = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("gyrussk.1", 0x0000, 0x2000, 0xc673b43d);
            ROM_LOAD("gyrussk.2", 0x2000, 0x2000, 0xa4ec03e4);
            ROM_LOAD("gyrussk.3", 0x4000, 0x2000, 0x27454a98);
            /* the diagnostics ROM would go here */

            ROM_REGION(2 * 0x10000, REGION_CPU2, 0);/* 64k for code + 64k for the decrypted opcodes */
            ROM_LOAD("gyrussk.9", 0xe000, 0x2000, 0x822bf27e);

            ROM_REGION(0x10000, REGION_CPU3, 0);/* 64k for the audio CPU */
            ROM_LOAD("gyrussk.1a", 0x0000, 0x2000, 0xf4ae1c17);
            ROM_LOAD("gyrussk.2a", 0x2000, 0x2000, 0xba498115);
            /* the diagnostics ROM would go here */

            ROM_REGION(0x1000, REGION_CPU4, 0);/* 8039 */
            ROM_LOAD("gyrussk.3a", 0x0000, 0x1000, 0x3f9b5dea);

            ROM_REGION(0x2000, REGION_GFX1, ROMREGION_DISPOSE);
            ROM_LOAD("gyrussk.4", 0x0000, 0x2000, 0x27d8329b);

            ROM_REGION(0x8000, REGION_GFX2, ROMREGION_DISPOSE);
            ROM_LOAD("gyrussk.6", 0x0000, 0x2000, 0xc949db10);
            ROM_LOAD("gyrussk.5", 0x2000, 0x2000, 0x4f22411a);
            ROM_LOAD("gyrussk.8", 0x4000, 0x2000, 0x47cd1fbc);
            ROM_LOAD("gyrussk.7", 0x6000, 0x2000, 0x8e8d388c);

            ROM_REGION(0x0220, REGION_PROMS, 0);
            ROM_LOAD("gyrussk.pr3", 0x0000, 0x0020, 0x98782db3);/* palette */
            ROM_LOAD("gyrussk.pr1", 0x0020, 0x0100, 0x7ed057de);/* sprite lookup table */
            ROM_LOAD("gyrussk.pr2", 0x0120, 0x0100, 0xde823a81);/* character lookup table */
            ROM_END();
        }
    };

    static RomLoadPtr rom_gyrussce = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("gya-1.bin", 0x0000, 0x2000, 0x85f8b7c2);
            ROM_LOAD("gya-2.bin", 0x2000, 0x2000, 0x1e1a970f);
            ROM_LOAD("gya-3.bin", 0x4000, 0x2000, 0xf6dbb33b);
            /* the diagnostics ROM would go here */

            ROM_REGION(2 * 0x10000, REGION_CPU2, 0);/* 64k for code + 64k for the decrypted opcodes */
            ROM_LOAD("gyrussk.9", 0xe000, 0x2000, 0x822bf27e);

            ROM_REGION(0x10000, REGION_CPU3, 0);/* 64k for the audio CPU */
            ROM_LOAD("gyrussk.1a", 0x0000, 0x2000, 0xf4ae1c17);
            ROM_LOAD("gyrussk.2a", 0x2000, 0x2000, 0xba498115);
            /* the diagnostics ROM would go here */

            ROM_REGION(0x1000, REGION_CPU4, 0);/* 8039 */
            ROM_LOAD("gyrussk.3a", 0x0000, 0x1000, 0x3f9b5dea);

            ROM_REGION(0x2000, REGION_GFX1, ROMREGION_DISPOSE);
            ROM_LOAD("gyrussk.4", 0x0000, 0x2000, 0x27d8329b);

            ROM_REGION(0x8000, REGION_GFX2, ROMREGION_DISPOSE);
            ROM_LOAD("gyrussk.6", 0x0000, 0x2000, 0xc949db10);
            ROM_LOAD("gyrussk.5", 0x2000, 0x2000, 0x4f22411a);
            ROM_LOAD("gyrussk.8", 0x4000, 0x2000, 0x47cd1fbc);
            ROM_LOAD("gyrussk.7", 0x6000, 0x2000, 0x8e8d388c);

            ROM_REGION(0x0220, REGION_PROMS, 0);
            ROM_LOAD("gyrussk.pr3", 0x0000, 0x0020, 0x98782db3);/* palette */
            ROM_LOAD("gyrussk.pr1", 0x0020, 0x0100, 0x7ed057de);/* sprite lookup table */
            ROM_LOAD("gyrussk.pr2", 0x0120, 0x0100, 0xde823a81);/* character lookup table */
            ROM_END();
        }
    };

    static RomLoadPtr rom_venus = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("r1", 0x0000, 0x2000, 0xd030abb1);
            ROM_LOAD("r2", 0x2000, 0x2000, 0xdbf65d4d);
            ROM_LOAD("r3", 0x4000, 0x2000, 0xdb246fcd);
            /* the diagnostics ROM would go here */

            ROM_REGION(2 * 0x10000, REGION_CPU2, 0);/* 64k for code + 64k for the decrypted opcodes */
            ROM_LOAD("gyrussk.9", 0xe000, 0x2000, 0x822bf27e);

            ROM_REGION(0x10000, REGION_CPU3, 0);/* 64k for the audio CPU */
            ROM_LOAD("gyrussk.1a", 0x0000, 0x2000, 0xf4ae1c17);
            ROM_LOAD("gyrussk.2a", 0x2000, 0x2000, 0xba498115);
            /* the diagnostics ROM would go here */

            ROM_REGION(0x1000, REGION_CPU4, 0);/* 8039 */
            ROM_LOAD("gyrussk.3a", 0x0000, 0x1000, 0x3f9b5dea);

            ROM_REGION(0x2000, REGION_GFX1, ROMREGION_DISPOSE);
            ROM_LOAD("gyrussk.4", 0x0000, 0x2000, 0x27d8329b);

            ROM_REGION(0x8000, REGION_GFX2, ROMREGION_DISPOSE);
            ROM_LOAD("gyrussk.6", 0x0000, 0x2000, 0xc949db10);
            ROM_LOAD("gyrussk.5", 0x2000, 0x2000, 0x4f22411a);
            ROM_LOAD("gyrussk.8", 0x4000, 0x2000, 0x47cd1fbc);
            ROM_LOAD("gyrussk.7", 0x6000, 0x2000, 0x8e8d388c);

            ROM_REGION(0x0220, REGION_PROMS, 0);
            ROM_LOAD("gyrussk.pr3", 0x0000, 0x0020, 0x98782db3);/* palette */
            ROM_LOAD("gyrussk.pr1", 0x0020, 0x0100, 0x7ed057de);/* sprite lookup table */
            ROM_LOAD("gyrussk.pr2", 0x0120, 0x0100, 0xde823a81);/* character lookup table */
            ROM_END();
        }
    };

    public static InitDriverPtr init_gyruss = new InitDriverPtr() {
        public void handler() {
            konami1_decode_cpu2();
        }
    };

    public static GameDriver driver_gyruss = new GameDriver("1983", "gyruss", "gyruss.java", rom_gyruss, null, machine_driver_gyruss, input_ports_gyruss, init_gyruss, ROT90, "Konami", "Gyruss (Konami)");
    public static GameDriver driver_gyrussce = new GameDriver("1983", "gyrussce", "gyruss.java", rom_gyrussce, driver_gyruss, machine_driver_gyruss, input_ports_gyrussce, init_gyruss, ROT90, "Konami (Centuri license)", "Gyruss (Centuri)");
    public static GameDriver driver_venus = new GameDriver("1983", "venus", "gyruss.java", rom_venus, driver_gyruss, machine_driver_gyruss, input_ports_gyrussce, init_gyruss, ROT90, "bootleg", "Venus");
}
