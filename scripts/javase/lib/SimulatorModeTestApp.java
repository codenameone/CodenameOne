package com.codenameone.examples.javase.tests;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Small simulator app used by JavaSE integration tests.
 */
public class SimulatorModeTestApp {
    private Form current;

    public void init(Object context) {
        Toolbar.setGlobalToolbar(true);
        boolean appThemeInstalled = false;
        try {
            Resources theme = Resources.openLayered("/theme");
            UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
            appThemeInstalled = true;
        } catch (Exception ignored) {
            // No app theme bundled - fall through to native-theme-only
            // path below so the screenshot still reflects the user's
            // Native Theme menu pick.
        }
        if (!appThemeInstalled && Display.getInstance().hasNativeTheme()) {
            // JavaSEPort.loadSkinFile has already cached the
            // simulatorNativeTheme pick as nativeThemeRes, but nothing
            // has pushed it into UIManager. Installing it directly
            // lights up the iOS Modern / Android Material chrome in
            // the captured screenshot; without this the form renders
            // with the bare DefaultLookAndFeel and the screenshots
            // for every theme look identical.
            Display.getInstance().installNativeTheme();
        }
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        String mode = System.getProperty("cn1.test.window.mode", "unknown");
        Form form = new Form("JavaSE Simulator Test", new BorderLayout());
        // The Toolbar carries most of the visual identity of a native
        // theme (title bar color, font, status-bar treatment), so we
        // need the global Toolbar in place for the screenshot to look
        // different across themes.
        Toolbar tb = form.getToolbar();
        tb.setTitle("JavaSE Simulator Test");
        tb.addMaterialCommandToSideMenu("Settings", com.codename1.ui.FontImage.MATERIAL_SETTINGS,
                evt -> { /* placeholder */ });

        form.add(BorderLayout.NORTH, new Label("Window mode: " + mode));

        com.codename1.ui.Container body = new com.codename1.ui.Container(BoxLayout.y());
        body.add(new Label("Robot validation baseline"));
        Button primary = new Button("Primary Action");
        primary.setUIID("RaisedButton");
        body.add(primary);
        Button dialogButton = new Button("Open Dialog");
        dialogButton.addActionListener(evt -> Dialog.show("Mode", "Current mode: " + mode, "OK", null));
        body.add(dialogButton);
        // Add a few more theme-bearing components so the screenshot
        // captures more than just two flat buttons. CheckBox + TextField
        // pick up theme-specific colors and metrics that diverge
        // visibly between iOS Modern, Android Material and the legacy
        // pair.
        body.add(new CheckBox("Enable feature"));
        TextField tf = new TextField("", "Text field hint", 20, TextField.ANY);
        body.add(tf);

        // Native-theme verification: when the harness sets
        // cn1.test.expectedNativeTheme, query JavaSEPort for the
        // resource it actually loaded and emit a result line to both
        // stdout and an optional sentinel file. The outer verifier reads
        // either channel to assert the menu's simulatorNativeTheme
        // preference was honored on simulator startup. We surface the
        // result in the UI too so the screenshot of a failing case is
        // self-explanatory.
        String expectedNativeTheme = System.getProperty("cn1.test.expectedNativeTheme");
        if (expectedNativeTheme != null && !expectedNativeTheme.isEmpty()) {
            String diagnostic = reportNativeThemeResult(expectedNativeTheme);
            Label diagLabel = new Label(diagnostic);
            diagLabel.setUIID("Label");
            body.add(diagLabel);
        }

        form.add(BorderLayout.CENTER, body);

        current = form;
        form.show();

        if (Boolean.getBoolean("cn1.test.landscape")) {
            CN.callSerially(() -> {
                try {
                    CN.setWindowSize(900, 520);
                    CN.lockOrientation(false);
                    form.revalidate();
                } catch (Throwable ignored) {
                }
            });
        }

        if (Boolean.getBoolean("cn1.test.doNetwork")) {
            ConnectionRequest req = new ConnectionRequest();
            req.setUrl("https://example.com");
            req.setPost(false);
            req.setFailSilently(true);
            req.setHttpMethod("GET");
            NetworkManager.getInstance().addToQueue(req);
        }

        // Safety exit in case a harness forgets to close the simulator.
        CN.setTimeout(120000, () -> {
            if (CN.getCurrentForm() != null) {
                CN.log("JavaSE simulator integration app safety timeout reached.");
                CN.exitApplication();
            }
        });
    }

    /**
     * Reports whether the active native theme matches {@code expected}.
     *
     * <p>The current Simulator path stores the user's "Native Theme" menu
     * choice in the {@code simulatorNativeTheme} preference, then reloads
     * the simulator. On reload, {@code JavaSEPort.loadSkinFile} reads the
     * preference, loads {@code /&lt;name&gt;.res} from the classpath, and
     * caches the resolved key. We check three things:
     *
     * <ol>
     *   <li>{@code JavaSEPort.getCurrentSimulatorNativeTheme()} matches
     *       the expected key - this is what the simulator's loadSkinFile
     *       captured from the preference / build hints. A mismatch here
     *       means the preference wasn't honored or auto-resolution
     *       picked something else.</li>
     *   <li>{@code JavaSEPort.getNativeTheme()} returns non-null - the
     *       cached {@code Resources} is what {@code installNativeTheme}
     *       actually layers under the app theme. Null here means the
     *       .res lookup failed even though the override was set, which
     *       is what you'd hit if the modern themes weren't bundled.</li>
     *   <li>The expected {@code .res} is still present in the classpath
     *       so the test is exercising a real load path.</li>
     * </ol>
     *
     * <p>The string returned is what we render on the form for the
     * screenshot.
     */
    private String reportNativeThemeResult(String expected) {
        String resolvedKey = null;
        boolean nativeResLoaded = false;
        boolean expectedResPresent = false;
        try {
            resolvedKey = JavaSEPort.getCurrentSimulatorNativeTheme();
            Resources nativeRes = JavaSEPort.getNativeTheme();
            nativeResLoaded = nativeRes != null;
            InputStream is = JavaSEPort.class.getResourceAsStream("/" + expected + ".res");
            if (is != null) {
                expectedResPresent = true;
                try { is.close(); } catch (Exception ignored) { }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        boolean pass = expected.equals(resolvedKey) && nativeResLoaded && expectedResPresent;
        String result = pass ? "PASS" : "FAIL";
        String line = "[native-theme-test] result=" + result
                + " expected=" + expected
                + " resolvedKey=" + resolvedKey
                + " nativeResLoaded=" + nativeResLoaded
                + " expectedResPresent=" + expectedResPresent;
        System.out.println(line);
        // Optional sentinel file so harnesses that can't tail stdout in
        // realtime can still read the result. The path is configured by
        // the verifier; absence is fine.
        String sentinel = System.getProperty("cn1.test.nativeThemeResultFile");
        if (sentinel != null && !sentinel.isEmpty()) {
            try {
                Path p = Paths.get(sentinel);
                Path parent = p.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.write(p, (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return "Native theme " + result + ": expected=" + expected
                + " loaded=" + (resolvedKey != null ? resolvedKey : "(none)");
    }

    public void stop() {
        current = CN.getCurrentForm();
    }

    public void destroy() {
        current = null;
    }
}
