/****************************************

TOYPOP
1986 Namco

driver by Edgardo E. Contini Salvan (pag2806@iperbole.bologna.it)

TOYPOP uses a 6809 main CPU,
another 6809 for the sound and
a 68000 to create the background image.

****************************************/

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
import static mame056.sound.sn76496.*;
import static mame056.sound.sn76496H.*;

import static mame056.vidhrdw.generic.*;
import static WIP.mame056.vidhrdw.toypop.*;
import static WIP.mame056.machine.toypop.*;
import static mame056.sound.namco.*;
import static mame056.sound.namcoH.*;

public class toypop
{
	
	
	/*************************************
	 *
	 *	Main CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress readmem_mainCPU[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x7fff, MRA_RAM ),								/* RAM everywhere else */
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),								/* ROM code */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem_mainCPU[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x03ff, MWA_RAM, videoram, videoram_size ),		/* video RAM */
		new Memory_WriteAddress( 0x0400, 0x07ff, MWA_RAM, colorram ),						/* color RAM */
		new Memory_WriteAddress( 0x0800, 0x0f7f, MWA_RAM ),								/* general RAM, area 1 */
		new Memory_WriteAddress( 0x0f80, 0x0fff, MWA_RAM, spriteram, spriteram_size ),	/* sprite RAM, area 1 */
		new Memory_WriteAddress( 0x1000, 0x177f, MWA_RAM ),								/* general RAM, area 2 */
		new Memory_WriteAddress( 0x1780, 0x17ff, MWA_RAM, spriteram_2 ),					/* sprite RAM, area 2 */
		new Memory_WriteAddress( 0x1800, 0x1f7f, MWA_RAM ),								/* general RAM, area 3 */
		new Memory_WriteAddress( 0x1f80, 0x1fff, MWA_RAM, spriteram_3 ),					/* sprite RAM, area 3 */
		new Memory_WriteAddress( 0x2800, 0x2fff, MWA_RAM, toypop_m68000_sharedram ),		/* shared RAM with the 68000 CPU */
		new Memory_WriteAddress( 0x7000, 0x7000, toypop_main_interrupt_enable_w ),			/* enable interrupt ??? */
		new Memory_WriteAddress( 0x7800, 0x7800, toypop_main_interrupt_disable_w ),		/* disable interrupt ??? */
		new Memory_WriteAddress( 0x8000, 0x8000, toypop_m68000_clear_w ),					/* reset 68000 */
		new Memory_WriteAddress( 0x8800, 0x8800, toypop_m68000_assert_w ),					/* reset 68000 */
		new Memory_WriteAddress( 0x9000, 0x9000, toypop_sound_clear_w ),					/* sound CPU reset */
		new Memory_WriteAddress( 0x9800, 0x9800, toypop_sound_assert_w ),					/* sound CPU reset */
		new Memory_WriteAddress( 0xa000, 0xa001, toypop_palettebank_w ),					/* background image palette */
		new Memory_WriteAddress( 0x8000, 0xffff, MWA_ROM ),								/* ROM code */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	/*************************************
	 *
	 *	Sound CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress readmem_soundCPU[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0040, 0x03ff, toypop_sound_sharedram_r ),		/* shared RAM with the main CPU */
		new Memory_ReadAddress( 0xe000, 0xffff, MRA_ROM ),				/* ROM code */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem_soundCPU[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	
                new Memory_WriteAddress( 0x0000, 0x003f, mappy_sound_w, namco_soundregs ),	/* sound control registers */
		new Memory_WriteAddress( 0x0040, 0x03ff, toypop_sound_sharedram_w ),					/* shared RAM with the main CPU */
		new Memory_WriteAddress( 0x2000, 0x2000, toypop_sound_interrupt_disable_w ),			/* interrupt disable ??? */
		new Memory_WriteAddress( 0x4000, 0x4000, toypop_sound_interrupt_enable_w ),			/* interrupt enable */
		new Memory_WriteAddress( 0x6000, 0x6000, MWA_NOP ),							/* watchdog ??? */
		new Memory_WriteAddress( 0xe000, 0xffff, MWA_ROM ),							/* ROM code */
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	/*************************************
	 *
	 *	68k CPU memory handlers
	 *
	 *************************************/
	
	public static Memory_ReadAddress readmem_68k[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x000000, 0x007fff, MRA_ROM ),				// ROM code
		new Memory_ReadAddress( 0x080000, 0x0bffff, MRA_RAM ),				// RAM
		new Memory_ReadAddress( 0x100000, 0x100fff, toypop_m68000_sharedram_r ),		// shared RAM with the main CPU
		new Memory_ReadAddress( 0x180000, 0x187fff, toypop_merged_background_r ),	// RAM merged with the background image
		new Memory_ReadAddress( 0x190000, 0x1dffff, MRA_RAM ),				// RAM containing the background image
                new Memory_ReadAddress(MEMPORT_MARKER, 0)
        };
	
	public static Memory_WriteAddress writemem_68k[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	
                new Memory_WriteAddress( 0x000000, 0x007fff, MWA_ROM ),						/* ROM code */
		new Memory_WriteAddress( 0x080000, 0x0bffff, MWA_RAM ),						/* RAM */
		new Memory_WriteAddress( 0x100000, 0x100fff, toypop_m68000_sharedram_w ),		/* shared RAM with the main CPU */
		new Memory_WriteAddress( 0x180000, 0x187fff, toypop_merged_background_w ),		/* RAM that has to be merged with the background image */
		new Memory_WriteAddress( 0x18fffc, 0x18ffff, toypop_flipscreen_w ),			/* flip mode */
		new Memory_WriteAddress( 0x190000, 0x1dffff, MWA_RAM, toypop_bg_image ),			/* RAM containing the background image */
		new Memory_WriteAddress( 0x300000, 0x300001, toypop_m68000_interrupt_enable_w ),		/* interrupt enable */
		new Memory_WriteAddress( 0x380000, 0x380001, toypop_m68000_interrupt_disable_w ),		/* interrupt disable */
                new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	/*************************************
	 *
	 *	Port definitions
	 *
	 *************************************/
	
	static InputPortPtr input_ports_toypop = new InputPortPtr(){ public void handler() { 
	// FAKE
		/* The player inputs and the dipswitches are not memory mapped, they are handled by an I/O chip. */
		/* These fake input ports are read by toypop_customio_r() */
		PORT_START(); 	// IN0
		PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_COIN1);	PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_COIN2);	PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_4WAY );	PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY );	PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_4WAY );	PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_4WAY );	PORT_BIT(0x0c, IP_ACTIVE_HIGH, IPT_UNKNOWN);
		PORT_START(); 	// IN1
		PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_JOYSTICK_UP | IPF_4WAY | IPF_PLAYER2);	PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_JOYSTICK_RIGHT | IPF_4WAY | IPF_PLAYER2);	PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_JOYSTICK_DOWN | IPF_4WAY | IPF_PLAYER2);	PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_JOYSTICK_LEFT | IPF_4WAY | IPF_PLAYER2);	PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_BUTTON1);	PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_BUTTON1 | IPF_PLAYER2);	PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_START1);	PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_START2);
		PORT_START(); 	// DSW0
		PORT_DIPNAME(0x03, 0x00, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(   0x03, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(   0x02, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(   0x00, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(   0x01, DEF_STR( "1C_2C") );
		PORT_DIPNAME(0x04, 0x04, DEF_STR( "Flip_Screen") );
		PORT_DIPSETTING(   0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x00, DEF_STR( "On") );
		PORT_SERVICE(0x08, IP_ACTIVE_HIGH );	PORT_DIPNAME(0x10, 0x00, "Freeze" );	PORT_DIPSETTING(   0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x10, DEF_STR( "On") );
		PORT_DIPNAME(0x20, 0x00, "Level Select" );	PORT_DIPSETTING(   0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x20, DEF_STR( "On") );
		PORT_DIPNAME(0x40, 0x00, "2p play" );	PORT_DIPSETTING(   0x00, "2 Credits" );	PORT_DIPSETTING(   0x40, "1 Credit" );	PORT_DIPNAME(0x80, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(   0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x00, DEF_STR( "On") );
	
		PORT_START(); 	// DSW1
		PORT_DIPNAME(0x01, 0x00, "Entering" );// ???
		PORT_DIPSETTING(   0x01, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x00, DEF_STR( "On") );
		PORT_DIPNAME(0x06, 0x00, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(   0x02, "Easy" );	PORT_DIPSETTING(   0x00, "Normal" );	PORT_DIPSETTING(   0x04, "Hard" );	PORT_DIPSETTING(   0x06, "Very hard" );	PORT_DIPNAME(0x08, 0x00, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(   0x00, "Every 15000 points" );	PORT_DIPSETTING(   0x08, "Every 20000 points" );	PORT_DIPNAME(0x30, 0x00, DEF_STR( "Lives") );
		PORT_DIPSETTING(   0x10, "1" );	PORT_DIPSETTING(   0x20, "2" );	PORT_DIPSETTING(   0x00, "3" );	PORT_DIPSETTING(   0x30, "5" );	PORT_DIPNAME(0xc0, 0x00, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(   0xc0, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(   0x80, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(   0x00, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(   0x40, DEF_STR( "1C_2C") );
	INPUT_PORTS_END(); }}; 
	
	
	static InputPortPtr input_ports_liblrabl = new InputPortPtr(){ public void handler() { 
	// FAKE
		/* The player inputs and the dipswitches are not memory mapped, they are handled by an I/O chip. */
		/* These fake input ports are read by liblrabl_customio_r() */
		PORT_START();       /* DSW0 */
		PORT_DIPNAME(0x01, 0x00, "Freeze" );	PORT_DIPSETTING(   0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x01, DEF_STR( "On") );
		PORT_DIPNAME(0x02, 0x00, "Rnd" );	PORT_DIPSETTING(   0x00, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x02, DEF_STR( "On") );
		PORT_DIPNAME(0x04, 0x00, DEF_STR( "Demo_Sounds") );
		PORT_DIPSETTING(   0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x00, DEF_STR( "On") );
		PORT_DIPNAME(0x18, 0x00, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(   0x18, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(   0x00, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(   0x10, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(   0x08, DEF_STR( "1C_7C") );
		PORT_DIPNAME(0x20, 0x00, "Practice" );	PORT_DIPSETTING(   0x20, DEF_STR( "Off") );
		PORT_DIPSETTING(   0x00, DEF_STR( "On") );
		PORT_DIPNAME(0xc0, 0x00, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(   0x00, "A" );	PORT_DIPSETTING(   0x80, "B" );	PORT_DIPSETTING(   0x40, "C" );	PORT_DIPSETTING(   0xc0, "D" );
		PORT_START();       /* DSW1 */
		PORT_DIPNAME(0x03, 0x00, DEF_STR( "Lives") );
		PORT_DIPSETTING(   0x01, "1" );	PORT_DIPSETTING(   0x03, "2" );	PORT_DIPSETTING(   0x00, "3" );	PORT_DIPSETTING(   0x02, "5" );	// TODO: bonus scores are different for 5 lives
		PORT_DIPNAME(0x1c, 0x00, DEF_STR( "Bonus_Life") );
		PORT_DIPSETTING(   0x18, "40k 120k and every 120k" );	PORT_DIPSETTING(   0x04, "40k 150k and every 150k" );	PORT_DIPSETTING(   0x00, "40k 120k 200k 400k..." );	PORT_DIPSETTING(   0x10, "40k 140k 250k 400k..." );	PORT_DIPSETTING(   0x08, "50k 150k 300k 500k..." );	PORT_DIPSETTING(   0x0c, "40k 120k 200k" );	PORT_DIPSETTING(   0x14, "50k 150k 300k" );	PORT_DIPSETTING(   0x1c, "None" );	PORT_DIPNAME(0xe0, 0x00, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(   0x60, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(   0x20, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(   0xe0, DEF_STR( "3C_2C") );
		PORT_DIPSETTING(   0x00, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(   0xa0, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(   0x80, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(   0x40, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(   0xc0, DEF_STR( "1C_6C") );
	
		PORT_START();       /* DSW2 */
		PORT_BIT(0x03, IP_ACTIVE_HIGH, IPT_UNKNOWN);	PORT_DIPNAME(0x04, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(   0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(   0x04, DEF_STR( "Cocktail") );
		PORT_SERVICE(0x08, IP_ACTIVE_HIGH );	PORT_BIT(0xf0, IP_ACTIVE_HIGH, IPT_UNKNOWN );
		PORT_START(); 	// IN0
		PORT_BIT_IMPULSE(0x01, IP_ACTIVE_HIGH, IPT_COIN1, 1);	PORT_BIT_IMPULSE(0x02, IP_ACTIVE_HIGH, IPT_COIN2, 1);	PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_START1);	PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_START2);	PORT_BIT(0xcc, IP_ACTIVE_HIGH, IPT_UNKNOWN);
		PORT_START(); 	// IN1
		PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_JOYSTICKLEFT_UP | IPF_8WAY );	PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_JOYSTICKLEFT_RIGHT | IPF_8WAY );	PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_JOYSTICKLEFT_DOWN | IPF_8WAY );	PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_JOYSTICKLEFT_LEFT | IPF_8WAY );	PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICKRIGHT_UP | IPF_8WAY );	PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICKRIGHT_RIGHT | IPF_8WAY );	PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_JOYSTICKRIGHT_DOWN | IPF_8WAY );	PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_JOYSTICKRIGHT_LEFT | IPF_8WAY );
		PORT_START(); 	// IN2
		PORT_BIT(0x01, IP_ACTIVE_HIGH, IPT_JOYSTICKLEFT_UP | IPF_8WAY | IPF_COCKTAIL);	PORT_BIT(0x02, IP_ACTIVE_HIGH, IPT_JOYSTICKLEFT_RIGHT | IPF_8WAY | IPF_COCKTAIL);	PORT_BIT(0x04, IP_ACTIVE_HIGH, IPT_JOYSTICKLEFT_DOWN | IPF_8WAY | IPF_COCKTAIL);	PORT_BIT(0x08, IP_ACTIVE_HIGH, IPT_JOYSTICKLEFT_LEFT | IPF_8WAY | IPF_COCKTAIL);	PORT_BIT(0x10, IP_ACTIVE_HIGH, IPT_JOYSTICKRIGHT_UP | IPF_8WAY | IPF_COCKTAIL);	PORT_BIT(0x20, IP_ACTIVE_HIGH, IPT_JOYSTICKRIGHT_RIGHT | IPF_8WAY | IPF_COCKTAIL);	PORT_BIT(0x40, IP_ACTIVE_HIGH, IPT_JOYSTICKRIGHT_DOWN | IPF_8WAY | IPF_COCKTAIL);	PORT_BIT(0x80, IP_ACTIVE_HIGH, IPT_JOYSTICKRIGHT_LEFT | IPF_8WAY | IPF_COCKTAIL);INPUT_PORTS_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	Graphics layouts
	 *
	 *************************************/
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,1),
		2,
		new int[] { 0, 4 },
		new int[] { 8*8+0, 8*8+1, 8*8+2, 8*8+3, 0, 1, 2, 3 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		16*8
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,1),
		2,
		new int[] { 0, 4 },
		new int[] { 0, 1, 2, 3, 8*8, 8*8+1, 8*8+2, 8*8+3, 16*8+0, 16*8+1, 16*8+2, 16*8+3,
		24*8+0, 24*8+1, 24*8+2, 24*8+3 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
		32 * 8, 33 * 8, 34 * 8, 35 * 8, 36 * 8, 37 * 8, 38 * 8, 39 * 8 },
		64*8
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,      0, 128 ),
		new GfxDecodeInfo( REGION_GFX2, 0, spritelayout, 64*4, 256 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	/*************************************
	 *
	 *	Sound interfaces
	 *
	 *************************************/
	
	static namco_interface namco_interface = new namco_interface
	(
		24000,	/* sample rate */
		8,		/* number of voices */
		100,	/* playback volume */
		REGION_SOUND1	/* memory region */
        );
	
	
	
	/*************************************
	 *
	 *	Machine driver
	 *
	 *************************************/
	
	static MachineDriver machine_driver_toypop = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(		
			
				CPU_M6809,
				1536000,	/* 1.536 MHz (measured on Libble Rabble board) */
				readmem_mainCPU,writemem_mainCPU,null,null,
				toypop_main_interrupt,1
			),
			new MachineCPU(
				CPU_M6809 | CPU_AUDIO_CPU,
				1536000,	/* 1.536 MHz (measured on Libble Rabble board) */
				readmem_soundCPU,writemem_soundCPU,null,null,
				toypop_sound_interrupt,1
                        ),
			new MachineCPU(
				CPU_M68000,
				6144000,	/* 6.144 MHz (measured on Libble Rabble board) */
				readmem_68k,writemem_68k,null,null,
				toypop_m68000_interrupt,1
                        )
                },
		60.606060f, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		100,    /* 100 CPU slices per frame - an high value to ensure proper */
				/* synchronization of the CPUs */
		toypop_init_machine,
	
		/* video hardware */
		18*16, 14*16, new rectangle( 0*16, 18*16-1, 0*16, 14*16-1 ),
		gfxdecodeinfo,
		256,256+256,
		toypop_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		null,
		null,
		toypop_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_NAMCO,
				namco_interface
                       )
		}
        );
	
	
	
	/*************************************
	 *
	 *	ROM definitions
	 *
	 *************************************/
	
	static RomLoadPtr rom_liblrabl = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for the first CPU */
		ROM_LOAD( "5b.rom",   0x8000, 0x4000, 0xda7a93c2 );	ROM_LOAD( "5c.rom",   0xc000, 0x4000, 0x6cae25dc );
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the second CPU */
		ROM_LOAD( "2c.rom",   0xe000, 0x2000, 0x7c09e50a );
		ROM_REGION( 0x8000, REGION_CPU3, 0 );	/* 32k for the third CPU */
		ROM_LOAD16_BYTE( "8c.rom",   0x0000, 0x4000, 0xa00cd959 );	ROM_LOAD16_BYTE("10c.rom",   0x0001, 0x4000, 0x09ce209b );
		/* temporary space for graphics (disposed after conversion) */
		ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "5p.rom",   0x0000, 0x2000, 0x3b4937f0 );/* characters */
	
		ROM_REGION( 0x4000, REGION_GFX2, ROMREGION_DISPOSE );	ROM_LOAD( "9t.rom",   0x0000, 0x4000, 0xa88e24ca );/* sprites */
	
		ROM_REGION( 0x0600, REGION_PROMS, 0 );/* color proms */
		ROM_LOAD( "lr1-3.1r", 0x0000, 0x0100, 0xf3ec0d07 );// palette: red component
		ROM_LOAD( "lr1-2.1s", 0x0100, 0x0100, 0x2ae4f702 );// palette: green component
		ROM_LOAD( "lr1-1.1t", 0x0200, 0x0100, 0x7601f208 );// palette: blue component
		ROM_LOAD( "lr1-5.5l", 0x0300, 0x0100, 0x940f5397 );/* characters */
		ROM_LOAD( "lr1-6.2p", 0x0400, 0x0200, 0xa6b7f850 );/* sprites */
	
		ROM_REGION( 0x0100, REGION_SOUND1, 0 );/* sound prom */
		ROM_LOAD( "lr1-4.3d", 0x0000, 0x0100, 0x16a9166a );ROM_END(); }}; 
	
	
	static RomLoadPtr rom_toypop = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for the first CPU */
		ROM_LOAD( "tp1-2.5b", 0x8000, 0x4000, 0x87469620 );	
                ROM_LOAD( "tp1-1.5c", 0xc000, 0x4000, 0xdee2fd6e );
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for the second CPU */
		ROM_LOAD( "tp1-3.2c", 0xe000, 0x2000, 0x5f3bf6e2 );
		ROM_REGION( 0x8000, REGION_CPU3, 0 );	/* 32k for the third CPU */
		ROM_LOAD( "tp1-4.8c", 0x0000, 0x4000, 0x76997db3 );	
                ROM_LOAD("tp1-5.10c", 0x0001, 0x4000, 0x37de8786 );
		/* temporary space for graphics (disposed after conversion) */
		ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );	
                ROM_LOAD( "tp1-7.5p", 0x0000, 0x2000, 0x95076f9e );/* characters */
	
		ROM_REGION( 0x4000, REGION_GFX2, ROMREGION_DISPOSE );	
                ROM_LOAD( "tp1-6.9t", 0x0000, 0x4000, 0x481ffeaf );/* sprites */
	
		ROM_REGION( 0x0600, REGION_PROMS, 0 );/* color proms */
		ROM_LOAD( "tp1-3.1r", 0x0000, 0x0100, 0xcfce2fa5 );// palette: red component
		ROM_LOAD( "tp1-2.1s", 0x0100, 0x0100, 0xaeaf039d );// palette: green component
		ROM_LOAD( "tp1-1.1t", 0x0200, 0x0100, 0x08e7cde3 );// palette: blue component
		ROM_LOAD( "tp1-4.5l", 0x0300, 0x0100, 0x74138973 );/* characters */
		ROM_LOAD( "tp1-5.2p", 0x0400, 0x0200, 0x4d77fa5a );/* sprites */
	
		ROM_REGION( 0x0100, REGION_SOUND1, 0 );/* sound prom */
		ROM_LOAD( "lr1-4.3d", 0x0000, 0x0100, 0x16a9166a );ROM_END(); }}; 
	
	
	
	/*************************************
	 *
	 *	Driver-specific init
	 *
	 *************************************/
	
	public static InitDriverPtr init_liblrabl = new InitDriverPtr() { public void handler()
	{
		/* install the custom I/O chip */
		install_mem_read_handler(0, 0x6800, 0x683f, liblrabl_customio_r);
		toypop_customio = install_mem_write_handler(0, 0x6800, 0x683f, MWA_RAM);
	
		/* install the shared sound RAM */
		toypop_sound_sharedram = install_mem_write_handler(0, 0x6040, 0x63ff, MWA_RAM);
	
		/* install the approprite flipscreen handler */
	} };
	
	
	public static InitDriverPtr init_toypop = new InitDriverPtr() { public void handler()
	{
		/* install the custom I/O chip */
		install_mem_read_handler(0, 0x6000, 0x603f, toypop_customio_r);
		toypop_customio = install_mem_write_handler(0, 0x6000, 0x603f, MWA_RAM);
	
		/* install the shared sound RAM */
		toypop_sound_sharedram = install_mem_write_handler(0, 0x6840, 0x6bff, MWA_RAM);
	
		/* install the approprite flipscreen handler */
	} };
	
	
	
	/*************************************
	 *
	 *	Game drivers
	 *
	 *************************************/
	
	public static GameDriver driver_liblrabl	   = new GameDriver("1983"	,"liblrabl"	,"toypop.java"	,rom_liblrabl,null	,machine_driver_toypop	,input_ports_liblrabl	,init_liblrabl	,ROT0	,	"Namco", "Libble Rabble" );
	public static GameDriver driver_toypop	   = new GameDriver("1986"	,"toypop"	,"toypop.java"	,rom_toypop,null	,machine_driver_toypop	,input_ports_toypop	,init_toypop	,ROT0	,	"Namco", "Toypop" );
}
