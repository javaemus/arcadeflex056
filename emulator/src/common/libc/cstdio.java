package common.libc;

import common.seekableBAIS;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author shadow
 */
public class cstdio {

    /**
     * Write formatted data to string
     *
     * @param str
     * @param arguments
     * @return
     */
    public static String sprintf(String str, Object... arguments) {
        return String.format(str, arguments);
    }

    /*
     *  function equals to c printf syntax
     */
    public static void printf(String str, Object... arguments) {
        System.out.printf(str, arguments);
    }
    /**
     * File access functions
     */
    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;

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
        public String name;
        public int offset;
    }

    public static FILE fopen(String name, String format) {
        try {
            FILE file;
            file = new FILE();
            switch (format) {
                case "a":
                    file.fw = new FileWriter(name, true);
                    break;
                case "w":
                    file.fw = new FileWriter(name, false);
                    break;
                case "rb":
                    try {
                        file.setBytes(getFileBytes(new File(name)));
                        file.name = name;
                    } catch (Exception e) {
                        file = null;
                        return null;
                    }
                    break;
                case "wa":
                    file.fw = new FileWriter(name, true);
                    break;
                case "wb":
                    file.fos = new FileOutputStream(name, false);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported");
            }
            return file;
        } catch (IOException ex) {
            return null;
        }
    }

    public static FILE fopen(byte[] bytes, String name, String format) {
        FILE file;
        file = new FILE();
        switch (format) {
            case "rb":
                file.setBytes(bytes);
                file.name = name;
                break;
            case "wb": {
                try {
                    file.fos = new FileOutputStream(name, false);
                } catch (FileNotFoundException ex) {
                    return null;
                }
            }
            break;

            default:
                throw new UnsupportedOperationException("Unsupported");
        }
        return file;
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

    public static long ftell(FILE file) {
        try {
            return file.bytes.length;
        } catch (Exception e) {
        }
        return 0;
    }

    public static void fprintf(FILE file, String str, Object... arguments) {
        str = str.replace("\n", "%n");//fix for windows
        String print = String.format(str, arguments);
        try {
            file.fw.write(print);
        } catch (Exception e) {
        }
    }

    public static void fseek(FILE file, int pos, int whence) {
        if (file.bais != null) {
            switch (whence) {
                case SEEK_SET:
                    file.bais.seek(pos);
                    break;
                case SEEK_CUR:
                    file.bais.seek((int) (file.bais.tell() + pos));
                    break;
                default:
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
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (ous != null) {
                    ous.close();
                }
            } catch (IOException e) {
                return null;
            }
            try {
                if (ios != null) {
                    ios.close();
                }
            } catch (IOException e) {
                return null;
            }
        }
        return ous.toByteArray();
    }
}
