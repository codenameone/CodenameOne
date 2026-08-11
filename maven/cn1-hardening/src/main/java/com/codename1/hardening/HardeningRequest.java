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
package com.codename1.hardening;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything the engine needs for one run, assembled by the caller. No builder
 * {@code BuildRequest} type crosses this boundary -- the plugin and the daemon each
 * build one of these from their own request object and their resolved hint map, so
 * the engine stays single-sourced across the two repositories.
 */
public final class HardeningRequest {
    private File inputJar;
    private File outputJar;
    private File mappingFile;
    private File reportFile;
    private File r8KeepFile;
    private File workDir;
    private HardeningConfig config;
    private String mainClass;
    private String buildKey = "";
    private final List<File> libraryJars = new ArrayList<File>();

    public File getInputJar() {
        return inputJar;
    }

    public HardeningRequest inputJar(File f) {
        this.inputJar = f;
        return this;
    }

    public File getOutputJar() {
        return outputJar;
    }

    public HardeningRequest outputJar(File f) {
        this.outputJar = f;
        return this;
    }

    public File getMappingFile() {
        return mappingFile;
    }

    public HardeningRequest mappingFile(File f) {
        this.mappingFile = f;
        return this;
    }

    public File getReportFile() {
        return reportFile;
    }

    public HardeningRequest reportFile(File f) {
        this.reportFile = f;
        return this;
    }

    public File getR8KeepFile() {
        return r8KeepFile;
    }

    public HardeningRequest r8KeepFile(File f) {
        this.r8KeepFile = f;
        return this;
    }

    public File getWorkDir() {
        return workDir;
    }

    public HardeningRequest workDir(File f) {
        this.workDir = f;
        return this;
    }

    public HardeningConfig getConfig() {
        return config;
    }

    public HardeningRequest config(HardeningConfig c) {
        this.config = c;
        return this;
    }

    public String getMainClass() {
        return mainClass;
    }

    public HardeningRequest mainClass(String s) {
        this.mainClass = s;
        return this;
    }

    public String getBuildKey() {
        return buildKey;
    }

    public HardeningRequest buildKey(String s) {
        this.buildKey = s == null ? "" : s;
        return this;
    }

    public List<File> getLibraryJars() {
        return libraryJars;
    }

    public HardeningRequest addLibraryJar(File f) {
        if (f != null) {
            libraryJars.add(f);
        }
        return this;
    }
}
