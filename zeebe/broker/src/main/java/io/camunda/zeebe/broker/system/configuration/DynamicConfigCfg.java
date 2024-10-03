/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.broker.system.configuration;

import io.camunda.zeebe.dynamic.config.gossip.ClusterConfigurationGossiperConfig;
import java.util.Optional;

public record DynamicConfigCfg(ClusterConfigurationGossiperConfig gossip)
    implements ConfigurationEntry {

  public DynamicConfigCfg(final ClusterConfigurationGossiperConfig gossip) {
    this.gossip = Optional.ofNullable(gossip).orElse(ClusterConfigurationGossiperConfig.DEFAULT);
  }

  public static DynamicConfigCfg defaultConfig() {
    return new DynamicConfigCfg(null);
  }
}
