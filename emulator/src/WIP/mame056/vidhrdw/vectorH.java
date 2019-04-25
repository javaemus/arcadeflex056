/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.vidhrdw;

public class vectorH
{
	
	public static String VECTOR_TEAM = 
		"-* Vector Heads *-+n" +
		"Brad Oliver+n" +
		"Aaron Giles+n" +
		"Bernd Wiebelt+n" +
		"Allard van der Bas+n" +
		"Al Kossow (VECSIM)+n" +
		"Hedley Rainnie (VECSIM)+n" +
		"Eric Smith (VECSIM)+n" +
		"Neil Bradley (technical advice)+n" +
		"Andrew Caldwell (anti-aliasing)+n" +
		"- *** -\n";
	
	public static int MAX_POINTS = 5000;	/* Maximum # of points we can queue in a vector list */
	
	public static int MAX_PIXELS = 850000;  /* Maximum of pixels we can remember */
	
	public static int VECTOR_COLOR111(int c){ 
            return  (((c & 1) !=0)? 0x0000ff : 0)
                    |(((c & 2)!=0)? 0x00ff00: 0)
                    |(((c & 4)!=0)? 0xff0000: 0);
        }
	
	public static int VECTOR_COLOR222(int c){
            return(((c) & 3) * 0x55)
                    |(((((c) >> 2) & 3) * 0x55) << 8)
                    |(((((c) >> 4) & 3) * 0x55) << 16);
        }
	
	public static int VECTOR_COLOR444(int c){
            return (((c) & 0xf) * 0x11)
                    |(((((c) >> 4) & 0xf) * 0x11) << 8)
                    |(((((c) >> 8) & 0xf) * 0x11) << 16);
        }
		
};
