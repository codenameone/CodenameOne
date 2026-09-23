/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.flutter.widgets;

import com.codename1.flutter.BoxFit;
import com.codename1.flutter.FlutterAssets;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.io.Log;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Label;
import com.codename1.ui.URLImage;

import java.io.InputStream;

/**
 * Leaf render box for {@link Image} (UIID "FlutterImage").
 *
 * <ul>
 *   <li><b>asset</b>: loaded from {@code /assets/&lt;name&gt;} on the
 *       classpath (the app build copies {@code src/main/flutter/assets}
 *       there) as an EncodedImage.</li>
 *   <li><b>network</b>: downloaded (or read from the storage cache) and
 *       decoded at its natural size, like an asset, so BoxFit sees the real
 *       aspect ratio; nothing is shown until the bytes arrive.</li>
 * </ul>
 *
 * <p>Sizing: explicit width/height tighten the incoming constraints; the
 * natural image size fills in unconstrained axes. BoxFit maps onto CN1 image
 * scaling at position time: {@code fill} stretches, {@code contain} (the
 * default) scales the larger dimension to fit, {@code cover} uses
 * {@code Image.fill} (scale smaller dimension, center-crop approximation),
 * {@code fitWidth}/{@code fitHeight} scale one axis, {@code none} keeps the
 * natural size.</p>
 */
public class ImageRenderElement extends RenderElement {

    private com.codename1.ui.Image img;
    private String loadedSource;
    /** Device pixels per logical pixel in the loaded asset file (1, 2, 3...). */
    private double assetRatio = 1;

    public ImageRenderElement(Image widget) {
        super(widget);
    }

    private Image image() {
        return (Image) widget();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        FittedImage l = new FittedImage();
        l.getAllStyles().setPadding(0, 0, 0, 0);
        l.getAllStyles().setMargin(0, 0, 0, 0);
        loadImage(l);
        return l;
    }

    @Override
    protected void updateComponent(Component c) {
        loadImage((Label) c);
    }

    private void loadImage(Label l) {
        String source = image().sourceKey();
        if (source.equals(loadedSource)) {
            return;
        }
        loadedSource = source;
        img = null;
        assetRatio = 1;
        try {
            if (image().getAssetName() != null) {
                FlutterAssets.Resolved res = FlutterAssets.open(getClass(), image().getAssetName());
                if (res == null) {
                    Log.p("Flutter runtime: asset image not found: " + image().getAssetName()
                            + " (resource " + FlutterAssets.resourceName(image().getAssetName()) + ")");
                } else {
                    img = downsample(encodedWithKnownSize(res.stream()));
                    assetRatio = res.ratio();
                }
            } else if (image().getMemoryBytes() != null) {
                // MemoryImage: already encoded bytes in hand, decoded the way an
                // asset's are -- same size bookkeeping, same downsampling.
                img = downsample(encodedWithKnownSize(
                        new java.io.ByteArrayInputStream(image().getMemoryBytes())));
                // The provider's scale is exactly what an asset variant's ratio is:
                // encoded pixels per logical pixel.
                assetRatio = image().getMemoryScale();
            } else if (image().getUrl() != null) {
                // Fetched and decoded at its NATURAL size, then applied like an asset.
                // URLImage with RESIZE_SCALE resampled the download to the placeholder's
                // exact box -- a square 100lp one when no size was given -- and this
                // element read the placeholder's size as the image's own, so BoxFit
                // contain/cover could never recover the real aspect ratio. Until the
                // bytes arrive there is simply no image, as in Flutter.
                fetchNetworkImage(l, source, image().getUrl(), image().getHeaders());
            }
        } catch (Exception err) {
            Log.p("Flutter runtime: could not load image " + source);
            Log.e(err);
        }
        // The NATURAL size is remembered here, at full resolution, because the
        // picture may later be re-decoded smaller to fit its box (see
        // shrinkToBox) and the size this box reports must not change when that
        // happens -- a natural size that shrank would shrink the box, which
        // would shrink the picture again.
        applyLoaded(l);
    }

    /** Takes the current {@code img} as the natural size and hands it to the component. */
    private void applyLoaded(Label l) {
        naturalW = img == null ? 0 : img.getWidth();
        naturalH = img == null ? 0 : img.getHeight();
        if (l instanceof FittedImage) {
            // The SOURCE, not a scaled copy: FittedImage draws it into its box
            // at paint time. See fitNow.
            ((FittedImage) l).setSource(img);
            l.setIcon(null);
        } else {
            l.setIcon(img);
        }
    }

    /// The decoded size of the artwork as it was loaded; see loadImage.
    private int naturalW;
    private int naturalH;

