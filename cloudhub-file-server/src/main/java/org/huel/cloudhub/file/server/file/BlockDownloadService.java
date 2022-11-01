package org.huel.cloudhub.file.server.file;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.huel.cloudhub.file.fs.LockException;
import org.huel.cloudhub.file.fs.block.ContainerBlock;
import org.huel.cloudhub.file.fs.block.FileBlockMetaInfo;
import org.huel.cloudhub.file.fs.container.ContainerGroup;
import org.huel.cloudhub.file.fs.container.ContainerProvider;
import org.huel.cloudhub.file.fs.container.file.ContainerFileReader;
import org.huel.cloudhub.file.rpc.block.*;
import org.huel.cloudhub.server.file.FileProperties;
import org.huel.cloudhub.util.math.Maths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author RollW
 */
@Service
public class BlockDownloadService extends BlockDownloadServiceGrpc.BlockDownloadServiceImplBase {
    private final Logger logger = LoggerFactory.getLogger(BlockDownloadService.class);
    private final ContainerProvider containerProvider;
    private final FileProperties fileProperties;

    public BlockDownloadService(ContainerProvider containerProvider,
                                FileProperties fileProperties) {
        this.containerProvider = containerProvider;
        this.fileProperties = fileProperties;
    }

    @Override
    public void downloadBlocks(DownloadBlockRequest request,
                               StreamObserver<DownloadBlockResponse> responseObserver) {
        final String fileId = request.getFileId();
        ContainerGroup containerGroup =
                containerProvider.findContainerGroupByFile(fileId);
        FileBlockMetaInfo fileBlockMetaInfo = containerGroup.getFileBlockMetaInfo(fileId);
        if (fileBlockMetaInfo == null) {
            responseObserver.onError(Status.NOT_FOUND.asException());
            responseObserver.onCompleted();
            return;
        }

        final long fileLength = fileBlockMetaInfo.getFileLength();
        final long responseSize = fileProperties.getMaxRequestSizeBytes() >> 1;
        final int responseCount = Maths.ceilDivideReturnsInt(fileLength, responseSize);
        final int maxBlocksInResponse = (int) (responseSize / fileProperties.getBlockSizeInBytes());

        DownloadBlockResponse firstResponse = buildFirstResponse(fileBlockMetaInfo,
                responseCount,
                "0");
        responseObserver.onNext(firstResponse);

        readSendResponse(fileId, containerGroup, fileBlockMetaInfo,
                responseObserver, maxBlocksInResponse, 0);

    }

    // TODO: set by meta-server.
    private static final int RETRY_TIMES = 5;

    private void readSendResponse(String fileId, ContainerGroup containerGroup,
                                  FileBlockMetaInfo fileBlockMetaInfo,
                                  StreamObserver<DownloadBlockResponse> responseObserver,
                                  int maxBlocksInResponse, int retry) {
        if (retry >= RETRY_TIMES) {
            logger.error("send failed because the number of retry times has reached the upper limit.");
            responseObserver.onError(Status.UNAVAILABLE.asException());
            return;
        }
        if (retry != 0) {
            logger.info("retry send download response for file '{}'.", fileId);
        }
        try (ContainerFileReader containerFileReader = new ContainerFileReader(
                containerProvider, fileId, containerGroup, fileBlockMetaInfo)) {
            sendUtilEnd(responseObserver, containerFileReader, maxBlocksInResponse);
            responseObserver.onCompleted();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (LockException e) {
            logger.error("Cannot get container's read lock.", e);
            readSendResponse(fileId, containerGroup, fileBlockMetaInfo,
                    responseObserver, maxBlocksInResponse, retry + 1);
        }
    }

    private void sendUtilEnd(StreamObserver<DownloadBlockResponse> responseObserver,
                             ContainerFileReader fileReader, int readSize) throws IOException, LockException {
        int index = 0;
        while (fileReader.hasNext()) {
            List<ContainerBlock> read = fileReader.read(readSize);
            if (read == null) {
                logger.info("read == null");
                return;
            }
            DownloadBlockResponse response = buildBlockDataResponse(read, index);
            logger.info("send download response. block size ={}", read.size());
            responseObserver.onNext(response);
            index++;
        }
    }

    private DownloadBlockResponse buildFirstResponse(FileBlockMetaInfo fileBlockMetaInfo,
                                                     int responseCount, String checkValue) {
        DownloadBlockResponse.CheckMessage checkMessage = DownloadBlockResponse.CheckMessage
                .newBuilder()
                .setFileLength(fileBlockMetaInfo.getFileLength())
                .setResponseCount(responseCount)
                .setValidBytes(fileBlockMetaInfo.getValidBytes())
                .setCheckValue(checkValue)
                .build();
        return DownloadBlockResponse.newBuilder()
                .setCheckMessage(checkMessage)
                .build();
    }

    private DownloadBlockResponse buildBlockDataResponse(List<ContainerBlock> containerBlocks, int index) {
        List<DownloadBlockData> downloadBlockData = new LinkedList<>();
        containerBlocks.forEach(containerBlock -> {
            downloadBlockData.add(DownloadBlockData.newBuilder()
                    .setData(ByteString.copyFrom(
                            containerBlock.getData(), 0,
                            (int) containerBlock.getValidBytes())
                    )
                    .build());
            containerBlock.release();
        });
        DownloadBlocksInfo downloadBlocksInfo = DownloadBlocksInfo.newBuilder()
                .addAllData(downloadBlockData)
                .setIndex(index)
                .build();
        return DownloadBlockResponse.newBuilder()
                .setDownloadBlocks(downloadBlocksInfo)
                .build();
    }


}
