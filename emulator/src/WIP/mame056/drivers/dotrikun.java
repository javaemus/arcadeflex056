/***************************************************************************

Dottori Kun (Head On's mini game)
(c)1990 SEGA

Driver by Takahiro Nogi (nogi@kt.rim.or.jp) 1999/12/15 -


CPU   : Z-80 (4MHz)
SOUND : (none)

14479.MPR  ; PRG (FIRST VER)
14479A.MPR ; PRG (NEW VER)

* This game is only for the test of cabinet
* BackRaster = WHITE on the FIRST version.
* BackRaster = BLACK on the NEW version.
* On the NEW version, push COIN-SW as TEST MODE.
* 0000-3FFF:ROM 8000-85FF:VRAM(128x96) 8600-87FF:WORK-RAM

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static WIP.mame056.vidhrdw.dotrikun.*;

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
import static mame056.sound.sn76496.*;
import static mame056.sound.sn76496H.*;

import static mame056.sound._2203intf.*;
import static mame056.sound._2203intfH.*;
import static mame056.vidhrdw.generic.*;

import static arcadeflex056.fileio.*;
import static mame056.palette.*;

public class dotrikun
{
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_ReadAddress( 0x0000, 0x3fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),	new Memory_WriteAddress( 0x0000, 0x3fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x87ff, dotrikun_videoram_w, videoram, videoram_size ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	public static IO_ReadPort readport[]={
		new IO_ReadPort(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_ReadPort( 0x00, 0x00, input_port_0_r ),
		new IO_ReadPort(MEMPORT_MARKER, 0)
	};
	public static IO_WritePort writeport[]={
		new IO_WritePort(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_IO | MEMPORT_WIDTH_8),	new IO_WritePort( 0x00, 0x00, dotrikun_color_w ),
		new IO_WritePort(MEMPORT_MARKER, 0)
	};
	
	static InputPortPtr input_ports_dotrikun = new InputPortPtr(){ public void handler() { 
	PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP );	PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN );	PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT );	PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT );	PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );	PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );	PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_START1 );	PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_COIN1 );INPUT_PORTS_END(); }}; 
	
	
	static MachineDriver machine_driver_dotrikun = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
				CPU_Z80,
				4000000,		 /* 4 MHz */
				readmem, writemem, readport, writeport,
				interrupt, 1
			)
		},
		60, DEFAULT_60HZ_VBLANK_DURATION,	/* frames per second, vblank duration */
		1,					/* single CPU, no need for interleaving */
		null,
	
		/* video hardware */
		256, 256, new rectangle( 0, 256-1, 0, 192-1 ),
		null,
		2, 0,
		null,
	
		VIDEO_TYPE_RASTER | VIDEO_SUPPORTS_DIRTY ,
		null,
		null,
		null,
		dotrikun_vh_screenrefresh,
	
		/* sound hardware */
		0, 0, 0, 0,
                null
	);
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_dotrikun = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "14479a.mpr",	0x0000, 0x4000, 0xb77a50db );ROM_END(); }}; 
	
	static RomLoadPtr rom_dotriku2 = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for code */
		ROM_LOAD( "14479.mpr",	0x0000, 0x4000, 0xa6aa7fa5 );ROM_END(); }}; 
	
	
	public static GameDriver driver_dotrikun	   = new GameDriver("1990"	,"dotrikun"	,"dotrikun.java"	,rom_dotrikun,null	,machine_driver_dotrikun	,input_ports_dotrikun	,null	,ROT0	,	"Sega", "Dottori Kun (new version)" );
	public static GameDriver driver_dotriku2	   = new GameDriver("1990"	,"dotriku2"	,"dotrikun.java"	,rom_dotriku2,driver_dotrikun	,machine_driver_dotrikun	,input_ports_dotrikun	,null	,ROT0	,	"Sega", "Dottori Kun (old version)" );
}
