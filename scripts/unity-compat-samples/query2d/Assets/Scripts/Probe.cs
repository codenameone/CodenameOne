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
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

// Asks the physics world questions and logs every answer in hundredths of a
// unit: a ray, a line, a circle, a box and a capsule cast against each kind
// of collider, the overlap tests, layer masks, triggers and contact filters.
// Then the hierarchy: objects found by a path, and components looked for
// below an object that is inactive. A coroutine that yields other routines
// without starting them, and the clock as a fixed step reads it.
public class Probe : MonoBehaviour
{
    public LayerMask walls;
    public Collider2D crate;
    public Collider2D floor;
    public Transform disc;

    private readonly RaycastHit2D[] buffer = new RaycastHit2D[2];
    private readonly Collider2D[] found = new Collider2D[8];
    private int frame;
    private int ticks;
    private Coroutine stopped;
    private int spins;
    private bool logUpdate;

    private static int Cm(float v)
    {
        return Mathf.RoundToInt(v * 100f);
    }

    private static string Hit(RaycastHit2D h)
    {
        if (h.collider == null)
        {
            return "none";
        }
        return h.collider.name + " d=" + Cm(h.distance) + " p=" + Cm(h.point.x) + "," + Cm(h.point.y)
            + " n=" + Cm(h.normal.x) + "," + Cm(h.normal.y) + " f=" + Cm(h.fraction);
    }

    private static string Names(RaycastHit2D[] hits, int count)
    {
        string s = "" + count;
        for (int i = 0; i < count; i++)
        {
            s += " " + hits[i].collider.name + "@" + Cm(hits[i].distance);
        }
        return s;
    }

    private static string Names(Collider2D[] cols, int count)
    {
        string s = "" + count;
        for (int i = 0; i < count; i++)
        {
            s += " " + cols[i].name;
        }
        return s;
    }

    private static string Name(Collider2D c)
    {
        return c == null ? "none" : c.name;
    }

    private void Start()
    {
        Rays();
        Masks();
        Casts();
        Overlaps();
        Hierarchy();
        StartCoroutine(Outer());
        stopped = StartCoroutine(Stopped());
    }

    private static string Path(GameObject go)
    {
        if (go == null)
        {
            return "none";
        }
        string s = go.name;
        for (Transform t = go.transform.parent; t != null; t = t.parent)
        {
            s = t.name + "/" + s;
        }
        return s;
    }

    private static string Name(Component c)
    {
        return c == null ? "none" : c.name;
    }

    // Menu/Panel/Button, and beside it a second Panel/Button with no Menu
    // above it. No colliders: the questions above are asked again later.
    private void Hierarchy()
    {
        GameObject menu = new GameObject("Menu");
        GameObject panel = new GameObject("Panel");
        panel.transform.SetParent(menu.transform);
        GameObject button = new GameObject("Button");
        button.transform.SetParent(panel.transform);
        GameObject loose = new GameObject("Panel");
        GameObject stray = new GameObject("Button");
        stray.transform.SetParent(loose.transform);
        stray.AddComponent<SpriteRenderer>();

        // A name with a slash is a path; a leading slash starts at a root.
        Debug.Log("find " + Path(GameObject.Find("Menu/Panel/Button")) + " " + Path(GameObject.Find("Panel/Button"))
            + " " + Path(GameObject.Find("/Panel/Button")).Length + " " + Path(GameObject.Find("/Menu")) + " "
            + Path(GameObject.Find("/Menu/Panel")) + " " + Path(GameObject.Find("/Button")) + " "
            + Path(GameObject.Find("Menu/Button")) + " " + Path(GameObject.Find("Panel/Menu")) + " "
            + Path(GameObject.Find("Menu/")) + " " + (GameObject.Find("/Panel/Button") == stray)
            + (GameObject.Find("Button") != null));

        menu.AddComponent<SpriteRenderer>();
        panel.AddComponent<SpriteRenderer>();
        button.AddComponent<SpriteRenderer>();
        Debug.Log("children " + menu.GetComponentsInChildren<SpriteRenderer>().Length + " "
            + Name(panel.GetComponentInChildren<SpriteRenderer>()) + " "
            + Name(loose.GetComponentInChildren<SpriteRenderer>()));
        // The object asked is searched though it is inactive; nothing below
        // an inactive object is, and an inactive object is not found.
        panel.SetActive(false);
        Debug.Log("inactive " + menu.GetComponentsInChildren<SpriteRenderer>().Length + " "
            + Name(panel.GetComponentInChildren<SpriteRenderer>()) + " "
            + panel.GetComponentsInChildren<SpriteRenderer>().Length + " "
            + Name(button.transform.GetComponentInChildren<SpriteRenderer>()) + " "
            + button.GetComponentsInChildren<SpriteRenderer>().Length + " "
            + Path(GameObject.Find("Menu/Panel/Button")) + " " + Path(GameObject.Find("Menu/Panel")) + " "
            + Path(GameObject.Find("Panel/Button")));
        loose.SetActive(false);
        Debug.Log("inactive root " + Name(loose.GetComponentInChildren<SpriteRenderer>()) + " "
            + loose.GetComponentsInChildren<SpriteRenderer>().Length + " "
            + Name(stray.GetComponentInChildren<SpriteRenderer>()));
        Destroy(menu);
        Destroy(loose);
    }

