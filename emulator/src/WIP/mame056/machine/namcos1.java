/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.mame.*;
import static mame056.memoryH.*;
import static mame056.cpuintrfH.*;

// refactor
import static arcadeflex036.osdepend.logerror;

import static mame056.sound.namco.*;
public class namcos1
{
	
	public static int NEW_TIMER = 0; /* CPU slice optimize with new timer system */
	
	public static int NAMCOS1_MAX_BANK = 0x400;
	
	/* from vidhrdw */

        public static int NAMCOS1_MAX_KEY = 0x100;
        public static int[] key = new int[NAMCOS1_MAX_KEY];

        static UBytePtr s1ram = new UBytePtr();

        static int namcos1_cpu1_banklatch;
        public static int namcos1_reset = 0;

        static int berabohm_input_counter;
	
	/*******************************************************************************
	*																			   *
	*	BANK area handling															*
	*																			   *
	*******************************************************************************/
	
	/* Bank handler definitions */
/*TODO*///	typedef struct {
/*TODO*///		mem_read_handler bank_handler_r;
/*TODO*///		mem_write_handler bank_handler_w;
/*TODO*///		int 		  bank_offset;
/*TODO*///		unsigned char *bank_pointer;
/*TODO*///	} bankhandler;
/*TODO*///	
/*TODO*///	/* hardware elements of 1Mbytes physical memory space */
/*TODO*///	static bankhandler namcos1_bank_element[NAMCOS1_MAX_BANK];
	
	static int org_bank_handler_r[] =
	{
		MRA_BANK1 ,MRA_BANK2 ,MRA_BANK3 ,MRA_BANK4 ,
		MRA_BANK5 ,MRA_BANK6 ,MRA_BANK7 ,MRA_BANK8 ,
		MRA_BANK9 ,MRA_BANK10,MRA_BANK11,MRA_BANK12,
		MRA_BANK13,MRA_BANK14,MRA_BANK15,MRA_BANK16
	};
	
	static int org_bank_handler_w[] =
	{
		MWA_BANK1 ,MWA_BANK2 ,MWA_BANK3 ,MWA_BANK4 ,
		MWA_BANK5 ,MWA_BANK6 ,MWA_BANK7 ,MWA_BANK8 ,
		MWA_BANK9 ,MWA_BANK10,MWA_BANK11,MWA_BANK12,
		MWA_BANK13,MWA_BANK14,MWA_BANK15,MWA_BANK16
	};
	
	
	
	/*******************************************************************************
	*																			   *
	*	Key emulation (CUS136) Rev1 (Pacmania & Galaga 88)						   *
	*																			   *
	*******************************************************************************/
	
	static int key_id;
	static int key_id_query;
	
	public static ReadHandlerPtr rev1_key_r  = new ReadHandlerPtr() { public int handler(int offset) {
	//	logerror("CPU #%d PC %08x: keychip read %04X=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,key[offset]);
		if(offset >= NAMCOS1_MAX_KEY)
		{
			logerror("CPU #%d PC %08x: unmapped keychip read %04x\n",cpu_getactivecpu(),cpu_get_pc(),offset);
			return 0;
		}
		return key[offset];
	} };
	
	public static WriteHandlerPtr rev1_key_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		int divider=0, divide_32 = 0;
		//logerror("CPU #%d PC %08x: keychip write %04X=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,data);
		if(offset >= NAMCOS1_MAX_KEY)
		{
			logerror("CPU #%d PC %08x: unmapped keychip write %04x=%04x\n",cpu_getactivecpu(),cpu_get_pc(),offset,data);
			return;
		}
	
		key[offset] = data;
	
