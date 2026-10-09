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
package com.codename1.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/// The remap generates what `PropertyValueFactory` needs in place of
/// reflection: the accessors of the application's bean classes, called by
/// name through the relocated `PropertyRegistry`.
public class PropertyAccessRemapTest {

    private static final String APP = "package com.acme.fx;\n"
            + "import javafx.scene.control.TableColumn;\n"
            + "import javafx.scene.control.cell.PropertyValueFactory;\n"
            + "public class Shop extends javafx.application.Application {\n"
            + "    public void start(javafx.stage.Stage stage) {\n"
            + "        TableColumn<Plain, String> c = new TableColumn<Plain, String>(\"Label\");\n"
            + "        c.setCellValueFactory(new PropertyValueFactory<Plain, String>(\"label\"));\n"
            + "    }\n"
            + "}\n";

    private static final String PERSON = "package com.acme.fx;\n"
            + "import javafx.beans.property.SimpleStringProperty;\n"
            + "import javafx.beans.property.StringProperty;\n"
            + "public class Person {\n"
            + "    private final StringProperty name = new SimpleStringProperty(this, \"name\", \"Ann\");\n"
            + "    public StringProperty nameProperty() { return name; }\n"
            + "    public String getName() { return name.get(); }\n"
            + "    public int getAge() { return 41; }\n"
            + "    public boolean isActive() { return true; }\n"
            + "    public String getWith(int argument) { return \"no\"; }\n"
            + "    String getHidden() { return \"no\"; }\n"
            + "    public static String getShared() { return \"no\"; }\n"
            + "}\n";

    private static final String EMPLOYEE = "package com.acme.fx;\n"
            + "public class Employee extends Person {\n"
            + "    public String getName() { return \"Employee \" + super.getName(); }\n"
            + "    public double getSalary() { return 2.5; }\n"
            + "}\n";

    /// Getters only: reached because the application names `label` as a
    /// constant where it creates the factory.
    private static final String PLAIN = "package com.acme.fx;\n"
            + "public class Plain {\n"
            + "    public String getLabel() { return \"plain\"; }\n"
            + "}\n";

    /// Getters only and no name the application asks for.
    private static final String OTHER = "package com.acme.fx;\n"
            + "public class Other {\n"
            + "    public String getTitle() { return \"other\"; }\n"
            + "}\n";

    /// A property method, but the class is not public.
    private static final String HIDDEN = "package com.acme.fx;\n"
            + "class Hidden {\n"
            + "    public javafx.beans.property.StringProperty nameProperty() { return null; }\n"
            + "}\n";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File scratch;

    private List<File> classpath() throws Exception {
        if (scratch == null) {
            scratch = tmp.newFolder("jars");
        }
        return Arrays.asList(RealCompatJars.javafx(scratch), RealCompatJars.jdk(scratch), RealCompatJars.core(scratch));
    }

    private File build() throws Exception {
        File classes = tmp.newFolder();
        CompatFixtures.compileAgainst(classpath(), tmp.newFolder(), classes, "com/acme/fx/Shop.java", APP,
                "com/acme/fx/Person.java", PERSON, "com/acme/fx/Employee.java", EMPLOYEE, "com/acme/fx/Plain.java",
                PLAIN, "com/acme/fx/Other.java", OTHER, "com/acme/fx/Hidden.java", HIDDEN);
        return classes;
    }

    private CompatRemapper remapper(File classes) throws Exception {
        File dir = tmp.newFolder();
        File record = DesktopSources.entryRecord(dir);
        Files.write(record.toPath(), "mainClass=com.acme.fx.Shop\nkind=javafx\n".getBytes("UTF-8"));
        return new CompatRemapper(classes, classpath(), null, CompatRemapperTest.LOG)
                .withDesktopEntryRecord(record).withApplicationMain("com.acme.MyApp");
    }

