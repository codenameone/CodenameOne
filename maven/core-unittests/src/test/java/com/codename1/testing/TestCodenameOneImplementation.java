package com.codename1.testing;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.l10n.L10NManager;
import com.codename1.location.LocationManager;
import com.codename1.media.Media;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Stroke;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.util.ImageIO;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    public TestCodenameOneImplementation() {
        this(true);
    }

    public TestCodenameOneImplementation(boolean timeoutSupported) {
        this.timeoutSupported = timeoutSupported;
    }

    public void setDisplaySize(int width, int height) {
        this.displayWidth = width;
        this.displayHeight = height;
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

    public void setLocationManager(LocationManager locationManager) {
        this.locationManager = locationManager;
    }

    public void setLocalizationManager(L10NManager localizationManager) {
        this.localizationManager = localizationManager;
    }

    public void setImageIO(ImageIO imageIO) {
        this.imageIO = imageIO;
    }

    public void setMediaRecorderBuilderHandler(MediaRecorderBuilderHandler handler) {
        this.mediaRecorderBuilderHandler = handler;
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
        return 2;
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
        return ((TestFont) nativeFont).charsWidth(ch, offset, length);
    }

    @Override
    public int stringWidth(Object nativeFont, String str) {
        return ((TestFont) nativeFont).stringWidth(str);
    }

    @Override
    public int charWidth(Object nativeFont, char ch) {
        return ((TestFont) nativeFont).charWidth(ch);
    }

    @Override
    public int getHeight(Object nativeFont) {
        return ((TestFont) nativeFont).height;
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
    public Object connect(String url, boolean read, boolean write) throws IOException {
        TestConnection connection = connections.computeIfAbsent(url, TestConnection::new);
        connection.readRequested = read;
        connection.writeRequested = write;
        return connection;
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
    public String getPlatformName() {
        return "test";
    }

    @Override
    public L10NManager getLocalizationManager() {
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
    public Form getCurrentForm() {
        return null;
    }

    @Override
    public ImageIO getImageIO() {
        return imageIO;
    }

    @Override
    public Media createMediaRecorder(MediaRecorderBuilder builder) {
        return mediaRecorderBuilderHandler == null ? null : mediaRecorderBuilderHandler.create(builder);
    }

    @Override
    public Media createMediaRecorder(String path, String mime) {
        return mediaRecorderHandler == null ? null : mediaRecorderHandler.create(path, mime);
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
        if (r != null) {
            r.run();
        }
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
