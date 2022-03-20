/*
 * This file is generated by jOOQ.
 */
package stroom.proxy.repo.db.jooq;


import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;

import stroom.proxy.repo.db.jooq.tables.Aggregate;
import stroom.proxy.repo.db.jooq.tables.ForwardAggregate;
import stroom.proxy.repo.db.jooq.tables.ForwardSource;
import stroom.proxy.repo.db.jooq.tables.ForwardUrl;
import stroom.proxy.repo.db.jooq.tables.Source;
import stroom.proxy.repo.db.jooq.tables.SourceEntry;
import stroom.proxy.repo.db.jooq.tables.SourceItem;
import stroom.proxy.repo.db.jooq.tables.records.AggregateRecord;
import stroom.proxy.repo.db.jooq.tables.records.ForwardAggregateRecord;
import stroom.proxy.repo.db.jooq.tables.records.ForwardSourceRecord;
import stroom.proxy.repo.db.jooq.tables.records.ForwardUrlRecord;
import stroom.proxy.repo.db.jooq.tables.records.SourceEntryRecord;
import stroom.proxy.repo.db.jooq.tables.records.SourceItemRecord;
import stroom.proxy.repo.db.jooq.tables.records.SourceRecord;


/**
 * A class modelling foreign key relationships and constraints of tables in the
 * default schema.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<AggregateRecord> AGGREGATE__ = Internal.createUniqueKey(Aggregate.AGGREGATE, DSL.name(""), new TableField[] { Aggregate.AGGREGATE.ID }, true);
    public static final UniqueKey<ForwardAggregateRecord> FORWARD_AGGREGATE__ = Internal.createUniqueKey(ForwardAggregate.FORWARD_AGGREGATE, DSL.name(""), new TableField[] { ForwardAggregate.FORWARD_AGGREGATE.ID }, true);
    public static final UniqueKey<ForwardSourceRecord> FORWARD_SOURCE__ = Internal.createUniqueKey(ForwardSource.FORWARD_SOURCE, DSL.name(""), new TableField[] { ForwardSource.FORWARD_SOURCE.ID }, true);
    public static final UniqueKey<ForwardUrlRecord> FORWARD_URL__ = Internal.createUniqueKey(ForwardUrl.FORWARD_URL, DSL.name(""), new TableField[] { ForwardUrl.FORWARD_URL.ID, ForwardUrl.FORWARD_URL.URL }, true);
    public static final UniqueKey<SourceRecord> SOURCE__ = Internal.createUniqueKey(Source.SOURCE, DSL.name(""), new TableField[] { Source.SOURCE.ID }, true);
    public static final UniqueKey<SourceEntryRecord> SOURCE_ENTRY__ = Internal.createUniqueKey(SourceEntry.SOURCE_ENTRY, DSL.name(""), new TableField[] { SourceEntry.SOURCE_ENTRY.ID }, true);
    public static final UniqueKey<SourceItemRecord> SOURCE_ITEM__ = Internal.createUniqueKey(SourceItem.SOURCE_ITEM, DSL.name(""), new TableField[] { SourceItem.SOURCE_ITEM.ID, SourceItem.SOURCE_ITEM.NAME, SourceItem.SOURCE_ITEM.SOURCE_ID }, true);
}
