package common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author shadow
 */
public class util {

    /**
     * CRC-32
     */
    private final static long[] crcTable = new long[256];

    static {
        for (int i = 0; i < 256; i++) {
            long c = i;
            for (int k = 0; k < 8; k++) {
                if ((c & 1) != 0) {
                    c = 0xedb88320L ^ (c >> 1);
                } else {
                    c >>= 1;
                }
            }
            crcTable[i] = c;
        }
    }

    private static long updateCrc(long crc, char[] buf, int size) {
        long ans = crc;
        for (int i = 0; i < size; i++) {
            ans = crcTable[(int) ((ans ^ buf[i]) & 0xff)] ^ (ans >> 8);
        }
        return ans;
    }

    public static long crc32(char[] buf, int size) {
        return updateCrc(0xffffffffL, buf, size) ^ 0xffffffffL;
    }

    /**
     * return bytearray of a file inside zip
     */
    public static byte[] unZipFile(String zipFile, String filename) {
        byte[] out = null;

        if (!new File(zipFile).exists()) {
            System.out.println("unzip failed");
        } else {
            byte[] buffer = new byte[1024];

            try {
                //get the zip file content
                ZipInputStream zis
                        = new ZipInputStream(new FileInputStream(zipFile));
                //get the zipped file list entry
                ZipEntry ze = zis.getNextEntry();

                while (ze != null) {

                    String fileName = ze.getName();
                    if (fileName.equalsIgnoreCase(filename)) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            baos.write(buffer, 0, len);
                        }

                        baos.close();
                        out = baos.toByteArray();
                    }
                    ze = zis.getNextEntry();
                }

                zis.closeEntry();
                zis.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
        return out;
    }

    /**
     * Copy array
     */
    public static void copyArray(Object[] dst, Object[] src) {
        if (src == null) {
            return;
        }
        int k;
        for (k = 0; k < src.length; k++) {
            dst[k] = src[k];

        }
    }
    /*
     *  Convert command-line parameters
     */
    public static int argc;
    public static String[] argv;

    public static void ConvertArguments(String mainClass, String[] arguments) {
        argc = arguments.length + 1;
        argv = new String[argc];
        argv[0] = mainClass;
        for (int i = 1; i < argc; i++) {
            argv[i] = arguments[i - 1];
        }
    }
        /**
     * Convert a char array to an unsigned integer
     *
     * @param b
     * @return
     */
    public static long charArrayToLong(char[] b) {
        int start = 0;
        int i = 0;
        int len = 4;
        int cnt = 0;
        char[] tmp = new char[len];
        for (i = start; i < (start + len); i++) {
            tmp[cnt] = b[i];
            cnt++;
        }
        long accum = 0;
        i = 0;
        for (int shiftBy = 0; shiftBy < 32; shiftBy += 8) {
            accum |= ((long) (tmp[i] & 0xff)) << shiftBy;
            i++;
        }
        return accum;
    }

    /**
     * Convert a char array to a unsigned short
     *
     * @param b
     * @return
     */
    public static int charArrayToInt(char[] b) {
        int start = 0;
        int low = b[start] & 0xff;
        int high = b[start + 1] & 0xff;
        return (int) (high << 8 | low);
    }
}
