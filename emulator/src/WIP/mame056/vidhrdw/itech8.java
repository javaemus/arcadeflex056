/***************************************************************************

  vidhrdw.c

  Functions to emulate the video hardware of the machine.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static WIP.mame056.vidhrdw.tms34061H.*;
import static WIP.mame056.vidhrdw.tms34061.*;
import static arcadeflex056.fucPtr.*;
import static mame056.machine._6812piaH.irqfuncPtr;

import static common.ptr.*;
import static common.subArrays.*;
import static common.libc.cstring.*;

import static mame056.common.*;
import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.inptport.*;
import static mame056.sound.mixer.*;
import static mame056.vidhrdw.generic.*;

import static mame056.timer.*;
import static mame056.timerH.*;

// refactor
import static arcadeflex036.osdepend.logerror;

import static WIP.mame056.drivers.itech8.itech8_update_interrupts;
import static WIP.mame056.machine.slikshot.*;

public class itech8
{
	/*************************************
	 *
	 *	Debugging
	 *
	 *************************************/
	
	public static int FULL_LOGGING			= 0;
	public static int BLIT_LOGGING			= 0;
	public static int INSTANT_BLIT			= 1;
	
        /*************************************
	 *
	 *	Global variables
	 *
	 *************************************/
	
	public static UBytePtr itech8_grom_bank = new UBytePtr();
	public static UBytePtr itech8_display_page = new UBytePtr();
	
	
	static int palette_addr;
	static int palette_index;
	static int[] palette_data=new int[3];
	
	static int[] blitter_data=new int[16];
	static int blit_in_progress;
	
	static int slikshot;
	
	static tms34061_display tms_state = new tms34061_display();
	static UBytePtr grom_base = new UBytePtr();
	static int grom_size;
	
	
	/*************************************
	 *
	 *	Blitter constants
	 *
	 *************************************/
	
	public static int BLITTER_ADDRHI(){
            return blitter_data[0];
        }
	public static int BLITTER_ADDRLO(){
            return blitter_data[1];
        }
	public static int BLITTER_FLAGS(){
            return blitter_data[2];
        }
	public static int BLITTER_STATUS(){
            return blitter_data[3];
        }
	public static int BLITTER_WIDTH(){
            return blitter_data[4];
        }
	public static int BLITTER_HEIGHT(){
            return blitter_data[5];
        }
	public static int BLITTER_MASK(){
            return blitter_data[6];
        }
	public static int BLITTER_OUTPUT(){
            return blitter_data[7];
        }
	public static int BLITTER_XSTART(){
            return blitter_data[8];
        }
	public static int BLITTER_YCOUNT(){
            return blitter_data[9];
        }
	public static int BLITTER_XSTOP(){
            return blitter_data[10];
        }
	public static int BLITTER_YSKIP(){
            return blitter_data[11];
        }
	
	public static int BLITFLAG_SHIFT			= 0x01;
	public static int BLITFLAG_XFLIP			= 0x02;
	public static int BLITFLAG_YFLIP			= 0x04;
	public static int BLITFLAG_RLE                          = 0x08;
	public static int BLITFLAG_TRANSPARENT                  = 0x10;
	
	
	
	
	
	/*************************************
	 *
	 *	TMS34061 interfacing
	 *
	 *************************************/
	
	static irqfuncPtr generate_interrupt = new irqfuncPtr() {
            public void handler(int state) {
                itech8_update_interrupts(-1, state, -1);
	
		if ((FULL_LOGGING!=0) && (state!=0)) logerror("------------ DISPLAY INT (%d) --------------n", cpu_getscanline());
            }
        };
	
	static tms34061_interface tms34061intf = new tms34061_interface
        (
		8,						/* VRAM address is (row << rowshift) | col */
		0x40000,				/* size of video RAM */
		0x100,					/* size of dirty chunks (must be power of 2) */
		generate_interrupt		/* interrupt gen callback */
        );
	
	
	
	/*************************************
	 *
	 *	Video start
	 *
	 *************************************/
	
	public static VhStartPtr itech8_vh_start = new VhStartPtr() { public int handler() 
	{
		/* initialize TMS34061 emulation */
	    if (tms34061_start(tms34061intf) != 0)
			return 1;
	
		/* get the TMS34061 display state */
		tms34061_get_display_state(tms_state);
	
		/* reset statics */
		palette_addr = 0;
		palette_index = 0;
		slikshot = 0;
	
		/* fetch the GROM base */
		grom_base = new UBytePtr(memory_region(REGION_GFX1));
		grom_size = memory_region_length(REGION_GFX1);
	
		return 0;
	} };
	
	public static VhStartPtr slikshot_vh_start = new VhStartPtr() { public int handler() 
	{
		int result = itech8_vh_start.handler();
		slikshot = 1;
		return result;
	} };
	
	
	
	/*************************************
	 *
	 *	Video stop
	 *
	 *************************************/
	
	public static VhStopPtr itech8_vh_stop = new VhStopPtr() { public void handler() 
	{
		tms34061_stop();
	} };
	
	
	
	/*************************************
	 *
	 *	Palette I/O
	 *
	 *************************************/
	
	public static WriteHandlerPtr itech8_palette_address_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* latch the address */
		palette_addr = data;
		palette_index = 0;
	} };
	
	
	public static WriteHandlerPtr itech8_palette_data_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* wait for 3 bytes to come in, then update the color */
		palette_data[palette_index++] = data;
		if (palette_index == 3)
		{
			palette_set_color(palette_addr++, palette_data[0] << 2, palette_data[1] << 2, palette_data[2] << 2);
			palette_index = 0;
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Low-level blitting primitives
	 *
	 *************************************/
        public static abstract interface draw_byte_Ptr {
            public abstract void handler(int addr, int val, int mask, int latch);
        }
        
        public static abstract interface blit_func_Ptr {
            public abstract void handler();
        }
	
	public static draw_byte_Ptr draw_byte = new draw_byte_Ptr() {
            public void handler(int addr, int val, int mask, int latch) {
                tms_state.vram.write(addr, val & mask);
		tms_state.latchram.write(addr, latch);
            }
        };
	
	public static draw_byte_Ptr draw_byte_trans4 = new draw_byte_Ptr() {
            public void handler(int addr, int val, int mask, int latch) {
                if (val == 0)
			return;
	
		if ((val & 0xf0) != 0)
		{
			if ((val & 0x0f) != 0)
			{
				tms_state.vram.write(addr, val & mask);
				tms_state.latchram.write(addr, latch);
			}
			else
			{
				tms_state.vram.write(addr, (tms_state.vram.read(addr) & 0x0f) | (val & mask & 0xf0));
				tms_state.latchram.write(addr, (tms_state.latchram.read(addr) & 0x0f) | (latch & 0xf0));
			}
		}
		else
		{
			tms_state.vram.write(addr, (tms_state.vram.read(addr) & 0xf0) | (val & mask & 0x0f));
			tms_state.latchram.write(addr, (tms_state.latchram.read(addr) & 0xf0) | (latch & 0x0f));
		}
	}};
	
	
	public static draw_byte_Ptr draw_byte_trans8 = new draw_byte_Ptr() {
            public void handler(int addr, int val, int mask, int latch) {
                if (val!=0) draw_byte.handler(addr, val, mask, latch);
	}};
	
	
	
	/*************************************
	 *
	 *	Low-level shifted blitting primitives
	 *
	 *************************************/
	
	public static draw_byte_Ptr draw_byte_shift = new draw_byte_Ptr() {
            public void handler(int addr, int val, int mask, int latch) {
                tms_state.vram.write(addr, (tms_state.vram.read(addr) & 0xf0) | ((val & mask) >> 4));
		tms_state.latchram.write(addr, (tms_state.latchram.read(addr) & 0xf0) | (latch >> 4));
		tms_state.vram.write(addr + 1, (tms_state.vram.read(addr + 1) & 0x0f) | ((val & mask) << 4));
		tms_state.latchram.write(addr + 1, (tms_state.latchram.read(addr + 1) & 0x0f) | (latch << 4));
	}};
	
	
	public static draw_byte_Ptr draw_byte_shift_trans4 = new draw_byte_Ptr() {
            public void handler(int addr, int val, int mask, int latch) {
                if (val == 0)
			return;
	
		if ((val & 0xf0) != 0)
		{
			tms_state.vram.write(addr, (tms_state.vram.read(addr) & 0xf0) | ((val & mask) >> 4));
			tms_state.latchram.write(addr, (tms_state.latchram.read(addr) & 0xf0) | (latch >> 4));
		}
		if ((val & 0x0f) != 0)
		{
			tms_state.vram.write(addr + 1, (tms_state.vram.read(addr + 1) & 0x0f) | ((val & mask) << 4));
			tms_state.latchram.write(addr + 1, (tms_state.latchram.read(addr + 1) & 0x0f) | (latch << 4));
		}
	}};
	
	
	public static draw_byte_Ptr draw_byte_shift_trans8 = new draw_byte_Ptr() {
            public void handler(int addr, int val, int mask, int latch) {
                if (val!=0) draw_byte_shift.handler(addr, val, mask, latch);
	}};
	
	
	
	/*************************************
	 *
	 *	Low-level flipped blitting primitives
	 *
	 *************************************/
	
	public static draw_byte_Ptr draw_byte_xflip = new draw_byte_Ptr() {
            public void handler(int addr, int val, int mask, int latch) {
                val = (val >> 4) | (val << 4);
		draw_byte.handler(addr, val, mask, latch);
	}};
	
	
	public static draw_byte_Ptr draw_byte_trans4_xflip = new draw_byte_Ptr() {
            public void handler(int addr, int val, int mask, int latch) {
                val = (val >> 4) | (val << 4);
		draw_byte_trans4.handler(addr, val, mask, latch);
	}};
	
	
	public static draw_byte_Ptr draw_byte_shift_xflip = new draw_byte_Ptr() {
            public void handler(int addr, int val, int mask, int latch) {
                val = (val >> 4) | (val << 4);
		draw_byte_shift.handler(addr, val, mask, latch);
	}};
	
	
	public static draw_byte_Ptr draw_byte_shift_trans4_xflip = new draw_byte_Ptr() {
            public void handler(int addr, int val, int mask, int latch) {
                val = (val >> 4) | (val << 4);
		draw_byte_shift_trans4.handler(addr, val, mask, latch);
	}};
	
	
	
	/*************************************
	 *
	 *	Uncompressed blitter macro
	 *
	 *************************************/
	public static void DRAW_RAW_MACRO(int TRANSPARENT, draw_byte_Ptr OPERATION)
        {			
            UBytePtr src = new UBytePtr(grom_base, ((itech8_grom_bank.read() << 16) | (BLITTER_ADDRHI() << 8) | BLITTER_ADDRLO()) % grom_size);
            int addr = tms_state.regs[TMS34061_XYADDRESS] | ((tms_state.regs[TMS34061_XYOFFSET] & 0x300) << 8);
            int ydir = (BLITTER_FLAGS() & BLITFLAG_YFLIP)!=0 ? -1 : 1;									
            int xdir = (BLITTER_FLAGS() & BLITFLAG_XFLIP)!=0 ? -1 : 1;									
            int color = tms34061_latch_r.handler(0);														
            int width = BLITTER_WIDTH();																
            int height = BLITTER_HEIGHT();															
            int mask = BLITTER_MASK();																
            int[] skip=new int[3];
            int x, y;																				

            /* compute horiz skip counts */															
            skip[0] = BLITTER_XSTART();																
            skip[1] = (width <= BLITTER_XSTOP()) ? 0 : width - 1 - BLITTER_XSTOP();
            if (xdir == -1) { int temp = skip[0]; skip[0] = skip[1]; skip[1] = temp; }				
            width -= skip[0] + skip[1];																

            /* compute vertical skip counts */														
            if (ydir == 1)																			
            {																						
                    skip[2] = (height <= BLITTER_YCOUNT()) ? 0 : height - BLITTER_YCOUNT();					
                    if (BLITTER_YSKIP() > 1) height -= BLITTER_YSKIP() - 1;									
            }																						
            else																					
            {																						
                    skip[2] = (height <= BLITTER_YSKIP()) ? 0 : height - BLITTER_YSKIP();
                    if (BLITTER_YCOUNT() > 1) height -= BLITTER_YCOUNT() - 1;								
            }																						

            /* skip top */																			
            for (y = 0; y < skip[2]; y++)															
            {																						
                    /* skip src and dest */																
                    addr += xdir * (width + skip[0] + skip[1]);
                    src.inc( width + skip[0] + skip[1] );

                    /* back up one and reverse directions */											
                    addr -= xdir;																		
                    addr += ydir * 256;																	
                    addr &= 0x3ffff;																	
                    xdir = -xdir;																		
            }																						

            /* loop over height */																	
            for (y = skip[2]; y < height; y++)														
            {																						
                    /* skip left */																		
                    addr += xdir * skip[y & 1];
                    src.inc(skip[y & 1]);

                    /* loop over width */																
                    for (x = 0; x < width; x++)															
                    {																					
                            OPERATION.handler(addr, src.readinc(), mask, color);											
                            addr += xdir;																	
                    }																					

                    /* skip right */																	
                    addr += xdir * skip[~y & 1];
                    src.inc(skip[~y & 1]);

                    /* back up one and reverse directions */											
                    addr -= xdir;																		
                    addr += ydir * 256;																	
                    addr &= 0x3ffff;																	
                    xdir = -xdir;																		
            }																						
        }
        
        public static blit_func_Ptr draw_raw = new blit_func_Ptr() {
            public void handler() {
                DRAW_RAW_MACRO(0, draw_byte);
        }};
        
        public static blit_func_Ptr draw_raw_shift = new blit_func_Ptr() {
            public void handler() {
                DRAW_RAW_MACRO(0, draw_byte_shift);
        }};
        
	public static blit_func_Ptr draw_raw_trans4 = new blit_func_Ptr() {
            public void handler() {
                DRAW_RAW_MACRO(1, draw_byte_trans4);
        }};
        
	public static blit_func_Ptr draw_raw_trans8 = new blit_func_Ptr() {
            public void handler() {
                DRAW_RAW_MACRO(1, draw_byte_trans8);
        }};
        
	public static blit_func_Ptr draw_raw_shift_trans4 = new blit_func_Ptr() {
            public void handler() {
                DRAW_RAW_MACRO(1, draw_byte_shift_trans4);
        }};
        
	public static blit_func_Ptr draw_raw_shift_trans8 = new blit_func_Ptr() {
            public void handler() {
                DRAW_RAW_MACRO(1, draw_byte_shift_trans8);
        }};
	
	
	
	/*************************************
	 *
	 *	Compressed blitter macro
	 *
	 *************************************/
	
	public static void DRAW_RLE_MACRO(int TRANSPARENT, draw_byte_Ptr OPERATION)
	{
                UBytePtr src = new UBytePtr(grom_base, ((itech8_grom_bank.read() << 16) | (BLITTER_ADDRHI() << 8) | BLITTER_ADDRLO()) % grom_size);
		int addr = tms_state.regs[TMS34061_XYADDRESS] | ((tms_state.regs[TMS34061_XYOFFSET] & 0x300) << 8);
		int ydir = (BLITTER_FLAGS() & BLITFLAG_YFLIP)!=0 ? -1 : 1;									
		int xdir = (BLITTER_FLAGS() & BLITFLAG_XFLIP)!=0 ? -1 : 1;									
		int count = 0, val = -1, innercount;													
		int color = tms34061_latch_r.handler(0);
		int width = BLITTER_WIDTH();																
		int height = BLITTER_HEIGHT();															
		int mask = BLITTER_MASK();																
		int[] skip=new int[3];																			
		int xleft, y;																			
																								
		/* skip past the double-0's */															
		src.inc(2);																				
																								
		/* compute horiz skip counts */															
		skip[0] = BLITTER_XSTART();																
		skip[1] = (width <= BLITTER_XSTOP()) ? 0 : width - 1 - BLITTER_XSTOP();
		if (xdir == -1) { int temp = skip[0]; skip[0] = skip[1]; skip[1] = temp; }				
		width -= skip[0] + skip[1];																
																								
		/* compute vertical skip counts */														
		if (ydir == 1)																			
		{																						
			skip[2] = (height <= BLITTER_YCOUNT()) ? 0 : height - BLITTER_YCOUNT();
			if (BLITTER_YSKIP() > 1) height -= BLITTER_YSKIP() - 1;									
		}																						
		else																					
		{																						
			skip[2] = (height <= BLITTER_YSKIP()) ? 0 : height - BLITTER_YSKIP();
			if (BLITTER_YCOUNT() > 1) height -= BLITTER_YCOUNT() - 1;
		}																						
																								
		/* skip top */																			
		for (y = 0; y < skip[2]; y++)															
		{																						
			/* skip dest */																		
			addr += xdir * (width + skip[0] + skip[1]);											
																								
			/* scan RLE until done */															
			for (xleft = width + skip[0] + skip[1]; xleft > 0; )								
			{																					
				/* load next RLE chunk if needed */												
				if (count == 0)																		
				{																				
					count = src.readinc();
					val = (count & 0x80)!=0 ? -1 : src.readinc();
					count &= 0x7f;
				}																				
																								
				/* determine how much to bite off */											
				innercount = (xleft > count) ? count : xleft;									
				count -= innercount;															
				xleft -= innercount;															
																								
				/* skip past the data */														
				if (val == -1) src.inc(innercount);
			}																					
																								
			/* back up one and reverse directions */											
			addr -= xdir;																		
			addr += ydir * 256;																	
			addr &= 0x3ffff;																	
			xdir = -xdir;																		
		}																						
																								
		/* loop over height */																	
		for (y = skip[2]; y < height; y++)														
		{																						
			/* skip left */																		
			addr += xdir * skip[y & 1];															
			for (xleft = skip[y & 1]; xleft > 0; )												
			{																					
				/* load next RLE chunk if needed */												
				if (count == 0)																		
				{																				
					count = src.readinc();
					val = (count & 0x80)!=0 ? -1 : src.readinc();
					count &= 0x7f;
				}
																								
				/* determine how much to bite off */											
				innercount = (xleft > count) ? count : xleft;									
				count -= innercount;															
				xleft -= innercount;															
																								
				/* skip past the data */														
				if (val == -1) src.inc(innercount);
			}																					
																								
			/* loop over width */																
			for (xleft = width; xleft > 0; )													
			{																					
				/* load next RLE chunk if needed */												
				if (count == 0)																		
				{																				
					count = src.readinc();
					val = (count & 0x80)!=0 ? -1 : src.readinc();
					count &= 0x7f;																
				}																				
																								
				/* determine how much to bite off */											
				innercount = (xleft > count) ? count : xleft;									
				count -= innercount;															
				xleft -= innercount;															
																								
				/* run of literals */															
				if (val == -1)																	
					for ( ; (innercount--)>0; addr += xdir)											
						OPERATION.handler(addr, src.readinc(), mask, color);									
																								
				/* run of non-transparent repeats */											
				else if (TRANSPARENT==0 || val!=0)
					for ( ; (innercount--)>0; addr += xdir)											
						OPERATION.handler(addr, val, mask, color);										
																								
				/* run of transparent repeats */												
				else																			
					addr += xdir * innercount;													
			}																					
																								
			/* skip right */																	
			addr += xdir * skip[~y & 1];														
			for (xleft = skip[~y & 1]; xleft > 0; )												
			{																					
				/* load next RLE chunk if needed */												
				if (count == 0)																		
				{																				
					count = src.readinc();
					val = (count & 0x80)!=0 ? -1 : src.readinc();
					count &= 0x7f;																
				}																				
																								
				/* determine how much to bite off */											
				innercount = (xleft > count) ? count : xleft;									
				count -= innercount;															
				xleft -= innercount;															
																								
				/* skip past the data */														
				if (val == -1) src.inc(innercount);
			}																					
																								
			/* back up one and reverse directions */											
			addr -= xdir;																		
			addr += ydir * 256;																	
			addr &= 0x3ffff;																	
			xdir = -xdir;																		
		}																						
	}
	
	
	
	/*************************************
	 *
	 *	Blitter functions and tables
	 *
	 *************************************/
	
	public static blit_func_Ptr draw_rle = new blit_func_Ptr() {
            public void handler() {
                DRAW_RLE_MACRO(0, draw_byte);
            }
        };
        
	public static blit_func_Ptr draw_rle_shift = new blit_func_Ptr() {
            public void handler() {
                DRAW_RLE_MACRO(0, draw_byte_shift);
        }};
        
	public static blit_func_Ptr draw_rle_trans4 = new blit_func_Ptr() {
            public void handler() {
                DRAW_RLE_MACRO(1, draw_byte_trans4);
        }};
        
	public static blit_func_Ptr draw_rle_trans8 = new blit_func_Ptr() {
            public void handler() {
                DRAW_RLE_MACRO(1, draw_byte_trans8);
        }};
        
	public static blit_func_Ptr draw_rle_shift_trans4 = new blit_func_Ptr() {
            public void handler() {
                DRAW_RLE_MACRO(1, draw_byte_shift_trans4);
        }};
        
	public static blit_func_Ptr draw_rle_shift_trans8 = new blit_func_Ptr() {
            public void handler() {
                DRAW_RLE_MACRO(1, draw_byte_shift_trans8);
        }};
	
	public static blit_func_Ptr draw_raw_xflip = new blit_func_Ptr() {
            public void handler() {
                DRAW_RAW_MACRO(0, draw_byte_xflip);
        }};
        
	public static blit_func_Ptr draw_raw_shift_xflip = new blit_func_Ptr() {
            public void handler() {
                DRAW_RAW_MACRO(0, draw_byte_shift_xflip);
        }};
        
	public static blit_func_Ptr draw_raw_trans4_xflip = new blit_func_Ptr() {
            public void handler() {
                DRAW_RAW_MACRO(1, draw_byte_trans4_xflip);
        }};
        
	public static blit_func_Ptr draw_raw_shift_trans4_xflip = new blit_func_Ptr() {
            public void handler() {
                DRAW_RAW_MACRO(1, draw_byte_shift_trans4_xflip);
        }};
	
	public static blit_func_Ptr draw_rle_xflip = new blit_func_Ptr() {
            public void handler() {
                DRAW_RLE_MACRO(0, draw_byte_xflip);
        }};
        
	public static blit_func_Ptr draw_rle_shift_xflip = new blit_func_Ptr() {
            public void handler() {
                DRAW_RLE_MACRO(0, draw_byte_shift_xflip);
        }};
        
	public static blit_func_Ptr draw_rle_trans4_xflip = new blit_func_Ptr() {
            public void handler() {
                DRAW_RLE_MACRO(1, draw_byte_trans4_xflip);
        }};
        
	public static blit_func_Ptr draw_rle_shift_trans4_xflip = new blit_func_Ptr() {
            public void handler() {
                DRAW_RLE_MACRO(1, draw_byte_shift_trans4_xflip);
        }};
	
	
	static blit_func_Ptr blit_table4[] =
	{
		draw_raw,			draw_raw_shift,			draw_raw,			draw_raw_shift,
		draw_raw,			draw_raw_shift,			draw_raw,			draw_raw_shift,
		draw_rle,			draw_rle_shift,			draw_rle,			draw_rle_shift,
		draw_rle,			draw_rle_shift,			draw_rle,			draw_rle_shift,
		draw_raw_trans4,	draw_raw_shift_trans4,	draw_raw_trans4,	draw_raw_shift_trans4,
		draw_raw_trans4,	draw_raw_shift_trans4,	draw_raw_trans4,	draw_raw_shift_trans4,
		draw_rle_trans4,	draw_rle_shift_trans4,	draw_rle_trans4,	draw_rle_shift_trans4,
		draw_rle_trans4,	draw_rle_shift_trans4,	draw_rle_trans4,	draw_rle_shift_trans4
	};
	
	static blit_func_Ptr blit_table4_xflip[] =
	{
		draw_raw_xflip,			draw_raw_shift_xflip,			draw_raw_xflip,			draw_raw_shift_xflip,
		draw_raw_xflip,			draw_raw_shift_xflip,			draw_raw_xflip,			draw_raw_shift_xflip,
		draw_rle_xflip,			draw_rle_shift_xflip,			draw_rle_xflip,			draw_rle_shift_xflip,
		draw_rle_xflip,			draw_rle_shift_xflip,			draw_rle_xflip,			draw_rle_shift_xflip,
		draw_raw_trans4_xflip,	draw_raw_shift_trans4_xflip,	draw_raw_trans4_xflip,	draw_raw_shift_trans4_xflip,
		draw_raw_trans4_xflip,	draw_raw_shift_trans4_xflip,	draw_raw_trans4_xflip,	draw_raw_shift_trans4_xflip,
		draw_rle_trans4_xflip,	draw_rle_shift_trans4_xflip,	draw_rle_trans4_xflip,	draw_rle_shift_trans4_xflip,
		draw_rle_trans4_xflip,	draw_rle_shift_trans4_xflip,	draw_rle_trans4_xflip,	draw_rle_shift_trans4_xflip
	};
	
	static blit_func_Ptr blit_table8[] =
	{
		draw_raw,			draw_raw_shift,			draw_raw,			draw_raw_shift,
		draw_raw,			draw_raw_shift,			draw_raw,			draw_raw_shift,
		draw_rle,			draw_rle_shift,			draw_rle,			draw_rle_shift,
		draw_rle,			draw_rle_shift,			draw_rle,			draw_rle_shift,
		draw_raw_trans8,	draw_raw_shift_trans8,	draw_raw_trans8,	draw_raw_shift_trans8,
		draw_raw_trans8,	draw_raw_shift_trans8,	draw_raw_trans8,	draw_raw_shift_trans8,
		draw_rle_trans8,	draw_rle_shift_trans8,	draw_rle_trans8,	draw_rle_shift_trans8,
		draw_rle_trans8,	draw_rle_shift_trans8,	draw_rle_trans8,	draw_rle_shift_trans8
	};
	
	
	
	/*************************************
	 *
	 *	Blitter operations
	 *
	 *************************************/
	
	static int perform_blit()
	{
		/* debugging */
		if (FULL_LOGGING != 0)
			logerror("Blit: scan=%d  src=%06x @ (%05x) for %dx%d ... flags=%02xn",
					cpu_getscanline(),
					(itech8_grom_bank.read() << 16) | (BLITTER_ADDRHI() << 8) | BLITTER_ADDRLO(),
					0, BLITTER_WIDTH(), BLITTER_HEIGHT(), BLITTER_FLAGS());
	
		/* draw appropriately */
		if ((BLITTER_OUTPUT() & 0x40) != 0)
		{
			if ((BLITTER_FLAGS() & BLITFLAG_XFLIP) != 0)
				(blit_table4_xflip[BLITTER_FLAGS() & 0x1f]).handler();
			else
				(blit_table4[BLITTER_FLAGS() & 0x1f]).handler();
		}
		else
			(blit_table8[BLITTER_FLAGS() & 0x1f]).handler();
	
		/* return the number of bytes processed */
		return BLITTER_WIDTH() * BLITTER_HEIGHT();
	}
	
	
	static timer_callback blitter_done = new timer_callback() {
            public void handler(int i) {
                /* turn off blitting and generate an interrupt */
		blit_in_progress = 0;
		itech8_update_interrupts(-1, -1, 1);
	
		if (FULL_LOGGING != 0) logerror("------------ BLIT DONE (%d) --------------n", cpu_getscanline());
            }
        };
	
	
	/*************************************
	 *
	 *	Blitter I/O
	 *
	 *************************************/
	
	public static ReadHandlerPtr itech8_blitter_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int result = blitter_data[offset / 2];
	
		/* debugging */
		/*TODO*///if (FULL_LOGGING != 0) logerror("%04x:blitter_r(%02x)n", cpu_getpreviouspc(), offset / 2);
	
		/* low bit seems to be ignored */
		offset /= 2;
	
		/* a read from offset 3 clears the interrupt and returns the status */
		if (offset == 3)
		{
			itech8_update_interrupts(-1, -1, 0);
			if (blit_in_progress != 0)
				result |= 0x80;
			else
				result &= 0x7f;
		}
	
		return result;
	} };
	
	
	public static WriteHandlerPtr itech8_blitter_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* low bit seems to be ignored */
		offset /= 2;
		blitter_data[offset] = data;
	
		/* a write to offset 3 starts things going */
		if (offset == 3)
		{
			int pixels;
	
			/* log to the blitter file */
			/*TODO*///if (BLIT_LOGGING != 0)
			/*TODO*///{
			/*TODO*///	static FILE blitlog;
			/*TODO*///	if (blitlog == 0) blitlog = fopen("blitter.log", "w");
			/*TODO*///	if (blitlog) fprintf(blitlog, "Blit: XY=%1X%02X%02X SRC=%02X%02X%02X SIZE=%3dx%3d FLAGS=%02x",
			/*TODO*///				tms34061_r(14*4+2, 0, 0) & 0x0f, tms34061_r(15*4+2, 0, 0), tms34061_r(15*4+0, 0, 0),
			/*TODO*///				*itech8_grom_bank, blitter_data[0], blitter_data[1],
			/*TODO*///				blitter_data[4], blitter_data[5],
			/*TODO*///				blitter_data[2]);
			/*TODO*///	if (blitlog) fprintf(blitlog, "   %02X %02X %02X [%02X] %02X %02X %02X [%02X]-%02X %02X %02X %02X [%02X %02X %02X %02X]n",
			/*TODO*///				blitter_data[0], blitter_data[1],
			/*TODO*///				blitter_data[2], blitter_data[3],
			/*TODO*///				blitter_data[4], blitter_data[5],
			/*TODO*///				blitter_data[6], blitter_data[7],
			/*TODO*///				blitter_data[8], blitter_data[9],
			/*TODO*///				blitter_data[10], blitter_data[11],
			/*TODO*///				blitter_data[12], blitter_data[13],
			/*TODO*///				blitter_data[14], blitter_data[15]);
			/*TODO*///}
	
			/* perform the blit */
			pixels = perform_blit();
			blit_in_progress = 1;
	
			/* set a timer to go off when we're done */
			if (INSTANT_BLIT != 0)
				blitter_done.handler(0);
			else
				timer_set((double)pixels * TIME_IN_HZ(12000000), 0, blitter_done);
		}
	
		/* debugging */
		/*TODO*///if (FULL_LOGGING) logerror("%04x:blitter_w(%02x)=%02xn", cpu_getpreviouspc(), offset, data);
	} };
	
	
	
	/*************************************
	 *
	 *	TMS34061 I/O
	 *
	 *************************************/
	
	public static WriteHandlerPtr itech8_tms34061_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int func = (offset >> 9) & 7;
		int col = offset & 0xff;
	
		/* Column address (CA0-CA8) is hooked up the A0-A7, with A1 being inverted
		   during register access. CA8 is ignored */
		if (func == 0 || func == 2)
			col ^= 2;
	
		/* Row address (RA0-RA8) is not dependent on the offset */
		tms34061_w(col, 0xff, func, data);
	} };
	
	
	public static ReadHandlerPtr itech8_tms34061_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int func = (offset >> 9) & 7;
		int col = offset & 0xff;
	
		/* Column address (CA0-CA8) is hooked up the A0-A7, with A1 being inverted
		   during register access. CA8 is ignored */
		if (func == 0 || func == 2)
			col ^= 2;
	
		/* Row address (RA0-RA8) is not dependent on the offset */
		return tms34061_r(col, 0xff, func);
	} };
	
	
	
	/*************************************
	 *
	 *	Main refresh
	 *
	 *************************************/
	
	public static VhUpdatePtr itech8_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		int y, ty;
                
                //tms_state = new tms34061_display();
	
		/* first get the current display state */
		tms34061_get_display_state(tms_state);
	
		/* if we're blanked, just fill with black */
		if (tms_state.blanked != 0)
		{
			fillbitmap(bitmap, Machine.pens[0], Machine.visible_area);
			return;
		}
	
		/* perform one of two types of blitting; I'm not sure if bit 40 in */
		/* the blitter mode register really controls this type of behavior, but */
		/* it is set consistently enough that we can use it */
	
		/* blit mode one: 4bpp in the TMS34061 RAM, plus 4bpp of latched data */
		/* two pages are available, at 0x00000 and 0x20000 */
		/* pages are selected via the display page register */
		/* width can be up to 512 pixels */
		if ((BLITTER_OUTPUT() & 0x40) != 0)
		{
			int halfwidth = (Machine.visible_area.max_x + 2) / 2;
			UBytePtr base = new UBytePtr(tms_state.vram, (~itech8_display_page.read() & 0x80) << 10);
			UBytePtr latch = new UBytePtr(tms_state.latchram, (~itech8_display_page.read() & 0x80) << 10);
	
			/* now regenerate the bitmap */
			for (ty = 0, y = Machine.visible_area.min_y; y <= Machine.visible_area.max_y; y++, ty++)
			{
				UBytePtr scanline=new UBytePtr(512);
				int x;
	
				for (x = 0; x < halfwidth; x++)
				{
					scanline.write(x * 2 + 0, (latch.read(256 * ty + x) & 0xf0) | (base.read(256 * ty + x) >> 4));
					scanline.write(x * 2 + 1, (latch.read(256 * ty + x) << 4) | (base.read(256 * ty + x) & 0x0f));
				}
                                scanline.offset=0;
				draw_scanline8(bitmap, 0, y, 2 * halfwidth, scanline, new IntArray(Machine.pens), -1);
			}
		}
	
		/* blit mode one: 8bpp in the TMS34061 RAM */
		/* two planes are available, at 0x00000 and 0x20000 */
		/* both planes are rendered; with 0x20000 transparent via color 0 */
		/* width can be up to 256 pixels */
		else
		{
			UBytePtr base = new UBytePtr(tms_state.vram, tms_state.dispstart & ~0x30000);
	
			/* now regenerate the bitmap */
			for (ty = 0, y = Machine.visible_area.min_y; y <= Machine.visible_area.max_y; y++, ty++)
			{
				draw_scanline8(bitmap, 0, y, 256, new UBytePtr(base, 0x20000 + 256 * ty), new IntArray(Machine.pens), -1);
				draw_scanline8(bitmap, 0, y, 256, new UBytePtr(base, 0x00000 + 256 * ty), new IntArray(Machine.pens), 0);
			}
		}
	
		/* extra rendering for slikshot */
		if (slikshot != 0)
			slikshot_extra_draw(bitmap);
	} };
}
