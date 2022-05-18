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
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundOutput;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableOutboundPublish;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscription;
import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
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
import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * {@link SparkplugSubscribeInterceptor},
 * The standard behavior for sys topics is following:
 *  if a subscriber exist - retained message will be published as not retained  - live publish
 *  if no subscriber exist - retained message will be published as retained
 *
 * The Interceptor modifies subscriptions
 *  Subscriptions for the sys topic will be modified, so that the retained flag will be handled as it is published.
 *
 * @since 4.3.1
 */
public class SparkplugSubscribeInterceptor implements SubscribeInboundInterceptor {
    private static final @NotNull Logger log = LoggerFactory.getLogger(SparkplugSubscribeInterceptor.class);
    private final @NotNull String sysTopic;

    public SparkplugSubscribeInterceptor(final @NotNull SparkplugConfiguration configuration) {
        this.sysTopic = configuration.getSparkplugSysTopic();
    }

    @Override
    public void onInboundSubscribe(@NotNull SubscribeInboundInput subscribeInboundInput, @NotNull SubscribeInboundOutput subscribeInboundOutput) {
        final String clientID = subscribeInboundInput.getClientInformation().getClientId();
        for( ModifiableSubscription subscription : subscribeInboundOutput.getSubscribePacket().getSubscriptions() ){
            if( subscription.getTopicFilter().startsWith(this.sysTopic)) {
                log.debug("Modify Subscribe - to have retained as published {} from Client {}",
                        subscription.getTopicFilter() , clientID);
                subscription.setRetainAsPublished(true);
            }
        }
    }



}