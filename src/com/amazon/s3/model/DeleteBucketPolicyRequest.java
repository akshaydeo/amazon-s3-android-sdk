package com.amazon.s3.model;

import com.amazon.s3.AmazonWebServiceRequest;


/**
 * Deletes the policy associated with the specified bucket. Only the owner of
 * the bucket can delete the bucket policy. </p>
 * <p>
 * Bucket policies provide access control management at the bucket level for
 * both the bucket resource and contained object resources. Only one policy can
 * be specified per-bucket.
 * </p>
 * <p>
 * See the <a href="http://docs.amazonwebservices.com/AmazonS3/latest/dev/">
 * Amazon S3 developer guide</a> for more information on forming bucket polices.
 * </p>
 *
 * @see AmazonS3#deleteBucketPolicy(DeleteBucketPolicyRequest)
 */
public class DeleteBucketPolicyRequest extends AmazonWebServiceRequest {

    /** The name of the Amazon S3 bucket whose policy is being deleted. */
    private String bucketName;


    /**
     * Creates a new request object, ready to be executed to delete the bucket
     * policy of an Amazon S3 bucket.
     *
     * @param bucketName
     *            The name of the Amazon S3 bucket whose policy is being
     *            deleted.
     */
    public DeleteBucketPolicyRequest(String bucketName) {
        this.bucketName = bucketName;
    }


    /**
     * Returns the name of the Amazon S3 bucket whose policy is being deleted.
     *
     * @return The name of the Amazon S3 bucket whose policy is being deleted.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the Amazon S3 bucket whose policy is being deleted.
     *
     * @param bucketName
     *            The name of the Amazon S3 bucket whose policy is being
     *            deleted.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Sets the name of the Amazon S3 bucket whose policy is being deleted, and
     * returns the updated request object so that additional method calls can be
     * chained together.
     *
     * @param bucketName
     *            The name of the Amazon S3 bucket whose policy is being
     *            deleted.
     *
     * @return The updated request object so that additional method calls can be
     *         chained together.
     */
    public DeleteBucketPolicyRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }
}
