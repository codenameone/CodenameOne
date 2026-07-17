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

package com.codenameone.playground;

final class PlaygroundExamples {
    static final class Sample {
        final String title;
        final String slug;
        final String script;

        Sample(String title, String script) {
            this.title = title;
            this.slug = slugify(title);
            this.script = script;
        }
    }

    static final String DEFAULT_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;
            import com.codename1.components.*;
            import com.codename1.ui.plaf.*;

            Form form = new Form("Welcome", BoxLayout.y());

            form.add(new SpanLabel(
                "Codename One Playground - tap the device toggle above to flip between iOS and Android themes."));

            Button btn = new Button("Default Button");
            form.add(btn);

            Button raised = new Button("Raised Button");
            raised.setUIID("RaisedButton");
            form.add(raised);

            Switch wifi = new Switch();
            wifi.setValue(true);
            form.add(BoxLayout.encloseX(new Label("Wi-Fi"), new Label(" "), wifi));

            CheckBox subscribe = new CheckBox("Subscribe");
            subscribe.setSelected(true);
            form.add(subscribe);

            form.add(new TextField("", "Type something"));

            FloatingActionButton fab = FloatingActionButton.createFAB(FontImage.MATERIAL_ADD);
            fab.bindFabToContainer(form.getContentPane());
            fab.addActionListener(e -> Dialog.show("FAB", "Tapped!", "OK", null));

            ctx.log("Preview built successfully");
            """;

    static final String HELLO_WORLD_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;

            Container root = new Container(BoxLayout.y());
            root.add(new Label("Hello, World!"));
            """;

    static final String DATE_PICKER_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;
            import com.codename1.ui.spinner.*;

            Container root = new Container(BoxLayout.y());
            Picker datePicker = new Picker();
            datePicker.setType(Display.PICKER_TYPE_DATE);
            root.add(new Label("Pick a date:"));
            root.add(datePicker);
            """;

    static final String BUILD_METHOD_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;

            Component build(PlaygroundContext ctx) {
                Container root = new Container(new BorderLayout());
                root.add(BorderLayout.CENTER, new Label("build(ctx) contract"));
                ctx.log("build(ctx) executed");
                return root;
            }
            """;

    static final String LIFECYCLE_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.events.*;
            import com.codename1.ui.layouts.*;

            public class DemoApp {
                private Label status;

                public void init(Object context) {}

                public void start() {
                    Form form = new Form("Lifecycle Demo", BoxLayout.y());
                    status = new Label("Ready");
                    Button button = new Button("Tap me");
                    button.addActionListener(evt -> {
                        status.setText("Tapped at " + System.currentTimeMillis());
                        status.getParent().revalidate();
                    });
                    form.addAll(new Label("Lifecycle-style scripts are the easiest place to test listeners."), button, status);
                    form.show();
                }
            }
            """;

    static final String FORM_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;
            import com.codename1.components.*;

            Form form = new Form("Profile", BoxLayout.y());

            form.add(new SpanLabel(
                "Modern theme inputs adapt automatically to the active device skin."));

            form.add(new TextField("Ada Lovelace", "Name"));
            form.add(new TextField("Mathematician", "Title"));
            form.add(new TextField("ada@analytical.engine", "Email"));
            form.add(new TextArea("First programmer; collaborator on Babbage's Analytical Engine.", 3, 30));

            CheckBox notify = new CheckBox("Email me about updates");
            notify.setSelected(true);
            form.add(notify);

            Button save = new Button("Save");
            save.setUIID("RaisedButton");
            FontImage.setMaterialIcon(save, FontImage.MATERIAL_SAVE);
            save.addActionListener(e -> Dialog.show("Saved", "Profile updated.", "OK", null));
            form.add(save);

            Button cancel = new Button("Cancel");
            FontImage.setMaterialIcon(cancel, FontImage.MATERIAL_CLOSE);
            form.add(cancel);
            """;

    static final String LIST_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;
            import com.codename1.components.*;

