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

import java.util.Collection;

/**
 * @author RollW
 */
public interface ServerChecker {
    Collection<FileNodeServer> getActiveServers();

    Collection<FileNodeServer> getDeadServers();

    int getActiveServerCount();

    boolean isActive(@Nullable FileNodeServer nodeServer);

    boolean isActive(@Nullable String serverId);
}