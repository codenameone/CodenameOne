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
package com.codename1.mcp;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Guards the Codex `config.toml` writer. The file being edited is the user's own Codex
/// configuration, so the two properties that matter are that everything the entry does not
/// own survives the edit unchanged, and that a document the editor cannot make sense of is
/// refused rather than rewritten.
class MCPTomlTest {
    private static final String SERVER = "cn1-my-app";

    /// The byte order mark a UTF-8 config is allowed to start with.
    private static final char BOM = '\uFEFF';

    private static final String EXISTING =
            "# Codex configuration\n"
            + "model = \"gpt-5\"\n"
            + "approval_policy = \"on-request\"\n"
            + "\n"
            + "[mcp_servers.docs]\n"
            + "command = \"docs-server\"\n"
            + "args = [\"--stdio\"]\n"
            + "\n"
            + "[tui]\n"
            + "theme = \"dark\"\n";

    private static MCPClientDescriptor descriptor() {
        List<String> args = new ArrayList<String>();
        args.add("-cp");
        args.add("/Users/me/My Project/target/classes");
        args.add("com.codename1.impl.javase.MCPStdioLauncher");
        args.add("--attach");
        args.add("8765");
        return new MCPClientDescriptor(SERVER, "/usr/bin/java", args);
    }

    private static MCPClientDescriptor descriptorWithEnv() {
        Map<String, String> env = new LinkedHashMap<String, String>();
        env.put("CN1_HOME", "/opt/cn1");
        return new MCPClientDescriptor(SERVER, "java", new ArrayList<String>(), env);
    }

    private static String apply(String toml, MCPClientDescriptor descriptor) {
        MCPToml.Result result = MCPToml.applyServerEntry(toml, SERVER, descriptor);
        assertTrue(result.isApplied(), "expected the edit to be applied: " + result.getProblem());
        assertNotNull(result.getText());
        return result.getText();
    }

    private static String refuse(String toml, MCPClientDescriptor descriptor) {
        MCPToml.Result result = MCPToml.applyServerEntry(toml, SERVER, descriptor);
        assertFalse(result.isApplied(), "expected the edit to be refused");
        assertNull(result.getText());
        assertNotNull(result.getProblem());
        return result.getProblem();
    }

    @Test
    void writesTheCodexTableIntoAnEmptyConfig() {
        assertEquals("[mcp_servers.cn1-my-app]\n"
                + "command = \"/usr/bin/java\"\n"
                + "args = [\"-cp\", \"/Users/me/My Project/target/classes\", "
                + "\"com.codename1.impl.javase.MCPStdioLauncher\", \"--attach\", \"8765\"]\n",
                apply("", descriptor()));
    }

    @Test
    void appendsWithoutTouchingExistingSettings() {
        String updated = apply(EXISTING, descriptor());
        assertTrue(updated.startsWith(EXISTING),
                "the user's settings must survive the edit unchanged:\n" + updated);
        assertTrue(updated.indexOf("[mcp_servers.cn1-my-app]") > 0);
        // The other server is left exactly as it was.
        assertTrue(updated.indexOf("[mcp_servers.docs]\ncommand = \"docs-server\"") > 0);
    }

    @Test
    void replacesTheEntryRatherThanDuplicatingIt() {
        String once = apply(EXISTING, descriptor());
        String twice = apply(once, descriptor());
        assertEquals(once, twice);
        assertEquals(once.indexOf("[mcp_servers.cn1-my-app]"),
                once.lastIndexOf("[mcp_servers.cn1-my-app]"));
    }

    @Test
    void replacingAnEntryInTheMiddleKeepsWhatFollows() {
        String doc = "[mcp_servers.cn1-my-app]\n"
                + "command = \"stale\"\n"
                + "args = [\"gone\"]\n"
                + "\n"
                + "[tui]\n"
                + "theme = \"dark\"\n";
        String updated = apply(doc, descriptor());
        assertTrue(updated.endsWith("\n[tui]\ntheme = \"dark\"\n"), updated);
        assertEquals(-1, updated.indexOf("stale"));
        assertEquals(-1, updated.indexOf("gone"));
    }

