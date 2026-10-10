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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Holds the native Windows and Linux ports to what a `native:` font name
/// means: `native:MainBold` is bold, `native:ItalicRegular` is italic, and
/// both survive a `derive` to another size with a plain weight.
///
/// The style of such a font is in its name, and the portable request is
/// `Font.createTrueTypeFont(name, name).derive(pixels, Font.STYLE_PLAIN)`.
/// Each port lost it in a different half of that: Linux loaded every
/// `native:` name as regular "Sans", and Windows read the name on load and
/// then built the derived font from the weight alone. Text asked for in bold
/// or italic was drawn in the regular face on both, with nothing failing.
///
/// Neither file is compiled by any job off its own platform, so the function
/// that reads the name is kept free of platform types in both ports, and is
/// compiled alone and run here; where the style then goes is read from the
/// source.
class NativeFontStyleContractTest {
    private static final Path ROOT = Paths.get("..", "..").normalize().toAbsolutePath();
    private static final String LINUX = "Ports/LinuxPort/nativeSources/cn1_linux_text.c";
    private static final String WINDOWS = "Ports/WindowsPort/nativeSources/cn1_windows_text.c";

    private static final int BOLD = 1;
    private static final int ITALIC = 2;

    /// The ten names of the scheme, a name outside it, and the style each
    /// asks for. Thin and Light have no bit in `Font.STYLE_*` and are plain.
    private static final String[] NAMES = {
        "native:MainThin", "native:MainLight", "native:MainRegular", "native:MainBold", "native:MainBlack",
        "native:ItalicThin", "native:ItalicLight", "native:ItalicRegular", "native:ItalicBold",
        "native:ItalicBlack", "Bold Italic"
    };
    private static final int[] STYLES = {
        0, 0, 0, BOLD, BOLD, ITALIC, ITALIC, ITALIC, BOLD | ITALIC, BOLD | ITALIC, 0
    };

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(ROOT.resolve(path)), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    /// The whole definition that starts with `signature`, to the brace that
    /// closes it in the first column.
    private static String function(String source, String path, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, path + " no longer defines `" + signature + "`");
        int end = source.indexOf("\n}\n", start);
        assertTrue(end > start, path + ": the end of `" + signature + "`");
        return source.substring(start, end + 3);
    }

    private static String output(Process p) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = p.getInputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    void theStyleOfANativeFontIsReadFromItsName() throws Exception {
        String linux = function(read(LINUX), LINUX, "static int cn1LinuxNativeFontStyle(const char* name) {");
        String windows = function(read(WINDOWS), WINDOWS, "static int cn1WinNativeFontStyle(const wchar_t* name) {");

        StringBuilder c = new StringBuilder();
        c.append("#include <stdio.h>\n#include <string.h>\n#include <wchar.h>\n");
        c.append("#define CN1_STYLE_BOLD 1\n#define CN1_STYLE_ITALIC 2\n");
        c.append(linux).append(windows);
        c.append("int main(void) {\n");
        for (String name : NAMES) {
            c.append("    printf(\"%d %d\\n\", cn1LinuxNativeFontStyle(\"").append(name)
                    .append("\"), cn1WinNativeFontStyle(L\"").append(name).append("\"));\n");
        }
        c.append("    printf(\"%d %d\\n\", cn1LinuxNativeFontStyle(0), cn1WinNativeFontStyle(0));\n");
        c.append("    return 0;\n}\n");

        Path work = Files.createTempDirectory("cn1-native-font-style");
        Path source = work.resolve("style.c");
        Files.write(source, c.toString().getBytes(StandardCharsets.UTF_8));
        Path binary = work.resolve("style");
        String cc = System.getenv("CN1_CC") == null ? "cc" : System.getenv("CN1_CC");
        Process compile;
        try {
            compile = new ProcessBuilder(cc, "-Wall", "-Werror", "-o", binary.toString(), source.toString())
                    .redirectErrorStream(true).start();
        } catch (IOException noCompiler) {
            Assumptions.assumeTrue(false, "no C compiler named " + cc);
            return;
        }
        String log = output(compile);
        assertEquals(0, compile.waitFor(), "the two functions that read a native: font name must compile"
                + " with nothing but the C library:\n" + log);

        Process run = new ProcessBuilder(binary.toString()).redirectErrorStream(true).start();
        String[] lines = output(run).trim().split("\n");
        assertEquals(0, run.waitFor());
        assertEquals(NAMES.length + 1, lines.length);
        for (int i = 0; i < NAMES.length; i++) {
            assertEquals(STYLES[i] + " " + STYLES[i], lines[i].trim(),
                    "the Font.STYLE_* bits of " + NAMES[i] + " on Linux and on Windows");
        }
        assertEquals("0 0", lines[NAMES.length].trim(), "a null name asks for nothing");
    }

    @Test
    void linuxLoadsANativeFontInTheStyleOfItsName() throws Exception {
        String source = read(LINUX);
        String make = function(source, LINUX, "static CN1Font* cn1LinuxNativeSchemeFont(const char* name) {");
        assertTrue(make.contains("cn1LinuxNativeFontStyle(name)"), LINUX + ": the font of a native: name"
                + " must take its style from the name");
        assertTrue(Pattern.compile("CN1_STYLE_BOLD\\)\\s*!=\\s*0\\s*\\?\\s*PANGO_WEIGHT_BOLD").matcher(make).find(),
                LINUX + ": a bold name must be made with PANGO_WEIGHT_BOLD");
        assertTrue(make.contains("(style & CN1_STYLE_ITALIC) != 0"), LINUX + ": an italic name must be made italic");

        // Both loads: by name, and from bytes when there are none.
        for (String loader : new String[] {
            "JAVA_LONG com_codename1_impl_linux_LinuxNative_loadTrueTypeFont___java_lang_String_java_lang_String_R_long(",
            "JAVA_LONG com_codename1_impl_linux_LinuxNative_loadTrueTypeFontFromMemory___java_lang_String_byte_1ARRAY_R_long("
        }) {
            String body = function(source, LINUX, loader);
            Matcher m = Pattern.compile("strncmp\\(name, \"native:\", 7\\) == 0\\) \\{\\s*([^}]*)\\}").matcher(body);
            assertTrue(m.find(), LINUX + ": " + loader + " no longer tests for the native: scheme");
            assertTrue(m.group(1).contains("cn1LinuxNativeSchemeFont(name)"), LINUX + ": " + loader
                    + " maps a native: name to a family and drops the style in the name: " + m.group(1).trim());
        }

        // The style has to outlive derive(pixels, STYLE_PLAIN).
        String derive = function(source, LINUX,
                "JAVA_LONG com_codename1_impl_linux_LinuxNative_deriveTrueTypeFont___long_float_int_R_long(");
        assertTrue(derive.contains("pango_font_description_copy(src->desc)"), LINUX
                + ": a derived font must start from the base font's description, or it loses the base's style");
        assertFalse(derive.contains("PANGO_WEIGHT_NORMAL") || derive.contains("PANGO_STYLE_NORMAL"), LINUX
                + ": a plain weight passed to derive must not reset the base font's style");
    }

    @Test
    void windowsKeepsTheStyleOfANativeFontWhenItIsDerived() throws Exception {
        String source = read(WINDOWS);
        String system = function(source, WINDOWS, "static CN1Font* cn1WinSystemFontForName(");
        assertTrue(system.contains("style = cn1WinNativeFontStyle(name)"), WINDOWS
                + ": the font of a native: name must take its style from the name");
        assertTrue(system.contains("cn1WinMakeFont(family, 15.0f * dpi, 0, style)"), WINDOWS
                + ": and must be made in that style");

        String derive = function(source, WINDOWS,
                "JAVA_LONG com_codename1_impl_windows_WindowsNative_deriveTrueTypeFont___long_float_int_R_long(");
        Matcher made = Pattern.compile("cn1WinMakeFont\\(family, px, 0, (\\w+)\\)").matcher(derive);
        assertTrue(made.find(), WINDOWS + ": deriveTrueTypeFont no longer makes its font with cn1WinMakeFont");
        String style = made.group(1);
        assertTrue(Pattern.compile("int " + style + " = \\(base != NULL \\? base->style : 0\\) \\| __cn1Arg3;")
                .matcher(derive).find(), WINDOWS + ": a derived font is made in the style `" + style
                + "`, which must be the base font's style with the requested bits added; made from the"
                + " requested weight alone, native:MainBold derived with STYLE_PLAIN is regular");
    }
}
