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
package com.codename1.backend.orm;

import com.codename1.backend.Database;
import com.codename1.orm.session.*;
import com.codename1.impl.orm.Attribute;
import com.codename1.impl.orm.Models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in wire-protocol tests; creates and removes only a uniquely named test schema. */
class ManagedSessionDatabaseTest {
    static class Cycle implements com.codename1.impl.orm.ManagedEntity {
        long id; Cycle next; com.codename1.impl.orm.EntityState state;
        public com.codename1.impl.orm.EntityState __cn1OrmState() { return state; }
        public void __cn1OrmState(com.codename1.impl.orm.EntityState value) { state=value; }
    }
    static class CycleModel extends com.codename1.impl.orm.EntityModel<Cycle> {
        public Class<Cycle> type() { return Cycle.class; }
        public String table() { return "required_cycle"; }
        public Attribute[] attributes() { return new Attribute[]{new Attribute("id","id",Attribute.BIGINT,true,false,false,false),new Attribute("next","next_id",Attribute.BIGINT,false,false,false,false)}; }
        public Cycle create() { return new Cycle(); }
        public Object get(Cycle value,int index) { return index==0?value.id:value.next==null?null:Long.valueOf(value.next.id); }
        public void set(Cycle value,int index,Object field) { if(index==0) value.id=((Number)field).longValue(); }
        public com.codename1.impl.orm.Relationship[] relationships() { return new com.codename1.impl.orm.Relationship[]{new com.codename1.impl.orm.Relationship("next",Cycle.class,false,false,1,"","","","",0,false)}; }
        public Object relation(Cycle value,int index) { return value.next; }
        public void relation(Cycle value,int index,Object target) { value.next=(Cycle)target; }
    }

    @Test void realDatabaseSupportsSequencesTransactionsCountersAndRowLocks() throws Exception {
        String url=System.getenv("CN1_ORM_TEST_DATABASE_URL");
        Assumptions.assumeTrue(url!=null && url.length()>0,"Set CN1_ORM_TEST_DATABASE_URL to an isolated PostgreSQL/MySQL/MariaDB database");
        Database db=Database.open(url);
        boolean mysql="mysql".equals(db.dialect().getName());
        String schema="cn1_orm_test_"+Long.toHexString(System.nanoTime());
        String quoted=db.dialect().quote(schema);
        String selectSchema=(mysql?"USE ":"SET search_path TO ")+quoted;
        db.execute((mysql?"CREATE DATABASE ":"CREATE SCHEMA ")+quoted,new Object[0]);
        db.execute(selectSchema,new Object[0]);
        Models.register(new ManagedSessionTest.Model() {
            public int generation() { return 2; }
            public String generator() { return "Record_Sequence"; }
            public Attribute[] attributes() {
                Attribute[] attrs=super.attributes().clone();
                attrs[0]=new Attribute("id","id",Attribute.BIGINT,true,false,false,false);return attrs;
            }
        });
        EntityManager manager=EntityManager.open(db);Session session=manager.openSession();
        ExecutorService workers=Executors.newFixedThreadPool(2);
        try {
            session.createTables();session.validateSchema();
            java.util.Map<String,com.codename1.impl.orm.EntityModel<?>> integerModels=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();integerModels.put(ManagedSessionTest.Record.class.getName(),ManagedSessionTest.integerModel());
            Session integerSession=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),integerModels);
            try { ManagedSessionTest.assertIntegerRanges(integerSession); } finally { integerSession.close(); }

