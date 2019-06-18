/*
Championship VBall
Driver by Paul "TBBle" Hampson

TODO:
Needs to be tilemapped. The background layer and sprite layer are identical to spdodgeb, except for the
 back-switched graphics roms and the size of the pallete banks.
Someone needs to look at Naz's board, and see what PCM sound chips are present.
And get whatever's in the dip package on Naz's board. (BG/FG Roms, I hope)
I'd also love to know whether Naz's is a bootleg or is missing the story for a different reason (US release?)

*/


/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.drivers;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;

import static mame056.commonH.*;
import static mame056.common.*;
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

import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame056.sound.MSM5205.*;
import static mame056.sound.MSM5205H.*;

import static WIP.mame056.vidhrdw.vball.*;
import static mame056.vidhrdw.generic.*;
import static mame056.inputH.*;
// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.sound._2151intf.*;
import static mame056.sound._2151intfH.*;
import static mame056.sound.mixerH.*;
import static mame056.sound.oki6295.*;
import static mame056.sound.oki6295H.*;

public class vball
{
	
	/* private globals */
	static int sound_irq, ym_irq;
	static int[] adpcm_pos=new int[2],adpcm_end=new int[2],adpcm_idle=new int[2];
	/* end of private globals */
	
	public static InitMachinePtr vb_init_machine = new InitMachinePtr() { public void handler() {
		sound_irq = Z80_NMI_INT;
		ym_irq = 0;//-1000;
	
	} };
	
	public static WriteHandlerPtr vb_bankswitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		UBytePtr RAM = new UBytePtr(memory_region(REGION_CPU1));
		cpu_setbank( 1,new UBytePtr(RAM,  0x10000 + ( 0x4000 * ( data & 1 ) ) ) );
	
		if (vball_gfxset != ((data  & 0x20) ^ 0x20)) {
			vball_gfxset = (data  & 0x20) ^ 0x20;
			memset(dirtybuffer,1, 0x800);
		}
	
