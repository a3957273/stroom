package stroom.data.meta.impl.db;

import stroom.lifecycle.api.AbstractLifecycleModule;
import stroom.lifecycle.api.RunnableWrapper;

import javax.inject.Inject;

public class DataMetaDbLifecycleModule extends AbstractLifecycleModule {
    @Override
    protected void configure() {
        super.configure();
        bindShutdown().to(MetaValueServiceFlush.class);
    }

    private static class MetaValueServiceFlush extends RunnableWrapper {
        @Inject
        MetaValueServiceFlush(final MetaValueServiceImpl metaValueService) {
            super(metaValueService::flush);
        }
    }
}