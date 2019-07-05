/*********************************************************************

  artwork.c

  Generic backdrop/overlay functions.

  Created by Mike Balfour - 10/01/1998

  Added some overlay and backdrop functions
  for vector games. Mathis Rosenhauer - 10/09/1998

  MAB - 09 MAR 1999 - made some changes to artwork_create
  MLR - 29 MAR 1999 - added disks to artwork_create
  MLR - 24 MAR 2000 - support for true color artwork
  MLR - 17 JUL 2001 - removed 8bpp code

*********************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056;

import static common.ptr.*;
import static mame056.artworkH.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;
import static mame056.drawgfx.*;
import static mame056.driverH.*;
import static mame056.mame.*;
import static mame056.palette.*;
// refactor
import static arcadeflex036.osdepend.logerror;
import static common.subArrays.*;
import static mame056.osdependH.*;
import static mame056.pngH.*;
import static mame056.png.*;
import static arcadeflex056.fileio.*;

public class artwork
{
	
/*TODO*///	#define LIMIT5(x) ((x < 0x1f)? x : 0x1f)
/*TODO*///	#define LIMIT8(x) ((x < 0xff)? x : 0xff)
/*TODO*///	
/*TODO*///	/* the backdrop instance */
/*TODO*///	struct artwork_info *artwork_backdrop = NULL;
/*TODO*///	
/*TODO*///	/* the overlay instance */
/*TODO*///	struct artwork_info *artwork_overlay = NULL;
/*TODO*///	
/*TODO*///	struct mame_bitmap *artwork_real_scrbitmap;
/*TODO*///	
/*TODO*///	void artwork_free(struct artwork_info **a);
/*TODO*///	
/*TODO*///	static int my_stricmp( const char *dst, const char *src)
/*TODO*///	{
/*TODO*///		while (*src && *dst)
/*TODO*///		{
/*TODO*///			if( tolower(*src) != tolower(*dst) ) return *dst - *src;
/*TODO*///			src++;
/*TODO*///			dst++;
/*TODO*///		}
/*TODO*///		return *dst - *src;
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void RGBtoHSV( float r, float g, float b, float *h, float *s, float *v )
/*TODO*///	{
/*TODO*///		float min, max, delta;
/*TODO*///	
/*TODO*///		min = MIN( r, MIN( g, b ));
/*TODO*///		max = MAX( r, MAX( g, b ));
/*TODO*///		*v = max;
/*TODO*///	
/*TODO*///		delta = max - min;
/*TODO*///	
/*TODO*///		if( delta > 0  )
/*TODO*///			*s = delta / max;
/*TODO*///		else {
/*TODO*///			*s = 0;
/*TODO*///			*h = 0;
/*TODO*///			return;
/*TODO*///		}
/*TODO*///	
/*TODO*///		if( r == max )
/*TODO*///			*h = ( g - b ) / delta;
/*TODO*///		else if( g == max )
/*TODO*///			*h = 2 + ( b - r ) / delta;
/*TODO*///		else
/*TODO*///			*h = 4 + ( r - g ) / delta;
/*TODO*///	
/*TODO*///		*h *= 60;
/*TODO*///		if( *h < 0 )
/*TODO*///			*h += 360;
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void HSVtoRGB( float *r, float *g, float *b, float h, float s, float v )
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///		float f, p, q, t;
/*TODO*///	
/*TODO*///		if( s == 0 ) {
/*TODO*///			*r = *g = *b = v;
/*TODO*///			return;
/*TODO*///		}
/*TODO*///	
/*TODO*///		h /= 60;
/*TODO*///		i = h;
/*TODO*///		f = h - i;
/*TODO*///		p = v * ( 1 - s );
/*TODO*///		q = v * ( 1 - s * f );
/*TODO*///		t = v * ( 1 - s * ( 1 - f ) );
/*TODO*///	
/*TODO*///		switch( i ) {
/*TODO*///		case 0: *r = v; *g = t; *b = p; break;
/*TODO*///		case 1: *r = q; *g = v; *b = p; break;
/*TODO*///		case 2: *r = p; *g = v; *b = t; break;
/*TODO*///		case 3: *r = p; *g = q; *b = v; break;
/*TODO*///		case 4: *r = t; *g = p; *b = v; break;
/*TODO*///		default: *r = v; *g = p; *b = q; break;
/*TODO*///		}
/*TODO*///	
/*TODO*///	}
/*TODO*///	
/*TODO*///	static void merge_cmy(struct artwork_info *a, struct mame_bitmap *source, struct mame_bitmap *source_alpha,int sx, int sy)
/*TODO*///	{
/*TODO*///		int c1, c2, m1, m2, y1, y2, pen1, pen2, max, alpha;
/*TODO*///		int x, y, w, h;
/*TODO*///		struct mame_bitmap *dest, *dest_alpha;
/*TODO*///	
/*TODO*///		dest = a.orig_artwork;
/*TODO*///		dest_alpha = a.alpha;
/*TODO*///	
/*TODO*///		if (Machine.orientation & ORIENTATION_SWAP_XY)
/*TODO*///		{
/*TODO*///			w = source.height;
/*TODO*///			h = source.width;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			h = source.height;
/*TODO*///			w = source.width;
/*TODO*///		}
/*TODO*///	
/*TODO*///		for (y = 0; y < h; y++)
/*TODO*///			for (x = 0; x < w; x++)
/*TODO*///			{
/*TODO*///				pen1 = read_pixel(dest, sx + x, sy + y);
/*TODO*///	
/*TODO*///				c1 = 0x1f - (pen1 >> 10);
/*TODO*///				m1 = 0x1f - ((pen1 >> 5) & 0x1f);
/*TODO*///				y1 = 0x1f - (pen1 & 0x1f);
/*TODO*///	
/*TODO*///				pen2 = read_pixel(source, x, y);
/*TODO*///				c2 = 0x1f - (pen2 >> 10) + c1;
/*TODO*///				m2 = 0x1f - ((pen2 >> 5) & 0x1f) + m1;
/*TODO*///				y2 = 0x1f - (pen2 & 0x1f) + y1;
/*TODO*///	
/*TODO*///				max = MAX(c2, MAX(m2, y2));
/*TODO*///				if (max > 0x1f)
/*TODO*///				{
/*TODO*///					c2 = (c2 * 0x1f) / max;
/*TODO*///					m2 = (m2 * 0x1f) / max;
/*TODO*///					y2 = (y2 * 0x1f) / max;
/*TODO*///				}
/*TODO*///	
/*TODO*///				alpha = MIN (0xff, read_pixel(source_alpha, x, y)
/*TODO*///							 + read_pixel(dest_alpha, sx + x, sy + y));
/*TODO*///				plot_pixel(dest, sx + x, sy + y,
/*TODO*///						   ((0x1f - c2) << 10)
/*TODO*///						   | ((0x1f - m2) << 5)
/*TODO*///						   | (0x1f - y2));
/*TODO*///	
/*TODO*///				plot_pixel(dest_alpha, sx + x, sy + y, alpha);
/*TODO*///			}
/*TODO*///	}
	
	/*********************************************************************
	  allocate_artwork_mem
	
	  Allocates memory for all the bitmaps.
	 *********************************************************************/
	static void allocate_artwork_mem (int width, int height, artwork_info a)
	{
		if ((Machine.orientation & ORIENTATION_SWAP_XY) != 0)
		{
			int temp;
	
			temp = height;
			height = width;
			width = temp;
		}
	
		a = new artwork_info();
		if (a == null)
		{
			logerror("Not enough memory for artwork!\n");
			return;
		}
	
		if (((a).orig_artwork = bitmap_alloc(width, height)) == null)
		{
			logerror("Not enough memory for artwork!\n");
			artwork_free(a);
			return;
		}
		fillbitmap((a).orig_artwork,0,null);
	
		if (((a).alpha = bitmap_alloc(width, height)) == null)
		{
			logerror("Not enough memory for artwork!\n");
			artwork_free(a);
			return;
		}
		fillbitmap((a).alpha,0,null);
	
		if (((a).artwork = bitmap_alloc(width,height)) == null)
		{
			logerror("Not enough memory for artwork!\n");
			artwork_free(a);
			return;
		}
	
		if (((a).artwork1 = bitmap_alloc(width,height)) == null)
		{
			logerror("Not enough memory for artwork!\n");
			artwork_free(a);
			return;
		}
	
		if (((a).rgb = new IntArray(width*height))==null)
		{
			logerror("Not enough memory.\n");
			artwork_free(a);
			return;
		}
	}
	
