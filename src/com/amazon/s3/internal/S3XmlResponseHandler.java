package com.amazon.s3.internal;

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

import java.io.InputStream;
import java.util.Map;

import android.util.Log;

import com.amazon.s3.AmazonWebServiceResponse;
import com.amazon.s3.http.HttpResponse;
import com.amazon.s3.transform.Unmarshaller;


/**
 * S3 Implementation of HttpResponseHandler. Relies on a SAX unmarshaller for
 * handling the response.
 */
public class S3XmlResponseHandler<T> extends AbstractS3ResponseHandler<T> {

    /** The SAX unmarshaller to use when handling the response from S3 */
    private Unmarshaller<T, InputStream> responseUnmarshaller;

    /** Shared logger for profiling information */
   private static final String TAG = "###S3XmlResponseHandler###";

    /** Response headers from the processed response */
    private Map<String, String> responseHeaders;

    /**
     * Constructs a new S3 response handler that will use the specified SAX
     * unmarshaller to turn the response into an object.
     *
     * @param responseUnmarshaller
     *            The SAX unmarshaller to use on the response from S3.
     */
    public S3XmlResponseHandler(Unmarshaller<T, InputStream> responseUnmarshaller) {
        this.responseUnmarshaller = responseUnmarshaller;
    }

    /**
     * @see com.amazonaws.http.HttpResponseHandler#handle(com.amazonaws.http.HttpResponse)
     */
    public AmazonWebServiceResponse<T> handle(HttpResponse response) throws Exception {
        AmazonWebServiceResponse<T> awsResponse = parseResponseMetadata(response);
        responseHeaders = response.getHeaders();

        if (responseUnmarshaller != null) {
            Log.d(TAG,"Beginning to parse service response XML");
            T result = responseUnmarshaller.unmarshall(response.getContent());
            Log.d(TAG,"Done parsing service response XML");
            awsResponse.setResult(result);
        }

        return awsResponse;
    }

    /**
     * Returns the headers from the processed response. Will return null until a
     * response has been handled.
     *
     * @return the headers from the processed response.
     */
    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

}
