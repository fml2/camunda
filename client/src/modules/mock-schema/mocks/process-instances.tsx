/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {ProcessInstance} from 'modules/types';

const processInstances: ProcessInstance[] = [
  {
    id: '111111111111111111111111',
    process: {
      id: '1',
      name: 'Process 1 name',
      bpmnProcessId: 'Process_1 id',
      version: 1,
      startEventFormId: null,
      sortValues: ['1'],
    },
    creationDate: '2021-01-01T00:00:00.000+00:00',
    isFirst: true,
    state: 'completed',
    sortValues: ['1', '2'],
  },
  {
    id: '111111111111111111111112',
    process: {
      id: '1',
      name: null,
      bpmnProcessId: 'Process_1 id',
      version: 1,
      startEventFormId: null,
      sortValues: ['1'],
    },
    creationDate: '2021-01-01T00:00:00.000+00:00',
    isFirst: false,
    state: 'active',
    sortValues: ['1', '2'],
  },
  {
    id: '111111111111111111111113',
    process: {
      id: '1',
      name: 'Process 1 name',
      bpmnProcessId: 'Process_1 id',
      version: 1,
      startEventFormId: null,
      sortValues: ['1'],
    },
    creationDate: '2021-01-01T00:00:00.000+00:00',
    isFirst: false,
    state: 'incident',
    sortValues: ['1', '2'],
  },
  {
    id: '111111111111111111111114',
    process: {
      id: '1',
      name: null,
      bpmnProcessId: 'Process_1 id',
      version: 1,
      startEventFormId: null,
      sortValues: ['1'],
    },
    creationDate: '2021-01-01T00:00:00.000+00:00',
    isFirst: false,
    state: 'terminated',
    sortValues: ['1', '2'],
  },
];

export {processInstances};
