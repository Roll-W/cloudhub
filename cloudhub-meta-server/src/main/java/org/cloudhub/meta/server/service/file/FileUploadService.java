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

package org.cloudhub.meta.server.service.file;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.RandomStringUtils;
import org.cloudhub.file.io.BufferedStreamIterator;
import org.cloudhub.file.io.ReopenableInputStream;
import org.cloudhub.file.rpc.block.*;
import org.cloudhub.meta.fs.FileObjectUploadStatus;
import org.cloudhub.meta.server.configuration.FileProperties;
import org.cloudhub.meta.server.data.database.repository.FileStorageLocationRepository;
import org.cloudhub.meta.server.data.database.repository.MasterReplicaLocationRepository;
import org.cloudhub.meta.server.data.entity.FileStorageLocation;
import org.cloudhub.meta.server.data.entity.MasterReplicaLocation;
import org.cloudhub.meta.server.service.node.*;
import org.cloudhub.rpc.GrpcProperties;
import org.cloudhub.rpc.GrpcServiceStubPool;
import org.cloudhub.rpc.StreamObserverWrapper;
import org.cloudhub.server.rpc.server.SerializedFileServer;
import org.cloudhub.util.math.Maths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author RollW
 */
@Service
public class FileUploadService {
    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    private final NodeAllocator nodeAllocator;
    private final FileProperties fileProperties;
    private final GrpcProperties grpcProperties;
    private final FileStorageLocationRepository storageLocationRepository;
    private final MasterReplicaLocationRepository masterReplicaLocationRepository;
    private final NodeChannelPool nodeChannelPool;
    private final GrpcServiceStubPool<BlockUploadServiceGrpc.BlockUploadServiceStub>
            blockUploadServiceStubPool;
    private final ServerChecker serverChecker;

    public FileUploadService(HeartbeatService heartbeatService,
                             FileProperties fileProperties,
                             NodeChannelPool nodeChannelPool,
                             FileStorageLocationRepository storageLocationRepository,
                             MasterReplicaLocationRepository masterReplicaLocationRepository,
                             GrpcProperties grpcProperties) {
        this.nodeAllocator = heartbeatService.getNodeAllocator();
        this.fileProperties = fileProperties;
        this.storageLocationRepository = storageLocationRepository;
        this.masterReplicaLocationRepository = masterReplicaLocationRepository;
        this.nodeChannelPool = nodeChannelPool;
        this.grpcProperties = grpcProperties;
        this.serverChecker = heartbeatService.getServerChecker();
        this.blockUploadServiceStubPool = new GrpcServiceStubPool<>();
        initial();
    }

    public boolean checkFileExists(String hash) {
        List<FileStorageLocation> locations =
                storageLocationRepository.getLocationsByFileId(hash);
        if (locations.isEmpty()) {
            return false;
        }
        for (FileStorageLocation location : locations) {
            boolean res = checkServers(location.getServerList());
            if (res) {
                return true;
            }
        }
        // all servers goes down, means file not exists anymore, for now.
        // allowed to re-upload.
        return false;
    }

    private boolean checkServers(List<String> serverIds) {
        if (serverIds == null) {
            return false;
        }

        for (String serverId : serverIds) {
            if (serverChecker.isActive(serverId)) {
                return true;
            }
        }
        return false;
    }

    public void uploadFile(InputStream inputStream, FileUploadStatusCallback callback) throws IOException {
        logger.debug("Start calculation on the given input stream.");
        Hasher sha256Hasher = Hashing.sha256().newHasher();
        ReopenableInputStream reopenableInputStream = convertInputStream(inputStream, sha256Hasher);
        final String hash = reopenableInputStream.getHash(sha256Hasher).toString();
        uploadFile(reopenableInputStream, hash, reopenableInputStream.getLength(), callback);
    }


    /**
     * Test only.
     */
    public void uploadFile(InputStream inputStream, FileUploadStatusDataCallback callback) throws IOException {
        logger.debug("Start calculation on the given input stream.");
        Hasher sha256Hasher = Hashing.sha256().newHasher();
        ReopenableInputStream reopenableInputStream = convertInputStream(inputStream, sha256Hasher);
        final String hash = reopenableInputStream.getHash(sha256Hasher).toString();
        if (callback != null) {
            callback.onCalc(hash);
        }
        uploadFile(reopenableInputStream, hash, reopenableInputStream.getLength(), callback);
    }

