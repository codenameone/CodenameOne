package com.codename1.testing;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.location.LocationManager;
import com.codename1.media.Media;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.ui.Button;
import com.codename1.ui.Display;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.Stroke;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.TextSelection;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.util.ImageIO;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import com.codename1.util.AsyncResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Lightweight {@link CodenameOneImplementation} used by unit tests.  It provides deterministic,
 * in-memory implementations for the storage, file system, and networking APIs that are required by
 * tests exercising {@link NetworkManager} and related infrastructure.
 */
public class TestCodenameOneImplementation extends CodenameOneImplementation {
    private final Map<String, byte[]> storageEntries = new ConcurrentHashMap<>();
    private final Map<String, TestFile> fileSystem = new ConcurrentHashMap<>();
    private final Map<String, TestConnection> connections = new ConcurrentHashMap<>();

    private final TestFont defaultFont = new TestFont(8, 16);
    private int displayWidth = 1080;
    private int displayHeight = 1920;
    private int deviceDensity = Display.DENSITY_MEDIUM;
    private boolean portrait = true;
    private boolean touchDevice = true;
    private boolean timeoutSupported;
    private boolean timeoutInvoked;
    private int timeoutValue;
    private boolean translationSupported;
    private boolean translateInvoked;
    private boolean shapeSupported;
    private boolean drawShapeInvoked;
    private boolean fillShapeInvoked;
    private Shape lastClipShape;
    private Shape lastDrawShape;
    private Shape lastFillShape;
    private Stroke lastDrawStroke;
    private String[] accessPointIds = new String[0];
    private final Map<String, Integer> accessPointTypes = new HashMap<>();
    private final Map<String, String> accessPointNames = new HashMap<>();
    private String currentAccessPoint;
    private LocationManager locationManager;
    private L10NManager localizationManager;
    private ImageIO imageIO;
    private MediaRecorderBuilderHandler mediaRecorderBuilderHandler;
    private MediaRecorderHandler mediaRecorderHandler;
    private boolean animation;
    private String[] availableRecordingMimeTypes;
    private Media mediaRecorder;
    private boolean trueTypeSupported = true;
    private static TestCodenameOneImplementation instance;
    private Map<String, String> properties = new HashMap<>();
    private boolean blockCopyAndPaste;
    private PeerComponent browserComponent;
    private final List<String> browserExecuted = new ArrayList<>();
    private AsyncResource<Media> backgroundMediaAsync;
    private Media backgroundMedia;
    private Media media;
    private AsyncResource<Media> mediaAsync;
    private int startRemoteControlInvocations;
    private int stopRemoteControlInvocations;
    private boolean mutableImagesFast = true;
    private boolean nativeTitle;
    private int softkeyCount = 2;
    private boolean thirdSoftButton = false;
    private boolean nativeFontSchemeSupported = true;
    private Map<String, InputStream> resourceAsStreams = new HashMap<>();
    private Object nativeBrowserWindow;
    private final List<ActionListener> nativeBrowserWindowOnLoadListener = new ArrayList<>();
    private String nativeBrowserWindowTitle;
    private Dimension nativeBrowserWindowSize;
    private final List<ActionListener> nativeBrowserWindowCloseListener = new ArrayList<>();
    private boolean nativeBrowserWindowShowInvoked;
    private boolean nativeBrowserWindowCleanupInvoked;
    private boolean nativeBrowserWindowHideInvoked;
    private boolean nativeImageCacheSupported;
    private int initializeTextSelectionCount;
    private int deinitializeTextSelectionCount;
    private TextSelection lastInitializedTextSelection;
    private TextSelection lastDeinitializedTextSelection;
    private int copySelectionInvocations;
    private TextSelection lastCopiedTextSelection;
    private String lastCopiedText;
    private final Map<Object, HeavyButtonPeerState> heavyButtonPeers = new HashMap<Object, HeavyButtonPeerState>();
    private boolean requiresHeavyButton;


    public TestCodenameOneImplementation() {
        this(true);
        instance = this;
    }

    public static class HeavyButtonPeerState {
        private final List<ActionListener> listeners = new ArrayList<ActionListener>();
        private int x;
        private int y;
        private int width;
        private int height;
        private boolean initCalled;
        private boolean deinitCalled;
        private int updateCount;

        public List<ActionListener> getListeners() {
            return listeners;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public boolean isInitCalled() {
            return initCalled;
        }

        public boolean isDeinitCalled() {
            return deinitCalled;
        }

        public int getUpdateCount() {
            return updateCount;
        }
    }

    @Override
    public InputStream getResourceAsStream(Class cls, String resource) {
        return resourceAsStreams.get(resource);
    }

    public void putResource(String s, InputStream i) {
        resourceAsStreams.put(s, i);
    }

    public void setSupportsNativeImageCache(boolean supported) {
        nativeImageCacheSupported = supported;
    }

    public void resetTextSelectionTracking() {
        initializeTextSelectionCount = 0;
        deinitializeTextSelectionCount = 0;
        lastInitializedTextSelection = null;
        lastDeinitializedTextSelection = null;
        copySelectionInvocations = 0;
        lastCopiedTextSelection = null;
        lastCopiedText = null;
    }

    public int getInitializeTextSelectionCount() {
        return initializeTextSelectionCount;
    }

    public int getDeinitializeTextSelectionCount() {
        return deinitializeTextSelectionCount;
    }

    public TextSelection getLastInitializedTextSelection() {
        return lastInitializedTextSelection;
    }

    public TextSelection getLastDeinitializedTextSelection() {
        return lastDeinitializedTextSelection;
    }

    public int getCopySelectionInvocations() {
        return copySelectionInvocations;
    }

    public TextSelection getLastCopiedTextSelection() {
        return lastCopiedTextSelection;
    }

    public String getLastCopiedText() {
        return lastCopiedText;
    }

    public void setRequiresHeavyButton(boolean requiresHeavyButton) {
        this.requiresHeavyButton = requiresHeavyButton;
    }

