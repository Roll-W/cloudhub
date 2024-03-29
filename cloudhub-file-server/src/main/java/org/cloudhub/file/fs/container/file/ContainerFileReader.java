/*
 * Cloudhub - A high available, scalable distributed file system.
 * Copyright (C) 2022 Cloudhub
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.cloudhub.file.fs.container.file;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudhub.file.fs.LockException;
import org.cloudhub.file.fs.block.BlockGroup;
import org.cloudhub.file.fs.block.BlockMetaInfo;
import org.cloudhub.file.fs.block.ContainerBlock;
import org.cloudhub.file.fs.block.FileBlockMetaInfo;
import org.cloudhub.file.fs.container.*;
import org.cloudhub.file.io.IoUtils;
import org.cloudhub.util.math.Maths;
import org.cloudhub.file.fs.LockException;
import org.cloudhub.file.fs.block.BlockGroup;
import org.cloudhub.file.fs.block.BlockMetaInfo;
import org.cloudhub.file.fs.block.ContainerBlock;
import org.cloudhub.file.fs.block.FileBlockMetaInfo;
import org.cloudhub.file.fs.container.*;
import org.cloudhub.file.io.IoUtils;
import org.cloudhub.util.math.Maths;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author RollW
 */
public class ContainerFileReader implements Closeable {
    private final ContainerFinder containerFinder;
    private final ContainerReadOpener containerReadOpener;
    private final ContainerGroup containerGroup;
    private final List<Container> containers;
    private final FileBlockMetaInfo fileBlockMetaInfo;
    private ListIterator<Container> containerIterator;
    private final int totalBlocks;
    private final AtomicInteger currentRead = new AtomicInteger(0);

    @Deprecated
    public ContainerFileReader(ContainerReadOpener containerReadOpener,
                               ContainerFinder containerFinder,
                               String fileId, String source) throws ContainerException {
        this.containerReadOpener = containerReadOpener;
        this.containerGroup = containerFinder.findContainerGroupByFile(fileId, source);
        this.containerFinder = containerFinder;
        this.containers = containerGroup.containersWithFile(fileId);
        this.fileBlockMetaInfo = containerGroup.getFileBlockMetaInfo(fileId);
        if (fileBlockMetaInfo == null) {
            throw new ContainerException("No such file '%s' of source '%s'".formatted(fileId, source));
        }
        this.totalBlocks = fileBlockMetaInfo.getBlocksCount();
        initIterator();
    }

    public ContainerFileReader(ContainerReadOpener containerReadOpener,
                               ContainerFinder containerFinder,
                               String fileId,
                               ContainerGroup containerGroup,
                               FileBlockMetaInfo fileBlockMetaInfo) throws ContainerException {
        this.containerReadOpener = containerReadOpener;
        this.containerGroup = containerGroup;
        this.containerFinder = containerFinder;
        this.containers = containerGroup.containersWithFile(fileId);
        this.fileBlockMetaInfo = fileBlockMetaInfo;
        if (fileBlockMetaInfo == null) {
            throw new ContainerException("No such file '%s' in container group id '%s', source '%s'.".formatted(
                    fileId,
                    containerGroup.getContainerId(),
                    containerGroup.getSourceId()
            ));
        }
        this.totalBlocks = fileBlockMetaInfo.getBlocksCount();
        initIterator();
    }

    private void initIterator() {
        containerIterator = containers.listIterator();
    }


    private record ReadBlockDest(long serial, int endIndex) {
    }

    private record ReadResult(List<ContainerBlock> containerBlocks,
                              ContainerReader reader,
                              ReadBlockDest readBlockDest) {
    }

    public boolean hasNext() {
        return currentRead.get() < totalBlocks;
    }

    private ReadResult lastResult = null;

    private static final int SKIP_BUFFER_SIZE = 640;

    /**
     * @deprecated Using {@link #seek(int)} instead.
     */
    @Deprecated
    public void skip(int size) throws LockException, IOException {
        if (size <= 0) {
            return;
        }
        int times = Maths.ceilDivide(size, SKIP_BUFFER_SIZE) - 1;
        for (int i = 0; i < times; i++) {
            read(SKIP_BUFFER_SIZE);
        }
        read(size - SKIP_BUFFER_SIZE * times);
    }

    public void seek(int blocks) throws LockException, IOException {
        int nowSum = 0;
        for (BlockMetaInfo blockMetaInfo : fileBlockMetaInfo.getBlockMetaInfos()) {
            int tBlocks = blockMetaInfo.getBlockGroupsInfo().occupiedBlocks();
            if (nowSum + tBlocks >= blocks) {
                long serial = blockMetaInfo.getContainerSerial();
                // we can know that we need start from the container here.
                seekAndRelocateContainer(serial, blocks - nowSum);
                return;
            }
            nowSum += tBlocks;
        }
    }

    private void seekAndRelocateContainer(long serial, int index) throws LockException, IOException {
        initIterator();
        currentRead.set(index);
        while (containerIterator.hasNext()) {
            Container container = containerIterator.next();
            if (container.getSerial() != serial) {
                continue;
            }
            ContainerReader reader = new ContainerReader(container, containerReadOpener);
            hackLastResult(reader, serial, index - 1);
            return;
        }
    }

    private void hackLastResult(ContainerReader reader, long serial, int endIndex) {
        lastResult = new ReadResult(List.of(), reader, new ReadBlockDest(serial, endIndex));
    }

    public List<ContainerBlock> read(int readBlocks) throws IOException, LockException {
        if (lastResult == null) {
            lastResult = readSizeOf(null, readBlocks, null);
            if (lastResult == null) {
                return null;
            }
            return lastResult.containerBlocks();
        }

        lastResult = readSizeOf(lastResult.reader(), readBlocks, lastResult.readBlockDest());
        if (lastResult == null) {
            return null;
        }
        return lastResult.containerBlocks();
    }

