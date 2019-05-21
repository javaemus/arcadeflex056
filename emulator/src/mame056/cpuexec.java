/**
 * Ported to 0.56
 */
package mame056;

import static arcadeflex056.fucPtr.*;

import static mame056.cpuexecH.*;
import static mame056.driverH.*;
import static mame056.hiscore.*;
import static mame056.sndintrf.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame056.mame.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static arcadeflex036.osdepend.*;
import static mame056.inptport.*;

public class cpuexec {

    /*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Debug logging
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///#define VERBOSE 0
/*TODO*///
/*TODO*///#if VERBOSE
/*TODO*///#define LOG(x)	logerror x
/*TODO*///#else
/*TODO*///#define LOG(x)
/*TODO*///#endif
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Macros to help verify active CPU
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///#define VERIFY_ACTIVECPU(retval, name)						
/*TODO*///	int activecpu = cpu_getactivecpu();						
/*TODO*///	if (activecpu < 0)										
/*TODO*///	{														
/*TODO*///		logerror(#name "() called with no active cpu!\n");	
/*TODO*///		return retval;										
/*TODO*///	}
/*TODO*///
/*TODO*///#define VERIFY_ACTIVECPU_VOID(name)							
/*TODO*///	int activecpu = cpu_getactivecpu();						
/*TODO*///	if (activecpu < 0)										
/*TODO*///	{														
/*TODO*///		logerror(#name "() called with no active cpu!\n");	
/*TODO*///		return;												
/*TODO*///	}
    /**
     * ***********************************
     *
     * Triggers for the timer system
     *
     ************************************
     */
    public static final int TRIGGER_TIMESLICE = -1000;
    public static final int TRIGGER_INT = -2000;
    public static final int TRIGGER_YIELDTIME = -3000;
    public static final int TRIGGER_SUSPENDTIME = -4000;

    /**
     * ***********************************
     *
     * Internal CPU info structure
     *
     ************************************
     */
    public static class cpuinfo {

        int iloops;/* number of interrupts remaining this frame */
        int totalcycles;/* total CPU cycles executed */
        int vblankint_countdown;/* number of vblank callbacks left until we interrupt */
        int vblankint_multiplier;/* number of vblank callbacks per interrupt */
        Object vblankint_timer;/* reference to elapsed time counter */
        double vblankint_period;/* timing period of the VBLANK interrupt */
        Object timedint_timer;/* reference to this CPU's timer */
        double timedint_period;/* timing period of the timed interrupt */

        public static cpuinfo[] create(int n) {
            cpuinfo[] a = new cpuinfo[n];
            for (int k = 0; k < n; k++) {
                a[k] = new cpuinfo();
            }
            return a;
        }
    }

    /**
     * ***********************************
     *
     * General CPU variables
     *
     ************************************
     */
    static cpuinfo[] cpu_exec = cpuinfo.create(MAX_CPU);
    static int time_to_reset;
    static int time_to_quit;

    static int vblank;
    static int current_frame;
    static int watchdog_counter;

    static int[] cycles_running = new int[1];

    /**
     * ***********************************
     *
     * CPU interrupt variables
     *
     ************************************
     */
    static int[]/*UINT8*/ interrupt_enable = new int[MAX_CPU];
    static int[] interrupt_vector = new int[MAX_CPU];

    static int[][]/*UINT8*/ irq_line_state = new int[MAX_CPU][MAX_IRQ_LINES];
    static int[][] irq_line_vector = new int[MAX_CPU][MAX_IRQ_LINES];

    /**
     * ***********************************
     *
     * Timer variables
     *
     ************************************
     */
    static Object vblank_timer;
    static int vblank_countdown;
    static int vblank_multiplier;
    static double vblank_period;

    static Object refresh_timer;
    static double refresh_period;
    static double refresh_period_inv;

    static Object timeslice_timer;
    static double timeslice_period;

    static double scanline_period;
    static double scanline_period_inv;

    /*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Save/load variables
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static int loadsave_schedule;
/*TODO*///static char loadsave_schedule_id;
/*TODO*///
    public static irqcallbacksPtr cpu_0_irq_callback = new irqcallbacksPtr() {
        public int handler(int irqline) {
            return cpu_irq_callback(0, irqline);
        }
    };
    public static irqcallbacksPtr cpu_1_irq_callback = new irqcallbacksPtr() {
        public int handler(int irqline) {
            return cpu_irq_callback(1, irqline);
        }
    };
    public static irqcallbacksPtr cpu_2_irq_callback = new irqcallbacksPtr() {
        public int handler(int irqline) {
            return cpu_irq_callback(2, irqline);
        }
    };
    public static irqcallbacksPtr cpu_3_irq_callback = new irqcallbacksPtr() {
        public int handler(int irqline) {
            return cpu_irq_callback(3, irqline);
        }
    };
    public static irqcallbacksPtr cpu_4_irq_callback = new irqcallbacksPtr() {
        public int handler(int irqline) {
            return cpu_irq_callback(4, irqline);
        }
    };
    public static irqcallbacksPtr cpu_5_irq_callback = new irqcallbacksPtr() {
        public int handler(int irqline) {
            return cpu_irq_callback(5, irqline);
        }
    };
    public static irqcallbacksPtr cpu_6_irq_callback = new irqcallbacksPtr() {
        public int handler(int irqline) {
            return cpu_irq_callback(6, irqline);
        }
    };
    public static irqcallbacksPtr cpu_7_irq_callback = new irqcallbacksPtr() {
        public int handler(int irqline) {
            return cpu_irq_callback(7, irqline);
        }
    };

    public static irqcallbacksPtr[] cpu_irq_callbacks = {
        cpu_0_irq_callback, cpu_1_irq_callback, cpu_2_irq_callback, cpu_3_irq_callback,
        cpu_4_irq_callback, cpu_5_irq_callback, cpu_6_irq_callback, cpu_7_irq_callback
    };

    public static irqcallbacksPtr[] drv_irq_callbacks = new irqcallbacksPtr[MAX_CPU];