    public void resetHeavyButtonTracking() {
        heavyButtonPeers.clear();
    }

    public HeavyButtonPeerState getHeavyButtonPeerState(Object peer) {
        return heavyButtonPeers.get(peer);
    }

    @Override
    public boolean isNativeTitle() {
        return nativeTitle;
    }

    public void setNativeTitle(boolean nativeTitle) {
        this.nativeTitle = nativeTitle;
    }

    @Override
    public boolean areMutableImagesFast() {
        return mutableImagesFast;
    }

    public void setMutableImagesFast(boolean mutableImagesFast) {
        this.mutableImagesFast = mutableImagesFast;
    }

    @Override
    public boolean supportsNativeImageCache() {
        return nativeImageCacheSupported;
    }

    @Override
    public AsyncResource<Media> createBackgroundMediaAsync(String uri) {
        return backgroundMediaAsync;
    }

    public void setBackgroundMediaAsync(AsyncResource<Media> backgroundMediaAsync) {
        this.backgroundMediaAsync = backgroundMediaAsync;
    }

    public void setBackgroundMedia(Media backgroundMedia) {
        this.backgroundMedia = backgroundMedia;
    }

    @Override
    public Media createBackgroundMedia(String uri) throws IOException {
        return backgroundMedia;
    }

    @Override
    public AsyncResource<Media> createMediaAsync(String uri, boolean video, Runnable onCompletion) {
        return mediaAsync;
    }

    @Override
    public Media createMedia(String uri, boolean isVideo, Runnable onCompletion) throws IOException {
        return media;
    }

