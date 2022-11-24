package org.huel.cloudhub.client.event.object;

import org.huel.cloudhub.client.data.dto.object.ObjectInfo;
import org.springframework.context.ApplicationEvent;

/**
 * @author RollW
 */
public class ObjectGetEvent extends ApplicationEvent implements ObjectRequestEvent {
    private final ObjectInfo objectInfo;

    public ObjectGetEvent(ObjectInfo objectInfo) {
        super(objectInfo);
        this.objectInfo = objectInfo;
    }

    @Override
    public ObjectInfo getObjectInfo() {
        return objectInfo;
    }
}