            String[] titles = {"Inbox", "Starred", "Archive", "Snoozed", "Folders", "Settings"};
            String[] subtitles = {
                "12 unread", "Starred messages", "Older threads",
                "Snoozed for later", "Shared with the team", "Account & preferences"
            };
            char[] icons = {
                FontImage.MATERIAL_INBOX, FontImage.MATERIAL_STAR, FontImage.MATERIAL_ARCHIVE,
                FontImage.MATERIAL_SNOOZE, FontImage.MATERIAL_FOLDER, FontImage.MATERIAL_SETTINGS
            };

            Form form = new Form("Menu", BoxLayout.y());
            for (int i = 0; i < titles.length; i++) {
                int idx = i;
                MultiButton row = new MultiButton(titles[i]);
                row.setTextLine2(subtitles[i]);
                row.setMaterialIcon(icons[i], 5);
                row.addActionListener(e -> Dialog.show(titles[idx], subtitles[idx], "OK", null));
                form.add(row);
            }
            ctx.log("List sample loaded");
            """;

    static final String UI_SHOWCASE_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;
            import com.codename1.components.*;
            import com.codename1.ui.spinner.*;

            Form form = new Form("UI Showcase", BoxLayout.y());

            form.add(new Label("Buttons"));
            Button flat = new Button("Flat");
            Button raised = new Button("Raised");
            raised.setUIID("RaisedButton");
            Button disabled = new Button("Disabled");
            disabled.setEnabled(false);
            form.add(BoxLayout.encloseX(flat, raised, disabled));

            form.add(new Label("Toggles"));
            CheckBox check = new CheckBox("Notifications");
            check.setSelected(true);
            RadioButton radioA = new RadioButton("Light");
            RadioButton radioB = new RadioButton("Dark");
            radioB.setSelected(true);
            ButtonGroup g = new ButtonGroup(radioA, radioB);
            Switch sw = new Switch();
            sw.setValue(true);
            form.add(check);
            form.add(BoxLayout.encloseX(radioA, radioB));
            form.add(BoxLayout.encloseX(new Label("Dark mode"), sw));

            form.add(new Label("Inputs"));
            form.add(new TextField("ada@analytical.engine", "Email"));
            form.add(new TextArea("First programmer.", 2, 28));

            Picker date = new Picker();
            date.setType(Display.PICKER_TYPE_DATE);
            form.add(BoxLayout.encloseX(new Label("Birthday"), date));

            FloatingActionButton fab = FloatingActionButton.createFAB(FontImage.MATERIAL_EDIT);
            fab.bindFabToContainer(form.getContentPane());
            fab.addActionListener(e -> Dialog.show("Edit", "FAB tapped.", "OK", null));
            """;

    static final String TABS_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;
            import com.codename1.components.*;

            // Tabs render with each platform's native placement: iOS Modern
            // floats a pill bar at the bottom, Material 3 stays on top with
            // an underline indicator. The theme decides; we just add tabs.
            Form form = new Form("Tabs", new BorderLayout());
            Tabs tabs = new Tabs();
            tabs.addTab("Home",
                FontImage.MATERIAL_HOME, 4,
                BoxLayout.encloseY(
                    new SpanLabel("Latest activity"),
                    new Label("3 new notifications"),
                    new Button("Open inbox")));
            tabs.addTab("Search",
                FontImage.MATERIAL_SEARCH, 4,
                BoxLayout.encloseY(
                    new TextField("", "Search anything"),
                    new SpanLabel("Results appear here")));
            tabs.addTab("Profile",
                FontImage.MATERIAL_PERSON, 4,
                BoxLayout.encloseY(
                    new Label("Ada Lovelace"),
                    new Label("ada@analytical.engine"),
                    new Button("Sign out")));
            form.add(BorderLayout.CENTER, tabs);
            """;

    static final String BROWSER_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;

            Container root = new Container(new BorderLayout());
            BrowserComponent browser = new BrowserComponent();
            browser.setPage("<html><body style='font-family: sans-serif; padding: 16px;'><h2>BrowserComponent</h2><p>Embedded web content works in the preview.</p><button onclick=\\"document.body.style.background='#dbeafe'\\">Change background</button></body></html>", "https://www.codenameone.com");
            root.add(BorderLayout.CENTER, browser);
            """;

