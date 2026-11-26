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
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.devruntime;

import com.codename1.tools.javac.Diagnostic;
import com.codename1.ui.Button;
import com.codename1.ui.CodeDiagnostic;
import com.codename1.ui.CodeEditor;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Sheet;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.List;

/// The on-device IDE: edit a project's sources, then compile and run them on
/// this device, with no computer involved.
///
/// Two layouts from one form. On a phone the editor owns the screen: files
/// are tabs under the toolbar, the project switcher is a sheet behind the
/// title, and the console is a panel that opens when there is something to
/// read. On a tablet the files and projects sit in a permanent sidebar and Run
/// is a labelled button, the way a desktop IDE is laid out.
///
/// Run compiles with the same compiler the Playground uses, packages the
/// classes into the bundle format a desktop push sends, and installs it
/// through the same path a push takes -- so a program behaves identically
/// whichever way it arrived. While it runs, a "Slow interpreted mode" label floats over its
/// screens ({@link IdeRunOverlay}) and brings the editor back.
public class IdeForm extends Form {
    static final int ACCENT = 0x6750A4;
    static final int TEXT = 0x1D1B20;
    static final int MUTED = 0x6B6573;
    static final int SIDEBAR = 0xF4F2F8;
    static final int SELECTED = 0xE6E0F4;
    static final int DIVIDER = 0xDDD8E6;
    static final int CONSOLE_BG = 0x1F1D24;
    static final int CONSOLE_HEADER = 0x2B2930;
    static final int CONSOLE_TEXT = 0xE6E1E5;
    static final int CONSOLE_ERROR = 0xF2B8B5;
    static final int CONSOLE_WARN = 0xF5D17A;
    static final int CONSOLE_OK = 0x9CD67F;

    private static IdeForm current;

    private final boolean tablet = Display.getInstance().isTablet();
    private IdeProject project;
    private String path;
    private final CodeEditor editor = new CodeEditor();
    private final Container tabs = new Container(BoxLayout.x());
    private final Container sidebar = new Container(BoxLayout.y());
    private final Label status = new Label();
    private final Button consoleToggle = new Button();
    private final Container consoleBody = new Container(BoxLayout.y()) {
        @Override
        protected Dimension calcPreferredSize() {
            // A third of the screen when open: enough for a stack of errors,
            // never so much that the line they point at is pushed off.
            Dimension d = super.calcPreferredSize();
            d.setHeight(Display.getInstance().getDisplayHeight() / (tablet ? 4 : 3));
            return d;
        }
    };
    private boolean busy;

    public IdeForm(IdeProject project) {
        super(project.getName(), new BorderLayout());
        this.project = project;
        getContentPane().getAllStyles().setBgColor(0xffffff);
        getContentPane().getAllStyles().setBgTransparency(255);

        editor.setLanguage("java");
        editor.setShowLineNumbers(true);
        editor.setCompletionProvider(new IdeCompletion());
        // The completion index reads every class name in a 5 MB library; do
        // it now, off the event thread, rather than on the first keystroke.
        new Thread(IdeCompletion::warmUp, "cn1-ide-index").start();

        buildToolbar();
        Container center = new Container(new BorderLayout());
        if (!tablet) {
            tabs.setScrollableX(true);
            pad(tabs, 1, 1, 2, 2);
            tabs.getAllStyles().setBorder(Border.createUnderlineBorder(1, DIVIDER));
            center.add(BorderLayout.NORTH, tabs);
        }
        center.add(BorderLayout.CENTER, editor);
        center.add(BorderLayout.SOUTH, buildConsole());
        add(BorderLayout.CENTER, center);
        if (tablet) {
            add(BorderLayout.WEST, buildSidebarShell());
        }

        List<String> paths = project.getPaths();
        open(paths.isEmpty() ? null : paths.get(0));
        setStatus("Ready", MUTED);
    }

    // ---- chrome ----

