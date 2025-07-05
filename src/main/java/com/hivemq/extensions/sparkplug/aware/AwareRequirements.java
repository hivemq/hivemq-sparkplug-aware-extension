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

public final class AwareRequirements {

    AwareRequirements() {
    }

    public static final @NotNull String CONFORMANCE_MQTT_AWARE_NBIRTH_MQTT_TOPIC =
            "A Sparkplug Aware MQTT Server MUST make NBIRTH messages available on a topic of the form: $sparkplug/certificates/namespace/group_id/NBIRTH/edge_node_id";
    public static final @NotNull String CONFORMANCE_MQTT_AWARE_NBIRTH_MQTT_RETAIN =
            "A Sparkplug Aware MQTT Server MUST make NBIRTH messages available on the topic: $sparkplug/certificates/namespace/group_id/NBIRTH/edge_node_id with the MQTT retain flag set to true";
    public static final @NotNull String CONFORMANCE_MQTT_AWARE_DBIRTH_MQTT_TOPIC =
            "A Sparkplug Aware MQTT Server MUST make DBIRTH messages available on a topic of the form: $sparkplug/certificates/namespace/group_id/DBIRTH/edge_node_id/device_id";
    public static final @NotNull String CONFORMANCE_MQTT_AWARE_DBIRTH_MQTT_RETAIN =
            "A Sparkplug Aware MQTT Server MUST make DBIRTH messages available on the topic: $sparkplug/certificates/namespace/group_id/DBIRTH/edge_node_id/device_id with the MQTT retain flag set to true";
    public static final @NotNull String CONFORMANCE_MQTT_AWARE_NDEATH_TIMESTAMP =
            "A Sparkplug Aware MQTT Server MAY replace the timestamp of NDEATH messages. If it does, it MUST set the timestamp to the UTC time at which it attempts to deliver the NDEATH to subscribed clients";
}
