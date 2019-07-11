/**
 * ported to v0.56
 */
package mame056;

import static arcadeflex056.fucPtr.ReadHandlerPtr;
import static common.libc.cstring.*;
import static common.ptr.*;
import static common.subArrays.*;
import static java.lang.Math.abs;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.driverH.*;
import static mame056.mame.*;
import static mame056.tilemapH.*;

public class tilemapC {

    public static void SWAP(int X, int Y) { int temp=X; X=Y; Y=temp; }
    public static int MAX_TILESIZE = 32;

    public static int TILE_FLAG_DIRTY	= (0x80);

    public static int eWHOLLY_TRANSPARENT   = 0;
    public static int eWHOLLY_OPAQUE        = 1;
    public static int eMASKED               = 2;

    public static abstract interface DrawHandlerPtr { public abstract void handler(struct_tilemap tilemap, int xpos, int ypos, int mask, int value); }
    public static abstract interface DrawTileHandlerPtr { public abstract int handler( struct_tilemap tilemap, int col, int row, int flags );}
    
    
    public static mame_bitmap priority_bitmap = bitmap_alloc_depth(Machine.scrbitmap.width, Machine.scrbitmap.height, 16);;
    //public static mame_bitmap priority_bitmap;

    public static int/*UINT32*/ priority_bitmap_pitch_line;
    public static int/*UINT32*/ priority_bitmap_pitch_row;

    static struct_tilemap first_tilemap = null;/* resource tracking */
    static int/*UINT32*/ screen_width, screen_height;
    public static struct_tile_info tile_info = new struct_tile_info();

    public static abstract interface blitmask_t { public abstract void handler(UShortPtr dest, UShortPtr source, UShortPtr pMask, int mask, int value, int count, UShortPtr pri, int pcode); }
/*TODO*///typedef void (*blitmask_t)( void *dest, const void *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///typedef void (*blitopaque_t)( void *dest, const void *source, int count, UINT8 *pri, UINT32 pcode );
    public static abstract interface blitopaque_t { public abstract void handler(UShortPtr dest, UShortPtr source, int count, UShortPtr pri, int pcode); }

    /* the following parameters are constant across tilemap_draw calls */
    public static class _blit
    {
        public blitmask_t draw_masked;
        public blitopaque_t draw_opaque;
	public int clip_left, clip_top, clip_right, clip_bottom;
	public int tilemap_priority_code;
	public mame_bitmap screen_bitmap;
	public int screen_bitmap_pitch_line;
	public int screen_bitmap_pitch_row;
    };
    
    public static _blit blit = new _blit();

/*TODO*////***********************************************************************************/
/*TODO*///
/*TODO*///static int PenToPixel_Init( struct tilemap *tilemap );
/*TODO*///static void PenToPixel_Term( struct tilemap *tilemap );
/*TODO*///static int mappings_create( struct tilemap *tilemap );
/*TODO*///static void mappings_dispose( struct tilemap *tilemap );
/*TODO*///static void mappings_update( struct tilemap *tilemap );
/*TODO*///static void recalculate_scroll( struct tilemap *tilemap );
/*TODO*///
/*TODO*////* {p/n}{blend/draw/invis}{opaque/trans}{16/32} */
/*TODO*///static void pio( void *dest, const void *source, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///static void pit( void *dest, const void *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///
/*TODO*///static void pdo16( UINT16 *dest, const UINT16 *source, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///static void pdo15( UINT16 *dest, const UINT16 *source, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///static void pbo15( UINT16 *dest, const UINT16 *source, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///static void pdo32( UINT32 *dest, const UINT16 *source, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///static void pbo32( UINT32 *dest, const UINT16 *source, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///
/*TODO*///static void pdt16( UINT16 *dest, const UINT16 *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///static void pdt15( UINT16 *dest, const UINT16 *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///static void pbt15( UINT16 *dest, const UINT16 *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///static void pdt32( UINT32 *dest, const UINT16 *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///static void pbt32( UINT32 *dest, const UINT16 *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///
/*TODO*///static void install_draw_handlers( struct tilemap *tilemap );
/*TODO*///static void tilemap_reset(void);
/*TODO*///
/*TODO*///static void update_tile_info( struct tilemap *tilemap, UINT32 cached_indx, UINT32 cached_col, UINT32 cached_row );

    /***********************************************************************************/

    static int PenToPixel_Init( struct_tilemap tilemap )
    {
            /*
                    Construct a table for all tile orientations in advance.
                    This simplifies drawing tiles and masks tremendously.
                    If performance is an issue, we can always (re)introduce
                    customized code for each case and forgo tables.
            */
            int i,x,y,tx,ty;
            IntArray pPenToPixel;
            int lError;

            lError = 0;
            for( i=0; i<8; i++ )
            {
                    pPenToPixel = new IntArray( tilemap.num_pens );
                    if( pPenToPixel==null )
                    {
                            lError = 1;
                    }
                    else
                    {
                            tilemap.pPenToPixel.write(i, pPenToPixel.read());
                            for( ty=0; ty<tilemap.logical_tile_height; ty++ )
                            {
                                    for( tx=0; tx<tilemap.logical_tile_width; tx++ )
                                    {
                                            if (( i&TILE_SWAPXY ) != 0)
                                            {
                                                    x = ty;
                                                    y = tx;
                                            }
                                            else
                                            {
                                                    x = tx;
                                                    y = ty;
                                            }
                                            if (( i&TILE_FLIPX ) != 0) x = tilemap.cached_tile_width-1-x;
                                            if (( i&TILE_FLIPY ) != 0) y = tilemap.cached_tile_height-1-y;
                                            pPenToPixel.writeinc( x+y*MAX_TILESIZE );
                                    }
                            }
                    }
            }
            return lError;
    }

/*TODO*///static void PenToPixel_Term( struct tilemap *tilemap )
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	for( i=0; i<8; i++ )
/*TODO*///	{
/*TODO*///		free( tilemap->pPenToPixel[i] );
/*TODO*///	}
/*TODO*///}
/*TODO*///
    public static void tilemap_set_transparent_pen(struct_tilemap tilemap, int pen) {
        tilemap.transparent_pen = pen;
    }

    
    public static void tilemap_set_transmask( struct_tilemap tilemap, int which, int fgmask, int bgmask )
    {
            if( tilemap.fgmask[which] != fgmask || tilemap.bgmask[which] != bgmask )
            {
                    tilemap.fgmask[which] = fgmask;
                    tilemap.bgmask[which] = bgmask;
                    tilemap_mark_all_tiles_dirty( tilemap );
            }
    }

    public static void tilemap_set_depth(struct_tilemap tilemap, int tile_depth, int tile_granularity) {
        if (tilemap.tile_dirty_map != null) {
            tilemap.tile_dirty_map = null;
        }
        tilemap.tile_dirty_map = new UBytePtr(Machine.drv.total_colors >> tile_granularity);
        if (tilemap.tile_dirty_map != null) {
            tilemap.tile_depth = (char) tile_depth;
            tilemap.tile_granularity = (char) tile_granularity;
        }
    }
    /**
     * ********************************************************************************
     */
    /* some common mappings */
    public static GetMemoryOffsetPtr tilemap_scan_rows = new GetMemoryOffsetPtr() {
        public int handler(int u32_col, int u32_row, int u32_num_cols, int u32_num_rows) {
            /* logical (col,row) -> memory offset */
            return u32_row * u32_num_cols + u32_col;
        }
    };
    public static GetMemoryOffsetPtr tilemap_scan_cols = new GetMemoryOffsetPtr() {
        public int handler(int u32_col, int u32_row, int u32_num_cols, int u32_num_rows) {
            /* logical (col,row) -> memory offset */
            return u32_col * u32_num_rows + u32_row;
        }
    };

    /***********************************************************************************/

    static int mappings_create( struct_tilemap tilemap )
    {
            int max_memory_offset = 0;
            int col,row;
            int num_logical_rows = tilemap.num_logical_rows;
            int num_logical_cols = tilemap.num_logical_cols;
            /* count offsets (might be larger than num_tiles) */
            for( row=0; row<num_logical_rows; row++ )
            {
                    for( col=0; col<num_logical_cols; col++ )
                    {
                            int memory_offset = tilemap.get_memory_offset.handler(col, row, num_logical_cols, num_logical_rows );
                            if( memory_offset>max_memory_offset ) max_memory_offset = memory_offset;
                    }
            }
            max_memory_offset++;
            tilemap.max_memory_offset = max_memory_offset;
            /* logical to cached (tilemap_mark_dirty) */
            tilemap.memory_offset_to_cached_indx = new int[max_memory_offset];
            if( tilemap.memory_offset_to_cached_indx != null )
            {
                    /* cached to logical (get_tile_info) */
                    tilemap.cached_indx_to_memory_offset = new int[ tilemap.num_tiles * 2 ];
                    if( tilemap.cached_indx_to_memory_offset != null ) return 0; /* no error */
                    tilemap.memory_offset_to_cached_indx = null;
            }
            return -1; /* error */
    }

/*TODO*///static void mappings_dispose( struct tilemap *tilemap )
/*TODO*///{
/*TODO*///	free( tilemap->cached_indx_to_memory_offset );
/*TODO*///	free( tilemap->memory_offset_to_cached_indx );
/*TODO*///}

    public static void mappings_update( struct_tilemap tilemap )
    {
	int logical_flip;
	int logical_indx, cached_indx;
	int num_cached_rows = tilemap.num_cached_rows;
	int num_cached_cols = tilemap.num_cached_cols;
	int num_logical_rows = tilemap.num_logical_rows;
	int num_logical_cols = tilemap.num_logical_cols;
	for( logical_indx=0; logical_indx<tilemap.max_memory_offset; logical_indx++ )
	{
		tilemap.memory_offset_to_cached_indx[logical_indx] = -1;
	}

	for( logical_indx=0; logical_indx<tilemap.num_tiles; logical_indx++ )
	{
		int logical_col = logical_indx%num_logical_cols;
		int logical_row = logical_indx/num_logical_cols;
		int memory_offset = tilemap.get_memory_offset.handler(logical_col, logical_row, num_logical_cols, num_logical_rows );
		int cached_col = logical_col;
		int cached_row = logical_row;
		if (( tilemap.orientation & ORIENTATION_SWAP_XY ) != 0) SWAP(cached_col,cached_row);
		if (( tilemap.orientation & ORIENTATION_FLIP_X ) != 0) cached_col = (num_cached_cols-1)-cached_col;
		if (( tilemap.orientation & ORIENTATION_FLIP_Y ) != 0) cached_row = (num_cached_rows-1)-cached_row;
		cached_indx = abs(cached_row*num_cached_cols+cached_col);
		tilemap.memory_offset_to_cached_indx[memory_offset] = cached_indx;
		tilemap.cached_indx_to_memory_offset[cached_indx] = memory_offset;
	}
	for( logical_flip = 0; logical_flip<4; logical_flip++ )
	{
		int cached_flip = logical_flip;
		if (( tilemap.attributes&TILEMAP_FLIPX ) != 0) cached_flip ^= TILE_FLIPX;
		if (( tilemap.attributes&TILEMAP_FLIPY ) != 0) cached_flip ^= TILE_FLIPY;
/*TODO*///#ifndef PREROTATE_GFX
/*TODO*///		if( Machine.orientation & ORIENTATION_SWAP_XY )
/*TODO*///		{
/*TODO*///			if( Machine.orientation & ORIENTATION_FLIP_X ) cached_flip ^= TILE_FLIPY;
/*TODO*///			if( Machine.orientation & ORIENTATION_FLIP_Y ) cached_flip ^= TILE_FLIPX;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			if( Machine.orientation & ORIENTATION_FLIP_X ) cached_flip ^= TILE_FLIPX;
/*TODO*///			if( Machine.orientation & ORIENTATION_FLIP_Y ) cached_flip ^= TILE_FLIPY;
/*TODO*///		}
/*TODO*///#endif
		if (( tilemap.orientation & ORIENTATION_SWAP_XY ) != 0)
		{
			cached_flip = ((cached_flip&1)<<1) | ((cached_flip&2)>>1);
		}
		tilemap.logical_flip_to_cached_flip[logical_flip] = cached_flip;
	}
    }

/*TODO*////***********************************************************************************/
/*TODO*///
/*TODO*///static void pio( void *dest, const void *source, int count, UINT8 *pri, UINT32 pcode )
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	for( i=0; i<count; i++ )
/*TODO*///	{
/*TODO*///		pri[i] |= pcode;
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///static void pit( void *dest, const void *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode )
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	for( i=0; i<count; i++ )
/*TODO*///	{
/*TODO*///		if( (pMask[i]&mask)==value )
/*TODO*///		{
/*TODO*///			pri[i] |= pcode;
/*TODO*///		}
/*TODO*///	}
/*TODO*///}

    /***********************************************************************************/

    public static blitopaque_t pdo16 = new blitopaque_t() {
        public void handler(UShortPtr dest, UShortPtr source, int count, UShortPtr pri, int pcode) {
            int i;
            memcpy( dest,source,count * 2 ); // size*2 ????
            for( i=0; i<count; i++ )
            {
                    pri.write(i, (char) (pri.read(i) | pcode));
            }
        }
    };
    
/*TODO*///static void pdo15( UINT16 *dest, const UINT16 *source, int count, UINT8 *pri, UINT32 pcode )
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	pen_t *clut = Machine->remapped_colortable;
/*TODO*///	for( i=0; i<count; i++ )
/*TODO*///	{
/*TODO*///		dest[i] = clut[source[i]];
/*TODO*///		pri[i] |= pcode;
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///static void pdo32( UINT32 *dest, const UINT16 *source, int count, UINT8 *pri, UINT32 pcode )
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	pen_t *clut = Machine->remapped_colortable;
/*TODO*///	for( i=0; i<count; i++ )
/*TODO*///	{
/*TODO*///		dest[i] = clut[source[i]];
/*TODO*///		pri[i] |= pcode;
/*TODO*///	}
/*TODO*///}

    /***********************************************************************************/