            if(!mysql) {
                db.execute("ALTER TABLE managed_record RENAME COLUMN name TO \"Name\"",new Object[0]);
                PersistenceException mismatch=assertThrows(PersistenceException.class,session::validateSchema);
                assertTrue(mismatch.getMessage().contains("Missing column managed_record.name"));
                db.execute("ALTER TABLE managed_record RENAME COLUMN \"Name\" TO name",new Object[0]);session.validateSchema();
                for(String sqlType:new String[]{"INTERVAL","POINT","BOOLEAN"}) {
                    db.execute("ALTER TABLE managed_record ALTER COLUMN counter TYPE "+sqlType+" USING NULL::"+sqlType,new Object[0]);
                    assertThrows(PersistenceException.class,session::validateSchema);
                    db.execute("ALTER TABLE managed_record ALTER COLUMN counter TYPE BIGINT USING NULL::BIGINT",new Object[0]);session.validateSchema();
                }
            }
            session.beginTransaction();
            ManagedSessionTest.Record entity=new ManagedSessionTest.Record();entity.name="record";entity.bytes=new byte[]{1,2};session.persist(entity);
            assertTrue(entity.id>0);session.commitTransaction();session.clear();
            for(String expression:new String[]{"1","abs(1)","coalesce(1,2)","nullif(1,2)","min(1)","max(1)","(select max(1) from ManagedSessionTest$Record i)"}) {
                assertEquals(Long.valueOf(1),session.createQuery("select "+expression+" from ManagedSessionTest$Record r",Long.class).first(),expression);
            }
            for(String expression:new String[]{"1.5","abs(1.5)","coalesce(1.5,2.5)","avg(1.5)"}) {
                assertEquals(Double.valueOf(1.5),session.createQuery("select "+expression+" from ManagedSessionTest$Record r",Double.class).first(),expression);
            }
            for(String predicate:new String[]{":value is null",":value is not null","(:value) is null","coalesce(:a,:b) is null"}) {
                assertThrows(IllegalArgumentException.class,()->session.createQuery("select r from ManagedSessionTest$Record r where "+predicate));
            }
            assertEquals(1,session.createQuery("select r from ManagedSessionTest$Record r where coalesce(:value,r.name) is not null").setParameter("value",null).list().size());
            assertThrows(IllegalArgumentException.class,()->session.createQuery("update ManagedSessionTest$Record r set r.counter=1.5"));
            session.beginTransaction();
            assertThrows(IllegalArgumentException.class,()->session.createQuery("update ManagedSessionTest$Record r set r.counter=:value").setParameter("value",1.5).executeUpdate());
            session.rollbackTransaction();
            assertThrows(IllegalArgumentException.class,()->session.find(ManagedSessionTest.Record.class,"abc"));
            assertThrows(IllegalArgumentException.class,()->session.find(ManagedSessionTest.Record.class,1.5));
            assertEquals(Long.valueOf(0),session.createQuery("select count(NULL) from ManagedSessionTest$Record r",Long.class).first());
            assertThrows(IllegalArgumentException.class,()->session.query(ManagedSessionTest.Record.class).eq("counter","abc"));
            assertThrows(IllegalArgumentException.class,()->session.createQuery("update ManagedSessionTest$Record r set r.counter='oops'"));
            session.beginTransaction();
            assertThrows(IllegalArgumentException.class,()->session.createQuery("update ManagedSessionTest$Record r set r.counter=:value").setParameter("value","oops").executeUpdate());
            assertEquals(1,session.createQuery("update ManagedSessionTest$Record r set r.counter=:value").setParameter("value",0L).executeUpdate());session.commitTransaction();
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select coalesce(:value,r.counter) from ManagedSessionTest$Record r").setParameter("value","oops").list());
            assertEquals(Long.valueOf(7),session.createQuery("select nullif(:value,r.counter) from ManagedSessionTest$Record r",Long.class).setParameter("value",7L).first());
            assertEquals(Long.valueOf(1),session.createQuery("select length('é') from ManagedSessionTest$Record r",Long.class).first());
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select (select max(i.name) from ManagedSessionTest$Record i where i.id=r.id),count(r.id) from ManagedSessionTest$Record r"));
            assertEquals("record",((Object[])session.createQuery("select (select max(i.name) from ManagedSessionTest$Record i where i.id=r.id),count(r.id) from ManagedSessionTest$Record r group by r.id").first())[0]);
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select r from ManagedSessionTest$Record r where :a=:b"));
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select (select i.name from ManagedSessionTest$Record i) from ManagedSessionTest$Record r"));
            assertEquals(Double.valueOf(0),session.createQuery("select avg(r.counter) from ManagedSessionTest$Record r",Double.class).first());
            assertNull(session.createQuery("select avg(r.counter) from ManagedSessionTest$Record r where r.id<0",Double.class).first());
            String[] overflowExpressions={":value+1",":value-1",":value*2",":value/-1","-:value"};
            long[] overflowValues={Long.MAX_VALUE,Long.MIN_VALUE,Long.MAX_VALUE,Long.MIN_VALUE,Long.MIN_VALUE};
            for(int i=0;i<overflowExpressions.length;i++) {
                String expression=overflowExpressions[i];long value=overflowValues[i];
                assertThrows(PersistenceException.class,()->session.createQuery("select "+expression+" from ManagedSessionTest$Record r",Long.class).setParameter("value",value).list());
                session.beginTransaction();
                assertThrows(PersistenceException.class,()->session.createQuery("update ManagedSessionTest$Record r set r.counter="+expression).setParameter("value",value).executeUpdate());
                session.rollbackTransaction();
                assertEquals(0,session.find(ManagedSessionTest.Record.class,entity.id).counter);
            }
            assertEquals(Long.valueOf(Long.MAX_VALUE),session.createQuery("select :value+0 from ManagedSessionTest$Record r",Long.class).setParameter("value",Long.MAX_VALUE).first());
            assertEquals(Long.valueOf(Long.MIN_VALUE),session.createQuery("select :value-0 from ManagedSessionTest$Record r",Long.class).setParameter("value",Long.MIN_VALUE).first());
            assertEquals(Long.valueOf(9007199254740993L),session.createQuery("select sum(r.counter+9007199254740993) from ManagedSessionTest$Record r",Long.class).first());
            assertEquals("record",session.createQuery("select (select max(i.name) from ManagedSessionTest$Record i) from ManagedSessionTest$Record r",String.class).first());
            assertEquals(1,session.createQuery("select r from ManagedSessionTest$Record r where :flag and NOT false").setParameter("flag",true).list().size());
            assertEquals(1,session.createQuery("select r from ManagedSessionTest$Record r where r.counter < 0.5").list().size());
            assertEquals(Long.valueOf(1),session.createQuery("select (r.counter+3)/2 from ManagedSessionTest$Record r",Long.class).first());
            assertEquals(Long.valueOf(-1),session.createQuery("select (r.counter-3)/2 from ManagedSessionTest$Record r",Long.class).first());
            assertEquals(Double.valueOf(1.5),session.createQuery("select (r.counter+3)/2.0 from ManagedSessionTest$Record r",Double.class).first());
            assertNull(session.createQuery("select r.counter/0 from ManagedSessionTest$Record r",Long.class).first());
            assertEquals(Long.valueOf(-2),session.createQuery("select -:value from ManagedSessionTest$Record r",Long.class).setParameter("value",2L).first());
            assertEquals(Long.valueOf(2),session.createQuery("select +:value from ManagedSessionTest$Record r",Long.class).setParameter("value",2L).first());
            assertEquals(Double.valueOf(1.5),session.createQuery("select r.counter+1.5 from ManagedSessionTest$Record r",Double.class).first());
            assertEquals(Double.valueOf(2.5),session.createQuery("select :value+1.0 from ManagedSessionTest$Record r",Double.class).setParameter("value",1.5).first());
            assertEquals(1,session.createQuery("select r.name,count(r.id) from ManagedSessionTest$Record r group by r.name having count(r.id)>0").list().size());
            assertArrayEquals(new byte[]{1,2},session.find(ManagedSessionTest.Record.class,entity.id).bytes);
            session.beginTransaction();
            ManagedSessionTest.Record unnamed=new ManagedSessionTest.Record(),upper=new ManagedSessionTest.Record();upper.name="Z";session.persist(unnamed);session.persist(upper);session.commitTransaction();
            session.beginTransaction();session.createQuery("update ManagedSessionTest$Record r set r.counter=:value where r.id=:id").setParameter("value",1L).setParameter("id",unnamed.id).executeUpdate();session.commitTransaction();
            assertEquals(1.0/3,session.createQuery("select avg(r.counter) from ManagedSessionTest$Record r",Double.class).first(),0.0000001);
            session.beginTransaction();session.createQuery("update ManagedSessionTest$Record r set r.counter=0").executeUpdate();session.commitTransaction();
            assertEquals(unnamed.id,session.query(ManagedSessionTest.Record.class).orderBy("name",true).first().id);
            assertEquals(upper.id,session.query(ManagedSessionTest.Record.class).orderBy("name",true).offset(1).first().id);
            assertEquals(entity.id,session.query(ManagedSessionTest.Record.class).orderBy("name",false).first().id);
            assertEquals(entity.id,session.query(ManagedSessionTest.Record.class).gt("name","Z").first().id);
            assertEquals(upper.id,session.query(ManagedSessionTest.Record.class).le("name","Z").first().id);
            assertEquals(entity.id,session.createQuery("select r from ManagedSessionTest$Record r where r.name > :bound",ManagedSessionTest.Record.class).setParameter("bound","Z").first().id);
            assertEquals(2,session.createQuery("select r from ManagedSessionTest$Record r where r.name between 'Z' and 'z'",ManagedSessionTest.Record.class).list().size());
            assertTrue(session.query(ManagedSessionTest.Record.class).like("name","R%").list().isEmpty());
            assertEquals(entity.id,session.createQuery("select r from ManagedSessionTest$Record r where r.name like :pattern",ManagedSessionTest.Record.class).setParameter("pattern","r%").first().id);
            assertEquals(entity.id,session.createQuery("select r from ManagedSessionTest$Record r where r.name like :pattern escape :escape",ManagedSessionTest.Record.class).setParameter("pattern","r%").setParameter("escape","!").first().id);
            assertEquals(java.util.Arrays.asList(null,"Z","record"),session.createQuery("select distinct r.name from ManagedSessionTest$Record r order by r.name",String.class).list());
            assertEquals(upper.id,session.createQuery("select distinct r from ManagedSessionTest$Record r order by r.name",ManagedSessionTest.Record.class).offset(1).first().id);
            session.beginTransaction();session.lock(session.find(ManagedSessionTest.Record.class,entity.id),LockMode.PESSIMISTIC_WRITE);
            CountDownLatch attempting=new CountDownLatch(1);
            Future<Long> waiting=workers.submit(()-> {
                Database other=Database.open(url);other.execute(selectSchema,new Object[0]);EntityManager em=EntityManager.open(other);Session s=em.openSession();
                try {
                    s.beginTransaction();attempting.countDown();
                    ManagedSessionTest.Record locked=s.find(ManagedSessionTest.Record.class,entity.id,LockMode.PESSIMISTIC_WRITE);
                    locked.counter++;s.commitTransaction();return locked.counter;
                } finally { s.close();em.close(); }
            });
            assertTrue(attempting.await(5,TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,()->waiting.get(100,TimeUnit.MILLISECONDS));
            session.commitTransaction();assertEquals(1L,waiting.get(5,TimeUnit.SECONDS).longValue());
            Callable<Void> increment=()-> {
                Database other=Database.open(url);other.execute(selectSchema,new Object[0]);EntityManager em=EntityManager.open(other);Session s=em.openSession();
                try { for(int i=0;i<5;i++) { s.beginTransaction();assertTrue(s.increment(ManagedSessionTest.Record.class,entity.id,"counter",1));s.commitTransaction(); }return null; }
                finally { s.close();em.close(); }
            };
            Future<Void> first=workers.submit(increment),second=workers.submit(increment);first.get(10,TimeUnit.SECONDS);second.get(10,TimeUnit.SECONDS);
            session.clear();assertEquals(11,session.find(ManagedSessionTest.Record.class,entity.id).counter);
            assertEquals(11,session.find(ManagedSessionTest.Record.class,entity.id).version);
            session.beginTransaction();assertEquals(1,session.createQuery("update ManagedSessionTest$Record r set r.name=:name where r.id=:id").setParameter("name","updated").setParameter("id",entity.id).executeUpdate());session.commitTransaction();
            assertEquals("updated",session.find(ManagedSessionTest.Record.class,entity.id).name);
            session.beginTransaction();assertEquals(1,session.createQuery("delete from ManagedSessionTest$Record r where r.id=:id").setParameter("id",entity.id).executeUpdate());session.commitTransaction();
            assertNull(session.find(ManagedSessionTest.Record.class,entity.id));
            java.util.Map<String,com.codename1.impl.orm.EntityModel<?>> cycleModels=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();cycleModels.put(Cycle.class.getName(),new CycleModel());
            com.codename1.impl.orm.BackendSqlAccess cycleAccess=new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect());
            if(mysql) {
                assertThrows(com.codename1.orm.session.PersistenceException.class,()->new com.codename1.impl.orm.SessionImpl(cycleAccess,cycleModels));
            } else {
                Session cycles=new com.codename1.impl.orm.SessionImpl(cycleAccess,cycleModels);
                try {
                    cycles.createTables();Cycle a=new Cycle(),b=new Cycle();a.id=1;b.id=2;a.next=b;b.next=a;cycles.beginTransaction();cycles.persist(a);cycles.persist(b);cycles.commitTransaction();assertEquals(2,cycles.query(Cycle.class).count());
                    cycles.beginTransaction();cycles.remove(a);cycles.remove(b);cycles.commitTransaction();assertEquals(0,cycles.query(Cycle.class).count());
                } finally { cycles.close(); }
            }
        } finally {
            workers.shutdownNow();workers.awaitTermination(5,TimeUnit.SECONDS);
            try { session.close();db.execute((mysql?"DROP DATABASE ":"DROP SCHEMA ")+quoted+(mysql?"":" CASCADE"),new Object[0]); }
            finally { manager.close(); }
        }
    }
}