/*TODO*///	/*********************************************************************
/*TODO*///	  create_disk
/*TODO*///	
/*TODO*///	  Creates a disk with radius r in the color of pen. A new bitmap
/*TODO*///	  is allocated for the disk.
/*TODO*///	
/*TODO*///	*********************************************************************/
/*TODO*///	static struct mame_bitmap *create_disk (int r, int fg, int bg)
/*TODO*///	{
/*TODO*///		struct mame_bitmap *disk;
/*TODO*///	
/*TODO*///		int x = 0, twox = 0;
/*TODO*///		int y = r;
/*TODO*///		int twoy = r+r;
/*TODO*///		int p = 1 - r;
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		if ((disk = bitmap_alloc(twoy, twoy)) == 0)
/*TODO*///		{
/*TODO*///			logerror("Not enough memory for artwork!\n");
/*TODO*///			return NULL;
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* background */
/*TODO*///		fillbitmap (disk, bg, 0);
/*TODO*///	
/*TODO*///		while (x < y)
/*TODO*///		{
/*TODO*///			x++;
/*TODO*///			twox +=2;
/*TODO*///			if (p < 0)
/*TODO*///				p += twox + 1;
/*TODO*///			else
/*TODO*///			{
/*TODO*///				y--;
/*TODO*///				twoy -= 2;
/*TODO*///				p += twox - twoy + 1;
/*TODO*///			}
/*TODO*///	
/*TODO*///			for (i = 0; i < twox; i++)
/*TODO*///			{
/*TODO*///				plot_pixel(disk, r-x+i, r-y	 , fg);
/*TODO*///				plot_pixel(disk, r-x+i, r+y-1, fg);
/*TODO*///			}
/*TODO*///	
/*TODO*///			for (i = 0; i < twoy; i++)
/*TODO*///			{
/*TODO*///				plot_pixel(disk, r-y+i, r-x	 , fg);
/*TODO*///				plot_pixel(disk, r-y+i, r+x-1, fg);
/*TODO*///			}
/*TODO*///		}
/*TODO*///		return disk;
/*TODO*///	}
	
	/*********************************************************************
	  artwork_remap
	
	  Creates the final artwork by adding the start_pen to the
	  original artwork.
	 *********************************************************************/
	static void artwork_remap(artwork_info a)
	{
		int x, y;
		if (Machine.color_depth == 16)
		{
			for ( y = 0; y < a.orig_artwork.height; y++)
				for (x = 0; x < a.orig_artwork.width; x++)
					new UShortPtr(a.artwork.line[y]).write(x, (char) (new UShortPtr(a.orig_artwork.line[y]).read(x)+a.start_pen));
		}
		else
			copybitmap(a.artwork, a.orig_artwork ,0,0,0,0,null,TRANSPARENCY_NONE,0);
	}
	
	/*********************************************************************
	  init_palette
	
	  This sets the palette colors used by the backdrop. It is a simple
	  rgb555 palette of 32768 entries.
	 *********************************************************************/
	static void init_palette(int start_pen)
	{
		int r, g, b;
	
		for (r = 0; r < 32; r++)
			for (g = 0; g < 32; g++)
				for (b = 0; b < 32; b++)
					palette_set_color(start_pen++, (r << 3) | (r >> 2), (g << 3) | (g >> 2), (b << 3) | (b >> 2));
	}
	
	/*********************************************************************
	
	  Reads a PNG for a artwork struct and converts it into a format
	  usable for mame.
	 *********************************************************************/
	static int decode_png(String file_name, mame_bitmap bitmap, mame_bitmap alpha, png_info p)
	{
            System.out.println("decode_png");
		UBytePtr tmp = new UBytePtr();
		int x, y, pen;
		Object fp;
		int file_name_len, depth;
		String file_name2="";
	
		depth = Machine.color_depth;
	
		/* check for .png */
		file_name2 = file_name;
		file_name_len = file_name2.length();
                System.out.println(file_name2);
/*TODO*///		if ((file_name_len < 4) || my_stricmp(&file_name2[file_name_len - 4], ".png"))
/*TODO*///		{
/*TODO*///			strcat(file_name2, ".png");
/*TODO*///		}
	
		if ((fp = osd_fopen(Machine.gamedrv.name, file_name2, OSD_FILETYPE_ARTWORK, 0)) == null)
		{
			logerror("Unable to open PNG %s\n", file_name);
			return 0;
		}
	
/*TODO*///		if (!png_read_file(fp, p))
/*TODO*///		{
/*TODO*///			osd_fclose (fp);
/*TODO*///			return 0;
/*TODO*///		}
		osd_fclose (fp);
	
		if (p.bit_depth > 8)
		{
			logerror("Unsupported bit depth %i (8 bit max.)\n", p.bit_depth);
			return 0;
		}
	
		if (p.interlace_method != 0)
		{
			logerror("Interlace unsupported\n");
			return 0;
		}
	
		switch (p.color_type)
		{
/*TODO*///		case 3:
/*TODO*///			/* Convert to 8 bit */
/*TODO*///			png_expand_buffer_8bit (p);
/*TODO*///	
/*TODO*///			png_delete_unused_colors (p);
/*TODO*///	
/*TODO*///			if ((*bitmap = bitmap_alloc(p.width,p.height)) == 0)
/*TODO*///			{
/*TODO*///				logerror("Unable to allocate memory for artwork\n");
/*TODO*///				return 0;
/*TODO*///			}
/*TODO*///	
/*TODO*///			tmp = p.image;
/*TODO*///			/* convert to 15/32 bit */
/*TODO*///			if (p.num_trans > 0)
/*TODO*///				if ((*alpha = bitmap_alloc(p.width,p.height)) == 0)
/*TODO*///				{
/*TODO*///					logerror("Unable to allocate memory for artwork\n");
/*TODO*///					return 0;
/*TODO*///				}
/*TODO*///	
/*TODO*///			for (y=0; y<p.height; y++)
/*TODO*///				for (x=0; x<p.width; x++)
/*TODO*///				{
/*TODO*///					if (depth == 32)
/*TODO*///						pen = (p.palette[*tmp * 3] << 16) | (p.palette[*tmp * 3 + 1] << 8) | p.palette[*tmp * 3 + 2];
/*TODO*///					else
/*TODO*///						pen = ((p.palette[*tmp * 3] & 0xf8) << 7) | ((p.palette[*tmp * 3 + 1] & 0xf8) << 2) | (p.palette[*tmp * 3 + 2] >> 3);
/*TODO*///					plot_pixel(*bitmap, x, y, pen);
/*TODO*///	
/*TODO*///					if (p.num_trans > 0)
/*TODO*///					{
/*TODO*///						if (*tmp < p.num_trans)
/*TODO*///							plot_pixel(*alpha, x, y, p.trans[*tmp]);
/*TODO*///						else
/*TODO*///							plot_pixel(*alpha, x, y, 255);
/*TODO*///					}
/*TODO*///					tmp++;
/*TODO*///				}
/*TODO*///	
/*TODO*///			free (p.palette);
/*TODO*///			break;
/*TODO*///	
/*TODO*///		case 6:
/*TODO*///			if ((*alpha = bitmap_alloc(p.width,p.height)) == 0)
/*TODO*///			{
/*TODO*///				logerror("Unable to allocate memory for artwork\n");
/*TODO*///				return 0;
/*TODO*///			}
/*TODO*///	
/*TODO*///		case 2:
/*TODO*///			if ((*bitmap = bitmap_alloc(p.width,p.height)) == 0)
/*TODO*///			{
/*TODO*///				logerror("Unable to allocate memory for artwork\n");
/*TODO*///				return 0;
/*TODO*///			}
/*TODO*///	
/*TODO*///			tmp = p.image;
/*TODO*///			for (y=0; y<p.height; y++)
/*TODO*///				for (x=0; x<p.width; x++)
/*TODO*///				{
/*TODO*///					if (depth == 32)
/*TODO*///						pen = (tmp[0] << 16) | (tmp[1] << 8) | tmp[2];
/*TODO*///					else
/*TODO*///						pen = ((tmp[0] & 0xf8) << 7) | ((tmp[1] & 0xf8) << 2) | (tmp[2] >> 3);
/*TODO*///					plot_pixel(*bitmap, x, y, pen);
/*TODO*///	
/*TODO*///					if (p.color_type == 6)
/*TODO*///					{
/*TODO*///						plot_pixel(*alpha, x, y, tmp[3]);
/*TODO*///						tmp += 4;
/*TODO*///					}
/*TODO*///					else
/*TODO*///						tmp += 3;
/*TODO*///				}
/*TODO*///	
/*TODO*///			break;
/*TODO*///	
/*TODO*///		default:
/*TODO*///			logerror("Unsupported color type %i \n", p.color_type);
/*TODO*///			return 0;
/*TODO*///			break;
		}
		p.image = null;
		return 1;
	}
	
	/*********************************************************************
	  load_png
	
	  This is what loads your backdrop in from disk.
	  start_pen = the first pen available for the backdrop to use
	 *********************************************************************/
	
	static void load_png(String filename, int start_pen,int width, int height, artwork_info a)
	{
            System.out.println("load_png");
		mame_bitmap picture = null, alpha = null;
		png_info p = new png_info();
		int scalex, scaley;
	
		/* If the user turned artwork off, bail */
		/*TODO*///if (options.use_artwork == 0) return;
	
		if (decode_png(filename, picture, alpha, p) == 0)
			return;

		allocate_artwork_mem(width, height, a);
	
	
		if (a==null)
			return;
	
		/* Scale the original picture to be the same size as the visible area */
		scalex = 0x10000 * picture.width  / (a).artwork.width;
		scaley = 0x10000 * picture.height / (a).artwork.height;
	
		if ((Machine.orientation & ORIENTATION_SWAP_XY) != 0)
		{
			int tmp;
			tmp = scalex;
			scalex = scaley;
			scaley = tmp;
		}
	
		copyrozbitmap((a).orig_artwork, picture, 0, 0, scalex, 0, 0, scaley, 0, null, TRANSPARENCY_NONE, 0, 0);
		/* We don't need the original any more */
		bitmap_free(picture);
	
		if (alpha != null)
		{
			copyrozbitmap((a).alpha, alpha, 0, 0, scalex, 0, 0, scaley, 0, null, TRANSPARENCY_NONE, 0, 0);
			bitmap_free(alpha);
		}
	
		if (Machine.color_depth == 16)
		{
			(a).start_pen = start_pen;
			init_palette(start_pen);
		}
		else
			(a).start_pen = 0;
	
		artwork_remap(a);
	}
	
	static void load_png_fit(String filename, int start_pen, artwork_info a)
	{
            System.out.println("load_png_fit");
		load_png(filename, start_pen, Machine.scrbitmap.width, Machine.scrbitmap.height, a);
	}

	
