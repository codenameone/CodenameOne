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
package com.codename1.doclet.hugo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.DocumentationTool;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the doclet over a source file written for the awkward cases and checks
 * what it emits.
 *
 * <p>The fragment identifiers are the reason this test exists. They are the
 * addresses the site publishes, they have to match the ones the standard doclet
 * writes exactly, and the encoding has details that look like details until a
 * link dies: type arguments are erased away but arrays keep their brackets, a
 * varargs parameter keeps its ellipsis in the declared spelling and loses it in
 * the erased one, and a method with a type variable answers to both spellings.
 *
 * <p>scripts/website/check-javadoc-parity.py checks the same property against a
 * real javadoc run over the real sources, which is the stronger check but needs
 * a full website build. This one runs in a second.
 */
class HugoDocletTest {

    @TempDir
    static Path workspace;

    private static Path content;

    @BeforeAll
    static void generate() throws IOException {
        Path sources = workspace.resolve("src/p");
        Files.createDirectories(sources);
        content = workspace.resolve("content");

        Files.writeString(sources.resolve("Sample.java"), String.join("\n",
                "package p;",
                "import java.util.List;",
                "import java.util.Map;",
                "",
                "/// A sample type.",
                "///",
                "/// @param <T> the analyzer's result type",
                "///",
                "/// #### See also",
                "///",
                "/// - Other",
                "public class Sample<T> {",
                "    /// A constant",
                "    public static final int LIMIT = 7;",
                "    /// Builds one",
                "    public Sample() {}",
                "    /// Type arguments are erased out of the identifier",
                "    ///",
                "    /// #### Parameters",
                "    ///",
                "    /// - `values`: the values to use",
                "    public void generic(List<String> values) {}",
                "    /// Nested generics collapse to the raw type too",
                "    public void nested(Map<String, List<Integer>> m) {}",
                "    /// Arrays keep their brackets",
                "    public void arrays(byte[] a, int[][] b) {}",
                "    /// Varargs keep the ellipsis",
                "    public void varargs(String... parts) {}",
                "    /// A type variable is spelt twice",
                "    public void variable(T value) {}",
                "    /// A varargs type variable is spelt twice as well",
                "    public void variableVarargs(T... values) {}",
                "    /// Takes a nested type",
                "    public void inner(Sample.Inner value) {}",
                "    /// Returns something documented",
                "    ///",
                "    /// #### Returns",
                "    ///",
                "    /// the limit",
                "    public int limit() { return LIMIT; }",
                "    /// Not part of the API",
                "    ///",
                "    /// @hidden",
                "    public void secret() {}",
                "    /// Nested",
                "    public static class Inner {}",
                "    /// The no-argument overload, declared first on purpose",
                "    public void clear() {}",
                "    /// The overload a reference names explicitly",
                "    ///",
                "    /// #### See also",
                "    ///",
                "    /// - #clear(int)",
                "    /// - Other#greet() with a trailing sentence",
                "    /// - #missing(int,int,int)",
                "    public void clear(int index) {}",
                "    /// A string constant",
                "    public static final String PATTERN = \"EEE, dd MMM\";",
                "    /// A constant holding a control character",
                "    public static final String UNIT = \"\\u001f\";",
                "    /// Returns what it was given.",
                "    ///",
                "    /// @param <V> the value type",
                "    /// @param value the value",
                "    /// @return the value",
                "    public <V> V identity(V value) { return value; }",
                "    /// A long constant",
                "    public static final long BIG = 5L;",
                "    /// Overloads that differ by type, not arity",
                "    ///",
                "    /// #### See also",
                "    ///",
                "    /// - #erase(Object)",
                "    public void erase(int index) {}",
                "    /// The other overload",
                "    public void erase(Object value) {}",
                "    /// Annotated but never tagged",
                "    @Deprecated",
                "    public void annotatedOnly() {}",
                "}",
                ""), StandardCharsets.UTF_8);

        Files.writeString(sources.resolve("Child.java"), String.join("\n",
                "package p;",
                "/// A subclass that refers to inherited members without qualifying them.",
                "///",
                "/// #### See also",
                "///",
                "/// - #ALIGN",
                "/// - #greet()",
                "public class Child extends Other {",
                "    // Deliberately no documentation comment: an override with none",
                "    // takes its parent's whole doc, which is the path that was",
                "    // dropping the text when the parameter had been renamed.",
                "    @Override",
                "    public void accept(String renamed) {}",
                "}",
                ""), StandardCharsets.UTF_8);

        Files.writeString(sources.resolve("Other.java"), String.join("\n",
                "package p;",
                "/// Another type.",
                "public class Other {",
                "    /// Says hello",
                "    public void greet() {}",
                "    /// A constant subclasses refer to as #ALIGN",
                "    public static final int ALIGN = 3;",
                "    /// Takes a value.",
                "    ///",
                "    /// #### Parameters",
                "    ///",
                "    /// - `original`: what the parent called it",
                "    public void accept(String original) {}",
                "}",
                "",
                "/// Inherits everything and refers to it locally.",
                "///",
                "/// #### See also",
                "///",
                "/// - #ALIGN",
                "/// - #greet()",
                "class Ignored {}",
                ""), StandardCharsets.UTF_8);

        // Two packages each holding a type called Shared, and a referrer in each
        // that names it without qualifying. Whichever type a global search
        // happens to reach first, it cannot satisfy both assertions -- which is
        // what makes this test bite. An earlier version compared against one
        // package only and passed with the fix removed.
        Files.writeString(sources.resolve("Shared.java"), String.join("\n",
                "package p;",
                "/// The p one.",
                "public class Shared {}",
                ""), StandardCharsets.UTF_8);
        Files.writeString(sources.resolve("UsesShared.java"), String.join("\n",
                "package p;",
                "/// Refers to Shared without qualifying it.",
                "///",
                "/// #### See also",
                "///",
                "/// - Shared",
                "public class UsesShared {}",
                ""), StandardCharsets.UTF_8);

        Files.writeString(sources.resolve("Listener.java"), String.join("\n",
                "package p;",
                "/// An interface, which inherits nothing from Object.",
                "public interface Listener {",
                "    /// Called back",
                "    void fired();",
                "}",
                ""), StandardCharsets.UTF_8);

        Path other = workspace.resolve("src/q");
        Files.createDirectories(other);
        Files.writeString(other.resolve("Shared.java"), String.join("\n",
                "package q;",
                "/// The q one.",
                "public class Shared {}",
                ""), StandardCharsets.UTF_8);
        Files.writeString(other.resolve("UsesShared.java"), String.join("\n",
                "package q;",
                "/// Refers to Shared without qualifying it.",
                "///",
                "/// #### See also",
                "///",
                "/// - Shared",
                "public class UsesShared {}",
                ""), StandardCharsets.UTF_8);

        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager files = tool.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> units = files.getJavaFileObjects(
                    sources.resolve("Sample.java"), sources.resolve("Other.java"),
                    sources.resolve("Child.java"),
                    sources.resolve("Shared.java"), sources.resolve("UsesShared.java"),
                    sources.resolve("Listener.java"),
                    other.resolve("Shared.java"), other.resolve("UsesShared.java"));
            boolean ok = tool.getTask(null, files, null, HugoDoclet.class,
                    List.of("-d", content.toString(),
                            "-sourcepath", workspace.resolve("src").toString(),
                            "-protected", "-quiet"),
                    units).call();
            assertTrue(ok, "the doclet run failed");
        }
    }

    private static String page(String name) throws IOException {
        return Files.readString(content.resolve("p").resolve(name), StandardCharsets.UTF_8);
    }

    private static void assertAnchor(String page, String anchor) throws IOException {
        // The identifier is written into JSON, so a quote and a backslash are the
        // only characters that could be escaped, and neither appears in one.
        assertTrue(page(page).contains("\"" + anchor + "\""),
                "expected fragment identifier " + anchor + " in " + page);
    }

    @Test
    void erasesTypeArgumentsFromIdentifiers() throws IOException {
        assertAnchor("Sample.md", "generic(java.util.List)");
        assertAnchor("Sample.md", "nested(java.util.Map)");
    }

    @Test
    void keepsArrayBrackets() throws IOException {
        assertAnchor("Sample.md", "arrays(byte[],int[][])");
    }

    @Test
    void keepsTheVarargsEllipsis() throws IOException {
        assertAnchor("Sample.md", "varargs(java.lang.String...)");
    }

    @Test
    void spellsATypeVariableBothWays() throws IOException {
        assertAnchor("Sample.md", "variable(T)");
        assertAnchor("Sample.md", "variable(java.lang.Object)");
    }

    @Test
    void dropsTheEllipsisFromAnErasedVarargs() throws IOException {
        // javadoc gives Stream.of(T... values) both of these, and only the
        // declared spelling keeps the ellipsis. Emitting "java.lang.Object..."
        // for the erasure loses the second address entirely.
        assertAnchor("Sample.md", "variableVarargs(T...)");
        assertAnchor("Sample.md", "variableVarargs(java.lang.Object[])");
    }

    @Test
    void namesConstructorsAndFieldsTheWayJavadocDoes() throws IOException {
        assertAnchor("Sample.md", "<init>()");
        assertAnchor("Sample.md", "LIMIT");
    }

    @Test
    void qualifiesNestedTypesWithDots() throws IOException {
        assertAnchor("Sample.md", "inner(p.Sample.Inner)");
    }

    @Test
    void writesNestedTypesToTheirOwnPageBesideTheOuterOne() throws IOException {
        assertTrue(Files.exists(content.resolve("p/Sample.Inner.md")),
                "a nested type gets Outer.Inner.md, which is what javadoc links");
    }

    @Test
    void publishesTheJavadocUrlAndItsDirectoryAlias() throws IOException {
        String page = page("Sample.md");
        assertTrue(page.contains("\"/javadoc/p/Sample.html\""), "the canonical URL");
        assertTrue(page.contains("\"/javadoc/p/Sample/\""), "the directory alias");
    }

    @Test
    void obeysHidden() throws IOException {
        assertFalse(page("Sample.md").contains("secret"),
                "@hidden removes the member from the API entirely");
    }

    @Test
    void liftsMarkdownSectionsIntoStructure() throws IOException {
        String page = page("Sample.md");
        assertTrue(page.contains("\"the values to use\""), "the parameter's documentation");
        assertTrue(page.contains("\"the limit\""), "the return documentation");
        assertFalse(page.contains("#### Parameters"),
                "a lifted heading must not also remain in the description");
    }

    @Test
    void linksSeeAlsoEntriesThatNameSomethingPublished() throws IOException {
        assertTrue(page("Sample.md").contains("/javadoc/p/Other.html"),
                "a bare type name under #### See also resolves to its page");
    }

    @Test
    void keepsTypeParameterDocumentation() throws IOException {
        // Read into doc.parameters and then never serialized, so the one tag in
        // the framework that carries text (VisionCameraView<T>) was dropped.
        assertTrue(page("Sample.md").contains("the analyzer's result type"),
                "type parameter documentation must reach the page");
    }

    @Test
    void marksAnAnnotatedElementDeprecatedWithoutATag() throws IOException {
        // com.codename1.ui.util.MutableResouce carries @Deprecated and no tag.
        String page = page("Sample.md");
        int at = page.indexOf("annotatedOnly");
        assertTrue(at > 0, "the member is on the page");
        assertTrue(page.indexOf("\"deprecated\": true", at) > 0
                        && page.indexOf("\"deprecated\": true", at) < at + 400,
                "the annotation alone must set the deprecated flag");
    }

    @Test
    void rendersConstantsAsJavaLiterals() throws IOException {
        String page = page("Sample.md");
        assertTrue(page.contains("\\\"EEE, dd MMM\\\""), "a String constant keeps its quotes");
        assertTrue(page.contains("5L"), "a long constant keeps its suffix");
        assertFalse(page.contains("\u001f"),
                "a control character must be escaped, never emitted raw");
    }

    @Test
    void linksTheSeeAlsoOverloadThatWasActuallyNamed() throws IOException {
        // #clear(int) against a type that declares clear() first. Linking to the
        // wrong overload is worse than not linking: the reader follows it.
        String page = page("Sample.md");
        assertTrue(page.contains("/javadoc/p/Sample.html#clear(int)"),
                "the named overload, not the first member of that name");
    }

    @Test
    void linksQualifiedSeeAlsoMembers() throws IOException {
        assertTrue(page("Sample.md").contains("/javadoc/p/Other.html#greet()"),
                "Type#member() must resolve to the member, not fail a type lookup");
    }

    @Test
    void leavesAStaleSeeAlsoOverloadUnlinked() throws IOException {
        // #missing(int,int,int) matches no overload. Linking it anywhere would
        // hide that the reference is stale.
        String page = page("Sample.md");
        int at = page.indexOf("#missing(int, int, int)");
        assertTrue(at > 0, "the entry is still listed");
        assertTrue(page.indexOf("\"url\": null", at) > 0
                        && page.indexOf("\"url\": null", at) < at + 200,
                "with no link");
    }

    @Test
    void resolvesAnInheritedSeeAlsoMemberToItsDeclaringPage() throws IOException {
        // "#CENTER" on Label means Component.CENTER. Roughly half the member
        // references that name something real are inherited like this, so a
        // resolver that searches only the enclosing type leaves them dead.
        String page = page("Child.md");
        assertTrue(page.contains("/javadoc/p/Other.html#ALIGN"),
                "an inherited field links to the page that declares it");
        assertTrue(page.contains("/javadoc/p/Other.html#greet()"),
                "and so does an inherited method");
    }

    @Test
    void matchesAnOverloadByTypeRatherThanByArity() throws IOException {
        // Vector#remove(Object) resolved to remove(int) on arity alone, and
        // Arrays#sort(Object[], int, int) to the byte[] overload. A wrong link is
        // worse than none: the reader follows it.
        String page = page("Sample.md");
        assertTrue(page.contains("/javadoc/p/Sample.html#erase(java.lang.Object)"),
                "the Object overload, not the int one");
        assertFalse(page.contains("\"url\": \"/javadoc/p/Sample.html#erase(int)\""),
                "must not link the reference to the wrong overload");
    }

    @Test
    void listsFieldsInheritedFromADocumentedSupertype() throws IOException {
        // Label showed none of Component's constants -- CENTER, TOP, the cursor
        // values -- while the standard pages list them.
        String page = page("Child.md");
        assertTrue(page.contains("\"inheritedFields\""), "the block exists");
        assertTrue(page.contains("ALIGN"), "and carries the inherited constant");
    }

    @Test
    void resolvesASimpleTypeNameInItsOwnPackageFirst() throws IOException {
        // "List" is both com.codename1.ui.List and java.util.List, and
        // java.util.AbstractList referring to "List#size" was sent to the UI
        // widget. The link resolved, so no link check could see it -- only
        // reading the page showed it was the wrong class.
        String fromP = Files.readString(content.resolve("p/UsesShared.md"), StandardCharsets.UTF_8);
        assertTrue(fromP.contains("/javadoc/p/Shared.html"), "p sees p.Shared");
        assertFalse(fromP.contains("/javadoc/q/Shared.html"), "and not q.Shared");

        String fromQ = Files.readString(content.resolve("q/UsesShared.md"), StandardCharsets.UTF_8);
        assertTrue(fromQ.contains("/javadoc/q/Shared.html"), "q sees q.Shared");
        assertFalse(fromQ.contains("/javadoc/p/Shared.html"), "and not p.Shared");
    }

    @Test
    void doesNotGiveAnInterfaceObjectsMethods() throws IOException {
        // types.directSupertypes() hands an interface java.lang.Object, which the
        // language does not. SuccessCallback claimed ten inherited Object
        // methods, protected clone() among them.
        String page = page("Listener.md");
        assertFalse(page.contains("\"from\": \"Object\""),
                "an interface inherits nothing from Object");
    }

    @Test
    void inheritsParameterDocumentationByPosition() throws IOException {
        // An override may rename a parameter, and an override with no comment of
        // its own takes the parent's whole documentation. Looking the text up
        // under the child's name then found nothing:
        // GridBagLayout.addLayoutComponent says "constraints" where Layout says
        // "value", and that one parameter printed "Not documented".
        String page = page("Child.md");
        assertTrue(page.contains("what the parent called it"),
                "the inherited text reaches the renamed parameter");
        assertTrue(page.contains("\"renamed\""), "under the override's own name");
    }

    @Test
    void documentsAMethodsOwnTypeParameter() throws IOException {
        // Map.of rejects a null value, and the type-parameter row passed one, so
        // the whole generation died with a NullPointerException the moment any
        // method documented a type parameter. Nothing in the framework does
        // today, which is the only reason this was not already a broken build.
        String page = page("Sample.md");
        assertTrue(page.contains("the value type"), "the type parameter's text is kept");
        assertTrue(page.contains("\"<V>\""), "under its own name");
    }

    @Test
    void writesTheOverviewAndPackagePages() throws IOException {
        assertTrue(Files.exists(content.resolve("_index.md")), "the API overview");
        assertTrue(Files.exists(content.resolve("p/package-summary.md")), "the package summary");
        assertEquals(true, Files.readString(content.resolve("_index.md")).contains("\"/api/\""),
                "the overview keeps the /api/ URL the site has published since 2015");
    }
}
