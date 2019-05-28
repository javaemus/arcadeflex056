/***************************************************************************

	Cinemat/Leland driver

	Leland video hardware
	driver by Aaron Giles and Paul Leaman

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

import static arcadeflex056.fucPtr.*;
import static common.libc.cstring.memset;

import static common.ptr.*;
import common.subArrays.IntArray;
import static mame056.common.*;

import static mame056.palette.*;
import static mame056.drawgfx.*;
import static mame056.mame.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

import static mame056.vidhrdw.generic.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpuexec.*;
import static mame056.cpuintrfH.*;
import static mame056.timerH.*;
import static mame056.timer.*;

import static WIP.mame056.drivers.leland.*;
import static WIP.mame056.sndhrdw.leland.*;

public class leland
{
	
	
	
	/* constants */
	public static int VRAM_SIZE = 0x10000;
	public static int QRAM_SIZE = 0x10000;
	
	public static int VIDEO_WIDTH = 0x28;
	public static int VIDEO_HEIGHT = 0x1e;
	
	
	/* debugging */
	/*TODO*///#define LOG_COMM	0
	
	
	
	public static class vram_state_data
	{
		public int	addr;
		public int[]	latch=new int[2];
	};
	
	public static class scroll_position
	{
		public int 	scanline;
		public int 	x, y;
		public int 	gfxbank;
	};
	
	
	/* video RAM */
	static mame_bitmap fgbitmap;
	static UBytePtr leland_video_ram = new UBytePtr();
	public static UBytePtr ataxx_qram = new UBytePtr();
	public static int leland_last_scanline_int;
	
	/* video RAM bitmap drawing */
	static vram_state_data[] vram_state = new vram_state_data[2];
        static {
            for (int i=0 ; i<2 ; i++)
                vram_state[i] = new vram_state_data();
        }
	static int sync_next_write;
	
	/* partial screen updating */
	static int next_update_scanline;
	
	/* scroll background registers */
	static int xscroll;
	static int yscroll;
	static int gfxbank;
	static int scroll_index;
	static scroll_position[] scroll_pos = new scroll_position[VIDEO_HEIGHT];
	
	
	/*************************************
	 *
	 *	Start video hardware
	 *
	 *************************************/
	
	public static VhStartPtr leland_vh_start = new VhStartPtr() { public int handler() 
	{
		
		/* allocate memory */
	    leland_video_ram = new UBytePtr(VRAM_SIZE*2);
	    fgbitmap = bitmap_alloc(VIDEO_WIDTH * 8, VIDEO_HEIGHT * 8);
	
		/* error cases */
	    if ((leland_video_ram==null) || (fgbitmap == null))
	    {
	    	leland_vh_stop.handler();
			return 1;
		}
	
		/* reset videoram */
	    memset(leland_video_ram, 0, VRAM_SIZE*2);
	
		/* reset scrolling */
		scroll_index = 0;
		//memset(scroll_pos, 0, sizeof(scroll_pos));
                for (int i=0 ; i<VIDEO_HEIGHT ; i++)
                    scroll_pos[i] = new scroll_position();
	
		return 0;
	} };
	
	
	public static VhStartPtr ataxx_vh_start = new VhStartPtr() { public int handler() 
	{
		
		/* first do the standard stuff */
		if (leland_vh_start.handler() != 0)
			return 1;
	
		/* allocate memory */
		ataxx_qram = new UBytePtr(QRAM_SIZE);
	
		/* error cases */
	    if (ataxx_qram == null)
	    {
	    	ataxx_vh_stop.handler();
			return 1;
		}
	
		/* reset QRAM */
		memset(ataxx_qram, 0, QRAM_SIZE);
		return 0;
	} };
	
	
	
	/*************************************
	 *
	 *	Stop video hardware
	 *
	 *************************************/
	
	public static VhStopPtr leland_vh_stop = new VhStopPtr() { public void handler() 
	{
		if (leland_video_ram != null)
			leland_video_ram = null;
	
		if (fgbitmap != null)
			fgbitmap = null;
	} };
	
	
	public static VhStopPtr ataxx_vh_stop = new VhStopPtr() { public void handler() 
	{
		leland_vh_stop.handler();
	
		if (ataxx_qram != null)
			ataxx_qram = null;
	} };
	
	
	
	/*************************************
	 *
	 *	Scrolling and banking
	 *
	 *************************************/
	
	public static WriteHandlerPtr leland_gfx_port_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int scanline = leland_last_scanline_int;
		scroll_position scroll;
	
		/* treat anything during the VBLANK as scanline 0 */
		if (scanline > Machine.visible_area.max_y)
			scanline = 0;
	
		/* adjust the proper scroll value */
	    switch (offset)
	    {
	    	case -1:
	    		gfxbank = data;
	    		break;
			case 0:
				xscroll = (xscroll & 0xff00) | (data & 0x00ff);
				break;
			case 1:
				xscroll = (xscroll & 0x00ff) | ((data << 8) & 0xff00);
				break;
			case 2:
				yscroll = (yscroll & 0xff00) | (data & 0x00ff);
				break;
			case 3:
				yscroll = (yscroll & 0x00ff) | ((data << 8) & 0xff00);
				break;
		}
	
		/* update if necessary */
		scroll = scroll_pos[scroll_index];
		if (xscroll != scroll.x || yscroll != scroll.y || gfxbank != scroll.gfxbank)
		{
			/* determine which entry to use */
			if (scroll.scanline != scanline && scroll_index < VIDEO_HEIGHT - 1){
				//scroll++, scroll_index++;
                                scroll = scroll_pos[scroll_index++];
                        }
	
			/* fill in the data */
			scroll.scanline = scanline;
			scroll.x = xscroll;
			scroll.y = yscroll;
			scroll.gfxbank = gfxbank;
		}
	} };
	
	
	
	/*************************************
	 *
	 *	Video address setting
	 *
	 *************************************/
	static int _vram_state = 0;
        
	public static void leland_video_addr_w(int offset, int data, int num)
	{
            
		vram_state_data state = vram_state[_vram_state + num];
	
		if (offset == 0)
			state.addr = (state.addr & 0xfe00) | ((data << 1) & 0x01fe);
		else
			state.addr = ((data << 9) & 0xfe00) | (state.addr & 0x01fe);
	
		if (num == 0)
			sync_next_write = (state.addr >= 0xf000)?1:0;
	}
	
	
	
	/*************************************
	 *
	 *	Flush data from VRAM into our copy
	 *
	 *************************************/
	
	static void update_for_scanline(int scanline)
	{
		int i, j;
	
		/* skip if we're behind the times */
		if (scanline <= next_update_scanline)
			return;
	
		/* update all scanlines */
		for (i = next_update_scanline; i < scanline; i++)
			if (i < VIDEO_HEIGHT * 8)
			{
				UBytePtr scandata=new UBytePtr(VIDEO_WIDTH * 8);
				UBytePtr dst = new UBytePtr(scandata);
				UBytePtr src = new UBytePtr(leland_video_ram, i * 256);
                                int _src = 0;
                                int _dst = 0;
	
				for (j = 0; j < VIDEO_WIDTH * 8 / 2; j++)
				{
					int pix = _src++;
					dst.write(_dst++, pix >> 4);
					dst.write(_dst++, pix & 15);
				}
				draw_scanline8(fgbitmap, 0, i, VIDEO_WIDTH * 8, scandata, null, -1);
			}
	
		/* also update the DACs */
		if (scanline >= VIDEO_HEIGHT * 8)
			scanline = 256;
		for (i = next_update_scanline; i < scanline; i++)
		{
			if ((leland_dac_control & 0x01) == 0){
				leland_dac_update(0, leland_video_ram.read(i * 256 + 160));
                        }
			if ((leland_dac_control & 0x02) == 0){
				leland_dac_update(1, leland_video_ram.read(i * 256 + 161));
                        }
		}
	
		/* set the new last update */
		next_update_scanline = scanline;
	}
	
	
	
	/*************************************
	 *
	 *	Common video RAM read
	 *
	 *************************************/
	
	public static int leland_vram_port_r(int offset, int num)
	{
		//struct vram_state_data *state = vram_state + num;
                vram_state_data state = vram_state[_vram_state + num];
		int addr = state.addr;
		int inc = (offset >> 2) & 2;
	    int ret;
	
	    switch (offset & 7)
	    {
	        case 3:	/* read hi/lo (alternating) */
	        	ret = leland_video_ram.read(addr);
	        	addr += inc & (addr << 1);
	        	addr ^= 1;
	            break;
	
	        case 5:	/* read hi */
			    ret = leland_video_ram.read(addr | 1);
			    addr += inc;
	            break;
	
	        case 6:	/* read lo */
			    ret = leland_video_ram.read(addr & ~1);
			    addr += inc;
	            break;
	
	        default:
	            logerror("CPU #%d %04x Warning: Unknown video port %02x read (address=%04x)\n",
	                        cpu_getactivecpu(),cpu_get_pc(), offset, addr);
	            ret = 0;
	            break;
	    }
	    state.addr = addr;
	
		/*TODO*///if (LOG_COMM && addr >= 0xf000)
		/*TODO*///	logerror("%04X:%s comm read %04X = %02X\n", cpu_getpreviouspc(), num ? "slave" : "master", addr, ret);
	
	    return ret;
	}
	
	
	
	/*************************************
	 *
	 *	Common video RAM write
	 *
	 *************************************/
	
	public static void leland_vram_port_w(int offset, int data, int num)
	{
		vram_state_data state = vram_state[_vram_state + num];
		int addr = state.addr;
		int inc = (offset >> 2) & 2;
		int trans = (offset >> 4) & num;
	
		/* if we're writing "behind the beam", make sure we've cached what was there */
		if (addr < 0xf000)
		{
			int cur_scanline = cpu_getscanline();
			int mod_scanline = addr / 256;
	
			if (cur_scanline != next_update_scanline && mod_scanline < cur_scanline)
				update_for_scanline(cur_scanline);
		}
	
		/*TODO*///if (LOG_COMM && addr >= 0xf000)
		/*TODO*///	logerror("%04X:%s comm write %04X = %02X\n", cpu_getpreviouspc(), num ? "slave" : "master", addr, data);
	
		/* based on the low 3 bits of the offset, update the destination */
	    switch (offset & 7)
	    {
	        case 1:	/* write hi = data, lo = latch */
	        	leland_video_ram.write(addr & ~1, state.latch[0]);
	        	leland_video_ram.write(addr |  1, data);
	        	addr += inc;
	        	break;
	
	        case 2:	/* write hi = latch, lo = data */
	        	leland_video_ram.write(addr & ~1, data);
	        	leland_video_ram.write(addr |  1, state.latch[1]);
	        	addr += inc;
	        	break;
	
	        case 3:	/* write hi/lo = data (alternating) */
	        	if (trans != 0)
	        	{
	        		if ((data & 0xf0)==0) data |= leland_video_ram.read(addr) & 0xf0;
	        		if ((data & 0x0f)==0) data |= leland_video_ram.read(addr) & 0x0f;
	        	}
	       		leland_video_ram.write(addr, data);
	        	addr += inc & (addr << 1);
	        	addr ^= 1;
	            break;
	
	        case 5:	/* write hi = data */
	        	state.latch[1] = data;
	        	if (trans != 0)
	        	{
	        		if ((data & 0xf0)==0) data |= leland_video_ram.read(addr | 1) & 0xf0;
	        		if ((data & 0x0f)==0) data |= leland_video_ram.read(addr | 1) & 0x0f;
	        	}
			    leland_video_ram.write(addr | 1, data);
			    addr += inc;
	            break;
	
	        case 6:	/* write lo = data */
	        	state.latch[0] = data;
	        	if (trans != 0)
	        	{
	        		if ((data & 0xf0)==0) data |= leland_video_ram.read(addr & ~1) & 0xf0;
	        		if ((data & 0x0f)==0) data |= leland_video_ram.read(addr & ~1) & 0x0f;
	        	}
			    leland_video_ram.write(addr & ~1, data);
			    addr += inc;
	            break;
	
	        default:
	            logerror("CPU #%d %04x Warning: Unknown video port %02x write (address=%04x value=%02x)\n",
	                        cpu_getactivecpu(),cpu_get_pc(), offset, addr);
	            break;
	    }
	
	    /* update the address and plane */
	    state.addr = addr;
	}
	
	
	
	/*************************************
	 *
	 *	Master video RAM read/write
	 *
	 *************************************/
	
	public static WriteHandlerPtr leland_master_video_addr_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    leland_video_addr_w(offset, data, 0);
	} };
	
	
	public static timer_callback leland_delayed_mvram_w = new timer_callback() {
            public void handler(int param) {
                int num = (param >> 16) & 1;
		int offset = (param >> 8) & 0xff;
		int data = param & 0xff;
		leland_vram_port_w(offset, data, num);
            }
        };
	
	
	public static WriteHandlerPtr leland_mvram_port_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if (sync_next_write != 0)
		{
			timer_set(TIME_NOW, 0x00000 | (offset << 8) | data, leland_delayed_mvram_w);
			sync_next_write = 0;
		}
		else
		    leland_vram_port_w(offset, data, 0);
	} };
	
	
	public static ReadHandlerPtr leland_mvram_port_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	    return leland_vram_port_r(offset, 0);
	} };
	
	
	
	/*************************************
	 *
	 *	Slave video RAM read/write
	 *
	 *************************************/
	
	public static WriteHandlerPtr leland_slave_video_addr_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    leland_video_addr_w(offset, data, 1);
	} };
	
	public static WriteHandlerPtr leland_svram_port_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	    leland_vram_port_w(offset, data, 1);
	} };
	
	public static ReadHandlerPtr leland_svram_port_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	    return leland_vram_port_r(offset, 1);
	} };
	
	
	
	/*************************************
	 *
	 *	Ataxx master video RAM read/write
	 *
	 *************************************/
	
	public static WriteHandlerPtr ataxx_mvram_port_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		offset = ((offset >> 1) & 0x07) | ((offset << 3) & 0x08) | (offset & 0x10);
		if (sync_next_write != 0)
		{
			timer_set(TIME_NOW, 0x00000 | (offset << 8) | data, leland_delayed_mvram_w);
			sync_next_write = 0;
		}
		else
			leland_vram_port_w(offset, data, 0);
	} };
	
	
	public static WriteHandlerPtr ataxx_svram_port_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		offset = ((offset >> 1) & 0x07) | ((offset << 3) & 0x08) | (offset & 0x10);
		leland_vram_port_w(offset, data, 1);
	} };
	
	
	
	/*************************************
	 *
	 *	Ataxx slave video RAM read/write
	 *
	 *************************************/
	
	public static ReadHandlerPtr ataxx_mvram_port_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		offset = ((offset >> 1) & 0x07) | ((offset << 3) & 0x08) | (offset & 0x10);
	    return leland_vram_port_r(offset, 0);
	} };
	
	
	public static ReadHandlerPtr ataxx_svram_port_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		offset = ((offset >> 1) & 0x07) | ((offset << 3) & 0x08) | (offset & 0x10);
	    return leland_vram_port_r(offset, 1);
	} };
	
	
	
	/*************************************
	 *
	 *	End-of-frame routine
	 *
	 *************************************/
	
	public static timer_callback scanline_reset = new timer_callback() {
            public void handler(int i) {
                /* flush the remaining scanlines */
		next_update_scanline = 0;
	
		/* turn off the DACs at the start of the frame */
		leland_dac_control = 3;
            }
        };	
        
	public static VhEofCallbackPtr leland_vh_eof = new VhEofCallbackPtr() {
            public void handler() {
                /* reset scrolling */
		scroll_index = 0;
		scroll_pos[0].scanline = 0;
		scroll_pos[0].x = xscroll;
		scroll_pos[0].y = yscroll;
		scroll_pos[0].gfxbank = gfxbank;
	
		/* update anything remaining */
		update_for_scanline(VIDEO_HEIGHT * 8);
	
		/* set a timer to go off at the top of the frame */
		timer_set(cpu_getscanlinetime(0), 0, scanline_reset);
            }
        };
	
	
	/*************************************
	 *
	 *	ROM-based refresh routine
	 *
	 *************************************/
	
	public static VhUpdatePtr leland_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		UBytePtr background_prom = new UBytePtr(memory_region(REGION_USER1));
		GfxElement gfx = Machine.gfx[0];
		int x, y, chunk;
	
		/* update anything remaining */
		update_for_scanline(VIDEO_HEIGHT * 8);
	
		/* loop over scrolling chunks */
		/* it's okay to do this before the palette calc because */
		/* these values are raw indexes, not pens */
		for (chunk = 0; chunk <= scroll_index; chunk++)
		{
			int char_bank = ((scroll_pos[chunk].gfxbank >> 4) & 0x03) * 0x0400;
			int prom_bank = ((scroll_pos[chunk].gfxbank >> 3) & 0x01) * 0x2000;
	
			/* determine scrolling parameters */
			int xfine = scroll_pos[chunk].x % 8;
			int yfine = scroll_pos[chunk].y % 8;
			int xcoarse = scroll_pos[chunk].x / 8;
			int ycoarse = scroll_pos[chunk].y / 8;
			rectangle clip;
	
			/* make a clipper */
			clip = Machine.visible_area;
			if (chunk != 0)
				clip.min_y = scroll_pos[chunk].scanline;
			if (chunk != scroll_index)
				clip.max_y = scroll_pos[chunk + 1].scanline - 1;
	
			/* draw what's visible to the main bitmap */
			for (y = clip.min_y / 8; y < clip.max_y / 8 + 2; y++)
			{
				int ysum = ycoarse + y;
				for (x = 0; x < VIDEO_WIDTH + 1; x++)
				{
					int xsum = xcoarse + x;
					int offs = ((xsum << 0) & 0x000ff) |
					           ((ysum << 8) & 0x01f00) |
					           prom_bank |
					           ((ysum << 9) & 0x1c000);
					int code = background_prom.read(offs) |
					           ((ysum << 2) & 0x300) |
					           char_bank;
					int color = (code >> 5) & 7;
	
					/* draw to the bitmap */
					drawgfx(bitmap, gfx,
							code, 8 * color, 0, 0,
							8 * x - xfine, 8 * y - yfine,
							clip, TRANSPARENCY_NONE_RAW, 0);
				}
			}
		}
	
		/* Merge the two bitmaps together */
		copybitmap(bitmap, fgbitmap, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_BLEND, 6);
	} };
	
	
	
	/*************************************
	 *
	 *	RAM-based refresh routine
	 *
	 *************************************/
	
	public static VhUpdatePtr ataxx_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) 
	{
		GfxElement gfx = Machine.gfx[0];
		int x, y, chunk;
	
		/* update anything remaining */
		update_for_scanline(VIDEO_HEIGHT * 8);
	
		/* loop over scrolling chunks */
		/* it's okay to do this before the palette calc because */
		/* these values are raw indexes, not pens */
		for (chunk = 0; chunk <= scroll_index; chunk++)
		{
			/* determine scrolling parameters */
			int xfine = scroll_pos[chunk].x % 8;
			int yfine = scroll_pos[chunk].y % 8;
			int xcoarse = scroll_pos[chunk].x / 8;
			int ycoarse = scroll_pos[chunk].y / 8;
			rectangle clip;
	
			/* make a clipper */
			clip = Machine.visible_area;
			if (chunk != 0)
				clip.min_y = scroll_pos[chunk].scanline;
			if (chunk != scroll_index)
				clip.max_y = scroll_pos[chunk + 1].scanline - 1;
	
			/* draw what's visible to the main bitmap */
			for (y = clip.min_y / 8; y < clip.max_y / 8 + 2; y++)
			{
				int ysum = ycoarse + y;
				for (x = 0; x < VIDEO_WIDTH + 1; x++)
				{
					int xsum = xcoarse + x;
					int offs = ((ysum & 0x40) << 9) + ((ysum & 0x3f) << 8) + (xsum & 0xff);
					int code = ataxx_qram.read(offs) | ((ataxx_qram.read(offs + 0x4000) & 0x7f) << 8);
	
					/* draw to the bitmap */
					drawgfx(bitmap, gfx,
							code, 0, 0, 0,
							8 * x - xfine, 8 * y - yfine,
							clip, TRANSPARENCY_NONE_RAW, 0);
				}
			}
		}
	
		/* Merge the two bitmaps together */
		copybitmap(bitmap, fgbitmap, 0, 0, 0, 0, Machine.visible_area, TRANSPARENCY_BLEND, 6);
	} };
}
