/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs.azurebfs.services;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.common.annotations.VisibleForTesting;
import org.apache.hadoop.fs.azurebfs.AbfsConfiguration;
import org.apache.hadoop.fs.azurebfs.extensions.ExtensionHelper;
import org.apache.hadoop.fs.azurebfs.oauth2.AccessTokenProvider;
import org.apache.hadoop.fs.azurebfs.utils.SSLSocketFactoryEx;
import org.apache.hadoop.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.fs.azurebfs.contracts.exceptions.AzureBlobFileSystemException;
import org.apache.hadoop.fs.azurebfs.contracts.exceptions.InvalidUriException;

import static org.apache.hadoop.fs.azurebfs.constants.AbfsHttpConstants.*;
import static org.apache.hadoop.fs.azurebfs.constants.FileSystemUriSchemes.HTTPS_SCHEME;
import static org.apache.hadoop.fs.azurebfs.constants.HttpHeaderConfigurations.*;
import static org.apache.hadoop.fs.azurebfs.constants.HttpQueryParams.*;

public class AbfsXPolicyClient implements Closeable {
    public static final Logger LOG = LoggerFactory.getLogger(AbfsXPolicyClient.class);
    private final URL baseUrl;
    private final SharedKeyCredentials sharedKeyCredentials;
    private final String xMsVersion = "2018-11-09";
    private final ExponentialRetryPolicy retryPolicy;
    private final AbfsConfiguration abfsConfiguration;
    private final String userAgent;

    private final AccessTokenProvider tokenProvider;

    public AbfsXPolicyClient(final URL baseUrl, final SharedKeyCredentials sharedKeyCredentials,
                             final AbfsConfiguration abfsConfiguration,
                             final ExponentialRetryPolicy exponentialRetryPolicy,
                             final AccessTokenProvider tokenProvider) {
        this.baseUrl = baseUrl;
        this.sharedKeyCredentials = sharedKeyCredentials;
        this.abfsConfiguration = abfsConfiguration;
        this.retryPolicy = exponentialRetryPolicy;

        String sslProviderName = null;

        if (this.baseUrl.toString().startsWith(HTTPS_SCHEME)) {
            try {
                SSLSocketFactoryEx.initializeDefaultFactory(this.abfsConfiguration.getPreferredSSLFactoryOption());
                sslProviderName = SSLSocketFactoryEx.getDefaultFactory().getProviderName();
            } catch (IOException e) {
                // Suppress exception. Failure to init SSLSocketFactoryEx would have only performance impact.
            }
        }

        this.userAgent = initializeUserAgent(abfsConfiguration, sslProviderName);
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void close() throws IOException {
        if (tokenProvider instanceof Closeable) {
            IOUtils.cleanupWithLogger(LOG, (Closeable) tokenProvider);
        }
    }

    List<AbfsHttpHeader> createDefaultHeaders() {
        final List<AbfsHttpHeader> requestHeaders = new ArrayList<AbfsHttpHeader>();
        requestHeaders.add(new AbfsHttpHeader(X_MS_VERSION, xMsVersion));
        requestHeaders.add(new AbfsHttpHeader(CONTENT_LENGTH, "0"));
        requestHeaders.add(new AbfsHttpHeader(USER_AGENT, userAgent));
        return requestHeaders;
    }

    AbfsUriQueryBuilder createDefaultUriQueryBuilder() {
        final AbfsUriQueryBuilder abfsUriQueryBuilder = new AbfsUriQueryBuilder();
        abfsUriQueryBuilder.addQuery(QUERY_PARAM_TIMEOUT, DEFAULT_TIMEOUT);
        return abfsUriQueryBuilder;
    }

    public AbfsXPolicyRestOperation getXPolicies(final String serviceName, final String policyName) throws AzureBlobFileSystemException {
        final AbfsUriQueryBuilder abfsUriQueryBuilder = createDefaultUriQueryBuilder();

        String relativePath = serviceName;
        if (policyName != null && !policyName.isEmpty()) {
            relativePath = relativePath + FORWARD_SLASH + policyName;
        }
        final URL url = createRequestUrl(relativePath, abfsUriQueryBuilder.toString());

        final List<AbfsHttpHeader> requestHeaders = createDefaultHeaders();

        final AbfsXPolicyRestOperation op = new AbfsXPolicyRestOperation(
                AbfsXPolicyRestOperationType.GetXPolicies,
                this,
                HTTP_METHOD_GET,
                url,
                requestHeaders);
        op.execute();
        return op;
    }

    private URL createRequestUrl(final String relativePath, final String query) throws AzureBlobFileSystemException {
        return createRequestUrl(EMPTY_STRING, relativePath, query);
    }

    private URL createRequestUrl(final String path, final String relativePath, final String query)
            throws AzureBlobFileSystemException {
        String base = baseUrl.toString();
        if (relativePath != null && !relativePath.isEmpty()) {
            base = base + FORWARD_SLASH + relativePath;
        }

        String encodedPath = path;
        try {
            encodedPath = urlEncode(path);
        } catch (AzureBlobFileSystemException ex) {
            LOG.debug("Unexpected error.", ex);
            throw new InvalidUriException(path);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(base);
        sb.append(encodedPath);
        sb.append(query);

        final URL url;
        try {
            url = new URL(sb.toString());
        } catch (MalformedURLException ex) {
            throw new InvalidUriException(sb.toString());
        }
        return url;
    }

    public static String urlEncode(final String value) throws AzureBlobFileSystemException {
        String encodedString;
        try {
            encodedString =  URLEncoder.encode(value, UTF_8)
                    .replace(PLUS, PLUS_ENCODE)
                    .replace(FORWARD_SLASH_ENCODE, FORWARD_SLASH);
        } catch (UnsupportedEncodingException ex) {
            throw new InvalidUriException(value);
        }

        return encodedString;
    }

    public synchronized String getAccessToken() throws IOException {
        if (tokenProvider != null) {
            return "Bearer " + tokenProvider.getToken().getAccessToken();
        } else {
            return null;
        }
    }

    String initializeUserAgent(final AbfsConfiguration abfsConfiguration,
                               final String sslProviderName) {
        StringBuilder sb = new StringBuilder();
        sb.append("(JavaJRE ");
        sb.append(System.getProperty(JAVA_VERSION));
        sb.append("; ");
        sb.append(
                System.getProperty(OS_NAME).replaceAll(SINGLE_WHITE_SPACE, EMPTY_STRING));
        sb.append(" ");
        sb.append(System.getProperty(OS_VERSION));
        if (sslProviderName != null && !sslProviderName.isEmpty()) {
            sb.append("; ");
            sb.append(sslProviderName);
        }
        String tokenProviderField =
                ExtensionHelper.getUserAgentSuffix(tokenProvider, "");
        if (!tokenProviderField.isEmpty()) {
            sb.append("; ").append(tokenProviderField);
        }
        sb.append(")");
        final String userAgentComment = sb.toString();
        String customUserAgentId = abfsConfiguration.getCustomUserAgentPrefix();
        if (customUserAgentId != null && !customUserAgentId.isEmpty()) {
            return String.format(Locale.ROOT, CLIENT_VERSION + " %s %s",
                    userAgentComment, customUserAgentId);
        }
        return String.format(Locale.ROOT, CLIENT_VERSION + " %s", userAgentComment);
    }

    URL getBaseUrl() {
        return baseUrl;
    }

    ExponentialRetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    SharedKeyCredentials getSharedKeyCredentials() {
        return sharedKeyCredentials;
    }
}