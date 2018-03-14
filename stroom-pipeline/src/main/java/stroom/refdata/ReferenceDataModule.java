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

package stroom.refdata;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import stroom.entity.DocumentPermissionCache;
import stroom.feed.FeedService;
import stroom.io.StreamCloser;
import stroom.pipeline.LocationFactoryProxy;
import stroom.pipeline.PipelineService;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.factory.PipelineDataCache;
import stroom.pipeline.factory.PipelineFactory;
import stroom.pipeline.state.FeedHolder;
import stroom.pipeline.state.PipelineHolder;
import stroom.pipeline.state.StreamHolder;
import stroom.security.SecurityContext;
import stroom.streamstore.StreamStore;
import stroom.task.TaskHandler;
import stroom.task.TaskManager;
import stroom.util.cache.CacheManager;
import stroom.util.spring.StroomScope;
import stroom.task.TaskContext;

import javax.inject.Named;

public class ReferenceDataModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ReferenceDataLoader.class).to(ReferenceDataLoaderImpl.class);
        bind(ContextDataLoader.class).to(ContextDataLoaderImpl.class);

        final Multibinder<TaskHandler> taskHandlerBinder = Multibinder.newSetBinder(binder(), TaskHandler.class);
        taskHandlerBinder.addBinding().to(stroom.refdata.ContextDataLoadTaskHandler.class);
        taskHandlerBinder.addBinding().to(stroom.refdata.ReferenceDataLoadTaskHandler.class);
    }
}