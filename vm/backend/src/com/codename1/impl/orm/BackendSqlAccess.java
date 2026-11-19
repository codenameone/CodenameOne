/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.impl.orm;

import com.codename1.backend.Crypto;
import com.codename1.backend.Database;
import com.codename1.backend.DataSource;
import com.codename1.backend.sql.Dialect;
import com.codename1.orm.session.LockMode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Connection borrowing and transaction pinning for a single managed session.
/// Internal connection adapter; not an application API.
/// @hidden
public final class BackendSqlAccess implements SqlAccess, TenantSource {
    private final DataSource pool;
    private final Database supplied;
    private final Dialect dialect;
    private Database transaction;
    /// Whether [#transaction] is a `@Transactional` method's rather than this
    /// session's own. Then the session neither sends BEGIN nor COMMIT: the
    /// method's transaction decides, and a rollback here only marks it.
    private boolean joinedTransaction;

    public BackendSqlAccess(DataSource pool, Database supplied, Dialect dialect) {
        this.pool = pool;
        this.supplied = supplied;
        this.dialect = dialect;
    }

    /// The tenant of the work on this thread; see
    /// [com.codename1.backend.tenancy.TenantContext].
    @Override
    public String currentTenant() {
        return com.codename1.backend.tenancy.TenantContext.get();
    }

    @Override
    public boolean isUnscoped() {
        return com.codename1.backend.tenancy.TenantContext.isUnscoped();
    }

    @Override
    public boolean isNativeSqlAllowed() {
        return com.codename1.backend.tenancy.TenantContext.isNativeSqlAllowed();
    }

    @Override
    public RuntimeException failure(int kind, String message) {
        switch (kind) {
            case MISSING:
                return new com.codename1.backend.tenancy.MissingTenantException(message);
            case NATIVE_SQL:
                return new com.codename1.backend.tenancy.TenantNativeSqlException(message);
            default:
                return new com.codename1.backend.tenancy.TenantMismatchException(message);
        }
    }

    @Override
    public String dialect() {
        return dialect.getName();
    }

