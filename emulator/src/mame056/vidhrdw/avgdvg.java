/*
 * avgdvg.c: Atari DVG and AVG simulators
 *
 * Copyright 1991, 1992, 1996 Eric Smith
 *
 * Modified for the MAME project 1997 by
 * Brad Oliver, Bernd Wiebelt, Aaron Giles, Andrew Caldwell
 *
 * 971108 Disabled vector timing routines, introduced an ugly (but fast!)
 *        busy flag hack instead. BW
 * 980202 New anti aliasing code by Andrew Caldwell (.ac)
 * 980206 New (cleaner) busy flag handling.
 *        Moved LBO's buffered point into generic vector code. BW
 * 980212 Introduced timing code based on Aaron timer routines. BW
 * 980318 Better color handling, Bzone and MHavoc clipping. BW
 *
 * Battlezone uses a red overlay for the top of the screen and a green one
 * for the rest. There is a circuit to clip color 0 lines extending to the
 * red zone. This is emulated now. Thanks to Neil Bradley for the info. BW
 *
 * Frame and interrupt rates (Neil Bradley) BW
 * ~60 fps/4.0ms: Asteroid, Asteroid Deluxe
 * ~40 fps/4.0ms: Lunar Lander
 * ~40 fps/4.1ms: Battle Zone
 * ~45 fps/5.4ms: Space Duel, Red Baron
 * ~30 fps/5.4ms: StarWars
 *
 * Games with self adjusting framerate
 *
 * 4.1ms: Black Widow, Gravitar
 * 4.1ms: Tempest
 * Major Havoc
 * Quantum
 *
 * TODO: accurate vector timing (need timing diagramm)
 */

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.vidhrdw;

import static java.lang.Math.abs;
import static mame056.vidhrdw.avgdvgH.*;
import static mame056.vidhrdw.vector.*;
import static mame056.vidhrdw.vectorH.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static arcadeflex056.fucPtr.*;
import static common.libc.cstdlib.rand;
import common.ptr.UBytePtr;
import static mame056.common.memory_region;
import static mame056.commonH.REGION_CPU1;
import static mame056.mame.Machine;
import static mame056.timerH.*;
import static mame056.timer.*;

public class avgdvg
{
	
	public static final int VEC_SHIFT = 16;	/* fixed for the moment */
	public static final int BRIGHTNESS = 12;   /* for maximum brightness, use 16! */
	
	
	/* the screen is red above this Y coordinate */
	public static int BZONE_TOP = 0x0050;
        
        /* These hold the X/Y coordinates the vector engine uses */
	static int width; static int height;
	static int xcenter; static int ycenter;
	static int xmin; static int xmax;
	static int ymin; static int ymax;
	
        public static void BZONE_CLIP(){
            int a = xmin<<VEC_SHIFT;
            int b = (BZONE_TOP<<VEC_SHIFT);
            int c = (xmax<<VEC_SHIFT);
            int d = (ymax<<VEC_SHIFT);
		vector_add_clip(
                        a,
                        b, 
                        c, 
                        d
                );
        }
        
	public static void BZONE_NOCLIP(){
            int a = (xmin<<VEC_SHIFT);
            int b = (ymin <<VEC_SHIFT);
            int c = (xmax<<VEC_SHIFT);
            int d = (ymax<<VEC_SHIFT);
            
		vector_add_clip(a, b, c, d);
        }
	
	public static int MHAVOC_YWINDOW = 0x0048;
        
	public static void MHAVOC_CLIP(){
		vector_add_clip (xmin<<VEC_SHIFT, MHAVOC_YWINDOW<<VEC_SHIFT,
						xmax<<VEC_SHIFT, ymax<<VEC_SHIFT);
        }
        
	public static void MHAVOC_NOCLIP(){
		vector_add_clip (xmin<<VEC_SHIFT, ymin <<VEC_SHIFT,
						xmax<<VEC_SHIFT, ymax<<VEC_SHIFT);
        }
	
	static int vectorEngine = USE_DVG;
	static int flipword = 0; /* little/big endian issues */
	static int busy = 0;     /* vector engine busy? */
	static int[] colorram=new int[16]; /* colorram entries */
	
	
	
	
	static int vector_updates; /* avgdvg_go_w()'s per Mame frame, should be 1 */
	
	static int vg_step = 0;    /* single step the vector generator */
	static int total_length;   /* length of all lines drawn in a frame */
	
	public static int  MAXSTACK = 8; 	/* Tempest needs more than 4     BW 210797 */
	
