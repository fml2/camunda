/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.operate.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;

import org.camunda.operate.es.reader.IncidentReader;
import org.camunda.operate.es.reader.ListViewReader;
import org.camunda.operate.rest.dto.listview.ListViewQueryDto;
import org.camunda.operate.rest.dto.listview.ListViewRequestDto;
import org.camunda.operate.rest.dto.listview.ListViewResponseDto;
import org.camunda.operate.rest.dto.listview.ListViewWorkflowInstanceDto;
import org.camunda.operate.util.CollectionUtil;
import org.camunda.operate.util.OperateZeebeIntegrationTest;
import org.camunda.operate.zeebe.operation.CancelWorkflowInstanceHandler;
import org.camunda.operate.zeebe.operation.ResolveIncidentHandler;
import org.camunda.operate.zeebe.operation.UpdateVariableHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.FieldSetter;
import org.springframework.beans.factory.annotation.Autowired;

public class ErrorMessagesIT extends OperateZeebeIntegrationTest{

  @Autowired
  private CancelWorkflowInstanceHandler cancelWorkflowInstanceHandler;

  @Autowired
  private ResolveIncidentHandler updateRetriesHandler;

  @Autowired
  private UpdateVariableHandler updateVariableHandler;
  
  @Autowired
  private IncidentReader incidentReader;
  
  @Autowired
  private ListViewReader listViewReader;

  @Autowired
  private OperateTester operateTester;
  
  @Before
  public void before() {
    super.before();
    injectZeebeClientIntoOperationHandler();
    operateTester.setZeebeClient(getClient()).setMockMvcTestRule(mockMvcTestRule);
  }

  private void injectZeebeClientIntoOperationHandler() {
    try {
      FieldSetter.setField(cancelWorkflowInstanceHandler, CancelWorkflowInstanceHandler.class.getDeclaredField("zeebeClient"), zeebeClient);
      FieldSetter.setField(updateRetriesHandler, ResolveIncidentHandler.class.getDeclaredField("zeebeClient"), zeebeClient);
      FieldSetter.setField(updateVariableHandler, UpdateVariableHandler.class.getDeclaredField("zeebeClient"), zeebeClient);
    } catch (NoSuchFieldException e) {
      fail("Failed to inject ZeebeClient into some of the beans");
    }
  }

  // OPE-453
  @Test
  public void testErrorMessageIsTrimmedBeforeSave() throws Exception {
    // Given
    String errorMessageWithWhitespaces ="   Error message with white spaces   ";
    String errorMessageWithoutWhiteSpaces = "Error message with white spaces";
  
    // when
    Long workflowInstanceKey = setupIncidentWith(errorMessageWithWhitespaces);
    operateTester.updateVariableOperation("a", "b").waitUntil().operationIsCompleted();
    
    // then
    assertThat(incidentReader.getAllIncidentsByWorkflowInstanceKey(workflowInstanceKey).get(0).getErrorMessage()).isEqualTo(errorMessageWithoutWhiteSpaces);
    ListViewResponseDto response = listViewReader.queryWorkflowInstances(new ListViewRequestDto(), 0, 5);
    ListViewWorkflowInstanceDto workflowInstances  = response.getWorkflowInstances().get(0);
    assertThat(workflowInstances).isNotNull();
    assertThat(workflowInstances.getOperations().get(0).getErrorMessage()).doesNotStartWith(" ").doesNotEndWith(" ");
  }
  
  // OPE-619
  @Test
  public void testFilterErrorMessagesBySubstringAndIgnoreCase() throws Exception {
    // Given
    String errorMessageToFind =        "   Find me by query only a substring  ";
    String anotherErrorMessageToFind = "   Unexpected error while executing query 'all_users'";
  
    // when
    String workflowInstanceKey = setupIncidentWith(errorMessageToFind).toString();
    String anotherWorkflowInstanceKey = setupIncidentWith(anotherErrorMessageToFind).toString();
    
    // then ensure that ...
    
    // 1. case should not find any results
    assertSearchResults(searchForErrorMessages("no"), 0);  
    // 2. case should find only one (first) result 
    assertSearchResults(searchForErrorMessages("only"), 1, workflowInstanceKey);
    assertSearchResults(searchForErrorMessages("by query only a"), 1, workflowInstanceKey);
    // 3. case should find two one results , because 'query' is in both error messages 
    assertSearchResults(searchForErrorMessages("query"), 2, workflowInstanceKey, anotherWorkflowInstanceKey);
    // 4. case (ignore lower/upper case) should find one result because 'Find' is in only one errorMessage 
    assertSearchResults(searchForErrorMessages("find"), 1, workflowInstanceKey);
    assertSearchResults(searchForErrorMessages("Find"), 1, workflowInstanceKey);
  }
  
  protected void assertSearchResults(ListViewResponseDto results,int count,String ...workflowInstanceKeys) {
    assertThat(results.getTotalCount()).isEqualTo(count);
    results.getWorkflowInstances().stream().allMatch( 
       workflowInstance -> Arrays.asList(workflowInstanceKeys).contains(workflowInstance.getId())
    );    
  }

  protected ListViewResponseDto searchForErrorMessages(String ... errorMessages) {
    ListViewRequestDto queriesRequest = new ListViewRequestDto();
    queriesRequest.setQueries(CollectionUtil.map(Arrays.asList(errorMessages),errorMessage -> {
      ListViewQueryDto query = ListViewQueryDto.createAll();
      query.setErrorMessage(errorMessage);
      return query;
    }));
    return listViewReader.queryWorkflowInstances(queriesRequest, 0, 10);
  }
 
  protected Long setupIncidentWith(String errorMessage) {
    return operateTester
        .deployWorkflow("demoProcess_v_1.bpmn").waitUntil().workflowIsDeployed()
        .startWorkflowInstance("demoProcess","{\"a\": \"b\"}").failTask("taskA", errorMessage)
        .waitUntil().workflowInstanceIsFinished()
        .getWorkflowInstanceKey(); 
  }
  
}
