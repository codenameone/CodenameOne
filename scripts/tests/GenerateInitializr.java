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
package com.codename1.initializr.model;
import java.nio.file.*;
import java.io.*;
import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.Display;
public class GenerateInitializr {
 // Initialize resource lookup only; no simulator window, browser or build account.
 public static void main(String[] args) throws Exception {
  java.lang.reflect.Field impl=Display.class.getDeclaredField("impl");
  impl.setAccessible(true); impl.set(null, new JavaSEPort());
  Files.createDirectories(Paths.get(args[0]));
  for (IDE ide : IDE.values()) {
   for (ProjectOptions.JavaVersion version : ProjectOptions.JavaVersion.values()) {
    ProjectOptions options = new ProjectOptions(ProjectOptions.ThemeMode.LIGHT,
      ProjectOptions.Accent.DEFAULT, true, false, ProjectOptions.PreviewLanguage.ENGLISH, version);
    GeneratorModel model=GeneratorModel.create(ide,Template.BAREBONES,"LauncherProbe","com.example.probe",options);
    try(OutputStream out=Files.newOutputStream(Paths.get(args[0],ide.name()+"-"+version.name()+".zip"))) {
     model.writeProjectZip(out);
    }
   }
   // Gradle downloads are Java 17 only; one per project type. Named GRADLE-* so
   // the fixture script tells them from the Maven ones above.
   for (ProjectOptions.ProjectType type : ProjectOptions.ProjectType.values()) {
    ProjectOptions options = ProjectOptions.defaults().withBuild(ProjectOptions.BuildTool.GRADLE, type);
    GeneratorModel model=GeneratorModel.create(ide,Template.BAREBONES,"LauncherProbe","com.example.probe",options);
    try(OutputStream out=Files.newOutputStream(Paths.get(args[0],"GRADLE-"+type.name()+"-"+ide.name()+".zip"))) {
     model.writeProjectZip(out);
    }
   }
   // The Maven layouts, generated against the first plugin that has them whatever
   // release this checkout is on. MAVEN-FULL is every platform module plus backend/.
   String[] layouts = {"APP", "APP_WITH_BACKEND", "BACKEND_ONLY", "FULL"};
   for (String layout : layouts) {
    boolean full = "FULL".equals(layout);
    ProjectOptions.ProjectType type = full ? ProjectOptions.ProjectType.APP_WITH_BACKEND
      : ProjectOptions.ProjectType.valueOf(layout);
    ProjectOptions options = ProjectOptions.defaults().withBuild(ProjectOptions.BuildTool.MAVEN, type)
      .withPlatformModules(full);
    GeneratorModel model=GeneratorModel.createForPluginVersion(ide,Template.BAREBONES,"LauncherProbe",
      "com.example.probe",options,GeneratorModel.MAVEN_LAYOUTS_SINCE);
    try(OutputStream out=Files.newOutputStream(Paths.get(args[0],"MAVEN-"+layout+"-"+ide.name()+".zip"))) {
     model.writeProjectZip(out);
    }
   }
   // The full-stack template, in both of its layouts and in a colour scheme of
   // its own with an icon: an app, its server and the module they share.
   for (boolean full : new boolean[] {false, true}) {
    ProjectOptions options = ProjectOptions.defaults().forFullStack().withPlatformModules(full)
      .withScheme(0x0f766e, false).withIcon(new byte[] {(byte)0x89, 'P', 'N', 'G'});
    GeneratorModel model=GeneratorModel.createForPluginVersion(ide,Template.WAYLINE,"LauncherProbe",
      "com.example.probe",options,GeneratorModel.FULL_STACK_SINCE);
    try(OutputStream out=Files.newOutputStream(Paths.get(args[0],
      "WAYLINE-"+(full ? "FULL" : "APP_WITH_BACKEND")+"-"+ide.name()+".zip"))) {
     model.writeProjectZip(out);
    }
   }
  }
  System.exit(0);
 }
}
