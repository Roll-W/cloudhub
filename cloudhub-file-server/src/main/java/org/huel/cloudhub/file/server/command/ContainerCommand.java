package org.huel.cloudhub.file.server.command;

import org.huel.cloudhub.file.fs.container.Container;
import org.huel.cloudhub.file.server.file.ContainerService;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.util.Collection;

/**
 * @author RollW
 */
@ShellComponent
public class ContainerCommand extends AbstractShellComponent {
    private final ContainerService containerService;

    public ContainerCommand(ContainerService containerService) {
        this.containerService = containerService;
    }

    @ShellMethod(value = "container operations.", key = {"cont"})
    public void nodeAction(
            @ShellOption(help = "options of container", defaultValue = "show",
                    value = {"--option", "-o"}) String option) throws IOException {
        if (option == null) {
            getTerminal().writer().println("no option provide");
            return;
        }
        if ("show".equals(option)) {
            showContainers();
        } else {
            showContainers(option);
        }
        getTerminal().writer().flush();
    }

    private void showContainers() {
        Collection<Container> containers = containerService.listContainers();
        getTerminal().writer().println("shows all containers.\tcount = [%d]"
                .formatted(containers.size()));
        containers
                .forEach(getTerminal().writer()::println);
        getTerminal().writer().flush();
    }

    private void showContainers(String id) {
        Collection<Container> containers = containerService.listContainers(id);
        getTerminal().writer().println("shows containers of %s.\tcount = [%d]"
                .formatted(id, containers.size()));
        containers
                .forEach(getTerminal().writer()::println);
        getTerminal().writer().flush();
    }


}
