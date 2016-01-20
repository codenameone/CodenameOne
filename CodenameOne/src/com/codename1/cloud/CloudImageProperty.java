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
package com.codename1.cloud;

import com.codename1.components.ReplaceableImage;
import com.codename1.io.CacheMap;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * A custom property that converts a cloud file id value to an image
 * that is cached locally asynchronously. 
 *
 * @author Shai Almog
 */
public class CloudImageProperty implements CustomProperty {
    private static CacheMap cloudImageCache;
    private String idProperty;
    private EncodedImage placeholderImage;
    private Hashtable<Object, ReplaceableImage> inProgress = new Hashtable<Object, ReplaceableImage>();
    
    /**
     * Create an image property for the given id that will hold the place holder image
     * while downloading the actual image
     * @param idProperty the id
     * @param placeholderImage placeholder shown while id is downloaded
     */
    public CloudImageProperty(String idProperty, EncodedImage placeholderImage) {
        this.idProperty = idProperty;
        this.placeholderImage = placeholderImage;
    }
            
    private CacheMap getCache() {
        if(cloudImageCache == null) {
            cloudImageCache = new CacheMap("CN1CIP$");
        }
        return cloudImageCache;
    }
    
    /**
     * {@inheritDoc}
     */
    public Object propertyValue(CloudObject obj, String propertyName) {
        final String key = (String)obj.getObject(idProperty);
        if(key == null) {
            return placeholderImage;
        }
        Image image = (Image)getCache().get(key);
        if(image == null) {
            ReplaceableImage r = inProgress.get(key);
            if(r != null) {
                return r;
            }
            final ReplaceableImage rp = ReplaceableImage.create(placeholderImage);
            inProgress.put(key, rp);
            ConnectionRequest cr = new ConnectionRequest() {
                private EncodedImage e;
                protected void readResponse(InputStream input) throws IOException  {
                    e = EncodedImage.create(input);;
                    if(e.getWidth() != placeholderImage.getWidth() || e.getHeight() != placeholderImage.getHeight()) {
                        ImageIO io = ImageIO.getImageIO();
                        if(io != null) {
                            ByteArrayOutputStream bo = new ByteArrayOutputStream();
                            io.save(new ByteArrayInputStream(e.getImageData()), bo, ImageIO.FORMAT_JPEG, placeholderImage.getWidth(), placeholderImage.getHeight(), 0.9f);
                            e = EncodedImage.create(bo.toByteArray());
                        }
                    }
                }
                protected void postResponse() {
                    rp.replace(e);
                    getCache().put(key, e);
                    inProgress.remove(key);
                }
            };
            cr.setPost(false);
            cr.setUrl(CloudStorage.getInstance().getUrlForCloudFileId(key));
            NetworkManager.getInstance().addToQueue(cr);
            return rp;
        }
        return image;
    }
    
}
