/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.report.combined.configuration.target_value;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Setter
@Getter
public class CombinedReportTargetValueDto {

  private CombinedReportCountChartDto countChart = new CombinedReportCountChartDto();
  private Boolean active = false;
  private CombinedReportDurationChartDto durationChart = new CombinedReportDurationChartDto();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CombinedReportTargetValueDto)) {
      return false;
    }
    CombinedReportTargetValueDto that = (CombinedReportTargetValueDto) o;
    return Objects.equals(countChart, that.countChart) &&
      Objects.equals(active, that.active) &&
      Objects.equals(durationChart, that.durationChart);
  }

  @Override
  public int hashCode() {
    return Objects.hash(countChart, active, durationChart);
  }
}
