package common;

/**
 *
 * @author shadow
 */
import java.io.ByteArrayInputStream;

public class seekableBAIS extends ByteArrayInputStream {

    public seekableBAIS(byte buf[]) {
        super(buf);
    }

    public seekableBAIS(byte buf[], int offset, int length) {
        super(buf, offset, length);
    }

    public void seek(int p) {
        this.pos = p;
    }

    public long tell() {
        return pos;
    }
}
