package org.huel.cloudhub.file.fs;

import org.huel.cloudhub.file.io.RepresentFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * ๆไปถๅ้
 *
 * @author RollW
 */
public class FileAllocator implements Closeable {
    private final FileChannel fileChannel;
    private final RandomAccessFile accessFile;

    public FileAllocator(RepresentFile file) throws IOException {
        this.accessFile =
                new RandomAccessFile(file.toFile(), "rw");
        this.fileChannel = accessFile.getChannel();
    }

    private static final ByteBuffer PADDING = ByteBuffer.allocateDirect(1);

    public void allocateSize(long byteSize) throws IOException {
        fileChannel.write(PADDING.position(0), byteSize);
    }

    @Override
    public void close() throws IOException {
        fileChannel.close();
        accessFile.close();
    }
}
