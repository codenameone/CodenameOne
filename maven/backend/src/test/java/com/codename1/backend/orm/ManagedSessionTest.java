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
        return EntityManager.open(Database.open(":memory:"));
    }
    private Record seed(Session session) {
        session.createTables();session.validateSchema(); session.beginTransaction();
        Record r=new Record();r.name="first";r.bytes=new byte[]{1};session.persist(r);session.commitTransaction();return r;
    }
    @Test void schemaCreationIncludesLegacyDefinitionsAlongsideManagedEntities() throws Exception {
        EntityManager.register(new TestEntities.NoteDefinition());
        EntityManager em=manager();
        try {
            em.createTables();
            TestEntities.Note note=new TestEntities.Note();note.title="legacy";
            em.dao(TestEntities.Note.class).insert(note);
            assertEquals("legacy",em.dao(TestEntities.Note.class).findById(note.id).title);
            Session s=em.openSession();s.validateSchema();s.beginTransaction();
            Record record=new Record();s.persist(record);s.commitTransaction();assertTrue(record.id>0);s.close();
        } finally { em.close(); }
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

    @Test void bulkDeleteUsesPortableAliasSyntaxForEachDialect() throws Exception {
        for(com.codename1.backend.sql.Dialect dialect:new com.codename1.backend.sql.Dialect[]{com.codename1.backend.sql.Dialect.SQLITE,com.codename1.backend.sql.Dialect.POSTGRES,com.codename1.backend.sql.Dialect.MYSQL,com.codename1.backend.sql.Dialect.MARIADB}) {
            java.util.List<String> statements=new java.util.ArrayList<String>();
            com.codename1.impl.orm.SqlAccess access=(com.codename1.impl.orm.SqlAccess)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{com.codename1.impl.orm.SqlAccess.class},(proxy,method,args)-> {
                if(method.getName().equals("dialect")) return dialect.getName();
                if(method.getName().equals("quote")) return dialect.quote((String)args[0]);
                if(method.getName().equals("execute")) { statements.add((String)args[0]);assertArrayEquals(new Object[]{"delete me"},(Object[])args[1]);return 1; }
                if(method.getName().equals("begin") || method.getName().equals("commit") || method.getName().equals("close")) return null;
                throw new AssertionError(method.getName());
            });
            java.util.Map<String,EntityModel<?>> models=new java.util.LinkedHashMap<String,EntityModel<?>>();models.put(Record.class.getName(),new Model());
            Session s=new com.codename1.impl.orm.SessionImpl(access,models);
            try {
                s.beginTransaction();assertEquals(1,s.createQuery("delete from ManagedSessionTest$Record r where r.name = :name").setParameter("name","delete me").executeUpdate());s.commitTransaction();
                String prefix="mysql".equals(dialect.getName())?"DELETE q0 FROM ":"DELETE FROM ";
                String target=dialect.quote("managed_record");
                assertEquals("sqlite".equals(dialect.getName())?"DELETE FROM "+target+" WHERE ("+target+"."+dialect.quote("name")+" = ?)":prefix+target+" AS q0 WHERE (q0."+dialect.quote("name")+" = ?)",statements.get(0));
                s.beginTransaction();assertEquals(1,s.createQuery("update ManagedSessionTest$Record r set r.counter = r.counter where r.name = :name").setParameter("name","delete me").executeUpdate());s.commitTransaction();
                String alias="sqlite".equals(dialect.getName())?target:"q1";
                assertEquals("UPDATE "+target+("sqlite".equals(dialect.getName())?"":" AS q1")+" SET "+dialect.quote("counter")+" = "+alias+"."+dialect.quote("counter")+" WHERE ("+alias+"."+dialect.quote("name")+" = ?)",statements.get(1));
            } finally { s.close(); }
        }
    }

    @Test void averagesRenderFloatingResultsForEveryDialect() throws Exception {
        for(com.codename1.backend.sql.Dialect dialect:new com.codename1.backend.sql.Dialect[]{com.codename1.backend.sql.Dialect.SQLITE,com.codename1.backend.sql.Dialect.POSTGRES,com.codename1.backend.sql.Dialect.MYSQL,com.codename1.backend.sql.Dialect.MARIADB}) {
            com.codename1.impl.orm.BackendSqlAccess adapter=new com.codename1.impl.orm.BackendSqlAccess(null,null,dialect);
            com.codename1.impl.orm.SqlAccess access=(com.codename1.impl.orm.SqlAccess)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{com.codename1.impl.orm.SqlAccess.class},(proxy,method,args)-> {
                if(method.getName().equals("query")) {
                    String sql=(String)args[0];assertTrue(sql.contains("mysql".equals(dialect.getName())?"1e0 * AVG(":"CAST(AVG("),sql);
                    assertTrue(sql.contains("mysql".equals(dialect.getName())?"1e0":"postgresql".equals(dialect.getName())?" AS DOUBLE PRECISION)":" AS REAL)"),sql);
                    assertEquals(Attribute.REAL,((int[])args[2])[0]);return java.util.Collections.singletonList(new Object[]{1.5});
                }
                return method.invoke(adapter,args);
            });
            java.util.Map<String,EntityModel<?>> models=new java.util.LinkedHashMap<String,EntityModel<?>>();models.put(Record.class.getName(),new Model());Session session=new com.codename1.impl.orm.SessionImpl(access,models);
            assertEquals(Double.valueOf(1.5),session.createQuery("select avg(r.counter) from ManagedSessionTest$Record r",Double.class).first());session.close();
        }
    }

    static class Other extends Record { }
    @Test void mysqlBulkSubqueriesCannotReadTheirMutationTarget() throws Exception {
        for(com.codename1.backend.sql.Dialect dialect:new com.codename1.backend.sql.Dialect[]{com.codename1.backend.sql.Dialect.SQLITE,com.codename1.backend.sql.Dialect.POSTGRES,com.codename1.backend.sql.Dialect.MYSQL,com.codename1.backend.sql.Dialect.MARIADB}) {
            java.util.Map<String,EntityModel<?>> models=new java.util.LinkedHashMap<String,EntityModel<?>>();models.put(Record.class.getName(),new Model());models.put(Other.class.getName(),new Model() { public Class<Record> type() { return (Class)Other.class; } public String table() { return "other_record"; } });
            Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,null,dialect),models);
            try {
                for(String query:new String[]{"update ManagedSessionTest$Record r set r.counter=1 where r.id in (select i.id from ManagedSessionTest$Record i)","delete from ManagedSessionTest$Record r where exists (select i.id from ManagedSessionTest$Record i)","update ManagedSessionTest$Record r set r.counter=(select max(i.counter) from ManagedSessionTest$Record i)","delete from ManagedSessionTest$Record r where r.id in (select o.id from ManagedSessionTest$Other o where exists (select i.id from ManagedSessionTest$Record i))"}) {
                    if("mysql".equals(dialect.getName())) assertThrows(IllegalArgumentException.class,()->session.createQuery(query),query);else assertNotNull(session.createQuery(query));
                }
                assertNotNull(session.createQuery("update ManagedSessionTest$Record r set r.counter=(select max(o.counter) from ManagedSessionTest$Other o where o.id=r.id)"));
                assertNotNull(session.createQuery("delete from ManagedSessionTest$Record r where r.id in (select o.id from ManagedSessionTest$Other o)"));
                assertNotNull(session.createQuery("select r from ManagedSessionTest$Record r where r.id in (select i.id from ManagedSessionTest$Record i)"));
            } finally { session.close(); }
        }
    }

    @Test void integerAssignmentsRenderCheckedRangesForEveryDialect() throws Exception {
        for(com.codename1.backend.sql.Dialect dialect:new com.codename1.backend.sql.Dialect[]{com.codename1.backend.sql.Dialect.SQLITE,com.codename1.backend.sql.Dialect.POSTGRES,com.codename1.backend.sql.Dialect.MYSQL,com.codename1.backend.sql.Dialect.MARIADB}) {
            com.codename1.impl.orm.BackendSqlAccess adapter=new com.codename1.impl.orm.BackendSqlAccess(null,null,dialect);
            com.codename1.impl.orm.SqlAccess access=(com.codename1.impl.orm.SqlAccess)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{com.codename1.impl.orm.SqlAccess.class},(proxy,method,args)-> {
                if(method.getName().equals("execute")) {
                    String sql=(String)args[0];assertTrue(sql.contains("CASE WHEN"),sql);assertTrue(sql.contains("< -2147483648") && sql.contains("> 2147483647"),sql);
                    if("sqlite".equals(dialect.getName())) assertTrue(sql.contains("abs(-9223372036854775808)"),sql);
                    else { assertTrue(sql.contains("+ CASE WHEN"),sql);assertTrue(sql.contains("mysql".equals(dialect.getName())?"AS SIGNED":"AS BIGINT"),sql); }
                    for(Object value:(Object[])args[1]) assertEquals(Long.valueOf(2147483648L),value);return 1;
                }
                if(method.getName().equals("begin") || method.getName().equals("commit") || method.getName().equals("close")) return null;
                return method.invoke(adapter,args);
            });
            java.util.Map<String,EntityModel<?>> models=new java.util.LinkedHashMap<String,EntityModel<?>>();models.put(Record.class.getName(),integerModel());Session session=new com.codename1.impl.orm.SessionImpl(access,models);
            try { session.beginTransaction();assertEquals(1,session.createQuery("update ManagedSessionTest$Record r set r.counter=:value").setParameter("value",2147483648L).executeUpdate());session.commitTransaction(); } finally { session.close(); }
        }
    }

    @Test void lengthUsesCharacterSemanticsForEachDialect() throws Exception {
        for(com.codename1.backend.sql.Dialect dialect:new com.codename1.backend.sql.Dialect[]{com.codename1.backend.sql.Dialect.SQLITE,com.codename1.backend.sql.Dialect.POSTGRES,com.codename1.backend.sql.Dialect.MYSQL,com.codename1.backend.sql.Dialect.MARIADB}) {
            com.codename1.impl.orm.BackendSqlAccess adapter=new com.codename1.impl.orm.BackendSqlAccess(null,null,dialect);
            com.codename1.impl.orm.SqlAccess access=(com.codename1.impl.orm.SqlAccess)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{com.codename1.impl.orm.SqlAccess.class},(proxy,method,args)-> {
                if(method.getName().equals("query")) {
                    assertTrue(((String)args[0]).contains("mysql".equals(dialect.getName())?"CHAR_LENGTH(":"LENGTH("));return java.util.Collections.singletonList(new Object[]{1L});
                }
                return method.invoke(adapter,args);
            });
            java.util.Map<String,EntityModel<?>> models=new java.util.LinkedHashMap<String,EntityModel<?>>();models.put(Record.class.getName(),new Model());
            Session session=new com.codename1.impl.orm.SessionImpl(access,models);
            assertEquals(Long.valueOf(1),session.createQuery("select length(r.name) from ManagedSessionTest$Record r",Long.class).first());session.close();
        }
    }

    @Test void managedOrderingUsesDialectForTextNumericAndExpressionTerms() throws Exception {
        for(com.codename1.backend.sql.Dialect dialect:new com.codename1.backend.sql.Dialect[]{com.codename1.backend.sql.Dialect.SQLITE,com.codename1.backend.sql.Dialect.POSTGRES,com.codename1.backend.sql.Dialect.MYSQL,com.codename1.backend.sql.Dialect.MARIADB}) {
            java.util.List<String> statements=new java.util.ArrayList<String>();
            com.codename1.impl.orm.BackendSqlAccess adapter=new com.codename1.impl.orm.BackendSqlAccess(null,null,dialect);
            com.codename1.impl.orm.SqlAccess access=(com.codename1.impl.orm.SqlAccess)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{com.codename1.impl.orm.SqlAccess.class},(proxy,method,args)-> {
                if(method.getName().equals("query")) {
                    String statement=(String)args[0];statements.add(statement);
                    long markers=statement.chars().filter(c->c=='?').count();assertEquals(markers,((Object[])args[1]).length,statement);
                    for(Object value:(Object[])args[1]) { if(statement.contains(" NULLIF(")) assertTrue(value instanceof Number);else assertEquals("Z",value); }
                    return java.util.Collections.emptyList();
                }
                return method.invoke(adapter,args);
            });
            java.util.Map<String,EntityModel<?>> models=new java.util.LinkedHashMap<String,EntityModel<?>>();models.put(Record.class.getName(),new Model() {
                public com.codename1.impl.orm.Relationship[] relationships() {
                    return new com.codename1.impl.orm.Relationship[]{new com.codename1.impl.orm.Relationship("peers",Record.class,true,true,-1,"","record_peers","owner_id","target_id",0,false)};
                }
            });
            Session s=new com.codename1.impl.orm.SessionImpl(access,models);
            try {
                for(boolean ascending:new boolean[]{true,false}) {
                    s.query(Record.class).orderBy("name",ascending).orderBy("counter",!ascending).limit(2).offset(1).list();
                    String builder=statements.get(statements.size()-1);String alias=ascending?"q0":"q2";
                    String expected=dialect.orderBy(alias+"."+dialect.quote("name"),ascending,true)+", "+dialect.orderBy(alias+"."+dialect.quote("counter"),!ascending,false);
                    assertTrue(builder.contains(" ORDER BY "+expected),builder);
                    s.createQuery("select r from ManagedSessionTest$Record r order by lower(r.name) "+(ascending?"asc":"desc")+", r.counter "+(ascending?"desc":"asc"),Record.class).limit(2).offset(1).list();
                    String jpql=statements.get(statements.size()-1);alias=ascending?"q1":"q3";
                    expected=dialect.orderBy("LOWER("+alias+"."+dialect.quote("name")+")",ascending,true)+", "+dialect.orderBy(alias+"."+dialect.quote("counter"),!ascending,false);
                    assertTrue(jpql.contains(" ORDER BY "+expected),jpql);
                }
                s.query(Record.class).join("peers").orderBy("name",true).list();
                String distinct=statements.get(statements.size()-1),column="q4."+dialect.quote("name");
                assertTrue(distinct.startsWith("SELECT DISTINCT "),distinct);assertTrue(distinct.contains(dialect.comparison(column,true)+", "),distinct);assertTrue(distinct.contains(" ORDER BY "+dialect.orderBy(column,true,true)),distinct);
                s.createQuery("select distinct r.name from ManagedSessionTest$Record r order by r.name",String.class).list();
                distinct=statements.get(statements.size()-1);column="q5."+dialect.quote("name");assertTrue(distinct.startsWith("SELECT DISTINCT "+dialect.comparison(column,true)+" AS cn1_scalar_0"),distinct);assertTrue(distinct.contains(" ORDER BY "+dialect.orderBy(column,true,true)),distinct);
                s.createQuery("select r from ManagedSessionTest$Record r order by coalesce(r.name, :fallback)",Record.class).setParameter("fallback","Z").list();
                s.query(Record.class).gt("name","Z").list();String range=statements.get(statements.size()-1);assertTrue(range.contains(dialect.comparison("q7."+dialect.quote("name"),true)+" > ?"),range);
                s.createQuery("select r from ManagedSessionTest$Record r where r.name > :bound",Record.class).setParameter("bound","Z").list();range=statements.get(statements.size()-1);assertTrue(range.contains(dialect.comparison("q8."+dialect.quote("name"),true)+" > ?"),range);
                s.createQuery("select r from ManagedSessionTest$Record r where r.name between :low and :high",Record.class).setParameter("low","Z").setParameter("high","Z").list();range=statements.get(statements.size()-1);assertTrue(range.contains(dialect.comparison("q9."+dialect.quote("name"),true)+" BETWEEN"),range);
                s.createQuery("select r.counter / 2 from ManagedSessionTest$Record r").list();String division=statements.get(statements.size()-1);
                assertTrue(division.contains("mysql".equals(dialect.getName())?" DIV NULLIF(":" / NULLIF("),division);
                s.createQuery("select r.counter / 2.0 from ManagedSessionTest$Record r").list();division=statements.get(statements.size()-1);
                assertTrue(division.contains("mysql".equals(dialect.getName())?"1e0 *":"CAST("),division);
                s.createQuery("select sum(r.counter) from ManagedSessionTest$Record r",Long.class).list();String sum=statements.get(statements.size()-1);
                assertTrue(sum.contains("CAST(SUM("),sum);assertTrue(sum.contains("mysql".equals(dialect.getName())?" AS SIGNED)":" AS BIGINT)"),sum);
            } finally { s.close(); }
        }
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

    @Test void schemaColumnNamesFollowDialectCaseRules() throws Exception {
        for(com.codename1.backend.sql.Dialect dialect:new com.codename1.backend.sql.Dialect[]{com.codename1.backend.sql.Dialect.SQLITE,com.codename1.backend.sql.Dialect.POSTGRES,com.codename1.backend.sql.Dialect.MYSQL}) {
            for(boolean collection:new boolean[]{false,true}) {
                Model model=new Model() {
                    public com.codename1.impl.orm.Relationship[] relationships() { return new com.codename1.impl.orm.Relationship[]{new com.codename1.impl.orm.Relationship("tags",String.class,true,true,-1,"","record_tags","owner_id","value",0,false,false,"","position","",true)}; }
                };
                com.codename1.impl.orm.BackendSqlAccess adapter=new com.codename1.impl.orm.BackendSqlAccess(null,null,dialect);
                com.codename1.impl.orm.SqlAccess access=(com.codename1.impl.orm.SqlAccess)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{com.codename1.impl.orm.SqlAccess.class},(proxy,method,args)-> {
                    if(method.getName().equals("describe")) {
                        java.util.List<Object[]> rows=new java.util.ArrayList<Object[]>();
                        if(args[0].equals("record_tags")) { for(String column:new String[]{collection?"Owner_id":"owner_id","value","position"}) rows.add(new Object[]{column,column.equals("value")?"TEXT":"BIGINT",column.equals("value")?0:1,column.equals("value")?0:1}); }
                        else { for(Attribute a:model.attributes()) rows.add(new Object[]{!collection && a.id ? "ID" : a.column,dialect.columnType(a.kind),a.nullable?0:1,a.id?1:0}); }
                        return rows;
                    }
                    if(method.getName().equals("close")) return null;
                    return method.invoke(adapter,args);
                });
                java.util.Map<String,EntityModel<?>> models=new java.util.LinkedHashMap<String,EntityModel<?>>();models.put(Record.class.getName(),model);
                Session session=new com.codename1.impl.orm.SessionImpl(access,models);
                try {
                    if(dialect==com.codename1.backend.sql.Dialect.POSTGRES) {
                        PersistenceException error=assertThrows(PersistenceException.class,session::validateSchema);
                        assertTrue(error.getMessage().contains(collection?"Missing collection column record_tags.owner_id":"Missing column managed_record.id"));
                    } else session.validateSchema();
                } finally { session.close(); }
            }
        }
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

    @Test void bulkClausesRejectUnscopedAggregates() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();Record record=seed(session);
            for(String query:new String[]{"update ManagedSessionTest$Record r set r.counter=sum(r.counter)","update ManagedSessionTest$Record r set r.counter=coalesce(max(r.counter),0)","update ManagedSessionTest$Record r set r.counter=1 where count(r.id)>0","delete from ManagedSessionTest$Record r where count(r.id)>0","delete from ManagedSessionTest$Record r where coalesce(sum(r.counter),0)>=0"}) {
                assertThrows(IllegalArgumentException.class,()->session.createQuery(query),query);
            }
            session.beginTransaction();assertEquals(1,session.createQuery("update ManagedSessionTest$Record r set r.counter=(select max(i.counter)+1 from ManagedSessionTest$Record i)").executeUpdate());session.commitTransaction();assertEquals(1,session.find(Record.class,record.id).counter);
            session.beginTransaction();assertEquals(1,session.createQuery("delete from ManagedSessionTest$Record r where r.id in (select i.id from ManagedSessionTest$Record i where i.counter=1)").executeUpdate());session.commitTransaction();assertEquals(0,session.query(Record.class).count());session.close();
        } finally { em.close(); }
    }

    static Model integerModel() {
        return new Model() {
            public String table() { return "integer_record"; }
            public Attribute[] attributes() { Attribute[] attrs=super.attributes().clone();attrs[0]=new Attribute("id","id",Attribute.INTEGER,true,true,false,false);attrs[2]=new Attribute("counter","counter",Attribute.INTEGER,false,false,false,false);return attrs; }
        };
    }
    static void assertIntegerRanges(Session session) {
        session.createTables();session.beginTransaction();Record record=new Record();record.name="integer";session.persist(record);session.commitTransaction();long id=record.id;
        for(boolean clear:new boolean[]{false,true}) {
            if(clear) session.clear();
            for(long bad:new long[]{2147483648L,-2147483649L,Long.MAX_VALUE,Long.MIN_VALUE}) assertThrows(IllegalArgumentException.class,()->session.find(Record.class,bad));
            assertEquals(id,session.find(Record.class,Long.valueOf(id)).id);
        }
        for(String predicate:new String[]{"r.counter=:value",":value=r.counter","r.counter>:value","r.counter in (:value)","r.counter in (:value,0)","r.counter between :value and :value"}) {
            boolean collection=predicate.equals("r.counter in (:value)");
            com.codename1.orm.session.JpqlQuery query=session.createQuery("select r from ManagedSessionTest$Record r where "+predicate);
            for(long invalid:new long[]{2147483648L,-2147483649L,Long.MIN_VALUE,Long.MAX_VALUE}) {
                session.beginTransaction();query.setParameter("value",collection?new Object[]{invalid}:invalid);assertThrows(IllegalArgumentException.class,query::list,predicate);assertFalse(session.isRollbackOnly());query.setParameter("value",collection?new Object[]{0L}:0L).list();session.commitTransaction();
            }
            for(long boundary:new long[]{Integer.MIN_VALUE,Integer.MAX_VALUE}) query.setParameter("value",collection?new Object[]{boundary}:boundary).list();
        }
        for(int operation=0;operation<4;operation++) {
            com.codename1.orm.session.Query<Record> query=session.query(Record.class);final int action=operation;
            for(long invalid:new long[]{2147483648L,-2147483649L}) assertThrows(IllegalArgumentException.class,()-> { switch(action) { case 0:query.eq("counter",invalid);break;case 1:query.gt("counter",invalid);break;case 2:query.lt("counter",invalid);break;default:query.in("counter",new Object[]{0L,invalid}); }});
            assertEquals(1,query.count());assertEquals(1,query.eq("counter",0L).count());
        }
        // Arithmetic widens the expression and must still accept a long operand.
        assertEquals(1,session.createQuery("select r from ManagedSessionTest$Record r where r.counter+:delta=:expected").setParameter("delta",2147483648L).setParameter("expected",2147483648L).list().size());
        for(String expression:new String[]{"2147483648","-2147483649","r.counter+2147483648","r.counter-2147483649",":value","coalesce(:value,r.counter)","(select max(i.counter)+2147483648 from ManagedSessionTest$Record i)"}) {
            session.beginTransaction();com.codename1.orm.session.JpqlQuery query=session.createQuery("update ManagedSessionTest$Record r set r.counter="+expression);if(expression.contains(":value")) query.setParameter("value",2147483648L);
            assertThrows(PersistenceException.class,query::executeUpdate,expression);session.rollbackTransaction();assertEquals(0L,session.find(Record.class,id).counter);
        }
        for(long boundary:new long[]{Integer.MIN_VALUE,Integer.MAX_VALUE,0}) {
            session.beginTransaction();assertEquals(1,session.createQuery("update ManagedSessionTest$Record r set r.counter=:value").setParameter("value",boundary).executeUpdate());session.commitTransaction();assertEquals(boundary,session.find(Record.class,id).counter);
        }
        for(long[] change:new long[][]{{Integer.MIN_VALUE,2147483648L,0},{Integer.MIN_VALUE,4294967295L,Integer.MAX_VALUE},{Integer.MAX_VALUE,-4294967295L,Integer.MIN_VALUE},{Integer.MAX_VALUE,-2147483649L,-2}}) {
            session.beginTransaction();session.createQuery("update ManagedSessionTest$Record r set r.counter=:value").setParameter("value",change[0]).executeUpdate();session.commitTransaction();
            Record current=session.find(Record.class,id);long version=current.version;
            session.beginTransaction();assertTrue(session.increment(Record.class,id,"counter",change[1]));assertEquals(change[2],current.counter);assertEquals(version+1,current.version);session.commitTransaction();
            session.clear();assertEquals(change[2],session.find(Record.class,id).counter);
        }
        for(long amount:new long[]{Long.MIN_VALUE,Long.MAX_VALUE,-4294967296L,4294967296L,-2147483648L,2147483650L}) {
            Record current=session.find(Record.class,id);long value=current.counter,version=current.version;
            session.beginTransaction();assertFalse(session.increment(Record.class,id,"counter",amount));assertFalse(session.isRollbackOnly());session.commitTransaction();
            assertEquals(value,current.counter);assertEquals(version,current.version);
        }
    }
    static class RealRecord { long id;float narrow;Float boxed;double wide; }
    static class RealModel extends EntityModel<RealRecord> {
        public Class<RealRecord> type() { return RealRecord.class; }
        public String table() { return "managed_real_record"; }
        public RealRecord create() { return new RealRecord(); }
        public Attribute[] attributes() { return new Attribute[]{new Attribute("id","id",Attribute.BIGINT,true,true,false,false),new Attribute("narrow","narrow",Attribute.REAL,false,false,false,false),new Attribute("boxed","boxed",Attribute.REAL,false,false,true,false),new Attribute("wide","wide",Attribute.REAL,false,false,false,false)}; }
        public boolean singlePrecision(int index) { return index==1 || index==2; }
        public boolean primitive(int index) { return index==1 || index==3; }
        public Object get(RealRecord r,int i) { switch(i) { case 0:return r.id;case 1:return (double)r.narrow;case 2:return r.boxed==null?null:Double.valueOf(r.boxed.doubleValue());default:return r.wide; } }
        public void set(RealRecord r,int i,Object value) { try { switch(i) { case 0:r.id=((Number)value).longValue();break;case 1:r.narrow=com.codename1.impl.orm.Values.asFloat(value,0);break;case 2:r.boxed=com.codename1.impl.orm.Values.asFloatObject(value);break;default:r.wide=com.codename1.impl.orm.Values.asDouble(value,0); }} catch(java.io.IOException error) { throw new PersistenceException("Invalid real value",error); } }
    }
    static void assertRealAssignments(Session session) {
        session.createTables();session.beginTransaction();RealRecord record=new RealRecord();record.narrow=1;record.boxed=1f;record.wide=1e100;session.persist(record);session.commitTransaction();long id=record.id;
        for(String bad:new String[]{"1e-400","-1e-400","0.00001e-400","1e400"}) {
            assertThrows(IllegalArgumentException.class,()->session.createQuery("select r from ManagedSessionTest$RealRecord r where r.wide="+bad));
            assertThrows(IllegalArgumentException.class,()->session.createQuery("update ManagedSessionTest$RealRecord r set r.wide="+bad));
        }
        for(String field:new String[]{"narrow","boxed"}) {
            for(String expression:new String[]{"1e100","-1e100","1e-100","-1e-100",":value","r.wide","r.wide*2","coalesce((select max(i.wide) from ManagedSessionTest$RealRecord i),0)"}) {
                session.beginTransaction();com.codename1.orm.session.JpqlQuery query=session.createQuery("update ManagedSessionTest$RealRecord r set r."+field+"="+expression);if(expression.equals(":value"))query.setParameter("value",1e100);
                assertThrows(PersistenceException.class,query::executeUpdate,field+"="+expression);session.rollbackTransaction();assertEquals(1f,session.find(RealRecord.class,id).narrow);assertEquals(Float.valueOf(1f),session.find(RealRecord.class,id).boxed);
            }
            for(double boundary:new double[]{0,-0.0,Float.MIN_VALUE,-Float.MIN_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,1e-45,-1e-45}) {
                session.beginTransaction();assertEquals(1,session.createQuery("update ManagedSessionTest$RealRecord r set r."+field+"=:value").setParameter("value",boundary).executeUpdate());session.commitTransaction();session.clear();RealRecord loaded=session.find(RealRecord.class,id);assertEquals((float)boundary,field.equals("narrow")?loaded.narrow:loaded.boxed.floatValue(),0.0f);
            }
            session.beginTransaction();session.createQuery("update ManagedSessionTest$RealRecord r set r."+field+"=1").executeUpdate();session.commitTransaction();
        }
        session.beginTransaction();session.createQuery("update ManagedSessionTest$RealRecord r set r.boxed=NULL,r.wide=1e-100").executeUpdate();session.commitTransaction();assertNull(session.find(RealRecord.class,id).boxed);assertEquals(1e-100,session.find(RealRecord.class,id).wide);
        session.beginTransaction();assertThrows(PersistenceException.class,()->session.createQuery("update ManagedSessionTest$RealRecord r set r.narrow=r.wide/2").executeUpdate());session.rollbackTransaction();
        for(String zero:new String[]{"0e-400","-0e-400","0.000e-400"}) assertEquals(0.0,session.createQuery("select "+zero+" from ManagedSessionTest$RealRecord r",Double.class).first().doubleValue(),0.0);
    }
    @Test void realAssignmentsPreserveFloatRangeAndRejectLiteralUnderflow() throws Exception {
        EntityManager em=manager();Models.register(new RealModel());try { Session session=em.openSession();try { assertRealAssignments(session); } finally { session.close(); } } finally { em.close(); }
    }

    static void assertMinimumLongLiteral(Session session) {
        session.createTables();session.beginTransaction();Record record=new Record();record.name="minimum";session.persist(record);session.commitTransaction();
        session.beginTransaction();assertEquals(1,session.createQuery("update ManagedSessionTest$Record r set r.counter=-9223372036854775808").executeUpdate());session.commitTransaction();
        assertEquals(Long.MIN_VALUE,session.find(Record.class,record.id).counter);
        assertEquals(1,session.createQuery("select r from ManagedSessionTest$Record r where r.counter=(-9223372036854775808)").list().size());
        assertEquals(Long.valueOf(Long.MIN_VALUE),session.createQuery("select -9223372036854775808 from ManagedSessionTest$Record r",Long.class).first());
        for(String invalid:new String[]{"9223372036854775808","+9223372036854775808","-9223372036854775809"}) assertThrows(IllegalArgumentException.class,()->session.createQuery("select r from ManagedSessionTest$Record r where r.counter="+invalid));
        session.beginTransaction();session.remove(session.find(Record.class,record.id));session.commitTransaction();
    }
    @Test void minimumLongLiteralSupportsPredicatesAndAssignments() throws Exception {
        EntityManager em=manager();try { Session session=em.openSession();try { assertMinimumLongLiteral(session); } finally { session.close(); } } finally { em.close(); }
    }

    @Test void integerKeysAndBulkAssignmentsRespectTheirRange() throws Exception {
        EntityManager em=manager();Models.register(integerModel());
        try { Session session=em.openSession();try { assertIntegerRanges(session); } finally { session.close(); } } finally { em.close(); }
    }

    @Test void identifierStorageKindsAreValidatedBeforeCacheOrSql() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();Record record=seed(session);
            for(boolean clear:new boolean[]{false,true}) {
                if(clear) session.clear();
                for(Object bad:new Object[]{"abc",String.valueOf(record.id),Double.valueOf(record.id),new byte[]{1}}) {
                    assertThrows(IllegalArgumentException.class,()->session.find(Record.class,bad));
                }
                assertEquals(record.id,session.find(Record.class,Integer.valueOf((int)record.id)).id);
            }
            Model composite=new Model() { public Attribute[] attributes() { return new Attribute[]{new Attribute("name","name",Attribute.TEXT,true,false,false,false),new Attribute("counter","counter",Attribute.BIGINT,true,false,false,false)}; } };
            assertArrayEquals(new Object[]{"key",2L},composite.keyValues(new Object[]{"key",Integer.valueOf(2)}));
            for(Object[] bad:new Object[][]{{1L,2L},{"key","abc"},{"key",2.5},{"key",null}}) {
                assertThrows(IllegalArgumentException.class,()->composite.keyValues(bad));
                assertThrows(IllegalArgumentException.class,()->composite.keyValues(com.codename1.orm.session.Identifier.of(bad)));
            }
            session.close();
        } finally { em.close(); }
    }

    @Test void bulkIntegralAssignmentsRejectRealValuesBeforeWriting() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();Record record=seed(session);session.beginTransaction();
            for(String expression:new String[]{"1.5","1.0","1+0.5","coalesce(:value,1.5)","(select avg(i.counter) from ManagedSessionTest$Record i)"}) {
                assertThrows(IllegalArgumentException.class,()->session.createQuery("update ManagedSessionTest$Record r set r.counter="+expression),expression);
            }
            for(String expression:new String[]{":value","(:value)","coalesce(:value,r.counter)","nullif(:value,r.counter)","r.counter+:value"}) {
                for(Number value:new Number[]{Double.valueOf(1.5),Float.valueOf(1.5f),Double.valueOf(1)}) {
                    assertThrows(IllegalArgumentException.class,()->session.createQuery("update ManagedSessionTest$Record r set r.counter="+expression).setParameter("value",value).executeUpdate(),expression);
                }
            }
            assertEquals(0L,session.find(Record.class,record.id).counter);
            assertEquals(1,session.createQuery("update ManagedSessionTest$Record r set r.counter=:value").setParameter("value",7L).executeUpdate());
            assertEquals(1,session.createQuery("update ManagedSessionTest$Record r set r.counter=r.counter+2").executeUpdate());
            session.commitTransaction();assertEquals(9L,session.find(Record.class,record.id).counter);session.close();
        } finally { em.close(); }
    }

    @Test void comparisonsNeedAKnownOperandKind() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();seed(session);
            for(String predicate:new String[]{":left=:right","(:left)<>:right",":left>:right",":left in (:right)",":left between :low and :high","coalesce(:a,:b)=nullif(:c,:d)",":value is null",":value is not null","(:value) is null","coalesce(:a,:b) is null"}) {
                assertThrows(IllegalArgumentException.class,()->session.createQuery("select r from ManagedSessionTest$Record r where "+predicate),predicate);
            }
            assertEquals(1,session.createQuery("select r from ManagedSessionTest$Record r where :value=1").setParameter("value",1L).list().size());
            assertEquals(1,session.createQuery("select r from ManagedSessionTest$Record r where coalesce(:value,r.name) is not null").setParameter("value",null).list().size());session.close();
        } finally { em.close(); }
    }

    @Test void scalarSubqueriesRequireASingleRowPlan() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();seed(session);session.beginTransaction();Record second=new Record();second.name="second";session.persist(second);session.commitTransaction();
            for(String nested:new String[]{"select i.name from ManagedSessionTest$Record i","select max(i.name) from ManagedSessionTest$Record i group by i.id","select distinct i.name from ManagedSessionTest$Record i","select max(r.counter) from ManagedSessionTest$Record i","select count(r) from ManagedSessionTest$Record i"}) {
                assertThrows(IllegalArgumentException.class,()->session.createQuery("select ("+nested+") from ManagedSessionTest$Record r"));
            }
            assertEquals("second",session.createQuery("select (select max(i.name) from ManagedSessionTest$Record i) from ManagedSessionTest$Record r",String.class).first());
            assertNull(session.createQuery("select (select max(i.name) from ManagedSessionTest$Record i where i.id<0) from ManagedSessionTest$Record r",String.class).first());
            assertEquals(2,session.createQuery("select r from ManagedSessionTest$Record r where r.id in (select i.id from ManagedSessionTest$Record i)").list().size());
            assertEquals(2,session.createQuery("select r from ManagedSessionTest$Record r where exists (select i.id from ManagedSessionTest$Record i)").list().size());session.close();
        } finally { em.close(); }
    }

    @Test void integralAverageReturnsDoubleIncludingFractionalResults() throws Exception {
        EntityManager em=manager();
        try {
            Session session=em.openSession();Record first=seed(session);session.beginTransaction();first.counter=1;Record second=new Record();second.counter=2;session.persist(second);session.commitTransaction();
            assertEquals(Double.valueOf(1.5),session.createQuery("select avg(r.counter) from ManagedSessionTest$Record r",Double.class).first());
            assertEquals(Double.valueOf(1.5),session.createQuery("select avg(distinct r.counter) from ManagedSessionTest$Record r",Double.class).first());
            assertNull(session.createQuery("select avg(r.counter) from ManagedSessionTest$Record r where r.id<0",Double.class).first());session.close();
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
            for(String expression:new String[]{"1","abs(1)","coalesce(1,2)","nullif(1,2)","min(1)","max(1)","(select max(1) from ManagedSessionTest$Record i)"}) {
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
            Object[] grouped=(Object[])session.createQuery("select r.counter+:n,count(r.id) from ManagedSessionTest$Record r group by r.counter+:n").setParameter("n",2L).first();assertEquals(Long.valueOf(2),grouped[0]);assertEquals(Long.valueOf(1),grouped[1]);
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

    @Test void nontransactionalReadsWaitForSuppliedConnectionTransaction() throws Exception {
        EntityManager em=manager();java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
        Session session=em.openSession();
        try {
            Record record=seed(session);
            for(boolean commit:new boolean[]{true,false}) {
                session.beginTransaction();record=session.find(Record.class,record.id);record.counter=commit?7:9;session.flush();
                java.util.concurrent.CountDownLatch started=new java.util.concurrent.CountDownLatch(1);
                java.util.concurrent.Future<Long> read=worker.submit(()-> {
                    Session other=em.openSession();
                    try { started.countDown();return other.createQuery("select r.counter from ManagedSessionTest$Record r",Long.class).first(); }
                    finally { other.close(); }
                });
                assertTrue(started.await(5,java.util.concurrent.TimeUnit.SECONDS));
                assertThrows(java.util.concurrent.TimeoutException.class,()->read.get(150,java.util.concurrent.TimeUnit.MILLISECONDS));
                if(commit) session.commitTransaction();else session.rollbackTransaction();
                assertEquals(Long.valueOf(7),read.get(5,java.util.concurrent.TimeUnit.SECONDS));
            }
            em.database().beginTransaction();
            try { assertThrows(PersistenceException.class,()->session.query(Record.class).count()); }
            finally { em.database().rollbackTransaction(); }
        } finally { session.close();worker.shutdownNow();em.close(); }
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
    @Test void closeDiscardsPooledConnectionWhenRollbackFails() throws Exception {
        com.codename1.backend.DataSource pool=com.codename1.backend.DataSource.open(":memory:",1);
        Database damaged=pool.borrow();pool.release(damaged);
        com.codename1.impl.orm.BackendSqlAccess access=new com.codename1.impl.orm.BackendSqlAccess(pool,null,damaged.dialect());
        try {
            access.begin();
            // End the engine transaction without clearing the facade's owner.
            // The adapter's subsequent ROLLBACK then fails on a still-open connection.
            damaged.execute("ROLLBACK",new Object[0]);
            assertThrows(java.io.IOException.class,access::close);
            assertFalse(damaged.isOpen());assertFalse(damaged.isInTransaction());
            Database replacement=pool.borrow();
            try { assertNotSame(damaged,replacement);assertEquals(1,replacement.query("SELECT 1",new Object[0]).size()); }
            finally { pool.release(replacement); }
            access.close();
        } finally { damaged.close();pool.close(); }
    }

    @Test void suppliedDatabaseSerializesOtherHandlersAcrossSessionTransaction() throws Exception {
        EntityManager em=manager();
        java.util.concurrent.ExecutorService workers=java.util.concurrent.Executors.newFixedThreadPool(2);
        Session s=em.openSession();
        Database db=em.database();
        try {
            db.execute("CREATE TABLE audit (value TEXT)",new Object[0]);
            for(int boundary=0;boundary<3;boundary++) {
                s.beginTransaction();
                db.execute("INSERT INTO audit VALUES ('session')",new Object[0]);
                java.util.concurrent.CountDownLatch attempting=new java.util.concurrent.CountDownLatch(2);
                java.util.concurrent.Future<?> raw=workers.submit(()-> {
                    attempting.countDown();db.execute("INSERT INTO audit VALUES ('raw')",new Object[0]);return null;
                });
                java.util.concurrent.Future<?> callback=workers.submit(()-> {
                    attempting.countDown();return db.transaction(connection -> {
                        connection.execute("INSERT INTO audit VALUES ('callback')",new Object[0]);return null;
                    });
                });
                assertTrue(attempting.await(5,java.util.concurrent.TimeUnit.SECONDS));
                assertThrows(java.util.concurrent.TimeoutException.class,()->raw.get(100,java.util.concurrent.TimeUnit.MILLISECONDS));
                assertThrows(java.util.concurrent.TimeoutException.class,()->callback.get(100,java.util.concurrent.TimeUnit.MILLISECONDS));
                if(boundary==0) s.commitTransaction();
                else if(boundary==1) s.rollbackTransaction();
                else s.close();
                raw.get(5,java.util.concurrent.TimeUnit.SECONDS);callback.get(5,java.util.concurrent.TimeUnit.SECONDS);
                assertEquals(2,db.query("SELECT value FROM audit WHERE value <> 'session'",new Object[0]).size());
                assertEquals(boundary==0?1:0,db.query("SELECT value FROM audit WHERE value = 'session'",new Object[0]).size());
                db.execute("DELETE FROM audit",new Object[0]);
            }
        } finally {
            s.close();workers.shutdownNow();workers.awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS);em.close();
        }
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
