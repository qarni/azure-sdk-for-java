// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.storage.blob;

import com.azure.core.http.rest.Response;
import com.azure.core.exception.UnexpectedLengthException;
import com.azure.core.util.Context;
import com.azure.storage.blob.models.BlobAccessConditions;
import com.azure.storage.blob.models.BlobHTTPHeaders;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.CopyStatusType;
import com.azure.storage.blob.models.Metadata;
import com.azure.storage.blob.models.ModifiedAccessConditions;
import com.azure.storage.blob.models.PageBlobAccessConditions;
import com.azure.storage.blob.models.PageBlobItem;
import com.azure.storage.blob.models.PageList;
import com.azure.storage.blob.models.PageRange;
import com.azure.storage.blob.models.SequenceNumberActionType;
import com.azure.storage.blob.models.SourceModifiedAccessConditions;
import com.azure.storage.blob.models.StorageException;
import com.azure.storage.common.Utility;
import java.util.Objects;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Duration;

/**
 * Client to a page blob. It may only be instantiated through a {@link BlobClientBuilder}, via
 * the method {@link BlobClient#asPageBlobClient()}, or via the method
 * {@link ContainerClient#getPageBlobClient(String)}. This class does not hold
 * any state about a particular blob, but is instead a convenient way of sending appropriate
 * requests to the resource on the service.
 *
 * <p>
 * This client contains operations on a blob. Operations on a container are available on {@link ContainerClient},
 * and operations on the service are available on {@link BlobServiceClient}.
 *
 * <p>
 * Please refer to the <a href=https://docs.microsoft.com/en-us/rest/api/storageservices/understanding-block-blobs--append-blobs--and-page-blobs>Azure Docs</a>
 * for more information.
 */
public final class PageBlobClient extends BlobClient {
    private final PageBlobAsyncClient pageBlobAsyncClient;

    /**
     * Indicates the number of bytes in a page.
     */
    public static final int PAGE_BYTES = PageBlobAsyncClient.PAGE_BYTES;

    /**
     * Indicates the maximum number of bytes that may be sent in a call to putPage.
     */
    public static final int MAX_PUT_PAGES_BYTES = PageBlobAsyncClient.MAX_PUT_PAGES_BYTES;

    /**
     * Package-private constructor for use by {@link BlobClientBuilder}.
     * @param pageBlobAsyncClient the async page blob client
     */
    PageBlobClient(PageBlobAsyncClient pageBlobAsyncClient) {
        super(pageBlobAsyncClient);
        this.pageBlobAsyncClient = pageBlobAsyncClient;
    }

    /**
     * Creates and opens an output stream to write data to the page blob. If the blob already exists on the service,
     * it will be overwritten.
     *
     * @param length A <code>long</code> which represents the length, in bytes, of the stream to create. This value must be
     *            a multiple of 512.
     *
     * @return A {@link BlobOutputStream} object used to write data to the blob.
     *
     * @throws StorageException If a storage service error occurred.
     */
    public BlobOutputStream getBlobOutputStream(long length) {
        return getBlobOutputStream(length, null);
    }

    /**
     * Creates and opens an output stream to write data to the page blob. If the blob already exists on the service,
     * it will be overwritten.
     *
     * @param length A <code>long</code> which represents the length, in bytes, of the stream to create. This value must be
     *            a multiple of 512.
     * @param accessConditions A {@link BlobAccessConditions} object that represents the access conditions for the blob.
     *
     * @return A {@link BlobOutputStream} object used to write data to the blob.
     *
     * @throws StorageException If a storage service error occurred.
     */
    public BlobOutputStream getBlobOutputStream(long length, BlobAccessConditions accessConditions) {
        return BlobOutputStream.pageBlobOutputStream(pageBlobAsyncClient, length, accessConditions);
    }

