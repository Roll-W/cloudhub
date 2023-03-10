package org.huel.cloudhub.meta.server.service.node;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author RollW
 */
public interface NodeAllocator {
    /**
     * 依照哈希值分配{@link NodeServer}。不检验文件是否存在。
     */
    NodeServer allocateNode(String fileId);

    @Nullable
    NodeServer findNodeServer(String serverId);

    void registerNodeServer(NodeServer server);
}
