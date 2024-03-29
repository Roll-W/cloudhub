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

import org.cloudhub.file.fs.LockException;
import org.cloudhub.file.fs.block.Block;
import org.cloudhub.file.fs.block.BlockGroup;
import org.cloudhub.file.fs.block.BlockMetaInfo;
import org.cloudhub.file.fs.container.*;
import org.cloudhub.file.fs.meta.MetadataException;
import org.cloudhub.util.math.Maths;
import org.cloudhub.file.fs.LockException;
import org.cloudhub.file.fs.block.Block;
import org.cloudhub.file.fs.block.BlockGroup;
import org.cloudhub.file.fs.block.BlockMetaInfo;
import org.cloudhub.file.fs.container.*;
import org.cloudhub.file.fs.meta.MetadataException;
import org.cloudhub.util.math.Maths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Container File Writer. Not working for replica container.
 *
 * @author RollW
 */
public class ContainerFileWriter implements Closeable {
    private final String fileId;
    private final long fileSize;
    private final ContainerAllocator containerAllocator;
    private final ContainerWriterOpener containerWriterOpener;
    private final ContainerChecker containerChecker;
    private final FileWriteStrategy fileWriteStrategy;
    private final AtomicInteger writeBlocksSum = new AtomicInteger(0);
    /**
     * blocks count of this file.
     */
    private int expectBlocks = -1;// -1 means not calculated.


    private List<Container> allowWriteContainers;
    private Iterator<Container> containerIterator;

    public ContainerFileWriter(String fileId, long fileSize,
                               ContainerAllocator containerAllocator,
                               ContainerWriterOpener containerWriterOpener,
                               ContainerChecker containerChecker,
                               FileWriteStrategy fileWriteStrategy) {
        this.fileId = fileId;
        this.fileSize = fileSize;
        this.containerAllocator = containerAllocator;
        this.containerWriterOpener = containerWriterOpener;
        this.containerChecker = containerChecker;
        this.fileWriteStrategy = fileWriteStrategy;
        preAllocateContainers();
    }

    private void preAllocateContainers() {
        allowWriteContainers =
                new ArrayList<>(containerAllocator.allocateContainers(fileId, fileSize));
        containerIterator = allowWriteContainers.iterator();
    }

    private void calcExpectBlocks(int blockSizeInBytes) {
        if (expectBlocks >= 0) {
            return;
        }
        expectBlocks = Maths.ceilDivideReturnsInt(fileSize, blockSizeInBytes);
    }

    private Container requireNewContainer() {
        if (writeBlocksSum.get() >= expectBlocks) {
            return null;
        }
        return containerAllocator.allocateNewContainer(fileId);
    }

    private record WriteResult(ContainerWriter writer,
                               Container container,
                               List<FreeBlockInfo> remainingFreeBlockInfos,
                               List<BlockGroup> blockGroups,
                               int endIndex, long validBytes) {
    }

    private static final Logger logger = LoggerFactory.getLogger(ContainerFileWriter.class);

    private volatile WriteResult lastResult = null;

    public void writeBlocks(List<Block> blocks) throws LockException, IOException, MetadataException {
        internalWrite(lastResult, blocks);
    }

    private void internalWrite(WriteResult lastResult, List<Block> blocks) throws LockException, IOException, MetadataException {
        WriteResult newResult = overwriteWriteResult(lastResult);
        this.lastResult = recursiveWrite(blocks, newResult, 0);
    }

    private WriteResult overwriteWriteResult(WriteResult result) {
        if (result == null) {
            return null;
        }
        final int limit = result.container.getIdentity().blockLimit();
        if (result.endIndex >= limit - 1) {
            return new WriteResult(
                    result.writer, result.container,
                    List.of(),
                    result.blockGroups, 0,
                    result.validBytes);
        }
        return result;
    }

    private WriteResult recursiveWrite(List<Block> blocks, WriteResult writeResult, int off) throws LockException, IOException, MetadataException {
        // TODO: method body too long, split to more methods.
        ContainerWriter writer = forNext(writeResult);
        if (writer == null) {
            logger.debug("writer open null, return.");
            handleClose(writeResult);
            return null;
        }
        Container container = writer.getContainer();

        List<FreeBlockInfo> freeBlockInfos = getAllowWriteBlocks(container);
        if (freeBlockInfos.isEmpty()) {
            throw new IllegalArgumentException("Maybe a bug: no more free blocks.");
        }
        List<BlockGroup> blockGroups = createIfNull(writeResult);
        final int blockSize = blocks.size(), freeBlocksSize = freeBlockInfos.size();
        int writeBlocks = off, i = 0, lastIndex = -1;
        for (FreeBlockInfo freeBlockInfo : freeBlockInfos) {
            if (writeBlocks >= blockSize) {
                break;
            }
            int len = freeBlockInfo.getCount();
            if (writeBlocks + freeBlockInfo.getCount() > blockSize) {
                len = blockSize - writeBlocks;
            }
            final int fixOffset = getFixOffset(i, writeResult);
            final int startIndex = freeBlockInfo.getStart() + fixOffset;
            if (startIndex + len > freeBlockInfo.getCount()) {
                len = freeBlockInfo.getCount() - startIndex;
            }
            lastIndex = startIndex + len - 1;


            writer.seek(startIndex);
            writer.write(blocks, off, len, true);

            BlockGroup blockGroup = new BlockGroup(startIndex, lastIndex);
            blockGroups.add(blockGroup);
            writeBlocksSum.addAndGet(len);
            logger.debug("write: {} --- fix={};start={};end={};len={};writeBlocks={};writeBlocksSum={}",
                    i, fixOffset, startIndex, lastIndex, len, writeBlocks, writeBlocksSum.get());
            writeBlocks += len;
            i++;
        }

        Block lastBlock = blocks.get(writeBlocks - 1);
        if (writeBlocks < blockSize) {
            return recursiveWrite(blocks, new WriteResult(writer, container, List.of(),
                    blockGroups, -1, lastBlock.getValidBytes()), writeBlocks);
        }
        return new WriteResult(writer, container, freeBlockInfos.subList(i - 1, freeBlocksSize),
                blockGroups, lastIndex, lastBlock.getValidBytes());
    }


