// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.storage.queue;

import com.azure.core.http.rest.Response;
import com.azure.core.http.rest.SimpleResponse;
import com.azure.core.http.rest.VoidResponse;
import com.azure.storage.common.AccountSASPermission;
import com.azure.storage.common.AccountSASResourceType;
import com.azure.storage.common.AccountSASService;
import com.azure.storage.common.IPRange;
import com.azure.storage.common.SASProtocol;
import com.azure.core.util.Context;
import com.azure.storage.common.credentials.SASTokenCredential;
import com.azure.storage.common.credentials.SharedKeyCredential;
import com.azure.storage.queue.models.CorsRule;
import com.azure.storage.queue.models.QueueItem;
import com.azure.storage.queue.models.QueuesSegmentOptions;
import com.azure.storage.queue.models.StorageException;
import com.azure.storage.queue.models.StorageServiceProperties;
import com.azure.storage.queue.models.StorageServiceStats;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * This class provides a client that contains all the operations for interacting with a queue account in Azure Storage.
 * Operations allowed by the client are creating, listing, and deleting queues, retrieving and updating properties of the account,
 * and retrieving statistics of the account.
 *
 * <p><strong>Instantiating an Synchronous Queue Service Client</strong></p>
 *
 * {@codesnippet com.azure.storage.queue.queueServiceClient.instantiation}
 *
 * <p>View {@link QueueServiceClientBuilder this} for additional ways to construct the client.</p>
 *
 * @see QueueServiceClientBuilder
 * @see QueueServiceAsyncClient
 * @see SharedKeyCredential
 * @see SASTokenCredential
 */
public final class QueueServiceClient {
    private final QueueServiceAsyncClient client;

    /**
     * Creates a QueueServiceClient that wraps a QueueServiceAsyncClient and blocks requests.
     *
     * @param client QueueServiceAsyncClient that is used to send requests
     */
    QueueServiceClient(QueueServiceAsyncClient client) {
        this.client = client;
    }

    /**
     * @return the URL of the storage queue
     */
    public URL getQueueServiceUrl() {
        return client.getQueueServiceUrl();
    }

    /**
     * Constructs a QueueClient that interacts with the specified queue.
     *
     * This will not create the queue in the storage account if it doesn't exist.
     *
     * @param queueName Name of the queue
     * @return QueueClient that interacts with the specified queue
     */
    public QueueClient getQueueClient(String queueName) {
        return new QueueClient(client.getQueueAsyncClient(queueName));
    }

    /**
     * Creates a queue in the storage account with the specified name and returns a QueueClient to interact with it.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Create the queue "test"</p>
     *
     * {@codesnippet com.azure.storage.queue.queueServiceClient.createQueue#string}
     *
     * @param queueName Name of the queue
     * @return A response containing the QueueClient and the status of creating the queue
     * @throws StorageException If a queue with the same name and different metadata already exists
     */
    public QueueClient createQueue(String queueName) {
        return createQueueWithResponse(queueName, null, Context.NONE).value();
    }

    /**
     * Creates a queue in the storage account with the specified name and metadata and returns a QueueClient to
     * interact with it.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Create the queue "test" with metadata "queue:metadata"</p>
     *
     * {@codesnippet com.azure.storage.queue.queueServiceClient.createQueueWithResponse#string-map-Context}
     *
     * @param queueName Name of the queue
     * @param metadata Metadata to associate with the queue
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing the QueueClient and the status of creating the queue
     * @throws StorageException If a queue with the same name and different metadata already exists
     */
    public Response<QueueClient> createQueueWithResponse(String queueName, Map<String, String> metadata, Context context) {
        Response<QueueAsyncClient> response = client.createQueueWithResponse(queueName, metadata, context).block();

        return new SimpleResponse<>(response, new QueueClient(response.value()));
    }

    /**
     * Deletes a queue in the storage account
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Delete the queue "test"</p>
     *
     * {@codesnippet com.azure.storage.queue.queueServiceClient.deleteQueue#string}
     *
     * @param queueName Name of the queue
     * @throws StorageException If the queue doesn't exist
     */
    public void deleteQueue(String queueName) {
        deleteQueueWithResponse(queueName, Context.NONE);
    }

    /**
     * Deletes a queue in the storage account
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Delete the queue "test"</p>
     *
     * {@codesnippet com.azure.storage.queue.queueServiceClient.deleteQueueWithResponse#string-Context}
     *
     * @param queueName Name of the queue
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing the status of deleting the queue
     * @throws StorageException If the queue doesn't exist
     */
    public VoidResponse deleteQueueWithResponse(String queueName, Context context) {
        return client.deleteQueueWithResponse(queueName, context).block();
    }

