package org.huel.cloudhub.file.fs.meta;

import org.huel.cloudhub.file.fs.container.ContainerFinder;
import org.huel.cloudhub.file.fs.container.ContainerType;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * @author RollW
 */
public class RawContainerMeta implements ContainerMeta {
    private final String locator;
    private final long version;
    private final int blockSize;
    private final int usedBlock;
    private final int blockCapacity;
    private final String checksum;
    private final List<? extends BlockFileMeta> blockFileMetas;

    private final SerializedContainerBlockMeta serializedContainerMeta;

    public RawContainerMeta(String locator, long version,
                            List<? extends BlockFileMeta> blockFileMetas,
                            SerializedContainerBlockMeta serializedContainerMeta) {
        this.locator = locator;
        this.version = version;
        this.blockSize = serializedContainerMeta.getBlockSize();
        this.usedBlock = serializedContainerMeta.getUsedBlock();
        this.blockCapacity = serializedContainerMeta.getBlockCap();
        this.checksum = serializedContainerMeta.getCrc();
        this.blockFileMetas = blockFileMetas;
        this.serializedContainerMeta = serializedContainerMeta;
    }

    public RawContainerMeta(String locator, long version,
                            int blockSize,
                            int usedBlock, int blockCapacity,
                            String checksum,
                            List<? extends BlockFileMeta> blockFileMetas) {
        this(locator, version, blockFileMetas,
                Helper.buildSerializedContainerMeta(
                        blockSize, usedBlock,
                        blockCapacity, checksum,
                        blockFileMetas)
        );
    }

    @Override
    public String getLocator() {
        return locator;
    }

    @Override
    public int getBlockSize() {
        return blockSize;
    }

    @Override
    public int getUsedBlock() {
        return usedBlock;
    }

    @Override
    public int getBlockCapacity() {
        return blockCapacity;
    }

    @Override
    public String getChecksum() {
        return checksum;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public String getSource() {
        return ContainerFinder.LOCAL;
    }

    @Override
    public ContainerType getContainerType() {
        return ContainerType.ORIGINAL;
    }

    @Override
    public List<? extends BlockFileMeta> getBlockFileMetas() {
        return blockFileMetas;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        serializedContainerMeta.writeTo(outputStream);
    }

    public static final class Builder implements ContainerMetaBuilder {
        private String locator;
        private long version;
        private int blockSize;
        private int usedBlock;
        private int blockCapacity;
        private String checksum;
        private List<? extends BlockFileMeta> blockFileMetas;

        @Override
        public Builder setLocator(String locator) {
            this.locator = locator;
            return this;
        }

        @Override
        public Builder setVersion(long version) {
            this.version = version;
            return this;
        }

        @Override
        public Builder setSource(String source) {
            return this;
        }

        @Override
        public Builder setBlockSize(int blockSize) {
            this.blockSize = blockSize;
            return this;
        }

        @Override
        public Builder setUsedBlock(int usedBlock) {
            this.usedBlock = usedBlock;
            return this;
        }

        @Override
        public Builder setBlockCapacity(int blockCapacity) {
            this.blockCapacity = blockCapacity;
            return this;
        }

        @Override
        public Builder setChecksum(String checksum) {
            this.checksum = checksum;
            return this;
        }

        @Override
        public Builder setBlockFileMetas(List<? extends BlockFileMeta> blockFileMetas) {
            this.blockFileMetas = blockFileMetas;
            return this;
        }

        @Override
        public RawContainerMeta build() {
            return new RawContainerMeta(locator, version,
                    blockSize, usedBlock, blockCapacity,
                    checksum, blockFileMetas
            );
        }
    }
}
