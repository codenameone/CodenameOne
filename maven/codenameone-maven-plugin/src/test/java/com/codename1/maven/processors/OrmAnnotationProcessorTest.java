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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Verifies the `@Entity` processor produces a structurally sound dao for a
/// simple POJO entity, and that the negative cases (missing @Id, relationship
/// fields) surface validation errors instead of silently emitting bad SQL.
public class OrmAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void managedPublicContractsDoNotExposeRuntimePlumbing() {
        Class<?>[] contracts = {
            com.codename1.orm.session.Session.class,
            com.codename1.orm.session.Query.class,
            com.codename1.orm.session.JpqlQuery.class
        };
        for (Class<?> contract : contracts) {
            assertTrue(contract.getName(), contract.isInterface());
            for (java.lang.reflect.Method method : contract.getMethods()) {
                assertFalse(method.toString(), method.getGenericReturnType().getTypeName().contains(".impl."));
                for (java.lang.reflect.Type parameter : method.getGenericParameterTypes()) {
                    assertFalse(method.toString(), parameter.getTypeName().contains(".impl."));
                }
            }
        }
    }

    @Test
    public void dependencyEnhancementRefreshPreservesNewApplicationClasses() throws Exception {
        File dependency=tmp.newFolder("relation-dependency"),classes=tmp.newFolder("relation-app");
        Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("overlay.Parent","package overlay; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Parent { @Id public long id; }");
        sources.put("overlay.Child","package overlay; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Child { @Id public long id; @ManyToOne(fetch=FetchType.LAZY) public Parent parent; }");
        sources.put("overlay.Reader","package overlay; public class Reader { public Parent read(Child child) { return child.parent; } }");
        JavaSourceCompiler.compile(sources,dependency,Arrays.asList(testClassesDir()));
        List<String> classpath=new ArrayList<String>(backendClasspath());classpath.add(dependency.getAbsolutePath());
        ProcessorContext first=runProcessor(classes,classpath);assertFalse(first.getErrors().toString(),first.hasErrors());
        File copied=new File(classes,"overlay/Reader.class");assertTrue(copied.isFile());
        byte[] originalDependency=Files.readAllBytes(new File(dependency,"overlay/Reader.class").toPath());
        assertFalse(Arrays.equals(originalDependency,Files.readAllBytes(copied.toPath())));
        compileInto(classes,"overlay.Reader","package overlay; public class Reader { public String applicationCode() { return \"preserved\"; } }");
        byte[] applicationClass=Files.readAllBytes(copied.toPath());
        ProcessorContext second=runProcessor(classes,classpath);assertFalse(second.getErrors().toString(),second.hasErrors());
        org.junit.Assert.assertArrayEquals(applicationClass,Files.readAllBytes(copied.toPath()));
        org.junit.Assert.assertArrayEquals(originalDependency,Files.readAllBytes(new File(dependency,"overlay/Reader.class").toPath()));
        assertTrue(new File(classes,"overlay/Child.class").isFile());
    }

    @Test
    public void clientPropertyScalarsAndCharactersUseManagedStorageConversions() throws Exception {
        File classes=tmp.newFolder("property-scalars");
        Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("propertymodel.Bean","package propertymodel; import com.codename1.annotations.*; import com.codename1.annotations.db.*; import com.codename1.properties.*; @Entity(table=\"property_beans\") public class Bean implements PropertyBusinessObject { @Id public long id; @Version public long version; public char symbol; public final IntProperty<Bean> counter=new IntProperty<Bean>(\"counter\",0); public final CharProperty<Bean> letter=new CharProperty<Bean>(\"letter\",'Q'); private final PropertyIndex index=new PropertyIndex(this,\"Bean\",counter,letter); public PropertyIndex getPropertyIndex() { return index; } }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext ctx=runProcessor(classes);assertFalse(ctx.getErrors().toString(),ctx.hasErrors());
        java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader());loader.loadClass("cn1app.DaoBootstrap").newInstance();Class type=loader.loadClass("propertymodel.Bean");
        Class.forName("org.sqlite.JDBC");
        com.codename1.orm.EntityManager em=com.codename1.orm.EntityManager.open(new com.codename1.impl.javase.SEDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite::memory:")));
        com.codename1.orm.session.Session s=em.openSession();
        try {
            s.createTables();s.beginTransaction();Object bean=type.newInstance();s.persist(bean);s.commitTransaction();Object id=type.getField("id").get(bean);s.clear();
            Object loaded=s.query(type).eq("letter",Character.valueOf('Q')).eq("symbol",Character.valueOf((char)0)).first();org.junit.Assert.assertNotNull(loaded);
            s.beginTransaction();assertTrue(s.increment(type,id,"counter",3));s.commitTransaction();
            org.junit.Assert.assertEquals(Integer.valueOf(3),((com.codename1.properties.Property)type.getField("counter").get(loaded)).get());
            org.junit.Assert.assertEquals(Integer.valueOf(3),s.createQuery("select b.counter from propertymodel.Bean b",Integer.class).first());
            org.junit.Assert.assertEquals(Long.valueOf(3),s.createQuery("select sum(b.counter) from propertymodel.Bean b",Long.class).first());
            org.junit.Assert.assertEquals(Character.valueOf('Q'),s.createQuery("select b.letter from propertymodel.Bean b",Character.class).first());
            org.junit.Assert.assertEquals(Character.valueOf((char)0),s.createQuery("select b.symbol from propertymodel.Bean b",Character.class).first());
            s.beginTransaction();((com.codename1.properties.Property)type.getField("letter").get(loaded)).set(Character.valueOf('Z'));s.commitTransaction();s.clear();
            org.junit.Assert.assertEquals(Character.valueOf('Z'),((com.codename1.properties.Property)type.getField("letter").get(s.find(type,id))).get());
        } finally { s.close();em.close();loader.close(); }
    }

    @Test
    public void scalarElementCollectionsSupportLazyReadsDirtyCheckingAndMembership() throws Exception {
        File classes=tmp.newFolder("elements");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("elements.Profile","package elements; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"element_profiles\") public class Profile { @Id public long id; @Version public long version; @ElementCollection public java.util.List<String> tags=new java.util.ArrayList<String>(); @ElementCollection public java.util.Set<Integer> flags=new java.util.LinkedHashSet<Integer>(); @ElementCollection public java.util.Map<String,java.util.Date> dates=new java.util.LinkedHashMap<String,java.util.Date>(); }");
        sources.put("elements.Reader","package elements; public class Reader { public static java.util.List<String> tags(Profile p) { return p.tags; } public static java.util.Set<Integer> flags(Profile p) { return p.flags; } public static java.util.Map<String,java.util.Date> dates(Profile p) { return p.dates; } }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());
        ProcessorContext ctx=runProcessor(classes,backendClasspath());assertFalse(ctx.getErrors().toString(),ctx.hasErrors());
        java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader());loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();Class type=loader.loadClass("elements.Profile"),reader=loader.loadClass("elements.Reader");
        com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));com.codename1.orm.session.Session s=em.openSession();
        try {
            s.createTables();s.beginTransaction();Object profile=type.newInstance();((List)type.getField("tags").get(profile)).addAll(Arrays.asList("a","b","a",null));((Set)type.getField("flags").get(profile)).add(7);((Map)type.getField("dates").get(profile)).put("start",new java.util.Date(1000));s.persist(profile);s.commitTransaction();Object id=type.getField("id").get(profile);s.clear();
            profile=s.find(type,id);assertFalse(s.isLoaded(profile,"tags"));org.junit.Assert.assertEquals(4,s.count(profile,"tags"));assertFalse(s.isLoaded(profile,"tags"));
            com.codename1.orm.session.Query reusable=s.query(type).eq("id",id);try { reusable.containsElement("tags",42);fail("Wrong element type"); } catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("element")); }org.junit.Assert.assertSame(profile,reusable.first());org.junit.Assert.assertEquals(1,reusable.containsElement("tags","a").count());
            org.junit.Assert.assertSame(profile,s.query(type).containsElement("tags","a").first());org.junit.Assert.assertEquals(1,s.query(type).containsElement("tags",null).count());
            org.junit.Assert.assertEquals(Arrays.asList("a","b","a",null),reader.getMethod("tags",type).invoke(null,profile));org.junit.Assert.assertEquals(java.util.Collections.singleton(7),reader.getMethod("flags",type).invoke(null,profile));
            Map dates=(Map)reader.getMethod("dates",type).invoke(null,profile);s.beginTransaction();((java.util.Date)dates.get("start")).setTime(2000);((List)reader.getMethod("tags",type).invoke(null,profile)).remove(0);s.commitTransaction();s.clear();
            profile=s.find(type,id);org.junit.Assert.assertEquals(Arrays.asList("b","a",null),reader.getMethod("tags",type).invoke(null,profile));org.junit.Assert.assertEquals(2000,((java.util.Date)((Map)reader.getMethod("dates",type).invoke(null,profile)).get("start")).getTime());
            s.detach(profile);((List)reader.getMethod("tags",type).invoke(null,profile)).add("merged");s.beginTransaction();profile=s.merge(profile);s.commitTransaction();org.junit.Assert.assertEquals(4,s.count(profile,"tags"));
            s.beginTransaction();s.remove(profile);s.commitTransaction();org.junit.Assert.assertEquals(0,s.query(type).count());
        } finally { s.close();em.close();loader.close(); }
    }

    @Test
    public void orderedListsAndEntityMapsRoundTripWithoutLosingDuplicateLinks() throws Exception {
        File classes=tmp.newFolder("collections");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("collections.Thing","package collections; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"collection_things\") public class Thing { @Id public long id; public String label; }");
        sources.put("collections.Box","package collections; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"collection_boxes\") public class Box { @Id public long id; @Version public long version; @ManyToMany(cascade=CascadeType.PERSIST) @OrderColumn public java.util.List<Thing> sequence=new java.util.ArrayList<Thing>(); @ManyToMany(cascade=CascadeType.PERSIST) public java.util.List<Thing> bag=new java.util.ArrayList<Thing>(); @ManyToMany(cascade=CascadeType.PERSIST) @OrderBy(\"label DESC\") public java.util.List<Thing> sorted=new java.util.ArrayList<Thing>(); @OneToMany(cascade=CascadeType.PERSIST) @MapKey(name=\"label\") public java.util.Map<String,Thing> named=new java.util.LinkedHashMap<String,Thing>(); }");
        sources.put("collections.Reader","package collections; public class Reader { public static java.util.List<Thing> sequence(Box b) { return b.sequence; } public static java.util.Map<String,Thing> named(Box b) { return b.named; } }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());
        ProcessorContext ctx=runProcessor(classes,backendClasspath());assertFalse(ctx.getErrors().toString(),ctx.hasErrors());
        java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader());loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();
        Class boxType=loader.loadClass("collections.Box"),thingType=loader.loadClass("collections.Thing"),reader=loader.loadClass("collections.Reader");
        com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));com.codename1.orm.session.Session s=em.openSession();
        try {
            s.createTables();s.beginTransaction();Object box=boxType.newInstance(),a=thingType.newInstance(),b=thingType.newInstance();thingType.getField("label").set(a,"a");thingType.getField("label").set(b,"b");
            List sequence=(List)boxType.getField("sequence").get(box);sequence.add(a);sequence.add(b);sequence.add(a);Map named=(Map)boxType.getField("named").get(box);named.put("a",a);named.put("b",b);
            ((List)boxType.getField("bag").get(box)).addAll(Arrays.asList(a,b,a));((List)boxType.getField("sorted").get(box)).addAll(Arrays.asList(a,b,a));
            s.persist(box);s.commitTransaction();Object id=boxType.getField("id").get(box);s.clear();box=s.find(boxType,id);
            assertFalse(s.isLoaded(box,"sequence"));sequence=(List)reader.getMethod("sequence",boxType).invoke(null,box);org.junit.Assert.assertSame(sequence.get(0),sequence.get(2));
            named=(Map)reader.getMethod("named",boxType).invoke(null,box);org.junit.Assert.assertSame(sequence.get(0),named.get("a"));org.junit.Assert.assertSame(sequence.get(1),named.get("b"));
            s.beginTransaction();Object first=sequence.remove(0);sequence.add(first);s.commitTransaction();s.clear();box=s.find(boxType,id);
            sequence=(List)reader.getMethod("sequence",boxType).invoke(null,box);org.junit.Assert.assertEquals("b",thingType.getField("label").get(sequence.get(0)));org.junit.Assert.assertSame(sequence.get(1),sequence.get(2));
            s.initialize(box,"bag");s.initialize(box,"sorted");List bag=(List)boxType.getField("bag").get(box),sorted=(List)boxType.getField("sorted").get(box);
            org.junit.Assert.assertEquals(3,bag.size());org.junit.Assert.assertSame(bag.get(0),bag.get(2));
            org.junit.Assert.assertEquals("b",thingType.getField("label").get(sorted.get(0)));org.junit.Assert.assertSame(sorted.get(1),sorted.get(2));
            s.beginTransaction();bag.remove(0);bag.add(bag.get(0));bag.add(bag.get(1));s.commitTransaction();s.clear();box=s.find(boxType,id);s.initialize(box,"bag");
            bag=(List)boxType.getField("bag").get(box);org.junit.Assert.assertEquals(4,bag.size());org.junit.Assert.assertSame(bag.get(0),bag.get(2));org.junit.Assert.assertSame(bag.get(1),bag.get(3));
            org.junit.Assert.assertEquals(3,s.count(box,"sequence"));s.beginTransaction();s.remove(box);s.commitTransaction();org.junit.Assert.assertEquals(2,s.query(thingType).count());
        } finally { s.close();em.close();loader.close(); }
    }

    @Test
    public void polymorphicSingleTableInheritancePreservesIdentityAndSubtypeQueries() throws Exception {
        File classes=tmp.newFolder("inheritance");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("hierarchy.Animal","package hierarchy; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"hierarchy_animals\") @Inheritance public abstract class Animal { @Id public long id; @Version public long version; public String name; public long counter; @DbTransient public int callbacks; @PrePersist public void baseCallback() { callbacks++; } }");
        sources.put("hierarchy.Cat","package hierarchy; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity @DiscriminatorValue(\"cat\") public class Cat extends Animal { public int lives; @PrePersist public void catCallback() { callbacks+=10; } }");
        sources.put("hierarchy.Dog","package hierarchy; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity @DiscriminatorValue(\"dog\") public class Dog extends Animal { public String breed; @ManyToOne(fetch=FetchType.LAZY,cascade=CascadeType.PERSIST) public Keeper keeper; }");
        sources.put("hierarchy.Keeper","package hierarchy; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"hierarchy_keepers\") public class Keeper { @Id public long id; public String name; }");
        sources.put("hierarchy.Shelter","package hierarchy; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"hierarchy_shelters\") public class Shelter { @Id public long id; @OneToMany(cascade=CascadeType.ALL,orphanRemoval=true) public java.util.List<Animal> animals=new java.util.ArrayList<Animal>(); }");
        sources.put("hierarchy.Reader","package hierarchy; public class Reader { public static Keeper keeper(Dog d) { return d.keeper; } public static java.util.List<Animal> animals(Shelter s) { return s.animals; } }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());
        ProcessorContext ctx=runProcessor(classes,backendClasspath());assertFalse(ctx.getErrors().toString(),ctx.hasErrors());
        java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader());
        loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();Class base=loader.loadClass("hierarchy.Animal"),catType=loader.loadClass("hierarchy.Cat"),dogType=loader.loadClass("hierarchy.Dog"),keeperType=loader.loadClass("hierarchy.Keeper"),shelterType=loader.loadClass("hierarchy.Shelter"),reader=loader.loadClass("hierarchy.Reader");
        com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));
        com.codename1.orm.session.Session s=em.openSession();
        try {
            em.createTables();s.validateSchema();s.beginTransaction();
            Object cat=catType.newInstance(),dog=dogType.newInstance(),keeper=keeperType.newInstance(),shelter=shelterType.newInstance();
            base.getField("name").set(cat,"Milo");catType.getField("lives").setInt(cat,9);base.getField("name").set(dog,"Fido");dogType.getField("breed").set(dog,"terrier");dogType.getField("keeper").set(dog,keeper);
            List animals=(List)shelterType.getField("animals").get(shelter);animals.add(cat);animals.add(dog);s.persist(shelter);s.commitTransaction();
            org.junit.Assert.assertEquals(11,base.getField("callbacks").getInt(cat));
            Object catId=base.getField("id").get(cat),dogId=base.getField("id").get(dog),shelterId=shelterType.getField("id").get(shelter);s.clear();
            Object foundCat=s.find(base,catId);org.junit.Assert.assertEquals(catType,foundCat.getClass());org.junit.Assert.assertSame(foundCat,s.find(catType,catId));
            org.junit.Assert.assertNull(s.find(dogType,catId));org.junit.Assert.assertEquals(2,s.query(base).count());org.junit.Assert.assertEquals(1,s.query(catType).count());
            s.beginTransaction();assertFalse(s.increment(catType,dogId,"counter",5));s.commitTransaction();
            Object sibling=s.find(dogType,dogId);org.junit.Assert.assertEquals(0,base.getField("counter").getLong(sibling));org.junit.Assert.assertEquals(0,base.getField("version").getLong(sibling));
            s.beginTransaction();assertFalse(s.increment(catType,dogId,"counter",5));assertTrue(s.increment(catType,catId,"counter",2));s.commitTransaction();
            org.junit.Assert.assertEquals(0,base.getField("counter").getLong(sibling));org.junit.Assert.assertEquals(0,base.getField("version").getLong(sibling));org.junit.Assert.assertEquals(2,base.getField("counter").getLong(foundCat));
            s.beginTransaction();assertTrue(s.increment(base,dogId,"counter",3));s.commitTransaction();org.junit.Assert.assertEquals(3,base.getField("counter").getLong(sibling));
            Object foundDog=s.find(base,dogId);assertFalse(s.isLoaded(foundDog,"keeper"));org.junit.Assert.assertNotNull(reader.getMethod("keeper",dogType).invoke(null,foundDog));
            List loaded=(List)reader.getMethod("animals",shelterType).invoke(null,s.find(shelterType,shelterId));org.junit.Assert.assertEquals(2,loaded.size());
            s.beginTransaction();catType.getField("lives").setInt(foundCat,8);s.commitTransaction();s.clear();org.junit.Assert.assertEquals(8,catType.getField("lives").getInt(s.find(base,catId)));
            s.beginTransaction();org.junit.Assert.assertEquals(1,s.createQuery("update Cat c set c.name = :name").setParameter("name","Kitty").executeUpdate());s.commitTransaction();
            org.junit.Assert.assertEquals("Fido",base.getField("name").get(s.find(base,dogId)));
            s.beginTransaction();s.remove(s.find(shelterType,shelterId));s.commitTransaction();org.junit.Assert.assertEquals(0,s.query(base).count());
        } finally { s.close();em.close();loader.close(); }
    }

    @Test
    public void booleanConvertersAcceptDomainParameters() throws Exception {
        File classes=tmp.newFolder("logicalconverter");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("logicalconverter.Flag","package logicalconverter; public enum Flag { YES, NO, MISSING }");
        sources.put("logicalconverter.Converter","package logicalconverter; public class Converter implements com.codename1.orm.session.AttributeConverter<Flag,Boolean> { public Boolean toDatabase(Flag f) { return f==null||f==Flag.MISSING?null:f==Flag.YES; } public Flag fromDatabase(Boolean b) { return b==null?Flag.MISSING:b?Flag.YES:Flag.NO; } }");
        sources.put("logicalconverter.Entry","package logicalconverter; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Entry { @Id public long id; @Convert(converter=Converter.class,storageType=Boolean.class) public Flag flag=Flag.YES; public boolean plain=true; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class type=loader.loadClass("logicalconverter.Entry"),flag=loader.loadClass("logicalconverter.Flag");Object yes=Enum.valueOf(flag,"YES"),no=Enum.valueOf(flag,"NO"),missing=Enum.valueOf(flag,"MISSING");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());
                com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try {
                    session.createTables();session.beginTransaction();session.persist(type.newInstance());session.commitTransaction();
                    for(String predicate:Arrays.asList("e.flag=:flag",":flag=e.flag","e.flag in (:flag)","e.flag=coalesce(:flag,e.flag)")) org.junit.Assert.assertEquals(predicate,1,session.createQuery("select e from Entry e where "+predicate).setParameter("flag",predicate.contains(" in ")?Arrays.asList(yes):yes).list().size());
                    org.junit.Assert.assertEquals(1,session.query(type).eq("flag",yes).count());
                    org.junit.Assert.assertEquals(0,session.createQuery("select e from Entry e where e.flag=:flag").setParameter("flag",missing).list().size());
                    for(Object invalid:Arrays.asList("YES",Boolean.TRUE,1L)) org.junit.Assert.assertThrows(IllegalArgumentException.class,()->session.createQuery("select e from Entry e where e.flag=:flag").setParameter("flag",invalid).list());
                    org.junit.Assert.assertThrows(IllegalArgumentException.class,()->session.createQuery("select e from Entry e where e.plain=:flag").setParameter("flag",1L).list());
                    session.beginTransaction();org.junit.Assert.assertEquals(1,session.createQuery("update Entry e set e.flag=:flag").setParameter("flag",no).executeUpdate());session.commitTransaction();org.junit.Assert.assertEquals(no,session.createQuery("select e.flag from Entry e",flag).first());
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void replacingUnloadedOrphansFlushesOwnershipMoves() throws Exception {
        for(boolean many:Arrays.asList(false,true)) {
            File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();String imports="package movedorphan; import com.codename1.annotations.*; import com.codename1.annotations.db.*; ";
            sources.put("movedorphan.Parent",imports+"@Entity public class Parent { @Id public long id; @"+(many?"OneToMany":"OneToOne")+"(mappedBy=\"parent\",fetch=FetchType.LAZY,orphanRemoval=true) public "+(many?"java.util.List<Child>":"Child")+" children; public void clearChildren() { children="+(many?"new java.util.ArrayList<Child>()":"null")+"; } }");
            sources.put("movedorphan.Child",imports+"@Entity public class Child { @Id public long id; @"+(many?"ManyToOne":"OneToOne")+" public Parent parent; }");
            JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
            try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
                Class parent=loader.loadClass("movedorphan.Parent"),child=loader.loadClass("movedorphan.Child");
                for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                    Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();for(Class type:Arrays.asList(parent,child)) models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());
                    com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                    try {
                        session.createTables();session.beginTransaction();Object oldParent=parent.newInstance(),newParent=parent.newInstance(),item=child.newInstance();child.getField("parent").set(item,oldParent);session.persist(oldParent);session.persist(newParent);session.persist(item);session.commitTransaction();Object oldId=parent.getField("id").get(oldParent),newId=parent.getField("id").get(newParent),childId=child.getField("id").get(item);session.clear();
                        oldParent=session.find(parent,oldId);newParent=session.find(parent,newId);item=session.find(child,childId);assertFalse(session.isLoaded(oldParent,"children"));session.beginTransaction();child.getField("parent").set(item,newParent);parent.getMethod("clearChildren").invoke(oldParent);session.commitTransaction();session.clear();
                        item=session.find(child,childId);org.junit.Assert.assertNotNull("Moving ownership must not delete the child",item);org.junit.Assert.assertEquals(newId,parent.getField("id").get(child.getField("parent").get(item)));org.junit.Assert.assertEquals(0,session.count(session.find(parent,oldId),"children"));org.junit.Assert.assertEquals(1,session.count(session.find(parent,newId),"children"));
                        session.clear();newParent=session.find(parent,newId);assertFalse(session.isLoaded(newParent,"children"));session.beginTransaction();parent.getMethod("clearChildren").invoke(newParent);session.commitTransaction();org.junit.Assert.assertEquals(0,session.query(child).count());
                    } finally { session.close();db.close(); }
                }
            }
        }
    }

    @Test
    public void owningToOneCountsUsePersistedForeignKeys() throws Exception {
        for(boolean composite:Arrays.asList(false,true)) {
            File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();String imports="package storedcount; import com.codename1.annotations.*; import com.codename1.annotations.db.*; ";
            String id=composite?"@EmbeddedId public Key id=new Key();":"@Id(autoIncrement=false) public long id;";
            sources.put("storedcount.Key",imports+"@Embeddable public class Key { public long first,second; }");
            sources.put("storedcount.Parent",imports+"@Entity public class Parent { "+id+" @ManyToOne(fetch=FetchType.LAZY) public Child child; public void setChild(Child value) { child=value; } }");
            sources.put("storedcount.Child",imports+"@Entity public class Child { "+id+" }");
            JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
            try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
                Class parent=loader.loadClass("storedcount.Parent"),child=loader.loadClass("storedcount.Child");
                for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                    Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();for(Class type:Arrays.asList(parent,child)) models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());
                    com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                    try {
                        session.createTables();session.beginTransaction();Object owner=parent.newInstance(),target=child.newInstance();session.persist(target);parent.getField("child").set(owner,target);session.persist(owner);session.commitTransaction();Object idValue=parent.getField("id").get(owner);session.clear();
                        parent.getField("child").set(owner,null);org.junit.Assert.assertEquals(1,session.count(owner,"child"));
                        Object managed=session.find(parent,idValue);assertFalse(session.isLoaded(managed,"child"));org.junit.Assert.assertEquals(1,session.count(managed,"child"));assertFalse(session.isLoaded(managed,"child"));
                        session.beginTransaction();parent.getMethod("setChild",child).invoke(managed,new Object[]{null});org.junit.Assert.assertEquals(0,session.count(managed,"child"));session.commitTransaction();session.clear();parent.getField("child").set(owner,target);org.junit.Assert.assertEquals(0,session.count(owner,"child"));
                        managed=session.find(parent,idValue);session.beginTransaction();session.remove(managed);session.commitTransaction();org.junit.Assert.assertEquals(0,session.count(owner,"child"));
                    } finally { session.close();db.close(); }
                }
            }
        }
    }

    @Test
    public void countOmitsOnlyOrderingJoins() throws Exception {
        File classes=tmp.newFolder("countjoins");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();String imports="package countjoins; import com.codename1.annotations.*; import com.codename1.annotations.db.*; ";
        sources.put("countjoins.Child",imports+"@Entity public class Child { @Id public long id; public String name=\"child\"; @ManyToOne(cascade=CascadeType.PERSIST) public Child next; }");
        sources.put("countjoins.Parent",imports+"@Entity public class Parent { @Id public long id; @ManyToOne(cascade=CascadeType.PERSIST) public Child child; @OneToMany(cascade=CascadeType.PERSIST) public java.util.List<Child> children=new java.util.ArrayList<Child>(); }");JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class parent=loader.loadClass("countjoins.Parent"),child=loader.loadClass("countjoins.Child");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();for(Class type:Arrays.asList(parent,child)) models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try { session.createTables();session.beginTransaction();Object empty=parent.newInstance(),full=parent.newInstance(),one=child.newInstance(),two=child.newInstance();child.getField("next").set(one,two);parent.getField("child").set(full,one);((java.util.List)parent.getField("children").get(full)).addAll(Arrays.asList(one,two));session.persist(empty);session.persist(full);session.commitTransaction();
                    for(String field:Arrays.asList("child.name","child.next.name","children.name")) org.junit.Assert.assertEquals(field,2,session.query(parent).orderBy(field,true).limit(1).offset(1).count());
                    org.junit.Assert.assertEquals(1,session.query(parent).orderBy("child.name",true).list().size());
                    org.junit.Assert.assertEquals(1,session.query(parent).orderBy("child.next.name",true).eq("child.next.name","child").count());
                    org.junit.Assert.assertEquals(1,session.query(parent).eq("child.name","child").orderBy("child.next.name",true).count());
                    org.junit.Assert.assertEquals(1,session.query(parent).orderBy("child.next.name",true).join("child.next").count());
                    org.junit.Assert.assertEquals(1,session.query(parent).join("child").orderBy("child.name",true).count());
                    org.junit.Assert.assertEquals(2,session.query(parent).leftJoin("child").orderBy("child.name",true).count());
                    for(String path:Arrays.asList("child","child.next")) {
                        com.codename1.orm.session.Query after=session.query(parent).orderBy(path+".name",true).leftJoin(path).isNull(path+".name");
                        org.junit.Assert.assertEquals(path,1,after.count());org.junit.Assert.assertEquals(path,1,after.list().size());
                        org.junit.Assert.assertSame(empty,after.first());
                    }
                    org.junit.Assert.assertEquals(1,session.query(parent).orderBy("children.name",true).leftJoin("children").isNull("children.name").count());
                    org.junit.Assert.assertEquals(1,session.query(parent).orderBy("children.name",true).eq("children.name","child").count());
                    org.junit.Assert.assertEquals(1,session.query(parent).join("children").orderBy("child.next.name",true).count());
                    session.beginTransaction();Object partial=parent.newInstance();parent.getField("child").set(partial,child.newInstance());session.persist(partial);session.commitTransaction();org.junit.Assert.assertEquals(2,session.query(parent).orderBy("child.next.name",true).eq("child.name","child").count());org.junit.Assert.assertEquals(1,session.query(parent).orderBy("child.next.name",true).join("child.next").count());

                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void coalesceRequiresCompatibleResultMappings() throws Exception {
        File classes=tmp.newFolder("coalescemappings");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();String imports="package coalescemappings; import com.codename1.annotations.*; import com.codename1.annotations.db.*; ";
        sources.put("coalescemappings.State","package coalescemappings; public enum State { ACTIVE, INACTIVE }");sources.put("coalescemappings.Color","package coalescemappings; public enum Color { RED, BLUE }");
        for(String name:Arrays.asList("First","Second")) sources.put("coalescemappings."+name,"package coalescemappings; public class "+name+" implements com.codename1.orm.session.AttributeConverter<String,String> { public String toDatabase(String v) { return v==null?null:\""+name+":\"+v; } public String fromDatabase(String v) { return v==null?null:v.substring("+(name.length()+1)+"); } }");
        sources.put("coalescemappings.Entry",imports+"@Entity public class Entry { @Id public long id; @Convert(converter=First.class) public String code,backup=\"fallback\"; @Convert(converter=Second.class) public String other=\"other\"; public String name=\"raw\"; public State state,backupState=State.ACTIVE; public Color color=Color.RED; }");JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class type=loader.loadClass("coalescemappings.Entry");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try { session.createTables();session.beginTransaction();session.persist(type.newInstance());session.commitTransaction();
                    for(String expression:Arrays.asList("coalesce(e.code,e.name)","coalesce(e.name,e.code)","coalesce(e.code,e.other)","coalesce(e.state,e.color)","coalesce(e.code,lower(e.backup))","coalesce(lower(e.backup),e.code)","coalesce(e.code,(select max(i.name) from Entry i))","coalesce(e.name,coalesce(e.code,e.backup))")) { try { session.createQuery("select "+expression+" from Entry e");fail(expression); } catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("mapping")); } }
                    org.junit.Assert.assertEquals("fallback",session.createQuery("select coalesce(e.code,e.backup) from Entry e",String.class).first());org.junit.Assert.assertEquals("ACTIVE",session.createQuery("select coalesce(e.state,e.backupState) from Entry e").first().toString());
                    org.junit.Assert.assertEquals("literal",session.createQuery("select coalesce(e.code,'literal') from Entry e",String.class).first());org.junit.Assert.assertEquals("parameter",session.createQuery("select coalesce(e.code,:value) from Entry e",String.class).setParameter("value","parameter").first());
                    org.junit.Assert.assertEquals("raw",session.createQuery("select coalesce(lower(e.name),'fallback') from Entry e",String.class).first());
                    for(String predicate:Arrays.asList("e.code=e.other","e.other<>e.code","e.code=e.name","e.name=e.code","e.code>e.other","e.code between e.other and e.backup","e.code in (e.other)","e.code in (select i.name from Entry i)","e.code like e.other","nullif(e.code,e.other) is null","e.state=e.color","e.code=lower(e.backup)")) { try { session.createQuery("select e from Entry e where "+predicate);fail(predicate); } catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("mapping")); } }
                    session.beginTransaction();session.createQuery("update Entry e set e.code=e.backup").executeUpdate();session.commitTransaction();
                    for(String predicate:Arrays.asList("e.code=e.backup","e.code in (select i.backup from Entry i)","nullif(e.code,e.backup) is null","e.backupState=e.backupState","e.name=lower(e.name)")) org.junit.Assert.assertEquals(predicate,1,session.createQuery("select e from Entry e where "+predicate).list().size());

                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void generatedIntegralMappingsEnforceJavaRanges() throws Exception {
        File classes=tmp.newFolder("bounded");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();sources.put("bounded.Entry","package bounded; import com.codename1.annotations.*; @Entity public class Entry { @Id(autoIncrement=false) public int id=1; public int amount; public Integer boxed; public byte narrow; public short small; public char letter; }");JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class type=loader.loadClass("bounded.Entry");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try { session.createTables();session.beginTransaction();session.persist(type.newInstance());session.commitTransaction();
                    String[] fields={"amount","boxed","narrow","small","letter"};long[] lows={Integer.MIN_VALUE,Integer.MIN_VALUE,Byte.MIN_VALUE,Short.MIN_VALUE,Character.MIN_VALUE},highs={Integer.MAX_VALUE,Integer.MAX_VALUE,Byte.MAX_VALUE,Short.MAX_VALUE,Character.MAX_VALUE};
                    for(int i=0;i<fields.length;i++) {
                        for(long invalid:new long[]{lows[i]-1,highs[i]+1}) { session.beginTransaction();try { session.createQuery("update Entry e set e."+fields[i]+"=:value").setParameter("value",invalid).executeUpdate();fail("Stored out-of-range "+fields[i]); } catch(com.codename1.orm.session.PersistenceException expected) { session.rollbackTransaction(); } }
                        for(long valid:new long[]{lows[i],highs[i]}) { session.beginTransaction();org.junit.Assert.assertEquals(1,session.createQuery("update Entry e set e."+fields[i]+"=:value").setParameter("value",valid).executeUpdate());session.commitTransaction();Object stored=type.getField(fields[i]).get(session.find(type,1));org.junit.Assert.assertEquals(valid,stored instanceof Character?((Character)stored).charValue():((Number)stored).longValue()); }
                    }
                    session.beginTransaction();org.junit.Assert.assertEquals(1,session.createQuery("update Entry e set e.boxed=NULL").executeUpdate());session.commitTransaction();org.junit.Assert.assertNull(type.getField("boxed").get(session.find(type,1)));
                    try { session.find(type,2147483648L);fail("Out-of-range identifier"); } catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("range")); }
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void mappedNullLiteralsUseConverterSentinels() throws Exception {
        File classes=tmp.newFolder("nullsentinel");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();String imports="package nullsentinel; import com.codename1.annotations.*; import com.codename1.annotations.db.*; ";
        sources.put("nullsentinel.Sentinel","package nullsentinel; public class Sentinel implements com.codename1.orm.session.AttributeConverter<String,String> { public String toDatabase(String value) { return value==null?\"<null>\":\"db:\"+value; } public String fromDatabase(String value) { return value==null || value.equals(\"<null>\")?null:value.substring(3); } }");
        sources.put("nullsentinel.Entry",imports+"@Entity(table=\"null_sentinel\") public class Entry { @Id public long id; @Convert(converter=Sentinel.class) public String code; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class type=loader.loadClass("nullsentinel.Entry");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());
                com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try {
                    session.createTables();session.beginTransaction();Object entity=type.newInstance();session.persist(entity);session.commitTransaction();
                    for(String assignment:Arrays.asList("NULL","(NULL)",":value")) {
                        session.beginTransaction();session.createQuery("update Entry e set e.code='Paris'").executeUpdate();com.codename1.orm.session.JpqlQuery query=session.createQuery("update Entry e set e.code="+assignment);if(assignment.equals(":value")) query.setParameter("value",null);org.junit.Assert.assertEquals(1,query.executeUpdate());session.commitTransaction();
                        org.junit.Assert.assertEquals("<null>",((java.util.Map)db.query("select code from null_sentinel",new Object[0]).get(0)).get("code"));
                        org.junit.Assert.assertEquals(1,session.createQuery("select e from Entry e where e.code=NULL").list().size());org.junit.Assert.assertEquals(1,session.createQuery("select e from Entry e where e.code=:value").setParameter("value",null).list().size());
                        org.junit.Assert.assertNull(session.createQuery("select e.code from Entry e",String.class).first());
                    }
                    org.junit.Assert.assertEquals(1,session.query(type).eq("code",null).count());org.junit.Assert.assertEquals(0,session.query(type).ne("code",null).count());org.junit.Assert.assertEquals(1,session.query(type).in("code",new Object[]{null}).count());
                    session.beginTransaction();session.createQuery("update Entry e set e.code='Paris'").executeUpdate();session.commitTransaction();org.junit.Assert.assertEquals(0,session.query(type).eq("code",null).count());org.junit.Assert.assertEquals(1,session.query(type).ne("code",null).count());
                    org.junit.Assert.assertEquals(Long.valueOf(0),session.createQuery("select count(NULL) from Entry e",Long.class).first());
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void prePersistCascadesJoinTheCurrentFlush() throws Exception {
        callbackCascadesJoinTheCurrentFlush(false);
    }

    @Test
    public void preUpdateCascadesJoinTheCurrentFlush() throws Exception {
        callbackCascadesJoinTheCurrentFlush(true);
    }

    private void callbackCascadesJoinTheCurrentFlush(boolean updating) throws Exception {
        for(boolean generated:Arrays.asList(false,true)) {
            File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();String imports="package callbackcascade; import com.codename1.annotations.*; import com.codename1.annotations.db.*; ";
            sources.put("callbackcascade.Child",imports+"@Entity public class Child { @Id(autoIncrement="+generated+") public long id"+(generated?";":"=++sequence; public static long sequence=100;")+" @Version public long version; @ManyToOne(cascade=CascadeType.PERSIST) public Child next; @ElementCollection public java.util.List<String> tags=new java.util.ArrayList<String>(); @DbTransient public int depth,pre,post,updates; @PrePersist public void before() { pre++; tags.add(\"created\"); if(depth==0) { next=new Child();next.depth=1;next.next=this; } } @PostPersist public void after() { post++; } @PreUpdate public void update() { updates++; } }");
            sources.put("callbackcascade.Owner",imports+"@Entity public class Owner { @Id public long id; @Version public long version; public String name; @ManyToOne(cascade=CascadeType.PERSIST) public Child child; @OneToMany(cascade=CascadeType.PERSIST) public java.util.List<Child> children=new java.util.ArrayList<Child>(); @DbTransient public int pre,post; @"+(updating?"PreUpdate":"PrePersist")+" public void before() { pre++;child=new Child();children.add(new Child()); } @"+(updating?"PostUpdate":"PostPersist")+" public void after() { post++; } }");
            JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
            try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
                Class ownerType=loader.loadClass("callbackcascade.Owner"),childType=loader.loadClass("callbackcascade.Child");
                for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                    Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();for(Class type:Arrays.asList(ownerType,childType)) models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());
                    com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                    try {
                        session.createTables();session.beginTransaction();Object owner=ownerType.newInstance();session.persist(owner);
                        if(updating) { session.commitTransaction();session.beginTransaction();ownerType.getField("name").set(owner,"changed"); }
                        session.flush();session.flush();org.junit.Assert.assertEquals(1,ownerType.getField("pre").getInt(owner));org.junit.Assert.assertEquals(1,ownerType.getField("post").getInt(owner));org.junit.Assert.assertEquals(updating?1:0,ownerType.getField("version").getLong(owner));
                        org.junit.Assert.assertEquals(4,session.query(childType).count());for(Object child:session.query(childType).list()) { org.junit.Assert.assertEquals(1,childType.getField("pre").getInt(child));org.junit.Assert.assertEquals(1,childType.getField("post").getInt(child));org.junit.Assert.assertEquals(0,childType.getField("updates").getInt(child)); }
                        Object childId=childType.getField("id").get(ownerType.getField("child").get(owner));
                        session.commitTransaction();Object id=ownerType.getField("id").get(owner);session.clear();owner=session.find(ownerType,id);session.initialize(owner,"children");org.junit.Assert.assertEquals(1,((java.util.List)ownerType.getField("children").get(owner)).size());
                        org.junit.Assert.assertEquals(childId,childType.getField("id").get(ownerType.getField("child").get(owner)));org.junit.Assert.assertEquals(updating?1:0,ownerType.getField("version").getLong(owner));
                        for(Object child:session.query(childType).list()) { Object next=childType.getField("next").get(child);org.junit.Assert.assertSame(child,childType.getField("next").get(next));session.initialize(child,"tags");org.junit.Assert.assertEquals(Arrays.asList("created"),childType.getField("tags").get(child)); }
                    } finally { session.close();db.close(); }
                }
            }
        }
    }

    @Test
    public void enumAssignmentsValidateDomainValuesBeforeWriting() throws Exception {
        File classes=tmp.newFolder("enumparameters");
        Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("enumparameters.State","package enumparameters; public enum State { OPEN, CLOSED }");
        sources.put("enumparameters.Other","package enumparameters; public enum Other { OPEN, UNKNOWN }");
        sources.put("enumparameters.Entry","package enumparameters; import com.codename1.annotations.*; @Entity public class Entry { @Id public long id; public State state=State.OPEN; public String name=\"original\"; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());
        ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class type=loader.loadClass("enumparameters.Entry"),stateType=loader.loadClass("enumparameters.State"),otherType=loader.loadClass("enumparameters.Other");
            Object open=Enum.valueOf(stateType,"OPEN"),closed=Enum.valueOf(stateType,"CLOSED");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                com.codename1.impl.orm.EntityModel model=(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance();
                assertFalse("Enums alone must not disable legacy DAOs",model.requiresSession());
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();models.put(type.getName(),model);
                com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");
                com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try {
                    session.createTables();session.beginTransaction();Object entity=type.newInstance();session.persist(entity);session.commitTransaction();Object id=type.getField("id").get(entity);
                    session.beginTransaction();
                    for(Object invalid:Arrays.asList("INVALID",Enum.valueOf(otherType,"UNKNOWN"),Enum.valueOf(otherType,"OPEN"),Integer.valueOf(1))) {
                        try { session.createQuery("update Entry e set e.name='changed',e.state=:state").setParameter("state",invalid).executeUpdate();fail("Accepted invalid enum: "+invalid); }
                        catch(IllegalArgumentException expected) { /* Reject before executing the update. */ }
                        org.junit.Assert.assertEquals("original",session.createQuery("select e.name from Entry e",String.class).first());
                        org.junit.Assert.assertEquals(open,session.createQuery("select e.state from Entry e",stateType).first());
                    }
                    try { session.createQuery("update Entry e set e.state='INVALID'").executeUpdate();fail("Accepted invalid enum literal"); }
                    catch(IllegalArgumentException expected) { /* Literals follow the same validation. */ }
                    for(Object valid:Arrays.asList(closed,"OPEN",null)) {
                        org.junit.Assert.assertEquals(1,session.createQuery("update Entry e set e.state=:state").setParameter("state",valid).executeUpdate());
                        org.junit.Assert.assertEquals(valid==closed?closed:valid==null?null:open,session.createQuery("select e.state from Entry e",stateType).first());
                    }
                    org.junit.Assert.assertEquals(1,session.createQuery("update Entry e set e.state='CLOSED'").executeUpdate());
                    org.junit.Assert.assertEquals(1,session.createQuery("select e from Entry e where e.state=:state").setParameter("state",closed).list().size());
                    org.junit.Assert.assertEquals(1,session.createQuery("select e from Entry e where e.state='CLOSED'").list().size());
                    session.commitTransaction();session.clear();org.junit.Assert.assertEquals(closed,type.getField("state").get(session.find(type,id)));
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void booleanMappingsRejectNativeBooleanDeclarations() throws Exception {
        for(boolean backend:Arrays.asList(false,true)) for(String declaration:Arrays.asList("BOOLEAN","bool"," Boolean NOT NULL ")) {
            File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();String imports="package nativebool; import com.codename1.annotations.*; ";
            sources.put("nativebool.Entry",imports+"@Entity public class Entry { @Id public long id; @Column(type=\""+declaration+"\") public boolean flag; @Column(type=\""+declaration+"\") public Boolean optional; }");JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext ctx=runProcessor(classes,backend?backendClasspath():Collections.<String>emptyList());assertTrue(ctx.hasErrors());assertTrue(ctx.getErrors().toString(),ctx.getErrors().toString().contains("Boolean mappings use numeric storage"));
        }
        File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();sources.put("numericbool.Entry","package numericbool; import com.codename1.annotations.*; @Entity public class Entry { @Id public long id; @Column(type=\"SMALLINT\") public boolean flag; public Boolean optional; }");JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
    }

    @Test
    public void jpqlLiteralsUseMappedConvertersLikeNamedParameters() throws Exception {
        File classes=tmp.newFolder("mappedliterals");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();String imports="package mappedliterals; import com.codename1.annotations.*; import com.codename1.annotations.db.*; ";
        sources.put("mappedliterals.Prefix","package mappedliterals; public class Prefix implements com.codename1.orm.session.AttributeConverter<String,String> { public String toDatabase(String value) { return value==null?null:\"db:\"+value; } public String fromDatabase(String value) { return value==null?null:value.substring(3); } }");
        sources.put("mappedliterals.Entry",imports+"@Entity public class Entry { @Id public long id; @Convert(converter=Prefix.class) public String code; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class type=loader.loadClass("mappedliterals.Entry");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());
                com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try {
                    session.createTables();session.beginTransaction();Object entity=type.newInstance();type.getField("code").set(entity,"Paris");session.persist(entity);session.commitTransaction();Object id=type.getField("id").get(entity);
                    for(String predicate:Arrays.asList("e.code='Paris'","'Paris'=e.code","e.code=('Paris')","e.code in ('Paris','Rome')","e.code between 'Paris' and 'Rome'","e.code like 'Par%'","e.code like 'Par%' escape '!'","coalesce(e.code,'Rome')='Paris'","e.code=nullif('Paris','Rome')")) {
                        com.codename1.orm.session.JpqlQuery query=session.createQuery("select e from Entry e where "+predicate);org.junit.Assert.assertEquals(predicate,1,query.list().size());org.junit.Assert.assertEquals(predicate,1,query.list().size());
                    }
                    org.junit.Assert.assertEquals(1,session.createQuery("select e from Entry e where e.code=:code").setParameter("code","Paris").list().size());
                    org.junit.Assert.assertEquals("Paris",session.createQuery("select 'Paris' from Entry e",String.class).first());
                    session.beginTransaction();org.junit.Assert.assertEquals(1,session.createQuery("update Entry e set e.code='Rome' where e.code='Paris'").executeUpdate());session.commitTransaction();session.clear();org.junit.Assert.assertEquals("Rome",type.getField("code").get(session.find(type,id)));
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void cancellingFreshOwnerPreservesRemovalOfPersistedDescendants() throws Exception {
        File classes=tmp.newFolder("cancelledowner");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();String imports="package cancelledowner; import com.codename1.annotations.*; import com.codename1.annotations.db.*; ";
        sources.put("cancelledowner.Node",imports+"@Entity public class Node { @Id public long id; @ManyToOne(cascade=CascadeType.ALL) public Node child; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class type=loader.loadClass("cancelledowner.Node");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());
                com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try {
                    session.createTables();session.beginTransaction();Object child=type.newInstance(),grandchild=type.newInstance();type.getField("child").set(child,grandchild);session.persist(child);session.commitTransaction();org.junit.Assert.assertEquals(2,session.query(type).count());
                    session.beginTransaction();Object owner=type.newInstance();type.getField("child").set(owner,child);session.persist(owner);session.remove(owner);assertFalse(session.contains(owner));session.commitTransaction();session.clear();org.junit.Assert.assertEquals(0,session.query(type).count());
                    session.beginTransaction();Object fresh=type.newInstance(),freshChild=type.newInstance();type.getField("child").set(fresh,freshChild);session.persist(fresh);session.remove(fresh);session.commitTransaction();org.junit.Assert.assertEquals(0,session.query(type).count());
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void bulkAssignmentsRequireMatchingDomainEncodings() throws Exception {
        File classes=tmp.newFolder("assignmentdomains");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        String imports="package assignmentdomains; import com.codename1.annotations.*; import com.codename1.annotations.db.*; ";
        sources.put("assignmentdomains.State","package assignmentdomains; public enum State { OPEN, CLOSED }");
        sources.put("assignmentdomains.Color","package assignmentdomains; public enum Color { RED, BLUE }");
        for(String converter:Arrays.asList("First","Second")) sources.put("assignmentdomains."+converter,"package assignmentdomains; public class "+converter+" implements com.codename1.orm.session.AttributeConverter<String,String> { public String toDatabase(String value) { return value==null?null:\""+converter+":\"+value; } public String fromDatabase(String value) { return value==null?null:value.substring("+(converter.length()+1)+"); } }");
        sources.put("assignmentdomains.Entry",imports+"@Entity public class Entry { @Id public long id; public State state=State.OPEN,otherState=State.CLOSED; public Color color=Color.RED; public String text=\"raw\"; @Convert(converter=First.class) public String first=\"a\",same=\"b\"; @Convert(converter=Second.class) public String second=\"c\"; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class type=loader.loadClass("assignmentdomains.Entry");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());
                com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try {
                    session.createTables();session.beginTransaction();Object entity=type.newInstance();session.persist(entity);session.commitTransaction();Object id=type.getField("id").get(entity);session.beginTransaction();
                    for(String assignment:Arrays.asList("state=e.color","state=e.text","text=e.state","first=e.second","first=e.text","text=e.first","first=coalesce(e.same,e.second)","state=(select max(i.color) from Entry i)")) {
                        try { session.createQuery("update Entry e set e."+assignment);fail("Accepted incompatible assignment: "+assignment); } catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("mapping")); }
                    }
                    org.junit.Assert.assertEquals(1,session.createQuery("update Entry e set e.state=e.otherState,e.first=coalesce(e.same,e.first)").executeUpdate());session.commitTransaction();session.clear();
                    Object loaded=session.find(type,id);org.junit.Assert.assertEquals("CLOSED",type.getField("state").get(loaded).toString());org.junit.Assert.assertEquals("b",type.getField("first").get(loaded));org.junit.Assert.assertEquals("c",type.getField("second").get(loaded));
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void embeddedIdentifiersRejectConvertersAtAnyDepth() throws Exception {
        for(boolean nested:Arrays.asList(false,true)) for(boolean backend:Arrays.asList(false,true)) {
            File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
            String imports="package convertedid; import com.codename1.annotations.*; import com.codename1.annotations.db.*; ";
            sources.put("convertedid.Prefix","package convertedid; public class Prefix implements com.codename1.orm.session.AttributeConverter<String,String> { public String toDatabase(String value) { return value; } public String fromDatabase(String value) { return value; } }");
            sources.put("convertedid.Part",imports+"@Embeddable public class Part { @Convert(converter=Prefix.class) public String value; }");
            sources.put("convertedid.Key",imports+"@Embeddable public class Key { "+(nested?"@Embedded public Part part;":"@Convert(converter=Prefix.class) public String value;")+" }");
            sources.put("convertedid.Entry",imports+"@Entity public class Entry { @EmbeddedId public Key key; }");
            JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext ctx=runProcessor(classes,backend?backendClasspath():Collections.<String>emptyList());
            assertTrue(ctx.hasErrors());assertTrue(ctx.getErrors().toString(),ctx.getErrors().toString().contains("Identifier and version fields cannot declare converters"));
        }
    }

    @Test
    public void owningCollectionsCanSwapAndMoveChildrenInOneFlush() throws Exception {
        for(boolean ordered:Arrays.asList(false,true)) {
            File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();String imports="package movedlinks; import com.codename1.annotations.*; import com.codename1.annotations.db.*; ";
            sources.put("movedlinks.Child",imports+"@Entity public class Child { @Id public long id; }");
            sources.put("movedlinks.Owner",imports+"@Entity public class Owner { @Id public long id; @OneToMany(cascade=CascadeType.PERSIST) "+(ordered?"@OrderColumn(name=\"position\") ":"")+"public java.util.List<Child> children=new java.util.ArrayList<Child>(); }");
            JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
            try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
                Class ownerType=loader.loadClass("movedlinks.Owner"),childType=loader.loadClass("movedlinks.Child");
                for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                    Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();for(Class type:Arrays.asList(ownerType,childType)) models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());
                    com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                    try {
                        session.createTables();session.beginTransaction();Object a=ownerType.newInstance(),b=ownerType.newInstance(),x=childType.newInstance(),y=childType.newInstance();
                        java.util.List left=(java.util.List)ownerType.getField("children").get(a),right=(java.util.List)ownerType.getField("children").get(b);left.add(x);right.add(y);session.persist(a);session.persist(b);session.commitTransaction();
                        Object aId=ownerType.getField("id").get(a),bId=ownerType.getField("id").get(b),xId=childType.getField("id").get(x),yId=childType.getField("id").get(y);
                        session.beginTransaction();left.clear();right.clear();left.add(y);right.add(x);session.commitTransaction();session.clear();
                        a=session.find(ownerType,aId);b=session.find(ownerType,bId);session.initialize(a,"children");session.initialize(b,"children");left=(java.util.List)ownerType.getField("children").get(a);right=(java.util.List)ownerType.getField("children").get(b);
                        org.junit.Assert.assertEquals(yId,childType.getField("id").get(left.get(0)));org.junit.Assert.assertEquals(xId,childType.getField("id").get(right.get(0)));
                        session.beginTransaction();left.addAll(right);right.clear();session.commitTransaction();session.clear();a=session.find(ownerType,aId);b=session.find(ownerType,bId);session.initialize(a,"children");session.initialize(b,"children");left=(java.util.List)ownerType.getField("children").get(a);right=(java.util.List)ownerType.getField("children").get(b);org.junit.Assert.assertEquals(2,left.size());assertTrue(right.isEmpty());org.junit.Assert.assertEquals(2,session.query(childType).count());
                        if(ordered) { org.junit.Assert.assertEquals(yId,childType.getField("id").get(left.get(0)));org.junit.Assert.assertEquals(xId,childType.getField("id").get(left.get(1))); }
                    } finally { session.close();db.close(); }
                }
            }
        }
    }

    @Test
    public void persistRevivesEntireRemovedAggregateIncludingCycles() throws Exception {
        File classes=tmp.newFolder("revived");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();String imports="package revived; import com.codename1.annotations.*; import com.codename1.annotations.db.*; ";
        sources.put("revived.Node",imports+"@Entity public class Node { @Id public long id; @OneToMany(cascade=CascadeType.ALL) public java.util.List<Node> children=new java.util.ArrayList<Node>(); }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class type=loader.loadClass("revived.Node");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());
                com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try {
                    session.createTables();session.beginTransaction();java.util.List<Object> nodes=new java.util.ArrayList<Object>();for(int i=0;i<5;i++) nodes.add(type.newInstance());
                    for(int i=0;i<nodes.size();i++) ((java.util.List)type.getField("children").get(nodes.get(i))).add(nodes.get((i+1)%nodes.size()));session.persist(nodes.get(0));session.commitTransaction();
                    for(int round=0;round<3;round++) { session.beginTransaction();session.remove(nodes.get(0));session.persist(nodes.get(0));session.flush();session.commitTransaction();org.junit.Assert.assertEquals(5,session.query(type).count()); }
                    Object rootId=type.getField("id").get(nodes.get(0));session.clear();Object root=session.find(type,rootId),next=root;for(int i=0;i<5;i++) { session.initialize(next,"children");java.util.List children=(java.util.List)type.getField("children").get(next);org.junit.Assert.assertEquals(1,children.size());next=children.get(0); }org.junit.Assert.assertSame(root,next);
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void generatedStringConvertersAndForeignKeysUsePortableValues() throws Exception {
        File classes=tmp.newFolder("portablevalues");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("portablevalues.Prefix","package portablevalues; public class Prefix implements com.codename1.orm.session.AttributeConverter<String,String> { public String toDatabase(String value) { return value==null?null:\"x:\"+value; } public String fromDatabase(String value) { return value==null?null:value.substring(2); } }");
        sources.put("portablevalues.Key","package portablevalues; import com.codename1.annotations.*; @Entity(table=\"portable_key\") public class Key { @Id(autoIncrement=false) public String id; }");
        sources.put("portablevalues.Holder","package portablevalues; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"portable_holder\") public class Holder { @Id public long id; @ManyToOne public Key target; @Convert(converter=Prefix.class) public String code; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class keyType=loader.loadClass("portablevalues.Key"),holderType=loader.loadClass("portablevalues.Holder");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();
                for(Class type:Arrays.asList(keyType,holderType)) models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());
                com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try {
                    session.createTables();session.beginTransaction();Object holder=holderType.newInstance();holderType.getField("code").set(holder,"alpha");session.persist(holder);session.commitTransaction();
                    org.junit.Assert.assertSame(holder,session.query(holderType).like("code","al%").first());
                    String overlong=new String(new char[256]).replace('\0','a');db.execute("INSERT INTO portable_key (id) VALUES (?)",new Object[]{overlong});
                    Object key=keyType.newInstance();keyType.getField("id").set(key,overlong);Object invalid=holderType.newInstance();holderType.getField("target").set(invalid,key);session.beginTransaction();
                    try { session.persist(invalid);session.flush();fail("Foreign key must have the portable text bound"); } catch(com.codename1.orm.session.PersistenceException expected) { assertTrue(expected.getMessage().contains("255"));session.rollbackTransaction(); }
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void convertersAndUniqueIndexesPreserveDomainValues() throws Exception {
        File classes=tmp.newFolder("converted");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("converted.Code","package converted; public class Code { public String value; public Code(String value) { this.value=value; } }");
        sources.put("converted.CodeConverter","package converted; public class CodeConverter implements com.codename1.orm.session.AttributeConverter<Code,String> { public String toDatabase(Code c) { return c==null?null:c.value; } public Code fromDatabase(String value) { return value==null?null:new Code(value); } }");
        sources.put("converted.Entry","package converted; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(indexes=@Index(name=\"converted_code\",fields=\"code\",unique=true)) public class Entry { @Id public long id; @Convert(converter=CodeConverter.class) public Code code; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());
        ProcessorContext ctx=runProcessor(classes,backendClasspath());assertFalse(ctx.getErrors().toString(),ctx.hasErrors());
        java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader());
        loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();Class type=loader.loadClass("converted.Entry"),codeType=loader.loadClass("converted.Code");
        com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));
        com.codename1.orm.session.Session s=em.openSession();
        try {
            s.createTables();s.validateSchema();s.beginTransaction();Object entity=type.newInstance(),code=codeType.getConstructor(String.class).newInstance("ABC");type.getField("code").set(entity,code);s.persist(entity);s.commitTransaction();
            Object id=type.getField("id").get(entity);s.clear();Object loaded=s.query(type).eq("code",code).first();
            org.junit.Assert.assertEquals("ABC",codeType.getField("value").get(type.getField("code").get(loaded)));
            s.beginTransaction();codeType.getField("value").set(type.getField("code").get(loaded),"DEF");s.commitTransaction();s.clear();
            org.junit.Assert.assertEquals("DEF",codeType.getField("value").get(type.getField("code").get(s.find(type,id))));
            Object included=codeType.getConstructor(String.class).newInstance("DEF"),excluded=codeType.getConstructor(String.class).newInstance("missing");
            org.junit.Assert.assertSame(s.find(type,id),s.query(type).in("code",excluded,included).first());
            org.junit.Assert.assertSame(s.find(type,id),s.query(type).ge("code",included).first());
            s.beginTransaction();org.junit.Assert.assertEquals(1,s.createQuery("update converted.Entry e set e.code=:code").setParameter("code",included).executeUpdate());s.commitTransaction();
            Object projected=s.createQuery("select e.code from converted.Entry e",codeType).first();org.junit.Assert.assertEquals("DEF",codeType.getField("value").get(projected));
            org.junit.Assert.assertSame(s.find(type,id),s.createQuery("select e from converted.Entry e where e.code in (:first, :second)",type).setParameter("first",excluded).setParameter("second",included).first());
            org.junit.Assert.assertSame(s.find(type,id),s.createQuery("select e from converted.Entry e where e.code in (:first, :second)",type).setParameter("first",included).setParameter("second",excluded).first());
            org.junit.Assert.assertNull(s.createQuery("select e from converted.Entry e where e.code not in (:first, :second)",type).setParameter("first",excluded).setParameter("second",included).first());
            try { s.createQuery("select e from converted.Entry e where e.code in ('missing', :second)",type).setParameter("second",included).first();fail("A string literal is not a Code domain value"); } catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("Converter input requires")); }
            org.junit.Assert.assertSame(s.find(type,id),s.createQuery("select e from converted.Entry e where e.code = coalesce((:code), e.code)",type).setParameter("code",included).first());
            org.junit.Assert.assertSame(s.find(type,id),s.createQuery("select e from converted.Entry e where coalesce(:code, e.code) = e.code",type).setParameter("code",null).first());
            s.beginTransaction();org.junit.Assert.assertEquals(1,s.createQuery("update converted.Entry e set e.code = coalesce(:code, e.code)").setParameter("code",included).executeUpdate());s.commitTransaction();
            Object low=codeType.getConstructor(String.class).newInstance("D"),high=codeType.getConstructor(String.class).newInstance("Z");
            org.junit.Assert.assertSame(s.find(type,id),s.createQuery("select e from converted.Entry e where e.code between :low and :high",type).setParameter("low",low).setParameter("high",high).first());
            org.junit.Assert.assertNull(s.createQuery("select e from converted.Entry e where e.code not between :low and :high",type).setParameter("low",low).setParameter("high",high).first());
            org.junit.Assert.assertNull(s.createQuery("select e from converted.Entry e where e.code between :low and :high",type).setParameter("low",codeType.getConstructor(String.class).newInstance("X")).setParameter("high",high).first());
            Object pattern=codeType.getConstructor(String.class).newInstance("D%");
            org.junit.Assert.assertSame(s.find(type,id),s.createQuery("select e from converted.Entry e where e.code like :pattern",type).setParameter("pattern",pattern).first());
            org.junit.Assert.assertNull(s.createQuery("select e from converted.Entry e where e.code not like :pattern",type).setParameter("pattern",pattern).first());
            org.junit.Assert.assertSame(s.find(type,id),s.createQuery("select e from converted.Entry e where e.code like :pattern escape :escape",type).setParameter("pattern",pattern).setParameter("escape","!").first());
            org.junit.Assert.assertNull(s.createQuery("select e from converted.Entry e where e.code like :pattern",type).setParameter("pattern",null).first());
            s.beginTransaction();Object replacement=codeType.getConstructor(String.class).newInstance("BULK");
            org.junit.Assert.assertEquals(1,s.createQuery("update converted.Entry e set e.code = :code where e.id = :id").setParameter("code",replacement).setParameter("id",id).executeUpdate());s.commitTransaction();
            org.junit.Assert.assertEquals("BULK",codeType.getField("value").get(type.getField("code").get(s.find(type,id))));
            s.beginTransaction();org.junit.Assert.assertEquals(1,s.createQuery("update converted.Entry e set e.code = :code where e.id = :id").setParameter("code",null).setParameter("id",id).executeUpdate());s.commitTransaction();
            org.junit.Assert.assertNull(type.getField("code").get(s.find(type,id)));
            s.beginTransaction();s.createQuery("update converted.Entry e set e.code = :code").setParameter("code",codeType.getConstructor(String.class).newInstance("DEF")).executeUpdate();s.commitTransaction();
            s.beginTransaction();Object duplicate=type.newInstance();type.getField("code").set(duplicate,codeType.getConstructor(String.class).newInstance("DEF"));s.persist(duplicate);
            try { s.commitTransaction();fail("Unique index must reject duplicate domain values"); } catch(com.codename1.orm.session.PersistenceException expected) { s.rollbackTransaction(); }
        } finally { s.close();em.close();loader.close(); }
    }

    @Test
    public void scalarDomainProjectionsAndJoinedEmbeddedPathsWorkForBothModels() throws Exception {
        File classes=tmp.newFolder("scalarprojections");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("scalarprojections.State","package scalarprojections; public enum State { ACTIVE, INACTIVE }");
        sources.put("scalarprojections.Prefix","package scalarprojections; public class Prefix implements com.codename1.orm.session.AttributeConverter<String,String> { public String toDatabase(String value) { return value==null?null:\"db:\"+value; } public String fromDatabase(String value) { return value==null?null:value.substring(3); } }");
        sources.put("scalarprojections.Address","package scalarprojections; import com.codename1.annotations.db.*; @Embeddable public class Address { @Convert(converter=Prefix.class) public String city; }");
        sources.put("scalarprojections.Owner","package scalarprojections; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"projection_owners\") @Inheritance public abstract class Owner { @Id public long id; public boolean active; public Boolean optional; public char letter; public java.util.Date moment; public State status; @Embedded public Address address=new Address(); }");
        sources.put("scalarprojections.Person","package scalarprojections; import com.codename1.annotations.*; @Entity public class Person extends Owner { public String name; }");
        sources.put("scalarprojections.Ticket","package scalarprojections; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"projection_tickets\") public class Ticket { @Id public long id; @ManyToOne(cascade=CascadeType.PERSIST) public Owner owner; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class ownerType=loader.loadClass("scalarprojections.Owner"),personType=loader.loadClass("scalarprojections.Person"),ticketType=loader.loadClass("scalarprojections.Ticket"),stateType=loader.loadClass("scalarprojections.State");Object active=Enum.valueOf(stateType,"ACTIVE");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();for(Class type:Arrays.asList(ownerType,personType,ticketType)) models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());
                com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try {
                    session.createTables();Object owner=personType.newInstance();ownerType.getField("active").set(owner,true);ownerType.getField("letter").set(owner,'Q');ownerType.getField("moment").set(owner,new java.util.Date(1234));ownerType.getField("status").set(owner,active);Object address=ownerType.getField("address").get(owner);address.getClass().getField("city").set(address,"Paris");Object ticket=ticketType.newInstance();ticketType.getField("owner").set(ticket,owner);session.beginTransaction();session.persist(ticket);session.commitTransaction();
                    org.junit.Assert.assertEquals(Boolean.TRUE,session.createQuery("select o.active from scalarprojections.Owner o",Boolean.class).first());
                    org.junit.Assert.assertNull(session.createQuery("select o.optional from scalarprojections.Owner o",Boolean.class).first());
                    org.junit.Assert.assertEquals(Character.valueOf('Q'),session.createQuery("select o.letter from scalarprojections.Owner o",Character.class).first());
                    org.junit.Assert.assertEquals(new java.util.Date(1234),session.createQuery("select o.moment from scalarprojections.Owner o",java.util.Date.class).first());
                    org.junit.Assert.assertEquals(active,session.createQuery("select o.status from scalarprojections.Owner o",stateType).first());
                    Object[] row=(Object[])session.createQuery("select o.active,o.status,o.moment,o.letter from scalarprojections.Owner o").first();org.junit.Assert.assertArrayEquals(new Object[]{true,active,new java.util.Date(1234),'Q'},row);
                    org.junit.Assert.assertEquals(1,session.createQuery("select o from scalarprojections.Owner o where o.active and NOT false").list().size());
                    org.junit.Assert.assertSame(ticket,session.query(ticketType).eq("owner.address.city","Paris").orderBy("owner.address.city",true).first());
                    org.junit.Assert.assertSame(ticket,session.query(ticketType).join("owner").like("owner.address.city","Pa%").first());
                    org.junit.Assert.assertEquals("Paris",session.createQuery("select t.owner.address.city from scalarprojections.Ticket t where t.owner.address.city=:city",String.class).setParameter("city","Paris").first());
                    for(String query:Arrays.asList("select t.owner from scalarprojections.Ticket t","select (t.owner) from scalarprojections.Ticket t","select coalesce(t.owner,t.owner) from scalarprojections.Ticket t","select o from scalarprojections.Ticket t join t.owner o")) {
                        org.junit.Assert.assertThrows(IllegalArgumentException.class,()->session.createQuery(query));
                    }
                    org.junit.Assert.assertEquals(Long.valueOf(1),session.createQuery("select count(t.owner) from scalarprojections.Ticket t",Long.class).first());
                    org.junit.Assert.assertEquals(Boolean.TRUE,session.createQuery("select t.owner is not null from scalarprojections.Ticket t",Boolean.class).first());
                    org.junit.Assert.assertEquals(ownerType.getField("id").get(owner),session.createQuery("select t.owner.id from scalarprojections.Ticket t",Long.class).first());
                    org.junit.Assert.assertEquals("Paris",session.createQuery("select o.address.city from scalarprojections.Ticket t join t.owner o",String.class).first());
                    org.junit.Assert.assertEquals(active,session.createQuery("select (select max(o.status) from scalarprojections.Owner o) from scalarprojections.Ticket t",stateType).first());
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void inheritanceRejectsReservedDiscriminatorFieldNames() throws Exception {
        for(boolean onRoot:Arrays.asList(false,true)) {
            Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
            sources.put("reservedtag.Base","package reservedtag; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity @Inheritance public abstract class Base { @Id public long id; "+(onRoot?"public String __cn1_discriminator;":"")+" }");
            sources.put("reservedtag.Child","package reservedtag; import com.codename1.annotations.*; @Entity public class Child extends Base { "+(onRoot?"":"public String __cn1_discriminator;")+" public String after; }");
            rejectsMappingForBothRuntimes(sources,"Reserved persistent field name: __cn1_discriminator");
        }
    }

    @Test
    public void partialCompositeForeignKeysAreRejectedForOptionalAndRequiredSubtypeRelations() throws Exception {
        File classes=tmp.newFolder("partialkeys");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("partialkeys.Target","package partialkeys; import com.codename1.annotations.*; @Entity(table=\"partial_targets\") public class Target { @Id(autoIncrement=false) public String first; @Id(autoIncrement=false) public String second; }");
        sources.put("partialkeys.OptionalOwner","package partialkeys; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"partial_optional\") public class OptionalOwner { @Id public long id; @ManyToOne public Target target; }");
        sources.put("partialkeys.Base","package partialkeys; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"partial_hierarchy\") @Inheritance public abstract class Base { @Id public long id; }");
        sources.put("partialkeys.RequiredOwner","package partialkeys; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class RequiredOwner extends Base { @ManyToOne(optional=false) public Target target; }");
        sources.put("partialkeys.Sibling","package partialkeys; import com.codename1.annotations.*; @Entity public class Sibling extends Base { public String name; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class target=loader.loadClass("partialkeys.Target");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();
                for(String name:Arrays.asList("Target","OptionalOwner","Base","RequiredOwner","Sibling")) { Class type=loader.loadClass("partialkeys."+name);models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance()); }
                com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try {
                    session.createTables();session.validateSchema();Object valid=target.newInstance();target.getField("first").set(valid,"a");target.getField("second").set(valid,"b");session.beginTransaction();session.persist(valid);session.commitTransaction();session.clear();
                    for(String name:Arrays.asList("OptionalOwner","RequiredOwner")) {
                        Class ownerType=loader.loadClass("partialkeys."+name);Object owner=ownerType.newInstance();ownerType.getField("target").set(owner,valid);session.beginTransaction();session.persist(owner);session.commitTransaction();Object id=ownerType.getField("id").get(owner);
                        for(int missing=0;missing<3;missing++) {
                            Object partial=target.newInstance();if(missing!=0 && missing!=2) target.getField("first").set(partial,"a");if(missing!=1 && missing!=2) target.getField("second").set(partial,"b");
                            for(boolean update:Arrays.asList(false,true)) {
                                session.clear();Object changed=update?session.find(ownerType,id):ownerType.newInstance();ownerType.getField("target").set(changed,partial);session.beginTransaction();
                                try { if(!update) session.persist(changed);session.flush();fail("Partial relationship key must fail before writing"); }
                                catch(com.codename1.orm.session.PersistenceException expected) { assertTrue(expected.getMessage().contains("Incomplete relationship identifier"));session.rollbackTransaction(); }
                            }
                        }
                        session.clear();org.junit.Assert.assertNotNull(session.find(ownerType,id));
                    }
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void subtypeQueriesRejectSiblingOnlyPathsWhileHydratingTheSharedTable() throws Exception {
        File classes=tmp.newFolder("querysiblings");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();String imports="package querysiblings; import com.codename1.annotations.*; import com.codename1.annotations.db.*; ";
        sources.put("querysiblings.Base",imports+"@Entity(table=\"query_siblings\") @Inheritance public abstract class Base { @Id public long id; public String common; }");
        sources.put("querysiblings.A",imports+"@Entity public class A extends Base { public long aOnly; @ManyToOne public Target aRelation; @ElementCollection public java.util.List<String> tags=new java.util.ArrayList<String>(); }");
        sources.put("querysiblings.B",imports+"@Entity public class B extends Base { public long bOnly; }");
        sources.put("querysiblings.Target",imports+"@Entity public class Target { @Id public long id; }");
        sources.put("querysiblings.Holder",imports+"@Entity public class Holder { @Id public long id; @ManyToOne public B member; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class base=loader.loadClass("querysiblings.Base"),a=loader.loadClass("querysiblings.A"),b=loader.loadClass("querysiblings.B"),holder=loader.loadClass("querysiblings.Holder");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();for(String name:Arrays.asList("Base","A","B","Target","Holder")) { Class type=loader.loadClass("querysiblings."+name);models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance()); }
                com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try {
                    session.createTables();session.beginTransaction();Object first=a.newInstance(),second=b.newInstance();a.getField("aOnly").setLong(first,5);b.getField("bOnly").setLong(second,9);base.getField("common").set(second,"shared");session.persist(first);session.persist(second);session.commitTransaction();Object id=base.getField("id").get(second);session.clear();
                    org.junit.Assert.assertEquals(2,session.query(base).list().size());org.junit.Assert.assertEquals(9,b.getField("bOnly").getLong(session.find(b,id)));
                    org.junit.Assert.assertEquals(1,session.query(b).eq("common","shared").eq("bOnly",9L).list().size());
                    org.junit.Assert.assertThrows(IllegalArgumentException.class,()->session.query(b).eq("aOnly",5L));
                    org.junit.Assert.assertThrows(IllegalArgumentException.class,()->session.query(b).orderBy("aOnly",true));
                    org.junit.Assert.assertThrows(IllegalArgumentException.class,()->session.query(b).join("aRelation"));
                    org.junit.Assert.assertThrows(IllegalArgumentException.class,()->session.query(b).fetch("aRelation"));
                    org.junit.Assert.assertThrows(IllegalArgumentException.class,()->session.query(b).containsElement("tags","x"));
                    org.junit.Assert.assertThrows(IllegalArgumentException.class,()->session.query(holder).eq("member.aOnly",5L));
                    for(String query:Arrays.asList("select b.aOnly from querysiblings.B b","select b from querysiblings.B b join fetch b.aRelation","update querysiblings.B b set b.aOnly=7")) org.junit.Assert.assertThrows(IllegalArgumentException.class,()->session.createQuery(query));
                    session.beginTransaction();org.junit.Assert.assertThrows(IllegalArgumentException.class,()->session.increment(b,id,"aOnly",1));org.junit.Assert.assertEquals(1,session.createQuery("update querysiblings.B b set b.bOnly=10").executeUpdate());session.commitTransaction();
                    org.junit.Assert.assertEquals(Long.valueOf(5),session.createQuery("select a.aOnly from querysiblings.A a",Long.class).first());org.junit.Assert.assertEquals(10,b.getField("bOnly").getLong(session.find(b,id)));
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void primitiveBulkNullsAreRejectedForBothGeneratedModels() throws Exception {
        File classes=tmp.newFolder("primitivebulk");
        Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("primitivebulk.Entry","package primitivebulk; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"primitive_bulk\") @Inheritance public class Entry { @Id public long id; public long counter; public int number; public boolean flag; public Double optional; public Long boxed; public String text; @Embedded public Details details; }");
        sources.put("primitivebulk.Details","package primitivebulk; import com.codename1.annotations.db.*; @Embeddable public class Details { public long value; }");
        sources.put("primitivebulk.Child","package primitivebulk; import com.codename1.annotations.*; @Entity public class Child extends Entry { public long childCounter; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class type=loader.loadClass("primitivebulk.Entry");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());models.put("primitivebulk.Child",(com.codename1.impl.orm.EntityModel)loader.loadClass("primitivebulk.Child"+suffix).newInstance());
                com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try {
                    session.createTables();session.beginTransaction();Object entity=type.newInstance();session.persist(entity);session.commitTransaction();
                    for(String field:Arrays.asList("counter","number","flag")) {
                        org.junit.Assert.assertThrows(IllegalArgumentException.class,()->session.createQuery("update primitivebulk.Entry e set e."+field+"=NULL"));
                        session.beginTransaction();org.junit.Assert.assertThrows(IllegalArgumentException.class,()->session.createQuery("update primitivebulk.Entry e set e."+field+"=:value").setParameter("value",null).executeUpdate());session.rollbackTransaction();
                    }
                    for(String expression:Arrays.asList("e.counter/0","e.counter%0","nullif(e.counter,e.counter)","e.boxed+1","length(e.text)","(select max(i.counter) from primitivebulk.Entry i where i.id<0)","coalesce(e.boxed,nullif(e.counter,e.counter))","e.details.value","e.childCounter","(select count(i.id) from primitivebulk.Entry i having count(i.id)<0)")) {
                        org.junit.Assert.assertThrows(expression,IllegalArgumentException.class,()->session.createQuery("update primitivebulk.Entry e set e.counter="+expression));
                    }
                    session.beginTransaction();
                    org.junit.Assert.assertEquals(1,session.createQuery("update primitivebulk.Entry e set e.counter=coalesce(e.counter/0,0)+:delta,e.optional=NULL").setParameter("delta",1L).executeUpdate());
                    org.junit.Assert.assertThrows(IllegalArgumentException.class,()->session.createQuery("update primitivebulk.Entry e set e.counter=e.counter+:delta").setParameter("delta",null).executeUpdate());
                    org.junit.Assert.assertEquals(1,session.createQuery("update primitivebulk.Entry e set e.counter=coalesce(:value,e.counter)").setParameter("value",null).executeUpdate());
                    org.junit.Assert.assertEquals(1,session.createQuery("update primitivebulk.Entry e set e.counter=coalesce((select max(i.counter) from primitivebulk.Entry i where i.id<0),e.counter)").executeUpdate());
                    session.commitTransaction();
                    org.junit.Assert.assertEquals(Long.valueOf(1),session.createQuery("select e.counter from primitivebulk.Entry e",Long.class).first());
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void entityMapKeysMatchUnconvertedDomainTypes() throws Exception {
        for(String[] types:new String[][]{{"String","int"},{"Integer","long"},{"String","java.util.Date"}}) {
            Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
            sources.put("badmap.Child","package badmap; import com.codename1.annotations.*; @Entity public class Child { @Id public long id; public "+types[1]+" code; }");
            sources.put("badmap.Owner","package badmap; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Owner { @Id public long id; @OneToMany @MapKey(name=\"code\") public java.util.Map<"+types[0]+",Child> values; }");
            rejectsMappingForBothRuntimes(sources,"MapKey generic must match target domain type");
        }
        File classes=tmp.newFolder("goodmap");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("goodmap.Code","package goodmap; public class Code { }");
        sources.put("goodmap.Converter","package goodmap; public class Converter implements com.codename1.orm.session.AttributeConverter<Code,String> { public String toDatabase(Code value) { return null; } public Code fromDatabase(String value) { return null; } }");
        sources.put("goodmap.Child","package goodmap; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Child { @Id public long id; public int number; @Convert(converter=Converter.class) public Code code; }");
        sources.put("goodmap.Owner","package goodmap; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Owner { @Id public long id; @OneToMany @MapKey(name=\"number\") public java.util.Map<Integer,Child> numbers; @OneToMany @MapKey(name=\"code\") public java.util.Map<Code,Child> codes; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
    }

    @Test
    public void generatedCompositeKeysRespectAggregatePortableWidth() throws Exception {
        File classes=tmp.newFolder("keywidth");Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        String imports="package keywidth; import com.codename1.annotations.*; import com.codename1.annotations.db.*; ";
        sources.put("keywidth.Wide",imports+"@Entity public class Wide { @Id(autoIncrement=false) public String a,b,c,d; }");
        sources.put("keywidth.Indexed",imports+"@Entity(indexes=@Index(fields={\"a\",\"b\",\"c\",\"d\"})) public class Indexed { @Id public long id; public String a,b,c,d; }");
        sources.put("keywidth.Joined",imports+"@Entity public class Joined { @Id(autoIncrement=false) public String a,b; @ManyToMany public java.util.Set<Joined> targets; }");
        sources.put("keywidth.Mapped",imports+"@Entity public class Mapped { @Id(autoIncrement=false) public String a,b,c; @ElementCollection public java.util.Map<String,String> values; }");
        sources.put("keywidth.Boundary",imports+"@Entity public class Boundary { @Id(autoIncrement=false) public String a,b,c; @Id(autoIncrement=false) public long number; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) for(String name:Arrays.asList("Wide","Indexed","Joined","Mapped","Boundary")) {
                Class type=loader.loadClass("keywidth."+name);Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());
                com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try {
                    if(name.equals("Boundary")) { session.createTables();session.validateSchema(); }
                    else { com.codename1.orm.session.PersistenceException error=org.junit.Assert.assertThrows(com.codename1.orm.session.PersistenceException.class,session::createTables);assertTrue(error.getMessage(),error.getMessage().contains("3072"));org.junit.Assert.assertEquals(0,db.query("SELECT name FROM sqlite_master WHERE type='table'",new Object[0]).size()); }
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void elementMapKeysHaveAPortableLengthLimit() throws Exception {
        File classes=tmp.newFolder("mapkeys");Map<String,String> sources=JavaSourceCompiler.singleSource("mapkeys.Owner","package mapkeys; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"mapkey_owner\") public class Owner { @Id public long id; @ElementCollection public java.util.Map<String,String> values=new java.util.LinkedHashMap<String,String>(); }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            Class type=loader.loadClass("mapkeys.Owner");
            for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();models.put(type.getName(),(com.codename1.impl.orm.EntityModel)loader.loadClass(type.getName()+suffix).newInstance());
                com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(new com.codename1.impl.orm.BackendSqlAccess(null,db,db.dialect()),models);
                try {
                    session.createTables();session.validateSchema();String boundary=new String(new char[255]).replace('\0','a');Object owner=type.newInstance();((Map)type.getField("values").get(owner)).put(boundary,"ok");session.beginTransaction();session.persist(owner);session.commitTransaction();Object id=type.getField("id").get(owner);
                    for(boolean update:Arrays.asList(false,true)) {
                        session.clear();Object changed=update?session.find(type,id):type.newInstance();if(update) session.initialize(changed,"values");((Map)type.getField("values").get(changed)).put(boundary+"b","invalid");session.beginTransaction();
                        try { if(!update) session.persist(changed);session.flush();fail("Overlong map key must be rejected"); } catch(com.codename1.orm.session.PersistenceException expected) { assertTrue(expected.getMessage().contains("255"));session.rollbackTransaction(); }
                    }
                    session.clear();Object loaded=session.find(type,id);session.initialize(loaded,"values");org.junit.Assert.assertEquals(java.util.Collections.singletonMap(boundary,"ok"),type.getField("values").get(loaded));
                } finally { session.close();db.close(); }
            }
        }
    }

    @Test
    public void compositeIdentifiersRoundTripThroughRelationsAndJoinTables() throws Exception {
        File classes=tmp.newFolder("composite");
        Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("composite.Key","package composite; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Embeddable public class Key { public String tenant; public long number; }");
        sources.put("composite.Account","package composite; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Account { @EmbeddedId public Key key; @Version public long version; public long counter; @OneToMany(mappedBy=\"account\",cascade=CascadeType.ALL,orphanRemoval=true) public java.util.List<Item> items=new java.util.ArrayList<Item>(); @ManyToMany(cascade=CascadeType.PERSIST) public java.util.Set<Label> labels=new java.util.LinkedHashSet<Label>(); }");
        sources.put("composite.Item","package composite; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Item { @Id public long id; @ManyToOne(fetch=FetchType.LAZY) public Account account; }");
        sources.put("composite.Label","package composite; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Label { @Id(autoIncrement=false) public String locale; @Id(autoIncrement=false) public long number; public String text; }");
        sources.put("composite.Reader","package composite; public class Reader { public static Account account(Item i) { return i.account; } public static java.util.Set<Label> labels(Account a) { return a.labels; } }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext ctx=runProcessor(classes,backendClasspath());assertFalse(ctx.getErrors().toString(),ctx.hasErrors());
        java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader());
        loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();
        Class accountType=loader.loadClass("composite.Account"),keyType=loader.loadClass("composite.Key"),itemType=loader.loadClass("composite.Item"),labelType=loader.loadClass("composite.Label"),reader=loader.loadClass("composite.Reader");
        com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));
        com.codename1.orm.session.Session session=em.openSession();
        try {
            Object key=keyType.newInstance();keyType.getField("tenant").set(key,"tenant-a");keyType.getField("number").setLong(key,7);
            Object account=accountType.newInstance();accountType.getField("key").set(account,key);
            Object item=itemType.newInstance();itemType.getField("account").set(item,account);((List)accountType.getField("items").get(account)).add(item);
            Object label=labelType.newInstance();labelType.getField("locale").set(label,"en");labelType.getField("number").setLong(label,3);labelType.getField("text").set(label,"gold");((Set)accountType.getField("labels").get(account)).add(label);
            session.createTables();session.beginTransaction();session.persist(account);session.commitTransaction();
            Object itemId=itemType.getField("id").get(item);session.clear();
            Object loadedItem=session.find(itemType,itemId),loaded=reader.getMethod("account",itemType).invoke(null,loadedItem);
            org.junit.Assert.assertSame(loaded,session.find(accountType,key));
            org.junit.Assert.assertEquals(1,session.count(loaded,"items"));org.junit.Assert.assertEquals(1,session.count(loaded,"labels"));
            Object loadedLabel=((Set)reader.getMethod("labels",accountType).invoke(null,loaded)).iterator().next();
            org.junit.Assert.assertSame(loadedLabel,session.find(labelType,com.codename1.orm.session.Identifier.of("en",3)));
            org.junit.Assert.assertEquals(1,session.query(accountType).join("labels").eq("labels.text","gold").count());
            org.junit.Assert.assertSame(loaded,session.query(accountType).join("items").eq("items.id",itemId).first());
            org.junit.Assert.assertSame(loaded,session.query(accountType).join("items").orderBy("key.tenant",true).orderBy("key.number",false).first());
            org.junit.Assert.assertSame(loaded,session.query(accountType).orderBy("key.tenant",true).join("items").first());
            try { session.query(accountType).join("items").orderBy("items.id",true).list();fail("Ordering a collection join by its target must still fail"); }
            catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("Collection joins require ordering by a root field")); }

            session.beginTransaction();assertTrue(session.increment(accountType,key,"counter",5));session.commitTransaction();
            org.junit.Assert.assertEquals(5,accountType.getField("counter").getLong(loaded));
            session.beginTransaction();session.remove(loaded);session.commitTransaction();
            org.junit.Assert.assertNull(session.find(accountType,key));org.junit.Assert.assertEquals(0,session.query(itemType).count());
            org.junit.Assert.assertEquals(1,session.query(labelType).count());
        } finally { session.close();em.close();loader.close(); }
    }

    @Test
    public void embeddedValuesAndMappedSuperclassRoundTrip() throws Exception {
        File classes=tmp.newFolder("embedded");
        java.util.Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("embedded.Base","package embedded; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @MappedSuperclass public class Base { @Id public long id; }");
        sources.put("embedded.Address","package embedded; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Embeddable public class Address { public String city; public int postalCode; }");
        sources.put("embedded.Contact","package embedded; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Contact extends Base { @Embedded public Address address; @Version public long version; @DbTransient public int callbacks; @PrePersist public void inserting() { callbacks++; } }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext ctx=runProcessor(classes,backendClasspath());assertFalse(ctx.getErrors().toString(),ctx.hasErrors());
        java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader());
        loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();
        Class type=loader.loadClass("embedded.Contact"),addressType=loader.loadClass("embedded.Address");
        com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));
        com.codename1.orm.session.Session session=em.openSession();
        try {
            session.createTables();session.beginTransaction();Object entity=type.newInstance();session.persist(entity);session.commitTransaction();
            org.junit.Assert.assertEquals(1,type.getField("callbacks").getInt(entity));
            Object id=type.getField("id").get(entity);session.clear();entity=session.find(type,id);org.junit.Assert.assertNull(type.getField("address").get(entity));
            session.beginTransaction();Object address=addressType.newInstance();addressType.getField("city").set(address,"Paris");addressType.getField("postalCode").setInt(address,75000);type.getField("address").set(entity,address);session.commitTransaction();
            session.clear();Object found=session.query(type).eq("address.city","Paris").first();
            org.junit.Assert.assertEquals(75000,addressType.getField("postalCode").getInt(type.getField("address").get(found)));
            session.beginTransaction();type.getField("address").set(found,null);session.commitTransaction();
            session.clear();org.junit.Assert.assertNull(type.getField("address").get(session.find(type,id)));
        } finally { session.close();em.close();loader.close(); }
    }

    @Test
    public void generatedRelationsLoadLazilyAndPreserveIdentity() throws Exception {
        File classes=tmp.newFolder("managed-relations");
        java.util.Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("managed.Parent","package managed; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Parent { @Id public long id; public String name; @OneToMany(mappedBy=\"parent\", cascade=CascadeType.ALL, orphanRemoval=true) public java.util.List<Child> children=new java.util.ArrayList<Child>(); }");
        sources.put("managed.Base","package managed; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @MappedSuperclass public class Base { @ManyToOne(fetch=FetchType.LAZY) public Parent parent; public Parent readParent() { return parent; } }");
        sources.put("managed.Child","package managed; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Child extends Base { @Id public long id; @Version public long version; public String name; }");
        sources.put("managed.ChildCn1Mapper","package managed; public class ChildCn1Mapper { public static Parent serialize(Child c) { return c.parent; } }");
        sources.put("managed.Reader","package managed; public class Reader { public static Parent parent(Child c) { return c.readParent(); } public static java.util.List<Child> children(Parent p) { return p.children; } }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext ctx=runProcessor(classes,backendClasspath());
        assertFalse("generated relations: "+ctx.getErrors(),ctx.hasErrors());
        // A second pass must not duplicate state fields or accessor methods.
        ctx=runProcessor(classes,backendClasspath());assertFalse(ctx.hasErrors());
        java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader());
        loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();
        Class parentType=loader.loadClass("managed.Parent"),childType=loader.loadClass("managed.Child"),reader=loader.loadClass("managed.Reader");
        com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));
        com.codename1.orm.session.Session session=em.openSession();
        try {
            session.createTables();session.beginTransaction();
            Object parent=parentType.newInstance(),child=childType.newInstance();
            parentType.getField("name").set(parent,"parent");childType.getField("name").set(child,"child");
            childType.getField("parent").set(child,parent);
            ((java.util.List)parentType.getField("children").get(parent)).add(child);
            session.persist(parent);session.commitTransaction();
            Object parentId=parentType.getField("id").get(parent),childId=childType.getField("id").get(child);
            session.clear();
            Object loadedChild=session.find(childType,childId);
            assertFalse(session.isLoaded(loadedChild,"parent"));
            try { loader.loadClass("managed.ChildCn1Mapper").getMethod("serialize",childType).invoke(null,loadedChild);fail("Serialization must not initialize an association"); }
            catch(java.lang.reflect.InvocationTargetException expected) { assertTrue(expected.getCause() instanceof com.codename1.orm.session.LazyInitializationException); }
            assertFalse(session.isLoaded(loadedChild,"parent"));
            Object loadedParent=reader.getMethod("parent",childType).invoke(null,loadedChild);
            assertTrue(session.isLoaded(loadedChild,"parent"));
            org.junit.Assert.assertSame(loadedParent,session.find(parentType,parentId));
            assertFalse(session.isLoaded(loadedParent,"children"));
            session.beginTransaction();Object pendingChild=childType.newInstance();childType.getField("parent").set(pendingChild,loadedParent);session.persist(pendingChild);
            java.util.List children=(java.util.List)reader.getMethod("children",parentType).invoke(null,loadedParent);
            org.junit.Assert.assertEquals(2,children.size());assertTrue(children.contains(loadedChild));assertTrue(children.contains(pendingChild));assertTrue(childType.getField("id").getLong(pendingChild)>0);
            session.commitTransaction();session.clear();loadedParent=session.find(parentType,parentId);session.beginTransaction();pendingChild=childType.newInstance();childType.getField("parent").set(pendingChild,loadedParent);session.persist(pendingChild);
            session.initialize(loadedParent,"children");children=(java.util.List)reader.getMethod("children",parentType).invoke(null,loadedParent);org.junit.Assert.assertEquals(3,children.size());assertTrue(children.contains(pendingChild));
            children.clear();session.commitTransaction();
            org.junit.Assert.assertEquals(0,session.query(childType).count());
            session.clear();Object detached=session.find(parentType,parentId);session.close();
            try { reader.getMethod("children",parentType).invoke(null,detached);fail("Detached lazy read must fail"); }
            catch(java.lang.reflect.InvocationTargetException expected) {
                assertTrue(expected.getCause() instanceof com.codename1.orm.session.LazyInitializationException);
            }
        } finally { session.close();em.close();loader.close(); }
    }


    private java.net.URLClassLoader cascadeReviewFixture() throws Exception {
        File classes=tmp.newFolder();
        Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("cascades.Parent","package cascades; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"cascade_parents\") public class Parent { @Id public long id; @ManyToOne(fetch=FetchType.LAZY,cascade={CascadeType.MERGE,CascadeType.REFRESH}) public Child selected; @OneToMany(cascade={CascadeType.MERGE,CascadeType.REFRESH}) public java.util.List<Child> children=new java.util.ArrayList<Child>(); @ElementCollection public java.util.List<String> tags=new java.util.ArrayList<String>(); @DbTransient public int pre,post; @PreUpdate public void before() { pre++; } @PostUpdate public void after() { post++; } }");
        sources.put("cascades.Child","package cascades; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"cascade_children\") public class Child { @Id public long id; public String name; @ManyToOne(fetch=FetchType.LAZY,cascade={CascadeType.MERGE,CascadeType.REFRESH}) public Parent parent; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());
        ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader());
        loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();
        return loader;
    }

    @Test
    public void managedMergeCascadesLoadedToOneAndToManyAndTerminatesCycles() throws Exception {
        try(java.net.URLClassLoader loader=cascadeReviewFixture()) {
            Class parentType=loader.loadClass("cascades.Parent"),childType=loader.loadClass("cascades.Child");
            com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));
            com.codename1.orm.session.Session s=em.openSession();
            try {
                s.createTables();s.beginTransaction();Object parent=parentType.newInstance(),a=childType.newInstance(),b=childType.newInstance();
                childType.getField("name").set(a,"a");childType.getField("name").set(b,"b");s.persist(a);s.persist(b);s.persist(parent);s.commitTransaction();
                Object parentId=parentType.getField("id").get(parent),aId=childType.getField("id").get(a),bId=childType.getField("id").get(b);
                s.detach(a);s.detach(b);childType.getField("name").set(a,"merged a");childType.getField("name").set(b,"merged b");
                childType.getField("parent").set(a,parent);childType.getField("parent").set(b,parent);
                parentType.getField("selected").set(parent,a);((List)parentType.getField("children").get(parent)).add(b);
                s.beginTransaction();org.junit.Assert.assertSame(parent,s.merge(parent));
                Object managedA=parentType.getField("selected").get(parent),managedB=((List)parentType.getField("children").get(parent)).get(0);
                assertTrue(s.contains(managedA));assertTrue(s.contains(managedB));
                org.junit.Assert.assertNotSame(a,managedA);org.junit.Assert.assertNotSame(b,managedB);
                org.junit.Assert.assertSame(parent,childType.getField("parent").get(managedA));
                s.commitTransaction();s.clear();
                org.junit.Assert.assertEquals("merged a",childType.getField("name").get(s.find(childType,aId)));
                org.junit.Assert.assertEquals("merged b",childType.getField("name").get(s.find(childType,bId)));
                parent=s.find(parentType,parentId);assertFalse(s.isLoaded(parent,"selected"));assertFalse(s.isLoaded(parent,"children"));
                s.beginTransaction();org.junit.Assert.assertSame(parent,s.merge(parent));
                assertFalse(s.isLoaded(parent,"selected"));assertFalse(s.isLoaded(parent,"children"));s.commitTransaction();
            } finally { s.close();em.close(); }
        }
    }

    @Test
    public void refreshCascadesThroughUnloadedToOneAndToManyWithoutFlushingTargets() throws Exception {
        try(java.net.URLClassLoader loader=cascadeReviewFixture()) {
            Class parentType=loader.loadClass("cascades.Parent"),childType=loader.loadClass("cascades.Child");
            com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));
            com.codename1.orm.session.Session s=em.openSession();
            try {
                s.createTables();s.beginTransaction();Object parent=parentType.newInstance(),a=childType.newInstance(),b=childType.newInstance();
                childType.getField("name").set(a,"a");childType.getField("name").set(b,"b");s.persist(a);s.persist(b);
                parentType.getField("selected").set(parent,a);((List)parentType.getField("children").get(parent)).add(b);s.persist(parent);s.commitTransaction();
                s.beginTransaction();childType.getField("parent").set(a,parent);childType.getField("parent").set(b,parent);s.commitTransaction();
                Object parentId=parentType.getField("id").get(parent),aId=childType.getField("id").get(a),bId=childType.getField("id").get(b);s.clear();
                parent=s.find(parentType,parentId);a=s.find(childType,aId);b=s.find(childType,bId);
                assertFalse(s.isLoaded(parent,"selected"));assertFalse(s.isLoaded(parent,"children"));
                s.beginTransaction();childType.getField("name").set(a,"discard a");childType.getField("name").set(b,"discard b");s.refresh(parent);
                org.junit.Assert.assertEquals("a",childType.getField("name").get(a));org.junit.Assert.assertEquals("b",childType.getField("name").get(b));
                org.junit.Assert.assertSame(a,parentType.getField("selected").get(parent));org.junit.Assert.assertSame(b,((List)parentType.getField("children").get(parent)).get(0));
                assertFalse(s.isLoaded(parent,"tags"));s.commitTransaction();s.clear();
                org.junit.Assert.assertEquals("a",childType.getField("name").get(s.find(childType,aId)));org.junit.Assert.assertEquals("b",childType.getField("name").get(s.find(childType,bId)));
            } finally { s.close();em.close(); }
        }
    }

    @Test
    public void collectionOnlyUpdatesPairLifecycleCallbacksOncePerFlush() throws Exception {
        try(java.net.URLClassLoader loader=cascadeReviewFixture()) {
            Class parentType=loader.loadClass("cascades.Parent"),childType=loader.loadClass("cascades.Child");
            com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));
            com.codename1.orm.session.Session s=em.openSession();
            try {
                s.createTables();s.beginTransaction();Object parent=parentType.newInstance(),child=childType.newInstance();s.persist(child);s.persist(parent);s.commitTransaction();
                Object id=parentType.getField("id").get(parent);
                s.beginTransaction();((List)parentType.getField("tags").get(parent)).add("tag");s.flush();
                org.junit.Assert.assertEquals(1,parentType.getField("pre").getInt(parent));org.junit.Assert.assertEquals(1,parentType.getField("post").getInt(parent));
                s.flush();org.junit.Assert.assertEquals(1,parentType.getField("post").getInt(parent));
                ((List)parentType.getField("children").get(parent)).add(child);s.flush();
                org.junit.Assert.assertEquals(2,parentType.getField("pre").getInt(parent));org.junit.Assert.assertEquals(2,parentType.getField("post").getInt(parent));
                parentType.getField("selected").set(parent,child);((List)parentType.getField("tags").get(parent)).add("second");s.flush();
                org.junit.Assert.assertEquals(3,parentType.getField("pre").getInt(parent));org.junit.Assert.assertEquals(3,parentType.getField("post").getInt(parent));
                s.commitTransaction();s.clear();parent=s.find(parentType,id);org.junit.Assert.assertEquals(2,s.count(parent,"tags"));org.junit.Assert.assertEquals(1,s.count(parent,"children"));
            } finally { s.close();em.close(); }
        }
    }

    @Test
    public void omittedAndEmptyIndexNamesGenerateWorkingIndexes() throws Exception {
        File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("derived.Entry","package derived; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"derived_entries\",indexes={@Index(fields=\"code\",unique=true),@Index(name=\"\",fields=\"label\")}) public class Entry { @Id public long id; public String code; public String label; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());
        ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();Class type=loader.loadClass("derived.Entry");
            com.codename1.backend.Database db=com.codename1.backend.Database.open(":memory:");
            com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(db);com.codename1.orm.session.Session s=em.openSession();
            try {
                s.createTables();List<Map<String,Object>> indexes=db.query("PRAGMA index_list(derived_entries)",new Object[0]);org.junit.Assert.assertEquals(2,indexes.size());
                s.beginTransaction();Object a=type.newInstance();type.getField("code").set(a,"same");s.persist(a);s.commitTransaction();
                s.beginTransaction();Object b=type.newInstance();type.getField("code").set(b,"same");s.persist(b);
                try { s.commitTransaction();fail("Derived unique index must reject duplicate values"); } catch(com.codename1.orm.session.PersistenceException expected) { s.rollbackTransaction(); }
            } finally { s.close();em.close(); }
        }
    }


    private void rejectsMappingForBothRuntimes(Map<String,String> sources,String message) throws Exception {
        for(boolean backend:new boolean[]{false,true}) {
            File classes=tmp.newFolder();JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
            ProcessorContext ctx=backend?runProcessor(classes,backendClasspath()):runProcessor(classes);
            assertTrue(ctx.getErrors().toString(),ctx.hasErrors());
            assertTrue(ctx.getErrors().toString(),ctx.getErrors().toString().contains(message));
        }
    }

    @Test
    public void generatorTableNameIsReservedForJoinAndElementTables() throws Exception {
        for(String collection:Arrays.asList("@ElementCollection public java.util.List<String> values=new java.util.ArrayList<String>();", "@ManyToMany public java.util.List<Target> values=new java.util.ArrayList<Target>();")) {
            Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
            sources.put("reserved.Owner","package reserved; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Owner { @Id @GeneratedValue(strategy=GenerationType.TABLE) public long id; @JoinTable(name=\"CN1_ORM_SEQUENCES\") "+collection+" }");
            sources.put("reserved.Target","package reserved; import com.codename1.annotations.*; @Entity public class Target { @Id public long id; }");
            rejectsMappingForBothRuntimes(sources,"cn1_orm_sequences is reserved");
        }
    }

    @Test
    public void siblingRelationshipDeclarationsCannotHideEachOther() throws Exception {
        Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("siblings.Root","package siblings; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity @Inheritance public abstract class Root { @Id public long id; }");
        sources.put("siblings.Target","package siblings; import com.codename1.annotations.*; @Entity public class Target { @Id public long id; }");
        for(String child:Arrays.asList("First","Second")) sources.put("siblings."+child,"package siblings; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class "+child+" extends Root { @ManyToMany public java.util.List<Target> links=new java.util.ArrayList<Target>(); }");
        rejectsMappingForBothRuntimes(sources,"Conflicting inherited relationship");
    }

    @Test
    public void hierarchyIdentifiersAndVersionsMustBeInheritedFromRoot() throws Exception {
        for(String field:Arrays.asList("@Id(autoIncrement=false) public long extraId;","@Version public long version;")) {
            Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
            sources.put("rootkeys.Root","package rootkeys; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity @Inheritance public abstract class Root { @Id(autoIncrement=false) public long id; }");
            sources.put("rootkeys.First","package rootkeys; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class First extends Root { "+field+" }");
            sources.put("rootkeys.Second","package rootkeys; import com.codename1.annotations.*; @Entity public class Second extends Root { }");
            rejectsMappingForBothRuntimes(sources,"must be declared on or inherited by the hierarchy root");
        }
    }

    @Test
    public void hierarchyRootMayInheritKeysAndRelationshipsFromMappedSuperclass() throws Exception {
        Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("rootbase.Base","package rootbase; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @MappedSuperclass public class Base { @Id public long id; @Version public long version; @ManyToMany public java.util.List<Target> links=new java.util.ArrayList<Target>(); }");
        sources.put("rootbase.Root","package rootbase; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity @Inheritance public abstract class Root extends Base { }");
        sources.put("rootbase.Target","package rootbase; import com.codename1.annotations.*; @Entity public class Target { @Id public long id; }");
        for(String child:Arrays.asList("First","Second")) sources.put("rootbase."+child,"package rootbase; import com.codename1.annotations.*; @Entity public class "+child+" extends Root { }");
        for(boolean backend:new boolean[]{false,true}) {
            File classes=tmp.newFolder();JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
            ProcessorContext ctx=backend?runProcessor(classes,backendClasspath()):runProcessor(classes);assertFalse(ctx.getErrors().toString(),ctx.hasErrors());
        }
    }

    @Test
    public void explicitIndexesCannotShareEntityCollectionOrGeneratorTableNames() throws Exception {
        for(String index:Arrays.asList("OWNERS","targets","owner_links","cn1_orm_sequences")) {
            Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
            sources.put("collision.Owner","package collision; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"owners\",indexes=@Index(name=\""+index+"\",fields=\"value\")) public class Owner { @Id @GeneratedValue(strategy=GenerationType.TABLE) public long id; public String value; @ManyToMany @JoinTable(name=\"owner_links\") public java.util.List<Target> links=new java.util.ArrayList<Target>(); }");
            sources.put("collision.Target","package collision; import com.codename1.annotations.*; @Entity(table=\"targets\") public class Target { @Id public long id; }");
            rejectsMappingForBothRuntimes(sources,"Index name conflicts with table");
        }
    }


    @Test
    public void explicitIndexNamesCannotDifferOnlyByCase() throws Exception {
        Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("caseindexes.Entry","package caseindexes; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(indexes={@Index(name=\"by_code\",fields=\"code\"),@Index(name=\"BY_CODE\",fields=\"label\")}) public class Entry { @Id public long id; public String code,label; }");
        rejectsMappingForBothRuntimes(sources,"Invalid or duplicate index name");
    }

    @Test
    public void relationshipFreeVersionedEntitiesHaveExclusiveSessionOwnership() throws Exception {
        File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("ownership.Versioned","package ownership; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"ownership_versioned\") public class Versioned { @Id public long id; @Version public long version; }");
        sources.put("ownership.Assigned","package ownership; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"ownership_assigned\") public class Assigned { @Id(autoIncrement=false) public String id; @Version public long version; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());
        ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        // A repeated enhancement must preserve exactly one state getter/setter.
        backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();Class versioned=loader.loadClass("ownership.Versioned"),assigned=loader.loadClass("ownership.Assigned");
            com.codename1.backend.orm.EntityManager first=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:")),second=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));
            com.codename1.orm.session.Session a=first.openSession(),b=second.openSession();
            try {
                a.createTables();b.createTables();Object entity=versioned.newInstance();a.beginTransaction();a.persist(entity);b.beginTransaction();
                try { b.persist(entity);fail("A pending version-only entity must belong to its original session"); }
                catch(com.codename1.orm.session.PersistenceException expected) { assertTrue(expected.getMessage().contains("already belongs to another session")); }
                assertTrue(a.contains(entity));assertFalse(b.contains(entity));b.rollbackTransaction();a.commitTransaction();
                org.junit.Assert.assertEquals(1,a.query(versioned).count());org.junit.Assert.assertEquals(0,b.query(versioned).count());
                for(int boundary=0;boundary<3;boundary++) {
                    Object keyed=assigned.newInstance();assigned.getField("id").set(keyed,"key"+boundary);a.beginTransaction();a.persist(keyed);b.beginTransaction();
                    try { b.persist(keyed);fail("Assigned ID must not bypass ownership"); }
                    catch(com.codename1.orm.session.PersistenceException expected) { assertTrue(expected.getMessage().contains("already belongs to another session")); }
                    b.rollbackTransaction();
                    if(boundary==0) { a.detach(keyed);a.commitTransaction(); }
                    else if(boundary==1) { a.clear();a.commitTransaction(); }
                    else a.close();
                    b.beginTransaction();b.persist(keyed);b.commitTransaction();assertTrue(b.contains(keyed));
                }
            } finally { a.close();b.close();first.close();second.close(); }
        }
    }


    @Test
    public void initialCollectionsDoNotTriggerUpdateCallbacksOrVersionIncrements() throws Exception {
        File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("insertstate.Owner","package insertstate; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"insertstate_owners\") public class Owner { @Id public long id; @Version public long version; @ElementCollection public java.util.List<String> tags=new java.util.ArrayList<String>(); @ManyToMany(cascade=CascadeType.PERSIST) public java.util.List<Child> children=new java.util.ArrayList<Child>(); @DbTransient public int prePersist,postPersist,preUpdate,postUpdate; @PrePersist public void beforeInsert() { prePersist++; } @PostPersist public void afterInsert() { postPersist++; } @PreUpdate public void beforeUpdate() { preUpdate++; } @PostUpdate public void afterUpdate() { postUpdate++; } }");
        sources.put("insertstate.Child","package insertstate; import com.codename1.annotations.*; @Entity(table=\"insertstate_children\") public class Child { @Id public long id; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext ctx=runProcessor(classes,backendClasspath());assertFalse(ctx.getErrors().toString(),ctx.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();Class type=loader.loadClass("insertstate.Owner"),child=loader.loadClass("insertstate.Child");
            com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));com.codename1.orm.session.Session session=em.openSession();
            try {
                session.createTables();session.beginTransaction();Object owner=type.newInstance();((List)type.getField("tags").get(owner)).add("initial");((List)type.getField("children").get(owner)).add(child.newInstance());session.persist(owner);session.flush();
                org.junit.Assert.assertEquals(0,type.getField("version").getLong(owner));org.junit.Assert.assertEquals(1,type.getField("prePersist").getInt(owner));org.junit.Assert.assertEquals(1,type.getField("postPersist").getInt(owner));
                org.junit.Assert.assertEquals(0,type.getField("preUpdate").getInt(owner));org.junit.Assert.assertEquals(0,type.getField("postUpdate").getInt(owner));
                session.flush();org.junit.Assert.assertEquals(0,type.getField("version").getLong(owner));
                ((List)type.getField("tags").get(owner)).add("updated");session.flush();org.junit.Assert.assertEquals(1,type.getField("version").getLong(owner));org.junit.Assert.assertEquals(1,type.getField("preUpdate").getInt(owner));org.junit.Assert.assertEquals(1,type.getField("postUpdate").getInt(owner));
                session.commitTransaction();Object id=type.getField("id").get(owner);session.clear();owner=session.find(type,id);org.junit.Assert.assertEquals(2,session.count(owner,"tags"));org.junit.Assert.assertEquals(1,session.count(owner,"children"));
            } finally { session.close();em.close(); }
        }
    }

    @Test
    public void cyclicAssignedAndGeneratedKeysCompleteWithoutUpdateEvents() throws Exception {
        for(boolean assigned:new boolean[]{true,false}) {
            File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
            sources.put("insertcycle.Node","package insertcycle; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"insertcycle_nodes\") public class Node { "+(assigned?"@Id(autoIncrement=false) public String id;":"@Id public long id;")+" @Version public long version; @ManyToOne(optional="+(!assigned)+",cascade=CascadeType.PERSIST) public Node next; @DbTransient public int inserts,updates; @PostPersist public void inserted() { inserts++; } @PostUpdate public void updated() { updates++; } }");
            JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext ctx=runProcessor(classes,backendClasspath());assertFalse(ctx.getErrors().toString(),ctx.hasErrors());
            try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
                loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();Class type=loader.loadClass("insertcycle.Node");
                com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));com.codename1.orm.session.Session session=em.openSession();
                try {
                    session.createTables();session.beginTransaction();Object a=type.newInstance(),b=type.newInstance();if(assigned) { type.getField("id").set(a,"a");type.getField("id").set(b,"b"); }type.getField("next").set(a,b);type.getField("next").set(b,a);session.persist(a);session.flush();
                    for(Object entity:Arrays.asList(a,b)) { org.junit.Assert.assertEquals(0,type.getField("version").getLong(entity));org.junit.Assert.assertEquals(1,type.getField("inserts").getInt(entity));org.junit.Assert.assertEquals(0,type.getField("updates").getInt(entity)); }
                    session.commitTransaction();Object id=type.getField("id").get(a);session.clear();a=session.find(type,id);session.initialize(a,"next");b=type.getField("next").get(a);org.junit.Assert.assertNotNull(b);session.initialize(b,"next");org.junit.Assert.assertSame(a,type.getField("next").get(b));
                    session.beginTransaction();session.remove(a);session.remove(b);session.commitTransaction();org.junit.Assert.assertEquals(0,session.query(type).count());
                } finally { session.close();em.close(); }
            }
        }
    }

    @Test
    public void mysqlRejectsRequiredCyclesBeforeAnyDatabaseOperation() throws Exception {
        for(String key:Arrays.asList("@Id(autoIncrement=false) public String id;","@Id @GeneratedValue(strategy=GenerationType.UUID) public String id;","@Id @GeneratedValue(strategy=GenerationType.SEQUENCE) public long id;","@Id @GeneratedValue(strategy=GenerationType.TABLE) public long id;")) {
            File classes=tmp.newFolder();JavaSourceCompiler.compile(JavaSourceCompiler.singleSource("mysqlcycle.Node","package mysqlcycle; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Node { "+key+" @ManyToOne(optional=false) public Node next; }"),classes,Arrays.asList(testClassesDir()));
            ProcessorContext ctx=runProcessor(classes,backendClasspath());assertFalse(ctx.getErrors().toString(),ctx.hasErrors());
            try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
                com.codename1.impl.orm.EntityModel model=(com.codename1.impl.orm.EntityModel)loader.loadClass("mysqlcycle.NodeCn1BackendModel").newInstance();
                Map<String,com.codename1.impl.orm.EntityModel<?>> models=new java.util.LinkedHashMap<String,com.codename1.impl.orm.EntityModel<?>>();models.put(model.type().getName(),model);
                for(String dialect:Arrays.asList("mysql","sqlite","postgresql")) {
                    com.codename1.impl.orm.SqlAccess access=(com.codename1.impl.orm.SqlAccess)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{com.codename1.impl.orm.SqlAccess.class},(proxy,method,args)-> {
                        if(method.getName().equals("dialect")) return dialect;
                        if(method.getName().equals("close")) return null;
                        throw new AssertionError("No database operation is allowed: "+method.getName());
                    });
                    try { new com.codename1.impl.orm.SessionImpl(access,models).close();if(dialect.equals("mysql")) fail("Immediate foreign keys cannot insert a required cycle"); }
                    catch(com.codename1.orm.session.PersistenceException expected) { assertTrue(dialect.equals("mysql"));assertTrue(expected.getMessage().contains("required relationship cycles")); }
                }
            }
        }
    }

    @Test
    public void fetchJoinsApplyInnerAndLeftSemanticsBeforePagination() throws Exception {
        try(java.net.URLClassLoader loader=cascadeReviewFixture()) {
            Class parentType=loader.loadClass("cascades.Parent"),childType=loader.loadClass("cascades.Child");
            com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));com.codename1.orm.session.Session session=em.openSession();
            try {
                session.createTables();session.beginTransaction();Object empty=parentType.newInstance(),populated=parentType.newInstance(),child=childType.newInstance(),second=childType.newInstance();session.persist(empty);session.flush();session.persist(child);session.persist(second);parentType.getField("selected").set(populated,child);((List)parentType.getField("children").get(populated)).addAll(Arrays.asList(child,second));((List)parentType.getField("tags").get(populated)).add("tag");session.persist(populated);session.commitTransaction();
                Object id=parentType.getField("id").get(populated);session.clear();
                for(String field:Arrays.asList("children","selected","tags")) {
                    List inner=session.createQuery("select distinct p from cascades.Parent p join fetch p."+field+" order by p.id",parentType).limit(1).list();org.junit.Assert.assertEquals(1,inner.size());org.junit.Assert.assertEquals(id,parentType.getField("id").get(inner.get(0)));assertTrue(session.isLoaded(inner.get(0),field));
                    List left=session.createQuery("select distinct p from cascades.Parent p left join fetch p."+field+" order by p.id",parentType).list();org.junit.Assert.assertEquals(2,left.size());org.junit.Assert.assertEquals(id,parentType.getField("id").get(left.get(1)));assertTrue(session.isLoaded(left.get(0),field));session.clear();
                }
            } finally { session.close();em.close(); }
        }
    }

    @Test
    public void hiddenMappedSuperclassFieldsAreRejectedForBothRuntimes() throws Exception {
        for(String child:Arrays.asList("@Column(name=\"child_code\") public String code;","@DbTransient public String code;","public static String code;")) {
            Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
            sources.put("hidden.Base","package hidden; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @MappedSuperclass public class Base { @Column(name=\"base_code\") public String code; }");
            sources.put("hidden.Child","package hidden; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Child extends Base { @Id public long id; "+child+" }");
            rejectsMappingForBothRuntimes(sources,"Hidden inherited persistent field");
        }
    }

    @Test
    public void subtypeRequirementsSurviveNullableSingleTableColumns() throws Exception {
        File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("requiredsub.Base","package requiredsub; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity @Inheritance public abstract class Base { @Id public long id; }");
        sources.put("requiredsub.Child","package requiredsub; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Child extends Base { @Column(nullable=false) public String code; @ManyToOne(optional=false,fetch=FetchType.LAZY,cascade=CascadeType.PERSIST) public Target target; }");
        sources.put("requiredsub.Sibling","package requiredsub; import com.codename1.annotations.*; @Entity public class Sibling extends Base { }");
        sources.put("requiredsub.Target","package requiredsub; import com.codename1.annotations.*; @Entity public class Target { @Id public Long id; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();Class childType=loader.loadClass("requiredsub.Child"),targetType=loader.loadClass("requiredsub.Target"),siblingType=loader.loadClass("requiredsub.Sibling");
            com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));com.codename1.orm.session.Session session=em.openSession();
            try {
                session.createTables();session.beginTransaction();session.persist(siblingType.newInstance());session.commitTransaction();
                for(String missing:Arrays.asList("code","target")) {
                    session.beginTransaction();Object child=childType.newInstance();if(!missing.equals("code")) childType.getField("code").set(child,"valid");if(!missing.equals("target")) childType.getField("target").set(child,targetType.newInstance());session.persist(child);
                    try { session.flush();fail("Missing required subtype field must fail on insert"); } catch(com.codename1.orm.session.PersistenceException expected) { assertTrue(expected.getMessage(),expected.getMessage().contains("Required"));session.rollbackTransaction(); }
                }
                session.beginTransaction();Object child=childType.newInstance();childType.getField("code").set(child,"valid");childType.getField("target").set(child,targetType.newInstance());session.persist(child);session.commitTransaction();Object id=childType.getField("id").get(child);session.clear();
                for(String missing:Arrays.asList("code","target")) {
                    child=session.find(childType,id);session.beginTransaction();childType.getField(missing).set(child,null);
                    // Reflection bypasses enhancement; initialize before assigning a relationship.
                    if(missing.equals("target")) { session.initialize(child,"target");childType.getField("target").set(child,null); }
                    try { session.flush();fail("Missing required subtype field must fail on update"); } catch(com.codename1.orm.session.PersistenceException expected) { assertTrue(expected.getMessage(),expected.getMessage().contains("Required"));session.rollbackTransaction(); }
                }
                child=session.find(childType,id);session.beginTransaction();childType.getField("code").set(child,"changed");session.commitTransaction();assertFalse(session.isLoaded(child,"target"));
                try { session.createQuery("update requiredsub.Child c set c.code = NULL");fail("Bulk NULL must reject a required subtype column"); } catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("required subtype")); }
                try { session.createQuery("update requiredsub.Base c set c.code = coalesce(NULL, NULL)");fail("Unknown-nullability bulk expressions must be rejected"); } catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("required subtype")); }
                session.beginTransaction();try { session.createQuery("update requiredsub.Child c set c.code = :value").setParameter("value",null).executeUpdate();fail("Null bulk parameters must be rejected"); } catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("required subtype")); }session.rollbackTransaction();
                session.beginTransaction();org.junit.Assert.assertEquals(1,session.createQuery("update requiredsub.Child c set c.code = :value").setParameter("value","bulk").executeUpdate());session.commitTransaction();org.junit.Assert.assertEquals("bulk",childType.getField("code").get(session.find(childType,id)));
            } finally { session.close();em.close(); }
        }
    }

    @Test
    public void compositeBinaryIdentifiersAreRejectedForBothRuntimes() throws Exception {
        Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("binarykey.Entry","package binarykey; import com.codename1.annotations.*; @Entity public class Entry { @Id(autoIncrement=false) public byte[] binary; @Id(autoIncrement=false) public long part; }");
        rejectsMappingForBothRuntimes(sources,"Composite identifiers cannot contain binary components");
    }

    @Test
    public void siblingOnlyCollectionsDoNotDirtyUnchangedEntities() throws Exception {
        File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("cleansibling.Base","package cleansibling; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"clean_sibling\") @Inheritance public abstract class Base { @Id public long id; @Version public long version; @DbTransient public int updates; @PostUpdate public void updated() { updates++; } }");
        sources.put("cleansibling.WithCollection","package cleansibling; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class WithCollection extends Base { @ElementCollection public java.util.List<String> tags=new java.util.ArrayList<String>(); }");
        sources.put("cleansibling.Empty","package cleansibling; import com.codename1.annotations.*; @Entity public class Empty extends Base { }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext ctx=runProcessor(classes,backendClasspath());assertFalse(ctx.getErrors().toString(),ctx.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();Class type=loader.loadClass("cleansibling.Empty");
            com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));com.codename1.orm.session.Session session=em.openSession();
            try { session.createTables();session.beginTransaction();Object entity=type.newInstance();session.persist(entity);session.commitTransaction();Object id=type.getField("id").get(entity);session.clear();entity=session.find(type,id);session.beginTransaction();session.query(type).list();session.flush();session.commitTransaction();org.junit.Assert.assertEquals(0,type.getField("version").getLong(entity));org.junit.Assert.assertEquals(0,type.getField("updates").getInt(entity));session.clear();org.junit.Assert.assertEquals(0,type.getField("version").getLong(session.find(type,id))); }
            finally { session.close();em.close(); }
        }
    }

    @Test
    public void inverseOneToOneFetchSnapshotsSupportOrphanRemoval() throws Exception {
        for(boolean eager:new boolean[]{false,true}) {
            File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
            sources.put("inverseorphan.Parent","package inverseorphan; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"inverse_orphan_parents\") public class Parent { @Id public long id; @OneToOne(mappedBy=\"parent\",fetch=FetchType."+(eager?"EAGER":"LAZY")+",orphanRemoval=true) public Child child; }");
            sources.put("inverseorphan.Child","package inverseorphan; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"inverse_orphan_children\") public class Child { @Id public long id; @OneToOne(fetch=FetchType.LAZY) public Parent parent; }");
            JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext ctx=runProcessor(classes,backendClasspath());assertFalse(ctx.getErrors().toString(),ctx.hasErrors());
            try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
                loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();Class parentType=loader.loadClass("inverseorphan.Parent"),childType=loader.loadClass("inverseorphan.Child");com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));com.codename1.orm.session.Session session=em.openSession();
                try { session.createTables();session.beginTransaction();Object parent=parentType.newInstance(),child=childType.newInstance();childType.getField("parent").set(child,parent);session.persist(parent);session.persist(child);session.commitTransaction();Object id=parentType.getField("id").get(parent);session.clear();parent=eager?session.query(parentType).first():session.createQuery("select p from inverseorphan.Parent p join fetch p.child",parentType).first();assertTrue(session.isLoaded(parent,"child"));org.junit.Assert.assertNotNull(parentType.getField("child").get(parent));session.beginTransaction();parentType.getField("child").set(parent,null);session.commitTransaction();org.junit.Assert.assertEquals(0,session.query(childType).count());org.junit.Assert.assertNotNull(session.find(parentType,id)); }
                finally { session.close();em.close(); }
            }
        }
    }

    @Test
    public void countersRejectNonIntLongDomainTypes() throws Exception {
        File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("countertypes.NumberConverter","package countertypes; public class NumberConverter implements com.codename1.orm.session.AttributeConverter<String,Long> { public Long toDatabase(String value) { return value==null?null:Long.valueOf(value); } public String fromDatabase(Long value) { return value==null?null:value.toString(); } }");
        sources.put("countertypes.Entry","package countertypes; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"counter_types\") public class Entry { @Id public long id; public int integer; public long whole; public Integer boxed=0; public Long boxedLong=0L; public byte narrow; public short small; public char letter; public boolean enabled; public java.util.Date moment; @Convert(converter=NumberConverter.class,storageType=Long.class) public String converted; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();Class type=loader.loadClass("countertypes.Entry");com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));com.codename1.orm.session.Session session=em.openSession();
            try { session.createTables();session.beginTransaction();Object entity=type.newInstance();session.persist(entity);session.commitTransaction();Object id=type.getField("id").get(entity);session.beginTransaction();
                for(String field:Arrays.asList("narrow","small","letter","enabled","moment","converted")) { try { session.increment(type,id,field,1);fail("Not a Java int/long counter: "+field); } catch(IllegalArgumentException expected) { } }
                for(String field:Arrays.asList("integer","whole","boxed","boxedLong")) assertTrue(session.increment(type,id,field,1));session.commitTransaction();
                for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) { com.codename1.impl.orm.EntityModel model=(com.codename1.impl.orm.EntityModel)loader.loadClass("countertypes.Entry"+suffix).newInstance();for(String field:Arrays.asList("narrow","small","letter","enabled","moment","converted")) assertFalse(model.counter(model.index(field))); }
            } finally { session.close();em.close(); }
        }
    }

    @Test
    public void hierarchyMembersShareSequenceAndTableGenerators() throws Exception {
        for(String strategy:Arrays.asList("SEQUENCE","TABLE")) for(String generator:Arrays.asList("","shared_ids")) {
            File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
            sources.put("sharedids.Base","package sharedids; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"shared_entities\") @Inheritance public abstract class Base { @Id @GeneratedValue(strategy=GenerationType."+strategy+",generator=\""+generator+"\") public long id; }");
            sources.put("sharedids.First","package sharedids; import com.codename1.annotations.*; @Entity public class First extends Base { }");
            sources.put("sharedids.Second","package sharedids; import com.codename1.annotations.*; @Entity public class Second extends Base { }");
            JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
            ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());
            ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
            try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
                for(String suffix:Arrays.asList("Cn1Model","Cn1BackendModel")) {
                    com.codename1.impl.orm.EntityModel root=(com.codename1.impl.orm.EntityModel)loader.loadClass("sharedids.Base"+suffix).newInstance();
                    for(String name:Arrays.asList("First","Second")) {
                        com.codename1.impl.orm.EntityModel child=(com.codename1.impl.orm.EntityModel)loader.loadClass("sharedids."+name+suffix).newInstance();
                        org.junit.Assert.assertEquals(root.generator(),child.generator());org.junit.Assert.assertEquals(root.generation(),child.generation());
                    }
                }
                loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();Class base=loader.loadClass("sharedids.Base"),first=loader.loadClass("sharedids.First"),second=loader.loadClass("sharedids.Second");
                com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));com.codename1.orm.session.Session session=em.openSession();
                try {
                    session.createTables();session.beginTransaction();Object a=first.newInstance(),b=second.newInstance();session.persist(a);session.persist(b);session.commitTransaction();
                    Object aid=base.getField("id").get(a),bid=base.getField("id").get(b);assertFalse(aid.equals(bid));session.clear();
                    org.junit.Assert.assertEquals(first,session.find(base,aid).getClass());org.junit.Assert.assertEquals(second,session.find(base,bid).getClass());org.junit.Assert.assertEquals(2,session.query(base).count());
                } finally { session.close();em.close(); }
            }
        }
    }

    @Test
    public void requiredIdentityCyclesAreRejectedBeforeSchemaCreation() throws Exception {
        for(String required:Arrays.asList("@ManyToOne(optional=false)","@ManyToOne @JoinColumn(nullable=false)","@OneToOne(optional=false)")) {
            for(int length:new int[]{1,2,3}) {
                Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
                for(int i=0;i<length;i++) sources.put("requiredcycle.Node"+i,"package requiredcycle; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Node"+i+" { @Id public long id; "+required+" public Node"+((i+1)%length)+" next; }");
                rejectsMappingForBothRuntimes(sources,"Required relationship cycle with identity-generated keys");
            }
        }
        // A required dependency chain is valid, as is a cycle whose keys exist before INSERT.
        for(boolean cycle:new boolean[]{false,true}) {
            File classes=tmp.newFolder();Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
            String key=cycle?"@Id @GeneratedValue(strategy=GenerationType.TABLE)":"@Id";
            sources.put("requiredcontrol.Owner","package requiredcontrol; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Owner { "+key+" public long id; @ManyToOne(optional=false) public Target target; }");
            sources.put("requiredcontrol.Target","package requiredcontrol; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity public class Target { "+key+" public long id; "+(cycle?"@ManyToOne(optional=false) public Owner owner;":"")+" }");
            JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        }
    }

    @Test
    public void jpqlPreservesDollarSignsInTopLevelEntityNames() throws Exception {
        File classes=tmp.newFolder();
        JavaSourceCompiler.compile(JavaSourceCompiler.singleSource("dollar.Invoice$Archive","package dollar; import com.codename1.annotations.*; @Entity(table=\"dollar_archive\") public class Invoice$Archive { @Id public long id; }"),classes,Arrays.asList(testClassesDir()));
        ProcessorContext client=runProcessor(classes);assertFalse(client.getErrors().toString(),client.hasErrors());ProcessorContext backend=runProcessor(classes,backendClasspath());assertFalse(backend.getErrors().toString(),backend.hasErrors());
        try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader())) {
            loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();Class type=loader.loadClass("dollar.Invoice$Archive");
            com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));com.codename1.orm.session.Session session=em.openSession();
            try {
                session.createTables();session.beginTransaction();Object entity=type.newInstance();session.persist(entity);session.commitTransaction();
                for(String name:Arrays.asList("Invoice$Archive","dollar.Invoice$Archive")) org.junit.Assert.assertSame(entity,session.createQuery("select e from "+name+" e",type).first());
                try { session.createQuery("select e from Archive e",type);fail("A dollar suffix is not an entity name"); } catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("Unknown entity")); }
            } finally { session.close();em.close(); }
        }
    }

    @Test
    public void derivedIndexesCannotDuplicateUniqueColumnsOrRelationshipIndexes() throws Exception {
        for(String field:Arrays.asList("code","target")) {
            Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
            sources.put("derivedclash.Owner","package derivedclash; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(indexes=@Index(fields=\""+field+"\",unique=true)) public class Owner { @Id public long id; @Column(unique=true) public String code; @OneToOne public Target target; }");
            sources.put("derivedclash.Target","package derivedclash; import com.codename1.annotations.*; @Entity public class Target { @Id public long id; }");
            rejectsMappingForBothRuntimes(sources,"Invalid or duplicate index name");
        }
        Map<String,String> duplicate=new java.util.LinkedHashMap<String,String>();
        duplicate.put("derivedclash.Repeated","package derivedclash; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(indexes={@Index(fields=\"code\"),@Index(fields=\"code\")}) public class Repeated { @Id public long id; public String code; }");
        rejectsMappingForBothRuntimes(duplicate,"Invalid or duplicate index name");
        String derived="cn1_index_"+Integer.toHexString("join_links/target_id".hashCode());
        Map<String,String> join=new java.util.LinkedHashMap<String,String>();
        join.put("derivedclash.Owner","package derivedclash; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(indexes=@Index(name=\""+derived+"\",fields=\"id\")) public class Owner { @Id public long id; @ManyToMany @JoinTable(name=\"join_links\") public java.util.Set<Target> targets; }");
        join.put("derivedclash.Target","package derivedclash; import com.codename1.annotations.*; @Entity public class Target { @Id public long id; }");
        rejectsMappingForBothRuntimes(join,"Invalid or duplicate index name");
    }

    @Test
    public void sequenceNamesCannotCollideWithTablesIndexesOrExceedPortableLength() throws Exception {
        for(String name:Arrays.asList("SEQ_OWNER","seq_links","by_code","cn1_unique_"+Integer.toHexString("seq_owner/code".hashCode()),"cn1_orm_sequences",String.join("",Collections.nCopies(64,"x")))) {
            Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
            sources.put("seqnames.Owner","package seqnames; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"seq_owner\",indexes=@Index(name=\"by_code\",fields=\"code\")) public class Owner { @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator=\""+name+"\") public long id; @Column(unique=true) public String code; @ElementCollection @JoinTable(name=\"seq_links\") public java.util.List<String> values; }");
            rejectsMappingForBothRuntimes(sources,name.length()>63?"Sequence name exceeds the portable limit":"Sequence name conflicts with schema object");
        }
    }

    private java.net.URLClassLoader reviewRelations(String idType) throws Exception {
        File classes=tmp.newFolder();
        Map<String,String> sources=new java.util.LinkedHashMap<String,String>();
        sources.put("review.Customer","package review; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"review_customer\") public class Customer { @Id public "+idType+" id; public String name; }");
        sources.put("review.Purchase","package review; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"review_purchase\") public class Purchase { @Id public long id; @ManyToOne(fetch=FetchType.LAZY) public Customer customer; @OneToMany(mappedBy=\"purchase\") public java.util.List<Item> items=new java.util.ArrayList<Item>(); }");
        sources.put("review.Item","package review; import com.codename1.annotations.*; import com.codename1.annotations.db.*; @Entity(table=\"review_item\") public class Item { @Id public long id; @ManyToOne(fetch=FetchType.LAZY) public Purchase purchase; }");
        JavaSourceCompiler.compile(sources,classes,Arrays.asList(testClassesDir()));
        ProcessorContext ctx=runProcessor(classes,backendClasspath());
        assertFalse(ctx.getErrors().toString(),ctx.hasErrors());
        java.net.URLClassLoader loader=new java.net.URLClassLoader(new URL[]{classes.toURI().toURL()},getClass().getClassLoader());
        loader.loadClass("cn1app.BackendDaoBootstrap").newInstance();
        return loader;
    }

    @Test
    public void updatingToOneRejectsUnsavedBoxedAndPrimitiveIdentifiers() throws Exception {
        for(String idType:Arrays.asList("Long","long")) {
            java.net.URLClassLoader loader=reviewRelations(idType);
            Class customerType=loader.loadClass("review.Customer"),purchaseType=loader.loadClass("review.Purchase");
            com.codename1.backend.orm.EntityManager em=com.codename1.backend.orm.EntityManager.open(com.codename1.backend.Database.open(":memory:"));
            com.codename1.orm.session.Session session=em.openSession();
            try {
                session.createTables();session.beginTransaction();
                Object customer=customerType.newInstance(),purchase=purchaseType.newInstance();
                session.persist(customer);purchaseType.getField("customer").set(purchase,customer);
                session.persist(purchase);session.commitTransaction();
                Object purchaseId=purchaseType.getField("id").get(purchase);
                // Test both replacement of a saved association and null -> unsaved.
                for(int iteration=0;iteration<2;iteration++) {
                    session.beginTransaction();
                    purchaseType.getField("customer").set(purchase,customerType.newInstance());
                    try { session.flush();fail("Unsaved "+idType+" association must fail during flush"); }
                    catch(com.codename1.orm.session.PersistenceException expected) {
                        assertTrue(expected.getMessage(),expected.getMessage().contains("Transient association without cascade PERSIST"));
                    }
                    assertTrue(session.isRollbackOnly());session.rollbackTransaction();
                    purchase=session.find(purchaseType,purchaseId);session.initialize(purchase,"customer");
                    org.junit.Assert.assertEquals(iteration==0, purchaseType.getField("customer").get(purchase)!=null);
                    session.beginTransaction();purchaseType.getField("customer").set(purchase,null);session.commitTransaction();
                }
                // Explicitly persisting the new target is valid without a cascade.
                session.beginTransaction();Object replacement=customerType.newInstance();session.persist(replacement);
                purchaseType.getField("customer").set(purchase,replacement);session.commitTransaction();
                session.clear();purchase=session.find(purchaseType,purchaseId);session.initialize(purchase,"customer");
                org.junit.Assert.assertNotNull(purchaseType.getField("customer").get(purchase));
            } finally { session.close();em.close();loader.close(); }
        }
    }

    @Test
    public void fetchJoinDoesNotForceDistinctWhenOrderingByRelatedColumn() throws Exception {
        java.net.URLClassLoader loader=reviewRelations("long");
        List<String> statements=new ArrayList<String>();
        com.codename1.impl.orm.SqlAccess access=(com.codename1.impl.orm.SqlAccess)java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),new Class[]{com.codename1.impl.orm.SqlAccess.class},(proxy,method,args)-> {
                    if(method.getName().equals("quote")) return com.codename1.backend.sql.Dialect.POSTGRES.quote((String)args[0]);
                    if(method.getName().equals("dialect")) return "postgresql";
                    if(method.getName().equals("orderBy")) return com.codename1.backend.sql.Dialect.POSTGRES.orderBy((String)args[0],(Boolean)args[1],((Integer)args[2])==com.codename1.impl.orm.Attribute.TEXT);
                    if(method.getName().equals("orderValue")) return com.codename1.backend.sql.Dialect.POSTGRES.comparison((String)args[0],((Integer)args[1])==com.codename1.impl.orm.Attribute.TEXT);
                    if(method.getName().equals("limit")) return com.codename1.backend.sql.Dialect.POSTGRES.limit((Integer)args[0],(Integer)args[1]);
                    if(method.getName().equals("query")) { statements.add((String)args[0]);return Collections.emptyList(); }
                    if(method.getName().equals("close")) return null;
                    throw new AssertionError("Unexpected SQL adapter operation: "+method.getName());
                });
        com.codename1.orm.session.Session session=new com.codename1.impl.orm.SessionImpl(access);
        try {
            session.createQuery("select p from Purchase p join fetch p.items order by p.customer.name").list();
            assertTrue(statements.get(0),statements.get(0).contains("ORDER BY"));
            assertTrue(statements.get(0),statements.get(0).contains("INNER JOIN \"review_item\""));
            assertFalse("Fetch joins must not force DISTINCT for an unselected ordering expression",statements.get(0).startsWith("SELECT DISTINCT "));
            session.createQuery("select distinct p from Purchase p join fetch p.items").list();
            assertTrue("An explicit DISTINCT remains effective",statements.get(1).startsWith("SELECT DISTINCT "));
        } finally { session.close();loader.close(); }
    }

    @Test
    public void generatesDaoWithExpectedShape() throws Exception {
        File classes = compileFixture(
                "com.example.User",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity(table=\"users\")\n"
                        + "public class User {\n"
                        + "    @Id(autoIncrement=true) public long id;\n"
                        + "    @Column(name=\"full_name\", nullable=false) public String name;\n"
                        + "    public int age;\n"
                        + "    @DbTransient public String tempCache;\n"
                        + "    public User() {}\n"
                        + "}\n");
        runProcessorOrFail(classes);

        File daoFile = new File(classes, "com/example/UserCn1Dao.class");
        assertTrue("generated dao file should exist: " + daoFile, daoFile.exists());
        File bootstrapFile = new File(classes, "cn1app/DaoBootstrap.class");
        assertTrue("DaoBootstrap should exist", bootstrapFile.exists());

        Shape shape = readShape(daoFile);
        assertTrue("dao should implement com.codename1.orm.Dao",
                shape.interfaces.contains("com/codename1/orm/Dao"));
        assertTrue(shape.methodNames.contains("createTable"));
        assertTrue(shape.methodNames.contains("insert"));
        assertTrue(shape.methodNames.contains("update"));
        assertTrue(shape.methodNames.contains("delete"));
        assertTrue(shape.methodNames.contains("findById"));
        assertTrue(shape.methodNames.contains("findAll"));
        assertTrue(shape.methodNames.contains("find"));
        assertTrue(shape.methodNames.contains("dropTable"));
        assertTrue(shape.methodNames.contains("attach"));
    }

    @Test
    public void rejectsEntityMissingIdField() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.NoId",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                                + "@Entity public class NoId {\n"
                                + "    public String name;\n"
                                + "    public NoId() {}\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected validation error when @Id is missing", ctx.hasErrors());
    }

    @Test
    public void rejectsAFlavourValueItDoesNotUnderstand() throws Exception {
        // "true".equalsIgnoreCase(v) reads EVERY other spelling as false, so a
        // typo silently selected the client flavour and skipped the
        // both-runtimes guard -- a green build and a packaged server with none
        // of its backend registrations.
        String previous = System.getProperty("cn1.backendOrm");
        System.setProperty("cn1.backendOrm", "ture");
        try {
            File classes = tmp.newFolder("classes");
            JavaSourceCompiler.compile(
                    JavaSourceCompiler.singleSource("com.example.Typo",
                            "package com.example;\n"
                                    + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                                    + "@Entity public class Typo {\n"
                                    + "    @Id public long id;\n"
                                    + "    public Typo() {}\n"
                                    + "}\n"),
                    classes, Arrays.asList(testClassesDir()));
            ProcessorContext ctx = runProcessor(classes);
            assertTrue("a flavour value that is neither true nor false must be refused",
                    ctx.hasErrors());
        } finally {
            if (previous == null) {
                System.clearProperty("cn1.backendOrm");
            } else {
                System.setProperty("cn1.backendOrm", previous);
            }
        }
    }

    @Test
    public void acceptsBothSpellingsOfTheFlavour() throws Exception {
        // THE CONTROL: the refusal above must not be "anything set is refused".
        String previous = System.getProperty("cn1.backendOrm");
        try {
            String[] values = new String[] {"true", "false", "TRUE", "False"};
            for (int iter = 0; iter < values.length; iter++) {
                String value = values[iter];
                System.setProperty("cn1.backendOrm", value);
                // Numbered, not named after the value: "true" and "TRUE" are the
                // same folder on a case-insensitive filesystem, which is most of
                // the machines this runs on.
                File classes = tmp.newFolder("classes-" + iter);
                JavaSourceCompiler.compile(
                        JavaSourceCompiler.singleSource("com.example.Fine",
                                "package com.example;\n"
                                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                                        + "@Entity public class Fine {\n"
                                        + "    @Id public long id;\n"
                                        + "    public Fine() {}\n"
                                        + "}\n"),
                        classes, Arrays.asList(testClassesDir()));
                ProcessorContext ctx = runProcessor(classes);
                assertFalse("-Dcn1.backendOrm=" + value + " is a value this accepts",
                        ctx.hasErrors());
            }
        } finally {
            if (previous == null) {
                System.clearProperty("cn1.backendOrm");
            } else {
                System.setProperty("cn1.backendOrm", previous);
            }
        }
    }

    @Test
    public void rejectsANestedEntity() throws Exception {
        // A public static member class can have a public no-arg constructor, so
        // this used to be ACCEPTED and then generated a dao naming the entity by
        // its binary name, com.example.Outer$Nested, which javac reads as a
        // top-level identifier and cannot resolve. The generated source did not
        // compile, for a class the processor had already approved.
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Outer",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                                + "public class Outer {\n"
                                + "    @Entity public static class Nested {\n"
                                + "        @Id public long id;\n"
                                + "        public String label;\n"
                                + "        public Nested() {}\n"
                                + "    }\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("a nested @Entity must be refused rather than generated for",
                ctx.hasErrors());
    }

    @Test
    public void acceptsATopLevelEntityWithADollarInItsName() throws Exception {
        // THE CONTROL, and it is not pedantry: a dollar is legal in a top-level
        // class name, so a refusal that keyed off the "$" in the binary name
        // would reject this one too. The nesting test has to come from the
        // InnerClasses attribute, which is what getSourceName reads.
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Od$d",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                                + "@Entity public class Od$d {\n"
                                + "    @Id public long id;\n"
                                + "    public String label;\n"
                                + "    public Od$d() {}\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertFalse("a top-level name containing a dollar is not a nested class",
                ctx.hasErrors());
    }

    @Test
    public void rejectsEntityWithRelationshipField() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Order",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                                + "@Entity public class Order {\n"
                                + "    @Id public long id;\n"
                                + "    public java.util.List<String> tags;\n"
                                + "    public Order() {}\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected validation error on relationship field", ctx.hasErrors());
    }

    @Test
    public void generatesAServerSideDaoWhenTheModuleIsABackendOne() throws Exception {
        // The SAME entity source as the client case. That is the whole claim of
        // sharing the annotations: one class, and the module it is compiled in
        // decides which database it is stored in.
        File classes = compileFixture(
                "com.example.Note",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity(table=\"notes\")\n"
                        + "public class Note {\n"
                        + "    @Id public long id;\n"
                        + "    @Column(nullable=false) public String title;\n"
                        + "    public Integer views;\n"
                        + "    public boolean pinned;\n"
                        + "    public java.util.Date created;\n"
                        + "    @DbTransient public String cache;\n"
                        + "    public Note() {}\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes, backendClasspath());
        assertFalse("the backend flavour reported errors: " + ctx.getErrors(), ctx.hasErrors());

        // The names are NOT the client's, deliberately: a shared module's jar can
        // carry the client dao for this very class, and both have to be able to
        // sit on one classpath.
        File dao = new File(classes, "com/example/NoteCn1BackendDao.class");
        assertTrue("generated server-side dao should exist: " + dao, dao.exists());
        assertTrue("the client dao must NOT be generated in a backend module",
                !new File(classes, "com/example/NoteCn1Dao.class").exists());
        assertTrue("BackendDaoBootstrap should exist",
                new File(classes, "cn1app/BackendDaoBootstrap.class").exists());
        assertFalse("the client bootstrap must not be generated in a backend module",
                new File(classes, "cn1app/DaoBootstrap.class").exists());

        Shape shape = readShape(dao);
        assertTrue("a server-side dao is an EntityDefinition, which is what carries the "
                        + "column descriptions the runtime builds statements from",
                shape.superName.equals("com/codename1/backend/orm/EntityDefinition"));
        // No SQL in the generated class: the statements are the runtime's, built
        // per dialect, which is what lets ONE generated dao serve SQLite,
        // PostgreSQL and MySQL.
        assertTrue(shape.methodNames.contains("columns"));
        assertTrue(shape.methodNames.contains("get"));
        assertTrue(shape.methodNames.contains("set"));
        assertTrue(shape.methodNames.contains("newInstance"));
        assertTrue(shape.methodNames.contains("table"));
    }

    @Test
    public void refusesToGenerateOverAClassTheProjectAlreadyHas() throws Exception {
        // The generated dao lands in the same output directory under a name
        // derived from the developer's entity, in the developer's own package.
        // javac never sees the two as duplicates -- the existing one is a
        // classpath class, not a second source -- so without this the
        // application class is simply overwritten and everything compiles.
        File classes = compileFixture(
                "com.example.Memo",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity public class Memo {\n"
                        + "    @Id public long id;\n"
                        + "    public String title;\n"
                        + "    public Memo() {}\n"
                        + "}\n");
        // The collision, compiled into the same output directory the way the
        // developer's own build would have put it there.
        compileInto(classes, "com.example.MemoCn1BackendDao",
                "package com.example;\n"
                        + "public class MemoCn1BackendDao {\n"
                        + "    public String mine() { return \"application code\"; }\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes, backendClasspath());
        assertTrue("a name the project already uses should be refused", ctx.hasErrors());
        assertTrue("the message should name the class: " + ctx.getErrors(),
                ctx.getErrors().toString().indexOf("MemoCn1BackendDao") >= 0);
    }

    @Test
    public void generatesTwiceWithoutReportingItsOwnOutputAsACollision() throws Exception {
        // The other half, and the one that broke @RestController before it: an
        // incremental build runs process-classes again without a clean and finds
        // the dao written on the first pass. Generated sources carry @Generated
        // so that one is recognised as ours; a real collision has no marker.
        File classes = compileFixture(
                "com.example.Card",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity public class Card {\n"
                        + "    @Id public long id;\n"
                        + "    public String title;\n"
                        + "    public Card() {}\n"
                        + "}\n");
        ProcessorContext first = runProcessor(classes, backendClasspath());
        assertFalse("the first pass should not have reported errors: " + first.getErrors(),
                first.hasErrors());
        assertTrue("the first pass should have generated the dao",
                new File(classes, "com/example/CardCn1BackendDao.class").exists());
        ProcessorContext second = runProcessor(classes, backendClasspath());
        assertFalse("a second pass must not report its own output as a collision: "
                + second.getErrors(), second.hasErrors());
    }

    @Test
    public void refusesANullableColumnOnAPrimitiveField() throws Exception {
        // A primitive has no null to read, so the two cannot both be true. Said
        // at build time rather than at the first row that happens to be null.
        File classes = compileFixture(
                "com.example.Gauge",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity public class Gauge {\n"
                        + "    @Id public long id;\n"
                        + "    @Column(nullable=true) public int reading;\n"
                        + "    public Gauge() {}\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes, backendClasspath());
        assertTrue("nullable=true on a primitive should be refused", ctx.hasErrors());
        assertTrue("the message should name the field: " + ctx.getErrors(),
                ctx.getErrors().toString().indexOf("reading") >= 0);
    }

    @Test
    public void removesTheBootstrapWhenTheLastEntityGoesAway() throws Exception {
        // The generated entry point asks whether the bootstrap FILE exists --
        // deliberately, because entities can come from a jar and then the file
        // is the only answer. So a bootstrap left behind by the previous run
        // made the server register definitions for entities that are gone:
        // reopening the datasource, recreating tables the developer removed, or
        // failing against fields that no longer exist, with mvn clean as the
        // only cure.
        File classes = compileFixture(
                "com.example.Draft",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity public class Draft {\n"
                        + "    @Id public long id;\n"
                        + "    public String title;\n"
                        + "    public Draft() {}\n"
                        + "}\n");
        ProcessorContext first = runProcessor(classes, backendClasspath());
        assertFalse("the first pass should not have reported errors: " + first.getErrors(),
                first.hasErrors());
        File bootstrap = new File(classes, "cn1app/BackendDaoBootstrap.class");
        assertTrue("the first pass should have generated the bootstrap", bootstrap.exists());

        // The SAME class without the annotation, compiled over the first one --
        // which is what an incremental build sees after the developer removes it.
        compileInto(classes, "com.example.Draft",
                "package com.example;\n"
                        + "public class Draft {\n"
                        + "    public long id;\n"
                        + "    public String title;\n"
                        + "    public Draft() {}\n"
                        + "}\n");
        ProcessorContext second = runProcessor(classes, backendClasspath());
        assertFalse("the second pass should not have reported errors: " + second.getErrors(),
                second.hasErrors());
        assertFalse("the stale bootstrap should have been removed", bootstrap.exists());
    }

    @Test
    public void doesNotResurrectAnOrphanThroughTheModulesOwnOutput() throws Exception {
        // THE REAL MOJO CLASSPATH. ProcessAnnotationsMojo passes
        // MavenProject.getCompileClasspathElements(), and Maven puts the module's
        // OWN target/classes first in it. The dependency scan then re-reads the
        // directory the module pass just walked -- with fromThisModule false,
        // which is what turns the backing-source check off -- so a class whose
        // .java is gone, skipped moments earlier as an orphan, came back as
        // somebody else's entity and its bootstrap with it.
        //
        // TWO things every other test here leaves out, and it needs both. They
        // pass only the backend jar, so the module's own output is never on the
        // classpath; and they pass NO compile source roots, which makes
        // hasBackingSource return true for everything, so the orphan skip this
        // is about never runs at all.
        File sourceRoot = tmp.newFolder();
        File pkgDir = new File(sourceRoot, "com/example");
        assertTrue("could not create the source package", pkgDir.mkdirs());
        File source = new File(pkgDir, "Ledger.java");
        String entity = "package com.example;\n"
                + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                + "@Entity public class Ledger {\n"
                + "    @Id public long id;\n"
                + "    public String memo;\n"
                + "    public Ledger() {}\n"
                + "}\n";
        writeFile(source, entity);
        File classes = compileFixture("com.example.Ledger", entity);

        List<String> withOwnOutput = new ArrayList<String>();
        withOwnOutput.add(classes.getAbsolutePath());
        withOwnOutput.addAll(backendClasspath());
        List<String> roots = Collections.singletonList(sourceRoot.getAbsolutePath());

        ProcessorContext first = runProcessor(classes, withOwnOutput, roots);
        assertFalse("the first pass should not have reported errors: " + first.getErrors(),
                first.hasErrors());
        File bootstrap = new File(classes, "cn1app/BackendDaoBootstrap.class");
        assertTrue("the first pass should have generated the bootstrap", bootstrap.exists());

        // THE SOURCE GOES, THE CLASS FILE STAYS -- still carrying @Entity, which
        // is what an incremental build leaves after a delete or a rename. This is
        // the orphan, and it is the case removing the annotation does NOT cover.
        assertTrue("could not delete the source", source.delete());
        ProcessorContext second = runProcessor(classes, withOwnOutput, roots);
        assertFalse("the second pass should not have reported errors: " + second.getErrors(),
                second.hasErrors());
        assertFalse("the orphan was re-accepted through the module's own output "
                + "being scanned as a dependency", bootstrap.exists());
    }

    @Test
    public void refusesAnExplicitTypeOnACharField() throws Exception {
        // A char is stored as its UTF-16 code unit in an integer column, so 'x'
        // binds as 120. Declared onto CHAR(1) -- the natural way to meet an
        // existing schema -- PostgreSQL refuses 120 as too long and MySQL
        // truncates it: the write and the declaration disagree. Refused rather
        // than guessed at, because deciding whether an arbitrary type string is
        // textual means parsing whatever three dialects accept.
        File classes = compileFixture(
                "com.example.Grade",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity public class Grade {\n"
                        + "    @Id public long id;\n"
                        + "    @Column(type=\"CHAR(1)\") public char letter;\n"
                        + "    public Grade() {}\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes, backendClasspath());
        assertTrue("a text type on a char field should be refused", ctx.hasErrors());
        assertTrue("the message should name the type: " + ctx.getErrors(),
                ctx.getErrors().toString().indexOf("CHAR(1)") >= 0);
    }

    @Test
    public void keepsACharFieldWithNoDeclaredType() throws Exception {
        // The other side: without a declared type the dialect chooses an integer
        // column and the code-unit mapping fits, so this must keep building.
        File classes = compileFixture(
                "com.example.Mark",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity public class Mark {\n"
                        + "    @Id public long id;\n"
                        + "    public char letter;\n"
                        + "    public Character optional;\n"
                        + "    public Mark() {}\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes, backendClasspath());
        assertFalse("a char with no declared type is fine: " + ctx.getErrors(),
                ctx.hasErrors());
    }

    @Test
    public void refusesAnIdentifierNoEngineStoresWhole() throws Exception {
        // Measured with a 70-character name: SQLite accepts it, PostgreSQL
        // accepts it and TRUNCATES to 63 bytes, MySQL refuses it. The
        // truncation is the dangerous one -- two names differing only past the
        // cut become one table and nothing reports it -- so the tightest limit
        // is the limit, and it is said at build time rather than at the first
        // CREATE TABLE in production.
        StringBuilder longName = new StringBuilder("t_");
        while (longName.length() < 70) {
            longName.append('x');
        }
        File classes = compileFixture(
                "com.example.Wide",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity(table=\"" + longName + "\")\n"
                        + "public class Wide {\n"
                        + "    @Id public long id;\n"
                        + "    public Wide() {}\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes, backendClasspath());
        assertTrue("a table name past every engine's limit should be refused",
                ctx.hasErrors());
        assertTrue("the message should give the length: " + ctx.getErrors(),
                ctx.getErrors().toString().indexOf("70 bytes") >= 0);
    }

    @Test
    public void refusesAnOverLongColumnName() throws Exception {
        StringBuilder longName = new StringBuilder("c_");
        while (longName.length() < 70) {
            longName.append('y');
        }
        File classes = compileFixture(
                "com.example.WideColumn",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity public class WideColumn {\n"
                        + "    @Id public long id;\n"
                        + "    @Column(name=\"" + longName + "\") public String v;\n"
                        + "    public WideColumn() {}\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes, backendClasspath());
        assertTrue("a column name past every engine's limit should be refused",
                ctx.hasErrors());
    }

    @Test
    public void keepsAnIdentifierThatFits() throws Exception {
        // The boundary, so the check cannot pass by refusing everything: 63
        // bytes is the limit and must still build.
        StringBuilder atLimit = new StringBuilder("t_");
        while (atLimit.length() < 63) {
            atLimit.append('z');
        }
        File classes = compileFixture(
                "com.example.Fits",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity(table=\"" + atLimit + "\")\n"
                        + "public class Fits {\n"
                        + "    @Id public long id;\n"
                        + "    public Fits() {}\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes, backendClasspath());
        assertFalse("63 bytes fits and must still build: " + ctx.getErrors(),
                ctx.hasErrors());
    }

    @Test
    public void refusesAnExplicitTypeOnAGeneratedKey() throws Exception {
        // A generated key's declaration is one indivisible form per engine --
        // SQLite's AUTOINCREMENT is legal only after the exact words INTEGER
        // PRIMARY KEY -- so there is nowhere to put BIGINT UNSIGNED. It used to
        // be discarded in silence, which made @Column(type) mean what it says on
        // every column but this one.
        File classes = compileFixture(
                "com.example.Invoice",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity public class Invoice {\n"
                        + "    @Id @Column(type=\"BIGINT UNSIGNED\") public long id;\n"
                        + "    public Invoice() {}\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes, backendClasspath());
        assertTrue("an explicit type on a generated key should be refused", ctx.hasErrors());
        assertTrue("the message should name the type: " + ctx.getErrors(),
                ctx.getErrors().toString().indexOf("BIGINT UNSIGNED") >= 0);
    }

    @Test
    public void keepsAnExplicitTypeOnAnAssignedKey() throws Exception {
        // The other side: with the application assigning the key there is no
        // engine-specific syntax to agree with, so the declared type is written
        // through and this must keep building.
        File classes = compileFixture(
                "com.example.Voucher",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity public class Voucher {\n"
                        + "    @Id(autoIncrement=false) @Column(type=\"CHAR(36)\")\n"
                        + "    public String code;\n"
                        + "    public Voucher() {}\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes, backendClasspath());
        assertFalse("an assigned key may declare its type: " + ctx.getErrors(),
                ctx.hasErrors());
    }

    @Test
    public void refusesAGeneratedStringKeyOnTheServer() throws Exception {
        // No engine generates a string key: SQLite's AUTOINCREMENT is legal only
        // after INTEGER PRIMARY KEY and the other two count. Caught here rather
        // than at start-up, where the message is the server's own syntax error.
        File classes = compileFixture(
                "com.example.Session",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity public class Session {\n"
                        + "    @Id public String token;\n"
                        + "    public Session() {}\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes, backendClasspath());
        assertTrue("a generated String key should be refused", ctx.hasErrors());
    }

    @Test
    public void refusesAGeneratedKeyNoDatabaseCanGenerate() throws Exception {
        // Every one of these fails somewhere the build cannot see: a byte[] key
        // commits the insert and then throws reading the generated Long back, and
        // a boolean key turns every generated value into true -- so the first
        // row's id is 1 and every later update and delete targets it.
        // byte and short are integers and were accepted by the first version of
        // this check, but a generated key leaves their range after 127 and
        // 32,767 rows and the setter then narrows it to a negative number.
        String[] fields = {"public byte[] id;", "public boolean id;", "public double id;",
                "public java.util.Date id;", "public byte id;", "public short id;",
                "public Short id;"};
        for (String field : fields) {
            File classes = compileFixture(
                    "com.example.Bad",
                    "package com.example;\n"
                            + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                            + "@Entity public class Bad {\n"
                            + "    @Id " + field + "\n"
                            + "    public Bad() {}\n"
                            + "}\n");
            ProcessorContext ctx = runProcessor(classes, backendClasspath());
            assertTrue("a generated key of type " + field + " should be refused",
                    ctx.hasErrors());
        }
        // And the types a database really does generate are accepted.
        String[] good = {"public long id;", "public int id;", "public Long id;"};
        for (String field : good) {
            File classes = compileFixture(
                    "com.example.Good",
                    "package com.example;\n"
                            + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                            + "@Entity public class Good {\n"
                            + "    @Id " + field + "\n"
                            + "    public Good() {}\n"
                            + "}\n");
            ProcessorContext ctx = runProcessor(classes, backendClasspath());
            assertFalse("a generated key of type " + field + " should be accepted: "
                    + ctx.getErrors(), ctx.hasErrors());
        }
    }

    @Test
    public void refusesTwoFieldsMappedToOneColumn() throws Exception {
        // Every generated statement would name the column twice: the CREATE
        // TABLE is refused for a duplicate column, and against a schema the ORM
        // did not create, a read loads the same value into both fields and a
        // write stores whichever the generated order put last. Caught at build
        // time, where both field names can be said out loud.
        File classes = compileFixture(
                "com.example.Clash",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity public class Clash {\n"
                        + "    @Id public long id;\n"
                        + "    @Column(name=\"label\") public String name;\n"
                        + "    @Column(name=\"label\") public String title;\n"
                        + "    public Clash() {}\n"
                        + "}\n");
        ProcessorContext ctx = runProcessor(classes, backendClasspath());
        assertTrue("two fields on one column should be refused", ctx.hasErrors());
        // And the same mapping differing only in CASE, because SQLite and MySQL
        // read those as one column even quoted while PostgreSQL keeps them
        // apart: a mapping only one of the three accepts is not portable.
        File folded = compileFixture(
                "com.example.Folded",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity public class Folded {\n"
                        + "    @Id public long id;\n"
                        + "    @Column(name=\"label\") public String name;\n"
                        + "    @Column(name=\"LABEL\") public String title;\n"
                        + "    public Folded() {}\n"
                        + "}\n");
        assertTrue("a case-folded duplicate should be refused too",
                runProcessor(folded, backendClasspath()).hasErrors());
        String reported = String.valueOf(ctx.getErrors());
        assertTrue(reported, reported.contains("label"));
        assertTrue(reported, reported.contains("name"));
        assertTrue(reported, reported.contains("title"));
    }

    @Test
    public void generatesForAnEntityThatLivesInADependency() throws Exception {
        // The entity in a module BOTH halves of the application depend on, which
        // is where a shared one belongs -- and then the backend module's own
        // compiled classes hold no @Entity at all. Without the classpath scan the
        // server gets no dao and fails on its first query.
        File shared = compileFixture(
                "com.example.Shared",
                "package com.example;\n"
                        + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                        + "@Entity public class Shared {\n"
                        + "    @Id public long id;\n"
                        + "    public String name;\n"
                        + "    public Shared() {}\n"
                        + "}\n");
        File moduleClasses = tmp.newFolder("module");
        List<String> classpath = new ArrayList<String>(backendClasspath());
        classpath.add(shared.getAbsolutePath());
        ProcessorContext ctx = runProcessor(moduleClasses, classpath);
        assertFalse("errors: " + ctx.getErrors(), ctx.hasErrors());
        assertTrue("a dao should be generated for an entity read off the classpath",
                new File(moduleClasses, "com/example/SharedCn1BackendDao.class").exists());
    }

    @Test
    public void ignoresVersionedEntitiesInMultiReleaseJars() throws Exception {
        File base = compileFixture("com.example.Shared",
                "package com.example; import com.codename1.annotations.*; import com.codename1.annotations.db.*; "
                + "@Entity public class Shared { @Id public long id; public String name; }");
        File versioned = compileFixture("com.example.Shared",
                "package com.example; import com.codename1.annotations.*; import com.codename1.annotations.db.*; "
                + "@Entity public class Shared { @Id public long id; public String later; }");
        File onlyVersioned = compileFixture("com.example.Later",
                "package com.example; import com.codename1.annotations.*; import com.codename1.annotations.db.*; "
                + "@Entity public class Later { @Id public long id; }");
        File jar = tmp.newFile("multi-release.jar");
        java.util.jar.Manifest manifest = new java.util.jar.Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Multi-Release", "true");
        try (java.util.jar.JarOutputStream out = new java.util.jar.JarOutputStream(
                new java.io.FileOutputStream(jar), manifest)) {
            // Put the override first so an accidental first-match scan cannot pass.
            addJarClass(out, "META-INF/versions/9/com/example/Shared.class",
                    new File(versioned, "com/example/Shared.class"));
            addJarClass(out, "META-INF/versions/11/com/example/Later.class",
                    new File(onlyVersioned, "com/example/Later.class"));
            addJarClass(out, "com/example/Shared.class",
                    new File(base, "com/example/Shared.class"));
        }
        File module = tmp.newFolder();
        List<String> classpath = new ArrayList<String>(backendClasspath());
        classpath.add(jar.getAbsolutePath());
        ProcessorContext ctx = runProcessor(module, classpath);
        assertFalse("errors: " + ctx.getErrors(), ctx.hasErrors());
        assertTrue(new File(module, "com/example/SharedCn1BackendDao.class").exists());
        assertFalse(new File(module, "com/example/LaterCn1BackendDao.class").exists());
    }

    private static void addJarClass(java.util.jar.JarOutputStream out, String name,
                                   File cls) throws Exception {
        out.putNextEntry(new java.util.jar.JarEntry(name));
        Files.copy(cls.toPath(), out);
        out.closeEntry();
    }

    @Test
    public void theEntryPointCompilesAgainstTheDaosGeneratedBeforeIt() throws Exception {
        // The packaging order, as a test rather than as a convention.
        //
        // cn1:backend-package compiles into its own class tree and generates
        // into it; the entry point it writes references cn1app.BackendDaoBootstrap
        // whenever the module has an @Entity, because that reference is what
        // keeps the generated daos in the binary. Run the controller processor
        // over a tree the entity processor has not touched and its own javac
        // fails on a class that is not there -- which is what native packaging
        // did for every project that had both.
        String entity = "package com.example;\n"
                + "import com.codename1.annotations.*; import com.codename1.annotations.db.*;\n"
                + "@Entity public class Stored {\n"
                + "    @Id public long id;\n"
                + "    public String name;\n"
                + "    public Stored() {}\n"
                + "}\n";
        String controller = "package com.example;\n"
                + "import com.codename1.backend.annotations.*;\n"
                + "@RestController public class Api {\n"
                + "    @GetMapping(\"/healthz\") public String health() { return \"ok\"; }\n"
                + "}\n";

        // Without the entity pass: the entry point cannot compile.
        File alone = tmp.newFolder("without-daos");
        java.util.Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.Stored", entity);
        sources.put("com.example.Api", controller);
        JavaSourceCompiler.compile(sources, alone, filesOf(backendClasspath()));
        try {
            runControllers(alone, backendClasspath());
            fail("the entry point referenced a bootstrap that was never generated, so its "
                    + "compilation should have failed");
        } catch (ProcessingException expected) {
            assertTrue(expected.getMessage(),
                    expected.getMessage().contains("BackendDaoBootstrap")
                            || expected.getMessage().contains("cannot find symbol"));
        }

        // With it, in the order the packaging goal now uses.
        File together = tmp.newFolder("with-daos");
        JavaSourceCompiler.compile(sources, together, filesOf(backendClasspath()));
        ProcessorContext ctx = runProcessor(together, backendClasspath(), true);
        assertFalse("errors: " + ctx.getErrors(), ctx.hasErrors());
        assertTrue(new File(together, "cn1app/BackendDaoBootstrap.class").exists());
        runControllers(together, backendClasspath());
        assertTrue("the generated entry point should have compiled",
                new File(together, "com/example/BackendApplication.class").exists());
    }

    /** The controller processor alone, over an already-compiled tree. */
    private void runControllers(File classesDir, List<String> compileClasspath) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        RestControllerAnnotationProcessor proc = new RestControllerAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog(), null, null, null,
                Collections.<String>emptyList(), "UTF-8", compileClasspath);
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        proc.finish(ctx);
    }

    private static List<File> filesOf(List<String> paths) {
        List<File> out = new ArrayList<File>();
        for (String p : paths) out.add(new File(p));
        return out;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private File compileFixture(String fqn, String src) throws Exception {
        // An unnamed folder, so a test that compiles several fixtures gets a
        // fresh one each time rather than "a folder with the path 'classes'
        // already exists".
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource(fqn, src),
                classes,
                Arrays.asList(testClassesDir()));
        return classes;
    }

    /// Compiles one more class into a directory a fixture already occupies, so
    /// the processor meets it exactly as it would meet a class the developer's
    /// own build had put there.
    private void compileInto(File classesDir, String fqn, String src) throws Exception {
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource(fqn, src),
                classesDir,
                Arrays.asList(testClassesDir()));
    }

    private void runProcessorOrFail(File classesDir) throws Exception {
        ProcessorContext ctx = runProcessor(classesDir);
        if (ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("processor reported errors:\n");
            for (ProcessorContext.ProcessingError e : ctx.getErrors()) sb.append(' ').append(e).append('\n');
            fail(sb.toString());
        }
    }

    private ProcessorContext runProcessor(File classesDir) throws Exception {
        return runProcessor(classesDir, Collections.<String>emptyList());
    }

    /// Where the backend runtime sits, taken from a loaded class rather than
    /// from a path so it follows the test classpath. Its presence is ALSO what
    /// the processor detects the flavour from, so this exercises the detection
    /// rather than forcing it with a system property.
    private static List<String> backendClasspath() throws Exception {
        URL url = com.codename1.backend.Database.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return Collections.singletonList(new File(url.toURI()).getAbsolutePath());
    }

    private ProcessorContext runProcessor(File classesDir, List<String> compileClasspath)
            throws Exception {
        return runProcessor(classesDir, compileClasspath, false);
    }

    /// As below, with compile source roots -- which is what makes
    /// hasBackingSource able to answer at all: an empty list means "cannot
    /// tell", and it keeps every class.
    private ProcessorContext runProcessor(File classesDir, List<String> compileClasspath,
                                          List<String> sourceRoots) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        OrmAnnotationProcessor proc = new OrmAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog(), null, null, null,
                sourceRoots, "UTF-8", compileClasspath);
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        proc.finish(ctx);
        return ctx;
    }

    private static void writeFile(File target, String content) throws Exception {
        Files.write(target.toPath(), content.getBytes("UTF-8"));
    }

    private ProcessorContext runProcessor(File classesDir, List<String> compileClasspath,
                                          boolean forceBackend) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        OrmAnnotationProcessor proc = new OrmAnnotationProcessor();
        if (forceBackend) {
            // What BackendPackageMojo does: that build compiles against the
            // JavaAPI with the runtime off the classpath, so detection cannot
            // answer for it.
            proc.setBackendFlavour(true);
        }
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog(), null, null, null,
                Collections.<String>emptyList(), "UTF-8", compileClasspath);
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        proc.finish(ctx);
        return ctx;
    }

    private static File testClassesDir() throws Exception {
        URL url = OrmAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }

    private static Shape readShape(File classFile) throws Exception {
        final Shape shape = new Shape();
        byte[] bytes = Files.readAllBytes(classFile.toPath());
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                shape.superName = superName;
                if (interfaces != null) {
                    for (String i : interfaces) shape.interfaces.add(i);
                }
            }

            @Override
            public org.objectweb.asm.MethodVisitor visitMethod(int access, String name,
                                                                String descriptor, String signature,
                                                                String[] exceptions) {
                shape.methodNames.add(name);
                return null;
            }
        }, ClassReader.SKIP_CODE);
        return shape;
    }

    private static final class Shape {
        String superName;
        final Set<String> interfaces = new LinkedHashSet<String>();
        final Set<String> methodNames = new LinkedHashSet<String>();
    }
}