	/* AVG commands */
	public static final int VCTR = 0;
	public static final int HALT = 1;
	public static final int SVEC = 2;
	public static final int STAT = 3;
	public static final int CNTR = 4;
	public static final int JSRL = 5;
	public static final int RTSL = 6;
	public static final int JMPL = 7;
	public static final int SCAL = 8;
	
	/* DVG commands */
	public static final int DVCTR = 0x01;
	public static final int DLABS = 0x0a;
	public static final int DHALT = 0x0b;
	public static final int DJSRL = 0x0c;
	public static final int DRTSL = 0x0d;
	public static final int DJMPL = 0x0e;
	public static final int DSVEC = 0x0f;
	
	public static int twos_comp_val(int num, int bits){
            return (((num&(1<<(bits-1)))!=0)?(num|~((1<<bits)-1)):(num&((1<<bits)-1)));
        }
	
	static String avg_mnem[] = { "vctr", "halt", "svec", "stat", "cntr",
				 "jsrl", "rtsl", "jmpl", "scal" };
	
	static String dvg_mnem[] = { "????", "vct1", "vct2", "vct3",
			     "vct4", "vct5", "vct6", "vct7",
			     "vct8", "vct9", "labs", "halt",
			     "jsrl", "rtsl", "jmpl", "svec" };
	
	/* ASG 971210 -- added banks and modified the read macros to use them */
	public static int BANK_BITS = 13;
	public static int BANK_SIZE = (1<<BANK_BITS);
	public static int NUM_BANKS = (0x4000/BANK_SIZE);
	
        public static int VECTORRAM(int offset){
            return vectorbank[(offset)>>BANK_BITS].read((offset)&(BANK_SIZE-1));
        }
	
        public static UBytePtr[] vectorbank=new UBytePtr[NUM_BANKS];
	
	public static int map_addr(int n){
            return (((n)<<1));
        }
        
	public static int memrdwd(int offset){
            return (VECTORRAM(offset) | (VECTORRAM(offset+1)<<8));
        }
        
	/* The AVG used by Star Wars reads the bytes in the opposite order */
	public static int memrdwd_flip(int offset){
            return (VECTORRAM(offset+1) | (VECTORRAM(offset)<<8));
        }	
	
	public static void vector_timer (int deltax, int deltay)
	{
		deltax = abs (deltax);
		deltay = abs (deltay);
		if (deltax > deltay)
			total_length += deltax >> VEC_SHIFT;
		else
			total_length += deltay >> VEC_SHIFT;
	}
	
	public static void dvg_vector_timer (int scale)
	{
		total_length += scale;
	}
	
