// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.storage.blob;

import com.azure.core.http.rest.Response;
import com.azure.core.http.rest.SimpleResponse;
import com.azure.core.implementation.util.FluxUtil;
import com.azure.core.util.Context;
import com.azure.storage.blob.implementation.AzureBlobStorageImpl;
import com.azure.storage.blob.models.AppendBlobAccessConditions;
import com.azure.storage.blob.models.AppendBlobItem;
import com.azure.storage.blob.models.BlobAccessConditions;
import com.azure.storage.blob.models.BlobHTTPHeaders;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.Metadata;
import com.azure.storage.blob.models.SourceModifiedAccessConditions;
import com.azure.storage.common.Constants;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.nio.ByteBuffer;

import static com.azure.storage.blob.PostProcessor.postProcessResponse;
import static com.azure.core.implementation.util.FluxUtil.withContext;


/**
 * Client to an append blob. It may only be instantiated through a {@link BlobClientBuilder#buildAppendBlobAsyncClient()},
 * via the method {@link BlobAsyncClient#asAppendBlobAsyncClient()}, or via the method
 * {@link ContainerAsyncClient#getAppendBlobAsyncClient(String)}. This class does not hold
 * any state about a particular blob, but is instead a convenient way of sending appropriate
 * requests to the resource on the service.
 *
 * <p>
 * This client contains operations on a blob. Operations on a container are available on {@link ContainerAsyncClient},
 * and operations on the service are available on {@link BlobServiceAsyncClient}.
 *
 * <p>
 * Please refer
 * to the <a href=https://docs.microsoft.com/en-us/rest/api/storageservices/understanding-block-blobs--append-blobs--and-page-blobs>Azure Docs</a>
 * for more information.
 *
 * <p>
 * Note this client is an async client that returns reactive responses from Spring Reactor Core
 * project (https://projectreactor.io/). Calling the methods in this client will <strong>NOT</strong>
 * start the actual network operation, until {@code .subscribe()} is called on the reactive response.
 * You can simply convert one of these responses to a {@link java.util.concurrent.CompletableFuture}
 * object through {@link Mono#toFuture()}.
 */
public final class AppendBlobAsyncClient extends BlobAsyncClient {
    /**
     * Indicates the maximum number of bytes that can be sent in a call to appendBlock.
     */
    public static final int MAX_APPEND_BLOCK_BYTES = 4 * Constants.MB;

    /**
     * Indicates the maximum number of blocks allowed in an append blob.
     */
    public static final int MAX_BLOCKS = 50000;

    /**
     * Package-private constructor for use by {@link BlobClientBuilder}.
     * @param azureBlobStorage the API client for blob storage
     */
    AppendBlobAsyncClient(AzureBlobStorageImpl azureBlobStorage, String snapshot) {
        super(azureBlobStorage, snapshot);
    }

    /**
     * Creates a 0-length append blob. Call appendBlock to append data to an append blob.
     *
     * @return A {@link Mono} containing the information of the created appended blob.
     */
    public Mono<AppendBlobItem> create() {
        return createWithResponse(null, null, null).flatMap(FluxUtil::toMono);
    }

    /**
     * Creates a 0-length append blob. Call appendBlock to append data to an append blob.
     * @param headers {@link BlobHTTPHeaders}
     * @param metadata {@link Metadata}
     * @param accessConditions {@link BlobAccessConditions}
     *
     * @return A {@link Mono} containing {@link Response} whose {@link Response#value() value} contains the created appended blob.
     */
    public Mono<Response<AppendBlobItem>> createWithResponse(BlobHTTPHeaders headers, Metadata metadata, BlobAccessConditions accessConditions) {
        return withContext(context -> createWithResponse(headers, metadata, accessConditions, context));
    }

    Mono<Response<AppendBlobItem>> createWithResponse(BlobHTTPHeaders headers, Metadata metadata, BlobAccessConditions accessConditions, Context context) {
        metadata = (metadata == null) ? new Metadata() : metadata;
        accessConditions = (accessConditions == null) ? new BlobAccessConditions() : accessConditions;

        return postProcessResponse(this.azureBlobStorage.appendBlobs().createWithRestResponseAsync(null,
            null, 0, null, metadata, null, headers, accessConditions.leaseAccessConditions(), null,
            accessConditions.modifiedAccessConditions(), context))
            .map(rb -> new SimpleResponse<>(rb, new AppendBlobItem(rb.deserializedHeaders())));
    }

    /**
     * Commits a new block of data to the end of the existing append blob.
     * <p>
     * Note that the data passed must be replayable if retries are enabled (the default). In other words, the
     * {@code Flux} must produce the same data each time it is subscribed to.
     * @param data The data to write to the blob. Note that this {@code Flux} must be replayable if retries are enabled
     *         (the default). In other words, the Flux must produce the same data each time it is subscribed to.
     * @param length The exact length of the data. It is important that this value match precisely the length of the data
     *          emitted by the {@code Flux}.
     *
     * @return {@link Mono} containing the information of the append blob operation.
     */
    public Mono<AppendBlobItem> appendBlock(Flux<ByteBuffer> data, long length) {
        return appendBlockWithResponse(data, length, null).flatMap(FluxUtil::toMono);
    }

