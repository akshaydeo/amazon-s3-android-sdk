package com.amazon.s3.http;

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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;

import android.util.Log;

import com.amazon.s3.AmazonWebServiceRequest;
import com.amazon.s3.AmazonWebServiceResponse;
import com.amazon.s3.ClientConfiguration;
import com.amazon.s3.Request;
import com.amazon.s3.ResponseMetadata;
import com.amazon.s3.exceptions.AmazonClientException;
import com.amazon.s3.exceptions.AmazonServiceException;
import com.amazon.s3.exceptions.AmazonServiceException.ErrorType;
import com.amazon.s3.handlers.RequestHandler;
import com.amazon.s3.internal.CRC32MismatchException;
import com.amazon.s3.internal.CustomBackoffStrategy;
import com.amazon.s3.util.AWSRequestMetrics;
import com.amazon.s3.util.AWSRequestMetrics.Field;
import com.amazon.s3.util.CountingInputStream;
import com.amazon.s3.util.ResponseMetadataCache;
import com.amazon.s3.util.TimingInfo;

public class AmazonHttpClient {

	/** Internal client for sending HTTP requests */
	private final HttpClient httpClient;

	/** Maximum exponential back-off time before retrying a request */
	private static final int MAX_BACKOFF_IN_MILLISECONDS = 20 * 1000;

	/** Client configuration options, such as proxy settings, max retries, etc. */
	private final ClientConfiguration config;

	/** Cache of metadata for recently executed requests for diagnostic purposes */
	private final ResponseMetadataCache responseMetadataCache = new ResponseMetadataCache(
			50);

	private static final Random random = new Random();

	private static HttpRequestFactory httpRequestFactory = new HttpRequestFactory();
	private static HttpClientFactory httpClientFactory = new HttpClientFactory();

	/** Internal system property to enable advanced timing info collection. */
	public static final String PROFILING_SYSTEM_PROPERTY = "com.amazonaws.sdk.enableRuntimeProfiling";

	private static final String TAG = "###AmazonHttpClient###";

	static {
		// Customers have reported XML parsing issues with the following
		// JVM versions, which don't occur with more recent versions, so
		// if we detect any of these, give customers a heads up.
		List<String> problematicJvmVersions = Arrays.asList(new String[] {
				"1.6.0_06", "1.6.0_13", "1.6.0_17", });
		String jvmVersion = System.getProperty("java.version");
		if (problematicJvmVersions.contains(jvmVersion)) {
			Log.w(TAG,
					"Detected a possible problem with the current JVM version ("
							+ jvmVersion
							+ ").  "
							+ "If you experience XML parsing problems using the SDK, try upgrading to a more recent JVM update.");
		}
	}

	/**
	 * Constructs a new AWS client using the specified client configuration
	 * options (ex: max retry attempts, proxy settings, etc).
	 * 
	 * @param clientConfiguration
	 *            Configuration options specifying how this client will
	 *            communicate with AWS (ex: proxy settings, retry count, etc.).
	 */
	public AmazonHttpClient(ClientConfiguration clientConfiguration) {
		this.config = clientConfiguration;
		this.httpClient = httpClientFactory.createHttpClient(config);
	}

	/**
	 * Returns additional response metadata for an executed request. Response
	 * metadata isn't considered part of the standard results returned by an
	 * operation, so it's accessed instead through this diagnostic interface.
	 * Response metadata is typically used for troubleshooting issues with AWS
	 * support staff when services aren't acting as expected.
	 * 
	 * @param request
	 *            A previously executed AmazonWebServiceRequest object, whose
	 *            response metadata is desired.
	 * 
	 * @return The response metadata for the specified request, otherwise null
	 *         if there is no response metadata available for the request.
	 */
	public ResponseMetadata getResponseMetadataForRequest(
			AmazonWebServiceRequest request) {
		return responseMetadataCache.get(request);
	}

	/**
	 * Disables the default strict hostname verification in this client and
	 * instead uses a browser compatible hostname verification strategy (i.e.
	 * cert hostname wildcards are evaulated more liberally).
	 */
	public void disableStrictHostnameVerification() {
		return;
	}

