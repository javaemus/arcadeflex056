/**
 * Ported to 0.56
 */
package mame056;

import static arcadeflex056.video.osd_allocate_colors;
import static arcadeflex056.video.osd_modify_pen;
import static common.subArrays.*;
import static mame056.commonH.REGION_PROMS;
import static mame056.common.memory_region;
import static mame056.usrintrf.usrintf_showmessage;
import static arcadeflex036.osdepend.logerror;
import static arcadeflex056.fucPtr.*;

import static common.ptr.*;
import static mame056.driverH.VIDEO_HAS_SHADOWS;
import static mame056.mame.Machine;

public class palette {

    /*TODO*///#define VERBOSE 0
    public static char[] game_palette;/* RGB palette as set by the driver */
    public static char[] actual_palette;/* actual RGB palette after brightness adjustments */
    static double[] brightness;
    /*TODO*///
    static int colormode;
    public static final int PALETTIZED_16BIT = 0;
    public static final int DIRECT_15BIT = 1;
    public static final int DIRECT_32BIT = 2;

    static int total_colors;
    static double shadow_factor, highlight_factor;
    static int palette_initialized;

    /*TODO*///UINT32 direct_rgb_components[3];
/*TODO*///
/*TODO*///
/*TODO*///
    public static char[] palette_shadow_table;
/*TODO*///
    public static int palette_start() {
/*TODO*///        	int i;
/*TODO*///
/*TODO*///	if ((Machine->drv->video_attributes & VIDEO_RGB_DIRECT) &&
/*TODO*///			Machine->drv->color_table_len)
/*TODO*///	{
/*TODO*///		logerror("Error: VIDEO_RGB_DIRECT requires color_table_len to be 0.\n");
/*TODO*///		return 1;
/*TODO*///	}
/*TODO*///
        total_colors = Machine.drv.total_colors;
        /*TODO*///	if (Machine->drv->video_attributes & VIDEO_HAS_SHADOWS)
/*TODO*///		total_colors += Machine->drv->total_colors;
/*TODO*///	if (Machine->drv->video_attributes & VIDEO_HAS_HIGHLIGHTS)
/*TODO*///		total_colors += Machine->drv->total_colors;
/*TODO*///	if (total_colors > 65536)
/*TODO*///	{
/*TODO*///		logerror("Error: palette has more than 65536 colors.\n");
/*TODO*///		return 1;
/*TODO*///	}
/*TODO*///	shadow_factor = PALETTE_DEFAULT_SHADOW_FACTOR;
/*TODO*///	highlight_factor = PALETTE_DEFAULT_HIGHLIGHT_FACTOR;
/*TODO*///
        game_palette = new char[3 * total_colors];
        actual_palette = new char[3 * total_colors];
        brightness = new double[Machine.drv.total_colors];
        /*TODO*///
/*TODO*///	if (Machine->color_depth == 15)
/*TODO*///		colormode = DIRECT_15BIT;
/*TODO*///	else if (Machine->color_depth == 32)
/*TODO*///		colormode = DIRECT_32BIT;
/*TODO*///	else
        colormode = PALETTIZED_16BIT;

        Machine.pens = new int[total_colors * 4];//malloc(total_colors * sizeof(*Machine->pens));

        if (Machine.drv.color_table_len != 0) {
            Machine.game_colortable = new char[Machine.drv.color_table_len * 4];//malloc(Machine.drv.color_table_len * sizeof(*Machine.game_colortable));
            Machine.remapped_colortable = new IntArray(Machine.drv.color_table_len * 4);//malloc(Machine.drv.color_table_len * sizeof(*Machine.remapped_colortable));
        } else {
            Machine.game_colortable = null;
            Machine.remapped_colortable = new IntArray(Machine.pens);/* straight 1:1 mapping from palette to colortable */
        }

        if (colormode == PALETTIZED_16BIT)
	{
		palette_shadow_table = new char[total_colors*2];
		if (palette_shadow_table == null)
		{
			palette_stop();
			return 1;
		}
		for (int i = 0;i < total_colors;i++)
		{
			palette_shadow_table[i] = (char)i;
			if (((Machine.drv.video_attributes & VIDEO_HAS_SHADOWS)!=0) && (i < Machine.drv.total_colors))
				palette_shadow_table[i] += Machine.drv.total_colors;
		}
	}
	else
		palette_shadow_table = null;

/*TODO*///	if ((Machine->drv->color_table_len && (Machine->game_colortable == 0 || Machine->remapped_colortable == 0))
/*TODO*///			|| game_palette == 0 || actual_palette == 0 || brightness == 0
/*TODO*///			|| Machine->pens == 0 || Machine->debug_pens == 0 || Machine->debug_remapped_colortable == 0)
/*TODO*///	{
/*TODO*///		palette_stop();
/*TODO*///		return 1;
/*TODO*///	}
        for (int i = 0; i < Machine.drv.total_colors; i++) {
            brightness[i] = 1.0;
        }

        /*TODO*///
/*TODO*///	state_save_register_UINT8("palette", 0, "colors", game_palette, total_colors*3);
/*TODO*///	state_save_register_UINT8("palette", 0, "actual_colors", actual_palette, total_colors*3);
/*TODO*///	state_save_register_double("palette", 0, "brightness", brightness, Machine->drv->total_colors);
/*TODO*///	switch (colormode)
/*TODO*///	{
/*TODO*///		case PALETTIZED_16BIT:
/*TODO*///			state_save_register_func_postload(palette_reset_16_palettized);
/*TODO*///			break;
/*TODO*///		case DIRECT_15BIT:
/*TODO*///			state_save_register_func_postload(palette_reset_15_direct);
/*TODO*///			break;
/*TODO*///		case DIRECT_32BIT:
/*TODO*///			state_save_register_func_postload(palette_reset_32_direct);
/*TODO*///			break;
/*TODO*///	}
/*TODO*///
        return 0;
    }

