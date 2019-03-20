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

package stroom.data.store.impl.mock;

import com.google.inject.AbstractModule;
import stroom.data.store.api.Store;
import stroom.data.store.impl.SteamStoreStreamCloserImpl;
import stroom.util.guice.GuiceUtil;
import stroom.util.io.StreamCloser;
import stroom.util.shared.Clearable;

public class MockStreamStoreModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Store.class).to(MockStore.class);
        bind(StreamCloser.class).to(SteamStoreStreamCloserImpl.class);

        GuiceUtil.buildMultiBinder(binder(), Clearable.class).addBinding(MockStore.class);
    }
}