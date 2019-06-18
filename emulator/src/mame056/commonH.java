/**
 * Ported to 0.56
 */
package mame056;

import static common.ptr.*;
import java.util.ArrayList;
import static mame056.common.*;

public class commonH {

    /**
     * *************************************************************************
     *
     * Type definitions
     *
     **************************************************************************
     */
    public static class mame_bitmap {

        public int width, height;/* width and height of the bitmap */
        public int depth;/* bits per pixel */
        public UBytePtr[] line;/* pointers to the start of each line - can be UINT8 **, UINT16 ** or UINT32 ** */

 /* alternate way of accessing the pixels */
        public UBytePtr base;/* pointer to pixel (0,0) (adjusted for padding) */
        public int rowpixels;/* pixels per row (including padding) */
        public int rowbytes;/* bytes per row (including padding) */
    }

    public static class RomModule {

        public RomModule(String _name, int _offset, int _length, int _crc) {
            this._name = _name;
            this._offset = _offset;
            this._length = _length;
            this._crc = _crc;
        }

        public String _name;/* name of the file to load */
        public int/*UINT32*/ _offset;/* offset to load it to */
        public int/*UINT32*/ _length;/* length of the file */
        public int/*UINT32*/ _crc;/* standard CRC-32 checksum */
    }

    public static class GameSample {

        public GameSample() {
            data = new byte[1];
        }

        public GameSample(int len) {
            data = new byte[len];
        }
        public int length;
        public int smpfreq;
        public int resolution;
        public byte data[];/* extendable */
    }

    public static class GameSamples {

        public GameSamples() {
            sample = new GameSample[1];
            sample[0] = new GameSample();
        }

        public GameSamples(int size) {
            sample = new GameSample[size];
            for (int i = 0; i < size; i++) {
                sample[i] = new GameSample(1);
            }
        }
        public int total;/* total number of samples */
        public GameSample sample[];/* extendable */
    }

    /**
     * *************************************************************************
     *
     * Constants and macros
     *
     **************************************************************************
     */
    public static final int REGION_INVALID = 0x80;
    public static final int REGION_CPU1 = 0x81;
    public static final int REGION_CPU2 = 0x82;
    public static final int REGION_CPU3 = 0x83;
    public static final int REGION_CPU4 = 0x84;
    public static final int REGION_CPU5 = 0x85;
    public static final int REGION_CPU6 = 0x86;
    public static final int REGION_CPU7 = 0x87;
    public static final int REGION_CPU8 = 0x88;
    public static final int REGION_GFX1 = 0x89;
    public static final int REGION_GFX2 = 0x8a;
    public static final int REGION_GFX3 = 0x8b;
    public static final int REGION_GFX4 = 0x8c;
    public static final int REGION_GFX5 = 0x8d;
    public static final int REGION_GFX6 = 0x8e;
    public static final int REGION_GFX7 = 0x8f;
    public static final int REGION_GFX8 = 0x90;
    public static final int REGION_PROMS = 0x91;
    public static final int REGION_SOUND1 = 0x92;
    public static final int REGION_SOUND2 = 0x93;
    public static final int REGION_SOUND3 = 0x94;
    public static final int REGION_SOUND4 = 0x95;
    public static final int REGION_SOUND5 = 0x96;
    public static final int REGION_SOUND6 = 0x97;
    public static final int REGION_SOUND7 = 0x98;
    public static final int REGION_SOUND8 = 0x99;
    public static final int REGION_USER1 = 0x9a;
    public static final int REGION_USER2 = 0x9b;
    public static final int REGION_USER3 = 0x9c;
    public static final int REGION_USER4 = 0x9d;
    public static final int REGION_USER5 = 0x9e;
    public static final int REGION_USER6 = 0x9f;
    public static final int REGION_USER7 = 0xa0;
    public static final int REGION_USER8 = 0xa1;
    public static final int REGION_MAX = 0xa2;

    public static int BADCRC(int crc) {
        return ~crc;
    }

    /**
     * *************************************************************************
     *
     * Core macros for the ROM loading system
     *
     **************************************************************************
     */

