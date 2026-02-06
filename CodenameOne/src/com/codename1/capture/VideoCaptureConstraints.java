/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.capture;

/// Encapsulates constraints that can be used for capturing video.
///
/// You should set
/// the preferred constraints then if they are supported using `#isSupported()`, `#isSizeSupported()`, `#isQualitySupported()`,
/// `#isMaxLengthSupported()`, or `#isMaxFileSizeSupported()`.  If all of the constraints
/// are supported, then, `#isSupported()` will return true, and the resolved
/// constraints (`#getWidth()`, `#getHeight()`, `#getQuality()`, `#getMaxLength()`,
/// `#getMaxLength()`) will match their preferred counterparts.
///
/// If `#isSupported()` is false, then at least one of the constraints
/// is not supported by the system.  You can check support for a specific constraint using
/// `#isSizeSupported()`, `#isMaxFileSizeSupported()`, `#isQualitySupported()`,  or `#isMaxLengthSupported()`.
///
/// Example Using size and duration constraints:
///
/// ```java
/// `// Create capture constraint 320x240, with max length 20 seconds
/// VideoCaptureConstraints vcc = new VideoCaptureConstraints(320, 240, 20);
/// if (vcc.isSupported()) {
///     // These constraints are fully supported by this platform
///     // We can pass them directly to Capture.captureVideo() and the resulting
///     // video will match the constraints exactly.
///     // At this point, the following conditions are guaranteed to be true:
///     // 1. vcc.getPreferredWidth() == vcc.getWidth() == 320
///     // 2. vcc.getPreferredHeight() == vcc.getHeight() == 320
///     // 3. vcc.getPreferredMaxLength() == vcc.getMaxLength() == 20` else {
///     // At least one of the constraints is not supported.
///     // You can find out the "granted" constraints using getWidth(), getHeight(),
///     // and getMaxLength().
/// }
/// }
/// ```
///
/// Example Using Quality:
///
/// ```java
/// `//
/// VideoCaptureConstraints vcc = new VideoCaptureConstraints(VideoCaptureConstraints.QUALITY_LOW);
/// if (vcc.isSupported()) {
///     // This platform supports a 'low quality' setting.
///     //  Low quality generally means a smaller file size.` else {
///     // Low quality constraint is not supported.
/// }
/// }
/// ```
///
/// Platform Support Status
///
/// **Android** `#preferredQuality(int)`, `#preferredMaxLength(int)`,
/// and `#preferredMaxFileSize(long)` natively.  It doesn't fully support specific widths and
/// heights, but if `#preferredWidth(int)`, and `#preferredHeight(int)` are supplied, it will
/// translate these into either `#QUALITY_LOW`, or `#QUALITY_HIGH`.
///
/// **Javascript** supports ....
/// TODO  Add support for javascript and others
///
/// @author shannah
///
/// #### Since
///
/// 7.0
///
/// #### See also
///
/// - Capture#captureVideo(com.codename1.capture.VideoCaptureConstraints)
///
/// - Capture#captureVideo(com.codename1.capture.VideoCaptureConstraints, com.codename1.ui.events.ActionListener)
public class VideoCaptureConstraints {

    public static final int QUALITY_LOW = 1;
    public static final int QUALITY_HIGH = 2;


    /// The compiler, which will be set by the implementation at initialization time.
    private static Compiler compiler;
    boolean compiled;
    private int maxLength;
    private int width;
    private int height;
    private int quality;
    private long maxFileSize;
    private int preferredMaxLength;
    private int preferredWidth;
    private int preferredHeight;
    private int preferredQuality;
    private long preferredMaxFileSize;

    /// Creates a new video cosntraint with no constraints specified.
    public VideoCaptureConstraints() {

    }

    /// Copy constructor.
    ///
    /// #### Parameters
    ///
    /// - `toCopy`
    public VideoCaptureConstraints(VideoCaptureConstraints toCopy) {
        this.preferredHeight = toCopy.preferredHeight;
        this.preferredWidth = toCopy.preferredWidth;
        this.preferredMaxLength = toCopy.preferredMaxLength;
        this.width = toCopy.width;
        this.height = toCopy.height;
        this.maxLength = toCopy.maxLength;
        this.compiled = toCopy.compiled;
        this.quality = toCopy.quality;
        this.preferredQuality = toCopy.preferredQuality;
        this.maxFileSize = toCopy.maxFileSize;
        this.preferredMaxFileSize = toCopy.preferredMaxFileSize;
    }

    /// Creates a new constraint with the given quality constraint.
    ///
    /// #### Parameters
    ///
    /// - `quality`: The quality of the constraint.  Should be one of `#QUALITY_LOW` or `#QUALITY_HIGH`
    public VideoCaptureConstraints(int quality) {
        this.preferredQuality = quality;
    }