    private void buildToolbar() {
        Toolbar tb = getToolbar();
        // Connecting to a computer mirrors Run: the two ways to start a
        // program, at the two ends of the toolbar.
        if (tablet) {
            Command connect = new Command("Connect") {
                @Override
                public void actionPerformed(com.codename1.ui.events.ActionEvent ev) {
                    saveThen(DeviceRuntimeForm::showPushScreen);
                }
            };
            connect.setMaterialIcon(FontImage.MATERIAL_COMPUTER);
            tb.addCommandToLeftBar(connect);
        } else {
            tb.addMaterialCommandToLeftBar("", FontImage.MATERIAL_COMPUTER,
                    e -> saveThen(DeviceRuntimeForm::showPushScreen));
        }
        if (tablet) {
            Command run = new Command("Run") {
                @Override
                public void actionPerformed(com.codename1.ui.events.ActionEvent ev) {
                    run();
                }
            };
            run.setMaterialIcon(FontImage.MATERIAL_PLAY_ARROW);
            tb.addCommandToRightBar(run);
        } else {
            // The title is the project switcher: one tap away, no menu to hunt
            // through, and it says which project is open while it does.
            Button title = new Button(project.getName());
            title.setUIID("Title");
            FontImage.setMaterialIcon(title, FontImage.MATERIAL_EXPAND_MORE);
            title.setTextPosition(Component.LEFT);
            title.addActionListener(e -> saveThen(this::showProjectSheet));
            tb.setTitleComponent(title);
            tb.addMaterialCommandToRightBar("", FontImage.MATERIAL_PLAY_ARROW, e -> run());
        }
    }

    private Container buildConsole() {
        Container header = new Container(new BorderLayout());
        header.getAllStyles().setBgColor(CONSOLE_HEADER);
        header.getAllStyles().setBgTransparency(255);
        pad(header, 1, 1, 3, 1);
        status.getAllStyles().setFgColor(CONSOLE_TEXT);
        status.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        status.getAllStyles().setBgTransparency(0);
        consoleToggle.getAllStyles().setFgColor(CONSOLE_TEXT);
        consoleToggle.getAllStyles().setBgTransparency(0);
        consoleToggle.getAllStyles().setBorder(Border.createEmpty());
        pad(consoleToggle, 1, 1, 2, 2);
        consoleToggle.addActionListener(e -> setConsoleOpen(consoleBody.isHidden()));
        header.add(BorderLayout.CENTER, status).add(BorderLayout.EAST, consoleToggle);
        header.setLeadComponent(consoleToggle);

        consoleBody.setScrollableY(true);
        consoleBody.getAllStyles().setBgColor(CONSOLE_BG);
        consoleBody.getAllStyles().setBgTransparency(255);
        pad(consoleBody, 1, 2, 3, 3);

        Container panel = new Container(new BorderLayout());
        panel.getAllStyles().setBgColor(CONSOLE_BG);
        panel.getAllStyles().setBgTransparency(255);
        panel.add(BorderLayout.NORTH, header).add(BorderLayout.CENTER, consoleBody);
        panel.setSafeArea(true);
        setConsoleOpen(false);
        return panel;
    }

    private void setConsoleOpen(boolean open) {
        consoleBody.setHidden(!open);
        FontImage.setMaterialIcon(consoleToggle, open ? FontImage.MATERIAL_EXPAND_MORE : FontImage.MATERIAL_EXPAND_LESS);
        revalidate();
    }

    private void setStatus(String text, int dotColor) {
        status.setText(text);
        status.setIcon(FontImage.createMaterial(FontImage.MATERIAL_CIRCLE, statusIconStyle(dotColor), 2.2f));
        status.setGap(Display.getInstance().convertToPixels(1.5f));
    }

    private static Style statusIconStyle(int color) {
        Style s = new Style();
        s.setFgColor(color);
        s.setBgTransparency(0);
        return s;
    }

    private void clearConsole() {
        consoleBody.removeAll();
    }

