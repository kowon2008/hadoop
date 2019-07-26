/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs.azurebfs.services;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.fs.azurebfs.constants.AbfsHttpConstants;
import org.apache.hadoop.fs.azurebfs.contracts.exceptions.AbfsRestOperationException;
import org.apache.hadoop.fs.azurebfs.contracts.exceptions.AzureBlobFileSystemException;
import org.apache.hadoop.fs.azurebfs.contracts.exceptions.InvalidAbfsRestOperationException;
import org.apache.hadoop.fs.azurebfs.constants.HttpHeaderConfigurations;
import org.apache.hadoop.fs.azurebfs.oauth2.AzureADAuthenticator.HttpException;

public class AbfsXPolicyRestOperation {
    private static final Logger LOG = LoggerFactory.getLogger(AbfsXPolicyRestOperation.class);
    private final AbfsXPolicyRestOperationType operationType;
    private final AbfsXPolicyClient client;
    private final String method;
    private final URL url;
    private final List<AbfsHttpHeader> requestHeaders;
    private final boolean hasRequestBody;
    private byte[] buffer;
    private int bufferOffset;
    private int bufferLength;
    private AbfsHttpOperation result;
    public AbfsHttpOperation getResult() {
        return result;
    }

    /**
     * Initializes a new REST operation.
     *
     * @param client The abfs XPolicy client.
     * @param method The HTTP method (PUT, PATCH, POST, GET, HEAD, or DELETE).
     * @param url The full URL including query string parameters.
     * @param requestHeaders The HTTP request headers.
     */
    AbfsXPolicyRestOperation(final AbfsXPolicyRestOperationType operationType,
                      final AbfsXPolicyClient client,
                      final String method,
                      final URL url,
                      final List<AbfsHttpHeader> requestHeaders) {
        this.operationType = operationType;
        this.client = client;
        this.method = method;
        this.url = url;
        this.requestHeaders = requestHeaders;
        this.hasRequestBody = (AbfsHttpConstants.HTTP_METHOD_PUT.equals(method)
                || AbfsHttpConstants.HTTP_METHOD_PATCH.equals(method));
    }

    /**
     * Initializes a new REST operation.
     *
     * @param operationType The type of the REST operation.
     * @param client The abfs XPolicy client.
     * @param method The HTTP method (PUT, PATCH, POST, GET, HEAD, or DELETE).
     * @param url The full URL including query string parameters.
     * @param requestHeaders The HTTP request headers.
     * @param buffer For uploads, this is the request entity body.  For downloads,
     *               this will hold the response entity body.
     * @param bufferOffset An offset into the buffer where the data beings.
     * @param bufferLength The length of the data in the buffer.
     */
    AbfsXPolicyRestOperation(AbfsXPolicyRestOperationType operationType,
                      AbfsXPolicyClient client,
                      String method,
                      URL url,
                      List<AbfsHttpHeader> requestHeaders,
                      byte[] buffer,
                      int bufferOffset,
                      int bufferLength) {
        this(operationType, client, method, url, requestHeaders);
        this.buffer = buffer;
        this.bufferOffset = bufferOffset;
        this.bufferLength = bufferLength;
    }

    /**
     * Executes the REST operation with retry, by issuing one or more
     * HTTP operations.
     */
    void execute() throws AzureBlobFileSystemException {
        int retryCount = 0;
        while (!executeHttpOperation(retryCount++)) {
            try {
                Thread.sleep(client.getRetryPolicy().getRetryInterval(retryCount));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        if (result.getStatusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
            throw new AbfsRestOperationException(result.getStatusCode(), result.getStorageErrorCode(),
                    result.getStorageErrorMessage(), null, result);
        }
    }

    /**
     * Executes a single HTTP operation to complete the REST operation.  If it
     * fails, there may be a retry.  The retryCount is incremented with each
     * attempt.
     */
    private boolean executeHttpOperation(final int retryCount) throws AzureBlobFileSystemException {
        AbfsHttpOperation httpOperation = null;
        try {
            // initialize the HTTP request and open the connection
            httpOperation = new AbfsHttpOperation(url, method, requestHeaders);

            // sign the HTTP request
            if (client.getAccessToken() == null) {
                LOG.debug("Signing request with shared key");
                // sign the HTTP request
                client.getSharedKeyCredentials().signRequest(
                        httpOperation.getConnection(),
                        hasRequestBody ? bufferLength : 0);
            } else {
                LOG.debug("Authenticating request with OAuth2 access token");
                httpOperation.getConnection().setRequestProperty(HttpHeaderConfigurations.AUTHORIZATION,
                        client.getAccessToken());
            }

            // dump the headers
            AbfsIoUtils.dumpHeadersToDebugLog("Request Headers",
                    httpOperation.getConnection().getRequestProperties());

            if (hasRequestBody) {
                // HttpUrlConnection requires
                httpOperation.sendRequest(buffer, bufferOffset, bufferLength);
            }

            httpOperation.processResponse(buffer, bufferOffset, bufferLength);
        } catch (IOException ex) {
            if (ex instanceof UnknownHostException) {
                LOG.warn(String.format("Unknown host name: %s. Retrying to resolve the host name...", httpOperation.getUrl().getHost()));
            }

            if (LOG.isDebugEnabled()) {
                if (httpOperation != null) {
                    LOG.debug("HttpRequestFailure: " + httpOperation.toString(), ex);
                } else {
                    LOG.debug("HttpRequestFailure: " + method + "," + url, ex);
                }
            }

            if (!client.getRetryPolicy().shouldRetry(retryCount, -1)) {
                throw new InvalidAbfsRestOperationException(ex);
            }

            // once HttpException is thrown by AzureADAuthenticator,
            // it indicates the policy in AzureADAuthenticator determined
            // retry is not needed
            if (ex instanceof HttpException) {
                throw new AbfsRestOperationException((HttpException) ex);
            }

            return false;
        }

        LOG.debug("HttpRequest: " + httpOperation.toString());

        if (client.getRetryPolicy().shouldRetry(retryCount, httpOperation.getStatusCode())) {
            return false;
        }

        result = httpOperation;

        return true;
    }
}
