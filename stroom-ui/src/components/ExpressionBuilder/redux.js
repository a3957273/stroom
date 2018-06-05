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
import { createAction, handleActions } from 'redux-actions';

import {
  moveItemInTree,
  assignRandomUuids,
  updateItemInTree,
  addItemToTree,
  deleteItemFromTree
} from 'lib/treeUtils';

import { docRefPicked } from 'components/DocExplorer';

// Expression Editors
const expressionEditorCreated = createAction('EXPRESSION_EDITOR_CREATED', expressionId => ({
  expressionId,
}));
const expressionEditorDestroyed = createAction('EXPRESSION_EDITOR_DESTROYED', expressionId => ({
  expressionId,
}));
const expressionSetEditable = createAction(
  'EXPRESSION_SET_EDITABLE_BY_USER',
  (expressionId, isEditableUserSet) => ({ expressionId, isEditableUserSet }),
);
const requestExpressionItemDelete = createAction(
  'REQUEST_EXPRESSION_ITEM_DELETE',
  (expressionId, itemUuid) => ({ expressionId, itemUuid }),
);
const cancelExpressionItemDelete = createAction('CANCEL_EXPRESSION_ITEM_DELETE', expressionId => ({
  expressionId,
}));

// Expressions
const expressionChanged = createAction('EXPRESSION_CHANGED', (expressionId, expression) => ({
  expressionId,
  expression,
}));
const expressionTermAdded = createAction('EXPRESSION_TERM_ADDED', (expressionId, operatorId) => ({
  expressionId,
  operatorId,
}));
const expressionOperatorAdded = createAction(
  'EXPRESSION_OPERATOR_ADDED',
  (expressionId, operatorId) => ({ expressionId, operatorId }),
);
const expressionItemUpdated = createAction(
  'EXPRESSION_ITEM_UPDATED',
  (expressionId, itemId, updates) => ({ expressionId, itemId, updates }),
);
const confirmExpressionItemDeleted = createAction(
  'EXPRESSION_ITEM_DELETED',
  (expressionId, itemId) => ({ expressionId, itemId }),
);
const expressionItemMoved = createAction(
  'EXPRESSION_ITEM_MOVED',
  (expressionId, itemToMove, destination) => ({ expressionId, itemToMove, destination }),
);

// expressions, keyed on ID, there may be several expressions on a page
const defaultExpressionState = {};

const NEW_TERM = {
  type: 'term',
  enabled: true,
};

const NEW_OPERATOR = {
  type: 'operator',
  op: 'AND',
  enabled: true,
  children: [],
};

/**
 * These constants and functions are used to generate Doc Ref pickerId values
 * that can carry the Dictionary choices for expression terms.
 * The Expression ID and the Term UUID must be carried inside the picker ID.
 */
const EXPRESSION_PREFIX = 'EXP';
const PICKER_DELIM = '_';

/**
 * Given the value of a docRef pickerId. Attempts to parse out the expression ID and term UUID.
 * Looks for the format composed in the join function below.
 * @param {string} value May or may not be a value generated by the joinDictionaryTermId function below
 * @return {object}
 *  {
 *      isExpressionBased : {boolean} to indicate that the input value is a dictionary term value
 *      expressionId : the parsed expression ID. Will be populated if isExpressionBased === true
 *      termUuid : the parsed term UUID. Will be populated if isExpressionBased === true
 *  }
 */
const splitDictionaryTermId = (value) => {
  const p = value.split(PICKER_DELIM);
  let isExpressionBased = false;
  let expressionId = null;
  let termUuid = null;
  if (p.length === 3 && p[0] === EXPRESSION_PREFIX) {
    isExpressionBased = true;
    expressionId = p[1];
    termUuid = p[2];
  }
  return {
    isExpressionBased,
    expressionId,
    termUuid,
  };
};

/**
 * Create the combined picker ID with the appropriate PREFIX and DELIMTER
 * that allow for unambiguous detection in the splitter function.
 *
 * @param {string} expressionId The ID of the expression to which this picker belong
 * @param {string} termUuid The UUID of the term that the picker is being used to pick a value for.
 */