    @Override
    public Media createMedia(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public void setMediaAsync(AsyncResource<Media> mediaAsync) {
        this.mediaAsync = mediaAsync;
    }

    @Override
    public void initializeTextSelection(TextSelection aThis) {
        initializeTextSelectionCount++;
        lastInitializedTextSelection = aThis;
    }

    @Override
    public void deinitializeTextSelection(TextSelection aThis) {
        deinitializeTextSelectionCount++;
        lastDeinitializedTextSelection = aThis;
    }

    @Override
    public void copySelectionToClipboard(TextSelection sel) {
        copySelectionInvocations++;
        lastCopiedTextSelection = sel;
        lastCopiedText = sel == null ? null : sel.getSelectionAsText();
    }

    @Override
    public void startRemoteControl() {
        startRemoteControlInvocations++;
    }

    @Override
    public void stopRemoteControl() {
        stopRemoteControlInvocations++;
    }

    @Override
    public Object createHeavyButton(Button aThis) {
        HeavyButtonPeerState state = new HeavyButtonPeerState();
        heavyButtonPeers.put(state, state);
        return state;
    }

    @Override
    public void addHeavyActionListener(Object peer, ActionListener l) {
        HeavyButtonPeerState state = heavyButtonPeers.get(peer);
        if (state != null) {
            state.getListeners().add(l);
        }
    }

    @Override
    public void removeHeavyActionListener(Object peer, ActionListener l) {
        HeavyButtonPeerState state = heavyButtonPeers.get(peer);
        if (state != null) {
            state.getListeners().remove(l);
        }
    }

    @Override
    public void updateHeavyButtonBounds(Object peer, int x, int y, int width, int height) {
        HeavyButtonPeerState state = heavyButtonPeers.get(peer);
        if (state != null) {
            state.x = x;
            state.y = y;
            state.width = width;
            state.height = height;
            state.updateCount++;
        }
    }

    @Override
    public void initHeavyButton(Object peer) {
        HeavyButtonPeerState state = heavyButtonPeers.get(peer);
        if (state != null) {
            state.initCalled = true;
        }
    }

    @Override
    public void deinitializeHeavyButton(Object peer) {
        HeavyButtonPeerState state = heavyButtonPeers.get(peer);
        if (state != null) {
            state.deinitCalled = true;
        }
    }

    @Override
    public boolean requiresHeavyButtonForCopyToClipboard() {
        return requiresHeavyButton;
    }

    public int getStartRemoteControlInvocations() {
        return startRemoteControlInvocations;
    }

    public int getStopRemoteControlInvocations() {
        return stopRemoteControlInvocations;
    }

    @Override
    public AsyncResource<Media> createMediaAsync(InputStream stream, String mimeType, Runnable onCompletion) {
        return mediaAsync;
    }

    @Override
    public Object createNativeBrowserWindow(String startURL) {
        return nativeBrowserWindow;
    }

    public void setNativeBrowserWindow(Object nativeBrowserWindow) {
        this.nativeBrowserWindow = nativeBrowserWindow;
    }

    @Override
    public void nativeBrowserWindowAddCloseListener(Object window, ActionListener l) {
        nativeBrowserWindowCloseListener.add(l);
    }

    @Override
    public void nativeBrowserWindowRemoveCloseListener(Object window, ActionListener l) {
        nativeBrowserWindowCloseListener.remove(l);
    }

    public List<ActionListener> getNativeBrowserWindowCloseListener() {
        return nativeBrowserWindowCloseListener;
    }

    @Override
    public void nativeBrowserWindowShow(Object window) {
        nativeBrowserWindowShowInvoked = true;
    }

    public boolean isNativeBrowserWindowShowInvoked() {
        return nativeBrowserWindowShowInvoked;
    }

    @Override
    public void nativeBrowserWindowCleanup(Object window) {
        nativeBrowserWindowCleanupInvoked = true;
    }

    @Override
    public void nativeBrowserWindowHide(Object window) {
        nativeBrowserWindowHideInvoked = true;
    }

    public boolean isNativeBrowserWindowCleanupInvoked() {
        return nativeBrowserWindowCleanupInvoked;
    }

    public boolean isNativeBrowserWindowHideInvoked() {
        return nativeBrowserWindowHideInvoked;
    }

    @Override
    public void addNativeBrowserWindowOnLoadListener(Object window, ActionListener l) {
        nativeBrowserWindowOnLoadListener.add(l);
    }

    @Override
    public void removeNativeBrowserWindowOnLoadListener(Object window, ActionListener l) {
        nativeBrowserWindowOnLoadListener.remove(l);
    }

    public List<ActionListener> getNativeBrowserWindowOnLoadListener() {
        return nativeBrowserWindowOnLoadListener;
    }

    @Override
    public void nativeBrowserWindowSetTitle(Object window, String title) {
        nativeBrowserWindowTitle = title;
    }

    public String getNativeBrowserWindowTitle() {
        return nativeBrowserWindowTitle;
    }

    @Override
    public void nativeBrowserWindowSetSize(Object window, int width, int height) {
        nativeBrowserWindowSize = new Dimension(width, height);
    }

    public Dimension getNativeBrowserWindowSize() {
        return nativeBrowserWindowSize;
    }

    @Override
    public PeerComponent createBrowserComponent(Object browserComponent) {
        return this.browserComponent;
    }

    public void setBrowserComponent(PeerComponent browserComponent) {
        this.browserComponent = browserComponent;
    }

    @Override
    public void browserExecute(PeerComponent browserPeer, String javaScript) {
        browserExecuted.add(javaScript);
    }

    @Override
    public String browserExecuteAndReturnString(PeerComponent internal, String javaScript) {
        browserExecuted.add(javaScript);
        return javaScript;
    }

    public List<String> getBrowserExecuted() {
        return browserExecuted;
    }

    @Override
    public void blockCopyPaste(boolean blockCopyPaste) {
        this.blockCopyAndPaste = blockCopyPaste;
    }

    public boolean isBlockCopyAndPaste() {
        return blockCopyAndPaste;
    }

    public static TestCodenameOneImplementation getInstance() {
        return instance;
    }

    @Override
    public String[] getAvailableRecordingMimeTypes() {
        return availableRecordingMimeTypes;
    }

    public void setAvailableRecordingMimeTypes(String[] availableRecordingMimeTypes) {
        this.availableRecordingMimeTypes = availableRecordingMimeTypes;
    }

    public void setAnimation(boolean animation) {
        this.animation = animation;
    }

    @Override
    public boolean isAnimation(Object nativeImage) {
        return animation;
    }

    @Override
    public boolean animateImage(Object nativeImage, long lastFrame) {
        return animation;
    }

    public TestCodenameOneImplementation(boolean timeoutSupported) {
        this.timeoutSupported = timeoutSupported;
    }

    public void setDisplaySize(int width, int height) {
        this.displayWidth = width;
        this.displayHeight = height;
        sizeChanged(width, height);
    }

    public void setDeviceDensity(int density) {
        this.deviceDensity = density;
    }

    public void setPortrait(boolean portrait) {
        this.portrait = portrait;
    }

    public void setTouchDevice(boolean touchDevice) {
        this.touchDevice = touchDevice;
    }

    public void setTimeoutSupported(boolean timeoutSupported) {
        this.timeoutSupported = timeoutSupported;
    }

    public void setAccessPoints(String[] ids, Map<String, Integer> types, Map<String, String> names) {
        this.accessPointIds = ids == null ? new String[0] : ids.clone();
        this.accessPointTypes.clear();
        if (types != null) {
            this.accessPointTypes.putAll(types);
        }
        this.accessPointNames.clear();
        if (names != null) {
            this.accessPointNames.putAll(names);
        }
    }

    @Override
    public LocationManager getLocationManager() {
        return locationManager;
    }

    public void setLocationManager(LocationManager locationManager) {
        this.locationManager = locationManager;
    }

    public void setLocalizationManager(L10NManager localizationManager) {
        this.localizationManager = localizationManager;
    }

    public void setImageIO(ImageIO imageIO) {
        this.imageIO = imageIO;
    }

    public void setMediaRecorderHandler(MediaRecorderHandler handler) {
        this.mediaRecorderHandler = handler;
    }

    public boolean wasTimeoutInvoked() {
        return timeoutInvoked;
    }

    public int getTimeoutValue() {
        return timeoutValue;
    }

    public void resetTimeoutTracking() {
        timeoutInvoked = false;
        timeoutValue = 0;
    }

    public void setTranslationSupported(boolean translationSupported) {
        this.translationSupported = translationSupported;
    }

    public boolean wasTranslateInvoked() {
        return translateInvoked;
    }

    public void resetTranslateTracking() {
        translateInvoked = false;
    }

    public void setShapeSupported(boolean shapeSupported) {
        this.shapeSupported = shapeSupported;
    }

    public boolean wasDrawShapeInvoked() {
        return drawShapeInvoked;
    }

    public boolean wasFillShapeInvoked() {
        return fillShapeInvoked;
    }

    public Shape getLastClipShape() {
        return lastClipShape;
    }

    public Shape getLastDrawShape() {
        return lastDrawShape;
    }

    public Shape getLastFillShape() {
        return lastFillShape;
    }

    public Stroke getLastDrawStroke() {
        return lastDrawStroke;
    }

    public void resetShapeTracking() {
        drawShapeInvoked = false;
        fillShapeInvoked = false;
        lastDrawShape = null;
        lastFillShape = null;
        lastDrawStroke = null;
    }

    public void resetClipTracking() {
        lastClipShape = null;
    }

    @Override
    public void resetAffine(Object nativeGraphics) {
    }

    @Override
    public void scale(Object nativeGraphics, float x, float y) {
    }


    @Override
    public void rotate(Object nativeGraphics, float angle, int pivotX, int pivotY) {
    }

    @Override
    public void shear(Object nativeGraphics, float x, float y) {
    }

    @Override
    public boolean isTransformSupported() {
        return true;
    }

    @Override
    public boolean isTransformSupported(Object graphics) {
        return true;
    }

    @Override
    public Object makeTransformIdentity() {
        return new TestTransform();
    }

    @Override
    public void setTransformIdentity(Object transform) {
        ((TestTransform) transform).setIdentity();
    }

    @Override
    public Object makeTransformTranslation(float translateX, float translateY, float translateZ) {
        TestTransform transform = new TestTransform();
        transform.setTranslation(translateX, translateY, translateZ);
        return transform;
    }

    @Override
    public void setTransformTranslation(Object transform, float translateX, float translateY, float translateZ) {
        ((TestTransform) transform).setTranslation(translateX, translateY, translateZ);
    }

    @Override
    public Object makeTransformScale(float scaleX, float scaleY, float scaleZ) {
        TestTransform transform = new TestTransform();
        transform.setScale(scaleX, scaleY, scaleZ);
        return transform;
    }

    @Override
    public void setTransformScale(Object transform, float scaleX, float scaleY, float scaleZ) {
        ((TestTransform) transform).setScale(scaleX, scaleY, scaleZ);
    }

    @Override
    public Object makeTransformRotation(float angle, float x, float y, float z) {
        TestTransform transform = new TestTransform();
        transform.setRotation(angle, x, y);
        return transform;
    }

    @Override
    public void setTransformRotation(Object transform, float angle, float x, float y, float z) {
        ((TestTransform) transform).setRotation(angle, x, y);
    }

    @Override
    public Object makeTransformAffine(double m00, double m10, double m01, double m11, double m02, double m12) {
        TestTransform transform = new TestTransform();
        transform.setAffine((float) m00, (float) m01, (float) m02, (float) m10, (float) m11, (float) m12);
        return transform;
    }

    @Override
    public Object makeTransformInverse(Object nativeTransform) {
        return ((TestTransform) nativeTransform).createInverse();
    }

    @Override
    public void setTransformInverse(Object nativeTransform) {
        ((TestTransform) nativeTransform).invert();
    }

    @Override
    public void concatenateTransform(Object left, Object right) {
        ((TestTransform) left).concatenate((TestTransform) right);
    }

    @Override
    public void copyTransform(Object src, Object dest) {
        ((TestTransform) dest).copyFrom((TestTransform) src);
    }

    @Override
    public boolean transformNativeEqualsImpl(Object t1, Object t2) {
        if (t1 == t2) {
            return true;
        }
        if (t1 == null || t2 == null) {
            return false;
        }
        return ((TestTransform) t1).equals((TestTransform) t2);
    }

    @Override
    public void transformPoint(Object nativeTransform, float[] in, float[] out) {
        if (nativeTransform == null) {
            int len = Math.min(in.length, out.length);
            System.arraycopy(in, 0, out, 0, len);
            return;
        }
        ((TestTransform) nativeTransform).transformPoint(in, out);
    }

    // -----------------------------------------------------------------
    // CodenameOneImplementation abstract methods
    // -----------------------------------------------------------------

    @Override
    public void init(Object m) {
    }

    @Override
    public int getDisplayWidth() {
        return displayWidth;
    }

    @Override
    public int getDisplayHeight() {
        return displayHeight;
    }

    @Override
    public void editString(com.codename1.ui.Component cmp, int maxSize, int constraint, String text, int initiatingKeycode) {
        if (cmp instanceof TextField) {
            ((TextField) cmp).setText(text);
        } else if (cmp instanceof TextArea) {
            ((TextArea) cmp).setText(text);
        }
    }

    @Override
    public void flushGraphics(int x, int y, int width, int height) {
    }

    @Override
    public void flushGraphics() {
    }

    @Override
    public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height) {
        TestImage img = (TestImage) nativeImage;
        img.getRGB(arr, offset, x, y, width, height);
    }

