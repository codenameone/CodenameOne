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
package com.codename1.builders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The pass that raises app-extension targets to the SDK's minimum deployment target.
///
/// Xcode 27 refuses a build outright when ANY target sits below the SDK floor, and the
/// generated VPN tunnel, call directory, share and notification-content extensions are
/// written at 12.0. The global deployment-target pass cannot fix them: it runs before those
/// targets exist and deliberately skips the ones that do, because stomping them down turned
/// a 16.1 WidgetKit extension into a 14.0 one.
///
/// These tests run the emitted ruby rather than matching its text, because what matters is
/// what it does to a project -- and because a ruby fragment that does not parse fails the
/// build with nothing pointing at the cause.
class ExtensionDeploymentFloorScriptTest {

    private static boolean rubyAvailable() {
        try {
            Process p = new ProcessBuilder("ruby", "--version").redirectErrorStream(true).start();
            return p.waitFor() == 0;
        } catch (Exception noRuby) {
            return false;
        }
    }

    /// Runs the emitted fragment against stub targets and returns each target's resulting
    /// deployment target, in the order given.
    private static List<String> applyTo(Path dir, String floor, String... nameTypeTarget)
            throws IOException, InterruptedException {
        StringBuilder harness = new StringBuilder();
        harness.append("Config = Struct.new(:build_settings)\n")
                .append("Target = Struct.new(:name, :product_type, :build_configurations)\n")
                .append("targets = []\n");
        for (String spec : nameTypeTarget) {
            String[] parts = spec.split("\\|", -1);
            String settings;
            if (parts[2].equals("<unset>")) {
                settings = "{}";
            } else if (parts[2].indexOf("->") >= 0) {
                // explicit key=value pairs, so qualified keys can be expressed
                StringBuilder m = new StringBuilder("{");
                for (String pair : parts[2].split(";")) {
                    // "->" because a qualified key contains '=' inside its brackets
                    String[] kv = pair.split("->", 2);
                    if (m.length() > 1) {
                        m.append(", ");
                    }
                    m.append("'").append(kv[0]).append("' => '").append(kv[1]).append("'");
                }
                settings = m.append("}").toString();
            } else {
                settings = "{'IPHONEOS_DEPLOYMENT_TARGET' => '" + parts[2] + "'}";
            }
            harness.append("targets << Target.new('").append(parts[0]).append("', '")
                    .append(parts[1]).append("', [Config.new(").append(settings).append(")])\n");
        }
        harness.append("xcproj = Struct.new(:targets, :save_count) do\n")
                .append("  def save; end\n")
                .append("end.new(targets)\n")
                .append(IPhoneBuilder.extensionDeploymentFloorScript(floor))
                .append("\ntargets.each do |t|\n"
                        + "  bs = t.build_configurations[0].build_settings\n"
                        + "  keys = bs.keys.select { |k| k.start_with?('IPHONEOS_DEPLOYMENT_TARGET') }\n"
                        + "  puts(keys.empty? ? 'nil' : keys.sort.map { |k| \"#{k}=#{bs[k]}\" }"
                        + ".join(',').inspect)\n"
                        + "end\n");

        File script = dir.resolve("harness.rb").toFile();
        Files.write(script.toPath(), harness.toString().getBytes(StandardCharsets.UTF_8));
        Process p = new ProcessBuilder("ruby", script.getAbsolutePath())
                .redirectErrorStream(true).start();
        List<String> out = new ArrayList<String>();
        for (String line : new String(readAll(p), StandardCharsets.UTF_8).split("\n")) {
            line = line.trim();
            if (line.startsWith("\"") || line.equals("nil")) {
                out.add(line.replace("\"", ""));
            }
        }
        assertEquals(0, p.waitFor(), "the emitted ruby must run: " + harness);
        return out;
    }

