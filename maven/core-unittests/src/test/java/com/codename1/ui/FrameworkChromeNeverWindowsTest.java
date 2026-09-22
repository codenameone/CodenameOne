/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.ui;

import com.codename1.junit.UITestBase;
import com.codename1.ui.plaf.UIManager;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Framework chrome must never become an operating system window, however the application or
 * its theme has set the default.
 *
 * <p>These popups are POSITIONED by the framework -- placed relative to the surface they
 * belong to -- and native window mode documents exactly those margins as ignored. In a
 * window they come out centred and lose the placement that is their whole point.</p>
 *
 * <p>Not hypothetical. The desktop themes set {@code defaultNativeWindowModeBool}, and the
 * Windows port's screenshot suite then captured two of {@code LightweightPickerButtons}'
 * four placements, found the two it did capture byte-identical, and timed out waiting for
 * the rest. {@code ComboBox} and {@code InfiniteProgress} had already opted out by hand, so
 * the hazard was known -- it had simply never been reachable, because nothing defaulted the
 * mode to true.</p>
 *
 * <p><b>Why this reads source.</b> The obvious runtime test does not work and it is worth
 * saying why, because it looked like it did: reaching one of these popups means pressing the
 * control that owns it, which parks the caller, so the popup does not exist while the test
 * can look at the hierarchy. A first version walked the current form and passed with every
 * fix removed -- a probe that cannot fail. {@code AbstractDialog} is a closed interface whose
 * own comment forbids new members, so there is no shared choke point to assert on either.
 * What is left is the invariant itself: a framework class that builds a positioned popup says
 * so at the point it builds it.</p>
 */
public class FrameworkChromeNeverWindowsTest extends UITestBase {

    /**
     * Framework sources that build a popup they position themselves, and must therefore opt
     * out of native window mode.
     *
     * <p>Deliberately a list rather than a scan of the whole tree: a dialog the APPLICATION
     * shows should take the theme default, and {@code Dialog.show}, {@code MasterDetail} and
     * {@code Oauth2} are exactly that. The distinction is whether the framework places it.</p>
     */
    private static final String[] POSITIONED_POPUP_SOURCES = {
        "com/codename1/ui/ComboBox.java",
        "com/codename1/ui/ContextMenu.java",
        "com/codename1/ui/TooltipManager.java",
        "com/codename1/ui/Toolbar.java",
        "com/codename1/ui/spinner/Picker.java",
        "com/codename1/ui/validation/Validator.java",
        "com/codename1/components/FloatingActionButton.java",
        "com/codename1/components/InfiniteProgress.java",
    };

    /** Every construction of a dialog that would take the default if left alone. */
    private static final Pattern CONSTRUCTS =
            Pattern.compile("new\\s+(?:InteractionDialog|Dialog)\\s*\\(");

    /**
     * Comments, which are stripped before anything is counted.
     *
     * <p>Not defensive tidiness: {@code BubbleTransition} shows a dialog in a {@code ///}
     * javadoc EXAMPLE, and counting that reported a source with no dialogs in it at all as
     * an unclassified builder. A check whose findings a reader has to filter by hand stops
     * being read.</p>
     */
    private static final Pattern COMMENTS =
            Pattern.compile("//[^\\n]*|/\\*.*?\\*/", Pattern.DOTALL);

    private static String withoutComments(String src) {
        return COMMENTS.matcher(src).replaceAll("");
    }

    private static final Pattern OPTS_OUT =
            Pattern.compile("setNativeWindowMode\\(\\s*false\\s*\\)");

    /**
     * The setting has to actually be reachable, or every assertion here is vacuous.
     */
    @Test
    public void theThemeConstantReallyTurnsTheDefaultOn() {
        Hashtable props = new Hashtable();
        props.put("@defaultNativeWindowModeBool", "true");
        UIManager.getInstance().addThemeProps(props);
        Dialog.setDefaultNativeWindowMode(true);
        try {
            assertTrue(new Dialog().isNativeWindowMode(),
                    "an ordinary dialog must take the default, or nothing below is a test");
            Dialog opted = new Dialog();
            opted.setNativeWindowMode(false);
            assertFalse(opted.isNativeWindowMode(),
                    "and the per-instance opt-out must outrank it, which is what the framework"
                            + " popups rely on");
        } finally {
            Dialog.setDefaultNativeWindowMode(false);
        }
    }

