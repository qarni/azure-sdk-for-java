// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.http.policy;

import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpResponse;
import com.azure.core.implementation.http.UrlBuilder;
import com.azure.core.util.logging.ClientLogger;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;

/**
 * The Pipeline policy that adds a given protocol to each HttpRequest.
 */
public class ProtocolPolicy implements HttpPipelinePolicy {
    private final String protocol;
    private final boolean overwrite;
    private final ClientLogger logger = new ClientLogger(ProtocolPolicy.class);

    /**
     * Create a new ProtocolPolicy.
     *
     * @param protocol The protocol to set.
     * @param overwrite Whether or not to overwrite a HttpRequest's protocol if it already has one.
     */
    public ProtocolPolicy(String protocol, boolean overwrite) {
        this.protocol = protocol;
        this.overwrite = overwrite;
    }

    @Override
    public Mono<HttpResponse> process(HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
        final UrlBuilder urlBuilder = UrlBuilder.parse(context.httpRequest().url());
        if (overwrite || urlBuilder.scheme() == null) {
            logger.info("Setting protocol to {}", protocol);

            try {
                context.httpRequest().url(urlBuilder.scheme(protocol).toURL());
            } catch (MalformedURLException e) {
                return Mono.error(e);
            }
        }
        return next.process();
    }
}