    /**
     * Honours a {@code ResizeImage}: keep the picture at the size it asked to
     * be decoded at, and let the full-resolution decode go.
     *
     * <p>The scaled copy is the ONLY thing retained — the EncodedImage it came
     * from, its bytes and its full-size bitmap all become garbage as this
     * method returns. That is the point: the gallery's flight thumbnails ask
     * for 80x80 out of artwork that is a thousand times the area, and holding
     * the big one to draw the small one is the single largest piece of resident
     * memory an image-heavy screen carries.</p>
     */
    /// Builds an EncodedImage that already knows its own size.
    ///
    /// EncodedImage.create(stream) leaves width and height unknown, and
    /// getWidth() then answers by DECODING the picture -- see
    /// EncodedImage.getWidth. loadImage asks for the natural size of every image
    /// it mounts, so every asset was fully decoded during the first build merely
    /// to be measured. Profiled interpreted on the desktop port, that single
    /// getWidth() chain was 39.7% of start-up.
    ///
    /// The size is in the file's header, so read it from there -- see pngSize
    /// for the PNG side -- and hand it to the four-argument create() -- whose own
    /// documentation exists for exactly this ("doesn't need to actually traverse
    /// the pixels of an image to find out details about it"). The decode then
    /// happens when something actually paints the image, and an image that never
    /// becomes visible is never decoded at all.
    ///
    /// Anything whose header is not recognised falls back to the old behaviour,
    /// so an unsupported format is slower but never wrong.
    /**
     * The storage key for a network image: a SHA-256 over the URL and the sorted request
     * headers. It was the URL's String.hashCode (plus the header map's), and URLImage
     * serves whatever is already stored or pending under a key -- two URLs with the same
     * 32-bit hash, which is easy to construct, got each other's picture, including one
     * fetched with another user's credentials.
     */
    static String storageKey(String url, java.util.Map<?, ?> headers) {
        StringBuilder sb = new StringBuilder(url);
        if (headers != null && !headers.isEmpty()) {
            java.util.List<String> lines = new java.util.ArrayList<String>();
            for (java.util.Map.Entry<?, ?> e : headers.entrySet()) {
                lines.add(String.valueOf(e.getKey()) + ": " + String.valueOf(e.getValue()));
            }
            java.util.Collections.sort(lines);
            for (String line : lines) {
                sb.append('\n').append(line);
            }
        }
        byte[] digest;
        try {
            digest = com.codename1.security.Hash.sha256(sb.toString().getBytes("UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            digest = com.codename1.security.Hash.sha256(sb.toString().getBytes());
        }
        // A table rather than Character.forDigit, which ParparVM's JavaAPI lacks.
        String digits = "0123456789abcdef";
        StringBuilder hex = new StringBuilder("flutter-img-");
        for (int i = 0; i < digest.length; i++) {
            int b = digest[i] & 0xff;
            hex.append(digits.charAt(b >> 4)).append(digits.charAt(b & 0xf));
        }
        return hex.toString();
    }

    /** Loads a network image from the storage cache, or downloads and caches it. */
    private void fetchNetworkImage(final Label l, final String source, String url,
            final java.util.Map<?, ?> headers) {
        final String key = storageKey(url, headers);
        final com.codename1.io.Storage storage = com.codename1.io.Storage.getInstance();
        if (storage.exists(key)) {
            java.io.InputStream in = null;
            try {
                in = storage.createInputStream(key);
                img = downsample(encodedWithKnownSize(in));
                return;
            } catch (java.io.IOException e) {
                storage.deleteStorageFile(key);   // a damaged cache entry: fetch again
            } finally {
                com.codename1.io.Util.cleanup(in);
            }
        }
        final com.codename1.io.ConnectionRequest req = new com.codename1.io.ConnectionRequest(url, false);
        req.setDuplicateSupported(true);
        if (headers != null) {
            for (java.util.Map.Entry<?, ?> e : headers.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    req.addRequestHeader(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                }
            }
        }
        req.addResponseListener(new com.codename1.ui.events.ActionListener<com.codename1.io.NetworkEvent>() {
            @Override
            public void actionPerformed(com.codename1.io.NetworkEvent evt) {
                final byte[] data = req.getResponseData();
                if (data == null || data.length == 0) {
                    return;
                }
                com.codename1.ui.CN.callSerially(new Runnable() {
                    @Override
                    public void run() {
                        if (!source.equals(loadedSource) || !isMounted()) {
                            return;   // the widget moved on to another image
                        }
                        try {
                            img = downsample(encodedWithKnownSize(new java.io.ByteArrayInputStream(data)));
                        } catch (java.io.IOException e) {
                            Log.p("Flutter runtime: could not decode image " + source);
                            return;
                        }
                        java.io.OutputStream out = null;
                        try {
                            out = storage.createOutputStream(key);
                            out.write(data);
                        } catch (java.io.IOException e) {
                            // not cached; it will simply be fetched again next time
                        } finally {
                            com.codename1.io.Util.cleanup(out);
                        }
                        applyLoaded(l);
                        markNeedsLayout();
                    }
                });
            }
        });
        com.codename1.io.NetworkManager.getInstance().addToQueue(req);
    }

    private static com.codename1.ui.Image encodedWithKnownSize(java.io.InputStream in)
            throws java.io.IOException {
        byte[] data = com.codename1.io.Util.readInputStream(in);
        int w = -1, h = -1;
        boolean opaque = false;
        if (data.length > 24 && (data[0] & 0xff) == 0x89 && data[1] == 'P'
                && data[2] == 'N' && data[3] == 'G') {
            int[] size = pngSize(data);
            w = size[0];
            h = size[1];
        } else if (data.length > 10 && (data[0] & 0xff) == 0xFF && (data[1] & 0xff) == 0xD8) {
            // JPEG: walk the marker segments to the frame header, which carries
            // the dimensions. JPEG has no alpha channel, hence opaque.
            opaque = true;
            int i = 2;
            while (i + 9 < data.length) {
                if ((data[i] & 0xff) != 0xFF) {
                    i++;
                    continue;
                }
                int marker = data[i + 1] & 0xff;
                int len = ((data[i + 2] & 0xff) << 8) | (data[i + 3] & 0xff);
                // SOF0-SOF15, excluding the four that are not frame headers.
                if (marker >= 0xC0 && marker <= 0xCF
                        && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                    h = ((data[i + 5] & 0xff) << 8) | (data[i + 6] & 0xff);
                    w = ((data[i + 7] & 0xff) << 8) | (data[i + 8] & 0xff);
                    break;
                }
                if (len <= 0) {
                    break;
                }
                i += 2 + len;
            }
        } else if (data.length > 10 && data[0] == 'G' && data[1] == 'I' && data[2] == 'F') {
            // GIF: logical screen width/height, little-endian, straight after
            // the six-byte signature.
            w = (data[6] & 0xff) | ((data[7] & 0xff) << 8);
            h = (data[8] & 0xff) | ((data[9] & 0xff) << 8);
        }
        if (w > 0 && h > 0) {
            return EncodedImage.create(data, w, h, opaque);
        }
        return EncodedImage.create(data);
    }

    /**
     * A PNG's pixel size, read out of its IHDR chunk.
     *
     * <p>IHDR is the first chunk in a standards-conforming PNG, and reading its
     * payload at a fixed offset is what this used to do. It is NOT first in the
     * PNGs an iOS app actually ships: Xcode rewrites every bundled PNG into
     * Apple's CgBI form, which puts a four-byte {@code CgBI} chunk in front of
     * it. A fixed offset then reads that chunk's payload as the width and its
     * checksum as the height, and the picture is drawn to a garbage aspect --
     * the gallery's category icons came out squashed to half their height on
     * device while being pixel-exact in every desktop sweep, because the desktop
     * copy of the same asset is an ordinary PNG.</p>
     *
     * @return {@code {width, height}}, or {@code {-1, -1}} when no IHDR is found
     */
    static int[] pngSize(byte[] data) {
        int off = 8;
        while (off + 12 <= data.length) {
            int len = be32(data, off);
            if (len < 0) {
                break;
            }
            if (data[off + 4] == 'I' && data[off + 5] == 'H'
                    && data[off + 6] == 'D' && data[off + 7] == 'R') {
                if (off + 16 > data.length) {
                    break;
                }
                return new int[] {be32(data, off + 8), be32(data, off + 12)};
            }
            // length + the 4-byte type + the 4-byte CRC
            long next = (long) off + 12L + (long) len;
            if (next <= off || next > data.length) {
                break;
            }
            off = (int) next;
        }
        return new int[] {-1, -1};
    }

    private static int be32(byte[] d, int off) {
        return ((d[off] & 0xff) << 24) | ((d[off + 1] & 0xff) << 16)
                | ((d[off + 2] & 0xff) << 8) | (d[off + 3] & 0xff);
    }

    private com.codename1.ui.Image downsample(com.codename1.ui.Image full) {
        if (full == null) {
            return null;
        }
        Long tw = image().getResizeWidthPx();
        Long th = image().getResizeHeightPx();
        if (tw == null && th == null) {
            return full;
        }
        int iw = full.getWidth();
        int ih = full.getHeight();
        if (iw <= 0 || ih <= 0) {
            return full;
        }
        // A missing axis keeps the aspect ratio, as ResizeImage does.
        int w = tw != null ? (int) tw.longValue()
                : Math.max(1, (int) Math.round(iw * (th.doubleValue() / ih)));
        int h = th != null ? (int) th.longValue()
                : Math.max(1, (int) Math.round(ih * (tw.doubleValue() / iw)));
        if (w >= iw && h >= ih) {
            return full;   // never upscale; that is ResizeImage's rule too
        }
        try {
            return full.scaled(Math.max(1, w), Math.max(1, h));
        } catch (Throwable t) {
            return full;
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Double wPx = image().getWidth() == null ? null : Double.valueOf(Dp.px(image().getWidth()));
        Double hPx = image().getHeight() == null ? null : Double.valueOf(Dp.px(image().getHeight()));
        BoxConstraints inner = constraints.tighten(wPx, hPx);
        // A density variant carries `assetRatio` device pixels per logical
        // pixel, so its natural size on screen is the decoded size rescaled
        // from that density to the screen's — a 3.0x file on a 3x screen is
        // 1:1, the same file on a 2x screen is two thirds the size.
        double naturalScale = assetRatio > 0 ? Dp.scale() / assetRatio : 1;
        // cacheWidth/cacheHeight bound the DECODED bitmap, so they apply in
        // decoded pixels -- before the density rescale above, not after it.
        double[] decoded = decodeHinted(naturalW, naturalH,
                image().cacheWidth, image().cacheHeight);
        Size natural = img == null
                ? new Size(wPx == null ? 0 : wPx, hPx == null ? 0 : hPx)
                : new Size(decoded[0] * naturalScale, decoded[1] * naturalScale);
        // Aspect-preserving, which is what RenderImage does. Clamping the axes
        // independently gives a box with the constraint's width and the
        // picture's own height: an asset laid out under a width-driven fit then
        // sat in a box taller than its content, the artwork centred in the
        // slack, and everything below it pushed down by half the difference.
        // The gallery's lead photo was 46px low that way, and every element
        // under it with it.
        return inner.constrainSizeAndAttemptToPreserveAspectRatio(natural);
    }

    /**
     * Applies {@code cacheWidth}/{@code cacheHeight}, Flutter's decode-size
     * hints, to the picture's intrinsic size.
     *
     * <p>They decode the asset at that many DEVICE pixels, so they bound what
     * the image is intrinsically: an attachment asking for a 200px decode is
     * 200 device pixels wide however large the file on disk is, and a box
     * roomier than that leaves it at its own size rather than blowing it up.
     * Ignoring them made every such image lay out at the full asset's size --
     * the mail study's attachment strip drew three photographs across the
     * screen where the design has small thumbnails.</p>
     *
     * <p>One hint given alone carries the other axis with it, which is what
     * decoding does: the ratio is a property of the picture, not of the box.</p>
     */
    /**
     * The size a decoded picture presents once {@code cacheWidth} and
     * {@code cacheHeight} are taken into account, in the same units as the
     * size passed in.
     *
     * <p>{@code Image.asset} wraps its provider in a {@code ResizeImage}, so the
     * hints bound the decoded bitmap and therefore everything downstream of it:
     * the intrinsic size layout constrains, and the size the picture is painted
     * at. They never enlarge -- {@code ResizeImage} passes
     * {@code allowUpscaling: false} -- and when BOTH are given dart:ui decodes to
     * exactly those dimensions, so the aspect ratio is preserved only while one
     * of them is left open.</p>
     */
    static double[] decodeHinted(double w, double h, Long cacheWidth, Long cacheHeight) {
        if (w <= 0 || h <= 0) {
            return new double[] {w, h};
        }
        double tw = cacheWidth != null && cacheWidth.longValue() > 0
                ? Math.min(cacheWidth.doubleValue(), w) : -1;
        double th = cacheHeight != null && cacheHeight.longValue() > 0
                ? Math.min(cacheHeight.doubleValue(), h) : -1;
        if (tw > 0 && th > 0) {
            return new double[] {tw, th};
        }
        if (tw > 0) {
            return new double[] {tw, h * (tw / w)};
        }
        if (th > 0) {
            return new double[] {w * (th / h), th};
        }
        return new double[] {w, h};
    }

    /**
     * The picture's on-screen size in device pixels: the decoded bitmap after
     * its decode hints and its density variant. This is what Flutter measures
     * against the box, and so the largest size {@code scaleDown} draws it at.
     */
    private double[] sourceSizeForFit(com.codename1.ui.Image src) {
        double scale = assetRatio > 0 ? Dp.scale() / assetRatio : 1;
        double[] d = decodeHinted(src.getWidth(), src.getHeight(),
                image().cacheWidth, image().cacheHeight);
        return new double[] {d[0] * scale, d[1] * scale};
    }

    @Override
    public void position(int x, int y) {
        super.position(x, y);
        applyFit();
    }

    /**
     * Scales the icon into the laid-out box per the BoxFit (best-effort CN1
     * approximation; URLImage instances are left to their adapter).
     */
    /// The source image, box and fit the current icon was scaled for. Rescaling resamples
    /// the whole bitmap, and position() runs on every layout pass - so without this a
    /// carousel that rebuilds its cards per scroll frame re-scaled every card's full-size
    /// artwork every frame. Measured on the iOS simulator that was 11ms of paint per pass
    /// with 150ms spikes as new cards came into view, i.e. the whole frame budget.
    private com.codename1.ui.Image fittedFrom;
    private int fittedW = -1;
    private int fittedH = -1;
    private BoxFit fittedFit;
    private int fittedRadius;

    /**
     * Whether a deferred fit is already queued for this element.
     *
     * <p>Scaling an image DECODES it, and this runs inside layout — so the very
     * first frame used to wait for every visible image to decode. Measured on
     * the gallery's home screen that was 636ms of a 1187ms first frame, for 15
     * images. Flutter does not do this: {@code Image.asset} resolves
     * asynchronously and the first frame paints without the artwork, which then
     * appears a frame or two later. Matching that is worth more than any
     * micro-optimisation of the decode itself.
     *
     * <p>Deferred with {@code callSerially} rather than a background thread on
     * purpose: image creation is not safe off the event thread on every port,
     * and the win here comes from not blocking the FIRST frame, not from using
     * another core.</p>
     */
    private boolean fitScheduled;

    /**
     * Whether artwork may be resolved after the frame that asked for it.
     *
     * <p>True only until the first frame is on screen. Deferring FOREVER is
     * what Flutter does and is the better model, but it needs the box and the
     * bitmap to converge over several frames, and on an image-heavy grid ours
     * settles on the wrong crop. Bounding it to start-up takes the whole win
     * that matters for cold start — the first frame no longer waits for every
     * visible image to decode — without changing steady-state rendering.</p>
     */
    private static boolean deferFits = true;

    /** Called once the first frame is up; see {@link #deferFits}. */
    public static void firstFrameShown() {
        deferFits = false;
    }

    private void applyFit() {
        Label l = (Label) component();
        if (l == null || img == null || img instanceof URLImage) {
            return;
        }
        if (!com.codename1.ui.Display.isInitialized() || !deferFits) {
            fitNow();
            return;
        }
        if (needsFit() && !fitScheduled) {
            fitScheduled = true;
            com.codename1.ui.Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    fitScheduled = false;
                    fitNow();
                }
            });
        }
    }

