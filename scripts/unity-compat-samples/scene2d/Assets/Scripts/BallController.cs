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
        yield return new WaitForSeconds(0.5f);
        Debug.Log(label + " half a second in, frame " + Time.frameCount + " y=" + Cm(transform.position.y));
        yield return null;
        Debug.Log(label + " one frame later, frame " + Time.frameCount);
        yield return StartCoroutine(Blink(2));
        Debug.Log(label + " blinking done, frame " + Time.frameCount);
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

    private void Update()
    {
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
