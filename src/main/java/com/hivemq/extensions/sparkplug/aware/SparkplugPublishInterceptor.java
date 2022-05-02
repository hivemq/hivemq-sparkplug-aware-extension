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
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extension.sdk.api.services.builder.PublishBuilder;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import com.hivemq.extensions.sparkplug.aware.topics.MessageType;
import com.hivemq.extensions.sparkplug.aware.topics.TopicStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * {@link PublishInboundInterceptor},
 * forwards each incoming NBIRTH and DBIRTH Message in a sparkplug topic structure into a system topic.
 *
 * @author Yannick Weber
 * @since 4.3.1
 */
public class SparkplugPublishInterceptor implements PublishInboundInterceptor {
    private static final @NotNull Logger log = LoggerFactory.getLogger(SparkplugPublishInterceptor.class);
    private final PublishService publishService;
    private final PublishBuilder publishBuilder;
    private final @NotNull String sparkplugVersion;
    private final @NotNull String sysTopic;

    public SparkplugPublishInterceptor(final @NotNull SparkplugConfiguration configuration,
                                       final @NotNull PublishService publishService,
                                       final @NotNull PublishBuilder publishBuilder ) {
        this.sparkplugVersion = configuration.getSparkplugVersion();
        this.sysTopic = configuration.getSparkplugSysTopic();
        this.publishService = publishService;
        this.publishBuilder = publishBuilder;
    }

    @Override
    public void onInboundPublish(
            final @NotNull PublishInboundInput publishInboundInput,
            final @NotNull PublishInboundOutput publishInboundOutput) {

        final PublishPacket publishPacket = publishInboundOutput.getPublishPacket();
        final String topic = publishPacket.getTopic();
        final TopicStructure topicStructure = new TopicStructure(topic);

        if (topicStructure.isValid(sparkplugVersion) &&
                (topicStructure.getMessageType() == MessageType.NBIRTH
                        || topicStructure.getMessageType() == MessageType.DBIRTH)) {
            //it is a sparkplug publish
            final PublishBuilder myBuilder = publishBuilder;
            try {
                // Build the publish
                myBuilder.fromPublish(publishInboundInput.getPublishPacket());
                myBuilder.topic(sysTopic + topic);
                myBuilder.retain(true);
                publishToSysTopic(topic, myBuilder);
            } catch (Exception all) {
                log.error("Publish to sysTopic {} failed: {}", sysTopic, all.getMessage());
            }
        }
    }

    private void publishToSysTopic(String topic, PublishBuilder myBuilder) {
        // Asynchronously sent PUBLISH
        final CompletableFuture<Void> future = publishService.publish(myBuilder.build());
        future.whenComplete((aVoid, throwable) -> {
            if (throwable == null) {
                log.debug("Publish Msg from {} to {} ", topic, sysTopic + topic);
            } else {
                log.error("Publish to sysTopic {} failed {} ", topic, throwable.fillInStackTrace());
            }
        });
    }
}