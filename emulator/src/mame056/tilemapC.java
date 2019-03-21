/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056;

import static common.libc.cstring.*;
import common.ptr.UBytePtr;
import common.ptr.UShortPtr;
import common.subArrays.IntArray;
import static java.lang.System.exit;
import static mame056.common.bitmap_alloc_depth;
import static mame056.common.bitmap_free;
import mame056.commonH.mame_bitmap;
import static mame056.drawgfxH.*;
import static mame056.driverH.*;
import static mame056.mame.Machine;
import static mame056.tilemapH.*;

public class tilemapC
{
	
            public static void SWAP(int X, int Y) { int temp=X; X=Y; Y=temp; }
            public static int MAX_TILESIZE = 32;

            public static int TILE_FLAG_DIRTY = (0x80);
            
            public static int eWHOLLY_TRANSPARENT = 0;
            public static int eWHOLLY_OPAQUE = 1;
            public static int eMASKED = 2;

            public static enum trans_t { eWHOLLY_TRANSPARENT, eWHOLLY_OPAQUE, eMASKED };
            
            public static abstract interface _draw {
                public abstract void handler(tilemap tilemap, int xpos, int ypos, int mask, int value);
            }

            public static abstract interface _draw_tile {
                public abstract int handler(tilemap tilemap, int col, int row, int flags, boolean ind );
            }

        public static abstract interface GetTileInfoPtr {

            public abstract void handler(int memory_offset);
        }
        
        public static class tilemap
	{
                public TilemapScanHandler get_memory_offset;
                public int[] memory_offset_to_cached_indx;
                public int[] cached_indx_to_memory_offset;
                public int[] logical_flip_to_cached_flip=new int[4];

		/* callback to interpret video RAM for the tilemap */
                public GetTileInfoPtr tile_get_info;

		public int max_memory_offset;
                public int num_tiles;
                public int num_pens;

                public int num_logical_rows, num_logical_cols;
                public int num_cached_rows, num_cached_cols;

                public int logical_tile_width, logical_tile_height;
		public int cached_tile_width, cached_tile_height;

                public int cached_width, cached_height;

/*TODO*///		int dx, dx_if_flipped;
/*TODO*///		int dy, dy_if_flipped;
                public int scrollx_delta, scrolly_delta;

		public int enable;
                public int attributes;

		public int type;
                public int transparent_pen;
                public int[] fgmask=new int[4], bgmask=new int[4]; /* for TILEMAP_SPLIT */

                public UShortPtr[] pPenToPixel=new UShortPtr[8*8*8];

                public _draw_tile draw_tile;

                public _draw draw;

		public int cached_scroll_rows, cached_scroll_cols;
		public int[] cached_rowscroll, cached_colscroll;
	
		public int logical_scroll_rows, logical_scroll_cols;
                public int[] logical_rowscroll;
                public int[] logical_colscroll;

                public int orientation;
		public int clip_left,clip_right,clip_top,clip_bottom;
                public rectangle logical_clip;

		public int tile_depth, tile_granularity;
                public UBytePtr tile_dirty_map=new UBytePtr();

		/* cached color data */
                public mame_bitmap pixmap;
                public int pixmap_pitch_line;
                public int pixmap_pitch_row;

                public mame_bitmap transparency_bitmap;
                public int transparency_bitmap_pitch_line;
                public int transparency_bitmap_pitch_row;
		public UBytePtr transparency_data;
                public UBytePtr[] transparency_data_row;
	
		public tilemap next; /* resource tracking */
	};
	
        public static mame_bitmap priority_bitmap;
	public static int					priority_bitmap_pitch_line;
	public static int					priority_bitmap_pitch_row;

        public static tilemap first_tilemap; /* resource tracking */
	public static int			screen_width, screen_height;
/*TODO*///	struct tile_info		tile_info;
/*TODO*///	
/*TODO*///	typedef void (blitmask_t)( void *dest, const void *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///	typedef void (*blitopaque_t)( void *dest, const void *source, int count, UINT8 *pri, UINT32 pcode );
	
	/* the following parameters are constant across tilemap_draw calls */
	public static class _blit
	{
		public blitmask_t draw_masked;
		public blitopaque_t draw_opaque;
		public int clip_left, clip_top, clip_right, clip_bottom;
		public int tilemap_priority_code;
		public mame_bitmap	screen_bitmap;
		public int				screen_bitmap_pitch_line;
		public int				screen_bitmap_pitch_row;
	};
        
        public static _blit blit = new _blit();
	
/*TODO*///	/***********************************************************************************/
/*TODO*///	
/*TODO*///	static int PenToPixel_Init( struct tilemap *tilemap );
/*TODO*///	static void PenToPixel_Term( struct tilemap *tilemap );
/*TODO*///	static int mappings_create( struct tilemap *tilemap );
/*TODO*///	static void mappings_dispose( struct tilemap *tilemap );
/*TODO*///	static void mappings_update( struct tilemap *tilemap );
/*TODO*///	static void recalculate_scroll( struct tilemap *tilemap );
/*TODO*///	
        public static abstract interface blitmask_t {
            public abstract void handler(UBytePtr dest, UBytePtr source, UBytePtr pMask, int mask, int value, int count, UBytePtr pri, int pcode);
        }
        
        public static abstract interface blitopaque_t {
            public abstract void handler(UBytePtr dest, UBytePtr source, int count, UBytePtr pri, int pcode);
        }
/*TODO*///	/* {p/n}{blend/draw/invis}{opaque/trans}{16/32} */
/*TODO*///	static void pio( void *dest, const void *source, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///	static void pit( void *dest, const void *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///	
/*TODO*///	static void pdo16( UINT16 *dest, const UINT16 *source, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///	static void pdo15( UINT16 *dest, const UINT16 *source, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///	static void pbo15( UINT16 *dest, const UINT16 *source, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///	static void pdo32( UINT32 *dest, const UINT16 *source, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///	static void pbo32( UINT32 *dest, const UINT16 *source, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///	
/*TODO*///	static void pdt16( UINT16 *dest, const UINT16 *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///	static void pdt15( UINT16 *dest, const UINT16 *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///	static void pbt15( UINT16 *dest, const UINT16 *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///	static void pdt32( UINT32 *dest, const UINT16 *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///	static void pbt32( UINT32 *dest, const UINT16 *source, const UINT8 *pMask, int mask, int value, int count, UINT8 *pri, UINT32 pcode );
/*TODO*///	
/*TODO*///	static void install_draw_handlers( struct tilemap *tilemap );
/*TODO*///	static 
/*TODO*///	static void update_tile_info( struct tilemap *tilemap, UINT32 cached_indx, UINT32 cached_col, UINT32 cached_row );
/*TODO*///	
	/***********************************************************************************/
	
	public static int PenToPixel_Init( tilemap tilemap )
	{
		/*
			Construct a table for all tile orientations in advance.
			This simplifies drawing tiles and masks tremendously.
			If performance is an issue, we can always (re)introduce
			customized code for each case and forgo tables.
		*/
		int i,x,y,tx,ty;
		UShortPtr pPenToPixel=new UShortPtr();
                int _colPen = 0;
		int lError;
	
		lError = 0;
		for( i=0; i<8; i++ )
		{
			pPenToPixel = new UShortPtr( tilemap.num_pens * 128 );
			if( pPenToPixel==null )
			{
				lError = 1;
			}
			else
			{
				tilemap.pPenToPixel[i] = pPenToPixel;
				for( ty=0; ty<tilemap.logical_tile_height; ty++ )
				{
					for( tx=0; tx<tilemap.logical_tile_width; tx++ )
					{
						if(( i&TILE_SWAPXY ) != 0)
						{
							x = ty;
							y = tx;
						}
						else
						{
							x = tx;
							y = ty;
						}
						if(( i&TILE_FLIPX ) != 0) x = tilemap.cached_tile_width-1-x;
						if(( i&TILE_FLIPY ) != 0) y = tilemap.cached_tile_height-1-y;
						pPenToPixel.write(_colPen++, (char) (x+y*MAX_TILESIZE));
					}
				}
			}
		}
		return lError;
	}
	
	public static void PenToPixel_Term( tilemap tilemap )
	{
		int i;
		for( i=0; i<8; i++ )
		{
			tilemap.pPenToPixel[i] = null;
		}
	}
	
	public static void tilemap_set_transparent_pen( tilemap tilemap, int pen )
	{
		tilemap.transparent_pen = pen;
	}
	
/*TODO*///	void tilemap_set_transmask( struct tilemap *tilemap, int which, UINT32 fgmask, UINT32 bgmask )
/*TODO*///	{
/*TODO*///		if( tilemap->fgmask[which] != fgmask || tilemap->bgmask[which] != bgmask )
/*TODO*///		{
/*TODO*///			tilemap->fgmask[which] = fgmask;
/*TODO*///			tilemap->bgmask[which] = bgmask;
/*TODO*///			tilemap_mark_all_tiles_dirty( tilemap );
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	void tilemap_set_depth( struct tilemap *tilemap, int tile_depth, int tile_granularity )
/*TODO*///	{
/*TODO*///		if( tilemap->tile_dirty_map )
/*TODO*///		{
/*TODO*///			free( tilemap->tile_dirty_map);
/*TODO*///		}
/*TODO*///		tilemap->tile_dirty_map = malloc( Machine->drv->total_colors >> tile_granularity );
/*TODO*///		if( tilemap->tile_dirty_map )
/*TODO*///		{
/*TODO*///			tilemap->tile_depth = tile_depth;
/*TODO*///			tilemap->tile_granularity = tile_granularity;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
	/***********************************************************************************/
	/* some common mappings */
        
        public static abstract interface TilemapScanHandler {
            public abstract int handler(int col, int row, int num_cols, int num_rows);
        }
	
	public static TilemapScanHandler tilemap_scan_rows = new TilemapScanHandler() {
            public int handler(int col, int row, int num_cols, int num_rows) {
                /* logical (col,row) -> memory offset */
                return row*num_cols + col;
            }
        };
		
/*TODO*///	UINT32 tilemap_scan_cols( UINT32 col, UINT32 row, UINT32 num_cols, UINT32 num_rows )
/*TODO*///	{
/*TODO*///		/* logical (col,row) -> memory offset */
/*TODO*///		return col*num_rows + row;
/*TODO*///	}
/*TODO*///	
	/***********************************************************************************/
	
