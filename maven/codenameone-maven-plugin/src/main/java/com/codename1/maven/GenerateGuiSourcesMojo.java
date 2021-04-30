/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.maven;

import com.codename1.build.client.GenerateGuiSources;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;


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
import javax.xml.transform.TransformerException;
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
        System.setProperty("javax.xml.bind.context.factory", "com.sun.xml.bind.v2.ContextFactory");
                
        GenerateGuiSources g = new GenerateGuiSources();
        g.setSrcDir(new File(getCN1ProjectDir(), "src" + File.separator + "main" + File.separator + "java"));
        g.setGuiDir(new File(getCN1ProjectDir(), "src" + File.separator + "main" + File.separator + "guibuilder"));
        g.execute();


        // Generate the RAD templates while we're at it

        File radFragments = new File(getCN1ProjectDir(), path("src", "main", "rad", "fragments"));
        getLog().info("Looking for fragments in "+radFragments);
        if (radFragments.isDirectory()) {
            project.addCompileSourceRoot(getRADGeneratedSourcesDirectory().getAbsolutePath());
            for (File child : radFragments.listFiles()) {
                File destClassFile = getDestClassForRADFragment(child);
                getLog().info("Found fragment "+child+".  Checking against "+destClassFile);
                if (!destClassFile.exists() || child.lastModified() > destClassFile.lastModified()) {
                    try {
                        generateRADFragmentClass(child);
                    } catch (IOException ex) {
                        throw new MojoFailureException("Failed to generate class for RAD fragment XML file "+child, ex);
                    }
                }
            }
        }
    }

    private File getRADGeneratedSourcesDirectory() {
        return new File(path(project.getBuild().getDirectory(), "generated-sources" , "rad-fragments"));
    }

    private File getDestClassForRADFragment(File fragment) {
        String ext = fragment.getName().substring(fragment.getName().lastIndexOf("."));
        String base = fragment.getName().substring(0, fragment.getName().lastIndexOf("."));

        File genSrcDir =  getRADGeneratedSourcesDirectory();
        return new File(genSrcDir, base.replace(".", File.separator) + ".java");

    }

    private void generateRADFragmentClass(File fragment) throws IOException {
        getLog().info("Generating RAD Fragment for XML template "+fragment);
        StringBuilder sb = new StringBuilder();
        String ext = fragment.getName().substring(fragment.getName().lastIndexOf("."));
        String base = fragment.getName().substring(0, fragment.getName().lastIndexOf("."));
        String packageName = "";
        String className = base;
        if (base.indexOf(".") > 0) {
            packageName = base.substring(0, base.lastIndexOf("."));
            className = base.substring(base.lastIndexOf(".")+1);
        }

        String xmlFragmentString = FileUtils.readFileToString(fragment, "utf-8");
        xmlFragmentString = addElementIdentifiersToXML(xmlFragmentString);

        if (!packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n");
        }
        sb.append("import com.codename1.rad.annotations.RAD;\n");
        sb.append("import com.codename1.rad.ui.EntityViewFragment;\n");
        sb.append("import com.codename1.rad.ui.EntityView;\n");
        sb.append("import com.codename1.io.CharArrayReader;\n");
        sb.append("@RAD\n");
        sb.append("public abstract class ").append(className).append(" extends EntityViewFragment {\n");
        sb.append("    private static final String FRAGMENT_XML=\"");
        sb.append(StringEscapeUtils.escapeJava(xmlFragmentString));
        sb.append("\";\n");
        sb.append("    public ").append(className).append("(EntityView contextView) {\n");
        sb.append("        super(contextView);\n");
        sb.append("    }\n\n");

        sb.append("}\n");






        File destFile = getDestClassForRADFragment(fragment);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        getLog().debug("Updating "+destFile);
        FileUtils.writeStringToFile(destFile, sb.toString(), "utf-8");
    }

    private static final String RAD_XML_NAMESPACE = "http://www.codenameone.com/rad";

    private String addElementIdentifiersToXML(String xml) throws IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    //an instance of builder to parse the specified xml file
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xml.getBytes("utf-8")));
            class Context {
                int index=0;
                void crawl(org.w3c.dom.Element el) {
                    el.setAttribute("elementId", String.valueOf(index++));
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