/*TODO*///	/*********************************************************************
/*TODO*///	  overlay_init
/*TODO*///	
/*TODO*///	
/*TODO*///	 *********************************************************************/
/*TODO*///	static void overlay_init(struct artwork_info *a)
/*TODO*///	{
/*TODO*///		int i,j, rgb555;
/*TODO*///		UINT8 r,g,b;
/*TODO*///		float h, s, v, rf, gf, bf;
/*TODO*///		int offset, height, width;
/*TODO*///		struct mame_bitmap *overlay, *overlay1, *orig;
/*TODO*///	
/*TODO*///		offset = a.start_pen;
/*TODO*///		height = a.artwork.height;
/*TODO*///		width = a.artwork.width;
/*TODO*///		overlay = a.artwork;
/*TODO*///		overlay1 = a.artwork1;
/*TODO*///		orig = a.orig_artwork;
/*TODO*///	
/*TODO*///		if (a.alpha)
/*TODO*///		{
/*TODO*///			for ( j=0; j<height; j++)
/*TODO*///				for (i=0; i<width; i++)
/*TODO*///				{
/*TODO*///					UINT32 v1,v2;
/*TODO*///					UINT16 alpha = ((UINT16 *)a.alpha.line[j])[i];
/*TODO*///	
/*TODO*///					rgb555 = ((UINT16 *)orig.line[j])[i];
/*TODO*///					r = rgb555 >> 10;
/*TODO*///					g = (rgb555 >> 5) & 0x1f;
/*TODO*///					b = rgb555 &0x1f;
/*TODO*///					v1 = (MAX(r, MAX(g, b)));
/*TODO*///					v2 = (v1 * (alpha >> 3)) / 0x1f;
/*TODO*///					a.rgb[j*width+i] = (v1 << 24) | (v2 << 16) | rgb555;
/*TODO*///	
/*TODO*///					RGBtoHSV( r/31.0, g/31.0, b/31.0, &h, &s, &v );
/*TODO*///	
/*TODO*///					HSVtoRGB( &rf, &gf, &bf, h, s, v * alpha/255.0);
/*TODO*///					r = rf*31; g = gf*31; b = bf*31;
/*TODO*///					((UINT16 *)overlay.line[j])[i] = ((r << 10) | (g << 5) | b) + offset;
/*TODO*///	
/*TODO*///					HSVtoRGB( &rf, &gf, &bf, h, s, 1);
/*TODO*///					r = rf*31; g = gf*31; b = bf*31;
/*TODO*///					((UINT16 *)overlay1.line[j])[i] = ((r << 10) | (g << 5) | b) + offset;
/*TODO*///				}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	/*********************************************************************
/*TODO*///	  overlay_draw
/*TODO*///	
/*TODO*///	  Supports different levels of intensity on the screen and different
/*TODO*///	  levels of transparancy of the overlay.
/*TODO*///	 *********************************************************************/
/*TODO*///	
/*TODO*///	static void overlay_draw(struct mame_bitmap *dest, struct mame_bitmap *source)
/*TODO*///	{
/*TODO*///		int i, j;
/*TODO*///		int height, width;
/*TODO*///		int r, g, b, bp, v, vn, black, start_pen;
/*TODO*///		UINT8 r8, g8, b8;
/*TODO*///		UINT16 *src, *dst, *bg, *fg;
/*TODO*///		UINT32 bright[65536];
/*TODO*///		UINT32 *rgb;
/*TODO*///	
/*TODO*///		memset (bright, 0xff, sizeof(int)*65536);
/*TODO*///		height = source.height;
/*TODO*///		width = source.width;
/*TODO*///	
/*TODO*///		switch (Machine.color_depth)
/*TODO*///		{
/*TODO*///		case 16:
/*TODO*///			if (artwork_overlay.start_pen == 2)
/*TODO*///			{
/*TODO*///				/* fast version */
/*TODO*///				height = artwork_overlay.artwork.height;
/*TODO*///				width = artwork_overlay.artwork.width;
/*TODO*///	
/*TODO*///				for ( j = 0; j < height; j++)
/*TODO*///				{
/*TODO*///					dst = (UINT16 *)dest.line[j];
/*TODO*///					src = (UINT16 *)source.line[j];
/*TODO*///					bg = (UINT16 *)artwork_overlay.artwork.line[j];
/*TODO*///					fg = (UINT16 *)artwork_overlay.artwork1.line[j];
/*TODO*///					for (i = width; i > 0; i--)
/*TODO*///					{
/*TODO*///						if (*src!=0)
/*TODO*///							*dst = *fg;
/*TODO*///						else
/*TODO*///							*dst = *bg;
/*TODO*///						dst++;
/*TODO*///						src++;
/*TODO*///						fg++;
/*TODO*///						bg++;
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///			else /* slow version */
/*TODO*///			{
/*TODO*///				rgb = artwork_overlay.rgb;
/*TODO*///				start_pen = artwork_overlay.start_pen;
/*TODO*///				copybitmap(dest, artwork_overlay.artwork ,0,0,0,0,NULL,TRANSPARENCY_NONE,0);
/*TODO*///				black = -1;
/*TODO*///				for ( j = 0; j < height; j++)
/*TODO*///				{
/*TODO*///					dst = (UINT16 *)dest.line[j];
/*TODO*///					src = (UINT16 *)source.line[j];
/*TODO*///	
/*TODO*///					for (i = width; i > 0; i--)
/*TODO*///					{
/*TODO*///						if (*src != black)
/*TODO*///						{
/*TODO*///							bp = bright[*src];
/*TODO*///							if (bp)
/*TODO*///							{
/*TODO*///								if (bp == 0xffffffff)
/*TODO*///								{
/*TODO*///									palette_get_color(*src, &r8, &g8, &b8);
/*TODO*///									bright[*src]=bp=(222*r8+707*g8+71*b8)/1000;
/*TODO*///								}
/*TODO*///	
/*TODO*///								v = *rgb >> 24;
/*TODO*///								vn =(*rgb >> 16) & 0x1f;
/*TODO*///								vn += ((0x1f - vn) * (bp >> 3)) / 0x1f;
/*TODO*///								if (v > 0)
/*TODO*///								{
/*TODO*///									r = (((*rgb >> 10) & 0x1f) * vn) / v;
/*TODO*///									g = (((*rgb >> 5)  & 0x1f) * vn) / v;
/*TODO*///									b = ((*rgb & 0x1f) * vn) / v;
/*TODO*///									*dst = ((r << 10) | (g << 5) | b) + start_pen;
/*TODO*///								}
/*TODO*///								else
/*TODO*///									*dst = ((vn << 10) | (vn << 5) | vn) + start_pen;
/*TODO*///							}
/*TODO*///							else
/*TODO*///								black = *src;
/*TODO*///						}
/*TODO*///						src++;
/*TODO*///						dst++;
/*TODO*///						rgb++;
/*TODO*///					}
/*TODO*///				}
/*TODO*///	
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		case 15:
/*TODO*///			rgb = artwork_overlay.rgb;
/*TODO*///			start_pen = artwork_overlay.start_pen;
/*TODO*///			copybitmap(dest, artwork_overlay.artwork ,0,0,0,0,NULL,TRANSPARENCY_NONE,0);
/*TODO*///			black = -1;
/*TODO*///			for ( j = 0; j < height; j++)
/*TODO*///			{
/*TODO*///				dst = (UINT16 *)dest.line[j];
/*TODO*///				src = (UINT16 *)source.line[j];
/*TODO*///	
/*TODO*///				for (i = width; i > 0; i--)
/*TODO*///				{
/*TODO*///					if (*src != black)
/*TODO*///					{
/*TODO*///						bp = bright[*src];
/*TODO*///						if (bp)
/*TODO*///						{
/*TODO*///							if (bp == 0xffffffff)
/*TODO*///								bright[*src]=bp=(222*(*src >> 10)
/*TODO*///												 +707*((*src >> 5) & 0x1f)
/*TODO*///												 +71*(*src & 0x1f))/1000;
/*TODO*///	
/*TODO*///							v = *rgb >> 24;
/*TODO*///							vn =(*rgb >> 16) & 0x1f;
/*TODO*///							vn += ((0x1f - vn) * bp) / 0x1f;
/*TODO*///							if (v > 0)
/*TODO*///							{
/*TODO*///								r = (((*rgb >> 10) & 0x1f) * vn) / v;
/*TODO*///								g = (((*rgb >> 5)  & 0x1f) * vn) / v;
/*TODO*///								b = ((*rgb & 0x1f) * vn) / v;
/*TODO*///								*dst = ((r << 10) | (g << 5) | b);
/*TODO*///							}
/*TODO*///							else
/*TODO*///								*dst = ((vn << 10) | (vn << 5) | vn);
/*TODO*///						}
/*TODO*///						else
/*TODO*///							black = *src;
/*TODO*///					}
/*TODO*///					src++;
/*TODO*///					dst++;
/*TODO*///					rgb++;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		default:
/*TODO*///			logerror ("Color depth of %d not supported with overlays\n", Machine.color_depth);
/*TODO*///			break;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	/*********************************************************************
/*TODO*///	  backdrop_draw
/*TODO*///	
/*TODO*///	 *********************************************************************/
/*TODO*///	
/*TODO*///	static void backdrop_draw(struct mame_bitmap *dest, struct mame_bitmap *source)
/*TODO*///	{
/*TODO*///		int i, j, brgb, bp, black = -1;
/*TODO*///		UINT8 r, g, b;
/*TODO*///		UINT32 bright[65536];
/*TODO*///	
/*TODO*///		memset (bright, 0xff, sizeof(int)*65536);
/*TODO*///		copybitmap(dest, artwork_backdrop.artwork ,0,0,0,0,NULL,TRANSPARENCY_NONE,0);
/*TODO*///	
/*TODO*///		switch (Machine.color_depth)
/*TODO*///		{
/*TODO*///		case 16:
/*TODO*///		{
/*TODO*///			UINT16 *dst, *bdr, *src;
/*TODO*///			for ( j = 0; j < source.height; j++)
/*TODO*///			{
/*TODO*///				dst = (UINT16 *)dest.line[j];
/*TODO*///				src = (UINT16 *)source.line[j];
/*TODO*///				bdr = (UINT16 *)artwork_backdrop.artwork.line[j];
/*TODO*///				for (i = 0; i < source.width; i++)
/*TODO*///				{
/*TODO*///					if (*src != black)
/*TODO*///					{
/*TODO*///						bp = bright[*src];
/*TODO*///						if (bp)
/*TODO*///						{
/*TODO*///							if (bp == 0xffffffff)
/*TODO*///							{
/*TODO*///								palette_get_color(*src, &r, &g, &b);
/*TODO*///								bright[*src]=(222*r+707*g+71*b)/1000;
/*TODO*///							}
/*TODO*///							else
/*TODO*///								palette_get_color(*src, &r, &g, &b);
/*TODO*///	
/*TODO*///							r >>= 3;
/*TODO*///							g >>= 3;
/*TODO*///							b >>= 3;
/*TODO*///							brgb = *bdr - artwork_backdrop.start_pen;
/*TODO*///							r += brgb >> 10;
/*TODO*///							if (r > 0x1f) r = 0x1f;
/*TODO*///							g += (brgb >> 5) & 0x1f;
/*TODO*///							if (g > 0x1f) g = 0x1f;
/*TODO*///							b += brgb & 0x1f;
/*TODO*///							if (b > 0x1f) b = 0x1f;
/*TODO*///							*dst = ((r << 10) | (g << 5) | b) + artwork_backdrop.start_pen;
/*TODO*///						}
/*TODO*///						else
/*TODO*///							black = *src;
/*TODO*///					}
/*TODO*///					dst++;
/*TODO*///					src++;
/*TODO*///					bdr++;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		}
/*TODO*///		case 15:
/*TODO*///		{
/*TODO*///			UINT16 *dst, *src, *bdr;
/*TODO*///			for ( j = 0; j < source.height; j++)
/*TODO*///			{
/*TODO*///				dst = (UINT16 *)dest.line[j];
/*TODO*///				src = (UINT16 *)source.line[j];
/*TODO*///				bdr = (UINT16 *)artwork_backdrop.artwork.line[j];
/*TODO*///				for (i = 0; i < source.width; i++)
/*TODO*///				{
/*TODO*///					if (*src)
/*TODO*///					{
/*TODO*///						*dst = LIMIT5((*src & 0x1f) + (*bdr & 0x1f))
/*TODO*///							| (LIMIT5(((*src >> 5) & 0x1f) + ((*bdr >> 5) & 0x1f)) << 5)
/*TODO*///							| (LIMIT5((*src >> 10) + (*bdr >> 10)) << 10);
/*TODO*///					}
/*TODO*///					dst++;
/*TODO*///					src++;
/*TODO*///					bdr++;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		}
/*TODO*///		case 32:
/*TODO*///		{
/*TODO*///			UINT32 *dst, *src, *bdr;
/*TODO*///			for ( j = 0; j < source.height; j++)
/*TODO*///			{
/*TODO*///				dst = (UINT32 *)dest.line[j];
/*TODO*///				src = (UINT32 *)source.line[j];
/*TODO*///				bdr = (UINT32 *)artwork_backdrop.artwork.line[j];
/*TODO*///				for (i = 0; i < source.width; i++)
/*TODO*///				{
/*TODO*///					if (*src)
/*TODO*///					{
/*TODO*///						*dst = LIMIT8((*src & 0xff) + (*bdr & 0xff))
/*TODO*///							| (LIMIT8(((*src >> 8) & 0xff) + ((*bdr >> 8) & 0xff)) << 8)
/*TODO*///							| (LIMIT8((*src >> 16) + (*bdr >> 16)) << 16);
/*TODO*///					}
/*TODO*///					dst++;
/*TODO*///					src++;
/*TODO*///					bdr++;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			break;
/*TODO*///		}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	void artwork_draw(struct mame_bitmap *dest, struct mame_bitmap *source, int full_refresh)
/*TODO*///	{
/*TODO*///		if (artwork_backdrop) backdrop_draw(dest, source);
/*TODO*///		if (artwork_overlay) overlay_draw(dest, source);
/*TODO*///	}
	
	/*********************************************************************
	  artwork_free
	
	  Don't forget to clean up when you're done with the backdrop!!!
	 *********************************************************************/
	
	public static void artwork_free(artwork_info a)
	{
		if (a != null)
		{
			if ((a).artwork != null)
				bitmap_free((a).artwork);
			if ((a).artwork1 != null)
				bitmap_free((a).artwork1);
			if ((a).alpha != null)
				bitmap_free((a).alpha);
			if ((a).orig_artwork != null)
				bitmap_free((a).orig_artwork);
			if ((a).rgb != null)
				(a).rgb = null;
			
			a = null;
		}
	}
	
