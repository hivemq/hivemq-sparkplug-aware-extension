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
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.ModifiablePublishPacket;
import com.hivemq.extension.sdk.api.services.builder.PublishBuilder;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import org.eclipse.tahu.SparkplugInvalidTypeException;
import org.eclipse.tahu.message.SparkplugBPayloadEncoder;
import org.eclipse.tahu.message.model.Metric;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.eclipse.tahu.message.model.MetricDataType.Int32;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Anja Helmbrecht-Schaar
 */
class SparkplugPublishInterceptorTest {

    private @NotNull SparkplugPublishInterceptor sparkplugPublishInterceptor;
    private @NotNull PublishInboundInput publishInboundInput;
    private @NotNull PublishInboundOutput publishInboundOutput;
    private @NotNull ModifiablePublishPacket publishPacket;
    private @NotNull PublishBuilder publishBuilder;
    private @NotNull Path file;
    List<Metric> metrics = new ArrayList<Metric>();
    byte[] encodedSparkplugPayload;

    @BeforeEach
    void setUp(final @TempDir @NotNull Path tempDir) {
        file = tempDir.resolve("sparkplug.properties");
        SparkplugConfiguration configuration = new SparkplugConfiguration(file.toFile());
        final @NotNull PublishService publishService = mock(PublishService.class);
        publishBuilder = mock(PublishBuilder.class);
        sparkplugPublishInterceptor = new SparkplugPublishInterceptor(configuration, publishService, publishBuilder);
        publishInboundInput = mock(PublishInboundInput.class);
        publishInboundOutput = mock(PublishInboundOutput.class);
        publishPacket = mock(ModifiablePublishPacket.class);
        when(publishInboundOutput.getPublishPacket()).thenReturn(publishPacket);

        try {
            encodedSparkplugPayload = createSparkplugBPayload();
        } catch (IOException | SparkplugInvalidTypeException e) {
            e.printStackTrace();
        }

    }

    @Test
    void topicSparkplug_published() throws IOException {
        Files.write(file, List.of("sparkplug.version:spBv1.0"));
        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NBIRTH/node/item");
        sparkplugPublishInterceptor.onInboundPublish(publishInboundInput, publishInboundOutput);
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(publishBuilder).topic(captor.capture());
        verify(publishBuilder).retain(true);
        assertEquals("$sparkplug/certificates/spBv1.0/group/NBIRTH/node/item", captor.getValue());
    }

    @Test
    void topicNBIRTH_payloadNotModified() {
        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NBIRTH/item");
        sparkplugPublishInterceptor.onInboundPublish(publishInboundInput, publishInboundOutput);
        verify(publishPacket, times(0)).setPayload(any());
    }

    @Test
    void topicNDEATH_payloadModified() {
        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NDEATH/edgeItem");
        when(publishPacket.getPayload()).thenReturn(Optional.of(ByteBuffer.wrap(encodedSparkplugPayload)));
        sparkplugPublishInterceptor.onInboundPublish(publishInboundInput, publishInboundOutput);
        verify(publishPacket, times(1)).setPayload(any());
    }

    private byte[] createSparkplugBPayload() throws IOException, SparkplugInvalidTypeException {
        // Add a 'real time' metric
        metrics.add(new Metric.MetricBuilder("a metric", Int32, 42)
                .timestamp(new Date())
                .createMetric());
        SparkplugBPayload sparkplugBPayload = new SparkplugBPayload( new Date(), metrics, 1, null, null);
        return new SparkplugBPayloadEncoder().getBytes(sparkplugBPayload);
    }
}