    /**
     * ***********************************
     *
     * Initialize all the CPUs
     *
     ************************************
     */
    public static int cpu_init() {
        int cpunum;

        /* initialize the interfaces first */
        if (cpuintrf_init() != 0) {
            return 1;
        }

        /* count how many CPUs we have to emulate */
        for (cpunum = 0; cpunum < MAX_CPU; cpunum++) {
            int cputype = Machine.drv.cpu[cpunum].cpu_type & ~CPU_FLAGS_MASK;
            int irqline;

            /* stop when we hit a dummy */
            if (cputype == CPU_DUMMY) {
                break;
            }

            /* set the save state tag */
 /*TODO*///		state_save_set_current_tag(cpunum + 1);

            /* initialize this CPU */
            if (cpuintrf_init_cpu(cpunum, cputype) != 0) {
                return 1;
            }

            /* reset the IRQ lines */
            for (irqline = 0; irqline < MAX_IRQ_LINES; irqline++) {
                irq_line_state[cpunum][irqline] = CLEAR_LINE;
                irq_line_vector[cpunum][irqline] = cpunum_default_irq_vector(cpunum);
            }
        }
        /*TODO*///
/*TODO*///	/* save some stuff in tag 0 */
/*TODO*///	state_save_set_current_tag(0);
/*TODO*///	state_save_register_UINT8("cpu", 0, "irq enable",     interrupt_enable,  cpunum);
/*TODO*///	state_save_register_INT32("cpu", 0, "irq vector",     interrupt_vector,  cpunum);
/*TODO*///	state_save_register_UINT8("cpu", 0, "irqline state",  &irq_line_state[0][0],  cpunum * MAX_IRQ_LINES);
/*TODO*///	state_save_register_INT32("cpu", 0, "irqline vector", &irq_line_vector[0][0], cpunum * MAX_IRQ_LINES);
/*TODO*///	state_save_register_INT32("cpu", 0, "watchdog count", &watchdog_counter, 1);

        /* init the timer system */
        timer_init();
        timeslice_timer = refresh_timer = vblank_timer = null;

        return 0;
    }

    /**
     * ***********************************
     *
     * Prepare the system for execution
     *
     ************************************
     */
    static void cpu_pre_run() {
        int cpunum;

        logerror("Machine reset\n");

        /* read hi scores information from hiscore.dat */
        hs_open(Machine.gamedrv.name);
        hs_init();

        /* initialize the various timers (suspends all CPUs at startup) */
        cpu_inittimers();
        watchdog_counter = -1;

        /* reset sound chips */
        sound_reset();

        /* first pass over CPUs */
        for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {
            /* enable all CPUs (except for audio CPUs if the sound is off) */
            if ((Machine.drv.cpu[cpunum].cpu_type & CPU_AUDIO_CPU) == 0 || Machine.sample_rate != 0) {
                timer_suspendcpu(cpunum, 0, SUSPEND_ANY_REASON);
            } else {
                timer_suspendcpu(cpunum, 1, SUSPEND_REASON_DISABLE);
            }

            /* start with interrupts enabled, so the generic routine will work even if */
 /* the machine doesn't have an interrupt enable port */
            interrupt_enable[cpunum] = 1;
            interrupt_vector[cpunum] = 0xff;

            /* reset any driver hooks into the IRQ acknowledge callbacks */
            drv_irq_callbacks[cpunum] = null;

            /* reset the total number of cycles */
            cpu_exec[cpunum].totalcycles = 0;
        }

        vblank = 0;

        /* do this AFTER the above so init_machine() can use cpu_halt() to hold the */
 /* execution of some CPUs, or disable interrupts */
        if (Machine.drv.init_machine != null) {
            (Machine.drv.init_machine).handler();
        }

        /* now reset each CPU */
        for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {
            cpunum_reset(cpunum, Machine.drv.cpu[cpunum].reset_param, cpu_irq_callbacks[cpunum]);
        }

        /* reset the globals */
        cpu_vblankreset();
        current_frame = 0;
        /*TODO*///	state_save_dump_registry();
    }

    /**
     * ***********************************
     *
     * Finish up execution
     *
     ************************************
     */
    static void cpu_post_run() {
        /* write hi scores to disk - No scores saving if cheat */
        hs_close();

    }

    /**
     * ***********************************
     *
     * Execute until done
     *
     ************************************
     */
    public static void cpu_run() {
        /* loop over multiple resets, until the user quits */
        time_to_quit = 0;
        int[] cpunum = new int[1];
        while (time_to_quit == 0) {
            /* prepare everything to run */
            cpu_pre_run();

            /* loop until the user quits or resets */
            time_to_reset = 0;
            while (time_to_quit == 0 && time_to_reset == 0) {

                /* if we have a load/save scheduled, handle it */
 /*TODO*///			if (loadsave_schedule != LOADSAVE_NONE)
/*TODO*///				handle_loadsave();

                /* ask the timer system to schedule */
                if (timer_schedule_cpu(cpunum, cycles_running) != 0) {
                    int ran;

                    /* run for the requested number of cycles */
                    ran = cpunum_execute(cpunum[0], cycles_running[0]);

                    /* update based on how many cycles we really ran */
                    cpu_exec[cpunum[0]].totalcycles += ran;

                    /* update the timer with how long we actually ran */
                    timer_update_cpu(cpunum[0], ran);
                }

            }

            /* finish up this iteration */
            cpu_post_run();
        }
    }

    


    /*************************************
     *
     *	Deinitialize all the CPUs
     *
     *************************************/

    public static void cpu_exit()
    {
            int cpunum;

            /* shut down the CPU cores */
            for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
                    cpuintrf_exit_cpu(cpunum);
    }
    /**
     * ***********************************
     *
     * Force a reset at the end of this timeslice
     *
     ************************************
     */
    public static void machine_reset() {
        time_to_reset = 1;
    }


