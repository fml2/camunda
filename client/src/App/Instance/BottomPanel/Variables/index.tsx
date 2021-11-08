/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import React, {useRef, useState} from 'react';

import {currentInstanceStore} from 'modules/stores/currentInstance';
import {variablesStore} from 'modules/stores/variables';
import {flowNodeMetaDataStore} from 'modules/stores/flowNodeMetaData';

import * as Styled from './styled';
import {observer} from 'mobx-react';
import {SpinnerSkeleton} from 'modules/components/SpinnerSkeleton';
import {VariableBackdrop} from './VariableBackdrop';
import {Skeleton} from './Skeleton';
import {Table, TH, TR} from './VariablesTable';
import {flowNodeSelectionStore} from 'modules/stores/flowNodeSelection';
// import {ExistingVariable} from './ExistingVariable';
import {NewVariableForm} from './NewVariableForm';
import {PendingVariable} from './PendingVariable';
import {useInstancePageParams} from '../../useInstancePageParams';
import {MAX_VARIABLES_STORED} from 'modules/constants/variables';
import {InfiniteScroller} from 'modules/components/InfiniteScroller';
// import {useNotifications} from 'modules/notifications';
import {Restricted} from 'modules/components/Restricted';

const Variables: React.FC = observer(() => {
  const {
    state: {items, pendingItem, loadingItemId, status},
    displayStatus,
    // scopeId,
  } = variablesStore;

  const scrollableContentRef = useRef<HTMLDivElement>(null);
  const variablesContentRef = useRef<HTMLDivElement>(null);
  const variableRowRef = useRef<HTMLTableRowElement>(null);
  const {processInstanceId} = useInstancePageParams();
  // const notifications = useNotifications();
  const [isAddMode, setIsAddMode] = useState(false);

  // const isTextareaOutOfBounds = (
  //   itemRef: React.RefObject<HTMLTableDataCellElement>
  // ) => {
  //   const inputTD = itemRef.current;

  //   const theadHeight = 45;

  //   if (inputTD && variablesContentRef?.current) {
  //     const container = variablesContentRef.current.children[0];
  //     // distance from top edge of container to bottom edge of td
  //     const tdPosition =
  //       inputTD.offsetTop -
  //       theadHeight -
  //       container.scrollTop +
  //       inputTD.offsetHeight;

  //     return tdPosition > container.clientHeight;
  //   }
  // };

  // const scrollToItem = (itemRef: React.RefObject<HTMLTableDataCellElement>) => {
  //   if (isTextareaOutOfBounds(itemRef)) {
  //     itemRef.current?.scrollIntoView();
  //   }
  // };

  const isVariableHeaderVisible =
    isAddMode || variablesStore.displayStatus === 'variables';

  return (
    <>
      <Styled.VariablesContent ref={variablesContentRef}>
        {displayStatus === 'spinner' && (
          <Styled.EmptyPanel
            data-testid="variables-spinner"
            type="skeleton"
            Skeleton={SpinnerSkeleton}
          />
        )}

        {!isAddMode && displayStatus === 'skeleton' && (
          <Skeleton type="skeleton" rowHeight={32} />
        )}
        {!isAddMode && displayStatus === 'no-variables' && (
          <Skeleton type="info" label="The Flow Node has no Variables" />
        )}
        {(isAddMode || displayStatus === 'variables') && (
          <>
            <Styled.Header>Variables</Styled.Header>

            <Styled.TableScroll ref={scrollableContentRef}>
              <Table data-testid="variables-list">
                <Styled.THead
                  isVariableHeaderVisible={isVariableHeaderVisible}
                  scrollBarWidth={
                    (scrollableContentRef?.current?.offsetWidth ?? 0) -
                    (scrollableContentRef?.current?.scrollWidth ?? 0)
                  }
                >
                  {isVariableHeaderVisible && (
                    <TR>
                      <TH>Name</TH>
                      <TH>Value</TH>
                      <TH />
                    </TR>
                  )}
                </Styled.THead>
                <InfiniteScroller
                  onVerticalScrollStartReach={async (scrollDown) => {
                    if (
                      variablesStore.shouldFetchPreviousVariables() === false
                    ) {
                      return;
                    }
                    await variablesStore.fetchPreviousVariables(
                      processInstanceId
                    );

                    if (
                      variablesStore.state.items.length ===
                        MAX_VARIABLES_STORED &&
                      variablesStore.state.latestFetch.itemsCount !== 0
                    ) {
                      scrollDown(
                        variablesStore.state.latestFetch.itemsCount *
                          (variableRowRef.current?.offsetHeight ?? 0)
                      );
                    }
                  }}
                  onVerticalScrollEndReach={() => {
                    if (variablesStore.shouldFetchNextVariables() === false) {
                      return;
                    }
                    variablesStore.fetchNextVariables(processInstanceId);
                  }}
                  scrollableContainerRef={scrollableContentRef}
                >
                  <tbody>
                    {items.map(
                      ({
                        name: variableName,
                        value: variableValue,
                        hasActiveOperation,
                        isPreview,
                        id,
                      }) => (
                        <TR
                          ref={variableRowRef}
                          key={variableName}
                          data-testid={variableName}
                          hasActiveOperation={hasActiveOperation}
                        >
                          <>
                            <Styled.TD>
                              <Styled.VariableName title={variableName}>
                                {variableName}
                              </Styled.VariableName>
                            </Styled.TD>

                            <Styled.DisplayTextTD>
                              <Styled.DisplayText
                                hasBackdrop={loadingItemId === id}
                              >
                                {loadingItemId === id && <VariableBackdrop />}
                                {variableValue}
                              </Styled.DisplayText>
                            </Styled.DisplayTextTD>
                            <Styled.EditButtonsTD></Styled.EditButtonsTD>
                          </>
                        </TR>
                      )
                    )}
                  </tbody>
                </InfiniteScroller>
              </Table>
            </Styled.TableScroll>
          </>
        )}
        <Restricted scopes={['write']}>
          <Styled.Footer
            scrollBarWidth={
              (scrollableContentRef?.current?.offsetWidth ?? 0) -
              (scrollableContentRef?.current?.scrollWidth ?? 0)
            }
          >
            {currentInstanceStore.isRunning && (
              <>
                {pendingItem !== null && <PendingVariable />}
                {isAddMode && pendingItem === null && (
                  <NewVariableForm
                    onExit={() => {
                      setIsAddMode(false);
                    }}
                  />
                )}
              </>
            )}

            {!isAddMode && pendingItem === null && (
              <Styled.Button
                type="button"
                title="Add variable"
                size="small"
                onClick={() => {
                  setIsAddMode(true);
                }}
                disabled={
                  status === 'first-fetch' ||
                  isAddMode ||
                  (flowNodeSelectionStore.isRootNodeSelected
                    ? !currentInstanceStore.isRunning
                    : !flowNodeMetaDataStore.isSelectedInstanceRunning) ||
                  loadingItemId !== null
                }
              >
                <Styled.Plus /> Add Variable
              </Styled.Button>
            )}
          </Styled.Footer>
        </Restricted>
      </Styled.VariablesContent>
    </>
  );
});

export default Variables;
