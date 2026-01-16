/*
 * Copyright 2018-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.sparkplug.aware.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extensions.sparkplug.aware.topics.MessageType;
import com.hivemq.extensions.sparkplug.aware.topics.TopicStructure;
import org.eclipse.tahu.message.SparkplugBPayloadDecoder;
import org.eclipse.tahu.message.SparkplugBPayloadEncoder;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.eclipse.tahu.util.CompressionAlgorithm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Utility class for handling Sparkplug B payload operations.
 * <p>
 * This class provides functionality for:
 * <ul>
 *     <li>Parsing and decoding Sparkplug B payloads from byte buffers</li>
 *     <li>Modifying timestamps in Sparkplug messages (e.g., for NDEATH messages)</li>
 *     <li>Converting Sparkplug payloads to JSON format for logging and debugging</li>
 *     <li>Extracting individual metrics from Sparkplug payloads</li>
 *     <li>Supporting optional GZIP compression for payload encoding</li>
 * </ul>
 * <p>
 * This is a utility class with private constructor to prevent instantiation.
 *
 * @author David Sondermann
 */
public final class PayloadUtil {

    private static final @NotNull CompressionAlgorithm COMPRESSION_ALGORITHM = CompressionAlgorithm.GZIP;

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(PayloadUtil.class);
    private static final @NotNull Logger JSON_LOG = LoggerFactory.getLogger("com.hivemq.extensions.sparkplug.jsonLog");

    PayloadUtil() {
    }

    /**
     * Modifies the timestamp of a Sparkplug B payload to the current time.
     * <p>
     * This method decodes a Sparkplug B payload from the provided byte buffer, creates a new
     * payload with an updated timestamp (current time), and re-encodes it. The method is
     * typically used to update NDEATH message timestamps from their original Last Will and
     * Testament (LWT) creation time to the actual disconnection time.
     * <p>
     * All other payload properties (metrics, sequence number, UUID, body) are preserved unchanged.
     *
     * @param useCompression whether to apply GZIP compression to the re-encoded payload
     * @param byteBuffer     the byte buffer containing the original Sparkplug B payload
     * @return a new byte buffer containing the payload with updated timestamp
     * @throws Exception                if the payload cannot be decoded or encoded
     * @throws IllegalArgumentException if the byte buffer does not contain a valid Sparkplug B payload
     */
    public static ByteBuffer modifySparkplugTimestamp(boolean useCompression, ByteBuffer byteBuffer) throws Exception {
        final var inboundPayload = getSparkplugBPayload(byteBuffer);
        if (inboundPayload == null) {
            throw new IllegalArgumentException("Unable to get Sparkplug B Payload from byte buffer.");
        }
        // create the same payload with a new timestamp.
        final var payload = new SparkplugBPayload(new Date(),
                inboundPayload.getMetrics(),
                inboundPayload.getSeq(),
                inboundPayload.getUuid(),
                inboundPayload.getBody());

        final var encoder = new SparkplugBPayloadEncoder();
        byte[] bytes;
        // compress payload (optional)
        if (useCompression) {
            bytes = encoder.getBytes(org.eclipse.tahu.util.PayloadUtil.compress(payload, COMPRESSION_ALGORITHM, false),
                    false);
        } else {
            bytes = encoder.getBytes(payload, false);
        }
        return ByteBuffer.wrap(bytes);
    }

    /**
     * Logs a Sparkplug payload in formatted JSON for debugging and monitoring purposes.
     * <p>
     * This method extracts the payload from a PUBLISH packet, converts it to JSON format,
     * and logs it with client and topic information. STATE messages are excluded from logging
     * as they do not contain Sparkplug B payloads.
     * <p>
     * The log output is written to the "com.hivemq.extensions.sparkplug.jsonLog" logger.
     *
     * @param clientId       the MQTT client ID that published the message
     * @param origin         the original topic on which the message was published
     * @param publishPacket  the PUBLISH packet containing the Sparkplug payload
     * @param topicStructure the parsed Sparkplug topic structure
     */
    public static void logFormattedPayload(
            final @NotNull String clientId,
            final @NotNull String origin,
            final @NotNull PublishPacket publishPacket,
            final @NotNull TopicStructure topicStructure) {
        if (publishPacket.getPayload().isPresent() && topicStructure.getMessageType() != MessageType.STATE) {
            JSON_LOG.info("JSON Sparkplug MSG: clientId={}, topic={} payload={}",
                    clientId,
                    origin,
                    asJSONFormatted(getPayloadAsJSON(publishPacket.getPayload().get())));
        }
    }

    /**
     * Formats a JSON string with proper indentation and line breaks for readability.
     * <p>
     * This method parses the input JSON string and re-serializes it with pretty printing
     * enabled. If the input is not valid JSON, an error message is returned instead.
     *
     * @param jsonObject the JSON string to format
     * @return the formatted JSON string with indentation, or an error message if parsing fails
     */
    @VisibleForTesting
    public static @NotNull String asJSONFormatted(String jsonObject) {
        final var mapper = new ObjectMapper();
        try {
            Object json = mapper.readValue(jsonObject, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (final IOException e) {
            return "*** PAYLOAD IS NOT VALID JSON DATA *** \n\n" + e.getMessage();
        }
    }

    /**
     * Converts a Sparkplug B payload to its JSON string representation.
     * <p>
     * This method decodes a Sparkplug B payload from a byte buffer and converts it to
     * a JSON string format using the Tahu library's built-in JSON serialization.
     * If decoding fails, an empty string is returned and an error is logged.
     *
     * @param payload the byte buffer containing the Sparkplug B payload
     * @return the JSON string representation of the payload, or an empty string if parsing fails
     */
    @VisibleForTesting
    public static @NotNull String getPayloadAsJSON(@NotNull ByteBuffer payload) {
        try {
            final var bytes = getBytesFromBuffer(payload);
            final var decoder = new SparkplugBPayloadDecoder();
            final var sparkplugPayload = decoder.buildFromByteArray(bytes, null);
            return org.eclipse.tahu.util.PayloadUtil.toJsonString(sparkplugPayload);
        } catch (final Exception e) {
            JSON_LOG.error("Failed to parse the Sparkplug payload", e);
        }
        return "";
    }

    /**
     * Decodes a Sparkplug B payload from a byte buffer.
     * <p>
     * This is a helper method that extracts bytes from the buffer and uses the
     * Tahu library's SparkplugBPayloadDecoder to parse them into a SparkplugBPayload object.
     * If decoding fails, an error is logged and {@code null} is returned.
     *
     * @param payload the byte buffer containing the Sparkplug B payload
     * @return the decoded SparkplugBPayload, or {@code null} if parsing fails
     */
    private static SparkplugBPayload getSparkplugBPayload(final @NotNull ByteBuffer payload) {
        try {
            final var bytes = getBytesFromBuffer(payload);
            final var decoder = new SparkplugBPayloadDecoder();
            return decoder.buildFromByteArray(bytes, null);
        } catch (final Exception e) {
            LOG.error("Failed to parse the Sparkplug payload", e);
        }
        return null;
    }

    /**
     * Extracts the remaining bytes from a byte buffer into a byte array.
     * <p>
     * This is a helper method that reads all remaining bytes from the buffer's current
     * position to its limit and returns them as a byte array.
     *
     * @param byteBuffer the byte buffer to extract bytes from
     * @return a byte array containing the remaining bytes from the buffer
     */
    private static byte[] getBytesFromBuffer(final @NotNull ByteBuffer byteBuffer) {
        final var bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }
}
