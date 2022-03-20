/*
 * This file is generated by jOOQ.
 */
package stroom.activity.impl.db.jooq.tables;


import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row8;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import stroom.activity.impl.db.jooq.Keys;
import stroom.activity.impl.db.jooq.Stroom;
import stroom.activity.impl.db.jooq.tables.records.ActivityRecord;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Activity extends TableImpl<ActivityRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>stroom.activity</code>
     */
    public static final Activity ACTIVITY = new Activity();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ActivityRecord> getRecordType() {
        return ActivityRecord.class;
    }

    /**
     * The column <code>stroom.activity.id</code>.
     */
    public final TableField<ActivityRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.activity.version</code>.
     */
    public final TableField<ActivityRecord, Integer> VERSION = createField(DSL.name("version"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>stroom.activity.create_time_ms</code>.
     */
    public final TableField<ActivityRecord, Long> CREATE_TIME_MS = createField(DSL.name("create_time_ms"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>stroom.activity.create_user</code>.
     */
    public final TableField<ActivityRecord, String> CREATE_USER = createField(DSL.name("create_user"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.activity.update_time_ms</code>.
     */
    public final TableField<ActivityRecord, Long> UPDATE_TIME_MS = createField(DSL.name("update_time_ms"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>stroom.activity.update_user</code>.
     */
    public final TableField<ActivityRecord, String> UPDATE_USER = createField(DSL.name("update_user"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.activity.user_id</code>.
     */
    public final TableField<ActivityRecord, String> USER_ID = createField(DSL.name("user_id"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.activity.json</code>.
     */
    public final TableField<ActivityRecord, String> JSON = createField(DSL.name("json"), SQLDataType.CLOB, this, "");

    private Activity(Name alias, Table<ActivityRecord> aliased) {
        this(alias, aliased, null);
    }

    private Activity(Name alias, Table<ActivityRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>stroom.activity</code> table reference
     */
    public Activity(String alias) {
        this(DSL.name(alias), ACTIVITY);
    }

    /**
     * Create an aliased <code>stroom.activity</code> table reference
     */
    public Activity(Name alias) {
        this(alias, ACTIVITY);
    }

    /**
     * Create a <code>stroom.activity</code> table reference
     */
    public Activity() {
        this(DSL.name("activity"), null);
    }

    public <O extends Record> Activity(Table<O> child, ForeignKey<O, ActivityRecord> key) {
        super(child, key, ACTIVITY);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Stroom.STROOM;
    }

    @Override
    public Identity<ActivityRecord, Integer> getIdentity() {
        return (Identity<ActivityRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<ActivityRecord> getPrimaryKey() {
        return Keys.KEY_ACTIVITY_PRIMARY;
    }

    @Override
    public TableField<ActivityRecord, Integer> getRecordVersion() {
        return VERSION;
    }

    @Override
    public Activity as(String alias) {
        return new Activity(DSL.name(alias), this);
    }

    @Override
    public Activity as(Name alias) {
        return new Activity(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Activity rename(String name) {
        return new Activity(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Activity rename(Name name) {
        return new Activity(name, null);
    }

    // -------------------------------------------------------------------------
    // Row8 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row8<Integer, Integer, Long, String, Long, String, String, String> fieldsRow() {
        return (Row8) super.fieldsRow();
    }
}
