/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {computed, makeObservable, override} from 'mobx';
import {ProcessXmlBase} from './processXml.base';
import {isMigratableFlowNode} from './utils/isMigratableFlowNode';

class ProcessesXml extends ProcessXmlBase {
  constructor() {
    super();

    makeObservable(this, {
      selectableFlowNodes: override,
      isTargetSelected: computed,
    });
  }

  get selectableFlowNodes() {
    return super.selectableFlowNodes
      .filter(isMigratableFlowNode)
      .map((flowNode) => {
        return {...flowNode, name: flowNode.name ?? flowNode.id};
      });
  }

  get isTargetSelected() {
    return this.state.xml !== null;
  }
}

const processXmlStore = new ProcessesXml();

export {processXmlStore};
