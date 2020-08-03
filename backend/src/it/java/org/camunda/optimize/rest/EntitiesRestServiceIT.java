/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.rest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.assertj.core.groups.Tuple;
import org.camunda.optimize.AbstractIT;
import org.camunda.optimize.dto.optimize.IdentityDto;
import org.camunda.optimize.dto.optimize.IdentityType;
import org.camunda.optimize.dto.optimize.ReportType;
import org.camunda.optimize.dto.optimize.RoleType;
import org.camunda.optimize.dto.optimize.query.IdDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionRoleDto;
import org.camunda.optimize.dto.optimize.query.collection.PartialCollectionDefinitionDto;
import org.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionDto;
import org.camunda.optimize.dto.optimize.query.entity.EntityDto;
import org.camunda.optimize.dto.optimize.query.entity.EntityType;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.sorting.SortOrder;
import org.camunda.optimize.dto.optimize.rest.sorting.EntitySorter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.optimize.dto.optimize.query.entity.EntityDto.Fields.entityType;
import static org.camunda.optimize.dto.optimize.query.entity.EntityDto.Fields.lastModified;
import static org.camunda.optimize.dto.optimize.query.entity.EntityDto.Fields.lastModifier;
import static org.camunda.optimize.dto.optimize.query.entity.EntityDto.Fields.name;
import static org.camunda.optimize.rest.RestTestUtil.getOffsetDiffInHours;
import static org.camunda.optimize.rest.constants.RestConstants.X_OPTIMIZE_CLIENT_TIMEZONE;
import static org.camunda.optimize.service.es.writer.CollectionWriter.DEFAULT_COLLECTION_NAME;
import static org.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static org.camunda.optimize.test.it.extension.TestEmbeddedCamundaOptimize.DEFAULT_PASSWORD;
import static org.camunda.optimize.test.it.extension.TestEmbeddedCamundaOptimize.DEFAULT_USERNAME;
import static org.camunda.optimize.test.util.DateCreationFreezer.dateFreezer;
import static org.camunda.optimize.test.util.ProcessReportDataBuilderHelper.createCombinedReportData;

public class EntitiesRestServiceIT extends AbstractIT {

