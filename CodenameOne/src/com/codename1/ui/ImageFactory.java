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
package com.codename1.ui;

/**
 * A factory class for creating mutable images.  This can be used to override how
 * mutable images are generated.  You can use this to implement, for example, a LRU
 * cache for images so that they can be reused to conserve memory.
 * 
 * <p>There is a default static image factory that can be assigned by calling:</p>
 * <pre>{@code
 * ImageFactory.setImageFactory(null, new ImageFactory() {
 *      public Image createImage(int w, int h, int bgColor) {
 *          // create an image
 *          // The Default would be just to call
 *          return Image.createImage(w, h, bgColor);
 *      }
 * });
 * }</pre>
 * 
 * <p>You can also assign a factory whose scope is limited to a particular Form, Container,
 * or Component, by setting the context argument in {@code setImageFactory()}.</p>
 * 
 * @author shannah
 * @since 8.0
 */
public abstract class ImageFactory {
    private static final String KEY = "$$IMAGE_FACTORY$$";
    
    /**
     * The default global image factory.
     */
    private static ImageFactory defaultFactory = new ImageFactory() {
        @Override
        public Image createImage(int w, int h, int bgColor) {
            return Image.createImage(w, h, bgColor);
        }
        
    };
    
    public abstract Image createImage(int w, int h, int bgColor);
    
    /**
     * Gets the image factory for a given component.  If the component
     * doesn't have an ImageFactory assigned, then it will check the parent
     * container, and its descendants, until it finds a Component with an ImageFactory.
     * 
     * <p>If none of the component's descendants have an ImageFactory, then it will
     * use the default global ImageFactory.</p>
     * 
     * @param cmp The context from which to load the image factory.  Use {@literal null} to get the global
     * default factory.
     * @return An image Factory
     */
    public static ImageFactory getImageFactory(Component cmp) {
        if (cmp == null) {
            return defaultFactory;
        }
        ImageFactory f = (ImageFactory)cmp.getClientProperty(KEY);
        if (f == null) {
            return getImageFactory(cmp.getParent());
            
        }
        return f;
    }
    
    /**
     * Sets the ImageFactory for the given component.
     * @param cmp The component to set the ImageFactory for.  If this parameter is {@literal null}, then 
     * this method will set the default global ImageFactory
     * @param f The ImageFactory to assign to the component.
     * @return The previous image factory that was assigned to the component, or null if none was previously assigned.
     */
    public static ImageFactory setImageFactory(Component cmp, ImageFactory f) {
        if (cmp != null) {
            ImageFactory old = (ImageFactory)cmp.getClientProperty(KEY);
            cmp.putClientProperty(KEY, f);
            return old;
        } else {
            ImageFactory old = defaultFactory;
            defaultFactory = f;
            return old;
        }
    }
    
    /**
     * Creates an image using the factory at a given context.
     * 
     * @param context The context where the ImageFactory should be loaded from.
     * @param w The width of the image to create.
     * @param h The height of the image to create.
     * @param bgColor The background color of the image.
     * @return A mutable Image.
     */
    public static Image createImage(Component context, int w, int h, int bgColor) {
        return getImageFactory(context).createImage(w, h, bgColor);
    }
}
