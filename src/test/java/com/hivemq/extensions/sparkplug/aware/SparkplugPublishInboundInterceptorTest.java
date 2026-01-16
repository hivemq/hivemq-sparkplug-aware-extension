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
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.ModifiablePublishPacket;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extension.sdk.api.services.builder.PublishBuilder;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import org.eclipse.tahu.message.SparkplugBPayloadEncoder;
import org.eclipse.tahu.message.model.Metric;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tahu.message.model.MetricDataType.Int32;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SparkplugPublishInboundInterceptorTest {

    @TempDir
    private @NotNull Path tempDir;

    private final @NotNull PublishInboundInput publishInboundInput = mock();
    private final @NotNull PublishInboundOutput publishInboundOutput = mock();
    private final @NotNull PublishPacket publishPacket = mock();
    private final @NotNull ModifiablePublishPacket modifiablePublishPacket = mock();
    private final @NotNull ClientInformation clientInformation = mock();
    private final @NotNull PublishService publishService = mock();
    private final @NotNull PublishBuilder publishBuilder = mock();
    private final @NotNull Publish builtPublish = mock();

    private @NotNull Path file;
    private byte @NotNull [] encodedSparkplugPayload;

    @BeforeEach
    void setUp() throws Exception {
        file = tempDir.resolve("sparkplug.properties");

        when(publishInboundInput.getPublishPacket()).thenReturn(publishPacket);
        when(publishInboundInput.getClientInformation()).thenReturn(clientInformation);
        when(clientInformation.getClientId()).thenReturn("testClient");

        when(publishInboundOutput.getPublishPacket()).thenReturn(modifiablePublishPacket);

        when(publishBuilder.fromPublish(any(PublishPacket.class))).thenReturn(publishBuilder);
        when(publishBuilder.topic(any())).thenReturn(publishBuilder);
        when(publishBuilder.qos(any())).thenReturn(publishBuilder);
        when(publishBuilder.retain(any(Boolean.class))).thenReturn(publishBuilder);
        when(publishBuilder.messageExpiryInterval(any(Long.class))).thenReturn(publishBuilder);
        when(publishBuilder.build()).thenReturn(builtPublish);

        when(publishService.publish(any(Publish.class))).thenReturn(CompletableFuture.completedFuture(null));

        encodedSparkplugPayload = createSparkplugBPayload();
    }

    @Test
    void nbirth_message_republished_to_systopic() throws Exception {
        final var configuration = getSparkplugConfiguration(List.of("sparkplug.version=spBv1.0"));
        final var interceptor = new SparkplugPublishInboundInterceptor(configuration, publishService, publishBuilder);

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NBIRTH/edgeNode");
        when(publishPacket.getPayload()).thenReturn(Optional.of(ByteBuffer.wrap(encodedSparkplugPayload)));

        interceptor.onInboundPublish(publishInboundInput, publishInboundOutput);

        // verify publish was built with correct parameters
        verify(publishBuilder).fromPublish(publishPacket);
        verify(publishBuilder).topic("$sparkplug/certificates/spBv1.0/group/NBIRTH/edgeNode");
        verify(publishBuilder).qos(Qos.AT_LEAST_ONCE);
        verify(publishBuilder).retain(true);
        verify(publishService).publish(builtPublish);
    }

    @Test
    void dbirth_message_republished_to_systopic() throws Exception {
        final var configuration = getSparkplugConfiguration(List.of("sparkplug.version=spBv1.0"));
        final var interceptor = new SparkplugPublishInboundInterceptor(configuration, publishService, publishBuilder);

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/DBIRTH/edgeNode/device");
        when(publishPacket.getPayload()).thenReturn(Optional.of(ByteBuffer.wrap(encodedSparkplugPayload)));

        interceptor.onInboundPublish(publishInboundInput, publishInboundOutput);

        // verify publish was built with correct parameters
        verify(publishBuilder).topic("$sparkplug/certificates/spBv1.0/group/DBIRTH/edgeNode/device");
        verify(publishBuilder).retain(true);
        verify(publishService).publish(builtPublish);
    }

    @Test
    void ndeath_message_timestamp_modified() throws Exception {
        final var configuration = getSparkplugConfiguration(List.of("sparkplug.version=spBv1.0"));
        final var interceptor = new SparkplugPublishInboundInterceptor(configuration, publishService, publishBuilder);

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NDEATH/edgeNode");
        when(modifiablePublishPacket.getPayload()).thenReturn(Optional.of(ByteBuffer.wrap(encodedSparkplugPayload)));

        interceptor.onInboundPublish(publishInboundInput, publishInboundOutput);

        // NDEATH modifies the payload timestamp but does not republish to systopic
        verify(modifiablePublishPacket).setPayload(any(ByteBuffer.class));
        verify(publishService, never()).publish(any());
    }

    @Test
    void non_sparkplug_topic_ignored() throws Exception {
        final var configuration = getSparkplugConfiguration(List.of("sparkplug.version=spBv1.0"));
        final var interceptor = new SparkplugPublishInboundInterceptor(configuration, publishService, publishBuilder);

        when(publishPacket.getTopic()).thenReturn("some/other/topic");

        interceptor.onInboundPublish(publishInboundInput, publishInboundOutput);

        // verify no publish was made
        verify(publishService, never()).publish(any());
        verify(modifiablePublishPacket, never()).setPayload(any());
    }

    @Test
    void ddata_message_not_republished() throws Exception {
        final var configuration = getSparkplugConfiguration(List.of("sparkplug.version=spBv1.0"));
        final var interceptor = new SparkplugPublishInboundInterceptor(configuration, publishService, publishBuilder);

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/DDATA/edgeNode/device");

        interceptor.onInboundPublish(publishInboundInput, publishInboundOutput);

        // DDATA messages should not be republished to systopic
        verify(publishService, never()).publish(any());
    }

    @Test
    void ndata_message_not_republished() throws Exception {
        final var configuration = getSparkplugConfiguration(List.of("sparkplug.version=spBv1.0"));
        final var interceptor = new SparkplugPublishInboundInterceptor(configuration, publishService, publishBuilder);

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NDATA/edgeNode");

        interceptor.onInboundPublish(publishInboundInput, publishInboundOutput);

        // NDATA messages should not be republished to systopic
        verify(publishService, never()).publish(any());
    }

    @Test
    void custom_systopic_used() throws Exception {
        final var configuration =
                getSparkplugConfiguration(List.of("sparkplug.version=spBv1.0", "sparkplug.systopic=$custom/systopic/"));
        final var interceptor = new SparkplugPublishInboundInterceptor(configuration, publishService, publishBuilder);

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NBIRTH/edgeNode");
        when(publishPacket.getPayload()).thenReturn(Optional.of(ByteBuffer.wrap(encodedSparkplugPayload)));

        interceptor.onInboundPublish(publishInboundInput, publishInboundOutput);

        // verify custom systopic is used
        final var topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(publishBuilder).topic(topicCaptor.capture());
        assertThat(topicCaptor.getValue()).startsWith("$custom/systopic/");
    }

    private SparkplugConfiguration getSparkplugConfiguration(final @NotNull List<String> properties) throws Exception {
        Files.write(file, properties);
        final var configuration = new SparkplugConfiguration(file.getParent().toFile());
        configuration.readPropertiesFromFile();
        return configuration;
    }

    private byte @NotNull [] createSparkplugBPayload() throws Exception {
        final var metrics = new ArrayList<Metric>();
        metrics.add(new Metric.MetricBuilder("testMetric", Int32, 42).timestamp(new Date()).createMetric());
        final var sparkplugBPayload = new SparkplugBPayload(new Date(), metrics, 1L, null, null);
        return new SparkplugBPayloadEncoder().getBytes(sparkplugBPayload, false);
    }
}
