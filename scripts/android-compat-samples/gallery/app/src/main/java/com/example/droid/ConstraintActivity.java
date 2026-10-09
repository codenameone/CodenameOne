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
package com.example.droid;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

public class ConstraintActivity extends Activity {
    private boolean moved;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_constraint);
        final ConstraintLayout root = (ConstraintLayout) findViewById(R.id.avatar).getParent();
        final TextView badge = findViewById(R.id.badge);
        findViewById(R.id.accept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Moves the badge to the bottom-right corner of the banner and back.
                ConstraintSet set = new ConstraintSet();
                set.clone(root);
                set.clear(R.id.badge);
                set.constrainWidth(R.id.badge, ConstraintSet.WRAP_CONTENT);
                set.constrainHeight(R.id.badge, ConstraintSet.WRAP_CONTENT);
                if (moved) {
                    set.connect(R.id.badge, ConstraintSet.START, R.id.text_end, ConstraintSet.END, 0);
                    set.connect(R.id.badge, ConstraintSet.TOP, R.id.name, ConstraintSet.TOP, 0);
                } else {
                    set.connect(R.id.badge, ConstraintSet.END, R.id.banner, ConstraintSet.END, 0);
                    set.connect(R.id.badge, ConstraintSet.BOTTOM, R.id.banner, ConstraintSet.BOTTOM, 0);
                }
                set.applyTo(root);
                moved = !moved;
                badge.setText(moved ? "MOVED" : "PRO");
            }
        });
    }
}