    /** Whether the icon in place is not the one this box now wants. */
    private boolean needsFit() {
        int bw = (int) Math.round(size().width());
        int bh = (int) Math.round(size().height());
        if (bw <= 0 || bh <= 0) {
            return false;
        }
        BoxFit fit = image().getFit() == null ? BoxFit.scaleDown : image().getFit();
        return !(img == fittedFrom && bw == fittedW && bh == fittedH && fit == fittedFit
                && enclosingCornerRadius(bw, bh) == fittedRadius);
    }

    private void fitNow() {
        Label l = (Label) component();
        if (l == null || img == null || img instanceof URLImage) {
            return;
        }
        int bw = (int) Math.round(size().width());
        int bh = (int) Math.round(size().height());
        double[] src = sourceSizeForFit(img);
        int iw = (int) Math.round(src[0]);
        int ih = (int) Math.round(src[1]);
        if (bw <= 0 || bh <= 0 || iw <= 0 || ih <= 0) {
            return;
        }
        BoxFit fit = image().getFit() == null ? BoxFit.scaleDown : image().getFit();
        int radius = enclosingCornerRadius(bw, bh);
        if (img == fittedFrom && bw == fittedW && bh == fittedH && fit == fittedFit
                && radius == fittedRadius) {
            return;
        }
        fittedFrom = img;
        fittedW = bw;
        fittedH = bh;
        fittedFit = fit;
        fittedRadius = radius;
        // A fit that OVERFLOWS the box cannot be rounded by rounding what is
        // DRAWN: `cover` paints a rectangle larger than the component, so its
        // rounded corners fall outside the clip and what shows is square. This
        // only bites where the platform rounds in hardware, which is iOS and not
        // the simulator -- the desktop builds a rounded copy and looks right --
        // so the gallery's carousel cards were square on device and round in
        // every sweep. Send an overflowing fit through the copy below, which
        // scales to the BOX first and rounds that.
        double[] drawn = fittedSize(fit, bw, bh, iw, ih);
        boolean overflowsBox = drawn[0] > bw + 0.5 || drawn[1] > bh + 0.5;
        if ((radius <= 0 || (FittedImage.roundsInHardware() && !overflowsBox))
                && l instanceof FittedImage) {
            // NO COPY. The component draws the decoded source into its own box
            // under the fit rule, the way Flutter draws one texture through a
            // transform. Materialising a scaled bitmap per image and handing it
            // to a Label held the artwork twice -- the decoded original (behind
            // its soft reference) and the copy (hard, from the live component)
            // -- and it did the scaling on the layout pass that produced the
            // first frame.
            //
            // Rounded corners come through here too WHEN THE PLATFORM CAN DRAW
            // THEM. Where it can, the corners are a property of the draw and cost
            // nothing: no getRGB, no second bitmap, no second texture, and the
            // edge is anti-aliased because the platform computes coverage rather
            // than testing a stencil.
            //
            // Where it cannot, the copy below is still the right answer, and it
            // is worth knowing why: painting the source through a shaped CLIP was
            // tried and measured worse -- a grid of clipped thumbnails came out
            // 12.90% wrong against the reference where the rounded bitmap
            // measures 8.03% (`/demo/grid-lists`) -- because Codename One's
            // shaped clip has a hard edge and the reference anti-aliases its
            // corners. A hardware rounded draw does not have that problem; a
            // clip does.
            FittedImage f = (FittedImage) l;
            f.setSource(img);
            f.srcW = iw;
            f.srcH = ih;
            f.fit = fit;
            f.radius = radius;
            l.repaint();
            return;
        }
        long fitStart = System.currentTimeMillis();
        com.codename1.ui.Image scaled;
        // Scale to a bitmap, not through the image codec. EncodedImage.scaled()
        // defaults to "scaled encoded": it decodes, scales, RE-ENCODES the result
        // and keeps the compressed bytes -- and it picks JPEG at quality 0.9 for
        // any opaque picture, so the artwork this runtime displays had been
        // through a lossy round trip it never needed. The result is decoded again
        // on the next line anyway (roundCorners reads getRGB()), so the encode
        // bought nothing and cost a codec pass per image inside the layout that
        // produces the first frame: 36 of them in the gallery, and the retained
        // rasters behind them.
        Object prevScaling = restoreScalingTo();
        try {
            scaled = scaleUnencoded(img, fit, bw, bh, iw, ih);
        } finally {
            restoreScaling(prevScaling);
        }
        // The icon is only looked at when there is no source: FittedImage paints
        // its source directly and ignores the icon entirely. Handing it a rounded
        // copy while a source was still set meant the copy was built, retained,
        // and never drawn -- the picture rendered with square corners and paid
        // for round ones. Clear the source so the copy is what shows.
        if (l instanceof FittedImage) {
            ((FittedImage) l).setSource(null);
        }
        l.setIcon(roundCorners(scaled, radius));
        scaleMs += System.currentTimeMillis() - fitStart;
        scaleCount++;
        // The box was laid out before the artwork existed, so the frame that
        // showed it empty has to be replaced.
        l.repaint();
    }