    public static blitmask_t pdt16 = new blitmask_t() {
        public void handler( UShortPtr dest, UShortPtr source, UShortPtr pMask, int mask, int value, int count, UShortPtr pri, int pcode ) {
            int i;
            for( i=0; i<count*2; i++ )
            {
                    if( (pMask.read(i)&mask)==value )
                    {
                            dest.write(i, source.read(i));
                            pri.write(i, (char) (pri.read(i) | pcode));
                    }
            }
        }
    };
    
/*TODO*///static void pdt15( UINT16 *dest, const UINT16 *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode )
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	pen_t *clut = Machine->remapped_colortable;
/*TODO*///	for( i=0; i<count; i++ )
/*TODO*///	{
/*TODO*///		if( (pMask[i]&mask)==value )
/*TODO*///		{
/*TODO*///			dest[i] = clut[source[i]];
/*TODO*///			pri[i] |= pcode;
/*TODO*///		}
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///static void pdt32( UINT32 *dest, const UINT16 *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode )
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	pen_t *clut = Machine->remapped_colortable;
/*TODO*///	for( i=0; i<count; i++ )
/*TODO*///	{
/*TODO*///		if( (pMask[i]&mask)==value )
/*TODO*///		{
/*TODO*///			dest[i] = clut[source[i]];
/*TODO*///			pri[i] |= pcode;
/*TODO*///		}
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*////***********************************************************************************/
/*TODO*///
/*TODO*///static void pbo15( UINT16 *dest, const UINT16 *source, int count, UINT8 *pri, UINT32 pcode )
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	pen_t *clut = Machine->remapped_colortable;
/*TODO*///	for( i=0; i<count; i++ )
/*TODO*///	{
/*TODO*///		dest[i] = alpha_blend16(dest[i], clut[source[i]]);
/*TODO*///		pri[i] |= pcode;
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///static void pbo32( UINT32 *dest, const UINT16 *source, int count, UINT8 *pri, UINT32 pcode )
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	pen_t *clut = Machine->remapped_colortable;
/*TODO*///	for( i=0; i<count; i++ )
/*TODO*///	{
/*TODO*///		dest[i] = alpha_blend32(dest[i], clut[source[i]]);
/*TODO*///		pri[i] |= pcode;
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*////***********************************************************************************/
/*TODO*///
/*TODO*///static void pbt15( UINT16 *dest, const UINT16 *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode )
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	pen_t *clut = Machine->remapped_colortable;
/*TODO*///	for( i=0; i<count; i++ )
/*TODO*///	{
/*TODO*///		if( (pMask[i]&mask)==value )
/*TODO*///		{
/*TODO*///			dest[i] = alpha_blend16(dest[i], clut[source[i]]);
/*TODO*///			pri[i] |= pcode;
/*TODO*///		}
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///static void pbt32( UINT32 *dest, const UINT16 *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode )
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	pen_t *clut = Machine->remapped_colortable;
/*TODO*///	for( i=0; i<count; i++ )
/*TODO*///	{
/*TODO*///		if( (pMask[i]&mask)==value )
/*TODO*///		{
/*TODO*///			dest[i] = alpha_blend32(dest[i], clut[source[i]]);
/*TODO*///			pri[i] |= pcode;
/*TODO*///		}
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*////***********************************************************************************/
/*TODO*///
/*TODO*///#define DEPTH 16
/*TODO*///#define DATA_TYPE UINT16
/*TODO*///#define DECLARE(function,args,body) static void function##16BPP args body
/*TODO*///#include "tilemap.c"
/*TODO*///
/*TODO*///#define DEPTH 32
/*TODO*///#define DATA_TYPE UINT32
/*TODO*///#define DECLARE(function,args,body) static void function##32BPP args body
/*TODO*///#include "tilemap.c"
/*TODO*///
/*TODO*///#define PAL_INIT const pen_t *pPalData = tile_info.pal_data
    /*TODO*///public static int PAL_GET(int pen){
    /*TODO*///    return tile_info.pal_data.read(pen);
    /*TODO*///}
/*TODO*///#define TRANSP(f) f ## _ind
/*TODO*///#include "tilemap.c"
/*TODO*///
    static IntArray palbase;
    public static void PAL_INIT(){
        //int palBase = tile_info.pal_data - Machine.remapped_colortable;
        palbase = tile_info.pal_data;
    }
    public static int PAL_GET(int pen){ 
        //return (palBase + (pen));
        return palbase.read(pen);
    }
/*TODO*///#define TRANSP(f) f ## _raw
/*TODO*///#include "tilemap.c"
/*TODO*///
    /*********************************************************************************/
    static void install_draw_handlers(struct_tilemap tilemap) {
        int tile_width = tilemap.cached_tile_width;
        int tile_height = tilemap.cached_tile_height;
        //tilemap.draw = tilemap.draw_opaque = null;
        /*TODO*///	if( Machine->scrbitmap->depth==16 ){
/*TODO*///		if( tile_width==8 && tile_height==8 ){
/*TODO*///			tilemap->draw = draw8x8x16BPP;
/*TODO*///			tilemap->draw_opaque = draw_opaque8x8x16BPP;
/*TODO*///		}
/*TODO*///		else if( tile_width==16 && tile_height==16 ){
/*TODO*///			tilemap->draw = draw16x16x16BPP;
/*TODO*///			tilemap->draw_opaque = draw_opaque16x16x16BPP;
/*TODO*///		}
/*TODO*///		else if( tile_width==32 && tile_height==32 ){
/*TODO*///			tilemap->draw = draw32x32x16BPP;
/*TODO*///			tilemap->draw_opaque = draw_opaque32x32x16BPP;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else {
        if (tile_width == 8 && tile_height == 8) {
            tilemap.draw = draw16BPP;
            tilemap.draw_tile = tilemap.draw_tile = dummy_plot;
            //tilemap.draw = draw8x8x8BPP;
            //tilemap.draw_opaque = draw_opaque8x8x8BPP;
            System.out.println("draw8x8x8BPP");
        } else if (tile_width == 16 && tile_height == 16) {
            tilemap.draw = draw16BPP;
            tilemap.draw_tile = tilemap.draw_tile = dummy_plot;
            //tilemap.draw = draw16x16x8BPP;
            //tilemap.draw_opaque = draw_opaque16x16x8BPP;
            System.out.println("draw16x16xBPP");
        } else if (tile_width == 32 && tile_height == 32) {
            tilemap.draw = draw8BPP;
            //tilemap.draw = draw32x32x8BPP;
            //tilemap.draw_opaque = draw_opaque32x32x8BPP;
            System.out.println("draw32x32x8BPP");
        }
        /*TODO*///	}
    }
    
    public static DrawTileHandlerPtr dummy_plot = new DrawTileHandlerPtr() {
        public int handler(struct_tilemap tilemap, int x0, int y0, int flags) {
            System.out.println("dummy");
            for (int y=0 ; y<100 ; y++){
                UShortPtr pp = new UShortPtr(priority_bitmap.line[y]);
                for (int i=0 ; i<100 ; i++){
                    pp.write((char) 2);
                    pp.inc();
                }
            }
            return 1;
        }
    };
    
    static void install_draw_handlers056( struct_tilemap tilemap )
    {
	tilemap.draw = null;
        //System.out.println("tilemap->type "+tilemap.type);
        //System.out.println("Machine->game_colortable "+(Machine.game_colortable != null));

	if( Machine.game_colortable != null )
	{
		if (( tilemap.type & TILEMAP_BITMASK ) != 0) {
                    System.out.println("HandleTransparencyBitmask_ind 1");
			/*TODO*///tilemap.draw_tile = HandleTransparencyBitmask_ind;
                } else if (( tilemap.type & TILEMAP_SPLIT_PENBIT ) != 0) {
                    System.out.println("HandleTransparencyPenBit_ind 2");
			/*TODO*///tilemap.draw_tile = HandleTransparencyPenBit_ind;
                } else if (( tilemap.type & TILEMAP_SPLIT ) != 0) {
                    System.out.println("HandleTransparencyPens_ind 3");
			/*TODO*///tilemap.draw_tile = HandleTransparencyPens_ind;
                } else if ( tilemap.type==TILEMAP_TRANSPARENT ) {  
                    System.out.println("HandleTransparencyPen_ind 4");
			tilemap.draw_tile = HandleTransparencyPen_ind;
                } else if ( tilemap.type==TILEMAP_TRANSPARENT_COLOR ) {
                    System.out.println("HandleTransparencyColor_ind 5");
			tilemap.draw_tile = HandleTransparencyColor_ind;
                } else {
                    System.out.println("HandleTransparencyNone_ind 6");
			tilemap.draw_tile = HandleTransparencyNone_ind;
                }
	}
	else
	{
            //System.out.println("colortable es null");
		if (( tilemap.type & TILEMAP_BITMASK ) != 0) {
                    System.out.println("HandleTransparencyBitmask_raw 1");
/*TODO*///			tilemap.draw_tile = HandleTransparencyBitmask_raw;
		} else if (( tilemap.type & TILEMAP_SPLIT_PENBIT ) != 0) {
                    System.out.println("HandleTransparencyPenBit_raw 2");
/*TODO*///			tilemap.draw_tile = HandleTransparencyPenBit_raw;
		} else if (( tilemap.type & TILEMAP_SPLIT ) != 0) {
                    System.out.println("HandleTransparencyPens_raw 3");
			tilemap.draw_tile = HandleTransparencyPens_raw;
		} else if ( tilemap.type==TILEMAP_TRANSPARENT ) {
                    System.out.println("HandleTransparencyPen_raw 4");
			tilemap.draw_tile = HandleTransparencyPen_raw;
		} else if ( tilemap.type==TILEMAP_TRANSPARENT_COLOR ) {
                    System.out.println("HandleTransparencyColor_raw 5");
/*TODO*///			tilemap.draw_tile = HandleTransparencyColor_raw;
		} else {
                    tilemap.draw_tile = HandleTransparencyNone_raw;
                }
	}
	switch( Machine.scrbitmap.depth )
	{
/*TODO*///	case 32:
/*TODO*///		tilemap.draw			= draw32BPP;
/*TODO*///		break;
/*TODO*///
/*TODO*///	case 15:
	case 16:
		tilemap.draw			= draw16BPP;
		break;

	default:
		System.exit(1);
		break;
	}
    }

    /***********************************************************************************/

    static void tilemap_reset()
    {
            tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
    }
    
    public static int tilemap_init() {
        // old methods
        return mame037b11.mame.tilemapC.tilemap_init();
        // end old method
        
        /*TODO*///screen_width = Machine.scrbitmap.width;
        /*TODO*///screen_height = Machine.scrbitmap.height;
        /*TODO*///first_tilemap = null;
        /*TODO*///	state_save_register_func_postload(tilemap_reset);
        /*TODO*///tilemap_reset();
        /*TODO*///priority_bitmap = bitmap_alloc_depth(screen_width, screen_height, 16);
        /*TODO*///if (priority_bitmap != null) {
        /*TODO*///    priority_bitmap_pitch_line = priority_bitmap.line[1].offset - priority_bitmap.line[0].offset;
        /*TODO*///    return 0;
        /*TODO*///} else {
        /*TODO*///    System.out.println("priority_bitmap==null");
        /*TODO*///}
        /*TODO*///return -1;
    }

    public static void tilemap_close() {
        /*TODO*///struct_tilemap next;

        /*TODO*///while (first_tilemap != null) {
        /*TODO*///    next = first_tilemap.next;
        /*TODO*///    tilemap_dispose(first_tilemap);
        /*TODO*///    first_tilemap = next;
        /*TODO*///}
        /*TODO*///bitmap_free(priority_bitmap);
    }

    /**
     * ********************************************************************************
     */
    public static struct_tilemap tilemap_create(GetTileInfoPtr tile_get_info,
            GetMemoryOffsetPtr get_memory_offset,
            int type,
            int tile_width,
            int tile_height,
            int num_cols,
            int num_rows) {
        int/*UINT32*/ row;
        int num_tiles;

        struct_tilemap tilemap = new struct_tilemap();
        if (tilemap != null) {
            System.out.println("dummy tilemap_create type =" + type + " tile_width= " + tile_width + " tile_height =" + tile_height + " num_cols= " + num_cols + " num_rows= " + num_rows);
            num_tiles = num_cols * num_rows;
            tilemap.num_logical_cols = num_cols;
            tilemap.num_logical_rows = num_rows;
            if ((Machine.orientation & ORIENTATION_SWAP_XY) != 0) {
                SWAP( num_cols,num_rows );
                int temp2 = num_cols;
                num_cols = num_rows;
                num_rows = temp2;
                SWAP( tile_width, tile_height );
                int temp = tile_width;
                tile_width = tile_height;
                tile_height = temp;

            }
            tilemap.num_cached_cols = num_cols;
            tilemap.num_cached_rows = num_rows;
            tilemap.num_tiles = num_tiles;
            tilemap.num_pens = tile_width * tile_height;
            tilemap.logical_tile_width = tile_width;
            tilemap.logical_tile_height = tile_height;
            tilemap.cached_tile_width = tile_width;
            tilemap.cached_tile_height = tile_height;
            tilemap.cached_width = tile_width * num_cols;
            tilemap.cached_height = tile_height * num_rows;
            tilemap.tile_get_info = tile_get_info;
            tilemap.get_memory_offset = get_memory_offset;
            tilemap.orientation = Machine.orientation;

            /* various defaults */
            tilemap.enable = 1;
            tilemap.type = type;
            tilemap.logical_scroll_rows = tilemap.cached_scroll_rows = 1;
            tilemap.logical_scroll_cols = tilemap.cached_scroll_cols = 1;
            tilemap.transparent_pen = -1;
            tilemap.tile_depth = 0;
            tilemap.tile_granularity = 0;
            tilemap.tile_dirty_map = null;
            /*TODO*///
            tilemap.logical_rowscroll	= new int[tilemap.cached_height];
            tilemap.cached_rowscroll	= new int[tilemap.cached_height];
            tilemap.logical_colscroll	= new int[tilemap.cached_width];
            tilemap.cached_colscroll	= new int[tilemap.cached_width];

            tilemap.transparency_data = new UBytePtr( num_tiles * 1024 );
            tilemap.transparency_data_row = new UBytePtr[ num_rows ];
            for (int i = 0 ; i < num_rows ; i++)
                tilemap.transparency_data_row[i] = new UBytePtr();

            tilemap.pixmap = bitmap_alloc_depth( tilemap.cached_width, tilemap.cached_height, -16 );
            tilemap.transparency_bitmap = bitmap_alloc_depth( tilemap.cached_width, tilemap.cached_height, -8 );

		if( tilemap.logical_rowscroll!=null && tilemap.cached_rowscroll!=null &&
			tilemap.logical_colscroll!=null && tilemap.cached_colscroll!=null &&
			tilemap.pixmap!=null &&
			tilemap.transparency_data!=null &&
			tilemap.transparency_data_row!=null &&
			tilemap.transparency_bitmap!=null &&
			(mappings_create( tilemap )==0) )
		{
			tilemap.pixmap_pitch_line = (new UBytePtr(tilemap.pixmap.line[1]).read() - (new UBytePtr(tilemap.pixmap.line[0]).read()))/2;
			tilemap.pixmap_pitch_row = tilemap.pixmap_pitch_line*tile_height;

			tilemap.transparency_bitmap_pitch_line = (new UBytePtr(tilemap.transparency_bitmap.line[1]).read()-(new UBytePtr(tilemap.transparency_bitmap.line[0]).read()));
			tilemap.transparency_bitmap_pitch_row = tilemap.transparency_bitmap_pitch_line*tile_height;

			for( row=0; row<num_rows; row++ )
			{
				tilemap.transparency_data_row[row] = new UBytePtr(tilemap.transparency_data, num_cols*row);
			}
			install_draw_handlers056( tilemap );
			mappings_update( tilemap );
			tilemap_set_clip( tilemap, Machine.visible_area );
			memset( tilemap.transparency_data, TILE_FLAG_DIRTY, num_tiles );
			tilemap.next = first_tilemap;
			first_tilemap = tilemap;
			if( PenToPixel_Init( tilemap ) == 0 )
			{
                            return tilemap;
            		}
		}
		tilemap_dispose( tilemap );
        }
        return null;
    }

