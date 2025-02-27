// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.storage.file.models;

import com.azure.core.implementation.annotation.Fluent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * A permission (a security descriptor) at the share level.
 */
@JacksonXmlRootElement(localName = "SharePermission")
@Fluent
public final class SharePermission {
    /*
     * The permission in the Security Descriptor Definition Language (SDDL).
     */
    @JsonProperty(value = "permission", required = true)
    private String permission;

    /**
     * Get the permission property: The permission in the Security Descriptor
     * Definition Language (SDDL).
     *
     * @return the permission value.
     */
    public String permission() {
        return this.permission;
    }

    /**
     * Set the permission property: The permission in the Security Descriptor
     * Definition Language (SDDL).
     *
     * @param permission the permission value to set.
     * @return the SharePermission object itself.
     */
    public SharePermission permission(String permission) {
        this.permission = permission;
        return this;
    }
}
