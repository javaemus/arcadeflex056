/*$DEADSERIOUSCLAN$*********************************************************************
* FILE
*	Yamaha 3812 emulator interface - MAME VERSION
*
* CREATED BY
*	Ernesto Corvi
*
* UPDATE LOG
*	CHS 1999-01-09	Fixes new ym3812 emulation interface.
*	CHS 1998-10-23	Mame streaming sound chip update
*	EC	1998		Created Interface
*
* NOTES
*
***************************************************************************************/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.sound;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstdio.sprintf;
import static common.ptr.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame056.sound._3812intfH.*;
import static mame037b11.sound.fm.*;
import static mame037b11.sound.fmH.*;
import static mame037b11.sound.fmoplH.*;
import static mame037b11.sound.fmopl.*;
import static mame056.common.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.cpuintrfH.*;
import static mame056.mame.*;
import static mame056.sound.streams.*;


public class _3812intf  extends snd_interface 
{
    
    public _3812intf() {
        sound_num = SOUND_YM3812;
        name = "YM-3812";
    }
	
	/*TODO*///#define OPL3CONVERTFREQUENCY
	
	/* This frequency is from Yamaha 3812 and 2413 documentation */
	public static int ym3812_StdClock = 3579545;
	
	
	/* Emulated YM3812 variables and defines */
	public static int[] stream = new int[MAX_3812];
	static Object[] Timer = new Object[MAX_3812 * 2];
	
	/* Non-Emulated YM3812 variables and defines */
	public static class NE_OPL_STATE {
		public int address_register;
		public int status_register;
		public int timer_register;
		public int timer1_val;
		public int timer2_val;
		public timer_entry timer1;
		public timer_entry timer2;
		public int[] aOPLFreqArray=new int[16];		/* Up to 9 channels.. */
	};
	
	static double timer_step;
	static NE_OPL_STATE[] nonemu_state;
	
	/* These ones are used by both */
	/*static const struct YM3812interface *intf = null; */
	static YM3812interface intf = null;
	
	/* Function procs to access the selected YM type */
	/* static int ( *sh_start )( const struct MachineSound *msound ); */
	/*TODO*///static void ( *sh_stop )(void );
	/*TODO*///static int ( *status_port_r )( int chip );
	/*TODO*///static void ( *control_port_w )( int chip, int data );
	/*TODO*///static void ( *write_port_w )( int chip, int data );
	/*TODO*///static int ( *read_port_r )( int chip );

    @Override
    public int chips_num(MachineSound msound) {
        return ((YM3812interface) msound.sound_interface).num;
    }

    @Override
    public int chips_clock(MachineSound msound) {
        return ((YM3812interface) msound.sound_interface).baseclock;
    }

    @Override
    public int start(MachineSound msound) {
        chiptype = OPL_TYPE_YM3812;
        return OPL_sh_start(msound);
    }

    @Override
    public void stop() {
        YM3812_sh_stop();
    }

    @Override
    public void update() {
        //no functionality expected
    }

    @Override
    public void reset() {
        //no functionality expected
    }

    	
	/**********************************************************************************************
		Begin of non-emulated YM3812 interface block
	 **********************************************************************************************/
	
	public static timer_callback timer1_callback = new timer_callback() {
            public void handler(int chip) {
                NE_OPL_STATE st = nonemu_state[chip];
		if ((st.timer_register & 0x40) == 0)
		{
			if((st.status_register&0x80) == 0)
				if (intf.handler[chip] != null) (intf.handler[chip]).handler(ASSERT_LINE);
			/* set the IRQ and timer 1 signal bits */
			st.status_register |= 0x80|0x40;
		}
	
		/* next! */
		st.timer1 = timer_set ((double)st.timer1_val*4*timer_step, chip, timer1_callback);
            }
        };
	
	public static timer_callback timer2_callback = new timer_callback() {
            public void handler(int chip) {
		NE_OPL_STATE st = nonemu_state[chip];
		if ((st.timer_register & 0x20) == 0)
		{
			if((st.status_register&0x80) == 0)
				if (intf.handler[chip] != null) (intf.handler[chip]).handler(ASSERT_LINE);
			/* set the IRQ and timer 2 signal bits */
			st.status_register |= 0x80|0x20;
		}
	
		/* next! */
		st.timer2 = timer_set ((double)st.timer2_val*16*timer_step, chip, timer2_callback);
	}};
	
