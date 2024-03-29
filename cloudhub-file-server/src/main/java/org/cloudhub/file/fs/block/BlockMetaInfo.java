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

package org.cloudhub.file.fs.block;

import org.cloudhub.file.fs.meta.BlockFileMeta;
import org.cloudhub.file.fs.meta.SerializedBlockFileMeta;
import org.cloudhub.file.fs.meta.SerializedBlockGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @author RollW
 */
@SuppressWarnings("unused")
public class BlockMetaInfo implements BlockFileMeta {
    public static final int NOT_CROSS_CONTAINER = -1;

    private final String fileId;
    private final BlockGroupsInfo blockGroupsInfo;
    private final long containerSerial;
    private final long validBytes;
    private final long nextContainerSerial;

    public BlockMetaInfo(String fileId, int start, int end,
                         long validBytes, long containerSerial,
                         long nextContainerSerial) {
        this.fileId = fileId;
        this.validBytes = validBytes;
        this.containerSerial = containerSerial;
        this.nextContainerSerial = nextContainerSerial;
        this.blockGroupsInfo = new BlockGroupsInfo(List.of(new BlockGroup(start, end)));
    }

    public BlockMetaInfo(String fileId, Collection<BlockGroup> blockGroups,
                         long validBytes, long containerSerial, long nextContainerSerial) {
        this.fileId = fileId;
        this.containerSerial = containerSerial;
        this.validBytes = validBytes;
        this.nextContainerSerial = nextContainerSerial;
        this.blockGroupsInfo = new BlockGroupsInfo(blockGroups);
    }


    public String getFileId() {
        return fileId;
    }

    public BlockGroupsInfo getBlockGroupsInfo() {
        return blockGroupsInfo;
    }

    public long getValidBytes() {
        return validBytes;
    }

    @Override
    public long getContainerSerial() {
        return containerSerial;
    }

    public long getNextContainerSerial() {
        return nextContainerSerial;
    }

    public boolean isInside(int start, int offset) {
        if (start == -1) {
            return isInside(blockGroupAt(0).start(), offset);
        }

        return getBlocksCountAfter(start) >= offset;
    }

    public int getBlocksCountAfter(int blockIndex) {
        return blockGroupsInfo.getBlocksCountAfter(blockIndex);
    }

    public List<BlockGroup> getBlockGroupAfter(int blockIndex) {
        return blockGroupsInfo.getBlockGroupAfter(blockIndex);
    }

    public int getBlockGroupsCount() {
        return blockGroupsInfo.getBlockGroupsCount();
    }

    public BlockGroup blockGroupAt(int index) {
        return blockGroupsInfo.blockGroupAt(index);
    }

    public boolean contains(int index) {
        return blockGroupsInfo.contains(index);
    }

    public int occupiedBlocks() {
        return blockGroupsInfo.occupiedBlocks();
    }

    public List<BlockGroup> getRawBlockGroups() {
        return blockGroupsInfo.getBlockGroups();
    }

    public BlockGroupsInfo getBlockGroups() {
        return blockGroupsInfo;
    }

    @Override
    public long getEndBlockByteOffset() {
        return validBytes;
    }

    @Override
    public long getCrossContainerSerial() {
        return nextContainerSerial;
    }

    public long validBytesAt(int index, long blockSizeInBytes) {
        // because of BlockMeta not hold block size value,
        // so you need pass this "blockSizeInBytes" value.
        List<BlockGroup> blockGroups = getRawBlockGroups();
        BlockGroup lastGroup = blockGroups.get(blockGroups.size() - 1);
        if (lastGroup.end() == index) {
            return validBytes;
        }
        if (contains(index)) {
            return blockSizeInBytes;
        }
        return -1;
    }

    @Deprecated
    public SerializedBlockFileMeta serialize() {
        List<BlockGroup> blockGroups = getRawBlockGroups();
        List<SerializedBlockGroup> serializedBlockGroups = new ArrayList<>();
        blockGroups.forEach(blockGroup ->
                serializedBlockGroups.add(blockGroup.serialize()));

        return SerializedBlockFileMeta.newBuilder()
                .setFileId(fileId)
                .addAllBlockGroups(serializedBlockGroups)
                .setCrossSerial(nextContainerSerial)
                .setEndBlockBytes(validBytes)
                .build();
    }

    @Deprecated
    public static BlockMetaInfo deserialize(SerializedBlockFileMeta blockFileMeta,
                                            long containerSerial) {
        List<BlockGroup> blockGroups = new ArrayList<>();
        blockFileMeta.getBlockGroupsList().forEach(serializedBlockGroup ->
                blockGroups.add(BlockGroup.deserialize(serializedBlockGroup)));

        return new BlockMetaInfo(blockFileMeta.getFileId(),
                blockGroups,
                blockFileMeta.getEndBlockBytes(),
                containerSerial,
                blockFileMeta.getCrossSerial());
    }

    public BlockMetaInfo forkNextSerial(long nextContainerSerial) {
        List<BlockGroup> blockGroups = getRawBlockGroups();
        return new BlockMetaInfo(fileId,
                blockGroups, validBytes,
                containerSerial, nextContainerSerial);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockMetaInfo that = (BlockMetaInfo) o;
        return containerSerial == that.containerSerial && validBytes == that.validBytes && nextContainerSerial == that.nextContainerSerial && Objects.equals(fileId, that.fileId) && Objects.equals(blockGroupsInfo, that.blockGroupsInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, blockGroupsInfo, containerSerial, validBytes, nextContainerSerial);
    }

    @Override
    public String toString() {
        return "BlockMetaInfo[" +
                "fileId=" + fileId +
                ";blockGroups=" + blockGroupsInfo +
                ";validBytes=" + validBytes +
                ";nextContainerSerial=" + nextContainerSerial +
                "]";
    }

}
