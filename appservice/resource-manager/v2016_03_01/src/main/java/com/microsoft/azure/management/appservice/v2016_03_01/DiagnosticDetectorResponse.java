/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.appservice.v2016_03_01;

import com.microsoft.azure.arm.model.HasInner;
import com.microsoft.azure.arm.resources.models.HasManager;
import com.microsoft.azure.management.appservice.v2016_03_01.implementation.WebManager;
import com.microsoft.azure.management.appservice.v2016_03_01.implementation.DiagnosticDetectorResponseInner;
import java.util.List;
import com.microsoft.azure.management.appservice.v2016_03_01.implementation.DetectorDefinitionInner;
import org.joda.time.DateTime;

/**
 * Type representing DiagnosticDetectorResponse.
 */
public interface DiagnosticDetectorResponse extends HasInner<DiagnosticDetectorResponseInner>, HasManager<WebManager> {
    /**
     * @return the abnormalTimePeriods value.
     */
    List<DetectorAbnormalTimePeriod> abnormalTimePeriods();

    /**
     * @return the data value.
     */
    List<List<NameValuePair>> data();

    /**
     * @return the detectorDefinition value.
     */
    DetectorDefinitionInner detectorDefinition();

    /**
     * @return the endTime value.
     */
    DateTime endTime();

    /**
     * @return the id value.
     */
    String id();

    /**
     * @return the issueDetected value.
     */
    Boolean issueDetected();

    /**
     * @return the kind value.
     */
    String kind();

    /**
     * @return the metrics value.
     */
    List<DiagnosticMetricSet> metrics();

    /**
     * @return the name value.
     */
    String name();

    /**
     * @return the responseMetaData value.
     */
    ResponseMetaData responseMetaData();

    /**
     * @return the startTime value.
     */
    DateTime startTime();

    /**
     * @return the type value.
     */
    String type();

}
