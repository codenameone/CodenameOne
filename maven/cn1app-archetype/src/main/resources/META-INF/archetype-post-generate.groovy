import static groovy.io.FileType.*
import java.nio.file.Path

def rootDir = new java.io.File(request.getOutputDirectory() + "/" + request.getArtifactId())
def rootPom = new java.io.File(rootDir, "pom.xml")
setupModules(rootPom);
def resolvedJava = resolveJavaVersion(rootDir);
applyJavaVersionTransforms(rootDir, rootPom, resolvedJava);

def projectType = (request.getProperties().getProperty("projectType", "app") ?: "app").trim()
def platformModules = (request.getProperties().getProperty("platformModules", "none") ?: "none").trim()
if (!(projectType in ["app", "app-with-backend", "backend-only"])) {
    throw new IllegalArgumentException("projectType must be app, app-with-backend or backend-only, not '"
            + projectType + "'")
}
def keptModules = parsePlatformModules(platformModules)
if (projectType == "backend-only") {
    if (!keptModules.isEmpty()) {
        throw new IllegalArgumentException("A backend-only project has no platform modules; drop -DplatformModules")
    }
    assembleBackendOnly(rootDir, request)
} else {
    pruneModules(rootDir, rootPom, keptModules, projectType == "app-with-backend")
}
deleteRecursively(new java.io.File(rootDir, ".cn1-backend-only"))

/**
 * There are a few scripts that need to be executable (or should be)
 */
["mvnw", "run.sh", "build.sh"].each { name ->
    def script = new java.io.File(rootDir, name)
    if (script.exists()) {
        script.setExecutable(true, false)
    }
}

if (projectType != "backend-only" && request.getProperties().getProperty("ide", null) == "netbeans") {
    def netbeansDir = new java.io.File(rootDir, "tools/netbeans");
    if (netbeansDir.exists()) {
        netbeansDir.listFiles().each {
            def destFile = new java.io.File(rootDir, it.getName())
            java.nio.file.Files.copy(it.toPath(), destFile.toPath())
        }
    }
}

/**
 * For some reason archetype automatically enables ALL modules definied in the archetype-metadata
 * even if we only want some of them conditionally enabled.  In our case, we only want the common
 * module enabled by default.  The rest are enabled according to the codename1.platform property.
 * @param pomFile
 * @return
 */
/**
 * The archetype ships codenameone_settings.properties and common/pom.xml with
 * `${javaVersion}` Velocity placeholders. With the default `javaVersion=auto`
 * those placeholders resolve to the literal string "auto" - we replace it here
 * with 17 when the archetype is invoked on a JDK >= 17 and with 8 otherwise.
 * If the user passed `-DjavaVersion=8` or `-DjavaVersion=17` explicitly, the
 * Velocity pass already substituted that value and there is nothing to do.
 *
 * Returns the resolved version ("17" or "8") as a string so callers can branch
 * on it for non-text-substitution transforms (e.g. dropping the win/ tree).
 */
def resolveJavaVersion(rootDir) {
    def settingsFile = new java.io.File(rootDir, "common/codenameone_settings.properties")
    def commonPom = new java.io.File(rootDir, "common/pom.xml")
    def hasAutoSettings = settingsFile.exists() && settingsFile.text.contains("codename1.arg.java.version=auto")
    def hasAutoPom = commonPom.exists() && commonPom.text.contains("<source>auto</source>")
    def explicit = readExplicitJavaVersion(settingsFile, commonPom)

    if (!hasAutoSettings && !hasAutoPom) {
        return explicit
    }

    def resolved = pickJavaVersionFromCurrentJvm()

    if (hasAutoSettings) {
        def content = settingsFile.text.replace("codename1.arg.java.version=auto",
                "codename1.arg.java.version=" + resolved)
        settingsFile.newWriter("UTF-8").withWriter { w -> w << content }
    }
    if (hasAutoPom) {
        def content = commonPom.text
                .replace("<source>auto</source>", "<source>" + resolved + "</source>")
                .replace("<target>auto</target>", "<target>" + resolved + "</target>")
        commonPom.newWriter("UTF-8").withWriter { w -> w << content }
    }
    return resolved
}

/**
 * When javaVersion is passed explicitly (-DjavaVersion=8 or =17), the Velocity
 * pass has already substituted the value into the templates. Read whichever
 * source still carries it so applyJavaVersionTransforms can branch correctly.
 */
def readExplicitJavaVersion(settingsFile, commonPom) {
    if (settingsFile.exists()) {
        def m = (settingsFile.text =~ /(?m)^codename1\.arg\.java\.version=(\d+)$/)
        if (m.find()) {
            return m.group(1)
        }
    }
    if (commonPom.exists()) {
        def m = (commonPom.text =~ /<source>(\d+)<\/source>/)
        if (m.find()) {
            return m.group(1)
        }
    }
    return "8"
}