    /// One line in the console; tapping it runs `onTap` when there is one.
    private void log(String text, int color, Runnable onTap) {
        TextArea line = new TextArea(text);
        line.setEditable(false);
        line.setFocusable(onTap != null);
        line.setGrowByContent(true);
        line.setRows(1);
        line.setColumns(10);
        line.setActAsLabel(true);
        Style s = line.getAllStyles();
        s.setFgColor(color);
        s.setBgTransparency(0);
        s.setBorder(Border.createEmpty());
        s.setFont(Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        s.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        s.setPadding(1, 1, 0, 0);
        s.setMargin(0, 0, 0, 0);
        if (onTap != null) {
            Button lead = new Button();
            lead.addActionListener(e -> onTap.run());
            Container row = new Container(new BorderLayout());
            row.add(BorderLayout.CENTER, line);
            row.setLeadComponent(lead);
            row.add(BorderLayout.EAST, lead);
            lead.setHidden(true);
            consoleBody.add(row);
        } else {
            consoleBody.add(line);
        }
    }

    // ---- files: tabs on a phone, the sidebar on a tablet ----

    private void refreshFiles() {
        if (tablet) {
            refreshSidebar();
            return;
        }
        tabs.removeAll();
        for (String p : project.getPaths()) {
            final String target = p;
            Button tab = chip(fileName(p), p.equals(path));
            tab.addActionListener(e -> {
                if (!target.equals(path)) {
                    saveThen(() -> open(target));
                }
            });
            tabs.add(tab);
        }
        Button add = chip("", false);
        FontImage.setMaterialIcon(add, FontImage.MATERIAL_ADD);
        add.addActionListener(e -> newFile());
        tabs.add(add);
        tabs.revalidate();
    }

    private static Button chip(String text, boolean selected) {
        Button b = new Button(text);
        Style s = b.getAllStyles();
        s.setFgColor(selected ? ACCENT : MUTED);
        s.setBgColor(selected ? SELECTED : 0xffffff);
        s.setBgTransparency(255);
        s.setFont(Font.createSystemFont(Font.FACE_SYSTEM, selected ? Font.STYLE_BOLD : Font.STYLE_PLAIN, Font.SIZE_SMALL));
        s.setBorder(Border.createRoundBorder(40, 40, selected ? SELECTED : DIVIDER, !selected));
        s.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        s.setPadding(1, 1, 3, 3);
        s.setMarginUnit(Style.UNIT_TYPE_DIPS);
        s.setMargin(0, 0, 0, 2);
        return b;
    }

    private Container buildSidebarShell() {
        Container shell = new Container(new BorderLayout()) {
            @Override
            protected Dimension calcPreferredSize() {
                Dimension d = super.calcPreferredSize();
                d.setWidth(Math.min(Display.getInstance().convertToPixels(55),
                        Display.getInstance().getDisplayWidth() / 3));
                return d;
            }
        };
        shell.getAllStyles().setBgColor(SIDEBAR);
        shell.getAllStyles().setBgTransparency(255);
        shell.getAllStyles().setBorder(Border.createCompoundBorder(null, null, null,
                Border.createLineBorder(1, DIVIDER)));
        sidebar.setScrollableY(true);
        pad(sidebar, 2, 2, 1, 1);
        Button about = row(FontImage.MATERIAL_INFO_OUTLINE, "About and help", false);
        about.addActionListener(e -> saveThen(() -> AboutForm.show(this)));
        Container bottom = new Container(BoxLayout.y());
        bottom.getAllStyles().setBorder(Border.createCompoundBorder(Border.createLineBorder(1, DIVIDER), null, null, null));
        pad(bottom, 1, 1, 1, 1);
        Button settings = row(FontImage.MATERIAL_SETTINGS, "Settings", false);
        settings.addActionListener(e -> saveThen(() -> DeviceRuntimeSettingsForm.show(this)));
        bottom.add(settings);
        bottom.add(about);
        bottom.setSafeArea(true);
        shell.add(BorderLayout.CENTER, sidebar).add(BorderLayout.SOUTH, bottom);
        return shell;
    }

    private void refreshSidebar() {
        sidebar.removeAll();
        sidebar.add(sectionHeader("FILES"));
        for (String p : project.getPaths()) {
            final String target = p;
            Button b = row(FontImage.MATERIAL_DESCRIPTION, fileName(p), p.equals(path));
            b.addActionListener(e -> {
                if (!target.equals(path)) {
                    saveThen(() -> open(target));
                }
            });
            sidebar.add(b);
        }
        Button add = row(FontImage.MATERIAL_ADD, "New file", false);
        add.getAllStyles().setFgColor(ACCENT);
        add.addActionListener(e -> newFile());
        sidebar.add(add);
        if (path != null && project.getPaths().size() > 1) {
            Button del = row(FontImage.MATERIAL_DELETE_OUTLINE, "Delete " + fileName(path), false);
            del.addActionListener(e -> deleteCurrentFile());
            sidebar.add(del);
        }

        sidebar.add(sectionHeader("PROJECTS"));
        for (String name : IdeProject.listNames()) {
            final String target = name;
            Button b = row(FontImage.MATERIAL_FOLDER, name, name.equals(project.getName()));
            b.addActionListener(e -> {
                if (!target.equals(project.getName())) {
                    saveThen(() -> showProject(IdeProject.open(target)));
                }
            });
            sidebar.add(b);
        }
        Button addProject = row(FontImage.MATERIAL_CREATE_NEW_FOLDER, "New project", false);
        addProject.getAllStyles().setFgColor(ACCENT);
        addProject.addActionListener(e -> newProject());
        sidebar.add(addProject);
        sidebar.revalidate();
    }

    private static Label sectionHeader(String text) {
        Label l = new Label(text);
        l.getAllStyles().setFgColor(MUTED);
        l.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        l.getAllStyles().setBgTransparency(0);
        pad(l, 3, 1, 3, 2);
        return l;
    }

    /// A list row for the sidebar and the sheets: icon, text, highlighted when
    /// it is the open item.
    static Button row(char icon, String text, boolean selected) {
        Button b = new Button(text);
        Style s = b.getAllStyles();
        s.setFgColor(selected ? ACCENT : TEXT);
        s.setBgColor(selected ? SELECTED : 0xffffff);
        s.setBgTransparency(selected ? 255 : 0);
        s.setAlignment(Component.LEFT);
        s.setFont(Font.createSystemFont(Font.FACE_SYSTEM, selected ? Font.STYLE_BOLD : Font.STYLE_PLAIN, Font.SIZE_MEDIUM));
        s.setBorder(selected ? Border.createRoundBorder(16, 16, SELECTED, false) : Border.createEmpty());
        s.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        s.setPadding(2, 2, 3, 3);
        s.setMarginUnit(Style.UNIT_TYPE_DIPS);
        s.setMargin(0, 0, 0, 0);
        FontImage.setMaterialIcon(b, icon, 3.2f);
        b.setGap(Display.getInstance().convertToPixels(2.5f));
        return b;
    }

    private static void pad(Component c, int top, int bottom, int left, int right) {
        c.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        c.getAllStyles().setPadding(top, bottom, left, right);
    }

    static String fileName(String p) {
        return p.substring(p.lastIndexOf('/') + 1);
    }

    // ---- sheets (phone) ----

    private void showProjectSheet() {
        final Sheet sheet = new Sheet(null, "Projects");
        Container c = sheet.getContentPane();
        c.setLayout(BoxLayout.y());
        c.setScrollableY(true);
        for (String name : IdeProject.listNames()) {
            final String target = name;
            Button b = row(FontImage.MATERIAL_FOLDER, name, name.equals(project.getName()));
            b.addActionListener(e -> {
                sheet.back();
                if (!target.equals(project.getName())) {
                    // Saved first, as the sidebar does: switching used to drop
                    // every edit since the last Run.
                    saveThen(() -> showProject(IdeProject.open(target)));
                }
            });
            c.add(b);
        }
        Button create = row(FontImage.MATERIAL_CREATE_NEW_FOLDER, "New project", false);
        create.getAllStyles().setFgColor(ACCENT);
        create.addActionListener(e -> {
            sheet.back();
            newProject();
        });
        c.add(create);
        if (path != null && project.getPaths().size() > 1) {
            Button del = row(FontImage.MATERIAL_DELETE_OUTLINE, "Delete " + fileName(path), false);
            del.addActionListener(e -> {
                sheet.back();
                deleteCurrentFile();
            });
            c.add(del);
        }
        Button about = row(FontImage.MATERIAL_INFO_OUTLINE, "About and help", false);
        about.addActionListener(e -> {
            sheet.back();
            saveThen(() -> AboutForm.show(this));
        });
        Button settings = row(FontImage.MATERIAL_SETTINGS, "Settings", false);
        settings.addActionListener(e -> {
            sheet.back();
            saveThen(() -> DeviceRuntimeSettingsForm.show(this));
        });
        c.add(settings);
        c.add(about);
        sheet.show();
    }

    /// Asks for one name in a sheet: a field, a Create button, and the
    /// keyboard already up.
    private void askName(String title, String hint, String initial, final SuccessCallback<String> done) {
        final Sheet sheet = new Sheet(null, title);
        Container c = sheet.getContentPane();
        c.setLayout(BoxLayout.y());
        pad(c, 1, 3, 3, 3);
        final TextField field = new TextField(initial, hint, 20, TextField.ANY);
        Label help = new Label(hint);
        help.getAllStyles().setFgColor(MUTED);
        help.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        Button create = new Button("Create");
        Style s = create.getAllStyles();
        s.setFgColor(0xffffff);
        s.setBgColor(ACCENT);
        s.setBgTransparency(255);
        s.setBorder(Border.createRoundBorder(40, 40, ACCENT, false));
        s.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        s.setPadding(2, 2, 4, 4);
        s.setMarginUnit(Style.UNIT_TYPE_DIPS);
        s.setMargin(3, 1, 0, 0);
        create.addActionListener(e -> {
            String v = field.getText().trim();
            sheet.back();
            if (v.length() > 0) {
                done.onSucess(v);
            }
        });
        field.setDoneListener(e -> create.pressed());
        c.add(field).add(help).add(FlowLayout.encloseRight(create));
        sheet.show();
        field.startEditingAsync();
    }

    // ---- actions ----

    private void open(String p) {
        path = p;
        editor.setText(p == null ? "" : project.getText(p));
        editor.setDiagnostics(null);
        refreshFiles();
        revalidate();
    }

    /// Reads the editor back into the project -- the editor answers
    /// asynchronously -- saves, and then continues on the event thread.
    private void saveThen(final Runnable next) {
        if (path == null) {
            next.run();
            return;
        }
        final String p = path;
        editor.getText(text -> {
            project.setText(p, text);
            project.save();
            next.run();
        });
    }

    private void newFile() {
        String pkg = "com/example/";
        if (path != null && path.lastIndexOf('/') > 0) {
            pkg = path.substring(0, path.lastIndexOf('/') + 1);
        }
        final String dir = pkg;
        askName("New file", "A class name, or a path such as " + dir + "Helper", "", name -> {
            String p = name.trim();
            while (p.startsWith("/")) {
                p = p.substring(1);
            }
            p = p.indexOf('/') >= 0 ? p : dir + p;
            if (!p.endsWith(".java")) {
                p = p + ".java";
            }
            if (!isJavaPath(p)) {
                // skeleton() would write "public class 1abc", and the project
                // would then fail to build with an error about a file the user
                // just made.
                Dialog.show("New file", "\"" + name + "\" is not a Java class name. Use letters "
                        + "and digits, starting with a letter, such as Helper.", "OK", null);
                return;
            }
            if (project.contains(p)) {
                final String existing = p;
                saveThen(() -> open(existing));
                return;
            }
            final String created = p;
            saveThen(() -> {
                project.setText(created, skeleton(created));
                project.save();
                open(created);
            });
        });
    }

    /// Every segment a Java identifier that is not a keyword.
    static boolean isJavaPath(String p) {
        String body = p.substring(0, p.length() - ".java".length());
        if (body.length() == 0) {
            return false;
        }
        for (String seg : com.codename1.util.StringUtil.tokenize(body, '/')) {
            if (seg.length() == 0 || !Character.isJavaIdentifierStart(seg.charAt(0))
                    || KEYWORDS.contains(seg)) {
                return false;
            }
            for (int i = 1; i < seg.length(); i++) {
                if (!Character.isJavaIdentifierPart(seg.charAt(i))) {
                    return false;
                }
            }
        }
        return body.indexOf("//") < 0 && !body.endsWith("/");
    }

    private static final java.util.Set<String> KEYWORDS = new java.util.HashSet<String>(
            java.util.Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case",
                    "catch", "char", "class", "const", "continue", "default", "do", "double",
                    "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if",
                    "implements", "import", "instanceof", "int", "interface", "long", "native",
                    "new", "package", "private", "protected", "public", "return", "short",
                    "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
                    "throws", "transient", "try", "void", "volatile", "while", "true", "false",
                    "null", "var", "record"));

