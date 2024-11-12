/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.filter;

import static io.camunda.optimize.service.db.es.filter.util.ModelElementFilterQueryUtilES.createUserTaskFlowNodeTypeFilter;
import static io.camunda.optimize.service.db.schema.index.ProcessInstanceIndex.FLOW_NODE_INSTANCES;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import io.camunda.optimize.dto.optimize.query.report.single.process.filter.data.InstancesContainingUserTasksFilterDataDto;
import io.camunda.optimize.service.db.filter.FilterContext;
import io.camunda.optimize.service.util.configuration.condition.ElasticSearchCondition;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(ElasticSearchCondition.class)
public class InstancesContainingUserTasksFilterES
    implements QueryFilterES<InstancesContainingUserTasksFilterDataDto> {

  @Override
  public void addFilters(
      final BoolQuery.Builder query,
      final List<InstancesContainingUserTasksFilterDataDto> instancesContainingUserTasksFilters,
      final FilterContext filterContext) {
    if (!CollectionUtils.isEmpty(instancesContainingUserTasksFilters)) {
      query.filter(
          f ->
              f.nested(
                  n ->
                      n.path(FLOW_NODE_INSTANCES)
                          .query(q -> q.bool(createUserTaskFlowNodeTypeFilter().build()))
                          .scoreMode(ChildScoreMode.None)));
    }
  }
}