	/**
	 * Executes the request and returns the result.
	 * 
	 * @param request
	 *            The AmazonWebServices request to send to the remote server
	 * @param responseHandler
	 *            A response handler to accept a successful response from the
	 *            remote server
	 * @param errorResponseHandler
	 *            A response handler to accept an unsuccessful response from the
	 *            remote server
	 * @param executionContext
	 *            Additional information about the context of this web service
	 *            call
	 */
	public <T> T execute(Request<?> request,
			HttpResponseHandler<AmazonWebServiceResponse<T>> responseHandler,
			HttpResponseHandler<AmazonServiceException> errorResponseHandler,
			ExecutionContext executionContext) throws AmazonClientException,
			AmazonServiceException {
		long startTime = System.currentTimeMillis();

		if (executionContext == null)
			throw new AmazonClientException(
					"Internal SDK Error: No execution context parameter specified.");
		List<RequestHandler> requestHandlers = executionContext
				.getRequestHandlers();
		if (requestHandlers == null)
			requestHandlers = new ArrayList<RequestHandler>();

		// Apply any additional service specific request handlers that need to
		// be run
		for (RequestHandler requestHandler : requestHandlers) {
			requestHandler.beforeRequest(request);
		}

		try {
			T t = executeHelper(request, responseHandler, errorResponseHandler,
					executionContext);
			TimingInfo timingInfo = executionContext.getAwsRequestMetrics()
					.getTimingInfo();
			timingInfo.setEndTime(System.currentTimeMillis());

			for (RequestHandler handler : requestHandlers) {
				try {
					handler.afterResponse(request, t, timingInfo);
				} catch (ClassCastException cce) {
				}
			}
			return t;
		} catch (AmazonClientException e) {
			for (RequestHandler handler : requestHandlers) {
				handler.afterError(request, e);
			}
			throw e;
		}
	}