    public static void tilemap_dispose(struct_tilemap tilemap) {
        System.out.println("dummy tilemap_dispose");
        /*TODO*///	struct tilemap *prev;
/*TODO*///
/*TODO*///	if( tilemap==first_tilemap )
/*TODO*///	{
/*TODO*///		first_tilemap = tilemap->next;
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		prev = first_tilemap;
/*TODO*///		while( prev->next != tilemap ) prev = prev->next;
/*TODO*///		prev->next =tilemap->next;
/*TODO*///	}
/*TODO*///	PenToPixel_Term( tilemap );
/*TODO*///	free( tilemap->logical_rowscroll );
/*TODO*///	free( tilemap->cached_rowscroll );
/*TODO*///	free( tilemap->logical_colscroll );
/*TODO*///	free( tilemap->cached_colscroll );
/*TODO*///	free( tilemap->transparency_data );
/*TODO*///	free( tilemap->transparency_data_row );
/*TODO*///	bitmap_free( tilemap->transparency_bitmap );
/*TODO*///	bitmap_free( tilemap->pixmap );
/*TODO*///	mappings_dispose( tilemap );
/*TODO*///	free( tilemap );
    }

    /*TODO*///
/*TODO*////***********************************************************************************/
/*TODO*///
/*TODO*///void tilemap_set_enable( struct tilemap *tilemap, int enable )
/*TODO*///{
/*TODO*///	tilemap->enable = enable?1:0;
/*TODO*///}
/*TODO*///
/*TODO*///
    public static void tilemap_set_flip(struct_tilemap tilemap, int attributes) {
        // old method
        //mame037b11.mame.tilemapC.tilemap_set_flip(tilemap, attributes);
        // end old method
        
        if (tilemap == ALL_TILEMAPS) {
            tilemap = first_tilemap;
            while (tilemap != null) {
                tilemap_set_flip(tilemap, attributes);
                tilemap = tilemap.next;
            }
        } else if (tilemap.attributes != attributes) {
            
            tilemap.attributes = attributes;
            tilemap.orientation = Machine.orientation;
            if (( attributes&TILEMAP_FLIPY ) != 0)
            {
                    tilemap.orientation ^= ORIENTATION_FLIP_Y;
            }

            if (( attributes&TILEMAP_FLIPX ) != 0)
            {
                    tilemap.orientation ^= ORIENTATION_FLIP_X;
            }

            mappings_update( tilemap );
            recalculate_scroll( tilemap );
            tilemap_mark_all_tiles_dirty( tilemap );
        }
    }

    
    public static void tilemap_set_clip( struct_tilemap tilemap, rectangle pClip )
    {
            int left,top,right,bottom;

            if( pClip != null )
            {
                    tilemap.logical_clip = pClip;
                    left	= pClip.min_x;
                    top		= pClip.min_y;
                    right	= pClip.max_x+1;
                    bottom	= pClip.max_y+1;

                    if (( tilemap.orientation & ORIENTATION_SWAP_XY ) != 0)
                    {
                            SWAP(left,top);
                            SWAP(right,bottom);
                    }

                    if (( tilemap.orientation & ORIENTATION_FLIP_X ) != 0)
                    {
                            SWAP(left,right);
                            left	= screen_width-left;
                            right	= screen_width-right;
                    }

                    if (( tilemap.orientation & ORIENTATION_FLIP_Y ) != 0)
                    {
                            SWAP(top,bottom);
                            top		= screen_height-top;
                            bottom	= screen_height-bottom;
                    }
            }
            else
            {
                    /* does anyone rely on this behavior? */
                    tilemap.logical_clip = Machine.visible_area;
                    left	= 0;
                    top		= 0;
                    right	= tilemap.cached_width;
                    bottom	= tilemap.cached_height;
            }

            tilemap.clip_left		= left;
            tilemap.clip_right		= right;
            tilemap.clip_top		= top;
            tilemap.clip_bottom	= bottom;
    }

/*TODO*////***********************************************************************************/
/*TODO*///
/*TODO*///void tilemap_set_scroll_cols( struct tilemap *tilemap, int n )
/*TODO*///{
/*TODO*///	tilemap->logical_scroll_cols = n;
/*TODO*///	if( tilemap->orientation & ORIENTATION_SWAP_XY )
/*TODO*///	{
/*TODO*///		tilemap->cached_scroll_rows = n;
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		tilemap->cached_scroll_cols = n;
/*TODO*///	}
/*TODO*///}
/*TODO*///
    public static void tilemap_set_scroll_rows(struct_tilemap tilemap, int n) {
        tilemap.logical_scroll_rows = n;
        if ((tilemap.orientation & ORIENTATION_SWAP_XY) != 0) {
            tilemap.cached_scroll_cols = n;
        } else {
            tilemap.cached_scroll_rows = n;
        }
    }

    
    /***********************************************************************************/

    public static void tilemap_mark_tile_dirty(struct_tilemap tilemap, int memory_offset) {
        if( memory_offset<tilemap.max_memory_offset )
	{
		int cached_indx = tilemap.memory_offset_to_cached_indx[memory_offset];
		if( cached_indx>=0 )
		{
			tilemap.transparency_data.write(cached_indx, TILE_FLAG_DIRTY);
		}
	}
    }

    
    public static void tilemap_mark_all_tiles_dirty( struct_tilemap tilemap )
    {
            if( tilemap==ALL_TILEMAPS )
            {
                    tilemap = first_tilemap;
                    while( tilemap != null )
                    {
                            tilemap_mark_all_tiles_dirty( tilemap );
                            tilemap = tilemap.next;
                    }
            }
            else
            {
                    memset( tilemap.transparency_data, TILE_FLAG_DIRTY, tilemap.num_tiles );
            }
    }

    /***********************************************************************************/

    static void update_tile_info( struct_tilemap tilemap, int cached_indx, int col, int row )
    {
        System.out.println("update_tile_info");
            int x0;
            int y0;
            int memory_offset;
            int flags;

/*TODO*///profiler_mark(PROFILER_TILEMAP_UPDATE);

            memory_offset = tilemap.cached_indx_to_memory_offset[cached_indx];
            tilemap.tile_get_info.handler(memory_offset );
            flags = tile_info.flags;
            flags = (flags&0xfc)|tilemap.logical_flip_to_cached_flip[flags&0x3];
            x0 = tilemap.cached_tile_width*col;
            y0 = tilemap.cached_tile_height*row;
            
            //System.out.println(tilemap);
            //System.out.println(tilemap.transparency_data);
            //System.out.println(tilemap.draw_tile);
            
            /*TODO*///tilemap.transparency_data.write(cached_indx, tilemap.draw_tile.handler(tilemap,x0,y0,flags ));
            //System.out.println("draw_tile.handler");

/*TODO*///profiler_mark(PROFILER_END);
    }

/*TODO*///struct mame_bitmap *tilemap_get_pixmap( struct tilemap * tilemap )
/*TODO*///{
/*TODO*///	UINT32 cached_indx = 0;
/*TODO*///	UINT32 row,col;
/*TODO*///
/*TODO*///profiler_mark(PROFILER_TILEMAP_DRAW);
/*TODO*///	memset( &tile_info, 0x00, sizeof(tile_info) ); /* initialize defaults */
/*TODO*///
/*TODO*///	/* walk over cached rows/cols (better to walk screen coords) */
/*TODO*///	for( row=0; row<tilemap->num_cached_rows; row++ )
/*TODO*///	{
/*TODO*///		for( col=0; col<tilemap->num_cached_cols; col++ )
/*TODO*///		{
/*TODO*///			if( tilemap->transparency_data[cached_indx] == TILE_FLAG_DIRTY )
/*TODO*///			{
/*TODO*///				update_tile_info( tilemap, cached_indx, col, row );
/*TODO*///			}
/*TODO*///			cached_indx++;
/*TODO*///		} /* next col */
/*TODO*///	} /* next row */
/*TODO*///
/*TODO*///profiler_mark(PROFILER_END);
/*TODO*///	return tilemap->pixmap;
/*TODO*///}
/*TODO*///
/*TODO*///struct mame_bitmap *tilemap_get_transparency_bitmap( struct tilemap * tilemap )
/*TODO*///{
/*TODO*///	return tilemap->transparency_bitmap;
/*TODO*///}

    /***********************************************************************************/

    static void recalculate_scroll( struct_tilemap tilemap )
	{
		int i;
	
		tilemap.scrollx_delta = (tilemap.attributes & TILEMAP_FLIPX )!=0?tilemap.dx_if_flipped:tilemap.dx;
		tilemap.scrolly_delta = (tilemap.attributes & TILEMAP_FLIPY )!=0?tilemap.dy_if_flipped:tilemap.dy;
	
		for( i=0; i<tilemap.logical_scroll_rows; i++ )
		{
			tilemap_set_scrollx( tilemap, i, tilemap.logical_rowscroll[i] );
		}
		for( i=0; i<tilemap.logical_scroll_cols; i++ )
		{
			tilemap_set_scrolly( tilemap, i, tilemap.logical_colscroll[i] );
		}
	}
	
	public static void tilemap_set_scrolldx( struct_tilemap tilemap, int dx, int dx_if_flipped )
	{
		tilemap.dx = dx;
		tilemap.dx_if_flipped = dx_if_flipped;
		recalculate_scroll( tilemap );
	}
	
	public static void tilemap_set_scrolldy( struct_tilemap tilemap, int dy, int dy_if_flipped )
	{
		tilemap.dy = dy;
		tilemap.dy_if_flipped = dy_if_flipped;
		recalculate_scroll( tilemap );
	}
	
	public static void tilemap_set_scrollx( struct_tilemap tilemap, int which, int value )
	{
		tilemap.logical_rowscroll[which] = value;
		value = tilemap.scrollx_delta-value; /* adjust */
	
		if(( tilemap.orientation & ORIENTATION_SWAP_XY ) != 0)
		{
			/* if xy are swapped, we are actually panning the screen bitmap vertically */
			if(( tilemap.orientation & ORIENTATION_FLIP_X ) != 0)
			{
				/* adjust affected col */
				which = tilemap.cached_scroll_cols-1 - which;
			}
			if(( tilemap.orientation & ORIENTATION_FLIP_Y ) != 0)
			{
				/* adjust scroll amount */
				value = screen_height-tilemap.cached_height-value;
			}
			tilemap.cached_colscroll[which] = value;
		}
		else
		{
			if(( tilemap.orientation & ORIENTATION_FLIP_Y ) != 0)
			{
				/* adjust affected row */
				which = tilemap.cached_scroll_rows-1 - which;
			}
			if(( tilemap.orientation & ORIENTATION_FLIP_X ) != 0)
			{
				/* adjust scroll amount */
				value = screen_width-tilemap.cached_width-value;
			}
			tilemap.cached_rowscroll[which] = value;
		}
	}
	
	public static void tilemap_set_scrolly( struct_tilemap tilemap, int which, int value )
	{
		tilemap.logical_colscroll[which] = value;
		value = tilemap.scrolly_delta - value; /* adjust */
	
		if(( tilemap.orientation & ORIENTATION_SWAP_XY ) != 0)
		{
			/* if xy are swapped, we are actually panning the screen bitmap horizontally */
			if(( tilemap.orientation & ORIENTATION_FLIP_Y ) != 0)
			{
				/* adjust affected row */
				which = tilemap.cached_scroll_rows-1 - which;
			}
			if(( tilemap.orientation & ORIENTATION_FLIP_X ) != 0)
			{
				/* adjust scroll amount */
				value = screen_width-tilemap.cached_width-value;
			}
			tilemap.cached_rowscroll[which] = value;
		}
		else
		{
			if(( tilemap.orientation & ORIENTATION_FLIP_X ) != 0)
			{
				/* adjust affected col */
				which = tilemap.cached_scroll_cols-1 - which;
			}
			if(( tilemap.orientation & ORIENTATION_FLIP_Y ) != 0)
			{
				/* adjust scroll amount */
				value = screen_height-tilemap.cached_height-value;
			}
			tilemap.cached_colscroll[which] = value;
		}
	}

    
    /***********************************************************************************/

