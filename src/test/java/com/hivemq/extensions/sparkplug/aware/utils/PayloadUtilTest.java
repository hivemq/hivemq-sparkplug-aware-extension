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

import org.eclipse.tahu.SparkplugInvalidTypeException;
import org.eclipse.tahu.message.SparkplugBPayloadEncoder;
import org.eclipse.tahu.message.model.Metric;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.hivemq.extensions.sparkplug.aware.utils.PayloadUtil.asJSONFormatted;
import static com.hivemq.extensions.sparkplug.aware.utils.PayloadUtil.getPayloadAsJSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tahu.message.model.MetricDataType.Int32;
import static org.eclipse.tahu.message.model.MetricDataType.Text;

class PayloadUtilTest {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(PayloadUtilTest.class);

    private final @NotNull List<Metric> metrics = new ArrayList<>();

    @Test
    void asJSONFormattedTest() throws IOException, SparkplugInvalidTypeException {
        final var payload = ByteBuffer.wrap(createSparkplugBPayload());
        final var json = asJSONFormatted(getPayloadAsJSON(payload));
        LOG.info(json);
        assertThat(json).isNotEmpty();
    }

    @Test
    void getPayloadAsJSONTest() throws IOException, SparkplugInvalidTypeException {
        final var payload = ByteBuffer.wrap(createSparkplugBPayload());
        final var json = getPayloadAsJSON(payload);
        LOG.info(json);
        assertThat(json).isNotEmpty();
    }

    private byte @NotNull [] createSparkplugBPayload() throws IOException, SparkplugInvalidTypeException {
        // add a 'real time' metric
        metrics.add(new Metric.MetricBuilder("a number metric", Int32, 42).timestamp(new Date()).createMetric());
        metrics.add(new Metric.MetricBuilder("a text metric", Text, "Hello").timestamp(new Date()).createMetric());

        final var sparkplugBPayload = new SparkplugBPayload(new Date(), metrics, 1L, null, null);
        return new SparkplugBPayloadEncoder().getBytes(sparkplugBPayload, false);
    }
}
