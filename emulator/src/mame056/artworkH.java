/*********************************************************************

  artwork.h

  Generic backdrop/overlay functions.

  Be sure to include driver.h before including this file.

  Created by Mike Balfour - 10/01/1998
*********************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056;

import static common.subArrays.*;
import static mame056.commonH.*;
import static mame056.drawgfxH.*;

public class artworkH {
    /*********************************************************************
    artwork

    This structure is a generic structure used to hold both backdrops
    and overlays.
    *********************************************************************/
  

    public static class artwork_info
    {
            public mame_bitmap artwork;
            public mame_bitmap artwork1;
            public mame_bitmap alpha;
            public mame_bitmap orig_artwork;   /* needed for palette recalcs */
            public int start_pen;
            public IntArray rgb;
    };
    
    public static class artwork_color
    {
        public int red,green,blue;
        public int alpha;   /* 0x00-0xff or OVERLAY_DEFAULT_OPACITY */
        
        public artwork_color(int red, int green, int blue, int alpha){
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }
    }


    public static class artwork_element
    {
            public String name=null;
            public rectangle box = new rectangle();
            public int red,green,blue;
            public int alpha;   /* 0x00-0xff or OVERLAY_DEFAULT_OPACITY */
            
            public artwork_element(String name){
                this.name = name;
            }
            
            public artwork_element(rectangle box, int red, int green, int blue, int alpha){
                this.box = box;
                this.red = red;
                this.green = green;
                this.blue = blue;
                this.alpha = alpha;
            }
            
            public artwork_element(rectangle box, artwork_color art_color){
                this.box = box;
                this.red = art_color.red;
                this.green = art_color.green;
                this.blue = art_color.blue;
                this.alpha = art_color.alpha;
            }
    };

    public static class artwork_size_info
    {
            public int width, height;         /* widht and height of the artwork */
            public rectangle screen = new rectangle();   /* location of the screen relative to the artwork */
    };

    public static int OVERLAY_DEFAULT_OPACITY = 0xffff;

    public static int MIN(int x, int y){
        return ((x)<(y)?(x):(y));
    }
    
    public static int MAX(int x, int y){
        return ((x)>(y)?(x):(y));
    }
    
}
