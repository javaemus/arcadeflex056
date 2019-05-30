/***************************************************************************

	Leland Ataxx-era driver

	driver by Aaron Giles and Paul Leaman

	-------------------------------------

	To enter service mode in Ataxx and Brute Force, press 1P start and
	then press the service switch (F2).

	For World Soccer Finals, press the 1P button B and then press the
	service switch.

	For Indy Heat, press the red turbo button (1P button 1) and then
	press the service switch.

	-------------------------------------

	Still to do:
		- memory map
		- generate fake serial numbers

***************************************************************************/


/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.libc.cstring.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.inptportH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.cpuintrf.*;
import static mame056.driverH.*;
import static mame056.cpu.z80.z80H.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.memoryH.*;
import static mame056.memory.*;
import static mame056.inptport.*;
import static mame056.drawgfxH.*;
import static mame056.inputH.*;
import static mame056.sndintrfH.*;
import static mame056.sndintrf.*;
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;
import static mame056.sound.mixer.*;
import static mame056.sound.mixerH.*;
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;
import static mame056.sound.sn76496.*;
import static mame056.sound.sn76496H.*;

import static mame056.sound._2151intf.*;
import static mame056.sound._2151intfH.*;
import static mame056.vidhrdw.generic.*;

import static arcadeflex056.fileio.*;
import static mame056.palette.*;

