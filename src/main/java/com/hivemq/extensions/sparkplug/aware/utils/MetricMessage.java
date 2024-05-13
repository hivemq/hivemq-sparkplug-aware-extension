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
package com.hivemq.extensions.sparkplug.aware.utils;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.text.MessageFormat;
import java.util.Date;

public class MetricMessage {

    private static final String JSON_MESSAGE_FORMAT =
            "'{'\n" +
            "    \"name\": \"{0}\", \n" +
            "    \"timestamp\": {1}, \n" +
            "    \"dataType\": \"{2}\", \n" +
            "    \"value\": {3}\n" +
            "'}'";

    private MetricMessage() {
    }

    public static String createJSON(final @NotNull String name, final @NotNull Date timestamp, final @NotNull String value, final @NotNull String dataType) {
        final MessageFormat mf = new MessageFormat(JSON_MESSAGE_FORMAT);
        return mf.format(new Object[]{
                name,
                timestamp != null ? timestamp.toInstant().getEpochSecond() : 0,
                dataType,
                value});
    }
}