    private void deleteCurrentFile() {
        final String doomed = path;
        if (doomed == null) {
            return;
        }
        if (!Dialog.show("Delete " + fileName(doomed) + "?",
                "The file is removed from this project. This cannot be undone.",
                "Delete", "Cancel")) {
            return;
        }
        project.remove(doomed);
        project.save();
        List<String> left = project.getPaths();
        path = null;
        open(left.isEmpty() ? null : left.get(0));
    }

    private void newProject() {
        askName("New project", "Letters and digits, such as MyApp", "", name -> {
            final String n = IdeProject.javaName(name);
            if (IdeProject.listNames().contains(n)) {
                // "My App" and "MyApp" are one project once normalised; opening
                // the existing one here would look like a fresh project and
                // invite overwriting its files.
                Dialog.show("New project", "A project named " + n + " already exists.", "OK", null);
                return;
            }
            saveThen(() -> showProject(IdeProject.open(n)));
        });
    }

    static String skeleton(String path) {
        int slash = path.lastIndexOf('/');
        String cls = path.substring(slash + 1, path.length() - ".java".length());
        String pkg = slash < 0 ? "" : path.substring(0, slash).replace('/', '.');
        return (pkg.length() == 0 ? "" : "package " + pkg + ";\n\n")
                + "public class " + cls + " {\n}\n";
    }

