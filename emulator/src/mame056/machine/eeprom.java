/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.machine;

import static arcadeflex056.fileio.*;
import static common.libc.cstring.*;
import static common.ptr.*;
import static mame056.cpuintrfH.*;
import static mame056.machine.eepromH.*;
import static mame056.usrintrf.usrintf_showmessage;

// refactor
import static arcadeflex036.osdepend.logerror;

public class eeprom
{
	
/*TODO*///	#define VERBOSE 0
/*TODO*///	
        public static int SERIAL_BUFFER_LENGTH = 40;
        public static int MEMORY_SIZE = 1024;
	
	public static EEPROM_interface intf;
	
	static int serial_count;
	static int[] serial_buffer = new int[SERIAL_BUFFER_LENGTH];
	static char[] eeprom_data = new char[MEMORY_SIZE];
	static int eeprom_data_bits;
	static int eeprom_read_address;
	static int eeprom_clock_count;
	static int latch,reset_line,clock_line,sending;
	static int locked;
	static int reset_delay;
	
	/*
		EEPROM_command_match:
	
		Try to match the first (len) digits in the EEPROM serial buffer
		string (*buf) with	an EEPROM command string (*cmd).
		Return non zero if a match was found.
	
		The serial buffer only contains '0' or '1' (e.g. "1001").
		The command can contain: '0' or '1' or these wildcards:
	
		'x' :	match both '0' and '1'
		"*1":	match "1", "01", "001", "0001" etc.
		"*0":	match "0", "10", "110", "1110" etc.
	
		Note: (cmd) may be NULL. Return 0 (no match) in this case.
	*/
	static int EEPROM_command_match(int[] buf, String cmd, int len)
	{
		if ( cmd == null )	return 0;
		if ( len == 0 )	return 0;
                
                int _buf = 0;
                int _cmd = 0;
	
		for (;( (len>0)  );)
		{
			char b = (char) buf[_buf];
                        //System.out.println(b);
			char c;
                        if (_cmd<cmd.length())
                            c = cmd.charAt(_cmd);
                        else
                            c = 0;
                        //System.out.println(c);
	
			if ((b==0) || (c==0))
				return (b==c)?1:0;
	
			switch ( c )
			{
				case '0':
				case '1':
					if (b != c)	return 0;
				case 'X':
				case 'x':
					_buf++;
					len--;
					_cmd++;
					break;
	
				case '*':
					c = cmd.charAt(1);
					switch( c )
					{
						case '0':
						case '1':
						  	if (b == c)	{	_cmd++;			}
							else		{	_buf++;	len--;	}
							break;
						default:	return 0;
					}
			}
		}
		return (cmd.charAt(_cmd)==0 ? 1:0);
	}
	
	
/*TODO*///	struct EEPROM_interface eeprom_interface_93C46 =
/*TODO*///	{
/*TODO*///		6,				// address bits	6
/*TODO*///		16,				// data bits	16
/*TODO*///		"*110",			// read			1 10 aaaaaa
/*TODO*///		"*101",			// write		1 01 aaaaaa dddddddddddddddd
/*TODO*///		"*111",			// erase		1 11 aaaaaa
/*TODO*///		"*10000xxxx",	// lock			1 00 00xxxx
/*TODO*///		"*10011xxxx",	// unlock		1 00 11xxxx
/*TODO*///	//	"*10001xxxx"	// write all	1 00 01xxxx dddddddddddddddd
/*TODO*///	//	"*10010xxxx"	// erase all	1 00 10xxxx
/*TODO*///	};
/*TODO*///	
/*TODO*///	
/*TODO*///	void nvram_handler_93C46(void *file,int read_or_write)
/*TODO*///	{
/*TODO*///		if (read_or_write)
/*TODO*///			EEPROM_save(file);
/*TODO*///		else
/*TODO*///		{
/*TODO*///			EEPROM_init(&eeprom_interface_93C46);
/*TODO*///			if (file)	EEPROM_load(file);
/*TODO*///		}
/*TODO*///	}
	
	
	public static void EEPROM_init(EEPROM_interface _interface)
	{
		intf = _interface;
	
		if ((1 << intf.address_bits) * intf.data_bits / 8 > MEMORY_SIZE)
		{
			usrintf_showmessage("EEPROM larger than eeprom.c allows");
			return;
		}
	
		memset(eeprom_data,0xff,(1 << intf.address_bits) * intf.data_bits / 8);
		serial_count = 0;
		latch = 0;
		reset_line = ASSERT_LINE;
		clock_line = ASSERT_LINE;
		eeprom_read_address = 0;
		sending = 0;
		if (intf.cmd_unlock != null) locked = 1;
		else locked = 0;
	
/*TODO*///		state_save_register_UINT8("eeprom", 0, "data",          eeprom_data,   MEMORY_SIZE);
/*TODO*///		state_save_register_UINT8("eeprom", 0, "serial buffer", serial_buffer, SERIAL_BUFFER_LENGTH);
/*TODO*///		state_save_register_int  ("eeprom", 0, "clock line",    &clock_line);
/*TODO*///		state_save_register_int  ("eeprom", 0, "reset line",    &reset_line);
/*TODO*///		state_save_register_int  ("eeprom", 0, "locked",        &locked);
/*TODO*///		state_save_register_int  ("eeprom", 0, "serial count",  &serial_count);
/*TODO*///		state_save_register_int  ("eeprom", 0, "latch",         &latch);
/*TODO*///		state_save_register_int  ("eeprom", 0, "reset delay",   &reset_delay);
/*TODO*///		state_save_register_int  ("eeprom", 0, "clock count",   &eeprom_clock_count);
/*TODO*///		state_save_register_int  ("eeprom", 0, "data bits",     &eeprom_data_bits);
/*TODO*///		state_save_register_int  ("eeprom", 0, "address",       &eeprom_read_address);
	}
	
