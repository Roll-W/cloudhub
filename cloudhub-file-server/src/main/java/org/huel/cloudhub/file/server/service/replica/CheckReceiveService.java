package org.huel.cloudhub.file.server.service.replica;

import io.grpc.stub.StreamObserver;
import org.huel.cloudhub.file.fs.LockException;
import org.huel.cloudhub.file.fs.container.ContainerChecker;
import org.huel.cloudhub.file.fs.container.ContainerFinder;
import org.huel.cloudhub.file.fs.container.ContainerGroup;
import org.huel.cloudhub.file.rpc.replica.CheckRequest;
import org.huel.cloudhub.file.rpc.replica.CheckResponse;
import org.huel.cloudhub.file.rpc.replica.CheckServiceGrpc;
import org.huel.cloudhub.file.rpc.replica.SerializedContainerStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author RollW
 */
@Service
public class CheckReceiveService extends CheckServiceGrpc.CheckServiceImplBase {
    private final ContainerFinder containerFinder;
    private final ContainerChecker containerChecker;

    public CheckReceiveService(ContainerFinder containerFinder, ContainerChecker containerChecker) {
        this.containerFinder = containerFinder;
        this.containerChecker = containerChecker;
    }

    @Override
    public void checkContainers(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
        final String id = request.getContainerId();
        List<Long> serials = request.getSerialsList();
        final String source = getSource(request);
        ContainerGroup group =
                containerFinder.findContainerGroup(id, source);
        if (group == null) {
            responseObserver.onNext(CheckResponse.newBuilder()
                    .build());
            responseObserver.onCompleted();
            return;
        }
        List<SerializedContainerStatus> statuses = new ArrayList<>();

        group.containers().stream()
                .filter(container -> serials.contains(container.getSerial()))
                .parallel().forEach(container -> {
                    String checkValue = null;
                    try {
                        checkValue = containerChecker.calculateChecksum(container);
                    } catch (LockException | IOException e) {
                        e.printStackTrace();
                    }
                    SerializedContainerStatus status = SerializedContainerStatus.newBuilder()
                            .setMetaDamaged(false)
                            .setSerial(container.getSerial())
                            .setCheckValue(checkValue)
                            .build();
                    statuses.add(status);
                });

        responseObserver.onNext(CheckResponse.newBuilder()
                .addAllStatus(statuses)
                .build());
        responseObserver.onCompleted();

    }

    private String getSource(CheckRequest request) {
        if (request.hasSource()) {
            return request.getSource();
        }
        return ContainerFinder.LOCAL;
    }
}
