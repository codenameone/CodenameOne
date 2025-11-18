package com.codename1.ui.util;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.Painter;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.util.LazyValue;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 5)
class UtilCoverageTest extends UITestBase {

    @FormTest
    void embeddedContainerStoresEmbed() {
        EmbeddedContainer embedded = new EmbeddedContainer();
        assertTrue(embedded.getLayout() instanceof BorderLayout);

        embedded.setEmbed("destination");
        assertEquals("destination", embedded.getEmbed());
    }

    @FormTest
    void glassTutorialPaintsHintsAroundDestination() {
        GlassTutorial tutorial = new GlassTutorial();
        RecordingComponent destination = new RecordingComponent(10, 20, 30, 40, 12, 8);

        RecordingComponent center = new RecordingComponent(0, 0, 0, 0, 7, 5);
        RecordingComponent south = new RecordingComponent(0, 0, 0, 0, 6, 6);
        RecordingComponent north = new RecordingComponent(0, 0, 0, 0, 4, 4);
        RecordingComponent east = new RecordingComponent(0, 0, 0, 0, 5, 3);
        RecordingComponent west = new RecordingComponent(0, 0, 0, 0, 9, 2);

        tutorial.addHint(center, destination, BorderLayout.CENTER);
        tutorial.addHint(south, destination, BorderLayout.SOUTH);
        tutorial.addHint(north, destination, BorderLayout.NORTH);
        tutorial.addHint(east, destination, BorderLayout.EAST);
        tutorial.addHint(west, destination, BorderLayout.WEST);

        Image canvas = Image.createImage(80, 80);
        Graphics g = canvas.getGraphics();
        tutorial.paint(g, new Rectangle(0, 0, 80, 80));

        assertEquals(destination.x, center.paintedX);
        assertEquals(destination.y, center.paintedY);
        assertEquals(destination.w, center.paintedW);
        assertEquals(destination.h, center.paintedH);

        assertEquals(destination.x + destination.w / 2 - south.pref.getWidth() / 2, south.paintedX);
        assertEquals(destination.y + destination.h, south.paintedY);

        assertEquals(destination.x + destination.w / 2 - north.pref.getWidth() / 2, north.paintedX);
        assertEquals(destination.y - north.pref.getHeight(), north.paintedY);

        assertEquals(destination.x + destination.w, east.paintedX);
        assertEquals(destination.y + destination.h / 2 - east.pref.getHeight() / 2, east.paintedY);

        assertEquals(destination.x - west.pref.getWidth(), west.paintedX);
        assertEquals(destination.y + destination.h / 2 - west.pref.getHeight() / 2, west.paintedY);
    }

    @FormTest
    void glassTutorialShowOnRestoresGlassPaneAndTint() {
        Form form = new Form();
        final boolean[] disposed = new boolean[1];
        form.setGlassPane(new Painter() {
            public void paint(Graphics g, Rectangle rect) {
                disposed[0] = true;
            }
        });
        int originalTint = form.getTintColor();

        GlassTutorial tutorial = new GlassTutorial();
        tutorial.showOn(form);

        assertEquals(originalTint, form.getTintColor());
        Image buffer = Image.createImage(5, 5);
        form.getGlassPane().paint(buffer.getGraphics(), new Rectangle(0, 0, 5, 5));
        assertTrue(disposed[0]);
    }