	public static void EEPROM_write(int bit)
	{
/*TODO*///	#if VERBOSE
/*TODO*///	logerror("EEPROM write bit %d\n",bit);
/*TODO*///	#endif
	
		if (serial_count >= SERIAL_BUFFER_LENGTH-1)
		{
	logerror("error: EEPROM serial buffer overflow\n");
			return;
		}
	
		serial_buffer[serial_count++] = (bit!=0 ? '1' : '0');
		serial_buffer[serial_count] = 0;	/* nul terminate so we can treat it as a string */
	
		if ( (serial_count > intf.address_bits) &&
		      EEPROM_command_match(serial_buffer,intf.cmd_read,(serial_buffer.length)-intf.address_bits) != 0)
		{
			int i,address;
	
			address = 0;
			for (i = serial_count-intf.address_bits;i < serial_count;i++)
			{
				address <<= 1;
				if (serial_buffer[i] == '1') address |= 1;
			}
			if (intf.data_bits == 16)
				eeprom_data_bits = (eeprom_data[2*address+0] << 8) + eeprom_data[2*address+1];
			else
				eeprom_data_bits = eeprom_data[address];
			eeprom_read_address = address;
			eeprom_clock_count = 0;
			sending = 1;
			serial_count = 0;
                        logerror("EEPROM read %04x from address %02x\n",eeprom_data_bits,address);
		}
		else if ( (serial_count > intf.address_bits) &&
		           EEPROM_command_match(serial_buffer,intf.cmd_erase,(serial_buffer.length)-intf.address_bits) != 0)
		{
			int i,address;
	
			address = 0;
			for (i = serial_count-intf.address_bits;i < serial_count;i++)
			{
				address <<= 1;
				if (serial_buffer[i] == '1') address |= 1;
			}
                        logerror("EEPROM erase address %02x\n",address);
			if (locked == 0)
			{
				if (intf.data_bits == 16)
				{
					eeprom_data[2*address+0] = 0x00;
					eeprom_data[2*address+1] = 0x00;
				}
				else
					eeprom_data[address] = 0x00;
			}
			else
                            logerror("Error: EEPROM is locked\n");
			serial_count = 0;
		}
		else if ( (serial_count > (intf.address_bits + intf.data_bits)) &&
		           EEPROM_command_match(serial_buffer,intf.cmd_write,(serial_buffer.length)-(intf.address_bits + intf.data_bits)) != 0)
		{
			int i,address,data;
	
			address = 0;
			for (i = serial_count-intf.data_bits-intf.address_bits;i < (serial_count-intf.data_bits);i++)
			{
				address <<= 1;
				if (serial_buffer[i] == '1') address |= 1;
			}
			data = 0;
			for (i = serial_count-intf.data_bits;i < serial_count;i++)
			{
				data <<= 1;
				if (serial_buffer[i] == '1') data |= 1;
			}
                        logerror("EEPROM write %04x to address %02x\n",data,address);
			if (locked == 0)
			{
				if (intf.data_bits == 16)
				{
					eeprom_data[2*address+0] = (char) (data >> 8);
					eeprom_data[2*address+1] = (char) (data & 0xff);
				}
				else
					eeprom_data[address] = (char) data;
			}
			else
                            logerror("Error: EEPROM is locked\n");
			serial_count = 0;
		}
		else if ( EEPROM_command_match(serial_buffer,intf.cmd_lock,(serial_buffer.length)) != 0)
		{
                        logerror("EEPROM lock\n");
			locked = 1;
			serial_count = 0;
		}
		else if ( EEPROM_command_match(serial_buffer,intf.cmd_unlock,(serial_buffer.length)) != 0 )
		{
                        logerror("EEPROM unlock\n");
			locked = 0;
			serial_count = 0;
		}
	}
	