    private void run() {
        if (busy) {
            return;
        }
        busy = true;
        clearConsole();
        setStatus("Compiling...", CONSOLE_WARN);
        revalidate();
        saveThen(() -> {
            final IdeProject p = project;
            // Off the event thread: compiling takes a moment, and the screen
            // should keep drawing while it does.
            new Thread(() -> {
                final IdeBuild.Result r = IdeBuild.build(p);
                Display.getInstance().callSerially(() -> built(r));
            }, "cn1-ide-compile").start();
        });
    }

    private void built(IdeBuild.Result r) {
        System.out.println("CN1SS:IDE build " + r.summary());
        int errors = showDiagnostics(r);
        if (!r.isSuccess()) {
            busy = false;
            setStatus(errors == 1 ? "Build failed: 1 error" : "Build failed: " + errors + " errors", CONSOLE_ERROR);
            setConsoleOpen(true);
            return;
        }
        setStatus("Running " + simpleName(r.getMainClass()), CONSOLE_OK);
        log("Starting " + r.getMainClass().replace('/', '.'), CONSOLE_TEXT, null);
        DeviceRuntimeService.getInstance().runLocalBundle(r.getBundle(),
                (result, error) -> {
                    busy = false;
                    System.out.println("CN1SS:IDE run " + (error == null ? "ok " + result : "error " + error));
                    if (error != null) {
                        setStatus("Stopped with an error", CONSOLE_ERROR);
                        log(error, CONSOLE_ERROR, null);
                        setConsoleOpen(true);
                        if (Display.getInstance().getCurrent() != IdeForm.this) {
                            show();
                        }
                    } else {
                        setStatus("Ready", MUTED);
                        IdeRunOverlay.install();
                    }
                });
    }

