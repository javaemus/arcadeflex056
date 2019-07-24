/***********************************************************

     Astrocade custom 'IO' chip sound chip driver
	 Frank Palazzolo

	 Portions copied from the Pokey emulator by Ron Fries

	 First Release:
	 	09/20/98

	 Issues:
	 	Noise generators need work
		Can do lots of speed optimizations

***********************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.sound;

import static WIP.mame056.sound.astrocdeH.*;
import static arcadeflex056.fucPtr.*;
import static common.libc.cstdlib.rand;
import common.ptr.ShortPtr;
import static common.subArrays.*;
import static mame056.cpu.z80.z80H.Z80_BC;
import static mame056.cpuintrfH.*;
import static mame056.mame.Machine;
import static mame056.sndintrfH.*;
import static mame056.sndintrf.*;
import static mame056.sound.mixer.*;
import static mame037b11.sound.mixer.*;
import static mame056.sound.mixerH.*;

public class astrocde extends snd_interface
{
	
	static astrocade_interface intf;
	
	static int emulation_rate;
	static int div_by_N_factor;
	static int buffer_len;
	
	static ShortPtr astrocade_buffer = new ShortPtr(MAX_ASTROCADE_CHIPS * 2);
	
	static int[] sample_pos = new int[MAX_ASTROCADE_CHIPS];
	
	static int[] current_count_A = new int[MAX_ASTROCADE_CHIPS];
	static int[] current_count_B = new int[MAX_ASTROCADE_CHIPS];
	static int[] current_count_C = new int[MAX_ASTROCADE_CHIPS];
	static int[] current_count_V = new int[MAX_ASTROCADE_CHIPS];
	static int[] current_count_N = new int[MAX_ASTROCADE_CHIPS * 1024];
	
	static int[] current_state_A = new int[MAX_ASTROCADE_CHIPS];
	static int[] current_state_B = new int[MAX_ASTROCADE_CHIPS];
	static int[] current_state_C = new int[MAX_ASTROCADE_CHIPS];
	static int[] current_state_V = new int[MAX_ASTROCADE_CHIPS];
	
	static int[] current_size_A = new int[MAX_ASTROCADE_CHIPS];
	static int[] current_size_B = new int[MAX_ASTROCADE_CHIPS];
	static int[] current_size_C = new int[MAX_ASTROCADE_CHIPS];
	static int[] current_size_V = new int[MAX_ASTROCADE_CHIPS];
	static int[] current_size_N = new int[MAX_ASTROCADE_CHIPS];
	
	static int channel;
	
	/* Registers */
	
	static int[] master_osc = new int[MAX_ASTROCADE_CHIPS];
	static int[] freq_A = new int[MAX_ASTROCADE_CHIPS];
	static int[] freq_B = new int[MAX_ASTROCADE_CHIPS];
	static int[] freq_C = new int[MAX_ASTROCADE_CHIPS];
	static int[] vol_A = new int[MAX_ASTROCADE_CHIPS];
	static int[] vol_B = new int[MAX_ASTROCADE_CHIPS];
	static int[] vol_C = new int[MAX_ASTROCADE_CHIPS];
	static int[] vibrato = new int[MAX_ASTROCADE_CHIPS];
	static int[] vibrato_speed = new int[MAX_ASTROCADE_CHIPS];
	static int[] mux = new int[MAX_ASTROCADE_CHIPS];
	static int[] noise_am = new int[MAX_ASTROCADE_CHIPS];
	static int[] vol_noise4 = new int[MAX_ASTROCADE_CHIPS];
	static int[] vol_noise8 = new int[MAX_ASTROCADE_CHIPS];
	
	static int randbyte = 0;
	static int randbit = 1;
        
        public astrocde() {
            this.sound_num = SOUND_ASTROCADE;
            this.name = "Astrocade";
        }
	
	static void astrocade_update(int num, int newpos)
	{
		ShortPtr buffer = new ShortPtr(astrocade_buffer, num);
	
		int pos = sample_pos[num];
		int i, data, data16, noise_plus_osc, vib_plus_osc;
	
		for(i=pos; i<newpos; i++)
		{
			if (current_count_N[i] == 0)
			{
				randbyte = rand() & 0xff;
			}
	
			current_size_V[num] = 32768*vibrato_speed[num]/div_by_N_factor;
	
			if (mux[num] == 0)
			{
				if (current_state_V[num] == -1)
					vib_plus_osc = (master_osc[num]-vibrato[num])&0xff;
				else
					vib_plus_osc = master_osc[num];
				current_size_A[num] = vib_plus_osc*freq_A[num]/div_by_N_factor;
				current_size_B[num] = vib_plus_osc*freq_B[num]/div_by_N_factor;
				current_size_C[num] = vib_plus_osc*freq_C[num]/div_by_N_factor;
			}
			else
			{
				noise_plus_osc = ((master_osc[num]-(vol_noise8[num]&randbyte)))&0xff;
				current_size_A[num] = noise_plus_osc*freq_A[num]/div_by_N_factor;
				current_size_B[num] = noise_plus_osc*freq_B[num]/div_by_N_factor;
				current_size_C[num] = noise_plus_osc*freq_C[num]/div_by_N_factor;
				current_size_N[num] = 2*noise_plus_osc/div_by_N_factor;
			}
	
			data = (current_state_A[num]*vol_A[num] +
				    current_state_B[num]*vol_B[num] +
				    current_state_C[num]*vol_C[num]);
	
			if (noise_am[num] != 0)
			{
				randbit = rand() & 1;
				data = data + randbit*vol_noise4[num];
			}
	
			data16 = data<<8;
			buffer.write(pos++, (short) data16);
	
			if (current_count_A[num] >= current_size_A[num])
			{
				current_state_A[num] = -current_state_A[num];
				current_count_A[num] = 0;
			}
			else
				current_count_A[num]++;
	
			if (current_count_B[num] >= current_size_B[num])
			{
				current_state_B[num] = -current_state_B[num];
				current_count_B[num] = 0;
			}
			else
				current_count_B[num]++;
	
			if (current_count_C[num] >= current_size_C[num])
			{
				current_state_C[num] = -current_state_C[num];
				current_count_C[num] = 0;
			}
			else
				current_count_C[num]++;
	
			if (current_count_V[num] >= current_size_V[num])
			{
				current_state_V[num] = -current_state_V[num];
				current_count_V[num] = 0;
			}
			else
				current_count_V[num]++;
	
			if (current_count_N[num] >= current_size_N[num])
			{
				current_count_N[num] = 0;
			}
			else
				current_count_N[num]++;
		}
		sample_pos[num]    = pos;
	}
	
	public static ShStartPtr astrocade_sh_start = new ShStartPtr() {
            public int handler(MachineSound msound) {
		int i;
	
		intf = (astrocade_interface) msound.sound_interface;
	
		if (Machine.sample_rate == 0)
		{
			return 0;
		}
	
		buffer_len = (int) (Machine.sample_rate / Machine.drv.frames_per_second);
	
		emulation_rate = (int) (buffer_len * Machine.drv.frames_per_second);
		div_by_N_factor = intf.baseclock/emulation_rate;
	
		channel = mixer_allocate_channels(intf.num,intf.volume);
		/* reserve buffer */
		for (i = 0;i < intf.num;i++)
		{
			if ((astrocade_buffer = new ShortPtr(buffer_len)) == null)
			{
				while (--i >= 0) astrocade_buffer.write(i, 0);
				return 1;
			}
			/* reset state */
			sample_pos[i] = 0;
			current_count_A[i] = 0;
			current_count_B[i] = 0;
			current_count_C[i] = 0;
			current_count_V[i] = 0;
			current_count_N[i] = 0;
			current_state_A[i] = 1;
			current_state_B[i] = 1;
			current_state_C[i] = 1;
			current_state_V[i] = 1;
		}
	
		return 0;
            }
        };
	
	public static ShStopPtr astrocade_sh_stop = new ShStopPtr() {
                public void handler() {
                    int i;
	
                    for (i = 0;i < intf.num;i++){
                            astrocade_buffer.write(i, (short)0);
                    }
                }
            };
	
	public static void astrocade_sound_w(int num, int offset, int data)
	{
		int i, bvalue, temp_vib;
	
		/* update */
		astrocade_update(num,sound_scalebufferpos(buffer_len));
	
		switch(offset)
		{
			case 0:  /* Master Oscillator */
/*TODO*///	#ifdef VERBOSE
/*TODO*///				logerror("Master Osc Write: %02x\n",data);
/*TODO*///	#endif
				master_osc[num] = data+1;
			break;
	
			case 1:  /* Tone A Frequency */
/*TODO*///	#ifdef VERBOSE
/*TODO*///				logerror("Tone A Write:        %02x\n",data);
/*TODO*///	#endif
				freq_A[num] = data+1;
			break;
	
			case 2:  /* Tone B Frequency */
/*TODO*///	#ifdef VERBOSE
/*TODO*///				logerror("Tone B Write:           %02x\n",data);
/*TODO*///	#endif
				freq_B[num] = data+1;
			break;
	
			case 3:  /* Tone C Frequency */
/*TODO*///	#ifdef VERBOSE
/*TODO*///				logerror("Tone C Write:              %02x\n",data);
/*TODO*///	#endif
				freq_C[num] = data+1;
			break;
	
			case 4:  /* Vibrato Register */
/*TODO*///	#ifdef VERBOSE
/*TODO*///				logerror("Vibrato Depth:                %02x\n",data&0x3f);
/*TODO*///				logerror("Vibrato Speed:                %02x\n",data>>6);
/*TODO*///	#endif
				vibrato[num] = data & 0x3f;
	
				temp_vib = (data>>6) & 0x03;
				vibrato_speed[num] = 1;
				for(i=0;i<temp_vib;i++)
					vibrato_speed[num] <<= 1;
	
			break;
	
			case 5:  /* Tone C Volume, Noise Modulation Control */
				vol_C[num] = data & 0x0f;
				mux[num] = (data>>4) & 0x01;
				noise_am[num] = (data>>5) & 0x01;
/*TODO*///	#ifdef VERBOSE
/*TODO*///				logerror("Tone C Vol:                      %02x\n",vol_C[num]);
/*TODO*///				logerror("Mux Source:                      %02x\n",mux[num]);
/*TODO*///				logerror("Noise Am:                        %02x\n",noise_am[num]);
/*TODO*///	#endif
			break;
	
			case 6:  /* Tone A & B Volume */
				vol_B[num] = (data>>4) & 0x0f;
				vol_A[num] = data & 0x0f;
/*TODO*///	#ifdef VERBOSE
/*TODO*///				logerror("Tone A Vol:                         %02x\n",vol_A[num]);
/*TODO*///				logerror("Tone B Vol:                         %02x\n",vol_B[num]);
/*TODO*///	#endif
			break;
	
			case 7:  /* Noise Volume Register */
				vol_noise8[num] = data;
				vol_noise4[num] = (data>>4) & 0x0f;
/*TODO*///	#ifdef VERBOSE
/*TODO*///				logerror("Noise Vol:                             %02x\n",vol_noise8[num]);
/*TODO*///				logerror("Noise Vol (4):                         %02x\n",vol_noise4[num]);
/*TODO*///	#endif
			break;
	
			case 8:  /* Sound Block Transfer */
	
				bvalue = (cpu_get_reg(Z80_BC) >> 8) & 0x07;
	
				astrocade_sound_w(num, bvalue, data);
	
			break;
		}
	}
	
	public static WriteHandlerPtr astrocade_sound1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		astrocade_sound_w(0, offset, data);
	} };
	
	public static WriteHandlerPtr astrocade_sound2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		astrocade_sound_w(1, offset, data);
	} };
	
	
	public static ShUpdatePtr astrocade_sh_update = new ShUpdatePtr() {
            public void handler() {
                int num;
	
		if (Machine.sample_rate == 0 ) return;
	
		for (num = 0;num < intf.num;num++)
		{
			astrocade_update(num, buffer_len);
			/* reset position , step , count */
			sample_pos[num] = 0;
			/* play sound */
			mixer_play_streamed_sample_16(channel+num,new ShortPtr(astrocade_buffer, num),2*buffer_len,emulation_rate);
		}
            }
        };

    @Override
    public int chips_num(MachineSound msound) {
        return ((astrocade_interface) msound.sound_interface).num;
    }

    @Override
    public int chips_clock(MachineSound msound) {
        return 0;
    }

    @Override
    public int start(MachineSound msound) {
        return astrocade_sh_start.handler(msound);
    }

    @Override
    public void stop() {
        astrocade_sh_stop.handler();
    }

    @Override
    public void update() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void reset() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
	
}
