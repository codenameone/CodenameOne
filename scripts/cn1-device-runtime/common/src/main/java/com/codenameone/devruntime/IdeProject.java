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
package com.codenameone.devruntime;

import com.codename1.io.Storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

/// A project written on the device: source files by path, saved whole.
///
/// One `Storage` entry per project holds every file. A project is a handful of
/// small text files, so reading and writing it as one unit is cheaper than a
/// file per source and cannot leave a project half saved.
public final class IdeProject {
    private static final String PREFIX = "cn1ide-project-";
    static final String DEFAULT_NAME = "Hello";

    private final String name;
    private final Hashtable<String, String> files;

    private IdeProject(String name, Hashtable<String, String> files) {
        this.name = name;
        this.files = files;
    }

    public String getName() {
        return name;
    }

    /// Paths of the project's files, sorted, e.g. `com/example/Main.java`.
    public List<String> getPaths() {
        List<String> out = new ArrayList<String>(files.keySet());
        Collections.sort(out);
        return out;
    }

    public String getText(String path) {
        String t = files.get(path);
        return t == null ? "" : t;
    }

    public void setText(String path, String text) {
        files.put(path, text);
    }

    public void remove(String path) {
        files.remove(path);
    }

    public boolean contains(String path) {
        return files.containsKey(path);
    }

    /// Writes the project. Returns false when storage refused it (a full
    /// disk, typically), so the caller can tell the user rather than let them
    /// believe their edits are safe.
    public boolean save() {
        return Storage.getInstance().writeObject(PREFIX + name, files);
    }

    /// A new project holding exactly these files, saved at once. Used to turn a
    /// program received from a computer into an editable project, so what ran
    /// on the device can be changed and run again here.
    public static IdeProject create(String name, java.util.Map<String, String> sources) {
        Hashtable<String, String> files = new Hashtable<String, String>(sources);
        IdeProject p = new IdeProject(name, files);
        p.save();
        return p;
    }

    /// Names of every saved project, sorted.
    public static List<String> listNames() {
        List<String> out = new ArrayList<String>();
        String[] entries = Storage.getInstance().listEntries();
        if (entries != null) {
            for (String e : entries) {
                if (e.startsWith(PREFIX)) {
                    out.add(e.substring(PREFIX.length()));
                }
            }
        }
        Collections.sort(out);
        return out;
    }

    /// Opens a saved project, or creates it from the starter template.
    public static IdeProject open(String name) {
        Object o = Storage.getInstance().readObject(PREFIX + name);
        Hashtable<String, String> files = new Hashtable<String, String>();
        if (o instanceof Hashtable) {
            Hashtable<?, ?> stored = (Hashtable<?, ?>) o;
            for (Object k : stored.keySet()) {
                Object v = stored.get(k);
                if (k instanceof String && v instanceof String) {
                    files.put((String) k, (String) v);
                }
            }
        }
        IdeProject p = new IdeProject(name, files);
        // Seeded only when there was nothing stored at all. An entry that
        // exists but did not read back (a damaged write) is left alone: seeding
        // over it and saving would replace the user's work with the template.
        if (files.isEmpty() && !Storage.getInstance().exists(PREFIX + name)) {
            p.setText("com/example/" + javaName(name) + ".java", starter(javaName(name)));
            p.save();
        }
        return p;
    }

    public static void delete(String name) {
        Storage.getInstance().deleteStorageFile(PREFIX + name);
    }

    /// A Java identifier made of a project name: letters and digits only.
    static String javaName(String name) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_'
                    || b.length() > 0 && c >= '0' && c <= '9';
            if (ok) {
                b.append(b.length() == 0 && c >= 'a' && c <= 'z' ? (char) (c - 'a' + 'A') : c);
            }
        }
        return b.length() == 0 ? "Main" : b.toString();
    }

    static String starter(String cls) {
        return "package com.example;\n"
                + "\n"
                + "import com.codename1.system.Lifecycle;\n"
                + "import com.codename1.ui.Button;\n"
                + "import com.codename1.ui.Form;\n"
                + "import com.codename1.ui.Label;\n"
                + "import com.codename1.ui.layouts.BoxLayout;\n"
                + "\n"
                + "public class " + cls + " extends Lifecycle {\n"
                + "    private int taps;\n"
                + "\n"
                + "    @Override\n"
                + "    public void runApp() {\n"
                + "        Form hi = new Form(\"Hi World\", BoxLayout.y());\n"
                + "        Label count = new Label(\"Tapped 0 times\");\n"
                + "        Button tap = new Button(\"Tap me\");\n"
                + "        tap.addActionListener(e -> {\n"
                + "            taps++;\n"
                + "            count.setText(\"Tapped \" + taps + \" times\");\n"
                + "            hi.revalidate();\n"
                + "        });\n"
                + "        hi.add(new Label(\"Hello from the device\"));\n"
                + "        hi.add(tap);\n"
                + "        hi.add(count);\n"
                + "        hi.show();\n"
                + "    }\n"
                + "}\n";
    }
}