		switch ( offset )
		{
		case 0x01:
			divider = ( key[0] << 8 ) + key[1];
			break;
		case 0x03:
			{
				int d=0;
				int	v1, v2;
				int	l=0;
	
				if ( divide_32 != 0 )
					l = d << 16;
	
				d = ( key[2] << 8 ) + key[3];
	
				if ( divider == 0 ) {
					v1 = 0xffff;
					v2 = 0;
				} else {
					if ( divide_32 != 0 ) {
						l |= d;
	
						v1 = l / divider;
						v2 = l % divider;
					} else {
						v1 = d / divider;
						v2 = d % divider;
					}
				}
	
				key[2] = v1 >> 8;
				key[3] = v1;
				key[0] = v2 >> 8;
				key[1] = v2;
			}
			break;
		case 0x04:
			if ( key[4] == key_id_query ) /* get key number */
				key[4] = key_id;
	
			if ( key[4] == 0x0c )
				divide_32 = 1;
			else
				divide_32 = 0;
			break;
		}
	} };
	
	/*******************************************************************************
	*																			   *
	*	Key emulation (CUS136) Rev2 (Dragon Spirit, Blazer, World Court)		   *
	*																			   *
	*******************************************************************************/
	
	public static ReadHandlerPtr rev2_key_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		//logerror("CPU #%d PC %08x: keychip read %04X=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,key[offset]);
		if(offset >= NAMCOS1_MAX_KEY)
		{
			logerror("CPU #%d PC %08x: unmapped keychip read %04x\n",cpu_getactivecpu(),cpu_get_pc(),offset);
			return 0;
		}
		return key[offset];
	} };
	
	public static WriteHandlerPtr rev2_key_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		//logerror("CPU #%d PC %08x: keychip write %04X=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,data);
		if(offset >= NAMCOS1_MAX_KEY)
		{
			logerror("CPU #%d PC %08x: unmapped keychip write %04x=%04x\n",cpu_getactivecpu(),cpu_get_pc(),offset,data);
			return;
		}
		key[offset] = data;
	
		switch(offset)
		{
		case 0x00:
			if ( data == 1 )
			{
				/* fetch key ID */
				key[3] = key_id;
				return;
			}
			break;
		case 0x02:
			/* $f2 = Dragon Spirit, $b7 = Blazer , $35($d9) = worldcourt */
			if ( key[3] == 0xf2 || key[3] == 0xb7 || key[3] == 0x35 )
			{
				switch( key[0] )
				{
					case 0x10: key[0] = 0x05; key[1] = 0x00; key[2] = 0xc6; break;
					case 0x12: key[0] = 0x09; key[1] = 0x00; key[2] = 0x96; break;
					case 0x15: key[0] = 0x0a; key[1] = 0x00; key[2] = 0x8f; break;
					case 0x22: key[0] = 0x14; key[1] = 0x00; key[2] = 0x39; break;
					case 0x32: key[0] = 0x31; key[1] = 0x00; key[2] = 0x12; break;
					case 0x3d: key[0] = 0x35; key[1] = 0x00; key[2] = 0x27; break;
					case 0x54: key[0] = 0x10; key[1] = 0x00; key[2] = 0x03; break;
					case 0x58: key[0] = 0x49; key[1] = 0x00; key[2] = 0x23; break;
					case 0x7b: key[0] = 0x48; key[1] = 0x00; key[2] = 0xd4; break;
					case 0xc7: key[0] = 0xbf; key[1] = 0x00; key[2] = 0xe8; break;
				}
				return;
			}
			break;
		case 0x03:
			/* $c2 = Dragon Spirit, $b6 = Blazer */
			if ( key[3] == 0xc2 || key[3] == 0xb6 ) {
				key[3] = 0x36;
				return;
			}
			/* $d9 = World court */
			if ( key[3] == 0xd9 )
			{
				key[3] = 0x35;
				return;
			}
			break;
		case 0x3f:	/* Splatter House */
			key[0x3f] = 0xb5;
			key[0x36] = 0xb5;
			return;
		}
		/* ?? */
		if ( key[3] == 0x01 ) {
			if ( key[0] == 0x40 && key[1] == 0x04 && key[2] == 0x00 ) {
				key[1] = 0x00; key[2] = 0x10;
				return;
			}
		}
	} };
	
	/*******************************************************************************
	*																			   *
	*	Key emulation (CUS136) for Dangerous Seed								   *
	*																			   *
	*******************************************************************************/
	
	public static ReadHandlerPtr dangseed_key_r  = new ReadHandlerPtr() { public int handler(int offset) {
	//	logerror("CPU #%d PC %08x: keychip read %04X=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,key[offset]);
		if(offset >= NAMCOS1_MAX_KEY)
		{
			logerror("CPU #%d PC %08x: unmapped keychip read %04x\n",cpu_getactivecpu(),cpu_get_pc(),offset);
			return 0;
		}
		return key[offset];
	} };
	
	public static WriteHandlerPtr dangseed_key_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		int i;
	//	logerror("CPU #%d PC %08x: keychip write %04X=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,data);
		if(offset >= NAMCOS1_MAX_KEY)
		{
			logerror("CPU #%d PC %08x: unmapped keychip write %04x=%04x\n",cpu_getactivecpu(),cpu_get_pc(),offset,data);
			return;
		}
	
		key[offset] = data;
	
		switch ( offset )
		{
			case 0x50:
				for ( i = 0; i < 0x50; i++ ) {
					key[i] = ( data >> ( ( i >> 4 ) & 0x0f ) ) & 0x0f;
					key[i] |= ( i & 0x0f ) << 4;
				}
				break;
	
			case 0x57:
				key[3] = key_id;
				break;
		}
	} };
	
	/*******************************************************************************
	*																			   *
	*	Key emulation (CUS136) for Dragon Spirit								   *
	*																			   *
	*******************************************************************************/
	
	public static ReadHandlerPtr dspirit_key_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		//logerror("CPU #%d PC %08x: keychip read %04X=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,key[offset]);
		if(offset >= NAMCOS1_MAX_KEY)
		{
			logerror("CPU #%d PC %08x: unmapped keychip read %04x\n",cpu_getactivecpu(),cpu_get_pc(),offset);
			return 0;
		}
		return key[offset];
	} };
	
	public static WriteHandlerPtr dspirit_key_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int divisor=0;
	//	logerror("CPU #%d PC %08x: keychip write %04X=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,data);
		if(offset >= NAMCOS1_MAX_KEY)
		{
			logerror("CPU #%d PC %08x: unmapped keychip write %04x=%04x\n",cpu_getactivecpu(),cpu_get_pc(),offset,data);
			return;
		}
		key[offset] = data;
	
		switch(offset)
		{
		case 0x00:
			if ( data == 1 )
			{
				/* fetch key ID */
				key[3] = key_id;
			} else
				divisor = data;
			break;
	
		case 0x01:
			if ( key[3] == 0x01 ) { /* division gets resolved on latch to $1 */
				int d, v1, v2;
	
				d = ( key[1] << 8 ) + key[2];
	
				if ( divisor == 0 ) {
					v1 = 0xffff;
					v2 = 0;
				} else {
					v1 = d / divisor;
					v2 = d % divisor;
				}
	
				key[0] = v2 & 0xff;
				key[1] = v1 >> 8;
				key[2] = v1 & 0xff;
	
				return;
			}
	
			if ( key[3] != 0xf2 ) { /* if its an invalid mode, clear regs */
				key[0] = 0;
				key[1] = 0;
				key[2] = 0;
			}
			break;
		case 0x02:
			if ( key[3] == 0xf2 ) { /* division gets resolved on latch to $2 */
				int d, v1, v2;
	
				d = ( key[1] << 8 ) + key[2];
	
				if ( divisor == 0 ) {
					v1 = 0xffff;
					v2 = 0;
				} else {
					v1 = d / divisor;
					v2 = d % divisor;
				}
	
				key[0] = v2 & 0xff;
				key[1] = v1 >> 8;
				key[2] = v1 & 0xff;
	
				return;
			}
	
			if ( key[3] != 0x01 ) { /* if its an invalid mode, clear regs */
				key[0] = 0;
				key[1] = 0;
				key[2] = 0;
			}
			break;
		case 0x03:
			if ( key[3] != 0xf2 && key[3] != 0x01 ) /* if the mode is unknown return the id on $3 */
				key[3] = key_id;
			break;
		}
	} };
	
	/*******************************************************************************
	*																			   *
	*	Key emulation (CUS136) for Blazer										   *
	*																			   *
	*******************************************************************************/
	
	public static ReadHandlerPtr blazer_key_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		logerror("CPU #%d PC %08x: keychip read %04X=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,key[offset]);
		if(offset >= NAMCOS1_MAX_KEY)
		{
			logerror("CPU #%d PC %08x: unmapped keychip read %04x\n",cpu_getactivecpu(),cpu_get_pc(),offset);
			return 0;
		}
		return key[offset];
	} };
	
	public static WriteHandlerPtr blazer_key_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int divisor=0;
		logerror("CPU #%d PC %08x: keychip write %04X=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,data);
		if(offset >= NAMCOS1_MAX_KEY)
		{
			logerror("CPU #%d PC %08x: unmapped keychip write %04x=%04x\n",cpu_getactivecpu(),cpu_get_pc(),offset,data);
			return;
		}
		key[offset] = data;
	
		switch(offset)
		{
		case 0x00:
			if ( data == 1 )
			{
				/* fetch key ID */
				key[3] = key_id;
			} else
				divisor = data;
			break;
	
		case 0x01:
			if ( key[3] != 0xb7 ) { /* if its an invalid mode, clear regs */
				key[0] = 0;
				key[1] = 0;
				key[2] = 0;
			}
			break;
	
		case 0x02:
			if ( key[3] == 0xb7 ) { /* division gets resolved on latch to $2 */
				int d, v1, v2;
	
				d = ( key[1] << 8 ) + key[2];
	
				if ( divisor == 0 ) {
					v1 = 0xffff;
					v2 = 0;
				} else {
					v1 = d / divisor;
					v2 = d % divisor;
				}
	
				key[0] = v2 & 0xff;
				key[1] = v1 >> 8;
				key[2] = v1 & 0xff;
	
				return;
			}
	
			/* if its an invalid mode, clear regs */
			key[0] = 0;
			key[1] = 0;
			key[2] = 0;
			break;
		case 0x03:
			if ( key[3] != 0xb7 ) { /* if the mode is unknown return the id on $3 */
				key[3] = key_id;
			}
			break;
		}
	} };
	
	/*******************************************************************************
	*																			   *
	*	Key emulation (CUS136) for World Stadium								   *
	*																			   *
	*******************************************************************************/
	
	public static ReadHandlerPtr ws_key_r  = new ReadHandlerPtr() { public int handler(int offset) {
	//	logerror("CPU #%d PC %08x: keychip read %04X=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,key[offset]);
		if(offset >= NAMCOS1_MAX_KEY)
		{
			logerror("CPU #%d PC %08x: unmapped keychip read %04x\n",cpu_getactivecpu(),cpu_get_pc(),offset);
			return 0;
		}
		return key[offset];
	} };
	
	public static WriteHandlerPtr ws_key_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		int divider=0;
		//logerror("CPU #%d PC %08x: keychip write %04X=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,data);
		if(offset >= NAMCOS1_MAX_KEY)
		{
			logerror("CPU #%d PC %08x: unmapped keychip write %04x=%04x\n",cpu_getactivecpu(),cpu_get_pc(),offset,data);
			return;
		}
	
		key[offset] = data;
	
		switch ( offset )
		{
		case 0x01:
			divider = ( key[0] << 8 ) + key[1];
			break;
		case 0x03:
			{
				int d;
				int v1, v2;
	
				d = ( key[2] << 8 ) + key[3];
	
				if ( divider == 0 ) {
					v1 = 0xffff;
					v2 = 0;
				} else {
					v1 = d / divider;
					v2 = d % divider;
				}
	
				key[2] = v1 >> 8;
				key[3] = v1;
				key[0] = v2 >> 8;
				key[1] = v2;
			}
			break;
		case 0x04:
			key[4] = key_id;
			break;
		}
	} };
	
	/*******************************************************************************
	*																			   *
	*	Key emulation (CUS181) for SplatterHouse								   *
	*																			   *
	*******************************************************************************/
	
	public static ReadHandlerPtr splatter_key_r  = new ReadHandlerPtr() { public int handler(int offset) {
	//	logerror("CPU #%d PC %08x: keychip read %04X=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,key[offset]);
		switch( ( offset >> 4 ) & 0x07 ) {
			case 0x00:
			case 0x06:
				return 0xff;
				//break;
	
			case 0x01:
			case 0x02:
			case 0x05:
			case 0x07:
				return ( ( offset & 0x0f ) << 4 ) | 0x0f;
				//break;
	
			case 0x03:
				return 0xb5;
				//break;
	
			case 0x04:
				{
					int data = 0x29;
	
					if ( offset >= 0x1000 )
						data |= 0x80;
					if ( offset >= 0x2000 )
						data |= 0x04;
	
					return data;
				}
				//break;
		}
	
		/* make compiler happy */
		return 0;
	} };
	
	public static WriteHandlerPtr splatter_key_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
	//	logerror("CPU #%d PC %08x: keychip write %04X=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,data);
		/* ignored */
	} };
	
	
	/*******************************************************************************
	*																			   *
	*	Banking emulation (CUS117)												   *
	*																			   *
	*******************************************************************************/
	
	public static ReadHandlerPtr soundram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if(offset<0x100)
			return namcos1_wavedata_r.handler(offset);
		if(offset<0x140)
			return namcos1_sound_r.handler(offset-0x100);
	
		/* shared ram */
		return namco_wavedata.read(offset);
	} };
	
	public static WriteHandlerPtr soundram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if(offset<0x100)
		{
			namcos1_wavedata_w.handler(offset,data);
			return;
		}
		if(offset<0x140)
		{
			namcos1_sound_w.handler(offset-0x100,data);
			return;
		}
		/* shared ram */
		namco_wavedata.write(offset, data);
	
		//if(offset>=0x1000)
		//	logerror("CPU #%d PC %04x: write shared ram %04x=%02x\n",cpu_getactivecpu(),cpu_get_pc(),offset,data);
	} };
	
	/* ROM handlers */
	
	public static WriteHandlerPtr rom_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		logerror("CPU #%d PC %04x: warning - write %02x to rom address %04x\n",cpu_getactivecpu(),cpu_get_pc(),data,offset);
	} };
	
	/* error handlers */
	public static ReadHandlerPtr unknown_r  = new ReadHandlerPtr() { public int handler(int offset) {
		logerror("CPU #%d PC %04x: warning - read from unknown chip\n",cpu_getactivecpu(),cpu_get_pc() );
		return 0;
	} };
	
	public static WriteHandlerPtr unknown_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		logerror("CPU #%d PC %04x: warning - wrote to unknown chip\n",cpu_getactivecpu(),cpu_get_pc() );
	} };
	