    public void uploadFile(InputStream inputStream, String hash, long length, FileUploadStatusCallback callback) throws IOException {
        if (checkFileExists(hash)) {
            inputStream.close();
            logger.debug("file exists. file_id={}", hash);
            if (callback != null) {
                callback.onNextStatus(FileObjectUploadStatus.AVAILABLE);
            }
            return;
        }
        logger.debug("Start upload fileId={}", hash);
        final long maxBlocksValue = grpcProperties.getMaxRequestSizeBytes() >> 1;
        final int blockSizeInBytes = fileProperties.getBlockSizeInBytes();
        // calc how many [UploadBlock]s a request can contain at most
        final int maxUploadBlockCount = (int) (maxBlocksValue / blockSizeInBytes);
        // calc how many requests will be sent.
        final int requestCount = Maths.ceilDivideReturnsInt(length, maxBlocksValue);
        final long validBytes = length % blockSizeInBytes;
        logger.debug("Calc: length={};maxBlocksValue={};blockSizeInBytes={};maxUploadBlockCount={};requestCount={};validBytes={}",
                length, maxBlocksValue, blockSizeInBytes, maxUploadBlockCount, requestCount, validBytes);

        FileNodeServer master = nodeAllocator.allocateNode(hash);
        BlockUploadServiceGrpc.BlockUploadServiceStub stub =
                requiredBlockUploadServiceStub(master);
        if (stub == null) {
            // TODO: retry upload
            logger.debug("Server goes down, please re-upload.");
            inputStream.close();
            if (callback != null) {
                callback.onNextStatus(FileObjectUploadStatus.LOST);
            }
            return;
        }

        List<SerializedFileServer> servers = allocateSerializedReplicaServers(hash, master.getId());
        UploadBlocksRequest firstRequest = buildFirstRequest(hash, "", validBytes,
                length, requestCount,
                servers);

        UploadBlocksResponseStreamObserver responseStreamObserver =
                new UploadBlocksResponseStreamObserver(hash, inputStream,
                        master, servers, callback,
                        maxUploadBlockCount, blockSizeInBytes, requestCount);

        StreamObserver<UploadBlocksRequest> requestStreamObserver = stub.uploadBlocks(
                responseStreamObserver);
        responseStreamObserver.setRequestStreamObserver(requestStreamObserver);
        logger.debug("Start requesting for {}, server id={}......", hash, master.getId());
        requestStreamObserver.onNext(firstRequest);
    }

    private BlockUploadServiceGrpc.BlockUploadServiceStub requiredBlockUploadServiceStub(FileNodeServer nodeServer) {
        ManagedChannel channel = nodeChannelPool.getChannel(nodeServer);
        if (channel == null) {
            return null;
        }

        BlockUploadServiceGrpc.BlockUploadServiceStub stub =
                blockUploadServiceStubPool.getStub(nodeServer.getId());
        if (stub != null) {
            return stub;
        }
        stub = BlockUploadServiceGrpc.newStub(channel);
        blockUploadServiceStubPool.registerStub(nodeServer.getId(), stub);
        return stub;
    }

    private UploadBlocksRequest buildFirstRequest(String hash, String crc32,
                                                  long validBytes, long length,
                                                  int requestCount,
                                                  List<SerializedFileServer> replicaServers) {
        UploadBlocksRequest.CheckMessage checkMessage = UploadBlocksRequest.CheckMessage
                .newBuilder()
                .setValidBytes(validBytes)
                .setRequestCount(requestCount)
                .setFileLength(length)
                .addAllReplicaHosts(replicaServers)
                .setCheckValue(crc32)
                .build();
        return UploadBlocksRequest.newBuilder()
                .setIdentity(hash)
                .setCheckMessage(checkMessage)
                .build();
    }

    @Async
    void updatesFileObjectLocation(String hash,
                                   String master,
                                   List<String> replicas) {
        saveReplicaLocation(hash, master, replicas);
        FileStorageLocation location = storageLocationRepository.getByFileId(hash);
        if (location == null) {
            location = new FileStorageLocation(hash, master,
                    replicas.toArray(new String[0]));
            storageLocationRepository.insertOrUpdate(location);
            return;
        }
        FileStorageLocation newLocation = new FileStorageLocation(
                location.getFileId(), master,
                replicas.toArray(new String[0]), location.getBackup() + 1);
        storageLocationRepository.insertOrUpdate(newLocation);
    }

