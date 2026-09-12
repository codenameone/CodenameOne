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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

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

    /**
     * A pom that declares a non-UTF-8 encoding has to be read and written in
     * that encoding. Reading ISO-8859-1 bytes as UTF-8 and writing them back
     * under an unchanged declaration silently rewrites developer-owned text,
     * and no XML or model check catches it because both are handed a String
     * that has already lost the original bytes.
     */
    @Test
    public void readsTheEncodingTheDocumentDeclares() {
        byte[] latin1 = ("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
                + "<project><name>Andr\u00e9</name></project>\n")
                .getBytes(StandardCharsets.ISO_8859_1);
        assertEquals(StandardCharsets.ISO_8859_1,
                AbstractCN1Mojo.declaredXmlEncoding(latin1));

        // A round trip through the declared charset preserves the text; the
        // same bytes read as UTF-8 do not.
        Charset declared = AbstractCN1Mojo.declaredXmlEncoding(latin1);
        assertTrue("declared charset round-trips the name",
                new String(latin1, declared).contains("Andr\u00e9"));
        assertTrue("reading the same bytes as UTF-8 loses it",
                !new String(latin1, StandardCharsets.UTF_8).contains("Andr\u00e9"));
    }

    /** No declaration, or a UTF-8 one, means UTF-8. */
    @Test
    public void defaultsToUtf8WithoutADeclaration() {
        assertEquals(StandardCharsets.UTF_8, AbstractCN1Mojo.declaredXmlEncoding(
                "<project/>".getBytes(StandardCharsets.UTF_8)));
        assertEquals(StandardCharsets.UTF_8, AbstractCN1Mojo.declaredXmlEncoding(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><project/>".getBytes(StandardCharsets.UTF_8)));
    }

    /** An encoding this JVM cannot provide is declined, not guessed at. */
    @Test
    public void declinesAnUnsupportedEncoding() {
        assertNull(AbstractCN1Mojo.declaredXmlEncoding(
                "<?xml version=\"1.0\" encoding=\"NOT-A-CHARSET\"?><project/>"
                        .getBytes(StandardCharsets.ISO_8859_1)));
    }

    /**
     * An existing pom.xml.bak is never overwritten. It may be the developer's
     * own backup, or the only surviving copy from an earlier repair, and a
     * routine build destroying it is not a trade this repair gets to make.
     */
    @Test
    public void neverOverwritesAnExistingBackup() throws Exception {
        File dir = temp.newFolder();
        File pom = new File(dir, "pom.xml");
        writeText(pom, "<project/>");

        File first = AbstractCN1Mojo.unusedBackupFile(pom);
        assertEquals("pom.xml.bak", first.getName());

        writeText(first, "a backup somebody else made");
        File second = AbstractCN1Mojo.unusedBackupFile(pom);
        assertEquals("pom.xml.bak.1", second.getName());
        assertEquals("the existing backup is untouched",
                "a backup somebody else made", readText(first));

        writeText(second, "and another");
        assertEquals("pom.xml.bak.2", AbstractCN1Mojo.unusedBackupFile(pom).getName());
    }

    /**
     * The atomic replace swaps the target's inode, so without care the pom
     * takes on the temporary file's mode. Measured before this was handled: a
     * group-writable pom came back rw-r--r--, losing group write on a shared
     * checkout.
     */
    @Test
    public void keepsThePomsPermissionsAcrossTheReplace() throws Exception {
        java.nio.file.FileSystem fs = java.nio.file.FileSystems.getDefault();
        org.junit.Assume.assumeTrue("POSIX only",
                fs.supportedFileAttributeViews().contains("posix"));

        File dir = temp.newFolder();
        File pom = new File(dir, "pom.xml");
        writeText(pom, "<project/>");
        java.nio.file.Files.setPosixFilePermissions(pom.toPath(),
                java.nio.file.attribute.PosixFilePermissions.fromString("rw-rw-r--"));

        AbstractCN1Mojo.writeAtomicallyForTest(pom, "<project2/>", StandardCharsets.UTF_8);

        assertEquals("<project2/>", readText(pom));
        assertEquals("rw-rw-r--", java.nio.file.attribute.PosixFilePermissions.toString(
                java.nio.file.Files.getPosixFilePermissions(pom.toPath())));
    }

    private static void writeText(File f, String text) throws Exception {
        java.io.OutputStream out = new java.io.FileOutputStream(f);
        try {
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    private static String readText(File f) throws Exception {
        byte[] buf = new byte[(int) f.length()];
        java.io.InputStream in = new java.io.FileInputStream(f);
        try {
            int read = 0;
            while (read < buf.length) {
                int n = in.read(buf, read, buf.length - read);
                if (n < 0) break;
                read += n;
            }
        } finally {
            in.close();
        }
        return new String(buf, "UTF-8");
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
