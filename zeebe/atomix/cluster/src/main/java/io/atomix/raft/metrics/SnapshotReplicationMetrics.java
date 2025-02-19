/*
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
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
package io.atomix.raft.metrics;

import static io.atomix.raft.metrics.SnapshotReplicationMetricsDoc.*;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class SnapshotReplicationMetrics extends RaftMetrics {

  private final AtomicLong count;
  private final AtomicLong duration;

  public SnapshotReplicationMetrics(final String partitionName, final MeterRegistry meterRegistry) {
    super(partitionName);
    Objects.requireNonNull(meterRegistry, "meterRegistry cannot be null");

    count = new AtomicLong(0L);
    Gauge.builder(COUNT.getName(), count::get)
        .description(COUNT.getDescription())
        .tags(PARTITION_GROUP_NAME_LABEL, partitionGroupName)
        .register(meterRegistry);

    duration = new AtomicLong(0L);
    Gauge.builder(DURATION.getName(), duration::get)
        .description(DURATION.getDescription())
        .tags(PARTITION_GROUP_NAME_LABEL, partitionGroupName)
        .register(meterRegistry);
  }

  public void incrementCount() {
    count.incrementAndGet();
  }

  public void decrementCount() {
    count.decrementAndGet();
  }

  public void setCount(final int value) {
    count.set(value);
  }

  public void observeDuration(final long durationMillis) {
    duration.set(durationMillis);
  }
}
