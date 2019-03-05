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

/**
 * Encapsulates constraints that can be used for capturing video. 
 * 
 * <p>You should set
 * the preferred constraints, then {@link #build() } the constraints to have the 
 * platform determine whether the constraints are supported.  If the constraints
 * are fully supported, then, {@link #isSupported() } will return true, and the resolved
 * constraints (height, width, maxLength) will match their preferred counterparts.  </p>
 * 
 * <p>If {@link #isSupported() } is {@literal false}, then at least one of the constraints
 * is not supported by the system.  You can check support for a specific constraint using
 * {@link #isSizeSupported() } or {@link #isMaxLengthSupported() }.</p>
 * 
 * <h3>Example Usage:</h3>
 * 
 * <pre>{@code
 * // Create capture constraint 320x240, with max length 20 seconds
 * VideoCaptureConstraints vcc = new VideoCaptureConstraints(320, 240, 20);
 * if (vcc.isSupported()) {
 *     // These constraints are fully supported by this platform
 *     // We can pass them directly to Capture.captureVideo() and the resulting
 *     // video will match the constraints exactly.
 *     // At this point, the following conditions are guaranteed to be true:
 *     // 1. vcc.getPreferredWidth() == vcc.getWidth() == 320
 *     // 2. vcc.getPreferredHeight() == vcc.getHeight() == 320
 *     // 3. vcc.getPreferredMaxLength() == vcc.getMaxLength() == 20
 * } else {
 *     // At least one of the constraints is not supported.
 *     // You can find out the "granted" constraints using getWidth(), getHeight(), 
 *     // and getMaxLength().
 * }
 * }</pre>
 * 
 * 
 * @author shannah
 * @since 7.0
 * @see Capture#captureVideo(com.codename1.capture.VideoCaptureConstraints) 
 * @see Capture#captureVideo(com.codename1.capture.VideoCaptureConstraints, com.codename1.ui.events.ActionListener) 
 */
public class VideoCaptureConstraints {
    
    /**
     * The compiler, which will be set by the implementation at initialization time.
     */
    private static Compiler compiler;
    
    /**
     * An interface that will be implemented by the implementation. It's job is 
     * to transfer the "preferred" constraints to corresponding "actual" constraints
     * based on what the platform supports.
     */
    public static interface Compiler {
        public VideoCaptureConstraints compile(VideoCaptureConstraints cnst);
    }
    
    /**
     * Sets the native platform constraint compiler.  Should be called once during
     * platform initialization.
     * @param cmp 
     * @deprecated Called by the platform.  For internal use only.
     */
    public static void init(Compiler cmp) {
        compiler = cmp;
    }
    
    private int maxLength;
    private int width;
    private int height;
    
    private int preferredMaxLength;
    private int preferredWidth;
    private int preferredHeight;
    
    boolean compiled;
    
    /**
     * Creates a new video cosntraint with no constraints specified.
     */
    public VideoCaptureConstraints() {
        
    }
    
    /**
     * Copy constructor.
     * @param toCopy 
     */
    public VideoCaptureConstraints(VideoCaptureConstraints toCopy) {
        this.preferredHeight = toCopy.preferredHeight;
        this.preferredWidth = toCopy.preferredWidth;
        this.preferredMaxLength = toCopy.preferredMaxLength;
        this.width = toCopy.width;
        this.height = toCopy.height;
        this.maxLength = toCopy.maxLength;
        this.compiled = toCopy.compiled;
    }

    /**
     *
     * @return 
     */
    @Override
    public String toString() {
        return "VideoCaptureConstraints{"+getWidth()+"x"+getHeight()+" @ "+getMaxLength()+"s}";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() == VideoCaptureConstraints.class) {
            VideoCaptureConstraints c = (VideoCaptureConstraints)obj;
            return c.preferredHeight == preferredHeight && c.preferredWidth == preferredWidth && c.maxLength == maxLength;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.maxLength;
        hash = 29 * hash + this.preferredWidth;
        hash = 29 * hash + this.preferredHeight;
        return hash;
    }
    
    
    
    
    /**
     * Creates a new constraints with given preferred values.
     * @param width The preferred width.  Pass {@literal 0} for no constraint.
     * @param height The preferred height. Pass {@literal 0} for no constraint.
     * @param maxLength The preferred max length in seconds. Pass {@literal 0} for no consraint.
     */
    public VideoCaptureConstraints(int width, int height, int maxLength) {
        this.preferredWidth = width;
        this.preferredHeight = height;
        this.preferredMaxLength = maxLength;
    }

    /**
     * Returns the maximum length (in seconds) of this constraint.  
     * 
     * <p>This method always
     * returns the resolved value that the platform supports.  If the platform doesn't support
     * setting the max length of a video capture, then this will return {@literal 0}.  If it supports
     * the value of {@link #getPreferredMaxLength() }, then this will return the same value.  If it 
     * supports limiting the length of video capture, but it doesn't support the preferred max length value,
     * then this will return the closest that the platform can provide to the preferred value.</p>
     * 
     * <p>Note: This value will be equal to {@link #getPreferredMaxLength() } iff {@link #isMaxLengthSupported() } is {@literal true}.</p>
     * 
     * @return the maxLength The maximum length, in seconds, for the video capture.  Zero for no limit.
     * 
     * @see #isMaxLengthSupported()
     * @see #preferredMaxLength(int) 
     * @see #getPreferredMaxLength() 
     */
    public int getMaxLength() {
        build();
        return maxLength;
    }