    static final String NETWORK_SCRIPT = """
            import com.codename1.components.*;
            import com.codename1.io.*;
            import com.codename1.ui.*;
            import com.codename1.ui.events.*;
            import com.codename1.ui.layouts.*;

            // HTML5 uses the browser's Same-Origin Policy.
            // Requests only succeed if the remote server allows this playground origin via CORS.
            // Use codenameone.com endpoints or your own endpoints with the proper Access-Control-Allow-Origin headers.
            Container root = new Container(BoxLayout.y());
            root.setScrollableY(true);
            SpanLabel output = new SpanLabel("Tap fetch to load https://www.codenameone.com/feed.xml");
            Button fetch = new Button("Fetch Feed XML");
            FontImage.setMaterialIcon(fetch, FontImage.MATERIAL_CLOUD_DOWNLOAD);
            fetch.addActionListener(evt -> {
                ConnectionRequest req = new ConnectionRequest();
                req.setUrl("https://www.codenameone.com/feed.xml");
                req.setHttpMethod("GET");
                req.addResponseListener(event -> {
                    NetworkEvent ne = (NetworkEvent) event;
                    byte[] data = ne.getConnectionRequest().getResponseData();
                    String text = data == null ? "No data" : new String(data);
                    output.setText(text.length() > 280 ? text.substring(0, 280) + "..." : text);
                    output.getParent().revalidate();
                });
                NetworkManager.getInstance().addToQueue(req);
            });
            root.addAll(fetch, output);
            """;

    static final String REST_SCRIPT = """
            import com.codename1.components.*;
            import com.codename1.io.rest.*;
            import com.codename1.ui.*;
            import com.codename1.ui.events.*;
            import com.codename1.ui.layouts.*;

            // SOP/CORS still applies on HTML5. Even REST requests need a remote server that allows this origin.
            // This sample targets a codenameone.com endpoint so it can be used to validate browser-side access.
            Container root = new Container(BoxLayout.y());
            root.setScrollableY(true);
            SpanLabel output = new SpanLabel("Tap load to fetch XML as text via RequestBuilder.");
            Button load = new Button("Load codenameone.com");
            FontImage.setMaterialIcon(load, FontImage.MATERIAL_CLOUD_DOWNLOAD);
            load.addActionListener(() -> {
                RequestBuilder builder = Rest.get("https://www.codenameone.com/feed.xml");
                builder.fetchAsString(response -> {
                    String text = response.getResponseData();
                    output.setText(text == null ? "No data" : (text.length() > 280 ? text.substring(0, 280) + "..." : text));
                    output.getParent().revalidate();
                });
            });
            root.addAll(load, output);
            """;

    static final String CAMERA_SCRIPT = """
            import com.codename1.camera.Camera;
            import com.codename1.camera.CameraFacing;
            import com.codename1.camera.CameraInfo;
            import com.codename1.camera.CameraSession;
            import com.codename1.camera.CameraSessionOptions;
            import com.codename1.camera.CameraView;
            import com.codename1.camera.CapturedPhoto;
            import com.codename1.components.*;
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;

            // The new com.codename1.camera.Camera API opens a LIVE getUserMedia
            // preview (not a file picker) and snaps a still. The browser prompts
            // for camera permission; a secure context (HTTPS or localhost) is
            // required. Camera.open() must originate from a tap.
            Form form = new Form("Camera", new BorderLayout());
            Label status = new Label("Tap 'Start Camera'");
            Button start = new Button("Start Camera");
            FontImage.setMaterialIcon(start, FontImage.MATERIAL_VIDEOCAM);
            Button snap = new Button("Take Photo");
            FontImage.setMaterialIcon(snap, FontImage.MATERIAL_PHOTO_CAMERA);
            snap.setEnabled(false);
            Container south = new Container(BoxLayout.y());
            south.addAll(start, snap, status);
            form.add(BorderLayout.SOUTH, south);

            CameraSession[] sessionHolder = new CameraSession[1];

            start.addActionListener(evt -> {
                if (!Camera.isSupported()) {
                    status.setText("Camera not supported on this device");
                    status.getParent().revalidate();
                    return;
                }
                CameraInfo info = Camera.getDefault(CameraFacing.BACK);
                if (info == null) {
                    status.setText("No camera available");
                    status.getParent().revalidate();
                    return;
                }
                try {
                    CameraSession session = Camera.open(info, new CameraSessionOptions());
                    sessionHolder[0] = session;
                    CameraView view = session.createView();
                    form.add(BorderLayout.CENTER, view);
                    start.setEnabled(false);
                    snap.setEnabled(true);
                    status.setText("Live preview running - tap 'Take Photo'");
                    form.revalidate();
                } catch (Exception ex) {
                    status.setText("Camera open failed: " + ex.getMessage());
                    status.getParent().revalidate();
                }
            });

            snap.addActionListener(evt -> {
                CameraSession session = sessionHolder[0];
                if (session == null) {
                    return;
                }
                status.setText("Capturing...");
                status.getParent().revalidate();
                session.takePhoto().ready(photo -> {
                    try {
                        Image img = photo.toImage();
                        status.setText("Captured " + photo.getWidth() + " x " + photo.getHeight());
                        status.getParent().revalidate();
                        Dialog dlg = new Dialog("Captured Photo");
                        dlg.setLayout(new BorderLayout());
                        dlg.add(BorderLayout.CENTER,
                                new Label(img.scaledWidth(Display.getInstance().getDisplayWidth() - 80)));
                        Button done = new Button("Close");
                        done.addActionListener(e2 -> dlg.dispose());
                        dlg.add(BorderLayout.SOUTH, done);
                        dlg.show();
                    } catch (Exception ex) {
                        status.setText("Capture failed: " + ex.getMessage());
                        status.getParent().revalidate();
                    }
                });
            });

            form.show();
            """;