    private List<BlockGroup> createIfNull(WriteResult result) {
        if (result == null || result.blockGroups == null) {
            return new ArrayList<>();
        }
        return result.blockGroups;
    }

    private int getFixOffset(int index, WriteResult result) {
        // get fix offset.
        if (index != 0 || result == null) {
            return 0;
        }
        return result.endIndex() + 1;
    }

    private ContainerWriter forNext(WriteResult result) throws LockException, IOException, MetadataException {
        if (result == null) {
            return open(findNextAllowContainer());
        }
        if (!result.remainingFreeBlockInfos.isEmpty()) {
            FreeBlockInfo last = result.remainingFreeBlockInfos.get(0);
            if (last.getEnd() <= result.endIndex()) {
                // needs next.
                logger.debug("needs next container, freeEnd={};resultEnd={}",
                        last.getEnd(), result.endIndex());
                handleClose(result);
                return open(findNextAllowContainer());
            }
            return result.writer();
        }

        handleClose(result);
        Container next = updatesContainerMeta(result);
        return open(next);
    }

    private Container updatesContainerMeta(WriteResult result) throws IOException, MetadataException, LockException {
        Container container = result.container;
        String newCrc = containerChecker.calculateChecksum(container);
        container.getIdentity().updatesChecksum(newCrc);

        Container next = findNextAllowContainer();
        final long nextSerial = next == null
                ? BlockMetaInfo.NOT_CROSS_CONTAINER
                : next.getSerial();
        // needs update container here.
        BlockMetaInfo blockMetaInfo = new BlockMetaInfo(
                fileId, result.blockGroups(),
                result.validBytes(),
                container.getSerial(),
                nextSerial);
        container.updatesVersion();
        container.addBlockMetaInfos(blockMetaInfo);
        containerAllocator.updatesContainerMetadata(container);
        // get prepared for next write.
        result.blockGroups().clear();
        return next;
    }

    private void handleClose(WriteResult result) throws IOException {
        if (result == null || result.writer == null) {
            return;
        }
        result.writer.close();
    }

    private ContainerWriter open(Container container) throws LockException, IOException {
        if (container == null) {
            return null;
        }
        containerAllocator.createsContainerFileWithMeta(container);
        return new ContainerWriter(container, containerWriterOpener);
    }

    private FileWriteStrategy degradeStrategy() {
        return FileWriteStrategy.SEARCH_MAX;
    }

    public String getFileId() {
        return fileId;
    }

    public long getFileSize() {
        return fileSize;
    }

    private List<FreeBlockInfo> getAllowWriteBlocks(Container container) {
        if (fileWriteStrategy == FileWriteStrategy.SEQUENCE) {
            return container.getFreeBlockInfos();
        }
        if (fileWriteStrategy == FileWriteStrategy.SKIP_SMALL) {
            return skipSmallBlocks(container);
        }
        if (fileWriteStrategy == FileWriteStrategy.SEARCH_MAX) {
            return searchMaxBlocks(container);
        }
        throw new UnsupportedOperationException("Not supported.");
    }


    private List<FreeBlockInfo> skipSmallBlocks(Container container) {
        final int smallBlocks = expectBlocks / 4;

        List<FreeBlockInfo> blockInfos = container.getFreeBlockInfos().stream()
                .filter(freeBlockInfo -> freeBlockInfo.getCount() >= smallBlocks)
                .toList();
        if (blockInfos.isEmpty()) {
            return searchMaxBlocks(container);
        }
        return blockInfos;
    }

    private List<FreeBlockInfo> searchMaxBlocks(Container container) {
        return container.getFreeBlockInfos().stream()
                .filter(freeBlockInfo -> freeBlockInfo.getCount() >= expectBlocks)
                .toList();
    }

    private Container findNextAllowContainer() {
        if (!containerIterator.hasNext()) {
            return requireNewContainer();
        }
        Container container = containerIterator.next();
        calcExpectBlocks((int) container.getIdentity().blockSizeBytes());
        if (allowsWrite(container)) {
            return container;
        }
        return findNextAllowContainer();
    }

    private boolean allowsWrite(Container container) {
        if (fileWriteStrategy == FileWriteStrategy.SEQUENCE) {
            return true;
        }
        if (fileWriteStrategy == FileWriteStrategy.SKIP_SMALL) {
            Optional<FreeBlockInfo> blockInfo = container.getFreeBlockInfos().stream()
                    .filter(freeBlockInfo -> freeBlockInfo.getCount() >= expectBlocks)
                    .findFirst();
            if (blockInfo.isPresent()) {
                return true;
            }
        }
        return true;
    }

    public FileWriteStrategy getFileWritePolicy() {
        return fileWriteStrategy;
    }

    @Override
    public void close() throws IOException {
        if (lastResult == null) {
            return;
        }

        handleClose(lastResult);
        try {
            updatesContainerMeta(lastResult);
        } catch (MetadataException | LockException e) {
            throw new IOException(e);
        }
        lastResult = null;
    }
}
