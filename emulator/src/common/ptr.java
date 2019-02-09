package common;

/**
 *
 * @author shadow
 */
public class ptr {

    /**
     * ShortPtr emulation
     */
    public static class ShortPtr {

        public int bsize = 2;
        public byte[] memory;
        public int offset;

        public ShortPtr() {
        }

        public ShortPtr(int size) {
            memory = new byte[size * bsize];
            offset = 0;
        }

        public ShortPtr(ShortPtr cp) {
            memory = cp.memory;
            offset = cp.offset;
        }

        public ShortPtr(ShortPtr cp, int b) {
            memory = cp.memory;
            offset = cp.offset + b;
        }

        public ShortPtr(byte[] m) {
            memory = m;
            offset = 0;
        }

        public ShortPtr(short[] m) {
            memory = new byte[m.length * 2];
            for (int i = 0; i < m.length; i++) {
                memory[i * 2] = (byte) (m[i] & 0xff);
                memory[i * 2 + 1] = (byte) ((m[i] >>> 8) & 0xff);
            }
            offset = 0;
        }

        public short read() {
            return (short) ((memory[offset + 1] & 0xFF) << 8 | (memory[offset] & 0xFF));
        }

        public short read(int index) {
            return (short) ((memory[offset + 1 + index * 2] & 0xFF) << 8 | (memory[offset + index * 2] & 0xFF));
        }

        public short readinc() {
            short read = (short) ((memory[offset + 1] & 0xFF) << 8 | (memory[offset] & 0xFF));
            offset += bsize;
            return read;
        }

        public void write(int index, short data) {
            memory[offset + index * 2] = (byte) (data & 0xff);
            memory[offset + index * 2 + 1] = (byte) ((data >>> 8) & 0xff);
        }

        public void write(short data) {
            memory[offset] = (byte) (data & 0xff);
            memory[offset + 1] = (byte) ((data >>> 8) & 0xff);
        }

        public void writeinc(short data) {
            memory[offset] = (byte) (data & 0xff);
            memory[offset + 1] = (byte) ((data >>> 8) & 0xff);
            offset += bsize;
        }

        public void inc(int count) {
            offset += count * bsize;
        }

        public void inc() {
            offset += bsize;
        }

        public void dec(int count) {
            offset -= count * bsize;
        }

        public void dec() {
            offset -= bsize;
        }
    }
}
