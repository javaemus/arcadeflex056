/* INTEL 8255 PPI I/O chip */


/* NOTE: When port is input, then data present on the ports
   outputs is 0xff */

/* KT 10/01/2000 - Added bit set/reset feature for control port */
/*               - Added more accurate port i/o data handling */
/*               - Added output reset when control mode is programmed */



/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.machine;

import static arcadeflex036.osdepend.logerror;
import static arcadeflex056.fucPtr.*;
import static mame056.cpuintrfH.activecpu_get_pc;

import static mame056.machine._8255ppiH.*;

public class _8255ppi
{
	
	
	static int num;
	
	public static class ppi8255
	{
		public ReadHandlerPtr portAread;
		public ReadHandlerPtr portBread;
		public ReadHandlerPtr portCread;
		
                public WriteHandlerPtr portAwrite;
		public WriteHandlerPtr portBwrite;
		public WriteHandlerPtr portCwrite;
		
                public int groupA_mode;
		public int groupB_mode;
                
		public int[] io = new int[3];		/* input output status */
		public int[] latch = new int[3];	/* data written to ports */
	};
	
	static ppi8255[] chips = new ppi8255[MAX_8255];
	
	/*TODO*///static void set_mode(int which, int data, int call_handlers);
	
	
	public static void ppi8255_init( ppi8255_interface intfce )
	{
		int i;
	
	
		num = intfce.num;
	
		for (i = 0; i < num; i++)
		{
                        chips[i] = new ppi8255();
			chips[i].portAread = intfce.portAread[i];
			chips[i].portBread = intfce.portBread[i];
			chips[i].portCread = intfce.portCread[i];
			chips[i].portAwrite = intfce.portAwrite[i];
			chips[i].portBwrite = intfce.portBwrite[i];
			chips[i].portCwrite = intfce.portCwrite[i];
	
			set_mode(i, 0x1b, 0);	/* Mode 0, all ports set to input */
		}
	}
	
	
	public static int ppi8255_r( int which, int offset )
	{
		ppi8255 chip;
	
	
		/* some bounds checking */
		if (which > num)
		{
			logerror("Attempting to access an unmapped 8255 chip.  PC: %04X\n", activecpu_get_pc());
			return 0xff;
		}
	
		chip = chips[which];
	
	
		if (offset > 3)
		{
			logerror("Attempting to access an invalid 8255 register.  PC: %04X\n", activecpu_get_pc());
			return 0xff;
		}
	
	
		switch(offset)
		{
		case 0: /* Port A read */
			if (chip.io[0] == 0)
				return chip.latch[0];	/* output */
			else
				if ((chip.portAread) != null)  return chip.portAread.handler(0);	/* input */
			break;
	
		case 1: /* Port B read */
			if (chip.io[1] == 0)
				return chip.latch[1];	/* output */
			else
				if ((chip.portBread) != null)  return (chip.portBread).handler(0);	/* input */
			break;
	
		case 2: /* Port C read */
			if (chip.io[2] == 0)
				return chip.latch[2];	/* output */
			else
				/* return data - combination of input and latched output depending on
				   the input/output status of each half of port C */
				if ((chip.portCread) != null)
                                    return ((chip.latch[2] & ~chip.io[2]) | (chip.portCread.handler(0) & chip.io[2]));
			break;
	
		case 3: /* Control word */
			return 0xff;
		}
	
		logerror("8255 chip %d: Port %c is being read but has no handler.  PC: %04X\n", which, 'A' + offset, activecpu_get_pc());
	
		return 0xff;
	}
	
	
	
	public static void PPI8255_PORT_A_WRITE(ppi8255 chip)
	{														
		int write_data;										
															
		write_data = (chip.latch[0] & ~chip.io[0]) |		
					 (0xff & chip.io[0]);					
															
		if (chip.portAwrite != null)								
			chip.portAwrite.handler(0, write_data);				
		/*TODO*///else												
		/*TODO*///	logerror("8255 chip %d: Port A is being written to but has no handler.  PC: %08X - %02X\n", which, cpu_get_pc(), write_data);
	}
	
	public static void PPI8255_PORT_B_WRITE(ppi8255 chip)							
	{														
		int write_data;										
															
		write_data = (chip.latch[1] & ~chip.io[1]) |		
					 (0xff & chip.io[1]);					
															
		if (chip.portBwrite != null)
			chip.portBwrite.handler(0, write_data);				
		/*TODO*///else												
		/*TODO*///	logerror("8255 chip %d: Port B is being written to but has no handler.  PC: %08X - %02X\n", which, cpu_get_pc(), write_data);
	}
	
	public static void PPI8255_PORT_C_WRITE(ppi8255 chip)
	{
		int write_data;

		write_data = (chip.latch[2] & ~chip.io[2]) |		
					 (0xff & chip.io[2]);

		if (chip.portCwrite != null)
			chip.portCwrite.handler(0, write_data);
		/*TODO*///else
		/*TODO*///	logerror("8255 chip %d: Port C is being written to but has no handler.  PC: %08X - %02X\n", which, cpu_get_pc(), write_data);
	}
	
	
	public static void ppi8255_w( int which, int offset, int data )
	{
		ppi8255	chip;
	
	
		/* Some bounds checking */
		if (which > num)
		{
			logerror("Attempting to access an unmapped 8255 chip.  PC: %04X\n", activecpu_get_pc());
			return;
		}
	
		chip = chips[which];
	
	
		if (offset > 3)
		{
			logerror("Attempting to access an invalid 8255 register.  PC: %04X\n", activecpu_get_pc());
			return;
		}
	
	
		switch( offset )
		{
		case 0: /* Port A write */
			chip.latch[0] = data;
			PPI8255_PORT_A_WRITE(chip);
			break;
	
		case 1: /* Port B write */
			chip.latch[1] = data;
			PPI8255_PORT_B_WRITE(chip);
			break;
	
		case 2: /* Port C write */
			chip.latch[2] = data;
			PPI8255_PORT_C_WRITE(chip);
			break;
	
		case 3: /* Control word */
			if (( data & 0x80 ) != 0)
			{
				set_mode(which, data & 0x7f, 1);
			}
			else
			{
				/* bit set/reset */
				int bit;
	
				bit = (data >> 1) & 0x07;
	
				if ((data & 1) != 0)
					chip.latch[2] |= (1<<bit);		/* set bit */
				else
					chip.latch[2] &= ~(1<<bit);	/* reset bit */
	
				if (chip.portCwrite != null)  PPI8255_PORT_C_WRITE(chip);
			}
		}
	}
	