    /* ----- length compaction macros ----- */
    public static final int INVALID_LENGTH = 0x7ff;

    public static final int COMPACT_LENGTH(int x) {
        return ((((x) & 0xffffff00) == 0) ? (0x000 | ((x) >> 0))
                : (((x) & 0xfffff00f) == 0) ? (0x100 | ((x) >> 4))
                        : (((x) & 0xffff00ff) == 0) ? (0x200 | ((x) >> 8))
                                : (((x) & 0xfff00fff) == 0) ? (0x300 | ((x) >> 12))
                                        : (((x) & 0xff00ffff) == 0) ? (0x400 | ((x) >> 16))
                                                : (((x) & 0xf00fffff) == 0) ? (0x500 | ((x) >> 20))
                                                        : (((x) & 0x00ffffff) == 0) ? (0x600 | ((x) >> 24))
                                                                : INVALID_LENGTH);
    }

    public static final int UNCOMPACT_LENGTH(int x) {
        return (((x) == INVALID_LENGTH) ? 0 : (((x) & 0xff) << (((x) & 0x700) >> 6)));
    }

    /* ----- per-entry constants ----- */
    //arcadeflex note : use negative numbers in case game has rom named "1" to "6" this has been found in wiping driver
    public static final int ROMENTRYTYPE_REGION = -1;/* this entry marks the start of a region */
    public static final int ROMENTRYTYPE_END = -2;/* this entry marks the end of a region */
    public static final int ROMENTRYTYPE_RELOAD = -3;/* this entry reloads the previous ROM */
    public static final int ROMENTRYTYPE_CONTINUE = -4;/* this entry continues loading the previous ROM */
    public static final int ROMENTRYTYPE_FILL = -5;/* this entry fills an area with a constant value */
    public static final int ROMENTRYTYPE_COPY = -6;/* this entry copies data from another region/offset */
    public static final int ROMENTRYTYPE_COUNT = -7;

    public static final String ROMENTRY_REGION = "-1";
    public static final String ROMENTRY_END = "-2";
    public static final String ROMENTRY_RELOAD = "-3";
    public static final String ROMENTRY_CONTINUE = "-4";
    public static final String ROMENTRY_FILL = "-5";
    public static final String ROMENTRY_COPY = "-6";

    /* ----- per-entry macros ----- */
    public static int ROMENTRY_GETTYPE(RomModule[] romp, int romp_ptr) {
        //((FPTR)(r)->_name)
        int result;
        try {
            result = Integer.parseInt(romp[romp_ptr]._name);//possible values 1-6
        } catch (Exception e) {
            result = 15; //random value just not to be something between 1-6
        }
        return result;
    }

    public static boolean ROMENTRY_ISSPECIAL(RomModule[] romp, int romp_ptr) {
        return (ROMENTRY_GETTYPE(romp, romp_ptr) < ROMENTRYTYPE_COUNT);
    }

    public static boolean ROMENTRY_ISFILE(RomModule[] romp, int romp_ptr) {
        return (!ROMENTRY_ISSPECIAL(romp, romp_ptr));
    }

    public static boolean ROMENTRY_ISREGION(RomModule[] romp, int romp_ptr) {
        return romp[romp_ptr]._name.matches(ROMENTRY_REGION);
    }

    public static boolean ROMENTRY_ISEND(RomModule[] romp, int romp_ptr) {
        return romp[romp_ptr]._name.matches(ROMENTRY_END);
    }

    public static boolean ROMENTRY_ISRELOAD(RomModule[] romp, int romp_ptr) {
        return romp[romp_ptr]._name.matches(ROMENTRY_RELOAD);
    }

    public static boolean ROMENTRY_ISCONTINUE(RomModule[] romp, int romp_ptr) {
        return romp[romp_ptr]._name.matches(ROMENTRY_CONTINUE);
    }

    public static boolean ROMENTRY_ISFILL(RomModule[] romp, int romp_ptr) {
        return romp[romp_ptr]._name.matches(ROMENTRY_FILL);
    }