    public static void tilemap_draw(mame_bitmap dest, struct_tilemap tilemap, int/*UINT32*/ flags, int/*UINT32*/ priority) {
        
        int xpos,ypos,mask,value;
        int rows, cols;
        int[] rowscroll, colscroll;
        int left, right, top, bottom;
        
/*TODO*///profiler_mark(PROFILER_TILEMAP_DRAW);
	if( tilemap.enable != 0 )
	{
		/* scroll registers */
		rows		= tilemap.cached_scroll_rows;
		cols		= tilemap.cached_scroll_cols;
		rowscroll	= tilemap.cached_rowscroll;
		colscroll	= tilemap.cached_colscroll;

		/* clipping */
		left		= tilemap.clip_left;
		right		= tilemap.clip_right;
		top		= tilemap.clip_top;
		bottom		= tilemap.clip_bottom;

		/* tile priority */
		mask		= TILE_FLAG_TILE_PRIORITY;
		value		= TILE_FLAG_TILE_PRIORITY&flags;

		/* initialize defaults */
		/*TODO*///memset( tile_info, 0x00, sizeof(tile_info) );

		/* priority_bitmap_pitch_row is tilemap-specific */
		priority_bitmap_pitch_row = priority_bitmap_pitch_line*tilemap.cached_tile_height;

		blit.screen_bitmap = dest;
		if( dest == null )
		{
                    System.out.println("dest=null");
			/*TODO*///blit.draw_masked = (blitmask_t)pit;
			/*TODO*///blit.draw_opaque = (blitopaque_t)pio;
		}
		else
		{
			blit.screen_bitmap_pitch_line = (new UBytePtr(dest.line[1]).read()) - (new UBytePtr(dest.line[0]).read());
                        
                        switch( dest.depth )
			{
/*TODO*///			case 32:
/*TODO*///				if( flags&TILEMAP_ALPHA )
/*TODO*///				{
/*TODO*///					blit.draw_masked = (blitmask_t)pbt32;
/*TODO*///					blit.draw_opaque = (blitopaque_t)pbo32;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					blit.draw_masked = (blitmask_t)pdt32;
/*TODO*///					blit.draw_opaque = (blitopaque_t)pdo32;
/*TODO*///				}
/*TODO*///				blit.screen_bitmap_pitch_line /= 4;
/*TODO*///				break;
/*TODO*///
/*TODO*///			case 15:
/*TODO*///				if( flags&TILEMAP_ALPHA )
/*TODO*///				{
/*TODO*///					blit.draw_masked = (blitmask_t)pbt15;
/*TODO*///					blit.draw_opaque = (blitopaque_t)pbo15;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					blit.draw_masked = (blitmask_t)pdt15;
/*TODO*///					blit.draw_opaque = (blitopaque_t)pdo15;
/*TODO*///				}
/*TODO*///				blit.screen_bitmap_pitch_line /= 2;
/*TODO*///				break;
/*TODO*///
                            case 16:
                                    blit.draw_masked = (blitmask_t)pdt16;
                                    blit.draw_opaque = (blitopaque_t)pdo16;
                                    blit.screen_bitmap_pitch_line /= 2;
                                    break;

                            default:
                                    System.out.println("Exitting tilemap_draw...");
                                    System.exit(1);
                                    break;
			}
			blit.screen_bitmap_pitch_row = blit.screen_bitmap_pitch_line*tilemap.cached_tile_height;
		} /* dest == bitmap */

		if( !(tilemap.type==TILEMAP_OPAQUE || (flags&TILEMAP_IGNORE_TRANSPARENCY)!=0) )
		{
			if(( flags&TILEMAP_BACK ) != 0)
			{
				mask	|= TILE_FLAG_BG_OPAQUE;
				value	|= TILE_FLAG_BG_OPAQUE;
			}
			else
			{
				mask	|= TILE_FLAG_FG_OPAQUE;
				value	|= TILE_FLAG_FG_OPAQUE;
			}
		}

		blit.tilemap_priority_code = priority;

		if( rows == 1 && cols == 1 )
		{ /* XY scrolling playfield */
                    //System.out.println("A");
			int scrollx = rowscroll[0];
			int scrolly = colscroll[0];

			if( scrollx < 0 )
			{
				scrollx = tilemap.cached_width - (-scrollx) % tilemap.cached_width;
			}
			else
			{
				scrollx = scrollx % tilemap.cached_width;
			}

			if( scrolly < 0 )
			{
				scrolly = tilemap.cached_height - (-scrolly) % tilemap.cached_height;
			}
			else
			{
				scrolly = scrolly % tilemap.cached_height;
			}

	 		blit.clip_left		= left;
	 		blit.clip_top		= top;
	 		blit.clip_right		= right;
	 		blit.clip_bottom	= bottom;
                        
                        //System.out.println("blit.clip_left="+blit.clip_left);
                        //System.out.println("blit.clip_top="+blit.clip_top);
                        //System.out.println("blit.clip_right="+blit.clip_right);
                        //System.out.println("blit.clip_bottom="+blit.clip_bottom);

			for(
				ypos = scrolly - tilemap.cached_height;
				ypos < blit.clip_bottom;
				ypos += tilemap.cached_height )
			{
				for(
					xpos = scrollx - tilemap.cached_width;
					xpos < blit.clip_right;
					xpos += tilemap.cached_width )
				{
					tilemap.draw.handler(tilemap, xpos, ypos, mask, value );
				}
			}
                        
		}
		else if( rows == 1 )
		{ /* scrolling columns + horizontal scroll */
                    System.out.println("B");
			int col = 0;
			int colwidth = tilemap.cached_width / cols;
			int scrollx = rowscroll[0];

			if( scrollx < 0 )
			{
				scrollx = tilemap.cached_width - (-scrollx) % tilemap.cached_width;
			}
			else
			{
				scrollx = scrollx % tilemap.cached_width;
			}

			blit.clip_top		= top;
			blit.clip_bottom	= bottom;

			while( col < cols )
			{
				int cons	= 1;
				int scrolly	= colscroll[col];

	 			/* count consecutive columns scrolled by the same amount */
				if( scrolly != TILE_LINE_DISABLED )
				{
					while( col + cons < cols &&	colscroll[col + cons] == scrolly ) cons++;

					if( scrolly < 0 )
					{
						scrolly = tilemap.cached_height - (-scrolly) % tilemap.cached_height;
					}
					else
					{
						scrolly %= tilemap.cached_height;
					}

					blit.clip_left = col * colwidth + scrollx;
					if (blit.clip_left < left) blit.clip_left = left;
					blit.clip_right = (col + cons) * colwidth + scrollx;
					if (blit.clip_right > right) blit.clip_right = right;

					for(
						ypos = scrolly - tilemap.cached_height;
						ypos < blit.clip_bottom;
						ypos += tilemap.cached_height )
					{
						tilemap.draw.handler(tilemap, scrollx, ypos, mask, value );
					}

					blit.clip_left = col * colwidth + scrollx - tilemap.cached_width;
					if (blit.clip_left < left) blit.clip_left = left;
					blit.clip_right = (col + cons) * colwidth + scrollx - tilemap.cached_width;
					if (blit.clip_right > right) blit.clip_right = right;

					for(
						ypos = scrolly - tilemap.cached_height;
						ypos < blit.clip_bottom;
						ypos += tilemap.cached_height )
					{
						tilemap.draw.handler(tilemap, scrollx - tilemap.cached_width, ypos, mask, value );
					}
				}
				col += cons;
			}
		}
		else if( cols == 1 )
		{ /* scrolling rows + vertical scroll */
                    //System.out.println("C");
			int row = 0;
			int rowheight = tilemap.cached_height / rows;
			int scrolly = colscroll[0];
			if( scrolly < 0 )
			{
				scrolly = tilemap.cached_height - (-scrolly) % tilemap.cached_height;
			}
			else
			{
				scrolly = scrolly % tilemap.cached_height;
			}
			blit.clip_left = left;
			blit.clip_right = right;
			while( row < rows )
			{
				int cons = 1;
				int scrollx = rowscroll[row];
				/* count consecutive rows scrolled by the same amount */
				if( scrollx != TILE_LINE_DISABLED )
				{
					while( row + cons < rows &&	rowscroll[row + cons] == scrollx ) cons++;
					if( scrollx < 0)
					{
						scrollx = tilemap.cached_width - (-scrollx) % tilemap.cached_width;
					}
					else
					{
						scrollx %= tilemap.cached_width;
					}
					blit.clip_top = row * rowheight + scrolly;
					if (blit.clip_top < top) blit.clip_top = top;
					blit.clip_bottom = (row + cons) * rowheight + scrolly;
					if (blit.clip_bottom > bottom) blit.clip_bottom = bottom;
					for(
						xpos = scrollx - tilemap.cached_width;
						xpos < blit.clip_right;
						xpos += tilemap.cached_width )
					{
						tilemap.draw.handler(tilemap, xpos, scrolly, mask, value );
					}
					blit.clip_top = row * rowheight + scrolly - tilemap.cached_height;
					if (blit.clip_top < top) blit.clip_top = top;
					blit.clip_bottom = (row + cons) * rowheight + scrolly - tilemap.cached_height;
					if (blit.clip_bottom > bottom) blit.clip_bottom = bottom;
					for(
						xpos = scrollx - tilemap.cached_width;
						xpos < blit.clip_right;
						xpos += tilemap.cached_width )
					{
						tilemap.draw.handler(tilemap, xpos, scrolly - tilemap.cached_height, mask, value );
					}
				}
				row += cons;
			}
		}
	}
/*TODO*///profiler_mark(PROFILER_END);
    }
    /*TODO*///
/*TODO*////* notes:
/*TODO*///   - startx and starty MUST be UINT32 for calculations to work correctly
/*TODO*///   - srcbitmap->width and height are assumed to be a power of 2 to speed up wraparound
/*TODO*///   */
/*TODO*///void tilemap_draw_roz(struct mame_bitmap *dest,struct tilemap *tilemap,
/*TODO*///		UINT32 startx,UINT32 starty,int incxx,int incxy,int incyx,int incyy,
/*TODO*///		int wraparound,
/*TODO*///		UINT32 flags, UINT32 priority )
/*TODO*///{
/*TODO*///	int mask,value;
/*TODO*///
/*TODO*///profiler_mark(PROFILER_TILEMAP_DRAW_ROZ);
/*TODO*///	if( tilemap->enable )
/*TODO*///	{
/*TODO*///		/* tile priority */
/*TODO*///		mask		= TILE_FLAG_TILE_PRIORITY;
/*TODO*///		value		= TILE_FLAG_TILE_PRIORITY&flags;
/*TODO*///
/*TODO*///		tilemap_get_pixmap( tilemap ); /* force update */
/*TODO*///
/*TODO*///		if( !(tilemap->type==TILEMAP_OPAQUE || (flags&TILEMAP_IGNORE_TRANSPARENCY)) )
/*TODO*///		{
/*TODO*///			if( flags&TILEMAP_BACK )
/*TODO*///			{
/*TODO*///				mask	|= TILE_FLAG_BG_OPAQUE;
/*TODO*///				value	|= TILE_FLAG_BG_OPAQUE;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				mask	|= TILE_FLAG_FG_OPAQUE;
/*TODO*///				value	|= TILE_FLAG_FG_OPAQUE;
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		switch( dest->depth )
/*TODO*///		{
/*TODO*///
/*TODO*///		case 32:
/*TODO*///			copyrozbitmap_core32BPP(dest,tilemap,startx,starty,incxx,incxy,incyx,incyy,
/*TODO*///				wraparound,&tilemap->logical_clip,mask,value,priority);
/*TODO*///			break;
/*TODO*///
/*TODO*///		case 15:
/*TODO*///		case 16:
/*TODO*///			copyrozbitmap_core16BPP(dest,tilemap,startx,starty,incxx,incxy,incyx,incyy,
/*TODO*///				wraparound,&tilemap->logical_clip,mask,value,priority);
/*TODO*///			break;
/*TODO*///
/*TODO*///		default:
/*TODO*///			exit(1);
/*TODO*///		}
/*TODO*///	} /* tilemap->enable */
/*TODO*///profiler_mark(PROFILER_END);
/*TODO*///}
/*TODO*///
/*TODO*////***********************************************************************************/
/*TODO*///
/*TODO*///#endif // !DECLARE && !TRANSP
/*TODO*///
/*TODO*///#ifdef DECLARE
/*TODO*///
/*TODO*///DECLARE(copyrozbitmap_core,(struct mame_bitmap *bitmap,struct tilemap *tilemap,
/*TODO*///		UINT32 startx,UINT32 starty,int incxx,int incxy,int incyx,int incyy,int wraparound,
/*TODO*///		const struct rectangle *clip,
/*TODO*///		int mask,int value,
/*TODO*///		UINT32 priority),
/*TODO*///{
/*TODO*///	UINT32 cx;
/*TODO*///	UINT32 cy;
/*TODO*///	int x;
/*TODO*///	int sx;
/*TODO*///	int sy;
/*TODO*///	int ex;
/*TODO*///	int ey;
/*TODO*///	struct mame_bitmap *srcbitmap = tilemap->pixmap;
/*TODO*///	struct mame_bitmap *transparency_bitmap = tilemap->transparency_bitmap;
/*TODO*///	const int xmask = srcbitmap->width-1;
/*TODO*///	const int ymask = srcbitmap->height-1;
/*TODO*///	const int widthshifted = srcbitmap->width << 16;
/*TODO*///	const int heightshifted = srcbitmap->height << 16;
/*TODO*///	DATA_TYPE *dest;
/*TODO*///	UINT8 *pri;
/*TODO*///	const UINT16 *src;
/*TODO*///	const UINT8 *pMask;
/*TODO*///
/*TODO*///	if (clip)
/*TODO*///	{
/*TODO*///		startx += clip->min_x * incxx + clip->min_y * incyx;
/*TODO*///		starty += clip->min_x * incxy + clip->min_y * incyy;
/*TODO*///
/*TODO*///		sx = clip->min_x;
/*TODO*///		sy = clip->min_y;
/*TODO*///		ex = clip->max_x;
/*TODO*///		ey = clip->max_y;
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		sx = 0;
/*TODO*///		sy = 0;
/*TODO*///		ex = bitmap->width-1;
/*TODO*///		ey = bitmap->height-1;
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///	if (Machine->orientation & ORIENTATION_SWAP_XY)
/*TODO*///	{
/*TODO*///		int t;
/*TODO*///
/*TODO*///		t = startx; startx = starty; starty = t;
/*TODO*///		t = sx; sx = sy; sy = t;
/*TODO*///		t = ex; ex = ey; ey = t;
/*TODO*///		t = incxx; incxx = incyy; incyy = t;
/*TODO*///		t = incxy; incxy = incyx; incyx = t;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (Machine->orientation & ORIENTATION_FLIP_X)
/*TODO*///	{
/*TODO*///		int w = ex - sx;
/*TODO*///
/*TODO*///		incxy = -incxy;
/*TODO*///		incyx = -incyx;
/*TODO*///		startx = widthshifted - startx - 1;
/*TODO*///		startx -= incxx * w;
/*TODO*///		starty -= incxy * w;
/*TODO*///
/*TODO*///		w = sx;
/*TODO*///		sx = bitmap->width-1 - ex;
/*TODO*///		ex = bitmap->width-1 - w;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (Machine->orientation & ORIENTATION_FLIP_Y)
/*TODO*///	{
/*TODO*///		int h = ey - sy;
/*TODO*///
/*TODO*///		incxy = -incxy;
/*TODO*///		incyx = -incyx;
/*TODO*///		starty = heightshifted - starty - 1;
/*TODO*///		startx -= incyx * h;
/*TODO*///		starty -= incyy * h;
/*TODO*///
/*TODO*///		h = sy;
/*TODO*///		sy = bitmap->height-1 - ey;
/*TODO*///		ey = bitmap->height-1 - h;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (incxy == 0 && incyx == 0 && !wraparound)
/*TODO*///	{
/*TODO*///		/* optimized loop for the not rotated case */
/*TODO*///
/*TODO*///		if (incxx == 0x10000)
/*TODO*///		{
/*TODO*///			/* optimized loop for the not zoomed case */
/*TODO*///
/*TODO*///			/* startx is unsigned */
/*TODO*///			startx = ((INT32)startx) >> 16;
/*TODO*///
/*TODO*///			if (startx >= srcbitmap->width)
/*TODO*///			{
/*TODO*///				sx += -startx;
/*TODO*///				startx = 0;
/*TODO*///			}
/*TODO*///
/*TODO*///			if (sx <= ex)
/*TODO*///			{
/*TODO*///				while (sy <= ey)
/*TODO*///				{
/*TODO*///					if (starty < heightshifted)
/*TODO*///					{
/*TODO*///						x = sx;
/*TODO*///						cx = startx;
/*TODO*///						cy = starty >> 16;
/*TODO*///						dest = ((DATA_TYPE *)bitmap->line[sy]) + sx;
/*TODO*///
/*TODO*///						pri = ((UINT8 *)priority_bitmap->line[sy]) + sx;
/*TODO*///						src = (UINT16 *)srcbitmap->line[cy];
/*TODO*///						pMask = (UINT8 *)transparency_bitmap->line[cy];
/*TODO*///
/*TODO*///						while (x <= ex && cx < srcbitmap->width)
/*TODO*///						{
/*TODO*///							if ( (pMask[cx]&mask) == value )
/*TODO*///							{
/*TODO*///								*dest = src[cx];
/*TODO*///								*pri |= priority;
/*TODO*///							}
/*TODO*///							cx++;
/*TODO*///							x++;
/*TODO*///							dest++;
/*TODO*///							pri++;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					starty += incyy;
/*TODO*///					sy++;
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			while (startx >= widthshifted && sx <= ex)
/*TODO*///			{
/*TODO*///				startx += incxx;
/*TODO*///				sx++;
/*TODO*///			}
/*TODO*///
/*TODO*///			if (sx <= ex)
/*TODO*///			{
/*TODO*///				while (sy <= ey)
/*TODO*///				{
/*TODO*///					if (starty < heightshifted)
/*TODO*///					{
/*TODO*///						x = sx;
/*TODO*///						cx = startx;
/*TODO*///						cy = starty >> 16;
/*TODO*///						dest = ((DATA_TYPE *)bitmap->line[sy]) + sx;
/*TODO*///
/*TODO*///						pri = ((UINT8 *)priority_bitmap->line[sy]) + sx;
/*TODO*///						src = (UINT16 *)srcbitmap->line[cy];
/*TODO*///						pMask = (UINT8 *)transparency_bitmap->line[cy];
/*TODO*///						while (x <= ex && cx < widthshifted)
/*TODO*///						{
/*TODO*///							if ( (pMask[cx>>16]&mask) == value )
/*TODO*///							{
/*TODO*///								*dest = src[cx >> 16];
/*TODO*///								*pri |= priority;
/*TODO*///							}
/*TODO*///							cx += incxx;
/*TODO*///							x++;
/*TODO*///							dest++;
/*TODO*///							pri++;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					starty += incyy;
/*TODO*///					sy++;
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		if (wraparound)
/*TODO*///		{
/*TODO*///			/* plot with wraparound */
/*TODO*///			while (sy <= ey)
/*TODO*///			{
/*TODO*///				x = sx;
/*TODO*///				cx = startx;
/*TODO*///				cy = starty;
/*TODO*///				dest = ((DATA_TYPE *)bitmap->line[sy]) + sx;
/*TODO*///				pri = ((UINT8 *)priority_bitmap->line[sy]) + sx;
/*TODO*///				while (x <= ex)
/*TODO*///				{
/*TODO*///					if( (((UINT8 *)transparency_bitmap->line[(cy>>16)&ymask])[(cx>>16)&xmask]&mask) == value )
/*TODO*///					{
/*TODO*///						*dest = ((UINT16 *)srcbitmap->line[(cy >> 16) & ymask])[(cx >> 16) & xmask];
/*TODO*///						*pri |= priority;
/*TODO*///					}
/*TODO*///					cx += incxx;
/*TODO*///					cy += incxy;
/*TODO*///					x++;
/*TODO*///					dest++;
/*TODO*///					pri++;
/*TODO*///				}
/*TODO*///				startx += incyx;
/*TODO*///				starty += incyy;
/*TODO*///				sy++;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			while (sy <= ey)
/*TODO*///			{
/*TODO*///				x = sx;
/*TODO*///				cx = startx;
/*TODO*///				cy = starty;
/*TODO*///				dest = ((DATA_TYPE *)bitmap->line[sy]) + sx;
/*TODO*///				pri = ((UINT8 *)priority_bitmap->line[sy]) + sx;
/*TODO*///				while (x <= ex)
/*TODO*///				{
/*TODO*///					if (cx < widthshifted && cy < heightshifted)
/*TODO*///					{
/*TODO*///						if( (((UINT8 *)transparency_bitmap->line[cy>>16])[cx>>16]&mask)==value )
/*TODO*///						{
/*TODO*///							*dest = ((UINT16 *)srcbitmap->line[cy >> 16])[cx >> 16];
/*TODO*///							*pri |= priority;
/*TODO*///						}
/*TODO*///					}
/*TODO*///					cx += incxx;
/*TODO*///					cy += incxy;
/*TODO*///					x++;
/*TODO*///					dest++;
/*TODO*///					pri++;
/*TODO*///				}
/*TODO*///				startx += incyx;
/*TODO*///				starty += incyy;
/*TODO*///				sy++;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///})
    static void memcpybitmask8(UBytePtr dest, UBytePtr source, UBytePtr bitmask, int count) {
        for (;;) {
            int/*UINT32*/ data = bitmask.readinc();
            if ((data & 0x80) != 0) {
                dest.write(0, source.read(0));
            }
            if ((data & 0x40) != 0) {
                dest.write(1, source.read(1));
            }
            if ((data & 0x20) != 0) {
                dest.write(2, source.read(2));
            }
            if ((data & 0x10) != 0) {
                dest.write(3, source.read(3));
            }
            if ((data & 0x08) != 0) {
                dest.write(4, source.read(4));
            }
            if ((data & 0x04) != 0) {
                dest.write(5, source.read(5));
            }
            if ((data & 0x02) != 0) {
                dest.write(6, source.read(6));
            }
            if ((data & 0x01) != 0) {
                dest.write(7, source.read(7));
            }
            if (--count == 0) {
                break;
            }
            source.offset += 8;
            dest.offset += 8;
        }
    }
    