	/*TODO*///#ifdef MESS
	public static int ppi8255_peek( int which, int offset )
	{
		ppi8255 chip;
	
	
		/* Some bounds checking */
		if (which > num)
		{
			logerror("Attempting to access an unmapped 8255 chip.  PC: %04X\n", activecpu_get_pc());
			return 0xff;
		}
	
		chip = chips[which];
	
	
		if (offset > 2)
		{
			logerror("Attempting to access an invalid 8255 port.  PC: %04X\n", activecpu_get_pc());
			return 0xff;
		}
	
	
		chip = chips[which];
	
		return chip.latch[offset];
	}
	/*TODO*///#endif
	
	
	public static void ppi8255_set_portAread( int which, ReadHandlerPtr portAread)
	{
		ppi8255 chip = chips[which];
	
		chip.portAread = portAread;
	}
	
	public static void ppi8255_set_portBread( int which, ReadHandlerPtr portBread)
	{
		ppi8255 chip = chips[which];
	
		chip.portBread = portBread;
	}
	
	public static void ppi8255_set_portCread( int which, ReadHandlerPtr portCread)
	{
		ppi8255 chip = chips[which];
	
		chip.portCread = portCread;
	}
	
	
	public static void ppi8255_set_portAwrite( int which, WriteHandlerPtr portAwrite)
	{
		ppi8255 chip = chips[which];
	
		chip.portAwrite = portAwrite;
	}
	
	public static void ppi8255_set_portBwrite( int which, WriteHandlerPtr portBwrite)
	{
		ppi8255 chip = chips[which];
	
		chip.portBwrite = portBwrite;
	}
	
	public static void ppi8255_set_portCwrite( int which, WriteHandlerPtr portCwrite)
	{
		ppi8255 chip = chips[which];
	
		chip.portCwrite = portCwrite;
	}
	
	
	static void set_mode(int which, int data, int call_handlers)
	{
		ppi8255 chip;
	
		chip = chips[which];
	
		chip.groupA_mode = ( data >> 5 ) & 3;
		chip.groupB_mode = ( data >> 2 ) & 1;
	
		if ((chip.groupA_mode != 0) || (chip.groupB_mode != 0))
		{
			logerror("8255 chip %d: Setting an unsupported mode %02X.  PC: %04X\n", which, data & 0x62, activecpu_get_pc());
			return;
		}
	
		/* Port A direction */
		if (( data & 0x10 ) != 0)
			chip.io[0] = 0xff;		/* input */
		else
			chip.io[0] = 0x00;		/* output */
	
		/* Port B direction */
		if (( data & 0x02 ) != 0)
			chip.io[1] = 0xff;
		else
			chip.io[1] = 0x00;
	
		/* Port C upper direction */
		if (( data & 0x08 ) != 0)
			chip.io[2] |= 0xf0;
		else
			chip.io[2] &= 0x0f;
	
		/* Port C lower direction */
		if (( data & 0x01 ) != 0)
			chip.io[2] |= 0x0f;
		else
			chip.io[2] &= 0xf0;
	
		/* KT: 25-Dec-99 - 8255 resets latches when mode set */
		chip.latch[0] = chip.latch[1] = chip.latch[2] = 0;
	
		if (call_handlers != 0)
		{
			if (chip.portAwrite != null)  PPI8255_PORT_A_WRITE(chip);
			if (chip.portBwrite != null)  PPI8255_PORT_B_WRITE(chip);
			if (chip.portCwrite != null)  PPI8255_PORT_C_WRITE(chip);
		}
	}
	
	
	/* Helpers */
	public static ReadHandlerPtr ppi8255_0_r  = new ReadHandlerPtr() { public int handler(int offset) { return ppi8255_r( 0, offset ); } };
	public static ReadHandlerPtr ppi8255_1_r  = new ReadHandlerPtr() { public int handler(int offset) { return ppi8255_r( 1, offset ); } };
	public static ReadHandlerPtr ppi8255_2_r  = new ReadHandlerPtr() { public int handler(int offset) { return ppi8255_r( 2, offset ); } };
	public static ReadHandlerPtr ppi8255_3_r  = new ReadHandlerPtr() { public int handler(int offset) { return ppi8255_r( 3, offset ); } };
	public static WriteHandlerPtr ppi8255_0_w = new WriteHandlerPtr() {public void handler(int offset, int data) { ppi8255_w( 0, offset, data ); } };
	public static WriteHandlerPtr ppi8255_1_w = new WriteHandlerPtr() {public void handler(int offset, int data) { ppi8255_w( 1, offset, data ); } };
	public static WriteHandlerPtr ppi8255_2_w = new WriteHandlerPtr() {public void handler(int offset, int data) { ppi8255_w( 2, offset, data ); } };
	public static WriteHandlerPtr ppi8255_3_w = new WriteHandlerPtr() {public void handler(int offset, int data) { ppi8255_w( 3, offset, data ); } };
}
