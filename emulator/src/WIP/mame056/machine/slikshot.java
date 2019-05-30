/***************************************************************************

	Slick Shot input handling

	Unlike the other 8-bit Strata games, Slick Shot has an interesting
	and fairly complex input system. The actual cabinet has a good-sized
	gap underneath the monitor, from which a small pool table emerges.
	An actual cue ball and pool sticks were included with the game.

	To "control" the game, players actually put the cue ball on the pool
	table and shot the ball into the gap. Four sensors underneath the
	monitor would count how long they saw the ball, and from this data,
	the velocity and crossing point of the ball could be derived.

	In order to read these sensors, an extra Z80 was added to the board.
	The Z80 program is astoundingly simple: on reset, it writes a value of
	$00 to the output port, then waits for either sensor 0 or 1 to fire.
	As soon as one of those sensors fires, it begins counting how long it
	takes for the bits corresponding to those sensors, as well as sensors
	2 and 3, to return to their 0 state. It then writes a $ff to the
	output port to signal that data is ready and waits for the main CPU
	to clock the data through.

	On the main program side of things, the result from the Z80 is
	periodically polled. Once a $ff is seen, 3 words and 1 bytes' worth
	of data is read from the Z80, after which the Z80 goes into an
	infinite loop. When the main program is ready to read a result again,
	it resets the Z80 to start the read going again.

	The way the Z80 reads the data, is as follows:

		- write $00 to output
		- wait for sensor 0 or 1 to fire (go to the 1 state)
		- count how long that sensor takes to return to 0
		- count how long sensors 2 and 3 take to return to 0
		- write $ff to output
		- wait for data to be clocked through
		- return 3 words + 1 byte of data:
			- word 0 = (value of larger of sensor 2/3 counts) - (value of smaller)
			- word 1 = value of smaller of sensor 2/3 counts
			- word 2 = value of sensor 0/1
			- byte = beam data
				- bit 0 = 1 if sensor 0 fired; 0 if sensor 1 fired
				- bit 1 = 1 if sensor 3 value > sensor 2 value; 0 otherwise
		- enter infinite loop

	Once this data is read from the Z80, it is converted to an intermediate
	form, and then processed using 32-bit math (yes, on a 6809!) to produce
	the final velocity and X position of the crossing.

	Because it is not understood exactly where the sensors are placed and
	how to simulate the actual behavior, this module attempts to do the
	next best thing: given a velocity and X position, figure out raw
	sensor values that will travel from the Z80 to the main 6809 and
	through the calculations produce approximately the correct results.

	There are several stages of data:

		- sens0, sens1, sens2, sens3 = raw sensor values
		- word1, word2, word3, beam = values from the Z80 (beam = byte val)
		- inter1, inter2, inter3, beam = intermediate forms in the 6809
		- vx, vy, x = final X,Y velocities and X crossing point

	And all the functions here are designed to take you through the various
	stages, both forwards and backwards, replicating the operations in the
	6809 or reversing them.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static java.lang.Math.abs;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.cpu.z80.z80H.Z80_PC;
import static mame056.timer.*;
import static mame056.timerH.*;
import static mame056.cpuexec.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame056.inptport.*;
import static mame056.mame.*;
import static mame056.palette.*;
// refactor
import static arcadeflex036.osdepend.logerror;

public class slikshot
{
	
	static int z80_ctrl;
	static int z80_port_val;
	static int z80_clear_to_send;
	
	static int nextsensor0, nextsensor1, nextsensor2, nextsensor3;
	static int sensor0, sensor1, sensor2, sensor3;
	
	static int curvx, curvy = 1, curx;
	static int lastshoot;
	
	
	
	/*************************************
	 *
	 *	sensors_to_words
	 *
	 *	converts from raw sensor data to
	 *	the three words + byte that the
	 *	Z80 sends to the main 6809
	 *
	 *************************************/
	
	static void sensors_to_words(int sens0, int sens1, int sens2, int sens3,
								ShortPtr word1, ShortPtr word2, ShortPtr word3, UBytePtr beams)
	{
		/* word 1 contains the difference between the larger of sensors 2 & 3 and the smaller */
		word1.write((short) ((sens3 > sens2) ? (sens3 - sens2) : (sens2 - sens3)));
	
		/* word 2 contains the value of the smaller of sensors 2 & 3 */
		word2.write((short) ((sens3 > sens2) ? sens2 : sens3));
	
		/* word 3 contains the value of sensor 0 or 1, depending on which fired */
		word3.write((short) (sens0!=0 ? sens0 : sens1));
	
		/* set the beams bits */
		beams.write( 0 );
	
		/* if sensor 1 fired first, set bit 0 */
		if (sens0 == 0)
			beams.write( beams.read() | 1 );
	
		/* if sensor 3 has the larger value, set bit 1 */
		if (sens3 > sens2)
			beams.write( beams.read() | 2 );
	}
	
	
	
	/*************************************
	 *
	 *	words_to_inters
	 *
	 *	converts the three words + byte
	 *	data from the Z80 into the three
	 *	intermediate values used in the
	 *	final calculations
	 *
	 *************************************/
	
	static void words_to_inters(int word1, int word2, int word3, int beams,
								ShortPtr inter1, ShortPtr inter2, ShortPtr inter3)
	{
		/* word 2 is scaled up by 0x1.6553 */
		int word2mod = (word2 * 0x16553) >> 16;
	
		/* intermediate values 1 and 2 are determined based on the beams bits */
		switch (beams)
		{
			case 0:
				inter1.write((short) (word1 + word2mod));
				inter2.write((short) (word2mod + word3));
				break;
	
			case 1:
				inter1.write((short) (word1 + word2mod + word3));
				inter2.write((short) word2mod);
				break;
	
			case 2:
				inter1.write((short) word2mod);
				inter2.write((short) (word1 + word2mod + word3));
				break;
	
			case 3:
				inter1.write((short) (word2mod + word3));
				inter2.write((short) (word1 + word2mod));
				break;
		}
	
		/* intermediate value 3 is always equal to the third word */
		inter3.write((short) word3);
	}
	
	
	
	/*************************************
	 *
	 *	inters_to_vels
	 *
	 *	converts the three intermediate
	 *	values to the final velocity and
	 *	X position values
	 *
	 *************************************/
	
	static void inters_to_vels(int inter1, int inter2, int inter3, int beams,
								UBytePtr xres, UBytePtr vxres, UBytePtr vyres)
	{
		int _27d8, _27c2;
		int vx, vy, _283a, _283e;
		int vxsgn;
		int xoffs = 0x0016;
		int xscale = 0xe6;
		int x;
	
		/* compute Vy */
		vy = inter1!=0 ? (0x31c28 / inter1) : 0;
	
		/* compute Vx */
		_283a = inter2!=0 ? (0x30f2e / inter2) : 0;
		_27d8 = (vy * 0xfbd3) >> 16;
		_27c2 = _283a - _27d8;
		vxsgn = 0;
		if (_27c2 < 0)
		{
			vxsgn = 1;
			_27c2 = _27d8 - _283a;
		}
		vx = (_27c2 * 0x58f8c) >> 16;
	
		/* compute X */
		_27d8 = ((inter3 << 16) * _283a) >> 16;
		_283e = (_27d8 * 0x4a574b) >> 16;
	
		/* adjust X based on the low bit of the beams */
		if ((beams & 1) != 0)
			x = 0x7a + (_283e >> 16) - xoffs;
		else
			x = 0x7a - (_283e >> 16) - xoffs;
	
		/* apply a constant X scale */
		if (xscale != 0)
			x = ((xscale * (x & 0xff)) >> 8) & 0xff;
	
		/* clamp if out of range */
		if ((vx & 0xffff) >= 0x80)
			x = 0;
	
		/* put the sign back in Vx */
		vx &= 0xff;
		if (vxsgn == 0)
			vx = -vx;
	
		/* clamp VY */
		if ((vy & 0xffff) > 0x7f)
			vy = 0x7f;
		else
			vy &= 0xff;
	
		/* copy the results */
		xres.write( x );
		vxres.write( vx );
		vyres.write( vy );
	}
	
	
	
	/*************************************
	 *
	 *	vels_to_inters
	 *
	 *	converts from the final velocity
	 *	and X position values back to
	 *	three intermediate values that
	 *	will produce the desired result
	 *
	 *************************************/
	
	static void vels_to_inters(int x, int vx, int vy,
								ShortPtr inter1, ShortPtr inter2, ShortPtr inter3, UBytePtr beams)
	{
		int _27d8;
		int xoffs = 0x0016;
		int xscale = 0xe6;
		int x1=0, vx1=0, vy1=0;
		int x2=0, vx2=0, vy2=0;
		int diff1, diff2;
		int inter2a;
	
		/* inter1 comes from Vy */
		inter1.write((short) (vy!=0 ? 0x31c28 / vy : 0));
	
		/* inter2 can be derived from Vx and Vy */
		_27d8 = (vy * 0xfbd3) >> 16;
		inter2.write((short) (0x30f2e / (_27d8 + ((abs(vx) << 16) / 0x58f8c))));
		inter2a = 0x30f2e / (_27d8 - ((abs(vx) << 16) / 0x58f8c));
	
		/* compute it back both ways and pick the closer */
		inters_to_vels(inter1.read(), inter2.read(), 0, 0, new UBytePtr(x1), new UBytePtr(vx1), new UBytePtr(vy1));
		inters_to_vels(inter1.read(), inter2a, 0, 0, new UBytePtr(x2), new UBytePtr(vx2), new UBytePtr(vy2));
		diff1 = (vx > vx1) ? (vx - vx1) : (vx1 - vx);
		diff2 = (vx > vx2) ? (vx - vx2) : (vx2 - vx);
		if (diff2 < diff1)
			inter2.write((short) inter2a);
	
		/* inter3: (beams & 1 == 1), inter3a: (beams & 1) == 0 */
		if (((x << 8) / xscale) + xoffs >= 0x7a)
		{
			beams.write( 1 );
			inter3.write((short) ((((((((((x << 8) / xscale) + xoffs - 0x7a)) << 16) << 16) / 0x4a574b) << 16) / (0x30f2e / inter2.read())) >> 16));
		}
		else
		{
			beams.write( 0 );
			inter3.write((short) ((((((((((x << 8) / xscale) + xoffs - 0x7a) * -1) << 16) << 16) / 0x4a574b) << 16) / (0x30f2e / inter2.read())) >> 16));
		}
	}
	
	
	
	/*************************************
	 *
	 *	inters_to_words
	 *
	 *	converts the intermediate values
	 *	used in the final calculations
	 *	back to the three words + byte
	 *	data from the Z80
	 *
	 *************************************/
	
	static void inters_to_words(int inter1, int inter2, int inter3, UBytePtr beams,
								ShortPtr word1, ShortPtr word2, ShortPtr word3)
	{
		int word2mod;
	
		/* intermediate value 3 is always equal to the third word */
		word3.write((short) inter3);
	
		/* on input, it is expected that the low bit of beams has already been determined */
		if ((beams.read() & 1) != 0)
		{
			/* make sure we can do it */
			if (inter3 <= inter1)
			{
				/* always go back via case 3 */
				beams.write( beams.read() | 2 );
	
				/* compute an appropriate value for the scaled version of word 2 */
				word2mod = inter1 - inter3;
	
				/* compute the other values from that */
				word1.write((short) (inter2 - word2mod));
				word2.write((short) ((word2mod << 16) / 0x16553));
			}
			else
				logerror("inters_to_words: unable to convert %04x %04x %04x %02x\n",
						inter1, inter2, inter3, beams.read());
		}
	
		/* handle the case where low bit of beams is 0 */
		else
		{
			/* make sure we can do it */
			if (inter3 <= inter2)
			{
				/* always go back via case 0 */
	
				/* compute an appropriate value for the scaled version of word 2 */
				word2mod = inter2 - inter3;
	
				/* compute the other values from that */
				word1.write((short) (inter1 - word2mod));
				word2.write((short) ((word2mod << 16) / 0x16553));
			}
			else
				logerror("inters_to_words: unable to convert %04x %04x %04x %02x\n",
						inter1, inter2, inter3, beams.read());
		}
	}
	
	
	
	/*************************************
	 *
	 *	words_to_sensors
	 *
	 *	converts from the three words +
	 *	byte that the Z80 sends to the
	 *	main 6809 back to raw sensor data
	 *
	 *************************************/
	
	static void words_to_sensors(int word1, int word2, int word3, int beams,
								ShortPtr sens0, ShortPtr sens1, ShortPtr sens2, ShortPtr sens3)
	{
		/* if bit 0 of the beams is set, sensor 1 fired first; otherwise sensor 0 fired */
		if ((beams & 1) != 0){
			sens0.write((short) 0 );
                        sens1.write((short) word3);
                } else {
			sens0.write((short) word3 );
                        sens1.write((short) 0 );
                }
	
		/* if bit 1 of the beams is set, sensor 3 had a larger value */
		if ((beams & 2) != 0){
			sens3.write((short) (word2 + word1));
                        sens2.write((short) word2);
                } else {
			sens2.write((short) (word2 + word1));
                        sens3.write((short) word2);
                }
	}
	
	
	
	/*************************************
	 *
	 *	compute_sensors
	 *
	 *************************************/
	
	static void compute_sensors()
	{
		int inter1=0, inter2=0, inter3=0;
		int word1=0, word2=0, word3=0;
		int beams=0;
	
		/* skip if we're not ready */
		if (sensor0 != 0 || sensor1 != 0 || sensor2 != 0 || sensor3 != 0)
			return;
	
		/* reverse map the inputs */
		vels_to_inters(curx, curvx, curvy, new ShortPtr(inter1), new ShortPtr(inter2), new ShortPtr(inter3), new UBytePtr(beams));
		inters_to_words(inter1, inter2, inter3, new UBytePtr(beams), new ShortPtr(word1), new ShortPtr(word2), new ShortPtr(word3));
		words_to_sensors(word1, word2, word3, beams, new ShortPtr(nextsensor0), new ShortPtr(nextsensor1), new ShortPtr(nextsensor2), new ShortPtr(nextsensor3));
	
		logerror("%15f: Sensor values: %04x %04x %04x %04x\n", timer_get_time(), nextsensor0, nextsensor1, nextsensor2, nextsensor3);
	}
	
	
	
	/*************************************
	 *
	 *	slikz80_port_r
	 *
	 *************************************/
	
	public static ReadHandlerPtr slikz80_port_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int result = 0;
	
		/* if we have nothing, return 0x03 */
		if (sensor0==0 && sensor1==0 && sensor2==0 && sensor3==0)
			return 0x03 | (z80_clear_to_send << 7);
	
		/* 1 bit for each sensor */
		if (sensor0 != 0){
			result |= 1;
                        sensor0--;
                }
		if (sensor1 != 0){
			result |= 2;
                        sensor1--;
                }
		if (sensor2 != 0){
			result |= 4;
                        sensor2--;
                }
		if (sensor3 != 0){
			result |= 8;
                        sensor3--;
                }
		result |= z80_clear_to_send << 7;
	
		return result;
	} };
	
	
	
	/*************************************
	 *
	 *	slikz80_port_w
	 *
	 *************************************/
	
	public static WriteHandlerPtr slikz80_port_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		z80_port_val = data;
		z80_clear_to_send = 0;
	} };
	
	
	
	/*************************************
	 *
	 *	slikshot_z80_r
	 *
	 *************************************/
	
	public static ReadHandlerPtr slikshot_z80_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* allow the Z80 to send us stuff now */
		z80_clear_to_send = 1;
		timer_set(TIME_NOW, 0, null);
	
		return z80_port_val;
	} };
	
	
	
	/*************************************
	 *
	 *	slikshot_z80_control_r
	 *
	 *************************************/
	
	public static ReadHandlerPtr slikshot_z80_control_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return z80_ctrl;
	} };
	
	
	
	/*************************************
	 *
	 *	slikshot_z80_control_w
	 *
	 *************************************/
	
	public static WriteHandlerPtr slikshot_z80_control_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int delta = z80_ctrl ^ data;
		z80_ctrl = data;
	
		/* reset the Z80 on bit 4 changing */
		if ((delta & 0x10) != 0)
		{
	//		logerror("%15f: Reset Z80: %02x  PC=%04x\n", timer_get_time(), data & 0x10, cpunum_get_reg(2, Z80_PC));
	
			/* this is a big kludge: only allow a reset if the Z80 is stopped */
			/* at its endpoint; otherwise, we never get a result from the Z80 */
			if ((data & 0x10)!=0 || cpunum_get_reg(2, Z80_PC) == 0x13a)
			{
				cpu_set_reset_line(2, (data & 0x10)!=0 ? CLEAR_LINE : ASSERT_LINE);
	
				/* on the rising edge, do housekeeping */
				if ((data & 0x10) != 0)
				{
					sensor0 = nextsensor0;
					sensor1 = nextsensor1;
					sensor2 = nextsensor2;
					sensor3 = nextsensor3;
					nextsensor0 = nextsensor1 = nextsensor2 = nextsensor3 = 0;
					z80_clear_to_send = 0;
				}
			}
		}
	
		/* on bit 5 going live, this looks like a clock, but the system */
		/* won't work with it configured as such */
		if ((delta & data & 0x20) != 0)
		{
	//		logerror("%15f: Clock edge high\n", timer_get_time());
		}
	} };
	
	
	
	/*************************************
	 *
	 *	slikshot_extra_draw
	 *
	 *	render a line representing the
	 *	current X crossing and the
	 *	velocities
	 *
	 *************************************/
	
	public static void slikshot_extra_draw(mame_bitmap bitmap)
	{
		int vx = readinputport(3);
		int vy = readinputport(4);
		int xpos = readinputport(5);
		int xstart, ystart, xend, yend;
		int dx, dy, absdx, absdy;
		int count, i;
		int newshoot;
	
		/* make sure color 256 is white for our crosshair */
		palette_set_color(256, 0xff, 0xff, 0xff);
	
		/* compute the updated values */
		curvx = vx;
		curvy = (vy < 1) ? 1 : vy;
		curx = xpos;
	
		/* if the shoot button is pressed, fire away */
		newshoot = readinputport(2) & 1;
		if (newshoot!=0 && lastshoot==0)
		{
			compute_sensors();
	//		usrintf_showmessage("V=%02x,%02x  X=%02x", curvx, curvy, curx);
		}
		lastshoot = newshoot;
	
		/* draw a crosshair (rotated) */
		xstart = (((int)curx - 0x60) * 0x100 / 0xd0) + 144;
		ystart = 256 - 48;
		xend = xstart + curvx;
		yend = ystart - curvy;
	
		/* compute line params */
		dx = xend - xstart;
		dy = yend - ystart;
		absdx = (dx < 0) ? -dx : dx;
		absdy = (dy < 0) ? -dy : dy;
		if (absdx > absdy)
		{
			dy = absdx!=0 ? ((dy << 16) / absdx) : 0;
			dx = (dx < 0) ? -0x10000 : 0x10000;
			count = absdx;
		}
		else
		{
			dx = absdy!=0 ? ((dx << 16) / absdy) : 0;
			dy = (dy < 0) ? -0x10000 : 0x10000;
			count = absdy;
		}
	
		/* scale the start points */
		xstart <<= 16;
		ystart <<= 16;
	
		/* draw the line */
		for (i = 0; i < count; i++)
		{
			int px = xstart >> 16, py = ystart >> 16;
	
			if (px >= 0 && px < bitmap.width &&
				py >= 0 && py < bitmap.height)
			{
				if (bitmap.depth == 8)
					new UBytePtr(bitmap.line[py]).write(px, Machine.pens[256]);
				else
					new UShortPtr(bitmap.line[py]).write(px, (char) Machine.pens[256]);
			}
			xstart += dx;
			ystart += dy;
		}
	}
	
	
	
	/*************************************
	 *
	 *	main
	 *
	 *	uncomment this to make a stand
	 *	alone version for testing
	 *
	 *************************************/
	
	/*TODO*///#if 0
	
	/*TODO*///int main(int argc, char *argv[])
	/*TODO*///{
	/*TODO*///	UINT16 word1, word2, word3;
	/*TODO*///	UINT16 inter1, inter2, inter3;
	/*TODO*///	UINT8 beams, x, vx, vy;
	/*TODO*///
	/*TODO*///	if (argc == 5)
	/*TODO*///	{
	/*TODO*///		unsigned int sens0, sens1, sens2, sens3;
	/*TODO*///
	/*TODO*///		sscanf(argv[1], "%x", &sens0);
	/*TODO*///		sscanf(argv[2], "%x", &sens1);
	/*TODO*///		sscanf(argv[3], "%x", &sens2);
	/*TODO*///		sscanf(argv[4], "%x", &sens3);
	/*TODO*///		printf("sensors: %04x %04x %04x %04x\n", sens0, sens1, sens2, sens3);
	/*TODO*///		if (sens0 && sens1)
	/*TODO*///		{
	/*TODO*///			printf("error: sensor 0 or 1 must be 0\n");
	/*TODO*///			return 1;
	/*TODO*///		}
	/*TODO*///
	/*TODO*///		sensors_to_words(sens0, sens1, sens2, sens3, &word1, &word2, &word3, &beams);
	/*TODO*///		printf("word1 = %04x  word2 = %04x  word3 = %04x  beams = %d\n",
	/*TODO*///				(UINT32)word1, (UINT32)word2, (UINT32)word3, (UINT32)beams);
	/*TODO*///
	/*TODO*///		words_to_inters(word1, word2, word3, beams, &inter1, &inter2, &inter3);
	/*TODO*///		printf("inter1 = %04x  inter2 = %04x  inter3 = %04x\n", (UINT32)inter1, (UINT32)inter2, (UINT32)inter3);
	
	/*TODO*///		inters_to_vels(inter1, inter2, inter3, beams, &x, &vx, &vy);
	/*TODO*///		printf("x = %02x  vx = %02x  vy = %02x\n", (UINT32)x, (UINT32)vx, (UINT32)vy);
	/*TODO*///	}
	/*TODO*///	else if (argc == 4)
	/*TODO*///	{
	/*TODO*///		unsigned int xin, vxin, vyin;
	/*TODO*///		UINT16 sens0, sens1, sens2, sens3;
	/*TODO*///
	/*TODO*///		sscanf(argv[1], "%x", &xin);
	/*TODO*///		sscanf(argv[2], "%x", &vxin);
	/*TODO*///		sscanf(argv[3], "%x", &vyin);
	/*TODO*///		x = xin;
	/*TODO*///		vx = vxin;
	/*TODO*///		vy = vyin;
	/*TODO*///		printf("x = %02x  vx = %02x  vy = %02x\n", (UINT32)x, (UINT32)vx, (UINT32)vy);
	/*TODO*///
	/*TODO*///		vels_to_inters(x, vx, vy, &inter1, &inter2, &inter3, &beams);
	/*TODO*///		printf("inter1 = %04x  inter2 = %04x  inter3 = %04x  beams = %d\n", (UINT32)inter1, (UINT32)inter2, (UINT32)inter3, (UINT32)beams);
	/*TODO*///
	/*TODO*///		inters_to_words(inter1, inter2, inter3, &beams, &word1, &word2, &word3);
	/*TODO*///		printf("word1 = %04x  word2 = %04x  word3 = %04x  beams = %d\n",
	/*TODO*///				(UINT32)word1, (UINT32)word2, (UINT32)word3, (UINT32)beams);
	
	/*TODO*///		words_to_sensors(word1, word2, word3, beams, &sens0, &sens1, &sens2, &sens3);
	/*TODO*///		printf("sensors: %04x %04x %04x %04x\n", sens0, sens1, sens2, sens3);
	/*TODO*///	}
	
	/*TODO*///	return 0;
	/*TODO*///}
	
	/*TODO*///#endif
}