    public static void palette_stop() {
        /*TODO*///	free(game_palette);
/*TODO*///	game_palette = 0;
/*TODO*///	free(actual_palette);
/*TODO*///	actual_palette = 0;
/*TODO*///	free(brightness);
/*TODO*///	brightness = 0;
/*TODO*///	if (Machine->game_colortable)
/*TODO*///	{
/*TODO*///		free(Machine->game_colortable);
/*TODO*///		Machine->game_colortable = 0;
/*TODO*///		/* remapped_colortable is malloc()ed only when game_colortable is, */
/*TODO*///		/* otherwise it just points to Machine->pens */
/*TODO*///		free(Machine->remapped_colortable);
/*TODO*///	}
/*TODO*///	Machine->remapped_colortable = 0;
/*TODO*///	free(Machine->debug_remapped_colortable);
/*TODO*///	Machine->debug_remapped_colortable = 0;
/*TODO*///	free(Machine->pens);
/*TODO*///	Machine->pens = 0;
/*TODO*///	free(Machine->debug_pens);
/*TODO*///	Machine->debug_pens = 0;
/*TODO*///	free(palette_shadow_table);
/*TODO*///	palette_shadow_table = 0;
/*TODO*///
/*TODO*///	palette_initialized = 0;
    }

    public static int palette_init() {
        int i;

        /* We initialize the palette and colortable to some default values so that */
 /* drivers which dynamically change the palette don't need a vh_init_palette() */
 /* function (provided the default color table fits their needs). */
        for (i = 0; i < total_colors; i++) {
            game_palette[3 * i + 0] = actual_palette[3 * i + 0] = (char) (((i & 1) >> 0) * 0xff);
            game_palette[3 * i + 1] = actual_palette[3 * i + 1] = (char) (((i & 2) >> 1) * 0xff);
            game_palette[3 * i + 2] = actual_palette[3 * i + 2] = (char) (((i & 4) >> 2) * 0xff);
        }

        /* Preload the colortable with a default setting, following the same */
 /* order of the palette. The driver can overwrite this in */
 /* vh_init_palette() */
        for (i = 0; i < (Machine.drv.color_table_len * 4); i++) {
            Machine.game_colortable[i] = (char) (i % total_colors);
        }

        /* now the driver can modify the default values if it wants to. */
        if (Machine.drv.vh_init_palette != null) {
            if (memory_region(REGION_PROMS) != null)
                (Machine.drv.vh_init_palette).handler(game_palette, Machine.game_colortable, new UBytePtr(memory_region(REGION_PROMS)));
            else
                (Machine.drv.vh_init_palette).handler(game_palette, Machine.game_colortable, null);                
        }

        switch (colormode) {
            case PALETTIZED_16BIT: {
                if (osd_allocate_colors(total_colors, game_palette, null) != 0) {
                    return 1;
                }

                for (i = 0; i < total_colors; i++) {
                    Machine.pens[i] = i;
                }

                /* refresh the palette to support shadows in PROM games */
                for (i = 0; i < Machine.drv.total_colors; i++) {
                    palette_set_color(i, game_palette[3 * i + 0], game_palette[3 * i + 1], game_palette[3 * i + 2]);
                }
            }
            break;
            /*TODO*///
/*TODO*///		case DIRECT_15BIT:
/*TODO*///		{
/*TODO*///			const UINT8 rgbpalette[3*3] = { 0xff,0x00,0x00, 0x00,0xff,0x00, 0x00,0x00,0xff };
/*TODO*///
/*TODO*///			if (osd_allocate_colors(3,rgbpalette,direct_rgb_components,debug_palette,debug_pens))
/*TODO*///				return 1;
/*TODO*///
/*TODO*///			for (i = 0;i < total_colors;i++)
/*TODO*///				Machine->pens[i] =
/*TODO*///						(game_palette[3*i + 0] >> 3) * (direct_rgb_components[0] / 0x1f) +
/*TODO*///						(game_palette[3*i + 1] >> 3) * (direct_rgb_components[1] / 0x1f) +
/*TODO*///						(game_palette[3*i + 2] >> 3) * (direct_rgb_components[2] / 0x1f);
/*TODO*///
/*TODO*///			break;
/*TODO*///		}
/*TODO*///
/*TODO*///		case DIRECT_32BIT:
/*TODO*///		{
/*TODO*///			const UINT8 rgbpalette[3*3] = { 0xff,0x00,0x00, 0x00,0xff,0x00, 0x00,0x00,0xff };
/*TODO*///
/*TODO*///			if (osd_allocate_colors(3,rgbpalette,direct_rgb_components,debug_palette,debug_pens))
/*TODO*///				return 1;
/*TODO*///
/*TODO*///			for (i = 0;i < total_colors;i++)
/*TODO*///				Machine->pens[i] =
/*TODO*///						game_palette[3*i + 0] * (direct_rgb_components[0] / 0xff) +
/*TODO*///						game_palette[3*i + 1] * (direct_rgb_components[1] / 0xff) +
/*TODO*///						game_palette[3*i + 2] * (direct_rgb_components[2] / 0xff);
/*TODO*///
/*TODO*///			break;
/*TODO*///		}
        }

        for (i = 0; i < Machine.drv.color_table_len; i++) {
            int color = Machine.game_colortable[i];

            /* check for invalid colors set by Machine->drv->vh_init_palette */
            if (color < total_colors) {
                Machine.remapped_colortable.write(i, Machine.pens[color]);
            } else {
                usrintf_showmessage("colortable[%d] (=%d) out of range (total_colors = %d)",
                        i, color, total_colors);
            }
        }

        palette_initialized = 1;

        return 0;
    }