    private void saveReplicaLocation(String hash,
                                     String master,
                                     List<String> replicas) {
        String containerId = MasterReplicaLocation.toContainerId(hash);
        MasterReplicaLocation replicaLocation = masterReplicaLocationRepository
                .getByContainerId(containerId);
        if (replicaLocation != null) {
            MasterReplicaLocation newLocation = new MasterReplicaLocation(containerId,
                    master, replicas.toArray(new String[0]),
                    replicaLocation.getBackup() + 1);
            masterReplicaLocationRepository.insertOrUpdate(newLocation);
            return;
        }
        MasterReplicaLocation newLocation = new MasterReplicaLocation(containerId,
                master,
                replicas.toArray(new String[0]));
        masterReplicaLocationRepository.insertOrUpdate(newLocation);
    }


    private static String[] appendOrCreate(String[] original, String... appends) {
        if (original == null) {
            return appends;
        }
        List<String> create = new ArrayList<>();
        create.addAll(Arrays.asList(original));
        create.addAll(Arrays.asList(appends));
        return create.toArray(new String[0]);
    }

    private class UploadBlocksResponseStreamObserver implements StreamObserver<UploadBlocksResponse> {
        private final FileNodeServer master;
        private final List<String> replicaIds;
        private final FileUploadStatusCallback callback;
        private final InputStream stream;
        private final String fileId;
        private final int maxUploadBlockCount, blockSizeInBytes,
                requestCount;
        private StreamObserverWrapper<UploadBlocksRequest>
                requestStreamObserver;
        private final BufferedStreamIterator iterator;
        private final AtomicInteger requestIndex = new AtomicInteger(0);

        public void setRequestStreamObserver(StreamObserver<UploadBlocksRequest> requestStreamObserver) {
            this.requestStreamObserver = new StreamObserverWrapper<>(requestStreamObserver);
        }

        UploadBlocksResponseStreamObserver(String fileId,
                                           InputStream stream,
                                           FileNodeServer master,
                                           List<SerializedFileServer> replicas,
                                           FileUploadStatusCallback callback,
                                           int maxUploadBlockCount,
                                           int blockSizeInBytes,
                                           int requestCount) {
            this.stream = stream;
            this.fileId = fileId;
            this.master = master;
            this.replicaIds = replicas.stream().map(SerializedFileServer::getId).toList();
            this.callback = callback;
            this.maxUploadBlockCount = maxUploadBlockCount;
            this.blockSizeInBytes = blockSizeInBytes;
            this.requestCount = requestCount;
            iterator = new BufferedStreamIterator(this.stream,
                    this.blockSizeInBytes);
            if (callback != null) {
                callback.onNextStatus(FileObjectUploadStatus.TEMPORARY);
            }
        }

