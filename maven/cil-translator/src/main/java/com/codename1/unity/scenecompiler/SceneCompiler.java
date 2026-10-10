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
package com.codename1.unity.scenecompiler;

import com.codename1.cil.metadata.CilAssembly;
import com.codename1.cil.metadata.CilAssembly.FieldDef;
import com.codename1.cil.metadata.CilAssembly.MethodDef;
import com.codename1.cil.metadata.CilAssembly.TypeDef;
import com.codename1.cil.metadata.CilType;
import com.codename1.cil.metadata.Universe;
import com.codename1.cil.translate.TypeMapping;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/// Turns the scenes and prefabs of a Unity project into Java source that
/// builds them.
///
/// Unity loads a scene by reading its file and creating each object by
/// reflection: the script class from a GUID, each serialized field by name.
/// A Codename One application has no reflection to do that with, so the same
/// work is done here, at build time, and what ships is
/// `com.codename1.generated.unity.UnityAppImpl` -- one method per scene and
/// one per prefab, made of `new`, field stores and method calls a compiler
/// checks. The class implements `UnityRuntime.App`, which is how the runtime
/// reaches them.
///
/// #### What it reads
///
/// - The scenes of `ProjectSettings/EditorBuildSettings.asset`, in that
///   order, which is the order of their build indexes. A project with no
///   such list gets every `.unity` file under `Assets`, by path.
/// - The `.cs.meta` beside each script, for the GUID a scene names it by.
///   A script's class is found by the name of its file, which is Unity's
///   own rule. The compiled scripts are read for the types of their
///   fields, never executed.
/// - The `.meta` beside each image, whose `TextureImporter` says whether it
///   is one sprite or a sheet of them, how many pixels make a world unit
///   and where each pivot is. The image itself is opened only for its
///   size, so that the generated code can say
///   `new Sprite("ball.png", 32, 32, ...)` and the running application
///   never has to load an image to know where to draw it.
/// - The `.prefab` files a script field refers to, and those they refer to
///   in turn. Each becomes a method that builds a fresh copy -- which is
///   what `Instantiate` calls -- and a template built once for scripts to
///   hold.
/// - `ProjectSettings`: 2D physics (gravity, iterations, the layer
///   collision matrix), the fixed timestep, the sorting layers and the
///   input axes.
///
/// #### Unity's own sprites
///
/// A sprite of Unity's built-in resources -- the *Knob* a new 2D sprite
/// object gets -- is not in the project, and Unity's image is Unity's. One
/// is drawn here instead, white so that the renderer's colour tints it as
/// it would the original: a disc for the knob, a plain square for any
/// other, at the size the renderer records.
///
/// An image ships as a resource under its own file name; with
/// `--resources` the ones used are copied to a directory to be packaged
/// from.
public final class SceneCompiler {
    private static final String RUNTIME = "com.codename1.unitycompat.unityengine.";
    private static final String BUILTIN_EXTRA = "0000000000000000f000000000000000";
    private static final String BUILTIN_DEFAULT = "0000000000000000e000000000000000";

    private final Universe universe = new Universe();
    private TypeMapping types;
    private List<CilAssembly> scripts;
    private File project;
    private final Map<String, TypeDef> scriptsByGuid = new HashMap<String, TypeDef>();
    private final Map<String, TextureAsset> texturesByGuid = new HashMap<String, TextureAsset>();
    private final Map<String, File> prefabFilesByGuid = new HashMap<String, File>();
    /// Physics materials by GUID: friction, then bounciness, as source.
    private final Map<String, String[]> materialsByGuid = new HashMap<String, String[]>();
    private final Map<String, File> audioByGuid = new HashMap<String, File>();
    private final Map<String, File> textByGuid = new HashMap<String, File>();
    /// Animator controllers, animation clips, the `.asset` files a tile is
    /// one of, and render materials, each by GUID.
    private final Map<String, File> controllerFilesByGuid = new HashMap<String, File>();
    private final Map<String, File> animationFilesByGuid = new HashMap<String, File>();
    private final Map<String, File> assetFilesByGuid = new HashMap<String, File>();
    private final Map<String, File> renderMaterialsByGuid = new HashMap<String, File>();
    /// Assets built so far: a key to the method that returns each.
    private final Map<String, String> assetMethods = new LinkedHashMap<String, String>();
    /// How each tile asset collides, by the method that returns it.
    private final Map<String, String> tileColliders = new HashMap<String, String>();
    /// The solid pixels of the sprites tiles collide as: a sprite's key to
    /// the constant that holds them, or to an empty string for a sprite
    /// that is solid all over.
    private final Map<String, String> solidConstants = new HashMap<String, String>();
    private final Map<File, BufferedImage> imagesRead = new HashMap<File, BufferedImage>();
    private final java.util.Set<String> saidOnce = new java.util.HashSet<String>();
    /// Text assets used so far: a file to the method that returns it.
    private final Map<File, String> textMethods = new LinkedHashMap<File, String>();
    /// Every file under a folder named `Resources`.
    private final List<ResourceFile> resourceFiles = new ArrayList<ResourceFile>();
    /// Audio clips used so far: a GUID to the method that returns it.
    private final Map<String, String> clipMethods = new LinkedHashMap<String, String>();
    /// Whether the object whose sprite is being resolved has a circle
    /// collider, which decides the shape drawn for a sprite that is not in
    /// the project.
    private boolean roundOwner;
    private final Map<String, Prefab> prefabsByGuid = new LinkedHashMap<String, Prefab>();
    private final List<Prefab> prefabs = new ArrayList<Prefab>();
    /// Resource name to the image shipped under it, for the sprites used.
    private final Map<String, File> usedImages = new LinkedHashMap<String, File>();
    private final Map<String, byte[]> generatedImages = new LinkedHashMap<String, byte[]>();
    private final List<String> warnings = new ArrayList<String>();
    private final List<String> notes = new ArrayList<String>();
    /// For each file, the name of the GameObject every document is or is
    /// on, by file ID: what a warning says beside an ID, since an ID is
    /// nothing a person can find in the editor.
    private final Map<String, Map<Long, String>> objectNames = new HashMap<String, Map<Long, String>>();
    /// The same for the components of each file: the name of the
    /// GameObject each is on.
    private final Map<String, Map<Long, String>> ownerNames = new HashMap<String, Map<Long, String>>();
    /// For each file, the components that were left out, by file ID, and
    /// what each was: a reference to one becomes null, with a word.
    private final Map<String, Map<Long, String>> dropped = new HashMap<String, Map<Long, String>>();
    /// For each file, what the Java variable of a script component is in
    /// the editor's terms, so that a warning about `c1234.speed` can say
    /// whose `speed` it is.
    private final Map<String, Map<String, String>> variableLabels = new HashMap<String, Map<String, String>>();
    /// Sprites used so far: a key to the name of the method that returns it.
    private final Map<String, String> spriteMethods = new LinkedHashMap<String, String>();
    // One compiler lives for one run and this is its output.
    @SuppressWarnings("PMD.AvoidStringBufferField")
    private final StringBuilder members = new StringBuilder();
    private int constants;
    private int temporaries;

    /// One sprite of an image.
    private static final class SpriteAsset {
        String name;
        int x;
        int y;
        int width;
        int height;
        String pivotX;
        String pivotY;
        /// The physics shape drawn for it in the sprite editor, as the
        /// importer records it; empty for the one Unity makes itself.
        Object physicsShape;
    }

    /// An image, as its `.meta` describes it.
    private static final class TextureAsset {
        File image;
        String resource;
        /// The name the image is shipped under, once a sprite uses it.
        String shipped;
        int width;
        int height;
        String pixelsPerUnit;
        /// The sprite of a single-sprite image; null for a sheet.
        SpriteAsset single;
        final Map<String, SpriteAsset> byFileId = new HashMap<String, SpriteAsset>();
        /// Why it cannot be used, or null.
        String unusable;
        /// For a polygon sprite whose outline cuts pixels away: the
        /// paths, each x then y of every point, in pixels from the centre
        /// of the image with y up.
        List<double[]> outline;
        /// The image drawn from `outline`, once made: its resource name
        /// and how many of its pixels make one of the original's.
        String cutResource;
        int cutScale;
    }

    /// A file `Resources.Load` can be asked for.
    private static final class ResourceFile implements Comparable<ResourceFile> {
        /// Below its `Resources` folder, without the extension.
        String path;
        File file;

        @Override
        public int compareTo(ResourceFile other) {
            int byPath = path.compareTo(other.path);
            return byPath != 0 ? byPath : file.compareTo(other.file);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ResourceFile && file.equals(((ResourceFile) o).file);
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }
    }

    private static final class Prefab {
        int index;
        File file;
        String label;
        Map<Long, Node> nodes;
        List<Node> order;
        Node root;
        Map<Long, UnityYaml.Document> meshRenderers;
    }

