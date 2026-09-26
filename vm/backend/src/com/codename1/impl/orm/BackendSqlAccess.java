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

import com.codename1.backend.Database;
import com.codename1.backend.DataSource;
import com.codename1.backend.sql.Dialect;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Connection borrowing and transaction pinning for a single managed session. */
/// Internal connection adapter; not an application API.
/// @hidden
public final class BackendSqlAccess implements SqlAccess {
    private final DataSource pool;
    private final Database supplied;
    private final Dialect dialect;
    private Database transaction;
    /**
     * Whether {@link #transaction} is a {@code @Transactional} method's rather
     * than this session's own. Then the session neither sends BEGIN nor COMMIT:
     * the method's transaction decides, and a rollback here only marks it.
     */
    private boolean joinedTransaction;
    public BackendSqlAccess(DataSource pool,Database supplied,Dialect dialect) {
        this.pool=pool; this.supplied=supplied; this.dialect=dialect;
    }
    public String dialect() { return dialect.getName(); }
    public List<Object[]> describe(String table) throws IOException {
        if("sqlite".equals(dialect())) {
            List<Object[]> raw=query("PRAGMA table_info("+quote(table)+")",new Object[0],new int[6]);
            List<Object[]> result=new ArrayList<Object[]>();
            for(Object[] row:raw) result.add(new Object[]{row[1],row[2],row[3],row[5]});
            return result;
        }
        String schema="mysql".equals(dialect())?"DATABASE()":"current_schema()";
        String statement="SELECT c.column_name,c.data_type,CASE WHEN c.is_nullable = 'NO' THEN 1 ELSE 0 END AS required, CASE WHEN k.column_name IS NULL THEN 0 ELSE 1 END AS primary_key "
            +"FROM information_schema.columns c LEFT JOIN (SELECT u.table_schema,u.table_name,u.column_name FROM information_schema.key_column_usage u INNER JOIN information_schema.table_constraints t ON t.constraint_schema=u.constraint_schema AND t.table_name=u.table_name AND t.constraint_name=u.constraint_name WHERE t.constraint_type='PRIMARY KEY') k "
            +"ON c.table_schema=k.table_schema AND c.table_name=k.table_name AND c.column_name=k.column_name WHERE c.table_schema="+schema+" AND c.table_name=? ORDER BY c.ordinal_position";
        return query(statement,new Object[]{table},new int[4]);
    }
    public String quote(String name) { return dialect.quote(name); }
    public String columnType(int kind) { return dialect.columnType(kind); }
    public String generatedKeyColumn(int kind,String name) { return dialect.generatedKeyColumn(kind,name); }
    public String assignedKeyColumn(int kind) { return dialect.assignedKeyColumn(kind); }
    public String insertDefaults(String table) { return dialect.insertDefaults(table); }
    public String likeOperator(boolean escaped) {
        return escaped && !"sqlite".equals(dialect()) ? " LIKE ? ESCAPE '!'" : dialect.likeOperator();
    }
    public String likePattern(String pattern,String escape) {
        return escape==null ? dialect.likePattern(pattern) : SqlPatterns.normalize(pattern,escape,"sqlite".equals(dialect()));
    }
    public String likeExpression(String expression) {
        return "sqlite".equals(dialect()) ? SqlPatterns.globExpression(expression) : expression;
    }
    public String lockClause(com.codename1.orm.session.LockMode mode) {
        if(mode==com.codename1.orm.session.LockMode.NONE) return "";
        if("sqlite".equals(dialect.getName())) throw new UnsupportedOperationException("SQLite does not support pessimistic row locks");
        if(mode==com.codename1.orm.session.LockMode.PESSIMISTIC_WRITE) return " FOR UPDATE";
        return "mysql".equals(dialect.getName())?" LOCK IN SHARE MODE":" FOR SHARE";
    }
    public String orderValue(String expression,int kind) {
        return dialect.comparison(expression,kind==Attribute.TEXT);
    }
    public String orderBy(String expression,boolean ascending,int kind) {
        return dialect.orderBy(expression,ascending,kind==Attribute.TEXT);
    }
    public String limit(int count,int offset) { return dialect.limit(count,offset); }
    private Database connection() throws IOException {
        Database db=transaction!=null?transaction:supplied!=null?supplied:pool.borrow();
        if(db!=transaction && db.isInTransaction()
                && !(pool!=null && com.codename1.backend.Transactions.isJoined(pool,db))) {
            if(db!=supplied) { db.close();pool.release(db); }
            throw new IOException("Database is in another transaction");
        }
        if("sqlite".equals(dialect()) && !db.isInTransaction()) db.execute("PRAGMA foreign_keys = ON",new Object[0]);
        return db;
    }
    private void release(Database db) { if(db!=transaction && db!=supplied) pool.release(db); }
    public List<Object[]> query(String sql,Object[] params,int[] kinds) throws IOException {
        Database db=connection();
        try {
            List rows=db.query(sql,params); List<Object[]> result=new ArrayList<Object[]>(rows.size());
            for(Object item:rows) {
                Map row=(Map)item;
                Object[] values=row.values().toArray();
                if(values.length!=kinds.length) throw new IOException("Unexpected SQL projection width");
                for(int i=0;i<values.length;i++) {
                    if(kinds[i]==Attribute.REAL && values[i] instanceof Number) values[i]=Double.valueOf(((Number)values[i]).doubleValue());
                }
                result.add(values);
            }
            return result;
        } finally { release(db); }
    }
    public int execute(String sql,Object[] params) throws IOException {
        Database db=connection(); try { return db.execute(sql,params); } finally { release(db); }
    }
    public long insert(String sql,Object[] params,String keyColumn) throws IOException {
        Database db=connection(); try { return db.insert(sql,params,keyColumn); } finally { release(db); }
    }
    public void prepareGenerator(int strategy,String name) throws IOException {
        if(strategy==1) return;
        if(strategy==2 && "postgresql".equals(dialect.getName())) {
            execute("CREATE SEQUENCE IF NOT EXISTS "+quote(name)+" START WITH 1",new Object[0]);return;
        }
        execute("CREATE TABLE IF NOT EXISTS cn1_orm_sequences (sequence_name "+dialect.assignedKeyColumn(Dialect.TEXT)+", next_value "+dialect.columnType(Dialect.BIGINT)+" NOT NULL)",new Object[0]);
        String sql="INSERT "+("mysql".equals(dialect.getName())?"IGNORE ":"")+"INTO cn1_orm_sequences (sequence_name,next_value) VALUES (?,0)";
        if(!"mysql".equals(dialect.getName())) sql+=" ON CONFLICT (sequence_name) DO NOTHING";
        execute(sql,new Object[]{name});
    }
    public Object nextIdentifier(int strategy,String name,int kind) throws IOException {
        if(strategy==1) {
            byte[] bytes=com.codename1.backend.Crypto.randomBytes(16);
            bytes[6]=(byte)((bytes[6]&15)|64);bytes[8]=(byte)((bytes[8]&63)|128);
            String hex="0123456789abcdef";StringBuilder value=new StringBuilder();
            for(int i=0;i<bytes.length;i++) {
                if(i==4 || i==6 || i==8 || i==10) value.append('-');
                value.append(hex.charAt((bytes[i]>>>4)&15)).append(hex.charAt(bytes[i]&15));
            }
            return value.toString();
        }
        if(strategy==2 && "postgresql".equals(dialect.getName()))
            return query("SELECT nextval(CAST(? AS regclass))",new Object[]{quote(name)},new int[]{Dialect.BIGINT}).get(0)[0];
        long max=kind==Dialect.INTEGER?Integer.MAX_VALUE:Long.MAX_VALUE;
        if(execute("UPDATE cn1_orm_sequences SET next_value = next_value + 1 WHERE sequence_name = ? AND next_value < ?",new Object[]{name,Long.valueOf(max)})!=1)
            throw new IOException("Identifier generator is missing or exhausted: "+name);
        return query("SELECT next_value FROM cn1_orm_sequences WHERE sequence_name = ?",new Object[]{name},new int[]{Dialect.BIGINT}).get(0)[0];
    }
    public void begin() throws IOException {
        if(transaction!=null) throw new IOException("Transaction already active");
        if(supplied==null && pool!=null) {
            // Inside a @Transactional method: this session becomes part of that
            // transaction instead of opening one the connection would refuse.
            Database joined=com.codename1.backend.Transactions.joined(pool);
            if(joined!=null) { transaction=joined; joinedTransaction=true; return; }
        }
        Database db=supplied!=null?supplied:pool.borrow();
        try {
            if("sqlite".equals(dialect())) db.execute("PRAGMA foreign_keys = ON",new Object[0]);
            db.beginExclusiveTransaction(); transaction=db;
        } finally { release(db); }
    }
    public boolean isTransactionActive() { return transaction!=null && transaction.isInTransaction(); }
    public void commit() throws IOException {
        if(transaction==null) throw new IOException("No transaction");
        if(joinedTransaction) { unpin(); return; }
        transaction.commitTransaction(); unpin();
    }
    public void rollback() throws IOException {
        if(transaction==null) throw new IOException("No transaction");
        if(joinedTransaction) { com.codename1.backend.Transactions.markRollbackOnly(pool); unpin(); return; }
        transaction.rollbackTransaction(); unpin();
    }
    private void unpin() { Database db=transaction; transaction=null; joinedTransaction=false; release(db); }
    public void close() throws IOException {
        if(transaction!=null && joinedTransaction) {
            // Closed without committing: its changes were never flushed, and the
            // method's transaction cannot commit as though they had been.
            com.codename1.backend.Transactions.markRollbackOnly(pool); unpin(); return;
        }
        if(transaction!=null) {
            boolean rolledBack=false;
            try { transaction.rollbackTransaction();rolledBack=true; }
            finally {
                // A failed rollback may leave both a transaction and its thread
                // reservation active. Close it before the pool can reuse it.
                try { if(!rolledBack) transaction.close(); }
                finally { unpin(); }
            }
        }
    }
}
