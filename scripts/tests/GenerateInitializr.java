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
  }
  System.exit(0);
 }
}