    @Override
    public Object createImage(int[] rgb, int width, int height) {
        return TestImage.fromRgb(rgb, width, height);
    }

    @Override
    public Object createImage(String path) throws IOException {
        TestFile file = fileSystem.get(path);
        if (file == null) {
            throw new IOException("Missing file " + path);
        }
        return TestImage.fromEncoded(file.content);
    }

    @Override
    public Object createImage(InputStream i) throws IOException {
        byte[] data = Util.readInputStream(i);
        return TestImage.fromEncoded(data);
    }

    @Override
    public Object createMutableImage(int width, int height, int fillColor) {
        return TestImage.mutable(width, height, fillColor);
    }

    @Override
    public Object createImage(byte[] bytes, int offset, int len) {
        byte[] data = Arrays.copyOfRange(bytes, offset, offset + len);
        return TestImage.fromEncoded(data);
    }

    @Override
    public int getImageWidth(Object i) {
        return ((TestImage) i).width;
    }

    @Override
    public int getImageHeight(Object i) {
        return ((TestImage) i).height;
    }

    @Override
    public Object scale(Object nativeImage, int width, int height) {
        TestImage img = (TestImage) nativeImage;
        return img.scale(width, height);
    }

    @Override
    public int getSoftkeyCount() {
        return softkeyCount;
    }

    public void setSoftkeyCount(int softkeyCount) {
        this.softkeyCount = softkeyCount;
    }

    @Override
    public boolean isThirdSoftButton() {
        return thirdSoftButton;
    }

    public void setThirdSoftButton(boolean thirdSoftButton) {
        this.thirdSoftButton = thirdSoftButton;
    }

    @Override
    public int[] getSoftkeyCode(int index) {
        return new int[]{index};
    }

    @Override
    public int getClearKeyCode() {
        return -8;
    }

    @Override
    public int getBackspaceKeyCode() {
        return -8;
    }

    @Override
    public int getBackKeyCode() {
        return -1;
    }

    @Override
    public int getGameAction(int keyCode) {
        return keyCode;
    }

    @Override
    public int getKeyCode(int gameAction) {
        return gameAction;
    }

    @Override
    public boolean isTouchDevice() {
        return touchDevice;
    }

    @Override
    public boolean isTranslationSupported() {
        return translationSupported;
    }