    private ContainerReader openFirst(ContainerReader reader) throws IOException, LockException {
        if (reader == null) {
            return openNext(containerIterator);
        }
        return reader;
    }

    // 读取给定块数
    private ReadResult readSizeOf(@Nullable ContainerReader reader,
                                  final int readBlockCount,
                                  final ReadBlockDest lastRead) throws IOException, LockException {
        ContainerReader containerReader = openFirst(reader);
        if (containerReader == null) {
            return null;
        }

        List<ContainerBlock> containerBlocksRes = new ArrayList<>();
        long serial = 1;
        int start = 0;
        if (lastRead != null) {
            serial = lastRead.serial();
            start = lastRead.endIndex() + 1;
        }

        List<BlockMetaInfo> blockMetaInfos = findBlockCount(fileBlockMetaInfo,
                serial, start, readBlockCount);

        int index = 0, readBlocks = 0;
        final int maxIndex = blockMetaInfos.size() - 1;
        if (maxIndex < 0) {
            return null;
        }

        for (BlockMetaInfo blockMetaInfo : blockMetaInfos) {
            if (blockMetaInfo.getContainerSerial() != serial) {
                containerReader = openNext(containerIterator);
                // basically, it will not be null.
                serial = containerReader.getContainer().getIdentity().serial();
                start = -1;
            }

            var readResult = readBlocks(containerReader, blockMetaInfo,
                    start, readBlockCount - readBlocks);
            start = readResult.readBlockDest().endIndex() + 1;
            final int readSize = readResult.containerBlocks().size();
            currentRead.addAndGet(readSize);
            readBlocks += readSize;

            containerBlocksRes.addAll(readResult.containerBlocks());

            if (index != maxIndex) {
                IoUtils.closeQuietly(containerReader);
            } else {
                return new ReadResult(containerBlocksRes, containerReader,
                        new ReadBlockDest(serial, readResult.readBlockDest().endIndex()));
            }

            index++;
        }

        throw new ContainerException("Illegal end at %d, and it should be end at %d in container group '%s'."
                .formatted(index, maxIndex, containerGroup.getContainerId()));
    }

    private ReadResult readBlocks(final ContainerReader reader,
                                  BlockMetaInfo blockMetaInfo,
                                  int start,
                                  int toReadSize) throws IOException {
        List<ContainerBlock> containerBlocks = new ArrayList<>();
        int index = 0, sizeRead = 0, endBlock = 0;
        List<BlockGroup> blockGroups = blockMetaInfo.getRawBlockGroups();
        for (BlockGroup blockGroup : blockGroups) {
            final int startBlock = calcStartBlock(index, start, blockGroup);
            final int willReadBlocks = index == 0
                    ? blockGroup.end() - startBlock + 1
                    : blockGroup.occupiedBlocks();

            if (sizeRead + willReadBlocks >= toReadSize) {
                endBlock = (toReadSize + startBlock - sizeRead) - 1;// -1 to index.
                var read = reader.readBlocks(startBlock, endBlock);

                containerBlocks.addAll(read);
                return new ReadResult(containerBlocks, reader,
                        new ReadBlockDest(blockMetaInfo.getContainerSerial(), endBlock));
            }
            endBlock = blockGroup.end();
            List<ContainerBlock> readBlocks = reader.readBlocks(startBlock, endBlock);
            sizeRead += willReadBlocks;
            containerBlocks.addAll(readBlocks);

            index++;
        }

        return new ReadResult(containerBlocks, reader, new ReadBlockDest(
                blockMetaInfo.getContainerSerial(), endBlock));
    }

    private int calcStartBlock(int index, int start, BlockGroup blockGroup) {
        if (index != 0) {
            return blockGroup.start();
        }
        if (start < 0) {
            return blockGroup.start();
        }
        return start;
    }

    private List<BlockMetaInfo> findBlockCount(FileBlockMetaInfo fileBlockMetaInfo,
                                               long startSerial, int start,
                                               int blockCount) {
        if (startSerial <= 1) {
            startSerial = 1;
        }

        List<BlockMetaInfo> blockMetaInfos = fileBlockMetaInfo.getAfter(startSerial);
        List<BlockMetaInfo> res = new ArrayList<>();
        if (blockMetaInfos.isEmpty()) {
            return List.of();
        }

        int needBlocks = blockCount, startBlock = start;
        for (BlockMetaInfo blockMetaInfo : blockMetaInfos) {
            if (needBlocks <= 0) {
                return res;
            }
            final int afterBlocks = blockMetaInfo.getBlocksCountAfter(startBlock);
            if (compareInside(afterBlocks, needBlocks)) {
                res.add(blockMetaInfo);
                return res;
            }
            startBlock = -1;
            needBlocks -= afterBlocks;
            if (afterBlocks > 0) {
                res.add(blockMetaInfo);
            }
        }
        return res;
    }

    private boolean compareInside(int afterBlocks, int offset) {
        return afterBlocks >= offset;
    }

    private ContainerReader openNext(ListIterator<Container> containerIterator) throws IOException, LockException {
        if (!containerIterator.hasNext()) {
            return null;
        }

        return new ContainerReader(containerIterator.next(), containerReadOpener);
    }

    public long getBlockSizeInBytes() {
        return containerGroup.getBlockSizeInBytes();
    }

    @Override
    public void close() throws IOException {
        if (lastResult == null) {
            return;
        }

        IoUtils.closeQuietly(lastResult.reader());
    }
}
