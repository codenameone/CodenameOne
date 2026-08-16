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
package com.codename1.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A named set of accessory states that can be applied in one go: HomeKit's
/// `HMActionSet`, a Google Home scene.
///
/// Run one with [SmartHome#executeScene(Scene)].
///
/// #### Scenes, and not automations
///
/// A scene is a list of things to set. An automation is a scene plus a
/// *trigger* -- a time, a sensor crossing a threshold, someone arriving -- and
/// the three ecosystems model triggers in three incompatible ways: HomeKit has
/// `HMTimerTrigger`, `HMEventTrigger` and `HMCharacteristicThreshold`, Google
/// has its own automations language, and Matter has nothing at all. There is
/// no honest common shape, so this release exposes scenes and
/// [SmartHome#isAutomationSupported()] answers `false` everywhere.
///
/// An immutable snapshot.
public final class Scene {

    private final String id;
    private final String name;
    private final String structureId;
    private final SceneType type;
    private final boolean executable;
    private final List<SceneAction> actions;

    /// Creates a scene snapshot. Called by the ports and by the local home.
    ///
    /// #### Parameters
    ///
    /// - `id`: the scene identifier
    ///
    /// - `name`: the user-visible name, or `null` for none
    ///
    /// - `structureId`: the structure this scene belongs to
    ///
    /// - `type`: what kind of scene it is; `null` becomes
    ///   [SceneType#USER_DEFINED]
    ///
    /// - `executable`: whether it can be run from here
    ///
    /// - `actions`: what it does; `null` becomes empty, which is also what a
    ///   backend that will not enumerate a scene's contents produces
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when `id` is `null` or empty
    public Scene(String id, String name, String structureId, SceneType type,
            boolean executable, List<SceneAction> actions) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("scene id is required");
        }
        this.id = id;
        this.name = name == null ? "" : name;
        this.structureId = structureId;
        this.type = type == null ? SceneType.USER_DEFINED : type;
        this.executable = executable;
        if (actions == null || actions.isEmpty()) {
            this.actions = Collections.<SceneAction>emptyList();
        } else {
            this.actions = Collections.unmodifiableList(
                    new ArrayList<SceneAction>(actions));
        }
    }

    /// The identifier this scene is addressed by.
    ///
    /// #### Returns
    ///
    /// the identifier, never `null`
    public String getId() {
        return id;
    }

    /// The user-visible name, empty when the scene has none. The user's own
    /// text; treat it as untrusted beyond display.
    ///
    /// #### Returns
    ///
    /// the name, never `null`
    public String getName() {
        return name;
    }

    /// The structure this scene belongs to.
    ///
    /// #### Returns
    ///
    /// the structure identifier, or `null` when unknown
    public String getStructureId() {
        return structureId;
    }

    /// What kind of scene this is.
    ///
    /// #### Returns
    ///
    /// the type, never `null`
    public SceneType getType() {
        return type;
    }

    /// Whether this scene can be run with [SmartHome#executeScene(Scene)].
    ///
    /// `false` for a [SceneType#TRIGGER_OWNED] scene, and for one the user's
    /// permissions on this home do not let them run. Calling
    /// `executeScene` anyway fails with [HomeError#UNAUTHORIZED] rather than
    /// quietly doing nothing, but checking first is how you avoid offering a
    /// button that cannot work.
    ///
    /// #### Returns
    ///
    /// `true` when the scene can be run
    public boolean isExecutable() {
        return executable;
    }

    /// What this scene does.
    ///
    /// **Empty is not the same as "does nothing".** Some backends will run a
    /// scene without enumerating its contents, so an empty list means the
    /// platform did not say. Do not render "this scene is empty" from it.
    ///
    /// #### Returns
    ///
    /// an immutable list, possibly empty
    public List<SceneAction> getActions() {
        return actions;
    }

    @Override
    public String toString() {
        return "Scene[" + id + (name.length() > 0 ? " " + name : "") + " "
                + type.name() + (executable ? "" : " not-executable") + "]";
    }
}
