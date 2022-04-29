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
package com.hivemq.extensions.helloworld;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.ModifiablePublishPacket;
import com.hivemq.extensions.sparkplug.SparkplugPublishInterceptor;
import com.hivemq.extensions.sparkplug.configuration.SparkplugConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Yannick Weber
 */
class SparkplugPublishInterceptorTest {

    private @NotNull SparkplugPublishInterceptor helloWorldInterceptor;
    private @NotNull PublishInboundInput publishInboundInput;
    private @NotNull PublishInboundOutput publishInboundOutput;
    private @NotNull ModifiablePublishPacket publishPacket;
    private @NotNull Path file;

    @BeforeEach
    void setUp(final @TempDir @NotNull Path tempDir) {
        file = tempDir.resolve("sparkplug.properties");
        SparkplugConfiguration configuration = new SparkplugConfiguration(file.toFile());
        helloWorldInterceptor = new SparkplugPublishInterceptor(configuration);
        publishInboundInput = mock(PublishInboundInput.class);
        publishInboundOutput = mock(PublishInboundOutput.class);
        publishPacket = mock(ModifiablePublishPacket.class);
        when(publishInboundOutput.getPublishPacket()).thenReturn(publishPacket);
    }

    @Test
    void topicSparkplug_published() throws IOException {
        Files.write(file, List.of("sparkplug.version:spBv1.0"));
        when(publishPacket.getTopic()).thenReturn("hello/world");
        helloWorldInterceptor.onInboundPublish(publishInboundInput, publishInboundOutput);
        final ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(publishPacket).setPayload(captor.capture());
        assertEquals("Hello World!", new String(captor.getValue().array(), StandardCharsets.UTF_8));
    }

    @Test
    void topicNotHelloWorld_payloadNotModified() {
        when(publishPacket.getTopic()).thenReturn("some/topic");
        helloWorldInterceptor.onInboundPublish(publishInboundInput, publishInboundOutput);
        verify(publishPacket, times(0)).setPayload(any());
    }
}