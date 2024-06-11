/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package org.camunda.optimize.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.camunda.optimize.dto.optimize.query.report.single.ReportDataDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.result.raw.RawDataDecisionInstanceDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionRequestDto;
import org.camunda.optimize.dto.optimize.query.variable.VariableType;
import org.camunda.optimize.dto.optimize.rest.report.AuthorizedDecisionReportEvaluationResponseDto;
import org.camunda.optimize.dto.optimize.rest.report.AuthorizedSingleReportEvaluationResponseDto;
import org.camunda.optimize.service.util.ProcessReportDataType;
import org.camunda.optimize.service.util.TemplatedProcessReportDataBuilder;
import org.camunda.optimize.test.util.decision.DecisionReportDataBuilder;
import org.camunda.optimize.test.util.decision.DecisionReportDataType;
import org.camunda.optimize.util.BpmnModels;
import org.camunda.optimize.util.DmnModels;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Tag(OPENSEARCH_PASSING)
public class ReportEvaluationMultiDefinitionRestServiceIT extends AbstractReportRestServiceIT {

  @ParameterizedTest
  @EnumSource(ProcessReportDataType.class)
  @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
  public void evaluateProcessReport(final ProcessReportDataType reportType) {
    // given
    final String key1 = "key1";
    final String key2 = "key2";
    final String variableName = "var1";
    final String candidateGroupName = "firstGroup";
    final String processInstanceId1 =
        engineIntegrationExtension
            .deployAndStartProcessWithVariables(
                BpmnModels.getSingleUserTaskDiagram(key1), ImmutableMap.of(variableName, 1))
            .getId();
    final String processInstanceId2 =
        engineIntegrationExtension
            .deployAndStartProcessWithVariables(
                BpmnModels.getSingleUserTaskDiagram(key2), ImmutableMap.of(variableName, 1))
            .getId();
    engineIntegrationExtension.createGroup(candidateGroupName);
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(candidateGroupName);
    engineIntegrationExtension.claimAllRunningUserTasks();
    engineIntegrationExtension.completeUserTaskWithoutClaim(processInstanceId1);
    engineIntegrationExtension.completeUserTaskWithoutClaim(processInstanceId2);

    importAllEngineEntitiesFromScratch();

    final List<ReportDataDefinitionDto> definitions =
        List.of(new ReportDataDefinitionDto(key1), new ReportDataDefinitionDto(key2));

    // when
    final AuthorizedSingleReportEvaluationResponseDto<?, SingleProcessReportDefinitionRequestDto>
        processReportResponse =
            reportClient.evaluateReport(
                TemplatedProcessReportDataBuilder.createReportData()
                    .setReportDataType(reportType)
                    .setVariableName(variableName)
                    .setVariableType(VariableType.SHORT)
                    .definitions(definitions)
                    .build());

    // then
    assertThat(processReportResponse.getResult().getInstanceCount()).isEqualTo(2);
  }

  @ParameterizedTest
  @EnumSource(ProcessReportDataType.class)
  @Tag(OPENSEARCH_SINGLE_TEST_FAIL_OK)
  public void evaluateProcessReportOneDefinitionHasNoData(final ProcessReportDataType reportType) {
    // given
    final String key1 = "key1";
    final String key2 = "key2";
    final String variableName = "var1";
    final String candidateGroupName = "firstGroup";
    final String processInstanceId1 =
        engineIntegrationExtension
            .deployAndStartProcessWithVariables(
                BpmnModels.getSingleUserTaskDiagram(key1), ImmutableMap.of(variableName, 1))
            .getId();
    final String processDefinition2 =
        engineIntegrationExtension.deployProcessAndGetId(BpmnModels.getSingleUserTaskDiagram(key2));
    engineIntegrationExtension.createGroup(candidateGroupName);
    engineIntegrationExtension.addCandidateGroupForAllRunningUserTasks(candidateGroupName);
    engineIntegrationExtension.claimAllRunningUserTasks();
    engineIntegrationExtension.completeUserTaskWithoutClaim(processInstanceId1);

    importAllEngineEntitiesFromScratch();

    final List<ReportDataDefinitionDto> definitions =
        List.of(new ReportDataDefinitionDto(key1), new ReportDataDefinitionDto(key2));

    // when
    final AuthorizedSingleReportEvaluationResponseDto<?, SingleProcessReportDefinitionRequestDto>
        processReportResponse =
            reportClient.evaluateReport(
                TemplatedProcessReportDataBuilder.createReportData()
                    .setReportDataType(reportType)
                    .setVariableName(variableName)
                    .setVariableType(VariableType.SHORT)
                    .definitions(definitions)
                    .build());

    // then
    assertThat(processReportResponse.getResult().getInstanceCount()).isEqualTo(1);
  }

  @ParameterizedTest
  @EnumSource(ProcessReportDataType.class)
  public void evaluateProcessReportNeitherDefinitionHasData(
      final ProcessReportDataType reportType) {
    // given
    final String key1 = "key1";
    final String key2 = "key2";
    final String variableName = "var1";
    engineIntegrationExtension.deployProcessAndGetId(BpmnModels.getSingleUserTaskDiagram(key1));
    engineIntegrationExtension.deployProcessAndGetId(BpmnModels.getSingleUserTaskDiagram(key2));

    importAllEngineEntitiesFromScratch();

    final List<ReportDataDefinitionDto> definitions =
        List.of(new ReportDataDefinitionDto(key1), new ReportDataDefinitionDto(key2));

    // when
    final AuthorizedSingleReportEvaluationResponseDto<?, SingleProcessReportDefinitionRequestDto>
        processReportResponse =
            reportClient.evaluateReport(
                TemplatedProcessReportDataBuilder.createReportData()
                    .setReportDataType(reportType)
                    .setVariableName(variableName)
                    .setVariableType(VariableType.SHORT)
                    .definitions(definitions)
                    .build());

    // then
    assertThat(processReportResponse.getResult().getInstanceCount()).isEqualTo(0);
  }

  @Test
  public void evaluateDecisionReport_onlyDataForTheFirstDefinitionIsIncluded() {
    // given
    final String key1 = "key1";
    final String key2 = "key2";
    final String firstDefinitionId =
        engineIntegrationExtension
            .deployAndStartDecisionDefinition(DmnModels.createDefaultDmnModel(key1))
            .getId();
    engineIntegrationExtension.deployAndStartDecisionDefinition(
        DmnModels.createDefaultDmnModel(key2));

    importAllEngineEntitiesFromScratch();

    final List<ReportDataDefinitionDto> definitions =
        List.of(new ReportDataDefinitionDto(key1), new ReportDataDefinitionDto(key2));

    // when
    final AuthorizedDecisionReportEvaluationResponseDto<List<RawDataDecisionInstanceDto>>
        decisionReportResponse =
            reportClient.evaluateDecisionRawReport(
                DecisionReportDataBuilder.create()
                    .setReportDataType(DecisionReportDataType.RAW_DATA)
                    .definitions(definitions)
                    .build());

    // then
    assertThat(decisionReportResponse.getResult().getData())
        .extracting(RawDataDecisionInstanceDto::getDecisionDefinitionId)
        .containsExactly(firstDefinitionId);
  }
}
