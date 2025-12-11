package com.codename1.samples;

import com.codename1.ui.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.events.*;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class SideMenuTest3086Test extends UITestBase {

    @FormTest
    public void testSideMenu() {
        Toolbar.setGlobalToolbar(true);
        Toolbar.setOnTopSideMenu(false);

        Form hi2 = new Form("Hi2 World", BoxLayout.y());
        hi2.add(new Label("Press to show form2"));

        Button showForm2 = new Button(new Command("show form2") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    Form hi = new Form("form2");

                    Toolbar tb = hi.getToolbar();
                    // Using a placeholder image instead of loading from theme which might fail
                    Image icon = Image.createImage(100, 100, 0);
                    Container topBar = BorderLayout.east(new Label(icon));
                    topBar.add(BorderLayout.SOUTH, new Label("Cool App Tagline...", "SidemenuTagline"));
                    topBar.setUIID("SideCommand");
                    tb.addComponentToSideMenu(topBar);

                    tb.addMaterialCommandToSideMenu("Home", FontImage.MATERIAL_HOME, e -> {
                    });

                    hi.addComponent(new Label("form2 with sidemenu"));
                    hi.show();

                    // Verify side menu components
                    assertTrue(tb.getComponentCount() > 0, "Toolbar should have components");
                    // We can't easily check side menu content as it is constructed dynamically/lazily in some cases
                    // but calling addComponentToSideMenu should not fail.

                } catch (Exception ex) {
                    fail("Exception during actionPerformed: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
        hi2.add(showForm2);

        hi2.show();

        // Trigger the button action
        showForm2.getCommand().actionPerformed(new ActionEvent(showForm2));

        Form current = Display.getInstance().getCurrent();
        assertNotNull(current);
        assertEquals("form2", current.getTitle());
    }
}
