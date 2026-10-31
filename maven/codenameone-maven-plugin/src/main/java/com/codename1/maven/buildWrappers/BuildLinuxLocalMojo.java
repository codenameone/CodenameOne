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
package com.codename1.maven.buildWrappers;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Builds the native Linux executable on this machine, with no cloud build:
 * {@code mvn cn1:buildLinuxLocal}. With {@code -Dcn1.board=rpi5} (or any board
 * profile) it is an embedded build for that board, cross-compiled for its
 * architecture. The executable is left in {@code linux/target/linux-native}, where
 * {@code cn1:deploy-device} finds it.
 */
@Mojo(name = "buildLinuxLocal", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildLinuxLocalMojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        return "linux";
    }

    @Override
    protected String getBuildTarget() {
        return "local-linux-device";
    }
}
