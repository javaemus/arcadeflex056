/*********************************************************************

  png.c

  PNG reading functions.

  07/15/1998 Created by Mathis Rosenhauer
  10/02/1998 Code clean up and abstraction by Mike Balfour
             and Mathis Rosenhauer
  10/15/1998 Image filtering. MLR
  11/09/1998 Bit depths 1-8 MLR
  11/10/1998 Some additional PNG chunks recognized MLR
  05/14/1999 Color type 2 and PNG save functions added
  05/15/1999 Handle RGB555 while saving, use osd_fxxx
             functions for writing MSH
  04/27/2001 Simple MNG support MLR

  TODO : Fully comply with the "Recommendations for Decoders"
         of the W3C

*********************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056;

public class png
{
    /*TODO*////* convert_uint is here so we don't have to deal with byte-ordering issues */
    /*TODO*///	static UINT32 convert_from_network_order (UINT8 *v)
    /*TODO*///	{
    /*TODO*///		UINT32 i;
    /*TODO*///	
    /*TODO*///		i = (v[0]<<24) | (v[1]<<16) | (v[2]<<8) | (v[3]);
    /*TODO*///		return i;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	int png_unfilter(struct png_info *p)
    /*TODO*///	{
    /*TODO*///		int i, j, bpp, filter;
    /*TODO*///		INT32 prediction, pA, pB, pC, dA, dB, dC;
    /*TODO*///		UINT8 *src, *dst;
    /*TODO*///	
    /*TODO*///		if((p->image = (UINT8 *)malloc (p->height*p->rowbytes))==NULL)
    /*TODO*///		{
    /*TODO*///			logerror("Out of memory\n");
    /*TODO*///			free (p->fimage);
    /*TODO*///			return 0;
    /*TODO*///		}
    /*TODO*///	
    /*TODO*///		src = p->fimage;
    /*TODO*///		dst = p->image;
    /*TODO*///		bpp = p->bpp;
    /*TODO*///	
    /*TODO*///		for (i=0; i<p->height; i++)
    /*TODO*///		{
    /*TODO*///			filter = *src++;
    /*TODO*///			if (filter == 0)
    /*TODO*///			{
    /*TODO*///				memcpy (dst, src, p->rowbytes);
    /*TODO*///				src += p->rowbytes;
    /*TODO*///				dst += p->rowbytes;
    /*TODO*///			}
    /*TODO*///			else
    /*TODO*///				for (j=0; j<p->rowbytes; j++)
    /*TODO*///				{
    /*TODO*///					pA = (j<bpp) ? 0: *(dst - bpp);
    /*TODO*///					pB = (i<1) ? 0: *(dst - p->rowbytes);
    /*TODO*///					pC = ((j<bpp)||(i<1)) ? 0: *(dst - p->rowbytes - bpp);
    /*TODO*///	
    /*TODO*///					switch (filter)
    /*TODO*///					{
    /*TODO*///					case PNG_PF_Sub:
    /*TODO*///						prediction = pA;
    /*TODO*///						break;
    /*TODO*///					case PNG_PF_Up:
    /*TODO*///						prediction = pB;
    /*TODO*///						break;
    /*TODO*///					case PNG_PF_Average:
    /*TODO*///						prediction = ((pA + pB) / 2);
    /*TODO*///						break;
    /*TODO*///					case PNG_PF_Paeth:
    /*TODO*///						prediction = pA + pB - pC;
    /*TODO*///						dA = abs(prediction - pA);
    /*TODO*///						dB = abs(prediction - pB);
    /*TODO*///						dC = abs(prediction - pC);
    /*TODO*///						if (dA <= dB && dA <= dC) prediction = pA;
    /*TODO*///						else if (dB <= dC) prediction = pB;
    /*TODO*///						else prediction = pC;
    /*TODO*///						break;
    /*TODO*///					default:
    /*TODO*///						logerror("Unknown filter type %i\n",filter);
    /*TODO*///						prediction = 0;
    /*TODO*///					}
    /*TODO*///					*dst = 0xff & (*src + prediction);
    /*TODO*///					dst++; src++;
    /*TODO*///				}
    /*TODO*///		}
    /*TODO*///	
    /*TODO*///		free (p->fimage);
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	int png_verify_signature (void *fp)
    /*TODO*///	{
    /*TODO*///		INT8 signature[8];
    /*TODO*///	
    /*TODO*///		if (osd_fread (fp, signature, 8) != 8)
    /*TODO*///		{
    /*TODO*///			logerror("Unable to read PNG signature (EOF)\n");
    /*TODO*///			return 0;
    /*TODO*///		}
    /*TODO*///	
    /*TODO*///		if (memcmp(signature, PNG_Signature, 8))
    /*TODO*///		{
    /*TODO*///			logerror("PNG signature mismatch found: %s expected: %s\n",signature,PNG_Signature);
    /*TODO*///			return 0;
    /*TODO*///		}
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	int png_inflate_image (struct png_info *p)
    /*TODO*///	{
    /*TODO*///		unsigned long fbuff_size;
    /*TODO*///	
    /*TODO*///		fbuff_size = p->height * (p->rowbytes + 1);
    /*TODO*///	
    /*TODO*///		if((p->fimage = (UINT8 *)malloc (fbuff_size))==NULL)
    /*TODO*///		{
    /*TODO*///			logerror("Out of memory\n");
    /*TODO*///			free (p->zimage);
    /*TODO*///			return 0;
    /*TODO*///		}
    /*TODO*///	
    /*TODO*///		if (uncompress(p->fimage, &fbuff_size, p->zimage, p->zlength) != Z_OK)
    /*TODO*///		{
    /*TODO*///			logerror("Error while inflating image\n");
    /*TODO*///			return 0;
    /*TODO*///		}
    /*TODO*///	
    /*TODO*///		free (p->zimage);
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	int png_read_file(void *fp, struct png_info *p)
    /*TODO*///	{
    /*TODO*///		/* translates color_type to bytes per pixel */
    /*TODO*///		const int samples[] = {1, 0, 3, 1, 2, 0, 4};
    /*TODO*///	
    /*TODO*///		UINT32 chunk_length, chunk_type=0, chunk_crc, crc;
    /*TODO*///		UINT8 *chunk_data, *temp;
    /*TODO*///		UINT8 str_chunk_type[5], v[4];
    /*TODO*///	
    /*TODO*///		struct idat
    /*TODO*///		{
    /*TODO*///			struct idat *next;
    /*TODO*///			int length;
    /*TODO*///			UINT8 *data;
    /*TODO*///		} *ihead, *pidat;
    /*TODO*///	
    /*TODO*///		if ((ihead = malloc (sizeof(struct idat))) == 0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		pidat = ihead;
    /*TODO*///	
    /*TODO*///		p->zlength = 0;
    /*TODO*///		p->num_palette = 0;
    /*TODO*///		p->num_trans = 0;
    /*TODO*///		p->trans = NULL;
    /*TODO*///		p->palette = NULL;
    /*TODO*///	
    /*TODO*///		if (png_verify_signature(fp)==0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		while (chunk_type != PNG_CN_IEND)
    /*TODO*///		{
    /*TODO*///			if (osd_fread(fp, v, 4) != 4)
    /*TODO*///				logerror("Unexpected EOF in PNG\n");
    /*TODO*///			chunk_length=convert_from_network_order(v);
    /*TODO*///	
    /*TODO*///			if (osd_fread(fp, str_chunk_type, 4) != 4)
    /*TODO*///				logerror("Unexpected EOF in PNG file\n");
    /*TODO*///	
    /*TODO*///			str_chunk_type[4]=0; /* terminate string */
    /*TODO*///	
    /*TODO*///			crc=crc32(0,str_chunk_type, 4);
    /*TODO*///			chunk_type = convert_from_network_order(str_chunk_type);
    /*TODO*///	
    /*TODO*///			if (chunk_length)
    /*TODO*///			{
    /*TODO*///				if ((chunk_data = (UINT8 *)malloc(chunk_length+1))==NULL)
    /*TODO*///				{
    /*TODO*///					logerror("Out of memory\n");
    /*TODO*///					return 0;
    /*TODO*///				}
    /*TODO*///				if (osd_fread (fp, chunk_data, chunk_length) != chunk_length)
    /*TODO*///				{
    /*TODO*///					logerror("Unexpected EOF in PNG file\n");
    /*TODO*///					free(chunk_data);
    /*TODO*///					return 0;
    /*TODO*///				}
    /*TODO*///	
    /*TODO*///				crc=crc32(crc,chunk_data, chunk_length);
    /*TODO*///			}
    /*TODO*///			else
    /*TODO*///				chunk_data = NULL;
    /*TODO*///	
    /*TODO*///			if (osd_fread(fp, v, 4) != 4)
    /*TODO*///				logerror("Unexpected EOF in PNG\n");
    /*TODO*///			chunk_crc=convert_from_network_order(v);
    /*TODO*///	
    /*TODO*///			if (crc != chunk_crc)
    /*TODO*///			{
    /*TODO*///				logerror("CRC check failed while reading PNG chunk %s\n",str_chunk_type);
    /*TODO*///				logerror("Found: %08X  Expected: %08X\n",crc,chunk_crc);
    /*TODO*///				return 0;
    /*TODO*///			}
    /*TODO*///	
    /*TODO*///			logerror("Reading PNG chunk %s\n", str_chunk_type);
    /*TODO*///	
    /*TODO*///			switch (chunk_type)
    /*TODO*///			{
    /*TODO*///			case PNG_CN_IHDR:
    /*TODO*///				p->width = convert_from_network_order(chunk_data);
    /*TODO*///				p->height = convert_from_network_order(chunk_data+4);
    /*TODO*///				p->bit_depth = *(chunk_data+8);
    /*TODO*///				p->color_type = *(chunk_data+9);
    /*TODO*///				p->compression_method = *(chunk_data+10);
    /*TODO*///				p->filter_method = *(chunk_data+11);
    /*TODO*///				p->interlace_method = *(chunk_data+12);
    /*TODO*///				free (chunk_data);
    /*TODO*///	
    /*TODO*///				logerror("PNG IHDR information:\n");
    /*TODO*///				logerror("Width: %i, Height: %i\n", p->width, p->height);
    /*TODO*///				logerror("Bit depth %i, color type: %i\n", p->bit_depth, p->color_type);
    /*TODO*///				logerror("Compression method: %i, filter: %i, interlace: %i\n",
    /*TODO*///						p->compression_method, p->filter_method, p->interlace_method);
    /*TODO*///				break;
    /*TODO*///	
    /*TODO*///			case PNG_CN_PLTE:
    /*TODO*///				p->num_palette=chunk_length/3;
    /*TODO*///				p->palette=chunk_data;
    /*TODO*///				logerror("%i palette entries\n", p->num_palette);
    /*TODO*///				break;
    /*TODO*///	
    /*TODO*///			case PNG_CN_tRNS:
    /*TODO*///				p->num_trans=chunk_length;
    /*TODO*///				p->trans=chunk_data;
    /*TODO*///				logerror("%i transparent palette entries\n", p->num_trans);
    /*TODO*///				break;
    /*TODO*///	
    /*TODO*///			case PNG_CN_IDAT:
    /*TODO*///				pidat->data = chunk_data;
    /*TODO*///				pidat->length = chunk_length;
    /*TODO*///				if ((pidat->next = malloc (sizeof(struct idat))) == 0)
    /*TODO*///					return 0;
    /*TODO*///				pidat = pidat->next;
    /*TODO*///				pidat->next = 0;
    /*TODO*///				p->zlength += chunk_length;
    /*TODO*///				break;
    /*TODO*///	
    /*TODO*///			case PNG_CN_tEXt:
    /*TODO*///				{
    /*TODO*///					char *text = (char *)chunk_data;
    /*TODO*///	
    /*TODO*///					while(*text++) ;
    /*TODO*///					chunk_data[chunk_length]=0;
    /*TODO*///	 				logerror("Keyword: %s\n", chunk_data);
    /*TODO*///					logerror("Text: %s\n", text);
    /*TODO*///				}
    /*TODO*///				free(chunk_data);
    /*TODO*///				break;
    /*TODO*///	
    /*TODO*///			case PNG_CN_tIME:
    /*TODO*///				{
    /*TODO*///					UINT8 *t=chunk_data;
    /*TODO*///					logerror("Image last-modification time: %i/%i/%i (%i:%i:%i) GMT\n",
    /*TODO*///						((short)(*t) << 8)+ (short)(*(t+1)), *(t+2), *(t+3), *(t+4), *(t+5), *(t+6));
    /*TODO*///				}
    /*TODO*///	
    /*TODO*///				free(chunk_data);
    /*TODO*///				break;
    /*TODO*///	
    /*TODO*///			case PNG_CN_gAMA:
    /*TODO*///				p->source_gamma	 = convert_from_network_order(chunk_data)/100000.0;
    /*TODO*///				logerror( "Source gamma: %f\n",p->source_gamma);
    /*TODO*///	
    /*TODO*///				free(chunk_data);
    /*TODO*///				break;
    /*TODO*///	
    /*TODO*///			case PNG_CN_pHYs:
    /*TODO*///				p->xres = convert_from_network_order(chunk_data);
    /*TODO*///				p->yres = convert_from_network_order(chunk_data+4);
    /*TODO*///				p->resolution_unit = *(chunk_data+8);
    /*TODO*///				logerror("Pixel per unit, X axis: %i\n",p->xres);
    /*TODO*///				logerror("Pixel per unit, Y axis: %i\n",p->yres);
    /*TODO*///				if (p->resolution_unit)
    /*TODO*///					logerror("Unit is meter\n");
    /*TODO*///				else
    /*TODO*///					logerror("Unit is unknown\n");
    /*TODO*///				free(chunk_data);
    /*TODO*///				break;
    /*TODO*///	
    /*TODO*///			case PNG_CN_IEND:
    /*TODO*///				break;
    /*TODO*///	
    /*TODO*///			default:
    /*TODO*///				if (chunk_type & 0x20000000)
    /*TODO*///					logerror("Ignoring ancillary chunk %s\n",str_chunk_type);
    /*TODO*///				else
    /*TODO*///					logerror("Ignoring critical chunk %s!\n",str_chunk_type);
    /*TODO*///				if (chunk_data)
    /*TODO*///					free(chunk_data);
    /*TODO*///				break;
    /*TODO*///			}
    /*TODO*///		}
    /*TODO*///		if ((p->zimage = (UINT8 *)malloc(p->zlength))==NULL)
    /*TODO*///		{
    /*TODO*///			logerror("Out of memory\n");
    /*TODO*///			return 0;
    /*TODO*///		}
    /*TODO*///	
    /*TODO*///		/* combine idat chunks to compressed image data */
    /*TODO*///		temp = p->zimage;
    /*TODO*///		while (ihead->next)
    /*TODO*///		{
    /*TODO*///			pidat = ihead;
    /*TODO*///			memcpy (temp, pidat->data, pidat->length);
    /*TODO*///			free (pidat->data);
    /*TODO*///			temp += pidat->length;
    /*TODO*///			ihead = pidat->next;
    /*TODO*///			free (pidat);
    /*TODO*///		}
    /*TODO*///		p->bpp = (samples[p->color_type] * p->bit_depth) / 8;
    /*TODO*///		p->rowbytes = ceil((p->width * p->bit_depth * samples[p->color_type]) / 8.0);
    /*TODO*///	
    /*TODO*///		if (png_inflate_image(p)==0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		if(png_unfilter (p)==0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	int png_read_info(void *fp, struct png_info *p)
    /*TODO*///	{
    /*TODO*///		UINT32 chunk_length, chunk_type=0, chunk_crc, crc;
    /*TODO*///		UINT8 *chunk_data;
    /*TODO*///		UINT8 str_chunk_type[5], v[4];
    /*TODO*///		int res = 0;
    /*TODO*///	
    /*TODO*///		if (png_verify_signature(fp)==0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		while (chunk_type != PNG_CN_IEND)
    /*TODO*///		{
    /*TODO*///			if (osd_fread(fp, v, 4) != 4)
    /*TODO*///				logerror("Unexpected EOF in PNG\n");
    /*TODO*///			chunk_length=convert_from_network_order(v);
    /*TODO*///	
    /*TODO*///			if (osd_fread(fp, str_chunk_type, 4) != 4)
    /*TODO*///				logerror("Unexpected EOF in PNG file\n");
    /*TODO*///	
    /*TODO*///			str_chunk_type[4]=0; /* terminate string */
    /*TODO*///	
    /*TODO*///			crc=crc32(0,str_chunk_type, 4);
    /*TODO*///			chunk_type = convert_from_network_order(str_chunk_type);
    /*TODO*///	
    /*TODO*///			if (chunk_length)
    /*TODO*///			{
    /*TODO*///				if ((chunk_data = (UINT8 *)malloc(chunk_length+1))==NULL)
    /*TODO*///				{
    /*TODO*///					logerror("Out of memory\n");
    /*TODO*///					return 0;
    /*TODO*///				}
    /*TODO*///				if (osd_fread (fp, chunk_data, chunk_length) != chunk_length)
    /*TODO*///				{
    /*TODO*///					logerror("Unexpected EOF in PNG file\n");
    /*TODO*///					free(chunk_data);
    /*TODO*///					return 0;
    /*TODO*///				}
    /*TODO*///	
    /*TODO*///				crc=crc32(crc,chunk_data, chunk_length);
    /*TODO*///			}
    /*TODO*///			else
    /*TODO*///				chunk_data = NULL;
    /*TODO*///	
    /*TODO*///			if (osd_fread(fp, v, 4) != 4)
    /*TODO*///				logerror("Unexpected EOF in PNG\n");
    /*TODO*///			chunk_crc=convert_from_network_order(v);
    /*TODO*///	
    /*TODO*///			if (crc != chunk_crc)
    /*TODO*///			{
    /*TODO*///				logerror("CRC check failed while reading PNG chunk %s\n",str_chunk_type);
    /*TODO*///				logerror("Found: %08X  Expected: %08X\n",crc,chunk_crc);
    /*TODO*///				return 0;
    /*TODO*///			}
    /*TODO*///	
    /*TODO*///			logerror("Reading PNG chunk %s\n", str_chunk_type);
    /*TODO*///	
    /*TODO*///			switch (chunk_type)
    /*TODO*///			{
    /*TODO*///			case PNG_CN_IHDR:
    /*TODO*///				p->width = convert_from_network_order(chunk_data);
    /*TODO*///				p->height = convert_from_network_order(chunk_data+4);
    /*TODO*///				p->bit_depth = *(chunk_data+8);
    /*TODO*///				p->color_type = *(chunk_data+9);
    /*TODO*///				p->compression_method = *(chunk_data+10);
    /*TODO*///				p->filter_method = *(chunk_data+11);
    /*TODO*///				p->interlace_method = *(chunk_data+12);
    /*TODO*///				free (chunk_data);
    /*TODO*///	
    /*TODO*///				logerror("PNG IHDR information:\n");
    /*TODO*///				logerror("Width: %i, Height: %i\n", p->width, p->height);
    /*TODO*///				logerror("Bit depth %i, color type: %i\n", p->bit_depth, p->color_type);
    /*TODO*///				logerror("Compression method: %i, filter: %i, interlace: %i\n",
    /*TODO*///						p->compression_method, p->filter_method, p->interlace_method);
    /*TODO*///				break;
    /*TODO*///	
    /*TODO*///			case PNG_CN_tEXt:
    /*TODO*///				{
    /*TODO*///					char *text = (char *)chunk_data;
    /*TODO*///					int c;
    /*TODO*///	
    /*TODO*///					while(*text++) ;
    /*TODO*///					chunk_data[chunk_length]=0;
    /*TODO*///					if (strcmp ((const char *)chunk_data, "Screen") == 0)
    /*TODO*///					{
    /*TODO*///						c = sscanf (text, "%i%i%i%i", &p->screen.min_x, &p->screen.max_x,
    /*TODO*///									&p->screen.min_y, &p->screen.max_y);
    /*TODO*///						if (c == 4)
    /*TODO*///						{
    /*TODO*///							res = 1;
    /*TODO*///							logerror("Screen location found at %i, %i, %i, %i\n",
    /*TODO*///									 p->screen.min_x, p->screen.max_x,
    /*TODO*///									 p->screen.min_y, p->screen.max_y);
    /*TODO*///						}
    /*TODO*///						else
    /*TODO*///							logerror("Invalid %s value %s\n", chunk_data, text);
    /*TODO*///					}
    /*TODO*///				}
    /*TODO*///				free(chunk_data);
    /*TODO*///				break;
    /*TODO*///	
    /*TODO*///			default:
    /*TODO*///				if (chunk_data)
    /*TODO*///					free(chunk_data);
    /*TODO*///				break;
    /*TODO*///			}
    /*TODO*///		}
    /*TODO*///		return res;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	/*	Expands a p->image from p->bit_depth to 8 bit */
    /*TODO*///	int png_expand_buffer_8bit (struct png_info *p)
    /*TODO*///	{
    /*TODO*///		int i,j, k;
    /*TODO*///		UINT8 *inp, *outp, *outbuf;
    /*TODO*///	
    /*TODO*///		if (p->bit_depth < 8)
    /*TODO*///		{
    /*TODO*///			if ((outbuf = (UINT8 *)malloc(p->width*p->height))==NULL)
    /*TODO*///			{
    /*TODO*///				logerror("Out of memory\n");
    /*TODO*///				return 0;
    /*TODO*///			}
    /*TODO*///	
    /*TODO*///			inp = p->image;
    /*TODO*///			outp = outbuf;
    /*TODO*///	
    /*TODO*///			for (i = 0; i < p->height; i++)
    /*TODO*///			{
    /*TODO*///				for(j = 0; j < p->width / ( 8 / p->bit_depth); j++)
    /*TODO*///				{
    /*TODO*///					for (k = 8 / p->bit_depth-1; k >= 0; k--)
    /*TODO*///						*outp++ = (*inp >> k * p->bit_depth) & (0xff >> (8 - p->bit_depth));
    /*TODO*///					inp++;
    /*TODO*///				}
    /*TODO*///				if (p->width % (8 / p->bit_depth))
    /*TODO*///				{
    /*TODO*///					for (k = p->width % (8 / p->bit_depth)-1; k >= 0; k--)
    /*TODO*///						*outp++ = (*inp >> k * p->bit_depth) & (0xff >> (8 - p->bit_depth));
    /*TODO*///					inp++;
    /*TODO*///				}
    /*TODO*///			}
    /*TODO*///			free (p->image);
    /*TODO*///			p->image = outbuf;
    /*TODO*///		}
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	void png_delete_unused_colors (struct png_info *p)
    /*TODO*///	{
    /*TODO*///		int i, tab[256], pen=0, trns=0;
    /*TODO*///		UINT8 ptemp[3*256], ttemp[256];
    /*TODO*///	
    /*TODO*///		memset (tab, 0, 256*sizeof(int));
    /*TODO*///		memcpy (ptemp, p->palette, 3*p->num_palette);
    /*TODO*///		memcpy (ttemp, p->trans, p->num_trans);
    /*TODO*///	
    /*TODO*///		/* check which colors are actually used */
    /*TODO*///		for (i = 0; i < p->height*p->width; i++)
    /*TODO*///			tab[p->image[i]]++;
    /*TODO*///	
    /*TODO*///		/* shrink palette and transparency */
    /*TODO*///		for (i = 0; i < p->num_palette; i++)
    /*TODO*///			if (tab[i])
    /*TODO*///			{
    /*TODO*///				p->palette[3*pen+0]=ptemp[3*i+0];
    /*TODO*///				p->palette[3*pen+1]=ptemp[3*i+1];
    /*TODO*///				p->palette[3*pen+2]=ptemp[3*i+2];
    /*TODO*///				if (i < p->num_trans)
    /*TODO*///				{
    /*TODO*///					p->trans[pen] = ttemp[i];
    /*TODO*///					trns++;
    /*TODO*///				}
    /*TODO*///				tab[i] = pen++;
    /*TODO*///			}
    /*TODO*///	
    /*TODO*///		/* remap colors */
    /*TODO*///		for (i = 0; i < p->height*p->width; i++)
    /*TODO*///			p->image[i]=tab[p->image[i]];
    /*TODO*///	
    /*TODO*///		if (p->num_palette!=pen)
    /*TODO*///			logerror("%i unused pen(s) deleted\n", p->num_palette-pen);
    /*TODO*///	
    /*TODO*///		p->num_palette = pen;
    /*TODO*///		p->num_trans = trns;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	/********************************************************************************
    /*TODO*///	
    /*TODO*///	  PNG write functions
    /*TODO*///	
    /*TODO*///	********************************************************************************/
    /*TODO*///	
    /*TODO*///	struct png_text
    /*TODO*///	{
    /*TODO*///		char *data;
    /*TODO*///		int length;
    /*TODO*///		struct png_text *next;
    /*TODO*///	};
    /*TODO*///	
    /*TODO*///	static struct png_text *png_text_list = 0;
    /*TODO*///	
    /*TODO*///	static void convert_to_network_order (UINT32 i, UINT8 *v)
    /*TODO*///	{
    /*TODO*///		v[0]=i>>24;
    /*TODO*///		v[1]=(i>>16)&0xff;
    /*TODO*///		v[2]=(i>>8)&0xff;
    /*TODO*///		v[3]=i&0xff;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	int png_add_text (const char *keyword, const char *text)
    /*TODO*///	{
    /*TODO*///		struct png_text *pt;
    /*TODO*///	
    /*TODO*///		pt = malloc (sizeof(struct png_text));
    /*TODO*///		if (pt == 0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		pt->length = strlen(keyword) + strlen(text) + 1;
    /*TODO*///		pt->data = malloc (pt->length + 1);
    /*TODO*///		if (pt->data == 0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		strcpy (pt->data, keyword);
    /*TODO*///		strcpy (pt->data + strlen(keyword) + 1, text);
    /*TODO*///		pt->next = png_text_list;
    /*TODO*///		png_text_list = pt;
    /*TODO*///	
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	static int write_chunk(void *fp, UINT32 chunk_type, UINT8 *chunk_data, UINT32 chunk_length)
    /*TODO*///	{
    /*TODO*///		UINT32 crc;
    /*TODO*///		UINT8 v[4];
    /*TODO*///		int written;
    /*TODO*///	
    /*TODO*///		/* write length */
    /*TODO*///		convert_to_network_order(chunk_length, v);
    /*TODO*///		written = osd_fwrite(fp, v, 4);
    /*TODO*///	
    /*TODO*///		/* write type */
    /*TODO*///		convert_to_network_order(chunk_type, v);
    /*TODO*///		written += osd_fwrite(fp, v, 4);
    /*TODO*///	
    /*TODO*///		/* calculate crc */
    /*TODO*///		crc=crc32(0, v, 4);
    /*TODO*///		if (chunk_length > 0)
    /*TODO*///		{
    /*TODO*///			/* write data */
    /*TODO*///			written += osd_fwrite(fp, chunk_data, chunk_length);
    /*TODO*///			crc=crc32(crc, chunk_data, chunk_length);
    /*TODO*///		}
    /*TODO*///		convert_to_network_order(crc, v);
    /*TODO*///	
    /*TODO*///		/* write crc */
    /*TODO*///		written += osd_fwrite(fp, v, 4);
    /*TODO*///	
    /*TODO*///		if (written != 3*4+chunk_length)
    /*TODO*///		{
    /*TODO*///			logerror("Chunk write failed\n");
    /*TODO*///			return 0;
    /*TODO*///		}
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	int png_write_sig(void *fp)
    /*TODO*///	{
    /*TODO*///		/* PNG Signature */
    /*TODO*///		if (osd_fwrite(fp, PNG_Signature, 8) != 8)
    /*TODO*///		{
    /*TODO*///			logerror("PNG sig write failed\n");
    /*TODO*///			return 0;
    /*TODO*///		}
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	int png_write_datastream(void *fp, struct png_info *p)
    /*TODO*///	{
    /*TODO*///		UINT8 ihdr[13];
    /*TODO*///		struct png_text *pt;
    /*TODO*///	
    /*TODO*///		/* IHDR */
    /*TODO*///		convert_to_network_order(p->width, ihdr);
    /*TODO*///		convert_to_network_order(p->height, ihdr+4);
    /*TODO*///		*(ihdr+8) = p->bit_depth;
    /*TODO*///		*(ihdr+9) = p->color_type;
    /*TODO*///		*(ihdr+10) = p->compression_method;
    /*TODO*///		*(ihdr+11) = p->filter_method;
    /*TODO*///		*(ihdr+12) = p->interlace_method;
    /*TODO*///		logerror("Type(%d) Color Depth(%d)\n", p->color_type,p->bit_depth);
    /*TODO*///		if (write_chunk(fp, PNG_CN_IHDR, ihdr, 13)==0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		/* PLTE */
    /*TODO*///		if (p->num_palette > 0)
    /*TODO*///			if (write_chunk(fp, PNG_CN_PLTE, p->palette, p->num_palette*3)==0)
    /*TODO*///				return 0;
    /*TODO*///	
    /*TODO*///		/* IDAT */
    /*TODO*///		if (write_chunk(fp, PNG_CN_IDAT, p->zimage, p->zlength)==0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		/* tEXt */
    /*TODO*///		while (png_text_list)
    /*TODO*///		{
    /*TODO*///			pt = png_text_list;
    /*TODO*///			if (write_chunk(fp, PNG_CN_tEXt, (UINT8 *)pt->data, pt->length)==0)
    /*TODO*///				return 0;
    /*TODO*///			free (pt->data);
    /*TODO*///	
    /*TODO*///			png_text_list = pt->next;
    /*TODO*///			free (pt);
    /*TODO*///		}
    /*TODO*///	
    /*TODO*///		/* IEND */
    /*TODO*///		if (write_chunk(fp, PNG_CN_IEND, NULL, 0)==0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	int png_filter(struct png_info *p)
    /*TODO*///	{
    /*TODO*///		int i;
    /*TODO*///		UINT8 *src, *dst;
    /*TODO*///	
    /*TODO*///		if((p->fimage = (UINT8 *)malloc (p->height*(p->rowbytes+1)))==NULL)
    /*TODO*///		{
    /*TODO*///			logerror("Out of memory\n");
    /*TODO*///			return 0;
    /*TODO*///		}
    /*TODO*///	
    /*TODO*///		dst = p->fimage;
    /*TODO*///		src = p->image;
    /*TODO*///	
    /*TODO*///		for (i=0; i<p->height; i++)
    /*TODO*///		{
    /*TODO*///			*dst++ = 0; /* No filter */
    /*TODO*///			memcpy (dst, src, p->rowbytes);
    /*TODO*///			src += p->rowbytes;
    /*TODO*///			dst += p->rowbytes;
    /*TODO*///		}
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	int png_deflate_image(struct png_info *p)
    /*TODO*///	{
    /*TODO*///		unsigned long zbuff_size;
    /*TODO*///	
    /*TODO*///		zbuff_size = (p->height*(p->rowbytes+1))*1.1+12;
    /*TODO*///	
    /*TODO*///		if((p->zimage = (UINT8 *)malloc (zbuff_size))==NULL)
    /*TODO*///		{
    /*TODO*///			logerror("Out of memory\n");
    /*TODO*///			return 0;
    /*TODO*///		}
    /*TODO*///	
    /*TODO*///		if (compress(p->zimage, &zbuff_size, p->fimage, p->height*(p->rowbytes+1)) != Z_OK)
    /*TODO*///		{
    /*TODO*///			logerror("Error while deflating image\n");
    /*TODO*///			return 0;
    /*TODO*///		}
    /*TODO*///		p->zlength = zbuff_size;
    /*TODO*///	
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	static int png_pack_buffer (struct png_info *p)
    /*TODO*///	{
    /*TODO*///		UINT8 *outp, *inp;
    /*TODO*///		int i,j,k;
    /*TODO*///	
    /*TODO*///		outp = inp = p->image;
    /*TODO*///	
    /*TODO*///		if (p->bit_depth < 8)
    /*TODO*///		{
    /*TODO*///			for (i=0; i<p->height; i++)
    /*TODO*///			{
    /*TODO*///				for(j=0; j<p->width/(8/p->bit_depth); j++)
    /*TODO*///				{
    /*TODO*///					for (k=8/p->bit_depth-1; k>=0; k--)
    /*TODO*///						*outp |= *inp++ << k * p->bit_depth;
    /*TODO*///					outp++;
    /*TODO*///					*outp = 0;
    /*TODO*///				}
    /*TODO*///				if (p->width % (8/p->bit_depth))
    /*TODO*///				{
    /*TODO*///					for (k=p->width%(8/p->bit_depth)-1; k>=0; k--)
    /*TODO*///						*outp |= *inp++ << k * p->bit_depth;
    /*TODO*///					outp++;
    /*TODO*///					*outp = 0;
    /*TODO*///				}
    /*TODO*///			}
    /*TODO*///		}
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	
    /*TODO*///	/*********************************************************************
    /*TODO*///	
    /*TODO*///	  Writes an mame_bitmap in a PNG file. If the depth of the bitmap
    /*TODO*///	  is 8, a color type 3 PNG with palette is written. Otherwise a
    /*TODO*///	  color type 2 true color RGB PNG is written.
    /*TODO*///	
    /*TODO*///	 *********************************************************************/
    /*TODO*///	
    /*TODO*///	static int png_create_datastream(void *fp, struct mame_bitmap *bitmap)
    /*TODO*///	{
    /*TODO*///		int i, j;
    /*TODO*///		int r, g, b;
    /*TODO*///		UINT32 color;
    /*TODO*///		UINT8 *ip;
    /*TODO*///		struct png_info p;
    /*TODO*///	
    /*TODO*///		memset (&p, 0, sizeof (struct png_info));
    /*TODO*///		p.xscale = p.yscale = p.source_gamma = 0.0;
    /*TODO*///		p.palette = p.trans = p.image = p.zimage = p.fimage = NULL;
    /*TODO*///		p.width = bitmap->width;
    /*TODO*///		p.height = bitmap->height;
    /*TODO*///	
    /*TODO*///		if ((Machine->color_depth == 16) && (Machine->drv->total_colors <= 256))
    /*TODO*///		{
    /*TODO*///			p.color_type = 3;
    /*TODO*///			if((p.palette = (UINT8 *)malloc (3*256))==NULL)
    /*TODO*///			{
    /*TODO*///				logerror("Out of memory\n");
    /*TODO*///				return 0;
    /*TODO*///			}
    /*TODO*///			memset (p.palette, 0, 3*256);
    /*TODO*///			/* get palette */
    /*TODO*///			for (i = 0; i < Machine->drv->total_colors; i++)
    /*TODO*///				palette_get_color(i,&p.palette[3*i],&p.palette[3*i+1],&p.palette[3*i+2]);
    /*TODO*///	
    /*TODO*///			p.num_palette = 256;
    /*TODO*///			if((p.image = (UINT8 *)malloc (p.height*p.width))==NULL)
    /*TODO*///			{
    /*TODO*///				logerror("Out of memory\n");
    /*TODO*///				return 0;
    /*TODO*///			}
    /*TODO*///	
    /*TODO*///			for (i = 0; i < p.height; i++)
    /*TODO*///				for (j = 0; j < p.width; j++)
    /*TODO*///					p.image[i * p.width + j] = ((UINT16 *)bitmap->line[i])[j];
    /*TODO*///	
    /*TODO*///			png_delete_unused_colors (&p);
    /*TODO*///			p.bit_depth = p.num_palette > 16 ? 8 : p.num_palette > 4 ? 4 : p.num_palette > 2 ? 2 : 1;
    /*TODO*///			p.rowbytes=ceil((p.width*p.bit_depth)/8.0);
    /*TODO*///			if (png_pack_buffer (&p) == 0)
    /*TODO*///				return 0;
    /*TODO*///	
    /*TODO*///		}
    /*TODO*///		else
    /*TODO*///		{
    /*TODO*///			p.color_type = 2;
    /*TODO*///			p.rowbytes = p.width * 3;
    /*TODO*///			p.bit_depth = 8;
    /*TODO*///			if((p.image = (UINT8 *)malloc (p.height * p.rowbytes))==NULL)
    /*TODO*///			{
    /*TODO*///				logerror("Out of memory\n");
    /*TODO*///				return 0;
    /*TODO*///			}
    /*TODO*///	
    /*TODO*///			ip = p.image;
    /*TODO*///	
    /*TODO*///			switch (Machine->color_depth)
    /*TODO*///			{
    /*TODO*///			case 16: /* 16BIT */
    /*TODO*///				for (i = 0; i < p.height; i++)
    /*TODO*///					for (j = 0; j < p.width; j++)
    /*TODO*///					{
    /*TODO*///						palette_get_color(((UINT16 *)bitmap->line[i])[j],ip, ip+1, ip+2);
    /*TODO*///						ip += 3;
    /*TODO*///					}
    /*TODO*///				break;
    /*TODO*///			case 15: /* DIRECT_15BIT */
    /*TODO*///				for (i = 0; i < p.height; i++)
    /*TODO*///					for (j = 0; j < p.width; j++)
    /*TODO*///					{
    /*TODO*///						color = ((UINT16 *)bitmap->line[i])[j];
    /*TODO*///	
    /*TODO*///						r = (color & direct_rgb_components[0]) / (direct_rgb_components[0] / 0x1f);
    /*TODO*///						g = (color & direct_rgb_components[1]) / (direct_rgb_components[1] / 0x1f);
    /*TODO*///						b = (color & direct_rgb_components[2]) / (direct_rgb_components[2] / 0x1f);
    /*TODO*///	
    /*TODO*///						*ip++ = (r << 3) | (r >> 2);
    /*TODO*///						*ip++ = (g << 3) | (g >> 2);
    /*TODO*///						*ip++ = (b << 3) | (b >> 2);
    /*TODO*///					}
    /*TODO*///				break;
    /*TODO*///			case 32: /* DIRECT_32BIT */
    /*TODO*///				for (i = 0; i < p.height; i++)
    /*TODO*///					for (j = 0; j < p.width; j++)
    /*TODO*///					{
    /*TODO*///						color = ((UINT32 *)bitmap->line[i])[j];
    /*TODO*///	
    /*TODO*///						r = (color & direct_rgb_components[0]) / (direct_rgb_components[0] / 0xff);
    /*TODO*///						g = (color & direct_rgb_components[1]) / (direct_rgb_components[1] / 0xff);
    /*TODO*///						b = (color & direct_rgb_components[2]) / (direct_rgb_components[2] / 0xff);
    /*TODO*///	
    /*TODO*///						*ip++ = r;
    /*TODO*///						*ip++ = g;
    /*TODO*///						*ip++ = b;
    /*TODO*///					}
    /*TODO*///				break;
    /*TODO*///			default:
    /*TODO*///				logerror("Unknown color depth\n");
    /*TODO*///				break;
    /*TODO*///			}
    /*TODO*///		}
    /*TODO*///		if(png_filter (&p)==0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		if (png_deflate_image(&p)==0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		if (png_write_datastream(fp, &p)==0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		if (p.palette) free (p.palette);
    /*TODO*///		if (p.image) free (p.image);
    /*TODO*///		if (p.zimage) free (p.zimage);
    /*TODO*///		if (p.fimage) free (p.fimage);
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	int png_write_bitmap(void *fp, struct mame_bitmap *bitmap)
    /*TODO*///	{
    /*TODO*///		char text[1024];
    /*TODO*///	
    /*TODO*///	#ifdef MESS
    /*TODO*///		sprintf (text, "MESS %s", build_version);
    /*TODO*///	#else
    /*TODO*///		sprintf (text, "MAME %s", build_version);
    /*TODO*///	#endif
    /*TODO*///		png_add_text("Software", text);
    /*TODO*///		sprintf (text, "%s %s", Machine->gamedrv->manufacturer, Machine->gamedrv->description);
    /*TODO*///		png_add_text("System", text);
    /*TODO*///	
    /*TODO*///		if(png_write_sig(fp) == 0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		if(png_create_datastream(fp, bitmap) == 0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	/********************************************************************************
    /*TODO*///	
    /*TODO*///	  MNG write functions
    /*TODO*///	
    /*TODO*///	********************************************************************************/
    /*TODO*///	
    /*TODO*///	static int mng_status;
    /*TODO*///	
    /*TODO*///	int mng_capture_start(void *fp, struct mame_bitmap *bitmap)
    /*TODO*///	{
    /*TODO*///		UINT8 mhdr[28];
    /*TODO*///	/*	UINT8 term; */
    /*TODO*///	
    /*TODO*///		if (osd_fwrite(fp, MNG_Signature, 8) != 8)
    /*TODO*///		{
    /*TODO*///			logerror("MNG sig write failed\n");
    /*TODO*///			return 0;
    /*TODO*///		}
    /*TODO*///	
    /*TODO*///		memset (mhdr, 0, 28);
    /*TODO*///		convert_to_network_order(bitmap->width, mhdr);
    /*TODO*///		convert_to_network_order(bitmap->height, mhdr+4);
    /*TODO*///		convert_to_network_order(Machine->drv->frames_per_second, mhdr+8);
    /*TODO*///		convert_to_network_order(0x0041, mhdr+24); /* Simplicity profile */
    /*TODO*///		/* frame count and play time unspecified because
    /*TODO*///		   we don't know at this stage */
    /*TODO*///		if (write_chunk(fp, MNG_CN_MHDR, mhdr, 28)==0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///	/*	term = 0x03;    loop sequence    */
    /*TODO*///	/*	if (write_chunk(fp, MNG_CN_TERM, &term, 1)==0) */
    /*TODO*///	/*		return 0; */
    /*TODO*///	
    /*TODO*///		mng_status = 1;
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	int mng_capture_frame(void *fp, struct mame_bitmap *bitmap)
    /*TODO*///	{
    /*TODO*///		if (mng_status)
    /*TODO*///		{
    /*TODO*///			if(png_create_datastream(fp, bitmap) == 0)
    /*TODO*///				return 0;
    /*TODO*///			return 1;
    /*TODO*///		}
    /*TODO*///		else
    /*TODO*///		{
    /*TODO*///			logerror("MNG recording not running\n");
    /*TODO*///			return 0;
    /*TODO*///		}
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	int mng_capture_stop(void *fp)
    /*TODO*///	{
    /*TODO*///		if (write_chunk(fp, MNG_CN_MEND, NULL, 0)==0)
    /*TODO*///			return 0;
    /*TODO*///	
    /*TODO*///		mng_status = 0;
    /*TODO*///		return 1;
    /*TODO*///	}
    /*TODO*///	
    /*TODO*///	int mng_capture_status(void)
    /*TODO*///	{
    /*TODO*///		return mng_status;
    /*TODO*///	}
	
}