	public static void EEPROM_reset()
	{
	if (serial_count != 0)
		logerror("EEPROM reset, buffer = %s\n",serial_buffer);
	
		serial_count = 0;
		sending = 0;
		reset_delay = 5;	/* delay a little before returning setting data to 1 (needed by wbeachvl) */
	}
	
	
	public static void EEPROM_write_bit(int bit)
	{
/*TODO*///	#if VERBOSE
/*TODO*///	logerror("write bit %d\n",bit);
/*TODO*///	#endif
		latch = bit;
	}
	
	public static int EEPROM_read_bit()
	{
		int res;
	
		if (sending != 0)
			res = (eeprom_data_bits >> intf.data_bits) & 1;
		else
		{
			if (reset_delay > 0)
			{
				/* this is needed by wbeachvl */
				reset_delay--;
				res = 0;
			}
			else
				res = 1;
		}
	
/*TODO*///	#if VERBOSE
/*TODO*///	logerror("read bit %d\n",res);
/*TODO*///	#endif
	
		return res;
	}
	
	public static void EEPROM_set_cs_line(int state)
	{
	/*TODO*///#if VERBOSE
	/*TODO*///logerror("set reset line %d\n",state);
	/*TODO*///#endif
		reset_line = state;
	
		if (reset_line != CLEAR_LINE)
			EEPROM_reset();
	}
	
	public static void EEPROM_set_clock_line(int state)
	{
/*TODO*///	#if VERBOSE
/*TODO*///	logerror("set clock line %d\n",state);
/*TODO*///	#endif
		if (state == PULSE_LINE || (clock_line == CLEAR_LINE && state != CLEAR_LINE))
		{
			if (reset_line == CLEAR_LINE)
			{
				if (sending != 0)
				{
					if ((eeprom_clock_count == intf.data_bits) && (intf.enable_multi_read)!=0)
					{
						eeprom_read_address = (eeprom_read_address + 1) & ((1 << intf.address_bits) - 1);
						if (intf.data_bits == 16)
							eeprom_data_bits = (eeprom_data[2*eeprom_read_address+0] << 8) + eeprom_data[2*eeprom_read_address+1];
						else
							eeprom_data_bits = eeprom_data[eeprom_read_address];
						eeprom_clock_count = 0;
                                                logerror("EEPROM read %04x from address %02x\n",eeprom_data_bits,eeprom_read_address);
					}
					eeprom_data_bits = (eeprom_data_bits << 1) | 1;
					eeprom_clock_count++;
				}
				else
					EEPROM_write(latch);
			}
		}
	
		clock_line = state;
	}
	
	
	public static void EEPROM_load(Object f)
	{
		osd_fread(f,eeprom_data,(1 << intf.address_bits) * intf.data_bits / 8);
	}
	
	public static void EEPROM_save(Object f)
	{
		osd_fwrite(f,eeprom_data,(1 << intf.address_bits) * intf.data_bits / 8);
	}
	
	public static void EEPROM_set_data(char[] data, int length)
	{
		memcpy(eeprom_data, data, length);
	}
	
/*TODO*///	UINT8 * EEPROM_get_data_pointer(int * length)
/*TODO*///	{
/*TODO*///		if(length)
/*TODO*///			*length = MEMORY_SIZE;
/*TODO*///	
/*TODO*///		return eeprom_data;
/*TODO*///	}
    
}
