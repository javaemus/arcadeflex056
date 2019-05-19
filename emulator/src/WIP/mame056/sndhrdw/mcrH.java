/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.sndhrdw;

import static mame056.cpuexecH.*;
import static mame056.cpuexec.*;
import static mame056.cpuintrfH.*;

import static mame056.sndintrfH.*;
import static WIP.mame056.sndhrdw.mcr.*;

public class mcrH {
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/************ Generic MCR routines ***************/
/*TODO*///	
/*TODO*///	
/*TODO*///	void ssio_reset_w(int state);
/*TODO*///	
/*TODO*///	void csdeluxe_reset_w(int state);
/*TODO*///	
/*TODO*///	void turbocs_reset_w(int state);
/*TODO*///	
/*TODO*///	void soundsgood_reset_w(int state);
/*TODO*///	
/*TODO*///	void squawkntalk_reset_w(int state);
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/************ Sound Configuration ***************/
/*TODO*///	
/*TODO*///	extern UINT8 mcr_sound_config;

    public static int MCR_SSIO = 0x01;
    public static int MCR_CHIP_SQUEAK_DELUXE = 0x02;
    public static int MCR_SOUNDS_GOOD = 0x04;
    public static int MCR_TURBO_CHIP_SQUEAK = 0x08;
    public static int MCR_SQUAWK_N_TALK = 0x10;
    public static int MCR_WILLIAMS_SOUND = 0x20;

    public static void MCR_CONFIGURE_SOUND(int x){
		mcr_sound_config = x;
    }

    public static MachineCPU SOUND_CPU_SSIO = new MachineCPU
    (												
            CPU_Z80 | CPU_AUDIO_CPU,					
            2000000,	/* 2 MHz */						
            ssio_readmem,ssio_writemem,null,null,				
            interrupt,26								
    );
	
    public static MachineSound SOUND_SSIO = new MachineSound
    (
            SOUND_AY8910,								
            ssio_ay8910_interface						
    );
	
	
	
/*TODO*///	/************ Chip Squeak Deluxe CPU and sound definitions ***************/
/*TODO*///	
/*TODO*///	extern const struct Memory_ReadAddress16 csdeluxe_readmem[];
/*TODO*///	extern const struct Memory_WriteAddress16 csdeluxe_writemem[];
/*TODO*///	
/*TODO*///	extern struct DACinterface mcr_dac_interface;
/*TODO*///	
/*TODO*///	#define SOUND_CPU_CHIP_SQUEAK_DELUXE				
/*TODO*///		{												
/*TODO*///			CPU_M68000 | CPU_AUDIO_CPU,					
/*TODO*///			15000000/2,	/* 7.5 MHz */					
/*TODO*///			csdeluxe_readmem,csdeluxe_writemem,0,0,		
/*TODO*///			ignore_interrupt,1							
/*TODO*///		}
/*TODO*///	
/*TODO*///	#define SOUND_CHIP_SQUEAK_DELUXE					
/*TODO*///		{												
/*TODO*///			SOUND_DAC,									
/*TODO*///			&mcr_dac_interface							
/*TODO*///		}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/************ Sounds Good CPU and sound definitions ***************/
/*TODO*///	
/*TODO*///	extern const struct Memory_ReadAddress16 soundsgood_readmem[];
/*TODO*///	extern const struct Memory_WriteAddress16 soundsgood_writemem[];
/*TODO*///	
/*TODO*///	extern struct DACinterface mcr_dual_dac_interface;
/*TODO*///	
/*TODO*///	#define SOUND_CPU_SOUNDS_GOOD						
/*TODO*///		{												
/*TODO*///			CPU_M68000 | CPU_AUDIO_CPU,					
/*TODO*///			16000000/2,	/* 8.0 MHz */					
/*TODO*///			soundsgood_readmem,soundsgood_writemem,0,0,	
/*TODO*///			ignore_interrupt,1							
/*TODO*///		}
/*TODO*///	
/*TODO*///	#define SOUND_SOUNDS_GOOD SOUND_CHIP_SQUEAK_DELUXE
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/************ Turbo Chip Squeak CPU and sound definitions ***************/
/*TODO*///	
/*TODO*///	extern const struct Memory_ReadAddress turbocs_readmem[];
/*TODO*///	extern const struct Memory_WriteAddress turbocs_writemem[];
/*TODO*///	
/*TODO*///	#define SOUND_CPU_TURBO_CHIP_SQUEAK					
/*TODO*///		{												
/*TODO*///			CPU_M6809 | CPU_AUDIO_CPU,					
/*TODO*///			9000000/4,	/* 2.25 MHz */					
/*TODO*///			turbocs_readmem,turbocs_writemem,0,0,		
/*TODO*///			ignore_interrupt,1							
/*TODO*///		}
/*TODO*///	
/*TODO*///	#define SOUND_TURBO_CHIP_SQUEAK SOUND_CHIP_SQUEAK_DELUXE
/*TODO*///	
/*TODO*///	#define SOUND_CPU_TURBO_CHIP_SQUEAK_PLUS_SOUNDS_GOOD 
/*TODO*///		SOUND_CPU_TURBO_CHIP_SQUEAK,					
/*TODO*///		SOUND_CPU_SOUNDS_GOOD
/*TODO*///	
/*TODO*///	#define SOUND_TURBO_CHIP_SQUEAK_PLUS_SOUNDS_GOOD	
/*TODO*///		{												
/*TODO*///			SOUND_DAC,									
/*TODO*///			&mcr_dual_dac_interface						
/*TODO*///		}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/************ Squawk & Talk CPU and sound definitions ***************/
/*TODO*///	
/*TODO*///	extern const struct Memory_ReadAddress squawkntalk_readmem[];
/*TODO*///	extern const struct Memory_WriteAddress squawkntalk_writemem[];
/*TODO*///	
/*TODO*///	extern struct TMS5220interface squawkntalk_tms5220_interface;
	
	public static MachineCPU SOUND_CPU_SQUAWK_N_TALK = new MachineCPU
        (												
                CPU_M6802 | CPU_AUDIO_CPU,					
                3580000/4,	/* .8 MHz */					
                squawkntalk_readmem,squawkntalk_writemem,0,0,
                ignore_interrupt,1							
        );
	
	/*TODO*///public static MachineSound SOUND_SQUAWK_N_TALK = new MachineSound
        /*TODO*///(												
	/*TODO*///		SOUND_TMS5220,								
	/*TODO*///		squawkntalk_tms5220_interface				
        /*TODO*///);    
}