    // A routine yielded without StartCoroutine is run to its end before the
    // one that yielded it goes on, whatever it waits for on the way.
    private IEnumerator Outer()
    {
        Debug.Log("outer starts frame=" + Time.frameCount);
        yield return Inner("a", 2);
        Debug.Log("outer after a frame=" + Time.frameCount);
        yield return Empty();
        Debug.Log("outer after empty frame=" + Time.frameCount);
        yield return Inner("b", 1);
        Debug.Log("outer ends frame=" + Time.frameCount);
    }

    private IEnumerator Inner(string name, int frames)
    {
        Debug.Log("inner " + name + " starts frame=" + Time.frameCount);
        for (int i = 0; i < frames; i++)
        {
            yield return null;
        }
        Debug.Log("inner " + name + " waited frame=" + Time.frameCount);
        yield return Deep(name);
        yield return new WaitForSeconds(0.05f);
        Debug.Log("inner " + name + " ends frame=" + Time.frameCount);
    }

    private IEnumerator Deep(string name)
    {
        yield return new WaitForFixedUpdate();
        Debug.Log("deep " + name + " step=" + frame + " frame=" + Time.frameCount);
    }

    private IEnumerator Empty()
    {
        Debug.Log("empty frame=" + Time.frameCount);
        yield break;
    }

    // Stopping the coroutine stops the routine it was waiting in.
    private IEnumerator Stopped()
    {
        yield return Forever();
        Debug.Log("stopped went on");
    }

    private IEnumerator Forever()
    {
        while (true)
        {
            ticks++;
            yield return null;
        }
    }

    // A child that ends without yielding is not a frame: this never reaches
    // the end of the one it starts in. Unity's player stops for good in it;
    // this runtime gives it up after a million children, says so in the log
    // and goes on in the next frame, which is what the count below holds.
    private IEnumerator Spin()
    {
        while (true)
        {
            spins++;
            yield return Nothing();
        }
    }

    private IEnumerator Nothing()
    {
        yield break;
    }

    private static int Tenths(float seconds)
    {
        return Mathf.RoundToInt(seconds * 10000f);
    }

    private void Update()
    {
        if (logUpdate)
        {
            logUpdate = false;
            Debug.Log("update clock dt=" + Tenths(Time.deltaTime) + " unscaled=" + Tenths(Time.unscaledDeltaTime)
                + " fixed=" + Tenths(Time.fixedDeltaTime) + " fixedUnscaled=" + Tenths(Time.fixedUnscaledDeltaTime));
        }
    }