    /**
     * Lists all queues in the storage account without their metadata.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>List all queues in the account</p>
     *
     * {@codesnippet com.azure.storage.queue.queueServiceClient.listQueues}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/list-queues1">Azure Docs</a>.</p>
     *
     * @return {@link QueueItem Queues} in the storage account
     */
    public Iterable<QueueItem> listQueues() {
        return listQueues(null, null);
    }

    /**
     * Lists the queues in the storage account that pass the filter.
     *
     * Pass true to {@link QueuesSegmentOptions#includeMetadata(boolean) includeMetadata} to have metadata returned for
     * the queues.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>List all queues that begin with "azure"</p>
     *
     * {@codesnippet com.azure.storage.queue.queueServiceClient.listQueues#queueSergmentOptions}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/list-queues1">Azure Docs</a>.</p>
     *
     * @param options Options for listing queues
     * @return {@link QueueItem Queues} in the storage account that satisfy the filter requirements
     */
    public Iterable<QueueItem> listQueues(QueuesSegmentOptions options) {
        return listQueues(null, options);
    }

    /**
     * Lists the queues in the storage account that pass the filter starting at the specified marker.
     *
     * Pass true to {@link QueuesSegmentOptions#includeMetadata(boolean) includeMetadata} to have metadata returned for
     * the queues.
     *
     * @param marker Starting point to list the queues
     * @param options Options for listing queues
     * @return {@link QueueItem Queues} in the storage account that satisfy the filter requirements
     */
    Iterable<QueueItem> listQueues(String marker, QueuesSegmentOptions options) {
        return client.listQueues(marker, options).toIterable();
    }

    /**
     * Retrieves the properties of the storage account's Queue service. The properties range from storage analytics and
     * metric to CORS (Cross-Origin Resource Sharing).
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Retrieve Queue service properties</p>
     *
     * {@codesnippet com.azure.storage.queue.queueServiceClient.getProperties}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-queue-service-properties">Azure Docs</a>.</p>
     *
     * @return Storage account Queue service properties
     */
    public StorageServiceProperties getProperties() {
        return getPropertiesWithResponse(Context.NONE).value();
    }

    /**
     * Retrieves the properties of the storage account's Queue service. The properties range from storage analytics and
     * metric to CORS (Cross-Origin Resource Sharing).
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Retrieve Queue service properties</p>
     *
     * {@codesnippet com.azure.storage.queue.queueServiceClient.getPropertiesWithResponse#Context}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-queue-service-properties">Azure Docs</a>.</p>
     *
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing the Storage account Queue service properties
     */
    public Response<StorageServiceProperties> getPropertiesWithResponse(Context context) {
        return client.getPropertiesWithResponse(context).block();
    }

    /**
     * Sets the properties for the storage account's Queue service. The properties range from storage analytics and
     * metric to CORS (Cross-Origin Resource Sharing).
     *
     * To maintain the CORS in the Queue service pass a {@code null} value for {@link StorageServiceProperties#cors() CORS}.
     * To disable all CORS in the Queue service pass an empty list for {@link StorageServiceProperties#cors() CORS}.
     *
     * <p><strong>Code Sample</strong></p>
     *
     * <p>Clear CORS in the Queue service</p>
     *
     * {@codesnippet com.azure.storage.queue.queueServiceClient.setProperties#storageServiceProperties}
     *
     * <p>Enable Minute and Hour Metrics</p>
     *
     * {@codesnippet com.azure.storage.queue.queueServiceClient.setPropertiesEnableMetrics#storageServiceProperties}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-queue-service-properties">Azure Docs</a>.</p>
     *
     * @param properties Storage account Queue service properties
     * @throws StorageException When one of the following is true
     * <ul>
     *     <li>A CORS rule is missing one of its fields</li>
     *     <li>More than five CORS rules will exist for the Queue service</li>
     *     <li>Size of all CORS rules exceeds 2KB</li>
     *     <li>
     *         Length of {@link CorsRule#allowedHeaders() allowed headers}, {@link CorsRule#exposedHeaders() exposed headers},
     *         or {@link CorsRule#allowedOrigins() allowed origins} exceeds 256 characters.
     *     </li>
     *     <li>{@link CorsRule#allowedMethods() Allowed methods} isn't DELETE, GET, HEAD, MERGE, POST, OPTIONS, or PUT</li>
     * </ul>
     */
    public void setProperties(StorageServiceProperties properties) {
        setPropertiesWithResponse(properties, Context.NONE);
    }