	static int nonemu_YM3812_sh_start(MachineSound msound)
	{
		int i;
	
		intf = (Y8950interface) msound.sound_interface;
	
		nonemu_state = new NE_OPL_STATE[intf.num];
		if(nonemu_state==null) return 1;
		/*TODO*///memset(nonemu_state,0,intf.num * sizeof(NE_OPL_STATE));
		for(i=0;i<intf.num;i++)
		{
			nonemu_state[i].address_register = 0;
			nonemu_state[i].timer1 =
			nonemu_state[i].timer2 = null;
			nonemu_state[i].status_register = 0;
			nonemu_state[i].timer_register = 0;
			nonemu_state[i].timer1_val =
			nonemu_state[i].timer2_val = 256;
		}
		timer_step = TIME_IN_HZ((double)intf.baseclock / 72.0);
		return 0;
	}
	
	static void nonemu_YM3812_sh_stop()
	{
		YM3812_sh_reset();
		nonemu_state = null;
	}
	
	static int nonemu_YM3812_status_port_r(int chip)
	{
		NE_OPL_STATE st = nonemu_state[chip];
		/* mask out the timer 1 and 2 signal bits as requested by the timer register */
		return st.status_register & ~(st.timer_register & 0x60);
	}
	
	static void nonemu_YM3812_control_port_w(int chip,int data)
	{
		NE_OPL_STATE st = nonemu_state[chip];
		st.address_register = data;
	
		/* pass through all non-timer registers */
	/*TODO*///#ifdef OPL3CONVERTFREQUENCY
	/*TODO*///	if ( ((data==0xbd)||((data&0xe0)!=0xa0)) && ((data<2)||(data>4)) )
	/*TODO*///#else
	/*TODO*///	if ( ((data<2)||(data>4)) )
	/*TODO*///#endif
			/*TODO*///osd_opl_control(chip,data);
	}
	
	static void nonemu_WriteConvertedFrequency( int chip,int nFrq, int nCh )
	{
		int		nRealOctave;
		double	vRealFrq;
	
		vRealFrq = (((nFrq&0x3ff)<<((nFrq&0x7000)>>12))) * (double)intf.baseclock / (double)ym3812_StdClock;
		nRealOctave = 0;
	
		while( (vRealFrq>1023.0)&&(nRealOctave<7) )
		{
			vRealFrq /= 2.0;
			nRealOctave++;
		}
		/*TODO*///osd_opl_control(chip,0xa0|nCh);
		/*TODO*///osd_opl_write(chip,((int)vRealFrq)&0xff);
		/*TODO*///osd_opl_control(chip,0xb0|nCh);
		/*TODO*///osd_opl_write(chip,((((int)vRealFrq)>>8)&3)|(nRealOctave<<2)|((nFrq&0x8000)>>10) );
	}
	
	static void nonemu_YM3812_write_port_w(int chip,int data)
	{
		NE_OPL_STATE st = nonemu_state[chip];
		int nCh = st.address_register&0x0f;
	
	/*TODO*///#ifdef OPL3CONVERTFREQUENCY
	/*TODO*///	if( (nCh<9) )
	/*TODO*///	{
	/*TODO*///		if( (st.address_register&0xf0) == 0xa0 )
	/*TODO*///		{
	/*TODO*///			st.aOPLFreqArray[nCh] = (st.aOPLFreqArray[nCh] & 0xf300)|(data&0xff);
	/*TODO*///			nonemu_WriteConvertedFrequency(chip, st.aOPLFreqArray[nCh], nCh );
	/*TODO*///			return;
	/*TODO*///		}
	/*TODO*///		else if( (st.address_register&0xf0)==0xb0 )
	/*TODO*///		{
	/*TODO*///			st.aOPLFreqArray[st.address_register&0xf] = (st.aOPLFreqArray[nCh] & 0x00ff)|((data&0x3)<<8)|((data&0x1c)<<10)|((data&0x20)<<10);
	/*TODO*///			nonemu_WriteConvertedFrequency(chip, st.aOPLFreqArray[nCh], nCh );
	/*TODO*///			return;
	/*TODO*///		}
	/*TODO*///	}
	/*TODO*///#endif
		switch (st.address_register)
		{
			case 2:
				st.timer1_val = 256 - data;
				break;
			case 3:
				st.timer2_val = 256 - data;
				break;
			case 4:
				/* bit 7 means reset the IRQ signal and status bits, and ignore all the other bits */
				if ((data & 0x80) != 0)
				{
					if((st.status_register&0x80) != 0)
						if (intf.handler[chip] != null) (intf.handler[chip]).handler(CLEAR_LINE);
					st.status_register = 0;
				}
				else
				{
					/* set the new timer register */
					st.timer_register = data;
						/*  bit 0 starts/stops timer 1 */
					if ((data & 0x01) != 0)
					{
						if (st.timer1 == null)
							st.timer1 = timer_set ((double)st.timer1_val*4*timer_step, chip, timer1_callback);
					}
					else if (st.timer1 != null)
					{
						timer_remove (st.timer1);
						st.timer1 = null;
					}
					/*  bit 1 starts/stops timer 2 */
					if ((data & 0x02) != 0)
					{
						if (st.timer2 == null)
							st.timer2 = timer_set ((double)st.timer2_val*16*timer_step, chip, timer2_callback);
					}
					else if (st.timer2 != null)
					{
						timer_remove (st.timer2);
						st.timer2 = null;
					}
					/* bits 5 & 6 clear and mask the appropriate bit in the status register */
					st.status_register &= ~(data & 0x60);
	
					if((st.status_register&0x7f) == 0)
					{
						if((st.status_register&0x80) != 0)
							if (intf.handler[chip] != null) (intf.handler[chip]).handler(CLEAR_LINE);
						st.status_register &=0x7f;
					}
				}
				break;
			default:
				/*TODO*///osd_opl_write(chip,data);
		}
	}
	