    /*TODO*///
/*TODO*///
/*TODO*///INLINE void palette_set_color_15_direct(int color,UINT8 red,UINT8 green,UINT8 blue)
/*TODO*///{
/*TODO*///	if (	actual_palette[3*color + 0] == red &&
/*TODO*///			actual_palette[3*color + 1] == green &&
/*TODO*///			actual_palette[3*color + 2] == blue)
/*TODO*///		return;
/*TODO*///	actual_palette[3*color + 0] = red;
/*TODO*///	actual_palette[3*color + 1] = green;
/*TODO*///	actual_palette[3*color + 2] = blue;
/*TODO*///	Machine->pens[color] =
/*TODO*///			(red   >> 3) * (direct_rgb_components[0] / 0x1f) +
/*TODO*///			(green >> 3) * (direct_rgb_components[1] / 0x1f) +
/*TODO*///			(blue  >> 3) * (direct_rgb_components[2] / 0x1f);
/*TODO*///}
/*TODO*///
/*TODO*///static void palette_reset_15_direct(void)
/*TODO*///{
/*TODO*///	int color;
/*TODO*///	for(color = 0; color < total_colors; color++)
/*TODO*///		Machine->pens[color] =
/*TODO*///				(game_palette[3*color + 0]>>3) * (direct_rgb_components[0] / 0x1f) +
/*TODO*///				(game_palette[3*color + 1]>>3) * (direct_rgb_components[1] / 0x1f) +
/*TODO*///				(game_palette[3*color + 2]>>3) * (direct_rgb_components[2] / 0x1f);
/*TODO*///}
/*TODO*///
/*TODO*///INLINE void palette_set_color_32_direct(int color,UINT8 red,UINT8 green,UINT8 blue)
/*TODO*///{
/*TODO*///	if (	actual_palette[3*color + 0] == red &&
/*TODO*///			actual_palette[3*color + 1] == green &&
/*TODO*///			actual_palette[3*color + 2] == blue)
/*TODO*///		return;
/*TODO*///	actual_palette[3*color + 0] = red;
/*TODO*///	actual_palette[3*color + 1] = green;
/*TODO*///	actual_palette[3*color + 2] = blue;
/*TODO*///	Machine->pens[color] =
/*TODO*///			red   * (direct_rgb_components[0] / 0xff) +
/*TODO*///			green * (direct_rgb_components[1] / 0xff) +
/*TODO*///			blue  * (direct_rgb_components[2] / 0xff);
/*TODO*///}
/*TODO*///
/*TODO*///static void palette_reset_32_direct(void)
/*TODO*///{
/*TODO*///	int color;
/*TODO*///	for(color = 0; color < total_colors; color++)
/*TODO*///		Machine->pens[color] =
/*TODO*///				game_palette[3*color + 0] * (direct_rgb_components[0] / 0xff) +
/*TODO*///				game_palette[3*color + 1] * (direct_rgb_components[1] / 0xff) +
/*TODO*///				game_palette[3*color + 2] * (direct_rgb_components[2] / 0xff);
/*TODO*///}
    public static void palette_set_color_16_palettized(int color, int/*UINT8*/ red, int/*UINT8*/ green, int/*UINT8*/ blue) {
        if (actual_palette[3 * color + 0] == red
                && actual_palette[3 * color + 1] == green
                && actual_palette[3 * color + 2] == blue) {
            return;
        }

        actual_palette[3 * color + 0] = (char) red;
        actual_palette[3 * color + 1] = (char) green;
        actual_palette[3 * color + 2] = (char) blue;

        if (palette_initialized != 0) {
            osd_modify_pen(Machine.pens[color], red, green, blue);
        }
    }

