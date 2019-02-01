/**
 * ported to v0.56
 * ported to v0.37b7
 */
package mame056;

public class usrintrfH {

    public static class DisplayText {

        public DisplayText() {
        }

        public DisplayText(String text, int color, int x, int y) {
            this.text = text;
            this.color = color;
            this.x = x;
            this.y = y;
        }

        public DisplayText(int z) {
            this(null, 0, 0, 0);
        }

        public static DisplayText[] create(int n) {
            DisplayText[] a = new DisplayText[n];
            for (int k = 0; k < n; k++) {
                a[k] = new DisplayText();
            }
            return a;
        }

        public String text;/* 0 marks the end of the array */
        public int color;
        public int x;
        public int y;
    }

    public static final int UI_COLOR_NORMAL = 0;/* white on black text */
    public static final int UI_COLOR_INVERSE = 1;/* black on white text */

    public static final int SEL_BITS = 12;/* main menu selection mask */
    public static final int SEL_BITS2 = 4;/* submenu selection masks */
    public static final int SEL_MASK = ((1 << SEL_BITS) - 1);
    public static final int SEL_MASK2 = ((1 << SEL_BITS2) - 1);
}