    /**
     * Commits a new block of data to the end of the existing append blob.
     * <p>
     * Note that the data passed must be replayable if retries are enabled (the default). In other words, the
     * {@code Flux} must produce the same data each time it is subscribed to.
     * @param data The data to write to the blob. Note that this {@code Flux} must be replayable if retries are enabled
     *         (the default). In other words, the Flux must produce the same data each time it is subscribed to.
     * @param length The exact length of the data. It is important that this value match precisely the length of the data
     *         emitted by the {@code Flux}.
     * @param appendBlobAccessConditions {@link AppendBlobAccessConditions}
     *
     * @return A {@link Mono} containing {@link Response} whose {@link Response#value() value} contains the append blob operation.
     */
    public Mono<Response<AppendBlobItem>> appendBlockWithResponse(Flux<ByteBuffer> data, long length,
                                                                  AppendBlobAccessConditions appendBlobAccessConditions) {
        return withContext(context -> appendBlockWithResponse(data, length, appendBlobAccessConditions, context));
    }

    Mono<Response<AppendBlobItem>> appendBlockWithResponse(Flux<ByteBuffer> data, long length,
                                                           AppendBlobAccessConditions appendBlobAccessConditions, Context context) {
        appendBlobAccessConditions = appendBlobAccessConditions == null ? new AppendBlobAccessConditions()
            : appendBlobAccessConditions;

        return postProcessResponse(this.azureBlobStorage.appendBlobs().appendBlockWithRestResponseAsync(
            null, null, data, length, null, null, null, null,
            appendBlobAccessConditions.leaseAccessConditions(),
            appendBlobAccessConditions.appendPositionAccessConditions(), null,
            appendBlobAccessConditions.modifiedAccessConditions(), context))
            .map(rb -> new SimpleResponse<>(rb, new AppendBlobItem(rb.deserializedHeaders())));

    }

    /**
     * Commits a new block of data from another blob to the end of this append blob.
     * @param sourceURL The url to the blob that will be the source of the copy.  A source blob in the same storage account can
     *          be authenticated via Shared Key. However, if the source is a blob in another account, the source blob
     *          must either be public or must be authenticated via a shared access signature. If the source blob is
     *          public, no authentication is required to perform the operation.
     * @param sourceRange The source {@link BlobRange} to copy.
     *
     * @return {@link Mono} containing the information of the append blob operation.
     */
    public Mono<AppendBlobItem> appendBlockFromUrl(URL sourceURL, BlobRange sourceRange) {
        return appendBlockFromUrlWithResponse(sourceURL, sourceRange, null, null, null).flatMap(FluxUtil::toMono);
    }

    /**
     * Commits a new block of data from another blob to the end of this append blob.
     * @param sourceURL The url to the blob that will be the source of the copy.  A source blob in the same storage account can
     *          be authenticated via Shared Key. However, if the source is a blob in another account, the source blob
     *          must either be public or must be authenticated via a shared access signature. If the source blob is
     *          public, no authentication is required to perform the operation.
     * @param sourceRange {@link BlobRange}
     * @param sourceContentMD5 An MD5 hash of the block content from the source blob. If specified, the service will calculate the MD5
     *          of the received data and fail the request if it does not match the provided MD5.
     * @param destAccessConditions {@link AppendBlobAccessConditions}
     * @param sourceAccessConditions {@link SourceModifiedAccessConditions}
     *
     * @return A {@link Mono} containing {@link Response} whose {@link Response#value() value} contains the append blob operation.
     */
    public Mono<Response<AppendBlobItem>> appendBlockFromUrlWithResponse(URL sourceURL, BlobRange sourceRange,
                                                                         byte[] sourceContentMD5, AppendBlobAccessConditions destAccessConditions,
                                                                         SourceModifiedAccessConditions sourceAccessConditions) {
        return withContext(context -> appendBlockFromUrlWithResponse(sourceURL, sourceRange, sourceContentMD5,
            destAccessConditions, sourceAccessConditions, context));
    }

    Mono<Response<AppendBlobItem>> appendBlockFromUrlWithResponse(URL sourceURL, BlobRange sourceRange,
                                                                  byte[] sourceContentMD5, AppendBlobAccessConditions destAccessConditions,
                                                                  SourceModifiedAccessConditions sourceAccessConditions, Context context) {
        sourceRange = sourceRange == null ? new BlobRange(0) : sourceRange;
        destAccessConditions = destAccessConditions == null
            ? new AppendBlobAccessConditions() : destAccessConditions;

        return postProcessResponse(
            this.azureBlobStorage.appendBlobs().appendBlockFromUrlWithRestResponseAsync(null, null,
                sourceURL, 0, sourceRange.toString(), sourceContentMD5, null, null, null, null, null,
                destAccessConditions.leaseAccessConditions(),
                destAccessConditions.appendPositionAccessConditions(),
                destAccessConditions.modifiedAccessConditions(), sourceAccessConditions, context))
            .map(rb -> new SimpleResponse<>(rb, new AppendBlobItem(rb.deserializedHeaders())));
    }
}