	static int nonemu_YM3812_read_port_r( int chip ) {
		return 0;
	}
	
	/**********************************************************************************************
		End of non-emulated YM3812 interface block
	 **********************************************************************************************/
	
	
	/*TODO*///typedef void (*STREAM_HANDLER)(int param,void *buffer,int length);
	
	static int chiptype;
	static FM_OPL[] F3812=new FM_OPL[MAX_3812];
	
	/* IRQ Handler */
	static OPL_IRQHANDLERPtr IRQHandler = new OPL_IRQHANDLERPtr() {
            public void handler(int n,int irq) {
                
                if ((intf!=null)&&(intf.handler!=null))
                    if (intf.handler[n] != null) (intf.handler[n]).handler(irq!=0 ? ASSERT_LINE : CLEAR_LINE);
            }
        };
	
	/* update handler */
	static StreamInitPtr YM3812UpdateHandler = new streams.StreamInitPtr() {
            public void handler(int n, ShortPtr buf, int length) {
                YM3812UpdateOne(F3812[n],buf,length);
            }
        };
	
	//#if (HAS_Y8950)
	static StreamInitPtr Y8950UpdateHandler = new streams.StreamInitPtr() {
            public void handler(int n, ShortPtr buf, int length) {
		Y8950UpdateOne(F3812[n],buf,length); 
            }
        };
	
	static OPL_PORTHANDLER_RPtr Y8950PortHandler_r = new OPL_PORTHANDLER_RPtr() {
            public int handler(int chip) {
                return ((Y8950interface)intf).portread[chip].handler(chip);
            }
        };
	
	public static OPL_PORTHANDLER_WPtr Y8950PortHandler_w = new OPL_PORTHANDLER_WPtr() {
            public void handler(int chip,int data) {
                ((Y8950interface)intf).portwrite[chip].handler(chip,data);
            }
        };
        
	static OPL_PORTHANDLER_RPtr Y8950KeyboardHandler_r = new OPL_PORTHANDLER_RPtr() {
            public int handler(int chip) {
                return ((Y8950interface)intf).keyboardread[chip].handler(chip);
            }
        };
	
	static OPL_PORTHANDLER_WPtr Y8950KeyboardHandler_w = new OPL_PORTHANDLER_WPtr() {
            public void handler(int chip, int data) {
                ((Y8950interface)intf).keyboardwrite[chip].handler(chip,data);
            }
        };
	
	//#endif
	
	/* Timer overflow callback from timer.c */
	static timer_callback timer_callback_3812 = new timer_callback() {
            public void handler(int param) {
                int n=param>>1;
		int c=param&1;
		Timer[param] = 0;
		OPLTimerOver(F3812[n],c);
            }
        };
	
	/* TimerHandler from fm.c */
	static OPL_TIMERHANDLERPtr TimerHandler = new OPL_TIMERHANDLERPtr() {
            public void handler(int c,double period) {
                if( period == 0 )
		{	/* Reset FM Timer */
			if( Timer[c] != null )
			{
		 		timer_remove (Timer[c]);
				Timer[c] = 0;
			}
		}
		else
		{	/* Start FM Timer */
			Timer[c] = timer_set(period, c, timer_callback_3812 );
		}
            }
        };
	