    @Override
    public List<Object[]> describe(String table) throws IOException {
        if ("sqlite".equals(dialect())) {
            List<Object[]> definition = query("SELECT sql FROM sqlite_master WHERE type='table' AND name=? COLLATE NOCASE",
                    new Object[]{table}, new int[1]);
            Object createSql = definition.isEmpty() ? null : definition.get(0)[0];
            Map<String, String> collations = new HashMap<String, String>();
            for (String index : SessionImpl.sqliteUniqueIndexes(query("PRAGMA index_list(" + quote(table) + ")",
                    new Object[0], new int[5]))) {
                SessionImpl.sqliteKeyCollations(collations, query("PRAGMA index_xinfo(" + quote(index) + ")",
                        new Object[0], new int[6]));
            }
            return SessionImpl.sqliteColumns(query("PRAGMA table_info(" + quote(table) + ")", new Object[0], new int[6]),
                    createSql instanceof String ? (String) createSql : null, collations);
        }
        if ("sqlserver".equals(dialect())) {
            return query(SQLSERVER_DESCRIBE, new Object[]{quote(table)}, new int[8]);
        }
        if ("oracle".equals(dialect())) {
            // The name as Dialect.quote stores it: upper case.
            String name = quote(table);
            return query(ORACLE_DESCRIBE, new Object[]{name.substring(1, name.length() - 1)}, new int[8]);
        }
        boolean mysql = "mysql".equals(dialect());
        // PostgreSQL: the schema an unqualified name resolves to through the
        // whole search_path, which is the table the ORM's SQL actually reads.
        String schema = mysql ? "DATABASE()"
                : "(SELECT n.nspname FROM pg_catalog.pg_class r INNER JOIN pg_catalog.pg_namespace n ON n.oid=r.relnamespace WHERE r.oid=to_regclass(quote_ident(?)))";
        // A literal NULL default fills nothing. MySQL's DEFAULT_GENERATED is a
        // CURRENT_TIMESTAMP default, not a generated column.
        String filled = mysql
                ? "CASE WHEN c.column_default IS NOT NULL OR c.extra LIKE '%auto_increment%' OR c.extra LIKE '%DEFAULT_GENERATED%' OR c.extra LIKE '%VIRTUAL GENERATED%' OR c.extra LIKE '%STORED GENERATED%' THEN 1 ELSE 0 END"
                : "CASE WHEN c.column_default IS NOT NULL AND upper(c.column_default) <> 'NULL' AND upper(c.column_default) NOT LIKE 'NULL::%' OR c.is_identity='YES' OR c.is_generated='ALWAYS' THEN 1 ELSE 0 END";
        String keyGenerated = mysql ? "CASE WHEN c.extra LIKE '%auto_increment%' THEN 1 ELSE 0 END"
                : "CASE WHEN c.is_identity='YES' OR c.column_default LIKE 'nextval(%' THEN 1 ELSE 0 END";
        // ON UPDATE CURRENT_TIMESTAMP rewrites the column on every update the
        // mapping makes without writing it, so the entity keeps a stale value;
        // it counts as a column the database writes.
        String readOnly = mysql ? "CASE WHEN c.extra LIKE '%VIRTUAL GENERATED%' OR c.extra LIKE '%STORED GENERATED%' OR c.extra LIKE '%on update%' THEN 1 ELSE 0 END"
                : "CASE WHEN c.is_generated='ALWAYS' OR c.is_identity='YES' AND c.identity_generation='ALWAYS' THEN 1 ELSE 0 END";
        // PostgreSQL: a nondeterministic collation can find distinct strings
        // equal, which a text key cannot allow; SessionImpl reads the mark.
        // Read through to_jsonb: PostgreSQL 10 and 11 have no such column (and
        // no such collations), and naming it would fail every describe.
        String collation = mysql ? "c.collation_name"
                : "CASE WHEN EXISTS (SELECT 1 FROM pg_catalog.pg_collation pc INNER JOIN pg_catalog.pg_namespace cn ON cn.oid = pc.collnamespace WHERE pc.collname = c.collation_name AND cn.nspname = c.collation_schema AND to_jsonb(pc) ->> 'collisdeterministic' = 'false') THEN 'nondeterministic ' || c.collation_name ELSE c.collation_name END";
        String statement = "SELECT c.column_name," + (mysql ? "c.column_type" : "c.data_type || COALESCE('(' || c.character_maximum_length || ')', CASE WHEN c.data_type LIKE 'time%' THEN '(' || c.datetime_precision || ')' END, '')") + ",CASE WHEN c.is_nullable = 'NO' THEN 1 ELSE 0 END AS required, " + (mysql ? "CASE WHEN c.column_key = 'PRI' THEN 1 ELSE 0 END" : "CASE WHEN k.column_name IS NULL THEN 0 ELSE 1 END") + " AS primary_key, " + filled + " AS filled, " + collation + ", " + keyGenerated + " AS key_generated, " + readOnly + " AS read_only "
                // The key: MySQL and MariaDB mark it on the column itself. The
                // derived table that finds it elsewhere is filtered to this one
                // table INSIDE, because MariaDB does not push the outer WHERE
                // into a derived information_schema table and read the key
                // usage of every table on the server, per entity (#6240).
                + (mysql ? "FROM information_schema.columns c "
                        : "FROM information_schema.columns c LEFT JOIN (SELECT u.table_schema,u.table_name,u.column_name FROM information_schema.key_column_usage u INNER JOIN information_schema.table_constraints t ON t.constraint_schema=u.constraint_schema AND t.table_name=u.table_name AND t.constraint_name=u.constraint_name WHERE t.constraint_type='PRIMARY KEY' AND u.table_schema=" + schema + " AND u.table_name=?) k "
                        + "ON c.table_schema=k.table_schema AND c.table_name=k.table_name AND c.column_name=k.column_name ")
                + "WHERE c.table_schema=" + schema + " AND c.table_name=?"
                // MySQL compares catalog strings without case, while
                // lower_case_table_names=0 (the Linux default) resolves table
                // names with it: only the table the ORM's SQL reads may match.
                + (mysql ? " AND (@@lower_case_table_names <> 0 OR BINARY c.table_name = BINARY ?)" : "")
                + " ORDER BY c.ordinal_position";
        // Every placeholder is the table name: MySQL's are c.table_name and the
        // BINARY comparison; PostgreSQL's are the schema subquery and the table,
        // once in the derived key table and once in the outer WHERE.
        return query(statement, mysql ? new Object[]{table, table} : new Object[]{table, table, table, table},
                new int[8]);
    }

