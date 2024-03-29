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

package org.cloudhub.file.server.service.file;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.RandomStringUtils;
import org.cloudhub.file.fs.LocalFileServer;
import org.cloudhub.file.fs.LockException;
import org.cloudhub.file.fs.ServerFile;
import org.cloudhub.file.fs.block.Block;
import org.cloudhub.file.fs.block.BlockMetaInfo;
import org.cloudhub.file.fs.block.FileBlockMetaInfo;
import org.cloudhub.file.fs.container.ContainerFinder;
import org.cloudhub.file.fs.container.ContainerProperties;
import org.cloudhub.file.fs.container.file.ContainerFileWriter;
import org.cloudhub.file.fs.container.file.FileWriteStrategy;
import org.cloudhub.file.fs.meta.MetadataException;
import org.cloudhub.file.rpc.block.*;
import org.cloudhub.file.server.service.replica.ReplicaService;
import org.cloudhub.file.server.service.replica.ReplicaSynchroPart;
import org.cloudhub.rpc.StreamObserverWrapper;
import org.cloudhub.server.rpc.server.SerializedFileServer;
import org.cloudhub.file.fs.LocalFileServer;
import org.cloudhub.file.fs.LockException;
import org.cloudhub.file.fs.ServerFile;
import org.cloudhub.file.fs.block.Block;
import org.cloudhub.file.fs.block.BlockMetaInfo;
import org.cloudhub.file.fs.block.FileBlockMetaInfo;
import org.cloudhub.file.fs.container.Container;
import org.cloudhub.file.fs.container.ContainerAllocator;
import org.cloudhub.file.fs.container.ContainerChecker;
import org.cloudhub.file.fs.container.ContainerFinder;
import org.cloudhub.file.fs.container.ContainerGroup;
import org.cloudhub.file.fs.container.ContainerProperties;
import org.cloudhub.file.fs.container.ContainerWriterOpener;
import org.cloudhub.file.fs.container.file.ContainerFileWriter;
import org.cloudhub.file.fs.container.file.FileWriteStrategy;
import org.cloudhub.file.fs.meta.MetadataException;
import org.cloudhub.file.rpc.block.BlockUploadServiceGrpc;
import org.cloudhub.file.rpc.block.UploadBlockData;
import org.cloudhub.file.rpc.block.UploadBlocksInfo;
import org.cloudhub.file.rpc.block.UploadBlocksRequest;
import org.cloudhub.file.rpc.block.UploadBlocksResponse;
import org.cloudhub.file.server.service.replica.ReplicaService;
import org.cloudhub.file.server.service.replica.ReplicaSynchroPart;
import org.cloudhub.rpc.StreamObserverWrapper;
import org.cloudhub.server.rpc.server.SerializedFileServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author RollW
 */
@Service
public class BlockReceiveService extends BlockUploadServiceGrpc.BlockUploadServiceImplBase {
    private final Logger logger = LoggerFactory.getLogger(BlockReceiveService.class);
    private final ContainerAllocator containerAllocator;
    private final ContainerFinder containerFinder;
    private final ContainerProperties containerProperties;
    private final ContainerWriterOpener containerWriterOpener;
    private final ContainerChecker containerChecker;
    private final ReplicaService replicaService;
    private final LocalFileServer localFileServer;
    private final ServerFile stagingDir;
    private static final int BUFFERED_BLOCK_SIZE = 320;

    public BlockReceiveService(ContainerAllocator containerAllocator,
                               ContainerFinder containerFinder,
                               ContainerProperties containerProperties,
                               ContainerWriterOpener containerWriterOpener,
                               ContainerChecker containerChecker,
                               ReplicaService replicaService,
                               LocalFileServer localFileServer) throws IOException {
        this.containerAllocator = containerAllocator;
        this.containerFinder = containerFinder;
        this.containerProperties = containerProperties;
        this.containerWriterOpener = containerWriterOpener;
        this.containerChecker = containerChecker;
        this.replicaService = replicaService;
        this.localFileServer = localFileServer;
        this.stagingDir = localFileServer.getServerFileProvider()
                .openFile(containerProperties.getStagePath());
        initStagingDirectory();
    }

    private void initStagingDirectory() throws IOException {
        if (stagingDir.exists()) {
            return;
        }
        stagingDir.mkdirs();
    }

