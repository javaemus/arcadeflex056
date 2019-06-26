/** *************************************************************************
 *
 * Nintendo VS UniSystem and DualSystem - (c) 198? Nintendo of America
 *
 * Portions of this code are heavily based on
 * Brad Oliver's MESS implementation of the NES.
 *
 * RP2C04-001:
 * - Gradius
 * - Pinball
 * - Hogan's Alley
 * - Baseball
 * - Super Xevious
 * - Platoon
 *
 * RP2C04-002:
 * - Ladies golf
 * - Mach Rider
 * - Stroke N' Match Golf
 * - Castlevania
 * - Slalom
 * - Wrecking
 *
 * RP2C04-003:
 * - Excite Bike
 * - Dr mario
 * - Soccer
 * - Goonies
 * - Tko boxing
 *
 * RP2c05-004:
 * - Super Mario Bros
 * - Ice climber
 * - Clu Clu Land
 * - Top gun ( ? ) (maybe a RC2c05-04)
 * - Excite Bike (Japan)
 * - Ice Climber Dual (Japan)
 *
 * Rcp2c03b:
 * - Duckhunt
 * - Tennis
 * - Skykid
 * - Rbi Baseball
 * - Mahjong
 * - Star Luster
 * - Stroke and Match Golf (Japan)
 * - Pinball (Japan)
 *
 * Needed roms:
 * - Gumshoe
 *
 *
 * Known issues:
 * Light Gun doesnt work in 16 bit mode
 * Can't Rotate
 *
 *
 ************************************************************************** */
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package WIP.mame056.drivers;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;

import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.inptportH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.memoryH.*;
import static mame056.drawgfxH.*;
import static mame056.inputH.*;
import static mame056.sndintrfH.*;

import static WIP.mame056.vidhrdw.vsnes.*;
import static WIP.mame056.machine.vsnes.*;
import static WIP.mame056.vidhrdw.ppu2c03b.*;
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;
import static mame056.sound.nes_apu.*;
import static mame056.sound.nes_apuH.*;

public class vsnes {

    /* local stuff */
    static UBytePtr work_ram = new UBytePtr(), work_ram_1 = new UBytePtr();

