package org.huel.cloudhub.file.server.service.container;

import org.huel.cloudhub.file.fs.FileAllocator;
import org.huel.cloudhub.file.fs.LocalFileServer;
import org.huel.cloudhub.file.fs.ServerFile;
import org.huel.cloudhub.file.fs.block.BlockMetaInfo;
import org.huel.cloudhub.file.fs.container.*;
import org.huel.cloudhub.file.fs.container.replica.ReplicaContainerCreator;
import org.huel.cloudhub.file.fs.container.replica.ReplicaContainerLoader;
import org.huel.cloudhub.file.fs.container.replica.ReplicaContainerNameMeta;
import org.huel.cloudhub.file.fs.container.replica.ReplicaGroup;
import org.huel.cloudhub.file.fs.meta.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author RollW
 */
@Service
public class ReplicaContainerDelegate implements ReplicaContainerLoader, ReplicaContainerCreator {
    private final Map<String, ReplicaGroup> replicaContainerGroups =
            new ConcurrentHashMap<>();
    private final ContainerProperties containerProperties;
    private final LocalFileServer localFileServer;
    private final ServerFile containerDir;

    public ReplicaContainerDelegate(ContainerProperties containerProperties,
                                    LocalFileServer localFileServer) {
        this.containerProperties = containerProperties;
        this.localFileServer = localFileServer;
        this.containerDir =
                localFileServer.getServerFileProvider().openFile(containerProperties.getContainerPath());
    }

    @Override
    public void loadInReplicaContainers(SerializedContainerGroupMeta containerMeta) throws IOException {
        for (SerializedContainerMeta serializedContainerMeta : containerMeta.getMetaList()) {
            Container container = loadInContainer(serializedContainerMeta);
            updatesContainer(container);
        }
    }

    private void updatesContainer(Container container) {
        ReplicaGroup replicaGroup = replicaContainerGroups.get(container.getSource());
        if (replicaGroup == null) {
            replicaGroup = new ReplicaGroup(container.getSource());
            replicaContainerGroups.put(container.getSource(), replicaGroup);
        }
        replicaGroup.put(container);
    }

    private Container loadInContainer(SerializedContainerMeta serializedContainerMeta) throws IOException {
        final String locator = serializedContainerMeta.getLocator();
        ServerFile file = localFileServer.getServerFileProvider()
                .openFile(containerDir, locator);
        ServerFile metaFile = localFileServer.getServerFileProvider()
                .openFile(containerDir, locator + ContainerLocation.META_SUFFIX);
        SerializedContainerBlockMeta containerBlockMeta = MetaReadWriteHelper.readContainerBlockMeta(metaFile);
        ReplicaContainerNameMeta nameMeta = ReplicaContainerNameMeta.parse(locator);

        List<BlockMetaInfo> blockMetaInfos = new ArrayList<>();
        containerBlockMeta.getBlockMetasList().forEach(serializeBlockFileMeta ->
                blockMetaInfos.add(BlockMetaInfo.deserialize(serializeBlockFileMeta, nameMeta.getSerial())));

        ContainerIdentity identity = buildIdentityFrom(nameMeta, containerBlockMeta);
        ContainerLocation location =
                new ContainerLocation(file.getPath());

        return new Container(location, nameMeta.getSourceId(), containerBlockMeta.getUsedBlock(),
                identity, blockMetaInfos, serializedContainerMeta.getVersion(), true);
    }

    @Override
    public Container findOrCreateContainer(String id, String source, long serial, SerializedContainerBlockMeta serializedContainerBlockMeta) throws IOException {
        final ReplicaGroup replicaGroup = replicaContainerGroups
                .computeIfAbsent(source, ReplicaGroup::new);
        Container container = replicaGroup.getContainer(id, serial);
        if (container != null) {
            return container;
        }
        container = createReplicaContainer(id, source, serial, serializedContainerBlockMeta);
        replicaGroup.put(container);
        // TODO: add to .rcmeta file to allow be indexed.
        return container;
    }

    private void updatesToReplicaGroupIndex() {

    }

