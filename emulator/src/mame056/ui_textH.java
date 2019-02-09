/**
 * ported to 0.56
 * ported to 0.37b7
 */
package mame056;

import static common.ptr.*;

public class ui_textH {

    /* Important: this must match the default_text list in ui_text.c! */
    public static final int UI_first_entry = -1;

    public static final int UI_mame = 0;

    /* copyright stuff */
    public static final int UI_copyright1 = 1;
    public static final int UI_copyright2 = 2;
    public static final int UI_copyright3 = 3;

    /* misc menu stuff */
    public static final int UI_returntomain = 4;
    public static final int UI_returntoprior = 5;
    public static final int UI_anykey = 6;
    public static final int UI_on = 7;
    public static final int UI_off = 8;
    public static final int UI_NA = 9;
    public static final int UI_OK = 10;
    public static final int UI_INVALID = 11;
    public static final int UI_none = 12;
    public static final int UI_cpu = 13;
    public static final int UI_address = 14;
    public static final int UI_value = 15;
    public static final int UI_sound = 16;
    public static final int UI_sound_lc = 17;
    /* lower-case version */
    public static final int UI_stereo = 18;
    public static final int UI_vectorgame = 19;
    public static final int UI_screenres = 20;
    public static final int UI_text = 21;
    public static final int UI_volume = 22;
    public static final int UI_relative = 23;
    public static final int UI_allchannels = 24;
    public static final int UI_brightness = 25;
    public static final int UI_gamma = 26;
    public static final int UI_vectorflicker = 27;
    public static final int UI_vectorintensity = 28;
    public static final int UI_overclock = 29;
    public static final int UI_allcpus = 30;
    public static final int UI_historymissing = 31;

    /* special characters */
    public static final int UI_leftarrow = 32;
    public static final int UI_rightarrow = 33;
    public static final int UI_uparrow = 34;
    public static final int UI_downarrow = 35;
    public static final int UI_lefthilight = 36;
    public static final int UI_righthilight = 37;

    /* warnings */
    public static final int UI_knownproblems = 38;
    public static final int UI_imperfectcolors = 39;
    public static final int UI_wrongcolors = 40;
    public static final int UI_imperfectgraphics = 41;
    public static final int UI_imperfectsound = 42;
    public static final int UI_nosound = 43;
    public static final int UI_nococktail = 44;
    public static final int UI_brokengame = 45;
    public static final int UI_brokenprotection = 46;
    public static final int UI_workingclones = 47;
    public static final int UI_typeok = 48;

    /* main menu */
    public static final int UI_inputgeneral = 49;
    public static final int UI_dipswitches = 50;
    public static final int UI_analogcontrols = 51;
    public static final int UI_calibrate = 52;
    public static final int UI_bookkeeping = 53;
    public static final int UI_inputspecific = 54;
    public static final int UI_gameinfo = 55;
    public static final int UI_history = 56;
    public static final int UI_resetgame = 57;
    public static final int UI_returntogame = 58;
    public static final int UI_cheat = 59;
    public static final int UI_memorycard = 60;

    /* input stuff */
    public static final int UI_keyjoyspeed = 61;
    public static final int UI_reverse = 62;
    public static final int UI_sensitivity = 63;

    /* stats */
    public static final int UI_tickets = 64;
    public static final int UI_coin = 65;
    public static final int UI_locked = 66;

    /* memory card */
    public static final int UI_loadcard = 67;
    public static final int UI_ejectcard = 68;
    public static final int UI_createcard = 69;
    public static final int UI_resetcard = 70;
    public static final int UI_loadfailed = 71;
    public static final int UI_loadok = 72;
    public static final int UI_cardejected = 73;
    public static final int UI_cardcreated = 74;
    public static final int UI_cardcreatedfailed = 75;
    public static final int UI_cardcreatedfailed2 = 76;
    public static final int UI_carderror = 77;

    /* cheat stuff */
    public static final int UI_enablecheat = 78;
    public static final int UI_addeditcheat = 79;
    public static final int UI_startcheat = 80;
    public static final int UI_continuesearch = 81;
    public static final int UI_viewresults = 82;
    public static final int UI_restoreresults = 83;
    public static final int UI_memorywatch = 84;
    public static final int UI_generalhelp = 85;
    public static final int UI_options = 86;
    public static final int UI_reloaddatabase = 87;
    public static final int UI_watchpoint = 88;
    public static final int UI_disabled = 89;
    public static final int UI_cheats = 90;
    public static final int UI_watchpoints = 91;
    public static final int UI_moreinfo = 92;
    public static final int UI_moreinfoheader = 93;
    public static final int UI_cheatname = 94;
    public static final int UI_cheatdescription = 95;
    public static final int UI_cheatactivationkey = 96;
    public static final int UI_code = 97;
    public static final int UI_max = 98;
    public static final int UI_set = 99;
    public static final int UI_conflict_found = 100;
    public static final int UI_no_help_available = 101;

    /* watchpoint stuff */
    public static final int UI_watchlength = 102;
    public static final int UI_watchdisplaytype = 103;
    public static final int UI_watchlabeltype = 104;
    public static final int UI_watchlabel = 105;
    public static final int UI_watchx = 106;
    public static final int UI_watchy = 107;
    public static final int UI_watch = 108;

    public static final int UI_hex = 109;
    public static final int UI_decimal = 110;
    public static final int UI_binary = 111;

    /* search stuff */
    public static final int UI_search_lives = 112;
    public static final int UI_search_timers = 113;
    public static final int UI_search_energy = 114;
    public static final int UI_search_status = 115;
    public static final int UI_search_slow = 116;
    public static final int UI_search_speed = 117;
    public static final int UI_search_speed_fast = 118;
    public static final int UI_search_speed_medium = 119;
    public static final int UI_search_speed_slow = 120;
    public static final int UI_search_speed_veryslow = 121;
    public static final int UI_search_speed_allmemory = 122;
    public static final int UI_search_select_memory_areas = 123;
    public static final int UI_search_matches_found = 124;
    public static final int UI_search_noinit = 125;
    public static final int UI_search_nosave = 126;
    public static final int UI_search_done = 127;
    public static final int UI_search_OK = 128;
    public static final int UI_search_select_value = 129;
    public static final int UI_search_all_values_saved = 130;
    public static final int UI_search_one_match_found_added = 131;

    public static final int UI_last_entry = 132;

    public static class lang_struct {

        public int version;
        public int multibyte;/* UNUSED: 1 if this is a multibyte font/language */
        public UBytePtr fontdata;/* pointer to the raw font data to be decoded */
        public char fontglyphs;/* total number of glyps in the external font - 1 */
        public String langname;
        public String fontname;
        public String author;
    }
}
