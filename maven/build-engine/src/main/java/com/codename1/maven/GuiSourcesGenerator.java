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
package com.codename1.maven;

import com.codename1.build.BuildExecutionException;
import com.codename1.build.BuildFailureException;
import com.codename1.build.Log;
import org.apache.commons.io.FileUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.function.Function;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import static com.codename1.maven.PathUtil.path;

/// Generates Java sources from a project's visual definitions: the legacy GUI
/// builder's XML (`src/main/guibuilder`, through the build client) and CodeRAD
/// view templates (`src/main/rad/views`, into an `Abstract<View>` class each).
///
/// The body of the Maven plugin's `generate-gui-sources` goal, shared with the
/// Gradle plugin's `generateGuiSources` task.
public final class GuiSourcesGenerator {
    private final Log log;
    private final File projectDir;
    private final File radViewsDir;
    private final File radGeneratedSourcesDir;
    private final File generatedSourcesDir;

    /// @param projectDir the directory holding the settings file
    /// @param radViewsDir the CodeRAD view templates
    /// @param radGeneratedSourcesDir where the view classes go; a source root
    /// @param generatedSourcesDir where the XML schemas the templates reference go
    public GuiSourcesGenerator(Log log, File projectDir, File radViewsDir, File radGeneratedSourcesDir,
                               File generatedSourcesDir) {
        this.log = log;
        this.projectDir = projectDir;
        this.radViewsDir = radViewsDir;
        this.radGeneratedSourcesDir = radGeneratedSourcesDir;
        this.generatedSourcesDir = generatedSourcesDir;
    }

