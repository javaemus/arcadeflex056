/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package WIP.mame056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstdlib.rand;
import static common.ptr.*;
import static mame056.cpu.m6502.m6502H.M6502_IRQ_LINE;
import static mame056.cpuintrfH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuexec.*;
import static mame056.machine._6812piaH.*;
import static mame056.machine._6812pia.*;
import static mame056.mame.Machine;
import static mame056.sndintrfH.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.driverH.*;
import static mame056.sound.streams.*;
import static common.libc.cstring.*;
import static mame056.machine._6812pia.*;
import static WIP.mame056.sound.tms5220.*;
import static WIP.mame056.sound._5220intf.*;
import static WIP.mame056.drivers.victory.*;
// refactor
import static arcadeflex036.osdepend.logerror;
import static WIP.mame056.sound.hc55516.*;

public class exidy {

    /**
     * ***********************************
     *
     * Constants
     *
     ************************************
     */
    public static int CRYSTAL_OSC = (3579545);
    public static double SH8253_CLOCK = (CRYSTAL_OSC / 2);
    public static double SH6840_CLOCK = (CRYSTAL_OSC / 4);
    public static double SH6532_CLOCK = (CRYSTAL_OSC / 4);
    public static double SH6532_PERIOD = (1.0 / (double) SH6532_CLOCK);
    public static double CVSD_CLOCK_FREQ = (1000000.0 / 34.0);
    public static double BASE_VOLUME = (32767 / 6);

    public static final int RIOT_IDLE = 0;
    public static final int RIOT_COUNT = 1;
    public static final int RIOT_POST_COUNT = 2;

    /**
     * ***********************************
     *
     * Local variables
     *
     ************************************
     */
    /* IRQ variables */
    static /*UINT8*/ int u8_pia_irq_state;
    static /*UINT8*/ int u8_riot_irq_state;

    /* 6532 variables */
    static Object riot_timer;
    static /*UINT8*/ int u8_riot_irq_flag;
    static /*UINT8*/ int u8_riot_timer_irq_enable;
    static /*UINT8*/ int u8_riot_PA7_irq_enable;
    static /*UINT8*/ int u8_riot_porta_data;
    static /*UINT8*/ int u8_riot_porta_ddr;
    static /*UINT8*/ int u8_riot_portb_data;
    static /*UINT8*/ int u8_riot_portb_ddr;
    static double riot_interval;
    static /*UINT8*/ int u8_riot_state;

    public static class PAIR {
        //L = low 8 bits
        //H = high 8 bits
        //D = whole 16 bits

        public int H, L, D;

        public void SetH(int val) {
            H = val & 0xFF;
            D = ((H << 8) | L) & 0xFFFF;
        }

        public void SetL(int val) {
            L = val & 0xFF;
            D = ((H << 8) | L) & 0xFFFF;
        }

        public void SetD(int val) {
            D = val & 0xFFFF;
            H = D >> 8 & 0xFF;
            L = D & 0xFF;
        }

        public void AddH(int val) {
            H = (H + val) & 0xFF;
            D = ((H << 8) | L) & 0xFFFF;
        }

        public void AddL(int val) {
            L = (L + val) & 0xFF;
            D = ((H << 8) | L) & 0xFFFF;
        }

        public void AddD(int val) {
            D = (D + val) & 0xFFFF;
            H = D >> 8 & 0xFF;
            L = D & 0xFF;
        }
    }

    /* 6840 variables */
    public static class sh6840_timer_channel {

        /*UINT8*/
        int u8_cr;
        /*UINT8*/
        int u8_state;
        /*UINT8*/
        int u8_leftovers;
        /*UINT16*/
        int u16_timer;
        /*UINT32*/
        int u32_clocks;
        PAIR counter = new PAIR();

        public static sh6840_timer_channel[] create(int n) {
            sh6840_timer_channel[] a = new sh6840_timer_channel[n];
            for (int k = 0; k < n; k++) {
                a[k] = new sh6840_timer_channel();
            }
            return a;
        }
    }

    static sh6840_timer_channel sh6840_timer[] = sh6840_timer_channel.create(3);
    static short[] sh6840_volume = new short[3];
    static /*UINT8*/ int u8_sh6840_MSB;
    static /*UINT8*/ int u8_sh6840_noise_state;
    static /*UINT8*/ int u8_sh6840_noise_history;
    static /*UINT32*/ int u32_sh6840_clocks_per_sample;
    static /*UINT32*/ int u32_sh6840_clock_count;
    static /*UINT8*/ int u8_exidy_sfxctrl;

