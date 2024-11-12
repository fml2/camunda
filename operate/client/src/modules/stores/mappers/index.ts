/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {SequenceFlowsDto} from 'modules/api/processInstances/sequenceFlows';

const getProcessedSequenceFlows = (sequenceFlows: SequenceFlowsDto) => {
  return sequenceFlows
    .map((sequenceFlow) => sequenceFlow.activityId)
    .filter((value, index, self) => self.indexOf(value) === index);
};

export {getProcessedSequenceFlows};