    private static String simpleName(String internal) {
        return internal.substring(internal.lastIndexOf('/') + 1);
    }

    /// Fills the console with the build's messages and marks this file's in
    /// the editor; answers the number of errors. A message in any file is a
    /// link to its line.
    private int showDiagnostics(IdeBuild.Result r) {
        List<CodeDiagnostic> here = new ArrayList<CodeDiagnostic>();
        int errors = 0;
        log(r.summary(), MUTED, null);
        for (final Diagnostic d : r.getDiagnostics()) {
            if (d.error) {
                errors++;
            }
            String where = d.file == null ? "" : fileName(d.file) + ":" + d.line + "  ";
            log(where + d.message, d.error ? CONSOLE_ERROR : CONSOLE_WARN,
                    d.file == null ? null : () -> reveal(d.file, d.line, d.column));
            if (d.file != null && d.file.equals(path)) {
                CodeDiagnostic cd = new CodeDiagnostic(Math.max(1, d.line), Math.max(1, d.column), d.message);
                cd.setSeverity(d.error ? CodeDiagnostic.ERROR : CodeDiagnostic.WARNING);
                here.add(cd);
            }
        }
        editor.setDiagnostics(here);
        revalidate();
        return errors;
    }

    /// Opens `file` if it is not the one showing and puts the caret on the
    /// line and column a message points at.
    private void reveal(final String file, final int line, final int column) {
        Runnable go = () -> {
            String text = project.getText(file);
            if (text == null) {
                return;
            }
            int off = 0;
            for (int l = 1; l < line && off >= 0; l++) {
                off = text.indexOf('\n', off);
                if (off >= 0) {
                    off++;
                }
            }
            if (off < 0) {
                off = text.length();
            }
            editor.setCursorPosition(Math.min(text.length(), off + Math.max(0, column - 1)));
            if (!tablet) {
                setConsoleOpen(false);
            }
        };
        if (file.equals(path)) {
            go.run();
        } else {
            saveThen(() -> {
                open(file);
                go.run();
            });
        }
    }

    /// A test hook: `scripts/run-device-runtime-ios.sh` drops `cn1ide-autorun`
    /// into the first file system root -- `Documents` on iOS; the app home is
    /// a directory below it named after the app, which the script cannot
    /// know -- and the first editor shown runs its project as if Run had been
    /// tapped. The marker is deleted first, so it fires once.
    private static void autorunIfAsked() {
        com.codename1.io.FileSystemStorage fs = com.codename1.io.FileSystemStorage.getInstance();
        String root = fs.getRoots()[0];
        String marker = root + (root.endsWith("/") ? "" : "/") + "cn1ide-autorun";
        if (fs.exists(marker)) {
            fs.delete(marker);
            System.out.println("CN1SS:IDE autorun");
            current.run();
        }
    }

    static void showProject(IdeProject p) {
        current = new IdeForm(p);
        current.show();
    }

    /// Shows the editor -- the one already open, if there is one -- from any
    /// thread.
    public static void showIt() {
        Display.getInstance().callSerially(() -> {
            Toolbar.setGlobalToolbar(true);
            if (current != null) {
                current.showBack();
            } else {
                showProject(IdeProject.open(IdeProject.DEFAULT_NAME));
                autorunIfAsked();
            }
        });
    }
}