	static void dvg_generate_vector_list()
	{
		int pc;
		int sp;
		int[] stack = new int[MAXSTACK];
	
		int scale;
		int statz;
	
		int currentx, currenty;
	
		int done = 0;
	
		int firstwd;
		int secondwd = 0; /* Initialize to tease the compiler */
		int opcode;
	
		int x, y;
		int z, temp;
		int a;
	
		int deltax, deltay;
	
		vector_clear_list();
		pc = 0;
		sp = 0;
		scale = 0;
		statz = 0;
	
		currentx = 0;
		currenty = 0;
	
		while (done==0)
		{
	
	/*#ifdef VG_DEBUG
			if (vg_step)
			{
		  		logerror("Current beam position: (%d, %d)\n",
					currentx, currenty);
		  		getchar();
			}
	#endif*/
	
			firstwd = memrdwd (map_addr (pc));
			opcode = firstwd >> 12;
	/*#ifdef VG_DEBUG
			logerror("%4x: %4x ", map_addr (pc), firstwd);
	#endif*/
			pc++;
			if ((opcode >= 0 /* DVCTR */) && (opcode <= DLABS))
			{
				secondwd = memrdwd (map_addr (pc));
				pc++;
	/*#ifdef VG_DEBUG
				logerror("%s ", dvg_mnem [opcode]);
				logerror("%4x  ", secondwd);
	#endif*/
			}
	/*#ifdef VG_DEBUG
			else logerror("Illegal opcode ");
	#endif*/
	
			switch (opcode)
			{
				case 0:
	/*#ifdef VG_DEBUG
		 			logerror("Error: DVG opcode 0!  Addr %4x Instr %4x %4x\n", map_addr (pc-2), firstwd, secondwd);
					done = 1;
					break;
	#endif*/
				case 1:
				case 2:
				case 3:
				case 4:
				case 5:
				case 6:
				case 7:
				case 8:
				case 9:
		  			y = firstwd & 0x03ff;
					if ((firstwd & 0x400) != 0)
						y=-y;
					x = secondwd & 0x3ff;
					if ((secondwd & 0x400) != 0)
						x=-x;
					z = secondwd >> 12;
	/*#ifdef VG_DEBUG
					logerror("(%d,%d) z: %d scal: %d", x, y, z, opcode);
	#endif*/
		  			temp = ((scale + opcode) & 0x0f);
		  			if (temp > 9)
						temp = -1;
		  			deltax = (x << VEC_SHIFT) >> (9-temp);		/* ASG 080497 */
					deltay = (y << VEC_SHIFT) >> (9-temp);		/* ASG 080497 */
		  			currentx += deltax;
					currenty -= deltay;
					dvg_vector_timer(temp);
	
					/* ASG 080497, .ac JAN2498 - V.V */
					if ((translucency) != 0)
						z = z * BRIGHTNESS;
					else
						if (z != 0) z = (z << 4) | 0x0f;
					vector_add_point (currentx, currenty, VECTOR_COLOR111(colorram[1]), z);
	
					break;
	
				case DLABS:
					x = twos_comp_val (secondwd, 12);
					y = twos_comp_val (firstwd, 12);
		  			scale = (secondwd >> 12);
					currentx = ((x-xmin) << VEC_SHIFT);		/* ASG 080497 */
					currenty = ((ymax-y) << VEC_SHIFT);		/* ASG 080497 */
	/*#ifdef VG_DEBUG
					logerror("(%d,%d) scal: %d", x, y, secondwd >> 12);
	#endif*/
					break;
	
				case DHALT:
	/*#ifdef VG_DEBUG
					if ((firstwd & 0x0fff) != 0)
	      				logerror("(%d?)", firstwd & 0x0fff);
	#endif*/
					done = 1;
					break;
	
				case DJSRL:
					a = firstwd & 0x0fff;
	/*#ifdef VG_DEBUG
					logerror("%4x", map_addr(a));
	#endif*/
					stack [sp] = pc;
					if (sp == (MAXSTACK - 1))
		    			{
						logerror("\n*** Vector generator stack overflow! ***\n");
						done = 1;
						sp = 0;
					}
					else
						sp++;
					pc = a;
					break;
	
				case DRTSL:
	/*#ifdef VG_DEBUG
					if ((firstwd & 0x0fff) != 0)
						 logerror("(%d?)", firstwd & 0x0fff);
	#endif*/
					if (sp == 0)
		    			{
						logerror("\n*** Vector generator stack underflow! ***\n");
						done = 1;
						sp = MAXSTACK - 1;
					}
					else
						sp--;
					pc = stack [sp];
					break;
	
				case DJMPL:
					a = firstwd & 0x0fff;
	/*#ifdef VG_DEBUG
					logerror("%4x", map_addr(a));
	#endif*/
					pc = a;
					break;
	
				case DSVEC:
					y = firstwd & 0x0300;
					if ((firstwd & 0x0400) != 0)
						y = -y;
					x = (firstwd & 0x03) << 8;
					if ((firstwd & 0x04) != 0)
						x = -x;
					z = (firstwd >> 4) & 0x0f;
					temp = 2 + ((firstwd >> 2) & 0x02) + ((firstwd >>11) & 0x01);
		  			temp = ((scale + temp) & 0x0f);
					if (temp > 9)
						temp = -1;
	/*#ifdef VG_DEBUG
					logerror("(%d,%d) z: %d scal: %d", x, y, z, temp);
	#endif*/
	
					deltax = (x << VEC_SHIFT) >> (9-temp);	/* ASG 080497 */
					deltay = (y << VEC_SHIFT) >> (9-temp);	/* ASG 080497 */
		  			currentx += deltax;
					currenty -= deltay;
					dvg_vector_timer(temp);
	
					/* ASG 080497, .ac JAN2498 */
					if (translucency != 0)
						z = z * BRIGHTNESS;
					else
						if (z != 0) z = (z << 4) | 0x0f;
					vector_add_point (currentx, currenty, VECTOR_COLOR111(colorram[1]), z);
					break;
	
				default:
					logerror("Unknown DVG opcode found\n");
					done = 1;
			}
	/*#ifdef VG_DEBUG
	      		logerror("\n");
	#endif*/
		}
	}
	
