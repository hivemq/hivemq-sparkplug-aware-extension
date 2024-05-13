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
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableOutboundPublish;
import com.hivemq.extension.sdk.api.packets.publish.ModifiablePublishPacket;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import org.eclipse.tahu.SparkplugInvalidTypeException;
import org.eclipse.tahu.message.SparkplugBPayloadEncoder;
import org.eclipse.tahu.message.model.Metric;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.eclipse.tahu.message.model.MetricDataType.Int32;
import static org.mockito.Mockito.*;

/**
 * @author Anja Helmbrecht-Schaar
 */
class SparkplugPublishOutboundInterceptorTest {

    List<Metric> metrics = new ArrayList<Metric>();
    byte[] encodedSparkplugPayload;
    private @NotNull SparkplugPublishOutboundInterceptor sparkplugPublishOutboundInterceptor;
    private @NotNull PublishOutboundInput publishOutboundInput;
    private @NotNull PublishOutboundOutput publishOutboundOutput;
    private @NotNull ModifiablePublishPacket publishPacket;
    private @NotNull Path file;
    private @NotNull ModifiableOutboundPublish modifiableOutboundPublish;
    private @NotNull ClientInformation clientInformation;

    @BeforeEach
    void setUp(final @TempDir @NotNull Path tempDir) {
        file = tempDir.resolve("sparkplug.properties");
        SparkplugConfiguration configuration = new SparkplugConfiguration(file.toFile());
        sparkplugPublishOutboundInterceptor = new SparkplugPublishOutboundInterceptor(configuration);
        publishOutboundInput = mock(PublishOutboundInput.class);
        publishOutboundOutput = mock(PublishOutboundOutput.class);
        modifiableOutboundPublish = mock(ModifiableOutboundPublish.class);
        publishPacket = mock(ModifiablePublishPacket.class);
        when(publishOutboundInput.getPublishPacket()).thenReturn(modifiableOutboundPublish);

        when(publishOutboundInput.getPublishPacket()).thenReturn(publishPacket);
        when(publishOutboundOutput.getPublishPacket()).thenReturn(modifiableOutboundPublish);

        clientInformation = mock(ClientInformation.class);

        try {
            encodedSparkplugPayload = createSparkplugBPayload();
        } catch (IOException | SparkplugInvalidTypeException e) {
            e.printStackTrace();
        }

    }

    @Test
    void topicNBIRTH_payloadNotModified() {
        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NBIRTH/edgeItem/node");
        when(clientInformation.getClientId()).thenReturn("orl");
        when(publishOutboundInput.getClientInformation()).thenReturn(clientInformation);
        sparkplugPublishOutboundInterceptor.onOutboundPublish(publishOutboundInput, publishOutboundOutput);
        verify(publishPacket, times(0)).setPayload(any());
    }

    @Test
    void topicNDEATH_payloadModified() {
        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NDEATH/edgeItem/node");
        when(modifiableOutboundPublish.getPayload()).thenReturn(Optional.of(ByteBuffer.wrap(encodedSparkplugPayload)));
        when(publishOutboundOutput.getPublishPacket()).thenReturn(modifiableOutboundPublish);
        when(clientInformation.getClientId()).thenReturn("42");
        when(publishOutboundInput.getClientInformation()).thenReturn(clientInformation);
        sparkplugPublishOutboundInterceptor.onOutboundPublish(publishOutboundInput, publishOutboundOutput);
        verify(modifiableOutboundPublish, times(1)).setPayload(any());
    }

    private byte[] createSparkplugBPayload() throws IOException, SparkplugInvalidTypeException {
        // Add a 'real time' metric
        metrics.add(new Metric.MetricBuilder("a metric", Int32, 42)
                .timestamp(new Date())
                .createMetric());
        SparkplugBPayload sparkplugBPayload = new SparkplugBPayload(new Date(), metrics, 1L, "alf", null);
        return new SparkplugBPayloadEncoder().getBytes(sparkplugBPayload,false);
    }

}