  @Test
  public void getEntities_WithoutAuthentication() {
    // when
    Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .withoutAuthentication()
      .buildGetAllPrivateReportsRequest()
      .execute();

    // then the status code is not authorized
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void getEntities_ReturnsMyUsersReports() {
    //given
    addSingleReportToOptimize("B Report", ReportType.PROCESS);
    addSingleReportToOptimize("A Report", ReportType.DECISION);
    addCombinedReport("D Combined");

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<EntityDto> privateEntities = entitiesClient.getAllEntities();

    // then
    assertThat(privateEntities)
      .hasSize(3)
      .extracting(EntityDto::getReportType, EntityDto::getCombined)
      .containsExactlyInAnyOrder(
        Tuple.tuple(ReportType.PROCESS, true),
        Tuple.tuple(ReportType.PROCESS, false),
        Tuple.tuple(ReportType.DECISION, false)
      );
  }

  @Test
  public void getEntities_adoptTimezoneFromHeader() {
    //given
    OffsetDateTime now = dateFreezer().timezone("Europe/Berlin").freezeDateAndReturn();

    addSingleReportToOptimize("My Report", ReportType.PROCESS);
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<EntityDto> privateEntities = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildGetAllEntitiesRequest()
      .addSingleHeader(X_OPTIMIZE_CLIENT_TIMEZONE, "Europe/London")
      .executeAndReturnList(EntityDto.class, Response.Status.OK.getStatusCode());

    // then
    assertThat(privateEntities).isNotNull().hasSize(1);
    EntityDto entityDto = privateEntities.get(0);
    assertThat(entityDto.getCreated()).isEqualTo(now);
    assertThat(entityDto.getLastModified()).isEqualTo(now);
    assertThat(getOffsetDiffInHours(entityDto.getCreated(), now)).isEqualTo(1.);
    assertThat(getOffsetDiffInHours(entityDto.getLastModified(), now)).isEqualTo(1.);
  }

  @Test
  public void getEntities_DoesNotReturnOtherUsersReports() {
    //given
    engineIntegrationExtension.addUser("kermit", "kermit");
    engineIntegrationExtension.grantUserOptimizeAccess("kermit");
    addSingleReportToOptimize("B Report", ReportType.PROCESS, null, "kermit");
    addSingleReportToOptimize("A Report", ReportType.DECISION);
    addCombinedReport("D Combined");

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when (default user)
    final List<EntityDto> defaultUserEntities = entitiesClient.getAllEntities();

    // then
    assertThat(defaultUserEntities)
      .hasSize(2)
      .extracting(EntityDto::getName)
      .containsExactlyInAnyOrder("A Report", "D Combined");

    // when
    final List<EntityDto> kermitUserEntities = entitiesClient.getAllEntitiesAsUser(KERMIT_USER, KERMIT_USER);

    // then
    assertThat(kermitUserEntities)
      .hasSize(1)
      .extracting(EntityDto::getName)
      .containsExactly("B Report");
  }

  @Test
  public void getEntities_emptyDefinitionKeyIsHandledAsEmptyReport() {
    // this is a regression test that could occur for old empty reports
    // see https://jira.camunda.com/browse/OPT-3496

    // given
    SingleProcessReportDefinitionDto singleProcessReportDefinitionDto = new SingleProcessReportDefinitionDto();
    singleProcessReportDefinitionDto.setName("empty");
    // an empty string definition key caused trouble
    singleProcessReportDefinitionDto.getData().setProcessDefinitionKey("");
    reportClient.createSingleProcessReport(singleProcessReportDefinitionDto);

    // when (default user)
    final List<EntityDto> defaultUserEntities = entitiesClient.getAllEntities();

    // then
    assertThat(defaultUserEntities)
      .hasSize(1)
      .extracting(EntityDto::getName)
      .containsExactly(singleProcessReportDefinitionDto.getName());
  }

  @Test
  public void getEntities_ReturnsMyUsersDashboards() {
    //given
    addDashboardToOptimize("A Dashboard");
    addDashboardToOptimize("B Dashboard");

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<EntityDto> privateEntities = entitiesClient.getAllEntities();

    // then
    assertThat(privateEntities).hasSize(2);
  }

  @Test
  public void getEntities_DoesNotReturnOtherUsersDashboards() {
    //given
    engineIntegrationExtension.addUser("kermit", "kermit");
    engineIntegrationExtension.grantUserOptimizeAccess("kermit");
    addDashboardToOptimize("A Dashboard");
    addDashboardToOptimize("B Dashboard", null, "kermit");

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when (default user)
    final List<EntityDto> defaultUserEntities = entitiesClient.getAllEntities();

    // then
    assertThat(defaultUserEntities)
      .hasSize(1)
      .extracting(EntityDto::getName)
      .containsExactly("A Dashboard");

    // when
    final List<EntityDto> kermitUserEntities = entitiesClient.getAllEntitiesAsUser(KERMIT_USER, KERMIT_USER);

    // then
    assertThat(kermitUserEntities)
      .hasSize(1)
      .extracting(EntityDto::getName)
      .containsExactly("B Dashboard");
  }

  @Test
  public void getEntities_ReturnsCollections() {
    //given
    collectionClient.createNewCollection();
    collectionClient.createNewCollection();

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<EntityDto> privateEntities = entitiesClient.getAllEntities();

    // then
    assertThat(privateEntities).hasSize(2);
  }

  @Test
  public void getEntities_DoesNotReturnEntitiesInCollections() {
    // given
    final String collectionId = collectionClient.createNewCollection();

    addSingleReportToOptimize("A Report", ReportType.DECISION);
    addSingleReportToOptimize("B Report", ReportType.PROCESS, collectionId, DEFAULT_USERNAME);
    addDashboardToOptimize("C Dashboard", collectionId, DEFAULT_USERNAME);
    addCombinedReport("D Combined");

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<EntityDto> defaultUserEntities = entitiesClient.getAllEntities();

    // then
    assertThat(defaultUserEntities)
      .hasSize(3)
      .extracting(EntityDto::getName)
      .containsExactlyInAnyOrder("A Report", "D Combined", DEFAULT_COLLECTION_NAME);
  }

  @Test
  public void getEntities__noSortApplied_OrderedByTypeAndLastModified() {
    //given
    addCollection("B Collection");
    addCollection("A Collection");
    addSingleReportToOptimize("D Report", ReportType.PROCESS);
    addSingleReportToOptimize("C Report", ReportType.DECISION);
    addDashboardToOptimize("B Dashboard");
    addDashboardToOptimize("A Dashboard");

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<EntityDto> entities = entitiesClient.getAllEntities();

    // then
    assertThat(entities)
      .hasSize(6)
      .extracting(EntityDto::getName, EntityDto::getEntityType)
      .containsExactly(
        Tuple.tuple("A Collection", EntityType.COLLECTION),
        Tuple.tuple("B Collection", EntityType.COLLECTION),
        Tuple.tuple("A Dashboard", EntityType.DASHBOARD),
        Tuple.tuple("B Dashboard", EntityType.DASHBOARD),
        Tuple.tuple("C Report", EntityType.REPORT),
        Tuple.tuple("D Report", EntityType.REPORT)
      );
  }

  @Test
  public void getEntities_IncludesCollectionSubEntityCountsIfThereAreNoEntities() {
    // given
    collectionClient.createNewCollection();

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<EntityDto> defaultUserEntities = entitiesClient.getAllEntities();

    // then
    assertThat(defaultUserEntities)
      .hasSize(1)
      .allSatisfy(entry -> assertThat(entry.getData().getSubEntityCounts())
        .hasSize(2)
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
          EntityType.REPORT, 0L,
          EntityType.DASHBOARD, 0L
        ))
      );
  }

  @Test
  public void getEntities_IncludesCollectionSubEntityCounts() {
    // given
    final String collectionId = collectionClient.createNewCollection();

    addSingleReportToOptimize("A Report", ReportType.DECISION, collectionId, DEFAULT_USERNAME);
    addSingleReportToOptimize("B Report", ReportType.PROCESS, collectionId, DEFAULT_USERNAME);
    addDashboardToOptimize("C Dashboard", collectionId, DEFAULT_USERNAME);
    addCombinedReport("D Combined", collectionId);

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<EntityDto> defaultUserEntities = entitiesClient.getAllEntities();

    // then
    assertThat(defaultUserEntities)
      .hasSize(1)
      .allSatisfy(entry -> assertThat(entry.getData().getSubEntityCounts())
        .hasSize(2)
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
          EntityType.REPORT, 3L,
          EntityType.DASHBOARD, 1L
        ))
      );
  }

  @Test
  public void getEntities_IncludesCollectionRoleCountsByDefault() {
    // given
    collectionClient.createNewCollection();

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<EntityDto> defaultUserEntities = entitiesClient.getAllEntities();

    // then
    assertThat(defaultUserEntities)
      .hasSize(1)
      .allSatisfy(entry -> assertThat(entry.getData().getRoleCounts())
        .hasSize(2)
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
          IdentityType.USER, 1L,
          IdentityType.GROUP, 0L
        ))
      );
  }

  @Test
  public void getEntities_IncludesCollectionRoleCounts() {
    // given
    final String collectionId = collectionClient.createNewCollection();
    final String user1 = "user1";
    authorizationClient.addUserAndGrantOptimizeAccess(user1);
    addRoleToCollection(collectionId, user1, IdentityType.USER);
    final String groupA = "groupA";
    authorizationClient.createGroupAndGrantOptimizeAccess(groupA, groupA);
    addRoleToCollection(collectionId, groupA, IdentityType.GROUP);
    final String groupB = "groupB";
    authorizationClient.createGroupAndGrantOptimizeAccess(groupB, groupB);
    addRoleToCollection(collectionId, groupB, IdentityType.GROUP);
    final String groupC = "groupC";
    authorizationClient.createGroupAndGrantOptimizeAccess(groupC, groupC);
    addRoleToCollection(collectionId, groupC, IdentityType.GROUP);

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<EntityDto> defaultUserEntities = entitiesClient.getAllEntities();

    // then
    assertThat(defaultUserEntities)
      .hasSize(1)
      .allSatisfy(entry -> assertThat(entry.getData().getRoleCounts())
        .hasSize(2)
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
          IdentityType.USER, 2L,
          IdentityType.GROUP, 3L
        ))
      );
  }

  @Test
  public void getEntities_IncludesPrivateCombinedReportSubEntityCounts() {
    // given
    final String reportId1 = addSingleReportToOptimize("A Report", ReportType.PROCESS);
    final String reportId2 = addSingleReportToOptimize("B Report", ReportType.PROCESS);
    final String combinedReportId = addCombinedReport("D Combined");

    final CombinedReportDefinitionDto combinedReportUpdate = new CombinedReportDefinitionDto();
    combinedReportUpdate.setData(createCombinedReportData(reportId1, reportId2));
    reportClient.updateCombinedReport(combinedReportId, Lists.newArrayList(reportId1, reportId2));

    // when
    final List<EntityDto> defaultUserEntities = entitiesClient.getAllEntities();

    // then
    assertThat(defaultUserEntities)
      .hasSize(3)
      .filteredOn(EntityDto::getCombined)
      .hasSize(1)
      .allSatisfy(entry -> assertThat(entry.getData().getSubEntityCounts())
        .hasSize(1)
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
          EntityType.REPORT, 2L
        ))
      );
  }

  @ParameterizedTest(name = "sortBy={0}, sortOrder={1}")
  @MethodSource("sortParamsAndExpectedComparator")
  public void getEntities_resultsAreSortedAccordingToExpectedComparator(String sortBy, SortOrder sortOrder,
                                                                        Comparator<EntityDto> expectedComparator) {
    //given
    addCollection("B Collection");
    addCollection("A Collection");
    addSingleReportToOptimize("D Report", ReportType.PROCESS);
    addSingleReportToOptimize("C Report", ReportType.DECISION);
    addDashboardToOptimize("B Dashboard");
    addDashboardToOptimize("A Dashboard");

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // given
    EntitySorter sorter = entitySorter(sortBy, sortOrder);

    // when
    final List<EntityDto> allEntities = entitiesClient.getAllEntities(sorter);

    // then
    assertThat(allEntities)
      .hasSize(6)
      .isSortedAccordingTo(expectedComparator);
  }

  @Test
  public void getEntities_unresolvableResultsAreSortedAccordingToDefaultComparator() {
    //given
    addCollection("An Entity");
    addCollection("An Entity");
    addSingleReportToOptimize("An Entity", ReportType.PROCESS);
    addSingleReportToOptimize("An Entity", ReportType.DECISION);
    addDashboardToOptimize("An Entity");
    addDashboardToOptimize("An Entity");

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // given
    EntitySorter sorter = entitySorter(name, SortOrder.ASC);
    final Comparator<EntityDto> expectedComparator = Comparator.comparing(EntityDto::getName)
      .thenComparing(EntityDto::getEntityType)
      .thenComparing(Comparator.comparing(EntityDto::getLastModified).reversed());

    // when
    final List<EntityDto> allEntities = entitiesClient.getAllEntities(sorter);

    // then
    assertThat(allEntities)
      .hasSize(6)
      .isSortedAccordingTo(expectedComparator);
  }

  @Test
  public void getEntities_resultsAreSortedInAscendingOrderIfNoOrderSupplied() {
    //given
    addCollection("A Entity");
    addCollection("B Entity");
    addSingleReportToOptimize("C Entity", ReportType.PROCESS);
    addSingleReportToOptimize("D Entity", ReportType.DECISION);
    addDashboardToOptimize("E Entity");
    addDashboardToOptimize("F Entity");

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // given
    EntitySorter sorter = entitySorter(name, null);
    final Comparator<EntityDto> expectedComparator = Comparator.comparing(EntityDto::getName);

    // when
    final List<EntityDto> allEntities = entitiesClient.getAllEntities(sorter);

    // then
    assertThat(allEntities)
      .hasSize(6)
      .isSortedAccordingTo(expectedComparator);
  }

  @Test
  public void getEntities_invalidSortByParameterPassed() {
    // given a sortBy field which is not supported
    EntitySorter sorter = entitySorter(EntityDto.Fields.currentUserRole, SortOrder.ASC);

    // when
    final Response response = embeddedOptimizeExtension.getRequestExecutor()
      .buildGetAllEntitiesRequest(sorter)
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void getEntities_sortOrderSuppliedWithNoSortByField() {
    // given
    EntitySorter sorter = entitySorter(null, SortOrder.ASC);

    // when
    final Response response = embeddedOptimizeExtension.getRequestExecutor()
      .buildGetAllEntitiesRequest(sorter)
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  private String addCollection(final String collectionName) {
    final String collectionId = collectionClient.createNewCollection();
    collectionClient.updateCollection(collectionId, new PartialCollectionDefinitionDto(collectionName));
    return collectionId;
  }

  private void addRoleToCollection(final String collectionId,
                                   final String identityId,
                                   final IdentityType identityType) {

    final CollectionRoleDto roleDto = new CollectionRoleDto(
      identityType.equals(IdentityType.USER)
        ? new IdentityDto(identityId, IdentityType.USER)
        : new IdentityDto(identityId, IdentityType.GROUP),
      RoleType.EDITOR
    );
    collectionClient.addRoleToCollection(collectionId, roleDto);
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

  private EntitySorter entitySorter(final String sortBy, final SortOrder sortOrder) {
    final EntitySorter sorter = new EntitySorter();
    sorter.setSortBy(sortBy);
    sorter.setSortOrder(sortOrder);
    return sorter;
  }

  private static Stream<Arguments> sortParamsAndExpectedComparator() {
    return Stream.of(
      Arguments.of(name, SortOrder.ASC, Comparator.comparing(EntityDto::getName)),
      Arguments.of(name, SortOrder.DESC, Comparator.comparing(EntityDto::getName).reversed()),
      Arguments.of(entityType, SortOrder.ASC, Comparator.comparing(EntityDto::getEntityType)),
      Arguments.of(entityType, SortOrder.DESC, Comparator.comparing(EntityDto::getEntityType).reversed()),
      Arguments.of(lastModified, SortOrder.ASC, Comparator.comparing(EntityDto::getLastModified)),
      Arguments.of(lastModified, SortOrder.DESC, Comparator.comparing(EntityDto::getLastModified).reversed()),
      Arguments.of(lastModifier, SortOrder.ASC, Comparator.comparing(EntityDto::getLastModifier)),
      Arguments.of(lastModifier, SortOrder.DESC, Comparator.comparing(EntityDto::getLastModifier).reversed())
    );
  }

}
