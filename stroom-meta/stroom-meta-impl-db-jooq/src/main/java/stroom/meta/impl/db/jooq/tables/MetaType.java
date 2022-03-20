/*
 * This file is generated by jOOQ.
 */
package stroom.meta.impl.db.jooq.tables;


import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row2;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import stroom.meta.impl.db.jooq.Keys;
import stroom.meta.impl.db.jooq.Stroom;
import stroom.meta.impl.db.jooq.tables.records.MetaTypeRecord;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MetaType extends TableImpl<MetaTypeRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom.meta_type</code>
     */
    public static final MetaType META_TYPE = new MetaType();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MetaTypeRecord> getRecordType() {
        return MetaTypeRecord.class;
    }

    /**
     * The column <code>stroom.meta_type.id</code>.
     */
    public final TableField<MetaTypeRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.meta_type.name</code>.
     */
    public final TableField<MetaTypeRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    private MetaType(Name alias, Table<MetaTypeRecord> aliased) {
        this(alias, aliased, null);
    }

    private MetaType(Name alias, Table<MetaTypeRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>stroom.meta_type</code> table reference
     */
    public MetaType(String alias) {
        this(DSL.name(alias), META_TYPE);
    }

    /**
     * Create an aliased <code>stroom.meta_type</code> table reference
     */
    public MetaType(Name alias) {
        this(alias, META_TYPE);
    }

    /**
     * Create a <code>stroom.meta_type</code> table reference
     */
    public MetaType() {
        this(DSL.name("meta_type"), null);
    }

    public <O extends Record> MetaType(Table<O> child, ForeignKey<O, MetaTypeRecord> key) {
        super(child, key, META_TYPE);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Stroom.STROOM;
    }

    @Override
    public Identity<MetaTypeRecord, Integer> getIdentity() {
        return (Identity<MetaTypeRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<MetaTypeRecord> getPrimaryKey() {
        return Keys.KEY_META_TYPE_PRIMARY;
    }

    @Override
    public List<UniqueKey<MetaTypeRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.KEY_META_TYPE_NAME);
    }

    @Override
    public MetaType as(String alias) {
        return new MetaType(DSL.name(alias), this);
    }

    @Override
    public MetaType as(Name alias) {
        return new MetaType(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public MetaType rename(String name) {
        return new MetaType(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public MetaType rename(Name name) {
        return new MetaType(name, null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}
