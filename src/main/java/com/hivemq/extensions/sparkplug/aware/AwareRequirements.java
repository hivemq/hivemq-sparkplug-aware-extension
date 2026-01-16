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

import org.jetbrains.annotations.NotNull;

/**
 * Defines conformance requirements for Sparkplug Aware MQTT Servers.
 * <p>
 * This class contains string constants that describe the mandatory and optional behaviors
 * that a Sparkplug Aware MQTT Server must implement according to the Sparkplug specification.
 * These requirements cover:
 * <ul>
 *     <li>Publishing NBIRTH and DBIRTH messages to system topics with retained flag</li>
 *     <li>Modifying NDEATH message timestamps to reflect actual disconnection time</li>
 * </ul>
 * <p>
 * The constants are used for logging, documentation, and verification purposes to ensure
 * the extension complies with the official Sparkplug specification requirements.
 * <p>
 * This is a utility class with package-private constructor to prevent external instantiation.
 */
final class AwareRequirements {

    AwareRequirements() {
    }

    /**
     * Conformance requirement for NBIRTH message topic structure.
     * <p>
     * Specifies that a Sparkplug Aware MQTT Server MUST publish NBIRTH messages
     * to system topics following the format:
     * {@code $sparkplug/certificates/namespace/group_id/NBIRTH/edge_node_id}
     */
    static final @NotNull String CONFORMANCE_MQTT_AWARE_NBIRTH_MQTT_TOPIC =
            "A Sparkplug Aware MQTT Server MUST make NBIRTH messages available on a topic of the form: $sparkplug/certificates/namespace/group_id/NBIRTH/edge_node_id";

    /**
     * Conformance requirement for NBIRTH message retained flag.
     * <p>
     * Specifies that a Sparkplug Aware MQTT Server MUST publish NBIRTH messages
     * to system topics with the MQTT retained flag set to {@code true}, ensuring
     * new subscribers receive the latest birth certificate immediately.
     */
    static final @NotNull String CONFORMANCE_MQTT_AWARE_NBIRTH_MQTT_RETAIN =
            "A Sparkplug Aware MQTT Server MUST make NBIRTH messages available on the topic: $sparkplug/certificates/namespace/group_id/NBIRTH/edge_node_id with the MQTT retain flag set to true";

    /**
     * Conformance requirement for DBIRTH message topic structure.
     * <p>
     * Specifies that a Sparkplug Aware MQTT Server MUST publish DBIRTH messages
     * to system topics following the format:
     * {@code $sparkplug/certificates/namespace/group_id/DBIRTH/edge_node_id/device_id}
     */
    static final @NotNull String CONFORMANCE_MQTT_AWARE_DBIRTH_MQTT_TOPIC =
            "A Sparkplug Aware MQTT Server MUST make DBIRTH messages available on a topic of the form: $sparkplug/certificates/namespace/group_id/DBIRTH/edge_node_id/device_id";

    /**
     * Conformance requirement for DBIRTH message retained flag.
     * <p>
     * Specifies that a Sparkplug Aware MQTT Server MUST publish DBIRTH messages
     * to system topics with the MQTT retained flag set to {@code true}, ensuring
     * new subscribers receive the latest device birth certificate immediately.
     */
    static final @NotNull String CONFORMANCE_MQTT_AWARE_DBIRTH_MQTT_RETAIN =
            "A Sparkplug Aware MQTT Server MUST make DBIRTH messages available on the topic: $sparkplug/certificates/namespace/group_id/DBIRTH/edge_node_id/device_id with the MQTT retain flag set to true";

    /**
     * Conformance requirement for NDEATH message timestamp modification.
     * <p>
     * Specifies that a Sparkplug Aware MQTT Server MAY replace the timestamp of NDEATH messages.
     * If it does modify the timestamp, it MUST set it to the UTC time at which it attempts
     * to deliver the NDEATH message to subscribed clients, rather than the original Last Will
     * and Testament (LWT) creation time.
     */
    static final @NotNull String CONFORMANCE_MQTT_AWARE_NDEATH_TIMESTAMP =
            "A Sparkplug Aware MQTT Server MAY replace the timestamp of NDEATH messages. If it does, it MUST set the timestamp to the UTC time at which it attempts to deliver the NDEATH to subscribed clients";
}
