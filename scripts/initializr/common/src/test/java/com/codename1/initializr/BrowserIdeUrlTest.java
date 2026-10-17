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
package com.codename1.initializr;

import com.codename1.initializr.model.ProjectOptions;
import com.codename1.initializr.model.Template;
import com.codename1.testing.AbstractTest;

/// The "Open in browser" link: the browser IDE creates the project from these.
public class BrowserIdeUrlTest extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        assertEqual("https://cloud.codenameone.com/ide/?new=app&name=MyApp&package=com.example.myapp&build=maven",
                Initializr.browserIdeUrl("MyApp", "com.example.myapp", Template.BAREBONES,
                        ProjectOptions.ProjectType.APP, ProjectOptions.BuildTool.MAVEN));
        String gradle = Initializr.browserIdeUrl("MyApp", "com.example.myapp", Template.BAREBONES,
                ProjectOptions.ProjectType.BACKEND_ONLY, ProjectOptions.BuildTool.GRADLE);
        assertTrue(gradle.contains("new=backend") && gradle.endsWith("&build=gradle"), gradle);
        assertTrue(Initializr.browserIdeUrl("MyApp", "com.example.myapp", Template.BAREBONES,
                ProjectOptions.ProjectType.APP_WITH_BACKEND, ProjectOptions.BuildTool.MAVEN).contains("new=fullstack"));
        // Full-stack is Maven-only, even if Gradle was picked before switching template.
        assertTrue(Initializr.browserIdeUrl("MyApp", "com.example.myapp", Template.BAREBONES,
                ProjectOptions.ProjectType.APP_WITH_BACKEND, ProjectOptions.BuildTool.GRADLE).endsWith("&build=maven"));
        // Names are encoded, so nothing can add its own parameters.
        assertTrue(Initializr.browserIdeUrl("A&b=c", "com.example.myapp", Template.BAREBONES,
                ProjectOptions.ProjectType.APP, ProjectOptions.BuildTool.MAVEN).contains("name=A%26b%3Dc"));
        return true;
    }
}
