package com.codenameone.playground;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;

/// Headless layout smoke test.
///
/// Boots the Playground lifecycle against the JavaSE port, lets the EDT tick
/// once, then walks the shown Form's component tree looking for chrome
/// elements by UIID. Each found element is expected to be visible, attached
/// to the form, and have a non-zero measured size.
///
/// Run twice: once with the simulator in "desktop" density and once in
/// "mobile" density so we catch the two shells (top bar + activity bar +
/// split pane vs. top bar + tab strip + tab content + bottom nav). Exits
/// with non-zero status on any failure so CI can fail the job.
///
/// Caveat: the JavaSE simulator cannot exercise the HTML5 Monaco editor
/// (it relies on BrowserComponent which renders as an empty placeholder),
/// so checks are scoped to the native-CN1 chrome.
public final class PlaygroundLayoutHarness {
    private PlaygroundLayoutHarness() {
    }

    public static void main(String[] args) throws Exception {
        Display.init(null);
        int failures = 0;
        failures += runScenario("desktop", false);
        failures += runScenario("mobile", true);
        if (failures > 0) {
            System.err.println("Playground layout harness: " + failures + " failure(s).");
            System.exit(1);
        }
        System.out.println("Playground layout harness: all checks passed.");
        System.exit(0);
    }

    private static int runScenario(String label, boolean mobile) throws Exception {
        // Display.isDesktop / isTablet ignore external system properties, so we
        // force the layout via the test-only hook on CN1Playground.
        CN1Playground.testOnlyForceLayout = mobile
                ? CN1Playground.LAYOUT_MOBILE
                : CN1Playground.LAYOUT_DESKTOP;

        CN1Playground app = new CN1Playground();
        final boolean[] done = new boolean[]{false};
        final Throwable[] failure = new Throwable[]{null};
        CN.callSerially(new Runnable() {
            public void run() {
                try {
                    app.runApp();
                } catch (Throwable t) {
                    failure[0] = t;
                } finally {
                    done[0] = true;
                }
            }
        });
        // Spin the EDT briefly so runApp and the first layout pass can complete.
        long deadline = System.currentTimeMillis() + 3_000;
        while (!done[0] && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        if (!done[0]) {
            System.err.println("[" + label + "] runApp did not complete within 3 s");
            return 1;
        }
        if (failure[0] != null) {
            System.err.println("[" + label + "] runApp threw:");
            failure[0].printStackTrace();
            return 1;
        }
        // One more tick so pending layout passes settle.
        Thread.sleep(200);

        Form form = Display.getInstance().getCurrent();
        if (form == null) {
            System.err.println("[" + label + "] no current form after runApp");
            return 1;
        }

        int failures = 0;
        failures += expectPresent(label, form, "PlaygroundTopBar", "PlaygroundTopBarDark");
        failures += expectPresent(label, form, "PlaygroundAppIcon", "PlaygroundAppIconDark");
        if (mobile) {
            failures += expectPresent(label, form, "PlaygroundSegment", "PlaygroundSegmentDark");
            Component nav = expectFound(label, form, "PlaygroundBottomNav", "PlaygroundBottomNavDark");
            if (nav != null) {
                failures += expectPresent(label, form, "PlaygroundBottomNav", "PlaygroundBottomNavDark");
                // Every bottom nav should carry three child buttons (Samples,
                // Inspector, History). A present container with zero usable
                // children is a regression the simple presence check misses.
                int itemCount = countVisibleItems(nav);
                if (itemCount != 3) {
                    System.err.println("[" + label + "] PlaygroundBottomNav expected 3 visible items, got " + itemCount);
                    failures++;
                } else {
                    System.out.println("[" + label + "] PlaygroundBottomNav item count OK (3)");
                }
                failures += verifyBottomNavItems(label, nav);
            } else {
                failures++;
            }
        } else {
            failures += expectPresent(label, form, "PlaygroundActivityBar", "PlaygroundActivityBarDark");
        }
        CN1Playground.testOnlyForceLayout = CN1Playground.LAYOUT_NONE;
        return failures;
    }

    private static int countVisibleItems(Component root) {
        if (!(root instanceof Container)) {
            return 0;
        }
        Container c = (Container) root;
        int n = 0;
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component child = c.getComponentAt(i);
            if (child.isVisible() && !child.isHidden() && child.getWidth() > 0 && child.getHeight() > 0) {
                n++;
            }
        }
        return n;
    }