    static final String PHYSICS_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.geom.*;
            import com.codename1.ui.layouts.*;
            import com.codename1.ui.util.*;

            // A tiny game loop: balls fall under gravity and bounce off the
            // walls and floor. Position / velocity live in parallel float[]
            // arrays; the loop runs from a UITimer and the scene is drawn through
            // GameScripting.canvas, which bridges a (Graphics, Component) lambda
            // to a paintable Component (bsh can't subclass Component to override
            // paint() on the ahead-of-time-compiled JavaScript port).
            float[] x = { 40f, 78f, 116f, 154f, 192f, 230f, 268f };
            float[] y = { 40f, 70f, 100f, 40f, 70f, 100f, 40f };
            float[] vx = { 2.4f, -2.0f, 2.8f, -2.6f, 2.2f, -2.4f, 2.0f };
            float[] vy = { 0f, 0f, 0f, 0f, 0f, 0f, 0f };
            int[] col = { 0x5fd0d6, 0xf2b134, 0xe8615f, 0x9b8cf2, 0x6fcf6b, 0xf28cc8, 0x7aa7ff };

            Component view = GameScripting.canvas(320, 480, g -> {
                g.setColor(0x10182a);
                g.fillRect(0, 0, 320, 480);
                for (int i = 0; i < x.length; i++) {
                    g.setColor(col[i]);
                    g.fillArc((int) x[i] - 16, (int) y[i] - 16, 32, 32, 0, 360);
                }
            });

            Form form = new Form("Bouncing Balls", new BorderLayout());
            form.add(BorderLayout.CENTER, view);
            UITimer.timer(16, true, form, () -> {
                for (int i = 0; i < x.length; i++) {
                    vy[i] += 0.45f;
                    x[i] += vx[i];
                    y[i] += vy[i];
                    if (x[i] < 16f) { x[i] = 16f; vx[i] = -vx[i]; }
                    if (x[i] > 304f) { x[i] = 304f; vx[i] = -vx[i]; }
                    if (y[i] > 464f) { y[i] = 464f; vy[i] = -vy[i] * 0.78f; }
                }
                view.repaint();
            });
            form.show();
            """;

    static final String GPU_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;
            import com.codename1.gpu.*;

            // GPU 3D: a spinning cube rendered through the RenderView / gpu API.
            // The two lambdas are the Renderer's onInit / onFrame callbacks
            // (GpuScripting bridges them to the multi-method Renderer interface).
            // Renders on GPU-capable targets (WebGL browsers, device builds).
            Form form = new Form("3D / GPU", new BorderLayout());

            Camera camera = new Camera()
                    .setPerspective(60f, 0.1f, 100f)
                    .setPosition(0f, 0f, 4f)
                    .setTarget(0f, 0f, 0f);
            Mesh[] cube = new Mesh[1];
            Material[] material = new Material[1];
            float[] angle = { 0f };

            RenderView view = new RenderView(GpuScripting.renderer(
                device -> {
                    cube[0] = Primitives.cube(device, 1f);
                    material[0] = new Material(Material.Type.PHONG).setColor(0xff3399ff);
                },
                device -> {
                    angle[0] += 0.02f;
                    device.clear(0xff10182a, true, true);
                    device.setCamera(camera);
                    device.draw(cube[0], material[0], Matrix4.rotation(angle[0], 0.4f, 1f, 0.2f));
                }));
            view.setContinuous(true);

            form.add(BorderLayout.CENTER, view);
            form.show();
            """;

