/*
	Hal21 (sound not working, missing color proms, possibly bad tile gfx ROMs)
	ASO (seems fine)
	Alpha Mission ('p3.6d' is a bad dump)

	todo:
	- hal21 sound (2xAY8192)
	- hal21 gfx
	- hal21 colors
	- sound cpu status needs hooked up in both games
	- virtualize palette (background palette is bank selected) for further speedup
*/
/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.drivers;

import static arcadeflex056.fucPtr.*;
import common.ptr.UBytePtr;

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

import static mame056.vidhrdw.generic.*;

import static arcadeflex056.fileio.*;
import static mame056.palette.*;

import static mame056.vidhrdw.snk.*;

// refactor
import static arcadeflex036.osdepend.logerror;

import static arcadeflex056.fucPtr.*;
import common.ptr.UBytePtr;

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

import static mame056.vidhrdw.generic.*;

import static arcadeflex056.fileio.*;
import static mame056.palette.game_palette;
import static mame056.palette.palette_RRRR_GGGG_BBBB_convert_prom;

import static mame056.vidhrdw.snk.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class hal21
{
	
/*TODO*///	
/*TODO*///	
/*TODO*///	static int scrollx_base; /* this is the only difference in video hardware found so far */
/*TODO*///	
/*TODO*///	public static VhStartPtr common_vh_start = new VhStartPtr() { public int handler() {
/*TODO*///		dirtybuffer = new char[ 64*64 ];
/*TODO*///		if( dirtybuffer != null ){
/*TODO*///			tmpbitmap = bitmap_alloc( 512, 512 );
/*TODO*///			if( tmpbitmap != null){
/*TODO*///				memset( dirtybuffer, 1, 64*64  );
/*TODO*///				return 0;
/*TODO*///			}
/*TODO*///			dirtybuffer = null;
/*TODO*///		}
/*TODO*///		return 1;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static VhStartPtr aso_vh_start = new VhStartPtr() { public int handler() {
/*TODO*///		scrollx_base = -16;
/*TODO*///		return common_vh_start();
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static VhStartPtr hal21_vh_start = new VhStartPtr() { public int handler() {
/*TODO*///		scrollx_base = 240;
/*TODO*///		return common_vh_start();
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static VhStopPtr aso_vh_stop = new VhStopPtr() { public void handler() {
/*TODO*///		bitmap_free( tmpbitmap );
/*TODO*///		free( dirtybuffer );
/*TODO*///	} };
	
	
	public static VhConvertColorPromPtr aso_vh_convert_color_prom = new VhConvertColorPromPtr() {
            public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
                int i;
		int num_colors = 1024;
                /* palette format is RRRG GGBB B??? the three unknown bits are used but */
                /* I'm not sure how, I'm currently using them as least significant bit but */
                /* that's most likely wrong. */
		for( i=0; i<num_colors; i++ ){
			int bit0=0,bit1,bit2,bit3,r,g,b;
	
			bit0 = (color_prom.read(i + 2*num_colors) >> 2) & 0x01;
			bit1 = (color_prom.read(i) >> 1) & 0x01;
			bit2 = (color_prom.read(i) >> 2) & 0x01;
			bit3 = (color_prom.read(i) >> 3) & 0x01;
			r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			bit0 = (color_prom.read(i + 2*num_colors) >> 1) & 0x01;
			bit1 = (color_prom.read(i + num_colors) >> 2) & 0x01;
			bit2 = (color_prom.read(i + num_colors) >> 3) & 0x01;
			bit3 = (color_prom.read(i) >> 0) & 0x01;
			g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			bit0 = (color_prom.read(i + 2*num_colors) >> 0) & 0x01;
			bit1 = (color_prom.read(i + 2*num_colors) >> 3) & 0x01;
			bit2 = (color_prom.read(i + num_colors) >> 0) & 0x01;
			bit3 = (color_prom.read(i + num_colors) >> 1) & 0x01;
			b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
	
			palette_set_color(i,r,g,b);
		}
            }
        };
		
/*TODO*///	static void aso_draw_background(
/*TODO*///			struct mame_bitmap *bitmap,
/*TODO*///			int scrollx, int scrolly,
/*TODO*///			int bank, int color,
/*TODO*///			const struct GfxElement *gfx )
/*TODO*///	{
/*TODO*///		const struct rectangle *clip = &Machine->visible_area;
/*TODO*///		int offs;
/*TODO*///	
/*TODO*///		static int old_bank, old_color;
/*TODO*///	
/*TODO*///		if( color!=old_color || bank!=old_bank ){
/*TODO*///			memset( dirtybuffer, 1, 64*64  );
/*TODO*///			old_bank = bank;
/*TODO*///			old_color = color;
/*TODO*///		}
/*TODO*///	
/*TODO*///		for( offs=0; offs<64*64; offs++ ){
/*TODO*///			if( dirtybuffer[offs] ){
/*TODO*///				int tile_number = videoram[offs]+bank*256;
/*TODO*///				int sy = (offs%64)*8;
/*TODO*///				int sx = (offs/64)*8;
/*TODO*///	
/*TODO*///				drawgfx( tmpbitmap,gfx,
/*TODO*///					tile_number,
/*TODO*///					color,
/*TODO*///					0,0, /* no flip */
/*TODO*///					sx,sy,
/*TODO*///					0,TRANSPARENCY_NONE,0);
/*TODO*///	
/*TODO*///				dirtybuffer[offs] = 0;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	
/*TODO*///		copyscrollbitmap(bitmap,tmpbitmap,
/*TODO*///			1,&scrollx,1,&scrolly,
/*TODO*///			clip,
/*TODO*///			TRANSPARENCY_NONE,0);
/*TODO*///	}
/*TODO*///	
/*TODO*///	void aso_draw_sprites(
/*TODO*///			struct mame_bitmap *bitmap,
/*TODO*///			int xscroll, int yscroll,
/*TODO*///			const struct GfxElement *gfx
/*TODO*///	){
/*TODO*///		const unsigned char *source = spriteram;
/*TODO*///		const unsigned char *finish = source+60*4;
/*TODO*///	
/*TODO*///		struct rectangle clip = Machine->visible_area;
/*TODO*///	
/*TODO*///		while( source<finish ){
/*TODO*///			int attributes = source[3]; /* YBBX.CCCC */
/*TODO*///			int tile_number = source[1];
/*TODO*///			int sy = source[0] + ((attributes&0x10)?256:0) - yscroll;
/*TODO*///			int sx = source[2] + ((attributes&0x80)?256:0) - xscroll;
/*TODO*///			int color = attributes&0xf;
/*TODO*///	
/*TODO*///			if( !(attributes&0x20) ) tile_number += 512;
/*TODO*///			if( attributes&0x40 ) tile_number += 256;
/*TODO*///	
/*TODO*///			drawgfx(bitmap,gfx,
/*TODO*///				tile_number,
/*TODO*///				color,
/*TODO*///				0,0,
/*TODO*///				(256-sx)&0x1ff,sy&0x1ff,
/*TODO*///				&clip,TRANSPARENCY_PEN,7);
/*TODO*///	
/*TODO*///			source+=4;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	int hal21_vreg[6];
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr hal21_vreg0_w = new WriteHandlerPtr() {public void handler(int offset, int data){ hal21_vreg[0] = data; } };
/*TODO*///	public static WriteHandlerPtr hal21_vreg1_w = new WriteHandlerPtr() {public void handler(int offset, int data){ hal21_vreg[1] = data; } };
/*TODO*///	public static WriteHandlerPtr hal21_vreg2_w = new WriteHandlerPtr() {public void handler(int offset, int data){ hal21_vreg[2] = data; } };
/*TODO*///	public static WriteHandlerPtr hal21_vreg3_w = new WriteHandlerPtr() {public void handler(int offset, int data){ hal21_vreg[3] = data; } };
/*TODO*///	public static WriteHandlerPtr hal21_vreg4_w = new WriteHandlerPtr() {public void handler(int offset, int data){ hal21_vreg[4] = data; } };
/*TODO*///	public static WriteHandlerPtr hal21_vreg5_w = new WriteHandlerPtr() {public void handler(int offset, int data){ hal21_vreg[5] = data; } };
/*TODO*///	
/*TODO*///	public static VhUpdatePtr aso_vh_screenrefresh = new VhUpdatePtr() { public void handler(mame_bitmap bitmap,int full_refresh) {
/*TODO*///		unsigned char *ram = memory_region(REGION_CPU1);
/*TODO*///		int attributes = hal21_vreg[1];
/*TODO*///		{
/*TODO*///			unsigned char bg_attrs = hal21_vreg[0];
/*TODO*///			int scrolly = -8+hal21_vreg[4]+((attributes&0x10)?256:0);
/*TODO*///			int scrollx = scrollx_base + hal21_vreg[5]+((attributes&0x02)?0:256);
/*TODO*///	
/*TODO*///			aso_draw_background( bitmap, -scrollx, -scrolly,
/*TODO*///				bg_attrs>>4, /* tile bank */
/*TODO*///				bg_attrs&0xf, /* color bank */
/*TODO*///				Machine->gfx[1]
/*TODO*///			);
/*TODO*///		}
/*TODO*///	
/*TODO*///		{
/*TODO*///			int scrollx = 0x1e + hal21_vreg[3] + ((attributes&0x01)?256:0);
/*TODO*///			int scrolly = -8+0x11+hal21_vreg[2] + ((attributes&0x08)?256:0);
/*TODO*///			aso_draw_sprites( bitmap, scrollx, scrolly, Machine->gfx[2] );
/*TODO*///		}
/*TODO*///	
/*TODO*///		{
/*TODO*///			int bank = (attributes&0x40)?1:0;
/*TODO*///			tnk3_draw_text( bitmap, bank, &ram[0xf800] );
/*TODO*///			tnk3_draw_status( bitmap, bank, &ram[0xfc00] );
/*TODO*///		}
/*TODO*///	/*
/*TODO*///		{
/*TODO*///			int i;
/*TODO*///			for( i=0; i<6; i++ ){
/*TODO*///				int data = hal21_vreg[i];
/*TODO*///				drawgfx( bitmap, Machine->uifont,
/*TODO*///					"0123456789abcdef"[data>>4],0,0,0,
/*TODO*///					0,i*16,
/*TODO*///					&Machine->visible_area,
/*TODO*///					TRANSPARENCY_NONE,0 );
/*TODO*///				drawgfx( bitmap, Machine->uifont,
/*TODO*///					"0123456789abcdef"[data&0xf],0,0,0,
/*TODO*///					8,i*16,
/*TODO*///					&Machine->visible_area,
/*TODO*///					TRANSPARENCY_NONE,0 );
/*TODO*///			}
/*TODO*///		}
/*TODO*///	*/
/*TODO*///	} };
/*TODO*///	
/*TODO*///	
/*TODO*///	static InputPortPtr input_ports_hal21 = new InputPortPtr(){ public void handler() { 
/*TODO*///		PORT_START(); 
/*TODO*///		PORT_BIT( 0x01, IP_ACTIVE_HIGH, IPT_COIN1 );
/*TODO*///		PORT_BIT( 0x02, IP_ACTIVE_HIGH, IPT_COIN2 );
/*TODO*///		PORT_BIT( 0x04, IP_ACTIVE_HIGH, IPT_COIN3 );
/*TODO*///		PORT_BIT( 0x08, IP_ACTIVE_LOW,	IPT_START1 );
/*TODO*///		PORT_BIT( 0x10, IP_ACTIVE_LOW,	IPT_START2 );
/*TODO*///		PORT_BIT( 0x20, IP_ACTIVE_HIGH,	IPT_UNKNOWN );/* sound CPU status */
/*TODO*///		PORT_BIT( 0x40, IP_ACTIVE_LOW,  IPT_BUTTON3 );
/*TODO*///		PORT_BIT( 0x80, IP_ACTIVE_LOW,  IPT_UNKNOWN );
/*TODO*///	
/*TODO*///		PORT_START();  /* P1 controls */
/*TODO*///		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY );
/*TODO*///		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY );
/*TODO*///		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY );
/*TODO*///		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
/*TODO*///		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 );
/*TODO*///		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 );
/*TODO*///		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
/*TODO*///	
/*TODO*///		PORT_START();  /* P2 controls */
/*TODO*///		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP | IPF_8WAY | IPF_COCKTAIL );
/*TODO*///		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN | IPF_8WAY | IPF_COCKTAIL );
/*TODO*///		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT | IPF_8WAY | IPF_COCKTAIL );
/*TODO*///		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_COCKTAIL );
/*TODO*///		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_COCKTAIL );
/*TODO*///		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_COCKTAIL );
/*TODO*///		PORT_BIT( 0xc0, IP_ACTIVE_LOW, IPT_UNUSED );
/*TODO*///	
/*TODO*///		PORT_START(); 	/* DSW1 */
/*TODO*///		PORT_DIPNAME( 0x01, 0x01, DEF_STR( "Unknown") ); /* unused */
/*TODO*///		PORT_DIPSETTING(    0x01, DEF_STR( "Off") );
/*TODO*///		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
/*TODO*///		PORT_DIPNAME( 0x02, 0x02, DEF_STR( "Unknown") ); /* ? */
/*TODO*///		PORT_DIPSETTING(    0x02, DEF_STR( "Off") );
/*TODO*///		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
/*TODO*///		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Lives") );
/*TODO*///		PORT_DIPSETTING(    0x04, "3" );
/*TODO*///		PORT_DIPSETTING(    0x00, "5" );
/*TODO*///		PORT_DIPNAME( 0x38, 0x38, DEF_STR( "Coinage") );
/*TODO*///		PORT_DIPSETTING(    0x20, DEF_STR( "3C_1C") );
/*TODO*///		PORT_DIPSETTING(    0x18, DEF_STR( "2C_1C") );
/*TODO*///		PORT_DIPSETTING(    0x38, DEF_STR( "1C_1C") );
/*TODO*///		PORT_DIPSETTING(    0x30, DEF_STR( "1C_2C") );
/*TODO*///		PORT_DIPSETTING(    0x28, DEF_STR( "1C_3C") );
/*TODO*///		PORT_DIPSETTING(    0x00, DEF_STR( "Free_Play") );
/*TODO*///		PORT_DIPNAME( 0xc0, 0xc0, DEF_STR( "Bonus_Life") );
/*TODO*///		PORT_DIPSETTING(    0xc0, "20000 60000" );
/*TODO*///		PORT_DIPSETTING(    0x80, "40000 90000" );
/*TODO*///		PORT_DIPSETTING(	0x40, "50000 120000" );
/*TODO*///		PORT_DIPSETTING(    0x00, "None" );
/*TODO*///	
/*TODO*///		PORT_START(); 	/* DSW2 */
/*TODO*///		PORT_DIPNAME( 0x01, 0x01, "Bonus Type" );
/*TODO*///		PORT_DIPSETTING(    0x00, "Every Bonus Set" );
/*TODO*///		PORT_DIPSETTING(    0x01, "Second Bonus Set" );
/*TODO*///		PORT_DIPNAME( 0x06, 0x06, DEF_STR( "Difficulty") );
/*TODO*///		PORT_DIPSETTING(    0x00, "Easy" );
/*TODO*///		PORT_DIPSETTING(    0x02, "2" );
/*TODO*///		PORT_DIPSETTING(    0x04, "3" );
/*TODO*///		PORT_DIPSETTING(    0x06, "4" );
/*TODO*///		PORT_DIPNAME( 0x18, 0x18, "Special" );
/*TODO*///		PORT_DIPSETTING(    0x18, "Normal" );
/*TODO*///		PORT_DIPSETTING(    0x10, DEF_STR( "Demo_Sounds"));
/*TODO*///		PORT_DIPSETTING(    0x08, "Infinite Lives" );
/*TODO*///		PORT_DIPSETTING(    0x00, "Freeze" );
/*TODO*///		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Flip_Screen") ); // 0x20 -> fe65
/*TODO*///		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
/*TODO*///		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
/*TODO*///		PORT_DIPNAME( 0x40, 0x40, DEF_STR( "Unknown") ); // unused
/*TODO*///		PORT_DIPSETTING(    0x40, DEF_STR( "Off") );
/*TODO*///		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
/*TODO*///		PORT_DIPNAME( 0x80, 0x80, DEF_STR( "Unknown") ); /* ? */
/*TODO*///		PORT_DIPSETTING(    0x80, DEF_STR( "Off") );
/*TODO*///		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
/*TODO*///	INPUT_PORTS_END(); }}; 
/*TODO*///	
/*TODO*///	/**************************************************************************/
/*TODO*///	
/*TODO*///	static InputPortPtr input_ports_aso = new InputPortPtr(){ public void handler() { 
/*TODO*///		PORT_START(); 
/*TODO*///		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_COIN2 );
/*TODO*///		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_COIN1 );
/*TODO*///		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_START1 );
/*TODO*///		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_START2 );
/*TODO*///		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_VBLANK ); /* ? */
/*TODO*///		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN  );
/*TODO*///		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	
/*TODO*///		PORT_START(); 
/*TODO*///		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY );
/*TODO*///		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY );
/*TODO*///		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY );
/*TODO*///		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY );
/*TODO*///		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON3 );
/*TODO*///		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 );
/*TODO*///		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON2 );
/*TODO*///		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	
/*TODO*///		PORT_START(); 
/*TODO*///		PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_JOYSTICK_UP    | IPF_8WAY | IPF_PLAYER2 );
/*TODO*///		PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_JOYSTICK_DOWN  | IPF_8WAY | IPF_PLAYER2 );
/*TODO*///		PORT_BIT( 0x04, IP_ACTIVE_LOW, IPT_JOYSTICK_LEFT  | IPF_8WAY | IPF_PLAYER2 );
/*TODO*///		PORT_BIT( 0x08, IP_ACTIVE_LOW, IPT_JOYSTICK_RIGHT | IPF_8WAY | IPF_PLAYER2 );
/*TODO*///		PORT_BIT( 0x10, IP_ACTIVE_LOW, IPT_BUTTON3 | IPF_PLAYER2 );
/*TODO*///		PORT_BIT( 0x20, IP_ACTIVE_LOW, IPT_BUTTON1 | IPF_PLAYER2 );
/*TODO*///		PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_BUTTON2 | IPF_PLAYER2 );
/*TODO*///		PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
/*TODO*///	
/*TODO*///		PORT_START(); 
/*TODO*///		PORT_DIPNAME( 0x01, 0x01, "Allow Continue" );
/*TODO*///		PORT_DIPSETTING(    0x01, DEF_STR( "No") );
/*TODO*///		PORT_DIPSETTING(    0x00, DEF_STR( "Yes") );
/*TODO*///		PORT_DIPNAME( 0x02, 0x00, DEF_STR( "Cabinet") );
/*TODO*///		PORT_DIPSETTING(    0x00, DEF_STR( "Upright") );
/*TODO*///		PORT_DIPSETTING(    0x02, DEF_STR( "Cocktail") );
/*TODO*///		PORT_DIPNAME( 0x04, 0x04, DEF_STR( "Lives") );
/*TODO*///		PORT_DIPSETTING(    0x04, "3" );
/*TODO*///		PORT_DIPSETTING(    0x00, "5" );
/*TODO*///		PORT_DIPNAME( 0x38, 0x38, DEF_STR( "Coinage") );
/*TODO*///		PORT_DIPSETTING(    0x20, DEF_STR( "4C_1C"));
/*TODO*///		PORT_DIPSETTING(    0x28, DEF_STR( "3C_1C"));
/*TODO*///		PORT_DIPSETTING(    0x30, DEF_STR( "2C_1C"));
/*TODO*///		PORT_DIPSETTING(    0x38, DEF_STR( "1C_1C"));
/*TODO*///		PORT_DIPSETTING(    0x18, DEF_STR( "1C_2C"));
/*TODO*///		PORT_DIPSETTING(    0x10, DEF_STR( "1C_3C"));
/*TODO*///		PORT_DIPSETTING(    0x08, DEF_STR( "1C_4C"));
/*TODO*///		PORT_DIPSETTING(    0x00, DEF_STR( "1C_6C"));
/*TODO*///		PORT_DIPNAME( 0xc0, 0xc0, DEF_STR( "Bonus_Life") );
/*TODO*///		PORT_DIPSETTING(    0xc0, "50k 100k" );
/*TODO*///		PORT_DIPSETTING(    0x80, "60k 120k" );
/*TODO*///		PORT_DIPSETTING(    0x40, "100k 200k" );
/*TODO*///		PORT_DIPSETTING(    0x00, "None" );
/*TODO*///	
/*TODO*///		PORT_START(); 
/*TODO*///		PORT_DIPNAME( 0x01, 0x01, "Bonus Occurrence" );
/*TODO*///		PORT_DIPSETTING(    0x01, "1st & every 2nd" );
/*TODO*///		PORT_DIPSETTING(    0x00, "1st & 2nd only" );
/*TODO*///		PORT_DIPNAME( 0x06, 0x06, DEF_STR( "Difficulty") );
/*TODO*///		PORT_DIPSETTING(    0x06, "Easy" );
/*TODO*///		PORT_DIPSETTING(    0x04, "Normal" );
/*TODO*///		PORT_DIPSETTING(    0x02, "Hard" );
/*TODO*///		PORT_DIPSETTING(    0x00, "Hardest" );
/*TODO*///		PORT_DIPNAME( 0x08, 0x00, DEF_STR( "Demo_Sounds") );
/*TODO*///		PORT_DIPSETTING(    0x08, DEF_STR( "Off") );
/*TODO*///		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
/*TODO*///		PORT_BITX( 0x10,    0x10, IPT_DIPSWITCH_NAME | IPF_CHEAT, "Cheat of some kind", IP_KEY_NONE, IP_JOY_NONE );
/*TODO*///		PORT_DIPSETTING(    0x10, DEF_STR( "Off") );
/*TODO*///		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
/*TODO*///		PORT_DIPNAME( 0x20, 0x20, DEF_STR( "Flip_Screen") );
/*TODO*///		PORT_DIPSETTING(    0x20, DEF_STR( "Off") );
/*TODO*///		PORT_DIPSETTING(    0x00, DEF_STR( "On") );
/*TODO*///		PORT_DIPNAME( 0xc0, 0xc0, "Start Area" );
/*TODO*///		PORT_DIPSETTING(    0xc0, "1" );
/*TODO*///		PORT_DIPSETTING(    0x80, "2" );
/*TODO*///		PORT_DIPSETTING(    0x40, "3" );
/*TODO*///		PORT_DIPSETTING(    0x00, "4" );
/*TODO*///	INPUT_PORTS_END(); }}; 
/*TODO*///	
/*TODO*///	
/*TODO*///	/**************************************************************************/
/*TODO*///	
/*TODO*///	static GfxLayout char256 = new GfxLayout(
/*TODO*///		8,8,
/*TODO*///		0x100,
/*TODO*///		4,
/*TODO*///		new int[] { 0, 1, 2, 3 },
/*TODO*///		new int[] { 4, 0, 12, 8, 20, 16, 28, 24},
/*TODO*///		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32 },
/*TODO*///		256
/*TODO*///	);
/*TODO*///	
/*TODO*///	static GfxLayout char1024 = new GfxLayout(
/*TODO*///		8,8,
/*TODO*///		0x400,
/*TODO*///		4,
/*TODO*///		new int[] { 0, 1, 2, 3 },
/*TODO*///		new int[] { 4, 0, 12, 8, 20, 16, 28, 24},
/*TODO*///		new int[] { 0*32, 1*32, 2*32, 3*32, 4*32, 5*32, 6*32, 7*32 },
/*TODO*///		256
/*TODO*///	);
/*TODO*///	
/*TODO*///	static GfxLayout sprite1024 = new GfxLayout(
/*TODO*///		16,16,
/*TODO*///		0x400,
/*TODO*///		3,
/*TODO*///		new int[] { 2*1024*256,1*1024*256,0*1024*256 },
/*TODO*///		new int[] {
/*TODO*///			7,6,5,4,3,2,1,0,
/*TODO*///			15,14,13,12,11,10,9,8
/*TODO*///		},
/*TODO*///		new int[] {
/*TODO*///			0*16, 1*16, 2*16, 3*16, 4*16, 5*16, 6*16, 7*16,
/*TODO*///			8*16, 9*16, 10*16, 11*16, 12*16, 13*16, 14*16, 15*16
/*TODO*///		},
/*TODO*///		256
/*TODO*///	);
/*TODO*///	
/*TODO*///	static GfxDecodeInfo aso_gfxdecodeinfo[] =
/*TODO*///	{
/*TODO*///		/* colors 512-1023 are currently unused, I think they are a second bank */
/*TODO*///		new GfxDecodeInfo( REGION_GFX1, 0, char256,    128*3,  8 ),	/* colors 384..511 */
/*TODO*///		new GfxDecodeInfo( REGION_GFX2, 0, char1024,   128*1, 16 ),	/* colors 128..383 */
/*TODO*///		new GfxDecodeInfo( REGION_GFX3, 0, sprite1024, 128*0, 16 ),	/* colors   0..127 */
/*TODO*///		new GfxDecodeInfo( -1 )
/*TODO*///	};
/*TODO*///	
/*TODO*///	/**************************************************************************/
/*TODO*///	
/*TODO*///	#define SNK_NMI_ENABLE	1
/*TODO*///	#define SNK_NMI_PENDING	2
/*TODO*///	
/*TODO*///	static int snk_soundcommand = 0;
/*TODO*///	static unsigned char *shared_ram, *shared_auxram;
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr shared_auxram_r  = new ReadHandlerPtr() { public int handler(int offset){ return shared_auxram[offset]; } };
/*TODO*///	public static WriteHandlerPtr shared_auxram_w = new WriteHandlerPtr() {public void handler(int offset, int data){ shared_auxram[offset] = data; } };
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr shared_ram_r  = new ReadHandlerPtr() { public int handler(int offset){ return shared_ram[offset]; } };
/*TODO*///	public static WriteHandlerPtr shared_ram_w = new WriteHandlerPtr() {public void handler(int offset, int data){ shared_ram[offset] = data; } };
/*TODO*///	
/*TODO*///	static int CPUA_latch = 0;
/*TODO*///	static int CPUB_latch = 0;
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr CPUA_int_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data){
/*TODO*///		if( CPUA_latch & SNK_NMI_PENDING ){
/*TODO*///			cpu_cause_interrupt( 0, Z80_NMI_INT );
/*TODO*///			CPUA_latch = 0;
/*TODO*///		}
/*TODO*///		else {
/*TODO*///			CPUA_latch |= SNK_NMI_ENABLE;
/*TODO*///		}
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr CPUA_int_trigger_r  = new ReadHandlerPtr() { public int handler(int offset){
/*TODO*///		if( CPUA_latch&SNK_NMI_ENABLE ){
/*TODO*///			cpu_cause_interrupt( 0, Z80_NMI_INT );
/*TODO*///			CPUA_latch = 0;
/*TODO*///		}
/*TODO*///		else {
/*TODO*///			CPUA_latch |= SNK_NMI_PENDING;
/*TODO*///		}
/*TODO*///		return 0xff;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr CPUB_int_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data){
/*TODO*///		if( CPUB_latch & SNK_NMI_PENDING ){
/*TODO*///			cpu_cause_interrupt( 1, Z80_NMI_INT );
/*TODO*///			CPUB_latch = 0;
/*TODO*///		}
/*TODO*///		else {
/*TODO*///			CPUB_latch |= SNK_NMI_ENABLE;
/*TODO*///		}
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr CPUB_int_trigger_r  = new ReadHandlerPtr() { public int handler(int offset){
/*TODO*///		if( CPUB_latch&SNK_NMI_ENABLE ){
/*TODO*///			cpu_cause_interrupt( 1, Z80_NMI_INT );
/*TODO*///			CPUB_latch = 0;
/*TODO*///		}
/*TODO*///		else {
/*TODO*///			CPUB_latch |= SNK_NMI_PENDING;
/*TODO*///		}
/*TODO*///		return 0xff;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static WriteHandlerPtr snk_soundcommand_w = new WriteHandlerPtr() {public void handler(int offset, int data){
/*TODO*///		snk_soundcommand = data;
/*TODO*///		cpu_cause_interrupt( 2, Z80_IRQ_INT );
/*TODO*///	//	cpu_cause_interrupt(2, 0xff); old ASO
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr snk_soundcommand_r  = new ReadHandlerPtr() { public int handler(int offset)
/*TODO*///	{
/*TODO*///		int val = snk_soundcommand;
/*TODO*///		snk_soundcommand = 0;
/*TODO*///		return val;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	/**************************************************************************/
/*TODO*///	
/*TODO*///	static struct YM3526interface ym3526_interface ={
/*TODO*///		1,			/* number of chips */
/*TODO*///		4000000,	/* 4 MHz? (hand tuned) */
/*TODO*///		{ 50 }		/* (not supported) */
/*TODO*///	};
/*TODO*///	
/*TODO*///	public static Memory_ReadAddress aso_readmem_sound[]={
/*TODO*///		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
/*TODO*///		new Memory_ReadAddress( 0x0000, 0xbfff, MRA_ROM ),
/*TODO*///		new Memory_ReadAddress( 0xc000, 0xc7ff, MRA_RAM ),
/*TODO*///		new Memory_ReadAddress( 0xd000, 0xd000, snk_soundcommand_r ),
/*TODO*///		new Memory_ReadAddress( 0xf000, 0xf000, YM3526_status_port_0_r ),
/*TODO*///		new Memory_ReadAddress(MEMPORT_MARKER, 0)
/*TODO*///	};
/*TODO*///	
/*TODO*///	public static Memory_WriteAddress aso_writemem_sound[]={
/*TODO*///		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
/*TODO*///		new Memory_WriteAddress( 0x0000, 0xbfff, MWA_ROM ),
/*TODO*///		new Memory_WriteAddress( 0xc000, 0xc7ff, MWA_RAM ),
/*TODO*///		new Memory_WriteAddress( 0xf000, 0xf000, YM3526_control_port_0_w ), /* YM3526 #1 control port? */
/*TODO*///		new Memory_WriteAddress( 0xf001, 0xf001, YM3526_write_port_0_w ),   /* YM3526 #1 write port?  */
/*TODO*///		new Memory_WriteAddress(MEMPORT_MARKER, 0)
/*TODO*///	};
/*TODO*///	
/*TODO*///	/**************************************************************************/
/*TODO*///	
/*TODO*///	static AY8910interface ay8910_interface = new AY8910interface(
/*TODO*///		2, /* number of chips */
/*TODO*///		2000000, /* 2 MHz */
/*TODO*///		new int[] { 35,35 },
/*TODO*///		new ReadHandlerPtr[] { 0 },
/*TODO*///		new ReadHandlerPtr[] { 0 },
/*TODO*///		new WriteHandlerPtr[] { 0 },
/*TODO*///		new WriteHandlerPtr[] { 0 }
/*TODO*///	);
/*TODO*///	
/*TODO*///	public static Memory_ReadAddress hal21_readmem_sound[]={
/*TODO*///		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
/*TODO*///		new Memory_ReadAddress( 0x0000, 0x3fff, MRA_ROM ),
/*TODO*///		new Memory_ReadAddress( 0x8000, 0x87ff, MRA_RAM ),
/*TODO*///		new Memory_ReadAddress( 0xa000, 0xa000, snk_soundcommand_r ),
/*TODO*///	//	new Memory_ReadAddress( 0xc000, 0xc000, ack ),
/*TODO*///		new Memory_ReadAddress(MEMPORT_MARKER, 0)
/*TODO*///	};
/*TODO*///	
/*TODO*///	public static Memory_WriteAddress hal21_writemem_sound[]={
/*TODO*///		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
/*TODO*///		new Memory_WriteAddress( 0x0000, 0x3fff, MWA_ROM ),
/*TODO*///		new Memory_WriteAddress( 0x8000, 0x87ff, MWA_RAM ),
/*TODO*///		new Memory_WriteAddress( 0xe000, 0xe000, AY8910_control_port_0_w ),
/*TODO*///		new Memory_WriteAddress( 0xe001, 0xe001, AY8910_write_port_0_w ),
/*TODO*///		new Memory_WriteAddress( 0xe008, 0xe008, AY8910_control_port_1_w ),
/*TODO*///		new Memory_WriteAddress( 0xe009, 0xe009, AY8910_write_port_1_w ),
/*TODO*///		new Memory_WriteAddress(MEMPORT_MARKER, 0)
/*TODO*///	};
/*TODO*///	
/*TODO*///	/**************************** ASO/Alpha Mission *************************/
/*TODO*///	
/*TODO*///	public static Memory_ReadAddress aso_readmem_cpuA[]={
/*TODO*///		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
/*TODO*///		new Memory_ReadAddress( 0x0000, 0xbfff, MRA_ROM ),
/*TODO*///		new Memory_ReadAddress( 0xc000, 0xc000, input_port_0_r ),	/* coin, start */
/*TODO*///		new Memory_ReadAddress( 0xc100, 0xc100, input_port_1_r ),	/* P1 */
/*TODO*///		new Memory_ReadAddress( 0xc200, 0xc200, input_port_2_r ),	/* P2 */
/*TODO*///		new Memory_ReadAddress( 0xc500, 0xc500, input_port_3_r ),	/* DSW1 */
/*TODO*///		new Memory_ReadAddress( 0xc600, 0xc600, input_port_4_r ),	/* DSW2 */
/*TODO*///		new Memory_ReadAddress( 0xc700, 0xc700, CPUB_int_trigger_r ),
/*TODO*///		new Memory_ReadAddress( 0xd000, 0xffff, MRA_RAM ),
/*TODO*///		new Memory_ReadAddress(MEMPORT_MARKER, 0)
/*TODO*///	};
/*TODO*///	
/*TODO*///	public static Memory_WriteAddress aso_writemem_cpuA[]={
/*TODO*///		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
/*TODO*///		new Memory_WriteAddress( 0x0000, 0xbfff, MWA_ROM ),
/*TODO*///		new Memory_WriteAddress( 0xc400, 0xc400, snk_soundcommand_w ),
/*TODO*///		new Memory_WriteAddress( 0xc700, 0xc700, CPUA_int_enable_w ),
/*TODO*///		new Memory_WriteAddress( 0xc800, 0xc800, hal21_vreg1_w ),
/*TODO*///		new Memory_WriteAddress( 0xc900, 0xc900, hal21_vreg2_w ),
/*TODO*///		new Memory_WriteAddress( 0xca00, 0xca00, hal21_vreg3_w ),
/*TODO*///		new Memory_WriteAddress( 0xcb00, 0xcb00, hal21_vreg4_w ),
/*TODO*///		new Memory_WriteAddress( 0xcc00, 0xcc00, hal21_vreg5_w ),
/*TODO*///		new Memory_WriteAddress( 0xcf00, 0xcf00, hal21_vreg0_w ),
/*TODO*///		new Memory_WriteAddress( 0xd800, 0xdfff, MWA_RAM, shared_auxram ),
/*TODO*///		new Memory_WriteAddress( 0xe000, 0xe7ff, MWA_RAM, spriteram ),
/*TODO*///		new Memory_WriteAddress( 0xe800, 0xf7ff, videoram_w, videoram ),
/*TODO*///		new Memory_WriteAddress( 0xf800, 0xffff, MWA_RAM, shared_ram ),
/*TODO*///		new Memory_WriteAddress(MEMPORT_MARKER, 0)
/*TODO*///	};
/*TODO*///	
/*TODO*///	public static Memory_ReadAddress aso_readmem_cpuB[]={
/*TODO*///		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
/*TODO*///		new Memory_ReadAddress( 0x0000, 0xbfff, MRA_ROM ),
/*TODO*///		new Memory_ReadAddress( 0xc000, 0xc000, CPUA_int_trigger_r ),
/*TODO*///		new Memory_ReadAddress( 0xc800, 0xe7ff, shared_auxram_r ),
/*TODO*///		new Memory_ReadAddress( 0xe800, 0xf7ff, MRA_RAM ),
/*TODO*///		new Memory_ReadAddress( 0xf800, 0xffff, shared_ram_r ),
/*TODO*///		new Memory_ReadAddress(MEMPORT_MARKER, 0)
/*TODO*///	};
/*TODO*///	public static Memory_WriteAddress aso_writemem_cpuB[]={
/*TODO*///		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
/*TODO*///		new Memory_WriteAddress( 0x0000, 0xbfff, MWA_ROM ),
/*TODO*///		new Memory_WriteAddress( 0xc000, 0xc000, CPUB_int_enable_w ),
/*TODO*///		new Memory_WriteAddress( 0xc800, 0xd7ff, shared_auxram_w ),
/*TODO*///		new Memory_WriteAddress( 0xd800, 0xe7ff, videoram_w ),
/*TODO*///		new Memory_WriteAddress( 0xe800, 0xf7ff, MWA_RAM ),
/*TODO*///		new Memory_WriteAddress( 0xf800, 0xffff, shared_ram_w ),
/*TODO*///		new Memory_WriteAddress(MEMPORT_MARKER, 0)
/*TODO*///	};
/*TODO*///	
/*TODO*///	/**************************** HAL21 *************************/
/*TODO*///	
/*TODO*///	public static Memory_ReadAddress hal21_readmem_CPUA[]={
/*TODO*///		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
/*TODO*///		new Memory_ReadAddress( 0x0000, 0x7fff, MRA_ROM ),
/*TODO*///		new Memory_ReadAddress( 0xc000, 0xc000, input_port_0_r ),	/* coin, start */
/*TODO*///		new Memory_ReadAddress( 0xc100, 0xc100, input_port_1_r ),	/* P1 */
/*TODO*///		new Memory_ReadAddress( 0xc200, 0xc200, input_port_2_r ),	/* P2 */
/*TODO*///		new Memory_ReadAddress( 0xc400, 0xc400, input_port_3_r ),	/* DSW1 */
/*TODO*///		new Memory_ReadAddress( 0xc500, 0xc500, input_port_4_r ),	/* DSW2 */
/*TODO*///		new Memory_ReadAddress( 0xc700, 0xc700, CPUB_int_trigger_r ),
/*TODO*///		new Memory_ReadAddress( 0xe000, 0xefff, MRA_RAM ),
/*TODO*///		new Memory_ReadAddress( 0xf000, 0xffff, MRA_RAM ),
/*TODO*///		new Memory_ReadAddress(MEMPORT_MARKER, 0)
/*TODO*///	};
/*TODO*///	
/*TODO*///	public static Memory_WriteAddress hal21_writemem_CPUA[]={
/*TODO*///		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
/*TODO*///		new Memory_WriteAddress( 0x0000, 0x7fff, MWA_ROM ),
/*TODO*///		new Memory_WriteAddress( 0xc300, 0xc300, snk_soundcommand_w ),
/*TODO*///		new Memory_WriteAddress( 0xc600, 0xc600, hal21_vreg0_w ),
/*TODO*///		new Memory_WriteAddress( 0xc700, 0xc700, CPUA_int_enable_w ),
/*TODO*///		new Memory_WriteAddress( 0xd300, 0xd300, hal21_vreg1_w ),
/*TODO*///		new Memory_WriteAddress( 0xd400, 0xd400, hal21_vreg2_w ),
/*TODO*///		new Memory_WriteAddress( 0xd500, 0xd500, hal21_vreg3_w ),
/*TODO*///		new Memory_WriteAddress( 0xd600, 0xd600, hal21_vreg4_w ),
/*TODO*///		new Memory_WriteAddress( 0xd700, 0xd700, hal21_vreg5_w ),
/*TODO*///		new Memory_WriteAddress( 0xe000, 0xefff, MWA_RAM, spriteram ),
/*TODO*///		new Memory_WriteAddress( 0xf000, 0xffff, MWA_RAM, shared_ram ),
/*TODO*///		new Memory_WriteAddress(MEMPORT_MARKER, 0)
/*TODO*///	};
/*TODO*///	
/*TODO*///	public static ReadHandlerPtr hal21_spriteram_r  = new ReadHandlerPtr() { public int handler(int offset){
/*TODO*///		return spriteram[offset];
/*TODO*///	} };
/*TODO*///	public static WriteHandlerPtr hal21_spriteram_w = new WriteHandlerPtr() {public void handler(int offset, int data){
/*TODO*///		spriteram[offset] = data;
/*TODO*///	} };
/*TODO*///	
/*TODO*///	public static Memory_ReadAddress hal21_readmem_CPUB[]={
/*TODO*///		new Memory_ReadAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_READ | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
/*TODO*///		new Memory_ReadAddress( 0x0000, 0x9fff, MRA_ROM ),
/*TODO*///		new Memory_ReadAddress( 0xc000, 0xcfff, hal21_spriteram_r ),
/*TODO*///		new Memory_ReadAddress( 0xd000, 0xdfff, MRA_RAM ), /* background */
/*TODO*///		new Memory_ReadAddress( 0xe000, 0xefff, shared_ram_r ),
/*TODO*///		new Memory_ReadAddress(MEMPORT_MARKER, 0)
/*TODO*///	};
/*TODO*///	
/*TODO*///	public static Memory_WriteAddress hal21_writemem_CPUB[]={
/*TODO*///		new Memory_WriteAddress(MEMPORT_MARKER, MEMPORT_DIRECTION_WRITE | MEMPORT_TYPE_MEM | MEMPORT_WIDTH_8),
/*TODO*///		new Memory_WriteAddress( 0x0000, 0x9fff, MWA_ROM ),
/*TODO*///		new Memory_WriteAddress( 0xa000, 0xa000, CPUB_int_enable_w ),
/*TODO*///		new Memory_WriteAddress( 0xc000, 0xcfff, hal21_spriteram_w ),
/*TODO*///		new Memory_WriteAddress( 0xd000, 0xdfff, videoram_w, videoram ),
/*TODO*///		new Memory_WriteAddress( 0xe000, 0xefff, shared_ram_w ),
/*TODO*///		new Memory_WriteAddress(MEMPORT_MARKER, 0)
/*TODO*///	};
/*TODO*///	
/*TODO*///	/**************************************************************************/
/*TODO*///	
/*TODO*///	static MachineDriver machine_driver_aso = new MachineDriver
/*TODO*///	(
/*TODO*///		new MachineCPU[] {
/*TODO*///			new MachineCPU(
/*TODO*///				CPU_Z80,
/*TODO*///				4000000, /* ? */
/*TODO*///				aso_readmem_cpuA,aso_writemem_cpuA,null,null,
/*TODO*///				interrupt,1
/*TODO*///			),
/*TODO*///			new MachineCPU(
/*TODO*///				CPU_Z80,
/*TODO*///				4000000, /* ? */
/*TODO*///				aso_readmem_cpuB,aso_writemem_cpuB,null,null,
/*TODO*///				interrupt,1
/*TODO*///			),
/*TODO*///			new MachineCPU(
/*TODO*///				CPU_Z80 | CPU_AUDIO_CPU,
/*TODO*///				4000000,	/* 4 MHz (?) */
/*TODO*///				aso_readmem_sound,aso_writemem_sound,null,null,
/*TODO*///				interrupt,1
/*TODO*///			),
/*TODO*///		},
/*TODO*///		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,
/*TODO*///		100,	/* CPU slices per frame */
/*TODO*///		null, /* init machine */
/*TODO*///	
/*TODO*///		/* video hardware */
/*TODO*///		36*8, 28*8, new rectangle( 0*8, 36*8-1, 1*8, 28*8-1 ),
/*TODO*///	
/*TODO*///		aso_gfxdecodeinfo,
/*TODO*///		1024, null,
/*TODO*///		aso_vh_convert_color_prom,
/*TODO*///	
/*TODO*///		VIDEO_TYPE_RASTER,
/*TODO*///		null,
/*TODO*///		aso_vh_start,
/*TODO*///		aso_vh_stop,
/*TODO*///		aso_vh_screenrefresh,
/*TODO*///	
/*TODO*///		/* sound hardware */
/*TODO*///		0,0,0,0,
/*TODO*///		new MachineSound[] {
/*TODO*///		    new MachineSound(
/*TODO*///		       SOUND_YM3526,
/*TODO*///		       ym3526_interface
/*TODO*///		    )
/*TODO*///		}
/*TODO*///	);
/*TODO*///	
/*TODO*///	static MachineDriver machine_driver_hal21 = new MachineDriver(
/*TODO*///		new MachineCPU[] {
/*TODO*///			new MachineCPU(
/*TODO*///				CPU_Z80,
/*TODO*///				3360000,	/* 3.336 MHz? */
/*TODO*///				hal21_readmem_CPUA,hal21_writemem_CPUA,null,null,
/*TODO*///				interrupt,1
/*TODO*///			),
/*TODO*///			new MachineCPU(
/*TODO*///				CPU_Z80,
/*TODO*///				3360000,	/* 3.336 MHz? */
/*TODO*///				hal21_readmem_CPUB,hal21_writemem_CPUB,null,null,
/*TODO*///				interrupt,1
/*TODO*///			),
/*TODO*///			new MachineCPU(
/*TODO*///				CPU_Z80 | CPU_AUDIO_CPU,
/*TODO*///				4000000,	/* 4 MHz (?) */
/*TODO*///				hal21_readmem_sound,hal21_writemem_sound,null,null,
/*TODO*///				interrupt,1
/*TODO*///			),
/*TODO*///		},
/*TODO*///		60, DEFAULT_REAL_60HZ_VBLANK_DURATION,
/*TODO*///		100,	/* CPU slices per frame */
/*TODO*///		null, /* init_machine */
/*TODO*///	
/*TODO*///		/* video hardware */
/*TODO*///		36*8, 28*8, new rectangle( 0*8, 36*8-1, 1*8, 28*8-1 ),
/*TODO*///		aso_gfxdecodeinfo,
/*TODO*///		1024, null,
/*TODO*///		aso_vh_convert_color_prom,
/*TODO*///	
/*TODO*///		VIDEO_TYPE_RASTER,
/*TODO*///		null,
/*TODO*///		hal21_vh_start,
/*TODO*///		aso_vh_stop,
/*TODO*///		aso_vh_screenrefresh,
/*TODO*///	
/*TODO*///		/* sound hardware */
/*TODO*///		0,0,0,0,
/*TODO*///		new MachineSound[] {
/*TODO*///		    new MachineSound(
/*TODO*///		       SOUND_AY8910,
/*TODO*///		       ay8910_interface
/*TODO*///		    )
/*TODO*///		}
/*TODO*///	);
/*TODO*///	
/*TODO*///	/**************************************************************************/
/*TODO*///	
/*TODO*///	static RomLoadPtr rom_hal21 = new RomLoadPtr(){ public void handler(){ 
/*TODO*///		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for CPUA code */
/*TODO*///		ROM_LOAD( "hal21p1.bin",    0x0000, 0x2000, 0x9d193830 );
/*TODO*///		ROM_LOAD( "hal21p2.bin",    0x2000, 0x2000, 0xc1f00350 );
/*TODO*///		ROM_LOAD( "hal21p3.bin",    0x4000, 0x2000, 0x881d22a6 );
/*TODO*///		ROM_LOAD( "hal21p4.bin",    0x6000, 0x2000, 0xce692534 );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for CPUB code */
/*TODO*///		ROM_LOAD( "hal21p5.bin",    0x0000, 0x2000, 0x3ce0684a );
/*TODO*///		ROM_LOAD( "hal21p6.bin",    0x2000, 0x2000, 0x878ef798 );
/*TODO*///		ROM_LOAD( "hal21p7.bin",    0x4000, 0x2000, 0x72ebbe95 );
/*TODO*///		ROM_LOAD( "hal21p8.bin",    0x6000, 0x2000, 0x17e22ad3 );
/*TODO*///		ROM_LOAD( "hal21p9.bin",    0x8000, 0x2000, 0xb146f891 );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* 64k for sound code */
/*TODO*///		ROM_LOAD( "hal21p10.bin",   0x0000, 0x4000, 0x916f7ba0 );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );
/*TODO*///		ROM_LOAD( "hal21p12.bin", 0x0000, 0x2000, 0x9839a7cd );/* char */
/*TODO*///	
/*TODO*///		ROM_REGION( 0x8000, REGION_GFX2, ROMREGION_DISPOSE );/* background tiles */
/*TODO*///		ROM_LOAD( "hal21p11.bin", 0x0000, 0x4000, 0x24abc57e );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x18000, REGION_GFX3, ROMREGION_DISPOSE );/* 16x16 sprites */
/*TODO*///		ROM_LOAD( "hal21p13.bin", 0x00000, 0x4000, 0x052b4f4f );
/*TODO*///		ROM_RELOAD(               0x04000, 0x4000 );
/*TODO*///		ROM_LOAD( "hal21p14.bin", 0x08000, 0x4000, 0xda0cb670 );
/*TODO*///		ROM_RELOAD(               0x0c000, 0x4000 );
/*TODO*///		ROM_LOAD( "hal21p15.bin", 0x10000, 0x4000, 0x5c5ea945 );
/*TODO*///		ROM_RELOAD(               0x14000, 0x4000 );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x0c00, REGION_PROMS, 0 );
/*TODO*///		ROM_LOAD( "hal21_1.prm",  0x000, 0x400, 0x195768fc );
/*TODO*///		ROM_LOAD( "hal21_2.prm",  0x400, 0x400, 0xc5d84225 );
/*TODO*///		ROM_LOAD( "hal21_3.prm",  0x800, 0x400, 0x605afff8 );
/*TODO*///	ROM_END(); }}; 
/*TODO*///	
/*TODO*///	static RomLoadPtr rom_hal21j = new RomLoadPtr(){ public void handler(){ 
/*TODO*///		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for CPUA code */
/*TODO*///		ROM_LOAD( "hal21p1.bin",    0x0000, 0x2000, 0x9d193830 );
/*TODO*///		ROM_LOAD( "hal21p2.bin",    0x2000, 0x2000, 0xc1f00350 );
/*TODO*///		ROM_LOAD( "hal21p3.bin",    0x4000, 0x2000, 0x881d22a6 );
/*TODO*///		ROM_LOAD( "hal21p4.bin",    0x6000, 0x2000, 0xce692534 );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for CPUB code */
/*TODO*///		ROM_LOAD( "hal21p5.bin",    0x0000, 0x2000, 0x3ce0684a );
/*TODO*///		ROM_LOAD( "hal21p6.bin",    0x2000, 0x2000, 0x878ef798 );
/*TODO*///		ROM_LOAD( "hal21p7.bin",    0x4000, 0x2000, 0x72ebbe95 );
/*TODO*///		ROM_LOAD( "hal21p8.bin",    0x6000, 0x2000, 0x17e22ad3 );
/*TODO*///		ROM_LOAD( "hal21p9.bin",    0x8000, 0x2000, 0xb146f891 );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* 64k for sound code */
/*TODO*///		ROM_LOAD( "hal21-10.bin",   0x0000, 0x4000, 0xa182b3f0 );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );
/*TODO*///		ROM_LOAD( "hal21p12.bin", 0x0000, 0x2000, 0x9839a7cd );/* char */
/*TODO*///	
/*TODO*///		ROM_REGION( 0x8000, REGION_GFX2, ROMREGION_DISPOSE );/* background tiles */
/*TODO*///		ROM_LOAD( "hal21p11.bin", 0x0000, 0x4000, 0x24abc57e );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x18000, REGION_GFX3, ROMREGION_DISPOSE );/* 16x16 sprites */
/*TODO*///		ROM_LOAD( "hal21p13.bin", 0x00000, 0x4000, 0x052b4f4f );
/*TODO*///		ROM_RELOAD(               0x04000, 0x4000 );
/*TODO*///		ROM_LOAD( "hal21p14.bin", 0x08000, 0x4000, 0xda0cb670 );
/*TODO*///		ROM_RELOAD(               0x0c000, 0x4000 );
/*TODO*///		ROM_LOAD( "hal21p15.bin", 0x10000, 0x4000, 0x5c5ea945 );
/*TODO*///		ROM_RELOAD(               0x14000, 0x4000 );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x0c00, REGION_PROMS, 0 );
/*TODO*///		ROM_LOAD( "hal21_1.prm",  0x000, 0x400, 0x195768fc );
/*TODO*///		ROM_LOAD( "hal21_2.prm",  0x400, 0x400, 0xc5d84225 );
/*TODO*///		ROM_LOAD( "hal21_3.prm",  0x800, 0x400, 0x605afff8 );
/*TODO*///	ROM_END(); }}; 
/*TODO*///	
/*TODO*///	static RomLoadPtr rom_aso = new RomLoadPtr(){ public void handler(){ 
/*TODO*///		ROM_REGION( 0x10000, REGION_CPU1, 0 );/* 64k for cpuA code */
/*TODO*///		ROM_LOAD( "aso.1",    0x0000, 0x8000, 0x3fc9d5e4 );
/*TODO*///		ROM_LOAD( "aso.3",    0x8000, 0x4000, 0x39a666d2 );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x10000, REGION_CPU2, 0 );/* 64k for cpuB code */
/*TODO*///		ROM_LOAD( "aso.4",    0x0000, 0x8000, 0x2429792b );
/*TODO*///		ROM_LOAD( "aso.6",    0x8000, 0x4000, 0xc0bfdf1f );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x10000, REGION_CPU3, 0 );/* 64k for sound code */
/*TODO*///		ROM_LOAD( "aso.7",    0x0000, 0x8000, 0x49258162 ); /* YM3526 */
/*TODO*///		ROM_LOAD( "aso.9",    0x8000, 0x4000, 0xaef5a4f4 );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x2000, REGION_GFX1, ROMREGION_DISPOSE );/* characters */
/*TODO*///		ROM_LOAD( "aso.14",   0x0000, 0x2000, 0x8baa2253 );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x8000, REGION_GFX2, ROMREGION_DISPOSE );/* background tiles */
/*TODO*///		ROM_LOAD( "aso.10",   0x0000, 0x8000, 0x00dff996 );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x18000, REGION_GFX3, ROMREGION_DISPOSE );/* 16x16 sprites */
/*TODO*///		ROM_LOAD( "aso.11",   0x00000, 0x8000, 0x7feac86c );
/*TODO*///		ROM_LOAD( "aso.12",   0x08000, 0x8000, 0x6895990b );
/*TODO*///		ROM_LOAD( "aso.13",   0x10000, 0x8000, 0x87a81ce1 );
/*TODO*///	
/*TODO*///		ROM_REGION( 0x0c00, REGION_PROMS, 0 );
/*TODO*///		ROM_LOAD( "up02_f12.rom",  0x000, 0x00400, 0x5b0a0059 );
/*TODO*///		ROM_LOAD( "up02_f13.rom",  0x400, 0x00400, 0x37e28dd8 );
/*TODO*///		ROM_LOAD( "up02_f14.rom",  0x800, 0x00400, 0xc3fd1dd3 );
/*TODO*///	ROM_END(); }}; 
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	public static GameDriver driver_aso	   = new GameDriver("1985"	,"aso"	,"hal21.java"	,rom_aso,null	,machine_driver_aso	,input_ports_aso	,null	,ROT270	,	"SNK", "ASO - Armored Scrum Object", GAME_IMPERFECT_SOUND )
/*TODO*///	public static GameDriver driver_hal21	   = new GameDriver("1985"	,"hal21"	,"hal21.java"	,rom_hal21,null	,machine_driver_hal21	,input_ports_hal21	,null	,ROT270	,	"SNK", "HAL21", GAME_NO_SOUND | GAME_WRONG_COLORS )
/*TODO*///	public static GameDriver driver_hal21j	   = new GameDriver("1985"	,"hal21j"	,"hal21.java"	,rom_hal21j,driver_hal21	,machine_driver_hal21	,input_ports_hal21	,null	,ROT270	,	"SNK", "HAL21 (Japan)", GAME_NO_SOUND | GAME_WRONG_COLORS )
}
