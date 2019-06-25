/***************************************************************************

						-= Dynax / Nakanihon Games =-

					driver by	Luca Elia (l.elia@tin.it)


---------------------------------------------------------------------------------------------------
Year + Game				CPU		Sound			Gfx							Misc
---------------------------------------------------------------------------------------------------
89 Sports Match			Z80 	YM2203			Color PROM + 6845 + DYNAX?
94 Rong Rong			Z80		YM2413 + M6295	NAKANIHON NL-002
95 Don Den Lover Vol 1	68000	YM2413 + M6295	NAKANIHON NL-005			NVRAM + RTC 72421B 4382
---------------------------------------------------------------------------------------------------


***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static WIP.mame056.vidhrdw.dynax.*;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.libc.cstring.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.inptportH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.memoryH.*;
import static mame056.inptport.*;
import static mame056.drawgfxH.*;
import static mame056.inputH.*;
import static mame056.sndintrfH.*;
import static mame056.sndintrf.*;
import static mame056.sound.nes_apu.*;
import static mame056.sound.nes_apuH.*;
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;
import static mame056.sound.mixer.*;
import static mame056.sound.mixerH.*;
import static mame056.sound._2203intf.*;
import static mame056.sound._2203intfH.*;
import static mame056.sound.oki6295.*;
import static mame056.sound.oki6295H.*;
import static mame056.inptport.*;
import static common.libc.cstdlib.*;

import static mame056.vidhrdw.generic.*;
// refactor
import static arcadeflex036.osdepend.logerror;
import static arcadeflex056.fileio.*;
import static mame056.palette.*;

public class dynax
{
	
	
	/***************************************************************************
	
	
									Interrupts
	
	
	***************************************************************************/
	
	/***************************************************************************
									Sports Match
	***************************************************************************/
	
	public static int dynax_blitter_irq;
	public static int dynax_sound_irq;
	public static int dynax_vblank_irq;
	
	/* It runs in IM 0, thus needs an opcode on the data bus */
	public static void sprtmtch_update_irq()
	{
		int irq	=	((dynax_sound_irq)!=0   ? 0x08 : 0) |
					((dynax_vblank_irq)!=0  ? 0x10 : 0) |
					((dynax_blitter_irq)!=0 ? 0x20 : 0) ;
	
		cpu_irq_line_vector_w(0,0,0xc7 | irq);	/* rst $xx */
		cpu_set_irq_line(0, 0, irq!=0 ? ASSERT_LINE : CLEAR_LINE );
	}
	
	public static WriteHandlerPtr sprtmtch_vblank_ack_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dynax_vblank_irq = 0;
		sprtmtch_update_irq();
	} };
	
	public static WriteHandlerPtr sprtmtch_blitter_ack_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		dynax_blitter_irq = 0;
		sprtmtch_update_irq();
	} };
	
	public static InterruptPtr sprtmtch_vblank_interrupt = new InterruptPtr() { public int handler() 
	{
		dynax_vblank_irq = 1;
		sprtmtch_update_irq();
		return ignore_interrupt.handler();
	} };
	
	public static WriteYmHandlerPtr sprtmtch_sound_callback = new WriteYmHandlerPtr() {
            public void handler(int state) {
                dynax_sound_irq = state;
		sprtmtch_update_irq();
            }
        };
	
	/***************************************************************************
	
	
									Memory Maps
	
	
	***************************************************************************/
	
	/***************************************************************************
									Sports Match
	***************************************************************************/
	
	public static WriteHandlerPtr sprtmtch_coincounter_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		coin_counter_w.handler(0, data & 1);
		if ((data & ~1) != 0)
			logerror("CPU#0 PC %06X: Warning, coin counter 0 <- %02X\n", cpu_get_pc(), data);
	} };
	public static WriteHandlerPtr sprtmtch_coincounter_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		coin_counter_w.handler(1, data & 1);
		if ((data & ~1) != 0)
			logerror("CPU#0 PC %06X: Warning, coin counter 1 <- %02X\n", cpu_get_pc(), data);
	} };
	
	public static ReadHandlerPtr ret_ff  = new ReadHandlerPtr() { public int handler(int offset)	{	return 0xff;	} };
	
	public static Memory_ReadAddress sprtmtch_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x6fff, MRA_ROM					),	// ROM
		new Memory_ReadAddress( 0x7000, 0x7fff, MRA_RAM					),	// RAM
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM					),	// ROM
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sprtmtch_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x6fff, MWA_ROM					),	// ROM
		new Memory_WriteAddress( 0x7000, 0x7fff, MWA_RAM					),	// RAM
		new Memory_WriteAddress( 0x8000, 0xffff, MWA_ROM					),	// ROM
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static IO_ReadPort sprtmtch_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x10, 0x10, YM2203_status_port_0_r	),	// YM2203
		new IO_ReadPort( 0x11, 0x11, YM2203_read_port_0_r		),	// 2 x DSW
		new IO_ReadPort( 0x20, 0x20, input_port_0_r			),	// P1
		new IO_ReadPort( 0x21, 0x21, input_port_1_r			),	// P2
		new IO_ReadPort( 0x22, 0x22, input_port_2_r			),	// Coins
		new IO_ReadPort( 0x23, 0x23, ret_ff					),	// ?
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static IO_WritePort sprtmtch_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x01, 0x01, sprtmtch_blit_draw_w		),	// Blitter
		new IO_WritePort( 0x02, 0x02, dynax_blit_x_w			),	// Destination X
		new IO_WritePort( 0x03, 0x03, dynax_blit_y_w			),	// Destination Y
		new IO_WritePort( 0x04, 0x04, dynax_blit_addr0_w		),	// Source Address
		new IO_WritePort( 0x05, 0x05, dynax_blit_addr1_w		),	//
		new IO_WritePort( 0x06, 0x06, dynax_blit_addr2_w		),	//
		new IO_WritePort( 0x07, 0x07, dynax_blit_scroll_w		),	// Layers Scroll X  Y
		new IO_WritePort( 0x10, 0x10, YM2203_control_port_0_w	),	// YM2203
		new IO_WritePort( 0x11, 0x11, YM2203_write_port_0_w		),	//
	//	new IO_WritePort( 0x12, 0x12, IOWP_NOP					),	// ?? CRT Controller ??
	//	new IO_WritePort( 0x13, 0x13, IOWP_NOP					),	// ?? CRT Controller ??
		new IO_WritePort( 0x30, 0x30, dynax_blit_enable_w		),	// Layers Enable
		new IO_WritePort( 0x32, 0x32, dynax_blit_dest_w			),	// Destination Layer
		new IO_WritePort( 0x33, 0x33, dynax_blit_pen_w			),	// Destination Pen
		new IO_WritePort( 0x34, 0x34, dynax_blit_palette01_w	),	// Layers Palettes (Low Bits)
		new IO_WritePort( 0x35, 0x35, dynax_blit_palette2_w		),	//
		new IO_WritePort( 0x36, 0x36, dynax_blit_backpen_w		),	// Background Color
		new IO_WritePort( 0x37, 0x37, sprtmtch_vblank_ack_w		),	// VBlank IRQ Ack
		new IO_WritePort( 0x41, 0x41, dynax_flipscreen_w		),	// Flip Screen
		new IO_WritePort( 0x42, 0x42, sprtmtch_coincounter_0_w	),	// Coin Counters
		new IO_WritePort( 0x43, 0x43, sprtmtch_coincounter_1_w	),	//
		new IO_WritePort( 0x44, 0x44, sprtmtch_blitter_ack_w	),	// Blitter IRQ Ack
		new IO_WritePort( 0x45, 0x45, dynax_blit_palbank_w		),	// Layers Palettes (High Bit)
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	/***************************************************************************
								Don Den Lover Vol.1
	***************************************************************************/
	
	static WriteHandlerPtr ddenlovr_oki_bank_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /*TODO*///if (ACCESSING_LSB)
		/*TODO*///	OKIM6295_set_bank_base(0, (data & 3) * 0x40000);
            }
        };
	
	static ReadHandlerPtr ddenlovr_gfxrom_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                UBytePtr ROM	=	new UBytePtr(memory_region( REGION_GFX1 ));
		int size	=	memory_region_length( REGION_GFX1 );
		int address	=	dynax_blit_address - 0x200000;	// why?
	
		if (address >= size)
		{
			address %= size;
			logerror("CPU#0 PC %06X: Error, Blitter address %06X out of range\n", cpu_get_pc(), address);
		}
	
		dynax_blit_address++;
	
		return ROM.read(address);
            }
        };
	
	static WriteHandlerPtr ddenlovr_blit_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /*TODO*///if (ACCESSING_LSB)
		/*TODO*///{
		/*TODO*///	data &= 0xff;
	//logerror("CPU#0 PC %06X: Blitter %x <- %02X\n", cpu_get_pc(), offset, data);
		/*TODO*///	switch(offset)
		/*TODO*///	{
		/*TODO*///	case 0:
		/*TODO*///		dynax_blit_reg = data;
		/*TODO*///		break;
	/*TODO*///
		/*TODO*///	case 1:
		/*TODO*///		switch(dynax_blit_reg)
		/*TODO*///		{
		/*TODO*///		case 0x00:
		/*TODO*///			//?
		/*TODO*///			break;
		/*TODO*///		case 0x05:
		/*TODO*///			//?
		/*TODO*///			break;
	/*TODO*///
		/*TODO*///		case 0x0d:
		/*TODO*///			dynax_blit_addr0_w(0,data&0xff);
		/*TODO*///			break;
		/*TODO*///		case 0x0e:
		/*TODO*///			dynax_blit_addr1_w(0,data&0xff);
		/*TODO*///			break;
		/*TODO*///		case 0x0f:
		/*TODO*///			dynax_blit_addr2_w(0,data&0xff);
		/*TODO*///			break;
	/*TODO*///
		/*TODO*///		case 0x14:
		/*TODO*///		case 0x54:
		/*TODO*///			dynax_blit_x_w(0,data&0xff);
		/*TODO*///			if (dynax_blit_reg & 0x40)	dynax_blit_x |= 0x100;
		/*TODO*///			break;
	/*TODO*///
		/*TODO*///		case 0x02:
		/*TODO*///		case 0x42:
		/*TODO*///			dynax_blit_y_w(0,data&0xff);
		/*TODO*///			if (dynax_blit_reg & 0x40)	dynax_blit_y |= 0x100;
		/*TODO*///			break;
	/*TODO*///
		/*TODO*///		case 0x24:
		/*TODO*///			drawgfx(	Machine->scrbitmap, Machine->uifont,
		/*TODO*///						dynax_blit_address,
		/*TODO*///						0,
		/*TODO*///						0, 0,
		/*TODO*///						dynax_blit_x, dynax_blit_y,
		/*TODO*///						&Machine->visible_area, TRANSPARENCY_PEN,0x00	);
		/*TODO*///			break;
		/*TODO*///		}
		/*TODO*///	}
		/*TODO*///}
            }
        };
	
	static ReadHandlerPtr ddenlovr_special_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return readinputport(2) | (rand() & 0x00c0);
            }
        };
	
	static WriteHandlerPtr ddenlovr_coincounter_0_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /*TODO*///if (ACCESSING_LSB)
		/*TODO*///	coin_counter_w(0, data & 1);
		/*TODO*///else
		/*TODO*///	logerror("CPU#0 PC %06X: Error, MSB of coin counter 0 written\n", cpu_get_pc());
            }
        };
	
	static WriteHandlerPtr ddenlovr_coincounter_1_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /*TODO*///if (ACCESSING_LSB)
		/*TODO*///	coin_counter_w(1, data & 1);
		/*TODO*///else
		/*TODO*///	logerror("CPU#0 PC %06X: Error, MSB of coin counter 1 written\n", cpu_get_pc());
            }
        };
	
	public static Memory_ReadAddress ddenlovr_readmem[]={
                new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_16),
		new Memory_ReadAddress( 0x000000, 0x07ffff, MRA_ROM					),	// ROM
		new Memory_ReadAddress( 0xff0000, 0xffffff, MRA_RAM					),	// RAM
		new Memory_ReadAddress( 0xe00086, 0xe00087, ddenlovr_gfxrom_r			),	// Video Chip
		new Memory_ReadAddress( 0xe00100, 0xe00101, input_port_0_word_r		),	// P1?
		new Memory_ReadAddress( 0xe00102, 0xe00103, input_port_1_word_r		),	// P2?
		new Memory_ReadAddress( 0xe00104, 0xe00105, ddenlovr_special_r		),	// Coins + ?
		new Memory_ReadAddress( 0xe00200, 0xe00201, input_port_3_word_r		),	// DSW
		new Memory_ReadAddress( 0xe00500, 0xe0051f, MRA_RAM 			),	// NVRAM?
		new Memory_ReadAddress( 0xe00700, 0xe00701, OKIM6295_status_0_lsb_r	),	// Sound
                new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress ddenlovr_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_16),
		new Memory_WriteAddress( 0x000000, 0x07ffff, MWA_ROM						),	// ROM
		new Memory_WriteAddress( 0xff0000, 0xffffff, MWA_RAM						),	// RAM
		new Memory_WriteAddress( 0xe00080, 0xe00083, ddenlovr_blit_w				),	// Video Chip
		new Memory_WriteAddress( 0xe00308, 0xe00309, ddenlovr_coincounter_0_w		),	// Coin Counters
		new Memory_WriteAddress( 0xe0030c, 0xe0030d, ddenlovr_coincounter_1_w		),	//
		/*TODO*///new Memory_WriteAddress( 0xe00400, 0xe00401, YM2413_register_port_0_lsb_w	),	// Sound
		/*TODO*///new Memory_WriteAddress( 0xe00402, 0xe00403, YM2413_data_port_0_lsb_w		),	//
		new Memory_WriteAddress( 0xe00500, 0xe0051f, MWA_RAM 					),	// NVRAM?
		new Memory_WriteAddress( 0xe00302, 0xe00303, MWA_NOP						),	// ?
		new Memory_WriteAddress( 0xe00700, 0xe00701, OKIM6295_data_0_lsb_w 		),	//
		new Memory_WriteAddress( 0xd00000, 0xd017ff, MWA_RAM 					),	// Palette?
                new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	/***************************************************************************
									Rong Rong
	***************************************************************************/
	
	static int rongrong_select,rongrong_select2;
	
	public static ReadHandlerPtr rongrong_input_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if ((rongrong_select & 0x01)==0)	return readinputport(3);
		if ((rongrong_select & 0x02)==0)	return readinputport(4);
		if ((rongrong_select & 0x04)==0)	return readinputport(0);
		if ((rongrong_select & 0x08)==0)	return readinputport(1);
		return 0xff;
	} };
	
	public static ReadHandlerPtr rongrong_input2_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		switch( rongrong_select2 )
		{
			case 0x00:	return 0xff;
			case 0x01:	return 0xff;
			case 0x02:	return readinputport(2);
		}
		return 0xff;
	} };
	
	public static WriteHandlerPtr rongrong_select_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
		rongrong_select = data;
		cpu_setbank(1, new UBytePtr(RAM, 0x10000 + 0x8000 * (rongrong_select & 0x0f)));
	} };
	
	public static WriteHandlerPtr rongrong_select2_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		rongrong_select2 = data;
	} };
	
	public static Memory_ReadAddress rongrong_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x5fff, MRA_ROM					),	// ROM
		new Memory_ReadAddress( 0x6000, 0x7fff, MRA_RAM					),	// RAM
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_BANK1					),	// ROM (Banked)
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress rongrong_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x5fff, MWA_ROM					),	// ROM
		new Memory_WriteAddress( 0x6000, 0x7fff, MWA_RAM					),	// RAM
		new Memory_WriteAddress( 0x8000, 0xffff, MWA_ROM					),	// ROM (Banked)
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static IO_ReadPort rongrong_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x1c, 0x1c, rongrong_input_r		),	//
		new IO_ReadPort( 0x40, 0x40, OKIM6295_status_0_r	),	//
		new IO_ReadPort( 0xa2, 0xa2, rongrong_input2_r		),	//
		new IO_ReadPort( 0xa3, 0xa3, rongrong_input2_r		),	//
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static IO_WritePort rongrong_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x1e, 0x1e, rongrong_select_w		),	//
		new IO_WritePort( 0x40, 0x40, OKIM6295_data_0_w		),	//
		new IO_WritePort( 0xa0, 0xa0, rongrong_select2_w	),	//
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	/*
	0&1: video chip
		14 x
		54
		02 y
		42
	
	1e input select,1c input read
		3e=dsw1	3d=dsw2
	a0 input select,a2 input read (protection?)
		0=?	1=?	2=coins(from a3)
	*/
	
	
	/***************************************************************************
	
	
									Input Ports
	
	
	***************************************************************************/
	
	/***************************************************************************
									Sports Match
	***************************************************************************/
	
	static InputPortPtr input_ports_sprtmtch = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	// IN0 - Player 1
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER1 );
		PORT_BIT(  0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER1 );
		PORT_BIT(  0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER1 );
		PORT_BIT(  0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 );
		PORT_BIT(  0x10, IP_ACTIVE_LOW, IPT_BUTTON1        | IPF_PLAYER1 );
		PORT_BIT(  0x20, IP_ACTIVE_LOW, IPT_BUTTON2        | IPF_PLAYER1 );
		PORT_BIT(  0x40, IP_ACTIVE_LOW, IPT_BUTTON3        | IPF_PLAYER1 );
		PORT_BIT(  0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START(); 	// IN1 - Player 2
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER2 );
		PORT_BIT(  0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER2 );
		PORT_BIT(  0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER2 );
		PORT_BIT(  0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 );
		PORT_BIT(  0x10, IP_ACTIVE_LOW, IPT_BUTTON1        | IPF_PLAYER2 );
		PORT_BIT(  0x20, IP_ACTIVE_LOW, IPT_BUTTON2        | IPF_PLAYER2 );
		PORT_BIT(  0x40, IP_ACTIVE_LOW, IPT_BUTTON3        | IPF_PLAYER2 );
		PORT_BIT(  0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		PORT_START(); 	// IN2 - Coins
		PORT_BIT_IMPULSE(  0x01, IP_ACTIVE_LOW, IPT_COIN1, 10);
		PORT_BIT_IMPULSE(  0x02, IP_ACTIVE_LOW, IPT_COIN2, 10);
		PORT_BIT(  0x04, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT(  0x08, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x10, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x20, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x40, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x80, IP_ACTIVE_LOW, IPT_UNKNOWN  );
	
		PORT_START(); 	// IN3 - DSW
		PORT_DIPNAME( 0x07, 0x07, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x00, DEF_STR( "5C_1C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x02, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x07, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x06, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x05, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "1C_4C") );
		PORT_DIPNAME( 0x38, 0x38, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x00, DEF_STR( "5C_1C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x18, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x38, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x28, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "1C_4C") );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_SERVICE( 0x80, IP_ACTIVE_LOW );
	
		PORT_START(); 	// IN4 - DSW
		PORT_DIPNAME( 0x07, 0x07, DEF_STR( "Difficulty") );	// Time
		PORT_DIPSETTING(    0x00, "1 (Easy)" );
		PORT_DIPSETTING(    0x01, "2" );
		PORT_DIPSETTING(    0x02, "3" );
		PORT_DIPSETTING(    0x03, "4" );
		PORT_DIPSETTING(    0x04, "5" );
		PORT_DIPSETTING(    0x05, "6" );
		PORT_DIPSETTING(    0x06, "7" );
		PORT_DIPSETTING(    0x07, "8 (Hard)" );
		PORT_DIPNAME( 0x18, 0x18, "Vs Time" );
		PORT_DIPSETTING(    0x18, "8 s" );
		PORT_DIPSETTING(    0x10, "10 s" );
		PORT_DIPSETTING(    0x08, "12 s" );
		PORT_DIPSETTING(    0x00, "14 s" );
		PORT_DIPNAME( 0x20, 0x20, "Unknown 2-5" );
		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x40, 0x40, "Unknown 2-6" );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Unknown 2-7" );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	/***************************************************************************
								Don Den Lover Vol.1
	***************************************************************************/
	
	static InputPortPtr input_ports_ddenlovr = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	// IN0 - Player 1
		PORT_BIT(  0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER1 );
		PORT_BIT(  0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 );
		PORT_BIT(  0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER1 );
		PORT_BIT(  0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER1 );
		PORT_BIT(  0x0010, IP_ACTIVE_LOW, IPT_BUTTON1        | IPF_PLAYER1 );
		PORT_BIT(  0x0020, IP_ACTIVE_LOW, IPT_BUTTON2        | IPF_PLAYER1 );
		PORT_BIT(  0x0040, IP_ACTIVE_LOW, IPT_BUTTON3        | IPF_PLAYER1 );
		PORT_BIT(  0x0080, IP_ACTIVE_LOW, IPT_BUTTON4        | IPF_PLAYER1 );
		PORT_BIT(  0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	// IN1 - Player 2
		PORT_BIT(  0x0001, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER2 );
		PORT_BIT(  0x0002, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 );
		PORT_BIT(  0x0004, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER2 );
		PORT_BIT(  0x0008, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER2 );
		PORT_BIT(  0x0010, IP_ACTIVE_LOW, IPT_BUTTON1        | IPF_PLAYER2 );
		PORT_BIT(  0x0020, IP_ACTIVE_LOW, IPT_BUTTON2        | IPF_PLAYER2 );
		PORT_BIT(  0x0040, IP_ACTIVE_LOW, IPT_BUTTON3        | IPF_PLAYER2 );
		PORT_BIT(  0x0080, IP_ACTIVE_LOW, IPT_BUTTON4        | IPF_PLAYER2 );
		PORT_BIT(  0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	// IN2 - Coins + ?
		PORT_BIT(  0x0001, IP_ACTIVE_LOW,  IPT_COIN1    );
		PORT_BIT(  0x0002, IP_ACTIVE_LOW,  IPT_COIN2    );
		PORT_BIT(  0x0004, IP_ACTIVE_LOW,  IPT_SERVICE1 );
		PORT_BIT(  0x0008, IP_ACTIVE_LOW,  IPT_UNKNOWN  );
		PORT_BIT(  0x00f0, IP_ACTIVE_HIGH, IPT_SPECIAL  );
		PORT_BIT(  0xff00, IP_ACTIVE_LOW,  IPT_UNKNOWN  );
	
		PORT_START(); 	// IN3 - DSW
		PORT_SERVICE( 0x0001, IP_ACTIVE_LOW );
		PORT_DIPNAME( 0x0002, 0x0002, "Unknown 1-1" );
		PORT_DIPSETTING(      0x0002, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
		PORT_DIPNAME( 0x0004, 0x0004, "Unknown 1-2" );
		PORT_DIPSETTING(      0x0004, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
		PORT_DIPNAME( 0x0008, 0x0008, "Unknown 1-3" );
		PORT_DIPSETTING(      0x0008, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
		PORT_DIPNAME( 0x0010, 0x0010, "Unknown 1-4" );
		PORT_DIPSETTING(      0x0010, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
		PORT_DIPNAME( 0x0020, 0x0020, "Unknown 1-5*" );
		PORT_DIPSETTING(      0x0020, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
		PORT_DIPNAME( 0x0040, 0x0040, "Unknown 1-6*" );// 6&7
		PORT_DIPSETTING(      0x0040, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
		PORT_DIPNAME( 0x0080, 0x0080, "Unknown 1-7*" );
		PORT_DIPSETTING(      0x0080, DEF_STR( "Off") );
		PORT_DIPSETTING(      0x0000, DEF_STR( "On") );
	
		PORT_BIT(  0xff00, IP_ACTIVE_LOW, IPT_UNKNOWN );
	INPUT_PORTS_END(); }}; 
	
	
	/***************************************************************************
									Rong Rong
	***************************************************************************/
	
	static InputPortPtr input_ports_rongrong = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	// IN0 - Player 1
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER1 );
		PORT_BIT(  0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER1 );
		PORT_BIT(  0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER1 );
		PORT_BIT(  0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER1 );
		PORT_BIT(  0x10, IP_ACTIVE_LOW, IPT_BUTTON1        | IPF_PLAYER1 );
		PORT_BIT(  0x20, IP_ACTIVE_LOW, IPT_BUTTON2        | IPF_PLAYER1 );
		PORT_BIT(  0x40, IP_ACTIVE_LOW, IPT_BUTTON3        | IPF_PLAYER1 );
		PORT_BIT(  0x80, IP_ACTIVE_LOW, IPT_START1 );
	
		PORT_START(); 	// IN1 - Player 2
		PORT_BIT(  0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_PLAYER2 );
		PORT_BIT(  0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_PLAYER2 );
		PORT_BIT(  0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_PLAYER2 );
		PORT_BIT(  0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 );
		PORT_BIT(  0x10, IP_ACTIVE_LOW, IPT_BUTTON1        | IPF_PLAYER2 );
		PORT_BIT(  0x20, IP_ACTIVE_LOW, IPT_BUTTON2        | IPF_PLAYER2 );
		PORT_BIT(  0x40, IP_ACTIVE_LOW, IPT_BUTTON3        | IPF_PLAYER2 );
		PORT_BIT(  0x80, IP_ACTIVE_LOW, IPT_START2 );
	
		PORT_START(); 	// IN2 - Coins
		PORT_BIT_IMPULSE(  0x01, IP_ACTIVE_LOW, IPT_COIN1, 10);
		PORT_BIT_IMPULSE(  0x02, IP_ACTIVE_LOW, IPT_COIN2, 10);
		PORT_BIT(  0x04, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT(  0x08, IP_ACTIVE_LOW, IPT_UNKNOWN  );//?
		PORT_BIT(  0x10, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x20, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x40, IP_ACTIVE_LOW, IPT_UNKNOWN  );
		PORT_BIT(  0x80, IP_ACTIVE_LOW, IPT_UNKNOWN  );
	
		PORT_START(); 	// IN3 - DSW
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x00, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x02, DEF_STR( "1C_2C") );
		PORT_DIPNAME( 0x0c, 0x0c, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x00, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "1C_2C") );
		PORT_DIPNAME( 0x10, 0x10, "Unknown 1-4*" );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x20, 0x20, "Lives?" );
		PORT_DIPSETTING(    0x00, "2" );
		PORT_DIPSETTING(    0x20, "3" );
		PORT_DIPNAME( 0x40, 0x40, "Unknown 1-6*" );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Unknown 1-7*" );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 	// IN4 - DSW
		PORT_DIPNAME( 0x03, 0x03, "Lives?" );
		PORT_DIPSETTING(    0x02, "0" );
		PORT_DIPSETTING(    0x03, "1" );
		PORT_DIPSETTING(    0x01, "2" );
		PORT_DIPSETTING(    0x00, "3" );
		PORT_DIPNAME( 0x0c, 0x0c, "Unknown 2-2&3*" );
		PORT_DIPSETTING(    0x0c, "0" );
		PORT_DIPSETTING(    0x08, "1" );
		PORT_DIPSETTING(    0x04, "2" );
		PORT_DIPSETTING(    0x00, "3" );
		PORT_DIPNAME( 0x30, 0x30, "Unknown 2-4&5*" );
		PORT_DIPSETTING(    0x30, "1" );
		PORT_DIPSETTING(    0x20, "2" );
		PORT_DIPSETTING(    0x10, "3" );
		PORT_DIPSETTING(    0x00, "3'" );
		PORT_DIPNAME( 0x40, 0x40, "Unknown 2-6*" );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x80, "Unknown 2-7*" );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	
	
	/***************************************************************************
	
	
									Machine Drivers
	
	
	***************************************************************************/
	
	/***************************************************************************
									Sports Match
	***************************************************************************/
	
	static YM2203interface ym2203_intf = new YM2203interface
	(
		1,
		22000000 / 6,					/* ? */
		new int[]{ YM2203_VOL(100,100) },
		new ReadHandlerPtr[]{ input_port_3_r },				/* Port A Read: DSW */
		new ReadHandlerPtr[]{ input_port_4_r },				/* Port B Read: DSW */
		new WriteHandlerPtr[]{ null },							/* Port A Write */
		new WriteHandlerPtr[]{ null },							/* Port B Write */
		new WriteYmHandlerPtr[]{ sprtmtch_sound_callback }	/* IRQ handler */
	);
	
	static MachineDriver machine_driver_sprtmtch = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				22000000 / 6,	/* ? */
				sprtmtch_readmem,  sprtmtch_writemem,
				sprtmtch_readport, sprtmtch_writeport,
				sprtmtch_vblank_interrupt, 1	/* IM 0 needs an opcode on the data bus */
			),
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,
		1,
		null,
	
		/* video hardware */
		256, 256, new rectangle( 0, 256-1, 0+8, 256-1-8 ),
		null,	// no tiles
		512, 0,
		sprtmtch_vh_convert_color_prom,			// static palette
		VIDEO_TYPE_RASTER | VIDEO_RGB_DIRECT,	// needs alpha blending
		null,
		sprtmtch_vh_start,
		sprtmtch_vh_stop,
		sprtmtch_vh_screenrefresh,
	
		/* sound hardware */
		SOUND_SUPPORTS_STEREO,0,0,0,
		new MachineSound[] {
			new MachineSound(	SOUND_YM2203,	ym2203_intf	),
		}
	);
	
	
	/***************************************************************************
								Don Den Lover Vol.1
	***************************************************************************/
	
	/*TODO*///static struct YM2413interface ym2413_intf =
	/*TODO*///{
	/*TODO*///	1,
	/*TODO*///	8000000,	/* ? */
	/*TODO*///	{ 100 },
	/*TODO*///};
	
	static OKIM6295interface okim6295_intf = new OKIM6295interface
        (
		1,
		new int[]{ 8000 },	/* ? */
		new int[]{ REGION_SOUND1 },
		new int[]{ 100 }
	);
	
	static MachineDriver machine_driver_ddenlovr = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
				CPU_M68000,
				24000000 / 2,
				ddenlovr_readmem, ddenlovr_writemem,null,null,
				/*TODO*///m68_level1_irq, 1
                                null, 1
			),
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,
		1,
		null,
	
		/* video hardware */
		320, 256, new rectangle( 0, 320-1, 0, 256-1 ),
		null,		// no tiles
		0x800, 0x800,
		null,
		VIDEO_TYPE_RASTER,
		null,
		dynax_vh_start,
		null,
		dynax_vh_screenrefresh,
	
		/* sound hardware */
		SOUND_SUPPORTS_STEREO,0,0,0,
		/*TODO*///new MachineSound[] {
		/*TODO*///	new MachineSound(	SOUND_YM2413,	ym2413_intf	),
		/*TODO*///	new MachineSound(	SOUND_OKIM6295,	okim6295_intf	),
		/*TODO*///}
                null
	);
	
	
	/***************************************************************************
									Rong Rong
	***************************************************************************/
	
	static MachineDriver machine_driver_rongrong = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				4000000,	/* ? */
				rongrong_readmem,  rongrong_writemem,
				rongrong_readport, rongrong_writeport,
				interrupt, 1
			),
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,
		1,
		null,
	
		/* video hardware */
		256, 256, new rectangle( 0, 256-1, 0, 256-1 ),
		null,		// no tiles
		0x800, 0x800,
		null,
		VIDEO_TYPE_RASTER,
		null,
		dynax_vh_start,
		null,
		dynax_vh_screenrefresh,
	
		/* sound hardware */
		SOUND_SUPPORTS_STEREO,0,0,0,
		/*TODO*///new MachineSound[] {
		/*TODO*///	new MachineSound(	SOUND_YM2413,	ym2413_intf	),
		/*TODO*///	new MachineSound(	SOUND_OKIM6295,	okim6295_intf	),
		/*TODO*///}
                null
	);
	
	
	/***************************************************************************
	
	
									ROMs Loading
	
	
	***************************************************************************/
	
	/***************************************************************************
	Sports Match
	Dynax 1989
	
	                     5563
	                     3101
	        SW2 SW1
	                             3103
	         YM2203              3102
	                     16V8
	                     Z80         DYNAX
	         22MHz
	
	           6845
	                         53462
	      17G                53462
	      18G                53462
	                         53462
	                         53462
	                         53462
	
	- Note: to enter hardware test mode keep start1 pressed during power-up.
	
	***************************************************************************/
	
	static RomLoadPtr rom_sprtmtch = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* Z80 Code */
		ROM_LOAD( "3101.3d", 0x0000, 0x10000, 0xd8fa9638 );
	
		ROM_REGION( 0x40000, REGION_GFX1, 0 );/* Gfx Data (Do not dispose) */
		ROM_LOAD( "3102.6c", 0x00000, 0x20000, 0x46f90e59 );
		ROM_LOAD( "3103.5c", 0x20000, 0x20000, 0xad29d7bd );
	
		ROM_REGION( 0x400, REGION_PROMS, ROMREGION_DISPOSE );/* Color PROMs */
		ROM_LOAD( "18g", 0x000, 0x200, 0xdcc4e0dd );// FIXED BITS (0xxxxxxx)
		ROM_LOAD( "17g", 0x200, 0x200, 0x5443ebfb );
	ROM_END(); }}; 
	
	
	
	/***************************************************************************
	
	Don Den Lover Vol 1
	(C) Dynax Inc 1995
	
	CPU: TMP68HC000N-12
	SND: OKI M6295, YM2413 (18 pin DIL), YMZ284-D (16 pin DIL. This chip is in place where a 40 pin chip is marked on PCB,
	                                     possibly a replacement for some other 40 pin YM chip?)
	OSC: 28.636MHz (near large GFX chip), 24.000MHz (near CPU)
	DIPS: 1 x 8 Position switch. DIP info is in Japanese !
	RAM: 1 x Toshiba TC5588-35, 2 x Toshiba TC55257-10, 5 x OKI M514262-70
	
	OTHER:
	Battery
	RTC 72421B   4382 (18 pin DIL)
	3 X PAL's (2 on daughter-board at locations 2E & 2D, 1 on main board near CPU at location 4C)
	GFX Chip - NAKANIHON NL-005 (208 pin, square, surface-mounted)
	Controls: 8 Way Joystick plus 2 Buttons.
	
	ROMS: (All located on a daughter-board)
	
	1133H.1C	27C020   \
	1134H.1A	27C020   / MAIN PROGRAM?
	
	1131H.1F	27C040   \
	1132H.1E	27C040   / SOUND?
	
	1135H.3H	27C040   -\
	1136H.3F	27C040    |
	1137H.3E	27C040    | GFX?
	1138H.3D	27C040    |
	1139H.3C	27C040    /
	
	***************************************************************************/
	
	static RomLoadPtr rom_ddenlovr = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x080000, REGION_CPU1, 0 );	/* 68000 Code */
		ROM_LOAD16_BYTE( "1134h.1a", 0x000000, 0x040000, 0x43accdff );
		ROM_LOAD16_BYTE( "1133h.1c", 0x000001, 0x040000, 0x361bf7b6 );
	
		ROM_REGION( 0x280000, REGION_GFX1, 0 );/* Gfx Data (Do not dispose) */
		ROM_LOAD( "1135h.3h", 0x000000, 0x080000, 0xee143d8e );
		ROM_LOAD( "1136h.3f", 0x080000, 0x080000, 0x58a662be );
		ROM_LOAD( "1137h.3e", 0x100000, 0x080000, 0xf96e0708 );
		ROM_LOAD( "1138h.3d", 0x180000, 0x080000, 0x633cff33 );
		ROM_LOAD( "1139h.3c", 0x200000, 0x080000, 0xbe1189ca );
	
		ROM_REGION( 0x100000, REGION_SOUND1, ROMREGION_SOUNDONLY );/* Samples */
		ROM_LOAD( "1131h.1f", 0x000000, 0x080000, 0x32f68241 );// 4 x $40000
		ROM_LOAD( "1132h.1e", 0x080000, 0x080000, 0x2de6363d );//
	ROM_END(); }}; 
	
	
	/***************************************************************************
	
									Rong Rong
	
	Here are the proms for Nakanihon's Rong Rong
	It's a quite nice Puzzle game.
	The CPU don't have any numbers on it except for this:
	Nakanihon
	NL-002
	3J3  JAPAN
	For the sound it uses A YM2413
	
	***************************************************************************/
	
	static RomLoadPtr rom_rongrong = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x90000, REGION_CPU1, 0 );/* Z80 Code */
		ROM_LOAD( "rr_8002g.rom", 0x00000, 0x80000, 0x9a5d2885 );
		ROM_RELOAD(               0x10000, 0x80000             );
	
		ROM_REGION( 0x280000, REGION_GFX1, 0 );/* Gfx Data (Do not dispose) */
		ROM_LOAD( "rr_8003.rom",  0x000000, 0x80000, 0xf57192e5 );
		ROM_LOAD( "rr_8004.rom",  0x080000, 0x80000, 0xc8c0b5cb );
		ROM_LOAD( "rr_8005g.rom", 0x100000, 0x80000, 0x11c7a23c );
		ROM_LOAD( "rr_8006g.rom", 0x180000, 0x80000, 0xf3de77e6 );
		ROM_LOAD( "rr_8007g.rom", 0x200000, 0x80000, 0x38a8caa3 );
	
		ROM_REGION( 0x40000, REGION_SOUND1, ROMREGION_SOUNDONLY );/* Samples */
		ROM_LOAD( "rr_8001w.rom", 0x00000, 0x40000, 0x8edc87a2 );
	ROM_END(); }}; 
	
	
	/***************************************************************************
	
	
									Game Drivers
	
	
	***************************************************************************/
	
	public static GameDriver driver_sprtmtch	   = new GameDriver("1989"	,"sprtmtch"	,"dynax.java"	,rom_sprtmtch,null	,machine_driver_sprtmtch	,input_ports_sprtmtch	,null	,ROT0	,	"Log+Dynax (Fabtek license)", "Sports Match" );
	
	/* TESTDRIVERS */
	/*TODO*///public static GameDriver driver_ddenlovr	   = new GameDriver("1995"	,"ddenlovr"	,"dynax.java"	,rom_ddenlovr,null	,machine_driver_ddenlovr	,input_ports_ddenlovr	,null	,ROT0	,	"Dynax",     "Don Den Lover Vol 1", GAME_NOT_WORKING );
	/*TODO*///public static GameDriver driver_rongrong	   = new GameDriver("1994"	,"rongrong"	,"dynax.java"	,rom_rongrong,null	,machine_driver_rongrong	,input_ports_rongrong	,null	,ROT0	,	"Nakanihon", "Rong Rong",           GAME_NOT_WORKING );
}
