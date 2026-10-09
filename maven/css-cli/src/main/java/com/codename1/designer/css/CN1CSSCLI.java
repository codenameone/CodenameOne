/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.designer.css;

import com.codename1.project.ProjectLayout;
import com.codename1.project.ProjectLayouts;
import com.codename1.tools.resourcebuilder.PropertiesUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/// The CSS compiler's command line: turns a project's CSS into a `theme.res`.
///
/// The Maven `css` goal, the Gradle `cn1Css` task and the simulator's live
/// CSS reload all fork this class. It runs without a display: the theme is
/// built on [HeadlessCssCompilerImplementation], and a rule that has no native
/// equivalent is painted in process rather than in a browser. `main` sets
/// `java.awt.headless` so that stays true even if the caller forgot to.
///
/// Around the compile itself it provides what a project build needs: merging
/// the stylesheets of installed libraries ahead of the application's own,
/// bundling localization `.properties` files, skipping a compile whose inputs
/// are older than its output, and a watch mode that recompiles on change and
/// tells the simulator to reload.
public class CN1CSSCLI {

    /// Exit status when the stylesheet could not be compiled.
    static final int EXIT_COMPILE_FAILED = 1;

    /// Exit status when the command line could not be understood.
    static final int EXIT_USAGE = 2;

    /// Printed on standard output, on a line of its own, after a recompile in
    /// watch mode. The simulator's CSS watcher reads this process's output and
    /// reloads the theme when it sees the line.
    static final String REFRESH_SIGNAL = "::refresh::";

    /// The system property a build passes when it has moved the project's build
    /// directory (Gradle's layout.buildDirectory), which detection by the
    /// directory tree cannot see.
    static final String BUILD_DIR_PROPERTY = "cn1.buildDir";

    /// Name of the file, in the project's CSS state directory, that is locked
    /// for the length of a compile. A build and the simulator's watcher can
    /// both be compiling the same output, and the incremental state must not be
    /// read by one while the other is writing it. The file holds nothing; it
    /// keeps the name it had when it also stored checksums, so two versions of
    /// the compiler still exclude each other.
    private static final String LOCK_FILE_NAME = ".cn1_css_checksums";

    private static final class Options {
        File[] inputFiles;
        File outputFile;
        File mergeFile;
        File localizationDir;
        boolean watch;
        boolean noRaster;
        boolean nativeThemeUnits;

        static Options parse(String[] args) {
            Options o = new Options();
            String input = getArgByName(args, "i", "input");
            if (input == null || "true".equals(input)) {
                throw new IllegalArgumentException("an input stylesheet is required. Use -input <file.css>. "
                        + "The positional form `<file.css> [<file.res>]` is no longer accepted.");
            }
            o.inputFiles = getInputFiles(input);
            if (o.inputFiles.length == 0) {
                throw new IllegalArgumentException("-input names no file");
            }
            for (File f : o.inputFiles) {
                if (!f.isFile()) {
                    throw new IllegalArgumentException("the input stylesheet does not exist: " + f.getAbsolutePath());
                }
            }
            String output = getArgByName(args, "o", "output");
            if (output == null || "true".equals(output)) {
                throw new IllegalArgumentException("an output file is required. Use -output <file.res>");
            }
            o.outputFile = new File(output);
            String merge = getArgByName(args, "m", "merge");
            if ("true".equals(merge)) {
                throw new IllegalArgumentException("-merge needs a file path");
            }
            if (merge != null) {
                o.mergeFile = new File(merge);
            } else if (o.inputFiles.length > 1) {
                throw new IllegalArgumentException("several input files need somewhere to be merged. "
                        + "Use -merge <file>");
            }
            String localization = getArgByName(args, "l", "localization");
            if ("true".equals(localization)) {
                throw new IllegalArgumentException("-localization needs a directory");
            }
            o.localizationDir = localization == null ? null : new File(localization);
            o.watch = hasFlag(args, "w", "watch");
            o.noRaster = hasFlag(args, "no-raster");
            o.nativeThemeUnits = hasFlag(args, "native-theme-units");
            return o;
        }
    }

