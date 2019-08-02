/******************************************************************************

	Machine Hardware for Nichibutsu Mahjong series.

	Driver by Takahiro Nogi <nogi@kt.rim.or.jp> 1999/11/05 -

******************************************************************************/
package WIP.mame056.machine;

import static mame056.inptportH.*;
import static mame056.inptport.*;
import static mame056.inputH.*;

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
public class nb1413m3H {
    	
        public static final int NB1413M3_NONE = 0;
	// unknown
	public static final int NB1413M3_JOKERMJN = 1;
	public static final int NB1413M3_JNGNIGHT = 2;
	public static final int NB1413M3_JNGLADY = 3;
	public static final int NB1413M3_NIGHTGL = 4;
	public static final int NB1413M3_NIGHTGLS = 5;
	public static final int NB1413M3_SWEETGAL = 6;
	// NB1411M1
	public static final int NB1413M3_PASTELGL = 7;
	// NB1413M3
	public static final int NB1413M3_CRYSTAL = 8;
	public static final int NB1413M3_CRYSTAL2 = 9;
	public static final int NB1413M3_NIGHTLOV = 10;
	public static final int NB1413M3_CITYLOVE = 11;
	public static final int NB1413M3_SECOLOVE = 12;
	public static final int NB1413M3_HOUSEMNQ = 13;
	public static final int NB1413M3_HOUSEMN2 = 14;
	public static final int NB1413M3_LIVEGAL = 15;
	public static final int NB1413M3_BIJOKKOY = 16;
	public static final int NB1413M3_IEMOTO = 17;
	public static final int NB1413M3_SEIHA = 18;
	public static final int NB1413M3_SEIHAM = 19;
	public static final int NB1413M3_HYHOO = 20;
	public static final int NB1413M3_HYHOO2 = 21;
	public static final int NB1413M3_SWINGGAL = 22;
	public static final int NB1413M3_BIJOKKOG = 23;
	public static final int NB1413M3_OJOUSAN = 24;
	public static final int NB1413M3_KORINAI = 25;
	public static final int NB1413M3_MJCAMERA = 26;
	public static final int NB1413M3_TAIWANMJ = 27;
	public static final int NB1413M3_TAIWANMB = 28;
	public static final int NB1413M3_OTONANO = 29;
	public static final int NB1413M3_MJSIKAKU = 30;
	public static final int NB1413M3_MSJIKEN = 31;
	public static final int NB1413M3_HANAMOMO = 32;
	public static final int NB1413M3_TELMAHJN = 33;
	public static final int NB1413M3_GIONBANA = 34;
	public static final int NB1413M3_SCANDAL = 35;
	public static final int NB1413M3_SCANDALM = 36;
	public static final int NB1413M3_MGMEN89 = 37;
	public static final int NB1413M3_OHPYEPEE = 38;
	public static final int NB1413M3_TOUGENK = 39;
	public static final int NB1413M3_MJUCHUU = 40;
	public static final int NB1413M3_MJFOCUS = 41;
	public static final int NB1413M3_MJFOCUSM = 42;
	public static final int NB1413M3_PEEPSHOW = 43;
	public static final int NB1413M3_GALKOKU = 44;
	public static final int NB1413M3_MJNANPAS = 45;
	public static final int NB1413M3_BANANADR = 46;
	public static final int NB1413M3_GALKAIKA = 47;
	public static final int NB1413M3_MCONTEST = 48;
	public static final int NB1413M3_TOKIMBSJ = 49;
	public static final int NB1413M3_TOKYOGAL = 50;
	public static final int NB1413M3_TRIPLEW1 = 51;
	public static final int NB1413M3_NTOPSTAR = 52;
	public static final int NB1413M3_MLADYHTR = 53;
	public static final int NB1413M3_PSTADIUM = 54;
	public static final int NB1413M3_TRIPLEW2 = 55;
	public static final int NB1413M3_CLUB90S = 56;
	public static final int NB1413M3_CHINMOKU = 57;
	public static final int NB1413M3_VANILLA = 58;
	public static final int NB1413M3_MJLSTORY = 59;
	public static final int NB1413M3_QMHAYAKU = 60;
	public static final int NB1413M3_MAIKO = 61;
	public static final int NB1413M3_HANAOJI = 62;
	public static final int NB1413M3_KAGUYA = 63;
	public static final int NB1413M3_APPAREL = 64;
	public static final int NB1413M3_AV2MJ1 = 65;
	public static final int NB1413M3_FINALBNY = 66;
	public static final int NB1413M3_HYOUBAN = 67;


        public static final int NB1413M3_VCR_NOP	= 0x00;
        public static final int NB1413M3_VCR_POWER	= 0x01;
        public static final int NB1413M3_VCR_STOP	= 0x02;
        public static final int NB1413M3_VCR_REWIND	= 0x04;
        public static final int NB1413M3_VCR_PLAY	= 0x08;
        public static final int NB1413M3_VCR_FFORWARD	= 0x10;
        public static final int NB1413M3_VCR_PAUSE	= 0x20;

