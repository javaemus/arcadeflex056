/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
/**
 * Changelog
 * =========
 * 13/05/2019 ported to mame 0.56 (shadow)
 */
package mame056.drivers;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;

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
import static mame056.memory.*;
import static mame056.sndintrfH.*;
import static mame056.inputH.*;

import static WIP.mame056.machine.segacrpt.*;

import static mame056.sound.sn76496.*;
import static mame056.sound.sn76496H.*;

import static mame056.vidhrdw.segasyse.*;

public class segasyse {

    /*-- Variables --*/
    public static int/*UINT8*/ u8_segae_8000bank;/* Current VDP Bank Selected for 0x8000 - 0xbfff writes */
    public static int/*UINT8*/ u8_port_fa_last;/* Last thing written to port 0xfa (control related) */
    public static int/*UINT8*/ u8_hintcount;/* line interrupt counter, decreased each scanline */
    public static int/*UINT8*/ u8_vintpending;/* vertical interrupt pending flag */
    public static int/*UINT8*/ u8_hintpending;/* scanline interrupt pending flag */

    /**
     * *****************************************************************************
     * Read / Write Handlers
     * *******************************************************************************
     * the ports 0xf8, 0xf9, 0xfa appear to be in some way control related,
     * their behavior is not fully understood, however for HangOnJr it appears
     * that we need to read either the accelerator or angle from port 0xf8
     * depending on the last write to port 0xfa, this yields a playable game,
     *
     * For Riddle of Pythagoras it doesn't seem so simple, the code we have here
     * seems to general do the job but it could probably do with looking at a
     * bit more
     * *****************************************************************************
     */