    private void Rays()
    {
        Debug.Log("ray box " + Hit(Physics2D.Raycast(new Vector2(0f, 2.75f), Vector2.right, 10f, walls)));
        Debug.Log("ray disc " + Hit(Physics2D.Raycast(new Vector2(0f, 1f), Vector2.right)));
        Debug.Log("ray disc oblique " + Hit(Physics2D.Raycast(new Vector2(0f, 1.3f), Vector2.right)));
        Debug.Log("ray pill side " + Hit(Physics2D.Raycast(new Vector2(0f, -1f), Vector2.right)));
        Debug.Log("ray pill cap " + Hit(Physics2D.Raycast(new Vector2(4.3f, 2f), Vector2.down, 10f, LayerMask.GetMask("Props"))));
        Debug.Log("ray log " + Hit(Physics2D.Raycast(new Vector2(-4f, 1.5f), Vector2.down, 10f, LayerMask.GetMask("Props"))));
        Debug.Log("ray ramp flat " + Hit(Physics2D.Raycast(new Vector2(-2f, -1f), Vector2.down)));
        Debug.Log("ray ramp slope " + Hit(Physics2D.Raycast(new Vector2(0f, -1f), Vector2.down)));
        Debug.Log("ray ramp below " + Hit(Physics2D.Raycast(new Vector2(0f, -4.5f), Vector2.up)));
        Debug.Log("ray ell notch " + Hit(Physics2D.Raycast(new Vector2(-2.5f, 5f), Vector2.down)));
        Debug.Log("ray ell arm " + Hit(Physics2D.Raycast(new Vector2(0f, 3.5f), Vector2.left)));
        Debug.Log("ray short " + Hit(Physics2D.Raycast(new Vector2(0f, 1f), Vector2.right, 3.4f)));
        Debug.Log("ray diagonal " + Hit(Physics2D.Raycast(new Vector2(1f, -1.5f), new Vector2(3f, 3f))));
        RaycastHit2D hit = Physics2D.Raycast(new Vector2(0f, 4.5f), Vector2.right);
        if (hit)
        {
            Debug.Log("ray empty hit something");
        }
        else
        {
            Debug.Log("ray empty misses, as bool false");
        }
        Debug.Log("line " + Hit(Physics2D.Linecast(new Vector2(0f, 1f), new Vector2(7f, 1f))));
        Debug.Log("line short " + Hit(Physics2D.Linecast(new Vector2(0f, 1f), new Vector2(3f, 1f))));
        Debug.Log("line all " + Names(Physics2D.LinecastAll(new Vector2(0f, 3f), new Vector2(6f, 3f)), 2));
    }

    private void Masks()
    {
        Debug.Log("layers Walls=" + LayerMask.NameToLayer("Walls") + " Props=" + LayerMask.NameToLayer("Props")
            + " Missing=" + LayerMask.NameToLayer("Missing") + " 9=" + LayerMask.LayerToName(9)
            + " mask=" + LayerMask.GetMask("Walls", "Sensors") + " walls=" + walls.value);
        int mask = walls;
        LayerMask sensors = 1 << 9;
        Debug.Log("mask int=" + mask + " sensors=" + sensors.value + " default=" + Physics2D.DefaultRaycastLayers);

        Vector2 from = new Vector2(0f, 3f);
        Debug.Log("trigger on " + Hit(Physics2D.Raycast(from, Vector2.right)));
        Debug.Log("trigger masked " + Hit(Physics2D.Raycast(from, Vector2.right, 10f, walls)));
        Debug.Log("trigger only " + Hit(Physics2D.Raycast(from, Vector2.right, 10f, sensors)));
        Physics2D.queriesHitTriggers = false;
        Debug.Log("trigger off " + Hit(Physics2D.Raycast(from, Vector2.right)));
        Debug.Log("trigger off masked " + Hit(Physics2D.Raycast(from, Vector2.right, 10f, sensors)));
        Physics2D.queriesHitTriggers = true;

        RaycastHit2D[] all = Physics2D.RaycastAll(from, Vector2.right);
        Debug.Log("all " + Names(all, all.Length));
        all = Physics2D.RaycastAll(new Vector2(-6f, -1f), Vector2.right, 20f);
        Debug.Log("all row " + Names(all, all.Length));
        all = Physics2D.RaycastAll(new Vector2(-6f, -1f), Vector2.right, 20f, Physics2D.AllLayers, -1f, 1f);
        Debug.Log("all row depth " + Names(all, all.Length));
        int n = Physics2D.RaycastNonAlloc(new Vector2(-6f, -1f), Vector2.right, buffer, 20f);
        Debug.Log("nonalloc " + Names(buffer, n));

        ContactFilter2D filter = new ContactFilter2D();
        filter.useTriggers = false;
        filter.SetLayerMask(walls);
        RaycastHit2D[] three = new RaycastHit2D[3];
        n = Physics2D.Raycast(from, Vector2.right, filter, three, 10f);
        Debug.Log("filter " + Names(three, n) + " filtering=" + filter.isFiltering);
        filter.NoFilter();
        List<RaycastHit2D> list = new List<RaycastHit2D>();
        n = Physics2D.Raycast(from, Vector2.right, filter, list);
        Debug.Log("filter list " + n + " " + list.Count + " first=" + list[0].collider.name);

        Vector2 inside = new Vector2(4f, 3f);
        Debug.Log("inside " + Hit(Physics2D.Raycast(inside, Vector2.right)));
        Physics2D.queriesStartInColliders = false;
        Debug.Log("inside off " + Hit(Physics2D.Raycast(inside, Vector2.right)));
        Debug.Log("inside off down " + Hit(Physics2D.Raycast(inside, Vector2.down)));
        Physics2D.queriesStartInColliders = true;
    }