    public static void main(String[] args) throws IOException {
        File project = null;
        File out = null;
        File resources = null;
        List<File> assemblies = new ArrayList<File>();
        List<File> references = new ArrayList<File>();
        for (int i = 0; i < args.length; i++) {
            if ("--project".equals(args[i])) {
                project = new File(args[++i]);
            } else if ("--out".equals(args[i])) {
                out = new File(args[++i]);
            } else if ("--resources".equals(args[i])) {
                resources = new File(args[++i]);
            } else if ("--ref".equals(args[i])) {
                references.add(new File(args[++i]));
            } else {
                assemblies.add(new File(args[i]));
            }
        }
        if (project == null || out == null || assemblies.isEmpty()) {
            System.err.println("usage: SceneCompiler --project <unity project dir> --out <java source dir>"
                    + " [--resources <dir to copy the sprites' images to>]"
                    + " [--ref <assembly>]... <translated assembly>...");
            System.exit(2);
        }
        SceneCompiler c = new SceneCompiler();
        String source;
        try {
            source = c.compile(project, assemblies, references);
        } catch (SceneException e) {
            System.err.println("error: " + e.getMessage());
            System.exit(1);
            return;
        }
        File file = new File(out, "com/codename1/generated/unity/UnityAppImpl.java");
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(), source.getBytes(StandardCharsets.US_ASCII));
        if (resources != null) {
            Files.createDirectories(resources.toPath());
            for (Map.Entry<String, File> e : c.usedImages.entrySet()) {
                Files.write(new File(resources, e.getKey()).toPath(), Files.readAllBytes(e.getValue().toPath()));
            }
            for (Map.Entry<String, byte[]> e : c.generatedImages.entrySet()) {
                Files.write(new File(resources, e.getKey()).toPath(), e.getValue());
            }
        }
        for (String n : c.notes) {
            System.err.println("note: " + n);
        }
        for (String w : c.warnings) {
            System.err.println("warning: " + w);
        }
        System.out.println("wrote " + file);
    }

    /// A scene that cannot be compiled: it names a script or a field that
    /// does not exist, or uses something this compiler does not read.
    public static final class SceneException extends RuntimeException {
        SceneException(String message) {
            super(message);
        }

        SceneException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /// What was left out or approximated, and changes how the project
    /// behaves.
    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    /// What was left out and changes nothing a player can see.
    public List<String> notes() {
        return Collections.unmodifiableList(notes);
    }

    /// The images the compiled scenes draw, by the resource name the
    /// generated code loads each one under.
    public Map<String, File> usedImages() {
        return Collections.unmodifiableMap(usedImages);
    }

    /// The images drawn here in place of Unity's built-in ones, as PNG
    /// files by resource name.
    public Map<String, byte[]> generatedImages() {
        return Collections.unmodifiableMap(generatedImages);
    }

    /// `assemblies` are the translated ones, scripts first; `references`
    /// whatever else they were compiled against.
    public String compile(File project, List<File> assemblies, List<File> references) throws IOException {
        this.project = project;
        List<File> all = new ArrayList<File>(assemblies);
        all.addAll(references);
        List<CilAssembly> loaded = universe.load(all);
        scripts = loaded.subList(0, assemblies.size());
        types = new TypeMapping(universe, scripts);
        File assets = new File(project, "Assets");
        List<File> found = new ArrayList<File>();
        walk(assets, found, null);
        Collections.sort(found);
        List<File> scenes = buildScenes(found);

        StringBuilder methods = new StringBuilder();
        for (int i = 0; i < scenes.size(); i++) {
            String text = new String(Files.readAllBytes(scenes.get(i).toPath()), StandardCharsets.UTF_8);
            methods.append("\n    /// ").append(javaComment(relative(project, scenes.get(i)))).append("\n");
            methods.append("    public static void buildScene_").append(i).append("() {\n");
            Graph g = graph(scenes.get(i).getName(), UnityYaml.parse(text));
            emit(g, methods, false);
            methods.append("    }\n");
        }
        String lookup = resources();
        // A prefab may name another, so the list grows while it is walked.
        for (int i = 0; i < prefabs.size(); i++) { // NOPMD ForLoopCanBeForeach
            emitPrefab(prefabs.get(i), methods);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("// Generated by SceneCompiler from the scenes of a Unity project. Do not edit.\n");
        sb.append("package com.codename1.generated.unity;\n\n");
        sb.append("public final class UnityAppImpl implements " + RUNTIME + "UnityRuntime.App {\n");
        sb.append("    public static final int SCENE_COUNT = ").append(scenes.size()).append(";\n\n");
        sb.append("    private UnityAppImpl() {\n    }\n\n");
        sb.append("    /// Makes this project the one the runtime runs.\n");
        sb.append("    public static void install() {\n");
        sb.append("        " + RUNTIME + "UnityRuntime.$install(new UnityAppImpl());\n    }\n\n");
        sb.append("    public int sceneCount() {\n        return SCENE_COUNT;\n    }\n\n");
        sb.append("    public String scenePath(int index) {\n        switch (index) {\n");
        for (int i = 0; i < scenes.size(); i++) {
            sb.append("            case ").append(i).append(":\n                return ")
                    .append(string(relative(project, scenes.get(i)))).append(";\n");
        }
        sb.append("            default:\n                throw new IllegalArgumentException(\"scene \" + index);\n");
        sb.append("        }\n    }\n\n");
        sb.append("    public void buildScene(int index) {\n        switch (index) {\n");
        for (int i = 0; i < scenes.size(); i++) {
            sb.append("            case ").append(i).append(":\n                buildScene_").append(i)
                    .append("();\n                return;\n");
        }
        sb.append("            default:\n                throw new IllegalArgumentException(\"scene \" + index);\n");
        sb.append("        }\n    }\n\n");
        sb.append("    public " + RUNTIME + "GameObject instantiatePrefab(int index) {\n        switch (index) {\n");
        for (Prefab p : prefabs) {
            sb.append("            case ").append(p.index).append(":\n                return buildPrefab_")
                    .append(p.index).append("(null);\n");
        }
        sb.append("            default:\n                throw new IllegalArgumentException(\"prefab \" + index);\n");
        sb.append("        }\n    }\n\n");
        sb.append("    public " + RUNTIME + "Component newComponent(Class type) {\n");
        for (CilAssembly assembly : scripts) {
            for (TypeDef t : assembly.types()) {
                if (t.isAbstract() || t.isInterface() || t.genericParamCount > 0 || t.enclosing != null
                        || "UnityEngine.MonoBehaviour".equals(t.fullName())
                        || !universe.derivesFrom(t.fullName(), "UnityEngine.MonoBehaviour") || !hasConstructor(t)) {
                    continue;
                }
                String c = types.javaClass(t.asType());
                sb.append("        if (type == ").append(c).append(".class) {\n            return new ").append(c)
                        .append("();\n        }\n");
            }
        }
        sb.append("        return null;\n    }\n\n");
        sb.append("    public java.lang.Object loadResource(String path, Class type) {\n");
        sb.append(lookup);
        sb.append("        return null;\n    }\n\n");
        sb.append("    public void settings() {\n");
        settings(sb);
        sb.append("    }\n");
        sb.append(members);
        sb.append(methods);
        sb.append("}\n");
        return sb.toString();
    }

    private static boolean hasConstructor(TypeDef t) {
        for (MethodDef m : t.methods) {
            if (".ctor".equals(m.name) && m.sig.params.length == 0) {
                return true;
            }
        }
        return false;
    }

    private static String relative(File base, File file) {
        String b = base.getAbsolutePath();
        String f = file.getAbsolutePath();
        return f.startsWith(b) ? f.substring(b.length() + 1).replace('\\', '/') : file.getName();
    }

    private static String javaComment(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c >= ' ' && c < 127 && c != '\\' ? c : '?');
        }
        return sb.toString();
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    /// The extensions Unity imports as a `TextAsset`.
    private static boolean isText(String lowerName) {
        final String[] extensions = {".txt", ".html", ".htm", ".xml", ".bytes", ".json", ".csv", ".yaml", ".fnt"};
        for (String e : extensions) {
            if (lowerName.endsWith(e)) {
                return true;
            }
        }
        return false;
    }

    /// `resources` is the path of `dir` below the `Resources` folder it is
    /// in, ending in a slash unless it is that folder, or null outside one.
    private void walk(File dir, List<File> scenes, String resources) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        // The order a directory is listed in is the file system's own.
        java.util.Arrays.sort(children);
        for (File f : children) {
            String name = f.getName();
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            if (resources != null && f.isFile() && !lower.endsWith(".meta") && name.lastIndexOf('.') > 0) {
                ResourceFile r = new ResourceFile();
                r.path = resources + name.substring(0, name.lastIndexOf('.'));
                r.file = f;
                resourceFiles.add(r);
            }
            if (f.isDirectory()) {
                walk(f, scenes, "Resources".equals(name) ? "" : resources == null ? null : resources + name + "/");
            } else if (lower.endsWith(".meta") && isText(lower.substring(0, lower.length() - ".meta".length()))) {
                String guid = text(UnityYaml.parseMeta(read(f)).get("guid"));
                if (guid.length() > 0) {
                    textByGuid.put(guid, new File(dir, name.substring(0, name.length() - ".meta".length())));
                }
            } else if (name.endsWith(".unity")) {
                scenes.add(f);
            } else if (name.endsWith(".cs.meta")) {
                indexScript(f);
            } else if (lower.endsWith(".png.meta") || lower.endsWith(".jpg.meta") || lower.endsWith(".jpeg.meta")) {
                indexTexture(f);
            } else if (lower.endsWith(".physicsmaterial2d.meta")) {
                indexMaterial(f);
            } else if (lower.endsWith(".wav.meta") || lower.endsWith(".ogg.meta") || lower.endsWith(".mp3.meta")) {
                String guid = text(UnityYaml.parseMeta(read(f)).get("guid"));
                if (guid.length() > 0) {
                    audioByGuid.put(guid, new File(dir, name.substring(0, name.length() - ".meta".length())));
                }
            } else if (lower.endsWith(".controller.meta")) {
                indexFile(controllerFilesByGuid, f);
            } else if (lower.endsWith(".anim.meta")) {
                indexFile(animationFilesByGuid, f);
            } else if (lower.endsWith(".asset.meta")) {
                indexFile(assetFilesByGuid, f);
            } else if (lower.endsWith(".mat.meta")) {
                indexFile(renderMaterialsByGuid, f);
            } else if (name.endsWith(".prefab.meta")) {
                String guid = text(UnityYaml.parseMeta(read(f)).get("guid"));
                if (guid.length() > 0) {
                    prefabFilesByGuid.put(guid, new File(dir, name.substring(0, name.length() - ".meta".length())));
                }
            }
        }
    }

    private static void indexFile(Map<String, File> byGuid, File meta) throws IOException {
        String guid = text(UnityYaml.parseMeta(read(meta)).get("guid"));
        if (guid.length() > 0) {
            byGuid.put(guid, new File(meta.getParentFile(), meta.getName().substring(0, meta.getName().length()
                    - ".meta".length())));
        }
    }

    /// The scenes that make the build, in the order of their indexes.
    private List<File> buildScenes(List<File> found) throws IOException {
        File settings = new File(project, "ProjectSettings/EditorBuildSettings.asset");
        if (!settings.isFile()) {
            return found;
        }
        List<File> listed = new ArrayList<File>();
        for (UnityYaml.Document d : UnityYaml.parse(read(settings))) {
            if (d.properties == null) {
                continue;
            }
            for (Object entry : list(d.properties.get("m_Scenes"))) {
                Map<String, Object> scene = map(entry);
                File f = new File(project, text(scene.get("path")));
                if ("0".equals(text(scene.get("enabled")))) {
                    continue;
                }
                if (f.isFile()) {
                    listed.add(f);
                } else {
                    warnings.add("the build settings list the scene " + text(scene.get("path"))
                            + ", which is not in the project; it was left out");
                }
            }
        }
        if (listed.isEmpty()) {
            if (!found.isEmpty()) {
                notes.add("the build settings list no scene; every scene under Assets was compiled, in order of"
                        + " path");
            }
            return found;
        }
        return listed;
    }

    /// A physics material is a file of one document with two numbers.
    private void indexMaterial(File meta) throws IOException {
        String guid = text(UnityYaml.parseMeta(read(meta)).get("guid"));
        File asset = new File(meta.getParentFile(), meta.getName().substring(0, meta.getName().length()
                - ".meta".length()));
        if (guid.length() == 0 || !asset.isFile()) {
            return;
        }
        for (UnityYaml.Document d : UnityYaml.parse(read(asset))) {
            if (d.properties != null && d.classId == 62) {
                materialsByGuid.put(guid, new String[] {
                        f(d.properties.get("friction"), "0.4"), f(d.properties.get("bounciness"), "0"),
                });
            }
        }
    }

    /// The friction and bounciness of the material a reference names, or
    /// null when it names none.
    private String[] material(String origin, String user, Object reference) {
        String guid = text(map(reference).get("guid"));
        if (guid.length() == 0) {
            if (fileId(reference).longValue() != 0) {
                warnings.add(origin + ": " + user + " has a physics material that is not an asset of the project;"
                        + " the default friction and bounciness are used");
            }
            return null;
        }
        String[] m = materialsByGuid.get(guid);
        if (m == null) {
            warnings.add(origin + ": " + user + " has the physics material with GUID " + guid + ", which is not"
                    + " under Assets; the default friction and bounciness are used");
        }
        return m;
    }

    // ---------------------------------------------------------------- text

    /// An expression for the `TextAsset` a file becomes. Its contents go
    /// into the generated source as string constants, which is what lets a
    /// script read a level with nothing to open: no file ships.
    ///
    /// A constant holds 65535 bytes at most, so a long text is put together
    /// from pieces the first time it is asked for. A `.bytes` file is kept
    /// byte for byte, a character to a byte.
    private String textAsset(File file) {
        String method = textMethods.get(file);
        if (method != null) {
            return method + "()";
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new SceneException(file + ": " + e, e);
        }
        String name = file.getName();
        boolean binary = name.toLowerCase(java.util.Locale.ROOT).endsWith(".bytes");
        name = name.substring(0, name.lastIndexOf('.'));
        String contents;
        if (binary) {
            contents = new String(bytes, StandardCharsets.ISO_8859_1);
        } else {
            contents = new String(bytes, StandardCharsets.UTF_8);
            if (contents.length() > 0 && contents.charAt(0) == 0xfeff) {
                contents = contents.substring(1);
            }
        }
        method = "text_" + textMethods.size();
        textMethods.put(file, method);
        String type = RUNTIME + "TextAsset";
        final int piece = 8000;
        StringBuilder value = new StringBuilder();
        if (contents.length() <= piece) {
            value.append(string(contents));
        } else {
            value.append("new StringBuilder(").append(contents.length()).append(')');
            for (int at = 0; at < contents.length(); at += piece) {
                value.append("\n                    .append(")
                        .append(string(contents.substring(at, Math.min(contents.length(), at + piece)))).append(')');
            }
            value.append(".toString()");
        }
        members.append("\n    /// ").append(javaComment(relative(project, file))).append("\n");
        members.append("    private static ").append(type).append(' ').append(method).append(";\n\n");
        members.append("    private static ").append(type).append(' ').append(method).append("() {\n");
        members.append("        if (").append(method).append(" == null) {\n");
        members.append("            ").append(method).append(" = ").append(binary ? type + ".$bytes(" : "new " + type + "(")
                .append(string(name)).append(", ").append(value).append(");\n");
        members.append("        }\n        return ").append(method).append(";\n    }\n");
        return method + "()";
    }

    /// The GUID a `.meta` beside a file gives it, or "".
    private static String guidOf(File file) {
        File meta = new File(file.getParentFile(), file.getName() + ".meta");
        if (!meta.isFile()) {
            return "";
        }
        try {
            return text(UnityYaml.parseMeta(read(meta)).get("guid"));
        } catch (IOException e) {
            throw new SceneException(meta + ": " + e, e);
        }
    }

    /// The body of `loadResource`: one comparison for each file under a
    /// `Resources` folder, in the order of their paths.
    ///
    /// Unity finds these by name while the game runs. Here the files are
    /// all known now, so the lookup is code and what it returns is built
    /// by code: nothing is scanned or opened on a device, and an asset no
    /// script asks for costs a string constant.
    private String resources() {
        Collections.sort(resourceFiles);
        StringBuilder sb = new StringBuilder();
        for (ResourceFile r : resourceFiles) {
            String origin = relative(project, r.file);
            String user = "Resources.Load(\"" + r.path + "\")";
            String lower = r.file.getName().toLowerCase(java.util.Locale.ROOT);
            String expression;
            String type;
            if (isText(lower)) {
                expression = textAsset(r.file);
                type = "TextAsset";
            } else if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                Map<String, Object> reference = new HashMap<String, Object>();
                reference.put("guid", guidOf(r.file));
                reference.put("fileID", "");
                TextureAsset a = texturesByGuid.get(guidOf(r.file));
                if (a == null || a.unusable != null || a.single == null) {
                    notes.add(origin + " is under a Resources folder and is not an image imported as one sprite;"
                            + " Resources.Load does not find it");
                    continue;
                }
                expression = sprite(origin, user, reference, null);
                type = "Sprite";
            } else if (lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".mp3")) {
                Map<String, Object> reference = new HashMap<String, Object>();
                reference.put("guid", guidOf(r.file));
                expression = clip(origin, user, reference);
                type = "AudioClip";
            } else if (lower.endsWith(".prefab")) {
                String guid = guidOf(r.file);
                Prefab prefab = guid.length() == 0 ? null : prefab(origin, user, guid);
                expression = prefab == null ? null : "prefabPart_" + prefab.index + "(" + prefab.root.part + ")";
                type = "GameObject";
            } else {
                notes.add(origin + " is under a Resources folder and is of a kind that is not supported;"
                        + " Resources.Load does not find it");
                continue;
            }
            if (expression == null) {
                continue;
            }
            String c = RUNTIME + type;
            sb.append("        if (").append(string(r.path)).append(".equalsIgnoreCase(path) && (type == null || type == ")
                    .append(c).append(".class || type == ").append(RUNTIME)
                    .append("Object.class || type == java.lang.Object.class)) {\n");
            sb.append("            return ").append(expression).append(";\n        }\n");
        }
        return sb.toString();
    }

    /// An expression for the audio clip a `{fileID, guid}` names, or null
    /// with a warning. The clip's length, which scripts ask for and which
    /// decides how long a source says it is playing, is read from the file
    /// here so that nothing has to decode audio to answer.
    private String clip(String origin, String user, Object reference) {
        String guid = text(map(reference).get("guid"));
        if (guid.length() == 0) {
            return null;
        }
        String method = clipMethods.get(guid);
        if (method != null) {
            return method + "()";
        }
        File file = audioByGuid.get(guid);
        if (file == null || !file.isFile()) {
            warnings.add(origin + ": " + user + " uses the audio clip with GUID " + guid + ", and no audio file"
                    + " under Assets has it; it was left null");
            return null;
        }
        long[] info;
        try {
            info = audioInfo(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new SceneException(file + ": " + e, e);
        }
        if (info == null) {
            notes.add(origin + ": the length of " + file.getName() + " could not be read from its header; the clip"
                    + " reports a length of zero");
            info = new long[] {0, 1, 44100};
        }
        File shipped = usedImages.get(file.getName());
        if (shipped != null && !shipped.equals(file)) {
            warnings.add(origin + ": two files are named " + file.getName() + "; resources have no folders, so"
                    + " both clips will play " + relativeName(shipped));
        } else {
            usedImages.put(file.getName(), file);
        }
        String name = file.getName();
        name = name.substring(0, name.lastIndexOf('.'));
        method = "clip_" + clipMethods.size();
        clipMethods.put(guid, method);
        String type = RUNTIME + "AudioClip";
        float seconds = info[2] <= 0 ? 0 : (float) ((double) info[0] / (double) info[2]);
        members.append("\n    private static ").append(type).append(' ').append(method).append(";\n\n");
        members.append("    private static ").append(type).append(' ').append(method).append("() {\n");
        members.append("        if (").append(method).append(" == null) {\n");
        members.append("            ").append(method).append(" = new ").append(type).append('(').append(string(name))
                .append(", ").append(string(file.getName())).append(", ").append(Float.toString(seconds))
                .append("f, ").append(info[0]).append(", ").append(info[1]).append(", ").append(info[2])
                .append(");\n");
        members.append("        }\n        return ").append(method).append(";\n    }\n");
        return method + "()";
    }

    private static long le(byte[] b, int at, int bytes) {
        long v = 0;
        for (int i = bytes - 1; i >= 0; i--) {
            v = (v << 8) | (b[at + i] & 0xff);
        }
        return v;
    }

    private static boolean tag(byte[] b, int at, String tag) {
        if (at < 0 || at + tag.length() > b.length) {
            return false;
        }
        for (int i = 0; i < tag.length(); i++) {
            if (b[at + i] != (byte) tag.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /// Sample frames, channels and sample rate of a WAV or an Ogg Vorbis
    /// file, from its headers; null for anything else.
    static long[] audioInfo(byte[] b) {
        if (tag(b, 0, "RIFF") && tag(b, 8, "WAVE")) {
            // A run of chunks: `fmt ` describes the samples, `data` holds
            // them, and each is a four letter tag, a length and a body
            // padded to an even size.
            long channels = 0;
            long rate = 0;
            long align = 0;
            int at = 12;
            while (at + 8 <= b.length) {
                long size = le(b, at + 4, 4);
                if (tag(b, at, "fmt ") && at + 24 <= b.length) {
                    channels = le(b, at + 10, 2);
                    rate = le(b, at + 12, 4);
                    align = le(b, at + 20, 2);
                } else if (tag(b, at, "data")) {
                    long bytes = Math.min(size, b.length - at - 8);
                    return align <= 0 || rate <= 0 ? null : new long[] {bytes / align, channels, rate};
                }
                long next = at + 8L + size + (size & 1);
                if (next <= at || next > b.length) {
                    break;
                }
                at = (int) next;
            }
            return null;
        }
        if (tag(b, 0, "OggS")) {
            // The first packet names the channels and the rate; the last
            // page of the stream counts the samples up to its end.
            int id = -1;
            for (int i = 0; i + 7 < Math.min(b.length, 4096); i++) {
                if (b[i] == 1 && tag(b, i + 1, "vorbis")) {
                    id = i;
                    break;
                }
            }
            if (id < 0 || id + 16 > b.length) {
                return null;
            }
            long channels = b[id + 11] & 0xff;
            long rate = le(b, id + 12, 4);
            for (int i = b.length - 14; i >= 0; i--) {
                if (tag(b, i, "OggS")) {
                    long frames = le(b, i + 6, 8);
                    return rate <= 0 || frames < 0 ? null : new long[] {frames, channels, rate};
                }
            }
        }
        return null;
    }

    /// Maps a script's GUID to the class its file declares.
    ///
    /// Unity knows a script by its file and names the class after the file,
    /// so the class is found by that name. A name can be had by several
    /// classes, though: `Player.cs` under two folders with a namespace
    /// each, or a second `Player` declared in some other file. Then the
    /// compiler's debug information says which file each was written in,
    /// and the one written in this file is taken. Without it -- no PDB, or
    /// a class with no method of its own to carry a source position --
    /// the first is taken, as it always was, and the build says so.
    private void indexScript(File meta) throws IOException {
        String guid = text(UnityYaml.parseMeta(read(meta)).get("guid"));
        if (guid.length() == 0) {
            return;
        }
        String simple = meta.getName().substring(0, meta.getName().length() - ".cs.meta".length());
        List<TypeDef> named = new ArrayList<TypeDef>();
        for (CilAssembly assembly : scripts) {
            for (TypeDef t : assembly.types()) {
                String full = t.fullName();
                if (full.equals(simple) || full.endsWith("." + simple)) {
                    named.add(t);
                }
            }
        }
        if (named.isEmpty()) {
            notes.add(meta.getName() + ": no compiled class is named " + simple);
            return;
        }
        TypeDef script = named.get(0);
        if (named.size() > 1) {
            File source = new File(meta.getParentFile(), simple + ".cs");
            TypeDef written = writtenIn(named, source);
            if (written != null) {
                script = written;
            } else {
                StringBuilder all = new StringBuilder();
                for (TypeDef t : named) {
                    all.append(all.length() == 0 ? "" : ", ").append(t.fullName());
                }
                warnings.add(relative(project, source).replace(File.separatorChar, '/') + ": " + named.size() + " classes are named " + simple + " (" + all
                        + ") and the debug information of the scripts does not say which of them this file"
                        + " declares; its components were given " + script.fullName());
            }
        }
        scriptsByGuid.put(guid, script);
    }

    /// Of several classes, the one whose source file is `source`: the one
    /// the debug information places in a file whose path ends in this
    /// one's path below the project, `Assets/Game/Player.cs`. Not the whole
    /// path, since the compiler may have been given the project under
    /// another root or told to rewrite it; and not the file's name alone,
    /// which is what the classes have in common. Null when none is there,
    /// or more than one.
    private TypeDef writtenIn(List<TypeDef> named, File source) {
        String wanted = relative(project, source).replace(File.separatorChar, '/');
        TypeDef found = null;
        for (TypeDef t : named) {
            String document = t.assembly.sourceFile(t);
            if (document == null || !(document.equals(wanted) || document.endsWith("/" + wanted))) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = t;
        }
        return found;
    }

    /// SpriteAlignment: 0 centre, 1 top left, 2 top, 3 top right, 4 left,
    /// 5 right, 6 bottom left, 7 bottom, 8 bottom right, 9 custom -- which
    /// is the `pivot` pair beside it.
    private static void pivot(SpriteAsset s, Object alignmentValue, Object pivotValue) {
        final String[] xs = {"0.5", "0", "0.5", "1", "0", "1", "0", "0.5", "1"};
        final String[] ys = {"0.5", "1", "1", "1", "0.5", "0.5", "0", "0", "0"};
        int alignment = (int) Long.parseLong(integer(alignmentValue, "0"));
        if (alignment >= 0 && alignment < xs.length) {
            s.pivotX = xs[alignment] + "f";
            s.pivotY = ys[alignment] + "f";
        } else {
            Map<String, Object> pivot = map(pivotValue);
            s.pivotX = f(pivot.get("x"), "0.5");
            s.pivotY = f(pivot.get("y"), "0.5");
        }
    }

    /// Reads the import settings of one image: the mode, the pixels per
    /// unit, and either the one pivot or, for a sheet, each sprite's
    /// rectangle and pivot and the id scenes know it by.
    /// Whether a polygon sprite's outline is the rectangle of its image.
    ///
    /// A sprite imported in *Polygon* mode is drawn as a mesh cut to an
    /// outline, which the `.meta` lists in pixels from the image's centre.
    /// The editor's four-sided polygon is the image's own four corners,
    /// and such a sprite is drawn exactly as a single one is; any other
    /// outline cuts pixels away, which nothing here does.
    private static boolean wholeOutline(Object outline, int width, int height) {
        List<Object> paths = list(outline);
        if (paths.size() != 1 || list(paths.get(0)).size() != 4) {
            return false;
        }
        int corners = 0;
        for (Object point : list(paths.get(0))) {
            try {
                double x = Double.parseDouble(number(map(point).get("x"), "0"));
                double y = Double.parseDouble(number(map(point).get("y"), "0"));
                if (Math.abs(Math.abs(x) * 2 - width) > 0.01 || Math.abs(Math.abs(y) * 2 - height) > 0.01) {
                    return false;
                }
                corners |= 1 << ((x > 0 ? 1 : 0) + (y > 0 ? 2 : 0));
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return corners == 15;
    }

    /// The paths of a polygon sprite's outline, or null when there is not
    /// one with at least three points.
    private static List<double[]> outlinePaths(Object outline) {
        List<double[]> paths = new ArrayList<double[]>();
        for (Object path : list(outline)) {
            List<Object> points = list(path);
            if (points.size() < 3) {
                continue;
            }
            double[] xy = new double[points.size() * 2];
            for (int i = 0; i < points.size(); i++) {
                try {
                    xy[i * 2] = Double.parseDouble(number(map(points.get(i)).get("x"), "0"));
                    xy[i * 2 + 1] = Double.parseDouble(number(map(points.get(i)).get("y"), "0"));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            paths.add(xy);
        }
        return paths.isEmpty() ? null : paths;
    }

    /// Cuts a polygon sprite to its outline, now, into an image of its own.
    ///
    /// Unity draws such a sprite as a mesh: the triangles of the outline,
    /// textured with the image. What paints here draws rectangles of
    /// images, so the cutting is done once at build time -- the image is
    /// drawn through the outline and everything outside it is left
    /// transparent. An outline is in fractions of a pixel and the image
    /// under it may be a few pixels across (a white square is all a tinted
    /// shape needs), so the result is made larger, up to 256 pixels on its
    /// longer side, and the sprite's pixels per unit grow by the same
    /// factor: it covers the same world units, with an edge that is smooth.
    private void cutToOutline(TextureAsset a) {
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true");
        }
        int scale = Math.max(1, 256 / Math.max(a.width, a.height));
        int w = a.width * scale;
        int h = a.height * scale;
        BufferedImage source;
        try {
            source = ImageIO.read(a.image);
        } catch (IOException e) {
            throw new SceneException(a.image + ": " + e, e);
        }
        if (source == null) {
            throw new SceneException(a.image + ": the image of a polygon sprite could not be read");
        }
        java.awt.geom.Path2D.Double shape = new java.awt.geom.Path2D.Double(java.awt.geom.Path2D.WIND_EVEN_ODD);
        for (double[] path : a.outline) {
            for (int i = 0; i < path.length; i += 2) {
                // From the centre with y up, to the top left with y down.
                double x = (path[i] + a.width / 2.0) * scale;
                double y = (a.height / 2.0 - path[i + 1]) * scale;
                if (i == 0) {
                    shape.moveTo(x, y);
                } else {
                    shape.lineTo(x, y);
                }
            }
            shape.closePath();
        }
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fill(shape);
        // Where the outline is, and nowhere else, the image.
        g.setComposite(java.awt.AlphaComposite.SrcIn);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, w, h, null);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new SceneException("could not draw the polygon sprite " + a.resource + ": " + e, e);
        }
        String base = a.resource.substring(0, a.resource.lastIndexOf('.'));
        String resource = "unity-polygon-" + base + ".png";
        for (int n = 2; generatedImages.containsKey(resource) || usedImages.containsKey(resource); n++) {
            resource = "unity-polygon-" + base + "-" + n + ".png";
        }
        generatedImages.put(resource, out.toByteArray());
        a.cutResource = resource;
        a.cutScale = scale;
        notes.add(a.resource + " is a polygon sprite: it was cut to its outline when the project was built, into "
                + resource + " (" + w + " by " + h + " pixels), and is drawn as that image");
    }

    private void indexTexture(File meta) throws IOException {
        Map<String, Object> root = UnityYaml.parseMeta(read(meta));
        String guid = text(root.get("guid"));
        if (guid.length() == 0) {
            return;
        }
        Map<String, Object> importer = map(root.get("TextureImporter"));
        TextureAsset a = new TextureAsset();
        a.resource = meta.getName().substring(0, meta.getName().length() - ".meta".length());
        a.image = new File(meta.getParentFile(), a.resource);
        texturesByGuid.put(guid, a);
        String mode = integer(importer.get("spriteMode"), "0");
        if ("0".equals(mode)) {
            a.unusable = "is not imported as a sprite (spriteMode 0)";
            return;
        }
        a.pixelsPerUnit = f(importer.get("spritePixelsToUnits"), "100");
        int[] size = imageSize(a.image);
        if (size == null) {
            a.unusable = "has no readable image beside its .meta";
            return;
        }
        if ("3".equals(mode)) {
            Object outline = map(importer.get("spriteSheet")).get("outline");
            if (!wholeOutline(outline, size[0], size[1])) {
                a.outline = outlinePaths(outline);
                if (a.outline == null) {
                    a.unusable = "is imported as a polygon sprite (spriteMode 3) and its .meta has no outline"
                            + " that can be read";
                    return;
                }
            }
        }
        a.width = size[0];
        a.height = size[1];
        if (!"2".equals(mode)) {
            SpriteAsset s = new SpriteAsset();
            s.width = a.width;
            s.height = a.height;
            pivot(s, importer.get("alignment"), importer.get("spritePivot"));
            s.physicsShape = map(importer.get("spriteSheet")).get("physicsShape");
            a.single = s;
            return;
        }
        // Older projects name a sheet's sprites by a table of ids kept
        // apart from the sprites; newer ones keep the id on each sprite.
        Map<String, String> idsByName = new HashMap<String, String>();
        for (Object entry : list(importer.get("internalIDToNameTable"))) {
            Map<String, Object> e = map(entry);
            for (Object id : map(e.get("first")).values()) {
                idsByName.put(text(e.get("second")), text(id));
            }
        }
        Map<String, Object> sheet = map(importer.get("spriteSheet"));
        for (Object entry : list(sheet.get("sprites"))) {
            Map<String, Object> e = map(entry);
            Map<String, Object> rect = map(e.get("rect"));
            SpriteAsset s = new SpriteAsset();
            s.name = text(e.get("name"));
            s.width = (int) Math.round(Double.parseDouble(number(rect.get("width"), "0")));
            s.height = (int) Math.round(Double.parseDouble(number(rect.get("height"), "0")));
            s.x = (int) Math.round(Double.parseDouble(number(rect.get("x"), "0")));
            // The sheet measures up from the bottom of the image; whatever
            // paints measures down from the top.
            s.y = a.height - (int) Math.round(Double.parseDouble(number(rect.get("y"), "0"))) - s.height;
            pivot(s, e.get("alignment"), e.get("pivot"));
            s.physicsShape = e.get("physicsShape");
            String id = text(e.get("internalID"));
            if (id.length() == 0 || "0".equals(id)) {
                id = idsByName.containsKey(s.name) ? idsByName.get(s.name) : "";
            }
            if (id.length() > 0) {
                a.byFileId.put(id, s);
            }
        }
    }

    /// The size of a PNG or a JPEG, from its header.
    private static int[] imageSize(File image) throws IOException {
        byte[] b = image.isFile() ? Files.readAllBytes(image.toPath()) : new byte[0];
        // A PNG opens with an 8 byte signature and the IHDR chunk, whose
        // first two fields are the width and the height, big endian.
        if (b.length >= 24 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G' && b[12] == 'I' && b[13] == 'H') {
            return new int[] {
                ((b[16] & 0xff) << 24) | ((b[17] & 0xff) << 16) | ((b[18] & 0xff) << 8) | (b[19] & 0xff),
                ((b[20] & 0xff) << 24) | ((b[21] & 0xff) << 16) | ((b[22] & 0xff) << 8) | (b[23] & 0xff),
            };
        }
        // A JPEG is a run of segments; the frame header, SOF0 to SOF15
        // less the three that are not one, holds the height and the width.
        if (b.length > 4 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8) {
            int at = 2;
            while (at + 9 < b.length && (b[at] & 0xff) == 0xff) {
                int marker = b[at + 1] & 0xff;
                int length = ((b[at + 2] & 0xff) << 8) | (b[at + 3] & 0xff);
                if (marker >= 0xc0 && marker <= 0xcf && marker != 0xc4 && marker != 0xc8 && marker != 0xcc) {
                    return new int[] {
                        ((b[at + 7] & 0xff) << 8) | (b[at + 8] & 0xff), ((b[at + 5] & 0xff) << 8) | (b[at + 6] & 0xff),
                    };
                }
                at += 2 + length;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- sprites

    private String spriteMethod(String key, String construction) {
        String method = spriteMethods.get(key);
        if (method == null) {
            method = "sprite_" + spriteMethods.size();
            spriteMethods.put(key, method);
            String type = RUNTIME + "Sprite";
            members.append("\n    private static ").append(type).append(' ').append(method).append(";\n\n");
            members.append("    private static ").append(type).append(' ').append(method).append("() {\n");
            members.append("        if (").append(method).append(" == null) {\n");
            members.append("            ").append(method).append(" = ").append(construction).append(";\n");
            members.append("        }\n        return ").append(method).append(";\n    }\n");
        }
        return method + "()";
    }

    /// An expression for the sprite a `{fileID, guid}` reference names, or
    /// null -- with a warning -- when there is none to be had. `sizeHint`
    /// is the size, in world units, the user of a built-in sprite records
    /// for it.
    private String sprite(String origin, String user, Object reference, Map<String, Object> sizeHint) {
        String guid = text(map(reference).get("guid"));
        String fileId = text(map(reference).get("fileID"));
        if (guid.length() == 0) {
            return null;
        }
        if (BUILTIN_EXTRA.equals(guid) || BUILTIN_DEFAULT.equals(guid)) {
            return builtinSprite(origin, user, fileId, sizeHint);
        }
        TextureAsset a = texturesByGuid.get(guid);
        if (a == null) {
            if (sizeHint != null) {
                // A sprite of one of Unity's packages: the shapes the
                // editor's "2D Object > Sprites" menu makes are of this
                // kind. Its image is not in the project and may not be
                // shipped; those shapes are plain, white and one unit
                // across, and that is what is drawn.
                String resource = roundOwner ? "unity-package-circle.png" : "unity-package-square.png";
                if (!generatedImages.containsKey(resource)) {
                    generatedImages.put(resource, drawBuiltin(roundOwner, 256, 256));
                }
                notes.add(origin + ": " + user + " uses the sprite with GUID " + guid + ", which belongs to a"
                        + " Unity package and is not in the project; a white " + (roundOwner ? "disc" : "square")
                        + " one unit across is drawn instead");
                return spriteMethod("package/" + resource, "new " + RUNTIME + "Sprite(" + string(resource)
                        + ", 256, 256, 256.0f, 0.5f, 0.5f)");
            }
            warnings.add(origin + ": " + user + " uses the sprite with GUID " + guid + ", and no image under"
                    + " Assets has it; it was left without a sprite");
            return null;
        }
        if (a.unusable != null) {
            warnings.add(origin + ": " + user + " uses " + a.resource + ", which " + a.unusable + "; it was left"
                    + " without a sprite");
            return null;
        }
        SpriteAsset s = a.single != null ? a.single : a.byFileId.get(fileId);
        if (s == null) {
            warnings.add(origin + ": " + user + " uses sprite " + fileId + " of the sheet " + a.resource
                    + ", which has no such sprite; it was left without a sprite");
            return null;
        }
        if (a.outline != null) {
            if (a.cutResource == null) {
                cutToOutline(a);
            }
            String pixelsPerUnit = Float.toString(Float.parseFloat(a.pixelsPerUnit.replace("f", "")) * a.cutScale);
            return spriteMethod(guid + "/", "new " + RUNTIME + "Sprite(" + string(a.cutResource) + ", "
                    + string(a.resource.substring(0, a.resource.lastIndexOf('.'))) + ", 0, 0, "
                    + s.width * a.cutScale + ", " + s.height * a.cutScale + ", " + pixelsPerUnit + "f, " + s.pivotX
                    + ", " + s.pivotY + ")");
        }
        String resource = ship(a);
        String construction = a.single != null
                ? resource.equals(a.resource)
                        ? "new " + RUNTIME + "Sprite(" + string(a.resource) + ", " + s.width + ", " + s.height + ", "
                                + a.pixelsPerUnit + ", " + s.pivotX + ", " + s.pivotY + ")"
                        : "new " + RUNTIME + "Sprite(" + string(resource) + ", "
                                + string(a.resource.substring(0, a.resource.lastIndexOf('.'))) + ", 0, 0, " + s.width
                                + ", " + s.height + ", " + a.pixelsPerUnit + ", " + s.pivotX + ", " + s.pivotY + ")"
                : "new " + RUNTIME + "Sprite(" + string(resource) + ", " + string(s.name) + ", " + s.x + ", " + s.y
                        + ", " + s.width + ", " + s.height + ", " + a.pixelsPerUnit + ", " + s.pivotX + ", "
                        + s.pivotY + ").$texture(" + a.width + ", " + a.height + ")";
        return spriteMethod(guid + "/" + (a.single != null ? "" : fileId), construction);
    }

    /// The name an image is shipped under. Resources have no folders, and
    /// a project is free to keep an `Idle.png` in each of twenty: the
    /// first one used keeps its name and each later one is numbered, so
    /// that every sprite shows its own image. The sprite's name, which a
    /// script can read, is the image's own either way.
    private String ship(TextureAsset a) {
        if (a.shipped == null) {
            String name = a.resource;
            File taken = usedImages.get(name);
            int dot = name.lastIndexOf('.');
            for (int i = 2; taken != null && !taken.equals(a.image); i++) {
                name = a.resource.substring(0, dot) + "~" + i + a.resource.substring(dot);
                taken = usedImages.get(name);
            }
            usedImages.put(name, a.image);
            a.shipped = name;
        }
        return a.shipped;
    }

    /// A stand-in for one of Unity's built-in sprites, which are imported
    /// at 100 pixels per unit: fileID 10913 is the Knob, a disc.
    private String builtinSprite(String origin, String user, String fileId, Map<String, Object> sizeHint) {
        boolean knob = "10913".equals(fileId);
        int w = 0;
        int h = 0;
        if (sizeHint != null) {
            w = (int) Math.round(Double.parseDouble(number(sizeHint.get("x"), "0")) * 100);
            h = (int) Math.round(Double.parseDouble(number(sizeHint.get("y"), "0")) * 100);
        }
        if (w <= 0 || h <= 0 || w > 1024 || h > 1024) {
            w = knob ? 20 : 32;
            h = w;
        }
        String resource = (knob ? "unity-builtin-knob-" : "unity-builtin-square-") + w + "x" + h + ".png";
        if (!generatedImages.containsKey(resource)) {
            generatedImages.put(resource, drawBuiltin(knob, w, h));
            if (!knob) {
                warnings.add(origin + ": " + user + " uses Unity's built-in sprite " + fileId + ", which is not in"
                        + " the project; a plain white rectangle of its size is drawn instead");
            }
        }
        return spriteMethod("builtin/" + resource, "new " + RUNTIME + "Sprite(" + string(resource) + ", " + w + ", "
                + h + ", 100.0f, 0.5f, 0.5f)");
    }

    private static byte[] drawBuiltin(boolean disc, int w, int h) {
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true");
        }
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        if (disc) {
            g.fillOval(0, 0, w, h);
        } else {
            g.fillRect(0, 0, w, h);
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new SceneException("could not draw a built-in sprite: " + e, e);
        }
        return out.toByteArray();
    }

    private static String relativeName(File f) {
        return f.getParentFile().getName() + "/" + f.getName();
    }

    /// The angle about z, in degrees, that a rotation quaternion stands
    /// for. A 2D scene turns about nothing else; one that does is told so.
    private String zDegrees(String origin, long id, Map<String, Object> q) {
        double x = Double.parseDouble(number(q.get("x"), "0"));
        double y = Double.parseDouble(number(q.get("y"), "0"));
        double z = Double.parseDouble(number(q.get("z"), "0"));
        double w = Double.parseDouble(number(q.get("w"), "1"));
        // A particle system's object is turned out of the plane to aim
        // its shape, which the system itself accounts for: no origin is
        // given for one, and nothing is said.
        if (origin != null && (Math.abs(x) > 1e-4 || Math.abs(y) > 1e-4)) {
            warnings.add(origin + ": the " + the("transform", origin, id) + " is rotated about x or y; only the rotation about z is"
                    + " kept");
        }
        double degrees = Math.toDegrees(2 * Math.atan2(z, w));
        if (degrees > 180) {
            degrees -= 360;
        } else if (degrees <= -180) {
            degrees += 360;
        }
        // To a hundred-thousandth: a quaternion written in seven digits
        // does not know its angle any better, and 30 should read as 30.
        degrees = Math.round(degrees * 1e5) / 1e5;
        return degrees == 0 ? null : Float.toString((float) degrees) + "f";
    }

    // ------------------------------------------------------------ one graph

    private static final class Node {
        UnityYaml.Document doc;
        String variable;
        String javaType;
        /// The .NET name of the type, for checking a reference to it
        /// against the type of the field that holds it.
        String cilType;
        TypeDef script;
        /// Where it is in a prefab's list of parts.
        int part = -1;
    }

    /// The objects of one scene or one prefab.
    private static final class Graph {
        String origin;
        final Map<Long, Node> nodes = new LinkedHashMap<Long, Node>();
        final List<Node> order = new ArrayList<Node>();
        /// The mesh renderer of each object that carries a world text.
        Map<Long, UnityYaml.Document> meshRenderers = new HashMap<Long, UnityYaml.Document>();
    }

    private static String builtin(int classId) {
        switch (classId) {
            case 1:
                return "GameObject";
            case 4:
                return "Transform";
            case 224:
                return "RectTransform";
            case 50:
                return "Rigidbody2D";
            case 58:
                return "CircleCollider2D";
            case 60:
                return "PolygonCollider2D";
            case 61:
                return "BoxCollider2D";
            case 68:
                return "EdgeCollider2D";
            case 70:
                return "CapsuleCollider2D";
            case 20:
                return "Camera";
            case 81:
                return "AudioListener";
            case 82:
                return "AudioSource";
            case 212:
                return "SpriteRenderer";
            case 222:
                return "CanvasRenderer";
            case 223:
                return "Canvas";
            case 95:
                return "Animator";
            case 66:
                return "CompositeCollider2D";
            case 251:
                return "PlatformEffector2D";
            case 198:
                return "ParticleSystem";
            case 199:
                return "ParticleSystemRenderer";
            case 156049354:
                return "Grid";
            case 1839735485:
                return "tilemaps.Tilemap";
            case 483693784:
                return "tilemaps.TilemapRenderer";
            case 19719996:
                return "tilemaps.TilemapCollider2D";
            default:
                return null;
        }
    }

    /// The scripts of Unity's own packages a scene commonly names, by the
    /// GUID the package gives each, so that a component left out is
    /// reported as "a Slider" and not as thirty-two hexadecimal digits.
    ///
    /// A GUID is in every scene that uses the component, beside the fields
    /// the component serializes, and that is where each of these was read:
    /// scenes of open-source projects, checked against the fields Unity's
    /// manual documents for the component. A GUID that is not here is
    /// reported as it is.
    private static final String[][] PACKAGE_SCRIPTS = {
        {"67db9e8f0e2ae9c40bc1e2b64352a6b4", "UnityEngine.UI.Slider"},
        {"9085046f02f69544eb97fd06b6048fe2", "UnityEngine.UI.Toggle"},
        {"2a4db7a114972834c8e4117be1d82ba3", "UnityEngine.UI.Scrollbar"},
        {"1aa08ab6e0800fa44ae55d278d1423e3", "UnityEngine.UI.ScrollRect"},
        {"0d0b652f32a2cc243917e4028fa0f046", "UnityEngine.UI.Dropdown"},
        {"1344c3c82d62a2a41a3576d8abb8e3ea", "UnityEngine.UI.RawImage"},
        {"31a19414c41e5ae4aae2af33fee712f6", "UnityEngine.UI.Mask"},
        {"3312d7739989d2b4e91e6319e9a96d76", "UnityEngine.UI.RectMask2D"},
        {"30649d3a9faa99c48a7b1166b86bf2a0", "UnityEngine.UI.HorizontalLayoutGroup"},
        {"59f8146938fff824cb5fd77236b75775", "UnityEngine.UI.VerticalLayoutGroup"},
        {"8a8695521f0d02e499659fee002a26c2", "UnityEngine.UI.GridLayoutGroup"},
        {"306cc8c2b49d7114eaa3623786fc2126", "UnityEngine.UI.LayoutElement"},
        {"3245ec927659c4140ac4f8d17403cc18", "UnityEngine.UI.ContentSizeFitter"},
        {"cfabb0440166ab443bba8876756fdfa9", "UnityEngine.UI.Shadow"},
        {"e19747de3f5aca642ab2be37e372fb86", "UnityEngine.UI.Outline"},
        {"d0b148fe25e99eb48b9724523833bab1", "UnityEngine.EventSystems.EventTrigger"},
    };

    /// The scripts of Unity's packages the runtime has a class of its own
    /// for: the GUID, the Java class and the .NET name. The Cinemachine
    /// pipeline is the hidden component that only holds a virtual camera's
    /// stages together; it becomes nothing.
    private static final String[][] PACKAGE_CLASSES = {
        {"f4688fdb7df04437aeb418b961361dc5", "com.codename1.unitycompat.tmpro.TextMeshProUGUI",
            "TMPro.TextMeshProUGUI"},
        {"9541d86e2fd84c1d9990edf0852d74ab", "com.codename1.unitycompat.tmpro.TextMeshPro", "TMPro.TextMeshPro"},
        {"72ece51f2901e7445ab60da3685d6b5f", "com.codename1.unitycompat.cinemachine.CinemachineBrain",
            "Cinemachine.CinemachineBrain"},
        {"45e653bab7fb20e499bda25e1b646fea", "com.codename1.unitycompat.cinemachine.CinemachineVirtualCamera",
            "Cinemachine.CinemachineVirtualCamera"},
        {"fa7155796051b734daa718462081dc5f", "com.codename1.unitycompat.cinemachine.CinemachineTransposer",
            "Cinemachine.CinemachineTransposer"},
        {"6ad980451443d70438faac0bc6c235a0", "com.codename1.unitycompat.cinemachine.CinemachineFramingTransposer",
            "Cinemachine.CinemachineFramingTransposer"},
        {"ac0b09e7857660247b1477e93731de29", null, "Cinemachine.CinemachinePipeline"},
    };
    private static final String TMP_WORLD = "9541d86e2fd84c1d9990edf0852d74ab";

    /// The row of [#PACKAGE_CLASSES] for a package's script GUID, or -1.
    static int packageClass(String guid) {
        for (int i = 0; i < PACKAGE_CLASSES.length; i++) {
            if (PACKAGE_CLASSES[i][0].equals(guid)) {
                return i;
            }
        }
        return -1;
    }

    /// The .NET name of a built-in component.
    private static String cilName(String builtin) {
        return builtin.startsWith("tilemaps.") ? "UnityEngine.Tilemaps." + builtin.substring("tilemaps.".length())
                : "UnityEngine." + builtin;
    }

    /// The class a package's script GUID names, or null.
    static String packageScript(String guid) {
        for (String[] known : PACKAGE_SCRIPTS) {
            if (known[0].equals(guid)) {
                return known[1];
            }
        }
        return null;
    }

    /// How a warning names a document of a file: what it is, the
    /// GameObject it is or is on, by name, and its file ID. `kind` is
    /// "camera", "transform"; null for the object itself.
    private String the(String kind, String origin, long id) {
        Map<Long, String> names = objectNames.get(origin);
        String name = names == null ? null : names.get(Long.valueOf(id));
        if (name != null) {
            return "GameObject '" + name + "' (file ID " + id + ")";
        }
        names = ownerNames.get(origin);
        name = names == null ? null : names.get(Long.valueOf(id));
        if (name == null) {
            return (kind == null ? "object" : kind) + " " + id;
        }
        return kind == null ? "GameObject '" + name + "' (component file ID " + id + ")"
                : kind + " of GameObject '" + name + "' (file ID " + id + ")";
    }

    /// What a component left out was, for a reference to it, or null for
    /// an ID that names none.
    private String droppedAt(String origin, Long id) {
        Map<Long, String> gone = dropped.get(origin);
        return gone == null ? null : gone.get(id);
    }

    /// A field as a warning names it. `target` is the Java the scene
    /// compiler assigns to, `c1234.speed`; what is said is `speed` of
    /// which script on which GameObject.
    private String field(String origin, String target) {
        int end = 0;
        while (end < target.length() && target.charAt(end) != '.' && target.charAt(end) != '[') {
            end++;
        }
        Map<String, String> labels = variableLabels.get(origin);
        String label = labels == null ? null : labels.get(target.substring(0, end));
        if (label == null || end >= target.length()) {
            return target;
        }
        return "field " + target.substring(end + 1) + " of " + label;
    }

    private static String variable(String prefix, long id) {
        return prefix + (id < 0 ? "m" + (-id) : String.valueOf(id));
    }

    /// Reads the documents of a scene or a prefab into nodes: decides what
    /// each one is, and leaves out, with a word, what nothing implements.
    private Graph graph(String origin, List<UnityYaml.Document> docs) {
        Graph g = new Graph();
        g.origin = origin;
        List<UnityYaml.Document> expanded = expand(origin, docs, 0);
        Map<Long, String> names = new HashMap<Long, String>();
        for (UnityYaml.Document d : expanded) {
            if (d.properties != null && d.classId == 1) {
                names.put(Long.valueOf(d.fileId), text(d.properties.get("m_Name")));
            }
        }
        Map<Long, String> owners = new HashMap<Long, String>();
        for (UnityYaml.Document d : expanded) {
            if (d.properties != null && d.classId != 1 && d.properties.containsKey("m_GameObject")) {
                String owner = names.get(fileId(d.properties.get("m_GameObject")));
                if (owner != null) {
                    owners.put(Long.valueOf(d.fileId), owner);
                }
            }
        }
        objectNames.put(origin, names);
        ownerNames.put(origin, owners);
        // The objects that carry a text of the world, whose mesh renderer is
        // part of that text and not a component left out.
        java.util.Set<Long> worldText = new java.util.HashSet<Long>();
        for (UnityYaml.Document d : expanded) {
            if (d.properties != null && d.classId == 114
                    && TMP_WORLD.equals(text(map(d.properties.get("m_Script")).get("guid")))) {
                worldText.add(fileId(d.properties.get("m_GameObject")));
            }
        }
        Map<Long, UnityYaml.Document> meshRenderers = new HashMap<Long, UnityYaml.Document>();
        g.meshRenderers = meshRenderers;
        Map<Long, String> gone = new HashMap<Long, String>();
        dropped.put(origin, gone);
        Map<String, String> labels = new HashMap<String, String>();
        variableLabels.put(origin, labels);
        for (UnityYaml.Document d : expanded) {
            if (d.properties == null) {
                continue;
            }
            Node n = new Node();
            n.doc = d;
            if (d.classId == 114) {
                String guid = text(map(d.properties.get("m_Script")).get("guid"));
                n.script = scriptsByGuid.get(guid);
                if (n.script != null && (n.script.isAbstract()
                        || !universe.derivesFrom(n.script.fullName(), "UnityEngine.MonoBehaviour"))) {
                    // A script that was a behaviour when the object was
                    // made and is something else now -- a static class, a
                    // plain one. Unity shows it as a missing script and
                    // runs the rest of the object.
                    String what = "a component of the script " + n.script.fullName();
                    gone.put(Long.valueOf(d.fileId), what);
                    warnings.add(origin + ": " + the(null, origin, d.fileId) + " has " + what + ", which is static,"
                            + " abstract or not a MonoBehaviour and so cannot be a component; it was left out, as"
                            + " Unity leaves it");
                    continue;
                }
                int row = n.script == null ? packageClass(guid) : -1;
                String[] packaged = row < 0 ? null : PACKAGE_CLASSES[row];
                if (packaged != null && packaged[1] == null) {
                    continue;
                }
                if (n.script != null) {
                    n.javaType = types.javaClass(n.script.asType());
                    n.cilType = n.script.fullName();
                } else if (packaged != null) {
                    n.javaType = packaged[1];
                    n.cilType = packaged[2];
                } else if (d.properties.containsKey("m_FontData") && d.properties.containsKey("m_Text")) {
                    n.javaType = RUNTIME + "ui.Text";
                    n.cilType = "UnityEngine.UI.Text";
                } else if (d.properties.containsKey("m_UiScaleMode")) {
                    n.javaType = RUNTIME + "ui.CanvasScaler";
                    n.cilType = "UnityEngine.UI.CanvasScaler";
                } else if (d.properties.containsKey("m_Sprite") && d.properties.containsKey("m_FillMethod")) {
                    n.javaType = RUNTIME + "ui.Image";
                    n.cilType = "UnityEngine.UI.Image";
                } else if (d.properties.containsKey("m_OnClick") && d.properties.containsKey("m_Interactable")) {
                    n.javaType = RUNTIME + "ui.Button";
                    n.cilType = "UnityEngine.UI.Button";
                } else if (d.properties.containsKey("m_sendNavigationEvents")) {
                    n.javaType = RUNTIME + "eventsystems.EventSystem";
                    n.cilType = "UnityEngine.EventSystems.EventSystem";
                } else if (d.properties.containsKey("m_BlockingObjects")) {
                    n.javaType = RUNTIME + "ui.GraphicRaycaster";
                    n.cilType = "UnityEngine.UI.GraphicRaycaster";
                } else {
                    // The scripts of Unity's own UI package are known by
                    // the fields they serialize, since their GUIDs belong
                    // to the package and not to the project.
                    String known = d.properties.containsKey("m_SubmitButton") ? "StandaloneInputModule" : null;
                    if (known == null) {
                        // A script of a package, or one whose .cs is not in
                        // the project. The scene is built without it: one
                        // slider nobody implements must not be the reason
                        // a whole game does not build.
                        String type = packageScript(guid);
                        String what = type == null ? "a component whose script has GUID " + guid
                                : "a " + type + " component (script GUID " + guid + ")";
                        gone.put(Long.valueOf(d.fileId), what);
                        warnings.add(origin + ": " + the(null, origin, d.fileId) + " has " + what + ", and "
                                + (type == null ? "no script under Assets has that GUID"
                                : "the compatibility runtime has no " + type) + "; the component was left out,"
                                + " and whatever refers to it is left null");
                        continue;
                    }
                    notes.add(origin + ": the " + known + " of " + the(null, origin, d.fileId) + " was left out;"
                            + " the event system beside it routes the pointer to the controls itself, and nothing"
                            + " moves between controls by keyboard");
                    continue;
                }
                n.variable = variable("c", d.fileId);
                if (n.script != null) {
                    labels.put(n.variable, "the " + n.script.fullName() + " script on "
                            + the(null, origin, d.fileId));
                }
            } else if (builtin(d.classId) != null) {
                n.javaType = RUNTIME + builtin(d.classId);
                n.cilType = cilName(builtin(d.classId));
                n.variable = variable(d.classId == 1 ? "o" : d.classId == 4 || d.classId == 224 ? "t" : "c", d.fileId);
            } else {
                // Scene-wide settings and components nothing implements yet.
                if (d.classId == 92 || d.classId == 124) {
                    // What a camera of an old project carries: the layer
                    // that drew the immediate-mode interface's legacy text
                    // and textures, and the one that drew lens flares.
                    notes.add(origin + ": the " + (d.classId == 92 ? "GUILayer" : "FlareLayer") + " component of "
                            + the(null, origin, d.fileId) + " was left out; nothing in a 2D scene is drawn through"
                            + " it");
                } else if ((d.classId == 23 || d.classId == 33) && worldText.contains(fileId(d.properties
                        .get("m_GameObject")))) {
                    // The mesh a world text is drawn with in Unity. The
                    // text draws itself here, and takes its sorting from
                    // the renderer when it is set up.
                    if (d.classId == 23) {
                        meshRenderers.put(fileId(d.properties.get("m_GameObject")), d);
                    }
                } else if (d.properties.containsKey("m_GameObject")) {
                    gone.put(Long.valueOf(d.fileId), "a " + d.type + " component");
                    warnings.add(origin + ": the " + d.type + " component of " + the(null, origin, d.fileId)
                            + " is not supported and was left out");
                }
                continue;
            }
            g.nodes.put(Long.valueOf(d.fileId), n);
            g.order.add(n);
        }
        return g;
    }

    // ------------------------------------------------------ prefab instances

    /// Ids for the objects an instance brings into a file that the file
    /// itself never names. They count down from far below anything Unity
    /// writes, so that they cannot meet an id of the file.
    private long synthetic = -4000000000000000000L;

    /// Replaces every prefab instance of a file by the objects it stands
    /// for.
    ///
    /// A scene does not hold the objects of a prefab placed in the editor.
    /// It holds one `PrefabInstance` document naming the prefab, the parent
    /// the instance hangs under and a list of overrides -- each the object
    /// of the prefab it applies to, the path of a property and its new
    /// value -- and a `stripped` placeholder for each object of the
    /// instance that something else in the scene refers to. What Unity does
    /// on loading is done here on the documents: the prefab's own are read,
    /// given ids of this file -- the placeholder's where there is one, so
    /// that references to it resolve with no further ado -- and the
    /// overrides are written into their properties. Everything after this
    /// sees plain objects, and a prefab nested in a prefab is the same thing
    /// one level down.
    private List<UnityYaml.Document> expand(String origin, List<UnityYaml.Document> docs, int depth) {
        boolean any = false;
        for (UnityYaml.Document d : docs) {
            any |= d.classId == 1001 || d.stripped;
        }
        if (!any) {
            return docs;
        }
        // instance id/prefab GUID/id in the prefab -> the placeholder's id.
        Map<String, Long> placeholders = new HashMap<String, Long>();
        for (UnityYaml.Document d : docs) {
            if (d.stripped && d.properties != null) {
                Map<String, Object> source = map(d.properties.get("m_CorrespondingSourceObject"));
                placeholders.put(fileId(d.properties.get("m_PrefabInstance")) + "/" + text(source.get("guid")) + "/"
                        + fileId(source), Long.valueOf(d.fileId));
            }
        }
        List<UnityYaml.Document> out = new ArrayList<UnityYaml.Document>();
        for (UnityYaml.Document d : docs) {
            if (d.stripped || d.properties == null) {
                continue;
            }
            if (d.classId == 1001) {
                out.addAll(instantiate(origin, d, placeholders, depth));
            } else {
                out.add(d);
            }
        }
        // A component added to an object of an instance is a document of
        // this file whose game object is the instance's; the object's own
        // list of components, which came from the prefab, lacks it.
        Map<Long, UnityYaml.Document> objects = new HashMap<Long, UnityYaml.Document>();
        for (UnityYaml.Document d : out) {
            if (d.classId == 1) {
                objects.put(Long.valueOf(d.fileId), d);
            }
        }
        for (UnityYaml.Document d : out) {
            if (d.classId == 1 || !d.properties.containsKey("m_GameObject")) {
                continue;
            }
            UnityYaml.Document owner = objects.get(fileId(d.properties.get("m_GameObject")));
            if (owner == null) {
                continue;
            }
            boolean listed = false;
            for (Object entry : list(owner.properties.get("m_Component"))) {
                listed |= fileId(map(entry).get("component")).longValue() == d.fileId;
            }
            if (!listed) {
                List<Object> components = new ArrayList<Object>(list(owner.properties.get("m_Component")));
                Map<String, Object> reference = new LinkedHashMap<String, Object>();
                reference.put("fileID", String.valueOf(d.fileId));
                Map<String, Object> entry = new LinkedHashMap<String, Object>();
                entry.put("component", reference);
                components.add(entry);
                owner.properties.put("m_Component", components);
            }
        }
        return out;
    }

    /// The objects of one instance: the prefab's, renumbered and overridden.
    private List<UnityYaml.Document> instantiate(String origin, UnityYaml.Document instance,
            Map<String, Long> placeholders, int depth) {
        List<UnityYaml.Document> none = new ArrayList<UnityYaml.Document>();
        String guid = text(map(instance.properties.get("m_SourcePrefab")).get("guid"));
        File file = prefabFilesByGuid.get(guid);
        if (file == null || !file.isFile()) {
            warnings.add(origin + ": prefab instance " + instance.fileId + " is of the prefab with GUID " + guid
                    + ", which is not under Assets; it was left out");
            return none;
        }
        if (depth > 16) {
            warnings.add(origin + ": prefab instance " + instance.fileId + " is nested more than 16 deep, which"
                    + " can only be a prefab that holds itself; it was left out");
            return none;
        }
        List<UnityYaml.Document> source;
        try {
            // Read afresh every time: the documents are rewritten below.
            source = expand(file.getName(), UnityYaml.parse(read(file)), depth + 1);
        } catch (IOException e) {
            throw new SceneException(file + ": " + e, e);
        }
        Map<String, String> renumbered = new HashMap<String, String>();
        for (UnityYaml.Document d : source) {
            d.sources.add(guid + "/" + d.fileId);
            Long id = null;
            for (String s : d.sources) {
                Long placeholder = placeholders.get(instance.fileId + "/" + s);
                id = placeholder != null ? placeholder : id;
            }
            if (id == null) {
                id = Long.valueOf(synthetic--);
            }
            renumbered.put(String.valueOf(d.fileId), String.valueOf(id));
            d.fileId = id.longValue();
        }
        for (UnityYaml.Document d : source) {
            renumber(d.properties, renumbered);
        }
        Map<String, Object> modification = map(instance.properties.get("m_Modification"));
        for (Object entry : list(modification.get("m_Modifications"))) {
            Map<String, Object> m = map(entry);
            String path = text(m.get("propertyPath"));
            List<UnityYaml.Document> targets = targets(source, m.get("target"));
            if (targets.size() != 1) {
                // An override of an object the prefab no longer has is
                // kept by the editor and ignored by the player.
                if (targets.size() > 1) {
                    warnings.add(origin + ": prefab instance " + instance.fileId + " overrides " + path + " of an"
                            + " object of a nested prefab that is there more than once; the override was dropped");
                }
                continue;
            }
            Object reference = m.get("objectReference");
            boolean isReference = fileId(reference).longValue() != 0 || text(map(reference).get("guid")).length() > 0;
            Object value = isReference ? reference : m.get("value");
            // A reference that is cleared is written as no value and a
            // null reference; a string that is emptied looks the same, and
            // the property that is there tells them apart.
            if (!isReference && text(m.get("value")).length() == 0 && get(targets.get(0).properties, path)
                    instanceof Map) {
                value = reference;
            }
            set(targets.get(0).properties, path, value == null ? "" : value);
        }
        for (Object removed : list(modification.get("m_RemovedComponents"))) {
            source.removeAll(targets(source, removed));
        }
        Object parent = modification.get("m_TransformParent");
        for (UnityYaml.Document d : source) {
            if ((d.classId == 4 || d.classId == 224) && fileId(d.properties.get("m_Father")).longValue() == 0
                    && parent != null) {
                d.properties.put("m_Father", parent);
            }
        }
        return source;
    }

    /// The documents a `{fileID, guid}` of an override names.
    private static List<UnityYaml.Document> targets(List<UnityYaml.Document> source, Object target) {
        String key = text(map(target).get("guid")) + "/" + fileId(target);
        List<UnityYaml.Document> found = new ArrayList<UnityYaml.Document>();
        for (UnityYaml.Document d : source) {
            if (d.sources.contains(key)) {
                found.add(d);
            }
        }
        return found;
    }

    /// Rewrites every reference to an object of the same file. One to an
    /// asset carries a GUID and is left alone.
    private static void renumber(Object value, Map<String, String> ids) {
        if (value instanceof Map) {
            Map<String, Object> m = map(value);
            Object id = m.get("fileID");
            if (id instanceof String && !m.containsKey("guid") && ids.containsKey(id)) {
                m.put("fileID", ids.get(id));
            }
            for (Object child : m.values()) {
                renumber(child, ids);
            }
        } else if (value instanceof List) {
            for (Object child : list(value)) {
                renumber(child, ids);
            }
        }
    }

    /// One step of a property path: `name`, or `data[3]` under `Array`.
    private static Object step(Object at, String name) {
        if (name.startsWith("data[") && at instanceof List) {
            int index = Integer.parseInt(name.substring(5, name.length() - 1));
            List<Object> l = list(at);
            return index < l.size() ? l.get(index) : null;
        }
        return at instanceof Map ? map(at).get(name) : null;
    }

    private static String[] path(String path) {
        // `items.Array.data[2].x`: the Array step says only that what
        // follows indexes a list, which the list itself says already.
        List<String> steps = new ArrayList<String>();
        for (String s : path.split("\\.")) {
            if (!"Array".equals(s)) {
                steps.add(s);
            }
        }
        return steps.toArray(new String[0]);
    }

    private static Object get(Map<String, Object> properties, String propertyPath) {
        Object at = properties;
        for (String s : path(propertyPath)) {
            at = step(at, s);
        }
        return at;
    }

    /// Writes an override into the properties of the object it applies to,
    /// making the maps and lists on the way that the prefab left out.
    private static void set(Map<String, Object> properties, String propertyPath, Object value) {
        String[] steps = path(propertyPath);
        Object at = properties;
        for (int i = 0; i < steps.length; i++) {
            String name = steps[i];
            boolean last = i == steps.length - 1;
            boolean listNext = !last && (steps[i + 1].startsWith("data[") || "size".equals(steps[i + 1]));
            if ("size".equals(name) && at instanceof List && last) {
                List<Object> l = list(at);
                int size = (int) Long.parseLong(integer(value, "0"));
                while (l.size() > size && size >= 0) {
                    l.remove(l.size() - 1);
                }
                while (l.size() < size && size < 100000) {
                    l.add("");
                }
                return;
            }
            if (name.startsWith("data[") && at instanceof List) {
                List<Object> l = list(at);
                int index = Integer.parseInt(name.substring(5, name.length() - 1));
                while (l.size() <= index && index < 100000) {
                    l.add("");
                }
                if (last) {
                    l.set(index, value);
                    return;
                }
                Object next = l.get(index);
                if (!(next instanceof Map) && !(next instanceof List)) {
                    next = listNext ? (Object) new ArrayList<Object>() : new LinkedHashMap<String, Object>();
                    l.set(index, next);
                }
                at = next;
                continue;
            }
            if (!(at instanceof Map)) {
                return;
            }
            Map<String, Object> m = map(at);
            if (last) {
                m.put(name, value);
                return;
            }
            Object next = m.get(name);
            if (!(next instanceof Map) && !(next instanceof List)) {
                next = listNext ? (Object) new ArrayList<Object>() : new LinkedHashMap<String, Object>();
                m.put(name, next);
            }
            at = next;
        }
    }

    private static boolean isTransform(Node n) {
        return n.doc.classId == 4 || n.doc.classId == 224;
    }

    /// Writes the statements that build a graph. With `parts`, every node
    /// is also stored in the array of that name when it is not null, so
    /// that a prefab's template can be asked for any object of it.
    private void emit(Graph g, StringBuilder sb, boolean parts) {
        String origin = g.origin;
        Map<Long, Node> nodes = g.nodes;
        boolean camera = false;
        boolean draws = false;
        // Every object first, each game object with its components in the
        // order the file lists them, so that a reference can point anywhere.
        for (Node n : g.order) {
            if (n.doc.classId != 1) {
                continue;
            }
            Map<String, Object> p = n.doc.properties;
            boolean rect = false;
            for (Object entry : list(p.get("m_Component"))) {
                Node c = nodes.get(fileId(map(entry).get("component")));
                rect |= c != null && c.doc.classId == 224;
            }
            line(sb, n.javaType + " " + n.variable + " = new " + n.javaType + "(" + string(text(p.get("m_Name")))
                    + (rect ? ", new " + RUNTIME + "RectTransform()" : "") + ");");
            for (Object entry : list(p.get("m_Component"))) {
                Node c = nodes.get(fileId(map(entry).get("component")));
                if (c == null) {
                    continue;
                }
                if (isTransform(c)) {
                    line(sb, c.javaType + " " + c.variable + " = (" + c.javaType + ") " + n.variable
                            + ".get_transform();");
                } else {
                    line(sb, c.javaType + " " + c.variable + " = new " + c.javaType + "();");
                    line(sb, n.variable + ".$attach(" + c.variable + ");");
                }
            }
        }
        if (parts) {
            line(sb, "if (parts != null) {");
            for (Node n : g.order) {
                line(sb, "    parts[" + n.part + "] = " + n.variable + ";");
            }
            line(sb, "}");
        }
        // Then what each one holds.
        for (Node n : g.order) {
            Map<String, Object> p = n.doc.properties;
            String v = n.variable;
            switch (n.doc.classId) {
                case 1:
                    if ("0".equals(text(p.get("m_IsActive")))) {
                        line(sb, v + ".SetActive(false);");
                    }
                    if (text(p.get("m_TagString")).length() > 0 && !"Untagged".equals(text(p.get("m_TagString")))) {
                        line(sb, v + ".set_tag(" + string(text(p.get("m_TagString"))) + ");");
                    }
                    if (!"0".equals(integer(p.get("m_Layer"), "0"))) {
                        line(sb, v + ".set_layer((int) " + integer(p.get("m_Layer"), "0") + "L);");
                    }
                    break;
                case 4:
                case 224: {
                    Map<String, Object> pos = map(p.get("m_LocalPosition"));
                    Map<String, Object> scale = map(p.get("m_LocalScale"));
                    Node above = nodes.get(fileId(p.get("m_Father")));
                    if (n.doc.classId == 224 && above != null && above.doc.classId == 4) {
                        // A rectangle under a plain transform has nothing
                        // to be anchored to but that transform's origin:
                        // its anchored position is its position, and the
                        // file leaves the position's own x and y at zero.
                        Map<String, Object> anchored = map(p.get("m_AnchoredPosition"));
                        Map<String, Object> moved = new LinkedHashMap<String, Object>(pos);
                        moved.put("x", anchored.get("x"));
                        moved.put("y", anchored.get("y"));
                        pos = moved;
                    }
                    line(sb, v + ".$place(" + f(pos.get("x"), "0") + ", " + f(pos.get("y"), "0") + ", "
                            + f(pos.get("z"), "0") + ", " + f(scale.get("x"), "1") + ", " + f(scale.get("y"), "1")
                            + ", " + f(scale.get("z"), "1") + ");");
                    boolean emitter = false;
                    Node self = nodes.get(fileId(p.get("m_GameObject")));
                    for (Object entry : list(self == null ? null : self.doc.properties.get("m_Component"))) {
                        Node other = nodes.get(fileId(map(entry).get("component")));
                        emitter |= other != null && other.doc.classId == 198;
                    }
                    String degrees = zDegrees(emitter ? null : origin, n.doc.fileId, map(p.get("m_LocalRotation")));
                    if (degrees != null) {
                        line(sb, v + ".$rotate(" + degrees + ");");
                    }
                    if (n.doc.classId == 224) {
                        Map<String, Object> min = map(p.get("m_AnchorMin"));
                        Map<String, Object> max = map(p.get("m_AnchorMax"));
                        Map<String, Object> at = map(p.get("m_AnchoredPosition"));
                        Map<String, Object> size = map(p.get("m_SizeDelta"));
                        Map<String, Object> pivot = map(p.get("m_Pivot"));
                        line(sb, v + ".$layout(" + f(min.get("x"), "0.5") + ", " + f(min.get("y"), "0.5") + ", "
                                + f(max.get("x"), "0.5") + ", " + f(max.get("y"), "0.5") + ", " + f(at.get("x"), "0")
                                + ", " + f(at.get("y"), "0") + ", " + f(size.get("x"), "100") + ", "
                                + f(size.get("y"), "100") + ", " + f(pivot.get("x"), "0.5") + ", "
                                + f(pivot.get("y"), "0.5") + ");");
                    }
                    break;
                }
                case 20: {
                    camera = true;
                    enabled(sb, v, p);
                    if ("0".equals(text(p.get("orthographic")))) {
                        warnings.add(origin + ": the " + the("camera", origin, n.doc.fileId) + " is a perspective camera, which is not"
                                + " supported; it was made orthographic");
                    }
                    line(sb, v + ".set_orthographicSize(" + f(p.get("orthographic size"), "5") + ");");
                    Map<String, Object> c = map(p.get("m_BackGroundColor"));
                    if (!c.isEmpty()) {
                        line(sb, v + ".$background(" + f(c.get("r"), "0") + ", " + f(c.get("g"), "0") + ", "
                                + f(c.get("b"), "0") + ", " + f(c.get("a"), "0") + ");");
                    }
                    break;
                }
                case 212: {
                    enabled(sb, v, p);
                    Map<String, Object> c = map(p.get("m_Color"));
                    if (!c.isEmpty()) {
                        line(sb, v + ".$tint(" + f(c.get("r"), "1") + ", " + f(c.get("g"), "1") + ", "
                                + f(c.get("b"), "1") + ", " + f(c.get("a"), "1") + ");");
                    }
                    if ("1".equals(text(p.get("m_FlipX")))) {
                        line(sb, v + ".set_flipX(true);");
                    }
                    if ("1".equals(text(p.get("m_FlipY")))) {
                        line(sb, v + ".set_flipY(true);");
                    }
                    String order = integer(p.get("m_SortingOrder"), "0");
                    if (!"0".equals(order)) {
                        line(sb, v + ".set_sortingOrder((int) " + order + "L);");
                    }
                    String layer = integer(p.get("m_SortingLayerID"), "0");
                    if (!"0".equals(layer)) {
                        line(sb, v + ".set_sortingLayerID((int) " + layer + "L);");
                    }
                    if (!"0".equals(integer(p.get("m_DrawMode"), "0"))) {
                        warnings.add(origin + ": the " + the("sprite renderer", origin, n.doc.fileId) + " is sliced or tiled, which is"
                                + " not supported; its sprite is drawn stretched");
                    }
                    roundOwner = false;
                    Node owner = nodes.get(fileId(p.get("m_GameObject")));
                    for (Object entry : list(owner == null ? null : owner.doc.properties.get("m_Component"))) {
                        Node other = nodes.get(fileId(map(entry).get("component")));
                        roundOwner |= other != null && other.doc.classId == 58;
                    }
                    String s = sprite(origin, "the " + the("sprite renderer", origin, n.doc.fileId), p.get("m_Sprite"),
                            map(p.get("m_Size")));
                    if (s != null) {
                        draws = true;
                        line(sb, v + ".set_sprite(" + s + ");");
                    }
                    break;
                }
                case 50:
                    line(sb, v + ".$bodyType = " + integer(p.get("m_BodyType"), "0") + ";");
                    line(sb, v + ".set_mass(" + f(p.get("m_Mass"), "1") + ");");
                    line(sb, v + ".set_gravityScale(" + f(p.get("m_GravityScale"), "1") + ");");
                    line(sb, v + ".set_drag(" + f(p.get("m_LinearDrag"), "0") + ");");
                    line(sb, v + ".set_angularDrag(" + f(p.get("m_AngularDrag"), "0.05") + ");");
                    if (!"0".equals(integer(p.get("m_Constraints"), "0"))) {
                        line(sb, v + ".set_constraints((int) " + integer(p.get("m_Constraints"), "0") + "L);");
                    }
                    if ("1".equals(integer(p.get("m_CollisionDetection"), "0"))) {
                        line(sb, v + ".set_collisionDetectionMode(1);");
                    }
                    if ("0".equals(text(p.get("m_Simulated")))) {
                        line(sb, v + ".set_simulated(false);");
                    }
                    if ("1".equals(text(p.get("m_UseAutoMass")))) {
                        warnings.add(origin + ": the " + the("rigidbody", origin, n.doc.fileId) + " takes its mass from its colliders"
                                + " (Use Auto Mass), which is not supported; the mass the file records is used");
                    }
                    break;
                case 82: {
                    enabled(sb, v, p);
                    String c = clip(origin, "the " + the("audio source", origin, n.doc.fileId), p.get("m_audioClip"));
                    line(sb, v + ".$setup(" + (c == null ? "null" : c) + ", " + f(p.get("m_Volume"), "1") + ", "
                            + f(p.get("m_Pitch"), "1") + ", " + "1".equals(text(p.get("Loop"))) + ", "
                            + "1".equals(text(p.get("Mute"))) + ", " + !"0".equals(text(p.get("m_PlayOnAwake")))
                            + ");");
                    break;
                }
                case 58:
                    collider(origin, n, nodes, sb);
                    line(sb, v + ".$radius = " + f(p.get("m_Radius"), "0.5") + ";");
                    break;
                case 60: {
                    collider(origin, n, nodes, sb);
                    StringBuilder paths = new StringBuilder();
                    for (Object path : list(map(p.get("m_Points")).get("m_Paths"))) {
                        paths.append("\n        {");
                        for (Object point : list(path)) {
                            paths.append(f(map(point).get("x"), "0")).append(", ")
                                    .append(f(map(point).get("y"), "0")).append(", ");
                        }
                        paths.append("},");
                    }
                    String constant = "PATHS_" + constants++;
                    members.append("\n    private static final float[][] ").append(constant).append(" = {")
                            .append(paths).append("\n    };\n");
                    line(sb, v + ".$paths(" + constant + ");");
                    break;
                }
                case 61: {
                    collider(origin, n, nodes, sb);
                    Map<String, Object> size = map(p.get("m_Size"));
                    line(sb, v + ".$width = " + f(size.get("x"), "1") + ";");
                    line(sb, v + ".$height = " + f(size.get("y"), "1") + ";");
                    break;
                }
                case 68: {
                    collider(origin, n, nodes, sb);
                    StringBuilder points = new StringBuilder();
                    for (Object point : list(p.get("m_Points"))) {
                        points.append(points.length() == 0 ? "" : ", ").append(f(map(point).get("x"), "0"))
                                .append(", ").append(f(map(point).get("y"), "0"));
                    }
                    // A scene written before the points were its own keeps
                    // none, and the collider keeps Unity's default line.
                    if (points.length() > 0) {
                        line(sb, v + ".$points(new float[] {" + points + "});");
                    }
                    if (Double.parseDouble(number(p.get("m_EdgeRadius"), "0")) != 0d) {
                        warnings.add(origin + ": the " + the("edge collider", origin, n.doc.fileId) + " has an edge radius,"
                                + " which is not supported; it collides as a thin line");
                    }
                    break;
                }
                case 70: {
                    collider(origin, n, nodes, sb);
                    Map<String, Object> size = map(p.get("m_Size"));
                    line(sb, v + ".$width = " + f(size.get("x"), "0.5") + ";");
                    line(sb, v + ".$height = " + f(size.get("y"), "1") + ";");
                    line(sb, v + ".$direction = (int) " + integer(p.get("m_Direction"), "0") + "L;");
                    break;
                }
                case 81:
                    enabled(sb, v, p);
                    break;
                case 95: {
                    enabled(sb, v, p);
                    String c = controller(origin, "the " + the("animator", origin, n.doc.fileId), p.get("m_Controller"));
                    if (c != null) {
                        line(sb, v + ".$setup(" + c + ");");
                    }
                    break;
                }
                case 66:
                    collider(origin, n, nodes, sb);
                    line(sb, v + ".$setup((int) " + integer(p.get("m_GeometryType"), "0") + "L);");
                    break;
                case 251:
                    enabled(sb, v, p);
                    line(sb, v + ".$setup(" + !"0".equals(text(p.get("m_UseColliderMask"))) + ", (int) "
                            + integer(map(p.get("m_ColliderMask")).get("m_Bits"), "-1") + "L, "
                            + !"0".equals(text(p.get("m_UseOneWay"))) + ", "
                            + "1".equals(text(p.get("m_UseOneWayGrouping"))) + ", " + f(p.get("m_SurfaceArc"), "180")
                            + ", " + f(p.get("m_RotationalOffset"), "0") + ");");
                    break;
                case 156049354: {
                    Map<String, Object> size = map(p.get("m_CellSize"));
                    Map<String, Object> gap = map(p.get("m_CellGap"));
                    line(sb, v + ".$cells(" + f(size.get("x"), "1") + ", " + f(size.get("y"), "1") + ", "
                            + f(gap.get("x"), "0") + ", " + f(gap.get("y"), "0") + ");");
                    if (!"0".equals(integer(p.get("m_CellLayout"), "0"))
                            || !"0".equals(integer(p.get("m_CellSwizzle"), "0"))) {
                        warnings.add(origin + ": the " + the("grid", origin, n.doc.fileId) + " is hexagonal, isometric or"
                                + " not in the XY plane, none of which is supported; its cells are laid out as"
                                + " rectangles in XY");
                    }
                    break;
                }
                case 1839735485:
                    tilemap(origin, n, sb);
                    break;
                case 483693784: {
                    enabled(sb, v, p);
                    String order = integer(p.get("m_SortingOrder"), "0");
                    if (!"0".equals(order)) {
                        line(sb, v + ".set_sortingOrder((int) " + order + "L);");
                    }
                    String layer = integer(p.get("m_SortingLayerID"), "0");
                    if (!"0".equals(layer)) {
                        line(sb, v + ".set_sortingLayerID((int) " + layer + "L);");
                    }
                    draws = true;
                    break;
                }
                case 19719996:
                    collider(origin, n, nodes, sb);
                    break;
                case 198: {
                    Map<String, Object> turned = new LinkedHashMap<String, Object>();
                    Node owner = nodes.get(fileId(p.get("m_GameObject")));
                    for (Object entry : list(owner == null ? null : owner.doc.properties.get("m_Component"))) {
                        Node other = nodes.get(fileId(map(entry).get("component")));
                        if (other != null && isTransform(other)) {
                            turned = map(other.doc.properties.get("m_LocalRotation"));
                        }
                    }
                    String code = ParticleAssets.system(origin + ": the " + the("particle system", origin,
                            n.doc.fileId), v, p, turned, warnings, notes);
                    for (String statement : code.split("\n")) {
                        line(sb, statement);
                    }
                    break;
                }
                case 199:
                    particleRenderer(origin, n, sb);
                    draws = true;
                    break;
                case 223: {
                    enabled(sb, v, p);
                    String mode = integer(p.get("m_RenderMode"), "0");
                    if ("2".equals(mode)) {
                        warnings.add(origin + ": the " + the("canvas", origin, n.doc.fileId) + " is a World Space canvas, which is not"
                                + " supported; it is not drawn");
                    }
                    Node cam = nodes.get(fileId(p.get("m_Camera")));
                    line(sb, v + ".$setup(" + mode + ", " + (cam != null && cam.doc.classId == 20 ? cam.variable
                            : "null") + ", (int) " + integer(p.get("m_SortingOrder"), "0") + "L, (int) "
                            + integer(p.get("m_SortingLayerID"), "0") + "L, " + f(p.get("m_PlaneDistance"), "100")
                            + ");");
                    break;
                }
                case 114:
                    if (n.script != null) {
                        script(origin, n, nodes, sb);
                    } else if (n.cilType.startsWith("TMPro.")) {
                        textMesh(origin, n, g, sb);
                        draws = true;
                    } else if (n.cilType.startsWith("Cinemachine.")) {
                        cinemachine(origin, n, nodes, sb);
                    } else if ("UnityEngine.UI.Text".equals(n.cilType)) {
                        enabled(sb, v, p);
                        Map<String, Object> font = map(p.get("m_FontData"));
                        Map<String, Object> c = map(p.get("m_Color"));
                        line(sb, v + ".$setup(" + string(text(p.get("m_Text"))) + ", (int) "
                                + integer(font.get("m_FontSize"), "14") + "L, (int) "
                                + integer(font.get("m_FontStyle"), "0") + "L, (int) "
                                + integer(font.get("m_Alignment"), "0") + "L, "
                                + "1".equals(text(font.get("m_BestFit"))) + ", (int) "
                                + integer(font.get("m_MinSize"), "10") + "L, (int) "
                                + integer(font.get("m_MaxSize"), "40") + "L, "
                                + !"1".equals(text(font.get("m_HorizontalOverflow"))) + ");");
                        if (!c.isEmpty()) {
                            line(sb, v + ".$color(" + f(c.get("r"), "1") + ", " + f(c.get("g"), "1") + ", "
                                    + f(c.get("b"), "1") + ", " + f(c.get("a"), "1") + ");");
                        }
                        String fontGuid = text(map(font.get("m_Font")).get("guid"));
                        if (fontGuid.length() > 0 && !BUILTIN_DEFAULT.equals(fontGuid)
                                && !BUILTIN_EXTRA.equals(fontGuid)) {
                            notes.add(origin + ": the " + the("text", origin, n.doc.fileId) + " names a font of the project; text is"
                                    + " drawn in the platform's font");
                        }
                    } else if ("UnityEngine.UI.Image".equals(n.cilType)) {
                        image(origin, n, sb);
                    } else if ("UnityEngine.UI.Button".equals(n.cilType)) {
                        button(origin, n, nodes, sb);
                    } else if (!"UnityEngine.UI.CanvasScaler".equals(n.cilType)) {
                        enabled(sb, v, p);
                    } else {
                        enabled(sb, v, p);
                        Map<String, Object> reference = map(p.get("m_ReferenceResolution"));
                        line(sb, v + ".$setup((int) " + integer(p.get("m_UiScaleMode"), "0") + "L, "
                                + f(p.get("m_ScaleFactor"), "1") + ", " + f(reference.get("x"), "800") + ", "
                                + f(reference.get("y"), "600") + ", (int) "
                                + integer(p.get("m_ScreenMatchMode"), "0") + "L, "
                                + f(p.get("m_MatchWidthOrHeight"), "0") + ");");
                    }
                    break;
                default:
                    break;
            }
        }
        // Then the hierarchy, each parent's children in the order it lists
        // them, which is the order of the object in the editor.
        Map<Long, Boolean> parented = new HashMap<Long, Boolean>();
        for (Node n : g.order) {
            if (!isTransform(n)) {
                continue;
            }
            for (Object child : list(n.doc.properties.get("m_Children"))) {
                Node c = nodes.get(fileId(child));
                if (c != null && isTransform(c) && parented.put(Long.valueOf(c.doc.fileId), Boolean.TRUE) == null) {
                    line(sb, c.variable + ".$parent(" + n.variable + ");");
                }
            }
        }
        for (Node n : g.order) {
            if (!isTransform(n) || parented.containsKey(Long.valueOf(n.doc.fileId))) {
                continue;
            }
            Long father = fileId(n.doc.properties.get("m_Father"));
            Node to = nodes.get(father);
            if (to != null && isTransform(to)) {
                line(sb, n.variable + ".$parent(" + to.variable + ");");
            } else if (father.longValue() != 0) {
                warnings.add(origin + ": the " + the("transform", origin, n.doc.fileId) + " has a parent that is not in the file; it"
                        + " was left without one");
            }
        }
        if (draws && !camera && !parts) {
            warnings.add(origin + ": the scene has sprites and no camera; nothing will be drawn");
        }
    }

    // ------------------------------------------------------- user interface

    private static final Pattern SPRITE_BORDER = Pattern.compile(
            "spriteBorder: \\{x: ([^,]+), y: ([^,]+), z: ([^,]+), w: ([^}]+)\\}");

    /// The sprite an image with none is drawn with: white, so that the
    /// image's colour is what shows. Unity draws such an image as a plain
    /// rectangle, and this is one.
    private String whiteSprite() {
        String resource = "unity-ui-white.png";
        if (!generatedImages.containsKey(resource)) {
            generatedImages.put(resource, drawBuiltin(false, 4, 4));
        }
        return spriteMethod("ui/" + resource, "new " + RUNTIME + "Sprite(" + string(resource)
                + ", 4, 4, 100.0f, 0.5f, 0.5f)");
    }

    private void image(String origin, Node n, StringBuilder sb) {
        Map<String, Object> p = n.doc.properties;
        String v = n.variable;
        String user = "the " + the("image", origin, n.doc.fileId);
        enabled(sb, v, p);
        Object reference = p.get("m_Sprite");
        String guid = text(map(reference).get("guid"));
        String type = integer(p.get("m_Type"), "0");
        String sprite;
        if (guid.length() == 0) {
            sprite = whiteSprite();
        } else if (BUILTIN_EXTRA.equals(guid) || BUILTIN_DEFAULT.equals(guid)) {
            // The sprites of Unity's own controls -- the rounded rectangle
            // of a button, a panel's background. They are part of the
            // editor, not of the project.
            notes.add(origin + ": " + user + " uses Unity's built-in interface sprite "
                    + text(map(reference).get("fileID")) + ", which is not in the project; a plain rectangle of"
                    + " the image's colour is drawn instead");
            sprite = whiteSprite();
        } else {
            sprite = sprite(origin, user, reference, null);
            TextureAsset a = texturesByGuid.get(guid);
            if (sprite != null && a != null && (type.equals("1") || type.equals("2"))) {
                boolean bordered = false;
                try {
                    Matcher m = SPRITE_BORDER.matcher(read(new File(a.image.getPath() + ".meta")));
                    while (m.find()) {
                        for (int i = 1; i <= 4; i++) {
                            bordered |= Double.parseDouble(m.group(i).trim()) != 0;
                        }
                    }
                } catch (IOException e) {
                    bordered = false;
                } catch (NumberFormatException e) {
                    bordered = false;
                }
                if ("2".equals(type)) {
                    warnings.add(origin + ": " + user + " is a tiled image, which is not supported; its sprite is"
                            + " stretched over it once");
                } else if (bordered) {
                    warnings.add(origin + ": " + user + " is a sliced image of a sprite with borders, which is not"
                            + " supported; the whole sprite is stretched and its corners with it");
                }
            }
        }
        String method = integer(p.get("m_FillMethod"), "0");
        if ("3".equals(type) && !"0".equals(method) && !"1".equals(method)) {
            warnings.add(origin + ": " + user + " is filled around a centre, which is not supported; it is drawn"
                    + " whole");
            type = "0";
        }
        line(sb, v + ".$setup(" + (sprite == null ? "null" : sprite) + ", (int) " + type + "L, "
                + "1".equals(text(p.get("m_PreserveAspect"))) + ", (int) " + method + "L, (int) "
                + integer(p.get("m_FillOrigin"), "0") + "L, " + f(p.get("m_FillAmount"), "1") + ");");
        Map<String, Object> c = map(p.get("m_Color"));
        if (!c.isEmpty()) {
            line(sb, v + ".$color(" + f(c.get("r"), "1") + ", " + f(c.get("g"), "1") + ", " + f(c.get("b"), "1")
                    + ", " + f(c.get("a"), "1") + ");");
        }
        if ("0".equals(text(p.get("m_RaycastTarget")))) {
            line(sb, v + ".set_raycastTarget(false);");
        }
    }

    private static int channel(Object value, double multiplier) {
        double v = Double.parseDouble(number(value, "1")) * multiplier;
        return v <= 0 ? 0 : v >= 1 ? 255 : (int) Math.round(v * 255);
    }

    private void button(String origin, Node n, Map<Long, Node> nodes, StringBuilder sb) {
        Map<String, Object> p = n.doc.properties;
        String v = n.variable;
        enabled(sb, v, p);
        String transition = integer(p.get("m_Transition"), "1");
        if ("2".equals(transition) || "3".equals(transition)) {
            notes.add(origin + ": the " + the("button", origin, n.doc.fileId) + " shows its state by "
                    + ("2".equals(transition) ? "swapping sprites" : "an animation") + ", which is not"
                    + " supported; it looks the same in every state and works as it should");
        }
        Node target = nodes.get(fileId(p.get("m_TargetGraphic")));
        String lost = droppedAt(origin, fileId(p.get("m_TargetGraphic")));
        if (lost != null) {
            warnings.add(origin + ": the " + the("button", origin, n.doc.fileId) + " has for its target graphic "
                    + lost + " of " + the(null, origin, fileId(p.get("m_TargetGraphic")).longValue())
                    + ", which was left out; the button shows no change of state");
        }
        boolean graphic = target != null && target.cilType != null
                && universe.derivesFrom(target.cilType, "UnityEngine.UI.Graphic");
        line(sb, v + ".$setup(" + !"0".equals(text(p.get("m_Interactable"))) + ", (int) " + transition + "L, "
                + (graphic ? target.variable : "null") + ");");
        Map<String, Object> colors = map(p.get("m_Colors"));
        if (!colors.isEmpty()) {
            double multiplier = Double.parseDouble(number(colors.get("m_ColorMultiplier"), "1"));
            String[] names = {"m_NormalColor", "m_HighlightedColor", "m_PressedColor", "m_SelectedColor",
                "m_DisabledColor"};
            for (int i = 0; i < names.length; i++) {
                Map<String, Object> c = map(colors.get(names[i]));
                if (c.isEmpty()) {
                    continue;
                }
                int argb = (channel(c.get("a"), multiplier) << 24) | (channel(c.get("r"), multiplier) << 16)
                        | (channel(c.get("g"), multiplier) << 8) | channel(c.get("b"), multiplier);
                line(sb, v + ".$color(" + i + ", 0x" + Integer.toHexString(argb) + ");");
            }
        }
        persistentCalls(origin, "the " + the("button", origin, n.doc.fileId), v + ".get_onClick()", map(p.get("m_OnClick")), nodes, sb);
    }

    /// The listeners the editor's event inspector lists for an event: each
    /// names an object of the scene, a public method of it and at most one
    /// argument, a constant. Unity finds the method by name when the scene
    /// loads. Here it is found now, in the compiled scripts, and written
    /// as a call -- so a row that names a method the script no longer has
    /// is a warning at build time, where Unity would log it at run time.
    private void persistentCalls(String origin, String user, String event, Map<String, Object> serialized,
            Map<Long, Node> nodes, StringBuilder sb) {
        for (Object entry : list(map(serialized.get("m_PersistentCalls")).get("m_Calls"))) {
            Map<String, Object> call = map(entry);
            Long id = fileId(call.get("m_Target"));
            String name = text(call.get("m_MethodName"));
            // Call state 0 is "Off"; a row with no object or no method
            // does nothing in Unity either.
            if (id.longValue() == 0 || name.length() == 0 || "0".equals(integer(call.get("m_CallState"), "2"))) {
                continue;
            }
            Node to = nodes.get(id);
            if (to == null || to.cilType == null) {
                String lost = droppedAt(origin, id);
                warnings.add(origin + ": " + user + " calls " + name + " on " + (lost != null
                        ? lost + " of " + the(null, origin, id.longValue()) + ", which was left out"
                        : the(null, origin, id.longValue()) + ", which is not in the file or is of a kind that is"
                        + " not supported") + "; that listener was left out");
                continue;
            }
            Map<String, Object> arguments = map(call.get("m_Arguments"));
            String mode = integer(call.get("m_Mode"), "1");
            CilType.Kind wanted = null;
            String argument = "";
            Node passed = null;
            if ("3".equals(mode)) {
                wanted = CilType.Kind.I4;
                argument = "(int) " + integer(arguments.get("m_IntArgument"), "0") + "L";
            } else if ("4".equals(mode)) {
                wanted = CilType.Kind.R4;
                argument = f(arguments.get("m_FloatArgument"), "0");
            } else if ("5".equals(mode)) {
                wanted = CilType.Kind.STRING;
                argument = string(text(arguments.get("m_StringArgument")));
            } else if ("6".equals(mode)) {
                wanted = CilType.Kind.BOOLEAN;
                argument = String.valueOf("1".equals(text(arguments.get("m_BoolArgument"))));
            } else if ("2".equals(mode)) {
                passed = nodes.get(fileId(arguments.get("m_ObjectArgument")));
            }
            boolean one = wanted != null || "2".equals(mode);
            MethodDef found = null;
            boolean hidden = false;
            TypeDef at = universe.find(to.cilType);
            while (at != null && found == null) {
                for (int mi = 0; mi < at.methods.size() && found == null; mi++) {
                    MethodDef m = at.methods.get(mi);
                    if (!m.name.equals(name) || m.isStatic() || m.sig.params.length != (one ? 1 : 0)) {
                        continue;
                    }
                    if (one) {
                        CilType param = types.storage(m.sig.params[0]);
                        if (wanted != null ? param.kind != wanted : types.isStruct(param)
                                || universe.definitionOf(param) == null) {
                            continue;
                        }
                    }
                    if ((m.flags & 7) != 6) {
                        hidden = true;
                        continue;
                    }
                    found = m;
                }
                at = at.baseType == null ? null : universe.definitionOf(at.baseType);
            }
            if (found == null) {
                warnings.add(origin + ": " + user + " calls " + name + " on a " + to.cilType + ", which has no "
                        + (hidden ? "public " : "") + "method of that name taking "
                        + (one ? "that one argument" : "no argument") + "; that listener was left out");
                continue;
            }
            if ("2".equals(mode)) {
                CilType param = types.storage(found.sig.params[0]);
                TypeDef def = universe.definitionOf(param);
                if (passed == null || passed.cilType == null || !universe.derivesFrom(passed.cilType, def.fullName())) {
                    argument = "null";
                } else {
                    argument = "(" + types.javaClass(param) + ") (java.lang.Object) " + passed.variable;
                }
            }
            String base = RUNTIME + "events.UnityEventBase";
            line(sb, event + ".$persistent(" + to.variable + ", new " + base + ".Call() {");
            line(sb, "    public void call(java.lang.Object target) {");
            line(sb, "        ((" + to.javaType + ") target)." + name + "(" + argument + ");");
            line(sb, "    }");
            line(sb, "});");
        }
    }

    private static void enabled(StringBuilder sb, String v, Map<String, Object> p) {
        if ("0".equals(text(p.get("m_Enabled")))) {
            line(sb, v + ".set_enabled(false);");
        }
    }

    private void collider(String origin, Node n, Map<Long, Node> nodes, StringBuilder sb) {
        Map<String, Object> p = n.doc.properties;
        String v = n.variable;
        Map<String, Object> offset = map(p.get("m_Offset"));
        line(sb, v + ".$offsetX = " + f(offset.get("x"), "0") + ";");
        line(sb, v + ".$offsetY = " + f(offset.get("y"), "0") + ";");
        enabled(sb, v, p);
        if ("1".equals(text(p.get("m_IsTrigger")))) {
            line(sb, v + ".set_isTrigger(true);");
        }
        // A collider with no material of its own takes the one of the
        // rigidbody it is attached to.
        String[] material = material(origin, "the " + the("collider", origin, n.doc.fileId), p.get("m_Material"));
        Node owner = nodes.get(fileId(p.get("m_GameObject")));
        for (Object entry : list(owner == null || material != null ? null : owner.doc.properties.get("m_Component"))) {
            Node c = nodes.get(fileId(map(entry).get("component")));
            if (c != null && c.doc.classId == 50 && material == null) {
                material = material(origin, "the " + the("rigidbody", origin, c.doc.fileId), c.doc.properties.get("m_Material"));
            }
        }
        if (material != null) {
            line(sb, v + ".$friction = " + material[0] + ";");
            line(sb, v + ".$bounciness = " + material[1] + ";");
        }
        boolean composite = "1".equals(text(p.get("m_UsedByComposite")));
        boolean effector = "1".equals(text(p.get("m_UsedByEffector")));
        if (composite || effector) {
            line(sb, v + ".$usedBy(" + composite + ", " + effector + ");");
        }
        if (effector) {
            boolean platform = false;
            for (Object entry : list(owner == null ? null : owner.doc.properties.get("m_Component"))) {
                Node c = nodes.get(fileId(map(entry).get("component")));
                platform |= c != null && c.doc.classId == 251;
            }
            if (!platform) {
                warnings.add(origin + ": the " + the("collider", origin, n.doc.fileId) + " is used by an effector, and"
                        + " the only effector supported is the platform effector; it collides as itself");
            }
        }
    }

    /// The serialized fields of a script: every key that is not one of
    /// Unity's own `m_` properties names a field of the class.
    private void script(String origin, Node n, Map<Long, Node> nodes, StringBuilder sb) {
        Map<String, Object> p = n.doc.properties;
        enabled(sb, n.variable, p);
        for (Map.Entry<String, Object> e : p.entrySet()) {
            String key = e.getKey();
            if (key.startsWith("m_") || "serializedVersion".equals(key)) {
                continue;
            }
            FieldDef field = findField(n.script, key);
            if (field == null || field.isStatic()) {
                // Unity ignores data for a field the script no longer has.
                notes.add(origin + ": " + n.script.fullName() + " has no field " + key + "; its value was"
                        + " dropped, as Unity drops it");
                continue;
            }
            assign(origin, n.variable + "." + key, field.type, e.getValue(), nodes, sb);
        }
    }

    private FieldDef findField(TypeDef type, String name) {
        TypeDef at = type;
        while (at != null) {
            FieldDef f = at.field(name);
            if (f != null) {
                return f;
            }
            at = at.baseType == null ? null : universe.definitionOf(at.baseType);
        }
        return null;
    }

    /// How Java source spells a type.
    private String javaType(CilType declared) {
        CilType type = types.storage(declared);
        switch (type.kind) {
            case BOOLEAN:
                return "boolean";
            case R4:
                return "float";
            case R8:
                return "double";
            case I1:
            case U1:
                return "byte";
            case I2:
                return "short";
            case U2:
            case CHAR:
                return "char";
            case I4:
            case U4:
                return "int";
            case I8:
            case U8:
                return "long";
            case SZARRAY:
                return javaType(type.element) + "[]";
            default:
                return types.javaClass(type);
        }
    }

    private static String boxed(String primitive) {
        if ("int".equals(primitive)) {
            return "Integer";
        }
        if ("char".equals(primitive)) {
            return "Character";
        }
        if ("boolean".equals(primitive) || "float".equals(primitive) || "double".equals(primitive)
                || "byte".equals(primitive) || "short".equals(primitive) || "long".equals(primitive)) {
            return Character.toUpperCase(primitive.charAt(0)) + primitive.substring(1);
        }
        return null;
    }

    /// The elements of a serialized array or list.
    ///
    /// Unity writes an array of integers -- or of an enum, which is one --
    /// as a single run of hexadecimal digits when it can: each element in
    /// as many bytes as its type has, low byte first. `[119, 100]` as
    /// `KeyCode[]` is `7700000064000000`.
    private List<Object> items(Object value, CilType element) {
        if (!(value instanceof String)) {
            return list(value);
        }
        String hex = (String) value;
        int bytes;
        switch (types.storage(element).kind) {
            case BOOLEAN:
            case I1:
            case U1:
                bytes = 1;
                break;
            case I2:
            case U2:
            case CHAR:
                bytes = 2;
                break;
            case I4:
            case U4:
                bytes = 4;
                break;
            case I8:
            case U8:
                bytes = 8;
                break;
            default:
                return list(value);
        }
        if (hex.length() == 0 || hex.length() % (bytes * 2) != 0 || !hex.matches("[0-9a-fA-F]+")) {
            return list(value);
        }
        List<Object> items = new ArrayList<Object>();
        for (int at = 0; at < hex.length(); at += bytes * 2) {
            long v = 0;
            for (int b = bytes - 1; b >= 0; b--) {
                v = (v << 8) | Integer.parseInt(hex.substring(at + b * 2, at + b * 2 + 2), 16);
            }
            // Signed, as the text of a number would be: every use narrows it.
            if (bytes < 8 && types.storage(element).kind != CilType.Kind.U4 && (v & (1L << (bytes * 8 - 1))) != 0) {
                v -= 1L << (bytes * 8);
            }
            items.add(String.valueOf(v));
        }
        return items;
    }

    /// How deep in classes that hold classes a field being assigned is.
    private int depth;

    private void assign(String origin, String target, CilType declared, Object value, Map<Long, Node> nodes,
            StringBuilder sb) {
        CilType type = types.storage(declared);
        switch (type.kind) {
            case BOOLEAN:
                line(sb, target + " = " + (!"0".equals(text(value))) + ";");
                return;
            case R4:
                line(sb, target + " = " + f(value, "0") + ";");
                return;
            case R8:
                line(sb, target + " = " + number(value, "0") + "d;");
                return;
            case I1:
            case U1:
                line(sb, target + " = (byte) " + integer(value, "0") + ";");
                return;
            case I2:
                line(sb, target + " = (short) " + integer(value, "0") + ";");
                return;
            case U2:
            case CHAR:
                line(sb, target + " = (char) " + integer(value, "0") + ";");
                return;
            case I4:
            case U4:
                line(sb, target + " = (int) " + integer(value, "0") + "L;");
                return;
            case I8:
            case U8:
                line(sb, target + " = " + integer(value, "0") + "L;");
                return;
            case STRING:
                line(sb, target + " = " + string(text(value)) + ";");
                return;
            case SZARRAY: {
                List<Object> items = items(value, type.element);
                String element = javaType(type.element);
                int brackets = element.indexOf('[');
                line(sb, target + " = new " + (brackets < 0 ? element + "[" + items.size() + "]"
                        : element.substring(0, brackets) + "[" + items.size() + "]" + element.substring(brackets))
                        + ";");
                for (int i = 0; i < items.size(); i++) {
                    if (types.isStruct(types.storage(type.element))) {
                        line(sb, target + "[" + i + "] = new " + element + "();");
                    }
                    assign(origin, target + "[" + i + "]", type.element, items.get(i), nodes, sb);
                }
                return;
            }
            case GENERICINST:
                if ("System.Collections.Generic.List`1".equals(type.element.typeName()) && type.args.length == 1) {
                    String listClass = types.javaClass(type);
                    line(sb, target + " = new " + listClass + "();");
                    String element = javaType(type.args[0]);
                    for (Object item : items(value, type.args[0])) {
                        String temporary = "v" + temporaries++;
                        line(sb, element + " " + temporary + (types.isStruct(types.storage(type.args[0]))
                                ? " = new " + element + "();" : ";"));
                        assign(origin, temporary, type.args[0], item, nodes, sb);
                        String box = boxed(element);
                        line(sb, target + ".Add(" + (box == null ? temporary : box + ".valueOf(" + temporary + ")")
                                + ");");
                    }
                    return;
                }
                break;
            default:
                break;
        }
        TypeDef def = universe.definitionOf(type);
        if (def == null) {
            warnings.add(origin + ": " + field(origin, target) + " is a " + declared + ", which is not supported;"
                    + " its value was dropped");
            return;
        }
        if (types.isStruct(type)) {
            // A struct field already holds its object: fill it in.
            for (Map.Entry<String, Object> e : map(value).entrySet()) {
                FieldDef member = def.field(e.getKey());
                if (member != null && !member.isStatic()) {
                    assign(origin, target + "." + e.getKey(), member.type, e.getValue(), nodes, sb);
                }
            }
            return;
        }
        String wanted = def.fullName();
        if (!universe.derivesFrom(wanted, "UnityEngine.Object")) {
            // A class of the project's own that Unity serializes in
            // place: its fields are written where the field is.
            if (!(value instanceof Map) || def.isAbstract() || def.isInterface() || def.genericParamCount > 0
                    || !hasConstructor(def)) {
                warnings.add(origin + ": " + field(origin, target) + " is a " + wanted + ", which is abstract,"
                        + " generic or has no constructor without arguments; it was left as its constructor made"
                        + " it");
                return;
            }
            if (depth > 7) {
                // Unity stops too, at ten levels: a class that holds
                // itself has no end.
                return;
            }
            depth++;
            line(sb, target + " = new " + types.javaClass(type) + "();");
            for (Map.Entry<String, Object> e : map(value).entrySet()) {
                FieldDef member = findField(def, e.getKey());
                if (member != null && !member.isStatic()) {
                    assign(origin, target + "." + e.getKey(), member.type, e.getValue(), nodes, sb);
                }
            }
            depth--;
            return;
        }
        String guid = text(map(value).get("guid"));
        Long id = fileId(value);
        if ("UnityEngine.RuntimeAnimatorController".equals(wanted)) {
            String c = controller(origin, field(origin, target), value);
            line(sb, target + " = " + (c == null ? "null" : c) + ";");
            return;
        }
        if ("UnityEngine.AnimationClip".equals(wanted)) {
            String c = animation(origin, field(origin, target), value);
            line(sb, target + " = " + (c == null ? "null" : c) + ";");
            return;
        }
        if ("UnityEngine.Tilemaps.TileBase".equals(wanted) || "UnityEngine.Tilemaps.Tile".equals(wanted)) {
            String t = tile(origin, field(origin, target), value);
            line(sb, target + " = " + (t == null ? "null" : t) + ";");
            return;
        }
        if ("UnityEngine.AudioClip".equals(wanted)) {
            String c = clip(origin, field(origin, target), value);
            line(sb, target + " = " + (c == null ? "null" : c) + ";");
            return;
        }
        if ("UnityEngine.TextAsset".equals(wanted)) {
            File file = textByGuid.get(guid);
            if (file == null && guid.length() > 0) {
                warnings.add(origin + ": " + field(origin, target) + " uses the text asset with GUID " + guid + ", and no text"
                        + " file under Assets has it; it was left null");
            }
            line(sb, target + " = " + (file == null ? "null" : textAsset(file)) + ";");
            return;
        }
        if ("UnityEngine.Sprite".equals(wanted)) {
            // An asset, not an object of the scene: named by GUID.
            String s = sprite(origin, field(origin, target), value, null);
            line(sb, target + " = " + (s == null ? "null" : s) + ";");
            return;
        }
        if (id.longValue() == 0) {
            line(sb, target + " = null;");
            return;
        }
        Node to;
        String expression;
        if (guid.length() > 0) {
            Prefab prefab = prefab(origin, field(origin, target), guid);
            if (prefab == null) {
                line(sb, target + " = null;");
                return;
            }
            to = prefab.nodes.get(id);
            expression = to == null ? null : "prefabPart_" + prefab.index + "(" + to.part + ")";
        } else {
            to = nodes.get(id);
            expression = to == null ? null : to.variable;
        }
        if (to == null) {
            String lost = guid.length() > 0 ? null : droppedAt(origin, id);
            warnings.add(origin + ": " + field(origin, target) + " refers to " + (lost != null
                    ? lost + " of " + the(null, origin, id.longValue()) + ", which was left out"
                    : (guid.length() > 0 ? "object " + id + " of a prefab" : the(null, origin, id.longValue()))
                    + ", which is not there or is of a kind that is not supported") + "; it was left null");
            line(sb, target + " = null;");
            return;
        }
        if (!universe.derivesFrom(to.cilType, wanted)) {
            warnings.add(origin + ": " + field(origin, target) + " is a " + wanted + " and refers to "
                    + (guid.length() > 0 ? "object " + id + " of a prefab" : the(null, origin, id.longValue()))
                    + ", a " + to.cilType + "; it was left null");
            line(sb, target + " = null;");
            return;
        }
        line(sb, target + " = (" + types.javaClass(type) + ") (java.lang.Object) " + expression + ";");
    }

    // ----------------------------------------------------------------- assets

    /// Says a thing once however many objects it is true of.
    private void once(List<String> to, String message) {
        if (saidOnce.add(message)) {
            to.add(message);
        }
    }

    /// A method that builds an asset the first time it is asked for and
    /// returns the same one after. `body` is the statements that build it
    /// into a variable named `made`.
    private String lazy(String key, String prefix, String type, String body) {
        String method = prefix + assetMethods.size();
        assetMethods.put(key, method);
        members.append("\n    private static ").append(type).append(' ').append(method).append(";\n\n");
        members.append("    private static ").append(type).append(' ').append(method).append("() {\n");
        members.append("        if (").append(method).append(" == null) {\n");
        members.append(body);
        members.append("            ").append(method).append(" = made;\n");
        members.append("        }\n        return ").append(method).append(";\n    }\n");
        return method;
    }

    /// An expression for the animation clip a reference names, or null.
    private String animation(final String origin, String user, Object reference) {
        String guid = text(map(reference).get("guid"));
        if (guid.length() == 0) {
            return null;
        }
        String method = assetMethods.get("clip/" + guid);
        if (method != null) {
            return method.length() == 0 ? null : method + "()";
        }
        File file = animationFilesByGuid.get(guid);
        if (file == null || !file.isFile()) {
            warnings.add(origin + ": " + user + " uses the animation clip with GUID " + guid + ", and no .anim"
                    + " file under Assets has it; it plays nothing");
            assetMethods.put("clip/" + guid, "");
            return null;
        }
        final String label = relative(project, file);
        String body;
        try {
            body = AnimationAssets.clip(label, UnityYaml.parse(read(file)), "made", new AnimationAssets.Sprites() {
                @Override
                public String sprite(String who, Object value) {
                    return SceneCompiler.this.sprite(label, who, value, null);
                }
            }, warnings, notes);
        } catch (IOException e) {
            throw new SceneException(file + ": " + e, e);
        }
        if (body == null) {
            assetMethods.put("clip/" + guid, "");
            return null;
        }
        return lazy("clip/" + guid, "animation_", RUNTIME + "AnimationClip", body) + "()";
    }

    /// An expression for the animator controller a reference names, or
    /// null.
    private String controller(final String origin, String user, Object reference) {
        String guid = text(map(reference).get("guid"));
        if (guid.length() == 0) {
            if (fileId(reference).longValue() != 0) {
                warnings.add(origin + ": " + user + " has a controller that is not an asset of the project; it"
                        + " plays nothing");
            }
            return null;
        }
        String method = assetMethods.get("controller/" + guid);
        if (method != null) {
            return method.length() == 0 ? null : method + "()";
        }
        File file = controllerFilesByGuid.get(guid);
        if (file == null || !file.isFile()) {
            warnings.add(origin + ": " + user + " uses the controller with GUID " + guid + ", and no .controller"
                    + " file under Assets has it (an override controller is not supported); it plays nothing");
            assetMethods.put("controller/" + guid, "");
            return null;
        }
        final String label = relative(project, file);
        String body;
        try {
            body = AnimationAssets.controller(label, UnityYaml.parse(read(file)), "made", new AnimationAssets.Clips() {
                @Override
                public String clip(String who, Object value) {
                    return animation(label, who, value);
                }
            }, warnings);
        } catch (IOException e) {
            throw new SceneException(file + ": " + e, e);
        }
        if (body == null) {
            assetMethods.put("controller/" + guid, "");
            return null;
        }
        return lazy("controller/" + guid, "controller_", RUNTIME + "RuntimeAnimatorController", body) + "()";
    }

    /// An expression for the tile asset a reference names, or null.
    private String tile(String origin, String user, Object reference) {
        String guid = text(map(reference).get("guid"));
        if (guid.length() == 0) {
            return null;
        }
        String method = assetMethods.get("tile/" + guid);
        if (method != null) {
            return method.length() == 0 ? null : method + "()";
        }
        File file = assetFilesByGuid.get(guid);
        Map<String, Object> p = null;
        try {
            for (UnityYaml.Document d : file == null || !file.isFile() ? new ArrayList<UnityYaml.Document>()
                    : UnityYaml.parse(read(file))) {
                if (d.properties != null && d.classId == 114 && p == null) {
                    p = d.properties;
                }
            }
        } catch (IOException e) {
            throw new SceneException(file + ": " + e, e);
        }
        if (p == null) {
            warnings.add(origin + ": " + user + " uses the tile with GUID " + guid + ", and no .asset file under"
                    + " Assets has it; its cells are drawn, and a script that asks for the tile gets null");
            assetMethods.put("tile/" + guid, "");
            return null;
        }
        String label = relative(project, file);
        Map<String, Object> script = map(p.get("m_Script"));
        boolean plain = "13312".equals(text(script.get("fileID"))) && BUILTIN_DEFAULT.equals(text(script.get("guid")));
        Object shown = plain ? p.get("m_Sprite") : p.get("m_DefaultSprite");
        String collides = integer(plain ? p.get("m_ColliderType") : p.get("m_DefaultColliderType"), "1");
        if (!plain) {
            notes.add(label + ": the tile is of a scripted kind (a rule tile, an animated tile) and not a plain"
                    + " Tile; the cells a scene painted with it show the sprites the editor chose for them, and a"
                    + " cell a script sets with it shows its default sprite");
        }
        String s = sprite(label, "the tile", shown, null);
        Map<String, Object> c = map(p.get("m_Color"));
        String body = "            " + RUNTIME + "tilemaps.Tile made = new " + RUNTIME + "tilemaps.Tile().$setup("
                + string(text(p.get("m_Name"))) + ", " + (s == null ? "null" : s + solid(shown)) + ", "
                + f(c.get("r"), "1") + ", " + f(c.get("g"), "1") + ", " + f(c.get("b"), "1") + ", " + f(c.get("a"), "1")
                + ", (int) " + collides + "L);\n";
        method = lazy("tile/" + guid, "tile_", RUNTIME + "tilemaps.Tile", body);
        tileColliders.put(method, collides);
        return method + "()";
    }

    private BufferedImage image(File file) {
        if (!imagesRead.containsKey(file)) {
            BufferedImage read = null;
            try {
                read = ImageIO.read(file);
            } catch (IOException e) {
                read = null;
            }
            imagesRead.put(file, read);
        }
        return imagesRead.get(file);
    }

    /// What to append to a sprite's expression so that it knows which of
    /// its pixels are solid: nothing for one that is solid all over.
    private String solid(Object reference) {
        String guid = text(map(reference).get("guid"));
        String fileId = text(map(reference).get("fileID"));
        TextureAsset a = texturesByGuid.get(guid);
        if (a == null || a.unusable != null || a.outline != null) {
            return "";
        }
        SpriteAsset s = a.single != null ? a.single : a.byFileId.get(fileId);
        if (s == null) {
            return "";
        }
        String key = guid + "/" + (a.single != null ? "" : fileId);
        String constant = solidConstants.get(key);
        if (constant == null) {
            constant = "";
            BufferedImage image = image(a.image);
            int[] rows = image == null ? null : TilemapAssets.solid(image, s.x, s.y, s.width, s.height,
                    TilemapAssets.shape(s.physicsShape));
            if (rows != null) {
                constant = "SOLID_" + constants++;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < rows.length; i++) {
                    sb.append(i == 0 ? "" : i % 12 == 0 ? ",\n        " : ", ").append(rows[i]);
                }
                members.append("\n    private static final int[] ").append(constant).append(" = {\n        ")
                        .append(sb).append("\n    };\n");
            }
            solidConstants.put(key, constant);
        }
        return constant.length() == 0 ? "" : ".$solid(" + constant + ")";
    }

    private void tilemap(String origin, Node n, StringBuilder sb) {
        Map<String, Object> p = n.doc.properties;
        String v = n.variable;
        String user = "the " + the("tilemap", origin, n.doc.fileId);
        Map<String, Object> anchor = map(p.get("m_TileAnchor"));
        Map<String, Object> tint = map(p.get("m_Color"));
        line(sb, v + ".$setup(" + f(anchor.get("x"), "0.5") + ", " + f(anchor.get("y"), "0.5") + ", "
                + f(tint.get("r"), "1") + ", " + f(tint.get("g"), "1") + ", " + f(tint.get("b"), "1") + ", "
                + f(tint.get("a"), "1") + ");");
        if (!"0".equals(integer(p.get("m_TileOrientation"), "0"))) {
            warnings.add(origin + ": " + user + " is not oriented in the XY plane, which is not supported; it is"
                    + " drawn in XY");
        }
        TilemapAssets.Cells cells = TilemapAssets.cells(p.get("m_Tiles"));
        if (cells.offPlane > 0) {
            notes.add(origin + ": " + user + " has " + cells.offPlane + " cells off the plane z = 0, which were"
                    + " left out");
        }
        List<Object> tiles = list(p.get("m_TileAssetArray"));
        List<Object> sprites = list(p.get("m_TileSpriteArray"));
        List<Object> matrices = list(p.get("m_TileMatrixArray"));
        List<Object> colors = list(p.get("m_TileColorArray"));
        for (TilemapAssets.Variant variant : cells.variants) {
            Object tileReference = variant.tile >= 0 && variant.tile < tiles.size()
                    ? map(tiles.get(variant.tile)).get("m_Data") : null;
            Object spriteReference = variant.sprite >= 0 && variant.sprite < sprites.size()
                    ? map(sprites.get(variant.sprite)).get("m_Data") : null;
            String tile = tile(origin, user, tileReference);
            String collides = tile == null ? "1" : tileColliders.get(tile.substring(0, tile.length() - 2));
            String s = sprite(origin, user, spriteReference, null);
            if (s != null && "1".equals(collides)) {
                s = s + solid(spriteReference);
            }
            double[] place = TilemapAssets.place(variant.matrix >= 0 && variant.matrix < matrices.size()
                    ? map(map(matrices.get(variant.matrix)).get("m_Data")) : map(null));
            Map<String, Object> c = variant.color >= 0 && variant.color < colors.size()
                    ? map(map(colors.get(variant.color)).get("m_Data")) : map(null);
            line(sb, v + ".$variant(" + (tile == null ? "null" : tile) + ", " + (s == null ? "null" : s)
                    + ", (int) " + collides + "L, " + (float) place[0] + "f, " + (float) place[1] + "f, "
                    + (float) place[2] + "f, " + (float) place[3] + "f, " + (float) place[4] + "f, "
                    + f(c.get("r"), "1") + ", " + f(c.get("g"), "1") + ", " + f(c.get("b"), "1") + ", "
                    + f(c.get("a"), "1") + ");");
        }
        if (cells.count == 0) {
            return;
        }
        line(sb, v + ".$bounds(" + cells.fromX + ", " + cells.fromY + ", " + cells.across + ", " + cells.up + ");");
        for (String runs : cells.runs) {
            String constant = "CELLS_" + constants++;
            members.append("\n    private static final String ").append(constant).append(" = ").append(string(runs))
                    .append(";\n");
            line(sb, v + ".$fill(" + constant + ");");
        }
    }

    /// A particle renderer draws with the main texture of its material.
    private void particleRenderer(String origin, Node n, StringBuilder sb) {
        Map<String, Object> p = n.doc.properties;
        String v = n.variable;
        String user = "the " + the("particle renderer", origin, n.doc.fileId);
        enabled(sb, v, p);
        String order = integer(p.get("m_SortingOrder"), "0");
        if (!"0".equals(order)) {
            line(sb, v + ".set_sortingOrder((int) " + order + "L);");
        }
        String layer = integer(p.get("m_SortingLayerID"), "0");
        if (!"0".equals(layer)) {
            line(sb, v + ".set_sortingLayerID((int) " + layer + "L);");
        }
        if (!"0".equals(integer(p.get("m_RenderMode"), "0"))) {
            warnings.add(origin + ": " + user + " stretches its particles or draws them as meshes, which is not"
                    + " supported; each is drawn as a sprite facing the camera");
        }
        String sprite = null;
        int color = 0xffffffff;
        List<Object> materials = list(p.get("m_Materials"));
        String guid = materials.isEmpty() ? "" : text(map(materials.get(0)).get("guid"));
        File file = renderMaterialsByGuid.get(guid);
        if (file != null && file.isFile()) {
            try {
                for (UnityYaml.Document d : UnityYaml.parse(read(file))) {
                    if (d.properties == null || d.classId != 21) {
                        continue;
                    }
                    Map<String, Object> saved = map(d.properties.get("m_SavedProperties"));
                    for (Object entry : list(saved.get("m_TexEnvs"))) {
                        for (Map.Entry<String, Object> e : map(entry).entrySet()) {
                            if (sprite == null && ("_MainTex".equals(e.getKey()) || "_BaseMap".equals(e.getKey()))) {
                                sprite = wholeImage(text(map(map(e.getValue()).get("m_Texture")).get("guid")));
                            }
                        }
                    }
                    for (Object entry : list(saved.get("m_Colors"))) {
                        for (Map.Entry<String, Object> e : map(entry).entrySet()) {
                            if ("_Color".equals(e.getKey()) || "_TintColor".equals(e.getKey())) {
                                Map<String, Object> c = map(e.getValue());
                                color = (channel(c.get("a"), 255) << 24) | (channel(c.get("r"), 255) << 16)
                                        | (channel(c.get("g"), 255) << 8) | channel(c.get("b"), 255);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new SceneException(file + ": " + e, e);
            }
        }
        if (sprite == null) {
            // Unity's default particle is a soft white dot, and that is
            // what is drawn for a material with no texture of the
            // project's: this one is made here, not copied.
            String resource = "unity-default-particle.png";
            if (!generatedImages.containsKey(resource)) {
                generatedImages.put(resource, drawSoftDot(64));
            }
            once(notes, origin + ": " + user + " has a material with no texture of the project's; its particles"
                    + " are drawn as soft white dots");
            sprite = spriteMethod("package/" + resource, "new " + RUNTIME + "Sprite(" + string(resource)
                    + ", 64, 64, 64.0f, 0.5f, 0.5f)");
        }
        line(sb, v + ".$setup(" + sprite + ", " + color + ", " + f(p.get("m_MinParticleSize"), "0") + ", "
                + f(p.get("m_MaxParticleSize"), "0.5") + ");");
    }

    /// An expression for a sprite that is the whole of an image, however
    /// the image is imported, or null.
    private String wholeImage(String guid) {
        TextureAsset a = texturesByGuid.get(guid);
        if (a == null || a.image == null || !a.image.isFile()) {
            return null;
        }
        int[] size;
        try {
            size = imageSize(a.image);
        } catch (IOException e) {
            size = null;
        }
        if (size == null) {
            return null;
        }
        // A particle's size is its width in units, whatever the image's.
        return spriteMethod(guid + "/whole", "new " + RUNTIME + "Sprite(" + string(ship(a)) + ", " + size[0]
                + ", " + size[1] + ", " + size[0] + ".0f, 0.5f, 0.5f)");
    }

    private static byte[] drawSoftDot(int size) {
        BufferedImage image = new BufferedImage(size, size,
                BufferedImage.TYPE_INT_ARGB);
        double middle = (size - 1) / 2.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double d = Math.sqrt((x - middle) * (x - middle) + (y - middle) * (y - middle)) / (size / 2.0);
                double a = d >= 1 ? 0 : (1 - d) * (1 - d);
                image.setRGB(x, y, ((int) Math.round(a * 255) << 24) | 0xffffff);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new SceneException("the default particle: " + e, e);
        }
        return out.toByteArray();
    }

    /// The two TextMesh Pro components. An older file keeps the alignment
    /// as one number, a newer one as two.
    private void textMesh(String origin, Node n, Graph g, StringBuilder sb) {
        Map<String, Object> p = n.doc.properties;
        String v = n.variable;
        enabled(sb, v, p);
        long alignment = Long.parseLong(integer(p.get("m_textAlignment"), "65535"));
        if (alignment == 65535 || p.containsKey("m_HorizontalAlignment")) {
            alignment = Long.parseLong(integer(p.get("m_HorizontalAlignment"), "1"))
                    | Long.parseLong(integer(p.get("m_VerticalAlignment"), "256"));
        }
        boolean wraps = p.containsKey("m_TextWrappingMode") ? !"0".equals(integer(p.get("m_TextWrappingMode"), "1"))
                : !"0".equals(text(p.get("m_enableWordWrapping")));
        line(sb, v + ".$setup(" + string(text(p.get("m_text"))) + ", " + f(p.get("m_fontSize"), "36") + ", "
                + f(p.get("m_fontSizeMin"), "18") + ", " + f(p.get("m_fontSizeMax"), "72") + ", "
                + "1".equals(text(p.get("m_enableAutoSizing"))) + ", " + wraps + ", (int) " + alignment + "L, (int) "
                + integer(p.get("m_fontStyle"), "0") + "L);");
        Map<String, Object> c = map(p.get("m_fontColor"));
        if (!c.isEmpty()) {
            line(sb, v + ".$color(" + f(c.get("r"), "1") + ", " + f(c.get("g"), "1") + ", " + f(c.get("b"), "1")
                    + ", " + f(c.get("a"), "1") + ");");
        }
        Map<String, Object> margin = map(p.get("m_margin"));
        if (!margin.isEmpty() && (Double.parseDouble(number(margin.get("x"), "0")) != 0d
                || Double.parseDouble(number(margin.get("y"), "0")) != 0d
                || Double.parseDouble(number(margin.get("z"), "0")) != 0d
                || Double.parseDouble(number(margin.get("w"), "0")) != 0d)) {
            line(sb, v + ".$margins(" + f(margin.get("x"), "0") + ", " + f(margin.get("y"), "0") + ", "
                    + f(margin.get("z"), "0") + ", " + f(margin.get("w"), "0") + ");");
        }
        if ("TMPro.TextMeshPro".equals(n.cilType)) {
            UnityYaml.Document mesh = g.meshRenderers.get(fileId(p.get("m_GameObject")));
            if (mesh != null) {
                String order = integer(mesh.properties.get("m_SortingOrder"), "0");
                String layer = integer(mesh.properties.get("m_SortingLayerID"), "0");
                if (!"0".equals(order) || !"0".equals(layer)) {
                    line(sb, v + ".$sorting((int) " + layer + "L, (int) " + order + "L);");
                }
            }
        }
        once(notes, origin + ": the scene has TextMesh Pro text; it is drawn in the platform's font at the size"
                + " asked for, without the font asset, its material or rich text");
    }

    private void cinemachine(String origin, Node n, Map<Long, Node> nodes, StringBuilder sb) {
        Map<String, Object> p = n.doc.properties;
        String v = n.variable;
        enabled(sb, v, p);
        if ("Cinemachine.CinemachineVirtualCamera".equals(n.cilType)) {
            line(sb, v + ".$setup((int) " + integer(p.get("m_Priority"), "10") + "L, "
                    + f(map(p.get("m_Lens")).get("OrthographicSize"), "5") + ");");
            final String[] targets = {"m_Follow", "m_LookAt"};
            for (String target : targets) {
                Node to = nodes.get(fileId(p.get(target)));
                if (to != null && isTransform(to)) {
                    line(sb, v + "." + target + " = " + to.variable + ";");
                } else if (fileId(p.get(target)).longValue() != 0) {
                    warnings.add(origin + ": the " + the("virtual camera", origin, n.doc.fileId) + " has a "
                            + target.substring(2) + " target that is not a transform of the file; it was left"
                            + " null");
                }
            }
            once(notes, origin + ": the scene has Cinemachine cameras; the runtime has its own virtual camera,"
                    + " which follows a target with a framing transposer or a transposer and cuts between"
                    + " cameras -- no blends, aiming, noise or extensions");
        } else if ("Cinemachine.CinemachineFramingTransposer".equals(n.cilType)) {
            Map<String, Object> offset = map(p.get("m_TrackedObjectOffset"));
            line(sb, v + ".$setup(" + f(offset.get("x"), "0") + ", " + f(offset.get("y"), "0") + ", "
                    + f(p.get("m_XDamping"), "1") + ", " + f(p.get("m_YDamping"), "1") + ", "
                    + f(p.get("m_ScreenX"), "0.5") + ", " + f(p.get("m_ScreenY"), "0.5") + ", "
                    + f(p.get("m_CameraDistance"), "10") + ", " + f(p.get("m_DeadZoneWidth"), "0") + ", "
                    + f(p.get("m_DeadZoneHeight"), "0") + ", " + "1".equals(text(p.get("m_UnlimitedSoftZone"))) + ", "
                    + f(p.get("m_SoftZoneWidth"), "0.8") + ", " + f(p.get("m_SoftZoneHeight"), "0.8") + ", "
                    + !"0".equals(text(p.get("m_CenterOnActivate"))) + ");");
            if (Double.parseDouble(number(p.get("m_LookaheadTime"), "0")) != 0d) {
                notes.add(origin + ": the " + the("framing transposer", origin, n.doc.fileId) + " looks ahead of"
                        + " its target, which is not supported; it frames the target where it is");
            }
        } else if ("Cinemachine.CinemachineTransposer".equals(n.cilType)) {
            Map<String, Object> offset = map(p.get("m_FollowOffset"));
            line(sb, v + ".$setup(" + f(offset.get("x"), "0") + ", " + f(offset.get("y"), "0") + ", "
                    + f(offset.get("z"), "-10") + ", " + f(p.get("m_XDamping"), "1") + ", "
                    + f(p.get("m_YDamping"), "1") + ");");
        }
    }

    // ---------------------------------------------------------------- prefabs

    /// The prefab a GUID names, read on first use, or null with a warning.
    private Prefab prefab(String origin, String user, String guid) {
        Prefab p = prefabsByGuid.get(guid);
        if (p != null) {
            return p;
        }
        File file = prefabFilesByGuid.get(guid);
        if (file == null || !file.isFile()) {
            warnings.add(origin + ": " + user + " refers to the asset with GUID " + guid + ", which is not a prefab"
                    + " or an image under Assets; it was left null");
            return null;
        }
        p = new Prefab();
        p.index = prefabs.size();
        p.file = file;
        p.label = relative(project, file);
        Graph g;
        try {
            g = graph(file.getName(), UnityYaml.parse(read(file)));
        } catch (IOException e) {
            throw new SceneException(file + ": " + e, e);
        }
        p.nodes = g.nodes;
        p.order = g.order;
        p.meshRenderers = g.meshRenderers;
        int part = 0;
        for (Node n : g.order) {
            n.part = part++;
            if (p.root == null && isTransform(n) && fileId(n.doc.properties.get("m_Father")).longValue() == 0) {
                p.root = g.nodes.get(fileId(n.doc.properties.get("m_GameObject")));
            }
        }
        if (p.root == null) {
            warnings.add(origin + ": " + user + " refers to " + p.label + ", which has no root object; it was left"
                    + " null");
            return null;
        }
        prefabsByGuid.put(guid, p);
        prefabs.add(p);
        return p;
    }

    /// A prefab becomes three methods: one that builds a copy of it, one
    /// that returns its template -- a copy built once and kept out of the
    /// scene, which is what a script field holds -- and one that returns
    /// an object of the template by its place, for a field that refers to
    /// a component of the prefab and not to its root.
    private void emitPrefab(Prefab p, StringBuilder methods) {
        String go = RUNTIME + "GameObject";
        String i = String.valueOf(p.index);
        Graph g = new Graph();
        g.origin = p.file.getName();
        g.nodes.putAll(p.nodes);
        g.order.addAll(p.order);
        g.meshRenderers = p.meshRenderers;
        methods.append("\n    /// ").append(javaComment(p.label)).append("\n");
        methods.append("    private static ").append(go).append(" buildPrefab_").append(i)
                .append("(java.lang.Object[] parts) {\n");
        emit(g, methods, true);
        line(methods, "return " + p.root.variable + ";");
        methods.append("    }\n\n");
        methods.append("    private static java.lang.Object[] prefabParts_").append(i).append(";\n");
        methods.append("    private static boolean prefabBuilding_").append(i).append(";\n\n");
        methods.append("    private static java.lang.Object prefabPart_").append(i).append("(int part) {\n");
        // A prefab that refers to itself finds its template half built;
        // the reference is left null, and Instantiate of a null says so.
        line(methods, "if (prefabParts_" + i + " == null) {");
        line(methods, "    if (prefabBuilding_" + i + ") {");
        line(methods, "        return null;");
        line(methods, "    }");
        line(methods, "    prefabBuilding_" + i + " = true;");
        line(methods, "    java.lang.Object[] parts = new java.lang.Object[" + p.order.size() + "];");
        line(methods, "    int mark = " + RUNTIME + "UnityRuntime.$beginAsset();");
        line(methods, "    " + go + " root = buildPrefab_" + i + "(parts);");
        line(methods, "    " + RUNTIME + "UnityRuntime.$endAsset(mark, root, " + i + ");");
        line(methods, "    prefabParts_" + i + " = parts;");
        line(methods, "    prefabBuilding_" + i + " = false;");
        line(methods, "}");
        line(methods, "return prefabParts_" + i + "[part];");
        methods.append("    }\n");
    }

    // --------------------------------------------------------------- settings

    private Map<String, Object> setting(String file) throws IOException {
        File f = new File(project, "ProjectSettings/" + file);
        if (!f.isFile()) {
            return null;
        }
        for (UnityYaml.Document d : UnityYaml.parse(read(f))) {
            if (d.properties != null) {
                return d.properties;
            }
        }
        return null;
    }

    private void settings(StringBuilder sb) throws IOException {
        Map<String, Object> physics = setting("Physics2DSettings.asset");
        if (physics != null) {
            Map<String, Object> gravity = map(physics.get("m_Gravity"));
            line(sb, RUNTIME + "Physics2D.$settings(" + f(gravity.get("x"), "0") + ", " + f(gravity.get("y"), "-9.81")
                    + ", (int) " + integer(physics.get("m_VelocityIterations"), "8") + "L, (int) "
                    + integer(physics.get("m_PositionIterations"), "3") + "L);");
            // 32 rows of 32 bits, each written as four bytes, low byte
            // first: row i has bit j set when layers i and j collide.
            String matrix = text(physics.get("m_LayerCollisionMatrix"));
            if (matrix.length() >= 256) {
                StringBuilder rows = new StringBuilder();
                boolean all = true;
                for (int row = 0; row < 32; row++) {
                    long bits = 0;
                    for (int b = 0; b < 4; b++) {
                        int at = row * 8 + b * 2;
                        bits |= Long.parseLong(matrix.substring(at, at + 2), 16) << (b * 8);
                    }
                    all &= bits == 0xffffffffL;
                    rows.append("0x").append(Long.toHexString(bits)).append(row == 31 ? "" : ", ");
                }
                if (!all) {
                    line(sb, RUNTIME + "Physics2D.$layerMatrix(new int[] {" + rows + "});");
                }
            }
            line(sb, RUNTIME + "Physics2D.$querySettings(" + !"0".equals(text(physics.get("m_QueriesHitTriggers")))
                    + ", " + !"0".equals(text(physics.get("m_QueriesStartInColliders"))) + ", "
                    + "1".equals(text(physics.get("m_AutoSyncTransforms"))) + ");");
        }
        Map<String, Object> time = setting("TimeManager.asset");
        if (time != null) {
            line(sb, RUNTIME + "UnityRuntime.$fixedTimestep(" + f(time.get("Fixed Timestep"), "0.02") + ");");
        }
        Map<String, Object> tags = setting("TagManager.asset");
        if (tags != null) {
            StringBuilder ids = new StringBuilder();
            for (Object layer : list(tags.get("m_SortingLayers"))) {
                ids.append(ids.length() == 0 ? "" : ", ").append("(int) ")
                        .append(integer(map(layer).get("uniqueID"), "0")).append("L");
            }
            line(sb, RUNTIME + "UnityRuntime.$sortingLayers(new int[] {" + ids + "});");
            // The names of the 32 layers, in order; one with no name is
            // written as an empty entry.
            List<Object> layers = list(tags.get("layers"));
            if (!layers.isEmpty()) {
                StringBuilder names = new StringBuilder();
                for (Object layer : layers) {
                    names.append(names.length() == 0 ? "" : ", ").append(string(text(layer)));
                }
                line(sb, RUNTIME + "LayerMask.$layerNames(new String[] {" + names + "});");
            }
        }
        Map<String, Object> input = setting("InputManager.asset");
        if (input != null) {
            boolean joystick = false;
            line(sb, RUNTIME + "Input.$clearAxes();");
            for (Object entry : list(input.get("m_Axes"))) {
                Map<String, Object> axis = map(entry);
                // 0 is a key or a button and 1 a movement of the mouse; a
                // joystick axis has no keys, and reads as zero.
                String type = integer(axis.get("type"), "0");
                boolean keys = "0".equals(type);
                if ("1".equals(type)) {
                    line(sb, RUNTIME + "Input.$mouseAxis(" + string(text(axis.get("m_Name"))) + ", (int) "
                            + integer(axis.get("axis"), "0") + "L, " + f(axis.get("sensitivity"), "0.1") + ");");
                    continue;
                }
                joystick |= !keys;
                String[] buttons = {"negativeButton", "positiveButton", "altNegativeButton", "altPositiveButton"};
                StringBuilder call = new StringBuilder(RUNTIME + "Input.$addAxis(" + string(text(axis.get("m_Name"))));
                for (String button : buttons) {
                    call.append(", ").append(RUNTIME).append("Input.$keyCode(")
                            .append(string(keys ? text(axis.get(button)) : "")).append(")");
                }
                line(sb, call + ");");
            }
            if (joystick) {
                notes.add("the input settings define joystick axes; those read as zero, and the keys of the same"
                        + " axes work");
            }
        }
    }

    // ------------------------------------------------------------- utilities

    private static void line(StringBuilder sb, String code) {
        sb.append("        ").append(code).append('\n');
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object o) {
        return o instanceof List ? (List<Object>) o : new ArrayList<Object>();
    }

    static String text(Object o) {
        return o instanceof String ? (String) o : "";
    }

    static Long fileId(Object reference) {
        String id = text(map(reference).get("fileID"));
        try {
            return Long.valueOf(id.length() == 0 ? "0" : id);
        } catch (NumberFormatException e) {
            return Long.valueOf(0);
        }
    }

    /// A number as the scene wrote it, checked to be one so that nothing
    /// from a scene file is pasted into source unread.
    static String number(Object o, String fallback) {
        String s = text(o);
        if (s.length() == 0) {
            return fallback;
        }
        try {
            double d = Double.parseDouble(s);
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return fallback;
            }
            return Double.toString(d);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static String f(Object o, String fallback) {
        return number(o, fallback) + "f";
    }

    static String integer(Object o, String fallback) {
        String s = text(o);
        try {
            return Long.toString(Long.parseLong(s.length() == 0 ? fallback : s));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static String string(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c >= ' ' && c < 127) {
                sb.append(c);
            } else if (c < ' ') {
                // Never a unicode escape: javac reads those before it reads
                // the literal, and one for a line end would end the line.
                String octal = Integer.toOctalString(c);
                sb.append('\\').append("000".substring(octal.length())).append(octal);
            } else {
                String hex = Integer.toHexString(c);
                sb.append("\\u").append("0000".substring(hex.length())).append(hex);
            }
        }
        return sb.append('"').toString();
    }
}
