/****************************************************************************
I-Robot Memory Map

0000 - 07FF  R/W    RAM
0800 - 0FFF  R/W    Banked RAM
1000 - 1000  INRD1  Bit 7 = Right Coin
                    Bit 6 = Left Coin
                    Bit 5 = Aux Coin
                    Bit 4 = Self Test
                    Bit 3 = ?
                    Bit 2 = ?
                    Bit 1 = ?
                    Bit 0 = ?
1040 - 1040  INRD2  Bit 7 = Start 1
                    Bit 6 = Start 2
                    Bit 5 = ?
                    Bit 4 = Fire
                    Bit 3 = ?
                    Bit 2 = ?
                    Bit 1 = ?
                    Bit 0 = ?
1080 - 1080  STATRD Bit 7 = VBLANK
                    Bit 6 = Polygon generator done
                    Bit 5 = Mathbox done
                    Bit 4 = Unused
                    Bit 3 = ?
                    Bit 2 = ?
                    Bit 1 = ?
                    Bit 0 = ?
10C0 - 10C0  INRD3  Dip switch
1140 - 1140  STATWR Bit 7 = Select Polygon RAM banks
                    Bit 6 = BFCALL
                    Bit 5 = Cocktail Flip
                    Bit 4 = Start Mathbox
                    Bit 3 = Connect processor bus to mathbox bus
                    Bit 2 = Start polygon generator
                    Bit 1 = Select polygon image RAM bank
                    Bit 0 = Erase polygon image memory
1180 - 1180  OUT0   Bit 7 = Alpha Map 1
                    Bit 6,5 = RAM bank select
                    Bit 4,3 = Mathbox memory select
                    Bit 2,1 = Mathbox bank select
11C0 - 11C0  OUT1   Bit 7 = Coin Counter R
                    Bit 6 = Coin Counter L
                    Bit 5 = LED2
                    Bit 4 = LED1
                    Bit 3,2,1 = ROM bank select
1200 - 12FF  R/W    NVRAM (bits 0..3 only)
1300 - 13FF  W      Select analog controller
1300 - 13FF  R      Read analog controller
1400 - 143F  R/W    Quad Pokey
1800 - 18FF         Palette RAM
1900 - 1900  W      Watchdog reset
1A00 - 1A00  W      FIREQ Enable
1B00 - 1BFF  W      Start analog controller ADC
1C00 - 1FFF  R/W    Character RAM
2000 - 3FFF  R/W    Mathbox/Vector Gen Shared RAM
4000 - 5FFF  R      Banked ROM
6000 - FFFF  R      Fixed ROM

Notes:
- There is no flip screen nor cocktail mode in the original game

****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.drivers;

public class irobot
{
/*TODO*///	static unsigned char *nvram;
/*TODO*///	static size_t nvram_size;
/*TODO*///	
/*TODO*///	static void nvram_handler(void *file, int read_or_write)
/*TODO*///	{
/*TODO*///		if (read_or_write)
/*TODO*///			osd_fwrite(file,nvram,nvram_size);
/*TODO*///		else
/*TODO*///		{
/*TODO*///			if (file)
/*TODO*///				osd_fread(file,nvram,nvram_size);
/*TODO*///			else
/*TODO*///				memset(nvram,0,nvram_size);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr irobot_nvram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///		nvram[offset] = data & 0x0f;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr irobot_clearirq_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///	    cpu_set_irq_line(0, M6809_IRQ_LINE ,CLEAR_LINE);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr irobot_clearfirq_w = new WriteHandlerPtr() {public void handler(int offset, int data)
/*TODO*///	{
/*TODO*///	    cpu_set_irq_line(0, M6809_FIRQ_LINE ,CLEAR_LINE);
/*TODO*///	} };
/*TODO*///	
/*TODO*///	
/*TODO*///	public static Memory_ReadAddress readmem[]={
/*TODO*///		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
/*TODO*///	    new Memory_ReadAddress( 0x0000, 0x07ff, MRA_RAM ),
/*TODO*///	    new Memory_ReadAddress( 0x0800, 0x0fff, MRA_BANK2 ),
/*TODO*///	    new Memory_ReadAddress( 0x1000, 0x103f, input_port_0_r ),
/*TODO*///	    new Memory_ReadAddress( 0x1040, 0x1040, input_port_1_r ),
/*TODO*///	    new Memory_ReadAddress( 0x1080, 0x1080, irobot_status_r ),
/*TODO*///	    new Memory_ReadAddress( 0x10c0, 0x10c0, input_port_3_r ),
/*TODO*///	    new Memory_ReadAddress( 0x1200, 0x12ff, MRA_RAM ),
/*TODO*///	    new Memory_ReadAddress( 0x1300, 0x13ff, irobot_control_r ),
/*TODO*///	    new Memory_ReadAddress( 0x1400, 0x143f, quad_pokey_r ),
/*TODO*///	    new Memory_ReadAddress( 0x1c00, 0x1fff, MRA_RAM ),
/*TODO*///	    new Memory_ReadAddress( 0x2000, 0x3fff, irobot_sharedmem_r ),
/*TODO*///	    new Memory_ReadAddress( 0x4000, 0x5fff, MRA_BANK1 ),
/*TODO*///	    new Memory_ReadAddress( 0x6000, 0xffff, MRA_ROM ),
/*TODO*///		new Memory_ReadAddress(MEMPORT_MARKER, 0)
/*TODO*///	};
/*TODO*///	
/*TODO*///	public static Memory_WriteAddress writemem[]={
/*TODO*///		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
/*TODO*///	    new Memory_WriteAddress( 0x0000, 0x07ff, MWA_RAM ),
/*TODO*///	    new Memory_WriteAddress( 0x0800, 0x0fff, MWA_BANK2 ),
/*TODO*///	    new Memory_WriteAddress( 0x1100, 0x1100, irobot_clearirq_w ),
/*TODO*///	    new Memory_WriteAddress( 0x1140, 0x1140, irobot_statwr_w ),
/*TODO*///	    new Memory_WriteAddress( 0x1180, 0x1180, irobot_out0_w ),
/*TODO*///	    new Memory_WriteAddress( 0x11c0, 0x11c0, irobot_rom_banksel_w ),
/*TODO*///	    new Memory_WriteAddress( 0x1200, 0x12ff, irobot_nvram_w, nvram, nvram_size ),
/*TODO*///	    new Memory_WriteAddress( 0x1400, 0x143f, quad_pokey_w ),
/*TODO*///	    new Memory_WriteAddress( 0x1800, 0x18ff, irobot_paletteram_w ),
/*TODO*///	    new Memory_WriteAddress( 0x1900, 0x19ff, MWA_RAM ),            /* Watchdog reset */
/*TODO*///	    new Memory_WriteAddress( 0x1a00, 0x1a00, irobot_clearfirq_w ),
/*TODO*///	    new Memory_WriteAddress( 0x1b00, 0x1bff, irobot_control_w ),
/*TODO*///	    new Memory_WriteAddress( 0x1c00, 0x1fff, MWA_RAM, videoram, videoram_size ),
/*TODO*///	    new Memory_WriteAddress( 0x2000, 0x3fff, irobot_sharedmem_w),
/*TODO*///	    new Memory_WriteAddress( 0x4000, 0xffff, MWA_ROM ),
/*TODO*///		new Memory_WriteAddress(MEMPORT_MARKER, 0)
/*TODO*///	};
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	static InputPortPtr input_ports_irobot = new InputPortPtr(){ public void handler() { 
/*TODO*///		PORT_START(); 	/* IN0 */
/*TODO*///	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	    PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	    PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	    PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	    PORT_SERVICE( 0x10, IP_ACTIVE_LOW );
/*TODO*///	    PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN3 );
/*TODO*///	    PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_COIN1 );
/*TODO*///	    PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN2 );
/*TODO*///	
/*TODO*///		PORT_START(); 	/* IN1 */
/*TODO*///	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	    PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	    PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	    PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	    PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
/*TODO*///	    PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
/*TODO*///	    PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START2 );
/*TODO*///	    PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
/*TODO*///	
/*TODO*///		PORT_START(); 	/* IN2 */
/*TODO*///	    PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	    PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	    PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	    PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	    PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	    PORT_BIT( 0x20, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* MB DONE */
/*TODO*///	    PORT_BIT( 0x40, IP_ACTIVE_HIGH, IPT_UNKNOWN );/* EXT DONE */
/*TODO*///	    PORT_BIT( 0x80, IP_ACTIVE_HIGH, IPT_VBLANK );
/*TODO*///	
/*TODO*///		PORT_START();  /* DSW1 */
/*TODO*///		PORT_DIPNAME(    0x03, 0x00, "Coins Per Credit" );
/*TODO*///		PORT_DIPSETTING( 0x00, "1 Coin 1 Credit" );
/*TODO*///		PORT_DIPSETTING( 0x01, "2 Coins 1 Credit" );
/*TODO*///		PORT_DIPSETTING( 0x02, "3 Coins 1 Credit" );
/*TODO*///		PORT_DIPSETTING( 0x03, "4 Coins 1 Credit" );
/*TODO*///		PORT_DIPNAME(    0x0c, 0x00, "Right Coin" );
/*TODO*///		PORT_DIPSETTING( 0x00, "1 Coin for 1 Coin Unit" );
/*TODO*///		PORT_DIPSETTING( 0x04, "1 Coin for 4 Coin Units" );
/*TODO*///		PORT_DIPSETTING( 0x08, "1 Coin for 5 Coin Units" );
/*TODO*///		PORT_DIPSETTING( 0x0c, "1 Coin for 6 Coin Units" );
/*TODO*///		PORT_DIPNAME(    0x10, 0x00, "Left Coin" );
/*TODO*///		PORT_DIPSETTING( 0x00, "1 Coin for 1 Coin Unit" );
/*TODO*///		PORT_DIPSETTING( 0x10, "1 Coin for 2 Coin Units" );
/*TODO*///		PORT_DIPNAME(    0xe0, 0x00, "Bonus Adder" );
/*TODO*///		PORT_DIPSETTING( 0x00, "None" );
/*TODO*///		PORT_DIPSETTING( 0x20, "1 Credit for 2 Coin Units" );
/*TODO*///		PORT_DIPSETTING( 0xa0, "1 Credit for 3 Coin Units" );
/*TODO*///		PORT_DIPSETTING( 0x40, "1 Credit for 4 Coin Units" );
/*TODO*///		PORT_DIPSETTING( 0x80, "1 Credit for 5 Coin Units" );
/*TODO*///		PORT_DIPSETTING( 0x60, "2 Credits for 4 Coin Units" );
/*TODO*///		PORT_DIPSETTING( 0xe0, DEF_STR( "Free_Play") );
/*TODO*///	
/*TODO*///		PORT_START();  /* DSW2 */
/*TODO*///		PORT_DIPNAME(    0x01, 0x01, "Language" );
/*TODO*///		PORT_DIPSETTING( 0x01, "English" );
/*TODO*///		PORT_DIPSETTING( 0x00, "German" );
/*TODO*///		PORT_DIPNAME(    0x02, 0x02, "Min Game Time" );
/*TODO*///		PORT_DIPSETTING( 0x00, "90 Sec" );
/*TODO*///		PORT_DIPSETTING( 0x02, "3 Lives" );
/*TODO*///		PORT_DIPNAME(    0x0c, 0x0c, DEF_STR( "Bonus_Life") );
/*TODO*///		PORT_DIPSETTING( 0x08, "None" );
/*TODO*///		PORT_DIPSETTING( 0x0c, "20000" );
/*TODO*///		PORT_DIPSETTING( 0x00, "30000" );
/*TODO*///		PORT_DIPSETTING( 0x04, "50000" );
/*TODO*///		PORT_DIPNAME(    0x30, 0x30, DEF_STR( "Lives") );
/*TODO*///		PORT_DIPSETTING( 0x20, "2" );
/*TODO*///		PORT_DIPSETTING( 0x30, "3" );
/*TODO*///		PORT_DIPSETTING( 0x00, "4" );
/*TODO*///		PORT_DIPSETTING( 0x10, "5" );
/*TODO*///		PORT_DIPNAME(    0x40, 0x40, DEF_STR( "Difficulty") );
/*TODO*///		PORT_DIPSETTING( 0x00, "Easy" );
/*TODO*///		PORT_DIPSETTING( 0x40, "Medium" );
/*TODO*///		PORT_DIPNAME(    0x80, 0x80, "Demo Mode" );
/*TODO*///		PORT_DIPSETTING( 0x80, DEF_STR( "Off") );
/*TODO*///		PORT_DIPSETTING( 0x00, DEF_STR( "On") );
/*TODO*///	
/*TODO*///		PORT_START(); 	/* IN4 */
/*TODO*///		PORT_ANALOG( 0xff, 0x80, IPT_AD_STICK_Y | IPF_CENTER, 70, 50, 95, 159 );
/*TODO*///	
/*TODO*///		PORT_START(); 	/* IN5 */
/*TODO*///		PORT_ANALOG( 0xff, 0x80, IPT_AD_STICK_X | IPF_REVERSE | IPF_CENTER, 50, 50, 95, 159 );
/*TODO*///	
/*TODO*///	INPUT_PORTS_END(); }}; 
/*TODO*///	
/*TODO*///	
/*TODO*///	static GfxLayout charlayout = new GfxLayout
/*TODO*///	(
/*TODO*///		8,8,    /* 8*8 characters */
/*TODO*///	    64,    /* 64 characters */
/*TODO*///	    1,      /* 1 bit per pixel */
/*TODO*///	    new int[] { 0 }, /* the bitplanes are packed in one nibble */
/*TODO*///	    new int[] { 4, 5, 6, 7, 12, 13, 14, 15},
/*TODO*///	    new int[] { 0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16},
/*TODO*///	    16*8   /* every char takes 16 consecutive bytes */
/*TODO*///	);
/*TODO*///	
/*TODO*///	static GfxDecodeInfo gfxdecodeinfo[] =
/*TODO*///	{
/*TODO*///	    new GfxDecodeInfo( REGION_GFX1, 0, charlayout, 64, 16 ),
/*TODO*///		new GfxDecodeInfo( -1 )
/*TODO*///	};
/*TODO*///	
/*TODO*///	static struct POKEYinterface pokey_interface =
/*TODO*///	{
/*TODO*///		4,	/* 4 chips */
/*TODO*///		1250000,	/* 1.25 MHz??? */
/*TODO*///		{ 25, 25, 25, 25 },
/*TODO*///		/* The 8 pot handlers */
/*TODO*///		{ 0, 0, 0, 0 },
/*TODO*///		{ 0, 0, 0, 0 },
/*TODO*///		{ 0, 0, 0, 0 },
/*TODO*///		{ 0, 0, 0, 0 },
/*TODO*///		{ 0, 0, 0, 0 },
/*TODO*///		{ 0, 0, 0, 0 },
/*TODO*///		{ 0, 0, 0, 0 },
/*TODO*///		{ 0, 0, 0, 0 },
/*TODO*///		/* The allpot handler */
/*TODO*///	    { input_port_4_r, 0, 0, 0 },
/*TODO*///	};
/*TODO*///	
/*TODO*///	
/*TODO*///	static MachineDriver machine_driver_irobot = new MachineDriver
/*TODO*///	(
/*TODO*///		/* basic machine hardware */
/*TODO*///		new MachineCPU[] {
/*TODO*///			new MachineCPU(
/*TODO*///	            CPU_M6809,
/*TODO*///	            1500000,    /* 1.5 MHz */
/*TODO*///				readmem,writemem,null,null,
/*TODO*///	            ignore_interrupt,0		/* interrupt handled by scanline callbacks */
/*TODO*///	         ),
/*TODO*///		},
/*TODO*///		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
/*TODO*///	    1,
/*TODO*///	    irobot_init_machine,
/*TODO*///	
/*TODO*///		/* video hardware */
/*TODO*///		32*8, 32*8, new rectangle( 0*8, 32*8-1, 0*8, 29*8-1 ),
/*TODO*///		gfxdecodeinfo,
/*TODO*///	    64 + 32,64 + 32, /* 64 for polygons, 32 for text */
/*TODO*///	    irobot_vh_convert_color_prom,
/*TODO*///	
/*TODO*///	    VIDEO_TYPE_RASTER,
/*TODO*///		null,
/*TODO*///	    irobot_vh_start,
/*TODO*///	    irobot_vh_stop,
/*TODO*///	    irobot_vh_screenrefresh,
/*TODO*///	
/*TODO*///		/* sound hardware */
/*TODO*///		0,0,0,0,
/*TODO*///		new MachineSound[] {
/*TODO*///			new MachineSound(
/*TODO*///				SOUND_POKEY,
/*TODO*///				pokey_interface
/*TODO*///			)
/*TODO*///		},
/*TODO*///	
/*TODO*///		nvram_handler
/*TODO*///	);
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/***************************************************************************
/*TODO*///	
/*TODO*///	  Game driver(s)
/*TODO*///	
/*TODO*///	***************************************************************************/
/*TODO*///	
/*TODO*///	static RomLoadPtr rom_irobot = new RomLoadPtr(){ public void handler(){ 
/*TODO*///		ROM_REGION( 0x20000, REGION_CPU1, 0 );/* 64k for code + 48K Banked ROM*/
/*TODO*///		ROM_LOAD( "136029.208",     0x06000, 0x2000, 0xb4d0be59 );
/*TODO*///		ROM_LOAD( "136029.209",     0x08000, 0x4000, 0xf6be3cd0 );
/*TODO*///		ROM_LOAD( "136029.210",     0x0c000, 0x4000, 0xc0eb2133 );
/*TODO*///		ROM_LOAD( "136029.405",     0x10000, 0x4000, 0x9163efe4 );
/*TODO*///		ROM_LOAD( "136029.206",     0x14000, 0x4000, 0xe114a526 );
/*TODO*///		ROM_LOAD( "136029.207",     0x18000, 0x4000, 0xb4556cb0 );
/*TODO*///	
/*TODO*///		ROM_REGION16_BE( 0x10000, REGION_CPU2, 0 ); /* mathbox region */
/*TODO*///		ROM_LOAD16_BYTE( "ir104.bin", 0x0000,  0x2000, 0x0a6cdcca );
/*TODO*///		ROM_LOAD16_BYTE( "ir103.bin", 0x0001,  0x2000, 0x0c83296d );/* ROM data from 0000-bfff */
/*TODO*///		ROM_LOAD16_BYTE( "ir102.bin", 0x4000,  0x4000, 0x9d588f22 );
/*TODO*///		ROM_LOAD16_BYTE( "ir101.bin", 0x4001,  0x4000, 0x62a38c08 );
/*TODO*///		/* RAM data from c000-dfff */
/*TODO*///		/* COMRAM from   e000-ffff */
/*TODO*///	
/*TODO*///		ROM_REGION( 0x800, REGION_GFX1, ROMREGION_DISPOSE );
/*TODO*///		ROM_LOAD( "136029.124",     0x0000,  0x0800, 0x848948b6 );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x3420, REGION_PROMS, 0 );
/*TODO*///		ROM_LOAD( "ir125.bin",      0x0000,  0x0020, 0x446335ba );
/*TODO*///		ROM_LOAD( "ir111.bin",      0x0020,  0x0400, 0x9fbc9bf3 );/* program ROMs from c000-f3ff */
/*TODO*///		ROM_LOAD( "ir112.bin",      0x0420,  0x0400, 0xb2713214 );
/*TODO*///		ROM_LOAD( "ir113.bin",      0x0820,  0x0400, 0x7875930a );
/*TODO*///		ROM_LOAD( "ir114.bin",      0x0c20,  0x0400, 0x51d29666 );
/*TODO*///		ROM_LOAD( "ir115.bin",      0x1020,  0x0400, 0x00f9b304 );
/*TODO*///		ROM_LOAD( "ir116.bin",      0x1420,  0x0400, 0x326aba54 );
/*TODO*///		ROM_LOAD( "ir117.bin",      0x1820,  0x0400, 0x98efe8d0 );
/*TODO*///		ROM_LOAD( "ir118.bin",      0x1c20,  0x0400, 0x4a6aa7f9 );
/*TODO*///		ROM_LOAD( "ir119.bin",      0x2020,  0x0400, 0xa5a13ad8 );
/*TODO*///		ROM_LOAD( "ir120.bin",      0x2420,  0x0400, 0x2a083465 );
/*TODO*///		ROM_LOAD( "ir121.bin",      0x2820,  0x0400, 0xadebcb99 );
/*TODO*///		ROM_LOAD( "ir122.bin",      0x2c20,  0x0400, 0xda7b6f79 );
/*TODO*///		ROM_LOAD( "ir123.bin",      0x3020,  0x0400, 0x39fff18f );
/*TODO*///	ROM_END(); }}; 
/*TODO*///	
/*TODO*///		/*  Colorprom from John's driver. ? */
/*TODO*///		/*  ROM_LOAD( "136029.125",    0x0000, 0x0020, 0xc05abf82 );*/
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	public static GameDriver driver_irobot	   = new GameDriver("1983"	,"irobot"	,"irobot.java"	,rom_irobot,null	,machine_driver_irobot	,input_ports_irobot	,init_irobot	,ROT0	,	"Atari", "I, Robot" )
}