    private void Casts()
    {
        Vector2 from = new Vector2(0f, 2.75f);
        Debug.Log("circle box " + Hit(Physics2D.CircleCast(from, 0.25f, Vector2.right, 10f, walls)));
        Debug.Log("circle disc " + Hit(Physics2D.CircleCast(new Vector2(0f, 1.6f), 0.5f, Vector2.right)));
        Debug.Log("circle corner " + Hit(Physics2D.CircleCast(new Vector2(0f, 3.8f), 0.5f, Vector2.right, 10f, walls)));
        Debug.Log("circle ramp " + Hit(Physics2D.CircleCast(new Vector2(0f, -1f), 0.5f, Vector2.down)));
        Debug.Log("circle miss " + Hit(Physics2D.CircleCast(new Vector2(0f, 4.6f), 0.5f, Vector2.right)));
        RaycastHit2D[] all = Physics2D.CircleCastAll(new Vector2(0f, 3f), 0.25f, Vector2.right);
        Debug.Log("circle all " + Names(all, all.Length));

        Vector2 unit = new Vector2(1f, 1f);
        Debug.Log("box box " + Hit(Physics2D.BoxCast(from, unit, 0f, Vector2.right, 10f, walls)));
        Debug.Log("box turned " + Hit(Physics2D.BoxCast(from, unit, 45f, Vector2.right, 10f, walls)));
        Debug.Log("box disc " + Hit(Physics2D.BoxCast(new Vector2(0f, 1f), unit, 0f, Vector2.right)));
        Debug.Log("box disc corner " + Hit(Physics2D.BoxCast(new Vector2(0f, 1.8f), unit, 0f, Vector2.right)));
        Debug.Log("box slope " + Hit(Physics2D.BoxCast(new Vector2(0f, -1f), unit, 0f, Vector2.down)));
        Debug.Log("box pill " + Hit(Physics2D.BoxCast(new Vector2(0f, -1f), unit, 30f, Vector2.right)));
        Debug.Log("box inside " + Hit(Physics2D.BoxCast(new Vector2(3.5f, 3f), unit, 0f, Vector2.right, 10f, walls)));
        all = Physics2D.BoxCastAll(new Vector2(0f, 3f), unit, 0f, Vector2.right);
        Debug.Log("box all " + Names(all, all.Length));
        Debug.Log("box nonalloc " + Names(buffer, Physics2D.BoxCastNonAlloc(new Vector2(-6f, -1f), unit, 0f, Vector2.right, buffer)));

        Debug.Log("capsule box " + Hit(Physics2D.CapsuleCast(from, new Vector2(1f, 2f), CapsuleDirection2D.Vertical, 0f,
            Vector2.right, 10f, walls)));
        Debug.Log("capsule disc " + Hit(Physics2D.CapsuleCast(new Vector2(0f, 1f), new Vector2(2f, 1f),
            CapsuleDirection2D.Horizontal, 0f, Vector2.right)));
    }