/*TODO*///	void artwork_kill (void)
/*TODO*///	{
/*TODO*///		if (artwork_backdrop || artwork_overlay)
/*TODO*///			bitmap_free(artwork_real_scrbitmap);
/*TODO*///	
/*TODO*///		if (artwork_backdrop) artwork_free(&artwork_backdrop);
/*TODO*///		if (artwork_overlay) artwork_free(&artwork_overlay);
/*TODO*///	}
/*TODO*///	
	public static void overlay_load(String filename, int start_pen)
	{
/*TODO*///		int width, height;
/*TODO*///	
/*TODO*///		/* replace the real display with a fake one, this way drivers can access Machine.scrbitmap
/*TODO*///		   the same way as before */
/*TODO*///	
/*TODO*///		width = Machine.scrbitmap.width;
/*TODO*///		height = Machine.scrbitmap.height;
/*TODO*///	
/*TODO*///		if (Machine.orientation & ORIENTATION_SWAP_XY)
/*TODO*///		{
/*TODO*///			int temp;
/*TODO*///	
/*TODO*///			temp = height;
/*TODO*///			height = width;
/*TODO*///			width = temp;
/*TODO*///		}
/*TODO*///	
/*TODO*///		load_png_fit(filename, start_pen, &artwork_overlay);
/*TODO*///	
/*TODO*///		if (artwork_overlay)
/*TODO*///		{
/*TODO*///			if ((artwork_real_scrbitmap = bitmap_alloc(width, height)) == 0)
/*TODO*///			{
/*TODO*///				artwork_kill();
/*TODO*///				logerror("Not enough memory for artwork!\n");
/*TODO*///				return;
/*TODO*///			}
/*TODO*///			overlay_init(artwork_overlay);
/*TODO*///		}
	}

	public static void backdrop_load(String filename, int start_pen)
	{
/*TODO*///		int width, height;
/*TODO*///	
/*TODO*///		/* replace the real display with a fake one, this way drivers can access Machine.scrbitmap
/*TODO*///		   the same way as before */
/*TODO*///	
/*TODO*///		load_png_fit(filename, start_pen, &artwork_backdrop);
/*TODO*///	
/*TODO*///		if (artwork_backdrop)
/*TODO*///		{
/*TODO*///			width = artwork_backdrop.artwork.width;
/*TODO*///			height = artwork_backdrop.artwork.height;
/*TODO*///	
/*TODO*///			if (Machine.orientation & ORIENTATION_SWAP_XY)
/*TODO*///			{
/*TODO*///				int temp;
/*TODO*///	
/*TODO*///				temp = height;
/*TODO*///				height = width;
/*TODO*///				width = temp;
/*TODO*///			}
/*TODO*///	
/*TODO*///			if ((artwork_real_scrbitmap = bitmap_alloc(width, height)) == 0)
/*TODO*///			{
/*TODO*///				artwork_kill();
/*TODO*///				logerror("Not enough memory for artwork!\n");
/*TODO*///				return;
/*TODO*///			}
/*TODO*///		}
	}
	
	public static void artwork_load(artwork_info a, String filename, int start_pen)
	{
            System.out.println("artwork_load");
		load_png_fit(filename, start_pen, a);
	}
	
