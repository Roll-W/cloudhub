package org.huel.cloudhub.client.controller.server;

import org.huel.cloudhub.client.controller.ValidateHelper;
import org.huel.cloudhub.client.data.dto.fs.ConnectedServers;
import org.huel.cloudhub.client.data.dto.fs.ContainerStatus;
import org.huel.cloudhub.client.service.rpc.FileServerCheckService;
import org.huel.cloudhub.client.service.rpc.ServerInfoCheckService;
import org.huel.cloudhub.client.service.user.UserGetter;
import org.huel.cloudhub.common.ErrorCode;
import org.huel.cloudhub.common.HttpResponseEntity;
import org.huel.cloudhub.server.DiskUsageInfo;
import org.huel.cloudhub.server.NetworkUsageInfo;
import org.huel.cloudhub.server.ServerHostInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author RollW
 */
@RestController
@ServerApi
public class ServerStatusController {
    private final UserGetter userGetter;
    private final FileServerCheckService fileServerCheckService;
    private final ServerInfoCheckService serverInfoCheckService;

    public ServerStatusController(UserGetter userGetter,
                                  FileServerCheckService fileServerCheckService, ServerInfoCheckService serverInfoCheckService) {
        this.userGetter = userGetter;
        this.fileServerCheckService = fileServerCheckService;
        this.serverInfoCheckService = serverInfoCheckService;
    }

    @GetMapping("/connected")
    public HttpResponseEntity<ConnectedServers> getConnectedServers(HttpServletRequest httpServletRequest) {
        var validateMessage =
                ValidateHelper.validateUserAdmin(httpServletRequest, userGetter);
        if (validateMessage != null) {
            return HttpResponseEntity.create(
                    validateMessage.toResponseBody(d -> null));
        }

        ConnectedServers connectedServers =
                fileServerCheckService.getConnectedServers();
        return HttpResponseEntity.success(connectedServers);
    }

    // 获取指定服务器下所有容器信息
    @GetMapping("/container/get")
    public HttpResponseEntity<List<ContainerStatus>> getFileServerContainerStatuses(
            HttpServletRequest request,
            @RequestParam String serverId) {
        var validateMessage =
                ValidateHelper.validateUserAdmin(request, userGetter);
        if (validateMessage != null) {
            return HttpResponseEntity.create(
                    validateMessage.toResponseBody(d -> null));
        }
        if (serverId == null || serverId.isEmpty()) {
            return HttpResponseEntity.failure("Missing of server id.",
                    ErrorCode.ERROR_PARAM_MISSING);
        }
        return HttpResponseEntity.success(
                fileServerCheckService.getContainerStatus(serverId));
    }

    public static final String META_SERVER_ID = "meta";

    @GetMapping("/server/get")
    public HttpResponseEntity<ServerHostInfo> getServerStatus(
            HttpServletRequest request,
            @RequestParam String serverId) {
        var validateMessage =
                ValidateHelper.validateUserAdmin(request, userGetter);
        if (validateMessage != null) {
            return HttpResponseEntity.create(
                    validateMessage.toResponseBody(d -> null));
        }
        if (serverId == null || serverId.isEmpty()) {
            return HttpResponseEntity.failure("Not found server",
                    ErrorCode.ERROR_DATA_NOT_EXIST);
        }
        if (serverId.equalsIgnoreCase(META_SERVER_ID)) {
            return HttpResponseEntity.success(
                    serverInfoCheckService.getMetaServerInfo());
        }
        return HttpResponseEntity.success(
                serverInfoCheckService.getFileServerInfo(serverId));
    }

    @GetMapping("/server/get/net")
    public HttpResponseEntity<List<NetworkUsageInfo>> getServerNetInfos(
            HttpServletRequest request,
            @RequestParam(required = false) String serverId) {
        var validateMessage =
                ValidateHelper.validateUserAdmin(request, userGetter);
        if (validateMessage != null) {
            return HttpResponseEntity.create(
                    validateMessage.toResponseBody(d -> null));
        }
        if (serverId == null || serverId.isEmpty()) {
            return HttpResponseEntity.failure("Not found server",
                    ErrorCode.ERROR_DATA_NOT_EXIST);
        }
        if (serverId.equalsIgnoreCase(META_SERVER_ID)) {
            return HttpResponseEntity.success(
                    serverInfoCheckService.getMetaServerNetRecords());
        }
        return HttpResponseEntity.success(
                serverInfoCheckService.getFileNetRecords(serverId));
    }

    @GetMapping("/server/get/disk")
    public HttpResponseEntity<List<DiskUsageInfo>> getServerDiskInfos(
            HttpServletRequest request,
            @RequestParam(required = false) String serverId) {
        var validateMessage =
                ValidateHelper.validateUserAdmin(request, userGetter);
        if (validateMessage != null) {
            return HttpResponseEntity.create(
                    validateMessage.toResponseBody(d -> null));
        }
        if (serverId == null || serverId.isEmpty()) {
            return HttpResponseEntity.failure("Not found server",
                    ErrorCode.ERROR_DATA_NOT_EXIST);
        }
        if (serverId.equalsIgnoreCase(META_SERVER_ID)) {
            return HttpResponseEntity.success(
                    serverInfoCheckService.getMetaServerDiskRecords());
        }
        return HttpResponseEntity.success(
                serverInfoCheckService.getFileServerDiskRecords(serverId));
    }
}
