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
import com.hivemq.extension.sdk.api.packets.publish.ModifiablePublishPacket;
import com.hivemq.extension.sdk.api.services.builder.PublishBuilder;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anja Helmbrecht-Schaar
 */
class SparkplugPublishInboundInterceptorTest {

    //@formatter:off
    private final byte @NotNull [] DDATA_PUBLISH_PAYLOAD = {
            8, -25, -34, -59, -70, -9, 49, 18, 12, 10, 6, 87, 101, 105, 103, 104, 116, 32, 3, 80, 7, 18, 19, 10, 13, 65,
            103, 105, 116, 97, 116, 111, 114, 83, 112, 101, 101, 100, 32, 3, 80, 2, 18, 19, 10, 6, 83, 116, 97, 116, 117,
            115, 32, 12, 122, 7, 82, 117, 110, 110, 105, 110, 103, 18, 24, 10, 11, 84, 101, 109, 112, 101, 114, 97, 116,
            117, 114, 101, 32, 10, 105, -113, -1, -35, 93, 108, 44, -95, 63, 18, 21, 10, 8, 80, 114, 101, 115, 115, 117,
            114, 101, 32, 10, 105, 126, 24, 3, -13, 108, 11, -82, 63, 24, 4
    };
    //@formatter:on

    private final @NotNull PublishInboundInput publishInboundInput = mock();
    private final @NotNull PublishInboundOutput publishInboundOutput = mock();
    private final @NotNull ModifiablePublishPacket publishPacket = mock();
    private final @NotNull ClientInformation clientInformation = mock();
    private final @NotNull PublishService publishService = mock();

    private @NotNull Path file;

    @TempDir
    private @NotNull Path tempDir;

    @BeforeEach
    void setUp() {
        when(publishInboundOutput.getPublishPacket()).thenReturn(publishPacket);

        file = tempDir.resolve("sparkplug.properties");
    }

    @Test
    void topicSparkplug_published() throws Exception {
        final var configuration = getSparkplugConfiguration(List.of("sparkplug.version:spBv1.0"));
        final var publishBuilder = mock(PublishBuilder.class);
        final var sparkplugPublishInboundInterceptor =
                new SparkplugPublishInboundInterceptor(configuration, publishService, publishBuilder);

        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NBIRTH/edgeItem/node");
        when(publishInboundInput.getClientInformation()).thenReturn(clientInformation);
        when(clientInformation.getClientId()).thenReturn("alf");
    }

    private SparkplugConfiguration getSparkplugConfiguration(final @NotNull List<String> properties) throws Exception {
        Files.write(file, properties);
        final var configuration = new SparkplugConfiguration(file.getParent().toFile());
        configuration.readPropertiesFromFile();
        return configuration;
    }
}
