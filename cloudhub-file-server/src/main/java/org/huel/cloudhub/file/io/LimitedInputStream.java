package org.huel.cloudhub.file.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import static org.huel.cloudhub.file.io.InternalLimitUtils.*;

/**
 * @author RollW
 */
public class LimitedInputStream extends FilterInputStream {
    private final AtomicLong readBytes = new AtomicLong(0);
    private boolean close = false;
    private final long limit;

    public LimitedInputStream(InputStream in, long limit) {
        super(in);
        this.limit = limit;
    }

    @Override
    public int read() throws IOException {
        return read1(close, limit, readBytes, in);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return readN(b, off, len, close, limit, readBytes, in);
    }

    void skipOver() throws IOException {
        int available = available();
        long s = skip(available);
    }

    @Override
    public long skip(long n) throws IOException {
        return skipN(n, limit, readBytes, in);
    }

    @Override
    public void close() {
        if (close) {
            return;
        }
        readBytes.set(0);
        close = true;
    }
}
