/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

public class avgdvgH
{    
    
    /* vector engine types, passed to vg_init */

    public static int AVGDVG_MIN =          1;
    public static int USE_DVG =             1;
    public static int USE_AVG_RBARON =      2;
    public static int USE_AVG_BZONE =       3;
    public static int USE_AVG =             4;
    public static int USE_AVG_TEMPEST =     5;
    public static int USE_AVG_MHAVOC =      6;
    public static int USE_AVG_SWARS =       7;
    public static int USE_AVG_QUANTUM =     8;
    public static int AVGDVG_MAX =          8;

    //WRITE16_HANDLER( avgdvg_go_word_w );
    //WRITE16_HANDLER( avgdvg_reset_word_w );
    //int avgdvg_init(int vgType);

    /* Apart from the color mentioned below, the vector games will make additional
     * entries for translucency/antialiasing and for backdrop/overlay artwork */

    /* Black and White vector colors for Asteroids, Lunar Lander, Omega Race */
    //void avg_init_palette_white (unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom);
    /* Monochrome Aqua vector colors for Red Baron */
    //void avg_init_palette_aqua  (unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom);
    /* Red and Green vector colors for Battlezone */
    //void avg_init_palette_bzone (unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom);
    /* Basic 8 rgb vector colors for Tempest, Gravitar, Major Havoc etc. */
    //void avg_init_palette_multi (unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom);
    /* Special case for Star Wars and Empire strikes back */
    //void avg_init_palette_swars (unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom);
    /* Monochrome Aqua vector colors for Asteroids Deluxe */
    //void avg_init_palette_astdelux  (unsigned char *palette, unsigned short *colortable,const unsigned char *color_prom);

    /* Some games use a colorram. This is not handled via the Mame core functions
     * right now, but in src/vidhrdw/avgdvg.c itself. */
    //WRITE16_HANDLER( quantum_colorram_w );


    
}