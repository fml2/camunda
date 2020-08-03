/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.rest;

import org.camunda.optimize.AbstractIT;
import org.camunda.optimize.dto.optimize.ReportType;
import org.camunda.optimize.dto.optimize.query.IdDto;
import org.camunda.optimize.dto.optimize.query.collection.PartialCollectionDefinitionDto;
import org.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionDto;
import org.camunda.optimize.dto.optimize.query.entity.EntityNameDto;
import org.camunda.optimize.dto.optimize.query.entity.EntityNameRequestDto;
import org.camunda.optimize.dto.optimize.query.event.EventScopeType;
import org.camunda.optimize.dto.optimize.query.event.EventSourceEntryDto;
import org.camunda.optimize.dto.optimize.query.event.EventSourceType;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionDto;
import org.camunda.optimize.dto.optimize.rest.EventProcessMappingCreateRequestDto;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.optimize.test.it.extension.TestEmbeddedCamundaOptimize.DEFAULT_PASSWORD;
import static org.camunda.optimize.test.it.extension.TestEmbeddedCamundaOptimize.DEFAULT_USERNAME;

public class EntityNamesRestServiceIT extends AbstractIT {

  @Test
  public void getEntityNames_WorksForAllPossibleEntities() {
    //given
    String reportId = addSingleReportToOptimize("aReportName", ReportType.PROCESS);
    String dashboardId = addDashboardToOptimize("aDashboardName");
    String collectionId = addCollection("aCollectionName");
    String eventProcessId = addEventProcessMappingToOptimize("anEventProcessName");
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    EntityNameDto result = entitiesClient.getEntityNames(collectionId, dashboardId, reportId, eventProcessId);

    // then
    assertThat(result.getCollectionName()).isEqualTo("aCollectionName");
    assertThat(result.getDashboardName()).isEqualTo("aDashboardName");
    assertThat(result.getReportName()).isEqualTo("aReportName");
    assertThat(result.getEventBasedProcessName()).isEqualTo("anEventProcessName");
  }

  @Test
  public void getEntityNames_ReturnsNoResponseForEventBasedProcessIfThereIsNone() {
    //given
    String reportId = addSingleReportToOptimize("aReportName", ReportType.PROCESS);
    String dashboardId = addDashboardToOptimize("aDashboardName");
    String collectionId = addCollection("aCollectionName");
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    EntityNameDto result = entitiesClient.getEntityNames(collectionId, dashboardId, reportId, "eventProcessId");

    // then
    assertThat(result.getCollectionName()).isEqualTo("aCollectionName");
    assertThat(result.getDashboardName()).isEqualTo("aDashboardName");
    assertThat(result.getReportName()).isEqualTo("aReportName");
    assertThat(result.getEventBasedProcessName()).isNull();
  }

  @Test
  public void getEntityNames_SeveralReportsDoNotDistortResult() {
    //given
    String reportId = addSingleReportToOptimize("aProcessReportName", ReportType.PROCESS);
    addSingleReportToOptimize("aDecisionReportName", ReportType.DECISION);
    addCombinedReport("aCombinedReportName");
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    EntityNameDto result = entitiesClient.getEntityNames(null, null, reportId, null);

    // then
    assertThat(result.getCollectionName()).isNull();
    assertThat(result.getDashboardName()).isNull();
    assertThat(result.getReportName()).isEqualTo("aProcessReportName");
    assertThat(result.getEventBasedProcessName()).isNull();
  }

  @Test
  public void getEntityNames_WorksForDecisionReports() {
    //given
    String reportId = addSingleReportToOptimize("aDecisionReportName", ReportType.DECISION);
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    EntityNameDto result = entitiesClient.getEntityNames(null, null, reportId, null);

    // then
    assertThat(result.getCollectionName()).isNull();
    assertThat(result.getDashboardName()).isNull();
    assertThat(result.getReportName()).isEqualTo("aDecisionReportName");
    assertThat(result.getEventBasedProcessName()).isNull();
  }