/*TODO*///	/* Main bankswitching routine */
/*TODO*///	void namcos1_bankswitch(int cpu, offs_t offset, data8_t data)
/*TODO*///	{
/*TODO*///		static int chip = 0;
/*TODO*///		mem_read_handler handler_r;
/*TODO*///		mem_write_handler handler_w;
/*TODO*///		offs_t offs;
/*TODO*///	
/*TODO*///		if ( offset & 1 ) {
/*TODO*///			int bank = (cpu*8) + ( ( offset >> 9 ) & 0x07 );
/*TODO*///			chip &= 0x0300;
/*TODO*///			chip |= ( data & 0xff );
/*TODO*///	
/*TODO*///			/* for BANK handlers , memory direct and OP-code base */
/*TODO*///			cpu_setbank(bank+1,namcos1_bank_element[chip].bank_pointer);
/*TODO*///	
/*TODO*///			/* Addition OFFSET for stub handlers */
/*TODO*///			offs = namcos1_bank_element[chip].bank_offset;
/*TODO*///	
/*TODO*///			/* read hardware */
/*TODO*///			handler_r = namcos1_bank_element[chip].bank_handler_r;
/*TODO*///			if( handler_r )
/*TODO*///				/* I/O handler */
/*TODO*///				memory_set_bankhandler_r( bank+1,offs,handler_r);
/*TODO*///			else	/* memory direct */
/*TODO*///				memory_set_bankhandler_r( bank+1,0,org_bank_handler_r[bank] );
/*TODO*///	
/*TODO*///			/* write hardware */
/*TODO*///			handler_w = namcos1_bank_element[chip].bank_handler_w;
/*TODO*///			if( handler_w )
/*TODO*///				/* I/O handler */
/*TODO*///				memory_set_bankhandler_w( bank+1,offs,handler_w);
/*TODO*///			else	/* memory direct */
/*TODO*///				memory_set_bankhandler_w( bank+1,0,org_bank_handler_w[bank] );
/*TODO*///	
/*TODO*///			/* unmapped bank warning */
/*TODO*///			if( handler_r == unknown_r)
/*TODO*///			{
/*TODO*///				logerror("CPU #%d PC %04x:warning unknown chip selected bank %x=$%04x\n", cpu , cpu_get_pc(), bank , chip );
/*TODO*///			}
/*TODO*///	
/*TODO*///			/* renew pc base */
/*TODO*///	//		change_pc16(cpu_get_pc());
/*TODO*///		} else {
/*TODO*///			chip &= 0x00ff;
/*TODO*///			chip |= ( data & 0xff ) << 8;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
	public static WriteHandlerPtr namcos1_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
