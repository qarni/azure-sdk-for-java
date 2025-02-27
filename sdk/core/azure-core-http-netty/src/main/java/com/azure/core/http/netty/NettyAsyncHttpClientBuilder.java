// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.http.netty;

import com.azure.core.http.ProxyOptions;
import com.azure.core.util.logging.ClientLogger;
import io.netty.channel.nio.NioEventLoopGroup;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.ProxyProvider;

/**
 * Builder class responsible for creating instances of {@link NettyAsyncHttpClient}.
 *
 * <p><strong>Building a new HttpClient instance</strong></p>
 *
 * {@codesnippet com.azure.core.http.netty.instantiation-simple}
 *
 * @see NettyAsyncHttpClient
 * @see HttpClient
 */
public class NettyAsyncHttpClientBuilder {
    private final ClientLogger logger = new ClientLogger(NettyAsyncHttpClientBuilder.class);

    private ProxyOptions proxyOptions;
    private boolean enableWiretap;
    private int port = 80;
    private NioEventLoopGroup nioEventLoopGroup;

    /**
     * Creates a new builder instance, where a builder is capable of generating multiple instances of
     * {@link NettyAsyncHttpClient}.
     */
    public NettyAsyncHttpClientBuilder() {
    }

    /**
     * Creates a new {@link NettyAsyncHttpClient} instance on every call, using the configuration set in the builder at
     * the time of the build method call.
     *
     * @return A new NettyAsyncHttpClient instance.
     * @throws IllegalStateException If the builder is configured to use an unknown proxy type.
     */
    public NettyAsyncHttpClient build() {
        HttpClient nettyHttpClient = HttpClient.create()
            .port(port)
            .wiretap(enableWiretap)
            .tcpConfiguration(tcpConfig -> {
                if (nioEventLoopGroup != null) {
                    tcpConfig = tcpConfig.runOn(nioEventLoopGroup);
                }

                if (proxyOptions != null) {
                    ProxyProvider.Proxy nettyProxy;
                    switch (proxyOptions.type()) {
                        case HTTP:
                            nettyProxy = ProxyProvider.Proxy.HTTP;
                            break;
                        case SOCKS4:
                            nettyProxy = ProxyProvider.Proxy.SOCKS4;
                            break;
                        case SOCKS5:
                            nettyProxy = ProxyProvider.Proxy.SOCKS5;
                            break;
                        default:
                            throw logger.logExceptionAsWarning(
                                new IllegalStateException(
                                    "Unknown Proxy type '" + proxyOptions.type()
                                        + "' in use. Not configuring Netty proxy."));
                    }

                    return tcpConfig.proxy(ts -> ts.type(nettyProxy).address(proxyOptions.address()));
                }

                return tcpConfig;
            });
        return new NettyAsyncHttpClient(nettyHttpClient);
    }

    /**
     * Sets the {@link ProxyOptions proxy options} that the client will use.
     *
     * @param proxyOptions The proxy configuration to use.
     * @return the updated NettyAsyncHttpClientBuilder object
     */
    public NettyAsyncHttpClientBuilder setProxy(ProxyOptions proxyOptions) {
        this.proxyOptions = proxyOptions;
        return this;
    }

    /**
     * Enables the Netty wiretap feature.
     *
     * @param enableWiretap Flag indicating wiretap status
     * @return the updated NettyAsyncHttpClientBuilder object
     */
    public NettyAsyncHttpClientBuilder setWiretap(boolean enableWiretap) {
        this.enableWiretap = enableWiretap;
        return this;
    }

    /**
     * Sets the port which this client should connect, which by default will be set to port 80.
     *
     * @param port The port to connect to.
     * @return the updated NettyAsyncHttpClientBuilder object
     */
    public NettyAsyncHttpClientBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Sets the NIO event loop group that will be used to run IO loops. For example, a fixed thread pool can be
     * specified as shown below:
     *
     * {@codesnippet com.azure.core.http.netty.NettyAsyncHttpClientBuilder#NioEventLoopGroup}
     *
     * @param nioEventLoopGroup The {@link NioEventLoopGroup} that will run IO loops.
     * @return the updated NettyAsyncHttpClientBuilder object
     */
    public NettyAsyncHttpClientBuilder setNioEventLoopGroup(NioEventLoopGroup nioEventLoopGroup) {
        this.nioEventLoopGroup = nioEventLoopGroup;
        return this;
    }
}