    /*TODO*///
/*TODO*///
/*TODO*///#if 0
/*TODO*///#pragma mark -
/*TODO*///#pragma mark SAVE/RESTORE
/*TODO*///#endif
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Handle saves at runtime
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void handle_save(void)
/*TODO*///{
/*TODO*///	char name[2] = { 0 };
/*TODO*///	void *file;
/*TODO*///	int cpunum;
/*TODO*///
/*TODO*///	/* open the file */
/*TODO*///	name[0] = loadsave_schedule_id;
/*TODO*///	file = osd_fopen(Machine->gamedrv->name, name, OSD_FILETYPE_STATE, 1);
/*TODO*///
/*TODO*///	/* write the save state */
/*TODO*///	state_save_save_begin(file);
/*TODO*///
/*TODO*///	/* write tag 0 */
/*TODO*///	state_save_set_current_tag(0);
/*TODO*///	state_save_save_continue();
/*TODO*///
/*TODO*///	/* loop over CPUs */
/*TODO*///	for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
/*TODO*///	{
/*TODO*///		cpuintrf_push_context(cpunum);
/*TODO*///
/*TODO*///		/* make sure banking is set */
/*TODO*///		activecpu_reset_banking();
/*TODO*///
/*TODO*///		/* save the CPU data */
/*TODO*///		state_save_set_current_tag(cpunum + 1);
/*TODO*///		state_save_save_continue();
/*TODO*///
/*TODO*///		cpuintrf_pop_context();
/*TODO*///	}
/*TODO*///
/*TODO*///	/* finish and close */
/*TODO*///	state_save_save_finish();
/*TODO*///	osd_fclose(file);
/*TODO*///
/*TODO*///	/* unschedule the save */
/*TODO*///	loadsave_schedule = LOADSAVE_NONE;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Handle loads at runtime
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void handle_load(void)
/*TODO*///{
/*TODO*///	char name[2] = { 0 };
/*TODO*///	void *file;
/*TODO*///	int cpunum;
/*TODO*///
/*TODO*///	/* open the file */
/*TODO*///	name[0] = loadsave_schedule_id;
/*TODO*///	file = osd_fopen(Machine->gamedrv->name, name, OSD_FILETYPE_STATE, 0);
/*TODO*///
/*TODO*///	/* if successful, load it */
/*TODO*///	if (file)
/*TODO*///	{
/*TODO*///		/* start loading */
/*TODO*///		if (!state_save_load_begin(file))
/*TODO*///		{
/*TODO*///			/* read tag 0 */
/*TODO*///			state_save_set_current_tag(0);
/*TODO*///			state_save_load_continue();
/*TODO*///
/*TODO*///			/* loop over CPUs */
/*TODO*///			for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
/*TODO*///			{
/*TODO*///				cpuintrf_push_context(cpunum);
/*TODO*///
/*TODO*///				/* make sure banking is set */
/*TODO*///				activecpu_reset_banking();
/*TODO*///
/*TODO*///				/* load the CPU data */
/*TODO*///				state_save_set_current_tag(cpunum + 1);
/*TODO*///				state_save_load_continue();
/*TODO*///
/*TODO*///				cpuintrf_pop_context();
/*TODO*///			}
/*TODO*///
/*TODO*///			/* finish and close */
/*TODO*///			state_save_load_finish();
/*TODO*///		}
/*TODO*///		osd_fclose(file);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* unschedule the load */
/*TODO*///	loadsave_schedule = LOADSAVE_NONE;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Handle saves & loads at runtime
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void handle_loadsave(void)
/*TODO*///{
/*TODO*///	/* it's one or the other */
/*TODO*///	if (loadsave_schedule == LOADSAVE_SAVE)
/*TODO*///		handle_save();
/*TODO*///	else if (loadsave_schedule == LOADSAVE_LOAD)
/*TODO*///		handle_load();
/*TODO*///
/*TODO*///	/* reset the schedule */
/*TODO*///	loadsave_schedule = LOADSAVE_NONE;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Schedules a save/load for later
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_loadsave_schedule(int type, char id)
/*TODO*///{
/*TODO*///	loadsave_schedule = type;
/*TODO*///	loadsave_schedule_id = id;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Unschedules any saves or loads
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_loadsave_reset(void)
/*TODO*///{
/*TODO*///	loadsave_schedule = LOADSAVE_NONE;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///#if 0
/*TODO*///#pragma mark -
/*TODO*///#pragma mark WATCHDOG
/*TODO*///#endif
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Watchdog routines
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////*--------------------------------------------------------------
/*TODO*///
/*TODO*///	Use these functions to initialize, and later maintain, the
/*TODO*///	watchdog. For convenience, when the machine is reset, the
/*TODO*///	watchdog is disabled. If you call this function, the
/*TODO*///	watchdog is initialized, and from that point onwards, if you
/*TODO*///	don't call it at least once every 3 seconds, the machine
/*TODO*///	will be reset.
/*TODO*///
/*TODO*///	The 3 seconds delay is targeted at qzshowby, which otherwise
/*TODO*///	would reset at the start of a game.
/*TODO*///
/*TODO*///--------------------------------------------------------------*/
    static void watchdog_reset() {
        if (watchdog_counter == -1) {
            logerror("watchdog armed\n");
        }
        watchdog_counter = (int) (3 * Machine.drv.frames_per_second);
    }

    public static WriteHandlerPtr watchdog_reset_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            watchdog_reset();
        }
    };

    public static ReadHandlerPtr watchdog_reset_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            watchdog_reset();
            return 0xff;
        }
    };

    /*TODO*///
