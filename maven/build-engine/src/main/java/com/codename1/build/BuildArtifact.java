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
package com.codename1.build;

import java.io.File;
import java.util.Collections;
import java.util.List;

/// One resolved dependency of the project being built, as a build tool
/// resolved it. The getters are named like Maven's `Artifact` so engine code
/// reads the same whichever tool supplied it.
public final class BuildArtifact {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;
    private final String type;
    private final String scope;
    private final File file;
    private final List<String> dependencyTrail;

    /// A resolved dependency.
    ///
    /// @param classifier null when there is none
    /// @param scope `compile`, `runtime`, `provided`, ... or null when the build
    ///        tool has no notion of one
    /// @param file the resolved file, or null when not resolved
    /// @param dependencyTrail the chain of `groupId:artifactId:type:version`
    ///        ids that brought it in, root first, or null when unknown
    public BuildArtifact(String groupId, String artifactId, String version, String classifier,
                         String type, String scope, File file, List<String> dependencyTrail) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier == null || classifier.length() == 0 ? null : classifier;
        this.type = type == null ? "jar" : type;
        this.scope = scope;
        this.file = file;
        this.dependencyTrail = dependencyTrail == null ? null : Collections.unmodifiableList(dependencyTrail);
    }

    /// The group id.
    public String getGroupId() {
        return groupId;
    }

    /// The artifact id.
    public String getArtifactId() {
        return artifactId;
    }

    /// The version.
    public String getVersion() {
        return version;
    }

    /// The classifier, or null.
    public String getClassifier() {
        return classifier;
    }

    /// The type, `jar` unless said otherwise.
    public String getType() {
        return type;
    }

    /// The scope, or null.
    public String getScope() {
        return scope;
    }

    /// The resolved file, or null.
    public File getFile() {
        return file;
    }

    /// The dependency trail, or null.
    public List<String> getDependencyTrail() {
        return dependencyTrail;
    }

    /// `groupId:artifactId:type:version`, the form trail entries use.
    public String getId() {
        return groupId + ":" + artifactId + ":" + type + ":" + version;
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + (classifier == null ? "" : ":" + classifier) + ":" + version;
    }
}
