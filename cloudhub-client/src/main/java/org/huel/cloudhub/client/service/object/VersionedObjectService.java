package org.huel.cloudhub.client.service.object;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.huel.cloudhub.client.data.dto.object.ObjectInfo;
import org.huel.cloudhub.client.data.dto.object.ObjectInfoDto;
import org.huel.cloudhub.client.data.entity.object.VersionedObject;
import org.huel.cloudhub.common.MessagePackage;

/**
 * @author RollW
 */
public interface VersionedObjectService {
    MessagePackage<VersionedObject> createNewVersionObject(ObjectInfoDto objectInfoDto);

    void deleteObjectVersion(ObjectInfo info, long version);

    @Nullable
    VersionedObject getObjectVersionOf(ObjectInfo info, long version);


}