	//	logerror("CPU #0 PC %04x: warning - write %02x to bankswitch memory address 1009n",cpu_get_pc(),data);
	} };
	
	/* The sound system comes all but verbatim from Double Dragon */
	
	
	public static WriteHandlerPtr cpu_sound_command_w = new WriteHandlerPtr() {public void handler(int offset, int data) {
		soundlatch_w.handler(offset, data );
		cpu_cause_interrupt( 1, sound_irq );
		logerror("Sound_command_wn");
	} };
	
	public static WriteHandlerPtr dd_adpcm_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		int chip = 0;
		logerror("dd_adpcm_w: %d %dn",offset, data);
		switch (offset)
		{
			case 0:
				adpcm_idle[chip] = 1;
				MSM5205_reset_w.handler(chip,1);
				break;
	
			case 3:
				adpcm_pos[chip] = (data) * 0x200;
				adpcm_end[chip] = (data + 1) * 0x200;
				break;
	
			case 1:
				adpcm_idle[chip] = 0;
				MSM5205_reset_w.handler(chip,0);
				break;
		}
	} };
        
        static int adpcm_data[] = { -1, -1 };
	
	static vclk_interruptPtr dd_adpcm_int = new vclk_interruptPtr() {
            public void handler(int chip) {
                if (adpcm_pos[chip] >= adpcm_end[chip] || adpcm_pos[chip] >= 0x10000)
		{
			adpcm_idle[chip] = 1;
			MSM5205_reset_w.handler(chip,1);
		}
		else if (adpcm_data[chip] != -1)
		{
			MSM5205_data_w.handler(chip,adpcm_data[chip] & 0x0f);
			adpcm_data[chip] = -1;
		}
		else
		{
			UBytePtr ROM = new UBytePtr(memory_region(REGION_SOUND1), 0x10000 * chip);
	
			adpcm_data[chip] = ROM.read(adpcm_pos[chip]++);
			MSM5205_data_w.handler(chip,adpcm_data[chip] >> 4);
		}
            }
        };
	
	public static ReadHandlerPtr dd_adpcm_status_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
	//	logerror("dd_adpcm_status_rn");
		return adpcm_idle[0] + (adpcm_idle[1] << 1);
	} };
	
	
	public static WriteHandlerPtr vb_scrollx_hi_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		vb_scrollx_hi = (data & 0x02) << 7;
		vb_bgprombank_w((data >> 2)&0x07);
		vb_spprombank_w((data >> 5)&0x07);
	
	//	logerror("vb_scrollx_hi %02xn",data);
	
	} };
	
	public static Memory_ReadAddress readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x0fff, MRA_RAM ),
		new Memory_ReadAddress( 0x1000, 0x1000, input_port_0_r ),
		new Memory_ReadAddress( 0x1001, 0x1001, input_port_1_r ),
		new Memory_ReadAddress( 0x1002, 0x1002, input_port_2_r ),
		new Memory_ReadAddress( 0x1003, 0x1003, input_port_3_r ),
		new Memory_ReadAddress( 0x1004, 0x1004, input_port_4_r ),
		new Memory_ReadAddress( 0x1005, 0x1005, input_port_5_r ),
		new Memory_ReadAddress( 0x1006, 0x1006, input_port_6_r ),
		new Memory_ReadAddress( 0x4000, 0x7fff, MRA_BANK1 ),
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress vball2pj_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x0fff, MRA_RAM ),
		new Memory_ReadAddress( 0x1000, 0x1000, input_port_0_r ),
		new Memory_ReadAddress( 0x1001, 0x1001, input_port_1_r ),
		new Memory_ReadAddress( 0x1002, 0x1002, input_port_2_r ),
		new Memory_ReadAddress( 0x1003, 0x1003, input_port_3_r ),
		new Memory_ReadAddress( 0x1004, 0x1004, input_port_4_r ),
		new Memory_ReadAddress( 0x4000, 0x7fff, MRA_BANK1 ),
		new Memory_ReadAddress( 0x8000, 0xffff, MRA_ROM ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x07ff, MWA_RAM ),
		new Memory_WriteAddress( 0x0800, 0x08ff, MWA_RAM, spriteram, spriteram_size ),
		new Memory_WriteAddress( 0x1008, 0x1008, vb_scrollx_hi_w ),
		new Memory_WriteAddress( 0x1009, 0x1009, vb_bankswitch_w ),
		new Memory_WriteAddress( 0x100a, 0x100a, MWA_RAM ),
		new Memory_WriteAddress( 0x100b, 0x100b, MWA_RAM ),
		new Memory_WriteAddress( 0x100c, 0x100c, MWA_RAM, vb_scrollx_lo ),
		new Memory_WriteAddress( 0x100d, 0x100d, cpu_sound_command_w ),
		new Memory_WriteAddress( 0x100e, 0x100e, MWA_RAM ),
		new Memory_WriteAddress( 0x2000, 0x27ff, vb_foreground_w, vb_videoram ),
		new Memory_WriteAddress( 0x2800, 0x2fff, videoram_w, videoram ),
		new Memory_WriteAddress( 0x3000, 0x37ff, vb_fgattrib_w, vb_fgattribram ),
		new Memory_WriteAddress( 0x3800, 0x3fff, vb_attrib_w, vb_attribram ),
		new Memory_WriteAddress( 0x4000, 0xffff, MWA_ROM ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),
		new Memory_ReadAddress( 0x8801, 0x8801, YM2151_status_port_0_r ),
		new Memory_ReadAddress( 0x9800, 0x9800, OKIM6295_status_0_r ),
		new Memory_ReadAddress( 0xA000, 0xA000, soundlatch_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM ),
		new Memory_WriteAddress( 0x8800, 0x8800, YM2151_register_port_0_w ),
		new Memory_WriteAddress( 0x8801, 0x8801, YM2151_data_port_0_w ),
		new Memory_WriteAddress( 0x9800, 0x9800, OKIM6295_data_0_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_ReadAddress vball2pj_sound_readmem[]={
		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),
		new Memory_ReadAddress( 0x8801, 0x8801, YM2151_status_port_0_r ),
	//	new Memory_ReadAddress( 0x9800, 0x9800, dd_adpcm_status_r ),
		new Memory_ReadAddress( 0xA000, 0xA000, soundlatch_r ),
		new Memory_ReadAddress(MEMPORT_MARKER, 0)
	};
	
	public static Memory_WriteAddress vball2pj_sound_writemem[]={
		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM ),
		new Memory_WriteAddress( 0x8800, 0x8800, YM2151_register_port_0_w ),
		new Memory_WriteAddress( 0x8801, 0x8801, YM2151_data_port_0_w ),
	//	new Memory_WriteAddress( 0x9800, 0x9807, dd_adpcm_w ),
		new Memory_WriteAddress(MEMPORT_MARKER, 0)
	};
	
	public static void COMMON_PORTS_BEFORE(){
                PORT_START();  
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER1 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER1 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER1 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START1 );
		PORT_START();  
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START2 );
		PORT_START();  
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN1 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_COIN2 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_SERVICE1 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_VBLANK );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNUSED );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNUSED );
        }
	
	public static void COMMON_PORTS_COINS(){
                PORT_START();  
		PORT_DIPNAME( 0x07, 0x07, DEF_STR( "Coin_A") ); 
		PORT_DIPSETTING(    0x00, DEF_STR( "4C_1C") ); 
		PORT_DIPSETTING(    0x01, DEF_STR( "3C_1C") ); 
		PORT_DIPSETTING(    0x02, DEF_STR( "2C_1C") ); 
		PORT_DIPSETTING(    0x07, DEF_STR( "1C_1C") ); 
		PORT_DIPSETTING(    0x06, DEF_STR( "1C_2C") ); 
		PORT_DIPSETTING(    0x05, DEF_STR( "1C_3C") ); 
		PORT_DIPSETTING(    0x04, DEF_STR( "1C_4C") ); 
		PORT_DIPSETTING(    0x03, DEF_STR( "1C_5C") ); 
		PORT_DIPNAME( 0x38, 0x38, DEF_STR( "Coin_B") ); 
		PORT_DIPSETTING(    0x00, DEF_STR( "4C_1C") ); 
		PORT_DIPSETTING(    0x08, DEF_STR( "3C_1C") ); 
		PORT_DIPSETTING(    0x10, DEF_STR( "2C_1C") ); 
		PORT_DIPSETTING(    0x38, DEF_STR( "1C_1C") ); 
		PORT_DIPSETTING(    0x30, DEF_STR( "1C_2C") ); 
		PORT_DIPSETTING(    0x28, DEF_STR( "1C_3C") ); 
		PORT_DIPSETTING(    0x20, DEF_STR( "1C_4C") ); 
		PORT_DIPSETTING(    0x18, DEF_STR( "1C_5C") ); 
		PORT_DIPNAME( 0x40, 0x00, DEF_STR( "Flip_Screen") ); 
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") ); 
		PORT_DIPSETTING(    0x40, DEF_STR( "On") ); 
		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Demo_Sounds") ); 
		PORT_DIPSETTING(    0x00, DEF_STR( "Off") ); 
		PORT_DIPSETTING(    0x80, DEF_STR( "On") ); 
        }
	
        static InputPortPtr input_ports_vball = new InputPortPtr(){ 
            public void handler() {
		COMMON_PORTS_BEFORE();
		/* The dipswitch instructions in naz's dump (vball) don't quite sync here) */
		/* Looks like the pins from the dips to the board were mixed up a little. */
	
		PORT_START(); 
		PORT_DIPNAME( 0x03, 0x00, DEF_STR( "Difficulty") );
                // This ordering is assumed. Someone has to play it a lot and find out.
		PORT_DIPSETTING(    0x01, "Easy");
		PORT_DIPSETTING(    0x00, "Medium");
		PORT_DIPSETTING(    0x02, "Hard");
		PORT_DIPSETTING(    0x03, "Very Hard");
		PORT_DIPNAME( 0x0c, 0x00, "Single Player Game Time");
		PORT_DIPSETTING(    0x00, "1:15");
		PORT_DIPSETTING(    0x04, "1:30");
		PORT_DIPSETTING(    0x0c, "1:45");
		PORT_DIPSETTING(    0x08, "2:00");
		PORT_DIPNAME( 0x30, 0x00, "Start Buttons (4-player)");
		PORT_DIPSETTING(    0x00, "Normal");
		PORT_DIPSETTING(    0x20, "Button A");
		PORT_DIPSETTING(    0x10, "Button B");
		PORT_DIPSETTING(    0x30, "Normal");
		PORT_DIPNAME( 0x40, 0x40, "PL 1&4 (4-player)");
		PORT_DIPSETTING(    0x40, "Normal");
		PORT_DIPSETTING(    0x00, "Rot 90");
		PORT_DIPNAME( 0x80, 0x00, "Player Mode");
		PORT_DIPSETTING(    0x80, "2");
		PORT_DIPSETTING(    0x00, "4");
	
		COMMON_PORTS_COINS();
	
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER3 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER3 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER3 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START3 );
		PORT_START(); 
		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER4 );
		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER4 );
		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER4 );
		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_START4 );
	INPUT_PORTS_END(); }}; 
	
	static InputPortPtr input_ports_vball2pj = new InputPortPtr(){ public void handler() {
		COMMON_PORTS_BEFORE();
	
	/* The 2-player roms have the game-time in the difficulty spot, and
	   I've assumed vice-versa. (VS the instructions scanned in Naz's dump)
	*/
		PORT_START(); 
		PORT_DIPNAME( 0x03, 0x00, "Single Player Game Time");
		PORT_DIPSETTING(    0x00, "1:30");
		PORT_DIPSETTING(    0x01, "1:45");
		PORT_DIPSETTING(    0x03, "2:00");
		PORT_DIPSETTING(    0x02, "2:15");
		PORT_DIPNAME( 0x0c, 0x00, DEF_STR( "Difficulty") );
	// This ordering is assumed. Someone has to play it a lot and find out.
		PORT_DIPSETTING(    0x04, "Easy");
		PORT_DIPSETTING(    0x00, "Medium");
		PORT_DIPSETTING(    0x08, "Hard");
		PORT_DIPSETTING(    0x0c, "Very Hard");
	
		COMMON_PORTS_COINS();
	INPUT_PORTS_END(); }}; 
	
	
	static GfxLayout charlayout = new GfxLayout
	(
		8,8,
		RGN_FRAC(1,1),
		4,
		new int[] { 0, 2, 4, 6 },
		new int[] { 0*8*8+1, 0*8*8+0, 1*8*8+1, 1*8*8+0, 2*8*8+1, 2*8*8+0, 3*8*8+1, 3*8*8+0 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
		32*8
	);
	
	static GfxLayout spritelayout = new GfxLayout
	(
		16,16,
		RGN_FRAC(1,2),
		4,
		new int[] { RGN_FRAC(1,2)+0, RGN_FRAC(1,2)+4, 0, 4 },
		new int[] { 3, 2, 1, 0, 16*8+3, 16*8+2, 16*8+1, 16*8+0,
			  32*8+3, 32*8+2, 32*8+1, 32*8+0, 48*8+3, 48*8+2, 48*8+1, 48*8+0 },
		new int[] { 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8,
			  8*8, 9*8, 10*8, 11*8, 12*8, 13*8, 14*8, 15*8 },
		64*8
	);
	
	
	static GfxDecodeInfo vb_gfxdecodeinfo[] =
	{
		new GfxDecodeInfo( REGION_GFX1, 0, charlayout,     0, 8 ),	/* 8x8 chars */
		new GfxDecodeInfo( REGION_GFX2, 0, spritelayout, 128, 8 ),	/* 16x16 sprites */
		new GfxDecodeInfo( -1 ) /* end of array */
	};
	
	static WriteYmHandlerPtr vball_irq_handler = new WriteYmHandlerPtr() {
            public void handler(int irq) {
                cpu_set_irq_line( 1, ym_irq , irq!=0 ? ASSERT_LINE : CLEAR_LINE );
            }
        };
	
	static YM2151interface ym2151_interface = new YM2151interface
	(
		1,			/* 1 chip */
		3579545,	/* ??? */
		new int[]{ YM3012_VOL(60,MIXER_PAN_LEFT,60,MIXER_PAN_RIGHT) },
		new WriteYmHandlerPtr[]{ vball_irq_handler }
	);
	
	static OKIM6295interface okim6295_interface = new OKIM6295interface
        (
		1,              /* 1 chip */
		new int[]{ 6000 },           /* frequency (Hz) */
		new int[]{ REGION_SOUND1 },  /* memory region */
		new int[]{ 20000 }
	);
	
	static MSM5205interface msm5205_interface = new MSM5205interface
        (
		1,					/* 2 chips             */
		384000,				/* 384KHz             */
		new vclk_interruptPtr[]{ dd_adpcm_int },/* interrupt function */
		new int[]{ MSM5205_S48_4B },	/* 8kHz and 6kHz      */
		new int[]{ 40 }				/* volume */
	);
	
	public static InterruptPtr vball_interrupt = new InterruptPtr() { public int handler() 
	{
		int line = 33 - cpu_getiloops();
	
		if (line < 30)
		{
	//		scrollx[line] = lastscroll;
			return M6502_INT_IRQ;
		}
		else if (line == 30)	/* vblank */
			return M6502_INT_NMI;
		else 	/* skip 31 32 33 to allow vblank to finish */
			return ignore_interrupt.handler();
	} };
	
	
	static MachineDriver machine_driver_vball = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
	 			CPU_M6502,
				3579545,	/* 3.579545 MHz */
				readmem,writemem,null,null,
				vball_interrupt,34	/* 1 IRQ every 8 visible scanlines, plus NMI for vblank */
                        ),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				3579545,	/* 3.579545 MHz */
				sound_readmem,sound_writemem,null,null,
				ignore_interrupt,0
                        )
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
		1,	/* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		vb_init_machine,
	
		/* video hardware */
		32*8, 32*8,new rectangle( 0*8, 32*8-1, 0*8, 32*8-1 ),
		vb_gfxdecodeinfo,
		256, 0,
		null,
		VIDEO_TYPE_RASTER,
		null,
		vb_vh_start,
		vb_vh_stop,
		vb_vh_screenrefresh,
	
		/* sound hardware */
		SOUND_SUPPORTS_STEREO,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_YM2151,
				ym2151_interface
                        ),
                        /* This is here purely based on what the Z80 seems to be doing. And the fact that it works */
			new MachineSound(
				SOUND_OKIM6295,
				okim6295_interface
                        )
		}
	);
	
	static MachineDriver machine_driver_vball2pj = new MachineDriver
	(
		/* basic machine hardware */
		new MachineCPU[] {
			new MachineCPU(
	 			CPU_M6502,
				3579545,	/* 3.579545 MHz */
				vball2pj_readmem,writemem,null,null,
				vball_interrupt,34	/* 1 IRQ every 8 visible scanlines, plus NMI for vblank */
                        ),
			new MachineCPU(
				CPU_Z80 | CPU_AUDIO_CPU,
				3579545,	/* 3.579545 MHz */
				vball2pj_sound_readmem,vball2pj_sound_writemem,null,null,
				ignore_interrupt,0
                        )
		},
		60, DEFAULT_REAL_60HZ_VBLANK_DURATION, /* frames per second, vblank duration */
		1,	/* 1 CPU slice per frame - interleaving is forced when a sound command is written */
		vb_init_machine,
	
		/* video hardware */
		32*8, 32*8,new rectangle( 0*8, 32*8-1, 0*8, 32*8-1 ),
		vb_gfxdecodeinfo,
		256, 0,
		null,
		VIDEO_TYPE_RASTER,
		null,
		vb_vh_start,
		vb_vh_stop,
		vb_vh_screenrefresh,
	
		/* sound hardware */
		SOUND_SUPPORTS_STEREO,0,0,0,
		new MachineSound[] {
			new MachineSound(
				SOUND_YM2151,
				ym2151_interface
                        ),
			new MachineSound(
				SOUND_MSM5205,
				msm5205_interface
                       )
		}
	);
	
	
	/***************************************************************************
	
	  Game driver(s)
	
	***************************************************************************/
	
	static RomLoadPtr rom_vball = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x18000, REGION_CPU1, 0 );/* Main CPU: 64k for code */
		ROM_LOAD( "vball.124",  0x10000, 0x08000, 0xbe04c2b5 );/* Bankswitched */
		ROM_CONTINUE(		0x08000, 0x08000 );	 /* Static code  */
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* region#2: music CPU, 64kb */
		ROM_LOAD( "vball.47",  0x00000, 0x8000,  0x10ca79ad );
	
		/* These are from the bootleg; the original doesn't seem to have them?? */
		ROM_REGION(0x80000, REGION_GFX1, ROMREGION_DISPOSE ); /* fg tiles */
		ROM_LOAD( "vball13.bin",  0x00000, 0x10000, 0xf26df8e1 );/* 0,1,2,3 */
		ROM_LOAD( "vball14.bin",  0x10000, 0x10000, 0xc9798d0e );/* 0,1,2,3 */
		ROM_LOAD( "vball15.bin",  0x20000, 0x10000, 0x68e69c4b );/* 0,1,2,3 */
		ROM_LOAD( "vball16.bin",  0x30000, 0x10000, 0x936457ba );/* 0,1,2,3 */
		ROM_LOAD( "vball09.bin",  0x40000, 0x10000, 0x42874924 );/* 0,1,2,3 */
		ROM_LOAD( "vball10.bin",  0x50000, 0x10000, 0x6cc676ee );/* 0,1,2,3 */
		ROM_LOAD( "vball11.bin",  0x60000, 0x10000, 0x4754b303 );/* 0,1,2,3 */
		ROM_LOAD( "vball12.bin",  0x70000, 0x10000, 0x21294a84 );/* 0,1,2,3 */
	
		ROM_REGION(0x40000, REGION_GFX2, ROMREGION_DISPOSE ); /* sprites */
		ROM_LOAD( "vball.35",  0x00000, 0x20000, 0x877826d8 );/* 0,1,2,3 */
		ROM_LOAD( "vball.5",   0x20000, 0x20000, 0xc6afb4fa );/* 0,1,2,3 */
	
		ROM_REGION(0x20000, REGION_SOUND1, 0 );/* Sound region#1: adpcm */
		ROM_LOAD( "vball.78a",  0x00000, 0x10000, 0xf3e63b76 );
		ROM_LOAD( "vball.78b",  0x10000, 0x10000, 0x7ad9d338 );
	
		ROM_REGION(0x1000, REGION_PROMS, 0 );/* color PROMs */
		ROM_LOAD_NIB_LOW ( "vball.44",   0x0000, 0x00800, 0xa317240f );
		ROM_LOAD_NIB_HIGH( "vball.43",   0x0000, 0x00800, 0x1ff70b4f );
		ROM_LOAD( "vball.160",  0x0800, 0x00800, 0x2ffb68b3 );
	ROM_END(); }}; 
	
	static RomLoadPtr rom_vball2pj = new RomLoadPtr(){ public void handler(){ 
		ROM_REGION( 0x18000, REGION_CPU1, 0 );/* Main CPU */
		ROM_LOAD( "vball01.bin",  0x10000, 0x08000,  0x432509c4 );/* Bankswitched */
		ROM_CONTINUE(		  0x08000, 0x08000 );	 /* Static code  */
	
		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* region#2: music CPU, 64kb */
		ROM_LOAD( "vball04.bin",  0x00000, 0x8000,  0x534dfbd9 );
	
		ROM_REGION(0x80000, REGION_GFX1, ROMREGION_DISPOSE ); /* fg tiles */
		ROM_LOAD( "vball13.bin",  0x00000, 0x10000, 0xf26df8e1 );/* 0,1,2,3 */
		ROM_LOAD( "vball14.bin",  0x10000, 0x10000, 0xc9798d0e );/* 0,1,2,3 */
		ROM_LOAD( "vball15.bin",  0x20000, 0x10000, 0x68e69c4b );/* 0,1,2,3 */
		ROM_LOAD( "vball16.bin",  0x30000, 0x10000, 0x936457ba );/* 0,1,2,3 */
		ROM_LOAD( "vball09.bin",  0x40000, 0x10000, 0x42874924 );/* 0,1,2,3 */
		ROM_LOAD( "vball10.bin",  0x50000, 0x10000, 0x6cc676ee );/* 0,1,2,3 */
		ROM_LOAD( "vball11.bin",  0x60000, 0x10000, 0x4754b303 );/* 0,1,2,3 */
		ROM_LOAD( "vball12.bin",  0x70000, 0x10000, 0x21294a84 );/* 0,1,2,3 */
	
		ROM_REGION(0x40000, REGION_GFX2, ROMREGION_DISPOSE ); /* sprites */
		ROM_LOAD( "vball08.bin",  0x00000, 0x10000, 0xb18d083c );/* 0,1,2,3 */
		ROM_LOAD( "vball07.bin",  0x10000, 0x10000, 0x79a35321 );/* 0,1,2,3 */
		ROM_LOAD( "vball06.bin",  0x20000, 0x10000, 0x49c6aad7 );/* 0,1,2,3 */
		ROM_LOAD( "vball05.bin",  0x30000, 0x10000, 0x9bb95651 );/* 0,1,2,3 */
	
		ROM_REGION(0x20000, REGION_SOUND1, 0 );/* Sound region#1: adpcm */
		ROM_LOAD( "vball.78a",  0x00000, 0x10000, 0xf3e63b76 );
		ROM_LOAD( "vball.78b",  0x10000, 0x10000, 0x7ad9d338 );
	
		ROM_REGION(0x1000, REGION_PROMS, 0 );/* color PROMs */
		ROM_LOAD_NIB_LOW ( "vball.44",   0x0000, 0x00800, 0xa317240f );
		ROM_LOAD_NIB_HIGH( "vball.43",   0x0000, 0x00800, 0x1ff70b4f );
		ROM_LOAD( "vball.160",  0x0800, 0x00800, 0x2ffb68b3 );
	ROM_END(); }}; 
	
	
	public static GameDriver driver_vball	   = new GameDriver("1988"	,"vball"	,"vball.java"	,rom_vball,null	,machine_driver_vball	,input_ports_vball	,null	,ROT0	,	"Technos", "U.S. Championship V'ball (set 1)", GAME_NO_COCKTAIL );
	public static GameDriver driver_vball2pj	   = new GameDriver("1988"	,"vball2pj"	,"vball.java"	,rom_vball2pj,driver_vball	,machine_driver_vball2pj	,input_ports_vball2pj	,null	,ROT0	,	"Technos", "U.S. Championship V'ball (Japan bootleg)", GAME_IMPERFECT_SOUND | GAME_NO_COCKTAIL );
}