    private static boolean hasFlag(String[] args, String... names) {
        List<String> namesList = Arrays.asList(names);
        for (String arg : args) {
            if (arg.length() > 1 && arg.charAt(0) == '-' && namesList.contains(arg.substring(1))) {
                return true;
            }
        }
        return false;
    }

    private static String getArgByName(String[] args, String... names) {
        int len = args.length;
        List<String> namesList = Arrays.asList(names);
        for (int i=0; i<len; i++) {
            String arg = args[i];
            if (arg.length() > 0 && arg.charAt(0) == '-' && namesList.contains(arg.substring(1))) {
                if (i + 1 < len) {
                    String nextArg = args[i+1];
                    if (nextArg.length() > 0 && nextArg.charAt(0) == '-') {
                        // If the next arg is another flag, then just return value "true"
                        // to confirm that the flag exists.
                        return "true";
                    } else {
                        return nextArg;
                    }
                } else {
                    // If this is the last arg, then just return "true" to verify that the
                    // flag exists.
                    return "true";
                }
            }
        }
        return null;
    }

    private static void printUsage() {
        System.out.println("Codename One CSS Compiler");
        System.out.println("Usage:\n");
        System.out.println(" java -cp <classpath> " + CN1CSSCLI.class.getName()
                + " -input <file.css> -output <file.res> OPTIONS...\n");
        System.out.println("Options:");
        System.out.println(" -i, -input         Input CSS file path.  Multiple files separated by commas.");
        System.out.println(" -o, -output        Output res file path.");
        System.out.println(" -m, -merge         Path of the file the inputs are merged into. Required with several inputs.");
        System.out.println(" -l, -localization  Directory containing Java resource bundle .properties files to include.");
        System.out.println(" -w, -watch         Run in watch mode.");
        System.out.println("                    Watches input files for changes and automatically recompiles.");
        System.out.println(" -no-raster         Fail, listing the offending rules, if any rule needs a generated image");
        System.out.println("                    (a 9-piece border or a stretched background) instead of a native primitive.");
        System.out.println(" -native-theme-units  Convert lengths the way the framework's own native themes are built:");
        System.out.println("                    a round border gets no default shadow spread. For those themes only;");
        System.out.println("                    an application theme compiled with it changes size.");
        System.out.println("\nSystem Properties:");
        System.out.println(" parent.port        A local port to connect to in watch mode. The compiler exits when the");
        System.out.println("                    connection closes, so it does not outlive the process that started it.");
        System.out.println(" " + BUILD_DIR_PROPERTY + "        The project's build directory, when the build has moved it.");
    }

    public static void main(String[] args) {
        // Before anything can load an AWT class. The compiler uses AWT only for
        // what needs no display (images, font metadata); stating that here turns
        // an accidental use of a window or a dialog into an immediate
        // HeadlessException instead of a build that works on a developer's
        // machine and fails on a server.
        System.setProperty("java.awt.headless", "true");
        System.exit(run(args));
    }

