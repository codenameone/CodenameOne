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

// A trigger on a moving body: tells a target it was hit and destroys
// itself, or expires.
public class Shot : MonoBehaviour
{
    public float life = 2f;

    private float age;

    private void Update()
    {
        age += Time.deltaTime;
        if (age > life)
        {
            Debug.Log("shot expired at " + Player.Cm(transform.position.x) + "," + Player.Cm(transform.position.y));
            Destroy(gameObject);
        }
    }

    private void OnTriggerEnter2D(Collider2D other)
    {
        Debug.Log("shot entered " + other.name + " tag=" + other.tag);
        if (!other.CompareTag("Target"))
        {
            return;
        }
        other.GetComponent<Target>().Hit();
        Destroy(gameObject);
    }
}
