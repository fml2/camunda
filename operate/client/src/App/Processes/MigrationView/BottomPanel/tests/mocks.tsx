/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {useEffect} from 'react';
import {processInstanceMigrationStore} from 'modules/stores/processInstanceMigration';
import {processXmlStore as processXmlMigrationSourceStore} from 'modules/stores/processXml/processXml.migration.source';
import {processXmlStore as processXmlMigrationTargetStore} from 'modules/stores/processXml/processXml.migration.target';
import {processStatisticsStore} from 'modules/stores/processStatistics/processStatistics.migration.source';
import {open} from 'modules/mocks/diagrams';

const elements = {
  checkPayment: {
    id: 'checkPayment',
    name: 'Check payment',
    type: 'serviceTask',
  },
  ExclusiveGateway: {
    id: 'ExclusiveGateway',
    name: 'Payment OK?',
    type: 'exclusiveGateway',
  },
  requestForPayment: {
    id: 'requestForPayment',
    name: 'Request for payment',
    type: 'serviceTask',
  },
  shippingSubProcess: {
    id: 'shippingSubProcess',
    name: 'Shipping Sub Process',
    type: 'subProcess',
  },
  shipArticles: {
    id: 'shipArticles',
    name: 'Ship Articles',
    type: 'userTask',
  },
  confirmDelivery: {
    id: 'confirmDelivery',
    name: 'Confirm delivery',
    type: 'callActivity',
  },
  MessageInterrupting: {
    id: 'MessageInterrupting',
    name: 'Message interrupting',
    type: 'messageBoundaryEventInterrupting',
  },
  TimerInterrupting: {
    id: 'TimerInterrupting',
    name: 'Timer interrupting',
    type: 'timerBoundaryEventInterrupting',
  },
  MessageNonInterrupting: {
    id: 'MessageNonInterrupting',
    name: 'Message non-interrupting',
    type: 'messageBoundaryEventNonInterrupting',
  },
  TimerNonInterrupting: {
    id: 'TimerNonInterrupting',
    name: 'Timer non-interrupting',
    type: 'timerBoundaryEventNonInterrupting',
  },
  MessageIntermediateCatch: {
    id: 'MessageIntermediateCatch',
    name: 'Message intermediate catch',
    type: 'messageIntermediateCatch',
  },
  TimerIntermediateCatch: {
    id: 'TimerIntermediateCatch',
    name: 'Timer intermediate catch',
    type: 'timerIntermediateCatch',
  },
  TaskX: {
    id: 'TaskX',
    name: 'Task X',
    type: 'serviceTask',
  },
  TaskY: {
    id: 'TaskY',
    name: 'Task Y',
    type: 'serviceTask',
  },
  MessageEventSubProcess: {
    id: 'MessageEventSubProcess',
    name: 'Message event sub process',
    type: 'eventSubProcess',
  },
  TimerEventSubProcess: {
    id: 'TimerEventSubProcess',
    name: 'Timer event sub process',
    type: 'eventSubProcess',
  },
  TimerStartEvent: {
    id: 'TimerStartEvent',
    name: 'Timer start event',
    type: 'startEvent',
  },
  MessageReceiveTask: {
    id: 'MessageReceiveTask',
    name: 'Message receive task',
    type: 'receiveTask',
  },
  BusinessRuleTask: {
    id: 'BusinessRuleTask',
    name: 'Business rule task',
    type: 'businessRuleTask',
  },
  ScriptTask: {
    id: 'ScriptTask',
    name: 'Script task',
    type: 'scriptTask',
  },
  SendTask: {
    id: 'SendTask',
    name: 'Send task',
    type: 'sendTask',
  },
  IntermediateTimerEvent: {
    id: 'IntermediateTimerEvent',
    name: 'IntermediateTimerEvent',
    type: 'timerIntermediateCatch',
  },
  EventBasedGateway: {
    id: 'EventBasedGatewayTask',
    name: 'EventBasedGateway',
    type: 'eventBasedGateway',
  },
  SignalIntermediateCatch: {
    id: 'SignalIntermediateCatch',
    name: 'Signal intermediate catch',
    type: 'signalIntermediateCatch',
  },
  SignalBoundaryEvent: {
    id: 'SignalBoundaryEvent',
    name: 'Signal boundary event',
    type: 'signalBoundaryEvent',
  },
  SignalEventSubProcess: {
    id: 'SignalEventSubProcess',
    name: 'Signal event sub process',
    type: 'signalEventSubProcess',
  },
  SignalStartEvent: {
    id: 'SignalStartEvent',
    name: 'Signal start event',
    type: 'signalStartEvent',
  },
  ErrorEventSubProcess: {
    id: 'ErrorEventSubProcess',
    name: 'Error event sub process',
    type: 'errorEventSubProcess',
  },
  ErrorStartEvent: {
    id: 'ErrorStartEvent',
    name: 'Error start event',
    type: 'ErrorStartEvent',
  },
  MultiInstanceSubProcess: {
    id: 'MultiInstanceSubProcess',
    name: 'Multi instance sub process',
    type: 'MultiInstanceSubProcess',
  },
  MultiInstanceTask: {
    id: 'MultiInstanceTask',
    name: 'Multi instance task',
    type: 'MultiInstanceTask',
  },
};

type Props = {
  children?: React.ReactNode;
};

const Wrapper = ({children}: Props) => {
  processXmlMigrationSourceStore.setProcessXml(open('instanceMigration.bpmn'));
  processInstanceMigrationStore.enable();

  useEffect(() => {
    return () => {
      processInstanceMigrationStore.reset();
      processXmlMigrationSourceStore.reset();
      processXmlMigrationTargetStore.reset();
      processStatisticsStore.reset();
    };
  }, []);

  return (
    <>
      {children}
      <button
        onClick={() => {
          processXmlMigrationTargetStore.fetchProcessXml();
        }}
      >
        Fetch Target Process
      </button>
    </>
  );
};

export {elements, Wrapper};