/*TODO*///		namcos1_bankswitch(cpu_getactivecpu(), offset, data);
	} };

	/* Sub cpu set start bank port */
	public static WriteHandlerPtr namcos1_subcpu_bank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
/*TODO*///		//logerror("cpu1 bank selected %02x=%02x\n",offset,data);
/*TODO*///		namcos1_cpu1_banklatch = (namcos1_cpu1_banklatch&0x300)|data;
/*TODO*///		/* Prepare code for Cpu 1 */
/*TODO*///		namcos1_bankswitch( 1, 0x0e00, namcos1_cpu1_banklatch>>8  );
/*TODO*///		namcos1_bankswitch( 1, 0x0e01, namcos1_cpu1_banklatch&0xff);
/*TODO*///		/* cpu_set_reset_line(1,PULSE_LINE); */
	
	} };
	
	/*******************************************************************************
	*																			   *
	*	63701 MCU emulation (CUS64) 											   *
	*																			   *
	*******************************************************************************/
	
	static int mcu_patch_data;
	
	public static WriteHandlerPtr namcos1_cpu_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	//	logerror("reset control pc=%04x %02x\n",cpu_get_pc(),data);
		if(( (data&1)^namcos1_reset) != 0)
		{
			namcos1_reset = data&1;
			if (namcos1_reset != 0)
			{
				cpu_set_reset_line(1,CLEAR_LINE);
				cpu_set_reset_line(2,CLEAR_LINE);
				cpu_set_reset_line(3,CLEAR_LINE);
				mcu_patch_data = 0;
			}
			else
			{
				cpu_set_reset_line(1,ASSERT_LINE);
				cpu_set_reset_line(2,ASSERT_LINE);
				cpu_set_reset_line(3,ASSERT_LINE);
			}
		}
	} };
	
	/*******************************************************************************
	*																			   *
	*	Sound banking emulation (CUS121)										   *
	*																			   *
	*******************************************************************************/
	
	public static WriteHandlerPtr namcos1_sound_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU3));
		int bank = ( data >> 4 ) & 0x07;
	
		cpu_setbank( 17, new UBytePtr(RAM, 0x0c000 + ( 0x4000 * bank ) ) );
	} };
	
	/*******************************************************************************
	*																			   *
	*	CPU idling spinlock routine 											   *
	*																			   *
	*******************************************************************************/
	static UBytePtr sound_spinlock_ram = new UBytePtr();
	static int sound_spinlock_pc;
	
	/* sound cpu */
	public static ReadHandlerPtr namcos1_sound_spinlock_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if(cpu_get_pc()==sound_spinlock_pc && sound_spinlock_ram.read() == 0)
			cpu_spinuntil_int();
		return sound_spinlock_ram.read();
	} };
	
	/*******************************************************************************
	*																			   *
	*	MCU banking emulation and patch 										   *
	*																			   *
	*******************************************************************************/
	
	/* mcu banked rom area select */
	public static WriteHandlerPtr namcos1_mcu_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
