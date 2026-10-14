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
using UnityEngine;
using UnityEngine.SceneManagement;

// Says what a scene finds when it starts, and when a coroutine that yields
// null comes back: in the frame after the one it was started in, whether that
// was from Start or from Update, and at the end of the same frame when it
// waits for that instead.
//
// Then what the engine answers at its edges: a routine that ends before it
// yields is no coroutine, a range given backwards keeps which end it leaves
// out, and the built-in components can be added by type. After its first
// Update the script switches itself off, and is still told when the
// application is paused, as a script on an active object is.
public class Marker : MonoBehaviour
{
    private bool updated;

    private void Start()
    {
        Debug.Log("marker start in " + SceneManager.GetActiveScene().name + " keeper=" + (Keeper.Instance != null)
            + " total=" + Keeper.Instance.total + " keepers=" + FindObjectsOfType<Keeper>().Length
            + " frame " + Time.frameCount);
        Coroutine waits = StartCoroutine(Next("start", null));
        Debug.Log("marker " + Roots());
        Debug.Log("marker " + Edges(waits));
        Debug.Log("marker " + Uniform());
        Debug.Log("marker " + InParent());
        StartCoroutine(AfterEmpty());
    }

    // GetComponentInParent searches the object itself whatever its state,
    // and of its parents only those active in the hierarchy.
    private static string InParent()
    {
        GameObject root = new GameObject("root");
        root.AddComponent<CapsuleCollider2D>();
        GameObject off = new GameObject("off");
        off.AddComponent<CapsuleCollider2D>();
        off.transform.SetParent(root.transform);
        off.SetActive(false);
        GameObject plain = new GameObject("plain");
        plain.transform.SetParent(off.transform);
        GameObject own = new GameObject("own");
        own.AddComponent<CapsuleCollider2D>();
        own.transform.SetParent(off.transform);
        GameObject offRoot = new GameObject("offRoot");
        offRoot.AddComponent<CapsuleCollider2D>();
        offRoot.SetActive(false);
        GameObject under = new GameObject("under");
        under.transform.SetParent(offRoot.transform);
        GameObject leaf = new GameObject("leaf");
        leaf.transform.SetParent(under.transform);
        string s = "in parent " + Owner(plain.GetComponentInParent<CapsuleCollider2D>()) + " "
            + Owner(own.GetComponentInParent<CapsuleCollider2D>()) + " "
            + Owner(leaf.GetComponentInParent<CapsuleCollider2D>()) + " "
            + Owner(plain.transform.GetComponentInParent<CapsuleCollider2D>()) + " "
            + Owner(off.GetComponentInParent<CapsuleCollider2D>());
        Destroy(root);
        Destroy(offRoot);
        return s;
    }

    private static string Owner(Component c)
    {
        return c == null ? "none" : c.gameObject.name;
    }

    // An int range of three quarters of 2^32: one 32-bit sample reduced
    // with a remainder alone lands in the first third of it half the time,
    // where every value equally likely lands there a third of the time.
    private static string Uniform()
    {
        int low = 0;
        for (int i = 0; i < 3000; i++)
        {
            if (Random.Range(-1610612736, 1610612736) < -536870912)
            {
                low++;
            }
        }
        return "range uniform=" + (low > 900 && low < 1100);
    }

    private string Edges(Coroutine waits)
    {
        string s = "empty coroutine null=" + (StartCoroutine(Empty()) == null) + " waiting null=" + (waits == null);
        StopCoroutine(null);
        // Nothing else in this project draws a random number, so these take
        // none from anybody.
        int low = int.MaxValue, high = int.MinValue;
        float lowF = float.MaxValue, highF = float.MinValue;
        for (int i = 0; i < 400; i++)
        {
            int n = Random.Range(10, 0);
            low = Mathf.Min(low, n);
            high = Mathf.Max(high, n);
            float f = Random.Range(2f, 1f);
            lowF = Mathf.Min(lowF, f);
            highF = Mathf.Max(highF, f);
        }
        s += " range(10,0)=" + low + ".." + high + " same=" + Random.Range(5, 5) + " floats "
            + (lowF >= 1f && highF <= 2f && lowF < 1.1f && highF > 1.9f) + " same="
            + Random.Range(3f, 3f);
        GameObject parts = new GameObject("parts");
        CapsuleCollider2D capsule = parts.AddComponent<CapsuleCollider2D>();
        EdgeCollider2D edge = parts.AddComponent<EdgeCollider2D>();
        AudioSource source = parts.AddComponent<AudioSource>();
        s += " added " + (parts.GetComponent<Collider2D>() == capsule) + " " + edge.pointCount + " "
            + (source != null && !source.isPlaying) + " of " + parts.GetComponents<Component>().Length;
        Destroy(parts);
        return s;
    }

    private IEnumerator Empty()
    {
        yield break;
    }

    // Waiting for a routine that was over before it yielded is waiting for
    // nothing, which is a frame.
    private IEnumerator AfterEmpty()
    {
        int began = Time.frameCount;
        yield return StartCoroutine(Empty());
        Debug.Log("marker after an empty coroutine began " + began + " resumed " + Time.frameCount);
    }

    private void OnApplicationPause(bool paused)
    {
        Debug.Log("marker pause=" + paused + " enabled=" + enabled + " frame " + Time.frameCount);
    }

    private void OnApplicationFocus(bool focus)
    {
        Debug.Log("marker focus=" + focus + " enabled=" + enabled + " frame " + Time.frameCount);
    }

    // An object with no parent has a place among the roots of its scene,
    // which it can read and change as a child can among its siblings.
    private string Roots()
    {
        Transform t = transform;
        string s = "root " + t.GetSiblingIndex();
        t.SetAsFirstSibling();
        s += " first " + t.GetSiblingIndex();
        Transform made = new GameObject("made").transform;
        int last = made.GetSiblingIndex();
        s += " made " + last;
        t.SetSiblingIndex(2);
        s += " at two " + t.GetSiblingIndex() + " made " + made.GetSiblingIndex();
        t.SetAsLastSibling();
        s += " last " + (t.GetSiblingIndex() == last) + " made " + (made.GetSiblingIndex() == last - 1);
        made.SetParent(t);
        s += " under " + made.GetSiblingIndex() + " last " + (t.GetSiblingIndex() == last - 1);
        made.SetParent(null);
        s += " out " + (made.GetSiblingIndex() == last);
        made.SetSiblingIndex(-3);
        t.SetSiblingIndex(1000);
        s += " ends " + made.GetSiblingIndex() + " " + (t.GetSiblingIndex() == last);
        Destroy(made.gameObject);
        return s;
    }

    private void Update()
    {
        if (!updated)
        {
            updated = true;
            StartCoroutine(Next("update", null));
            StartCoroutine(Next("update end of frame", new WaitForEndOfFrame()));
            // Its coroutines go on: they belong to the object's being active.
            enabled = false;
        }
    }

    private IEnumerator Next(string from, object wait)
    {
        int began = Time.frameCount;
        yield return wait;
        int resumed = Time.frameCount;
        yield return null;
        Debug.Log("marker coroutine from " + from + " began " + began + " resumed " + resumed + " again "
            + Time.frameCount);
    }
}