    public static boolean ROMENTRY_ISCOPY(RomModule[] romp, int romp_ptr) {
        return romp[romp_ptr]._name.matches(ROMENTRY_COPY);
    }

    public static boolean ROMENTRY_ISREGIONEND(RomModule[] romp, int romp_ptr) {
        return (ROMENTRY_ISREGION(romp, romp_ptr) || ROMENTRY_ISEND(romp, romp_ptr));
    }

    /* ----- per-region constants ----- */
    public static final int ROMREGION_WIDTHMASK = 0x00000003;/* native width of region, as power of 2 */
    public static final int ROMREGION_8BIT = 0x00000000;/*    (non-CPU regions only) */
    public static final int ROMREGION_16BIT = 0x00000001;
    public static final int ROMREGION_32BIT = 0x00000002;
    public static final int ROMREGION_64BIT = 0x00000003;

    public static final int ROMREGION_ENDIANMASK = 0x00000004;/* endianness of the region */
    public static final int ROMREGION_LE = 0x00000000;/*    (non-CPU regions only) */
    public static final int ROMREGION_BE = 0x00000004;

    public static final int ROMREGION_INVERTMASK = 0x00000008;/* invert the bits of the region */
    public static final int ROMREGION_NOINVERT = 0x00000000;
    public static final int ROMREGION_INVERT = 0x00000008;

    public static final int ROMREGION_DISPOSEMASK = 0x00000010;/* dispose of the region after init */
    public static final int ROMREGION_NODISPOSE = 0x00000000;
    public static final int ROMREGION_DISPOSE = 0x00000010;

    public static final int ROMREGION_SOUNDONLYMASK = 0x00000020;/* load only if sound is enabled */
    public static final int ROMREGION_NONSOUND = 0x00000000;
    public static final int ROMREGION_SOUNDONLY = 0x00000020;

    public static final int ROMREGION_LOADUPPERMASK = 0x00000040;/* load into the upper part of CPU space */
    public static final int ROMREGION_LOADLOWER = 0x00000000;/*     (CPU regions only) */
    public static final int ROMREGION_LOADUPPER = 0x00000040;

    public static final int ROMREGION_ERASEMASK = 0x00000080;/* erase the region before loading */
    public static final int ROMREGION_NOERASE = 0x00000000;
    public static final int ROMREGION_ERASE = 0x00000080;

    public static final int ROMREGION_ERASEVALMASK = 0x0000ff00;/* value to erase the region to */
 /*TODO*///#define		ROMREGION_ERASEVAL(x)	((((x) & 0xff) << 8) | ROMREGION_ERASE)
/*TODO*///#define		ROMREGION_ERASE00		ROMREGION_ERASEVAL(0)
/*TODO*///#define		ROMREGION_ERASEFF		ROMREGION_ERASEVAL(0xff)

    /* ----- per-region macros ----- */
    public static int ROMREGION_GETTYPE(RomModule[] romp, int romp_ptr) {
        return romp[romp_ptr]._crc;
    }

    public static int ROMREGION_GETLENGTH(RomModule[] romp, int romp_ptr) {
        return romp[romp_ptr]._length;
    }

    public static int ROMREGION_GETFLAGS(RomModule[] romp, int romp_ptr) {
        return romp[romp_ptr]._offset;
    }

    public static int ROMREGION_GETWIDTH(RomModule[] romp, int romp_ptr) {
        return (8 << (ROMREGION_GETFLAGS(romp, romp_ptr) & ROMREGION_WIDTHMASK));
    }

    public static boolean ROMREGION_ISLITTLEENDIAN(RomModule[] romp, int romp_ptr) {
        return ((ROMREGION_GETFLAGS(romp, romp_ptr) & ROMREGION_ENDIANMASK) == ROMREGION_LE);
    }

    public static boolean ROMREGION_ISBIGENDIAN(RomModule[] romp, int romp_ptr) {
        return ((ROMREGION_GETFLAGS(romp, romp_ptr) & ROMREGION_ENDIANMASK) == ROMREGION_BE);
    }

