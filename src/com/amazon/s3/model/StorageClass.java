package com.amazon.s3.model;

/*
 * Copyright 2010-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

/**
 * <p>
 * Specifies constants that define Amazon S3 storage classes. The standard storage class
 * is the default storage class.
 * </p>
 * <p>
 * Amazon S3 offers multiple storage classes for different customers' needs. The
 * <code>STANDARD</code> storage class is the default storage class, and means that
 * redundant copies of data will be stored in different locations.
 * </p>
 * <p>
 * The <code>REDUCED_REDUNDANCY</code> storage class offers customers who are using Amazon S3
 * for storing non-critical, reproducible data a low-cost highly available,
 * but less redundant, storage option.
 * </p>
 */
public enum StorageClass {

    /**
     * The default Amazon S3 storage class. This storage class
     * is recommended for critical, non-reproducible data.  The standard
     * storage class is a highly available and highly redundant storage option
     * provided for an affordable price.
     */
    Standard("STANDARD"),

    /**
     * The reduced redundancy storage class.
     * This storage class allows customers to reduce their storage costs
     * in return for a reduced level of data redundancy. Customers who are using
     * Amazon S3 for storing non-critical, reproducible data can choose this
     * low cost and highly available, but less redundant, storage option.
     */
    ReducedRedundancy("REDUCED_REDUNDANCY"),

    /**
     * The Amazon Glacier storage class.
     * This storage class means your object's data is stored in Amazon Glacier,
     * and Amazon S3 stores a reference to the data in the Amazon S3 bucket.
     */
    Glacier("GLACIER");

    /**
     * Returns the Amazon S3 {@link StorageClass} enumeration value representing the
     * specified Amazon S3 <code>StorageClass</code> ID string.
     * If the specified string doesn't map to a known Amazon S3 storage class,
     * an <code>IllegalArgumentException</code> is thrown.
     *
     * @param s3StorageClassString
     *            The Amazon S3 storage class ID string.
     *
     * @return The Amazon S3 <code>StorageClass</code> enumeration value representing the
     *         specified Amazon S3 storage class ID.
     *
     * @throws IllegalArgumentException
     *             If the specified value does not map to one of the known
     *             Amazon S3 storage classes.
     */
    public static StorageClass fromValue(String s3StorageClassString) throws IllegalArgumentException {
        for (StorageClass storageClass : StorageClass.values()) {
            if (storageClass.toString().equals(s3StorageClassString)) return storageClass;
        }

        throw new IllegalArgumentException(
                "Cannot create enum from " + s3StorageClassString + " value!");
    }

    private final String storageClassId;

    private StorageClass(String id) {
        this.storageClassId = id;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return storageClassId;
    }

}
