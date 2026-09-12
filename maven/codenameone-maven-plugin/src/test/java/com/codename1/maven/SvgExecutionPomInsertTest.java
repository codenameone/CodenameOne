/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Covers the pom edit that repairs a project created before the build-time SVG
 * transcoder existed.
 *
 * <p>This is the one part of the repair that writes to a file the developer
 * owns, so it is tested as a pure text transformation: that it puts the
 * execution where Maven will actually read it, that it leaves the rest of the
 * document untouched byte for byte, and that it declines rather than guesses
 * when the pom is not shaped the way it expects.</p>
 */
public class SvgExecutionPomInsertTest {

    /** A pom shaped like the one the archetype emitted before #5042. */
    private static String oldStyleProjectPom() {
        return "<project>\n"
                + "    <build>\n"
                + "        <plugins>\n"
                + "            <plugin>\n"
                + "                <groupId>org.apache.maven.plugins</groupId>\n"
                + "                <artifactId>maven-compiler-plugin</artifactId>\n"
                + "            </plugin>\n"
                + "            <plugin>\n"
                + "                <groupId>com.codenameone</groupId>\n"
                + "                <artifactId>codenameone-maven-plugin</artifactId>\n"
                + "                <executions>\n"
                + "                    <execution>\n"
                + "                        <id>generate-gui-sources</id>\n"
                + "                        <phase>process-sources</phase>\n"
                + "                        <goals>\n"
                + "                            <goal>generate-gui-sources</goal>\n"
                + "                        </goals>\n"
                + "                    </execution>\n"
                + "                </executions>\n"
                + "            </plugin>\n"
                + "        </plugins>\n"
                + "    </build>\n"
                + "</project>\n";
    }

    @Test
    public void addsTheGoalWhereMavenWillReadIt() throws Exception {
        String updated = AbstractCN1Mojo.insertTranscodeSvgExecution(oldStyleProjectPom());
        assertNotNull("an ordinary CN1 pom must be editable", updated);

        Element plugin = codenameOnePlugin(updated);
        List<String> goals = new ArrayList<String>();
        String phase = null;
        for (Element execution : children(child(plugin, "executions"), "execution")) {
            for (Element goal : children(child(execution, "goals"), "goal")) {
                goals.add(goal.getTextContent().trim());
                if ("transcode-svg".equals(goal.getTextContent().trim())) {
                    phase = child(execution, "phase").getTextContent().trim();
                }
            }
        }
        assertTrue("the transcode-svg goal is bound: " + goals, goals.contains("transcode-svg"));
        assertEquals("generate-sources", phase);
        assertTrue("the existing execution survives: " + goals, goals.contains("generate-gui-sources"));
    }

    /**
     * The edit must be an insert and nothing else. A model round-trip would
     * reflow the document, which is why this is done as text -- so prove that
     * every original line is still there, in order, untouched.
     */
    @Test
    public void leavesEveryOtherLineByteIdentical() {
        String original = oldStyleProjectPom();
        String updated = AbstractCN1Mojo.insertTranscodeSvgExecution(original);

        List<String> kept = new ArrayList<String>();
        List<String> added = new ArrayList<String>();
        List<String> originalLines = new ArrayList<String>();
        for (String line : original.split("\n", -1)) {
            originalLines.add(line);
        }
        int next = 0;
        for (String line : updated.split("\n", -1)) {
            if (next < originalLines.size() && originalLines.get(next).equals(line)) {
                kept.add(line);
                next++;
            } else {
                added.add(line);
            }
        }
        assertEquals("every original line survives in order", originalLines.size(), kept.size());
        assertTrue("only the execution block is added: " + added, added.size() <= 12);
        for (String line : added) {
            assertTrue("added line belongs to the execution block: '" + line + "'",
                    line.trim().isEmpty()
                            || line.contains("execution")
                            || line.contains("transcode-svg")
                            || line.contains("goal")
                            || line.contains("phase")
                            || line.startsWith("                        <!--")
                            || line.trim().startsWith("assets,")
                            || line.trim().startsWith("blank 1x1"));
        }
    }

    /** A plugin declared with no executions at all still gets the goal. */
    @Test
    public void createsTheExecutionsElementWhenThereIsNone() throws Exception {
        String pom = "<project>\n"
                + "    <build>\n"
                + "        <plugins>\n"
                + "            <plugin>\n"
                + "                <groupId>com.codenameone</groupId>\n"
                + "                <artifactId>codenameone-maven-plugin</artifactId>\n"
                + "            </plugin>\n"
                + "        </plugins>\n"
                + "    </build>\n"
                + "</project>\n";
        String updated = AbstractCN1Mojo.insertTranscodeSvgExecution(pom);
        assertNotNull(updated);
        Element executions = child(codenameOnePlugin(updated), "executions");
        assertNotNull("an executions element is created", executions);
        assertEquals(1, children(executions, "execution").size());
    }