/*TODO*///		int addr;
/*TODO*///		/* bit 2-7 : chip select line of ROM chip */
/*TODO*///		switch(data&0xfc)
/*TODO*///		{
/*TODO*///		case 0xf8: addr = 0x10000; break; /* bit 2 : ROM 0 */
/*TODO*///		case 0xf4: addr = 0x30000; break; /* bit 3 : ROM 1 */
/*TODO*///		case 0xec: addr = 0x50000; break; /* bit 4 : ROM 2 */
/*TODO*///		case 0xdc: addr = 0x70000; break; /* bit 5 : ROM 3 */
/*TODO*///		case 0xbc: addr = 0x90000; break; /* bit 6 : ROM 4 */
/*TODO*///		case 0x7c: addr = 0xb0000; break; /* bit 7 : ROM 5 */
/*TODO*///		default:   addr = 0x100000; /* illegal */
/*TODO*///		}
/*TODO*///		/* bit 0-1 : address line A15-A16 */
/*TODO*///		addr += (data&3)*0x8000;
/*TODO*///		if( addr >= memory_region_length(REGION_CPU4))
/*TODO*///		{
/*TODO*///			logerror("unmapped mcu bank selected pc=%04x bank=%02x\n",cpu_get_pc(),data);
/*TODO*///			addr = 0x4000;
/*TODO*///		}
/*TODO*///		cpu_setbank( 20, memory_region(REGION_CPU4)+addr );
	} };
	
	/* This point is very obscure, but i havent found any better way yet. */
	/* Works with all games so far. 									  */
	
	/* patch points of memory address */
	/* CPU0/1 bank[17f][1000] */
	/* CPU2   [7000]	  */
	/* CPU3   [c000]	  */
	
	/* This memory point should be set $A6 by anywhere, but 		*/
	/* I found set $A6 only initialize in MCU						*/
	/* This patch kill write this data by MCU case $A6 to xx(clear) */
	
	public static WriteHandlerPtr namcos1_mcu_patch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
/*TODO*///		//logerror("mcu C000 write pc=%04x data=%02x\n",cpu_get_pc(),data);
/*TODO*///		if(mcu_patch_data == 0xa6) return;
/*TODO*///		mcu_patch_data = data;
/*TODO*///		cpu_bankbase[19][offset] = data;
	} };
