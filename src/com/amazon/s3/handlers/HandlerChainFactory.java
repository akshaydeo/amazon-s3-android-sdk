package com.amazon.s3.handlers;

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


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.amazon.s3.exceptions.AmazonClientException;



/**
 * Factory for creating request/response handler chains.
 */
public class HandlerChainFactory {

    /**
     * Constructs a new request handler chain by analyzing the specified
     * classpath resource.
     *
     * @param resource
     *            The resource to load from the classpath containing the list of
     *            request handlers to instantiate.
     *
     * @return A list of request handlers based on the handlers referenced in
     *         the specified resource.
     */
    public List<RequestHandler> newRequestHandlerChain(String resource) {
        List<RequestHandler> handlers = new ArrayList<RequestHandler>();

        try {
            InputStream input = getClass().getResourceAsStream(resource);
            if (input == null) return handlers;

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            while (true) {
                String requestHandlerClassName = reader.readLine();
                if (requestHandlerClassName == null) break;
                requestHandlerClassName = requestHandlerClassName.trim();
                if (requestHandlerClassName.equals("")) continue;

                Class<?> requestHandlerClass = getClass().getClassLoader().loadClass(requestHandlerClassName);
                Object requestHandlerObject = requestHandlerClass.newInstance();
                if (requestHandlerObject instanceof RequestHandler) {
                    handlers.add((RequestHandler)requestHandlerObject);
                } else {
                    throw new AmazonClientException("Unable to instantiate request handler chain for client.  "
                            + "Listed request handler ('" + requestHandlerClassName + "') "
                            + "does not implement the RequestHandler interface.");
                }
            }
        } catch (Exception e) {
            throw new AmazonClientException("Unable to instantiate request handler chain for client: "
                    + e.getMessage(), e);
        }

        return handlers;
    }
}
