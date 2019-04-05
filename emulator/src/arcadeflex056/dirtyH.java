/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcadeflex056;

/**
 *
 * @author chusogar
 */
public class dirtyH {
    public static int MAX_GFX_WIDTH     = 1600;
    public static int MAX_GFX_HEIGHT    = 1600;
    public static int DIRTY_H           = 256;  /* 160 would be enough, but * 256 is faster */
    public static int DIRTY_V           = (MAX_GFX_HEIGHT/16);
    
    public static char[] dirty_new=new char[DIRTY_V * DIRTY_H];
    
    /*TODO*/////typedef char dirtygrid[DIRTY_V * DIRTY_H];
    public static int ISDIRTY(int x,int y) {
        return (dirty_new[(y)/16 * DIRTY_H + (x)/16]);
    }
    
    public static void MARKDIRTY(int x,int y){
        dirty_new[(y)/16 * DIRTY_H + (x)/16] = 1;
    }
}