    /// Verifies each bottom-nav child has either an icon OR non-empty text.
    /// An empty Button with zero rendering is a common way the bottom nav can
    /// look "completely gone" even when the container reports 3 children.
    private static int verifyBottomNavItems(String scenario, Component nav) {
        if (!(nav instanceof Container)) {
            return 1;
        }
        Container c = (Container) nav;
        int failures = 0;
        String[] expectedKeys = new String[]{"samples", "inspector", "history"};
        int seen = 0;
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component child = c.getComponentAt(i);
            if (!(child instanceof com.codename1.ui.Button)) {
                continue;
            }
            com.codename1.ui.Button btn = (com.codename1.ui.Button) child;
            Object keyObj = btn.getClientProperty("navKey");
            String text = btn.getText();
            boolean hasIcon = btn.getIcon() != null;
            boolean hasText = text != null && text.length() > 0;
            if (!hasIcon && !hasText) {
                System.err.println("[" + scenario + "] bottom-nav item " + i + " (key=" + keyObj
                        + ") has no icon and no text");
                failures++;
            }
            seen++;
        }
        if (seen < expectedKeys.length) {
            System.err.println("[" + scenario + "] bottom-nav expected " + expectedKeys.length
                    + " Button children, got " + seen);
            failures++;
        }
        return failures;
    }

    private static Component expectFound(String scenario, Form form, String... uiids) {
        Component found = findByUiid(form, uiids);
        if (found == null) {
            System.err.println("[" + scenario + "] " + uiids[0] + " NOT FOUND in form tree");
        }
        return found;
    }

    private static int expectPresent(String scenario, Form form, String... uiids) {
        Component found = findByUiid(form, uiids);
        if (found == null) {
            System.err.println("[" + scenario + "] " + uiids[0] + " NOT FOUND in form tree");
            return 1;
        }
        if (!found.isVisible()) {
            System.err.println("[" + scenario + "] " + found.getUIID() + " not visible");
            return 1;
        }
        int w = found.getWidth();
        int h = found.getHeight();
        if (w <= 0 || h <= 0) {
            System.err.println("[" + scenario + "] " + found.getUIID()
                    + " has zero size: " + w + "x" + h);
            return 1;
        }
        int x = found.getAbsoluteX();
        int y = found.getAbsoluteY();
        int fw = form.getWidth();
        int fh = form.getHeight();
        if (x < 0 || y < 0 || x + w > fw + 1 || y + h > fh + 1) {
            System.err.println("[" + scenario + "] " + found.getUIID()
                    + " out of form bounds: pos=(" + x + "," + y + ") size=" + w + "x" + h
                    + " form=" + fw + "x" + fh);
            return 1;
        }
        System.out.println("[" + scenario + "] " + found.getUIID()
                + " OK pos=(" + x + "," + y + ") size=" + w + "x" + h);
        return 0;
    }

    private static Component findByUiid(Component root, String... uiids) {
        if (root == null) {
            return null;
        }
        // Skip hidden subtrees - several UIIDs (e.g. PlaygroundSegment) are used
        // by multiple Components (the top-bar Code/CSS toggle AND the mobile
        // tab strip). On mobile the first match is the hidden toggle; we want
        // the visible tab strip, so don't descend into hidden branches.
        if (root.isHidden() || !root.isVisible()) {
            return null;
        }
        String uiid = root.getUIID();
        if (uiid != null) {
            for (String candidate : uiids) {
                if (candidate.equals(uiid)) {
                    return root;
                }
            }
        }
        if (root instanceof Container) {
            Container c = (Container) root;
            for (int i = 0; i < c.getComponentCount(); i++) {
                Component found = findByUiid(c.getComponentAt(i), uiids);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
