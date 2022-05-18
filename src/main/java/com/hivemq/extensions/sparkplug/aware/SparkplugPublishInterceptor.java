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
package com.hivemq.extensions.sparkplug.aware;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundOutput;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableOutboundPublish;
import com.hivemq.extension.sdk.api.packets.publish.ModifiablePublishPacket;
import com.hivemq.extension.sdk.api.services.builder.PublishBuilder;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import com.hivemq.extensions.sparkplug.aware.topics.MessageType;
import com.hivemq.extensions.sparkplug.aware.topics.TopicStructure;
import org.eclipse.tahu.message.SparkplugBPayloadDecoder;
import org.eclipse.tahu.message.SparkplugBPayloadEncoder;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.eclipse.tahu.util.CompressionAlgorithm;
import org.eclipse.tahu.util.PayloadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * {@link PublishInboundInterceptor},
 * forwards each incoming NBIRTH and DBIRTH Message in a sparkplug topic structure into a system topic.
 *
 * @since 4.3.1
 */
public class SparkplugPublishInterceptor implements PublishInboundInterceptor, PublishOutboundInterceptor {
    private static final @NotNull Logger log = LoggerFactory.getLogger(SparkplugPublishInterceptor.class);
    private static final CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.GZIP;
    private final PublishService publishService;
    private final PublishBuilder publishBuilder;
    private final @NotNull String sparkplugVersion;
    private final @NotNull String sysTopic;
    private final Boolean useCompression;

    public SparkplugPublishInterceptor(final @NotNull SparkplugConfiguration configuration,
                                       final @NotNull PublishService publishService,
                                       final @NotNull PublishBuilder publishBuilder) {
        this.sparkplugVersion = configuration.getSparkplugVersion();
        this.sysTopic = configuration.getSparkplugSysTopic();
        this.useCompression = configuration.getCompression();
        this.publishService = publishService;
        this.publishBuilder = publishBuilder;
    }

    @Override
    public void onInboundPublish(
            final @NotNull PublishInboundInput publishInboundInput,
            final @NotNull PublishInboundOutput publishInboundOutput) {

        final String origin = publishInboundOutput.getPublishPacket().getTopic();
        final TopicStructure topicStructure = new TopicStructure(origin);

        if (!topicStructure.isValid(sparkplugVersion)) {
            //skip it is not a sparkplug publish
            return;
        }

        if (topicStructure.getMessageType() == MessageType.NBIRTH || topicStructure.getMessageType() == MessageType.DBIRTH) {
            //it is a sparkplug publish
            final PublishBuilder myBuilder = publishBuilder;
            try {
                // Build the publish
                myBuilder.fromPublish(publishInboundInput.getPublishPacket());
                myBuilder.topic(sysTopic + origin);
                myBuilder.qos(Qos.AT_LEAST_ONCE);
                myBuilder.retain(true);
                publishToSysTopic(origin, myBuilder);
            } catch (Exception all) {
                log.error("Publish to sysTopic {} failed: {}", sysTopic, all.getMessage());
            }
        }
    }

    @Override
    public void onOutboundPublish(@NotNull PublishOutboundInput publishOutboundInput, @NotNull PublishOutboundOutput publishOutboundOutput) {
        final String topic = publishOutboundInput.getPublishPacket().getTopic();
        final TopicStructure topicStructure = new TopicStructure(topic);

        if (!topicStructure.isValid(sparkplugVersion)) {
            //skip it is not a sparkplug publish
            return;
        }
        if (topicStructure.getMessageType() == MessageType.NDEATH) {
            ModifiableOutboundPublish publishPacket = publishOutboundOutput.getPublishPacket();
            final ByteBuffer byteBuffer = publishPacket.getPayload().get();
            try {
                final ByteBuffer newDeath = modifySparkplugTimestamp(byteBuffer);
                publishPacket.setPayload(newDeath);
                log.debug("Modify timestamp of NDEATH message from: {}", topic);
            } catch (Exception all) {
                log.error("Modify NDEATH message from {} failed: {}", topic, all.getMessage());
                if(log.isDebugEnabled()) {
                    all.printStackTrace();
                }
            }
        }
    }

    private ByteBuffer modifySparkplugTimestamp(ByteBuffer byteBuffer) throws Exception {
        final @NotNull SparkplugBPayloadDecoder decoder = new SparkplugBPayloadDecoder();
        byte[] bytes = null;

        SparkplugBPayload inboundPayload = decoder.buildFromByteArray(byteBuffer.array());
        //create the same payload with a new timestamp.
        SparkplugBPayload payload =
                new SparkplugBPayload(new Date(),
                        inboundPayload.getMetrics(),
                        inboundPayload.getSeq(),
                        inboundPayload.getUuid(), inboundPayload.getBody());

        SparkplugBPayloadEncoder encoder = new SparkplugBPayloadEncoder();

        // Compress payload (optional)
        if (useCompression) {
            bytes = encoder.getBytes(PayloadUtil.compress(payload, compressionAlgorithm));
        } else {
            bytes = encoder.getBytes(payload);
        }
        return ByteBuffer.wrap(bytes);
    }

    private void publishToSysTopic(String origin, PublishBuilder myBuilder) {
        // Asynchronously sent PUBLISH
        final CompletableFuture<Void> future = publishService.publish(myBuilder.build());
        future.whenComplete((aVoid, throwable) -> {
            if (throwable == null) {
                log.debug("Publish Msg from: {} to: {} ", origin, sysTopic + origin);
            } else {
                log.error("Publish to sysTopic: {} failed: {} ", sysTopic + origin, throwable.fillInStackTrace());
            }
        });
    }

}