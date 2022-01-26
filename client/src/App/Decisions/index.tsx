/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {FiltersPanel} from './FiltersPanel';
import {DecisionTablePanel} from './DecisionTablePanel';
import {InstancesListPanel} from './InstancesListPanel';
import {Container, RightContainer} from './styled';

const Decisions: React.FC = () => {
  return (
    <Container>
      <FiltersPanel />
      <RightContainer>
        <DecisionTablePanel />
        <InstancesListPanel />
      </RightContainer>
    </Container>
  );
};

export {Decisions};