/*TODO*///	
/*TODO*///	/*******************************************************************************
/*TODO*///	*																			   *
/*TODO*///	*	Initialization															   *
/*TODO*///	*																			   *
/*TODO*///	*******************************************************************************/
/*TODO*///	
/*TODO*///	static void namcos1_install_bank(int start,int end,mem_read_handler hr,mem_write_handler hw,
/*TODO*///				  int offset,unsigned char *pointer)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///		for(i=start;i<=end;i++)
/*TODO*///		{
/*TODO*///			namcos1_bank_element[i].bank_handler_r = hr;
/*TODO*///			namcos1_bank_element[i].bank_handler_w = hw;
/*TODO*///			namcos1_bank_element[i].bank_offset    = offset;
/*TODO*///			namcos1_bank_element[i].bank_pointer   = pointer;
/*TODO*///			offset	+= 0x2000;
/*TODO*///			if(pointer) pointer += 0x2000;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void namcos1_install_rom_bank(int start,int end,int size,int offset)
/*TODO*///	{
/*TODO*///		unsigned char *BROM = memory_region(REGION_USER1);
/*TODO*///		int step = size/0x2000;
/*TODO*///		while(start < end)
/*TODO*///		{
/*TODO*///			namcos1_install_bank(start,start+step-1,0,rom_w,0,&BROM[offset]);
/*TODO*///			start += step;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void namcos1_build_banks(mem_read_handler key_r,mem_write_handler key_w)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		/* S1 RAM pointer set */
/*TODO*///		s1ram = memory_region(REGION_USER2);
/*TODO*///	
/*TODO*///		/* clear all banks to unknown area */
/*TODO*///		for(i=0;i<NAMCOS1_MAX_BANK;i++)
/*TODO*///			namcos1_install_bank(i,i,unknown_r,unknown_w,0,0);
/*TODO*///	
/*TODO*///		/* RAM 6 banks - palette */
/*TODO*///		namcos1_install_bank(0x170,0x172,namcos1_paletteram_r,namcos1_paletteram_w,0,s1ram);
/*TODO*///		/* RAM 6 banks - work ram */
/*TODO*///		namcos1_install_bank(0x173,0x173,0,0,0,&s1ram[0x6000]);
/*TODO*///		/* RAM 5 banks - videoram */
/*TODO*///		namcos1_install_bank(0x178,0x17b,namcos1_videoram_r,namcos1_videoram_w,0,0);
/*TODO*///		/* key chip bank (rev1_key_w / rev2_key_w ) */
/*TODO*///		namcos1_install_bank(0x17c,0x17c,key_r,key_w,0,0);
/*TODO*///		/* RAM 7 banks - display control, playfields, sprites */
/*TODO*///		namcos1_install_bank(0x17e,0x17e,0,namcos1_videocontrol_w,0,&s1ram[0x8000]);
/*TODO*///		/* RAM 1 shared ram, PSG device */
/*TODO*///		namcos1_install_bank(0x17f,0x17f,soundram_r,soundram_w,0,namco_wavedata);
/*TODO*///		/* RAM 3 banks */
/*TODO*///		namcos1_install_bank(0x180,0x183,0,0,0,&s1ram[0xc000]);
/*TODO*///		/* PRG0 */
/*TODO*///		namcos1_install_rom_bank(0x200,0x23f,0x20000 , 0xe0000);
/*TODO*///		/* PRG1 */
/*TODO*///		namcos1_install_rom_bank(0x240,0x27f,0x20000 , 0xc0000);
/*TODO*///		/* PRG2 */
/*TODO*///		namcos1_install_rom_bank(0x280,0x2bf,0x20000 , 0xa0000);
/*TODO*///		/* PRG3 */
/*TODO*///		namcos1_install_rom_bank(0x2c0,0x2ff,0x20000 , 0x80000);
/*TODO*///		/* PRG4 */
/*TODO*///		namcos1_install_rom_bank(0x300,0x33f,0x20000 , 0x60000);
/*TODO*///		/* PRG5 */
/*TODO*///		namcos1_install_rom_bank(0x340,0x37f,0x20000 , 0x40000);
/*TODO*///		/* PRG6 */
/*TODO*///		namcos1_install_rom_bank(0x380,0x3bf,0x20000 , 0x20000);
/*TODO*///		/* PRG7 */
/*TODO*///		namcos1_install_rom_bank(0x3c0,0x3ff,0x20000 , 0x00000);
/*TODO*///	}
	
	public static InitMachinePtr init_namcos1 = new InitMachinePtr() {
            public void handler() {
                
	
/*TODO*///		int bank;
/*TODO*///	
/*TODO*///		/* Point all of our bankhandlers to the error handlers */
/*TODO*///		for( bank =0 ; bank < 2*8 ; bank++ )
/*TODO*///		{
/*TODO*///			/* set bank pointer & handler for cpu interface */
/*TODO*///			memory_set_bankhandler_r( bank+1,0,unknown_r);
/*TODO*///			memory_set_bankhandler_w( bank+1,0,unknown_w);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* Prepare code for Cpu 0 */
/*TODO*///		namcos1_bankswitch(0, 0x0e00, 0x03 ); /* bank7 = 0x3ff(PRG7) */
/*TODO*///		namcos1_bankswitch(0, 0x0e01, 0xff );
/*TODO*///	
/*TODO*///		/* Prepare code for Cpu 1 */
/*TODO*///		namcos1_bankswitch(1, 0x0e00, 0x03);
/*TODO*///		namcos1_bankswitch(1, 0x0e01, 0xff);
/*TODO*///	
/*TODO*///		namcos1_cpu1_banklatch = 0x03ff;
/*TODO*///	
/*TODO*///		/* Point mcu & sound shared RAM to destination */
/*TODO*///		{
/*TODO*///			unsigned char *RAM = namco_wavedata + 0x1000; /* Ram 1, bank 1, offset 0x1000 */
/*TODO*///			cpu_setbank( 18, RAM );
/*TODO*///			cpu_setbank( 19, RAM );
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* In case we had some cpu's suspended, resume them now */
/*TODO*///		cpu_set_reset_line(1,ASSERT_LINE);
/*TODO*///		cpu_set_reset_line(2,ASSERT_LINE);
/*TODO*///		cpu_set_reset_line(3,ASSERT_LINE);
/*TODO*///	
/*TODO*///		namcos1_reset = 0;
/*TODO*///		/* mcu patch data clear */
/*TODO*///		mcu_patch_data = 0;
/*TODO*///	
/*TODO*///		berabohm_input_counter = 4;	/* for berabohm pressure sensitive buttons */
	} };
	
	
	/*******************************************************************************
	*																			   *
	*	driver specific initialize routine										   *
	*																			   *
	*******************************************************************************/
	public static class namcos1_slice_timer
	{
		public int sync_cpu;	/* synchronus cpu attribute */
		public int sliceHz;	/* slice cycle				*/
		public int delayHz;	/* delay>=0 : delay cycle	*/
						/* delay<0	: slide cycle	*/
	};
	
	public static class namcos1_specific
	{
		/* keychip */
		public int key_id_query , key_id;
		public ReadHandlerPtr key_r;
		public WriteHandlerPtr key_w;
		/* cpu slice timer */
		//const struct namcos1_slice_timer *slice_timer;
                public namcos1_slice_timer slice_timer = new namcos1_slice_timer();
	};
	
/*TODO*///	static void namcos1_driver_init(const struct namcos1_specific *specific )
/*TODO*///	{
/*TODO*///		/* keychip id */
/*TODO*///		key_id_query = specific->key_id_query;
/*TODO*///		key_id		 = specific->key_id;
/*TODO*///	
/*TODO*///		/* build bank elements */
/*TODO*///		namcos1_build_banks(specific->key_r,specific->key_w);
/*TODO*///	
/*TODO*///		/* sound cpu speedup optimize (auto detect) */
/*TODO*///		{
/*TODO*///			unsigned char *RAM = memory_region(REGION_CPU3); /* sound cpu */
/*TODO*///			int addr,flag_ptr;
/*TODO*///	
/*TODO*///			for(addr=0xd000;addr<0xd0ff;addr++)
/*TODO*///			{
/*TODO*///				if(RAM[addr+0]==0xb6 &&   /* lda xxxx */
/*TODO*///				   RAM[addr+3]==0x27 &&   /* BEQ addr */
/*TODO*///				   RAM[addr+4]==0xfb )
/*TODO*///				{
/*TODO*///					flag_ptr = RAM[addr+1]*256 + RAM[addr+2];
/*TODO*///					if(flag_ptr>0x5140 && flag_ptr<0x5400)
/*TODO*///					{
/*TODO*///						sound_spinlock_pc	= addr+3;
/*TODO*///						sound_spinlock_ram	= install_mem_read_handler(2,flag_ptr,flag_ptr,namcos1_sound_spinlock_r);
/*TODO*///						logerror("Set sound cpu spinlock : pc=%04x , addr = %04x\n",sound_spinlock_pc,flag_ptr);
/*TODO*///						break;
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	#if NEW_TIMER
/*TODO*///		/* all cpu's does not need synchronization to all timers */
/*TODO*///		cpu_set_full_synchronize(SYNC_NO_CPU);
/*TODO*///		{
/*TODO*///			const struct namcos1_slice_timer *slice = specific->slice_timer;
/*TODO*///			while(slice->sync_cpu != SYNC_NO_CPU)
/*TODO*///			{
/*TODO*///				/* start CPU slice timer */
/*TODO*///				cpu_start_extend_time_slice(slice->sync_cpu,
/*TODO*///					TIME_IN_HZ(slice->delayHz),TIME_IN_HZ(slice->sliceHz) );
/*TODO*///				slice++;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	#else
/*TODO*///		/* compatible with old timer system */
/*TODO*///		timer_pulse(TIME_IN_HZ(60*25),0,0);
/*TODO*///	#endif
/*TODO*///	}
/*TODO*///	
/*TODO*///	#if NEW_TIMER
/*TODO*///	/* normaly CPU slice optimize */
/*TODO*///	/* slice order is 0:2:1:x:0:3:1:x */
/*TODO*///	static const struct namcos1_slice_timer normal_slice[]={
/*TODO*///		{ SYNC_2CPU(0,1),60*20,-60*20*2 },	/* CPU 0,1 20/vblank , slide slice */
/*TODO*///		{ SYNC_2CPU(2,3),60*5,-(60*5*2+60*20*4) },	/* CPU 2,3 10/vblank */
/*TODO*///		{ SYNC_NO_CPU }
/*TODO*///	};
/*TODO*///	#else
/*TODO*///	static const struct namcos1_slice_timer normal_slice[]={{0}};
/*TODO*///	#endif
	
	/*******************************************************************************
	*	Shadowland / Youkai Douchuuki specific									   *
	*******************************************************************************/
	public static InitDriverPtr init_shadowld = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific shadowld_specific=
