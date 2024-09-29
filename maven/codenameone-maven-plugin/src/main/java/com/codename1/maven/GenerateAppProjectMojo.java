package com.codename1.maven;

import com.codename1.ant.SortedProperties;
import com.codename1.util.RichPropertiesReader;
import org.apache.commons.io.FileUtils;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.jdom.etl.JDomModelETL;
import org.apache.maven.model.jdom.etl.JDomModelETLFactory;
import org.apache.maven.model.jdom.etl.ModelETL;
import org.apache.maven.model.jdom.etl.ModelETLRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.*;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;

import java.io.*;
import java.util.*;

import static com.codename1.maven.PathUtil.path;

/**
 * Generates a Maven project (using the cn1app-archetype) using a provided ANT Codename One application project as a template.
 * This is to assist in migrating Ant projects to Maven projects.
 *
 *
 */
@Mojo(name="generate-app-project", requiresProject = false)
public class GenerateAppProjectMojo extends AbstractMojo {

    @Parameter(property = "cn1.tidy", defaultValue = "true")
    private boolean tidy;

    @Parameter(property = "sourceProject")
    private File sourceProject;

    @Parameter(property="artifactId")
    private String artifactId;

    @Parameter(property="groupId")
    private String groupId;

    @Parameter(property="version", defaultValue = "1.0-SNAPSHOT")
    private String version;


    private Properties loadSourceProjectProperties() throws IOException {
        Properties props = new Properties();
        if (sourceProject.isDirectory()) {
            File propsFile = new File(sourceProject, "codenameone_settings.properties");
            if (propsFile.exists()) {
                try (FileInputStream fis = new FileInputStream(propsFile)) {
                    props.load(fis);
                }
            }

        }
        return props;
    }

