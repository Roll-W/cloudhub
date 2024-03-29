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

package org.cloudhub.meta.server.service.node;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudhub.meta.server.service.node.util.ConsistentHashServerMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author RollW
 */
public class RegisterNodeAllocator implements
        ServerEventRegistry.ServerEventCallback, NodeAllocator, NodeWeightUpdater {
    private final NodeWeightProvider nodeWeightProvider;
    private final Map<String, FileNodeServer> nodeServers =
            new ConcurrentHashMap<>();
    private final ConsistentHashServerMap<FileNodeServer> serverConsistentHashMap =
            new ConsistentHashServerMap<>();

    public RegisterNodeAllocator(NodeWeightProvider nodeWeightProvider) {
        this.nodeWeightProvider = nodeWeightProvider;
    }

    @Override
    public void registerNodeServer(FileNodeServer nodeServer) {
        if (nodeServers.containsKey(nodeServer.getId())) {
            return;
        }
        nodeServers.put(nodeServer.getId(), nodeServer);
    }

    @Override
    public FileNodeServer allocateNode(String fileId) {
        if (nodeServers.isEmpty()) {
            throw new NodeServerException("No file server connected.");
        }

        FileNodeServer nodeServer =  serverConsistentHashMap.allocateServer(fileId);
        if (nodeServer == null) {
            throw new NodeServerException("No file server available, " +
                    "probably all servers are down or no storage space available.");
        }

        return nodeServer;
    }

    @Override
    @Nullable
    public FileNodeServer findNodeServer(String serverId) {
        return nodeServers.get(serverId);
    }

    @Override
    public void registerServer(FileNodeServer server) {
        registerNodeServer(server);
    }

    @Override
    public void removeActiveServer(FileNodeServer nodeServer) {
        serverConsistentHashMap.removeServer(nodeServer);
        // refresh server's weight at a fixed rate
    }

    @Override
    public void addActiveServer(FileNodeServer nodeServer) {
        int weight = nodeWeightProvider.getWeightOf(nodeServer);
        serverConsistentHashMap.addServer(nodeServer, weight);
    }

    @Override
    public void onNewNodeWeight(String nodeId, int weight) {
        FileNodeServer nodeServer = nodeServers.get(nodeId);
        if (nodeServer == null) {
            return;
        }
        serverConsistentHashMap.setServerWeight(nodeServer, weight);
    }
}
