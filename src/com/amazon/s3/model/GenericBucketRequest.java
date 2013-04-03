package com.amazon.s3.model;

import com.amazon.s3.AmazonWebServiceRequest;


/**
 * Generic request container for web service requests on buckets.
 */
public class GenericBucketRequest extends AmazonWebServiceRequest {

    private final String bucket;

    public GenericBucketRequest(String bucket) {
        super();
        this.bucket = bucket;
    }

    public String getBucket() {
        return bucket;
    }

}
