package org.huel.cloudhub.client.disk.database;

import org.huel.cloudhub.client.disk.database.dao.ContentTagDao;
import org.huel.cloudhub.client.disk.database.dao.DiskFileStorageDao;
import org.huel.cloudhub.client.disk.database.dao.OperationLogAssociationDao;
import org.huel.cloudhub.client.disk.database.dao.OperationLogDao;
import org.huel.cloudhub.client.disk.database.dao.StorageMetadataDao;
import org.huel.cloudhub.client.disk.database.dao.StoragePermissionDao;
import org.huel.cloudhub.client.disk.database.dao.StorageUserPermissionDao;
import org.huel.cloudhub.client.disk.database.dao.TagGroupDao;
import org.huel.cloudhub.client.disk.database.dao.UserDao;
import org.huel.cloudhub.client.disk.database.dao.UserFolderDao;
import org.huel.cloudhub.client.disk.database.dao.UserFileStorageDao;
import org.huel.cloudhub.client.disk.database.dao.UserShareDao;
import org.huel.cloudhub.client.disk.database.dao.VersionedFileStorageDao;
import org.huel.cloudhub.client.disk.domain.operatelog.OperationLog;
import org.huel.cloudhub.client.disk.domain.operatelog.OperationLogAssociation;
import org.huel.cloudhub.client.disk.domain.share.UserShare;
import org.huel.cloudhub.client.disk.domain.storage.DiskFileStorage;
import org.huel.cloudhub.client.disk.domain.storagepermission.StoragePermission;
import org.huel.cloudhub.client.disk.domain.storagepermission.StorageUserPermission;
import org.huel.cloudhub.client.disk.domain.tag.ContentTag;
import org.huel.cloudhub.client.disk.domain.tag.TagGroup;
import org.huel.cloudhub.client.disk.domain.user.User;
import org.huel.cloudhub.client.disk.domain.userstorage.StorageMetadata;
import org.huel.cloudhub.client.disk.domain.userstorage.UserFolder;
import org.huel.cloudhub.client.disk.domain.userstorage.UserFileStorage;
import org.huel.cloudhub.client.disk.domain.versioned.VersionedFileStorage;
import space.lingu.light.DataConverters;
import space.lingu.light.Database;
import space.lingu.light.LightConfiguration;
import space.lingu.light.LightDatabase;

/**
 * @author RollW
 */
@Database(name = "cloudhub_disk_database", version = 1, tables = {
        User.class,
        DiskFileStorage.class, UserFileStorage.class, UserFolder.class,
        UserShare.class,
        StorageMetadata.class, VersionedFileStorage.class,
        StoragePermission.class, StorageUserPermission.class,
        OperationLog.class, OperationLogAssociation.class,
        ContentTag.class, TagGroup.class,
})
@DataConverters({DiskConverter.class})
@LightConfiguration(key = LightConfiguration.KEY_VARCHAR_LENGTH, value = "255")
public abstract class DiskDatabase extends LightDatabase {
    public abstract UserDao getUserDao();

    public abstract DiskFileStorageDao getDiskFileStorageDao();

    public abstract UserFileStorageDao getUserFileStorageDao();

    public abstract UserFolderDao getUserDirectoryDao();

    public abstract UserShareDao getUserShareDao();

    public abstract StorageMetadataDao getStorageMetadataDao();

    public abstract StoragePermissionDao getStoragePermissionDao();

    public abstract StorageUserPermissionDao getStorageUserPermissionDao();

    public abstract VersionedFileStorageDao getVersionedFileStorageDao();

    public abstract OperationLogDao getOperationLogDao();

    public abstract OperationLogAssociationDao getOperationLogAssociationDao();

    public abstract ContentTagDao getContentTagDao();

    public abstract TagGroupDao getTagGroupDao();
}
