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

package org.cloudhub.file.fs.meta;

import com.google.protobuf.InvalidProtocolBufferException;
import org.cloudhub.file.fs.container.ContainerNameMeta;
import org.cloudhub.file.fs.container.replica.ReplicaContainerNameMeta;
import org.cloudhub.file.fs.ServerFile;
import org.cloudhub.file.fs.container.ContainerNameMeta;
import org.cloudhub.file.fs.container.ContainerType;
import org.cloudhub.file.fs.container.replica.ReplicaContainerNameMeta;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author RollW
 */
@SuppressWarnings("unused")
public class ContainerMetaFactory {
    public ContainerMetaBuilder createContainerMetaBuilder(ContainerType containerType) {
        return switch (containerType) {
            case ORIGINAL -> new LocalContainerMeta.Builder();
            case REPLICA -> new ReplicaContainerMeta.Builder();
        };
    }

    public ContainerGroupMetaBuilder createContainerGroupMetaBuilder(
            ContainerType containerType) {
        return null;
    }

    private record ContainerNameInfo(
            String name,
            String id,
            long serial,
            String source) {
    }

    public ContainerGroupMeta loadContainerGroupMeta(ServerFile metaFile)
            throws IOException, MetadataException {
        boolean isReplica = ContainerMetaKeys.isReplicaMetaFile(metaFile.getName());
        try (InputStream inputStream = metaFile.openInput()) {
            if (isReplica) {
                return new ReplicaContainerGroupMeta(
                        SerializedReplicaContainerGroupMeta.parseFrom(inputStream)
                );
            }
            return new LocalContainerGroupMeta(
                    SerializedContainerGroupMeta.parseFrom(inputStream)
            );
        } catch (InvalidProtocolBufferException e) {
            throw new MetadataException(e);
        }
    }

    public ContainerMeta loadContainerMeta(ServerFile metaFile,
                                           ContainerLocator containerLocator) throws IOException, MetadataException {
        if (!metaFile.exists()) {
            throw new MetadataLostException(containerLocator.getLocator());
        }

        String locator = removeSuffix(metaFile.getName());
        boolean isReplica = isReplica(locator);
        ContainerNameInfo containerNameInfo = readNameMeta(locator, isReplica);

        try (InputStream inputStream = metaFile.openInput()) {
            SerializedContainerBlockMeta serializedContainerBlockMeta =
                    SerializedContainerBlockMeta.parseFrom(inputStream);

            if (isReplica) {
                return new ReplicaContainerMeta(
                        locator,
                        containerNameInfo.id(),
                        containerNameInfo.serial(),
                        containerLocator.getVersion(),
                        containerNameInfo.source(),
                        serializedContainerBlockMeta
                );
            }
            return new LocalContainerMeta(
                    locator,
                    containerNameInfo.id(),
                    containerNameInfo.serial(),
                    containerLocator.getVersion(),
                    serializedContainerBlockMeta
            );
        } catch (InvalidProtocolBufferException e) {
            throw new MetadataException(e);
        }
    }

    private ContainerNameInfo readNameMeta(String name, boolean replica) throws MetadataException {
        if (replica) {
            try {
                ReplicaContainerNameMeta replicaContainerNameMeta =
                        ReplicaContainerNameMeta.parse(name);
                return new ContainerNameInfo(
                        replicaContainerNameMeta.getName(),
                        replicaContainerNameMeta.getId(),
                        replicaContainerNameMeta.getSerial(),
                        replicaContainerNameMeta.getSourceId()
                );
            } catch (Exception e) {
                throw new MetadataException(e);
            }
        }
        try {
            ContainerNameMeta containerNameMeta = ContainerNameMeta.parse(name);
            return new ContainerNameInfo(
                    containerNameMeta.getName(),
                    containerNameMeta.getId(),
                    containerNameMeta.getSerial(),
                    null
            );
        } catch (Exception e) {
            throw new MetadataException(e);
        }
    }


    private String removeSuffix(String name) {
        return name.substring(0, name.lastIndexOf('.'));
    }

    private boolean isReplica(String name) {
        return ContainerMetaKeys.isReplicaMetaFile(name);
    }
}
