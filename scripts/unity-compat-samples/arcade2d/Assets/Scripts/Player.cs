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
    public int[] bonus;
    public List<string> words;
    public Color[] tints;
    [SerializeField] private int startAngle;

    private float angle;
    private int fired;

    private void OnEnable()
    {
        Debug.Log("player enabled");
    }

    private void Start()
    {
        angle = startAngle;
        Debug.Log("player start bonus=" + bonus.Length + ":" + bonus[1] + " words=" + words.Count + ":" + words[1]
            + " tints=" + tints.Length + " shot=" + shot.name + " label=" + label.text + " muzzle at "
            + Cm(muzzle.position.x) + "," + Cm(muzzle.position.y));
        Invoke("Ready", 0.25f);
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