/*TODO*///		{
/*TODO*///			0x00,0x00,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&shadowld_specific);
	} };
	
	/*******************************************************************************
	*	Dragon Spirit specific													   *
	*******************************************************************************/
	public static InitDriverPtr init_dspirit = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific dspirit_specific=
/*TODO*///		{
/*TODO*///			0x00,0x36,						/* key query , key id */
/*TODO*///			dspirit_key_r,dspirit_key_w,	/* key handler */
/*TODO*///			normal_slice,					/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&dspirit_specific);
	} };
	
	/*******************************************************************************
	*	Quester specific														   *
	*******************************************************************************/
	public static InitDriverPtr init_quester = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific quester_specific=
/*TODO*///		{
/*TODO*///			0x00,0x00,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&quester_specific);
	} };
	
	/*******************************************************************************
	*	Blazer specific 														   *
	*******************************************************************************/
	public static InitDriverPtr init_blazer = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific blazer_specific=
/*TODO*///		{
/*TODO*///			0x00,0x13,					/* key query , key id */
/*TODO*///			blazer_key_r,blazer_key_w,	/* key handler */
/*TODO*///			normal_slice,				/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&blazer_specific);
	} };
	
	/*******************************************************************************
	*	Pac-Mania / Pac-Mania (Japan) specific									   *
	*******************************************************************************/
	public static InitDriverPtr init_pacmania = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific pacmania_specific=
/*TODO*///		{
/*TODO*///			0x4b,0x12,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&pacmania_specific);
	} };
	
	/*******************************************************************************
	*	Galaga '88 / Galaga '88 (Japan) specific								   *
	*******************************************************************************/
	public static InitDriverPtr init_galaga88 = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific galaga88_specific=
/*TODO*///		{
/*TODO*///			0x2d,0x31,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&galaga88_specific);
	} };
	
	/*******************************************************************************
	*	World Stadium specific													   *
	*******************************************************************************/
	public static InitDriverPtr init_ws = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific ws_specific=
/*TODO*///		{
/*TODO*///			0xd3,0x07,				/* key query , key id */
/*TODO*///			ws_key_r,ws_key_w,		/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&ws_specific);
	} };
	
/*TODO*///	/*******************************************************************************
/*TODO*///	*	Beraboh Man specific													   *
/*TODO*///	*******************************************************************************/
/*TODO*///	public static ReadHandlerPtr berabohm_buttons_r  = new ReadHandlerPtr() { public int handler(int offset)
/*TODO*///	{
/*TODO*///		int res;
/*TODO*///	
/*TODO*///	
/*TODO*///		if (offset == 0)
/*TODO*///		{
/*TODO*///			if (berabohm_input_counter == 0) res = readinputport(0);
/*TODO*///			else
/*TODO*///			{
/*TODO*///				static int counter[4];
/*TODO*///	
/*TODO*///				res = readinputport(4 + (berabohm_input_counter-1));
/*TODO*///				if (res & 0x80)
/*TODO*///				{
/*TODO*///					if (counter[berabohm_input_counter-1] >= 0)
/*TODO*///	//					res = 0x40 | counter[berabohm_input_counter-1];	I can't get max power with this...
/*TODO*///						res = 0x40 | (counter[berabohm_input_counter-1]>>1);
/*TODO*///					else
/*TODO*///					{
/*TODO*///						if (res & 0x40) res = 0x40;
/*TODO*///						else res = 0x00;
/*TODO*///					}
/*TODO*///				}
/*TODO*///				else if (res & 0x40)
/*TODO*///				{
/*TODO*///					if (counter[berabohm_input_counter-1] < 0x3f)
/*TODO*///					{
/*TODO*///						counter[berabohm_input_counter-1]++;
/*TODO*///						res = 0x00;
/*TODO*///					}
/*TODO*///					else res = 0x7f;
/*TODO*///				}
/*TODO*///				else
/*TODO*///					counter[berabohm_input_counter-1] = -1;
/*TODO*///			}
/*TODO*///			berabohm_input_counter = (berabohm_input_counter+1) % 5;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			static int clk;
/*TODO*///	
/*TODO*///			res = 0;
/*TODO*///			clk++;
/*TODO*///			if (clk & 1) res |= 0x40;
/*TODO*///			else if (berabohm_input_counter == 4) res |= 0x10;
/*TODO*///	
/*TODO*///			res |= (readinputport(1) & 0x8f);
/*TODO*///		}
/*TODO*///	
/*TODO*///		return res;
/*TODO*///	} };
	
	public static InitDriverPtr init_berabohm = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific berabohm_specific=
