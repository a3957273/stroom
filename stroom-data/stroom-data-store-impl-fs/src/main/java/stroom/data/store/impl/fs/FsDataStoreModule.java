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

package stroom.data.store.impl.fs;

import stroom.data.store.api.Store;
import stroom.data.store.impl.DataStoreMaintenanceService;
import stroom.data.store.impl.fs.api.FsVolumeResource;
import stroom.job.api.RunnableWrapper;
import stroom.job.api.ScheduledJobsBinder;
import stroom.meta.api.AttributeMapFactory;
import stroom.util.guice.GuiceUtil;
import stroom.util.guice.RestResourcesBinder;
import stroom.util.guice.ServletBinder;
import stroom.util.shared.Clearable;

import com.google.inject.AbstractModule;

import javax.inject.Inject;

import static stroom.job.api.Schedule.ScheduleType.CRON;

public class FsDataStoreModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DataStoreMaintenanceService.class).to(FsDataStoreMaintenanceService.class);
        bind(Store.class).to(FsStore.class);
        bind(AttributeMapFactory.class).to(FsStore.class);

        GuiceUtil.buildMultiBinder(binder(), Clearable.class).addBinding(FsVolumeService.class);

        RestResourcesBinder.create(binder())
                .bind(FsVolumeResource.class);

        ServletBinder.create(binder())
                .bind(EchoServlet.class);

        ScheduledJobsBinder.create(binder())
                .bindJobTo(FileSystemClean.class, builder -> builder
                        .withName("File System Clean")
                        .withDescription("Job to process a volume deleting files that are no " +
                                "longer indexed (maybe the retention period has past or they have been deleted)")
                        .withSchedule(CRON, "0 0 *")
                        .withAdvancedState(false))
                .bindJobTo(MetaDelete.class, builder -> builder
                        .withName("Meta Delete")
                        .withDescription("Physically delete streams that have been logically deleted " +
                                "based on age of delete (stroom.data.store.deletePurgeAge)")
                        .withSchedule(CRON, "0 0 *"));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    private static class FileSystemClean extends RunnableWrapper {
        @Inject
        FileSystemClean(final FsCleanExecutor fileSystemCleanExecutor) {
            super(fileSystemCleanExecutor::clean);
        }
    }

    private static class MetaDelete extends RunnableWrapper {
        @Inject
        MetaDelete(final PhysicalDeleteExecutor physicalDeleteExecutor) {
            super(physicalDeleteExecutor::exec);
        }
    }
}