/*TODO*///
/*TODO*///WRITE16_HANDLER( watchdog_reset16_w )
/*TODO*///{
/*TODO*///	watchdog_reset();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///READ16_HANDLER( watchdog_reset16_r )
/*TODO*///{
/*TODO*///	watchdog_reset();
/*TODO*///	return 0xffff;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///WRITE32_HANDLER( watchdog_reset32_w )
/*TODO*///{
/*TODO*///	watchdog_reset();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///READ32_HANDLER( watchdog_reset32_r )
/*TODO*///{
/*TODO*///	watchdog_reset();
/*TODO*///	return 0xffffffff;
/*TODO*///}
/*TODO*///
    /**
     * ***********************************
     *
     * Handle reset line changes
     *
     ************************************
     */
    public static timer_callback reset_callback = new timer_callback() {
        public void handler(int param) {
            int cpunum = param & 0xff;
            int state = param >> 8;

            /* if we're asserting the line, just halt the CPU */
            if (state == ASSERT_LINE) {
                timer_suspendcpu(cpunum, 1, SUSPEND_REASON_RESET);
                return;
            }

            /* if we're clearing the line that was previously asserted, or if we're just */
 /* pulsing the line, reset the CPU */
            if ((state == CLEAR_LINE && timer_iscpususpended(cpunum, SUSPEND_REASON_RESET) != 0) || state == PULSE_LINE) {
                cpunum_reset(cpunum, Machine.drv.cpu[cpunum].reset_param, cpu_irq_callbacks[cpunum]);
            }

            /* if we're clearing the line, make sure the CPU is not halted */
            timer_suspendcpu(cpunum, 0, SUSPEND_REASON_RESET);
        }
    };

    public static void cpu_set_reset_line(int cpunum, int state) {
        timer_set(TIME_NOW, (cpunum & 0xff) | (state << 8), reset_callback);
    }

    /**
     * ***********************************
     *
     * Handle halt line changes
     *
     ************************************
     */
    public static timer_callback halt_callback = new timer_callback() {
        public void handler(int param) {
            int cpunum = param & 0xff;
            int state = param >> 8;

            /* if asserting, halt the CPU */
            if (state == ASSERT_LINE) {
                timer_suspendcpu(cpunum, 1, SUSPEND_REASON_HALT);
            } /* if clearing, unhalt the CPU */ else if (state == CLEAR_LINE) {
                timer_suspendcpu(cpunum, 0, SUSPEND_REASON_HALT);
            }
        }
    };

    public static void cpu_set_halt_line(int cpunum, int state) {
        timer_set(TIME_NOW, (cpunum & 0xff) | (state << 8), halt_callback);
    }

    /**
     * ***********************************
     *
     * Return suspended status of CPU
     *
     ************************************
     */
    public static int cpu_getstatus(int cpunum) {
        if (cpunum < cpu_gettotalcpu()) {
            return timer_iscpususpended(cpunum, SUSPEND_REASON_HALT | SUSPEND_REASON_RESET | SUSPEND_REASON_DISABLE) == 0 ? 1 : 0;
        }
        return 0;
    }

    /**
     * ***********************************
     *
     * Return cycles ran this iteration
     *
     ************************************
     */
    static int cycles_currently_ran() {
        int activecpu = cpu_getactivecpu();
        if (activecpu < 0) {
            logerror("cycles_currently_ran() called with no active cpu!\n");
            return 0;
        }
        return cycles_running[0] - activecpu_get_icount();
    }

    /*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Return cycles remaining in this
/*TODO*/// *	iteration
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///int cycles_left_to_run(void)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU(0, cycles_left_to_run);
/*TODO*///	return activecpu_get_icount();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Return total number of CPU cycles
/*TODO*/// *	for the active CPU.
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////*--------------------------------------------------------------
/*TODO*///
/*TODO*///	IMPORTANT: this value wraps around in a relatively short
/*TODO*///	time. For example, for a 6MHz CPU, it will wrap around in
/*TODO*///	2^32/6000000 = 716 seconds = 12 minutes.
/*TODO*///	Make sure you don't do comparisons between values returned
/*TODO*///	by this function, but only use the difference (which will
/*TODO*///	be correct regardless of wraparound).
/*TODO*///
/*TODO*///--------------------------------------------------------------*/
/*TODO*///
    public static int cpu_gettotalcycles() {
        int activecpu = cpu_getactivecpu();
        if (activecpu < 0) {
            logerror("cpu_gettotalcycles() called with no active cpu!\n");
            return 0;
        }
        return cpu_exec[activecpu].totalcycles + cycles_currently_ran();
    }

    /*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Return cycles until next interrupt
/*TODO*/// *	handler call
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///int cpu_geticount(void)
/*TODO*///{
/*TODO*///	int result;
/*TODO*///
/*TODO*////* remove me - only used by mamedbg, m92 */
/*TODO*///	VERIFY_ACTIVECPU(0, cpu_geticount);
/*TODO*///	result = TIME_TO_CYCLES(activecpu, cpu[activecpu].vblankint_period - timer_timeelapsed(cpu[activecpu].vblankint_timer));
/*TODO*///	return (result < 0) ? 0 : result;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Scales a given value by the fraction
/*TODO*/// *	of time elapsed between refreshes
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
    public static int cpu_scalebyfcount(int value) {
        int result = (int) ((double) value * timer_timeelapsed(refresh_timer) * refresh_period_inv);
        if (value >= 0) {
            return (result < value) ? result : value;
        } else {
            return (result > value) ? result : value;
        }
    }

    /*************************************
    *
    *	Returns the current scanline
    *
    *************************************/

   /*--------------------------------------------------------------

           Note: cpu_getscanline() counts from 0, 0 being the first
           visible line. You might have to adjust this value to match
           the hardware, since in many cases the first visible line
           is >0.

   --------------------------------------------------------------*/

   public static int cpu_getscanline()
   {
           return (int)(timer_timeelapsed(refresh_timer) * scanline_period_inv);
   }



    /*************************************
     *
     *	Returns time until given scanline
     *
     *************************************/

    public static double cpu_getscanlinetime(int scanline)
    {
            double scantime = timer_starttime(refresh_timer) + (double)scanline * scanline_period;
            double abstime = timer_get_time();
            double result;

            /* if we're already past the computed time, count it for the next frame */
            if (abstime >= scantime)
                    scantime += TIME_IN_HZ(Machine.drv.frames_per_second);

            /* compute how long from now until that time */
            result = scantime - abstime;

            /* if it's small, just count a whole frame */
            if (result < TIME_IN_NSEC(1))
                    result = TIME_IN_HZ(Machine.drv.frames_per_second);
            return result;
    }



    /*************************************
     *
     *	Returns time for one scanline
     *
     *************************************/

    public static double cpu_getscanlineperiod()
    {
            return scanline_period;
    }



    /*************************************
     *
     *	Returns a crude approximation
     *	of the horizontal position of the
     *	bream
     *
     *************************************/

    public static int cpu_gethorzbeampos()
    {
            double elapsed_time = timer_timeelapsed(refresh_timer);
            int scanline = (int)(elapsed_time * scanline_period_inv);
            double time_since_scanline = elapsed_time - (double)scanline * scanline_period;
            return (int)(time_since_scanline * scanline_period_inv * (double)Machine.drv.screen_width);
    }


    /**
     * ***********************************
     *
     * Returns the VBLANK state
     *
     ************************************
     */
    public static int cpu_getvblank() {
        return vblank;
    }

    /**
     * ***********************************
     *
     * Returns the current frame count
     *
     ************************************
     */
    public static int cpu_getcurrentframe() {
        return current_frame;
    }

    /*************************************
    *
    *	Set IRQ callback for drivers
    *
    *************************************/

   public static void cpu_set_irq_callback(int cpunum, irqcallbacksPtr callback)
   {
           drv_irq_callbacks[cpunum] = callback;
   }


    /**
     * ***********************************
     *
     * Internal IRQ callbacks
     *
     ************************************
     */
    public static int cpu_irq_callback(int cpunum, int irqline) {
        int vector = irq_line_vector[cpunum][irqline];
        //LOG(("cpu_%d_irq_callback(%d) $%04xn", cpunum, irqline, vector));

        /* if the IRQ state is HOLD_LINE, clear it */
        if (irq_line_state[cpunum][irqline] == HOLD_LINE) {
            //LOG(("->set_irq_line(%d,%d,%d)\n", cpunum, irqline, CLEAR_LINE));
            activecpu_set_irq_line(irqline, INTERNAL_CLEAR_LINE);
            irq_line_state[cpunum][irqline] = CLEAR_LINE;
        }

        /* if there's a driver callback, run it */
        if (drv_irq_callbacks[cpunum] != null) {
            vector = (drv_irq_callbacks[cpunum]).handler(irqline);
        }

        /* otherwise, just return the current vector */
        return vector;
    }

    /**
     * ***********************************
     *
     * Set the IRQ vector for a given IRQ line on a CPU
     *
     ************************************
     */
    public static void cpu_irq_line_vector_w(int cpunum, int irqline, int vector) {
        if (cpunum < cpu_gettotalcpu() && irqline >= 0 && irqline < MAX_IRQ_LINES) {
            //LOG(("cpu_irq_line_vector_w(%d,%d,$%04x)\n",cpunum,irqline,vector));
            irq_line_vector[cpunum][irqline] = vector;
            return;
        }
        //LOG(("cpu_irq_line_vector_w CPU#%d irqline %d > max irq lines\n", cpunum, irqline));
    }

    /**
     * ***********************************
     *
     * Generate a IRQ interrupt
     *
     ************************************
     */
    public static timer_callback cpu_manualirqcallback = new timer_callback() {
        public void handler(int param) {
            int cpunum = param & 0x0f;
            int state = (param >> 4) & 0x0f;
            int irqline = (param >> 8) & 0x7f;
            int set_vector = (param >> 15) & 0x01;
            int vector = param >> 16;

            //LOG(("cpu_manualirqcallback %d,%d,%d\n",cpunum,irqline,state));

            /* swap to the CPU's context */
            cpuintrf_push_context(cpunum);

            /* set the IRQ line state and vector */
            if (irqline >= 0 && irqline < MAX_IRQ_LINES) {
                irq_line_state[cpunum][irqline] = state;
                if (set_vector != 0) {
                    irq_line_vector[cpunum][irqline] = vector;
                }
            }

            /* switch off the requested state */
            switch (state) {
                case PULSE_LINE:
                    activecpu_set_irq_line(irqline, INTERNAL_ASSERT_LINE);
                    activecpu_set_irq_line(irqline, INTERNAL_CLEAR_LINE);
                    break;

                case HOLD_LINE:
                case ASSERT_LINE:
                    activecpu_set_irq_line(irqline, INTERNAL_ASSERT_LINE);
                    break;

                case CLEAR_LINE:
                    activecpu_set_irq_line(irqline, INTERNAL_CLEAR_LINE);
                    break;

                default:
                    logerror("cpu_manualirqcallback cpu #%d, line %d, unknown state %d\n", cpunum, irqline, state);
            }
            cpuintrf_pop_context();

            /* generate a trigger to unsuspend any CPUs waiting on the interrupt */
            if (state != CLEAR_LINE) {
                cpu_triggerint(cpunum);
            }
        }
    };

    public static void cpu_set_irq_line(int cpunum, int irqline, int state) {
        int vector = 0xff;

        /* don't trigger interrupts on suspended CPUs */
        if (cpu_getstatus(cpunum) == 0) {
            return;
        }

        /* determine the current vector */
        if (irqline >= 0 && irqline < MAX_IRQ_LINES) {
            vector = irq_line_vector[cpunum][irqline];
        }

        //LOG(("cpu_set_irq_line(%d,%d,%d,%02x)\n", cpunum, irqline, state, vector));

        /* set a timer to go off */
        timer_set(TIME_NOW, (cpunum & 0x0f) | ((state & 0x0f) << 4) | ((irqline & 0x7f) << 8), cpu_manualirqcallback);
    }

    public static void cpu_set_irq_line_and_vector(int cpunum, int irqline, int state, int vector) {
        /* don't trigger interrupts on suspended CPUs */
        if (cpu_getstatus(cpunum) == 0) {
            return;
        }

        //LOG(("cpu_set_irq_line(%d,%d,%d,%02x)\n", cpunum, irqline, state, vector));

        /* set a timer to go off */
        timer_set(TIME_NOW, (cpunum & 0x0f) | ((state & 0x0f) << 4) | ((irqline & 0x7f) << 8) | (1 << 15) | (vector << 16), cpu_manualirqcallback);
    }

    /**
     * ***********************************
     *
     * Old-style interrupt generation
     *
     ************************************
     */
    public static void cpu_cause_interrupt(int cpunum, int type) {
        /* special case for none */
        if (type == INTERRUPT_NONE) {
            return;
        } /* special case for NMI type */ else if (type == INTERRUPT_NMI) {
            cpu_set_irq_line(cpunum, IRQ_LINE_NMI, PULSE_LINE);
        } /* otherwise, convert to an IRQ */ else {
            int[] vector = new int[1];
            int irqline;
            irqline = convert_type_to_irq_line(cpunum, type, vector);
            cpu_set_irq_line_and_vector(cpunum, irqline, HOLD_LINE, vector[0]);
        }
    }

    /**
     * ***********************************
     *
     * Interrupt enabling
     *
     ************************************
     */
    public static timer_callback cpu_clearintcallback = new timer_callback() {
        public void handler(int cpunum) {
            int irqcount = cputype_get_interface(Machine.drv.cpu[cpunum].cpu_type & ~CPU_FLAGS_MASK).num_irqs;
            int irqline;

            cpuintrf_push_context(cpunum);

            /* clear NMI and all IRQs */
            activecpu_set_irq_line(IRQ_LINE_NMI, INTERNAL_CLEAR_LINE);
            for (irqline = 0; irqline < irqcount; irqline++) {
                activecpu_set_irq_line(irqline, INTERNAL_CLEAR_LINE);
            }

            cpuintrf_pop_context();
        }
    };

    public static void cpu_interrupt_enable(int cpunum, int enabled) {
        interrupt_enable[cpunum] = enabled;

        //LOG(("CPU#%d interrupt_enable=%d\n", cpunum, enabled));

        /* make sure there are no queued interrupts */
        if (enabled == 0) {
            timer_set(TIME_NOW, cpunum, cpu_clearintcallback);
        }
    }

    public static WriteHandlerPtr interrupt_enable_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int activecpu = cpu_getactivecpu();
            if (activecpu < 0) {
                logerror("interrupt_enable_w() called with no active cpu!\n");
                return;
            }
            cpu_interrupt_enable(activecpu, data);
        }
    };

    public static WriteHandlerPtr interrupt_vector_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int activecpu = cpu_getactivecpu();
            if (activecpu < 0) {
                logerror("interrupt_vector_w() called with no active cpu!\n");
                return;
            }
            if (interrupt_vector[activecpu] != data) {
                //LOG(("CPU#%d interrupt_vector_w $%02x\n", activecpu, data));
                interrupt_vector[activecpu] = data;

                /* make sure there are no queued interrupts */
                timer_set(TIME_NOW, activecpu, cpu_clearintcallback);
            }
        }
    };

    /**
     * ***********************************
     *
     * Interrupt generation callbacks
     *
     ************************************
     */
    public static InterruptPtr interrupt = new InterruptPtr() {
        public int handler() {
            int val = 0;
            int activecpu = cpu_getactivecpu();
            if (activecpu < 0) {
                logerror("interrupt() called with no active cpu!\n");
                return INTERRUPT_NONE;
            }
            if (interrupt_enable[activecpu] == 0) {
                return INTERRUPT_NONE;
            }

            val = activecpu_default_irq_line();
            return (val == -1000) ? interrupt_vector[activecpu] : val;
        }
    };
    public static InterruptPtr nmi_interrupt = new InterruptPtr() {
        public int handler() {
            int activecpu = cpu_getactivecpu();
            if (activecpu < 0) {
                logerror("nmi_interrupt() called with no active cpu!\n");
                return INTERRUPT_NONE;
            }
            //LOG(("nmi_interrupt: interrupt_enable[%d]=%d\n", activecpu, interrupt_enable[activecpu]));
            if (interrupt_enable[activecpu] != 0) {
                cpu_set_nmi_line(activecpu, PULSE_LINE);
            }
            return INTERRUPT_NONE;
        }
    };
    public static InterruptPtr ignore_interrupt = new InterruptPtr() {
        public int handler() {
            int activecpu = cpu_getactivecpu();
            if (activecpu < 0) {
                logerror("ignore_interrupt() called with no active cpu!\n");
                return INTERRUPT_NONE;
            }
            return INTERRUPT_NONE;
        }
    };

    /*TODO*///