    /* 8253 variables */
    public static class sh8253_timer_channel {

        /*UINT8*/
        int u8_clstate;
        /*UINT8*/
        int u8_enable;
        /*UINT16*/
        int u16_count;
        /*UINT32*/
        int u32_step;
        /*UINT32*/
        int u32_fraction;

        public static sh8253_timer_channel[] create(int n) {
            sh8253_timer_channel[] a = new sh8253_timer_channel[n];
            for (int k = 0; k < n; k++) {
                a[k] = new sh8253_timer_channel();
            }
            return a;
        }
    }
    static sh8253_timer_channel sh8253_timer[] = sh8253_timer_channel.create(3);

    /* 5220/CVSD variables */
    static /*UINT8*/ int u8_has_hc55516;
    static /*UINT8*/ int u8_has_tms5220;

    /* sound streaming variables */
    static int exidy_stream;
    static double freq_to_step;

    /**
     * ***********************************
     *
     * Interrupt generation helpers
     *
     ************************************
     */
    public static void update_irq_state() {
        cpu_set_irq_line(1, M6502_IRQ_LINE, (u8_pia_irq_state | u8_riot_irq_state) != 0 ? ASSERT_LINE : CLEAR_LINE);
    }

    static irqfuncPtr exidy_irq = new irqfuncPtr() {
        public void handler(int state) {
            u8_pia_irq_state = state & 0xFF;
            update_irq_state();
        }
    };

    /**
     * ***********************************
     *
     * PIA interface
     *
     ************************************
     */
    /* PIA 0 */
    static pia6821_interface pia_0_intf = new pia6821_interface(
            /*inputs : A/B,CA/B1,CA/B2 */null, null, null, null, null, null,
            /*outputs: A/B,CA/B2       */ pia_1_portb_w, pia_1_porta_w, pia_1_cb1_w, pia_1_ca1_w,
            /*irqs   : A/B             */ null, null
    );

    /* PIA 1 */
    static pia6821_interface pia_1_intf = new pia6821_interface(
            /*inputs : A/B,CA/B1,CA/B2 */null, null, null, null, null, null,
            /*outputs: A/B,CA/B2       */ pia_0_portb_w, pia_0_porta_w, pia_0_cb1_w, pia_0_ca1_w,
            /*irqs   : A/B             */ null, exidy_irq
    );

    /* Victory PIA 0 */
    static pia6821_interface victory_pia_0_intf = new pia6821_interface(
            /*inputs : A/B,CA/B1,CA/B2 */null, null, null, null, null, null,
            /*outputs: A/B,CA/B2       */ null, victory_sound_response_w, victory_sound_irq_clear_w, victory_main_ack_w,
            /*irqs   : A/B             */ null, exidy_irq
    );

    /**
     * ***********************************
     *
     * 6840 clock counting helper
     *
     ************************************
     */
    public static void sh6840_apply_clock(sh6840_timer_channel t, int clocks) {
        /* dual 8-bit case */
        if ((t.u8_cr & 0x04) != 0) {
            /* handle full decrements */
            while (clocks > t.counter.L) {
                t.counter.AddL(1);
                clocks -= t.counter.L;
                //clocks -= t->counter.b.l + 1;
                t.counter.SetL(t.u16_timer);

                /* decrement MSB */
                t.counter.AddH(-1);
                //if (!t->counter.b.h--)
                if (t.counter.H == 0) {
                    t.u8_state = 0;
                    t.counter.SetD(t.u16_timer);
                } /* state goes high when MSB is 0 */ else if (t.counter.H == 0) {
                    t.u8_state = 1;
                    t.u32_clocks++;
                }
            }

            /* subtract off the remainder */
            t.counter.SetL(t.counter.L - clocks);//t->counter.b.l -= clocks;
        } /* 16-bit case */ else {
            /* handle full decrements */
            while (clocks > t.counter.D) {
                t.counter.AddD(1);
                clocks -= t.counter.D;
                //clocks -= t->counter.w + 1;
                t.u8_state = (t.u8_state ^ 1) & 0xFF;
                t.u32_clocks += t.u8_state;
                t.counter.SetD(t.u16_timer);
            }

            /* subtract off the remainder */
            t.counter.SetD(t.counter.D - clocks);//t->counter.w -= clocks;
        }
    }

