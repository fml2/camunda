/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {useEffect, useState} from 'react';

import {
  Button,
  Icon,
  Labeled,
  Input,
  BPMNDiagram,
  Modal,
  VersionPopover,
  TenantPopover,
} from 'components';
import {withErrorHandling} from 'HOC';
import {loadProcessDefinitionXml} from 'services';
import {t} from 'translation';
import {showError} from 'notifications';

import {loadTenants, loadVersions} from './service';

import './DefinitionEditor.scss';

export function DefinitionEditor({
  mightFail,
  collection,
  type,
  definition,
  tenantInfo,
  onChange,
  onRemove,
  onCopy,
}) {
  const [availableVersions, setAvailableVersions] = useState([]);
  const [selectedSpecificVersions, setSelectedSpecificVersions] = useState(
    isSpecificVersion(definition.versions) ? definition.versions : []
  );
  const [availableTenants, setAvailableTenants] = useState(tenantInfo);
  const [xml, setXml] = useState();
  const [loadingXml, setLoadingXml] = useState(true);
  const [displayName, setDisplayName] = useState(definition.displayName);
  const [diagramModalOpen, setDiagramModalOpen] = useState(false);

  const {key, versions, tenantIds} = definition;
  const firstVersion = versions[0];
  const firstTenant = tenantIds[0];

  useEffect(() => {
    mightFail(loadVersions(type, collection, key), setAvailableVersions, showError);
  }, [mightFail, collection, key, type]);

  useEffect(() => {
    setLoadingXml(true);
    mightFail(
      loadProcessDefinitionXml(key, firstVersion, firstTenant),
      (xml) => {
        setXml(xml);
        setLoadingXml(false);
      },
      showError
    );
  }, [mightFail, key, firstVersion, firstTenant]);

  return (
    <div className="DefinitionEditor">
      <div className="definitionLabel">{t('report.definition.' + type)}</div>
      <div className="definitionName">{definition.name}</div>
      <div className="selectionPanel">
        <div className="version entry">
          <Labeled label={t('common.definitionSelection.version.label')} />
          <VersionPopover
            versions={availableVersions}
            selected={definition.versions}
            selectedSpecificVersions={selectedSpecificVersions}
            onChange={(newVersions) => {
              if (isSpecificVersion(newVersions)) {
                setSelectedSpecificVersions(newVersions);
              }

              if (!newVersions.length) {
                setAvailableTenants([]);
                onChange({...definition, versions: newVersions, tenantIds: []});
              } else {
                mightFail(
                  loadTenants(type, [{key: definition.key, version: newVersions}], collection),
                  ([{tenants: newAvailableTenants}]) => {
                    const prevTenants = availableTenants;
                    const deselectedTenants = prevTenants
                      ?.map(({id}) => id)
                      .filter((tenant) => !definition.tenantIds?.includes(tenant));
                    const tenantIds = newAvailableTenants
                      ?.map(({id}) => id)
                      .filter((tenant) => !deselectedTenants?.includes(tenant));

                    setAvailableTenants(newAvailableTenants);
                    onChange({...definition, versions: newVersions, tenantIds});
                  },
                  showError
                );
              }
            }}
          />
        </div>
        {availableTenants?.length > 1 && (
          <div className="tenant entry">
            <Labeled label={t('common.tenant.label')} />
            <TenantPopover
              tenants={availableTenants}
              selected={definition.tenantIds}
              onChange={(newTenants) => {
                onChange({...definition, tenantIds: newTenants});
              }}
            />
          </div>
        )}
        <div className="displayName">
          <Labeled label={t('report.displayName')} />
          <Input
            placeholder={t('report.displayNamePlaceholder')}
            value={displayName}
            onChange={(evt) => setDisplayName(evt.target.value)}
            onBlur={(evt) => {
              // This input field is inside a Popover. When the user click outside of the Popover, we want to close it.
              // Popovers close, when they receive a click event outside of the Popover. However, click events are
              // only triggered after the mouseup, while the blur event fires after mousedown. The onChange handler
              // we call here causes a react state update that rerenders the ReportRenderer. So if a user clicks on
              // the ReportRenderer to close the popover, we get a blur event that causes the original target of the click to
              // disappear because of the rerender, so that the click event is never generated and the Popover stays open.
              //
              // To fix that, we need to identify that the blur event was caused by a click rather than a key event (1) and
              // then delay the execution of the onChange update until after the click has been processed (2). (1) can be
              // handled with the relatedTarget property of the event. For keyboard blur events this is always set to the new
              // element to receive focus. Some mouse events can also set this, if the clicked element is focusable. But as the
              // ReportRenderer does not have any focusable elements, we can use this to identify a click in the ReportRenderer
              // For (2), we register a one time mouseup event listener on the body. As the click handler fires after mouseup,
              // we further delay the execution of onChange using setTimeout to give the Popover time to process the click event.
              if (!evt.relatedTarget) {
                document.body.addEventListener(
                  'mouseup',
                  () => setTimeout(() => onChange({...definition, displayName})),
                  {once: true}
                );
              } else {
                onChange({...definition, displayName});
              }
            }}
          />
        </div>
      </div>
      <div className="diagram">
        <BPMNDiagram xml={xml} disableNavigation loading={loadingXml} />
        <Button small disabled={loadingXml || !xml} onClick={() => setDiagramModalOpen(true)}>
          <Icon type="fullscreen" />
          {t('common.entity.viewModel.model')}
        </Button>
      </div>
      <div className="actionBar">
        <Button small onClick={onCopy}>
          <Icon type="copy-small" />
          {t('common.addACopy')}
        </Button>
        <Button small onClick={onRemove}>
          <Icon type="close-small" />
          {t('common.removeEntity', {entity: t(`common.${type}.label`)})}
        </Button>
      </div>
      <Modal
        className="DefinitionEditor diagramModal"
        open={diagramModalOpen}
        size="max"
        onClose={() => setDiagramModalOpen(false)}
      >
        <Modal.Header>{definition.name}</Modal.Header>
        <Modal.Content>
          <BPMNDiagram xml={xml} />
        </Modal.Content>
        <Modal.Actions>
          <Button main onClick={() => setDiagramModalOpen(false)}>
            {t('common.close')}
          </Button>
        </Modal.Actions>
      </Modal>
    </div>
  );
}

function isSpecificVersion(versions) {
  return versions && versions[0] !== 'latest' && versions[0] !== 'all';
}

export default withErrorHandling(DefinitionEditor);