/*TODO*///
/*TODO*///#if (HAS_M68000 || HAS_M68010 || HAS_M68020 || HAS_M68EC020)
/*TODO*///
/*TODO*///INLINE int m68_irq(int level)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU(INTERRUPT_NONE, m68_irq);
/*TODO*///	if (interrupt_enable[activecpu])
/*TODO*///	{
/*TODO*///		cpu_irq_line_vector_w(activecpu, level, MC68000_INT_ACK_AUTOVECTOR);
/*TODO*///		cpu_set_irq_line(activecpu, level, HOLD_LINE);
/*TODO*///	}
/*TODO*///	return INTERRUPT_NONE;
/*TODO*///}
/*TODO*///
/*TODO*///int m68_level1_irq(void) { return m68_irq(1); }
/*TODO*///int m68_level2_irq(void) { return m68_irq(2); }
/*TODO*///int m68_level3_irq(void) { return m68_irq(3); }
/*TODO*///int m68_level4_irq(void) { return m68_irq(4); }
/*TODO*///int m68_level5_irq(void) { return m68_irq(5); }
/*TODO*///int m68_level6_irq(void) { return m68_irq(6); }
/*TODO*///int m68_level7_irq(void) { return m68_irq(7); }
/*TODO*///
/*TODO*///#endif
/*TODO*///
/*TODO*///
    /**
     * ***********************************
     *
     * Generate a specific trigger
     *
     ************************************
     */
    public static timer_callback cpu_trigger = new timer_callback() {
        public void handler(int trigger) {
            timer_trigger(trigger);
        }
    };

    /**
     * ***********************************
     *
     * Generate a trigger in the future
     *
     ************************************
     */
    public static void cpu_triggertime(double duration, int trigger) {
        timer_set(duration, trigger, cpu_trigger);
    }

    /**
     * ***********************************
     *
     * Generate a trigger for an int
     *
     ************************************
     */
    public static void cpu_triggerint(int cpunum) {
        timer_trigger(TRIGGER_INT + cpunum);
    }

    /**
     * ***********************************
     *
     * Burn/yield CPU cycles until a trigger
     *
     ************************************
     */
    public static void cpu_spinuntil_trigger(int trigger) {
        timer_suspendcpu_trigger(activecpu, trigger);
    }

    public static void cpu_yielduntil_trigger(int trigger) {
        int activecpu = cpu_getactivecpu();
        if (activecpu < 0) {
            logerror("cpu_yielduntil_trigger() called with no active cpu!\n");
            return;
        }
        timer_holdcpu_trigger(activecpu, trigger);
    }

    /**
     * ***********************************
     *
     * Burn/yield CPU cycles until an interrupt
     *
     ************************************
     */
    public static void cpu_spinuntil_int() {
        int activecpu = cpu_getactivecpu();
        if (activecpu < 0) {
            logerror("cpu_spinuntil_int() called with no active cpu!\n");
            return;
        }
        cpu_spinuntil_trigger(TRIGGER_INT + activecpu);
    }

    /*TODO*///
