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

import com.codename1.gaming.physics.box2d.dynamics.Body;
import java.util.ArrayList;

/// `UnityEngine.GameObject`: a named bag of components with a transform.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class GameObject extends Object {
    final ArrayList components = new ArrayList();
    final Transform transform;
    boolean active = true;
    String tag = "Untagged";
    int layer;
    /// In the scene: seen by the player loop, by rendering and by the
    /// `Find` family. False for an object still being built by
    /// `Instantiate`, and for good for the objects of a prefab asset.
    boolean registered;
    /// Part of a prefab asset: a template that is copied and never runs.
    boolean asset;
    /// For the root of a prefab asset, which generated factory builds it.
    int prefabIndex = -1;
    /// The Box2D body of this object: its [Rigidbody2D]'s, or the static
    /// one that carries its colliders when it has no rigidbody above it.
    Body body;
    /// The world pose the body was last given or last reported. A transform
    /// that no longer matches was moved by a script, and the body follows.
    float syncX;
    float syncY;
    float syncRotation;
    /// The body's own angle at that moment, in radians, so that an angle
    /// physics did not change is not converted to degrees and back.
    float bodyAngle;
    /// The colliders whose fixtures are on [#body].
    ArrayList fixturesOf;
    boolean physicsDirty;
    /// A root that `DontDestroyOnLoad` keeps when a scene is loaded.
    boolean keptOnLoad;

    public GameObject() {
        this("GameObject");
    }

    public GameObject(String name) {
        this(name, new Transform());
    }

    /// What generated code calls for an object whose transform is a
    /// `RectTransform`.
    public GameObject(String name, Transform transform) {
        this.name = name;
        this.transform = transform;
        transform.gameObject = this;
        components.add(transform);
        UnityRuntime.created(this);
    }

    /// Adds a component the caller constructed. Scene code and
    /// `AddComponent` both end here; nothing is created by reflection.
    public void $attach(Component c) {
        c.gameObject = this;
        components.add(c);
        if (registered) {
            c.registered();
            UnityRuntime.attached(this, c);
        }
    }

    public Transform get_transform() {
        return transform;
    }

    public GameObject get_gameObject() {
        return this;
    }

    public String get_tag() {
        return tag;
    }

    public void set_tag(String value) {
        tag = value;
    }

    public int get_layer() {
        return layer;
    }

    public void set_layer(int value) {
        if (layer != value) {
            layer = value;
            PhysicsWorld.changed(this);
        }
    }

    public boolean CompareTag(String other) {
        return tag.equals(other);
    }

    public boolean get_activeSelf() {
        return active;
    }

    public boolean get_activeInHierarchy() {
        return registered && activeInHierarchy();
    }

    boolean activeInHierarchy() {
        if (!active || destroyed) {
            return false;
        }
        Transform p = transform.parent;
        while (p != null) {
            if (!p.gameObject.active) {
                return false;
            }
            p = p.parent;
        }
        return true;
    }

    public void SetActive(boolean value) {
        if (active == value) {
            return;
        }
        if (!registered) {
            active = value;
            return;
        }
        // The behaviours below this object that are live before and after
        // differ by exactly those this call switches.
        ArrayList before = new ArrayList();
        collectLive(this, before, null);
        active = value;
        UnityRuntime.activeChanged(this, before);
    }

    /// Every live behaviour on `go` and below it, in hierarchy order. With
    /// `was` given, only those not in it.
    static void collectLive(GameObject go, ArrayList out, ArrayList was) {
        int n = go.components.size();
        for (int i = 0; i < n; i++) {
            java.lang.Object c = go.components.get(i);
            if (c instanceof Behaviour && ((Behaviour) c).live() && (was == null || !was.contains(c))) {
                out.add(c);
            }
        }
        ArrayList children = go.transform.children;
        if (children != null) {
            for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach
                collectLive(((Transform) children.get(i)).gameObject, out, was);
            }
        }
    }

    /// `GetComponent<T>()`. The translator passes the class a type argument
    /// names, since erased code has no other way to say it.
    public java.lang.Object GetComponent(Class type) {
        int n = components.size();
        for (int i = 0; i < n; i++) {
            java.lang.Object c = components.get(i);
            if (type.isInstance(c)) {
                return c;
            }
        }
        return null;
    }

    public java.lang.Object[] GetComponents(Class type) { // NOPMD UnnecessaryFullyQualifiedName
        ArrayList found = new ArrayList();
        int n = components.size();
        for (int i = 0; i < n; i++) {
            java.lang.Object c = components.get(i);
            if (type.isInstance(c)) {
                found.add(c);
            }
        }
        return found.toArray();
    }

    /// Depth first, this object first. Unity's one-argument form leaves
    /// out inactive *children*; the object asked is searched whether it is
    /// active or not, as the documentation of the `includeInactive`
    /// argument has it. Below an object that is itself inactive, or whose
    /// parent is, every child is inactive in the hierarchy and none is
    /// searched.
    public java.lang.Object GetComponentInChildren(Class type) {
        java.lang.Object c = GetComponent(type);
        if (c == null && activeInHierarchy()) {
            c = firstBelow(type);
        }
        return c;
    }

    /// The first component of a type on the active objects below this one.
    private java.lang.Object firstBelow(Class type) {
        ArrayList children = transform.children;
        if (children == null) {
            return null;
        }
        for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach
            GameObject child = ((Transform) children.get(i)).gameObject;
            if (!child.active) {
                continue;
            }
            java.lang.Object c = child.GetComponent(type);
            if (c == null) {
                c = child.firstBelow(type);
            }
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    public java.lang.Object[] GetComponentsInChildren(Class type) { // NOPMD UnnecessaryFullyQualifiedName
        ArrayList found = new ArrayList();
        // This object's own whatever its state, then the active ones below
        // it: see [#GetComponentInChildren(Class)].
        collect(type, found, activeInHierarchy());
        return found.toArray();
    }

    private void collect(Class type, ArrayList found, boolean below) {
        int n = components.size();
        for (int i = 0; i < n; i++) {
            java.lang.Object c = components.get(i);
            if (type.isInstance(c)) {
                found.add(c);
            }
        }
        ArrayList children = transform.children;
        if (below && children != null) {
            for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach
                GameObject child = ((Transform) children.get(i)).gameObject;
                if (child.active) {
                    child.collect(type, found, true);
                }
            }
        }
    }

    public java.lang.Object GetComponentInParent(Class type) {
        Transform t = transform;
        while (t != null) {
            java.lang.Object c = t.gameObject.GetComponent(type);
            if (c != null) {
                return c;
            }
            t = t.parent;
        }
        return null;
    }

    /// `AddComponent<T>()`. The engine's own components are created here;
    /// a script's by the factory the scene compiler generated, which is the
    /// only code that can say `new` for a class this library has never seen.
    public java.lang.Object AddComponent(Class type) {
        Component c = UnityRuntime.newComponent(type);
        if (c == null) {
            throw new IllegalArgumentException("AddComponent: no way to create a " + type.getName());
        }
        $attach(c);
        return c;
    }

    public static GameObject Find(String name) {
        return UnityRuntime.find(name, null, null);
    }

    public static GameObject FindWithTag(String tag) {
        return UnityRuntime.find(null, tag, null);
    }

    public static GameObject FindGameObjectWithTag(String tag) {
        return UnityRuntime.find(null, tag, null);
    }

    public static GameObject[] FindGameObjectsWithTag(String tag) {
        ArrayList found = new ArrayList();
        UnityRuntime.find(null, tag, found);
        GameObject[] out = new GameObject[found.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (GameObject) found.get(i);
        }
        return out;
    }
}