    private static byte[] readAll(Process p) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = p.getInputStream().read(buf)) > 0) {
            bos.write(buf, 0, len);
        }
        return bos.toByteArray();
    }

    private static final String EXT = "com.apple.product-type.app-extension";
    private static final String APP = "com.apple.product-type.application";

    /// The bug: extensions generated at 12.0 while the host was raised to 15.0, which Xcode 27
    /// refuses. And the property that must survive the fix: an extension ABOVE the floor keeps
    /// its own higher target.
    @Test
    void raisesExtensionsBelowTheFloorAndLeavesHigherOnesAlone(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyTo(dir, "15.0",
                "VpnTunnel|" + EXT + "|12.0",
                "CallDirectory|" + EXT + "|12.0",
                "Widgets|" + EXT + "|16.1",
                "HostApp|" + APP + "|12.0");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(0), "a 12.0 VPN tunnel must be raised");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(1), "a 12.0 call directory must be raised");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=16.1", got.get(2), "a WidgetKit extension must keep its higher target");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=12.0", got.get(3),
                "the app target is not this pass's business; the global pass owns it");
    }

    /// Xcode honours IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*] over the plain key for the
    /// build it matches, and an imported extension archive carries whatever its own project
    /// had. Raising only the base leaves a qualified 12.0 to win on the device archive, and
    /// the build fails exactly as before -- a trap this tree has already been caught by once.
    @Test
    void raisesQualifiedDeploymentTargetsToo(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyTo(dir, "15.0",
                "Imported|" + EXT + "|IPHONEOS_DEPLOYMENT_TARGET->12.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]->12.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphonesimulator*]->14.0",
                "AlreadyHigh|" + EXT + "|IPHONEOS_DEPLOYMENT_TARGET->16.1;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]->16.4");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=15.0,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphonesimulator*]=15.0",
                got.get(0), "every spelling below the floor must be raised");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=16.1,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=16.4",
                got.get(1), "nothing above the floor may be touched");
    }

    /// A qualified key that is simply absent must stay absent: writing one would pin a build
    /// Xcode was resolving from the base value.
    @Test
    void doesNotInventQualifiedKeys(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyTo(dir, "15.0", "Plain|" + EXT + "|12.0");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(0));
    }

    /// An expression resolves against the project, which the global pass already raised.
    /// Rewriting it to a literal would throw away the author's intent for no gain.
    @Test
    void leavesSettingReferencesAlone(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyTo(dir, "15.0", "Inherited|" + EXT + "|$(inherited)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=$(inherited)", got.get(0));
    }

    /// An extension declaring nothing, or something that is not a version, gets the floor
    /// rather than an exception: this fragment runs inside the build and must not be the
    /// thing that breaks it.
    @Test
    void unsetOrUnparseableGetsTheFloor(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyTo(dir, "15.0",
                "Unset|" + EXT + "|<unset>",
                "Junk|" + EXT + "|not-a-version");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(0));
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(1));
    }

    /// Off a Mac there is no SDK to ask, and the build must behave exactly as it did before
    /// any of this existed: nothing emitted, nothing changed.
    @Test
    void noFloorEmitsNothing() {
        assertEquals("", IPhoneBuilder.extensionDeploymentFloorScript(null));
        assertEquals("", IPhoneBuilder.extensionDeploymentFloorScript(""));
        assertEquals("", IPhoneBuilder.extensionDeploymentFloorScript("   "));
    }

    /// A fragment that does not parse fails the build with nothing naming the cause, so the
    /// ruby is syntax-checked on its own as well as executed above.
    @Test
    void theEmittedRubyParses(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        File script = dir.resolve("fragment.rb").toFile();
        Files.write(script.toPath(),
                ("xcproj = nil\n" + IPhoneBuilder.extensionDeploymentFloorScript("15.0"))
                        .getBytes(StandardCharsets.UTF_8));
        Process p = new ProcessBuilder("ruby", "-c", script.getAbsolutePath())
                .redirectErrorStream(true).start();
        String out = new String(readAll(p), StandardCharsets.UTF_8);
        assertEquals(0, p.waitFor(), "ruby -c failed: " + out);
        assertTrue(out.contains("Syntax OK"), out);
    }
}
