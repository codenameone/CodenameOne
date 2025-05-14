/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase;

import com.codename1.impl.javase.util.MavenUtils;
import com.codename1.io.Log;
import com.codename1.ui.*;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author shannah
 */
public class SourceChangeWatcher implements Runnable {
    private int simulatorReloadVersion = Integer.parseInt(System.getProperty("reload.simulator.count", "0"));
    private WatchService watchService;
    private List<File> watchDirectories = new ArrayList<File>();
    private List<Watch> watches = new ArrayList<Watch>();
    private boolean stopped;
    private Object app;
    private final static boolean isWindows = File.separatorChar == '\\';
    
    public void setApp(Object obj) {
        this.app = obj;
    }
    
    private class Watch {
        private Path path;
        private WatchKey key;
        
        Watch(Path path, WatchKey key) {
            this.path = path;
            this.key = key;
        }
    }
    
    private Path getPathForKey(WatchKey key) {
        for (Watch w : watches) {
            if (key.equals(w.key)) {
                return w.path;
            }
        }
        return null;
    }

    private File getRADViewsDirectory(File viewXMLFile) {
        return new File(findPom(viewXMLFile).getParentFile(), "src" + File.separator + "main" + File.separator + "rad" + File.separator + "views");
    }

    private String getPackageForRADView(File viewXMLFile) {
        String ext = viewXMLFile.getName().substring(viewXMLFile.getName().lastIndexOf("."));
        String base = viewXMLFile.getName().substring(0, viewXMLFile.getName().lastIndexOf("."));
        File viewsDirectory = getRADViewsDirectory(viewXMLFile);

        int levels = 0;
        LinkedList<String> pathParts = new LinkedList<String>();
        File f = viewXMLFile.getParentFile();
        while (f != null && !f.equals(viewsDirectory)) {
            pathParts.addFirst(f.getName());
            f = f.getParentFile();
        }
        StringBuilder pathSb = new StringBuilder();
        for (String part : pathParts) {
            pathSb.append(part).append(".");
        }
        return pathSb.substring(0, pathSb.length()-1);
    }

    private boolean isRADView(Path path) {

        File pom = findPom(path.toFile().getParentFile());
        if (!pom.exists()) return false;
        File radViews = new File(pom.getParentFile(), "src" + File.separator + "main" + File.separator + "rad" + File.separator + "views");
        return path.toFile().getName().endsWith(".xml") && path.startsWith(radViews.toPath());
    }

