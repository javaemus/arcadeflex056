/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package WIP.mame056.vidhrdw;


public class konamiicH {

    public static final int MAX_K007121 = 2;

    /*
    You don't have to decode the graphics: the vh_start() routines will do that
    for you, using the plane order passed.
    Of course the ROM data must be in the correct order. This is a way to ensure
    that the ROM test will pass.
    The konami_rom_deinterleave() function above will do the reorganization for
    you in most cases (but see tmnt.c for additional bit rotations or byte
    permutations which may be required).
    */
    /*TODO*///#define NORMAL_PLANE_ORDER 0,1,2,3
    /*TODO*///REVERSE_PLANE_ORDER 3,2,1,0

    public static final int K053251_CI0=0;
    public static final int K053251_CI1=1;
    public static final int K053251_CI2=2;
    public static final int K053251_CI3=3;
    public static final int K053251_CI4=4;

}