    /**
     * Set the preferred max length for the video capture in seconds.  
     * 
     * <p>If the platform supports
     * this value, then {@link #getMaxLength() } will return this same value.  If the platform
     * does not support this value, then {@link #getMaxLength() } will return the closest value
     * that the platform supports.</p>
     * @param maxLength the maxLength to set, in seconds. Set {@literal 0} for no limit.
     * @return Self for chaining
     * 
     * @see #getPreferredMaxLength() 
     * @see #getMaxLength() 
     * @see #isMaxLengthSupported() 
     */
    public VideoCaptureConstraints preferredMaxLength(int maxLength) {
        if (maxLength != preferredMaxLength) {
            this.preferredMaxLength = maxLength;
            compiled = false;
        }
        return this;
    }
    
    /**
     * Gets the preferred max length video capture, in seconds.
     * 
     * @return The preferred max length, in seconds.
     * 
     * @see #preferredMaxLength(int) 
     * @see #getMaxLength() 
     * @see #isMaxLengthSupported() 
     */
    public int getPreferredMaxLength() {
        return preferredMaxLength;
    }
    
    /**
     * Checks to see if the preferred max length specified in this constraint is supported
     * by the underlying platform.
     * @return True if and only if the preferred max length value is supported by the underlying platform.  
     * @see #getMaxLength() 
     * @see #preferredMaxLength(int) 
     * @see #getPreferredMaxLength() 
     */
    public boolean isMaxLengthSupported() {
        build();
        return maxLength == preferredMaxLength;
    }
    
    

    /**
     * Gets the width constraint that is supported by the platform, and is nearest to the specified
     * preferred width.  If the platform supports the preferred width constraint, then {@link #getPreferredWidth() }
     * will be the same as {@link #getWidth() }.  If the platform doesn't support any width constraints at all
     * then this will return {@link 0}.  If the platform supports some width constraints, but not the constraint specified,
     * then this will return the most nearest value that the platform supports.
     * @return the Platform-supported width of this constraint.
     * 
     * @see #getPreferredWidth() 
     * @see #isSizeSupported() 
     * @see #preferredWidth(int) 
     */
    public int getWidth() {
        build();
        return width;
    }
    
    /**
     * Gets the preferred width constraint.
     * @return The preferred width constraint.
     * @see #getWidth() 
     * @see #preferredWidth(int) 
     * @see #isSizeSupported() 
     */
    public int getPreferredWidth() {
        return preferredWidth;
    }
    
    /**
     * Gets the preferred height constraint.
     * 
     * @return The preferred height constraint.
     * 
     * @see #getHeight() 
     * @see #preferredHeight(int) 
     * @see #isSizeSupported() `
     */
    public int getPreferredHeight() {
        return preferredHeight;
    }
    
    /**
     * Checks if the specified preferred width and height constraints are supported by the platform.
     * @return True if the platform supports the width and height constraints specified directly.
     * 
     * @see #getWidth() 
     * @see #getHeight() 
     * @see #getPreferredWidth() 
     * @see #getPreferredHeight()
     * @see #preferredWidth(int) 
     * @see #preferredHeight(int) 
     * @see #isSupported() 
     */
    public boolean isSizeSupported() {
        build();
        return (width == preferredWidth && height == preferredHeight);
    }

    /**
     * Sets the preferred width constraint.
     * @param width the width to set
     * @see #getWidth() 
     * @see #getPreferredWidth() 
     * @see #isSizeSupported() 
     */
    public VideoCaptureConstraints preferredWidth(int width) {
        if (width != preferredWidth) {
            this.preferredWidth = width;
            compiled = false;
        }
        return this;
    }

    /**
     * Gets the platform-supported height constraint.  If the platform supports
     * the preferred height constraint, then this will return the same value
     * as {@link #getPreferredHeight() }.  If the platform doesn't support any
     * height constraints, then this will return {@literal 0}.  If the platform
     * supports height constraints, but not the specific preferred height value, then
     * this will return the nearest value that is supported by the platform.
     * @return the platform-supported height constraint.
     * 
     * @see #getPreferredHeight() 
     * @see #preferredHeight(int) 
     * @see #isSizeSupported() 
     */
    public int getHeight() {
        build();
        return height;
    }

    /**
     * Sets the preferred height constraint.
     * @param height the height to set
     * 
     * @see #getPreferredHeight() 
     * @see #getHeight() 
     * @see #isSizeSupported() 
     */
    public VideoCaptureConstraints preferredHeight(int height) {
        if (height != preferredHeight) {
            this.preferredHeight = height;
            compiled = false;
        }   
        return this;
    }
    
    /**
     * Builds the constraint.  This will defer to the platform's compiler to 
     * figure out the supported constraint values.
     * 
     * @return 
     */
    private VideoCaptureConstraints build() {
        if (compiled) return this;
        if (compiler == null) {
            this.height = 0;
            this.width = 0;
            this.maxLength = 0;
        } else {
            VideoCaptureConstraints result = compiler.compile(this);
            width = result.preferredWidth;
            height = result.preferredHeight;
            maxLength = result.preferredMaxLength;
        }
        compiled = true;
        return this;
    }
   
    /**
     * Checks if this constraint is fully supported by the platform.
     * @return True if the constrains described in this object are fully supported by the platform.
     * 
     * @see #isSizeSupported() 
     * @see #isMaxLengthSupported() 
     */
    public boolean isSupported() {
        return isSizeSupported() && isMaxLengthSupported();
    }
    
    /**
     * Checks if this constraint is effectively a null constraint.  I.e. it doesn't include
     * any width, height, or max length constraints.
     * @return True if this constraints is effectively null.
     */
    public boolean isNullConstraint() {
        return getWidth() == 0 && getHeight() == 0 && getMaxLength() == 0;
    }
    
}
