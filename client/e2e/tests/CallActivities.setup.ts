/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {deploy, createSingleInstance} from '../setup-utils';

export async function setup() {
  await deploy(['callActivityProcess.bpmn', 'calledProcess.bpmn']);

  return {
    callActivityProcessInstance: await createSingleInstance(
      'CallActivityProcess',
      1
    ),
  };
}