    /**
     * ***********************************
     *
     * Noise generation helper
     *
     ************************************
     */
    public static int sh6840_update_noise(int clocks) {
        int/*UINT8*/ history = u8_sh6840_noise_history & 0xFF;
        int noise_clocks = 0;
        int i;

        /* loop over clocks */
        for (i = 0; i < clocks; i++) {
            /* keep a history of the last few noise samples */
            history = ((history << 1) | (rand() & 1)) & 0xFF;

            /* if we clocked 0->1, that will serve as an external clock */
            if ((history & 0x03) == 0x01) {
                u8_sh6840_noise_state = (u8_sh6840_noise_state ^ 1) & 0xFF;
                noise_clocks += u8_sh6840_noise_state;
            }
        }

        /* remember the history for next time */
        u8_sh6840_noise_history = history & 0xFF;
        return noise_clocks;
    }

    /**
     * ***********************************
     *
     * Core sound generation
     *
     ************************************
     */
    static StreamInitPtr exidy_stream_update = new StreamInitPtr() {
        public void handler(int param, ShortPtr buffer, int length) {
            int noisy = ((sh6840_timer[0].u8_cr & sh6840_timer[1].u8_cr & sh6840_timer[2].u8_cr & 0x02) == 0) ? 1 : 0;

            /* loop over samples */
            while (length-- != 0) {
                sh6840_timer_channel t;
                sh8253_timer_channel c;
                int clocks_this_sample;
                short sample = 0;

                /* determine how many 6840 clocks this sample */
                u32_sh6840_clock_count += u32_sh6840_clocks_per_sample;
                clocks_this_sample = u32_sh6840_clock_count >> 24;
                u32_sh6840_clock_count &= (1 << 24) - 1;

                /* skip if nothing enabled */
                if ((sh6840_timer[0].u8_cr & 0x01) == 0) {
                    int noise_clocks_this_sample = 0;
                    int/*UINT32*/ chan0_clocks;

                    /* generate E-clocked noise if necessary */
                    if (noisy != 0 && (u8_exidy_sfxctrl & 0x01) == 0) {
                        noise_clocks_this_sample = sh6840_update_noise(clocks_this_sample);
                    }

                    /* handle timer 0 if enabled */
                    t = sh6840_timer[0];
                    chan0_clocks = t.u32_clocks;
                    if ((t.u8_cr & 0x80) != 0) {
                        int clocks = (t.u8_cr & 0x02) != 0 ? clocks_this_sample : noise_clocks_this_sample;
                        sh6840_apply_clock(t, clocks);
                        if (t.u8_state != 0 && (u8_exidy_sfxctrl & 0x02) == 0) {
                            sample += sh6840_volume[0];
                        }
                    }

                    /* generate channel 0-clocked noise if necessary */
                    if (noisy != 0 && (u8_exidy_sfxctrl & 0x01) != 0) {
                        noise_clocks_this_sample = sh6840_update_noise(t.u32_clocks - chan0_clocks);
                    }

                    /* handle timer 1 if enabled */
                    t = sh6840_timer[1];
                    if ((t.u8_cr & 0x80) != 0) {
                        int clocks = (t.u8_cr & 0x02) != 0 ? clocks_this_sample : noise_clocks_this_sample;
                        sh6840_apply_clock(t, clocks);
                        if ((t.u8_state) != 0) {
                            sample += sh6840_volume[1];
                        }
                    }

                    /* handle timer 2 if enabled */
                    t = sh6840_timer[2];
                    if ((t.u8_cr & 0x80) != 0) {
                        int clocks = (t.u8_cr & 0x02) != 0 ? clocks_this_sample : noise_clocks_this_sample;

                        /* prescale */
                        if ((t.u8_cr & 0x01) != 0) {
                            clocks += t.u8_leftovers;
                            t.u8_leftovers = (clocks % 8) & 0xFF;
                            clocks /= 8;
                        }
                        sh6840_apply_clock(t, clocks);
                        if ((t.u8_state) != 0) {
                            sample += sh6840_volume[2];
                        }
                    }
                }

                /* music channel 0 */
                c = sh8253_timer[0];
                if ((c.u8_enable) != 0) {
                    c.u32_fraction += c.u32_step;
                    if ((c.u32_fraction & 0x0800000) != 0) {
                        sample += BASE_VOLUME;
                    }
                }

                /* music channel 1 */
                c = sh8253_timer[1];
                if ((c.u8_enable) != 0) {
                    c.u32_fraction += c.u32_step;
                    if ((c.u32_fraction & 0x0800000) != 0) {
                        sample += BASE_VOLUME;
                    }
                }

                /* music channel 2 */
                c = sh8253_timer[2];
                if ((c.u8_enable) != 0) {
                    c.u32_fraction += c.u32_step;
                    if ((c.u32_fraction & 0x0800000) != 0) {
                        sample += BASE_VOLUME;
                    }
                }

                /* stash */
                buffer.writeinc(sample);
            }
        }
    };

