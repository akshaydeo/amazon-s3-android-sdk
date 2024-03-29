package com.amazon.s3.internal;

import com.amazon.s3.Headers;
import com.amazon.s3.http.HttpResponse;

/*
 * Copyright 2011-2013 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * Base request handler for responses that include a server-side encryption
 * header
 */
public class ServerSideEncryptionHeaderHandler <T extends ServerSideEncryptionResult> implements HeaderHandler<T>{

    /* (non-Javadoc)
     * @see com.amazonaws.services.s3.internal.HeaderHandler#handle(java.lang.Object, com.amazonaws.http.HttpResponse)
     */
    @Override
    public void handle(T result, HttpResponse response) {
        result.setServerSideEncryption(response.getHeaders().get(Headers.SERVER_SIDE_ENCRYPTION));
    }
}