    /// SQL Server's columns, read from the catalog views rather than
    /// INFORMATION_SCHEMA, which has no identity or computed flag. The type is
    /// spelled the way the other engines' describes spell theirs -- lower case,
    /// the length in characters (sys.columns counts NVARCHAR in bytes) and
    /// `max` for an unbounded one -- so SessionImpl's type families read it
    /// unchanged. An IDENTITY column refuses an explicit value, so it is both
    /// generated and read only, as PostgreSQL's GENERATED ALWAYS is.
    private static final String SQLSERVER_DESCRIBE = "SELECT c.name, CASE"
            + " WHEN t.name IN ('nvarchar','nchar') THEN t.name + '(' + CASE WHEN c.max_length = -1 THEN 'max' ELSE CAST(c.max_length / 2 AS VARCHAR(10)) END + ')'"
            + " WHEN t.name IN ('varchar','char','varbinary','binary') THEN t.name + '(' + CASE WHEN c.max_length = -1 THEN 'max' ELSE CAST(c.max_length AS VARCHAR(10)) END + ')'"
            + " WHEN t.name IN ('decimal','numeric') THEN t.name + '(' + CAST(c.precision AS VARCHAR(10)) + ',' + CAST(c.scale AS VARCHAR(10)) + ')'"
            + " WHEN t.name = 'float' THEN 'float(' + CAST(c.precision AS VARCHAR(10)) + ')'"
            + " WHEN t.name IN ('datetime2','time','datetimeoffset') THEN t.name + '(' + CAST(c.scale AS VARCHAR(10)) + ')'"
            + " ELSE t.name END AS column_type,"
            + " CASE WHEN c.is_nullable = 0 THEN 1 ELSE 0 END AS required,"
            + " CASE WHEN EXISTS (SELECT 1 FROM sys.index_columns ic INNER JOIN sys.indexes i ON i.object_id = ic.object_id AND i.index_id = ic.index_id"
            + " WHERE i.is_primary_key = 1 AND ic.object_id = c.object_id AND ic.column_id = c.column_id) THEN 1 ELSE 0 END AS primary_key,"
            + " CASE WHEN c.default_object_id <> 0 OR c.is_identity = 1 OR c.is_computed = 1 THEN 1 ELSE 0 END AS filled,"
            + " c.collation_name,"
            + " CASE WHEN c.is_identity = 1 THEN 1 ELSE 0 END AS key_generated,"
            + " CASE WHEN c.is_identity = 1 OR c.is_computed = 1 THEN 1 ELSE 0 END AS read_only"
            + " FROM sys.columns c INNER JOIN sys.types t ON t.user_type_id = c.system_type_id"
            + " WHERE c.object_id = OBJECT_ID(?) ORDER BY c.column_id";

    /// Oracle's columns from USER_TAB_COLUMNS, in the schema the session reads.
    /// The type keeps Oracle's spelling (`number(19)`, `varchar2(255 char)`,
    /// `timestamp(3)`), lower case; SessionImpl's families read NUMBER by its
    /// scale. A column with no NOT NULL is reported as not required, and an
    /// IDENTITY column is generated but takes an explicit value (BY DEFAULT),
    /// so it is not read only unless it is GENERATED ALWAYS.
    private static final String ORACLE_DESCRIBE = "SELECT LOWER(c.column_name), CASE"
            + " WHEN c.data_type IN ('VARCHAR2','NVARCHAR2','CHAR','NCHAR') THEN LOWER(c.data_type) || '(' || c.char_length || ' char)'"
            + " WHEN c.data_type = 'NUMBER' AND c.data_precision IS NULL AND c.data_scale = 0 THEN 'number(38)'"
            + " WHEN c.data_type = 'NUMBER' AND c.data_precision IS NULL THEN 'number'"
            + " WHEN c.data_type = 'NUMBER' AND c.data_scale = 0 THEN 'number(' || c.data_precision || ')'"
            + " WHEN c.data_type = 'NUMBER' THEN 'number(' || c.data_precision || ',' || c.data_scale || ')'"
            + " WHEN c.data_type = 'RAW' THEN 'raw(' || c.data_length || ')'"
            + " ELSE LOWER(c.data_type) END,"
            + " CASE WHEN c.nullable = 'N' THEN 1 ELSE 0 END,"
            + " CASE WHEN EXISTS (SELECT 1 FROM user_constraints k INNER JOIN user_cons_columns kc"
            + " ON kc.constraint_name = k.constraint_name AND kc.table_name = k.table_name"
            + " WHERE k.constraint_type = 'P' AND k.table_name = c.table_name AND kc.column_name = c.column_name) THEN 1 ELSE 0 END,"
            + " CASE WHEN c.data_default IS NOT NULL OR c.identity_column = 'YES' OR c.virtual_column = 'YES' THEN 1 ELSE 0 END,"
            + " c.collation,"
            + " CASE WHEN c.identity_column = 'YES' THEN 1 ELSE 0 END,"
            + " CASE WHEN c.virtual_column = 'YES' OR EXISTS (SELECT 1 FROM user_tab_identity_cols i"
            + " WHERE i.table_name = c.table_name AND i.column_name = c.column_name AND i.generation_type = 'ALWAYS') THEN 1 ELSE 0 END"
            + " FROM user_tab_cols c WHERE c.table_name = ? AND c.hidden_column = 'NO' ORDER BY c.column_id";

