/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.function.Function;


import org.apache.commons.text.StringEscapeUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.surefire.shared.io.FileUtils;

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

/**
 * Goal to generate java sources from the guibuilder files.
 * @author shannah
 */
@Mojo(name="generate-gui-sources", defaultPhase = LifecyclePhase.INITIALIZE)
public class GenerateGuiSourcesMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            return;
        }
        if (System.getProperty("generate-gui-sources-done") != null) {
            return;
        }
        System.setProperty("generate-gui-sources-done", "true");
        System.setProperty("javax.xml.bind.context.factory", "com.sun.xml.bind.v2.ContextFactory");
        String buildClientJarPath = path(System.getProperty("user.home"), ".codenameone", "CodeNameOneBuildClient.jar");
        File jarFile = new File(buildClientJarPath);
        if (!jarFile.exists()) {
            throw new MojoExecutionException(buildClientJarPath + " not found at " + jarFile.getAbsolutePath());
        }
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader())) {
            Class<?> clazz = classLoader.loadClass("com.codename1.build.client.GenerateGuiSources");
            Object g = clazz.getDeclaredConstructor().newInstance();
            clazz.getMethod("setSrcDir", File.class).invoke(g, new File(getCN1ProjectDir(), "src" + File.separator + "main" + File.separator + "java"));
            clazz.getMethod("setGuiDir", File.class).invoke(g, new File(getCN1ProjectDir(), "src" + File.separator + "main" + File.separator + "guibuilder"));
            clazz.getMethod("execute").invoke(g);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to load and execute GenerateGuiSources", e);
        }

        // Generate the RAD templates while we're at it

        File radViews = getRADViewsDirectory();
        getLog().debug("Looking for views in "+radViews);
        if (radViews.isDirectory()) {
            project.addCompileSourceRoot(getRADGeneratedSourcesDirectory().getAbsolutePath());
            Exception res = forEach(radViews, child -> {
                if (!child.getName().endsWith(".xml")) {
                    return null;
                }
                File destClassFile = getDestClassForRADView(child);
                getLog().debug("Found view "+child+".  Checking against "+destClassFile);
                if (!destClassFile.exists() || child.lastModified() > destClassFile.lastModified()) {
                    try {
                        generateRADViewClass(child);
                    } catch (IOException ex) {
                        return new MojoFailureException("Failed to generate class for RAD fragment XML file "+child, ex);
                    }
                }
                return null;
            });
            if (res != null) {
                if (res instanceof MojoExecutionException) {
                    throw (MojoFailureException) res;
                } else {
                    throw new MojoFailureException("Failed to compile RAD views:" + res.getMessage(), res);
                }
            }

        }
    }

    private File getRADViewsDirectory() {
        return new File(getCN1ProjectDir(), path("src", "main", "rad", "views"));
    }

    private File getRADGeneratedSourcesDirectory() {
        return new File(path(project.getBuild().getDirectory(), "generated-sources" , "rad-views"));
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
        getLog().debug("Generating RAD View for XML template "+xmlViewFile);
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
        getLog().debug("Updating "+destFile);
        FileUtils.writeStringToFile(destFile, sb.toString(), "utf-8");
    }

    private static final String RAD_XML_NAMESPACE = "http://www.codenameone.com/rad";

    private String parentEntityViewClass = "AbstractEntityView";
    private String viewModelType = "Entity";


    private void generateSchemaFor(File xmlViewFile, String contents) throws IOException {
        File generatedSources = new File(getCN1ProjectDir(), path("target", "generated-sources"));
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
            getLog().info("Writing XSD alias file at "+xsdAliasFile);
            FileUtils.writeStringToFile(xsdAliasFile, sb.toString(), "UTF-8");

        }
        if (!contents.contains("xsi:noNamespaceSchemaLocation=\"") || !contents.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")) {
            int rootTagStart = contents.indexOf("?>");
            if (rootTagStart < 0) {
                getLog().info("Not adding schema declaration to "+xmlViewFile+" because it failed to find the root element.  The file may be malformed.");
                return;
            }
            rootTagStart = contents.indexOf("<", rootTagStart);
            if (rootTagStart < 0) {
                getLog().info("Not adding schema declaration to "+xmlViewFile+" because it failed to find the root element.  The file may be malformed.");
                return;
            }
            int rootTagEnd = contents.indexOf(">", rootTagStart);
            if (rootTagEnd < 0) {
                getLog().info("Not adding schema declaration to "+xmlViewFile+" because it failed to find the close of the root element. The file may be malformed.");
            }
            String toInject = !contents.contains("xsi:noNamespaceSchemaLocation=\"") ? " xsi:noNamespaceSchemaLocation=\"" + baseName + ".xsd\"" : "";
            if (!contents.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")) {
                toInject += " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
            }
            if (!contents.substring(0, rootTagEnd).contains("xsi:xsi:noNamespaceSchemaLocation") || !contents.substring(0, rootTagEnd).contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")) {
                contents = contents.substring(0, rootTagEnd) + toInject + contents.substring(rootTagEnd);
                FileUtils.writeStringToFile(xmlViewFile, contents, "utf-8");
                getLog().info("Injected schema declaration into document element of " + xmlViewFile);
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



}
