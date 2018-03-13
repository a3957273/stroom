/*
 * Copyright 2017 Crown Copyright
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
 *
 */

package stroom.pipeline;

import stroom.entity.MockDocumentEntityService;
import stroom.explorer.ExplorerActionHandler;
import stroom.explorer.shared.DocumentType;
import stroom.importexport.ImportExportActionHandler;
import stroom.importexport.ImportExportHelper;
import stroom.pipeline.shared.FindXSLTCriteria;
import stroom.pipeline.shared.XSLT;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * <p>
 * Very simple mock that keeps everything in memory.
 * </p>
 * <p>
 * <p>
 * You can call clear at any point to clear everything down.
 * </p>
 */
@Singleton
public class MockXSLTService extends MockDocumentEntityService<XSLT, FindXSLTCriteria> implements XSLTService, ExplorerActionHandler, ImportExportActionHandler {
    @Inject
    public MockXSLTService(final ImportExportHelper importExportHelper) {
        super(importExportHelper);
    }

    @Override
    public Class<XSLT> getEntityClass() {
        return XSLT.class;
    }

    @Override
    public DocumentType getDocumentType() {
        return new DocumentType(5, XSLT.ENTITY_TYPE, XSLT.ENTITY_TYPE);
    }
}
