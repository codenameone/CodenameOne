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
package com.codename1.android.rescompiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/// Compiles an Android `res/` tree and manifest for the Codename One
/// compatibility runtime.
///
/// The output is deliberately small in Java terms: an `R` class per package
/// (the application's code is compiled against it unchanged), one binary
/// resource table that holds every value, style and compiled XML file, the
/// image/font/raw files under flat names, and one generated class with the
/// two things that cannot be data -- `new` for each view class a layout names
/// and for each activity the manifest declares. Everything else, including
/// inflation itself, is runtime code that is the same for every application.
///
/// The same compiler builds the framework: run with
/// [Request#framework] set over the runtime's own `res/`, it assigns
/// `0x01` ids, writes `android.R`, and writes the symbols file application
/// builds later read so their `@android:` references agree with it.
public final class ResourceCompiler {

    public static final String DEFAULT_GENERATED_PACKAGE = "com.codename1.generated.android";
    public static final String APP_IMPL_CLASS = "AndroidAppImpl";
    public static final String APP_TABLE = "cn1_android_res.bin";
    public static final String FRAMEWORK_TABLE = "cn1_android_framework.bin";
    public static final String FRAMEWORK_SYMBOLS_RESOURCE = "META-INF/android-compat/framework.symbols";
    /// The libraries' table and symbols (see [Request#library]).
    public static final String LIBRARY_TABLE = "cn1_android_library.bin";
    public static final String LIBRARY_SYMBOLS_RESOURCE = "META-INF/android-compat/library.symbols";

    /// One res directory and the Java package its R class is generated in.
    public static final class ResSource {
        public final File dir;
        public final String javaPackage;

        public ResSource(File dir, String javaPackage) {
            this.dir = dir;
            this.javaPackage = javaPackage;
        }
    }

    public static final class Request {
        /// Highest priority first: the application's res, then libraries.
        public final List<ResSource> res = new ArrayList<ResSource>();
        public File manifest;
        /// Overrides the manifest's `package`, as a Gradle `namespace` does.
        public String namespace;
        public File javaOut;
        /// Where the table and the flat resource files are written.
        public File resourcesOut;
        public boolean framework;
        /// Compiles the AndroidX and Material resources the runtime ships, once,
        /// with the runtime: package `0x7e`, named like the application's own
        /// package (`app`), with an R class for every library package in
        /// [#res]. The ids are fixed when the runtime is built, so the
        /// runtime's library classes can inline them.
        public boolean library;
        /// App builds: the framework's symbols, read from the runtime jar.
        public InputStream frameworkSymbols;
        /// App builds: the libraries' symbols, read from the runtime jar. They
        /// join the application's own namespace, as the Android Gradle plugin
        /// merges a library's resources into the application: the application
        /// refers to them unqualified (`?attr/colorPrimary`, `app:` attributes,
        /// `parent="Theme.AppCompat"`), its R lists them, and a resource it
        /// defines under a library resource's name keeps the library's id, so
        /// the library's own references see the application's value.
        public InputStream librarySymbols;
        /// Framework builds: where to write the symbols.
        public File symbolsOut;
        /// Framework builds: view tag -> class for every view class the
        /// runtime implements.
        public final Map<String, String> frameworkViews = new TreeMap<String, String>();
        public String generatedPackage = DEFAULT_GENERATED_PACKAGE;
        /// The `assets/` directory; each file is shipped under a flat name and
        /// listed in [#ASSET_INDEX].
        public File assetsDir;
        /// Where to write the `android:onClick` method names the layouts use,
        /// one per line, for the build step that generates their dispatcher.
        public File onClickNamesOut;
    }

    public static final String ASSET_INDEX = "cn1_android_assets.idx";

    public static final class Result {
        public final List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
        public final List<File> javaFiles = new ArrayList<File>();
        public final List<File> resourceFiles = new ArrayList<File>();
        public ManifestInfo manifest;
        public int resourceCount;

        public boolean hasErrors() {
            for (Diagnostic d : diagnostics) {
                if (d.severity == Diagnostic.Severity.ERROR) {
                    return true;
                }
            }
            return false;
        }
    }

    private Request req;
    private Result result;
    private SymbolTable symbols;
    private ValueEncoder encoder;
    private String local;
    private int packageId;
    /// type -> name -> resource, sorted so ids are deterministic.
    private final Map<ResType, TreeMap<String, Resource>> table = new TreeMap<ResType, TreeMap<String, Resource>>();
    /// Locally defined attrs, by bare name.
    private final Map<String, AttrDef> localAttrs = new TreeMap<String, AttrDef>();
    /// Locally declared styleables: name -> qualified attr names.
    private final Map<String, List<String>> localStyleables = new TreeMap<String, List<String>>();
    /// View tags seen in layouts, in a stable order.
    private final Set<String> viewTags = new java.util.TreeSet<String>();
    private final Map<String, String> viewTagLocation = new LinkedHashMap<String, String>();

    public Result compile(Request request) throws IOException {
        this.req = request;
        this.result = new Result();
        this.symbols = new SymbolTable();
        this.local = request.framework ? SymbolTable.FRAMEWORK : SymbolTable.APP;
        this.packageId = request.framework ? SymbolTable.FRAMEWORK_PACKAGE_ID
                : request.library ? SymbolTable.LIBRARY_PACKAGE_ID : SymbolTable.APP_PACKAGE_ID;
        this.encoder = new ValueEncoder(symbols, local);
        if (!request.framework && request.frameworkSymbols != null) {
            symbols.read(SymbolTable.FRAMEWORK, request.frameworkSymbols);
        }
        if (!request.framework && !request.library && request.librarySymbols != null) {
            symbols.read(SymbolTable.APP, request.librarySymbols);
        }
        if (request.framework) {
            for (Map.Entry<String, String> e : request.frameworkViews.entrySet()) {
                symbols.putView(e.getKey(), e.getValue());
            }
        }

        for (int i = 0; i < request.res.size(); i++) {
            scanResDir(request.res.get(i).dir, i);
        }
        assignIds();
        if (request.manifest != null && request.manifest.isFile()) {
            result.manifest = parseManifest(request.manifest);
        }
        encodeAll();
        if (result.hasErrors()) {
            return result;
        }
        writeTable();
        writeRClasses();
        writeAssets();
        writeOnClickNames();
        if (request.framework || request.library) {
            if (request.symbolsOut != null) {
                ensureDir(request.symbolsOut.getParentFile());
                Writer w = new OutputStreamWriter(new FileOutputStream(request.symbolsOut), Charset.forName("UTF-8"));
                try {
                    symbols.write(local, w);
                } finally {
                    w.close();
                }
                result.resourceFiles.add(request.symbolsOut);
            }
        } else {
            writeAppImpl();
        }
        return result;
    }