/**
 * @return "17" when running on JDK 17 or newer, "8" otherwise.
 */
def pickJavaVersionFromCurrentJvm() {
    def specVersion = System.getProperty("java.specification.version", "1.8")
    def major
    try {
        if (specVersion.startsWith("1.")) {
            major = Integer.parseInt(specVersion.substring(2))
        } else {
            major = Integer.parseInt(specVersion.split("\\.")[0])
        }
    } catch (NumberFormatException ignored) {
        major = 8
    }
    return major >= 17 ? "17" : "8"
}

/**
 * Apply the Java-version-specific transforms that the initializr does on its
 * server-rendered templates:
 *   - Java 17 keeps the Codename One authoring skill in the same three-part
 *     layout the initializr generates: AGENTS.md (vendor-neutral root pointer),
 *     .agent-skills/codename-one/** (the skill), and .claude/skills/codename-one/
 *     SKILL.md (a thin stub redirecting to it)
 *   - Java 8 strips all three so older projects don't suddenly grow an AI-agent
 *     skill they never opted into. The skill's guidance is Java 17 anyway (var,
 *     records, text blocks, single-file source mode in tools/).
 *
 * The win/ module is the native win32 target and ships for every Java version
 * (only the long-retired UWP module that previously lived under win/ used to be
 * dropped for Java 17).
 *
 * Also rewrites the IntelliJ misc.xml languageLevel attribute to match the
 * resolved JDK (universal cleanup: the project-jdk-name/-type attributes
 * are already stripped in the archetype template itself).
 */
def applyJavaVersionTransforms(rootDir, rootPom, resolvedJava) {
    if (resolvedJava != "17") {
        [".claude", ".agent-skills", "AGENTS.md"].each { name ->
            def skillPath = new java.io.File(rootDir, name)
            if (skillPath.exists()) {
                deleteRecursively(skillPath)
            }
        }
    }
    setIntellijLanguageLevel(rootDir, resolvedJava)
}

/**
 * Match initializr's normalizeIntellijMiscXml: rewrite the `languageLevel`
 * attribute on the <component name="ProjectRootManager"> element to
 * JDK_17 for Java 17 projects and JDK_1_8 for Java 8. No-op if misc.xml
 * doesn't exist (e.g. user generated without IntelliJ-style .idea files).
 */
def setIntellijLanguageLevel(rootDir, resolvedJava) {
    def miscXml = new java.io.File(rootDir, ".idea/misc.xml")
    if (!miscXml.exists()) {
        return
    }
    def desired = resolvedJava == "17" ? "JDK_17" : "JDK_1_8"
    def content = miscXml.text
    def pattern = 'languageLevel="'
    def pos = content.indexOf(pattern)
    if (pos < 0) {
        return
    }
    def valueStart = pos + pattern.length()
    def valueEnd = content.indexOf('"', valueStart)
    if (valueEnd < 0) {
        return
    }
    def rewritten = content.substring(0, valueStart) + desired + content.substring(valueEnd)
    miscXml.newWriter("UTF-8").withWriter { w -> w << rewritten }
}

def deleteRecursively(file) {
    if (file.isDirectory()) {
        file.listFiles().each { deleteRecursively(it) }
    }
    file.delete()
}

/**
 * The platform modules a project is generated with. The default, none, is the
 * minimal layout: common builds every platform itself (see the cn1-host-*
 * profiles in common/pom.xml), and a module can be added later. "all" is the
 * full multi-module layout; a comma-separated list keeps just those.
 */
def parsePlatformModules(value) {
    def all = ["javase", "android", "ios", "javascript", "win", "linux"]
    if (value == "" || value == "none") {
        return []
    }
    if (value == "all") {
        return all
    }
    def out = []
    value.split(",").each { raw ->
        def id = raw.trim()
        if (id.length() == 0) {
            return
        }
        if (!(id in all)) {
            throw new IllegalArgumentException("platformModules: unknown platform '" + id
                    + "'; expected none, all, or a list of " + all.join(","))
        }
        out << id
    }
    return out
}

/**
 * Removes the platform modules the project was not asked to keep, and the
 * backend module unless it was. A module that is gone is simply not in the
 * reactor: each root profile also needs <root>/<module>/pom.xml. The javase
 * profile's activeByDefault goes with the javase module, because Maven applies
 * it even when the profile's own conditions fail.
 */