    /*TODO*///
/*TODO*///static void palette_reset_16_palettized(void)
/*TODO*///{
/*TODO*///	if (palette_initialized)
/*TODO*///	{
/*TODO*///		int color;
/*TODO*///		for (color=0; color<total_colors; color++)
/*TODO*///			osd_modify_pen(Machine->pens[color],
/*TODO*///						   game_palette[3*color + 0],
/*TODO*///						   game_palette[3*color + 1],
/*TODO*///						   game_palette[3*color + 2]);
/*TODO*///   }
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///INLINE void adjust_shadow(UINT8 *r,UINT8 *g,UINT8 *b,double factor)
/*TODO*///{
/*TODO*///	if (factor > 1)
/*TODO*///	{
/*TODO*///		int max = *r;
/*TODO*///		if (*g > max) max = *g;
/*TODO*///		if (*b > max) max = *b;
/*TODO*///
/*TODO*///		if ((int)(max * factor + 0.5) >= 256)
/*TODO*///			factor = 255.0 / max;
/*TODO*///	}
/*TODO*///
/*TODO*///	*r = *r * factor + 0.5;
/*TODO*///	*g = *g * factor + 0.5;
/*TODO*///	*b = *b * factor + 0.5;
/*TODO*///}
    public static void palette_set_color(int color, int/*UINT8*/ r, int/*UINT8*/ g, int/*UINT8*/ b) {
        if (color >= total_colors) {
            logerror("error: palette_set_color() called with color %d, but only %d allocated.\n", color, total_colors);
            return;
        }

        game_palette[3 * color + 0] = (char) (r & 0xFFFF);
        game_palette[3 * color + 1] = (char) (g & 0xFFFF);
        game_palette[3 * color + 2] = (char) (b & 0xFFFF);

        if (color < Machine.drv.total_colors && brightness[color] != 1.0) {
            r = (int) (r * brightness[color] + 0.5) & 0xFFFF;
            g = (int) (g * brightness[color] + 0.5) & 0xFFFF;
            b = (int) (b * brightness[color] + 0.5) & 0xFFFF;
        }

        switch (colormode) {
            case PALETTIZED_16BIT:
                palette_set_color_16_palettized(color, r, g, b);
                break;
            /*TODO*///		case DIRECT_15BIT:
/*TODO*///			palette_set_color_15_direct(color,r,g,b);
/*TODO*///			break;
/*TODO*///		case DIRECT_32BIT:
/*TODO*///			palette_set_color_32_direct(color,r,g,b);
/*TODO*///			break;
        }
        /*TODO*///
/*TODO*///	if (color < Machine->drv->total_colors)
/*TODO*///	{
/*TODO*///		/* automatically create darker shade for shadow handling */
/*TODO*///		if (Machine->drv->video_attributes & VIDEO_HAS_SHADOWS)
/*TODO*///		{
/*TODO*///			UINT8 nr=r,ng=g,nb=b;
/*TODO*///
/*TODO*///			adjust_shadow(&nr,&ng,&nb,shadow_factor);
/*TODO*///
/*TODO*///			color += Machine->drv->total_colors;	/* carry this change over to highlight handling */
/*TODO*///			palette_set_color(color,nr,ng,nb);
/*TODO*///		}
/*TODO*///
/*TODO*///		/* automatically create brighter shade for highlight handling */
/*TODO*///		if (Machine->drv->video_attributes & VIDEO_HAS_HIGHLIGHTS)
/*TODO*///		{
/*TODO*///			UINT8 nr=r,ng=g,nb=b;
/*TODO*///
/*TODO*///			adjust_shadow(&nr,&ng,&nb,highlight_factor);
/*TODO*///
/*TODO*///			color += Machine->drv->total_colors;
/*TODO*///			palette_set_color(color,nr,ng,nb);
/*TODO*///		}
/*TODO*///	}
    }
    /*TODO*///
/*TODO*///void palette_get_color(int color,UINT8 *r,UINT8 *g,UINT8 *b)
/*TODO*///{
/*TODO*///	*r = game_palette[3*color + 0];
/*TODO*///	*g = game_palette[3*color + 1];
/*TODO*///	*b = game_palette[3*color + 2];
/*TODO*///}
/*TODO*///
/*TODO*///void palette_set_brightness(int color,double bright)
/*TODO*///{
/*TODO*///	if (brightness[color] != bright)
/*TODO*///	{
/*TODO*///		brightness[color] = bright;
/*TODO*///
/*TODO*///		palette_set_color(color,game_palette[3*color + 0],game_palette[3*color + 1],game_palette[3*color + 2]);
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///void palette_set_shadow_factor(double factor)
/*TODO*///{
/*TODO*///	if (shadow_factor != factor)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///
/*TODO*///		shadow_factor = factor;
/*TODO*///
/*TODO*///		if (palette_initialized)
/*TODO*///		{
/*TODO*///			for (i = 0;i < Machine->drv->total_colors;i++)
/*TODO*///				palette_set_color(i,game_palette[3*i + 0],game_palette[3*i + 1],game_palette[3*i + 2]);
/*TODO*///		}
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///void palette_set_highlight_factor(double factor)
/*TODO*///{
/*TODO*///	if (highlight_factor != factor)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///
/*TODO*///		highlight_factor = factor;
/*TODO*///
/*TODO*///		for (i = 0;i < Machine->drv->total_colors;i++)
/*TODO*///			palette_set_color(i,game_palette[3*i + 0],game_palette[3*i + 1],game_palette[3*i + 2]);
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////******************************************************************************
/*TODO*///
/*TODO*/// Commonly used palette RAM handling functions
/*TODO*///
/*TODO*///******************************************************************************/
/*TODO*///
    public static UBytePtr paletteram = new UBytePtr();
    public static UBytePtr paletteram_2 = new UBytePtr();
    /* use when palette RAM is split in two parts */
    public static UBytePtr paletteram16 = new UBytePtr();
/*TODO*///data16_t *paletteram16_2;
/*TODO*///data32_t *paletteram32;

    public static ReadHandlerPtr paletteram_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return paletteram.read(offset);
        }
    };

    /*TODO*///
/*TODO*///READ_HANDLER( paletteram_2_r )
/*TODO*///{
/*TODO*///	return paletteram_2[offset];
/*TODO*///}
/*TODO*///
/*TODO*///READ16_HANDLER( paletteram16_word_r )
/*TODO*///{
/*TODO*///	return paletteram16[offset];
/*TODO*///}
/*TODO*///
/*TODO*///READ16_HANDLER( paletteram16_2_word_r )
/*TODO*///{
/*TODO*///	return paletteram16_2[offset];
/*TODO*///}
/*TODO*///
/*TODO*///READ32_HANDLER( paletteram32_r )
/*TODO*///{
/*TODO*///	return paletteram32[offset];
/*TODO*///}
/*TODO*///
/*TODO*///WRITE_HANDLER( paletteram_RRRGGGBB_w )
/*TODO*///{
/*TODO*///	int r,g,b;
/*TODO*///	int bit0,bit1,bit2;
/*TODO*///
/*TODO*///
/*TODO*///	paletteram[offset] = data;
/*TODO*///
/*TODO*///	/* red component */
/*TODO*///	bit0 = (data >> 5) & 0x01;
/*TODO*///	bit1 = (data >> 6) & 0x01;
/*TODO*///	bit2 = (data >> 7) & 0x01;
/*TODO*///	r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
/*TODO*///	/* green component */
/*TODO*///	bit0 = (data >> 2) & 0x01;
/*TODO*///	bit1 = (data >> 3) & 0x01;
/*TODO*///	bit2 = (data >> 4) & 0x01;
/*TODO*///	g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
/*TODO*///	/* blue component */
/*TODO*///	bit0 = 0;
/*TODO*///	bit1 = (data >> 0) & 0x01;
/*TODO*///	bit2 = (data >> 1) & 0x01;
/*TODO*///	b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
/*TODO*///
/*TODO*///	palette_set_color(offset,r,g,b);
/*TODO*///}
/*TODO*///
    public static WriteHandlerPtr paletteram_BBBGGGRR_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int r,g,b;
            int bit0,bit1,bit2;

            paletteram.write(offset, data);

            /* blue component */
            bit0 = (data >> 5) & 0x01;
            bit1 = (data >> 6) & 0x01;
            bit2 = (data >> 7) & 0x01;
            b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
            /* green component */
            bit0 = (data >> 2) & 0x01;
            bit1 = (data >> 3) & 0x01;
            bit2 = (data >> 4) & 0x01;
            g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
            /* blue component */
            bit0 = (data >> 0) & 0x01;
            bit1 = (data >> 1) & 0x01;
            r = 0x55 * bit0 + 0xaa * bit1;

            palette_set_color(offset,r,g,b);
        }
    };

    public static WriteHandlerPtr paletteram_BBGGGRRR_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int r, g, b;
            int bit0, bit1, bit2;

            paletteram.write(offset, data);

            /* red component */
            bit0 = (data >> 0) & 0x01;
            bit1 = (data >> 1) & 0x01;
            bit2 = (data >> 2) & 0x01;
            r = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
            /* green component */
            bit0 = (data >> 3) & 0x01;
            bit1 = (data >> 4) & 0x01;
            bit2 = (data >> 5) & 0x01;
            g = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;
            /* blue component */
            bit0 = 0;
            bit1 = (data >> 6) & 0x01;
            bit2 = (data >> 7) & 0x01;
            b = 0x21 * bit0 + 0x47 * bit1 + 0x97 * bit2;

            palette_set_color(offset, r, g, b);
        }
    };


    /*TODO*///WRITE_HANDLER( paletteram_IIBBGGRR_w )