    // ---------------------------------------------------------------- scan

    private void scanResDir(File resDir, int sourceIndex) throws IOException {
        File[] dirs = resDir.listFiles();
        if (dirs == null) {
            return;
        }
        Arrays.sort(dirs);
        for (File d : dirs) {
            if (!d.isDirectory() || d.getName().startsWith(".")) {
                continue;
            }
            String dn = d.getName();
            int dash = dn.indexOf('-');
            String typeName = dash < 0 ? dn : dn.substring(0, dash);
            List<String> errors = new ArrayList<String>();
            ResConfig cfg = ResConfig.parse(dash < 0 ? "" : dn.substring(dash + 1), errors);
            if (cfg == null) {
                for (String e : errors) {
                    error("E0001", rel(d), 0, e + "; directory skipped");
                }
                continue;
            }
            if (cfg.unsupported != null) {
                warn("W0002", rel(d), 0, "qualifier '" + cfg.unsupported
                        + "' cannot match a Codename One device; resources in this directory are never selected");
            }
            File[] files = d.listFiles();
            if (files == null) {
                continue;
            }
            Arrays.sort(files);
            if (typeName.equals("values")) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".xml")) {
                        parseValues(f, cfg, sourceIndex);
                    }
                }
                continue;
            }
            ResType type = ResType.fromTag(typeName);
            if (type == null || !type.fileBased) {
                warn("W0003", rel(d), 0, "'" + typeName + "' is not a resource directory type; skipped");
                continue;
            }
            for (File f : files) {
                if (!f.isFile() || f.getName().startsWith(".")) {
                    continue;
                }
                scanFile(type, cfg, f, sourceIndex);
            }
        }
    }

    private void scanFile(ResType type, ResConfig cfg, File f, int sourceIndex) throws IOException {
        String fn = f.getName();
        String name;
        String ext;
        if (fn.endsWith(".9.png")) {
            name = fn.substring(0, fn.length() - 6);
            ext = ".9.png";
        } else {
            int dot = fn.lastIndexOf('.');
            name = dot < 0 ? fn : fn.substring(0, dot);
            ext = dot < 0 ? "" : fn.substring(dot);
        }
        if (!isValidName(name)) {
            error("E0002", rel(f), 0, "'" + name + "' is not a valid resource name (letters, digits and _ only)");
            return;
        }
        Resource.Item item;
        if (ext.equals(".xml") && type != ResType.RAW) {
            RawNode root = RawNode.parse(f);
            if (root == null) {
                error("E0003", rel(f), 0, "empty XML resource");
                return;
            }
            collectIds(root, rel(f));
            if (type == ResType.LAYOUT) {
                collectViewTags(root, rel(f));
                collectOnClick(root);
            }
            item = new Resource.Xml(rel(f), root.line, root);
        } else {
            String c = cfg.canonical();
            String flat = (req.framework ? "andrfw_" : "andr_") + type.tag + (c.length() == 0 ? "" : "_" + c.replace('-', '_').replace('+', '_'))
                    + "_" + name + asciiLower(ext);
            item = new Resource.FileRes(rel(f), f, flat);
        }
        addVariant(type, name, cfg, item, sourceIndex);
    }

    private final Map<String, Integer> variantSource = new java.util.HashMap<String, Integer>();

    private void addVariant(ResType type, String name, ResConfig cfg, Resource.Item item, int sourceIndex) {
        Resource r = resource(type, name, true);
        String key = type.tag + "/" + name + "@" + cfg.canonical();
        Integer prior = variantSource.get(key);
        if (prior != null) {
            if (prior == sourceIndex) {
                Resource.Variant v = r.variant(cfg.canonical());
                error("E0004", item.file, item.line, "duplicate resource " + type.tag + "/" + name
                        + (cfg.canonical().length() == 0 ? "" : " for " + cfg.canonical())
                        + (v == null ? "" : " (also defined in " + v.item.file + ")"));
            }
            // A library defining what the application already defines is the
            // normal override; the application's wins.
            return;
        }
        variantSource.put(key, sourceIndex);
        r.variants.add(new Resource.Variant(cfg, item));
    }

    private Resource resource(ResType type, String name, boolean create) {
        TreeMap<String, Resource> m = table.get(type);
        if (m == null) {
            if (!create) {
                return null;
            }
            m = new TreeMap<String, Resource>();
            table.put(type, m);
        }
        Resource r = m.get(name);
        if (r == null && create) {
            r = new Resource(type, name);
            m.put(name, r);
        }
        return r;
    }

    /// Every `@+id/name` in a compiled XML file declares an id.
    private void collectIds(RawNode n, String file) {
        for (RawNode.Attr a : n.attrs) {
            String v = a.value == null ? "" : a.value.trim();
            if (v.startsWith("@+id/")) {
                declareId(v.substring(5), file, n.line);
            } else if (v.startsWith("@+" + local + ":id/") || (req.framework && v.startsWith("@+android:id/"))) {
                declareId(v.substring(v.indexOf('/') + 1), file, n.line);
            }
        }
        for (RawNode c : n.children) {
            collectIds(c, file);
        }
    }

    private void declareId(String name, String file, int line) {
        if (!isValidName(name)) {
            error("E0002", file, line, "'" + name + "' is not a valid id name");
            return;
        }
        Resource r = resource(ResType.ID, name, true);
        if (r.variants.isEmpty()) {
            r.variants.add(new Resource.Variant(ResConfig.DEFAULT,
                    new Resource.Simple(file, line, new Value(Value.TYPE_INT_BOOLEAN, 0, null))));
        }
    }

    private void collectViewTags(RawNode n, String file) {
        String tag = n.tag;
        String cls = null;
        if (tag.equals("view")) {
            cls = n.attr(RawNode.NS_NONE, "class");
        } else if (!tag.equals("merge") && !tag.equals("include") && !tag.equals("requestFocus")
                && !tag.equals("tag") && !tag.equals("fragment") && !tag.equals("blink")) {
            cls = tag;
        }
        if (cls != null) {
            if (viewTags.add(cls)) {
                viewTagLocation.put(cls, file + ":" + n.line);
            }
        }
        for (RawNode c : n.children) {
            collectViewTags(c, file);
        }
    }

    // ---------------------------------------------------------------- values

    private void parseValues(File f, ResConfig cfg, int sourceIndex) throws IOException {
        RawNode root = RawNode.parse(f);
        String file = rel(f);
        if (root == null || !root.tag.equals("resources")) {
            warn("W0004", file, 0, "values file has no <resources> root; skipped");
            return;
        }
        for (RawNode n : root.children) {
            String tag = n.tag;
            String name = n.attr(RawNode.NS_NONE, "name");
            if (tag.equals("eat-comment") || tag.equals("skip") || tag.equals("public") || tag.equals("public-group")
                    || tag.equals("java-symbol") || tag.equals("add-resource") || tag.equals("overlayable")
                    || tag.equals("staging-public-group") || tag.equals("staging-public-group-final")) {
                continue;
            }
            if (name == null) {
                error("E0010", file, n.line, "<" + tag + "> has no name");
                continue;
            }
            if (tag.equals("attr")) {
                defineAttr(n, file);
                continue;
            }
            if (tag.equals("declare-styleable")) {
                declareStyleable(n, file);
                continue;
            }
            ResType type;
            Resource.Item item;
            if (tag.equals("item")) {
                String t = n.attr(RawNode.NS_NONE, "type");
                type = t == null ? null : ResType.fromTag(t);
                if (type == null) {
                    error("E0011", file, n.line, "<item name=\"" + name + "\"> has no valid type");
                    continue;
                }
                if (type == ResType.ID) {
                    declareId(name, file, n.line);
                    continue;
                }
                String fmt = n.attr(RawNode.NS_NONE, "format");
                AttrDef def = fmt == null ? formatFor(type) : new AttrDef(type.tag, AttrDef.parseFormats(fmt));
                item = new Resource.Simple(file, n.line, n.innerText(), def);
            } else if (tag.equals("string")) {
                type = ResType.STRING;
                String text = n.innerText();
                String trimmed = text.trim();
                if (trimmed.startsWith("@") || trimmed.startsWith("?")) {
                    // <string name="a">@string/b</string> is an alias, as
                    // in aapt; an escaped \@ never gets here.
                    item = new Resource.Simple(file, n.line, trimmed, formatFor(ResType.STRING));
                } else {
                    item = new Resource.Simple(file, n.line, Value.string(AndroidStrings.unescape(text)));
                }
            } else if (tag.equals("string-array") || tag.equals("integer-array") || tag.equals("array")) {
                type = ResType.ARRAY;
                Resource.Bag bag = new Resource.Bag(file, n.line, null);
                AttrDef fmt = tag.equals("integer-array") ? new AttrDef("integer", AttrDef.FORMAT_INTEGER | AttrDef.FORMAT_REFERENCE)
                        : tag.equals("string-array") ? new AttrDef("string", AttrDef.FORMAT_STRING | AttrDef.FORMAT_REFERENCE) : null;
                for (RawNode c : n.children) {
                    if (c.tag.equals("item")) {
                        bag.entries.add(new Resource.RawEntry(null, c.innerText(), c.line, fmt));
                    }
                }
                item = bag;
            } else if (tag.equals("plurals")) {
                type = ResType.PLURALS;
                Resource.Bag bag = new Resource.Bag(file, n.line, null);
                for (RawNode c : n.children) {
                    if (c.tag.equals("item")) {
                        String q = c.attr(RawNode.NS_NONE, "quantity");
                        if (q == null || Resource.QUANTITIES.indexOf(q) < 0) {
                            error("E0012", file, c.line, "plural item has no valid quantity");
                            continue;
                        }
                        bag.entries.add(new Resource.RawEntry(q, c.innerText(), c.line,
                                new AttrDef("string", AttrDef.FORMAT_STRING | AttrDef.FORMAT_REFERENCE)));
                    }
                }
                item = bag;
            } else if (tag.equals("style")) {
                type = ResType.STYLE;
                String parent = n.attr(RawNode.NS_NONE, "parent");
                if (parent == null) {
                    // Theme.App.Dark implicitly inherits Theme.App -- resolved
                    // once every style is known.
                    int dot = name.lastIndexOf('.');
                    parent = dot > 0 ? name.substring(0, dot) : null;
                }
                Resource.Bag bag = new Resource.Bag(file, n.line, parent);
                bag.implicitParent = n.attr(RawNode.NS_NONE, "parent") == null;
                for (RawNode c : n.children) {
                    if (c.tag.equals("item")) {
                        String key = c.attr(RawNode.NS_NONE, "name");
                        if (key == null) {
                            error("E0013", file, c.line, "style item has no name");
                            continue;
                        }
                        bag.entries.add(new Resource.RawEntry(key, c.innerText(), c.line, null));
                    }
                }
                item = bag;
            } else {
                type = ResType.fromTag(tag);
                if (type == null || type == ResType.LAYOUT || type == ResType.MENU || type == ResType.XML) {
                    warn("W0005", file, n.line, "<" + tag + "> is not a values resource; skipped");
                    continue;
                }
                if (type == ResType.ID) {
                    declareId(name, file, n.line);
                    continue;
                }
                item = new Resource.Simple(file, n.line, n.innerText(), formatFor(type));
            }
            if (!isValidName(name.replace('.', '_'))) {
                error("E0002", file, n.line, "'" + name + "' is not a valid resource name");
                continue;
            }
            addVariant(type, name, cfg, item, sourceIndex);
        }
    }

    /// The format a values element implies for its body.
    private static AttrDef formatFor(ResType type) {
        switch (type) {
            case COLOR:
            case DRAWABLE:
                return new AttrDef(type.tag, AttrDef.FORMAT_COLOR | AttrDef.FORMAT_REFERENCE);
            case DIMEN:
                return new AttrDef(type.tag, AttrDef.FORMAT_DIMENSION | AttrDef.FORMAT_FLOAT | AttrDef.FORMAT_REFERENCE);
            case INTEGER:
                return new AttrDef(type.tag, AttrDef.FORMAT_INTEGER | AttrDef.FORMAT_REFERENCE);
            case BOOL:
                return new AttrDef(type.tag, AttrDef.FORMAT_BOOLEAN | AttrDef.FORMAT_REFERENCE);
            case FRACTION:
                return new AttrDef(type.tag, AttrDef.FORMAT_FRACTION | AttrDef.FORMAT_REFERENCE);
            case STRING:
                return new AttrDef(type.tag, AttrDef.FORMAT_STRING | AttrDef.FORMAT_REFERENCE);
            default:
                return null;
        }
    }

    private void defineAttr(RawNode n, String file) {
        String name = n.attr(RawNode.NS_NONE, "name");
        if (name.startsWith("android:")) {
            // A reference to a framework attr, not a definition.
            return;
        }
        if (name.indexOf(':') >= 0) {
            name = name.substring(name.indexOf(':') + 1);
        }
        AttrDef def = localAttrs.get(name);
        String fmt = n.attr(RawNode.NS_NONE, "format");
        boolean defines = fmt != null || !n.children.isEmpty();
        if (def == null && !req.library && !req.framework) {
            AttrDef lib = symbols.attr(local + ":" + name);
            if (lib != null) {
                // A library attr: <attr name="colorPrimary"/> in a styleable
                // only refers to it. A redefinition adds its formats and values
                // to the library's, which keeps the one id.
                if (defines) {
                    mergeAttrDefinition(lib, n, fmt, file);
                }
                return;
            }
        }
        if (def == null) {
            def = new AttrDef(local + ":" + name, 0);
            localAttrs.put(name, def);
            Resource r = resource(ResType.ATTR, name, true);
            if (r.variants.isEmpty()) {
                r.variants.add(new Resource.Variant(ResConfig.DEFAULT,
                        new Resource.Simple(file, n.line, new Value(Value.TYPE_INT_DEC, 0, null))));
            }
        }
        if (defines) {
            mergeAttrDefinition(def, n, fmt, file);
        }
    }

    private void mergeAttrDefinition(AttrDef def, RawNode n, String fmt, String file) {
        def.formats |= AttrDef.parseFormats(fmt);
        for (RawNode c : n.children) {
            String cn = c.attr(RawNode.NS_NONE, "name");
            String cv = c.attr(RawNode.NS_KEY_ANDROID, "value") != null ? c.attr(RawNode.NS_KEY_ANDROID, "value")
                    : c.attr(RawNode.NS_NONE, "value");
            if (cn == null || cv == null) {
                continue;
            }
            int v;
            try {
                v = SymbolTable.parseInt(cv);
            } catch (NumberFormatException e) {
                error("E0014", file, c.line, "attr value '" + cv + "' is not an integer");
                continue;
            }
            if (c.tag.equals("enum")) {
                def.formats |= AttrDef.FORMAT_ENUM;
                def.enums.put(cn, v);
            } else if (c.tag.equals("flag")) {
                def.formats |= AttrDef.FORMAT_FLAGS;
                def.flags.put(cn, v);
            }
        }
    }

    private void declareStyleable(RawNode n, String file) {
        String name = n.attr(RawNode.NS_NONE, "name");
        List<String> attrs = localStyleables.get(name);
        if (attrs == null) {
            attrs = new ArrayList<String>();
            localStyleables.put(name, attrs);
        }
        for (RawNode c : n.children) {
            if (!c.tag.equals("attr")) {
                continue;
            }
            String an = c.attr(RawNode.NS_NONE, "name");
            if (an == null) {
                continue;
            }
            String q;
            if (an.startsWith("android:")) {
                q = an;
            } else {
                defineAttr(c, file);
                q = local + ":" + (an.indexOf(':') >= 0 ? an.substring(an.indexOf(':') + 1) : an);
            }
            if (!attrs.contains(q)) {
                attrs.add(q);
            }
        }
    }

    // ---------------------------------------------------------------- ids

    private void assignIds() {
        for (Map.Entry<ResType, TreeMap<String, Resource>> e : table.entrySet()) {
            int index = 0;
            for (Resource r : e.getValue().values()) {
                int preset = symbols.get(local, e.getKey(), r.name);
                if (preset != 0 && (preset >>> 24) == SymbolTable.LIBRARY_PACKAGE_ID && packageId == SymbolTable.APP_PACKAGE_ID) {
                    // Overrides a library resource: same id, the application's
                    // table answers first.
                    r.id = preset;
                    continue;
                }
                r.id = (packageId << 24) | (e.getKey().typeId << 16) | index++;
                symbols.put(local, e.getKey(), r.name, r.id);
            }
        }
        for (Map.Entry<String, AttrDef> e : localAttrs.entrySet()) {
            symbols.putAttr(local + ":" + e.getKey(), e.getValue());
        }
        for (Map.Entry<String, List<String>> e : localStyleables.entrySet()) {
            symbols.putStyleable(local, e.getKey(), e.getValue());
        }
    }

    // ---------------------------------------------------------------- encode

    private void encodeAll() {
        for (TreeMap<String, Resource> m : table.values()) {
            for (Resource r : m.values()) {
                result.resourceCount++;
                for (Resource.Variant v : r.variants) {
                    encodeItem(r, v.item);
                }
            }
        }
        for (Map.Entry<String, List<String>> e : localStyleables.entrySet()) {
            for (String a : e.getValue()) {
                if (symbols.attrId(a) == 0) {
                    error("E0015", null, 0, "styleable " + e.getKey() + " names attr " + a + ", which does not exist");
                }
            }
        }
        if (!req.framework) {
            for (String tag : viewTags) {
                if (tag.indexOf('.') < 0 && !symbols.views().isEmpty() && !symbols.views().containsKey(tag)) {
                    String loc = viewTagLocation.get(tag);
                    int colon = loc.lastIndexOf(':');
                    error("E0201", loc.substring(0, colon), Integer.parseInt(loc.substring(colon + 1)),
                            "<" + tag + "> is not a view class the Codename One Android runtime implements");
                }
            }
        }
    }

    private void encodeItem(final Resource r, final Resource.Item item) {
        if (item instanceof Resource.Simple) {
            final Resource.Simple s = (Resource.Simple) item;
            if (s.value == null) {
                String raw = s.raw;
                if (r.type != ResType.STRING) {
                    raw = raw == null ? "" : raw.trim();
                }
                s.value = encoder.encode(raw, s.format, problems(item.file, item.line));
            }
        } else if (item instanceof Resource.Bag) {
            Resource.Bag b = (Resource.Bag) item;
            int n = b.entries.size();
            b.keys = new int[n];
            b.values = new Value[n];
            if (r.type == ResType.STYLE) {
                b.parentId = resolveStyleParent(b);
            }
            for (int i = 0; i < n; i++) {
                Resource.RawEntry e = b.entries.get(i);
                ValueEncoder.Problems p = problems(item.file, e.line);
                AttrDef def = e.format;
                if (r.type == ResType.STYLE) {
                    String q = qualifyAttr(e.key);
                    b.keys[i] = symbols.attrId(q);
                    if (b.keys[i] == 0) {
                        p.error("E0103", "style " + r.name + " sets unknown attr " + e.key);
                    }
                    def = symbols.attr(q);
                } else if (r.type == ResType.PLURALS) {
                    b.keys[i] = Resource.QUANTITIES.indexOf(e.key);
                } else {
                    b.keys[i] = i;
                }
                String raw = e.raw;
                if (def == null || !def.allows(AttrDef.FORMAT_STRING) || r.type != ResType.STYLE) {
                    raw = raw.trim();
                }
                b.values[i] = encoder.encode(raw, def, p);
                if (r.type != ResType.STYLE && b.values[i].isString()) {
                    b.values[i] = Value.string(AndroidStrings.unescape(e.raw));
                }
            }
        } else if (item instanceof Resource.Xml) {
            Resource.Xml x = (Resource.Xml) item;
            x.compiled = compileXml(x.root, item.file);
        }
    }

    private int resolveStyleParent(Resource.Bag b) {
        String p = b.parentRaw;
        if (p == null || p.length() == 0) {
            return 0;
        }
        if (b.implicitParent) {
            return symbols.get(local, ResType.STYLE, p);
        }
        String ref = p;
        if (!ref.startsWith("@")) {
            int colon = ref.indexOf(':');
            ref = colon >= 0 ? "@" + ref.substring(0, colon) + ":style/" + ref.substring(colon + 1) : "@style/" + ref;
        }
        Value v = encoder.encodeReference(ref, problems(b.file, b.line));
        return v != null && v.type == Value.TYPE_REFERENCE ? v.data : 0;
    }

    /// `textColor` -> `app:textColor` (or `android:` when compiling the
    /// framework); `android:textColor` stays.
    private String qualifyAttr(String key) {
        int colon = key.indexOf(':');
        if (colon < 0) {
            return local + ":" + key;
        }
        String pkg = key.substring(0, colon);
        return encoder.normalizePackage(pkg) + key.substring(colon);
    }

    private XmlTree compileXml(RawNode n, String file) {
        XmlTree t = new XmlTree(n.tag, n.line);
        for (RawNode.Attr a : n.attrs) {
            String q = null;
            if (a.ns == RawNode.NS_KEY_ANDROID) {
                q = SymbolTable.FRAMEWORK + ":" + a.name;
            } else if (a.ns == RawNode.NS_KEY_APP) {
                q = local + ":" + a.name;
            }
            int attrId = q == null ? 0 : symbols.attrId(q);
            AttrDef def = q == null ? null : symbols.attr(q);
            ValueEncoder.Problems p = problems(file, n.line);
            if (q != null && attrId == 0 && !(req.framework && a.ns == RawNode.NS_KEY_ANDROID)) {
                p.warning("W0102", "unknown attribute " + q + " on <" + n.tag + ">; it is kept but no view reads it");
            }
            Value v = encoder.encode(a.value, def, p);
            t.attrs.add(new XmlTree.Attr(attrId, a.ns, a.name, v));
        }
        String text = n.ownText().trim();
        t.text = text.length() == 0 ? null : text;
        for (RawNode c : n.children) {
            t.children.add(compileXml(c, file));
        }
        return t;
    }

    private ValueEncoder.Problems problems(final String file, final int line) {
        return new ValueEncoder.Problems() {
            @Override
            public void error(String code, String message) {
                ResourceCompiler.this.error(code, file, line, message);
            }

            @Override
            public void warning(String code, String message) {
                ResourceCompiler.this.warn(code, file, line, message);
            }
        };
    }

    // ---------------------------------------------------------------- manifest

    private ManifestInfo parseManifest(File f) throws IOException {
        RawNode root = RawNode.parse(f);
        String file = rel(f);
        ManifestInfo m = new ManifestInfo();
        m.packageName = req.namespace != null ? req.namespace : root.attr(RawNode.NS_NONE, "package");
        if (m.packageName == null) {
            error("E0301", file, root.line, "the manifest has no package and no namespace was configured");
            m.packageName = "app";
        }
        m.versionName = root.attr(RawNode.NS_KEY_ANDROID, "versionName");
        String vc = root.attr(RawNode.NS_KEY_ANDROID, "versionCode");
        if (vc != null) {
            try {
                m.versionCode = Integer.parseInt(vc.trim());
            } catch (NumberFormatException e) {
                warn("W0301", file, root.line, "versionCode '" + vc + "' is not an integer");
            }
        }
        for (RawNode n : root.children) {
            if (n.tag.equals("uses-sdk")) {
                m.minSdk = intOr(n.attr(RawNode.NS_KEY_ANDROID, "minSdkVersion"), 0);
                m.targetSdk = intOr(n.attr(RawNode.NS_KEY_ANDROID, "targetSdkVersion"), 0);
            } else if (n.tag.equals("uses-permission")) {
                String p = n.attr(RawNode.NS_KEY_ANDROID, "name");
                if (p != null) {
                    m.permissions.add(p);
                }
            } else if (n.tag.equals("application")) {
                ValueEncoder.Problems p = problems(file, n.line);
                m.applicationClass = ManifestInfo.resolveClass(m.packageName, n.attr(RawNode.NS_KEY_ANDROID, "name"));
                m.appTheme = encodeManifestRef(n.attr(RawNode.NS_KEY_ANDROID, "theme"), p);
                m.appLabel = encodeManifestRef(n.attr(RawNode.NS_KEY_ANDROID, "label"), p);
                m.appIcon = encodeManifestRef(n.attr(RawNode.NS_KEY_ANDROID, "icon"), p);
                for (RawNode a : n.children) {
                    if (a.tag.equals("activity") || a.tag.equals("activity-alias")) {
                        if (a.tag.equals("activity-alias")) {
                            warn("W0302", file, a.line, "<activity-alias> is not supported; ignored");
                            continue;
                        }
                        // android:enabled="false" makes an activity unreachable:
                        // no filter selects it, an explicit start fails and it
                        // is never the launcher. The runtime has no
                        // setComponentEnabledSetting to switch it back on, so
                        // it is left out entirely. A resource reference
                        // (@bool/...) can differ per configuration and is
                        // kept, as an enabled activity.
                        if ("false".equals(a.attr(RawNode.NS_KEY_ANDROID, "enabled"))) {
                            continue;
                        }
                        m.activities.add(parseActivity(a, m.packageName, file));
                    }
                }
            }
        }
        return m;
    }

    private ManifestInfo.Activity parseActivity(RawNode n, String pkg, String file) {
        ValueEncoder.Problems p = problems(file, n.line);
        ManifestInfo.Activity a = new ManifestInfo.Activity();
        a.line = n.line;
        a.className = ManifestInfo.resolveClass(pkg, n.attr(RawNode.NS_KEY_ANDROID, "name"));
        a.theme = encodeManifestRef(n.attr(RawNode.NS_KEY_ANDROID, "theme"), p);
        a.label = encodeManifestRef(n.attr(RawNode.NS_KEY_ANDROID, "label"), p);
        a.screenOrientation = n.attr(RawNode.NS_KEY_ANDROID, "screenOrientation");
        a.windowSoftInputMode = n.attr(RawNode.NS_KEY_ANDROID, "windowSoftInputMode");
        String mode = n.attr(RawNode.NS_KEY_ANDROID, "launchMode");
        if (mode != null) {
            int idx = java.util.Arrays.asList("standard", "singleTop", "singleTask", "singleInstance",
                    "singleInstancePerTask").indexOf(mode.trim());
            if (idx < 0) {
                warn("W0305", file, n.line, "launchMode '" + mode + "' is not one Android defines; using standard");
            } else {
                a.launchMode = idx;
            }
        }
        a.noHistory = "true".equals(n.attr(RawNode.NS_KEY_ANDROID, "noHistory"));
        String changes = n.attr(RawNode.NS_KEY_ANDROID, "configChanges");
        if (changes != null) {
            for (String flag : changes.split("\\|")) {
                String f = flag.trim();
                if (f.isEmpty()) {
                    continue;
                }
                int bit = ManifestInfo.configChangeBit(f);
                if (bit == 0) {
                    warn("W0303", file, n.line, "configChanges flag '" + f + "' is not one Android defines; ignored");
                } else {
                    a.configChanges |= bit;
                }
            }
        }
        for (RawNode f : n.children) {
            if (!f.tag.equals("intent-filter")) {
                continue;
            }
            ManifestInfo.IntentFilter filter = parseIntentFilter(f, file);
            if (filter.actions.contains("android.intent.action.MAIN")
                    && filter.categories.contains("android.intent.category.LAUNCHER")) {
                a.launcher = true;
            }
            if (filter.actions.isEmpty()) {
                // Android drops a filter with no action: it can never match.
                continue;
            }
            if (!unsupportedData(f, file)) {
                a.filters.add(filter);
            }
        }
        return a;
    }

    /// The `<data>` attributes and the `PatternMatcher` type each pattern
    /// attribute stands for (`PATTERN_LITERAL` 0, `PREFIX` 1, `SIMPLE_GLOB`
    /// 2, `SUFFIX` 4).
    private static final String[] PATH_ATTRS = {"path", "pathPrefix", "pathPattern", "pathSuffix"};
    private static final String[] SSP_ATTRS = {"ssp", "sspPrefix", "sspPattern", "sspSuffix"};
    private static final int[] PATTERN_TYPES = {0, 1, 2, 4};

    /// Keeps a whole `<intent-filter>` -- actions, categories and every
    /// `<data>` constraint -- because flattening it to its actions made an
    /// `ACTION_VIEW` filter for a custom scheme capture every browser,
    /// dialer and share intent. Attribute values go through the resource
    /// string unescaping aapt applies, so `pathPattern=".*\\.pdf"` reaches
    /// the matcher as `.*\.pdf`, as it does on a device.
    private ManifestInfo.IntentFilter parseIntentFilter(RawNode f, String file) {
        ManifestInfo.IntentFilter filter = new ManifestInfo.IntentFilter();
        for (RawNode c : f.children) {
            if (c.tag.equals("action") || c.tag.equals("category")) {
                String name = c.attr(RawNode.NS_KEY_ANDROID, "name");
                if (name == null) {
                    continue;
                }
                List<String> into = c.tag.equals("action") ? filter.actions : filter.categories;
                if (!into.contains(name)) {
                    into.add(name);
                }
            } else if (c.tag.equals("data")) {
                String scheme = dataAttr(c, "scheme");
                if (scheme != null && !filter.schemes.contains(scheme)) {
                    filter.schemes.add(scheme);
                }
                String host = dataAttr(c, "host");
                if (host != null) {
                    filter.authorities.add(new ManifestInfo.Authority(host, dataAttr(c, "port")));
                }
                for (int i = 0; i < PATH_ATTRS.length; i++) {
                    String v = dataAttr(c, PATH_ATTRS[i]);
                    if (v != null) {
                        filter.paths.add(new ManifestInfo.DataPattern(v, PATTERN_TYPES[i]));
                    }
                    v = dataAttr(c, SSP_ATTRS[i]);
                    if (v != null) {
                        filter.schemeSpecificParts.add(new ManifestInfo.DataPattern(v, PATTERN_TYPES[i]));
                    }
                }
                String type = dataAttr(c, "mimeType");
                if (type != null) {
                    if (type.indexOf('/') <= 0) {
                        error("E0304", file, c.line, "mimeType '" + type + "' is not a MIME type (type/subtype)");
                    } else if (!filter.types.contains(type)) {
                        filter.types.add(type);
                    }
                }
            }
        }
        return filter;
    }

    private static String dataAttr(RawNode c, String name) {
        String v = c.attr(RawNode.NS_KEY_ANDROID, name);
        return v == null ? null : AndroidStrings.unescape(v);
    }

    /// True, with a warning, when a filter's `<data>` uses something the
    /// runtime cannot match. Such a filter is dropped rather than kept
    /// without the constraint: a filter that matched more than it declared
    /// would take intents meant for the browser or another application.
    private boolean unsupportedData(RawNode f, String file) {
        for (RawNode c : f.children) {
            if (!c.tag.equals("data")) {
                continue;
            }
            String[] unsupported = {"pathAdvancedPattern", "sspAdvancedPattern", "mimeGroup"};
            for (String u : unsupported) {
                if (c.attr(RawNode.NS_KEY_ANDROID, u) != null) {
                    warn("W0304", file, c.line, "<data android:" + u + "> is not supported; the intent filter is"
                            + " ignored");
                    return true;
                }
            }
            for (RawNode.Attr at : c.attrs) {
                if (at.ns == RawNode.NS_KEY_ANDROID && at.value != null && at.value.startsWith("@")) {
                    warn("W0304", file, c.line, "<data android:" + at.name + "> names a resource; only literal"
                            + " values are supported, and the intent filter is ignored");
                    return true;
                }
            }
        }
        return false;
    }

    private Value encodeManifestRef(String raw, ValueEncoder.Problems p) {
        if (raw == null) {
            return null;
        }
        return encoder.encode(raw, new AttrDef("manifest", AttrDef.FORMAT_REFERENCE | AttrDef.FORMAT_STRING), p);
    }

    private static int intOr(String s, int def) {
        if (s == null) {
            return def;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ---------------------------------------------------------------- output

    private static final Comparator<Resource> BY_ID = new Comparator<Resource>() {
        @Override
        public int compare(Resource a, Resource b) {
            return a.id < b.id ? -1 : (a.id == b.id ? 0 : 1);
        }
    };

    /// Creates `dir` if needed and fails if it is still not a directory.
    static void ensureDir(File dir) throws IOException {
        if (dir != null && !dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IOException("Cannot create directory " + dir);
        }
    }

    private void writeTable() throws IOException {
        File out = new File(req.resourcesOut, req.framework ? FRAMEWORK_TABLE : req.library ? LIBRARY_TABLE : APP_TABLE);
        ensureDir(out.getParentFile());
        List<Resource> all = new ArrayList<Resource>();
        for (TreeMap<String, Resource> m : table.values()) {
            all.addAll(m.values());
        }
        Collections.sort(all, BY_ID);
        OutputStream os = new FileOutputStream(out);
        try {
            new TableWriter(packageId).write(all, os);
        } finally {
            os.close();
        }
        result.resourceFiles.add(out);
        for (Resource r : all) {
            for (Resource.Variant v : r.variants) {
                if (v.item instanceof Resource.FileRes) {
                    Resource.FileRes fr = (Resource.FileRes) v.item;
                    File dest = new File(req.resourcesOut, fr.flatName);
                    copyIfChanged(fr.source, dest);
                    result.resourceFiles.add(dest);
                }
            }
        }
    }

    static void copyIfChanged(File src, File dest) throws IOException {
        if (dest.isFile() && dest.length() == src.length() && dest.lastModified() >= src.lastModified()) {
            return;
        }
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dest);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private void writeRClasses() throws IOException {
        Set<String> packages = new LinkedHashSet<String>();
        if (req.framework) {
            packages.add("android");
        } else if (req.library) {
            for (ResSource s : req.res) {
                packages.add(s.javaPackage);
            }
        } else {
            if (result.manifest != null) {
                packages.add(result.manifest.packageName);
            } else if (req.namespace != null) {
                packages.add(req.namespace);
            }
            for (ResSource s : req.res) {
                if (s.javaPackage != null) {
                    packages.add(s.javaPackage);
                }
            }
        }
        JavaWriter jw = new JavaWriter(symbols, local);
        for (String pkg : packages) {
            File f = jw.writeR(req.javaOut, pkg, !req.framework);
            result.javaFiles.add(f);
        }
    }

    private void writeAppImpl() throws IOException {
        Map<String, String> tagToClass = new TreeMap<String, String>();
        for (String tag : viewTags) {
            String cls = tag.indexOf('.') >= 0 ? tag : symbols.views().get(tag);
            if (cls == null) {
                cls = "android.widget." + tag;
            }
            tagToClass.put(tag, cls);
        }
        JavaWriter jw = new JavaWriter(symbols, local);
        File f = jw.writeAppImpl(req.javaOut, req.generatedPackage, APP_IMPL_CLASS, APP_TABLE,
                result.manifest, tagToClass);
        result.javaFiles.add(f);
    }

    private final Set<String> onClickNames = new java.util.TreeSet<String>();

    private void collectOnClick(RawNode n) {
        String v = n.attr(RawNode.NS_KEY_ANDROID, "onClick");
        if (v != null && v.trim().length() > 0 && !v.trim().startsWith("@")) {
            onClickNames.add(v.trim());
        }
        for (RawNode c : n.children) {
            collectOnClick(c);
        }
    }

    private void writeOnClickNames() throws IOException {
        if (req.onClickNamesOut == null) {
            return;
        }
        ensureDir(req.onClickNamesOut.getParentFile());
        Writer w = new OutputStreamWriter(new FileOutputStream(req.onClickNamesOut), Charset.forName("UTF-8"));
        try {
            for (String n : onClickNames) {
                w.write(n);
                w.write('\n');
            }
        } finally {
            w.close();
        }
    }

    /// Ships every asset under `andra_<hash>_<name>` and writes the index the
    /// runtime's `AssetManager` reads to map paths back.
    private void writeAssets() throws IOException {
        // The output directory persists between builds, and the index is what
        // AssetManager.list() and open() read: an asset deleted from the
        // project, the last one included (which removes the assets directory
        // too), must leave the index and its copy, not linger in both.
        java.util.Set<String> shipped = new java.util.HashSet<String>();
        try {
            if (req.assetsDir == null || !req.assetsDir.isDirectory()) {
                return;
            }
            writeAssetIndex(shipped);
        } finally {
            removeStaleAssets(shipped);
        }
    }

    private void removeStaleAssets(java.util.Set<String> shipped) {
        File[] existing = req.resourcesOut == null ? null : req.resourcesOut.listFiles();
        if (existing == null) {
            return;
        }
        for (File f : existing) {
            String n = f.getName();
            if ((n.startsWith("andra_") || n.equals(ASSET_INDEX)) && !shipped.contains(n) && !f.delete()) {
                throw new IllegalStateException("Cannot delete the stale asset output " + f);
            }
        }
    }

    private void writeAssetIndex(java.util.Set<String> shipped) throws IOException {
        List<String> paths = new ArrayList<String>();
        listFiles(req.assetsDir, "", paths);
        Collections.sort(paths);
        StringBuilder index = new StringBuilder();
        // Output name (case folded, for case-insensitive file systems) to the
        // asset that claimed it: two assets must never share a copy.
        Map<String, String> claimed = new HashMap<String, String>();
        for (String p : paths) {
            String flat = flatAssetName(p);
            String owner = claimed.put(asciiLower(flat), p);
            if (owner != null) {
                throw new IOException("The assets " + owner + " and " + p + " both map to the output file " + flat);
            }
            File dest = new File(req.resourcesOut, flat);
            copyIfChanged(new File(req.assetsDir, p), dest);
            result.resourceFiles.add(dest);
            shipped.add(flat);
            index.append(p).append('\t').append(flat).append('\n');
        }
        File idx = new File(req.resourcesOut, ASSET_INDEX);
        OutputStream os = new FileOutputStream(idx);
        try {
            os.write(index.toString().getBytes(Charset.forName("UTF-8")));
        } finally {
            os.close();
        }
        result.resourceFiles.add(idx);
        shipped.add(ASSET_INDEX);
    }

    /// The flat output name of the asset at relative path `path`:
    /// `andra_<digest>_<basename>`. The digest is SHA-256 of the whole path,
    /// not `String.hashCode()`, under which `Aa/file.txt` and `BB/file.txt`
    /// collide and one asset silently overwrote the other.
    static String flatAssetName(String path) {
        String base = path.substring(path.lastIndexOf('/') + 1);
        StringBuilder sb = new StringBuilder("andra_");
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(path.getBytes(Charset.forName("UTF-8")));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
        // 128 bits: no accidental collision across any asset tree.
        for (int i = 0; i < 16; i++) {
            int b = digest[i] & 0xff;
            sb.append(Character.forDigit(b >> 4, 16)).append(Character.forDigit(b & 0xf, 16));
        }
        sb.append('_');
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            sb.append((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.'
                    || c == '_' || c == '-' ? c : '_');
        }
        return sb.toString();
    }

    private static void listFiles(File dir, String prefix, List<String> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.getName().startsWith(".")) {
                continue;
            }
            if (f.isDirectory()) {
                listFiles(f, prefix + f.getName() + "/", out);
            } else {
                out.add(prefix + f.getName());
            }
        }
    }

    // ---------------------------------------------------------------- util

    private String rel(File f) {
        for (ResSource s : req.res) {
            String base = s.dir.getParentFile() == null ? "" : s.dir.getParentFile().getPath();
            if (f.getPath().startsWith(base) && base.length() > 0) {
                return f.getPath().substring(base.length() + 1).replace(File.separatorChar, '/');
            }
        }
        return f.getPath().replace(File.separatorChar, '/');
    }

    static boolean isValidName(String name) {
        if (name.length() == 0 || Character.isDigit(name.charAt(0))) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')) {
                return false;
            }
        }
        return true;
    }

    static String asciiLower(String s) {
        return ResConfig.asciiLower(s);
    }

    private void error(String code, String file, int line, String msg) {
        result.diagnostics.add(new Diagnostic(Diagnostic.Severity.ERROR, code, file, line, msg));
    }

    private void warn(String code, String file, int line, String msg) {
        result.diagnostics.add(new Diagnostic(Diagnostic.Severity.WARNING, code, file, line, msg));
    }
}
