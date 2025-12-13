package com.codenameone.developerguide.advancedtheming;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.Resources;
import com.codename1.ui.FontImage;

/**
 * Demonstrates code used in the advanced theming guide for creating a side menu.
 */
public class AdvancedThemingSideMenu {

    public void createSideMenu(Resources theme) {
        // tag::sideMenu[]
        Form hi = new Form("Hi World");

        Toolbar tb = hi.getToolbar();
        Image icon = theme.getImage("icon.png"); // <1>
        Container topBar = BorderLayout.east(new Label(icon));
        topBar.add(BorderLayout.SOUTH, new Label("Cool App Tagline...", "SidemenuTagline")); // <2>
        topBar.setUIID("SideCommand");
        tb.addComponentToSideMenu(topBar);

        tb.addMaterialCommandToSideMenu("Home", FontImage.MATERIAL_HOME, e -> {}); // <3>
        tb.addMaterialCommandToSideMenu("Website", FontImage.MATERIAL_WEB, e -> {});
        tb.addMaterialCommandToSideMenu("Settings", FontImage.MATERIAL_SETTINGS, e -> {});
        tb.addMaterialCommandToSideMenu("About", FontImage.MATERIAL_INFO, e -> {});

        hi.addComponent(new Label("Hi World"));
        hi.show();
        // end::sideMenu[]
    }
}
