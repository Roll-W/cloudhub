package org.huel.cloudhub.file.server.service.replica;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.huel.cloudhub.file.fs.LockException;
import org.huel.cloudhub.file.fs.block.Block;
import org.huel.cloudhub.file.fs.container.Container;
import org.huel.cloudhub.file.fs.container.ContainerChecker;
import org.huel.cloudhub.file.fs.container.ContainerWriter;
import org.huel.cloudhub.file.fs.container.ContainerWriterOpener;
import org.huel.cloudhub.file.fs.container.replica.ReplicaContainerCreator;
import org.huel.cloudhub.file.fs.container.replica.ReplicaContainerDeleter;
import org.huel.cloudhub.file.fs.container.replica.ReplicaContainerNameMeta;
import org.huel.cloudhub.file.io.IoUtils;
import org.huel.cloudhub.file.rpc.replica.*;
import org.huel.cloudhub.file.server.service.SourceServerGetter;
import org.huel.cloudhub.rpc.StreamObserverWrapper;
import org.huel.cloudhub.server.rpc.server.SerializedFileServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author RollW
 */
@Service
public class ReplicaReceiveService extends ReplicaServiceGrpc.ReplicaServiceImplBase {
    private final Logger logger = LoggerFactory.getLogger(ReplicaReceiveService.class);
    private final ReplicaContainerCreator replicaContainerCreator;
    private final ReplicaContainerDeleter replicaContainerDeleter;
    private final ContainerWriterOpener containerWriterOpener;
    private final ContainerChecker containerChecker;
    private final SourceServerGetter.ServerInfo serverInfo;

    public ReplicaReceiveService(ReplicaContainerCreator replicaContainerCreator,
                                 ReplicaContainerDeleter replicaContainerDeleter,
                                 ContainerWriterOpener containerWriterOpener,
                                 ContainerChecker containerChecker,
                                 SourceServerGetter sourceServerGetter) {
        this.replicaContainerCreator = replicaContainerCreator;
        this.replicaContainerDeleter = replicaContainerDeleter;
        this.containerWriterOpener = containerWriterOpener;
        this.containerChecker = containerChecker;
        this.serverInfo = sourceServerGetter.getLocalServer();
    }

    @Override
    public StreamObserver<ReplicaRequest> sendReplica(StreamObserver<ReplicaResponse> responseObserver) {
        return new ReplicaRequestObserver(responseObserver);
    }

    @Override
    public void deleteReplica(ReplicaDeleteRequest request, StreamObserver<ReplicaDeleteResponse> responseObserver) {
        logger.debug("Receive delete request, id={};serial={};source={}",
                request.getId(), request.getSerial(), request.getSource());
        try {
            replicaContainerDeleter.deleteReplicaContainer(
                    request.getId(),
                    request.getSerial(),
                    request.getSource());
        } catch (IOException e) {
            logger.debug("Delete replica error.", e);
        }
        responseObserver.onNext(ReplicaDeleteResponse.newBuilder()
                .build());
        responseObserver.onCompleted();
    }

    private boolean checkId(String id) {
        return serverInfo.id().equals(id);
    }

    private class ReplicaRequestObserver implements StreamObserver<ReplicaRequest> {
        private final StreamObserverWrapper<ReplicaResponse> responseObserver;
        private ContainerWriter containerWriter;
        private Container replicaContainer;

        public ReplicaRequestObserver(StreamObserver<ReplicaResponse> responseObserver) {
            this.responseObserver = new StreamObserverWrapper<>(responseObserver);
        }