    public static boolean ROMREGION_ISINVERTED(RomModule[] romp, int romp_ptr) {
        return ((ROMREGION_GETFLAGS(romp, romp_ptr) & ROMREGION_INVERTMASK) == ROMREGION_INVERT);
    }

    public static boolean ROMREGION_ISDISPOSE(RomModule[] romp, int romp_ptr) {
        return ((ROMREGION_GETFLAGS(romp, romp_ptr) & ROMREGION_DISPOSEMASK) == ROMREGION_DISPOSE);
    }

    public static boolean ROMREGION_ISSOUNDONLY(RomModule[] romp, int romp_ptr) {
        return ((ROMREGION_GETFLAGS(romp, romp_ptr) & ROMREGION_SOUNDONLYMASK) == ROMREGION_SOUNDONLY);
    }

    /*TODO*///#define ROMREGION_ISLOADUPPER(r)	((ROMREGION_GETFLAGS(r) & ROMREGION_LOADUPPERMASK) == ROMREGION_LOADUPPER)
    public static boolean ROMREGION_ISERASE(RomModule[] romp, int romp_ptr) {
        return ((ROMREGION_GETFLAGS(romp, romp_ptr) & ROMREGION_ERASEMASK) == ROMREGION_ERASE);
    }
    /*TODO*///#define ROMREGION_GETERASEVAL(r)	((ROMREGION_GETFLAGS(r) & ROMREGION_ERASEVALMASK) >> 8)

    /* ----- per-ROM constants ----- */
    public static final int ROM_LENGTHMASK = 0x000007ff;/* the compacted length of the ROM */
    public static final int ROM_INVALIDLENGTH = INVALID_LENGTH;

    public static final int ROM_OPTIONALMASK = 0x00000800;/* optional - won't hurt if it's not there */
    public static final int ROM_REQUIRED = 0x00000000;
    public static final int ROM_OPTIONAL = 0x00000800;

    public static final int ROM_GROUPMASK = 0x0000f000;/* load data in groups of this size + 1 */
    public static int ROM_GROUPSIZE(int n){
        return ((((n) - 1) & 15) << 12);
    }
/*TODO*///#define		ROM_GROUPBYTE			ROM_GROUPSIZE(1)
    public static int		ROM_GROUPWORD() {
        return ROM_GROUPSIZE(2);
    }
/*TODO*///#define		ROM_GROUPDWORD			ROM_GROUPSIZE(4)

    public static final int ROM_SKIPMASK = 0x000f0000;/* skip this many bytes after each group */
    public static int ROM_SKIP(int n){
        return (((n) & 15) << 16);
    }
/*TODO*///#define		ROM_NOSKIP				ROM_SKIP(0)

    public static final int ROM_REVERSEMASK = 0x00100000;/* reverse the byte order within a group */
    public static final int ROM_NOREVERSE = 0x00000000;
    public static final int ROM_REVERSE = 0x00100000;
    
    public static final int ROM_BITWIDTHMASK = 0x00e00000;/* width of data in bits */
    public static int ROM_BITWIDTH(int n){
        return (((n) & 7) << 21);
    }
    public static int ROM_NIBBLE = ROM_BITWIDTH(4);
    public static int ROM_FULLBYTE = ROM_BITWIDTH(8);

    public static final int ROM_BITSHIFTMASK = 0x07000000;/* left-shift count for the bits */
    public static int ROM_BITSHIFT(int n){
        return (((n) & 7) << 24);
    }
    public static final int ROM_NOSHIFT = ROM_BITSHIFT(0);
    public static final int ROM_SHIFT_NIBBLE_LO = ROM_BITSHIFT(0);
    public static final int ROM_SHIFT_NIBBLE_HI = ROM_BITSHIFT(4);

    public static final int ROM_INHERITFLAGSMASK = 0x08000000;/* inherit all flags from previous definition */
    public static final int ROM_INHERITFLAGS = 0x08000000;

