/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.rive.transcoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.TreeSet;

/// Translates a `.riv` file into Java source: a subclass of
/// `com.codename1.rive.RiveFile` that replays the file's object stream
/// through `com.codename1.impl.rive.RiveBuilder`. The device never parses the
/// file, and the generated code names only the runtime classes the file uses,
/// so the rest of the runtime is stripped from the application.
///
/// Objects of a type the runtime does not have (Luau scripting, and anything a
/// newer editor added) keep their place in the stream as skipped objects,
/// because ids in the file count them. Their properties are dropped, using the
/// header's table of contents for the ones the schema does not know.
public final class RiveTranscoder {
    public static final int MAJOR_VERSION = 7;

    /// The second line of every generated class starts with this, followed by
    /// `stamp()`; a build regenerates a class whose stamp differs, so a changed
    /// transcoder is never paired with classes an older one wrote.
    public static final String STAMP_PREFIX = "// rive-transcoder: ";

    private static String stamp;

    /// A hash of every class file of the transcoder's package (nested and
    /// anonymous classes included), read from the jar or class directory it
    /// was loaded from, so any change to what it emits changes the stamp
    /// without anyone remembering to bump a number or a list of names.
    public static synchronized String stamp() {
        if (stamp == null) {
            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            if (!hashPackage(crc)) {
                crc.reset();
                String[] names = {"RiveTranscoder.class", "RiveSchema.class", "RiveReader.class"};
                for (String n : names) {
                    java.io.InputStream in = RiveTranscoder.class.getResourceAsStream(n);
                    if (in != null) {
                        hashStream(crc, n, in);
                    }
                }
            }
            stamp = Long.toHexString(crc.getValue());
        }
        return stamp;
    }

    private static final String PACKAGE_PATH = "com/codename1/rive/transcoder/";