    private void tidyPomXmlFiles(File projectDirectory) {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(projectDirectory);
        if (getLog().isDebugEnabled()) {
            request.setDebug(true);
        }
        request.setGoals(Collections.singletonList("com.diffplug.spotless:spotless-maven-plugin:2.43.0:apply"));
        Properties props = new Properties();
        props.setProperty("spotless.pom.includes", "pom.xml");
        props.setProperty("spotless.pom.sortPom.keepBlankLines", "false");

        request.setProperties(props);

        Invoker invoker = new DefaultInvoker();
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                getLog().warn("Failed to tidy pom.xml files.  Exit code "+result.getExitCode());
            }
        } catch (MavenInvocationException ex) {
            getLog().warn("Failed to tidy pom.xml files", ex);
        }
    }

    private void generateProject() throws MojoExecutionException{


        String archetypeVersion = "LATEST";
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(getClass().getResourceAsStream("/META-INF/maven/com.codenameone/codenameone-maven-plugin/pom.xml"));
            archetypeVersion = model.getVersion();
        } catch (Exception ex) {
            getLog().warn("Attempted to read archetype version from embedded pom.xml file but failed", ex);
        }
        InvocationRequest request = new DefaultInvocationRequest();
        //request.setPomFile( new File( "/path/to/pom.xml" ) );

        request.setGoals( Collections.singletonList( "archetype:generate" ) );

        String[] propsArr = {
                "interactiveMode=false",
            "archetypeArtifactId=cn1app-archetype",
            "archetypeGroupId=com.codenameone",
            "archetypeVersion="+archetypeVersion,
            "artifactId="+artifactId,
                "groupId="+groupId,
                "version="+version,
                "mainName="+mainName(),
                "package="+packageName()
        };
        Properties props = new Properties();
        for (String prop : propsArr) {
            int eqpos = prop.indexOf("=");
            if (eqpos > 0) {
                props.setProperty(prop.substring(0, eqpos), prop.substring(eqpos+1));
            } else if (eqpos < 0) {
                props.setProperty(prop, "true");
            }
        }

        if (getLog().isDebugEnabled()) {
            request.setDebug(true);
        }
        request.setShowErrors(true);

        request.setProperties(props);

        Invoker invoker = new DefaultInvoker();
        try {
            InvocationResult result = invoker.execute( request );
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException("Failed to generate project using cn1app-archetype.  Exit code "+result.getExitCode());
            }
        } catch (MavenInvocationException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);

        }
    }

    private File targetProjectDir() {
        return new File(artifactId);
    }



    private File targetCommonDir() {
        return new File(targetProjectDir(), "common");
    }

    private File targetIosDir() {
        return new File(targetProjectDir(), "ios");
    }

    private File targetAndroidDir() {
        return new File(targetProjectDir(), "android");
    }

    private File targetJavascriptDir() {
        return new File(targetProjectDir(), "javascript");
    }

    private File targetJavaseDir() {
        return new File(targetProjectDir(), "javase");
    }

    private File targetWinDir() {
        return new File(targetProjectDir(), "win");
    }

    private File targetSrcDir() {
        return new File(targetCommonDir(), "src");
    }

    private File targetSrcDir(String type) {
        return new File(targetSrcDir(), path("main", type));
    }



    private File targetTestSrcDir(String type) {
        return new File(targetSrcDir(), path("test", type));
    }

    private File sourceSrcDir() {
        return new File(sourceProject, "src");
    }

    private File sourceTestsDir() {
        return new File(sourceProject, "test");
    }

    private File sourceNativeDir() {
        return new File(sourceProject, "native");
    }

    private File sourceNativeDir(String type) {
        return new File(sourceNativeDir(), type);
    }

    private void copyPropertiesFiles() throws IOException {

        for (File child : sourceProject.listFiles()) {
            if (child.getName().endsWith(".properties")) {
                FileUtils.copyFile(child, new File(targetCommonDir(), child.getName()));
            }
        }
    }

    private Properties sourceProperties;
    private Properties sourceProperties() throws IOException {
        if (sourceProperties == null) {
            sourceProperties = loadSourceProjectProperties();
        }
        return sourceProperties;
    }


    private File sourceIconFile() throws IOException {
        Properties props = sourceProperties();
        String icon = props.getProperty("codename1.icon");
        if (icon == null || icon.isEmpty()) {
            icon = "icon.png";
        }
        File iconFile = new File(icon);
        if (!iconFile.isAbsolute()) {
            iconFile = new File(sourceProject, icon);
            if (iconFile.isFile()) {
                return iconFile;
            }
        }

        return new File(sourceProject, "icon.png");

    }

    private File destIconFile() throws IOException {
        return new File(targetCommonDir(), sourceIconFile().getName());

    }

    private void copyIcon() throws IOException {
        File sourceIconFile = sourceIconFile();
        if (sourceIconFile.exists() && sourceIconFile.isFile()) {
            FileUtils.copyFile(sourceIconFile(), destIconFile());
        } else {
            try (InputStream iconStream = getClass().getResourceAsStream("codenameone-icon.png")) {
                FileUtils.copyInputStreamToFile(iconStream, destIconFile());
            }
        }
        SortedProperties props = new SortedProperties();
        File propertiesFile = new File(targetCommonDir(), "codenameone_settings.properties");
        try (InputStream input = new FileInputStream(propertiesFile)) {
            props.load(input);
        }
        if (!destIconFile().getName().equals(props.getProperty("codename1.icon"))) {
            props.setProperty("codename1.icon", destIconFile().getName());
            try (OutputStream output = new FileOutputStream(propertiesFile)) {
                props.store(output, "Updated icon");
            }
        }


    }

    private boolean hasFilesWithSuffix(File root, String suffix) {
        if (root.isDirectory()) {
            for (File child : root.listFiles()) {
                if (child.getName().endsWith(suffix)) {
                    return true;
                }
            }
            for (File child : root.listFiles()) {
                if (child.isDirectory()) {
                    if (hasFilesWithSuffix(child, suffix)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void copyIosFiles() {
        if (sourceNativeDir("ios").exists()) {
            File srcDir = new File(targetIosDir(), path("src", "main", "objectivec"));
            File resDir = new File(targetIosDir(), path("src", "main", "resources"));
            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(srcDir);
                copy.setOverwrite(true);
                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("ios"));
                files.setIncludes("**/*.m, **/*.c, **/*.h, *.m, *.c, *.h");
                copy.addFileset(files);

                copy.execute();
            }

            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(resDir);
                copy.setOverwrite(true);
                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("ios"));
                files.setExcludes("**/*.m, **/*.c, **/*.h, *.m, *.c, *.h");
                copy.addFileset(files);

                copy.execute();
            }

        }

    }

    private void copyAndroidFiles() {
        if (sourceNativeDir("android").exists()) {
            File srcDir = new File(targetAndroidDir(), path("src", "main", "java"));
            File resDir = new File(targetAndroidDir(), path("src", "main", "resources"));
            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(srcDir);
                copy.setOverwrite(true);
                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("android"));
                files.setIncludes("**/*.java, *.java");
                copy.addFileset(files);

                copy.execute();
            }

            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(resDir);
                copy.setOverwrite(true);
                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("android"));
                files.setExcludes("**/*.java, *.java");
                copy.addFileset(files);

                copy.execute();
            }

        }

    }

    private void copyJavascriptFiles() {
        if (sourceNativeDir("javascript").exists()) {
            File srcDir = new File(targetJavascriptDir(), path("src", "main", "javascript"));
            File resDir = new File(targetJavascriptDir(), path("src", "main", "resources"));
            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(srcDir);
                copy.setOverwrite(true);
                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("javascript"));
                files.setIncludes("**/*.js, *.js");
                copy.addFileset(files);

                copy.execute();
            }

            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(resDir);
                copy.setOverwrite(true);
                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("javascript"));
                files.setExcludes("**/*.js, *.js");
                copy.addFileset(files);

                copy.execute();
            }

        }
    }

    private void copyWinFiles() {
        if (sourceNativeDir("win").exists()) {
            File srcDir = new File(targetWinDir(), path("src", "main", "csharp"));
            File resDir = new File(targetWinDir(), path("src", "main", "resources"));
            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(srcDir);
                copy.setOverwrite(true);
                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("win"));
                files.setIncludes("**/*.cs, *.cs");
                copy.addFileset(files);

                copy.execute();
            }

            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(resDir);
                copy.setOverwrite(true);
                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("win"));
                files.setExcludes("**/*.cs, *.cs");
                copy.addFileset(files);

                copy.execute();
            }

        }
    }

    private File sourceLibDir() {
        return new File(sourceProject, "lib");
    }

    private File sourceCSSDir() {
        return new File(sourceProject, "css");
    }




    private void fixCSS(File cssFile) throws IOException {
        String contents = FileUtils.readFileToString(cssFile, "UTF-8");
        int pos = 0;
        int matchPos = -1;
        boolean changed = false;
        while ((matchPos = contents.indexOf("url(../", pos)) >= 0) {
            int closingParen = contents.indexOf(")", matchPos+4);

            if (closingParen < 0) {
                // No closing parenthesis... probably an error
                // but we dont' want this fixing to be strict
                break;
            }


            String path = contents.substring(matchPos + 4, closingParen);

            if (path.contains("(") || path.contains("{") || path.contains("}") || path.contains("\n")) {
                // This path contains invalid characters..
                // this may be in a comment. In any case we don't want to
                // mess with it for fear of deleting important stuff
                pos = matchPos+4;
                continue;
            }

            File referencedFile = new File(sourceCSSDir(), path.replace("/", File.separator));
            if (referencedFile.exists()) {
                File targetFile = new File(targetSrcDir("css"), referencedFile.getName());
                if (targetFile.exists()) {
                    pos = closingParen+1;
                    continue;
                }
                FileUtils.copyFile(referencedFile, targetFile);
                contents = contents.substring(0, matchPos) + "url(\""+targetFile.getName()+"\")" + contents.substring(closingParen+1);
                // Need to get closingParen position again because we changed the contents.
                closingParen = contents.indexOf(")", matchPos+4);
                changed = true;
            }

            pos = closingParen+1;

        }
        while ((matchPos = contents.indexOf("url(\"../", pos)) >= 0) {
            int closingQuote = contents.indexOf("\"", matchPos+5);

            if (closingQuote < 0) {
                // No closing parenthesis... probably an error
                // but we dont' want this fixing to be strict
                break;
            }


            String path = contents.substring(matchPos + 5, closingQuote);

            if (path.contains("(") || path.contains("{") || path.contains("}") || path.contains("\n")) {
                // This path contains invalid characters..
                // this may be in a comment. In any case we don't want to
                // mess with it for fear of deleting important stuff
                pos = matchPos+5;
                continue;
            }

            File referencedFile = new File(sourceCSSDir(), path.replace("/", File.separator));
            if (referencedFile.exists()) {
                File targetFile = new File(targetSrcDir("css"), referencedFile.getName());
                if (targetFile.exists()) {
                    pos = closingQuote+1;
                    continue;
                }
                FileUtils.copyFile(referencedFile, targetFile);
                contents = contents.substring(0, matchPos) + "url(\""+targetFile.getName()+"\"" + contents.substring(closingQuote+1);
                // Need to get closingParen position again because we changed the contents.
                closingQuote = contents.indexOf("\"", matchPos+5);
                changed = true;
            }

            pos = closingQuote+1;

        }
        while ((matchPos = contents.indexOf("url('../", pos)) >= 0) {
            int closingQuote = contents.indexOf("'", matchPos+5);

            if (closingQuote < 0) {
                // No closing parenthesis... probably an error
                // but we dont' want this fixing to be strict
                break;
            }


            String path = contents.substring(matchPos + 5, closingQuote);

            if (path.contains("(") || path.contains("{") || path.contains("}") || path.contains("\n")) {
                // This path contains invalid characters..
                // this may be in a comment. In any case we don't want to
                // mess with it for fear of deleting important stuff
                pos = matchPos+5;
                continue;
            }

            File referencedFile = new File(sourceCSSDir(), path.replace("/", File.separator));
            if (referencedFile.exists()) {
                File targetFile = new File(targetSrcDir("css"), referencedFile.getName());
                if (targetFile.exists()) {
                    pos = closingQuote+1;
                    continue;
                }
                FileUtils.copyFile(referencedFile, targetFile);
                contents = contents.substring(0, matchPos) + "url(\""+targetFile.getName()+"\"" + contents.substring(closingQuote+1);
                // Need to get closingParen position again because we changed the contents.
                closingQuote = contents.indexOf("\"", matchPos+5);
                changed = true;
            }

            pos = closingQuote+1;

        }

        if (changed) {
            FileUtils.writeStringToFile(cssFile, contents, "UTF-8");
        }
    }

    private void copyCSSFiles() throws IOException {
        File srcDir = targetSrcDir("css");
        if (sourceCSSDir().exists()) {


            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(srcDir);
                copy.setOverwrite(true);
                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceCSSDir());
                files.setIncludes("**");
                copy.addFileset(files);

                copy.execute();

                if (srcDir.isDirectory()) {
                    for (File child : srcDir.listFiles()) {
                        if (child.getName().endsWith(".css")) {
                            fixCSS(child);

                        } else if (child.getName().endsWith(".checksums")) {
                            child.delete();
                        }
                    }
                }

                File backups = new File(srcDir, ".backups");
                if (backups.isDirectory()) {
                    FileUtils.deleteDirectory(backups);
                }


            }
        } else {
            if (srcDir.exists()) {
                // This project doesn't have a css directory
                // so the target project won't have a css directory either.
                FileUtils.deleteDirectory(srcDir);
            }
        }
    }

    private void copyJavaseFiles() {
        if (sourceNativeDir("javase").exists()) {
            File srcDir = new File(targetJavaseDir(), path("src", "main", "java"));
            File resDir = new File(targetJavaseDir(), path("src", "main", "resources"));
            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(srcDir);
                copy.setOverwrite(true);
                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("javase"));
                files.setIncludes("**/*.java, *.java");
                copy.addFileset(files);

                copy.execute();
            }

            {
                Copy copy = (Copy) antProject().createTask("copy");
                copy.setTodir(resDir);
                copy.setOverwrite(true);
                FileSet files = new FileSet();
                files.setProject(antProject());
                files.setDir(sourceNativeDir("javase"));
                files.setExcludes("**/*.java, *.java");
                copy.addFileset(files);

                copy.execute();


            }

            // If there are jar files in the resources directory, we should issue a warning that
            // they should replace these with dependencies in the pom.xml
            if (resDir.isDirectory()) {
                for (File child : resDir.listFiles()) {
                    if (child.getName().endsWith(".jar")) {
                        getLog().warn("Found jar file '" + child.getName() + "' in the native/javase directory.  This has been copied to " + child + ", but you should " +
                                "remove this file and replace it with the equivalent Maven dependency inside your " + new File(targetJavaseDir(), "pom.xml") + " file.");
                    }
                }
            }

        }
    }

    private void copyTestSourceFiles() {
        if (!sourceTestsDir().exists()) {
            getLog().debug("Source project does not include a test directory.  Skipping copying test source files");
            return;
        }
        getLog().debug("Copying test source files from "+sourceTestsDir());
        {
            Copy copy = (Copy) antProject().createTask("copy");
            copy.setTodir(targetTestSrcDir("java"));
            copy.setOverwrite(true);
            FileSet files = new FileSet();
            files.setProject(antProject());
            files.setDir(sourceTestsDir());
            files.setIncludes("**/*.java, *.java");
            copy.addFileset(files);

            copy.execute();
        }

        {
            Copy copy = (Copy) antProject().createTask("copy");
            copy.setTodir(targetTestSrcDir("resources"));
            copy.setOverwrite(true);
            FileSet files = new FileSet();
            files.setProject(antProject());
            files.setDir(sourceTestsDir());
            files.setExcludes("**/*.kt, **/*.java, **/*.mirah, *.kt, *.java, *.mirah");
            copy.addFileset(files);

            copy.execute();
        }


        if (hasFilesWithSuffix(sourceSrcDir(), ".kt")){
            targetSrcDir("kotlin").mkdirs();
            Copy copy = (Copy) antProject().createTask("copy");
            copy.setTodir(targetTestSrcDir("kotlin"));
            copy.setOverwrite(true);
            FileSet files = new FileSet();
            files.setProject(antProject());
            files.setDir(sourceTestsDir());
            files.setIncludes("**/*.kt, *.kt");
            copy.addFileset(files);

            copy.execute();
        }
        if (hasFilesWithSuffix(sourceSrcDir(), ".mirah")){
            targetSrcDir("mirah").mkdirs();
            Copy copy = (Copy) antProject().createTask("copy");
            copy.setTodir(targetTestSrcDir("mirah"));
            copy.setOverwrite(true);
            FileSet files = new FileSet();
            files.setProject(antProject());
            files.setDir(sourceTestsDir());
            files.setIncludes("**/*.mirah, *.mirah");
            copy.addFileset(files);

            copy.execute();
        }

    }

    private void copySourceFiles() {
        {
            Copy copy = (Copy) antProject().createTask("copy");
            copy.setTodir(targetSrcDir("java"));
            copy.setOverwrite(true);
            FileSet files = new FileSet();
            files.setProject(antProject());
            files.setDir(sourceSrcDir());
            files.setIncludes("**/*.java, *.java");
            copy.addFileset(files);

            copy.execute();
        }

        {
            Copy copy = (Copy) antProject().createTask("copy");
            copy.setTodir(targetSrcDir("resources"));
            copy.setOverwrite(true);
            FileSet files = new FileSet();
            files.setProject(antProject());
            files.setDir(sourceSrcDir());
            files.setExcludes("**/*.kt, **/*.java, **/*.mirah, *.kt, *.java, *.mirah");
            copy.addFileset(files);

            copy.execute();

            File cn1PropertiesFile = new File(sourceProject, "codenameone_settings.properties");
            if (cn1PropertiesFile.exists()) {
                Properties cn1Properties = new SortedProperties();
                try (FileInputStream input = new FileInputStream(cn1PropertiesFile)) {
                    cn1Properties.load(input);
                } catch (IOException ex) {
                    getLog().error("Failed to open "+cn1Properties+" while checking or cssTheme property", ex);
                }
                if ("true".equals(cn1Properties.getProperty("codename1.cssTheme", "false"))) {
                    // If we're using a CSS theme, then we need to delete the theme.res file
                    File themeRes = new File(targetSrcDir("resources"), "theme.res");
                    if (themeRes.exists()) {
                        getLog().debug("Deleting "+themeRes+" because this project uses CSS themes.  In maven the theme.res is generated at build time, and is never saved in the source directory.");
                        themeRes.delete();
                    }
                }

            }


        }


        if (hasFilesWithSuffix(sourceSrcDir(), ".kt")){
            targetSrcDir("kotlin").mkdirs();
            Copy copy = (Copy) antProject().createTask("copy");
            copy.setTodir(targetSrcDir("kotlin"));
            copy.setOverwrite(true);
            FileSet files = new FileSet();
            files.setProject(antProject());
            files.setDir(sourceSrcDir());
            files.setIncludes("**/*.kt, *.kt");
            copy.addFileset(files);

            copy.execute();
        }
        if (hasFilesWithSuffix(sourceSrcDir(), ".mirah")){
            targetSrcDir("mirah").mkdirs();
            Copy copy = (Copy) antProject().createTask("copy");
            copy.setTodir(targetSrcDir("mirah"));
            copy.setOverwrite(true);
            FileSet files = new FileSet();
            files.setProject(antProject());
            files.setDir(sourceSrcDir());
            files.setIncludes("**/*.mirah, *.mirah");
            copy.addFileset(files);

            copy.execute();
        }

    }

    private Properties generateAppProjectProperties;
    private Properties generateAppProjectProperties() throws IOException, RichPropertiesReader.ConfigSyntaxException {
        if (generateAppProjectProperties == null) {
            generateAppProjectProperties = new Properties();
            if (generateAppProjectConfigFile().exists()) {

                new RichPropertiesReader().load(generateAppProjectConfigFile(), generateAppProjectProperties);

            }
        }
        return generateAppProjectProperties;
    }

    private String packageName() {
        if (System.getProperty("packageName") != null) {
            return System.getProperty("packageName");
        }
        if (System.getProperty("package") != null) {
            return System.getProperty("package");
        }
        return groupId;

    }

    private String mainName() {
        if (System.getProperty("mainName") != null) {
            return System.getProperty("mainName");
        }
        StringBuilder sb = new StringBuilder();
        int len = artifactId.length();
        boolean firstChar = true;
        boolean capNext = false;
        for (int i=0; i<len; i++) {
            char ch = artifactId.charAt(i);
            if (firstChar) {
                if (Character.isLetter(ch)) {
                    sb.append(Character.toUpperCase(ch));
                    firstChar = false;
                }
                continue;
            }
            if (Character.isLetterOrDigit(ch)) {
                if (capNext) {
                    sb.append(Character.toUpperCase(ch));
                    capNext = false;
                } else {
                    sb.append(ch);
                }
            } else {
                capNext = true;
            }
        }



        return sb.toString();
    }


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {

            Properties props;
            try {
                props = generateAppProjectProperties();
            } catch (Exception ex) {
                throw new MojoExecutionException("Failed to load "+generateAppProjectConfigFile(), ex);
            }
            String templateType = props.getProperty("template.type");

            if (templateType == null) {
                // templateType == null is a proxy to test whether this is just a request to migrate
                // an existing ANT project.  If it is, then we will want to retain the main name and
                // package name of the original project.
                // We set the system properties here so that the call to generateProject() after this
                // will use the correct coordinates.
                Properties sourceAntProjectCn1Properties = loadSourceProjectProperties();
                if (sourceAntProjectCn1Properties.getProperty("codename1.mainName") != null) {
                    System.setProperty("mainName", sourceAntProjectCn1Properties.getProperty("codename1.mainName"));
                }
                if (sourceAntProjectCn1Properties.getProperty("codename1.packageName") != null) {
                    System.setProperty("packageName", sourceAntProjectCn1Properties.getProperty("codename1.packageName"));
                }
            }

            generateProject();



            if (templateType != null) {
                // This is a project template
                // Make a copy of it so that we can turn it into a concrete proejct
                File dest = new File(targetProjectDir(), path("target", "codenameone", "tmpProject"));
                dest.getParentFile().mkdirs();
                FileUtils.copyDirectory(sourceProject, dest);
                File origSource = sourceProject;
                sourceProject = dest;

                Properties renderProperties = new Properties();
                renderProperties.put("packageName", packageName());
                renderProperties.put("mainName", mainName());
                renderProperties.putAll(System.getProperties());


                ProjectTemplate tpl = new ProjectTemplate(sourceProject, renderProperties);

                if (props.getProperty("template.mainName") != null && props.getProperty("template.packageName") != null) {
                    tpl.convertToTemplate(props.getProperty("template.packageName"), props.getProperty("template.mainName"));
                }

                tpl.processFiles();

                // The project should now be a concrete project



                //

            }

            if (templateType == null || "ant".equalsIgnoreCase(templateType)) {
                // The source project was an ANT project
                copyPropertiesFiles();
                copyIcon();
                copySourceFiles();
                copyTestSourceFiles();
                copyAndroidFiles();
                copyIosFiles();
                copyJavascriptFiles();
                copyWinFiles();
                copyJavaseFiles();
                copyCSSFiles();
                copyCn1libs();
                injectDependencies();
            } else if ("maven".equalsIgnoreCase(templateType)) {
                //The source project was a Maven project
                {
                    File src = new File(sourceProject, path("common", "src"));
                    File destSrc = new File(targetProjectDir(), path("common", "src"));
                    if (src.exists()) {
                        FileUtils.deleteDirectory(destSrc);
                        FileUtils.copyDirectory(src, destSrc);
                    }
                }

                File cn1Settings = new File(sourceProject, path("common", "codenameone_settings.properties"));
                File destCn1Settings = new File(targetProjectDir(), path("common", "codenameone_settings.properties"));
                if (cn1Settings.exists()) {
                    FileUtils.copyFile(cn1Settings, destCn1Settings);
                }
                for (File child : new File(sourceProject, "common").listFiles()) {
                    if (child.getName().endsWith(".png")) {
                        FileUtils.copyFile(child, new File(targetProjectDir(), path("common", child.getName())));
                    }
                }

                injectDependencies();
            }



        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to copy files", ex);
        }
        if (tidy) {
            tidyPomXmlFiles(targetProjectDir());
        }
    }

    private Project antProject;
    private Project antProject() {
        if (antProject == null) {
            antProject = new Project();
            antProject.setBaseDir(sourceProject);

            antProject.setDefaultInputStream(System.in);

            InputHandler handler = new DefaultInputHandler();
            antProject.setProjectReference(handler);
            antProject.setInputHandler(handler);


            antProject.init();
        }
        return antProject;

    }



    private void copyCn1libs() throws MojoExecutionException, MojoFailureException{
        if (sourceLibDir() == null || !sourceLibDir().exists() || !sourceLibDir().isDirectory()) {
            return;
        }
        for (File cn1lib : sourceLibDir().listFiles()) {
            if (cn1lib.getName().startsWith("kotlin-runtime")) {
                getLog().debug("Skipping "+cn1lib+" because kotlin no longer requires a cn1lib.");
                continue;
            }
            if (cn1lib.getName().endsWith(".cn1lib")) {
                installLegacyCn1lib(cn1lib);
            }
        }
    }

    private static String getBaseName(File file) {
        return file.getName().substring(0, file.getName().indexOf("."));
    }

    private void installLegacyCn1lib(File cn1lib) throws MojoExecutionException, MojoFailureException {
        getLog().info("Installing cn1lib "+cn1lib);
        Cn1libInstaller installer = new Cn1libInstaller(new File(targetProjectDir(), "common"), groupId, artifactId, version, getLog());
        installer.setFile(cn1lib);
        installer.setOverwrite(false);
        installer.setUpdatePom(true);

        installer.executeImpl();

    }

    private File generateAppProjectConfigFile() {
        return new File(sourceProject, "generate-app-project.rpf");
    }

    private File targetCommonPomXml() {
        return new File(targetCommonDir(), "pom.xml");
    }

    private File targetRootPomXml() {
        return new File(targetProjectDir(), "pom.xml");
    }

    private void injectDependencies() throws MojoExecutionException {
        if (!generateAppProjectConfigFile().exists()) {
            return;
        }
        try {
            Properties props = new Properties();
            new RichPropertiesReader().load(generateAppProjectConfigFile(), props);
            String dependencies = props.getProperty("dependencies");
            String parentDependencies = props.getProperty("parentDependencies");
            if (targetCommonPomXml().exists()) {
                Model model;
                ModelETL modelETL;
                /*
                try (FileInputStream fis = new FileInputStream(targetCommonPomXml())){
                    MavenXpp3Reader reader = new MavenXpp3Reader();
                    model = reader.read(fis);

                } catch (Exception ex) {
                    throw new MojoExecutionException("Failed to read pom.xml file from "+targetCommonPomXml(), ex);
                }
                */
                try {
                    ModelETLRequest modelETLRequest = new ModelETLRequest();
                    modelETL = new JDomModelETLFactory().newInstance(modelETLRequest);
                    modelETL.extract(targetCommonPomXml());
                    model = modelETL.getModel();

                } catch (Exception ex) {
                    throw new MojoExecutionException("Failed to read pom.xml file from "+targetCommonPomXml(), ex);
                }
                if (dependencies != null) {
                    String dummyModelStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                            "    <modelVersion>4.0.0</modelVersion>\n" +
                            "    <groupId>link.sharpe</groupId>\n" +
                            "    <artifactId>mavenproject1</artifactId>\n" +
                            "    <version>1.0-SNAPSHOT</version>\n" +
                            "    <dependencies>\n" +
                            dependencies +
                            "    </dependencies>\n" +
                            "</project>";
                    Model dummyModel;
                    try {
                        MavenXpp3Reader reader = new MavenXpp3Reader();
                        dummyModel = reader.read(new CharArrayReader(dummyModelStr.toCharArray()));
                    } catch (Exception ex) {
                        throw new MojoExecutionException("Failed to read dummy pom.xml file while injecting dependencies into "+targetCommonPomXml(), ex);
                    }
                    for(Dependency dep : dummyModel.getDependencies()) {
                        model.addDependency(dep);
                    }
                    /*
                    MavenXpp3Writer writer = new MavenXpp3Writer();
                    try (FileOutputStream fos = new FileOutputStream(targetCommonPomXml())) {
                        writer.write(fos, model);
                    }
                    */
                    modelETL.load(targetCommonPomXml());

                }


            }
            if (targetRootPomXml().exists()) {
                Model model;
                ModelETL modelETL;
                /*
                try (FileInputStream fis = new FileInputStream(targetRootPomXml())){
                    MavenXpp3Reader reader = new MavenXpp3Reader();
                    model = reader.read(fis);

                } catch (Exception ex) {
                    throw new MojoExecutionException("Failed to read pom.xml file from "+targetRootPomXml(), ex);
                }

                 */
                try {
                    ModelETLRequest modelETLRequest = new ModelETLRequest();
                    modelETL = new JDomModelETLFactory().newInstance(modelETLRequest);
                    modelETL.extract(targetRootPomXml());
                    model = modelETL.getModel();

                } catch (Exception ex) {
                    throw new MojoExecutionException("Failed to read pom.xml file from "+targetRootPomXml(), ex);
                }
                if (parentDependencies != null) {
                    //getLog().info("Parent dependencies: "+parentDependencies);
                    String dummyModelStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                            "    <modelVersion>4.0.0</modelVersion>\n" +
                            "    <groupId>link.sharpe</groupId>\n" +
                            "    <artifactId>mavenproject1</artifactId>\n" +
                            "    <version>1.0-SNAPSHOT</version>\n" +
                            "    <dependencies>\n" +
                            parentDependencies +
                            "    </dependencies>\n" +
                            "</project>";
                    Model dummyModel;
                    try {
                        MavenXpp3Reader reader = new MavenXpp3Reader();
                        dummyModel = reader.read(new CharArrayReader(dummyModelStr.toCharArray()));
                    } catch (Exception ex) {
                        throw new MojoExecutionException("Failed to read dummy pom.xml file while injecting dependencies into "+targetRootPomXml(), ex);
                    }
                    for(Dependency dep : dummyModel.getDependencies()) {
                        model.addDependency(dep);
                    }
                    /*
                    MavenXpp3Writer writer = new MavenXpp3Writer();
                    try (FileOutputStream fos = new FileOutputStream(targetRootPomXml())) {
                        writer.write(fos, model);
                    }

                     */
                    modelETL.load(targetRootPomXml());

                }


            }


        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to process configuration for generateAppProjectConfigFile "+generateAppProjectConfigFile(), ex);
        } catch (RichPropertiesReader.ConfigSyntaxException ex) {
            throw new MojoExecutionException("Failed to process configuration for generateAppProjectConfigFile "+generateAppProjectConfigFile(), ex);
        }
    }

}
