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

// A ball that falls, bounces, counts its landings, and jumps when tapped.
// Everything it logs is an integer: the trace is compared between a JVM and
// a native build, and the two need not print a float the same way.
public class BallController : MonoBehaviour
{
    public float jumpImpulse = 3f;
    public int maxBounces = 2;
    public string label = "unnamed";
    public Vector2 nudge;
    public Transform ground;
    public GameObject marker;
    public bool verbose;
    public Mood mood;

    private Rigidbody2D body;
    private int bounces;
    private int fixedSteps;
    private float lowest = 1000f;
    private int hitFrame;
    private int fixedFrame;

    public enum Mood
    {
        Calm,
        Bouncy
    }

    private void Awake()
    {
        body = GetComponent<Rigidbody2D>();
        Debug.Log(label + " awake body=" + (body != null) + " mood=" + (int)mood + " verbose=" + verbose);
    }

    private IEnumerator Start()
    {
        Vector3 p = transform.position;
        Debug.Log(label + " start at " + Cm(p.x) + "," + Cm(p.y) + " ground at " + Cm(ground.position.y)
            + " marker=" + marker.name + " nudge=" + Cm(nudge.x) + "," + Cm(nudge.y));
        body.velocity = nudge;
        Managers();
        yield return new WaitForSeconds(0.5f);
        Debug.Log(label + " half a second in, frame " + Time.frameCount + " y=" + Cm(transform.position.y));
        yield return null;
        Debug.Log(label + " one frame later, frame " + Time.frameCount);
        yield return StartCoroutine(Blink(2));
        Debug.Log(label + " blinking done, frame " + Time.frameCount);
    }

    // Two managers off one generic base, each the instance of its own
    // constructed type; a second audio manager, which finds the first one in
    // place and removes itself; and two lookups by type argument.
    private void Managers()
    {
        GameObject managers = new GameObject("Managers");
        managers.AddComponent<GameManager>();
        managers.AddComponent<AudioManager>();
        new GameObject("Managers again").AddComponent<AudioManager>();
        Debug.Log(label + " managers score=" + GameManager.Instance.score + " clip=" + AudioManager.Instance.clip
            + " awakened=" + Singleton<GameManager>.Awakened + "," + Singleton<AudioManager>.Awakened + " kept="
            + (AudioManager.Instance.gameObject == managers) + " found=" + Locator<Spinner>.Get().name + ","
            + Locator<BallController>.Get().name + "," + Locator<Spinner>.Get().name + " searches="
            + Locator<Spinner>.Searches + "," + Locator<BallController>.Searches + ","
            + Locator<AudioManager>.Searches);
    }

    private IEnumerator Blink(int times)
    {
        SpriteRenderer sprite = GetComponent<SpriteRenderer>();
        for (int i = 0; i < times; i++)
        {
            sprite.color = i % 2 == 0 ? Color.red : Color.white;
            Debug.Log(label + " blink " + i + " red=" + Cm(sprite.color.r) + " green=" + Cm(sprite.color.g));
            yield return new WaitForSeconds(0.1f);
        }
    }

    // A wait is counted from the frame it is yielded in, whichever message of
    // that frame yields it: one asked for in Start, in FixedUpdate or in a
    // collision message is over on the frame the same wait asked for in that
    // frame's Update is, and not a frame sooner.
    private IEnumerator Waited(string from, int milliseconds)
    {
        int began = Time.frameCount;
        yield return new WaitForSeconds(milliseconds / 1000f);
        Debug.Log(label + " waited " + milliseconds + "ms from " + from + ", began " + began + " resumed "
            + Time.frameCount);
    }

    private void Update()
    {
        if (Time.frameCount == 1)
        {
            // Beside the half second Start waits, above.
            StartCoroutine(Waited("update", 500));
        }
        if (Time.frameCount == hitFrame)
        {
            StartCoroutine(Waited("update beside collision", 250));
        }
        if (Time.frameCount == fixedFrame)
        {
            StartCoroutine(Waited("update beside fixed", 250));
        }
        if (Input.GetMouseButtonDown(0))
        {
            Vector3 at = Input.mousePosition;
            Debug.Log(label + " tapped at " + (int)at.x + "," + (int)at.y + " frame " + Time.frameCount);
            body.AddForce(Vector2.up * jumpImpulse, ForceMode2D.Impulse);
        }
    }

    private void FixedUpdate()
    {
        fixedSteps++;
        if (bounces == 1 && fixedFrame == 0)
        {
            fixedFrame = Time.frameCount;
            StartCoroutine(Waited("fixed", 250));
        }
        float y = transform.position.y;
        if (y < lowest)
        {
            lowest = y;
        }
    }

    private void OnCollisionEnter2D(Collision2D collision)
    {
        bounces++;
        Debug.Log(label + " hit " + collision.gameObject.name + " bounce " + bounces + " after " + fixedSteps
            + " steps, y=" + Cm(transform.position.y) + " closing=" + Cm(Mathf.Abs(collision.relativeVelocity.y)));
        if (bounces == 1)
        {
            hitFrame = Time.frameCount;
            StartCoroutine(Waited("collision", 250));
        }
        if (bounces == maxBounces)
        {
            Debug.Log(label + " retiring the marker");
            Destroy(marker);
        }
    }

    public string Report()
    {
        return label + " bounces=" + bounces + " steps=" + fixedSteps + " lowest=" + Cm(lowest) + " y="
            + Cm(transform.position.y) + " vy=" + Cm(body.velocity.y) + " marker gone=" + (marker == null);
    }

    private static int Cm(float metres)
    {
        return Mathf.RoundToInt(metres * 100f);
    }
}
