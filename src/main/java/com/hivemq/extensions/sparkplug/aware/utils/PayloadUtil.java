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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extensions.sparkplug.aware.topics.MessageType;
import com.hivemq.extensions.sparkplug.aware.topics.TopicStructure;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.tahu.message.PayloadDecoder;
import org.eclipse.tahu.message.SparkplugBPayloadDecoder;
import org.eclipse.tahu.message.SparkplugBPayloadEncoder;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.eclipse.tahu.util.CompressionAlgorithm;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

public class PayloadUtil {
    private static final @NotNull Logger log = LoggerFactory.getLogger(PayloadUtil.class);
    private static final @NotNull Logger jsonLog = LoggerFactory.getLogger("com.hivemq.extensions.sparkplug.jsonLog");
    private static final CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.GZIP;

    public static ByteBuffer modifySparkplugTimestamp(Boolean useCompression, ByteBuffer byteBuffer) throws Exception {
        SparkplugBPayload inboundPayload = getSparkplugBPayload(byteBuffer);
        //create the same payload with a new timestamp.
        SparkplugBPayload payload =
                new SparkplugBPayload(new Date(),
                        inboundPayload.getMetrics(),
                        inboundPayload.getSeq(),
                        inboundPayload.getUuid(), inboundPayload.getBody());

        SparkplugBPayloadEncoder encoder = new SparkplugBPayloadEncoder();
        byte[] bytes = null;
        // Compress payload (optional)
        if (useCompression) {
            bytes = encoder.getBytes(org.eclipse.tahu.util.PayloadUtil.compress(payload, compressionAlgorithm));
        } else {
            bytes = encoder.getBytes(payload);
        }
        return ByteBuffer.wrap(bytes);
    }

    public static void logFormattedPayload(String clientId, String origin, PublishPacket publishPacket, TopicStructure topicStructure) {
        if (publishPacket.getPayload().isPresent() && topicStructure.getMessageType() != MessageType.STATE) {
            jsonLog.info("JSON Sparkplug MSG: clientId={}, topic={} payload={}",
                    clientId,
                    origin,
                    asJSONFormatted(getPayloadAsJSON(publishPacket.getPayload().get())));
        }
    }

    @VisibleForTesting
    public static String asJSONFormatted(String jsonObject) {
        final @NotNull ObjectMapper mapper = new ObjectMapper();
        @NotNull String result;
        try {
            Object json = mapper.readValue(jsonObject, Object.class);
            result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (IOException ex) {
            result = "*** PAYLOAD IS NOT VALID JSON DATA *** \n\n" + ex.getMessage();
        }
        return result;
    }

    @VisibleForTesting
    public static String getPayloadAsJSON(@NotNull ByteBuffer payload) {
        try {
            byte[] bytes = getBytesFromBuffer(payload);
            PayloadDecoder<SparkplugBPayload> decoder = new SparkplugBPayloadDecoder();
            SparkplugBPayload sparkplugPayload = decoder.buildFromByteArray(bytes);
            return org.eclipse.tahu.util.PayloadUtil.toJsonString(sparkplugPayload);
        } catch (Exception e) {
            jsonLog.error("Failed to parse the sparkplug payload - reason:", e);
        }
        return "";
    }

    private static SparkplugBPayload getSparkplugBPayload(@NotNull ByteBuffer payload) {
        try {
            byte[] bytes = getBytesFromBuffer(payload);
            PayloadDecoder<SparkplugBPayload> decoder = new SparkplugBPayloadDecoder();
            return decoder.buildFromByteArray(bytes);
        } catch (Exception e) {
            log.error("Failed to parse the sparkplug payload - reason:", e);
        }
        return null;
    }

    private static byte[] getBytesFromBuffer(ByteBuffer byteBuffer) {
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }

}