    private Container createReplicaContainer(String id,
                                             String source,
                                             long serial,
                                             SerializedContainerBlockMeta containerBlockMeta) throws IOException {
        final String containerId = ContainerIdentity.toContainerId(id);
        ReplicaContainerNameMeta containerNameMeta =
                new ReplicaContainerNameMeta(source, containerId, serial);
        ContainerLocation location = new ContainerLocation(
                toDataPath(containerNameMeta.getName()),
                ContainerLocation.REPLICA_META_SUFFIX);
        ContainerIdentity identity = buildIdentityFrom(containerNameMeta, containerBlockMeta);
        return new Container(location, source,
                containerBlockMeta.getUsedBlock(),
                identity, List.of(), 1, false);
    }

    private ContainerIdentity buildIdentityFrom(ReplicaContainerNameMeta nameMeta,
                                                SerializedContainerBlockMeta containerBlockMeta) {
        return new ContainerIdentity(
                nameMeta.getId(), containerBlockMeta.getCrc(),
                nameMeta.getSerial(), containerBlockMeta.getBlockCap(),
                containerBlockMeta.getBlockSize()
        );
    }

    private String toDataPath(String name) {
        return containerProperties.getContainerPath() + File.separator + name;
    }

    @Override
    public void createContainerWithMeta(Container container,
                                        SerializedContainerBlockMeta serializedContainerBlockMeta) throws IOException {
        containerDir.mkdirs();
        ServerFile containerFile = localFileServer.getServerFileProvider()
                .openFile(containerDir, container.getResourceLocator());
        boolean containerExists = container.isUsable();
        containerFile.createFile();
        ServerFile metaFile = localFileServer.getServerFileProvider()
                .openFile(containerDir, container.getResourceLocator() + ContainerLocation.REPLICA_META_SUFFIX);
        metaFile.createFile();
        if (!containerExists) {
            // first time create.
            allocateContainerSize(container);
            container.setUsable();
            updateContainerGroupMeta(container);
        }
        MetaReadWriteHelper.writeContainerBlockMeta(serializedContainerBlockMeta, metaFile);
    }

    private void allocateContainerSize(Container container) throws IOException {
        try (FileAllocator allocator = new FileAllocator(container.getLocation())) {
            allocator.allocateSize(container.getLimitBytes());
        }
    }

    public List<Container> listContainers() {
        return replicaContainerGroups
                .values().stream()
                .flatMap(replicaGroup ->
                        replicaGroup.listGroup().stream())
                .flatMap(containerGroup ->
                        containerGroup.containers().stream())
                .toList();
    }

    private void updateContainerGroupMeta(Container container) throws IOException {
        if (!container.isUsable()) {
            return;
        }
        SerializedReplicaContainerMeta meta = SerializedReplicaContainerMeta.newBuilder()
                .setLocator(container.getResourceLocator())
                .setVersion(container.getVersion())
                .build();
        writeContainerMeta(meta);
    }

    @Async
    void writeContainerMeta(SerializedReplicaContainerMeta containerMeta) throws IOException {
        ReplicaContainerNameMeta fileNameMeta =
                ReplicaContainerNameMeta.parse(containerMeta.getLocator());
        String fileName = ContainerIdentity.toCmetaId(fileNameMeta.getId());

        ServerFile file = localFileServer.getServerFileProvider()
                .openFile(containerProperties.getMetaPath(), fileName + ContainerMetaKeys.CONTAINER_META_SUFFIX);
        file.createFile();
        SerializedReplicaContainerGroupMeta containerGroupMeta = MetaReadWriteHelper.readReplicaContainerMeta(file);

        List<SerializedReplicaContainerMeta> containerMetas = new ArrayList<>(containerGroupMeta.getMetaList());
        containerMetas.removeIf(serializedContainerMeta ->
                serializedContainerMeta.getLocator().equals(containerMeta.getLocator()));
        containerMetas.add(containerMeta);

        containerGroupMeta = SerializedReplicaContainerGroupMeta.newBuilder()
                .addAllMeta(containerMetas)
                .build();

        MetaReadWriteHelper.writeReplicaContainerGroupMeta(containerGroupMeta, file);
    }
}