    /*-- Memory -- */
    public static WriteHandlerPtr segae_mem_8000_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* write the data the non-selected VRAM bank of the opposite number VDP chip stored in segae_8000bank */
            segae_vdp_vram[1 - u8_segae_8000bank].write(offset + (0x4000 - (u8_segae_vdp_vrambank[1 - u8_segae_8000bank] * 0x4000)), data);
        }
    };

    /*-- Ports --*/
    /**
     * *************************************
     * WRITE_HANDLER (segae_port_f7_w) ***************************************
     * writes here control the banking of ROM and RAM
     *
     * bit: 7	- Back Layer VDP (0) Vram Bank Select 6	- Front Layer VDP (1) Vram
     * Bank Select 5	- Select 0x8000 write location (1 = Back Layer VDP RAM, 0 =
     * Front Layer VDP RAM) writes are to the non-selected bank* 4 - unknown 3 -
     * unknown 2 - \ 1 - | Rom Bank Select for 0x8000 - 0 - |	0xbfff reads
     *
     **************************************
     */
    static int/*UINT8*/ rombank;

    public static void segae_bankswitch() {
        UBytePtr RAM = memory_region(REGION_CPU1);

        cpu_setbank(1, new UBytePtr(RAM, 0x10000 + (rombank * 0x4000)));
    }

    public static WriteHandlerPtr segae_port_f7_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            u8_segae_vdp_vrambank[0] = ((data & 0x80) >> 7) & 0xFF;/* Back  Layer VDP (0) VRAM Bank */
            u8_segae_vdp_vrambank[1] = ((data & 0x40) >> 6) & 0xFF;/* Front Layer VDP (1) VRAM Bank */
            u8_segae_8000bank = ((data & 0x20) >> 5) & 0xFF;/* 0x8000 Write Select */
            rombank = data & 0x07;/* Rom Banking */

            segae_bankswitch();
        }
    };

    /*- Beam Position -*/
    public static ReadHandlerPtr segae_port_7e_7f_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            int/*UINT8*/ temp = 0;
            int/*UINT16*/ sline;

            switch (offset) {
                case 0:
                    /* port 0x7e, Vert Position (in scanlines) */
                    sline = (261 - cpu_getiloops()) & 0xFFFF;
                    if (sline > 0xDA) {
                        sline = (sline - 6) & 0xFFFF;//sline -= 6;

                    }
                    temp = (sline - 1) & 0xFF;
                    break;
                case 1:
                    /* port 0x7f, Horz Position (in pixel clock cycles)  */
 /* unhandled for now */
                    break;
            }
            return temp & 0xFF;
        }
    };

    /*- VDP Related -*/
    public static ReadHandlerPtr segae_port_ba_bb_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            /* These Addresses access the Back Layer VDP (0) */
            int/*UINT8*/ temp = 0;

            switch (offset) {
                case 0:
                    /* port 0xba, VDP 0 DATA Read */
                    temp = segae_vdp_data_r(0);
                    break;
                case 1:
                    /* port 0xbb, VDP 0 CTRL Read */
                    temp = segae_vdp_ctrl_r(0);
                    break;
            }
            return temp & 0xFF;
        }
    };

    public static ReadHandlerPtr segae_port_be_bf_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            /* These Addresses access the Front Layer VDP (1) */
            int/*UINT8*/ temp = 0;

            switch (offset) {
                case 0:
                    /* port 0xbe, VDP 1 DATA Read */
                    temp = segae_vdp_data_r(1);
                    break;
                case 1:
                    /* port 0xbf, VDP 1 CTRL Read */
                    temp = segae_vdp_ctrl_r(1);
                    break;
            }
            return temp & 0xFF;
        }
    };
    public static WriteHandlerPtr segae_port_ba_bb_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* These Addresses access the Back Layer VDP (0) */
            switch (offset) {
                case 0:
                    /* port 0xba, VDP 0 DATA Write */
                    segae_vdp_data_w(0, data & 0xFF);
                    break;
                case 1:
                    /* port 0xbb, VDP 0 CTRL Write */
                    segae_vdp_ctrl_w(0, data & 0xFF);
                    break;
            }
        }
    };

    public static WriteHandlerPtr segae_port_be_bf_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* These Addresses access the Front Layer VDP (1) */
            switch (offset) {
                case 0:
                    /* port 0xbe, VDP 1 DATA Write */
                    segae_vdp_data_w(1, data & 0xFF);
                    break;
                case 1:
                    /* port 0xbf, VDP 1 CTRL Write */
                    segae_vdp_ctrl_w(1, data & 0xFF);
                    break;
            }
        }
    };

    /*- Hang On Jr. Specific -*/
    public static ReadHandlerPtr segae_hangonjr_port_f8_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            int/*UINT8*/ temp;

            temp = 0;

            if (u8_port_fa_last == 0x08) /* 0000 1000 */ /* Angle */ {
                temp = readinputport(4);
            }

            if (u8_port_fa_last == 0x09) /* 0000 1001 */ /* Accel */ {
                temp = readinputport(5);
            }

            return temp & 0xFF;
        }
    };

    public static WriteHandlerPtr segae_hangonjr_port_fa_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* Seems to write the same pattern again and again bits ---- xx-x used */
            u8_port_fa_last = data & 0xFF;
        }
    };

    /*- Riddle of Pythagoras Specific -*/
    static int port_to_read, last1, last2, diff1, diff2;

    public static ReadHandlerPtr segae_ridleofp_port_f8_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            switch (port_to_read) {
                default:
                case 0:
                    return diff1 & 0xff;
                case 1:
                    return diff1 >> 8;
                case 2:
                    return diff2 & 0xff;
                case 3:
                    return diff2 >> 8;
            }
        }
    };
    public static WriteHandlerPtr segae_ridleofp_port_fa_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* 0x10 is written before reading the dial (hold counters?) */
 /* 0x03 is written after reading the dial (reset counters?) */

            port_to_read = (data & 0x0c) >> 2;

            if ((data & 1) != 0) {
                int curr = readinputport(4);
                diff1 = ((curr - last1) & 0x0fff) | (curr & 0xf000);
                last1 = curr;
            }
            if ((data & 2) != 0) {
                int curr = readinputport(5) & 0x0fff;
                diff2 = ((curr - last2) & 0x0fff) | (curr & 0xf000);
                last2 = curr;
            }
        }
    };

    /**
     * *****************************************************************************
     * Port & Memory Maps
     * *******************************************************************************
     * most things on this hardware are done via port writes, including reading
     * of controls dipswitches, reads and writes to the vdp's etc. see notes at
     * top of file, the most noteworthy thing is the use of the 0x8000 - 0xbfff
     * region, reads are used to read ram, writes are used as a secondary way of
     * writing to VRAM
     * *****************************************************************************
     */
    /*-- Memory --*/
    public static Memory_ReadAddress segae_readmem[] = {
        new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_ReadAddress(0x0000, 0x7fff, MRA_ROM), /* Fixed ROM */
        new Memory_ReadAddress(0x8000, 0xbfff, MRA_BANK1), /* Banked ROM */
        new Memory_ReadAddress(0xc000, 0xffff, MRA_RAM), /* Main RAM */
        new Memory_ReadAddress(MEMPORT_MARKER, 0)
    };

    public static Memory_WriteAddress segae_writemem[] = {
        new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
        new Memory_WriteAddress(0x0000, 0x7fff, MWA_ROM), /* Fixed ROM */
        new Memory_WriteAddress(0x8000, 0xbfff, segae_mem_8000_w), /* Banked VRAM */
        new Memory_WriteAddress(0xc000, 0xffff, MWA_RAM), /* Main RAM */
        new Memory_WriteAddress(MEMPORT_MARKER, 0)
    };

    /*-- Ports --*/
    public static IO_ReadPort segae_readport[] = {
        new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
        new IO_ReadPort(0x7e, 0x7f, segae_port_7e_7f_r), /* Vertical / Horizontal Beam Position Read */
        new IO_ReadPort(0xba, 0xbb, segae_port_ba_bb_r), /* Back Layer VDP */
        new IO_ReadPort(0xbe, 0xbf, segae_port_be_bf_r), /* Front Layer VDP */
        new IO_ReadPort(0xe0, 0xe0, input_port_2_r), /* Coins + Starts */
        new IO_ReadPort(0xe1, 0xe1, input_port_3_r), /* Controls */
        new IO_ReadPort(0xf2, 0xf2, input_port_0_r), /* DSW0 */
        new IO_ReadPort(0xf3, 0xf3, input_port_1_r), /* DSW1 */
        new IO_ReadPort(MEMPORT_MARKER, 0)
    };

    public static IO_WritePort segae_writeport[] = {
        new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
        new IO_WritePort(0x7b, 0x7b, SN76496_0_w), /* Not sure which chip each is on */
        new IO_WritePort(0x7f, 0x7f, SN76496_1_w), /* Not sure which chip each is on */
        new IO_WritePort(0xba, 0xbb, segae_port_ba_bb_w), /* Back Layer VDP */
        new IO_WritePort(0xbe, 0xbf, segae_port_be_bf_w), /* Front Layer VDP */
        new IO_WritePort(0xf7, 0xf7, segae_port_f7_w), /* Banking Control */
        new IO_WritePort(MEMPORT_MARKER, 0)
    };

    /**
     * *****************************************************************************
     * Input Ports
     * *******************************************************************************
     * mostly unknown for the time being
     * *****************************************************************************
     */
    /* The Coinage is similar to Sega System 1 and C2, but
		it seems that Free Play is not used in all games
		(in fact, the only playable game that use it is
		Riddle of Pythagoras) */
    public static void SEGA_COIN_A() {
        PORT_DIPNAME(0x0f, 0x0f, DEF_STR("Coin_A"));
        PORT_DIPSETTING(0x07, DEF_STR("4C_1C"));
        PORT_DIPSETTING(0x08, DEF_STR("3C_1C"));
        PORT_DIPSETTING(0x09, DEF_STR("2C_1C"));
        PORT_DIPSETTING(0x05, "2 Coins/1 Credit 5/3 6/4");
        PORT_DIPSETTING(0x04, "2 Coins/1 Credit, 4/3");
        PORT_DIPSETTING(0x0f, DEF_STR("1C_1C"));
        PORT_DIPSETTING(0x03, "1 Coin/1 Credit, 5/6");
        PORT_DIPSETTING(0x02, "1 Coin/1 Credit, 4/5");
        PORT_DIPSETTING(0x01, "1 Coin/1 Credit, 2/3");
        PORT_DIPSETTING(0x06, DEF_STR("2C_3C"));
        PORT_DIPSETTING(0x0e, DEF_STR("1C_2C"));
        PORT_DIPSETTING(0x0d, DEF_STR("1C_3C"));
        PORT_DIPSETTING(0x0c, DEF_STR("1C_4C"));
        PORT_DIPSETTING(0x0b, DEF_STR("1C_5C"));
        PORT_DIPSETTING(0x0a, DEF_STR("1C_6C"));
    }

    public static void SEGA_COIN_B() {
        PORT_DIPNAME(0xf0, 0xf0, DEF_STR("Coin_B"));
        PORT_DIPSETTING(0x70, DEF_STR("4C_1C"));
        PORT_DIPSETTING(0x80, DEF_STR("3C_1C"));
        PORT_DIPSETTING(0x90, DEF_STR("2C_1C"));
        PORT_DIPSETTING(0x50, "2 Coins/1 Credit 5/3 6/4");
        PORT_DIPSETTING(0x40, "2 Coins/1 Credit, 4/3");
        PORT_DIPSETTING(0xf0, DEF_STR("1C_1C"));
        PORT_DIPSETTING(0x30, "1 Coin/1 Credit, 5/6");
        PORT_DIPSETTING(0x20, "1 Coin/1 Credit, 4/5");
        PORT_DIPSETTING(0x10, "1 Coin/1 Credit, 2/3");
        PORT_DIPSETTING(0x60, DEF_STR("2C_3C"));
        PORT_DIPSETTING(0xe0, DEF_STR("1C_2C"));
        PORT_DIPSETTING(0xd0, DEF_STR("1C_3C"));
        PORT_DIPSETTING(0xc0, DEF_STR("1C_4C"));
        PORT_DIPSETTING(0xb0, DEF_STR("1C_5C"));
        PORT_DIPSETTING(0xa0, DEF_STR("1C_6C"));
    }

    static InputPortPtr input_ports_dummy = new InputPortPtr() {
        public void handler() {
            /* Used by the Totally Non-Working Games */
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_transfrm = new InputPortPtr() {
        public void handler() {
            /* Used By Transformer */
            PORT_START();
            /* DSW0 Read from Port 0xf2 */
            SEGA_COIN_A();
            SEGA_COIN_B();

            PORT_START();
            /* DSW1 Read from Port 0xf3 */
            PORT_DIPNAME(0x01, 0x00, "1 Player Only");
            PORT_DIPSETTING(0x00, DEF_STR("Off"));
            PORT_DIPSETTING(0x01, DEF_STR("On"));
            PORT_DIPNAME(0x02, 0x00, DEF_STR("Demo_Sounds"));
            PORT_DIPSETTING(0x02, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_DIPNAME(0x0c, 0x0c, DEF_STR("Lives"));
            PORT_DIPSETTING(0x0c, "3");
            PORT_DIPSETTING(0x08, "4");
            PORT_DIPSETTING(0x04, "5");
            PORT_BITX(0, 0x00, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "Infinite", 0, 0);
            PORT_DIPNAME(0x30, 0x30, DEF_STR("Bonus_Life"));
            PORT_DIPSETTING(0x20, "10k, 30k, 50k and 70k");
            PORT_DIPSETTING(0x30, "20k, 60k, 100k and 140k");
            PORT_DIPSETTING(0x10, "30k, 80k, 130k and 180k");
            PORT_DIPSETTING(0x00, "50k, 150k and 250k");
            PORT_DIPNAME(0xc0, 0xc0, DEF_STR("Difficulty"));
            PORT_DIPSETTING(0x40, "Easy");
            PORT_DIPSETTING(0xc0, "Medium");
            PORT_DIPSETTING(0x80, "Hard");
            PORT_DIPSETTING(0x00, "Hardest");

            PORT_START();
            /* Read from Port 0xe0 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_COIN1);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_COIN2);
            PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR("Service_Mode"), KEYCODE_F2, IP_JOY_NONE);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_SERVICE1);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_START1);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_START2);

            PORT_START();
            /* Read from Port 0xe1 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_BUTTON1);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_BUTTON2);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNUSED);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNUSED);
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_hangonjr = new InputPortPtr() {
        public void handler() {
            /* Used By Hang On Jr */
            PORT_START();
            /* DSW0 Read from Port 0xf2 */
            SEGA_COIN_A();
            SEGA_COIN_B();

            PORT_START();
            /* DSW1 Read from Port 0xf3 */
            PORT_DIPNAME(0x01, 0x01, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x01, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_DIPNAME(0x06, 0x06, "Enemies");
            PORT_DIPSETTING(0x06, "Easy");
            PORT_DIPSETTING(0x04, "Medium");
            PORT_DIPSETTING(0x02, "Hard");
            PORT_DIPSETTING(0x00, "Hardest");
            PORT_DIPNAME(0x18, 0x18, DEF_STR("Difficulty"));
            PORT_DIPSETTING(0x18, "Easy");
            PORT_DIPSETTING(0x10, "Medium");
            PORT_DIPSETTING(0x08, "Hard");
            PORT_DIPSETTING(0x00, "Hardest");
            PORT_DIPNAME(0x20, 0x20, DEF_STR("Unknown"));  // These three dips seems to be unused
            PORT_DIPSETTING(0x20, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_DIPNAME(0x40, 0x40, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x40, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_DIPNAME(0x80, 0x80, DEF_STR("Unknown"));
            PORT_DIPSETTING(0x80, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));

            PORT_START();
            /* Read from Port 0xe0 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_COIN1);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_COIN2);
            PORT_BITX(0x04, IP_ACTIVE_LOW, IPT_SERVICE, DEF_STR("Service_Mode"), KEYCODE_F2, IP_JOY_NONE);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_SERVICE1);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_START1);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN);

            PORT_START();
            /* Read from Port 0xe1 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN);

            PORT_START();
            /* Read from Port 0xf8 */
            PORT_ANALOG(0xff, 0x7f, IPT_AD_STICK_X | IPF_PLAYER1, 25, 15, 0, 0xff);

            PORT_START();
            /* Read from Port 0xf8 */
            PORT_ANALOG(0xff, 0x00, IPT_AD_STICK_Y | IPF_REVERSE | IPF_PLAYER1, 20, 10, 0, 0xff);
            INPUT_PORTS_END();
        }
    };

    static InputPortPtr input_ports_ridleofp = new InputPortPtr() {
        public void handler() {
            /* Used By Riddle Of Pythagoras */
            PORT_START();
            /* DSW0 Read from Port 0xf2 */
            SEGA_COIN_A();
            PORT_DIPSETTING(0x00, DEF_STR("Free_Play"));
            SEGA_COIN_B();
            PORT_DIPSETTING(0x00, DEF_STR("Free_Play"));

            PORT_START();
            /* DSW1 Read from Port 0xf3 */
            PORT_DIPNAME(0x03, 0x02, DEF_STR("Lives"));
            PORT_DIPSETTING(0x03, "2");
            PORT_DIPSETTING(0x02, "3");
            PORT_DIPSETTING(0x01, "4");
            PORT_BITX(0, 0x00, IPT_DIPSWITCH_SETTING | IPF_CHEAT, "98", IP_KEY_NONE, IP_JOY_NONE);
            PORT_DIPNAME(0x04, 0x04, DEF_STR("Unknown"));  // Unknown
            PORT_DIPSETTING(0x04, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_DIPNAME(0x08, 0x08, "Difficulty?");// To be tested ! I don't see what else it could do
            PORT_DIPSETTING(0x08, "Easy");
            PORT_DIPSETTING(0x00, "Hard");
            PORT_DIPNAME(0x10, 0x10, DEF_STR("Unknown"));  // Unknown
            PORT_DIPSETTING(0x10, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));
            PORT_DIPNAME(0x60, 0x60, DEF_STR("Bonus_Life"));
            PORT_DIPSETTING(0x60, "50K 100K 200K 500K 1M 2M 5M 10M");
            PORT_DIPSETTING(0x40, "100K 200K 500K 1M 2M 5M 10M");
            PORT_DIPSETTING(0x20, "200K 500K 1M 2M 5M 10M");
            PORT_DIPSETTING(0x00, "None");
            PORT_DIPNAME(0x80, 0x80, DEF_STR("Unknown"));  // Unknown
            PORT_DIPSETTING(0x80, DEF_STR("Off"));
            PORT_DIPSETTING(0x00, DEF_STR("On"));

            PORT_START();
            /* Read from Port 0xe0 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_COIN1);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_COIN2);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_SERVICE1);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_START1);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN);// Would Be IPT_START2 but the code doesn't use it

            PORT_START();
            /* Port 0xe1 */
            PORT_BIT(0x01, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x02, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x04, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x08, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x10, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x20, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x40, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x80, IP_ACTIVE_LOW, IPT_UNKNOWN);

            PORT_START();
            /* Read from Port 0xf8 */
            PORT_ANALOG(0x0fff, 0x0000, IPT_DIAL, 50, 20, 0, 0);
            PORT_BIT(0x1000, IP_ACTIVE_LOW, IPT_BUTTON2);/* is this used in the game? */
            PORT_BIT(0x2000, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x4000, IP_ACTIVE_LOW, IPT_BUTTON1);
            PORT_BIT(0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN);

            PORT_START();
            /* Read from Port 0xf8 */
            PORT_ANALOG(0x0fff, 0x0000, IPT_DIAL | IPF_COCKTAIL, 50, 20, 0, 0);
            PORT_BIT(0x1000, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL);
            PORT_BIT(0x2000, IP_ACTIVE_LOW, IPT_UNKNOWN);
            PORT_BIT(0x4000, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL);
            PORT_BIT(0x8000, IP_ACTIVE_LOW, IPT_UNKNOWN);
            INPUT_PORTS_END();
        }
    };

    /**
     * *****************************************************************************
     * Interrupt Function
     * *******************************************************************************
     * Lines 0 - 191 | Dislay Lines 0 - 261 | Non-Display / VBlank Period
     *
     * VDP1 Seems to be in Charge of Line Interrupts at Least
     *
     * Interrupt enable bits etc are a bit uncertain
     * *****************************************************************************
     */
    public static InterruptPtr segae_interrupt = new InterruptPtr() {
        public int handler() {
            int sline;
            sline = 261 - cpu_getiloops();

            if (sline == 0) {
                u8_hintcount = segae_vdp_regs[1].read(10) & 0xFF;
            }

            if (sline <= 192) {

                if (sline != 192) {
                    segae_drawscanline(sline);
                }

                if (sline == 192) {
                    u8_vintpending = 1;
                }

                if (u8_hintcount == 0) {
                    u8_hintcount = segae_vdp_regs[1].read(10) & 0xFF;
                    u8_hintpending = 1;

                    if ((segae_vdp_regs[1].read(0) & 0x10) != 0) {
                        return Z80_IRQ_INT;
                    }

                } else {
                    u8_hintcount = (u8_hintcount - 1) & 0xFF;//hintcount--;
                }
            }

            if (sline > 192) {
                u8_hintcount = segae_vdp_regs[1].read(10) & 0xFF;

                if ((sline < 0xe0) && (u8_vintpending) != 0) {
                    return Z80_IRQ_INT;
                }

            }
            return ignore_interrupt.handler();
        }
    };

    /**
     * *****************************************************************************
     * Machine Driver(s)
     * *******************************************************************************
     * a z80, unknown speed the two SN76489's are located on the VDP chips, one
     * on each
     *
     * some of this could potentially vary between games as the CPU is on the
     * ROM board no the main board, for instance Astro Flash appears to have
     * some kind of custom cpu
     * *****************************************************************************
     */
    static SN76496interface sn76489_intf = new SN76496interface(
            2, /* 2 chips */
            new int[]{4000000, 4000000}, /* 4 mhz? (guess) */
            new int[]{50, 50}
    );

    static MachineDriver machine_driver_segae = new MachineDriver(
            /* basic machine hardware */
            new MachineCPU[]{
                new MachineCPU(
                        CPU_Z80,
                        10738600 / 2, /* correct for hangonjr, and astroflash/transformer at least  */
                        segae_readmem, segae_writemem, segae_readport, segae_writeport,
                        segae_interrupt, 262
                )
            },
            60, DEFAULT_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
            1, /* single CPU, no need for interleaving */
            null,
            /* video hardware */
            256, 192, new rectangle(0, 256 - 1, 0, 192 - 1),
            null,
            64, 0,
            null,
            VIDEO_TYPE_RASTER,
            null,
            segae_vh_start,
            segae_vh_stop,
            segae_vh_screenrefresh,
            /* sound hardware */
            0, 0, 0, 0,
            new MachineSound[]{
                new MachineSound(SOUND_SN76496, sn76489_intf)
            },
            null
    );

    /**
     * *****************************************************************************
     * General Init
     * *******************************************************************************
     * for Save State support
     * *****************************************************************************
     */
    public static InitDriverPtr init_segasyse = new InitDriverPtr() {
        public void handler() {
            /*TODO*///	state_save_register_UINT8 ( "SEGASYSE-MAIN", 0, "8000 Write Bank",		&segae_8000bank, 1);
            /*TODO*///	state_save_register_UINT8 ( "SEGASYSE-MAIN", 0, "Vertical Int Pending",	&vintpending, 1);
            /*TODO*///	state_save_register_UINT8 ( "SEGASYSE-MAIN", 0, "Line Int Pending",		&hintpending, 1);
            /*TODO*///	state_save_register_UINT8 ( "SEGASYSE-MAIN", 0, "Main Rom Bank",		&rombank, 1);
            /*TODO*///	state_save_register_func_postload(segae_bankswitch);
        }
    };

    /**
     * *****************************************************************************
     * Game Inits
     * *******************************************************************************
     * Just the One for now (Hang On Jr), Installing the custom READ/WRITE
     * handlers we need for the controls
     * *****************************************************************************
     */
    public static InitDriverPtr init_hangonjr = new InitDriverPtr() {
        public void handler() {
            install_port_read_handler(0, 0xf8, 0xf8, segae_hangonjr_port_f8_r);
            install_port_write_handler(0, 0xfa, 0xfa, segae_hangonjr_port_fa_w);

            /*TODO*///	state_save_register_UINT8 ( "SEGASYSE-HOJ", 0, "port_fa_last",			&port_fa_last, 1);
            init_segasyse.handler();
        }
    };

    public static InitDriverPtr init_ridleofp = new InitDriverPtr() {
        public void handler() {
            install_port_read_handler(0, 0xf8, 0xf8, segae_ridleofp_port_f8_r);
            install_port_write_handler(0, 0xfa, 0xfa, segae_ridleofp_port_fa_w);

            init_segasyse.handler();
        }
    };

    public static InitDriverPtr init_astrofl = new InitDriverPtr() {
        public void handler() {
            astrofl_decode();

            init_segasyse.handler();
        }
    };

    /**
     * *****************************************************************************
     * Rom Loaders / Game Drivers
     * *******************************************************************************
     * Good Dumps: hangonjr - Hang On Jr. ridleofp - Riddle of Pythagoras (Jp.)
     * transfrm - Transformer astrofl - Astro Flash (Jp. Version of Transformer)
     * *Custom CPU, scratched surface 'NEC??'* fantzn2 - Fantasy Zone 2 (set 2)
     * *Rom at IC7 Encrypted* opaopa	- Opa Opa	*Roms Encrypted/Bad?*
     * *****************************************************************************
     */
    static RomLoadPtr rom_hangonjr = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x30000, REGION_CPU1, 0);
            ROM_LOAD("rom5.ic7", 0x00000, 0x08000, 0xd63925a7);/* Fixed Code */

 /* The following are 8 0x4000 banks that get mapped to reads from 0x8000 - 0xbfff */
            ROM_LOAD("rom4.ic5", 0x10000, 0x08000, 0xee3caab3);
            ROM_LOAD("rom3.ic4", 0x18000, 0x08000, 0xd2ba9bc9);
            ROM_LOAD("rom2.ic3", 0x20000, 0x08000, 0xe14da070);
            ROM_LOAD("rom1.ic2", 0x28000, 0x08000, 0x3810cbf5);
            ROM_END();
        }
    };

    static RomLoadPtr rom_ridleofp = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x30000, REGION_CPU1, 0);
            ROM_LOAD("epr10426.bin", 0x00000, 0x08000, 0x4404c7e7);/* Fixed Code */

 /* The following are 8 0x4000 banks that get mapped to reads from 0x8000 - 0xbfff */
            ROM_LOAD("epr10425.bin", 0x10000, 0x08000, 0x35964109);
            ROM_LOAD("epr10424.bin", 0x18000, 0x08000, 0xfcda1dfa);
            ROM_LOAD("epr10423.bin", 0x20000, 0x08000, 0x0b87244f);
            ROM_LOAD("epr10422.bin", 0x28000, 0x08000, 0x14781e56);
            ROM_END();
        }
    };

    static RomLoadPtr rom_transfrm = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x30000, REGION_CPU1, 0);
            ROM_LOAD("ic7.top", 0x00000, 0x08000, 0xccf1d123);/* Fixed Code */

 /* The following are 8 0x4000 banks that get mapped to reads from 0x8000 - 0xbfff */
            ROM_LOAD("epr-7347.ic5", 0x10000, 0x08000, 0xdf0f639f);
            ROM_LOAD("epr-7348.ic4", 0x18000, 0x08000, 0x0f38ea96);
            ROM_LOAD("ic3.top", 0x20000, 0x08000, 0x9d485df6);
            ROM_LOAD("epr-7350.ic2", 0x28000, 0x08000, 0x0052165d);
            ROM_END();
        }
    };

    static RomLoadPtr rom_astrofl = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(2 * 0x30000, REGION_CPU1, 0);
            ROM_LOAD("epr-7723.ic7", 0x00000, 0x08000, 0x66061137);/* encrypted */

 /* The following are 8 0x4000 banks that get mapped to reads from 0x8000 - 0xbfff */
            ROM_LOAD("epr-7347.ic5", 0x10000, 0x08000, 0xdf0f639f);
            ROM_LOAD("epr-7348.ic4", 0x18000, 0x08000, 0x0f38ea96);
            ROM_LOAD("epr-7349.ic3", 0x20000, 0x08000, 0xf8c352d5);
            ROM_LOAD("epr-7350.ic2", 0x28000, 0x08000, 0x0052165d);
            ROM_END();
        }
    };

    static RomLoadPtr rom_fantzn2 = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x50000, REGION_CPU1, 0);
            ROM_LOAD("fz2_ic7.rom", 0x00000, 0x08000, 0x76db7b7b);
            ROM_LOAD("fz2_ic5.rom", 0x10000, 0x10000, 0x57b45681);
            ROM_LOAD("fz2_ic4.rom", 0x20000, 0x10000, 0x6f7a9f5f);
            ROM_LOAD("fz2_ic3.rom", 0x30000, 0x10000, 0xa231dc85);
            ROM_LOAD("fz2_ic2.rom", 0x40000, 0x10000, 0xb14db5af);
            ROM_END();
        }
    };

    static RomLoadPtr rom_opaopa = new RomLoadPtr() {
        public void handler() {
            ROM_REGION(0x30000, REGION_USER1, 0);
            ROM_LOAD("epr11224.ic7", 0x00000, 0x08000, 0x024b1244);/* Fixed Code */

 /* The following are 8 0x4000 banks that get mapped to reads from 0x8000 - 0xbfff */
            ROM_LOAD("epr11223.ic5", 0x10000, 0x08000, 0x6bc41d6e);
            ROM_LOAD("epr11222.ic4", 0x18000, 0x08000, 0x395c1d0a);
            ROM_LOAD("epr11221.ic3", 0x20000, 0x08000, 0x4ca132a2);
            ROM_LOAD("epr11220.ic2", 0x28000, 0x08000, 0xa165e2ef);
            ROM_END();
        }
    };

    /*-- Game Drivers --*/
    public static GameDriver driver_hangonjr = new GameDriver("1985", "hangonjr", "segasyse.java", rom_hangonjr, null, machine_driver_segae, input_ports_hangonjr, init_hangonjr, ROT0, "Sega", "Hang-On Jr.");
    public static GameDriver driver_transfrm = new GameDriver("1986", "transfrm", "segasyse.java", rom_transfrm, null, machine_driver_segae, input_ports_transfrm, init_segasyse, ROT0, "Sega", "Transformer");
    public static GameDriver driver_astrofl = new GameDriver("1986", "astrofl", "segasyse.java", rom_astrofl, driver_transfrm, machine_driver_segae, input_ports_transfrm, init_astrofl, ROT0, "Sega", "Astro Flash (Japan)");
    public static GameDriver driver_ridleofp = new GameDriver("1986", "ridleofp", "segasyse.java", rom_ridleofp, null, machine_driver_segae, input_ports_ridleofp, init_ridleofp, ROT90, "Sega / Nasco", "Riddle of Pythagoras (Japan)");
    public static GameDriver driver_fantzn2 = new GameDriver("198?", "fantzn2", "segasyse.java", rom_fantzn2, null, machine_driver_segae, input_ports_dummy, init_segasyse, ROT0, "????", "Fantasy Zone 2", GAME_NOT_WORKING);/* encrypted */
    public static GameDriver driver_opaopa = new GameDriver("198?", "opaopa", "segasyse.java", rom_opaopa, null, machine_driver_segae, input_ports_dummy, init_segasyse, ROT0, "????", "Opa Opa", GAME_NOT_WORKING);/* either encrypted or bad */
}
