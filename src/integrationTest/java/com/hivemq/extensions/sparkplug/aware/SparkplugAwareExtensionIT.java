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

import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import io.github.sgtsilvio.gradle.oci.junit.jupiter.OciImages;
import org.eclipse.tahu.message.SparkplugBPayloadDecoder;
import org.eclipse.tahu.message.SparkplugBPayloadEncoder;
import org.eclipse.tahu.message.model.SparkplugBPayload.SparkplugBPayloadBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Sparkplug Aware Extension.
 * <p>
 * Tests verify that NBIRTH and DBIRTH messages are republished to the system topic
 * {@code $sparkplug/certificates/...} with the retain flag set.
 */
@Testcontainers
class SparkplugAwareExtensionIT {

    private static final @NotNull String SPARKPLUG_VERSION = "spBv1.0";
    private static final @NotNull String GROUP_ID = "testGroup";
    private static final @NotNull String EDGE_NODE_ID = "testEdgeNode";
    private static final @NotNull String DEVICE_ID = "testDevice";

    @Container
    private final @NotNull HiveMQContainer hivemq =
            new HiveMQContainer(OciImages.getImageName("hivemq/extensions/hivemq-sparkplug-aware-extension")
                    .asCompatibleSubstituteFor("hivemq/hivemq4")) //
                    .withHiveMQConfig(MountableFile.forClasspathResource("config.xml"))
                    .withLogConsumer(outputFrame -> System.out.print("HiveMQ: " + outputFrame.getUtf8String()))
                    .withEnv("HIVEMQ_DISABLE_STATISTICS", "true");

    private Mqtt5BlockingClient publisherClient;
    private Mqtt5BlockingClient subscriberClient;

    @BeforeEach
    void setUp() {
        publisherClient = Mqtt5Client.builder()
                .serverHost(hivemq.getHost())
                .serverPort(hivemq.getMappedPort(1883))
                .identifier("publisher")
                .buildBlocking();

        subscriberClient = Mqtt5Client.builder()
                .serverHost(hivemq.getHost())
                .serverPort(hivemq.getMappedPort(1883))
                .identifier("subscriber")
                .buildBlocking();

        publisherClient.connect();
        subscriberClient.connect();
    }

    @AfterEach
    void tearDown() {
        if (publisherClient != null) {
            publisherClient.disconnect();
        }
        if (subscriberClient != null) {
            subscriberClient.disconnect();
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void test_nbirth_republished_to_systopic() throws Exception {
        // the extension should republish NBIRTH messages to $sparkplug/certificates/...
        final var nbirthTopic = SPARKPLUG_VERSION + "/" + GROUP_ID + "/NBIRTH/" + EDGE_NODE_ID;
        final var expectedSysTopic = "$sparkplug/certificates/" + nbirthTopic;

        // subscribe to the system topic where the extension should republish
        final var receivedMessage = new CompletableFuture<byte[]>();
        subscriberClient.toAsync().subscribeWith().topicFilter(expectedSysTopic).callback(publish -> {
            if (publish.getPayload().isPresent()) {
                final var payload = new byte[publish.getPayload().get().remaining()];
                publish.getPayload().get().get(payload);
                receivedMessage.complete(payload);
            }
        }).send().get();

        // create and publish an NBIRTH message
        final var timestamp = System.currentTimeMillis();
        final var payload = new SparkplugBPayloadBuilder().setTimestamp(new Date(timestamp)).setSeq(0L).createPayload();
        final var encodedPayload = new SparkplugBPayloadEncoder().getBytes(payload, false);

        publisherClient.publishWith().topic(nbirthTopic).payload(encodedPayload).send();

        // wait for the republished message on the system topic
        final var republishedPayload = receivedMessage.get(30, TimeUnit.SECONDS);

        // verify the payload can be decoded and matches
        final var decodedPayload = new SparkplugBPayloadDecoder().buildFromByteArray(republishedPayload, null);
        assertThat(decodedPayload.getTimestamp().getTime()).isEqualTo(timestamp);
        assertThat(decodedPayload.getSeq()).isEqualTo(0L);
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void test_dbirth_republished_to_systopic() throws Exception {
        // the extension should republish DBIRTH messages to $sparkplug/certificates/...
        final var dbirthTopic = SPARKPLUG_VERSION + "/" + GROUP_ID + "/DBIRTH/" + EDGE_NODE_ID + "/" + DEVICE_ID;
        final var expectedSysTopic = "$sparkplug/certificates/" + dbirthTopic;

        // subscribe to the system topic where the extension should republish
        final var receivedMessage = new CompletableFuture<byte[]>();
        subscriberClient.toAsync().subscribeWith().topicFilter(expectedSysTopic).callback(publish -> {
            if (publish.getPayload().isPresent()) {
                final var payload = new byte[publish.getPayload().get().remaining()];
                publish.getPayload().get().get(payload);
                receivedMessage.complete(payload);
            }
        }).send().get();

        // create and publish a DBIRTH message
        final var timestamp = System.currentTimeMillis();
        final var payload = new SparkplugBPayloadBuilder().setTimestamp(new Date(timestamp)).setSeq(0L).createPayload();
        final var encodedPayload = new SparkplugBPayloadEncoder().getBytes(payload, false);

        publisherClient.publishWith().topic(dbirthTopic).payload(encodedPayload).send();

        // wait for the republished message on the system topic
        final var republishedPayload = receivedMessage.get(30, TimeUnit.SECONDS);

        // verify the payload can be decoded and matches
        final var decodedPayload = new SparkplugBPayloadDecoder().buildFromByteArray(republishedPayload, null);
        assertThat(decodedPayload.getTimestamp().getTime()).isEqualTo(timestamp);
        assertThat(decodedPayload.getSeq()).isEqualTo(0L);
    }
}