    /**
     * ***********************************
     *
     * Sound startup routines
     *
     ************************************
     */
    static int common_start() {
        int i;

        /* determine which sound hardware is installed */
        u8_has_hc55516 = 0;
        u8_has_tms5220 = 0;
        for (i = 0; i < MAX_SOUND; i++) {
            if (Machine.drv.sound[i].sound_type == SOUND_TMS5220) {
                u8_has_tms5220 = 1;
            }
            if (Machine.drv.sound[i].sound_type == SOUND_HC55516) {
                u8_has_hc55516 = 1;
            }
        }

        /* allocate the stream */
        exidy_stream = stream_init("Exidy custom", 100, Machine.sample_rate, 0, exidy_stream_update);

        /* Init PIA */
        pia_reset();

        /* Init 6532 */
        riot_timer = 0;
        u8_riot_irq_flag = 0;
        u8_riot_timer_irq_enable = 0;
        u8_riot_porta_data = 0xff;
        u8_riot_portb_data = 0xff;
        riot_interval = SH6532_PERIOD;
        u8_riot_state = RIOT_IDLE;

        /* Init 6840 */
        //memset(sh6840_timer, 0, sizeof(sh6840_timer));
        if (Machine.sample_rate != 0) {
            u32_sh6840_clocks_per_sample = (int) ((double) SH6840_CLOCK / (double) Machine.sample_rate * (double) (1 << 24));
        }
        u8_sh6840_MSB = 0;
        u8_exidy_sfxctrl = 0;

        /* Init 8253 */
        //memset(sh8253_timer, 0, sizeof(sh8253_timer));
        if (Machine.sample_rate != 0) {
            freq_to_step = (double) (1 << 24) / (double) Machine.sample_rate;
        }

        return 0;
    }

    public static ShStartPtr exidy_sh_start = new ShStartPtr() {
        public int handler(MachineSound msound) {
            /* Init PIA */
            pia_config(0, PIA_STANDARD_ORDERING, pia_0_intf);
            pia_config(1, PIA_STANDARD_ORDERING, pia_1_intf);
            return common_start();
        }
    };
    public static ShStartPtr victory_sh_start = new ShStartPtr() {
        public int handler(MachineSound msound) {
            /* Init PIA */
            pia_config(0, PIA_STANDARD_ORDERING, victory_pia_0_intf);
            pia_0_cb1_w.handler(0, 1);
            return common_start();
        }
    };

    /**
     * ***********************************
     *
     * 6532 RIOT timer callback
     *
     ************************************
     */
    static timer_callback riot_interrupt = new timer_callback() {
        public void handler(int parm) {
            /* if we're doing the initial interval counting... */
            if (u8_riot_state == RIOT_COUNT) {
                /* generate the IRQ */
                u8_riot_irq_flag = (u8_riot_irq_flag | 0x80) & 0xFF;
                u8_riot_irq_state = u8_riot_timer_irq_enable & 0xFF;
                update_irq_state();

                /* now start counting clock cycles down */
                u8_riot_state = RIOT_POST_COUNT;
                riot_timer = timer_set(SH6532_PERIOD * 0xff, 0, riot_interrupt);
            } /* if not, we are done counting down */ else {
                u8_riot_state = RIOT_IDLE;
                riot_timer = 0;
            }
        }
    };

    /**
     * ***********************************
     *
     * 6532 RIOT write handler
     *
     ************************************
     */
    public static WriteHandlerPtr exidy_shriot_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* mask to the low 7 bits */
            offset &= 0x7f;

