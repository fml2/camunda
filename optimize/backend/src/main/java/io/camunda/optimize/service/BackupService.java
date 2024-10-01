/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service;

import static io.camunda.optimize.dto.optimize.rest.SnapshotState.FAILED;
import static io.camunda.optimize.dto.optimize.rest.SnapshotState.INCOMPATIBLE;
import static io.camunda.optimize.dto.optimize.rest.SnapshotState.IN_PROGRESS;
import static io.camunda.optimize.dto.optimize.rest.SnapshotState.PARTIAL;
import static io.camunda.optimize.dto.optimize.rest.SnapshotState.SUCCESS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

import co.elastic.clients.elasticsearch._types.ShardFailure;
import co.elastic.clients.elasticsearch.snapshot.SnapshotInfo;
import io.camunda.optimize.dto.optimize.BackupState;
import io.camunda.optimize.dto.optimize.rest.BackupInfoDto;
import io.camunda.optimize.dto.optimize.rest.SnapshotInfoDto;
import io.camunda.optimize.dto.optimize.rest.SnapshotState;
import io.camunda.optimize.service.db.reader.BackupReader;
import io.camunda.optimize.service.db.writer.BackupWriter;
import io.camunda.optimize.service.exceptions.OptimizeConfigurationException;
import io.camunda.optimize.service.util.configuration.ConfigurationService;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class BackupService {

  private static final int EXPECTED_NUMBER_OF_SNAPSHOTS_PER_BACKUP = 2;
  private static final Logger log = org.slf4j.LoggerFactory.getLogger(BackupService.class);

  private final BackupReader backupReader;
  private final BackupWriter backupWriter;
  private final ConfigurationService configurationService;

  public BackupService(
      BackupReader backupReader,
      BackupWriter backupWriter,
      ConfigurationService configurationService) {
    this.backupReader = backupReader;
    this.backupWriter = backupWriter;
    this.configurationService = configurationService;
  }

  public synchronized void triggerBackup(final Long backupId) {
    validateRepositoryExists();
    backupReader.validateNoDuplicateBackupId(backupId);

    log.info("Triggering backup with ID {}", backupId);
    backupWriter.triggerSnapshotCreation(backupId);
  }

  public List<BackupInfoDto> getAllBackupInfo() {
    validateRepositoryExists();
    return backupReader.getAllOptimizeSnapshotsByBackupId().entrySet().stream()
        .map(
            entry ->
                getSingleBackupInfo(
                    entry.getKey(),
                    entry.getValue().stream()
                        .collect(groupingBy(s -> SnapshotState.valueOf(s.state())))))
        .toList();
  }

  public BackupInfoDto getSingleBackupInfo(final Long backupId) {
    validateRepositoryExists();
    return getSingleBackupInfo(
        backupId,
        backupReader.getOptimizeSnapshotsForBackupId(backupId).stream()
            .collect(groupingBy(s -> SnapshotState.valueOf(s.state()))));
  }

  private BackupInfoDto getSingleBackupInfo(
      final Long backupId, final Map<SnapshotState, List<SnapshotInfo>> snapshotInfosPerState) {
    if (snapshotInfosPerState.isEmpty()) {
      final String reason =
          String.format("No Optimize backup with ID [%d] could be found.", backupId);
      log.error(reason);
      throw new NotFoundException(reason);
    }
    return getBackupInfoDto(backupId, snapshotInfosPerState);
  }

  private BackupInfoDto getBackupInfoDto(
      final Long backupId, final Map<SnapshotState, List<SnapshotInfo>> snapshotInfosPerState) {
    final BackupState backupState = determineBackupState(snapshotInfosPerState);
    String failureReason = null;
    if (BackupState.FAILED == backupState) {
      failureReason =
          String.format(
              "The following snapshots failed: [%s]",
              snapshotInfosPerState.getOrDefault(FAILED, Collections.emptyList()).stream()
                  .map(snapshotInfo -> snapshotInfo.snapshot())
                  .collect(joining(", ")));
    }
    return new BackupInfoDto(
        backupId,
        failureReason,
        backupState,
        snapshotInfosPerState.values().stream()
            .flatMap(List::stream)
            .map(
                snapshotInfo ->
                    new SnapshotInfoDto(
                        snapshotInfo.snapshot(),
                        SnapshotState.valueOf(snapshotInfo.state()),
                        OffsetDateTime.ofInstant(
                            Instant.ofEpochMilli(snapshotInfo.startTime().toEpochMilli()),
                            ZoneId.systemDefault()),
                        snapshotInfo.shards().failures().stream()
                            .map(ShardFailure::toString)
                            .toList()))
            .toList());
  }

  public void deleteBackup(final Long backupId) {
    validateRepositoryExists();
    backupWriter.deleteOptimizeSnapshots(backupId);
  }

  private BackupState determineBackupState(
      final Map<SnapshotState, List<SnapshotInfo>> snapshotInfosPerState) {
    if (snapshotInfosPerState.getOrDefault(SUCCESS, Collections.emptyList()).size()
        == EXPECTED_NUMBER_OF_SNAPSHOTS_PER_BACKUP) {
      return BackupState.COMPLETED;
    } else if (snapshotInfosPerState.get(FAILED) != null
        || snapshotInfosPerState.get(PARTIAL) != null) {
      return BackupState.FAILED;
    } else if (snapshotInfosPerState.get(INCOMPATIBLE) != null) {
      return BackupState.INCOMPATIBLE;
    } else if (snapshotInfosPerState.get(IN_PROGRESS) != null) {
      return BackupState.IN_PROGRESS;
    } else if (snapshotInfosPerState.getOrDefault(SUCCESS, Collections.emptyList()).size()
        < EXPECTED_NUMBER_OF_SNAPSHOTS_PER_BACKUP) {
      return BackupState.INCOMPLETE;
    } else {
      // this can for example occur if users create additional manual snapshots matching our naming
      // scheme
      return BackupState.FAILED;
    }
  }

  // todo extract this validation to the readers to avoid using there configuration or extract it
  // from db configuration by OPT-7245
  private void validateRepositoryExists() {
    if (StringUtils.isEmpty(
        configurationService.getElasticSearchConfiguration().getSnapshotRepositoryName())) {
      final String reason =
          "Cannot execute backup request because no Elasticsearch snapshot repository name found in Optimize configuration.";
      log.error(reason);
      throw new OptimizeConfigurationException(reason);
    } else {
      backupReader.validateRepositoryExistsOrFail();
    }
  }
}