    /**
     * Sets the properties for the storage account's Queue service. The properties range from storage analytics and
     * metric to CORS (Cross-Origin Resource Sharing).
     *
     * To maintain the CORS in the Queue service pass a {@code null} value for {@link StorageServiceProperties#cors() CORS}.
     * To disable all CORS in the Queue service pass an empty list for {@link StorageServiceProperties#cors() CORS}.
     *
     * <p><strong>Code Sample</strong></p>
     *
     * <p>Clear CORS in the Queue service</p>
     *
     * {@codesnippet com.azure.storage.queue.queueServiceClient.setPropertiesWithResponse#storageServiceProperties-Context}
     *
     * <p>Enable Minute and Hour Metrics</p>
     *
     * {@codesnippet com.azure.storage.queue.queueServiceClient.setPropertiesWithResponseEnableMetrics#storageServiceProperties-Context}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-queue-service-properties">Azure Docs</a>.</p>
     *
     * @param properties Storage account Queue service properties
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response that only contains headers and response status code
     * @throws StorageException When one of the following is true
     * <ul>
     *     <li>A CORS rule is missing one of its fields</li>
     *     <li>More than five CORS rules will exist for the Queue service</li>
     *     <li>Size of all CORS rules exceeds 2KB</li>
     *     <li>
     *         Length of {@link CorsRule#allowedHeaders() allowed headers}, {@link CorsRule#exposedHeaders() exposed headers},
     *         or {@link CorsRule#allowedOrigins() allowed origins} exceeds 256 characters.
     *     </li>
     *     <li>{@link CorsRule#allowedMethods() Allowed methods} isn't DELETE, GET, HEAD, MERGE, POST, OPTIONS, or PUT</li>
     * </ul>
     */
    public VoidResponse setPropertiesWithResponse(StorageServiceProperties properties, Context context) {
        return client.setPropertiesWithResponse(properties, context).block();
    }

    /**
     * Retrieves the geo replication information about the Queue service.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Retrieve the geo replication information</p>
     *
     * {@codesnippet com.azure.storage.queue.queueServiceClient.getStatistics}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-queue-service-stats">Azure Docs</a>.</p>
     *
     * @return The geo replication information about the Queue service
     */
    public StorageServiceStats getStatistics() {
        return getStatisticsWithResponse(Context.NONE).value();
    }

    /**
     * Retrieves the geo replication information about the Queue service.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Retrieve the geo replication information</p>
     *
     * {@codesnippet com.azure.storage.queue.queueServiceClient.getStatisticsWithResponse#Context}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-queue-service-stats">Azure Docs</a>.</p>
     *
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing the geo replication information about the Queue service
     */
    public Response<StorageServiceStats> getStatisticsWithResponse(Context context) {
        return client.getStatisticsWithResponse(context).block();
    }

    /**
     * Generates an account SAS token with the specified parameters
     *
     * @param accountSASService The {@code AccountSASService} services for the account SAS
     * @param accountSASResourceType An optional {@code AccountSASResourceType} resources for the account SAS
     * @param accountSASPermission The {@code AccountSASPermission} permission for the account SAS
     * @param expiryTime The {@code OffsetDateTime} expiry time for the account SAS
     * @return A string that represents the SAS token
     */
    public String generateAccountSAS(AccountSASService accountSASService, AccountSASResourceType accountSASResourceType,
        AccountSASPermission accountSASPermission, OffsetDateTime expiryTime) {
        return this.client.generateAccountSAS(accountSASService, accountSASResourceType, accountSASPermission, expiryTime);
    }

    /**
     * Generates an account SAS token with the specified parameters
     *
     * <p><strong>Code Samples</strong></p>
     *
     *{@codesnippet com.azure.storage.queue.queueServiceClient.generateAccountSAS#AccountSASService-AccountSASResourceType-AccountSASPermission-OffsetDateTime-OffsetDateTime-String-IPRange-SASProtocol}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/create-account-sas">Azure Docs</a>.</p>
     *
     * @param accountSASService The {@code AccountSASService} services for the account SAS
     * @param accountSASResourceType An optional {@code AccountSASResourceType} resources for the account SAS
     * @param accountSASPermission The {@code AccountSASPermission} permission for the account SAS
     * @param expiryTime The {@code OffsetDateTime} expiry time for the account SAS
     * @param startTime The {@code OffsetDateTime} start time for the account SAS
     * @param version The {@code String} version for the account SAS
     * @param ipRange An optional {@code IPRange} ip address range for the SAS
     * @param sasProtocol An optional {@code SASProtocol} protocol for the SAS
     * @return A string that represents the SAS token
     */
    public String generateAccountSAS(AccountSASService accountSASService, AccountSASResourceType accountSASResourceType,
        AccountSASPermission accountSASPermission, OffsetDateTime expiryTime, OffsetDateTime startTime, String version,
        IPRange ipRange, SASProtocol sasProtocol) {
        return this.client.generateAccountSAS(accountSASService, accountSASResourceType, accountSASPermission, expiryTime, startTime, version, ipRange, sasProtocol);
    }
}
