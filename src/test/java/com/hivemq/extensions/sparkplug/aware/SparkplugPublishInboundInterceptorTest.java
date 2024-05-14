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
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.ModifiablePublishPacket;
import com.hivemq.extension.sdk.api.services.builder.PublishBuilder;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Anja Helmbrecht-Schaar
 */
class SparkplugPublishInboundInterceptorTest {

    private byte DDATA_PUBLISH_PAYLOAD[] = {8, -25, -34, -59, -70, -9, 49, 18, 12, 10, 6, 87, 101, 105, 103, 104, 116, 32, 3, 80, 7, 18, 19, 10, 13, 65, 103, 105, 116, 97, 116, 111, 114, 83, 112, 101, 101, 100, 32, 3, 80, 2, 18, 19, 10, 6, 83, 116, 97, 116, 117, 115, 32, 12, 122, 7, 82, 117, 110, 110, 105, 110, 103, 18, 24, 10, 11, 84, 101, 109, 112, 101, 114, 97, 116, 117, 114, 101, 32, 10, 105, -113, -1, -35, 93, 108, 44, -95, 63, 18, 21, 10, 8, 80, 114, 101, 115, 115, 117, 114, 101, 32, 10, 105, 126, 24, 3, -13, 108, 11, -82, 63, 24, 4};

    private @NotNull PublishInboundInput publishInboundInput;
    private @NotNull PublishInboundOutput publishInboundOutput;
    private @NotNull ModifiablePublishPacket publishPacket;
    private @NotNull Path file;
    private @NotNull ClientInformation clientInformation;
    private @NotNull PublishService publishService;

    private String target = "$sparkplug/certificates/spBv1.0/group/NBIRTH/edgeItem/node";

    @BeforeEach
    void setUp(final @TempDir @NotNull Path tempDir) {
        file = tempDir.resolve("sparkplug.properties");

        publishService = mock(PublishService.class);
        publishInboundInput = mock(PublishInboundInput.class);
        publishInboundOutput = mock(PublishInboundOutput.class);
        publishPacket = mock(ModifiablePublishPacket.class);
        clientInformation = mock(ClientInformation.class);

        when(publishInboundOutput.getPublishPacket()).thenReturn(publishPacket);
    }

    @Test
    void topicSparkplug_published() throws IOException {
        final SparkplugConfiguration configuration = getSparkplugConfiguration(List.of("sparkplug.version:spBv1.0"));
        final PublishBuilder publishBuilder = mock(PublishBuilder.class);
        final SparkplugPublishInboundInterceptor sparkplugPublishInboundInterceptor = new SparkplugPublishInboundInterceptor(configuration, publishService, publishBuilder);

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NBIRTH/edgeItem/node");
        when(publishInboundInput.getClientInformation()).thenReturn(clientInformation);
        when(clientInformation.getClientId()).thenReturn("alf");
        //TODO - test
        assertEquals("$sparkplug/certificates/spBv1.0/group/NBIRTH/edgeItem/node", target);
    }

    @Test
    void metric2TopicSparkplug_published() throws IOException {
        final SparkplugConfiguration configuration = getSparkplugConfiguration(List.of("sparkplug.version:spBv1.0", "sparkplug.metrics2topic:true"));

        final PublishBuilder publishBuilder = mock(PublishBuilder.class);
        when(publishBuilder.fromPublish(any(Publish.class))).thenReturn(publishBuilder);
        when(publishBuilder.topic(anyString())).thenReturn(publishBuilder);
        when(publishBuilder.qos(any(Qos.class))).thenReturn(publishBuilder);
        when(publishBuilder.payload(any(ByteBuffer.class))).thenReturn(publishBuilder);
        when(publishBuilder.build()).thenReturn(mock(Publish.class));

        final SparkplugPublishInboundInterceptor sparkplugPublishInboundInterceptor = new SparkplugPublishInboundInterceptor(configuration, publishService, publishBuilder);

        publishInboundInput = mock(PublishInboundInput.class);
        publishInboundOutput = mock(PublishInboundOutput.class);

        when(publishPacket.getPayload()).thenReturn(Optional.of(ByteBuffer.wrap(DDATA_PUBLISH_PAYLOAD)));
        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/DDATA/edgeItem/node");
        when(publishInboundInput.getPublishPacket()).thenReturn(publishPacket);
        when(publishInboundInput.getClientInformation()).thenReturn(clientInformation);
        when(clientInformation.getClientId()).thenReturn("alf");
        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/DDATA/edgeItem/node");
        when(publishInboundInput.getPublishPacket()).thenReturn(publishPacket);
        when(publishInboundInput.getClientInformation()).thenReturn(clientInformation);
        when(clientInformation.getClientId()).thenReturn("alf");

        sparkplugPublishInboundInterceptor.onInboundPublish(publishInboundInput, publishInboundOutput);

        verify(publishService, times(4)).publish(any(Publish.class));
    }

    private SparkplugConfiguration getSparkplugConfiguration(final List<String> properties) throws IOException {
        Files.write(file, properties);

        final SparkplugConfiguration configuration = new SparkplugConfiguration(file.getParent().toFile());
        configuration.readPropertiesFromFile();

        return configuration;
    }
}