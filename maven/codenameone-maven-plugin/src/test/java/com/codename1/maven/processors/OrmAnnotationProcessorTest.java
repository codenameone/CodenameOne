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
            org.junit.Assert.assertSame(s.find(type,id),s.createQuery("select e from converted.Entry e where e.code in (:first, :second)",type).setParameter("first",excluded).setParameter("second",included).first());
            org.junit.Assert.assertSame(s.find(type,id),s.createQuery("select e from converted.Entry e where e.code in (:first, :second)",type).setParameter("first",included).setParameter("second",excluded).first());
            org.junit.Assert.assertNull(s.createQuery("select e from converted.Entry e where e.code not in (:first, :second)",type).setParameter("first",excluded).setParameter("second",included).first());
            org.junit.Assert.assertSame(s.find(type,id),s.createQuery("select e from converted.Entry e where e.code in ('missing', :second)",type).setParameter("second",included).first());
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