	public static int mappings_create( tilemap tilemap )
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
				int memory_offset = tilemap.get_memory_offset.handler( col, row, num_logical_cols, num_logical_rows );
				if( memory_offset>max_memory_offset ) max_memory_offset = memory_offset;
			}
		}
		max_memory_offset++;
		tilemap.max_memory_offset = max_memory_offset;
		/* logical to cached (tilemap_mark_dirty) */
		tilemap.memory_offset_to_cached_indx = new int[ max_memory_offset ];
		if( tilemap.memory_offset_to_cached_indx != null)
		{
			/* cached to logical (get_tile_info) */
			tilemap.cached_indx_to_memory_offset = new int[ tilemap.num_tiles ];
			if( tilemap.cached_indx_to_memory_offset != null) return 0; /* no error */
			tilemap.memory_offset_to_cached_indx = null;
		}
		return -1; /* error */
	}
	
	public static void mappings_dispose( tilemap tilemap )
	{
		//free( tilemap->cached_indx_to_memory_offset );
                tilemap.cached_indx_to_memory_offset = null;
		//free( tilemap->memory_offset_to_cached_indx );
                tilemap.memory_offset_to_cached_indx = null;
	}
	
	public static void mappings_update( tilemap tilemap )
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
			int memory_offset = tilemap.get_memory_offset.handler( logical_col, logical_row, num_logical_cols, num_logical_rows );
			int cached_col = logical_col;
			int cached_row = logical_row;
			if(( tilemap.orientation & ORIENTATION_SWAP_XY ) != 0) SWAP(cached_col,cached_row);
			if(( tilemap.orientation & ORIENTATION_FLIP_X ) != 0) cached_col = (num_cached_cols-1)-cached_col;
			if(( tilemap.orientation & ORIENTATION_FLIP_Y ) != 0) cached_row = (num_cached_rows-1)-cached_row;
			cached_indx = cached_row*num_cached_cols+cached_col;
			tilemap.memory_offset_to_cached_indx[memory_offset] = cached_indx;
			tilemap.cached_indx_to_memory_offset[cached_indx] = memory_offset;
		}
		for( logical_flip = 0; logical_flip<4; logical_flip++ )
		{
			int cached_flip = logical_flip;
			if(( tilemap.attributes&TILEMAP_FLIPX ) != 0) cached_flip ^= TILE_FLIPX;
			if(( tilemap.attributes&TILEMAP_FLIPY ) != 0) cached_flip ^= TILE_FLIPY;
	//#ifndef PREROTATE_GFX
			if(( Machine.orientation & ORIENTATION_SWAP_XY ) != 0)
			{
				if(( Machine.orientation & ORIENTATION_FLIP_X ) != 0) cached_flip ^= TILE_FLIPY;
				if(( Machine.orientation & ORIENTATION_FLIP_Y ) != 0) cached_flip ^= TILE_FLIPX;
			}
			else
			{
				if(( Machine.orientation & ORIENTATION_FLIP_X ) != 0) cached_flip ^= TILE_FLIPX;
				if(( Machine.orientation & ORIENTATION_FLIP_Y ) != 0) cached_flip ^= TILE_FLIPY;
			}
	//#endif
			if(( tilemap.orientation & ORIENTATION_SWAP_XY ) != 0)
			{
				cached_flip = ((cached_flip&1)<<1) | ((cached_flip&2)>>1);
			}
			tilemap.logical_flip_to_cached_flip[logical_flip] = cached_flip;
		}
	}
	
	/***********************************************************************************/
	
	public static blitopaque_t pio = new blitopaque_t() {
            public void handler(UBytePtr dest, UBytePtr source, int count, UBytePtr pri, int pcode) {
                int i;
		for( i=0; i<count; i++ )
		{
			pri.write(i, (pri.read(i) | pcode));
		}
            }
        };
		
	public static blitmask_t pit = new blitmask_t() {
            public void handler(UBytePtr dest, UBytePtr source, UBytePtr pMask, int mask, int value, int count, UBytePtr pri, int pcode) {
                int i;
		for( i=0; i<count; i++ )
		{
			if( (pMask.read(i)&mask)==value )
			{
				pri.write(i, (pri.read(i) | pcode));
			}
		}
            }
        };
		
	/***********************************************************************************/
	
	public static blitopaque_t pdo16 = new blitopaque_t() {
            public void handler(UBytePtr dest, UBytePtr source, int count, UBytePtr pri, int pcode) {
                int i;
		memcpy( dest,source,count );
		for( i=0; i<count; i++ )
		{
			pri.write(i, pri.read(i) | pcode);
		}
            }
        };
		
	public static blitopaque_t pdo15 = new blitopaque_t() {
            public void handler(UBytePtr dest, UBytePtr source, int count, UBytePtr pri, int pcode) {
                int i;
		IntArray clut = Machine.remapped_colortable;
		for( i=0; i<count; i++ )
		{
			dest.write(i, clut.read(source.read(i)));
			pri.write(i, pri.read(i) | pcode);
		}
            }
        };
	
	public static blitopaque_t pdo32 = new blitopaque_t() {
            public void handler(UBytePtr dest, UBytePtr source, int count, UBytePtr pri, int pcode) {
                int i;
		IntArray clut = Machine.remapped_colortable;
		for( i=0; i<count; i++ )
		{
			dest.write(i, clut.read(source.read(i)));
			pri.write(i, pri.read(i) | pcode);
		}
            }
        };
	
	/***********************************************************************************/
	
	public static blitmask_t pdt16 = new blitmask_t() {
            public void handler(UBytePtr dest, UBytePtr source, UBytePtr pMask, int mask, int value, int count, UBytePtr pri, int pcode) {
                int i;
		for( i=0; i<count; i++ )
		{
			if( (pMask.read(i)&mask)==value )
			{
				dest.write(i, source.read(i));
				pri.write(i, pri.read(i) | pcode);
			}
		}
            }
        };

	
	public static blitmask_t pdt15 = new blitmask_t() {
            public void handler(UBytePtr dest, UBytePtr source, UBytePtr pMask, int mask, int value, int count, UBytePtr pri, int pcode) {
                int i;
		IntArray clut = Machine.remapped_colortable;
		for( i=0; i<count; i++ )
		{
			if( (pMask.read(i)&mask)==value )
			{
				dest.write(i, clut.read(source.read(i)));
				pri.write(i, pri.read(i) | pcode);
			}
		}
            }
        };

	
	public static blitmask_t pdt32 = new blitmask_t() {
            public void handler(UBytePtr dest, UBytePtr source, UBytePtr pMask, int mask, int value, int count, UBytePtr pri, int pcode) {
                int i;
		IntArray clut = Machine.remapped_colortable;
		for( i=0; i<count; i++ )
		{
			if( (pMask.read(i)&mask)==value )
			{
				dest.write(i, clut.read(source.read(i)));
				pri.write(i, pri.read(i) | pcode);
			}
		}
            }
        };
		
	
	/***********************************************************************************/
	
	public static blitopaque_t pbo15 = new blitopaque_t() {
            public void handler(UBytePtr dest, UBytePtr source, int count, UBytePtr pri, int pcode) {
                int i;
		IntArray clut = Machine.remapped_colortable;
		for( i=0; i<count; i++ )
		{
			dest.write(i, alpha_blend16(dest.read(i), clut.read(source.read(i))));
			pri.write(i, pri.read(i) | pcode);
		}
            }
        };
		
	public static blitopaque_t pbo32 = new blitopaque_t() {
            public void handler(UBytePtr dest, UBytePtr source, int count, UBytePtr pri, int pcode) {
                int i;
		IntArray clut = Machine.remapped_colortable;
		for( i=0; i<count; i++ )
		{
			dest.write(i, alpha_blend32(dest.read(i), clut.read(source.read(i))));
			pri.write(i, pri.read(i) | pcode);
		}
            }
        };

	
	/***********************************************************************************/
	
	public static blitmask_t pbt15 = new blitmask_t() {
            public void handler(UBytePtr dest, UBytePtr source, UBytePtr pMask, int mask, int value, int count, UBytePtr pri, int pcode) {
                int i;
		IntArray clut = Machine.remapped_colortable;
		for( i=0; i<count; i++ )
		{
			if( (pMask.read(i)&mask)==value )
			{
				dest.write(i, alpha_blend16(dest.read(i), clut.read(source.read(i))));
				pri.write(i, pri.read(i) | pcode);
			}
		}
            }
        };
		
	public static blitmask_t pbt32= new blitmask_t() {
            public void handler(UBytePtr dest, UBytePtr source, UBytePtr pMask, int mask, int value, int count, UBytePtr pri, int pcode) {
                int i;
		IntArray clut = Machine.remapped_colortable;
		for( i=0; i<count; i++ )
		{
			if( (pMask.read(i)&mask)==value )
			{
				dest.write(i, alpha_blend32(dest.read(i), clut.read(source.read(i))));
				pri.write(i, pri.read(i)| pcode);
			}
		}
            }
        };
	
	/***********************************************************************************/
        public static int DEPTH = 8 ;
/*TODO*///	#define DEPTH 16
/*TODO*///	#define DATA_TYPE UINT16
/*TODO*///	#define DECLARE(function,args,body) static void function##16BPP args body
/*TODO*///	
/*TODO*///	#define DEPTH 32
/*TODO*///	#define DATA_TYPE UINT32
/*TODO*///	#define DECLARE(function,args,body) static void function##32BPP args body
/*TODO*///	
        public static IntArray pPalData=new IntArray(1024*128);
        
        public static int PAL_INIT_ind( _tile_info tile_info ){
            pPalData = tile_info.pal_data;
            
            return 0;
        }
        
        public static int PAL_GET_ind(int pen){
            return pPalData.read(pen);
        }
        
