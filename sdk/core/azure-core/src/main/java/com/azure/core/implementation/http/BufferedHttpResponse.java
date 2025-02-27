// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.http;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * HTTP response which will buffer the response's body when/if it is read.
 */
public final class BufferedHttpResponse extends HttpResponse {
    private final HttpResponse innerHttpResponse;
    private final Mono<byte[]> cachedBody;

    /**
     * Creates a buffered HTTP response.
     *
     * @param innerHttpResponse The HTTP response to buffer
     */
    public BufferedHttpResponse(HttpResponse innerHttpResponse) {
        this.innerHttpResponse = innerHttpResponse;
        this.cachedBody = innerHttpResponse.bodyAsByteArray().cache();
        this.request(innerHttpResponse.request());
    }

    @Override
    public int statusCode() {
        return innerHttpResponse.statusCode();
    }

    @Override
    public String headerValue(String name) {
        return innerHttpResponse.headerValue(name);
    }

    @Override
    public HttpHeaders headers() {
        return innerHttpResponse.headers();
    }

    @Override
    public Mono<byte[]> bodyAsByteArray() {
        return cachedBody;
    }

    @Override
    public Flux<ByteBuffer> body() {
        return bodyAsByteArray().flatMapMany(bytes -> Flux.just(ByteBuffer.wrap(bytes)));
    }

    @Override
    public Mono<String> bodyAsString() {
        return bodyAsByteArray()
                .map(bytes -> bytes == null ? null : new String(bytes, StandardCharsets.UTF_8));
    }

    @Override
    public Mono<String> bodyAsString(Charset charset) {
        return bodyAsByteArray()
                .map(bytes -> bytes == null ? null : new String(bytes, charset));
    }

    @Override
    public BufferedHttpResponse buffer() {
        return this;
    }
}