    /// Creates a new constraints with given preferred values.
    ///
    /// #### Parameters
    ///
    /// - `width`: The preferred width.  Pass 0 for no constraint.
    ///
    /// - `height`: The preferred height. Pass 0 for no constraint.
    ///
    /// - `maxLength`: The preferred max length in seconds. Pass 0 for no consraint.
    public VideoCaptureConstraints(int width, int height, int maxLength) {
        this.preferredWidth = width;
        this.preferredHeight = height;
        this.preferredMaxLength = maxLength;
    }

    /// Sets the native platform constraint compiler.  Should be called once during
    /// platform initialization.
    ///
    /// #### Parameters
    ///
    /// - `cmp`
    ///
    /// #### Deprecated
    ///
    /// Called by the platform.  For internal use only.
    public static void init(Compiler cmp) {
        compiler = cmp;
    }

    ///
    @Override
    public String toString() {
        return "VideoCaptureConstraints{" + getWidth() + "x" + getHeight() + " @ " + getMaxLength() + "s, " + getQualityString() + " " + getMaxFileSizeString() + "}";
    }

    private String getQualityString() {
        switch (getQuality()) {
            case QUALITY_LOW:
                return "Low quality";
            case QUALITY_HIGH:
                return "High quality";
            default:
                return "";
        }
    }

    private String getMaxFileSizeString() {
        if (getMaxFileSize() > 0) {
            return "<=" + getMaxFileSize() + " bytes";
        }
        return "";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VideoCaptureConstraints) {
            VideoCaptureConstraints c = (VideoCaptureConstraints) obj;
            return c.preferredHeight == preferredHeight && c.preferredWidth == preferredWidth && c.preferredMaxLength == preferredMaxLength && c.preferredQuality == preferredQuality && c.preferredMaxFileSize == preferredMaxFileSize;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.preferredMaxLength;
        hash = 29 * hash + this.preferredWidth;
        hash = 29 * hash + this.preferredHeight;
        hash = 29 * hash + this.preferredQuality;
        hash = 29 * hash + (int) this.preferredMaxFileSize;
        return hash;
    }

    /// Gets the maximum file size of the capture in bytes.
    ///
    /// #### Returns
    ///
    /// The max file size.
    ///
    /// #### See also
    ///
    /// - #preferredMaxFileSize(long)
    public long getMaxFileSize() {
        build();
        return maxFileSize;
    }

    /// Sets the preferred max file size.
    ///
    /// #### Parameters
    ///
    /// - `size`: The max file size in bytes.
    ///
    /// #### Returns
    ///
    /// Self for chaining
    ///
    /// #### See also
    ///
    /// - #getMaxFileSize()
    ///
    /// - #isMaxFileSizeSupported()
    public VideoCaptureConstraints preferredMaxFileSize(long size) {
        if (preferredMaxFileSize != size) {
            preferredMaxFileSize = size;
            compiled = false;
        }
        return this;
    }

    /// Gets the preferred max file size.
    ///
    /// #### Returns
    ///
    /// The preferred max file size, in bytes.
    ///
    /// #### See also
    ///
    /// - #preferredMaxFileSize(long)
    ///
    /// - #getMaxFileSize()
    ///
    /// - #isMaxFileSizeSupported()
    public long getPreferredMaxFileSize() {
        return preferredMaxFileSize;
    }

    /// Returns the maximum length (in seconds) of this constraint.
    ///
    /// This method always
    /// returns the resolved value that the platform supports.  If the platform doesn't support
    /// setting the max length of a video capture, then this will return 0.  If it supports
    /// the value of `#getPreferredMaxLength()`, then this will return the same value.  If it
    /// supports limiting the length of video capture, but it doesn't support the preferred max length value,
    /// then this will return the closest that the platform can provide to the preferred value.
    ///
    /// Note: This value will be equal to `#getPreferredMaxLength()` iff `#isMaxLengthSupported()` is true.
    ///
    /// #### Returns
    ///
    /// the maxLength The maximum length, in seconds, for the video capture.  Zero for no limit.
    ///
    /// #### See also
    ///
    /// - #isMaxLengthSupported()
    ///
    /// - #preferredMaxLength(int)
    ///
    /// - #getPreferredMaxLength()
    public int getMaxLength() {
        build();
        return maxLength;
    }

