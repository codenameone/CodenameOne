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
package com.codename1.unitycompat.unityengine.events;

import com.codename1.unitycompat.unityengine.UnityRuntime;
import java.util.ArrayList;

/// `UnityEngine.Events.UnityEventBase`: the listeners of an event.
///
/// There are two kinds, and Unity keeps them apart. A *persistent*
/// listener is one set up in the editor -- the row in a button's
/// "On Click ()" list naming an object, one of its methods and perhaps an
/// argument -- and is part of the scene file. A listener added from a
/// script with `AddListener` is not, and `RemoveAllListeners` removes
/// only those.
///
/// A persistent listener cannot be looked up by name at run time: nothing
/// here has reflection. The scene compiler, which has the compiled
/// scripts in front of it, writes each as a [Call] that makes the one
/// call the row describes, and keeps the object it is made on beside it so
/// that copying the event's owner can redirect it.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public abstract class UnityEventBase {
    /// One persistent listener: what to do with its target.
    public interface Call {
        void call(Object target);
    }

    /// Targets and calls in turn.
    private final ArrayList persistent = new ArrayList();
    final ArrayList listeners = new ArrayList();

    /// Adds a persistent listener. Generated scene code calls this.
    public void $persistent(Object target, Call call) {
        persistent.add(target);
        persistent.add(call);
    }

    /// Takes the persistent listeners of the event this one is a copy of.
    /// A target inside what is being copied becomes its copy, as a
    /// reference in a script's field does.
    public void $copyFrom(UnityEventBase source) {
        persistent.clear();
        for (int i = 0; i + 1 < source.persistent.size(); i += 2) {
            persistent.add(UnityRuntime.$remap(source.persistent.get(i)));
            persistent.add(source.persistent.get(i + 1));
        }
    }

    public int GetPersistentEventCount() {
        return persistent.size() / 2;
    }

    public void RemoveAllListeners() {
        listeners.clear();
    }

    /// Calls the persistent listeners, in the order the editor lists
    /// them. One whose target has been destroyed is passed over, as Unity
    /// passes over a row whose object is missing.
    void invokePersistent() {
        // The size is read each time round: a listener may load a scene
        // or destroy the event's owner, and must not strand the loop.
        for (int i = 0; i + 1 < persistent.size(); i += 2) {
            Object target = persistent.get(i);
            if (target instanceof com.codename1.unitycompat.unityengine.Object
                    && !com.codename1.unitycompat.unityengine.Object.op_Implicit(
                            (com.codename1.unitycompat.unityengine.Object) target)) {
                continue;
            }
            ((Call) persistent.get(i + 1)).call(target);
        }
    }
}
