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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        return applyWithProject(dir, floor, "{}", nameTypeTarget);
    }

    /// @param projectSettings a ruby hash literal for the PROJECT's build settings, which is
    /// what $(inherited) and any other reference resolve against
    private static List<String> applyWithProject(Path dir, String floor,
            String projectSettings, String... nameTypeTarget)
            throws IOException, InterruptedException {
        StringBuilder harness = new StringBuilder();
        harness.append("Config = Struct.new(:name, :build_settings)\n")
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
                    .append(parts[1]).append("', [Config.new('Release', ")
                    .append(settings).append(")])\n");
        }
        harness.append("project_configs = [Config.new('Release', ")
                .append(projectSettings).append(")]\n")
                .append("xcproj = Struct.new(:targets, :build_configurations) do\n")
                .append("  def save; end\n")
                .append("end.new(targets, project_configs)\n")
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

    /// $(inherited) picks up the PROJECT's target, which the global pass already raised, so
    /// the author's expression is worth keeping. Rewriting it to a literal would throw away
    /// their intent for no gain.
    @Test
    void keepsAnInheritedReferenceThatAlreadyClearsTheFloor(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Inherited|" + EXT + "|$(inherited)",
                // Control: without it this passes when the pass does nothing at all.
                "MustRise|" + EXT + "|12.0");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=$(inherited)", got.get(0));
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(1), "control: the pass ran");
    }

    /// The finding behind resolving rather than skipping: an imported archive writes
    /// $(EXTENSION_MIN), that setting is 12.0, and skipping every reference let it through
    /// while the archive still failed. A reference is judged by what it resolves to.
    @Test
    void raisesAReferenceThatResolvesBelowTheFloor(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Imported|" + EXT + "|EXTENSION_MIN->12.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(0),
                "a reference resolving to 12.0 is below the floor however it is spelled");
    }

    /// ...and one that resolves ABOVE the floor keeps its expression, so this does not
    /// flatten every archive's settings into literals.
    @Test
    void keepsAReferenceThatResolvesAboveTheFloor(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Imported|" + EXT + "|EXTENSION_MIN->16.4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(EXTENSION_MIN)",
                // Control: without it this passes when the pass does nothing at all.
                "MustRise|" + EXT + "|12.0");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=$(EXTENSION_MIN)", got.get(0));
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(1), "control: the pass ran");
    }

    /// Xcode accepts modifiers on a reference, and missing that spelling is worse than
    /// missing the reference entirely: the expression survives unresolved, fails to parse as
    /// a version, and is clamped to the floor -- LOWERING an extension that asked for 16.4 to
    /// 15.0 and letting it run on an OS its code does not support.
    @Test
    void resolvesModifiersRatherThanLoweringTheExtension(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Higher|" + EXT + "|EXTENSION_MIN->16.4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(EXTENSION_MIN:lower)",
                "Lower|" + EXT + "|EXTENSION_MIN->12.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(EXTENSION_MIN:lower)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=$(EXTENSION_MIN:lower)", got.get(0),
                "16.4 through a modifier still clears the floor and must not be lowered");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(1),
                "12.0 through a modifier is still below the floor");
    }

    /// A setting's value may name another setting, so resolution has to reach a fixed point
    /// rather than make one pass. Nesting resolves; a cycle never settles and must fall out
    /// at the cap rather than spin, which is what the cap is for.
    @Test
    void resolvesNestingAndSurvivesACycle(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Nested|" + EXT + "|OUTER->$(INNER);INNER->16.4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(OUTER)",
                "Cyclic|" + EXT + "|A->$(B);B->$(A);IPHONEOS_DEPLOYMENT_TARGET->$(A)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=$(OUTER)", got.get(0),
                "a nested reference resolving to 16.4 clears the floor and is kept");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(1),
                "a cycle resolves to nothing usable, so the floor is the answer");
    }

    /// A helper may be qualified too. Xcode reads EXTENSION_MIN[sdk=iphoneos*] for a device
    /// build, so taking the base 12.0 would resolve a 16.4 extension down to the floor and
    /// weaken it below what its code needs.
    @Test
    void resolvesAHelperInTheQualifiedKeysContext(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Qualified|" + EXT + "|EXTENSION_MIN->12.0;"
                        + "EXTENSION_MIN[sdk=iphoneos*]->16.4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]->$(EXTENSION_MIN)");
        // The base key is unset on this fixture, so it takes the floor -- a simulator build
        // would otherwise declare no minimum at all. The qualified key is the one under test.
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=$(EXTENSION_MIN)",
                got.get(0),
                "the device build resolves EXTENSION_MIN to 16.4, which clears the floor");
    }

    /// A helper qualified for a DIFFERENT build cannot win this one, so it is not
    /// uncertainty: [sdk=iphonesimulator*] never applies to an [sdk=iphoneos*] key, the base
    /// 12.0 is what Xcode uses, and raising it is correct. An earlier, coarser version of
    /// this pass treated any sibling as ambiguity and left the target standing.
    @Test
    void aSiblingForAnotherBuildDoesNotBlockTheRaise(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Mismatched|" + EXT + "|EXTENSION_MIN->12.0;"
                        + "EXTENSION_MIN[sdk=iphonesimulator*]->16.4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=15.0",
                got.get(0),
                "the device key resolves to the base 12.0; the simulator helper is irrelevant");
    }

    /// The base key IS uncertain: no condition is known, so a qualified helper really can win
    /// on the real build. A guess may raise but never lower, so the expression stands.
    @Test
    void anUnqualifiedKeyWithAQualifiedHelperIsUncertain(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "BaseKey|" + EXT + "|EXTENSION_MIN->12.0;"
                        + "EXTENSION_MIN[sdk=iphoneos*]->16.4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(EXTENSION_MIN)",
                // Control: without it this passes when the pass does nothing at all.
                "MustRise|" + EXT + "|12.0");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=$(EXTENSION_MIN)", got.get(0),
                "EXTENSION_MIN is 16.4 on a device build, so this must not be clamped to 15.0");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(1), "control: the pass ran");
    }

    /// Two equally specific applicable helpers are a genuine tie, and a tie is a guess.
    @Test
    void twoEquallySpecificHelpersAreATie(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Tied|" + EXT + "|EXTENSION_MIN[sdk=iphoneos*]->12.0;"
                        + "EXTENSION_MIN[arch=arm64]->16.4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*][arch=arm64]->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*][arch=arm64]=$(EXTENSION_MIN)",
                got.get(0),
                "which of the two one-condition helpers wins is not decidable here");
    }

    /// A tie only matters if one of the answers would clear the floor. When every tied
    /// candidate is below it, whichever Xcode picks is below it too -- so raising is safe,
    /// and refusing would leave an extension Xcode 27 rejects, which is the opposite of
    /// protecting it.
    @Test
    void clampsATieWhereEveryCandidateIsBelowTheFloor(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "AllLow|" + EXT + "|EXTENSION_MIN[sdk=iphoneos*]->12.0;"
                        + "EXTENSION_MIN[arch=arm64]->13.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*][arch=arm64]->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*][arch=arm64]=15.0",
                got.get(0),
                "12.0 and 13.0 are both below 15.0, so the tie changes nothing");
    }

    /// The same shape at the BASE key: no condition is known, but if neither the base nor any
    /// qualified sibling clears the floor there is nothing to protect.
    @Test
    void clampsAnUnqualifiedKeyWhenNoSiblingClearsTheFloor(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "AllLowBase|" + EXT + "|EXTENSION_MIN->12.0;"
                        + "EXTENSION_MIN[sdk=iphoneos*]->13.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(0));
    }

    /// A helper qualified on only SOME of the key's conditions still applies, and requiring an
    /// identical suffix found nothing -- leaving a 12.0 extension for Xcode 27 to reject.
    @Test
    void resolvesAHelperQualifiedOnASubsetOfTheConditions(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Subset|" + EXT + "|EXTENSION_MIN[sdk=iphoneos*]->12.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*][arch=arm64]->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*][arch=arm64]=15.0",
                got.get(0),
                "the sdk-qualified helper applies to this build and resolves to 12.0");
    }

    /// The certain case still raises: one unqualified helper, genuinely below the floor.
    @Test
    void stillRaisesWhenTheResolutionIsCertain(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Plain|" + EXT + "|EXTENSION_MIN->12.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=15.0", got.get(0));
    }

    /// An exact qualified hit is not a guess. With EXTENSION_MIN qualified for BOTH sdks, the
    /// simulator key resolves exactly to 12.0 and must be raised -- treating the device
    /// sibling as uncertainty left it below the floor and the build failed on it anyway,
    /// which is the opposite of what the uncertainty guard is for.
    @Test
    void anExactQualifiedHitIsNotUncertain(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "BothSdks|" + EXT + "|EXTENSION_MIN[sdk=iphoneos*]->16.4;"
                        + "EXTENSION_MIN[sdk=iphonesimulator*]->12.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphonesimulator*]->$(EXTENSION_MIN);"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=$(EXTENSION_MIN),"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphonesimulator*]=15.0",
                got.get(0),
                "the device key resolves to 16.4 and is kept; the simulator key resolves to "
                        + "12.0 and is raised");
    }

    /// Xcode expands a reference nothing defines to the empty string, so the extension would
    /// declare no minimum at all. The floor is the answer there, not the expression.
    @Test
    void raisesAReferenceNothingDefines(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyTo(dir, "15.0",
                "Dangling|" + EXT + "|$(NOTHING_DEFINES_THIS)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(0));
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

    /// Watch, tv and macOS extensions are app-extension targets too, and an iOS floor means
    /// nothing on them. The generated CN1WatchWidgets is SDKROOT=watchos with its own
    /// WATCHOS_DEPLOYMENT_TARGET; writing IPHONEOS_DEPLOYMENT_TARGET onto it is a stray
    /// setting. Found by running the real pass against a real generated project -- the
    /// earlier stub fixtures had no non-iOS extension in them to notice it.
    @Test
    void leavesNonIosExtensionsAlone(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyTo(dir, "15.0",
                "WatchWidgets|" + EXT + "|SDKROOT->watchos;"
                        + "SUPPORTED_PLATFORMS->watchos watchsimulator;"
                        + "WATCHOS_DEPLOYMENT_TARGET->10.0",
                "MacProvider|" + EXT + "|SDKROOT->macosx;MACOSX_DEPLOYMENT_TARGET->13.0",
                "PhoneWidgets|" + EXT + "|SDKROOT->iphoneos;IPHONEOS_DEPLOYMENT_TARGET->12.0");
        assertEquals("nil", got.get(0), "a watchOS extension must not gain an iOS floor");
        assertEquals("nil", got.get(1), "nor a macOS one");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(2),
                "the iOS extension beside them is still raised");
    }

    /// Xcode does not care in which order a key spells its conditions, so a helper declared
    /// [arch=arm64][sdk=iphoneos*] is the same helper as [sdk=iphoneos*][arch=arm64]. Matching
    /// the text missed it, resolved to nothing, and clamped a 16.4 extension to the floor.
    @Test
    void matchesConditionsRegardlessOfOrder(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Reordered|" + EXT + "|SDKROOT->iphoneos;"
                        + "EXTENSION_MIN[arch=arm64][sdk=iphoneos*]->16.4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*][arch=arm64]->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*][arch=arm64]=$(EXTENSION_MIN)",
                got.get(0),
                "the helper is the same one however its conditions are ordered, and 16.4 "
                        + "clears the floor");
    }

    /// IPHONEOS_DEPLOYMENT_TARGET governs simulator builds too, and Xcode 27's simulator
    /// floor is the same 15.0. Recognising only "iphoneos" skipped a simulator-only
    /// configuration and left it below the floor.
    @Test
    void governsSimulatorOnlyConfigurations(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyTo(dir, "15.0",
                "SimOnly|" + EXT + "|SDKROOT->iphonesimulator;IPHONEOS_DEPLOYMENT_TARGET->12.0",
                "SimPlatforms|" + EXT + "|SUPPORTED_PLATFORMS->iphonesimulator;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->12.0",
                "WatchStillSkipped|" + EXT + "|SDKROOT->watchos;WATCHOS_DEPLOYMENT_TARGET->10.0");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(0), "SDKROOT=iphonesimulator");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(1), "SUPPORTED_PLATFORMS sim");
        assertEquals("nil", got.get(2), "watchOS is still none of this floor's business");
    }

    /// $(inherited) picks up the PROJECT's value for the same setting, and the project can
    /// qualify it too. A base of 16.4 beside [sdk=iphoneos*] = 12.0 means a device archive
    /// inherits 12.0 -- reading only the unqualified project value called that "already above
    /// the floor" and left an extension Xcode 27 rejects.
    @Test
    void resolvesInheritedAgainstTheProjectsQualifiedValue(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '16.4', "
                        + "'IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]' => '12.0'}",
                "Inherits|" + EXT + "|SDKROOT->iphoneos;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]->$(inherited)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=15.0",
                got.get(0),
                "the device build inherits the project's 12.0, not its 16.4 base");
    }

    /// And the other way round: a project qualified HIGHER than its base means the inherited
    /// value clears the floor, so replacing the expression would weaken the extension.
    @Test
    void keepsInheritedWhenTheProjectsQualifiedValueClearsTheFloor(@TempDir Path dir)
            throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '12.0', "
                        + "'IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]' => '16.4'}",
                "Inherits|" + EXT + "|SDKROOT->iphoneos;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]->$(inherited)",
                // Control: without it this passes when the pass does nothing at all.
                "MustRise|" + EXT + "|SDKROOT->iphoneos;IPHONEOS_DEPLOYMENT_TARGET->12.0");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=$(inherited)",
                got.get(0),
                "the device build inherits 16.4, which clears the floor");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(1), "control: the pass ran");
    }

    /// SDKROOT can be written qualified. Reading only the unqualified key saw nothing,
    /// called the platform unknown, and wrote an iOS floor onto a watch extension -- the same
    /// stray setting the platform guard exists to stop, reached through a qualifier.
    @Test
    void readsEverySdkrootDeclarationNotJustTheUnqualifiedOne(@TempDir Path dir)
            throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyTo(dir, "15.0",
                "WatchQualified|" + EXT + "|SDKROOT[arch=arm64_32]->watchos;"
                        + "WATCHOS_DEPLOYMENT_TARGET->10.0",
                "PhoneQualified|" + EXT + "|SDKROOT[sdk=iphoneos*]->iphoneos;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->12.0");
        assertEquals("nil", got.get(0),
                "a watch extension declaring SDKROOT only through a qualifier is still a "
                        + "watch extension");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(1),
                "and an iOS one declared the same way is still raised");
    }

    /// Conditions can OVERLAP without being equal: sdk=iphoneos27.* is narrower than
    /// sdk=iphoneos* and Xcode picks it for a matching build. Set logic on the raw text calls
    /// the two unrelated, so the helper looked absent, resolved to nothing, and clamped a
    /// 16.4 extension to the floor.
    @Test
    void treatsOverlappingWildcardConditionsAsUndecidable(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Narrower|" + EXT + "|SDKROOT->iphoneos;"
                        + "EXTENSION_MIN[sdk=iphoneos27.*]->16.4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]->$(EXTENSION_MIN)",
                // Control: without it this passes when the pass does nothing at all.
                "MustRise|" + EXT + "|SDKROOT->iphoneos;IPHONEOS_DEPLOYMENT_TARGET->12.0");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=$(EXTENSION_MIN)",
                got.get(0),
                "iphoneos27.* may win on a 27 build, so this must not be clamped to 15.0");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(1), "control: the pass ran");
    }

    /// ...but patterns that cannot both match stay disjoint, so the raise still happens.
    /// iphoneos* and iphonesimulator* never match the same SDK.
    @Test
    void disjointWildcardsDoNotBlockTheRaise(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Disjoint|" + EXT + "|SDKROOT->iphoneos;EXTENSION_MIN->12.0;"
                        + "EXTENSION_MIN[sdk=iphonesimulator*]->16.4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0,"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=15.0",
                got.get(0),
                "a simulator-qualified helper cannot win a device build");
    }

    /// $(inherited) means the inherited value of the setting it appears IN. With
    /// EXTENSION_MIN = $(inherited) and IPHONEOS_DEPLOYMENT_TARGET = $(EXTENSION_MIN), the
    /// nested one inherits EXTENSION_MIN's project value, not the deployment target's.
    @Test
    void nestedInheritedKeepsItsOwnSettingName(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0', 'EXTENSION_MIN' => '12.0'}",
                "Nested|" + EXT + "|SDKROOT->iphoneos;EXTENSION_MIN->$(inherited);"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(0),
                "it resolves to the project's EXTENSION_MIN of 12.0, which is below the floor");
    }

    /// The generated project really does carry SDKROOT = iphoneos at PROJECT level, so a
    /// watch extension's own watchos must outrank it. Pooling the two put the iOS floor back
    /// onto CN1WatchWidgets in the real project while every fixture here still passed --
    /// because the fixtures declared no project SDKROOT at all.
    @Test
    void theTargetsOwnPlatformOutranksTheProjects(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'SDKROOT' => 'iphoneos', 'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "WatchInIosProject|" + EXT + "|SDKROOT->watchos;"
                        + "WATCHOS_DEPLOYMENT_TARGET->10.0",
                // Declares no platform of its own, so it inherits the project's iOS one.
                "InheritsProjectPlatform|" + EXT + "|IPHONEOS_DEPLOYMENT_TARGET->12.0");
        assertEquals("nil", got.get(0),
                "the watch extension's own SDKROOT wins over the iOS project's");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=15.0", got.get(1),
                "a target naming no platform still takes the project's, and is raised");
    }

    /// A mixed conditional is not wholly a guess. When the base key resolves through a
    /// helper whose branches disagree, the LOW branch is decided even though the whole
    /// expression is not -- and skipping all of them left the device archive at 12.0 for
    /// Xcode 27 to reject. The floor is pinned for exactly the branch proven below it.
    @Test
    void pinsTheFloorForBranchesProvenBelowIt(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyTo(dir, "15.0",
                "Mixed|" + EXT + "|SDKROOT->iphoneos;"
                        + "EXTENSION_MIN[sdk=iphoneos*]->12.0;"
                        + "EXTENSION_MIN[sdk=iphonesimulator*]->16.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=$(EXTENSION_MIN),"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=15.0",
                got.get(0),
                "the device branch is pinned; the base expression still serves the simulator "
                        + "branch, which resolves to 16.0");
    }

    /// ...and no key is invented when every branch already clears the floor.
    @Test
    void pinsNothingWhenEveryBranchClearsTheFloor(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyTo(dir, "15.0",
                "AllHigh|" + EXT + "|SDKROOT->iphoneos;"
                        + "EXTENSION_MIN[sdk=iphoneos*]->16.4;"
                        + "EXTENSION_MIN[sdk=iphonesimulator*]->16.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=$(EXTENSION_MIN)", got.get(0),
                "nothing is below the floor, so nothing is written");
    }

    /// An author's own qualified value is never overwritten by the pin.
    @Test
    void doesNotOverwriteAnExistingBranch(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyTo(dir, "15.0",
                "HasBranch|" + EXT + "|SDKROOT->iphoneos;"
                        + "EXTENSION_MIN[sdk=iphoneos*]->12.0;"
                        + "EXTENSION_MIN[sdk=iphonesimulator*]->16.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(EXTENSION_MIN);"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]->16.4");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=$(EXTENSION_MIN),"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=16.4",
                got.get(0),
                "the author already decided that branch, and 16.4 clears the floor");
    }

    /// A branch's own value may itself be a reference. Skipping it for containing a $ left
    /// the device branch unpinned and the archive at 12.0, so each candidate is resolved in
    /// ITS OWN condition context before the branch is judged.
    @Test
    void resolvesANestedBranchBeforePinningIt(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyTo(dir, "15.0",
                "Nested|" + EXT + "|SDKROOT->iphoneos;DEVICE_MIN->12.0;"
                        + "EXTENSION_MIN[sdk=iphoneos*]->$(DEVICE_MIN);"
                        + "EXTENSION_MIN[sdk=iphonesimulator*]->16.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=$(EXTENSION_MIN),"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=15.0",
                got.get(0),
                "the device branch resolves through DEVICE_MIN to 12.0 and is pinned");
    }

    /// ...and a nested branch that resolves ABOVE the floor is still left alone.
    @Test
    void leavesANestedBranchThatClearsTheFloor(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyTo(dir, "15.0",
                "NestedHigh|" + EXT + "|SDKROOT->iphoneos;DEVICE_MIN->16.4;"
                        + "EXTENSION_MIN[sdk=iphoneos*]->$(DEVICE_MIN);"
                        + "EXTENSION_MIN[sdk=iphonesimulator*]->16.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(EXTENSION_MIN)");
        assertEquals("IPHONEOS_DEPLOYMENT_TARGET=$(EXTENSION_MIN)", got.get(0),
                "both branches clear the floor, so nothing is written");
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
        // An empty fragment also parses, so the emptiness is ruled out first -- otherwise
        // this passes when the pass has been disabled entirely.
        String fragment = IPhoneBuilder.extensionDeploymentFloorScript("15.0");
        assertTrue(fragment.contains("IPHONEOS_DEPLOYMENT_TARGET"),
                "there is supposed to be a pass here, got: " + fragment);
        Process p = new ProcessBuilder("ruby", "-c", script.getAbsolutePath())
                .redirectErrorStream(true).start();
        String out = new String(readAll(p), StandardCharsets.UTF_8);
        assertEquals(0, p.waitFor(), "ruby -c failed: " + out);
        assertTrue(out.contains("Syntax OK"), out);
    }

    /// A deployment target COMPOSED from several helpers has no per-helper answer. Judging
    /// $(VERSION_MAJOR).$(VERSION_MINOR) one reference at a time reads the minor as '4',
    /// which is below every floor, and pinning on it would write a device key of 15.0 over
    /// an extension whose real minimum is 16.4 -- lowering it, which is worse than leaving
    /// the pass out entirely. The whole expression has to be resolved in the branch's
    /// context before the branch is judged.
    @Test
    void aComposedExpressionIsJudgedWhole(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Composed|" + EXT + "|VERSION_MAJOR[sdk=iphoneos*]->16;"
                        + "VERSION_MINOR[sdk=iphoneos*]->4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(VERSION_MAJOR).$(VERSION_MINOR)");
        // The assertion is about the OUTCOME for a device build, not about one key. Writing
        // a qualified 15.0 lowers it; so does replacing the base expression with a flat
        // 15.0, which reaches the same device build through a different key.
        assertFalse(got.get(0).contains("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=15.0"),
                "the device branch resolves to 16.4 and clears the floor, so pinning it "
                        + "would LOWER the extension's minimum: " + got.get(0));
        assertTrue(got.get(0).contains("IPHONEOS_DEPLOYMENT_TARGET=$(VERSION_MAJOR).$(VERSION_MINOR)"),
                "the base expression must survive: overwriting it with the floor lowers the "
                        + "same device build to 15.0: " + got.get(0));
    }

    /// The other half of the same rule: when the COMPLETE composed value really is below the
    /// floor, the branch still gets pinned. Without this the previous test could be passed by
    /// a change that simply stopped pinning composed expressions at all.
    @Test
    void aComposedExpressionBelowTheFloorIsStillPinned(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "ComposedLow|" + EXT + "|VERSION_MAJOR[sdk=iphoneos*]->12;"
                        + "VERSION_MINOR[sdk=iphoneos*]->1;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(VERSION_MAJOR).$(VERSION_MINOR)");
        // Asserted as an OUTCOME, not a mechanism. Here BOTH builds are below the floor --
        // the device resolves to 12.1 and the simulator to nothing, since the helpers are
        // device-qualified -- so raising the base key covers them both and no qualified key
        // is needed. What must not survive is the expression itself, which would leave the
        // device archive at 12.1 for Xcode to reject.
        assertFalse(got.get(0).contains("IPHONEOS_DEPLOYMENT_TARGET=$(VERSION_MAJOR).$(VERSION_MINOR)"),
                "12.1 is below the floor, so the expression must not survive: " + got.get(0));
        assertTrue(got.get(0).contains("=15.0"),
                "the floor has to be written somewhere the device build reads it: "
                        + got.get(0));
    }

    /// Xcode resolves build settings per LEVEL: a TARGET declaration overrides a PROJECT one,
    /// and conditions rank only WITHIN a level. Pooling the two ranked the project's
    /// conditional above the target's unconditional purely because it carried a condition, so
    /// the pass read 12.0 where Xcode reads 16.4 and wrote the floor over a target that did not
    /// need it -- lowering the extension, which is the one outcome this pass must never cause.
    @Test
    void aTargetSettingOverridesAProjectQualifier(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'EXTENSION_MIN[sdk=iphoneos*]' => '12.0'}",
                "TargetWins|" + EXT + "|EXTENSION_MIN->16.4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]->$(EXTENSION_MIN)");
        assertTrue(got.get(0).contains("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=$(EXTENSION_MIN)"),
                "the target's own EXTENSION_MIN is 16.4 and clears the floor, so the project's "
                        + "qualified 12.0 must not decide this: " + got.get(0));
    }

    /// Branch discovery has to follow helpers TRANSITIVELY. Here the outer helper carries no
    /// conditions at all and the branches hang off the inner one, so scanning only the names
    /// written in the expression produced no candidate context and left the device target at
    /// 12.0 for the new SDK to reject.
    @Test
    void branchDiscoveryFollowsHelpersTransitively(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Transitive|" + EXT + "|EXTENSION_MIN->$(DEVICE_MIN);"
                        + "DEVICE_MIN[sdk=iphoneos*]->12.0;"
                        + "DEVICE_MIN[sdk=iphonesimulator*]->16.4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(EXTENSION_MIN)");
        assertTrue(got.get(0).contains("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=15.0"),
                "the device branch resolves through EXTENSION_MIN to DEVICE_MIN's 12.0 and must "
                        + "be pinned: " + got.get(0));
        assertTrue(got.get(0).contains("IPHONEOS_DEPLOYMENT_TARGET=$(EXTENSION_MIN)"),
                "the simulator branch resolves to 16.4, so the base expression still serves it: "
                        + got.get(0));
    }

    /// Xcode conditions settings on the build CONFIGURATION as well as the sdk, and this pass
    /// iterates configurations -- so the configuration is known, not guessed. Leaving it out of
    /// the context made both branches of a per-configuration helper look inapplicable, the
    /// answer confidently empty, and wrote the floor over a Release that resolves to 16.4.
    /// The harness builds one configuration named Release, which is the one being written.
    @Test
    void theConfigurationIsPartOfTheResolutionContext(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "PerConfig|" + EXT + "|EXTENSION_MIN[config=Debug]->12.0;"
                        + "EXTENSION_MIN[config=Release]->16.4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]->$(EXTENSION_MIN)");
        assertTrue(got.get(0).contains("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=$(EXTENSION_MIN)"),
                "this configuration is Release, where EXTENSION_MIN is 16.4 and clears the "
                        + "floor; the Debug branch must not decide it: " + got.get(0));
    }

    /// The other direction, so the test above cannot be satisfied by a change that simply stops
    /// resolving per-configuration helpers: when THIS configuration's value is below the floor,
    /// the raise still happens.
    @Test
    void aConfigurationBelowTheFloorIsStillRaised(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "PerConfigLow|" + EXT + "|EXTENSION_MIN[config=Debug]->16.4;"
                        + "EXTENSION_MIN[config=Release]->12.0;"
                        + "IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]->$(EXTENSION_MIN)");
        assertTrue(got.get(0).contains("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]=15.0"),
                "Release resolves to 12.0 here and must be raised: " + got.get(0));
    }

    /// Xcode compares a key's conditions as a SET, so [arch=arm64][sdk=iphoneos*] and
    /// [sdk=iphoneos*][arch=arm64] are one branch written two ways. Deciding "this branch has
    /// no setting yet" by exact string match missed the author's own 16.4 and appended a second
    /// key for the same build carrying the floor -- which can win, lowering it to 15.0.
    ///
    /// The simulator helper is what makes this reach the branch path at all: without a branch
    /// that CLEARS the floor the whole expression is not uncertain, the base key is simply
    /// raised, and no branch is ever considered. An earlier version of this test omitted it and
    /// passed against the unfixed builder, proving nothing.
    @Test
    void aReorderedConditionSetIsTheSameBranch(@TempDir Path dir) throws Exception {
        assumeTrue(rubyAvailable(), "needs ruby");
        List<String> got = applyWithProject(dir, "15.0",
                "{'IPHONEOS_DEPLOYMENT_TARGET' => '15.0'}",
                "Reordered|" + EXT + "|EXTENSION_MIN[sdk=iphoneos*][arch=arm64]->12.0;"
                        + "EXTENSION_MIN[sdk=iphonesimulator*]->16.4;"
                        + "IPHONEOS_DEPLOYMENT_TARGET->$(EXTENSION_MIN);"
                        + "IPHONEOS_DEPLOYMENT_TARGET[arch=arm64][sdk=iphoneos*]->16.4");
        assertFalse(got.get(0).contains("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*][arch=arm64]=15.0"),
                "that branch already has the author's 16.4, spelled in the other order; adding "
                        + "a competing key for the same build can lower it: " + got.get(0));
        assertTrue(got.get(0).contains("IPHONEOS_DEPLOYMENT_TARGET[arch=arm64][sdk=iphoneos*]=16.4"),
                "the author's own branch must survive untouched: " + got.get(0));
    }
}