    /// Set the preferred max length for the video capture in seconds.
    ///
    /// If the platform supports
    /// this value, then `#getMaxLength()` will return this same value.  If the platform
    /// does not support this value, then `#getMaxLength()` will return the closest value
    /// that the platform supports.
    ///
    /// #### Parameters
    ///
    /// - `maxLength`: the maxLength to set, in seconds. Set 0 for no limit.
    ///
    /// #### Returns
    ///
    /// Self for chaining
    ///
    /// #### See also
    ///
    /// - #getPreferredMaxLength()
    ///
    /// - #getMaxLength()
    ///
    /// - #isMaxLengthSupported()
    public VideoCaptureConstraints preferredMaxLength(int maxLength) {
        if (maxLength != preferredMaxLength) {
            this.preferredMaxLength = maxLength;
            compiled = false;
        }
        return this;
    }

    /// Gets the preferred max length video capture, in seconds.
    ///
    /// #### Returns
    ///
    /// The preferred max length, in seconds.
    ///
    /// #### See also
    ///
    /// - #preferredMaxLength(int)
    ///
    /// - #getMaxLength()
    ///
    /// - #isMaxLengthSupported()
    public int getPreferredMaxLength() {
        return preferredMaxLength;
    }

    /// Checks to see if the preferred max length specified in this constraint is supported
    /// by the underlying platform.
    ///
    /// #### Returns
    ///
    /// True if and only if the preferred max length value is supported by the underlying platform.
    ///
    /// #### See also
    ///
    /// - #getMaxLength()
    ///
    /// - #preferredMaxLength(int)
    ///
    /// - #getPreferredMaxLength()
    public boolean isMaxLengthSupported() {
        build();
        return maxLength == 0 || maxLength == preferredMaxLength;
    }

    /// Gets the width constraint that is supported by the platform, and is nearest to the specified
    /// preferred width.  If the platform supports the preferred width constraint, then `#getPreferredWidth()`
    /// will be the same as `#getWidth()`.  If the platform doesn't support any width constraints at all
    /// then this will return `0`.  If the platform supports some width constraints, but not the constraint specified,
    /// then this will return the most nearest value that the platform supports.
    ///
    /// #### Returns
    ///
    /// the Platform-supported width of this constraint.
    ///
    /// #### See also
    ///
    /// - #getPreferredWidth()
    ///
    /// - #isSizeSupported()
    ///
    /// - #preferredWidth(int)
    public int getWidth() {
        build();
        return width;
    }

    /// Gets the preferred width constraint.
    ///
    /// #### Returns
    ///
    /// The preferred width constraint.
    ///
    /// #### See also
    ///
    /// - #getWidth()
    ///
    /// - #preferredWidth(int)
    ///
    /// - #isSizeSupported()
    public int getPreferredWidth() {
        return preferredWidth;
    }

    /// Gets the preferred height constraint.
    ///
    /// #### Returns
    ///
    /// The preferred height constraint.
    ///
    /// #### See also
    ///
    /// - #getHeight()
    ///
    /// - #preferredHeight(int)
    ///
    /// - #isSizeSupported() `
    public int getPreferredHeight() {
        return preferredHeight;
    }

    /// Checks if the specified preferred width and height constraints are supported by the platform.
    ///
    /// #### Returns
    ///
    /// True if the platform supports the width and height constraints specified directly.
    ///
    /// #### See also
    ///
    /// - #getWidth()
    ///
    /// - #getHeight()
    ///
    /// - #getPreferredWidth()
    ///
    /// - #getPreferredHeight()
    ///
    /// - #preferredWidth(int)
    ///
    /// - #preferredHeight(int)
    ///
    /// - #isSupported()
    public boolean isSizeSupported() {
        build();
        if (preferredWidth == 0 && preferredHeight == 0) {
            return true;
        }
        return (width == preferredWidth && height == preferredHeight);
    }

    /// Sets the preferred width constraint.
    ///
    /// #### Parameters
    ///
    /// - `width`: the width to set
    ///
    /// #### See also
    ///
    /// - #getWidth()
    ///
    /// - #getPreferredWidth()
    ///
    /// - #isSizeSupported()
    public VideoCaptureConstraints preferredWidth(int width) {
        if (width != preferredWidth) {
            this.preferredWidth = width;
            compiled = false;
        }
        return this;
    }

    /// Gets the platform-supported height constraint.  If the platform supports
    /// the preferred height constraint, then this will return the same value
    /// as `#getPreferredHeight()`.  If the platform doesn't support any
    /// height constraints, then this will return 0.  If the platform
    /// supports height constraints, but not the specific preferred height value, then
    /// this will return the nearest value that is supported by the platform.
    ///
    /// #### Returns
    ///
    /// the platform-supported height constraint.
    ///
    /// #### See also
    ///
    /// - #getPreferredHeight()
    ///
    /// - #preferredHeight(int)
    ///
    /// - #isSizeSupported()
    public int getHeight() {
        build();
        return height;
    }