    private String readFileToString(File file, String encoding) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int)file.length()];
            fis.read(bytes);
            return new String(bytes, encoding);
        }
    }

    private void writeStringToFile(File file, String string, String encoding) throws IOException {
        try (FileOutputStream fos = new FileOutputStream((file))) {
            fos.write(string.getBytes(encoding));
        }
    }

    private File getCN1ProjectDir(File startingPoint) {
        return findPom(startingPoint).getParentFile();
    }

    private void generateSchemaFor(File xmlViewFile, String contents) throws IOException {
        File generatedSources = new File(getCN1ProjectDir(xmlViewFile), "target" + File.separator + "generated-sources");
        File xmlSchemasDirectory = new File(generatedSources, "rad" + File.separator + "xmlSchemas");
        String packageName = getPackageForRADView(xmlViewFile);
        String baseName = xmlViewFile.getName();
        baseName = baseName.substring(0, baseName.lastIndexOf("."));
        File actualXsdFile = new File(xmlSchemasDirectory, packageName.replace('.', File.separatorChar) + File.separator + baseName + ".xsd");
        File xsdAliasFile = new File(xmlViewFile.getParentFile(), baseName + ".xsd");
        if (!xsdAliasFile.exists()) {
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\"?>\n");
            sb.append("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n");
            sb.append("  <xs:include schemaLocation=\"").append("file://").append(actualXsdFile.getAbsolutePath()).append("\"/>\n");
            sb.append("</xs:schema>\n");
            //getLog().info("Writing XSD alias file at "+xsdAliasFile);
            writeStringToFile(xsdAliasFile, sb.toString(), "UTF-8");

        }
        if (!contents.contains("xsi:noNamespaceSchemaLocation=\"") || !contents.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")) {
            int rootTagStart = contents.indexOf("?>");
            if (rootTagStart < 0) {
                //getLog().info("Not adding schema declaration to "+xmlViewFile+" because it failed to find the root element.  The file may be malformed.");
                return;
            }
            rootTagStart = contents.indexOf("<", rootTagStart);
            if (rootTagStart < 0) {
                //getLog().info("Not adding schema declaration to "+xmlViewFile+" because it failed to find the root element.  The file may be malformed.");
                return;
            }
            int rootTagEnd = contents.indexOf(">", rootTagStart);
            if (rootTagEnd < 0) {
                //getLog().info("Not adding schema declaration to "+xmlViewFile+" because it failed to find the close of the root element. The file may be malformed.");
            }
            String toInject = !contents.contains("xsi:noNamespaceSchemaLocation=\"") ? " xsi:noNamespaceSchemaLocation=\"" + baseName + ".xsd\"" : "";
            if (!contents.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")) {
                toInject += " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
            }
            if (!contents.substring(0, rootTagEnd).contains("xsi:xsi:noNamespaceSchemaLocation") || !contents.substring(0, rootTagEnd).contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")) {
                contents = contents.substring(0, rootTagEnd) + toInject + contents.substring(rootTagEnd);
                writeStringToFile(xmlViewFile, contents, "utf-8");
                //getLog().info("Injected schema declaration into document element of " + xmlViewFile);
            }
        }

    }

    private String parentEntityViewClass = "AbstractEntityView", viewModelType = "Entity" ;
    private StringBuilder importStatements = new StringBuilder();
    private String addElementIdentifiersToXML(String xml) throws IOException {
        importStatements.setLength(0);
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            //an instance of builder to parse the specified xml file
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xml.getBytes("utf-8")));
            class Context {
                int index=0;
                void crawl(org.w3c.dom.Element el) {
                    if (index == 0) {
                        if (el.hasAttribute("rad-extends")) {
                            parentEntityViewClass = el.getAttribute("rad-extends");
                        }
                        if (el.hasAttribute("rad-model")) {
                            viewModelType = el.getAttribute("rad-model");
                        }

                    }
                    if (el.getTagName().equalsIgnoreCase("import")) {
                        importStatements.append(el.getTextContent()).append("\n");
                    }
                    el.setAttribute("rad-id", String.valueOf(index++));
                    NodeList children = el.getChildNodes();
                    int len = children.getLength();
                    for (int i=0; i<len; i++) {
                        Node child = (Node)children.item(i);
                        if (!(child instanceof org.w3c.dom.Element)) {
                            continue;
                        }
                        crawl((org.w3c.dom.Element)child);
                    }
                }
            }

            Context ctx = new Context();
            ctx.crawl(doc.getDocumentElement());
            return writeXmlDocumentToString(doc);

        } catch (Exception ex) {
            throw new IOException("Failed to parse CodeRAD XML template", ex);
        }
    }

    private static String writeXmlDocumentToString(Document xmlDocument) throws IOException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();

            StringWriter writer = new StringWriter();

            //transform document to string
            transformer.transform(new DOMSource(xmlDocument), new StreamResult(writer));

            String xmlString = writer.getBuffer().toString();
            return xmlString;
        }
        catch (Exception e) {
            throw new IOException("Failed to output CodeRAD as XML document", e);
        }

    }

    private String escapeJava(String str) {
        return str.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }



    private void generateRADViewClass(File xmlViewFile) throws IOException {
        //getLog().debug("Generating RAD View for XML template "+xmlViewFile);
        parentEntityViewClass = "AbstractEntityView";
        viewModelType = "Entity" ;
        StringBuilder sb = new StringBuilder();

        String packageName = getPackageForRADView(xmlViewFile);
        String className = xmlViewFile.getName().substring(0, xmlViewFile.getName().indexOf("."));

        className = "Abstract" + className;

        String radViewString = readFileToString(xmlViewFile, "utf-8");
        generateSchemaFor(xmlViewFile, radViewString);
        radViewString = addElementIdentifiersToXML(radViewString);

        if (!packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n");
        }
        sb.append("import com.codename1.rad.annotations.RAD;\n");
        sb.append("import com.codename1.rad.ui.AbstractEntityView;\n");
        sb.append("import com.codename1.rad.ui.EntityView;\n");
        sb.append("import com.codename1.rad.models.Entity;\n");
        sb.append("import com.codename1.rad.nodes.Node;\n");
        sb.append("import com.codename1.io.CharArrayReader;\n");
        sb.append("import com.codename1.rad.ui.ViewContext;\n");
        sb.append(importStatements);
        sb.append("@RAD\n");
        String parentClassName = parentEntityViewClass;
        if (parentClassName.equals("AbstractEntityView")) {
            parentClassName += "<T>";
        }
        sb.append("public abstract class ").append(className).append("<T extends ").append(viewModelType).append(">  extends ").append(parentClassName).append(" {\n");
        sb.append("    private static final String FRAGMENT_XML=\"");
        sb.append(escapeJava(radViewString));
        sb.append("\";\n");
        sb.append("    public ").append(className).append("(ViewContext<T> context) {\n");
        sb.append("        super(context);\n");
        sb.append("    }\n\n");

        sb.append("}\n");






        File destFile = getDestClassForRADView(xmlViewFile);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        //getLog().debug("Updating "+destFile);
        writeStringToFile(destFile, sb.toString(), "utf-8");
    }

    private File getDestClassForRADView(File viewXMLFile) {
        String ext = viewXMLFile.getName().substring(viewXMLFile.getName().lastIndexOf("."));
        String base = viewXMLFile.getName().substring(0, viewXMLFile.getName().lastIndexOf("."));
        File viewsDirectory = getRADViewsDirectory(viewXMLFile);

        int levels = 0;
        LinkedList<String> pathParts = new LinkedList<String>();
        File f = viewXMLFile.getParentFile();
        while (f != null && !f.equals(viewsDirectory)) {
            pathParts.addFirst(f.getName());
            f = f.getParentFile();
        }
        StringBuilder pathSb = new StringBuilder();
        for (String part : pathParts) {
            pathSb.append(part).append(File.separator);
        }

        File genSrcDir =  new File(getRADGeneratedSourcesDirectory(viewXMLFile), pathSb.substring(0, pathSb.length()-1));

        File out =  new File(genSrcDir, base + ".java");
        out = new File(out.getParentFile(), "Abstract" + out.getName());
        return out;

    }

    private File getRADGeneratedSourcesDirectory(File viewXMLFile) {
        return new File(getCN1ProjectDir(viewXMLFile), "target" + File.separator + "generated-sources" + File.separator + "rad-views");
    }





    private boolean recompileWithJavac(Path path) throws IOException, InterruptedException {


        int hotReloadSetting = Integer.parseInt(System.getProperty("hotReload", "0"));
        if (hotReloadSetting == 0) return false;

        if (!path.toFile().exists()) {
            return false;
        }
        try {
            String fileContents = readFileToString(path.toFile(), "utf-8");
            if (fileContents.trim().isEmpty()) {
                // don't compile empty file.

                return false;
            }
        } catch (Exception ex) {
            // Failed to read file.
            return false;
        }

        File f = path.toFile();
        File pom = findPom(f.getParentFile());
        if (pom == null) {
            System.out.println("Skipping recompile of "+path+" because no pom.xml was found");
            return false;
        }


        String javaHome = System.getProperty("java.home");
        File javac = MavenUtils.findJavac();
        if (javac == null) {
            javac = new File(new File(javaHome), "bin" + File.separator + "javac");
        }
        if (!javac.exists()) {
            javac = new File(javac.getParentFile(), "javac.exe");

        }
        if (!javac.exists()) {
            Log.p("Not recompiling because javac could not be found.");
            return false;
        }



        final boolean isRADView = isRADView(path);


        if (isRADView) {
            generateRADViewClass(path.toFile());
        }

        StringBuilder classPath = new StringBuilder();
        File classDestination = new File(getCN1ProjectDir(path.toFile()), "target" + File.separator + "classes");
        classPath.append(classDestination.getAbsolutePath()).append(File.pathSeparator);
        classPath.append(System.getProperty("cn1.maven.compileClasspathElements", ""));

        boolean isKotlinFile = path.toFile().getName().endsWith(".kt");

        File generatedSources = new File(getCN1ProjectDir(path.toFile()), "target" + File.separator + "generated-sources" + File.separator + "annotations");
        File sourcePath = isRADView ? getRADGeneratedSourcesDirectory(path.toFile()) : new File(getCN1ProjectDir(path.toFile()), "src" + File.separator + "main" + File.separator + "java");
        File kotlinSourcePath = isRADView ? getRADGeneratedSourcesDirectory(path.toFile()) : new File(getCN1ProjectDir(path.toFile()), "src" + File.separator + "main" + File.separator + "kotlin");
        String recompilingClass = isKotlinFile ?
                path.toFile().getAbsolutePath().substring(kotlinSourcePath.getAbsolutePath().length()+1) :
                isRADView ?
                getDestClassForRADView(path.toFile()).getAbsolutePath().substring(getRADGeneratedSourcesDirectory(path.toFile()).getAbsolutePath().length()+1) :
                path.toFile().getAbsolutePath().substring(sourcePath.getAbsolutePath().length()+1);
                ;

        System.out.println("Recompiling "+recompilingClass);
        //System.out.println("getestClassForRADView: "+getDestClassForRADView(path.toFile()));
        //System.out.println("getRADGeneratedSourcesDirectory: "+getRADGeneratedSourcesDirectory(path.toFile()));
        //System.out.println("ClassPath: "+classPath);

        ProcessBuilder pb;
        if (isKotlinFile) {
            downloadKotlinCompiler();
            File kotlincDir = findKotlinCompilerDir();
            File kotlinc = new File(kotlincDir, "bin" + File.separator + "kotlinc");
            if (isWindows) {
                kotlinc = new File(kotlincDir, "bin" + File.separator + "kotlinc.bat");
            }
            try {
                kotlinc.setExecutable(true, false);
            } catch (Exception ex){}

            pb = new ProcessBuilder(
                    kotlinc.getAbsolutePath(),
                    "-classpath", classPath.toString(),
                    "-d", classDestination.getAbsolutePath(),
                    "-jvm-target", "1.8",
                    recompilingClass
            );
            pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
            pb.directory(kotlinSourcePath);
        } else {
            pb = new ProcessBuilder(
                    javac.getAbsolutePath(),
                    "-cp", classPath.toString(),
                    "-d", classDestination.getAbsolutePath(),
                    "-source", "1.8",
                    "-target", "1.8",
                    "-encoding", "utf-8",
                    "-s", generatedSources.getAbsolutePath(),
                    "-sourcepath", sourcePath.getAbsolutePath(),
                    recompilingClass
            );
            pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
            pb.directory(sourcePath);
        }
        pb.inheritIO();
        Process p = pb.start();
        int result = p.waitFor();
        if (result != 0) {
            return false;
        }
        System.clearProperty("rad.reloadrad.reload.form");
        if (hotReloadSetting == 2 && usingHotswapAgent && isDebug) {
            // Using hotswap agent.  Try actual hot reload

            /// Sleep for a secont to allow the classloader to pick up the new classes.

            int startingVersion = Integer.parseInt(System.getProperty("hotswap-agent-classes-version", "-1"));
            System.out.println("Waiting for version to change from " + startingVersion);
            if (startingVersion < 0) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {

                }
            } else {
                for (int i = 0; i < 30; i++) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {

                    }
                    int newVersion = Integer.parseInt(System.getProperty("hotswap-agent-classes-version", "-1"));
                    if (startingVersion != newVersion) {
                        break;
                    }
                }
            }

            CN.callSeriallyAndWait(new Runnable() {
                public void run() {
                    if (isRADView) {
                        try {
                            Class cls = Class.forName("com.codename1.rad.controllers.FormController");
                            Method method = cls.getMethod("tryCloneAndReplaceCurrentForm", new Class[0]);

                            Boolean result = (Boolean) method.invoke(cls, new Object[0]);
                            if (!result) {
                                CN.restoreToBookmark();
                            }
                        } catch (Exception ex) {
                            CN.restoreToBookmark();
                        }
                    } else {
                        CN.restoreToBookmark();
                    }
                }
            });
            return true;
        } else if (hotReloadSetting == 2) {
            // Not using hotswap agent, but the option is selected to refresh current form.
            stopped = true;
            Window win = SwingUtilities.getWindowAncestor(JavaSEPort.instance.canvas);
            JavaSEPort.instance.deinitializeSync();
            win.dispose();
            registerCurrentFormForReload();
            System.setProperty("reload.simulator", "true");
            return true;

        } else if (hotReloadSetting == 1) {
            stopped = true;
            Window win = SwingUtilities.getWindowAncestor(JavaSEPort.instance.canvas);
            JavaSEPort.instance.deinitializeSync();
            win.dispose();
            System.setProperty("reload.simulator", "true");
            return true;
        }
        return true;


    }

    private void registerCurrentFormForReload() {
        try {
            Class formController = getClass().getClassLoader().loadClass("com.codename1.rad.controllers.FormController");
            if (formController != null) {
                Method getCurrentFormController = formController.getMethod("getCurrentFormController", new Class[0]);
                Object currentFormController = getCurrentFormController.invoke(null, new Object[0]);
                if (currentFormController != null) {
                    System.setProperty("rad.reload.form", currentFormController.getClass().getName());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private boolean recompileWithMaven(Path path) throws IOException, InterruptedException {
        int hotReloadSetting = Integer.parseInt(System.getProperty("hotReload", "0"));
        if (hotReloadSetting == 0) return false;
        if (!path.toFile().exists()) {
            return false;
        }
        try {
            String fileContents = readFileToString(path.toFile(), "utf-8");
            if (fileContents.trim().isEmpty()) {
                // don't compile empty file.

                return false;
            }
        } catch (Exception ex) {
            // Failed to read file.
            return false;
        }


        File f = path.toFile();
        File pom = findPom(f.getParentFile());
        if (pom == null) {
            System.out.println("Skipping recompile of "+path+" because no pom.xml was found");
            return false;
        }
        
        String mavenHome = System.getProperty("maven.home");
        if (mavenHome == null) {
            Log.p("Not recompiling path "+path+" because maven.home system property was not found.");
            return false;
        }
        
        String mavenPath = mavenHome + File.separator + "bin" + File.separator + "mvn";
        if (!new File(mavenPath).exists()) {
            if (new File(mavenPath+".exe").exists()) {
                mavenPath += ".exe";
            } else if (new File(mavenPath+".bat").exists()) {
                mavenPath += ".bat";
            } else if (new File(mavenPath+".cmd").exists()) {
                mavenPath += ".cmd";
            } else {
                Log.p("Not recompiling path "+path+" because " +mavenPath+" could not be found.");
                return false;
            }
        }

        ProcessBuilder pb = new ProcessBuilder(mavenPath, "compile", "-DskipComplianceCheck", "-Dmaven.compiler.useIncrementalCompilation=false", "-e");
        pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
        pb.directory(pom.getParentFile());
        pb.inheritIO();
        Process p = pb.start();
        int result = p.waitFor();
        if (result != 0) {
            return false;
        }
        System.clearProperty("rad.reload.form");
        if (hotReloadSetting == 2 && isDebug && usingHotswapAgent) {


            /// Sleep for a secont to allow the classloader to pick up the new classes.

            int startingVersion = Integer.parseInt(System.getProperty("hotswap-agent-classes-version", "-1"));
            System.out.println("Waiting for version to change from " + startingVersion);
            if (startingVersion < 0) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {

                }
            } else {
                for (int i = 0; i < 30; i++) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {

                    }
                    int newVersion = Integer.parseInt(System.getProperty("hotswap-agent-classes-version", "-1"));
                    if (startingVersion != newVersion) {
                        break;
                    }
                }
            }

            CN.callSeriallyAndWait(new Runnable() {
                public void run() {
                    System.out.println("Restoring to bookmark");
                    CN.restoreToBookmark();
                }
            });
            return true;
        } else if (hotReloadSetting == 2) {
            stopped = true;
            Window win = SwingUtilities.getWindowAncestor(JavaSEPort.instance.canvas);
            JavaSEPort.instance.deinitializeSync();
            win.dispose();
            registerCurrentFormForReload();
            System.setProperty("reload.simulator", "true");
            return true;

        } else if (hotReloadSetting == 1) {
            stopped = true;
            Window win = SwingUtilities.getWindowAncestor(JavaSEPort.instance.canvas);
            JavaSEPort.instance.deinitializeSync();
            win.dispose();
            System.setProperty("reload.simulator", "true");
            return true;
        }

        /*
        CN.callSeriallyAndWait(new Runnable() {
            public void run() {

                final Sheet sheet = new Sheet(null, "Source Change Detected");
                Container contentPane = sheet.getContentPane();
                contentPane.setLayout(new BorderLayout());
                contentPane.add(BorderLayout.CENTER, new SpanLabel("Changes were detected to files in the classpath.  Apply these changes now and refresh?"));
                Container buttons = new Container(BoxLayout.y());
                Button refreshSimulator = new Button("Refresh Simulator");
                Button refreshForm = new Button("Refresh Current Form");
                Button ignore = new Button("Ignore");

                refreshSimulator.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        stopped = true;
                        System.setProperty("reload.simulator", "true");
                        sheet.back();
                    }
                });


                refreshForm.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        sheet.back();


                        try {

                            System.setProperty("restore-to-bookmark", "true");
                            CN.restoreToBookmark();

                        } catch (Exception ex) {
                            Log.e(ex);
                        }
                    }
                });

                ignore.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        sheet.back();
                    }
                });

                buttons.addAll(refreshForm, refreshSimulator, ignore);
                contentPane.add(BorderLayout.SOUTH, buttons);
                sheet.setPosition(BorderLayout.CENTER);
                sheet.show();




            }
        });
        */
        return true;
        
    }
    
    private File findPom(File startingPoint) {
        File pom = new File(startingPoint, "pom.xml");
        if (pom.exists()) return pom;
        File parent = startingPoint.getParentFile();
        if (parent != null) {
            return findPom(parent);
        }
        return null;
    }
    
    private void registerWatchRecursive(File directory) throws IOException {
        if (directory.isDirectory()) {
            WatchKey key = directory.toPath().register(watchService, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE}, SensitivityWatchEventModifier.HIGH);
            watches.add(new Watch(directory.toPath(), key));
            for (File child : directory.listFiles()) {
                registerWatchRecursive(child);
            }
        }
                    
    }
    private boolean requiresRecompile;
    private Path fileToRecompile;
    @Override
    public void run() {
        try {
            System.out.println("SourceChangeWatcher running.  Watching directories "+watchDirectories);
            watchService = FileSystems.getDefault().newWatchService();
            for (File directory : watchDirectories) {
               registerWatchRecursive(directory);
                
            }
            
            while (!stopped && Display.isInitialized()) {
                int reloadVersion = Integer.parseInt(System.getProperty("reload.simulator.count", "0"));
                if (reloadVersion != simulatorReloadVersion) {
                    stop();
                    break;
                }
                try {
                    final WatchKey key = watchService.take();
                    
                    if (stopped) {
                        return;
                    }

                    final Path path = getPathForKey(key);
                    requiresRecompile = false;
                    fileToRecompile = null;

                    key.pollEvents().forEach(new Consumer<WatchEvent<?>>() {
                        @Override
                        public void accept(WatchEvent<?> evt) {
                            System.out.println("[Watcher " + SourceChangeWatcher.this + "] File changedL: " + evt.context() + " key=" + key);

                            if (evt.context().toString().endsWith(".java") || evt.context().toString().endsWith(".kt") || evt.context().toString().endsWith(".xml")) {
                                requiresRecompile = true;
                            }
                            File changedFile = new File(path.toFile(), evt.context().toString());
                            File cn1ProjectDir = getCN1ProjectDir(changedFile);
                            File commonSrcDir = cn1ProjectDir == null ? null : new File(cn1ProjectDir, "src" + File.separator + "main" + File.separator + "java");
                            File kotlinSrcDir = cn1ProjectDir == null ? null : new File(cn1ProjectDir, "src" + File.separator + "main" + File.separator + "kotlin");
                            boolean isEligibleKotlinFile = kotlinSrcDir != null && changedFile.exists() && changedFile.getName().endsWith(".kt") && changedFile.toPath().startsWith(kotlinSrcDir.toPath());
                            boolean isEligibleJavaFile = commonSrcDir != null && changedFile.exists() && changedFile.getName().endsWith(".java") && changedFile.toPath().startsWith(commonSrcDir.toPath());
                            if (isRADView(changedFile.toPath()) || isEligibleJavaFile || isEligibleKotlinFile) {
                                fileToRecompile = changedFile.toPath();
                            }
                            if (changedFile.isDirectory()) {
                                if (!watchDirectories.contains(changedFile)) {
                                    addWatchFolder(changedFile);
                                }
                            }
                        }
                    });
                    if (requiresRecompile) {
                    
                        System.out.println("Changes detected in directory "+path);
                        if (fileToRecompile != null && System.getProperty("cn1.maven.compileClasspathElements", null) != null) {
                            try {
                                recompileWithJavac(fileToRecompile);
                            } catch (Exception ex) {
                                Log.e(ex);
                            }
                        } else {
                            recompileWithMaven(path);
                        }
                    }
                   
                    // STEP8: Reset the watch key everytime for continuing to use it for further event polling
                    key.reset();
                } catch (InterruptedException ex) {
                    if (stopped) {
                        return;
                    }
                    Log.e(ex);
                }
            }
        } catch (IOException ex) {
            Log.e(ex);
        }

        
    }
    
    public void stop() {
        stopped = true;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (Exception ex){}
        
    }


    private boolean isDebug, usingHotswapAgent;
    public SourceChangeWatcher() {
        List<String> inputArgs = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments();
        isDebug = inputArgs.toString().indexOf("-agentlib:jdwp") > 0;
        usingHotswapAgent = inputArgs.toString().indexOf("-XX:HotswapAgent") > 0;
    }
    
    public void addWatchFolder(File path) {
        System.out.println("Adding watch folder "+path);
        watchDirectories.add(path);
    }
    
    public boolean hasWatchFolder(File path) {
        return watchDirectories.contains(path);
    }


    private String findKotlinVersion() {
        String classPath = System.getProperty("cn1.class.path", null);
        if (classPath == null) return null;

        Pattern regex = Pattern.compile("kotlin-stdlib-([\\d\\.\\-]+)\\.jar");
        Matcher matcher = regex.matcher(classPath);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void downloadKotlinCompiler() throws IOException {

        String kotlinVersion = findKotlinVersion();
        if (kotlinVersion == null) {
            System.out.println("Cannot find kotlin version.  Skipping kotlin compiler download");
            throw new IOException("Cannot download kotlin compiler because no kotlin version could be found");
        }
        String codenameOneHome = System.getProperty("user.home") + File.separator + ".codenameone";
        String kotlinc = codenameOneHome + File.separator + "kotlinc";
        String kotlincVersionPath = kotlinc + File.separator + kotlinVersion;
        File kotlincVersionDir = new File(kotlincVersionPath);
        if (kotlincVersionDir.exists()) {
            System.out.println(kotlincVersionDir+" already exists");
            return;
        }

        String url = "https://github.com/JetBrains/kotlin/releases/download/v"+kotlinVersion+"/kotlin-compiler-"+kotlinVersion+".zip";
        URL u = new URL(url);
        System.out.print("Downloading kotlin command-line compiler from "+url+"...");
        HttpsURLConnection conn = (HttpsURLConnection)u.openConnection();
        conn.setInstanceFollowRedirects(true);


        unzipFolder(conn.getInputStream(), kotlincVersionDir.toPath());
        System.out.println("Done.");

    }

    private File findKotlinCompilerDir() throws IOException {
        String kotlinVersion = findKotlinVersion();
        if (kotlinVersion == null) {
            System.out.println("Cannot find kotlin version.  Skipping kotlin compiler download");
            throw new IOException("Cannot download kotlin compiler because no kotlin version could be found");
        }
        String codenameOneHome = System.getProperty("user.home") + File.separator + ".codenameone";
        String kotlinc = codenameOneHome + File.separator + "kotlinc";
        String kotlincVersionPath = kotlinc + File.separator + kotlinVersion;
        File kotlincVersionDir = new File(kotlincVersionPath);
        return new File(kotlincVersionDir + File.separator + "kotlinc");


    }


    public static void unzipFolder(InputStream source, Path target) throws IOException {
        System.out.println("Unzipping folder to "+target.toFile());
        try (ZipInputStream zis = new ZipInputStream(source)) {

            // list files in zip
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                System.out.println("Entry: "+zipEntry);
                boolean isDirectory = false;
                // example 1.1
                // some zip stored files and folders separately
                // e.g data/
                //     data/folder/
                //     data/folder/file.txt
                if (zipEntry.getName().endsWith(File.separator)) {
                    isDirectory = true;
                }

                Path newPath = zipSlipProtect(zipEntry, target);

                if (isDirectory) {
                    Files.createDirectories(newPath);
                } else {

                    // example 1.2
                    // some zip stored file path only, need create parent directories
                    // e.g data/folder/file.txt
                    if (newPath.getParent() != null) {
                        if (Files.notExists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
                    }

                    // copy files, nio
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);

                    // copy files, classic
                    /*try (FileOutputStream fos = new FileOutputStream(newPath.toFile())) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }*/
                }

                zipEntry = zis.getNextEntry();

            }
            zis.closeEntry();

        }

    }

    public static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir)
            throws IOException {

        // test zip slip vulnerability
        // Path targetDirResolved = targetDir.resolve("../../" + zipEntry.getName());

        Path targetDirResolved = targetDir.resolve(zipEntry.getName());

        // make sure normalized file still has targetDir as its prefix
        // else throws exception
        Path normalizePath = targetDirResolved.normalize();
        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Bad zip entry: " + zipEntry.getName());
        }

        return normalizePath;
    }


}