    /**
     * Creates a page blob of the specified length. Call PutPage to upload data data to a page blob.
     * For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/put-blob">Azure Docs</a>.
     *
     * @param size Specifies the maximum size for the page blob, up to 8 TB. The page blob size must be aligned to a
     *         512-byte boundary.
     *
     * @return The information of the created page blob.
     */
    public PageBlobItem create(long size) {
        return createWithResponse(size, null, null, null, null, null, Context.NONE).value();
    }


    /**
     * Creates a page blob of the specified length. Call PutPage to upload data data to a page blob.
     * For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/put-blob">Azure Docs</a>.
     *
     * @param size Specifies the maximum size for the page blob, up to 8 TB. The page blob size must be aligned to a
     *         512-byte boundary.
     * @param sequenceNumber A user-controlled value that you can use to track requests. The value of the sequence number must be
     *         between 0 and 2^63 - 1.The default value is 0.
     * @param headers {@link BlobHTTPHeaders}
     * @param metadata {@link Metadata}
     * @param accessConditions {@link BlobAccessConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     *
     * @return The information of the created page blob.
     */
    public Response<PageBlobItem> createWithResponse(long size, Long sequenceNumber, BlobHTTPHeaders headers,
                                                     Metadata metadata, BlobAccessConditions accessConditions, Duration timeout, Context context) {
        Mono<Response<PageBlobItem>> response = pageBlobAsyncClient.createWithResponse(size, sequenceNumber, headers, metadata, accessConditions, context);
        return Utility.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Writes 1 or more pages to the page blob. The start and end offsets must be a multiple of 512.
     * For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/put-page">Azure Docs</a>.
     * <p>
     * Note that the data passed must be replayable if retries are enabled (the default). In other words, the
     * {@code Flux} must produce the same data each time it is subscribed to.
     *
     * @param pageRange A {@link PageRange} object. Given that pages must be aligned with 512-byte boundaries, the start offset must
     *         be a modulus of 512 and the end offset must be a modulus of 512 - 1. Examples of valid byte ranges are
     *         0-511, 512-1023, etc.
     * @param body The data to upload.
     *
     * @return The information of the uploaded pages.
     */
    public PageBlobItem uploadPages(PageRange pageRange, InputStream body) {
        return uploadPagesWithResponse(pageRange, body, null, null, Context.NONE).value();
    }

    /**
     * Writes 1 or more pages to the page blob. The start and end offsets must be a multiple of 512.
     * For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/put-page">Azure Docs</a>.
     * <p>
     * Note that the data passed must be replayable if retries are enabled (the default). In other words, the
     * {@code Flux} must produce the same data each time it is subscribed to.
     *
     * @param pageRange A {@link PageRange} object. Given that pages must be aligned with 512-byte boundaries, the start offset
     *         must be a modulus of 512 and the end offset must be a modulus of 512 - 1. Examples of valid byte ranges
     *         are 0-511, 512-1023, etc.
     * @param body The data to upload.
     * @param pageBlobAccessConditions {@link PageBlobAccessConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     *
     * @return The information of the uploaded pages.
     * @throws UnexpectedLengthException when the length of data does not match the input {@code length}.
     * @throws NullPointerException if the input data is null.
     */
    public Response<PageBlobItem> uploadPagesWithResponse(PageRange pageRange, InputStream body,
            PageBlobAccessConditions pageBlobAccessConditions, Duration timeout, Context context) {
        Objects.requireNonNull(body);
        final long length = pageRange.end() - pageRange.start() + 1;
        Flux<ByteBuffer> fbb = Utility.convertStreamToByteBuffer(body, length, PAGE_BYTES);

        Mono<Response<PageBlobItem>> response = pageBlobAsyncClient.uploadPagesWithResponse(pageRange,
            fbb.subscribeOn(Schedulers.elastic()),
            pageBlobAccessConditions, context);
        return Utility.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Writes 1 or more pages from the source page blob to this page blob. The start and end offsets must be a multiple
     * of 512.
     * For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/put-page">Azure Docs</a>.
     * <p>
     *
     * @param range A {@link PageRange} object. Given that pages must be aligned with 512-byte boundaries, the start offset
     *          must be a modulus of 512 and the end offset must be a modulus of 512 - 1. Examples of valid byte ranges
     *          are 0-511, 512-1023, etc.
     * @param sourceURL The url to the blob that will be the source of the copy.  A source blob in the same storage account can be
     *          authenticated via Shared Key. However, if the source is a blob in another account, the source blob must
     *          either be public or must be authenticated via a shared access signature. If the source blob is public, no
     *          authentication is required to perform the operation.
     * @param sourceOffset The source offset to copy from.  Pass null or 0 to copy from the beginning of source page blob.
     *
     * @return The information of the uploaded pages.
     */
    public PageBlobItem uploadPagesFromURL(PageRange range, URL sourceURL, Long sourceOffset) {
        return uploadPagesFromURLWithResponse(range, sourceURL, sourceOffset, null, null, null, null, Context.NONE).value();
    }

    /**
     * Writes 1 or more pages from the source page blob to this page blob. The start and end offsets must be a multiple
     * of 512.
     * For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/put-page">Azure Docs</a>.
     * <p>
     *
     * @param range The destination {@link PageRange} range. Given that pages must be aligned with 512-byte boundaries, the start offset
     *          must be a modulus of 512 and the end offset must be a modulus of 512 - 1. Examples of valid byte ranges
     *          are 0-511, 512-1023, etc.
     * @param sourceURL The url to the blob that will be the source of the copy.  A source blob in the same storage account can be
     *          authenticated via Shared Key. However, if the source is a blob in another account, the source blob must
     *          either be public or must be authenticated via a shared access signature. If the source blob is public, no
     *          authentication is required to perform the operation.
     * @param sourceOffset The source offset to copy from.  Pass null or 0 to copy from the beginning of source blob.
     * @param sourceContentMD5 An MD5 hash of the block content from the source blob. If specified, the service will calculate the MD5
     *          of the received data and fail the request if it does not match the provided MD5.
     * @param destAccessConditions {@link PageBlobAccessConditions}
     * @param sourceAccessConditions {@link SourceModifiedAccessConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     *
     * @return The information of the uploaded pages.
     */
    public Response<PageBlobItem> uploadPagesFromURLWithResponse(PageRange range, URL sourceURL, Long sourceOffset,
            byte[] sourceContentMD5, PageBlobAccessConditions destAccessConditions,
            SourceModifiedAccessConditions sourceAccessConditions, Duration timeout, Context context) {

        Mono<Response<PageBlobItem>> response = pageBlobAsyncClient.uploadPagesFromURLWithResponse(range, sourceURL, sourceOffset, sourceContentMD5, destAccessConditions, sourceAccessConditions, context);
        return Utility.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Frees the specified pages from the page blob.
     * For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/put-page">Azure Docs</a>.
     *
     * @param pageRange A {@link PageRange} object. Given that pages must be aligned with 512-byte boundaries, the start offset
     *         must be a modulus of 512 and the end offset must be a modulus of 512 - 1. Examples of valid byte ranges
     *         are 0-511, 512-1023, etc.
     *
     * @return The information of the cleared pages.
     */
    public PageBlobItem clearPages(PageRange pageRange) {
        return clearPagesWithResponse(pageRange, null, null, Context.NONE).value();
    }

    /**
     * Frees the specified pages from the page blob.
     * For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/put-page">Azure Docs</a>.
     *
     * @param pageRange A {@link PageRange} object. Given that pages must be aligned with 512-byte boundaries, the start offset
     *         must be a modulus of 512 and the end offset must be a modulus of 512 - 1. Examples of valid byte ranges
     *         are 0-511, 512-1023, etc.
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param pageBlobAccessConditions {@link PageBlobAccessConditions}
     * @param context Additional context that is passed through the Http pipeline during the service call.
     *
     * @return The information of the cleared pages.
     */
    public Response<PageBlobItem> clearPagesWithResponse(PageRange pageRange,
            PageBlobAccessConditions pageBlobAccessConditions, Duration timeout, Context context) {
        Mono<Response<PageBlobItem>> response = pageBlobAsyncClient.clearPagesWithResponse(pageRange, pageBlobAccessConditions, context);

        return Utility.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Returns the list of valid page ranges for a page blob or snapshot of a page blob.
     * For more information, see the <a href="https://docs.microsoft.com/rest/api/storageservices/get-page-ranges">Azure Docs</a>.
     *
     * @param blobRange {@link BlobRange}
     *
     * @return The information of the cleared pages.
     */
    public PageList getPageRanges(BlobRange blobRange) {
        return getPageRangesWithResponse(blobRange, null, null, Context.NONE).value();
    }

    /**
     * Returns the list of valid page ranges for a page blob or snapshot of a page blob.
     * For more information, see the <a href="https://docs.microsoft.com/rest/api/storageservices/get-page-ranges">Azure Docs</a>.
     *
     * @param blobRange {@link BlobRange}
     * @param accessConditions {@link BlobAccessConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     *
     * @return All the page ranges.
     */
    public Response<PageList> getPageRangesWithResponse(BlobRange blobRange, BlobAccessConditions accessConditions, Duration timeout, Context context) {
        return Utility.blockWithOptionalTimeout(pageBlobAsyncClient.getPageRangesWithResponse(blobRange, accessConditions, context), timeout);
    }

    /**
     * Gets the collection of page ranges that differ between a specified snapshot and this page blob.
     * For more information, see the <a href="https://docs.microsoft.com/rest/api/storageservices/get-page-ranges">Azure Docs</a>.
     *
     * @param blobRange {@link BlobRange}
     * @param prevSnapshot Specifies that the response will contain only pages that were changed between target blob and previous
     *         snapshot. Changed pages include both updated and cleared pages. The target
     *         blob may be a snapshot, as long as the snapshot specified by prevsnapshot is the older of the two.
     *
     * @return All the different page ranges.
     */
    public PageList getPageRangesDiff(BlobRange blobRange, String prevSnapshot) {
        return getPageRangesDiffWithResponse(blobRange, prevSnapshot, null, null, Context.NONE).value();
    }

    /**
     * Gets the collection of page ranges that differ between a specified snapshot and this page blob.
     * For more information, see the <a href="https://docs.microsoft.com/rest/api/storageservices/get-page-ranges">Azure Docs</a>.
     *
     * @param blobRange {@link BlobRange}
     * @param prevSnapshot Specifies that the response will contain only pages that were changed between target blob and previous
     *         snapshot. Changed pages include both updated and cleared pages. The target
     *         blob may be a snapshot, as long as the snapshot specified by prevsnapshot is the older of the two.
     * @param accessConditions {@link BlobAccessConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     *
     * @return All the different page ranges.
     */
    public Response<PageList> getPageRangesDiffWithResponse(BlobRange blobRange, String prevSnapshot, BlobAccessConditions accessConditions, Duration timeout, Context context) {
        return Utility.blockWithOptionalTimeout(pageBlobAsyncClient.getPageRangesDiffWithResponse(blobRange, prevSnapshot, accessConditions, context), timeout);
    }

    /**
     * Resizes the page blob to the specified size (which must be a multiple of 512).
     * For more information, see the <a href="https://docs.microsoft.com/rest/api/storageservices/set-blob-properties">Azure Docs</a>.
     *
     * @param size Resizes a page blob to the specified size. If the specified value is less than the current size of the
     *         blob, then all pages above the specified value are cleared.
     *
     * @return The resized page blob.
     */
    public PageBlobItem resize(long size) {
        return resizeWithResponse(size, null, null, Context.NONE).value();
    }

    /**
     * Resizes the page blob to the specified size (which must be a multiple of 512).
     * For more information, see the <a href="https://docs.microsoft.com/rest/api/storageservices/set-blob-properties">Azure Docs</a>.
     *
     * @param size Resizes a page blob to the specified size. If the specified value is less than the current size of the
     *         blob, then all pages above the specified value are cleared.
     * @param accessConditions {@link BlobAccessConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     *
     * @return The resized page blob.
     */
    public Response<PageBlobItem> resizeWithResponse(long size, BlobAccessConditions accessConditions, Duration timeout, Context context) {
        Mono<Response<PageBlobItem>> response = pageBlobAsyncClient.resizeWithResponse(size, accessConditions);
        return Utility.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Sets the page blob's sequence number.
     * For more information, see the <a href="https://docs.microsoft.com/rest/api/storageservices/set-blob-properties">Azure Docs</a>.
     *
     * @param action Indicates how the service should modify the blob's sequence number.
     * @param sequenceNumber The blob's sequence number. The sequence number is a user-controlled property that you can use to track
     *         requests and manage concurrency issues.
     *
     * @return The updated page blob.
     */
    public PageBlobItem updateSequenceNumber(SequenceNumberActionType action,
            Long sequenceNumber) {
        return updateSequenceNumberWithResponse(action, sequenceNumber, null, null, Context.NONE).value();
    }

    /**
     * Sets the page blob's sequence number.
     * For more information, see the <a href="https://docs.microsoft.com/rest/api/storageservices/set-blob-properties">Azure Docs</a>.
     *
     * @param action Indicates how the service should modify the blob's sequence number.
     * @param sequenceNumber The blob's sequence number. The sequence number is a user-controlled property that you can use to track
     *         requests and manage concurrency issues.
     * @param accessConditions {@link BlobAccessConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     *
     * @return The updated page blob.
     */
    public Response<PageBlobItem> updateSequenceNumberWithResponse(SequenceNumberActionType action,
            Long sequenceNumber, BlobAccessConditions accessConditions, Duration timeout, Context context) {
        Mono<Response<PageBlobItem>> response = pageBlobAsyncClient.updateSequenceNumberWithResponse(action, sequenceNumber, accessConditions, context);
        return Utility.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Begins an operation to start an incremental copy from one page blob's snapshot to this page
     * blob. The snapshot is copied such that only the differential changes between the previously copied snapshot are
     * transferred to the destination. The copied snapshots are complete copies of the original snapshot and can be read
     * or copied from as usual. For more information, see
     * the Azure Docs <a href="https://docs.microsoft.com/rest/api/storageservices/incremental-copy-blob">here</a> and
     * <a href="https://docs.microsoft.com/en-us/azure/virtual-machines/windows/incremental-snapshots">here</a>.
     *
     * @param source The source page blob.
     * @param snapshot The snapshot on the copy source.
     *
     * @return The copy status.
     */
    public CopyStatusType copyIncremental(URL source, String snapshot) {
        return copyIncrementalWithResponse(source, snapshot, null, null, Context.NONE).value();
    }

    /**
     * Begins an operation to start an incremental copy from one page blob's snapshot to this page
     * blob. The snapshot is copied such that only the differential changes between the previously copied snapshot are
     * transferred to the destination. The copied snapshots are complete copies of the original snapshot and can be read
     * or copied from as usual. For more information, see
     * the Azure Docs <a href="https://docs.microsoft.com/rest/api/storageservices/incremental-copy-blob">here</a> and
     * <a href="https://docs.microsoft.com/en-us/azure/virtual-machines/windows/incremental-snapshots">here</a>.
     *
     * @param source The source page blob.
     * @param snapshot The snapshot on the copy source.
     * @param modifiedAccessConditions Standard HTTP Access conditions related to the modification of data. ETag and LastModifiedTime are used
     *         to construct conditions related to when the blob was changed relative to the given request. The request
     *         will fail if the specified condition is not satisfied.
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     *
     * @return The copy status.
     */
    public Response<CopyStatusType> copyIncrementalWithResponse(URL source, String snapshot,
            ModifiedAccessConditions modifiedAccessConditions, Duration timeout, Context context) {
        Mono<Response<CopyStatusType>> response = pageBlobAsyncClient.copyIncrementalWithResponse(source, snapshot, modifiedAccessConditions, context);
        return Utility.blockWithOptionalTimeout(response, timeout);
    }
}