	/**
	 * Internal method to execute the HTTP method given.
	 * 
	 * @see AmazonHttpClient#execute(Request, HttpResponseHandler,
	 *      HttpResponseHandler)
	 * @see AmazonHttpClient#execute(Request, HttpResponseHandler,
	 *      HttpResponseHandler, ExecutionContext)
	 */
	private <T extends Object> T executeHelper(Request<?> request,
			HttpResponseHandler<AmazonWebServiceResponse<T>> responseHandler,
			HttpResponseHandler<AmazonServiceException> errorResponseHandler,
			ExecutionContext executionContext) throws AmazonClientException,
			AmazonServiceException {

		/*
		 * Depending on which response handler we end up choosing to handle the
		 * HTTP response, it might require us to leave the underlying HTTP
		 * connection open, depending on whether or not it reads the complete
		 * HTTP response stream from the HTTP connection, or if delays reading
		 * any of the content until after a response is returned to the caller.
		 */
		boolean leaveHttpConnectionOpen = false;

		AWSRequestMetrics awsRequestMetrics = executionContext
				.getAwsRequestMetrics();
		/*
		 * add the service endpoint to the logs. You can infer service name from
		 * service endpoint
		 */
		awsRequestMetrics.addProperty(Field.ServiceName.name(),
				request.getServiceName());
		awsRequestMetrics.addProperty(Field.ServiceEndpoint.name(),
				request.getEndpoint());

		// Apply whatever request options we know how to handle, such as
		// user-agent.
		applyRequestData(request);

		int retryCount = 0;
		URI redirectedURI = null;
		HttpEntity entity = null;
		AmazonServiceException exception = null;

		// Make a copy of the original request params and headers so that we can
		// permute it in this loop and start over with the original every time.
		Map<String, String> originalParameters = new HashMap<String, String>();
		originalParameters.putAll(request.getParameters());
		Map<String, String> originalHeaders = new HashMap<String, String>();
		originalHeaders.putAll(request.getHeaders());

		while (true) {
			awsRequestMetrics.setCounter(Field.AttemptCount.name(),
					retryCount + 1);
			if (retryCount > 0) {
				request.setParameters(originalParameters);
				request.setHeaders(originalHeaders);
			}

			HttpRequestBase httpRequest = null;
			org.apache.http.HttpResponse response = null;

			try {
				// Sign the request if a signer was provided
				if (executionContext.getSigner() != null
						&& executionContext.getCredentials() != null) {
					awsRequestMetrics.startEvent(Field.RequestSigningTime
							.name());
					executionContext.getSigner().sign(request,
							executionContext.getCredentials());
					awsRequestMetrics.endEvent(Field.RequestSigningTime.name());
				}

				Log.d(TAG, "Sending Request: " + request.toString());
				httpRequest = httpRequestFactory.createHttpRequest(request,
						config, entity, executionContext);

				if (httpRequest instanceof HttpEntityEnclosingRequest) {
					entity = ((HttpEntityEnclosingRequest) httpRequest)
							.getEntity();
				}

				if (redirectedURI != null) {
					httpRequest.setURI(redirectedURI);
				}

				if (retryCount > 0) {
					awsRequestMetrics.startEvent(Field.RetryPauseTime.name());
					pauseExponentially(retryCount, exception,
							executionContext.getCustomBackoffStrategy());
					awsRequestMetrics.endEvent(Field.RetryPauseTime.name());
				}

				if (entity != null) {
					InputStream content = entity.getContent();
					if (retryCount > 0) {
						if (content.markSupported()) {
							content.reset();
							content.mark(-1);
						}
					} else {
						if (content.markSupported()) {
							content.mark(-1);
						}
					}
				}

				exception = null;

				awsRequestMetrics.startEvent(Field.HttpRequestTime.name());
				response = httpClient.execute(httpRequest);
				awsRequestMetrics.endEvent(Field.HttpRequestTime.name());

				if (isRequestSuccessful(response)) {

					awsRequestMetrics.addProperty(Field.StatusCode.name(),
							response.getStatusLine().getStatusCode());

					/*
					 * If we get back any 2xx status code, then we know we
					 * should treat the service call as successful.
					 */
					leaveHttpConnectionOpen = responseHandler
							.needsConnectionLeftOpen();
					return handleResponse(request, responseHandler,
							httpRequest, response, executionContext);
				} else if (isTemporaryRedirect(response)) {
					/*
					 * S3 sends 307 Temporary Redirects if you try to delete an
					 * EU bucket from the US endpoint. If we get a 307, we'll
					 * point the HTTP method to the redirected location, and let
					 * the next retry deliver the request to the right location.
					 */
					Header[] locationHeaders = response.getHeaders("location");
					String redirectedLocation = locationHeaders[0].getValue();
					Log.d(TAG, "Redirecting to: " + redirectedLocation);
					redirectedURI = URI.create(redirectedLocation);
					httpRequest.setURI(redirectedURI);
					awsRequestMetrics.addProperty(Field.StatusCode.name(),
							response.getStatusLine().getStatusCode());
					awsRequestMetrics.addProperty(
							Field.RedirectLocation.name(), redirectedLocation);
					awsRequestMetrics.addProperty(Field.AWSRequestID.name(),
							null);

				} else {
					leaveHttpConnectionOpen = errorResponseHandler
							.needsConnectionLeftOpen();
					exception = handleErrorResponse(request,
							errorResponseHandler, httpRequest, response);
					awsRequestMetrics.addProperty(Field.AWSRequestID.name(),
							exception.getRequestId());
					awsRequestMetrics.addProperty(Field.AWSErrorCode.name(),
							exception.getErrorCode());
					awsRequestMetrics.addProperty(Field.StatusCode.name(),
							exception.getStatusCode());

					if (!shouldRetry(httpRequest, exception, retryCount)) {
						throw exception;
					}
					resetRequestAfterError(request, exception);
				}
			} catch (IOException ioe) {
				Log.i(TAG,
						"Unable to execute HTTP request: " + ioe.getMessage(),
						ioe);
				awsRequestMetrics.addProperty(Field.Exception.name(),
						ioe.toString());
				awsRequestMetrics.addProperty(Field.AWSRequestID.name(), null);

				if (!shouldRetry(httpRequest, ioe, retryCount)) {
					throw new AmazonClientException(
							"Unable to execute HTTP request: "
									+ ioe.getMessage(), ioe);
				}
				resetRequestAfterError(request, ioe);
			} finally {
				retryCount++;

				/*
				 * Some response handlers need to manually manage the HTTP
				 * connection and will take care of releasing the connection on
				 * their own, but if this response handler doesn't need the
				 * connection left open, we go ahead and release the it to free
				 * up resources.
				 */
				if (!leaveHttpConnectionOpen) {
					try {
						response.getEntity().getContent().close();
					} catch (Throwable t) {
					}
				}
			}
		} /* end while (true) */
	}

