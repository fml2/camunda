/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.upgrade.migrate313to86.indices.os;

import io.camunda.optimize.service.db.os.OptimizeOpenSearchUtil;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import io.camunda.optimize.upgrade.migrate313to86.indices.db.EventIndexV4;
import java.io.IOException;
import org.opensearch.client.opensearch.indices.IndexSegmentSort;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.IndexSettings.Builder;
import org.opensearch.client.opensearch.indices.SegmentSortOrder;

public class EventIndexV4OS extends EventIndexV4<Builder> {

  @Override
  public IndexSettings.Builder addStaticSetting(
      final String key, final int value, final IndexSettings.Builder contentBuilder) {
    return OptimizeOpenSearchUtil.addStaticSetting(key, value, contentBuilder);
  }

  @Override
  public IndexSettings.Builder getStaticSettings(
      IndexSettings.Builder contentBuilder, ConfigurationService configurationService)
      throws IOException {
    final IndexSettings.Builder result =
        super.getStaticSettings(contentBuilder, configurationService);
    return result.sort(
        new IndexSegmentSort.Builder()
            .field(INGESTION_TIMESTAMP)
            .order(SegmentSortOrder.Desc)
            .build());
  }
}
