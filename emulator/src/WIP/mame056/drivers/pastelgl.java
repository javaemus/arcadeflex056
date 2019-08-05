/******************************************************************************

	Game Driver for Nichibutsu Mahjong series.

	Pastel Gal
	(c)1985 Nihon Bussan Co.,Ltd.

	Driver by Takahiro Nogi <nogi@kt.rim.or.jp> 2000/06/07 -

******************************************************************************/
/******************************************************************************
Memo:

- Custom chip used by pastelgl PCB is 1411M1.

- Some games display "GFXROM BANK OVER!!" or "GFXROM ADDRESS OVER!!"
  in Debug build.

- Screen flip is not perfect.

******************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static WIP.mame056.machine.nb1413m3H.*;
import static WIP.mame056.machine.nb1413m3.*;
import static WIP.mame056.vidhrdw.pastelgl.*;

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
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;
import static mame056.sound.mixer.*;
import static mame056.sound.mixerH.*;
import static mame056.sound.dac.*;
import static mame056.sound.dacH.*;
import static mame056.sound.ay8910.*;
import static mame056.sound.ay8910H.*;
import static mame056.sound.pokey.*;
import static mame056.sound.pokeyH.*;

import static mame056.vidhrdw.generic.*;
import static mame056.timer.*;
import static mame056.timerH.*;

import static arcadeflex056.fileio.*;
import static mame056.palette.*;
// refactor
import static arcadeflex036.osdepend.logerror;

public class pastelgl
{
	
	
	public static int	SIGNED_DAC	= 0;		// 0:unsigned DAC, 1:signed DAC
	
	static int voiradr_l, voiradr_h;
	
	
	public static void pastelgl_voiradr_l_w(int data)
	{
		voiradr_l = data;
	}
	
	public static void pastelgl_voiradr_h_w(int data)
	{
		voiradr_h = data;
	}
	
	public static int pastelgl_sndrom_r(int offset)
	{
		UBytePtr ROM = new UBytePtr(memory_region(REGION_SOUND1));
	
		return ROM.read((((0x0100 * voiradr_h) + voiradr_l) & 0x7fff));
	}
	
	public static InitDriverPtr init_pastelgl = new InitDriverPtr() { public void handler()
	{
		nb1413m3_type = NB1413M3_PASTELGL;
		nb1413m3_int_count = 96;
	} };
	
	
	public static Memory_ReadAddress readmem_pastelgl[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0xbfff, MRA_ROM ),
		new Memory_ReadAddress( 0xe000, 0xe7ff, MRA_RAM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem_pastelgl[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0xbfff, MWA_ROM ),
		new Memory_WriteAddress( 0xe000, 0xe7ff, MWA_RAM, nb1413m3_nvram, nb1413m3_nvram_size ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	
	public static ReadHandlerPtr io_pastelgl_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		offset = (((offset & 0xff00) >> 8) | ((offset & 0x00ff) << 8));
	
		if (offset < 0x8000) return nb1413m3_sndrom_r(offset);
	
		switch (offset & 0xff00)
		{
			case	0x8100:	return AY8910_read_port_0_r.handler(0);
			case	0x9000:	return nb1413m3_inputport0_r();
			case	0xa000:	return nb1413m3_inputport1_r();
			case	0xb000:	return nb1413m3_inputport2_r();
			case	0xc000:	return pastelgl_sndrom_r(0);
			case	0xe000:	return input_port_2_r.handler(0);
			case	0xf000:	return nb1413m3_dipsw1_r();
			case	0xf100:	return nb1413m3_dipsw2_r();
			default:	return 0xff;
		}
	} };
	
	public static IO_ReadPort readport_pastelgl[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_ReadPort( 0x0000, 0xffff, io_pastelgl_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	
	public static WriteHandlerPtr io_pastelgl_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		offset = (((offset & 0xff00) >> 8) | ((offset & 0x00ff) << 8));
	
		if ((0xc000 <= offset) && (0xd000 > offset))
		{
			pastelgl_paltbl_w(((offset & 0x0f00) >> 8), data);
			return;
		}
	
		switch (offset & 0xff00)
		{
			case	0x0000:	break;
			case	0x8200:	AY8910_write_port_0_w.handler(0, data); break;
			case	0x8300:	AY8910_control_port_0_w.handler(0, data); break;
			case	0x9000:	pastelgl_radrx_w(data);
					pastelgl_voiradr_l_w(data); break;
			case	0x9100:	pastelgl_radry_w(data);
					pastelgl_voiradr_h_w(data); break;
			case	0x9200:	pastelgl_drawx_w(data); break;
			case	0x9300:	pastelgl_drawy_w(data); break;
			case	0x9400:	pastelgl_sizex_w(data); break;
			case	0x9500:	pastelgl_sizey_w(data); break;
			case	0x9600:	pastelgl_dispflag_w(data); break;
			case	0x9700:	break;
			case	0xa000:	nb1413m3_inputportsel_w(data); break;
			case	0xb000:	pastelgl_romsel_w(data);
					nb1413m3_sndrombank1_w(data);
					break;
	//#if SIGNED_DAC
	//		case	0xd000:	DAC_0_signed_data_w(0, data); break;
	//#else
			case	0xd000:	DAC_0_data_w.handler(0, data); break;
	//#endif
		}
	} };
	
	public static IO_WritePort writeport_pastelgl[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),
		new IO_WritePort( 0x0000, 0xffff, io_pastelgl_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	
	static InputPortPtr input_ports_pastelgl = new InputPortPtr(){ public void handler() { 
		PORT_START(); 	/* (0) DIPSW-A */
		PORT_DIPNAME( 0x03, 0x03, DEF_STR( "Difficulty") );
		PORT_DIPSETTING(    0x03, "1 (Easy)" );
		PORT_DIPSETTING(    0x02, "2" );
		PORT_DIPSETTING(    0x01, "3" );
		PORT_DIPSETTING(    0x00, "4 (Hard)" );
		PORT_BIT( 0xfc, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* (1) DIPSW-B */
		PORT_DIPNAME( 0x03, 0x00, "Number of last chance" );
		PORT_DIPSETTING(    0x03, "0" );
		PORT_DIPSETTING(    0x02, "1" );
		PORT_DIPSETTING(    0x01, "3" );
		PORT_DIPSETTING(    0x00, "10" );
		PORT_DIPNAME( 0x04, 0x04, "No. of tiles on final match" );
		PORT_DIPSETTING(    0x04, "20" );
		PORT_DIPSETTING(    0x00, "10" );
		PORT_BIT( 0x18, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_DIPNAME( 0x60, 0x00, "SANGEN Rush" );
		PORT_DIPSETTING(    0x06, "0" );
		PORT_DIPSETTING(    0x04, "1" );
		PORT_DIPSETTING(    0x02, "5" );
		PORT_DIPSETTING(    0x00, "infinite" );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
	
		PORT_START(); 	/* (2) DIPSW-C */
		PORT_DIPNAME( 0x03, 0x03, "Change Rate" );
		PORT_DIPSETTING(    0x03, "Type-A" );
		PORT_DIPSETTING(    0x02, "Type-B" );
		PORT_DIPSETTING(    0x01, "Type-C" );
		PORT_DIPSETTING(    0x00, "Type-D" );
		PORT_DIPNAME( 0x04, 0x00, "Open CPU's hand on Player's Reach" );
		PORT_DIPSETTING(    0x04, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
		PORT_BIT( 0x18, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_DIPNAME( 0x60, 0x60, "YAKUMAN cut" );
		PORT_DIPSETTING(    0x60, "10%" );
		PORT_DIPSETTING(    0x40, "30%" );
		PORT_DIPSETTING(    0x20, "50%" );
		PORT_DIPSETTING(    0x00, "90%" );
		PORT_DIPNAME( 0x80, 0x00, "Nudity" );
		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
	
		PORT_START(); 	/* (3) PORT 0 */
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNUSED );	// DRAW BUSY
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNUSED );	//
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE3 );	// MEMORY RESET
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_SERVICE2 );	// ANALYZER
		PORT_SERVICE( 0x10, IP_ACTIVE_LOW );		// TEST
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_COIN1 );	// COIN1
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_SERVICE4 );	// CREDIT CLEAR
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_SERVICE1 );	// SERVICE
	
		NBMJCTRL_PORT1();	/* (4) PORT 1-1 */
		NBMJCTRL_PORT2();	/* (5) PORT 1-2 */
		NBMJCTRL_PORT3();	/* (6) PORT 1-3 */
		NBMJCTRL_PORT4();	/* (7) PORT 1-4 */
		NBMJCTRL_PORT5();	/* (8) PORT 1-5 */
	INPUT_PORTS_END(); }}; 
	
	
	static AY8910interface ay8910_interface = new AY8910interface
	(
		1,				/* 1 chip */
		1250000,			/* 1.25 MHz ?? */
		new int[] { 35 },
		new ReadHandlerPtr[] { null },
		new ReadHandlerPtr[] { null },
		new WriteHandlerPtr[] { null },
		new WriteHandlerPtr[] { null }
	);
	
	
	static DACinterface dac_interface = new DACinterface
	(
		1,				/* 1 channels */
		new int[] { 50 }
	);
	
	//	     NAME, INT,  MAIN_RM,  MAIN_WM,  MAIN_RP,  MAIN_WP, NV_RAM
	//NBMJDRV( pastelgl,  96, pastelgl, pastelgl, pastelgl, pastelgl, nb1413m3_nvram_handler )
	//#define NBMJDRV(_name_, _intcnt_, _mrmem_, _mwmem_, _mrport_, _mwport_, _nvram_) 
	static MachineDriver machine_driver_pastelgl = new MachineDriver
	(															
		/* basic machine hardware */   							
		new MachineCPU[] {														
			new MachineCPU(
				CPU_Z80 | CPU_16BIT_PORT, 
				19968000/8,		/* 2.496 MHz ? */ 
				readmem_pastelgl, writemem_pastelgl, readport_pastelgl, writeport_pastelgl, 
				nb1413m3_interrupt, 96 
			)													
		},	
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION, 
		1, 
		null, 
	
		/* video hardware */ 
		256, 256, new rectangle( 0, 256-1, 16, 240-1 ), 
		null, 
		32, 0, 
		pastelgl_init_palette, 
	
		VIDEO_TYPE_RASTER, 
		null, 
		pastelgl_vh_start, 
		pastelgl_vh_stop, 
		pastelgl_vh_screenrefresh, 
	
		/* sound hardware */ 
		0, 0, 0, 0, 
		new MachineSound[] {														
			new MachineSound( 
				SOUND_AY8910, 
				ay8910_interface 
			), 
			new MachineSound( 
				SOUND_DAC, 
				dac_interface 
			) 
		}, 
		nb1413m3_nvram_handler
        );
	
	
	
	
	
	static RomLoadPtr rom_pastelgl = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* program */
		ROM_LOAD( "pgal_09.bin",  0x00000, 0x04000, 0x1e494af3 );
		ROM_LOAD( "pgal_10.bin",  0x04000, 0x04000, 0x677cccea );
		ROM_LOAD( "pgal_11.bin",  0x08000, 0x04000, 0xc2ccea38 );
	
		ROM_REGION( 0x08000, REGION_SOUND1, 0 );/* voice */
		ROM_LOAD( "pgal_08.bin",  0x00000, 0x08000, 0x895961a1 );
	
		ROM_REGION( 0x38000, REGION_GFX1, 0 );/* gfx */
		ROM_LOAD( "pgal_01.bin",  0x00000, 0x08000, 0x1bb14d52 );
		ROM_LOAD( "pgal_02.bin",  0x08000, 0x08000, 0xea85673a );
		ROM_LOAD( "pgal_03.bin",  0x10000, 0x08000, 0x40011248 );
		ROM_LOAD( "pgal_04.bin",  0x18000, 0x08000, 0x10613a66 );
		ROM_LOAD( "pgal_05.bin",  0x20000, 0x08000, 0x6a152703 );
		ROM_LOAD( "pgal_06.bin",  0x28000, 0x08000, 0xf56acfe8 );
		ROM_LOAD( "pgal_07.bin",  0x30000, 0x08000, 0xfa4226dc );
	
		ROM_REGION( 0x0040, REGION_PROMS, 0 );/* color */
		ROM_LOAD( "pgal_bp1.bin", 0x0000, 0x0020, 0x2b7fc61a );
		ROM_LOAD( "pgal_bp2.bin", 0x0020, 0x0020, 0x4433021e );
	ROM_END(); }}; 
	
	
	public static GameDriver driver_pastelgl	   = new GameDriver("1985"	,"pastelgl"	,"pastelgl.java"	,rom_pastelgl,null	,machine_driver_pastelgl	,input_ports_pastelgl	,init_pastelgl	,ROT0	,	"Nichibutsu", "Pastel Gal (Japan)" );
}