/*TODO*///	#define TRANSP(f) f ## _ind
/*TODO*///	
/*TODO*///	#define PAL_INIT_raw int palBase = tile_info.pal_data - Machine->remapped_colortable
/*TODO*///	#define PAL_GET_raw(pen) (palBase + (pen))
/*TODO*///	#define TRANSP(f) f ## _raw
/*TODO*///	
	/*********************************************************************************/
	
	public static void install_draw_handlers( tilemap tilemap )
	{
		tilemap.draw = null;
	
		if( Machine.game_colortable != null )
		{
			if(( tilemap.type & TILEMAP_BITMASK ) != 0){
				System.out.println("1");
                                tilemap.draw_tile = HandleTransparencyBitmask_ind;
                        } else if(( tilemap.type & TILEMAP_SPLIT_PENBIT ) != 0){
				System.out.println("2");
                                tilemap.draw_tile = HandleTransparencyPenBit_ind;
                        } else if(( tilemap.type & TILEMAP_SPLIT ) != 0){
				System.out.println("3");
                                tilemap.draw_tile = HandleTransparencyPens_ind;
                        } else if( tilemap.type==TILEMAP_TRANSPARENT ){
				System.out.println("4");
                                tilemap.draw_tile = HandleTransparencyPen_ind;
                        } else if( tilemap.type==TILEMAP_TRANSPARENT_COLOR ){
				System.out.println("5");
                                tilemap.draw_tile = HandleTransparencyColor_ind;
                        } else {
				System.out.println("6");
                                tilemap.draw_tile = HandleTransparencyNone_ind;
                        }
		}
		else
		{
			if(( tilemap.type & TILEMAP_BITMASK ) != 0)
				tilemap.draw_tile = HandleTransparencyBitmask_raw;
			else if(( tilemap.type & TILEMAP_SPLIT_PENBIT ) != 0)
				tilemap.draw_tile = HandleTransparencyPenBit_raw;
			else if(( tilemap.type & TILEMAP_SPLIT ) != 0)
				tilemap.draw_tile = HandleTransparencyPens_raw;
			else if( tilemap.type==TILEMAP_TRANSPARENT )
				tilemap.draw_tile = HandleTransparencyPen_raw;
			else if( tilemap.type==TILEMAP_TRANSPARENT_COLOR )
				tilemap.draw_tile = HandleTransparencyColor_raw;
			else
				tilemap.draw_tile = HandleTransparencyNone_raw;
		}
		switch( Machine.scrbitmap.depth )
		{
		case 32:
			tilemap.draw			= draw32BPP;
			break;
	
		case 15:
		case 16:
			tilemap.draw			= draw16BPP;
			break;
	
		default:
			exit(1);
			break;
		}
	}
	
