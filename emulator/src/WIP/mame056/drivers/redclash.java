/***************************************************************************

Zero Hour / Red Clash

runs on hardware similar to Lady Bug

driver by inkling

Notes:
- In the Tehkan set the ship doesn't move during attract mode. Earlier version?
  Gameplay is different too.

TODO:
- Missing Galaxian-like starfield (speed is controlled by three output ports)

- Colors might be right, need screen shots to verify

- Some graphical problems in both games, but without screenshots its hard to
  know what we're aiming for

- Sound (analog, schematics available for Zero Hour)

***************************************************************************/

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
import static mame056.inptportH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.memoryH.*;
import static mame056.inptport.*;
import static mame056.drawgfxH.*;
import static mame056.memory.*;
import static mame056.sndintrfH.*;
import static mame056.inputH.*;
import static mame056.drawgfxH.*;

import static WIP.mame056.vidhrdw.redclash.*;
import static mame056.vidhrdw.generic.*;


public class redclash
{
	
	/*
	  This game doesn't have VBlank interrupts.
	  Interrupts are still used, but they are related to coin
	  slots. Left slot generates an IRQ, Right slot a NMI.
	*/
	public static InterruptPtr redclash_interrupt = new InterruptPtr() { public int handler() 
	{
		if ((readinputport(4) & 1) != 0)	/* Left Coin */
			cpu_set_irq_line(0,0,ASSERT_LINE);
		else if ((readinputport(4) & 2) != 0)	/* Right Coin */
			cpu_set_nmi_line(0,PULSE_LINE);
	
		return ignore_interrupt.handler();
	} };
	
