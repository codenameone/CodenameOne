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

/** Outcome of a hardening run. When skipped, {@link #getHardenedJar()} is the original input. */
public final class HardeningResult {

    public enum Outcome {
        HARDENED,
        SKIPPED_NOT_REQUESTED,
        SKIPPED_PLATFORM_DISABLED
    }

    private final Outcome outcome;
    private final File hardenedJar;
    private final File mappingFile;
    private final List<String> warnings = new ArrayList<String>();
    private final List<String> transformsApplied = new ArrayList<String>();
    private int classesIn;
    private int classesOut;
    private int renamedClasses;
    private int encryptedStrings;
    private String mappingId = "";

    private HardeningResult(Outcome outcome, File hardenedJar, File mappingFile) {
        this.outcome = outcome;
        this.hardenedJar = hardenedJar;
        this.mappingFile = mappingFile;
    }

    public static HardeningResult skipped(Outcome outcome, File inputJar) {
        return new HardeningResult(outcome, inputJar, null);
    }

    public static HardeningResult hardened(File hardenedJar, File mappingFile) {
        return new HardeningResult(Outcome.HARDENED, hardenedJar, mappingFile);
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public boolean isHardened() {
        return outcome == Outcome.HARDENED;
    }

    public File getHardenedJar() {
        return hardenedJar;
    }

    public File getMappingFile() {
        return mappingFile;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getTransformsApplied() {
        return transformsApplied;
    }

    public int getClassesIn() {
        return classesIn;
    }

    public void setClassesIn(int classesIn) {
        this.classesIn = classesIn;
    }

    public int getClassesOut() {
        return classesOut;
    }

    public void setClassesOut(int classesOut) {
        this.classesOut = classesOut;
    }

    public int getRenamedClasses() {
        return renamedClasses;
    }

    public void setRenamedClasses(int renamedClasses) {
        this.renamedClasses = renamedClasses;
    }

    public int getEncryptedStrings() {
        return encryptedStrings;
    }

    public void setEncryptedStrings(int encryptedStrings) {
        this.encryptedStrings = encryptedStrings;
    }

    public String getMappingId() {
        return mappingId;
    }

    public void setMappingId(String mappingId) {
        this.mappingId = mappingId;
    }
}
