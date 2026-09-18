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
package com.codenameone.examples.hellocodenameone;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.impl.android.InPlaceEditView;
import com.codename1.impl.android.AndroidImplementation;
import com.codenameone.examples.hellocodenameone.tests.InPlaceEditViewTest;

public class InPlaceEditViewNativeImpl {
    public void runReproductionTest() {
        Display.getInstance().callSerially(() -> {
            try {
                java.lang.reflect.Method getImplMethod = Display.class.getDeclaredMethod("getImplementation");
                getImplMethod.setAccessible(true);
                final Object impl = getImplMethod.invoke(Display.getInstance());

                if (!(impl instanceof AndroidImplementation)) {
                    Display.getInstance().callSerially(() -> InPlaceEditViewTest.onError("Implementation is not AndroidImplementation: " + impl.getClass().getName()));
                    return;
                }
                final AndroidImplementation androidImpl = (AndroidImplementation) impl;

                Form f = new Form("Test NPE", new BoxLayout(BoxLayout.Y_AXIS));
                final TextArea ta = new TextField("Test");
                f.add(ta);
                f.show();
                f.revalidate();

                new Thread(() -> {
                    try {
                        for (int i = 0; i < 50; i++) {
                            // Start editing
                            callOnEdtAndWait(() -> InPlaceEditView.edit(androidImpl, ta, ta.getConstraint()));

                            // Schedule reLayoutEdit calls
                            for (int j = 0; j < 5; j++) {
                                callOnEdtAndWait(() -> InPlaceEditView.reLayoutEdit());
                                Thread.sleep(10);
                            }

                            // stopEdit removes Android views and requires Android's UI thread,
                            // not the CN1 EDT. The production wrapper performs that handoff
                            // and waits for teardown while queued relayouts can still race it.
                            callOnEdtAndWait(() -> AndroidImplementation.stopEditing());
                        }
                        Display.getInstance().callSerially(() -> InPlaceEditViewTest.onSuccess());
                    } catch (Throwable t) {
                        t.printStackTrace();
                        Display.getInstance().callSerially(() -> InPlaceEditViewTest.onError(t.toString()));
                    }
                }).start();

            } catch (Throwable t) {
                t.printStackTrace();
                Display.getInstance().callSerially(() -> InPlaceEditViewTest.onError(t.toString()));
            }
        });
    }

    private static void callOnEdtAndWait(Runnable action) throws Throwable {
        final java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        // callSeriallyAndWait does not propagate EDT exceptions to the worker.
        // Forward them so a failed iteration cannot become a successful test.
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    public boolean isSupported() {
        return true;
    }

}
