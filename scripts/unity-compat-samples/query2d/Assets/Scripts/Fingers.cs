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

// Logs every finger of every frame that has one, and what the mouse emulation
// makes of the first: ids, phases, positions, movement and tap counts.
public class Fingers : MonoBehaviour
{
    private int frame;

    private static string Phase(TouchPhase phase)
    {
        switch (phase)
        {
            case TouchPhase.Began: return "began";
            case TouchPhase.Moved: return "moved";
            case TouchPhase.Stationary: return "stationary";
            case TouchPhase.Ended: return "ended";
            default: return "canceled";
        }
    }

    private void Start()
    {
        Debug.Log("touch supported=" + Input.touchSupported + " multi=" + Input.multiTouchEnabled
            + " simulate=" + Input.simulateMouseWithTouches + " count=" + Input.touchCount);
    }

    private void Update()
    {
        frame++;
        if (frame == 100)
        {
            Input.simulateMouseWithTouches = false;
            Debug.Log("mouse emulation off");
        }
        if (frame == 120)
        {
            Input.multiTouchEnabled = false;
            Debug.Log("multi touch off");
        }
        int count = Input.touchCount;
        bool mouse = Input.GetMouseButton(0) || Input.GetMouseButtonDown(0) || Input.GetMouseButtonUp(0);
        if (count == 0 && !mouse)
        {
            return;
        }
        string s = "frame " + frame + " touches=" + count;
        for (int i = 0; i < count; i++)
        {
            Touch t = Input.GetTouch(i);
            s += " [" + t.fingerId + " " + Phase(t.phase) + " " + Mathf.RoundToInt(t.position.x) + ","
                + Mathf.RoundToInt(t.position.y) + " d=" + Mathf.RoundToInt(t.deltaPosition.x) + ","
                + Mathf.RoundToInt(t.deltaPosition.y) + " taps=" + t.tapCount + "]";
        }
        Touch[] touches = Input.touches;
        s += " array=" + touches.Length;
        s += " mouse=" + (Input.GetMouseButtonDown(0) ? "down" : Input.GetMouseButtonUp(0) ? "up"
            : Input.GetMouseButton(0) ? "held" : "none")
            + " at " + Mathf.RoundToInt(Input.mousePosition.x) + "," + Mathf.RoundToInt(Input.mousePosition.y)
            + " axis=" + Mathf.RoundToInt(Input.GetAxis("Mouse X") * 100f) + "," + Mathf.RoundToInt(Input.GetAxis("Mouse Y") * 100f);
        Debug.Log(s);
    }
}
