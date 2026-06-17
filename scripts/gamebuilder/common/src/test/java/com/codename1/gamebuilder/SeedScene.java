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
package com.codename1.gamebuilder;

import com.codename1.gamebuilder.editor.StarterPacks;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Dev helper: writes the demo platformer level as a {@code .game} file (pure, no
/// Display) so a sandbox project has real content for interactive testing. The output
/// path comes from the {@code seed.out} system property.
public final class SeedScene {
    public static void main(String[] args) throws Exception {
        String out = System.getProperty("seed.out");
        if (out == null) {
            throw new IllegalArgumentException("set -Dseed.out=<path/to/Scene.game>");
        }
        Path path = Path.of(out);
        Files.createDirectories(path.getParent());
        Files.writeString(path, StarterPacks.demoLevel().toJson(), StandardCharsets.UTF_8);
        System.out.println("[SeedScene] wrote " + path.toAbsolutePath());
    }
}
