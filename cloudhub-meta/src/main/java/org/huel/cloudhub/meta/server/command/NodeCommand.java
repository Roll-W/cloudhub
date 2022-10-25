package org.huel.cloudhub.meta.server.command;

import org.huel.cloudhub.meta.server.service.node.HeartbeatService;
import org.huel.cloudhub.meta.server.service.node.HeartbeatWatcher;
import org.huel.cloudhub.meta.server.service.node.NodeServer;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.util.List;

/**
 * 调试用
 *
 * @author RollW
 */
@ShellComponent
public class NodeCommand extends AbstractShellComponent {
    private final HeartbeatService heartbeatService;

    public NodeCommand(HeartbeatService heartbeatService) {
        this.heartbeatService = heartbeatService;
    }

    @ShellMethod(value = "node operations.", key = {"node"})
    public void nodeAction(
            @ShellOption(help = "options of node", defaultValue = "show",
                    value = {"--option", "-o"}) String option) throws IOException {
        if (option == null) {
            getTerminal().writer().println("no option provide");
            return;
        }
        switch (option) {
            case "show" -> showActiveNodes();
            case "heartbeat" -> showActiveHeartbeatWatchers();
            default -> getTerminal().writer().println("Unknown node command '%s'.".formatted(option));
        }
        getTerminal().writer().flush();
    }

    private void showActiveNodes() {
        List<NodeServer> activeNodes = heartbeatService.activeServers();
        getTerminal().writer().println("shows all active nodes: active nodes count = [%d]"
                .formatted(activeNodes.size()));
        activeNodes
                .forEach(getTerminal().writer()::println);
        getTerminal().writer().flush();
    }

    private void showActiveHeartbeatWatchers() {
        List<HeartbeatWatcher> heartbeatWatchers =
                heartbeatService.activeHeartbeatWatchers();
        getTerminal().writer().println("shows all active heartbeat watchers: active watchers count = [%d]"
                .formatted(heartbeatWatchers.size()));
        heartbeatWatchers
                .forEach(getTerminal().writer()::println);
        getTerminal().writer().flush();
    }
}