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

package org.cloudhub.meta.server.service.synchro;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.cloudhub.meta.server.data.database.repository.FileStorageLocationRepository;
import org.cloudhub.meta.server.data.entity.FileStorageLocation;
import org.cloudhub.meta.server.service.node.HeartbeatService;
import org.cloudhub.meta.server.service.node.NodeAllocator;
import org.cloudhub.meta.server.service.node.FileNodeServer;
import org.cloudhub.meta.server.service.node.ServerChecker;
import org.cloudhub.meta.server.service.node.ServerEventRegistry;
import org.cloudhub.meta.util.ScheduledCountdownTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * When server lost connection, start a timer to check the file status to synchro
 * the file parts to other servers.
 *
 * @author RollW
 */
@SuppressWarnings("all")
public class FileSynchroListener implements ServerEventRegistry.ServerEventCallback {
    private final FileStorageLocationRepository fileRepository;
    private final SynchroDispatchService synchroDispatchService;
    private final NodeAllocator nodeAllocator;
    private final ServerChecker serverChecker;

    private final Map<FileNodeServer, ScheduledCountdownTimer>
            synchroTimerMap = new ConcurrentHashMap<>();

    public FileSynchroListener(FileStorageLocationRepository fileRepository,
                               SynchroDispatchService synchroDispatchService,
                               HeartbeatService heartbeatService) {
        this.fileRepository = fileRepository;
        this.synchroDispatchService = synchroDispatchService;
        this.nodeAllocator = heartbeatService.getNodeAllocator();
        this.serverChecker = heartbeatService.getServerChecker();
    }

    @Override
    public void registerServer(FileNodeServer server) {
        // do nothing.
    }

    @Override
    public void removeActiveServer(FileNodeServer nodeServer) {
        ScheduledCountdownTimer timer = synchroTimerMap.get(nodeServer);
        if (timer != null) {
            timer.stop();
            timer.reset();
        }
        if (timer == null) {
            timer = new ScheduledCountdownTimer(1000 * 60 * 5, new SynchroTask(nodeServer));
            synchroTimerMap.put(nodeServer, timer);
        }
        timer.start();
    }

    @Override
    public void addActiveServer(FileNodeServer nodeServer) {
        ScheduledCountdownTimer timer = synchroTimerMap.get(nodeServer);
        if (timer == null) {
            return;
        }
        timer.stop();
    }

    private class SynchroTask implements Runnable {
        private final FileNodeServer nodeServer;

        private SynchroTask(FileNodeServer nodeServer) {
            this.nodeServer = nodeServer;
        }

        @Override
        public void run() {
            List<FileParts> lostFileParts = checkLostFileParts();
            if (lostFileParts.isEmpty()) {
                return;
            }
            Multimap<FileNodeServer, FileParts> serverMap =
                    Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
            for (FileParts lostFilePart : lostFileParts) {
                FileNodeServer server = nodeAllocator.allocateNode(lostFilePart.fileId());
                if (server == null) {
                    continue;
                }
                serverMap.put(server, lostFilePart);
            }
            serverMap.asMap().forEach((server, fileIds) -> {
                synchroDispatchService.dispatchSynchro(
                        server,
                        fileIds.stream().map(FileParts::fileId).toList(),
                        null
                );
            });
        }


        private List<FileParts> checkLostFileParts() {
            List<FileStorageLocation> locations =
                    fileRepository.getLocationsByServerId(nodeServer.getId());
            List<FileParts> fileParts = new ArrayList<>();
            for (FileStorageLocation location : locations) {
                List<FileNodeServer> activeReplicas =
                        location.getServerList()
                                .stream()
                                .filter(serverChecker::isActive)
                                .map(nodeAllocator::findNodeServer)
                                .toList();
                if (activeReplicas.size() > 2) {
                    continue;
                }
                FileParts file = new FileParts(
                        location.getFileId(),
                        location.getMasterServerId(),
                        activeReplicas);
                fileParts.add(file);
            }
            return fileParts;
        }
    }

    private record FileParts(
            String fileId,
            String source,
            List<FileNodeServer> activeReplicas) {
    }


    // TODO: not formal impl it yet.
}
