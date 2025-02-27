// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.storage.blob;

import com.azure.core.implementation.http.UrlBuilder;
import com.azure.storage.common.Constants;
import com.azure.storage.common.Utility;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A BlobURLParts object represents the components that make up an Azure Storage Container/Blob URL. You may parse an
 * existing URL into its parts with the {@link URLParser} class. You may construct a URL from parts by calling toURL().
 * It is also possible to use the empty constructor to buildClient a blobURL from scratch.
 * NOTE: Changing any SAS-related field requires computing a new SAS signature.
 *
 * @apiNote ## Sample Code \n
 * [!code-java[Sample_Code](../azure-storage-java/src/test/java/com/microsoft/azure/storage/Samples.java?name=url_parts "Sample code for BlobURLParts")] \n
 * For more samples, please see the [Samples file](%https://github.com/Azure/azure-storage-java/blob/master/src/test/java/com/microsoft/azure/storage/Samples.java)
 */
final class BlobURLParts {

    private String scheme;

    private String host;

    private String containerName;

    private String blobName;

    private String snapshot;

    private BlobServiceSASQueryParameters blobServiceSasQueryParameters;

    private Map<String, String[]> unparsedParameters;

    /**
     * Initializes a BlobURLParts object with all fields set to null, except unparsedParameters, which is an empty map.
     * This may be useful for constructing a URL to a blob storage resource from scratch when the constituent parts are
     * already known.
     */
    BlobURLParts() {
        unparsedParameters = new HashMap<>();
    }

    /**
     * The scheme. Ex: "https://".
     */
    public String scheme() {
        return scheme;
    }

    /**
     * The scheme. Ex: "https://".
     */
    public BlobURLParts scheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    /**
     * The host. Ex: "account.blob.core.windows.net".
     */
    public String host() {
        return host;
    }

    /**
     * The host. Ex: "account.blob.core.windows.net".
     */
    public BlobURLParts host(String host) {
        this.host = host;
        return this;
    }

    /**
     * The container name or {@code null} if a {@link BlobServiceAsyncClient} was parsed.
     */
    public String containerName() {
        return containerName;
    }

    /**
     * The container name or {@code null} if a {@link BlobServiceAsyncClient} was parsed.
     */
    public BlobURLParts containerName(String containerName) {
        this.containerName = containerName;
        return this;
    }

    /**
     * The blob name or {@code null} if a {@link BlobServiceAsyncClient} or {@link ContainerAsyncClient} was parsed.
     */
    public String blobName() {
        return blobName;
    }

    /**
     * The blob name or {@code null} if a {@link BlobServiceAsyncClient} or {@link ContainerAsyncClient} was parsed.
     */
    public BlobURLParts blobName(String blobName) {
        this.blobName = blobName;
        return this;
    }

    /**
     * The snapshot time or {@code null} if anything except a URL to a snapshot was parsed.
     */
    public String snapshot() {
        return snapshot;
    }

    /**
     * The snapshot time or {@code null} if anything except a URL to a snapshot was parsed.
     */
    public BlobURLParts snapshot(String snapshot) {
        this.snapshot = snapshot;
        return this;
    }

    /**
     * A {@link BlobServiceSASQueryParameters} representing the SAS query parameters or {@code null} if there were no such
     * parameters.
     */
    public BlobServiceSASQueryParameters sasQueryParameters() {
        return blobServiceSasQueryParameters;
    }

    /**
     * A {@link BlobServiceSASQueryParameters} representing the SAS query parameters or {@code null} if there were no such
     * parameters.
     */
    public BlobURLParts sasQueryParameters(BlobServiceSASQueryParameters blobServiceSasQueryParameters) {
        this.blobServiceSasQueryParameters = blobServiceSasQueryParameters;
        return this;
    }

    /**
     * The query parameter key value pairs aside from SAS parameters and snapshot time or {@code null} if there were
     * no such parameters.
     */
    public Map<String, String[]> unparsedParameters() {
        return unparsedParameters;
    }

    /**
     * The query parameter key value pairs aside from SAS parameters and snapshot time or {@code null} if there were
     * no such parameters.
     */
    public BlobURLParts unparsedParameters(Map<String, String[]> unparsedParameters) {
        this.unparsedParameters = unparsedParameters;
        return this;
    }

    /**
     * Converts the blob URL parts to a {@link URL}.
     *
     * @return A {@code java.net.URL} to the blob resource composed of all the elements in the object.
     *
     * @throws MalformedURLException The fields present on the BlobURLParts object were insufficient to construct a valid URL or were
     *         ill-formatted.
     */
    public URL toURL() throws MalformedURLException {
        UrlBuilder url = new UrlBuilder().scheme(this.scheme).host(this.host);

        StringBuilder path = new StringBuilder();
        if (this.containerName != null) {
            path.append(this.containerName);
            if (this.blobName != null) {
                path.append('/');
                path.append(this.blobName);
            }
        }
        url.path(path.toString());

        if (this.snapshot != null) {
            url.setQueryParameter(Constants.SNAPSHOT_QUERY_PARAMETER, this.snapshot);
        }
        if (this.blobServiceSasQueryParameters != null) {
            String encodedSAS = this.blobServiceSasQueryParameters.encode();
            if (encodedSAS.length() != 0) {
                url.query(encodedSAS);
            }
        }

        for (Map.Entry<String, String[]> entry : this.unparsedParameters.entrySet()) {
            // The commas are intentionally encoded.
            url.setQueryParameter(entry.getKey(),
                    Utility.urlEncode(String.join(",", entry.getValue())));
        }

        return url.toURL();
    }
}
