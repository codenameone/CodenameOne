import static groovy.io.FileType.*
import java.nio.file.Path

def rootDir = new java.io.File(request.getOutputDirectory() + "/" + request.getArtifactId())
def rootPom = new java.io.File(rootDir, "pom.xml")
setupModules(rootPom);
def resolvedJava = resolveJavaVersion(rootDir);
applyJavaVersionTransforms(rootDir, rootPom, resolvedJava);

/**
 * There are a few scripts that need to be executable (or should be)
 */

// The maven wrapper scripts should be executable
def mvnw = new java.io.File(rootDir, "mvnw")
mvnw.setExecutable(true, false)

// run.sh should be executable
def runSh = new java.io.File(rootDir, "run.sh")
runSh.setExecutable(true, false)

// The build.sh should be executable
def buildSh = new java.io.File(rootDir, "build.sh")
buildSh.setExecutable(true, false)

if (request.getProperties().getProperty("ide", null) == "netbeans") {
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
 *   - Java 17 keeps .claude/skills/codename-one/** (the Codename One authoring skill)
 *   - Java 8 strips .claude/ so older projects don't suddenly grow an AI-agent
 *     skill they never opted into.
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
        def claudeDir = new java.io.File(rootDir, ".claude")
        if (claudeDir.exists()) {
            deleteRecursively(claudeDir)
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