    /// Sets the preferred height constraint.
    ///
    /// #### Parameters
    ///
    /// - `height`: the height to set
    ///
    /// #### See also
    ///
    /// - #getPreferredHeight()
    ///
    /// - #getHeight()
    ///
    /// - #isSizeSupported()
    public VideoCaptureConstraints preferredHeight(int height) {
        if (height != preferredHeight) {
            this.preferredHeight = height;
            compiled = false;
        }
        return this;
    }

    /// Gets the preferred quality of the recording.
    ///
    /// #### Returns
    ///
    /// May be one of `#QUALITY_LOW`, `#QUALITY_HIGH`, or 0.
    ///
    /// #### See also
    ///
    /// - #preferredQuality(int)
    ///
    /// - #isQualitySupported()
    ///
    /// - #getQuality()
    public int getPreferredQuality() {
        return preferredQuality;
    }

    /// Gets the quality of the recording.
    ///
    /// #### Returns
    ///
    /// May be one of `#QUALITY_LOW`, `#QUALITY_HIGH`, or 0.
    ///
    /// #### See also
    ///
    /// - #getPreferredQuality()
    ///
    /// - #preferredQuality(int)
    ///
    /// - #isQualitySupported()
    public int getQuality() {
        build();
        return quality;
    }

    /// Sets the preferred quality of the video recording.
    ///
    /// #### Parameters
    ///
    /// - `quality`: May be one of `#QUALITY_LOW`, `#QUALITY_HIGH`, or 0.
    ///
    /// #### Returns
    ///
    /// Self for chaining
    ///
    /// #### See also
    ///
    /// - #getQuality()
    ///
    /// - #getPreferredQuality()
    ///
    /// - #isQualitySupported()
    public VideoCaptureConstraints preferredQuality(int quality) {
        if (quality != preferredQuality) {
            preferredQuality = quality;
            compiled = false;
        }
        return this;
    }

    /// Builds the constraint.  This will defer to the platform's compiler to
    /// figure out the supported constraint values.
    private VideoCaptureConstraints build() {
        if (compiled) {
            return this;
        }
        if (compiler == null) {
            this.height = 0;
            this.width = 0;
            this.maxLength = 0;
            this.quality = 0;
            this.maxFileSize = 0;
        } else {
            VideoCaptureConstraints result = compiler.compile(this);
            width = result.preferredWidth;
            height = result.preferredHeight;
            maxLength = result.preferredMaxLength;
            quality = result.preferredQuality;
            maxFileSize = result.preferredMaxFileSize;
        }
        compiled = true;
        return this;
    }

    /// Checks if this constraint is fully supported by the platform.
    ///
    /// #### Returns
    ///
    /// True if the constrains described in this object are fully supported by the platform.
    ///
    /// #### See also
    ///
    /// - #isSizeSupported()
    ///
    /// - #isMaxLengthSupported()
    public boolean isSupported() {
        return isSizeSupported() && isMaxLengthSupported() && isQualitySupported() && isMaxFileSizeSupported();
    }

    /// Checks if the preferred quality setting is supported.
    ///
    /// #### Returns
    ///
    /// True if the preferred quality is not set (i.e. 0), or is supported by the platform.
    ///
    /// #### See also
    ///
    /// - #getPreferredQuality()
    ///
    /// - #getQuality()
    ///
    /// - #preferredQuality(int)
    public boolean isQualitySupported() {
        return preferredQuality == 0 || quality == preferredQuality;
    }

    /// Checks if the max file size constraint is supported.
    ///
    /// #### Returns
    ///
    /// true if the max file size constraint is supported, or the max file size constraint isn't set.
    ///
    /// #### See also
    ///
    /// - #preferredMaxFileSize(long)
    ///
    /// - #getMaxFileSize()
    ///
    /// - #getPreferredMaxFileSize()
    public boolean isMaxFileSizeSupported() {
        return preferredMaxFileSize == 0 || maxFileSize == preferredMaxFileSize;
    }

    /// Checks if this constraint is effectively a null constraint.  I.e. it doesn't include
    /// any width, height, or max length constraints.
    ///
    /// #### Returns
    ///
    /// True if this constraints is effectively null.
    public boolean isNullConstraint() {
        return getWidth() == 0 && getHeight() == 0 && getMaxLength() == 0 && getQuality() == 0 && getMaxFileSize() == 0;
    }

    /// An interface that will be implemented by the implementation. It's job is
    /// to transfer the "preferred" constraints to corresponding "actual" constraints
    /// based on what the platform supports.
    public interface Compiler { // PMD Fix: UnnecessaryModifier removed implicit static
        VideoCaptureConstraints compile(VideoCaptureConstraints cnst); // PMD Fix: UnnecessaryModifier removed redundant public
    }

}