    @Test
    void environmentGoesIntoItsOwnSubTable() {
        assertEquals("[mcp_servers.cn1-my-app]\n"
                + "command = \"java\"\n"
                + "args = []\n"
                + "\n"
                + "[mcp_servers.cn1-my-app.env]\n"
                + "CN1_HOME = \"/opt/cn1\"\n", apply("", descriptorWithEnv()));
    }

    @Test
    void removingTakesTheSubTablesAndLeavesTheRestAsItWas() {
        String withEntry = apply(EXISTING, descriptorWithEnv());
        assertTrue(withEntry.indexOf("[mcp_servers.cn1-my-app.env]") > 0);
        assertEquals(EXISTING, apply(withEntry, null));
    }

    @Test
    void removingAnEntryThatIsNotThereChangesNothing() {
        assertEquals(EXISTING, apply(EXISTING, null));
        assertEquals("", apply("", null));
    }

    @Test
    void consolidatesTablesTheUserWroteOutOfOrder() {
        String doc = "[mcp_servers.cn1-my-app]\n"
                + "command = \"stale\"\n"
                + "\n"
                + "[other]\n"
                + "z = 1\n"
                + "\n"
                + "[mcp_servers.cn1-my-app.env]\n"
                + "STALE = \"1\"\n";
        String updated = apply(doc, descriptor());
        assertEquals(-1, updated.indexOf("STALE"));
        assertEquals(-1, updated.indexOf("stale"));
        assertTrue(updated.indexOf("[other]\nz = 1") > 0);
        assertEquals(updated.indexOf("[mcp_servers.cn1-my-app]"),
                updated.lastIndexOf("[mcp_servers.cn1-my-app]"));
    }

    @Test
    void quotesAServerNameThatIsNotABareKey() {
        // Written bare, a dot would make this two nested tables instead of one server.
        MCPClientDescriptor descriptor =
                new MCPClientDescriptor("cn1-my.app 2", "java", new ArrayList<String>());
        MCPToml.Result result = MCPToml.applyServerEntry("", "cn1-my.app 2", descriptor);
        assertTrue(result.isApplied());
        assertTrue(result.getText().startsWith("[mcp_servers.\"cn1-my.app 2\"]\n"),
                result.getText());
    }

    @Test
    void escapesQuotesBackslashesAndControlCharacters() {
        assertEquals("\"a\\\"b\\\\c\\td\\ne\"", MCPToml.renderString("a\"b\\c\td\ne"));
        assertEquals("\"\\u0000\\u001F\"",
                MCPToml.renderString(new String(new char[] {0, 0x1f})));
        // A bare key needs no quoting; anything else does.
        assertEquals("cn1-my-app", MCPToml.renderKey("cn1-my-app"));
        assertEquals("\"has space\"", MCPToml.renderKey("has space"));
        assertEquals("\"has.dot\"", MCPToml.renderKey("has.dot"));
        assertEquals("\"\"", MCPToml.renderKey(""));
    }

    @Test
    void keepsTheDocumentsOwnLineEnding() {
        String updated = apply("model = \"gpt-5\"\r\n", descriptor());
        assertTrue(updated.indexOf("[mcp_servers.cn1-my-app]\r\n") > 0, updated);
        assertEquals(-1, updated.replace("\r\n", "").indexOf('\n'));
    }

    @Test
    void keepsAByteOrderMark() {
        String updated = apply(BOM + "model = \"gpt-5\"\n", descriptor());
        assertEquals(BOM, updated.charAt(0));
        assertEquals(0, updated.lastIndexOf(BOM));
    }

    @Test
    void aTableHeaderInsideAMultiLineStringIsNotATable() {
        // The scan has to read the shape of the document rather than search its text, or
        // this banner would look like a table of the server's own.
        String doc = "banner = \"\"\"\n[mcp_servers.cn1-my-app]\ncommand = \"evil\"\n\"\"\"\n";
        String updated = apply(doc, descriptor());
        assertTrue(updated.startsWith(doc), updated);
        assertTrue(updated.indexOf("command = \"evil\"") > 0);
    }

