/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.reader;

import io.camunda.optimize.dto.optimize.query.event.process.EventProcessDefinitionDto;
import io.camunda.optimize.service.db.repository.EventRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class EventProcessDefinitionReader {

  private static final Logger log =
      org.slf4j.LoggerFactory.getLogger(EventProcessDefinitionReader.class);
  private final EventRepository eventRepository;

  public EventProcessDefinitionReader(final EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  public Optional<EventProcessDefinitionDto> getEventProcessDefinitionByKeyOmitXml(
      final String eventProcessDefinitionKey) {
    log.debug("Fetching event-based process definition with key [{}].", eventProcessDefinitionKey);
    return eventRepository.getEventProcessDefinitionByKeyOmitXml(eventProcessDefinitionKey);
  }

  public List<EventProcessDefinitionDto> getAllEventProcessDefinitionsOmitXml() {
    log.debug("Fetching all available event-based processes definitions.");
    return eventRepository.getAllEventProcessDefinitionsOmitXml();
  }
}