    @FormTest
    void swipeBackSupportRespondsToPointersAndMotion() {
        Hashtable theme = new Hashtable();
        theme.put("sideSwipeSensitiveInt", new Integer(10));
        theme.put("backGestureThresholdInt", new Integer(1));
        UIManager.getInstance().setThemeProps(theme);

        Form current = new Form();
        current.setTransitionOutAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, false, 50));
        current.show();

        final Form destination = new Form();
        destination.setTransitionOutAnimator(new Transition() {
            public boolean animate() {
                return false;
            }

            public void paint(Graphics g) {
            }
        });
        destination.setTransitionInAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, false, 50));

        final SwipeBackSupport support = new SwipeBackSupport();
        support.bind(current, new LazyValue() {
            public Object get(Object... args) {
                return destination;
            }
        });

        ActionEvent press = new ActionEvent(current, ActionEvent.Type.PointerPressed, 2, 2);
        support.pointerPressed.actionPerformed(press);

        ActionEvent drag = new ActionEvent(current, ActionEvent.Type.PointerDrag, Display.getInstance().convertToPixels(20, true), 2);
        support.pointerDragged.actionPerformed(drag);
        assertTrue(support.dragActivated);
        assertNotNull(support.destinationForm);

        support.startBackTransition(current, destination);
        assertTrue(support.transitionRunning);

        support.currentX = Display.getInstance().getDisplayWidth() / 2;
        SwipeBackSupport.ManualMotion motion = support.new ManualMotion(0, Display.getInstance().getDisplayWidth(), 100);
        int midValue = motion.getValue();
        assertTrue(midValue > 0);

        ActionEvent release = new ActionEvent(current, ActionEvent.Type.PointerReleased, Display.getInstance().getDisplayWidth(), 2);
        support.pointerReleased.actionPerformed(release);
    }

    @FormTest
    void effectsUtilitiesHandleImages() {
        implementation.setGaussianBlurSupported(false);
        Image base = Image.createImage(2, 2, 0xff00ff00);
        Graphics g = base.getGraphics();
        g.setColor(0xffff0000);
        g.fillRect(0, 0, 2, 2);

        Image reflected = Effects.reflectionImage(base, 0.5f, 200, 0);
        assertEquals(2, reflected.getWidth());
        assertEquals(3, reflected.getHeight());
        int[] rgb = reflected.getRGB();
        assertEquals(0xffff0000, rgb[0]);
        assertTrue((rgb[rgb.length - 1] >>> 24) > 0);

        Image perspective = Effects.verticalPerspective(base, 1.0f, 0.5f, 1.0f);
        assertEquals(2, perspective.getWidth());
        assertEquals(2, perspective.getHeight());

        Image shadow = Effects.dropshadow(base, 2, 0.5f, 1, 1);
        assertEquals(base.getWidth() + 1, shadow.getWidth());
        assertEquals(base.getHeight() + 1, shadow.getHeight());

        Image simpleShadow = Effects.squareShadow(4, 3, 2, 0.4f);
        assertEquals(4 + 4, simpleShadow.getWidth());
        assertEquals(3 + 4, simpleShadow.getHeight());

        implementation.setGaussianBlurSupported(true);
        int before = implementation.getGaussianBlurInvocations();
        Image blurredShadow = Effects.squareShadow(2, 2, 1, 0.6f);
        assertEquals(before + 1, implementation.getGaussianBlurInvocations());
        assertEquals(4, blurredShadow.getWidth());
        assertEquals(4, blurredShadow.getHeight());
    }

    @FormTest
    void effectsSupportGrowShrinkAndGaussianBlurQueries() {
        Component c = new Label("grow");
        int originalPreferred = c.getPreferredW();
        Effects.growShrink(c, 5);
        assertEquals(originalPreferred, c.getPreferredW());

        implementation.setGaussianBlurSupported(false);
        assertFalse(Effects.isGaussianBlurSupported());

        implementation.setGaussianBlurSupported(true);
        Image base = Image.createImage(1, 1);
        Effects.dropshadow(base, 1, 0.3f);
        assertTrue(Effects.isGaussianBlurSupported());
    }

    @FormTest
    void imageIOOperationsRespectSizing() throws IOException {
        final Vector calls = new Vector();
        ImageIO recorder = new ImageIO() {
            public void save(java.io.InputStream image, OutputStream response, String format, int width, int height, float quality) {
                calls.addElement(new Integer(width));
                calls.addElement(new Integer(height));
                calls.addElement(format);
            }

            protected void saveImage(Image img, OutputStream response, String format, float quality) {
                calls.addElement("image:" + format);
            }

            public boolean isFormatSupported(String format) {
                return true;
            }
        };
        implementation.setImageIO(recorder);

        Image plain = Image.createImage(2, 2, 0xff112233);
        com.codename1.ui.EncodedImage encoded = com.codename1.ui.EncodedImage.createFromImage(plain, false);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.getImageIO().save(encoded, out, ImageIO.FORMAT_PNG, 1.0f);
        assertEquals(3, calls.size());
        assertEquals(new Integer(2), calls.elementAt(0));
        assertEquals(new Integer(2), calls.elementAt(1));
        assertEquals(ImageIO.FORMAT_PNG, calls.elementAt(2));

        String path = FileSystemStorage.getInstance().getAppHomePath() + "img.png";
        OutputStream fileOut = FileSystemStorage.getInstance().openOutputStream(path);
        fileOut.write(encoded.getImageData());
        fileOut.close();

        calls.clear();
        ImageIO.getImageIO().save(path, new ByteArrayOutputStream(), ImageIO.FORMAT_PNG, -1, -1, 0.8f);
        assertEquals(new Integer(-1), calls.elementAt(0));
        assertEquals(new Integer(-1), calls.elementAt(1));

        Dimension size = ImageIO.getImageIO().getImageSize(path);
        assertEquals(2, size.getWidth());
        assertEquals(2, size.getHeight());

        calls.clear();
        String resized = FileSystemStorage.getInstance().getAppHomePath() + "resized.png";
        String returned = ImageIO.getImageIO().saveAndKeepAspect(path, resized, ImageIO.FORMAT_PNG, 4, 2, 0.9f, false, false);
        assertEquals(resized, returned);
        assertEquals(new Integer(4), calls.elementAt(0));
        assertEquals(new Integer(4), calls.elementAt(1));
    }

    @FormTest
    void imageIOSaveScalesWhenPositiveSizesProvided() throws IOException {
        final Vector calls = new Vector();
        ImageIO recorder = new ImageIO() {
            public void save(java.io.InputStream image, OutputStream response, String format, int width, int height, float quality) {
                calls.addElement("stream" + width + "x" + height);
            }

            protected void saveImage(Image img, OutputStream response, String format, float quality) {
                calls.addElement("direct:" + img.getWidth() + "x" + img.getHeight());
            }

            public boolean isFormatSupported(String format) {
                return true;
            }
        };
        implementation.setImageIO(recorder);
        Image plain = Image.createImage(3, 5, 0xff00ff00);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.getImageIO().save(plain, out, ImageIO.FORMAT_PNG, 1.0f);
        assertTrue(calls.contains("direct:3x5"));

        calls.clear();
        String path = FileSystemStorage.getInstance().getAppHomePath() + "scaled.png";
        OutputStream fileOut = FileSystemStorage.getInstance().openOutputStream(path);
        fileOut.write(com.codename1.ui.EncodedImage.createFromImage(Image.createImage(2, 2), false).getImageData());
        fileOut.close();
        ImageIO.getImageIO().save(path, new ByteArrayOutputStream(), ImageIO.FORMAT_PNG, 6, 4, 0.8f);
        assertTrue(calls.contains("stream6x4"));
    }

    @FormTest
    void resourcesAndUIBuilderSimpleState() {
        Resources resources = new Resources();
        resources.setFailOnMissingTruetype(true);
        assertTrue(resources.isFailOnMissingTruetype());
        resources.setEnableMediaQueries(true);
        assertTrue(resources.isEnableMediaQueries());
        resources.setRuntimeMultiImageEnabled(true);
        resources.setClassLoader(getClass());
        resources.setPassword("secret");
        resources.clear();

        ArrayList list = new ArrayList();
        resources.setResource("l10n", Resources.MAGIC_L10N, new Hashtable());
        resources.setResource("theme", Resources.MAGIC_THEME, new Hashtable());
        resources.setResource("font", Resources.MAGIC_FONT, new Object());
        resources.setResource("animation", Resources.MAGIC_ANIMATION_LEGACY, new byte[0]);
        resources.setResource("data", Resources.MAGIC_DATA, new byte[]{1});
        resources.setResource("ui", Resources.MAGIC_UI, new byte[]{2});
        resources.setResource("image", Resources.MAGIC_IMAGE, new byte[]{3});

        assertTrue(resources.isL10N("l10n"));
        assertTrue(resources.isTheme("theme"));
        assertTrue(resources.isFont("font"));
        assertTrue(resources.isAnimation("animation"));
        assertTrue(resources.isData("data"));
        assertTrue(resources.isUI("ui"));
        assertTrue(resources.isImage("image"));
        assertFalse(resources.isImage("missing"));
        assertEquals(Resources.MAGIC_UI, resources.getResourceType("ui"));

        resources.setResource("data", Resources.MAGIC_DATA, new byte[]{1, 2});
        assertNotNull(resources.getData("data"));
        assertNotNull(resources.getUi("ui"));

        Hashtable locales = new Hashtable();
        Hashtable strings = new Hashtable();
        strings.put("key", "value");
        locales.put("en", strings);
        resources.setResource("l10n", Resources.MAGIC_L10N, locales);
        assertEquals("value", resources.getL10N("l10n", "en").get("key"));

        UIBuilder builder = new UIBuilder();
        UIBuilder.setBlockAnalytics(true);
        assertTrue(UIBuilder.isBlockAnalytics());
        UIBuilder.registerCustomComponent("Custom", Label.class);
        assertTrue(UIBuilder.getComponentRegistry().containsKey("Custom"));

        builder.popNavigationStack();
        Hashtable formState = new Hashtable();
        formState.put(UIBuilder.FORM_STATE_KEY_NAME, "Main");
        builder.baseFormNavigationStack.addElement(formState);
        builder.setBackDestination("Main");
        assertTrue(builder.isBackCommandEnabled());
        assertTrue(builder.formNavigationStackDebug().contains("Main"));

        Container container = new Container();
        Label label = new Label("hello");
        label.setName("searchMe");
        container.addComponent(label);
        assertEquals(label, builder.findByName("searchMe", container));

        builder.setBackCommandEnabled(false);
        assertFalse(builder.isBackCommandEnabled());
        builder.setHomeForm("Home");
        assertEquals("Home", builder.getHomeForm());

        builder.setBackCommandEnabled(true);
        builder.setResourceFilePath("/tmp/resource.res");
        assertEquals("/tmp/resource.res", builder.getResourceFilePath());
        builder.setResourceFile(resources);
        assertEquals(resources, builder.fetchResourceFile());
    }

    private static class RecordingComponent extends Component {
        private final int x;
        private final int y;
        private final int w;
        private final int h;
        private final Dimension pref;
        int paintedX;
        int paintedY;
        int paintedW;
        int paintedH;

        RecordingComponent(int x, int y, int w, int h, int prefW, int prefH) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.pref = new Dimension(prefW, prefH);
            setWidth(w);
            setHeight(h);
        }

        public int getAbsoluteX() {
            return x;
        }

        public int getAbsoluteY() {
            return y;
        }

        public int getWidth() {
            return w;
        }

        public int getHeight() {
            return h;
        }

        protected Dimension calcPreferredSize() {
            return pref;
        }

        public void paint(Graphics g) {
            paintedX = getX();
            paintedY = getY();
            paintedW = getWidth();
            paintedH = getHeight();
        }
    }
}