/*TODO*///{
/*TODO*///	int r,g,b,i;
/*TODO*///
/*TODO*///
/*TODO*///	paletteram[offset] = data;
/*TODO*///
/*TODO*///	i = (data >> 6) & 0x03;
/*TODO*///	/* red component */
/*TODO*///	r = (data << 2) & 0x0c;
/*TODO*///	if (r) r |= i;
/*TODO*///	r *= 0x11;
/*TODO*///	/* green component */
/*TODO*///	g = (data >> 0) & 0x0c;
/*TODO*///	if (g) g |= i;
/*TODO*///	g *= 0x11;
/*TODO*///	/* blue component */
/*TODO*///	b = (data >> 2) & 0x0c;
/*TODO*///	if (b) b |= i;
/*TODO*///	b *= 0x11;
/*TODO*///
/*TODO*///	palette_set_color(offset,r,g,b);
/*TODO*///}


    public static WriteHandlerPtr paletteram_BBGGRRII_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int r,g,b,i;


            paletteram.write(offset, data);

            i = (data >> 0) & 0x03;
            /* red component */
            r = (((data >> 0) & 0x0c) | i) * 0x11;
            /* green component */
            g = (((data >> 2) & 0x0c) | i) * 0x11;
            /* blue component */
            b = (((data >> 4) & 0x0c) | i) * 0x11;

            palette_set_color(offset,r,g,b);
        }
    };
    
    public static void changecolor_xxxxBBBBGGGGRRRR(int color, int data) {
        int r, g, b;

        r = (data >> 0) & 0x0f;
        g = (data >> 4) & 0x0f;
        b = (data >> 8) & 0x0f;

        r = (r << 4) | r;
        g = (g << 4) | g;
        b = (b << 4) | b;

        palette_set_color(color, r, g, b);
    }

    public static WriteHandlerPtr paletteram_xxxxBBBBGGGGRRRR_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram.write(offset, data);
            changecolor_xxxxBBBBGGGGRRRR(offset / 2, paletteram.read(offset & ~1) | (paletteram.read(offset | 1) << 8));
        }
    };

    public static WriteHandlerPtr paletteram_xxxxBBBBGGGGRRRR_swap_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram.write(offset, data);
            changecolor_xxxxBBBBGGGGRRRR(offset / 2, paletteram.read(offset | 1) | (paletteram.read(offset & ~1) << 8));
        }
    };


    public static WriteHandlerPtr paletteram_xxxxBBBBGGGGRRRR_split1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram.write(offset, data);
            changecolor_xxxxBBBBGGGGRRRR(offset,paletteram.read(offset) | (paletteram_2.read(offset) << 8));
        }
    };

    public static WriteHandlerPtr paletteram_xxxxBBBBGGGGRRRR_split2_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram_2.write(offset, data);
            changecolor_xxxxBBBBGGGGRRRR(offset,paletteram.read(offset) | (paletteram_2.read(offset) << 8));
        }
    };

/*TODO*///WRITE16_HANDLER( paletteram16_xxxxBBBBGGGGRRRR_word_w )
/*TODO*///{
/*TODO*///	COMBINE_DATA(&paletteram16[offset]);
/*TODO*///	changecolor_xxxxBBBBGGGGRRRR(offset,paletteram16[offset]);
/*TODO*///}
    public static void changecolor_xxxxBBBBRRRRGGGG(int color, int data) {
        int r, g, b;

        r = (data >> 4) & 0x0f;
        g = (data >> 0) & 0x0f;
        b = (data >> 8) & 0x0f;

        r = (r << 4) | r;
        g = (g << 4) | g;
        b = (b << 4) | b;

        palette_set_color(color, r, g, b);
    }

    /*TODO*///WRITE_HANDLER( paletteram_xxxxBBBBRRRRGGGG_w )