            /* I/O is done if A2 == 0 */
            if ((offset & 0x04) == 0) {
                switch (offset & 0x03) {
                    case 0:
                        /* port A */
                        if (u8_has_hc55516 != 0) {
                            cpu_set_reset_line(2, (data & 0x10) != 0 ? CLEAR_LINE : ASSERT_LINE);
                        }
                        u8_riot_porta_data = ((u8_riot_porta_data & ~u8_riot_porta_ddr) | (data & u8_riot_porta_ddr)) & 0xFF;
                        break;

                    case 1:
                        /* port A DDR */
                        u8_riot_porta_ddr = data & 0xFF;
                        break;

                    case 2:
                        /* port B */
                        if (u8_has_tms5220 != 0) {
                            if ((data & 0x01) == 0 && (u8_riot_portb_data & 0x01) != 0) {
                                u8_riot_porta_data = tms5220_status_r.handler(0);
                                logerror("(%f)%04X:TMS5220 status read = %02X\n", timer_get_time(), cpu_getpreviouspc(), u8_riot_porta_data);
                            }
                            if ((data & 0x02) != 0 && (u8_riot_portb_data & 0x02) == 0) {
                                logerror("(%f)%04X:TMS5220 data write = %02X\n", timer_get_time(), cpu_getpreviouspc(), u8_riot_porta_data);
                                tms5220_data_w.handler(0, u8_riot_porta_data);
                            }
                        }
                        u8_riot_portb_data = ((u8_riot_portb_data & ~u8_riot_portb_ddr) | (data & u8_riot_portb_ddr)) & 0xFF;
                        break;

                    case 3:
                        /* port B DDR */
                        u8_riot_portb_ddr = data & 0xFF;
                        break;
                }
            } /* PA7 edge detect control if A2 == 1 and A4 == 0 */ else if ((offset & 0x10) == 0) {
                u8_riot_PA7_irq_enable = offset & 0x03;
            } /* timer enable if A2 == 1 and A4 == 1 */ else {
                double divisors[] = {1.0, 8.0, 64.0, 1024.0};

                /* make sure the IRQ state is clear */
                if (u8_riot_state != RIOT_COUNT) {
                    u8_riot_irq_flag = (u8_riot_irq_flag & ~0x80) & 0xFF;
                }
                u8_riot_irq_state = 0;
                update_irq_state();

                /* set the enable from the offset */
                u8_riot_timer_irq_enable = offset & 0x08;

                /* remove any old timer */
                if (riot_timer != null) {
                    timer_remove(riot_timer);
                }

                /* set a new timer */
                riot_interval = SH6532_PERIOD * divisors[offset & 0x03];
                riot_timer = timer_set(riot_interval * data, 0, riot_interrupt);
                u8_riot_state = RIOT_COUNT;
            }
        }
    };

    /**
     * ***********************************
     *
     * 6532 RIOT read handler
     *
     ************************************
     */
    public static ReadHandlerPtr exidy_shriot_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            /* mask to the low 7 bits */
            offset &= 0x7f;

            /* I/O is done if A2 == 0 */
            if ((offset & 0x04) == 0) {
                switch (offset & 0x03) {
                    case 0x00:
                        /* port A */
                        return u8_riot_porta_data & 0xFF;

                    case 0x01:
                        /* port A DDR */
                        return u8_riot_porta_ddr & 0xFF;

                    case 0x02:
                        /* port B */
                        if (u8_has_tms5220 != 0) {
                            u8_riot_portb_data = (u8_riot_portb_data & ~0x0c) & 0xFF;
                            if (tms5220_ready_r() == 0) {
                                u8_riot_portb_data = (u8_riot_portb_data | 0x04) & 0xFF;
                            }
                            if (tms5220_int_r() == 0) {
                                u8_riot_portb_data = (u8_riot_portb_data | 0x08) & 0xFF;
                            }
                        }
                        return u8_riot_portb_data & 0xFF;

                    case 0x03:
                        /* port B DDR */
                        return u8_riot_portb_ddr & 0xFF;
                }
            } /* interrupt flags are read if A2 == 1 and A0 == 1 */ else if ((offset & 0x01) != 0) {
                int temp = u8_riot_irq_flag & 0xFF;
                u8_riot_irq_flag = 0;
                u8_riot_irq_state = 0;
                update_irq_state();
                return temp;
            } /* timer count is read if A2 == 1 and A0 == 0 */ else {
                /* set the enable from the offset */
                u8_riot_timer_irq_enable = offset & 0x08;

                /* compute the timer based on the current state */
                switch (u8_riot_state) {
                    case RIOT_IDLE:
                        return 0x00;

                    case RIOT_COUNT:
                        return (int) (timer_timeleft(riot_timer) / riot_interval);

                    case RIOT_POST_COUNT:
                        return (int) (timer_timeleft(riot_timer) / SH6532_PERIOD);
                }
            }

            logerror("Undeclared RIOT read: %x  PC:%x\n", offset, cpu_get_pc());
            return 0xff;
        }
    };

    /**
     * ***********************************
     *
     * 8253 timer handlers
     *
     ************************************
     */
    public static WriteHandlerPtr exidy_sh8253_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int chan;

            stream_update(exidy_stream, 0);

            offset &= 3;
            switch (offset) {
                case 0:
                case 1:
                case 2:
                    chan = offset;
                    if (sh8253_timer[chan].u8_clstate == 0) {
                        sh8253_timer[chan].u8_clstate = 1;
                        sh8253_timer[chan].u16_count = (sh8253_timer[chan].u16_count & 0xff00) | (data & 0x00ff);
                    } else {
                        sh8253_timer[chan].u8_clstate = 0;
                        sh8253_timer[chan].u16_count = (sh8253_timer[chan].u16_count & 0x00ff) | ((data << 8) & 0xff00);
                        if (sh8253_timer[chan].u16_count != 0) {
                            sh8253_timer[chan].u32_step = (int) (freq_to_step * (double) SH8253_CLOCK / (double) sh8253_timer[chan].u16_count);
                        } else {
                            sh8253_timer[chan].u32_step = 0;
                        }
                    }
                    break;

                case 3:
                    chan = (data & 0xc0) >> 6;
                    sh8253_timer[chan].u8_enable = ((data & 0x0e) != 0) ? 1 : 0;
                    break;
            }
        }
    };

    public static ReadHandlerPtr exidy_sh8253_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            logerror("8253(R): %x\n", offset);
            return 0;
        }
    };

    /**
     * ***********************************
     *
     * 6840 timer handlers
     *
     ************************************
     */
    public static ReadHandlerPtr exidy_sh6840_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            logerror("6840R %x\n", offset);
            return 0;
        }
    };

    public static WriteHandlerPtr exidy_sh6840_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* force an update of the stream */
            stream_update(exidy_stream, 0);

            /* only look at the low 3 bits */
            offset &= 7;
            switch (offset) {
                /* offset 0 writes to either channel 0 control or channel 2 control */
                case 0:
                    if ((sh6840_timer[1].u8_cr & 0x01) != 0) {
                        sh6840_timer[0].u8_cr = data & 0xFF;
                    } else {
                        sh6840_timer[2].u8_cr = data & 0xFF;
                    }
                    break;

                /* offset 1 writes to channel 1 control */
                case 1:
                    sh6840_timer[1].u8_cr = data & 0xFF;
                    break;

                /* offsets 2/4/6 write to the common MSB latch */
                case 2:
                case 4:
                case 6:
                    u8_sh6840_MSB = data & 0xFF;
                    break;

                /* offsets 3/5/7 write to the LSB controls */
                case 3:
                case 5:
                case 7: {
                    /* latch the timer value */
                    int ch = (offset - 3) / 2;
                    sh6840_timer[ch].counter.SetD((u8_sh6840_MSB << 8) | (data & 0xff));
                    sh6840_timer[ch].u16_timer = sh6840_timer[ch].counter.D;
                    break;
                }
            }
        }
    };

    /**
     * ***********************************
     *
     * External sound effect controls
     *
     ************************************
     */
    public static WriteHandlerPtr exidy_sfxctrl_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            stream_update(exidy_stream, 0);

            offset &= 3;
            switch (offset) {
                case 0:
                    u8_exidy_sfxctrl = data & 0xFF;
                    break;

                case 1:
                case 2:
                case 3:
                    sh6840_volume[offset - 1] = (short) (((data & 7) * BASE_VOLUME) / 7);
                    break;
            }
        }
    };

    /**
     * ***********************************
     *
     * CVSD sound for Mouse Trap
     *
     ************************************
     */
    public static WriteHandlerPtr mtrap_voiceio_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            if ((offset & 0x10) == 0) {
                hc55516_digit_clock_clear_w(0, data);
                hc55516_clock_set_w(0, data);
            }
            if ((offset & 0x20) == 0) {
                u8_riot_portb_data = data & 1;
            }
        }
    };

    public static ReadHandlerPtr mtrap_voiceio_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            if ((offset & 0x80) == 0) {
                int data = (u8_riot_porta_data & 0x06) >> 1;
                data |= (u8_riot_porta_data & 0x01) << 2;
                data |= (u8_riot_porta_data & 0x08);
                return data;
            }
            if ((offset & 0x40) == 0) {
                int clock_pulse = (int) (timer_get_time() * (2.0 * CVSD_CLOCK_FREQ));
                return (clock_pulse & 1) << 7;
            }
            return 0;
        }
    };
}
