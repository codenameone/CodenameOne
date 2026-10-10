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
using UnityEngine.UI;

// Counts hits into a UI text, swaps its sprite for another of the sheet and
// switches itself off after the last one.
public class Target : MonoBehaviour
{
    public Text label;
    public Sprite hurt;
    public int lives = 2;

    private int hits;

    public void Hit()
    {
        hits++;
        label.text = "Hits: " + hits;
        GetComponent<SpriteRenderer>().sprite = hurt;
        Debug.Log("target hit " + hits + " of " + lives);
        if (hits >= lives)
        {
            gameObject.SetActive(false);
        }
    }

    private void OnTriggerEnter2D(Collider2D other)
    {
        Debug.Log("target entered by " + other.name);
    }

    private void OnDisable()
    {
        Debug.Log("target disabled");
    }
}
