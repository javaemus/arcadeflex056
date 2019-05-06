/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.drivers;

import static arcadeflex056.fucPtr.*;

import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.inptportH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.memoryH.*;
import static mame056.drawgfxH.*;
import static mame056.sndintrfH.*;

import static mame056.drivers.galaxian.*;
import static mame056.drivers.scobra.*;

import static mame056.machine.scramble.*;

import static mame056.vidhrdw.galaxian.*;

public class amidar {

    public static WriteHandlerPtr amidar_coina_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            coin_counter_w.handler(0, data);
            coin_counter_w.handler(0, 0);
        }
    };

    public static WriteHandlerPtr amidar_coinb_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            coin_counter_w.handler(1, data);
            coin_counter_w.handler(1, 0);
        }
    };

    public static Memory_ReadAddress readmem[] = {
        new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_ReadAddress(0x0000, 0x7fff, MRA_ROM),
        new Memory_ReadAddress(0x8000, 0x87ff, MRA_RAM),
        new Memory_ReadAddress(0x9000, 0x93ff, MRA_RAM),
        new Memory_ReadAddress(0x9800, 0x98ff, MRA_RAM),
        new Memory_ReadAddress(0xa800, 0xa800, watchdog_reset_r),
        new Memory_ReadAddress(0xb000, 0xb03f, amidar_ppi8255_0_r),
        new Memory_ReadAddress(0xb800, 0xb83f, amidar_ppi8255_1_r),
        new Memory_ReadAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_WriteAddress writemem[] = {
        new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_WriteAddress(0x0000, 0x7fff, MWA_ROM),
        new Memory_WriteAddress(0x8000, 0x87ff, MWA_RAM),
        new Memory_WriteAddress(0x9000, 0x93ff, MWA_RAM, galaxian_videoram),
        new Memory_WriteAddress(0x9800, 0x983f, MWA_RAM, galaxian_attributesram),
        new Memory_WriteAddress(0x9840, 0x985f, MWA_RAM, galaxian_spriteram, galaxian_spriteram_size),
        new Memory_WriteAddress(0x9860, 0x98ff, MWA_RAM),
        new Memory_WriteAddress(0xa000, 0xa000, scramble_background_red_w),
        new Memory_WriteAddress(0xa008, 0xa008, interrupt_enable_w),
        new Memory_WriteAddress(0xa010, 0xa010, galaxian_flip_screen_x_w),
        new Memory_WriteAddress(0xa018, 0xa018, galaxian_flip_screen_y_w),
        new Memory_WriteAddress(0xa020, 0xa020, scramble_background_green_w),
        new Memory_WriteAddress(0xa028, 0xa028, scramble_background_blue_w),
        new Memory_WriteAddress(0xa030, 0xa030, amidar_coina_w),
        new Memory_WriteAddress(0xa038, 0xa038, amidar_coinb_w),
        new Memory_WriteAddress(0xb000, 0xb03f, amidar_ppi8255_0_w),
        new Memory_WriteAddress(0xb800, 0xb83f, amidar_ppi8255_1_w),
        new Memory_WriteAddress(MEMPORT_MARKER, 0)
    };

    static InputPortPtr input_ports_amidar = new InputPortPtr() {
        public void handler() {
            PORT_START();
            /* IN0 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);/* probably space for button 2 */
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_COIN3);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_BUTTON1);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_COIN2);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_COIN1);

            PORT_START();
            /* IN1 */
            PORT_DIPNAME(0x03, 0x03, DEF_STR("Lives"));
            PORT_DIPSETTING(0x03, "3");
            PORT_DIPSETTING(0x02, "4");
            PORT_DIPSETTING(0x01, "5");
            PORT_BITX(0, 0x00, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "255", IP_KEY_NONE, IP_JOY_NONE);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);/* probably space for player 2 button 2 */
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_START2);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_START1);

            PORT_START();
            /* IN2 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY | IPF_COCKTAIL);
            PORT_DIPNAME(0x02, 0x00, DEF_STR("Demo_Sounds"));
            PORT_DIPSETTING(0x02, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Bonus_Life"));
            PORT_DIPSETTING(0x00, "30000 50000");
            PORT_DIPSETTING(0x04, "50000 50000");
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Cabinet"));
            PORT_DIPSETTING(0x00, DEF_STR("Upright"));
            PORT_DIPSETTING(0x08, DEF_STR("Cocktail"));
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY);
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x20, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY);
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x80, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));

            PORT_START();
            /* DSW */
            PORT_DIPNAME(0x0f, 0x0f, DEF_STR("Coin_A"));
            PORT_DIPSETTING(0x04, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x0a, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x01, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x02, DEF_STR("3C_2C"));
            PORT_DIPSETTING(0x08, DEF_STR("4C_3C"));
            PORT_DIPSETTING(0x0f, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x0c, DEF_STR("3C_4C"));
            PORT_DIPSETTING(0x0e, DEF_STR("2C_3C"));
            PORT_DIPSETTING(0x07, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x06, DEF_STR("2C_5C"));
            PORT_DIPSETTING(0x0b, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x03, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0x0d, DEF_STR("1C_5C"));
            PORT_DIPSETTING(0x05, DEF_STR("1C_6C"));
            PORT_DIPSETTING(0x09, DEF_STR("1C_7C"));
            PORT_DIPSETTING(0x00, DEF_STR("Free_Play"));
            PORT_DIPNAME(0xf0, 0xf0, DEF_STR("Coin_B"));
            PORT_DIPSETTING(0x40, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0xa0, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x10, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x20, DEF_STR("3C_2C"));
            PORT_DIPSETTING(0x80, DEF_STR("4C_3C"));
            PORT_DIPSETTING(0xf0, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0xc0, DEF_STR("3C_4C"));
            PORT_DIPSETTING(0xe0, DEF_STR("2C_3C"));
            PORT_DIPSETTING(0x70, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x60, DEF_STR("2C_5C"));
            PORT_DIPSETTING(0xb0, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x30, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0xd0, DEF_STR("1C_5C"));
            PORT_DIPSETTING(0x50, DEF_STR("1C_6C"));
            PORT_DIPSETTING(0x90, DEF_STR("1C_7C"));
            PORT_DIPSETTING(0x00, "Disable All Coins");
            INPUT_PORTS_END();
        }
    };

    /* absolutely identical to amidar, the only difference is the BONUS dip switch */
    static InputPortPtr input_ports_amidaru = new InputPortPtr() {
        public void handler() {
            PORT_START();
            /* IN0 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);/* probably space for button 2 */
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_COIN3);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_BUTTON1);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_COIN2);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_COIN1);

            PORT_START();
            /* IN1 */
            PORT_DIPNAME(0x03, 0x03, DEF_STR("Lives"));
            PORT_DIPSETTING(0x03, "3");
            PORT_DIPSETTING(0x02, "4");
            PORT_DIPSETTING(0x01, "5");
            PORT_BITX(0, 0x00, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "255", IP_KEY_NONE, IP_JOY_NONE);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);/* probably space for player 2 button 2 */
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_START2);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_START1);

            PORT_START();
            /* IN2 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY | IPF_COCKTAIL);
            PORT_DIPNAME(0x02, 0x00, DEF_STR("Demo_Sounds"));
            PORT_DIPSETTING(0x02, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Bonus_Life"));
            PORT_DIPSETTING(0x00, "30000 70000");
            PORT_DIPSETTING(0x04, "50000 80000");
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Cabinet"));
            PORT_DIPSETTING(0x00, DEF_STR("Upright"));
            PORT_DIPSETTING(0x08, DEF_STR("Cocktail"));
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY);
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x20, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY);
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x80, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));

            PORT_START();
            /* DSW */
            PORT_DIPNAME(0x0f, 0x0f, DEF_STR("Coin_A"));
            PORT_DIPSETTING(0x04, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x0a, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x01, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x02, DEF_STR("3C_2C"));
            PORT_DIPSETTING(0x08, DEF_STR("4C_3C"));
            PORT_DIPSETTING(0x0f, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x0c, DEF_STR("3C_4C"));
            PORT_DIPSETTING(0x0e, DEF_STR("2C_3C"));
            PORT_DIPSETTING(0x07, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x06, DEF_STR("2C_5C"));
            PORT_DIPSETTING(0x0b, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x03, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0x0d, DEF_STR("1C_5C"));
            PORT_DIPSETTING(0x05, DEF_STR("1C_6C"));
            PORT_DIPSETTING(0x09, DEF_STR("1C_7C"));
            PORT_DIPSETTING(0x00, DEF_STR("Free_Play"));
            PORT_DIPNAME(0xf0, 0xf0, DEF_STR("Coin_B"));
            PORT_DIPSETTING(0x40, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0xa0, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x10, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x20, DEF_STR("3C_2C"));
            PORT_DIPSETTING(0x80, DEF_STR("4C_3C"));
            PORT_DIPSETTING(0xf0, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0xc0, DEF_STR("3C_4C"));
            PORT_DIPSETTING(0xe0, DEF_STR("2C_3C"));
            PORT_DIPSETTING(0x70, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x60, DEF_STR("2C_5C"));
            PORT_DIPSETTING(0xb0, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x30, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0xd0, DEF_STR("1C_5C"));
            PORT_DIPSETTING(0x50, DEF_STR("1C_6C"));
            PORT_DIPSETTING(0x90, DEF_STR("1C_7C"));
            PORT_DIPSETTING(0x00, "Disable All Coins");
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_amidaro = new InputPortPtr() {
        public void handler() {
            PORT_START();
            /* IN0 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);/* probably space for button 2 */
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_COIN3);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_BUTTON1);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_COIN2);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_COIN1);

            PORT_START();
            /* IN1 */
            PORT_DIPNAME(0x03, 0x01, DEF_STR("Lives"));
            PORT_DIPSETTING(0x03, "1");
            PORT_DIPSETTING(0x02, "2");
            PORT_DIPSETTING(0x01, "3");
            PORT_DIPSETTING(0x00, "4");
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);/* probably space for player 2 button 2 */
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_START2);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_START1);

            PORT_START();
            /* IN2 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY | IPF_COCKTAIL);
            PORT_DIPNAME(0x02, 0x00, "Level Progression");
            PORT_DIPSETTING(0x00, "Slow");
            PORT_DIPSETTING(0x02, "Fast");
            PORT_DIPNAME(0x04, 0x00, DEF_STR("Bonus_Life"));
            PORT_DIPSETTING(0x00, "30000 70000");
            PORT_DIPSETTING(0x04, "50000 80000");
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Cabinet"));
            PORT_DIPSETTING(0x00, DEF_STR("Upright"));
            PORT_DIPSETTING(0x08, DEF_STR("Cocktail"));
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY);
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x20, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY);
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x80, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));

            PORT_START();
            /* DSW */
            PORT_DIPNAME(0x0f, 0x0f, DEF_STR("Coin_A"));
            PORT_DIPSETTING(0x04, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x0a, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x01, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x02, DEF_STR("3C_2C"));
            PORT_DIPSETTING(0x08, DEF_STR("4C_3C"));
            PORT_DIPSETTING(0x0f, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x0c, DEF_STR("3C_4C"));
            PORT_DIPSETTING(0x0e, DEF_STR("2C_3C"));
            PORT_DIPSETTING(0x07, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x06, DEF_STR("2C_5C"));
            PORT_DIPSETTING(0x0b, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x03, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0x0d, DEF_STR("1C_5C"));
            PORT_DIPSETTING(0x05, DEF_STR("1C_6C"));
            PORT_DIPSETTING(0x09, DEF_STR("1C_7C"));
            PORT_DIPSETTING(0x00, DEF_STR("Free_Play"));
            PORT_DIPNAME(0xf0, 0xf0, DEF_STR("Coin_B"));
            PORT_DIPSETTING(0x40, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0xa0, DEF_STR("3C_1C"));
            PORT_DIPSETTING(0x10, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x20, DEF_STR("3C_2C"));
            PORT_DIPSETTING(0x80, DEF_STR("4C_3C"));
            PORT_DIPSETTING(0xf0, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0xc0, DEF_STR("3C_4C"));
            PORT_DIPSETTING(0xe0, DEF_STR("2C_3C"));
            PORT_DIPSETTING(0x70, DEF_STR("1C_2C"));
            PORT_DIPSETTING(0x60, DEF_STR("2C_5C"));
            PORT_DIPSETTING(0xb0, DEF_STR("1C_3C"));
            PORT_DIPSETTING(0x30, DEF_STR("1C_4C"));
            PORT_DIPSETTING(0xd0, DEF_STR("1C_5C"));
            PORT_DIPSETTING(0x50, DEF_STR("1C_6C"));
            PORT_DIPSETTING(0x90, DEF_STR("1C_7C"));
            PORT_DIPSETTING(0x00, "Disable All Coins");
            INPUT_PORTS_END();
        }
    };

    /* similar to Amidar, dip switches are different and port 3, which in Amidar */
 /* selects coins per credit, is not used. */
    static InputPortPtr input_ports_turtles = new InputPortPtr() {
        public void handler() {
            PORT_START();
            /* IN0 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_COCKTAIL);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);/* probably space for button 2 */
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_COIN3);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_BUTTON1);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_COIN2);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_COIN1);

            PORT_START();
            /* IN1 */
            PORT_DIPNAME(0x03, 0x01, DEF_STR("Lives"));
            PORT_DIPSETTING(0x00, "3");
            PORT_DIPSETTING(0x01, "4");
            PORT_DIPSETTING(0x02, "5");
            PORT_BITX(0, 0x03, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "126", IP_KEY_NONE, IP_JOY_NONE);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);/* probably space for player 2 button 2 */
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_COCKTAIL);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_START2);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_START1);

            PORT_START();
            /* IN2 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_COCKTAIL);
            PORT_DIPNAME(0x06, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x00, "A 1/1 B 2/1 C 1/1");
            PORT_DIPSETTING(0x02, "A 1/2 B 1/1 C 1/2");
            PORT_DIPSETTING(0x04, "A 1/3 B 3/1 C 1/3");
            PORT_DIPSETTING(0x06, "A 1/4 B 4/1 C 1/4");
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Cabinet"));
            PORT_DIPSETTING(0x00, DEF_STR("Upright"));
            PORT_DIPSETTING(0x08, DEF_STR("Cocktail"));
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY);
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x20, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY);
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x80, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    /* same as Turtles, but dip switches are different. */
    static InputPortPtr input_ports_turpin = new InputPortPtr() {
        public void handler() {
            PORT_START();
            /* IN0 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);/* probably space for button 2 */
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_COIN3);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_BUTTON1);
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
            PORT_BITX(0, 0x03, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "126", IP_KEY_NONE, IP_JOY_NONE);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);/* probably space for player 2 button 2 */
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_4WAY | IPF_COCKTAIL);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_START2);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_START1);

            PORT_START();
            /* IN2 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY | IPF_COCKTAIL);
            PORT_DIPNAME(0x06, 0x00, DEF_STR("Coinage"));
            PORT_DIPSETTING(0x06, DEF_STR("4C_1C"));
            PORT_DIPSETTING(0x02, DEF_STR("2C_1C"));
            PORT_DIPSETTING(0x00, DEF_STR("1C_1C"));
            PORT_DIPSETTING(0x04, DEF_STR("1C_2C"));
            PORT_DIPNAME(0x08, 0x00, DEF_STR("Cabinet"));
            PORT_DIPSETTING(0x00, DEF_STR("Upright"));
            PORT_DIPSETTING(0x08, DEF_STR("Cocktail"));
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_4WAY);
            PORT_DIPNAME(0x20, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x20, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_4WAY);
            PORT_DIPNAME(0x80, 0x00, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x80, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            INPUT_PORTS_END();
        }
    };

    static MachineDriver machine_driver_amidar = new MachineDriver(
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
                        scobra_sound_readmem, scobra_sound_writemem, scobra_sound_readport, scobra_sound_writeport,
                        ignore_interrupt, 1 /* interrupts are triggered by the main CPU */
                )
            },
            16000 / 132 / 2, 2500, /* frames per second, vblank duration */
            1, /* 1 CPU slice per frame - interleaving is forced when a sound command is written */
            scramble_init_machine,
            /* video hardware */
            32 * 8, 32 * 8, new rectangle(0 * 8, 32 * 8 - 1, 2 * 8, 30 * 8 - 1),
            galaxian_gfxdecodeinfo,
            32 + 64 + 2 + 8, 8 * 4, /* 32 for characters, 64 for stars, 2 for bullets, 8 for background */
            turtles_vh_convert_color_prom,
            VIDEO_TYPE_RASTER,
            null,
            turtles_vh_start,
            null,
            galaxian_vh_screenrefresh,
            /* sound hardware */
            0, 0, 0, 0,
            new MachineSound[]{
                new MachineSound(
                        SOUND_AY8910,
                        scobra_ay8910_interface
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
    static RomLoadPtr rom_amidar = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("amidar.2c", 0x0000, 0x1000, 0xc294bf27);
            ROM_LOAD("amidar.2e", 0x1000, 0x1000, 0xe6e96826);
            ROM_LOAD("amidar.2f", 0x2000, 0x1000, 0x3656be6f);
            ROM_LOAD("amidar.2h", 0x3000, 0x1000, 0x1be170bd);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 64k for the audio CPU */
            ROM_LOAD("amidar.5c", 0x0000, 0x1000, 0xc4b66ae4);
            ROM_LOAD("amidar.5d", 0x1000, 0x1000, 0x806785af);

            ROM_REGION(0x1000, REGION_GFX1, ROMREGION_DISPOSE);
            ROM_LOAD("amidar.5f", 0x0000, 0x0800, 0x5e51e84d);
            ROM_LOAD("amidar.5h", 0x0800, 0x0800, 0x2f7f1c30);

            ROM_REGION(0x0020, REGION_PROMS, 0);
            ROM_LOAD("amidar.clr", 0x0000, 0x0020, 0xf940dcc3);
            ROM_END();
        }
    };

    static RomLoadPtr rom_amidaru = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("amidarus.2c", 0x0000, 0x1000, 0x951e0792);
            ROM_LOAD("amidarus.2e", 0x1000, 0x1000, 0xa1a3a136);
            ROM_LOAD("amidarus.2f", 0x2000, 0x1000, 0xa5121bf5);
            ROM_LOAD("amidarus.2h", 0x3000, 0x1000, 0x051d1c7f);
            ROM_LOAD("amidarus.2j", 0x4000, 0x1000, 0x351f00d5);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 64k for the audio CPU */
            ROM_LOAD("amidarus.5c", 0x0000, 0x1000, 0x8ca7b750);
            ROM_LOAD("amidarus.5d", 0x1000, 0x1000, 0x9b5bdc0a);

            ROM_REGION(0x1000, REGION_GFX1, ROMREGION_DISPOSE);
            ROM_LOAD("amidarus.5f", 0x0000, 0x0800, 0x2cfe5ede);
            ROM_LOAD("amidarus.5h", 0x0800, 0x0800, 0x57c4fd0d);

            ROM_REGION(0x0020, REGION_PROMS, 0);
            ROM_LOAD("amidar.clr", 0x0000, 0x0020, 0xf940dcc3);
            ROM_END();
        }
    };

    static RomLoadPtr rom_amidaro = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("107.2cd", 0x0000, 0x1000, 0xc52536be);
            ROM_LOAD("108.2fg", 0x1000, 0x1000, 0x38538b98);
            ROM_LOAD("109.2fg", 0x2000, 0x1000, 0x69907f0f);
            ROM_LOAD("110.2h", 0x3000, 0x1000, 0xba149a93);
            ROM_LOAD("111.2j", 0x4000, 0x1000, 0x20d01c2e);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 64k for the audio CPU */
            ROM_LOAD("amidarus.5c", 0x0000, 0x1000, 0x8ca7b750);
            ROM_LOAD("amidarus.5d", 0x1000, 0x1000, 0x9b5bdc0a);

            ROM_REGION(0x1000, REGION_GFX1, ROMREGION_DISPOSE);
            ROM_LOAD("amidarus.5f", 0x0000, 0x0800, 0x2cfe5ede);
            ROM_LOAD("113.5h", 0x0800, 0x0800, 0xbcdce168);
            /* The letter 'S' is slightly different */

            ROM_REGION(0x0020, REGION_PROMS, 0);
            ROM_LOAD("amidar.clr", 0x0000, 0x0020, 0xf940dcc3);
            ROM_END();
        }
    };

    static RomLoadPtr rom_amigo = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("2732.a1", 0x0000, 0x1000, 0x930dc856);
            ROM_LOAD("2732.a2", 0x1000, 0x1000, 0x66282ff5);
            ROM_LOAD("2732.a3", 0x2000, 0x1000, 0xe9d3dc76);
            ROM_LOAD("2732.a4", 0x3000, 0x1000, 0x4a4086c9);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 64k for the audio CPU */
            ROM_LOAD("amidarus.5c", 0x0000, 0x1000, 0x8ca7b750);
            ROM_LOAD("amidarus.5d", 0x1000, 0x1000, 0x9b5bdc0a);

            ROM_REGION(0x1000, REGION_GFX1, ROMREGION_DISPOSE);
            ROM_LOAD("2716.a6", 0x0000, 0x0800, 0x2082ad0a);
            ROM_LOAD("2716.a5", 0x0800, 0x0800, 0x3029f94f);

            ROM_REGION(0x0020, REGION_PROMS, 0);
            ROM_LOAD("amidar.clr", 0x0000, 0x0020, 0xf940dcc3);
            ROM_END();
        }
    };

    static RomLoadPtr rom_turtles = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("turt_vid.2c", 0x0000, 0x1000, 0xec5e61fb);
            ROM_LOAD("turt_vid.2e", 0x1000, 0x1000, 0xfd10821e);
            ROM_LOAD("turt_vid.2f", 0x2000, 0x1000, 0xddcfc5fa);
            ROM_LOAD("turt_vid.2h", 0x3000, 0x1000, 0x9e71696c);
            ROM_LOAD("turt_vid.2j", 0x4000, 0x1000, 0xfcd49fef);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 64k for the audio CPU */
            ROM_LOAD("turt_snd.5c", 0x0000, 0x1000, 0xf0c30f9a);
            ROM_LOAD("turt_snd.5d", 0x1000, 0x1000, 0xaf5fc43c);

            ROM_REGION(0x1000, REGION_GFX1, ROMREGION_DISPOSE);
            ROM_LOAD("turt_vid.5h", 0x0000, 0x0800, 0xe5999d52);
            ROM_LOAD("turt_vid.5f", 0x0800, 0x0800, 0xc3ffd655);

            ROM_REGION(0x0020, REGION_PROMS, 0);
            ROM_LOAD("turtles.clr", 0x0000, 0x0020, 0xf3ef02dd);
            ROM_END();
        }
    };

    static RomLoadPtr rom_turpin = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("m1", 0x0000, 0x1000, 0x89177473);
            ROM_LOAD("m2", 0x1000, 0x1000, 0x4c6ca5c6);
            ROM_LOAD("m3", 0x2000, 0x1000, 0x62291652);
            ROM_LOAD("turt_vid.2h", 0x3000, 0x1000, 0x9e71696c);
            ROM_LOAD("m5", 0x4000, 0x1000, 0x7d2600f2);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 64k for the audio CPU */
            ROM_LOAD("turt_snd.5c", 0x0000, 0x1000, 0xf0c30f9a);
            ROM_LOAD("turt_snd.5d", 0x1000, 0x1000, 0xaf5fc43c);

            ROM_REGION(0x1000, REGION_GFX1, ROMREGION_DISPOSE);
            ROM_LOAD("turt_vid.5h", 0x0000, 0x0800, 0xe5999d52);
            ROM_LOAD("turt_vid.5f", 0x0800, 0x0800, 0xc3ffd655);

            ROM_REGION(0x0020, REGION_PROMS, 0);
            ROM_LOAD("turtles.clr", 0x0000, 0x0020, 0xf3ef02dd);
            ROM_END();
        }
    };

    static RomLoadPtr rom_600 = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x10000, REGION_CPU1, 0);/* 64k for code */
            ROM_LOAD("600_vid.2c", 0x0000, 0x1000, 0x8ee090ae);
            ROM_LOAD("600_vid.2e", 0x1000, 0x1000, 0x45bfaff2);
            ROM_LOAD("600_vid.2f", 0x2000, 0x1000, 0x9f4c8ed7);
            ROM_LOAD("600_vid.2h", 0x3000, 0x1000, 0xa92ef056);
            ROM_LOAD("600_vid.2j", 0x4000, 0x1000, 0x6dadd72d);

            ROM_REGION(0x10000, REGION_CPU2, 0);/* 64k for the audio CPU */
            ROM_LOAD("600_snd.5c", 0x0000, 0x1000, 0x1773c68e);
            ROM_LOAD("600_snd.5d", 0x1000, 0x1000, 0xa311b998);

            ROM_REGION(0x1000, REGION_GFX1, ROMREGION_DISPOSE);
            ROM_LOAD("600_vid.5h", 0x0000, 0x0800, 0x006c3d56);
            ROM_LOAD("600_vid.5f", 0x0800, 0x0800, 0x7dbc0426);

            ROM_REGION(0x0020, REGION_PROMS, 0);
            ROM_LOAD("turtles.clr", 0x0000, 0x0020, 0xf3ef02dd);
            ROM_END();
        }
    };

    public static GameDriver driver_amidar = new GameDriver("1981", "amidar", "amidar.java", rom_amidar, null, machine_driver_amidar, input_ports_amidar, init_amidar, ROT90, "Konami", "Amidar");
    public static GameDriver driver_amidaru = new GameDriver("1982", "amidaru", "amidar.java", rom_amidaru, driver_amidar, machine_driver_amidar, input_ports_amidaru, init_amidar, ROT90, "Konami (Stern license)", "Amidar (Stern)");
    public static GameDriver driver_amidaro = new GameDriver("1982", "amidaro", "amidar.java", rom_amidaro, driver_amidar, machine_driver_amidar, input_ports_amidaro, init_amidar, ROT90, "Konami (Olympia license)", "Amidar (Olympia)");
    public static GameDriver driver_amigo = new GameDriver("1982", "amigo", "amidar.java", rom_amigo, driver_amidar, machine_driver_amidar, input_ports_amidaru, init_amidar, ROT90, "bootleg", "Amigo");
    public static GameDriver driver_turtles = new GameDriver("1981", "turtles", "amidar.java", rom_turtles, null, machine_driver_amidar, input_ports_turtles, init_scramble_ppi, ROT90, "[Konami] (Stern license)", "Turtles");
    public static GameDriver driver_turpin = new GameDriver("1981", "turpin", "amidar.java", rom_turpin, driver_turtles, machine_driver_amidar, input_ports_turpin, init_scramble_ppi, ROT90, "[Konami] (Sega license)", "Turpin");
    public static GameDriver driver_600 = new GameDriver("1981", "600", "amidar.java", rom_600, driver_turtles, machine_driver_amidar, input_ports_turtles, init_scramble_ppi, ROT90, "Konami", "600");
}
