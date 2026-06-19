package com.codename1.gamebuilder;

import com.codename1.gamebuilder.editor.StarterPacks;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.ImageIO;
import com.codename1.ui.util.Resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/// Boots a large-world scene in the editor and captures the editor (with the World/Regions
/// inspector) and the live preview (active region's streaming terrain) to verify it renders.
public final class LargeWorldProbe {
    private static GameBuilder gb;

    public static void main(String[] args) throws Exception {
        Display.init(null);
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                Resources r = Resources.openLayered("/theme");
                String[] n = r.getThemeResourceNames();
                if (n != null && n.length > 0) {
                    UIManager.getInstance().setThemeProps(r.getTheme(n[0]));
                }
            } catch (Exception ignore) {
            }
        });
        Display.getInstance().callSeriallyAndWait(() -> {
            gb = new GameBuilder();
            gb.runApp();
        });
        Display.getInstance().callSeriallyAndWait(() -> {
            gb.getController().loadLevel(StarterPacks.newLargeWorld(), "World1");
            gb.getController().model().setSelection(null);
            gb.refreshUI();
            System.out.println("[LW] isLargeWorld=" + gb.getController().model().level().isLargeWorld()
                    + " regions=" + gb.getController().model().level().getWorld().residentRegions().size());
            // grow the world: add an eastern region via the inspector button
            Component addEast = find(Display.getInstance().getCurrent(), "btn.addregion.east");
            if (addEast instanceof Button) {
                ((Button) addEast).released();
            }
            System.out.println("[LW] after add: regions="
                    + gb.getController().model().level().getWorld().residentRegions().size()
                    + " active=" + gb.getController().model().level().getWorld().getActiveRegion().getId());
        });
        shot("_largeworld-editor");
        Display.getInstance().callSeriallyAndWait(() -> {
            // back to home region so the starter terrain shows, then play
            gb.getController().model().level().getWorld().setActiveRegion("home");
            gb.refreshUI();
            Component live = find(Display.getInstance().getCurrent(), "btn.live");
            if (live instanceof Button) {
                ((Button) live).released();
            }
            gb.getCanvas().tick(0.2);
        });
        shot("_largeworld-preview");
        System.out.println("[LW] RESULT OK");
        System.exit(0);
    }

    private static void shot(String name) {
        final Image[] img = new Image[1];
        Display.getInstance().callSeriallyAndWait(() -> {
            Form f = Display.getInstance().getCurrent();
            f.setWidth(1280);
            f.setHeight(800);
            f.revalidate();
            f.layoutContainer();
            Image i = Image.createImage(1280, 800, 0xff061634);
            f.paintComponent(i.getGraphics(), true);
            img[0] = i;
        });
        try {
            File dir = new File("target/screenshots");
            dir.mkdirs();
            try (OutputStream os = new FileOutputStream(new File(dir, name + ".png"))) {
                ImageIO.getImageIO().save(img[0], os, ImageIO.FORMAT_PNG, 1f);
            }
            System.out.println("[LW] wrote " + name);
        } catch (Exception ex) {
            com.codename1.io.Log.e(ex);
        }
    }

    private static Component find(Container root, String name) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (name.equals(c.getName())) {
                return c;
            }
            if (c instanceof Container) {
                Component f = find((Container) c, name);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }
}