	/**
	 * Resets the specified request, so that it can be sent again, after
	 * receiving the specified error. If a problem is encountered with resetting
	 * the request, then an AmazonClientException is thrown with the original
	 * error as the cause (not an error about being unable to reset the stream).
	 * 
	 * @param request
	 *            The request being executed that failed and needs to be reset.
	 * @param cause
	 *            The original error that caused the request to fail.
	 * 
	 * @throws AmazonClientException
	 *             If the request can't be reset.
	 */
	private void resetRequestAfterError(Request<?> request, Exception cause)
			throws AmazonClientException {
		if (request.getContent() == null) {
			return; // no reset needed
		}
		if (!request.getContent().markSupported()) {
			throw new AmazonClientException(
					"Encountered an exception and stream is not resettable",
					cause);
		}
		try {
			request.getContent().reset();
		} catch (IOException e) {
			// This exception comes from being unable to reset the input stream,
			// so throw the original, more meaningful exception
			throw new AmazonClientException(
					"Encountered an exception and couldn't reset the stream to retry",
					cause);
		}
	}

	/**
	 * Applies any additional options set in the request.
	 */
	private void applyRequestData(Request<?> request) {
		if (config.getUserAgent() != null) {
			request.addHeader("User-Agent", config.getUserAgent());
		}

		if (request.getOriginalRequest() != null
				&& request.getOriginalRequest().getRequestClientOptions() != null
				&& request.getOriginalRequest().getRequestClientOptions()
						.getClientMarker() != null) {
			request.addHeader(
					"User-Agent",
					createUserAgentString(config.getUserAgent(), request
							.getOriginalRequest().getRequestClientOptions()
							.getClientMarker()));
		}
	}

	/**
	 * Appends the given user-agent string to the existing one and returns it.
	 */
	private static String createUserAgentString(String existingUserAgentString,
			String userAgent) {
		if (existingUserAgentString.contains(userAgent)) {
			return existingUserAgentString;
		} else {
			return existingUserAgentString.trim() + " " + userAgent.trim();
		}
	}

	/**
	 * Shuts down this HTTP client object, releasing any resources that might be
	 * held open. This is an optional method, and callers are not expected to
	 * call it, but can if they want to explicitly release any open resources.
	 * Once a client has been shutdown, it cannot be used to make more requests.
	 */
	public void shutdown() {
		IdleConnectionReaper.removeConnectionManager(httpClient
				.getConnectionManager());
		httpClient.getConnectionManager().shutdown();
	}

	/**
	 * Returns true if a failed request should be retried.
	 * 
	 * @param method
	 *            The current HTTP method being executed.
	 * @param exception
	 *            The exception from the failed request.
	 * @param retries
	 *            The number of times the current request has been attempted.
	 * 
	 * @return True if the failed request should be retried.
	 */
	private boolean shouldRetry(HttpRequestBase method, Exception exception,
			int retries) {
		if (retries >= config.getMaxErrorRetry())
			return false;

		if (method instanceof HttpEntityEnclosingRequest) {
			HttpEntity entity = ((HttpEntityEnclosingRequest) method)
					.getEntity();
			if (entity != null && !entity.isRepeatable()) {

				Log.d(TAG, "Entity not repeatable");

				return false;
			}
		}

		if (exception instanceof IOException) {

			Log.d(TAG, "Retrying on " + exception.getClass().getName() + ": "
					+ exception.getMessage());

			return true;
		}

		if (exception instanceof AmazonServiceException) {
			AmazonServiceException ase = (AmazonServiceException) exception;

			/*
			 * For 500 internal server errors and 503 service unavailable
			 * errors, we want to retry, but we need to use an exponential
			 * back-off strategy so that we don't overload a server with a flood
			 * of retries. If we've surpassed our retry limit we handle the
			 * error response as a non-retryable error and go ahead and throw it
			 * back to the user as an exception.
			 */
			if (ase.getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR
					|| ase.getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
				return true;
			}

			/*
			 * Throttling is reported as a 400 error from newer services. To try
			 * and smooth out an occasional throttling error, we'll pause and
			 * retry, hoping that the pause is long enough for the request to
			 * get through the next time.
			 */
			if (isThrottlingException(ase))
				return true;
		}

		return false;
	}

	private boolean isTemporaryRedirect(org.apache.http.HttpResponse response) {
		int status = response.getStatusLine().getStatusCode();
		return status == HttpStatus.SC_TEMPORARY_REDIRECT
				&& response.getHeaders("Location") != null
				&& response.getHeaders("Location").length > 0;
	}

