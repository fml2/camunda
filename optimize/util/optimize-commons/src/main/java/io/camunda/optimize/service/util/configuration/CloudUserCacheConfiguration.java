/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.util.configuration;

public class CloudUserCacheConfiguration {

  private int maxSize;
  private long minFetchIntervalSeconds;

  public CloudUserCacheConfiguration() {}

  public int getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(final int maxSize) {
    this.maxSize = maxSize;
  }

  public long getMinFetchIntervalSeconds() {
    return minFetchIntervalSeconds;
  }

  public void setMinFetchIntervalSeconds(final long minFetchIntervalSeconds) {
    this.minFetchIntervalSeconds = minFetchIntervalSeconds;
  }

  protected boolean canEqual(final Object other) {
    return other instanceof CloudUserCacheConfiguration;
  }

  @Override
  public int hashCode() {
    return org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public boolean equals(final Object o) {
    return org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public String toString() {
    return "CloudUserCacheConfiguration(maxSize="
        + getMaxSize()
        + ", minFetchIntervalSeconds="
        + getMinFetchIntervalSeconds()
        + ")";
  }
}
