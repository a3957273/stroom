/*
 * This file is generated by jOOQ.
 */
package stroom.meta.impl.db.jooq.tables.records;


import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;

import stroom.meta.impl.db.jooq.tables.MetaFeed;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetaFeedRecord extends UpdatableRecordImpl<MetaFeedRecord> implements Record2<Integer, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>stroom.meta_feed.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>stroom.meta_feed.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>stroom.meta_feed.name</code>.
     */
    public void setName(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>stroom.meta_feed.name</code>.
     */
    public String getName() {
        return (String) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<Integer, String> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return MetaFeed.META_FEED.ID;
    }

    @Override
    public Field<String> field2() {
        return MetaFeed.META_FEED.NAME;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getName();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getName();
    }

    @Override
    public MetaFeedRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public MetaFeedRecord value2(String value) {
        setName(value);
        return this;
    }

    @Override
    public MetaFeedRecord values(Integer value1, String value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached MetaFeedRecord
     */
    public MetaFeedRecord() {
        super(MetaFeed.META_FEED);
    }

    /**
     * Create a detached, initialised MetaFeedRecord
     */
    public MetaFeedRecord(Integer id, String name) {
        super(MetaFeed.META_FEED);

        setId(id);
        setName(name);
    }
}