        public static void NBMJCTRL_PORT1() {
                PORT_START(); 	/* (3) PORT 1-1 */ 
                PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_START1 );
                PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P1 Kan", KEYCODE_LCONTROL, IP_JOY_NONE );
                PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P1 M", KEYCODE_M, IP_JOY_NONE );
                PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 I", KEYCODE_I, IP_JOY_NONE );
                PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 E", KEYCODE_E, IP_JOY_NONE );
                PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 A", KEYCODE_A, IP_JOY_NONE );
                PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }

        public static void NBMJCTRL_PORT2() {
                PORT_START(); 	/* (4) PORT 1-2 */ 
                PORT_BITX(0x01, IP_ACTIVE_LOW, 0, "P1 Bet", KEYCODE_2, IP_JOY_NONE );
                PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P1 Reach", KEYCODE_LSHIFT, IP_JOY_NONE );
                PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P1 N", KEYCODE_N, IP_JOY_NONE );
                PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 J", KEYCODE_J, IP_JOY_NONE );
                PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 F", KEYCODE_F, IP_JOY_NONE );
                PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 B", KEYCODE_B, IP_JOY_NONE );
                PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }

        public static void NBMJCTRL_PORT3() {
                PORT_START(); 	/* (5) PORT 1-3 */ 
                PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P1 Ron", KEYCODE_Z, IP_JOY_NONE );
                PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P1 Chi", KEYCODE_SPACE, IP_JOY_NONE );
                PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 K", KEYCODE_K, IP_JOY_NONE );
                PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 G", KEYCODE_G, IP_JOY_NONE );
                PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 C", KEYCODE_C, IP_JOY_NONE );
                PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }

        public static void NBMJCTRL_PORT4() {
                PORT_START(); 	/* (6) PORT 1-4 */ 
                PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P1 Pon", KEYCODE_LALT, IP_JOY_NONE );
                PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 L", KEYCODE_L, IP_JOY_NONE );
                PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 H", KEYCODE_H, IP_JOY_NONE );
                PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 D", KEYCODE_D, IP_JOY_NONE );
                PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }

        public static void NBMJCTRL_PORT5() {
                PORT_START(); 	/* (7) PORT 1-5 */ 
                PORT_BITX(0x01, IP_ACTIVE_LOW, 0, "P1 Small", KEYCODE_BACKSPACE, IP_JOY_NONE );
                PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P1 Big", KEYCODE_ENTER, IP_JOY_NONE );
                PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P1 Flip", KEYCODE_X, IP_JOY_NONE );
                PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P1 Double Up", KEYCODE_RSHIFT, IP_JOY_NONE );
                PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P1 Take Score", KEYCODE_RCONTROL, IP_JOY_NONE );
                PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P1 Last Chance", KEYCODE_RALT, IP_JOY_NONE );
                PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }

        public static void NBMJCTRL_PORT6() {
                PORT_START(); 	/* (6) PORT 2-1 */ 
                PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_START2 );
                PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P2 Kan", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P2 M", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P2 I", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 E", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 A", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }

        public static void NBMJCTRL_PORT7() {
                PORT_START(); 	/* (7) PORT 2-2 */ 
                PORT_BITX(0x01, IP_ACTIVE_LOW, 0, "P2 Bet", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P2 Reach", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P2 N", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P2 J", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 F", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 B", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }

        public static void NBMJCTRL_PORT8() {
                PORT_START(); 	/* (8) PORT 2-3 */ 
                PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P2 Ron", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P2 Chi", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P2 K", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 G", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 C", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }

        public static void NBMJCTRL_PORT9() {
                PORT_START(); 	/* (9) PORT 2-4 */ 
                PORT_BIT( 0x01, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BIT( 0x02, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P2 Pon", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P2 L", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 H", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 D", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }

        public static void NBMJCTRL_PORT10() {
                PORT_START(); 	/* (10) PORT 2-5 */ 
                PORT_BITX(0x01, IP_ACTIVE_LOW, 0, "P2 Small", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x02, IP_ACTIVE_LOW, 0, "P2 Big", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x04, IP_ACTIVE_LOW, 0, "P2 Flip", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x08, IP_ACTIVE_LOW, 0, "P2 Double Up", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x10, IP_ACTIVE_LOW, 0, "P2 Take Score", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BITX(0x20, IP_ACTIVE_LOW, 0, "P2 Last Chance", IP_KEY_DEFAULT, IP_JOY_NONE );
                PORT_BIT( 0x40, IP_ACTIVE_LOW, IPT_UNKNOWN );
                PORT_BIT( 0x80, IP_ACTIVE_LOW, IPT_UNKNOWN );
        }

}