    /** Windows line endings are preserved rather than normalized to LF. */
    @Test
    public void preservesCrlfLineEndings() {
        String pom = oldStyleProjectPom().replace("\n", "\r\n");
        String updated = AbstractCN1Mojo.insertTranscodeSvgExecution(pom);
        assertNotNull(updated);
        assertEquals("no bare LF is introduced into a CRLF document",
                count(updated, "\r\n"), count(updated, "\n"));
    }

    /**
     * A module that inherits the plugin instead of declaring it cannot be
     * edited here -- adding a plugin element with no version would change what
     * the build resolves. Declining is the correct answer, not a guess.
     */
    @Test
    public void declinesWhenThePluginIsNotDeclaredInThisPom() {
        String pom = "<project>\n"
                + "    <build>\n"
                + "        <plugins>\n"
                + "            <plugin>\n"
                + "                <artifactId>maven-compiler-plugin</artifactId>\n"
                + "            </plugin>\n"
                + "        </plugins>\n"
                + "    </build>\n"
                + "</project>\n";
        assertNull(AbstractCN1Mojo.insertTranscodeSvgExecution(pom));
    }

    /**
     * Every legal spelling of the executions element has to be recognized as
     * the existing one. Appending a second element instead is well-formed XML
     * that Maven refuses outright with "Duplicated tag: 'executions'", so the
     * automatic edit would leave the project unbuildable until its backup was
     * restored -- strictly worse than the blank icons it set out to fix.
     */
    @Test
    public void recognizesEveryExecutionsSpelling() throws Exception {
        String[] spellings = {
                "<executions></executions>",
                "<executions/>",
                "<executions />",
                "<executions combine.children=\"append\"/>",
                "<executions combine.children=\"append\"></executions>",
        };
        for (String spelling : spellings) {
            String pom = "<project>\n"
                    + "    <modelVersion>4.0.0</modelVersion>\n"
                    + "    <build>\n"
                    + "        <plugins>\n"
                    + "            <plugin>\n"
                    + "                <groupId>com.codenameone</groupId>\n"
                    + "                <artifactId>codenameone-maven-plugin</artifactId>\n"
                    + "                " + spelling + "\n"
                    + "            </plugin>\n"
                    + "        </plugins>\n"
                    + "    </build>\n"
                    + "</project>\n";
            String updated = AbstractCN1Mojo.insertTranscodeSvgExecution(pom);
            assertNotNull(spelling + " must be editable", updated);
            assertEquals("exactly one executions element for " + spelling,
                    1, count(updated, "<executions"));

            // The real gate: Maven's own parser, not a DOM well-formedness check.
            org.apache.maven.model.Model model =
                    new MavenXpp3Reader().read(new StringReader(updated));
            List<String> goals = new ArrayList<String>();
            for (org.apache.maven.model.Plugin p : model.getBuild().getPlugins()) {
                for (org.apache.maven.model.PluginExecution e : p.getExecutions()) {
                    goals.addAll(e.getGoals());
                }
            }
            assertTrue("Maven binds the goal for " + spelling + ": " + goals,
                    goals.contains("transcode-svg"));
        }
    }

    /** A tag that merely starts with the same letters is not the element. */
    @Test
    public void doesNotMistakeALongerTagNameForExecutions() throws Exception {
        String pom = "<project>\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "    <build>\n"
                + "        <plugins>\n"
                + "            <plugin>\n"
                + "                <groupId>com.codenameone</groupId>\n"
                + "                <artifactId>codenameone-maven-plugin</artifactId>\n"
                + "                <configuration>\n"
                + "                    <executionsEnabled>true</executionsEnabled>\n"
                + "                </configuration>\n"
                + "            </plugin>\n"
                + "        </plugins>\n"
                + "    </build>\n"
                + "</project>\n";
        String updated = AbstractCN1Mojo.insertTranscodeSvgExecution(pom);
        assertNotNull(updated);
        assertTrue("the unrelated tag is untouched",
                updated.contains("<executionsEnabled>true</executionsEnabled>"));
        org.apache.maven.model.Model model =
                new MavenXpp3Reader().read(new StringReader(updated));
        assertEquals(1, model.getBuild().getPlugins().get(0).getExecutions().size());
    }

    // ---- helpers -----------------------------------------------------

    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    private static Element codenameOnePlugin(String pom) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Element root = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(pom.getBytes("UTF-8")))
                .getDocumentElement();
        for (Element plugin : children(child(child(root, "build"), "plugins"), "plugin")) {
            Element artifactId = child(plugin, "artifactId");
            if (artifactId != null
                    && "codenameone-maven-plugin".equals(artifactId.getTextContent().trim())) {
                return plugin;
            }
        }
        return null;
    }

    private static Element child(Element parent, String name) {
        List<Element> found = children(parent, name);
        return found.isEmpty() ? null : found.get(0);
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<Element>();
        if (parent == null) {
            return result;
        }
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && name.equals(n.getNodeName())) {
                result.add((Element) n);
            }
        }
        return result;
    }
}
