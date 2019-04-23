/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.report.single.process.parameters;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessPartDto {

  protected String start;
  protected String end;

  public String createCommandKey() {
    return "processPart";
  }

  @Override
  public String toString() {
    return "ProcessPartDto{" +
      "start='" + start + '\'' +
      ", end='" + end + '\'' +
      '}';
  }
}
