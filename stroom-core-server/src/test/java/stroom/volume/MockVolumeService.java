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

package stroom.volume;

import stroom.entity.MockEntityService;
import stroom.node.VolumeService;
import stroom.node.shared.FindVolumeCriteria;
import stroom.node.shared.Node;
import stroom.node.shared.Volume;

import javax.inject.Singleton;
import java.util.Set;

@Singleton
public class MockVolumeService extends MockEntityService<Volume, FindVolumeCriteria> implements VolumeService {
    @Override
    public Set<Volume> getStreamVolumeSet(final Node node) {
        return null;
    }

    @Override
    public Set<Volume> getIndexVolumeSet(final Node node, final Set<Volume> allowedVolumes) {
        return null;
    }

    @Override
    public void flush() {
    }

    @Override
    public void clear() {
    }

    @Override
    public Class<Volume> getEntityClass() {
        return Volume.class;
    }
}