    @Override
    public void translate(Object graphics, int x, int y) {
        translateInvoked = true;
        TestGraphics g = (TestGraphics) graphics;
        g.translateX += x;
        g.translateY += y;
    }

    @Override
    public int getTranslateX(Object graphics) {
        return ((TestGraphics) graphics).translateX;
    }

    @Override
    public int getTranslateY(Object graphics) {
        return ((TestGraphics) graphics).translateY;
    }

    @Override
    public int getColor(Object graphics) {
        return ((TestGraphics) graphics).color;
    }

    @Override
    public void setColor(Object graphics, int RGB) {
        ((TestGraphics) graphics).color = RGB;
    }

    @Override
    public void setAlpha(Object graphics, int alpha) {
        ((TestGraphics) graphics).alpha = alpha;
    }

    @Override
    public int getAlpha(Object graphics) {
        return ((TestGraphics) graphics).alpha;
    }

    public Object getFont(Object graphics) {
        return ((TestGraphics) graphics).font;
    }

    @Override
    public void setNativeFont(Object graphics, Object font) {
        ((TestGraphics) graphics).font = (TestFont) font;
    }

    @Override
    public int getClipX(Object graphics) {
        return ((TestGraphics) graphics).clipX;
    }

    @Override
    public int getClipY(Object graphics) {
        return ((TestGraphics) graphics).clipY;
    }

    @Override
    public int getClipWidth(Object graphics) {
        return ((TestGraphics) graphics).clipWidth;
    }

    @Override
    public int getClipHeight(Object graphics) {
        return ((TestGraphics) graphics).clipHeight;
    }

    @Override
    public void setClip(Object graphics, Shape shape) {
        lastClipShape = shape;
        if (shape == null) {
            setClip(graphics, 0, 0, getDisplayWidth(), getDisplayHeight());
            return;
        }
        Rectangle bounds = shape.getBounds();
        setClip(graphics, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    @Override
    public void setClip(Object graphics, int x, int y, int width, int height) {
        TestGraphics g = (TestGraphics) graphics;
        g.clipX = x;
        g.clipY = y;
        g.clipWidth = width;
        g.clipHeight = height;
    }

    @Override
    public void clipRect(Object graphics, int x, int y, int width, int height) {
        TestGraphics g = (TestGraphics) graphics;
        int newX = Math.max(g.clipX, x);
        int newY = Math.max(g.clipY, y);
        int newW = Math.max(0, Math.min(g.clipX + g.clipWidth, x + width) - newX);
        int newH = Math.max(0, Math.min(g.clipY + g.clipHeight, y + height) - newY);
        g.clipX = newX;
        g.clipY = newY;
        g.clipWidth = newW;
        g.clipHeight = newH;
    }

    @Override
    public boolean isShapeSupported(Object nativeGraphics) {
        return shapeSupported;
    }

    @Override
    public void drawShape(Object graphics, Shape shape, Stroke stroke) {
        drawShapeInvoked = true;
        lastDrawShape = shape;
        lastDrawStroke = stroke;
    }

    @Override
    public void fillShape(Object graphics, Shape shape) {
        fillShapeInvoked = true;
        lastFillShape = shape;
    }

    @Override
    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
    }

    @Override
    public void fillRect(Object graphics, int x, int y, int width, int height) {
    }

    @Override
    public void drawRect(Object graphics, int x, int y, int width, int height) {
    }

    @Override
    public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
    }

    @Override
    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
    }

    @Override
    public Boolean canExecute(String url) {
        return url.startsWith("scheme:");
    }

