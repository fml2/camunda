/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FlowNodeOutlierParametersDto extends ProcessDefinitionParametersDto {
  protected String flowNodeId;
  protected Long lowerOutlierBound;
  protected Long higherOutlierBound;
}
