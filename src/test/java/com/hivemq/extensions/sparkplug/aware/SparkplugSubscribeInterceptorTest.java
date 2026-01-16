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

import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundOutput;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscribePacket;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscription;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SparkplugSubscribeInterceptorTest {

    @TempDir
    private @NotNull Path tempDir;

    private final @NotNull SubscribeInboundInput subscribeInboundInput = mock();
    private final @NotNull SubscribeInboundOutput subscribeInboundOutput = mock();
    private final @NotNull ModifiableSubscription subscription = mock();
    private final @NotNull ModifiableSubscribePacket subscribePacket = mock();
    private final @NotNull ClientInformation clientInformation = mock();

    private @NotNull Path file;

    @BeforeEach
    void setUp() {
        file = tempDir.resolve("sparkplug.properties");

        when(subscribeInboundInput.getClientInformation()).thenReturn(clientInformation);
        when(clientInformation.getClientId()).thenReturn("testClient");
        when(subscribeInboundOutput.getSubscribePacket()).thenReturn(subscribePacket);
    }

    @Test
    void systopic_subscription_sets_retain_as_published() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.systopic=$sparkplug/certificates/"));

        when(subscription.getTopicFilter()).thenReturn("$sparkplug/certificates/spBv1.0/group/NBIRTH/node");
        when(subscribePacket.getSubscriptions()).thenReturn(List.of(subscription));

        interceptor.onInboundSubscribe(subscribeInboundInput, subscribeInboundOutput);

        final var captor = ArgumentCaptor.forClass(Boolean.class);
        verify(subscription).setRetainAsPublished(captor.capture());
        assertThat(captor.getValue()).isTrue();
    }

    @Test
    void systopic_wildcard_subscription_sets_retain_as_published() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.systopic=$sparkplug/certificates/"));

        when(subscription.getTopicFilter()).thenReturn("$sparkplug/certificates/#");
        when(subscribePacket.getSubscriptions()).thenReturn(List.of(subscription));

        interceptor.onInboundSubscribe(subscribeInboundInput, subscribeInboundOutput);

        final var captor = ArgumentCaptor.forClass(Boolean.class);
        verify(subscription).setRetainAsPublished(captor.capture());
        assertThat(captor.getValue()).isTrue();
    }

    @Test
    void non_systopic_subscription_not_modified() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.systopic=$sparkplug/certificates/"));

        when(subscription.getTopicFilter()).thenReturn("some/other/topic");
        when(subscribePacket.getSubscriptions()).thenReturn(List.of(subscription));

        interceptor.onInboundSubscribe(subscribeInboundInput, subscribeInboundOutput);

        verify(subscription, never()).setRetainAsPublished(true);
    }

    @Test
    void multiple_subscriptions_only_systopic_modified() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.systopic=$sparkplug/certificates/"));

        final var sysTopicSubscription = mock(ModifiableSubscription.class);
        final var otherSubscription = mock(ModifiableSubscription.class);

        when(sysTopicSubscription.getTopicFilter()).thenReturn("$sparkplug/certificates/spBv1.0/group/NBIRTH/node");
        when(otherSubscription.getTopicFilter()).thenReturn("some/other/topic");
        when(subscribePacket.getSubscriptions()).thenReturn(List.of(sysTopicSubscription, otherSubscription));

        interceptor.onInboundSubscribe(subscribeInboundInput, subscribeInboundOutput);

        verify(sysTopicSubscription).setRetainAsPublished(true);
        verify(otherSubscription, never()).setRetainAsPublished(true);
    }

    @Test
    void custom_systopic_subscription_sets_retain_as_published() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.systopic=$custom/systopic/"));

        when(subscription.getTopicFilter()).thenReturn("$custom/systopic/spBv1.0/group/NBIRTH/node");
        when(subscribePacket.getSubscriptions()).thenReturn(List.of(subscription));

        interceptor.onInboundSubscribe(subscribeInboundInput, subscribeInboundOutput);

        final var captor = ArgumentCaptor.forClass(Boolean.class);
        verify(subscription).setRetainAsPublished(captor.capture());
        assertThat(captor.getValue()).isTrue();
    }

    @Test
    void empty_subscriptions_list_handled() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.systopic=$sparkplug/certificates/"));

        when(subscribePacket.getSubscriptions()).thenReturn(new ArrayList<>());

        // should not throw
        interceptor.onInboundSubscribe(subscribeInboundInput, subscribeInboundOutput);

        verify(subscription, never()).setRetainAsPublished(true);
    }

    private @NotNull SparkplugSubscribeInterceptor createInterceptor(final @NotNull List<String> properties)
            throws Exception {
        Files.write(file, properties);
        final var configuration = new SparkplugConfiguration(file.getParent().toFile());
        configuration.readPropertiesFromFile();
        return new SparkplugSubscribeInterceptor(configuration);
    }
}
