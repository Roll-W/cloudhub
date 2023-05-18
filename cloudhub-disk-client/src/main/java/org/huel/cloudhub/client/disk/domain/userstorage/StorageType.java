package org.huel.cloudhub.client.disk.domain.userstorage;

import org.huel.cloudhub.client.disk.domain.systembased.SystemResourceKind;

/**
 * @author RollW
 */
public enum StorageType implements SystemResourceKind.Kind {
    FILE(SystemResourceKind.FILE),
    FOLDER(SystemResourceKind.FOLDER),
    LINK(SystemResourceKind.LINK);

    private final SystemResourceKind systemResourceKind;

    StorageType(SystemResourceKind systemResourceKind) {
        this.systemResourceKind = systemResourceKind;
    }

    public boolean isFile() {
        return this == FILE;
    }

    public static StorageType from(String nameIgnoreCase) {
        for (StorageType value : values()) {
            if (value.name().equalsIgnoreCase(nameIgnoreCase)) {
                return value;
            }
        }
        return null;
    }

    public static StorageType from(SystemResourceKind systemResourceKind) {
        for (StorageType value : values()) {
            if (value.getSystemResourceKind() == systemResourceKind) {
                return value;
            }
        }
        return null;
    }

    @Override
    public SystemResourceKind getSystemResourceKind() {
        return systemResourceKind;
    }
}
