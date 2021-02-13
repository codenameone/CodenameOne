package com.codename1.maven;

import com.codename1.ant.SortedProperties;
import org.apache.commons.io.FileUtils;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.*;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.zip.ZipUtil;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * <p>A mojo that generates an archetype project from a template.  The template specifies a base archetype project
 * to start with, and specifies customizations that need to be made to it, such as dependencies and profiles (which
 * are injected into the pom.xml of the "common" submodule project in archetype-resources.</p>
 *
 * <p>Currently templates are just .java files with a comment block containing metadata.  Metadata sections are marked
 * with the syntax:</p>
 *
 * <pre>{@code
 * [sectionname]
 * ----
 * Section content
 * ----
 * }</pre>
 *
 * <p>Sections include:</p>
 *
 * <ul>
 *     <li><em>dependencies</em> - contains one or more {@literal &lt;dependency&gt; } tags to insert into the pom.xml file.</li>
 *     <li><em>profiles</em> - contains one or more {@literal &lt;profile&gt; } tags to insert into the pom.xml file.</li>
 *     <li><em>archetype</em> - key-value pairs specifying the archetype project that this derives from</li>
 *
 * </ul>
 *
 */
@Mojo(name="generate-archetype", requiresProject = false)
public class GenerateArchetypeFromTemplateMojo extends AbstractCN1Mojo {




    @Parameter(required = true, property="template")
    private String template;

    @Parameter(required = true, property="outputDir", defaultValue = "${basedir}")
    private File outputDir;

    private File baseArchetypeDir;

    @Parameter(property = "overwrite", defaultValue="false")
    private boolean overwrite;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {

        File tplFile = loadTemplate(template);


        if (tplFile.isFile() && tplFile.getName().endsWith(".java")) {

            processJavaFile(tplFile);
        }
    }

    /**
     * Counts the number of repeating characters starting at a point in the string.
     * @param str The string
     * @param startPos The starting position
     * @param ch The character to check for.
     * @return The number of repeating characters `ch` starting at `startPos`.
     */
    private int countRepeats(String str, int startPos, char ch) {
        int count = 0;
        int len = str.length();
        for (int i = startPos; i < len; i++) {
            if (str.charAt(i) == ch) {
                count++;
            } else {
                return count;
            }
        }
        return count;

    }

    /**
     * Exception thrown when parsing a template fails.  Generally for a syntax error.
     */
    public class TemplateParseException extends Exception {
        public TemplateParseException(String message) {
            super(message);
        }
    }

    /**
     * Extracts a section from a template string.
     * @param haystack
     * @param sectionName
     * @return
     * @throws TemplateParseException
     */
    private String extractSectionFrom(String haystack, String sectionName) throws TemplateParseException {
        int sectionPos = haystack.indexOf("["+sectionName+"]");
        if (sectionPos >= 0) {
            int startingPos = haystack.indexOf("---", sectionPos);
            if (startingPos < 0) {
                throw new TemplateParseException("Found section heading "+sectionName+" without body in "+haystack);
            }
            if (!("["+sectionName+"]").equals(haystack.substring(sectionPos, startingPos).trim())) {
                throw new TemplateParseException("Illegal formatting in "+sectionName+" section.  There are characters between the header and the body.");
            }

            int separatorLength = countRepeats(haystack, startingPos, '-');
            assert separatorLength > 2;

            String separator = haystack.substring(startingPos, startingPos + separatorLength);

            int endPos = haystack.indexOf(separator, startingPos + separatorLength);
            if (endPos < 0) {
                throw new TemplateParseException("No closing separator found for section "+sectionName);
            }

            return haystack.substring(startingPos + separatorLength, endPos).trim();
        }
        return "";
    }

    /**
     * Extracts dependencies.  Dependencies are embedded with:
     *
     * [dependencies]
     * ----
     * content here
     * ----
     *
     * @param str
     * @return
     * @throws TemplateParseException
     */
    private String extractDependencies(String str) throws TemplateParseException {
        return extractSectionFrom(str, "dependencies");
    }

    /**
     * Extracts properties.   Properties are embedded with:
     *
     * [properties]
     * ----
     * content here
     * ----
     *
     * @param str
     * @return
     * @throws TemplateParseException
     */
    private String extractProperties(String str) throws TemplateParseException {
        return extractSectionFrom(str, "properties");
    }

    /**
     * Extracts CSS.  CSS embedded with:
     *
     * [css]
     * ----
     * content here
     * ----
     *
     * @param str
     * @return
     * @throws TemplateParseException
     */
    private String extractCSS(String str) throws TemplateParseException {
        return extractSectionFrom(str, "css");
    }

    private class FileContent {
        private String path;
        private String content;

        FileContent(String path, String content) {
            this.path = path;
            this.content = content;
        }
    }

    private List<FileContent> extractFiles(String str) throws TemplateParseException {
        List<FileContent> out = new ArrayList<FileContent>();
        java.util.Scanner scanner = new java.util.Scanner(extractSectionFrom(str, "files"));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String content = extractSectionFrom(str, "file:"+line.trim());
            out.add(new FileContent(line, content));
        }
        return out;
    }

    /**
     * Replaces variables in a string.
     * @param content The string to replace variables in.
     * @param variables Map of variables.  Keys cannot contain the "." character.  E.g. mainName OK.  codename1.mainName NOT OK
     * @return The content string with variables replaced.
     */
    private String replaceVariables(String content, Map<String,String> variables) {
        for (String key : variables.keySet()) {
            if (key.indexOf(".") >= 0) {

                throw new IllegalArgumentException("Variable keys should not contain a '.'.  This is to ensure that they don't conflict with maven properties.  Found key "+key);
            }
            content = content.replace("${"+key+"}", variables.get(key));
        }
        return content;
    }

    /**
     * Replaces variables in a string.  Variables include:
     *
     * `package` - The package name (from the codename1.packageName property)
     * `mainName` - The main name (from the codename1.mainName property)
     *
     * @param content Input content string. Variables marked with ${varname}
     * @return The content string with variables replaced.
     * @return The content string with variables replaced.
     */
    private String replaceVariables(String content) {
        Map<String,String> vars = new HashMap<>();
        vars.put("package", properties.getProperty("codename1.packageName"));
        vars.put("mainName", properties.getProperty("codename1.mainName"));
        return replaceVariables(content, vars);
    }

    /**
     * Processes a string with template instructions.  This string may have [dependencies], [css], or [properties]
     * sections with content that will be injected.
     *
     * WARNING:  This will make changes to the existing pom.xml file, and the src/main/css/theme.css file.
     *
     * @param contents String contents to be processed.
     * @throws MojoExecutionException
     */
    private void processString(String contents, File projectDir) throws MojoExecutionException {
        File archetypeResourcesDir = new File(projectDir, path("src", "main", "resources", "archetype-resources"));
        try {
            File commonProjectDir = new File(archetypeResourcesDir, "common");
            File pomFile = new File(commonProjectDir, "pom.xml");
            File codenameoneSettingsProperties = new File(commonProjectDir, "codenameone_settings.properties");
            File themeCss = new File(commonProjectDir, "src" + File.separator + "main" + File.separator + "css");
            String pomContents = FileUtils.readFileToString(pomFile, "UTF-8");
            final String origPomContents = pomContents;

            String dependencies = extractDependencies(contents);
            if (!dependencies.isEmpty()) {
                getLog().info("Injecting dependencies:\n" + dependencies+" \ninto "+pomFile);
                String marker = "<!-- INJECT DEPENDENCIES -->";
                pomContents = pomContents.replace(marker, dependencies + "\n" + marker);
            }

            String properties = extractProperties(contents);
            if (!properties.isEmpty()) {
                SortedProperties props = new SortedProperties();
                props.load(new StringReader(properties));


                if (codenameoneSettingsProperties == null || !codenameoneSettingsProperties.exists()) {
                    throw new MojoExecutionException("Cannot find codenameone_settings.properties");
                }
                SortedProperties cn1Props = new SortedProperties();
                cn1Props.load(new FileReader(codenameoneSettingsProperties));
                cn1Props.putAll(props);

                getLog().info("Injecting properties:\n" + props+"\n into "+codenameoneSettingsProperties);
                cn1Props.store(new FileWriter(codenameoneSettingsProperties), "Injected properties from template");

            }

            String css = extractCSS(contents);
            if (!css.isEmpty()) {

                if (!themeCss.exists()) {
                    themeCss.getParentFile().mkdirs();

                }
                getLog().info("Adding CSS to "+themeCss);
                FileUtils.writeStringToFile(themeCss, css, "UTF-8");
            }

            // We change the codename1.template property to codename1.template.installed so that
            // this mojo won't operate on this project again. (Notice the check at the beginning
            // of execImpl() to return if it doesn't find the codename1.template property).
            pomContents = pomContents.replace("<codename1.template>", "<codename1.templated.installed>")
                    .replace("</codename1.template>", "</codename1.template.installed>");





            if (!pomContents.equals(origPomContents)) {
                getLog().info("Writing changes to "+pomFile);
                FileUtils.writeStringToFile(pomFile, pomContents, "UTF-8");
            }

            for (FileContent file : extractFiles(contents)) {
                File f = new File(commonProjectDir, file.path);
                f.getParentFile().mkdirs();
                FileUtils.writeStringToFile(f, file.content, "UTF-8");
            }


        } catch (TemplateParseException ex) {
            throw new MojoExecutionException("Syntax error in template file", ex);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to process template file", ex);
        }
    }

    private File generateBaseArchetype(String string, File templateFile) throws TemplateParseException, IOException {
        Dependency out = new Dependency();
        String archetype = extractSectionFrom(string, "archetype");
        Properties props = new Properties();


        props.load(new StringReader(archetype));
        String[] requiredProperties = new String[]{
            "artifactId", "groupId", "version"
        };
        for (String key : requiredProperties) {
            if (!props.containsKey(key)) {
                throw new TemplateParseException("archetype property "+key+" required and missing.  Make sure it is defined in the [archetype] section of the template");
            }
        }


        File dest = new File(outputDir, props.getProperty("artifactId"));
        if (dest.exists()) {
            if (overwrite) {
                FileUtils.deleteDirectory(dest);
            } else {
                throw new IOException("Project already exists at " + dest + ".  Delete this project before regenerating");
            }
        }

        String base = props.getProperty("extends", null);
        if (base == null) {
            throw new TemplateParseException("[archetype] section requires the 'extends' property to specify the path to the archetype project that this extends");
        }
        baseArchetypeDir = new File(base);
        if (!baseArchetypeDir.isAbsolute()) {
            baseArchetypeDir = new File(templateFile.getParentFile(), base);
        }
        if (!baseArchetypeDir.exists()) {
            throw new IOException("Cannot find archetype project that this template extends.  Looking for it in "+baseArchetypeDir);
        }

        if (!new File(baseArchetypeDir, "pom.xml").exists()) {
            throw new IOException("Base archetype directory "+baseArchetypeDir+ " is not a maven project.");
        }

        FileUtils.copyDirectory(baseArchetypeDir, dest);

        File pomFile = new File(dest, "pom.xml");

        String groupId = null;
        String artifactId = null;
        String version = null;

        if (props.containsKey("id")) {
            String id = props.getProperty("id");
            String[] parts = id.split(":");
            if (parts.length != 3) {
                throw new TemplateParseException("Failed ot parse id property in [archetype] section.  It should be in the format groupId:artifactId:version");
            }
            groupId = parts[0];
            artifactId = parts[1];
            version = parts[2];
        }
        groupId = props.getProperty("groupId", groupId);
        artifactId = props.getProperty("artifactId", artifactId);
        version = props.getProperty("version", version);

        if (groupId == null || artifactId == null || version == null) {
            throw new TemplateParseException("The [archetype] section is required, and must have at least groupId, artifactId, and version defined.  You may also define these using the id property in the format groupId:artifactId:version");
        }

        String parentTag = "";
        String parentGroupId = null;
        String parentArtifactId = null;
        String parentVersion = null;
        if (props.containsKey("parent")) {
            String parent = props.getProperty("parent");
            String[] parts = parent.split(":");
            if (parts.length != 3) {
                throw new TemplateParseException("Failed to parse parent property in [archetype] section. It should be in the format groupId:artifactId:version");
            }
            parentGroupId = parts[0];
            parentArtifactId = parts[1];
            parentVersion = parts[2];

        }
        parentGroupId = props.getProperty("parentGroupId", parentGroupId);
        parentArtifactId = props.getProperty("parentArtifactId", parentArtifactId);
        parentVersion = props.getProperty("parentVersion", parentVersion);
        if (parentGroupId != null && parentVersion != null && parentArtifactId != null) {
            parentTag = "  <parent>\n" +
                    "    <groupId>"+parentGroupId+"</groupId>\n" +
                    "    <artifactId>"+parentArtifactId+"</artifactId>\n" +
                    "    <version>"+parentVersion+"</version>\n" +
                    "  </parent>\n";
        }

        String pomContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                parentTag +
                "  <groupId>"+groupId+"</groupId>\n" +
                "  <artifactId>"+artifactId+"</artifactId>\n" +
                "  <version>"+version+"</version>\n" +
                "  <packaging>maven-archetype</packaging>\n" +
                "\n" +
                "  <name>"+artifactId+"</name>\n" +
                "\n" +
                "\n" +
                "  <build>\n" +
                "    <extensions>\n" +
                "      <extension>\n" +
                "        <groupId>org.apache.maven.archetype</groupId>\n" +
                "        <artifactId>archetype-packaging</artifactId>\n" +
                "        <version>3.2.0</version>\n" +
                "      </extension>\n" +
                "    </extensions>\n" +
                "\n" +
                "    <pluginManagement>\n" +
                "      <plugins>\n" +
                "        <plugin>\n" +
                "          <artifactId>maven-archetype-plugin</artifactId>\n" +
                "          <version>3.2.0</version>\n" +
                "        </plugin>\n" +
                "      </plugins>\n" +
                "    </pluginManagement>\n" +
                "  </build>\n" +
                "\n" +
                "  <description>Artifact generated using the cn1:generate-archetype goal</description>\n" +
                "\n" +
                "  <url>https://www.codenameone.com</url>\n" +
                "\n" +
                "  <licenses>\n" +
                "    <license>\n" +
                "      <name>GPL v2 With Classpath Exception</name>\n" +
                "      <url>https://openjdk.java.net/legal/gplv2+ce.html</url>\n" +
                "      <distribution>repo</distribution>\n" +
                "      <comments>A business-friendly OSS license</comments>\n" +
                "    </license>\n" +
                "  </licenses>\n" +
                "</project>\n";
        FileUtils.writeStringToFile(pomFile, pomContents, "UTF-8");
        return dest;

    }

    private String path(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(File.separator);
            }
            sb.append(part);
        }
        return sb.toString();
    }

    /**
     * Processes a java file that have embedded template commands in it.
     * @param tplFile The java template file.
     * @throws MojoExecutionException
     */
    private void processJavaFile(File tplFile) throws MojoExecutionException{
        try {
            String javaContents = FileUtils.readFileToString(tplFile, "UTF-8");
            File destProject = generateBaseArchetype(javaContents, tplFile);

            processString(javaContents, destProject);
            File archetypeResourcesDir = new File(destProject, path("src", "main", "resources", "archetype-resources"));
            File commonDir = new File(archetypeResourcesDir, "common");
            File mainClassFile = new File(commonDir, path("src", "main", "java", "__mainName__.java"));


            getLog().info("Writing Java Source file at " + mainClassFile);
            FileUtils.writeStringToFile(mainClassFile, javaContents, "UTF-8");
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to load java template file from "+tplFile, ex);
        } catch (TemplateParseException ex) {
            throw new MojoExecutionException("Failed to parse template from file "+tplFile, ex);
        }
    }

    /**
     * Loads a template as specified in the codename1.template property.
     * @param template The path or URL to the template to load.  Will be treated as URL if it starts with
     *                 http:// or https://.  If it resolves to a zip file, it will extract the zip and return
     *                 the extracted directory.  Otherwise it returns the file itself.
     * @return
     * @throws MojoExecutionException
     */
    private File loadTemplate(String template) throws MojoExecutionException {
        File tplFile = null;
        if (template.startsWith("http://") || template.startsWith("https://")) {
            tplFile = new File(project.getBuild().getDirectory() + File.separator + "templates");
            tplFile.mkdirs();
            try {
                URL url = new URL(template);
                tplFile = new File(tplFile, new File(url.getPath()).getName());

                FileUtils.copyURLToFile(url, tplFile);
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to download template from "+template, ex);
            }
        } else {
            tplFile = new File(template);
        }

        if (!tplFile.exists()) {
            throw new MojoExecutionException("Cannot find template file "+tplFile);
        }

        if (tplFile.isDirectory()) {
            return tplFile;
        }
        if (tplFile.getName().endsWith(".zip")) {
            Expand unzip = (Expand)antProject.createTask("unzip");
            unzip.setSrc(tplFile);

            File destFile = new File(project.getBuild().getDirectory() + File.separator + tplFile.getName() + "-expanded" );
            unzip.setDest(destFile);
            unzip.execute();
            tplFile = destFile;
            if (!tplFile.exists()) {
                throw new MojoExecutionException("Failed to extract template to "+tplFile);
            }
        }
        return tplFile;

    }
}
