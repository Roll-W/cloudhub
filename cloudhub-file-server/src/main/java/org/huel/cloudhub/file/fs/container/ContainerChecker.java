package org.huel.cloudhub.file.fs.container;

import org.huel.cloudhub.file.fs.LockException;
import org.huel.cloudhub.file.fs.ServerFile;
import org.huel.cloudhub.file.fs.container.validate.ContainerStatues;
import org.huel.cloudhub.file.fs.meta.MetadataException;

import java.io.IOException;

/**
 * @author RollW
 */
public interface ContainerChecker extends ChecksumCalculator {
    ContainerStatues checkContainer(String containerLocator) throws IOException, MetadataException;

    String calculateChecksum(Container container) throws LockException, IOException;

    @Override
    String calculateChecksum(ServerFile file) throws IOException;
}
