package org.huel.cloudhub.client.event.bucket;

import org.huel.cloudhub.client.data.dto.bucket.BucketInfo;
import org.springframework.context.ApplicationEvent;

/**
 * @author RollW
 */
public class OnBucketDeleteEvent extends ApplicationEvent {
    private final BucketInfo bucketInfo;

    public OnBucketDeleteEvent(BucketInfo bucketInfo) {
        super(bucketInfo);
        this.bucketInfo = bucketInfo;
    }

    public BucketInfo getBucketInfo() {
        return bucketInfo;
    }
}
