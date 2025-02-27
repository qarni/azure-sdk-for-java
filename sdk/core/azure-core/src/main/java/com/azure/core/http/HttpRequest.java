// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.http;

import reactor.core.publisher.Flux;

import java.io.Serializable;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * The outgoing Http request.
 */
public class HttpRequest implements Serializable {
    private static final long serialVersionUID = 6338479743058758810L;

    private HttpMethod httpMethod;
    private URL url;
    private HttpHeaders headers;
    private Flux<ByteBuffer> body;

    /**
     * Create a new HttpRequest instance.
     *
     * @param httpMethod the HTTP request method
     * @param url the target address to send the request to
     */
    public HttpRequest(HttpMethod httpMethod, URL url) {
        this.httpMethod = httpMethod;
        this.url = url;
        this.headers = new HttpHeaders();
    }

    /**
     * Create a new HttpRequest instance.
     *
     * @param httpMethod the HTTP request method
     * @param url the target address to send the request to
     * @param headers the HTTP headers to use with this request
     * @param body the request content
     */
    public HttpRequest(HttpMethod httpMethod, URL url, HttpHeaders headers, Flux<ByteBuffer> body) {
        this.httpMethod = httpMethod;
        this.url = url;
        this.headers = headers;
        this.body = body;
    }

    /**
     * Get the request method.
     *
     * @return the request method
     */
    public HttpMethod httpMethod() {
        return httpMethod;
    }

    /**
     * Set the request method.
     *
     * @param httpMethod the request method
     * @return this HttpRequest
     */
    public HttpRequest httpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    /**
     * Get the target address.
     *
     * @return the target address
     */
    public URL url() {
        return url;
    }

    /**
     * Set the target address to send the request to.
     *
     * @param url target address as {@link URL}
     * @return this HttpRequest
     */
    public HttpRequest url(URL url) {
        this.url = url;
        return this;
    }

    /**
     * Get the request headers.
     *
     * @return headers to be sent
     */
    public HttpHeaders headers() {
        return headers;
    }

    /**
     * Set the request headers.
     *
     * @param headers the set of headers
     * @return this HttpRequest
     */
    public HttpRequest headers(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }

    /**
     * Set a request header, replacing any existing value.
     * A null for {@code value} will remove the header if one with matching name exists.
     *
     * @param name the header name
     * @param value the header value
     * @return this HttpRequest
     */
    public HttpRequest header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    /**
     * Get the request content.
     *
     * @return the content to be send
     */
    public Flux<ByteBuffer> body() {
        return body;
    }

    /**
     * Set the request content.
     *
     * @param content the request content
     * @return this HttpRequest
     */
    public HttpRequest body(String content) {
        final byte[] bodyBytes = content.getBytes(StandardCharsets.UTF_8);
        return body(bodyBytes);
    }

    /**
     * Set the request content.
     * The Content-Length header will be set based on the given content's length
     *
     * @param content the request content
     * @return this HttpRequest
     */
    public HttpRequest body(byte[] content) {
        headers.put("Content-Length", String.valueOf(content.length));
        return body(Flux.defer(() -> Flux.just(ByteBuffer.wrap(content))));
    }

    /**
     * Set request content.
     *
     * Caller must set the Content-Length header to indicate the length of the content,
     * or use Transfer-Encoding: chunked.
     *
     * @param content the request content
     * @return this HttpRequest
     */
    public HttpRequest body(Flux<ByteBuffer> content) {
        this.body = content;
        return this;
    }

    /**
     * Creates a clone of the request.
     *
     * The main purpose of this is so that this HttpRequest can be changed and the resulting
     * HttpRequest can be a backup. This means that the buffered HttpHeaders and body must
     * not be able to change from side effects of this HttpRequest.
     *
     * @return a new HTTP request instance with cloned instances of all mutable properties.
     */
    public HttpRequest buffer() {
        final HttpHeaders bufferedHeaders = new HttpHeaders(headers);
        return new HttpRequest(httpMethod, url, bufferedHeaders, body);
    }
}
