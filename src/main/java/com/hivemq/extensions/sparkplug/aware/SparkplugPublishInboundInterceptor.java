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

import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extension.sdk.api.services.builder.PublishBuilder;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import com.hivemq.extensions.sparkplug.aware.topics.MessageType;
import com.hivemq.extensions.sparkplug.aware.topics.TopicStructure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hivemq.extensions.sparkplug.aware.utils.PayloadUtil.logFormattedPayload;
import static com.hivemq.extensions.sparkplug.aware.utils.PayloadUtil.modifySparkplugTimestamp;

/**
 * {@link PublishInboundInterceptor},
 * forwards each incoming NBIRTH and DBIRTH Message in a sparkplug topic structure into a system topic.
 *
 * @since 4.3.1
 */
public class SparkplugPublishInboundInterceptor implements PublishInboundInterceptor {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SparkplugPublishInboundInterceptor.class);

    private final @NotNull PublishService publishService;
    private final @NotNull PublishBuilder publishBuilder;
    private final @NotNull String sparkplugVersion;
    private final @NotNull String sysTopic;
    private final boolean useCompression;
    private final boolean jsonLogEnabled;
    private final @NotNull Long messageExpiry;

    public SparkplugPublishInboundInterceptor(
            final @NotNull SparkplugConfiguration configuration,
            final @NotNull PublishService publishService) {
        this(configuration, publishService, Builders.publish());
    }

    @VisibleForTesting
    SparkplugPublishInboundInterceptor(
            final @NotNull SparkplugConfiguration configuration,
            final @NotNull PublishService publishService,
            final @NotNull PublishBuilder publishBuilder) {
        this.sparkplugVersion = configuration.getSparkplugVersion();
        this.sysTopic = configuration.getSparkplugSysTopic();
        this.useCompression = configuration.getCompression();
        this.jsonLogEnabled = configuration.getJsonLogEnabled();
        this.publishService = publishService;
        this.publishBuilder = publishBuilder;
        this.messageExpiry = configuration.getSparkplugSystopicMsgexpiry();
    }

    @Override
    public void onInboundPublish(
            final @NotNull PublishInboundInput publishInboundInput,
            final @NotNull PublishInboundOutput publishInboundOutput) {
        final var clientId = publishInboundInput.getClientInformation().getClientId();
        final var publishPacket = publishInboundInput.getPublishPacket();
        final var origin = publishPacket.getTopic();
        final var topicStructure = new TopicStructure(origin);
        if (log.isTraceEnabled()) {
            log.trace("INBOUND PUBLISH at: {} from: {}", origin, clientId);
        }
        if (!topicStructure.isValid(sparkplugVersion)) {
            // skip it is not a sparkplug publish
            return;
        }
        if (topicStructure.getMessageType() == MessageType.NBIRTH ||
                topicStructure.getMessageType() == MessageType.DBIRTH) {
            // it is a sparkplug publish
            try {
                // build the publish
                publishBuilder.fromPublish(publishPacket);
                publishBuilder.topic(sysTopic + origin);
                publishBuilder.qos(Qos.AT_LEAST_ONCE);
                publishBuilder.retain(true);
                publishBuilder.messageExpiryInterval(messageExpiry);
                final var clone = publishBuilder.build();
                publishToSysTopic(origin, clone);
            } catch (final Exception all) {
                log.error("Publish to sysTopic {} failed: {}", sysTopic, all.getMessage());
            }
        } else if (topicStructure.getMessageType() == MessageType.NDEATH) {
            final var modifiablePublishPacket = publishInboundOutput.getPublishPacket();
            if (modifiablePublishPacket.getPayload().isPresent()) {
                final var byteBuffer = modifiablePublishPacket.getPayload().get();
                try {
                    final var newDeath = modifySparkplugTimestamp(useCompression, byteBuffer);
                    modifiablePublishPacket.setPayload(newDeath);
                    if (log.isTraceEnabled()) {
                        log.trace("Modify timestamp of NDEATH message from: {}", origin);
                    }
                } catch (final Exception all) {
                    log.error("Modify NDEATH message from {} failed: {}", origin, all.getMessage());
                    if (log.isTraceEnabled()) {
                        log.trace("Original exception", all);
                    }
                }
            } else {
                log.warn("No payload present in the sparkplug message");
            }
        }
        if (jsonLogEnabled) {
            logFormattedPayload(clientId, origin, publishPacket, topicStructure);
        }
    }

    private void publishToSysTopic(final @NotNull String origin, final @NotNull Publish publish) {
        // Asynchronously sent PUBLISH
        final var future = publishService.publish(publish);
        future.whenComplete((aVoid, throwable) -> {
            if (throwable == null) {
                if (log.isTraceEnabled()) {
                    log.trace("Published CLONE Msg from: {} to: {} ", origin, sysTopic + origin);
                }
            } else {
                log.error("Publish to sysTopic: {} failed.", sysTopic + origin, throwable.fillInStackTrace());
            }
        });
    }
}
