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
package com.codename1.orm;

import com.codename1.impl.javase.SEDatabase;
import com.codename1.impl.orm.Attribute;
import com.codename1.impl.orm.EntityModel;
import com.codename1.impl.orm.Models;
import com.codename1.orm.session.OptimisticLockException;
import com.codename1.orm.session.PersistenceException;
import com.codename1.orm.session.Session;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ManagedSessionTest {
    static class Record {
        long id,version,counter;
        String name;
        byte[] bytes;
    }
    static class Model extends EntityModel<Record> {
        private final Attribute[] attrs={
            new Attribute("id","id",Attribute.BIGINT,true,true,false,false),
            new Attribute("version","version",Attribute.BIGINT,false,false,false,true),
            new Attribute("counter","counter",Attribute.BIGINT,false,false,false,false),
            new Attribute("name","name",Attribute.TEXT,false,false,true,false),
            new Attribute("bytes","bytes",Attribute.BLOB,false,false,true,false)
        };
        public Class<Record> type() { return Record.class; }
        public String table() { return "managed_record"; }
        public boolean counter(int index) { return index==2; }
        public Attribute[] attributes() { return attrs; }
        public Record create() { return new Record(); }
        public Object get(Record r,int i) {
            switch(i) { case 0:return r.id; case 1:return r.version;case 2:return r.counter;case 3:return r.name;default:return r.bytes; }
        }
        public void set(Record r,int i,Object value) {
            switch(i) {
                case 0:r.id=((Number)value).longValue();break;
                case 1:r.version=((Number)value).longValue();break;
                case 2:r.counter=((Number)value).longValue();break;
                case 3:r.name=(String)value;break;
                case 4:r.bytes=(byte[])value;break;
            }
        }
    }
    private EntityManager manager() throws Exception {
        Models.register(new Model());
        Class.forName("org.sqlite.JDBC");
        return EntityManager.open(new SEDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite::memory:")));
    }
    private Record seed(Session session) {
        session.createTables();session.validateSchema(); session.beginTransaction();
        Record r=new Record();r.name="first";r.bytes=new byte[]{1};session.persist(r);session.commitTransaction();return r;
    }
    @Test void schemaValidationRejectsIncompatibleColumnsAndSqliteRowLocks() throws Exception {
        EntityManager em=manager();
        try {
            Session s=em.openSession();Record r=seed(s);s.validateSchema();
            s.beginTransaction();
            assertThrows(UnsupportedOperationException.class,()->s.lock(r,com.codename1.orm.session.LockMode.PESSIMISTIC_WRITE));
            s.rollbackTransaction();s.close();
            Models.register(new Model() { public String table() { return "missing_record_table"; } });
            Session missing=em.openSession();assertThrows(PersistenceException.class,missing::validateSchema);missing.close();
        } finally { em.close(); }
    }

    @Test void jpqlQueriesBindValuesProjectGroupAndExecuteBulkDml() throws Exception {
        EntityManager em=manager();
        try {
            Session s=em.openSession();Record r=seed(s);
            assertSame(r,s.createQuery("select r from ManagedSessionTest$Record r where r.name = :name and (r.counter = 0 or r.counter > 3)",Record.class).setParameter("name","first").first());
            assertEquals(Long.valueOf(1),s.createQuery("select count(r) from ManagedSessionTest$Record r",Long.class).first());
            com.codename1.orm.session.JpqlQuery<Record> selection=s.createQuery("select r from ManagedSessionTest$Record r where r.name in :names and r.counter < 1e2",Record.class);
            assertSame(r,selection.setParameter("names",java.util.Arrays.asList("first","second")).first());
            assertNull(selection.setParameter("names",java.util.Collections.emptyList()).first());
            assertSame(r,selection.setParameter("names",new String[]{"first"}).first());
            assertNull(s.createQuery("select r from ManagedSessionTest$Record r where r.name = :name",Record.class).setParameter("name","' OR 1=1 --").first());
            Object[] projection=s.createQuery("select r.name, sum(r.counter) from ManagedSessionTest$Record r group by r.name having count(r) > 0 order by r.name",Object[].class).first();
            assertEquals("first",projection[0]);assertEquals(0,((Number)projection[1]).longValue());
            assertArrayEquals(new Object[]{"first","first"},s.createQuery("select r.name,r.name from ManagedSessionTest$Record r",Object[].class).first());
            assertEquals(Long.valueOf(1),s.createQuery("select count(r) from ManagedSessionTest$Record r where exists (select x.id from ManagedSessionTest$Record x where x.id = r.id)",Long.class).first());
            assertThrows(IllegalArgumentException.class,()->s.createQuery("select r from ManagedSessionTest$Record r where r.id = :id",Record.class).list());
            assertThrows(IllegalArgumentException.class,()->s.createQuery("select r from ManagedSessionTest$Record r; delete from ManagedSessionTest$Record",Record.class));
            assertThrows(IllegalArgumentException.class,()->s.createQuery("select unsupported(r.name) from ManagedSessionTest$Record r",String.class));
            s.beginTransaction();r.name="flushed";
            assertEquals(1,s.createQuery("update ManagedSessionTest$Record r set r.counter = r.counter + :delta where r.name = :name").setParameter("delta",4L).setParameter("name","flushed").executeUpdate());
            assertFalse(s.contains(r));s.commitTransaction();assertEquals(4,s.find(Record.class,r.id).counter);
            s.beginTransaction();assertEquals(1,s.createQuery("delete from ManagedSessionTest$Record r where r.counter between 3 and 5").executeUpdate());s.commitTransaction();
            assertEquals(0,s.query(Record.class).count());s.close();
        } finally { em.close(); }
    }

    @Test void sqliteBulkDeletePreservesCorrelatedPredicatesWithoutTargetAliases() throws Exception {
        EntityManager em=manager();
        try {
            Session s=em.openSession();Record first=seed(s);s.beginTransaction();Record kept=new Record();kept.name="keep";s.persist(kept);s.commitTransaction();
            s.beginTransaction();
            assertEquals(1,s.createQuery("update ManagedSessionTest$Record r set r.counter = r.counter + :delta where exists (select x.id from ManagedSessionTest$Record x where x.id = r.id and x.name = :name)").setParameter("delta",2L).setParameter("name","first").executeUpdate());s.commitTransaction();assertEquals(2,s.find(Record.class,first.id).counter);assertEquals(0,s.find(Record.class,kept.id).counter);
            s.beginTransaction();
            assertEquals(1,s.createQuery("delete from ManagedSessionTest$Record r where exists (select x.id from ManagedSessionTest$Record x where x.id = r.id and x.name = :name)").setParameter("name","first").executeUpdate());
            s.commitTransaction();assertNull(s.find(Record.class,first.id));assertNotNull(s.find(Record.class,kept.id));assertEquals(1,s.query(Record.class).count());s.close();
        } finally { em.close(); }
    }

    @Test void coalesceAndDistinctProjectionValidationArePortable() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();seed(session);
            assertEquals(Long.valueOf(0),session.createQuery("select coalesce(NULL, r.counter) from ManagedSessionTest$Record r",Long.class).first());
            assertEquals(Long.valueOf(0),session.createQuery("select coalesce(:missing, NULL, r.counter) from ManagedSessionTest$Record r",Long.class).setParameter("missing",null).first());
            assertEquals(Double.valueOf(0),session.createQuery("select coalesce(NULL, r.counter, 1.5) from ManagedSessionTest$Record r",Double.class).first());
            assertEquals("first",session.createQuery("select coalesce(NULL, r.name) from ManagedSessionTest$Record r",String.class).first());
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select distinct r.name from ManagedSessionTest$Record r order by r.id",String.class));
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select distinct r from ManagedSessionTest$Record r order by lower(r.name)",Record.class));
            assertEquals("first",session.createQuery("select distinct lower(r.name) from ManagedSessionTest$Record r order by lower(r.name)",String.class).first());session.close();
        } finally { em.close(); }
    }

    @Test void likePredicatesUseCaseSensitiveWildcardsAndExplicitEscapes() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();session.createTables();session.beginTransaction();
            for(String name:new String[]{"Alpha","alpha","a*b?c[d]","100%","100_","a\\b"}) { Record row=new Record();row.name=name;session.persist(row); }session.commitTransaction();
            assertEquals(1,session.query(Record.class).like("name","A%").list().size());
            assertEquals(1,session.createQuery("select r from ManagedSessionTest$Record r where r.name like :pattern",Record.class).setParameter("pattern","A%").list().size());
            assertEquals(5,session.createQuery("select r from ManagedSessionTest$Record r where r.name not like 'A%'",Record.class).list().size());
            for(String pattern:new String[]{"a*b?c[d]","a\\b"}) { assertEquals(pattern,session.query(Record.class).like("name",pattern).first().name); }
            assertEquals("100%",session.createQuery("select r from ManagedSessionTest$Record r where r.name like :pattern escape :escape",Record.class).setParameter("pattern","100!%").setParameter("escape","!").first().name);
            assertEquals("100_",session.createQuery("select r from ManagedSessionTest$Record r where r.name like '100!_' escape '!'",Record.class).first().name);
            assertEquals(1,session.createQuery("select r from ManagedSessionTest$Record r where r.name like upper(:pattern)",Record.class).setParameter("pattern","a%").list().size());
            assertEquals(6,session.createQuery("select r from ManagedSessionTest$Record r where r.name like r.name",Record.class).list().size());
            assertTrue(session.createQuery("select r from ManagedSessionTest$Record r where r.name like :pattern",Record.class).setParameter("pattern",null).list().isEmpty());session.close();
        } finally { em.close(); }
    }

    @Test void schemaValidationRejectsUnexpectedNotNull() throws Exception {
        EntityManager em=manager();
        try {
            em.database().execute("CREATE TABLE managed_record (id INTEGER PRIMARY KEY AUTOINCREMENT, version INTEGER NOT NULL, counter INTEGER NOT NULL, name TEXT NOT NULL, bytes BLOB)",new Object[0]);
            Session session=em.openSession();PersistenceException error=assertThrows(PersistenceException.class,session::validateSchema);assertTrue(error.getMessage().contains("Unexpected NOT NULL on managed_record.name"));session.close();
        } finally { em.close(); }
    }

    @Test void counterOverflowAndCrossSessionTransactionReadsAreRejected() throws Exception {
        EntityManager em=manager();
        try {
            Session s=em.openSession(),other=em.openSession();Record r=seed(s);
            s.beginTransaction();r.counter=Long.MAX_VALUE;s.commitTransaction();
            s.beginTransaction();assertFalse(s.increment(Record.class,r.id,"counter",1));
            assertEquals(Long.MAX_VALUE,r.counter);
            assertThrows(PersistenceException.class,()->other.query(Record.class).count());
            s.rollbackTransaction();s.close();other.close();
        } finally { em.close(); }
    }

    @Test void portableTableAndUuidIdentifiersAreGenerated() throws Exception {
        EntityManager em=manager();
        try {
            Models.register(new Model() {
                public int generation() { return 3; }
                public String generator() { return "managed_record_ids"; }
                public Attribute[] attributes() {
                    Attribute[] attrs=super.attributes().clone();
                    attrs[0]=new Attribute("id","id",Attribute.BIGINT,true,false,false,false);return attrs;
                }
            });
            Session s=em.openSession();s.createTables();s.beginTransaction();Record a=new Record(),b=new Record();s.persist(a);s.persist(b);
            assertEquals(1,a.id);assertEquals(2,b.id);s.commitTransaction();s.close();
            Models.register(new EntityModel<Token>() {
                public Class<Token> type() { return Token.class; }
                public String table() { return "managed_token"; }
                public Attribute[] attributes() { return new Attribute[]{new Attribute("id","id",Attribute.TEXT,true,false,false,false)}; }
                public Token create() { return new Token(); }
                public Object get(Token token,int index) { return token.id; }
                public void set(Token token,int index,Object value) { token.id=(String)value; }
                public int generation() { return 1; }
            });
            s=em.openSession();s.createTables();s.beginTransaction();Token one=new Token(),two=new Token();s.persist(one);s.persist(two);s.commitTransaction();
            assertNotEquals(one.id,two.id);assertTrue(one.id.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"));s.close();
        } finally { em.close(); }
    }
    static class Token { String id; }

    @Test void identityDirtyCheckingMutableValuesAndVersion() throws Exception {
        EntityManager em=manager();
        try {
            Session s=em.openSession();Record r=seed(s);
            assertSame(r,s.find(Record.class,r.id));
            s.beginTransaction();r.name="changed";r.bytes[0]=2;s.commitTransaction();assertEquals(1,r.version);
            s.clear();Record read=s.find(Record.class,r.id);
            assertEquals("changed",read.name);assertArrayEquals(new byte[]{2},read.bytes);
            assertSame(read,s.query(Record.class).eq("name","changed").first());s.close();
        } finally { em.close(); }
    }
    @Test void staleUpdateAndDeleteFailWithoutLosingCommittedChanges() throws Exception {
        EntityManager em=manager();
        try {
            Session a=em.openSession(),b=em.openSession();Record current=seed(a),stale=b.find(Record.class,current.id);
            a.beginTransaction();current.name="winner";a.commitTransaction();
            b.beginTransaction();stale.name="loser";
            assertThrows(OptimisticLockException.class,b::commitTransaction);assertTrue(b.isRollbackOnly());b.rollbackTransaction();
            assertFalse(b.contains(stale));assertEquals("winner",b.find(Record.class,current.id).name);
            b.beginTransaction();b.remove(b.find(Record.class,current.id));b.commitTransaction();
            a.beginTransaction();a.remove(current);assertThrows(OptimisticLockException.class,a::commitTransaction);a.rollbackTransaction();
            a.close();b.close();
        } finally { em.close(); }
    }
    @Test void rollbackCloseAndCounterHaveDefinedState() throws Exception {
        EntityManager em=manager();
        try {
            Session s=em.openSession();Record r=seed(s);
            assertThrows(PersistenceException.class,()->s.persist(new Record()));
            s.beginTransaction();assertTrue(s.increment(Record.class,r.id,"counter",3));assertEquals(3,r.counter);assertEquals(1,r.version);s.commitTransaction();
            s.beginTransaction();r.name="rolled back";s.flush();s.close();
            Session fresh=em.openSession();assertEquals("first",fresh.find(Record.class,r.id).name);
            assertEquals(3,fresh.find(Record.class,r.id).counter);
            assertThrows(PersistenceException.class,()->s.find(Record.class,r.id));fresh.close();
        } finally { em.close(); }
    }
    @Test void mergeReturnsManagedCopyAndDetectsStaleVersion() throws Exception {
        EntityManager em=manager();
        try {
            Session s=em.openSession();Record r=seed(s);s.detach(r);r.name="merged";
            s.beginTransaction();Record merged=s.merge(r);assertNotSame(r,merged);assertSame(merged,s.find(Record.class,r.id));s.commitTransaction();
            s.beginTransaction();assertThrows(OptimisticLockException.class,()->s.merge(r));s.rollbackTransaction();s.close();
        } finally { em.close(); }
    }
    @Test void invalidRefreshPreservesPendingChangesAndTransaction() throws Exception {
        EntityManager em=manager();
        try {
            Session s=em.openSession();Record saved=seed(s);
            s.beginTransaction();saved.name="pending update";
            Record fresh=new Record();fresh.name="pending insert";s.persist(fresh);
            assertThrows(PersistenceException.class,()->s.refresh(fresh));
            assertTrue(s.contains(saved));assertTrue(s.contains(fresh));
            assertFalse(s.isRollbackOnly());
            assertThrows(PersistenceException.class,()->s.refresh(new Record()));
            assertTrue(s.contains(saved));assertTrue(s.contains(fresh));
            assertFalse(s.isRollbackOnly());
            s.commitTransaction();s.clear();
            assertEquals("pending update",s.find(Record.class,saved.id).name);
            assertEquals("pending insert",s.find(Record.class,fresh.id).name);s.close();
        } finally { em.close(); }
    }

    @Test void uncheckedRefreshFailureDetachesContextAndRequiresRollback() throws Exception {
        for(boolean transactional:new boolean[]{false,true}) {
            EntityManager em=manager();boolean[] fail={false};RuntimeException failure=new IllegalStateException("post-load failed");
            Models.register(new Model() { public void lifecycle(Record record,int event) { if(event==6 && fail[0]) throw failure; } });
            Session s=em.openSession();
            try {
                Record saved=seed(s);s.beginTransaction();Record other=new Record();other.name="other";s.persist(other);s.commitTransaction();
                if(transactional) s.beginTransaction();saved.name="discard";fail[0]=true;
                assertSame(failure,assertThrows(IllegalStateException.class,()->s.refresh(saved)));
                assertFalse(s.contains(saved));assertFalse(s.contains(other));
                if(transactional) { assertTrue(s.isRollbackOnly());assertThrows(PersistenceException.class,s::commitTransaction);s.rollbackTransaction(); }
                else assertFalse(s.isTransactionActive());
                fail[0]=false;assertEquals("first",s.find(Record.class,saved.id).name);
            } finally { s.close();em.close(); }
        }
    }

    @Test void failedFlushPreservesCallbackExceptionAndRequiresRollback() throws Exception {
        EntityManager em=manager();
        RuntimeException failure=new IllegalStateException("callback failed");
        Models.register(new Model() {
            @Override public void lifecycle(Record record,int event) {
                if(event==2) { throw failure; }
            }
        });
        try {
            Session s=em.openSession();Record record=seed(s);
            s.beginTransaction();record.name="uncommitted";
            assertSame(failure,assertThrows(IllegalStateException.class,s::flush));
            assertTrue(s.isRollbackOnly());
            assertThrows(PersistenceException.class,s::commitTransaction);
            s.rollbackTransaction();
            assertEquals("first",s.find(Record.class,record.id).name);s.close();
        } finally { Models.register(new Model());em.close(); }
    }

    @Test void identifierMutationCannotRedirectRefreshOrDeletion() throws Exception {
        EntityManager em=manager();
        try {
            Session s=em.openSession();Record a=seed(s);long aId=a.id;
            s.beginTransaction();Record b=new Record();b.name="second";s.persist(b);s.commitTransaction();long bId=b.id;
            a.id=bId;s.detach(a);assertNotSame(a,s.find(Record.class,aId));assertSame(b,s.find(Record.class,bId));
            s.beginTransaction();s.remove(b);b.id=aId;assertThrows(PersistenceException.class,s::commitTransaction);s.rollbackTransaction();
            assertEquals(2,s.query(Record.class).count());assertEquals("second",s.find(Record.class,bId).name);
            Record changed=s.find(Record.class,aId);changed.id=bId;assertThrows(PersistenceException.class,()->s.refresh(changed));
            assertEquals("first",s.find(Record.class,aId).name);s.close();
        } finally { em.close(); }
    }

    @Test void countsIgnorePaginationAndLimitsDoNotLeak() throws Exception {
        EntityManager em=manager();
        try {
            Session s=em.openSession();seed(s);
            assertEquals(1,s.query(Record.class).limit(0).count());assertTrue(s.query(Record.class).limit(0).list().isEmpty());
            assertEquals(0,s.query(Record.class).in("id").count());
            assertThrows(IllegalArgumentException.class,()->s.query(Record.class).eq("id) OR 1=1 --",1));
            assertThrows(IllegalArgumentException.class,()->s.query(Record.class).limit(-1));s.close();
        } finally { em.close(); }
    }
}
