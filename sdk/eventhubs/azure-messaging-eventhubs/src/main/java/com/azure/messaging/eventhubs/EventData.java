// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.eventhubs;

import com.azure.core.amqp.MessageConstant;
import com.azure.core.util.Context;
import com.azure.core.util.logging.ClientLogger;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.message.Message;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.azure.core.amqp.MessageConstant.ENQUEUED_TIME_UTC_ANNOTATION_NAME;
import static com.azure.core.amqp.MessageConstant.OFFSET_ANNOTATION_NAME;
import static com.azure.core.amqp.MessageConstant.PARTITION_KEY_ANNOTATION_NAME;
import static com.azure.core.amqp.MessageConstant.PUBLISHER_ANNOTATION_NAME;
import static com.azure.core.amqp.MessageConstant.SEQUENCE_NUMBER_ANNOTATION_NAME;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The data structure encapsulating the event being sent-to and received-from Event Hubs. Each Event Hub partition can
 * be visualized as a stream of {@link EventData}.
 *
 * <p>
 * Here's how AMQP message sections map to {@link EventData}. For reference, the specification can be found here:
 * <a href="http://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-complete-v1.0-os.pdf">AMQP 1.0 specification</a>
 *
 * <ol>
 * <li>{@link #properties()} - AMQPMessage.ApplicationProperties section</li>
 * <li>{@link #body()} - if AMQPMessage.Body has Data section</li>
 * </ol>
 *
 * <p>
 * Serializing a received {@link EventData} with AMQP sections other than ApplicationProperties (with primitive Java
 * types) and Data section is not supported.
 * </p>
 *
 * @see EventHubProducer
 * @see EventHubAsyncProducer
 */
public class EventData implements Comparable<EventData> {
    /*
     * These are properties owned by the service and set when a message is received.
     */
    public static final Set<String> RESERVED_SYSTEM_PROPERTIES;

    private final ClientLogger logger = new ClientLogger(EventData.class);
    private final Map<String, Object> properties;
    private final ByteBuffer body;
    private final SystemProperties systemProperties;
    private Context context;

    static {
        final Set<String> properties = new HashSet<>();
        properties.add(OFFSET_ANNOTATION_NAME.getValue());
        properties.add(PARTITION_KEY_ANNOTATION_NAME.getValue());
        properties.add(SEQUENCE_NUMBER_ANNOTATION_NAME.getValue());
        properties.add(ENQUEUED_TIME_UTC_ANNOTATION_NAME.getValue());
        properties.add(PUBLISHER_ANNOTATION_NAME.getValue());

        RESERVED_SYSTEM_PROPERTIES = Collections.unmodifiableSet(properties);
    }

    /**
     * Creates an event containing the {@code data}.
     *
     * @param body The data to set for this event.
     */
    public EventData(byte[] body) {
        this(body, Context.NONE);
    }

    /**
     * Creates an event containing the {@code data}.
     *
     * @param body The data to set for this event.
     * @param context A specified key-value pair of type {@link Context}.
     * @throws NullPointerException if {@code body} or if {@code context} is {@code null}.
     */
    public EventData(byte[] body, Context context) {
        this(ByteBuffer.wrap(body), context);
    }

    /**
     * Creates an event containing the {@code body}.
     *
     * @param body The data to set for this event.
     * @throws NullPointerException if {@code body} is {@code null}.
     */
    public EventData(ByteBuffer body) {
        this(body, Context.NONE);
    }

    /**
     * Creates an event containing the {@code body}.
     *
     * @param body The data to set for this event.
     * @param context A specified key-value pair of type {@link Context}.
     * @throws NullPointerException if {@code body} or if {@code context} is {@code null}.
     */
    public EventData(ByteBuffer body, Context context) {
        Objects.requireNonNull(body, "'body' cannot be null.");
        Objects.requireNonNull(body, "'context' cannot be null.");

        this.body = body;
        this.properties = new HashMap<>();
        this.systemProperties = new SystemProperties();
        this.context = context;
    }

    /**
     * Creates an event by encoding the {@code body} using UTF-8 charset.
     *
     * @param body The string that will be UTF-8 encoded to create an event.
     */
    public EventData(String body) {
        this(body.getBytes(UTF_8));
    }

    /*
     * Creates an event from a proton-j message
     *
     * @throws IllegalStateException if required the system properties, enqueued time, offset, or sequence number are
     *     not found in the message.
     * @throws NullPointerException if {@code message} is null.
     */
    EventData(Message message) {
        Objects.requireNonNull(message, "'message' cannot be null.");

        final Map<Symbol, Object> messageAnnotations = message.getMessageAnnotations().getValue();
        final HashMap<String, Object> receiveProperties = new HashMap<>();

        for (Map.Entry<Symbol, Object> annotation : messageAnnotations.entrySet()) {
            receiveProperties.put(annotation.getKey().toString(), annotation.getValue());
        }

        if (message.getProperties() != null) {
            addMapEntry(receiveProperties, MessageConstant.MESSAGE_ID, message.getMessageId());
            addMapEntry(receiveProperties, MessageConstant.USER_ID, message.getUserId());
            addMapEntry(receiveProperties, MessageConstant.TO, message.getAddress());
            addMapEntry(receiveProperties, MessageConstant.SUBJECT, message.getSubject());
            addMapEntry(receiveProperties, MessageConstant.REPLY_TO, message.getReplyTo());
            addMapEntry(receiveProperties, MessageConstant.CORRELATION_ID, message.getCorrelationId());
            addMapEntry(receiveProperties, MessageConstant.CONTENT_TYPE, message.getContentType());
            addMapEntry(receiveProperties, MessageConstant.CONTENT_ENCODING, message.getContentEncoding());
            addMapEntry(receiveProperties, MessageConstant.ABSOLUTE_EXPIRY_TIME, message.getExpiryTime());
            addMapEntry(receiveProperties, MessageConstant.CREATION_TIME, message.getCreationTime());
            addMapEntry(receiveProperties, MessageConstant.GROUP_ID, message.getGroupId());
            addMapEntry(receiveProperties, MessageConstant.GROUP_SEQUENCE, message.getGroupSequence());
            addMapEntry(receiveProperties, MessageConstant.REPLY_TO_GROUP_ID, message.getReplyToGroupId());
        }

        this.context = Context.NONE;
        this.systemProperties = new SystemProperties(receiveProperties);
        this.properties = message.getApplicationProperties() == null
            ? new HashMap<>()
            : message.getApplicationProperties().getValue();

        final Section bodySection = message.getBody();
        if (bodySection instanceof Data) {
            Data bodyData = (Data) bodySection;
            this.body = bodyData.getValue().asByteBuffer();
        } else {
            logger.warning(String.format(Locale.US,
                "Message body type is not of type Data, but type: %s. Not setting body contents.",
                bodySection != null ? bodySection.getType() : "null"));

            this.body = null;
        }

        message.clear();
    }

    /**
     * Adds a piece of metadata to the event, allowing publishers to offer additional information to event consumers. If
     * the {@code key} exists in the map, its existing value is overwritten.
     *
     * <p>
     * A common use case for {@link #properties()} is to associate serialization hints for the {@link #body()} as an aid
     * to consumers who wish to deserialize the binary data.
     * </p>
     *
     * <p>
     * <strong>Adding serialization hint using {@code addProperty(String, Object)}</strong>
     * </p>
     *
     * {@codesnippet com.azure.messaging.eventhubs.eventdata.addProperty#string-object}
     *
     * @param key The key for this application property
     * @param value The value for this application property.
     * @return The updated EventData object.
     * @throws NullPointerException if {@code key} or {@code value} is null.
     */
    public EventData addProperty(String key, Object value) {
        Objects.requireNonNull(key, "'key' cannot be null.");
        Objects.requireNonNull(value, "'value' cannot be null.");

        properties.put(key, value);
        return this;
    }

    /**
     * Adds a new key value pair to the existing context on Event Data.
     *
     * @param key The key for this context object
     * @param value The value for this context object.
     * @return The updated EventData object.
     * @throws NullPointerException if {@code key} or {@code value} is null.
     */
    public EventData addContext(String key, Object value) {
        Objects.requireNonNull(key, "The 'key' parameter cannot be null.");
        Objects.requireNonNull(value, "The 'value' parameter cannot be null.");
        this.context = context.addData(key, value);
        return this;
    }

    /**
     * The set of free-form event properties which may be used for passing metadata associated with the event with the
     * event body during Event Hubs operations.
     *
     * <p>
     * A common use case for {@code properties()} is to associate serialization hints for the {@link #body()} as an aid
     * to consumers who wish to deserialize the binary data. See {@link #addProperty(String, Object)} for a sample.
     * </p>
     *
     * @return Application properties associated with this {@link EventData}.
     */
    public Map<String, Object> properties() {
        return properties;
    }

    /**
     * A specified key-value pair of type {@link Context} to set additional information on the event.
     *
     * @return the {@link Context} object set on the event
     */
    public Context context() {
        return context;
    }

    /**
     * Properties that are populated by EventHubService. As these are populated by Service, they are only present on a
     * <b>received</b> EventData.
     *
     * @return an encapsulation of all SystemProperties appended by EventHubs service into EventData. {@code null} if
     *     the {@link EventData} is not received and is created by the public constructors.
     */
    public Map<String, Object> systemProperties() {
        return systemProperties;
    }

    /**
     * Gets the actual payload/data wrapped by EventData.
     *
     * <p>
     * If the means for deserializing the raw data is not apparent to consumers, a common technique is to make use of
     * {@link #properties()} when creating the event, to associate serialization hints as an aid to consumers who wish
     * to deserialize the binary data.
     * </p>
     *
     * @return ByteBuffer representing the data.
     */
    public ByteBuffer body() {
        return body.duplicate();
    }

    /**
     * Returns event data as UTF-8 decoded string.
     *
     * @return UTF-8 decoded string representation of the event data.
     */
    public String bodyAsString() {
        return UTF_8.decode(body).toString();
    }

    /**
     * Gets the offset of the event when it was received from the associated Event Hub partition.
     *
     * @return The offset within the Event Hub partition of the received event. {@code null} if the EventData was not
     *     received from Event Hub service.
     */
    public Long offset() {
        return systemProperties.offset();
    }

    /**
     * Gets a partition key used for message partitioning. If it exists, this value was used to compute a hash to select
     * a partition to send the message to.
     *
     * @return A partition key for this Event Data. {@code null} if the EventData was not received from Event Hub
     *     service or there was no partition key set when the event was sent to the Event Hub.
     */
    public String partitionKey() {
        return systemProperties.partitionKey();
    }

    /**
     * Gets the instant, in UTC, of when the event was enqueued in the Event Hub partition.
     *
     * @return The instant, in UTC, this was enqueued in the Event Hub partition. {@code null} if the EventData was not
     *     received from Event Hub service.
     */
    public Instant enqueuedTime() {
        return systemProperties.enqueuedTime();
    }

    /**
     * Gets the sequence number assigned to the event when it was enqueued in the associated Event Hub partition. This
     * is unique for every message received in the Event Hub partition.
     *
     * @return The sequence number for this event. {@code null} if the EventData was not received from Event Hub
     *     service.
     */
    public Long sequenceNumber() {
        return systemProperties.sequenceNumber();
    }

    private void addMapEntry(Map<String, Object> map, MessageConstant key, Object content) {
        if (content == null) {
            return;
        }

        map.put(key.getValue(), content);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(EventData other) {
        return Long.compare(
            this.sequenceNumber(),
            other.sequenceNumber()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EventData eventData = (EventData) o;
        return Objects.equals(body, eventData.body);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(body);
    }

    /**
     * A collection of properties populated by Azure Event Hubs service.
     */
    private static class SystemProperties extends HashMap<String, Object> {
        private static final long serialVersionUID = -2827050124966993723L;
        private final Long offset;
        private final String partitionKey;
        private final Instant enqueuedTime;
        private final Long sequenceNumber;

        SystemProperties() {
            super();
            offset = null;
            partitionKey = null;
            enqueuedTime = null;
            sequenceNumber = null;
        }

        SystemProperties(final Map<String, Object> map) {
            super(map);
            this.partitionKey = removeSystemProperty(PARTITION_KEY_ANNOTATION_NAME.getValue());

            final String offset = removeSystemProperty(OFFSET_ANNOTATION_NAME.getValue());
            if (offset == null) {
                throw new IllegalStateException(String.format(Locale.US,
                    "offset: %s should always be in map.", OFFSET_ANNOTATION_NAME.getValue()));
            }
            this.offset = Long.valueOf(offset);

            final Date enqueuedTimeValue = removeSystemProperty(ENQUEUED_TIME_UTC_ANNOTATION_NAME.getValue());
            if (enqueuedTimeValue == null) {
                throw new IllegalStateException(String.format(Locale.US,
                    "enqueuedTime: %s should always be in map.", ENQUEUED_TIME_UTC_ANNOTATION_NAME.getValue()));
            }
            this.enqueuedTime = enqueuedTimeValue.toInstant();

            final Long sequenceNumber = removeSystemProperty(SEQUENCE_NUMBER_ANNOTATION_NAME.getValue());
            if (sequenceNumber == null) {
                throw new IllegalStateException(String.format(Locale.US,
                    "sequenceNumber: %s should always be in map.", SEQUENCE_NUMBER_ANNOTATION_NAME.getValue()));
            }
            this.sequenceNumber = sequenceNumber;
        }

        /**
         * Gets the offset within the Event Hubs stream.
         *
         * @return The offset within the Event Hubs stream.
         */
        private Long offset() {
            return offset;
        }

        /**
         * Gets a partition key used for message partitioning. If it exists, this value was used to compute a hash to
         * select a partition to send the message to.
         *
         * @return A partition key for this Event Data.
         */
        private String partitionKey() {
            return partitionKey;
        }

        /**
         * Gets the time this event was enqueued in the Event Hub.
         *
         * @return The time this was enqueued in the service.
         */
        private Instant enqueuedTime() {
            return enqueuedTime;
        }

        /**
         * Gets the sequence number in the event stream for this event. This is unique for every message received in the
         * Event Hub.
         *
         * @return Sequence number for this event.
         * @throws IllegalStateException if {@link SystemProperties} does not contain the sequence number in a
         *     retrieved event.
         */
        private Long sequenceNumber() {
            return sequenceNumber;
        }

        @SuppressWarnings("unchecked")
        private <T> T removeSystemProperty(final String key) {
            if (this.containsKey(key)) {
                return (T) (this.remove(key));
            }

            return null;
        }
    }
}