  @Test
  public void getEntityNames_WorksForCombinedReports() {
    //given
    String reportId = addCombinedReport("aCombinedReportName");
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    EntityNameDto result = entitiesClient.getEntityNames(null, null, reportId, null);

    // then
    assertThat(result.getCollectionName()).isNull();
    assertThat(result.getDashboardName()).isNull();
    assertThat(result.getReportName()).isEqualTo("aCombinedReportName");
    assertThat(result.getEventBasedProcessName()).isNull();
  }

  @Test
  public void getEntityNames_NotAvailableIdReturns404() {
    //given
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildGetEntityNamesRequest(new EntityNameRequestDto(null, null, "notAvailableRequest", null))
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void getEntityNames_NoIdProvidedReturns400() {
    //given
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildGetEntityNamesRequest(new EntityNameRequestDto(null, null, null, null))
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  private String addCollection(final String collectionName) {
    final String collectionId = collectionClient.createNewCollection();
    collectionClient.updateCollection(collectionId, new PartialCollectionDefinitionDto(collectionName));
    return collectionId;
  }

  private String addSingleReportToOptimize(String name, ReportType reportType) {
    return addSingleReportToOptimize(name, reportType, null, DEFAULT_USERNAME);
  }

  private String addSingleReportToOptimize(String name, ReportType reportType, String collectionId, String user) {
    switch (reportType) {
      case PROCESS:
        SingleProcessReportDefinitionDto singleProcessReportDefinitionDto = new SingleProcessReportDefinitionDto();
        singleProcessReportDefinitionDto.setName(name);
        singleProcessReportDefinitionDto.setCollectionId(collectionId);
        return reportClient.createSingleProcessReportAsUser(singleProcessReportDefinitionDto, user, user);
      case DECISION:
        SingleDecisionReportDefinitionDto singleDecisionReportDefinitionDto = new SingleDecisionReportDefinitionDto();
        singleDecisionReportDefinitionDto.setName(name);
        singleDecisionReportDefinitionDto.setCollectionId(collectionId);
        return reportClient.createNewDecisionReportAsUser(singleDecisionReportDefinitionDto, user, user);
      default:
        throw new IllegalStateException("ReportType not allowed!");
    }
  }

  private String addDashboardToOptimize(String name) {
    return addDashboardToOptimize(name, null, DEFAULT_USERNAME);
  }

  private String addDashboardToOptimize(String name, String collectionId, String user) {
    DashboardDefinitionDto dashboardDefinitionDto = new DashboardDefinitionDto();
    dashboardDefinitionDto.setName(name);
    dashboardDefinitionDto.setCollectionId(collectionId);
    return dashboardClient.createDashboardAsUser(dashboardDefinitionDto, user, user);
  }

  private String addCombinedReport(String name) {
    return addCombinedReport(name, null);
  }

  private String addCombinedReport(String name, String collectionId) {
    CombinedReportDefinitionDto combinedReportDefinitionDto = new CombinedReportDefinitionDto();
    combinedReportDefinitionDto.setName(name);
    combinedReportDefinitionDto.setCollectionId(collectionId);
    return embeddedOptimizeExtension
      .getRequestExecutor()
      .buildCreateCombinedReportRequest(combinedReportDefinitionDto)
      .withUserAuthentication(DEFAULT_USERNAME, DEFAULT_PASSWORD)
      .execute(IdDto.class, Response.Status.OK.getStatusCode()).getId();
  }

  @SuppressWarnings("SameParameterValue")
  private String addEventProcessMappingToOptimize(final String eventProcessName) {
    EventProcessMappingCreateRequestDto eventBasedProcessDto =
      EventProcessMappingCreateRequestDto.eventProcessMappingCreateBuilder()
        .name(eventProcessName)
        .eventSources(Collections.singletonList(
          EventSourceEntryDto.builder()
            .eventScope(Collections.singletonList(EventScopeType.ALL))
            .type(EventSourceType.EXTERNAL)
            .build()))
        .build();
    return eventProcessClient.createEventProcessMapping(eventBasedProcessDto);
  }

}
