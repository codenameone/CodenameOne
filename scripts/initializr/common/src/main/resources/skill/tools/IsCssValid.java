import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// Validates that a Codename One theme.css file compiles cleanly via the
/// `com.codename1.ui.css.CSSThemeCompiler` API. Run **with** the codename-one
/// jars on the classpath; the tool auto-discovers the latest version in
/// ~/.m2/repository.
///
/// Usage:
///
///     java tools/IsCssValid.java common/src/main/css/theme.css
///
/// The tool locates `~/.m2/repository/com/codenameone/codenameone-core/<latest>/
/// codenameone-core-<latest>.jar` (and a matching `java-runtime` jar) and runs
/// the compiler via reflection. No need to pre-set `-cp` — the tool builds the
/// classpath internally.
///
/// Exit codes:
///
///   0 — CSS compiles (printed as `VALID`)
///   1 — CSS rejected; the compiler's error message is printed
///   2 — discovery / classpath / usage error
public class IsCssValid {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java IsCssValid.java <path/to/theme.css>");
            System.exit(2);
        }
        Path cssFile = Paths.get(args[0]);
        if (!Files.isRegularFile(cssFile)) {
            System.err.println("Not a file: " + cssFile);
            System.exit(2);
        }
        String css = Files.readString(cssFile);

        Path coreJar = findLatestJar("codenameone-core");
        Path runtimeJar = findLatestJar("java-runtime");
        if (coreJar == null) {
            System.err.println("Could not locate codenameone-core jar in ~/.m2. Run `mvn -pl common compile` first.");
            System.exit(2);
        }
        System.err.println("[IsCssValid] using core=" + coreJar
                + (runtimeJar == null ? "" : " runtime=" + runtimeJar));

        List<URL> urls = new ArrayList<>();
        urls.add(coreJar.toUri().toURL());
        if (runtimeJar != null) urls.add(runtimeJar.toUri().toURL());

        try (URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[0]),
                ClassLoader.getSystemClassLoader())) {
            Class<?> compilerCls = cl.loadClass("com.codename1.ui.css.CSSThemeCompiler");
            Class<?> resourceCls = cl.loadClass("com.codename1.ui.util.MutableResource");

            Constructor<?> resourceCtor = resourceCls.getDeclaredConstructor();
            resourceCtor.setAccessible(true);
            Object resource = resourceCtor.newInstance();

            Constructor<?> compilerCtor = compilerCls.getDeclaredConstructor();
            compilerCtor.setAccessible(true);
            Object compiler = compilerCtor.newInstance();

            Method compile = compilerCls.getDeclaredMethod("compile",
                    String.class, resourceCls, String.class);
            compile.setAccessible(true);

            try {
                compile.invoke(compiler, css, resource, "AgentValidationTheme");
            } catch (Throwable t) {
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                System.out.println("INVALID");
                System.out.println(cause.getClass().getSimpleName() + ": " + cause.getMessage());
                System.exit(1);
            }

            Method getTheme = resourceCls.getDeclaredMethod("getTheme", String.class);
            getTheme.setAccessible(true);
            Object theme = getTheme.invoke(resource, "AgentValidationTheme");
            if (theme == null) {
                System.out.println("INVALID");
                System.out.println("Compiler returned no theme — file may be empty or contain only @constants.");
                System.exit(1);
            }
        }

        System.out.println("VALID");
    }

    private static Path findLatestJar(String artifactId) throws IOException {
        Path baseDir = Paths.get(System.getProperty("user.home"),
                ".m2", "repository", "com", "codenameone", artifactId);
        if (!Files.isDirectory(baseDir)) return null;
        List<Path> candidates = new ArrayList<>();
        try (DirectoryStream<Path> versions = Files.newDirectoryStream(baseDir)) {
            for (Path versionDir : versions) {
                if (!Files.isDirectory(versionDir)) continue;
                try (DirectoryStream<Path> jars = Files.newDirectoryStream(versionDir,
                        artifactId + "-*.jar")) {
                    for (Path jar : jars) {
                        String n = jar.getFileName().toString();
                        if (n.endsWith("-sources.jar") || n.endsWith("-javadoc.jar")) continue;
                        candidates.add(jar);
                    }
                }
            }
        }
        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed());
        return candidates.get(0);
    }
}
