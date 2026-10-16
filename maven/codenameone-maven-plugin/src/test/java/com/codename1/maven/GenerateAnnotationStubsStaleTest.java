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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A stub the goal wrote for a contract that has since gone is deleted by the
/// next run, and nothing else in the directory is: it is a compile source
/// root that another goal writes into as well.
class GenerateAnnotationStubsStaleTest {

    @Test
    void aStubNoLongerEmittedIsDeletedAndOtherFilesAreKept(@TempDir File tmp) throws Exception {
        GenerateAnnotationStubsMojo mojo = new GenerateAnnotationStubsMojo();
        mojo.stubSourceDirectory = new File(tmp, "generated-sources/cn1-annotations");
        File pkg = new File(mojo.stubSourceDirectory, "com/example");
        assertTrue(pkg.mkdirs());
        File kept = new File(pkg, "KeptApi.java");
        File removed = new File(pkg, "RemovedApi.java");
        File someoneElses = new File(pkg, "WrittenByAnotherGoal.java");
        for (File f : new File[] {kept, removed, someoneElses}) {
            assertTrue(f.createNewFile());
        }

        Map<String, String> first = new LinkedHashMap<String, String>();
        first.put("com/example/KeptApi", "");
        first.put("com/example/RemovedApi", "");
        mojo.removeStubsNoLongerEmitted(first);
        assertTrue(removed.isFile(), "emitted by this run");

        Map<String, String> second = new LinkedHashMap<String, String>();
        second.put("com/example/KeptApi", "");
        mojo.removeStubsNoLongerEmitted(second);
        assertFalse(removed.exists(), "its contract is gone, and so is its stub");
        assertTrue(kept.isFile());
        assertTrue(someoneElses.isFile(), "not this goal's to delete");
    }
}