	private boolean isRequestSuccessful(org.apache.http.HttpResponse response) {
		int status = response.getStatusLine().getStatusCode();
		return status / 100 == HttpStatus.SC_OK / 100;
	}

	/**
	 * Handles a successful response from a service call by unmarshalling the
	 * results using the specified response handler.
	 * 
	 * @param <T>
	 *            The type of object expected in the response.
	 * 
	 * @param request
	 *            The original request that generated the response being
	 *            handled.
	 * @param responseHandler
	 *            The response unmarshaller used to interpret the contents of
	 *            the response.
	 * @param method
	 *            The HTTP method that was invoked, and contains the contents of
	 *            the response.
	 * @param executionContext
	 *            Extra state information about the request currently being
	 *            executed.
	 * @return The contents of the response, unmarshalled using the specified
	 *         response handler.
	 * 
	 * @throws IOException
	 *             If any problems were encountered reading the response
	 *             contents from the HTTP method object.
	 */
	private <T> T handleResponse(Request<?> request,
			HttpResponseHandler<AmazonWebServiceResponse<T>> responseHandler,
			HttpRequestBase method,
			org.apache.http.HttpResponse apacheHttpResponse,
			ExecutionContext executionContext) throws IOException {

		HttpResponse httpResponse = createResponse(method, request,
				apacheHttpResponse);
		if (responseHandler.needsConnectionLeftOpen()
				&& method instanceof HttpEntityEnclosingRequest) {
			HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) method;
			httpResponse.setContent(new HttpMethodReleaseInputStream(
					httpEntityEnclosingRequest));
		}

