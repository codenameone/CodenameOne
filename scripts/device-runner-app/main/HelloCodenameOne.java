package com.codenameone.examples.hellocodenameone;

import com.codename1.ui.Button;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

public class HelloCodenameOne {
    private Form current;
    private Form mainForm;

    public void init(Object context) {
        // No special initialization required for this sample
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        showMainForm();
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
    }

    public void destroy() {
        // Nothing to clean up for this sample
    }

    private void showMainForm() {
        if (mainForm == null) {
            mainForm = new Form("Main Screen", new BorderLayout());

            Container content = new Container(BoxLayout.y());
            content.getAllStyles().setBgColor(0x1f2937);
            content.getAllStyles().setBgTransparency(255);
            content.getAllStyles().setPadding(6, 6, 6, 6);
            content.getAllStyles().setFgColor(0xf9fafb);

            Label heading = new Label("Hello Codename One");
            heading.getAllStyles().setFgColor(0x38bdf8);
            heading.getAllStyles().setMargin(0, 4, 0, 0);

            Label body = new Label("Instrumentation main activity preview");
            body.getAllStyles().setFgColor(0xf9fafb);

            Button openBrowser = new Button("Open Browser Screen");
            openBrowser.addActionListener(evt -> showBrowserForm());

            content.add(heading);
            content.add(body);
            content.add(openBrowser);

            mainForm.add(BorderLayout.CENTER, content);
        }
        current = mainForm;
        mainForm.show();
    }

    private void showBrowserForm() {
        Form browserForm = new Form("Browser Screen", new BorderLayout());

        BrowserComponent browser = new BrowserComponent();
        browser.setPage(buildBrowserHtml(), null);
        browserForm.add(BorderLayout.CENTER, browser);
        browserForm.getToolbar().addMaterialCommandToLeftBar(
                "Back",
                FontImage.MATERIAL_ARROW_BACK,
                evt -> showMainForm()
        );

        current = browserForm;
        browserForm.show();
    }

    private String buildBrowserHtml() {
        return "<html><head><meta charset='utf-8'/>"
                + "<style>body{margin:0;font-family:sans-serif;background:#0e1116;color:#f3f4f6;}"
                + ".container{padding:24px;text-align:center;}h1{font-size:24px;margin-bottom:12px;}"
                + "p{font-size:16px;line-height:1.4;}span{color:#4cc9f0;}</style></head>"
                + "<body><div class='container'><h1>Codename One</h1>"
                + "<p>BrowserComponent <span>instrumentation</span> test content.</p></div></body></html>";
    }
}
