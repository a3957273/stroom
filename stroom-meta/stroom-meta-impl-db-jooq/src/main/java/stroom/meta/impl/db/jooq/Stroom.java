/*
 * This file is generated by jOOQ.
 */
package stroom.meta.impl.db.jooq;


import java.util.Arrays;
import java.util.List;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import stroom.meta.impl.db.jooq.tables.Meta;
import stroom.meta.impl.db.jooq.tables.MetaFeed;
import stroom.meta.impl.db.jooq.tables.MetaKey;
import stroom.meta.impl.db.jooq.tables.MetaProcessor;
import stroom.meta.impl.db.jooq.tables.MetaRetentionTracker;
import stroom.meta.impl.db.jooq.tables.MetaType;
import stroom.meta.impl.db.jooq.tables.MetaVal;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Stroom extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom</code>
     */
    public static final Stroom STROOM = new Stroom();

    /**
     * The table <code>stroom.meta</code>.
     */
    public final Meta META = Meta.META;

    /**
     * The table <code>stroom.meta_feed</code>.
     */
    public final MetaFeed META_FEED = MetaFeed.META_FEED;

    /**
     * The table <code>stroom.meta_key</code>.
     */
    public final MetaKey META_KEY = MetaKey.META_KEY;

    /**
     * The table <code>stroom.meta_processor</code>.
     */
    public final MetaProcessor META_PROCESSOR = MetaProcessor.META_PROCESSOR;

    /**
     * The table <code>stroom.meta_retention_tracker</code>.
     */
    public final MetaRetentionTracker META_RETENTION_TRACKER = MetaRetentionTracker.META_RETENTION_TRACKER;

    /**
     * The table <code>stroom.meta_type</code>.
     */
    public final MetaType META_TYPE = MetaType.META_TYPE;

    /**
     * The table <code>stroom.meta_val</code>.
     */
    public final MetaVal META_VAL = MetaVal.META_VAL;

    /**
     * No further instances allowed
     */
    private Stroom() {
        super("stroom", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            Meta.META,
            MetaFeed.META_FEED,
            MetaKey.META_KEY,
            MetaProcessor.META_PROCESSOR,
            MetaRetentionTracker.META_RETENTION_TRACKER,
            MetaType.META_TYPE,
            MetaVal.META_VAL
        );
    }
}
