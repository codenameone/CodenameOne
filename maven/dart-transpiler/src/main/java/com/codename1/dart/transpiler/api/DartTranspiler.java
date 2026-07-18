package com.codename1.dart.transpiler.api;

import com.codename1.dart.transpiler.analyze.Program;
import com.codename1.dart.transpiler.analyze.StubRegistry;
import com.codename1.dart.transpiler.ast.Ast;
import com.codename1.dart.transpiler.codegen.JavaEmitter;
import com.codename1.dart.transpiler.parser.AstBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Whole-program Dart-to-Java transpiler entry point (called by the
 * Codename One Maven plugin's transcode-flutter goal and by tests).
 *
 * <p>Incrementality is whole-program: a digest over every input file plus
 * the transpiler version is compared against {@code stateFile}; on match the
 * transpile is skipped, otherwise everything regenerates. Output writes are
 * content-stable — unchanged files are not rewritten, keeping downstream
 * javac incremental.</p>
 */
public final class DartTranspiler {

    /** Bumped whenever emission changes so stale state files don't skip. */
    private static final String VERSION = "m1-1";

    public TranspileResult transpile(TranspileRequest req) {
        Diagnostics diags = new Diagnostics();
        List<File> dartFiles = new ArrayList<File>();
        for (File root : req.sourceRoots) {
            collectDartFiles(root, root, dartFiles);
        }
        Collections.sort(dartFiles);

        String digest = digest(dartFiles) + stubDigest(req.stubClasspath);
        if (req.stateFile != null && req.stateFile.exists() && req.outputDir != null && req.outputDir.exists()) {
            try {
                String prev = new String(Files.readAllBytes(req.stateFile.toPath()), StandardCharsets.UTF_8).trim();
                if (prev.equals(digest)) {
                    return new TranspileResult(diags.asList(), new ArrayList<GeneratedFile>(), true);
                }
            } catch (IOException ignore) {
                // fall through to full transpile
            }
        }

        Program program = new Program();
        AstBuilder builder = new AstBuilder(diags);
        for (File f : dartFiles) {
            try {
                String src = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                String rel = relativize(req.sourceRoots, f);
                program.add(builder.parse(rel, src));
            } catch (IOException e) {
                diags.error(f.getName(), 0, 0, "E0003", "Cannot read file: " + e);
            }
        }

        StubRegistry stubs = req.stubClasspath.isEmpty()
                ? StubRegistry.loadEmbedded(diags)
                : StubRegistry.loadFromClasspath(req.stubClasspath, diags);
        JavaEmitter emitter = new JavaEmitter(program, stubs, diags, req.packageName);
        List<GeneratedFile> files = emitter.emit();

        if (!diags.hasErrors() && req.outputDir != null) {
            writeOutput(req, files, diags);
            if (req.stateFile != null) {
                try {
                    req.stateFile.getParentFile().mkdirs();
                    Files.write(req.stateFile.toPath(), digest.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    diags.warn(null, "W0001", "Could not write transpiler state file: " + e);
                }
            }
        }
        return new TranspileResult(diags.asList(), files, false);
    }

    private void writeOutput(TranspileRequest req, List<GeneratedFile> files, Diagnostics diags) {
        File pkgDir = new File(req.outputDir, req.packageName.replace('.', File.separatorChar));
        pkgDir.mkdirs();
        Set<String> expected = new HashSet<String>();
        for (GeneratedFile gf : files) {
            expected.add(gf.relativePath);
            File out = new File(pkgDir, gf.relativePath);
            out.getParentFile().mkdirs();
            try {
                byte[] content = gf.content.getBytes(StandardCharsets.UTF_8);
                if (out.exists() && Arrays.equals(Files.readAllBytes(out.toPath()), content)) {
                    continue;  // content-stable: keep timestamp for incremental javac
                }
                Files.write(out.toPath(), content);
            } catch (IOException e) {
                diags.error(gf.relativePath, 0, 0, "E0004", "Cannot write generated file: " + e);
            }
        }
        // sweep stale generated files
        File[] existing = pkgDir.listFiles();
        if (existing != null) {
            for (File f : existing) {
                if (f.isFile() && f.getName().endsWith(".java") && !expected.contains(f.getName())) {
                    f.delete();
                }
            }
        }
    }

    private void collectDartFiles(File root, File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                if (!f.getName().equals("assets")) {
                    collectDartFiles(root, f, out);
                }
            } else if (f.getName().endsWith(".dart")) {
                out.add(f);
            }
        }
    }

    private String relativize(List<File> roots, File f) {
        for (File root : roots) {
            String rootPath = root.getAbsolutePath();
            String path = f.getAbsolutePath();
            if (path.startsWith(rootPath)) {
                String rel = path.substring(rootPath.length());
                if (rel.startsWith(File.separator)) {
                    rel = rel.substring(1);
                }
                return rel.replace(File.separatorChar, '/');
            }
        }
        return f.getName();
    }

    private String stubDigest(List<File> stubClasspath) {
        if (stubClasspath.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("-stubs");
        for (File f : stubClasspath) {
            sb.append('|').append(f.getAbsolutePath()).append('@').append(f.lastModified());
        }
        return Integer.toHexString(sb.toString().hashCode());
    }

    private String digest(List<File> files) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(VERSION.getBytes(StandardCharsets.UTF_8));
            for (File f : files) {
                md.update(f.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
                md.update(Files.readAllBytes(f.toPath()));
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "no-digest-" + System.nanoTime();
        }
    }
}
