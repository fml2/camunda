/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.rest.cache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.camunda.search.entities.FlowNodeInstanceEntity;
import io.camunda.search.entities.UserTaskEntity;
import io.camunda.zeebe.gateway.rest.config.GatewayRestConfiguration;
import io.camunda.zeebe.gateway.rest.util.XmlUtil;
import io.camunda.zeebe.util.VisibleForTesting;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ProcessCache {

  private final LoadingCache<Long, ProcessCacheItem> cache;
  private final XmlUtil xmlUtil;

  public ProcessCache(final GatewayRestConfiguration configuration, final XmlUtil xmlUtil) {
    this.xmlUtil = xmlUtil;
    final var cacheBuilder =
        Caffeine.newBuilder().maximumSize(configuration.getProcessCache().getMaxSize());
    final var ttlMillis = configuration.getProcessCache().getExpirationMillis();
    if (ttlMillis != null && ttlMillis > 0) {
      cacheBuilder.expireAfterAccess(ttlMillis, TimeUnit.MILLISECONDS);
    }
    cache = cacheBuilder.build(new ProcessCacheLoader());
  }

  public ProcessCacheItem getCacheItem(final long processDefinitionKey) {
    return cache.get(processDefinitionKey);
  }

  public ProcessCacheItems getCacheItems(final Set<Long> processDefinitionKeys) {
    return new ProcessCacheItems(cache.getAll(processDefinitionKeys));
  }

  public ProcessCacheItems getUserTaskNames(final List<UserTaskEntity> items) {
    return getCacheItems(
        items.stream().map(UserTaskEntity::processDefinitionKey).collect(Collectors.toSet()));
  }

  public String getUserTaskName(final UserTaskEntity userTask) {
    return getCacheItem(userTask.processDefinitionKey()).getFlowNodeName(userTask.elementId());
  }

  public ProcessCacheItems getFlowNodeNames(final List<FlowNodeInstanceEntity> items) {
    return getCacheItems(
        items.stream()
            .map(FlowNodeInstanceEntity::processDefinitionKey)
            .collect(Collectors.toSet()));
  }

  public String getFlowNodeName(final FlowNodeInstanceEntity flowNode) {
    return getCacheItem(flowNode.processDefinitionKey()).getFlowNodeName(flowNode.flowNodeId());
  }

  @VisibleForTesting
  LoadingCache<Long, ProcessCacheItem> getCache() {
    return cache;
  }

  private final class ProcessCacheLoader implements CacheLoader<Long, ProcessCacheItem> {

    @Override
    public ProcessCacheItem load(final Long key) {
      final var flowNodes = new ConcurrentHashMap<String, String>();
      xmlUtil.extractFlowNodeNames(key, (pdId, node) -> flowNodes.put(node.id(), node.name()));
      return new ProcessCacheItem(flowNodes);
    }

    @Override
    public Map<Long, ProcessCacheItem> loadAll(final Set<? extends Long> keys) {
      final var processMap = new HashMap<Long, ProcessCacheItem>();
      xmlUtil.extractFlowNodeNames(
          (Set<Long>) keys,
          (pdId, node) -> {
            final var flowNodeMap =
                processMap.computeIfAbsent(
                    pdId, key -> new ProcessCacheItem(new ConcurrentHashMap<>()));
            flowNodeMap.putFlowNode(node.id(), node.name());
          });
      return processMap;
    }
  }
}