    static final String CLIPBOARD_SCRIPT = """
            import com.codename1.components.*;
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;

            // Copies text to the system clipboard via Display.copyToClipboard().
            // On the web (JavaScript port) the write is performed on the browser's
            // main thread through the Clipboard API / execCommand fallback.
            Container root = new Container(BoxLayout.y());
            root.setScrollableY(true);
            root.add(new SpanLabel(
                "Type something, tap Copy, then paste (Cmd/Ctrl+V) into another app to confirm the system clipboard received it."));

            TextField input = new TextField("https://www.codenameone.com", "Text to copy");
            Label status = new Label("Ready");

            Button copy = new Button("Copy to Clipboard");
            FontImage.setMaterialIcon(copy, FontImage.MATERIAL_CONTENT_COPY);
            copy.addActionListener(evt -> {
                Display.getInstance().copyToClipboard(input.getText());
                status.setText("Copied: " + input.getText());
                status.getParent().revalidate();
                ctx.log("copyToClipboard called");
            });

            Button paste = new Button("Read Back");
            FontImage.setMaterialIcon(paste, FontImage.MATERIAL_CONTENT_PASTE);
            paste.addActionListener(evt -> {
                Object data = Display.getInstance().getPasteDataFromClipboard();
                status.setText("Clipboard: " + (data == null ? "<empty>" : data.toString()));
                status.getParent().revalidate();
            });

            root.addAll(input, copy, paste, status);
            """;

    static final String SHARE_SCRIPT = """
            import com.codename1.components.*;
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;

            // Invokes the native share sheet via Display.share(). On the web this
            // uses the browser Web Share API (navigator.share), which requires an
            // HTTPS origin and a user gesture; otherwise it falls back.
            Container root = new Container(BoxLayout.y());
            root.setScrollableY(true);
            root.add(new SpanLabel(
                "Shares text/links through the platform share dialog (navigator.share on the web)."));
            root.add(new Label("Native share supported: " + Display.getInstance().isNativeShareSupported()));

            TextField input = new TextField(
                "Check out Codename One https://www.codenameone.com", "Text to share");
            Label status = new Label("Ready");

            Button share = new Button("Share");
            FontImage.setMaterialIcon(share, FontImage.MATERIAL_SHARE);
            share.addActionListener(evt -> {
                Display.getInstance().share(input.getText());
                status.setText("share() invoked");
                status.getParent().revalidate();
                ctx.log("share called");
            });

            root.addAll(input, share, status);
            """;

    static final String FULLSCREEN_SCRIPT = """
            import com.codename1.components.*;
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;

            // Enters/exits browser fullscreen via CN.requestFullScreen() and
            // CN.exitFullScreen(). Fullscreen requires a user gesture on the web.
            Container root = new Container(BoxLayout.y());
            root.setScrollableY(true);
            root.add(new SpanLabel("Toggle the browser fullscreen mode."));

            Label status = new Label("");

            Button enter = new Button("Enter Fullscreen");
            FontImage.setMaterialIcon(enter, FontImage.MATERIAL_FULLSCREEN);
            enter.addActionListener(evt -> {
                CN.requestFullScreen();
                status.setText("Supported: " + CN.isFullScreenSupported()
                    + "  In fullscreen: " + CN.isInFullScreenMode());
                status.getParent().revalidate();
            });

            Button exit = new Button("Exit Fullscreen");
            FontImage.setMaterialIcon(exit, FontImage.MATERIAL_FULLSCREEN_EXIT);
            exit.addActionListener(evt -> {
                CN.exitFullScreen();
                status.setText("Supported: " + CN.isFullScreenSupported()
                    + "  In fullscreen: " + CN.isInFullScreenMode());
                status.getParent().revalidate();
            });

            status.setText("Supported: " + CN.isFullScreenSupported()
                + "  In fullscreen: " + CN.isInFullScreenMode());
            root.addAll(enter, exit, status);
            """;

