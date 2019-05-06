/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import fetch from 'node-fetch';

import config from './config';
import license from './license';

export default async function setup() {
  await ensureLicense();

  const sessionToken = await getSession();

  await cleanEntities('report', sessionToken);
  await cleanEntities('dashboard', sessionToken);
  await cleanEntities('collection', sessionToken);
  await cleanEntities('alert', sessionToken);
}

async function ensureLicense() {
  const resp = await fetch(config.endpoint + '/api/license/validate');
  if (!resp.ok) {
    await fetch(config.endpoint + '/api/license/validate-and-store', {
      method: 'POST',
      body: license
    });
  }
}

async function getSession() {
  const resp = await fetch(config.endpoint + '/api/authentication', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(config.agentUser)
  });
  return await resp.text();
}

async function cleanEntities(type, sessionToken) {
  const headers = {Cookie: `X-Optimize-Authorization="Bearer ${sessionToken}"`};

  const response = await fetch(`${config.endpoint}/api/${type}`, {headers});
  const entities = await response.json();

  for (let i = 0; i < entities.length; i++) {
    await fetch(`${config.endpoint}/api/${type}/${entities[i].id}?force=true`, {
      method: 'DELETE',
      headers
    });
  }
}