    /// Runs the compiler and reports the outcome as an exit status instead of
    /// ending the JVM, for callers in the same process.
    ///
    /// #### Returns
    ///
    /// 0 when the output is up to date, [#EXIT_COMPILE_FAILED] when the
    /// stylesheet could not be compiled, [#EXIT_USAGE] for a bad command line.
    /// In watch mode it returns only once the parent process has gone.
    public static int run(String[] args) {
        if (hasFlag(args, "help", "h")) {
            printUsage();
            return 0;
        }
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException ex) {
            System.err.println("CSS compile failed: " + ex.getMessage());
            System.err.println("Run with -help for the options.");
            return EXIT_USAGE;
        }
        HeadlessCssCompilerImplementation.install();
        HeadlessCssCompilerImplementation.setNativeThemeUnits(options.nativeThemeUnits);
        if (options.watch) {
            return watch(options);
        }
        try {
            if (compileOnce(options)) {
                System.out.println("CSS file successfully compiled.  " + options.outputFile);
            }
            return 0;
        } catch (Throwable t) {
            report(t);
            return EXIT_COMPILE_FAILED;
        }
    }

    private static void report(Throwable t) {
        // One line a build log can be searched for, then the detail.
        System.err.println("CSS compile failed: " + (t.getMessage() == null ? t.toString() : t.getMessage()));
        t.printStackTrace();
    }

    /// Brings the output up to date.
    ///
    /// #### Returns
    ///
    /// true when the output was written, false when it was already newer than
    /// everything it is built from.
    private static boolean compileOnce(Options options) throws IOException {
        File css = options.mergeFile != null ? options.mergeFile : options.inputFiles[0];
        File outputDir = options.outputFile.getAbsoluteFile().getParentFile();
        if (outputDir != null && !outputDir.isDirectory() && !outputDir.mkdirs()) {
            throw new IOException("Could not create the output directory " + outputDir);
        }
        // The project is found from where the stylesheet is, which is known
        // before a merged one has been written for the first time.
        ProjectLayout layout = getLayout(css.isFile() ? css : css.getAbsoluteFile().getParentFile());
        if (layout == null && options.mergeFile != null) {
            // A build directory can be outside the project, and the merged
            // stylesheet with it. The stylesheets it is merged from are not.
            layout = getLayout(options.inputFiles[0]);
        }
        if (layout == null) {
            // Not inside a project (the framework's own theme build, a one-off
            // compile). There is no state directory to keep anything in, so the
            // compile is unconditional and leaves nothing behind but its output.
            if (options.mergeFile != null) {
                updateMergeFile(options.inputFiles, options.mergeFile);
            }
            compile(css, options, false);
            return true;
        }
        File stateDir = layout.cssChecksumDir();
        if (!stateDir.isDirectory() && !stateDir.mkdirs()) {
            throw new IOException("Could not create " + stateDir);
        }
        RandomAccessFile lockFile = new RandomAccessFile(new File(stateDir, LOCK_FILE_NAME), "rw");
        try {
            FileChannel channel = lockFile.getChannel();
            FileLock lock = channel.lock();
            try {
                // The merged stylesheet is rewritten under the lock. A build
                // and the simulator's watcher compile the same theme, and one
                // must not replace the file the other is parsing.
                if (options.mergeFile != null) {
                    updateMergeFile(options.inputFiles, options.mergeFile);
                }
                if (isUpToDate(css, options)) {
                    System.out.println("File has not changed since last compile.");
                    return false;
                }
                compile(css, options, true);
                return true;
            } finally {
                lock.release();
            }
        } finally {
            lockFile.close();
        }
    }

    /// Everything, besides the stylesheet handed to the parser, that the
    /// output depends on: the files it imports and the localization bundles.
    private static List<File> dependencies(File css, Options options) {
        List<File> out = new ArrayList<File>();
        // A broken import is the compile's error to report, with its message.
        // The file it names is still a dependency: it appearing is a change.
        out.addAll(CssImports.collectReachable(css));
        try {
            // The images and fonts too. An edit to one changes the theme and
            // no stylesheet; in a merged build the copy in the mirror is what
            // the merged file names, and refreshing it moves its time.
            out.addAll(CssImports.assets(css));
        } catch (IOException ex) {
            // As above: the compile reports it.
        }
        if (options.localizationDir != null && options.localizationDir.isDirectory()) {
            collectLocalizationFiles(options.localizationDir, out);
        }
        return out;
    }

    private static void collectLocalizationFiles(File dir, List<File> out) {
        // The directory too: adding or removing a bundle changes its own
        // modification time and nothing else.
        out.add(dir);
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectLocalizationFiles(child, out);
            } else if (child.getName().endsWith(".properties")) {
                out.add(child);
            }
        }
    }

    private static boolean isUpToDate(File css, Options options) {
        if (options.noRaster || options.nativeThemeUnits) {
            // Modification times cannot tell which mode an existing output
            // was built in. -no-raster is a check as much as a compile, and
            // skipping it would report a stylesheet clean without looking;
            // -native-theme-units changes the numbers in the result. Both
            // belong to the native theme build, which always compiles.
            return false;
        }
        // The output is what this compiler builds and nothing else: it is
        // overwritten whenever the stylesheet is newer, and no copy of the old
        // one is kept. A resource file edited by hand belongs under another
        // name; one that shares the output's name is replaced by the next
        // compile, which is also what a build directory has always done.
        File output = options.outputFile;
        if (!output.exists()) {
            return false;
        }
        long built = output.lastModified();
        if (css.lastModified() > built) {
            return false;
        }
        if (!new File(css.getAbsoluteFile().getParentFile(), css.getName() + ".checksums").exists()) {
            // Every compile this check follows leaves that file, except one
            // in a native theme mode, which removes it. An output without it
            // was built with other units, or by something else altogether.
            return false;
        }
        try {
            CssImports.collect(css);
        } catch (IOException ex) {
            // An import that cannot be followed -- a file deleted since the
            // last build -- is for the compile to report. Calling the old
            // output current would hide it.
            return false;
        }
        for (File dependency : dependencies(css, options)) {
            if (dependency.lastModified() > built) {
                return false;
            }
        }
        return true;
    }

    private static void compile(File css, Options options, boolean incremental) throws IOException {
        File outputFile = options.outputFile;
        CSSTheme theme = CSSTheme.load(css.toURI().toURL());
        if (theme == null) {
            throw new IOException("The CSS parser could not be started for " + css);
        }
        theme.cssFile = css;
        theme.resourceFile = outputFile;
        theme.setRasterizationAllowed(!options.noRaster);

        // The per-selector cache lets a recompile reuse the generated images of
        // rules that did not change.
        File cacheFile = new File(css.getAbsoluteFile().getParentFile(), css.getName() + ".checksums");
        // The cache knows a rule by its CSS alone. It cannot tell that an
        // image the rule names was edited, nor which units or which raster
        // policy the existing output was built with, so in each of those
        // cases nothing is reused and every rule is built again.
        boolean nativeThemeMode = options.noRaster || options.nativeThemeUnits;
        if (nativeThemeMode && cacheFile.exists() && !cacheFile.delete()) {
            throw new IOException("Could not delete " + cacheFile);
        }
        if (incremental && !nativeThemeMode && outputFile.exists() && cacheFile.exists()
                && !assetsNewerThan(css, cacheFile.lastModified())) {
            theme.loadResourceFile();
            theme.loadSelectorCacheStatus(cacheFile);
        }

        Map<String, Map<String, Map<String, String>>> localizationBundles =
                loadLocalizationBundles(options.localizationDir);

        theme.createImageBorders();
        theme.updateResources();
        if (!localizationBundles.isEmpty()) {
            theme.applyLocalizationBundles(localizationBundles);
        }
        theme.save(outputFile);
        if (incremental && !nativeThemeMode) {
            theme.saveSelectorChecksums(cacheFile);
        }
    }

    private static boolean assetsNewerThan(File css, long time) {
        try {
            for (File asset : CssImports.assets(css)) {
                if (asset.lastModified() > time) {
                    return true;
                }
            }
            return false;
        } catch (IOException ex) {
            // Cannot tell, so nothing is assumed unchanged.
            return true;
        }
    }

    /// Watch mode: compile, then recompile whenever a source changes, until the
    /// process that started this one goes away.
    private static int watch(Options options) {
        startParentPulse(Thread.currentThread());
        long reported = options.outputFile.lastModified();
        boolean first = true;
        while (!Thread.currentThread().isInterrupted()) {
            // Snapshot the sources before compiling, not after: an edit made
            // while the compile is running then shows up as a change on the
            // next poll instead of being absorbed into the baseline.
            PollingFileWatcher watcher = new PollingFileWatcher(watchedFiles(options), 1000);
            try {
                if (compileOnce(options) && first) {
                    System.out.println("CSS file successfully compiled.  " + options.outputFile);
                }
            } catch (Throwable t) {
                // A stylesheet that does not compile is the normal state of one
                // being edited. Report it and keep watching.
                report(t);
            }
            long now = options.outputFile.lastModified();
            if (!first && now != reported) {
                // Whoever wrote it: a build running beside the simulator can get
                // there first, and the theme on screen is stale either way.
                System.out.println(REFRESH_SIGNAL);
                System.out.flush();
            }
            reported = now;
            first = false;
            try {
                watcher.poll();
            } catch (InterruptedException parentGone) {
                break;
            }
            System.out.println("Change detected in " + Arrays.toString(options.inputFiles) + ".  Recompiling");
        }
        return 0;
    }

    private static File[] watchedFiles(Options options) {
        Set<File> files = new LinkedHashSet<File>(Arrays.asList(options.inputFiles));
        for (File input : options.inputFiles) {
            files.addAll(dependencies(input, options));
        }
        return files.toArray(new File[files.size()]);
    }

    /// Ends watch mode when the process that started it exits.
    ///
    /// The parent listens on `parent.port` and never writes; the read below
    /// returns only when the connection drops, which the operating system
    /// guarantees when the parent dies however it dies. Without this a
    /// simulator that crashed would leave a compiler polling forever.
    private static void startParentPulse(final Thread watchThread) {
        final String parentPort = System.getProperty("parent.port", null);
        if (parentPort == null) {
            return;
        }
        Thread pulseThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket sock = new Socket("127.0.0.1", Integer.parseInt(parentPort));
                    try {
                        sock.setKeepAlive(true);
                        InputStream is = sock.getInputStream();
                        while (is.read() >= 0) {
                        }
                    } finally {
                        sock.close();
                    }
                } catch (IOException ex) {
                    // Refused or reset: the parent is gone, which is the event
                    // this thread exists to notice.
                } catch (NumberFormatException ex) {
                    System.err.println("Ignoring parent.port=" + parentPort + ": not a port number");
                    return;
                }
                watchThread.interrupt();
            }
        }, "css-parent-pulse");
        pulseThread.setDaemon(true);
        pulseThread.start();
    }

    private static String prefixUrls(String contents, final String prefix) throws IOException {
        return CssImports.rewriteUrls(contents, new CssImports.UrlRewriter() {
            @Override
            public String rewrite(String url) {
                // Only a path relative to the stylesheet moves with it.
                return CssImports.isRelativeUrl(url) ? prefix + url : url;
            }
        });
    }

    private static void delTree(File dir) {
        for(File f : dir.listFiles()) {
            if(f.isDirectory()) {
                delTree(f);
            } else {
                f.delete();
            }
        }
    }
    private static void syncDirectories(File srcDir, File destDir) throws IOException {
        File canonicalSrc = srcDir.getCanonicalFile();
        File canonicalDest = destDir.getCanonicalFile();
        
        if (canonicalSrc.equals(canonicalDest)) {
            return;
        }
        
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        
        if (contains(srcDir, destDir) || contains(destDir, srcDir)) {
            throw new IllegalArgumentException("Cannot sync dir "+srcDir+" to "+destDir+" because one contains the other");
        }
        HashSet<String> destChildren = new HashSet<String>();
        String[] children = destDir.list();
        if (children != null) {
            for (String child : children) {
                destChildren.add(child);
            }
        }
        for (File child : srcDir.listFiles()) {
            
            String childName = child.getName();
            File destChild = new File(destDir, childName);
            if (destChildren.contains(childName)) {
                
                if (child.isDirectory()) {
                    if (destChild.isDirectory()) {
                        syncDirectories(child, destChild);
                    } else {
                        destChild.delete();
                        destChild.mkdir();
                        syncDirectories(child, destChild);
                    }
                } else {
                    if (destChild.isDirectory()) {
                        delTree(destChild);
                        Files.copy(child.toPath(), destChild.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }  else {
                        long destMtime = destChild.lastModified();
                        long srcMTime = child.lastModified();
                        if (destMtime < srcMTime) {
                            Files.copy(child.toPath(), destChild.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            } else {
                
                if (child.isDirectory()) {
                    destChild.mkdir();
                    syncDirectories(child, destChild);
                } else {
                    Files.copy(child.toPath(), destChild.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        
        
    }
    
    private static String getMd5(String input) 
    { 
        try { 
  
            // Static getInstance method is called with hashing MD5 
            MessageDigest md = MessageDigest.getInstance("MD5"); 
  
            // digest() method is called to calculate message digest 
            //  of an input digest() return array of byte 
            byte[] messageDigest = md.digest(input.getBytes()); 
  
            // Convert byte array into signum representation 
            BigInteger no = new BigInteger(1, messageDigest); 
  
            // Convert message digest into hex value 
            String hashtext = no.toString(16); 
            while (hashtext.length() < 32) { 
                hashtext = "0" + hashtext; 
            } 
            return hashtext; 
        }  
  
        // For specifying wrong message digest algorithms 
        catch (NoSuchAlgorithmException e) { 
            throw new RuntimeException(e); 
        } 
    } 
    
    /**
     * Checks if directory 1 contains directory 2
     * @param directory1
     * @param directory2
     * @return true if directory 1 contains directory 2
     */
    private static boolean contains(File directory1, File directory2) throws IOException {
        File canonical1 = directory1.getCanonicalFile();
        File canonical2 = directory2.getCanonicalFile();
        File parent2 = canonical2.getParentFile();
        if (parent2 == null) {
            return false;
        }
        if (canonical1.equals(parent2)) {
            return true;
        }
        // Walk directory2 up towards directory1. This passed directory2 as the first
        // argument, which dropped directory1 from the comparison after one level, so only
        // a direct parent was ever detected: contains(/a, /a/b/c) answered false.
        return contains(directory1, parent2);
    }

    /// Writes the one stylesheet the compiler reads when there are several
    /// inputs: each library's CSS, then the application's, in the order given.
    ///
    /// Every input's directory is mirrored under `cn1-merged-files/` beside the
    /// merged file and its `url()`s are pointed there, so an image or font keeps
    /// resolving once the text has moved. An `@import` is expanded first, while
    /// the path it is relative to is still known.
    /// What an imported stylesheet outside that directory refers to is copied
    /// in beside it, see [#mirrorOutsideAssets].
    ///
    /// The file is rewritten only when its content changes. Its modification
    /// time is what decides whether the theme is stale, so it has to move when
    /// an input or anything an input imports does, and stay put otherwise.
    private static void updateMergeFile(File[] inputFiles, File mergedFile) throws IOException {
        File mergeRoot = mergedFile.getAbsoluteFile().getParentFile();
        if (!mergeRoot.isDirectory() && !mergeRoot.mkdirs()) {
            throw new IOException("Could not create " + mergeRoot);
        }
        File incDir = new File(mergeRoot, "cn1-merged-files");
        incDir.mkdir();
        StringBuilder buf = new StringBuilder();
        for (File f : inputFiles) {
            File canonicalFile = f.getCanonicalFile();
            String md5 = getMd5(canonicalFile.getAbsolutePath());
            File destDir = new File(incDir, md5);
            syncDirectories(canonicalFile.getParentFile(), destDir);
            String contents = new String(Files.readAllBytes(f.toPath()), "UTF-8");
            contents = CssImports.inline(canonicalFile, contents, new LinkedHashSet<File>());
            contents = mirrorOutsideAssets(contents, canonicalFile.getParentFile(), destDir);
            contents = prefixUrls(contents, "cn1-merged-files/"+md5+"/");
            buf.append("\n/* "+f.getAbsolutePath()+" */\n").append(contents).append("\n/* end "+f.getAbsolutePath()+"*/\n");
        }
        byte[] merged = buf.toString().getBytes("UTF-8");
        if (mergedFile.isFile() && Arrays.equals(merged, Files.readAllBytes(mergedFile.toPath()))) {
            return;
        }
        System.out.println("Updating merge file " + mergedFile);
        try (FileOutputStream fos = new FileOutputStream(mergedFile)) {
            fos.write(merged);
        }
    }

    /// Where files from outside an input's directory are copied inside its
    /// mirror. Named so that it cannot plausibly collide with a directory of
    /// the stylesheet's own.
    private static final String OUTSIDE_ASSETS_DIR = "cn1-imported-assets";

    /// Copies into `mirrorDir` every file a `url()` of `contents` names that
    /// lies outside `inputDir`, and points the `url()` at the copy.
    ///
    /// An input's directory is mirrored whole, which covers everything the
    /// input and the stylesheets beside it refer to. A stylesheet imported
    /// from somewhere else -- `@import "../shared/base.css"` -- brings
    /// `url()`s that, once expressed relative to the input, climb out of that
    /// directory, and so out of its mirror: prefixed with the mirror's path
    /// they would name a file nothing copied. Each such file is copied on its
    /// own, under a directory named for where it came from, so two imports
    /// with an `img/logo.png` each do not overwrite one another.
    ///
    /// A `url()` naming something that does not exist is left as written, for
    /// the compiler to report.
    private static String mirrorOutsideAssets(String contents, File inputDir, final File mirrorDir)
            throws IOException {
        final java.nio.file.Path base = inputDir.getCanonicalFile().toPath();
        return CssImports.rewriteUrls(contents, new CssImports.UrlRewriter() {
            @Override
            public String rewrite(String url) throws IOException {
                if (!CssImports.isRelativeUrl(url)) {
                    return url;
                }
                java.nio.file.Path target;
                try {
                    target = base.resolve(url).normalize();
                } catch (java.nio.file.InvalidPathException ex) {
                    return url;
                }
                if (target.startsWith(base) || !Files.isRegularFile(target)) {
                    return url;
                }
                String origin = getMd5(target.getParent().toString());
                File copy = new File(new File(new File(mirrorDir, OUTSIDE_ASSETS_DIR), origin),
                        target.getFileName().toString());
                File source = target.toFile();
                if (!copy.isFile() || copy.lastModified() < source.lastModified()
                        || copy.length() != source.length()) {
                    File parent = copy.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Could not create " + parent);
                    }
                    Files.copy(target, copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return OUTSIDE_ASSETS_DIR + "/" + origin + "/" + copy.getName();
            }
        });
    }

    /// The layout of the project containing `start`, or null when `start` is
    /// not inside a Codename One project.
    ///
    /// [ProjectLayouts#detect(File)] answers with canonical paths. On macOS
    /// `/tmp` is a link to `/private/tmp`, so a canonical project directory is
    /// spelled differently from an input given under `/tmp`; the detected
    /// directories are therefore re-expressed as ancestors of `start` in its
    /// own spelling, which keeps every path this class prints or derives
    /// recognisable to the caller.
    static ProjectLayout getLayout(File start) {
        ProjectLayout layout = detectLayout(start);
        String buildDir = System.getProperty(BUILD_DIR_PROPERTY);
        return layout != null && buildDir != null && buildDir.length() > 0
                ? layout.withBuildDir(new File(buildDir)) : layout;
    }

    private static ProjectLayout detectLayout(File start) {
        if (start == null) {
            return null;
        }
        ProjectLayout detected = ProjectLayouts.detect(start);
        if (detected == null) {
            return null;
        }
        File projectDir = sameSpellingAncestor(start.getAbsoluteFile(), detected.projectDir());
        if (projectDir == null) {
            return detected;
        }
        File rootDir = sameSpellingAncestor(projectDir, detected.rootDir());
        if (rootDir == null) {
            return detected;
        }
        return ProjectLayouts.of(detected.buildSystem(), detected.kind(), rootDir, projectDir);
    }

    private static File sameSpellingAncestor(File from, File canonicalTarget) {
        for (File f = from; f != null; f = f.getParentFile()) {
            try {
                if (f.getCanonicalFile().equals(canonicalTarget)) {
                    return f;
                }
            } catch (IOException ex) {
                return null;
            }
        }
        return null;
    }

    private static File[] getInputFiles(String inputPath) {
        List<File> out = new ArrayList<File>();
        if (inputPath.contains(",")) {
            String[] paths = inputPath.split(",");
            for (String path : paths) {
                if (path.trim().isEmpty()) {
                    continue;
                }
                File f = new File(path);
                out.add(f);
            }
        } else {
            if (!inputPath.trim().isEmpty()) {
                out.add(new File(inputPath));
            }
        }
        return out.toArray(new File[out.size()]);
    }

    private static Map<String, Map<String, Map<String, String>>> loadLocalizationBundles(File localizationDirectory) throws IOException {
        Map<String, Map<String, Map<String, String>>> bundles = new LinkedHashMap<>();
        if (localizationDirectory == null) {
            return bundles;
        }
        if (!localizationDirectory.exists()) {
            throw new IOException("Localization directory does not exist: " + localizationDirectory.getAbsolutePath());
        }
        if (!localizationDirectory.isDirectory()) {
            throw new IOException("Localization path is not a directory: " + localizationDirectory.getAbsolutePath());
        }
        Path root = localizationDirectory.toPath();
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".properties"))
                    .forEach(p -> {
                        try {
                            Path relPath = root.relativize(p);
                            String rel = relPath.toString().replace(File.separatorChar, '/');
                            if (rel.isEmpty() || !rel.endsWith(".properties")) {
                                return;
                            }
                            String withoutExt = rel.substring(0, rel.length() - ".properties".length());
                            int lastSlash = withoutExt.lastIndexOf('/');
                            String packagePath = lastSlash >= 0 ? withoutExt.substring(0, lastSlash) : "";
                            String fileNamePart = lastSlash >= 0 ? withoutExt.substring(lastSlash + 1) : withoutExt;
                            if (fileNamePart.isEmpty()) {
                                return;
                            }
                            String[] tokens = fileNamePart.split("_");
                            String baseNamePart = fileNamePart;
                            String locale = "";
                            if (tokens.length > 1) {
                                for (int start = 1; start < tokens.length; start++) {
                                    String localeCandidate = joinTokens(tokens, start, tokens.length);
                                    if (isValidLocale(localeCandidate)) {
                                        baseNamePart = joinTokens(tokens, 0, start);
                                        locale = normalizeLocale(localeCandidate);
                                        break;
                                    }
                                }
                            }
                            String baseName;
                            if (!packagePath.isEmpty()) {
                                baseName = packagePath.replace('/', '.');
                                if (!baseNamePart.isEmpty()) {
                                    baseName = baseName + "." + baseNamePart;
                                }
                            } else {
                                baseName = baseNamePart;
                            }
                            if (baseName == null || baseName.isEmpty()) {
                                return;
                            }
                            Properties props = new Properties();
                            PropertiesUtil.loadUtf8WithFallback(p.toFile(), props);
                            Map<String, Map<String, String>> baseBundles = bundles.computeIfAbsent(baseName, k -> new LinkedHashMap<>());
                            Map<String, String> translations = new LinkedHashMap<>();
                            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                                translations.put(entry.getKey().toString(), entry.getValue().toString());
                            }
                            baseBundles.put(locale, translations);
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    });
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
        return bundles;
    }

    private static String joinTokens(String[] parts, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) {
                sb.append('_');
            }
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    private static boolean isValidLocale(String localeCandidate) {
        if (localeCandidate == null || localeCandidate.isEmpty()) {
            return false;
        }
        String[] parts = localeCandidate.split("_");
        if (parts.length == 0 || !isValidLanguage(parts[0])) {
            return false;
        }
        int index = 1;
        if (index < parts.length && isValidScript(parts[index])) {
            index++;
        }
        if (index < parts.length && isValidCountry(parts[index])) {
            index++;
        }
        while (index < parts.length) {
            if (!isValidVariant(parts[index])) {
                return false;
            }
            index++;
        }
        return true;
    }

    private static boolean isValidLanguage(String token) {
        if (token.length() < 2 || token.length() > 8) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!Character.isLetter(c) || !Character.isLowerCase(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidScript(String token) {
        if (token.length() != 4) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isLetter(token.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidCountry(String token) {
        if (token.length() == 2) {
            for (int i = 0; i < token.length(); i++) {
                if (!Character.isLetter(token.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
        if (token.length() == 3) {
            for (int i = 0; i < token.length(); i++) {
                if (!Character.isDigit(token.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean isValidVariant(String token) {
        if (token.isEmpty()) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_')) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeLocale(String locale) {
        if (locale == null || locale.isEmpty()) {
            return "";
        }
        String[] parts = locale.split("_");
        if (parts.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase());
        int index = 1;
        if (index < parts.length && isValidScript(parts[index])) {
            String token = parts[index];
            sb.append('_').append(Character.toUpperCase(token.charAt(0))).append(token.substring(1).toLowerCase());
            index++;
        }
        if (index < parts.length && isValidCountry(parts[index])) {
            sb.append('_').append(parts[index].toUpperCase());
            index++;
        }
        while (index < parts.length) {
            sb.append('_').append(parts[index]);
            index++;
        }
        return sb.toString();
    }
}