def pruneModules(rootDir, rootPom, keptModules, keepBackend) {
    def javase = new java.io.File(rootDir, "javase")
    if (!("javase" in keptModules) && javase.exists()) {
        // The packaged desktop app's native theme moves to common, where the
        // desktop goals look for it when there is no javase module.
        def theme = new java.io.File(javase, "src/desktop/resources")
        if (theme.isDirectory()) {
            def dest = new java.io.File(rootDir, "common/src/desktop/resources")
            dest.mkdirs()
            theme.listFiles().each { f ->
                if (f.isFile()) {
                    java.nio.file.Files.copy(f.toPath(), new java.io.File(dest, f.getName()).toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
        def content = rootPom.text.replaceAll(/\n[ \t]*<activeByDefault>true<\/activeByDefault>/, "")
        rootPom.newWriter("UTF-8").withWriter { w -> w << content }
    }
    ["javase", "android", "ios", "javascript", "win", "linux"].each { id ->
        if (!(id in keptModules)) {
            def dir = new java.io.File(rootDir, id)
            if (dir.exists()) {
                deleteRecursively(dir)
            }
        }
    }
    if (!keepBackend) {
        def backend = new java.io.File(rootDir, "backend")
        if (backend.exists()) {
            deleteRecursively(backend)
        }
    }
}

/**
 * Turns the generated tree into a backend-only project: one module, the server,
 * at the root. Its files are the ones the initializr and the Gradle generators
 * use, staged into .cn1-backend-only/ when the archetype is built.
 */
def assembleBackendOnly(rootDir, request) {
    def staged = new java.io.File(rootDir, ".cn1-backend-only")
    def pkg = request.getPackage()
    def props = request.getProperties()
    def maven = { String text ->
        text.replace("./gradlew __BACKEND__runBackend", "./mvnw cn1:backend")
            .replace("./gradlew __BACKEND__backendPackage", "./mvnw cn1:backend-package")
            .replace("under `runBackend`", "under `cn1:backend`")
            .replace("\${package}", pkg)
    }
    def pom = new java.io.File(staged, "backend-only-pom.xml").getText("UTF-8")
            .replace("<groupId>com.example.myapp</groupId>", "<groupId>" + request.getGroupId() + "</groupId>")
            .replace("myappname", request.getArtifactId())
            .replace("<version>1.0-SNAPSHOT</version>", "<version>" + request.getVersion() + "</version>")
            .replace("<cn1.plugin.version>8.0-SNAPSHOT</cn1.plugin.version>",
                    "<cn1.plugin.version>" + props.getProperty("cn1PluginVersion") + "</cn1.plugin.version>")
            .replace("<cn1.version>8.0-SNAPSHOT</cn1.version>",
                    "<cn1.version>" + props.getProperty("cn1Version") + "</cn1.version>")

    def keep = ["pom.xml", "mvnw", "mvnw.cmd", ".mvn", ".gitignore", ".cn1-backend-only"] as Set
    rootDir.listFiles().each { f ->
        if (!(f.getName() in keep)) {
            deleteRecursively(f)
        }
    }
    new java.io.File(rootDir, "pom.xml").newWriter("UTF-8").withWriter { w -> w << pom }
    ["application.properties", "application-dev.properties"].each { name ->
        new java.io.File(rootDir, name).newWriter("UTF-8").withWriter { w ->
            w << maven(new java.io.File(staged, name + ".txt").getText("UTF-8"))
        }
    }
    def srcDir = new java.io.File(rootDir, "src/main/java/" + pkg.replace('.', '/'))
    srcDir.mkdirs()
    ["Api", "Greeter"].each { name ->
        new java.io.File(srcDir, name + ".java").newWriter("UTF-8").withWriter { w ->
            w << maven(new java.io.File(staged, name + ".java.txt").getText("UTF-8"))
        }
    }
    def gitignore = new java.io.File(rootDir, ".gitignore")
    if (!gitignore.exists()) {
        gitignore.newWriter("UTF-8").withWriter { w -> w << "target/\n" }
    }
    new java.io.File(rootDir, "README.md").newWriter("UTF-8").withWriter { w ->
        w << "# " + request.getArtifactId() + "\n\n" +
             "A Codename One backend. The routes are the @RestController classes under src/main/java.\n\n" +
             "    ./mvnw cn1:backend                   # run it on this JVM\n" +
             "    CN1_PROFILE=dev ./mvnw cn1:backend   # with application-dev.properties\n" +
             "    ./mvnw cn1:backend-package           # build a single native binary\n\n" +
             "Settings are read from application.properties, beside this file.\n"
    }
}

def setupModules(pomFile) {
    def content = pomFile.text;
    def modulesPos = content.indexOf("<modules>");
    def endTag = "</modules>";

    def modulesEndPos = content.indexOf(endTag) + endTag.length();
    def modulesSection = "<modules>\n<module>common</module>\n</modules>";

    content = content.substring(0, modulesPos) + modulesSection + content.substring(modulesEndPos);
    pomFile.newWriter("UTF-8").withWriter { w ->
        w << content
    }


}