        @Override
        public void onNext(UploadBlocksResponse value) {
            if (requestStreamObserver.isClose()) {
                return;
            }
            if (value.getBlockResponseCase() ==
                    UploadBlocksResponse.BlockResponseCase.BLOCKRESPONSE_NOT_SET) {
                throw new IllegalStateException("Not set on block response");
            }
            if (value.getBlockResponseCase() ==
                    UploadBlocksResponse.BlockResponseCase.FILE_EXISTS) {
                if (value.getFileExists()) {
                    logger.debug("File exists.");
                    if (callback != null) {
                        callback.onNextStatus(FileObjectUploadStatus.AVAILABLE);
                    }
                    requestStreamObserver.onCompleted();
                    return;
                }
                try {
                    sendData();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (callback != null) {
                    callback.onNextStatus(FileObjectUploadStatus.STORING);
                }
                return;
            }

            logger.debug("check block count: {}", value.getBlockCount());
        }

        @Override
        public void onError(Throwable t) {
            // received error needs retry.
            // sets a limit for retry times.
            try {
                stream.close();
            } catch (IOException ignored) {
            }
            if (callback != null) {
                callback.onNextStatus(FileObjectUploadStatus.LOST);
            }
            logger.error("Error receive upload blocks response", t);
        }

        @Override
        public void onCompleted() {
            try {
                stream.close();
            } catch (IOException ignored) {
            }

            // success upload.
            updatesFileObjectLocation(fileId, master.getId(), replicaIds);
            if (callback != null) {
                callback.onNextStatus(FileObjectUploadStatus.SYNCING);
            }
            logger.debug("Upload file complete.");
        }

        private void sendData() throws IOException {
            List<UploadBlockData> blocks = new ArrayList<>();
            logger.debug("Send request.");
            for (int i = 0; i < maxUploadBlockCount; i++) {
                BufferedStreamIterator.Buffer buffer =
                        iterator.nextBuffer();
                UploadBlockData uploadBlockData = UploadBlockData.newBuilder()
                        .setData(ByteString.copyFrom(
                                buffer.getData(), 0, (int) buffer.getSize()))
                        .build();
                blocks.add(uploadBlockData);
                if (buffer.notFilled()) {
                    break;
                }
            }
                if (requestStreamObserver.isClose()) {
                    return;
                }
            try {
                requestStreamObserver.waitForReady();
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for request stream observer ready", e);
                return;
            }
            UploadBlocksRequest request = buildUploadBlocksRequest(fileId, blocks,
                    requestIndex.get());
            requestStreamObserver.onNext(request);
            if (requestIndex.get() + 1 >= requestCount) {
                requestStreamObserver.onCompleted();
            }
            requestIndex.incrementAndGet();
        }
    }

    private List<SerializedFileServer> allocateSerializedReplicaServers(String hash, String masterId) {
        int replicas = calcReplicas();
        List<FileNodeServer> replicaServers = allocatesReplicaServers(hash, replicas, masterId);
        logger.debug("Allocate replicas = {}", replicaServers);
        if (replicaServers.isEmpty()) {
            return List.of();
        }
        return RequestServer.toSerialized(replicaServers);
    }

    private UploadBlocksRequest buildUploadBlocksRequest(String fileId,
                                                         List<UploadBlockData> uploadBlocks,
                                                         int index) {
        UploadBlocksInfo blocks = UploadBlocksInfo.newBuilder()
                .addAllBlocks(uploadBlocks)
                .setIndex(index)
                .build();
        return UploadBlocksRequest.newBuilder()
                .setIdentity(fileId)
                .setUploadBlocks(blocks)
                .build();
    }

    private ReopenableInputStream convertInputStream(InputStream inputStream, Hasher... hashers) throws IOException {
        if (inputStream instanceof ReopenableInputStream) {
            return (ReopenableInputStream) inputStream;
        }

        File tempDir = new File(fileProperties.getTempFilePath());
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        File tempFile = new File(tempDir,
                RandomStringUtils.randomAlphanumeric(20));
        return new ReopenableInputStream(inputStream, tempFile, hashers);
    }

    private void initial() {
        File tempDir = new File(fileProperties.getTempFilePath());
        if (tempDir.exists()) {
            tempDir.delete();
        }
        tempDir.mkdirs();
    }

    // finds replica servers.
    private List<FileNodeServer> allocatesReplicaServers(String hash, int replicas, String masterId) {
        if (replicas <= 0) {
            return List.of();
        }
        Set<String> ids = new HashSet<>();
        ids.add(masterId);
        List<FileNodeServer> servers = new ArrayList<>();
        int reps = replicas, retries = 0;
        for (int i = 0; i < reps; i++) {
            FileNodeServer server =
                    nodeAllocator.allocateNode(hash + "-" + i);
            if (retries >= 20) {
                // still cannot allocate given replicas count after 20 tries.
                return servers;
            }
            if (ids.contains(server.getId())) {
                reps++;
                retries++;
                continue;
            }
            ids.add(server.getId());
            servers.add(server);
            if (servers.size() == replicas) {
                break;
            }
        }
        return servers;
    }

    /**
     * Calculate the number of replicas required.
     *
     * @return replicas count
     */
    private int calcReplicas() {
        int activeServerSize = serverChecker.getActiveServerCount();
        if (activeServerSize == 1) {
            return 0;
        }

        return Math.min(activeServerSize, 2);
        // 1 master with 2 replicas
    }

}
