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
using System;
using System.Collections.Generic;
using UnityEngine;

// A script whose fields hold [Serializable] classes, which Unity writes into
// the object that holds them: by value. Instantiate of a live object therefore
// gives the copy instances of its own -- directly in a field, in an array, in
// a list, and one inside another -- and what the copy does to its own does not
// reach the object it was copied from.
public class Probe : MonoBehaviour
{
    [Serializable]
    public class Base
    {
        public int health = 3;
    }

    [Serializable]
    public class Stats : Base
    {
        public Vector2 aim;
        public int[] marks = new int[2];
        public Stats inner;
        public Transform owner;
        [NonSerialized] public int scratch;
        [SerializeField] private int secret;

        public int Secret
        {
            get { return secret; }
            set { secret = value; }
        }
    }

    public Stats stats = new Stats();
    public Stats[] squad;
    public List<Stats> reserve;

    private string Describe()
    {
        return stats.health + "/" + stats.aim.x + "/" + stats.marks[0] + "/" + stats.inner.health + "/"
            + squad[0].health + "/" + reserve[0].health + "/" + stats.scratch + "/" + stats.Secret;
    }

    // Called by the player's Start, so that its line has a place in the trace.
    public static void Check()
    {
        GameObject source = new GameObject("probe");
        Probe a = source.AddComponent<Probe>();
        a.stats.health = 5;
        a.stats.aim = new Vector2(2f, 0f);
        a.stats.inner = new Stats();
        a.stats.owner = source.transform;
        a.stats.scratch = 4;
        a.stats.Secret = 6;
        a.squad = new Stats[] { new Stats() };
        a.reserve = new List<Stats>();
        a.reserve.Add(new Stats());

        GameObject copy = Instantiate(source);
        Probe b = copy.GetComponent<Probe>();
        string taken = b.Describe();
        b.stats.health = 1;
        b.stats.aim.x = 7f;
        b.stats.marks[0] = 9;
        b.stats.inner.health = 8;
        b.squad[0].health = 11;
        b.reserve[0].health = 12;
        b.stats.Secret = 13;
        Debug.Log("probe copied " + taken + " now " + b.Describe() + " source " + a.Describe() + " owner "
            + (b.stats.owner == copy.transform) + (a.stats.owner == source.transform) + " own "
            + (b.stats != a.stats) + (b.stats.inner != a.stats.inner) + (b.squad[0] != a.squad[0])
            + (b.reserve[0] != a.reserve[0]));
        Destroy(source);
        Destroy(copy);
    }

    private static string Pose(Transform t)
    {
        return Player.Cm(t.position.x) + "," + Player.Cm(t.position.y) + "@" + Mathf.RoundToInt(t.eulerAngles.z)
            + " local " + Player.Cm(t.localPosition.x) + "," + Player.Cm(t.localPosition.y) + "@"
            + Mathf.RoundToInt(t.localEulerAngles.z) + "x" + Player.Cm(t.localScale.x);
    }

    // Where a copy of a CHILD is put. With no parent, or when asked to stay
    // in the world, it is where the original is in the world; given a parent
    // and nothing else, it keeps the original's local values under the new
    // one; and a position and rotation passed in are the world's.
    public static void Poses()
    {
        GameObject home = new GameObject("home");
        home.transform.position = new Vector3(1f, 2f, 0f);
        home.transform.rotation = Quaternion.Euler(0f, 0f, 90f);
        home.transform.localScale = new Vector3(2f, 2f, 1f);
        GameObject child = new GameObject("child");
        child.transform.SetParent(home.transform, false);
        child.transform.localPosition = new Vector3(1f, 0f, 0f);
        child.transform.localRotation = Quaternion.Euler(0f, 0f, 30f);
        child.transform.localScale = new Vector3(0.5f, 0.5f, 1f);
        GameObject other = new GameObject("other");
        other.transform.position = new Vector3(10f, 0f, 0f);
        other.transform.rotation = Quaternion.Euler(0f, 0f, 270f);
        other.transform.localScale = new Vector3(5f, 5f, 1f);

        Vector3 at = new Vector3(5f, 5f, 0f);
        Quaternion turned = Quaternion.Euler(0f, 0f, 45f);
        GameObject free = Instantiate(child);
        GameObject local = Instantiate(child, other.transform);
        GameObject world = Instantiate(child, other.transform, true);
        GameObject placed = Instantiate(child, at, turned);
        GameObject both = Instantiate(child, at, turned, other.transform);
        Debug.Log("pose child " + Pose(child.transform));
        Debug.Log("pose free " + Pose(free.transform) + " root=" + (free.transform.parent == null));
        Debug.Log("pose local " + Pose(local.transform) + " under=" + local.transform.parent.name);
        Debug.Log("pose world " + Pose(world.transform) + " under=" + world.transform.parent.name);
        Debug.Log("pose placed " + Pose(placed.transform) + " root=" + (placed.transform.parent == null));
        Debug.Log("pose both " + Pose(both.transform) + " under=" + both.transform.parent.name);
        Destroy(home);
        Destroy(other);
        Destroy(free);
        Destroy(placed);
    }
}