	/*
	Atari Analog Vector Generator Instruction Set
	
	Compiled from Atari schematics and specifications
	Eric Smith  7/2/92
	---------------------------------------------
	
	NOTE: The vector generator is little-endian.  The instructions are 16 bit
	      words, which need to be stored with the least significant byte in the
	      lower (even) address.  They are shown here with the MSB on the left.
	
	The stack allows four levels of subroutine calls in the TTL version, but only
	three levels in the gate array version.
	
	inst  bit pattern          description
	----  -------------------  -------------------
	VCTR  000- yyyy yyyy yyyy  normal vector
	      zzz- xxxx xxxx xxxx
	HALT  001- ---- ---- ----  halt - does CNTR also on newer hardware
	SVEC  010y yyyy zzzx xxxx  short vector - don't use zero length
	STAT  0110 ---- zzzz cccc  status
	SCAL  0111 -bbb llll llll  scaling
	CNTR  100- ---- dddd dddd  center
	JSRL  101a aaaa aaaa aaaa  jump to subroutine
	RTSL  110- ---- ---- ----  return
	JMPL  111a aaaa aaaa aaaa  jump
	
	-     unused bits
	x, y  relative x and y coordinates in two's complement (5 or 13 bit,
	      5 bit quantities are scaled by 2, so x=1 is really a length 2 vector.
	z     intensity, 0 = blank, 1 means use z from STAT instruction,  2-7 are
	      doubled for actual range of 4-14
	c     color
	b     binary scaling, multiplies all lengths by 2**(1-b), 0 is double size,
	      1 is normal, 2 is half, 3 is 1/4, etc.
	l     linear scaling, multiplies all lengths by 1-l/256, don't exceed $80
	d     delay time, use $40
	a     address (word address relative to base of vector memory)
	
	Notes:
	
	Quantum:
	        the VCTR instruction has a four bit Z field, that is not
	        doubled.  The value 2 means use Z from STAT instruction.
	
	        the SVEC instruction can't be used
	
	Major Havoc:
	        SCAL bit 11 is used for setting a Y axis window.
	
	        STAT bit 11 is used to enable "sparkle" color.
	        STAT bit 10 inverts the X axis of vectors.
	        STAT bits 9 and 8 are the Vector ROM bank select.
	
	Star Wars:
	        STAT bits 10, 9, and 8 are used directly for R, G, and B.
	*/
	
