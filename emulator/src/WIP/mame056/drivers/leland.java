/***************************************************************************

	Cinemat/Leland driver

	driver by Aaron Giles and Paul Leaman

	-------------------------------------

	To enter service mode in most games, press 1P start and then press
	the service switch (F2).

	For Redline Racer, hold the service switch down and reset the machine.

	For Super Offroad, press the blue nitro button (3P button 1) and then
	press the service switch.

	-------------------------------------

	Still to do:
		- memory map
		- generate fake serial numbers
		- kludge Quarterback sound

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
import static mame056.inptport.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;
import static mame056.driverH.*;
import static mame056.memoryH.*;
import static mame056.drawgfxH.*;
import static mame056.inputH.*;
import static mame056.sndintrfH.*;
import static mame056.sndintrf.*;
import static mame056.sound.samples.*;
import static mame056.sound.samplesH.*;
import static mame056.timer.*;
import static mame056.timerH.*;

import static WIP.mame056.vidhrdw.leland.*;
import static mame056.vidhrdw.generic.*;
import static WIP.mame056.sndhrdw.leland.*;

import static arcadeflex056.fileio.*;
import mame056.timer;


public class leland
{
	public static int leland_dac_control = 0;

}
