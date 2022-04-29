/*
 * Copyright 2021-present HiveMQ GmbH
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
package com.hivemq.extensions.sparkplug.topics;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

/**
 * Topic structure meta object to create sparkplug structure and
 * validate the incoming topic parts against sparkplug topic specification
 *
 * @author Anja Helmbrecht-Schaar
 */
public class TopicStructure {

    private final int topicLevels;
    private @NotNull String namespace;
    private @NotNull String groupId;
    private @NotNull MessageType messageType;
    private @Nullable String eonId;
    private @Nullable String scadaId;
    private @Nullable String deviceId;

    public TopicStructure(final @NotNull String topic) {
        final String[] arr = topic.split("/");
        topicLevels = arr.length;
        if (topicLevels >= 4) {
            namespace = arr[0];
            groupId = arr[1];
            messageType = MessageType.fromString(arr[2]);
            if (MessageType.STATE == messageType) {
                scadaId = arr[3];
            } else {
                eonId = arr[3];
            }
            if (topicLevels > 4) {
                deviceId = arr[4];
            }
        }
    }

    private boolean isValidNamespace(final @NotNull String sparkplugVersion) {
        return sparkplugVersion.matches(namespace);
    }

    private boolean isValidMessageType() {
        return (messageType != MessageType.UNKNOWN);
    }

    public @NotNull String getNamespace() {
        return namespace;
    }

    public @NotNull MessageType getMessageType() {
        return messageType;
    }

    public @Nullable String getEonId() {
        return eonId;
    }

    public @Nullable String getScadaId() {
        return scadaId;
    }

    public @Nullable String getDeviceId() {
        return deviceId;
    }

    public boolean isValid(final @NotNull String sparkplugVersion) {
        return topicLevels > 3
                && isValidNamespace(sparkplugVersion)
                && isValidMessageType()
                && (scadaId != null || eonId != null);
    }

    @Override
    public @NotNull String toString() {
        return "TopicStructure{" +
                "namespace='" + namespace + '\'' +
                ", groupId='" + groupId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", eonId='" + eonId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", scadaId='" + scadaId + '\'' +
                '}';
    }
}