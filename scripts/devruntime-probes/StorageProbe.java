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
import com.codename1.io.*;
import java.util.*;
public class StorageProbe {
    public static void main(String[] a) {
        StringBuilder r = new StringBuilder();
        Storage.getInstance().writeObject("probe-key", "stored-value");
        r.append("storage=").append(Storage.getInstance().readObject("probe-key"));
        Preferences.set("probe-pref", 99);
        r.append(" pref=").append(Preferences.get("probe-pref", 0));
        java.util.List<String> l = new ArrayList<String>();
        l.add("a"); l.add("b");
        Storage.getInstance().writeObject("probe-list", l);
        r.append(" list=").append(Storage.getInstance().readObject("probe-list"));
        System.out.println("PROBE StorageProbe: " + r);
        new Form("Storage").show();
    }
}
