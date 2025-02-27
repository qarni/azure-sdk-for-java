// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.storage.file.models;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.rest.ResponseBase;
import java.util.Map;

/**
 * Contains all response data for the delete operation.
 */
public final class FilesDeleteResponse extends ResponseBase<FileDeleteHeaders, Void> {
    /**
     * Creates an instance of FilesDeleteResponse.
     *
     * @param request the request which resulted in this FilesDeleteResponse.
     * @param statusCode the status code of the HTTP response.
     * @param rawHeaders the raw headers of the HTTP response.
     * @param value the deserialized value of the HTTP response.
     * @param headers the deserialized headers of the HTTP response.
     */
    public FilesDeleteResponse(HttpRequest request, int statusCode, HttpHeaders rawHeaders, Void value, FileDeleteHeaders headers) {
        super(request, statusCode, rawHeaders, value, headers);
    }
}
