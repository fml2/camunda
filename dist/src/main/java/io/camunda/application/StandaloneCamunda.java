/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.application;

import static org.springframework.core.env.AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME;

import io.camunda.application.initializers.HealthConfigurationInitializer;
import io.camunda.application.listeners.ApplicationErrorListener;
import io.camunda.operate.OperateModuleConfiguration;
import io.camunda.services.CamundaServicesConfiguration;
import io.camunda.zeebe.broker.BrokerModuleConfiguration;
import io.camunda.zeebe.gateway.GatewayModuleConfiguration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringBootConfiguration;

@SpringBootConfiguration(proxyBeanMethods = false)
public class StandaloneCamunda {

  private static final String SPRING_PROFILES_ACTIVE_PROPERTY = ACTIVE_PROFILES_PROPERTY_NAME;
  private static final String DEFAULT_CAMUNDA_PROFILES =
      String.format("%s,%s", Profile.OPERATE.getId(), Profile.BROKER.getId());

  public static void main(final String[] args) {
    MainSupport.setDefaultGlobalConfiguration();
    MainSupport.putSystemPropertyIfAbsent(
        "spring.banner.location", "classpath:/assets/camunda_banner.txt");

    final var defaultActiveProfiles = getDefaultActiveProfiles();
    final var standaloneCamundaApplication =
        MainSupport.createDefaultApplicationBuilder()
            .sources(
                OperateModuleConfiguration.class,
                BrokerModuleConfiguration.class,
                GatewayModuleConfiguration.class,
                CamundaServicesConfiguration.class)
            .properties(defaultActiveProfiles)
            .initializers(new HealthConfigurationInitializer())
            .listeners(new ApplicationErrorListener())
            .build(args);

    standaloneCamundaApplication.run();
  }

  public static Map<String, Object> getDefaultActiveProfiles() {
    final var defaultProperties = new HashMap<String, Object>();
    defaultProperties.put(SPRING_PROFILES_ACTIVE_PROPERTY, DEFAULT_CAMUNDA_PROFILES);
    return defaultProperties;
  }
}
