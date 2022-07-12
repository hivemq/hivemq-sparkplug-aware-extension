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
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.tahu.message.PayloadDecoder;
import org.eclipse.tahu.message.SparkplugBPayloadDecoder;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PayloadUtil {
    private static final @NotNull Logger log = LoggerFactory.getLogger(PayloadUtil.class);

    public static String asJSONFormatted(String jsonObject) {
        ObjectMapper mapper = new ObjectMapper();
        String result;
        try {
            Object json = mapper.readValue(jsonObject, Object.class);
            result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (IOException ex) {
            result = "*** PAYLOAD IS NOT VALID JSON DATA *** \n\n" + ex.getMessage();
        }
        return result;
    }

    public static String getPayloadAsJSON(@NotNull ByteBuffer payload) {
        try {
            byte[] bytes = getBytesFromBuffer(payload);
            PayloadDecoder<SparkplugBPayload> decoder = new SparkplugBPayloadDecoder();
            SparkplugBPayload sparkplugPayload = decoder.buildFromByteArray(bytes);
            return org.eclipse.tahu.util.PayloadUtil.toJsonString(sparkplugPayload);
        } catch (Exception e) {
            log.error("Failed to parse the sparkplug payload - reason:", e);
        }
        return "";
    }

    private static byte[] getBytesFromBuffer(ByteBuffer byteBuffer) {
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }

}
