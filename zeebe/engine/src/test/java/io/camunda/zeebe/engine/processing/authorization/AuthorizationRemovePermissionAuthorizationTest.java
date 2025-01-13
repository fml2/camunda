/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.security.configuration.ConfiguredUser;
import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.protocol.record.Assertions;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.AuthorizationIntent;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import io.camunda.zeebe.protocol.record.value.UserRecordValue;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import io.camunda.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.util.List;
import java.util.UUID;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

public class AuthorizationRemovePermissionAuthorizationTest {
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

  private static long defaultUserKey = -1L;
  @Rule public final TestWatcher recordingExporterTestWatcher = new RecordingExporterTestWatcher();

  @Test
  public void shouldBeAuthorizedToRemovePermissionsWithDefaultUser() {
    // given
    final var resourceId = "resourceId";
    final var resourceType = AuthorizationResourceType.DEPLOYMENT;
    final var permissionType = PermissionType.DELETE;
    final var user = createUser();
    addPermissionsToUser(user.getUserKey(), resourceType, permissionType, resourceId);

    // when
    ENGINE
        .authorization()
        .permission()
        .withOwnerKey(user.getUserKey())
        .withResourceType(resourceType)
        .withPermission(permissionType, resourceId)
        .remove(DEFAULT_USER.getUsername());

    // when
    assertThat(
            RecordingExporter.authorizationRecords(AuthorizationIntent.PERMISSION_REMOVED)
                .withOwnerKey(user.getUserKey())
                .exists())
        .isTrue();
  }

  @Test
  public void shouldBeAuthorizedToRemovePermissionsWithUser() {
    // given
    final var resourceId = "resourceId";
    final var resourceType = AuthorizationResourceType.AUTHORIZATION;
    final var permissionType = PermissionType.UPDATE;
    final var user = createUser();
    addPermissionsToUser(user.getUserKey(), resourceType, permissionType, resourceId, "*");

    // when
    ENGINE
        .authorization()
        .permission()
        .withOwnerKey(user.getUserKey())
        .withResourceType(resourceType)
        .withPermission(permissionType, resourceId)
        .remove(user.getUsername());

    // then
    assertThat(
            RecordingExporter.authorizationRecords(AuthorizationIntent.PERMISSION_REMOVED)
                .withOwnerKey(user.getUserKey())
                .exists())
        .isTrue();
  }

  @Test
  public void shouldBeUnauthorizedToRemovePermissionsIfNoPermissions() {
    // given
    final var user = createUser();

    // when
    final var rejection =
        ENGINE
            .authorization()
            .permission()
            .withOwnerKey(user.getUserKey())
            .withResourceType(AuthorizationResourceType.DEPLOYMENT)
            .withPermission(PermissionType.DELETE, "*")
            .expectRejection()
            .remove(user.getUsername());

    // then
    Assertions.assertThat(rejection)
        .hasRejectionType(RejectionType.FORBIDDEN)
        .hasRejectionReason(
            "Insufficient permissions to perform operation 'UPDATE' on resource 'AUTHORIZATION'");
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
}
