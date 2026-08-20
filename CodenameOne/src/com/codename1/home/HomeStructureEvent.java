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

/// Notification that the home graph moved, delivered to a
/// [HomeStructureListener] on the EDT.
public final class HomeStructureEvent {

    private final StructureChangeKind kind;
    private final String structureId;
    private final String accessoryId;

    /// Creates an event. Called by the ports and by the local home.
    ///
    /// #### Parameters
    ///
    /// - `kind`: what changed; `null` becomes
    ///   [StructureChangeKind#STRUCTURES_CHANGED], the conservative answer,
    ///   because it is the one that makes a listener re-read everything
    ///
    /// - `structureId`: the home affected, or `null` when the change is not
    ///   scoped to one
    ///
    /// - `accessoryId`: the accessory affected, or `null`
    public HomeStructureEvent(StructureChangeKind kind, String structureId,
            String accessoryId) {
        this.kind = kind == null ? StructureChangeKind.STRUCTURES_CHANGED
                : kind;
        this.structureId = structureId;
        this.accessoryId = accessoryId;
    }

    /// What changed.
    ///
    /// #### Returns
    ///
    /// the kind, never `null`
    public StructureChangeKind getKind() {
        return kind;
    }

    /// The home this is about.
    ///
    /// #### Returns
    ///
    /// the structure identifier, or `null` when the change is not scoped to
    /// one home
    public String getStructureId() {
        return structureId;
    }

    /// The accessory this is about.
    ///
    /// #### Returns
    ///
    /// the accessory identifier, or `null` when the change is not about one
    /// accessory
    public String getAccessoryId() {
        return accessoryId;
    }

    @Override
    public String toString() {
        return kind.name() + (structureId != null ? " home=" + structureId : "")
                + (accessoryId != null ? " accessory=" + accessoryId : "");
    }
}
