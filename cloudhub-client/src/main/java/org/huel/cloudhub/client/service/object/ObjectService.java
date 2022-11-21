package org.huel.cloudhub.client.service.object;

import org.huel.cloudhub.client.data.dto.object.ObjectInfo;
import org.huel.cloudhub.common.MessagePackage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author RollW
 */
public interface ObjectService {
    MessagePackage<ObjectInfo> saveObject(ObjectInfo objectInfo, InputStream stream) throws IOException;

    MessagePackage<Void> getObjectData(ObjectInfo objectInfo, OutputStream stream);

    MessagePackage<Void> deleteObject(ObjectInfo objectInfo);

    MessagePackage<Void> clearBucketObjects(String bucketName);

    List<ObjectInfo> getObjectsInBucket(String bucketName);
}