/*TODO*///{
/*TODO*///	paletteram[offset] = data;
/*TODO*///	changecolor_xxxxBBBBRRRRGGGG(offset / 2,paletteram[offset & ~1] | (paletteram[offset | 1] << 8));
/*TODO*///}
/*TODO*///
    public static WriteHandlerPtr paletteram_xxxxBBBBRRRRGGGG_swap_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram.write(offset, data);
            changecolor_xxxxBBBBRRRRGGGG(offset / 2,paletteram.read(offset | 1) | (paletteram.read(offset & ~1) << 8));
        }
    };
        
    public static WriteHandlerPtr paletteram_xxxxBBBBRRRRGGGG_split1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram.write(offset, data);
            changecolor_xxxxBBBBRRRRGGGG(offset, paletteram.read(offset) | (paletteram_2.read(offset) << 8));
        }
    };

    public static WriteHandlerPtr paletteram_xxxxBBBBRRRRGGGG_split2_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram_2.write(offset, data);
            changecolor_xxxxBBBBRRRRGGGG(offset, paletteram.read(offset) | (paletteram_2.read(offset) << 8));
        }
    };

    public static void changecolor_xxxxRRRRBBBBGGGG(int color, int data) {
        int r, g, b;

        r = (data >> 8) & 0x0f;
        g = (data >> 0) & 0x0f;
        b = (data >> 4) & 0x0f;

        r = (r << 4) | r;
        g = (g << 4) | g;
        b = (b << 4) | b;

        palette_set_color(color, r, g, b);
    }

    public static WriteHandlerPtr paletteram_xxxxRRRRBBBBGGGG_split1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram.write(offset, data);
            changecolor_xxxxRRRRBBBBGGGG(offset, paletteram.read(offset) | (paletteram_2.read(offset) << 8));
        }
    };

    public static WriteHandlerPtr paletteram_xxxxRRRRBBBBGGGG_split2_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram_2.write(offset, data);
            changecolor_xxxxRRRRBBBBGGGG(offset, paletteram.read(offset) | (paletteram_2.read(offset) << 8));
        }
    };

    public static void changecolor_xxxxRRRRGGGGBBBB(int color, int data) {
        int r, g, b;

        r = (data >> 8) & 0x0f;
        g = (data >> 4) & 0x0f;
        b = (data >> 0) & 0x0f;

        r = (r << 4) | r;
        g = (g << 4) | g;
        b = (b << 4) | b;

        palette_set_color(color, r, g, b);
    }
    
    public static WriteHandlerPtr paletteram_xxxxRRRRGGGGBBBB_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram.write(offset, data);
            changecolor_xxxxRRRRGGGGBBBB(offset / 2,paletteram.read(offset & ~1) | (paletteram.read(offset | 1) << 8));
        }
    };
    
    public static WriteHandlerPtr paletteram_xxxxRRRRGGGGBBBB_swap_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram.write(offset, data);
            changecolor_xxxxRRRRGGGGBBBB(offset / 2, paletteram.read(offset | 1) | (paletteram.read(offset & ~1) << 8));

        }
    };

    /*TODO*///
/*TODO*///WRITE16_HANDLER( paletteram16_xxxxRRRRGGGGBBBB_word_w )
/*TODO*///{
/*TODO*///	COMBINE_DATA(&paletteram16[offset]);
/*TODO*///	changecolor_xxxxRRRRGGGGBBBB(offset,paletteram16[offset]);
/*TODO*///}
/*TODO*///

    public static void changecolor_RRRRGGGGBBBBxxxx(int color,int data)
    {
            int r,g,b;


            r = (data >> 12) & 0x0f;
            g = (data >>  8) & 0x0f;
            b = (data >>  4) & 0x0f;

            r = (r << 4) | r;
            g = (g << 4) | g;
            b = (b << 4) | b;

            palette_set_color(color,r,g,b);
    }

    public static WriteHandlerPtr paletteram_RRRRGGGGBBBBxxxx_swap_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram.write(offset, data);
            changecolor_RRRRGGGGBBBBxxxx(offset / 2,paletteram.read(offset | 1) | (paletteram.read(offset & ~1) << 8));
        }
    };

    public static WriteHandlerPtr paletteram_RRRRGGGGBBBBxxxx_split1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram.write(offset, data);
            changecolor_RRRRGGGGBBBBxxxx(offset,paletteram.read(offset) | (paletteram_2.read(offset) << 8));
        }
    };

    public static WriteHandlerPtr paletteram_RRRRGGGGBBBBxxxx_split2_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram_2.write(offset, data);
            changecolor_RRRRGGGGBBBBxxxx(offset,paletteram.read(offset) | (paletteram_2.read(offset) << 8));
        }
    };

/*TODO*///WRITE16_HANDLER( paletteram16_RRRRGGGGBBBBxxxx_word_w )
/*TODO*///{
/*TODO*///	COMBINE_DATA(&paletteram16[offset]);
/*TODO*///	changecolor_RRRRGGGGBBBBxxxx(offset,paletteram16[offset]);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///INLINE void changecolor_BBBBGGGGRRRRxxxx(int color,int data)
/*TODO*///{
/*TODO*///	int r,g,b;
/*TODO*///
/*TODO*///
/*TODO*///	r = (data >>  4) & 0x0f;
/*TODO*///	g = (data >>  8) & 0x0f;
/*TODO*///	b = (data >> 12) & 0x0f;
/*TODO*///
/*TODO*///	r = (r << 4) | r;
/*TODO*///	g = (g << 4) | g;
/*TODO*///	b = (b << 4) | b;
/*TODO*///
/*TODO*///	palette_set_color(color,r,g,b);
/*TODO*///}
/*TODO*///
/*TODO*///WRITE_HANDLER( paletteram_BBBBGGGGRRRRxxxx_swap_w )
/*TODO*///{
/*TODO*///	paletteram[offset] = data;
/*TODO*///	changecolor_BBBBGGGGRRRRxxxx(offset / 2,paletteram[offset | 1] | (paletteram[offset & ~1] << 8));
/*TODO*///}
/*TODO*///
/*TODO*///WRITE_HANDLER( paletteram_BBBBGGGGRRRRxxxx_split1_w )
/*TODO*///{
/*TODO*///	paletteram[offset] = data;
/*TODO*///	changecolor_BBBBGGGGRRRRxxxx(offset,paletteram[offset] | (paletteram_2[offset] << 8));
/*TODO*///}
/*TODO*///
/*TODO*///WRITE_HANDLER( paletteram_BBBBGGGGRRRRxxxx_split2_w )
/*TODO*///{
/*TODO*///	paletteram_2[offset] = data;
/*TODO*///	changecolor_BBBBGGGGRRRRxxxx(offset,paletteram[offset] | (paletteram_2[offset] << 8));
/*TODO*///}
/*TODO*///
/*TODO*///WRITE16_HANDLER( paletteram16_BBBBGGGGRRRRxxxx_word_w )
/*TODO*///{
/*TODO*///	COMBINE_DATA(&paletteram16[offset]);
/*TODO*///	changecolor_BBBBGGGGRRRRxxxx(offset,paletteram16[offset]);
/*TODO*///}

    public static void changecolor_xBBBBBGGGGGRRRRR(int color, int data) {
        int r, g, b;

        r = (data >> 0) & 0x1f;
        g = (data >> 5) & 0x1f;
        b = (data >> 10) & 0x1f;

        r = (r << 3) | (r >> 2);
        g = (g << 3) | (g >> 2);
        b = (b << 3) | (b >> 2);

        palette_set_color(color, r, g, b);
    }

    public static WriteHandlerPtr paletteram_xBBBBBGGGGGRRRRR_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram.write(offset, data);
            changecolor_xBBBBBGGGGGRRRRR(offset / 2, paletteram.read(offset & ~1) | (paletteram.read(offset | 1) << 8));
        }
    };


    public static WriteHandlerPtr paletteram_xBBBBBGGGGGRRRRR_swap_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram.write(offset, data);
            changecolor_xBBBBBGGGGGRRRRR(offset / 2,paletteram.read(offset | 1) | (paletteram.read(offset & ~1) << 8));
        }
    };

