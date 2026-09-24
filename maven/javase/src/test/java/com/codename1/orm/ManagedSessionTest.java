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
        double floating;
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

    @Test void predicatesRejectIncompatibleTypesBeforeExecution() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();seed(session);
            for(String predicate:new String[]{"r.counter AND r.id=1","r.name OR r.id=1","NOT r.counter","r.counter","r.name","r.name=r.counter","r.name>1","r.counter like '1%'","r.name like r.counter","r.name between 1 and 2","r.name in (1,2)"}) {
                assertThrows(IllegalArgumentException.class,()->session.createQuery("select r from ManagedSessionTest$Record r where "+predicate),predicate);
            }
            assertThrows(IllegalArgumentException.class,()->session.query(Record.class).like("counter","1%"));
            assertThrows(IllegalArgumentException.class,()->session.query(Record.class).like("bytes","1%"));
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select r from ManagedSessionTest$Record r where :flag").setParameter("flag",1L).list());
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select r from ManagedSessionTest$Record r where r.name=:name").setParameter("name",1L).list());
            assertEquals(1,session.createQuery("select r from ManagedSessionTest$Record r where :flag and NOT false").setParameter("flag",true).list().size());
            assertEquals(Boolean.TRUE,session.createQuery("select r.counter=0 from ManagedSessionTest$Record r",Boolean.class).first());
            assertEquals(1,session.createQuery("select r from ManagedSessionTest$Record r where r.counter < 0.5").list().size());session.close();
        } finally { em.close(); }
    }

    @Test void scalarSubqueriesAndIntegralSumsPreserveResultTypes() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();Record r=seed(session);session.beginTransaction();r.counter=9007199254740993L;session.commitTransaction();
            assertEquals("first",session.createQuery("select (select max(i.name) from ManagedSessionTest$Record i) from ManagedSessionTest$Record r",String.class).first());
            assertEquals(Long.valueOf(r.counter),session.createQuery("select (select max(i.counter) from ManagedSessionTest$Record i) from ManagedSessionTest$Record r",Long.class).first());
            assertEquals(Long.valueOf(r.counter),session.createQuery("select sum(r.counter) from ManagedSessionTest$Record r",Long.class).first());
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select (select max(i.name) from ManagedSessionTest$Record i)+1 from ManagedSessionTest$Record r"));session.close();
        } finally { em.close(); }
    }

    @Test void publicInListsRespectTheCombinedParameterLimit() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();Record r=seed(session);Object[] allowed=new Object[999],tooMany=new Object[1000],half=new Object[500];java.util.Arrays.fill(allowed,r.id);java.util.Arrays.fill(tooMany,r.id);java.util.Arrays.fill(half,r.id);
            assertEquals(1,session.query(Record.class).in("id",allowed).list().size());
            assertThrows(IllegalArgumentException.class,()->session.query(Record.class).in("id",tooMany));
            assertThrows(IllegalArgumentException.class,()->session.query(Record.class).in("id",half).in("id",half));
            assertEquals(1,session.createQuery("select r from ManagedSessionTest$Record r where r.id in :ids").setParameter("ids",allowed).list().size());
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select r from ManagedSessionTest$Record r where r.id in :ids").setParameter("ids",tooMany).list());
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select r from ManagedSessionTest$Record r where r.id in :a and r.id in :b").setParameter("a",half).setParameter("b",half).list());
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select r from ManagedSessionTest$Record r where r.id in :ids and r.name=:name").setParameter("ids",allowed).setParameter("name","first").list());session.close();
        } finally { em.close(); }
    }

    @Test void generatedIdInsertRejectsNaNBeforeStoringARow() throws Exception {
        EntityManager em=manager();
        try {
            Model model=new Model() {
                public Attribute[] attributes() { Attribute[] result=super.attributes().clone();result[2]=new Attribute("floating","counter",Attribute.REAL,false,false,true,false);return result; }
                public Object get(Record record,int index) { return index==2?Double.valueOf(record.floating):super.get(record,index); }
            };
            java.util.Map<String,EntityModel<?>> models=new java.util.LinkedHashMap<String,EntityModel<?>>();models.put(Record.class.getName(),model);
            Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.SessionSqlAccess(em.database()),models);session.createTables();
            session.beginTransaction();Record record=new Record();record.floating=Double.NaN;
            assertThrows(PersistenceException.class,()->{session.persist(record);session.flush();});session.rollbackTransaction();
            assertEquals(0,session.query(Record.class).count());session.close();
        } finally { em.close(); }
    }

    @Test void clientAdapterRejectsNonportableValuesBeforeBinding() throws Exception {
        EntityManager em=manager();
        try {
            com.codename1.impl.orm.SessionSqlAccess access=new com.codename1.impl.orm.SessionSqlAccess(em.database());
            access.execute("CREATE TABLE portable_values (id INTEGER PRIMARY KEY AUTOINCREMENT, value REAL)",new Object[0]);
            for(Object value:new Object[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,"bad\0text"}) {
                assertThrows(java.io.IOException.class,()->access.query("SELECT ?",new Object[]{value},new int[]{Attribute.REAL}));
                assertThrows(java.io.IOException.class,()->access.execute("INSERT INTO portable_values(value) VALUES (?)",new Object[]{value}));
                assertThrows(java.io.IOException.class,()->access.insert("INSERT INTO portable_values(value) VALUES (?)",new Object[]{value},"id"));
            }
            assertEquals(Long.valueOf(0),access.query("SELECT COUNT(*) FROM portable_values",new Object[0],new int[]{Attribute.BIGINT}).get(0)[0]);
            assertEquals(1,access.insert("INSERT INTO portable_values(value) VALUES (?)",new Object[]{1.5},"id"));
            assertEquals(Double.valueOf(1.5),access.query("SELECT value FROM portable_values",new Object[0],new int[]{Attribute.REAL}).get(0)[0]);
        } finally { em.close(); }
    }

    @Test void functionsValidateUnknownOperandsAgainstKnownKinds() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();seed(session);
            for(String expression:new String[]{"coalesce(:value,r.counter)","coalesce(r.counter,:value)","nullif(:value,r.counter)","nullif(r.counter,:value)","coalesce(coalesce(:value,:other),r.counter)"}) {
                com.codename1.orm.session.JpqlQuery<?> query=session.createQuery("select "+expression+" from ManagedSessionTest$Record r").setParameter("value","oops");
                if(expression.contains(":other")) query.setParameter("other",null);
                assertThrows(IllegalArgumentException.class,query::list,expression);
            }
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select coalesce(:value,r.name) from ManagedSessionTest$Record r").setParameter("value",1L).list());
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select coalesce(:value,r.bytes) from ManagedSessionTest$Record r").setParameter("value","oops").list());
            assertEquals(Long.valueOf(7),session.createQuery("select coalesce(:value,r.counter) from ManagedSessionTest$Record r",Long.class).setParameter("value",7L).first());
            assertEquals(Long.valueOf(7),session.createQuery("select nullif(:value,r.counter) from ManagedSessionTest$Record r",Long.class).setParameter("value",7L).first());
            assertEquals(Long.valueOf(1),session.createQuery("select length('é') from ManagedSessionTest$Record r",Long.class).first());
            session.close();
        } finally { em.close(); }
    }

    @Test void groupingTracksCorrelatedSubqueryFields() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();seed(session);
            String correlated="(select max(i.name) from ManagedSessionTest$Record i where i.id=r.id)";
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select "+correlated+",count(r.id) from ManagedSessionTest$Record r"));
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select count(r.id) from ManagedSessionTest$Record r having "+correlated+"='first'"));
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select count(r.id) from ManagedSessionTest$Record r order by "+correlated));
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select count(r.id) from ManagedSessionTest$Record r having exists (select i.id from ManagedSessionTest$Record i where i.id=r.id)"));
            String nested="(select max(i.name) from ManagedSessionTest$Record i where i.id=(select max(j.id) from ManagedSessionTest$Record j where j.id=r.id))";
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select "+nested+",count(r.id) from ManagedSessionTest$Record r"));
            assertEquals("first",((Object[])session.createQuery("select "+correlated+",count(r.id) from ManagedSessionTest$Record r group by r.id").first())[0]);
            assertEquals("first",((Object[])session.createQuery("select "+nested+",count(r.id) from ManagedSessionTest$Record r group by r.id").first())[0]);
            assertEquals(1,session.createQuery("select (select max(i.name) from ManagedSessionTest$Record i),count(r.id) from ManagedSessionTest$Record r").list().size());
            session.close();
        } finally { em.close(); }
    }

    @Test void builderOperandsMatchStorageKindsAndRejectedClausesLeaveQueryUsable() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();Record record=seed(session);
            for(String field:new String[]{"counter","name","bytes"}) {
                Object bad=field.equals("counter")?"abc":Long.valueOf(1);
                com.codename1.orm.session.Query<Record> query=session.query(Record.class);
                assertThrows(IllegalArgumentException.class,()->query.eq(field,bad));
                assertThrows(IllegalArgumentException.class,()->query.ne(field,bad));
                assertThrows(IllegalArgumentException.class,()->query.gt(field,bad));
                assertThrows(IllegalArgumentException.class,()->query.ge(field,bad));
                assertThrows(IllegalArgumentException.class,()->query.lt(field,bad));
                assertThrows(IllegalArgumentException.class,()->query.le(field,bad));
                assertThrows(IllegalArgumentException.class,()->query.in(field,null,bad));
                assertSame(record,query.eq("counter",0L).first());
            }
            assertSame(record,session.query(Record.class).in("counter",null,0).first());
            assertSame(record,session.query(Record.class).eq("bytes",new byte[]{1}).first());
            assertNull(session.query(Record.class).eq("bytes",null).first());
            session.close();
        } finally { em.close(); }
    }

    @Test void bulkAssignmentsRejectIncompatibleStorageBeforeWriting() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();Record record=seed(session);
            for(String assignment:new String[]{"r.counter='oops'","r.counter=r.name","r.name=1","r.bytes='oops'","r.counter=coalesce(:a,'oops')"}) {
                assertThrows(IllegalArgumentException.class,()->session.createQuery("update ManagedSessionTest$Record r set "+assignment));
            }
            session.beginTransaction();
            for(String assignment:new String[]{"r.counter=:value","r.counter=(:value)","r.counter=coalesce(:value,:other)"}) {
                com.codename1.orm.session.JpqlQuery<?> query=session.createQuery("update ManagedSessionTest$Record r set "+assignment).setParameter("value","oops");
                if(assignment.contains(":other")) query.setParameter("other",0L);
                assertThrows(IllegalArgumentException.class,query::executeUpdate);
            }
            assertThrows(IllegalArgumentException.class,()->session.createQuery("update ManagedSessionTest$Record r set r.name=:value").setParameter("value",7L).executeUpdate());
            assertThrows(IllegalArgumentException.class,()->session.createQuery("update ManagedSessionTest$Record r set r.bytes=:value").setParameter("value","oops").executeUpdate());
            assertEquals(1,session.createQuery("update ManagedSessionTest$Record r set r.counter=:value,r.name=:name,r.bytes=:bytes").setParameter("value",7L).setParameter("name",null).setParameter("bytes",new byte[]{1,2}).executeUpdate());
            session.commitTransaction();Record loaded=session.find(Record.class,record.id);
            assertEquals(7,loaded.counter);assertNull(loaded.name);assertArrayEquals(new byte[]{1,2},loaded.bytes);
            session.close();
        } finally { em.close(); }
    }

    @Test void numericConstantProjectionsKeepTheirPlannedTypes() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();seed(session);
            for(String expression:new String[]{"1","abs(1)","coalesce(1,2)","nullif(1,2)","min(1)","max(1)","(select 1 from ManagedSessionTest$Record i)"}) {
                assertEquals(Long.valueOf(1),session.createQuery("select "+expression+" from ManagedSessionTest$Record r",Long.class).first(),expression);
            }
            for(String expression:new String[]{"1.5","abs(1.5)","coalesce(1.5,2.5)","avg(1.5)"}) {
                assertEquals(Double.valueOf(1.5),session.createQuery("select "+expression+" from ManagedSessionTest$Record r",Double.class).first(),expression);
            }
            session.close();
        } finally { em.close(); }
    }

    @Test void scalarProjectionsRequireKnownStorageKinds() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();seed(session);
            for(String expression:new String[]{":value","coalesce(:a,:b)","null","(select :value from ManagedSessionTest$Record i)"}) {
                assertThrows(IllegalArgumentException.class,()->session.createQuery("select "+expression+" from ManagedSessionTest$Record r"));
            }
            assertEquals(Long.valueOf(7),session.createQuery("select coalesce(:value,r.counter) from ManagedSessionTest$Record r",Long.class).setParameter("value",7L).first());
            session.close();
        } finally { em.close(); }
    }

    @Test void integralArithmeticRejectsOverflowBeforeReturningOrStoringValues() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();Record record=seed(session);
            String[] expressions={":value + 1",":value - 1",":value * 2",":value / -1","-:value","(:value + 0) + 1"};
            long[] values={Long.MAX_VALUE,Long.MIN_VALUE,Long.MAX_VALUE,Long.MIN_VALUE,Long.MIN_VALUE,Long.MAX_VALUE};
            for(int i=0;i<expressions.length;i++) {
                String expression=expressions[i];long value=values[i];
                assertThrows(PersistenceException.class,()->session.createQuery("select "+expression+" from ManagedSessionTest$Record r",Long.class).setParameter("value",value).list());
                session.beginTransaction();
                assertThrows(PersistenceException.class,()->session.createQuery("update ManagedSessionTest$Record r set r.counter="+expression).setParameter("value",value).executeUpdate());
                session.rollbackTransaction();
                assertEquals(0,session.find(Record.class,record.id).counter);
            }
            assertEquals(Long.valueOf(Long.MAX_VALUE),session.createQuery("select :value + 0 from ManagedSessionTest$Record r",Long.class).setParameter("value",Long.MAX_VALUE).first());
            assertEquals(Long.valueOf(Long.MIN_VALUE),session.createQuery("select :value - 0 from ManagedSessionTest$Record r",Long.class).setParameter("value",Long.MIN_VALUE).first());
            assertEquals(Long.valueOf(15),session.createQuery("select (:a + :b) * :c from ManagedSessionTest$Record r",Long.class).setParameter("a",2L).setParameter("b",3L).setParameter("c",3L).first());
            assertEquals(Long.valueOf(1),session.createQuery("select count(*) + 0 from ManagedSessionTest$Record r",Long.class).first());
            assertEquals(Long.valueOf(1),session.createQuery("select sum(r.counter + 1) from ManagedSessionTest$Record r",Long.class).first());
            session.close();
        } finally { em.close(); }
    }

    @Test void arithmeticValidatesOperandsAndUsesPortableDivision() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();Record r=seed(session);session.beginTransaction();r.counter=3;session.commitTransaction();
            for(String expression:new String[]{"r.name + 1","1 - r.name","r.bytes * 2","-r.name","+r.name","r / 2","r.counter % 1.5"}) {
                assertThrows(IllegalArgumentException.class,()->session.createQuery("select "+expression+" from ManagedSessionTest$Record r"),expression);
            }
            for(String expression:new String[]{":value + r.counter","r.counter / :value","-:value","+:value","coalesce(:value,r.counter)+1"}) {
                for(Object bad:new Object[]{"2",Boolean.TRUE,new java.util.Date(0),Double.valueOf(1.5)}) {
                    assertThrows(IllegalArgumentException.class,()->session.createQuery("select "+expression+" from ManagedSessionTest$Record r").setParameter("value",bad).list());
                }
            }
            assertEquals(Long.valueOf(5),session.createQuery("select r.counter + :value from ManagedSessionTest$Record r",Long.class).setParameter("value",2L).first());
            assertEquals(Double.valueOf(2.5),session.createQuery("select :value + 1.0 from ManagedSessionTest$Record r",Double.class).setParameter("value",1.5).first());
            assertEquals(Long.valueOf(1),session.createQuery("select r.counter / 2 from ManagedSessionTest$Record r",Long.class).first());
            assertEquals(Long.valueOf(-1),session.createQuery("select -r.counter / 2 from ManagedSessionTest$Record r",Long.class).first());
            assertEquals(Double.valueOf(1.5),session.createQuery("select r.counter / 2.0 from ManagedSessionTest$Record r",Double.class).first());
            assertNull(session.createQuery("select r.counter / 0 from ManagedSessionTest$Record r",Long.class).first());
            session.beginTransaction();session.createQuery("update ManagedSessionTest$Record r set r.counter = r.counter / 2").executeUpdate();session.commitTransaction();
            assertEquals(1,session.find(Record.class,r.id).counter);session.close();
        } finally { em.close(); }
    }

    @Test void groupedQueriesRejectUncoveredFieldsAndPreserveValidExpressions() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();seed(session);
            for(String query:new String[]{
                    "select r.name, count(r.id) from ManagedSessionTest$Record r",
                    "select length(r.name), count(r.id) from ManagedSessionTest$Record r",
                    "select r.name, count(r.id) from ManagedSessionTest$Record r group by r.counter",
                    "select r.name from ManagedSessionTest$Record r group by lower(r.name)",
                    "select count(r.id) from ManagedSessionTest$Record r having r.name is not null",
                    "select count(r.id) from ManagedSessionTest$Record r having r.name like 'f%'",
                    "select count(r.id) from ManagedSessionTest$Record r order by r.name",
                    "select count(r.id) from ManagedSessionTest$Record r where count(r.id) > 0",
                    "select sum(count(r.id)) from ManagedSessionTest$Record r",
                    "select r.name from ManagedSessionTest$Record r group by count(r.id)",
                    "select coalesce(r.name,'x'),count(r.id) from ManagedSessionTest$Record r group by coalesce(r.name,'y')"}) {
                assertThrows(IllegalArgumentException.class,()->session.createQuery(query),query);
            }
            assertEquals(1,session.createQuery("select r.name, count(r.id) from ManagedSessionTest$Record r group by r.name having count(r.id)>0").list().size());
            assertEquals(1,session.createQuery("select upper(r.name), count(r.id) from ManagedSessionTest$Record r group by r.name order by count(r.id)").list().size());
            assertEquals(1,session.createQuery("select lower(r.name), count(r.id) from ManagedSessionTest$Record r group by lower(r.name)").list().size());
            assertEquals(Long.valueOf(2),session.createQuery("select count(r.id)+1 from ManagedSessionTest$Record r",Long.class).first());session.close();
        } finally { em.close(); }
    }

    @Test void collectionSchemasValidateTypesNullabilityAndKeyRoles() throws Exception {
        for(String definition:new String[]{
                "owner_id BIGINT NOT NULL, position INTEGER NOT NULL, value INTEGER, PRIMARY KEY(owner_id,position)",
                "owner_id TEXT NOT NULL, position INTEGER NOT NULL, value TEXT, PRIMARY KEY(owner_id,position)",
                "owner_id BIGINT NOT NULL, position TEXT NOT NULL, value TEXT, PRIMARY KEY(owner_id,position)",
                "owner_id BIGINT NOT NULL, position INTEGER NOT NULL, value TEXT NOT NULL, PRIMARY KEY(owner_id,position)",
                "owner_id BIGINT NOT NULL, position INTEGER NOT NULL, value TEXT",
                "owner_id BIGINT NOT NULL, position INTEGER NOT NULL, value TEXT, extra INTEGER, PRIMARY KEY(owner_id,position,extra)"}) {
            EntityManager em=manager();
            try {
                Models.register(new Model() { public com.codename1.impl.orm.Relationship[] relationships() { return new com.codename1.impl.orm.Relationship[]{new com.codename1.impl.orm.Relationship("tags",String.class,true,true,-1,"","record_tags","owner_id","value",0,false,false,"","position","",true)}; } });
                Session session=em.openSession();session.createTables();session.validateSchema();
                em.database().execute("DROP TABLE record_tags",new Object[0]);em.database().execute("CREATE TABLE record_tags ("+definition+")",new Object[0]);
                assertThrows(PersistenceException.class,session::validateSchema,definition);session.close();
            } finally { em.close(); }
        }
    }

    @Test void orderedJoinAndMapSchemasValidateTheirDistinctKeyRoles() throws Exception {
        for(int shape=0;shape<3;shape++) {
            final int mode=shape;EntityManager em=manager();
            try {
                Models.register(new Model() { public com.codename1.impl.orm.Relationship[] relationships() { return new com.codename1.impl.orm.Relationship[]{new com.codename1.impl.orm.Relationship("links",mode==2?String.class:Record.class,true,true,-1,"","record_links","owner_id",mode==2?"value":"target_id",0,false,false,mode==2?"map_key":"",mode==0?"":"position","",mode==2)}; } });
                Session session=em.openSession();session.createTables();session.validateSchema();em.database().execute("DROP TABLE record_links",new Object[0]);
                String definition=mode==2?"owner_id BIGINT NOT NULL, value TEXT, map_key TEXT NOT NULL, position INTEGER, PRIMARY KEY(owner_id,map_key)"
                        :"owner_id BIGINT NOT NULL, target_id TEXT NOT NULL"+(mode==1?", position INTEGER NOT NULL":"")+", PRIMARY KEY(owner_id,"+(mode==1?"position":"target_id")+")";
                em.database().execute("CREATE TABLE record_links ("+definition+")",new Object[0]);
                PersistenceException error=assertThrows(PersistenceException.class,session::validateSchema);
                assertTrue(error.getMessage().contains(mode==2?"Nullability mismatch on collection record_links.position":"Storage type mismatch on collection record_links.target_id"));session.close();
            } finally { em.close(); }
        }
    }

    @Test void schemaIntegerFamiliesRejectIntervalAndPoint() throws Exception {
        for(String type:new String[]{"INTERVAL","POINT","INTEGER","BIGINT","INT8"}) {
            EntityManager em=manager();
            try {
                em.database().execute("CREATE TABLE managed_record (id INTEGER PRIMARY KEY, version BIGINT NOT NULL, counter "+type+" NOT NULL, name TEXT, bytes BLOB)",new Object[0]);
                Session session=em.openSession();session.createTables();
                if(type.equals("INTERVAL") || type.equals("POINT")) assertThrows(PersistenceException.class,session::validateSchema);
                else session.validateSchema();session.close();
            } finally { em.close(); }
        }
    }

    @Test void builderLikeConvertsPatternsBeforeDialectNormalization() throws Exception {
        EntityManager em=manager();
        try {
            Models.register(new Model() {
                public Object get(Record r,int i) { return i==3 ? encode(r.name) : super.get(r,i); }
                public void set(Record r,int i,Object value) { super.set(r,i,i==3 && value!=null ? ((String)value).substring(2) : value); }
                public Object parameter(int i,Object value) { return i==3 ? encode((String)value) : super.parameter(i,value); }
                private String encode(String value) { return value==null ? null : "x:"+value; }
            });
            Session session=em.openSession();Record r=seed(session);session.clear();
            assertEquals(r.id,session.query(Record.class).like("name","fir%").first().id);
            assertEquals(r.id,session.createQuery("select r from ManagedSessionTest$Record r where r.name like :pattern",Record.class).setParameter("pattern","fir%").first().id);
            assertTrue(session.query(Record.class).like("name",null).list().isEmpty());session.close();
        } finally { em.close(); }
    }

    @Test void jpqlRejectsMalformedFunctionsAtQueryCreation() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();seed(session);
            for(String expression:new String[]{"lower(r.counter)","upper(r.counter)","trim(r.counter)","length(r.counter)","sum(r.name)","avg(r.name)","abs(r.name)","length(r.name,r.name)","lower()","count(r.id,r.name)","coalesce(r.name)","nullif(r.name)","nullif(r.name,r.counter)","abs(*)","count(distinct *)","lower(distinct r.name)","max(r.bytes)"}) {
                assertThrows(IllegalArgumentException.class,()->session.createQuery("select "+expression+" from ManagedSessionTest$Record r"),expression);
            }
            assertEquals("FIRST",session.createQuery("select upper(trim(r.name)) from ManagedSessionTest$Record r",String.class).first());
            assertEquals(Long.valueOf(5),session.createQuery("select length(r.name) from ManagedSessionTest$Record r",Long.class).first());
            assertEquals(Long.valueOf(1),session.createQuery("select count(*) from ManagedSessionTest$Record r",Long.class).first());
            assertEquals(Long.valueOf(0),session.createQuery("select sum(abs(r.counter)) from ManagedSessionTest$Record r",Long.class).first());
            assertNull(session.createQuery("select nullif(r.name,r.name) from ManagedSessionTest$Record r",String.class).first());session.close();
        } finally { em.close(); }
    }

    @Test void managedTextKeysHaveTheSameBoundOnEveryRuntime() throws Exception {
        String boundary=new String(new char[255]).replace('\0','a'),tooLong=boundary+"b";
        for(int shape=0;shape<5;shape++) {
            final int mode=shape;
            EntityManager em=manager();
            try {
                Models.register(new Model() {
                    public Attribute[] attributes() {
                        Attribute[] a=super.attributes().clone();
                        if(mode==0) { a[0]=new Attribute("id","id",Attribute.BIGINT,false,false,false,false);a[3]=new Attribute("name","name",Attribute.TEXT,true,false,false,false); }
                        if(mode==3) a[3]=new Attribute("name","name",Attribute.TEXT,false,false,true,false,"TEXT");
                        return a;
                    }
                    public com.codename1.impl.orm.Index[] indexes() { return mode==0 || mode==4 ? new com.codename1.impl.orm.Index[0] : new com.codename1.impl.orm.Index[]{new com.codename1.impl.orm.Index("text_name_index",mode==2,"name")}; }
                });
                Session session=em.openSession();session.createTables();session.beginTransaction();Record r=new Record();r.name=boundary;session.persist(r);session.commitTransaction();
                session.beginTransaction();Record longRecord=new Record();longRecord.name=tooLong;
                if(mode<3) { assertThrows(PersistenceException.class,()->{session.persist(longRecord);session.flush();});session.rollbackTransaction(); }
                else { session.persist(longRecord);session.commitTransaction(); }
                if(mode==1 || mode==2) {
                    session.beginTransaction();Record loaded=session.find(Record.class,r.id);loaded.name=tooLong;assertThrows(PersistenceException.class,session::flush);session.rollbackTransaction();
                    assertEquals(boundary,session.find(Record.class,r.id).name);
                }
                session.close();
            } finally { em.close(); }
        }
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
