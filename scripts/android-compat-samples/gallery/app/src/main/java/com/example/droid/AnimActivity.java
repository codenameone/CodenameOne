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

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

public class AnimActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_anim);
        TextView out = findViewById(R.id.xml_out);
        StringBuilder sb = new StringBuilder();
        try {
            XmlResourceParser x = getResources().getXml(R.xml.fruits);
            int ev;
            while ((ev = x.next()) != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && "fruit".equals(x.getName())) {
                    sb.append(x.getAttributeValue(null, "name")).append('=')
                            .append(x.getAttributeValue(null, "color")).append(' ');
                }
            }
            x.close();
        } catch (Exception e) {
            sb.append("xml error: ").append(e);
        }
        out.setText(sb.toString().trim());
        findViewById(R.id.card).startAnimation(AnimationUtils.loadAnimation(this, R.anim.pop));
        final View spin = findViewById(R.id.spin);
        spin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ObjectAnimator.ofFloat(spin, "rotation", 0f, 360f).setDuration(2000).start();
            }
        });
        ObjectAnimator c = ObjectAnimator.ofArgb(findViewById(R.id.pulse), "textColor", 0xff000000, 0xffff4081);
        c.setDuration(800);
        c.setRepeatCount(ValueAnimator.INFINITE);
        c.setRepeatMode(ValueAnimator.REVERSE);
        c.start();
    }
}
