package org.huel.cloudhub.file.server.service.id;

import com.google.common.io.Files;
import org.huel.cloudhub.file.fs.container.ContainerProperties;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author RollW
 */
@Service
public class ServerIdService {
    private final UUID uuid;
    private final String uuidString;

    private final ContainerProperties containerProperties;

    public static final String ID_KEY = "fid";


    public ServerIdService(ContainerProperties containerProperties) throws IOException {
        this.containerProperties = containerProperties;
        uuid = initialId();
        uuidString = uuid.toString();
    }

    private UUID initialId() throws IOException {
        // TODO: move to properties

        File file = new File(containerProperties.getFilePath(), "ID_VERSION");
        if (!file.exists()) {
            file.createNewFile();
            UUID uid = UUID.randomUUID();
            Files.write(uid.toString().getBytes(StandardCharsets.UTF_8), file);
            return uid;
        }
        String firstLine = Files.asCharSource(file, StandardCharsets.UTF_8).readFirstLine();
        if (firstLine == null) {
            boolean delete = file.delete();
            if (!delete) {
                throw new IllegalStateException("failed delete ID_VERSION file.");
            }
            return initialId();
        }
        return UUID.fromString(firstLine);
    }

    public String getServerId() {
        return uuidString;
    }

    public UUID getServerUuid() {
        return uuid;
    }
}
