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

// Puts an Animator where a screenshot wants it. `speed` is given to the
// controller in the frame `runFrom`, which takes it from Idle to Run, and
// the `hit` trigger is set in the frame `hitAt`, which takes it to Hurt from
// wherever it is. Both by the frame count and never by the clock, so the
// clip and the place in it are the same on every run.
public class Actor : MonoBehaviour
{
    public int runFrom = -1;
    public int hitAt = -1;

    private Animator animator;
    private int steps;

    private void Awake()
    {
        animator = GetComponent<Animator>();
    }

    private void Update()
    {
        if (Time.frameCount == runFrom)
        {
            animator.SetFloat("speed", 1f);
        }
        if (Time.frameCount == hitAt)
        {
            animator.SetTrigger("hit");
        }
    }

    // The Run clip's animation event.
    public void Step()
    {
        steps++;
    }
}
