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
package com.codename1.maven.buildWrappers;

import java.util.Properties;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Sends the native Linux build to the cloud as a Steam depot: {@code mvn cn1:buildSteam}.
 * It is the {@code linux-device} build with the {@code steam.package} hint on. The build server supplies
 * the Steamworks SDK, and with a Steam submission configured uploads the depot to
 * the app named by {@code steam.appId}.
 */
@Mojo(name = "buildSteam", requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class BuildSteamMojo extends AbstractBuildWrapperMojo {
    @Override
    protected String getPlatform() {
        return "linux";
    }

    @Override
    protected String getBuildTarget() {
        return "linux-device";
    }

    @Override
    protected void addProperties(Properties properties) {
        properties.setProperty("codename1.arg.steam.package", "true");
    }
}