	/************************************************/
	/* Sound Hardware Start							*/
	/************************************************/
	static int emu_YM3812_sh_start(MachineSound msound)
	{
		int i;
		int rate = Machine.sample_rate;
	
		intf = (Y8950interface) msound.sound_interface;
		if( intf.num > MAX_3812 ) return 1;
	
		/* Timer state clear */
		/*TODO*///memset(Timer,0,sizeof(Timer));
                Timer = null;
	
		/* stream system initialize */
		for (i = 0;i < intf.num;i++)
		{
			/* stream setup */
			String name;
			int vol = intf.mixing_level[i];
			/* emulator create */
			F3812[i] = OPLCreate(chiptype,intf.baseclock,rate);
			if(F3812[i] == null) return 1;
			/* stream setup */
			name=sprintf("%s #%d",sound_name(msound),i);
	/*TODO*///#if (HAS_Y8950)
			/* ADPCM ROM DATA */
			if(chiptype == OPL_TYPE_Y8950)
			{
				F3812[i].deltat.memory = (memory_region(((Y8950interface)intf).rom_region[i]));
				F3812[i].deltat.memory_size = memory_region_length(((Y8950interface)intf).rom_region[i]);
				stream[i] = stream_init(name,vol,rate,i,Y8950UpdateHandler);
				/* port and keyboard handler */
				OPLSetPortHandler(F3812[i],Y8950PortHandler_w,Y8950PortHandler_r,i);
				OPLSetKeyboardHandler(F3812[i],Y8950KeyboardHandler_w,Y8950KeyboardHandler_r,i);
			}
			else
	/*TODO*///#endif
			stream[i] = stream_init(name,vol,rate,i,YM3812UpdateHandler);
			/* YM3812 setup */
			OPLSetTimerHandler(F3812[i],TimerHandler,i*2);
			OPLSetIRQHandler(F3812[i]  ,IRQHandler,i);
			OPLSetUpdateHandler(F3812[i],stream_updateptr,stream[i]);
		}
		return 0;
	}
        
        public static OPL_UPDATEHANDLERPtr stream_updateptr = new OPL_UPDATEHANDLERPtr() {
            @Override
            public void handler(int param, int min_interval_us) {
                stream_update(param, min_interval_us);
            }
        };
	
	/************************************************/
	/* Sound Hardware Stop							*/
	/************************************************/
	static void emu_YM3812_sh_stop()
	{
		int i;
	
		for (i = 0;i < intf.num;i++)
		{
			OPLDestroy(F3812[i]);
		}
	}
	
	/* reset */
	static void emu_YM3812_sh_reset()
	{
		int i;
	
		for (i = 0;i < intf.num;i++)
			OPLResetChip(F3812[i]);
	}
	
	static int emu_YM3812_status_port_r(int chip)
	{
		return OPLRead(F3812[chip],0);
	}
	static void emu_YM3812_control_port_w(int chip,int data)
	{
		OPLWrite(F3812[chip],0,data);
	}
	static void emu_YM3812_write_port_w(int chip,int data)
	{
		OPLWrite(F3812[chip],1,data);
	}
	
	static int emu_YM3812_read_port_r(int chip)
	{
		return OPLRead(F3812[chip],1);
	}
	
	/**********************************************************************************************
		Begin of YM3812 interface stubs block
	 **********************************************************************************************/
	
