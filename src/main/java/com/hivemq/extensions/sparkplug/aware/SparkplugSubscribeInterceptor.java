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

import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundOutput;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interceptor for SUBSCRIBE packets that modifies subscription behavior for Sparkplug system topics.
 * <p>
 * The standard MQTT behavior for system topics is as follows:
 * <ul>
 *     <li>If a subscriber exists - retained messages will be published as non-retained (live publish)</li>
 *     <li>If no subscriber exists - retained messages will be published as retained</li>
 * </ul>
 * <p>
 * This interceptor modifies subscriptions to Sparkplug system topics so that the retained flag
 * is preserved as published, ensuring consistent behavior regardless of subscriber presence.
 *
 * @author David Sondermann
 * @since 4.3.1
 */
public class SparkplugSubscribeInterceptor implements SubscribeInboundInterceptor {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(SparkplugSubscribeInterceptor.class);

    private final @NotNull String sysTopic;

    public SparkplugSubscribeInterceptor(final @NotNull SparkplugConfiguration configuration) {
        this.sysTopic = configuration.getSparkplugSysTopic();
    }

    @Override
    public void onInboundSubscribe(
            final @NotNull SubscribeInboundInput subscribeInboundInput,
            final @NotNull SubscribeInboundOutput subscribeInboundOutput) {
        final var clientID = subscribeInboundInput.getClientInformation().getClientId();
        for (final var subscription : subscribeInboundOutput.getSubscribePacket().getSubscriptions()) {
            if (subscription.getTopicFilter().startsWith(this.sysTopic)) {
                LOG.debug("Modify Subscribe - to have retained as published {} from Client {}",
                        subscription.getTopicFilter(),
                        clientID);
                subscription.setRetainAsPublished(true);
            }
        }
    }
}
