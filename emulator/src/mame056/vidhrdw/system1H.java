/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.vidhrdw;

public class system1H
{
	
	public static int SPR_Y_TOP     = 0;
	public static int SPR_Y_BOTTOM	= 1;
	public static int SPR_X_LO      = 2;
	public static int SPR_X_HI      = 3;
	public static int SPR_SKIP_LO   = 4;
	public static int SPR_SKIP_HI   = 5;
	public static int SPR_GFXOFS_LO = 6;
	public static int SPR_GFXOFS_HI	= 7;
	
	public static int system1_BACKGROUND_MEMORY_SINGLE = 0;
	public static int system1_BACKGROUND_MEMORY_BANKED = 1;
	
	/*TODO*///extern unsigned char 	*system1_scroll_y;
	/*TODO*///extern unsigned char 	*system1_scroll_x;
	/*TODO*///extern unsigned char 	*system1_videoram;
	/*TODO*///extern unsigned char 	*system1_backgroundram;
	/*TODO*///extern unsigned char 	*system1_sprites_collisionram;
	/*TODO*///extern unsigned char 	*system1_background_collisionram;
	/*TODO*///extern unsigned char 	*system1_scrollx_ram;
	/*TODO*///extern size_t system1_videoram_size;
	/*TODO*///extern size_t system1_backgroundram_size;
	
	
	/*TODO*///int  system1_vh_start(void);
	/*TODO*///void system1_vh_stop(void);
	/*TODO*///void system1_define_background_memory(int Mode);
	
	/*TODO*///void system1_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
	/*TODO*///void system1_vh_convert_color_prom(unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom);
	
	/*TODO*///void choplifter_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
	/*TODO*///void wbml_vh_screenrefresh(struct mame_bitmap *bitmap,int full_refresh);
	
	/*TODO*///#endif
}
