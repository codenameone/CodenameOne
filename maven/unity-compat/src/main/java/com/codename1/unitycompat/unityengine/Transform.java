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

import UnityEngine.Quaternion;
import UnityEngine.Vector3;
import com.codename1.unitycompat.system.collections.IEnumerable;
import com.codename1.unitycompat.system.collections.IEnumerator;
import java.util.ArrayList;

/// `UnityEngine.Transform`: where an object is, which way it faces and how
/// big it is, relative to its parent.
///
/// #### What is kept
///
/// A local position and scale in world units, y up, and one angle: the
/// rotation about z, in degrees, counter-clockwise as Unity counts it. A 2D
/// scene turns about nothing else, so the x and y of `eulerAngles` read as
/// zero, what is written to them is dropped, and a quaternion handed to
/// `rotation` is reduced to the angle about z it contains.
///
/// #### The hierarchy
///
/// A child's world pose is its parent's applied to its own local one:
/// scaled by the parent's world scale, turned by the parent's world angle,
/// moved to the parent's world position. The world scale of a child is the
/// product of the local scales above it -- exact for a uniform scale and for
/// an unrotated child, and an approximation for a rotated child of a
/// non-uniformly scaled parent, which in Unity is sheared.
///
/// World values are cached and recomputed on demand. A change marks the
/// transform and everything below it stale; reading a world value brings
/// the chain above it up to date. So a thousand children of an object that
/// never moves cost nothing per frame.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Transform extends Component implements IEnumerable {
    static final double DEG2RAD = 3.14159265358979323846 / 180.0;
    static final double RAD2DEG = 180.0 / 3.14159265358979323846;

    float x;
    float y;
    float z;
    float scaleX = 1f;
    float scaleY = 1f;
    float scaleZ = 1f;
    /// Degrees about z relative to the parent; not wrapped.
    float rotationZ;
    Transform parent;
    ArrayList children;

    private boolean stale = true;
    float wx;
    float wy;
    float wz;
    float wsx = 1f;
    float wsy = 1f;
    float wsz = 1f;
    float wrot;
    float wcos = 1f;
    float wsin;

    // ---------------------------------------------------------- world cache

    /// Brings the world values up to date.
    final void update() {
        if (!stale) {
            return;
        }
        stale = false;
        Transform p = parent;
        if (p == null) {
            wx = x;
            wy = y;
            wz = z;
            wsx = scaleX;
            wsy = scaleY;
            wsz = scaleZ;
            wrot = rotationZ;
        } else {
            p.update();
            // Every product is stored before it is added to: a compiler that
            // fuses a multiply into the add after it rounds once where the
            // JVM rounds twice, and two targets would then disagree in the
            // last bit.
            float px = x * p.wsx;
            float py = y * p.wsy;
            float a = px * p.wcos;
            float b = py * p.wsin;
            float c = px * p.wsin;
            float d = py * p.wcos;
            float dz = z * p.wsz;
            wx = p.wx + (a - b);
            wy = p.wy + (c + d);
            wz = p.wz + dz;
            wsx = p.wsx * scaleX;
            wsy = p.wsy * scaleY;
            wsz = p.wsz * scaleZ;
            wrot = p.wrot + rotationZ;
        }
        if (wrot == 0f) {
            wcos = 1f;
            wsin = 0f;
        } else {
            double r = wrot * DEG2RAD;
            wcos = (float) Math.cos(r);
            wsin = (float) Math.sin(r);
        }
    }

    /// Marks this and everything below it stale. A stale transform's
    /// children are stale already, so the walk stops there.
    final void invalidate() {
        if (stale) {
            return;
        }
        stale = true;
        if (children != null) {
            for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach
                ((Transform) children.get(i)).invalidate();
            }
        }
    }

    final void setWorldPosition(float nx, float ny, float nz) {
        Transform p = parent;
        if (p == null) {
            x = nx;
            y = ny;
            z = nz;
        } else {
            p.update();
            float dx = nx - p.wx;
            float dy = ny - p.wy;
            float a = dx * p.wcos;
            float b = dy * p.wsin;
            float c = dy * p.wcos;
            float d = dx * p.wsin;
            float lx = a + b;
            float ly = c - d;
            // A parent scaled to nothing has no inverse: its children are
            // all at its own position, wherever they are put.
            x = p.wsx != 0f ? lx / p.wsx : 0f;
            y = p.wsy != 0f ? ly / p.wsy : 0f;
            z = p.wsz != 0f ? (nz - p.wz) / p.wsz : 0f;
        }
        invalidate();
    }

    final void setWorldRotation(float degrees) {
        if (parent == null) {
            rotationZ = degrees;
        } else {
            parent.update();
            rotationZ = degrees - parent.wrot;
        }
        invalidate();
    }

    /// The angle about z a quaternion holds, in degrees.
    static float zAngle(Quaternion q) {
        if (q.z == 0f) {
            return 0f;
        }
        return (float) (2.0 * com.codename1.unitycompat.system.Math.Atan2(q.z, q.w) * RAD2DEG);
    }

    static void zRotation(float degrees, Quaternion ret) {
        ret.x = 0f;
        ret.y = 0f;
        if (degrees == 0f) {
            ret.z = 0f;
            ret.w = 1f;
        } else {
            double half = degrees * DEG2RAD * 0.5;
            ret.z = (float) Math.sin(half);
            ret.w = (float) Math.cos(half);
        }
    }

    private static float wrap(float degrees) {
        float a = degrees % 360f;
        return a < 0f ? a + 360f : a;
    }

    // ------------------------------------------------------- generated code

    /// What a scene file sets: local position and scale.
    public void $place(float px, float py, float pz, float sx, float sy, float sz) {
        x = px;
        y = py;
        z = pz;
        scaleX = sx;
        scaleY = sy;
        scaleZ = sz;
        invalidate();
    }

    /// What a scene file sets: the angle about z its quaternion stands for.
    public void $rotate(float degrees) {
        rotationZ = degrees;
        invalidate();
    }

    /// What a scene file sets: the parent, with the local values kept.
    public void $parent(Transform p) {
        SetParent(p, false);
    }

    /// Where this is in the world, for runtime code outside this package
    /// that must not make a vector to ask.
    public float $worldX() {
        update();
        return wx;
    }

    public float $worldY() {
        update();
        return wy;
    }

    public float $worldZ() {
        update();
        return wz;
    }

    /// The scale every transform above this one and this one give it.
    public float $worldScaleX() {
        update();
        return wsx;
    }

    public float $worldScaleY() {
        update();
        return wsy;
    }

    /// Degrees about z, anticlockwise, in the world.
    public float $worldTurn() {
        update();
        return wrot;
    }

    /// Puts this at a place in the world.
    public void $moveTo(float nx, float ny, float nz) {
        setWorldPosition(nx, ny, nz);
    }

    @Override
    public Component $new() {
        return new Transform();
    }

    @Override
    public void $copyFrom(Component source) {
        Transform t = (Transform) source;
        x = t.x;
        y = t.y;
        z = t.z;
        scaleX = t.scaleX;
        scaleY = t.scaleY;
        scaleZ = t.scaleZ;
        rotationZ = t.rotationZ;
        invalidate();
    }

    // ------------------------------------------------------------- position

    public Vector3 get_position(Vector3 ret) {
        update();
        ret.x = wx;
        ret.y = wy;
        ret.z = wz;
        return ret;
    }

    public void set_position(Vector3 value) {
        setWorldPosition(value.x, value.y, value.z);
    }

    public Vector3 get_localPosition(Vector3 ret) {
        ret.x = x;
        ret.y = y;
        ret.z = z;
        return ret;
    }

    public void set_localPosition(Vector3 value) {
        x = value.x;
        y = value.y;
        z = value.z;
        invalidate();
    }

    public void Translate(Vector3 translation) {
        Translate(translation.x, translation.y, translation.z);
    }

    /// Along this transform's own axes, as Unity's default `Space.Self`.
    public void Translate(float dx, float dy, float dz) {
        update();
        float a = dx * wcos;
        float b = dy * wsin;
        float c = dx * wsin;
        float d = dy * wcos;
        setWorldPosition(wx + (a - b), wy + (c + d), wz + dz);
    }

    /// `relativeTo` is `Space`: 0 the world's axes, 1 this transform's.
    public void Translate(Vector3 translation, int relativeTo) {
        if (relativeTo == 0) {
            update();
            setWorldPosition(wx + translation.x, wy + translation.y, wz + translation.z);
        } else {
            Translate(translation.x, translation.y, translation.z);
        }
    }

    // ------------------------------------------------------------- rotation

    public void SetPositionAndRotation(Vector3 position, Quaternion rotation) {
        set_position(position);
        set_rotation(rotation);
    }

    /// The place of this transform among its parent's children. Among the
    /// roots of a scene there is no such list here, and the answer is 0.
    public int GetSiblingIndex() {
        return parent == null || parent.children == null ? 0 : parent.children.indexOf(this);
    }

    public void SetSiblingIndex(int index) {
        if (parent == null || parent.children == null) {
            return;
        }
        ArrayList list = parent.children;
        if (!list.remove(this)) {
            return;
        }
        int at = index < 0 ? 0 : index > list.size() ? list.size() : index;
        list.add(at, this);
    }

    public void SetAsFirstSibling() {
        SetSiblingIndex(0);
    }

    public void SetAsLastSibling() {
        SetSiblingIndex(Integer.MAX_VALUE);
    }

    public Quaternion get_rotation(Quaternion ret) {
        update();
        zRotation(wrot, ret);
        return ret;
    }

    public void set_rotation(Quaternion value) {
        setWorldRotation(zAngle(value));
    }

    public Quaternion get_localRotation(Quaternion ret) {
        zRotation(rotationZ, ret);
        return ret;
    }

    public void set_localRotation(Quaternion value) {
        rotationZ = zAngle(value);
        invalidate();
    }

    /// The angle about z, in Unity's range of 0 up to 360.
    public Vector3 get_eulerAngles(Vector3 ret) {
        update();
        ret.x = 0f;
        ret.y = 0f;
        ret.z = wrap(wrot);
        return ret;
    }

    public void set_eulerAngles(Vector3 value) {
        setWorldRotation(value.z);
    }

    public Vector3 get_localEulerAngles(Vector3 ret) {
        ret.x = 0f;
        ret.y = 0f;
        ret.z = wrap(rotationZ);
        return ret;
    }

    public void set_localEulerAngles(Vector3 value) {
        rotationZ = value.z;
        invalidate();
    }

    public void Rotate(Vector3 eulers) {
        Rotate(eulers.x, eulers.y, eulers.z);
    }

    public void Rotate(float xAngle, float yAngle, float zAngle) {
        rotationZ += zAngle;
        invalidate();
    }

    public Vector3 get_up(Vector3 ret) {
        update();
        ret.x = wsin == 0f ? 0f : -wsin;
        ret.y = wcos;
        ret.z = 0f;
        return ret;
    }

    public void set_up(Vector3 value) {
        setWorldRotation((float) (com.codename1.unitycompat.system.Math.Atan2(-value.x, value.y) * RAD2DEG));
    }

    public Vector3 get_right(Vector3 ret) {
        update();
        ret.x = wcos;
        ret.y = wsin;
        ret.z = 0f;
        return ret;
    }

    public void set_right(Vector3 value) {
        setWorldRotation((float) (com.codename1.unitycompat.system.Math.Atan2(value.y, value.x) * RAD2DEG));
    }

    public Vector3 get_forward(Vector3 ret) {
        ret.x = 0f;
        ret.y = 0f;
        ret.z = 1f;
        return ret;
    }

    // ---------------------------------------------------------------- scale

    public Vector3 get_localScale(Vector3 ret) {
        ret.x = scaleX;
        ret.y = scaleY;
        ret.z = scaleZ;
        return ret;
    }

    public void set_localScale(Vector3 value) {
        scaleX = value.x;
        scaleY = value.y;
        scaleZ = value.z;
        invalidate();
    }

    public Vector3 get_lossyScale(Vector3 ret) {
        update();
        ret.x = wsx;
        ret.y = wsy;
        ret.z = wsz;
        return ret;
    }

    // ------------------------------------------------------------ hierarchy

    public Transform get_parent() {
        return parent;
    }

    public void set_parent(Transform value) {
        SetParent(value, true);
    }

    public void SetParent(Transform p) {
        SetParent(p, true);
    }

    /// With `worldPositionStays` the object does not move: its local
    /// position, angle and scale are rewritten to mean, under the new
    /// parent, what they meant under the old one.
    public void SetParent(Transform p, boolean worldPositionStays) {
        if (p == parent || p == this || (p != null && p.IsChildOf(this))) { // NOPMD CompareObjectsWithEquals
            return;
        }
        update();
        float kx = wx;
        float ky = wy;
        float kz = wz;
        float kr = wrot;
        float ksx = wsx;
        float ksy = wsy;
        float ksz = wsz;
        boolean inScene = gameObject != null && gameObject.registered;
        ArrayList before = null;
        if (inScene) {
            before = new ArrayList();
            GameObject.collectLive(gameObject, before, null);
        }
        if (parent != null) {
            parent.children.remove(this);
        }
        parent = p;
        if (p != null) {
            if (p.children == null) {
                p.children = new ArrayList();
            }
            p.children.add(this);
        }
        if (worldPositionStays) {
            if (p == null) {
                rotationZ = kr;
                scaleX = ksx;
                scaleY = ksy;
                scaleZ = ksz;
            } else {
                p.update();
                rotationZ = kr - p.wrot;
                scaleX = p.wsx != 0f ? ksx / p.wsx : ksx;
                scaleY = p.wsy != 0f ? ksy / p.wsy : ksy;
                scaleZ = p.wsz != 0f ? ksz / p.wsz : ksz;
            }
            setWorldPosition(kx, ky, kz);
        }
        invalidate();
        if (inScene) {
            UnityRuntime.activeChanged(gameObject, before);
            PhysicsWorld.reparented(gameObject);
        }
    }

    public Transform get_root() {
        Transform t = this;
        while (t.parent != null) {
            t = t.parent;
        }
        return t;
    }

    public int get_childCount() {
        return children == null ? 0 : children.size();
    }

    public Transform GetChild(int index) {
        if (children == null) {
            throw new IndexOutOfBoundsException("Transform child out of bounds");
        }
        return (Transform) children.get(index);
    }

    /// A child by name, or a descendant by a path of names separated by `/`.
    public Transform Find(String n) {
        int slash = n.indexOf('/');
        String first = slash < 0 ? n : n.substring(0, slash);
        if (children != null) {
            for (int i = 0; i < children.size(); i++) { // NOPMD ForLoopCanBeForeach
                Transform c = (Transform) children.get(i);
                if (c.gameObject.name.equals(first)) {
                    return slash < 0 ? c : c.Find(n.substring(slash + 1));
                }
            }
        }
        return null;
    }

    public void DetachChildren() {
        while (children != null && !children.isEmpty()) {
            ((Transform) children.get(children.size() - 1)).SetParent(null, true);
        }
    }

    /// True for a descendant of `p`, and for `p` itself.
    public boolean IsChildOf(Transform p) {
        Transform t = this;
        while (t != null) {
            if (t == p) { // NOPMD CompareObjectsWithEquals
                return true;
            }
            t = t.parent;
        }
        return false;
    }

    /// `foreach (Transform child in transform)`: the children as they are
    /// now, so the loop may reparent or destroy them.
    @Override
    public IEnumerator GetEnumerator() {
        final java.lang.Object[] snapshot = children == null ? new java.lang.Object[0] : children.toArray(); // NOPMD UnnecessaryFullyQualifiedName
        return new Children(snapshot);
    }

    /// The enumerator of [#GetEnumerator()], over a snapshot.
    private static final class Children implements IEnumerator {
        private final java.lang.Object[] snapshot; // NOPMD UnnecessaryFullyQualifiedName
        private int at = -1;

        Children(java.lang.Object[] snapshot) { // NOPMD UnnecessaryFullyQualifiedName
            this.snapshot = snapshot;
        }

        @Override
        public boolean MoveNext() {
            return ++at < snapshot.length;
        }

        @Override
        public java.lang.Object get_Current() { // NOPMD UnnecessaryFullyQualifiedName
            return snapshot[at];
        }

        @Override
        public void Reset() {
            at = -1;
        }
    }

    // ---------------------------------------------------------- conversions

    public Vector3 TransformPoint(Vector3 position, Vector3 ret) {
        update();
        float px = position.x * wsx;
        float py = position.y * wsy;
        float a = px * wcos;
        float b = py * wsin;
        float c = px * wsin;
        float d = py * wcos;
        float dz = position.z * wsz;
        ret.x = wx + (a - b);
        ret.y = wy + (c + d);
        ret.z = wz + dz;
        return ret;
    }

    public Vector3 InverseTransformPoint(Vector3 position, Vector3 ret) {
        update();
        float dx = position.x - wx;
        float dy = position.y - wy;
        float a = dx * wcos;
        float b = dy * wsin;
        float c = dy * wcos;
        float d = dx * wsin;
        float lx = a + b;
        float ly = c - d;
        float lz = position.z - wz;
        ret.x = wsx != 0f ? lx / wsx : 0f;
        ret.y = wsy != 0f ? ly / wsy : 0f;
        ret.z = wsz != 0f ? lz / wsz : 0f;
        return ret;
    }

    public Vector3 TransformDirection(Vector3 direction, Vector3 ret) {
        update();
        float a = direction.x * wcos;
        float b = direction.y * wsin;
        float c = direction.x * wsin;
        float d = direction.y * wcos;
        float dz = direction.z;
        ret.x = a - b;
        ret.y = c + d;
        ret.z = dz;
        return ret;
    }

    public Vector3 InverseTransformDirection(Vector3 direction, Vector3 ret) {
        update();
        float a = direction.x * wcos;
        float b = direction.y * wsin;
        float c = direction.y * wcos;
        float d = direction.x * wsin;
        float dz = direction.z;
        ret.x = a + b;
        ret.y = c - d;
        ret.z = dz;
        return ret;
    }
}