	static void avg_generate_vector_list ()
	{
	
		int pc;
		int sp;
		int[] stack = new int[MAXSTACK];
	
		int scale;
		int statz   = 0;
		int sparkle = 0;
		int xflip   = 0;
	
		int color   = 0;
		int bz_col  = -1; /* Battle Zone color selection */
		int ywindow = -1; /* Major Havoc Y-Window */
	
		int currentx, currenty;
		int done    = 0;
	
		int firstwd, secondwd;
		int opcode;
	
		int x, y, z=0, b, l, d, a;
	
		int deltax, deltay;
	
	
		pc = 0;
		sp = 0;
		statz = 0;
		color = 0;
	
		if (flipword != 0)
		{
			firstwd = memrdwd_flip (map_addr (pc));
			secondwd = memrdwd_flip (map_addr (pc+1));
		}
		else
		{
			firstwd = memrdwd (map_addr (pc));
			secondwd = memrdwd (map_addr (pc+1));
		}
		if ((firstwd == 0) && (secondwd == 0))
		{
			logerror("VGO with zeroed vector memory\n");
			return;
		}
	
		/* kludge to bypass Major Havoc's empty frames. BW 980216 */
		if (vectorEngine == USE_AVG_MHAVOC && firstwd == 0xafe2)
			return;
	
		scale = 0;          /* ASG 080497 */
		currentx = xcenter; /* ASG 080497 */ /*.ac JAN2498 */
		currenty = ycenter; /* ASG 080497 */ /*.ac JAN2498 */
	
		vector_clear_list();
	
		while (done==0)
		{
	
	/*#ifdef VG_DEBUG
			if (vg_step) getchar();
	#endif*/
	
			if (flipword != 0) firstwd = memrdwd_flip (map_addr (pc));
			else          firstwd = memrdwd      (map_addr (pc));
	
			opcode = firstwd >> 13;
	/*#ifdef VG_DEBUG
			logerror("%4x: %4x ", map_addr (pc), firstwd);
	#endif*/
			pc++;
			if (opcode == VCTR)
			{
				if (flipword != 0) secondwd = memrdwd_flip (map_addr (pc));
				else          secondwd = memrdwd      (map_addr (pc));
				pc++;
	/*#ifdef VG_DEBUG
				logerror("%4x  ", secondwd);
	#endif*/
			}
	/*#ifdef VG_DEBUG
			else logerror("      ");
	#endif*/
	
			if ((opcode == STAT) && ((firstwd & 0x1000) != 0))
				opcode = SCAL;
	
	/*#ifdef VG_DEBUG
			logerror("%s ", avg_mnem [opcode]);
	#endif*/
	
			switch (opcode)
			{
				case VCTR:
	
					if (vectorEngine == USE_AVG_QUANTUM)
					{
						x = twos_comp_val (secondwd, 12);
						y = twos_comp_val (firstwd, 12);
					}
					else
					{
						/* These work for all other games. */
						x = twos_comp_val (secondwd, 13);
						y = twos_comp_val (firstwd, 13);
					}
					z = (secondwd >> 12) & ~0x01;
	
					/* z is the maximum DAC output, and      */
					/* the 8 bit value from STAT does some   */
					/* fine tuning. STATs of 128 should give */
					/* highest intensity. */
					if (vectorEngine == USE_AVG_SWARS)
					{
						if (translucency != 0)
							z = (statz * z) / 12;
						else
							z = (statz * z) >> 3;
						if (z > 0xff)
							z = 0xff;
					}
					else
					{
						if (z == 2)
							z = statz;
							if (translucency != 0)
								z = z * BRIGHTNESS;
							else
								if (z != 0) z = (z << 4) | 0x1f;
					}
	
					deltax = x * scale;
					if (xflip != 0) deltax = -deltax;
	
					deltay = y * scale;
					currentx += deltax;
					currenty -= deltay;
					vector_timer(deltax, deltay);
	
					if (sparkle != 0)
					{
						color = rand() & 0x07;
					}
	
					if ((vectorEngine == USE_AVG_BZONE) && (bz_col != 0))
					{
						if (currenty < (BZONE_TOP<<16))
							color = 4;
						else
							color = 2;
					}
	
					vector_add_point (currentx, currenty, VECTOR_COLOR111(colorram[color]), z);
	
	/*#ifdef VG_DEBUG
					logerror("VCTR x:%d y:%d z:%d statz:%d", x, y, z, statz);
	#endif*/
					break;
	
				case SVEC:
					x = twos_comp_val (firstwd, 5) << 1;
					y = twos_comp_val (firstwd >> 8, 5) << 1;
					z = ((firstwd >> 4) & 0x0e);
	
					if (vectorEngine == USE_AVG_SWARS)
					{
						if (translucency != 0)
							z = (statz * z) / 12;
						else
							z = (statz * z) >> 3;
						if (z > 0xff) z = 0xff;
					}
					else
					{
						if (z == 2)
							z = statz;
							if (translucency != 0)
								z = z * BRIGHTNESS;
							else
								if (z != 0) z = (z << 4) | 0x1f;
					}
	
					deltax = x * scale;
					if (xflip != 0) deltax = -deltax;
	
					deltay = y * scale;
					currentx += deltax;
					currenty -= deltay;
					vector_timer(deltax, deltay);
	
					if (sparkle != 0)
					{
						color = rand() & 0x07;
					}
	
					vector_add_point (currentx, currenty, VECTOR_COLOR111(colorram[color]), z);
	
	/*#ifdef VG_DEBUG
					logerror("SVEC x:%d y:%d z:%d statz:%d", x, y, z, statz);
	#endif*/
					break;
	
				case STAT:
					if (vectorEngine == USE_AVG_SWARS)
					{
						/* color code 0-7 stored in top 3 bits of `color' */
						color=(char)((firstwd & 0x0700)>>8);
						statz = (firstwd) & 0xff;
					}
					else
					{
						color = (firstwd) & 0x000f;
						statz = (firstwd >> 4) & 0x000f;
						if (vectorEngine == USE_AVG_TEMPEST)
									sparkle = ((firstwd & 0x0800)!=0)?0:1;
						if (vectorEngine == USE_AVG_MHAVOC)
						{
							sparkle = (firstwd & 0x0800);
							xflip = firstwd & 0x0400;
							/* Bank switch the vector ROM for Major Havoc */
							vectorbank[1]=(new UBytePtr(memory_region(REGION_CPU1), 0x18000 + ((firstwd & 0x300) >> 8) * 0x2000));
						}
						if (vectorEngine == USE_AVG_BZONE)
						{
							bz_col = color;
							if (color == 0)
							{
								BZONE_CLIP();
								color = 2;
							}
							else
							{
								BZONE_NOCLIP();
							}
						}
					}
	/*#ifdef VG_DEBUG
					logerror("STAT: statz: %d color: %d", statz, color);
					if (xflip || sparkle)
						logerror("xflip: %02x  sparkle: %02x\n", xflip, sparkle);
	#endif*/
	
					break;
	
				case SCAL:
					b = ((firstwd >> 8) & 0x07)+8;
					l = (~firstwd) & 0xff;
					scale = (l << VEC_SHIFT) >> b;		/* ASG 080497 */
	
					/* Y-Window toggle for Major Havoc BW 980318 */
					if (vectorEngine == USE_AVG_MHAVOC)
					{
						if ((firstwd & 0x0800) != 0)
						{
							logerror("CLIP %d\n", firstwd & 0x0800);
							if (ywindow == 0)
							{
								ywindow = 1;
								MHAVOC_CLIP();
							}
							else
							{
								ywindow = 0;
								MHAVOC_NOCLIP();
							}
						}
					}
	/*#ifdef VG_DEBUG
					logerror("bin: %d, lin: ", b);
					if (l > 0x80)
						logerror("(%d?)", l);
					else
						logerror("%d", l);
					logerror(" scale: %f", (scale/(float)(1<<VEC_SHIFT)));
	#endif*/
					break;
	
				case CNTR:
					d = firstwd & 0xff;
	/*#ifdef VG_DEBUG
					if (d != 0x40) logerror("%d", d);
	#endif*/
					currentx = xcenter ;  /* ASG 080497 */ /*.ac JAN2498 */
					currenty = ycenter ;  /* ASG 080497 */ /*.ac JAN2498 */
					vector_add_point (currentx, currenty, 0, 0);
					break;
	
				case RTSL:
	/*#ifdef VG_DEBUG
					if ((firstwd & 0x1fff) != 0)
						logerror("(%d?)", firstwd & 0x1fff);
	#endif*/
					if (sp == 0)
					{
						logerror("\n*** Vector generator stack underflow! ***\n");
						done = 1;
						sp = MAXSTACK - 1;
					}
					else
						sp--;
	
					pc = stack [sp];
					break;
	
				case HALT:
	/*#ifdef VG_DEBUG
					if ((firstwd & 0x1fff) != 0)
						logerror("(%d?)", firstwd & 0x1fff);
	#endif*/
					done = 1;
					break;
	
				case JMPL:
					a = firstwd & 0x1fff;
	/*#ifdef VG_DEBUG
					logerror("%4x", map_addr(a));
	#endif*/
					/* if a = 0x0000, treat as HALT */
					if (a == 0x0000)
						done = 1;
					else
						pc = a;
					break;
	
				case JSRL:
					a = firstwd & 0x1fff;
	/*#ifdef VG_DEBUG
					logerror("%4x", map_addr(a));
	#endif*/
					/* if a = 0x0000, treat as HALT */
					if (a == 0x0000)
						done = 1;
					else
					{
						stack [sp] = pc;
						if (sp == (MAXSTACK - 1))
						{
							logerror("\n*** Vector generator stack overflow! ***\n");
							done = 1;
							sp = 0;
						}
						else
							sp++;
	
						pc = a;
					}
					break;
	
				default:
					logerror("internal error\n");
			}
	/*#ifdef VG_DEBUG
			logerror("\n");
	#endif*/
		}
	}
	
	
	public static int avgdvg_done ()
	{
		if (busy != 0)
			return 0;
		else
			return 1;
	}
	
