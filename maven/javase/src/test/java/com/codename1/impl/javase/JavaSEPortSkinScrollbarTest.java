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
 * Please contact Codename One through http://www.codenameone.com/ if
 * you need additional information or have any questions.
 */
package com.codename1.impl.javase;

import com.codename1.testing.junit.CodenameOneTest;
import com.codename1.ui.Form;

import org.junit.jupiter.api.Test;

import java.awt.Adjustable;
import java.awt.event.AdjustmentEvent;

import javax.swing.JScrollBar;

import static org.junit.jupiter.api.Assertions.assertTrue;

@CodenameOneTest
public class JavaSEPortSkinScrollbarTest {

    @Test
    public void simulatorScrollbarAdjustmentRequestsCodenameOneRepaint() {
        JavaSEPort port = JavaSEPort.instance;
        RepaintCountingForm form = new RepaintCountingForm();
        port.setCurrentForm(form);
        form.repaintCount = 0;

        port.canvas.setSize(200, 200);
        JScrollBar scrollbar = new JScrollBar(Adjustable.VERTICAL);
        scrollbar.setValues(50, 1, 0, 100);

        port.canvas.adjustmentValueChanged(new AdjustmentEvent(scrollbar,
                AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
                AdjustmentEvent.TRACK,
                scrollbar.getValue()));

        assertTrue(form.repaintCount > 0,
                "Changing the simulator scrollbar offset must request a CN1 repaint; "
                        + "otherwise Swing can redraw a stale buffer at a new offset and "
                        + "subsequent clicks are routed against a different component tree "
                        + "than the pixels visible on screen.");
    }

    private static class RepaintCountingForm extends Form {
        int repaintCount;

        @Override
        public void repaint() {
            repaintCount++;
            super.repaint();
        }
    }
}
