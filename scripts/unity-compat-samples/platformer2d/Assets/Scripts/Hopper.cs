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
using UnityEngine;

// Thrown straight up at the start. One of them meets a one-way platform from
// below, passes through it and comes to rest on top; the other meets a tile
// that is solid only in its top quarter, and is turned back by that quarter
// and not by the cell's lower edge.
public class Hopper : MonoBehaviour
{
    public float launch = 9f;

    private int highest = -100000;
    private bool reported;

    private void Start()
    {
        GetComponent<Rigidbody2D>().velocity = new Vector2(0f, launch);
    }

    private void FixedUpdate()
    {
        int y = Mathf.RoundToInt(transform.position.y * 100f);
        if (y > highest)
        {
            highest = y;
        }
        else if (!reported && y < highest - 5)
        {
            reported = true;
            Debug.Log(name + " turned back at " + highest);
        }
    }

    private void OnCollisionEnter2D(Collision2D collision)
    {
        Debug.Log("frame " + Time.frameCount + " " + name + " met " + collision.gameObject.name + " at "
            + Mathf.RoundToInt(transform.position.y * 100f));
    }
}
