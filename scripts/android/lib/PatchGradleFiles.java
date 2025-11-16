import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatchGradleFiles {
    private static final String REPOSITORIES_BLOCK = """
            repositories {
                google()
                mavenCentral()
            }
            """.stripTrailing();

    private static final Pattern REPOSITORIES_PATTERN = Pattern.compile("(?ms)^\\s*repositories\\s*\\{.*?\\}");

    private static final Pattern ANDROID_BLOCK_PATTERN = Pattern.compile("(?m)^\\s*android\\s*\\{");
    private static final Pattern DEFAULT_CONFIG_PATTERN = Pattern.compile("(?ms)^\\s*defaultConfig\\s*\\{.*?^\\s*\\}");
    private static final Pattern DEFAULT_CONFIG_HEADER_PATTERN = Pattern.compile("(?ms)^\\s*defaultConfig\\s*\\{");
    private static final Pattern COMPILE_SDK_PATTERN = Pattern.compile("(?m)^\\s*compileSdkVersion\\s+\\d+");
    private static final Pattern TARGET_SDK_PATTERN = Pattern.compile("(?m)^\\s*targetSdkVersion\\s+\\d+");
    private static final Pattern TEST_INSTRUMENTATION_PATTERN = Pattern.compile("(?m)^\\s*testInstrumentationRunner\\s*\".*?\"\\s*$");
    private static final Pattern USE_LIBRARY_PATTERN = Pattern.compile("(?m)^\\s*useLibrary\\s+'android\\.test\\.(?:base|mock|runner)'\\s*$");
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("(?m)^\\s*(implementation|api|testImplementation|androidTestImplementation)\\b");

    public static void main(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        if (arguments == null) {
            System.exit(2);
            return;
        }

        boolean modifiedRoot = patchRootBuildGradle(arguments.root);
        boolean modifiedApp = patchAppBuildGradle(arguments.app, arguments.compileSdk, arguments.targetSdk);

        if (modifiedRoot) {
            System.out.println("Patched " + arguments.root);
        }
        if (modifiedApp) {
            System.out.println("Patched " + arguments.app);
        }
        if (!modifiedRoot && !modifiedApp) {
            System.out.println("Gradle files already normalized");
        }
    }

    private static boolean patchRootBuildGradle(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        Matcher matcher = REPOSITORIES_PATTERN.matcher(content);
        if (!matcher.find()) {
            if (!content.endsWith("\n")) {
                content += "\n";
            }
            content += REPOSITORIES_BLOCK;
            Files.writeString(path, ensureTrailingNewline(content), StandardCharsets.UTF_8);
            return true;
        }

        String block = matcher.group();
        boolean changed = false;
        if (!block.contains("google()") || !block.contains("mavenCentral()")) {
            String[] lines = block.split("\n");
            java.util.LinkedHashSet<String> body = new java.util.LinkedHashSet<>();
            for (int i = 1; i < lines.length - 1; i++) {
                String line = lines[i].trim();
                if (!line.isEmpty()) {
                    body.add("    " + line.trim());
                }
            }
            body.add("    google()");
            body.add("    mavenCentral()");
            StringBuilder newBlock = new StringBuilder();
            newBlock.append(lines[0]).append('\n');
            for (String line : body) {
                newBlock.append(line).append('\n');
            }
            newBlock.append(lines[lines.length - 1]);
            content = content.substring(0, matcher.start()) + newBlock + content.substring(matcher.end());
            changed = true;
        }

        if (changed) {
            Files.writeString(path, ensureTrailingNewline(content), StandardCharsets.UTF_8);
        }
        return changed;
    }

    private static boolean patchAppBuildGradle(Path path, int compileSdk, int targetSdk) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        boolean changed = false;

        Result r = ensureAndroidBlock(content, compileSdk, targetSdk);
        content = r.content();
        changed |= r.changed();

        r = ensureInstrumentationRunner(content);
        content = r.content();
        changed |= r.changed();

        r = removeLegacyUseLibrary(content);
        content = r.content();
        changed |= r.changed();

        r = ensureTestDependencies(content);
        content = r.content();
        changed |= r.changed();

        r = ensureJacocoConfiguration(content);
        content = r.content();
        changed |= r.changed();

        if (changed) {
            Files.writeString(path, ensureTrailingNewline(content), StandardCharsets.UTF_8);
        }
        return changed;
    }

    private static Result ensureAndroidBlock(String content, int compileSdk, int targetSdk) {
        Matcher androidBlockMatcher = ANDROID_BLOCK_PATTERN.matcher(content);
        if (!androidBlockMatcher.find()) {
            if (!content.endsWith("\n")) {
                content += "\n";
            }
            String block = "\nandroid {\n" +
                    "    compileSdkVersion " + compileSdk + "\n" +
                    "    defaultConfig {\n" +
                    "        targetSdkVersion " + targetSdk + "\n" +
                    "    }\n}";
            return new Result(content + block, true);
        }

        boolean changed = false;
        Matcher compileMatcher = COMPILE_SDK_PATTERN.matcher(content);
        if (compileMatcher.find()) {
            String replacement = "    compileSdkVersion " + compileSdk;
            String newContent = compileMatcher.replaceFirst(replacement);
            if (!newContent.equals(content)) {
                content = newContent;
                changed = true;
            }
        } else {
            Matcher insertMatcher = ANDROID_BLOCK_PATTERN.matcher(content);
            if (insertMatcher.find()) {
                int pos = insertMatcher.end();
                content = content.substring(0, pos) + "\n    compileSdkVersion " + compileSdk + content.substring(pos);
                changed = true;
            }
        }

        Matcher defaultConfigMatcher = DEFAULT_CONFIG_PATTERN.matcher(content);
        if (defaultConfigMatcher.find()) {
            String block = defaultConfigMatcher.group();
            Matcher targetMatcher = TARGET_SDK_PATTERN.matcher(block);
            String replacement = "        targetSdkVersion " + targetSdk;
            String updated;
            if (targetMatcher.find()) {
                updated = targetMatcher.replaceFirst(replacement);
            } else {
                int brace = block.indexOf('{');
                if (brace >= 0) {
                    updated = block.substring(0, brace + 1) + "\n" + replacement + block.substring(brace + 1);
                } else {
                    updated = block;
                }
            }
            if (!updated.equals(block)) {
                content = content.substring(0, defaultConfigMatcher.start()) + updated + content.substring(defaultConfigMatcher.end());
                changed = true;
            }
        } else {
            Matcher insertMatcher = ANDROID_BLOCK_PATTERN.matcher(content);
            if (insertMatcher.find()) {
                int pos = insertMatcher.end();
                String snippet = "\n    defaultConfig {\n        targetSdkVersion " + targetSdk + "\n    }";
                content = content.substring(0, pos) + snippet + content.substring(pos);
                changed = true;
            }
        }

        return new Result(content, changed);
    }

    private static Result ensureInstrumentationRunner(String content) {
        String runner = "androidx.test.runner.AndroidJUnitRunner";
        if (content.contains(runner)) {
            return new Result(content, false);
        }
        Matcher matcher = TEST_INSTRUMENTATION_PATTERN.matcher(content);
        if (matcher.find()) {
            String replacement = "        testInstrumentationRunner \"" + runner + "\"";
            String newContent = matcher.replaceAll(replacement);
            return new Result(newContent, !newContent.equals(content));
        }

        Matcher defaultConfigHeaderMatcher = DEFAULT_CONFIG_HEADER_PATTERN.matcher(content);
        if (defaultConfigHeaderMatcher.find()) {
            int pos = defaultConfigHeaderMatcher.end();
            String snippet = "\n        testInstrumentationRunner \"" + runner + "\"";
            content = content.substring(0, pos) + snippet + content.substring(pos);
            return new Result(content, true);
        }

        Matcher androidMatcher = ANDROID_BLOCK_PATTERN.matcher(content);
        if (androidMatcher.find()) {
            int pos = androidMatcher.end();
            String snippet = "\n    defaultConfig {\n        testInstrumentationRunner \"" + runner + "\"\n    }";
            content = content.substring(0, pos) + snippet + content.substring(pos);
            return new Result(content, true);
        }
        return new Result(content, false);
    }

    private static Result removeLegacyUseLibrary(String content) {
        Matcher matcher = USE_LIBRARY_PATTERN.matcher(content);
        String newContent = matcher.replaceAll("");
        return new Result(newContent, !newContent.equals(content));
    }

    private static Result ensureTestDependencies(String content) {
        String moduleView = content.replaceAll("(?ms)^\\s*(buildscript|pluginManagement)\\s*\\{.*?^\\s*\\}", "");
        boolean usesModern = DEPENDENCY_PATTERN.matcher(moduleView).find();
        String configuration = usesModern ? "androidTestImplementation" : "androidTestCompile";
        String[] dependencies = {
                "androidx.test.ext:junit:1.1.5",
                "androidx.test:runner:1.5.2",
                "androidx.test:core:1.5.0",
                "androidx.test.services:storage:1.4.2"
        };
        boolean missing = false;
        for (String dep : dependencies) {
            if (!moduleView.contains(dep)) {
                missing = true;
                break;
            }
        }
        if (!missing) {
            return new Result(content, false);
        }
        StringBuilder block = new StringBuilder();
        block.append("\n\ndependencies {\n");
        for (String dep : dependencies) {
            if (!moduleView.contains(dep)) {
                block.append("    ").append(configuration).append(" \"").append(dep).append("\"\n");
            }
        }
        block.append("}\n");
        if (!content.endsWith("\n")) {
            content += "\n";
        }
        return new Result(content + block, true);
    }

    private static Result ensureJacocoConfiguration(String content) {
        if (content.contains("jacocoAndroidReport")) {
            return new Result(content, false);
        }

        String jacocoBlock = """
apply plugin: 'jacoco'

android {
    buildTypes {
        debug {
            testCoverageEnabled true
        }
    }
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.register("jacocoAndroidReport", JacocoReport) {
    group = "verification"
    description = "Generates Jacoco coverage report for the debug variant"
    dependsOn("connectedDebugAndroidTest")

    reports {
        xml.required = true
        html.required = true
        html.outputLocation = layout.buildDirectory.dir("reports/jacoco/jacocoAndroidReport/html")
    }

    def excludes = [
            '**/R.class',
            '**/R$*.class',
            '**/BuildConfig.*',
            '**/Manifest*.*',
            '**/*Test*.*',
            '**/androidx/**/*',
            '**/com/google/**/*'
    ]

    def javaClasses = fileTree(dir: "$buildDir/intermediates/javac/debug/classes", exclude: excludes)
    def kotlinClasses = fileTree(dir: "$buildDir/tmp/kotlin-classes/debug", exclude: excludes)

    classDirectories.setFrom(files(javaClasses, kotlinClasses).asFileTree.matching {
        include 'com/codename1/impl/android/**'
    })

    sourceDirectories.setFrom(files("src/main/java"))

    executionData.setFrom(fileTree(dir: "$buildDir", includes: [
            "outputs/code_coverage/debugAndroidTest/connected/*coverage.ec",
            "outputs/code_coverage/connected/*coverage.ec",
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
    ]))
}
""".stripTrailing();

        return new Result(ensureTrailingNewline(content) + "\n" + jacocoBlock + "\n", true);
    }

    private static String ensureTrailingNewline(String content) {
        return content.endsWith("\n") ? content : content + "\n";
    }

    private record Result(String content, boolean changed) {
    }

    private static class Arguments {
        final Path root;
        final Path app;
        final int compileSdk;
        final int targetSdk;

        Arguments(Path root, Path app, int compileSdk, int targetSdk) {
            this.root = root;
            this.app = app;
            this.compileSdk = compileSdk;
            this.targetSdk = targetSdk;
        }

        static Arguments parse(String[] args) {
            Path root = null;
            Path app = null;
            int compileSdk = 33;
            int targetSdk = 33;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--root" -> {
                        if (i + 1 >= args.length) {
                            System.err.println("Missing value for --root");
                            return null;
                        }
                        root = Path.of(args[++i]);
                    }
                    case "--app" -> {
                        if (i + 1 >= args.length) {
                            System.err.println("Missing value for --app");
                            return null;
                        }
                        app = Path.of(args[++i]);
                    }
                    case "--compile-sdk" -> {
                        if (i + 1 >= args.length) {
                            System.err.println("Missing value for --compile-sdk");
                            return null;
                        }
                        compileSdk = Integer.parseInt(args[++i]);
                    }
                    case "--target-sdk" -> {
                        if (i + 1 >= args.length) {
                            System.err.println("Missing value for --target-sdk");
                            return null;
                        }
                        targetSdk = Integer.parseInt(args[++i]);
                    }
                    default -> {
                        System.err.println("Unknown argument: " + arg);
                        return null;
                    }
                }
            }
            if (root == null || app == null) {
                System.err.println("--root and --app are required");
                return null;
            }
            return new Arguments(root, app, compileSdk, targetSdk);
        }
    }
}
