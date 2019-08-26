/*
 This file is part of Arcadeflex.

 Arcadeflex is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Arcadeflex is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Arcadeflex.  If not, see <http://www.gnu.org/licenses/>.
 */
package arcadeflex036;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static common.ptr.*;
import java.util.Scanner;
/**
 *
 * @author shadow
 */
public class libc_old {


    /*
     *  function convert string to integer
     */

    public static int atoi(String str) {
        return Integer.parseInt(str);
    }
    /*
     *   return next random number
     */



    /*
     *   getcharacter
     */

    public static void getchar() {
        try {
            System.in.read();
        } catch (Exception e) {
        }
    }

    /*
     *  Compare c relative functions
     *
     *
     *
     */

    /*
     *   Compare 2 Strings
     */


    /**
     * Compares array and string without sensitivity to case
     *
     * @param array
     * @param String2
     * @return a negative integer, zero, or a positive integer as the specified
     * String is greater than, equal to, or less than this String, ignoring case
     * considerations.
     */
    public static int stricmp(char[] array, String str2) {
        String str = makeString(array);
        return str.compareToIgnoreCase(str2);
    }

    /**
     * Compares string1 and string2 without sensitivity to case
     *
     * @param array1
     * @param array2
     * @return a negative integer, zero, or a positive integer as the specified
     * String is greater than, equal to, or less than this String, ignoring case
     * considerations.
     */
    public static int stricmp(char[] array1, char[] array2) {
        String str1 = makeString(array1);
        String str2 = makeString(array2);
        return str1.compareToIgnoreCase(str2);
    }

    /**
     * Copy characters from string
     *
     * @param dst - destination
     * @param src - source
     * @param size - number of character to copy
     */
    public static void strncpy(char[] dst, String src, int size) {
        if (src.length() > 0) {
            for (int i = 0; i < size; i++) {
                dst[i] = src.charAt(i);
            }
        }
    }
    /*
     *   copy String to array
     */

    public static void strcpy(char[] dst, String src) {
        for (int i = 0; i < src.length(); i++) {
            dst[i] = src.charAt(i);

        }
    }

    /*
     *   measure a String
     */

    public static int strlen(char[] ch) {
        int size = 0;
        for (int i = 0; i < ch.length; i++) {
            if (ch[i] == 0) {
                break;
            }
            size++;
        }
        return size;
    }
    

    /*
     *
     *    Create a String from an Array
     *
     */
    public static String makeString(char[] array) {
        int i = 0;
        for (i = 0; i < array.length; i++) {
            if (array[i] == 0) {
                break;
            }
        }
        return new String(array, 0, i);
    }


    /**
     * ***********************************
     *
     * Int Pointer Emulation
     ************************************
     */
    public static class IntPtr {

        public IntPtr() {
        }

        public IntPtr(char[] m) {
            set(m, 0);
        }

        public IntPtr(int size) {
            memory = new char[size];
            base = 0;
        }

        public IntPtr(IntPtr cp, int b) {
            set(cp.memory, cp.base + b);
        }

        public IntPtr(UBytePtr p) {
            set(p.memory, p.offset);
        }
        public IntPtr(UBytePtr p,int b) {
            set(p.memory, p.offset+b);
        }

        public void set(char[] input, int b) {
            base = b;
            memory = input;
        }

        public void inc() {
            base += 4;
        }

        public void dec() {
            base -= 4;
        }

        public int read(int offset) {
            int myNumber = (((int) memory[base + offset]) << 0)
                    | (((int) memory[base + offset + 1]) << 8)
                    | (((int) memory[base + offset + 2]) << 16)
                    | (((int) memory[base + offset + 3]) << 24);
            return myNumber;
        }

        public int read() {
            int myNumber = (((int) memory[base]) << 0)
                    | (((int) memory[base + 1]) << 8)
                    | (((int) memory[base + 2]) << 16)
                    | (((int) memory[base + 3]) << 24);
            return myNumber;
        }

        public void or(int value) {
            int tempbase = this.base;
            char[] tempmemory = this.memory;
            tempmemory[tempbase] = (char) (tempmemory[tempbase] | (char) value);
        }

        public char[] readCA() {
            return memory;
        }

        public int getBase() {
            return base;
        }
        public int base;
        public char[] memory;
    }

}