/*TODO*///	void artwork_load_size(struct artwork_info **a, const char *filename, unsigned int start_pen,
/*TODO*///						   int width, int height)
/*TODO*///	{
/*TODO*///		load_png(filename, start_pen, width, height, a);
/*TODO*///	}
/*TODO*///	
/*TODO*///	/*********************************************************************
/*TODO*///	  artwork_elements scale
/*TODO*///	
/*TODO*///	  scales an array of artwork elements to width and height. The first
/*TODO*///	  element (which has to be a box) is used as reference. This is useful
/*TODO*///	  for atwork with disks.
/*TODO*///	
/*TODO*///	*********************************************************************/
/*TODO*///	
/*TODO*///	void artwork_elements_scale(struct artwork_element *ae, int width, int height)
/*TODO*///	{
/*TODO*///		int scale_w, scale_h;
/*TODO*///	
/*TODO*///		if (Machine.orientation & ORIENTATION_SWAP_XY)
/*TODO*///		{
/*TODO*///			scale_w = (height << 16)/(ae.box.max_x + 1);
/*TODO*///			scale_h = (width << 16)/(ae.box.max_y + 1);
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			scale_w = (width << 16)/(ae.box.max_x + 1);
/*TODO*///			scale_h = (height << 16)/(ae.box.max_y + 1);
/*TODO*///		}
/*TODO*///		while (ae.box.min_x >= 0)
/*TODO*///		{
/*TODO*///			ae.box.min_x = (ae.box.min_x * scale_w) >> 16;
/*TODO*///			ae.box.max_x = (ae.box.max_x * scale_w) >> 16;
/*TODO*///			ae.box.min_y = (ae.box.min_y * scale_h) >> 16;
/*TODO*///			if (ae.box.max_y >= 0)
/*TODO*///				ae.box.max_y = (ae.box.max_y * scale_h) >> 16;
/*TODO*///			ae++;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	/*********************************************************************
/*TODO*///	  overlay_create
/*TODO*///	
/*TODO*///	  This works similar to artwork_load but generates artwork from
/*TODO*///	  an array of artwork_element. This is useful for very simple artwork
/*TODO*///	  like the overlay in the Space invaders series of games.  The overlay
/*TODO*///	  is defined to be the same size as the screen.
/*TODO*///	  The end of the array is marked by an entry with negative coordinates.
/*TODO*///	  Boxes and disks are supported. Disks are marked max_y == -1,
/*TODO*///	  min_x == x coord. of center, min_y == y coord. of center, max_x == radius.
/*TODO*///	  If there are transparent and opaque overlay elements, the opaque ones
/*TODO*///	  have to be at the end of the list to stay compatible with the PNG
/*TODO*///	  artwork.
/*TODO*///	 *********************************************************************/
	public static void overlay_create(artwork_element[] ae, int start_pen)
	{
/*TODO*///		struct mame_bitmap *disk, *disk_alpha, *box, *box_alpha;
/*TODO*///		int pen, transparent_pen = -1, disk_type, white_pen;
/*TODO*///		int width, height;
/*TODO*///	
/*TODO*///		allocate_artwork_mem(Machine.scrbitmap.width, Machine.scrbitmap.height, &artwork_overlay);
/*TODO*///	
/*TODO*///		if (artwork_overlay==NULL)
/*TODO*///			return;
/*TODO*///	
/*TODO*///		/* replace the real display with a fake one, this way drivers can access Machine.scrbitmap
/*TODO*///		   the same way as before */
/*TODO*///	
/*TODO*///		width = Machine.scrbitmap.width;
/*TODO*///		height = Machine.scrbitmap.height;
/*TODO*///	
/*TODO*///		if (Machine.orientation & ORIENTATION_SWAP_XY)
/*TODO*///		{
/*TODO*///			int temp;
/*TODO*///	
/*TODO*///			temp = height;
/*TODO*///			height = width;
/*TODO*///			width = temp;
/*TODO*///		}
/*TODO*///	
/*TODO*///		if ((artwork_real_scrbitmap = bitmap_alloc(width, height)) == 0)
/*TODO*///		{
/*TODO*///			artwork_kill();
/*TODO*///			logerror("Not enough memory for artwork!\n");
/*TODO*///			return;
/*TODO*///		}
/*TODO*///	
/*TODO*///		transparent_pen = 0xffff;
/*TODO*///		white_pen = 0x7fff;
/*TODO*///		fillbitmap (artwork_overlay.orig_artwork, white_pen, 0);
/*TODO*///		fillbitmap (artwork_overlay.alpha, 0, 0);
/*TODO*///	
/*TODO*///		while (ae.box.min_x >= 0)
/*TODO*///		{
/*TODO*///			int alpha = ae.alpha;
/*TODO*///	
/*TODO*///			if (alpha == OVERLAY_DEFAULT_OPACITY)
/*TODO*///			{
/*TODO*///				alpha = 0x18;
/*TODO*///			}
/*TODO*///	
/*TODO*///			pen = ((ae.red & 0xf8) << 7) | ((ae.green & 0xf8) << 2) | (ae.blue >> 3);
/*TODO*///			if (ae.box.max_y < 0) /* disk */
/*TODO*///			{
/*TODO*///				int r = ae.box.max_x;
/*TODO*///				disk_type = ae.box.max_y;
/*TODO*///	
/*TODO*///				switch (disk_type)
/*TODO*///				{
/*TODO*///				case -1: /* disk overlay */
/*TODO*///					if ((disk = create_disk (r, pen, white_pen)) == NULL)
/*TODO*///					{
/*TODO*///						artwork_kill();
/*TODO*///						return;
/*TODO*///					}
/*TODO*///					if ((disk_alpha = create_disk (r, alpha, 0)) == NULL)
/*TODO*///					{
/*TODO*///						artwork_kill();
/*TODO*///						return;
/*TODO*///					}
/*TODO*///					merge_cmy (artwork_overlay, disk, disk_alpha, ae.box.min_x - r, ae.box.min_y - r);
/*TODO*///					bitmap_free(disk_alpha);
/*TODO*///					bitmap_free(disk);
/*TODO*///					break;
/*TODO*///	
/*TODO*///				case -2: /* punched disk */
/*TODO*///					if ((disk = create_disk (r, pen, transparent_pen)) == NULL)
/*TODO*///					{
/*TODO*///						artwork_kill();
/*TODO*///						return;
/*TODO*///					}
/*TODO*///					copybitmap(artwork_overlay.orig_artwork,disk,0, 0,
/*TODO*///							   ae.box.min_x - r,
/*TODO*///							   ae.box.min_y - r,
/*TODO*///							   0,TRANSPARENCY_PEN, transparent_pen);
/*TODO*///					/* alpha */
/*TODO*///					if ((disk_alpha = create_disk (r, alpha, transparent_pen)) == NULL)
/*TODO*///					{
/*TODO*///						artwork_kill();
/*TODO*///						return;
/*TODO*///					}
/*TODO*///					copybitmap(artwork_overlay.alpha,disk_alpha,0, 0,
/*TODO*///							   ae.box.min_x - r,
/*TODO*///							   ae.box.min_y - r,
/*TODO*///							   0,TRANSPARENCY_PEN, transparent_pen);
/*TODO*///					bitmap_free(disk_alpha);
/*TODO*///					bitmap_free(disk);
/*TODO*///					break;
/*TODO*///	
/*TODO*///				}
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				if ((box = bitmap_alloc(ae.box.max_x - ae.box.min_x + 1,
/*TODO*///											 ae.box.max_y - ae.box.min_y + 1)) == 0)
/*TODO*///				{
/*TODO*///					logerror("Not enough memory for artwork!\n");
/*TODO*///					artwork_kill();
/*TODO*///					return;
/*TODO*///				}
/*TODO*///				if ((box_alpha = bitmap_alloc(ae.box.max_x - ae.box.min_x + 1,
/*TODO*///											 ae.box.max_y - ae.box.min_y + 1)) == 0)
/*TODO*///				{
/*TODO*///					logerror("Not enough memory for artwork!\n");
/*TODO*///					artwork_kill();
/*TODO*///					return;
/*TODO*///				}
/*TODO*///				fillbitmap (box, pen, 0);
/*TODO*///				fillbitmap (box_alpha, alpha, 0);
/*TODO*///				merge_cmy (artwork_overlay, box, box_alpha, ae.box.min_x, ae.box.min_y);
/*TODO*///				bitmap_free(box);
/*TODO*///				bitmap_free(box_alpha);
/*TODO*///			}
/*TODO*///			ae++;
/*TODO*///		}
/*TODO*///	
/*TODO*///		if (Machine.color_depth == 16)
/*TODO*///		{
/*TODO*///			artwork_overlay.start_pen = start_pen;
/*TODO*///			init_palette(start_pen);
/*TODO*///		}
/*TODO*///		else
/*TODO*///			artwork_overlay.start_pen = 0;
/*TODO*///	
/*TODO*///		artwork_remap(artwork_overlay);
/*TODO*///		overlay_init(artwork_overlay);
	}
/*TODO*///	
/*TODO*///	int artwork_get_size_info(const char *file_name, struct artwork_size_info *a)
/*TODO*///	{
/*TODO*///		void *fp;
/*TODO*///		struct png_info p;
/*TODO*///		int file_name_len;
/*TODO*///		char file_name2[256];
/*TODO*///	
/*TODO*///		/* If the user turned artwork off, bail */
/*TODO*///		if (!options.use_artwork) return 0;
/*TODO*///	
/*TODO*///		/* check for .png */
/*TODO*///		strcpy(file_name2, file_name);
/*TODO*///		file_name_len = strlen(file_name2);
/*TODO*///		if ((file_name_len < 4) || my_stricmp(&file_name2[file_name_len - 4], ".png"))
/*TODO*///		{
/*TODO*///			strcat(file_name2, ".png");
/*TODO*///		}
/*TODO*///	
/*TODO*///		if (!(fp = osd_fopen(Machine.gamedrv.name, file_name2, OSD_FILETYPE_ARTWORK, 0)))
/*TODO*///		{
/*TODO*///			logerror("Unable to open PNG %s\n", file_name);
/*TODO*///			return 0;
/*TODO*///		}
/*TODO*///	
/*TODO*///		if (!png_read_info(fp, &p))
/*TODO*///		{
/*TODO*///			osd_fclose (fp);
/*TODO*///			return 0;
/*TODO*///		}
/*TODO*///		osd_fclose (fp);
/*TODO*///	
/*TODO*///		a.width = p.width;
/*TODO*///		a.height = p.height;
/*TODO*///		a.screen = p.screen;
/*TODO*///	
/*TODO*///		return 1;
/*TODO*///	}
	
}
