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

// Turns its object, and half way through swaps the picture it shows. It
// logs nothing while the scene runs: what it did shows in the draw list,
// and Describe() reports the camera's view of it at the end, in integers.
public class Spinner : MonoBehaviour
{
    public float degreesPerSecond = 45f;
    public Sprite alternate;

    private SpriteRenderer picture;

    private void Awake()
    {
        picture = GetComponent<SpriteRenderer>();
    }

    private void Update()
    {
        transform.Rotate(0f, 0f, degreesPerSecond * Time.deltaTime);
        if (Time.frameCount == 120 && alternate != null)
        {
            picture.sprite = alternate;
            picture.flipX = !picture.flipX;
            picture.sortingOrder = 7;
        }
    }

    public string Describe()
    {
        Camera cam = Camera.main;
        Vector3 on = cam.WorldToScreenPoint(transform.position);
        Vector3 corner = cam.ScreenToWorldPoint(new Vector3(0f, 0f, 10f));
        Vector2 pivot = picture.sprite.pivot;
        return gameObject.name + " angle=" + C(transform.eulerAngles.z) + " screen=" + Screen.width + "x"
            + Screen.height + " size=" + C(cam.orthographicSize) + " on screen at " + C(on.x) + "," + C(on.y) + ","
            + C(on.z) + " corner=" + C(corner.x) + "," + C(corner.y) + "," + C(corner.z) + " sprite="
            + picture.sprite.name + " ppu=" + C(picture.sprite.pixelsPerUnit) + " pivot=" + C(pivot.x) + ","
            + C(pivot.y) + " order=" + picture.sortingOrder + " main=" + cam.CompareTag("MainCamera");
    }

    private static int C(float value)
    {
        return Mathf.RoundToInt(value * 100f);
    }
}
