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

            Container root = new Container(BoxLayout.y());
            root.setScrollableY(true);
            root.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
            root.getAllStyles().setPadding(3, 3, 3, 3);

            SpanLabel title = new SpanLabel("Codename One Playground");
            title.getAllStyles().setFgColor(0x1f3a5f);
            root.add(title);

            Button button = new Button("Tap me");
            button.setText("Interactive controls can be added next");
            root.add(button);

            Label info = new Label("Rendered inside the preview panel");
            root.add(info);

            ctx.log("Preview built successfully");
            root;
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

            Container root = new Container(BoxLayout.y());
            root.setScrollableY(true);
            root.add(new Label("Profile Card"));
            root.add(new SpanLabel("Use the side menu to load more samples or restore history."));
            root.add(new TextField("Ada Lovelace", "Name"));
            root.add(new TextField("Mathematician", "Title"));
            root.add(new Button("Save"));
            root;
            """;

    static final String LIST_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;
            import com.codename1.components.*;

            Container root = new Container(BoxLayout.y());
            root.setScrollableY(true);
            for (int i = 1; i <= 8; i++) {
                MultiButton row = new MultiButton("Menu Item " + i);
                row.addActionListener(e -> Dialog.show("Clicked", "Clicked item " + i, "OK", null));
                row.setTextLine2("Secondary line for item " + i);
                root.add(row);
            }
            ctx.log("List sample loaded");
            root;
            """;

    static final String TABS_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;

            Tabs tabs = new Tabs();
            tabs.setTabPlacement(Component.TOP);
            tabs.addTab("News", BoxLayout.encloseY(new Label("Latest updates"), new Label("Deployment is green")));
            tabs.addTab("Stats", BoxLayout.encloseY(new Label("Users: 42"), new Label("Build time: 3m")));
            tabs.addTab("Notes", BoxLayout.encloseY(new Label("Dark mode follows the website theme")));
            tabs.getTabsContainer().getAllStyles().setBgTransparency(255);
            tabs.getTabsContainer().getAllStyles().setBgColor(0xe2e8f0);
            for (int i = 0; i < tabs.getTabCount(); i++) {
                Component tab = tabs.getTabsContainer().getComponentAt(i);
                tab.getAllStyles().setBgTransparency(255);
                tab.getAllStyles().setBgColor(0xe2e8f0);
                tab.getAllStyles().setFgColor(0x0f172a);
            }
            tabs;
            """;

    static final String BROWSER_SCRIPT = """
            import com.codename1.ui.*;
            import com.codename1.ui.layouts.*;

            Container root = new Container(new BorderLayout());
            BrowserComponent browser = new BrowserComponent();
            browser.setPage("<html><body style='font-family: sans-serif; padding: 16px;'><h2>BrowserComponent</h2><p>Embedded web content works in the preview.</p><button onclick=\\"document.body.style.background='#dbeafe'\\">Change background</button></body></html>", "https://www.codenameone.com");
            root.add(BorderLayout.CENTER, browser);
            root;
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
            root;
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
            load.addActionListener(() -> {
                RequestBuilder builder = Rest.get("https://www.codenameone.com/feed.xml");
                builder.fetchAsString(response -> {
                    String text = response.getResponseData();
                    output.setText(text == null ? "No data" : (text.length() > 280 ? text.substring(0, 280) + "..." : text));
                    output.getParent().revalidate();
                });
            });
            root.addAll(load, output);
            root;
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
            photo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    String path = Capture.capturePhoto();
                    status.setText(path == null ? "Photo capture cancelled" : path);
                    root.add(new Label(Image.createImage(path)));
                    root.revalidate();
                }
            });
            Button audio = new Button("Record Audio");
            audio.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    String path = Capture.captureAudio();
                    status.setText(path == null ? "Audio capture cancelled" : path);
                    status.getParent().revalidate();
                }
            });
            root.addAll(photo, audio, status);
            root;
            """;

    static final Sample[] SAMPLES = new Sample[]{
            new Sample("Welcome", DEFAULT_SCRIPT),
            new Sample("build(ctx)", BUILD_METHOD_SCRIPT),
            new Sample("Lifecycle Demo", LIFECYCLE_SCRIPT),
            new Sample("Profile Form", FORM_SCRIPT),
            new Sample("Menu List", LIST_SCRIPT),
            new Sample("Tabs", TABS_SCRIPT),
            new Sample("BrowserComponent", BROWSER_SCRIPT),
            new Sample("Network Fetch", NETWORK_SCRIPT),
            new Sample("REST Request", REST_SCRIPT),
            new Sample("Camera Capture", CAMERA_SCRIPT)
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
