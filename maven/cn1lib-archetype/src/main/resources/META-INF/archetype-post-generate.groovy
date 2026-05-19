import static groovy.io.FileType.*

def rootDir = new java.io.File(request.getOutputDirectory() + "/" + request.getArtifactId())
def rootPom = new java.io.File(rootDir, "pom.xml")
def resolvedJava = resolveJavaVersion(rootDir)
applyJavaVersionTransforms(rootDir, rootPom, resolvedJava)

/**
 * Mirrors cn1app-archetype's resolveJavaVersion: when the archetype was
 * invoked with javaVersion=auto (the default), Velocity will have left the
 * literal "auto" string in the rewritten templates. Replace those with the
 * resolved version ("17" on JDK >= 17, else "8") so the generated project
 * compiles out of the box.
 *
 * When -DjavaVersion=8 or =17 is passed explicitly, Velocity has already
 * substituted the value and there is nothing here to rewrite — but we still
 * read the value back from the templates so applyJavaVersionTransforms can
 * branch on it.
 */
def resolveJavaVersion(rootDir) {
    def requiredProps = new java.io.File(rootDir, "common/codenameone_library_required.properties")
    def commonPom = new java.io.File(rootDir, "common/pom.xml")
    def rootPom = new java.io.File(rootDir, "pom.xml")
    def javasePom = new java.io.File(rootDir, "javase/pom.xml")
    def libPom = new java.io.File(rootDir, "lib/pom.xml")

    def autoSettings = requiredProps.exists() && requiredProps.text.contains("codename1.arg.java.version=auto")
    def autoCommonPom = commonPom.exists() && commonPom.text.contains("<maven.compiler.source>auto</maven.compiler.source>")
    def autoRootPom = rootPom.exists() && rootPom.text.contains("<maven.compiler.source>auto</maven.compiler.source>")
    def autoJavasePom = javasePom.exists() && javasePom.text.contains("<maven.compiler.source>auto</maven.compiler.source>")
    def autoLibPom = libPom.exists() && libPom.text.contains("<maven.compiler.source>auto</maven.compiler.source>")

    if (!autoSettings && !autoCommonPom && !autoRootPom && !autoJavasePom && !autoLibPom) {
        return readExplicitJavaVersion(requiredProps, commonPom)
    }

    def resolved = pickJavaVersionFromCurrentJvm()

    if (autoSettings) {
        rewriteFile(requiredProps,
                requiredProps.text.replace("codename1.arg.java.version=auto",
                        "codename1.arg.java.version=" + resolved))
    }
    [rootPom, commonPom, javasePom, libPom].each { f ->
        if (f.exists() && f.text.contains("auto")) {
            def rewritten = f.text
                    .replace("<maven.compiler.source>auto</maven.compiler.source>",
                            "<maven.compiler.source>" + resolved + "</maven.compiler.source>")
                    .replace("<maven.compiler.target>auto</maven.compiler.target>",
                            "<maven.compiler.target>" + resolved + "</maven.compiler.target>")
                    .replace("<source>auto</source>", "<source>" + resolved + "</source>")
                    .replace("<target>auto</target>", "<target>" + resolved + "</target>")
            if (rewritten != f.text) {
                rewriteFile(f, rewritten)
            }
        }
    }
    return resolved
}

/**
 * When javaVersion is passed explicitly (-DjavaVersion=8 or =17), Velocity
 * has already substituted the literal value into the templates. Read it back
 * so applyJavaVersionTransforms can branch correctly.
 */
def readExplicitJavaVersion(requiredProps, commonPom) {
    if (requiredProps.exists()) {
        def m = (requiredProps.text =~ /(?m)^codename1\.arg\.java\.version=(\d+)$/)
        if (m.find()) {
            return m.group(1)
        }
    }
    if (commonPom.exists()) {
        def m = (commonPom.text =~ /<maven\.compiler\.source>(\d+)<\/maven\.compiler\.source>/)
        if (m.find()) {
            return m.group(1)
        }
    }
    return "8"
}

/** "17" when running on JDK 17 or newer, "8" otherwise. */
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
 * For Java 17 library projects: drop the win/ source tree and the
 * `<module>win</module>` line from the root pom. Note the cn1lib root pom
 * lists win as a direct module rather than a profile (unlike cn1app), so
 * the strip pattern is line-removal rather than block-removal.
 *
 * Always: rewrite the IntelliJ misc.xml languageLevel attribute to match
 * the resolved JDK.
 */
def applyJavaVersionTransforms(rootDir, rootPom, resolvedJava) {
    if (resolvedJava == "17") {
        def winDir = new java.io.File(rootDir, "win")
        if (winDir.exists()) {
            deleteRecursively(winDir)
        }
        if (rootPom.exists()) {
            def content = rootPom.text
            def pattern = "\n        <module>win</module>"
            if (content.contains(pattern)) {
                rewriteFile(rootPom, content.replace(pattern, ""))
            } else {
                // Fall back to a regex-tolerant strip if indentation changes.
                rewriteFile(rootPom, (content =~ /\n\s*<module>win<\/module>/).replaceFirst(""))
            }
        }
    }
    setIntellijLanguageLevel(rootDir, resolvedJava)
}

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
    rewriteFile(miscXml, content.substring(0, valueStart) + desired + content.substring(valueEnd))
}

def deleteRecursively(file) {
    if (file.isDirectory()) {
        file.listFiles().each { deleteRecursively(it) }
    }
    file.delete()
}

def rewriteFile(file, content) {
    file.newWriter("UTF-8").withWriter { w -> w << content }
}
