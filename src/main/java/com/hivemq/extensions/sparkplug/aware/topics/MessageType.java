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

/**
 * Enumeration of Sparkplug message types as defined in the Sparkplug specification.
 * <p>
 * Sparkplug defines specific message types for lifecycle management, data transmission,
 * and command execution within an Industrial IoT infrastructure:
 * <ul>
 *     <li><b>BIRTH messages</b> - Announce availability and publish metrics/metadata</li>
 *     <li><b>DEATH messages</b> - Announce disconnection or unavailability</li>
 *     <li><b>DATA messages</b> - Publish metric data updates</li>
 *     <li><b>CMD messages</b> - Receive commands from SCADA host applications</li>
 *     <li><b>STATE messages</b> - SCADA host availability status</li>
 * </ul>
 * <p>
 * Message types are prefixed with:
 * <ul>
 *     <li><b>N</b> - Node (Edge of Network node)</li>
 *     <li><b>D</b> - Device (under an edge node)</li>
 * </ul>
 *
 * @author David Sondermann
 */
public enum MessageType {
    /**
     * Device BIRTH certificate message - announces device availability and publishes its metrics.
     */
    DBIRTH,
    /**
     * Device DEATH certificate message - announces device disconnection or unavailability.
     */
    DDEATH,
    /**
     * Node BIRTH certificate message - announces edge node availability and publishes its metrics.
     */
    NBIRTH,
    /**
     * Node DEATH certificate message - announces edge node disconnection or unavailability.
     */
    NDEATH,
    /**
     * Device DATA message - publishes metric data updates from a device.
     */
    DDATA,
    /**
     * Node DATA message - publishes metric data updates from an edge node.
     */
    NDATA,
    /**
     * Device COMMAND message - receives commands from a SCADA host application for a device.
     */
    DCMD,
    /**
     * Node COMMAND message - receives commands from a SCADA host application for an edge node.
     */
    NCMD,
    /**
     * STATE message - indicates the online/offline state of a SCADA host application.
     */
    STATE,
    /**
     * UNKNOWN message type - represents any message type not matching the Sparkplug specification.
     */
    UNKNOWN;

    /**
     * Converts a string representation to a {@link MessageType} enum constant.
     * <p>
     * This method performs a case-sensitive match against the enum constant names.
     * If no match is found, {@link #UNKNOWN} is returned.
     *
     * @param s the string to convert to a message type
     * @return the corresponding {@link MessageType}, or {@link #UNKNOWN} if no match is found
     */
    public static @NotNull MessageType fromString(final @NotNull String s) {
        try {
            return valueOf(s);
        } catch (final IllegalArgumentException | NullPointerException e) {
            return UNKNOWN;
        }
    }
}