    @Test
    void aBracketInsideAnArrayIsNotATableHeader() {
        String doc = "matrix = [\n[1, 2],\n[3, 4],\n]\ninline = { a = \"]\", b = 1 } # note\n";
        assertTrue(apply(doc, descriptor()).startsWith(doc));
    }

    @Test
    void aMultiLineStringMayEndInExtraQuotes() {
        // TOML lets the VALUE end in one or two quotes, so the closing run is four or five
        // characters and only the last three are the delimiter. Reading the first three as
        // the terminator left a stray quote behind and refused a perfectly valid config.
        String doc = "a = \"\"\"ends in one quote\"\"\"\"\n"
                + "b = \"\"\"ends in two quotes\"\"\"\"\"\n"
                + "c = '''literal ends in one quote''''\n"
                + "d = \"\"\"two \"\" inside\"\"\"\n";
        String updated = apply(doc, descriptor());
        assertTrue(updated.startsWith(doc), updated);
        assertTrue(updated.indexOf("[mcp_servers.cn1-my-app]") > 0);
    }

    @Test
    void refusesAConfigThatIsNotValidToml() {
        assertTrue(refuse("[mcp_servers.docs]\ncommand = \"unterminated\n", descriptor())
                .indexOf("not valid TOML") >= 0);
        assertTrue(refuse("[unclosed\n", descriptor()).indexOf("not valid TOML") >= 0);
        // A container has to close with the delimiter it opened with. Counting depth
        // alone accepted this, and the file was then edited despite the promise not to.
        assertTrue(refuse("value = [}\n", descriptor()).indexOf("wrong delimiter") >= 0);
        assertTrue(refuse("value = { a = 1 ]\n", descriptor()).indexOf("wrong delimiter") >= 0);
        assertTrue(refuse("value = [[1, 2}]\n", descriptor()).indexOf("wrong delimiter") >= 0);
    }

    @Test
    void refusesShapesItWouldHaveToGuessAt() {
        // Assigning mcp_servers itself is fatal: TOML forbids a [mcp_servers.x] header
        // after it, so appending one would produce a file Codex cannot read.
        assertTrue(refuse("mcp_servers = { docs = { command = \"d\" } }\n", descriptor())
                .indexOf("declared as a value") >= 0);
        // A root dotted key that names THIS server would be a second declaration of it.
        assertTrue(refuse("mcp_servers.cn1-my-app.command = \"d\"\n", descriptor())
                .indexOf("dotted key") >= 0);
        // The server itself declared in a shape the editor does not rewrite.
        assertTrue(refuse("[mcp_servers]\n\"cn1-my-app\" = { command = \"x\" }\n", descriptor())
                .indexOf("inline table") >= 0);
        assertTrue(refuse("[[mcp_servers.cn1-my-app]]\ncommand = \"x\"\n", descriptor())
                .indexOf("array of tables") >= 0);
        assertTrue(refuse("[mcp_servers.cn1-my-app]\na = 1\n[mcp_servers.cn1-my-app]\nb = 2\n",
                descriptor()).indexOf("more than once") >= 0);
    }

    @Test
    void anotherServersRootDottedKeyDoesNotBlockThisOne() {
        // `mcp_servers.docs.command = "d"` leaves mcp_servers defined by dotted keys, and
        // TOML explicitly allows a [table] header to add a sub-table to one of those. This
        // used to refuse every registration, and every removal, because of a neighbour.
        String doc = "mcp_servers.docs.command = \"d\"\n";
        String updated = apply(doc, descriptor());
        assertTrue(updated.startsWith(doc), updated);
        assertTrue(updated.indexOf("[mcp_servers.cn1-my-app]") > 0);
        // ...and the entry can be taken out again.
        assertEquals(doc, apply(updated, null));
    }

    @Test
    void leavesAnotherServersUnusualShapeAlone() {
        // Only the entry being written has to be in a shape the editor understands.
        String doc = "[mcp_servers]\ndocs = { command = \"d\" }\n";
        String updated = apply(doc, descriptor());
        assertTrue(updated.startsWith(doc), updated);
        assertTrue(updated.indexOf("[mcp_servers.cn1-my-app]") > 0);
    }
}