/*TODO*///	/***********************************************************************************/
/*TODO*///	
/*TODO*///	static void tilemap_reset(void)
/*TODO*///	{
/*TODO*///		tilemap_mark_all_tiles_dirty(ALL_TILEMAPS);
/*TODO*///	}
/*TODO*///	
	public static int tilemap_init()
	{
		screen_width	= Machine.scrbitmap.width;
		screen_height	= Machine.scrbitmap.height;
		first_tilemap	= null;
	
		/*TODO*///state_save_register_func_postload(tilemap_reset);
		priority_bitmap = bitmap_alloc_depth( screen_width, screen_height, -8 );
		if( priority_bitmap != null )
		{
			priority_bitmap_pitch_line = (priority_bitmap.line[1].read()) - (priority_bitmap.line[0]).read();
			return 0;
		}
		return -1;
	}
	
	public static void tilemap_close()
	{
		tilemap next;
	
		while( first_tilemap != null )
		{
			next = first_tilemap.next;
			tilemap_dispose( first_tilemap );
			first_tilemap = next;
		}
		bitmap_free( priority_bitmap );
	}
	
	/***********************************************************************************/
	
	public static tilemap tilemap_create(
		GetTileInfoPtr get_tile_info,
		TilemapScanHandler get_memory_offset,
		int type,
		int tile_width, int tile_height,
		int num_cols, int num_rows )
	{
		tilemap tilemap;
		int row;
		int num_tiles;
	
		//tilemap = calloc( 1,sizeof( struct tilemap ) );
                tilemap = new tilemap();
		if( tilemap != null )
		{
			num_tiles = num_cols*num_rows;
			tilemap.num_logical_cols = num_cols;
			tilemap.num_logical_rows = num_rows;
			if( (Machine.orientation & ORIENTATION_SWAP_XY) != 0 )
			{
				SWAP( num_cols, num_rows );
				SWAP( tile_width, tile_height );
			}
			tilemap.num_cached_cols = num_cols;
			tilemap.num_cached_rows = num_rows;
			tilemap.num_tiles = num_tiles;
			tilemap.num_pens = tile_width*tile_height;
			tilemap.logical_tile_width = tile_width;
			tilemap.logical_tile_height = tile_height;
			tilemap.cached_tile_width = tile_width;
			tilemap.cached_tile_height = tile_height;
			tilemap.cached_width = tile_width*num_cols;
			tilemap.cached_height = tile_height*num_rows;
			tilemap.tile_get_info = get_tile_info;
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
	
			tilemap.logical_rowscroll	= new int[(tilemap.cached_height)];
			tilemap.cached_rowscroll	= new int[(tilemap.cached_height)];
			tilemap.logical_colscroll	= new int[(tilemap.cached_width)];
			tilemap.cached_colscroll	= new int[(tilemap.cached_width)];
	
			tilemap.transparency_data = new UBytePtr(num_tiles);
			tilemap.transparency_data_row = new UBytePtr[num_rows];
	
			tilemap.pixmap = bitmap_alloc_depth( tilemap.cached_width, tilemap.cached_height, -16 );
			tilemap.transparency_bitmap = bitmap_alloc_depth( tilemap.cached_width, tilemap.cached_height, -8 );
	
			if( (tilemap.logical_rowscroll != null) && (tilemap.cached_rowscroll != null) &&
				(tilemap.logical_colscroll != null) && (tilemap.cached_colscroll != null) &&
				(tilemap.pixmap != null) &&
				(tilemap.transparency_data != null) &&
				(tilemap.transparency_data_row != null) &&
				(tilemap.transparency_bitmap != null) &&
				(mappings_create( tilemap )==0) )
			{
				tilemap.pixmap_pitch_line = ((tilemap.pixmap.line[1].read()) - (tilemap.pixmap.line[0].read()))/2;
				tilemap.pixmap_pitch_row = tilemap.pixmap_pitch_line*tile_height;
	
				tilemap.transparency_bitmap_pitch_line = (tilemap.transparency_bitmap.line[1].read())-(tilemap.transparency_bitmap.line[0].read());
				tilemap.transparency_bitmap_pitch_row = tilemap.transparency_bitmap_pitch_line*tile_height;
	
				for( row=0; row<num_rows; row++ )
				{
					tilemap.transparency_data_row[row] = new UBytePtr(tilemap.transparency_data, num_cols*row);
				}
				install_draw_handlers( tilemap );
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
	
	public static void tilemap_dispose( tilemap tilemap )
	{
		tilemap prev;
	
		if( tilemap==first_tilemap )
		{
			first_tilemap = tilemap.next;
		}
		else
		{
			prev = first_tilemap;
			while( prev.next != tilemap ) prev = prev.next;
			prev.next =tilemap.next;
		}
		PenToPixel_Term( tilemap );
		//free( tilemap.logical_rowscroll );
                tilemap.logical_rowscroll = null;
		//free( tilemap.cached_rowscroll );
                tilemap.cached_rowscroll = null;
		//free( tilemap.logical_colscroll );
                tilemap.logical_colscroll = null;
		//free( tilemap.cached_colscroll );
                tilemap.cached_colscroll = null;
		//free( tilemap.transparency_data );
                tilemap.transparency_data = null;
		//free( tilemap.transparency_data_row );
                tilemap.transparency_data_row = null;
		//bitmap_free( tilemap.transparency_bitmap );
                tilemap.transparency_bitmap = null;
		//bitmap_free( tilemap.pixmap );
                tilemap.pixmap = null;
		mappings_dispose( tilemap );
		//free( tilemap );
                tilemap = null;
	}
	
/*TODO*///	/***********************************************************************************/
/*TODO*///	
/*TODO*///	void tilemap_set_enable( struct tilemap *tilemap, int enable )
/*TODO*///	{
/*TODO*///		tilemap->enable = enable?1:0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
	public static void tilemap_set_flip( tilemap tilemap, int attributes )
	{
/*TODO*///		if( tilemap==ALL_TILEMAPS )
/*TODO*///		{
/*TODO*///			tilemap = first_tilemap;
/*TODO*///			while( tilemap )
/*TODO*///			{
/*TODO*///				tilemap_set_flip( tilemap, attributes );
/*TODO*///				tilemap = tilemap->next;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else if( tilemap->attributes!=attributes )
/*TODO*///		{
/*TODO*///			tilemap->attributes = attributes;
/*TODO*///			tilemap->orientation = Machine->orientation;
/*TODO*///			if( attributes&TILEMAP_FLIPY )
/*TODO*///			{
/*TODO*///				tilemap->orientation ^= ORIENTATION_FLIP_Y;
/*TODO*///			}
/*TODO*///	
/*TODO*///			if( attributes&TILEMAP_FLIPX )
/*TODO*///			{
/*TODO*///				tilemap->orientation ^= ORIENTATION_FLIP_X;
/*TODO*///			}
/*TODO*///	
/*TODO*///			mappings_update( tilemap );
/*TODO*///			recalculate_scroll( tilemap );
/*TODO*///			tilemap_mark_all_tiles_dirty( tilemap );
/*TODO*///		}
	}
	
	public static void tilemap_set_clip( tilemap tilemap, rectangle pClip )
	{
		int left,top,right,bottom;
	
		if( pClip != null )
		{
			tilemap.logical_clip = pClip;
			left	= pClip.min_x;
			top		= pClip.min_y;
			right	= pClip.max_x+1;
			bottom	= pClip.max_y+1;
	
			if(( tilemap.orientation & ORIENTATION_SWAP_XY ) != 0)
			{
				SWAP(left,top);
				SWAP(right,bottom);
			}
	
			if(( tilemap.orientation & ORIENTATION_FLIP_X ) != 0)
			{
				SWAP(left,right);
				left	= screen_width-left;
				right	= screen_width-right;
			}
	
			if(( tilemap.orientation & ORIENTATION_FLIP_Y ) != 0)
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
	
/*TODO*///	/***********************************************************************************/
/*TODO*///	
/*TODO*///	void tilemap_set_scroll_cols( struct tilemap *tilemap, int n )
/*TODO*///	{
/*TODO*///		tilemap->logical_scroll_cols = n;
/*TODO*///		if( tilemap->orientation & ORIENTATION_SWAP_XY )
/*TODO*///		{
/*TODO*///			tilemap->cached_scroll_rows = n;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			tilemap->cached_scroll_cols = n;
/*TODO*///		}
/*TODO*///	}
	
	public static void tilemap_set_scroll_rows( tilemap tilemap, int n )
	{
		tilemap.logical_scroll_rows = n;
		if( (tilemap.orientation & ORIENTATION_SWAP_XY) != 0)
		{
			tilemap.cached_scroll_cols = n;
		}
		else
		{
			tilemap.cached_scroll_rows = n;
		}
	}
	
	/***********************************************************************************/
	
	public static void tilemap_mark_tile_dirty( tilemap tilemap, int memory_offset )
	{
		if( memory_offset<tilemap.max_memory_offset )
		{
			int cached_indx = tilemap.memory_offset_to_cached_indx[memory_offset];
			if( cached_indx>=0 )
			{
				tilemap.transparency_data.write(cached_indx, TILE_FLAG_DIRTY);
			}
		}
	}
	
/*TODO*///	void tilemap_mark_all_tiles_dirty( struct tilemap *tilemap )
/*TODO*///	{
/*TODO*///		if( tilemap==ALL_TILEMAPS )
/*TODO*///		{
/*TODO*///			tilemap = first_tilemap;
/*TODO*///			while( tilemap )
/*TODO*///			{
/*TODO*///				tilemap_mark_all_tiles_dirty( tilemap );
/*TODO*///				tilemap = tilemap->next;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			memset( tilemap->transparency_data, TILE_FLAG_DIRTY, tilemap->num_tiles );
/*TODO*///		}
/*TODO*///	}
	
	/***********************************************************************************/
	
	public static void update_tile_info( tilemap tilemap, int cached_indx, int col, int row )
	{
		int x0;
		int y0;
		int memory_offset;
		int flags;
	
/*TODO*///	profiler_mark(PROFILER_TILEMAP_UPDATE);
	
		memory_offset = tilemap.cached_indx_to_memory_offset[cached_indx];
		//tilemap.tile_get_info( memory_offset );
                tilemap.tile_get_info.handler(memory_offset);
		flags = tile_info.flags;
		flags = (flags&0xfc)|tilemap.logical_flip_to_cached_flip[flags&0x3];
		x0 = tilemap.cached_tile_width*col;
		y0 = tilemap.cached_tile_height*row;
	
		tilemap.transparency_data.write(cached_indx, tilemap.draw_tile.handler(tilemap,x0,y0,flags, true ));
	
/*TODO*///	profiler_mark(PROFILER_END);
	}
/*TODO*///	
/*TODO*///	struct mame_bitmap *tilemap_get_pixmap( struct tilemap * tilemap )
/*TODO*///	{
/*TODO*///		UINT32 cached_indx = 0;
/*TODO*///		UINT32 row,col;
/*TODO*///	
/*TODO*///	profiler_mark(PROFILER_TILEMAP_DRAW);
/*TODO*///		memset( &tile_info, 0x00, sizeof(tile_info) ); /* initialize defaults */
/*TODO*///	
/*TODO*///		/* walk over cached rows/cols (better to walk screen coords) */
/*TODO*///		for( row=0; row<tilemap->num_cached_rows; row++ )
/*TODO*///		{
/*TODO*///			for( col=0; col<tilemap->num_cached_cols; col++ )
/*TODO*///			{
/*TODO*///				if( tilemap->transparency_data[cached_indx] == TILE_FLAG_DIRTY )
/*TODO*///				{
/*TODO*///					update_tile_info( tilemap, cached_indx, col, row );
/*TODO*///				}
/*TODO*///				cached_indx++;
/*TODO*///			} /* next col */
/*TODO*///		} /* next row */
/*TODO*///	
/*TODO*///	profiler_mark(PROFILER_END);
/*TODO*///		return tilemap->pixmap;
/*TODO*///	}
/*TODO*///	
/*TODO*///	struct mame_bitmap *tilemap_get_transparency_bitmap( struct tilemap * tilemap )
/*TODO*///	{
/*TODO*///		return tilemap->transparency_bitmap;
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***********************************************************************************/
/*TODO*///	
/*TODO*///	static void
/*TODO*///	recalculate_scroll( struct tilemap *tilemap )
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		tilemap->scrollx_delta = (tilemap->attributes & TILEMAP_FLIPX )?tilemap->dx_if_flipped:tilemap->dx;
/*TODO*///		tilemap->scrolly_delta = (tilemap->attributes & TILEMAP_FLIPY )?tilemap->dy_if_flipped:tilemap->dy;
/*TODO*///	
/*TODO*///		for( i=0; i<tilemap->logical_scroll_rows; i++ )
/*TODO*///		{
/*TODO*///			tilemap_set_scrollx( tilemap, i, tilemap->logical_rowscroll[i] );
/*TODO*///		}
/*TODO*///		for( i=0; i<tilemap->logical_scroll_cols; i++ )
/*TODO*///		{
/*TODO*///			tilemap_set_scrolly( tilemap, i, tilemap->logical_colscroll[i] );
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	void
/*TODO*///	tilemap_set_scrolldx( struct tilemap *tilemap, int dx, int dx_if_flipped )
/*TODO*///	{
/*TODO*///		tilemap->dx = dx;
/*TODO*///		tilemap->dx_if_flipped = dx_if_flipped;
/*TODO*///		recalculate_scroll( tilemap );
/*TODO*///	}
/*TODO*///	
/*TODO*///	void
/*TODO*///	tilemap_set_scrolldy( struct tilemap *tilemap, int dy, int dy_if_flipped )
/*TODO*///	{
/*TODO*///		tilemap->dy = dy;
/*TODO*///		tilemap->dy_if_flipped = dy_if_flipped;
/*TODO*///		recalculate_scroll( tilemap );
/*TODO*///	}
	
	public static void tilemap_set_scrollx( tilemap tilemap, int which, int value )
	{
		tilemap.logical_rowscroll[which] = value;
		value = tilemap.scrollx_delta-value; /* adjust */
	
		if( (tilemap.orientation & ORIENTATION_SWAP_XY) != 0 )
		{
			/* if xy are swapped, we are actually panning the screen bitmap vertically */
			if( (tilemap.orientation & ORIENTATION_FLIP_X) != 0 )
			{
				/* adjust affected col */
				which = tilemap.cached_scroll_cols-1 - which;
			}
			if( (tilemap.orientation & ORIENTATION_FLIP_Y) != 0 )
			{
				/* adjust scroll amount */
				value = screen_height-tilemap.cached_height-value;
			}
			tilemap.cached_colscroll[which] = value;
		}
		else
		{
			if( (tilemap.orientation & ORIENTATION_FLIP_Y) != 0 )
			{
				/* adjust affected row */
				which = tilemap.cached_scroll_rows-1 - which;
			}
			if( (tilemap.orientation & ORIENTATION_FLIP_X) != 0 )
			{
				/* adjust scroll amount */
				value = screen_width-tilemap.cached_width-value;
			}
			tilemap.cached_rowscroll[which] = value;
		}
	}
	
/*TODO*///	void tilemap_set_scrolly( struct tilemap *tilemap, int which, int value )
/*TODO*///	{
/*TODO*///		tilemap->logical_colscroll[which] = value;
/*TODO*///		value = tilemap->scrolly_delta - value; /* adjust */
/*TODO*///	
/*TODO*///		if( tilemap->orientation & ORIENTATION_SWAP_XY )
/*TODO*///		{
/*TODO*///			/* if xy are swapped, we are actually panning the screen bitmap horizontally */
/*TODO*///			if( tilemap->orientation & ORIENTATION_FLIP_Y )
/*TODO*///			{
/*TODO*///				/* adjust affected row */
/*TODO*///				which = tilemap->cached_scroll_rows-1 - which;
/*TODO*///			}
/*TODO*///			if( tilemap->orientation & ORIENTATION_FLIP_X )
/*TODO*///			{
/*TODO*///				/* adjust scroll amount */
/*TODO*///				value = screen_width-tilemap->cached_width-value;
/*TODO*///			}
/*TODO*///			tilemap->cached_rowscroll[which] = value;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			if( tilemap->orientation & ORIENTATION_FLIP_X )
/*TODO*///			{
/*TODO*///				/* adjust affected col */
/*TODO*///				which = tilemap->cached_scroll_cols-1 - which;
/*TODO*///			}
/*TODO*///			if( tilemap->orientation & ORIENTATION_FLIP_Y )
/*TODO*///			{
/*TODO*///				/* adjust scroll amount */
/*TODO*///				value = screen_height-tilemap->cached_height-value;
/*TODO*///			}
/*TODO*///			tilemap->cached_colscroll[which] = value;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
	/***********************************************************************************/
	
	public static void tilemap_draw( mame_bitmap dest, tilemap tilemap, int flags, int priority )
	{
		int xpos,ypos,mask,value;
		int rows, cols;
		int[] rowscroll=new int[1], colscroll=new int[1];
		int left, right, top, bottom;
	
/*TODO*///	profiler_mark(PROFILER_TILEMAP_DRAW);
		if( tilemap.enable != 0)
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
                        tile_info = new _tile_info();
	
			/* priority_bitmap_pitch_row is tilemap-specific */
			priority_bitmap_pitch_row = priority_bitmap_pitch_line*tilemap.cached_tile_height;
	
			blit.screen_bitmap = dest;
			if( dest == null )
			{
				blit.draw_masked = (blitmask_t)pit;
				blit.draw_opaque = (blitopaque_t)pio;
			}
			else
			{
				blit.screen_bitmap_pitch_line = (dest.line[1].read()) - (dest.line[0].read());
                                //System.out.println(dest.depth);
				switch( dest.depth )
				{
				case 32:
					if( (flags&TILEMAP_ALPHA) != 0)
					{
						blit.draw_masked = (blitmask_t)pbt32;
						blit.draw_opaque = (blitopaque_t)pbo32;
					}
					else
					{
						blit.draw_masked = (blitmask_t)pdt32;
						blit.draw_opaque = (blitopaque_t)pdo32;
					}
					blit.screen_bitmap_pitch_line /= 4;
					break;
	
				case 15:
					if( (flags&TILEMAP_ALPHA) != 0)
					{
						blit.draw_masked = (blitmask_t)pbt15;
						blit.draw_opaque = (blitopaque_t)pbo15;
					}
					else
					{
						blit.draw_masked = (blitmask_t)pdt15;
						blit.draw_opaque = (blitopaque_t)pdo15;
					}
					blit.screen_bitmap_pitch_line /= 2;
					break;
	
				case 16:
					blit.draw_masked = (blitmask_t)pdt16;
					blit.draw_opaque = (blitopaque_t)pdo16;
					blit.screen_bitmap_pitch_line /= 2;
					break;
	
				default:
					exit(1);
					break;
				}
				blit.screen_bitmap_pitch_row = blit.screen_bitmap_pitch_line*tilemap.cached_tile_height;
			} /* dest == bitmap */
	
			if( !((tilemap.type==TILEMAP_OPAQUE) || ((flags&TILEMAP_IGNORE_TRANSPARENCY) != 0)) )
			{
				if( (flags&TILEMAP_BACK) != 0)
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
							tilemap.draw.handler( tilemap, scrollx, ypos, mask, value );
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
							tilemap.draw.handler( tilemap, scrollx - tilemap.cached_width, ypos, mask, value );
						}
					}
					col += cons;
				}
			}
			else if( cols == 1 )
			{ /* scrolling rows + vertical scroll */
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
							tilemap.draw.handler( tilemap, xpos, scrolly, mask, value );
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
							tilemap.draw.handler( tilemap, xpos, scrolly - tilemap.cached_height, mask, value );
						}
					}
					row += cons;
				}
			}
		}
