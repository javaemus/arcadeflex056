/***************************************************************************

Nintendo VS UniSystem and DualSystem - (c) 198? Nintendo of America

	Portions of this code are heavily based on
	Brad Oliver's MESS implementation of the NES.

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.inptport.*;
import static mame056.memory.*;
import static mame056.memoryH.*;
import static WIP.mame056.vidhrdw.ppu2c03b.*;
import static WIP.mame056.vidhrdw.ppu2c03bH.*;
import static mame056.mame.*;
import static common.libc.cstring.*;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.cpuexec.*;
import static mame056.cpuintrfH.*;

public class vsnes
{
	
	/* Globals */
	public static int vsnes_gun_controller;
	
	/* Locals */
	static int[] input_latch = new int[4];
	static UBytePtr remapped_colortable = null;
	
	static int sound_fix=0;
	/*************************************
	 *
	 *	Color Mapping
	 *
	 *************************************/
	
	/* RP2C04-001 */
	static char rp2c04001_colortable[] =
	{
		0x35, 0xff, 0x16, 0x22, 0x1c, 0xff, 0xff, 0x15, /* 0x00 - 0x07 */
		0xff, 0x00, 0x27, 0x05, 0x04, 0x27, 0x08, 0x30, /* 0x08 - 0x0f */
		0x21, 0xff, 0xff, 0x29, 0x3c, 0xff, 0x36, 0x12, /* 0x10 - 0x17 */
		0xff, 0x2b, 0xff, 0xff, 0xff, 0xff, 0xff, 0x01, /* 0x18 - 0x1f */
		0xff, 0x31, 0xff, 0x2a, 0x2c, 0x0c, 0xff, 0xff, /* 0x20 - 0x27 */
		0xff, 0x07, 0x34, 0x06, 0x13, 0xff, 0x26, 0x0f, /* 0x28 - 0x2f */
		0xff, 0x19, 0x10, 0x0a, 0xff, 0xff, 0xff, 0x17, /* 0x30 - 0x37 */
		0xff, 0x11, 0x09, 0xff, 0xff, 0x25, 0x18, 0xff  /* 0x38 - 0x3f */
	};
	
	/* RP2C04-002 */
	static char rp2c04002_colortable[] =
	{
		0xff, 0x27, 0x18, 0xff, 0x3a, 0x25, 0xff, 0x31, /* 0x00 - 0x07 */
		0x16, 0x13, 0x38, 0x34, 0x20, 0x23, 0xff, 0x0b, /* 0x08 - 0x0f */
		0xff, 0x21, 0x06, 0xff, 0x1b, 0x29, 0xff, 0x22, /* 0x10 - 0x17 */
		0xff, 0x24, 0xff, 0xff, 0xff, 0x08, 0xff, 0x03, /* 0x18 - 0x1f */
		0xff, 0x36, 0x26, 0x33, 0x11, 0xff, 0x10, 0x02, /* 0x20 - 0x27 */
		0x14, 0xff, 0x00, 0x09, 0x12, 0x0f, 0xff, 0x30, /* 0x28 - 0x2f */
		0xff, 0xff, 0x2a, 0x17, 0x0c, 0x01, 0x15, 0x19, /* 0x30 - 0x37 */
		0xff, 0x2c, 0x07, 0x37, 0xff, 0x05, 0xff, 0xff  /* 0x38 - 0x3f */
	};
	
	/* RP2C04-003 */
	/* check 0x0f, 0x2e */
	static char rp2c04003_colortable[] =
	{
	
		0xff, 0xff, 0xff, 0x10, 0x1a, 0x30, 0x31, 0x09, /* 0x00 - 0x07 */
		0x01, 0x0f, 0x36, 0x08, 0x15, 0xff, 0xff, 0x30, /* 0x08 - 0x0f */
		0x22, 0x1c, 0xff, 0x12, 0x19, 0x18, 0x17, 0xff, /* 0x10 - 0x17 */
		0x00, 0xff, 0xff, 0x02, 0x16, 0x06, 0xff, 0x35, /* 0x18 - 0x1f */
		0x23, 0xff, 0x8b, 0xf7, 0xff, 0x27, 0x26, 0x20, /* 0x20 - 0x27 */
		0x29, 0xff, 0x21, 0x24, 0x11, 0xff, 0xef, 0xff, /* 0x28 - 0x2f */
		0x2c, 0xff, 0xff, 0xff, 0x07, 0xf9, 0x28, 0xff, /* 0x30 - 0x37 */
		0x0a, 0xff, 0x32, 0x37, 0x13, 0xff, 0xff, 0x0c  /* 0x38 - 0x3f */
	};
	
	/* RP2C05-004 */
	/* check 0x03 0x1d, 0x38, 0x3b*/
	static char rp2c05004_colortable[] =
	{
		0x18, 0xff, 0x1c, 0x89, 0xff, 0xff, 0x01, 0x17, /* 0x00 - 0x07 */
		0x10, 0xff, 0x2a, 0xff, 0x36, 0x37, 0x1a, 0xff, /* 0x08 - 0x0f */
		0x25, 0xff, 0x12, 0xff, 0x0f, 0xff, 0xff, 0x26, /* 0x10 - 0x17 */
		0xff, 0xff, 0x22, 0xff, 0xff, 0x0f, 0x3a, 0x21, /* 0x18 - 0x1f */
		0x05, 0x0a, 0x07, 0xc2, 0x13, 0xff, 0x00, 0x15, /* 0x20 - 0x27 */
		0x0c, 0xff, 0x11, 0xff, 0xff, 0x38, 0xff, 0xff, /* 0x28 - 0x2f */
		0xff, 0xff, 0x08, 0x45, 0xff, 0xff, 0x30, 0x3c, /* 0x30 - 0x37 */
		0x0f, 0x27, 0xff, 0x60, 0x29, 0xff, 0xff, 0x09  /* 0x38 - 0x3f */
	};
	
	
	
	/* remap callback */
	static ppu2c03b_vidaccess_cb remap_colors = new ppu2c03b_vidaccess_cb() {
            public int handler(int num, int addr, int data) {
                /* this is the protection. color codes are shuffled around */
		/* the ones with value 0xff are unknown */
	
		if ( addr >= 0x3f00 )
		{
			int newdata = remapped_colortable.read( data & 0x3f );
	
			if ( newdata != 0xff )
				data = newdata;
	
			/*TODO*///#ifdef MAME_DEBUG
			/*TODO*///else
			/*TODO*///	usrintf_showmessage( "Unmatched color %02x, at address %04x\n", data & 0x3f, addr );
			/*TODO*///#endif
		}
	
		return data;
            }
        };
	
	
	/*************************************
	 *
	 *	Input Ports
	 *
	 *************************************/
	public static WriteHandlerPtr vsnes_in0_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* Toggling bit 0 high then low resets both controllers */
		if (( data & 1 ) != 0)
		{
			/* load up the latches */
			input_latch[0] = readinputport( 0 );
			input_latch[1] = readinputport( 1 );
		}
	} };
	
	public static ReadHandlerPtr gun_in0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	
	
		int ret = ( input_latch[0] ) & 1;
	
		/* shift */
		input_latch[0] >>= 1;
	
		ret |= readinputport( 2 ); 				/* merge coins, etc */
		ret |= ( readinputport( 3 ) & 3 ) << 3; /* merge 2 dipswitches */
	
	
	/* The gun games expect a 1 returned on every 5th read after sound_fix is reset*/
	/* Info Supplied by Ben Parnell <xodnizel@home.com> of FCE Ultra fame */
	
		if (sound_fix == 4)
		{
			ret = 1;
		}
	
	
	sound_fix ++;
	
	
	return ret;
	
	
	} };
	
	
	public static ReadHandlerPtr vsnes_in0_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	
		int ret = ( input_latch[0] ) & 1;
	
		/* shift */
		input_latch[0] >>= 1;
	
		ret |= readinputport( 2 ); 				/* merge coins, etc */
		ret |= ( readinputport( 3 ) & 3 ) << 3; /* merge 2 dipswitches */
	
		return ret;
	
	} };
	
	
	public static ReadHandlerPtr vsnes_in1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int ret = ( input_latch[1] ) & 1;
	
		ret |= readinputport( 3 ) & ~3;			/* merge the rest of the dipswitches */
	
		/* shift */
		input_latch[1] >>= 1;
	
		return ret;
	} };
	
	public static WriteHandlerPtr vsnes_in0_1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* Toggling bit 0 high then low resets both controllers */
		if (( data & 1 ) != 0)
		{
			/* load up the latches */
			input_latch[2] = readinputport( 4 );
			input_latch[3] = readinputport( 5 );
		}
	} };
	
	public static ReadHandlerPtr vsnes_in0_1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int ret = ( input_latch[2] ) & 1;
	
		/* shift */
		input_latch[2] >>= 1;
	
		ret |= readinputport( 6 ); 				/* merge coins, etc */
		ret |= ( readinputport( 7 ) & 3 ) << 3; /* merge 2 dipswitches */
		return ret;
	} };
	
	public static ReadHandlerPtr vsnes_in1_1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int ret = ( input_latch[3] ) & 1;
	
		ret |= readinputport( 7 ) & ~3;			/* merge the rest of the dipswitches */
	
		/* shift */
		input_latch[3] >>= 1;
	
		return ret;
	
	} };
	
	/*************************************
	 *
	 *	Init machine
	 *
	 *************************************/
	public static InitMachinePtr vsnes_init_machine = new InitMachinePtr() { public void handler() 
	{
		input_latch[0] = input_latch[1] = 0;
		input_latch[2] = input_latch[3] = 0;
	
		/* reset the ppu */
		ppu2c03b_reset( 0, 1 );
	
		/* if we need to remap, install the callback */
		if ( remapped_colortable != null )
			ppu2c03b_set_vidaccess_callback( 0, remap_colors );
	} };
	
	/*************************************
	 *
	 *	Init machine
	 *
	 *************************************/
	public static InitMachinePtr vsdual_init_machine = new InitMachinePtr() { public void handler() 
	{
		input_latch[0] = input_latch[1] = 0;
		input_latch[2] = input_latch[3] = 0;
	
		/* reset the ppu */
		ppu2c03b_reset( 0,1);
		ppu2c03b_reset( 1,1 );
	
	/* if we need to remap, install the callback */
		if ( remapped_colortable != null )
		{
		ppu2c03b_set_vidaccess_callback( 0, remap_colors );
		ppu2c03b_set_vidaccess_callback( 1, remap_colors );
		}
	} };
	
	/*************************************
	 *
	 *	Common init for all games
	 *º
	 *************************************/
	public static InitDriverPtr init_vsnes = new InitDriverPtr() { public void handler() 
	{
		/* set the controller to default */
		vsnes_gun_controller = 0;
	
		/* no color remapping */
		remapped_colortable = null;
	} };
	
	/**********************************************************************************
	 *
	 *	Game and Board-specific initialization
	 *
	 **********************************************************************************/
	
	public static WriteHandlerPtr vsnormal_vrom_banking = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* switch vrom */
		ppu2c03b_set_videorom_bank( 0, 0, 8, ( data & 4 )!=0 ? 1 : 0, 512 );
	
		/* bit 1 ( data & 2 ) enables writes to extra ram, we ignore it */
	
		/* move along */
		vsnes_in0_w.handler(offset, data );
	} };
	
	/* Most games switch VROM Banks in controller 0 write */
	/* they dont do any other trickery */
	public static InitDriverPtr init_vsnormal = new InitDriverPtr() { public void handler() 
	{
		/* vrom switching is enabled with bit 2 of $4016 */
		install_mem_write_handler( 0, 0x4016, 0x4016, vsnormal_vrom_banking );
	} };
	
	/**********************************************************************************/
	
	/* Super Mario Bros. Extra ram at $6000 (NV?) and remapped colors */
	
	public static InitDriverPtr init_suprmrio = new InitDriverPtr() { public void handler() 
	{
		/* common init */
		init_vsnes.handler();
	
		/* normal banking */
		init_vsnormal.handler();
	
		/* extra ram at $6000 is enabled with bit 1 of $4016 */
		install_mem_read_handler( 0, 0x6000, 0x7fff, MRA_RAM );
		install_mem_write_handler( 0, 0x6000, 0x7fff, MWA_RAM );
	
		/* now override the vidaccess callback */
		/* we need to remap color tables */
		/* this *is* the VS games protection, I guess */
		remapped_colortable = new UBytePtr(rp2c05004_colortable);
	} };
	
	/**********************************************************************************/
	
	/* Gun Games - VROM Banking in controller 0 write */
	static int zapstore;
        
	public static WriteHandlerPtr duckhunt_vrom_banking = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	
	
	
		/* switch vrom */
		ppu2c03b_set_videorom_bank( 0, 0, 8, ( data & 4 )!=0 ? 1 : 0, 512 );
	
		/* here we do things a little different */
		if (( data & 1 ) != 0)
		{
	
			/* load up the latches */
			input_latch[0] = readinputport( 0 );
	
			/* do the gun thing */
			if ( vsnes_gun_controller != 0 )
			{
				int x = readinputport( 4 );
				int y = readinputport( 5 );
				int pix, color_base;
				int[] pens = Machine.pens;
	
				/* get the pixel at the gun position */
				pix = ppu2c03b_get_pixel( 0, x, y );
	
				/* get the color base from the ppu */
				color_base = ppu2c03b_get_colorbase( 0 );
	
				/* look at the screen and see if the cursor is over a bright pixel */
				if ( ( pix == pens[color_base+0x20] ) || ( pix == pens[color_base+0x30] ) ||
					 ( pix == pens[color_base+0x33] ) || ( pix == pens[color_base+0x34] ) )
				{
					input_latch[0] |= 0x40;
	
	
				}
			}
	
			input_latch[1] = readinputport( 1 );
		}
	
	
	                        if ((zapstore&1)!=0 && ((data&1)==0))
	
						/* reset sound_fix to keep sound from hanging */
	
	                                {sound_fix=0;}
	                        zapstore=data;
	
	} };
	
	public static InitDriverPtr init_duckhunt = new InitDriverPtr() { public void handler() 
	{
		install_mem_read_handler ( 0, 0x4016, 0x4016, gun_in0_r);
		/* vrom switching is enabled with bit 2 of $4016 */
		install_mem_write_handler( 0, 0x4016, 0x4016, duckhunt_vrom_banking );
	
		/* common init */
		init_vsnes.handler();
	
		/* enable gun controller */
		vsnes_gun_controller = 1;
	} };
	
	/**********************************************************************************/
	
	/* The Goonies, VS Gradius: ROMs bankings at $8000-$ffff */
	
	public static WriteHandlerPtr goonies_rom_banking = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int reg = ( offset >> 12 ) & 0x07;
		int bankoffset = ( data & 7 ) * 0x2000 + 0x10000;
	
		switch( reg )
		{
			case 0: /* code bank 0 */
				memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), bankoffset), 0x2000 );
			break;
	
			case 2: /* code bank 1 */
				memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x0a000), new UBytePtr(memory_region( REGION_CPU1 ), bankoffset), 0x2000 );
			break;
	
			case 4: /* code bank 2 */
				memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x0c000), new UBytePtr(memory_region( REGION_CPU1 ), bankoffset), 0x2000 );
			break;
	
			case 6: /* vrom bank 0 */
				ppu2c03b_set_videorom_bank( 0, 0, 4, data, 256 );
			break;
	
			case 7: /* vrom bank 1 */
				ppu2c03b_set_videorom_bank( 0, 4, 4, data, 256 );
			break;
		}
	} };
	
	public static InitDriverPtr init_goonies = new InitDriverPtr() { public void handler() 
	{
		/* We do manual banking, in case the code falls through */
		/* Copy the initial banks */
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), 0x18000), 0x8000 );
	
		/* banking is done with writes to the $8000-$ffff area */
		install_mem_write_handler( 0, 0x8000, 0xffff, goonies_rom_banking );
	
		/* common init */
		init_vsnes.handler();
	
		/* now override the vidaccess callback */
		remapped_colortable = new UBytePtr(rp2c04003_colortable);
	} };
	
	public static InitDriverPtr init_vsgradus = new InitDriverPtr() { public void handler() 
	{
		/* We do manual banking, in case the code falls through */
		/* Copy the initial banks */
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), 0x18000), 0x8000 );
	
		/* banking is done with writes to the $8000-$ffff area */
		install_mem_write_handler( 0, 0x8000, 0xffff, goonies_rom_banking );
	
		/* common init */
		init_vsnes.handler();
	
		/* now override the vidaccess callback */
		remapped_colortable = new UBytePtr(rp2c04001_colortable);
	} };
	
	public static InitDriverPtr init_vspinbal = new InitDriverPtr() { public void handler() 
	{
		/* common init */
		init_vsnes.handler();
	
		/* normal banking */
		init_vsnormal.handler();
	
		/* now override the vidaccess callback */
		remapped_colortable = new UBytePtr(rp2c04001_colortable);
	
	
	} };
	
	public static InitDriverPtr init_hogalley = new InitDriverPtr() { public void handler() 
	{
	
		install_mem_read_handler ( 0, 0x4016, 0x4016, gun_in0_r);
		/* vrom switching is enabled with bit 2 of $4016 */
		install_mem_write_handler( 0, 0x4016, 0x4016, duckhunt_vrom_banking );
	
		/* common init */
		init_vsnes.handler();
	
		/* enable gun controller */
		vsnes_gun_controller = 1;
	
		/* now override the vidaccess callback */
		remapped_colortable = new UBytePtr(rp2c04001_colortable);
	} };
	
	/**********************************************************************************/
	
	/* Dr Mario: ROMs bankings at $8000-$ffff */
	
	static int drmario_shiftreg;
	static int drmario_shiftcount;
        /* basically, a MMC1 mapper from the nes */
	static int size16k, switchlow, vrom4k;
	
	public static WriteHandlerPtr drmario_rom_banking = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int reg = ( offset >> 13 );
	
		/* reset mapper */
		if (( data & 0x80 ) != 0)
		{
			drmario_shiftreg = drmario_shiftcount = 0;
	
			size16k = 1;
			switchlow = 1;
			vrom4k = 0;
	
			return;
		}
	
		/* see if we need to clock in data */
		if ( drmario_shiftcount < 5 )
		{
			drmario_shiftreg >>= 1;
			drmario_shiftreg |= ( data & 1 ) << 4;
			drmario_shiftcount++;
		}
	
		/* are we done shifting? */
		if ( drmario_shiftcount == 5 )
		{
			/* reset count */
			drmario_shiftcount = 0;
	
			/* apply data to registers */
			switch( reg )
			{
				case 0:		/* mirroring and options */
					{
						int mirroring;
	
						vrom4k = drmario_shiftreg & 0x10;
						size16k = drmario_shiftreg & 0x08;
						switchlow = drmario_shiftreg & 0x04;
	
						switch( drmario_shiftreg & 3 )
						{
							case 0:
								mirroring = PPU_MIRROR_LOW;
							break;
	
							case 1:
								mirroring = PPU_MIRROR_HIGH;
							break;
	
							case 2:
								mirroring = PPU_MIRROR_VERT;
							break;
	
							default:
							case 3:
								mirroring = PPU_MIRROR_HORZ;
							break;
						}
	
						/* apply mirroring */
						ppu2c03b_set_mirroring( 0, mirroring );
					}
				break;
	
				case 1:	/* video rom banking - bank 0 - 4k or 8k */
					ppu2c03b_set_videorom_bank( 0, 0, ( vrom4k )!=0 ? 4 : 8, drmario_shiftreg, ( vrom4k )!=0 ? 256 : 512 );
				break;
	
				case 2: /* video rom banking - bank 1 - 4k only */
					if ( vrom4k != 0 )
						ppu2c03b_set_videorom_bank( 0, 4, 4, drmario_shiftreg, 256 );
				break;
	
				case 3:	/* program banking */
					{
						int bank = ( drmario_shiftreg & 0x03 ) * 0x4000;
	
						if (size16k == 0)
						{
							/* switch 32k */
							memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), 0x010000+bank), 0x8000 );
						}
						else
						{
							/* switch 16k */
							if ( switchlow != 0)
							{
								/* low */
								memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), 0x010000+bank), 0x4000 );
							}
							else
							{
								/* high */
								memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x0c000), new UBytePtr(memory_region( REGION_CPU1 ), 0x010000+bank), 0x4000 );
							}
						}
					}
				break;
			}
	
			drmario_shiftreg = 0;
		}
	} };
	
	public static InitDriverPtr init_drmario = new InitDriverPtr() { public void handler() 
	{
		/* We do manual banking, in case the code falls through */
		/* Copy the initial banks */
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), 0x10000), 0x4000 );
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x0c000), new UBytePtr(memory_region( REGION_CPU1 ), 0x1c000), 0x4000 );
	
		/* MMC1 mapper at writes to $8000-$ffff */
		install_mem_write_handler( 0, 0x8000, 0xffff, drmario_rom_banking );
	
		drmario_shiftreg = 0;
		drmario_shiftcount = 0;
	
		/* common init */
		init_vsnes.handler();
	
		/* now override the vidaccess callback */
		remapped_colortable = new UBytePtr(rp2c04003_colortable);
	} };
	
	/**********************************************************************************/
	
	/* Excite Bike */
	
	public static InitDriverPtr init_excitebk = new InitDriverPtr() { public void handler() 
	{
		/* common init */
		init_vsnes.handler();
	
		/* normal banking */
		init_vsnormal.handler();
	
		/* now override the vidaccess callback */
		/* we need to remap color tables */
		/* this *is* the VS games protection, I guess */
		remapped_colortable = new UBytePtr(rp2c04003_colortable);
	} };
	
	
	public static InitDriverPtr init_excitbkj = new InitDriverPtr() { public void handler() 
	{
		/* common init */
		init_vsnes.handler();
	
		/* normal banking */
		init_vsnormal.handler();
	
		/* now override the vidaccess callback */
		/* we need to remap color tables */
		/* this *is* the VS games protection, I guess */
		remapped_colortable = new UBytePtr(rp2c05004_colortable);
	} };
	
	/**********************************************************************************/
	
	/* Mach Rider */
	
	public static InitDriverPtr init_machridr = new InitDriverPtr() { public void handler() 
	{
		/* common init */
		init_vsnes.handler();
	
		/* normal banking */
		init_vsnormal.handler();
	
		/* now override the vidaccess callback */
		/* we need to remap color tables */
		/* this *is* the VS games protection, I guess */
		remapped_colortable = new UBytePtr(rp2c04002_colortable);
	} };
	
	/**********************************************************************************/
	
	/* VS Slalom */
	
	public static InitDriverPtr init_vsslalom = new InitDriverPtr() { public void handler() 
	{
		/* common init */
		init_vsnes.handler();
	
		/* now override the vidaccess callback */
		/* we need to remap color tables */
		/* this *is* the VS games protection, I guess */
		remapped_colortable = new UBytePtr(rp2c04002_colortable);
	} };
	
	/**********************************************************************************/
	
	/* Castelvania: ROMs bankings at $8000-$ffff */
	
	public static WriteHandlerPtr castlevania_rom_banking = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int rombank = 0x10000 + ( data & 7 ) * 0x4000;
	
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), rombank), 0x4000 );
	} };
	
	public static InitDriverPtr init_cstlevna = new InitDriverPtr() { public void handler() 
	{
		/* when starting the game , the 1st 16k and the last 16k are loaded into the 2 banks */
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), 0x28000), 0x8000 );
	
	   	/* banking is done with writes to the $8000-$ffff area */
		install_mem_write_handler( 0, 0x8000, 0xffff, castlevania_rom_banking );
	
		/* common init */
		init_vsnes.handler();
	
		/* now override the vidaccess callback */
		/* we need to remap color tables */
		/* this *is* the VS games protection, I guess */
		remapped_colortable = new UBytePtr(rp2c04002_colortable);
	} };
	
	/**********************************************************************************/
	
	/* VS Top Gun: ROMs bankings at $8000-$ffff, plus some protection */
	
	public static ReadHandlerPtr topgun_security_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		/* low part must be 0x1b */
		return ppu2c03b_0_r.handler(2 ) | 0x1b;
	} };
        
        static int control;
	
	public static WriteHandlerPtr topgun_security_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
		ppu2c03b_set_mirroring( 0, PPU_MIRROR_VERT );
	
		if ( offset == 0 )
		{
			data &= 0x7f;
			control = data;
			ppu2c03b_0_w.handler(0, data );
			return;
		}
	
		ppu2c03b_0_w.handler(0, control | ( data & 0x80 ) );
		ppu2c03b_0_w.handler(1, data );
	} };
	
	public static InitDriverPtr init_vstopgun = new InitDriverPtr() { public void handler() 
	{
		/* when starting a mmc1 game , the 1st 16k and the last 16k are loaded into the 2 banks */
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), 0x28000), 0x8000 );
	
	   	/* banking is done with writes to the $8000-$ffff area */
		install_mem_write_handler( 0, 0x8000, 0xffff, castlevania_rom_banking );
	
		/* tap on the PPU, due to some tricky protection */
		install_mem_read_handler( 0, 0x2002, 0x2002, topgun_security_r );
		install_mem_write_handler( 0, 0x2000, 0x2001, topgun_security_w );
	
		/* common init */
		init_vsnes.handler();
	
		/* now override the vidaccess callback */
		/* we need to remap color tables */
		/* this *is* the VS games protection, I guess */
		remapped_colortable = new UBytePtr(rp2c04003_colortable);
	} };
	
	/**********************************************************************************/
	
	/* RBI Baseball: ROMs bankings at $8000-$ffff */
	
	static int rbibb_scanline_counter;
	static int rbibb_scanline_latch;
	
	static ppu2c03b_scanline_cb rbibb_scanline_cb = new ppu2c03b_scanline_cb() {
            public void handler(int num, int scanline, int vblank, int blanked) {
                if ( vblank==0 && blanked==0 )
		{
			if ( --rbibb_scanline_counter <= 0 )
			{
				rbibb_scanline_counter = rbibb_scanline_latch;
				cpu_set_irq_line( 0, 0, PULSE_LINE );
			}
		}
            }
        };
	
        static int VSindex;
	
	public static ReadHandlerPtr rbi_hack_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	/* Supplied by Ben Parnell <xodnizel@home.com> of FCE Ultra fame */
	
	
	if (offset == 0)
		{
			VSindex=0;
			return 0xFF;
	
		}
	
		else{
	
		switch(VSindex++)
	    		{
	
	    		case 9:
	    		return 0x6F;
				//break;
	
				case 14:
				return 0x94;
				//break;
	
	    		default:
	    		return 0xB4;
				//break;
	    		}
			}
	} };
        
        /* basically, a MMC3 mapper from the nes */
        static int last_bank = 0xff;
        static int rbibb_command;
	
	public static WriteHandlerPtr rbibb_rom_switch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		
		switch( offset & 0x7001 )
		{
			case 0x0000:
				rbibb_command = data;
	
				if ( last_bank != ( data & 0xc0 ) )
				{
					/* reset the banks */
					memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), 0x1c000), 0x4000 );
					memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x0c000), new UBytePtr(memory_region( REGION_CPU1 ), 0x1c000), 0x4000 );
	
					last_bank = data & 0xc0;
				}
			break;
	
			case 0x0001:
				{
					int cmd = rbibb_command & 0x07;
					int page = ( rbibb_command & 0x80 ) >> 5;
	
					switch( cmd )
					{
						case 0:	/* char banking */
						case 1: /* char banking */
							data &= 0xfe;
							page ^= ( cmd << 1 );
							ppu2c03b_set_videorom_bank( 0, page, 2, data, 64 );
	
	
						break;
	
						case 2: /* char banking */
						case 3: /* char banking */
						case 4: /* char banking */
						case 5: /* char banking */
							page ^= cmd + 2;
							ppu2c03b_set_videorom_bank( 0, page, 1, data, 64 );
	
						break;
	
						case 6: /* program banking */
							if ( (rbibb_command & 0x40) != 0)
							{
								/* high bank */
								int bank = ( data & 0x07 ) * 0x2000 + 0x10000;
	
								memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x0c000), new UBytePtr(memory_region( REGION_CPU1 ), bank), 0x2000 );
								memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), 0x1c000), 0x2000 );
							}
							else
							{
								/* low bank */
								int bank = ( data & 0x07 ) * 0x2000 + 0x10000;
	
								memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), bank), 0x2000 );
								memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x0c000), new UBytePtr(memory_region( REGION_CPU1 ), 0x1c000), 0x2000 );
							}
						break;
	
						case 7: /* program banking */
							{
								/* mid bank */
								int bank = ( data & 0x07 ) * 0x2000 + 0x10000;
	
								memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x0a000), new UBytePtr(memory_region( REGION_CPU1 ), bank), 0x2000 );
							}
						break;
					}
				}
			break;
	
			case 0x2000: /* mirroring */
				if (( data & 0x40 ) != 0)
					ppu2c03b_set_mirroring( 0, PPU_MIRROR_HIGH );
				else
					ppu2c03b_set_mirroring( 0, ( data & 1 )!=0 ? PPU_MIRROR_HORZ : PPU_MIRROR_VERT );
			break;
	
			case 0x2001: /* enable ram at $6000 */
				/* ignored - we always enable it */
			break;
	
			case 0x4000: /* scanline counter */
				rbibb_scanline_counter = data;
			break;
	
			case 0x4001: /* scanline latch */
				rbibb_scanline_latch = data;
			break;
	
			case 0x6000: /* disable irqs */
				ppu2c03b_set_scanline_callback( 0, null );
			break;
	
			case 0x6001: /* enable irqs */
				ppu2c03b_set_scanline_callback( 0, rbibb_scanline_cb );
			break;
		}
	} };
	
	public static InitDriverPtr init_rbibb = new InitDriverPtr() { public void handler() 
	{
		/* We do manual banking, in case the code falls through */
		/* Copy the initial banks */
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), 0x2c000), 0x4000 );
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x0c000), new UBytePtr(memory_region( REGION_CPU1 ), 0x10000), 0x4000 );
	
		/* RBI Base ball hack */
		install_mem_read_handler(0,0x5e00, 0x5e01, rbi_hack_r) ;
	
		/* MMC3 mapper at writes to $8000-$ffff */
		install_mem_write_handler( 0, 0x8000, 0xffff, rbibb_rom_switch_w );
	
		/* extra ram at $6000-$7fff */
		install_mem_read_handler( 0, 0x6000, 0x7fff, MRA_RAM );
		install_mem_write_handler( 0, 0x6000, 0x7fff, MWA_RAM );
	
		/* common init */
		init_vsnes.handler();
	
	} };
	
	
	public static ReadHandlerPtr xevious_hack_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	
	
	return 0x05;
	
	} };
	
	public static ReadHandlerPtr xevious_hack_r1  = new ReadHandlerPtr() { public int handler(int offset)
	{
	
	
	
	return 0x01;
	
	} };
	
	
	public static ReadHandlerPtr xevious_hack_r2  = new ReadHandlerPtr() { public int handler(int offset)
	{
	
	
	
	return 0x89;
	
	} };
	
	
	public static ReadHandlerPtr xevious_hack_r3  = new ReadHandlerPtr() { public int handler(int offset)
	{
	
	
	
	return 0x37;
	
	} };
	
	
	public static InitDriverPtr init_xevious = new InitDriverPtr() { public void handler() 
	{
		/* We do manual banking, in case the code falls through */
		/* Copy the initial banks */
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), 0x1c000), 0x4000 );
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x0c000), new UBytePtr(memory_region( REGION_CPU1 ), 0x1c000), 0x4000 );
	
		/* RBI Base ball hack */
	
	
		/* MMC3 mapper at writes to $8000-$ffff */
		install_mem_write_handler( 0, 0x8000, 0xffff, rbibb_rom_switch_w );
	
		//install_mem_read_handler(0,0x54ff,0x54ff,xevious_hack_r);
		//install_mem_read_handler(0,0x5678,0x5678,xevious_hack_r1);
		//install_mem_read_handler(0,0x578f,0x578f,xevious_hack_r2);
		//install_mem_read_handler(0,0x5567,0x5567,xevious_hack_r3);
	
		/* extra ram at $6000-$7fff */
	//	install_mem_read_handler( 0, 0x6000, 0x7fff, MRA_RAM );
	//	install_mem_write_handler( 0, 0x6000, 0x7fff, MWA_RAM );
	
		/* common init */
		init_vsnes.handler();
		//remapped_colortable = rp2c04001_colortable;
	} };
	
	static int security_counter;
        
	public static ReadHandlerPtr tko_security_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		
		int security_data[] = {
			0xff, 0xbf, 0xb7, 0x97, 0x97, 0x17, 0x57, 0x4f,
			0x6f, 0x6b, 0xeb, 0xa9, 0xb1, 0x90, 0x94, 0x14,
			0x56, 0x4e, 0x6f, 0x6b, 0xeb, 0xa9, 0xb1, 0x90,
			0xd4, 0x5c, 0x3e, 0x26, 0x87, 0x83, 0x13, 0x00
		};
	
		if ( offset == 0 )
		{
			security_counter = 0;
			return 0;
		}
	
		return security_data[(security_counter++)];
	
	} };
	
	public static InitDriverPtr init_tkoboxng = new InitDriverPtr() { public void handler() 
	{
		/* We do manual banking, in case the code falls through */
		/* Copy the initial banks */
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), 0x1c000), 0x4000 );
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x0c000), new UBytePtr(memory_region( REGION_CPU1 ), 0x1c000), 0x4000 );
	
		/* MMC3 mapper at writes to $8000-$ffff */
		install_mem_write_handler( 0, 0x8000, 0xffff, rbibb_rom_switch_w );
	
		/* extra ram at $6000-$7fff */
		install_mem_read_handler( 0, 0x6000, 0x7fff, MRA_RAM );
		install_mem_write_handler( 0, 0x6000, 0x7fff, MWA_RAM );
	
		/* security device at $5e00-$5e01 */
		install_mem_read_handler( 0, 0x5e00, 0x5e01, tko_security_r );
	
	
		/* common init */
		init_vsnes.handler();
	
		/* now override the vidaccess callback */
		/* we need to remap color tables */
		/* this *is* the VS games protection, I guess */
		remapped_colortable = new UBytePtr(rp2c04003_colortable);
	} };
	
	/**********************************************************************************/
	
	/* VS SkyKid: ROMs bankings at $8000-$ffff */
	static int mapper_command;
        
	public static WriteHandlerPtr vsskykid_rom_switch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	
	/* basically, a MMC3 mapper from the nes with out the program rom banking */
	
	
		switch( offset & 0x7001 )
		{
			case 0x0000:
				mapper_command = data;
	
	
			case 0x0001:
				{
					int cmd = mapper_command & 0x07;
					int page = ( mapper_command & 0x80 ) >> 5;
	
					switch( cmd )
					{
						case 0:	/* char banking */
						case 1:	/* char banking */
	
							data &= 0xfe;
							page ^= ( cmd << 1 );
	
							ppu2c03b_set_videorom_bank( 0, page, 2, data, 64 );
						break;
	
						case 2: /* char banking */
						case 3: /* char banking */
						case 4: /* char banking */
						case 5: /* char banking */
	
							page ^= cmd + 2;
	
							ppu2c03b_set_videorom_bank( 0, page, 1, data, 64 );
						break;
	
	
					}
				}
			break;
	
	
	
		}
	
	
	} };
	
	public static InitDriverPtr init_vsskykid = new InitDriverPtr() { public void handler() 
	{
		/* ??? mapper at writes to $8000-$ffff */
		install_mem_write_handler( 0, 0x8000, 0xffff, vsskykid_rom_switch_w );
	
	
		/* common init */
		init_vsnes.handler();
	
	
	
	
	} };
	
	
	
	/**********************************************************************************/
	/* Platoon rom banking */
	
	public static WriteHandlerPtr mapper68_rom_banking = new WriteHandlerPtr() {public void handler(int offset, int data){
	
		switch (offset & 0x7000)
		{
			case 0x0000:
			ppu2c03b_set_videorom_bank(0,0,2,data,128);
	
			break;
			case 0x1000:
			ppu2c03b_set_videorom_bank(0,2,2,data,128);
	
			break;
			case 0x2000:
			ppu2c03b_set_videorom_bank(0,4,2,data,128);
	
			break;
			case 0x3000: /* ok? */
			ppu2c03b_set_videorom_bank(0,6,2,data,128);
	
			break;
	
			case 0x7000:
			memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), 0x10000 +data*0x4000), 0x4000 );
			break;
	
	
		}
	
	} };
	
	
	
	public static InitDriverPtr init_platoon = new InitDriverPtr() { public void handler() 
	{
	
	/* when starting a mapper 68 game  the first 16K ROM bank in the cart is loaded into $8000
		the LAST 16K ROM bank is loaded into $C000. The last 16K of ROM cannot be swapped. */
	
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x08000), new UBytePtr(memory_region( REGION_CPU1 ), 0x10000), 0x4000 );
		memcpy( new UBytePtr(memory_region( REGION_CPU1 ), 0x0c000), new UBytePtr(memory_region( REGION_CPU1 ), 0x2c000), 0x4000 );
	
		install_mem_write_handler( 0, 0x8000, 0xffff, mapper68_rom_banking );
	
		init_vsnes.handler();
	
	 	remapped_colortable = new UBytePtr(rp2c04001_colortable);
	
	
	
	} };
	
	
	/**********************************************************************************/
	
	/* VS Tennis */
	
	public static WriteHandlerPtr vstennis_vrom_banking = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int other_cpu = cpu_getactivecpu() ^ 1;
	
		/* switch vrom */
		ppu2c03b_set_videorom_bank( cpu_getactivecpu(), 0, 8, ( data & 4 )!=0 ? 1 : 0, 512 );
	
	
		/* bit 1 ( data & 2 ) triggers irq on the other cpu */
		cpu_set_irq_line( other_cpu, 0, ( data & 2 )!=0 ? CLEAR_LINE : ASSERT_LINE );
	
		/* move along */
		if ( cpu_getactivecpu() == 0 )
			vsnes_in0_w.handler(offset, data );
		else
			vsnes_in0_1_w.handler(offset, data );
	} };
	
	public static InitDriverPtr init_vstennis = new InitDriverPtr() { public void handler() 
	{
		/* vrom switching is enabled with bit 2 of $4016 */
		install_mem_write_handler( 0, 0x4016, 0x4016, vstennis_vrom_banking );
		install_mem_write_handler( 1, 0x4016, 0x4016, vstennis_vrom_banking );
	
		/* shared ram at $6000 */
		install_mem_read_handler( 0, 0x6000, 0x7fff, MRA_BANK2 );
		install_mem_write_handler( 0, 0x6000, 0x7fff, MWA_BANK2 );
		install_mem_read_handler( 1, 0x6000, 0x7fff, MRA_BANK2 );
		install_mem_write_handler( 1, 0x6000, 0x7fff, MWA_BANK2 );
	
	
	
	
	} };
	
	
	/**********************************************************************/
	/* Wrecking Crew Init*/
	
	public static InitDriverPtr init_wrecking = new InitDriverPtr() { public void handler() 
	{
	/* only differance between this and vstennis is the colors */
	
		init_vstennis.handler();
		remapped_colortable = new UBytePtr(rp2c04002_colortable);
	} };
	
	/**********************************************************************/
	/* VS Balloon Fight */
	
	public static InitDriverPtr init_balonfgt = new InitDriverPtr() { public void handler() 
	{
	/* only differance between this and vstennis is the colors */
	
		init_vstennis.handler();
	
		remapped_colortable = new UBytePtr(rp2c04003_colortable);
	} };
	
	
	/**********************************************************************/
	/* VS Baseball */
	
	public static InitDriverPtr init_vsbball = new InitDriverPtr() { public void handler() 
	{
	/* only differance between this and vstennis is the colors */
	
		init_vstennis.handler();
	
                remapped_colortable = new UBytePtr(rp2c04001_colortable);
	
	} };
	
	
	/**********************************************************************/
	/* Dual Ice climr Jpn */
	
	public static InitDriverPtr init_iceclmrj = new InitDriverPtr() { public void handler() 
	{
	/* only differance between this and vstennis is the colors */
	
		init_vstennis.handler();
	
                remapped_colortable = new UBytePtr(rp2c05004_colortable);
	
	} };
	
	//remapped_colortable = rp2c04002_colortable;
	//remapped_colortable = new UBytePtr(rp2c04003_colortable);
	//remapped_colortable = rp2c05004_colortable;
}
