package org.huel.cloudhub.client.disk.domain.systembased;

/**
 * @author RollW
 */
public enum SystemResourceKind {
    FILE,
    FOLDER,
    LINK,
    STORAGE_PERMISSION,
    VERSIONED_FILE,
    VERSIONED_FOLDER,
    STORAGE_SHARE,
    TAG,
    TAG_GROUP,
    USER,
    USER_GROUP,
    ORGANIZATION,
    STORAGE_USER_PERMISSION,
    ;

    public interface Kind {
        SystemResourceKind getSystemResourceKind();
    }

    public static SystemResourceKind from(String nameIgnoreCase) {
        if (nameIgnoreCase == null || nameIgnoreCase.isBlank()) {
            return null;
        }
        return SystemResourceKind.valueOf(nameIgnoreCase.toUpperCase());
    }
}