    public static final int ROM_INHERITEDFLAGS = (ROM_GROUPMASK | ROM_SKIPMASK | ROM_REVERSEMASK | ROM_BITWIDTHMASK | ROM_BITSHIFTMASK);

    /* ----- per-ROM macros ----- */
    public static String ROM_GETNAME(RomModule[] romp, int romp_ptr) {
        return romp[romp_ptr]._name;
    }

    /*TODO*///#define ROM_SAFEGETNAME(r)			(ROMENTRY_ISFILL(r) ? "fill" : ROMENTRY_ISCOPY(r) ? "copy" : ROM_GETNAME(r))
    public static int ROM_GETOFFSET(RomModule[] romp, int romp_ptr) {
        return romp[romp_ptr]._offset;
    }

    public static int ROM_GETCRC(RomModule[] romp, int romp_ptr) {
        return romp[romp_ptr]._crc;
    }

    public static int ROM_GETLENGTH(RomModule[] romp, int romp_ptr) {
        return (UNCOMPACT_LENGTH(romp[romp_ptr]._length & ROM_LENGTHMASK));
    }

    public static int ROM_GETFLAGS(RomModule[] romp, int romp_ptr) {
        return romp[romp_ptr]._length & ~ROM_LENGTHMASK;
    }

    public static boolean ROM_ISOPTIONAL(RomModule[] romp, int romp_ptr) {
        return ((ROM_GETFLAGS(romp, romp_ptr) & ROM_OPTIONALMASK) == ROM_OPTIONAL);
    }

    public static int ROM_GETGROUPSIZE(RomModule[] romp, int romp_ptr) {
        return (((ROM_GETFLAGS(romp, romp_ptr) & ROM_GROUPMASK) >> 12) + 1);
    }

    public static int ROM_GETSKIPCOUNT(RomModule[] romp, int romp_ptr) {
        return ((ROM_GETFLAGS(romp, romp_ptr) & ROM_SKIPMASK) >> 16);
    }

    public static boolean ROM_ISREVERSED(RomModule[] romp, int romp_ptr) {
        return ((ROM_GETFLAGS(romp, romp_ptr) & ROM_REVERSEMASK) == ROM_REVERSE);
    }

    public static int ROM_GETBITWIDTH(RomModule[] romp, int romp_ptr) {
        return (((ROM_GETFLAGS(romp, romp_ptr) & ROM_BITWIDTHMASK) >> 21) + 8 * (((ROM_GETFLAGS(romp, romp_ptr) & ROM_BITWIDTHMASK) == 0) ? 1 : 0));
    }

    public static int ROM_GETBITSHIFT(RomModule[] romp, int romp_ptr) {
        return ((ROM_GETFLAGS(romp, romp_ptr) & ROM_BITSHIFTMASK) >> 24);
    }

    public static boolean ROM_INHERITSFLAGS(RomModule[] romp, int romp_ptr) {
        return ((ROM_GETFLAGS(romp, romp_ptr) & ROM_INHERITFLAGSMASK) == ROM_INHERITFLAGS);
    }

    public static boolean ROM_NOGOODDUMP(RomModule[] romp, int romp_ptr) {
        return (ROM_GETCRC(romp, romp_ptr) == 0);
    }

    /**
     * *************************************************************************
     *
     * Derived macros for the ROM loading system
     *
     **************************************************************************
     */
    public static RomModule[] rommodule_macro = null;
    public static ArrayList<RomModule> arload = new ArrayList<>();

    /* ----- start/stop macros ----- */
    public static void ROM_END() {
        arload.add(new RomModule(ROMENTRY_END, 0, 0, 0));
        rommodule_macro = arload.toArray(new RomModule[arload.size()]);
        arload.clear();
    }

    /* ----- ROM region macros ----- */
    public static void ROM_REGION(int length, int type, int flags) {
        arload.add(new RomModule(ROMENTRY_REGION, flags, length, type));
    }