    static void memsetbitmask8(UBytePtr dest, int value, UBytePtr bitmask, int count) {
        /* TBA: combine with memcpybitmask */
        for (;;) {
            int/*UINT32*/ data = bitmask.readinc();
            if ((data & 0x80) != 0) {
                dest.write(0, dest.read(0) | value);
            }
            if ((data & 0x40) != 0) {
                dest.write(1, dest.read(1) | value);
            }
            if ((data & 0x20) != 0) {
                dest.write(2, dest.read(2) | value);
            }
            if ((data & 0x10) != 0) {
                dest.write(3, dest.read(3) | value);
            }
            if ((data & 0x08) != 0) {
                dest.write(4, dest.read(4) | value);
            }
            if ((data & 0x04) != 0) {
                dest.write(5, dest.read(5) | value);
            }
            if ((data & 0x02) != 0) {
                dest.write(6, dest.read(6) | value);
            }
            if ((data & 0x01) != 0) {
                dest.write(7, dest.read(7) | value);
            }
            if (--count == 0) {
                break;
            }
            dest.offset += 8;
        }
    }
    
    public static DrawHandlerPtr draw8BPP = new DrawHandlerPtr() {
        public void handler(struct_tilemap tilemap, int xpos, int ypos, int mask, int value) {
            //System.out.println("draw8BPP");
            int tilemap_priority_code = blit.tilemap_priority_code;
        int x1 = xpos;
        int y1 = ypos;
        int x2 = xpos + tilemap.cached_width*2;
        int y2 = ypos + tilemap.cached_height*2;

        /* clip source coordinates */
        if (x1 < blit.clip_left) {
            x1 = blit.clip_left;
        }
        if (x2 > blit.clip_right*2) {
            x2 = blit.clip_right*2;
        }
        if (y1 < blit.clip_top) {
            y1 = blit.clip_top;
        }
        if (y2 > blit.clip_bottom*2) {
            y2 = blit.clip_bottom*2;
        }

        if (true) {
            /* do nothing if totally clipped */
            UBytePtr dest_baseaddr = new UBytePtr(blit.screen_bitmap.line[y1], xpos);
            UBytePtr dest_next;
                
            int priority_bitmap_row_offset = priority_bitmap_pitch_line * tilemap.cached_height;
            UBytePtr priority_bitmap_baseaddr = new UBytePtr(priority_bitmap.line[y1], xpos);
            UBytePtr priority_bitmap_next;

            int priority = blit.tilemap_priority_code;
            UBytePtr source_baseaddr;
            UBytePtr source_next;
            UBytePtr mask_baseaddr;
            UBytePtr mask_next;

            int c1;
            int c2;
            /* leftmost and rightmost visible columns in source tilemap */
            int y;
            /* current screen line to render */
            int y_next;

            /* convert screen coordinates to source tilemap coordinates */
            x1 -= xpos;
            y1 -= ypos;
            x2 -= xpos;
            y2 -= ypos;

            source_baseaddr = new UBytePtr(tilemap.pixmap.line[y1]);
            mask_baseaddr = new UBytePtr(tilemap.transparency_bitmap.line[y1]);

            c1 = x1 / tilemap.cached_width;
            /* round down */
            c2 = (x2 + tilemap.cached_width - 1) / tilemap.cached_width;
            /* round up */

            y = y1;
            y_next = tilemap.cached_height * (y1 / tilemap.cached_height) + tilemap.cached_height;
            if (y_next > y2) {
                y_next = y2;
            }

            {
                int dy = y_next - y;
                dest_next = new UBytePtr(dest_baseaddr, dy * blit.screen_bitmap_pitch_line);
                priority_bitmap_next = new UBytePtr(priority_bitmap_baseaddr, dy * priority_bitmap_pitch_line);
                source_next = new UBytePtr(source_baseaddr, dy * tilemap.pixmap_pitch_line);
                mask_next = new UBytePtr(mask_baseaddr, dy * tilemap.transparency_bitmap_pitch_line);
            }

            for (;;) {
                int row = y / tilemap.cached_height;
                
                UBytePtr mask_data = new UBytePtr(mask_baseaddr, row);
                UBytePtr priority_data = new UBytePtr(tilemap.transparency_data, row);

                int tile_type;
                int prev_tile_type = eWHOLLY_TRANSPARENT;

                int x_start = x1;
                int x_end;

                int column;
                for (column = c1; column <= c2; column++) {
                    if (column == c2 || priority_data.read(column) != priority) {
                        tile_type = eWHOLLY_TRANSPARENT;
                    } else {
                        tile_type = mask_data.read(column);
                        if(( (tilemap.transparency_data.read(column)&mask)!=0 ))
                                    {
                                            tile_type = eMASKED;
                                    }
                                    else
                                    {
                                            tile_type = (((tilemap.transparency_data.read())&mask) == value)?eWHOLLY_OPAQUE:eWHOLLY_TRANSPARENT;
                                    }
                    }

                    if (tile_type != prev_tile_type) {
                        x_end = column * tilemap.cached_width;
                        if (x_end < x1) {
                            x_end = x1;
                        }
                        if (x_end > x2) {
                            x_end = x2;
                        }

                        if (prev_tile_type != eWHOLLY_TRANSPARENT) {
                            if (prev_tile_type == eMASKED) {
                                int count = (x_end + 7) / 8 - x_start / 8;
                                UBytePtr mask0 = new UBytePtr(mask_baseaddr, x_start / 8);
                                UBytePtr source0 = new UBytePtr(source_baseaddr, (x_start & 0xfff8));
                                UBytePtr dest0 = new UBytePtr(dest_baseaddr, (x_start & 0xfff8));
                                UBytePtr pmap0 = new UBytePtr(priority_bitmap_baseaddr, (x_start & 0xfff8));
                                int i = y;
                                for (;;) {
                                    memcpybitmask8(new UBytePtr(dest0), new UBytePtr(source0), new UBytePtr(mask0), count);
                                    memsetbitmask8(new UBytePtr(pmap0), tilemap_priority_code, new UBytePtr(mask0), count);
                                    if (++i == y_next) {
                                        break;
                                    }

                                    dest0.offset += blit.screen_bitmap_pitch_line;
                                    source0.offset += tilemap.pixmap_pitch_line;
                                    mask0.offset += tilemap.transparency_bitmap_pitch_line;
                                    pmap0.offset += priority_bitmap_pitch_line;
                                }
                            } else {
                                /* TILE_OPAQUE */
                                int num_pixels = x_end - x_start;
                                UBytePtr dest0 = new UBytePtr(dest_baseaddr, x_start);
                                UBytePtr source0 = new UBytePtr(source_baseaddr, x_start);
                                UBytePtr pmap0 = new UBytePtr(priority_bitmap_baseaddr, x_start);
                                int i = y;
                                for (;;) {
                                    memcpy(dest0, source0, num_pixels);
                                    memset(pmap0, tilemap_priority_code, num_pixels);
                                    if (++i == y_next) {
                                        break;
                                    }

                                    dest0.offset += blit.screen_bitmap_pitch_line;
                                    source0.offset += tilemap.pixmap_pitch_line;
                                    pmap0.offset += priority_bitmap_pitch_line;
                                }
                            }
                        }
                        x_start = x_end;
                    }

                    prev_tile_type = tile_type;
                }

                if (y_next == y2) {
                    break;
                    /* we are done! */
                }

                priority_bitmap_baseaddr = new UBytePtr(priority_bitmap_next);
                dest_baseaddr = new UBytePtr(dest_next);
                source_baseaddr = new UBytePtr(source_next);
                mask_baseaddr = new UBytePtr(mask_next);

                y = y_next;
                y_next += tilemap.cached_height;

                if (y_next >= y2) {
                    y_next = y2;
                } else {
                    dest_next.offset += blit.screen_bitmap_pitch_row;
                    priority_bitmap_next.offset += priority_bitmap_row_offset;
                    source_next.offset += tilemap.pixmap_pitch_row;
                    mask_next.offset += tilemap.transparency_bitmap_pitch_row;
                }
            }
            /* process next row */
        }
        /* not totally clipped */
        }
    };
    
