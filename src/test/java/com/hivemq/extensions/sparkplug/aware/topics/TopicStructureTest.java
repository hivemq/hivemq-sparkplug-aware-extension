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
package com.hivemq.extensions.sparkplug.aware.topics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TopicStructureTest {

    private static final String SPARKPLUG_VERSION = "spBv1.0";

    @Test
    void constructor_withValidNodeBirthTopic_parsesCorrectly() {
        final var topic = "spBv1.0/group1/NBIRTH/edge1";

        final var topicStructure = new TopicStructure(topic);

        assertThat(topicStructure.getNamespace()).isEqualTo("spBv1.0");
        assertThat(topicStructure.getMessageType()).isEqualTo(MessageType.NBIRTH);
        assertThat(topicStructure.getEonId()).isEqualTo("edge1");
        assertThat(topicStructure.getDeviceId()).isNull();
        assertThat(topicStructure.getScadaId()).isNull();
    }

    @Test
    void constructor_withValidDeviceBirthTopic_parsesCorrectly() {
        final var topic = "spBv1.0/group1/DBIRTH/edge1/device1";

        final var topicStructure = new TopicStructure(topic);

        assertThat(topicStructure.getNamespace()).isEqualTo("spBv1.0");
        assertThat(topicStructure.getMessageType()).isEqualTo(MessageType.DBIRTH);
        assertThat(topicStructure.getEonId()).isEqualTo("edge1");
        assertThat(topicStructure.getDeviceId()).isEqualTo("device1");
        assertThat(topicStructure.getScadaId()).isNull();
    }

    @Test
    void constructor_withValidStateTopic_parsesCorrectly() {
        final var topic = "spBv1.0/group1/STATE/scada1";

        final var topicStructure = new TopicStructure(topic);

        assertThat(topicStructure.getNamespace()).isEqualTo("spBv1.0");
        assertThat(topicStructure.getMessageType()).isEqualTo(MessageType.STATE);
        assertThat(topicStructure.getScadaId()).isEqualTo("scada1");
        assertThat(topicStructure.getEonId()).isNull();
        assertThat(topicStructure.getDeviceId()).isNull();
    }

    @ParameterizedTest
    @MethodSource("provideValidTopics")
    void constructor_withVariousValidTopics_parsesCorrectly(
            final String topic,
            final String expectedNamespace,
            final MessageType expectedMessageType,
            final String expectedEonId,
            final String expectedDeviceId,
            final String expectedScadaId) {
        final var topicStructure = new TopicStructure(topic);

        assertThat(topicStructure.getNamespace()).isEqualTo(expectedNamespace);
        assertThat(topicStructure.getMessageType()).isEqualTo(expectedMessageType);
        assertThat(topicStructure.getEonId()).isEqualTo(expectedEonId);
        assertThat(topicStructure.getDeviceId()).isEqualTo(expectedDeviceId);
        assertThat(topicStructure.getScadaId()).isEqualTo(expectedScadaId);
    }

    private static Stream<Arguments> provideValidTopics() {
        return Stream.of(
                // Node messages (4 levels)
                arguments("spBv1.0/group1/NBIRTH/edge1", "spBv1.0", MessageType.NBIRTH, "edge1", null, null),
                arguments("spBv1.0/group1/NDEATH/edge1", "spBv1.0", MessageType.NDEATH, "edge1", null, null),
                arguments("spBv1.0/group1/NDATA/edge1", "spBv1.0", MessageType.NDATA, "edge1", null, null),
                arguments("spBv1.0/group1/NCMD/edge1", "spBv1.0", MessageType.NCMD, "edge1", null, null),
                // Device messages (5 levels)
                arguments("spBv1.0/group1/DBIRTH/edge1/device1",
                        "spBv1.0",
                        MessageType.DBIRTH,
                        "edge1",
                        "device1",
                        null),
                arguments("spBv1.0/group1/DDEATH/edge1/device1",
                        "spBv1.0",
                        MessageType.DDEATH,
                        "edge1",
                        "device1",
                        null),
                arguments("spBv1.0/group1/DDATA/edge1/device1", "spBv1.0", MessageType.DDATA, "edge1", "device1", null),
                arguments("spBv1.0/group1/DCMD/edge1/device1", "spBv1.0", MessageType.DCMD, "edge1", "device1", null),
                // STATE messages
                arguments("spBv1.0/group1/STATE/scada1", "spBv1.0", MessageType.STATE, null, null, "scada1"),
                // Different group IDs
                arguments("spBv1.0/myGroup/NBIRTH/edge1", "spBv1.0", MessageType.NBIRTH, "edge1", null, null),
                arguments("spBv1.0/group-123/DBIRTH/edge1/device1",
                        "spBv1.0",
                        MessageType.DBIRTH,
                        "edge1",
                        "device1",
                        null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "topic",                    // 1 level
            "topic/level2",             // 2 levels
            "topic/level2/level3"       // 3 levels
    })
    void constructor_withInsufficientLevels_setsDefaultValues(final String topic) {
        final var topicStructure = new TopicStructure(topic);

        assertThat(topicStructure.getNamespace()).isEmpty();
        assertThat(topicStructure.getMessageType()).isEqualTo(MessageType.UNKNOWN);
        assertThat(topicStructure.getEonId()).isNull();
        assertThat(topicStructure.getDeviceId()).isNull();
        assertThat(topicStructure.getScadaId()).isNull();
        assertThat(topicStructure.isValid(SPARKPLUG_VERSION)).isFalse();
    }

    @Test
    void constructor_withEmptyTopic_setsDefaultValues() {
        final var topicStructure = new TopicStructure("");

        assertThat(topicStructure.getNamespace()).isEmpty();
        assertThat(topicStructure.getMessageType()).isEqualTo(MessageType.UNKNOWN);
        assertThat(topicStructure.getEonId()).isNull();
        assertThat(topicStructure.getDeviceId()).isNull();
        assertThat(topicStructure.getScadaId()).isNull();
        assertThat(topicStructure.isValid(SPARKPLUG_VERSION)).isFalse();
    }

    @Test
    void isValid_withValidNodeTopic_returnsTrue() {
        final var topicStructure = new TopicStructure("spBv1.0/group1/NBIRTH/edge1");

        assertThat(topicStructure.isValid(SPARKPLUG_VERSION)).isTrue();
    }

    @Test
    void isValid_withValidDeviceTopic_returnsTrue() {
        final var topicStructure = new TopicStructure("spBv1.0/group1/DBIRTH/edge1/device1");

        assertThat(topicStructure.isValid(SPARKPLUG_VERSION)).isTrue();
    }

    @Test
    void isValid_withValidStateTopic_returnsTrue() {
        final var topicStructure = new TopicStructure("spBv1.0/group1/STATE/scada1");

        assertThat(topicStructure.isValid(SPARKPLUG_VERSION)).isTrue();
    }

    @Test
    void isValid_withInvalidNamespace_returnsFalse() {
        final var topicStructure = new TopicStructure("invalidVersion/group1/NBIRTH/edge1");

        assertThat(topicStructure.isValid(SPARKPLUG_VERSION)).isFalse();
    }

    @Test
    void isValid_withUnknownMessageType_returnsFalse() {
        final var topicStructure = new TopicStructure("spBv1.0/group1/INVALID/edge1");

        assertThat(topicStructure.isValid(SPARKPLUG_VERSION)).isFalse();
    }

    @Test
    void isValid_withDifferentSparkplugVersion_acceptsValidVersionPattern() {
        final var topicStructure = new TopicStructure("spBv2.0/group1/NBIRTH/edge1");

        assertThat(topicStructure.isValid("spBv2.0")).isTrue();
        assertThat(topicStructure.isValid(SPARKPLUG_VERSION)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("provideAllMessageTypes")
    void constructor_withAllMessageTypes_parsesCorrectly(final String messageType, final MessageType expected) {
        final var topic = "spBv1.0/group1/" + messageType + "/edge1";

        final var topicStructure = new TopicStructure(topic);

        assertThat(topicStructure.getMessageType()).isEqualTo(expected);
    }

    private static Stream<Arguments> provideAllMessageTypes() {
        return Stream.of(arguments("NBIRTH", MessageType.NBIRTH),
                arguments("NDEATH", MessageType.NDEATH),
                arguments("NDATA", MessageType.NDATA),
                arguments("NCMD", MessageType.NCMD),
                arguments("DBIRTH", MessageType.DBIRTH),
                arguments("DDEATH", MessageType.DDEATH),
                arguments("DDATA", MessageType.DDATA),
                arguments("DCMD", MessageType.DCMD),
                arguments("STATE", MessageType.STATE),
                arguments("INVALID", MessageType.UNKNOWN));
    }

    @Test
    void toString_containsAllFields() {
        final var topicStructure = new TopicStructure("spBv1.0/group1/DBIRTH/edge1/device1");

        final var result = topicStructure.toString();

        assertThat(result).contains("namespace='spBv1.0'");
        assertThat(result).contains("groupId='group1'");
        assertThat(result).contains("messageType='DBIRTH'");
        assertThat(result).contains("eonId='edge1'");
        assertThat(result).contains("deviceId='device1'");
        assertThat(result).contains("scadaId='null'");
    }

    @Test
    void toString_forStateTopic_showsScadaId() {
        final var topicStructure = new TopicStructure("spBv1.0/group1/STATE/scada1");

        final var result = topicStructure.toString();

        assertThat(result).contains("scadaId='scada1'");
        assertThat(result).contains("eonId='null'");
    }

    @Test
    void constructor_withSpecialCharactersInIds_parsesCorrectly() {
        final var topic = "spBv1.0/group-123/NBIRTH/edge_node-456";

        final var topicStructure = new TopicStructure(topic);

        assertThat(topicStructure.getEonId()).isEqualTo("edge_node-456");
    }

    @Test
    void constructor_withExtraLevels_onlyParsesFirstFive() {
        final var topic = "spBv1.0/group1/DBIRTH/edge1/device1/extra/levels";

        final var topicStructure = new TopicStructure(topic);

        assertThat(topicStructure.getNamespace()).isEqualTo("spBv1.0");
        assertThat(topicStructure.getMessageType()).isEqualTo(MessageType.DBIRTH);
        assertThat(topicStructure.getEonId()).isEqualTo("edge1");
        assertThat(topicStructure.getDeviceId()).isEqualTo("device1");
        // Extra levels are not captured by the structure
    }

    @Test
    void isValid_withAllValidComponents_returnsTrue() {
        final var topicStructure = new TopicStructure("spBv1.0/group1/NBIRTH/edge1");

        final var result = topicStructure.isValid(SPARKPLUG_VERSION);

        assertThat(result).isTrue();
        assertThat(topicStructure.getMessageType()).isNotEqualTo(MessageType.UNKNOWN);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "spBv1.0/group1/NBIRTH/edge1",
            "spBv1.0/group1/DBIRTH/edge1/device1",
            "spBv1.0/group1/NDATA/edge1",
            "spBv1.0/group1/DDATA/edge1/device1",
            "spBv1.0/group1/STATE/scada1"})
    void isValid_withVariousValidTopics_returnsTrue(final String topic) {
        final var topicStructure = new TopicStructure(topic);

        assertThat(topicStructure.isValid(SPARKPLUG_VERSION)).isTrue();
    }
}