    /// Runs the build client's legacy GUI builder generator over `guiDir`,
    /// writing into `srcDir`.
    public void generateLegacyGui(File buildClientJar, File srcDir, File guiDir) throws BuildExecutionException {
        System.setProperty("javax.xml.bind.context.factory", "com.sun.xml.bind.v2.ContextFactory");
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{buildClientJar.toURI().toURL()},
                GuiSourcesGenerator.class.getClassLoader())) {
            Class<?> clazz = classLoader.loadClass("com.codename1.build.client.GenerateGuiSources");
            Object g = clazz.getDeclaredConstructor().newInstance();
            clazz.getMethod("setSrcDir", File.class).invoke(g, srcDir);
            clazz.getMethod("setGuiDir", File.class).invoke(g, guiDir);
            clazz.getMethod("execute").invoke(g);
        } catch (Exception e) {
            throw new BuildExecutionException("Failed to load and execute GenerateGuiSources", e);
        }
    }

    /// Generates the view classes for every template under the views directory.
    ///
    /// @return whether there is a views directory (and so a generated source root)
    public boolean generateRadViews() throws BuildFailureException {
        File radViews = getRADViewsDirectory();
        log.debug("Looking for views in " + radViews);
        if (!radViews.isDirectory()) {
            return false;
        }
        Exception res = forEach(radViews, child -> {
            if (!child.getName().endsWith(".xml")) {
                return null;
            }
            File destClassFile = getDestClassForRADView(child);
            log.debug("Found view " + child + ".  Checking against " + destClassFile);
            if (!destClassFile.exists() || child.lastModified() > destClassFile.lastModified()) {
                try {
                    generateRADViewClass(child);
                } catch (IOException ex) {
                    return new BuildFailureException("Failed to generate class for RAD fragment XML file " + child, ex);
                }
            }
            return null;
        });
        if (res != null) {
            throw new BuildFailureException("Failed to compile RAD views:" + res.getMessage(), res);
        }
        return true;
    }

    private File getRADViewsDirectory() {
        return radViewsDir;
    }

    private File getRADGeneratedSourcesDirectory() {
        return radGeneratedSourcesDir;
    }

    private static Exception forEach(File root, Function<File, Exception> callback) {
        Exception res = callback.apply(root);
        if (res != null) return res;
        if (root.isDirectory()) {
            for (File child : root.listFiles()) {
                res = forEach(child, callback);
                if (res != null) return res;
            }
        }
        return null;

    }

    private File getDestClassForRADView(File viewXMLFile) {
        String ext = viewXMLFile.getName().substring(viewXMLFile.getName().lastIndexOf("."));
        String base = viewXMLFile.getName().substring(0, viewXMLFile.getName().lastIndexOf("."));
        File viewsDirectory = getRADViewsDirectory();

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

        File genSrcDir =  new File(getRADGeneratedSourcesDirectory(), pathSb.substring(0, pathSb.length()-1));

        File out =  new File(genSrcDir, base + ".java");
        out = new File(out.getParentFile(), "Abstract" + out.getName());
        return out;

    }

    private String getPackageForRADView(File viewXMLFile) {
        String ext = viewXMLFile.getName().substring(viewXMLFile.getName().lastIndexOf("."));
        String base = viewXMLFile.getName().substring(0, viewXMLFile.getName().lastIndexOf("."));
        File viewsDirectory = getRADViewsDirectory();

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

    private void generateRADViewClass(File xmlViewFile) throws IOException {
        parentEntityViewClass = "AbstractEntityView";
        viewModelType = "Entity";
        log.debug("Generating RAD View for XML template "+xmlViewFile);
        StringBuilder sb = new StringBuilder();

        String packageName = getPackageForRADView(xmlViewFile);
        String className = xmlViewFile.getName().substring(0, xmlViewFile.getName().indexOf("."));

        className = "Abstract" + className;

        String radViewString = FileUtils.readFileToString(xmlViewFile, "utf-8");
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
        sb.append(StringEscapeUtils.escapeJava(radViewString));
        sb.append("\";\n");
        sb.append("    public ").append(className).append("(ViewContext<T> context) {\n");
        sb.append("        super(context);\n");
        sb.append("    }\n\n");

        sb.append("}\n");






        File destFile = getDestClassForRADView(xmlViewFile);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        log.debug("Updating "+destFile);
        FileUtils.writeStringToFile(destFile, sb.toString(), "utf-8");
    }

    private static final String RAD_XML_NAMESPACE = "http://www.codenameone.com/rad";

    private String parentEntityViewClass = "AbstractEntityView";
    private String viewModelType = "Entity";


    private void generateSchemaFor(File xmlViewFile, String contents) throws IOException {
        File generatedSources = generatedSourcesDir;
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
            log.info("Writing XSD alias file at "+xsdAliasFile);
            FileUtils.writeStringToFile(xsdAliasFile, sb.toString(), "UTF-8");

        }
        if (!contents.contains("xsi:noNamespaceSchemaLocation=\"") || !contents.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")) {
            int rootTagStart = contents.indexOf("?>");
            if (rootTagStart < 0) {
                log.info("Not adding schema declaration to "+xmlViewFile+" because it failed to find the root element.  The file may be malformed.");
                return;
            }
            rootTagStart = contents.indexOf("<", rootTagStart);
            if (rootTagStart < 0) {
                log.info("Not adding schema declaration to "+xmlViewFile+" because it failed to find the root element.  The file may be malformed.");
                return;
            }
            int rootTagEnd = contents.indexOf(">", rootTagStart);
            if (rootTagEnd < 0) {
                log.info("Not adding schema declaration to "+xmlViewFile+" because it failed to find the close of the root element. The file may be malformed.");
            }
            String toInject = !contents.contains("xsi:noNamespaceSchemaLocation=\"") ? " xsi:noNamespaceSchemaLocation=\"" + baseName + ".xsd\"" : "";
            if (!contents.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")) {
                toInject += " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
            }
            if (!contents.substring(0, rootTagEnd).contains("xsi:xsi:noNamespaceSchemaLocation") || !contents.substring(0, rootTagEnd).contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")) {
                contents = contents.substring(0, rootTagEnd) + toInject + contents.substring(rootTagEnd);
                FileUtils.writeStringToFile(xmlViewFile, contents, "utf-8");
                log.info("Injected schema declaration into document element of " + xmlViewFile);
            }
        }





    }

    private StringBuilder importStatements = new StringBuilder();

    private String addElementIdentifiersToXML(String xml) throws IOException {
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
        TransformerFactory tf = createTransformerFactory();
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

    private static TransformerFactory createTransformerFactory() {
        try {
            return TransformerFactory.newInstance();
        } catch (Error ex) {
            return createJdkTransformerFactory(ex);
        }
    }

    private static TransformerFactory createJdkTransformerFactory(Error rootCause) {
        try {
            return TransformerFactory.newInstance("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl", GuiSourcesGenerator.class.getClassLoader());
        } catch (Throwable fallbackError) {
            rootCause.addSuppressed(fallbackError);
            throw rootCause;
        }
    }



}
