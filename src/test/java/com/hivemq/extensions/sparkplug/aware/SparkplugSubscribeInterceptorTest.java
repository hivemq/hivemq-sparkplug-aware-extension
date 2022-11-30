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
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundOutput;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscribePacket;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscription;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.eclipse.tahu.message.model.MetricDataType.Int32;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Anja Helmbrecht-Schaar
 */
class SparkplugSubscribeInterceptorTest {

    List<Metric> metrics = new ArrayList<Metric>();
    byte[] encodedSparkplugPayload;
    private @NotNull SparkplugSubscribeInterceptor sparkplugSubscribeInterceptor;
    private @NotNull SubscribeInboundInput subscribeInboundInput;
    private @NotNull SubscribeInboundOutput subscribeInboundOutput;
    private @NotNull ModifiableSubscription subscription;
    private @NotNull ModifiableSubscribePacket subscribePacket;
    private @NotNull Path file;
    private @NotNull ClientInformation clientInformation;

    @BeforeEach
    void setUp(final @TempDir @NotNull Path tempDir) {
        file = tempDir.resolve("sparkplug.properties");
        SparkplugConfiguration configuration = new SparkplugConfiguration(file.toFile());

        sparkplugSubscribeInterceptor = new SparkplugSubscribeInterceptor(configuration);
        subscribeInboundInput = mock(SubscribeInboundInput.class);
        subscribeInboundOutput = mock(SubscribeInboundOutput.class);
        subscribePacket = mock(ModifiableSubscribePacket.class);
        subscription = mock(ModifiableSubscription.class);
        clientInformation = mock(ClientInformation.class);


        try {
            encodedSparkplugPayload = createSparkplugBPayload();
        } catch (IOException | SparkplugInvalidTypeException e) {
            e.printStackTrace();
        }

    }

    @Test
    void onInboundSubscribeTest() throws IOException {
        Files.write(file, List.of("sparkplug.version:spBv1.0"));
        when(subscription.getTopicFilter()).thenReturn("$sparkplug/certificates/spBv1.0/group/NBIRTH/node/item");

        List<ModifiableSubscription> subscriptions = new ArrayList<>();
        subscriptions.add(subscription);
        when(subscribePacket.getSubscriptions()).thenReturn(subscriptions);
        when(subscribeInboundOutput.getSubscribePacket()).thenReturn(subscribePacket);

        when(clientInformation.getClientId()).thenReturn("42");
        when(subscribeInboundInput.getClientInformation()).thenReturn(clientInformation);

        sparkplugSubscribeInterceptor.onInboundSubscribe(subscribeInboundInput, subscribeInboundOutput);
        final ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
        verify(subscription).setRetainAsPublished(captor.capture());
        assertEquals(true, captor.getValue());
    }


    private byte[] createSparkplugBPayload() throws IOException, SparkplugInvalidTypeException {
        // Add a 'real time' metric
        metrics.add(new Metric.MetricBuilder("a metric", Int32, 42)
                .timestamp(new Date())
                .createMetric());
        SparkplugBPayload sparkplugBPayload = new SparkplugBPayload(new Date(), metrics, 1L, null, null);
        return new SparkplugBPayloadEncoder().getBytes(sparkplugBPayload, false);
    }

}