    public static DrawHandlerPtr draw16BPP = new DrawHandlerPtr() {
        public void handler(struct_tilemap tilemap, int xpos, int ypos, int mask, int value) {
            //System.out.println("draw16BPP");
            int transPrev = eWHOLLY_OPAQUE;
            int transCur = eWHOLLY_OPAQUE;
            UShortPtr pTrans = new UShortPtr();
            int cached_indx;
            //mame_bitmap screen = blit.screen_bitmap;
            int tilemap_priority_code = blit.tilemap_priority_code;
            int x1 = xpos;
            int y1 = ypos;
            int x2 = xpos+tilemap.cached_width;
            //System.out.println("cached_width="+tilemap.cached_width);
            //System.out.println("xpos="+xpos);
            //System.out.println("XX2="+xx2);
            int y2 = ypos+tilemap.cached_height;
            //System.out.println("cached_height="+tilemap.cached_height);
            UShortPtr dest_baseaddr = null;
            UShortPtr dest_next = new UShortPtr();
            int dy;
            int count;
            UShortPtr source0=new UShortPtr();
            UShortPtr dest0 = new UShortPtr();
            UShortPtr pmap0 = new UShortPtr();
            int i;
            int row;
            int x_start;
            int x_end;
            int column;
            int c1; /* leftmost visible column in source tilemap */
            int c2; /* rightmost visible column in source tilemap */
            int y; /* current screen line to render */
            int y_next;
            UShortPtr priority_bitmap_baseaddr = new UShortPtr();
            UShortPtr priority_bitmap_next = new UShortPtr();
            UShortPtr source_baseaddr = new UShortPtr();
            UShortPtr source_next = new UShortPtr();
            UShortPtr mask0 = new UShortPtr();
            UShortPtr mask_baseaddr = new UShortPtr();
            UShortPtr mask_next = new UShortPtr();

            /* clip source coordinates */
            if (x1 < blit.clip_left) {
                x1 = blit.clip_left;
            }
            if (x2 > blit.clip_right) {
                x2 = blit.clip_right;
            }
            if (y1 < blit.clip_top) {
                y1 = blit.clip_top;
            }
            if (y2 > blit.clip_bottom) {
                y2 = blit.clip_bottom;
            }
            
            //System.out.println("x1="+x1);
            //System.out.println("y1="+y1);
            //System.out.println("x2="+xx2);
            //System.out.println("y2="+y2);

            if( x1<x2 && y1<y2 ) /* do nothing if totally clipped */
            {
                //System.out.println("IN");
                    priority_bitmap_baseaddr = new UShortPtr(priority_bitmap.line[y1], xpos );
                    if( blit.screen_bitmap != null )
                    {
                            dest_baseaddr = new UShortPtr(blit.screen_bitmap.line[y1], xpos);
                    }

                    /* convert screen coordinates to source tilemap coordinates */
                    x1 -= xpos;
                    y1 -= ypos;
                    x2 -= xpos;
                    y2 -= ypos;

                    source_baseaddr = new UShortPtr(tilemap.pixmap.line[y1]);
                    mask_baseaddr = new UShortPtr(tilemap.transparency_bitmap.line[y1]);

                    c1 = x1/tilemap.cached_tile_width; /* round down */
                    c2 = (x2+tilemap.cached_tile_width-1)/tilemap.cached_tile_width; /* round up */

                    y = y1;
                    y_next = tilemap.cached_tile_height*(y1/tilemap.cached_tile_height) + tilemap.cached_tile_height;
                    if( y_next>y2 ) y_next = y2;

                    dy = y_next-y;
                    dest_next = new UShortPtr(dest_baseaddr, dy*blit.screen_bitmap_pitch_line);
                    priority_bitmap_next = new UShortPtr(priority_bitmap_baseaddr, dy*priority_bitmap_pitch_line);
                    source_next = new UShortPtr(source_baseaddr, dy*tilemap.pixmap_pitch_line);
                    mask_next = new UShortPtr(mask_baseaddr, dy*tilemap.transparency_bitmap_pitch_line);
                    for(;;)
                    {
                            row = y/tilemap.cached_tile_height;
                            x_start = x1;

                            transPrev = eWHOLLY_TRANSPARENT;
                            pTrans = new UShortPtr(mask_baseaddr, x_start);

                            cached_indx = row*tilemap.num_cached_cols + c1;
                            boolean L_Skip = false;
                            for( column=c1; column<=c2; column++ )
                            {
                                    if( column == c2 )
                                    {
                                            transCur = eWHOLLY_TRANSPARENT;
                                            L_Skip = true;
                                    }

                                    if(( tilemap.transparency_data.read(cached_indx)==TILE_FLAG_DIRTY ) && !L_Skip)
                                    {
                                            update_tile_info( tilemap, cached_indx, column, row );
                                    }

                                    if(( (tilemap.transparency_data.read(cached_indx)&mask)!=0 ) && !L_Skip)
                                    {
                                            transCur = eMASKED;
                                    }
                                    else if (!L_Skip)
                                    {
                                            transCur = (((pTrans.read(pTrans.offset))&mask) == value)?eWHOLLY_OPAQUE:eWHOLLY_TRANSPARENT;
                                    }
                                    if (!L_Skip)
                                        pTrans.inc( tilemap.cached_tile_width );

                            //L_Skip:
                                    if( transCur!=transPrev )
                                    {
                                            x_end = column*tilemap.cached_tile_width;
                                            if( x_end<x1 ) x_end = x1;
                                            if( x_end>x2 ) x_end = x2;

                                            if( transPrev != eWHOLLY_TRANSPARENT )
                                            {
                                                    count = x_end - x_start;
                                                    source0 = new UShortPtr(source_baseaddr, x_start);
                                                    dest0 = new UShortPtr(dest_baseaddr, x_start);
                                                    pmap0 = new UShortPtr(priority_bitmap_baseaddr, x_start);
                                                    mask0 = new UShortPtr(mask_baseaddr, x_start / 8);

                                                    if( transPrev == eWHOLLY_OPAQUE )
                                                    {
                                                            i = y;
                                                            for(;;)
                                                            {
                                                                    blit.draw_opaque.handler(dest0, source0, count, pmap0, tilemap_priority_code );
                                                                    //memcpybitmask8(new UBytePtr(dest0), new UBytePtr(source0), new UBytePtr(mask0), count);
                                                                    //memsetbitmask8(new UBytePtr(pmap0), tilemap_priority_code, new UBytePtr(mask0), count);
                                                                    
                                                                    //System.out.println("blit.draw_opaque");
                                                                    if( ++i == y_next ) break;

                                                                    dest0.inc( blit.screen_bitmap_pitch_line );
                                                                    source0.inc( tilemap.pixmap_pitch_line );
                                                                    pmap0.inc( priority_bitmap_pitch_line );
                                                            }
                                                            //System.out.println("tilemap.enable="+tilemap.enable);
                                                            
                                                    } /* transPrev == eWHOLLY_OPAQUE */
                                                    else /* transPrev == eMASKED */
                                                    {
                                                            mask0 = new UShortPtr(mask_baseaddr, x_start);
                                                            i = y;
                                                            for(;;)
                                                            {
                                                                    blit.draw_masked.handler(dest0, source0, mask0, mask, value, count, pmap0, tilemap_priority_code );
                                                                    //System.out.println("blit.draw_masked");
                                                                    if( ++i == y_next ) break;

                                                                    dest0.inc( blit.screen_bitmap_pitch_line );
                                                                    source0.inc( tilemap.pixmap_pitch_line );
                                                                    mask0.inc( tilemap.transparency_bitmap_pitch_line );
                                                                    pmap0.inc( priority_bitmap_pitch_line );
                                                            }
                                                    } /* transPrev == eMASKED */
                                            } /* transPrev != eWHOLLY_TRANSPARENT */
                                            x_start = x_end;
                                            transPrev = transCur;
                                    }
                                    cached_indx++;
                            }
                            if( y_next==y2 ) break; /* we are done! */

                            priority_bitmap_baseaddr = priority_bitmap_next;
                            dest_baseaddr = dest_next;
                            source_baseaddr = source_next;
                            mask_baseaddr = mask_next;
                            y = y_next;
                            y_next += tilemap.cached_tile_height;

                            if( y_next>=y2 )
                            {
                                    y_next = y2;
                            }
                            else
                            {
                                    dest_next.inc( blit.screen_bitmap_pitch_row );
                                    priority_bitmap_next.inc( priority_bitmap_pitch_row );
                                    source_next.inc( tilemap.pixmap_pitch_row );
                                    mask_next.inc( tilemap.transparency_bitmap_pitch_row );
                            }
                    } /* process next row */
            } /* not totally clipped */
        }
    };
        
/*TODO*///DECLARE( draw, (struct tilemap *tilemap, int xpos, int ypos, int mask, int value ),
/*TODO*///{
/*TODO*///	trans_t transPrev;
/*TODO*///	trans_t transCur;
/*TODO*///	const UINT8 *pTrans;
/*TODO*///	UINT32 cached_indx;
/*TODO*///	struct mame_bitmap *screen = blit.screen_bitmap;
/*TODO*///	int tilemap_priority_code = blit.tilemap_priority_code;
/*TODO*///	int x1 = xpos;
/*TODO*///	int y1 = ypos;
/*TODO*///	int x2 = xpos+tilemap->cached_width;
/*TODO*///	int y2 = ypos+tilemap->cached_height;
/*TODO*///	DATA_TYPE *dest_baseaddr = NULL;
/*TODO*///	DATA_TYPE *dest_next;
/*TODO*///	int dy;
/*TODO*///	int count;
/*TODO*///	const UINT16 *source0;
/*TODO*///	DATA_TYPE *dest0;
/*TODO*///	UINT8 *pmap0;
/*TODO*///	int i;
/*TODO*///	int row;
/*TODO*///	int x_start;
/*TODO*///	int x_end;
/*TODO*///	int column;
/*TODO*///	int c1; /* leftmost visible column in source tilemap */
/*TODO*///	int c2; /* rightmost visible column in source tilemap */
/*TODO*///	int y; /* current screen line to render */
/*TODO*///	int y_next;
/*TODO*///	UINT8 *priority_bitmap_baseaddr;
/*TODO*///	UINT8 *priority_bitmap_next;
/*TODO*///	const UINT16 *source_baseaddr;
/*TODO*///	const UINT16 *source_next;
/*TODO*///	const UINT8 *mask0;
/*TODO*///	const UINT8 *mask_baseaddr;
/*TODO*///	const UINT8 *mask_next;
/*TODO*///
/*TODO*///	/* clip source coordinates */
/*TODO*///	if( x1<blit.clip_left ) x1 = blit.clip_left;
/*TODO*///	if( x2>blit.clip_right ) x2 = blit.clip_right;
/*TODO*///	if( y1<blit.clip_top ) y1 = blit.clip_top;
/*TODO*///	if( y2>blit.clip_bottom ) y2 = blit.clip_bottom;
/*TODO*///
/*TODO*///	if( x1<x2 && y1<y2 ) /* do nothing if totally clipped */
/*TODO*///	{
/*TODO*///		priority_bitmap_baseaddr = xpos + (UINT8 *)priority_bitmap->line[y1];
/*TODO*///		if( screen )
/*TODO*///		{
/*TODO*///			dest_baseaddr = xpos + (DATA_TYPE *)screen->line[y1];
/*TODO*///		}
/*TODO*///
/*TODO*///		/* convert screen coordinates to source tilemap coordinates */
/*TODO*///		x1 -= xpos;
/*TODO*///		y1 -= ypos;
/*TODO*///		x2 -= xpos;
/*TODO*///		y2 -= ypos;
/*TODO*///
/*TODO*///		source_baseaddr = (UINT16 *)tilemap->pixmap->line[y1];
/*TODO*///		mask_baseaddr = tilemap->transparency_bitmap->line[y1];
/*TODO*///
/*TODO*///		c1 = x1/tilemap->cached_tile_width; /* round down */
/*TODO*///		c2 = (x2+tilemap->cached_tile_width-1)/tilemap->cached_tile_width; /* round up */
/*TODO*///
/*TODO*///		y = y1;
/*TODO*///		y_next = tilemap->cached_tile_height*(y1/tilemap->cached_tile_height) + tilemap->cached_tile_height;
/*TODO*///		if( y_next>y2 ) y_next = y2;
/*TODO*///
/*TODO*///		dy = y_next-y;
/*TODO*///		dest_next = dest_baseaddr + dy*blit.screen_bitmap_pitch_line;
/*TODO*///		priority_bitmap_next = priority_bitmap_baseaddr + dy*priority_bitmap_pitch_line;
/*TODO*///		source_next = source_baseaddr + dy*tilemap->pixmap_pitch_line;
/*TODO*///		mask_next = mask_baseaddr + dy*tilemap->transparency_bitmap_pitch_line;
/*TODO*///		for(;;)
/*TODO*///		{
/*TODO*///			row = y/tilemap->cached_tile_height;
/*TODO*///			x_start = x1;
/*TODO*///
/*TODO*///			transPrev = eWHOLLY_TRANSPARENT;
/*TODO*///			pTrans = mask_baseaddr + x_start;
/*TODO*///
/*TODO*///			cached_indx = row*tilemap->num_cached_cols + c1;
/*TODO*///			for( column=c1; column<=c2; column++ )
/*TODO*///			{
/*TODO*///				if( column == c2 )
/*TODO*///				{
/*TODO*///					transCur = eWHOLLY_TRANSPARENT;
/*TODO*///					goto L_Skip;
/*TODO*///				}
/*TODO*///
/*TODO*///				if( tilemap->transparency_data[cached_indx]==TILE_FLAG_DIRTY )
/*TODO*///				{
/*TODO*///					update_tile_info( tilemap, cached_indx, column, row );
/*TODO*///				}
/*TODO*///
/*TODO*///				if( (tilemap->transparency_data[cached_indx]&mask)!=0 )
/*TODO*///				{
/*TODO*///					transCur = eMASKED;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					transCur = (((*pTrans)&mask) == value)?eWHOLLY_OPAQUE:eWHOLLY_TRANSPARENT;
/*TODO*///				}
/*TODO*///				pTrans += tilemap->cached_tile_width;
/*TODO*///
/*TODO*///			L_Skip:
/*TODO*///				if( transCur!=transPrev )
/*TODO*///				{
/*TODO*///					x_end = column*tilemap->cached_tile_width;
/*TODO*///					if( x_end<x1 ) x_end = x1;
/*TODO*///					if( x_end>x2 ) x_end = x2;
/*TODO*///
/*TODO*///					if( transPrev != eWHOLLY_TRANSPARENT )
/*TODO*///					{
/*TODO*///						count = x_end - x_start;
/*TODO*///						source0 = source_baseaddr + x_start;
/*TODO*///						dest0 = dest_baseaddr + x_start;
/*TODO*///						pmap0 = priority_bitmap_baseaddr + x_start;
/*TODO*///
/*TODO*///						if( transPrev == eWHOLLY_OPAQUE )
/*TODO*///						{
/*TODO*///							i = y;
/*TODO*///							for(;;)
/*TODO*///							{
/*TODO*///								blit.draw_opaque( dest0, source0, count, pmap0, tilemap_priority_code );
/*TODO*///								if( ++i == y_next ) break;
/*TODO*///
/*TODO*///								dest0 += blit.screen_bitmap_pitch_line;
/*TODO*///								source0 += tilemap->pixmap_pitch_line;
/*TODO*///								pmap0 += priority_bitmap_pitch_line;
/*TODO*///							}
/*TODO*///						} /* transPrev == eWHOLLY_OPAQUE */
/*TODO*///						else /* transPrev == eMASKED */
/*TODO*///						{
/*TODO*///							mask0 = mask_baseaddr + x_start;
/*TODO*///							i = y;
/*TODO*///							for(;;)
/*TODO*///							{
/*TODO*///								blit.draw_masked( dest0, source0, mask0, mask, value, count, pmap0, tilemap_priority_code );
/*TODO*///								if( ++i == y_next ) break;
/*TODO*///
/*TODO*///								dest0 += blit.screen_bitmap_pitch_line;
/*TODO*///								source0 += tilemap->pixmap_pitch_line;
/*TODO*///								mask0 += tilemap->transparency_bitmap_pitch_line;
/*TODO*///								pmap0 += priority_bitmap_pitch_line;
/*TODO*///							}
/*TODO*///						} /* transPrev == eMASKED */
/*TODO*///					} /* transPrev != eWHOLLY_TRANSPARENT */
/*TODO*///					x_start = x_end;
/*TODO*///					transPrev = transCur;
/*TODO*///				}
/*TODO*///				cached_indx++;
/*TODO*///			}
/*TODO*///			if( y_next==y2 ) break; /* we are done! */
/*TODO*///
/*TODO*///			priority_bitmap_baseaddr = priority_bitmap_next;
/*TODO*///			dest_baseaddr = dest_next;
/*TODO*///			source_baseaddr = source_next;
/*TODO*///			mask_baseaddr = mask_next;
/*TODO*///			y = y_next;
/*TODO*///			y_next += tilemap->cached_tile_height;
/*TODO*///
/*TODO*///			if( y_next>=y2 )
/*TODO*///			{
/*TODO*///				y_next = y2;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				dest_next += blit.screen_bitmap_pitch_row;
/*TODO*///				priority_bitmap_next += priority_bitmap_pitch_row;
/*TODO*///				source_next += tilemap->pixmap_pitch_row;
/*TODO*///				mask_next += tilemap->transparency_bitmap_pitch_row;
/*TODO*///			}
/*TODO*///		} /* process next row */
/*TODO*///	} /* not totally clipped */
/*TODO*///})
/*TODO*///
/*TODO*///#undef DATA_TYPE
/*TODO*///#undef DEPTH
/*TODO*///#undef DECLARE
/*TODO*///#endif /* DECLARE */
/*TODO*///
/*TODO*///#ifdef TRANSP
/*TODO*////*************************************************************************************************/
/*TODO*///
/*TODO*////* Each of the following routines draws pixmap and transarency data for a single tile.
/*TODO*/// *
/*TODO*/// * This function returns a per-tile code.  Each bit of this code is 0 if the corresponding
/*TODO*/// * bit is zero in every byte of transparency data in the tile, or 1 if that bit is not
/*TODO*/// * consistant within the tile.
/*TODO*/// *
/*TODO*/// * This precomputer value allows us for any particular tile and mask, to determine if all pixels
/*TODO*/// * in that tile have the same masked transparency value.
/*TODO*/// */
/*TODO*///
/*TODO*///static UINT8 TRANSP(HandleTransparencyBitmask)(struct tilemap *tilemap, UINT32 x0, UINT32 y0, UINT32 flags)
/*TODO*///{
/*TODO*///	UINT32 tile_width = tilemap->cached_tile_width;
/*TODO*///	UINT32 tile_height = tilemap->cached_tile_height;
/*TODO*///	struct mame_bitmap *pixmap = tilemap->pixmap;
/*TODO*///	struct mame_bitmap *transparency_bitmap = tilemap->transparency_bitmap;
/*TODO*///	int pitch = tile_width + tile_info.skip;
/*TODO*///	PAL_INIT;
/*TODO*///	UINT32 *pPenToPixel = tilemap->pPenToPixel[flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX)];
/*TODO*///	const UINT8 *pPenData = tile_info.pen_data;
/*TODO*///	const UINT8 *pSource;
/*TODO*///	UINT32 code_transparent = tile_info.priority;
/*TODO*///	UINT32 code_opaque = code_transparent | TILE_FLAG_FG_OPAQUE;
/*TODO*///	UINT32 tx;
/*TODO*///	UINT32 ty;
/*TODO*///	UINT32 data;
/*TODO*///	UINT32 yx;
/*TODO*///	UINT32 x;
/*TODO*///	UINT32 y;
/*TODO*///	UINT32 pen;
/*TODO*///	UINT8 *pBitmask = tile_info.mask_data;
/*TODO*///	UINT32 bitoffs = 0;
/*TODO*///	int bWhollyOpaque;
/*TODO*///	int bWhollyTransparent;
/*TODO*///
/*TODO*///	bWhollyOpaque = 1;
/*TODO*///	bWhollyTransparent = 1;
/*TODO*///
/*TODO*///	if( flags&TILE_4BPP )
/*TODO*///	{
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width/2; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				data = *pSource++;
/*TODO*///
/*TODO*///				pen = data&0xf;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				if( (pBitmask[bitoffs/8]&(0x80>>(bitoffs&7))) == 0 )
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_transparent;
/*TODO*///					bWhollyOpaque = 0;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_opaque;
/*TODO*///					bWhollyTransparent = 0;
/*TODO*///				}
/*TODO*///				bitoffs++;
/*TODO*///
/*TODO*///				pen = data>>4;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				if( (pBitmask[bitoffs/8]&(0x80>>(bitoffs&7))) == 0 )
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_transparent;
/*TODO*///					bWhollyOpaque = 0;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_opaque;
/*TODO*///					bWhollyTransparent = 0;
/*TODO*///				}
/*TODO*///				bitoffs++;
/*TODO*///			}
/*TODO*///			pPenData += pitch/2;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				pen = *pSource++;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				if( (pBitmask[bitoffs/8]&(0x80>>(bitoffs&7))) == 0 )
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_transparent;
/*TODO*///					bWhollyOpaque = 0;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_opaque;
/*TODO*///					bWhollyTransparent = 0;
/*TODO*///				}
/*TODO*///				bitoffs++;
/*TODO*///			}
/*TODO*///			pPenData += pitch;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	return (bWhollyOpaque || bWhollyTransparent)?0:TILE_FLAG_FG_OPAQUE;
/*TODO*///}
/*TODO*///
    public static DrawTileHandlerPtr HandleTransparencyColor_ind = new DrawTileHandlerPtr() {
        public int handler(struct_tilemap tilemap, int x0, int y0, int flags) {
            int tile_width = tilemap.cached_tile_width;
            int tile_height = tilemap.cached_tile_height;
            mame_bitmap pixmap = tilemap.pixmap;
            mame_bitmap transparency_bitmap = tilemap.transparency_bitmap;
            int pitch = tile_width + tile_info.skip;
            PAL_INIT();
            IntArray pPenToPixel = new IntArray(tilemap.pPenToPixel, flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX));
            UBytePtr pPenData = new UBytePtr(tile_info.pen_data);
            UBytePtr pSource = null;
            int code_transparent = tile_info.priority;
            int code_opaque = code_transparent | TILE_FLAG_FG_OPAQUE;
            int tx;
            int ty;
            int data;
            int yx;
            int x;
            int y;
            int pen;
            int transparent_color = tilemap.transparent_pen;
            int bWhollyOpaque;
            int bWhollyTransparent;

            bWhollyOpaque = 1;
            bWhollyTransparent = 1;

            if(( flags&TILE_4BPP ) != 0)
            {
                    for( ty=tile_height; ty!=0; ty-- )
                    {
                            pSource = new UBytePtr(pPenData);
                            for( tx=tile_width/2; tx!=0; tx-- )
                            {
                                    data = pSource.readinc();

                                    pen = data&0xf;
                                    yx = pPenToPixel.read();
                                    pPenToPixel.offset++;
                                    x = x0+(yx%MAX_TILESIZE);
                                    y = y0+(yx/MAX_TILESIZE);
                                    new UShortPtr(pixmap.line[y], x).write((char) PAL_GET(pen));
                                    if( PAL_GET(pen)==transparent_color )
                                    {
                                            new UBytePtr(transparency_bitmap.line[y], x).write( code_transparent );
                                            bWhollyOpaque = 0;
                                    }
                                    else
                                    {
                                            new UBytePtr(transparency_bitmap.line[y], x).write( code_opaque );
                                            bWhollyTransparent = 0;
                                    }

                                    pen = data>>4;
                                    yx = pPenToPixel.read();
                                    pPenToPixel.offset++;
                                    x = x0+(yx%MAX_TILESIZE);
                                    y = y0+(yx/MAX_TILESIZE);
                                    new UShortPtr(pixmap.line[y], x).write((char) PAL_GET(pen));
                                    if( PAL_GET(pen)==transparent_color )
                                    {
                                            new UBytePtr(transparency_bitmap.line[y], x).write( code_transparent );
                                            bWhollyOpaque = 0;
                                    }
                                    else
                                    {
                                            new UBytePtr(transparency_bitmap.line[y], x).write( code_opaque );
                                            bWhollyTransparent = 0;
                                    }
                            }
                            pPenData.offset += pitch/2;
                    }
            }
            else
            {
                    for( ty=tile_height; ty!=0; ty-- )
                    {
                            pSource = new UBytePtr(pPenData);
                            for( tx=tile_width; tx!=0; tx-- )
                            {
                                    pen = pSource.readinc();
                                    yx = pPenToPixel.read();
                                    pPenToPixel.offset++;
                                    x = x0+(yx%MAX_TILESIZE);
                                    y = y0+(yx/MAX_TILESIZE);
                                    new UShortPtr(pixmap.line[y], x).write((char) PAL_GET(pen));
                                    if( PAL_GET(pen)==transparent_color )
                                    {
                                            new UBytePtr(transparency_bitmap.line[y], x).write( code_transparent );
                                            bWhollyOpaque = 0;
                                    }
                                    else
                                    {
                                            new UBytePtr(transparency_bitmap.line[y], x).write( code_opaque );
                                            bWhollyTransparent = 0;
                                    }
                            }
                            pPenData.offset += pitch;
                    }
            }
            return (bWhollyOpaque!=0 || bWhollyTransparent!=0)?0:TILE_FLAG_FG_OPAQUE;
        }
    };
