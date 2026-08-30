package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Tabs;
import com.codename1.ui.layouts.BorderLayout;

public class TabsScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        // Skipped on the native macOS port, which renders this frame
        // wrongly. A DEFECT, not a capability the port lacks; the reason
        // code is documented in port_status_supplement.json and both must
        // be deleted when the rendering is fixed.
        if ("mac".equals(com.codename1.ui.Display.getInstance().getPlatformName())) {
            System.out.println("CN1SS:INFO:test=TabsBehavior status=SKIPPED reason=macos-capture-mid-transition");
            done();
            return true;
        }
        Form form = createForm("Tabs", new BorderLayout(), "TabsBehavior");
        Container content = new Container(new BorderLayout());
        Tabs tabs = new Tabs();
        tabs.setTabUIID("TabsColorSync");

        tabs.addTab("MatIcn", FontImage.MATERIAL_10MP, 8, new Button("Tab with material icon"));
        tabs.addTab("MatHome", FontImage.MATERIAL_HOME, 8, new Button("Second tab with material icon"));
        tabs.addTab("Txt", new Button("Tab without icon"));
        tabs.setSelectedIndex(1);

        content.add(BorderLayout.CENTER, tabs);
        form.add(BorderLayout.CENTER, content);
        form.show();
        return true;
    }
}