    /*TODO*///#define ROM_REGION16_LE(length,type,flags)			ROM_REGION(length, type, (flags) | ROMREGION_16BIT | ROMREGION_LE)
    public static void ROM_REGION16_BE(int length, int type, int flags){
        ROM_REGION(length, type, (flags) | ROMREGION_16BIT | ROMREGION_BE);
    }
    
/*TODO*///#define ROM_REGION32_LE(length,type,flags)			ROM_REGION(length, type, (flags) | ROMREGION_32BIT | ROMREGION_LE)
/*TODO*///#define ROM_REGION32_BE(length,type,flags)			ROM_REGION(length, type, (flags) | ROMREGION_32BIT | ROMREGION_BE)
/*TODO*///
    /* ----- core ROM loading macros ----- */
    public static void ROMX_LOAD(String name, int offset, int length, int crc, int flags) {
        arload.add(new RomModule(name, offset, (flags) | COMPACT_LENGTH(length), crc));
    }

    public static void ROM_LOAD(String name, int offset, int length, int crc) {
        ROMX_LOAD(name, offset, length, crc, 0);
    }

    public static void ROM_LOAD_OPTIONAL(String name, int offset, int length, int crc) {
        ROMX_LOAD(name, offset, length, crc, ROM_OPTIONAL);
    }

    public static void ROM_CONTINUE(int offset, int length) {
        ROMX_LOAD(ROMENTRY_CONTINUE, offset, length, 0, ROM_INHERITFLAGS);
    }

    public static void ROM_RELOAD(int offset, int length) {
        ROMX_LOAD(ROMENTRY_RELOAD, offset, length, 0, ROM_INHERITFLAGS);
    }
    
    public static void ROM_FILL(int offset, int length, int value){
        ROM_LOAD(ROMENTRY_FILL, offset, length, value);
    }
    
    public static void ROM_COPY(int rgn, int srcoffset, int offset, int length){
        ROMX_LOAD(ROMENTRY_COPY, offset, length, srcoffset, (rgn) << 24);
    }

    /* ----- nibble loading macros ----- */
    public static void ROM_LOAD_NIB_HIGH(String name, int offset, int length, int crc){
        ROMX_LOAD(name, offset, length, crc, ROM_NIBBLE | ROM_SHIFT_NIBBLE_HI);
    }
    public static void ROM_LOAD_NIB_LOW(String name, int offset, int length, int crc){
        ROMX_LOAD(name, offset, length, crc, ROM_NIBBLE | ROM_SHIFT_NIBBLE_LO);
    }

    /* ----- new-style 16-bit loading macros ----- */
    public static void ROM_LOAD16_BYTE(String name, int offset, int length, int crc){
            ROMX_LOAD(name, offset, length, crc, ROM_SKIP(1));
    }
/*TODO*///#define ROM_LOAD16_WORD(name,offset,length,crc)		ROM_LOAD(name, offset, length, crc)
    public static void ROM_LOAD16_WORD_SWAP(String name, int offset, int length, int crc){
        ROMX_LOAD(name, offset, length, crc, ROM_GROUPWORD() | ROM_REVERSE);
    }

/*TODO*////* ----- new-style 32-bit loading macros ----- */
/*TODO*///#define ROM_LOAD32_BYTE(name,offset,length,crc)		ROMX_LOAD(name, offset, length, crc, ROM_SKIP(3))
/*TODO*///#define ROM_LOAD32_WORD(name,offset,length,crc)		ROMX_LOAD(name, offset, length, crc, ROM_GROUPWORD | ROM_SKIP(2))
/*TODO*///#define ROM_LOAD32_WORD_SWAP(name,offset,length,crc)ROMX_LOAD(name, offset, length, crc, ROM_GROUPWORD | ROM_REVERSE | ROM_SKIP(2))
/*TODO*///
    public static final int COIN_COUNTERS = 4;/* total # of coin counters */

    public static int flip_screen() {
        return flip_screen_x[0];
    }
    
    /***************************************************************************

	Useful macros to deal with bit shuffling encryptions

    ***************************************************************************/

