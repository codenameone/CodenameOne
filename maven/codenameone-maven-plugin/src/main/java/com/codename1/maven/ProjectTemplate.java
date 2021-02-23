package com.codename1.maven;

import com.codename1.ant.SortedProperties;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static com.codename1.maven.PathUtil.path;

/**
 * Encapsulates a project template which means that certain files in the project may contain placeholders
 * which are replaced with values from some properties when {@link #processFiles()}} is called.
 *
 * The project structure may be either a maven project or an Ant project.  It will assume that the project
 * is an Ant project if the codenameone_settings.properties is in the project root.  It will assume that the
 * project is a Maven project if codenameone_settings.properties is in the common subdirectory.
 */
public class ProjectTemplate {
    /**
     * Properties that will be replaced when {@link #processFiles()} is called.  Only root level properties are
     * replaced. E.g. "foo.bar=bazz" will be ignored.  But "foo=bazz" will result in ${foo} to be replaced with "bazz".
     */
    private Properties properties;

    /**
     * The root directory of the project.
     */
    private File projectRoot;


    public ProjectTemplate(File projectRoot, Properties properties) {
        this.projectRoot = projectRoot;
        this.properties = properties;
    }

    private boolean isDirectoryEmpty(File directory) {
        for (File child : directory.listFiles()) {
            return true;
        }
        return false;
    }

    /**
     * Convert a real project to a template.
     * This mainly consists changing all references to the specified packageName and mainName
     * to be changed the the ${packageName} and ${mainName} variables.
     *
     * @param packageName
     * @param mainName
     */
    public void convertToTemplate(String packageName, String mainName) throws IOException {
        File codenameOneSettings = new File(projectRoot, "codenameone_settings.properties");
        SortedProperties settingsProps = new SortedProperties();
        String path;
        if (codenameOneSettings.exists()) {
            // This is an ant project
            try (FileInputStream fis = new FileInputStream(codenameOneSettings)) {
                settingsProps.load(fis);
            }

            settingsProps.put("codename1.packageName", "${packageName}");
            settingsProps.put("codename1.mainName", "${mainName}");
            try (FileOutputStream fos = new FileOutputStream(codenameOneSettings)) {
                settingsProps.store(fos, "Updated packageName and mainName");
            }
            File srcDir = new File(projectRoot, "src");
            if (srcDir.exists()) {
                convertToTemplate(packageName, mainName, srcDir);
            }

            path = packageName.replace('.', File.separatorChar);
            File packageDirectory = new File(srcDir, path);
            if (packageDirectory.exists()) {
                File dest = new File(srcDir, "__packagePath__");
                FileUtils.moveDirectory(packageDirectory, dest);
                for (File child : dest.listFiles()) {
                    if (child.getName().equals(mainName + ".java") || child.getName().equals(mainName + ".kt") || child.getName().equals(mainName + ".mirah")) {
                        File destMain = new File(dest, "__mainName__" + child.getName().substring(child.getName().lastIndexOf(".")));
                        FileUtils.moveFile(child, destMain);
                    }
                }
                convertToTemplate(packageName, mainName, dest);

            }

            return;
        }

        codenameOneSettings = new File(projectRoot, path("common", "codenameone_settings.properties"));
        if (codenameOneSettings.exists()) {
            // This is a maven project.
            // This is an ant project
            try (FileInputStream fis = new FileInputStream(codenameOneSettings)) {
                settingsProps.load(fis);
            }

            settingsProps.put("codename1.packageName", "${packageName}");
            settingsProps.put("codename1.mainName", "${mainName}");
            try (FileOutputStream fos = new FileOutputStream(codenameOneSettings)) {
                settingsProps.store(fos, "Updated packageName and mainName");
            }
            for (String lang : new String[]{"java", "kotlin", "mirah", "resources"}) {
                File srcDir = new File(codenameOneSettings.getParentFile(), path("src", "main", lang));
                if (srcDir.exists()) {
                    convertToTemplate(packageName, mainName, srcDir);
                }

                path = packageName.replace('.', File.separatorChar);
                File packageDirectory = new File(srcDir, path);
                if (packageDirectory.exists()) {
                    File dest = new File(srcDir, "__packagePath__");
                    FileUtils.moveDirectory(packageDirectory, dest);
                    for (File child : dest.listFiles()) {
                        if (child.getName().equals(mainName + ".java") || child.getName().equals(mainName + ".kt") || child.getName().equals(mainName + ".mirah")) {
                            File destMain = new File(dest, "__mainName__" + child.getName().substring(child.getName().lastIndexOf(".")));
                            FileUtils.moveFile(child, destMain);


                        }
                    }
                    convertToTemplate(packageName, mainName, dest);

                }
            }

            return;
        }

    }