    /// Hashes every `.class` of this package, in name order, from the code
    /// source; false when the code source cannot be read that way.
    private static boolean hashPackage(java.util.zip.CRC32 crc) {
        try {
            java.security.CodeSource cs = RiveTranscoder.class.getProtectionDomain().getCodeSource();
            if (cs == null || cs.getLocation() == null) {
                return false;
            }
            java.io.File src = new java.io.File(cs.getLocation().toURI());
            java.util.TreeMap<String, byte[]> classes = new java.util.TreeMap<String, byte[]>();
            if (src.isDirectory()) {
                java.io.File[] files = new java.io.File(src, PACKAGE_PATH).listFiles();
                if (files == null) {
                    return false;
                }
                for (java.io.File f : files) {
                    if (f.getName().endsWith(".class")) {
                        classes.put(f.getName(), java.nio.file.Files.readAllBytes(f.toPath()));
                    }
                }
            } else {
                try (java.util.jar.JarFile jar = new java.util.jar.JarFile(src)) {
                    java.util.Enumeration<java.util.jar.JarEntry> en = jar.entries();
                    while (en.hasMoreElements()) {
                        java.util.jar.JarEntry e = en.nextElement();
                        String n = e.getName();
                        if (n.startsWith(PACKAGE_PATH) && n.endsWith(".class")
                                && n.indexOf('/', PACKAGE_PATH.length()) < 0) {
                            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                            try (java.io.InputStream in = jar.getInputStream(e)) {
                                byte[] buf = new byte[8192];
                                int r;
                                while ((r = in.read(buf)) > 0) {
                                    bo.write(buf, 0, r);
                                }
                            }
                            classes.put(n.substring(PACKAGE_PATH.length()), bo.toByteArray());
                        }
                    }
                }
            }
            if (classes.isEmpty()) {
                return false;
            }
            for (java.util.Map.Entry<String, byte[]> e : classes.entrySet()) {
                crc.update(e.getKey().getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                crc.update(e.getValue());
            }
            return true;
        } catch (IOException | java.net.URISyntaxException | RuntimeException ex) {
            return false;
        }
    }

    private static void hashStream(java.util.zip.CRC32 crc, String n, java.io.InputStream in) {
        byte[] buf = new byte[8192];
        try {
            int r;
            while ((r = in.read(buf)) > 0) {
                crc.update(buf, 0, r);
            }
        } catch (IOException ex) {
            crc.update(n.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // nothing was written; the hash is already taken
            }
        }
    }
    static final int ARTBOARD_TYPE = 1;
    static final int LINEAR_ANIMATION_TYPE = 31;
    static final int STATE_MACHINE_TYPE = 53;
    static final int COMPONENT_NAME_KEY = 4;
    static final int ANIMATION_NAME_KEY = 55;
    /// Statements per generated chunk class. Each chunk is a class of its own,
    /// so neither a method's 64 KB of bytecode nor a class's 65535 constants
    /// limits the size of a file.
    static final int CHUNK = 1500;
    static final int BASE64_CHUNK = 60000;

    /// One object of the stream; `className` is null for a skipped object.
    static final class Obj {
        final int typeKey;
        final String className;
        final List<Object[]> props = new ArrayList<Object[]>();

        Obj(int typeKey, String className) {
            this.typeKey = typeKey;
            this.className = className;
        }

        String string(int key) {
            for (Object[] p : props) {
                if (((Integer) p[0]).intValue() == key && p[2] instanceof String) {
                    return (String) p[2];
                }
            }
            return "";
        }
    }

    private final List<Obj> assets = new ArrayList<Obj>();
    private final List<List<Obj>> artboards = new ArrayList<List<Obj>>();
    /// Runtime classes that exist (so a file holding them builds and the rest
    /// of it plays) but whose behaviour is not implemented yet: they are built
    /// and then do nothing. They are reported as skipped so the build note
    /// tells the truth about what the file will not do on the device.
    static final java.util.Set<String> INERT_TYPES = new java.util.HashSet<String>(java.util.Arrays.asList(
            "TransitionArtboardCondition", "TransitionFocusCondition", "FocusData", "FocusAction",
            "FocusActionClear", "FocusActionTarget", "FocusActionTraversal", "GridTrack",
            "GridItemPlacement", "ScrollPhysics", "ClampedScrollPhysics", "ElasticScrollPhysics",
            "ScrollConstraint", "ScrollBarConstraint"));

    private final List<String> skippedTypes = new ArrayList<String>();
    private int[] tocKeys = new int[0];
    private int[] tocFields = new int[0];

    private RiveTranscoder() {
    }

    /// The Java source of class `className` in package `pkg` (empty for the
    /// default package) for the `.riv` file in `riv`.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if `riv` is not a Rive file of a supported version
    public static String transcode(byte[] riv, String pkg, String className) throws IOException {
        RiveTranscoder t = new RiveTranscoder();
        t.read(riv);
        return t.emit(pkg, className);
    }

    /// The types of objects in `riv` that the runtime skips or builds without
    /// behaviour (`INERT_TYPES`), one per distinct
    /// type, as `"Name (type <key>)"`; for build notes.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if `riv` is not a Rive file of a supported version
    public static List<String> skippedTypes(byte[] riv) throws IOException {
        RiveTranscoder t = new RiveTranscoder();
        t.read(riv);
        return t.skippedTypes;
    }

    private void read(byte[] bytes) throws IOException {
        RiveReader reader = new RiveReader(bytes);
        if (reader.readByte() != 'R' || reader.readByte() != 'I' || reader.readByte() != 'V'
                || reader.readByte() != 'E') {
            throw new IOException("not a Rive file");
        }
        int major = reader.readVarUint();
        reader.readVarUint(); // minor version
        if (major != MAJOR_VERSION) {
            throw new IOException("unsupported Rive major version " + major);
        }
        reader.readVarUint(); // file id
        readToc(reader);
        if (reader.didOverflow()) {
            throw new IOException("truncated header");
        }
        TreeSet<Integer> skipped = new TreeSet<Integer>();
        List<Obj> current = null;
        while (!reader.reachedEnd()) {
            Obj o = readObject(reader);
            if (o.typeKey != 0 && (o.className == null || INERT_TYPES.contains(o.className))) {
                skipped.add(Integer.valueOf(o.typeKey));
            }
            if (o.className != null && (RiveSchema.isAsset(o.typeKey)
                    || current == null && o.typeKey != ARTBOARD_TYPE)) {
                // Assets, and everything before the first artboard (view
                // models, their instances, enums, converters), belong to the
                // whole file and are built once, in stream order.
                assets.add(o);
            } else if (o.typeKey == ARTBOARD_TYPE) {
                current = new ArrayList<Obj>();
                current.add(o);
                artboards.add(current);
            } else if (current != null) {
                current.add(o);
            }
        }
        if (reader.didOverflow()) {
            throw new IOException("truncated file");
        }
        if (artboards.isEmpty()) {
            throw new IOException("the file has no artboard");
        }
        for (Integer k : skipped) {
            String name = RiveSchema.typeName(k.intValue());
            skippedTypes.add(name == null ? "type " + k : name + " (type " + k + ")");
        }
    }

    private void readToc(RiveReader reader) {
        List<Integer> keys = new ArrayList<Integer>();
        while (true) {
            int k = reader.readVarUint();
            if (k == 0 || reader.didOverflow()) {
                break;
            }
            keys.add(Integer.valueOf(k));
        }
        int n = keys.size();
        tocKeys = new int[n];
        tocFields = new int[n];
        // Four 2-bit field ids per uint32, as rive-runtime's RuntimeHeader
        // reads them (it starts a new word once the bit cursor reaches 8).
        int bit = 8;
        int word = 0;
        for (int i = 0; i < n; i++) {
            if (bit == 8) {
                word = reader.readUint32();
                bit = 0;
            }
            tocKeys[i] = keys.get(i).intValue();
            tocFields[i] = (word >>> bit) & 3;
            bit += 2;
        }
    }

    private int tocField(int key) {
        for (int i = 0; i < tocKeys.length; i++) {
            if (tocKeys[i] == key) {
                return tocFields[i];
            }
        }
        return RiveSchema.FIELD_UNKNOWN;
    }

    private Obj readObject(RiveReader reader) throws IOException {
        int typeKey = reader.readVarUint();
        Obj o = new Obj(typeKey, RiveSchema.className(typeKey));
        while (true) {
            int key = reader.readVarUint();
            if (key == 0 || reader.didOverflow()) {
                break;
            }
            int field = RiveSchema.propertyFieldId(key);
            if (field == RiveSchema.FIELD_UNKNOWN) {
                field = tocField(key);
                if (field == RiveSchema.FIELD_UNKNOWN) {
                    throw new IOException("property " + key + " of type " + typeKey + " has no field type");
                }
                skip(reader, field);
                continue;
            }
            Object v;
            switch (field) {
                case RiveSchema.FIELD_UINT:
                    v = Integer.valueOf(reader.readVarUint());
                    break;
                case RiveSchema.FIELD_DOUBLE:
                    v = Float.valueOf(reader.readFloat32());
                    break;
                case RiveSchema.FIELD_COLOR:
                    v = Integer.valueOf(reader.readUint32());
                    break;
                case RiveSchema.FIELD_BOOL:
                    v = Boolean.valueOf(reader.readByte() != 0);
                    break;
                case RiveSchema.FIELD_STRING:
                    v = reader.readString();
                    break;
                default:
                    v = reader.readBytes();
                    break;
            }
            if (o.className != null) {
                o.props.add(new Object[]{Integer.valueOf(key), Integer.valueOf(field), v});
            }
        }
        return o;
    }

    private static void skip(RiveReader reader, int tocFieldId) {
        switch (tocFieldId) {
            case 0:
                reader.readVarUint64();
                break;
            case 1:
                reader.readBytes();
                break;
            case 2:
            case 3:
                reader.readUint32();
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------ emitting

    private String emit(String pkg, String className) {
        TreeSet<String> used = new TreeSet<String>();
        collect(assets, used);
        for (List<Obj> a : artboards) {
            collect(a, used);
        }
        StringBuilder out = new StringBuilder();
        out.append("// Generated from a .riv file by the Codename One Rive transcoder; do not edit.\n");
        out.append(STAMP_PREFIX).append(stamp()).append('\n');
        if (pkg != null && pkg.length() > 0) {
            out.append("package ").append(pkg).append(";\n\n");
        }
        out.append("import com.codename1.impl.rive.RiveBuilder;\n");
        for (String c : used) {
            out.append("import com.codename1.impl.rive.").append(c).append(";\n");
        }
        out.append("\npublic final class ").append(className).append(" extends com.codename1.rive.RiveFile {\n");
        out.append("    public ").append(className).append("() {\n        super(new String[]{");
        for (int i = 0; i < artboards.size(); i++) {
            out.append(i == 0 ? "" : ", ").append(literal(artboards.get(i).get(0).string(COMPONENT_NAME_KEY)));
        }
        out.append("},\n                ");
        names(out, LINEAR_ANIMATION_TYPE);
        out.append(",\n                ");
        names(out, STATE_MACHINE_TYPE);
        out.append(");\n    }\n");

        List<String> chunks = new ArrayList<String>();
        out.append("\n    @Override\n    protected void buildAssets(RiveBuilder b) {\n");
        for (String c : chunk(assets, "A", chunks)) {
            out.append("        ").append(c).append(".run(b);\n");
        }
        out.append("    }\n\n    @Override\n    protected void buildArtboard(int index, RiveBuilder b) {\n");
        out.append("        switch (index) {\n");
        for (int i = 0; i < artboards.size(); i++) {
            out.append("            case ").append(i).append(":\n");
            for (String c : chunk(artboards.get(i), "B" + i + "_", chunks)) {
                out.append("                ").append(c).append(".run(b);\n");
            }
            out.append("                break;\n");
        }
        out.append("            default:\n                break;\n        }\n    }\n");
        for (String c : chunks) {
            out.append(c);
        }
        out.append("}\n");
        return out.toString();
    }

    private static void collect(List<Obj> objs, TreeSet<String> used) {
        for (Obj o : objs) {
            if (o.className != null) {
                used.add(o.className);
            }
        }
    }

    private void names(StringBuilder out, int typeKey) {
        out.append("new String[][]{");
        for (int i = 0; i < artboards.size(); i++) {
            out.append(i == 0 ? "{" : ", {");
            boolean first = true;
            for (Obj o : artboards.get(i)) {
                if (o.typeKey == typeKey) {
                    out.append(first ? "" : ", ").append(literal(o.string(ANIMATION_NAME_KEY)));
                    first = false;
                }
            }
            out.append("}");
        }
        out.append("}");
    }

    /// Writes `objs` as nested chunk classes into `classes`; answers their names.
    private static List<String> chunk(List<Obj> objs, String prefix, List<String> classes) {
        List<String> names = new ArrayList<String>();
        StringBuilder body = new StringBuilder();
        int statements = 0;
        for (Obj o : objs) {
            int before = body.length();
            object(o, body);
            statements += 2 + o.props.size();
            if (statements >= CHUNK && before > 0) {
                String moved = body.substring(before);
                body.setLength(before);
                names.add(flush(prefix, names.size(), body, classes));
                body.setLength(0);
                body.append(moved);
                statements = 2 + o.props.size();
            }
        }
        if (body.length() > 0) {
            names.add(flush(prefix, names.size(), body, classes));
        }
        return names;
    }

    private static String flush(String prefix, int index, StringBuilder body, List<String> classes) {
        String name = prefix + index;
        classes.add("\n    private static final class " + name + " {\n"
                + "        static void run(RiveBuilder b) {\n" + body + "        }\n    }\n");
        return name;
    }

    private static void object(Obj o, StringBuilder out) {
        if (o.className == null) {
            out.append("            b.skip();\n");
            return;
        }
        out.append("            b.begin(new ").append(o.className).append("());\n");
        for (Object[] p : o.props) {
            int key = ((Integer) p[0]).intValue();
            int field = ((Integer) p[1]).intValue();
            Object v = p[2];
            switch (field) {
                case RiveSchema.FIELD_UINT:
                    out.append("            b.u(").append(key).append(", ").append(v).append(");\n");
                    break;
                case RiveSchema.FIELD_DOUBLE:
                    out.append("            b.d(").append(key).append(", ").append(floatLiteral((Float) v)).append(");\n");
                    break;
                case RiveSchema.FIELD_COLOR:
                    out.append("            b.c(").append(key).append(", 0x")
                            .append(Integer.toHexString(((Integer) v).intValue())).append(");\n");
                    break;
                case RiveSchema.FIELD_BOOL:
                    out.append("            b.b(").append(key).append(", ").append(v).append(");\n");
                    break;
                case RiveSchema.FIELD_STRING:
                    out.append("            b.s(").append(key).append(", ").append(literal((String) v)).append(");\n");
                    break;
                default:
                    out.append("            b.bytes(").append(key).append(", new String[]{");
                    String b64 = Base64.getEncoder().encodeToString((byte[]) v);
                    for (int i = 0; i < b64.length(); i += BASE64_CHUNK) {
                        out.append(i == 0 ? "\"" : ",\n                \"")
                                .append(b64, i, Math.min(b64.length(), i + BASE64_CHUNK)).append('"');
                    }
                    out.append("});\n");
                    break;
            }
        }
        out.append("            b.end();\n");
    }

    static String floatLiteral(Float f) {
        float v = f.floatValue();
        if (Float.isNaN(v)) {
            return "Float.NaN";
        }
        if (Float.isInfinite(v)) {
            return v > 0 ? "Float.POSITIVE_INFINITY" : "Float.NEGATIVE_INFINITY";
        }
        return Float.toString(v) + "f";
    }

    /// A Java string literal; everything outside printable ASCII is escaped
    /// (control characters in octal, the rest as unicode escapes),
    /// so the generated source is ASCII.
    static String literal(String s) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c >= 0x20 && c < 0x7f) {
                out.append(c);
            } else if (c < 0x80) {
                // Octal: javac turns a backslash-u escape of a line break
                // into a real one before it reads the literal.
                out.append('\\').append(Integer.toOctalString(c | 0x200).substring(1));
            } else {
                String h = Integer.toHexString(c);
                out.append("\\u");
                for (int k = h.length(); k < 4; k++) {
                    out.append('0');
                }
                out.append(h);
            }
        }
        return out.append('"').toString();
    }
}
