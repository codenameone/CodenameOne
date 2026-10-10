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
package com.codename1.unitycompat.system;

/// `System.Action`3`. Type arguments are erased, so every parameter is an
/// `Object` and the generated subclass unboxes what the method it calls
/// takes as a primitive.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public abstract class Action_3 extends MulticastDelegate {
    public abstract void Invoke(Object a0, Object a1, Object a2);

    @Override
    protected Delegate $multi(Delegate[] list) {
        return new Multi(list);
    }

    private static final class Multi extends Action_3 {
        Multi(Delegate[] list) {
            $list = list;
        }

        @Override
        public void Invoke(Object a0, Object a1, Object a2) {
            for (int i = 0; i < $list.length; i++) {
                ((Action_3) $list[i]).Invoke(a0, a1, a2);
            }
        }
    }
}
