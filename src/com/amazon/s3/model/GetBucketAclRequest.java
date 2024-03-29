package com.amazon.s3.model;

import com.amazon.s3.AmazonWebServiceRequest;


/**
 * Request object containing all the options for requesting a bucket's Access Control List (ACL).
 */
public class GetBucketAclRequest extends AmazonWebServiceRequest {
	/** The name of the bucket whose ACL is being retrieved. */
	private String bucketName;

	/**
	 * Constructs a new GetBucketAclRequest object, ready to retrieve the ACL
	 * for the specified bucket when executed.
	 * 
	 * @param bucketName
	 *            The name of the bucket whose ACL will be retrieved by this
	 *            request when executed.
	 */
	public GetBucketAclRequest(String bucketName) {
		this.bucketName = bucketName;
	}

	/**
	 * Returns the name of the bucket whose ACL will be retrieved by this
	 * request, when executed.
	 * 
	 * @return The name of the bucket whose ACL will be retrieved by this
	 *         request, when executed.
	 */
	public String getBucketName() {
		return bucketName;
	}
}
