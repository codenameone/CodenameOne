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
package com.codename1.printing;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the static {@link Printer} facade against the non-printing test
 * platform: support probing, and the {@code print} / {@code printPDF}
 * delegation that reports {@link PrintResult#STATUS_FAILED} through the
 * listener (delivered on the EDT) plus the null-listener tolerance.
 */
class PrinterTest extends UITestBase {

    @Test
    void printingIsNotSupportedOnTheTestPlatform() {
        assertFalse(Printer.isPrintingSupported());
    }

    @Test
    void printReportsFailureWhenUnsupported() {
        PrintResult r = awaitPrint(null, "application/pdf");
        assertNotNull(r);
        assertTrue(r.isFailed());
        assertNotNull(r.getError());
    }

    @Test
    void printPdfDelegatesAndReportsFailure() {
        final AtomicReference<PrintResult> holder = new AtomicReference<PrintResult>();
        final CountDownLatch latch = new CountDownLatch(1);
        Printer.printPDF("file://doc.pdf", new PrintResultListener() {
            public void onResult(PrintResult result) {
                holder.set(result);
                latch.countDown();
            }
        });
        waitFor(latch, 2000);
        assertNotNull(holder.get());
        assertTrue(holder.get().isFailed());
    }

    @Test
    void printToleratesNullListener() {
        // Must not throw even though there is no listener to deliver to.
        Printer.print("file://doc.pdf", "application/pdf", null);
        flushSerialCalls();
    }

    private PrintResult awaitPrint(String unusedPath, String mimeType) {
        final AtomicReference<PrintResult> holder = new AtomicReference<PrintResult>();
        final CountDownLatch latch = new CountDownLatch(1);
        Printer.print("file://doc", mimeType, new PrintResultListener() {
            public void onResult(PrintResult result) {
                holder.set(result);
                latch.countDown();
            }
        });
        waitFor(latch, 2000);
        return holder.get();
    }
}
