// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.storage.file.spock

import com.azure.storage.common.credentials.SharedKeyCredential
import com.azure.storage.file.ShareAsyncClient
import com.azure.storage.file.models.CorsRule
import com.azure.storage.file.models.FileServiceProperties
import com.azure.storage.file.models.ListSharesOptions
import com.azure.storage.file.models.Metrics
import com.azure.storage.file.models.RetentionPolicy
import com.azure.storage.file.models.ShareItem
import com.azure.storage.file.models.ShareProperties
import com.azure.storage.file.models.StorageErrorCode
import reactor.test.StepVerifier
import spock.lang.Unroll

class FileServiceAsyncAPITests extends APISpec {
    def shareName

    static def testMetadata = Collections.singletonMap("testmetadata", "value")
    static def reallyLongString = "thisisareallylongstringthatexceedsthe64characterlimitallowedoncertainproperties"
    static def TOO_MANY_RULES = new ArrayList<>()
    static def INVALID_ALLOWED_HEADER = Collections.singletonList(new CorsRule().allowedHeaders(reallyLongString))
    static def INVALID_EXPOSED_HEADER = Collections.singletonList(new CorsRule().exposedHeaders(reallyLongString))
    static def INVALID_ALLOWED_ORIGIN = Collections.singletonList(new CorsRule().allowedOrigins(reallyLongString))
    static def INVALID_ALLOWED_METHOD = Collections.singletonList(new CorsRule().allowedMethods("NOTAREALHTTPMETHOD"))

    def setup() {
        shareName = testResourceName.randomName(methodName, 60)
        primaryFileServiceAsyncClient = fileServiceBuilderHelper(interceptorManager).buildAsyncClient()
        for (int i = 0; i < 6; i++) {
            TOO_MANY_RULES.add(new CorsRule())
        }
    }

    def "Get file service URL"() {
        given:
        def accountName = SharedKeyCredential.fromConnectionString(connectionString).accountName()
        def expectURL = String.format("https://%s.file.core.windows.net", accountName)
        when:
        def fileServiceURL = primaryFileServiceAsyncClient.getFileServiceUrl().toString()
        then:
        expectURL.equals(fileServiceURL)
    }

    def "Get share does not create a share"() {
        when:
        def shareAsyncClient = primaryFileServiceAsyncClient.getShareAsyncClient(shareName)
        then:
        shareAsyncClient instanceof ShareAsyncClient
    }

    def "Create share"() {
        when:
        def createShareVerifier = StepVerifier.create(primaryFileServiceAsyncClient.createShareWithResponse(shareName, null, null))
        then:
        createShareVerifier.assertNext {
            assert FileTestHelper.assertResponseStatusCode(it, 201)
        }.verifyComplete()
    }

    @Unroll
    def "Create share with metadata"() {
        when:
        def createShareVerifier = StepVerifier.create(primaryFileServiceAsyncClient.createShareWithResponse(shareName, metadata, quota))
        then:
        createShareVerifier.assertNext {
            assert FileTestHelper.assertResponseStatusCode(it, 201)
        }
        where:
        metadata     | quota
        null         | null
        testMetadata | null
        null         | 1
        testMetadata | 1
    }

    @Unroll
    def "Create share with invalid args"() {
        when:
        def createShareVerifier = StepVerifier.create(primaryFileServiceAsyncClient.createShareWithResponse(shareName, metadata, quota))
        then:
        createShareVerifier.verifyErrorSatisfies {
            assert FileTestHelper.assertExceptionStatusCodeAndMessage(it, statusCode, errMsg)
        }
        where:
        metadata                                      | quota | statusCode | errMsg
        Collections.singletonMap("invalid#", "value") | 1     | 400        | StorageErrorCode.INVALID_METADATA
        testMetadata                                  | -1    | 400        | StorageErrorCode.INVALID_HEADER_VALUE
        testMetadata                                  | 0     | 400        | StorageErrorCode.INVALID_HEADER_VALUE
        testMetadata                                  | 5200  | 400        | StorageErrorCode.INVALID_HEADER_VALUE
    }

    def "Delete share"() {
        given:
        primaryFileServiceAsyncClient.createShare(shareName)
        when:
        def deleteShareVerifier = StepVerifier.create(primaryFileServiceAsyncClient.deleteShareWithResponse(shareName, null))
        then:
        deleteShareVerifier.assertNext {
            assert FileTestHelper.assertResponseStatusCode(it, 202)
        }
    }

    def "Delete share does not exist"() {
        when:
        def deleteShareVerifier = StepVerifier.create(primaryFileServiceAsyncClient.deleteShare(testResourceName.randomName(methodName, 60)))
        then:
        deleteShareVerifier.verifyErrorSatisfies {
            assert FileTestHelper.assertExceptionStatusCodeAndMessage(it, 404, StorageErrorCode.SHARE_NOT_FOUND)
        }
    }