/*TODO*///WRITE16_HANDLER( paletteram16_xBBBBBGGGGGRRRRR_word_w )
/*TODO*///{
/*TODO*///	COMBINE_DATA(&paletteram16[offset]);
/*TODO*///	changecolor_xBBBBBGGGGGRRRRR(offset,paletteram16[offset]);
/*TODO*///}
    public static void changecolor_xRRRRRGGGGGBBBBB(int color, int data) {
        int r, g, b;

        r = (data >> 10) & 0x1f;
        g = (data >> 5) & 0x1f;
        b = (data >> 0) & 0x1f;

        r = (r << 3) | (r >> 2);
        g = (g << 3) | (g >> 2);
        b = (b << 3) | (b >> 2);

        palette_set_color(color, r, g, b);
    }

    public static WriteHandlerPtr paletteram_xRRRRRGGGGGBBBBB_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            paletteram.write(offset, data);
            changecolor_xRRRRRGGGGGBBBBB(offset / 2, paletteram.read(offset & ~1) | (paletteram.read(offset | 1) << 8));
        }
    };

    public static WriteHandlerPtr paletteram16_xRRRRRGGGGGBBBBB_word_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /*TODO*///	COMBINE_DATA(&paletteram16[offset]);
            changecolor_xRRRRRGGGGGBBBBB(offset,paletteram.read(offset));
        }
    };

    