    private void Overlaps()
    {
        Debug.Log("point box " + Name(Physics2D.OverlapPoint(new Vector2(4.5f, 3.2f))));
        Debug.Log("point empty " + Name(Physics2D.OverlapPoint(new Vector2(0f, 0f))));
        Debug.Log("point notch " + Name(Physics2D.OverlapPoint(new Vector2(-2.5f, 3.5f))));
        Debug.Log("point ell " + Name(Physics2D.OverlapPoint(new Vector2(-3.5f, 3.5f))));
        Debug.Log("point pill cap " + Name(Physics2D.OverlapPoint(new Vector2(4.3f, -0.2f))));
        Debug.Log("point pill corner " + Name(Physics2D.OverlapPoint(new Vector2(4.45f, -0.05f))));
        Debug.Log("point log " + Name(Physics2D.OverlapPoint(new Vector2(-3.3f, -0.6f))));
        Debug.Log("point edge " + Name(Physics2D.OverlapPoint(new Vector2(-2f, -3f))));
        Debug.Log("circle " + Name(Physics2D.OverlapCircle(new Vector2(3f, 1f), 0.6f)));
        Debug.Log("circle short " + Name(Physics2D.OverlapCircle(new Vector2(3f, 1f), 0.4f)));
        Debug.Log("circle edge " + Name(Physics2D.OverlapCircle(new Vector2(-2f, -2.8f), 0.25f)));
        Collider2D[] all = Physics2D.OverlapCircleAll(new Vector2(4f, 0f), 3.2f);
        Debug.Log("circle all " + Names(all, all.Length));
        all = Physics2D.OverlapCircleAll(new Vector2(4f, 0f), 3.2f, Physics2D.AllLayers, -1f, 1f);
        Debug.Log("circle all depth " + Names(all, all.Length));
        all = Physics2D.OverlapCircleAll(new Vector2(4f, 0f), 3.2f, LayerMask.GetMask("Props", "Sensors"));
        Debug.Log("circle all masked " + Names(all, all.Length));
        Debug.Log("circle nonalloc " + Names(found, Physics2D.OverlapCircleNonAlloc(new Vector2(4f, 0f), 3.2f, found)));
        Debug.Log("box " + Name(Physics2D.OverlapBox(new Vector2(2.6f, 1f), new Vector2(2f, 0.5f), 0f)));
        Debug.Log("box short " + Name(Physics2D.OverlapBox(new Vector2(2.4f, 1f), new Vector2(2f, 0.5f), 0f)));
        Debug.Log("box turned " + Name(Physics2D.OverlapBox(new Vector2(2.4f, 1f), new Vector2(2f, 0.5f), 20f)));
        all = Physics2D.OverlapBoxAll(new Vector2(0f, -2.5f), new Vector2(12f, 1.4f), 0f);
        Debug.Log("box all " + Names(all, all.Length));
        Debug.Log("area " + Name(Physics2D.OverlapArea(new Vector2(1f, 2f), new Vector2(1.6f, 4f))));
        all = Physics2D.OverlapAreaAll(new Vector2(-6f, -5f), new Vector2(6f, 5f), walls);
        Debug.Log("area all " + Names(all, all.Length));
        Debug.Log("capsule " + Name(Physics2D.OverlapCapsule(new Vector2(2.6f, 1f), new Vector2(2f, 1f),
            CapsuleDirection2D.Horizontal, 0f)));
        ContactFilter2D filter = new ContactFilter2D();
        filter.SetLayerMask(LayerMask.GetMask("Props"));
        Debug.Log("filter " + Names(found, Physics2D.OverlapCircle(new Vector2(4f, 0f), 3.2f, filter, found)));

        Debug.Log("own point " + floor.OverlapPoint(new Vector2(-5.9f, -3.6f)) + " " + floor.OverlapPoint(new Vector2(-6.1f, -3.6f)));
        Collider2D pill = Physics2D.OverlapPoint(new Vector2(4f, -1f));
        Collider2D ramp = Physics2D.OverlapCircle(new Vector2(-2f, -2.8f), 0.25f);
        Collider2D log = Physics2D.OverlapPoint(new Vector2(-4f, -1f), LayerMask.GetMask("Props"));
        Debug.Log("bounds pill " + Box(pill.bounds) + " ramp " + Box(ramp.bounds) + " log " + Box(log.bounds)
            + " floor " + Box(floor.bounds));
    }