/*TODO*///	profiler_mark(PROFILER_END);
	}
/*TODO*///	
/*TODO*///	/* notes:
/*TODO*///	   - startx and starty MUST be UINT32 for calculations to work correctly
/*TODO*///	   - srcbitmap->width and height are assumed to be a power of 2 to speed up wraparound
/*TODO*///	   */
/*TODO*///	void tilemap_draw_roz(struct mame_bitmap *dest,struct tilemap *tilemap,
/*TODO*///			UINT32 startx,UINT32 starty,int incxx,int incxy,int incyx,int incyy,
/*TODO*///			int wraparound,
/*TODO*///			UINT32 flags, UINT32 priority )
/*TODO*///	{
/*TODO*///		int mask,value;
/*TODO*///	
/*TODO*///	profiler_mark(PROFILER_TILEMAP_DRAW_ROZ);
/*TODO*///		if( tilemap->enable )
/*TODO*///		{
/*TODO*///			/* tile priority */
/*TODO*///			mask		= TILE_FLAG_TILE_PRIORITY;
/*TODO*///			value		= TILE_FLAG_TILE_PRIORITY&flags;
/*TODO*///	
/*TODO*///			tilemap_get_pixmap( tilemap ); /* force update */
/*TODO*///	
/*TODO*///			if( !(tilemap->type==TILEMAP_OPAQUE || (flags&TILEMAP_IGNORE_TRANSPARENCY)) )
/*TODO*///			{
/*TODO*///				if( flags&TILEMAP_BACK )
/*TODO*///				{
/*TODO*///					mask	|= TILE_FLAG_BG_OPAQUE;
/*TODO*///					value	|= TILE_FLAG_BG_OPAQUE;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					mask	|= TILE_FLAG_FG_OPAQUE;
/*TODO*///					value	|= TILE_FLAG_FG_OPAQUE;
/*TODO*///				}
/*TODO*///			}
/*TODO*///	
/*TODO*///			switch( dest->depth )
/*TODO*///			{
/*TODO*///	
/*TODO*///			case 32:
/*TODO*///				copyrozbitmap_core32BPP(dest,tilemap,startx,starty,incxx,incxy,incyx,incyy,
/*TODO*///					wraparound,&tilemap->logical_clip,mask,value,priority);
/*TODO*///				break;
/*TODO*///	
/*TODO*///			case 15:
/*TODO*///			case 16:
/*TODO*///				copyrozbitmap_core16BPP(dest,tilemap,startx,starty,incxx,incxy,incyx,incyy,
/*TODO*///					wraparound,&tilemap->logical_clip,mask,value,priority);
/*TODO*///				break;
/*TODO*///	
/*TODO*///			default:
/*TODO*///				exit(1);
/*TODO*///			}
/*TODO*///		} /* tilemap->enable */
/*TODO*///	profiler_mark(PROFILER_END);
/*TODO*///	}
/*TODO*///	
/*TODO*///	/***********************************************************************************/
/*TODO*///	
/*TODO*///	#endif // !DECLARE && !TRANSP
/*TODO*///	
/*TODO*///	#ifdef DECLARE
/*TODO*///	
/*TODO*///	DECLARE(copyrozbitmap_core,(struct mame_bitmap *bitmap,struct tilemap *tilemap,
/*TODO*///			UINT32 startx,UINT32 starty,int incxx,int incxy,int incyx,int incyy,int wraparound,
/*TODO*///			const struct rectangle *clip,
/*TODO*///			int mask,int value,
/*TODO*///			UINT32 priority),
/*TODO*///	{
/*TODO*///		UINT32 cx;
/*TODO*///		UINT32 cy;
/*TODO*///		int x;
/*TODO*///		int sx;
/*TODO*///		int sy;
/*TODO*///		int ex;
/*TODO*///		int ey;
/*TODO*///		struct mame_bitmap *srcbitmap = tilemap->pixmap;
/*TODO*///		struct mame_bitmap *transparency_bitmap = tilemap->transparency_bitmap;
/*TODO*///		const int xmask = srcbitmap->width-1;
/*TODO*///		const int ymask = srcbitmap->height-1;
/*TODO*///		const int widthshifted = srcbitmap->width << 16;
/*TODO*///		const int heightshifted = srcbitmap->height << 16;
/*TODO*///		DATA_TYPE *dest;
/*TODO*///		UINT8 *pri;
/*TODO*///		const UINT16 *src;
/*TODO*///		const UINT8 *pMask;
/*TODO*///	
/*TODO*///		if (clip)
/*TODO*///		{
/*TODO*///			startx += clip->min_x * incxx + clip->min_y * incyx;
/*TODO*///			starty += clip->min_x * incxy + clip->min_y * incyy;
/*TODO*///	
/*TODO*///			sx = clip->min_x;
/*TODO*///			sy = clip->min_y;
/*TODO*///			ex = clip->max_x;
/*TODO*///			ey = clip->max_y;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			sx = 0;
/*TODO*///			sy = 0;
/*TODO*///			ex = bitmap->width-1;
/*TODO*///			ey = bitmap->height-1;
/*TODO*///		}
/*TODO*///	
/*TODO*///	
/*TODO*///		if (Machine->orientation & ORIENTATION_SWAP_XY)
/*TODO*///		{
/*TODO*///			int t;
/*TODO*///	
/*TODO*///			t = startx; startx = starty; starty = t;
/*TODO*///			t = sx; sx = sy; sy = t;
/*TODO*///			t = ex; ex = ey; ey = t;
/*TODO*///			t = incxx; incxx = incyy; incyy = t;
/*TODO*///			t = incxy; incxy = incyx; incyx = t;
/*TODO*///		}
/*TODO*///	
/*TODO*///		if (Machine->orientation & ORIENTATION_FLIP_X)
/*TODO*///		{
/*TODO*///			int w = ex - sx;
/*TODO*///	
/*TODO*///			incxy = -incxy;
/*TODO*///			incyx = -incyx;
/*TODO*///			startx = widthshifted - startx - 1;
/*TODO*///			startx -= incxx * w;
/*TODO*///			starty -= incxy * w;
/*TODO*///	
/*TODO*///			w = sx;
/*TODO*///			sx = bitmap->width-1 - ex;
/*TODO*///			ex = bitmap->width-1 - w;
/*TODO*///		}
/*TODO*///	
/*TODO*///		if (Machine->orientation & ORIENTATION_FLIP_Y)
/*TODO*///		{
/*TODO*///			int h = ey - sy;
/*TODO*///	
/*TODO*///			incxy = -incxy;
/*TODO*///			incyx = -incyx;
/*TODO*///			starty = heightshifted - starty - 1;
/*TODO*///			startx -= incyx * h;
/*TODO*///			starty -= incyy * h;
/*TODO*///	
/*TODO*///			h = sy;
/*TODO*///			sy = bitmap->height-1 - ey;
/*TODO*///			ey = bitmap->height-1 - h;
/*TODO*///		}
/*TODO*///	
/*TODO*///		if (incxy == 0 && incyx == 0 && !wraparound)
/*TODO*///		{
/*TODO*///			/* optimized loop for the not rotated case */
/*TODO*///	
/*TODO*///			if (incxx == 0x10000)
/*TODO*///			{
/*TODO*///				/* optimized loop for the not zoomed case */
/*TODO*///	
/*TODO*///				/* startx is unsigned */
/*TODO*///				startx = ((INT32)startx) >> 16;
/*TODO*///	
/*TODO*///				if (startx >= srcbitmap->width)
/*TODO*///				{
/*TODO*///					sx += -startx;
/*TODO*///					startx = 0;
/*TODO*///				}
/*TODO*///	
/*TODO*///				if (sx <= ex)
/*TODO*///				{
/*TODO*///					while (sy <= ey)
/*TODO*///					{
/*TODO*///						if (starty < heightshifted)
/*TODO*///						{
/*TODO*///							x = sx;
/*TODO*///							cx = startx;
/*TODO*///							cy = starty >> 16;
/*TODO*///							dest = ((DATA_TYPE *)bitmap->line[sy]) + sx;
/*TODO*///	
/*TODO*///							pri = ((UINT8 *)priority_bitmap->line[sy]) + sx;
/*TODO*///							src = (UINT16 *)srcbitmap->line[cy];
/*TODO*///							pMask = (UINT8 *)transparency_bitmap->line[cy];
/*TODO*///	
/*TODO*///							while (x <= ex && cx < srcbitmap->width)
/*TODO*///							{
/*TODO*///								if ( (pMask[cx]&mask) == value )
/*TODO*///								{
/*TODO*///									*dest = src[cx];
/*TODO*///									*pri |= priority;
/*TODO*///								}
/*TODO*///								cx++;
/*TODO*///								x++;
/*TODO*///								dest++;
/*TODO*///								pri++;
/*TODO*///							}
/*TODO*///						}
/*TODO*///						starty += incyy;
/*TODO*///						sy++;
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				while (startx >= widthshifted && sx <= ex)
/*TODO*///				{
/*TODO*///					startx += incxx;
/*TODO*///					sx++;
/*TODO*///				}
/*TODO*///	
/*TODO*///				if (sx <= ex)
/*TODO*///				{
/*TODO*///					while (sy <= ey)
/*TODO*///					{
/*TODO*///						if (starty < heightshifted)
/*TODO*///						{
/*TODO*///							x = sx;
/*TODO*///							cx = startx;
/*TODO*///							cy = starty >> 16;
/*TODO*///							dest = ((DATA_TYPE *)bitmap->line[sy]) + sx;
/*TODO*///	
/*TODO*///							pri = ((UINT8 *)priority_bitmap->line[sy]) + sx;
/*TODO*///							src = (UINT16 *)srcbitmap->line[cy];
/*TODO*///							pMask = (UINT8 *)transparency_bitmap->line[cy];
/*TODO*///							while (x <= ex && cx < widthshifted)
/*TODO*///							{
/*TODO*///								if ( (pMask[cx>>16]&mask) == value )
/*TODO*///								{
/*TODO*///									*dest = src[cx >> 16];
/*TODO*///									*pri |= priority;
/*TODO*///								}
/*TODO*///								cx += incxx;
/*TODO*///								x++;
/*TODO*///								dest++;
/*TODO*///								pri++;
/*TODO*///							}
/*TODO*///						}
/*TODO*///						starty += incyy;
/*TODO*///						sy++;
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			if (wraparound)
/*TODO*///			{
/*TODO*///				/* plot with wraparound */
/*TODO*///				while (sy <= ey)
/*TODO*///				{
/*TODO*///					x = sx;
/*TODO*///					cx = startx;
/*TODO*///					cy = starty;
/*TODO*///					dest = ((DATA_TYPE *)bitmap->line[sy]) + sx;
/*TODO*///					pri = ((UINT8 *)priority_bitmap->line[sy]) + sx;
/*TODO*///					while (x <= ex)
/*TODO*///					{
/*TODO*///						if( (((UINT8 *)transparency_bitmap->line[(cy>>16)&ymask])[(cx>>16)&xmask]&mask) == value )
/*TODO*///						{
/*TODO*///							*dest = ((UINT16 *)srcbitmap->line[(cy >> 16) & ymask])[(cx >> 16) & xmask];
/*TODO*///							*pri |= priority;
/*TODO*///						}
/*TODO*///						cx += incxx;
/*TODO*///						cy += incxy;
/*TODO*///						x++;
/*TODO*///						dest++;
/*TODO*///						pri++;
/*TODO*///					}
/*TODO*///					startx += incyx;
/*TODO*///					starty += incyy;
/*TODO*///					sy++;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				while (sy <= ey)
/*TODO*///				{
/*TODO*///					x = sx;
/*TODO*///					cx = startx;
/*TODO*///					cy = starty;
/*TODO*///					dest = ((DATA_TYPE *)bitmap->line[sy]) + sx;
/*TODO*///					pri = ((UINT8 *)priority_bitmap->line[sy]) + sx;
/*TODO*///					while (x <= ex)
/*TODO*///					{
/*TODO*///						if (cx < widthshifted && cy < heightshifted)
/*TODO*///						{
/*TODO*///							if( (((UINT8 *)transparency_bitmap->line[cy>>16])[cx>>16]&mask)==value )
/*TODO*///							{
/*TODO*///								*dest = ((UINT16 *)srcbitmap->line[cy >> 16])[cx >> 16];
/*TODO*///								*pri |= priority;
/*TODO*///							}
/*TODO*///						}
/*TODO*///						cx += incxx;
/*TODO*///						cy += incxy;
/*TODO*///						x++;
/*TODO*///						dest++;
/*TODO*///						pri++;
/*TODO*///					}
/*TODO*///					startx += incyx;
/*TODO*///					starty += incyy;
/*TODO*///					sy++;
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	})
        
        public static _draw draw16BPP = new _draw() {
            public void handler(tilemap tilemap, int xpos, int ypos, int mask, int value) {
                DEPTH = 16;
                draw(tilemap, xpos, ypos, mask, value);
            }
        };
        
        public static _draw draw32BPP = new _draw() {
            public void handler(tilemap tilemap, int xpos, int ypos, int mask, int value) {
                DEPTH = 32;
                draw(tilemap, xpos, ypos, mask, value);
            }
        };
        
	public static void draw(tilemap tilemap, int xpos, int ypos, int mask, int value )
	{
		int transPrev;
		int transCur=0;
		UBytePtr pTrans;
		int cached_indx;
		mame_bitmap screen = blit.screen_bitmap;
		int tilemap_priority_code = blit.tilemap_priority_code;
		int x1 = xpos;
		int y1 = ypos;
		int x2 = xpos+tilemap.cached_width;
		int y2 = ypos+tilemap.cached_height;
		UBytePtr dest_baseaddr = null;
		UBytePtr dest_next;
		int dy;
		int count;
		UBytePtr source0;
		UBytePtr dest0;
		UBytePtr pmap0;
		int i;
		int row;
		int x_start;
		int x_end;
		int column;
		int c1; /* leftmost visible column in source tilemap */
		int c2; /* rightmost visible column in source tilemap */
		int y; /* current screen line to render */
		int y_next;
		UBytePtr priority_bitmap_baseaddr;
		UBytePtr priority_bitmap_next;
		UBytePtr source_baseaddr;
		UBytePtr source_next;
		UBytePtr mask0;
		UBytePtr mask_baseaddr;
		UBytePtr mask_next;
                
                if (priority_bitmap == null)
                        tilemap_init();
	
		/* clip source coordinates */
		if( x1<blit.clip_left ) x1 = blit.clip_left;
		if( x2>blit.clip_right ) x2 = blit.clip_right;
		if( y1<blit.clip_top ) y1 = blit.clip_top;
		if( y2>blit.clip_bottom ) y2 = blit.clip_bottom;
	
		if( x1<x2 && y1<y2 ) /* do nothing if totally clipped */
		{
			priority_bitmap_baseaddr = new UBytePtr( priority_bitmap.line[y1], xpos);
			if( screen != null )
			{
				dest_baseaddr = new UBytePtr(screen.line[y1], xpos);
			}
	
			/* convert screen coordinates to source tilemap coordinates */
			x1 -= xpos;
			y1 -= ypos;
			x2 -= xpos;
			y2 -= ypos;
	
			source_baseaddr = new UBytePtr( tilemap.pixmap.line[y1] );
			mask_baseaddr = tilemap.transparency_bitmap.line[y1];
	
			c1 = x1/tilemap.cached_tile_width; /* round down */
			c2 = (x2+tilemap.cached_tile_width-1)/tilemap.cached_tile_width; /* round up */
	
			y = y1;
			y_next = tilemap.cached_tile_height*(y1/tilemap.cached_tile_height) + tilemap.cached_tile_height;
			if( y_next>y2 ) y_next = y2;
	
			dy = y_next-y;
			dest_next = new UBytePtr( dest_baseaddr, dy*blit.screen_bitmap_pitch_line );
			priority_bitmap_next = new UBytePtr(priority_bitmap_baseaddr, dy*priority_bitmap_pitch_line);
			source_next = new UBytePtr( source_baseaddr, dy*tilemap.pixmap_pitch_line );
			mask_next = new UBytePtr( mask_baseaddr, dy*tilemap.transparency_bitmap_pitch_line );
                        
                        boolean L_Skip = false;
                        
			for(;;)
			{
				row = y/tilemap.cached_tile_height;
				x_start = x1;
	
				transPrev = eWHOLLY_TRANSPARENT;
				pTrans = new UBytePtr(mask_baseaddr, x_start);
	
				cached_indx = row*tilemap.num_cached_cols + c1;
				for( column=c1; column<=c2; column++ )
				{
					if( column == c2 )
					{
						transCur = eWHOLLY_TRANSPARENT;
						L_Skip = true;
					}
	
					if( (!L_Skip) & tilemap.transparency_data.read(cached_indx)==TILE_FLAG_DIRTY )
					{
						update_tile_info( tilemap, cached_indx, column, row );
					}
	
					if( (!L_Skip) & (tilemap.transparency_data.read(cached_indx)&mask)!=0 )
					{
						transCur = eMASKED;
					}
                                        else if (!L_Skip)
					{
						transCur = (((pTrans.read())&mask) == value)?eWHOLLY_OPAQUE:eWHOLLY_TRANSPARENT;
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
							source0 = new UBytePtr( source_baseaddr, x_start );
							dest0 = new UBytePtr(dest_baseaddr, x_start);
							pmap0 = new UBytePtr(priority_bitmap_baseaddr, x_start);
	
							if( transPrev == eWHOLLY_OPAQUE )
							{
								i = y;
								for(;;)
								{
									blit.draw_opaque.handler(dest0, source0, count, pmap0, tilemap_priority_code );
									if( ++i == y_next ) break;
	
									dest0.inc( blit.screen_bitmap_pitch_line );
									source0.inc( tilemap.pixmap_pitch_line );
									pmap0.inc( priority_bitmap_pitch_line );
								}
							} /* transPrev == eWHOLLY_OPAQUE */
							else /* transPrev == eMASKED */
							{
								mask0 = new UBytePtr(mask_baseaddr, x_start);
								i = y;
								for(;;)
								{
									blit.draw_masked.handler(dest0, source0, mask0, mask, value, count, pmap0, tilemap_priority_code );
									if( ++i == y_next ) break;
	
									dest0.inc(  blit.screen_bitmap_pitch_line );
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
	};
/*TODO*///	
/*TODO*///	#undef DATA_TYPE
/*TODO*///	#undef DEPTH
/*TODO*///	#undef DECLARE
/*TODO*///	#endif /* DECLARE */
/*TODO*///	
/*TODO*///	#ifdef TRANSP
	/*************************************************************************************************/
	
	/* Each of the following routines draws pixmap and transarency data for a single tile.
	 *
	 * This function returns a per-tile code.  Each bit of this code is 0 if the corresponding
	 * bit is zero in every byte of transparency data in the tile, or 1 if that bit is not
	 * consistant within the tile.
	 *
	 * This precomputer value allows us for any particular tile and mask, to determine if all pixels
	 * in that tile have the same masked transparency value.
	 */
        
        public static _draw_tile HandleTransparencyBitmask_ind = new _draw_tile() {
            public int handler(tilemap tilemap, int col, int row, int flags, boolean ind) {
                
                return HandleTransparencyBitmask.handler(tilemap, col, row, flags, true);
            }
        };
        
        public static _draw_tile HandleTransparencyBitmask_raw = new _draw_tile() {
            public int handler(tilemap tilemap, int col, int row, int flags, boolean ind) {
                
                return HandleTransparencyBitmask.handler(tilemap, col, row, flags, false);
            }
        };
	
        public static _draw_tile HandleTransparencyBitmask = new _draw_tile() {
                public int handler(tilemap tilemap, int x0, int y0, int flags, boolean ind) {
                    int tile_width = tilemap.cached_tile_width;
                    int tile_height = tilemap.cached_tile_height;
                    mame_bitmap pixmap = tilemap.pixmap;
                    mame_bitmap transparency_bitmap = tilemap.transparency_bitmap;
                    int pitch = tile_width + tile_info.skip;
                    if (ind){
                        PAL_INIT_ind(tile_info);
                    } else { //raw
                        /*TODO*///PAL_INIT_raw(tile_info);
                    }
                    UShortPtr pPenToPixel = tilemap.pPenToPixel[flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX)];
                    int _pPenToPixel = 0;
                    UBytePtr pPenData = tile_info.pen_data;
                    int _pPenData = 0;
                    UBytePtr pSource;
                    int _pSource = 0;
                    int code_transparent = tile_info.priority;
                    int code_opaque = code_transparent | TILE_FLAG_FG_OPAQUE;
                    int tx;
                    int ty;
                    int data;
                    int yx;
                    int x;
                    int y;
                    int pen;
                    UBytePtr pBitmask = tile_info.mask_data;
                    int bitoffs = 0;
                    int bWhollyOpaque;
                    int bWhollyTransparent;

                    bWhollyOpaque = 1;
                    bWhollyTransparent = 1;

                    if( (flags&TILE_4BPP) != 0)
                    {
                            for( ty=tile_height; ty!=0; ty-- )
                            {
                                    pSource = pPenData;
                                    for( tx=tile_width/2; tx!=0; tx-- )
                                    {
                                            data = pSource.read( _pSource++ );

                                            pen = data&0xf;
                                            yx = pPenToPixel.read(_pPenToPixel++);
                                            x = x0+(yx%MAX_TILESIZE);
                                            y = y0+(yx/MAX_TILESIZE);
                                            int valP = 0;
                                            
                                            if (ind) {
                                                valP = PAL_GET_ind(pen);
                                            }else {
                                                /*TODO*///valP = PAL_GET_raw(pen);
                                            }
                                            
                                            new UShortPtr(pixmap.line[y]).write(x, (char) valP);
                                            if( (pBitmask.read(bitoffs/8)&(0x80>>(bitoffs&7))) == 0 )
                                            {
                                                    (transparency_bitmap.line[y]).write(x, code_transparent);
                                                    bWhollyOpaque = 0;
                                            }
                                            else
                                            {
                                                    (transparency_bitmap.line[y]).write(x, code_opaque);
                                                    bWhollyTransparent = 0;
                                            }
                                            bitoffs++;

                                            pen = data>>4;
                                            yx = pPenToPixel.read(_pPenToPixel++);
                                            x = x0+(yx%MAX_TILESIZE);
                                            y = y0+(yx/MAX_TILESIZE);
                                            if (ind) {
                                                valP = PAL_GET_ind(pen);
                                            }else {
                                                /*TODO*///valP = PAL_GET_raw(pen);
                                            }
                                            
                                            new UShortPtr(pixmap.line[y]).write(x, (char) valP);
                                            
                                            if( (pBitmask.read(bitoffs/8)&(0x80>>(bitoffs&7))) == 0 )
                                            {
                                                    (transparency_bitmap.line[y]).write(x, code_transparent);
                                                    bWhollyOpaque = 0;
                                            }
                                            else
                                            {
                                                    (transparency_bitmap.line[y]).write(x, code_opaque);
                                                    bWhollyTransparent = 0;
                                            }
                                            bitoffs++;
                                    }
                                    _pPenData += pitch/2;
                                    pPenData.inc(_pPenData);
                                            
                            }
                    }
                    else
                    {
                            for( ty=tile_height; ty!=0; ty-- )
                            {
                                    pSource = pPenData;
                                    for( tx=tile_width; tx!=0; tx-- )
                                    {
                                            pen = pSource.read(_pSource++);
                                            yx = pPenToPixel.read(_pPenToPixel++);
                                            x = x0+(yx%MAX_TILESIZE);
                                            y = y0+(yx/MAX_TILESIZE);
                                            
                                            int valP = 0;
                                            if (ind) {
                                                valP = PAL_GET_ind(pen);
                                            }else {
                                                /*TODO*///valP = PAL_GET_raw(pen);
                                            }
                                            
                                            new UShortPtr(pixmap.line[y]).write(x, (char) valP);
                                            if( (pBitmask.read(bitoffs/8)&(0x80>>(bitoffs&7))) == 0 )
                                            {
                                                    (transparency_bitmap.line[y]).write(x, code_transparent);
                                                    bWhollyOpaque = 0;
                                            }
                                            else
                                            {
                                                    (transparency_bitmap.line[y]).write(x, code_opaque);
                                                    bWhollyTransparent = 0;
                                            }
                                            bitoffs++;
                                    }
                                    _pPenData += pitch;
                                    pPenData.inc(_pPenData);
                            }
                    }
                    return ((bWhollyOpaque!=0) || (bWhollyTransparent!=0))?0:TILE_FLAG_FG_OPAQUE;
                }
            };


        public static _draw_tile HandleTransparencyColor_ind = new _draw_tile() {
            public int handler(tilemap tilemap, int col, int row, int flags, boolean ind) {
                
                return HandleTransparencyColor.handler(tilemap, col, row, flags, true);
            }
        };
        
        public static _draw_tile HandleTransparencyColor_raw = new _draw_tile() {
            public int handler(tilemap tilemap, int col, int row, int flags, boolean ind) {
                
                return HandleTransparencyColor.handler(tilemap, col, row, flags, false);
            }
        };
	
        public static _draw_tile HandleTransparencyColor = new _draw_tile() {
            public int handler(tilemap tilemap, int x0, int y0, int flags, boolean ind) {
                int tile_width = tilemap.cached_tile_width;
		int tile_height = tilemap.cached_tile_height;
                
		mame_bitmap pixmap = tilemap.pixmap;
		mame_bitmap transparency_bitmap = tilemap.transparency_bitmap;
		int pitch = tile_width + tile_info.skip;
		if (ind){
                    PAL_INIT_ind(tile_info);
                    //System.out.println("IND");
                } else {
                    /*TODO*///PAL_INIT_raw(tile_info);
                }
		UShortPtr pPenToPixel = tilemap.pPenToPixel[flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX)];
                int _pPenToPixel = 0;
		UBytePtr pPenData = tile_info.pen_data;
                int _pPenData = 0;
		UBytePtr pSource=new UBytePtr(1024*128);
                int _pSource = 0;
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
	
		if( (flags&TILE_4BPP) != 0 )
		{
			for( ty=tile_height; ty!=0; ty-- )
			{
				pSource = pPenData;
				for( tx=tile_width/2; tx!=0; tx-- )
				{
					data = pSource.read(_pSource++);
	
					pen = data&0xf;
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
                                        int pVal = 0;
                                        if (ind){
                                            pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal = PAL_GET_raw(pen);
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					if( pVal==transparent_color )
					{
						(transparency_bitmap.line[y]).write(x, code_transparent);
						bWhollyOpaque = 0;
					}
					else
					{
						(transparency_bitmap.line[y]).write(x, code_opaque);
						bWhollyTransparent = 0;
					}
	
					pen = data>>4;
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
					if (ind){
                                            pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal = PAL_GET_raw(pen);
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					if( pVal==transparent_color )
					{
						(transparency_bitmap.line[y]).write(x, code_transparent);
						bWhollyOpaque = 0;
					}
					else
					{
						(transparency_bitmap.line[y]).write(x, code_opaque);
						bWhollyTransparent = 0;
					}
				}
				_pPenData += pitch/2;
                                pPenData.inc(_pPenData);
			}
		}
		else
		{
			for( ty=tile_height; ty!=0; ty-- )
			{
				pSource = new UBytePtr(pPenData);
                                pSource.offset=0;
                                
				for( tx=tile_width; tx!=0; tx-- )
				{
					pen = pSource.read(_pSource++);
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
                                        int pVal = 0;
					if (ind){
                                            pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal = PAL_GET_raw(pen);
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					if( pVal==transparent_color )
					{
						(transparency_bitmap.line[y]).write(x, code_transparent);
						bWhollyOpaque = 0;
					}
					else
					{
						(transparency_bitmap.line[y]).write(x, code_opaque);
						bWhollyTransparent = 0;
					}
				}
				_pPenData += pitch;
                                pPenData.inc(_pPenData);
			}
		}
		return ((bWhollyOpaque!=0) || (bWhollyTransparent!=0))?0:TILE_FLAG_FG_OPAQUE;
            }
        };

        
        public static _draw_tile HandleTransparencyPen_ind = new _draw_tile() {
            public int handler(tilemap tilemap, int col, int row, int flags, boolean ind) {
                
                return HandleTransparencyPen.handler(tilemap, col, row, flags, true);
            }
        };
        
        public static _draw_tile HandleTransparencyPen_raw = new _draw_tile() {
            public int handler(tilemap tilemap, int col, int row, int flags, boolean ind) {
                
                return HandleTransparencyPen.handler(tilemap, col, row, flags, false);
            }
        };
        
        public static _draw_tile HandleTransparencyPen = new _draw_tile() {
            public int handler(tilemap tilemap, int x0, int y0, int flags, boolean ind) {
                int tile_width = tilemap.cached_tile_width;
		int tile_height = tilemap.cached_tile_height;
		mame_bitmap pixmap = tilemap.pixmap;
		mame_bitmap transparency_bitmap = tilemap.transparency_bitmap;
		int pitch = tile_width + tile_info.skip;
		
                if (ind){
                    PAL_INIT_ind(tile_info);
                } else {
                    /*TODO*///PAL_INIT_raw(tile_info);
                }
                
		UShortPtr pPenToPixel = tilemap.pPenToPixel[flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX)];
                int _pPenToPixel = 0;
		UBytePtr pPenData = tile_info.pen_data;
                int _pPenData = 0;
		UBytePtr pSource;
                int _pSource = 0;
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
	
		if( (flags&TILE_IGNORE_TRANSPARENCY)!=0 )
		{
			transparent_pen = ~0;
		}
	
		if( (flags&TILE_4BPP)!=0 )
		{
			for( ty=tile_height; ty!=0; ty-- )
			{
				pSource = pPenData;
				for( tx=tile_width/2; tx!=0; tx-- )
				{
					data = pSource.read(_pSource++);
	
					pen = data&0xf;
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
                                        int pVal = 0;
                                        if (ind){
                                            pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal = PAL_GET_raw(pen);
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					if( pen==transparent_pen )
					{
						(transparency_bitmap.line[y]).write(x, code_transparent);
						bWhollyOpaque = 0;
					}
					else
					{
						(transparency_bitmap.line[y]).write(x, code_opaque);
						bWhollyTransparent = 0;
					}
	
					pen = data>>4;
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
					if (ind){
                                            pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal = PAL_GET_raw(pen);
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					(transparency_bitmap.line[y]).write(x, (pen==transparent_pen)?code_transparent:code_opaque);
				}
				_pPenData += pitch/2;
                                pPenData.inc(_pPenData);
			}
		}
		else
		{
			for( ty=tile_height; ty!=0; ty-- )
			{
				pSource = pPenData;
				for( tx=tile_width; tx!=0; tx-- )
				{
					pen = pSource.read(_pSource++);
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
                                        int pVal = 0;
					if (ind){
                                            pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal = PAL_GET_raw(pen);
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					if( pen==transparent_pen )
					{
						(transparency_bitmap.line[y]).write(x, code_transparent);
						bWhollyOpaque = 0;
	
					}
					else
					{
						(transparency_bitmap.line[y]).write(x, code_opaque);
						bWhollyTransparent = 0;
					}
				}
				_pPenData += pitch;
                                pPenData.inc(_pPenData);
			}
		}
	
		return ((bWhollyOpaque!=0) || (bWhollyTransparent!=0))?0:TILE_FLAG_FG_OPAQUE;
            }
        };
	
	public static _draw_tile HandleTransparencyPenBit_ind = new _draw_tile() {
            public int handler(tilemap tilemap, int x0, int y0, int flags, boolean ind) {
                return HandleTransparencyPenBit.handler(tilemap, x0, y0, flags, true);
            }
        };
        
        public static _draw_tile HandleTransparencyPenBit_raw = new _draw_tile() {
            public int handler(tilemap tilemap, int x0, int y0, int flags, boolean ind) {
                return HandleTransparencyPenBit.handler(tilemap, x0, y0, flags, false);
            }
        };
        
        public static _draw_tile HandleTransparencyPenBit = new _draw_tile() {
            public int handler(tilemap tilemap, int x0, int y0, int flags, boolean ind) {
                int tile_width = tilemap.cached_tile_width;
		int tile_height = tilemap.cached_tile_height;
		mame_bitmap pixmap = tilemap.pixmap;
		mame_bitmap transparency_bitmap = tilemap.transparency_bitmap;
		int pitch = tile_width + tile_info.skip;
		if (ind){
                    PAL_INIT_ind(tile_info);
                } else {
                    /*TODO*///PAL_INIT_raw(tile_info);
                }
		UShortPtr pPenToPixel = tilemap.pPenToPixel[flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX)];
                int _pPenToPixel = 0;
		UBytePtr pPenData = tile_info.pen_data;
                int _pPenData = 0;
		UBytePtr pSource;
                int _pSource = 0;
		int tx;
		int ty;
		int data;
		int yx;
		int x;
		int y;
		int pen;
		int penbit = tilemap.transparent_pen;
		int code_front = tile_info.priority | TILE_FLAG_FG_OPAQUE;
		int code_back = tile_info.priority | TILE_FLAG_BG_OPAQUE;
		int code;
		int and_flags = ~0;
		int or_flags = 0;
	
		if( (flags&TILE_4BPP)!=0 )
		{
			for( ty=tile_height; ty!=0; ty-- )
			{
				pSource = pPenData;
				for( tx=tile_width/2; tx!=0; tx-- )
				{
					data = pSource.read(_pSource++);
	
					pen = data&0xf;
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
                                        int pVal = 0;
                                        if (ind){
                                          pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal =PAL_GET_raw(pen);                             
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					code = ((pen&penbit)==penbit)?code_front:code_back;
					and_flags &= code;
					or_flags |= code;
					(transparency_bitmap.line[y]).write(x, code);
	
					pen = data>>4;
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
					if (ind){
                                          pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal =PAL_GET_raw(pen);                             
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					code = ((pen&penbit)==penbit)?code_front:code_back;
					and_flags &= code;
					or_flags |= code;
					(transparency_bitmap.line[y]).write(x, code);
				}
				_pPenData += pitch/2;
                                pPenData.inc(_pPenData);
			}
		}
		else
		{
			for( ty=tile_height; ty!=0; ty-- )
			{
				pSource = pPenData;
				for( tx=tile_width; tx!=0; tx-- )
				{
					pen = pSource.read(_pSource++);
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
                                        int pVal = 0;
					if (ind){
                                          pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal =PAL_GET_raw(pen);                             
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					code = ((pen&penbit)==penbit)?code_front:code_back;
					and_flags &= code;
					or_flags |= code;
					(transparency_bitmap.line[y]).write(x, code);
				}
				_pPenData += pitch;
                                pPenData.inc(_pPenData);
			}
		}
		return or_flags ^ and_flags;
            }
        };
        
	
        public static _draw_tile HandleTransparencyPens_ind = new _draw_tile() {
            public int handler(tilemap tilemap, int x0, int y0, int flags, boolean ind) {
                return HandleTransparencyPens.handler(tilemap, x0, y0, flags, true);
            }
        };
        
        public static _draw_tile HandleTransparencyPens_raw = new _draw_tile() {
            public int handler(tilemap tilemap, int x0, int y0, int flags, boolean ind) {
                return HandleTransparencyPens.handler(tilemap, x0, y0, flags, false);
            }
        };
        
        public static _draw_tile HandleTransparencyPens = new _draw_tile() {
            public int handler(tilemap tilemap, int x0, int y0, int flags, boolean ind) {
                int tile_width = tilemap.cached_tile_width;
		int tile_height = tilemap.cached_tile_height;
		mame_bitmap pixmap = tilemap.pixmap;
		mame_bitmap transparency_bitmap = tilemap.transparency_bitmap;
		int pitch = tile_width + tile_info.skip;
		
                if (ind){
                    PAL_INIT_ind(tile_info);
                } else {
                    /*TODO*///PAL_INIT_raw(tile_info);
                }
                
		UShortPtr pPenToPixel = tilemap.pPenToPixel[flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX)];
                int _pPenToPixel = 0;
		UBytePtr pPenData = tile_info.pen_data;
                int _pPenData = 0;
		UBytePtr pSource;
                int _pSource = 0;
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
	
		if(( flags&TILE_4BPP )!=0)
		{
			for( ty=tile_height; ty!=0; ty-- )
			{
				pSource = pPenData;
				for( tx=tile_width/2; tx!=0; tx-- )
				{
					data = pSource.read(_pSource++);
	
					pen = data&0xf;
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
                                        int pVal = 0;
                                        if (ind){
                                            pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal = PAL_GET_raw(pen);
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					code = code_transparent;
					if( ((1<<pen)&fgmask)==0 ) code |= TILE_FLAG_FG_OPAQUE;
					if( ((1<<pen)&bgmask)==0 ) code |= TILE_FLAG_BG_OPAQUE;
					and_flags &= code;
					or_flags |= code;
					(transparency_bitmap.line[y]).write(x, code);
	
					pen = data>>4;
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
					if (ind){
                                            pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal = PAL_GET_raw(pen);
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					code = code_transparent;
					if( ((1<<pen)&fgmask)==0 ) code |= TILE_FLAG_FG_OPAQUE;
					if( ((1<<pen)&bgmask)==0 ) code |= TILE_FLAG_BG_OPAQUE;
					and_flags &= code;
					or_flags |= code;
					(transparency_bitmap.line[y]).write(x, code);
				}
				_pPenData += pitch/2;
                                pPenData.inc(_pPenData);
			}
		}
		else
		{
			for( ty=tile_height; ty!=0; ty-- )
			{
				pSource = pPenData;
				for( tx=tile_width; tx!=0; tx-- )
				{
					pen = pSource.read(_pSource++);
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
                                        int pVal = 0;
					if (ind){
                                            pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal = PAL_GET_raw(pen);
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					code = code_transparent;
					if( ((1<<pen)&fgmask)==0 ) code |= TILE_FLAG_FG_OPAQUE;
					if( ((1<<pen)&bgmask)==0 ) code |= TILE_FLAG_BG_OPAQUE;
					and_flags &= code;
					or_flags |= code;
					(transparency_bitmap.line[y]).write(x, code);
				}
				_pPenData += pitch;
                                pPenData.inc(_pPenData);
			}
		}
		return and_flags ^ or_flags;
            }
        };
        
        public static _draw_tile HandleTransparencyNone_ind = new _draw_tile() {
            public int handler(tilemap tilemap, int col, int row, int flags, boolean ind) {
                
                return HandleTransparencyNone.handler(tilemap, col, row, flags, true);
            }
        };
        
        public static _draw_tile HandleTransparencyNone_raw = new _draw_tile() {
            public int handler(tilemap tilemap, int col, int row, int flags, boolean ind) {
                
                return HandleTransparencyNone.handler(tilemap, col, row, flags, false);
            }
        };
	
        public static _draw_tile HandleTransparencyNone = new _draw_tile() {
            public int handler(tilemap tilemap, int x0, int y0, int flags, boolean ind) {
		int tile_width = tilemap.cached_tile_width;
		int tile_height = tilemap.cached_tile_height;
		mame_bitmap pixmap = tilemap.pixmap;
		mame_bitmap transparency_bitmap = tilemap.transparency_bitmap;
		int pitch = tile_width + tile_info.skip;
		if (ind){
                    PAL_INIT_ind(tile_info);
                } else {
                    /*TODO*///PAL_INIT_raw(tile_info);
                }
		UShortPtr pPenToPixel = tilemap.pPenToPixel[flags&(TILE_SWAPXY|TILE_FLIPY|TILE_FLIPX)];
                int _pPenToPixel = 0;
		UBytePtr pPenData = tile_info.pen_data;
                int _pPenData = 0;
		UBytePtr pSource;
                int _pSource = 0;
		int code_opaque = tile_info.priority;
		int tx;
		int ty;
		int data;
		int yx;
		int x;
		int y;
		int pen;
	
		if( (flags&TILE_4BPP) != 0 )
		{
			for( ty=tile_height; ty!=0; ty-- )
			{
				pSource = pPenData;
				for( tx=tile_width/2; tx!=0; tx-- )
				{
					data = pSource.read(_pSource++);
	
					pen = data&0xf;
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
                                        int pVal = 0;
                                        if (ind){
                                            pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal = PAL_GET_raw(pen);
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					(transparency_bitmap.line[y]).write(x, code_opaque);
	
					pen = data>>4;
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
					if (ind){
                                            pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal = PAL_GET_raw(pen);
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					(transparency_bitmap.line[y]).write(x, code_opaque);
				}
				_pPenData += pitch/2;
                                pPenData.inc(_pPenData);
			}
		}
		else
		{
			for( ty=tile_height; ty!=0; ty-- )
			{
				pSource = pPenData;
				for( tx=tile_width; tx!=0; tx-- )
				{
					pen = pSource.read(_pSource++);
					yx = pPenToPixel.read(_pPenToPixel++);
					x = x0+(yx%MAX_TILESIZE);
					y = y0+(yx/MAX_TILESIZE);
                                        int pVal = 0;
					if (ind){
                                            pVal = PAL_GET_ind(pen);
                                        } else {
                                            /*TODO*///pVal = PAL_GET_raw(pen);
                                        }
					new UShortPtr(pixmap.line[y]).write(x, (char) pVal);
					(transparency_bitmap.line[y]).write(x, code_opaque);
				}
				_pPenData += pitch;
                                pPenData.inc(_pPenData);
			}
		}
		return 0;
            }
        };
/*TODO*///	static UINT8 TRANSP(HandleTransparencyNone)(struct tilemap *tilemap, UINT32 x0, UINT32 y0, UINT32 flags)
/*TODO*///	{

/*TODO*///	}
/*TODO*///	
/*TODO*///	#undef TRANSP
/*TODO*///	#undef PAL_INIT
/*TODO*///	#undef PAL_GET
/*TODO*///	#endif // TRANSP
/*TODO*///	
}
