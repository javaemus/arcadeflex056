/***************************************************************************
Ultra Tank
(C) 1979 Kee Games

Known Issues:

- sound samples needed
- colors are probably correct, but should be verified
- invisible tanks option doesn't work
- coin counters aren't mapped
- hardware collision detection is not emulated. However, the game is fully playable,
  since the game software uses it only as a hint to check for tanks bumping into
  walls/mines.
*/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;

import static mame056.commonH.*;
import static mame056.cpuexec.*;
import static mame056.inptportH.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.memoryH.*;
import static mame056.inptport.*;
import static mame056.drawgfxH.*;
import static mame056.sndintrfH.*;
import static mame056.timerH.*;
import static mame056.timer.*;
import static mame056.palette.*;
import static common.libc.cstring.*;
import static mame056.common.*;
import static mame056.drawgfx.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;
import static mame056.inputH.*;
import static mame056.mame.*;

public class ultratnk
{
	
	static int ultratnk_controls;
	static UBytePtr mirror_ram = new UBytePtr();
	
	static void draw_sprites( mame_bitmap bitmap )
	{
		UBytePtr pMem = new UBytePtr(memory_region( REGION_CPU1 ));
	
		if( (pMem.read(0x93)&0x80)==0 )
		/*	Probably wrong; game description indicates that one or both tanks can
			be invisible; in this game mode, tanks are visible when hit, bumping
			into a wall, or firing
		*/
		{
			drawgfx( bitmap, Machine.gfx[1], /* tank */
				pMem.read(0x99)>>3,
				0,
				0,0, /* no flip */
				pMem.read(0x90)-16,pMem.read(0x98)-16,
				Machine.visible_area,
				TRANSPARENCY_PEN, 0 );
	
			drawgfx( bitmap, Machine.gfx[1], /* tank */
				pMem.read(0x9b)>>3,
				1,
				0,0, /* no flip */
				pMem.read(0x92)-16,pMem.read(0x9a)-16,
				Machine.visible_area,
				TRANSPARENCY_PEN, 0 );
		}
	
		drawgfx( bitmap, Machine.gfx[1], /* bullet */
			(pMem.read(0x9f)>>3)|0x20,
			0,
			0,0, /* no flip */
			pMem.read(0x96)-16,pMem.read(0x9e)-16,
			Machine.visible_area,
			TRANSPARENCY_PEN, 0 );
	
		drawgfx( bitmap, Machine.gfx[1], /* bullet */
			(pMem.read(0x9d)>>3)|0x20,
			1,
			0,0, /* no flip */
			pMem.read(0x94)-16,pMem.read(0x9c)-16,
			Machine.visible_area,
			TRANSPARENCY_PEN, 0 );
	}
	
	public static VhUpdatePtr ultratnk_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int code;
		int sx,sy;
		int offs;
	