	public static timer_callback avgdvg_clr_busy = new timer_callback() {
            public void handler(int i) {
                busy = 0;
            }
        };
		
	public static WriteHandlerPtr avgdvg_go_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (busy != 0)
			return;
	
		vector_updates++;
		total_length = 1;
		busy = 1;
	
		if (vectorEngine == USE_DVG)
		{
			dvg_generate_vector_list();
			timer_set (TIME_IN_NSEC(4500) * total_length, 1, avgdvg_clr_busy);
		}
		else
		{
			avg_generate_vector_list();
			if (total_length > 1)
				timer_set (TIME_IN_NSEC(1500) * total_length, 1, avgdvg_clr_busy);
			/* this is for Major Havoc */
			else
			{
				vector_updates--;
				busy = 0;
			}
		}
	} };
	
	//WRITE16_HANDLER( avgdvg_go_word_w )
        public static WriteHandlerPtr avgdvg_go_word_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                avgdvg_go_w.handler(offset, data);
            }
        };
	
	public static WriteHandlerPtr avgdvg_reset_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		avgdvg_clr_busy.handler(0);
	} };
	
	//WRITE16_HANDLER( avgdvg_reset_word_w )
        public static WriteHandlerPtr avgdvg_reset_word_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
	
		avgdvg_clr_busy.handler(0);
	}};
	
	public static int avgdvg_init (int vgType)
	{
		int i;
	
		if (vectorram_size[0] == 0)
		{
			logerror("Error: vectorram_size not initialized\n");
			return 1;
		}
	
		/* ASG 971210 -- initialize the pages */
		for (i = 0; i < NUM_BANKS; i++)
			vectorbank[i] = new UBytePtr(vectorram, (i<<BANK_BITS));
		if (vgType == USE_AVG_MHAVOC)
			vectorbank[1] = new UBytePtr(memory_region(REGION_CPU1), 0x18000);
	
		vectorEngine = vgType;
		if ((vectorEngine<AVGDVG_MIN) || (vectorEngine>AVGDVG_MAX))
		{
			logerror("Error: unknown Atari Vector Game Type\n");
			return 1;
		}
	
		if (vectorEngine==USE_AVG_SWARS)
			flipword=1;
	/*#ifndef LSB_FIRST
		else if (vectorEngine==USE_AVG_QUANTUM)
			flipword=1;
	#endif*/
		else
			flipword=0;
	
		vg_step = 0;
	
		busy = 0;
	
		xmin=Machine.visible_area.min_x;
		ymin=Machine.visible_area.min_y;
		xmax=Machine.visible_area.max_x;
		ymax=Machine.visible_area.max_y;
		width=xmax-xmin;
		height=ymax-ymin;
	
		xcenter=((xmax+xmin)/2) << VEC_SHIFT; /*.ac JAN2498 */
		ycenter=((ymax+ymin)/2) << VEC_SHIFT; /*.ac JAN2498 */
	
		vector_set_shift (VEC_SHIFT);
	
		if (vector_vh_start.handler() != 0)
			return 1;
	
		return 0;
	}
	
	/*
	 * These functions initialise the colors for all atari games.
	 */
	
	public static final int VEC_PAL_WHITE     = 1;
	public static final int VEC_PAL_AQUA      = 2;
	public static final int VEC_PAL_BZONE     = 3;
	public static final int VEC_PAL_MULTI     = 4;
	public static final int VEC_PAL_SWARS     = 5;
	public static final int VEC_PAL_ASTDELUX  = 6;
	
	/* Helper function to construct the color palette for the Atari vector
	 * games. DO NOT reference this function from the Gamedriver or
	 * MachineDriver. Use "avg_init_palette_XXXXX" instead. */
	public static void avg_init_palette (int paltype, char[] palette, char[] colortable, UBytePtr color_prom)
	{
		int i;
	
		/* initialize the colorram */
		for (i = 0; i < 16; i++){
			colorram[i] = i & 0x07;
                        //System.out.println("colorram["+i+"]="+colorram[i]);
                }
                colorram[1] = 7;
	
		/* fill the rest of the 256 color entries depending on the game */
		switch (paltype)
		{
			/* Black and White vector colors (Asteroids,Omega Race) .ac JAN2498 */
			case  VEC_PAL_WHITE:
				colorram[1] = 7; /* BW games use only color 1 (== white) */
				break;
	
			/* Monochrome Aqua colors (Asteroids Deluxe,Red Baron) .ac JAN2498 */
			case  VEC_PAL_ASTDELUX:
				/* Use backdrop if present MLR OCT0598 */
				/*TODO*///backdrop_load("astdelux.png", 256);
				colorram[1] =  3; /* for Asteroids */
				break;
	
			case  VEC_PAL_AQUA:
				colorram[0] =  3; /* for Red Baron */
				break;
	
			/* Monochrome Green/Red vector colors (Battlezone) .ac JAN2498 */
			case  VEC_PAL_BZONE:
				/* Use backdrop if present MLR OCT0598 */
				/*TODO*///backdrop_load("bzone.png", 256);
				break;
	
			default:
				logerror("Wrong palette type in avgdvg.c");
				break;
		}
	};
	
	public static VhConvertColorPromPtr avg_init_palette_white = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                avg_init_palette (VEC_PAL_WHITE, palette, colortable, color_prom);
            }
        };
	
        public static VhConvertColorPromPtr avg_init_palette_aqua = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                avg_init_palette (VEC_PAL_AQUA, palette, colortable, color_prom);
            } 
        };
        
        public static VhConvertColorPromPtr avg_init_palette_bzone = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                avg_init_palette (VEC_PAL_BZONE, palette, colortable, color_prom);
            } 
        };
        
        public static VhConvertColorPromPtr avg_init_palette_multi = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                avg_init_palette (VEC_PAL_MULTI, palette, colortable, color_prom);
            } 
        };
        
        public static VhConvertColorPromPtr avg_init_palette_swars = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                avg_init_palette (VEC_PAL_SWARS, palette, colortable, color_prom);
            } 
        };
        
        public static VhConvertColorPromPtr avg_init_palette_astdelux = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                avg_init_palette (VEC_PAL_ASTDELUX, palette, colortable, color_prom);
            } 
        };
	
	/* If you want to use the next two functions, please make sure that you have
	 * a fake GfxLayout, otherwise you'll crash */
	public static WriteHandlerPtr colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		colorram[offset & 0x0f] = data & 0x0f;
	} };
	
	/*
	 * Tempest, Major Havoc and Quantum select colors via a 16 byte colorram.
	 * What's more, they have a different ordering of the rgbi bits than the other
	 * color avg games.
	 * We need translation tables.
	 */
	
	public static WriteHandlerPtr tempest_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	//#if 0 /* with low intensity bit */
	//	static const int trans[]= { 7, 15, 3, 11, 6, 14, 2, 10, 5, 13, 1,  9, 4, 12, 0,  8 };
	//#else /* high intensity */
		int trans[]= { 7,  7, 3,  3, 6,  6, 2,  2, 5,  5, 1,  1, 4,  4, 0,  0 };
	//#endif
		colorram_w.handler(offset, trans[data & 0x0f]);
	} };
	
	public static WriteHandlerPtr mhavoc_colorram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	//#if 0 /* with low intensity bit */
	//	static const int trans[]= { 7, 6, 5, 4, 15, 14, 13, 12, 3, 2, 1, 0, 11, 10, 9, 8 };
	//#else /* high intensity */
		int trans[]= { 7, 6, 5, 4,  7,  6,  5,  4, 3, 2, 1, 0,  3,  2, 1, 0 };
	//#endif
		logerror("colorram: %02x: %02x\n", offset, data);
		colorram_w.handler(offset , trans[data & 0x0f]);
	} };
	
	
	//WRITE16_HANDLER( quantum_colorram_w )
        public static WriteHandlerPtr quantum_colorram_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                
            
            /* Notes on colors:
            offset:				color:			color (game):
            0 - score, some text		0 - black?
            1 - nothing?			1 - blue
            2 - nothing?			2 - green
            3 - Quantum, streaks		3 - cyan
            4 - text/part 1 player		4 - red
            5 - part 2 of player		5 - purple
            6 - nothing?			6 - yellow
            7 - part 3 of player		7 - white
            8 - stars			8 - black
            9 - nothing?			9 - blue
            10 - nothing?			10 - green
            11 - some text, 1up, like 3	11 - cyan
            12 - some text, like 4
            13 - nothing?			13 - purple
            14 - nothing?
            15 - nothing?

            1up should be blue
            score should be red
            high score - white? yellow?
            level # - green
            */

                    //if (ACCESSING_LSB != 0)
                    //{
                            int trans[]= { 7/*white*/, 0, 3, 1/*blue*/, 2/*green*/, 5, 6, 4/*red*/,
                                           7/*white*/, 0, 3, 1/*blue*/, 2/*green*/, 5, 6, 4/*red*/};

                            colorram_w.handler(offset, trans[data & 0x0f]);
                    //}
            }

        };
	
	/***************************************************************************
	
	  Draw the game screen in the given mame_bitmap.
	  Do NOT call osd_update_display() from this function, it will be called by
	  the main emulation engine.
	
	***************************************************************************/
	
	public static VhStartPtr dvg_start = new VhStartPtr() {
            public int handler() {
                return avgdvg_init (USE_DVG);
            }
        };
	
	int avg_start()
	{
		return avgdvg_init (USE_AVG);
	}
	
	public static VhStartPtr avg_start_starwars = new VhStartPtr() {
            public int handler() {
                return avgdvg_init (USE_AVG_SWARS);
            }
        };
	
	int avg_start_tempest()
	{
		return avgdvg_init (USE_AVG_TEMPEST);
	}
	
	int avg_start_mhavoc()
	{
		return avgdvg_init (USE_AVG_MHAVOC);
	}
	
	int avg_start_bzone()
	{
		return avgdvg_init (USE_AVG_BZONE);
	}
	
	int avg_start_quantum()
	{
		return avgdvg_init (USE_AVG_QUANTUM);
	}
	
	int avg_start_redbaron()
	{
		return avgdvg_init (USE_AVG_RBARON);
	}
	
	public static VhStopPtr avg_stop = new VhStopPtr() {
            public void handler() {
                busy = 0;
		vector_clear_list();
	
		vector_vh_stop.handler();
            }
        };
	
	public static VhStopPtr dvg_stop = new VhStopPtr() {
            public void handler() {
                busy = 0;
		vector_clear_list();
	
		vector_vh_stop.handler();
            }
        };
	
}
