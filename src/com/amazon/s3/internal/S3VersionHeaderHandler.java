package com.amazon.s3.internal;

import com.amazon.s3.Headers;
import com.amazon.s3.http.HttpResponse;
import com.amazon.s3.model.transform.XmlResponsesSaxParser.CopyObjectResultHandler;

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
 * Header handler to pull the S3_VERSION_ID header out of the response. This
 * header is required for the copyPart and copyObject api methods.
 */
public class S3VersionHeaderHandler implements HeaderHandler<CopyObjectResultHandler> {

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.amazonaws.services.s3.internal.HeaderHandler#handle(java.lang.Object,
     * com.amazonaws.http.HttpResponse)
     */
    @Override
    public void handle(CopyObjectResultHandler result, HttpResponse response) {
        result.setVersionId(response.getHeaders().get(Headers.S3_VERSION_ID));
    }
}