    @Override
    public String quote(String name) {
        return dialect.quote(name);
    }

    @Override
    public String columnType(int kind) {
        return dialect.columnType(kind);
    }

    @Override
    public String generatedKeyColumn(int kind, String name) {
        return dialect.generatedKeyColumn(kind, name);
    }

    @Override
    public String assignedKeyColumn(int kind) {
        return dialect.assignedKeyColumn(kind);
    }

    @Override
    public String insertDefaults(String table, String keyColumn) {
        return dialect.insertDefaults(table, keyColumn);
    }

    @Override
    public String likeOperator(boolean escaped) {
        return escaped && !"sqlite".equals(dialect()) ? " LIKE ? ESCAPE '!'" : dialect.likeOperator();
    }

    @Override
    public String likePattern(String pattern, String escape) {
        if (escape == null) {
            return dialect.likePattern(pattern);
        }
        String normalized = SqlPatterns.normalize(pattern, escape, "sqlite".equals(dialect()));
        // normalize escapes with '!' only before ! % _; SQL Server also reads
        // '[' as a wildcard, so it gets the same escape.
        return normalized != null && "sqlserver".equals(dialect()) ? normalized.replace("[", "![") : normalized;
    }

    @Override
    public String likeExpression(String expression) {
        return "sqlite".equals(dialect()) ? SqlPatterns.globExpression(expression) : expression;
    }

    @Override
    public String lockClause(LockMode mode) {
        if (mode == LockMode.NONE) {
            return "";
        }
        if ("sqlite".equals(dialect.getName())) {
            throw new UnsupportedOperationException("SQLite does not support pessimistic row locks");
        }
        if ("sqlserver".equals(dialect.getName())) {
            // Table hints, which SessionImpl.locked writes after the table.
            // UPDLOCK is held to the end of the transaction and excludes other
            // writers and lockers; HOLDLOCK keeps a shared lock as long, which
            // is what FOR SHARE means elsewhere.
            return mode == LockMode.PESSIMISTIC_WRITE ? " WITH (UPDLOCK, ROWLOCK)" : " WITH (HOLDLOCK, ROWLOCK)";
        }
        if (mode == LockMode.PESSIMISTIC_WRITE) {
            return " FOR UPDATE";
        }
        return "mysql".equals(dialect.getName()) ? " LOCK IN SHARE MODE" : " FOR SHARE";
    }

    @Override
    public String orderValue(String expression, int kind) {
        return dialect.comparison(expression, kind == Attribute.TEXT);
    }

    @Override
    public String orderBy(String expression, boolean ascending, int kind) {
        return dialect.orderBy(expression, ascending, kind == Attribute.TEXT);
    }

    @Override
    public String limit(int count, int offset) {
        return dialect.limit(count, offset);
    }

    // Connections are compared by IDENTITY throughout: the question is always
    // whether this is the very connection the session pinned or was handed, and
    // two distinct connections are never interchangeable.
    private Database connection() throws IOException {
        Database db = transaction != null ? transaction : supplied != null ? supplied : pool.borrow();
        if (db != transaction && db.isInTransaction() //NOPMD CompareObjectsWithEquals - connection identity
                && !(pool != null && com.codename1.impl.backend.BackendAccess.get().isJoined(pool, db))) {
            if (db != supplied) { //NOPMD CompareObjectsWithEquals - connection identity
                db.close();
                pool.release(db);
            }
            throw new IOException("Database is in another transaction");
        }
        if ("sqlite".equals(dialect()) && !db.isInTransaction()) {
            db.execute("PRAGMA foreign_keys = ON", new Object[0]);
        }
        return db;
    }