    /// Turns off {@code encodedImageScaling} for the duration of one scale and
    /// returns the previous setting for {@link #restoreScaling}. The property is
    /// Codename One's own switch for this: with it off, {@code EncodedImage
    /// .scaled} scales the decoded bitmap instead of re-encoding.
    private static Object restoreScalingTo() {
        try {
            com.codename1.ui.Display d = com.codename1.ui.Display.getInstance();
            String prev = d.getProperty("encodedImageScaling", "true");
            d.setProperty("encodedImageScaling", "false");
            return prev;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void restoreScaling(Object prev) {
        if (prev == null) {
            return;
        }
        try {
            com.codename1.ui.Display.getInstance().setProperty("encodedImageScaling", (String) prev);
        } catch (Throwable t) {
            // leaving it off is safe; it only ever means "scale pixels, not bytes"
        }
    }

    private static com.codename1.ui.Image scaleUnencoded(com.codename1.ui.Image img,
            BoxFit fit, int bw, int bh, int iw, int ih) {
        com.codename1.ui.Image scaled;
        switch (fit) {
            case fill:
                scaled = img.scaled(bw, bh);
                break;
            case cover:
                scaled = img.fill(bw, bh);
                break;
            case fitWidth:
                scaled = img.scaled(bw, Math.max(1, ih * bw / iw));
                break;
            case fitHeight:
                scaled = img.scaled(Math.max(1, iw * bh / ih), bh);
                break;
            case none:
                scaled = img;
                break;
            case scaleDown: {
                double sr = Math.min(Math.min((double) bw / iw, (double) bh / ih), 1.0);
                scaled = img.scaled(Math.max(1, (int) Math.round(iw * sr)),
                        Math.max(1, (int) Math.round(ih * sr)));
                break;
            }
            case contain:
            default:
                double r = Math.min((double) bw / iw, (double) bh / ih);
                scaled = img.scaled(Math.max(1, (int) Math.round(iw * r)),
                        Math.max(1, (int) Math.round(ih * r)));
                break;
        }
        return scaled;
    }

    /**
     * A Label that paints its SOURCE image into its own box under a
     * {@link BoxFit} rule, instead of holding a scaled copy of it.
     *
     * <p>Codename One clips a component's paint to its bounds before calling
     * {@code paint}, so {@code cover} — which draws a rectangle larger than the
     * box — crops for free, and more faithfully than the whole-image
     * {@code Image.fill} approximation it replaces.</p>
     */
    /**
     * Holds at most one image lock, on whatever picture is currently on screen.
     *
     * <p>Separated from {@link FittedImage} because this is where the bugs live —
     * a lock left on a replaced picture never gets released, and a second lock on
     * the same picture never gets balanced — and because it can then be tested at
     * all: constructing any {@link com.codename1.ui.Image} needs an initialised
     * {@code Display}, which a headless test does not have. The two hooks are
     * overridable so a test can drive the bookkeeping with plain objects.</p>
     */
    static class ImageLock {

        private Object held;

        /// Makes {@code img} the locked image, releasing whatever was locked
        /// before. Null means "nothing should be locked".
        final void want(Object img) {
            if (held == img) {
                return;
            }
            if (held != null) {
                unlock(held);
            }
            held = img;
            if (held != null) {
                lock(held);
            }
        }

        final Object held() {
            return held;
        }

        void lock(Object img) {
            ((com.codename1.ui.Image) img).lock();
        }

        void unlock(Object img) {
            ((com.codename1.ui.Image) img).unlock();
        }
    }

    static final class FittedImage extends Label {

        private com.codename1.ui.Image source;
        /// Corner radius in device pixels, drawn by the platform. Only ever set
        /// when {@link #roundsInHardware()} is true; otherwise the caller builds
        /// a rounded bitmap instead and this stays zero.
        int radius;
        private final ImageLock lock = new ImageLock();
        BoxFit fit = BoxFit.scaleDown;
        /// The source's on-screen size in device pixels once its decode hints and
        /// density variant are applied; 0 means "ask the bitmap".
        int srcW;
        int srcH;
        FittedImage() {
            super("", "FlutterImage");
        }

        /// Sets the picture to draw, keeping the decode lock on whatever is
        /// actually being shown.
        ///
        /// An {@link com.codename1.ui.EncodedImage} keeps its decoded bitmap behind
        /// a soft reference and re-decodes on demand, so a picture that is drawn
        /// every frame and collected between them is decoded every frame. Label
        /// avoids that by locking its ICON while it is on screen; this component
        /// paints its source directly instead of holding a scaled copy as an icon,
        /// which is what keeps one bitmap in memory instead of two -- and which
        /// also stepped outside the locking Label does for free. Lock and unlock
        /// on the same boundary Label uses, so the picture is pinned exactly while
        /// it is on screen and collectable the moment it is not.
        void setSource(com.codename1.ui.Image img) {
            if (source == img) {
                return;
            }
            source = img;
            syncLock();
        }

        com.codename1.ui.Image getSource() {
            return source;
        }

        /// Whether this component is on screen, tracked rather than read back from
        /// {@code isInitialized()} so the two transitions drive the lock directly:
        /// the framework clears the initialised flag before it calls
        /// {@code deinitialize()}, and a lock that depends on the ORDER of those
        /// two is a lock that leaks the day the order changes.
        private boolean onScreen;

        private void syncLock() {
            lock.want(onScreen ? source : null);
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            onScreen = true;
            syncLock();
        }

        @Override
        protected void deinitialize() {
            onScreen = false;
            syncLock();
            super.deinitialize();
        }

        @Override
        public void paint(com.codename1.ui.Graphics g) {
            com.codename1.ui.Image s = source;
            if (s == null) {
                super.paint(g);
                return;
            }
            int bw = getWidth();
            int bh = getHeight();
            int iw = srcW > 0 ? srcW : s.getWidth();
            int ih = srcH > 0 ? srcH : s.getHeight();
            if (bw <= 0 || bh <= 0 || iw <= 0 || ih <= 0) {
                return;
            }
            double[] r = fittedSize(fit, bw, bh, iw, ih);
            double dw = r[0];
            double dh = r[1];
            // Centred in the box, which is what every BoxFit but `fill` means.
            int dx = getX() + (int) Math.round((bw - dw) / 2);
            int dy = getY() + (int) Math.round((bh - dh) / 2);
            // CLIPPED TO THE BOX, because several of the fits deliberately overflow it:
            // `cover` scales until the box is covered and spills on the other axis,
            // `none` draws the picture at its own size however big that is. Flutter's
            // paintImage clips; this did not, so a picture drew outside the component
            // that owns it -- and nothing else on the screen expects that. It showed up
            // as smearing in the status bar while scrolling the mail list: the avatars
            // were painting above the top of the list, over a band that is not part of
            // any scrolling content and so is not repainted as the list moves.
            int[] clip = g.getClip();
            g.clipRect(getX(), getY(), bw, bh);
            if (radius > 0) {
                g.drawImageRounded(s, dx, dy, (int) Math.round(dw), (int) Math.round(dh), radius);
            } else {
                g.drawImage(s, dx, dy, (int) Math.round(dw), (int) Math.round(dh));
            }
            g.setClip(clip[0], clip[1], clip[2], clip[3]);
        }

        /// Whether the platform rounds a picture's corners as it draws it, in
        /// which case no rounded copy has to be built. Resolved once: it is a
        /// property of the renderer, not of any one image.
        static boolean roundsInHardware() {
            if (roundsInHardware == null) {
                try {
                    roundsInHardware = com.codename1.ui.Display.isInitialized()
                            && com.codename1.ui.Display.getInstance().isRoundedImageSupported();
                } catch (Throwable t) {
                    roundsInHardware = Boolean.FALSE;
                }
            }
            return roundsInHardware.booleanValue();
        }

        private static Boolean roundsInHardware;
    }

    /**
     * The size {@code iw x ih} is drawn at inside a box {@code bw x bh} under
     * {@code fit} — the whole of BoxFit's arithmetic, free of any Graphics so
     * it can be pinned by a test.
     *
     * @return {@code {width, height}} in device pixels; {@code cover} returns a
     *         size LARGER than the box, which the component's own clip crops.
     */
    static double[] fittedSize(BoxFit fit, double bw, double bh, double iw, double ih) {
        switch (fit == null ? BoxFit.scaleDown : fit) {
            case fill:
                return new double[] {bw, bh};
            case cover: {
                double r = Math.max(bw / iw, bh / ih);
                return new double[] {iw * r, ih * r};
            }
            case fitWidth:
                return new double[] {bw, ih * (bw / iw)};
            case fitHeight:
                return new double[] {iw * (bh / ih), bh};
            case none:
                return new double[] {iw, ih};
            case scaleDown: {
                // Flutter's paintImage defaults to scaleDown when the widget
                // names no fit, and scaleDown is `contain` that never ENLARGES:
                // a picture smaller than its box is drawn at its own size,
                // centred in the slack. Treating the default as `contain` blew
                // every under-sized picture up to fill its box -- the reply
                // study's attachment strip drew its 200px thumbnails at 432px.
                double r = Math.min(Math.min(bw / iw, bh / ih), 1.0);
                return new double[] {iw * r, ih * r};
            }
            case contain:
            default: {
                double r = Math.min(bw / iw, bh / ih);
                return new double[] {iw * r, ih * r};
            }
        }
    }

    /// Cumulative cost of decoding and rescaling artwork, which happens inside
    /// layout and therefore inside the first frame. Read through
    /// {@link #scalingCost()}; see the startup trace in FlutterUI.
    private static long scaleMs;
    private static int scaleCount;

    /** How much of the frame went into decoding and rescaling images. */
    public static String scalingCost() {
        return scaleCount + " image(s) in " + scaleMs + "ms";
    }

    /**
     * The corner radius this image has to round into its own bitmap, or 0.
     *
     * <p>Non-zero only when the image exactly fills a clipping {@link
     * com.codename1.flutter.material.Material} with rounded corners — i.e. when the image
     * IS the card's surface and its own square corners are what you would see.</p>
     *
     * <p>Flutter expresses this as a clip and so does this runtime, but a clip is only as
     * good as the port underneath: on Codename One's iOS Metal backend a polygon clip masks
     * geometry (a {@code fillRect} through it comes out round) and does NOT mask a textured
     * quad, so the card's artwork paints straight over the rounded corners. Rounding the
     * bitmap once, when it is scaled, does not depend on the clip at all — and costs
     * nothing per frame, which a clip does.</p>
     */
    private int enclosingCornerRadius(int bw, int bh) {
        for (com.codename1.flutter.Element e = parent(); e != null; e = e.parent()) {
            if (!(e instanceof com.codename1.flutter.material.MaterialRenderElement)) {
                continue;
            }
            com.codename1.flutter.material.MaterialRenderElement m =
                    (com.codename1.flutter.material.MaterialRenderElement) e;
            int r = m.clipRadiusPx();
            if (r <= 0) {
                return 0;
            }
            // Only when this image really is the surface. An image inset inside a card has
            // square corners in Flutter too, and rounding it would be wrong.
            com.codename1.flutter.rendering.Size ms = m.size();
            if (ms == null || Math.abs(ms.width() - bw) > 1 || Math.abs(ms.height() - bh) > 1) {
                return 0;
            }
            return Math.min(r, Math.min(bw, bh) / 2);
        }
        return 0;
    }

    /**
     * Returns {@code src} with its corners cut to {@code radius}, transparent outside the
     * curve. Touches only the four corner squares, so the cost is the pixel copy rather
     * than the rounding.
     */
    private static com.codename1.ui.Image roundCorners(com.codename1.ui.Image src, int radius) {
        if (radius <= 0) {
            return src;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        int[] argb = src.getRGB();
        roundCornersInPlace(argb, w, h, radius);
        return com.codename1.ui.Image.createImage(argb, w, h);
    }

    /// Clears the alpha of every pixel lying outside the four corner arcs. Package-private
    /// and free of any Image so the geometry can be asserted headlessly.
    static void roundCornersInPlace(int[] argb, int w, int h, int radius) {
        int r = Math.min(radius, Math.min(w, h) / 2);
        for (int cy = 0; cy < r; cy++) {
            for (int cx = 0; cx < r; cx++) {
                // Distance from the centre of THIS corner's arc; outside it, clear alpha.
                double dx = r - 0.5 - cx;
                double dy = r - 0.5 - cy;
                if (dx * dx + dy * dy <= (double) r * r) {
                    continue;
                }
                argb[cy * w + cx] = 0;                              // top left
                argb[cy * w + (w - 1 - cx)] = 0;                    // top right
                argb[(h - 1 - cy) * w + cx] = 0;                    // bottom left
                argb[(h - 1 - cy) * w + (w - 1 - cx)] = 0;          // bottom right
            }
        }
    }
}
