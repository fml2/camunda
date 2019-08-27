/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.rest;

import org.camunda.optimize.dto.optimize.query.FlowNodeOutlierParametersDto;
import org.camunda.optimize.rest.queryparam.QueryParamUtil;

import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.stream.Collectors;

public class FlowNodeOutlierParametersRestDto extends FlowNodeOutlierParametersDto {

  @Override
  @QueryParam("processDefinitionKey")
  public void setProcessDefinitionKey(final String processDefinitionKey) {
    super.setProcessDefinitionKey(processDefinitionKey);
  }

  @Override
  @QueryParam("processDefinitionVersions")
  public void setProcessDefinitionVersions(final List<String> processDefinitionVersions) {
    super.setProcessDefinitionVersions(processDefinitionVersions);
  }

  @Override
  @QueryParam("tenantIds")
  public void setTenantIds(List<String> tenantIds) {
    this.tenantIds = normalizeTenants(tenantIds);
  }

  @Override
  @QueryParam("flowNodeId")
  public void setFlowNodeId(final String flowNodeId) {
    super.setFlowNodeId(flowNodeId);
  }

  @Override
  @QueryParam("lowerOutlierBound")
  public void setLowerOutlierBound(final Long lowerOutlierBound) {
    super.setLowerOutlierBound(lowerOutlierBound);
  }

  @Override
  @QueryParam("higherOutlierBound")
  public void setHigherOutlierBound(final Long higherOutlierBound) {
    super.setHigherOutlierBound(higherOutlierBound);
  }

  private List<String> normalizeTenants(List<String> tenantIds) {
    return tenantIds.stream().map(QueryParamUtil::normalizeNullStringValue).collect(Collectors.toList());
  }
}
