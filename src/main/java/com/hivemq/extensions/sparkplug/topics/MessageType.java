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

public enum MessageType {
    /**
     * THE BIRTH certificate message of a device
     */
    DBIRTH,
    /**
     * THE DEATH certificate message of a device
     */
    DDEATH,
    /**
     * THE BIRTH certificate message of an edge node
     */
    NBIRTH,
    /**
     * THE DEATH certificate message of an edge node
     */
    NDEATH,
    /**
     * THE DATA message message from a device
     */
    DDATA,
    /**
     * THE DATA message message from an edge node
     */
    NDATA,
    /**
     * THE Command message message from a SCADA host for a device
     */
    DCMD,
    /**
     * THE Command message message from a SCADA host for an edge node
     */
    NCMD,
    /**
     * THE Command message message from a SCADA host
     */
    STATE,
    /**
     * THE UNKNOWN - if something else was used
     */
    UNKNOWN;

    public static @NotNull MessageType fromString(final @NotNull String s) {
        try {
            return valueOf(s);
        } catch (final IllegalArgumentException | NullPointerException e) {
            return UNKNOWN;
        }
    }
}