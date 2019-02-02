/**
 * Ported to 0.56
 */
package mame056;

public class osdependH {

    /**
     * ****************************************************************************
     * Joystick & Mouse/Trackball
     * ****************************************************************************
     */

    /* We support 4 players for each analog control */
    public static final int OSD_MAX_JOY_ANALOG = 4;
    public static final int X_AXIS = 1;
    public static final int Y_AXIS = 2;

    /**
     * ****************************************************************************
     *
     * File I/O
     *
     *****************************************************************************
     */

    /*TODO*////* inp header */
/*TODO*///typedef struct
/*TODO*///{
/*TODO*///	char name[9];      /* 8 bytes for game->name + NUL */
/*TODO*///	char version[3];   /* byte[0] = 0, byte[1] = version byte[2] = beta_version */
/*TODO*///	char reserved[20]; /* for future use, possible store game options? */
/*TODO*///} INP_HEADER;
/*TODO*///

    /* file handling routines */
    public static final int OSD_FILETYPE_ROM = 1;
    public static final int OSD_FILETYPE_SAMPLE = 2;
    public static final int OSD_FILETYPE_NVRAM = 3;
    public static final int OSD_FILETYPE_HIGHSCORE = 4;
    public static final int OSD_FILETYPE_HIGHSCORE_DB = 5;/* LBO 040400 */
    public static final int OSD_FILETYPE_CONFIG = 6;
    public static final int OSD_FILETYPE_INPUTLOG = 7;
    public static final int OSD_FILETYPE_STATE = 8;
    public static final int OSD_FILETYPE_ARTWORK = 9;
    public static final int OSD_FILETYPE_MEMCARD = 10;
    public static final int OSD_FILETYPE_SCREENSHOT = 11;
    public static final int OSD_FILETYPE_HISTORY = 12;/* LBO 040400 */
    public static final int OSD_FILETYPE_CHEAT = 13;/* LBO 040400 */
    public static final int OSD_FILETYPE_LANGUAGE = 14;/* LBO 042400 */
    public static final int OSD_FILETYPE_end = 15;/* dummy last entry */

 /*TODO*///#ifdef LSB_FIRST
/*TODO*///#define osd_fread_msbfirst osd_fread_swap
/*TODO*///#define osd_fwrite_msbfirst osd_fwrite_swap
/*TODO*///#define osd_fread_lsbfirst osd_fread
/*TODO*///#define osd_fwrite_lsbfirst osd_fwrite
/*TODO*///#else
/*TODO*///#define osd_fread_msbfirst osd_fread
/*TODO*///#define osd_fwrite_msbfirst osd_fwrite
/*TODO*///#define osd_fread_lsbfirst osd_fread_swap
/*TODO*///#define osd_fwrite_lsbfirst osd_fwrite_swap
/*TODO*///#endif

}
