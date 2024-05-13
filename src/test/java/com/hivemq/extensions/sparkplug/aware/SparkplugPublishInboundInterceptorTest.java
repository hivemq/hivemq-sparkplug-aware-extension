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
import com.hivemq.extension.sdk.api.packets.publish.ModifiablePublishPacket;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anja Helmbrecht-Schaar
 */
class SparkplugPublishInboundInterceptorTest {

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
        final SparkplugPublishInboundInterceptor sparkplugPublishInboundInterceptor = new SparkplugPublishInboundInterceptor(configuration, publishService);

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NBIRTH/edgeItem/node");
        when(publishInboundInput.getClientInformation()).thenReturn(clientInformation);
        when(clientInformation.getClientId()).thenReturn("alf");
        //TODO - test
        assertEquals("$sparkplug/certificates/spBv1.0/group/NBIRTH/edgeItem/node", target);
    }

    @Test
    void metric2TopicSparkplug_published() throws IOException {
        final SparkplugConfiguration configuration = getSparkplugConfiguration(List.of("sparkplug.version:spBv1.0", "sparkplug.metrics2topic:true"));
        final SparkplugPublishInboundInterceptor sparkplugPublishInboundInterceptor = new SparkplugPublishInboundInterceptor(configuration, publishService);

        publishInboundInput = mock(PublishInboundInput.class);
        publishInboundOutput = mock(PublishInboundOutput.class);

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/DDATA/edgeItem/node");
        when(publishInboundInput.getPublishPacket()).thenReturn(publishPacket);
        when(publishInboundInput.getClientInformation()).thenReturn(clientInformation);
        when(clientInformation.getClientId()).thenReturn("alf");

        sparkplugPublishInboundInterceptor.onInboundPublish(publishInboundInput, publishInboundOutput);
    }

    private SparkplugConfiguration getSparkplugConfiguration(final List<String> properties) throws IOException {
        Files.write(file, properties);

        final SparkplugConfiguration configuration = new SparkplugConfiguration(file.getParent().toFile());
        configuration.readPropertiesFromFile();

        return configuration;
    }
}