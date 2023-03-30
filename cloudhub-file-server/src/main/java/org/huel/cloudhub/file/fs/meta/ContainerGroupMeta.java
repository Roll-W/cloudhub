package org.huel.cloudhub.file.fs.meta;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * @author RollW
 */
public interface ContainerGroupMeta {
    List<? extends ContainerLocator> getChildLocators();

    ContainerLocator getChildLocator(String locator);

    List<? extends ContainerMeta> loadChildContainerMeta(MetadataLoader loader)
            throws IOException, MetadataException;

    ContainerMeta loadChildContainerMeta(MetadataLoader loader, String locator)
            throws IOException, MetadataException;

    String getSource();

    void writeTo(OutputStream outputStream) throws IOException;
}