        private void saveCheckMessage(ReplicaRequest.CheckMessage checkMessage) throws LockException, IOException {
            if (containerWriter != null) {
                containerWriter.close();
            }
            if (checkId(checkMessage.getSource().getId())) {
                responseObserver.onError(Status.INVALID_ARGUMENT.asException());
                return;
            }
            ReplicaContainerNameMeta nameMeta = new ReplicaContainerNameMeta(
                    checkMessage.getSource().getId(),
                    checkMessage.getId(),
                    checkMessage.getSerial());
            long version = checkMessage.getVersion();
            replicaContainer = replicaContainerCreator.findOrCreateContainer(nameMeta.getId(),
                    nameMeta.getSourceId(), nameMeta.getSerial(), checkMessage.getBlockMeta());
            replicaContainer.updatesVersion(version);
            replicaContainerCreator.createContainerWithMeta(replicaContainer, checkMessage.getBlockMeta());
            containerWriter = new ContainerWriter(replicaContainer, containerWriterOpener);
        }

        private boolean checkLastCheckMessage(ReplicaRequest replicaRequest) {
            ReplicaRequest.CheckMessage checkMessage = replicaRequest.getCheckMessage();
            if (!checkMessage.hasLastReq()) {
                return false;
            }
            return checkMessage.getLastReq();
        }

        @Override
        public void onNext(ReplicaRequest value) {
            if (value.getReplicaMessageCase() == ReplicaRequest.ReplicaMessageCase.REPLICAMESSAGE_NOT_SET) {
                responseObserver.onError(Status.INVALID_ARGUMENT.asException());
                return;
            }
            if (value.getReplicaMessageCase() == ReplicaRequest.ReplicaMessageCase.CHECK_MESSAGE) {
                sendCheckValue(replicaContainer);// TODO: check value
                if (checkLastCheckMessage(value)) {
                    logger.debug("Last replica request, close observer.");
                    responseObserver.onCompleted();
                    return;
                }

                ReplicaRequest.CheckMessage checkMessage = value.getCheckMessage();
                SerializedFileServer server = checkMessage.getSource();
                // means a new container replica.
                try {
                    saveCheckMessage(checkMessage);
                } catch (LockException | IOException e) {
                    logger.error("Error occurred while initialize message.", e);
                }

                logger.debug("Received message, source server={};address={}:{}",
                        server.getId(), server.getHost(), server.getPort());
                return;
            }
            trySeek(value.getReplicaDataInfo(), containerWriter);
            List<Block> blocks = toBlockList(value.getReplicaDataInfo().getDataList());
            try {
                containerWriter.write(blocks, true);
            } catch (IOException e) {
                responseObserver.onError(Status.INTERNAL.asRuntimeException());
                logger.error("Error: write error, message=%s".formatted(e.getMessage()), e);
            }
            logger.debug("Write replica data, size={}", blocks.size());
        }


        @Override
        public void onError(Throwable t) {
            logger.debug("Error occurred while receive replicas.", t);
        }

        @Override
        public void onCompleted() {
            logger.info("On completed.");
            IoUtils.closeQuietly(containerWriter);
        }

        private void sendCheckValue(Container container) {
            if (container == null) {
                return;
            }
            logger.debug("Send check value: cont={}.", container.getResourceLocator());
            try {
                String value = containerChecker.calculateChecksum(container);
                responseObserver.onNext(ReplicaResponse.newBuilder()
                        .setCheckValue(value).build());
            } catch (LockException | IOException e) {
                logger.error("Container crc32 calc failed.", e);
                responseObserver.onError(e);
            }
        }
    }

    private List<Block> toBlockList(List<ReplicaData> replicaDataList) {
        List<Block> blocks = new ArrayList<>();
        for (ReplicaData replicaData : replicaDataList) {
            byte[] data = replicaData.getData().toByteArray();
            Block block = new Block(data, data.length);
            blocks.add(block);
        }
        return blocks;
    }

    private void trySeek(SerializedReplicaDataInfo dataInfo,
                         ContainerWriter writer) {
        if (!dataInfo.hasBlockInfo()) {
            try {
                writer.seek(0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        int i = dataInfo.getBlockInfo().getStartIndex();
        if (i < 0) {
            return;
        }
        try {
            writer.seek(i);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
