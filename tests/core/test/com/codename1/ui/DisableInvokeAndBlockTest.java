/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui;

import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;

/**
 * Tests for {@link Display#invokeWithoutBlocking(java.lang.Runnable) }
 * @author shannah
 */
public class DisableInvokeAndBlockTest extends AbstractTest {

    Throwable ex = null;

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        ex = null;
        try {
            Display.getInstance().invokeWithoutBlocking(() -> {
                Display.getInstance().invokeAndBlock(() -> {
                    Util.sleep(5);
                });
            });
        } catch (BlockingDisallowedException e) {
            ex = e;
        } catch (Throwable e) {
            ex = e;

        }
        assertNotNull(ex, "Should have thrown BlockingDisallowedException");
        assertTrue(ex instanceof BlockingDisallowedException, "invokeWithoutBlocking threw exception, but not the expected type.  Expected BlockingDisallowedException, but found " + ex.getClass().getName());

        ex = null;
        try {
            Display.getInstance().invokeAndBlock(() -> {
                Util.sleep(5);
            });

        } catch (BlockingDisallowedException e) {
            ex = e;
        }
        assertNull(ex, "Should not have thrown a BlockingDisallowedException");
        Label l = new Label("Foo");
        try {

            Display.getInstance().invokeWithoutBlocking(() -> {
                l.setText("Bar");

            });
        } catch (BlockingDisallowedException e) {
            ex = e;
        }
        assertEqual("Bar", l.getText(), "invbokeWithoutBlocking didn't seem to run");
        assertNull(ex, "Should not have thrown BlockingDisallowedException");

        return true;
    }
}