	static int OPL_sh_start(MachineSound msound)
	{
		/*if ( options.use_emulated_ym3812 != 0 ) {
			sh_stop  = emu_YM3812_sh_stop;
			status_port_r = emu_YM3812_status_port_r;
			control_port_w = emu_YM3812_control_port_w;
			write_port_w = emu_YM3812_write_port_w;
			read_port_r = emu_YM3812_read_port_r;
			return emu_YM3812_sh_start(msound);
		} else {
			sh_stop = nonemu_YM3812_sh_stop;
			status_port_r = nonemu_YM3812_status_port_r;
			control_port_w = nonemu_YM3812_control_port_w;
			write_port_w = nonemu_YM3812_write_port_w;
			read_port_r = nonemu_YM3812_read_port_r;
			return nonemu_YM3812_sh_start(msound);
		}*/
            int i;
            int rate = Machine.sample_rate;

            intf = (YM3812interface) msound.sound_interface;
            if (intf.num > MAX_3812) {
                return 1;
            }

            /* Timer state clear */
            for (i = 0; i < Timer.length; i++) {
                Timer[i] = null;//memset(Timer,0,sizeof(Timer));
            }
            /* stream system initialize */
            for (i = 0; i < intf.num; i++) {
                /* stream setup */
                String name;
                int vol = intf.mixing_level[i];
                /* emulator create */
                F3812[i] = OPLCreate(chiptype, intf.baseclock, rate);
                if (F3812[i] == null) {
                    return 1;
                }
                /* stream setup */
                name = sprintf("%s #%d", sound_name(msound), i);
                stream[i] = stream_init(name, vol, rate, i, YM3812UpdateHandler);
                /* YM3812 setup */
                OPLSetTimerHandler(F3812[i], TimerHandler, i * 2);
                OPLSetIRQHandler(F3812[i], IRQHandler, i);
                OPLSetUpdateHandler(F3812[i], stream_updateptr, stream[i]);
            }
            return 0;
	}
	
	int YM3812_sh_start(MachineSound msound)
	{
		chiptype = OPL_TYPE_YM3812;
		return OPL_sh_start(msound);
	}
	
	void YM3812_sh_stop() {
		//(*sh_stop)();
                int i;

                for (i = 0; i < intf.num; i++) {
                    OPLDestroy(F3812[i]);
                }
	}
	
	public static void YM3812_sh_reset()
	{
		int i;
	
		for(i=0xff;i<=0;i--)
		{
			YM3812_control_port_0_w.handler(0,i);
			YM3812_write_port_0_w.handler(0,0);
		}
		/* IRQ clear */
		YM3812_control_port_0_w.handler(0,4);
		YM3812_write_port_0_w.handler(0,0x80);
	}
	
	public static WriteHandlerPtr YM3812_control_port_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		//control_port_w( 0, data );
                OPLWrite(F3812[0], 0, data);
	} };
	
	public static WriteHandlerPtr YM3812_write_port_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		//(*write_port_w)( 0, data );
                OPLWrite(F3812[0], 1, data);
	} };
	
	public static ReadHandlerPtr YM3812_status_port_0_r  = new ReadHandlerPtr() { public int handler(int offset) {
		//return (*status_port_r)( 0 );
                return OPLRead(F3812[0], 0);
	} };
	
	public static ReadHandlerPtr YM3812_read_port_0_r  = new ReadHandlerPtr() { public int handler(int offset) {
		//return (*read_port_r)( 0 );
                return OPLRead(F3812[0], 1);
	} };
	
	public static WriteHandlerPtr YM3812_control_port_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		//(*control_port_w)( 1, data );
                OPLWrite(F3812[1], 0, data);
	} };
	
	public static WriteHandlerPtr YM3812_write_port_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		//(*write_port_w)( 1, data );
                OPLWrite(F3812[1], 1, data);
	} };
	
	public static ReadHandlerPtr YM3812_status_port_1_r  = new ReadHandlerPtr() { public int handler(int offset) {
		//return (*status_port_r)( 1 );
                return OPLRead(F3812[1], 0);
	} };
	
	public static ReadHandlerPtr YM3812_read_port_1_r  = new ReadHandlerPtr() { public int handler(int offset) {
		//return (*read_port_r)( 1 );
                return OPLRead(F3812[1], 1);
	} };
	
	/**********************************************************************************************
		End of YM3812 interface stubs block
	 **********************************************************************************************/
	
	/**********************************************************************************************
		Begin of YM3526 interface stubs block
	 **********************************************************************************************/
	int YM3526_sh_start(MachineSound msound)
	{
		chiptype = OPL_TYPE_YM3526;
		return OPL_sh_start(msound);
	}
	
	/**********************************************************************************************
		End of YM3526 interface stubs block
	 **********************************************************************************************/
	
	/**********************************************************************************************
		Begin of Y8950 interface stubs block
	 **********************************************************************************************/
	public static int Y8950_sh_start(MachineSound msound)
	{
		chiptype = OPL_TYPE_Y8950;
		if( OPL_sh_start(msound) != 0) return 1;
		/* !!!!! port handler set !!!!! */
		/* !!!!! delta-t memory address set !!!!! */
		return 0;
	}
		
	/**********************************************************************************************
		End of Y8950 interface stubs block
	 **********************************************************************************************/
}