    public static ReadHandlerPtr mirror_ram_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return work_ram.read(offset & 0x7ff);
        }
    };

    public static ReadHandlerPtr mirror_ram_1_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return work_ram.read(offset & 0x7ff);
        }
    };

    public static WriteHandlerPtr mirror_ram_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            work_ram.write(offset & 0x7ff, data);
        }
    };

    public static WriteHandlerPtr mirror_ram_1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            work_ram.write(offset & 0x7ff, data);
        }
    };

    public static WriteHandlerPtr sprite_dma_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int source = (data & 7) * 0x100;

            ppu2c03b_spriteram_dma(0, new UBytePtr(work_ram, source));
        }
    };

    public static WriteHandlerPtr sprite_dma_1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int source = (data & 7) * 0x100;

            ppu2c03b_spriteram_dma(1, new UBytePtr(work_ram_1, source));
        }
    };

    /**
     * ***************************************************************************
     */
    public static Memory_ReadAddress readmem[] = {
        new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_ReadAddress(0x0000, 0x07ff, MRA_RAM),
        new Memory_ReadAddress(0x0800, 0x1fff, mirror_ram_r),
        new Memory_ReadAddress(0x2000, 0x3fff, ppu2c03b_0_r),
        new Memory_ReadAddress(0x4000, 0x4015, NESPSG_0_r),
        new Memory_ReadAddress(0x4016, 0x4016, vsnes_in0_r),
        new Memory_ReadAddress(0x4017, 0x4017, vsnes_in1_r),
        new Memory_ReadAddress(0x8000, 0xffff, MRA_ROM),
        new Memory_ReadAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_WriteAddress writemem[] = {
        new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_WriteAddress(0x0000, 0x07ff, MWA_RAM, work_ram),
        new Memory_WriteAddress(0x0800, 0x1fff, mirror_ram_w),
        new Memory_WriteAddress(0x2000, 0x3fff, ppu2c03b_0_w),
        new Memory_WriteAddress(0x4011, 0x4011, DAC_0_data_w),
        new Memory_WriteAddress(0x4014, 0x4014, sprite_dma_w),
        new Memory_WriteAddress(0x4000, 0x4015, NESPSG_0_w),
        new Memory_WriteAddress(0x4016, 0x4016, vsnes_in0_w),
        new Memory_WriteAddress(0x4017, 0x4017, MWA_NOP), /* in 1 writes ignored */
        new Memory_WriteAddress(0x8000, 0xffff, MWA_ROM),
        new Memory_WriteAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_ReadAddress readmem_1[] = {
        new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_ReadAddress(0x0000, 0x07ff, MRA_RAM),
        new Memory_ReadAddress(0x0800, 0x1fff, mirror_ram_1_r),
        new Memory_ReadAddress(0x2000, 0x3fff, ppu2c03b_1_r),
        new Memory_ReadAddress(0x4000, 0x4015, NESPSG_0_r),
        new Memory_ReadAddress(0x4016, 0x4016, vsnes_in0_1_r),
        new Memory_ReadAddress(0x4017, 0x4017, vsnes_in1_1_r),
        new Memory_ReadAddress(0x8000, 0xffff, MRA_ROM),
        new Memory_ReadAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_WriteAddress writemem_1[] = {
        new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_WriteAddress(0x0000, 0x07ff, MWA_RAM, work_ram_1),
        new Memory_WriteAddress(0x0800, 0x1fff, mirror_ram_1_w),
        new Memory_WriteAddress(0x2000, 0x3fff, ppu2c03b_1_w),
        new Memory_WriteAddress(0x4011, 0x4011, DAC_1_data_w),
        new Memory_WriteAddress(0x4014, 0x4014, sprite_dma_1_w),
        new Memory_WriteAddress(0x4000, 0x4015, NESPSG_1_w),
        new Memory_WriteAddress(0x4016, 0x4016, vsnes_in0_1_w),
        new Memory_WriteAddress(0x4017, 0x4017, MWA_NOP), /* in 1 writes ignored */
        new Memory_WriteAddress(0x8000, 0xffff, MWA_ROM),
        new Memory_WriteAddress(MEMPORT_MARKER, 0)
    };

    /**
     * ***************************************************************************
     */
    public static void VS_CONTROLS() {
        PORT_START();
        /* IN0 */
        PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_BUTTON2);
        /* BUTTON A on a nes */
        PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_BUTTON1);
        /* BUTTON B on a nes */
        PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_START1);
        /* SELECT on a nes */
        PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_BUTTON3);
        /* START on a nes */
        PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP);
        PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN);
        PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT);
        PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT);

        PORT_START();
        /* IN1 */
        PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_PLAYER2);/* BUTTON A on a nes */
        PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2);/* BUTTON B on a nes */
        PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_START2);
        /* SELECT on a nes */
        PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_BUTTON3 | IPF_PLAYER2);
        /* START on a nes */
        PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_PLAYER2);
        PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_PLAYER2);
        PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_PLAYER2);
        PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER2);

        PORT_START();
        /* IN2 */
        PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_UNUSED);/* serial pin from controller */
        PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN);
        PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_SERVICE1);/* service credit? */
        PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_UNUSED);/* bit 0 of dsw goes here */
        PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_UNUSED);/* bit 1 of dsw goes here */
        PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_COIN1);
        PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_COIN2);
        PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN);
    }

    public static void VS_CONTROLS_REVERSE() {
        PORT_START();
        /* IN0 */
        PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_PLAYER2);
        /* BUTTON A on a nes */
        PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2);
        /* BUTTON B on a nes */
        PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_START1);
        /* SELECT on a nes */
        PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_BUTTON3 | IPF_PLAYER2);
        /* START on a nes */
        PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_PLAYER2);
        PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_PLAYER2);
        PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_PLAYER2);
        PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER2);

        PORT_START();
        /* IN1 */
        PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_BUTTON2);/* BUTTON A on a nes */
        PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_BUTTON1);/* BUTTON B on a nes */
        PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_START2);
        /* SELECT on a nes */
        PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_BUTTON3);
        /* START on a nes */
        PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP);
        PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN);
        PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT);
        PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT);

        PORT_START();
        /* IN2 */
        PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_UNUSED);/* serial pin from controller */
        PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN);
        PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_SERVICE1);/* service credit? */
        PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_UNUSED);/* bit 0 of dsw goes here */
        PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_UNUSED);/* bit 1 of dsw goes here */
        PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_COIN1);
        PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_COIN2);
        PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN);
    }

    public static void VS_ZAPPER() {
        PORT_START();
        /* IN0 */
        PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_UNUSED);/* sprite hit */
        PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_BUTTON1);/* gun trigger */

        PORT_START();
        /* IN1 */
        PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_UNUSED);
    }

    public static void VS_DUAL_CONTROLS_L() {

        PORT_START();
        /* IN0 */
        PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_BUTTON2);
        /* BUTTON A on a nes */
        PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_BUTTON1);
        /* BUTTON B on a nes */
        PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_START1);
        /* SELECT on a nes */
        PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_START3);
        /* START on a nes */
        PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP);
        PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN);
        PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT);
        PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT);

        PORT_START();
        /* IN1 */
        PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_PLAYER2);/* BUTTON A on a nes */
        PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2);/* BUTTON B on a nes */
        PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_START2);
        /* SELECT on a nes */
        PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_START4);
        /* START on a nes */
        PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_PLAYER2);
        PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_PLAYER2);
        PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_PLAYER2);
        PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER2);

        PORT_START();
        /* IN2 */
        PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_UNUSED);/* serial pin from controller */
        PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN);
        PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_SERVICE1);/* service credit? */
        PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_UNUSED);/* bit 0 of dsw goes here */
        PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_UNUSED);/* bit 1 of dsw goes here */
        PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_COIN1);
        PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_COIN2);
        PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_UNUSED);/* this bit masks irqs - dont change */
    }

    public static void VS_DUAL_CONTROLS_R() {
        PORT_START();
        /* IN3 */
        PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_PLAYER3);/* BUTTON A on a nes */
        PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER3);/* BUTTON B on a nes */
        PORT_BITX(0x04, IP_ACTIVE_HIGH, 0, "2nd Side 1 Player Start", KEYCODE_MINUS, IP_JOY_NONE);/* SELECT on a nes */
        PORT_BITX(0x08, IP_ACTIVE_HIGH, 0, "2nd Side 3 Player Start", KEYCODE_BACKSLASH, IP_JOY_NONE);/* START on a nes */
        PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_PLAYER3);
        PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_PLAYER3);
        PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_PLAYER3);
        PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER3);

        PORT_START();
        /* IN4 */
        PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_PLAYER4);/* BUTTON A on a nes */
        PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER4);/* BUTTON B on a nes */
        PORT_BITX(0x04, IP_ACTIVE_HIGH, 0, "2nd Side 2 Player Start", KEYCODE_EQUALS, IP_JOY_NONE);/* SELECT on a nes */
        PORT_BITX(0x08, IP_ACTIVE_HIGH, 0, "2nd Side 4 Player Start", KEYCODE_BACKSPACE, IP_JOY_NONE);/* START on a nes */
        PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_PLAYER4);
        PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_PLAYER4);
        PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_PLAYER4);
        PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER4);

        PORT_START();
        /* IN5 */
        PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_UNUSED);/* serial pin from controller */
        PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN);
        PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_SERVICE2);/* service credit? */
        PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_UNUSED);
        PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_COIN3);
        PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_COIN4);
        PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_UNUSED);/* this bit masks irqs - dont change */
    }

    static InputPortPtr input_ports_vsnes = new InputPortPtr() {
        public void handler() {
            VS_CONTROLS();

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x01, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x02, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x02, DEF_STR("On"));
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x04, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_platoon = new InputPortPtr() {
        public void handler() {
            VS_CONTROLS();

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x01, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x02, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x02, DEF_STR("On"));
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Demo_Sounds"));
            PORT_DIPSETTING(0x04, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0xE0, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0xc0, DEF_STR("5C_1C"));
            PORT_DIPSETTING(0xa0, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x80, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x60, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x20, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x40, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0xe0, DEF_STR("Free_Play"));
            INPUT_PORTS_END();
        }
    };

    /*
	Stroke Play Off On
	Hole in 1 +5 +4
	Double Eagle +4 +3
	Eagle +3 +2
	Birdie +2 +1
	Par +1 0
	Bogey 0 -1
	Other 0 -2
	
	Match Play OFF ON
	Win Hole +1 +2
	Tie 0 0
	Lose Hole -1 -2
     */
    static InputPortPtr input_ports_golf = new InputPortPtr() {
        public void handler() {
            VS_CONTROLS();

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x07, 0x01, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x07, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x03, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x05, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x01, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x06, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x02, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x04, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0x00, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x08, 0x00, "Hole Size");
            PORT_DIPSETTING(0x00, "Large");
            PORT_DIPSETTING(0x08, "Small");
            PORT_DIPNAME(0x10, 0x00, "Points per Stroke");
            PORT_DIPSETTING(0x00, "Easier");
            PORT_DIPSETTING(0x10, "Harder");
            PORT_DIPNAME(0x60, 0x00, "Starting Points");
            PORT_DIPSETTING(0x00, "10");
            PORT_DIPSETTING(0x40, "13");
            PORT_DIPSETTING(0x20, "16");
            PORT_DIPSETTING(0x60, "20");
            PORT_DIPNAME(0x80, 0x00, "Difficulty Vs. Computer");
            PORT_DIPSETTING(0x00, "Easy");
            PORT_DIPSETTING(0x80, "Hard");

            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_vstennis = new InputPortPtr() {
        public void handler() {
            VS_DUAL_CONTROLS_L();
            /* left side controls */

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x03, 0x00, "Difficulty Vs. Computer");
            PORT_DIPSETTING(0x00, "Easy");
            PORT_DIPSETTING(0x01, "Normal");
            PORT_DIPSETTING(0x02, "Hard");
            PORT_DIPSETTING(0x03, "Very Hard");
            PORT_DIPNAME(0x0c, 0x00, "Difficulty Vs. Player");
            PORT_DIPSETTING(0x00, "Easy");
            PORT_DIPSETTING(0x04, "Normal");
            PORT_DIPSETTING(0x08, "Hard");
            PORT_DIPSETTING(0x0c, "Very Hard");
            PORT_DIPNAME(0x10, 0x00, "Raquet Size");
            PORT_DIPSETTING(0x00, "Large");
            PORT_DIPSETTING(0x10, "Small");
            PORT_DIPNAME(0x20, 0x00, "Extra Score");
            PORT_DIPSETTING(0x00, "1 Set");
            PORT_DIPSETTING(0x20, "1 Game");
            PORT_DIPNAME(0x40, 0x00, "Court Color");
            PORT_DIPSETTING(0x00, "Green");
            PORT_DIPSETTING(0x40, "Blue");
            PORT_DIPNAME(0x80, 0x00, "Copyright");
            PORT_DIPSETTING(0x00, "Japan");
            PORT_DIPSETTING(0x80, "USA");

            VS_DUAL_CONTROLS_R();
            /* Right Side Controls */

            PORT_START();
            /* DSW1 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x01, 0x00, DEF_STR("Service_Mode"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x06, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x02, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x06, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x08, 0x00, "Doubles 4 Player");
            PORT_DIPSETTING(0x00, "2 Credits");
            PORT_DIPSETTING(0x08, "4 Credits");
            PORT_DIPNAME(0x10, 0x00, "Doubles Vs CPU");
            PORT_DIPSETTING(0x00, "1 Credit");
            PORT_DIPSETTING(0x10, "2 Credits");
            PORT_DIPNAME(0x60, 0x00, "Rackets Per Game");
            PORT_DIPSETTING(0x00, "3");
            PORT_DIPSETTING(0x20, "5");
            PORT_DIPSETTING(0x40, "4");
            PORT_DIPSETTING(0x60, "2");
            PORT_DIPNAME(0x80, 0x80, DEF_STR("Demo_Sounds"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_wrecking = new InputPortPtr() {
        public void handler() {
            VS_DUAL_CONTROLS_L();
            /* left side controls */

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x03, 0x02, DEF_STR("Lives"));
            PORT_DIPSETTING(0x00, "3");
            PORT_DIPSETTING(0x02, "4");
            PORT_DIPSETTING(0x01, "5");
            PORT_DIPSETTING(0x03, "6");

            PORT_DIPNAME(0x04, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x04, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));

            VS_DUAL_CONTROLS_R();
            /* right side controls */

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x07, 0x01, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x07, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x03, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x05, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x01, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x06, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x02, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x04, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0x00, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_balonfgt = new InputPortPtr() {
        public void handler() {
            VS_DUAL_CONTROLS_L();
            /* left side controls */

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x07, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x03, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x05, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x01, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x02, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x06, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0x07, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Service_Mode"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));

            VS_DUAL_CONTROLS_R();
            /* right side controls */

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x03, 0x00, DEF_STR("Lives"));
            PORT_DIPSETTING(0x00, "3");
            PORT_DIPSETTING(0x02, "4");
            PORT_DIPSETTING(0x01, "5");
            PORT_DIPSETTING(0x03, "6");
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x04, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_vsmahjng = new InputPortPtr() {
        public void handler() {
            VS_DUAL_CONTROLS_L();
            /* left side controls */

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x01, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x02, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x02, DEF_STR("On"));
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x04, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));

            VS_DUAL_CONTROLS_R();
            /* right side controls */

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x01, 0x00, DEF_STR("Service_Mode"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x06, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x02, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x06, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_vsbball = new InputPortPtr() {
        public void handler() {
            VS_DUAL_CONTROLS_L();
            /* left side controls */

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x01, 0x00, DEF_STR("Service_Mode"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x06, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x02, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x06, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));

            VS_DUAL_CONTROLS_R();
            /* right side controls */

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x01, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x02, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x02, DEF_STR("On"));
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x04, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_vsbballj = new InputPortPtr() {
        public void handler() {
            VS_DUAL_CONTROLS_L();
            /* left side controls */

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x01, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x02, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x02, DEF_STR("On"));
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x04, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));

            VS_DUAL_CONTROLS_R();
            /* right side controls */

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x01, 0x00, DEF_STR("Service_Mode"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x02, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x02, DEF_STR("On"));
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x04, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x80, DEF_STR("Demo_Sounds"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_iceclmrj = new InputPortPtr() {
        public void handler() {
            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            VS_DUAL_CONTROLS_L();

            PORT_DIPNAME(0x01, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x02, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x02, DEF_STR("On"));
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x04, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));

            VS_DUAL_CONTROLS_R();

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x01, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x02, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x02, DEF_STR("On"));
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x04, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_drmario = new InputPortPtr() {
        public void handler() {

            VS_CONTROLS_REVERSE();

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x03, 0x00, "Drop Rate Increases After");
            PORT_DIPSETTING(0x00, "7 Pills");
            PORT_DIPSETTING(0x01, "8 Pills");
            PORT_DIPSETTING(0x02, "9 Pills");
            PORT_DIPSETTING(0x03, "10 Pills");
            PORT_DIPNAME(0x0c, 0x00, "Virus Level");
            PORT_DIPSETTING(0x00, "1");
            PORT_DIPSETTING(0x04, "3");
            PORT_DIPSETTING(0x08, "5");
            PORT_DIPSETTING(0x0c, "7");
            PORT_DIPNAME(0x30, 0x00, "Drop Speed Up");
            PORT_DIPSETTING(0x00, "Slow");
            PORT_DIPSETTING(0x10, "Medium");
            PORT_DIPSETTING(0x20, "Fast");
            PORT_DIPSETTING(0x30, "Fastest");
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x40, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x80, 0x80, DEF_STR("Demo_Sounds"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_rbibb = new InputPortPtr() {
        public void handler() {

            VS_CONTROLS_REVERSE();

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x03, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x03, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x02, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x01, DEF_STR("1C_2C"));
            PORT_DIPNAME(0x0c, 0x0c, "Max. 1p/in, 2p/in, Min");
            PORT_DIPSETTING(0x04, "2, 1, 3");
            PORT_DIPSETTING(0x0c, "2, 2, 4");
            PORT_DIPSETTING(0x00, "3, 2, 6");
            PORT_DIPSETTING(0x08, "4, 3, 7");
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Demo_Sounds"));
            PORT_DIPSETTING(0x10, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            /*Note the 3 dips below are docuemtned as required to be off in the manual */
 /* Turning them on messes with the colors */
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNUSED);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNUSED);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNUSED);

            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_btlecity = new InputPortPtr() {
        public void handler() {
            VS_CONTROLS();

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x01, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x02, 0x00, DEF_STR("Lives"));
            PORT_DIPSETTING(0x00, "2");
            PORT_DIPSETTING(0x02, "4");
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Demo_Sounds"));
            PORT_DIPSETTING(0x04, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0xc0, 0x00, "Color Palette");
            PORT_DIPSETTING(0x00, "1");
            PORT_DIPSETTING(0x40, "2");
            PORT_DIPSETTING(0x80, "3");
            PORT_DIPSETTING(0xc0, "4");
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_cluclu = new InputPortPtr() {
        public void handler() {
            VS_CONTROLS();

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x07, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x03, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x05, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x01, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x02, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x06, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0x07, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Lives"));
            PORT_DIPSETTING(0x00, "3");
            PORT_DIPSETTING(0x20, "5");
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_cstlevna = new InputPortPtr() {
        public void handler() {
            VS_CONTROLS();

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x07, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x03, DEF_STR("5C_1C"));
            PORT_DIPSETTING(0x05, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x01, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x06, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x02, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x07, DEF_STR("Free_Play"));

            PORT_DIPNAME(0x08, 0x00, DEF_STR("Lives"));
            PORT_DIPSETTING(0x00, "2");
            PORT_DIPSETTING(0x08, "1");
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_iceclimb = new InputPortPtr() {
        public void handler() {
            VS_CONTROLS();

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x07, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x03, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x05, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x01, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x02, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x06, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0x07, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x18, 0x00, DEF_STR("Lives"));
            PORT_DIPSETTING(0x00, "2");
            PORT_DIPSETTING(0x10, "3");
            PORT_DIPSETTING(0x08, "4");
            PORT_DIPSETTING(0x18, "6");
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_excitebk = new InputPortPtr() {
        public void handler() {
            VS_CONTROLS();

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x07, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x03, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x05, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x01, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x02, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x06, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0x07, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x18, 0x00, "Bonus");
            PORT_DIPSETTING(0x00, "100k and Every 50k");
            PORT_DIPSETTING(0x10, "Every 100k");
            PORT_DIPSETTING(0x08, "100k Only");
            PORT_DIPSETTING(0x18, "Nothing");
            PORT_DIPNAME(0x20, 0x00, "1st Half Qualifying Time");
            PORT_DIPSETTING(0x00, "Normal");
            PORT_DIPSETTING(0x20, "Hard");
            PORT_DIPNAME(0x40, 0x00, "2nd Half Qualifying Time");
            PORT_DIPSETTING(0x00, "Normal");
            PORT_DIPSETTING(0x40, "Hard");
            PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_UNUSED);
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_machridr = new InputPortPtr() {
        public void handler() {
            VS_CONTROLS();

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x07, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x03, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x05, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x01, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x02, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x06, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0x07, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x18, 0x00, "Timer");
            PORT_DIPSETTING(0x00, "280");
            PORT_DIPSETTING(0x10, "250");
            PORT_DIPSETTING(0x08, "220");
            PORT_DIPSETTING(0x18, "200");
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_suprmrio = new InputPortPtr() {
        public void handler() {
            VS_CONTROLS();

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x07, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x02, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x06, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x01, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x05, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0x03, DEF_STR("1C_5C"));
            PORT_DIPSETTING(0x07, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Lives"));
            PORT_DIPSETTING(0x00, "3");
            PORT_DIPSETTING(0x08, "2");
            PORT_DIPNAME(0x30, 0x00, DEF_STR("Bonus_Life"));
            PORT_DIPSETTING(0x00, "100");
            PORT_DIPSETTING(0x20, "150");
            PORT_DIPSETTING(0x10, "200");
            PORT_DIPSETTING(0x30, "250");
            PORT_DIPNAME(0x40, 0x00, "Timer");
            PORT_DIPSETTING(0x00, "Slow");
            PORT_DIPSETTING(0x40, "FAST");
            PORT_DIPNAME(0x80, 0x00, "Continue Lives");
            PORT_DIPSETTING(0x00, "4");
            PORT_DIPSETTING(0x80, "3");
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_duckhunt = new InputPortPtr() {
        public void handler() {
            VS_ZAPPER();

            PORT_START();
            /* IN2 */
            PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_UNUSED);/* serial pin from controller */
            PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN);
            PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_SERVICE1);/* service credit? */
            PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_UNUSED);/* bit 0 of dsw goes here */
            PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_UNUSED);/* bit 1 of dsw goes here */
            PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_COIN1);
            PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_COIN2);
            PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN);

            PORT_START();
            /* IN3 */
            PORT_DIPNAME(0x07, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0X03, DEF_STR("5C_1C"));
            PORT_DIPSETTING(0x05, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0X01, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x06, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0X00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0X02, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x07, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x20, "Misses per game");
            PORT_DIPSETTING(0x00, "3");
            PORT_DIPSETTING(0x20, "5");
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));

            PORT_START();
            /* IN4 - FAKE - Gun X pos */
            PORT_ANALOG(0xff, 0x80, IPT_AD_STICK_X, 70, 30, 0, 255);

            PORT_START();
            /* IN5 - FAKE - Gun Y pos */
            PORT_ANALOG(0xff, 0x80, IPT_AD_STICK_Y, 50, 30, 0, 255);

            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_hogalley = new InputPortPtr() {
        public void handler() {
            VS_ZAPPER();

            PORT_START();
            /* IN2 */
            PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_UNUSED);/* serial pin from controller */
            PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN);
            PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_SERVICE1);/* service credit? */
            PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_UNUSED);/* bit 0 of dsw goes here */
            PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_UNUSED);/* bit 1 of dsw goes here */
            PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_COIN1);
            PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_COIN2);
            PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN);

            PORT_START();
            /* IN3 */
            PORT_DIPNAME(0x07, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0X03, DEF_STR("5C_1C"));
            PORT_DIPSETTING(0x05, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0X01, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x06, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0X00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0X02, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x07, DEF_STR("Free_Play"));
            PORT_DIPNAME(0x18, 0x10, DEF_STR("Difficulty"));
            PORT_DIPSETTING(0x00, "Easiest");
            PORT_DIPSETTING(0x08, "Easy");
            PORT_DIPSETTING(0x10, "Hard");
            PORT_DIPSETTING(0x18, "Hardest");
            PORT_DIPNAME(0x20, 0x20, "Misses per game");
            PORT_DIPSETTING(0x00, "3");
            PORT_DIPSETTING(0x20, "5");
            PORT_DIPNAME(0xc0, 0x00, DEF_STR("Bonus_Life"));
            PORT_DIPSETTING(0x00, "30000 ");
            PORT_DIPSETTING(0x40, "50000 ");
            PORT_DIPSETTING(0x80, "80000 ");
            PORT_DIPSETTING(0xc0, "100000 ");

            PORT_START();
            /* IN4 - FAKE - Gun X pos */
            PORT_ANALOG(0xff, 0x80, IPT_AD_STICK_X, 70, 30, 0, 255);

            PORT_START();
            /* IN5 - FAKE - Gun Y pos */
            PORT_ANALOG(0xff, 0x80, IPT_AD_STICK_Y, 50, 30, 0, 255);

            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_vstetris = new InputPortPtr() {
        public void handler() {

            VS_CONTROLS_REVERSE();

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x01, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x02, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x02, DEF_STR("On"));
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x04, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x20, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));

            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_vsskykid = new InputPortPtr() {
        public void handler() {
            PORT_START();
            /* IN0 */
            PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_PLAYER2);
            /* BUTTON A on a nes */
            PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2);
            /* BUTTON B on a nes */
            PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_START1);
            /* SELECT on a nes */
            PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_START3);
            /* START on a nes */
            PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_PLAYER2);
            PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_PLAYER2);
            PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_PLAYER2);
            PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_PLAYER2);

            PORT_START();
            /* IN1 */
            PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_BUTTON2);/* BUTTON A on a nes */
            PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_BUTTON1);/* BUTTON B on a nes */
            PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_START2);
            /* SELECT on a nes */
            PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_BUTTON3);
            /* START on a nes */
            PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP);
            PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN);
            PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT);
            PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT);

            PORT_START();
            /* IN2 */
            PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_UNUSED);/* serial pin from controller */
            PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN);
            PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_SERVICE1);/* service credit? */
            PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_UNUSED);/* bit 0 of dsw goes here */
            PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_UNUSED);/* bit 1 of dsw goes here */
            PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_COIN1);
            PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_COIN2);
            PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN);

            PORT_START();
            /* DSW0 - bit 0 and 1 read from bit 3 and 4 on $4016, rest of the bits read on $4017 */
            PORT_DIPNAME(0x01, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x02, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x02, DEF_STR("On"));
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x04, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x08, DEF_STR("On"));
            PORT_DIPNAME(0x10, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x10, DEF_STR("On"));
            PORT_DIPNAME(0x20, 0x20, "Colors");
            PORT_DIPSETTING(0x00, "Alternate");
            PORT_DIPSETTING(0x20, "Normal");
            PORT_DIPNAME(0x40, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x40, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x80, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static GfxDecodeInfo nes_gfxdecodeinfo[]
            = {
                /* none, the ppu generates one */
                new GfxDecodeInfo(-1) /* end of array */};

    static NESinterface nes_interface = new NESinterface(
            1,
            new int[]{REGION_CPU1},
            new int[]{50}
    );

    static DACinterface nes_dac_interface = new DACinterface(
            1,
            new int[]{50}
    );

    static NESinterface nes_dual_interface = new NESinterface(
            2,
            new int[]{REGION_CPU1, REGION_CPU2},
            new int[]{25, 25}
    );

    static DACinterface nes_dual_dac_interface = new DACinterface(
            2,
            new int[]{25, 25}
    );

    static MachineDriver machine_driver_vsnes = new MachineDriver(
            /* basic machine hardware */
            new MachineCPU[]{
                new MachineCPU(
                        CPU_N2A03, 21477272,
                        readmem, writemem, null, null,
                        ignore_interrupt, 0 /* NMIs are triggered by the PPU */
                /* some carts also trigger IRQs */
                )
            },
            60, (int) (((1.0 / 60.0) * 1000000.0) / 262) * (262 - 239), /* frames per second, vblank duration */
            1,
            vsnes_init_machine,
            /* video hardware */
            32 * 8, 30 * 8, new rectangle(0 * 8, 32 * 8 - 1, 0 * 8, 30 * 8 - 1),
            nes_gfxdecodeinfo,
            4 * 16, 4 * 8,
            vsnes_vh_convert_color_prom,
            VIDEO_TYPE_RASTER,
            null,
            vsnes_vh_start,
            vsnes_vh_stop,
            vsnes_vh_screenrefresh,
            /* sound hardware */
            0, 0, 0, 0,
            new MachineSound[]{
                new MachineSound(
                        SOUND_NES,
                        nes_interface
                ),
                new MachineSound(
                        SOUND_DAC,
                        nes_dac_interface
                )
            }
    );

    static MachineDriver machine_driver_vsdual = new MachineDriver(
            /* basic machine hardware */
            new MachineCPU[]{
                new MachineCPU(
                        CPU_N2A03, (int) N2A03_DEFAULTCLOCK,
                        readmem, writemem, null, null,
                        ignore_interrupt, 0 /* NMIs are triggered by the PPU */
                /* some carts also trigger IRQs */
                ),
                new MachineCPU(
                        CPU_N2A03, (int) N2A03_DEFAULTCLOCK,
                        readmem_1, writemem_1, null, null,
                        ignore_interrupt, 0 /* NMIs are triggered by the PPU */
                /* some carts also trigger IRQs */
                )
            },
            60, (int) (((1.0 / 60.0) * 1000000.0) / 262) * (262 - 239), /* frames per second, vblank duration */
            1,
            vsdual_init_machine,
            /* video hardware */
            32 * 8 * 2, 30 * 8, new rectangle(0 * 8, 32 * 8 * 2 - 1, 0 * 8, 30 * 8 - 1),
            nes_gfxdecodeinfo,
            2 * 4 * 16, 2 * 4 * 16,
            vsdual_vh_convert_color_prom,
            VIDEO_TYPE_RASTER | VIDEO_DUAL_MONITOR | VIDEO_ASPECT_RATIO(8, 3),
            null,
            vsdual_vh_start,
            vsnes_vh_stop,
            vsdual_vh_screenrefresh,
            /* sound hardware */
            0, 0, 0, 0,
            new MachineSound[]{
                new MachineSound(
                        SOUND_NES,
                        nes_dual_interface
                ),
                new MachineSound(
                        SOUND_DAC,
                        nes_dual_dac_interface
                )
            }
    );

    /**
     * ***************************************************************************
     */
    static RomLoadPtr rom_suprmrio = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("1d", 0x8000, 0x2000, 0xbe4d5436);
            ROM_LOAD("1c", 0xa000, 0x2000, 0x0011fc5a);
            ROM_LOAD("1b", 0xc000, 0x2000, 0xb1b87893);
            ROM_LOAD("1a", 0xe000, 0x2000, 0x1abf053c);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("2b", 0x0000, 0x2000, 0x42418d40);
            ROM_LOAD("2a", 0x2000, 0x2000, 0x15506b86);
            ROM_END();
        }
    };

    static RomLoadPtr rom_iceclimb = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("ic-1d", 0x8000, 0x2000, 0x65e21765);
            ROM_LOAD("ic-1c", 0xa000, 0x2000, 0xa7909c51);
            ROM_LOAD("ic-1b", 0xc000, 0x2000, 0x7fb3cc21);
            ROM_LOAD("ic-1a", 0xe000, 0x2000, 0xbf196bf7);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("ic-2b", 0x0000, 0x2000, 0x331460b4);
            ROM_LOAD("ic-2a", 0x2000, 0x2000, 0x4ec44fb3);
            ROM_END();
        }
    };

    /* Gun games */
    static RomLoadPtr rom_duckhunt = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("1d", 0x8000, 0x2000, 0x3f51f0ed);
            ROM_LOAD("1c", 0xa000, 0x2000, 0x8bc7376c);
            ROM_LOAD("1b", 0xc000, 0x2000, 0xa042b6e1);
            ROM_LOAD("1a", 0xe000, 0x2000, 0x1906e3ab);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("2b", 0x0000, 0x2000, 0x0c52ec28);
            ROM_LOAD("2a", 0x2000, 0x2000, 0x3d238df3);
            ROM_END();
        }
    };

    static RomLoadPtr rom_hogalley = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("1d", 0x8000, 0x2000, 0x2089e166);
            ROM_LOAD("1c", 0xa000, 0x2000, 0xa85934ae);
            ROM_LOAD("1b", 0xc000, 0x2000, 0x718e25b3);
            ROM_LOAD("1a", 0xe000, 0x2000, 0xf9526852);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("2b", 0x0000, 0x2000, 0x7623e954);
            ROM_LOAD("2a", 0x2000, 0x2000, 0x78c842b6);
            ROM_END();
        }
    };

    static RomLoadPtr rom_goonies = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x20000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("prg.u7", 0x10000, 0x10000, 0x1e438d52);

            ROM_REGION(0x10000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("chr.u4", 0x0000, 0x10000, 0x4c4b61b0);
            ROM_END();
        }
    };

    static RomLoadPtr rom_vsgradus = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x20000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("prg.u7", 0x10000, 0x10000, 0xd99a2087);

            ROM_REGION(0x10000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("chr.u4", 0x0000, 0x10000, 0x23cf2fc3);
            ROM_END();
        }
    };

    static RomLoadPtr rom_btlecity = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("bc.1d", 0x8000, 0x2000, 0x6aa87037);
            ROM_LOAD("bc.1c", 0xa000, 0x2000, 0xbdb317db);
            ROM_LOAD("bc.1b", 0xc000, 0x2000, 0x1a0088b8);
            ROM_LOAD("bc.1a", 0xe000, 0x2000, 0x86307c89);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("bc.2b", 0x0000, 0x2000, 0x634f68bd);
            ROM_LOAD("bc.2a", 0x2000, 0x2000, 0xa9b49a05);
            ROM_END();
        }
    };

    static RomLoadPtr rom_cluclu = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("cl.6d", 0x8000, 0x2000, 0x1e9f97c9);
            ROM_LOAD("cl.6c", 0xa000, 0x2000, 0xe8b843a7);
            ROM_LOAD("cl.6b", 0xc000, 0x2000, 0x418ee9ea);
            ROM_LOAD("cl.6a", 0xe000, 0x2000, 0x5e8a8457);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("cl.8b", 0x0000, 0x2000, 0x960d9a6c);
            ROM_LOAD("cl.8a", 0x2000, 0x2000, 0xe3139791);
            ROM_END();
        }
    };

    static RomLoadPtr rom_excitebk = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("eb-1d", 0x8000, 0x2000, 0x7e54df1d);
            ROM_LOAD("eb-1c", 0xa000, 0x2000, 0x89baae91);
            ROM_LOAD("eb-1b", 0xc000, 0x2000, 0x4c0c2098);
            ROM_LOAD("eb-1a", 0xe000, 0x2000, 0xb9ab7110);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("eb-2b", 0x0000, 0x2000, 0x80be1f50);
            ROM_LOAD("eb-2a", 0x2000, 0x2000, 0xa9b49a05);

            ROM_END();
        }
    };

    static RomLoadPtr rom_excitbkj = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("eb4-46da.bin", 0x8000, 0x2000, 0x6aa87037);
            ROM_LOAD("eb4-46ca.bin", 0xa000, 0x2000, 0xbdb317db);
            ROM_LOAD("eb4-46ba.bin", 0xc000, 0x2000, 0xd1afe2dd);
            ROM_LOAD("eb4-46aa.bin", 0xe000, 0x2000, 0x46711d0e);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("eb4-48ba.bin", 0x0000, 0x2000, 0x62a76c52);
            //	ROM_LOAD( "eb4-48aa.bin",  0x2000, 0x2000, 0xa9b49a05 );
            ROM_LOAD("eb-2a", 0x2000, 0x2000, 0xa9b49a05);

            ROM_END();
        }
    };

    static RomLoadPtr rom_jajamaru = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            //ROM_LOAD( "10.bin", 0x8000, 0x2000, 0x16af1704);
            //ROM_LOAD( "9.bin",  0xa000, 0x2000, 0xdb7d1814);

            ROM_LOAD("8.bin", 0x8000, 0x2000, 0xce263271);
            ROM_LOAD("7.bin", 0xa000, 0x2000, 0xa406d0e4);
            ROM_LOAD("9.bin", 0xc000, 0x2000, 0xdb7d1814);
            ROM_LOAD("10.bin", 0xe000, 0x2000, 0x16af1704);

            ROM_REGION(0x8000, REGION_GFX1, 0);/* PPU memory */
            //ROM_LOAD( "12.bin",  0x0000, 0x2000, 0xc91d536a );
            //ROM_LOAD( "11.bin",  0x2000, 0x2000, 0xf0034c04 );
            //ROM_LOAD( "7.bin",   0x4000, 0x2000, 0xc91d536a );
            //ROM_LOAD( "8.bin",  0x6000, 0x2000, 0xf0034c04 );

            ROM_END();
        }
    };

    static RomLoadPtr rom_ladygolf = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("lg-1d", 0x8000, 0x2000, 0x8b2ab436);
            ROM_LOAD("lg-1c", 0xa000, 0x2000, 0xbda6b432);
            ROM_LOAD("lg-1b", 0xc000, 0x2000, 0xdcdd8220);
            ROM_LOAD("lg-1a", 0xe000, 0x2000, 0x26a3cb3b);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("lg-2b", 0x0000, 0x2000, 0x95618947);
            ROM_LOAD("lg-2a", 0x2000, 0x2000, 0xd07407b1);
            ROM_END();
        }
    };

    static RomLoadPtr rom_smgolfj = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("gf3_6d_b.bin", 0x8000, 0x2000, 0x8ce375b6);
            ROM_LOAD("gf3_6c_b.bin", 0xa000, 0x2000, 0x50a938d3);
            ROM_LOAD("gf3_6b_b.bin", 0xc000, 0x2000, 0x7dc39f1f);
            ROM_LOAD("gf3_6a_b.bin", 0xe000, 0x2000, 0x9b8a2106);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("gf3_8b_b.bin", 0x0000, 0x2000, 0x7ef68029);
            ROM_LOAD("gf3_8a_b.bin", 0x2000, 0x2000, 0xf2285878);
            ROM_END();
        }
    };

    static RomLoadPtr rom_machridr = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("mr-1d", 0x8000, 0x2000, 0x379c44b9);
            ROM_LOAD("mr-1c", 0xa000, 0x2000, 0xcb864802);
            ROM_LOAD("mr-1b", 0xc000, 0x2000, 0x5547261f);
            ROM_LOAD("mr-1a", 0xe000, 0x2000, 0xe3e3900d);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("mr-2b", 0x0000, 0x2000, 0x33a2b41a);
            ROM_LOAD("mr-2a", 0x2000, 0x2000, 0x685899d8);
            ROM_END();
        }
    };

    static RomLoadPtr rom_smgolf = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("golf-1d", 0x8000, 0x2000, 0xa3e286d3);
            ROM_LOAD("golf-1c", 0xa000, 0x2000, 0xe477e48b);
            ROM_LOAD("golf-1b", 0xc000, 0x2000, 0x7d80b511);
            ROM_LOAD("golf-1a", 0xe000, 0x2000, 0x7b767da6);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("golf-2b", 0x0000, 0x2000, 0x2782a3e5);
            ROM_LOAD("golf-2a", 0x2000, 0x2000, 0x6e93fdef);
            ROM_END();
        }
    };

    static RomLoadPtr rom_vspinbal = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("pb-6d", 0x8000, 0x2000, 0x69fc575e);
            ROM_LOAD("pb-6c", 0xa000, 0x2000, 0xfa9472d2);
            ROM_LOAD("pb-6b", 0xc000, 0x2000, 0xf57d89c5);
            ROM_LOAD("pb-6a", 0xe000, 0x2000, 0x640c4741);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("pb-8b", 0x0000, 0x2000, 0x8822ee9e);
            ROM_LOAD("pb-8a", 0x2000, 0x2000, 0xcbe98a28);
            ROM_END();
        }
    };

    static RomLoadPtr rom_vspinblj = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("pn3_6d_b.bin", 0x8000, 0x2000, 0xfd50c42e);
            ROM_LOAD("pn3_6c_b.bin", 0xa000, 0x2000, 0x59beb9e5);
            ROM_LOAD("pn3_6b_b.bin", 0xc000, 0x2000, 0xce7f47ce);
            ROM_LOAD("pn3_6a_b.bin", 0xe000, 0x2000, 0x5685e2ee);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("pn3_8b_b.bin", 0x0000, 0x2000, 0x1e3fec3e);
            ROM_LOAD("pn3_8a_b.bin", 0x2000, 0x2000, 0x6f963a65);
            ROM_END();
        }
    };

    static RomLoadPtr rom_vsslalom = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("slalom.1d", 0x8000, 0x2000, 0x6240a07d);
            ROM_LOAD("slalom.1c", 0xa000, 0x2000, 0x27c355e4);
            ROM_LOAD("slalom.1b", 0xc000, 0x2000, 0xd4825fbf);
            ROM_LOAD("slalom.1a", 0xe000, 0x2000, 0x82333f80);

            ROM_REGION(0x2000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("slalom.2a", 0x0000, 0x2000, 0x977bb126);

            ROM_END();
        }
    };

    static RomLoadPtr rom_vssoccer = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("soccer1d", 0x8000, 0x2000, 0x0ac52145);
            ROM_LOAD("soccer1c", 0xa000, 0x2000, 0xf132e794);
            ROM_LOAD("soccer1b", 0xc000, 0x2000, 0x26bb7325);
            ROM_LOAD("soccer1a", 0xe000, 0x2000, 0xe731635a);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("soccer2b", 0x0000, 0x2000, 0x307b19ab);
            ROM_LOAD("soccer2a", 0x2000, 0x2000, 0x7263613a);
            ROM_END();
        }
    };

    static RomLoadPtr rom_starlstr = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("sl_04.1d", 0x8000, 0x2000, 0x4fd5b385);
            ROM_LOAD("sl_03.1c", 0xa000, 0x2000, 0xf26cd7ca);
            ROM_LOAD("sl_02.1b", 0xc000, 0x2000, 0x9308f34e);
            ROM_LOAD("sl_01.1a", 0xe000, 0x2000, 0xd87296e4);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("sl_06.2b", 0x0000, 0x2000, 0x25f0e027);
            ROM_LOAD("sl_05.2a", 0x2000, 0x2000, 0x2bbb45fd);
            ROM_END();
        }
    };

    static RomLoadPtr rom_vstetris = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("a000.6c", 0xa000, 0x2000, 0x92a1cf10);
            ROM_LOAD("c000.6b", 0xc000, 0x2000, 0x9e9cda9d);
            ROM_LOAD("e000.6a", 0xe000, 0x2000, 0xbfeaf6c1);

            ROM_REGION(0x2000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("char.8b", 0x0000, 0x2000, 0x51e8d403);

            ROM_END();
        }
    };

    static RomLoadPtr rom_drmario = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x20000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("dm-uiprg", 0x10000, 0x10000, 0xd5d7eac4);

            ROM_REGION(0x8000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("dm-u3chr", 0x0000, 0x8000, 0x91871aa5);
            ROM_END();
        }
    };

    static RomLoadPtr rom_cstlevna = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x30000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("mds-cv.prg", 0x10000, 0x20000, 0xffbef374);

            /* No cart gfx - uses vram */
            ROM_END();
        }
    };

    static RomLoadPtr rom_tkoboxng = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x20000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("tkoprg.bin", 0x10000, 0x10000, 0xeb2dba63);

            ROM_REGION(0x10000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("tkochr.bin", 0x0000, 0x10000, 0x21275ba5);
            ROM_END();
        }
    };

    /* not working yet */
    static RomLoadPtr rom_topgun = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x30000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("rc-003", 0x10000, 0x20000, 0x8c0c2df5);

            /* No cart gfx - uses vram */
            ROM_END();
        }
    };

    static RomLoadPtr rom_rbibb = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x20000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("rbi-prg", 0x10000, 0x10000, 0x135adf7c);

            ROM_REGION(0x8000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("rbi-cha", 0x0000, 0x8000, 0xa3c14889);
            ROM_END();
        }
    };

    static RomLoadPtr rom_vsskykid = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("sk-prg1", 0x08000, 0x08000, 0xcf36261e);

            ROM_REGION(0x8000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("sk-cha", 0x0000, 0x8000, 0x9bd44dad);
            ROM_END();
        }
    };

    static RomLoadPtr rom_platoon = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x30000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("prgver0.ic4", 0x10000, 0x20000, 0xe2c0a2be);

            ROM_REGION(0x20000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("chrver0.ic6", 0x00000, 0x20000, 0x689df57d);
            ROM_END();
        }
    };

    static RomLoadPtr rom_vsxevus = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x30000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("prg2n", 0x10000, 0x10000, 0xe2c0a2be);
            ROM_LOAD("prg1", 0x20000, 0x10000, 0xe2c0a2be);

            ROM_REGION(0x8000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("cha", 0x00000, 0x8000, 0x689df57d);

            ROM_END();
        }
    };

    /* Dual System */
    static RomLoadPtr rom_balonfgt = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("bf.1d", 0x08000, 0x02000, 0x1248a6d6);
            ROM_LOAD("bf.1c", 0x0a000, 0x02000, 0x14af0e42);
            ROM_LOAD("bf.1b", 0x0c000, 0x02000, 0xa420babf);
            ROM_LOAD("bf.1a", 0x0e000, 0x02000, 0x9c31f94d);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("bf.2b", 0x0000, 0x2000, 0xf27d9aa0);
            ROM_LOAD("bf.2a", 0x2000, 0x2000, 0x76e6bbf8);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 6502 memory */
            ROM_LOAD("bf.6d", 0x08000, 0x02000, 0xef4ebff1);
            ROM_LOAD("bf.6c", 0x0a000, 0x02000, 0x14af0e42);
            ROM_LOAD("bf.6b", 0x0c000, 0x02000, 0xa420babf);
            ROM_LOAD("bf.6a", 0x0e000, 0x02000, 0x3aa5c095);

            ROM_REGION(0x4000, REGION_GFX2, 0);/* PPU memory */
            ROM_LOAD("bf.8b", 0x0000, 0x2000, 0xf27d9aa0);
            ROM_LOAD("bf.8a", 0x2000, 0x2000, 0x76e6bbf8);

            ROM_END();
        }
    };

    static RomLoadPtr rom_vsmahjng = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("mj.1c", 0x0a000, 0x02000, 0xec77671f);
            ROM_LOAD("mj.1b", 0x0c000, 0x02000, 0xac53398b);
            ROM_LOAD("mj.1a", 0x0e000, 0x02000, 0x62f0df8e);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("mj.2b", 0x0000, 0x2000, 0x9dae3502);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 6502 memory */
            ROM_LOAD("mj.6c", 0x0a000, 0x02000, 0x3cee11e9);
            ROM_LOAD("mj.6b", 0x0c000, 0x02000, 0xe8341f7b);
            ROM_LOAD("mj.6a", 0x0e000, 0x02000, 0x0ee69f25);

            ROM_REGION(0x4000, REGION_GFX2, 0);/* PPU memory */
            ROM_LOAD("mj.8b", 0x0000, 0x2000, 0x9dae3502);

            ROM_END();
        }
    };

    static RomLoadPtr rom_vsbball = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("bb-1d", 0x08000, 0x02000, 0x0cc5225f);
            ROM_LOAD("bb-1c", 0x0a000, 0x02000, 0x9856ac60);
            ROM_LOAD("bb-1b", 0x0c000, 0x02000, 0xd1312e63);
            ROM_LOAD("bb-1a", 0x0e000, 0x02000, 0x28199b4d);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("bb-2b", 0x0000, 0x2000, 0x3ff8bec3);
            ROM_LOAD("bb-2a", 0x2000, 0x2000, 0xebb88502);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 6502 memory */
            ROM_LOAD("bb-6d", 0x08000, 0x02000, 0x7ec792bc);
            ROM_LOAD("bb-6c", 0x0a000, 0x02000, 0xb631f8aa);
            ROM_LOAD("bb-6b", 0x0c000, 0x02000, 0xc856b45a);
            ROM_LOAD("bb-6a", 0x0e000, 0x02000, 0x06b74c18);

            ROM_REGION(0x4000, REGION_GFX2, 0);/* PPU memory */
            ROM_LOAD("bb-8b", 0x0000, 0x2000, 0x3ff8bec3);
            ROM_LOAD("bb-8a", 0x2000, 0x2000, 0x13b20cfd);
            ROM_END();
        }
    };

    static RomLoadPtr rom_vsbballj = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("ba_1d_a1.bin", 0x08000, 0x02000, 0x6dbc129b);
            ROM_LOAD("ba_1c_a1.bin", 0x0a000, 0x02000, 0x2a684b3a);
            ROM_LOAD("ba_1b_a1.bin", 0x0c000, 0x02000, 0x7ca0f715);
            ROM_LOAD("ba_1a_a1.bin", 0x0e000, 0x02000, 0x926bb4fc);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("ba_2b_a.bin", 0x0000, 0x2000, 0x919147d0);
            ROM_LOAD("ba_2a_a.bin", 0x2000, 0x2000, 0x3f7edb00);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 6502 memory */
            ROM_LOAD("ba_6d_a1.bin", 0x08000, 0x02000, 0xd534dca4);
            ROM_LOAD("ba_6c_a1.bin", 0x0a000, 0x02000, 0x73904bbc);
            ROM_LOAD("ba_6b_a1.bin", 0x0c000, 0x02000, 0x7c130724);
            ROM_LOAD("ba_6a_a1.bin", 0x0e000, 0x02000, 0xd938080e);

            ROM_REGION(0x4000, REGION_GFX2, 0);/* PPU memory */
            ROM_LOAD("ba_8b_a.bin", 0x0000, 0x2000, 0x919147d0);
            ROM_LOAD("ba_8a_a.bin", 0x2000, 0x2000, 0x3f7edb00);
            ROM_END();
        }
    };

    static RomLoadPtr rom_vsbbalja = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("ba_1d_a2.bin", 0x08000, 0x02000, 0xf3820b70);
            ROM_LOAD("ba_1c_a2.bin", 0x0a000, 0x02000, 0x39fbbf28);
            ROM_LOAD("ba_1b_a2.bin", 0x0c000, 0x02000, 0xb1377b12);
            ROM_LOAD("ba_1a_a2.bin", 0x0e000, 0x02000, 0x08fab347);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("ba_2b_a.bin", 0x0000, 0x2000, 0x919147d0);
            ROM_LOAD("ba_2a_a.bin", 0x2000, 0x2000, 0x3f7edb00);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 6502 memory */
            ROM_LOAD("ba_6d_a2.bin", 0x08000, 0x02000, 0xc69561b0);
            ROM_LOAD("ba_6c_a2.bin", 0x0a000, 0x02000, 0x17d1ca39);
            ROM_LOAD("ba_6b_a2.bin", 0x0c000, 0x02000, 0x37481900);
            ROM_LOAD("ba_6a_a2.bin", 0x0e000, 0x02000, 0xa44ffc4b);

            ROM_REGION(0x4000, REGION_GFX2, 0);/* PPU memory */
            ROM_LOAD("ba_8b_a.bin", 0x0000, 0x2000, 0x919147d0);
            ROM_LOAD("ba_8a_a.bin", 0x2000, 0x2000, 0x3f7edb00);
            ROM_END();
        }
    };

    static RomLoadPtr rom_vstennis = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("vst-1d", 0x08000, 0x02000, 0xf4e9fca0);
            ROM_LOAD("vst-1c", 0x0a000, 0x02000, 0x7e52df58);
            ROM_LOAD("vst-1b", 0x0c000, 0x02000, 0x1a0d809a);
            ROM_LOAD("vst-1a", 0x0e000, 0x02000, 0x8483a612);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("vst-2b", 0x0000, 0x2000, 0x9de19c9c);
            ROM_LOAD("vst-2a", 0x2000, 0x2000, 0x67a5800e);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 6502 memory */
            ROM_LOAD("vst-6d", 0x08000, 0x02000, 0x3131b1bf);
            ROM_LOAD("vst-6c", 0x0a000, 0x02000, 0x27195d13);
            ROM_LOAD("vst-6b", 0x0c000, 0x02000, 0x4b4e26ca);
            ROM_LOAD("vst-6a", 0x0e000, 0x02000, 0xb6bfee07);

            ROM_REGION(0x4000, REGION_GFX2, 0);/* PPU memory */
            ROM_LOAD("vst-8b", 0x0000, 0x2000, 0xc81e9260);
            ROM_LOAD("vst-8a", 0x2000, 0x2000, 0xd91eb295);
            ROM_END();
        }
    };

    static RomLoadPtr rom_wrecking = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("wr.1d", 0x08000, 0x02000, 0x8897e1b9);
            ROM_LOAD("wr.1c", 0x0a000, 0x02000, 0xd4dc5ebb);
            ROM_LOAD("wr.1b", 0x0c000, 0x02000, 0x8ee4a454);
            ROM_LOAD("wr.1a", 0x0e000, 0x02000, 0x63d6490a);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("wr.2b", 0x0000, 0x2000, 0x455d77ac);
            ROM_LOAD("wr.2a", 0x2000, 0x2000, 0x653350d8);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 6502 memory */
            ROM_LOAD("wr.6d", 0x08000, 0x02000, 0x90e49ce7);
            ROM_LOAD("wr.6c", 0x0a000, 0x02000, 0xa12ae745);
            ROM_LOAD("wr.6b", 0x0c000, 0x02000, 0x03947ca9);
            ROM_LOAD("wr.6a", 0x0e000, 0x02000, 0x2c0a13ac);

            ROM_REGION(0x4000, REGION_GFX2, 0);/* PPU memory */
            ROM_LOAD("wr.8b", 0x0000, 0x2000, 0x455d77ac);
            ROM_LOAD("wr.8a", 0x2000, 0x2000, 0x653350d8);
            ROM_END();
        }
    };

    static RomLoadPtr rom_iceclmrj = new RomLoadPtr() {
        public void handler() {

            ROM_REGION(0x10000, REGION_CPU1, 0);/* 6502 memory */
            ROM_LOAD("ic4-41da.bin", 0x08000, 0x02000, 0x94e3197d);
            ROM_LOAD("ic4-41ca.bin", 0x0a000, 0x02000, 0xb253011e);
            ROM_LOAD("ic441ba1.bin", 0x0c000, 0x02000, 0xf3795874);
            ROM_LOAD("ic4-41aa.bin", 0x0e000, 0x02000, 0x094c246c);

            ROM_REGION(0x4000, REGION_GFX1, 0);/* PPU memory */
            ROM_LOAD("ic4-42ba.bin", 0x0000, 0x2000, 0x331460b4);
            ROM_LOAD("ic4-42aa.bin", 0x2000, 0x2000, 0x4ec44fb3);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 6502 memory */
            ROM_LOAD("ic4-46da.bin", 0x08000, 0x02000, 0x94e3197d);
            ROM_LOAD("ic4-46ca.bin", 0x0a000, 0x02000, 0xb253011e);
            ROM_LOAD("ic4-46ba.bin", 0x0c000, 0x02000, 0x2ee9c1f9);
            ROM_LOAD("ic4-46aa.bin", 0x0e000, 0x02000, 0x094c246c);

            ROM_REGION(0x4000, REGION_GFX2, 0);/* PPU memory */
            ROM_LOAD("ic4-48ba.bin", 0x0000, 0x2000, 0x331460b4);
            ROM_LOAD("ic4-48aa.bin", 0x2000, 0x2000, 0x4ec44fb3);

            ROM_END();
        }
    };

    /**
     * ***************************************************************************
     */
    /*    YEAR  NAME      PARENT    MACHINE  INPUT     INIT  	   MONITOR  */
    public static GameDriver driver_btlecity = new GameDriver("1985", "btlecity", "vsnes.java", rom_btlecity, null, machine_driver_vsnes, input_ports_btlecity, init_vsnormal, ROT0, "Namco", "Battle City", GAME_WRONG_COLORS);
    public static GameDriver driver_starlstr = new GameDriver("1985", "starlstr", "vsnes.java", rom_starlstr, null, machine_driver_vsnes, input_ports_vsnes, init_vsnormal, ROT0, "Namco", "Star Luster");
    public static GameDriver driver_cstlevna = new GameDriver("1987", "cstlevna", "vsnes.java", rom_cstlevna, null, machine_driver_vsnes, input_ports_cstlevna, init_cstlevna, ROT0, "Konami", "Vs. Castlevania");
    public static GameDriver driver_cluclu = new GameDriver("1984", "cluclu", "vsnes.java", rom_cluclu, null, machine_driver_vsnes, input_ports_cluclu, init_suprmrio, ROT0, "Nintendo", "Clu Clu Land");
    public static GameDriver driver_drmario = new GameDriver("1990", "drmario", "vsnes.java", rom_drmario, null, machine_driver_vsnes, input_ports_drmario, init_drmario, ROT0, "Nintendo", "Dr. Mario");
    public static GameDriver driver_duckhunt = new GameDriver("1985", "duckhunt", "vsnes.java", rom_duckhunt, null, machine_driver_vsnes, input_ports_duckhunt, init_duckhunt, ROT0, "Nintendo", "Duck Hunt");
    public static GameDriver driver_excitebk = new GameDriver("1984", "excitebk", "vsnes.java", rom_excitebk, null, machine_driver_vsnes, input_ports_excitebk, init_excitebk, ROT0, "Nintendo", "Excitebike");
    public static GameDriver driver_excitbkj = new GameDriver("1984", "excitbkj", "vsnes.java", rom_excitbkj, driver_excitebk, machine_driver_vsnes, input_ports_excitebk, init_excitbkj, ROT0, "Nintendo", "Excitebike (Japan)");
    public static GameDriver driver_goonies = new GameDriver("1986", "goonies", "vsnes.java", rom_goonies, null, machine_driver_vsnes, input_ports_vsnes, init_goonies, ROT0, "Konami", "Vs. The Goonies");
    public static GameDriver driver_hogalley = new GameDriver("1985", "hogalley", "vsnes.java", rom_hogalley, null, machine_driver_vsnes, input_ports_hogalley, init_hogalley, ROT0, "Nintendo", "Hogan's Alley");
    public static GameDriver driver_iceclimb = new GameDriver("1984", "iceclimb", "vsnes.java", rom_iceclimb, null, machine_driver_vsnes, input_ports_iceclimb, init_suprmrio, ROT0, "Nintendo", "Ice Climber");
    public static GameDriver driver_ladygolf = new GameDriver("1984", "ladygolf", "vsnes.java", rom_ladygolf, null, machine_driver_vsnes, input_ports_golf, init_machridr, ROT0, "Nintendo", "Stroke and Match Golf (Ladies Version)");
    public static GameDriver driver_machridr = new GameDriver("1985", "machridr", "vsnes.java", rom_machridr, null, machine_driver_vsnes, input_ports_machridr, init_machridr, ROT0, "Nintendo", "Mach Rider");
    public static GameDriver driver_rbibb = new GameDriver("1986", "rbibb", "vsnes.java", rom_rbibb, null, machine_driver_vsnes, input_ports_rbibb, init_rbibb, ROT0, "Namco", "Atari RBI Baseball");
    public static GameDriver driver_suprmrio = new GameDriver("1986", "suprmrio", "vsnes.java", rom_suprmrio, null, machine_driver_vsnes, input_ports_suprmrio, init_suprmrio, ROT0, "Nintendo", "Vs. Super Mario Bros");
    public static GameDriver driver_vsskykid = new GameDriver("1985", "vsskykid", "vsnes.java", rom_vsskykid, null, machine_driver_vsnes, input_ports_vsskykid, init_vsskykid, ROT0, "Namco", "Super SkyKid");
    public static GameDriver driver_tkoboxng = new GameDriver("1987", "tkoboxng", "vsnes.java", rom_tkoboxng, null, machine_driver_vsnes, input_ports_vsnes, init_tkoboxng, ROT0, "Namco LTD.", "Vs. TKO Boxing", GAME_WRONG_COLORS);
    public static GameDriver driver_smgolf = new GameDriver("1984", "smgolf", "vsnes.java", rom_smgolf, null, machine_driver_vsnes, input_ports_golf, init_machridr, ROT0, "Nintendo", "Stroke and Match Golf (Men's Version)");
    public static GameDriver driver_smgolfj = new GameDriver("1984", "smgolfj", "vsnes.java", rom_smgolfj, driver_smgolf, machine_driver_vsnes, input_ports_golf, init_vsnormal, ROT0, "Nintendo", "Stroke and Match Golf (Japan)");
    public static GameDriver driver_vspinbal = new GameDriver("1984", "vspinbal", "vsnes.java", rom_vspinbal, null, machine_driver_vsnes, input_ports_vsnes, init_vspinbal, ROT0, "Nintendo", "Pinball");
    public static GameDriver driver_vspinblj = new GameDriver("1984", "vspinblj", "vsnes.java", rom_vspinblj, driver_vspinbal, machine_driver_vsnes, input_ports_vsnes, init_vsnormal, ROT0, "Nintendo", "Pinball (Japan)");
    public static GameDriver driver_vsslalom = new GameDriver("1986", "vsslalom", "vsnes.java", rom_vsslalom, null, machine_driver_vsnes, input_ports_vsnes, init_vsslalom, ROT0, "Rare LTD.", "Vs. Slalom");
    public static GameDriver driver_vssoccer = new GameDriver("1985", "vssoccer", "vsnes.java", rom_vssoccer, null, machine_driver_vsnes, input_ports_vsnes, init_excitebk, ROT0, "Nintendo", "Soccer");
    public static GameDriver driver_vsgradus = new GameDriver("1986", "vsgradus", "vsnes.java", rom_vsgradus, null, machine_driver_vsnes, input_ports_vsnes, init_vsgradus, ROT0, "Konami", "Vs. Gradius");
    public static GameDriver driver_platoon = new GameDriver("1987", "platoon", "vsnes.java", rom_platoon, null, machine_driver_vsnes, input_ports_platoon, init_platoon, ROT0, "Ocean Software Limited", "Platoon", GAME_WRONG_COLORS);
    public static GameDriver driver_vstetris = new GameDriver("1987", "vstetris", "vsnes.java", rom_vstetris, null, machine_driver_vsnes, input_ports_vstetris, init_vspinbal, ROT0, "Academysoft-Elory", "Vs. Tetris", GAME_WRONG_COLORS);

    /* Dual games */
    public static GameDriver driver_vstennis = new GameDriver("1984", "vstennis", "vsnes.java", rom_vstennis, null, machine_driver_vsdual, input_ports_vstennis, init_vstennis, ROT0, "Nintendo", "Vs. Tennis");
    public static GameDriver driver_wrecking = new GameDriver("1984", "wrecking", "vsnes.java", rom_wrecking, null, machine_driver_vsdual, input_ports_wrecking, init_wrecking, ROT0, "Nintendo", "Vs. Wrecking Crew");
    public static GameDriver driver_balonfgt = new GameDriver("1984", "balonfgt", "vsnes.java", rom_balonfgt, null, machine_driver_vsdual, input_ports_balonfgt, init_balonfgt, ROT0, "Nintendo", "Vs. Baloon Fight");
    public static GameDriver driver_vsmahjng = new GameDriver("1984", "vsmahjng", "vsnes.java", rom_vsmahjng, null, machine_driver_vsdual, input_ports_vsmahjng, init_vstennis, ROT0, "Nintendo", "Vs. Mahjang");
    public static GameDriver driver_vsbball = new GameDriver("1984", "vsbball", "vsnes.java", rom_vsbball, null, machine_driver_vsdual, input_ports_vsbball, init_vsbball, ROT0, "Nintendo of America", "Vs. BaseBall");
    public static GameDriver driver_vsbballj = new GameDriver("1984", "vsbballj", "vsnes.java", rom_vsbballj, driver_vsbball, machine_driver_vsdual, input_ports_vsbballj, init_vsbball, ROT0, "Nintendo of America", "Vs. BaseBall (Japan set 1)");
    public static GameDriver driver_vsbbalja = new GameDriver("1984", "vsbbalja", "vsnes.java", rom_vsbbalja, driver_vsbball, machine_driver_vsdual, input_ports_vsbballj, init_vsbball, ROT0, "Nintendo of America", "Vs. BaseBall (Japan set 2)");
    public static GameDriver driver_iceclmrj = new GameDriver("1984", "iceclmrj", "vsnes.java", rom_iceclmrj, null, machine_driver_vsdual, input_ports_iceclmrj, init_iceclmrj, ROT0, "Nintendo", "Ice Climber Dual (Japan)");

    /* are these using the correct mappers? */
    public static GameDriver driver_topgun = new GameDriver("19??", "topgun", "vsnes.java", rom_topgun, null, machine_driver_vsnes, input_ports_vsnes, init_vstopgun, ROT0, "Nintendo", "VS Topgun", GAME_NOT_WORKING);
    public static GameDriver driver_jajamaru = new GameDriver("19??", "jajamaru", "vsnes.java", rom_jajamaru, null, machine_driver_vsnes, input_ports_vsnes, init_vsnormal, ROT0, "Nintendo", "JAJARU", GAME_NOT_WORKING);
    public static GameDriver driver_vsxevus = new GameDriver("19??", "vsxevus", "vsnes.java", rom_vsxevus, null, machine_driver_vsnes, input_ports_vsnes, init_xevious, ROT0, "Namco?", "Xevious", GAME_NOT_WORKING);

}