    /**
     * Every framework source that builds a positioned popup opts out at least as many times
     * as it builds one.
     *
     * <p>Counting rather than merely requiring one occurrence: {@code Toolbar} builds two
     * side menus and {@code Picker} builds both a modal dialog and the lightweight popup, and
     * the bug that started this was the SECOND of a pair being missed.</p>
     */
    @Test
    public void everyFrameworkPositionedPopupOptsOut() throws IOException {
        File root = locateCoreSources();
        if (root == null) {
            // The sources are not beside this checkout; nothing to assert rather than a
            // failure a developer cannot act on.
            return;
        }
        List<String> problems = new ArrayList<String>();
        for (String rel : POSITIONED_POPUP_SOURCES) {
            File f = new File(root, rel);
            if (!f.isFile()) {
                problems.add(rel + ": missing; the list is stale");
                continue;
            }
            String src = withoutComments(
                    new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
            int built = count(CONSTRUCTS, src);
            int optedOut = count(OPTS_OUT, src);
            if (built > optedOut) {
                problems.add(rel + ": builds " + built + " dialog(s) and opts out " + optedOut
                        + " time(s); a framework-positioned popup that takes the theme default"
                        + " comes out centred in a window of its own");
            }
        }
        if (!problems.isEmpty()) {
            fail("framework chrome would become an operating system window:\n  "
                    + String.join("\n  ", problems));
        }
    }

    /**
     * The list above has to stay honest as the framework grows, so this fails when a NEW
     * framework source starts building one of these and is not classified either way.
     */
    @Test
    public void newFrameworkPopupsHaveToBeClassified() throws IOException {
        File root = locateCoreSources();
        if (root == null) {
            return;
        }
        // Sources that build a dialog the APPLICATION owns, which correctly takes the
        // default. Listed so that "not in either list" is a real answer rather than silence.
        Set<String> applicationOwned = new HashSet<String>(Arrays.asList(
                "com/codename1/ui/Dialog.java",
                "com/codename1/ui/AbstractDialog.java",
                "com/codename1/components/InteractionDialog.java",
                "com/codename1/components/MasterDetail.java",
                "com/codename1/io/Oauth2.java",
                // Modal choosers and prompts the USER operates. Each is a thing a desktop
                // would reasonably show in a window of its own, so taking the default is
                // right: a file chooser, a crash report, a signature pad, a share sheet and
                // a country list are dialogs, not chrome placed against a control.
                "com/codename1/impl/CodenameOneImplementation.java",
                "com/codename1/system/DefaultCrashReporter.java",
                "com/codename1/components/SignatureComponent.java",
                "com/codename1/components/ShareButton.java",
                "com/codename1/components/PhoneNumberField.java"));

        // Exempt by MECHANISM rather than by a call at the construction site. Dialog's own
        // usesNativeWindow() already refuses a window for a menu, so MenuBar's popup is
        // covered without saying so -- and listing it as application-owned would be a lie
        // that the next reader would have to disprove.
        Set<String> exemptByMechanism = new HashSet<String>(Arrays.asList(
                "com/codename1/ui/MenuBar.java"));
        applicationOwned.addAll(exemptByMechanism);
        Set<String> positioned = new HashSet<String>(Arrays.asList(POSITIONED_POPUP_SOURCES));

        List<String> unclassified = new ArrayList<String>();
        collectDialogBuilders(root, root, positioned, applicationOwned, unclassified);
        if (!unclassified.isEmpty()) {
            fail("these framework sources build a Dialog or InteractionDialog and are in"
                    + " neither list; decide whether the framework positions it (opt out) or"
                    + " the application owns it (take the default):\n  "
                    + String.join("\n  ", unclassified));
        }
    }

    private void collectDialogBuilders(File root, File dir, Set<String> positioned,
                                       Set<String> applicationOwned, List<String> out)
            throws IOException {
        File[] entries = dir.listFiles();
        if (entries == null) {
            return;
        }
        for (File f : entries) {
            if (f.isDirectory()) {
                collectDialogBuilders(root, f, positioned, applicationOwned, out);
                continue;
            }
            if (!f.getName().endsWith(".java")) {
                continue;
            }
            String rel = root.toPath().relativize(f.toPath()).toString().replace('\\', '/');
            if (positioned.contains(rel) || applicationOwned.contains(rel)) {
                continue;
            }
            String src = withoutComments(
                    new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
            if (count(CONSTRUCTS, src) > 0) {
                out.add(rel);
            }
        }
    }

    private static int count(Pattern p, String src) {
        Matcher m = p.matcher(src);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    /** Walks up for CodenameOne/src, the way the native-theme tests locate Themes/. */
    private static File locateCoreSources() {
        File cwd = new File(".").getAbsoluteFile();
        for (int i = 0; i < 6 && cwd != null; i++) {
            File candidate = new File(cwd, "CodenameOne/src");
            if (candidate.isDirectory()) {
                return candidate;
            }
            cwd = cwd.getParentFile();
        }
        return null;
    }
}