		/* for every character in the Video RAM, check if it has been modified */
		/* since last time and update it accordingly. */
		for( offs = 0; offs<0x400; offs++ )
		{
			if (dirtybuffer[offs] != 0)
			{
				dirtybuffer[offs]=0;
				code = videoram.read(offs);
				sx = 8 * (offs % 32);
				sy = 8 * (offs / 32);
				drawgfx( tmpbitmap,
					Machine.gfx[0],
					code&0x3F, code>>6,
					0,0,
					sx,sy,
					null,TRANSPARENCY_NONE,0);
			}
		}
		/* copy the character mapped graphics */
		copybitmap(bitmap,tmpbitmap,0,0,0,0,Machine.visible_area,TRANSPARENCY_NONE,0);
		draw_sprites( bitmap );
	} };
	
	public static WriteHandlerPtr da_latch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int joybits;
		ultratnk_controls = input_port_3_r.handler(0); /* start and fire buttons */
	
		joybits = input_port_4_r.handler(0);
	
		switch( data )
		{
		case 0x0a:
			if(( joybits&0x08 ) != 0) ultratnk_controls &= ~0x40;
			if(( joybits&0x04 ) != 0) ultratnk_controls &= ~0x04;
	
			if(( joybits&0x80 ) != 0) ultratnk_controls &= ~0x10;
			if(( joybits&0x40 ) != 0) ultratnk_controls &= ~0x01;
			break;
	
		case 0x05:
			if(( joybits&0x02 ) != 0) ultratnk_controls &= ~0x40;
			if(( joybits&0x01 ) != 0) ultratnk_controls &= ~0x04;
	
			if(( joybits&0x20 ) != 0) ultratnk_controls &= ~0x10;
			if(( joybits&0x10 ) != 0) ultratnk_controls &= ~0x01;
			break;
		}
	} };
	
	public static ReadHandlerPtr ultratnk_controls_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return (ultratnk_controls << offset) & 0x80;
	} };
	
	public static ReadHandlerPtr ultratnk_barrier_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return readinputport(2) & 0x80;
	} };
	
	public static ReadHandlerPtr ultratnk_coin_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		switch (offset & 0x06)
		{
			case 0x00: return (readinputport(2) << 3) & 0x80;	/* left coin */
			case 0x02: return (readinputport(2) << 4) & 0x80;	/* right coin */
			case 0x04: return (readinputport(2) << 1) & 0x80;	/* invisible tanks */
			case 0x06: return (readinputport(2) << 2) & 0x80;	/* rebounding shots */
		}
	
		return 0;
	} };
	
	public static ReadHandlerPtr ultratnk_tilt_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return (readinputport(2) << 5) & 0x80;	/* tilt */
	} };
	
	public static ReadHandlerPtr ultratnk_collision_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/**	Note: hardware collision detection is not emulated.
		 *	However, the game is fully playable, since the game software uses it
		 *	only as a hint to check for tanks bumping into walls/mines.
		 */
		switch( offset )
		{
			case 0x01:	return 0x80;	/* white tank = D7 */
			case 0x03:	return 0x80;	/* black tank = D7 */
		}
		return 0;
	} };
	
	public static ReadHandlerPtr ultratnk_dipsw_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int dipsw = input_port_0_r.handler(offset);
		switch( offset )
		{
			case 0x00: return ((dipsw & 0xC0) >> 6); /* language? */
			case 0x01: return ((dipsw & 0x30) >> 4); /* credits */
			case 0x02: return ((dipsw & 0x0C) >> 2); /* game time */
			case 0x03: return ((dipsw & 0x03) >> 0); /* extended time */
		}
		return 0;
	} };
	
	public static InterruptPtr ultratnk_interrupt = new InterruptPtr() { public int handler() 
	{
		if(( input_port_1_r.handler(0) & 0x40 ) != 0)
		{
			/* only do NMI interrupt if not in TEST mode */
			return nmi_interrupt.handler();
		}
		return ignore_interrupt.handler();
	} };
	
	public static WriteHandlerPtr ultratnk_leds_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		set_led_status(offset/2,offset&1);
	} };
	
	public static ReadHandlerPtr mirror_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return mirror_ram.read(offset);
	} };
	
	public static WriteHandlerPtr mirror_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		mirror_ram.write(offset, data);
	} };
	
	
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x00ff, MRA_RAM ),
		new Memory_ReadAddress( 0x0100, 0x01ff, mirror_r ),
		new Memory_ReadAddress( 0x0800, 0x0bff, MRA_RAM ),
		new Memory_ReadAddress( 0x0c00, 0x0cff, MRA_RAM ),
		new Memory_ReadAddress( 0x1000, 0x1000, input_port_1_r ), /* self test, vblank */
		new Memory_ReadAddress( 0x1800, 0x1800, ultratnk_barrier_r ), /* barrier */
		new Memory_ReadAddress( 0x2000, 0x2007, ultratnk_controls_r ),
		new Memory_ReadAddress( 0x2020, 0x2026, ultratnk_coin_r ),
		new Memory_ReadAddress( 0x2040, 0x2043, ultratnk_collision_r ),
		new Memory_ReadAddress( 0x2046, 0x2046, ultratnk_tilt_r ),
		new Memory_ReadAddress( 0x2060, 0x2063, ultratnk_dipsw_r ),
		new Memory_ReadAddress( 0x2800, 0x2fff, MRA_NOP ), /* diagnostic ROM (see code at B1F3) */
		new Memory_ReadAddress( 0xb000, 0xbfff, MRA_ROM ),
		new Memory_ReadAddress( 0xf000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x00ff, MWA_RAM, mirror_ram ),
		new Memory_WriteAddress( 0x0100, 0x01ff, mirror_w ),
		new Memory_WriteAddress( 0x0800, 0x0bff, videoram_w, videoram, videoram_size ),
		new Memory_WriteAddress( 0x0c00, 0x0cff, MWA_RAM ), /* ? */
		new Memory_WriteAddress( 0x2000, 0x2000, MWA_NOP ), /* sound-related? */
		new Memory_WriteAddress( 0x2020, 0x2023, MWA_NOP ), /* collision reset? */
		new Memory_WriteAddress( 0x2040, 0x2040, da_latch_w ), /* D/A LATCH */
		new Memory_WriteAddress( 0x2042, 0x2042, MWA_NOP ), /* EXPLOSION (sound) */
		new Memory_WriteAddress( 0x2044, 0x2044, MWA_NOP ), /* TIMER (watchdog) RESET */
		new Memory_WriteAddress( 0x2066, 0x2066, MWA_NOP ), /* LOCKOUT (coin lockout latched on */
		new Memory_WriteAddress( 0x2067, 0x2067, MWA_NOP ), /* LOCKOUT (coin lockout latched off */
		new Memory_WriteAddress( 0x2068, 0x206b, ultratnk_leds_w ),
	//	new Memory_WriteAddress( 0x206c, 0x206f, MWA_RAM ), /* ? */
		new Memory_WriteAddress( 0xb000, 0xbfff, MWA_ROM ),
		new Memory_WriteAddress( 0xf000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	
	static InputPortPtr input_ports_ultratnk = new InputPortPtr(){ public void handler() { 
		PORT_START(); 
		PORT_DIPNAME( 0x03, 0x01, "Extended Play" );
		PORT_DIPSETTING(	0x01, "25 Points" );
		PORT_DIPSETTING(	0x02, "50 Points" );
		PORT_DIPSETTING(	0x03, "75 Points" );
		PORT_DIPSETTING(	0x00, "None" );
		PORT_DIPNAME( 0x0c, 0x04, "Game Length" );
		PORT_DIPSETTING(	0x00, "60 Seconds" );
		PORT_DIPSETTING(	0x04, "90 Seconds" );
		PORT_DIPSETTING(	0x08, "120 Seconds" );
		PORT_DIPSETTING(	0x0c, "150 Seconds" );
		PORT_DIPNAME( 0x30, 0x20, DEF_STR( "Coinage") );
		PORT_DIPSETTING(	0x30, DEF_STR( "2C_1C") );
		PORT_DIPSETTING(	0x20, DEF_STR( "1C_1C") );
		PORT_DIPSETTING(	0x10, DEF_STR( "1C_2C") );
		PORT_DIPSETTING(	0x00, DEF_STR( "Free_Play") );
		PORT_DIPNAME( 0xc0, 0x00, "Spare" );/* Language?  Doesn't have any effect. */
		PORT_DIPSETTING(	0x00, "A" );
		PORT_DIPSETTING(	0x40, "B" );
		PORT_DIPSETTING(	0x80, "C" );
		PORT_DIPSETTING(	0xc0, "D" );
	
		PORT_START(); 
		PORT_SERVICE( 0x40, IP_ACTIVE_LOW );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_VBLANK );
	
		PORT_START();  /* input#2 (arbitrarily arranged) */
		PORT_BITX(0x80, IP_ACTIVE_HIGH, IPT_SERVICE1 | IPF_TOGGLE, "Option 1", IP_KEY_DEFAULT, IP_JOY_DEFAULT );
		PORT_BITX(0x40, IP_ACTIVE_HIGH, IPT_SERVICE2 | IPF_TOGGLE, "Option 2", IP_KEY_DEFAULT, IP_JOY_DEFAULT );
		PORT_BITX(0x20, IP_ACTIVE_HIGH, IPT_SERVICE3 | IPF_TOGGLE, "Option 3", IP_KEY_DEFAULT, IP_JOY_DEFAULT );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_TILT );
	
		PORT_START();  /* input#3 */
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_SPECIAL );/* joystick (taken from below) */
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_START2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_SPECIAL );/* joystick (taken from below) */
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SPECIAL );/* joystick (taken from below) */
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_SPECIAL );/* joystick (taken from below) */
	
		PORT_START();  /* input#4 - fake */
		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_JOYSTICKLEFT_DOWN  | IPF_PLAYER1 );
		PORT_BIT( 0x0a, IP_ACTIVE_HIGH, IPT_JOYSTICKLEFT_UP    | IPF_PLAYER1 );/* note that this sets 2 bits */
		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_JOYSTICKRIGHT_DOWN | IPF_PLAYER1 );
		PORT_BIT( 0x05, IP_ACTIVE_HIGH, IPT_JOYSTICKRIGHT_UP   | IPF_PLAYER1 );/* note that this sets 2 bits */
		PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_JOYSTICKLEFT_DOWN  | IPF_PLAYER2 );
		PORT_BIT( 0xa0, IP_ACTIVE_HIGH, IPT_JOYSTICKLEFT_UP    | IPF_PLAYER2 );/* note that this sets 2 bits */
		PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_JOYSTICKRIGHT_DOWN | IPF_PLAYER2 );
		PORT_BIT( 0x50, IP_ACTIVE_HIGH, IPT_JOYSTICKRIGHT_UP   | IPF_PLAYER2 );/* note that this sets 2 bits */
	INPUT_PORTS_END(); }}; 
	
	
	
	static char palette[] =
	{
		0x00,0x00,0x00, /* BLACK */
		0xff,0xff,0xff, /* WHITE */
		0x80,0x80,0x80, /* LT GREY */
		0x55,0x55,0x55, /* DK GREY */
	};
	
	static char colortable[] =
	{
		0x02, 0x01,
		0x02, 0x00,
		0x02, 0x00,
		0x02, 0x01
	};
	
	static VhConvertColorPromPtr init_palette = new VhConvertColorPromPtr() {
            public void handler(char[] game_palette, char[] game_colortable, UBytePtr color_prom) {
                memcpy(game_palette,palette,palette.length);
		memcpy(game_colortable,colortable,colortable.length);
            }
        };
	
	static GfxLayout playfield_layout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,2),
		1,
		new int[] { 0 },
		new int[] { 4, 5, 6, 7, 4 + RGN_FRAC(1,2), 5 + RGN_FRAC(1,2), 6 + RGN_FRAC(1,2), 7 + RGN_FRAC(1,2) },
		new int[] { 0, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		8*8
	);
	
	static GfxLayout motion_layout = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,4),
		1,
		new int[] { 0 },
		new int[] { 7, 6, 5, 4, 7 + RGN_FRAC(1,4), 6 + RGN_FRAC(1,4), 5 + RGN_FRAC(1,4), 4 + RGN_FRAC(1,4),
		  7 + RGN_FRAC(2,4), 6 + RGN_FRAC(2,4), 5 + RGN_FRAC(2,4), 4 + RGN_FRAC(2,4),
		  7 + RGN_FRAC(3,4), 6 + RGN_FRAC(3,4), 5 + RGN_FRAC(3,4), 4 + RGN_FRAC(3,4) },
		new int[] { 0, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
		  8*8, 9*8, 10*8, 11*8, 12*8, 13*8, 14*8, 15*8 },
		16*8
	);
	
	static GfxDecodeInfo gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, playfield_layout, 0, 4 ), 	/* playfield graphics */
		new GfxDecodeInfo( REGION_GFX2, 0, motion_layout,    0, 4 ), 	/* motion graphics */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	
	
	static MachineDriver machine_driver_ultratnk = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_M6502,
				1500000, /* ? */
				//12096000/16, 	   /* clock input is the "4H" signal */
				readmem,writemem,null,null,
				ultratnk_interrupt,4
				/* NMI interrupt on the 32V signal if not in self-TEST */
                        )
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,	/* single CPU, no need for interleaving */
		null,
	
		/* video hardware */
		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 28*8-1 ),
		gfxdecodeinfo,
		palette.length/3,colortable.length,
		init_palette,
	
		VIDEO_TYPE_RASTER,
		null,
		generic_vh_start,
		generic_vh_stop,
		ultratnk_vh_screenrefresh,
	
		/* sound hardware */
		0,0,0,0,
                null
        );
	
	
	
	static RomLoadPtr rom_ultratnk = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x12000, REGION_CPU1, 0 );
		ROM_LOAD_NIB_LOW ( "030180.n1",	 0xb000, 0x0800, 0xb6aa6056 );/* ROM 3 D0-D3 */
		ROM_LOAD_NIB_HIGH( "030181.k1",	 0xb000, 0x0800, 0x17145c97 );/* ROM 3 D4-D7 */
		ROM_LOAD_NIB_LOW ( "030182.m1",	 0xb800, 0x0800, 0x034366a2 );/* ROM 4 D0-D3 */
		ROM_RELOAD(                      0xf800, 0x0800 );/* for 6502 vectors */
		ROM_LOAD_NIB_HIGH( "030183.l1",	 0xb800, 0x0800, 0xbe141602 );/* ROM 4 D4-D7 */
		ROM_RELOAD(                      0xf800, 0x0800 );/* for 6502 vectors */
	
		ROM_REGION( 0x0400, REGION_GFX1, ROMREGION_DISPOSE );
		ROM_LOAD( "30172-01.j6", 0x0000, 0x0200, 0x1d364b23 );
		ROM_LOAD( "30173-01.h6", 0x0200, 0x0200, 0x5c32f331 );
	
		ROM_REGION( 0x1000, REGION_GFX2, ROMREGION_DISPOSE );
		ROM_LOAD( "30174-01.n6", 0x0000, 0x0400, 0xd0e20e73 );
		ROM_LOAD( "30175-01.m6", 0x0400, 0x0400, 0xa47459c9 );
		ROM_LOAD( "30176-01.l6", 0x0800, 0x0400, 0x1cc7c2dd );
		ROM_LOAD( "30177-01.k6", 0x0c00, 0x0400, 0x3a91b09f );
	ROM_END(); }}; 
	
	
	public static GameDriver driver_ultratnk	   = new GameDriver("1978"	,"ultratnk"	,"ultratnk.java"	,rom_ultratnk,null	,machine_driver_ultratnk	,input_ports_ultratnk	,null	,0	,	"Atari", "Ultra Tank", GAME_NO_SOUND );
}
