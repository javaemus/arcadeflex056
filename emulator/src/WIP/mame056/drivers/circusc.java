/***************************************************************************

Based on drivers from Juno First emulator by Chris Hardy (chrish@kcbbs.gen.nz)

To enter service mode, keep 1&2 pressed on reset

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static arcadeflex056.fucPtr.*;
import common.ptr.UBytePtr;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.memory.*;
import static mame056.drawgfxH.*;
import static mame056.driverH.*;
import static mame056.inptport.*;
import static mame056.inptportH.*;
import static mame056.memoryH.*;
import static mame056.palette.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame056.sound._2203intf.*;
import static mame056.sound._2203intfH.*;
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;
import static WIP.mame056.vidhrdw.circusc.*;
import static mame056.sound.sn76496.*;
import static mame056.sound.sn76496H.*;
import static mame056.vidhrdw.generic.*;
import static mame056.machine.konami.*;

public class circusc
{
    
        public static final int CIRCUSCHALIE_TIMER_RATE = (14318180/6144);
	
	public static ReadHandlerPtr circusc_sh_timer_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int clock;
	
		clock = (cpu_gettotalcycles()*4) / CIRCUSCHALIE_TIMER_RATE;
	
		return clock & 0xF;
	} };
	
	public static WriteHandlerPtr circusc_sh_irqtrigger_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_cause_interrupt(1,0xff);
	} };
	
	public static WriteHandlerPtr circusc_dac_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		DAC_data_w(0,data);
	} };
	
	public static WriteHandlerPtr circusc_coin_counter_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		coin_counter_w.handler(offset,data);
	} };
	
	
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x1000, 0x1000, input_port_0_r ), /* IO Coin */
		new Memory_ReadAddress( 0x1001, 0x1001, input_port_1_r ), /* P1 IO */
		new Memory_ReadAddress( 0x1002, 0x1002, input_port_2_r ), /* P2 IO */
		new Memory_ReadAddress( 0x1400, 0x1400, input_port_3_r ), /* DIP 1 */
		new Memory_ReadAddress( 0x1800, 0x1800, input_port_4_r ), /* DIP 2 */
		new Memory_ReadAddress( 0x2000, 0x39ff, MRA_RAM ),
		new Memory_ReadAddress( 0x6000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x0000, circusc_flipscreen_w ),
		new Memory_WriteAddress( 0x0001, 0x0001, interrupt_enable_w ),
		new Memory_WriteAddress( 0x0003, 0x0004, circusc_coin_counter_w ),  /* Coin counters */
		new Memory_WriteAddress( 0x0005, 0x0005, MWA_RAM, circusc_spritebank ),
		new Memory_WriteAddress( 0x0400, 0x0400, watchdog_reset_w ),
		new Memory_WriteAddress( 0x0800, 0x0800, soundlatch_w ),
		new Memory_WriteAddress( 0x0c00, 0x0c00, circusc_sh_irqtrigger_w ),  /* cause interrupt on audio CPU */
		new Memory_WriteAddress( 0x1c00, 0x1c00, MWA_RAM, circusc_scroll ),
		new Memory_WriteAddress( 0x2000, 0x2fff, MWA_RAM ),
		new Memory_WriteAddress( 0x3000, 0x33ff, circusc_colorram_w, circusc_colorram ),
		new Memory_WriteAddress( 0x3400, 0x37ff, circusc_videoram_w, circusc_videoram ),
		new Memory_WriteAddress( 0x3800, 0x38ff, MWA_RAM, spriteram_2 ),
		new Memory_WriteAddress( 0x3900, 0x39ff, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0x3a00, 0x3fff, MWA_RAM ),
		new Memory_WriteAddress( 0x6000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x3fff, MRA_ROM ),
		new Memory_ReadAddress( 0x4000, 0x43ff, MRA_RAM ),
		new Memory_ReadAddress( 0x6000, 0x6000, soundlatch_r ),
		new Memory_ReadAddress( 0x8000, 0x8000, circusc_sh_timer_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x3fff, MWA_ROM ),
		new Memory_WriteAddress( 0x4000, 0x43ff, MWA_RAM ),
		new Memory_WriteAddress( 0xa000, 0xa000, MWA_NOP ),    /* latch command for the 76496. We should buffer this */
										/* command and send it to the chip, but we just use */
										/* the triggers below because the program always writes */
										/* the same number here and there. */
		new Memory_WriteAddress( 0xa001, 0xa001, SN76496_0_w ),        /* trigger the 76496 to read the latch */
		new Memory_WriteAddress( 0xa002, 0xa002, SN76496_1_w ),        /* trigger the 76496 to read the latch */
		new Memory_WriteAddress( 0xa003, 0xa003, circusc_dac_w ),
		new Memory_WriteAddress( 0xa004, 0xa004, MWA_NOP ),            /* ??? */
		new Memory_WriteAddress( 0xa07c, 0xa07c, MWA_NOP ),            /* ??? */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	static InputPortPtr input_ports_circusc = new InputPortPtr(){ public void handler() { 
		PORT_START();       /* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START();       /* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
	
		PORT_START();       /* IN2 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_2WAY | IPF_COCKTAIL );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_2WAY | IPF_COCKTAIL );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_START();       /* DSW0 */
	
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
		PORT_DIPSETTING(    0x00, DEF_STR( "Free_Play") );
	
		PORT_START();       /* DSW1 */
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x03, "3" );
		PORT_DIPSETTING(    0x02, "4" );
		PORT_DIPSETTING(    0x01, "5" );
		PORT_DIPSETTING(    0x00, "7" );
		PORT_DIPNAME( 0x04, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(    0x08, "20000 70000" );
		PORT_DIPSETTING(    0x00, "30000 80000" );
		PORT_DIPNAME( 0x10, 0x10, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x60, 0x40, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x60, "Easy" );
		PORT_DIPSETTING(    0x40, "Normal" );
		PORT_DIPSETTING(    0x20, "Hard" );
		PORT_DIPSETTING(    0x00, "Hardest" );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,    /* 8*8 characters */
		512,    /* 512 characters */
		4,      /* 4 bits per pixel */
		new int[] { 0, 1, 2, 3 }, /* the four bitplanes are packed in one nibble */
		new int[] { 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4 },
		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32 },
		32*8    /* every char takes 8 consecutive bytes */
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,  /* 16*16 sprites */
		384,    /* 384 sprites */
		4,      /* 4 bits per pixel */
		new int[] { 0, 1, 2, 3 },        /* the bitplanes are packed */
		new int[] { 0*4, 1*4, 2*4, 3*4, 4*4, 5*4, 6*4, 7*4,
				8*4, 9*4, 10*4, 11*4, 12*4, 13*4, 14*4, 15*4 },
		new int[] { 0*4*16, 1*4*16, 2*4*16, 3*4*16, 4*4*16, 5*4*16, 6*4*16, 7*4*16,
				8*4*16, 9*4*16, 10*4*16, 11*4*16, 12*4*16, 13*4*16, 14*4*16, 15*4*16 },
		32*4*8    /* every sprite takes 128 consecutive bytes */
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,       0, 16 ),
		new GfxDecodeInfo( REGION_GFX2, 0, spritelayout, 16*16, 16 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	static SN76496interface sn76496_interface = new SN76496interface
	(
		2,      /* 2 chips */
		new int[] { 14318180/8, 14318180/8 },     /*  1.7897725 MHz */
		new int[] { 100, 100 }
	);
	
	static DACinterface dac_interface = new DACinterface
	(
		1,
		new int[] { 100 }
	);
	
	
	
	static MachineDriver machine_driver_circusc = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6809,
				2048000,        /* 2 MHz */
				readmem,writemem,null,null,
				interrupt,1
			),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				14318180/4,     /* Z80 Clock is derived from a 14.31818 MHz crystal */
				sound_readmem,sound_writemem,null,null,
				ignore_interrupt,1      /* interrupts are triggered by the main CPU */
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,       /* frames per second, vblank duration */
		1,      /* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 2*8, 30*8-1 ),
		gfxdecodeinfo,
		32,16*16+16*16,
		circusc_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		circusc_vh_start,
		null,
		circusc_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_SN76496,
				sn76496_interface
			),
			new MachineSound(
				SOUND_DAC,
				dac_interface
			)
		}
	);
	
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_circusc = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 2*0x10000, REGION_CPU1, 0 );    /* 64k for code + 64k for decrypted opcodes */
		ROM_LOAD( "s05",          0x6000, 0x2000, 0x48feafcf );
		ROM_LOAD( "q04",          0x8000, 0x2000, 0xc283b887 );
		ROM_LOAD( "q03",          0xa000, 0x2000, 0xe90c0e86 );
		ROM_LOAD( "q02",          0xc000, 0x2000, 0x4d847dc6 );
		ROM_LOAD( "q01",          0xe000, 0x2000, 0x18c20adf );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );    /* 64k for the audio CPU */
		ROM_LOAD( "cd05_l14.bin", 0x0000, 0x2000, 0x607df0fb );
		ROM_LOAD( "cd07_l15.bin", 0x2000, 0x2000, 0xa6ad30e1 );
	
		ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "a04_j12.bin",  0x0000, 0x2000, 0x56e5b408 );
		ROM_LOAD( "a05_k13.bin",  0x2000, 0x2000, 0x5aca0193 );
	
		ROM_REGION( 0x0c000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "e11_j06.bin",  0x0000, 0x2000, 0xdf0405c6 );
		ROM_LOAD( "e12_j07.bin",  0x2000, 0x2000, 0x23dfe3a6 );
		ROM_LOAD( "e13_j08.bin",  0x4000, 0x2000, 0x3ba95390 );
		ROM_LOAD( "e14_j09.bin",  0x6000, 0x2000, 0xa9fba85a );
		ROM_LOAD( "e15_j10.bin",  0x8000, 0x2000, 0x0532347e );
		ROM_LOAD( "e16_j11.bin",  0xa000, 0x2000, 0xe1725d24 );
	
		ROM_REGION( 0x0220, REGION_PROMS, 0 );
		ROM_LOAD( "a02_j18.bin",  0x0000, 0x020, 0x10dd4eaa );/* palette */
		ROM_LOAD( "c10_j16.bin",  0x0020, 0x100, 0xc244f2aa );/* character lookup table */
		ROM_LOAD( "b07_j17.bin",  0x0120, 0x100, 0x13989357 );/* sprite lookup table */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_circusc2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 2*0x10000, REGION_CPU1, 0 );    /* 64k for code + 64k for decrypted opcodes */
		ROM_LOAD( "h03_r05.bin",  0x6000, 0x2000, 0xed52c60f );
		ROM_LOAD( "h04_n04.bin",  0x8000, 0x2000, 0xfcc99e33 );
		ROM_LOAD( "h05_n03.bin",  0xa000, 0x2000, 0x5ef5b3b5 );
		ROM_LOAD( "h06_n02.bin",  0xc000, 0x2000, 0xa5a5e796 );
		ROM_LOAD( "h07_n01.bin",  0xe000, 0x2000, 0x70d26721 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );    /* 64k for the audio CPU */
		ROM_LOAD( "cd05_l14.bin", 0x0000, 0x2000, 0x607df0fb );
		ROM_LOAD( "cd07_l15.bin", 0x2000, 0x2000, 0xa6ad30e1 );
	
		ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "a04_j12.bin",  0x0000, 0x2000, 0x56e5b408 );
		ROM_LOAD( "a05_k13.bin",  0x2000, 0x2000, 0x5aca0193 );
	
		ROM_REGION( 0x0c000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "e11_j06.bin",  0x0000, 0x2000, 0xdf0405c6 );
		ROM_LOAD( "e12_j07.bin",  0x2000, 0x2000, 0x23dfe3a6 );
		ROM_LOAD( "e13_j08.bin",  0x4000, 0x2000, 0x3ba95390 );
		ROM_LOAD( "e14_j09.bin",  0x6000, 0x2000, 0xa9fba85a );
		ROM_LOAD( "e15_j10.bin",  0x8000, 0x2000, 0x0532347e );
		ROM_LOAD( "e16_j11.bin",  0xa000, 0x2000, 0xe1725d24 );
	
		ROM_REGION( 0x0220, REGION_PROMS, 0 );
		ROM_LOAD( "a02_j18.bin",  0x0000, 0x020, 0x10dd4eaa );/* palette */
		ROM_LOAD( "c10_j16.bin",  0x0020, 0x100, 0xc244f2aa );/* character lookup table */
		ROM_LOAD( "b07_j17.bin",  0x0120, 0x100, 0x13989357 );/* sprite lookup table */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_circuscc = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 2*0x10000, REGION_CPU1, 0 );    /* 64k for code + 64k for decrypted opcodes */
		ROM_LOAD( "cc_u05.h3",    0x6000, 0x2000, 0x964c035a );
		ROM_LOAD( "p04",          0x8000, 0x2000, 0xdd0c0ee7 );
		ROM_LOAD( "p03",          0xa000, 0x2000, 0x190247af );
		ROM_LOAD( "p02",          0xc000, 0x2000, 0x7e63725e );
		ROM_LOAD( "p01",          0xe000, 0x2000, 0xeedaa5b2 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );    /* 64k for the audio CPU */
		ROM_LOAD( "cd05_l14.bin", 0x0000, 0x2000, 0x607df0fb );
		ROM_LOAD( "cd07_l15.bin", 0x2000, 0x2000, 0xa6ad30e1 );
	
		ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "a04_j12.bin",  0x0000, 0x2000, 0x56e5b408 );
		ROM_LOAD( "a05_k13.bin",  0x2000, 0x2000, 0x5aca0193 );
	
		ROM_REGION( 0x0c000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "e11_j06.bin",  0x0000, 0x2000, 0xdf0405c6 );
		ROM_LOAD( "e12_j07.bin",  0x2000, 0x2000, 0x23dfe3a6 );
		ROM_LOAD( "e13_j08.bin",  0x4000, 0x2000, 0x3ba95390 );
		ROM_LOAD( "e14_j09.bin",  0x6000, 0x2000, 0xa9fba85a );
		ROM_LOAD( "e15_j10.bin",  0x8000, 0x2000, 0x0532347e );
		ROM_LOAD( "e16_j11.bin",  0xa000, 0x2000, 0xe1725d24 );
	
		ROM_REGION( 0x0220, REGION_PROMS, 0 );
		ROM_LOAD( "a02_j18.bin",  0x0000, 0x020, 0x10dd4eaa );/* palette */
		ROM_LOAD( "c10_j16.bin",  0x0020, 0x100, 0xc244f2aa );/* character lookup table */
		ROM_LOAD( "b07_j17.bin",  0x0120, 0x100, 0x13989357 );/* sprite lookup table */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_circusce = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 2*0x10000, REGION_CPU1, 0 );    /* 64k for code + 64k for decrypted opcodes */
		ROM_LOAD( "p05",          0x6000, 0x2000, 0x7ca74494 );
		ROM_LOAD( "p04",          0x8000, 0x2000, 0xdd0c0ee7 );
		ROM_LOAD( "p03",          0xa000, 0x2000, 0x190247af );
		ROM_LOAD( "p02",          0xc000, 0x2000, 0x7e63725e );
		ROM_LOAD( "p01",          0xe000, 0x2000, 0xeedaa5b2 );
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );    /* 64k for the audio CPU */
		ROM_LOAD( "cd05_l14.bin", 0x0000, 0x2000, 0x607df0fb );
		ROM_LOAD( "cd07_l15.bin", 0x2000, 0x2000, 0xa6ad30e1 );
	
		ROM_REGION( 0x04000, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "a04_j12.bin",  0x0000, 0x2000, 0x56e5b408 );
		ROM_LOAD( "a05_k13.bin",  0x2000, 0x2000, 0x5aca0193 );
	
		ROM_REGION( 0x0c000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "e11_j06.bin",  0x0000, 0x2000, 0xdf0405c6 );
		ROM_LOAD( "e12_j07.bin",  0x2000, 0x2000, 0x23dfe3a6 );
		ROM_LOAD( "e13_j08.bin",  0x4000, 0x2000, 0x3ba95390 );
		ROM_LOAD( "e14_j09.bin",  0x6000, 0x2000, 0xa9fba85a );
		ROM_LOAD( "e15_j10.bin",  0x8000, 0x2000, 0x0532347e );
		ROM_LOAD( "e16_j11.bin",  0xa000, 0x2000, 0xe1725d24 );
	
		ROM_REGION( 0x0220, REGION_PROMS, 0 );
		ROM_LOAD( "a02_j18.bin",  0x0000, 0x020, 0x10dd4eaa );/* palette */
		ROM_LOAD( "c10_j16.bin",  0x0020, 0x100, 0xc244f2aa );/* character lookup table */
		ROM_LOAD( "b07_j17.bin",  0x0120, 0x100, 0x13989357 );/* sprite lookup table */
	ROM_END(); }}; 
	
	
	public static InitDriverPtr init_circusc = new InitDriverPtr() { public void handler()
	{
		konami1_decode();
	} };
	
	
	public static GameDriver driver_circusc	   = new GameDriver("1984"	,"circusc"	,"circusc.java"	,rom_circusc,null	,machine_driver_circusc	,input_ports_circusc	,init_circusc	,ROT90	,	"Konami", "Circus Charlie" );
	public static GameDriver driver_circusc2	   = new GameDriver("1984"	,"circusc2"	,"circusc.java"	,rom_circusc2,driver_circusc	,machine_driver_circusc	,input_ports_circusc	,init_circusc	,ROT90	,	"Konami", "Circus Charlie (no level select)" );
	public static GameDriver driver_circuscc	   = new GameDriver("1984"	,"circuscc"	,"circusc.java"	,rom_circuscc,driver_circusc	,machine_driver_circusc	,input_ports_circusc	,init_circusc	,ROT90	,	"Konami (Centuri licence)", "Circus Charlie (Centuri)" );
	public static GameDriver driver_circusce	   = new GameDriver("1984"	,"circusce"	,"circusc.java"	,rom_circusce,driver_circusc	,machine_driver_circusc	,input_ports_circusc	,init_circusc	,ROT90	,	"Konami (Centuri licence)", "Circus Charlie (Centuri, earlier)" );
}
