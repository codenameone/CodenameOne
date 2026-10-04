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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The compiled backend test run selects the classes Surefire runs.
class SurefireSelectionTest {
    @Test
    void surefiresDefaultsSelectTestClassesAndSkipHelpersAndNestedClasses() {
        SurefireSelection s = SurefireSelection.of(project(null, null), null);
        assertTrue(s.test("com.acme.ApiTest"));
        assertTrue(s.test("com.acme.TestSupportFlows"));
        assertTrue(s.test("com.acme.ApiTests"));
        assertTrue(s.test("com.acme.ApiTestCase"));
        assertTrue(s.test("ApiTest"), "the default package");
        assertFalse(s.test("com.acme.IntegrationSpec"), "not a name Surefire discovers");
        assertFalse(s.test("com.acme.Fixtures"), "a helper with an @Test method");
        assertFalse(s.test("com.acme.ApiTest$Nested"), "nested classes are excluded");
    }

    @Test
    void thePomsIncludesAndExcludesReplaceTheDefaults() {
        SurefireSelection s = SurefireSelection.of(project(new String[] {"**/*Spec.java"},
                new String[] {"**/slow/**"}), null);
        assertTrue(s.test("com.acme.IntegrationSpec"));
        assertFalse(s.test("com.acme.ApiTest"), "the pom's includes replace the defaults");
        assertFalse(s.test("com.acme.slow.LoadSpec"), "excluded by the pom");
        assertTrue(s.test("com.acme.ApiTest$NestedSpec"), "the pom's excludes replace the defaults");
    }

    @Test
    void dashDTestSelectsByClassName() {
        SurefireSelection s = SurefireSelection.of(project(null, null), "ApiTest#greets, Other*");
        assertTrue(s.test("com.acme.ApiTest"));
        assertTrue(s.test("com.acme.deep.OtherTests"));
        assertFalse(s.test("com.acme.ServedApiTest"));
    }

    @Test
    void aMethodSelectorNarrowsItsClassToThoseMethods() {
        SurefireSelection s = SurefireSelection.of(project(null, null),
                "ApiTest#greets+count*, Other*, !OtherTests#slow");
        assertTrue(s.test("com.acme.ApiTest#greets"));
        assertTrue(s.test("com.acme.ApiTest#countsVisits"), "a * in a method selector");
        assertFalse(s.test("com.acme.ApiTest#deletes"), "a method the selector does not name");
        assertTrue(s.test("com.acme.deep.OtherTests#fast"), "a class named whole runs every method");
        assertFalse(s.test("com.acme.deep.OtherTests#slow"), "a ! method selector leaves it out");
        assertFalse(s.test("com.acme.ServedApiTest#greets"), "a class the run does not select");
        assertTrue(SurefireSelection.of(project(null, null), null).test("com.acme.ApiTest#anything"),
                "with no -Dtest every method of a selected class runs");
    }

    @Test
    void aRegexIncludeSelectsAsSurefireDoes() {
        SurefireSelection s = SurefireSelection.of(
                project(new String[] {"%regex[.*(Cat|Dog).*Test.*]"}, null), null);
        assertTrue(s.test("com.acme.CatFoodTest"));
        assertTrue(s.test("com.acme.deep.DogWalkTest"));
        assertFalse(s.test("com.acme.BirdTest"), "a class the regex does not match");
        SurefireSelection dashD = SurefireSelection.of(project(null, null), "%regex[.*Cat.*]#feeds");
        assertTrue(dashD.test("com.acme.CatTest#feeds"));
        assertFalse(dashD.test("com.acme.CatTest#sleeps"));
        assertFalse(dashD.test("com.acme.DogTest#feeds"));
    }

    @Test
    void theDefaultTestExecutionsFiltersApply() {
        MavenProject project = new MavenProject(new Model());
        project.getModel().setBuild(new Build());
        Plugin surefire = new Plugin();
        surefire.setGroupId("org.apache.maven.plugins");
        surefire.setArtifactId("maven-surefire-plugin");
        org.apache.maven.model.PluginExecution execution = new org.apache.maven.model.PluginExecution();
        execution.setId("default-test");
        Xpp3Dom config = new Xpp3Dom("configuration");
        Xpp3Dom includes = new Xpp3Dom("includes");
        Xpp3Dom include = new Xpp3Dom("include");
        include.setValue("**/*Spec.java");
        includes.addChild(include);
        config.addChild(includes);
        execution.setConfiguration(config);
        surefire.addExecution(execution);
        project.getBuild().addPlugin(surefire);
        SurefireSelection s = SurefireSelection.of(project, null);
        assertTrue(s.test("com.acme.ApiSpec"), "the execution's include was not read");
        assertFalse(s.test("com.acme.ApiTest"), "the execution's includes replace the defaults");
    }

    @Test
    void aNegativeDashDTestEntryExcludes() {
        SurefireSelection s = SurefireSelection.of(project(null, null), "*Test, !SlowTest");
        assertTrue(s.test("com.acme.ApiTest"));
        assertFalse(s.test("com.acme.SlowTest"), "-Dtest excluded it");
        SurefireSelection only = SurefireSelection.of(project(null, null), "!SlowTest");
        assertTrue(only.test("com.acme.ApiTest"), "only exclusions keep the default includes");
        assertFalse(only.test("com.acme.SlowTest"));
    }

    private static MavenProject project(String[] includes, String[] excludes) {
        Model model = new Model();
        model.setBuild(new Build());
        MavenProject project = new MavenProject(model);
        if (includes != null || excludes != null) {
            Plugin surefire = new Plugin();
            surefire.setGroupId("org.apache.maven.plugins");
            surefire.setArtifactId("maven-surefire-plugin");
            Xpp3Dom config = new Xpp3Dom("configuration");
            config.addChild(list("includes", "include", includes));
            config.addChild(list("excludes", "exclude", excludes));
            surefire.setConfiguration(config);
            model.getBuild().addPlugin(surefire);
        }
        return project;
    }

    private static Xpp3Dom list(String name, String item, String[] values) {
        Xpp3Dom parent = new Xpp3Dom(name);
        for (String v : values == null ? new String[0] : values) {
            Xpp3Dom child = new Xpp3Dom(item);
            child.setValue(v);
            parent.addChild(child);
        }
        return parent;
    }
}
