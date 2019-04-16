/***************************************************************************

Jailbreak - (c) 1986 Konami

Ernesto Corvi
ernesto@imagina.com

***************************************************************************/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.drivers;

import static arcadeflex056.fucPtr.*;
import common.ptr.UBytePtr;

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
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;
import static mame056.sound.vlm5030.*;

import static mame056.vidhrdw.generic.*;

import static mame056.vidhrdw.jailbrek.*;

import static arcadeflex056.fileio.*;
import static mame056.sound.sn76496.*;
import mame056.sound.sn76496H.SN76496interface;
import mame056.sound.vlm5030H.VLM5030interface;
import static mame056.machine.konami.*;

public class jailbrek
{
	
	
	
	static int irq_enable,nmi_enable;
	
	public static WriteHandlerPtr ctrl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		nmi_enable = data & 0x01;
		irq_enable = data & 0x02;
		flip_screen_set(data & 0x08);
	} };
	
	public static InterruptPtr jb_interrupt = new InterruptPtr() { public int handler() 
	{
		if (irq_enable != 0)
			return interrupt.handler();
		else
			return ignore_interrupt.handler();
	} };
	
	public static InterruptPtr jb_interrupt_nmi = new InterruptPtr() { public int handler() 
	{
		if (nmi_enable != 0)
			return nmi_interrupt.handler();
		else
			return ignore_interrupt.handler();
	} };
	
	
	public static ReadHandlerPtr jailbrek_speech_r  = new ReadHandlerPtr() { public int handler(int offset) {
		return ( VLM5030_BSY()!=0 ? 1 : 0 );
	} };
	
	public static WriteHandlerPtr jailbrek_speech_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		/* bit 0 could be latch direction like in yiear */
		VLM5030_ST( ( data >> 1 ) & 1 );
		VLM5030_RST( ( data >> 2 ) & 1 );
	} };
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x07ff, colorram_r ),
		new Memory_ReadAddress( 0x0800, 0x0fff, videoram_r ),
		new Memory_ReadAddress( 0x1000, 0x10bf, MRA_RAM ), /* sprites */
		new Memory_ReadAddress( 0x10c0, 0x14ff, MRA_RAM ), /* ??? */
		new Memory_ReadAddress( 0x1500, 0x1fff, MRA_RAM ), /* work ram */
		new Memory_ReadAddress( 0x2000, 0x203f, MRA_RAM ), /* scroll registers */
		new Memory_ReadAddress( 0x3000, 0x307f, MRA_NOP ), /* related to sprites? */
		new Memory_ReadAddress( 0x3100, 0x3100, input_port_4_r ), /* DSW1 */
		new Memory_ReadAddress( 0x3200, 0x3200, input_port_5_r ), /* DSW2 */
		new Memory_ReadAddress( 0x3300, 0x3300, input_port_0_r ), /* coins, start */
		new Memory_ReadAddress( 0x3301, 0x3301, input_port_1_r ), /* joy1 */
		new Memory_ReadAddress( 0x3302, 0x3302, input_port_2_r ), /* joy2 */
		new Memory_ReadAddress( 0x3303, 0x3303, input_port_3_r ), /* DSW0 */
		new Memory_ReadAddress( 0x6000, 0x6000, jailbrek_speech_r ),
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x07ff, colorram_w, colorram ),
	    new Memory_WriteAddress( 0x0800, 0x0fff, videoram_w, videoram, videoram_size ),
	    new Memory_WriteAddress( 0x1000, 0x10bf, MWA_RAM, spriteram, spriteram_size ), /* sprites */
	    new Memory_WriteAddress( 0x10c0, 0x14ff, MWA_RAM ), /* ??? */
		new Memory_WriteAddress( 0x1500, 0x1fff, MWA_RAM ), /* work ram */
	    new Memory_WriteAddress( 0x2000, 0x203f, MWA_RAM, jailbrek_scroll_x ), /* scroll registers */
	    new Memory_WriteAddress( 0x2043, 0x2043, MWA_NOP ), /* ??? */
	    new Memory_WriteAddress( 0x2044, 0x2044, ctrl_w ), /* irq, nmi enable, screen flip */
	    new Memory_WriteAddress( 0x3000, 0x307f, MWA_RAM ), /* ??? */
		new Memory_WriteAddress( 0x3100, 0x3100, SN76496_0_w ), /* SN76496 data write */
		new Memory_WriteAddress( 0x3200, 0x3200, MWA_NOP ),	/* mirror of the previous? */
	    new Memory_WriteAddress( 0x3300, 0x3300, watchdog_reset_w ), /* watchdog */
		new Memory_WriteAddress( 0x4000, 0x4000, jailbrek_speech_w ), /* speech pins */
		new Memory_WriteAddress( 0x5000, 0x5000, VLM5030_data_w ), /* speech data */
	    new Memory_WriteAddress( 0x8000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	static InputPortPtr input_ports_jailbrek = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* IN0 - $3300 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	/* IN1 - $3301 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	/* IN2 - $3302 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START(); 	/* DSW0  - $3303 */
		PORT_DIPNAME( 0x0f, 0x0f, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x02, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x05, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x04, DEF_STR( "3C_2C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "4C_3C") );
		PORT_DIPSETTING(    0x0f, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "3C_4C") );
		PORT_DIPSETTING(    0x07, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0x0e, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x06, DEF_STR( "2C_5C") );
		PORT_DIPSETTING(    0x0d, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x0b, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x0a, DEF_STR( "1C_6C") );
		PORT_DIPSETTING(    0x09, DEF_STR( "1C_7C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Free_Play") );
		PORT_DIPNAME( 0xf0, 0xf0, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x20, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x50, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x80, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x40, DEF_STR( "3C_2C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "4C_3C") );
		PORT_DIPSETTING(    0xf0, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "3C_4C") );
		PORT_DIPSETTING(    0x70, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0xe0, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x60, DEF_STR( "2C_5C") );
		PORT_DIPSETTING(    0xd0, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0xc0, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0xb0, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0xa0, DEF_STR( "1C_6C") );
		PORT_DIPSETTING(    0x90, DEF_STR( "1C_7C") );
		PORT_DIPSETTING(    0x00, "Invalid?" );
	
		PORT_START(); 	/* DSW1  - $3100 */
		PORT_DIPNAME( 0x03, 0x01, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x03, "1" );
		PORT_DIPSETTING(    0x02, "2" );
		PORT_DIPSETTING(    0x01, "3" );
		PORT_DIPSETTING(    0x00, "5" );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x08, "30000 70000" );
		PORT_DIPSETTING(    0x00, "40000 80000" );
		PORT_DIPNAME( 0x30, 0x30, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x30, "Easy" );
		PORT_DIPSETTING(    0x20, "Normal" );
		PORT_DIPSETTING(    0x10, "Hard" );
		PORT_DIPSETTING(    0x00, "Very Hard" );
		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unused") );
		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 	/* DSW2  - $3200 */
		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(	0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(	0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x02, 0x02, "Upright Controls" );
		PORT_DIPSETTING(    0x02, "Single" );
		PORT_DIPSETTING(    0x00, "Dual" );
		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unused") );
		PORT_DIPSETTING(	0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(	0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unused") );
		PORT_DIPSETTING(	0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(	0x00, DEF_STR( "On") );
		PORT_BIT( 0xf0, IP_ACTIVE_LOW, IPT_UNUSED );
	INPUT_PORTS_END(); }}; 
	
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,	/* 8*8 characters */
		1024,	/* 1024 characters */
		4,	/* 4 bits per pixel */
		new int[] { 0, 1, 2, 3 },	/* the four bitplanes are packed in one nibble */
		new int[] { 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4 },
		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32 },
		32*8	/* every char takes 32 consecutive bytes */
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,	/* 16*16 sprites */
		512,	/* 512 sprites */
		4,	/* 4 bits per pixel */
		new int[] { 0, 1, 2, 3 },	/* the bitplanes are packed in one nibble */
		new int[] { 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4,
				32*8+0*4, 32*8+1*4, 32*8+2*4, 32*8+3*4, 32*8+4*4, 32*8+5*4, 32*8+6*4, 32*8+7*4 },
		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32,
				16*32, 17*32, 18*32, 19*32, 20*32, 21*32, 22*32, 23*32 },
		128*8	/* every sprite takes 128 consecutive bytes */
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,   0, 16 ), /* characters */
		new GfxDecodeInfo( REGION_GFX2, 0, spritelayout, 16*16, 16 ), /* sprites */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	static SN76496interface sn76496_interface = new SN76496interface
	(
		1,	/* 1 chip */
		new int[] { 1500000 },	/*  1.5 MHz ? (hand tuned) */
		new int[] { 100 }
	);
	
	static VLM5030interface vlm5030_interface = new VLM5030interface
	(
		3580000,    /* master clock */
		100,        /* volume       */
		REGION_SOUND1,	/* memory region of speech rom */
		0          /* memory size of speech rom */
        );
	
	static MachineDriver machine_driver_jailbrek = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
			    CPU_M6809,
			    3000000,        /* 3 MHz ??? */
			    readmem,writemem,null,null,
			    jb_interrupt,1,
			    jb_interrupt_nmi, 500 /* ? */
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,  /* frames per second, vblank duration */
		1, /* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 1*8, 31*8-1, 2*8, 30*8-1 ),
		gfxdecodeinfo,
		32,512,
		jailbrek_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		jailbrek_vh_start,
		generic_vh_stop,
		jailbrek_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_SN76496,
				sn76496_interface
			),
			new MachineSound(
				SOUND_VLM5030,
				vlm5030_interface
			)
		}
	);
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_jailbrek = new RomLoadPtr(){ public void handler(){ 
	    ROM_REGION( 2*0x10000, REGION_CPU1, 0 );    /* 64k for code + 64k for decrypted opcodes */
		ROM_LOAD( "jailb11d.bin", 0x8000, 0x4000, 0xa0b88dfd );
		ROM_LOAD( "jailb9d.bin",  0xc000, 0x4000, 0x444b7d8e );
	
	    ROM_REGION( 0x08000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "jailb4f.bin",  0x00000, 0x4000, 0xe3b7a226 );/* characters */
	    ROM_LOAD( "jailb5f.bin",  0x04000, 0x4000, 0x504f0912 );
	
	    ROM_REGION( 0x10000, REGION_GFX2, ROMREGION_DISPOSE );
	    ROM_LOAD( "jailb3e.bin",  0x00000, 0x4000, 0x0d269524 );/* sprites */
	    ROM_LOAD( "jailb4e.bin",  0x04000, 0x4000, 0x27d4f6f4 );
	    ROM_LOAD( "jailb5e.bin",  0x08000, 0x4000, 0x717485cb );
	    ROM_LOAD( "jailb3f.bin",  0x0c000, 0x4000, 0xe933086f );
	
		ROM_REGION( 0x0240, REGION_PROMS, 0 );
		ROM_LOAD( "jailbbl.cl2",  0x0000, 0x0020, 0xf1909605 );/* red & green */
		ROM_LOAD( "jailbbl.cl1",  0x0020, 0x0020, 0xf70bb122 );/* blue */
		ROM_LOAD( "jailbbl.bp2",  0x0040, 0x0100, 0xd4fe5c97 );/* char lookup */
		ROM_LOAD( "jailbbl.bp1",  0x0140, 0x0100, 0x0266c7db );/* sprites lookup */
	
		ROM_REGION( 0x2000, REGION_SOUND1, 0 );/* speech rom */
		ROM_LOAD( "jailb8c.bin",  0x0000, 0x2000, 0xd91d15e3 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_manhatan = new RomLoadPtr(){ public void handler(){ 
	    ROM_REGION( 2*0x10000, REGION_CPU1, 0 );    /* 64k for code + 64k for decrypted opcodes */
		ROM_LOAD( "507n03.9d",    0x8000, 0x4000, 0xe5039f7e );
		ROM_LOAD( "507n02.11d",   0xc000, 0x4000, 0x143cc62c );
	
	    ROM_REGION( 0x08000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "507j08.4f",    0x00000, 0x4000, 0x175e1b49 );/* characters */
	    ROM_LOAD( "jailb5f.bin",  0x04000, 0x4000, 0x504f0912 );
	
	    ROM_REGION( 0x10000, REGION_GFX2, ROMREGION_DISPOSE );
	    ROM_LOAD( "jailb3e.bin",  0x00000, 0x4000, 0x0d269524 );/* sprites */
	    ROM_LOAD( "jailb4e.bin",  0x04000, 0x4000, 0x27d4f6f4 );
	    ROM_LOAD( "jailb5e.bin",  0x08000, 0x4000, 0x717485cb );
	    ROM_LOAD( "jailb3f.bin",  0x0c000, 0x4000, 0xe933086f );
	
		ROM_REGION( 0x0240, REGION_PROMS, 0 );
		ROM_LOAD( "jailbbl.cl2",  0x0000, 0x0020, 0xf1909605 );/* red & green */
		ROM_LOAD( "jailbbl.cl1",  0x0020, 0x0020, 0xf70bb122 );/* blue */
		ROM_LOAD( "jailbbl.bp2",  0x0040, 0x0100, 0xd4fe5c97 );/* char lookup */
		ROM_LOAD( "jailbbl.bp1",  0x0140, 0x0100, 0x0266c7db );/* sprites lookup */
	
		ROM_REGION( 0x2000, REGION_SOUND1, 0 );/* speech rom */
		ROM_LOAD( "507p01.8c",    0x0000, 0x2000, 0x4a1da0b7 );
	ROM_END(); }}; 
	
	
	public static InitDriverPtr init_jailbrek = new InitDriverPtr() { public void handler()
	{
		konami1_decode();
	} };
	
	
	public static GameDriver driver_jailbrek	   = new GameDriver("1986"	,"jailbrek"	,"jailbrek.java"	,rom_jailbrek,null	,machine_driver_jailbrek	,input_ports_jailbrek	,init_jailbrek	,ROT0	,	"Konami", "Jail Break" );
	public static GameDriver driver_manhatan	   = new GameDriver("1986"	,"manhatan"	,"jailbrek.java"	,rom_manhatan,driver_jailbrek	,machine_driver_jailbrek	,input_ports_jailbrek	,init_jailbrek	,ROT0	,	"Konami", "Manhattan 24 Bunsyo (Japan)" );
}
