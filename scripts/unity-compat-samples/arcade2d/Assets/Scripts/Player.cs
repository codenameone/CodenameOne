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
using System.Collections.Generic;
using System.Collections;
using UnityEngine;
using UnityEngine.UI;

// Turns with the Horizontal axis and fires a prefab from a child transform:
// keyboard input, Instantiate, parenting at run time, Invoke, and fields of
// the kinds a scene serializes -- a prefab, scene objects, an array, a list
// and an array of structs.
public class Player : MonoBehaviour
{
    public GameObject shot;
    public Transform muzzle;
    public Transform shots;
    public Text label;
    public float turnSpeed = 90f;
    public float shotSpeed = 6f;
    // A C# name that is a Java keyword: the scene's value still reaches it.
    public int @class = 1;
    public int[] bonus;
    public List<string> words;
    public Color[] tints;
    [SerializeField] private int startAngle;

    private float angle;
    private int fired;
    private int steps;

    private void OnEnable()
    {
        Debug.Log("player enabled");
    }

    private void Start()
    {
        angle = startAngle;
        Debug.Log("player start bonus=" + bonus.Length + ":" + bonus[1] + " class=" + @class + " words=" + words.Count + ":" + words[1]
            + " tints=" + tints.Length + " shot=" + shot.name + " label=" + label.text + " muzzle at "
            + Cm(muzzle.position.x) + "," + Cm(muzzle.position.y));
        Probe.Check();
        // Approximately is relative: beside zero it allows a few of the
        // smallest floats and no more. And an angle keeps the side of the
        // axis a negative zero says it is on.
        Debug.Log("mathf approximately " + Mathf.Approximately(0f, 0.000001f) + Mathf.Approximately(0f, 0f)
            + Mathf.Approximately(0f, Mathf.Epsilon) + Mathf.Approximately(1f, 1.0000001f)
            + Mathf.Approximately(1000f, 1000.0001f) + Mathf.Approximately(1f, 1.00001f)
            + " atan2 " + Cm(Mathf.Atan2(-0f, -1f)) + "," + Cm(Mathf.Atan2(0f, -1f)) + "," + Cm(Mathf.Atan2(0f, -0f)));
        Probe.Poses();
        StartCoroutine(AfterStep("start"));
        Invoke("Ready", 0.25f);
    }

    // WaitForFixedUpdate is the end of the NEXT physics step. Asked for from
    // FixedUpdate that is the step after the one running, and never the same
    // one twice; asked for from Start it is the first step there is.
    private void FixedUpdate()
    {
        steps++;
        if (steps == 1)
        {
            StartCoroutine(AfterStep("fixed"));
        }
    }

    private IEnumerator AfterStep(string from)
    {
        int asked = steps;
        yield return new WaitForFixedUpdate();
        int first = steps;
        yield return new WaitForFixedUpdate();
        Debug.Log("after step from " + from + " asked in " + asked + " resumed in " + first + " then " + steps);
    }

    private void Ready()
    {
        Debug.Log("player ready, frame " + Time.frameCount);
    }

    private void Update()
    {
        angle -= Input.GetAxisRaw("Horizontal") * turnSpeed * Time.deltaTime;
        transform.rotation = Quaternion.Euler(0f, 0f, angle);
        if (Input.GetKeyDown(KeyCode.Space))
        {
            Fire();
        }
        // GetAxis follows a key at the axis's own sensitivity, falls back at
        // its own gravity and starts a reversal from zero only if it snaps:
        // Glide is fast up, slow down and does not snap, Snappy the other way
        // round, and Plain says nothing and has the 3, 3 and snap of the
        // default Horizontal axis.
        int f = Time.frameCount;
        if (f == 201 || f == 214 || f == 215 || f == 216 || f == 231 || f == 240)
        {
            Debug.Log("axes frame " + f + " glide=" + Mathf.RoundToInt(Input.GetAxis("Glide") * 1000f) + " snappy="
                + Mathf.RoundToInt(Input.GetAxis("Snappy") * 1000f) + " plain="
                + Mathf.RoundToInt(Input.GetAxis("Plain") * 1000f) + " raw=" + Input.GetAxisRaw("Glide"));
            // Plain's keys, inverted: e counts negative and q positive.
            Debug.Log("inverted frame " + f + " flipped=" + Mathf.RoundToInt(Input.GetAxis("Flipped") * 1000f)
                + " raw=" + Input.GetAxisRaw("Flipped"));
        }
        // Of two axes with one name, GetAxisRaw answers with the one of the
        // largest absolute value, whichever of them comes last.
        if (f == 206 || f == 236)
        {
            Debug.Log("mixed frame " + f + " raw=" + Mathf.RoundToInt(Input.GetAxisRaw("Mixed") * 1000f));
        }
    }

    private void Fire()
    {
        GameObject s = Instantiate(shot, muzzle.position, transform.rotation);
        s.GetComponent<Rigidbody2D>().velocity = transform.up * shotSpeed;
        s.transform.parent = shots;
        s.GetComponent<SpriteRenderer>().color = tints[fired % tints.Length];
        fired++;
        Debug.Log("fired " + fired + " " + s.name + " from " + Cm(muzzle.position.x) + "," + Cm(muzzle.position.y)
            + " heading " + Cm(transform.up.x) + "," + Cm(transform.up.y) + " live=" + shots.childCount);
    }

    public static int Cm(float value)
    {
        return Mathf.RoundToInt(value * 100f);
    }
}