/*TODO*///		{
/*TODO*///			0x00,0x00,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&berabohm_specific);
/*TODO*///		install_mem_read_handler(3,0x1400,0x1401,berabohm_buttons_r);
	} };
	
	/*******************************************************************************
	*	Alice in Wonderland / Marchen Maze specific 							   *
	*******************************************************************************/
	public static InitDriverPtr init_alice = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific alice_specific=
/*TODO*///		{
/*TODO*///			0x5b,0x25,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&alice_specific);
	} };
	
	/*******************************************************************************
	*	Bakutotsu Kijuutei specific 											   *
	*******************************************************************************/
	public static InitDriverPtr init_bakutotu = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific bakutotu_specific=
/*TODO*///		{
/*TODO*///			0x03,0x22,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&bakutotu_specific);
	} };
	
	/*******************************************************************************
	*	World Court specific													   *
	*******************************************************************************/
	public static InitDriverPtr init_wldcourt = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific worldcourt_specific=
/*TODO*///		{
/*TODO*///			0x00,0x35,				/* key query , key id */
/*TODO*///			rev2_key_r,rev2_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&worldcourt_specific);
	} };
	
	/*******************************************************************************
	*	Splatter House specific 												   *
	*******************************************************************************/
	public static InitDriverPtr init_splatter = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific splatter_specific=
/*TODO*///		{
/*TODO*///			0x00,0x00,						/* key query , key id */
/*TODO*///			splatter_key_r,splatter_key_w,	/* key handler */
/*TODO*///			normal_slice,					/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&splatter_specific);
	} };
	
	/*******************************************************************************
	*	Face Off specific														   *
	*******************************************************************************/
	public static InitDriverPtr init_faceoff = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific faceoff_specific=
/*TODO*///		{
/*TODO*///			0x00,0x00,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&faceoff_specific);
	} };
	
	/*******************************************************************************
	*	Rompers specific														   *
	*******************************************************************************/
	public static InitDriverPtr init_rompers = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific rompers_specific=
/*TODO*///		{
/*TODO*///			0x00,0x00,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&rompers_specific);
/*TODO*///		key[0x70] = 0xb6;
	} };
	
	/*******************************************************************************
	*	Blast Off specific														   *
	*******************************************************************************/
	public static InitDriverPtr init_blastoff = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific blastoff_specific=
/*TODO*///		{
/*TODO*///			0x00,0x00,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&blastoff_specific);
/*TODO*///		key[0] = 0xb7;
	} };
	
	/*******************************************************************************
	*	World Stadium '89 specific                                                 *
	*******************************************************************************/
	public static InitDriverPtr init_ws89 = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific ws89_specific=
/*TODO*///		{
/*TODO*///			0x00,0x00,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&ws89_specific);
/*TODO*///	
/*TODO*///		key[0x20] = 0xb8;
	} };
	
	/*******************************************************************************
	*	Dangerous Seed specific 												   *
	*******************************************************************************/
	public static InitDriverPtr init_dangseed = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific dangseed_specific=
/*TODO*///		{
/*TODO*///			0x00,0x34,						/* key query , key id */
/*TODO*///			dangseed_key_r,dangseed_key_w,	/* key handler */
/*TODO*///			normal_slice,					/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&dangseed_specific);
	} };
	
	/*******************************************************************************
	*	World Stadium '90 specific                                                 *
	*******************************************************************************/
	public static InitDriverPtr init_ws90 = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific ws90_specific=
/*TODO*///		{
/*TODO*///			0x00,0x00,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&ws90_specific);
/*TODO*///	
/*TODO*///		key[0x47] = 0x36;
/*TODO*///		key[0x40] = 0x36;
	} };
	
	/*******************************************************************************
	*	Pistol Daimyo no Bouken specific										   *
	*******************************************************************************/
	public static InitDriverPtr init_pistoldm = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific pistoldm_specific=
/*TODO*///		{
/*TODO*///			0x00,0x00,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&pistoldm_specific);
/*TODO*///		//key[0x17] = ;
/*TODO*///		//key[0x07] = ;
/*TODO*///		key[0x43] = 0x35;
	} };
	
	/*******************************************************************************
	*	Souko Ban DX specific													   *
	*******************************************************************************/
	public static InitDriverPtr init_soukobdx = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific soukobdx_specific=
/*TODO*///		{
/*TODO*///			0x00,0x00,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&soukobdx_specific);
/*TODO*///		//key[0x27] = ;
/*TODO*///		//key[0x07] = ;
/*TODO*///		key[0x43] = 0x37;
	} };
	
	/*******************************************************************************
	*	Puzzle Club specific													   *
	*******************************************************************************/
	public static InitDriverPtr init_puzlclub = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific puzlclub_specific=
/*TODO*///		{
/*TODO*///			0x00,0x00,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&puzlclub_specific);
/*TODO*///		key[0x03] = 0x35;
	} };
	
	/*******************************************************************************
	*	Tank Force specific 													   *
	*******************************************************************************/
	public static InitDriverPtr init_tankfrce = new InitDriverPtr() { public void handler() 
	{
/*TODO*///		const struct namcos1_specific tankfrce_specific=
/*TODO*///		{
/*TODO*///			0x00,0x00,				/* key query , key id */
/*TODO*///			rev1_key_r,rev1_key_w,	/* key handler */
/*TODO*///			normal_slice,			/* CPU slice normal */
/*TODO*///		};
/*TODO*///		namcos1_driver_init(&tankfrce_specific);
/*TODO*///		//key[0x57] = ;
/*TODO*///		//key[0x17] = ;
/*TODO*///		key[0x2b] = 0xb9;
/*TODO*///		key[0x50] = 0xb9;
	} };
}
