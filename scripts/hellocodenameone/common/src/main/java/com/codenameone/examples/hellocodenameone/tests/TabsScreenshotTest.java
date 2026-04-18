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
        System.out.println("CN1SS:TABS:step=createForm");
        Form form = createForm("Tabs", new BorderLayout(), "TabsBehavior");
        System.out.println("CN1SS:TABS:step=createContent");
        Container content = new Container(new BorderLayout());
        System.out.println("CN1SS:TABS:step=createTabs");
        Tabs tabs = new Tabs();
        System.out.println("CN1SS:TABS:step=setTabUIID");
        tabs.setTabUIID("TabsColorSync");

        System.out.println("CN1SS:TABS:step=createTab1Button");
        Button tab1Button = new Button("Tab with material icon");
        System.out.println("CN1SS:TABS:step=addTab1");
        tabs.addTab("MatIcn", FontImage.MATERIAL_10MP, 8, tab1Button);
        System.out.println("CN1SS:TABS:step=createTab2Button");
        Button tab2Button = new Button("Second tab with material icon");
        System.out.println("CN1SS:TABS:step=addTab2");
        tabs.addTab("MatHome", FontImage.MATERIAL_HOME, 8, tab2Button);
        System.out.println("CN1SS:TABS:step=addTab3");
        tabs.addTab("Txt", new Button("Tab without icon"));
        System.out.println("CN1SS:TABS:step=setSelectedIndex");
        tabs.setSelectedIndex(1);

        System.out.println("CN1SS:TABS:step=addContent");
        content.add(BorderLayout.CENTER, tabs);
        System.out.println("CN1SS:TABS:step=addForm");
        form.add(BorderLayout.CENTER, content);
        System.out.println("CN1SS:TABS:step=show");
        form.show();
        System.out.println("CN1SS:TABS:step=done");
        return true;
    }
}
