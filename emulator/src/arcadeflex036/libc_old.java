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
import common.seekableBAIS;
import java.util.Scanner;
/**
 *
 * @author shadow
 */
public class libc_old {

    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;

    
    /*
     *  function equals to c printf syntax
     */
    public static void printf(String str, Object... arguments) {
        System.out.printf(str, arguments);
    }
 
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
     *
     * FILE functions
     ***********************************
     */
    public static class FILE {

        public seekableBAIS bais;
        public byte[] bytes;

        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
            bais = new seekableBAIS(bytes);
        }

        public FILE() {
            offset = 0;
        }
        public FileOutputStream fos;
        public FileWriter fw;
        public InputStream is;
        public String Name;
        public int offset;
        public char[] buffer;
        Scanner scan;
    }

    public static FILE fopen(char[] name, String format) {
        String nameS = new String(name);
        return fopen(nameS, format);
    }

    public static byte[] getFileBytes(File file) throws IOException {
        ByteArrayOutputStream ous = null;
        InputStream ios = null;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            ios = new FileInputStream(file);
            int read = 0;
            while ((read = ios.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
        } finally {
            try {
                if (ous != null) {
                    ous.close();
                }
            } catch (IOException e) {
                // swallow, since not that important
            }
            try {
                if (ios != null) {
                    ios.close();
                }
            } catch (IOException e) {
                // swallow, since not that important
            }
        }
        return ous.toByteArray();
    }

    public static FILE fopen(String name, String format) {
        FILE file;
        file = new FILE();
        if (format.compareTo("rb") == 0) {
            try {
                //file.raf = new RandomAccessFile(name, "r");
                file.setBytes(getFileBytes(new File(name)));
                file.Name = name;
            } catch (Exception e) {
                file = null;
                return null;
            }
            return file;
        } else if (format.compareTo("wb") == 0) {
            try {
                file.fos = new FileOutputStream(name, false);

            } catch (Exception e) {
                file = null;
                return null;
            }
            return file;
        } else if (format.compareTo("wa") == 0) {
            try {
                file.fw = new FileWriter(name, true);

            } catch (Exception e) {
                file = null;
                return null;
            }
            return file;
        }
        else if (format.compareTo("w") == 0) {
            try {
                file.fw = new FileWriter(name, false);

            } catch (Exception e) {
                file = null;
                return null;
            }
            return file;
        }
        file = null;
        return null;
    }

    public static FILE fopen(byte[] bytes, String name, String format) {
        FILE file;
        file = new FILE();
        if (format.compareTo("rb") == 0) {
            try {
                //file.raf = new RandomAccessFile(name, "r");
                file.setBytes(bytes);
                file.Name = name;
            } catch (Exception e) {
                file = null;
                return null;
            }
            return file;
        } else if (format.compareTo("wb") == 0) {
            try {
                file.fos = new FileOutputStream(name, false);

            } catch (Exception e) {
                file = null;
                return null;
            }
            return file;
        } else if (format.compareTo("wa") == 0) {
            try {
                file.fw = new FileWriter(name, true);

            } catch (Exception e) {
                file = null;
                return null;
            }
            return file;
        }
        else if (format.compareTo("w") == 0) {
            try {
                file.fw = new FileWriter(name, false);

            } catch (Exception e) {
                file = null;
                return null;
            }
            return file;
        }
        file = null;
        return null;
    }

    public static FILE fopen(File fl, String format) {
        FILE file;
        file = new FILE();
        if (format.compareTo("rb") == 0) {
            try {
                //file.raf = new RandomAccessFile(fl, "r");
                file.setBytes(getFileBytes(fl));
                file.Name = fl.getName();
            } catch (Exception e) {
                file = null;
                return null;
            }
            return file;
        } else if (format.compareTo("wb") == 0) {
            try {
                file.fos = new FileOutputStream(fl, false);

            } catch (Exception e) {
                file = null;
                return null;
            }
            return file;
        } else if (format.compareTo("wa") == 0) {
            try {
                file.fw = new FileWriter(fl, true);

            } catch (Exception e) {
                file = null;
                return null;
            }
            return file;
        }
        else if (format.compareTo("w") == 0) {
            try {
                file.fw = new FileWriter(fl, false);

            } catch (Exception e) {
                file = null;
                return null;
            }
            return file;
        }
        file = null;
        return null;
    }

    public static int fread(char[] buf, int offset, int size, int count, FILE file) {
        byte bbuf[] = new byte[size * count];
        int readsize;

        try {
            readsize = file.bais.read(bbuf);
        } catch (Exception e) {
            bbuf = null;
            return -1;
        }

        for (int i = 0; i < readsize; i++) {
            buf[offset + i] = (char) ((bbuf[i] + 256) & 0xFF);
        }
        bbuf = null;
        return readsize;
    }

    public static int fread(char[] buf, int size, int count, FILE file) {
        return fread(buf, 0, size, count, file);
    }
    public static void readAll(FILE file) {
        file.buffer = new char[(int) ftell(file)];
        fread(file.buffer, 0, (int) ftell(file), 1, file);
    }

    public static char fgetc(FILE file) {
        return file.buffer[file.offset++];
    }
    public static void setScanner(FILE f)
    {
        f.scan = new Scanner(f.bais);
    }
    public static String fgets(char[] str, int n, FILE f) {
       // Scanner scan = new Scanner(f.bais); 
        if(f.scan.hasNext())
        {
            String x = f.scan.next();
            //System.out.println(x);
            //str = x.toCharArray();
            char[] la = x.toCharArray();
            int la_lenght = la.length;
            System.arraycopy(x.toCharArray(), 0, str, 0, la_lenght<48?la_lenght:48);
            if(la_lenght<48)
            {
                for(int i=la_lenght; i<48; i++)
                {
                    str[i]=' ';
                }
            }
            //System.out.println(str);
            return x;
        }
        else
        {
            return null;
        }
    }
    public static void fseek(FILE file, int pos, int whence) {
        if (file.bais != null) {
            if (whence == SEEK_SET) {
                file.bais.seek(pos);
            }
            else if(whence == SEEK_CUR)
            {
               file.bais.seek((int)(file.bais.tell()+pos));
            }
            else {
                throw new UnsupportedOperationException("FSEEK other than SEEK_SET,SEEK_CUR NOT SUPPORTED.");
            }
        }
    }

    public static void fwrite(char[] buf, int offset, int size, int count, FILE file) {
        byte bbuf[] = new byte[size * count];

        for (int i = 0; i < size * count; i++) {
            bbuf[i] = (byte) (buf[offset + i] & 0xFF);
        }
        try {
            file.fos.write(bbuf);
        } catch (Exception e) {
            bbuf = null;
            return;
        }

        bbuf = null;
    }

    public static void fwrite(char[] buf, int size, int count, FILE file) {
        fwrite(buf, 0, size, count, file);
    }

    public static void fwrite(char buf, int size, int count, FILE file) {
        byte bbuf[] = new byte[size * count];

        bbuf[0] = (byte) (buf & 0xFF);
        try {
            file.fos.write(bbuf);
        } catch (Exception e) {
            bbuf = null;
            return;
        }

        bbuf = null;
    }

    public static long ftell(FILE file) {
        try {
            return file.bytes.length;
        } catch (Exception e) {
            Logger.getLogger(libc_old.class.getName()).log(Level.SEVERE, null, e);
        }
        return 0;
    }
     public static int feof(FILE file) {
        if (file.offset == file.bytes.length) {
            return 1;
        }
        return 0;
    }

    public static void fprintf(FILE file, String str, Object... arguments) {
        String print = String.format(str, arguments);
        try {
            file.fw.write(print);
        } catch (Exception e) {
        }
    }

    public static void fclose(FILE file) {
        if (file.bais != null) {
            try {
                file.bais.close();
            } catch (Exception e) {
            }

        }
        if (file.is != null) {
            try {
                file.is.close();
            } catch (Exception e) {
            }

        }
        if (file.fos != null) {
            try {
                file.fos.close();
            } catch (Exception e) {
            }

        }
        if (file.fw != null) {
            try {
                file.fw.close();
            } catch (Exception e) {
            }

        }
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
        char[] memory;
    }

}
