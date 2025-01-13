/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.processinstance;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.security.configuration.ConfiguredUser;
import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.protocol.record.Assertions;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceModificationIntent;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import io.camunda.zeebe.protocol.record.value.UserRecordValue;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import io.camunda.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.util.List;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

public class ProcessInstanceModificationModifyAuthorizationTest {

  public static final String JOB_TYPE = "jobType";
  public static final String SERVICE_TASK_ID = "serviceTask";
  private static final String PROCESS_ID = "processId";
  private static final ConfiguredUser DEFAULT_USER =
      new ConfiguredUser(
          UUID.randomUUID().toString(),
          UUID.randomUUID().toString(),
          UUID.randomUUID().toString(),
          UUID.randomUUID().toString());

  @ClassRule
  public static final EngineRule ENGINE =
      EngineRule.singlePartition()
          .withSecurityConfig(cfg -> cfg.getAuthorizations().setEnabled(true))
          .withSecurityConfig(cfg -> cfg.getInitialization().setUsers(List.of(DEFAULT_USER)));

  @Rule public final TestWatcher recordingExporterTestWatcher = new RecordingExporterTestWatcher();

  @BeforeClass
  public static void beforeAll() {
    ENGINE
        .deployment()
        .withXmlResource(
            "process.bpmn",
            Bpmn.createExecutableProcess(PROCESS_ID)
                .startEvent()
                .serviceTask(SERVICE_TASK_ID, t -> t.zeebeJobType(JOB_TYPE))
                .endEvent()
                .done())
        .deploy(DEFAULT_USER.getUsername())
        .getValue();
  }

  @Test
  public void shouldBeAuthorizedToModifyProcessWithDefaultUser() {
    // given
    final var processInstanceKey = createProcessInstance();
    final var serviceTaskKey = getServiceTaskKey(processInstanceKey);

    // when
    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .modification()
        .terminateElement(serviceTaskKey)
        .modify(DEFAULT_USER.getUsername());

    // then
    assertThat(
            RecordingExporter.processInstanceModificationRecords(
                    ProcessInstanceModificationIntent.MODIFIED)
                .withProcessInstanceKey(processInstanceKey)
                .exists())
        .isTrue();
  }

  @Test
  public void shouldBeAuthorizedToModifyProcessWithUser() {
    // given
    final var processInstanceKey = createProcessInstance();
    final var serviceTaskKey = getServiceTaskKey(processInstanceKey);
    final var user = createUser();
    addPermissionsToUser(
        user.getUserKey(),
        AuthorizationResourceType.PROCESS_DEFINITION,
        PermissionType.UPDATE_PROCESS_INSTANCE,
        PROCESS_ID);

    // when
    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .modification()
        .terminateElement(serviceTaskKey)
        .modify(user.getUsername());

    // then
    assertThat(
            RecordingExporter.processInstanceModificationRecords(
                    ProcessInstanceModificationIntent.MODIFIED)
                .withProcessInstanceKey(processInstanceKey)
                .exists())
        .isTrue();
  }

  @Test
  public void shouldBeUnauthorizedToModifyProcessIfNoPermissions() {
    // given
    final var processInstanceKey = createProcessInstance();
    final var serviceTaskKey = getServiceTaskKey(processInstanceKey);
    final var user = createUser();

    // when
    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .modification()
        .terminateElement(serviceTaskKey)
        .expectRejection()
        .modify(user.getUsername());

    // then
    Assertions.assertThat(
            RecordingExporter.processInstanceModificationRecords()
                .onlyCommandRejections()
                .getFirst())
        .hasRejectionType(RejectionType.FORBIDDEN)
        .hasRejectionReason(
            "Insufficient permissions to perform operation 'UPDATE_PROCESS_INSTANCE' on resource 'PROCESS_DEFINITION', required resource identifiers are one of '[*, %s]'"
                .formatted(PROCESS_ID));
  }

  private static UserRecordValue createUser() {
    return ENGINE
        .user()
        .newUser(UUID.randomUUID().toString())
        .withPassword(UUID.randomUUID().toString())
        .withName(UUID.randomUUID().toString())
        .withEmail(UUID.randomUUID().toString())
        .create()
        .getValue();
  }

  private void addPermissionsToUser(
      final long userKey,
      final AuthorizationResourceType authorization,
      final PermissionType permissionType,
      final String... resourceIds) {
    ENGINE
        .authorization()
        .permission()
        .withOwnerKey(userKey)
        .withResourceType(authorization)
        .withPermission(permissionType, resourceIds)
        .add(DEFAULT_USER.getUsername());
  }

  private long createProcessInstance() {
    return ENGINE.processInstance().ofBpmnProcessId(PROCESS_ID).create(DEFAULT_USER.getUsername());
  }

  private long getServiceTaskKey(final long processInstanceKey) {
    return RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_ACTIVATED)
        .withProcessInstanceKey(processInstanceKey)
        .withElementId(SERVICE_TASK_ID)
        .limit(1)
        .getFirst()
        .getKey();
  }
}
