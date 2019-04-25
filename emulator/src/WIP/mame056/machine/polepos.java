/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static arcadeflex056.fucPtr.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.inptport.*;
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;

// refactor
import static arcadeflex036.osdepend.logerror;

import static WIP.mame056.sndhrdw.polepos.*;

public class polepos
{
	
	
	/*TODO*///#define VERBOSE 0
	
	/*TODO*///#if VERBOSE
	/*TODO*///#define LOG(x)	logerror x
	/*TODO*///#else
	/*TODO*///#define LOG(x)
	/*TODO*///#endif
	
	
	/* interrupt states */
	static int z80_irq_enabled = 0, z8002_1_nvi_enabled = 0, z8002_2_nvi_enabled = 0;
	
	/* ADC states */
	static int adc_input = 0;
	
	/* protection states */
	static int ic25_last_result;
	static int ic25_last_signed;
	static int ic25_last_unsigned;
	
	/* 4-bit MCU state */
	public static class polepos_mcu_def
	{
		public int 	enabled;			/* Enabled */
		public int 	status; 			/* Status */
		public int 	transfer_id;		/* Transfer id */
		public timer_entry	timer; 			/* Transfer timer */
		public int 	coin1_coinpercred;	/* Coinage info */
		public int 	coin1_credpercoin;	/* Coinage info */
		public int 	coin2_coinpercred;	/* Coinage info */
		public int 	coin2_credpercoin;	/* Coinage info */
		public int 	mode;				/* Mode ( 0 = read switches, 1 = Coinage, 2 = in-game ) */
		public int 	credits;			/* Credits count */
		public int 	start;				/* Start switch */
	};
        
        public static polepos_mcu_def polepos_mcu = new polepos_mcu_def();
	
	/*************************************************************************************/
	/* Interrupt handling																 */
	/*************************************************************************************/
	
	public static InitMachinePtr polepos_init_machine = new InitMachinePtr() { public void handler() 
	{
		/* reset all the interrupt states */
		z80_irq_enabled = z8002_1_nvi_enabled = z8002_2_nvi_enabled = 0;
	
		/* reset the ADC state */
		adc_input = 0;
	
		/* reset the protection state */
		ic25_last_result = 0;
		ic25_last_signed = 0;
		ic25_last_unsigned = 0;
	
		/* Initialize the MCU */
		polepos_mcu.enabled = 0; /* disabled */
		polepos_mcu.status = 0x10; /* ready to transfer */
		polepos_mcu.transfer_id = 0; /* clear out the transfer id */
		polepos_mcu.timer = null;
		polepos_mcu.start = 0;
	
		/* halt the two Z8002 cpus */
		cpu_set_reset_line(1, ASSERT_LINE);
		cpu_set_reset_line(2, ASSERT_LINE);
	
		/* start a timer for the Z80's interrupt */
		timer_set(cpu_getscanlinetime(0), 0, z80_interrupt);
	} };
	
	public static timer_callback z80_interrupt = new timer_callback() {
            public void handler(int scanline) {
                cpu_set_irq_line(0, 0, ((scanline & 64) == 0) ? ASSERT_LINE : CLEAR_LINE);
		scanline += 64;
		if (scanline >= 256) scanline = 0;
		timer_set(cpu_getscanlinetime(scanline), scanline, z80_interrupt);
            }
        };
	