    private void release(Database db) {
        if (db != transaction && db != supplied) { //NOPMD CompareObjectsWithEquals - connection identity
            pool.release(db);
        }
    }

    @Override
    public List<Object[]> query(String sql, Object[] params, int[] kinds) throws IOException {
        Database db = connection();
        try {
            com.codename1.backend.sql.QueryRows rows = (com.codename1.backend.sql.QueryRows) db.query(sql, params);
            if (rows.getColumnCount() != kinds.length) {
                throw new IOException("Unexpected SQL projection width");
            }
            List<Object[]> result = new ArrayList<Object[]>(rows.size());
            for (Object item : rows) {
                Map row = (Map) item;
                Object[] values = row.values().toArray();
                if (values.length != kinds.length) {
                    throw new IOException("Unexpected SQL projection width");
                }
                for (int i = 0; i < values.length; i++) {
                    if (kinds[i] == Attribute.REAL && values[i] instanceof Number) {
                        values[i] = Double.valueOf(((Number) values[i]).doubleValue());
                    }
                }
                result.add(values);
            }
            return result;
        } finally {
            release(db);
        }
    }

    @Override
    public int execute(String sql, Object[] params) throws IOException {
        Database db = connection();
        try {
            return db.execute(sql, params);
        } finally {
            release(db);
        }
    }

    @Override
    public long insert(String sql, Object[] params, String keyColumn) throws IOException {
        Database db = connection();
        try {
            return db.insert(sql, params, keyColumn);
        } finally {
            release(db);
        }
    }

    @Override
    public void prepareGenerator(int strategy, String name) throws IOException {
        if (strategy == 1) {
            return;
        }
        if (strategy == 2 && "postgresql".equals(dialect.getName())) {
            execute("CREATE SEQUENCE IF NOT EXISTS " + quote(name) + " START WITH 1", new Object[0]);
            return;
        }
        if ("oracle".equals(dialect.getName())) {
            // 19c has no IF NOT EXISTS: an anonymous block swallows ORA-00955
            // ("name is already used"), which is the race-free form too.
            if (strategy == 2) {
                execute(oracleIgnoringExisting("CREATE SEQUENCE " + quote(name) + " START WITH 1"), new Object[0]);
                return;
            }
            execute(oracleIgnoringExisting("CREATE TABLE cn1_orm_sequences (sequence_name "
                    + dialect.assignedKeyColumn(Dialect.TEXT) + ", next_value "
                    + dialect.columnType(Dialect.BIGINT) + " NOT NULL)"), new Object[0]);
            execute("MERGE INTO cn1_orm_sequences s USING (SELECT ? AS n FROM DUAL) v ON (s.sequence_name = v.n)"
                    + " WHEN NOT MATCHED THEN INSERT (sequence_name,next_value) VALUES (v.n,0)", new Object[]{name});
            return;
        }
        boolean sqlServer = "sqlserver".equals(dialect.getName());
        if (strategy == 2 && sqlServer) {
            execute("IF OBJECT_ID(?, 'SO') IS NULL EXEC('CREATE SEQUENCE ' + ?  + ' AS BIGINT START WITH 1')",
                    new Object[]{quote(name), quote(name)});
            return;
        }
        execute("CREATE TABLE IF NOT EXISTS cn1_orm_sequences (sequence_name " + dialect.assignedKeyColumn(Dialect.TEXT) + ", next_value " + dialect.columnType(Dialect.BIGINT) + " NOT NULL)", new Object[0]);
        if (sqlServer) {
            execute("INSERT INTO cn1_orm_sequences WITH (HOLDLOCK) (sequence_name,next_value) SELECT ?,0 "
                    + "WHERE NOT EXISTS (SELECT 1 FROM cn1_orm_sequences WITH (UPDLOCK, HOLDLOCK) WHERE sequence_name = ?)",
                    new Object[]{name, name});
            return;
        }
        String sql = "INSERT " + ("mysql".equals(dialect.getName()) ? "IGNORE " : "") + "INTO cn1_orm_sequences (sequence_name,next_value) VALUES (?,0)";
        if (!"mysql".equals(dialect.getName())) {
            sql += " ON CONFLICT (sequence_name) DO NOTHING";
        }
        execute(sql, new Object[]{name});
    }

