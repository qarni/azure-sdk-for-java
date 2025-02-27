// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.storage.queue.spock

import com.azure.storage.queue.QueueAsyncClient
import com.azure.storage.queue.models.AccessPolicy
import com.azure.storage.queue.models.SignedIdentifier
import com.azure.storage.queue.models.StorageErrorCode
import reactor.test.StepVerifier
import spock.lang.Ignore
import spock.lang.Unroll

import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class QueueAysncAPITests extends APISpec {
    QueueAsyncClient queueAsyncClient

    static def testMetadata = Collections.singletonMap("metadata", "value")
    static def createMetadata = Collections.singletonMap("metadata1", "value")

    def setup() {
        primaryQueueServiceAsyncClient = queueServiceBuilderHelper(interceptorManager).buildAsyncClient()
        queueAsyncClient = primaryQueueServiceAsyncClient.getQueueAsyncClient(testResourceName.randomName(methodName, 60))
    }

    def "Create queue with shared key"() {
        expect:
        StepVerifier.create(queueAsyncClient.createWithResponse(null)).assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 201) }
            .verifyComplete()
    }

    // TODO: Will implement the test after introduce the sas token generator
    @Ignore
    def "Create queue with sas token"() {

    }

    def "Delete exist queue"() {
        given:
        queueAsyncClient.createWithResponse(null).block()
        when:
        def deleteQueueVerifier = StepVerifier.create(queueAsyncClient.deleteWithResponse())
        then:
        deleteQueueVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 204) }
            .verifyComplete()
    }

    def "Delete queue error"() {
        when:
        def deleteQueueVerifier = StepVerifier.create(queueAsyncClient.deleteWithResponse())
        then:
        deleteQueueVerifier.verifyErrorSatisfies {
            assert QueueTestHelper.assertExceptionStatusCodeAndMessage(it, 404, StorageErrorCode.QUEUE_NOT_FOUND)
        }
    }

    def "Get properties"() {
        given:
        queueAsyncClient.createWithResponse(testMetadata).block()
        when:
        def getPropertiesVerifier = StepVerifier.create(queueAsyncClient.getPropertiesWithResponse())
        then:
        getPropertiesVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 200)
            assert it.value().approximateMessagesCount() == 0
            assert testMetadata.equals(it.value().metadata())
        }.verifyComplete()
    }

    def "Get properties error"() {
        when:
        def getPropertiesVerifier = StepVerifier.create(queueAsyncClient.getProperties())
        then:
        getPropertiesVerifier.verifyErrorSatisfies {
            assert QueueTestHelper.assertExceptionStatusCodeAndMessage(it, 404, StorageErrorCode.QUEUE_NOT_FOUND)
        }
    }

    @Unroll
    def "Set and clear metadata"() {
        given:
        queueAsyncClient.createWithResponse(matadataInCreate).block()
        when:
        def getPropertiesVerifierBefore = StepVerifier.create(queueAsyncClient.getPropertiesWithResponse())
        def setMetadataVerifier = StepVerifier.create(queueAsyncClient.setMetadataWithResponse(metadataInSet))
        def getPropertiesVerifierAfter = StepVerifier.create(queueAsyncClient.getPropertiesWithResponse())
        then:
        getPropertiesVerifierBefore.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 200)
            assert expectMetadataInCreate.equals(it.value().metadata())
        }.verifyComplete()
        setMetadataVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 204) }
            .verifyComplete()
        getPropertiesVerifierAfter.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 200)
            assert expectMetadataInSet.equals(it.value().metadata)
        }.verifyComplete()
        where:
        matadataInCreate | metadataInSet | expectMetadataInCreate | expectMetadataInSet
        null             | testMetadata  | Collections.emptyMap() | testMetadata
        createMetadata   | testMetadata  | createMetadata         | testMetadata
        createMetadata   | null          | createMetadata         | Collections.emptyMap()
        testMetadata     | testMetadata  | testMetadata           | testMetadata
        null             | null          | Collections.emptyMap() | Collections.emptyMap()
    }

    def "Set metadata queue error"() {
        when:
        def setMetadataVerifier = StepVerifier.create(queueAsyncClient.setMetadataWithResponse(testMetadata))
        then:
        setMetadataVerifier.verifyErrorSatisfies {
            assert QueueTestHelper.assertExceptionStatusCodeAndMessage(it, 404, StorageErrorCode.QUEUE_NOT_FOUND)
        }
    }

    @Unroll
    def "Set invalid meta"() {
        given:
        def invalidMetadata = Collections.singletonMap(invalidKey, "value")
        queueAsyncClient.create().block()
        when:
        def setMetadataVerifier = StepVerifier.create(queueAsyncClient.setMetadataWithResponse(invalidMetadata))
        then:
        setMetadataVerifier.verifyErrorSatisfies {
            assert QueueTestHelper.assertExceptionStatusCodeAndMessage(it, statusCode, errMessage)
        }
        where:
        invalidKey     | statusCode | errMessage
        "invalidMeta"  | 403        | StorageErrorCode.AUTHENTICATION_FAILED
        "invalid-meta" | 400        | StorageErrorCode.INVALID_METADATA
        "12345"        | 400        | StorageErrorCode.INVALID_METADATA
        ""             | 400        | StorageErrorCode.EMPTY_METADATA_KEY
    }

    def "Get access policy"() {
        given:
        queueAsyncClient.create().block()
        when:
        def getAccessPolicyVerifier = StepVerifier.create(queueAsyncClient.getAccessPolicy())
        then:
        getAccessPolicyVerifier.verifyComplete()
    }

    def "Get access policy does error"() {
        when:
        def getAccessPolicyVerifier = StepVerifier.create(queueAsyncClient.getAccessPolicy())
        then:
        getAccessPolicyVerifier.verifyErrorSatisfies {
            assert QueueTestHelper.assertExceptionStatusCodeAndMessage(it, 404, StorageErrorCode.QUEUE_NOT_FOUND)
        }
    }

    def "Set access policy"() {
        given:
        queueAsyncClient.create().block()
        def accessPolicy = new AccessPolicy()
            .permission("raup")
            .start(OffsetDateTime.of(LocalDateTime.of(2000, 1, 1, 0, 0), ZoneOffset.UTC))
            .expiry(OffsetDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0), ZoneOffset.UTC))
        def permission = new SignedIdentifier()
            .id("testpermission")
            .accessPolicy(accessPolicy)
        when:
        def setAccessPolicyVerifier = StepVerifier.create(queueAsyncClient.setAccessPolicyWithResponse(Collections.singletonList(permission)))
        def getAccessPolicyVerifier = StepVerifier.create(queueAsyncClient.getAccessPolicy())
        then:
        setAccessPolicyVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 204)
        }.verifyComplete()
        getAccessPolicyVerifier.assertNext {
            assert QueueTestHelper.assertPermissionsAreEqual(permission, it)
        }.verifyComplete()
    }

    def "Set invalid access policy"() {
        given:
        def accessPolicy = new AccessPolicy()
            .permission("r")
            .start(OffsetDateTime.of(LocalDateTime.of(2000, 1, 1, 0, 0), ZoneOffset.UTC))
            .expiry(OffsetDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0), ZoneOffset.UTC))

        def permission = new SignedIdentifier()
            .id("theidofthispermissionislongerthanwhatisallowedbytheserviceandshouldfail")
            .accessPolicy(accessPolicy)
        queueAsyncClient.create().block()
        when:
        def setAccessPolicyVerifier = StepVerifier.create(queueAsyncClient.setAccessPolicyWithResponse(Collections.singletonList(permission)))
        then:
        setAccessPolicyVerifier.verifyErrorSatisfies {
            assert QueueTestHelper.assertExceptionStatusCodeAndMessage(it, 400, StorageErrorCode.INVALID_XML_DOCUMENT)
        }
    }

    def "Set multiple access policies"() {
        given:
        def accessPolicy = new AccessPolicy()
            .permission("r")
            .start(OffsetDateTime.of(LocalDateTime.of(2000, 1, 1, 0, 0), ZoneOffset.UTC))
            .expiry(OffsetDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0), ZoneOffset.UTC))

        def permissions = new ArrayList<>()
        for (int i = 0; i < 3; i++) {
            permissions.add(new SignedIdentifier()
                .id("policy" + i)
                .accessPolicy(accessPolicy))
        }
        queueAsyncClient.create().block()
        when:
        def setAccessPolicyVerifier = StepVerifier.create(queueAsyncClient.setAccessPolicyWithResponse(permissions))
        def getAccessPolicyVerifier = StepVerifier.create(queueAsyncClient.getAccessPolicy())
        then:
        setAccessPolicyVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 204)
        }.verifyComplete()
        getAccessPolicyVerifier.assertNext {
            assert QueueTestHelper.assertPermissionsAreEqual(permissions[0], it)
        }.assertNext {
            assert QueueTestHelper.assertPermissionsAreEqual(permissions[1], it)
        }.assertNext {
            assert QueueTestHelper.assertPermissionsAreEqual(permissions[2], it)
        }.verifyComplete()
    }

    def "Set too many access policies"() {
        given:
        def accessPolicy = new AccessPolicy()
            .permission("r")
            .start(OffsetDateTime.of(LocalDateTime.of(2000, 1, 1, 0, 0), ZoneOffset.UTC))
            .expiry(OffsetDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0), ZoneOffset.UTC))

        def permissions = new ArrayList<>()
        for (int i = 0; i < 6; i++) {
            permissions.add(new SignedIdentifier()
                .id("policy" + i)
                .accessPolicy(accessPolicy))
        }
        queueAsyncClient.create().block()
        when:
        def setAccessPolicyVerifier = StepVerifier.create(queueAsyncClient.setAccessPolicyWithResponse(permissions))
        then:
        setAccessPolicyVerifier.verifyErrorSatisfies {
            assert QueueTestHelper.assertExceptionStatusCodeAndMessage(it, 400, StorageErrorCode.INVALID_XML_DOCUMENT)
        }
    }

    def "Enqueue message"() {
        given:
        queueAsyncClient.create().block()
        def expectMsg = "test message"
        when:
        def enqueueMsgVerifier = StepVerifier.create(queueAsyncClient.enqueueMessageWithResponse(expectMsg, null, null))
        def peekMsgVerifier = StepVerifier.create(queueAsyncClient.peekMessages())
        then:
        enqueueMsgVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 201)
        }.verifyComplete()
        peekMsgVerifier.assertNext {
            assert expectMsg.equals(it.messageText())
            assert !it.hasNext()
        }
    }

    def "Enqueue empty message"() {
        given:
        queueAsyncClient.create().block()
        when:
        def enqueueMsgVerifier = StepVerifier.create(queueAsyncClient.enqueueMessageWithResponse("", null, null))
        def peekMsgVerifier = StepVerifier.create(queueAsyncClient.peekMessages())
        then:
        enqueueMsgVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 201)
        }.verifyComplete()
        peekMsgVerifier.assertNext {
            assert it.messageText() == null
            assert !it.hasNext()
        }
    }

    def "Enqueue time to live"() {
        given:
        queueAsyncClient.create().block()
        when:
        def enqueueMsgVerifier = StepVerifier.create(queueAsyncClient.enqueueMessageWithResponse("test message",
            Duration.ofSeconds(0), Duration.ofSeconds(2)))
        then:
        enqueueMsgVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 201)
        }.verifyComplete()
    }

    def "Dequeue message"() {
        given:
        queueAsyncClient.create().block()
        def expectMsg = "test message"
        queueAsyncClient.enqueueMessage(expectMsg).block()
        when:
        def dequeueMsgVerifier = StepVerifier.create(queueAsyncClient.dequeueMessages())
        then:
        dequeueMsgVerifier.assertNext {
            assert expectMsg.equals(it.messageText())
        }.verifyComplete()
    }

    def "Dequeue multiple messages"() {
        given:
        queueAsyncClient.create().block()
        def expectMsg1 = "test message 1"
        def expectMsg2 = "test message 2"
        queueAsyncClient.enqueueMessage(expectMsg1).block()
        queueAsyncClient.enqueueMessage(expectMsg2).block()
        when:
        def dequeueMsgVerifier = StepVerifier.create(queueAsyncClient.dequeueMessages(2))
        then:
        dequeueMsgVerifier.assertNext {
            assert expectMsg1.equals(it.messageText())
        }.assertNext {
            assert expectMsg2.equals(it.messageText())
        }.verifyComplete()
    }

    def "Dequeue too many message"() {
        given:
        queueAsyncClient.create().block()
        when:
        def dequeueMsgVerifier = StepVerifier.create(queueAsyncClient.dequeueMessages(33))
        then:
        dequeueMsgVerifier.verifyErrorSatisfies {
            assert QueueTestHelper.assertExceptionStatusCodeAndMessage(it, 400, StorageErrorCode.OUT_OF_RANGE_QUERY_PARAMETER_VALUE)
        }
    }

    def "Peek message"() {
        given:
        queueAsyncClient.create().block()
        def expectMsg = "test message"
        queueAsyncClient.enqueueMessage(expectMsg).block()
        when:
        def peekMsgVerifier = StepVerifier.create(queueAsyncClient.peekMessages())
        then:
        peekMsgVerifier.assertNext {
            assert expectMsg.equals(it.messageText())
        }.verifyComplete()
    }

    def "Peek multiple messages"() {
        given:
        queueAsyncClient.create().block()
        def expectMsg1 = "test message 1"
        def expectMsg2 = "test message 2"
        queueAsyncClient.enqueueMessage(expectMsg1).block()
        queueAsyncClient.enqueueMessage(expectMsg2).block()
        when:
        def peekMsgVerifier = StepVerifier.create(queueAsyncClient.peekMessages(2))
        then:
        peekMsgVerifier.assertNext {
            assert expectMsg1.equals(it.messageText())
        }.assertNext {
            assert expectMsg2.equals(it.messageText())
        }.verifyComplete()
    }

    def "Peek too many message"() {
        given:
        queueAsyncClient.create().block()
        when:
        def peekMsgVerifier = StepVerifier.create(queueAsyncClient.peekMessages(33))
        then:
        peekMsgVerifier.verifyErrorSatisfies {
            assert QueueTestHelper.assertExceptionStatusCodeAndMessage(it, 400, StorageErrorCode.OUT_OF_RANGE_QUERY_PARAMETER_VALUE)
        }
    }

    def "Peek messages error"() {
        when:
        def peekMsgVerifier = StepVerifier.create(queueAsyncClient.peekMessages())
        then:
        peekMsgVerifier.verifyErrorSatisfies {
            assert QueueTestHelper.assertExceptionStatusCodeAndMessage(it, 404, StorageErrorCode.QUEUE_NOT_FOUND)
        }
    }

    def "Clear messages"() {
        given:
        queueAsyncClient.create().block()
        queueAsyncClient.enqueueMessage("test message 1").block()
        queueAsyncClient.enqueueMessage("test message 2").block()
        queueAsyncClient.enqueueMessage("test message 3").block()
        when:
        def getPropertiesVerifier = StepVerifier.create(queueAsyncClient.getPropertiesWithResponse())
        def clearMsgVerifier = StepVerifier.create(queueAsyncClient.clearMessagesWithResponse())
        def getPropertiesAfterVerifier = StepVerifier.create(queueAsyncClient.getPropertiesWithResponse())
        then:
        getPropertiesVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 200)
            assert it.value().approximateMessagesCount() == 3
        }.verifyComplete()
        clearMsgVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 204)
        }.verifyComplete()
        getPropertiesAfterVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 200)
            assert it.value().approximateMessagesCount() == 0
        }.verifyComplete()
    }

    def "Clear messages error"() {
        when:
        def clearMsgVerifier = StepVerifier.create(queueAsyncClient.clearMessagesWithResponse())
        then:
        clearMsgVerifier.verifyErrorSatisfies {
            assert QueueTestHelper.assertExceptionStatusCodeAndMessage(it, 404, StorageErrorCode.QUEUE_NOT_FOUND)
        }
    }

    def "Delete message"() {
        given:
        queueAsyncClient.create().block()
        queueAsyncClient.enqueueMessage("test message 1").block()
        queueAsyncClient.enqueueMessage("test message 2").block()
        queueAsyncClient.enqueueMessage("test message 3").block()
        def dequeueMsg = queueAsyncClient.dequeueMessages().blockFirst()
        when:
        def getPropertiesVerifier = StepVerifier.create(queueAsyncClient.getPropertiesWithResponse())
        def deleteMsgVerifier = StepVerifier.create(queueAsyncClient.deleteMessageWithResponse(dequeueMsg.messageId(), dequeueMsg.popReceipt()))
        def getPropertiesAfterVerifier = StepVerifier.create(queueAsyncClient.getPropertiesWithResponse())
        then:
        getPropertiesVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 200)
            assert it.value().approximateMessagesCount() == 3

        }.verifyComplete()
        deleteMsgVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 204)
        }.verifyComplete()
        getPropertiesAfterVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 200)
            assert it.value().approximateMessagesCount() == 2
        }.verifyComplete()
    }

    @Unroll
    def "Delete message invalid args"() {
        given:
        queueAsyncClient.create().block()
        def expectMsg = "test message"
        queueAsyncClient.enqueueMessage(expectMsg).block()
        def dequeueMessage = queueAsyncClient.dequeueMessages().blockFirst()
        when:
        def deleteMessageId = messageId ? dequeueMessage.messageId() : dequeueMessage.messageId() + "Random"
        def deletePopReceipt = popReceipt ? dequeueMessage.popReceipt() : dequeueMessage.popReceipt() + "Random"
        def deleteMsgVerifier = StepVerifier.create(queueAsyncClient.deleteMessageWithResponse(deleteMessageId, deletePopReceipt))
        then:
        deleteMsgVerifier.verifyErrorSatisfies {
            assert QueueTestHelper.assertExceptionStatusCodeAndMessage(it, statusCode, errMsg)
        }
        where:
        messageId | popReceipt | statusCode | errMsg
        true      | false      | 400        | StorageErrorCode.INVALID_QUERY_PARAMETER_VALUE
        false     | true       | 404        | StorageErrorCode.MESSAGE_NOT_FOUND
        false     | false      | 400        | StorageErrorCode.INVALID_QUERY_PARAMETER_VALUE
    }

    def "Update message"() {
        given:
        def updateMsg = "Updated test message"
        queueAsyncClient.create().block()
        queueAsyncClient.enqueueMessage("test message before update").block()

        def dequeueMsg = queueAsyncClient.dequeueMessages().blockFirst()
        when:
        def updateMsgVerifier = StepVerifier.create(queueAsyncClient.updateMessageWithResponse(updateMsg,
            dequeueMsg.messageId(), dequeueMsg.popReceipt(), Duration.ofSeconds(1)))
        def peekMsgVerifier = StepVerifier.create(queueAsyncClient.peekMessages().delaySubscription(Duration.ofSeconds(2)))
        then:
        updateMsgVerifier.assertNext {
            assert QueueTestHelper.assertResponseStatusCode(it, 204)
        }.verifyComplete()
        peekMsgVerifier.assertNext {
            assert updateMsg.equals(it.messageText())
        }.verifyComplete()
    }

    @Unroll
    def "Update message invalid args"() {
        given:
        queueAsyncClient.create().block()
        def updateMsg = "Updated test message"
        queueAsyncClient.enqueueMessage("test message before update").block()
        def dequeueMessage = queueAsyncClient.dequeueMessages().blockFirst()
        when:
        def updateMessageId = messageId ? dequeueMessage.messageId() : dequeueMessage.messageId() + "Random"
        def updatePopReceipt = popReceipt ? dequeueMessage.popReceipt() : dequeueMessage.popReceipt() + "Random"
        def updateMsgVerifier = StepVerifier.create(queueAsyncClient.updateMessageWithResponse(updateMsg, updateMessageId, updatePopReceipt, Duration.ofSeconds(1)))
        then:
        updateMsgVerifier.verifyErrorSatisfies {
            assert QueueTestHelper.assertExceptionStatusCodeAndMessage(it, statusCode, errMsg)
        }
        where:
        messageId | popReceipt | statusCode | errMsg
        true      | false      | 400        | StorageErrorCode.INVALID_QUERY_PARAMETER_VALUE
        false     | true       | 404        | StorageErrorCode.MESSAGE_NOT_FOUND
        false     | false      | 400        | StorageErrorCode.INVALID_QUERY_PARAMETER_VALUE
    }

}