/*TODO*///static UINT8 TRANSP(HandleTransparencyColor)(struct tilemap *tilemap, UINT32 x0, UINT32 y0, UINT32 flags)
/*TODO*///{
/*TODO*///	UINT32 tile_width = tilemap->cached_tile_width;
/*TODO*///	UINT32 tile_height = tilemap->cached_tile_height;
/*TODO*///	struct mame_bitmap *pixmap = tilemap->pixmap;
/*TODO*///	struct mame_bitmap *transparency_bitmap = tilemap->transparency_bitmap;
/*TODO*///	int pitch = tile_width + tile_info.skip;
/*TODO*///	PAL_INIT;
/*TODO*///	UINT32 *pPenToPixel = tilemap->pPenToPixel[flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX)];
/*TODO*///	const UINT8 *pPenData = tile_info.pen_data;
/*TODO*///	const UINT8 *pSource;
/*TODO*///	UINT32 code_transparent = tile_info.priority;
/*TODO*///	UINT32 code_opaque = code_transparent | TILE_FLAG_FG_OPAQUE;
/*TODO*///	UINT32 tx;
/*TODO*///	UINT32 ty;
/*TODO*///	UINT32 data;
/*TODO*///	UINT32 yx;
/*TODO*///	UINT32 x;
/*TODO*///	UINT32 y;
/*TODO*///	UINT32 pen;
/*TODO*///	UINT32 transparent_color = tilemap->transparent_pen;
/*TODO*///	int bWhollyOpaque;
/*TODO*///	int bWhollyTransparent;
/*TODO*///
/*TODO*///	bWhollyOpaque = 1;
/*TODO*///	bWhollyTransparent = 1;
/*TODO*///
/*TODO*///	if( flags&TILE_4BPP )
/*TODO*///	{
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width/2; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				data = *pSource++;
/*TODO*///
/*TODO*///				pen = data&0xf;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				if( PAL_GET(pen)==transparent_color )
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_transparent;
/*TODO*///					bWhollyOpaque = 0;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_opaque;
/*TODO*///					bWhollyTransparent = 0;
/*TODO*///				}
/*TODO*///
/*TODO*///				pen = data>>4;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				if( PAL_GET(pen)==transparent_color )
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_transparent;
/*TODO*///					bWhollyOpaque = 0;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_opaque;
/*TODO*///					bWhollyTransparent = 0;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			pPenData += pitch/2;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				pen = *pSource++;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				if( PAL_GET(pen)==transparent_color )
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_transparent;
/*TODO*///					bWhollyOpaque = 0;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_opaque;
/*TODO*///					bWhollyTransparent = 0;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			pPenData += pitch;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	return (bWhollyOpaque || bWhollyTransparent)?0:TILE_FLAG_FG_OPAQUE;
/*TODO*///}
/*TODO*///
    
    public static DrawTileHandlerPtr HandleTransparencyPen_raw = new DrawTileHandlerPtr() {
        public int handler(struct_tilemap tilemap, int x0, int y0, int flags) {
            System.out.println("HandleTransparencyPen_raw");
            //return HandleTransparencyPen_ind.handler(tilemap, x0, y0, flags);
            return 1;
        }
    };
    
    public static DrawTileHandlerPtr HandleTransparencyPen_ind = new DrawTileHandlerPtr() {
        public int handler(struct_tilemap tilemap, int x0, int y0, int flags) {
            //System.out.println("HandleTransparencyPen_ind");
            int tile_width = tilemap.cached_tile_width;
            int tile_height = tilemap.cached_tile_height;
            mame_bitmap pixmap = tilemap.pixmap;
            mame_bitmap transparency_bitmap = tilemap.transparency_bitmap;
            int pitch = tile_width + tile_info.skip;
            PAL_INIT();
            IntArray pPenToPixel = new IntArray(tilemap.pPenToPixel, flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX));
            UBytePtr pPenData = new UBytePtr(tile_info.pen_data);
            UBytePtr pSource = new UBytePtr();
            int code_transparent = tile_info.priority;
            int code_opaque = code_transparent | TILE_FLAG_FG_OPAQUE;
            int tx;
            int ty;
            int data;
            int yx;
            int x;
            int y;
            int pen;
            int transparent_pen = tilemap.transparent_pen;
            int bWhollyOpaque;
            int bWhollyTransparent;

            bWhollyOpaque = 1;
            bWhollyTransparent = 1;

            if (( flags&TILE_IGNORE_TRANSPARENCY ) != 0)
            {
                    transparent_pen = ~0;
            }

            if (( flags&TILE_4BPP ) != 0)
            {
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width/2; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				data = *pSource++;
/*TODO*///
/*TODO*///				pen = data&0xf;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap.line[y]) = PAL_GET(pen);
/*TODO*///				if( pen==transparent_pen )
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap.line[y])[x] = code_transparent;
/*TODO*///					bWhollyOpaque = 0;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap.line[y])[x] = code_opaque;
/*TODO*///					bWhollyTransparent = 0;
/*TODO*///				}
/*TODO*///
/*TODO*///				pen = data>>4;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap.line[y]) = PAL_GET(pen);
/*TODO*///				((UINT8 *)transparency_bitmap.line[y])[x] = (pen==transparent_pen)?code_transparent:code_opaque;
/*TODO*///			}
/*TODO*///			pPenData += pitch/2;
/*TODO*///		}
            }
            else
            {
		for( ty=tile_height; ty!=0; ty-- )
		{
			pSource = new UBytePtr(pPenData);
			for( tx=tile_width; tx!=0; tx-- )
			{
				pen = pSource.readinc();
				yx = pPenToPixel.read();
                                pPenToPixel.offset++;
				x = x0+(yx%MAX_TILESIZE);
				y = y0+(yx/MAX_TILESIZE);
				(new UShortPtr(pixmap.line[y], x)).write((char) PAL_GET(pen));
				if( pen==transparent_pen )
				{
					(new UBytePtr(transparency_bitmap.line[y], x)).write( code_transparent );
					bWhollyOpaque = 0;

				}
				else
				{
					(new UBytePtr(transparency_bitmap.line[y], x)).write( code_opaque );
					bWhollyTransparent = 0;
				}
			}
			pPenData.inc( pitch );
		}
            }

            return (bWhollyOpaque!=0 || bWhollyTransparent!=0)?0:TILE_FLAG_FG_OPAQUE;
        }
    };
