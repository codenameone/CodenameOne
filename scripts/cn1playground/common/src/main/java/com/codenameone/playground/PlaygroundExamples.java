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
                    button.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            status.setText("Tapped at " + System.currentTimeMillis());
                            status.getParent().revalidate();
                        }
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
                FontImage.createMaterial(FontImage.MATERIAL_HOME, "Tab", 4),
                BoxLayout.encloseY(
                    new SpanLabel("Latest activity"),
                    new Label("3 new notifications"),
                    new Button("Open inbox")));
            tabs.addTab("Search",
                FontImage.createMaterial(FontImage.MATERIAL_SEARCH, "Tab", 4),
                BoxLayout.encloseY(
                    new TextField("", "Search anything"),
                    new SpanLabel("Results appear here")));
            tabs.addTab("Profile",
                FontImage.createMaterial(FontImage.MATERIAL_PERSON, "Tab", 4),
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
            fetch.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    ConnectionRequest req = new ConnectionRequest();
                    req.setUrl("https://www.codenameone.com/feed.xml");
                    req.setHttpMethod("GET");
                    req.addResponseListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            NetworkEvent ne = (NetworkEvent) event;
                            byte[] data = ne.getConnectionRequest().getResponseData();
                            String text = data == null ? "No data" : new String(data);
                            output.setText(text.length() > 280 ? text.substring(0, 280) + "..." : text);
                            output.getParent().revalidate();
                        }
                    });
                    NetworkManager.getInstance().addToQueue(req);
                }
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
            import com.codename1.capture.Capture;
            import com.codename1.components.*;
            import com.codename1.ui.events.*;
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;

            Container root = new Container(BoxLayout.y());
            root.setScrollableY(true);
            Label status = new Label("Ready to use camera features");
            root.add(new SpanLabel("Use this sample on device or simulator targets that support capture."));
            Button photo = new Button("Capture Photo");
            FontImage.setMaterialIcon(photo, FontImage.MATERIAL_PHOTO_CAMERA);
            photo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    String path = Capture.capturePhoto();
                    status.setText(path == null ? "Photo capture cancelled" : path);
                    root.add(new Label(Image.createImage(path)));
                    root.revalidate();
                }
            });
            Button audio = new Button("Record Audio");
            FontImage.setMaterialIcon(audio, FontImage.MATERIAL_MIC);
            audio.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    String path = Capture.captureAudio();
                    status.setText(path == null ? "Audio capture cancelled" : path);
                    status.getParent().revalidate();
                }
            });
            root.addAll(photo, audio, status);
            """;

    static final String PHYSICS_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.geom.*;
            import com.codename1.ui.layouts.*;
            import com.codename1.gaming.physics.*;

            // 2D physics with Codename One's bundled engine: drop a few balls
            // into a box and let them bounce. The world is stepped from a
            // UITimer and the bodies are drawn straight from their positions.
            final PhysicsWorld world = new PhysicsWorld(0, 1400);
            final java.util.ArrayList balls = new java.util.ArrayList();
            world.createBox(160, 470, 320, 24, BodyType.STATIC);   // floor
            world.createBox(8, 240, 16, 480, BodyType.STATIC);     // left wall
            world.createBox(312, 240, 16, 480, BodyType.STATIC);   // right wall
            for (int i = 0; i < 6; i++) {
                PhysicsBody ball = world.createCircle(45 + i * 44, 50 + (i % 3) * 34, 15, BodyType.DYNAMIC);
                ball.setRestitution(0.72f);
                balls.add(ball);
            }

            Component view = new Component() {
                protected Dimension calcPreferredSize() {
                    return new Dimension(320, 480);
                }
                public void paint(Graphics g) {
                    g.setColor(0x10182a);
                    g.fillRect(getX(), getY(), getWidth(), getHeight());
                    g.setColor(0x3dc8d1);
                    for (int i = 0; i < balls.size(); i++) {
                        PhysicsBody b = (PhysicsBody) balls.get(i);
                        int cx = getX() + (int) b.getX();
                        int cy = getY() + (int) b.getY();
                        g.fillArc(cx - 15, cy - 15, 30, 30, 0, 360);
                    }
                }
            };

            Form form = new Form("Physics", new BorderLayout());
            form.add(BorderLayout.CENTER, view);
            UITimer.timer(16, true, form, () -> {
                world.step(1f / 60f);
                view.repaint();
            });
            form.show();
            """;

    static final String GPU_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;
            import com.codename1.gaming.*;
            import com.codename1.gpu.*;

            // GPU 3D: a spinning cube rendered through the GameView / gpu API.
            // Renders on GPU-capable targets (device builds, WebGL browsers).
            final double[] angle = { 0 };
            final Model[] cubeRef = new Model[1];

            GameView game = new GameView() {
                protected void onSetup(GraphicsDevice device) {
                    setClearColor(0xff10182a);
                    Model cube = new Model(Primitives.cube(device, 1.0f));
                    addModel(cube);
                    cubeRef[0] = cube;
                    getCamera().setPerspective(60, 0.1f, 100f)
                               .setPosition(0, 0, 4)
                               .setTarget(0, 0, 0);
                }
                protected void update(double deltaSeconds) {
                    angle[0] += deltaSeconds * 45;
                    if (cubeRef[0] != null) {
                        cubeRef[0].setRotation((float) angle[0], (float) (angle[0] * 0.7), 0);
                    }
                }
            };

            Form form = new Form("3D / GPU", new BorderLayout());
            form.add(BorderLayout.CENTER, game);
            game.start();
            form.show();
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
            new Sample("Physics", PHYSICS_SCRIPT),
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