const joinDictionaryTermId = (expressionId, termUuid) =>
  EXPRESSION_PREFIX + PICKER_DELIM + expressionId + PICKER_DELIM + termUuid;

const expressionReducer = handleActions(
  {
    // Expression Changed
    [expressionChanged]: (state, action) => ({
      ...state,
      [action.payload.expressionId]: assignRandomUuids(action.payload.expression),
    }),

    // Expression Term Added
    [expressionTermAdded]: (state, action) => ({
      ...state,
      [action.payload.expressionId]: addItemToTree(
        state[action.payload.expressionId],
        action.payload.operatorId,
        NEW_TERM,
      ),
    }),

    // Expression Operator Added
    [expressionOperatorAdded]: (state, action) => ({
      ...state,
      [action.payload.expressionId]: addItemToTree(
        state[action.payload.expressionId],
        action.payload.operatorId,
        NEW_OPERATOR,
      ),
    }),

    // Expression Term Updated
    [expressionItemUpdated]: (state, action) => ({
      ...state,
      [action.payload.expressionId]: updateItemInTree(
        state[action.payload.expressionId],
        action.payload.itemId,
        action.payload.updates,
      ),
    }),

    // Expression Item Deleted
    [confirmExpressionItemDeleted]: (state, action) => ({
      ...state,
      [action.payload.expressionId]: deleteItemFromTree(
        state[action.payload.expressionId],
        action.payload.itemId,
      ),
    }),

    // Expression Item Moved
    [expressionItemMoved]: (state, action) => ({
      ...state,
      [action.payload.expressionId]: moveItemInTree(
        state[action.payload.expressionId],
        action.payload.itemToMove,
        action.payload.destination,
      ),
    }),

    // Doc Ref Picked
    [docRefPicked]: (state, action) => {
      const { isExpressionBased, expressionId, termUuid } = splitDictionaryTermId(action.payload.pickerId);

      if (isExpressionBased) {
        return {
          ...state,
          [expressionId]: updateItemInTree(state[expressionId], termUuid, {
            value: undefined,
            dictionary: action.payload.docRef,
          }),
        };
      }
      return state;
    },
  },
  defaultExpressionState,
);

//
const defaultEditorsState = {};
const defaultEditorState = {
  isEditableUserSet: false,
  pendingDeletionUuid: undefined,
};

const expressionEditorReducer = handleActions(
  {
    [expressionEditorCreated]: (state, action) => ({
      [action.payload.expressionId]: {
        ...defaultEditorState,
        ...state[action.payload.expressionId],
      },
    }),

    [expressionEditorDestroyed]: (state, action) => ({
      [action.payload.expressionId]: undefined,
    }),

    [expressionSetEditable]: (state, action) => ({
      [action.payload.expressionId]: {
        ...state[action.payload.expressionId],
        isEditableUserSet: action.payload.isEditableUserSet,
      },
    }),

    [requestExpressionItemDelete]: (state, action) => ({
      [action.payload.expressionId]: {
        ...state[action.payload.expressionId],
        pendingDeletionUuid: action.payload.itemUuid,
      },
    }),

    [confirmExpressionItemDeleted]: (state, action) => ({
      [action.payload.expressionId]: {
        ...state[action.payload.expressionId],
        pendingDeletionUuid: undefined,
      },
    }),

    [cancelExpressionItemDelete]: (state, action) => ({
      [action.payload.expressionId]: {
        ...state[action.payload.expressionId],
        pendingDeletionUuid: undefined,
      },
    }),
  },
  defaultEditorsState,
);

export {
  expressionEditorCreated,
  expressionEditorDestroyed,
  expressionSetEditable,
  requestExpressionItemDelete,
  cancelExpressionItemDelete,
  expressionChanged,
  expressionTermAdded,
  expressionOperatorAdded,
  expressionItemUpdated,
  confirmExpressionItemDeleted,
  expressionItemMoved,
  expressionReducer,
  expressionEditorReducer,
  joinDictionaryTermId,
};
