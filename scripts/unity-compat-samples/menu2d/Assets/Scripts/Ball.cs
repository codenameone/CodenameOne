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
using UnityEngine;

// Falls onto the floor and bounces on a physics material, sounding a clip
// and raising an event with a tuple payload each time it lands.
[RequireComponent(typeof(AudioSource))]
public class Ball : MonoBehaviour
{
    public AudioClip beep;
    public Menu menu;

    public event EventHandler<(Menu.Level level, int count)> Bounced;

    private AudioSource source;
    private int count;
    private int soughtAt;

    private void Awake()
    {
        source = GetComponent<AudioSource>();
    }

    private void OnCollisionEnter2D(Collision2D collision)
    {
        count++;
        source.PlayOneShot(beep);
        Bounced?.Invoke(this, (menu.level, count));
        Debug.Log("bounce " + count + " on " + collision.gameObject.name + " at y=" + Menu.Cm(transform.position.y)
            + " frame " + Time.frameCount);
        if (count == 1)
        {
            Seek();
            Refused();
        }
    }

    // The source's own voice, at half speed and moved to a place in the clip:
    // `time` is that place whatever the pitch, and it goes on from there at
    // the pitch. Update reads it again a few frames on.
    private void Seek()
    {
        source.clip = beep;
        source.pitch = 0.5f;
        source.Play();
        source.time = 0.04f;
        soughtAt = Time.frameCount;
        Debug.Log("sought to " + Mathf.RoundToInt(source.time * 1000f) + "ms of "
            + Mathf.RoundToInt(beep.length * 1000f) + " playing=" + source.isPlaying);
    }

    // What the engine refuses, a script can catch: an axis that is not set
    // up is an ArgumentException in Unity, and anything is an Exception.
    private void Refused()
    {
        try
        {
            Debug.Log("never " + Input.GetAxis("Nope"));
        }
        catch (ArgumentException e)
        {
            Debug.Log("caught: " + e.Message);
        }
        try
        {
            Debug.Log("never " + transform.GetChild(3).name);
        }
        catch (Exception e)
        {
            Debug.Log("caught a child out of range=" + (e is IndexOutOfRangeException));
        }
    }

    private void Update()
    {
        int since = soughtAt == 0 ? 0 : Time.frameCount - soughtAt;
        if (since == 3 || since == 9)
        {
            Debug.Log("voice " + since + " frames on: " + Mathf.RoundToInt(source.time * 1000f) + "ms playing="
                + source.isPlaying);
        }
    }
}