	public static WriteHandlerPtr polepos_z80_irq_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		z80_irq_enabled = data & 1;
		if ((data & 1) == 0) cpu_set_irq_line(0, 0, CLEAR_LINE);
	} };
	
	public static WriteHandlerPtr polepos_z8002_nvi_enable_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                int which = offset + 1;
	
		if (which == cpu_getactivecpu())
		{
			if (which == 1)
				z8002_1_nvi_enabled = data & 1;
			else
				z8002_2_nvi_enabled = data & 1;
			if ((data & 1) == 0) cpu_set_irq_line(which, 0, CLEAR_LINE);
		}
		//LOG(("Z8K#%d cpu%d_nvi_enable_w $%02x\n", cpu_getactivecpu(), which, data));
            }
        };
	
	public static InterruptPtr polepos_z8002_1_interrupt = new InterruptPtr() { public int handler() 
	{
		if (z8002_1_nvi_enabled != 0)
			cpu_set_irq_line(1, 0, ASSERT_LINE);
	
		return ignore_interrupt.handler();
	} };
	
	public static InterruptPtr polepos_z8002_2_interrupt = new InterruptPtr() { public int handler() 
	{
		if (z8002_2_nvi_enabled != 0)
			cpu_set_irq_line(2, 0, ASSERT_LINE);
	
		return ignore_interrupt.handler();
	} };
	
	public static WriteHandlerPtr polepos_z8002_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if ((data & 1) != 0)
			cpu_set_reset_line(offset + 1, CLEAR_LINE);
		else
			cpu_set_reset_line(offset + 1, ASSERT_LINE);
	} };
	
	
	/*************************************************************************************/
	/* I/O and ADC handling 															 */
	/*************************************************************************************/
	
	public static WriteHandlerPtr polepos_adc_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		adc_input = data;
	} };
	
	public static ReadHandlerPtr polepos_adc_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int ret = 0;
	
		switch (adc_input)
		{
			case 0x00:
				ret = readinputport(3);
				break;
	
			case 0x01:
				ret = readinputport(4);
				break;
	
			default:
				//LOG(("Unknown ADC Input select (%02x)!\n", adc_input));
				break;
		}
	
		return ret;
	} };
	
	public static ReadHandlerPtr polepos_io_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int ret = 0xff;
	
		if (cpu_getscanline() >= 128)
			ret ^= 0x02;
	
		ret ^= 0x08; /* ADC End Flag */
	
		return ret;
	} };
	
	
	/*************************************************************************************/
	/* Pole Position II protection														 */
	/*************************************************************************************/
	
	public static ReadHandlerPtr polepos2_ic25_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                int result;
	
		offset = offset & 0x1ff;
		if (offset < 0x100)
		{
			ic25_last_signed = offset & 0xff;
			result = ic25_last_result & 0xff;
		}
		else
		{
			ic25_last_unsigned = offset & 0xff;
			result = (ic25_last_result >> 8) & 0xff;
			ic25_last_result = ic25_last_signed * ic25_last_unsigned;
		}
	
		logerror("%04X: read IC25 @ %04X = %02X\n", cpu_get_pc(), offset, result);
	
		return result | (result << 8);
            }
        };
	
	/*************************************************************************************/
	/* 4 bit cpu emulation																 */
	/*************************************************************************************/
	
	public static WriteHandlerPtr polepos_mcu_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		polepos_mcu.enabled = data & 1;
	
		if (polepos_mcu.enabled == 0)
		{
			/* If its getting disabled, kill our timer */
			if (polepos_mcu.timer != null)
			{
				timer_remove(polepos_mcu.timer);
				polepos_mcu.timer = null;
			}
		}
	} };
	
	public static timer_callback polepos_mcu_callback = new timer_callback() {
            public void handler(int param) {
                cpu_set_nmi_line(0, PULSE_LINE);
            }
        };
	
	public static ReadHandlerPtr polepos_mcu_control_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (polepos_mcu.enabled != 0)
			return polepos_mcu.status;
	
		return 0x00;
	} };
	
	public static WriteHandlerPtr polepos_mcu_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		//LOG(("polepos_mcu_control_w: %d, $%02x\n", offset, data));
	
		if (polepos_mcu.enabled != 0)
		{
			if (data != 0x10)
			{
				/* start transfer */
				polepos_mcu.transfer_id = data; /* get the id */
				polepos_mcu.status = 0xe0;		/* set status */
				if (polepos_mcu.timer != null)
					timer_remove(polepos_mcu.timer);
				/* fire off the transfer timer */
				polepos_mcu.timer = timer_pulse(TIME_IN_USEC(50), 0, polepos_mcu_callback);
			}
			else
			{
				/* end transfer */
				if (polepos_mcu.timer != null) /* shut down our transfer timer */
					timer_remove(polepos_mcu.timer);
				polepos_mcu.timer = null;
				polepos_mcu.status = 0x10; /* set status */
			}
		}
	} };
	
	public static ReadHandlerPtr polepos_mcu_data_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (polepos_mcu.enabled != 0)
		{
			//LOG(("MCU read: PC = %04x, transfer mode = %02x, offset = %02x\n", cpu_get_pc(), polepos_mcu.transfer_id & 0xff, offset ));
	
			switch(polepos_mcu.transfer_id)
			{
				case 0x71: /* 3 bytes */
					switch (offset)
					{
						case 0x00:
							if ( polepos_mcu.mode == 0 )
								return ~( readinputport(0) ^ polepos_mcu.start ); /* Service, Gear, etc */
							else {
								int last_in = 0;
								int in;
								int coin1inserted=0;
								int coin2inserted=0;
	
								in = readinputport(0);
	
								/* check if the user inserted a coin */
								if (polepos_mcu.coin1_coinpercred > 0)
								{
									if (( ( last_in ^ in ) & 0x10 ) != 0) {
										if ((in & 0x10) == 0 && polepos_mcu.credits < 99)
										{
											coin1inserted++;
											if (coin1inserted >= polepos_mcu.coin1_coinpercred)
											{
												polepos_mcu.credits += polepos_mcu.coin1_credpercoin;
												coin1inserted = 0;
											}
										}
									}
	
									if (( ( last_in ^ in ) & 0x20 ) != 0) {
										if ((in & 0x20) == 0 && polepos_mcu.credits < 99)
										{
											coin2inserted++;
											if (coin2inserted >= polepos_mcu.coin2_coinpercred)
											{
												polepos_mcu.credits += polepos_mcu.coin2_credpercoin;
												coin2inserted = 0;
											}
										}
									}
								}
								else polepos_mcu.credits = 100; /* freeplay */
	
								last_in = in;
	
								return (polepos_mcu.credits / 10) * 16 + polepos_mcu.credits % 10;
							}
						case 0x01:
							return ~readinputport(2); /* DSW1 */
						case 0x02:
							return ~( ( readinputport(0) & 2 ) << 4 );
					}
					break;
	
				case 0x72: /* 8 bytes */
					switch (offset)
					{
						case 0x00: /* Steering */
							return readinputport(5);
	
						case 0x04:
							return ~readinputport(1); /* DSW 0 */
					}
					break;
	
				case 0x91: /* 3 bytes */
					/*
					   Same as 0x71 but sets coinage mode? - Program seems only to check offset 0 and make sure
					   it's 0 or 0xa0 (freeplay?), otherwise, it resets itself.
					*/
					case 0x00:
						polepos_mcu.mode = 1;
						if ( polepos_mcu.coin1_coinpercred > 0 )
							polepos_mcu.credits = 0;
						else
							polepos_mcu.credits = 100; /* freeplay */
						return (polepos_mcu.credits / 10) * 16 + polepos_mcu.credits % 10;
					//break;
	
				default:
					logerror("Unknwon MCU transfer mode: %02x\n", polepos_mcu.transfer_id);
					break;
			}
		}
	
		return 0xff; /* pull up */
	} };
	
	public static WriteHandlerPtr polepos_mcu_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (polepos_mcu.enabled != 0)
		{
			//LOG(("MCU write: PC = %04x, transfer mode = %02x, offset = %02x, data = %02x\n", cpu_get_pc(), polepos_mcu.transfer_id & 0xff, offset, data ));
	
			if ( polepos_mcu.transfer_id == 0xa1 ) { /* setup coins/credits, etc ( 8 bytes ) */
				switch( offset ) {
					case 1:
						polepos_mcu.coin1_coinpercred = data;
						break;
	
					case 2:
						polepos_mcu.coin1_credpercoin = data;
						break;
	
					case 3:
						polepos_mcu.coin2_coinpercred = data;
						break;
	
					case 4:
						polepos_mcu.coin2_credpercoin = data;
						break;
	
					/* NOTE: I still have no clue what offset 0, 5, 6 & 7 do */
				}
			}
	
			if ( polepos_mcu.transfer_id == 0xc1 ) { /* set switch mode */
				polepos_mcu.mode = 0;
			}
	
			if ( polepos_mcu.transfer_id == 0x84 ) { /* play sample */
				if ( offset == 0 ) {
					switch( data ) {
						case 0x01:
							polepos_sample_play( 0 );
						break;
	
						case 0x02:
							polepos_sample_play( 1 );
						break;
	
						case 0x04:
							polepos_sample_play( 2 );
						break;
	
						default:
							logerror("Unknown sample triggered (%d)\n", data );
						break;
					}
				}
			}
	
			if ( polepos_mcu.transfer_id == 0x88 ) { /* play screech/explosion */
				if ( offset == 0 ) {
					/* 0x40 = Start Explosion sample */
					/* 0x20 = ???? */
					/* 0x7n = Screech sound. n = pitch (if 0 then no sound) */
					if ( data == 0x40 )
						sample_start( 0, 0, 0 );
	
					if ( ( data & 0xf0 ) == 0x70 ) {
						if ( ( data & 0x0f ) == 0 ) {
							if ( sample_playing(1) != 0)
								sample_stop(1);
						} else {
							int freq = (int)( ( 44100.0f / 10.0f ) * (float)(data & 0x0f) );
	
							if ( sample_playing(1) == 0)
								sample_start( 1, 1, 1 );
							sample_set_freq(1, freq);
						}
					}
				}
			}
	
			if ( polepos_mcu.transfer_id == 0x81 ) { /* Set coinage mode */
				polepos_mcu.mode = 1;
			}
		}
	} };
	
	public static WriteHandlerPtr polepos_start_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int last_start = 0;
	
		data &= 1;
	
		polepos_mcu.start = data << 2;
	
		/* check for start button if not in-game */
		if ( polepos_mcu.mode != 2 ) {
			if ( (( last_start ^ data )!=0) && ( data == 0 ) ) {
				if (polepos_mcu.credits >= 1) {
					polepos_mcu.mode = 2; /* in-game */
					polepos_mcu.credits--;
				}
			}
		}
	
		last_start = data;
	} };
	
}