    @Unroll
    def "List shares with filter"() {
        given:
        LinkedList<ShareItem> testShares = new LinkedList<>()
        for (int i = 0; i < 3; i++) {
            ShareItem share = new ShareItem().name(shareName + i).properties(new ShareProperties().quota(i + 1))
            if (i == 2) {
                share.metadata(testMetadata)
            }
            testShares.add(share)
            primaryFileServiceAsyncClient.createShareWithResponse(share.name(), share.metadata(), share.properties().quota()).block()
        }

        when:
        def sharesVerifier = StepVerifier.create(primaryFileServiceAsyncClient.listShares(options))

        then:
        sharesVerifier.thenConsumeWhile {
            FileTestHelper.assertSharesAreEqual(testShares.pop(), it, includeMetadata, includeSnapshot)
        }.verifyComplete()

        for (int i = 0; i < 3 - limits; i++) {
            testShares.pop()
        }

        testShares.isEmpty()

        where:
        options                                                                                               | limits | includeMetadata | includeSnapshot
        new ListSharesOptions().prefix("fileserviceasyncapitestslistshareswithfilter")                        | 3      | false           | true
        new ListSharesOptions().prefix("fileserviceasyncapitestslistshareswithfilter").includeMetadata(true)  | 3      | true            | true
        new ListSharesOptions().prefix("fileserviceasyncapitestslistshareswithfilter").includeMetadata(false) | 3      | false           | true
        new ListSharesOptions().prefix("fileserviceasyncapitestslistshareswithfilter").maxResults(2)          | 3      | false           | true
    }

    @Unroll
    def "List shares with args"() {
        given:
        LinkedList<ShareItem> testShares = new LinkedList<>()
        for (int i = 0; i < 3; i++) {
            ShareItem share = new ShareItem().name(shareName + i).properties(new ShareProperties().quota(2))
                .metadata(testMetadata)
            def shareAsyncClient = primaryFileServiceAsyncClient.getShareAsyncClient(share.name())
            shareAsyncClient.createWithResponse(share.metadata(), share.properties().quota()).block()
            if (i == 2) {
                StepVerifier.create(shareAsyncClient.createSnapshotWithResponse(null))
                    .assertNext {
                        testShares.add(new ShareItem().name(share.name()).metadata(share.metadata()).properties(share.properties()).snapshot(it.value().snapshot()))
                        FileTestHelper.assertResponseStatusCode(it, 201)
                    }.verifyComplete()
            }
            testShares.add(share)
        }
        when:
        def sharesVerifier = StepVerifier.create(primaryFileServiceAsyncClient.listShares(options))
        then:
        sharesVerifier.assertNext {
            assert FileTestHelper.assertSharesAreEqual(testShares.pop(), it, includeMetadata, includeSnapshot)
        }.expectNextCount(limits - 1).verifyComplete()

        where:
        options                                                                                                                   | limits | includeMetadata | includeSnapshot
        new ListSharesOptions().prefix("fileserviceasyncapitestslistshareswithargs")                                              | 3      | false           | false
        new ListSharesOptions().prefix("fileserviceasyncapitestslistshareswithargs").includeMetadata(true)                        | 3      | true            | false
        new ListSharesOptions().prefix("fileserviceasyncapitestslistshareswithargs").includeMetadata(true).includeSnapshots(true) | 4      | true            | true
    }

    def "Set and get properties"() {
        given:
        def originalProperties = primaryFileServiceAsyncClient.getProperties().block()
        def retentionPolicy = new RetentionPolicy().enabled(true).days(3)
        def metrics = new Metrics().enabled(true).includeAPIs(false)
            .retentionPolicy(retentionPolicy).version("1.0")
        def updatedProperties = new FileServiceProperties().hourMetrics(metrics)
            .minuteMetrics(metrics).cors(new ArrayList<>())
        when:
        def getPropertiesBeforeVerifier = StepVerifier.create(primaryFileServiceAsyncClient.getPropertiesWithResponse())
        def setPropertiesVerifier = StepVerifier.create(primaryFileServiceAsyncClient.setPropertiesWithResponse(updatedProperties))
        def getPropertiesAfterVerifier = StepVerifier.create(primaryFileServiceAsyncClient.getPropertiesWithResponse())
        then:
        getPropertiesBeforeVerifier.assertNext {
            assert FileTestHelper.assertResponseStatusCode(it, 200)
            assert FileTestHelper.assertFileServicePropertiesAreEqual(originalProperties, it.value())
        }.verifyComplete()
        setPropertiesVerifier.assertNext {
            assert FileTestHelper.assertResponseStatusCode(it, 202)
        }

        getPropertiesAfterVerifier.assertNext {
            assert FileTestHelper.assertResponseStatusCode(it, 200)
            assert FileTestHelper.assertFileServicePropertiesAreEqual(originalProperties, it.value())
        }.verifyComplete()
    }

    @Unroll
    def "Set and get properties with invalid args"() {
        given:
        def retentionPolicy = new RetentionPolicy().enabled(true).days(3)
        def metrics = new Metrics().enabled(true).includeAPIs(false)
            .retentionPolicy(retentionPolicy).version("1.0")

        when:
        def updatedProperties = new FileServiceProperties().hourMetrics(metrics)
            .minuteMetrics(metrics).cors(coreList)
        def setPropertyVerifier = StepVerifier.create(primaryFileServiceAsyncClient.setProperties(updatedProperties))
        then:
        setPropertyVerifier.verifyErrorSatisfies {
            assert FileTestHelper.assertExceptionStatusCodeAndMessage(it, statusCode, errMsg)
        }
        where:
        coreList               | statusCode | errMsg
        TOO_MANY_RULES         | 400        | StorageErrorCode.INVALID_XML_DOCUMENT
        INVALID_ALLOWED_HEADER | 400        | StorageErrorCode.INVALID_XML_DOCUMENT
        INVALID_EXPOSED_HEADER | 400        | StorageErrorCode.INVALID_XML_DOCUMENT
        INVALID_ALLOWED_ORIGIN | 400        | StorageErrorCode.INVALID_XML_DOCUMENT
        INVALID_ALLOWED_METHOD | 400        | StorageErrorCode.INVALID_XML_NODE_VALUE

    }
}
