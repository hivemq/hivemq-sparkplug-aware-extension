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
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableOutboundPublish;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import org.eclipse.tahu.message.SparkplugBPayloadEncoder;
import org.eclipse.tahu.message.model.Metric;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.eclipse.tahu.message.model.MetricDataType.Int32;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SparkplugPublishOutboundInterceptorTest {

    @TempDir
    private @NotNull Path tempDir;

    private final @NotNull PublishOutboundInput publishOutboundInput = mock();
    private final @NotNull PublishOutboundOutput publishOutboundOutput = mock();
    private final @NotNull PublishPacket publishPacket = mock();
    private final @NotNull ModifiableOutboundPublish modifiableOutboundPublish = mock();
    private final @NotNull ClientInformation clientInformation = mock();

    private @NotNull Path file;
    private byte @NotNull [] encodedSparkplugPayload;

    @BeforeEach
    void setUp() throws Exception {
        file = tempDir.resolve("sparkplug.properties");

        when(publishOutboundInput.getPublishPacket()).thenReturn(publishPacket);
        when(publishOutboundInput.getClientInformation()).thenReturn(clientInformation);
        when(clientInformation.getClientId()).thenReturn("testClient");

        when(publishOutboundOutput.getPublishPacket()).thenReturn(modifiableOutboundPublish);

        encodedSparkplugPayload = createSparkplugBPayload();
    }

    @Test
    void nbirth_payload_not_modified() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.version=spBv1.0"));

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NBIRTH/edgeNode");

        interceptor.onOutboundPublish(publishOutboundInput, publishOutboundOutput);

        verify(modifiableOutboundPublish, never()).setPayload(any());
    }

    @Test
    void dbirth_payload_not_modified() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.version=spBv1.0"));

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/DBIRTH/edgeNode/device");

        interceptor.onOutboundPublish(publishOutboundInput, publishOutboundOutput);

        verify(modifiableOutboundPublish, never()).setPayload(any());
    }

    @Test
    void ndeath_payload_timestamp_modified() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.version=spBv1.0"));

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NDEATH/edgeNode");
        when(modifiableOutboundPublish.getPayload()).thenReturn(Optional.of(ByteBuffer.wrap(encodedSparkplugPayload)));

        interceptor.onOutboundPublish(publishOutboundInput, publishOutboundOutput);

        verify(modifiableOutboundPublish).setPayload(any(ByteBuffer.class));
    }

    @Test
    void ddeath_payload_not_modified() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.version=spBv1.0"));

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/DDEATH/edgeNode/device");

        interceptor.onOutboundPublish(publishOutboundInput, publishOutboundOutput);

        // DDEATH is not handled by the outbound interceptor (only NDEATH)
        verify(modifiableOutboundPublish, never()).setPayload(any());
    }

    @Test
    void ddata_payload_not_modified() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.version=spBv1.0"));

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/DDATA/edgeNode/device");

        interceptor.onOutboundPublish(publishOutboundInput, publishOutboundOutput);

        verify(modifiableOutboundPublish, never()).setPayload(any());
    }

    @Test
    void ndata_payload_not_modified() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.version=spBv1.0"));

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NDATA/edgeNode");

        interceptor.onOutboundPublish(publishOutboundInput, publishOutboundOutput);

        verify(modifiableOutboundPublish, never()).setPayload(any());
    }

    @Test
    void non_sparkplug_topic_ignored() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.version=spBv1.0"));

        when(publishPacket.getTopic()).thenReturn("some/other/topic");

        interceptor.onOutboundPublish(publishOutboundInput, publishOutboundOutput);

        verify(modifiableOutboundPublish, never()).setPayload(any());
    }

    @Test
    void ndeath_without_payload_logs_warning() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.version=spBv1.0"));

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NDEATH/edgeNode");
        when(modifiableOutboundPublish.getPayload()).thenReturn(Optional.empty());

        // should not throw, just log warning
        interceptor.onOutboundPublish(publishOutboundInput, publishOutboundOutput);

        verify(modifiableOutboundPublish, never()).setPayload(any());
    }

    @Test
    void wrong_sparkplug_version_ignored() throws Exception {
        final var interceptor = createInterceptor(List.of("sparkplug.version=spBv1.0"));

        // using wrong version in topic
        when(publishPacket.getTopic()).thenReturn("spBv2.0/group/NDEATH/edgeNode");

        interceptor.onOutboundPublish(publishOutboundInput, publishOutboundOutput);

        verify(modifiableOutboundPublish, never()).setPayload(any());
    }

    private SparkplugPublishOutboundInterceptor createInterceptor(
            final @NotNull List<String> properties) throws Exception {
        Files.write(file, properties);
        final var configuration = new SparkplugConfiguration(file.getParent().toFile());
        configuration.readPropertiesFromFile();
        return new SparkplugPublishOutboundInterceptor(configuration);
    }

    private byte @NotNull [] createSparkplugBPayload() throws Exception {
        final var metrics = new ArrayList<Metric>();
        metrics.add(new Metric.MetricBuilder("testMetric", Int32, 42).timestamp(new Date()).createMetric());
        final var sparkplugBPayload = new SparkplugBPayload(new Date(), metrics, 1L, null, null);
        return new SparkplugBPayloadEncoder().getBytes(sparkplugBPayload, false);
    }
}
