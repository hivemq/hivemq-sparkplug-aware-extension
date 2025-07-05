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

import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extensions.sparkplug.aware.topics.MessageType;
import com.hivemq.extensions.sparkplug.aware.topics.TopicStructure;
import org.codehaus.jackson.map.ObjectMapper;
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
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public final class PayloadUtil {

    private static final @NotNull Logger jsonLog = LoggerFactory.getLogger("com.hivemq.extensions.sparkplug.jsonLog");
    private static final @NotNull CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.GZIP;
    private static final @NotNull Logger log = LoggerFactory.getLogger(PayloadUtil.class);

    PayloadUtil() {
    }

    public static ByteBuffer modifySparkplugTimestamp(boolean useCompression, ByteBuffer byteBuffer) throws Exception {
        final SparkplugBPayload inboundPayload = getSparkplugBPayload(byteBuffer);
        if (inboundPayload == null) {
            throw new IllegalArgumentException("Unable to get Sparkplug B Payload from byte buffer.");
        }
        // create the same payload with a new timestamp.
        SparkplugBPayload payload = new SparkplugBPayload(new Date(),
                inboundPayload.getMetrics(),
                inboundPayload.getSeq(),
                inboundPayload.getUuid(),
                inboundPayload.getBody());

        SparkplugBPayloadEncoder encoder = new SparkplugBPayloadEncoder();
        byte[] bytes;
        // compress payload (optional)
        if (useCompression) {
            bytes = encoder.getBytes(org.eclipse.tahu.util.PayloadUtil.compress(payload, compressionAlgorithm, false),
                    false);
        } else {
            bytes = encoder.getBytes(payload, false);
        }
        return ByteBuffer.wrap(bytes);
    }

    public static void logFormattedPayload(
            final @NotNull String clientId,
            final @NotNull String origin,
            final @NotNull PublishPacket publishPacket,
            final @NotNull TopicStructure topicStructure) {
        if (publishPacket.getPayload().isPresent() && topicStructure.getMessageType() != MessageType.STATE) {
            jsonLog.info("JSON Sparkplug MSG: clientId={}, topic={} payload={}",
                    clientId,
                    origin,
                    asJSONFormatted(getPayloadAsJSON(publishPacket.getPayload().get())));
        }
    }

    @VisibleForTesting
    public static @NotNull String asJSONFormatted(String jsonObject) {
        final var mapper = new ObjectMapper();
        String result;
        try {
            Object json = mapper.readValue(jsonObject, Object.class);
            result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (final IOException e) {
            result = "*** PAYLOAD IS NOT VALID JSON DATA *** \n\n" + e.getMessage();
        }
        return result;
    }

    @VisibleForTesting
    public static @NotNull String getPayloadAsJSON(@NotNull ByteBuffer payload) {
        try {
            final var bytes = getBytesFromBuffer(payload);
            final var decoder = new SparkplugBPayloadDecoder();
            final var sparkplugPayload = decoder.buildFromByteArray(bytes, null);
            return org.eclipse.tahu.util.PayloadUtil.toJsonString(sparkplugPayload);
        } catch (final Exception e) {
            jsonLog.error("Failed to parse the sparkplug payload - reason:", e);
        }
        return "";
    }

    public static Map<String, String> getMetricsAsMessages(final @NotNull ByteBuffer byteBuffer) {
        final var inboundPayload = getSparkplugBPayload(byteBuffer);
        if (inboundPayload == null) {
            log.warn("No payload present in the sparkplug message");
            return Collections.emptyMap();
        }
        final var metricAsJSONMessage = new TreeMap<String, String>();
        final var metricList = inboundPayload.getMetrics();
        for (final var metric : metricList) {
            if (metric.getDataType().toIntValue() > 11) {
                continue;
            }
            final var message = MetricMessage.createJSON(metric.getName(),
                    metric.getTimestamp(),
                    metric.getValue().toString(),
                    metric.getDataType().toString());
            metricAsJSONMessage.put(metric.getName(), message);
        }
        return metricAsJSONMessage;
    }

    private static SparkplugBPayload getSparkplugBPayload(final @NotNull ByteBuffer payload) {
        try {
            final var bytes = getBytesFromBuffer(payload);
            final var decoder = new SparkplugBPayloadDecoder();
            return decoder.buildFromByteArray(bytes, null);
        } catch (final Exception e) {
            log.error("Failed to parse the sparkplug payload - reason:", e);
        }
        return null;
    }

    private static byte[] getBytesFromBuffer(final @NotNull ByteBuffer byteBuffer) {
        final var bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }
}