    private ServerFile openNewStagingFile() throws IOException {
        initStagingDirectory();
        ServerFile file = localFileServer.getServerFileProvider().openFile(
                containerProperties.getStagePath(),
                RandomStringUtils.randomAlphanumeric(20));
        if (file.exists()) {
            return openNewStagingFile();
        }
        file.createFile();
        return file;
    }

    @Override
    public StreamObserver<UploadBlocksRequest> uploadBlocks(StreamObserver<UploadBlocksResponse> responseObserver) {
        try {
            return new UploadBlocksRequestObserver(responseObserver);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class UploadBlocksRequestObserver implements StreamObserver<UploadBlocksRequest> {
        private final StreamObserverWrapper<UploadBlocksResponse> responseObserver;
        private final ServerFile stagingFile;
        private final AtomicInteger received = new AtomicInteger(0);
        private OutputStream stagingOut;

        // save to staging dir, and puts blocks in the area.
        // when receive all requests,
        // reads all data into the container step by step after.

        private String fileId;
        private int indexCount = -1;
        private long validBytes;
        private long fileLength;
        private String checkValue;
        private List<SerializedFileServer> fileServers;

        UploadBlocksRequestObserver(StreamObserver<UploadBlocksResponse> responseObserver) throws IOException {
            this.responseObserver = new StreamObserverWrapper<>(responseObserver);
            this.stagingFile = openNewStagingFile();
        }

        private void checkExistsWithClose(String fileId) {
            boolean exists = containerFinder.dataExists(fileId, ContainerFinder.LOCAL);
            UploadBlocksResponse response = UploadBlocksResponse.newBuilder()
                    .setFileExists(exists)
                    .build();
            responseObserver.onNext(response);
            if (!exists) {
                return;
            }
            logger.debug("File exists, id={}", fileId);
            responseObserver.onCompleted();
        }

        private void saveCheckMessageInfo(UploadBlocksRequest.CheckMessage checkMessage) {
            indexCount = checkMessage.getRequestCount();
            validBytes = checkMessage.getValidBytes();
            fileLength = checkMessage.getFileLength();
            checkValue = checkMessage.getCheckValue();
            fileServers = checkMessage.getReplicaHostsList();
            logger.debug("Receive upload message. count={};validBytes={};fileLen={};check={}",
                    indexCount, validBytes, fileLength, checkValue);
            try {
                stagingOut = stagingFile.openOutput();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onNext(UploadBlocksRequest request) {
            if (responseObserver.isClose()) {
                return;
            }
            if (request.getUploadCase() == UploadBlocksRequest.UploadCase.UPLOAD_NOT_SET) {
                responseObserver.onError(Status.INVALID_ARGUMENT.asException());
                return;
            }
            if (fileId != null && !fileId.equals(request.getIdentity())) {
                responseObserver.onError(Status.INVALID_ARGUMENT.asRuntimeException());
                logger.error("Error receive blocks: The ids of the files before and after are different.");
                return;
            }
            fileId = request.getIdentity();
            checkExistsWithClose(fileId);

            if (request.getUploadCase() == UploadBlocksRequest.UploadCase.CHECK_MESSAGE) {
                saveCheckMessageInfo(request.getCheckMessage());
                return;
            }

            UploadBlocksInfo uploadBlocksInfo = request.getUploadBlocks();
            final int count = uploadBlocksInfo.getBlocksCount();
            logger.debug("Receive blocks. index={};count=[{}];id={};",
                    uploadBlocksInfo.getIndex(), count, request.getIdentity());
            received.incrementAndGet();
            long valid = uploadBlocksInfo.getIndex() == count ? validBytes : -1;
            try {
                writeOut(uploadBlocksInfo, valid);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void writeOut(UploadBlocksInfo uploadBlocksInfo, long validBytes) throws IOException {
            if (stagingOut == null) {
                return;
            }
            int i = 0;
            for (UploadBlockData uploadBlockData : uploadBlocksInfo.getBlocksList()) {
                if (i == uploadBlocksInfo.getBlocksCount() - 1 && validBytes >= 0) {
                    stagingOut.write(uploadBlockData.getData().toByteArray(), 0,
                            (int) validBytes);
                    return;
                }
                stagingOut.write(uploadBlockData.getData().toByteArray());
                i++;
            }
        }

        @Override
        public void onError(Throwable t) {
            logger.error("Receive blocks error.", t);
        }

        private void closeAndExit() {
            try {
                stagingOut.close();
                delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private long closeAndCalcLength() {
            try {
                stagingOut.close();
                return stagingFile.length();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return -1;
        }

        private void delete() {
            try {
                stagingFile.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCompleted() {
            // TODO(style): method body too long.
            if (indexCount <= 0 && responseObserver.isOpen()) {
                closeAndExit();
                logger.error("Invalid index count {} in uploading {}", indexCount, fileId);
                responseObserver.onError(Status.INVALID_ARGUMENT.asException());
                return;
            }
            if (indexCount != received.get() && responseObserver.isOpen()) {
                closeAndExit();
                logger.debug("Data may have been lost in uploading {}. given: {}, receive: {}",
                        fileId, indexCount, received.get());
                responseObserver.onError(Status.DATA_LOSS.asException());
                return;
            }
            if (responseObserver.isClose()) {
                closeAndExit();
                return;
            }
            long savedLength = closeAndCalcLength();
            if (savedLength != fileLength) {
                delete();
                logger.debug("Data may have been lost in uploading {}. given len: {}, receive len: {}",
                        fileId, fileLength, savedLength);
                responseObserver.onError(Status.DATA_LOSS.asException());
                return;
            }

            if (savedLength < 0) {
                delete();
                logger.debug("Internal receive data error, file length < 0.");
                responseObserver.onError(Status.INTERNAL.asException());
                return;
            }

            try (ContainerFileWriter containerFileWriter = new ContainerFileWriter(fileId,
                    savedLength, containerAllocator, containerWriterOpener, containerChecker, FileWriteStrategy.SEQUENCE)) {
                writeUntilEnd(containerFileWriter, stagingFile.openInput(), BUFFERED_BLOCK_SIZE, validBytes);
            } catch (IOException | MetadataException e) {
                logger.error("Occurred error here while saving to container.", e);
            } catch (LockException e) {
                logger.error("Cannot get container's lock.", e);
            }
            Context context = Context.current().fork();
            responseObserver.onNext(UploadBlocksResponse.newBuilder().build());
            responseObserver.onCompleted();
            delete();
            context.run(() ->
                    sendReplicaRequest(fileId, fileServers));
        }

    }

    private void writeUntilEnd(ContainerFileWriter writer,
                               InputStream inputStream,
                               int blockSize, long validBytes) throws IOException, LockException, MetadataException {
        List<Block> blocks = new ArrayList<>();
        Block block;
        int size = 0;
        while ((block = readBlock(inputStream, validBytes)) != null) {
            if (size == blockSize) {
                writer.writeBlocks(blocks);
                blocks = new ArrayList<>();
                size = 0;
            }
            blocks.add(block);
            size++;
        }
        writer.writeBlocks(blocks);
        inputStream.close();
    }

    private Block readBlock(InputStream inputStream, long validBytes) throws IOException {
        int size = containerProperties.getBlockSizeInBytes();
        byte[] chunk = new byte[size];
        int read = inputStream.read(chunk);
        if (read == -1) {
            return null;
        }
        if (read == 0) {
            return new Block(chunk, validBytes);
        }
        return new Block(chunk, read);
    }

    private void sendReplicaRequest(String fileId, List<SerializedFileServer> servers) {
        if (servers.isEmpty()) {
            return;
        }
        List<ReplicaSynchroPart> parts = buildSynchroParts(fileId);
        servers.forEach(server -> callRequestReplicaSynchro(parts, server));
    }

    @Async
    void callRequestReplicaSynchro(List<ReplicaSynchroPart> parts, SerializedFileServer server) {
        //
        replicaService.requestReplicasSynchro(parts, server);
    }


    private List<ReplicaSynchroPart> buildSynchroParts(String fileId) {
        ContainerGroup group =
                containerFinder.findContainerGroupByFile(fileId, ContainerFinder.LOCAL);
        List<ReplicaSynchroPart> synchroParts = new ArrayList<>();
        FileBlockMetaInfo fileBlockMetaInfo = group.getFileBlockMetaInfo(fileId);
        for (BlockMetaInfo blockMetaInfo : fileBlockMetaInfo.getBlockMetaInfos()) {
            Container container =
                    group.getContainer(blockMetaInfo.getContainerSerial());
            ReplicaSynchroPart part = new ReplicaSynchroPart(container,
                    blockMetaInfo.getBlockGroupsInfo(),
                    blockMetaInfo.occupiedBlocks());
            synchroParts.add(part);
        }
        return synchroParts;
    }

}
