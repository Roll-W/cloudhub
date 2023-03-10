package org.huel.cloudhub.file.fs;

import space.lingu.NonNull;
import space.lingu.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * An abstract interface that represents a file or folder.
 * <p>
 * Although called "Server File", it can still be used to
 * represent a local file.
 *
 * @author RollW
 */
public interface ServerFile {
    String getName();

    String getPath();

    long length() throws IOException;

    long lastModified() throws IOException;

    default OutputStream openOutput() throws IOException {
        return openOutput(false);
    }

    OutputStream openOutput(boolean overwrite) throws IOException;

    InputStream openInput() throws IOException;

    boolean delete() throws IOException;

    boolean delete(boolean recursive) throws IOException;

    boolean exists() throws IOException;

    /**
     * Create new file. {@link #openOutput()} will also create a new file.
     *
     * @return create success or not.
     * @throws IOException IOException
     */
    boolean createFile() throws IOException;

    boolean mkdirs() throws IOException;

    boolean isFile() throws IOException;

    boolean isDirectory() throws IOException;

    @Nullable
    ServerFile getParent() throws IOException;

    @NonNull
    List<ServerFile> listFiles() throws IOException;

    @NonNull
    List<ServerFile> listFiles(boolean recursive) throws IOException;

}
