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

import com.codename1.unitycompat.system.Delegate;

/// `UnityEngine.Events.UnityEvent`: an event of no arguments, such as a
/// button's `onClick`.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class UnityEvent extends UnityEventBase {
    /// Unity's documentation: adding identical listeners several times
    /// results in one call to that listener. Identical is what C# calls
    /// equal -- the same method on the same object -- so a second
    /// `AddListener(Hear)` is a new delegate object and still not added.
    public void AddListener(UnityAction call) {
        if (call == null) {
            return;
        }
        for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach
            if (listeners.get(i).equals(call)) {
                return;
            }
        }
        listeners.add(call);
    }

    public void RemoveListener(UnityAction call) {
        if (call == null) {
            return;
        }
        // Two delegates over the same method of the same object are equal
        // though they are two objects, which is what lets a script remove
        // a listener it did not keep.
        for (int i = listeners.size() - 1; i >= 0; i--) {
            if (listeners.get(i).equals(call)) {
                listeners.remove(i);
            }
        }
    }

    public void Invoke() {
        invokePersistent();
        // A copy, because a listener may add or remove one.
        Object[] now = listeners.toArray();
        for (int i = 0; i < now.length; i++) { // NOPMD ForLoopCanBeForeach
            if (!(now[i] instanceof UnityAction)) {
                continue;
            }
            UnityAction a = (UnityAction) now[i];
            Delegate[] list = a.$list;
            if (list == null) {
                if (live(a)) {
                    a.Invoke();
                }
                continue;
            }
            // A delegate joined from several: each is passed over or
            // called by itself, in order, as each would be on its own.
            for (int j = 0; j < list.length; j++) { // NOPMD ForLoopCanBeForeach
                if (live(list[j]) && list[j] instanceof UnityAction) {
                    ((UnityAction) list[j]).Invoke();
                }
            }
        }
    }

    /// Unity's manual, of events: when the target is a `UnityEngine.Object`
    /// whose native object has been destroyed, the callback is not invoked.
    /// That is said of every listener, not only the editor's, so a
    /// listener added in code over a method of a destroyed object is
    /// passed over as a persistent one is.
    private static boolean live(Delegate d) {
        Object target = d.$target;
        return !(target instanceof com.codename1.unitycompat.unityengine.Object)
                || com.codename1.unitycompat.unityengine.Object.op_Implicit(
                        (com.codename1.unitycompat.unityengine.Object) target);
    }
}
