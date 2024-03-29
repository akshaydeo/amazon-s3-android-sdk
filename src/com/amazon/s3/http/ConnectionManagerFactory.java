package com.amazon.s3.http;

/*
 * Copyright 2011-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.PlainSocketFactory;

import org.apache.http.conn.ssl.SSLSocketFactory;

import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;

import org.apache.http.params.HttpParams;

import com.amazon.s3.ClientConfiguration;

/**
 * Responsible for creating and configuring instances of Apache HttpClient4's
 * Connection Manager.
 */
class ConnectionManagerFactory {

	public static ThreadSafeClientConnManager createThreadSafeClientConnManager(
			ClientConfiguration config, HttpParams httpClientParams) {
		ConnManagerParams.setMaxConnectionsPerRoute(httpClientParams,
				new ConnPerRouteBean(20));

		SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
		sslSocketFactory
				.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);

		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		registry.register(new Scheme("https", sslSocketFactory, 443));

		return new ThreadSafeClientConnManager(httpClientParams, registry);
	}

}
