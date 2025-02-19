/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.atomix.raft.metrics;

import io.micrometer.common.docs.KeyName;

@SuppressWarnings("NullableProblems")
public enum RaftKeyNames implements KeyName {
  /** partitionGroupName */
  PARTITION_GROUP {
    @Override
    public String asString() {
      return "partitionGroupName";
    }
  },
  /** follower */
  FOLLOWER {
    @Override
    public String asString() {
      return "follower";
    }
  },
  /** member of the cluster the message is sent */
  TO {
    @Override
    public String asString() {
      return "to";
    }
  },
  /** type of the message received */
  TYPE {
    @Override
    public String asString() {
      return "type";
    }
  }
}