    static final String PRINT_SCRIPT = """
            import com.codename1.components.*;
            import com.codename1.printing.*;
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;

            // Prints an image through Printer.printImage(). On the web this opens
            // the browser print dialog for a generated PNG. The result listener
            // is a lambda (PrintResultListener is in the lambda-adapter switch).
            Container root = new Container(BoxLayout.y());
            root.setScrollableY(true);
            root.add(new SpanLabel("Generate an image and send it to the platform print dialog."));
            root.add(new Label("Printing supported: " + Printer.isPrintingSupported()));

            Label status = new Label("Ready");

            Button print = new Button("Print Sample Image");
            FontImage.setMaterialIcon(print, FontImage.MATERIAL_PRINT);
            print.addActionListener(evt -> {
                Image img = Image.createImage(400, 250, 0xffffffff);
                Graphics g = img.getGraphics();
                g.setColor(0x1565c0);
                g.fillRect(0, 0, 400, 250);
                g.setColor(0xffffff);
                g.drawString("Codename One Playground", 40, 110);
                status.setText("Printing...");
                status.getParent().revalidate();
                Printer.printImage(img, result -> {
                    if (result.isCompleted()) {
                        status.setText("Print dialog opened");
                    } else if (result.isCancelled()) {
                        status.setText("Print cancelled");
                    } else {
                        status.setText("Print failed: " + result.getError());
                    }
                    status.getParent().revalidate();
                });
                ctx.log("printImage called");
            });

            root.addAll(print, status);
            """;

    static final Sample[] SAMPLES = new Sample[]{
            new Sample("Welcome", DEFAULT_SCRIPT),
            new Sample("UI Showcase", UI_SHOWCASE_SCRIPT),
            new Sample("Hello World", HELLO_WORLD_SCRIPT),
            new Sample("Lifecycle Demo", LIFECYCLE_SCRIPT),
            new Sample("Date Picker", DATE_PICKER_SCRIPT),
            new Sample("Menu List", LIST_SCRIPT),
            new Sample("Profile Form", FORM_SCRIPT),
            new Sample("Tabs", TABS_SCRIPT),
            new Sample("BrowserComponent", BROWSER_SCRIPT),
            new Sample("Network Fetch", NETWORK_SCRIPT),
            new Sample("REST Request", REST_SCRIPT),
            new Sample("Camera Capture", CAMERA_SCRIPT),
            new Sample("Clipboard", CLIPBOARD_SCRIPT),
            new Sample("Native Share", SHARE_SCRIPT),
            new Sample("Fullscreen", FULLSCREEN_SCRIPT),
            new Sample("Printing", PRINT_SCRIPT),
            new Sample("Bouncing Balls", PHYSICS_SCRIPT),
            new Sample("3D / GPU", GPU_SCRIPT)
    };

    private PlaygroundExamples() {
    }

    static Sample findBySlug(String slug) {
        if (slug == null || slug.length() == 0) {
            return null;
        }
        for (int i = 0; i < SAMPLES.length; i++) {
            if (slug.equals(SAMPLES[i].slug)) {
                return SAMPLES[i];
            }
        }
        return null;
    }

    private static String slugify(String value) {
        StringBuilder out = new StringBuilder();
        boolean previousDash = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = Character.toLowerCase(value.charAt(i));
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                out.append(ch);
                previousDash = false;
            } else if (!previousDash) {
                out.append('-');
                previousDash = true;
            }
        }
        int len = out.length();
        while (len > 0 && out.charAt(len - 1) == '-') {
            out.deleteCharAt(len - 1);
            len--;
        }
        return out.toString();
    }
}
