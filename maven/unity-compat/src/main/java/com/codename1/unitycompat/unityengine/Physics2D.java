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

import UnityEngine.Vector2;
import com.codename1.unitycompat.system.collections.generic.List_1;

/// `UnityEngine.Physics2D`: the settings of the physics world, and the
/// questions a script asks of it. How those are answered, and how exactly,
/// is described at [PhysicsQuery].
///
/// A query that takes a layer mask reports triggers when
/// `queriesHitTriggers` says so; one that takes a `ContactFilter2D` asks the
/// filter alone. Results of a cast come nearest first, and of an overlap
/// least z first.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Physics2D {
    private Physics2D() {
    }

    /// What generated code sets from the project's physics settings:
    /// gravity and the solver's iteration counts.
    public static void $settings(float gravityX, float gravityY, int velocityIterations, int positionIterations) {
        PhysicsWorld.setGravity(gravityX, gravityY);
        PhysicsWorld.velocityIterations = velocityIterations > 0 ? velocityIterations : 8;
        PhysicsWorld.positionIterations = positionIterations > 0 ? positionIterations : 3;
    }

    /// What generated code sets from the project's layer collision matrix:
    /// for each of the 32 layers, a bit for every layer it collides with.
    public static void $layerMatrix(int[] masks) {
        for (int i = 0; i < 32 && i < masks.length; i++) {
            PhysicsWorld.layerMask[i] = masks[i];
        }
        PhysicsWorld.refilter();
    }

    public static Vector2 get_gravity(Vector2 ret) {
        ret.x = PhysicsWorld.gravityX;
        ret.y = PhysicsWorld.gravityY;
        return ret;
    }

    public static void set_gravity(Vector2 value) {
        PhysicsWorld.setGravity(value.x, value.y);
    }

    public static void IgnoreLayerCollision(int layer1, int layer2) {
        IgnoreLayerCollision(layer1, layer2, true);
    }

    public static void IgnoreLayerCollision(int layer1, int layer2, boolean ignore) {
        if (layer1 < 0 || layer1 > 31 || layer2 < 0 || layer2 > 31) {
            throw new IllegalArgumentException("layer numbers must be between 0 and 31");
        }
        if (ignore) {
            PhysicsWorld.layerMask[layer1] &= ~(1 << layer2);
            PhysicsWorld.layerMask[layer2] &= ~(1 << layer1);
        } else {
            PhysicsWorld.layerMask[layer1] |= 1 << layer2;
            PhysicsWorld.layerMask[layer2] |= 1 << layer1;
        }
        PhysicsWorld.refilter();
    }

    public static boolean GetIgnoreLayerCollision(int layer1, int layer2) {
        if (layer1 < 0 || layer1 > 31 || layer2 < 0 || layer2 > 31) {
            throw new IllegalArgumentException("layer numbers must be between 0 and 31");
        }
        return (PhysicsWorld.layerMask[layer1] & (1 << layer2)) == 0;
    }

    // -------------------------------------------------------------- queries

    public static final int IgnoreRaycastLayer = 4; // NOPMD FieldNamingConventions
    public static final int DefaultRaycastLayers = -5; // NOPMD FieldNamingConventions
    public static final int AllLayers = -1; // NOPMD FieldNamingConventions

    static boolean hitTriggers = true;
    static boolean startInColliders = true;
    static boolean autoSync;

    // The direction and length of the cast being answered.
    private static double ux;
    private static double uy;
    private static double length;

    static void reset() {
        hitTriggers = true;
        startInColliders = true;
        autoSync = false;
    }

    /// What generated code sets from the project's physics settings: whether
    /// queries report triggers, whether a ray reports a collider it starts
    /// inside, and whether a query first moves bodies to their transforms.
    public static void $querySettings(boolean queriesHitTriggers, boolean queriesStartInColliders,
            boolean autoSyncTransforms) {
        hitTriggers = queriesHitTriggers;
        startInColliders = queriesStartInColliders;
        autoSync = autoSyncTransforms;
    }

    public static boolean get_queriesHitTriggers() {
        return hitTriggers;
    }

    public static void set_queriesHitTriggers(boolean value) {
        hitTriggers = value;
    }

    public static boolean get_queriesStartInColliders() {
        return startInColliders;
    }

    public static void set_queriesStartInColliders(boolean value) {
        startInColliders = value;
    }

    public static boolean get_autoSyncTransforms() {
        return autoSync;
    }

    public static void set_autoSyncTransforms(boolean value) {
        autoSync = value;
    }

    /// Puts every body where a script moved its transform to, as the next
    /// fixed step would.
    public static void SyncTransforms() {
        PhysicsWorld.syncTransforms();
    }

    public static boolean IsTouching(Collider2D collider1, Collider2D collider2) {
        return PhysicsWorld.touching(collider1, collider2);
    }

    public static boolean IsTouchingLayers(Collider2D collider, int layerMask) {
        return PhysicsWorld.touchingLayers(collider, layerMask);
    }

    private static void along(Vector2 direction, float distance) {
        double x = direction.x;
        double y = direction.y;
        double l = Math.sqrt(x * x + y * y);
        ux = l > 0d ? x / l : 0d;
        uy = l > 0d ? y / l : 0d;
        length = distance > 0f ? distance : 0d;
    }

    private static void between(Vector2 start, Vector2 end) {
        double x = (double) end.x - start.x;
        double y = (double) end.y - start.y;
        double l = Math.sqrt(x * x + y * y);
        ux = l > 0d ? x / l : 0d;
        uy = l > 0d ? y / l : 0d;
        length = l;
    }

    private static RaycastHit2D first(RaycastHit2D ret) {
        if (PhysicsQuery.nearestFirst() > 0) {
            PhysicsQuery.hit(0, ux, uy, length, ret);
        } else {
            ret.$clear();
        }
        PhysicsQuery.clear();
        return ret;
    }

    private static RaycastHit2D[] all() {
        int n = PhysicsQuery.nearestFirst();
        RaycastHit2D[] hits = RaycastHit2D.$newArray(n);
        for (int i = 0; i < n; i++) {
            PhysicsQuery.hit(i, ux, uy, length, hits[i]);
        }
        PhysicsQuery.clear();
        return hits;
    }

    private static int into(RaycastHit2D[] results) {
        int n = PhysicsQuery.nearestFirst();
        if (n > results.length) {
            n = results.length;
        }
        for (int i = 0; i < n; i++) {
            PhysicsQuery.hit(i, ux, uy, length, results[i]);
        }
        PhysicsQuery.clear();
        return n;
    }

    private static int into(List_1 results) {
        int n = PhysicsQuery.nearestFirst();
        results.Clear();
        for (int i = 0; i < n; i++) {
            RaycastHit2D hit = new RaycastHit2D();
            PhysicsQuery.hit(i, ux, uy, length, hit);
            results.Add(hit);
        }
        PhysicsQuery.clear();
        return n;
    }

    private static Collider2D front() {
        Collider2D c = PhysicsQuery.frontFirst() > 0 ? PhysicsQuery.collider(0) : null;
        PhysicsQuery.clear();
        return c;
    }

    private static Collider2D[] colliders() {
        int n = PhysicsQuery.frontFirst();
        Collider2D[] out = new Collider2D[n];
        for (int i = 0; i < n; i++) {
            out[i] = PhysicsQuery.collider(i);
        }
        PhysicsQuery.clear();
        return out;
    }

    static int into(Collider2D[] results) {
        int n = PhysicsQuery.frontFirst();
        if (n > results.length) {
            n = results.length;
        }
        for (int i = 0; i < n; i++) {
            results[i] = PhysicsQuery.collider(i);
        }
        PhysicsQuery.clear();
        return n;
    }

    private static int collidersInto(List_1 results) {
        int n = PhysicsQuery.frontFirst();
        results.Clear();
        for (int i = 0; i < n; i++) {
            results.Add(PhysicsQuery.collider(i));
        }
        PhysicsQuery.clear();
        return n;
    }

    /// Starts a cast of colliders' own shapes; [#castWith] moves each and
    /// [#castEnd] collects what they met, nearest first.
    static void castBegin(Vector2 direction, float distance) {
        PhysicsQuery.begin();
        PhysicsQuery.where(AllLayers, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        along(direction, distance);
    }

    static void castWith(Collider2D collider, boolean ignoreSiblings) {
        PhysicsQuery.sweep(collider, ignoreSiblings, ux, uy, length);
    }

    static int castEnd(RaycastHit2D[] results) {
        return into(results);
    }

    /// A ray: the nearest collider it meets, or a hit with none.
    public static RaycastHit2D Raycast(Vector2 origin, Vector2 direction, float distance, int layerMask, float minDepth, float maxDepth, RaycastHit2D ret) {
        PhysicsQuery.begin();
        PhysicsQuery.point(origin.x, origin.y);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, true);
        return first(ret);
    }

    /// The same through a filter: fills `results` nearest first and answers
    /// how many it wrote.
    public static int Raycast(Vector2 origin, Vector2 direction, ContactFilter2D contactFilter, RaycastHit2D[] results, float distance) {
        PhysicsQuery.begin();
        PhysicsQuery.point(origin.x, origin.y);
        PhysicsQuery.where(contactFilter);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, true);
        return into(results);
    }

    public static int Raycast(Vector2 origin, Vector2 direction, ContactFilter2D contactFilter, List_1 results, float distance) {
        PhysicsQuery.begin();
        PhysicsQuery.point(origin.x, origin.y);
        PhysicsQuery.where(contactFilter);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, true);
        return into(results);
    }

    /// Every collider met, nearest first.
    public static RaycastHit2D[] RaycastAll(Vector2 origin, Vector2 direction, float distance, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.point(origin.x, origin.y);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, true);
        return all();
    }

    public static int RaycastNonAlloc(Vector2 origin, Vector2 direction, RaycastHit2D[] results, float distance, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.point(origin.x, origin.y);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, true);
        return into(results);
    }

    /// A circle moved along a direction: the nearest collider it meets, or a hit with none.
    public static RaycastHit2D CircleCast(Vector2 origin, float radius, Vector2 direction, float distance, int layerMask, float minDepth, float maxDepth, RaycastHit2D ret) {
        PhysicsQuery.begin();
        PhysicsQuery.circle(origin.x, origin.y, radius);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return first(ret);
    }

    /// The same through a filter: fills `results` nearest first and answers
    /// how many it wrote.
    public static int CircleCast(Vector2 origin, float radius, Vector2 direction, ContactFilter2D contactFilter, RaycastHit2D[] results, float distance) {
        PhysicsQuery.begin();
        PhysicsQuery.circle(origin.x, origin.y, radius);
        PhysicsQuery.where(contactFilter);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return into(results);
    }

    public static int CircleCast(Vector2 origin, float radius, Vector2 direction, ContactFilter2D contactFilter, List_1 results, float distance) {
        PhysicsQuery.begin();
        PhysicsQuery.circle(origin.x, origin.y, radius);
        PhysicsQuery.where(contactFilter);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return into(results);
    }

    /// Every collider met, nearest first.
    public static RaycastHit2D[] CircleCastAll(Vector2 origin, float radius, Vector2 direction, float distance, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.circle(origin.x, origin.y, radius);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return all();
    }

    public static int CircleCastNonAlloc(Vector2 origin, float radius, Vector2 direction, RaycastHit2D[] results, float distance, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.circle(origin.x, origin.y, radius);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return into(results);
    }

    /// A box, turned by `angle` degrees, moved along a direction: the nearest collider it meets, or a hit with none.
    public static RaycastHit2D BoxCast(Vector2 origin, Vector2 size, float angle, Vector2 direction, float distance, int layerMask, float minDepth, float maxDepth, RaycastHit2D ret) {
        PhysicsQuery.begin();
        PhysicsQuery.box(origin.x, origin.y, size.x, size.y, angle);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return first(ret);
    }

    /// The same through a filter: fills `results` nearest first and answers
    /// how many it wrote.
    public static int BoxCast(Vector2 origin, Vector2 size, float angle, Vector2 direction, ContactFilter2D contactFilter, RaycastHit2D[] results, float distance) {
        PhysicsQuery.begin();
        PhysicsQuery.box(origin.x, origin.y, size.x, size.y, angle);
        PhysicsQuery.where(contactFilter);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return into(results);
    }

    public static int BoxCast(Vector2 origin, Vector2 size, float angle, Vector2 direction, ContactFilter2D contactFilter, List_1 results, float distance) {
        PhysicsQuery.begin();
        PhysicsQuery.box(origin.x, origin.y, size.x, size.y, angle);
        PhysicsQuery.where(contactFilter);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return into(results);
    }

    /// Every collider met, nearest first.
    public static RaycastHit2D[] BoxCastAll(Vector2 origin, Vector2 size, float angle, Vector2 direction, float distance, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.box(origin.x, origin.y, size.x, size.y, angle);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return all();
    }

    public static int BoxCastNonAlloc(Vector2 origin, Vector2 size, float angle, Vector2 direction, RaycastHit2D[] results, float distance, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.box(origin.x, origin.y, size.x, size.y, angle);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return into(results);
    }

    /// A capsule, turned by `angle` degrees, moved along a direction: the nearest collider it meets, or a hit with none.
    public static RaycastHit2D CapsuleCast(Vector2 origin, Vector2 size, int capsuleDirection, float angle, Vector2 direction, float distance, int layerMask, float minDepth, float maxDepth, RaycastHit2D ret) {
        PhysicsQuery.begin();
        PhysicsQuery.capsule(origin.x, origin.y, size.x, size.y, capsuleDirection, angle);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return first(ret);
    }

    /// The same through a filter: fills `results` nearest first and answers
    /// how many it wrote.
    public static int CapsuleCast(Vector2 origin, Vector2 size, int capsuleDirection, float angle, Vector2 direction, ContactFilter2D contactFilter, RaycastHit2D[] results, float distance) {
        PhysicsQuery.begin();
        PhysicsQuery.capsule(origin.x, origin.y, size.x, size.y, capsuleDirection, angle);
        PhysicsQuery.where(contactFilter);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return into(results);
    }

    public static int CapsuleCast(Vector2 origin, Vector2 size, int capsuleDirection, float angle, Vector2 direction, ContactFilter2D contactFilter, List_1 results, float distance) {
        PhysicsQuery.begin();
        PhysicsQuery.capsule(origin.x, origin.y, size.x, size.y, capsuleDirection, angle);
        PhysicsQuery.where(contactFilter);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return into(results);
    }

    /// Every collider met, nearest first.
    public static RaycastHit2D[] CapsuleCastAll(Vector2 origin, Vector2 size, int capsuleDirection, float angle, Vector2 direction, float distance, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.capsule(origin.x, origin.y, size.x, size.y, capsuleDirection, angle);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return all();
    }

    public static int CapsuleCastNonAlloc(Vector2 origin, Vector2 size, int capsuleDirection, float angle, Vector2 direction, RaycastHit2D[] results, float distance, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.capsule(origin.x, origin.y, size.x, size.y, capsuleDirection, angle);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        along(direction, distance);
        PhysicsQuery.sweep(ux, uy, length, false);
        return into(results);
    }

    // A line from one point to another is a ray that stops at the second.

    public static RaycastHit2D Linecast(Vector2 start, Vector2 end, int layerMask, float minDepth, float maxDepth, RaycastHit2D ret) {
        PhysicsQuery.begin();
        PhysicsQuery.point(start.x, start.y);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        between(start, end);
        PhysicsQuery.sweep(ux, uy, length, true);
        return first(ret);
    }

    public static int Linecast(Vector2 start, Vector2 end, ContactFilter2D contactFilter, RaycastHit2D[] results) {
        PhysicsQuery.begin();
        PhysicsQuery.point(start.x, start.y);
        PhysicsQuery.where(contactFilter);
        between(start, end);
        PhysicsQuery.sweep(ux, uy, length, true);
        return into(results);
    }

    public static int Linecast(Vector2 start, Vector2 end, ContactFilter2D contactFilter, List_1 results) {
        PhysicsQuery.begin();
        PhysicsQuery.point(start.x, start.y);
        PhysicsQuery.where(contactFilter);
        between(start, end);
        PhysicsQuery.sweep(ux, uy, length, true);
        return into(results);
    }

    public static RaycastHit2D[] LinecastAll(Vector2 start, Vector2 end, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.point(start.x, start.y);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        between(start, end);
        PhysicsQuery.sweep(ux, uy, length, true);
        return all();
    }

    public static int LinecastNonAlloc(Vector2 start, Vector2 end, RaycastHit2D[] results, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.point(start.x, start.y);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        between(start, end);
        PhysicsQuery.sweep(ux, uy, length, true);
        return into(results);
    }

    /// A point: the collider there nearest a camera looking along z, or null.
    public static Collider2D OverlapPoint(Vector2 point, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.point(point.x, point.y);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return front();
    }

    public static int OverlapPoint(Vector2 point, ContactFilter2D contactFilter, Collider2D[] results) {
        PhysicsQuery.begin();
        PhysicsQuery.point(point.x, point.y);
        PhysicsQuery.where(contactFilter);
        PhysicsQuery.overlap();
        return into(results);
    }

    public static int OverlapPoint(Vector2 point, ContactFilter2D contactFilter, List_1 results) {
        PhysicsQuery.begin();
        PhysicsQuery.point(point.x, point.y);
        PhysicsQuery.where(contactFilter);
        PhysicsQuery.overlap();
        return collidersInto(results);
    }

    public static Collider2D[] OverlapPointAll(Vector2 point, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.point(point.x, point.y);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return colliders();
    }

    public static int OverlapPointNonAlloc(Vector2 point, Collider2D[] results, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.point(point.x, point.y);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return into(results);
    }

    /// A circle: the collider there nearest a camera looking along z, or null.
    public static Collider2D OverlapCircle(Vector2 point, float radius, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.circle(point.x, point.y, radius);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return front();
    }

    public static int OverlapCircle(Vector2 point, float radius, ContactFilter2D contactFilter, Collider2D[] results) {
        PhysicsQuery.begin();
        PhysicsQuery.circle(point.x, point.y, radius);
        PhysicsQuery.where(contactFilter);
        PhysicsQuery.overlap();
        return into(results);
    }

    public static int OverlapCircle(Vector2 point, float radius, ContactFilter2D contactFilter, List_1 results) {
        PhysicsQuery.begin();
        PhysicsQuery.circle(point.x, point.y, radius);
        PhysicsQuery.where(contactFilter);
        PhysicsQuery.overlap();
        return collidersInto(results);
    }

    public static Collider2D[] OverlapCircleAll(Vector2 point, float radius, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.circle(point.x, point.y, radius);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return colliders();
    }

    public static int OverlapCircleNonAlloc(Vector2 point, float radius, Collider2D[] results, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.circle(point.x, point.y, radius);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return into(results);
    }

    /// A box turned by `angle` degrees: the collider there nearest a camera looking along z, or null.
    public static Collider2D OverlapBox(Vector2 point, Vector2 size, float angle, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.box(point.x, point.y, size.x, size.y, angle);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return front();
    }

    public static int OverlapBox(Vector2 point, Vector2 size, float angle, ContactFilter2D contactFilter, Collider2D[] results) {
        PhysicsQuery.begin();
        PhysicsQuery.box(point.x, point.y, size.x, size.y, angle);
        PhysicsQuery.where(contactFilter);
        PhysicsQuery.overlap();
        return into(results);
    }

    public static int OverlapBox(Vector2 point, Vector2 size, float angle, ContactFilter2D contactFilter, List_1 results) {
        PhysicsQuery.begin();
        PhysicsQuery.box(point.x, point.y, size.x, size.y, angle);
        PhysicsQuery.where(contactFilter);
        PhysicsQuery.overlap();
        return collidersInto(results);
    }

    public static Collider2D[] OverlapBoxAll(Vector2 point, Vector2 size, float angle, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.box(point.x, point.y, size.x, size.y, angle);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return colliders();
    }

    public static int OverlapBoxNonAlloc(Vector2 point, Vector2 size, float angle, Collider2D[] results, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.box(point.x, point.y, size.x, size.y, angle);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return into(results);
    }

    /// The box with two opposite corners: the collider there nearest a camera looking along z, or null.
    public static Collider2D OverlapArea(Vector2 pointA, Vector2 pointB, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.area(pointA.x, pointA.y, pointB.x, pointB.y);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return front();
    }

    public static int OverlapArea(Vector2 pointA, Vector2 pointB, ContactFilter2D contactFilter, Collider2D[] results) {
        PhysicsQuery.begin();
        PhysicsQuery.area(pointA.x, pointA.y, pointB.x, pointB.y);
        PhysicsQuery.where(contactFilter);
        PhysicsQuery.overlap();
        return into(results);
    }

    public static int OverlapArea(Vector2 pointA, Vector2 pointB, ContactFilter2D contactFilter, List_1 results) {
        PhysicsQuery.begin();
        PhysicsQuery.area(pointA.x, pointA.y, pointB.x, pointB.y);
        PhysicsQuery.where(contactFilter);
        PhysicsQuery.overlap();
        return collidersInto(results);
    }

    public static Collider2D[] OverlapAreaAll(Vector2 pointA, Vector2 pointB, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.area(pointA.x, pointA.y, pointB.x, pointB.y);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return colliders();
    }

    public static int OverlapAreaNonAlloc(Vector2 pointA, Vector2 pointB, Collider2D[] results, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.area(pointA.x, pointA.y, pointB.x, pointB.y);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return into(results);
    }

    /// A capsule turned by `angle` degrees: the collider there nearest a camera looking along z, or null.
    public static Collider2D OverlapCapsule(Vector2 point, Vector2 size, int direction, float angle, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.capsule(point.x, point.y, size.x, size.y, direction, angle);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return front();
    }

    public static int OverlapCapsule(Vector2 point, Vector2 size, int direction, float angle, ContactFilter2D contactFilter, Collider2D[] results) {
        PhysicsQuery.begin();
        PhysicsQuery.capsule(point.x, point.y, size.x, size.y, direction, angle);
        PhysicsQuery.where(contactFilter);
        PhysicsQuery.overlap();
        return into(results);
    }

    public static int OverlapCapsule(Vector2 point, Vector2 size, int direction, float angle, ContactFilter2D contactFilter, List_1 results) {
        PhysicsQuery.begin();
        PhysicsQuery.capsule(point.x, point.y, size.x, size.y, direction, angle);
        PhysicsQuery.where(contactFilter);
        PhysicsQuery.overlap();
        return collidersInto(results);
    }

    public static Collider2D[] OverlapCapsuleAll(Vector2 point, Vector2 size, int direction, float angle, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.capsule(point.x, point.y, size.x, size.y, direction, angle);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return colliders();
    }

    public static int OverlapCapsuleNonAlloc(Vector2 point, Vector2 size, int direction, float angle, Collider2D[] results, int layerMask, float minDepth, float maxDepth) {
        PhysicsQuery.begin();
        PhysicsQuery.capsule(point.x, point.y, size.x, size.y, direction, angle);
        PhysicsQuery.where(layerMask, minDepth, maxDepth);
        PhysicsQuery.overlap();
        return into(results);
    }
}