    @Test
    public void theGeneratedRegistryCallsTheAccessorsOfTheBeans() throws Exception {
        File classes = build();
        assertTrue(remapper(classes).run());
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                PropertyAccessRemapTest.class.getClassLoader());
        try {
            Class<?> accessType = loader.loadClass("com.codename1.fxcompat.rt.PropertyAccess");
            Object access = loader.loadClass("com.codename1.fxcompat.rt.PropertyRegistry").newInstance();
            assertTrue(accessType.isInstance(access));
            Object none = accessType.getField("NONE").get(null);
            Method call = accessType.getMethod("call", Object.class, String.class);
            Method knows = accessType.getMethod("knows", Object.class);

            Object person = loader.loadClass("com.acme.fx.Person").newInstance();
            Object property = call.invoke(access, person, "nameProperty");
            assertNotNull(property);
            assertEquals("com.codename1.fxcompat.javafx.beans.property.SimpleStringProperty",
                    property.getClass().getName());
            assertEquals("Ann", call.invoke(access, person, "getName"));
            assertEquals("a primitive comes back in its wrapper", Integer.valueOf(41),
                    call.invoke(access, person, "getAge"));
            assertEquals(Boolean.TRUE, call.invoke(access, person, "isActive"));
            assertSame("a method with an argument is no accessor", none, call.invoke(access, person, "getWith"));
            assertSame(none, call.invoke(access, person, "getHidden"));
            assertSame(none, call.invoke(access, person, "getShared"));
            assertSame(none, call.invoke(access, person, "getNothing"));
            assertEquals(Boolean.TRUE, knows.invoke(access, person));

            // The subclass is asked first, and has what it inherits.
            Object employee = loader.loadClass("com.acme.fx.Employee").newInstance();
            assertEquals("Employee Ann", call.invoke(access, employee, "getName"));
            assertEquals(Double.valueOf(2.5), call.invoke(access, employee, "getSalary"));
            assertSame(property.getClass(), call.invoke(access, employee, "nameProperty").getClass());
            assertSame(none, call.invoke(access, person, "getSalary"));

            Object plain = loader.loadClass("com.acme.fx.Plain").newInstance();
            assertEquals("a bean of getters is reached by the constant name", "plain",
                    call.invoke(access, plain, "getLabel"));
            Object other = loader.loadClass("com.acme.fx.Other").newInstance();
            assertSame(none, call.invoke(access, other, "getTitle"));
            assertEquals(Boolean.FALSE, knows.invoke(access, other));
            assertEquals(Boolean.FALSE, knows.invoke(access, "a string"));

            // The factory itself, relocated, over the generated registry.
            Class<?> factoryType = loader.loadClass(
                    "com.codename1.fxcompat.javafx.scene.control.cell.PropertyValueFactory");
            Method valueOf = factoryType.getDeclaredMethod("cn1ValueOf", Object.class, String.class);
            valueOf.setAccessible(true);
            assertSame(property, valueOf.invoke(null, person, "name"));
            Object age = valueOf.invoke(null, person, "age");
            assertEquals(Integer.valueOf(41), age.getClass().getMethod("getValue").invoke(age));
            try {
                valueOf.invoke(null, person, "nmae");
                org.junit.Assert.fail("Person has no such property");
            } catch (java.lang.reflect.InvocationTargetException e) {
                String message = String.valueOf(e.getCause().getMessage());
                assertTrue(message, e.getCause() instanceof IllegalStateException);
                assertTrue(message, message.contains("com.acme.fx.Person") && message.contains("nmae"));
            }
            try {
                valueOf.invoke(null, other, "title");
                org.junit.Assert.fail("Nothing was generated for Other");
            } catch (java.lang.reflect.InvocationTargetException e) {
                String message = String.valueOf(e.getCause().getMessage());
                assertTrue(message, message.contains("com.acme.fx.Other") && message.contains("title"));
            }
        } finally {
            loader.close();
        }
        new BytecodeCompliance(RealCompatJars.host(classes, tmp.newFolder(), scratch)).execute();
    }

    @Test
    public void aClassThatIsNotPublicGetsNoAccessorsAndASecondRunChangesNothing() throws Exception {
        File classes = build();
        assertTrue(remapper(classes).run());
        File dir = new File(classes, "com/codename1/fxcompat/rt");
        File registry = new File(dir, "PropertyRegistry.class");
        assertTrue(registry.isFile());
        // Employee, Person and Plain; not Other, not Hidden.
        assertTrue(new File(dir, "PropertyRegistry$P2.class").isFile());
        assertFalse(new File(dir, "PropertyRegistry$P3.class").exists());
        String text = new String(Files.readAllBytes(registry.toPath()), "ISO-8859-1");
        assertFalse(text.contains("com/acme/fx/Hidden"));
        assertFalse(text.contains("com/acme/fx/Other"));

        byte[] first = Files.readAllBytes(registry.toPath());
        long stamp = registry.lastModified();
        assertTrue(remapper(classes).run());
        assertTrue(Arrays.equals(first, Files.readAllBytes(registry.toPath())));
        assertEquals(stamp, registry.lastModified());
        assertFalse(new File(dir, "PropertyRegistry$P3.class").exists());
    }
}
