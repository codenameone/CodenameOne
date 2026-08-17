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
import com.codename1.ui.*;

/**
 * A callback that arrives well after the program started, doing enough work to
 * reach a watchdog checkpoint.
 *
 * This is what every button press in a real application looks like: the program
 * ran once, and the framework calls back into it minutes later. The EDT budget
 * must apply to that one callback, not to the age of the session.
 */
public class LateCallbackProbe {
    public static void main(String[] a) {
        new Form("LateCallback").show();
        System.out.println("PROBE LateCallbackProbe: started, callback scheduled");
        new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(5000); } catch (InterruptedException e) { }
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        long n = 0;
                        for (int i = 0; i < 3000000; i++) { n += i; }
                        System.out.println("PROBE LateCallbackProbe: late callback ran, n=" + n);
                    }
                });
            }
        }).start();
    }
}
