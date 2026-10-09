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
package com.codename1.unitycompat.unityengine;

/// `UnityEngine.Component`: something attached to a [GameObject].
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Component extends Object {
    GameObject gameObject;

    public GameObject get_gameObject() {
        return gameObject;
    }

    public Transform get_transform() {
        return gameObject.transform;
    }

    /// A component has no name of its own: it answers with its object's.
    @Override
    public String get_name() {
        return gameObject == null ? name : gameObject.name;
    }

    @Override
    public void set_name(String value) {
        if (gameObject == null) {
            name = value;
        } else {
            gameObject.name = value;
        }
    }

    public String get_tag() {
        return gameObject.tag;
    }

    public void set_tag(String value) {
        gameObject.tag = value;
    }

    public boolean CompareTag(String other) {
        return gameObject.tag.equals(other);
    }

    public java.lang.Object GetComponent(Class type) {
        return gameObject.GetComponent(type);
    }

    public java.lang.Object GetComponentInChildren(Class type) {
        return gameObject.GetComponentInChildren(type);
    }

    public java.lang.Object GetComponentInParent(Class type) {
        return gameObject.GetComponentInParent(type);
    }

    public java.lang.Object[] GetComponents(Class type) { // NOPMD UnnecessaryFullyQualifiedName
        return gameObject.GetComponents(type);
    }

    public java.lang.Object[] GetComponentsInChildren(Class type) { // NOPMD UnnecessaryFullyQualifiedName
        return gameObject.GetComponentsInChildren(type);
    }

    // ------------------------------------------------------------ the engine
    //
    // What follows is for the runtime and for generated code. The names
    // start with a dollar sign because C# cannot declare one, so nothing a
    // script defines can collide with them.

    /// A new, unattached component of the same class, or null for a class
    /// that cannot be copied. Scripts get an override from the translator;
    /// this is how `Instantiate` copies an object with no reflection.
    public Component $new() {
        return null;
    }

    /// Takes on the serialized state of `source`, a component of the same
    /// class. A reference to something inside the hierarchy being copied
    /// must go through [UnityRuntime#$remap] so that it lands on the copy.
    public void $copyFrom(Component source) {
    }

    /// The object this is attached to has entered the scene.
    /// [#$roles()]: advanced once a frame, after every `Update`.
    public static final int TICKS = 1;
    /// [#$roles()]: advanced once a frame, after every `LateUpdate`.
    public static final int TICKS_LATE = 2;
    /// [#$roles()]: draws in the world.
    public static final int DRAWS = 4;

    /// What the runtime does with this component beyond keeping it: a sum
    /// of [#TICKS], [#TICKS_LATE] and [#DRAWS]. Asked once, when the
    /// component joins the scene.
    public int $roles() {
        return 0;
    }

    /// The step of a component that [#TICKS]: `dt` is the frame's scaled
    /// time. Called only while the component is live.
    public void $tick(float dt) {
    }

    /// The step of a component that [#TICKS_LATE].
    public void $lateTick(float dt) {
    }

    /// Adds what a component that [#DRAWS] shows to the frame's draw list.
    public void $draw(DrawView view) {
    }

    /// Whether the component is in a scene, on an active object, and, for a
    /// [Behaviour], enabled.
    public boolean $live() {
        return live();
    }

    void registered() {
    }

    /// The component, or its object, has left the scene for good.
    void unregistered() {
    }

    /// True while this takes part in the scene: not destroyed, switched on
    /// where it has a switch, and on an object that is active.
    boolean live() {
        return !destroyed && gameObject != null && gameObject.registered && gameObject.activeInHierarchy();
    }
}