    @Override
    public void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
    }

    @Override
    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
    }

    @Override
    public void drawString(Object graphics, String str, int x, int y) {
    }

    @Override
    public void drawImage(Object graphics, Object img, int x, int y) {
    }

    @Override
    public void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
    }

    @Override
    public Object getNativeGraphics() {
        return new TestGraphics(displayWidth, displayHeight);
    }

    @Override
    public Object getNativeGraphics(Object image) {
        TestImage img = (TestImage) image;
        if (img.graphics == null) {
            img.graphics = new TestGraphics(img.width, img.height);
        }
        return img.graphics;
    }

    @Override
    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        return font(nativeFont).charsWidth(ch, offset, length);
    }
    
    private TestFont font(Object nativeFont) {
        return nativeFont == null ? defaultFont : (TestFont) nativeFont;
    }

    @Override
    public int stringWidth(Object nativeFont, String str) {
        return font(nativeFont).stringWidth(str);
    }

    @Override
    public int charWidth(Object nativeFont, char ch) {
        return font(nativeFont).charWidth(ch);
    }

    @Override
    public int getHeight(Object nativeFont) {
        return font(nativeFont).height;
    }

    @Override
    public Object getDefaultFont() {
        return defaultFont;
    }

    @Override
    public Object createFont(int face, int style, int size) {
        return new TestFont(defaultFont.charWidth, defaultFont.height);
    }

    @Override
    public Object loadTrueTypeFont(String fontName, String fileName) {
        if(fontName != null && fontName.toLowerCase().contains("missing") ||
                fileName != null && fileName.toLowerCase().contains("missing")) {
            return null;
        }
        return new TestFont(defaultFont.charWidth, defaultFont.height);
    }

    @Override
    public Object deriveTrueTypeFont(Object font, float size, int weight) {
        return new TestFont(defaultFont.charWidth, defaultFont.height);
    }

    @Override
    public Object loadNativeFont(String lookup) {
        return new TestFont(defaultFont.charWidth, defaultFont.height);
    }

    @Override
    public boolean isTrueTypeSupported() {
        return trueTypeSupported;
    }

    public void setTrueTypeSupported(boolean trueTypeSupported) {
        this.trueTypeSupported = trueTypeSupported;
    }

    @Override
    public boolean isNativeFontSchemeSupported() {
        return nativeFontSchemeSupported;
    }

    public void setNativeFontSchemeSupported(boolean nativeFontSchemeSupported) {
        this.nativeFontSchemeSupported = nativeFontSchemeSupported;
    }

    @Override
    public Object connect(String url, boolean read, boolean write) throws IOException {
        TestConnection connection = connections.computeIfAbsent(url, TestConnection::new);
        connection.readRequested = read;
        connection.writeRequested = write;
        return connection;
    }

    public TestConnection getConnection(String url) {
        return connections.get(url);
    }

    public TestConnection createConnection(String url) {
        TestConnection connection = connections.computeIfAbsent(url, TestConnection::new);
        return connection;
    }

    public void clearConnections() {
        connections.clear();
    }

    @Override
    public void setHeader(Object connection, String key, String val) {
        ((TestConnection) connection).headers.put(key, val);
    }

    @Override
    public int getContentLength(Object connection) {
        return ((TestConnection) connection).contentLength;
    }

    @Override
    public OutputStream openOutputStream(Object connection) {
        return ((TestConnection) connection).openOutputStream();
    }

    @Override
    public OutputStream openOutputStream(Object connection, int offset) {
        TestConnection conn = (TestConnection) connection;
        conn.outputOffset = offset;
        return conn.openOutputStream();
    }

    @Override
    public InputStream openInputStream(Object connection) {
        return ((TestConnection) connection).openInputStream();
    }

    @Override
    public void setPostRequest(Object connection, boolean p) {
        ((TestConnection) connection).postRequest = p;
    }

    @Override
    public int getResponseCode(Object connection) {
        return ((TestConnection) connection).responseCode;
    }

    @Override
    public String getResponseMessage(Object connection) {
        return ((TestConnection) connection).responseMessage;
    }

    @Override
    public String getHeaderField(String name, Object connection) {
        return ((TestConnection) connection).headers.get(name);
    }

    @Override
    public String[] getHeaderFieldNames(Object connection) {
        return ((TestConnection) connection).headers.keySet().toArray(new String[0]);
    }

    @Override
    public String[] getHeaderFields(String name, Object connection) {
        TestConnection conn = (TestConnection) connection;
        List<String> values = conn.multiHeaders.get(name);
        if (values != null) {
            return values.toArray(new String[0]);
        }
        String single = conn.headers.get(name);
        if (single == null) {
            return null;
        }
        return new String[]{single};
    }

    @Override
    public void deleteStorageFile(String name) {
        storageEntries.remove(name);
    }

    public void putStorageEntry(String name, byte[] data) {
        if (data == null) {
            storageEntries.remove(name);
        } else {
            storageEntries.put(name, data.clone());
        }
    }

    @Override
    public OutputStream createStorageOutputStream(String name) {
        return new StorageOutput(name);
    }

    @Override
    public InputStream createStorageInputStream(String name) throws IOException {
        byte[] data = storageEntries.get(name);
        if (data == null) {
            throw new IOException("Missing storage entry " + name);
        }
        return new ByteArrayInputStream(data);
    }

    @Override
    public boolean storageFileExists(String name) {
        return storageEntries.containsKey(name);
    }

    @Override
    public int getStorageEntrySize(String name) {
        byte[] data = storageEntries.get(name);
        return data == null ? -1 : data.length;
    }

    @Override
    public String[] listStorageEntries() {
        return storageEntries.keySet().toArray(new String[0]);
    }

    @Override
    public String[] listFilesystemRoots() {
        return new String[]{"/"};
    }

    @Override
    public String[] listFiles(String directory) {
        return fileSystem.keySet().stream()
                .filter(path -> path.startsWith(directory))
                .toArray(String[]::new);
    }

    @Override
    public long getRootSizeBytes(String root) {
        return 1024 * 1024;
    }

    @Override
    public long getRootAvailableSpace(String root) {
        return 1024 * 512;
    }

    @Override
    public void mkdir(String directory) {
        fileSystem.putIfAbsent(directory, TestFile.directory());
    }

    @Override
    public void deleteFile(String file) {
        fileSystem.remove(file);
    }

    @Override
    public boolean isHidden(String file) {
        return false;
    }

    @Override
    public void setHidden(String file, boolean h) {
    }

    @Override
    public long getFileLength(String file) {
        TestFile f = fileSystem.get(file);
        return f == null ? 0 : f.content.length;
    }

    @Override
    public boolean isDirectory(String file) {
        TestFile f = fileSystem.get(file);
        return f != null && f.directory;
    }

    @Override
    public boolean exists(String file) {
        return fileSystem.containsKey(file);
    }

    @Override
    public void rename(String file, String newName) {
        TestFile f = fileSystem.remove(file);
        if (f != null) {
            fileSystem.put(newName, f);
        }
    }

    @Override
    public char getFileSystemSeparator() {
        return '/';
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    public void putProperty(String key, String value) {
        properties.put(key, value);
    }


    @Override
    public String getPlatformName() {
        return "test";
    }

    @Override
    public L10NManager getLocalizationManager() {
        if (localizationManager == null) {
            localizationManager = new L10NManager("en", "US") {
            };
        }
        return localizationManager;
    }

    // -----------------------------------------------------------------
    // Optional overrides for tests
    // -----------------------------------------------------------------

    @Override
    public boolean isTimeoutSupported() {
        return timeoutSupported;
    }

    @Override
    public void setTimeout(int time) {
        timeoutInvoked = true;
        timeoutValue = time;
    }

    @Override
    public boolean isPortrait() {
        return portrait;
    }

    @Override
    public int getDeviceDensity() {
        return deviceDensity;
    }

    @Override
    public int convertToPixels(int dipCount, boolean horizontal) {
        return dipCount;
    }

    @Override
    public boolean isAPSupported() {
        return accessPointIds.length > 0;
    }

    @Override
    public String[] getAPIds() {
        return accessPointIds.clone();
    }

    @Override
    public int getAPType(String id) {
        return accessPointTypes.getOrDefault(id, NetworkManager.ACCESS_POINT_TYPE_UNKNOWN);
    }

    @Override
    public String getAPName(String id) {
        return accessPointNames.get(id);
    }

    @Override
    public String getCurrentAccessPoint() {
        return currentAccessPoint;
    }

    @Override
    public void setCurrentAccessPoint(String id) {
        currentAccessPoint = id;
    }

    @Override
    public ImageIO getImageIO() {
        return imageIO;
    }

    public void setMediaRecorder(Media mediaRecorder) {
        this.mediaRecorder = mediaRecorder;
    }

    @Override
    public Media createMediaRecorder(MediaRecorderBuilder builder) {
        return mediaRecorder;
    }

    @Override
    public Media createMediaRecorder(String path, String mime) {
        return mediaRecorder;
    }

    @Override
    public void clearStorage() {
        storageEntries.clear();
    }

    @Override
    public void flushStorageCache() {
    }

    @Override
    public void setStorageData(Object data) {
    }

    @Override
    public void closingOutput(OutputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void cleanup(Object obj) {
    }

    @Override
    public void addConnectionToQueue(ConnectionRequest r) {
        super.addConnectionToQueue(r);
    }

    @Override
    public void startThread(String name, Runnable r) {
        if (r == null) {
            return;
        }

        Thread worker = new Thread(r, name == null ? "CN1-TestThread" : name);
        worker.setDaemon(true);
        worker.start();
    }

    // -----------------------------------------------------------------
    // Helper classes
    // -----------------------------------------------------------------

    public interface MediaRecorderBuilderHandler {
        Media create(MediaRecorderBuilder builder);
    }

    public interface MediaRecorderHandler {
        Media create(String path, String mime);
    }

    private final class StorageOutput extends ByteArrayOutputStream {
        private final String name;

        StorageOutput(String name) {
            this.name = name;
        }

        @Override
        public void close() throws IOException {
            super.close();
            storageEntries.put(name, toByteArray());
        }
    }

    public static final class TestGraphics {
        int color = 0x000000;
        int alpha = 0xff;
        int clipX;
        int clipY;
        int clipWidth;
        int clipHeight;
        int translateX;
        int translateY;
        TestFont font;

        TestGraphics(int width, int height) {
            this.clipWidth = width;
            this.clipHeight = height;
        }
    }

    private static final class TestTransform {
        private float m00;
        private float m01;
        private float m02;
        private float m10;
        private float m11;
        private float m12;
        private float m20;
        private float m21;
        private float m22;
        private float translateZ;

        TestTransform() {
            setIdentity();
        }

        void setIdentity() {
            m00 = 1f;
            m01 = 0f;
            m02 = 0f;
            m10 = 0f;
            m11 = 1f;
            m12 = 0f;
            m20 = 0f;
            m21 = 0f;
            m22 = 1f;
            translateZ = 0f;
        }

        void setTranslation(float tx, float ty, float tz) {
            setIdentity();
            m02 = tx;
            m12 = ty;
            translateZ = tz;
        }

        void setScale(float sx, float sy, float sz) {
            setIdentity();
            m00 = sx;
            m11 = sy;
            m22 = sz;
        }

        void setRotation(float angle, float px, float py) {
            setIdentity();
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            m00 = cos;
            m01 = -sin;
            m10 = sin;
            m11 = cos;
            m02 = px - px * cos + py * sin;
            m12 = py - px * sin - py * cos;
        }

        void setAffine(float nm00, float nm01, float nm02, float nm10, float nm11, float nm12) {
            m00 = nm00;
            m01 = nm01;
            m02 = nm02;
            m10 = nm10;
            m11 = nm11;
            m12 = nm12;
            m20 = 0f;
            m21 = 0f;
            m22 = 1f;
            translateZ = 0f;
        }

        void copyFrom(TestTransform other) {
            m00 = other.m00;
            m01 = other.m01;
            m02 = other.m02;
            m10 = other.m10;
            m11 = other.m11;
            m12 = other.m12;
            m20 = other.m20;
            m21 = other.m21;
            m22 = other.m22;
            translateZ = other.translateZ;
        }

        TestTransform createInverse() {
            TestTransform inverse = new TestTransform();
            inverse.copyFrom(this);
            inverse.invert();
            return inverse;
        }

        void invert() {
            float det = m00 * m11 - m01 * m10;
            if (Math.abs(det) < 1.0e-6f) {
                setIdentity();
                return;
            }
            float invDet = 1f / det;
            float nm00 = m11 * invDet;
            float nm01 = -m01 * invDet;
            float nm02 = (m01 * m12 - m11 * m02) * invDet;
            float nm10 = -m10 * invDet;
            float nm11 = m00 * invDet;
            float nm12 = (m10 * m02 - m00 * m12) * invDet;
            m00 = nm00;
            m01 = nm01;
            m02 = nm02;
            m10 = nm10;
            m11 = nm11;
            m12 = nm12;
            translateZ = -translateZ;
        }

        void concatenate(TestTransform right) {
            float nm00 = m00 * right.m00 + m01 * right.m10;
            float nm01 = m00 * right.m01 + m01 * right.m11;
            float nm02 = m00 * right.m02 + m01 * right.m12 + m02;
            float nm10 = m10 * right.m00 + m11 * right.m10;
            float nm11 = m10 * right.m01 + m11 * right.m11;
            float nm12 = m10 * right.m02 + m11 * right.m12 + m12;
            float nm20 = m20 * right.m00 + m21 * right.m10 + m22 * right.m20;
            float nm21 = m20 * right.m01 + m21 * right.m11 + m22 * right.m21;
            float nm22 = m20 * right.m02 + m21 * right.m12 + m22 * right.m22;
            m00 = nm00;
            m01 = nm01;
            m02 = nm02;
            m10 = nm10;
            m11 = nm11;
            m12 = nm12;
            m20 = nm20;
            m21 = nm21;
            m22 = nm22;
            translateZ = translateZ + right.translateZ;
        }

        void transformPoint(float[] in, float[] out) {
            float x = in[0];
            float y = in[1];
            out[0] = m00 * x + m01 * y + m02;
            out[1] = m10 * x + m11 * y + m12;
            if (in.length > 2 && out.length > 2) {
                float z = in[2];
                out[2] = m22 * z + translateZ;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TestTransform)) {
                return false;
            }
            TestTransform other = (TestTransform) obj;
            return Float.compare(m00, other.m00) == 0
                    && Float.compare(m01, other.m01) == 0
                    && Float.compare(m02, other.m02) == 0
                    && Float.compare(m10, other.m10) == 0
                    && Float.compare(m11, other.m11) == 0
                    && Float.compare(m12, other.m12) == 0
                    && Float.compare(m20, other.m20) == 0
                    && Float.compare(m21, other.m21) == 0
                    && Float.compare(m22, other.m22) == 0
                    && Float.compare(translateZ, other.translateZ) == 0;
        }

        @Override
        public int hashCode() {
            int result = Float.floatToIntBits(m00);
            result = 31 * result + Float.floatToIntBits(m01);
            result = 31 * result + Float.floatToIntBits(m02);
            result = 31 * result + Float.floatToIntBits(m10);
            result = 31 * result + Float.floatToIntBits(m11);
            result = 31 * result + Float.floatToIntBits(m12);
            result = 31 * result + Float.floatToIntBits(m20);
            result = 31 * result + Float.floatToIntBits(m21);
            result = 31 * result + Float.floatToIntBits(m22);
            result = 31 * result + Float.floatToIntBits(translateZ);
            return result;
        }
    }

    public static final class TestFont {
        final int charWidth;
        final int height;

        TestFont(int charWidth, int height) {
            this.charWidth = charWidth;
            this.height = height;
        }

        int stringWidth(String text) {
            if (text == null) {
                return 0;
            }
            return text.length() * charWidth;
        }

        int charsWidth(char[] chars, int offset, int length) {
            if (chars == null || length <= 0) {
                return 0;
            }
            return Math.max(0, length) * charWidth;
        }

        int charWidth(char c) {
            return charWidth;
        }
    }

    public static final class TestImage {
        final int width;
        final int height;
        final int[] argb;
        TestGraphics graphics;

        private TestImage(int width, int height, int[] argb) {
            this.width = width;
            this.height = height;
            this.argb = argb;
        }

        static TestImage fromRgb(int[] rgb, int width, int height) {
            int[] data = Arrays.copyOf(rgb, rgb.length);
            return new TestImage(width, height, data);
        }

        static TestImage mutable(int width, int height, int fillColor) {
            int[] data = new int[Math.max(1, width * height)];
            Arrays.fill(data, fillColor);
            return new TestImage(width, height, data);
        }

        static TestImage fromEncoded(byte[] encoded) {
            int width = encoded.length > 0 ? Math.max(1, encoded[0]) : 1;
            int height = encoded.length > 1 ? Math.max(1, encoded[1]) : 1;
            int[] data = new int[Math.max(1, width * height)];
            Arrays.fill(data, 0xff000000);
            return new TestImage(width, height, data);
        }

        void getRGB(int[] out, int offset, int x, int y, int width, int height) {
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    int src = (y + row) * this.width + (x + col);
                    int dst = offset + row * width + col;
                    if (src >= 0 && src < argb.length && dst < out.length) {
                        out[dst] = argb[src];
                    }
                }
            }
        }

        TestImage scale(int width, int height) {
            int[] data = new int[Math.max(1, width * height)];
            Arrays.fill(data, 0xff000000);
            return new TestImage(width, height, data);
        }
    }

    public static final class TestConnection {
        final String url;
        final Map<String, String> headers = new HashMap<>();
        final Map<String, List<String>> multiHeaders = new HashMap<>();
        byte[] inputData;
        ByteArrayOutputStream output;
        boolean readRequested;
        boolean writeRequested;
        boolean postRequest;
        int responseCode = 200;
        String responseMessage = "OK";
        int contentLength;
        int outputOffset;

        TestConnection(String url) {
            this.url = url;
        }

        InputStream openInputStream() {
            byte[] data = inputData == null ? new byte[0] : inputData;
            return new ByteArrayInputStream(Arrays.copyOf(data, data.length));
        }

        OutputStream openOutputStream() {
            if (output == null) {
                output = new ByteArrayOutputStream();
            }
            return output;
        }

        public String getUrl() {
            return url;
        }

        public Map<String, String> getHeaders() {
            return new HashMap<String, String>(headers);
        }

        public boolean isReadRequested() {
            return readRequested;
        }

        public boolean isWriteRequested() {
            return writeRequested;
        }

        public boolean isPostRequest() {
            return postRequest;
        }

        public void setPostRequest(boolean postRequest) {
            this.postRequest = postRequest;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }

        public String getResponseMessage() {
            return responseMessage;
        }

        public void setResponseMessage(String responseMessage) {
            this.responseMessage = responseMessage;
        }

        public void setInputData(byte[] inputData) {
            this.inputData = inputData == null ? null : Arrays.copyOf(inputData, inputData.length);
        }

        public byte[] getInputData() {
            return inputData == null ? null : Arrays.copyOf(inputData, inputData.length);
        }

        public byte[] getOutputData() {
            if (output == null) {
                return new byte[0];
            }
            return output.toByteArray();
        }

        public void setContentLength(int contentLength) {
            this.contentLength = contentLength;
        }
    }

    public static final class TestFile {
        final boolean directory;
        final byte[] content;

        TestFile(boolean directory, byte[] content) {
            this.directory = directory;
            this.content = content == null ? new byte[0] : content;
        }

        static TestFile file(byte[] content) {
            return new TestFile(false, content);
        }

        static TestFile directory() {
            return new TestFile(true, new byte[0]);
        }
    }
}