    public static int BITSWAP8(int val, int B7, int B6, int B5, int B4, int B3, int B2, int B1, int B0){
                    return (((((val) >> (B7)) & 1) << 7) | 
                     ((((val) >> (B6)) & 1) << 6) | 
                     ((((val) >> (B5)) & 1) << 5) | 
                     ((((val) >> (B4)) & 1) << 4) | 
                     ((((val) >> (B3)) & 1) << 3) | 
                     ((((val) >> (B2)) & 1) << 2) | 
                     ((((val) >> (B1)) & 1) << 1) | 
                     ((((val) >> (B0)) & 1) << 0));
    }

    public static int BITSWAP16(int val,int B15,int B14,int B13,int B12,int B11,int B10,int B9,int B8,int B7,int B6,int B5,int B4,int B3,int B2,int B1,int B0){
		return (((((val) >> (B15)) & 1) << 15) | 
		 ((((val) >> (B14)) & 1) << 14) | 
		 ((((val) >> (B13)) & 1) << 13) | 
		 ((((val) >> (B12)) & 1) << 12) | 
		 ((((val) >> (B11)) & 1) << 11) | 
		 ((((val) >> (B10)) & 1) << 10) | 
		 ((((val) >> ( B9)) & 1) <<  9) | 
		 ((((val) >> ( B8)) & 1) <<  8) | 
		 ((((val) >> ( B7)) & 1) <<  7) | 
		 ((((val) >> ( B6)) & 1) <<  6) | 
		 ((((val) >> ( B5)) & 1) <<  5) | 
		 ((((val) >> ( B4)) & 1) <<  4) | 
		 ((((val) >> ( B3)) & 1) <<  3) | 
		 ((((val) >> ( B2)) & 1) <<  2) | 
		 ((((val) >> ( B1)) & 1) <<  1) | 
		 ((((val) >> ( B0)) & 1) <<  0));
    }

/*TODO*///#define BITSWAP24(val,B23,B22,B21,B20,B19,B18,B17,B16,B15,B14,B13,B12,B11,B10,B9,B8,B7,B6,B5,B4,B3,B2,B1,B0) \
/*TODO*///		(((((val) >> (B23)) & 1) << 23) | \
/*TODO*///		 ((((val) >> (B22)) & 1) << 22) | \
/*TODO*///		 ((((val) >> (B21)) & 1) << 21) | \
/*TODO*///		 ((((val) >> (B20)) & 1) << 20) | \
/*TODO*///		 ((((val) >> (B19)) & 1) << 19) | \
/*TODO*///		 ((((val) >> (B18)) & 1) << 18) | \
/*TODO*///		 ((((val) >> (B17)) & 1) << 17) | \
/*TODO*///		 ((((val) >> (B16)) & 1) << 16) | \
/*TODO*///		 ((((val) >> (B15)) & 1) << 15) | \
/*TODO*///		 ((((val) >> (B14)) & 1) << 14) | \
/*TODO*///		 ((((val) >> (B13)) & 1) << 13) | \
/*TODO*///		 ((((val) >> (B12)) & 1) << 12) | \
/*TODO*///		 ((((val) >> (B11)) & 1) << 11) | \
/*TODO*///		 ((((val) >> (B10)) & 1) << 10) | \
/*TODO*///		 ((((val) >> ( B9)) & 1) <<  9) | \
/*TODO*///		 ((((val) >> ( B8)) & 1) <<  8) | \
/*TODO*///		 ((((val) >> ( B7)) & 1) <<  7) | \
/*TODO*///		 ((((val) >> ( B6)) & 1) <<  6) | \
/*TODO*///		 ((((val) >> ( B5)) & 1) <<  5) | \
/*TODO*///		 ((((val) >> ( B4)) & 1) <<  4) | \
/*TODO*///		 ((((val) >> ( B3)) & 1) <<  3) | \
/*TODO*///		 ((((val) >> ( B2)) & 1) <<  2) | \
/*TODO*///		 ((((val) >> ( B1)) & 1) <<  1) | \
/*TODO*///		 ((((val) >> ( B0)) & 1) <<  0))
/*TODO*///
/*TODO*///   
}