	public static WriteHandlerPtr irqack_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		cpu_set_irq_line(0,0,CLEAR_LINE);
	} };
	
	
	
	public static Memory_ReadAddress zero_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x2fff, MRA_ROM ),
		new Memory_ReadAddress( 0x3000, 0x37ff, MRA_RAM ),
		new Memory_ReadAddress( 0x4800, 0x4800, input_port_0_r ), /* IN0 */
		new Memory_ReadAddress( 0x4801, 0x4801, input_port_1_r ), /* IN1 */
		new Memory_ReadAddress( 0x4802, 0x4802, input_port_2_r ), /* DSW0 */
		new Memory_ReadAddress( 0x4803, 0x4803, input_port_3_r ), /* DSW1 */
		new Memory_ReadAddress( 0x4000, 0x43ff, MRA_RAM ),  /* video RAM */
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress zero_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x2fff, MWA_ROM ),
		new Memory_WriteAddress( 0x3000, 0x37ff, MWA_RAM ),
		new Memory_WriteAddress( 0x3800, 0x3bff, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0x4000, 0x43ff, MWA_RAM, redclash_textram ),
		new Memory_WriteAddress( 0x5000, 0x5007, MWA_NOP ),	/* to sound board */
		new Memory_WriteAddress( 0x5800, 0x5800, redclash_star0_w ),
		new Memory_WriteAddress( 0x5801, 0x5804, MWA_NOP ),	/* to sound board */
		new Memory_WriteAddress( 0x5805, 0x5805, redclash_star1_w ),
		new Memory_WriteAddress( 0x5806, 0x5806, redclash_star2_w ),
		new Memory_WriteAddress( 0x5807, 0x5807, redclash_flipscreen_w ),
		new Memory_WriteAddress( 0x7000, 0x7000, redclash_star_reset_w ),
		new Memory_WriteAddress( 0x7800, 0x7800, irqack_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x2fff, MRA_ROM ),
		new Memory_ReadAddress( 0x4800, 0x4800, input_port_0_r ), /* IN0 */
		new Memory_ReadAddress( 0x4801, 0x4801, input_port_1_r ), /* IN1 */
		new Memory_ReadAddress( 0x4802, 0x4802, input_port_2_r ), /* DSW0 */
		new Memory_ReadAddress( 0x4803, 0x4803, input_port_3_r ), /* DSW1 */
		new Memory_ReadAddress( 0x4000, 0x43ff, MRA_RAM ),  /* video RAM */
		new Memory_ReadAddress( 0x6000, 0x67ff, MRA_RAM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x2fff, MWA_ROM ),
	//	new Memory_WriteAddress( 0x3000, 0x3000, MWA_NOP ),
	//	new Memory_WriteAddress( 0x3800, 0x3800, MWA_NOP ),
		new Memory_WriteAddress( 0x4000, 0x43ff, MWA_RAM, redclash_textram ),
		new Memory_WriteAddress( 0x5000, 0x5007, MWA_NOP ),	/* to sound board */
		new Memory_WriteAddress( 0x5800, 0x5800, redclash_star0_w ),
		new Memory_WriteAddress( 0x5801, 0x5801, redclash_gfxbank_w ),
		new Memory_WriteAddress( 0x5805, 0x5805, redclash_star1_w ),
		new Memory_WriteAddress( 0x5806, 0x5806, redclash_star2_w ),
		new Memory_WriteAddress( 0x5807, 0x5807, redclash_flipscreen_w ),
		new Memory_WriteAddress( 0x6000, 0x67ff, MWA_RAM ),
		new Memory_WriteAddress( 0x6800, 0x6bff, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0x7000, 0x7000, redclash_star_reset_w ),
		new Memory_WriteAddress( 0x7800, 0x7800, irqack_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	static InputPortPtr input_ports_redclash = new InputPortPtr(){ public void handler() { 
	PORT_START(); 	/* IN0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START1 );	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_START(); 	/* IN1 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_COCKTAIL );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_COCKTAIL );	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_COCKTAIL );	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNKNOWN );	/* Note that there are TWO VBlank inputs, one is active low, the other active */
		/* high. There are probably other differencies in the hardware, but emulating */
		/* them this way is enough to get the game running. */
		PORT_BIT( 0xc0, 0x40, IPT_VBLANK );
		PORT_START(); 	/* DSW0 */
		PORT_DIPNAME( 0x03, 0x03, "Difficulty?" );	PORT_DIPSETTING(    0x03, "Easy?" );	PORT_DIPSETTING(    0x02, "Medium?" );	PORT_DIPSETTING(    0x01, "Hard?" );	PORT_DIPSETTING(    0x00, "Hardest?" );	PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x08, 0x08, DEF_STR( "Unknown") );
		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_DIPNAME( 0x10, 0x10, "High Score" );	PORT_DIPSETTING(    0x00, "0" );	PORT_DIPSETTING(    0x10, "10000" );	PORT_DIPNAME( 0x20, 0x00, DEF_STR( "Cabinet") );
		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
		PORT_DIPSETTING(    0x20, DEF_STR( "Cocktail") );
		PORT_DIPNAME( 0xc0, 0xc0, DEF_STR( "Lives") );
		PORT_DIPSETTING(    0x00, "1" );	PORT_DIPSETTING(    0xc0, "3" );	PORT_DIPSETTING(    0x80, "5" );	PORT_DIPSETTING(    0x40, "7" );
		PORT_START(); 	/* DSW1 */
		PORT_DIPNAME( 0x0f, 0x0f, DEF_STR( "Coin_A") );
		PORT_DIPSETTING(    0x04, DEF_STR( "6C_1C") );
		PORT_DIPSETTING(    0x05, DEF_STR( "5C_1C") );
		PORT_DIPSETTING(    0x06, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x08, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0x0a, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x07, DEF_STR( "3C_2C") );
		PORT_DIPSETTING(    0x0f, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x09, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0x0e, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0x0d, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0x0c, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0x0b, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x03, DEF_STR( "1C_6C") );
		PORT_DIPSETTING(    0x02, DEF_STR( "1C_7C") );
		PORT_DIPSETTING(    0x01, DEF_STR( "1C_8C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_9C") );
		PORT_DIPNAME( 0xf0, 0xf0, DEF_STR( "Coin_B") );
		PORT_DIPSETTING(    0x40, DEF_STR( "6C_1C") );
		PORT_DIPSETTING(    0x50, DEF_STR( "5C_1C") );
		PORT_DIPSETTING(    0x60, DEF_STR( "4C_1C") );
		PORT_DIPSETTING(    0x80, DEF_STR( "3C_1C") );
		PORT_DIPSETTING(    0xa0, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(    0x70, DEF_STR( "3C_2C") );
		PORT_DIPSETTING(    0xf0, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(    0x90, DEF_STR( "2C_3C") );
		PORT_DIPSETTING(    0xe0, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(    0xd0, DEF_STR( "1C_3C") );
		PORT_DIPSETTING(    0xc0, DEF_STR( "1C_4C") );
		PORT_DIPSETTING(    0xb0, DEF_STR( "1C_5C") );
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_6C") );
		PORT_DIPSETTING(    0x20, DEF_STR( "1C_7C") );
		PORT_DIPSETTING(    0x10, DEF_STR( "1C_8C") );
		PORT_DIPSETTING(    0x00, DEF_STR( "1C_9C") );
	
		PORT_START(); 	/* FAKE */
		/* The coin slots are not memory mapped. Coin Left causes a NMI, */
		/* Coin Right an IRQ. This fake input port is used by the interrupt */
		/* handler to be notified of coin insertions. We use IMPULSE to */
		/* trigger exactly one interrupt, without having to check when the */
		/* user releases the key. */
		PORT_BIT_IMPULSE( 0x01, IP_ACTIVE_HIGH, IPT_COIN1, 1 );	PORT_BIT_IMPULSE( 0x02, IP_ACTIVE_HIGH, IPT_COIN2, 1 );INPUT_PORTS_END(); }}; 
	
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,1),
		2,
		new int[] { 8*8, 0 },
		new int[] { 7, 6, 5, 4, 3, 2, 1, 0 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		16*8
	);
	
	static GfxLayout spritelayout8x8 = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,1),
		2,
		new int[] { 1, 0 },
		//new int[] { STEP8(0,2) },
                new int[] {
                    0,
                    (0)+1*(2),
                    (0)+2*(2),
                    (0)+3*(2),
                    ((0)+4*(2)),
                    ((0)+4*(2))+1*(2),
                    ((0)+4*(2))+2*(2),
                    ((0)+4*(2))+3*(2)
                },
		//new int[] { STEP8(7*16,-16) },
                new int[] {
                  0,
                    (7*16)+1*(-16),
                    (7*16)+2*(-16),
                    (7*16)+3*(-16),
                    ((7*16)+4*(-16)),
                    ((7*16)+4*(-16))+1*(-16),
                    ((7*16)+4*(-16))+2*(-16),
                    ((7*16)+4*(-16))+3*(-16)  
                },
                
		16*8
	);
	
	static GfxLayout spritelayout16x16 = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,1),
		2,
		new int[] { 1, 0 },
		new int[] { 
                            STEP8(0, 24*2,2), STEP8(1, 24*2,2), STEP8(2, 24*2,2), STEP8(3, 24*2,2), STEP8(4, 24*2,2), STEP8(5, 24*2,2), STEP8(6, 24*2,2), STEP8(7, 24*2,2),
                            STEP8(0, 8*64+24*2,2), STEP8(1, 8*64+24*2,2), STEP8(2, 8*64+24*2,2), STEP8(3, 8*64+24*2,2), STEP8(4, 8*64+24*2,2), STEP8(5, 8*64+24*2,2), STEP8(6, 8*64+24*2,2), STEP8(7, 8*64+24*2,2)
                          },
		new int[] { 
                            STEP8(0, 23*64,-64), STEP8(1, 23*64,-64), STEP8(2, 23*64,-64), STEP8(3, 23*64,-64), STEP8(4, 23*64,-64), STEP8(5, 23*64,-64), STEP8(6, 23*64,-64), STEP8(7, 23*64,-64), 
                            STEP8(0, 7*64,-64), STEP8(1, 7*64,-64), STEP8(2, 7*64,-64), STEP8(3, 7*64,-64), STEP8(4, 7*64,-64), STEP8(5, 7*64,-64), STEP8(6, 7*64,-64), STEP8(7, 7*64,-64)
                          },
		64*32
	);
	
	static GfxLayout spritelayout24x24 = new GfxLayout
	(
		24,24,
		RGN_FRAC(1,1),
		2,
		new int[] { 1, 0 },
		new int[] { 
                    STEP8(0,0,2), STEP8(1,0,2), STEP8(2,0,2), STEP8(3,0,2), STEP8(4,0,2), STEP8(5,0,2), STEP8(6,0,2), STEP8(7,0,2), 
                    STEP8(0,8*2,2), STEP8(1,8*2,2), STEP8(2,8*2,2), STEP8(3,8*2,2), STEP8(4,8*2,2), STEP8(5,8*2,2), STEP8(6,8*2,2), STEP8(7,8*2,2), 
                    STEP8(0,16*2,2), STEP8(1,16*2,2), STEP8(2,16*2,2), STEP8(3,16*2,2), STEP8(4,16*2,2), STEP8(5,16*2,2), STEP8(6,16*2,2), STEP8(7,16*2,2)
                },
		new int[] { 
                    STEP8(0,23*64,-64), STEP8(1,23*64,-64), STEP8(2,23*64,-64), STEP8(3,23*64,-64), STEP8(4,23*64,-64), STEP8(5,23*64,-64), STEP8(6,23*64,-64), STEP8(7,23*64,-64), 
                    STEP8(0,15*64,-64), STEP8(1,15*64,-64), STEP8(2,15*64,-64), STEP8(3,15*64,-64), STEP8(4,15*64,-64), STEP8(5,15*64,-64), STEP8(6,15*64,-64), STEP8(7,15*64,-64), 
                    STEP8(0,7*64,-64), STEP8(1,7*64,-64), STEP8(2,7*64,-64), STEP8(3,7*64,-64), STEP8(4,7*64,-64), STEP8(5,7*64,-64), STEP8(6,7*64,-64), STEP8(7,7*64,-64)
                },
		64*32
	);
	
	static GfxLayout spritelayout16x16bis = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,1),
		2,
		new int[] { 1, 0 },
		new int[] { 
                    STEP8(0,0,2), STEP8(1,0,2), STEP8(2,0,2), STEP8(3,0,2), STEP8(4,0,2), STEP8(5,0,2), STEP8(6,0,2), STEP8(7,0,2), 
                    STEP8(0,8*2,2), STEP8(1,8*2,2), STEP8(2,8*2,2), STEP8(3,8*2,2), STEP8(4,8*2,2), STEP8(5,8*2,2), STEP8(6,8*2,2), STEP8(7,8*2,2)
                },
		new int[] { 
                    STEP8(0,15*64,-64), STEP8(1,15*64,-64), STEP8(2,15*64,-64), STEP8(3,15*64,-64), STEP8(4,15*64,-64), STEP8(5,15*64,-64), STEP8(6,15*64,-64), STEP8(7,15*64,-64), 
                    STEP8(0,7*64,-64), STEP8(1,7*64,-64), STEP8(2,7*64,-64), STEP8(3,7*64,-64), STEP8(4,7*64,-64), STEP8(5,7*64,-64), STEP8(6,7*64,-64), STEP8(7,7*64,-64)
                },
		32*32
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0x0000, charlayout,          0,  8 ),
		new GfxDecodeInfo( REGION_GFX3, 0x0000, spritelayout8x8,   4*8, 16 ),
		new GfxDecodeInfo( REGION_GFX2, 0x0000, spritelayout16x16, 4*8, 16 ),
		new GfxDecodeInfo( REGION_GFX2, 0x0000, spritelayout24x24, 4*8, 16 ),
		new GfxDecodeInfo( REGION_GFX2, 0x0000, spritelayout16x16bis, 4*8, 16 ),
		new GfxDecodeInfo( REGION_GFX2, 0x0004, spritelayout16x16bis, 4*8, 16 ),
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	static MachineDriver machine_driver_zerohour = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
			  CPU_Z80,
			  4000000,  /* 4 MHz */
			  zero_readmem,zero_writemem,null,null,
			  redclash_interrupt,1
			)
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,  /* frames per second, vblank duration */
		1,  /* single CPU, no need for interleaving */
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 1*8, 31*8-1, 4*8, 28*8-1 ),
		gfxdecodeinfo,
		32,4*24,
		redclash_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		null,
		null,
		redclash_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
                null
	);
	
	static MachineDriver machine_driver_redclash = new MachineDriver
	(
		new MachineCPU[] {
			new MachineCPU(
			  CPU_Z80,
			  4000000,  /* 4 MHz */
			  readmem,writemem,null,null,
			  redclash_interrupt,1
			)
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,  /* frames per second, vblank duration */
		1,  /* single CPU, no need for interleaving */
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 1*8, 31*8-1, 4*8, 28*8-1 ),
		gfxdecodeinfo,
		32,4*24,
		redclash_vh_convert_color_prom,
	
		VIDEO_TYPE_RASTER,
		null,
		null,
		null,
		redclash_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
                null
	);
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_zerohour = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "zerohour.1",   0x0000, 0x0800, 0x0dff4b48 );	ROM_LOAD( "zerohour.2",   0x0800, 0x0800, 0xcf41b6ac );	ROM_LOAD( "zerohour.3",	  0x1000, 0x0800, 0x5ef48b67 );	ROM_LOAD( "zerohour.4",	  0x1800, 0x0800, 0x25c5872d );	ROM_LOAD( "zerohour.5",	  0x2000, 0x0800, 0xd7ce3add );	ROM_LOAD( "zerohour.6",	  0x2800, 0x0800, 0x8a93ae6e );
		ROM_REGION( 0x0800, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "zerohour.9",   0x0000, 0x0800, 0x17ae6f13 );
		ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );	ROM_LOAD( "zerohour.7",	  0x0000, 0x0800, 0x4c12f59d );	ROM_LOAD( "zerohour.8",	  0x0800, 0x0800, 0x6b9a6b6e );
		ROM_REGION( 0x1000, REGION_GFX3, ROMREGION_DISPOSE );	/* gfx data will be rearranged here for 8x8 sprites */
	
		ROM_REGION( 0x0600, REGION_PROMS, 0 );	ROM_LOAD( "zerohour.ic2", 0x0000, 0x0020, 0xb55aee56 );/* palette */
		ROM_LOAD( "zerohour.n2",  0x0020, 0x0020, 0x9adabf46 );/* sprite color lookup table */
		ROM_LOAD( "zerohour.u6",  0x0040, 0x0020, 0x27fa3a50 );/* ?? */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_redclash = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "rc1.11c",      0x0000, 0x1000, 0x5b62ff5a );	ROM_LOAD( "rc3.7c",       0x1000, 0x1000, 0x409c4ee7 );	ROM_LOAD( "rc2.9c",       0x2000, 0x1000, 0x5f215c9a );
		ROM_REGION(0x0800, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "rc6.12a",      0x0000, 0x0800, 0xda9bbcc2 );
		ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );	ROM_LOAD( "rc4.3e",       0x0000, 0x0800, 0x64ca8b63 );	ROM_CONTINUE(             0x1000, 0x0800 );	ROM_LOAD( "rc5.3d",       0x0800, 0x0800, 0xfce610a2 );	ROM_CONTINUE(             0x1800, 0x0800 );
		ROM_REGION( 0x2000, REGION_GFX3, ROMREGION_DISPOSE );	/* gfx data will be rearranged here for 8x8 sprites */
	
		ROM_REGION( 0x0060, REGION_PROMS, 0 );	ROM_LOAD( "6331-1.12f",   0x0000, 0x0020, 0x43989681 );/* palette */
		ROM_LOAD( "6331-2.4a",    0x0020, 0x0020, 0x9adabf46 );/* sprite color lookup table */
		ROM_LOAD( "6331-3.11e",   0x0040, 0x0020, 0x27fa3a50 );/* ?? */
	ROM_END(); }}; 
	
	static RomLoadPtr rom_redclask = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION(0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "rc1.8c",       0x0000, 0x0800, 0xfd90622a );	ROM_LOAD( "rc2.7c",       0x0800, 0x0800, 0xc8f33440 );	ROM_LOAD( "rc3.6c",       0x1000, 0x0800, 0x2172b1e9 );	ROM_LOAD( "rc4.5c",       0x1800, 0x0800, 0x55c0d1b5 );	ROM_LOAD( "rc5.4c",       0x2000, 0x0800, 0x744b5261 );	ROM_LOAD( "rc6.3c",       0x2800, 0x0800, 0xfa507e17 );
		ROM_REGION( 0x0800, REGION_GFX1, ROMREGION_DISPOSE );	ROM_LOAD( "rc6.12a",      0x0000, 0x0800, 0xda9bbcc2 );/* rc9.7m */
	
		ROM_REGION( 0x2000, REGION_GFX2, ROMREGION_DISPOSE );	ROM_LOAD( "rc4.4m",       0x0000, 0x0800, 0x483a1293 );	ROM_CONTINUE(             0x1000, 0x0800 );	ROM_LOAD( "rc5.5m",       0x0800, 0x0800, 0xc45d9601 );	ROM_CONTINUE(             0x1800, 0x0800 );
		ROM_REGION( 0x2000, REGION_GFX3, ROMREGION_DISPOSE );	/* gfx data will be rearranged here for 8x8 sprites */
	
		ROM_REGION( 0x0060, REGION_PROMS, 0 );	ROM_LOAD( "6331-1.12f",   0x0000, 0x0020, 0x43989681 );/* 6331.7e */
		ROM_LOAD( "6331-2.4a",    0x0020, 0x0020, 0x9adabf46 );/* 6331.2r */
		ROM_LOAD( "6331-3.11e",   0x0040, 0x0020, 0x27fa3a50 );/* 6331.6w */
	ROM_END(); }}; 
	
	public static InitDriverPtr init_redclash = new InitDriverPtr() { public void handler()
	{
		int i,j;
	
		/* rearrange the sprite graphics */
		for (i = 0;i < memory_region_length(REGION_GFX3);i++)
		{
			j = (i & ~0x003e) | ((i & 0x0e) << 2) | ((i & 0x30) >> 3);
			memory_region(REGION_GFX3).write(i, memory_region(REGION_GFX2).read(j));
		}
	} };
	
	
	
	public static GameDriver driver_zerohour	   = new GameDriver("1980?"	,"zerohour"	,"redclash.java"	,rom_zerohour,null	,machine_driver_zerohour	,input_ports_redclash	,init_redclash	,ROT270	,	"Universal", "Zero Hour",          GAME_NO_SOUND | GAME_WRONG_COLORS | GAME_IMPERFECT_GRAPHICS );
	public static GameDriver driver_redclash	   = new GameDriver("1981"	,"redclash"	,"redclash.java"	,rom_redclash,null	,machine_driver_redclash	,input_ports_redclash	,init_redclash	,ROT270	,	"Tehkan",    "Red Clash",          GAME_NO_SOUND | GAME_WRONG_COLORS | GAME_IMPERFECT_GRAPHICS );
	public static GameDriver driver_redclask	   = new GameDriver("1981"	,"redclask"	,"redclash.java"	,rom_redclask,driver_redclash	,machine_driver_redclash	,input_ports_redclash	,init_redclash	,ROT270	,	"Kaneko",    "Red Clash (Kaneko)", GAME_NO_SOUND | GAME_WRONG_COLORS | GAME_IMPERFECT_GRAPHICS );
}
