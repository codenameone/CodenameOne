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
package com.codenameone.developerguide.androidinterop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// An Android view placed in a Codename One form.
public class EmbedAndroidView {

    /// A custom Android view, as an application being ported would have.
    static class MyChartView extends android.view.View {
        private final Paint paint = new Paint();

        MyChartView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 4f, paint);
        }
    }

    public void embed(Context androidContext, Form form) {
        // tag::androidInteropEmbedView[]
        android.view.View chart = new MyChartView(androidContext);
        form.add(BorderLayout.CENTER, chart.getPeer());
        // end::androidInteropEmbedView[]
    }
}
