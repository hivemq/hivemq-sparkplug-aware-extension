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

import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundOutput;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import com.hivemq.extensions.sparkplug.aware.topics.MessageType;
import com.hivemq.extensions.sparkplug.aware.topics.TopicStructure;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hivemq.extensions.sparkplug.aware.utils.PayloadUtil.logFormattedPayload;
import static com.hivemq.extensions.sparkplug.aware.utils.PayloadUtil.modifySparkplugTimestamp;

/**
 * {@link PublishOutboundInterceptor},
 * Modifies the timestamp of the DEATH message
 * - that was originally stored in a will - so that the timestamp will be updated.
 *
 * @since 4.3.1
 */
public class SparkplugPublishOutboundInterceptor implements PublishOutboundInterceptor {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SparkplugPublishOutboundInterceptor.class);

    private final @NotNull String sparkplugVersion;
    private final boolean useCompression;
    private final boolean jsonLogEnabled;

    public SparkplugPublishOutboundInterceptor(final @NotNull SparkplugConfiguration configuration) {
        this.sparkplugVersion = configuration.getSparkplugVersion();
        this.useCompression = configuration.getCompression();
        this.jsonLogEnabled = configuration.getJsonLogEnabled();
    }

    @Override
    public void onOutboundPublish(
            @NotNull PublishOutboundInput publishOutboundInput,
            @NotNull PublishOutboundOutput publishOutboundOutput) {
        final var topic = publishOutboundInput.getPublishPacket().getTopic();
        final var clientId = publishOutboundInput.getClientInformation().getClientId();
        if (log.isTraceEnabled()) {
            log.trace("OUTBOUND PUBLISH at: {} to: {} ", topic, clientId);
        }
        final var topicStructure = new TopicStructure(topic);
        if (!topicStructure.isValid(sparkplugVersion)) {
            // skip it is not a sparkplug publish
            return;
        }
        if (topicStructure.getMessageType() == MessageType.NDEATH) {
            final var publishPacket = publishOutboundOutput.getPublishPacket();
            if (publishPacket.getPayload().isPresent()) {
                try {
                    final var newDeath = modifySparkplugTimestamp(useCompression, publishPacket.getPayload().get());
                    publishPacket.setPayload(newDeath);
                    if (log.isTraceEnabled()) {
                        log.trace("Modify timestamp of NDEATH message from: {}", topic);
                    }
                    if (jsonLogEnabled) {
                        logFormattedPayload(clientId, topic, publishPacket, topicStructure);
                    }
                } catch (final Exception all) {
                    log.error("Modify NDEATH message from {} failed: {}", topic, all.getMessage());
                    if (log.isDebugEnabled()) {
                        log.debug("Original exception", all);
                    }
                }
            } else {
                log.warn("No payload present in the sparkplug message");
            }
        }
    }
}
