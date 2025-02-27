// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.http;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.rest.Page;
import com.azure.core.http.rest.PagedResponse;

import java.util.List;

/**
 * Represents an HTTP response that contains a list of items deserialized into a {@link Page}.
 *
 * @param <H> The HTTP response headers
 * @param <T> The type of items contained in the {@link Page}
 * @see com.azure.core.http.rest.PagedResponse
 */
public class PagedResponseBase<H, T> implements PagedResponse<T> {
    private final HttpRequest request;
    private final int statusCode;
    private final H deserializedHeaders;
    private final HttpHeaders headers;
    private final List<T> items;
    private final String nextLink;

    public PagedResponseBase(HttpRequest request, int statusCode, HttpHeaders headers, Page<T> page,
                             H deserializedHeaders) {
        this(request, statusCode, headers, page.items(), page.nextLink(), deserializedHeaders);
    }

    public PagedResponseBase(HttpRequest request, int statusCode, HttpHeaders headers, List<T> items, String nextLink,
                             H deserializedHeaders) {
        this.request = request;
        this.statusCode = statusCode;
        this.headers = headers;
        this.items = items;
        this.nextLink = nextLink;
        this.deserializedHeaders = deserializedHeaders;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> items() {
        return items;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String nextLink() {
        return nextLink;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int statusCode() {
        return statusCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpHeaders headers() {
        return headers;
    }

    /**
     * @return the request which resulted in this PagedRequestResponse.
     */
    @Override
    public HttpRequest request() {
        return request;
    }

    /**
     * Get the headers from the HTTP response, transformed into the header type H.
     *
     * @return an instance of header type H, containing the HTTP response headers.
     */
    public H deserializedHeaders() {
        return deserializedHeaders;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
    }
}
