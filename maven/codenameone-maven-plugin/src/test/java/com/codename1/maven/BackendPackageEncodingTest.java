/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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


import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The packaging goal recompiles the module's own sources, so it has to read
/// them the way the lifecycle compile already did. Forcing UTF-8 made it a
/// second, disagreeing compilation of one tree: a module declaring another
/// encoding either fails here on bytes javac accepted a phase earlier, or
/// translates string literals that are not the ones the JVM build produced.
class BackendPackageEncodingTest {

    @Test
    void defaultsToUtf8WhenTheModuleSaysNothing() throws Exception {
        assertEquals("UTF-8", encodingOf(project(null, null)),
                "the platform default is deliberately not the fallback: it makes "
                        + "the build depend on the machine that runs it");
    }

    @Test
    void takesTheProjectProperty() throws Exception {
        assertEquals("ISO-8859-1", encodingOf(project("ISO-8859-1", null)),
                "project.build.sourceEncoding is what the compiler plugin itself "
                        + "defaults to, so it is what the lifecycle compile used");
    }

    @Test
    void prefersAnExplicitCompilerPluginEncoding() throws Exception {
        assertEquals("Cp1252", encodingOf(project("ISO-8859-1", "Cp1252")),
                "an explicit <encoding> overrides the property for the compiler "
                        + "plugin, so it has to override it here as well");
    }

    @Test
    void ignoresAConfigurationThatIsStillAPropertyReference() throws Exception {
        // Maven interpolates the configuration it hands a plugin; one that still
        // reads ${...} here names no charset, and javac would refuse it.
        assertEquals("ISO-8859-1",
                encodingOf(project("ISO-8859-1", "${project.build.sourceEncoding}")),
                "an uninterpolated reference falls through to the property");
    }

    @Test
    void ignoresAnEmptyCompilerPluginEncoding() throws Exception {
        assertEquals("UTF-8", encodingOf(project(null, "   ")),
                "and so does a blank one");
    }

    private static MavenProject project(String property, String pluginEncoding) {
        Model model = new Model();
        model.setBuild(new Build());
        MavenProject project = new MavenProject(model);
        if (property != null) {
            Properties properties = new Properties();
            properties.setProperty("project.build.sourceEncoding", property);
            model.setProperties(properties);
        }
        if (pluginEncoding != null) {
            Plugin compiler = new Plugin();
            compiler.setGroupId("org.apache.maven.plugins");
            compiler.setArtifactId("maven-compiler-plugin");
            Xpp3Dom configuration = new Xpp3Dom("configuration");
            Xpp3Dom encoding = new Xpp3Dom("encoding");
            encoding.setValue(pluginEncoding);
            configuration.addChild(encoding);
            compiler.setConfiguration(configuration);
            model.getBuild().addPlugin(compiler);
        }
        return project;
    }

    private static String encodingOf(MavenProject project) throws Exception {
        BackendPackageMojo mojo = new BackendPackageMojo();
        Field field = BackendPackageMojo.class.getDeclaredField("project");
        field.setAccessible(true);
        field.set(mojo, project);
        Method method = BackendPackageMojo.class.getDeclaredMethod("sourceEncoding");
        method.setAccessible(true);
        return (String) method.invoke(mojo);
    }
}
