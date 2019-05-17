/***************************************************************************

Issues:
-sound effects missing
-cpu speeds are guessed
-colours might be wrong in the night stage
***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.common.*;
import static mame056.mame.*;

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
import static mame056.memory.*;
import static mame056.sound.ay8910H.*;
import static mame056.sound.ay8910.*;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;

import static mame056.vidhrdw.generic.*;
import static WIP.mame056.vidhrdw.rollrace.*;
import static WIP.mame056.vidhrdw.wiz.*;

public class rollrace
{
	
	public static ReadHandlerPtr ra_fake_d800_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return 0x51;
	} };
	
	public static WriteHandlerPtr ra_fake_d800_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	/*	logerror("d900: %02X\n",data);*/
	} };
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0xc000, 0xcfff, MRA_RAM ),
		new Memory_ReadAddress( 0xd900, 0xd900, ra_fake_d800_r ), /* protection ??*/
		new Memory_ReadAddress( 0xe000, 0xe3ff, MRA_RAM ),
		new Memory_ReadAddress( 0xe400, 0xe47f, MRA_RAM ),
		new Memory_ReadAddress( 0xec00, 0xec0f, MRA_NOP ), /* Analog sound effects ??*/
		new Memory_ReadAddress( 0xf000, 0xf0ff, MRA_RAM ),
		new Memory_ReadAddress( 0xf800, 0xf800, input_port_0_r ),		/* player1*/
	        new Memory_ReadAddress( 0xf801, 0xf801, input_port_1_r ),		/* player2*/
		new Memory_ReadAddress( 0xf804, 0xf804, input_port_3_r ),
		new Memory_ReadAddress( 0xf802, 0xf802, input_port_2_r ),
		new Memory_ReadAddress( 0xf805, 0xf805, input_port_4_r ),
		new Memory_ReadAddress( 0xd806, 0xd806, MRA_NOP ), /* looks like a watchdog, bit4 checked*/
                new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0xc000, 0xcfff, MWA_RAM ),
		new Memory_WriteAddress( 0xd900, 0xd900, ra_fake_d800_w ), /* protection ?? */
		new Memory_WriteAddress( 0xe000, 0xe3ff, videoram_w, videoram, videoram_size ),
		new Memory_WriteAddress( 0xe400, 0xe47f, colorram_w, colorram ),
		new Memory_WriteAddress( 0xe800, 0xe800, soundlatch_w ),
		new Memory_WriteAddress( 0xec00, 0xec0f, MWA_NOP ), /* Analog sound effects ?? ec00 sound enable ?*/
		new Memory_WriteAddress( 0xf000, 0xf0ff, MWA_RAM , spriteram, spriteram_size ),
		new Memory_WriteAddress( 0xfc00, 0xfc00, rollrace_flipx_w ),
		new Memory_WriteAddress( 0xfc01, 0xfc01, interrupt_enable_w ),
		new Memory_WriteAddress( 0xfc02, 0xfc03, MWA_NOP ), /* coin counters */
		new Memory_WriteAddress( 0xfc04, 0xfc05, rollrace_charbank_w ),
		new Memory_WriteAddress( 0xfc06, 0xfc06, rollrace_spritebank_w ),
		new Memory_WriteAddress( 0xf400, 0xf400, rollrace_backgroundcolor_w ),
		new Memory_WriteAddress( 0xf801, 0xf801, rollrace_bkgpen_w ),
		new Memory_WriteAddress( 0xf802, 0xf802, rollrace_backgroundpage_w ),
		new Memory_WriteAddress( 0xf803, 0xf803, rollrace_flipy_w ),
                new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress readmem_snd[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
                new Memory_ReadAddress( 0x0000, 0x0fff, MRA_ROM ),
                new Memory_ReadAddress( 0x2000, 0x2fff, MRA_RAM ),
                new Memory_ReadAddress( 0x3000, 0x3000, soundlatch_r ),
                new Memory_ReadAddress(MEMPORT_MARKER, 0)
        };
	
	public static Memory_WriteAddress writemem_snd[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
                new Memory_WriteAddress( 0x0000, 0x0fff, MWA_ROM ),
                new Memory_WriteAddress( 0x2000, 0x2fff, MWA_RAM ),
                new Memory_WriteAddress( 0x3000, 0x3000, interrupt_enable_w ),
                new Memory_WriteAddress( 0x4000, 0x4000, AY8910_control_port_0_w ),
                new Memory_WriteAddress( 0x4001, 0x4001, AY8910_write_port_0_w ),
                new Memory_WriteAddress( 0x5000, 0x5000, AY8910_control_port_1_w ),
                new Memory_WriteAddress( 0x5001, 0x5001, AY8910_write_port_1_w ),
                new Memory_WriteAddress( 0x6000, 0x6000, AY8910_control_port_2_w ),
                new Memory_WriteAddress( 0x6001, 0x6001, AY8910_write_port_2_w ),
                new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	static InputPortPtr input_ports_rollrace = new InputPortPtr(){ public void handler() { 
	PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_8WAY );	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT| IPF_8WAY );	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP   | IPF_8WAY );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_8WAY );	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 );// Jump
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_BUTTON2 );// Punch
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT  | IPF_COCKTAIL |IPF_8WAY );	PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_COCKTAIL |IPF_8WAY );	PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP    | IPF_COCKTAIL |IPF_8WAY );	PORT_BIT( 0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN  | IPF_COCKTAIL |IPF_8WAY );	PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_COCKTAIL );// Jump
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_BUTTON2 | IPF_COCKTAIL );// Punch
		PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );	PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_HIGH,IPT_COIN1 );	PORT_BIT( 0x02, IP_ACTIVE_HIGH,IPT_COIN2 );	PORT_BIT( 0x04, IP_ACTIVE_HIGH,IPT_SERVICE1 );	PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING( 0x00, DEF_STR( "Off") );
		PORT_DIPSETTING( 0x08, DEF_STR( "On") );
		PORT_BIT( 0x10, IP_ACTIVE_HIGH, IPT_START1 );	PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_START2 );	PORT_SERVICE( 0x40, IP_ACTIVE_HIGH );	PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING( 0x00, DEF_STR( "Off") );
		PORT_DIPSETTING( 0x80, DEF_STR( "On") );
	
		PORT_START(); 
		PORT_DIPNAME( 0x07, 0x00, DEF_STR( "Coin_A") );
		PORT_DIPSETTING( 	0x07, DEF_STR( "6C_1C") );
		PORT_DIPSETTING( 	0x06, DEF_STR( "3C_1C") );
		PORT_DIPSETTING( 	0x04, DEF_STR( "2C_1C") );
		PORT_DIPSETTING( 	0x00, DEF_STR( "1C_1C") );
		PORT_DIPSETTING( 	0x05, DEF_STR( "2C_3C") );
		PORT_DIPSETTING( 	0x01, DEF_STR( "1C_2C") );
		PORT_DIPSETTING( 	0x02, DEF_STR( "1C_3C") );
		PORT_DIPSETTING( 	0x03, DEF_STR( "1C_6C") );
	
		PORT_DIPNAME( 0x38, 0x00, DEF_STR( "Coin_B") );
		PORT_DIPSETTING( 	0x38, DEF_STR( "6C_1C") );
		PORT_DIPSETTING( 	0x30, DEF_STR( "3C_1C") );
		PORT_DIPSETTING( 	0x20, DEF_STR( "2C_1C") );
		PORT_DIPSETTING( 	0x00, DEF_STR( "1C_1C") );
		PORT_DIPSETTING( 	0x28, DEF_STR( "2C_3C") );
		PORT_DIPSETTING( 	0x08, DEF_STR( "1C_2C") );
		PORT_DIPSETTING( 	0x10, DEF_STR( "1C_3C") );
		PORT_DIPSETTING( 	0x18, DEF_STR( "1C_6C") );
	
	/*	PORT_BIT( 0x40, IP_ACTIVE_HIGH , IPT_VBLANK ); freezes frame, could be vblank ?*/
		PORT_DIPNAME( 0x40, 0x00, "Freeze" );	PORT_DIPSETTING( 	0x00, DEF_STR( "Off") );
		PORT_DIPSETTING( 	0x40, DEF_STR( "On") );
	/*	PORT_DIPNAME( 0x80, 0x00, "Free Run" );*/
		PORT_BITX(    0x80, 0x00, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Invulnerability", 0, 0 );	PORT_DIPSETTING( 	0x00, DEF_STR( "Off") ); /* test mode, you are invulnerable */
		PORT_DIPSETTING( 	0x80, DEF_STR( "On") );	/* to 'static' objects */
	
		PORT_START(); 
		PORT_DIPNAME( 0x03, 0x01, DEF_STR( "Lives") );
		PORT_DIPSETTING( 	0x00, "2" );	PORT_DIPSETTING( 	0x01, "3" );	PORT_DIPSETTING( 	0x02, "5" );	PORT_DIPSETTING( 	0x03, "7" );	PORT_DIPNAME( 0x0c, 0x00, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING( 	0x04, "20000" );	PORT_DIPSETTING( 	0x08, "50000" );	PORT_DIPSETTING( 	0x0c, "100000" );	PORT_DIPSETTING( 	0x00, "None" );	PORT_DIPNAME( 0x30, 0x00, DEF_STR( "Difficulty") );
		PORT_DIPSETTING( 	0x00, "A" );	PORT_DIPSETTING( 	0x10, "B" );	PORT_DIPSETTING( 	0x20, "C" );	PORT_DIPSETTING( 	0x30, "D" );	PORT_DIPNAME( 0x40, 0x00, DEF_STR("Cabinet"));
		PORT_DIPSETTING( 	0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING( 	0x40, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0x80, 0x00, DEF_STR( "Unknown") );
		PORT_DIPSETTING( 0x00, DEF_STR( "Off") );
		PORT_DIPSETTING( 0x80, DEF_STR( "On") );
	INPUT_PORTS_END(); }}; 
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,	/* 8*8 characters */
			256,	/* 256 characters */
			3,	  /* 3 bits per pixel */
			new int[] { 0,1024*8*8, 2*1024*8*8 }, /* the two bitplanes are separated */
		new int[] { 0,1,2,3,4,5,6,7 },
		new int[] { 7*8, 6*8, 5*8, 4*8, 3*8, 2*8, 1*8, 0*8 },
	
	
		8*8	/* every char takes 8 consecutive bytes */
	);
	static GfxLayout charlayout2 = new GfxLayout
	(
		8,8,	/* 8*8 characters */
			1024,	/* 1024 characters */
			3,	  /* 3 bits per pixel */
			new int[] { 0,1024*8*8, 2*1024*8*8 }, /* the two bitplanes are separated */
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
		new int[] { 7*8, 6*8, 5*8, 4*8, 3*8, 2*8, 1*8, 0*8 },
	
	//	new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
	
		8*8	/* every char takes 8 consecutive bytes */
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
			32,32,  /* 32*32 sprites */
			64,	/* 64 sprites */
		3,	  /* 3 bits per pixel */
			new int[] { 0x4000*8, 0x2000*8, 0 }, /* the three bitplanes are separated */
		new int[] { 0, 1, 2, 3, 4, 5, 6, 7,
			 8*8+0, 8*8+1, 8*8+2, 8*8+3, 8*8+4, 8*8+5, 8*8+6, 8*8+7,16*8,16*8+1,16*8+2,16*8+3,16*8+4,16*8+5,16*8+6,16*8+7,24*8,24*8+1,24*8+2,24*8+3,24*8+4,24*8+5,24*8+6,24*8+7},
	
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
			  32*8, 33*8, 34*8, 35*8, 36*8, 37*8, 38*8, 39*8,
			  64*8,65*8,66*8,67*8,68*8,69*8,70*8,71*8, 96*8,97*8,98*8,99*8,100*8,101*8,102*8,103*8 },
	
			32*32	 /* every sprite takes 128 consecutive bytes */
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
			new GfxDecodeInfo( REGION_GFX1, 0x0000, charlayout,	0,	32 ), /* foreground */
			new GfxDecodeInfo( REGION_GFX1, 0x0800, charlayout,	0,	32 ),
			new GfxDecodeInfo( REGION_GFX1, 0x1000, charlayout,	0,	32 ),
			new GfxDecodeInfo( REGION_GFX1, 0x1800, charlayout,	0,	32 ),
			new GfxDecodeInfo( REGION_GFX2, 0x0000, charlayout2,	0, 	32 ), /* for the road */
			new GfxDecodeInfo( REGION_GFX3, 0x0000, spritelayout,	0, 	32 ), /* sprites */
			new GfxDecodeInfo( REGION_GFX4, 0x0000, spritelayout,	0,	32 ),
			new GfxDecodeInfo( REGION_GFX5, 0x0000, spritelayout,	0,	32 ),
	
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	static AY8910interface ra_ay8910_interface = new AY8910interface
	(
		3,	  	/* 3 chips */
		14318000/8,	/* 1.78975 MHz */
		new int[] { 10,10,10 },
		new ReadHandlerPtr[] { null, null, null },
		new ReadHandlerPtr[] { null, null, null },
		new WriteHandlerPtr[] { null, null, null },
		new WriteHandlerPtr[] { null, null, null }
	);
	
	static MachineDriver machine_driver_rollrace = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				18432000/6,			/* ?? */
				readmem, writemem, null, null,
				nmi_interrupt, 1
                        ),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				14318000/6,			/* ?? */
				readmem_snd, writemem_snd, null, null,
				nmi_interrupt, 3
                        ),
	
	
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,
		null,
	
		/* video hardware */
		256, 256, new rectangle( 0,255,16, 255-16 ),
		gfxdecodeinfo,
		256, 32*8,
		wiz_vh_convert_color_prom,
		VIDEO_TYPE_RASTER,
		null,
		generic_vh_start,
		generic_vh_stop,
		rollrace_vh_screenrefresh,
	
		/* sound hardware */
		0, 0, 0, 0,
		new MachineSound[] {
			new MachineSound(
				SOUND_AY8910,
				ra_ay8910_interface
                        )
		}
	);
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_fightrol = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1,0 );/* 64k for code */
		ROM_LOAD( "4.8k", 0x0000, 0x2000, 0xefa2f430 );	ROM_LOAD( "5.8h", 0x2000, 0x2000, 0x2497d9f6 );	ROM_LOAD( "6.8f", 0x4000, 0x2000, 0xf39727b9 );	ROM_LOAD( "7.8d", 0x6000, 0x2000, 0xee65b728 );
		ROM_REGION( 0x6000,REGION_GFX1,ROMREGION_DISPOSE );/* characters */
		ROM_LOAD( "3.7m", 0x0000, 0x2000, 0xca4f353c );	ROM_LOAD( "2.8m", 0x2000, 0x2000, 0x93786171 );	ROM_LOAD( "1.9m", 0x4000, 0x2000, 0xdc072be1 );
		ROM_REGION( 0x6000, REGION_GFX2,ROMREGION_DISPOSE );/* road graphics */
		ROM_LOAD ( "6.20k", 0x0000, 0x2000, 0x003d7515 );	ROM_LOAD ( "7.18k", 0x2000, 0x2000, 0x27843afa );	ROM_LOAD ( "5.20f", 0x4000, 0x2000, 0x51dd0108 );
		ROM_REGION( 0x6000, REGION_GFX3,ROMREGION_DISPOSE );	/* sprite bank 0*/
		ROM_LOAD ( "8.17m",  0x0000, 0x2000, 0x08ad783e );	ROM_LOAD ( "9.17r",  0x2000, 0x2000, 0x69b23461 );	ROM_LOAD ( "10.17t", 0x4000, 0x2000, 0xba6ccd8c );
		ROM_REGION( 0x6000, REGION_GFX4,ROMREGION_DISPOSE );	/* sprite bank 1*/
		ROM_LOAD ( "11.18m", 0x0000, 0x2000, 0x06a5d849 );	ROM_LOAD ( "12.18r", 0x2000, 0x2000, 0x569815ef );	ROM_LOAD ( "13.18t", 0x4000, 0x2000, 0x4f8af872 );
		ROM_REGION( 0x6000, REGION_GFX5,ROMREGION_DISPOSE );/* sprite bank 2*/
		ROM_LOAD ( "14.19m", 0x0000, 0x2000, 0x93f3c649 );	ROM_LOAD ( "15.19r", 0x2000, 0x2000, 0x5b3d87e4 );	ROM_LOAD ( "16.19u", 0x4000, 0x2000, 0xa2c24b64 );
		ROM_REGION( 0x8000, REGION_USER1,ROMREGION_NODISPOSE );/* road layout */
		ROM_LOAD ( "1.17a", 0x0000, 0x2000, 0xf0fa72fc );	ROM_LOAD ( "3.18b", 0x2000, 0x2000, 0x954268f7 );	ROM_LOAD ( "2.17d", 0x4000, 0x2000, 0x2e38bb0e );	ROM_LOAD ( "4.18d", 0x6000, 0x2000, 0x3d9e16ab );
		ROM_REGION( 0x300, REGION_PROMS,ROMREGION_NODISPOSE );/* colour */
		ROM_LOAD("tbp24s10.7u", 0x0000, 0x0100, 0x9d199d33 );	ROM_LOAD("tbp24s10.7t", 0x0100, 0x0100, 0xc0426582 );	ROM_LOAD("tbp24s10.6t", 0x0200, 0x0100, 0xc096e05c );
		ROM_REGION( 0x10000,REGION_CPU2,0 );	ROM_LOAD( "8.6f", 0x0000, 0x1000, 0x6ec3c545 );ROM_END(); }}; 
	
	static RomLoadPtr rom_rollrace = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1,0 );/* 64k for code */
		ROM_LOAD( "w1.8k", 0x0000, 0x2000, 0xc0bd3cf3 );	ROM_LOAD( "w2.8h", 0x2000, 0x2000, 0xc1900a75 );	ROM_LOAD( "w3.8f", 0x4000, 0x2000, 0x16ceced6 );	ROM_LOAD( "w4.8d", 0x6000, 0x2000, 0xae826a96 );
		ROM_REGION( 0x6000,REGION_GFX1,ROMREGION_DISPOSE );/* characters */
		ROM_LOAD( "w3.7m", 0x0000, 0x2000, 0xf9970aae );	ROM_LOAD( "w2.8m", 0x2000, 0x2000, 0x80573091 );	ROM_LOAD( "w1.9m", 0x4000, 0x2000, 0xb37effd8 );
		ROM_REGION( 0x6000, REGION_GFX2,ROMREGION_DISPOSE );/* road graphics */
		ROM_LOAD ( "6.20k", 0x0000, 0x2000, 0x003d7515 );	ROM_LOAD ( "7.18k", 0x2000, 0x2000, 0x27843afa );	ROM_LOAD ( "5.20f", 0x4000, 0x2000, 0x51dd0108 );
		ROM_REGION( 0x6000, REGION_GFX3,ROMREGION_DISPOSE );	/* sprite bank 0*/
		ROM_LOAD ( "w8.17m",  0x0000, 0x2000, 0xe2afe3a3 );	ROM_LOAD ( "w9.17p",  0x2000, 0x2000, 0x8a8e6b62 );	ROM_LOAD ( "w10.17t", 0x4000, 0x2000, 0x70bf7b23 );
		ROM_REGION( 0x6000, REGION_GFX4,ROMREGION_DISPOSE );	/* sprite bank 1*/
		ROM_LOAD ( "11.18m", 0x0000, 0x2000, 0x06a5d849 );	ROM_LOAD ( "12.18r", 0x2000, 0x2000, 0x569815ef );	ROM_LOAD ( "13.18t", 0x4000, 0x2000, 0x4f8af872 );
		ROM_REGION( 0x6000, REGION_GFX5,ROMREGION_DISPOSE );/* sprite bank 2*/
		ROM_LOAD ( "14.19m", 0x0000, 0x2000, 0x93f3c649 );	ROM_LOAD ( "15.19r", 0x2000, 0x2000, 0x5b3d87e4 );	ROM_LOAD ( "16.19u", 0x4000, 0x2000, 0xa2c24b64 );
		ROM_REGION( 0x8000, REGION_USER1,ROMREGION_NODISPOSE );/* road layout */
		ROM_LOAD ( "1.17a", 0x0000, 0x2000, 0xf0fa72fc );	ROM_LOAD ( "3.18b", 0x2000, 0x2000, 0x954268f7 );	ROM_LOAD ( "2.17d", 0x4000, 0x2000, 0x2e38bb0e );	ROM_LOAD ( "4.18d", 0x6000, 0x2000, 0x3d9e16ab );
		ROM_REGION( 0x300, REGION_PROMS,ROMREGION_NODISPOSE );/* colour */
		ROM_LOAD("tbp24s10.7u", 0x0000, 0x0100, 0x9d199d33 );	ROM_LOAD("tbp24s10.7t", 0x0100, 0x0100, 0xc0426582 );	ROM_LOAD("tbp24s10.6t", 0x0200, 0x0100, 0xc096e05c );
		ROM_REGION( 0x10000,REGION_CPU2,0 );	ROM_LOAD( "8.6f", 0x0000, 0x1000, 0x6ec3c545 );ROM_END(); }}; 
	
	
	
	public static GameDriver driver_fightrol	   = new GameDriver("1983"	,"fightrol"	,"rollrace.java"	,rom_fightrol,null	,machine_driver_rollrace	,input_ports_rollrace	,null	,ROT270	,	"[Kaneko] (Taito license)", "Fighting Roller", GAME_IMPERFECT_SOUND|GAME_IMPERFECT_COLORS );
	public static GameDriver driver_rollrace	   = new GameDriver("1983"	,"rollrace"	,"rollrace.java"	,rom_rollrace,driver_fightrol	,machine_driver_rollrace	,input_ports_rollrace	,null	,ROT270	,	"[Kaneko] (Williams license)", "Roller Aces", GAME_IMPERFECT_SOUND|GAME_IMPERFECT_COLORS );
}