/*TODO*///INLINE void changecolor_xGGGGGRRRRRBBBBB(int color,int data)
/*TODO*///{
/*TODO*///	int r,g,b;
/*TODO*///
/*TODO*///
/*TODO*///	r = (data >>  5) & 0x1f;
/*TODO*///	g = (data >> 10) & 0x1f;
/*TODO*///	b = (data >>  0) & 0x1f;
/*TODO*///
/*TODO*///	r = (r << 3) | (r >> 2);
/*TODO*///	g = (g << 3) | (g >> 2);
/*TODO*///	b = (b << 3) | (b >> 2);
/*TODO*///
/*TODO*///	palette_set_color(color,r,g,b);
/*TODO*///}
/*TODO*///
/*TODO*///WRITE16_HANDLER( paletteram16_xGGGGGRRRRRBBBBB_word_w )
/*TODO*///{
/*TODO*///	COMBINE_DATA(&paletteram16[offset]);
/*TODO*///	changecolor_xGGGGGRRRRRBBBBB(offset,paletteram16[offset]);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///INLINE void changecolor_RRRRRGGGGGBBBBBx(int color,int data)
/*TODO*///{
/*TODO*///	int r,g,b;
/*TODO*///
/*TODO*///
/*TODO*///	r = (data >> 11) & 0x1f;
/*TODO*///	g = (data >>  6) & 0x1f;
/*TODO*///	b = (data >>  1) & 0x1f;
/*TODO*///
/*TODO*///	r = (r << 3) | (r >> 2);
/*TODO*///	g = (g << 3) | (g >> 2);
/*TODO*///	b = (b << 3) | (b >> 2);
/*TODO*///
/*TODO*///	palette_set_color(color,r,g,b);
/*TODO*///}
/*TODO*///
/*TODO*///WRITE_HANDLER( paletteram_RRRRRGGGGGBBBBBx_w )
/*TODO*///{
/*TODO*///	paletteram[offset] = data;
/*TODO*///	changecolor_RRRRRGGGGGBBBBBx(offset / 2,paletteram[offset & ~1] | (paletteram[offset | 1] << 8));
/*TODO*///}
/*TODO*///
/*TODO*///WRITE16_HANDLER( paletteram16_RRRRRGGGGGBBBBBx_word_w )
/*TODO*///{
/*TODO*///	COMBINE_DATA(&paletteram16[offset]);
/*TODO*///	changecolor_RRRRRGGGGGBBBBBx(offset,paletteram16[offset]);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///INLINE void changecolor_IIIIRRRRGGGGBBBB(int color,int data)
/*TODO*///{
/*TODO*///	int i,r,g,b;
/*TODO*///
/*TODO*///
/*TODO*///	static const int ztable[16] =
/*TODO*///		{ 0x0, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf, 0x10, 0x11 };
/*TODO*///
/*TODO*///	i = ztable[(data >> 12) & 15];
/*TODO*///	r = ((data >> 8) & 15) * i;
/*TODO*///	g = ((data >> 4) & 15) * i;
/*TODO*///	b = ((data >> 0) & 15) * i;
/*TODO*///
/*TODO*///	palette_set_color(color,r,g,b);
/*TODO*///
/*TODO*///	if (!(Machine->drv->video_attributes & VIDEO_NEEDS_6BITS_PER_GUN))
/*TODO*///		usrintf_showmessage("driver should use VIDEO_NEEDS_6BITS_PER_GUN flag");
/*TODO*///}
/*TODO*///
/*TODO*///WRITE16_HANDLER( paletteram16_IIIIRRRRGGGGBBBB_word_w )
/*TODO*///{
/*TODO*///	COMBINE_DATA(&paletteram16[offset]);
/*TODO*///	changecolor_IIIIRRRRGGGGBBBB(offset,paletteram16[offset]);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///INLINE void changecolor_RRRRGGGGBBBBIIII(int color,int data)
/*TODO*///{
/*TODO*///	int i,r,g,b;
/*TODO*///
/*TODO*///
/*TODO*///	static const int ztable[16] =
/*TODO*///		{ 0x0, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf, 0x10, 0x11 };
/*TODO*///
/*TODO*///	i = ztable[(data >> 0) & 15];
/*TODO*///	r = ((data >> 12) & 15) * i;
/*TODO*///	g = ((data >>  8) & 15) * i;
/*TODO*///	b = ((data >>  4) & 15) * i;
/*TODO*///
/*TODO*///	palette_set_color(color,r,g,b);
/*TODO*///
/*TODO*///	if (!(Machine->drv->video_attributes & VIDEO_NEEDS_6BITS_PER_GUN))
/*TODO*///		usrintf_showmessage("driver should use VIDEO_NEEDS_6BITS_PER_GUN flag");
/*TODO*///}
/*TODO*///
/*TODO*///WRITE16_HANDLER( paletteram16_RRRRGGGGBBBBIIII_word_w )
/*TODO*///{
/*TODO*///	COMBINE_DATA(&paletteram16[offset]);
/*TODO*///	changecolor_RRRRGGGGBBBBIIII(offset,paletteram16[offset]);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///WRITE16_HANDLER( paletteram16_xrgb_word_w )
/*TODO*///{
/*TODO*///	int r, g, b;
/*TODO*///	data16_t data0, data1;
/*TODO*///
/*TODO*///	COMBINE_DATA(paletteram16 + offset);
/*TODO*///
/*TODO*///	offset &= ~1;
/*TODO*///
/*TODO*///	data0 = paletteram16[offset];
/*TODO*///	data1 = paletteram16[offset + 1];
/*TODO*///
/*TODO*///	r = data0 & 0xff;
/*TODO*///	g = data1 >> 8;
/*TODO*///	b = data1 & 0xff;
/*TODO*///
/*TODO*///	palette_set_color(offset>>1, r, g, b);
/*TODO*///
/*TODO*///	if (!(Machine->drv->video_attributes & VIDEO_NEEDS_6BITS_PER_GUN))
/*TODO*///		usrintf_showmessage("driver should use VIDEO_NEEDS_6BITS_PER_GUN flag");
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///INLINE void changecolor_RRRRGGGGBBBBRGBx(int color,int data)
/*TODO*///{
/*TODO*///	int r,g,b;
/*TODO*///
/*TODO*///	r = ((data >> 11) & 0x1e) | ((data>>3) & 0x01);
/*TODO*///	g = ((data >>  7) & 0x1e) | ((data>>2) & 0x01);
/*TODO*///	b = ((data >>  3) & 0x1e) | ((data>>1) & 0x01);
/*TODO*///	r = (r<<3) | (r>>2);
/*TODO*///	g = (g<<3) | (g>>2);
/*TODO*///	b = (b<<3) | (b>>2);
/*TODO*///
/*TODO*///	palette_set_color(color,r,g,b);
/*TODO*///}
/*TODO*///
/*TODO*///WRITE16_HANDLER( paletteram16_RRRRGGGGBBBBRGBx_word_w )
/*TODO*///{
/*TODO*///	COMBINE_DATA(&paletteram16[offset]);
/*TODO*///	changecolor_RRRRGGGGBBBBRGBx(offset,paletteram16[offset]);
/*TODO*///}
    /**
     * ****************************************************************************
     *
     * Commonly used color PROM handling functions
     *
     *****************************************************************************
     */
    /**
     * *************************************************************************
     *
     * This assumes the commonly used resistor values:
     *
     * bit 3 -- 220 ohm resistor -- RED/GREEN/BLUE -- 470 ohm resistor --
     * RED/GREEN/BLUE -- 1 kohm resistor -- RED/GREEN/BLUE bit 0 -- 2.2kohm
     * resistor -- RED/GREEN/BLUE
     *
     **************************************************************************
     */
    public static VhConvertColorPromPtr palette_RRRR_GGGG_BBBB_convert_prom = new VhConvertColorPromPtr() {
        public void handler(char[] palette, char[] colortable, UBytePtr color_prom) {
            int i;

            for (i = 0; i < Machine.drv.total_colors; i++) {
                int bit0, bit1, bit2, bit3, r, g, b;

                /* red component */
                bit0 = (color_prom.read(i) >> 0) & 0x01;
                bit1 = (color_prom.read(i) >> 1) & 0x01;
                bit2 = (color_prom.read(i) >> 2) & 0x01;
                bit3 = (color_prom.read(i) >> 3) & 0x01;
                r = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
                /* green component */
                bit0 = (color_prom.read(i + Machine.drv.total_colors) >> 0) & 0x01;
                bit1 = (color_prom.read(i + Machine.drv.total_colors) >> 1) & 0x01;
                bit2 = (color_prom.read(i + Machine.drv.total_colors) >> 2) & 0x01;
                bit3 = (color_prom.read(i + Machine.drv.total_colors) >> 3) & 0x01;
                g = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;
                /* blue component */
                bit0 = (color_prom.read(i + 2 * Machine.drv.total_colors) >> 0) & 0x01;
                bit1 = (color_prom.read(i + 2 * Machine.drv.total_colors) >> 1) & 0x01;
                bit2 = (color_prom.read(i + 2 * Machine.drv.total_colors) >> 2) & 0x01;
                bit3 = (color_prom.read(i + 2 * Machine.drv.total_colors) >> 3) & 0x01;
                b = 0x0e * bit0 + 0x1f * bit1 + 0x43 * bit2 + 0x8f * bit3;

                palette_set_color(i, r, g, b);
            }
            
            /*palette[0]=0x00;
            palette[1]=0x00;
            palette[2]=0x00;

            palette[3]=0xFF;
            palette[4]=0xFF;
            palette[5]=0xFF;*/
            //palette_set_color(0, 0, 0, 0);
            //palette_set_color(1, 0xff, 0xff, 0xff);
        }
    };

}
