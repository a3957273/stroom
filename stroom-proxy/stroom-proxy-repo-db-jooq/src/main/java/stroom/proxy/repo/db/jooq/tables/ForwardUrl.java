/*
 * This file is generated by jOOQ.
 */
package stroom.proxy.repo.db.jooq.tables;


import org.jooq.Field;
import org.jooq.ForeignKey;
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

import stroom.proxy.repo.db.jooq.DefaultSchema;
import stroom.proxy.repo.db.jooq.Keys;
import stroom.proxy.repo.db.jooq.tables.records.ForwardUrlRecord;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ForwardUrl extends TableImpl<ForwardUrlRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>forward_url</code>
     */
    public static final ForwardUrl FORWARD_URL = new ForwardUrl();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ForwardUrlRecord> getRecordType() {
        return ForwardUrlRecord.class;
    }

    /**
     * The column <code>forward_url.id</code>.
     */
    public final TableField<ForwardUrlRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>forward_url.url</code>.
     */
    public final TableField<ForwardUrlRecord, String> URL = createField(DSL.name("url"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    private ForwardUrl(Name alias, Table<ForwardUrlRecord> aliased) {
        this(alias, aliased, null);
    }

    private ForwardUrl(Name alias, Table<ForwardUrlRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>forward_url</code> table reference
     */
    public ForwardUrl(String alias) {
        this(DSL.name(alias), FORWARD_URL);
    }

    /**
     * Create an aliased <code>forward_url</code> table reference
     */
    public ForwardUrl(Name alias) {
        this(alias, FORWARD_URL);
    }

    /**
     * Create a <code>forward_url</code> table reference
     */
    public ForwardUrl() {
        this(DSL.name("forward_url"), null);
    }

    public <O extends Record> ForwardUrl(Table<O> child, ForeignKey<O, ForwardUrlRecord> key) {
        super(child, key, FORWARD_URL);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : DefaultSchema.DEFAULT_SCHEMA;
    }

    @Override
    public UniqueKey<ForwardUrlRecord> getPrimaryKey() {
        return Keys.FORWARD_URL__;
    }

    @Override
    public ForwardUrl as(String alias) {
        return new ForwardUrl(DSL.name(alias), this);
    }

    @Override
    public ForwardUrl as(Name alias) {
        return new ForwardUrl(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public ForwardUrl rename(String name) {
        return new ForwardUrl(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ForwardUrl rename(Name name) {
        return new ForwardUrl(name, null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}
