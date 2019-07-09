/****************************************************************

	MAME / MESS functions

****************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.sound;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstdio.*;
import static common.ptr.*;
import static mame056.driverH.*;
import static mame056.mame.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame056.sound.ay8910H.*;
import static mame056.sound.ym2413H.*;
import static mame056.sound._2413intfH.*;
import static mame056.sound.streams.*;
import static mame056.sound.ym2413.*;

public class _2413intf  extends snd_interface 
{
	
	static OPLL[] opll = new OPLL[MAX_2413];
	static int[] stream = new int[MAX_2413];
        static int[] ym_latch = new int[MAX_2413];
        static int num;
        
        public _2413intf(){
            this.name = "YM-2413";
            this.sound_num = SOUND_YM2413;
        }
        
        static StreamInitPtr YM2413_update = new StreamInitPtr() {
            public void handler(int ch, ShortPtr buffer, int length) {
                while ((length--) != 0) 
                    buffer.writeinc((short) OPLL_calc (opll[ch]));
            }
        };
	
	static int YM2413_sh_start (MachineSound msound)
	{
		YM2413interface intf = (YM2413interface) msound.sound_interface;
		int i;
		String buf="";
	
		OPLL_init (intf.baseclock/2, Machine.sample_rate);
		num = intf.num;
	
		for (i=0;i<num;i++)
			{
			opll[i] = OPLL_new ();
			if (opll[i]==null) return 1;
			OPLL_reset (opll[i]);
			OPLL_reset_patch (opll[i]);
	
			if (num > 1)
				buf = sprintf ("YM-2413 #%d", i);
			else
				buf = "YM-2413";
	
			stream[i] = stream_init (buf, intf.mixing_level[i],
				Machine.sample_rate, i, YM2413_update);
			}
	
		return 0;
	}
	
	public static ShStopPtr YM2413_sh_stop = new ShStopPtr() {
            public void handler() {
                int i;
	
		for (i=0;i<num;i++)
		{
			OPLL_delete (opll[i]);
		}
		OPLL_close ();
            }
        };
	
	
	void YM2413_sh_reset ()
	{
		int i;
	
		for (i=0;i<num;i++)
		{
			OPLL_reset (opll[i]);
			OPLL_reset_patch (opll[i]);
		}
	}
	
	public static WriteHandlerPtr YM2413_register_port_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { ym_latch[0] = data; } };
	public static WriteHandlerPtr YM2413_register_port_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { ym_latch[1] = data; } };
	public static WriteHandlerPtr YM2413_register_port_2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { ym_latch[2] = data; } };
	public static WriteHandlerPtr YM2413_register_port_3_w = new WriteHandlerPtr() {public void handler(int offset, int data) { ym_latch[3] = data; } };
	
	static void YM2413_write_reg (int chip, int data)
	{
		OPLL_writeReg (opll[chip], ym_latch[chip], data);
		stream_update(stream[chip], chip);
	}
	
	public static WriteHandlerPtr YM2413_data_port_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { YM2413_write_reg (0, data); } };
	public static WriteHandlerPtr YM2413_data_port_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { YM2413_write_reg (1, data); } };
	public static WriteHandlerPtr YM2413_data_port_2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { YM2413_write_reg (2, data); } };
	public static WriteHandlerPtr YM2413_data_port_3_w = new WriteHandlerPtr() {public void handler(int offset, int data) { YM2413_write_reg (3, data); } };
	
	public static WriteHandlerPtr YM2413_register_port_0_lsb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { YM2413_register_port_0_w.handler(offset,data & 0xff); }};
	public static WriteHandlerPtr YM2413_register_port_1_lsb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { YM2413_register_port_1_w.handler(offset,data & 0xff); }};
	public static WriteHandlerPtr YM2413_register_port_2_lsb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { YM2413_register_port_2_w.handler(offset,data & 0xff); }};
	public static WriteHandlerPtr YM2413_register_port_3_lsb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { YM2413_register_port_3_w.handler(offset,data & 0xff); }};
	public static WriteHandlerPtr YM2413_data_port_0_lsb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { YM2413_data_port_0_w.handler(offset,data & 0xff); }};
	public static WriteHandlerPtr YM2413_data_port_1_lsb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { YM2413_data_port_1_w.handler(offset,data & 0xff); }};
	public static WriteHandlerPtr YM2413_data_port_2_lsb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { YM2413_data_port_2_w.handler(offset,data & 0xff); }};
	public static WriteHandlerPtr YM2413_data_port_3_lsb_w = new WriteHandlerPtr() {public void handler(int offset, int data) { YM2413_data_port_3_w.handler(offset,data & 0xff); }};

        @Override
        public int chips_num(MachineSound msound) {
            return ((YM2413interface) msound.sound_interface).num;
        }

        @Override
        public int chips_clock(MachineSound msound) {
            return ((YM2413interface) msound.sound_interface).baseclock;
        }

        @Override
        public int start(MachineSound msound) {
            //return YM2413_sh_start(msound);
            return 0;
        }

        @Override
        public void stop() {
            YM2413_sh_stop.handler();
        }

        @Override
        public void update() {
            //no functionality expected
        }

        @Override
        public void reset() {
            //no functionality expected
        }
}