    private static string Box(Bounds b)
    {
        return Cm(b.min.x) + "," + Cm(b.min.y) + ".." + Cm(b.max.x) + "," + Cm(b.max.y);
    }

    private void FixedUpdate()
    {
        frame++;
        if (frame == 5)
        {
            // The crate is still falling: it touches nothing.
            Debug.Log("falling touching=" + crate.IsTouching(floor) + " layers=" + crate.IsTouchingLayers(walls)
                + " y=" + Cm(crate.transform.position.y));
        }
        if (frame == 20)
        {
            Coroutine spinning = StartCoroutine(Spin());
            Debug.Log("spin given up spins=" + spins);
            StopCoroutine(spinning);
        }
        if (frame == 21)
        {
            Debug.Log("spin stopped spins=" + spins);
        }
        if (frame == 8)
        {
            StopCoroutine(stopped);
            Debug.Log("stopping ticks=" + ticks);
        }
        if (frame == 61)
        {
            // Half speed: a step is still 0.02 seconds of the game, and now
            // 0.04 of the clock on the wall.
            Debug.Log("stopped ticks=" + ticks + " fixed clock dt=" + Tenths(Time.deltaTime) + " unscaled="
                + Tenths(Time.unscaledDeltaTime) + " fixedUnscaled=" + Tenths(Time.fixedUnscaledDeltaTime)
                + " ahead=" + (Time.unscaledTime == Time.fixedUnscaledTime) + (Time.time == Time.fixedTime));
            Time.timeScale = 0.5f;
        }
        if (frame == 62)
        {
            Debug.Log("fixed clock half dt=" + Tenths(Time.deltaTime) + " unscaled=" + Tenths(Time.unscaledDeltaTime)
                + " fixedUnscaled=" + Tenths(Time.fixedUnscaledDeltaTime));
            logUpdate = true;
        }
        if (frame == 63)
        {
            Time.timeScale = 1f;
        }
        if (frame != 60)
        {
            return;
        }
        Rigidbody2D body = crate.attachedRigidbody;
        Debug.Log("resting touching=" + crate.IsTouching(floor) + " " + Physics2D.IsTouching(floor, crate)
            + " layers=" + crate.IsTouchingLayers(walls) + " props=" + crate.IsTouchingLayers(LayerMask.GetMask("Props"))
            + " body=" + body.IsTouchingLayers() + " " + body.IsTouching(floor)
            + " y=" + Cm(crate.transform.position.y));
        int n = body.Cast(Vector2.up, buffer, 5f);
        Debug.Log("body cast up " + Names(buffer, n));
        n = body.Cast(Vector2.right, buffer, 5f);
        Debug.Log("body cast right " + Names(buffer, n));
        n = crate.Cast(Vector2.up, buffer, 0.5f);
        Debug.Log("collider cast short " + Names(buffer, n));
        Debug.Log("body point " + body.OverlapPoint(new Vector2(-4f, -3f)) + " " + body.OverlapPoint(new Vector2(-4f, 0f)));

        // A collider moved through its transform is found where physics last
        // put it until the transforms are synchronised.
        disc.position = new Vector3(4f, 0.5f, 0f);
        Debug.Log("moved stale " + Hit(Physics2D.Raycast(new Vector2(0f, 0.9f), Vector2.right, 10f, 1)));
        Physics2D.SyncTransforms();
        Debug.Log("moved synced " + Hit(Physics2D.Raycast(new Vector2(0f, 0.9f), Vector2.right, 10f, 1)));
    }
}
