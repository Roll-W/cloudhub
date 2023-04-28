package org.huel.cloudhub.objectstorage.service.object;

import org.huel.cloudhub.objectstorage.data.dto.object.ObjectInfo;
import org.huel.cloudhub.objectstorage.data.dto.object.ObjectInfoDto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author RollW
 */
public interface ObjectService {
    ObjectInfoDto saveObject(ObjectInfo objectInfo, InputStream stream) throws IOException;

    void getObjectData(ObjectInfo objectInfo, OutputStream stream) throws IOException;

    void getObjectData(ObjectInfo objectInfo, OutputStream stream, long startBytes, long endBytes) throws IOException;

    void deleteObject(ObjectInfo objectInfo);

    void clearBucketObjects(String bucketName);

    List<ObjectInfoDto> getObjectsInBucket(String bucketName);

    ObjectInfoDto getObjectInBucket(String bucketName, String objectName);

    void setObjectFileId(String bucketName, String objectName, String fileId);

    ObjectInfoDto renameObject(ObjectInfo oldInfo, String newName);

    boolean isObjectExist(ObjectInfo objectInfo);

    int getObjectsCount();
}