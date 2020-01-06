/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.camunda.optimize.dto.optimize.query.alert.AlertCreationDto;
import org.camunda.optimize.dto.optimize.query.analysis.BranchAnalysisQueryDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionRoleDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionRoleUpdateDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionScopeEntryDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionScopeEntryUpdateDto;
import org.camunda.optimize.dto.optimize.query.collection.PartialCollectionDefinitionDto;
import org.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionDto;
import org.camunda.optimize.dto.optimize.query.entity.EntityNameRequestDto;
import org.camunda.optimize.dto.optimize.query.event.EventCountRequestDto;
import org.camunda.optimize.dto.optimize.query.event.EventCountSuggestionsRequestDto;
import org.camunda.optimize.dto.optimize.query.event.EventDto;
import org.camunda.optimize.dto.optimize.query.event.EventProcessMappingDto;
import org.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.SingleReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.DecisionReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.security.CredentialsDto;
import org.camunda.optimize.dto.optimize.query.sharing.DashboardShareDto;
import org.camunda.optimize.dto.optimize.query.sharing.ReportShareDto;
import org.camunda.optimize.dto.optimize.query.sharing.ShareSearchDto;
import org.camunda.optimize.dto.optimize.query.variable.DecisionVariableNameRequestDto;
import org.camunda.optimize.dto.optimize.query.variable.DecisionVariableValueRequestDto;
import org.camunda.optimize.dto.optimize.query.variable.ProcessVariableNameRequestDto;
import org.camunda.optimize.dto.optimize.query.variable.ProcessVariableValueRequestDto;
import org.camunda.optimize.dto.optimize.rest.FlowNodeIdsToNamesRequestDto;
import org.camunda.optimize.dto.optimize.rest.OnboardingStateRestDto;
import org.camunda.optimize.exception.OptimizeIntegrationTestException;
import org.camunda.optimize.service.security.AuthCookieService;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static org.camunda.optimize.rest.IngestionRestService.OPTIMIZE_API_SECRET_HEADER;
import static org.camunda.optimize.service.security.AuthCookieService.OPTIMIZE_AUTHORIZATION;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class OptimizeRequestExecutor {
  private static final String ALERT = "alert";

  private WebTarget client;
  private String defaultAuthCookie;
  private String authCookie;
  private String path;
  private String method;
  private Entity body;
  private Map<String, Object> queryParams;
  private Map<String, String> cookies = new HashMap<>();
  private Map<String, String> requestHeaders = new HashMap<>();

  private ObjectMapper objectMapper;

  public OptimizeRequestExecutor(WebTarget client, String defaultAuthToken, ObjectMapper objectMapper) {
    this.client = client;
    this.authCookie = defaultAuthToken;
    this.defaultAuthCookie = defaultAuthToken;
    this.objectMapper = objectMapper;
  }

  public OptimizeRequestExecutor addQueryParams(Map<String, Object> queryParams) {
    if (this.queryParams != null && queryParams.size() != 0) {
      this.queryParams.putAll(queryParams);
    } else {
      this.queryParams = queryParams;
    }
    return this;
  }

  public OptimizeRequestExecutor addSingleQueryParam(String key, Object value) {
    if (this.queryParams != null && queryParams.size() != 0) {
      this.queryParams.put(key, value);
    } else {
      HashMap<String, Object> params = new HashMap<>();
      params.put(key, value);
      this.queryParams = params;
    }
    return this;
  }

  public OptimizeRequestExecutor addSingleCookie(String key, String value) {
    cookies.put(key, value);
    return this;
  }

  public OptimizeRequestExecutor addSingleHeader(String key, String value) {
    requestHeaders.put(key, value);
    return this;
  }

  public OptimizeRequestExecutor withUserAuthentication(String username, String password) {
    this.authCookie = authenticateUserRequest(username, password);
    return this;
  }

  public OptimizeRequestExecutor withoutAuthentication() {
    this.authCookie = null;
    return this;
  }

  public OptimizeRequestExecutor withGivenAuthToken(String authToken) {
    this.authCookie = AuthCookieService.createOptimizeAuthCookieValue(authToken);
    return this;
  }

  public Response execute() {
    Invocation.Builder builder = prepareRequest();

    final Response response;
    switch (this.method) {
      case GET:
        response = builder.get();
        break;
      case POST:
        response = builder.post(body);
        break;
      case PUT:
        response = builder.put(body);
        break;
      case DELETE:
        response = builder.delete();
        break;
      default:
        throw new OptimizeIntegrationTestException("Unsupported http method: " + this.method);
    }

    resetBuilder();
    return response;
  }

  private Invocation.Builder prepareRequest() {
    WebTarget webTarget = client.path(this.path);

    if (queryParams != null && queryParams.size() != 0) {
      for (Map.Entry<String, Object> queryParam : queryParams.entrySet()) {
        if (queryParam.getValue() instanceof List) {
          for (Object p : ((List) queryParam.getValue())) {
            if (p == null) {
              webTarget = webTarget.queryParam(queryParam.getKey(), "null");
            } else {
              webTarget = webTarget.queryParam(queryParam.getKey(), p);
            }
          }
        } else {
          webTarget = webTarget.queryParam(queryParam.getKey(), queryParam.getValue());
        }
      }
    }

    Invocation.Builder builder = webTarget.request();

    for (Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
      builder = builder.cookie(cookieEntry.getKey(), cookieEntry.getValue());
    }

    if (authCookie != null) {
      builder = builder.cookie(OPTIMIZE_AUTHORIZATION, this.authCookie);
    }

    for (Map.Entry<String, String> headerEntry : requestHeaders.entrySet()) {
      builder = builder.header(headerEntry.getKey(), headerEntry.getValue());
    }
    return builder;
  }

  public Response execute(int expectedResponseCode) {
    Response response = execute();
    assertThat(response.getStatus(), is(expectedResponseCode));
    return response;
  }

  public <T> T execute(TypeReference<T> classToExtractFromResponse) {
    Response response = execute();
    assertThat(response.getStatus(), is(200));

    String jsonString = response.readEntity(String.class);
    try {
      return objectMapper.readValue(jsonString, classToExtractFromResponse);
    } catch (IOException e) {
      throw new OptimizeIntegrationTestException(e);
    }
  }

  public <T> T execute(Class<T> classToExtractFromResponse, int responseCode) {
    Response response = execute();
    assertThat(response.getStatus(), is(responseCode));
    return response.readEntity(classToExtractFromResponse);
  }

  public <T> List<T> executeAndReturnList(Class<T> classToExtractFromResponse, int responseCode) {
    Response response = execute();
    assertThat(response.getStatus(), is(responseCode));
    String jsonString = response.readEntity(String.class);
    try {
      TypeFactory factory = objectMapper.getTypeFactory();
      JavaType listOfT = factory.constructCollectionType(List.class, classToExtractFromResponse);
      return objectMapper.readValue(jsonString, listOfT);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void resetBuilder() {
    this.authCookie = defaultAuthCookie;
    this.body = null;
    this.path = null;
    this.method = null;
    this.queryParams = null;
    this.cookies.clear();
    this.requestHeaders.clear();
  }

  public OptimizeRequestExecutor buildGenericRequest(final String method, final String path, final Object payload) {
    this.path = path;
    this.method = method;
    this.body = getBody(payload);
    return this;
  }

  public OptimizeRequestExecutor buildCreateAlertRequest(AlertCreationDto alert) {
    this.body = getBody(alert);
    this.path = ALERT;
    this.method = POST;
    return this;
  }

  public OptimizeRequestExecutor buildUpdateAlertRequest(String id, AlertCreationDto alert) {
    this.body = getBody(alert);
    this.path = ALERT + "/" + id;
    this.method = PUT;
    return this;
  }

  public OptimizeRequestExecutor buildGetAllAlertsRequest() {
    this.path = ALERT;
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildDeleteAlertRequest(String id) {
    this.path = ALERT + "/" + id;
    this.method = DELETE;
    return this;
  }

  public OptimizeRequestExecutor buildUpdateSingleReportRequest(String id,
                                                                ReportDefinitionDto entity) {
    switch (entity.getReportType()) {
      default:
      case PROCESS:
        return buildUpdateSingleProcessReportRequest(id, entity, null);
      case DECISION:
        return buildUpdateSingleDecisionReportRequest(id, entity, null);
    }
  }

  public OptimizeRequestExecutor buildUpdateSingleProcessReportRequest(String id,
                                                                       ReportDefinitionDto entity) {
    return buildUpdateSingleProcessReportRequest(id, entity, null);
  }

  public OptimizeRequestExecutor buildUpdateSingleProcessReportRequest(String id,
                                                                       ReportDefinitionDto entity,
                                                                       Boolean force) {
    this.path = "report/process/single/" + id;
    this.body = getBody(entity);
    this.method = PUT;
    Optional.ofNullable(force).ifPresent(value -> addSingleQueryParam("force", value));
    return this;
  }

  public OptimizeRequestExecutor buildUpdateSingleDecisionReportRequest(String id,
                                                                        ReportDefinitionDto entity) {
    return buildUpdateSingleDecisionReportRequest(id, entity, null);
  }

  public OptimizeRequestExecutor buildUpdateSingleDecisionReportRequest(String id,
                                                                        ReportDefinitionDto entity,
                                                                        Boolean force) {
    this.path = "report/decision/single/" + id;
    this.body = getBody(entity);
    this.method = PUT;
    Optional.ofNullable(force).ifPresent(value -> addSingleQueryParam("force", value));
    return this;
  }

  public OptimizeRequestExecutor buildUpdateCombinedProcessReportRequest(String id,
                                                                         ReportDefinitionDto entity) {
    return buildUpdateCombinedProcessReportRequest(id, entity, null);
  }

  public OptimizeRequestExecutor buildUpdateCombinedProcessReportRequest(String id,
                                                                         ReportDefinitionDto entity,
                                                                         Boolean force) {
    this.path = "report/process/combined/" + id;
    this.body = getBody(entity);
    this.method = PUT;
    Optional.ofNullable(force).ifPresent(value -> addSingleQueryParam("force", value));
    return this;
  }

  public OptimizeRequestExecutor buildCreateSingleProcessReportRequest() {
    return buildCreateSingleProcessReportRequest(null);
  }

  public OptimizeRequestExecutor buildCreateSingleProcessReportRequest(final SingleProcessReportDefinitionDto singleProcessReportDefinitionDto) {
    this.path = "report/process/single";
    Optional.ofNullable(singleProcessReportDefinitionDto)
      .ifPresent(definitionDto -> this.body = getBody(definitionDto));
    this.method = POST;
    return this;
  }

  public OptimizeRequestExecutor buildCreateSingleDecisionReportRequest() {
    return buildCreateSingleDecisionReportRequest(null);
  }

  public OptimizeRequestExecutor buildCreateSingleDecisionReportRequest(final SingleDecisionReportDefinitionDto singleDecisionReportDefinitionDto) {
    this.path = "report/decision/single";
    Optional.ofNullable(singleDecisionReportDefinitionDto)
      .ifPresent(definitionDto -> this.body = getBody(definitionDto));
    this.method = POST;
    return this;
  }

  public OptimizeRequestExecutor buildCreateCombinedReportRequest() {
    return buildCreateCombinedReportRequest(null);
  }

  public OptimizeRequestExecutor buildCreateCombinedReportRequest(final CombinedReportDefinitionDto combinedReportDefinitionDto) {
    this.path = "report/process/combined";
    Optional.ofNullable(combinedReportDefinitionDto).ifPresent(definitionDto -> this.body = getBody(definitionDto));
    this.method = POST;
    return this;
  }

  public OptimizeRequestExecutor buildGetReportRequest(String id) {
    this.path = "report/" + id;
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetReportDeleteConflictsRequest(String id) {
    this.path = "report/" + id + "/delete-conflicts";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildDeleteReportRequest(String id, Boolean force) {
    this.path = "report/" + id;
    this.method = DELETE;
    Optional.ofNullable(force).ifPresent(value -> addSingleQueryParam("force", value));
    return this;
  }

  public OptimizeRequestExecutor buildDeleteReportRequest(String id) {
    return buildDeleteReportRequest(id, null);
  }

  public OptimizeRequestExecutor buildGetAllPrivateReportsRequest() {
    this.method = GET;
    this.path = "/report";
    return this;
  }

  public OptimizeRequestExecutor buildEvaluateSavedReportRequest(String id) {
    this.path = "/report/" + id + "/evaluate";
    this.method = GET;
    return this;
  }

  public <T extends SingleReportDataDto> OptimizeRequestExecutor buildEvaluateSingleUnsavedReportRequest(T entity) {
    this.path = "report/evaluate";
    if (entity instanceof ProcessReportDataDto) {
      ProcessReportDataDto dataDto = (ProcessReportDataDto) entity;
      SingleProcessReportDefinitionDto definitionDto = new SingleProcessReportDefinitionDto();
      definitionDto.setData(dataDto);
      this.body = getBody(definitionDto);
    } else if (entity instanceof DecisionReportDataDto) {
      DecisionReportDataDto dataDto = (DecisionReportDataDto) entity;
      SingleDecisionReportDefinitionDto definitionDto = new SingleDecisionReportDefinitionDto();
      definitionDto.setData(dataDto);
      this.body = getBody(definitionDto);
    } else if (entity == null) {
      this.body = getBody(null);
    } else {
      throw new OptimizeIntegrationTestException("Unknown report data type!");
    }
    this.method = POST;
    return this;
  }

  public OptimizeRequestExecutor buildEvaluateCombinedUnsavedReportRequest(CombinedReportDataDto entity) {
    this.path = "report/evaluate";
    this.method = POST;
    this.body = getBody(new CombinedReportDefinitionDto(entity));
    return this;
  }

  public OptimizeRequestExecutor buildCreateDashboardRequest() {
    return buildCreateDashboardRequest(new DashboardDefinitionDto());
  }

  public OptimizeRequestExecutor buildCreateDashboardRequest(DashboardDefinitionDto dashboardDefinitionDto) {
    this.method = POST;
    this.body = Optional.ofNullable(dashboardDefinitionDto)
      .map(definitionDto -> getBody(dashboardDefinitionDto))
      .orElseGet(() -> Entity.json(""));
    this.path = "dashboard";
    return this;
  }

  public OptimizeRequestExecutor buildCreateCollectionRequest() {
    return buildCreateCollectionRequestWithPartialDefinition(null);
  }

  public OptimizeRequestExecutor buildCreateCollectionRequestWithPartialDefinition(PartialCollectionDefinitionDto partialCollectionDefinitionDto) {
    this.method = POST;
    this.body = Optional.ofNullable(partialCollectionDefinitionDto)
      .map(definitionDto -> getBody(partialCollectionDefinitionDto))
      .orElseGet(() -> Entity.json(""));
    this.path = "collection";
    return this;
  }

  public OptimizeRequestExecutor buildUpdateDashboardRequest(String id, DashboardDefinitionDto entity) {
    this.path = "dashboard/" + id;
    this.method = PUT;
    this.body = getBody(entity);
    return this;
  }

  public OptimizeRequestExecutor buildUpdatePartialCollectionRequest(String id,
                                                                     PartialCollectionDefinitionDto updateDto) {
    this.path = "collection/" + id;
    this.method = PUT;
    this.body = getBody(updateDto);
    return this;
  }

  public OptimizeRequestExecutor buildGetRolesToCollectionRequest(final String id) {
    this.path = "collection/" + id + "/role/";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildAddRoleToCollectionRequest(final String collectionId,
                                                                 final CollectionRoleDto roleDto) {
    this.path = "collection/" + collectionId + "/role/";
    this.method = POST;
    this.body = getBody(roleDto);
    return this;
  }

  public OptimizeRequestExecutor buildUpdateRoleToCollectionRequest(final String id,
                                                                    final String roleEntryId,
                                                                    final CollectionRoleUpdateDto updateDto) {
    this.path = "collection/" + id + "/role/" + roleEntryId;
    this.method = PUT;
    this.body = getBody(updateDto);
    return this;
  }


  public OptimizeRequestExecutor buildDeleteRoleToCollectionRequest(final String id,
                                                                    final String roleEntryId) {
    this.path = "collection/" + id + "/role/" + roleEntryId;
    this.method = DELETE;
    return this;
  }

  public OptimizeRequestExecutor buildGetReportsForCollectionRequest(String id) {
    this.path = "collection/" + id + "/reports/";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetDashboardRequest(String id) {
    this.path = "dashboard/" + id;
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetCollectionRequest(String id) {
    this.path = "collection/" + id;
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetAlertsForCollectionRequest(String id) {
    this.path = "collection/" + id + "/alerts/";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetCollectionDeleteConflictsRequest(String id) {
    this.path = "collection/" + id + "/delete-conflicts";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetAllEntitiesRequest() {
    this.path = "entities/";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetEntityNamesRequest(EntityNameRequestDto requestDto) {
    this.path = "entities/names";
    this.method = GET;
    this.addSingleQueryParam(EntityNameRequestDto.Fields.collectionId.name(), requestDto.getCollectionId());
    this.addSingleQueryParam(EntityNameRequestDto.Fields.dashboardId.name(), requestDto.getDashboardId());
    this.addSingleQueryParam(EntityNameRequestDto.Fields.reportId.name(), requestDto.getReportId());
    this.addSingleQueryParam(
      EntityNameRequestDto.Fields.eventBasedProcessId.name(),
      requestDto.getEventBasedProcessId()
    );
    return this;
  }

  public OptimizeRequestExecutor buildDeleteDashboardRequest(String id) {
    return buildDeleteDashboardRequest(id, false);
  }

  public OptimizeRequestExecutor buildDeleteDashboardRequest(String id, Boolean force) {
    this.path = "dashboard/" + id;
    this.method = DELETE;
    Optional.ofNullable(force).ifPresent(value -> addSingleQueryParam("force", value));
    return this;
  }

  public OptimizeRequestExecutor buildDeleteCollectionRequest(String id) {
    return buildDeleteCollectionRequest(id, false);
  }

  public OptimizeRequestExecutor buildDeleteCollectionRequest(String id, Boolean force) {
    this.path = "collection/" + id;
    this.method = DELETE;
    Optional.ofNullable(force).ifPresent(value -> addSingleQueryParam("force", value));
    return this;
  }

  public OptimizeRequestExecutor buildFindShareForReportRequest(String id) {
    this.path = "share/report/" + id;
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildFindShareForDashboardRequest(String id) {
    this.path = "share/dashboard/" + id;
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildShareDashboardRequest(DashboardShareDto share) {
    this.path = "share/dashboard";
    this.body = getBody(share);
    this.method = POST;
    return this;
  }

  public OptimizeRequestExecutor buildShareReportRequest(ReportShareDto share) {
    this.path = "share/report";
    this.body = getBody(share);
    this.method = POST;
    return this;
  }

  public OptimizeRequestExecutor buildEvaluateSharedReportRequest(String shareId) {
    this.path = "share/report/" + shareId + "/evaluate";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildEvaluateSharedDashboardReportRequest(String dashboardShareId, String reportId) {
    this.path = "share/dashboard/" + dashboardShareId + "/report/" + reportId + "/evaluate";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildEvaluateSharedDashboardRequest(String shareId) {
    this.path = "share/dashboard/" + shareId + "/evaluate";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildCheckSharingStatusRequest(ShareSearchDto shareSearchDto) {
    this.path = "share/status";
    this.method = POST;
    this.body = getBody(shareSearchDto);
    return this;
  }

  public OptimizeRequestExecutor buildCheckImportStatusRequest() {
    this.path = "/status";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetUIConfigurationRequest() {
    this.path = "/ui-configuration";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildDeleteReportShareRequest(String id) {
    this.path = "share/report/" + id;
    this.method = DELETE;
    return this;
  }

  public OptimizeRequestExecutor buildDeleteDashboardShareRequest(String id) {
    this.path = "share/dashboard/" + id;
    this.method = DELETE;
    return this;
  }

  public OptimizeRequestExecutor buildDashboardShareAuthorizationCheck(String id) {
    this.path = "share/dashboard/" + id + "/isAuthorizedToShare";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetProcessDefinitionsRequest() {
    this.path = "process-definition";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetProcessDefinitionVersionsWithTenants() {
    return buildGetProcessDefinitionVersionsWithTenants(null);
  }

  public OptimizeRequestExecutor buildGetProcessDefinitionVersionsWithTenants(final String collectionId) {
    this.path = "process-definition/definitionVersionsWithTenants";
    this.method = GET;
    addSingleQueryParam("filterByCollectionScope", collectionId);
    return this;
  }

  public OptimizeRequestExecutor buildGetProcessDefinitionXmlRequest(String key, Object version) {
    return buildGetProcessDefinitionXmlRequest(key, version, null);
  }

  public OptimizeRequestExecutor buildGetProcessDefinitionXmlRequest(String key, Object version, String tenantId) {
    this.path = "process-definition/xml";
    this.addSingleQueryParam("processDefinitionKey", key);
    this.addSingleQueryParam("processDefinitionVersion", version);
    this.addSingleQueryParam("tenantId", tenantId);
    this.method = GET;
    return this;
  }


  public OptimizeRequestExecutor buildProcessDefinitionCorrelation(BranchAnalysisQueryDto entity) {
    this.path = "analysis/correlation";
    this.method = POST;
    this.body = getBody(entity);
    return this;
  }

  public OptimizeRequestExecutor buildProcessVariableNamesRequest(ProcessVariableNameRequestDto variableRequestDto) {
    this.path = "variables/";
    this.method = POST;
    this.body = getBody(variableRequestDto);
    return this;
  }

  public OptimizeRequestExecutor buildProcessVariableValuesRequest(ProcessVariableValueRequestDto valueRequestDto) {
    this.path = "variables/values";
    this.method = POST;
    this.body = getBody(valueRequestDto);
    return this;
  }

  public OptimizeRequestExecutor buildDecisionInputVariableValuesRequest(DecisionVariableValueRequestDto requestDto) {
    this.path = "decision-variables/inputs/values";
    this.method = POST;
    this.body = getBody(requestDto);
    return this;
  }

  public OptimizeRequestExecutor buildDecisionInputVariableNamesRequest(DecisionVariableNameRequestDto variableRequestDto) {
    this.path = "decision-variables/inputs/names";
    this.method = POST;
    this.body = getBody(variableRequestDto);
    return this;
  }

  public OptimizeRequestExecutor buildDecisionOutputVariableValuesRequest(DecisionVariableValueRequestDto requestDto) {
    this.path = "decision-variables/outputs/values";
    this.method = POST;
    this.body = getBody(requestDto);
    return this;
  }

  public OptimizeRequestExecutor buildDecisionOutputVariableNamesRequest(DecisionVariableNameRequestDto variableRequestDto) {
    this.path = "decision-variables/outputs/names";
    this.method = POST;
    this.body = getBody(variableRequestDto);
    return this;
  }

  public OptimizeRequestExecutor buildGetFlowNodeNames(FlowNodeIdsToNamesRequestDto entity) {
    this.path = "flow-node/flowNodeNames";
    this.method = POST;
    this.body = getBody(entity);
    return this;
  }

  public OptimizeRequestExecutor buildCsvExportRequest(String reportId, String fileName) {
    this.path = "export/csv/" + reportId + "/" + fileName;
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetOptimizeVersionRequest() {
    this.path = "meta/version";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildLogOutRequest() {
    this.path = "authentication/logout";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildAuthTestRequest() {
    this.path = "authentication/test";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildValidateAndStoreLicenseRequest(String license) {
    this.path = "license/validate-and-store";
    this.method = POST;
    this.body = Entity.entity(license, MediaType.TEXT_PLAIN);
    return this;
  }

  public OptimizeRequestExecutor buildValidateLicenseRequest() {
    this.path = "license/validate";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetDefinitionByTypeAndKeyRequest(final String type, final String key) {
    this.path = "/definition/" + type + "/" + key;
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetDefinitions() {
    this.path = "/definition";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetDefinitionsGroupedByTenant() {
    this.path = "/definition/_groupByTenant";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetDecisionDefinitionsRequest() {
    this.path = "decision-definition";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetDecisionDefinitionVersionsWithTenants() {
    return buildGetDecisionDefinitionVersionsWithTenants(null);
  }

  public OptimizeRequestExecutor buildGetDecisionDefinitionVersionsWithTenants(final String collectionId) {
    this.path = "decision-definition/definitionVersionsWithTenants";
    this.method = GET;
    addSingleQueryParam("filterByCollectionScope", collectionId);
    return this;
  }

  public OptimizeRequestExecutor buildGetDecisionDefinitionXmlRequest(String key, Object version) {
    return buildGetDecisionDefinitionXmlRequest(key, version, null);
  }

  public OptimizeRequestExecutor buildGetDecisionDefinitionXmlRequest(String key, Object version, String tenantId) {
    this.path = "decision-definition/xml";
    this.addSingleQueryParam("key", key);
    this.addSingleQueryParam("version", version);
    this.addSingleQueryParam("tenantId", tenantId);
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetLocalizationRequest(final String localeCode) {
    this.path = "localization";
    this.method = GET;
    this.addSingleQueryParam("localeCode", localeCode);
    return this;
  }

  public OptimizeRequestExecutor buildGetLocalizedWhatsNewMarkdownRequest(final String localeCode) {
    this.path = "localization/whatsnew";
    this.method = GET;
    this.addSingleQueryParam("localeCode", localeCode);
    return this;
  }

  public OptimizeRequestExecutor buildFlowNodeOutliersRequest(String key,
                                                              List<String> version,
                                                              List<String> tenantIds) {
    this.path = "analysis/flowNodeOutliers";
    this.method = GET;
    this.addSingleQueryParam("processDefinitionKey", key);
    this.addSingleQueryParam("processDefinitionVersions", version);
    this.addSingleQueryParam("tenantIds", tenantIds);
    return this;
  }

  public OptimizeRequestExecutor buildFlowNodeDurationChartRequest(String key,
                                                                   List<String> version,
                                                                   List<String> tenantIds,
                                                                   String flowNodeId) {
    return buildFlowNodeDurationChartRequest(key, version, flowNodeId, tenantIds, null, null);
  }

  public OptimizeRequestExecutor buildFlowNodeDurationChartRequest(String key,
                                                                   List<String> version,
                                                                   String flowNodeId,
                                                                   List<String> tenantIds,
                                                                   Long lowerOutlierBound,
                                                                   Long higherOutlierBound) {
    this.path = "analysis/durationChart";
    this.method = GET;
    this.addSingleQueryParam("processDefinitionKey", key);
    this.addSingleQueryParam("processDefinitionVersions", version);
    this.addSingleQueryParam("flowNodeId", flowNodeId);
    this.addSingleQueryParam("tenantIds", tenantIds);
    this.addSingleQueryParam("lowerOutlierBound", lowerOutlierBound);
    this.addSingleQueryParam("higherOutlierBound", higherOutlierBound);
    return this;
  }

  public OptimizeRequestExecutor buildSignificantOutlierVariableTermsRequest(String key,
                                                                             List<String> version,
                                                                             List<String> tenantIds,
                                                                             String flowNodeId,
                                                                             Long lowerOutlierBound,
                                                                             Long higherOutlierBound) {
    this.path = "analysis/significantOutlierVariableTerms";
    this.method = GET;
    this.addSingleQueryParam("processDefinitionKey", key);
    this.addSingleQueryParam("processDefinitionVersions", version);
    this.addSingleQueryParam("flowNodeId", flowNodeId);
    this.addSingleQueryParam("tenantIds", tenantIds);
    this.addSingleQueryParam("lowerOutlierBound", lowerOutlierBound);
    this.addSingleQueryParam("higherOutlierBound", higherOutlierBound);
    return this;
  }

  public OptimizeRequestExecutor buildSignificantOutlierVariableTermsInstanceIdsRequest(String key,
                                                                                        List<String> version,
                                                                                        List<String> tenantIds,
                                                                                        String flowNodeId,
                                                                                        Long lowerOutlierBound,
                                                                                        Long higherOutlierBound,
                                                                                        String variableName,
                                                                                        String variableTerm) {
    this.path = "analysis/significantOutlierVariableTerms/processInstanceIdsExport";
    this.method = GET;
    this.addSingleQueryParam("processDefinitionKey", key);
    this.addSingleQueryParam("processDefinitionVersions", version);
    this.addSingleQueryParam("flowNodeId", flowNodeId);
    this.addSingleQueryParam("tenantIds", tenantIds);
    this.addSingleQueryParam("lowerOutlierBound", lowerOutlierBound);
    this.addSingleQueryParam("higherOutlierBound", higherOutlierBound);
    this.addSingleQueryParam("variableName", variableName);
    this.addSingleQueryParam("variableTerm", variableTerm);
    return this;
  }

  public OptimizeRequestExecutor buildCopyReportRequest(String id) {
    return buildCopyReportRequest(id, null);
  }

  public OptimizeRequestExecutor buildCopyReportRequest(String id, String collectionId) {
    this.path = "report/" + id + "/copy";
    this.method = POST;
    Optional.ofNullable(collectionId).ifPresent(value -> addSingleQueryParam("collectionId", value));
    return this;
  }

  public OptimizeRequestExecutor buildCopyDashboardRequest(String id) {
    return buildCopyDashboardRequest(id, null);
  }

  public OptimizeRequestExecutor buildCopyDashboardRequest(String id, String collectionId) {
    this.path = "dashboard/" + id + "/copy";
    this.method = POST;
    Optional.ofNullable(collectionId).ifPresent(value -> addSingleQueryParam("collectionId", value));
    return this;
  }

  public OptimizeRequestExecutor buildGetIsEventProcessEnabledRequest() {
    this.path = "eventBasedProcess/isEnabled";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildCreateEventProcessMappingRequest(EventProcessMappingDto eventProcessMappingDto) {
    this.path = "eventBasedProcess/";
    this.body = getBody(eventProcessMappingDto);
    this.method = POST;
    return this;
  }

  public OptimizeRequestExecutor buildGetEventProcessMappingRequest(String eventProcessId) {
    this.path = "eventBasedProcess/" + eventProcessId;
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildGetAllEventProcessMappingsRequests() {
    this.path = "eventBasedProcess";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildUpdateEventProcessMappingRequest(String eventProcessId,
                                                                       EventProcessMappingDto eventProcessMappingDto) {
    this.path = "eventBasedProcess/" + eventProcessId;
    this.body = getBody(eventProcessMappingDto);
    this.method = PUT;
    return this;
  }

  public OptimizeRequestExecutor buildPublishEventProcessMappingRequest(String eventProcessId) {
    this.path = "eventBasedProcess/" + eventProcessId + "/_publish";
    this.method = POST;
    return this;
  }

  public OptimizeRequestExecutor buildCancelPublishEventProcessMappingRequest(String eventProcessId) {
    this.path = "eventBasedProcess/" + eventProcessId + "/_cancelPublish";
    this.method = POST;
    return this;
  }

  public OptimizeRequestExecutor buildGetDeleteConflictsForEventProcessMappingRequest(String eventProcessId) {
    this.path = "eventBasedProcess/" + eventProcessId + "/delete-conflicts";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildDeleteEventProcessMappingRequest(String eventProcessId) {
    this.path = "eventBasedProcess/" + eventProcessId;
    this.method = DELETE;
    return this;
  }

  public OptimizeRequestExecutor buildGetScopeForCollectionRequest(final String collectionId) {
    this.path = "collection/" + collectionId + "/scope";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildAddScopeEntryToCollectionRequest(String collectionId,
                                                                       CollectionScopeEntryDto entryDto) {
    return buildAddScopeEntriesToCollectionRequest(collectionId, Collections.singletonList(entryDto));
  }

  public OptimizeRequestExecutor buildAddScopeEntriesToCollectionRequest(String collectionId,
                                                                         List<CollectionScopeEntryDto> entryDto) {
    this.path = "collection/" + collectionId + "/scope";
    this.method = PUT;
    this.body = getBody(entryDto);
    return this;
  }

  public OptimizeRequestExecutor buildDeleteScopeEntryFromCollectionRequest(String collectionId,
                                                                            String scopeEntryId) {
    return buildDeleteScopeEntryFromCollectionRequest(collectionId, scopeEntryId, false);
  }

  public OptimizeRequestExecutor buildDeleteScopeEntryFromCollectionRequest(String collectionId,
                                                                            String scopeEntryId,
                                                                            Boolean force) {
    this.path = "collection/" + collectionId + "/scope/" + scopeEntryId;
    this.method = DELETE;
    Optional.ofNullable(force).ifPresent(value -> addSingleQueryParam("force", value));
    return this;
  }

  public OptimizeRequestExecutor buildGetScopeDeletionConflictsRequest(final String collectionId,
                                                                       final String scopeEntryId) {
    this.path = "collection/" + collectionId + "/scope/" + scopeEntryId + "/delete-conflicts";
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildUpdateCollectionScopeEntryRequest(String collectionId,
                                                                        String scopeEntryId,
                                                                        CollectionScopeEntryUpdateDto entryDto) {
    return buildUpdateCollectionScopeEntryRequest(collectionId, scopeEntryId, entryDto, false);
  }

  public OptimizeRequestExecutor buildUpdateCollectionScopeEntryRequest(String collectionId,
                                                                        String scopeEntryId,
                                                                        CollectionScopeEntryUpdateDto entryDto,
                                                                        Boolean force) {
    this.path = "collection/" + collectionId + "/scope/" + scopeEntryId;
    this.method = PUT;
    this.body = getBody(entryDto);
    Optional.ofNullable(force).ifPresent(value -> addSingleQueryParam("force", value));
    return this;
  }

  public OptimizeRequestExecutor buildSearchForIdentities(final String searchTerms) {
    return buildSearchForIdentities(searchTerms, null);
  }

  public OptimizeRequestExecutor buildSearchForIdentities(final String searchTerms, final Integer limit) {
    this.path = "identity/search";
    this.method = GET;
    addSingleQueryParam("terms", searchTerms);
    Optional.ofNullable(limit).ifPresent(limitValue -> addSingleQueryParam("limit", limitValue));
    return this;
  }

  public OptimizeRequestExecutor buildCopyCollectionRequest(String collectionId) {
    this.path = "/collection/" + collectionId + "/copy";
    this.method = POST;
    return this;
  }

  public OptimizeRequestExecutor buildIngestSingleEvent(final EventDto eventDto, final String secret) {
    this.path = "ingestion/event";
    this.method = PUT;
    addSingleHeader(OPTIMIZE_API_SECRET_HEADER, secret);
    this.body = getBody(eventDto);
    return this;
  }

  public OptimizeRequestExecutor buildIngestEventBatch(final List<EventDto> eventDtos, final String secret) {
    this.path = "ingestion/event/batch";
    this.method = PUT;
    addSingleHeader(OPTIMIZE_API_SECRET_HEADER, secret);
    this.body = getBody(eventDtos);
    return this;
  }

  public OptimizeRequestExecutor buildGetOnboardingStateForKey(final String key) {
    this.path = "onboarding/" + key;
    this.method = GET;
    return this;
  }

  public OptimizeRequestExecutor buildSetOnboardingStateForKey(final String key, final boolean seen) {
    this.path = "onboarding/" + key;
    this.method = PUT;
    this.body = getBody(new OnboardingStateRestDto(seen));
    return this;
  }

  public OptimizeRequestExecutor buildGetEventCountRequest(EventCountRequestDto eventCountRequestDto) {
    this.path = "event/count";
    this.method = GET;
    Optional.ofNullable(eventCountRequestDto).map(EventCountRequestDto::getSearchTerm).ifPresent(term -> addSingleQueryParam("searchTerm", term));
    Optional.ofNullable(eventCountRequestDto).map(EventCountRequestDto::getOrderBy).ifPresent(orderBy -> addSingleQueryParam("orderBy", orderBy));
    Optional.ofNullable(eventCountRequestDto).map(EventCountRequestDto::getSortOrder).ifPresent(sortOrder -> addSingleQueryParam("sortOrder", sortOrder));
    return this;
  }

  public OptimizeRequestExecutor buildPostEventCountRequest(EventCountRequestDto eventCountRequestDto,
                                                            EventCountSuggestionsRequestDto eventCountSuggestionsRequestDto) {
    this.path = "event/count";
    this.method = POST;
    Optional.ofNullable(eventCountRequestDto).map(EventCountRequestDto::getSearchTerm).ifPresent(term -> addSingleQueryParam("searchTerm", term));
    Optional.ofNullable(eventCountRequestDto).map(EventCountRequestDto::getOrderBy).ifPresent(orderBy -> addSingleQueryParam("orderBy", orderBy));
    Optional.ofNullable(eventCountRequestDto).map(EventCountRequestDto::getSortOrder).ifPresent(sortOrder -> addSingleQueryParam("sortOrder", sortOrder));
    this.body = Optional.ofNullable(eventCountSuggestionsRequestDto).map(this::getBody).orElse(null);
    return this;
  }

  private Entity getBody(Object entity) {
    try {
      return entity == null ? Entity.json("") : Entity.json(objectMapper.writeValueAsString(entity));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Couldn't serialize request" + e.getMessage(), e);
    }
  }

  private String authenticateUserRequest(String username, String password) {
    CredentialsDto entity = new CredentialsDto();
    entity.setUsername(username);
    entity.setPassword(password);

    Response response = client.path("authentication")
      .request()
      .post(Entity.json(entity));
    return AuthCookieService.createOptimizeAuthCookieValue(response.readEntity(String.class));
  }
}
