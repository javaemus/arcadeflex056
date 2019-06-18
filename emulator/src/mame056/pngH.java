/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056;

import static common.ptr.*;
import static mame056.drawgfxH.*;

public class pngH {
    /*TODO*///#define PNG_Signature       "\x89\x50\x4E\x47\x0D\x0A\x1A\x0A"
    /*TODO*///#define MNG_Signature       "\x8A\x4D\x4E\x47\x0D\x0A\x1A\x0A"
    /*TODO*///
    /*TODO*///#define PNG_CN_IHDR 0x49484452L     /* Chunk names */
    /*TODO*///#define PNG_CN_PLTE 0x504C5445L
    /*TODO*///#define PNG_CN_IDAT 0x49444154L
    /*TODO*///#define PNG_CN_IEND 0x49454E44L
    /*TODO*///#define PNG_CN_gAMA 0x67414D41L
    /*TODO*///#define PNG_CN_sBIT 0x73424954L
    /*TODO*///#define PNG_CN_cHRM 0x6348524DL
    /*TODO*///#define PNG_CN_tRNS 0x74524E53L
    /*TODO*///#define PNG_CN_bKGD 0x624B4744L
    /*TODO*///#define PNG_CN_hIST 0x68495354L
    /*TODO*///#define PNG_CN_tEXt 0x74455874L
    /*TODO*///#define PNG_CN_zTXt 0x7A545874L
    /*TODO*///#define PNG_CN_pHYs 0x70485973L
    /*TODO*///#define PNG_CN_oFFs 0x6F464673L
    /*TODO*///#define PNG_CN_tIME 0x74494D45L
    /*TODO*///#define PNG_CN_sCAL 0x7343414CL
    /*TODO*///
    /*TODO*///#define PNG_PF_None     0   /* Prediction filters */
    /*TODO*///#define PNG_PF_Sub      1
    /*TODO*///#define PNG_PF_Up       2
    /*TODO*///#define PNG_PF_Average  3
    /*TODO*///#define PNG_PF_Paeth    4
    /*TODO*///
    /*TODO*///#define MNG_CN_MHDR 0x4D484452L     /* MNG Chunk names */
    /*TODO*///#define MNG_CN_MEND 0x4D454E44L
    /*TODO*///#define MNG_CN_TERM 0x5445524DL
    /*TODO*///#define MNG_CN_BACK 0x4241434BL
    
    
    /* PNG support */
    public static class png_info {
    	public int width, height;
    	public int xres, yres;
    	public rectangle screen = new rectangle();
    	public double xscale, yscale;
    	public double source_gamma;
    	public int[] chromaticities = new int[8];
    	public int resolution_unit, offset_unit, scale_unit;
    	public int bit_depth;
    	public int[] significant_bits = new int[4];
    	public int[] background_color = new int[4];
    	public int color_type;
    	public int compression_method;
    	public int filter_method;
    	public int interlace_method;
    	public int num_palette;
    	public UBytePtr palette = new UBytePtr();
    	public int num_trans;
    	public UBytePtr trans = new UBytePtr();
    	public UBytePtr image = new UBytePtr();
    
    	/* The rest is private and should not be used
    	 * by the public functions
    	 */
    	public int bpp;
    	public int rowbytes;
    	public UBytePtr zimage = new UBytePtr();
    	public int zlength;
    	public UBytePtr fimage = new UBytePtr();
        
    };
}