/*TODO*///
/*TODO*///void cpu_yielduntil_int(void)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU_VOID(cpu_yielduntil_int);
/*TODO*///	cpu_yielduntil_trigger(TRIGGER_INT + activecpu);
/*TODO*///}
    /**
     * ***********************************
     *
     * Burn/yield CPU cycles until the end of the current timeslice
     *
     ************************************
     */
    public static void cpu_spin() {
        cpu_spinuntil_trigger(TRIGGER_TIMESLICE);
    }

    public static void cpu_yield() {
        cpu_yielduntil_trigger(TRIGGER_TIMESLICE);
    }

    /**
     * ***********************************
     *
     * Burn/yield CPU cycles for a specific period of time
     *
     ************************************
     */
    static int timetrig_spinuntil_time = 0;

    public static void cpu_spinuntil_time(double duration) {
        cpu_spinuntil_trigger(TRIGGER_SUSPENDTIME + timetrig_spinuntil_time);
        cpu_triggertime(duration, TRIGGER_SUSPENDTIME + timetrig_spinuntil_time);
        timetrig_spinuntil_time = (timetrig_spinuntil_time + 1) & 255;
    }

    
    static int timetrig = 0;
    
    public static void cpu_yielduntil_time(double duration)
    {
            

            cpu_yielduntil_trigger(TRIGGER_YIELDTIME + timetrig);
            cpu_triggertime(duration, TRIGGER_YIELDTIME + timetrig);
            timetrig = (timetrig + 1) & 255;
    }
    /**
     * ***********************************
     *
     * Returns the number of times the interrupt handler will be called before
     * the end of the current video frame.
     *
     ************************************
     */

    /*--------------------------------------------------------------

            This can be useful to interrupt handlers to synchronize
            their operation. If you call this from outside an interrupt
            handler, add 1 to the result, i.e. if it returns 0, it means
            that the interrupt handler will be called once.

    --------------------------------------------------------------*/
    public static int cpu_getiloops() {
        int activecpu = cpu_getactivecpu();
        if (activecpu < 0) {
            logerror("cpu_getiloops() called with no active cpu!\n");
            return 0;
        }
        return cpu_exec[activecpu].iloops;
    }

    /**
     * ***********************************
     *
     * Hook for updating things on the real VBLANK (once per frame)
     *
     ************************************
     */
    static void cpu_vblankreset() {
        int cpunum;

        /* read hi scores from disk */
        hs_update();

        /* read keyboard & update the status of the input ports */
        update_input_ports();

        /* reset the cycle counters */
        for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {
            if (timer_iscpususpended(cpunum, SUSPEND_REASON_DISABLE) == 0) {
                cpu_exec[cpunum].iloops = Machine.drv.cpu[cpunum].vblank_interrupts_per_frame - 1;
            } else {
                cpu_exec[cpunum].iloops = -1;
            }
        }
    }

    /**
     * ***********************************
     *
     * First-run callback for VBLANKs
     *
     ************************************
     */
    public static timer_callback cpu_firstvblankcallback = new timer_callback() {
        public void handler(int param) {
            /* now that we're synced up, pulse from here on out */
            vblank_timer = timer_pulse(vblank_period, param, cpu_vblankcallback);

            /* but we need to call the standard routine as well */
            cpu_vblankcallback.handler(param);
        }
    };
    /**
     * ***********************************
     *
     * VBLANK core handler
     *
     ************************************
     */

    public static timer_callback cpu_vblankcallback = new timer_callback() {
        public void handler(int param) {
            int cpunum;

            /* loop over CPUs */
            for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {
                /* if the interrupt multiplier is valid */
                if (cpu_exec[cpunum].vblankint_multiplier != -1) {
                    /* decrement; if we hit zero, generate the interrupt and reset the countdown */
                    if (--cpu_exec[cpunum].vblankint_countdown == 0) {
                        /* a param of -1 means don't call any callbacks */
                        if (param != -1) {
                            /* if the CPU has a VBLANK handler, call it */
                            if (Machine.drv.cpu[cpunum].vblank_interrupt != null && cpu_getstatus(cpunum) != 0) {
                                cpuintrf_push_context(cpunum);
                                cpu_cause_interrupt(cpunum, Machine.drv.cpu[cpunum].vblank_interrupt.handler());
                                cpuintrf_pop_context();
                            }

                            /* update the counters */
                            cpu_exec[cpunum].iloops--;
                        }

                        /* reset the countdown and timer */
                        cpu_exec[cpunum].vblankint_countdown = cpu_exec[cpunum].vblankint_multiplier;
                        timer_reset(cpu_exec[cpunum].vblankint_timer, TIME_NEVER);
                    }
                } /* else reset the VBLANK timer if this is going to be a real VBLANK */ else if (vblank_countdown == 1) {
                    timer_reset(cpu_exec[cpunum].vblankint_timer, TIME_NEVER);
                }
            }

            /* is it a real VBLANK? */
            if (--vblank_countdown == 0) {
                /* do we update the screen now? */
                if ((Machine.drv.video_attributes & VIDEO_UPDATE_AFTER_VBLANK) == 0) {
                    time_to_quit = updatescreen();
                }

                /* Set the timer to update the screen */
                timer_set(TIME_IN_USEC(Machine.drv.vblank_duration), 0, cpu_updatecallback);
                vblank = 1;

                /* reset the globals */
                cpu_vblankreset();

                /* reset the counter */
                vblank_countdown = vblank_multiplier;
            }
        }
    };

    /**
     * ***********************************
     *
     * End-of-VBLANK callback
     *
     ************************************
     */
    public static timer_callback cpu_updatecallback = new timer_callback() {
        public void handler(int param) {
            /* update the screen if we didn't before */
            if ((Machine.drv.video_attributes & VIDEO_UPDATE_AFTER_VBLANK) != 0) {
                time_to_quit = updatescreen();
            }
            vblank = 0;

            /* update IPT_VBLANK input ports */
            inputport_vblank_end();

            /* check the watchdog */
            if (watchdog_counter > 0) {
                if (--watchdog_counter == 0) {
                    logerror("reset caused by the watchdog\n");
                    machine_reset();
                }
            }

            /* track total frames */
            current_frame++;

            /* reset the refresh timer */
            timer_reset(refresh_timer, TIME_NEVER);
        }
    };
    /**
     * ***********************************
     *
     * Callback for timed interrupts (not tied to a VBLANK)
     *
     ************************************
     */
    public static timer_callback cpu_timedintcallback = new timer_callback() {
        public void handler(int param) {
            /* bail if there is no routine */
            if (Machine.drv.cpu[param].timed_interrupt != null && cpu_getstatus(param) != 0) {
                cpuintrf_push_context(param);
                cpu_cause_interrupt(param, Machine.drv.cpu[param].timed_interrupt.handler());
                cpuintrf_pop_context();
            }
        }
    };

    /**
     * ***********************************
     *
     * Converts an integral timing rate into a period
     *
     ************************************
     */

    /*--------------------------------------------------------------

            Rates can be specified as follows:

                    rate <= 0		-> 0
                    rate < 50000	-> 'rate' cycles per frame
                    rate >= 50000	-> 'rate' nanoseconds

    --------------------------------------------------------------*/
    static double cpu_computerate(int value) {
        /* values equal to zero are zero */
        if (value <= 0) {
            return 0.0;
        }

        /* values above between 0 and 50000 are in Hz */
        if (value < 50000) {
            return TIME_IN_HZ(value);
        } /* values greater than 50000 are in nanoseconds */ else {
            return TIME_IN_NSEC(value);
        }
    }
    /**
     * ***********************************
     *
     * Callback to force a timeslice
     *
     ************************************
     */
    public static timer_callback cpu_timeslicecallback = new timer_callback() {
        public void handler(int i) {
            timer_trigger(TRIGGER_TIMESLICE);
        }
    };

    /**
     * ***********************************
     *
     * Setup all the core timers
     *
     ************************************
     */
    static void cpu_inittimers() {
        double first_time;
        int cpunum, max, ipf;

        /* remove old timers */
        if (timeslice_timer != null) {
            timer_remove(timeslice_timer);
        }
        if (refresh_timer != null) {
            timer_remove(refresh_timer);
        }
        if (vblank_timer != null) {
            timer_remove(vblank_timer);
        }

        /* allocate a dummy timer at the minimum frequency to break things up */
        ipf = Machine.drv.cpu_slices_per_frame;
        if (ipf <= 0) {
            ipf = 1;
        }
        timeslice_period = TIME_IN_HZ(Machine.drv.frames_per_second * ipf);
        timeslice_timer = timer_pulse(timeslice_period, 0, cpu_timeslicecallback);

        /* allocate an infinite timer to track elapsed time since the last refresh */
        refresh_period = TIME_IN_HZ(Machine.drv.frames_per_second);
        refresh_period_inv = 1.0 / refresh_period;
        refresh_timer = timer_set(TIME_NEVER, 0, null);

        /* while we're at it, compute the scanline times */
        if (Machine.drv.vblank_duration != 0) {
            scanline_period = (refresh_period - TIME_IN_USEC(Machine.drv.vblank_duration))
                    / (double) (Machine.visible_area.max_y - Machine.visible_area.min_y + 1);
        } else {
            scanline_period = refresh_period / (double) Machine.drv.screen_height;
        }
        scanline_period_inv = 1.0 / scanline_period;

        /*
	 *	The following code finds all the CPUs that are interrupting in sync with the VBLANK
	 *	and sets up the VBLANK timer to run at the minimum number of cycles per frame in
	 *	order to service all the synced interrupts
         */

 /* find the CPU with the maximum interrupts per frame */
        max = 1;
        for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {
            ipf = Machine.drv.cpu[cpunum].vblank_interrupts_per_frame;
            if (ipf > max) {
                max = ipf;
            }
        }

        /* now find the LCD with the rest of the CPUs (brute force - these numbers aren't huge) */
        vblank_multiplier = max;
        while (true) {
            for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {
                ipf = Machine.drv.cpu[cpunum].vblank_interrupts_per_frame;
                if (ipf > 0 && (vblank_multiplier % ipf) != 0) {
                    break;
                }
            }
            if (cpunum == cpu_gettotalcpu()) {
                break;
            }
            vblank_multiplier += max;
        }

        /* initialize the countdown timers and intervals */
        for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {
            ipf = Machine.drv.cpu[cpunum].vblank_interrupts_per_frame;
            if (ipf > 0) {
                cpu_exec[cpunum].vblankint_countdown = cpu_exec[cpunum].vblankint_multiplier = vblank_multiplier / ipf;
            } else {
                cpu_exec[cpunum].vblankint_countdown = cpu_exec[cpunum].vblankint_multiplier = -1;
            }
        }

        /* allocate a vblank timer at the frame rate * the LCD number of interrupts per frame */
        vblank_period = TIME_IN_HZ(Machine.drv.frames_per_second * vblank_multiplier);
        vblank_timer = timer_pulse(vblank_period, 0, cpu_vblankcallback);
        vblank_countdown = vblank_multiplier;

        /*
	 *		The following code creates individual timers for each CPU whose interrupts are not
	 *		synced to the VBLANK, and computes the typical number of cycles per interrupt
         */

 /* start the CPU interrupt timers */
        for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {
            ipf = Machine.drv.cpu[cpunum].vblank_interrupts_per_frame;

            /* remove old timers */
            if (cpu_exec[cpunum].vblankint_timer != null) {
                timer_remove(cpu_exec[cpunum].vblankint_timer);
            }
            if (cpu_exec[cpunum].timedint_timer != null) {
                timer_remove(cpu_exec[cpunum].timedint_timer);
            }

            /* compute the average number of cycles per interrupt */
            if (ipf <= 0) {
                ipf = 1;
            }
            cpu_exec[cpunum].vblankint_period = TIME_IN_HZ(Machine.drv.frames_per_second * ipf);
            cpu_exec[cpunum].vblankint_timer = timer_set(TIME_NEVER, 0, null);

            /* see if we need to allocate a CPU timer */
            ipf = Machine.drv.cpu[cpunum].timed_interrupts_per_second;
            if (ipf != 0) {
                cpu_exec[cpunum].timedint_period = cpu_computerate(ipf);
                cpu_exec[cpunum].timedint_timer = timer_pulse(cpu_exec[cpunum].timedint_period, cpunum, cpu_timedintcallback);
            }
        }

        /* note that since we start the first frame on the refresh, we can't pulse starting
	   immediately; instead, we back up one VBLANK period, and inch forward until we hit
	   positive time. That time will be the time of the first VBLANK timer callback */
        timer_remove(vblank_timer);

        first_time = -TIME_IN_USEC(Machine.drv.vblank_duration) + vblank_period;
        while (first_time < 0) {
            cpu_vblankcallback.handler(-1);
            first_time += vblank_period;
        }
        vblank_timer = timer_set(first_time, 0, cpu_firstvblankcallback);
    }

}
