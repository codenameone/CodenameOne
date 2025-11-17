package com.codename1.ui.util;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.io.FileSystemStorage;
import com.codename1.util.LazyValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

class AdditionalUtilCoverageTest extends UITestBase {

    @FormTest
    void swipeBackManualMotionReflectsCurrentX() {
        Form current = new Form();
        current.show();
        final SwipeBackSupport support = new SwipeBackSupport();
        support.bind(current, new LazyValue() {
            public Object get(Object... args) {
                Form dest = new Form();
                dest.setTransitionInAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, false, 20));
                dest.setTransitionOutAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 20));
                return dest;
            }
        });
        ActionEvent press = new ActionEvent(current, ActionEvent.Type.PointerPressed, 0, 0);
        support.pointerPressed.actionPerformed(press);
        ActionEvent drag = new ActionEvent(current, ActionEvent.Type.PointerDrag, Display.getInstance().convertToPixels(12, true), 0);
        support.pointerDragged.actionPerformed(drag);
        support.currentX = Display.getInstance().getDisplayWidth();
        SwipeBackSupport.ManualMotion motion = support.new ManualMotion(0, Display.getInstance().getDisplayWidth(), 50);
        assertEquals(Display.getInstance().getDisplayWidth(), motion.getValue());
        assertFalse(motion.isFinished());
    }

    @FormTest
    void effectsReflectionOverloadsAndBlurSupport() {
        implementation.setGaussianBlurSupported(true);
        int[] rgb = new int[]{0xff0000ff, 0xff00ff00, 0xffff0000, 0xffffffff};
        Image base = Image.createImage(rgb, 2, 2);
        Image mirroredDefault = Effects.reflectionImage(base);
        assertEquals(2, mirroredDefault.getWidth());
        Image mirroredSpacing = Effects.reflectionImage(base, 0.5f, 200, 1);
        assertEquals(base.getHeight() + 1 + 1, mirroredSpacing.getHeight());
        Image blurred = Effects.gaussianBlurImage(base, 0.5f);
        assertEquals(implementation.getGaussianBlurInvocations(), 1);
        assertTrue(Effects.isGaussianBlurSupported());
    }

    @FormTest
    void imageIOAspectDownscaleShortCircuits() throws IOException {
        final Vector calls = new Vector();
        ImageIO io = new ImageIO() {
            public void save(java.io.InputStream image, OutputStream response, String format, int width, int height, float quality) {
                calls.addElement(new Dimension(width, height));
            }

            protected void saveImage(Image img, OutputStream response, String format, float quality) {
                calls.addElement("image:" + img.getWidth());
            }

            public boolean isFormatSupported(String format) {
                return true;
            }
        };
        implementation.setImageIO(io);
        Image base = Image.createImage(3, 3);
        OutputStream out = new ByteArrayOutputStream();
        ImageIO.getImageIO().save(base, out, ImageIO.FORMAT_PNG, 1.0f);
        assertTrue(calls.contains("image:3"));

        String path = FileSystemStorage.getInstance().getAppHomePath() + "shortCircuit.png";
        OutputStream fileOut = FileSystemStorage.getInstance().openOutputStream(path);
        fileOut.write(com.codename1.ui.EncodedImage.createFromImage(base, false).getImageData());
        fileOut.close();
        String returned = ImageIO.getImageIO().saveAndKeepAspect(path, path, ImageIO.FORMAT_PNG, 4, 4, 0.9f, true, true);
        assertEquals(path, returned);
    }

    @FormTest
    void resourcesExposeTypeListsAndChecks() {
        Resources res = new Resources();
        res.setResource("l10n", Resources.MAGIC_L10N, new Hashtable());
        res.setResource("theme", Resources.MAGIC_THEME, new Hashtable());
        res.setResource("font", Resources.MAGIC_FONT, new Object());
        res.setResource("fontLegacy", Resources.MAGIC_FONT_LEGACY, new Object());
        res.setResource("animation", Resources.MAGIC_ANIMATION_LEGACY, new Object());
        res.setResource("data", Resources.MAGIC_DATA, new byte[]{1});
        res.setResource("ui", Resources.MAGIC_UI, new byte[]{2});
        res.setResource("image", Resources.MAGIC_IMAGE, new byte[]{3});

        assertArrayEquals(new String[]{"l10n"}, res.getL10NResourceNames());
        assertArrayEquals(new String[]{"data"}, res.getDataResourceNames());
        assertArrayEquals(new String[]{"ui"}, res.getUIResourceNames());
        assertEquals(2, res.getFontResourceNames().length);
        assertArrayEquals(new String[]{"theme"}, res.getThemeResourceNames());
        assertArrayEquals(new String[]{"image"}, res.getImageResourceNames());

        assertEquals(Resources.MAGIC_DATA, res.getResourceType("data"));
        assertTrue(res.isL10N("l10n"));
        assertTrue(res.isTheme("theme"));
        assertTrue(res.isFont("font"));
        assertTrue(res.isFont("fontLegacy"));
        assertTrue(res.isAnimation("animation"));
        assertTrue(res.isData("data"));
        assertTrue(res.isUI("ui"));
        assertTrue(res.isImage("image"));
    }

    @FormTest
    void uiBuilderCreatesComponentsAndTracksState() throws Exception {
        UIBuilder builder = new UIBuilder();
        builder.setKeepResourcesInRam(true);
        assertTrue(builder.isKeepResourcesInRam());
        builder.setBackCommandEnabled(true);
        builder.setHomeForm("Home");
        assertEquals("Home", builder.getHomeForm());
        Component created = builder.createComponentType("Label");
        assertTrue(created instanceof Label);

        Form form = new Form("Root", BoxLayout.y());
        form.setName("Root");
        builder.setResourceFile(new Resources());
        builder.setNextForm(form, "Next", builder.fetchResourceFile(), form);
        assertEquals("Next", form.getClientProperty("%next_form%"));

        Container root = new Container();
        Container child = new Container();
        root.add(child);
        assertEquals(root, builder.getRootAncestor(child));

        Container first = new Container();
        Container second = new Container();
        first.setName("A");
        second.setName("A");
        assertTrue(builder.isSameBackDestination(first, second));
        assertTrue(builder.allowBackTo("anything"));
    }
}