import static WIP.mame056.vidhrdw.leland.*;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.machine.eepromH.*;
import static mame056.machine.eeprom.*;
import static WIP.mame056.sndhrdw.leland.*;
import static WIP.mame056.drivers.leland.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class ataxx
{
	/* define these to 0 to disable, or to 1 to enable */
	public static int LOG_BANKSWITCHING_M           = 0;
	public static int LOG_BANKSWITCHING_S           = 0;
	public static int LOG_EEPROM			= 0;
	public static int LOG_XROM			= 0;
	public static int LOG_BATTERY_RAM		= 0;
	
	
	/* Helps document the input ports. */
	public static int IPT_SLAVEHALT                 = IPT_SPECIAL;
	public static int IPT_EEPROM_DATA               = IPT_SPECIAL;
	
        public static void PORT_SERVICE_NO_TOGGLE(int mask, int _default){
                PORT_BITX(    mask, mask & _default, IPT_DIPSWITCH_NAME, DEF_STR( "Service_Mode"), KEYCODE_F2, IP_JOY_NONE );
		PORT_DIPSETTING(    mask & _default, DEF_STR( "Off") );	
		PORT_DIPSETTING(    mask &~_default, DEF_STR( "On") );	
        };
	
	static int wcol_enable;
	
	static timer_entry master_int_timer;
	
	static UBytePtr master_base = new UBytePtr();
	static UBytePtr slave_base = new UBytePtr();
	static UBytePtr xrom_base = new UBytePtr();
	static int master_length;
	static int slave_length;
	static int xrom_length;
	
	static int analog_result;
	static int[] dial_last_input=new int[4];
	static int[] dial_last_result=new int[4];
	
	public static int master_bank;
	
	static int xrom1_addr;
	static int xrom2_addr;
	
	public static int battery_ram_size = 0x4000;
	static int battery_ram_enable;
	static UBytePtr battery_ram = new UBytePtr();
	
	public static int extra_tram_size = 0x800;
	static UBytePtr extra_tram = new UBytePtr();
	
	static char[] eeprom_data= new char[128*2];
	static EEPROM_interface eeprom_interface = new EEPROM_interface
        (
		7,
		16,
		"000001100",
		"000001010",
		null,
		"0000010000000000",
		"0000010011000000",
		1
	);
	
	
	/*************************************
	 *
	 *	Generic dial encoding
	 *
	 *************************************/
	
	static int dial_compute_value(int new_val, int indx)
	{
		int delta = new_val - (int)dial_last_input[indx];
		int result = dial_last_result[indx] & 0x80;
	
		dial_last_input[indx] = new_val;
	
		if (delta > 0x80)
			delta -= 0x100;
		else if (delta < -0x80)
			delta += 0x100;
	
		if (delta < 0)
		{
			result = 0x80;
			delta = -delta;
		}
		else if (delta > 0)
			result = 0x00;
	
		if (delta > 0x1f)
			delta = 0x1f;
		result |= (dial_last_result[indx] + delta) & 0x1f;
	
		dial_last_result[indx] = result;
		return result;
	}
	
	
	
	/*************************************
	 *
	 *	Ataxx inputs
	 *
	 *************************************/
	
	public static ReadHandlerPtr ataxx_trackball_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return dial_compute_value(readinputport(3 + offset), offset);
	} };
	
	
	
	/*************************************
	 *
	 *	Indy Heat inputs
	 *
	 *************************************/
	
	public static ReadHandlerPtr indyheat_wheel_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return dial_compute_value(readinputport(3 + offset), offset);
	} };
	
	public static ReadHandlerPtr indyheat_analog_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		switch (offset)
		{
			case 0:
				return 0;
	
			case 1:
				return analog_result;
	
			case 2:
				return 0;
	
			case 3:
				logerror("Unexpected analog read(%02X)n", 8 + offset);
				break;
		}
		return 0xff;
	} };
	
	public static WriteHandlerPtr indyheat_analog_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		switch (offset)
		{
			case 3:
				analog_result = readinputport(6 + data);
				break;
	
			case 0:
			case 1:
			case 2:
				logerror("Unexpected analog write(%02X) = %02Xn", 8 + offset, data);
				break;
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Machine initialization
	 *
	 *************************************/
	
	public static InitMachinePtr init_machine = new InitMachinePtr() { public void handler()
	{
		/* set the odd data banks */
		battery_ram = new UBytePtr(memory_region(REGION_USER2));
		extra_tram = new UBytePtr(battery_ram, battery_ram_size);
	
		/* initialize the XROM */
		xrom_length = memory_region_length(REGION_USER1);
		xrom_base = memory_region(REGION_USER1);
		xrom1_addr = 0;
		xrom2_addr = 0;
	
		/* start scanline interrupts going */
		master_int_timer = timer_set(cpu_getscanlinetime(8), 8, interrupt_callback);
	
		/* reset globals */
		wcol_enable = 0;
	
		analog_result = 0xff;
		memset(dial_last_input, 0, dial_last_input.length);
		memset(dial_last_result, 0, dial_last_result.length);
	
		master_bank = 0;
	
		/* initialize the master banks */
		master_length = memory_region_length(REGION_CPU1);
		master_base = memory_region(REGION_CPU1);
		master_bankswitch();
	
		/* initialize the slave banks */
		slave_length = memory_region_length(REGION_CPU2);
		slave_base = memory_region(REGION_CPU2);
		if (slave_length > 0x10000)
			cpu_setbank(3, new UBytePtr(slave_base, 0x10000));
	
		/* reset the I186 */
		/*TODO*///leland_i186_sound_init();
	} };
	
	
	
	/*************************************
	 *
	 *	Master CPU interrupt handling
	 *
	 *************************************/
	
	static timer_callback interrupt_callback = new timer_callback() {
            public void handler(int scanline) {
                leland_last_scanline_int = scanline;
	
		/* interrupts generated according to the interrupt control register */
		cpu_cause_interrupt(0, 0);
	
		/* set a timer for the next one */
		master_int_timer = timer_set(cpu_getscanlinetime(scanline), scanline, interrupt_callback);
            }
        };
	
	/*************************************
	 *
	 *	Master CPU bankswitch handlers
	 *
	 *************************************/
	
	public static void master_bankswitch()
	{
		int bank_list[] =
		{
			0x02000, 0x18000, 0x20000, 0x28000, 0x30000, 0x38000, 0x40000, 0x48000,
			0x50000, 0x58000, 0x60000, 0x68000, 0x70000, 0x78000, 0x80000, 0x88000
		};
		UBytePtr address = new UBytePtr();
	
		battery_ram_enable = ((master_bank & 0x30) == 0x10)?1:0;
	
		address = new UBytePtr(master_base, bank_list[master_bank & 15]);
		if (bank_list[master_bank & 15] >= master_length)
		{
			logerror("%04X:Master bank %02X out of range!n", cpu_getpreviouspc(), master_bank & 15);
			address = new UBytePtr(master_base, bank_list[0]);
		}
		cpu_setbank(1, address);
	
		if (battery_ram_enable != 0)
			address = battery_ram;
		else if ((master_bank & 0x30) == 0x20)
			address = new UBytePtr(ataxx_qram, (master_bank & 0xc0) << 8);
		else
			address = new UBytePtr(master_base, 0xa000);
		cpu_setbank(2, address);
	
		wcol_enable = ((master_bank & 0x30) == 0x30)?1:0;
	}
	
	
	
	/*************************************
	 *
	 *	EEPROM handling (128 x 16bits)
	 *
	 *************************************/
	
	public static void init_eeprom(int default_val, UBytePtr data, int serial_offset)
	{
		int serial;
                int _num = 0;
		/* initialize everything to the default value */
		memset(eeprom_data, default_val, eeprom_data.length);
	
		/* fill in the preset data */
		do 
		{
                    //System.out.println(_num++);
			int offset = data.read();
                       // System.out.println(offset);
                        
                            data.inc();
                            int value = data.read();
                            eeprom_data[offset * 2 + 0] = (char) (value >> 8);
                            eeprom_data[offset * 2 + 1] = (char) (value & 0xff);
                        data.inc();
                        
		}while (data.read() != 0xff);
	
		/* pick a serial number -- examples of real serial numbers:
	
			WSF:         30101190
			Indy Heat:   31201339
		*/
		serial = 0x12345678;
	
		/* encrypt the serial number */
		{
			int d, e, h, l;
	
			/* break the serial number out into pieces */
			l = (serial >> 24) & 0xff;
			h = (serial >> 16) & 0xff;
			e = (serial >> 8) & 0xff;
			d = serial & 0xff;
	
			/* decrypt the data */
			h = ((h ^ 0x2a ^ l) ^ 0xff) + 5;
			d = ((d + 0x2a) ^ e) ^ 0xff;
			l ^= e;
			e ^= 0x2a;
	
			/* store the bytes */
			eeprom_data[serial_offset * 2 + 0] = (char) h;
			eeprom_data[serial_offset * 2 + 1] = (char) l;
			eeprom_data[serial_offset * 2 + 2] = (char) d;
			eeprom_data[serial_offset * 2 + 3] = (char) e;
		}
	
		/* compute the checksum */
		{
			int i, sum = 0;
			for (i = 0; i < 0x7f * 2; i++)
				sum += eeprom_data[i];
			sum ^= 0xffff;
			eeprom_data[0x7f * 2 + 0] = (char) ((sum >> 8) & 0xff);
			eeprom_data[0x7f * 2 + 1] = (char) (sum & 0xff);
	
			EEPROM_init(eeprom_interface);
		}
	};
	
	
	
	/*************************************
	 *
	 *	Battery backed RAM
	 *
	 *************************************/
	
	public static WriteHandlerPtr battery_ram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (battery_ram_enable != 0)
		{
			/*TODO*///if (LOG_BATTERY_RAM) logerror("%04X:BatteryW@%04X=%02Xn", cpu_getpreviouspc(), offset, data);
			battery_ram.write(offset, data);
		}
		else if ((master_bank & 0x30) == 0x20)
			ataxx_qram.write(((master_bank & 0xc0) << 8) + offset, data);
		else
			logerror("%04X:BatteryW@%04X (invalid!)n", cpu_getpreviouspc(), offset, data);
	} };
	
	
	static nvramPtr nvram_handler = new nvramPtr() {
            public void handler(Object file, int read_or_write) {
                if (read_or_write != 0)
		{
			EEPROM_save(file);
			osd_fwrite(file, memory_region(REGION_USER2), battery_ram_size);
		}
		else if (file != null)
		{
			EEPROM_load(file);
			osd_fread(file, memory_region(REGION_USER2), battery_ram_size);
		}
		else
		{
			EEPROM_set_data(eeprom_data, 128*2);
			memset(memory_region(REGION_USER2), 0x00, battery_ram_size);
		}
            }
        };
	
	/*************************************
	 *
	 *	Master CPU internal I/O
	 *
	 *************************************/
	
	public static ReadHandlerPtr master_input_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int result = 0xff;
	
		switch (offset)
		{
			case 0x06:	/* /GIN0 */
				result = readinputport(0);
				break;
	
			case 0x07:	/* /SLVBLK */
				result = readinputport(1);
				if (cpunum_get_reg(1, Z80_HALT) != 0)
					result ^= 0x01;
				break;
	
			default:
				logerror("Master I/O read offset %02Xn", offset);
				break;
		}
		return result;
	} };
	
	
	public static WriteHandlerPtr master_output_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		switch (offset)
		{
			case 0x00:	/* /BKXL */
			case 0x01:	/* /BKXH */
			case 0x02:	/* /BKYL */
			case 0x03:	/* /BKYH */
				leland_gfx_port_w.handler(offset, data);
				break;
	
			case 0x04:	/* /MBNK */
				if (LOG_BANKSWITCHING_M != 0)
					if (((master_bank ^ data) & 0xff) != 0)
						logerror("%04X:master_bank = %02Xn", cpu_getpreviouspc(), data & 0xff);
				master_bank = data;
				master_bankswitch();
				break;
	
			case 0x05:	/* /SLV0 */
				cpu_set_irq_line  (1, 0, (data & 0x01)!=0 ? CLEAR_LINE : ASSERT_LINE);
				cpu_set_nmi_line  (1,    (data & 0x04)!=0 ? CLEAR_LINE : ASSERT_LINE);
				cpu_set_reset_line(1,    (data & 0x10)!=0 ? CLEAR_LINE : ASSERT_LINE);
				break;
	
			case 0x08:	/*  */
				if (master_int_timer != null)
					timer_remove(master_int_timer);
				master_int_timer = timer_set(cpu_getscanlinetime(data + 1), data + 1, interrupt_callback);
				break;
	
			default:
				logerror("Master I/O write offset %02X=%02Xn", offset, data);
				break;
		}
	} };
	
	
	public static ReadHandlerPtr eeprom_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int port = readinputport(2);
		/*TODO*///if (LOG_EEPROM) logerror("%04X:EE readn", cpu_getpreviouspc());
		return (port & ~0x01) | EEPROM_read_bit();
	} };
	
	
	public static WriteHandlerPtr eeprom_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (LOG_EEPROM!=0) logerror("%04X:EE write %d%d%dn", cpu_getpreviouspc(),(data >> 6) & 1, (data >> 5) & 1, (data >> 4) & 1);
		EEPROM_write_bit     ((data & 0x10) >> 4);
		EEPROM_set_clock_line((data & 0x20)!=0 ? ASSERT_LINE : CLEAR_LINE);
		EEPROM_set_cs_line  ((~data & 0x40)!=0 ? ASSERT_LINE : CLEAR_LINE);
	} };
	
	
	
	/*************************************
	 *
	 *	Master CPU palette gates
	 *
	 *************************************/
	
	public static WriteHandlerPtr paletteram_and_misc_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (wcol_enable != 0)
			paletteram_xxxxRRRRGGGGBBBB_w.handler(offset, data);
		else if (offset == 0x7f8 || offset == 0x7f9)
			leland_master_video_addr_w.handler(offset - 0x7f8, data);
		else if (offset == 0x7fc)
		{
			xrom1_addr = (xrom1_addr & 0xff00) | (data & 0x00ff);
			if (LOG_XROM != 0) logerror("%04X:XROM1 address low write = %02X (addr=%04X)n", cpu_getpreviouspc(), data, xrom1_addr);
		}
		else if (offset == 0x7fd)
		{
			xrom1_addr = (xrom1_addr & 0x00ff) | ((data << 8) & 0xff00);
			if (LOG_XROM != 0) logerror("%04X:XROM1 address high write = %02X (addr=%04X)n", cpu_getpreviouspc(), data, xrom1_addr);
		}
		else if (offset == 0x7fe)
		{
			xrom2_addr = (xrom2_addr & 0xff00) | (data & 0x00ff);
			if (LOG_XROM != 0) logerror("%04X:XROM2 address low write = %02X (addr=%04X)n", cpu_getpreviouspc(), data, xrom2_addr);
		}
		else if (offset == 0x7ff)
		{
			xrom2_addr = (xrom2_addr & 0x00ff) | ((data << 8) & 0xff00);
			if (LOG_XROM != 0) logerror("%04X:XROM2 address high write = %02X (addr=%04X)n", cpu_getpreviouspc(), data, xrom2_addr);
		}
		else
			extra_tram.write(offset, data);
	} };
	
	
	public static ReadHandlerPtr paletteram_and_misc_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if (wcol_enable != 0)
			return paletteram_r.handler(offset);
		else if (offset == 0x7fc || offset == 0x7fd)
		{
			int result = xrom_base.read(0x00000 | xrom1_addr | ((offset & 1) << 16));
			if (LOG_XROM != 0) logerror("%04X:XROM1 read(%d) = %02X (addr=%04X)n", cpu_getpreviouspc(), offset - 0x7fc, result, xrom1_addr);
			return result;
		}
		else if (offset == 0x7fe || offset == 0x7ff)
		{
			int result = xrom_base.read(0x20000 | xrom2_addr | ((offset & 1) << 16));
			if (LOG_XROM != 0) logerror("%04X:XROM2 read(%d) = %02X (addr=%04X)n", cpu_getpreviouspc(), offset - 0x7fc, result, xrom2_addr);
			return result;
		}
		else
			return extra_tram.read(offset);
	} };
	
	
	
	/*************************************
	 *
	 *	Slave CPU bankswitching
	 *
	 *************************************/
	
	public static WriteHandlerPtr slave_banksw_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int bankaddress, bank = data & 15;
	
		if (bank == 0)
			bankaddress = 0x2000;
		else
		{
			bankaddress = 0x10000 * bank + 0x8000 * ((data >> 4) & 1);
			if (slave_length > 0x100000)
				bankaddress += 0x100000 * ((data >> 5) & 1);
		}
	
		if (bankaddress >= slave_length)
		{
			logerror("%04X:Slave bank %02X out of range!", cpu_getpreviouspc(), data & 0x3f);
			bankaddress = 0x2000;
		}
		cpu_setbank(3, new UBytePtr(slave_base, bankaddress));
	
		if (LOG_BANKSWITCHING_S != 0) logerror("%04X:Slave bank = %02X (%05X)n", cpu_getpreviouspc(), data, bankaddress);
	} };
	
	
	
	/*************************************
	 *
	 *	Slave CPU I/O
	 *
	 *************************************/
	
	public static ReadHandlerPtr raster_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int scanline = cpu_getscanline();
		return (scanline < 255) ? scanline : 255;
	} };
	
	
	
	/*************************************
	 *
	 *	Master CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress master_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x1fff, MRA_ROM ),
		new Memory_ReadAddress( 0x2000, 0x9fff, MRA_BANK1 ),
		new Memory_ReadAddress( 0xa000, 0xdfff, MRA_BANK2 ),
		new Memory_ReadAddress( 0xe000, 0xf7ff, MRA_RAM ),
		new Memory_ReadAddress( 0xf800, 0xffff, paletteram_and_misc_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress master_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x9fff, MWA_ROM ),
		new Memory_WriteAddress( 0xa000, 0xdfff, battery_ram_w ),
		new Memory_WriteAddress( 0xe000, 0xf7ff, MWA_RAM ),
		new Memory_WriteAddress( 0xf800, 0xffff, paletteram_and_misc_w, paletteram ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	public static IO_ReadPort master_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),    new IO_ReadPort( 0x04, 0x04, leland_i86_response_r ),
	    new IO_ReadPort( 0x20, 0x20, eeprom_r ),
	    new IO_ReadPort( 0xd0, 0xef, ataxx_mvram_port_r ),
	    new IO_ReadPort( 0xf0, 0xff, master_input_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	public static IO_WritePort master_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),    new IO_WritePort( 0x05, 0x05, leland_i86_command_hi_w ),
	    new IO_WritePort( 0x06, 0x06, leland_i86_command_lo_w ),
	    new IO_WritePort( 0x0c, 0x0c, ataxx_i86_control_w ),
	    new IO_WritePort( 0x20, 0x20, eeprom_w ),
	    new IO_WritePort( 0xd0, 0xef, ataxx_mvram_port_w ),
	    new IO_WritePort( 0xf0, 0xff, master_output_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	
	/*************************************
	 *
	 *	Slave CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress slave_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x1fff, MRA_ROM ),
		new Memory_ReadAddress( 0x2000, 0x9fff, MRA_BANK3 ),
		new Memory_ReadAddress( 0xa000, 0xdfff, MRA_ROM ),
		new Memory_ReadAddress( 0xe000, 0xefff, MRA_RAM ),
		new Memory_ReadAddress( 0xfffe, 0xfffe, raster_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress slave_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0xdfff, MWA_ROM ),
		new Memory_WriteAddress( 0xe000, 0xefff, MWA_RAM ),
		new Memory_WriteAddress( 0xfffc, 0xfffd, leland_slave_video_addr_w ),
		new Memory_WriteAddress( 0xffff, 0xffff, slave_banksw_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	public static IO_ReadPort slave_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_ReadPort( 0x60, 0x7f, ataxx_svram_port_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	public static IO_WritePort slave_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_WritePort( 0x60, 0x7f, ataxx_svram_port_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	/*************************************
	 *
	 *	Port definitions
	 *
	 *************************************/
	
	static InputPortPtr input_ports_ataxx = new InputPortPtr(){ public void handler() { 
	PORT_START(); 		/* 0xF6 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );/* huh? affects trackball movement */
	    PORT_SERVICE_NO_TOGGLE( 0x08, IP_ACTIVE_LOW );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START1 );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_START(); 		/* 0xF7 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_SLAVEHALT );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_VBLANK );	PORT_BIT( 0xfc, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_START(); 		/* 0x20 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_EEPROM_DATA );	PORT_BIT( 0xfe, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_START(); 		/* 0x00 - analog X */
		PORT_ANALOG( 0xff, 0x00, IPT_TRACKBALL_X | IPF_PLAYER1, 100, 10, 0, 255 );	PORT_START(); 		/* 0x01 - analog Y */
		PORT_ANALOG( 0xff, 0x00, IPT_TRACKBALL_Y | IPF_PLAYER1, 100, 10, 0, 255 );	PORT_START(); 		/* 0x02 - analog X */
		PORT_ANALOG( 0xff, 0x00, IPT_TRACKBALL_X | IPF_PLAYER2, 100, 10, 0, 255 );	PORT_START(); 		/* 0x03 - analog Y */
		PORT_ANALOG( 0xff, 0x00, IPT_TRACKBALL_Y | IPF_PLAYER2, 100, 10, 0, 255 );INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_wsf = new InputPortPtr(){ public void handler() { 
	PORT_START(); 		/* 0xF6 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );    PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );    PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN3 );    PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_COIN4 );    PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );    PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );    PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER3 );    PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER4 );
		PORT_START(); 		/* 0xF7 */
	    PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_SLAVEHALT );    PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_VBLANK );    PORT_BIT( 0xfc, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_START(); 		/* 0x20 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_EEPROM_DATA );	
                PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNUSED );    
                PORT_SERVICE_NO_TOGGLE( 0x04, IP_ACTIVE_LOW );
		PORT_BIT( 0xf8, IP_ACTIVE_LOW, IPT_UNUSED );
	    PORT_START(); 		/* 0x0D */
	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );    PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );    PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );    PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );    PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );    PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );    PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );    PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
	    PORT_START(); 		/* 0x0E */
	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER4 );    PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER4 );    PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER4 );    PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER4 );    PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER3 );    PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER3 );    PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER3 );    PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER3 );
	    PORT_START(); 		/* 0x0F */
	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );    PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_START2 );    PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );    PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );    PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER3 );    PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START3 );    PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER4 );    PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START4 );INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_indyheat = new InputPortPtr(){ public void handler() { 
	PORT_START(); 		/* 0xF6 */
	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );	PORT_BIT_IMPULSE( 0x02, IP_ACTIVE_HIGH, IPT_COIN1, 1 );    PORT_BIT_IMPULSE( 0x04, IP_ACTIVE_HIGH, IPT_COIN2, 1 );    PORT_BIT_IMPULSE( 0x08, IP_ACTIVE_HIGH, IPT_COIN3, 1 );	PORT_BIT( 0x70, IP_ACTIVE_LOW, IPT_UNUSED );    PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_START(); 		/* 0xF7 */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_SLAVEHALT );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_VBLANK );    PORT_BIT( 0xfc, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_START(); 		/* 0x20 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_EEPROM_DATA );	PORT_BIT( 0xfe, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_START();       /* Analog wheel 1 */
		PORT_ANALOG( 0xff, 0x80, IPT_DIAL | IPF_PLAYER1, 100, 10, 0, 255 );	PORT_START();       /* Analog wheel 2 */
		PORT_ANALOG( 0xff, 0x80, IPT_DIAL | IPF_PLAYER2, 100, 10, 0, 255 );	PORT_START();       /* Analog wheel 3 */
		PORT_ANALOG( 0xff, 0x80, IPT_DIAL | IPF_PLAYER3, 100, 10, 0, 255 );	PORT_START();       /* Analog pedal 1 */
		PORT_ANALOG( 0xff, 0x00, IPT_PEDAL | IPF_PLAYER1, 100, 10, 0, 255 );	PORT_START();       /* Analog pedal 2 */
		PORT_ANALOG( 0xff, 0x00, IPT_PEDAL | IPF_PLAYER2, 100, 10, 0, 255 );	PORT_START();       /* Analog pedal 3 */
		PORT_ANALOG( 0xff, 0x00, IPT_PEDAL | IPF_PLAYER3, 100, 10, 0, 255 );
	    PORT_START(); 		/* 0x0D */
	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );	PORT_BIT( 0xfe, IP_ACTIVE_LOW, IPT_UNUSED );
	    PORT_START(); 		/* 0x0E */
	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );	PORT_BIT( 0xfe, IP_ACTIVE_LOW, IPT_UNUSED );
	    PORT_START(); 		/* 0x0F */
	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER3 );	
            PORT_BIT( 0x7e, IP_ACTIVE_LOW, IPT_UNUSED );    
            PORT_SERVICE_NO_TOGGLE( 0x80, IP_ACTIVE_LOW );
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_brutforc = new InputPortPtr(){ public void handler() { 
	PORT_START(); 		/* 0xF6 */
	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );	
            PORT_BIT_IMPULSE( 0x02, IP_ACTIVE_HIGH, IPT_COIN2, 1 );    
            PORT_BIT_IMPULSE( 0x04, IP_ACTIVE_HIGH, IPT_COIN1, 1 );    
            PORT_BIT_IMPULSE( 0x08, IP_ACTIVE_HIGH, IPT_COIN3, 1 );    
            PORT_BIT( 0x70, IP_ACTIVE_LOW, IPT_UNUSED );    
            PORT_SERVICE_NO_TOGGLE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START(); 		/* 0xF7 */
	    PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_SLAVEHALT );    PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_VBLANK );    PORT_BIT( 0xfc, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_START(); 		/* 0x20 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_EEPROM_DATA );	PORT_BIT( 0xfe, IP_ACTIVE_LOW, IPT_UNUSED );
	    PORT_START(); 		/* 0x0D */
	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );    PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );    PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );    PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );    PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );    PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );    PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );    PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	    PORT_START(); 		/* 0x0E */
	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );    PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );    PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );    PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );    PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );    PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );    PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START1 );    PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	    PORT_START(); 		/* 0x0F */
	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER3 );    PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER3 );    PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER3 );    PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER3 );    PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );    PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );    PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START3 );    PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );INPUT_PORTS_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	Graphics definitions
	 *
	 *************************************/
	
	static GfxLayout bklayout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,6),
		6,
		new int[] { RGN_FRAC(5,6), RGN_FRAC(4,6), RGN_FRAC(3,6), RGN_FRAC(2,6), RGN_FRAC(1,6), RGN_FRAC(0,6) },
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		8*8
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, bklayout, 0, 1 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	/*************************************
	 *
	 *	Sound definitions
	 *
	 *************************************/
	
	static YM2151interface ym2151_interface = new YM2151interface
	(
		1,
		4000000,
		new int[]{ YM3012_VOL(40,MIXER_PAN_LEFT,40,MIXER_PAN_RIGHT) },
		new WriteYmHandlerPtr[]{ null }
	);
	
	static CustomSound_interface i186_custom_interface = new CustomSound_interface
	(
	    leland_i186_sh_start, null, null
	);
	
	
	
	/*************************************
	 *
	 *	Machine driver
	 *
	 *************************************/
	
	static MachineDriver machine_driver_ataxx = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				6000000,
				master_readmem,master_writemem,
				master_readport,master_writeport,
				ignore_interrupt,1
			),
			new MachineCPU(
				CPU_Z80,
				6000000,
				slave_readmem,slave_writemem,
				slave_readport,slave_writeport,
		    	ignore_interrupt,1
			/*TODO*///),
			/*TODO*///new MachineCPU(
		    	/*TODO*///CPU_I186 | CPU_AUDIO_CPU,
			/*TODO*///	16000000/2,
			/*TODO*///	leland_i86_readmem,leland_i86_writemem,
			/*TODO*///	leland_i86_readport,ataxx_i86_writeport,
			/*TODO*///	ignore_interrupt,1
			)
		},
		60, (1000000*16)/(256*60),
		1,
		init_machine,
	
		/* video hardware */
		40*8, 30*8, new rectangle( 0*8, 40*8-1, 0*8, 30*8-1 ),
		gfxdecodeinfo,
		1024, 0,
		null,
	
		VIDEO_TYPE_RASTER,
		leland_vh_eof,
		ataxx_vh_start,
		ataxx_vh_stop,
		ataxx_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound( SOUND_CUSTOM, i186_custom_interface ),
		},
		nvram_handler
	);
	
	
	static MachineDriver machine_driver_wsf = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				6000000,
				master_readmem,master_writemem,
				master_readport,master_writeport,
				ignore_interrupt,1
			),
			new MachineCPU(
				CPU_Z80,
				6000000,
				slave_readmem,slave_writemem,
				slave_readport,slave_writeport,
		    	ignore_interrupt,1
			/*TODO*///),
			/*TODO*///new MachineCPU(
		    	/*TODO*///CPU_I186 | CPU_AUDIO_CPU,
			/*TODO*///	16000000/2,
			/*TODO*///	leland_i86_readmem,leland_i86_writemem,
			/*TODO*///	leland_i86_readport,ataxx_i86_writeport,
			/*TODO*///	ignore_interrupt,1
			)
		},
		60, (1000000*16)/(256*60),
		1,
		init_machine,
	
		/* video hardware */
		40*8, 30*8, new rectangle( 0*8, 40*8-1, 0*8, 30*8-1 ),
		gfxdecodeinfo,
		1024, 0,
		null,
	
		VIDEO_TYPE_RASTER,
		leland_vh_eof,
		ataxx_vh_start,
		ataxx_vh_stop,
		ataxx_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound( SOUND_CUSTOM, i186_custom_interface ),
			new MachineSound( SOUND_YM2151, ym2151_interface ),
		},
		nvram_handler
	);
	
	
	
	/*************************************
	 *
	 *	ROM definitions
	 *
	 *************************************/
	
	static RomLoadPtr rom_ataxx = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x30000, REGION_CPU1, 0 );	ROM_LOAD( "ataxx.038",   0x00000, 0x20000, 0x0e1cf6236 );	ROM_RELOAD(              0x10000, 0x20000 );
		ROM_REGION( 0x60000, REGION_CPU2, 0 );	ROM_LOAD( "ataxx.111",  0x00000, 0x20000, 0x09a3297cc );	ROM_LOAD( "ataxx.112",  0x20000, 0x20000, 0x07e7c3e2f );	ROM_LOAD( "ataxx.113",  0x40000, 0x20000, 0x08cf3e101 );
		ROM_REGION( 0x100000, REGION_CPU3, 0 );	ROM_LOAD16_BYTE( "ataxx.015",  0x20001, 0x20000, 0x08bb3233b );	ROM_LOAD16_BYTE( "ataxx.001",  0x20000, 0x20000, 0x0728d75f2 );	ROM_LOAD16_BYTE( "ataxx.016",  0x60001, 0x20000, 0x0f2bdff48 );	ROM_RELOAD(                    0xc0001, 0x20000 );	ROM_LOAD16_BYTE( "ataxx.002",  0x60000, 0x20000, 0x0ca06a394 );	ROM_RELOAD(                    0xc0000, 0x20000 );
		ROM_REGION( 0xc0000, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "ataxx.098",  0x00000, 0x20000, 0x059d0f2ae );	ROM_LOAD( "ataxx.099",  0x20000, 0x20000, 0x06ab7db25 );	ROM_LOAD( "ataxx.100",  0x40000, 0x20000, 0x02352849e );	ROM_LOAD( "ataxx.101",  0x60000, 0x20000, 0x04c31e02b );	ROM_LOAD( "ataxx.102",  0x80000, 0x20000, 0x0a951228c );	ROM_LOAD( "ataxx.103",  0xa0000, 0x20000, 0x0ed326164 );
		ROM_REGION( 0x00001, REGION_USER1, 0 );/* X-ROM (data used by main processor) */
	    /* Empty / not used */
	
		ROM_REGION( battery_ram_size + extra_tram_size, REGION_USER2, 0 );/* extra RAM regions */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ataxxa = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x30000, REGION_CPU1, 0 );	ROM_LOAD( "u38",   0x00000, 0x20000, 0x3378937d );	ROM_RELOAD(        0x10000, 0x20000 );
		ROM_REGION( 0x60000, REGION_CPU2, 0 );	ROM_LOAD( "ataxx.111",  0x00000, 0x20000, 0x09a3297cc );	ROM_LOAD( "ataxx.112",  0x20000, 0x20000, 0x07e7c3e2f );	ROM_LOAD( "ataxx.113",  0x40000, 0x20000, 0x08cf3e101 );
		ROM_REGION( 0x100000, REGION_CPU3, 0 );	ROM_LOAD16_BYTE( "ataxx.015",  0x20001, 0x20000, 0x08bb3233b );	ROM_LOAD16_BYTE( "ataxx.001",  0x20000, 0x20000, 0x0728d75f2 );	ROM_LOAD16_BYTE( "ataxx.016",  0x60001, 0x20000, 0x0f2bdff48 );	ROM_RELOAD(                    0xc0001, 0x20000 );	ROM_LOAD16_BYTE( "ataxx.002",  0x60000, 0x20000, 0x0ca06a394 );	ROM_RELOAD(                    0xc0000, 0x20000 );
		ROM_REGION( 0xc0000, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "ataxx.098",  0x00000, 0x20000, 0x059d0f2ae );	ROM_LOAD( "ataxx.099",  0x20000, 0x20000, 0x06ab7db25 );	ROM_LOAD( "ataxx.100",  0x40000, 0x20000, 0x02352849e );	ROM_LOAD( "ataxx.101",  0x60000, 0x20000, 0x04c31e02b );	ROM_LOAD( "ataxx.102",  0x80000, 0x20000, 0x0a951228c );	ROM_LOAD( "ataxx.103",  0xa0000, 0x20000, 0x0ed326164 );
		ROM_REGION( 0x00001, REGION_USER1, 0 );/* X-ROM (data used by main processor) */
	    /* Empty / not used */
	
		ROM_REGION( battery_ram_size + extra_tram_size, REGION_USER2, 0 );/* extra RAM regions */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_ataxxj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x30000, REGION_CPU1, 0 );	ROM_LOAD( "ataxxj.038", 0x00000, 0x20000, 0x513fa7d4 );	ROM_RELOAD(             0x10000, 0x20000 );
		ROM_REGION( 0x60000, REGION_CPU2, 0 );	ROM_LOAD( "ataxx.111",  0x00000, 0x20000, 0x09a3297cc );	ROM_LOAD( "ataxx.112",  0x20000, 0x20000, 0x07e7c3e2f );	ROM_LOAD( "ataxx.113",  0x40000, 0x20000, 0x08cf3e101 );
		ROM_REGION( 0x100000, REGION_CPU3, 0 );	ROM_LOAD16_BYTE( "ataxxj.015", 0x20001, 0x20000, 0xdb266d3f );	ROM_LOAD16_BYTE( "ataxxj.001", 0x20000, 0x20000, 0xd6db2724 );	ROM_LOAD16_BYTE( "ataxxj.016", 0x60001, 0x20000, 0x2b127f56 );	ROM_RELOAD(                    0xc0001, 0x20000 );	ROM_LOAD16_BYTE( "ataxxj.002", 0x60000, 0x20000, 0x1b63b882 );	ROM_RELOAD(                    0xc0000, 0x20000 );
		ROM_REGION( 0xc0000, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "ataxx.098",  0x00000, 0x20000, 0x059d0f2ae );	ROM_LOAD( "ataxx.099",  0x20000, 0x20000, 0x06ab7db25 );	ROM_LOAD( "ataxx.100",  0x40000, 0x20000, 0x02352849e );	ROM_LOAD( "ataxx.101",  0x60000, 0x20000, 0x04c31e02b );	ROM_LOAD( "ataxx.102",  0x80000, 0x20000, 0x0a951228c );	ROM_LOAD( "ataxx.103",  0xa0000, 0x20000, 0x0ed326164 );
		ROM_REGION( 0x00001, REGION_USER1, 0 );/* X-ROM (data used by main processor) */
	    /* Empty / not used */
	
		ROM_REGION( battery_ram_size + extra_tram_size, REGION_USER2, 0 );/* extra RAM regions */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_wsf = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x50000, REGION_CPU1, 0 );	ROM_LOAD( "30022-03.u64",  0x00000, 0x20000, 0x2e7faa96 );	ROM_RELOAD(                0x10000, 0x20000 );	ROM_LOAD( "30023-03.u65",  0x30000, 0x20000, 0x7146328f );
		ROM_REGION( 0x100000, REGION_CPU2, 0 );	ROM_LOAD( "30001-01.151",  0x00000, 0x20000, 0x31c63af5 );	ROM_LOAD( "30002-01.152",  0x20000, 0x20000, 0xa53e88a6 );	ROM_LOAD( "30003-01.153",  0x40000, 0x20000, 0x12afad1d );	ROM_LOAD( "30004-01.154",  0x60000, 0x20000, 0xb8b3d59c );	ROM_LOAD( "30005-01.155",  0x80000, 0x20000, 0x505724b9 );	ROM_LOAD( "30006-01.156",  0xa0000, 0x20000, 0xc86b5c4d );	ROM_LOAD( "30007-01.157",  0xc0000, 0x20000, 0x451321ae );	ROM_LOAD( "30008-01.158",  0xe0000, 0x20000, 0x4d23836f );
		ROM_REGION( 0x100000, REGION_CPU3, 0 );	ROM_LOAD16_BYTE( "30017-01.u3",  0x20001, 0x20000, 0x39ec13c1 );	ROM_LOAD16_BYTE( "30020-01.u6",  0x20000, 0x20000, 0x532c02bf );	ROM_LOAD16_BYTE( "30018-01.u4",  0x60001, 0x20000, 0x1ec16735 );	ROM_RELOAD(                      0xc0001, 0x20000 );	ROM_LOAD16_BYTE( "30019-01.u5",  0x60000, 0x20000, 0x2881f73b );	ROM_RELOAD(                      0xc0000, 0x20000 );
		ROM_REGION( 0x60000, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "30011-02.145",  0x00000, 0x10000, 0x6153569b );	ROM_LOAD( "30012-02.146",  0x10000, 0x10000, 0x52d65e21 );	ROM_LOAD( "30013-02.147",  0x20000, 0x10000, 0xb3afda12 );	ROM_LOAD( "30014-02.148",  0x30000, 0x10000, 0x624e6c64 );	ROM_LOAD( "30015-01.149",  0x40000, 0x10000, 0x5d9064f2 );	ROM_LOAD( "30016-01.150",  0x50000, 0x10000, 0xd76389cd );
		ROM_REGION( 0x20000, REGION_USER1, 0 );/* X-ROM (data used by main processor) */
		ROM_LOAD( "30009-01.u68",  0x00000, 0x10000, 0xf2fbfc15 );	ROM_LOAD( "30010-01.u69",  0x10000, 0x10000, 0xb4ed2d3b );
		ROM_REGION( 0x20000, REGION_SOUND1, 0 );/* externally clocked DAC data */
		ROM_LOAD( "30021-01.u8",   0x00000, 0x20000, 0xbb91dc10 );
		ROM_REGION( battery_ram_size + extra_tram_size, REGION_USER2, 0 );/* extra RAM regions */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_indyheat = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x90000, REGION_CPU1, 0 );	ROM_LOAD( "u64_27c.010",   0x00000, 0x20000, 0x2b97a347 );	ROM_RELOAD(                0x10000, 0x20000 );	ROM_LOAD( "u65_27c.010",   0x30000, 0x20000, 0x71301d74 );	ROM_LOAD( "u66_27c.010",   0x50000, 0x20000, 0xc9612072 );	ROM_LOAD( "u67_27c.010",   0x70000, 0x20000, 0x4c4b25e0 );
		ROM_REGION( 0x160000, REGION_CPU2, 0 );	ROM_LOAD( "u151_27c.010",  0x00000, 0x20000, 0x2622dfa4 );	ROM_LOAD( "u152_27c.020",  0x20000, 0x20000, 0xad40e4e2 );	ROM_CONTINUE(             0x120000, 0x20000 );	ROM_LOAD( "u153_27c.020",  0x40000, 0x20000, 0x1e3803f7 );	ROM_CONTINUE(             0x140000, 0x20000 );	ROM_LOAD( "u154_27c.010",  0x60000, 0x20000, 0x76d3c235 );	ROM_LOAD( "u155_27c.010",  0x80000, 0x20000, 0xd5d866b3 );	ROM_LOAD( "u156_27c.010",  0xa0000, 0x20000, 0x7fe71842 );	ROM_LOAD( "u157_27c.010",  0xc0000, 0x20000, 0xa6462adc );	ROM_LOAD( "u158_27c.010",  0xe0000, 0x20000, 0xd6ef27a3 );
		ROM_REGION( 0x100000, REGION_CPU3, 0 );	ROM_LOAD16_BYTE( "u3_27c.010",  0x20001, 0x20000, 0x97413818 );	ROM_LOAD16_BYTE( "u6_27c.010",  0x20000, 0x20000, 0x15a89962 );	ROM_LOAD16_BYTE( "u4_27c.010",  0x60001, 0x20000, 0xfa7bfa04 );	ROM_RELOAD(                     0xc0001, 0x20000 );	ROM_LOAD16_BYTE( "u5_27c.010",  0x60000, 0x20000, 0x198285d4 );	ROM_RELOAD(                     0xc0000, 0x20000 );
		ROM_REGION( 0xc0000, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "u145_27c.010",  0x00000, 0x20000, 0x612d4bf8 );	ROM_LOAD( "u146_27c.010",  0x20000, 0x20000, 0x77a725f6 );	ROM_LOAD( "u147_27c.010",  0x40000, 0x20000, 0xd6aac372 );	ROM_LOAD( "u148_27c.010",  0x60000, 0x20000, 0x5d19723e );	ROM_LOAD( "u149_27c.010",  0x80000, 0x20000, 0x29056791 );	ROM_LOAD( "u150_27c.010",  0xa0000, 0x20000, 0xcb73dd6a );
		ROM_REGION( 0x40000, REGION_USER1, 0 );/* X-ROM (data used by main processor) */
		ROM_LOAD( "u68_27c.010",   0x00000, 0x10000, 0x9e88efb3 );	ROM_CONTINUE(              0x20000, 0x10000 );	ROM_LOAD( "u69_27c.010",   0x10000, 0x10000, 0xaa39fcb3 );	ROM_CONTINUE(              0x30000, 0x10000 );
		ROM_REGION( 0x40000, REGION_SOUND1, 0 );/* externally clocked DAC data */
		ROM_LOAD( "u8_27c.010",  0x00000, 0x20000, 0x9f16e5b6 );	ROM_LOAD( "u9_27c.010",  0x20000, 0x20000, 0x0dc8f488 );
		ROM_REGION( battery_ram_size + extra_tram_size, REGION_USER2, 0 );/* extra RAM regions */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_brutforc = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x90000, REGION_CPU1, 0 );	ROM_LOAD( "u64",   0x00000, 0x20000, 0x008ae3b8 );	ROM_RELOAD(                 0x10000, 0x20000 );	ROM_LOAD( "u65",   0x30000, 0x20000, 0x6036e3fa );	ROM_LOAD( "u66",   0x50000, 0x20000, 0x7ebf0795 );	ROM_LOAD( "u67",   0x70000, 0x20000, 0xe3cbf8b4 );
		ROM_REGION( 0x100000, REGION_CPU2, 0 );	ROM_LOAD( "u151",  0x00000, 0x20000, 0xbd3b677b );	ROM_LOAD( "u152",  0x20000, 0x20000, 0x5f4434e7 );	ROM_LOAD( "u153",  0x40000, 0x20000, 0x20f7df53 );	ROM_LOAD( "u154",  0x60000, 0x20000, 0x69ce2329 );	ROM_LOAD( "u155",  0x80000, 0x20000, 0x33d92e25 );	ROM_LOAD( "u156",  0xa0000, 0x20000, 0xde7eca8b );	ROM_LOAD( "u157",  0xc0000, 0x20000, 0xe42b3dba );	ROM_LOAD( "u158",  0xe0000, 0x20000, 0xa0aa3220 );
		ROM_REGION( 0x100000, REGION_CPU3, 0 );	ROM_LOAD16_BYTE( "u3",  0x20001, 0x20000, 0x9984906c );	ROM_LOAD16_BYTE( "u6",  0x20000, 0x20000, 0xc9c5a413 );	ROM_LOAD16_BYTE( "u4",  0x60001, 0x20000, 0xca8ab3a6 );	ROM_RELOAD(             0xc0001, 0x20000 );	ROM_LOAD16_BYTE( "u5",  0x60000, 0x20000, 0xcbdb914b );	ROM_RELOAD(             0xc0000, 0x20000 );
		ROM_REGION( 0x180000, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "u145",  0x000000, 0x40000, 0xc3d20d24 );	ROM_LOAD( "u146",  0x040000, 0x40000, 0x43e9dd87 );	ROM_LOAD( "u147",  0x080000, 0x40000, 0xfb855ce8 );	ROM_LOAD( "u148",  0x0c0000, 0x40000, 0xe4b54eae );	ROM_LOAD( "u149",  0x100000, 0x40000, 0xcf48401c );	ROM_LOAD( "u150",  0x140000, 0x40000, 0xca9e1e33 );
		ROM_REGION( 0x40000, REGION_USER1, 0 );/* X-ROM (data used by main processor) */
		ROM_LOAD( "u68",   0x00000, 0x10000, 0x77c8de62 );	ROM_CONTINUE(      0x20000, 0x10000 );	ROM_LOAD( "u69",   0x10000, 0x10000, 0x113aa6d5 );	ROM_CONTINUE(      0x30000, 0x10000 );
		ROM_REGION( 0x80000, REGION_SOUND1, 0 );/* externally clocked DAC data */
		ROM_LOAD( "u8",  0x00000, 0x20000, 0x1e0ead72 );	ROM_LOAD( "u9",  0x20000, 0x20000, 0x3195b305 );	ROM_LOAD( "u10", 0x40000, 0x20000, 0x1dc5f375 );	ROM_LOAD( "u11", 0x60000, 0x20000, 0x5ed4877f );
		ROM_REGION( battery_ram_size + extra_tram_size, REGION_USER2, 0 );/* extra RAM regions */
	ROM_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	Driver initialization
	 *
	 *************************************/
	
	
	public static InitDriverPtr init_ataxx = new InitDriverPtr() { public void handler()
	{
		/* initialize the default EEPROM state */
		char ataxx_eeprom_data[] =
		{
			0x09,0x0101,
			0x0a,0x0104,
			0x0b,0x0401,
			0x0c,0x0101,
			0x0d,0x0004,
			0x13,0x0100,
			0x14,0x5a04,
			0xffff
		};
		init_eeprom(0x00, new UBytePtr(ataxx_eeprom_data), 0x00);
	
		leland_rotate_memory(0);
		leland_rotate_memory(1);
	
		/* set up additional input ports */
		install_port_read_handler(0, 0x00, 0x03, ataxx_trackball_r);
	
		/* optimize the sound */
		leland_i86_optimize_address(0x612);
	} };
	
	public static InitDriverPtr init_ataxxj = new InitDriverPtr() { public void handler()
	{
		/* initialize the default EEPROM state */
		char ataxxj_eeprom_data[] =
		{
			0x09,0x0101,
			0x0a,0x0104,
			0x0b,0x0001,
			0x0c,0x0101,
			0x13,0xff00,
			0x3f,0x3c0c,
			0xffff
		};
		init_eeprom(0x00, new UBytePtr(ataxxj_eeprom_data), 0x00);
	
		leland_rotate_memory(0);
		leland_rotate_memory(1);
	
		/* set up additional input ports */
		install_port_read_handler(0, 0x00, 0x03, ataxx_trackball_r);
	
		/* optimize the sound */
		leland_i86_optimize_address(0x612);
	} };
	
	public static InitDriverPtr init_wsf = new InitDriverPtr() { public void handler()
	{
		/* initialize the default EEPROM state */
		char wsf_eeprom_data[] =
		{
			0x04,0x0101,
			0x0b,0x04ff,
			0x0d,0x0500,
			0x26,0x26ac,
			0x27,0xff0a,
			0x28,0xff00,
			0xffff
		};
		init_eeprom(0x00, new UBytePtr(wsf_eeprom_data), 0x00);
	
		leland_rotate_memory(0);
		leland_rotate_memory(1);
	
		/* set up additional input ports */
		install_port_read_handler(0, 0x0d, 0x0d, input_port_3_r);
		install_port_read_handler(0, 0x0e, 0x0e, input_port_4_r);
		install_port_read_handler(0, 0x0f, 0x0f, input_port_5_r);
	
		/* optimize the sound */
		leland_i86_optimize_address(0x612);
	} };
	
	public static InitDriverPtr init_indyheat = new InitDriverPtr() { public void handler()
	{
		/* initialize the default EEPROM state */
		char indyheat_eeprom_data[] =
		{
			0x2c,0x0100,
			0x2d,0x0401,
			0x2e,0x05ff,
			0x2f,0x4b4b,
			0x30,0xfa4b,
			0x31,0xfafa,
			0xffff
		};
		init_eeprom(0x00, new UBytePtr(indyheat_eeprom_data), 0x00);
	
		leland_rotate_memory(0);
		leland_rotate_memory(1);
	
		/* set up additional input ports */
		install_port_read_handler(0, 0x00, 0x02, indyheat_wheel_r);
		install_port_read_handler(0, 0x08, 0x0b, indyheat_analog_r);
		install_port_read_handler(0, 0x0d, 0x0d, input_port_9_r);
		install_port_read_handler(0, 0x0e, 0x0e, input_port_10_r);
		install_port_read_handler(0, 0x0f, 0x0f, input_port_11_r);
	
		/* set up additional output ports */
		install_port_write_handler(0, 0x08, 0x0b, indyheat_analog_w);
	
		/* optimize the sound */
		leland_i86_optimize_address(0x613);
	} };
	
	public static InitDriverPtr init_brutforc = new InitDriverPtr() { public void handler()
	{
		/* initialize the default EEPROM state */
		char brutforc_eeprom_data[] =
		{
			0x27,0x0303,
			0x28,0x0003,
			0x30,0x01ff,
			0x31,0x0100,
			0x35,0x0404,
			0x36,0x0104,
			0xffff
		};
		init_eeprom(0x00, new UBytePtr(brutforc_eeprom_data), 0x00);
	
		leland_rotate_memory(0);
		leland_rotate_memory(1);
	
		/* set up additional input ports */
		install_port_read_handler(0, 0x0d, 0x0d, input_port_3_r);
		install_port_read_handler(0, 0x0e, 0x0e, input_port_4_r);
		install_port_read_handler(0, 0x0f, 0x0f, input_port_5_r);
	
		/* optimize the sound */
		leland_i86_optimize_address(0x613);
	} };
	
	
	
	/*************************************
	 *
	 *	Game drivers
	 *
	 *************************************/
	
	public static GameDriver driver_ataxx	   = new GameDriver("1990"	,"ataxx"	,"ataxx.java"	,rom_ataxx,null	,machine_driver_ataxx	,input_ports_ataxx	,init_ataxx	,ROT0	,	"Leland Corp.", "Ataxx (set 1)" );
	public static GameDriver driver_ataxxa	   = new GameDriver("1990"	,"ataxxa"	,"ataxx.java"	,rom_ataxxa,driver_ataxx	,machine_driver_ataxx	,input_ports_ataxx	,init_ataxx	,ROT0	,	"Leland Corp.", "Ataxx (set 2)" );
	public static GameDriver driver_ataxxj	   = new GameDriver("1990"	,"ataxxj"	,"ataxx.java"	,rom_ataxxj,driver_ataxx	,machine_driver_ataxx	,input_ports_ataxx	,init_ataxxj	,ROT0	,	"Leland Corp.", "Ataxx (Japan)" );
	public static GameDriver driver_wsf	   = new GameDriver("1990"	,"wsf"	,"ataxx.java"	,rom_wsf,null	,machine_driver_wsf	,input_ports_wsf	,init_wsf	,ROT0	,	"Leland Corp.", "World Soccer Finals" );
	public static GameDriver driver_indyheat	   = new GameDriver("1991"	,"indyheat"	,"ataxx.java"	,rom_indyheat,null	,machine_driver_wsf	,input_ports_indyheat	,init_indyheat	,ROT0	,	"Leland Corp.", "Danny Sullivan's Indy Heat" );
	public static GameDriver driver_brutforc	   = new GameDriver("1991"	,"brutforc"	,"ataxx.java"	,rom_brutforc,null	,machine_driver_wsf	,input_ports_brutforc	,init_brutforc	,ROT0	,	"Leland Corp.", "Brute Force" );
}