		try {
			CountingInputStream countingInputStream = null;
			if (System.getProperty(PROFILING_SYSTEM_PROPERTY) != null) {
				countingInputStream = new CountingInputStream(
						httpResponse.getContent());
				httpResponse.setContent(countingInputStream);
			}

			AWSRequestMetrics awsRequestMetrics = executionContext
					.getAwsRequestMetrics();
			awsRequestMetrics.startEvent(Field.ResponseProcessingTime.name());
			AmazonWebServiceResponse<? extends T> awsResponse = responseHandler
					.handle(httpResponse);
			awsRequestMetrics.endEvent(Field.ResponseProcessingTime.name());
			if (countingInputStream != null) {
				awsRequestMetrics.setCounter(Field.BytesProcessed.name(),
						countingInputStream.getByteCount());
			}

			if (awsResponse == null)
				throw new RuntimeException(
						"Unable to unmarshall response metadata");

			responseMetadataCache.add(request.getOriginalRequest(),
					awsResponse.getResponseMetadata());

			Log.d(TAG, "Received successful response: "
					+ apacheHttpResponse.getStatusLine().getStatusCode()
					+ ", AWS Request ID: " + awsResponse.getRequestId());

			awsRequestMetrics.addProperty(Field.AWSRequestID.name(),
					awsResponse.getRequestId());

			return awsResponse.getResult();
		} catch (CRC32MismatchException e) {
			throw e;
		} catch (Exception e) {
			String errorMessage = "Unable to unmarshall response ("
					+ e.getMessage() + ")";
			throw new AmazonClientException(errorMessage, e);
		}
	}

	/**
	 * Responsible for handling an error response, including unmarshalling the
	 * error response into the most specific exception type possible, and
	 * throwing the exception.
	 * 
	 * @param request
	 *            The request that generated the error response being handled.
	 * @param errorResponseHandler
	 *            The response handler responsible for unmarshalling the error
	 *            response.
	 * @param method
	 *            The HTTP method containing the actual response content.
	 * 
	 * @throws IOException
	 *             If any problems are encountering reading the error response.
	 */
	private AmazonServiceException handleErrorResponse(Request<?> request,
			HttpResponseHandler<AmazonServiceException> errorResponseHandler,
			HttpRequestBase method,
			org.apache.http.HttpResponse apacheHttpResponse) throws IOException {

		int status = apacheHttpResponse.getStatusLine().getStatusCode();
		HttpResponse response = createResponse(method, request,
				apacheHttpResponse);
		if (errorResponseHandler.needsConnectionLeftOpen()
				&& method instanceof HttpEntityEnclosingRequestBase) {
			HttpEntityEnclosingRequestBase entityEnclosingRequest = (HttpEntityEnclosingRequestBase) method;
			response.setContent(new HttpMethodReleaseInputStream(
					entityEnclosingRequest));
		}

		AmazonServiceException exception = null;
		try {
			exception = errorResponseHandler.handle(response);
			Log.d(TAG, "Received error response: " + exception.toString());
		} catch (Exception e) {
			// If the errorResponseHandler doesn't work, then check for error
			// responses that don't have any content
			if (status == 413) {
				exception = new AmazonServiceException(
						"Request entity too large");
				exception.setServiceName(request.getServiceName());
				exception.setStatusCode(413);
				exception.setErrorType(ErrorType.Client);
				exception.setErrorCode("Request entity too large");
			} else if (status == 503
					&& "Service Unavailable"
							.equalsIgnoreCase(apacheHttpResponse
									.getStatusLine().getReasonPhrase())) {
				exception = new AmazonServiceException("Service unavailable");
				exception.setServiceName(request.getServiceName());
				exception.setStatusCode(503);
				exception.setErrorType(ErrorType.Service);
				exception.setErrorCode("Service unavailable");
			} else {
				String errorMessage = "Unable to unmarshall error response ("
						+ e.getMessage() + ")";
				throw new AmazonClientException(errorMessage, e);
			}
		}

		exception.setStatusCode(status);
		exception.setServiceName(request.getServiceName());
		exception.fillInStackTrace();
		return exception;
	}

	/**
	 * Creates and initializes an HttpResponse object suitable to be passed to
	 * an HTTP response handler object.
	 * 
	 * @param method
	 *            The HTTP method that was invoked to get the response.
	 * @param request
	 *            The HTTP request associated with the response.
	 * 
	 * @return The new, initialized HttpResponse object ready to be passed to an
	 *         HTTP response handler object.
	 * 
	 * @throws IOException
	 *             If there were any problems getting any response information
	 *             from the HttpClient method object.
	 */
	private HttpResponse createResponse(HttpRequestBase method,
			Request<?> request, org.apache.http.HttpResponse apacheHttpResponse)
			throws IOException {
		HttpResponse httpResponse = new HttpResponse(request, method);

		if (apacheHttpResponse.getEntity() != null) {
			httpResponse
					.setContent(apacheHttpResponse.getEntity().getContent());
		}

		httpResponse.setStatusCode(apacheHttpResponse.getStatusLine()
				.getStatusCode());
		httpResponse.setStatusText(apacheHttpResponse.getStatusLine()
				.getReasonPhrase());
		for (Header header : apacheHttpResponse.getAllHeaders()) {
			httpResponse.addHeader(header.getName(), header.getValue());
		}

		return httpResponse;
	}

	/**
	 * Exponential sleep on failed request to avoid flooding a service with
	 * retries.
	 * 
	 * @param retries
	 *            Current retry count.
	 * @param previousException
	 *            Exception information for the previous attempt, if any.
	 */
	private void pauseExponentially(int retries,
			AmazonServiceException previousException,
			CustomBackoffStrategy backoffStrategy) {
		long delay = 0;
		if (backoffStrategy != null) {
			delay = backoffStrategy.getBackoffPeriod(retries);
		} else {
			long scaleFactor = 300;
			if (isThrottlingException(previousException)) {
				scaleFactor = 500 + random.nextInt(100);
			}
			delay = (long) (Math.pow(2, retries) * scaleFactor);
		}

		delay = Math.min(delay, MAX_BACKOFF_IN_MILLISECONDS);

		Log.d(TAG, "Retriable error detected, " + "will retry in " + delay
				+ "ms, attempt number: " + retries);

		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AmazonClientException(e.getMessage(), e);
		}
	}

	/**
	 * Returns true if the specified exception is a throttling error.
	 * 
	 * @param ase
	 *            The exception to test.
	 * 
	 * @return True if the exception resulted from a throttling error message
	 *         from a service, otherwise false.
	 */
	private boolean isThrottlingException(AmazonServiceException ase) {
		if (ase == null)
			return false;
		return "Throttling".equals(ase.getErrorCode())
				|| "ThrottlingException".equals(ase.getErrorCode())
				|| "ProvisionedThroughputExceededException".equals(ase
						.getErrorCode());
	}

	@Override
	protected void finalize() throws Throwable {
		this.shutdown();
		super.finalize();
	}

}