    /// A DDL statement run so that ORA-00955, the object already existing,
    /// is not an error.
    static String oracleIgnoringExisting(String ddl) {
        return "BEGIN EXECUTE IMMEDIATE '" + ddl.replace("'", "''")
                + "'; EXCEPTION WHEN OTHERS THEN IF SQLCODE <> -955 THEN RAISE; END IF; END;";
    }

    @Override
    public Object nextIdentifier(int strategy, String name, int kind) throws IOException {
        if (strategy == 1) {
            byte[] bytes = Crypto.randomBytes(16);
            bytes[6] = (byte) ((bytes[6] & 15) | 64);
            bytes[8] = (byte) ((bytes[8] & 63) | 128);
            String hex = "0123456789abcdef";
            StringBuilder value = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                if (i == 4 || i == 6 || i == 8 || i == 10) {
                    value.append('-');
                }
                value.append(hex.charAt((bytes[i] >>> 4) & 15)).append(hex.charAt(bytes[i] & 15));
            }
            return value.toString();
        }
        if (strategy == 2 && "postgresql".equals(dialect.getName())) {
            return query("SELECT nextval(CAST(? AS regclass))", new Object[]{quote(name)}, new int[]{Dialect.BIGINT}).get(0)[0];
        }
        if (strategy == 2 && "sqlserver".equals(dialect.getName())) {
            return query("SELECT NEXT VALUE FOR " + quote(name), new Object[0], new int[]{Dialect.BIGINT}).get(0)[0];
        }
        if (strategy == 2 && "oracle".equals(dialect.getName())) {
            return query("SELECT " + quote(name) + ".NEXTVAL FROM DUAL", new Object[0], new int[]{Dialect.BIGINT}).get(0)[0];
        }
        long max = kind == Dialect.INTEGER ? Integer.MAX_VALUE : Long.MAX_VALUE;
        if (execute("UPDATE cn1_orm_sequences SET next_value = next_value + 1 WHERE sequence_name = ? AND next_value < ?", new Object[]{name, Long.valueOf(max)}) != 1) {
            throw new IOException("Identifier generator is missing or exhausted: " + name);
        }
        return query("SELECT next_value FROM cn1_orm_sequences WHERE sequence_name = ?", new Object[]{name}, new int[]{Dialect.BIGINT}).get(0)[0];
    }

    @Override
    public void begin() throws IOException {
        if (transaction != null) {
            throw new IOException("Transaction already active");
        }
        if (supplied == null && pool != null) {
            // Inside a @Transactional method: this session becomes part of that
            // transaction instead of opening one the connection would refuse.
            Database joined = com.codename1.impl.backend.BackendAccess.get().joined(pool);
            if (joined != null) {
                transaction = joined;
                joinedTransaction = true;
                return;
            }
        }
        Database db = supplied != null ? supplied : pool.borrow();
        try {
            if ("sqlite".equals(dialect())) {
                db.execute("PRAGMA foreign_keys = ON", new Object[0]);
            }
            db.beginExclusiveTransaction();
            transaction = db;
        } finally {
            release(db);
        }
    }

    @Override
    public boolean isTransactionActive() {
        return transaction != null && transaction.isInTransaction();
    }

    @Override
    public void commit() throws IOException {
        if (transaction == null) {
            throw new IOException("No transaction");
        }
        if (joinedTransaction) {
            unpin();
            return;
        }
        transaction.commitTransaction();
        unpin();
    }

    @Override
    public void rollback() throws IOException {
        if (transaction == null) {
            throw new IOException("No transaction");
        }
        if (joinedTransaction) {
            com.codename1.impl.backend.BackendAccess.get().markRollbackOnly(pool);
            unpin();
            return;
        }
        transaction.rollbackTransaction();
        unpin();
    }

    private void unpin() {
        Database db = transaction;
        transaction = null;
        joinedTransaction = false;
        release(db);
    }

    @Override
    public void close() throws IOException {
        if (transaction != null && joinedTransaction) {
            // Closed without committing: its changes were never flushed, and the
            // method's transaction cannot commit as though they had been.
            com.codename1.impl.backend.BackendAccess.get().markRollbackOnly(pool);
            unpin();
            return;
        }
        if (transaction != null) {
            boolean rolledBack = false;
            try {
                transaction.rollbackTransaction();
                rolledBack = true;
            } finally {
                // A failed rollback may leave both a transaction and its thread
                // reservation active. Close it before the pool can reuse it.
                try {
                    if (!rolledBack) {
                        transaction.close();
                    }
                } finally {
                    unpin();
                }
            }
        }
    }
}