    private void convertToTemplate(String packageName, String mainName, File file) throws IOException {
        if (file.isFile()) {
            String contents = FileUtils.readFileToString(file, "UTF-8");
            String origContents = contents;
            if (file.getName().endsWith(".java") || file.getName().endsWith(".kt") || file.getName().endsWith(".mirah"));
            {
                String pattern = "package " + packageName + "";
                String replacement = "package ${packageName}";
                contents = contents.replace(pattern, replacement);
            }
            {
                String pattern = "import " + packageName + ".";
                String replacement = "import ${packageName}.";
                contents = contents.replace(pattern, replacement);
            }
            {
                String pattern = "class " + mainName + " ";
                String replacement = "class ${mainName} ";
                contents = contents.replace(pattern, replacement);
            }
            if (!contents.equals(origContents)) {

                FileUtils.writeStringToFile(file, contents, "UTF-8");
            }

        } else if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                convertToTemplate(packageName, mainName, child);
            }
        }
    }

    public void processFiles() throws IOException {
        process(projectRoot);
        File cn1SettingsFile = new File(projectRoot, "codenameone_settings.properties");
        if (!cn1SettingsFile.exists()) {
            cn1SettingsFile = new File(projectRoot, path("common", "codenameone_settings.properties"));
        }
        if (cn1SettingsFile.exists()) {
            SortedProperties props = new SortedProperties();
            try(FileInputStream fis = new FileInputStream(cn1SettingsFile)) {

                props.load(fis);
            }
            props.setProperty("codename1.packageName", properties.getProperty("packageName"));
            props.setProperty("codename1.mainName", properties.getProperty("mainName"));
            try (FileOutputStream fos = new FileOutputStream(cn1SettingsFile)) {
                props.store(fos, "Updated mainName and packageName");
            }
        }
    }

    public String processContent(String contents) {
        for (String key : properties.stringPropertyNames()) {
            if (key.contains(".")) continue;
            contents = contents.replace("${" + key + "}", properties.getProperty(key));
        }
        return contents;
    }

    public File processFileName(File file) {
        properties.setProperty("packagePath", properties.getProperty("packageName").replace('.', File.separatorChar));
        String name = file.getName();
        for (String key : properties.stringPropertyNames()) {
            name = name.replace("__" + key + "__", properties.getProperty(key));
        }
        if (!name.equals(file.getName())) {
            File dest = new File(file.getParentFile(), name);
            return dest;
        }
        return file;
    }

    private File process(File file) throws IOException {
        if (!properties.containsKey("mainName")) {
            throw new IllegalStateException("Properties must contain mainName property");
        }
        if (!properties.containsKey("packageName")) {
            throw new IllegalStateException("Properties must contains pacakgeName property");
        }
        properties.put("packagePath", properties.getProperty("packageName").replace('.', File.separatorChar));
        String name = file.getName();
        if (file.isFile()) {
            String contents = FileUtils.readFileToString(file, "UTF-8");
            String newContent = processContent(contents);
            if (!contents.equals(newContent)) {
                FileUtils.writeStringToFile(file, newContent, "UTF-8");
            }
            File newFile = processFileName(file);
            if (!newFile.equals(file)) {
                newFile.getParentFile().mkdirs();
                FileUtils.moveFile(file, newFile);
                if (isDirectoryEmpty(file.getParentFile())) {
                    file.getParentFile().delete();
                }
            }
            return newFile;
        } else if (file.isDirectory()) {
            File newFile = processFileName(file);
            if (!newFile.equals(file)) {
                newFile.getParentFile().mkdirs();
                FileUtils.moveDirectory(file, newFile);
                if (isDirectoryEmpty(file.getParentFile())) {
                    file.getParentFile().delete();
                }
                file = newFile;
            }
            for (File child : file.listFiles()) {
                File moved = process(child);
            }
            return file;
        } else  {
            return file;
        }




    }



}