/*TODO*///static UINT8 TRANSP(HandleTransparencyPen)(struct tilemap *tilemap, UINT32 x0, UINT32 y0, UINT32 flags)
/*TODO*///{
/*TODO*///	UINT32 tile_width = tilemap->cached_tile_width;
/*TODO*///	UINT32 tile_height = tilemap->cached_tile_height;
/*TODO*///	struct mame_bitmap *pixmap = tilemap->pixmap;
/*TODO*///	struct mame_bitmap *transparency_bitmap = tilemap->transparency_bitmap;
/*TODO*///	int pitch = tile_width + tile_info.skip;
/*TODO*///	PAL_INIT;
/*TODO*///	UINT32 *pPenToPixel = tilemap->pPenToPixel[flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX)];
/*TODO*///	const UINT8 *pPenData = tile_info.pen_data;
/*TODO*///	const UINT8 *pSource;
/*TODO*///	UINT32 code_transparent = tile_info.priority;
/*TODO*///	UINT32 code_opaque = code_transparent | TILE_FLAG_FG_OPAQUE;
/*TODO*///	UINT32 tx;
/*TODO*///	UINT32 ty;
/*TODO*///	UINT32 data;
/*TODO*///	UINT32 yx;
/*TODO*///	UINT32 x;
/*TODO*///	UINT32 y;
/*TODO*///	UINT32 pen;
/*TODO*///	UINT32 transparent_pen = tilemap->transparent_pen;
/*TODO*///	int bWhollyOpaque;
/*TODO*///	int bWhollyTransparent;
/*TODO*///
/*TODO*///	bWhollyOpaque = 1;
/*TODO*///	bWhollyTransparent = 1;
/*TODO*///
/*TODO*///	if( flags&TILE_IGNORE_TRANSPARENCY )
/*TODO*///	{
/*TODO*///		transparent_pen = ~0;
/*TODO*///	}
/*TODO*///
/*TODO*///	if( flags&TILE_4BPP )
/*TODO*///	{
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width/2; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				data = *pSource++;
/*TODO*///
/*TODO*///				pen = data&0xf;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				if( pen==transparent_pen )
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_transparent;
/*TODO*///					bWhollyOpaque = 0;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_opaque;
/*TODO*///					bWhollyTransparent = 0;
/*TODO*///				}
/*TODO*///
/*TODO*///				pen = data>>4;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				((UINT8 *)transparency_bitmap->line[y])[x] = (pen==transparent_pen)?code_transparent:code_opaque;
/*TODO*///			}
/*TODO*///			pPenData += pitch/2;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				pen = *pSource++;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				if( pen==transparent_pen )
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_transparent;
/*TODO*///					bWhollyOpaque = 0;
/*TODO*///
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					((UINT8 *)transparency_bitmap->line[y])[x] = code_opaque;
/*TODO*///					bWhollyTransparent = 0;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			pPenData += pitch;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	return (bWhollyOpaque || bWhollyTransparent)?0:TILE_FLAG_FG_OPAQUE;
/*TODO*///}
/*TODO*///
/*TODO*///static UINT8 TRANSP(HandleTransparencyPenBit)(struct tilemap *tilemap, UINT32 x0, UINT32 y0, UINT32 flags)
/*TODO*///{
/*TODO*///	UINT32 tile_width = tilemap->cached_tile_width;
/*TODO*///	UINT32 tile_height = tilemap->cached_tile_height;
/*TODO*///	struct mame_bitmap *pixmap = tilemap->pixmap;
/*TODO*///	struct mame_bitmap *transparency_bitmap = tilemap->transparency_bitmap;
/*TODO*///	int pitch = tile_width + tile_info.skip;
/*TODO*///	PAL_INIT;
/*TODO*///	UINT32 *pPenToPixel = tilemap->pPenToPixel[flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX)];
/*TODO*///	const UINT8 *pPenData = tile_info.pen_data;
/*TODO*///	const UINT8 *pSource;
/*TODO*///	UINT32 tx;
/*TODO*///	UINT32 ty;
/*TODO*///	UINT32 data;
/*TODO*///	UINT32 yx;
/*TODO*///	UINT32 x;
/*TODO*///	UINT32 y;
/*TODO*///	UINT32 pen;
/*TODO*///	UINT32 penbit = tilemap->transparent_pen;
/*TODO*///	UINT32 code_front = tile_info.priority | TILE_FLAG_FG_OPAQUE;
/*TODO*///	UINT32 code_back = tile_info.priority | TILE_FLAG_BG_OPAQUE;
/*TODO*///	int code;
/*TODO*///	int and_flags = ~0;
/*TODO*///	int or_flags = 0;
/*TODO*///
/*TODO*///	if( flags&TILE_4BPP )
/*TODO*///	{
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width/2; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				data = *pSource++;
/*TODO*///
/*TODO*///				pen = data&0xf;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				code = ((pen&penbit)==penbit)?code_front:code_back;
/*TODO*///				and_flags &= code;
/*TODO*///				or_flags |= code;
/*TODO*///				((UINT8 *)transparency_bitmap->line[y])[x] = code;
/*TODO*///
/*TODO*///				pen = data>>4;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				code = ((pen&penbit)==penbit)?code_front:code_back;
/*TODO*///				and_flags &= code;
/*TODO*///				or_flags |= code;
/*TODO*///				((UINT8 *)transparency_bitmap->line[y])[x] = code;
/*TODO*///			}
/*TODO*///			pPenData += pitch/2;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				pen = *pSource++;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				code = ((pen&penbit)==penbit)?code_front:code_back;
/*TODO*///				and_flags &= code;
/*TODO*///				or_flags |= code;
/*TODO*///				((UINT8 *)transparency_bitmap->line[y])[x] = code;
/*TODO*///			}
/*TODO*///			pPenData += pitch;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	return or_flags ^ and_flags;
/*TODO*///}
/*TODO*///
    public static DrawTileHandlerPtr HandleTransparencyPens_raw = new DrawTileHandlerPtr() {
        public int handler(struct_tilemap tilemap, int x0, int y0, int flags) {
            //System.out.println("Running HandleTransparencyPens_raw");
            int tile_width = tilemap.cached_tile_width;
            int tile_height = tilemap.cached_tile_height;
            mame_bitmap pixmap = tilemap.pixmap;
            mame_bitmap transparency_bitmap = tilemap.transparency_bitmap;
            int pitch = tile_width + tile_info.skip;
            /*TODO*///PAL_INIT;
            IntArray pPenToPixel = new IntArray(tilemap.pPenToPixel, flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX));
            UBytePtr pPenData = new UBytePtr(tile_info.pen_data);
            UBytePtr pSource = new UBytePtr();
            int code_transparent = tile_info.priority;
            int tx;
            int ty;
            int data;
            int yx;
            int x;
            int y;
            int pen;
            int fgmask = tilemap.fgmask[(flags>>TILE_SPLIT_OFFSET)&3];
            int bgmask = tilemap.bgmask[(flags>>TILE_SPLIT_OFFSET)&3];
            int code;
            int and_flags = ~0;
            int or_flags = 0;

            if (( flags&TILE_4BPP ) != 0)
            {
                System.out.println("4BPP");
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width/2; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				data = *pSource++;
/*TODO*///
/*TODO*///				pen = data&0xf;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				code = code_transparent;
/*TODO*///				if( !((1<<pen)&fgmask) ) code |= TILE_FLAG_FG_OPAQUE;
/*TODO*///				if( !((1<<pen)&bgmask) ) code |= TILE_FLAG_BG_OPAQUE;
/*TODO*///				and_flags &= code;
/*TODO*///				or_flags |= code;
/*TODO*///				((UINT8 *)transparency_bitmap->line[y])[x] = code;
/*TODO*///
/*TODO*///				pen = data>>4;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				code = code_transparent;
/*TODO*///				if( !((1<<pen)&fgmask) ) code |= TILE_FLAG_FG_OPAQUE;
/*TODO*///				if( !((1<<pen)&bgmask) ) code |= TILE_FLAG_BG_OPAQUE;
/*TODO*///				and_flags &= code;
/*TODO*///				or_flags |= code;
/*TODO*///				((UINT8 *)transparency_bitmap->line[y])[x] = code;
/*TODO*///			}
/*TODO*///			pPenData += pitch/2;
/*TODO*///		}
            }
            else
            {
                
		for( ty=tile_height; ty!=0; ty-- )
		{
			pSource = new UBytePtr(pPenData);
			for( tx=tile_width; tx!=0; tx-- )
			{
				pen = pSource.readinc();
				yx = pPenToPixel.read();
                                pPenToPixel.offset++;
				x = x0+(yx%MAX_TILESIZE);
				y = y0+(yx/MAX_TILESIZE);
				(new UShortPtr(pixmap.line[y], x)).write( (char) PAL_GET(pen) );
				code = code_transparent;
				if( ((1<<pen)&fgmask) == 0 ) code |= TILE_FLAG_FG_OPAQUE;
				if( ((1<<pen)&bgmask) == 0 ) code |= TILE_FLAG_BG_OPAQUE;
				and_flags &= code;
				or_flags |= code;
				(new UBytePtr(transparency_bitmap.line[y])).write(x, code);
			}
			pPenData.inc( pitch );
		}
            }
            return and_flags ^ or_flags;
        }
    };
/*TODO*///static UINT8 TRANSP(HandleTransparencyPens)(struct tilemap *tilemap, UINT32 x0, UINT32 y0, UINT32 flags)
/*TODO*///{
/*TODO*///	UINT32 tile_width = tilemap->cached_tile_width;
/*TODO*///	UINT32 tile_height = tilemap->cached_tile_height;
/*TODO*///	struct mame_bitmap *pixmap = tilemap->pixmap;
/*TODO*///	struct mame_bitmap *transparency_bitmap = tilemap->transparency_bitmap;
/*TODO*///	int pitch = tile_width + tile_info.skip;
/*TODO*///	PAL_INIT;
/*TODO*///	UINT32 *pPenToPixel = tilemap->pPenToPixel[flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX)];
/*TODO*///	const UINT8 *pPenData = tile_info.pen_data;
/*TODO*///	const UINT8 *pSource;
/*TODO*///	UINT32 code_transparent = tile_info.priority;
/*TODO*///	UINT32 tx;
/*TODO*///	UINT32 ty;
/*TODO*///	UINT32 data;
/*TODO*///	UINT32 yx;
/*TODO*///	UINT32 x;
/*TODO*///	UINT32 y;
/*TODO*///	UINT32 pen;
/*TODO*///	UINT32 fgmask = tilemap->fgmask[(flags>>TILE_SPLIT_OFFSET)&3];
/*TODO*///	UINT32 bgmask = tilemap->bgmask[(flags>>TILE_SPLIT_OFFSET)&3];
/*TODO*///	UINT32 code;
/*TODO*///	int and_flags = ~0;
/*TODO*///	int or_flags = 0;
/*TODO*///
/*TODO*///	if( flags&TILE_4BPP )
/*TODO*///	{
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width/2; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				data = *pSource++;
/*TODO*///
/*TODO*///				pen = data&0xf;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				code = code_transparent;
/*TODO*///				if( !((1<<pen)&fgmask) ) code |= TILE_FLAG_FG_OPAQUE;
/*TODO*///				if( !((1<<pen)&bgmask) ) code |= TILE_FLAG_BG_OPAQUE;
/*TODO*///				and_flags &= code;
/*TODO*///				or_flags |= code;
/*TODO*///				((UINT8 *)transparency_bitmap->line[y])[x] = code;
/*TODO*///
/*TODO*///				pen = data>>4;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				code = code_transparent;
/*TODO*///				if( !((1<<pen)&fgmask) ) code |= TILE_FLAG_FG_OPAQUE;
/*TODO*///				if( !((1<<pen)&bgmask) ) code |= TILE_FLAG_BG_OPAQUE;
/*TODO*///				and_flags &= code;
/*TODO*///				or_flags |= code;
/*TODO*///				((UINT8 *)transparency_bitmap->line[y])[x] = code;
/*TODO*///			}
/*TODO*///			pPenData += pitch/2;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				pen = *pSource++;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				code = code_transparent;
/*TODO*///				if( !((1<<pen)&fgmask) ) code |= TILE_FLAG_FG_OPAQUE;
/*TODO*///				if( !((1<<pen)&bgmask) ) code |= TILE_FLAG_BG_OPAQUE;
/*TODO*///				and_flags &= code;
/*TODO*///				or_flags |= code;
/*TODO*///				((UINT8 *)transparency_bitmap->line[y])[x] = code;
/*TODO*///			}
/*TODO*///			pPenData += pitch;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	return and_flags ^ or_flags;
/*TODO*///}

    public static DrawTileHandlerPtr HandleTransparencyNone_ind = new DrawTileHandlerPtr() {
        public int handler(struct_tilemap tilemap, int x0, int y0, int flags) {
            int tile_width = tilemap.cached_tile_width;
            int tile_height = tilemap.cached_tile_height;
            mame_bitmap pixmap = tilemap.pixmap;
            mame_bitmap transparency_bitmap = tilemap.transparency_bitmap;
            int pitch = tile_width + tile_info.skip;
/*TODO*///	PAL_INIT;
            IntArray pPenToPixel = new IntArray(tilemap.pPenToPixel, flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX));
            UBytePtr pPenData = new UBytePtr(tile_info.pen_data);
            UBytePtr pSource;
            int code_opaque = tile_info.priority;
            int tx;
            int ty;
            int data;
            int yx;
            int x;
            int y;
            int pen;

            if( (flags&TILE_4BPP) != 0)
            {
                System.out.println("AA");
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width/2; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				data = *pSource++;
/*TODO*///
/*TODO*///				pen = data&0xf;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap.line[y]) = PAL_GET(pen);
/*TODO*///				((UINT8 *)transparency_bitmap.line[y])[x] = code_opaque;
/*TODO*///
/*TODO*///				pen = data>>4;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap.line[y]) = PAL_GET(pen);
/*TODO*///				((UINT8 *)transparency_bitmap.line[y])[x] = code_opaque;
/*TODO*///			}
/*TODO*///			pPenData += pitch/2;
/*TODO*///		}
            }
            else
            {
                System.out.println("BB");
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				pen = *pSource++;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap.line[y]) = PAL_GET(pen);
/*TODO*///				((UINT8 *)transparency_bitmap.line[y])[x] = code_opaque;
/*TODO*///			}
/*TODO*///			pPenData += pitch;
/*TODO*///		}
            }
            return 0;
        }
    };
    
    public static DrawTileHandlerPtr HandleTransparencyNone_raw = new DrawTileHandlerPtr() {
        public int handler(struct_tilemap tilemap, int x0, int y0, int flags) {
            System.out.println("HandleTransparencyNone_raw 6");
            return HandleTransparencyNone_ind.handler(tilemap, x0, y0, flags);
        }
    };
    
/*TODO*///static UINT8 TRANSP(HandleTransparencyNone)(struct tilemap *tilemap, UINT32 x0, UINT32 y0, UINT32 flags)
/*TODO*///{
/*TODO*///	UINT32 tile_width = tilemap->cached_tile_width;
/*TODO*///	UINT32 tile_height = tilemap->cached_tile_height;
/*TODO*///	struct mame_bitmap *pixmap = tilemap->pixmap;
/*TODO*///	struct mame_bitmap *transparency_bitmap = tilemap->transparency_bitmap;
/*TODO*///	int pitch = tile_width + tile_info.skip;
/*TODO*///	PAL_INIT;
/*TODO*///	UINT32 *pPenToPixel = tilemap->pPenToPixel[flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX)];
/*TODO*///	const UINT8 *pPenData = tile_info.pen_data;
/*TODO*///	const UINT8 *pSource;
/*TODO*///	UINT32 code_opaque = tile_info.priority;
/*TODO*///	UINT32 tx;
/*TODO*///	UINT32 ty;
/*TODO*///	UINT32 data;
/*TODO*///	UINT32 yx;
/*TODO*///	UINT32 x;
/*TODO*///	UINT32 y;
/*TODO*///	UINT32 pen;
/*TODO*///
/*TODO*///	if( flags&TILE_4BPP )
/*TODO*///	{
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width/2; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				data = *pSource++;
/*TODO*///
/*TODO*///				pen = data&0xf;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				((UINT8 *)transparency_bitmap->line[y])[x] = code_opaque;
/*TODO*///
/*TODO*///				pen = data>>4;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				((UINT8 *)transparency_bitmap->line[y])[x] = code_opaque;
/*TODO*///			}
/*TODO*///			pPenData += pitch/2;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		for( ty=tile_height; ty!=0; ty-- )
/*TODO*///		{
/*TODO*///			pSource = pPenData;
/*TODO*///			for( tx=tile_width; tx!=0; tx-- )
/*TODO*///			{
/*TODO*///				pen = *pSource++;
/*TODO*///				yx = *pPenToPixel++;
/*TODO*///				x = x0+(yx%MAX_TILESIZE);
/*TODO*///				y = y0+(yx/MAX_TILESIZE);
/*TODO*///				*(x+(UINT16 *)pixmap->line[y]) = PAL_GET(pen);
/*TODO*///				((UINT8 *)transparency_bitmap->line[y])[x] = code_opaque;
/*TODO*///			}
/*TODO*///			pPenData += pitch;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*///#undef TRANSP
/*TODO*///#undef PAL_INIT
/*TODO*///#undef PAL_GET
/*TODO*///#endif // TRANSP
}
