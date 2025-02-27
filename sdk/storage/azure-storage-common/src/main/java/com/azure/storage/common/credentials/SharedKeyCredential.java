// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.storage.common.credentials;

import com.azure.core.implementation.util.ImplUtils;
import com.azure.storage.common.Utility;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * SharedKey credential policy that is put into a header to authorize requests.
 */
public final class SharedKeyCredential {
    private static final String AUTHORIZATION_HEADER_FORMAT = "SharedKey %s:%s";

    // Pieces of the connection string that are needed.
    private static final String ACCOUNT_NAME = "accountname";
    private static final String ACCOUNT_KEY = "accountkey";

    private final String accountName;
    private final String accountKey;

    /**
     * Initializes a new instance of SharedKeyCredential contains an account's name and its primary or secondary
     * accountKey.
     *
     * @param accountName The account name associated with the request.
     * @param accountKey The account access key used to authenticate the request.
     */
    public SharedKeyCredential(String accountName, String accountKey) {
        Objects.requireNonNull(accountName);
        Objects.requireNonNull(accountKey);
        this.accountName = accountName;
        this.accountKey = accountKey;
    }

    /**
     * Creates a SharedKey credential from the passed connection string.
     * @param connectionString Connection string used to build the SharedKey credential.
     * @return a SharedKey credential if the connection string contains AccountName and AccountKey
     * @throws IllegalArgumentException If {@code connectionString} doesn't have AccountName or AccountKey.
     */
    public static SharedKeyCredential fromConnectionString(String connectionString) {
        HashMap<String, String> connectionStringPieces = new HashMap<>();
        for (String connectionStringPiece : connectionString.split(";")) {
            String[] kvp = connectionStringPiece.split("=", 2);
            connectionStringPieces.put(kvp[0].toLowerCase(Locale.ROOT), kvp[1]);
        }

        String accountName = connectionStringPieces.get(ACCOUNT_NAME);
        String accountKey = connectionStringPieces.get(ACCOUNT_KEY);

        if (ImplUtils.isNullOrEmpty(accountName) || ImplUtils.isNullOrEmpty(accountKey)) {
            throw new IllegalArgumentException("Connection string must contain 'AccountName' and 'AccountKey'.");
        }

        return new SharedKeyCredential(accountName, accountKey);
    }

    /**
     * Gets the account name associated with the request.
     *
     * @return The account name.
     */
    public String accountName() {
        return accountName;
    }

    /**
     * Generates the SharedKey Authorization value from information in the request.
     * @param requestURL URL of the request
     * @param httpMethod HTTP method being used
     * @param headers Headers on the request
     * @return the SharedKey authorization value
     */
    public String generateAuthorizationHeader(URL requestURL, String httpMethod, Map<String, String> headers) {
        String signature = Utility.computeHMac256(accountKey, buildStringToSign(requestURL, httpMethod, headers));
        return String.format(AUTHORIZATION_HEADER_FORMAT, accountName, signature);
    }

    /**
     * Computes a signature for the specified string using the HMAC-SHA256 algorithm.
     * Package-private because it is used to generate SAS signatures.
     *
     * @param stringToSign The UTF-8-encoded string to sign.
     * @return A {@code String} that contains the HMAC-SHA256-encoded signature.
     * @throws RuntimeException If the HMAC-SHA256 algorithm isn't support, if the key isn't a valid Base64 encoded
     * string, or the UTF-8 charset isn't supported.
     */
    public String computeHmac256(final String stringToSign) {
        return Utility.computeHMac256(accountKey, stringToSign);
    }

    private String buildStringToSign(URL requestURL, String httpMethod, Map<String, String> headers) {
        String contentLength = headers.get("Content-Length");
        contentLength = contentLength.equals("0") ? "" : contentLength;

        // If the x-ms-header exists ignore the Date header
        String dateHeader = (headers.containsKey("x-ms-date")) ? "" : getStandardHeaderValue(headers, "Date");

        return String.join("\n",
            httpMethod,
            getStandardHeaderValue(headers, "Content-Encoding"),
            getStandardHeaderValue(headers, "Content-Language"),
            contentLength,
            getStandardHeaderValue(headers, "Content-MD5"),
            getStandardHeaderValue(headers, "Content-Type"),
            dateHeader,
            getStandardHeaderValue(headers, "If-Modified-Since"),
            getStandardHeaderValue(headers, "If-Match"),
            getStandardHeaderValue(headers, "If-None-Match"),
            getStandardHeaderValue(headers, "If-Unmodified-Since"),
            getStandardHeaderValue(headers, "Range"),
            getAdditionalXmsHeaders(headers),
            getCanonicalizedResource(requestURL));
    }

    /*
     * Returns an empty string if the header value is null or empty.
     */
    private String getStandardHeaderValue(Map<String, String> headers, String headerName) {
        final String headerValue = headers.get(headerName);

        return headerValue == null ? "" : headerValue;
    }

    private String getAdditionalXmsHeaders(Map<String, String> headers) {
        // Add only headers that begin with 'x-ms-'
        final List<String> xmsHeaderNameArray = headers.entrySet().stream()
            .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).startsWith("x-ms-"))
            .filter(entry -> entry.getValue() != null)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if (xmsHeaderNameArray.isEmpty()) {
            return "";
        }

        Collections.sort(xmsHeaderNameArray);

        final StringBuilder canonicalizedHeaders = new StringBuilder();
        for (final String key : xmsHeaderNameArray) {
            if (canonicalizedHeaders.length() > 0) {
                canonicalizedHeaders.append('\n');
            }

            canonicalizedHeaders.append(key)
                .append(':')
                .append(headers.get(key));
        }

        return canonicalizedHeaders.toString();
    }

    private String getCanonicalizedResource(URL requestURL) {

        // Resource path
        final StringBuilder canonicalizedResource = new StringBuilder("/");
        canonicalizedResource.append(accountName);

        // Note that AbsolutePath starts with a '/'.
        if (requestURL.getPath().length() > 0) {
            canonicalizedResource.append(requestURL.getPath());
        } else {
            canonicalizedResource.append('/');
        }

        // check for no query params and return
        if (requestURL.getQuery() == null) {
            return canonicalizedResource.toString();
        }

        // The URL object's query field doesn't include the '?'. The QueryStringDecoder expects it.
        Map<String, String[]> queryParams = Utility.parseQueryStringSplitValues(requestURL.getQuery());

        ArrayList<String> queryParamNames = new ArrayList<>(queryParams.keySet());
        Collections.sort(queryParamNames);

        for (String queryParamName : queryParamNames) {
            String[] queryParamValues = queryParams.get(queryParamName);
            Arrays.sort(queryParamValues);
            String queryParamValuesStr = String.join(",", queryParamValues);
            canonicalizedResource.append("\n")
                .append(queryParamName.toLowerCase(Locale.ROOT))
                .append(":")
                .append(queryParamValuesStr);
        }

        // append to main string builder the join of completed params with new line
        return canonicalizedResource.toString();
    }
}
