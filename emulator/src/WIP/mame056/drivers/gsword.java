/* Great Swordsman (Taito) 1984

TODO:
- I haven't really tried to make Joshi Volley work. It's booting into
  service mode now, it might not be hard to fix it. It has only two Z80.


Credits:
- Steve Ellenoff: Original emulation and Mame driver
- Jarek Parchanski: Dip Switch Fixes, Color improvements, ADPCM Interface code
- Tatsuyuki Satoh: sound improvements, NEC 8741 emulation,adpcm improvements
- Charlie Miltenberger: sprite colors improvements & precious hardware
			information and screenshots

Trick:
If you want fight with ODILION swordsman patch program for 1st CPU
at these addresses, otherwise you won't never fight with him.

		ROM[0x2256] = 0
		ROM[0x2257] = 0
		ROM[0x2258] = 0
		ROM[0x2259] = 0
		ROM[0x225A] = 0


There are 3 Z80s and two AY-3-8910s..

Prelim memory map (last updated 6/15/98)
*****************************************
GS1		z80 Main Code	(8K)	0000-1FFF
Gs2     z80 Game Data   (8K)    2000-3FFF
Gs3     z80 Game Data   (8K)    4000-5FFF
Gs4     z80 Game Data   (8K)    6000-7FFF
Gs5     z80 Game Data   (4K)    8000-8FFF
Gs6     Sprites         (8K)
Gs7     Sprites         (8K)
Gs8		Sprites			(8K)
Gs10	Tiles			(8K)
Gs11	Tiles			(8K)
Gs12    3rd z80 CPU &   (8K)
        ADPCM Samples?
Gs13    ADPCM Samples?  (8K)
Gs14    ADPCM Samples?  (8K)
Gs15    2nd z80 CPU     (8K)    0000-1FFF
Gs16    2nd z80 Data    (8K)    2000-3FFF
*****************************************

**********
*Main Z80*
**********

	9000 - 9fff	Work Ram
        982e - 982e Free play
        98e0 - 98e0 Coin Input
        98e1 - 98e1 Player 1 Controls
        98e2 - 98e2 Player 2 Controls
        9c00 - 9c30 (Hi score - Scores)
        9c78 - 9cd8 (Hi score - Names)
        9e00 - 9e7f Sprites in working ram!
        9e80 - 9eff Sprite X & Y in working ram!

	a000 - afff	Sprite RAM & Video Attributes
        a000 - a37F	???
        a380 - a77F	Sprite Tile #s
        a780 - a7FF	Sprite Y & X positions
        a980 - a980	Background Tile Bank Select
        ab00 - ab00	Background Tile Y-Scroll register
        ab80 - abff	Sprite Attributes(X & Y Flip)

	b000 - b7ff	Screen RAM
	b800 - ffff	not used?!

PORTS:
7e 8741-#0 data port
7f 8741-#1 command / status port

*************
*2nd Z80 CPU*
*************
0000 - 3FFF ROM CODE
4000 - 43FF WORK RAM

write
6000 adpcm sound command for 3rd CPU

PORTS:
00 8741-#2 data port
01 8741-#2 command / status port
20 8741-#3 data port
21 8741-#3 command / status port
40 8741-#1 data port
41 8741-#1 command / status port

read:
60 fake port #0 ?
61 ay8910-#0 read port
data / ay8910-#0 read
80 fake port #1 ?
81 ay8910-#1 read port

write:
60 ay8910-#0 controll port
61 ay8910-#0 data port
80 ay8910-#1 controll port
81 ay8910-#1 data port
   ay8910-A  : NMI controll ?
a0 unknown
e0 unknown (watch dog?)

*************
*3rd Z80 CPU*
*************
0000-5fff ROM

read:
a000 adpcm sound command

write:
6000 MSM5205 reset and data

*************
I8741 communication data

reg: 0->1 (main->2nd) /     : (1->0) 2nd->main :
 0 : DSW.2 (port)           : DSW.1(port)
 1 : DSW.1                  : DSW.2
 2 : IN0 / sound error code :
 3 : IN1 / ?                :
 4 : IN2                    :
 4 : IN3                    :
 5 :                        :
 6 :                        : DSW0?
 7 :                        : ?

******************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;

import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.mame.*;
import static mame056.inptportH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.memoryH.*;
import static mame056.memory.*;
import static mame056.inptport.*;
import static mame056.drawgfxH.*;
import static mame056.inputH.*;
import static mame056.sndintrfH.*;
import static mame056.sndintrf.*;
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;
import static mame056.sound.ay8910.*;
import static mame056.sound.ay8910H.*;
import static mame056.sound.MSM5205.*;
import static mame056.sound.MSM5205H.*;
import static mame056.timer.*;
import static mame056.timerH.*;

import static mame056.vidhrdw.generic.*;

import static arcadeflex056.fileio.*;
import static mame056.palette.*;
import static mame056.inptport.*;
import static mame056.sound.sn76477H.*;
import static mame056.sound.sn76477.*;

import static WIP.mame056.vidhrdw.gsword.*;
import static WIP.mame056.machine.tait8741.*;
import static WIP.mame056.machine.tait8741H.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class gsword
{
	static int coins;
	static int fake8910_0,fake8910_1;
	static int gsword_nmi_step,gsword_nmi_count;
	
	
	static int gsword_coins_in()
	{
		/* emulate 8741 coin slot */
		if ((readinputport(4)&0xc0) != 0)
		{
			logerror("Coin In\n");
			return 0x80;
		}
		logerror("NO Coin\n");
		return 0x00;
	}
	
	public static ReadHandlerPtr gsword_8741_2_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		switch (offset)
		{
		case 0x01: /* start button , coins */
			return readinputport(0);
		case 0x02: /* Player 1 Controller */
			return readinputport(1);
		case 0x04: /* Player 2 Controller */
			return readinputport(3);
		default:
			logerror("8741-2 unknown read %d PC=%04x\n",offset,cpu_get_pc());
		}
		/* unknown */
		return 0;
	} };
	
	public static ReadHandlerPtr gsword_8741_3_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		switch (offset)
		{
		case 0x01: /* start button  */
			return readinputport(2);
		case 0x02: /* Player 1 Controller? */
			return readinputport(1);
		case 0x04: /* Player 2 Controller? */
			return readinputport(3);
		}
		/* unknown */
		logerror("8741-3 unknown read %d PC=%04x\n",offset,cpu_get_pc());
		return 0;
	} };
	
	static TAITO8741interface gsword_8741interface=new TAITO8741interface
        (
		4,         /* 4 chips */
		new int[]{ TAITO8741_MASTER,TAITO8741_SLAVE,TAITO8741_PORT,TAITO8741_PORT },  /* program mode */
		new int[]{ 1,0,0,0 },							     /* serial port connection */
		new ReadHandlerPtr[]{ input_port_7_r,input_port_6_r,gsword_8741_2_r,gsword_8741_3_r }    /* port handler */
	);
	
	public static InitMachinePtr machine_init = new InitMachinePtr() { public void handler() 
	{
		int i;
	
		for(i=0;i<4;i++) TAITO8741_reset(i);
		coins = 0;
		gsword_nmi_count = 0;
		gsword_nmi_step  = 0;
	
		TAITO8741_start(gsword_8741interface);
	} };
	
	public static InterruptPtr gsword_snd_interrupt = new InterruptPtr() { public int handler() 
	{
		if( (gsword_nmi_count+=gsword_nmi_step) >= 4)
		{
			gsword_nmi_count = 0;
			return Z80_NMI_INT;
		}
		return ignore_interrupt.handler();
	} };
	
	public static WriteHandlerPtr gsword_nmi_set_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		switch(data)
		{
		case 0x02:
			/* needed to disable NMI for memory check */
			gsword_nmi_step  = 0;
			gsword_nmi_count = 0;
			break;
		case 0x0d:
		case 0x0f:
			gsword_nmi_step  = 4;
			break;
		case 0xfe:
		case 0xff:
			gsword_nmi_step  = 4;
			break;
		}
		/* bit1= nmi disable , for ram check */
		logerror("NMI controll %02x\n",data);
	} };
	
	public static WriteHandlerPtr gsword_AY8910_control_port_0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		AY8910_control_port_0_w.handler(offset,data);
		fake8910_0 = data;
	} };
	public static WriteHandlerPtr gsword_AY8910_control_port_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		AY8910_control_port_1_w.handler(offset,data);
		fake8910_1 = data;
	} };
	
	public static ReadHandlerPtr gsword_fake_0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return fake8910_0+1;
	} };
	public static ReadHandlerPtr gsword_fake_1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return fake8910_1+1;
	} };
	
	public static WriteHandlerPtr gsword_adpcm_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		MSM5205_data_w.handler(0,data & 0x0f); /* bit 0..3 */
		MSM5205_reset_w.handler(0,(data>>5)&1); /* bit 5    */
		MSM5205_vclk_w.handler(0,(data>>4)&1);  /* bit 4    */
	} };
	
	public static WriteHandlerPtr adpcm_soundcommand_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		soundlatch_w.handler(0,data);
		cpu_set_nmi_line(2, PULSE_LINE);
	} };
	
	public static Memory_ReadAddress gsword_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x8fff, MRA_ROM ),
		new Memory_ReadAddress( 0x9000, 0x9fff, MRA_RAM ),
		new Memory_ReadAddress( 0xb000, 0xb7ff, MRA_RAM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress gsword_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x8fff, MWA_ROM ),
		new Memory_WriteAddress( 0x9000, 0x9fff, MWA_RAM ),
		new Memory_WriteAddress( 0xa380, 0xa3ff, MWA_RAM, gs_spritetile_ram ),
		new Memory_WriteAddress( 0xa780, 0xa7ff, MWA_RAM, gs_spritexy_ram, gs_spritexy_size ),
		new Memory_WriteAddress( 0xa980, 0xa980, gs_charbank_w ),
		new Memory_WriteAddress( 0xaa80, 0xaa80, gs_videoctrl_w ),	/* flip screen, char palette bank */
		new Memory_WriteAddress( 0xab00, 0xab00, MWA_RAM, gs_scrolly_ram ),
		new Memory_WriteAddress( 0xab80, 0xabff, MWA_RAM, gs_spriteattrib_ram ),
		new Memory_WriteAddress( 0xb000, 0xb7ff, gs_videoram_w, gs_videoram, gs_videoram_size ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_ReadAddress readmem_cpu2[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x3fff, MRA_ROM ),
		new Memory_ReadAddress( 0x4000, 0x43ff, MRA_RAM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress writemem_cpu2[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x3fff, MWA_ROM ),
		new Memory_WriteAddress( 0x4000, 0x43ff, MWA_RAM ),
		new Memory_WriteAddress( 0x6000, 0x6000, adpcm_soundcommand_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_ReadAddress readmem_cpu3[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x5fff, MRA_ROM ),
		new Memory_ReadAddress( 0xa000, 0xa000, soundlatch_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress writemem_cpu3[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x5fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x8000, gsword_adpcm_data_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	public static IO_ReadPort readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_ReadPort( 0x7e, 0x7f, TAITO8741_0_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	public static IO_WritePort writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_WritePort( 0x7e, 0x7f, TAITO8741_0_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	public static IO_ReadPort readport_cpu2[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_ReadPort( 0x00, 0x01, TAITO8741_2_r ),
		new IO_ReadPort( 0x20, 0x21, TAITO8741_3_r ),
		new IO_ReadPort( 0x40, 0x41, TAITO8741_1_r ),
		new IO_ReadPort( 0x60, 0x60, gsword_fake_0_r ),
		new IO_ReadPort( 0x61, 0x61, AY8910_read_port_0_r ),
		new IO_ReadPort( 0x80, 0x80, gsword_fake_1_r ),
		new IO_ReadPort( 0x81, 0x81, AY8910_read_port_1_r ),
		new IO_ReadPort( 0xe0, 0xe0, IORP_NOP ), /* ?? */
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	public static IO_WritePort writeport_cpu2[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_WritePort( 0x00, 0x01, TAITO8741_2_w ),
		new IO_WritePort( 0x20, 0x21, TAITO8741_3_w ),
		new IO_WritePort( 0x40, 0x41, TAITO8741_1_w ),
		new IO_WritePort( 0x60, 0x60, gsword_AY8910_control_port_0_w ),
		new IO_WritePort( 0x61, 0x61, AY8910_write_port_0_w ),
		new IO_WritePort( 0x80, 0x80, gsword_AY8910_control_port_1_w ),
		new IO_WritePort( 0x81, 0x81, AY8910_write_port_1_w ),
		new IO_WritePort( 0xa0, 0xa0, IOWP_NOP ), /* ?? */
		new IO_WritePort( 0xe0, 0xe0, IOWP_NOP ), /* watch dog ?*/
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	
	
	public static Memory_ReadAddress josvolly_sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x0fff, MRA_ROM ),
	//	new Memory_ReadAddress( 0x2000, 0x3fff, MRA_ROM ), another ROM probably, not sure which one (tested on boot)
		new Memory_ReadAddress( 0x4000, 0x43ff, MRA_RAM ),
	//	new Memory_ReadAddress( 0xa000, 0xa000, soundlatch_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress josvolly_sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x0fff, MWA_ROM ),
		new Memory_WriteAddress( 0x4000, 0x43ff, MWA_RAM ),
	//	new Memory_WriteAddress( 0x8000, 0x8000, gsword_adpcm_data_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	public static IO_ReadPort josvolly_sound_readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_ReadPort( 0x00, 0x00, AY8910_read_port_0_r ),
		new IO_ReadPort( 0x40, 0x40, AY8910_read_port_1_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	public static IO_WritePort josvolly_sound_writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_WritePort( 0x00, 0x00, AY8910_control_port_0_w ),
		new IO_WritePort( 0x01, 0x01, AY8910_write_port_0_w ),
		new IO_WritePort( 0x40, 0x40, AY8910_control_port_1_w ),
		new IO_WritePort( 0x41, 0x41, AY8910_write_port_1_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	
	static InputPortPtr input_ports_gsword = new InputPortPtr(){ public void handler() { 
	PORT_START(); 	/* IN0 (8741-2 port1?) */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_START1 );	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_START2 );	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_HIGH, IPT_COIN1, 1 );	PORT_START(); 	/* IN1 (8741-2 port2?) */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY );	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_2WAY );	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_BUTTON3 );	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_BUTTON2 );	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_HIGH, IPT_COIN1, 1 );	PORT_START(); 	/* IN2 (8741-3 port1?) */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_START1 );	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_START2 );	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_HIGH, IPT_COIN1, 1 );	PORT_START(); 	/* IN3  (8741-3 port2?) */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_COCKTAIL );	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_2WAY | IPF_COCKTAIL );	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_BUTTON3 | IPF_COCKTAIL );	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_COCKTAIL );	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_COCKTAIL );	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_HIGH, IPT_COIN1, 1 );	PORT_START(); 	/* IN4 (coins) */
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT_IMPULSE( 0x80, IP_ACTIVE_HIGH, IPT_COIN1, 1 );
		PORT_START(); 	/* DSW0 */
		/* NOTE: Switches 0 & 1, 6,7,8 not used 	 */
		/*	 Coins configurations were handled 	 */
		/*	 via external hardware & not via program */
		PORT_DIPNAME( 0x1c, 0x00, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x1c, DEF_STR( "5C_1C") );
		PORT_DIPSETTING(    0x18, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x14, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_5C") );
	
		PORT_START();       /* DSW1 */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x0c, 0x04, "Stage 1 Difficulty" );	PORT_DIPSETTING(    0x00, "Easy" );	PORT_DIPSETTING(    0x04, "Normal" );	PORT_DIPSETTING(    0x08, "Hard" );	PORT_DIPSETTING(    0x0c, "Hardest" );	PORT_DIPNAME( 0x10, 0x10, "Stage 2 Difficulty" );	PORT_DIPSETTING(    0x00, "Easy" );	PORT_DIPSETTING(    0x10, "Hard" );	PORT_DIPNAME( 0x20, 0x20, "Stage 3 Difficulty" );	PORT_DIPSETTING(    0x00, "Easy" );	PORT_DIPSETTING(    0x20, "Hard" );	PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, "Free Game Round" );	PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	
		PORT_START();       /* DSW2 */
		PORT_DIPNAME( 0x01, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x01, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x02, DEF_STR( "On") );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Free_Play") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x04, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x30, 0x00, "Stage Begins" );	PORT_DIPSETTING(    0x00, "Fencing" );	PORT_DIPSETTING(    0x10, "Kendo" );	PORT_DIPSETTING(    0x20, "Rome" );	PORT_DIPSETTING(    0x30, "Kendo" );	PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x40, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x80, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	static GfxLayout gsword_text = new GfxLayout
	(
		8,8,    /* 8x8 characters */
		1024,	/* 1024 characters */
		2,      /* 2 bits per pixel */
		new int[] { 0, 4 },	/* the two bitplanes for 4 pixels are packed into one byte */
		new int[] { 0, 1, 2, 3, 8*8+0, 8*8+1, 8*8+2, 8*8+3 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		16*8    /* every char takes 16 bytes */
	);
	
	static GfxLayout gsword_sprites1 = new GfxLayout
	(
		16,16,   /* 16x16 sprites */
		64*2,    /* 128 sprites */
		2,       /* 2 bits per pixel */
		new int[] { 0, 4 },	/* the two bitplanes for 4 pixels are packed into one byte */
		new int[] { 0, 1, 2, 3, 8*8+0, 8*8+1, 8*8+2, 8*8+3,
				16*8+0, 16*8+1, 16*8+2, 16*8+3, 24*8+0, 24*8+1, 24*8+2, 24*8+3},
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
				32*8, 33*8, 34*8, 35*8, 36*8, 37*8, 38*8, 39*8 },
		64*8     /* every sprite takes 64 bytes */
	);
	
	static GfxLayout gsword_sprites2 = new GfxLayout
	(
		32,32,    /* 32x32 sprites */
		64,       /* 64 sprites */
		2,       /* 2 bits per pixel */
		new int[] { 0, 4 }, /* the two bitplanes for 4 pixels are packed into one byte */
		new int[] { 0, 1, 2, 3, 8*8+0, 8*8+1, 8*8+2, 8*8+3,
				16*8+0, 16*8+1, 16*8+2, 16*8+3, 24*8+0, 24*8+1, 24*8+2, 24*8+3,
				64*8+0, 64*8+1, 64*8+2, 64*8+3, 72*8+0, 72*8+1, 72*8+2, 72*8+3,
				80*8+0, 80*8+1, 80*8+2, 80*8+3, 88*8+0, 88*8+1, 88*8+2, 88*8+3},
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
				32*8, 33*8, 34*8, 35*8, 36*8, 37*8, 38*8, 39*8,
				128*8, 129*8, 130*8, 131*8, 132*8, 133*8, 134*8, 135*8,
				160*8, 161*8, 162*8, 163*8, 164*8, 165*8, 166*8, 167*8 },
		64*8*4    /* every sprite takes (64*8=16x6)*4) bytes */
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, gsword_text,         0, 64 ),
		new GfxDecodeInfo( REGION_GFX2, 0, gsword_sprites1,  64*4, 64 ),
		new GfxDecodeInfo( REGION_GFX3, 0, gsword_sprites2,  64*4, 64 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	static AY8910interface ay8910_interface = new AY8910interface
	(
		2,		/* 2 chips */
		1500000,	/* 1.5 MHz */
		new int[] { 30, 30 },
		new ReadHandlerPtr[] { null, null },
		new ReadHandlerPtr[] { null, null },
		new WriteHandlerPtr[] { null, gsword_nmi_set_w }, /* portA write */
		new WriteHandlerPtr[] { null, null }
	);
	
	static MSM5205interface msm5205_interface = new MSM5205interface
        (
		1,				/* 1 chip             */
		384000,				/* 384KHz verified!   */
		new vclk_interruptPtr[]{ null },				/* interrupt function */
		new int[]{ MSM5205_SEX_4B },		/* vclk input mode    */
		new int[]{ 60 }
	);
	
	
	
	static MachineDriver machine_driver_josvolly = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				3000000,
				gsword_readmem,gsword_writemem,readport,writeport,
				interrupt,1
			),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				3000000,
				josvolly_sound_readmem,josvolly_sound_writemem,josvolly_sound_readport,josvolly_sound_writeport,
				ignore_interrupt,0
			)
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,
		machine_init,
	
		/* video hardware */
		32*8, 32*8,new rectangle( 0*8, 32*8-1, 2*8, 30*8-1 ),
	
		gfxdecodeinfo,
		256, 64*4+64*4,
		josvolly_vh_convert_color_prom,
		VIDEO_TYPE_RASTER,
		null,
		gsword_vh_start,
		gsword_vh_stop,
		gsword_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_AY8910,
				ay8910_interface
			),
			new MachineSound(
				SOUND_MSM5205,
				msm5205_interface
			)
		}
	
	);
	
	static MachineDriver machine_driver_gsword = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				3000000,
				gsword_readmem,gsword_writemem,
				readport,writeport,
				interrupt,1
			),
			new MachineCPU(
				CPU_Z80,
				3000000,
				readmem_cpu2,writemem_cpu2,
				readport_cpu2,writeport_cpu2,
				gsword_snd_interrupt,4
			),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				3000000,
				readmem_cpu3,writemem_cpu3,
				null,null,
				ignore_interrupt,0
			)
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		200,                                 /* Allow time for 2nd cpu to interleave*/
		machine_init,
	
		/* video hardware */
		32*8, 32*8,new rectangle( 0*8, 32*8-1, 2*8, 30*8-1 ),
	
		gfxdecodeinfo,
		256, 64*4+64*4,
		gsword_vh_convert_color_prom,
		VIDEO_TYPE_RASTER,
		null,
		gsword_vh_start,
		gsword_vh_stop,
		gsword_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_AY8910,
				ay8910_interface
			),
			new MachineSound(
				SOUND_MSM5205,
				msm5205_interface
			)
		}
	
	);
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_josvolly = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64K for main CPU */
		ROM_LOAD( "aa2-1.2c",     0x0000, 0x2000, 0x27f740a5 );	ROM_LOAD( "aa1-2.2d",     0x2000, 0x2000, 0x3e02e3e1 );	ROM_LOAD( "aa0-3.2e",     0x4000, 0x2000, 0x72843ffe );	ROM_LOAD( "aa1-4.2f",     0x6000, 0x2000, 0x22c1466e );
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64K for 2nd CPU */
		ROM_LOAD( "aa3-12.2h",    0x0000, 0x1000, 0x3796bbf6 );
		ROM_REGION( 0x04000, REGION_USER1, 0 );/* music data and samples - not sure where it's mapped */
		ROM_LOAD( "aa0-13.2j",    0x0000, 0x2000, 0x58cc89ac );	ROM_LOAD( "aa0-14.4j",    0x2000, 0x2000, 0x436fe91f );
		ROM_REGION( 0x4000, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "aa0-10.9n",    0x0000, 0x2000, 0x207c4f42 );/* tiles */
		ROM_LOAD( "aa1-11.9p",    0x2000, 0x1000, 0xc130464a );
		ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );	ROM_LOAD( "aa0-6.9e",     0x0000, 0x2000, 0xc2c2401a );/* sprites */
	
		ROM_REGION( 0x4000, REGION_GFX3, ROMREGION_DISPOSE );	ROM_LOAD( "aa0-7.9f",     0x0000, 0x2000, 0xda836231 );	ROM_LOAD( "aa0-8.9h",     0x2000, 0x2000, 0xa0426d57 );
		ROM_REGION( 0x0460, REGION_PROMS, 0 );	ROM_LOAD( "a1.10k",       0x0000, 0x0100, 0x09f7b56a );/* palette red? */
		ROM_LOAD( "a2.9k",        0x0100, 0x0100, 0x852eceac );/* palette green? */
		ROM_LOAD( "a3.9j",        0x0200, 0x0100, 0x1312718b );/* palette blue? */
		ROM_LOAD( "a4.8c",        0x0300, 0x0100, 0x1dcec967 );/* sprite lookup table */
		ROM_LOAD( "003.4e",       0x0400, 0x0020, 0x43a548b8 );/* address decoder? not used */
		ROM_LOAD( "004.4d",       0x0420, 0x0020, 0x43a548b8 );/* address decoder? not used */
		ROM_LOAD( "005.3h",       0x0440, 0x0020, 0xe8d6dec0 );/* address decoder? not used */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_gsword = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64K for main CPU */
		ROM_LOAD( "gs1",          0x0000, 0x2000, 0x565c4d9e );	ROM_LOAD( "gs2",          0x2000, 0x2000, 0xd772accf );	ROM_LOAD( "gs3",          0x4000, 0x2000, 0x2cee1871 );	ROM_LOAD( "gs4",          0x6000, 0x2000, 0xca9d206d );	ROM_LOAD( "gs5",          0x8000, 0x1000, 0x2a892326 );
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64K for 2nd CPU */
		ROM_LOAD( "gs15",         0x0000, 0x2000, 0x1aa4690e );	ROM_LOAD( "gs16",         0x2000, 0x2000, 0x10accc10 );
		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* 64K for 3nd z80 */
		ROM_LOAD( "gs12",         0x0000, 0x2000, 0xa6589068 );	ROM_LOAD( "gs13",         0x2000, 0x2000, 0x4ee79796 );	ROM_LOAD( "gs14",         0x4000, 0x2000, 0x455364b6 );
		ROM_REGION( 0x4000, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "gs10",         0x0000, 0x2000, 0x517c571b );/* tiles */
		ROM_LOAD( "gs11",         0x2000, 0x2000, 0x7a1d8a3a );
		ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );	ROM_LOAD( "gs6",          0x0000, 0x2000, 0x1b0a3cb7 );/* sprites */
	
		ROM_REGION( 0x4000, REGION_GFX3, ROMREGION_DISPOSE );	ROM_LOAD( "gs7",          0x0000, 0x2000, 0xef5f28c6 );	ROM_LOAD( "gs8",          0x2000, 0x2000, 0x46824b30 );
		ROM_REGION( 0x0360, REGION_PROMS, 0 );	ROM_LOAD( "ac0-1.bpr",    0x0000, 0x0100, 0x5c4b2adc );/* palette low bits */
		ROM_LOAD( "ac0-2.bpr",    0x0100, 0x0100, 0x966bda66 );/* palette high bits */
		ROM_LOAD( "ac0-3.bpr",    0x0200, 0x0100, 0xdae13f77 );/* sprite lookup table */
		ROM_LOAD( "003",          0x0300, 0x0020, 0x43a548b8 );/* address decoder? not used */
		ROM_LOAD( "004",          0x0320, 0x0020, 0x43a548b8 );/* address decoder? not used */
		ROM_LOAD( "005",          0x0340, 0x0020, 0xe8d6dec0 );/* address decoder? not used */
	ROM_END(); }}; 
	
	
	
	public static InitDriverPtr init_gsword = new InitDriverPtr() { public void handler()
	{
		UBytePtr ROM2 = new UBytePtr(memory_region(REGION_CPU2));
	
		ROM2.write(0x1da, 0xc3); /* patch for rom self check */
		ROM2.write(0x726, 0);    /* patch for sound protection or time out function */
		ROM2.write(0x727, 0);
	} };
	
	
	public static GameDriver driver_josvolly	   = new GameDriver("1983"	,"josvolly"	,"gsword.java"	,rom_josvolly,null	,machine_driver_josvolly	,input_ports_gsword	,null	,ROT90	,	"Taito Corporation", "Joshi Volleyball", GAME_NOT_WORKING );
	public static GameDriver driver_gsword	   = new GameDriver("1984"	,"gsword"	,"gsword.java"	,rom_gsword,null	,machine_driver_gsword	,input_ports_gsword	,init_gsword	,ROT0	,	"Taito Corporation", "Great Swordsman", GAME_IMPERFECT_COLORS );
}
