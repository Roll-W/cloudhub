package org.huel.cloudhub.client.disk.domain.userstorage.event;

import org.huel.cloudhub.client.disk.domain.operatelog.dto.Operation;
import org.huel.cloudhub.client.disk.domain.operatelog.event.OperationEvent;
import org.huel.cloudhub.client.disk.domain.userstorage.UserFileStorage;

/**
 * @author RollW
 */
public class OnFileCreateEvent extends OperationEvent {
    private final UserFileStorage userFileStorage;

    public OnFileCreateEvent(UserFileStorage userFileStorage) {
        super(buildOperation(userFileStorage));
        this.userFileStorage = userFileStorage;
    }

    public UserFileStorage getUserFileStorage() {
        return userFileStorage;
    }

    private static Operation buildOperation(UserFileStorage userFileStorage) {
        return null;
    }
}
