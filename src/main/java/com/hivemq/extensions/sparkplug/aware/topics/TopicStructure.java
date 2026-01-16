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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a parsed Sparkplug topic structure and provides validation against the Sparkplug specification.
 * <p>
 * This class parses MQTT topic strings into their Sparkplug components and validates them according
 * to the Sparkplug topic structure specification:
 * <pre>
 * namespace/group_id/message_type/edge_node_id/[device_id]
 * </pre>
 * or for STATE messages:
 * <pre>
 * namespace/group_id/STATE/scada_host_id
 * </pre>
 * <p>
 * The class extracts and provides access to:
 * <ul>
 *     <li>Namespace - typically the Sparkplug version (e.g., "spBv1.0")</li>
 *     <li>Group ID - logical grouping of edge nodes</li>
 *     <li>Message Type - NBIRTH, DBIRTH, NDEATH, DDEATH, NDATA, DDATA, NCMD, DCMD, or STATE</li>
 *     <li>Edge of Network (EoN) ID - identifier for the edge node</li>
 *     <li>Device ID - optional identifier for devices under an edge node</li>
 *     <li>SCADA ID - identifier for SCADA host (STATE messages only)</li>
 * </ul>
 *
 * @author David Sondermann
 */
public class TopicStructure {

    private final int topicLevels;
    private final @NotNull String namespace;
    private final @NotNull String groupId;
    private final @NotNull MessageType messageType;
    private final @Nullable String scadaId;
    private final @Nullable String eonId;
    private final @Nullable String deviceId;

    public TopicStructure(final @NotNull String topic) {
        final var arr = topic.split("/");
        topicLevels = arr.length;
        if (topicLevels >= 4) {
            namespace = arr[0];
            groupId = arr[1];
            messageType = MessageType.fromString(arr[2]);
            if (MessageType.STATE == messageType) {
                scadaId = arr[3];
                eonId = null;
            } else {
                scadaId = null;
                eonId = arr[3];
            }
            if (topicLevels > 4) {
                deviceId = arr[4];
            } else {
                deviceId = null;
            }
        } else {
            namespace = "";
            groupId = "";
            messageType = MessageType.UNKNOWN;
            scadaId = null;
            eonId = null;
            deviceId = null;
        }
    }

    public @NotNull String getNamespace() {
        return namespace;
    }

    public @NotNull MessageType getMessageType() {
        return messageType;
    }

    public @Nullable String getScadaId() {
        return scadaId;
    }

    public @Nullable String getEonId() {
        return eonId;
    }

    public @Nullable String getDeviceId() {
        return deviceId;
    }

    @Override
    public @NotNull String toString() {
        return "TopicStructure{" +
                "namespace='" +
                namespace +
                "', groupId='" +
                groupId +
                "', messageType='" +
                messageType +
                "', scadaId='" +
                scadaId +
                "', eonId='" +
                eonId +
                "', deviceId='" +
                deviceId +
                "'}";
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isValid(final @NotNull String sparkplugVersion) {
        return topicLevels > 3 &&
                isValidNamespace(sparkplugVersion) &&
                isValidMessageType() &&
                (scadaId != null || eonId != null);
    }

    private boolean isValidNamespace(final @NotNull String sparkplugVersion) {
        return sparkplugVersion.matches(namespace);
    }

    private boolean isValidMessageType() {
        return (messageType != MessageType.UNKNOWN);
    }
}
