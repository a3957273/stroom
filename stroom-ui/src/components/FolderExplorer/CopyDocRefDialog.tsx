/*
 * Copyright 2018 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import * as React from "react";

import { compose, withHandlers } from "recompose";
import { connect } from "react-redux";
import { Formik, Field, FieldProps } from "formik";

import { GlobalStoreState } from "../../startup/reducers";
import IconHeader from "../IconHeader";
import { findItem } from "../../lib/treeUtils";
import {
  actionCreators,
  defaultStatePerId,
  StoreStatePerId as CopyStoreState
} from "./redux/copyDocRefReducer";
import { copyDocuments } from "./explorerClient";
import withDocumentTree from "./withDocumentTree";
import DialogActionButtons from "./DialogActionButtons";
import ThemedModal from "../ThemedModal";
import AppSearchBar from "../AppSearchBar";
import PermissionInheritancePicker from "../PermissionInheritancePicker";
import {
  DocRefWithLineage,
  DocRefType,
  PermissionInheritance
} from "../../types";

const { completeDocRefCopy } = actionCreators;

const LISTING_ID = "copy-item-listing";

export interface Props {
  listingId: string;
}

interface ConnectState extends CopyStoreState {}

interface ConnectDispatch {
  completeDocRefCopy: typeof completeDocRefCopy;
  copyDocuments: typeof copyDocuments;
}

interface WithHandlers {
  onCancel: React.MouseEventHandler;
}

export interface EnhancedProps
  extends Props,
    ConnectState,
    ConnectDispatch,
    WithHandlers {}

const enhance = compose<EnhancedProps, Props>(
  withDocumentTree,
  connect<ConnectState, ConnectDispatch, Props, GlobalStoreState>(
    ({ folderExplorer: { documentTree, copyDocRef } }, { listingId }) => {
      const thisState: CopyStoreState =
        copyDocRef[listingId] || defaultStatePerId;

      const initialDestination:
        | DocRefWithLineage
        | undefined = thisState.destinationUuid
        ? findItem(documentTree, thisState.destinationUuid)
        : undefined;

      return {
        ...thisState,
        initialValues: {
          destination: initialDestination && initialDestination.node
        }
      };
    },
    { completeDocRefCopy, copyDocuments }
  ),
  withHandlers<Props & ConnectState & ConnectDispatch, WithHandlers>({
    onCancel: ({ completeDocRefCopy, listingId }) => () =>
      completeDocRefCopy(listingId)
  })
);

interface FormValues {
  destination?: DocRefType;
  permissionInheritance: PermissionInheritance;
}

let CopyDocRefDialog = ({
  isCopying,
  onCancel,
  copyDocuments,
  uuids
}: EnhancedProps) => (
  <Formik<FormValues>
    initialValues={{
      destination: undefined,
      permissionInheritance: PermissionInheritance.NONE
    }}
    onSubmit={values =>
      copyDocuments(
        uuids,
        values.destination!.uuid,
        values.permissionInheritance
      )
    }
  >
    {({ setFieldValue, submitForm }: Formik) => (
      <ThemedModal
        isOpen={isCopying}
        header={
          <IconHeader
            icon="copy"
            text="Select a Destination Folder for the Copy"
          />
        }
        content={
          <form>
            <div>
              <label>Destination</label>
              <Field name="destination">
                {({ field: { value } }: FieldProps) => (
                  <AppSearchBar
                    pickerId={LISTING_ID}
                    onChange={d => setFieldValue("destination", d)}
                    value={value}
                    typeFilters={[]}
                  />
                )}
              </Field>
            </div>
            <div>
              <label>Permission Inheritance</label>
              <Field name="permissionInheritance">
                {({ field: { value } }: FieldProps) => (
                  <PermissionInheritancePicker
                    onChange={d => setFieldValue("permissionInheritance", d)}
                    value={value}
                  />
                )}
              </Field>
            </div>
          </form>
        }
        actions={
          <DialogActionButtons onCancel={onCancel} onConfirm={submitForm} />
        }
      />
    )}
  </Formik>
);

export default enhance(CopyDocRefDialog);
